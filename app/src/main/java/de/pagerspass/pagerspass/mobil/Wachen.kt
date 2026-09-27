package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.GemeinschaftDetail
import de.pagerspass.pagerspass.netz.Gemeinschaftseinstellungen
import de.pagerspass.pagerspass.netz.Gemeinschaftsfilter
import de.pagerspass.pagerspass.netz.Landkreisvorschlag
import de.pagerspass.pagerspass.netz.Wachenplatz
import de.pagerspass.pagerspass.netz.Wachenrang
import de.pagerspass.pagerspass.netz.Wachensuchtreffer
import de.pagerspass.pagerspass.netz.Wachenwege
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Der Speicher der Wachengemeinschaft — das Gegenstück zu
 * `web/src/stores/gemeinschaften.ts`.
 *
 * <b>Er schreibt in denselben `Seitenstand` wie die Sitzung</b> (`wache`,
 * `wacheDetail`, `wachenantraege`): Die Tableiste liest ihre Marke daraus, und
 * eine zweite Ablage für dieselbe Gemeinschaft wäre eine zweite Wahrheit. Was
 * nur die Wachenseiten brauchen — Suche, Rangliste, Laufbahn, die Meldung unter
 * dem Kopf —, steht in seinem eigenen `Wachenstand`.
 *
 * <b>Geprüft wird hier nichts.</b> Rollen, Beitrittsregeln und Grenzen
 * entscheidet der Server; was er ablehnt, steht in `meldung` — an der Stelle
 * der Seite, an der es passiert ist, und nicht als Zeile am Fuß des
 * Bildschirms, die zu einem anderen Knopf gehören könnte.
 *
 * <b>Viele Wege antworten schon mit dem vollen neuen Stand</b> (Beförderung,
 * Aushang, Kauf). Der wird übernommen statt gleich wieder abgeholt — ein Klick
 * auf „Befördern" kostete sonst drei Anfragen statt einer.
 */
class Wachenspeicher(
    private val wege: Wachenwege,
    private val umfang: CoroutineScope,
    private val kennung: () -> String?,
    private val daten: MutableStateFlow<Seitenstand>,
    /** Holt das eigene Konto neu — Credits und Wachentag stehen auch dort. */
    private val kontoAuffrischen: () -> Unit,
) {
    private val _stand = MutableStateFlow(Wachenstand())
    val stand: StateFlow<Wachenstand> = _stand.asStateFlow()

    /** Ob Wachenseite oder Wachen-Shop gerade offen sind — dann zählt ein Anstoß doppelt. */
    private var seiteOffen = false

    /** Nur die jüngste Suche darf die Liste setzen — siehe `oeffentlicheLaden`. */
    private var sucheNr = 0

    /** Die Laufbahn ändert sich nur mit einer Auslieferung — einmal geholt, gilt sie. */
    private var laufbahnGeholt = false

    // ------------------------------------------------------------ Verbindung

    /**
     * Den ersten Stand holen — beim Anmelden, nicht erst beim Öffnen der Seite.
     *
     * Die Marke an der Tableiste zählt Einladungen und offene Anträge mit; wer
     * erst beim Öffnen der Wache davon erführe, öffnete sie nie deswegen.
     */
    fun anmelden() {
        val jetzt = kennung()
        if (_stand.value.fuer != jetzt) _stand.value = Wachenstand(fuer = jetzt)
        kurzLaden()
        antraegeLaden()
    }

    /**
     * Was der Sozial-Hub anstößt: `Gemeinschaft`, `Gemeinschaften`,
     * `Gemeinschaftsantrag`, eine Zeile an eine geschlossene Wachenseite.
     *
     * Nur nachladen, was gerade offen ist — an einer geschlossenen Seite ändert
     * ein Beitritt nichts, was man sähe.
     */
    fun anstoss() {
        kurzLaden()
        antraegeLaden()
        val offen = daten.value.wacheDetail.inhalt?.gemeinschaft?.id
        if (seiteOffen && offen != null) detailLaden(offen)
    }

    /** Die Seite ist offen — der Detailstand wird nachgezogen, Anstöße zählen. */
    fun oeffnen(id: String) {
        seiteOffen = true
        detailLaden(id)
    }

    /**
     * Die Seite ist zu. Detail und Verlauf bleiben als warmer Stand liegen —
     * beim Wechsel zwischen Wache und Shop ist so sofort Inhalt da.
     */
    fun schliessen() {
        seiteOffen = false
    }

    // ----------------------------------------------------------------- Laden

    fun kurzLaden() = umfang.launch {
        val k = kennung() ?: return@launch
        val vorher = daten.value.wache
        if (!vorher.geladen) daten.update { it.copy(wache = vorher.copy(laedt = true, fehler = null)) }

        runCatching { wege.gemeinschaft(k) }
            .onSuccess { eigene ->
                daten.update { d ->
                    d.copy(
                        wache = Bereich(
                            inhalt = Wachendaten(
                                eigene = eigene,
                                offene = d.wache.inhalt?.offene.orEmpty(),
                            ),
                            geladen = true,
                        ),
                        // Wer nicht mehr dabei ist, hat auch keinen Detailstand mehr —
                        // sonst stünde nach dem Hinauswurf die alte Mannschaft da.
                        wacheDetail = if (eigene == null) Bereich<GemeinschaftDetail?>(geladen = true) else d.wacheDetail,
                    )
                }
            }
            .onFailure { f ->
                daten.update { d ->
                    d.copy(
                        wache = d.wache.copy(
                            laedt = false,
                            geladen = true,
                            fehler = f.message ?: "Die Wache ließ sich nicht laden.",
                        ),
                    )
                }
            }
    }

    fun antraegeLaden() = umfang.launch {
        val k = kennung() ?: return@launch
        runCatching { wege.antraege(k) }
            .onSuccess { liste ->
                daten.update { d -> d.copy(wachenantraege = Bereich(liste, geladen = true)) }
            }
    }

    fun detailLaden(id: String) = umfang.launch {
        val k = kennung() ?: return@launch
        runCatching { wege.detail(k, id) }
            .onSuccess { detail ->
                daten.update { d -> d.copy(wacheDetail = Bereich(detail, geladen = true)) }
            }
            .onFailure { f ->
                // Ein alter Stand bleibt stehen — ein Aussetzer beim Nachziehen
                // soll die Mannschaft nicht vom Schirm nehmen.
                daten.update { d ->
                    d.copy(wacheDetail = d.wacheDetail.copy(fehler = f.message, geladen = true))
                }
            }
    }

    /**
     * Die öffentliche Liste mit den gerade gesetzten Filtern.
     *
     * Nur die jüngste Suche darf die Liste setzen — eine langsame ältere
     * überschriebe sonst die Treffer der neueren.
     */
    fun oeffentlicheLaden(filter: Gemeinschaftsfilter = _stand.value.filter) = umfang.launch {
        val nr = ++sucheNr
        _stand.update { it.copy(filter = filter, oeffentlicheLaeuft = true) }
        runCatching { wege.oeffentliche(filter) }
            .onSuccess { liste ->
                if (nr == sucheNr) {
                    _stand.update {
                        it.copy(oeffentliche = liste, oeffentlicheLaeuft = false, oeffentlicheGeladen = true)
                    }
                }
            }
            .onFailure { f ->
                if (nr == sucheNr) {
                    _stand.update {
                        it.copy(
                            oeffentlicheLaeuft = false,
                            oeffentlicheGeladen = true,
                            meldung = f.message ?: "Die Liste ließ sich nicht laden.",
                        )
                    }
                }
            }
    }

    /** Einen Filter ändern, ohne gleich zu suchen — für das Suchfeld. */
    fun filterSetzen(filter: Gemeinschaftsfilter) = _stand.update { it.copy(filter = filter) }

    fun ranglisteLaden() = umfang.launch {
        _stand.update { it.copy(rangliste = it.rangliste.copy(laedt = true, fehler = null)) }
        runCatching { wege.rangliste(kennung()) }
            .onSuccess { liste -> _stand.update { it.copy(rangliste = Bereich(liste, geladen = true)) } }
            .onFailure { f ->
                _stand.update {
                    it.copy(
                        rangliste = it.rangliste.copy(
                            laedt = false,
                            geladen = true,
                            fehler = f.message ?: "Die Rangliste ließ sich nicht laden.",
                        ),
                    )
                }
            }
    }

    /** Die Stufen der Wachen-Laufbahn — still: Ohne sie fehlt nur das Band. */
    fun laufbahnLaden() = umfang.launch {
        if (laufbahnGeholt) return@launch
        runCatching { wege.laufbahn() }
            .onSuccess { liste ->
                laufbahnGeholt = true
                _stand.update { it.copy(laufbahn = liste) }
            }
    }

    /** Der Landkreis-Vorschlag fürs Gründen — Bequemlichkeit, also still. */
    fun landkreisvorschlagLaden() = umfang.launch {
        val k = kennung() ?: return@launch
        runCatching { wege.landkreisvorschlag(k) }
            .onSuccess { v -> _stand.update { it.copy(landkreisvorschlag = v) } }
    }

    // ------------------------------------------------------------- Suche

    /** Sucht ein Konto über den Benutzernamen — für den Einladen-Dialog. */
    fun benutzerSuchen(name: String) = umfang.launch {
        val k = kennung() ?: return@launch
        if (name.isBlank()) return@launch
        _stand.update { it.copy(suchtreffer = null, suchmeldung = null) }
        runCatching { wege.benutzerSuchen(k, name) }
            .onSuccess { treffer ->
                _stand.update {
                    it.copy(
                        suchtreffer = treffer,
                        suchmeldung = if (treffer == null) {
                            "Zu diesem Benutzernamen ist niemand zu finden."
                        } else {
                            null
                        },
                    )
                }
            }
            .onFailure { f ->
                _stand.update { it.copy(suchmeldung = f.message ?: "Die Suche ging gerade nicht.") }
            }
    }

    /** Beim Schließen des Dialogs — ein alter Treffer gehört nicht in den nächsten. */
    fun sucheLeeren() = _stand.update { it.copy(suchtreffer = null, suchmeldung = null) }

    fun meldungWegnehmen() = _stand.update { it.copy(meldung = null) }

    // -------------------------------------------------------------- Aktionen

    fun gruenden(
        name: String,
        beschreibung: String?,
        landkreisId: String?,
        landkreis: String?,
        danach: () -> Unit = {},
    ) = mitMeldung(danach) { k ->
        Neuer.Kurz(wege.gruenden(k, name, beschreibung, landkreisId, landkreis))
    }

    fun beitretenMitCode(code: String) = mitMeldung { k ->
        Neuer.Kurz(wege.beitretenMitCode(k, code))
    }

    fun bewerben(id: String, nachricht: String?) = mitMeldung { k ->
        val beigetreten = wege.bewerben(k, id, nachricht)
        if (!beigetreten) _stand.update { it.copy(meldung = "Deine Bewerbung ist raus.") }
        antraegeLaden()
        null
    }

    /**
     * Entscheiden — über einen Antrag an die Wache, eine Einladung an einen
     * selbst, oder die eigene Bewerbung zurückziehen (`annehmen = false`).
     */
    fun antragEntscheiden(nr: Long, annehmen: Boolean) = mitMeldung { k ->
        wege.antragEntscheiden(k, nr, annehmen)
        antraegeLaden()
        null
    }

    /**
     * Einladen — mit der <b>Kennung</b>. Hier lief früher der getippte
     * Benutzername als Kennung zum Server, und jede Einladung scheiterte.
     */
    fun einladen(id: String, wen: String) = mitMeldung { k ->
        wege.einladen(k, id, wen)
        _stand.update { it.copy(meldung = "Die Einladung ist raus.", eingeladen = it.eingeladen + wen) }
        Neuer.Nichts
    }

    fun rolleSetzen(id: String, wen: String, rolle: String) = mitMeldung { k ->
        Neuer.Detail(wege.rolleSetzen(k, id, wen, rolle))
    }

    fun leitungUebertragen(id: String, an: String) = mitMeldung { k ->
        Neuer.Detail(wege.leitungUebertragen(k, id, an))
    }

    fun entfernen(id: String, wen: String) = mitMeldung { k ->
        wege.mitgliedEntfernen(k, id, wen)
        null
    }

    /**
     * Austreten. Danach gibt es nichts mehr zu zeigen — der lokale Stand fällt
     * mit. Der Hub erfährt es über den Anstoß des Servers.
     */
    fun verlassen(id: String, danach: () -> Unit = {}) = mitMeldung(danach) { k ->
        wege.mitgliedEntfernen(k, id, k)
        Neuer.Raus
    }

    fun zeileMelden(nr: Long, grund: String?) = mitMeldung { k ->
        wege.nachrichtMelden(k, nr, grund)
        _stand.update { it.copy(meldung = "Die Wachenführung sieht sich das an.") }
        Neuer.Nichts
    }

    /** Ohne Nachladen des Verlaufs: Die entfernte Zeile kommt über den Hub zurück. */
    fun zeileEntfernen(nr: Long) = mitMeldung { k ->
        wege.nachrichtEntfernen(k, nr)
        null
    }

    fun meldungAbhaken(nr: Long) = mitMeldung { k ->
        wege.meldungErledigen(k, nr)
        null
    }

    fun einstellungenSpeichern(id: String, e: Gemeinschaftseinstellungen) = mitMeldung { k ->
        Neuer.Detail(wege.einstellungenSpeichern(k, id, e))
    }

    fun codeErneuern(id: String) = mitMeldung { k ->
        wege.beitrittscodeErneuern(k, id)
        null
    }

    fun aufloesen(id: String, danach: () -> Unit = {}) = mitMeldung(danach) { k ->
        wege.aufloesen(k, id)
        Neuer.Raus
    }

    fun rundeSchliessen(id: String) = mitMeldung { k ->
        wege.clanrundeSchliessen(k, id)
        null
    }

    /** Heftet einen Aushang an — ein leerer Text nimmt ihn ab. */
    fun anheften(id: String, text: String?, danach: () -> Unit = {}) = mitMeldung(danach) { k ->
        Neuer.Detail(wege.pinnwandSpeichern(k, id, text?.trim()?.ifEmpty { null }))
    }

    /**
     * Setzt den Wachentag — leer nimmt ihn ab. Danach das eigene Konto neu: Der
     * Tag steht auch am eigenen Namen.
     */
    fun tagSetzen(id: String, tag: String) = mitMeldung { k ->
        val neu = wege.tagSetzen(k, id, tag.trim().uppercase())
        kontoAuffrischen()
        Neuer.Detail(neu)
    }

    /** Einen Dienst planen — `wann` ist ein echter Zeitpunkt, die Zone rechnet `Instant`. */
    fun dienstPlanen(id: String, titel: String, wann: Instant) = mitMeldung { k ->
        wege.terminPlanen(k, id, titel, wann.toString())
        null
    }

    fun dienstBeantworten(nr: Long, antwort: String) = mitMeldung { k ->
        wege.terminBeantworten(k, nr, antwort)
        null
    }

    fun dienstAbsagen(nr: Long) = mitMeldung { k ->
        wege.terminAbsagen(k, nr)
        null
    }

    fun ausbauKaufen(id: String, artikelId: String) = mitMeldung { k ->
        Neuer.Detail(wege.ausbauKaufen(k, id, artikelId))
    }

    fun wunschSetzen(id: String, artikelId: String, an: Boolean) = mitMeldung { k ->
        Neuer.Detail(wege.wunschSetzen(k, id, artikelId, an))
    }

    /**
     * Credits in die Wachenkasse. Danach stimmt der eigene Credit-Stand nicht
     * mehr — die Detailantwort kennt ihn nicht, also holt ihn das Konto nach.
     */
    fun einzahlen(id: String, credits: Int) = mitMeldung { k ->
        val neu = wege.einzahlen(k, id, credits)
        kontoAuffrischen()
        Neuer.Detail(neu)
    }

    /** Legt ein Zierstück an oder ab — der Platz steht in `art`. */
    fun schmuckSetzen(id: String, art: String, stueckId: String?) = mitMeldung { k ->
        Neuer.Detail(wege.schmuckSetzen(k, id, art, stueckId))
    }

    fun farbeSetzen(id: String, farbe: Int) = mitMeldung { k ->
        Neuer.Detail(wege.farbeSetzen(k, id, farbe))
    }

    // ------------------------------------------------------------ Der Mantel

    /**
     * Was eine Aktion an neuem Stand mitbringt.
     *
     * `null` heißt: nur eine Quittung — dann wird nachgeladen. `Nichts` heißt:
     * weder Stand noch Nachladen (eine Meldung, eine Einladung).
     */
    private sealed interface Neuer {
        data class Detail(val detail: GemeinschaftDetail) : Neuer
        data class Kurz(val gemeinschaft: Gemeinschaft) : Neuer
        data object Raus : Neuer
        data object Nichts : Neuer
    }

    /**
     * Führt eine Aktion aus und schreibt eine abgelehnte Antwort in `meldung`.
     *
     * @param danach Läuft nur nach Erfolg — ein Dialog schließt sich dann, und
     *   bleibt bei einem Fehler offen, damit man die Meldung darin lesen kann.
     */
    private fun mitMeldung(
        danach: () -> Unit = {},
        arbeit: suspend (String) -> Neuer?,
    ) = umfang.launch {
        val k = kennung() ?: return@launch
        _stand.update { it.copy(laeuft = true, meldung = null) }

        runCatching { arbeit(k) }
            .onSuccess { neu ->
                uebernehmen(neu)
                danach()
            }
            .onFailure { f ->
                _stand.update { it.copy(meldung = f.message ?: "Das hat gerade nicht geklappt.") }
            }

        _stand.update { it.copy(laeuft = false) }
    }

    private fun uebernehmen(neu: Neuer?) {
        when (neu) {
            null -> nachladen()
            Neuer.Nichts -> Unit
            Neuer.Raus -> daten.update { d ->
                d.copy(
                    wache = Bereich(Wachendaten(eigene = null), geladen = true),
                    wacheDetail = Bereich<GemeinschaftDetail?>(geladen = true),
                )
            }
            is Neuer.Kurz -> daten.update { d ->
                d.copy(wache = Bereich(Wachendaten(eigene = neu.gemeinschaft), geladen = true))
            }
            is Neuer.Detail -> daten.update { d ->
                d.copy(
                    wacheDetail = Bereich(neu.detail, geladen = true),
                    wache = Bereich(Wachendaten(eigene = neu.detail.gemeinschaft), geladen = true),
                )
            }
        }
    }

    /** Holt nach, was eine bloß quittierte Aktion geändert haben kann. */
    private fun nachladen() {
        kurzLaden()
        daten.value.wacheDetail.inhalt?.gemeinschaft?.id?.let { id ->
            // Ein Hauch Luft: Der Server schreibt den Anstoß an alle anderen
            // Geräte nach der Antwort — wer sofort fragt, liest manchmal den
            // Stand davor.
            umfang.launch {
                delay(150)
                detailLaden(id)
            }
        }
    }
}

/** Was nur die Wachenseiten brauchen — neben `wache`/`wacheDetail` im `Seitenstand`. */
data class Wachenstand(
    /** Für welches Konto dieser Stand gilt — beim Kontowechsel wird er verworfen. */
    val fuer: String? = null,
    val laeuft: Boolean = false,
    /** Der Satz des Servers oder eine Bestätigung — steht unter dem Kopf der Seite. */
    val meldung: String? = null,
    val filter: Gemeinschaftsfilter = Gemeinschaftsfilter(),
    val oeffentliche: List<Gemeinschaft> = emptyList(),
    val oeffentlicheLaeuft: Boolean = false,
    val oeffentlicheGeladen: Boolean = false,
    val rangliste: Bereich<List<Wachenplatz>> = Bereich(),
    val laufbahn: List<Wachenrang> = emptyList(),
    val landkreisvorschlag: Landkreisvorschlag? = null,
    val suchtreffer: Wachensuchtreffer? = null,
    val suchmeldung: String? = null,
    /** Wen man in dieser Sitzung schon eingeladen hat — gegen den Doppelklick. */
    val eingeladen: Set<String> = emptySet(),
)

/**
 * Die Marke am Wache-Weg der Tableiste.
 *
 * <b>Alles, was eine Antwort verlangt:</b> ungelesene Zeilen, Einladungen an
 * einen selbst, Beitrittsanträge und gemeldete Zeilen — dieselbe Summe wie
 * `offenesGesamt` im Web. Anträge und Meldungen liefert der Server nur an die,
 * die darüber entscheiden; für alle anderen sind sie 0.
 *
 * Solange die Wache nicht geladen ist, steht dort nichts — eine Null wäre eine
 * Behauptung, keine Auskunft.
 */
fun wachenmarke(daten: Seitenstand): Int? {
    val wache = daten.wache.inhalt ?: return null
    val eigene = wache.eigene
    val einladungen = daten.wachenantraege.inhalt.orEmpty()
        .count { it.richtung == "Einladung" && it.stand == "Offen" }

    return (eigene?.ungelesen ?: 0) +
        einladungen +
        (eigene?.offeneAntraege ?: 0) +
        (eigene?.offeneMeldungen ?: 0)
}
