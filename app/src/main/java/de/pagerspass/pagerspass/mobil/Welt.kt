package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Netzfehler
import de.pagerspass.pagerspass.netz.WeltAuswahl
import de.pagerspass.pagerspass.netz.WeltChatzeile
import de.pagerspass.pagerspass.netz.WeltEvent
import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltLage
import de.pagerspass.pagerspass.netz.WeltLaufbahnstufe
import de.pagerspass.pagerspass.netz.WeltPoiAenderung
import de.pagerspass.pagerspass.netz.WeltPoiNeu
import de.pagerspass.pagerspass.netz.WeltPunkt
import de.pagerspass.pagerspass.netz.WeltStreifenroute
import de.pagerspass.pagerspass.netz.WeltStreifenstation
import de.pagerspass.pagerspass.netz.WeltVorlage
import de.pagerspass.pagerspass.netz.WeltZug
import de.pagerspass.pagerspass.netz.Weltbetrieb
import de.pagerspass.pagerspass.netz.Weltgrosslage
import de.pagerspass.pagerspass.netz.Weltleitstelle
import de.pagerspass.pagerspass.netz.Weltpoi
import de.pagerspass.pagerspass.netz.Weltstand
import de.pagerspass.pagerspass.netz.Weltwege
import de.pagerspass.pagerspass.netz.wert
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * PagerSpass - World — das Gegenstück zu `web/src/stores/welt.ts`.
 *
 * <b>Ein eigenes ViewModel neben Sitzung und Runde</b>, und aus demselben Grund
 * wie die Runde: Der Betrieb kommt alle fünf Sekunden, der Hub meldet dazwischen
 * jede neue Lage. Läge das in der Sitzung, zeichnete jeder Takt die Tableiste
 * neu. Und es hängt an der Activity, damit ein Drehen des Geräts weder den Takt
 * noch die Funkverbindung abreißt.
 *
 * <b>Die Welt ist ein Vollbildmodus wie die Runde.</b> `offen` sagt dem Rahmen,
 * dass er die sechs Wege wegnimmt; wer die Welt verlässt, beendet Takt, Hub,
 * Mikrofon und Lautsprecher — in dieser Reihenfolge, damit kein Aufnehmer in
 * eine geschlossene Leitung schickt.
 *
 * <b>Geladen wird nach jeder Handlung neu, nicht nachgerechnet.</b> Der Server
 * deckelt Preise, Plätze und Fähigkeiten; eine Liste, die etwas anderes
 * behauptet als die Datenbank, ist schlimmer als eine, die kurz wartet.
 */
class Welt(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val netz = Netz(ablage)

    /** Die REST-Wege — die Seiten rufen die Nachschlagewege (Kasse, Leihe, …) selbst. */
    val wege = Weltwege(netz)

    private val draht = Funkverbindung(ablage, pfad = "/hub/welt")
    private val mikrofon = Mikrofon()
    private val lautsprecher = Lautsprecher()
    private var funklimit: Job? = null

    private val _stand = MutableStateFlow(Weltzustand())
    val stand: StateFlow<Weltzustand> = _stand.asStateFlow()

    private var takt: Job? = null
    private var nachladen: Job? = null
    private var betriebNr = 0
    private var auswahlNr = 0
    private var funkgeraetGesetzt = false

    /** Ob die Anwendung gerade zu sehen ist — im Hintergrund ruht der Takt. */
    private var sichtbar = true

    val kennung: String? get() = _stand.value.kennung

    init {
        draht.beiLage = { lage ->
            _stand.update { it.copy(verbunden = lage == Funkverbindung.Lage.Verbunden) }
            // Nach jedem Verbinden — auch dem automatischen Wiederverbinden —
            // neu anmelden: Der Server nimmt niemanden von selbst in den
            // Funkkreis, und die Ereignisse der Zwischenzeit fehlen.
            if (lage == Funkverbindung.Lage.Verbunden) {
                viewModelScope.launch {
                    runCatching { draht.frage("Anmelden") }
                    if (_stand.value.funkgeraet) draht.rufen("FunkHoeren", wert(true))
                    bald()
                }
            }
        }

        // Die Argumentzahl muss zum Server passen (Hubs/WeltHub.cs) — hier liest
        // jeder Empfänger nur, was er braucht; der Rest wird ohnehin neu geholt.
        draht.auf("LageNeu") { bald() }
        draht.auf("LageWeg") { bald() }
        draht.auf("LageErledigt") { bald() }
        draht.auf("Grosslage") { viewModelScope.launch { grosslageLaden() } }
        draht.auf("WeltEvents") { viewModelScope.launch { eventsLaden() } }
        draht.auf("Gutschrift") { argumente ->
            val betrag = argumente.firstOrNull()?.let { runCatching { it.jsonPrimitive.longOrNull }.getOrNull() } ?: 0L
            _stand.update { it.copy(gutschrift = Gutschriftzeichen(betrag, System.currentTimeMillis())) }
            bald()
        }
        draht.auf("Wochensieg") { argumente ->
            val betrag = argumente.firstOrNull()?.let { runCatching { it.jsonPrimitive.longOrNull }.getOrNull() } ?: 0L
            _stand.update {
                it.copy(
                    gutschrift = Gutschriftzeichen(betrag, System.currentTimeMillis()),
                    wochensiege = it.wochensiege + 1,
                )
            }
            bald()
        }

        draht.auf("ChatNachricht") { argumente ->
            val zeile = argumente.firstOrNull()?.let { entziffern<WeltChatzeile>(it) } ?: return@auf
            chatAnhaengen(zeile)
        }
        draht.auf("ChatAbgelehnt") { argumente ->
            val satz = argumente.firstOrNull()?.let { text(it) } ?: "Das ging nicht."
            _stand.update { it.copy(chatfehler = satz) }
        }
        // Ein Ping gilt einem Menschen und nicht dem Kanal: gemerkt, nicht
        // angezeigt — der Chat-Eintrag unter „Mehr“ trägt die Marke.
        draht.auf("ChatPing") { argumente ->
            val von = argumente.getOrNull(0)?.let { text(it) }
            val satz = argumente.getOrNull(1)?.let { text(it) }.orEmpty()
            _stand.update { it.copy(angepingt = Anpingen(von, satz)) }
        }

        draht.auf("FunkStart") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val name = argumente.getOrNull(1)?.let { text(it) }
            _stand.update { it.copy(spricht = Sprecher(wer, name)) }
        }
        draht.auf("FunkEnde") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) }
            _stand.update { if (it.spricht?.kennung == wer) it.copy(spricht = null) else it }
        }
        // Ein Häppchen Sprache — nur, wenn das Gerät an ist. Die Prüfung steht
        // auch auf dem Server; doppelt, weil zwischen Ausschalten und Verlassen
        // der Gruppe noch Pakete unterwegs sein können.
        draht.auf("FunkAudio") { argumente ->
            if (!_stand.value.funkgeraet) return@auf
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            lautsprecher.abspielen(paket)
        }
    }

    // ================================================================= Zugang

    /**
     * Die Welt betreten.
     *
     * Holt zuerst den Stand: 404 heißt „noch keine Leitstelle“ und führt zur
     * Gründung; jeder andere Fehler bleibt ein Fehler (ein Netzfehler ist kein
     * „noch nicht gegründet“).
     */
    fun oeffnen(kennung: String) {
        if (_stand.value.kennung == kennung) return
        _stand.value = Weltzustand(offen = true, kennung = kennung)
        viewModelScope.launch { betreten() }
    }

    /**
     * Den Vollbildmodus einschalten — noch ohne zu laden. Ob geladen wird,
     * entscheidet der Rahmen: ohne Premium oder mit offener Einweisung nicht.
     */
    fun anzeigen() {
        if (!_stand.value.offen) _stand.update { it.copy(offen = true) }
    }

    /** Die Welt verlassen — erst Mikrofon, dann Lautsprecher, dann die Leitung. */
    fun schliessen() {
        taktBeenden()
        _stand.value = Weltzustand()
        funkgeraetGesetzt = false
    }

    /** Die Anwendung ist zu sehen oder nicht — der Takt ruht im Hintergrund. */
    fun sichtbarkeit(an: Boolean) {
        sichtbar = an
        if (an && _stand.value.betriebLaeuft) viewModelScope.launch { betriebLaden() }
    }

    fun erneutBetreten() {
        _stand.update { it.copy(standFehler = null, standFehlerStand = 0) }
        viewModelScope.launch { betreten() }
    }

    private suspend fun betreten() {
        val k = kennung ?: return
        _stand.update { it.copy(laedtStand = true) }
        // Der Katalog und die fremden Leitstellen laufen nebenher: Die Gründung
        // zeigt die grauen Punkte, der Arbeitsplatz braucht die Fähigkeiten.
        viewModelScope.launch { katalogLaden() }
        viewModelScope.launch { leitstellenLaden() }

        val ergebnis = runCatching { wege.stand(k) }
        val fehler = ergebnis.exceptionOrNull()
        when {
            ergebnis.isSuccess -> {
                _stand.update {
                    it.copy(stand = ergebnis.getOrNull(), standGeprueft = true, laedtStand = false, standFehler = null)
                }
                betriebStarten()
            }
            fehler is Netzfehler && fehler.stand == 404 -> _stand.update {
                it.copy(stand = null, standGeprueft = true, laedtStand = false, standFehler = null)
            }
            else -> _stand.update {
                it.copy(
                    laedtStand = false,
                    standFehler = fehler?.message ?: "Der Stand ließ sich nicht laden.",
                    standFehlerStand = (fehler as? Netzfehler)?.stand ?: 0,
                )
            }
        }
    }

    /** Die eine Leitstelle gründen — danach geht es direkt an den Arbeitsplatz. */
    suspend fun gruenden(name: String, lat: Double, lon: Double): String? = imModell {
        val k = kennung ?: return@imModell "Kein Konto."
        runCatching { wege.gruenden(k, name, lat, lon) }.exceptionOrNull()?.let {
            return@imModell it.message ?: "Die Gründung ging nicht."
        }
        betreten()
        null
    }

    private suspend fun katalogLaden() {
        if (_stand.value.vorlagen.isNotEmpty()) return
        runCatching { wege.katalog() }.getOrNull()?.let { katalog ->
            _stand.update { it.copy(vorlagen = katalog.fahrzeuge.associateBy { v -> v.id }) }
        }
    }

    private suspend fun leitstellenLaden() {
        val k = kennung ?: return
        runCatching { wege.karte(k) }.getOrNull()?.let { liste ->
            _stand.update { it.copy(leitstellen = liste) }
        }
    }

    // ================================================================ Betrieb

    private fun betriebStarten() {
        taktBeenden()
        _stand.update { it.copy(betriebLaeuft = true) }
        viewModelScope.launch { grosslageLaden() }
        viewModelScope.launch { eventsLaden() }
        viewModelScope.launch { poisLaden() }
        viewModelScope.launch { streifenroutenLaden() }
        viewModelScope.launch { zuegeLaden() }
        viewModelScope.launch { chatLaden() }

        takt = viewModelScope.launch {
            betriebLaden()
            var zaehler = 0
            // Fünf Sekunden und nicht eine: Die Fahrzeuge bewegt die Karte
            // selbst; der Takt holt nur, was sich nicht ausrechnen lässt.
            while (true) {
                delay(5_000)
                if (!sichtbar) continue
                betriebLaden()
                zaehler++
                // Großlage und Events alle Minute — sie ändern sich selten, und
                // der Hub meldet ihre Wechsel ohnehin.
                if (zaehler % 12 == 0) {
                    launch { grosslageLaden() }
                    launch { eventsLaden() }
                }
            }
        }
        viewModelScope.launch { runCatching { draht.verbinden() } }
    }

    private fun taktBeenden() {
        takt?.cancel()
        takt = null
        nachladen?.cancel()
        nachladen = null
        betriebNr++
        sprechenAbbrechen()
        lautsprecher.schliessen()
        draht.trennen()
    }

    /**
     * Den Betrieb holen — nur die jüngste Antwort zählt.
     *
     * Abgerufen wird von drei Seiten zugleich (Takt, Hub, nach jeder Handlung);
     * die Antworten kommen nicht in der Reihenfolge zurück, in der sie losgingen.
     */
    suspend fun betriebLaden() {
        val k = kennung ?: return
        val nr = ++betriebNr
        runCatching { wege.betrieb(k) }
            .onSuccess { b ->
                if (nr != betriebNr || kennung != k) return
                val server = b.serverzeit.takeIf { it.isNotBlank() }?.let { Weltfahrt.zeit(it) }
                val jetzt = System.currentTimeMillis()
                _stand.update {
                    it.copy(
                        betrieb = b,
                        versatzMs = if (server != null && server > 0) server - jetzt else it.versatzMs,
                        standSeitMs = jetzt,
                        fehler = null,
                    )
                }
            }
            .onFailure { e ->
                if (nr != betriebNr || kennung != k) return
                _stand.update { it.copy(fehler = e.message ?: "Der Betrieb ließ sich nicht laden.") }
            }
    }

    /** Entprellt: drei `LageNeu` im selben Schub sind ein Nachladen, nicht drei. */
    private fun bald() {
        if (nachladen?.isActive == true) return
        nachladen = viewModelScope.launch {
            delay(300)
            betriebLaden()
        }
    }

    suspend fun standLaden() {
        val k = kennung ?: return
        runCatching { wege.stand(k) }.getOrNull()?.let { s -> _stand.update { it.copy(stand = s) } }
    }

    /** Nach einer Handlung, die den Besitz ändert: Stand und Betrieb neu. */
    suspend fun allesLaden() {
        standLaden()
        betriebLaden()
    }

    suspend fun grosslageLaden() {
        val k = kennung ?: return
        val g = runCatching { wege.grosslage(k) }.getOrNull()
        _stand.update { it.copy(grosslage = g) }
    }

    suspend fun eventsLaden() {
        val k = kennung ?: return
        val e = runCatching { wege.events(k) }.getOrDefault(emptyList())
        _stand.update { it.copy(events = e) }
    }

    suspend fun poisLaden() {
        val k = kennung ?: return
        val p = runCatching { wege.pois(k) }.getOrDefault(emptyList())
        _stand.update { it.copy(pois = p) }
    }

    suspend fun streifenroutenLaden() {
        val k = kennung ?: return
        val r = runCatching { wege.streifenrouten(k) }.getOrDefault(emptyList())
        _stand.update { it.copy(streifenrouten = r) }
    }

    suspend fun zuegeLaden() {
        val k = kennung ?: return
        val z = runCatching { wege.zuege(k) }.getOrDefault(emptyList())
        _stand.update { it.copy(zuege = z) }
    }

    suspend fun laufbahnLaden() {
        val k = kennung ?: return
        if (_stand.value.laufbahn.isNotEmpty()) return
        val l = runCatching { wege.laufbahn(k) }.getOrDefault(emptyList())
        _stand.update { it.copy(laufbahn = l) }
    }

    fun bereichWaehlen(name: String?) = _stand.update { it.copy(gewaehlterBereich = name) }

    fun lageWaehlen(id: String?) = _stand.update { it.copy(gewaehlteLage = id) }

    fun fehlerWegnehmen() = _stand.update { it.copy(fehler = null) }

    // =========================================================== Alarmierung

    /**
     * Die Fahrzeugauswahl einer Lage holen — samt Handlungs-Token.
     *
     * <b>Genau eine ist offen.</b> Eine Sammlung offener Auswahlen wäre ein
     * Vorrat an Token — genau das, was der Deckel im Server verhindern soll.
     */
    suspend fun auswahlLaden(lageId: String): String? = imModell {
        val k = kennung ?: return@imModell "Du hast noch keine Leitstelle."
        val nr = ++auswahlNr
        _stand.update { it.copy(auswahlLaedt = true) }
        val ergebnis = runCatching { wege.auswahl(k, lageId) }
        if (nr != auswahlNr) return@imModell null
        _stand.update {
            it.copy(
                auswahl = ergebnis.getOrNull(),
                auswahlLage = if (ergebnis.isSuccess) lageId else null,
                auswahlLaedt = false,
            )
        }
        ergebnis.exceptionOrNull()?.let { it.message ?: "Die Fahrzeugauswahl ließ sich nicht öffnen." }
    }

    fun auswahlSchliessen() {
        auswahlNr++
        _stand.update { it.copy(auswahl = null, auswahlLage = null, auswahlLaedt = false) }
    }

    private fun auswahlSchliessenFuer(lageId: String) {
        if (_stand.value.auswahlLage == lageId) auswahlSchliessen()
    }

    /** Das Token für eine Lage — frisch geholt, wenn keines zu ihr offen ist. */
    private suspend fun tokenFuer(lageId: String): String? {
        val s = _stand.value
        if (s.auswahlLage == lageId && s.auswahl != null) return s.auswahl.token
        val k = kennung ?: return null
        return runCatching { wege.auswahl(k, lageId).token }.getOrNull()
    }

    /**
     * Mit Token senden — und bei einem entwerteten (409, `auswahlNeu`) genau
     * einmal nachholen. Das ist der ehrliche Fall: zwei Minuten überlegt oder
     * einen Serverneustart erwischt.
     */
    private suspend fun mitToken(lageId: String, senden: suspend (String) -> Unit): String? {
        val token = tokenFuer(lageId) ?: return "Die Fahrzeugauswahl ließ sich nicht öffnen."
        val erster = runCatching { senden(token) }.exceptionOrNull() ?: return null
        if (erster !is Netzfehler || erster.stand != 409) return erster.message ?: "Das Alarmieren ging nicht."
        auswahlSchliessenFuer(lageId)
        val zweites = tokenFuer(lageId) ?: return erster.message
        return runCatching { senden(zweites) }.exceptionOrNull()?.let { it.message ?: "Das Alarmieren ging nicht." }
    }

    suspend fun alarmieren(lageId: String, fahrzeugIds: List<String>): String? = imModell {
        val k = kennung
        if (k == null || fahrzeugIds.isEmpty()) return@imModell "Kein Fahrzeug gewählt."
        val fehler = mitToken(lageId) { wege.alarmieren(k, lageId, fahrzeugIds, it) }
        // Ein verbrauchtes Token ist verbraucht — gleichgültig, ob es geklappt hat.
        auswahlSchliessenFuer(lageId)
        betriebLaden()
        fehler
    }

    suspend fun zugAlarmieren(lageId: String, zugId: String): String? = imModell {
        val k = kennung ?: return@imModell "Keine Leitstelle."
        val fehler = mitToken(lageId) { wege.zugAlarmieren(k, lageId, zugId, it) }
        auswahlSchliessenFuer(lageId)
        betriebLaden()
        zuegeLaden()
        fehler
    }

    suspend fun ausLageEntlassen(lageId: String): String? =
        handlung("Das Entlassen ging nicht.") { wege.einruecken(it, lageId); betriebLaden() }

    suspend fun anfahrtAbbrechen(fahrzeugId: String): String? =
        handlung("Der Abbruch ging nicht.") { wege.anfahrtAbbrechen(it, fahrzeugId); betriebLaden() }

    suspend fun freigeben(lageId: String): String? =
        handlung("Die Freigabe ging nicht.") { wege.freigeben(it, lageId); betriebLaden() }

    suspend fun bereitstellen(fahrzeugIds: List<String>): String? {
        val g = _stand.value.grosslage ?: return "Kein Großeinsatz angekündigt."
        if (fahrzeugIds.isEmpty()) return "Kein Fahrzeug gewählt."
        return handlung("Das Vorschicken ging nicht.") {
            wege.bereitstellen(it, g.woche, fahrzeugIds)
            betriebLaden()
            grosslageLaden()
        }
    }

    suspend fun eventBereitstellen(eventId: String, fahrzeugIds: List<String>): String? {
        if (fahrzeugIds.isEmpty()) return "Kein Fahrzeug gewählt."
        return handlung("Das Vorschicken ging nicht.") {
            wege.eventBereitstellen(it, eventId, fahrzeugIds)
            betriebLaden()
            eventsLaden()
        }
    }

    // ======================================================== Eigene Punkte

    suspend fun poiSetzen(
        name: String,
        art: String,
        lat: Double,
        lon: Double,
        flaeche: Double,
        gelaende: List<WeltPunkt>?,
        gelaendeart: String?,
        strecke: Boolean,
    ): String? = handlung("Der Punkt ließ sich nicht setzen.") {
        wege.poiSetzen(it, WeltPoiNeu(name, art, lat, lon, flaeche, gelaende, gelaendeart, strecke))
        poisLaden()
    }

    suspend fun poiAendern(id: String, aenderung: WeltPoiAenderung): String? =
        handlung("Die Änderung ging nicht.") { wege.poiAendern(it, id, aenderung); poisLaden() }

    suspend fun poiEntfernen(id: String): String? =
        handlung("Das Entfernen ging nicht.") { wege.poiEntfernen(it, id); poisLaden() }

    // ============================================================ Streife

    suspend fun streifenrouteSichern(name: String, stationen: List<WeltStreifenstation>): String? =
        handlung("Die Route ließ sich nicht sichern.") {
            wege.streifenrouteSichern(it, name, stationen)
            streifenroutenLaden()
        }

    suspend fun streifenrouteEntfernen(id: String): String? =
        handlung("Das Entfernen ging nicht.") { wege.streifenrouteEntfernen(it, id); streifenroutenLaden() }

    suspend fun streifenrouteAuflegen(fahrzeugId: String, routeId: String): String? =
        handlung("Die Route ließ sich nicht auflegen.") {
            wege.streifenrouteAuflegen(it, fahrzeugId, routeId)
            betriebLaden()
        }

    /**
     * Eine Handlung gegen den Server: `null` bei Erfolg, sonst der Satz des
     * Servers. Sie läuft im Bereich des Modells — wer die Seite schließt,
     * während sie unterwegs ist, bricht sie nicht mittendrin ab.
     */
    suspend fun handlung(ersatz: String, tun: suspend (String) -> Unit): String? = imModell {
        val k = kennung ?: return@imModell "Keine Leitstelle."
        runCatching { tun(k) }.exceptionOrNull()?.let { it.message ?: ersatz }
    }

    private suspend fun <T> imModell(block: suspend () -> T): T = viewModelScope.async { block() }.await()

    // ================================================================= Chat

    private fun chatAnhaengen(n: WeltChatzeile) {
        _stand.update { alt ->
            if (alt.chat.any { it.id == n.id }) return@update alt
            val neu = (alt.chat + n).takeLast(400)
            // Fremde Direktpost, während der Chat zu ist: die Marke zählt mit.
            val mehr = if (!n.eigen && n.kanal == "Direkt" && !alt.chatOffen) 1 else 0
            alt.copy(chat = neu, ungelesen = alt.ungelesen + mehr)
        }
    }

    suspend fun chatLaden() {
        val k = kennung ?: return
        val antwort = runCatching { wege.chat(k) }.getOrNull() ?: return
        _stand.update { it.copy(chat = antwort.nachrichten, ungelesen = antwort.ungelesen, chatMax = antwort.maxLaenge) }
        // Die Vorgabe des Servers gilt nur beim ersten Laden: Wer das Gerät
        // eingeschaltet hat, soll es nach einem Nachladen nicht wieder aus haben.
        if (!funkgeraetGesetzt) {
            funkgeraetGesetzt = true
            _stand.update { it.copy(funkgeraet = antwort.funkgeraetVorgabe) }
            if (antwort.funkgeraetVorgabe && draht.lage == Funkverbindung.Lage.Verbunden) {
                draht.rufen("FunkHoeren", wert(true))
            }
        }
    }

    /** Der Chat ist offen oder zu — beim Öffnen wird die Post abgehakt. */
    fun chatGeoeffnet(offen: Boolean) {
        _stand.update { it.copy(chatOffen = offen, angepingt = if (offen) null else it.angepingt) }
        if (!offen) return
        val k = kennung ?: return
        if (_stand.value.ungelesen == 0) return
        _stand.update { it.copy(ungelesen = 0) }
        viewModelScope.launch { runCatching { wege.chatGelesen(k) } }
    }

    /**
     * Eine Nachricht über den Hub. Ohne Warten auf die eigene Zeile: Sie kommt
     * denselben Weg wie jede fremde, und eine Ablehnung kommt als
     * `ChatAbgelehnt`.
     */
    fun chatSenden(anId: String?, text: String): Boolean {
        _stand.update { it.copy(chatfehler = null) }
        if (draht.lage != Funkverbindung.Lage.Verbunden) {
            _stand.update { it.copy(chatfehler = "Keine Verbindung zum Kanal.") }
            return false
        }
        draht.rufen(
            "ChatSenden",
            wert(if (anId != null) "Direkt" else "Allgemein"),
            anId?.let { wert(it) } ?: JsonNull,
            wert(text),
        )
        return true
    }

    // ================================================================= Funk

    /**
     * Das Funkgerät ein- oder ausschalten. Zwei Dinge auf einmal: Der Server
     * nimmt einen aus der Gruppe, und der Lautsprecher verstummt sofort.
     */
    fun funkgeraetSetzen(an: Boolean) {
        funkgeraetGesetzt = true
        _stand.update { it.copy(funkgeraet = an, spricht = if (an) it.spricht else null) }
        if (!an) lautsprecher.schliessen()
        if (draht.lage == Funkverbindung.Lage.Verbunden) draht.rufen("FunkHoeren", wert(an))
    }

    /**
     * Die Sprechtaste drücken. Sprechen geht auch mit ausgeschaltetem Gerät —
     * der Schalter heißt „hören“ (siehe `WeltHub`).
     */
    fun sprechenStarten(): Boolean {
        if (_stand.value.sendet) return true
        if (draht.lage != Funkverbindung.Lage.Verbunden) return false
        val los = mikrofon.starten { paket -> draht.rufen("FunkAudio", wert(paket)) }
        if (!los) {
            _stand.update { it.copy(chatfehler = "Das Mikrofon ließ sich nicht öffnen.") }
            return false
        }
        _stand.update { it.copy(sendet = true) }
        draht.rufen("SprechenStarten")
        funklimit?.cancel()
        funklimit = viewModelScope.launch {
            delay(Sprechfunk.MAX_SENDEDAUER_MS)
            sprechenBeenden()
        }
        return true
    }

    fun sprechenBeenden() {
        funklimit?.cancel()
        funklimit = null
        if (!_stand.value.sendet) return
        mikrofon.stoppen()
        _stand.update { it.copy(sendet = false) }
        if (draht.lage == Funkverbindung.Lage.Verbunden) draht.rufen("SprechenBeenden")
    }

    private fun sprechenAbbrechen() {
        funklimit?.cancel()
        funklimit = null
        mikrofon.stoppen()
        _stand.update { it.copy(sendet = false) }
    }

    override fun onCleared() {
        taktBeenden()
        super.onCleared()
    }

    private fun text(element: JsonElement): String? =
        if (element is JsonNull) null else runCatching { element.jsonPrimitive.content }.getOrNull()

    private inline fun <reified T> entziffern(element: JsonElement): T? =
        runCatching { Netz.abgabe.decodeFromJsonElement(kotlinx.serialization.serializer<T>(), element) }.getOrNull()
}

/** Eine Gutschrift, die gerade am Kopf aufleuchten soll. */
data class Gutschriftzeichen(val betrag: Long, val um: Long)

data class Anpingen(val von: String?, val text: String)

data class Sprecher(val kennung: String, val name: String?)

/**
 * Alles, was der Arbeitsplatz der Welt zeigt.
 *
 * <b>Ein Zustand, nicht zwanzig.</b> Die abgeleiteten Listen (offene Lagen,
 * freie Fahrzeuge, …) stehen als Rechnungen daran und nicht als eigene Felder —
 * zwei Fassungen derselben Liste laufen auseinander.
 */
data class Weltzustand(
    val offen: Boolean = false,
    val kennung: String? = null,
    val laedtStand: Boolean = true,
    val standGeprueft: Boolean = false,
    val stand: Weltstand? = null,
    val standFehler: String? = null,
    val standFehlerStand: Int = 0,
    val betriebLaeuft: Boolean = false,
    val betrieb: Weltbetrieb? = null,
    val fehler: String? = null,
    val versatzMs: Long = 0,
    val standSeitMs: Long = 0,
    val grosslage: Weltgrosslage? = null,
    val events: List<WeltEvent> = emptyList(),
    val pois: List<Weltpoi> = emptyList(),
    val streifenrouten: List<WeltStreifenroute> = emptyList(),
    val laufbahn: List<WeltLaufbahnstufe> = emptyList(),
    val leitstellen: List<Weltleitstelle> = emptyList(),
    val vorlagen: Map<String, WeltVorlage> = emptyMap(),
    val auswahl: WeltAuswahl? = null,
    val auswahlLage: String? = null,
    val auswahlLaedt: Boolean = false,
    val zuege: List<WeltZug> = emptyList(),
    val gewaehlterBereich: String? = null,
    val gewaehlteLage: String? = null,
    val gutschrift: Gutschriftzeichen? = null,
    val wochensiege: Int = 0,
    val chat: List<WeltChatzeile> = emptyList(),
    val chatfehler: String? = null,
    val chatMax: Int = 300,
    val ungelesen: Int = 0,
    val chatOffen: Boolean = false,
    val angepingt: Anpingen? = null,
    val funkgeraet: Boolean = false,
    val spricht: Sprecher? = null,
    val sendet: Boolean = false,
    val verbunden: Boolean = false,
) {
    val hatLeitstelle: Boolean get() = stand != null

    val fahrt: Weltfahrt get() = Weltfahrt(versatzMs, standSeitMs)

    val fahrzeuge: List<WeltFahrzeug> get() = betrieb?.fahrzeuge.orEmpty()

    val guthaben: Long get() = betrieb?.guthaben ?: stand?.guthaben ?: 0

    val stufe: Int get() = betrieb?.stufe ?: stand?.stufe ?: 1

    val bereiche get() = betrieb?.bereiche.orEmpty()

    val mehrereBereiche: Boolean get() = bereiche.size > 1

    /** Nur die Fahrzeuge auf der Wache — die Zahl an der Leiste. */
    val freieFahrzeuge: List<WeltFahrzeug> get() = fahrzeuge.filter { it.lage == "Wache" }

    val offeneLagen: List<WeltLage> get() = betrieb?.lagen.orEmpty().filter { it.zustand != "Erledigt" }

    /** Wie viele Lagen auf meine Entscheidung warten. */
    val meineOffenen: Int get() = offeneLagen.count { it.zustaendig && !it.freigegeben }

    /** Die Lagen nach Entfernung, im gewählten Bereich. */
    val lagenImBereich: List<WeltLage>
        get() = betrieb?.lagen.orEmpty().sortedBy { it.entfernungMeter }
            .filter { gewaehlterBereich == null || it.bereich == gewaehlterBereich }

    fun bereichVonFahrzeug(f: WeltFahrzeug): String? = stand?.wachen?.firstOrNull { it.id == f.wacheId }?.bereich

    fun fahrzeugImBereich(f: WeltFahrzeug): Boolean =
        gewaehlterBereich == null || bereichVonFahrzeug(f) == gewaehlterBereich

    /** Dieselbe Regel wie im Web und auf dem Server — siehe `istAlarmierbar`. */
    fun istAlarmierbar(f: WeltFahrzeug, lage: WeltLage? = null): Boolean {
        val woche = grosslage?.woche
        return !f.verschlissen && !f.inWerkstatt && (
            f.lage == "Wache" || f.lage == "Rueckfahrt" ||
                (f.lage == "Bereitstellung" && lage?.ausGrosslage == true && woche != null && f.zielGrosslage == woche)
            )
    }

    /** Ob sich das Fahrzeug für den Anschlusseinsatz vormerken lässt. */
    fun istVormerkbar(f: WeltFahrzeug): Boolean =
        (f.lage == "VorOrt" || f.lage == "Anfahrt" || f.lage == "Ausrueckt") &&
            f.lageId != null && f.lehrgang == null && !f.verschlissen && !f.inWerkstatt

    fun vorlage(id: String): WeltVorlage? = vorlagen[id]

    val grossAktiv: Boolean get() = grosslage?.zustand == "Laeuft"
}
