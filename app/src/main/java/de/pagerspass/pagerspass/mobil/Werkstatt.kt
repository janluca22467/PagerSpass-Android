package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Abzeichen
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Archivrunde
import de.pagerspass.pagerspass.netz.Archivstatistik
import de.pagerspass.pagerspass.netz.Bestenlistenplatz
import de.pagerspass.pagerspass.netz.Dienstauswertung
import de.pagerspass.pagerspass.netz.Erfahrungsposten
import de.pagerspass.pagerspass.netz.Footerknopf
import de.pagerspass.pagerspass.netz.Freischaltung
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kreiswache
import de.pagerspass.pagerspass.netz.Lehrgang
import de.pagerspass.pagerspass.netz.Lehrgangsmodul
import de.pagerspass.pagerspass.netz.Leitstellenvorlage
import de.pagerspass.pagerspass.netz.Mitgliedshilfsfrist
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Netzfehler
import de.pagerspass.pagerspass.netz.OffeneUmfrage
import de.pagerspass.pagerspass.netz.Rang
import de.pagerspass.pagerspass.netz.Rundenvorlagenzeile
import de.pagerspass.pagerspass.netz.Saisonplatz
import de.pagerspass.pagerspass.netz.Schichtzeile
import de.pagerspass.pagerspass.netz.Startkachel
import de.pagerspass.pagerspass.netz.Szenario
import de.pagerspass.pagerspass.netz.Szenariozeile
import de.pagerspass.pagerspass.netz.Tagesschicht
import de.pagerspass.pagerspass.netz.Theorieurteil
import de.pagerspass.pagerspass.netz.einrichtungsauszug
import de.pagerspass.pagerspass.netz.theorieAbgeben
import de.pagerspass.pagerspass.netz.Uebungsfahrt
import de.pagerspass.pagerspass.netz.Vorlageninhalt
import de.pagerspass.pagerspass.netz.Vorlagenzeile
import de.pagerspass.pagerspass.netz.Werkwege
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Die Werkstatt — alles, was man zwischen zwei Schichten nachliest oder baut.
 *
 * Das Gegenstück zu `stores/dienstbuch.ts` und zu dem Zustand, den im Web
 * `LehrgangView`, `UebungenView`, `LeitstellenbauView` und die
 * Rundenvorlagen-Dialoge jeweils selbst halten.
 *
 * <b>Ein eigenes ViewModel und nicht die Sitzung.</b> Die Sitzung trägt die
 * Anmeldung und die sechs Wege der Leiste; das hier sind Unterseiten mit
 * Entwürfen, die ein Drehen des Geräts überleben müssen — ein halb
 * geschriebener Übungsablauf, der beim Drehen verschwindet, ist verlorene
 * Arbeit. Im ViewModel überlebt er; in einem `remember` nicht.
 *
 * <b>Die Runde eröffnet die Werkstatt nicht selbst.</b> Sie holt den Raumcode
 * und gibt ihn zurück (`beiCode`); betreten wird der Raum vom Rahmen, der die
 * Runde kennt. Genau wie im Web, wo die Ansicht den Code bekommt und dann nach
 * `/raum/{code}` springt.
 */
class Werkstatt(anwendung: Application) : AndroidViewModel(anwendung) {

    private val netz = Netz(Ablage(anwendung))
    private val wege = Werkwege(netz)

    private val _stand = MutableStateFlow(Werkstand())
    val stand: StateFlow<Werkstand> = _stand.asStateFlow()

    private var konto: Konto? = null

    /**
     * Wer gerade angemeldet ist. Ein Kontowechsel wirft alles weg — dieselbe
     * Regel wie `leeren()` im Dienstbuch-Speicher des Webs.
     */
    fun kontoSetzen(neu: Konto?) {
        val vorher = konto
        konto = neu
        if (vorher?.kennung != neu?.kennung) _stand.value = Werkstand()
    }

    private fun kennung(): String =
        konto?.kennung ?: throw IllegalStateException("Kein Konto angemeldet.")

    fun meldungWegnehmen() = _stand.update { it.copy(meldung = null, fehler = null) }

    // ------------------------------------------------------------ Dienstbuch

    /**
     * Das Dienstbuch in einem Zug.
     *
     * <b>Archiv, Chronik und Statistik tragen, der Rest ist Beiwerk.</b> Fällt
     * die Laufbahn oder die Bestenliste aus, bleibt das Buch lesbar — die
     * eigenen Schichten sind das, weswegen man herkommt (so steht es auch im
     * Web-Speicher).
     */
    fun buchLaden(neu: Boolean = false) = laden(
        holen = { _stand.value.buch },
        setzen = { b -> _stand.update { it.copy(buch = b) } },
        nurWennNoetig = !neu,
    ) {
        val k = kennung()
        coroutineScope {
            val archiv = async { wege.archiv(k) }
            val chronik = async { wege.chronik(k) }
            val statistik = async { runCatching { wege.archivstatistik(k, 10) }.getOrNull() }
            val laufbahn = async { runCatching { wege.laufbahn() }.getOrDefault(emptyList()) }
            val beste = async { runCatching { wege.bestenliste(k) }.getOrDefault(emptyList()) }
            val saison = async { runCatching { wege.saisonbestenliste(k) }.getOrDefault(emptyList()) }
            val tages = async { runCatching { wege.tagesschicht(k) }.getOrNull() }
            val abzeichen = async { runCatching { wege.abzeichen(k) }.getOrDefault(emptyList()) }
            val frei = async { runCatching { wege.freischaltungen(k) }.getOrDefault(emptyList()) }
            val garage = async { runCatching { wege.garagenauszug(k) }.getOrNull() }

            val nachCode = chronik.await().associateBy { it.roomCode }
            Dienstbuchdaten(
                schichten = archiv.await().map { a ->
                    val eigen = nachCode[a.code]
                    Schichtzeile(
                        code = a.code,
                        beendetUm = a.beendetUm,
                        ort = a.landkreis ?: a.ort,
                        landkreis = a.landkreis,
                        einsaetze = eigen?.einsaetze ?: a.einsaetze,
                        hilfsfristSekunden = eigen?.hilfsfristSekunden,
                        rolle = eigen?.rolle?.ifBlank { null },
                        funkrufname = eigen?.funkrufname,
                        fahrzeugtyp = eigen?.fahrzeugtyp,
                        punkte = eigen?.punkte,
                    )
                },
                statistik = statistik.await(),
                laufbahn = laufbahn.await(),
                bestenliste = beste.await(),
                saison = saison.await(),
                tagesschicht = tages.await(),
                abzeichen = abzeichen.await(),
                freischaltungen = frei.await(),
                garage = garage.await(),
            )
        }
    }.also { wachenkurvenLaden() }

    /** Die Kurven der Wachenmitglieder — still, wer in keiner Wache ist, hat keine. */
    private fun wachenkurvenLaden() = viewModelScope.launch {
        val k = konto?.kennung ?: return@launch
        if (_stand.value.mitglieder.isNotEmpty()) return@launch
        val liste = runCatching {
            wege.eigeneGemeinschaft(k)?.let { wege.hilfsfristen(k, it.id) }.orEmpty()
        }.getOrDefault(emptyList())
        _stand.update { it.copy(mitglieder = liste) }
    }

    /** Wofür es in einer Schicht Punkte gab — beim ersten Aufklappen geholt. */
    fun buchungenLaden(code: String) = viewModelScope.launch {
        if (_stand.value.posten.containsKey(code)) return@launch
        val k = konto?.kennung ?: return@launch
        // Eine Schicht ohne nachlesbare Einzelbuchungen ist kein Fehler, der die
        // Seite aufhalten sollte — die Zeile bleibt, nur die Aufschlüsselung fehlt.
        val geholt = runCatching { wege.buchungen(k, code) }.getOrDefault(emptyList())
        _stand.update { it.copy(posten = it.posten + (code to geholt)) }
    }

    /** Eine vergangene Schicht vollständig — für die Nachbesprechung aus dem Archiv. */
    fun archivrundeLaden(code: String) {
        if (_stand.value.archivrunde.inhalt?.code == code) return
        _stand.update { it.copy(archivrunde = Bereich()) }
        laden(
            holen = { _stand.value.archivrunde },
            setzen = { b -> _stand.update { it.copy(archivrunde = b) } },
            nurWennNoetig = false,
        ) { wege.archivrunde(code, kennung()) }
        buchungenLaden(code)
    }

    /**
     * Die Auswertung — ohne Abo antwortet der Server mit 403.
     *
     * <b>Daraus wird kein Fehler, sondern das Angebot.</b> Wer nicht weiß, was
     * er bekäme, kauft es auch nicht; deshalb steht dann eine Erklärung da und
     * ein Weg zur Webseite, wo das Abo abgeschlossen wird.
     */
    fun auswertungLaden() = viewModelScope.launch {
        if (_stand.value.auswertung.laedt || _stand.value.auswertung.geladen) return@launch
        _stand.update { it.copy(auswertung = Bereich(laedt = true)) }
        runCatching { wege.dienstauswertung(kennung()) }
            .onSuccess { a -> _stand.update { it.copy(auswertung = Bereich(a, geladen = true)) } }
            .onFailure { f ->
                val gesperrt = (f as? Netzfehler)?.stand == 403 ||
                    f.message.orEmpty().contains("Premium")
                _stand.update {
                    it.copy(
                        auswertung = Bereich(
                            geladen = true,
                            fehler = if (gesperrt) null else f.message ?: "Die Auswertung ließ sich nicht laden.",
                        ),
                        auswertungGesperrt = gesperrt,
                    )
                }
            }
    }

    /** Die Vitrine — welche Abzeichen im Profil ausgestellt sind. */
    fun vitrineLaden() = viewModelScope.launch {
        val k = konto ?: return@launch
        if (k.benutzername.isBlank()) return@launch
        runCatching { wege.vitrine(k.kennung, k.benutzername) }
            .onSuccess { ids -> _stand.update { it.copy(vitrine = ids) } }
    }

    /**
     * Ein Abzeichen in die Vitrine oder heraus.
     *
     * Erst gesetzt, dann gespeichert, und bei einem Fehler zurückgenommen — der
     * Stern soll unter dem Finger umspringen, nicht nach einer Sekunde.
     */
    fun vitrineUmschalten(abzeichen: Abzeichen) = viewModelScope.launch {
        val k = konto?.kennung ?: return@launch
        if (!abzeichen.erreicht) return@launch
        val vorher = _stand.value.vitrine
        val drin = abzeichen.id in vorher
        if (!drin && vorher.size >= VITRINE_HOECHSTENS) {
            _stand.update {
                it.copy(
                    vitrinenmeldung = "In die Vitrine passen $VITRINE_HOECHSTENS Abzeichen. " +
                        "Nimm erst eines heraus.",
                )
            }
            return@launch
        }
        val neu = if (drin) vorher - abzeichen.id else vorher + abzeichen.id
        _stand.update { it.copy(vitrine = neu, vitrinenmeldung = null) }
        runCatching { wege.vitrineSetzen(k, neu) }.onFailure { f ->
            _stand.update {
                it.copy(vitrine = vorher, vitrinenmeldung = f.message ?: "Das ließ sich nicht speichern.")
            }
        }
    }

    // --------------------------------------------------------------- Lehrgang

    fun lehrgaengeLaden(neu: Boolean = false) {
        laden(
            holen = { _stand.value.lehrgaenge },
            setzen = { b -> _stand.update { it.copy(lehrgaenge = b) } },
            nurWennNoetig = !neu,
        ) { wege.lehrgaenge(kennung()) }
        // Die Spielrolle aus dem Einrichtungsbogen entscheidet, welcher Lehrgang
        // „Dein Einstieg" heißt. Fehlt sie (älterer Server, Bogen offen), gilt die
        // Leitstelle — wie im Web.
        if (_stand.value.spielrolle == null) viewModelScope.launch {
            val k = konto?.kennung ?: return@launch
            val rolle = runCatching { netz.einrichtungsauszug(k).spielrolle }.getOrNull()
            _stand.update { it.copy(spielrolle = rolle ?: "") }
        }
    }

    /**
     * Eine Übung ist durch — jede Karte saß einmal. Abgehakt wird wie Lesestoff;
     * ein Fehlschlag steht als Satz da, statt still zu verschwinden.
     */
    fun uebungGeschafft(lehrgang: Lehrgang, modul: Lehrgangsmodul) = viewModelScope.launch {
        runCatching { wege.modulErledigen(lehrgang.id, modul.id, kennung()) }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message ?: "Die Übung ließ sich nicht abhaken.") } }
        lehrgaengeLaden(neu = true)
    }

    /**
     * Gibt eine Wissensprüfung ab. Ausgewertet wird auf dem Server, der die
     * richtigen Antworten als einziger kennt; das Urteil geht an die Seite zurück.
     */
    fun theorieAbgeben(
        lehrgang: Lehrgang,
        modul: Lehrgangsmodul,
        antworten: Map<String, Int>,
        beiUrteil: (Theorieurteil) -> Unit,
    ) = viewModelScope.launch {
        if (_stand.value.laeuft) return@launch
        _stand.update { it.copy(laeuft = true, fehler = null) }
        runCatching { netz.theorieAbgeben(lehrgang.id, modul.id, kennung(), antworten) }
            .onSuccess { beiUrteil(it) }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message ?: "Die Prüfung ließ sich nicht abgeben.") } }
        _stand.update { it.copy(laeuft = false) }
        lehrgaengeLaden(neu = true)
    }

    /** Lesestoff abhaken — nachdem die Wikiseite geöffnet ist, so wie im Web. */
    fun modulGelesen(lehrgang: Lehrgang, modul: Lehrgangsmodul) = viewModelScope.launch {
        runCatching { wege.modulErledigen(lehrgang.id, modul.id, kennung()) }
        lehrgaengeLaden(neu = true)
    }

    fun pruefungFahren(lehrgang: Lehrgang, modul: Lehrgangsmodul, beiCode: (String) -> Unit) =
        raumHolen(beiCode) { wege.pruefungAnlegen(lehrgang.id, modul.id, kennung()) }

    // ---------------------------------------------------------------- Übungen

    fun szenarienLaden(neu: Boolean = false) {
        laden(
            holen = { _stand.value.szenarien },
            setzen = { b -> _stand.update { it.copy(szenarien = b) } },
            nurWennNoetig = !neu,
        ) { wege.szenarien(kennung()) }
        viewModelScope.launch {
            val k = konto?.kennung ?: return@launch
            val verlauf = runCatching { wege.uebungsverlauf(k) }.getOrDefault(emptyList())
            _stand.update { it.copy(uebungsverlauf = verlauf) }
        }
    }

    /** Eine Übung in den Editor holen — oder, mit `null`, einen frischen Entwurf. */
    fun uebungOeffnen(id: String?, neu: () -> Szenario) {
        if (id == null) {
            _stand.update { it.copy(uebung = Bereich(neu(), geladen = true), meldung = null, fehler = null) }
            return
        }
        if (_stand.value.uebung.inhalt?.id == id) return
        _stand.update { it.copy(uebung = Bereich(laedt = true), meldung = null, fehler = null) }
        laden(
            holen = { _stand.value.uebung },
            setzen = { b -> _stand.update { it.copy(uebung = b) } },
            nurWennNoetig = false,
        ) { wege.szenario(id, kennung()) }
    }

    /** Der Editor schreibt seinen Entwurf hierher — so überlebt er das Drehen. */
    fun uebungAendern(neu: Szenario) =
        _stand.update { it.copy(uebung = it.uebung.copy(inhalt = neu)) }

    fun uebungSichern() = arbeiten {
        val entwurf = _stand.value.uebung.inhalt ?: return@arbeiten
        val gespeichert = wege.szenarioSichern(kennung(), entwurf)
        _stand.update {
            it.copy(
                uebung = Bereich(gespeichert, geladen = true),
                meldung = "Gespeichert. Code zum Weitergeben: ${gespeichert.code}",
            )
        }
        szenarienLaden(neu = true)
    }

    fun uebungLoeschen(id: String) = arbeiten {
        wege.szenarioLoeschen(id, kennung())
        if (_stand.value.uebung.inhalt?.id == id) _stand.update { it.copy(uebung = Bereich()) }
        szenarienLaden(neu = true)
    }

    fun uebungUebernehmen(code: String) = arbeiten {
        val kopie = wege.szenarioUebernehmen(code.trim().uppercase(), kennung())
        _stand.update { it.copy(meldung = "„${kopie.name}\" liegt jetzt in deiner Liste.") }
        szenarienLaden(neu = true)
    }

    /** Eine Runde, die diese Übung fährt — selbst fahren oder leiten, das entscheidet der Aufrufer. */
    fun uebungRunde(id: String, beiCode: (String) -> Unit) =
        raumHolen(beiCode) { wege.szenarioRunde(id, kennung()) }

    // --------------------------------------------------------- Leitstellenbau

    fun vorlagenLaden(neu: Boolean = false) {
        laden(
            holen = { _stand.value.vorlagen },
            setzen = { b -> _stand.update { it.copy(vorlagen = b) } },
            nurWennNoetig = !neu,
        ) { wege.vorlagen(kennung()) }
        freischaltungenLaden()
    }

    private fun freischaltungenLaden() = viewModelScope.launch {
        val k = konto?.kennung ?: return@launch
        runCatching { wege.freischaltungen(k) }
            .onSuccess { f -> _stand.update { it.copy(freischaltungen = f) } }
    }

    fun leitstelleOeffnen(id: String?, neu: () -> Leitstellenvorlage) {
        if (id == null) {
            val entwurf = neu()
            _stand.update {
                it.copy(leitstelle = Bereich(entwurf, geladen = true), meldung = null, fehler = null)
            }
            kreiswachenLaden(entwurf.landkreisId)
            return
        }
        if (_stand.value.leitstelle.inhalt?.id == id) return
        _stand.update { it.copy(leitstelle = Bereich(laedt = true), meldung = null, fehler = null) }
        viewModelScope.launch {
            runCatching { wege.vorlage(id, kennung()) }
                .onSuccess { v ->
                    _stand.update { it.copy(leitstelle = Bereich(v, geladen = true)) }
                    kreiswachenLaden(v.landkreisId)
                }
                .onFailure { f ->
                    _stand.update {
                        it.copy(leitstelle = Bereich(geladen = true, fehler = f.message))
                    }
                }
        }
    }

    fun leitstelleAendern(neu: Leitstellenvorlage) =
        _stand.update { it.copy(leitstelle = it.leitstelle.copy(inhalt = neu)) }

    /** Die echten Wachen des Kreises — leer, wo keine Geodaten vorliegen. */
    fun kreiswachenLaden(landkreisId: String?) = viewModelScope.launch {
        if (landkreisId == null) {
            _stand.update { it.copy(kreiswachen = Bereich(emptyList(), geladen = true)) }
            return@launch
        }
        _stand.update { it.copy(kreiswachen = Bereich(laedt = true)) }
        runCatching { wege.kreiswachen(landkreisId) }
            .onSuccess { l -> _stand.update { it.copy(kreiswachen = Bereich(l, geladen = true)) } }
            .onFailure { f ->
                _stand.update { it.copy(kreiswachen = Bereich(emptyList(), geladen = true, fehler = f.message)) }
            }
    }

    fun leitstelleSichern() = arbeiten {
        val entwurf = _stand.value.leitstelle.inhalt ?: return@arbeiten
        val gespeichert = wege.vorlageSichern(kennung(), entwurf)
        _stand.update {
            it.copy(
                leitstelle = Bereich(gespeichert, geladen = true),
                meldung = "Gespeichert. Code zum Weitergeben: ${gespeichert.code}",
            )
        }
        vorlagenLaden(neu = true)
    }

    fun leitstelleLoeschen(id: String) = arbeiten {
        wege.vorlageLoeschen(id, kennung())
        vorlagenLaden(neu = true)
    }

    fun leitstelleUebernehmen(code: String) = arbeiten {
        val kopie = wege.vorlageUebernehmen(code.trim().uppercase(), kennung())
        _stand.update {
            it.copy(
                meldung = "„${kopie.name}\" liegt jetzt bei dir — als eigene Kopie mit dem Code ${kopie.code}.",
            )
        }
        vorlagenLaden(neu = true)
    }

    fun sandkastenEroeffnen(id: String, beiCode: (String) -> Unit) =
        raumHolen(beiCode) { wege.sandkasten(id, kennung()) }

    // --------------------------------------------------------- Rundenvorlagen

    fun rundenvorlagenLaden(neu: Boolean = false) = laden(
        holen = { _stand.value.rundenvorlagen },
        setzen = { b -> _stand.update { it.copy(rundenvorlagen = b) } },
        nurWennNoetig = !neu,
    ) { wege.rundenvorlagen(kennung()) }

    fun rundenvorlageStarten(v: Rundenvorlagenzeile, beiCode: (String) -> Unit) =
        raumHolen(beiCode) { wege.rundenvorlageStarten(v.id, kennung()) }

    fun rundenvorlageLoeschen(v: Rundenvorlagenzeile) = arbeiten {
        wege.rundenvorlageLoeschen(v.id, kennung())
        _stand.update {
            it.copy(
                rundenvorlagen = it.rundenvorlagen.copy(
                    inhalt = it.rundenvorlagen.inhalt.orEmpty().filter { z -> z.id != v.id },
                ),
            )
        }
    }

    fun rundenvorlageUebernehmen(code: String) = arbeiten {
        val kopie = wege.rundenvorlageUebernehmen(code.trim().uppercase(), kennung())
        _stand.update { it.copy(meldung = "„${kopie.name}\" liegt jetzt bei deinen Vorlagen.") }
        rundenvorlagenLaden(neu = true)
    }

    /** Den Inhalt einer Vorlage in den Editor holen — und die Wachen ihres Kreises. */
    fun vorlageninhaltLaden(id: String) = viewModelScope.launch {
        _stand.update {
            it.copy(vorlageninhalt = Bereich(laedt = true), inhaltswachen = emptyList(), fehler = null)
        }
        val inhalt = runCatching { wege.vorlageninhalt(id, kennung()) }
        inhalt.onFailure {
            _stand.update {
                it.copy(vorlageninhalt = Bereich(geladen = true, fehler = "Die Vorlage ließ sich nicht laden."))
            }
        }
        val geholt = inhalt.getOrNull() ?: return@launch
        _stand.update { it.copy(vorlageninhalt = Bereich(geholt, geladen = true)) }
        val kreis = geholt.landkreisId ?: return@launch
        runCatching { wege.kreiswachen(kreis) }
            .onSuccess { w -> _stand.update { it.copy(inhaltswachen = w) } }
            .onFailure {
                _stand.update { it.copy(fehler = "Die Wachen des Kreises ließen sich nicht laden.") }
            }
    }

    fun vorlageninhaltAendern(neu: Vorlageninhalt) =
        _stand.update { it.copy(vorlageninhalt = it.vorlageninhalt.copy(inhalt = neu)) }

    fun vorlageninhaltSichern(id: String, fertig: () -> Unit) = arbeiten {
        val inhalt = _stand.value.vorlageninhalt.inhalt ?: return@arbeiten
        val zeile = wege.vorlageninhaltSichern(id, kennung(), inhalt.copy(name = inhalt.name.trim()))
        _stand.update {
            it.copy(
                rundenvorlagen = it.rundenvorlagen.copy(
                    inhalt = it.rundenvorlagen.inhalt.orEmpty().map { z -> if (z.id == zeile.id) zeile else z },
                ),
            )
        }
        fertig()
    }

    // -------------------------------------------------------- Startbildschirm

    /**
     * Kacheln, Fußzeile, Umfrage — alles Beiwerk. Scheitert ein Abruf, bleibt
     * der Startbildschirm, was er ohne ihn war; eine Fehlermeldung hätte dort
     * keinen Adressaten.
     */
    fun startLaden() = viewModelScope.launch {
        runCatching { wege.startkacheln() }.onSuccess { l -> _stand.update { it.copy(startkacheln = l) } }
        runCatching { wege.footer() }.onSuccess { l -> _stand.update { it.copy(footer = l) } }
        val k = konto?.kennung
        if (k != null) {
            runCatching { wege.umfrage(k) }.onSuccess { u ->
                _stand.update { if (it.umfrageDank) it else it.copy(umfrage = u) }
            }
        }
        if (k != null && !_stand.value.rundenvorlagen.geladen) rundenvorlagenLaden()
    }

    fun umfrageAntworten(option: Int) = viewModelScope.launch {
        val k = konto?.kennung ?: return@launch
        val u = _stand.value.umfrage ?: return@launch
        runCatching { wege.umfrageAntworten(k, u.id, option) }
            .onSuccess { _stand.update { it.copy(umfrageDank = true) } }
            // Auch „schon geantwortet" heißt: Karte weg — nachzuholen gibt es nichts.
            .onFailure { _stand.update { it.copy(umfrage = null) } }
    }

    // ------------------------------------------------------------- Mäntel

    private fun raumHolen(beiCode: (String) -> Unit, tun: suspend () -> String) = viewModelScope.launch {
        if (_stand.value.laeuft) return@launch
        _stand.update { it.copy(laeuft = true, fehler = null) }
        runCatching { tun() }
            .onSuccess { code -> beiCode(code) }
            .onFailure { f ->
                _stand.update { it.copy(fehler = f.message ?: "Die Runde ließ sich nicht eröffnen.") }
            }
        _stand.update { it.copy(laeuft = false) }
    }

    private fun arbeiten(tun: suspend () -> Unit) = viewModelScope.launch {
        _stand.update { it.copy(laeuft = true, fehler = null, meldung = null) }
        runCatching { tun() }.onFailure { f ->
            _stand.update { it.copy(fehler = f.message ?: "Das hat nicht geklappt.") }
        }
        _stand.update { it.copy(laeuft = false) }
    }

    /** Derselbe Mantel wie `Sitzung.laden` — Ladezustand, Fehler als Satz, `geladen` merken. */
    private fun <T> laden(
        holen: () -> Bereich<T>,
        setzen: (Bereich<T>) -> Unit,
        nurWennNoetig: Boolean,
        tun: suspend () -> T,
    ) = viewModelScope.launch {
        val vorher = holen()
        if (vorher.laedt) return@launch
        if (nurWennNoetig && vorher.geladen && vorher.fehler == null) return@launch
        if (konto == null) return@launch

        setzen(vorher.copy(laedt = true, fehler = null))
        runCatching { tun() }
            .onSuccess { setzen(Bereich(inhalt = it, laedt = false, geladen = true)) }
            .onFailure { f ->
                setzen(
                    holen().copy(
                        laedt = false,
                        geladen = true,
                        fehler = f.message ?: "Konnte nicht geladen werden.",
                    ),
                )
            }
    }

    companion object {
        /** So viele Abzeichen fasst die Vitrine im Profil — wie im Web. */
        const val VITRINE_HOECHSTENS = 6
    }
}

/** Was das Dienstbuch zeigt — einmal geholt, über alle Reiter hinweg gehalten. */
data class Dienstbuchdaten(
    val schichten: List<Schichtzeile> = emptyList(),
    val statistik: Archivstatistik? = null,
    val laufbahn: List<Rang> = emptyList(),
    val bestenliste: List<Bestenlistenplatz> = emptyList(),
    val saison: List<Saisonplatz> = emptyList(),
    val tagesschicht: Tagesschicht? = null,
    val abzeichen: List<Abzeichen> = emptyList(),
    val freischaltungen: List<Freischaltung> = emptyList(),
    val garage: de.pagerspass.pagerspass.netz.Garagenauszug? = null,
)

/** Der Stand der Werkstatt — je Bereich ein eigener Ladezustand. */
data class Werkstand(
    val buch: Bereich<Dienstbuchdaten> = Bereich(),
    /** Die Hilfsfristkurven der eigenen Wache — leer, wer in keiner ist. */
    val mitglieder: List<Mitgliedshilfsfrist> = emptyList(),
    /** Einzelbuchungen je Raumcode — erst geholt, wenn jemand sie aufklappt. */
    val posten: Map<String, List<Erfahrungsposten>> = emptyMap(),
    val archivrunde: Bereich<Archivrunde?> = Bereich(),
    val auswertung: Bereich<Dienstauswertung?> = Bereich(),
    val auswertungGesperrt: Boolean = false,
    val vitrine: List<String> = emptyList(),
    val vitrinenmeldung: String? = null,
    val lehrgaenge: Bereich<List<Lehrgang>> = Bereich(),
    /** Die Spielrolle aus dem Einrichtungsbogen — `null` ungeholt, leer unbekannt. */
    val spielrolle: String? = null,
    val szenarien: Bereich<List<Szenariozeile>> = Bereich(),
    val uebungsverlauf: List<Uebungsfahrt> = emptyList(),
    /** Der Entwurf im Übungseditor. */
    val uebung: Bereich<Szenario?> = Bereich(),
    val vorlagen: Bereich<List<Vorlagenzeile>> = Bereich(),
    val freischaltungen: List<Freischaltung> = emptyList(),
    /** Der Entwurf im Leitstelleneditor. */
    val leitstelle: Bereich<Leitstellenvorlage?> = Bereich(),
    val kreiswachen: Bereich<List<Kreiswache>> = Bereich(),
    val rundenvorlagen: Bereich<List<Rundenvorlagenzeile>> = Bereich(),
    val vorlageninhalt: Bereich<Vorlageninhalt?> = Bereich(),
    val inhaltswachen: List<Kreiswache> = emptyList(),
    val startkacheln: List<Startkachel> = emptyList(),
    val footer: List<Footerknopf> = emptyList(),
    val umfrage: OffeneUmfrage? = null,
    val umfrageDank: Boolean = false,
    /** Ob gerade gespeichert, gelöscht oder eröffnet wird. */
    val laeuft: Boolean = false,
    /** Die Rückmeldung nach einer Aktion — „Gespeichert. Code …". */
    val meldung: String? = null,
    val fehler: String? = null,
) {
    /** Ob das Konto eine Zusatzfunktion schon hat — `Sandkasten`, `EigeneAao`, … */
    fun frei(was: String): Boolean = freischaltungen.firstOrNull { it.was == was }?.offen ?: true
    fun abRang(was: String): String = freischaltungen.firstOrNull { it.was == was }?.abRang.orEmpty()
}
