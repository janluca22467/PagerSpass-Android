package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.Abostand
import de.pagerspass.pagerspass.netz.Abzeichen
import de.pagerspass.pagerspass.netz.ArchivRunde
import de.pagerspass.pagerspass.netz.ArchivStatistik
import de.pagerspass.pagerspass.netz.Beschenkbarer
import de.pagerspass.pagerspass.netz.Bestenlistenplatz
import de.pagerspass.pagerspass.netz.Buchungsposten
import de.pagerspass.pagerspass.netz.Codegutschrift
import de.pagerspass.pagerspass.netz.Dienstauswertung
import de.pagerspass.pagerspass.netz.Dienstbuchwege
import de.pagerspass.pagerspass.netz.Dienstschicht
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Gluecksradergebnis
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kontofreischaltung
import de.pagerspass.pagerspass.netz.Kontofreischaltungen
import de.pagerspass.pagerspass.netz.Laufbahnstufe
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Netzfehler
import de.pagerspass.pagerspass.netz.Saisonbestenlistenplatz
import de.pagerspass.pagerspass.netz.Shop
import de.pagerspass.pagerspass.netz.Tagesschicht
import de.pagerspass.pagerspass.netz.Wachenkurve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Der Speicher von Dienstbuch und Shop — das Gegenstück zu `stores/dienstbuch.ts`
 * und dem Shop-Teil von `stores/spiel.ts`.
 *
 * <b>Ein eigener Speicher neben dem `Seitenstand`</b>, und zwar aus demselben Grund
 * wie im Web: Das Dienstbuch liegt auf sechs Seiten (Übersicht, Schichten,
 * Laufbahn, Garage, Abzeichen, Auswertung), die sich denselben Bestand teilen. Er
 * wird einmal geholt und über alle Reiter gehalten — ein Wechsel ist dann ein
 * Sprung, und die Zahlen springen nicht mit.
 *
 * <b>Er gehört der Sitzung</b> und bekommt von ihr, was er nicht selbst hat: das
 * Konto, und die Rückwege für Credits, Shop und Garage. Ein Kauf ändert den
 * Kontostand, der auch in der Leiste und am Dienstausweis steht — eine zweite
 * Wahrheit dafür wäre eine Zahl, die irgendwann etwas anderes sagt.
 *
 * <b>Die Handlungen antworten mit `Result`.</b> Im Web legt jede Ansicht ihre
 * Rückmeldung neben den Knopf, auf den sie sich bezieht („steht jetzt in deiner
 * Garage", „noch 2 Tage"); eine Meldung am Fuß der App wäre dort am falschen Ort.
 * Deshalb reicht der Speicher den Ausgang zurück und die Seite entscheidet, wohin
 * er gehört.
 */
class Dienstbuchstelle(
    netz: Netz,
    private val bereich: CoroutineScope,
    private val konto: () -> Konto?,
    private val kontoGesetzt: (Konto) -> Unit,
    private val creditsGesetzt: (Int) -> Unit,
    private val shopGesetzt: (Shop) -> Unit,
    private val garageNeu: () -> Unit,
) {
    private val wege = Dienstbuchwege(netz)

    private val _stand = MutableStateFlow(Dienstbuchstand())
    val stand: StateFlow<Dienstbuchstand> = _stand.asStateFlow()

    /** Für welche Kennung der Bestand gilt; ein Kontowechsel wirft ihn weg. */
    private var geladenFuer: String? = null

    private fun kennung(): String =
        konto()?.kennung ?: throw IllegalStateException("Kein Konto angemeldet.")

    /**
     * Die Kennung des angemeldeten Kontos — und, wenn es ein anderes ist als beim
     * letzten Mal, ein leerer Stand. Ohne das stünde nach einem Kontowechsel das
     * Abo, die Vitrine oder die Nachbesprechung des Vorgängers da, bis jemand neu
     * lädt: Jede Ladefunktion prüft „schon geholt?" und sähe den alten Stand.
     */
    private fun kontoPruefen(): String? {
        val jetzt = konto()?.kennung ?: return null
        if (geladenFuer != jetzt) {
            _stand.value = Dienstbuchstand()
            geladenFuer = jetzt
        }
        return jetzt
    }

    // ------------------------------------------------------------- Dienstbuch

    /**
     * Den Bestand holen — nur, wenn er für dieses Konto noch fehlt.
     *
     * <b>Archiv und Statistik tragen die Seite, alles andere ist Beiwerk.</b>
     * Fällt die Laufbahn, die Bestenliste oder die Tagesschicht aus, bleibt das
     * Dienstbuch lesbar — die eigenen Schichten sind das, weswegen man herkommt.
     * Ohne das eigene `runCatching` risse ein Fehler dort alles mit.
     */
    fun laden(neu: Boolean = false) {
        val jetzt = kontoPruefen() ?: return
        val vorher = _stand.value
        if (vorher.buch.laedt) return
        if (!neu && vorher.buch.geladen && vorher.buch.fehler == null) return

        _stand.update { it.copy(buch = it.buch.copy(laedt = true, fehler = null)) }

        bereich.launch {
            runCatching { holen(jetzt) }
                .onSuccess { daten ->
                    if (geladenFuer != jetzt) return@onSuccess
                    _stand.update {
                        it.copy(buch = Bereich(daten, geladen = true), posten = emptyMap())
                    }
                }
                .onFailure { f ->
                    _stand.update {
                        it.copy(
                            buch = it.buch.copy(
                                laedt = false,
                                geladen = true,
                                fehler = f.message ?: "Das Dienstbuch ist nicht erreichbar.",
                            ),
                        )
                    }
                }
        }
    }

    private suspend fun holen(kennung: String): Dienstbuchdaten = coroutineScope {
        val archiv = async { wege.archiv(kennung, 50) }
        val chronik = async { runCatching { wege.chronik(kennung, 100) }.getOrDefault(emptyList()) }
        val statistik = async { wege.statistik(kennung, 10) }
        val laufbahn = async { runCatching { wege.laufbahn() }.getOrDefault(emptyList()) }
        val bestenliste = async { runCatching { wege.bestenliste(kennung, 25) }.getOrDefault(emptyList()) }
        val saison = async { runCatching { wege.saisonbestenliste(kennung, 25) }.getOrDefault(emptyList()) }
        val tagesschicht = async { runCatching { wege.tagesschicht(kennung) }.getOrNull() }
        val abzeichen = async { runCatching { wege.abzeichen(kennung) }.getOrDefault(emptyList()) }
        val kontoRoh = async { runCatching { wege.kontoRoh(kennung) }.getOrNull() }

        val nachCode = chronik.await().associateBy { it.roomCode }
        val schichten = archiv.await().map { a ->
            val eigen: Dienstschicht? = nachCode[a.code]
            Schichtzeile(
                code = a.code,
                beendetUm = a.beendetUm,
                ort = a.landkreis ?: a.ort,
                landkreis = a.landkreis,
                einsaetze = eigen?.einsaetze ?: a.einsaetze,
                hilfsfristSekunden = eigen?.hilfsfristSekunden,
                rolle = eigen?.rolle,
                funkrufname = eigen?.funkrufname,
                fahrzeugtyp = eigen?.fahrzeugtyp,
                punkte = eigen?.punkte,
            )
        }

        // Das Konto frisch — Stufe und Punkte stehen im Kopf aller sechs Seiten,
        // und die Freischaltungen braucht der Laufbahnpass.
        val roh = kontoRoh.await()
        val freischaltungen = roh?.let { text ->
            runCatching { Netz.abgabe.decodeFromString<Kontofreischaltungen>(text) }
                .getOrNull()?.freischaltungen
        }.orEmpty()
        roh?.let { text ->
            runCatching { Netz.abgabe.decodeFromString<Konto>(text) }.getOrNull()
        }?.let { frisch ->
            // Nur, wenn es noch dasselbe Konto ist — wer inzwischen abgemeldet
            // ist, bekommt kein Konto zurück, das er nicht mehr hat.
            if (konto()?.kennung == frisch.kennung) kontoGesetzt(frisch)
        }

        Dienstbuchdaten(
            schichten = schichten,
            statistik = statistik.await(),
            laufbahn = laufbahn.await(),
            bestenliste = bestenliste.await(),
            saison = saison.await(),
            tagesschicht = tagesschicht.await(),
            abzeichen = abzeichen.await(),
            freischaltungen = freischaltungen,
        )
    }

    /**
     * Die Kurven der eigenen Wache — reines Beiwerk fürs Diagramm. Ein Fehler hier
     * darf das Dienstbuch nicht mitreißen; wer in keiner Wache ist, sieht nur die
     * eigene Kurve.
     */
    fun wachenkurvenLaden() {
        val kennung = kontoPruefen() ?: return
        bereich.launch {
            val kurven = runCatching {
                val eigene = wege.gemeinschaft(kennung)
                if (eigene == null) emptyList() else wege.wachenkurven(kennung, eigene.id)
            }.getOrDefault(emptyList())
            _stand.update { it.copy(wachenkurven = kurven) }
        }
    }

    /**
     * Wofür es in einer Schicht Punkte gab — beim ersten Aufklappen geholt.
     *
     * Eine Schicht ohne nachlesbare Einzelbuchungen ist kein Fehler, der die Seite
     * aufhalten sollte: Die Zeile bleibt, nur die Aufschlüsselung fehlt.
     */
    fun buchungen(code: String) {
        val kennung = kontoPruefen() ?: return
        if (_stand.value.posten.containsKey(code)) return
        bereich.launch {
            val posten = runCatching { wege.buchungen(kennung, code) }.getOrDefault(emptyList())
            _stand.update { it.copy(posten = it.posten + (code to posten)) }
        }
    }

    /** Eine archivierte Schicht öffnen — die Nachbesprechung aus dem Dienstbuch. */
    fun rundeLaden(code: String) {
        val kennung = kontoPruefen() ?: return
        val jetzt = _stand.value.runde
        if (jetzt.laedt && _stand.value.rundeCode == code) return
        if (jetzt.geladen && jetzt.fehler == null && _stand.value.rundeCode == code) return

        _stand.update { it.copy(runde = Bereich(laedt = true), rundeCode = code) }
        bereich.launch {
            runCatching { wege.archivRunde(code, kennung) }
                .onSuccess { runde ->
                    if (_stand.value.rundeCode == code) {
                        _stand.update { it.copy(runde = Bereich(runde, geladen = true)) }
                    }
                }
                .onFailure { f ->
                    if (_stand.value.rundeCode == code) {
                        _stand.update {
                            it.copy(
                                runde = Bereich(
                                    geladen = true,
                                    fehler = f.message ?: "Diese Schicht ließ sich nicht öffnen.",
                                ),
                            )
                        }
                    }
                }
        }
    }

    /**
     * Die Auswertung über alle Schichten.
     *
     * <b>Der 403 ist kein Fehler, sondern eine Auskunft:</b> „gehört zum Abo".
     * Daraus wird der Weg in den Laden, nicht „Fehler 403". Erkannt am Stand und —
     * wie im Web — am Wortlaut, falls ein Zwischenglied den Stand verschluckt.
     */
    fun auswertungLaden() {
        val kennung = kontoPruefen() ?: return
        if (_stand.value.auswertung.laedt) return
        _stand.update { it.copy(auswertung = it.auswertung.copy(laedt = true, fehler = null)) }

        bereich.launch {
            runCatching { wege.auswertung(kennung) }
                .onSuccess { a ->
                    _stand.update {
                        it.copy(auswertung = Bereich(a, geladen = true), auswertungGesperrt = false)
                    }
                }
                .onFailure { f ->
                    val gesperrt = (f as? Netzfehler)?.stand == 403 ||
                        f.message.orEmpty().contains("Premium")
                    _stand.update {
                        it.copy(
                            auswertung = Bereich(
                                geladen = true,
                                fehler = if (gesperrt) null
                                else f.message ?: "Die Auswertung ließ sich nicht laden.",
                            ),
                            auswertungGesperrt = gesperrt,
                        )
                    }
                }
        }
    }

    // ---------------------------------------------------------------- Vitrine

    /** Die Vitrine aus dem eigenen Profil — ohne Antwort bleibt sie leer. */
    fun vitrineLaden() {
        kontoPruefen() ?: return
        val k = konto() ?: return
        bereich.launch {
            runCatching { wege.vitrine(k.kennung, k.benutzername) }
                .onSuccess { profil ->
                    _stand.update { it.copy(vitrine = profil.vitrine.map { v -> v.id }) }
                }
        }
    }

    /**
     * Ein erreichtes Abzeichen in die Vitrine stellen oder herausnehmen.
     *
     * <b>Sofort auf dem Schirm, dann gespeichert</b> — kein „Übernehmen" unter
     * sechs Sternchen. Scheitert der Aufruf, kommt der alte Stand zurück und der
     * Satz des Servers steht neben der Vitrine.
     */
    fun vitrineUmschalten(abzeichen: Abzeichen) {
        val kennung = konto()?.kennung ?: return
        if (!abzeichen.erreicht) return

        val vorher = _stand.value.vitrine
        val drin = abzeichen.id in vorher

        if (!drin && vorher.size >= VITRINE_HOECHSTENS) {
            _stand.update {
                it.copy(
                    vitrinenmeldung = "In die Vitrine passen $VITRINE_HOECHSTENS Abzeichen. " +
                        "Nimm erst eines heraus.",
                )
            }
            return
        }

        val neu = if (drin) vorher - abzeichen.id else (vorher + abzeichen.id).take(VITRINE_HOECHSTENS)
        _stand.update { it.copy(vitrine = neu, vitrinenmeldung = null) }

        bereich.launch {
            runCatching { wege.vitrineSpeichern(kennung, neu) }
                .onFailure { f ->
                    _stand.update {
                        it.copy(
                            vitrine = vorher,
                            vitrinenmeldung = f.message ?: "Das ließ sich nicht speichern.",
                        )
                    }
                }
        }
    }

    // ----------------------------------------------------------------- Freunde

    /**
     * Eine Freundschaftsanfrage — aus der Mannschaftsliste der Laufbahn und der
     * Nachbesprechung. Die Seite merkt sich selbst, wen sie schon angefragt hat.
     */
    suspend fun freundAnfragen(wen: String): Result<Unit> = runCatching {
        wege.freundAnfragen(kennung(), wen)
        Unit
    }

    // -------------------------------------------------------------------- Shop

    fun aboLaden(neu: Boolean = false) {
        val kennung = kontoPruefen() ?: return
        val vorher = _stand.value.abo
        if (vorher.laedt || (!neu && vorher.geladen && vorher.fehler == null)) return
        _stand.update { it.copy(abo = vorher.copy(laedt = true, fehler = null)) }

        bereich.launch {
            runCatching { wege.premium(kennung) }
                .onSuccess { a -> _stand.update { it.copy(abo = Bereich(a, geladen = true)) } }
                .onFailure { f ->
                    _stand.update {
                        it.copy(
                            abo = it.abo.copy(
                                laedt = false,
                                geladen = true,
                                fehler = f.message ?: "Premium konnte nicht geladen werden.",
                            ),
                        )
                    }
                }
        }
    }

    /** Den frischen Shop übernehmen — und den Kontostand, der auch anderswo steht. */
    private fun shopUebernehmen(shop: Shop) {
        shopGesetzt(shop)
        creditsGesetzt(shop.credits)
    }

    /** Einen Artikel kaufen — die Bestätigung davor holt die Seite ein. */
    suspend fun artikelKaufen(artikelId: String): Result<Unit> = runCatching {
        shopUebernehmen(wege.artikelKaufen(kennung(), artikelId))
    }

    /**
     * Das Glücksrad drehen. Der Gewinn steht fest, bevor das Rad sich dreht — der
     * Server entscheidet, die Seite zeigt nur, wo er liegt.
     */
    suspend fun radDrehen(): Result<Gluecksradergebnis> = runCatching {
        val ergebnis = wege.tagesbonus(kennung())
        shopUebernehmen(ergebnis.shop)
        if (ergebnis.gewinn.art == "Fahrzeuggutschein") garageNeu()
        ergebnis
    }

    /**
     * Einen Aktionscode einlösen. Ein Gutschein-Code ändert die offenen
     * Fahrzeugwahlen — die stehen nicht im Shop, also wird die Garage mitgezogen.
     */
    suspend fun codeEinloesen(code: String): Result<Codegutschrift> = runCatching {
        val ertrag = wege.codeEinloesen(kennung(), code.trim())
        shopUebernehmen(ertrag.shop)
        if (ertrag.wahlen > 0) garageNeu()
        ertrag
    }

    suspend fun beschenkbare(): Result<List<Beschenkbarer>> = runCatching {
        wege.beschenkbare(kennung())
    }

    /** Verschenken — bezahlt aus dem eigenen Guthaben, das jetzt kleiner ist. */
    suspend fun schenken(an: String, artikelId: String): Result<Unit> = runCatching {
        shopUebernehmen(wege.schenken(kennung(), an, artikelId))
    }

    /** Einen Fahrzeuggutschein einlösen. */
    suspend fun fahrzeugWaehlen(vorlage: Fahrzeugvorlage): Result<Unit> = runCatching {
        val garage = wege.fahrzeugWaehlen(kennung(), vorlage.id)
        creditsGesetzt(garage.credits)
        garageNeu()
    }

    /** Ein Fahrzeug gegen Credits kaufen — der Gutschein bleibt dabei stehen. */
    suspend fun fahrzeugKaufen(vorlage: Fahrzeugvorlage): Result<Unit> = runCatching {
        val garage = wege.fahrzeugKaufen(kennung(), vorlage.id)
        creditsGesetzt(garage.credits)
        garageNeu()
    }

    /** Die Kasse öffnen — heraus kommt die Adresse der Stripe-Sitzung. */
    suspend fun premiumKasse(plan: String, sofortAusfuehren: Boolean): Result<String> = runCatching {
        val url = wege.premiumKasse(kennung(), plan, sofortAusfuehren).url
        if (url.isBlank()) throw Netzfehler("Die Kasse ließ sich nicht öffnen.")
        url
    }

    /**
     * Die Rückkehr aus der Kasse abschließen — für den, der `session_id` aus der
     * Rückkehradresse in die App leitet.
     */
    suspend fun premiumAbschliessen(sitzung: String): Result<Abostand> = runCatching {
        val abo = wege.premiumAbschliessen(kennung(), sitzung)
        _stand.update { it.copy(abo = Bereich(abo, geladen = true)) }
        abo
    }

    companion object {
        /** Wie viele Abzeichen in der Vitrine stehen dürfen. */
        const val VITRINE_HOECHSTENS = 6
    }
}

/**
 * Was die Dienstbuch-Seiten zeigen.
 *
 * `rundeCode` steht neben `runde`, damit eine späte Antwort für eine Schicht, die
 * man längst wieder verlassen hat, nicht über die neue gelegt wird.
 */
data class Dienstbuchstand(
    val buch: Bereich<Dienstbuchdaten> = Bereich(),
    /** Einzelbuchungen je Raumcode — erst geholt, wenn jemand sie aufklappt. */
    val posten: Map<String, List<Buchungsposten>> = emptyMap(),
    val wachenkurven: List<Wachenkurve> = emptyList(),
    val vitrine: List<String> = emptyList(),
    val vitrinenmeldung: String? = null,
    val auswertung: Bereich<Dienstauswertung> = Bereich(),
    /** Ohne Abo: statt der Zahlen das Angebot. */
    val auswertungGesperrt: Boolean = false,
    val runde: Bereich<ArchivRunde> = Bereich(),
    val rundeCode: String? = null,
    val abo: Bereich<Abostand> = Bereich(),
)

/** Der geteilte Bestand der sechs Seiten. */
data class Dienstbuchdaten(
    val schichten: List<Schichtzeile> = emptyList(),
    val statistik: ArchivStatistik? = null,
    val laufbahn: List<Laufbahnstufe> = emptyList(),
    val bestenliste: List<Bestenlistenplatz> = emptyList(),
    val saison: List<Saisonbestenlistenplatz> = emptyList(),
    val tagesschicht: Tagesschicht? = null,
    val abzeichen: List<Abzeichen> = emptyList(),
    val freischaltungen: List<Kontofreischaltung> = emptyList(),
) {
    val einsaetzeGesamt: Int get() = schichten.sumOf { it.einsaetze }
    val punkteGesamt: Int get() = schichten.sumOf { it.punkte ?: 0 }

    /** Die eigene Zeile der Bestenliste; `null`, wenn man nicht unter den Ersten steht. */
    val eigenerPlatz: Bestenlistenplatz? get() = bestenliste.firstOrNull { it.istEigenerEintrag }
}

/**
 * Eine Zeile je gefahrener Schicht — Archivdaten, um die eigene Ausbeute ergänzt.
 *
 * <b>Grundlage ist das Archiv, nicht die Buchung:</b> Wer eine Runde vor seinem
 * Konto gefahren hat, findet sie trotzdem, nur eben ohne Punkte (`punkte == null`).
 */
data class Schichtzeile(
    val code: String,
    val beendetUm: String?,
    val ort: String,
    val landkreis: String?,
    val einsaetze: Int,
    val hilfsfristSekunden: Double?,
    /** Erst gefüllt, wenn für diese Schicht auch gebucht wurde. */
    val rolle: String?,
    val funkrufname: String?,
    /** Womit man gefahren ist — daraus folgt die Organisation. */
    val fahrzeugtyp: String?,
    val punkte: Int?,
)

/**
 * Die Organisation einer Schicht — abgeleitet, nicht gespeichert.
 *
 * Die Chronik führt Rolle und Fahrzeugtyp, aber keine Organisation; der Typ steht
 * im Katalog, und dort steht die Organisation daneben. Leitstellendienst ist eine
 * eigene Antwort und kein fehlender Wert.
 */
fun schichtOrganisation(schicht: Schichtzeile, fahrzeuge: List<Fahrzeugvorlage>): String? {
    if (schicht.rolle == "Leitstelle") return "Leitstelle"
    val typ = schicht.fahrzeugtyp ?: return null
    return fahrzeuge.firstOrNull { it.typ == typ }?.organisation
}
