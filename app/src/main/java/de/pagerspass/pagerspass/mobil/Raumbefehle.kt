package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.AaoVorlagenzeile
import de.pagerspass.pagerspass.netz.Raumwache
import de.pagerspass.pagerspass.netz.Raumwege
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenvorlagenzeile
import de.pagerspass.pagerspass.netz.Stichwortset
import de.pagerspass.pagerspass.netz.wert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import de.pagerspass.pagerspass.netz.Kreiswache
import de.pagerspass.pagerspass.netz.Wachenwahl

/**
 * Die Handgriffe im Raum, die über Rolle, Status und Funk hinausgehen — das
 * Gegenstück zum größeren Teil von `stores/spiel.ts`.
 *
 * <b>Warum neben `Runde` und nicht darin.</b> `Runde` hält, was eine Runde *ist*:
 * Verbindung, Raumzustand, Melder, Sprechfunk. Hier steht, was man darin *tut* —
 * über sechzig Hub-Aufrufe von der Platzanfrage bis zur Blutdruckmessung, dazu die
 * vier Nebenleitungen mit Stimme (Draht, Einsatzstelle, Notruftelefon, Einzelruf).
 * In einer Klasse wären das zweitausend Zeilen, in denen man den Beitritt nicht mehr
 * findet.
 *
 * <b>Alles geht über `rufen`, nichts wartet auf eine Antwort.</b> Die Wirkung kommt
 * als nächster `RoomState` zurück, eine Absage als `Rejected` — dieselbe Aufteilung
 * wie im Web.
 *
 * <b>Die Argumentzahl muss auf den Kopf stimmen.</b> SignalR bindet nach Stelle und
 * verwirft einen Aufruf mit einer Stelle zu wenig *im Ganzen* — C#-Standardwerte
 * helfen über die Leitung nicht. Deshalb schickt jeder Aufruf hier jede Stelle mit,
 * auch die, die `null` sind.
 */
class Raumbefehle internal constructor(
    private val leitung: Leitung,
    private val bereich: CoroutineScope,
    private val wege: Raumwege?,
    private val eigeneKennung: suspend () -> String?,
    private val beiFehler: (String) -> Unit,
) {

    /** Was diese Klasse von der Verbindung braucht — und nicht mehr. */
    interface Leitung {
        fun rufen(methode: String, vararg argumente: JsonElement)
        suspend fun frage(methode: String, vararg argumente: JsonElement): JsonElement?
        fun auf(ereignis: String, empfang: (List<JsonElement>) -> Unit)
    }

    private val _neben = MutableStateFlow(Raumneben())

    /** Der Stand neben dem Raumzustand — Warnband, Nebenleitungen, geladene Listen. */
    val neben: StateFlow<Raumneben> = _neben.asStateFlow()

    // ----------------------------------------------------- Mikrofon und Ohren

    /**
     * Ein Mikrofon für die Nebenleitungen.
     *
     * <b>Eines, nicht vier.</b> Es gibt nur ein Mikrofon am Gerät; wer am Draht
     * spricht, fragt nicht zugleich den Anrufer. `weg` sagt, wem es gerade gehört,
     * und jede Taste fragt erst, ob es frei ist.
     */
    private val mikrofon = Mikrofon()
    private var weg: String? = null
    private var sendegrenze: Job? = null

    // Je Leitung ein eigener Lautsprecher: Funk und Telefon teilen sich sonst
    // denselben Abspielzeiger, und ein Anrufer spräche in den Draht hinein.
    private val drahtOhr = Lautsprecher()
    private val stellenOhr = Lautsprecher()
    private val telefonOhr = Lautsprecher()
    private val einzelrufOhr = Lautsprecher()

    private var ich: String? = null
    private var drahtSprecherId: String? = null
    private var stellenSprecherId: String? = null
    private var einzelrufMikroFuer: String? = null
    private var letzterRaum: Raumzustand? = null

    init {
        // Die Bevölkerungswarnung kommt an alle — auch an die Besatzung, das ist ihr Witz.
        leitung.auf("Bevoelkerungswarnung") { a ->
            val text = a.firstOrNull()?.let { text(it) } ?: return@auf
            _neben.update { it.copy(warnung = text, warnungUm = System.currentTimeMillis()) }
        }

        // ------------------------------------------------------------ Draht
        leitung.auf("DrahtSprecher") { a ->
            val id = a.getOrNull(0)?.let { text(it) } ?: return@auf
            val name = a.getOrNull(1)?.let { text(it) } ?: ""
            drahtSprecherId = id
            _neben.update { it.copy(drahtSprecher = if (id == ich) null else name) }
        }
        leitung.auf("DrahtSprecherEnde") { a ->
            val id = a.getOrNull(0)?.let { text(it) }
            // Nur der laufende Sprecher räumt ab — zwei Disponenten lösen sich schnell
            // ab, und ein ungeprüftes Zurücksetzen verschluckte den Nachfolger.
            if (id == drahtSprecherId) {
                drahtSprecherId = null
                _neben.update { it.copy(drahtSprecher = null) }
            }
        }
        leitung.auf("DrahtBelegt") { a ->
            val name = a.firstOrNull()?.let { text(it) } ?: "jemand"
            beiFehler("Draht belegt – $name spricht.")
            if (weg == "draht") drahtSprechenBeenden()
        }
        leitung.auf("DrahtAudio") { a ->
            val id = a.getOrNull(0)?.let { text(it) }
            val paket = a.getOrNull(1)?.let { text(it) } ?: return@auf
            if (id == drahtSprecherId && id != ich) drahtOhr.abspielen(paket)
        }

        // ---------------------------------------------------- Einsatzstelle
        leitung.auf("EinsatzstelleSprecher") { a ->
            val id = a.getOrNull(0)?.let { text(it) } ?: return@auf
            val name = a.getOrNull(1)?.let { text(it) } ?: ""
            stellenSprecherId = id
            _neben.update { it.copy(stellenSprecher = if (id == ich) null else name) }
        }
        leitung.auf("EinsatzstelleSprecherEnde") { a ->
            val id = a.getOrNull(0)?.let { text(it) }
            if (id == stellenSprecherId) {
                stellenSprecherId = null
                _neben.update { it.copy(stellenSprecher = null) }
            }
        }
        leitung.auf("EinsatzstelleBelegt") { a ->
            val name = a.firstOrNull()?.let { text(it) } ?: "jemand"
            beiFehler("Einsatzstelle belegt – $name spricht.")
            if (weg == "stelle") einsatzstelleSprechenBeenden()
        }
        leitung.auf("EinsatzstelleAudio") { a ->
            val id = a.getOrNull(0)?.let { text(it) }
            val paket = a.getOrNull(1)?.let { text(it) } ?: return@auf
            if (id == stellenSprecherId && id != ich) stellenOhr.abspielen(paket)
        }

        // ------------------------------------------------------- Notruftelefon

        // Die Stimme des Anrufers — nur zum Anruf, der gerade am eigenen Apparat
        // liegt. Ein Häppchen zu einem längst aufgelegten Anruf muss weg.
        leitung.auf("TelefonAudio") { a ->
            val anrufId = a.getOrNull(0)?.let { text(it) }
            val paket = a.getOrNull(1)?.let { text(it) } ?: return@auf
            if (anrufId != null && anrufId == _neben.value.telefonAnruf) telefonOhr.abspielen(paket)
        }

        leitung.auf("NotrufErkannt") { a ->
            val anrufId = a.getOrNull(0)?.let { text(it) }
            val erkannt = a.getOrNull(1)?.let { text(it) }
            _neben.update { it.copy(notrufVersteht = false) }
            if (erkannt.isNullOrBlank()) {
                beiFehler("Nichts verstanden — die Frage wurde nicht gestellt.")
                return@auf
            }
            // Wer inzwischen aufgelegt hat, findet seine letzte Frage nicht im
            // nächsten Gespräch wieder.
            if (anrufId == null || anrufId != _neben.value.telefonAnruf) return@auf
            anrufFrageText(anrufId, erkannt)
        }

        // ---------------------------------------------------------- Einzelruf
        leitung.auf("EinzelrufAudio") { a ->
            val id = a.getOrNull(0)?.let { text(it) }
            val paket = a.getOrNull(1)?.let { text(it) } ?: return@auf
            val ruf = eigenerLaufenderRuf(letzterRaum) ?: return@auf
            val gegenstelle = if (ruf.vonPlayerId == ich) ruf.angenommenVonPlayerId else ruf.vonPlayerId
            if (id == gegenstelle) einzelrufOhr.abspielen(paket)
        }

        leitung.auf("EinzelrufErkannt") { a ->
            val rufId = a.getOrNull(0)?.let { text(it) }
            val erkannt = a.getOrNull(1)?.let { text(it) }
            _neben.update { it.copy(einzelrufVersteht = false) }
            if (erkannt.isNullOrBlank()) {
                beiFehler("Nichts verstanden — der Satz wurde nicht gesagt.")
                return@auf
            }
            if (rufId == null || rufId != eigenerLaufenderRuf(letzterRaum)?.id) return@auf
            einzelrufSagen(rufId, erkannt)
        }
    }

    /**
     * Der Raumzustand hat sich geändert — die Nebenleitungen folgen ihm.
     *
     * <b>Das Mikrofon des Einzelrufs hängt am Zustand, nicht an einem Knopf.</b>
     * Vollduplex heißt: Es geht mit der Annahme an und mit dem Auflegen aus, egal
     * wer auflegt. Am Knopf aufgehängt bliebe es offen, wenn die Gegenstelle zuerst
     * auflegt.
     */
    internal fun raumGeaendert(raum: Raumzustand) {
        letzterRaum = raum
        bereich.launch {
            if (ich == null) ich = runCatching { eigeneKennung() }.getOrNull()
            val selbst = ich ?: return@launch

            // Der Anruf am eigenen Apparat — daran hängen Anruferstimme und die
            // Sprechtaste der Rückfrage.
            val amApparat = raum.anrufe.firstOrNull {
                it.imGespraech && it.bearbeiterPlayerId == selbst
            }?.id
            if (amApparat != _neben.value.telefonAnruf) {
                _neben.update { it.copy(telefonAnruf = amApparat) }
            }

            val ruf = eigenerLaufenderRuf(raum)
            if (ruf != null && !ruf.mitBot && einzelrufMikroFuer != ruf.id) {
                einzelrufMikroFuer = ruf.id
                if (weg == null) {
                    mikroAn("einzelruf") { p -> leitung.rufen("EinzelrufAudio", wert(p)) }
                }
            } else if (ruf == null && einzelrufMikroFuer != null) {
                einzelrufMikroFuer = null
                if (weg == "einzelruf") mikroAus()
                einzelrufOhr.schliessen()
            }
        }
    }

    private fun eigenerLaufenderRuf(raum: Raumzustand?) = raum?.einzelrufe?.firstOrNull {
        it.laeuft && (it.vonPlayerId == ich || it.angenommenVonPlayerId == ich)
    }

    /** Beim Verlassen: alle Leitungen zu, alle Merker weg. */
    internal fun zuruecksetzen() {
        mikroAus()
        listOf(drahtOhr, stellenOhr, telefonOhr, einzelrufOhr).forEach { it.schliessen() }
        drahtSprecherId = null
        stellenSprecherId = null
        einzelrufMikroFuer = null
        letzterRaum = null
        _neben.value = Raumneben()
    }

    fun warnungWegnehmen() = _neben.update { it.copy(warnung = null) }

    // ------------------------------------------------------ Mikrofon-Verwaltung

    private fun mikroAn(neuerWeg: String, beiPaket: (String) -> Unit): Boolean {
        if (weg != null) {
            beiFehler("Das Mikrofon ist gerade belegt.")
            return false
        }
        if (!mikrofon.starten(beiPaket)) {
            beiFehler("Das Mikrofon war nicht zu bekommen.")
            return false
        }
        weg = neuerWeg
        _neben.update { it.copy(sendetAuf = neuerWeg) }
        return true
    }

    private fun mikroAus() {
        if (weg == null) return
        sendegrenze?.cancel()
        mikrofon.stoppen()
        weg = null
        _neben.update { it.copy(sendetAuf = null) }
    }

    /** Dieselbe Obergrenze wie beim Funk — niemand hält eine Leitung ewig. */
    private fun grenzeSetzen(beiAblauf: () -> Unit) {
        sendegrenze?.cancel()
        sendegrenze = bereich.launch {
            delay(Sprechfunk.MAX_SENDEDAUER_MS)
            beiAblauf()
        }
    }

    // ================================================================ Lobby

    /** Um den Leitstellentisch bitten — der Host entscheidet. */
    fun leitstellenplatzAnfragen() = leitung.rufen("LeitstellenplatzAnfragen")

    fun platzanfrageEntscheiden(spielerId: String, annehmen: Boolean) =
        leitung.rufen("PlatzanfrageEntscheiden", wert(spielerId), wert(annehmen))

    fun platzanfrageZuruecknehmen() = leitung.rufen("PlatzanfrageZuruecknehmen")

    /** Ein zusehendes Premium-Konto bittet um einen Platz in einer vollen Runde. */
    fun beitrittAnfragen() = leitung.rufen("BeitrittAnfragen")

    fun beitrittEntscheiden(spielerId: String, annehmen: Boolean) =
        leitung.rufen("BeitrittEntscheiden", wert(spielerId), wert(annehmen))

    fun beitrittsanfrageZuruecknehmen() = leitung.rufen("BeitrittsanfrageZuruecknehmen")

    /** Einen menschlichen Mitspieler aus dem Raum werfen — nur die Leitstelle. */
    fun spielerKicken(spielerId: String) = leitung.rufen("KickPlayer", wert(spielerId))

    /** Beim späten Beitritt eine vorhandene Bot-Besatzung übernehmen. */
    fun botUebernehmen(botId: String) = leitung.rufen("BotUebernehmen", wert(botId))

    /** Eine Bot-Besatzung auf ein anderes Fahrzeug versetzen. */
    fun botVersetzen(botId: String, vorlageId: String) =
        leitung.rufen("AssignBot", wert(botId), wert(vorlageId))

    fun botHinzufuegen(vorlageId: String, anzahl: Int = 1) =
        leitung.rufen("AddBot", wert(vorlageId), wert(anzahl.coerceIn(1, 10)))

    fun botEntfernen(botId: String) = leitung.rufen("RemoveBot", wert(botId))

    /**
     * Die Rundeneinstellungen ändern — alle 44 Stellen von `UpdateSettings`.
     *
     * Was `null` bleibt, heißt „nicht anfassen". Nur die Kennzahlen stehen fest auf
     * `null` — sie setzt der Leitstellenbau, nicht die Lobby (wie im Web).
     *
     * <b>Wachen, Funkgruppen und die beiden Wörterbücher ersetzen das Ganze.</b>
     * Ein Löschen ließe sich anders nicht ausdrücken; eine leere Liste ist deshalb
     * eine gültige Ansage („alle Wachen", „ein Kanal für alle").
     */
    fun einstellungen(a: Einstellungsaenderung) = leitung.rufen(
        "UpdateSettings",
        opt(a.mode),
        opt(a.ort),
        opt(a.leitstelle),
        opt(a.einsatzIntervallSekunden),
        a.organisationen?.let { liste(it) } ?: JsonNull,
        opt(a.botTempo),
        a.hiOrgs?.let { liste(it) } ?: JsonNull,
        opt(a.freischaltungenIgnorieren),
        opt(a.oeffentlich),
        opt(a.botGespraechigkeit),
        opt(a.botFunkAktiv),
        opt(a.zeitmodus),
        opt(a.telefonischeLeitstelle),
        opt(a.stoerungshaeufigkeit),
        opt(a.tagesalarmstaerke),
        opt(a.loeschwasser),
        opt(a.sonderobjekte),
        opt(a.wiederherstellung),
        opt(a.gefahrgutlagen),
        opt(a.jahreszeit),
        opt(a.silvester),
        opt(a.verlegungsfahrten),
        opt(a.suchlagen),
        opt(a.einsatzarbeit),
        opt(a.vegetationsbraende),
        opt(a.arbeitsfunk),
        opt(a.einsatzende),
        opt(a.einsatzleitung),
        opt(a.maxSpieler),
        a.wachen?.let { wachenliste(it) } ?: JsonNull,
        a.rufnamenpraefixe?.let { woerterbuch(it) } ?: JsonNull,
        JsonNull, // kennzahlen
        opt(a.wachennummerStellen),
        opt(a.laufnummerStellen),
        a.funkgruppen?.let { gruppenliste(it) } ?: JsonNull,
        a.wachnummern?.let { m -> buildJsonObject { m.forEach { (k, v) -> put(k, v) } } } ?: JsonNull,
        opt(a.funkverstossSchwelle),
        opt(a.einsatzdichte),
        opt(a.streamermodus),
        opt(a.streamerplattform),
        opt(a.streamerkanal),
        opt(a.streameraufzeichnung),
        opt(a.kiFunkAktiv),
        opt(a.stichwortsetId),
        // Die drei KI-Schalter aus 5.0.0.26 hängen nur dann an, wenn einer davon
        // gesetzt ist: SignalR verwirft einen Aufruf mit einer Stelle *zu viel* im
        // Ganzen, und ein älterer Server kennt sie nicht. So bleibt jede andere
        // Einstellung auch dort einstellbar.
        *(
            if (a.kiAnrufeAktiv != null || a.kiLeitstelleAktiv != null || a.kiLagenAktiv != null) {
                arrayOf(opt(a.kiAnrufeAktiv), opt(a.kiLeitstelleAktiv), opt(a.kiLagenAktiv))
            } else {
                emptyArray<JsonElement>()
            }
        ),
    )

    /** Ein Fahrzeug vor Dienstbeginn auf eine andere Wache stellen. */
    fun wacheZuweisen(fahrzeugId: String, kennung: String) =
        leitung.rufen("WacheZuweisen", wert(fahrzeugId), wert(kennung))

    /** Ohne Kurzform leitet der Server sie selbst ab — aus dem letzten Wort. */
    fun funkrufnameAendern(fahrzeugId: String, funkrufname: String, kurzname: String? = null) =
        leitung.rufen(
            "FunkrufnameAendern",
            wert(fahrzeugId),
            wert(funkrufname.trim()),
            kurzname?.trim()?.ifBlank { null }?.let { wert(it) } ?: JsonNull,
        )

    fun funkrufnameZuruecksetzen(fahrzeugId: String) =
        leitung.rufen("FunkrufnameZuruecksetzen", wert(fahrzeugId))

    /**
     * Einen Wechsellader umrüsten. `null` setzt den Behälter nur ab — über die
     * Leitung als leerer Text, denn ein weggelassenes Argument verwirft den Aufruf.
     */
    fun abrollbehaelterWechseln(fahrzeugId: String, vorlageId: String?) =
        leitung.rufen("AbrollbehaelterWechseln", wert(fahrzeugId), wert(vorlageId ?: ""))

    // ======================================================= Schichtübergabe

    fun leitstelleUebergeben(spielerId: String) =
        leitung.rufen("LeitstelleUebergeben", wert(spielerId))

    fun uebergabeAntworten(annehmen: Boolean) =
        leitung.rufen("UebergabeAntworten", wert(annehmen))

    /** Der Notweg: Die einzige Leitstelle ist länger als die Schonfrist weg. */
    fun leitstelleUebernehmen() = leitung.rufen("LeitstelleUebernehmen")

    // ============================================================ Leitstelle

    /** Einen Einsatz auswürfeln — das Werkzeug der freien Vergabe. */
    fun einsatzWuerfeln() = leitung.rufen("GenerateIncident")

    /** Folgenlos, nur zum Spaß — der Server sperrt 30 Sekunden gegen Dauerfeuer. */
    fun bevoelkerungWarnen(text: String) {
        if (text.isBlank()) return
        leitung.rufen("BevoelkerungWarnen", wert(text.trim().take(300)))
    }

    fun anrufOrten(anrufId: String) = leitung.rufen("AnrufOrten", wert(anrufId))

    fun journalOrten(anrufId: String) = leitung.rufen("JournalOrten", wert(anrufId))

    /** Dieser Anrufer meldet eine Lage, die schon läuft — kein zweiter Einsatz. */
    fun anrufZuordnen(anrufId: String, incidentId: String) =
        leitung.rufen("AnrufZuordnen", wert(anrufId), wert(incidentId))

    /** Eine frei formulierte Rückfrage — gedeutet wird am Server. */
    fun anrufFrageText(anrufId: String, text: String) {
        if (text.isBlank()) return
        leitung.rufen("AnrufFrage", wert(anrufId), JsonNull, wert(text.trim()), JsonNull)
    }

    fun feststellungUebernehmen(id: String) = leitung.rufen("FeststellungUebernehmen", wert(id))

    fun feststellungVerwerfen(id: String) = leitung.rufen("FeststellungVerwerfen", wert(id))

    /** Kräfte wieder aus einem Einsatz nehmen; ohne Grund: „Kräfte nicht erforderlich". */
    fun fahrzeugeZurueckrufen(incidentId: String, fahrzeuge: List<String>, grund: String? = null) =
        leitung.rufen(
            "FahrzeugeZurueckrufen",
            wert(incidentId),
            liste(fahrzeuge),
            grund?.ifBlank { null }?.let { wert(it) } ?: JsonNull,
        )

    fun streifeSchicken(fahrzeugId: String, an: Boolean) =
        leitung.rufen("StreifeSchicken", wert(fahrzeugId), wert(an))

    fun zielklinikZuweisen(fahrzeugId: String, klinikId: String) =
        leitung.rufen("ZielklinikZuweisen", wert(fahrzeugId), wert(klinikId))

    fun suchabschnittZuteilen(fahrzeugId: String, abschnitt: Int) =
        leitung.rufen("SuchabschnittZuteilen", wert(fahrzeugId), wert(abschnitt))

    fun aufgabeZuteilen(fahrzeugId: String, nummer: Int) =
        leitung.rufen("AufgabeZuteilen", wert(fahrzeugId), wert(nummer))

    /** Die Ordnung eines laufenden Einsatzes nachschärfen — alles andere bleibt. */
    fun ordnungAendern(incidentId: String, anzahl: Int, faehigkeiten: List<String>) =
        leitung.rufen(
            "UpdateIncident",
            wert(incidentId),
            JsonNull, JsonNull, JsonNull, JsonNull,
            wert(anzahl),
            liste(faehigkeiten),
        )

    /** Eine AAO für diese Schicht merken — alle Disponenten am Tisch sehen sie. */
    fun aaoVorlageSpeichern(name: String, anzahl: Int, faehigkeiten: List<String>) {
        if (name.isBlank()) return
        leitung.rufen("SaveAaoVorlage", wert(name.trim()), wert(anzahl), liste(faehigkeiten))
    }

    fun aaoVorlageLoeschen(name: String) = leitung.rufen("DeleteAaoVorlage", wert(name))

    /** Ein Fahrzeug auf eine andere Funkgruppe — `null` heißt zurück auf Stamm. */
    fun funkgruppeZuweisen(fahrzeugId: String, gruppeId: String?) =
        leitung.rufen("FunkgruppeZuweisen", wert(fahrzeugId), wert(gruppeId ?: ""))

    /**
     * Den eigenen Leitstellenplatz einstellen: welche Gruppen er hört, auf welcher er
     * sendet. Leere Liste heißt „alle" — so sitzt man allein am Tisch.
     */
    fun funkgruppenPlatz(gruppen: List<String>, sendegruppe: String?) =
        leitung.rufen("FunkgruppenPlatz", liste(gruppen), wert(sendegruppe ?: ""))

    /** Den eigenen getippten Funk vorlesen lassen — für alle, die mithören. */
    fun funkVorlesen(an: Boolean) = leitung.rufen("FunkVorlesen", wert(an))

    // ========================================================= Fahrzeug

    /** Nachfordern — ohne eigenen Text der Satz, den das Web auch schickt. */
    fun nachfordern(text: String) =
        leitung.rufen("RequestSupport", wert(text.trim().ifBlank { "Weitere Kräfte erforderlich." }))

    /** Die Notruftaste am Handfunkgerät. Der Server sperrt danach 30 Sekunden. */
    fun notruf() = leitung.rufen("Notruf")

    /** Wasser holen — die Besatzung entscheidet das selbst. */
    fun wasserAufnehmen() = leitung.rufen("WasserAufnehmen")

    /** Eine Aufgabe übernehmen; `null` gibt sie ab. */
    fun aufgabeUebernehmen(nummer: Int?) =
        leitung.rufen("AufgabeUebernehmen", nummer?.let { wert(it) } ?: JsonNull)

    fun bereitstellungsraumFestlegen(incidentId: String) =
        leitung.rufen("BereitstellungsraumFestlegen", wert(incidentId))

    fun bereitstellungSetzen(incidentId: String, fahrzeugId: String, halten: Boolean) =
        leitung.rufen("BereitstellungSetzen", wert(incidentId), wert(fahrzeugId), wert(halten))

    fun landeplatzFestlegen(incidentId: String) =
        leitung.rufen("LandeplatzFestlegen", wert(incidentId))

    fun landeplatzAusleuchten(incidentId: String) =
        leitung.rufen("LandeplatzAusleuchten", wert(incidentId))

    fun einsatzfunkgruppeOeffnen(incidentId: String, name: String) =
        leitung.rufen("EinsatzfunkgruppeOeffnen", wert(incidentId), wert(name.trim().take(40)))

    fun einsatzfunkgruppeSchliessen(gruppeId: String) =
        leitung.rufen("EinsatzfunkgruppeSchliessen", wert(gruppeId))

    /**
     * Die eigene Maske der Streife: Was die Besatzung sieht, wird ein Einsatz.
     * Koordinaten bleiben leer — der Server nimmt die eigene Fahrzeugposition.
     */
    fun streifeneinsatzAnlegen(
        stichwort: String,
        stichwortText: String,
        meldebild: String,
        adresse: String,
        prioritaet: Int,
        empfohleneFahrzeuge: Int?,
        empfohleneFaehigkeiten: List<String>?,
    ) = leitung.rufen(
        "StreifeneinsatzAnlegen",
        wert(stichwort),
        wert(stichwortText),
        wert(meldebild),
        wert(adresse),
        JsonNull, // ortsteil
        wert(prioritaet),
        JsonNull, // lat
        JsonNull, // lon
        empfohleneFahrzeuge?.let { wert(it) } ?: JsonNull,
        empfohleneFaehigkeiten?.let { liste(it) } ?: JsonNull,
    )

    // ------------------------------------------------------- Patientenbogen

    fun patientMessen(incidentId: String, patientId: String, was: String) =
        leitung.rufen("PatientMessen", wert(incidentId), wert(patientId), wert(was))

    fun patientSchema(incidentId: String, patientId: String, schema: String) =
        leitung.rufen("PatientSchema", wert(incidentId), wert(patientId), wert(schema))

    fun patientMassnahme(incidentId: String, patientId: String, massnahmeId: String) =
        leitung.rufen("PatientMassnahme", wert(incidentId), wert(patientId), wert(massnahmeId))

    fun patientVerdacht(incidentId: String, patientId: String, text: String) =
        leitung.rufen("PatientVerdacht", wert(incidentId), wert(patientId), wert(text.trim().take(120)))

    /** Ohne eigenen Text steht der Klartext des Krankheitsbildes da. */
    fun patientDiagnose(incidentId: String, patientId: String, text: String?) =
        leitung.rufen(
            "PatientDiagnose",
            wert(incidentId),
            wert(patientId),
            text?.trim()?.ifBlank { null }?.let { wert(it.take(120)) } ?: JsonNull,
        )

    // ============================================================ Einzelruf

    /** `null` ruft die Leitstelle. */
    fun einzelrufStarten(zielFahrzeugId: String?) =
        leitung.rufen("EinzelrufStarten", zielFahrzeugId?.let { wert(it) } ?: JsonNull)

    fun einzelrufAnnehmen(rufId: String) = leitung.rufen("EinzelrufAnnehmen", wert(rufId))

    fun einzelrufAbweisen(rufId: String) = leitung.rufen("EinzelrufAbweisen", wert(rufId))

    fun einzelrufBeenden(rufId: String) = leitung.rufen("EinzelrufBeenden", wert(rufId))

    /** Ein Satz ins Gespräch — nur mit einer Bot-Besatzung am anderen Ende. */
    fun einzelrufSagen(rufId: String, text: String) {
        if (text.isBlank()) return
        leitung.rufen("EinzelrufSagen", wert(rufId), wert(text.trim().take(500)))
    }

    /** Ob Besatzungen einen anrufen dürfen — die Leitstelle kommt immer durch. */
    fun einzelrufZulassen(zulassen: Boolean) =
        leitung.rufen("EinzelrufZulassenSetzen", wert(zulassen))

    /**
     * Im Gespräch mit einer Bot-Besatzung sprechen: Der Server schneidet mit und
     * schickt den Satz als `EinzelrufErkannt` zurück.
     */
    fun einzelrufSprechenStarten(): Boolean {
        if (weg != null) return weg == "einzelrufBot"
        leitung.rufen("EinzelrufSprechenStarten")
        return mikroAn("einzelrufBot") { p -> leitung.rufen("EinzelrufAudio", wert(p)) }
    }

    fun einzelrufSprechenBeenden(rufId: String) {
        if (weg != "einzelrufBot") return
        mikroAus()
        bereich.launch {
            val folgt = runCatching { leitung.frage("EinzelrufSprechenBeenden", wert(rufId)) }
                .getOrNull()
            if ((folgt as? JsonPrimitive)?.content == "true") {
                _neben.update { it.copy(einzelrufVersteht = true) }
            }
        }
    }

    // ===================================================== Nebenleitungen

    /** Sprechtaste am Leitstellendraht. */
    fun drahtSprechenStarten(): Boolean {
        if (weg == "draht") return true
        leitung.rufen("DrahtSprechenStarten")
        if (!mikroAn("draht") { p -> leitung.rufen("DrahtAudio", wert(p)) }) {
            leitung.rufen("DrahtSprechenBeenden", wert(true))
            return false
        }
        grenzeSetzen { drahtSprechenBeenden() }
        return true
    }

    fun drahtSprechenBeenden() {
        if (weg != "draht") return
        mikroAus()
        // `true` = diese Seite liefert keinen Text; im Draht steht der Sprechvermerk.
        leitung.rufen("DrahtSprechenBeenden", wert(true))
    }

    /** Sprechtaste an der Einsatzstelle — nur mit Status 4 an der Lage. */
    fun einsatzstelleSprechenStarten(): Boolean {
        if (weg == "stelle") return true
        leitung.rufen("EinsatzstelleSprechenStarten")
        if (!mikroAn("stelle") { p -> leitung.rufen("EinsatzstelleAudio", wert(p)) }) {
            leitung.rufen("EinsatzstelleSprechenBeenden", wert(true))
            return false
        }
        grenzeSetzen { einsatzstelleSprechenBeenden() }
        return true
    }

    fun einsatzstelleSprechenBeenden() {
        if (weg != "stelle") return
        mikroAus()
        leitung.rufen("EinsatzstelleSprechenBeenden", wert(true))
    }

    /**
     * Sprechtaste am Notruftelefon — die Rückfrage wird gesprochen statt getippt.
     * Übertragen wird nichts; am anderen Ende sitzt ein Anrufer, den der Server
     * spielt. Der erkannte Satz kommt als `NotrufErkannt` zurück.
     */
    fun notrufSprechenStarten(): Boolean {
        if (weg == "notruf") return true
        leitung.rufen("NotrufSprechenStarten")
        if (!mikroAn("notruf") { p -> leitung.rufen("NotrufAudio", wert(p)) }) return false
        grenzeSetzen { _neben.value.telefonAnruf?.let { notrufSprechenBeenden(it) } ?: mikroAus() }
        return true
    }

    fun notrufSprechenBeenden(anrufId: String) {
        if (weg != "notruf") return
        mikroAus()
        bereich.launch {
            val folgt = runCatching { leitung.frage("NotrufSprechenBeenden", wert(anrufId)) }
                .getOrNull()
            if ((folgt as? JsonPrimitive)?.content == "true") {
                _neben.update { it.copy(notrufVersteht = true) }
            }
        }
    }

    /**
     * Die eigene Meldung im Alarmfenster sprechen. Sie wartet unter ihrer Kennung
     * auf dem Server, bis der Alarm hinausgeht — gehört wird sie nur von den
     * Alarmierten.
     */
    fun alarmMeldungSprechenStarten(meldungId: String): Boolean {
        if (weg == "meldung") return true
        leitung.rufen("AlarmMeldungSprechenStarten", wert(meldungId))
        return mikroAn("meldung") { p ->
            leitung.rufen("AlarmMeldungAudio", wert(meldungId), wert(p))
        }
    }

    /** Liefert, ob überhaupt etwas angekommen ist — sonst gibt es nichts anzubieten. */
    suspend fun alarmMeldungSprechenBeenden(meldungId: String): Boolean {
        if (weg != "meldung") return false
        mikroAus()
        // Die letzten Pakete sind noch unterwegs — kurz warten, bevor gefragt wird.
        delay(250)
        val antwort = runCatching {
            leitung.frage("AlarmMeldungSprechenBeenden", wert(meldungId))
        }.getOrNull()
        return (antwort as? JsonPrimitive)?.content == "true"
    }

    fun alarmMeldungVerwerfen(meldungId: String) =
        leitung.rufen("AlarmMeldungVerwerfen", wert(meldungId))

    // =========================================================== REST im Raum

    /** Die Wachen dieser Runde — für „Wache wechseln" in der Aufstellung. */
    fun raumwachenLaden(code: String) = rest {
        val liste = wege?.raumwachen(code) ?: return@rest
        _neben.update { it.copy(raumwachen = liste) }
    }

    /**
     * Die Wachen des Kreises für die Wachenmaske — einmal je Kreis und erst, wenn
     * der Kasten aufgeht. Ein Kreis liefert bis zu 155 Wachen; die bei jedem
     * Beitritt mitzuladen verzögerte ihn für eine Einstellung, die kaum jemand
     * anfasst. Ein Fehlschlag lässt die Liste leer und sagt nichts — eine Meldung
     * für eine Nebeneinstellung wäre lauter als die Sache (wie im Web).
     */
    fun kreiswachenLaden(landkreisId: String) = bereich.launch {
        if (_neben.value.kreiswachenFuer == landkreisId) return@launch
        val liste = runCatching { wege?.kreiswachen(landkreisId) }.getOrNull().orEmpty()
        _neben.update { it.copy(kreiswachen = liste, kreiswachenFuer = landkreisId) }
    }

    /**
     * Die echten Straßen des Kreises — einmal geladen und dann aus dem Speicher.
     * Leer ist kein Fehler: Für den Kreis liegen dann noch keine OSM-Daten vor.
     */
    fun strassenLaden(landkreisId: String) = bereich.launch {
        if (_neben.value.strassenFuer == landkreisId) return@launch
        val liste = runCatching { wege?.strassen(landkreisId) }.getOrNull().orEmpty()
        _neben.update { it.copy(strassen = liste, strassenFuer = landkreisId) }
    }

    // ============================================================ Übertragung

    /** „Ich streame gerade" — die Marke am eigenen Namen, an oder aus. */
    fun live(an: Boolean) = leitung.rufen("Live", wert(an))

    /**
     * Nach dem Erteilen am schon besetzten Platz: „sieh noch mal nach". Der
     * Server schlägt selbst nach — der Aufruf behauptet nichts.
     */
    suspend fun streamerfreigabeNachtragen(): Boolean =
        (runCatching { leitung.frage("StreamerfreigabeNachtragen") }.getOrNull() as? JsonPrimitive)
            ?.content == "true"

    /** Fragt einmal nach, ob der Server die KI-Schalter anbietet — still, ohne Meldung. */
    fun serverangebotLaden() = bereich.launch {
        val da = runCatching { wege?.kiFunkVerfuegbar() }.getOrNull() ?: return@launch
        _neben.update { it.copy(kiFunkVerfuegbar = da) }
    }

    fun stichwortsetsLaden() = rest {
        val liste = wege?.stichwortsets() ?: return@rest
        _neben.update { it.copy(stichwortsets = liste) }
    }

    fun rundenvorlagenLaden() = rest {
        val kennung = eigeneKennung() ?: return@rest
        val liste = wege?.rundenvorlagen(kennung) ?: return@rest
        _neben.update { it.copy(rundenvorlagen = liste) }
    }

    /** Den Reglerstand der Lobby als Vorlage merken — mit `id` überschreibt es. */
    fun rundenvorlageSichern(id: String?, name: String, raumCode: String) = rest {
        val kennung = eigeneKennung() ?: return@rest
        val w = wege ?: return@rest
        val zeile = w.rundenvorlageSichern(kennung, id, name.trim(), raumCode)
        val liste = w.rundenvorlagen(kennung)
        _neben.update {
            it.copy(rundenvorlagen = liste, vorlageMeldung = "Gespeichert. Code zum Weitergeben: ${zeile.code}")
        }
    }

    fun dauerVorlagenLaden(landkreisId: String?) = rest {
        val kennung = eigeneKennung() ?: return@rest
        val liste = wege?.aaoVorlagen(kennung, landkreisId) ?: return@rest
        _neben.update { it.copy(dauerVorlagen = liste) }
    }

    /** Eine Ordnung dauerhaft am Konto sichern — `fuerKreis` nimmt den Kreis der Runde. */
    fun dauerVorlageSichern(
        name: String,
        anzahl: Int,
        faehigkeiten: List<String>,
        landkreisId: String?,
    ) = rest {
        val kennung = eigeneKennung() ?: return@rest
        val w = wege ?: return@rest
        val ergebnis = w.aaoVorlageSichern(kennung, name.trim(), landkreisId, anzahl, faehigkeiten)
        val liste = w.aaoVorlagen(kennung, landkreisId)
        _neben.update {
            it.copy(
                dauerVorlagen = liste,
                vorlageMeldung = "Gesichert — ${ergebnis.anzahl} von ${ergebnis.grenze} Ordnungen belegt.",
            )
        }
    }

    fun dauerVorlageLoeschen(id: String) = rest {
        val kennung = eigeneKennung() ?: return@rest
        wege?.aaoVorlageLoeschen(kennung, id)
        _neben.update { n -> n.copy(dauerVorlagen = n.dauerVorlagen.filter { it.id != id }) }
    }

    fun vorlageMeldungWegnehmen() = _neben.update { it.copy(vorlageMeldung = null) }

    /** REST im Hintergrund — ein Fehler wird zur Meldung, nicht zum Absturz. */
    private fun rest(tun: suspend () -> Unit) = bereich.launch {
        runCatching { tun() }.onFailure { f ->
            beiFehler(f.message ?: "Das ging gerade nicht.")
        }
    }

    // ------------------------------------------------------------- Werkzeug

    private fun text(roh: JsonElement): String? =
        (roh as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

    private fun opt(w: String?): JsonElement = w?.let { JsonPrimitive(it) } ?: JsonNull
    private fun opt(w: Int?): JsonElement = w?.let { JsonPrimitive(it) } ?: JsonNull
    private fun opt(w: Boolean?): JsonElement = w?.let { JsonPrimitive(it) } ?: JsonNull

    private fun liste(werte: List<String>): JsonElement =
        buildJsonArray { werte.forEach { add(JsonPrimitive(it)) } }

    private fun woerterbuch(werte: Map<String, String>): JsonElement =
        buildJsonObject { werte.forEach { (k, v) -> put(k, v) } }

    /**
     * Die Wachenwahl, wie der Hub sie liest (`WachenwahlEingabe`). Jede Stelle
     * ausdrücklich, auch die leeren: Ein Eintrag aus dem Sandkasten muss mit
     * seinem Umbau unverändert zurückgehen.
     */
    private fun wachenliste(wachen: List<Wachenwahl>): JsonElement = buildJsonArray {
        wachen.forEach { w ->
            add(
                buildJsonObject {
                    put("kennung", w.kennung)
                    put("name", w.name?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("zugnummer", w.zugnummer)
                    put("lat", w.lat?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("lon", w.lon?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("organisation", w.organisation?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("traeger", w.traeger?.let { JsonPrimitive(it) } ?: JsonNull)
                },
            )
        }
    }

    /** Die Funkgruppen als `FunkgruppeEingabe` — leere Id heißt „vergib eine". */
    private fun gruppenliste(gruppen: List<Funkgruppenentwurf>): JsonElement = buildJsonArray {
        gruppen.forEach { g ->
            add(
                buildJsonObject {
                    put("id", g.id)
                    put("nummer", g.nummer.trim())
                    put("name", g.name.trim())
                    put("organisationen", liste(g.organisationen))
                    put("hiOrgs", liste(g.hiOrgs))
                    put("fuehrung", g.fuehrung)
                },
            )
        }
    }

    companion object {
        /**
         * Ein Satz Griffe ohne Leitung — für Vorschauen und als Vorgabewert der
         * Ansichten. Jeder Druck verpufft still.
         */
        val Leer: Raumbefehle by lazy {
            Raumbefehle(
                leitung = object : Leitung {
                    override fun rufen(methode: String, vararg argumente: JsonElement) = Unit
                    override suspend fun frage(methode: String, vararg argumente: JsonElement) = null
                    override fun auf(ereignis: String, empfang: (List<JsonElement>) -> Unit) = Unit
                },
                bereich = CoroutineScope(SupervisorJob() + Dispatchers.Default),
                wege = null,
                eigeneKennung = { null },
                beiFehler = {},
            )
        }
    }
}

/**
 * Was neben dem Raumzustand steht — nichts davon schickt der Server im `RoomState`.
 */
data class Raumneben(
    /** Die letzte Bevölkerungswarnung — das Band, bis jemand es wegtippt. */
    val warnung: String? = null,
    val warnungUm: Long = 0,
    /** Welche Nebenleitung gerade das Mikrofon hat — `draht`, `stelle`, `notruf` … */
    val sendetAuf: String? = null,
    /** Wer gerade am Draht bzw. an der Einsatzstelle spricht — `null`: frei. */
    val drahtSprecher: String? = null,
    val stellenSprecher: String? = null,
    /** Der Server übersetzt gerade die gesprochene Rückfrage bzw. den Einzelrufsatz. */
    val notrufVersteht: Boolean = false,
    val einzelrufVersteht: Boolean = false,
    /** Der Anruf, der gerade am eigenen Apparat liegt. */
    val telefonAnruf: String? = null,
    val raumwachen: List<Raumwache> = emptyList(),
    val stichwortsets: List<Stichwortset> = emptyList(),
    val rundenvorlagen: List<Rundenvorlagenzeile> = emptyList(),
    val dauerVorlagen: List<AaoVorlagenzeile> = emptyList(),
    /** Die Rückmeldung nach dem Sichern einer Vorlage. */
    val vorlageMeldung: String? = null,
    /** Die echten Wachen des Kreises — für die Wachenmaske der Lobby. */
    val kreiswachen: List<Kreiswache> = emptyList(),
    /** Für welchen Kreis `kreiswachen` geladen ist — `null`: noch keiner. */
    val kreiswachenFuer: String? = null,
    /** Echte Straßen des Kreises — die Vorschläge im Adressfeld. */
    val strassen: List<String> = emptyList(),
    val strassenFuer: String? = null,
    /**
     * Ob der Server die KI-Schalter überhaupt anbietet (`kiFunk` in `/api/version`).
     * Ohne Antwort `false` — dann stehen KI-Funk, KI-Anrufe und KI-Lagen gar nicht da.
     */
    val kiFunkVerfuegbar: Boolean = false,
)

/**
 * Eine Zeile der Funkgruppen-Maske, wie sie an den Hub geht (`FunkgruppeEingabe`).
 * Leere Id heißt „vergib eine" — der Server tut das, damit zwei Geräte nicht
 * dieselbe erfinden.
 */
data class Funkgruppenentwurf(
    val id: String = "",
    val nummer: String = "",
    val name: String = "",
    val organisationen: List<String> = emptyList(),
    val hiOrgs: List<String> = emptyList(),
    val fuehrung: Boolean = false,
)

/**
 * Eine Änderung der Rundeneinstellungen — was `null` bleibt, bleibt unberührt.
 *
 * Die Werte sind die Aufzählungsnamen des Servers als Text (`Zufall`, `Dicht`,
 * `Gelegentlich` …); ein unbekannter Name fällt dort still auf „nicht mitgeschickt".
 */
data class Einstellungsaenderung(
    val mode: String? = null,
    val ort: String? = null,
    val leitstelle: String? = null,
    val einsatzIntervallSekunden: Int? = null,
    val organisationen: List<String>? = null,
    val botTempo: String? = null,
    val hiOrgs: List<String>? = null,
    val freischaltungenIgnorieren: Boolean? = null,
    val oeffentlich: Boolean? = null,
    val botGespraechigkeit: String? = null,
    val botFunkAktiv: Boolean? = null,
    val zeitmodus: String? = null,
    val telefonischeLeitstelle: Boolean? = null,
    val stoerungshaeufigkeit: String? = null,
    val tagesalarmstaerke: Boolean? = null,
    val loeschwasser: Boolean? = null,
    val sonderobjekte: Boolean? = null,
    val wiederherstellung: Boolean? = null,
    val gefahrgutlagen: Boolean? = null,
    val jahreszeit: String? = null,
    val silvester: Boolean? = null,
    val verlegungsfahrten: Boolean? = null,
    val suchlagen: Boolean? = null,
    val einsatzarbeit: Boolean? = null,
    val vegetationsbraende: Boolean? = null,
    val arbeitsfunk: String? = null,
    val einsatzende: String? = null,
    val einsatzleitung: Boolean? = null,
    val maxSpieler: Int? = null,
    val wachennummerStellen: Int? = null,
    val laufnummerStellen: Int? = null,
    val funkverstossSchwelle: Int? = null,
    val einsatzdichte: String? = null,
    val kiFunkAktiv: Boolean? = null,
    /** Leerer Text setzt auf den Grundkatalog zurück. */
    val stichwortsetId: String? = null,
    /** Ersetzt die ganze Liste; leer heißt „alle Wachen des Kreises". */
    val wachen: List<Wachenwahl>? = null,
    /** Ersetzt das ganze Wörterbuch; leere Werte heißen „wie im Buch". */
    val rufnamenpraefixe: Map<String, String>? = null,
    /** Ersetzt die ganze Liste; leer heißt „ein Kanal für alle". */
    val funkgruppen: List<Funkgruppenentwurf>? = null,
    val wachnummern: Map<String, Int>? = null,
    /**
     * Die Übertragung. Beim Einschalten gehen alle vier zusammen: Der Server weist
     * „an" ohne Kanal ab, übernimmt Kanal, Plattform und Aufzeichnung aber vor dem
     * Schalter — ein Griff genügt.
     */
    val streamermodus: Boolean? = null,
    val streamerplattform: String? = null,
    val streamerkanal: String? = null,
    val streameraufzeichnung: Boolean? = null,
    /** Notrufer sprechen frei (KI, Premium) — gibt es erst ab 5.0.0.26 am Server. */
    val kiAnrufeAktiv: Boolean? = null,
    /** Die KI-Leitstelle besetzt den leeren Tisch (Premium). */
    val kiLeitstelleAktiv: Boolean? = null,
    /** Im Zufallsmodus erfindet eine KI die Lagen (Premium). */
    val kiLagenAktiv: Boolean? = null,
)
