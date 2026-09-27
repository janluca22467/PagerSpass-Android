package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.netz.Begleiterbeitritt
import de.pagerspass.pagerspass.netz.Begleitergeraete
import de.pagerspass.pagerspass.netz.Einzelruf
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
     * Der Hörer des Einzelrufs — ein eigener Lautsprecher, damit Funk und
     * Telefonat sich nicht denselben Abspielpuffer teilen.
     */
    private val einzelrufLautsprecher = Lautsprecher()

    /**
     * Für welchen Ruf das Mikrofon gerade offen ist. Die Ruf-Id und nicht nur ein
     * Ja/Nein: Endet ein Gespräch und beginnt im selben Zustand ein neues
     * (auflegen, sofort zurückrufen), sähe ein bloßes „läuft" keinen Wechsel.
     */
    @Volatile
    private var einzelrufMikroFuer: String? = null

    /** Ob gerade eine Äußerung an eine Bot-Besatzung aufgenommen wird. */
    @Volatile
    private var amEinzelrufSprechen = false

    /** Die Toneinstellungen überleben den Neustart — wie im Web im Melder-Speicher. */
    private val tonablage = anwendung.getSharedPreferences("pagerspass-begleiter-ton", 0)

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
        _stand.update { mitTon(it) }
        pegelAnwenden()

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

            geraeteUebernehmen(geraete)
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

        // -------------------------------------------------------- Einzelruf
        //
        // Nur die Gegenstelle des eigenen laufenden Gesprächs kommt durch — ein
        // spätes Paket eines eben beendeten Rufs soll nicht ins nächste Gespräch
        // hineinsprechen. Der Server schickt an Rechner *und* Handy eines Platzes;
        // gespielt wird nur dort, wo der Einzelruf stattfindet (`einzelrufHier`).
        draht.auf("EinzelrufAudio") { argumente ->
            val von = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            val s = _stand.value
            val ruf = s.eigenerEinzelruf ?: return@auf
            val gegenstelle = if (ruf.vonPlayerId == s.spielerId) ruf.angenommenVonPlayerId else ruf.vonPlayerId
            if (von != gegenstelle) return@auf
            einzelrufLautsprecher.abspielen(paket)
        }

        // Was der Server aus der Äußerung an eine Bot-Besatzung verstanden hat.
        draht.auf("EinzelrufErkannt") { argumente ->
            val rufId = argumente.getOrNull(0)?.let { text(it) }
            val erkannt = argumente.getOrNull(1)?.let { text(it) }
            _stand.update { it.copy(einzelrufErkennung = false) }

            if (erkannt.isNullOrBlank()) {
                _stand.update { it.copy(funkhinweis = "Nichts verstanden — der Satz wurde nicht gesagt.") }
                return@auf
            }
            // Wer inzwischen aufgelegt hat, darf seinen letzten Satz nicht im
            // nächsten Gespräch wiederfinden.
            if (rufId == null || rufId != _stand.value.laufenderEinzelruf?.id) return@auf
            einzelrufSagen(rufId, erkannt)
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

        _stand.update { mitTon(Begleiterstand(token = token, laeuft = true)) }

        runCatching {
            draht.verbinden()
            beitreten(token)
        }.onFailure { f ->
            draht.trennen()
            _stand.update {
                mitTon(Begleiterstand(fehler = f.message ?: "Die Kopplung ist fehlgeschlagen."))
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
            _stand.update { mitTon(Begleiterstand()) }
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
                funk = ergebnis.state.funkprotokoll,
                fehler = null,
            )
        }
        ergebnis.geraete?.let { geraeteUebernehmen(it) }
        einzelrufNachziehen()

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
        einzelrufAufraeumen()
        lautsprecher.schliessen()
        runCatching { draht.rufen("Leave") }
        draht.trennen()
        ablage.begleiterMerken(null)
        _stand.value = mitTon(Begleiterstand())
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
        // Ein Mikrofon, zwei Wege: Wer telefoniert, funkt nicht zugleich.
        if (einzelrufMikroFuer != null || amEinzelrufSprechen) {
            _stand.update { it.copy(funkhinweis = "Im Einzelruf — erst auflegen, dann funken.") }
            return false
        }
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
        einzelrufAufraeumen()
        lautsprecher.schliessen()
        draht.trennen()
        ablage.begleiterMerken(null)
        _stand.value = mitTon(Begleiterstand(fehler = grund))
    }

    private fun setzen(roh: JsonElement) {
        val raum = runCatching {
            Netz.abgabe.decodeFromJsonElement(Raumzustand.serializer(), roh)
        }.getOrNull() ?: return

        _stand.update { it.copy(raum = raum, funk = raum.funkprotokoll) }
        einzelrufNachziehen()
    }

    private fun text(roh: JsonElement): String? = (roh as? JsonPrimitive)?.content

    private fun liste(werte: List<String>): JsonElement =
        buildJsonArray { werte.forEach { add(JsonPrimitive(it)) } }

    override fun onCleared() {
        super.onCleared()
        mikrofon.stoppen()
        lautsprecher.schliessen()
        einzelrufLautsprecher.schliessen()
    }

    // ----------------------------------------------------- Der Gerätestand

    /**
     * Den gespiegelten Gerätestand übernehmen — samt der Wahl, was der Begleiter
     * zeigt, und ob er der Funkplatz ist.
     *
     * Nur echte Angaben zählen: `null` kommt von einem Rechner, der die Wahl noch
     * nicht kannte, und lässt stehen, was hier gilt.
     */
    private fun geraeteUebernehmen(g: Begleitergeraete) {
        _stand.update {
            it.copy(
                geraete = g,
                zeigtMelder = g.zeigtMelder ?: it.zeigtMelder,
                zeigtFunkgeraet = g.zeigtFunkgeraet ?: it.zeigtFunkgeraet,
                zeigtFunkchat = g.zeigtFunkchat ?: it.zeigtFunkchat,
                istFunkplatz = g.funkAusgelagert ?: it.istFunkplatz,
            )
        }
        einzelrufNachziehen()
    }

    // ------------------------------------------------------------ Einzelruf
    //
    // Das Telefonat über das Funkgerät. Die Vermittlung liegt am Server
    // (`GameRoom.Einzelrufe`); hier stehen nur die Handgriffe — und dass das
    // Mikrofon dem Gespräch folgt. Der Hub lässt sie vom Begleiter durch, solange
    // das Handy der Funkplatz ist (`GameHub.Mit`).

    fun einzelrufStarten(zielVehicleId: String?) {
        draht.rufen("EinzelrufStarten", zielVehicleId?.let { wert(it) } ?: JsonNull)
    }

    fun einzelrufAnnehmen(rufId: String) = draht.rufen("EinzelrufAnnehmen", wert(rufId))

    fun einzelrufAbweisen(rufId: String) = draht.rufen("EinzelrufAbweisen", wert(rufId))

    fun einzelrufBeenden(rufId: String) = draht.rufen("EinzelrufBeenden", wert(rufId))

    /** Ein Satz ins Gespräch — nur mit einer Bot-Besatzung; zwischen Menschen trägt die Leitung Stimme. */
    fun einzelrufSagen(rufId: String, text: String) {
        if (text.isBlank()) return
        draht.rufen("EinzelrufSagen", wert(rufId), wert(text.trim()))
    }

    /** Der Ruhe-Schalter des Platzes — die Leitstelle kommt immer durch. */
    fun einzelrufZulassen(zulassen: Boolean) = draht.rufen("EinzelrufZulassenSetzen", wert(zulassen))

    /**
     * Eine Äußerung an eine Bot-Besatzung beginnen — die rastende Taste der Leiste.
     *
     * Am anderen Ende hört niemand zu: Der Server schneidet mit und schickt den
     * erkannten Satz als `EinzelrufErkannt` zurück.
     *
     * @return false, wenn das Mikrofon nicht zu bekommen war.
     */
    fun einzelrufSprechenStarten(): Boolean {
        if (amEinzelrufSprechen) return true
        if (_stand.value.sendet) return false

        val los = mikrofon.starten { paket -> draht.rufen("EinzelrufAudio", wert(paket)) }
        if (!los) {
            _stand.update { it.copy(funkhinweis = "Das Mikrofon war nicht zu bekommen.") }
            return false
        }
        amEinzelrufSprechen = true
        draht.rufen("EinzelrufSprechenStarten")
        _stand.update { it.copy(einzelrufHoert = true) }
        return true
    }

    fun einzelrufSprechenBeenden(rufId: String) {
        if (!amEinzelrufSprechen) return
        amEinzelrufSprechen = false
        mikrofon.stoppen()
        _stand.update { it.copy(einzelrufHoert = false) }

        bereich.launch {
            val antwort = runCatching { draht.frage("EinzelrufSprechenBeenden", wert(rufId)) }.getOrNull()
            if ((antwort as? JsonPrimitive)?.content == "true") {
                _stand.update { it.copy(einzelrufErkennung = true) }
            }
        }
    }

    /**
     * Das Mikrofon folgt dem Gespräch — vollduplex, also ohne Sprechtaste: Es geht
     * mit der Annahme an und mit dem Auflegen aus.
     *
     * Mit einer Bot-Besatzung bleibt es zu: Am anderen Ende hört niemand zu; dort
     * öffnet die rastende Taste der Leiste das Mikrofon für eine Äußerung.
     */
    private fun einzelrufNachziehen() {
        val s = _stand.value
        val ruf = s.eigenerEinzelruf

        if (ruf != null && einzelrufMikroFuer != ruf.id) {
            einzelrufMikroFuer = ruf.id
            if (ruf.mitBot) return

            // Wer gerade funkt, hört damit auf — ein Mikrofon, ein Weg.
            if (s.sendet) sprechenBeenden()
            val los = mikrofon.starten { paket -> draht.rufen("EinzelrufAudio", wert(paket)) }
            if (!los) _stand.update { it.copy(funkhinweis = "Das Mikrofon war nicht zu bekommen.") }
            return
        }

        if (ruf == null && einzelrufMikroFuer != null) {
            einzelrufAufraeumen()
        }
    }

    /** Aufgelegt, beendet oder abgerissen — Mikrofon zu, Hörer leer. */
    private fun einzelrufAufraeumen() {
        if (einzelrufMikroFuer == null && !amEinzelrufSprechen) return
        einzelrufMikroFuer = null
        amEinzelrufSprechen = false
        if (!_stand.value.sendet) mikrofon.stoppen()
        // Was noch im Puffer liegt, gehört zu einem Gespräch, das es nicht mehr gibt.
        einzelrufLautsprecher.schliessen()
        _stand.update { it.copy(einzelrufHoert = false) }
    }

    // ------------------------------------------------------------ Tonregler

    /**
     * Lauter, leiser, still — der Tonregler im Kopf des Begleiters.
     *
     * Der Funkregler trägt wie im Web auch den Einzelruf; „stumm" nimmt alles weg
     * außer dem Melder. Gespeichert wird am Gerät.
     */
    fun tonSetzen(funk: Float? = null, melder: Float? = null, stumm: Boolean? = null) {
        _stand.update {
            it.copy(
                tonFunk = funk?.coerceIn(0f, 1f) ?: it.tonFunk,
                tonMelder = melder?.coerceIn(0f, 1f) ?: it.tonMelder,
                stumm = stumm ?: it.stumm,
            )
        }
        val s = _stand.value
        tonablage.edit()
            .putFloat("funk", s.tonFunk)
            .putFloat("melder", s.tonMelder)
            .putBoolean("stumm", s.stumm)
            .apply()
        pegelAnwenden()
    }

    /** Ein neuer Stand trägt die Toneinstellungen weiter — sie gehören dem Gerät, nicht der Kopplung. */
    private fun mitTon(neu: Begleiterstand): Begleiterstand = neu.copy(
        tonFunk = tonablage.getFloat("funk", 1f),
        tonMelder = tonablage.getFloat("melder", 0.9f),
        stumm = tonablage.getBoolean("stumm", false),
    )

    private fun pegelAnwenden() {
        val s = _stand.value
        val funk = if (s.stumm) 0f else s.tonFunk
        lautsprecher.lautstaerke(funk)
        einzelrufLautsprecher.lautstaerke(funk)
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
    /** Was der Begleiter zeigt — die Wahl vom Rechner (siehe `Begleitergeraete`). */
    val zeigtMelder: Boolean = true,
    val zeigtFunkgeraet: Boolean = true,
    val zeigtFunkchat: Boolean = true,
    /**
     * Ob dieses Handy der Funkplatz ist — dann klingelt hier der Einzelruf. Vorgabe
     * ja: Der Dialog am Rechner setzt den Haken beim Erzeugen des QR-Codes selbst.
     */
    val istFunkplatz: Boolean = true,
    /** Ob gerade eine Äußerung an eine Bot-Besatzung aufgenommen wird. */
    val einzelrufHoert: Boolean = false,
    /** Ob der Server die letzte Äußerung gerade in Text übersetzt. */
    val einzelrufErkennung: Boolean = false,
    /** Der Funkregler, 0–1 — Stimmen und Einzelruf. */
    val tonFunk: Float = 1f,
    /** Der Melderregler, 0–1. */
    val tonMelder: Float = 0.9f,
    /** Alles außer dem Melder stumm. */
    val stumm: Boolean = false,
) {
    /** Ob an **diesem** Gerät der Einzelruf stattfindet — hier klingelt es, hier ist das Mikrofon. */
    val einzelrufHier: Boolean get() = istFunkplatz

    val einzelrufe: List<Einzelruf> get() = raum?.einzelrufe.orEmpty()

    /** Klingelt es bei mir? Bei einem Leitstellenruf klingelt es an jedem Leitstellenplatz. */
    val eingehenderEinzelruf: Einzelruf?
        get() = einzelrufe.firstOrNull {
            it.klingelt && (it.zielPlayerId == spielerId || (it.zielPlayerId == null && istLeitstelle))
        }

    /** Mein eigener Rufversuch, solange die Gegenstelle noch nicht abgenommen hat. */
    val ausgehenderEinzelruf: Einzelruf?
        get() = einzelrufe.firstOrNull { it.klingelt && it.vonPlayerId == spielerId }

    /** Das laufende Gespräch, an dem dieser Platz beteiligt ist. */
    val laufenderEinzelruf: Einzelruf?
        get() = einzelrufe.firstOrNull {
            it.laeuft && (it.vonPlayerId == spielerId || it.angenommenVonPlayerId == spielerId)
        }

    /** Das laufende Gespräch — nur an dem Gerät, an dem der Einzelruf stattfindet. */
    val eigenerEinzelruf: Einzelruf? get() = if (einzelrufHier) laufenderEinzelruf else null

    /** Der Ruhe-Schalter des Platzes, wie ihn der Server führt. */
    val einzelrufZulassen: Boolean get() = spieler?.einzelrufZulassen ?: true

    /** Der Melderregler als Lautstärke des Tongebers, 0–100. */
    val melderpegel: Int get() = (tonMelder * 100).toInt().coerceIn(0, 100)

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
 * <b>Geprüft wird nur die Form</b> — dieselbe wie in `BegleiterScanView.vue`:
 * mindestens sechzehn Hexzeichen, groß oder klein, hinter `/funk/`. Ob der
 * Token gilt, weiß allein der Server; hier geht es darum, einen versehentlich
 * gescannten fremden QR-Code (WLAN, Paketaufkleber) nicht als Kopplungsversuch
 * an den Hub zu schicken. Bis hierher verlangte die App genau 64 Zeichen in
 * Kleinschrift — ein von Hand gekürzter oder groß getippter Link, den das Web
 * annahm, scheiterte am Handy.
 */
fun tokenAus(eingabe: String): String? {
    val roh = eingabe.trim()
    if (roh.isEmpty()) return null

    FUNKPFAD.find(roh)?.let { return it.groupValues[1] }

    // Ein nackter Token, ohne Adresse davor.
    return roh.takeIf { NACKTER_TOKEN.matches(it) }
}

private val FUNKPFAD = Regex("/funk/([0-9a-fA-F]{16,})")
private val NACKTER_TOKEN = Regex("[0-9a-fA-F]{16,}")

/**
 * Den Begleiter über eine Adresse öffnen — `…/mobile/funk/<token>` oder den Token
 * allein.
 *
 * Das ist die Stelle, an der ein Deep Link ankommt (siehe den Weg `funk/{token}`
 * in `ExtrasWege`): Sie nimmt, was das System übergibt, und koppelt — mit
 * denselben Prüfungen wie der Scanner.
 */
fun Begleiter.linkOeffnen(adresse: String) = koppeln(adresse)

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

/**
 * Die Griffe des Begleiters, die über Funk, Melder und Kopplung hinausgehen —
 * Einzelruf und Tonregler —, gebündelt für die Ansicht.
 */
fun Begleiter.griffe(): de.pagerspass.pagerspass.ansichten.BegleiterGriffe =
    de.pagerspass.pagerspass.ansichten.BegleiterGriffe(
        einzelrufStarten = { einzelrufStarten(it) },
        einzelrufAnnehmen = { einzelrufAnnehmen(it) },
        einzelrufAbweisen = { einzelrufAbweisen(it) },
        einzelrufBeenden = { einzelrufBeenden(it) },
        einzelrufSagen = { ruf, text -> einzelrufSagen(ruf, text) },
        einzelrufSprechenStarten = { einzelrufSprechenStarten() },
        einzelrufSprechenBeenden = { einzelrufSprechenBeenden(it) },
        einzelrufZulassen = { einzelrufZulassen(it) },
        tonSetzen = { funk, melder, stumm -> tonSetzen(funk, melder, stumm) },
    )
