package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.netz.Begleiterbeitritt
import de.pagerspass.pagerspass.netz.Begleitergeraete
import de.pagerspass.pagerspass.netz.Funkgruppe
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Funkzeile
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
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

/**
 * Der mobile Begleiter — das Gegenstück zu `web/src/views/BegleiterView.vue`.
 *
 * <b>Was er ist.</b> Wer am Rechner Dienst macht, erzeugt dort über das
 * Handy-Symbol einen QR-Code. Wird er gescannt, hängt dieses Gerät als
 * *zweiter Bildschirm desselben Platzes* an der laufenden Runde: Funkgerät,
 * Funkchat und Melder liegen dann in der Hand, der Rechner blendet sie aus. Es
 * entsteht **kein zweiter Spieler** — der Token gehört einem Platz in einer
 * Runde, gilt acht Stunden und trägt keine Kontokennung.
 *
 * <b>Warum eine eigene Verbindung neben [Runde].</b> Der Begleiter ist keine
 * Runde, die man selbst fährt: Der Hub markiert die Verbindung als
 * `IstBegleiter` und lässt dann nur noch sieben Kommandos durch (Sprechtaste,
 * Funkspruch, Alarm quittieren, Funkgruppe schalten, Vorlesen). Beides in einem
 * ViewModel hieße, eine Verbindung zwischen zwei Rollen umzuschalten — und die
 * Rolle steht am Server, nicht am Client.
 *
 * <b>Was der Begleiter nicht darf</b>, hat am Server eigene Riegel: Draht,
 * Notruf, Einsatzstellenkanal, Alarmvorschlag. Ein Schalter, dessen Kommando
 * nicht auf der Positivliste steht, tut **still** gar nichts — der Server
 * protokolliert nur eine Warnung. Deshalb steht hier auch keiner.
 *
 * <b>Die Geräte werden gespiegelt, nicht gewählt.</b> Bauart (Piepser oder
 * Alarm-App), Bauform, Gehäuse, Alarmton und Meldergesicht kommen vom Rechner:
 * einmal mit dem Token, danach bei jeder Änderung als `BegleiterGeraete`. Ein
 * Dienstbildschirm ist kein Einstellungsmenü — eingestellt wird im Konto.
 */
class Begleiter(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val draht = Funkverbindung(ablage)
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mikrofon = Mikrofon()
    private val lautsprecher = Lautsprecher()
    private var funklimit: Job? = null

    /**
     * Ob die Begleiteransicht gerade im Vordergrund liegt.
     *
     * Der Wert muss die Verbindung überleben: Nach einem Netzabriss besitzt der
     * Server eine neue Verbindungskennung und kennt deren Sichtbarkeit noch
     * nicht. Gerade wenn das Handy dabei in der Tasche liegt, darf es nicht als
     * sichtbar gelten — sonst bleibt der nächste Melder-Push aus.
     */
    @Volatile
    private var sichtbar = true

    private val _stand = MutableStateFlow(Begleiterstand())
    val stand: StateFlow<Begleiterstand> = _stand.asStateFlow()

    /**
     * Der Haken für die Systemmeldung — derselbe wie in [Runde].
     *
     * <b>Er ist beim Begleiter wichtiger als dort.</b> Ein Melder liegt auf dem
     * Tisch mit dunklem Bildschirm; im Web musste dafür eigens Web Push
     * einspringen. Hier reicht die Meldung des Systems, die der Rahmen zeigt.
     */
    var beiAlarm: ((Alarmmeldung) -> Unit)? = null

    init {
        draht.beiLage = { lage ->
            _stand.update { it.copy(lage = lage) }

            // <b>Nach einem Abriss muss neu gekoppelt werden.</b> Die Verbindung
            // kommt von selbst zurück, der Platz nicht: `JoinBegleiter` setzt
            // den Raumkontext dieser Verbindung, und ohne ihn hängt das Handy
            // zwar am Server, aber an keiner Runde — LIVE, ohne dass der Melder
            // je wieder piepste. Genau das macht der Web-Client in seinem
            // `onreconnected` auch.
            if (lage == Funkverbindung.Lage.Verbunden && _stand.value.gekoppelt) {
                bereich.launch { runCatching { beitreten(_stand.value.token) } }
            }
        }

        draht.auf("RoomState") { argumente ->
            argumente.firstOrNull()?.let { setzen(it) }
        }

        // <b>Der Alarm ist ein Ereignis, kein Zustand.</b> Nur er öffnet den
        // Melder; wer ihn am Raumzustand aufhängt, weckt ihn bei jedem
        // Wiederverbinden erneut. Und: Was vor der Kopplung lief, kommt nicht
        // nach — `melderverlauf` beginnt mit dem Augenblick, in dem das Gerät in
        // der Hand liegt.
        draht.auf("Alarm") { argumente ->
            val alarm = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Alarmmeldung.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update {
                it.copy(alarm = alarm, melderverlauf = (it.melderverlauf + alarm).takeLast(50))
            }
            beiAlarm?.invoke(alarm)
        }

        draht.auf("Radio") { argumente ->
            val zeile = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Funkzeile.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update { alt ->
                if (alt.funk.any { it.id == zeile.id }) alt
                else alt.copy(funk = (alt.funk + zeile).takeLast(FUNKZEILEN_BEGLEITER))
            }
        }

        draht.auf("Rejected") { argumente ->
            val grund = argumente.firstOrNull()?.let { text(it) }
            _stand.update { it.copy(fehler = grund ?: "Der Beitritt wurde abgelehnt.") }
        }

        draht.auf("KickedFromRoom") { argumente ->
            val grund = argumente.firstOrNull()?.let { text(it) }
            beenden(grund ?: "Die Leitstelle hat den Platz geräumt.")
        }

        /**
         * Der Rechner hat im Profil umgestellt — der Begleiter zieht nach.
         *
         * Ohne dieses Ereignis trüge das Handy für den Rest der Schicht das
         * Gerät von dem Augenblick, in dem der QR-Code entstand.
         */
        draht.auf("BegleiterGeraete") { argumente ->
            val geraete = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Begleitergeraete.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update { it.copy(geraete = geraete) }
        }

        // ------------------------------------------------------- Sprechfunk
        //
        // Derselbe Weg wie im Dienst: Der Server reicht die 40-ms-Pakete
        // unverändert weiter. Der Begleiter *ist* der Funkplatz, solange er
        // hängt — am Rechner ist der Funkbereich dann ausgeblendet.

        draht.auf("Audio") { argumente ->
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            lautsprecher.abspielen(paket)
        }

        draht.auf("TransmissionStarted") { argumente ->
            val name = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            val gruppe = argumente.getOrNull(2)?.let { text(it) } ?: ""
            _stand.update { it.copy(sprecher = it.sprecher + (gruppe to name)) }
        }

        draht.auf("TransmissionEnded") { argumente ->
            val gruppe = argumente.getOrNull(1)?.let { text(it) } ?: ""
            _stand.update { it.copy(sprecher = it.sprecher - gruppe) }
        }

        draht.auf("ChannelBusy") { argumente ->
            val name = argumente.firstOrNull()?.let { text(it) }
            sprechenAbbrechen()
            _stand.update {
                it.copy(funkhinweis = "Kanal belegt – ${name ?: "jemand"} spricht.")
            }
        }

        draht.auf("FunkErkannt") { argumente ->
            val erkannt = argumente.firstOrNull()?.let { text(it) }
            _stand.update { it.copy(wirdVerstanden = false) }
            if (!erkannt.isNullOrBlank()) funken(erkannt)
        }
    }

    // ------------------------------------------------------------- Koppeln

    /**
     * Den gescannten QR-Code annehmen und den Platz übernehmen.
     *
     * @param eingabe Was die Kamera gelesen hat oder was jemand eingetippt hat —
     *   die volle Adresse (`https://…/play/mobile/funk/<token>`) oder der Token
     *   allein. Beides ist erlaubt: Der Rechner bietet neben dem QR-Code auch
     *   „Direkten Link kopieren" an, und der landet als Text hier.
     */
    fun koppeln(eingabe: String) = viewModelScope.launch {
        val token = tokenAus(eingabe)
        if (token == null) {
            _stand.update {
                it.copy(fehler = "Das ist kein Begleiter-Code. Scanne den QR-Code am Rechner.")
            }
            return@launch
        }

        /*
         * <b>Der Token gehört einem Server, nicht dem Spiel.</b> Er liegt allein
         * im Arbeitsspeicher der Instanz, die ihn ausgegeben hat. Wer am
         * Vorabstand spielt und die App auf den Betrieb gestellt hat, bekäme
         * sonst „Dieser QR-Link ist ungültig oder abgelaufen" — und suchte den
         * Fehler beim Code, obwohl nur die Adresse nicht zusammenpasst.
         */
        val eigener = ablage.server()
        val fremder = serverAus(eingabe)
        if (fremder != null && !gleicherServer(fremder, eigener)) {
            _stand.update {
                it.copy(
                    fehler = "Dieser Code gehört zu ${kurz(fremder)}, die App spricht mit " +
                        "${kurz(eigener)}. Stell den Server auf der Anmeldeseite um.",
                )
            }
            return@launch
        }

        _stand.update { Begleiterstand(token = token, laeuft = true) }

        runCatching {
            draht.verbinden()
            beitreten(token)
        }.onFailure { f ->
            draht.trennen()
            _stand.update {
                Begleiterstand(fehler = f.message ?: "Die Kopplung ist fehlgeschlagen.")
            }
        }
        _stand.update { it.copy(laeuft = false) }
    }

    /**
     * Zurück an den Platz, an dem das Gerät vor einem Prozesstod hing.
     *
     * <b>Ein Melder, der das Beenden der App nicht überlebt, ist keiner.</b>
     * Android räumt eine App im Hintergrund weg, wann es will — und genau dann
     * liegt das Handy auf dem Tisch und soll piepsen. Der Token liegt deshalb in
     * der Ablage; er gilt acht Stunden.
     *
     * <b>Ein abgelaufener Token ist keine Meldung wert.</b> Wer am nächsten Tag
     * die App öffnet, will nicht mit „Dieser QR-Link ist ungültig" begrüßt
     * werden — der Eintrag wird still weggeräumt.
     */
    fun wiederAufnehmen() = viewModelScope.launch {
        if (_stand.value.gekoppelt) return@launch
        val token = ablage.offenerBegleiter() ?: return@launch

        runCatching {
            draht.verbinden()
            beitreten(token)
        }.onFailure {
            ablage.begleiterMerken(null)
            draht.trennen()
            _stand.update { Begleiterstand() }
        }
    }

    /** Der eigentliche Beitritt — geteilt zwischen Kopplung und Rückkehr. */
    private suspend fun beitreten(token: String) {
        val antwort = draht.frage("JoinBegleiter", wert(token))
        val ergebnis = antwort?.let {
            runCatching {
                Netz.abgabe.decodeFromJsonElement(Begleiterbeitritt.serializer(), it)
            }.getOrNull()
        } ?: throw IllegalStateException("Die Leitstelle antwortet nicht.")

        if (!ergebnis.ok || ergebnis.state == null) {
            throw IllegalStateException(
                ergebnis.fehler ?: "Dieser QR-Link ist ungültig oder abgelaufen.",
            )
        }

        _stand.update {
            it.copy(
                token = token,
                gekoppelt = true,
                raum = ergebnis.state,
                spielerId = ergebnis.playerId.orEmpty(),
                name = ergebnis.name.orEmpty(),
                geraete = ergebnis.geraete ?: it.geraete,
                funk = ergebnis.state.funkprotokoll,
                fehler = null,
            )
        }

        ablage.begleiterMerken(token)

        // Die App hat keine eigene Spracherkennung — der Server soll Durchsagen
        // in Text übersetzen (derselbe Wunsch wie im Dienst).
        draht.rufen("FunkerkennungWuenschen", wert(true))

        // Sichtbarkeit hängt serverseitig an der Verbindungskennung. Deshalb
        // nach jedem Join erneut melden, auch nach einer Wiederverbindung.
        draht.rufen("SetzeSichtbarkeit", wert(sichtbar))
    }

    /**
     * Ob der Bildschirm gerade zu sehen ist.
     *
     * <b>Der Server braucht das für den Melder in der Tasche.</b> Ist der
     * Begleiter verdeckt, springt Push ein — auch wenn am Rechner sichtbar
     * weitergespielt wird. Der Anwesenheitsstand des *Rechnerplatzes* wird davon
     * nicht angefasst: Tasche zu ist nicht Dienst beendet.
     */
    fun sichtbarkeit(sichtbar: Boolean) {
        this.sichtbar = sichtbar
        if (!_stand.value.gekoppelt) return
        draht.rufen("SetzeSichtbarkeit", wert(sichtbar))
    }

    /** Die Kopplung lösen — der Rechner bekommt Funk und Melder zurück. */
    fun trennen() = viewModelScope.launch {
        sprechenAbbrechen()
        lautsprecher.schliessen()
        runCatching { draht.rufen("Leave") }
        draht.trennen()
        ablage.begleiterMerken(null)
        _stand.value = Begleiterstand()
    }

    fun fehlerWegnehmen() = _stand.update { it.copy(fehler = null) }

    fun funkhinweisWegnehmen() = _stand.update { it.copy(funkhinweis = null) }

    // ------------------------------------------------------------- Der Dienst

    /** Den Alarm am Melder bestätigen — eines der sieben erlaubten Kommandos. */
    fun alarmQuittieren() {
        draht.rufen("AcknowledgeAlarm")
        _stand.update { it.copy(alarm = null) }
    }

    /** Den Melder wegtippen, ohne zu quittieren — der Alarm bleibt offen. */
    fun alarmWegtippen() = _stand.update { it.copy(alarm = null) }

    fun funken(text: String) {
        if (text.isBlank()) return
        draht.rufen("SendRadio", wert(text.trim()), JsonNull)
    }

    /**
     * Auf eine andere Funkgruppe schalten — `null` heißt zurück auf die
     * Stammgruppe des Fahrzeugs.
     *
     * <b>Zwei verschiedene Griffe, wie am Rechner.</b> Die Besatzung dreht am
     * eigenen Gerät (`FunkgruppeZuweisen`), der Disponent legt seinen Platz auf
     * eine Gruppe (`FunkgruppenPlatz`) — und hört sie dabei zwangsläufig mit,
     * sonst bekäme er die Antwort auf den eigenen Spruch nicht. Eine
     * Stammgruppe hat ein Leitstellenplatz nicht.
     *
     * <b>Warum das überhaupt auf dem Handy steht.</b> Der Rechner blendet seine
     * Funkgruppenwahl aus, sobald ein Handy hängt. Ohne diese Tasten käme ein
     * gekoppelter Platz an keinen anderen Kanal mehr — weder hier noch dort.
     */
    fun funkgruppeSchalten(gruppeId: String?) {
        val s = _stand.value
        if (s.istLeitstelle) {
            if (gruppeId == null) return
            val hoeren = (s.raum?.gehoerteFunkgruppen.orEmpty() + gruppeId).distinct()
            draht.rufen("FunkgruppenPlatz", liste(hoeren), wert(gruppeId))
            return
        }

        val eigenes = s.meinFahrzeug ?: return
        draht.rufen(
            "FunkgruppeZuweisen",
            wert(eigenes.id),
            gruppeId?.let { wert(it) } ?: JsonNull,
        )
    }

    /**
     * Der Server liest den Funkverkehr vor.
     *
     * Am Rechner ist das ein Schalter im Funkbereich — und er stand am Handy
     * monatelang sichtbar da und tat nichts, weil sein Kommando nicht auf der
     * Positivliste des Hubs stand. Seit 4.0.3.4 steht es dort.
     */
    fun vorlesen(an: Boolean) {
        draht.rufen("FunkVorlesen", wert(an))
        _stand.update { it.copy(vorlesen = an) }
    }

    // ------------------------------------------------------------ Sprechfunk
    //
    // Zeichengleich zu `Runde` — dieselben drei Schritte, dieselben zwei Uhren.
    // Abgeschrieben und nicht geteilt: Der Begleiter hat einen anderen Draht,
    // und eine gemeinsame Basisklasse für zwei Verbindungen wäre der Anfang
    // genau der Kopplung, die es hier nicht geben soll.

    /** @return false, wenn nicht gesendet wird (belegt, Sendepause, Mikrofon). */
    fun sprechenStarten(): Boolean {
        val jetzt = System.currentTimeMillis()
        val s = _stand.value
        if (s.sendet) return true
        if (s.sprecher.isNotEmpty()) {
            _stand.update {
                it.copy(funkhinweis = "Kanal belegt – ${s.sprecher.values.first()} spricht.")
            }
            return false
        }
        val gesperrtBis = s.funkGesperrtBis
        if (gesperrtBis != null && gesperrtBis > jetzt) return false

        draht.rufen("StartTransmission")
        val los = mikrofon.starten { paket -> draht.rufen("SendAudio", wert(paket)) }
        if (!los) {
            draht.rufen("EndTransmission", wert(true))
            _stand.update { it.copy(funkhinweis = "Das Mikrofon war nicht zu bekommen.") }
            return false
        }

        _stand.update { it.copy(sendet = true, funkhinweis = null) }

        funklimit = bereich.launch {
            delay(Sprechfunk.MAX_SENDEDAUER_MS)
            if (_stand.value.sendet) {
                mikrofon.stoppen()
                draht.rufen("FunklimitErreicht")
                _stand.update {
                    it.copy(
                        sendet = false,
                        funkGesperrtBis = System.currentTimeMillis() + Sprechfunk.SENDEPAUSE_MS,
                        funkhinweis = "Funklimit erreicht — eine Minute Sendepause.",
                    )
                }
            }
        }
        return true
    }

    fun sprechenBeenden() {
        if (!_stand.value.sendet) return
        funklimit?.cancel()
        mikrofon.stoppen()

        val leise = mikrofon.spitzenpegel < 0.01f
        _stand.update {
            it.copy(
                sendet = false,
                funkhinweis = when {
                    mikrofon.pakete == 0 -> "Es kam kein Ton an — Mikrofon prüfen."
                    leise -> "Die Durchsage war fast still — Mikrofon prüfen."
                    else -> it.funkhinweis
                },
            )
        }

        bereich.launch {
            val antwort = runCatching { draht.frage("EndTransmission", wert(true)) }.getOrNull()
            val erkanntFolgt = (antwort as? JsonPrimitive)?.content == "true"
            if (erkanntFolgt) _stand.update { it.copy(wirdVerstanden = true) }
        }
    }

    private fun sprechenAbbrechen() {
        if (!_stand.value.sendet) return
        funklimit?.cancel()
        mikrofon.stoppen()
        _stand.update { it.copy(sendet = false) }
    }

    // ------------------------------------------------------------- Innereien

    /** Die Kopplung ist zu Ende — mit einem Satz, der erklärt, warum. */
    private fun beenden(grund: String) = viewModelScope.launch {
        sprechenAbbrechen()
        lautsprecher.schliessen()
        draht.trennen()
        ablage.begleiterMerken(null)
        _stand.value = Begleiterstand(fehler = grund)
    }

    private fun setzen(roh: JsonElement) {
        val raum = runCatching {
            Netz.abgabe.decodeFromJsonElement(Raumzustand.serializer(), roh)
        }.getOrNull() ?: return

        _stand.update { it.copy(raum = raum, funk = raum.funkprotokoll) }
    }

    private fun text(roh: JsonElement): String? = (roh as? JsonPrimitive)?.content

    private fun liste(werte: List<String>): JsonElement =
        buildJsonArray { werte.forEach { add(JsonPrimitive(it)) } }

    override fun onCleared() {
        super.onCleared()
        mikrofon.stoppen()
        lautsprecher.schliessen()
    }
}

/**
 * Was über die Kopplung bekannt ist.
 *
 * <b>Kein `Rundenstand`.</b> Vieles sieht gleich aus (Funk, Alarm, Sprecher),
 * aber die Hälfte des Rundenstands gibt es hier nicht — keine Lobby, keine
 * Gutschrift, kein Zuschauerplatz, kein Draht —, und dafür gibt es hier den
 * gespiegelten Gerätestand. Ein gemeinsamer Datentyp wäre einer, bei dem man an
 * jedem Feld nachsehen müsste, ob es in diesem Fall überhaupt gefüllt ist.
 */
data class Begleiterstand(
    /** Der QR-Token dieser Kopplung — leer, solange keine steht. */
    val token: String = "",
    /** Ob der Platz übernommen ist. Erst dann darf gefunkt werden. */
    val gekoppelt: Boolean = false,
    val raum: Raumzustand? = null,
    /** Der Platz, an dem dieses Gerät hängt — nicht das eigene Konto. */
    val spielerId: String = "",
    /** Wie der Platz heißt, an dem dieses Gerät hängt. */
    val name: String = "",
    /** Der gespiegelte Gerätestand des Rechners (siehe Kopf von [Begleiter]). */
    val geraete: Begleitergeraete = Begleitergeraete(),
    val lage: Funkverbindung.Lage = Funkverbindung.Lage.Getrennt,
    val laeuft: Boolean = false,
    val fehler: String? = null,
    /** Der Alarm, der gerade am Melder hängt. */
    val alarm: Alarmmeldung? = null,
    /**
     * Was der Melder seit der Kopplung bekommen hat.
     *
     * <b>Er beginnt bei der Kopplung, nicht bei Dienstbeginn.</b> Alarme kommen
     * nur an verbundene Begleiter; wer sein Handy mitten in der Schicht scannt,
     * sieht einen ruhenden Melder — das ist kein Fehler, sondern die Wahrheit
     * über dieses Gerät.
     */
    val melderverlauf: List<Alarmmeldung> = emptyList(),
    val funk: List<Funkzeile> = emptyList(),
    val sendet: Boolean = false,
    val wirdVerstanden: Boolean = false,
    val sprecher: Map<String, String> = emptyMap(),
    val funkGesperrtBis: Long? = null,
    val funkhinweis: String? = null,
    /** Ob der Server den Funkverkehr vorliest. */
    val vorlesen: Boolean = false,
) {
    val spieler get() = raum?.players?.firstOrNull { it.id == spielerId }

    val istLeitstelle: Boolean get() = spieler?.istLeitstelle == true

    val meinFahrzeug: Rundenfahrzeug?
        get() = raum?.vehicles?.firstOrNull { it.playerId == spielerId }

    /** Wie der Platz im Funk heißt — Rufname des Fahrzeugs oder Leitstelle. */
    val meinRufname: String
        get() = meinFahrzeug?.funkrufname
            ?: raum?.settings?.leitstelle?.takeIf { istLeitstelle }
            ?: name

    /** Ob diese Runde überhaupt getrennte Funkverkehrskreise hat. */
    val funkgruppenGetrennt: Boolean
        get() = raum?.settings?.funkgruppen.orEmpty().isNotEmpty()

    /**
     * Die Gruppen, auf die dieses Gerät schalten darf.
     *
     * Die dynamischen Einsatzstellengruppen (DMO) nur bei der eigenen Lage:
     * Direktbetrieb ist Gerät zu Gerät vor Ort — ein Disponent ist dort nicht
     * dabei und hätte auf dem Kanal nichts zu hören.
     */
    val waehlbareFunkgruppen: List<Funkgruppe>
        get() {
            val eigene = meinFahrzeug?.einsatzId
            return raum?.settings?.funkgruppen.orEmpty()
                .filter { it.einsatzId == null || it.einsatzId == eigene }
        }

    /** Auf welcher Gruppe gesendet wird; `null` = der eine Kanal. */
    val sendegruppe: String? get() = raum?.sendegruppe

    /** Ob das Gerät unverändert auf der Stammgruppe seines Fahrzeugs steht. */
    val stehtAufStamm: Boolean get() = meinFahrzeug?.funkgruppeAufgeschaltet != true

    fun funkgruppeVon(id: String?): Funkgruppe? =
        id?.let { gesucht -> raum?.settings?.funkgruppen.orEmpty().firstOrNull { it.id == gesucht } }

    /** Wer gerade auf dem eigenen Kanal spricht — `null` heißt: Kanal frei. */
    val belegtVon: String? get() = sprecher.values.firstOrNull()

    /** Ob die Runde vorbei ist. Dann sind Funk und Melder außer Dienst. */
    val beendet: Boolean get() = raum?.beendet == true
}

/**
 * Den Token aus dem herausziehen, was die Kamera gelesen hat.
 *
 * Der QR-Code trägt die volle Adresse (`…/play/mobile/funk/<token>`), weil er
 * am Rechner auch für ein Handy ohne App gedacht ist. Ein getippter oder
 * geteilter Token allein geht ebenso.
 *
 * <b>Geprüft wird nur die Form</b> — 64 Hexzeichen, wie sie
 * `BegleiterVerbindungen.Erzeugen` vergibt. Ob der Token gilt, weiß allein der
 * Server; hier geht es darum, einen versehentlich gescannten fremden QR-Code
 * (WLAN, Paketaufkleber) nicht als Kopplungsversuch an den Hub zu schicken.
 */
fun tokenAus(eingabe: String): String? {
    val roh = eingabe.trim()
    if (roh.isEmpty()) return null

    val stueck = roh
        .substringBefore('?')
        .substringBefore('#')
        .trimEnd('/')
        .substringAfterLast('/')

    return stueck.takeIf { it.length == 64 && it.all { z -> z.isDigit() || z in 'a'..'f' } }
}

/**
 * Zu welchem Server der gescannte Code gehört — `null` bei einem nackten Token.
 *
 * Der QR-Code trägt die Adresse des Rechners, an dem er entstand. Sie ist die
 * einzige Auskunft darüber, welche Instanz den Token kennt.
 */
fun serverAus(eingabe: String): String? {
    val roh = eingabe.trim()
    if (!roh.startsWith("http://") && !roh.startsWith("https://")) return null

    val ohneSchema = roh.substringAfter("://")
    val wirt = ohneSchema.substringBefore('/').substringBefore('?').substringBefore('#')
    return if (wirt.isBlank()) null else roh.substringBefore("://") + "://" + wirt
}

/**
 * Ob zwei Serveradressen dieselbe Instanz meinen.
 *
 * Verglichen wird der Wirt ohne Schema und ohne abschließenden Schrägstrich:
 * `https://pagerspass.de/` und `https://pagerspass.de` sind dasselbe, und beim
 * Entwickeln kommt derselbe Rechner mal als `localhost`, mal als `127.0.0.1`.
 */
private fun gleicherServer(einer: String, anderer: String): Boolean {
    fun wirt(adresse: String) = adresse
        .substringAfter("://")
        .trimEnd('/')
        .lowercase()
        .replace("127.0.0.1", "localhost")

    return wirt(einer) == wirt(anderer)
}

/** Die Adresse ohne Schema — so, wie ein Mensch sie nennt. */
private fun kurz(adresse: String): String = adresse.substringAfter("://").trimEnd('/')

/**
 * Wie viele Funkzeilen der Begleiter hält.
 *
 * Der Server schickt live ohnehin nur die letzten 150 — dieselbe Zahl wie im
 * Dienst, aus demselben Grund.
 */
private const val FUNKZEILEN_BEGLEITER = 200
