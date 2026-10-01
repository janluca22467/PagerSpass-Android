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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.withTimeoutOrNull
import de.pagerspass.pagerspass.netz.Sprechkanal
import android.util.Base64
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull

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

    /**
     * Der eigene Kanal für die Stimme (`api/sprechkanal.ts`) — gebunden an den
     * Hub, nach jedem Verbinden neu. Steht er nicht, geht die Stimme wie bisher
     * über `FunkAudio` am Hub.
     */
    private val sprechkanal = Sprechkanal(ablage, "Welt").apply {
        empfang = { weg, _, _, pcm ->
            // Dieselbe Prüfung wie bei „FunkAudio“: Ein ausgeschaltetes Gerät spricht nicht.
            if (weg == Sprechkanal.Weg.WELT && _stand.value.funkgeraet) lautsprecher.abspielen(pcm)
        }
    }

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
                    sprechkanalBinden()
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
            val betrag = argumente.firstOrNull()?.let { runCatching { it.jsonPrimitive.doubleOrNull?.toLong() }.getOrNull() } ?: 0L
            _stand.update { it.copy(gutschrift = Gutschriftzeichen(betrag, System.currentTimeMillis())) }
            bald()
        }
        draht.auf("Wochensieg") { argumente ->
            val betrag = argumente.firstOrNull()?.let { runCatching { it.jsonPrimitive.doubleOrNull?.toLong() }.getOrNull() } ?: 0L
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
        // Ein Ping gilt einem Menschen und nicht dem Kanal: Er bleibt als Marke
        // am Chat stehen und kommt als Benachrichtigung oben an (`ChatHinweis.vue`).
        draht.auf("ChatPing") { argumente ->
            val von = argumente.getOrNull(0)?.let { text(it) }
            val satz = argumente.getOrNull(1)?.let { text(it) }.orEmpty()
            val vonId = argumente.getOrNull(2)?.let { text(it) }
            _stand.update {
                // Wer stummgeschaltet ist, soll auch über den Umweg des Pings nicht blinken.
                if (it.chatOffen || (vonId != null && vonId in it.stumm)) return@update it
                val jetzt = System.currentTimeMillis()
                it.copy(angepingt = Anpingen(von, satz), chatHinweis = Chathinweis("ping", von, satz, jetzt))
            }
        }
        // Eine Zeile wurde zurückgenommen — vom Absender, in seiner Frist. War
        // sie ungelesene Post, zählt die Marke zurück; steht sie gerade als
        // Benachrichtigung da, geht sie auch dort.
        draht.auf("ChatEntfernt") { argumente ->
            val id = argumente.firstOrNull()?.let { runCatching { it.jsonPrimitive.content.toLong() }.getOrNull() }
                ?: return@auf
            _stand.update { alt ->
                val weg = alt.chat.firstOrNull { it.id == id } ?: return@update alt
                val zaehlt = !weg.eigen && !alt.chatOffen && weg.kanal == "Direkt" && !weg.gelesen
                alt.copy(
                    chat = alt.chat - weg,
                    ungelesen = if (zaehlt) (alt.ungelesen - 1).coerceAtLeast(0) else alt.ungelesen,
                    chatHinweis = alt.chatHinweis?.takeIf { it.text != weg.text },
                    angepingt = alt.angepingt?.takeIf { it.text != weg.text },
                )
            }
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
        _stand.value = Weltzustand(offen = true, kennung = kennung, stumm = stummLesen())
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
        _stand.value = Weltzustand(stumm = stummLesen())
        funkgeraetGesetzt = false
    }

    /** Die Anwendung ist zu sehen oder nicht — der Takt ruht im Hintergrund. */
    fun sichtbarkeit(an: Boolean) {
        sichtbar = an
        if (an && _stand.value.betriebLaeuft) viewModelScope.launch { betriebLaden() }
    }

    /** Noch einmal von vorn — nach einem Fehler oder nach dem Zurücksetzen der Welt. */
    fun erneutBetreten() {
        taktBeenden()
        funkgeraetGesetzt = false
        _stand.update { Weltzustand(offen = true, kennung = it.kennung, vorlagen = it.vorlagen, stumm = it.stumm) }
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
                // Nach einem endgültig gerissenen Hub alle halbe Minute neu
                // verbinden — der Takt trägt bis dahin alles Nötige.
                if (zaehler % 6 == 0 && draht.lage == Funkverbindung.Lage.Getrennt) {
                    launch { runCatching { draht.verbinden() } }
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
        sprechkanal.loesen()
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

    /**
     * Eine Zeile über den Hub — mit Dublettenschutz, denn der Absender bekommt
     * seine eigene über die Kontogruppe <em>und</em> als Aufrufer.
     *
     * Fremde Post, während der Chat zu ist, meldet sich oben als
     * Benachrichtigung (`ChatHinweis.vue`): Direktpost zählt die Marke hoch,
     * eine Zeile im offenen Kanal nur den Hinweis. Von Stummgeschalteten zählt
     * und meldet sich nichts.
     */
    private fun chatAnhaengen(n: WeltChatzeile) {
        _stand.update { alt ->
            if (alt.chat.any { it.id == n.id }) return@update alt
            val neu = (alt.chat + n).takeLast(400)
            if (n.eigen || alt.chatOffen || n.vonId in alt.stumm) return@update alt.copy(chat = neu)
            val jetzt = System.currentTimeMillis()
            if (n.kanal == "Direkt") {
                return@update alt.copy(
                    chat = neu,
                    ungelesen = alt.ungelesen + 1,
                    chatHinweis = Chathinweis("direkt", n.von, n.text, jetzt),
                )
            }
            // Ein Ping kommt als eigenes Ereignis und als Zeile — die Zeile soll
            // die Erwähnung nicht zur gewöhnlichen Kanalmeldung herabstufen.
            val h = alt.chatHinweis
            val ebenPing = h?.art == "ping" && h.text == n.text && jetzt - h.um < 3_000
            alt.copy(chat = neu, chatHinweis = if (ebenPing) h else Chathinweis("kanal", n.von, n.text, jetzt))
        }
    }

    suspend fun chatLaden() {
        val k = kennung ?: return
        val antwort = runCatching { wege.chat(k) }.getOrNull() ?: return
        _stand.update {
            it.copy(
                chat = antwort.nachrichten,
                ungelesen = antwort.ungelesen,
                chatMax = antwort.maxLaenge,
                aeltereVorhanden = antwort.nachrichten.isNotEmpty(),
                zuruecknahmeMs = antwort.zuruecknahmeSekunden * 1_000L,
            )
        }
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

    /**
     * Holt die Zeilen vor der ältesten geladenen und setzt sie oben an — mit
     * Dublettenschutz wie beim Anhängen.
     */
    fun chatAeltereLaden() {
        val k = kennung ?: return
        val alt = _stand.value
        if (alt.aeltereLaedt || !alt.aeltereVorhanden) return
        val aelteste = alt.chat.firstOrNull() ?: run {
            _stand.update { it.copy(aeltereVorhanden = false) }
            return
        }
        _stand.update { it.copy(aeltereLaedt = true) }
        viewModelScope.launch {
            runCatching { wege.chat(k, aelteste.id) }
                .onSuccess { antwort ->
                    _stand.update { s ->
                        val schonDa = s.chat.map { it.id }.toSet()
                        val neu = antwort.nachrichten.filter { it.id !in schonDa }
                        s.copy(chat = neu + s.chat, aeltereVorhanden = neu.isNotEmpty(), aeltereLaedt = false)
                    }
                }
                .onFailure {
                    _stand.update { it.copy(chatfehler = "Ältere Nachrichten ließen sich nicht laden.", aeltereLaedt = false) }
                }
        }
    }

    /** Ob diese Zeile noch zurückgenommen werden kann — eigene, in der Frist. */
    fun zuruecknehmbar(z: WeltChatzeile, jetzt: Long): Boolean {
        val s = _stand.value
        if (!z.eigen || s.zuruecknahmeMs <= 0) return false
        val um = Weltfahrt.zeit(z.um).takeIf { it > 0 } ?: return false
        return jetzt + s.versatzMs - um < s.zuruecknahmeMs
    }

    /**
     * Nimmt eine eigene Zeile zurück. Wie beim Senden ohne eigenes Wegnehmen:
     * Die Zeile verschwindet, wenn der Server `ChatEntfernt` schickt — bei
     * allen gleichzeitig und nur, wenn er es auch getan hat.
     */
    fun chatZuruecknehmen(id: Long) {
        _stand.update { it.copy(chatfehler = null) }
        if (draht.lage != Funkverbindung.Lage.Verbunden) {
            _stand.update { it.copy(chatfehler = "Keine Verbindung zum Kanal.") }
            return
        }
        draht.rufen("ChatZuruecknehmen", kotlinx.serialization.json.JsonPrimitive(id))
    }

    /** Jemanden stummschalten oder wieder hören — nur auf diesem Gerät. */
    fun stummSetzen(vonId: String, an: Boolean) {
        val ohne = _stand.value.stumm.filter { it != vonId }
        val neu = if (an) ohne + vonId else ohne
        _stand.update { it.copy(stumm = neu) }
        runCatching {
            getApplication<Application>().getSharedPreferences(STUMM_DATEI, android.content.Context.MODE_PRIVATE)
                .edit().putString(STUMM_SCHLUESSEL, neu.joinToString(",")).apply()
        }
    }

    private fun stummLesen(): List<String> = runCatching {
        getApplication<Application>().getSharedPreferences(STUMM_DATEI, android.content.Context.MODE_PRIVATE)
            .getString(STUMM_SCHLUESSEL, null)
    }.getOrNull()?.split(",")?.filter { it.isNotBlank() }.orEmpty()

    /** Die Benachrichtigung wegnehmen — das Kreuz oder nach acht Sekunden. */
    fun chatHinweisWeg() {
        _stand.update { it.copy(chatHinweis = null) }
    }

    /** Der Chat ist offen oder zu — beim Öffnen wird die Post abgehakt. */
    fun chatGeoeffnet(offen: Boolean) {
        _stand.update {
            it.copy(
                chatOffen = offen,
                angepingt = if (offen) null else it.angepingt,
                chatHinweis = if (offen) null else it.chatHinweis,
            )
        }
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
        val los = mikrofon.starten { paket ->
            // Erst der Sprechkanal, der Hub nur, wenn er gerade nicht steht —
            // für jedes Paket einzeln, wie im Web.
            val roh = runCatching { Base64.decode(paket, Base64.NO_WRAP) }.getOrNull()
            if (roh == null || !sprechkanal.senden(Sprechkanal.Weg.WELT, roh)) {
                draht.rufen("FunkAudio", wert(paket))
            }
        }
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
        if (draht.lage != Funkverbindung.Lage.Verbunden) return
        // Die letzten Pakete zuerst — sonst verschwindet „spricht“ bei den
        // anderen, bevor der Satz zu Ende gespielt ist.
        viewModelScope.launch {
            sprechkanal.schranke()
            if (draht.lage == Funkverbindung.Lage.Verbunden) draht.rufen("SprechenBeenden")
        }
    }

    /**
     * Einen Schein beim Hub holen und den Sprechkanal damit öffnen. Ein Server
     * ohne Sprechkanal kennt die Methode nicht — dann bleibt es beim Hub.
     */
    private fun sprechkanalBinden() {
        sprechkanal.anbinden {
            if (draht.lage != Funkverbindung.Lage.Verbunden) return@anbinden null
            val antwort = withTimeoutOrNull(5_000) { runCatching { draht.frage("SprechkanalOeffnen") }.getOrNull() }
            (antwort as? JsonPrimitive)?.takeIf { it.isString }?.content
        }
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

/**
 * Eine Benachrichtigung aus dem Weltchat — `chatHinweis` in `stores/welt.ts`.
 * `art` ist `direkt`, `ping` oder `kanal`; eine neue ersetzt die alte.
 */
data class Chathinweis(val art: String, val von: String?, val text: String, val um: Long)

/** Wo die stummgeschalteten Kennungen des Weltchats liegen — nur auf diesem Gerät. */
private const val STUMM_DATEI = "pagerspass_welt"
private const val STUMM_SCHLUESSEL = "chatStumm"

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
    val chatHinweis: Chathinweis? = null,
    /** Wen man im Chat nicht mehr lesen will — Kennungen, nur auf diesem Gerät. */
    val stumm: List<String> = emptyList(),
    /** Wie lange eine eigene Zeile zurückgenommen werden kann; 0 heißt „gar nicht“. */
    val zuruecknahmeMs: Long = 0,
    val aeltereVorhanden: Boolean = false,
    val aeltereLaedt: Boolean = false,
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
