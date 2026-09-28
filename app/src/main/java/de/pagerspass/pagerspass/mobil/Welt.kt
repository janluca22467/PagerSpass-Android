package de.pagerspass.pagerspass.mobil

import android.app.Application
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Netzfehler
import de.pagerspass.pagerspass.netz.WeltAuswahl
import de.pagerspass.pagerspass.netz.WeltBereich
import de.pagerspass.pagerspass.netz.WeltChatzeile
import de.pagerspass.pagerspass.netz.WeltEcke
import de.pagerspass.pagerspass.netz.WeltEvent
import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltLage
import de.pagerspass.pagerspass.netz.WeltLaufbahnstufe
import de.pagerspass.pagerspass.netz.WeltPackicon
import de.pagerspass.pagerspass.netz.WeltPoiAenderung
import de.pagerspass.pagerspass.netz.WeltStreifenroute
import de.pagerspass.pagerspass.netz.WeltStreifenstation
import de.pagerspass.pagerspass.netz.WeltZug
import de.pagerspass.pagerspass.netz.Weltbetrieb
import de.pagerspass.pagerspass.netz.Weltgrosslage
import de.pagerspass.pagerspass.netz.Weltleitstelle
import de.pagerspass.pagerspass.netz.Weltpoi
import de.pagerspass.pagerspass.netz.Weltstand
import de.pagerspass.pagerspass.netz.Weltwege
import de.pagerspass.pagerspass.netz.wert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/**
 * Der Zustand der einen Welt — das Gegenstück zu `web/src/stores/welt.ts`.
 *
 * <b>Getrennt von `Runde`, und das mit Absicht.</b> Jene hält eine Runde: Sie fängt an,
 * sie hört auf, sie hat einen Raumcode und eine Live-Verbindung, die den ganzen Zustand
 * schiebt. Die Welt ist keine Runde — sie läuft immer, sie gehört allen, und ihr Zustand
 * steht in der Datenbank.
 *
 * <b>Warum abgefragt und nicht geschoben wird.</b> Der Hub (`/hub/welt`) meldet
 * <em>dass</em> sich etwas geändert hat — eine neue Lage, eine erledigte, die Großlage.
 * Er schiebt keine Fahrzeugpositionen: Bei fünfzig Spielern mit je dreißig Fahrzeugen
 * wären das fünfzehnhundert Nachrichten je Sekunde. Die eigenen Fahrzeuge kommen im
 * Fünf-Sekunden-Takt, und zwischen zwei Takten fährt `Weltfahrt` sie auf derselben
 * Straße weiter.
 *
 * <b>Die Aktionen geben einen Satz zurück oder `null`.</b> Dieselbe Form wie im Web
 * (`Promise<string | null>`): Die Blende zeigt den Satz an ihrem Knopf, statt dass
 * eine rote Zeile über die ganze Karte läuft.
 */
class Welt(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val netz = Netz(ablage)

    /** Die Wege — für die Blenden, die etwas holen, was nicht im Zustand steht. */
    val wege = Weltwege(netz)

    /** Die Fahrtrechnung zwischen zwei Abrufen. */
    val fahrt = Weltfahrt()

    private val draht = Funkverbindung(ablage, "/hub/welt")
    private val mikrofon = Mikrofon()
    private val lautsprecher = Lautsprecher()

    private val _zustand = MutableStateFlow(Weltzustand())
    val zustand: StateFlow<Weltzustand> = _zustand.asStateFlow()

    /** Die Bilder des aktiven Icon-Packs, je Fahrzeugvorlage — leer heißt Standard. */
    private val _packbilder = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val packbilder: StateFlow<Map<String, ImageBitmap>> = _packbilder.asStateFlow()

    private val kennung: String? get() = _zustand.value.kennung

    init {
        draht.beiLage = { lage ->
            _zustand.update { it.copy(hub = lage) }
            // Nach jedem Verbinden — dem ersten und jedem Wiederverbinden — muss sich die
            // Leitung neu anmelden und den Funkkreis neu melden (siehe `WeltHub.Anmelden`).
            if (lage == Funkverbindung.Lage.Verbunden) {
                viewModelScope.launch {
                    runCatching { draht.frage("Anmelden") }
                    funkkreisMelden()
                    betriebLaden()
                }
            }
        }

        // Die Argumentzahl muss zum Server passen — jedes Ereignis trägt genau einen Wert.
        draht.auf("LageNeu") { bald() }
        draht.auf("LageWeg") { bald() }
        draht.auf("LageErledigt") { bald() }
        draht.auf("Grosslage") { viewModelScope.launch { grosslageLaden() } }
        draht.auf("WeltEvents") { viewModelScope.launch { eventsLaden() } }

        draht.auf("Gutschrift") { argumente ->
            val betrag = argumente.firstOrNull()?.let { zahl(it) } ?: return@auf
            _zustand.update { it.copy(gutschrift = Gutschriftblitz(betrag, System.currentTimeMillis())) }
            bald()
        }

        // Der eigene Wochensieg: Das Preisgeld leuchtet wie jede Gutschrift, und der
        // Zähler ist das Zeichen für die Rangliste, sich sofort neu zu holen.
        draht.auf("Wochensieg") { argumente ->
            val preisgeld = argumente.firstOrNull()?.let { zahl(it) } ?: return@auf
            _zustand.update {
                it.copy(
                    gutschrift = Gutschriftblitz(preisgeld, System.currentTimeMillis()),
                    wochensiege = it.wochensiege + 1,
                )
            }
            bald()
        }

        // ----------------------------------------------------------------- Chat

        draht.auf("ChatNachricht") { argumente ->
            val zeile = argumente.firstOrNull()?.let {
                runCatching { Netz.abgabe.decodeFromJsonElement(WeltChatzeile.serializer(), it) }
                    .getOrNull()
            } ?: return@auf
            chatAnhaengen(zeile)
        }

        draht.auf("ChatAbgelehnt") { argumente ->
            val satz = argumente.firstOrNull()?.let { text(it) } ?: "Das ging nicht."
            _zustand.update { it.copy(chatfehler = satz) }
        }

        // Ein Ping wird gemerkt und nicht angezeigt — der Chat blinkt, und die Marke fällt
        // beim Öffnen des Chats.
        draht.auf("ChatPing") { argumente ->
            val von = argumente.getOrNull(0)?.let { text(it) }
            val satz = argumente.getOrNull(1)?.let { text(it) }.orEmpty()
            _zustand.update { it.copy(angepingt = Ping(von, satz, System.currentTimeMillis())) }
        }

        // ----------------------------------------------------------------- Funk

        draht.auf("FunkStart") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val name = argumente.getOrNull(1)?.let { text(it) }
            _zustand.update { it.copy(spricht = Sprecher(wer, name)) }
        }

        draht.auf("FunkEnde") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            _zustand.update { if (it.spricht?.kennung == wer) it.copy(spricht = null) else it }
        }

        // Ein Häppchen Sprache — nur, wenn das Gerät an ist. Die Prüfung steht hier und
        // auf dem Server: Zwischen Ausschalten und Verlassen der Gruppe können noch ein
        // paar Pakete unterwegs sein.
        draht.auf("FunkAudio") { argumente ->
            if (!_zustand.value.funkgeraet) return@auf
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            lautsprecher.abspielen(paket)
        }
    }

    // ------------------------------------------------------------ Kennung

    /**
     * Für welches Konto geladen wird — vom Aufrufer gesetzt, nicht selbst geraten. Ein
     * anderes Konto bekommt einen leeren Zustand, damit es nichts erbt.
     */
    fun kennungSetzen(neu: String?) {
        if (neu == kennung) return
        leeren()
        _zustand.update { it.copy(kennung = neu) }
    }

    // ------------------------------------------------------------ Betreten

    /** Was beim Betreten herauskam — und wohin es damit geht. */
    enum class Betreten { Arbeitsplatz, Gruendung, Fehler }

    /**
     * Holt den Stand — und sagt, ob es eine Leitstelle gibt.
     *
     * Nur 404 heißt „noch keine Leitstelle gebaut" (siehe `WeltEndpunkte`, `/stand`).
     * Alles andere ist ein Fehler, und der bisherige Stand bleibt stehen: Ein Netzfehler
     * ist kein „noch nicht gegründet" — sonst gründete man womöglich ein zweites Mal.
     */
    suspend fun standLaden() {
        val k = kennung ?: return
        try {
            val stand = wege.stand(k)
            _zustand.update { it.copy(stand = stand, standFehler = null, fehler = null, standGeprueft = true) }
        } catch (f: Netzfehler) {
            if (f.stand == 404) {
                _zustand.update { it.copy(stand = null, standFehler = null, standGeprueft = true) }
            } else {
                _zustand.update {
                    it.copy(standFehler = f.message ?: "Der Stand ließ sich nicht laden.", standGeprueft = true)
                }
            }
        } catch (f: Exception) {
            _zustand.update {
                it.copy(standFehler = f.message ?: "Der Stand ließ sich nicht laden.", standGeprueft = true)
            }
        }
    }

    /**
     * Der Arbeitsplatz wird betreten — `betreten` in `WeltView.vue`.
     *
     * <b>Ab dem Stand alles gleichzeitig</b>: Betrieb, Großlage, Events, Punkte,
     * Streifenrouten und Karte brauchen die Antwort keines anderen. Der Takt wartet nur
     * auf den Betrieb.
     */
    suspend fun betreten(): Betreten {
        standLaden()
        val z = _zustand.value
        if (z.standFehler != null && z.stand == null) return Betreten.Fehler
        if (z.stand == null) return Betreten.Gruendung

        val nebenher = viewModelScope.launch {
            listOf(
                async { grosslageLaden() },
                async { eventsLaden() },
                async { poisLaden() },
                async { streifenroutenLaden() },
                async { leitstellenLaden() },
            ).awaitAll()
        }

        betriebLaden()
        taktStarten()

        // Das Icon-Pack ohne Warten: Die Karte zeichnet sofort mit dem Standard und
        // tauscht nach, sobald das Pack da ist.
        viewModelScope.launch { packLaden() }
        nebenher.join()
        return Betreten.Arbeitsplatz
    }

    /** Die Ansicht wird verlassen: Takt, Mikrofon, Lautsprecher und Hub stoppen. */
    fun verlassen() {
        taktBeenden()
        packVergessen()
    }

    // --------------------------------------------------------------- Laden

    /**
     * Die laufende Nummer der Betriebsabrufe. Abgerufen wird von drei Seiten zugleich —
     * im Takt, beim Zurückkehren und nach jeder Aktion. Übernommen wird nur die jüngste
     * Antwort, und nur für die Kennung, für die sie losging.
     */
    @Volatile private var betriebNr = 0

    suspend fun betriebLaden() {
        val k = kennung ?: return
        val nr = ++betriebNr

        // Nur der allererste Abruf ist ein „Laden" mit Anzeige — danach ist jeder Takt eine
        // stille Auffrischung.
        if (_zustand.value.betrieb == null) _zustand.update { it.copy(laedt = true) }

        try {
            val antwort = wege.betrieb(k)
            if (nr != betriebNr || kennung != k) return

            val server = weltzeit(antwort.serverzeit)
            val jetzt = System.currentTimeMillis()
            if (server != null) fahrt.versatzMs = server - jetzt
            fahrt.standSeitMs = jetzt

            _zustand.update {
                it.copy(
                    betrieb = antwort,
                    fehler = null,
                    versatzMs = fahrt.versatzMs,
                    standSeitMs = jetzt,
                )
            }
        } catch (f: Exception) {
            if (nr != betriebNr || kennung != k) return
            _zustand.update { it.copy(fehler = f.message ?: "Der Betrieb ließ sich nicht laden.") }
        } finally {
            if (nr == betriebNr) _zustand.update { it.copy(laedt = false) }
        }
    }

    suspend fun grosslageLaden() {
        val k = kennung ?: return
        val g = runCatching { wege.grosslage(k) }.getOrNull()
        _zustand.update { it.copy(grosslage = g) }
    }

    suspend fun eventsLaden() {
        val k = kennung ?: return
        val e = runCatching { wege.events(k) }.getOrDefault(emptyList())
        _zustand.update { it.copy(events = e) }
    }

    suspend fun poisLaden() {
        val k = kennung ?: return
        val p = runCatching { wege.pois(k) }.getOrDefault(emptyList())
        _zustand.update { it.copy(pois = p) }
    }

    suspend fun streifenroutenLaden() {
        val k = kennung ?: return
        val r = runCatching { wege.streifenrouten(k) }.getOrDefault(emptyList())
        _zustand.update { it.copy(streifenrouten = r) }
    }

    /** Fremde Leitstellen sind Beiwerk: Ohne sie fehlen ein paar Punkte auf der Karte. */
    suspend fun leitstellenLaden() {
        val k = kennung ?: return
        val l = runCatching { wege.karte(k) }.getOrNull() ?: return
        _zustand.update { it.copy(leitstellen = l) }
    }

    /** Die Laufbahn, falls sie noch nicht da ist — ein stiller Fehlschlag. */
    suspend fun laufbahnLaden() {
        val k = kennung ?: return
        if (_zustand.value.laufbahn.isNotEmpty()) return
        val l = runCatching { wege.laufbahn(k) }.getOrDefault(emptyList())
        _zustand.update { it.copy(laufbahn = l) }
    }

    /** Die eigenen Züge — Beiwerk: Ohne sie funktioniert die Handauswahl unverändert. */
    suspend fun zuegeLaden() {
        val k = kennung ?: return
        val z = runCatching { wege.zuege(k) }.getOrDefault(emptyList())
        _zustand.update { it.copy(zuege = z) }
    }

    // ---------------------------------------------------------------- Takt

    private var takt: Job? = null
    private var taktZaehler = 0
    private var sichtbar = true

    /**
     * Der Takt: fünf Sekunden und nicht eine. Die Fahrzeuge bewegt die App selbst; der
     * Takt holt nur, was sie nicht ausrechnen kann — neue Lagen, fremde Kräfte, das
     * eigene Guthaben. Nach einem endgültig gerissenen Hub wird alle 30 s neu verbunden.
     */
    fun taktStarten() {
        taktBeenden()
        takt = viewModelScope.launch {
            while (true) {
                delay(5_000)
                if (!sichtbar) continue
                betriebLaden()
                taktZaehler++
                if (draht.lage == Funkverbindung.Lage.Getrennt && taktZaehler % 6 == 0) hubStarten()

                // Die Großlage und die Events ändern sich selten; einmal je Minute reicht.
                if (taktZaehler % 12 == 0) {
                    grosslageLaden()
                    eventsLaden()
                }
            }
        }
        hubStarten()

        // Der Verlauf einmal beim Betreten — danach kommt jede Zeile über den Hub.
        viewModelScope.launch { chatLaden() }
    }

    fun taktBeenden() {
        takt?.cancel()
        takt = null

        // Wer die Welt verlässt, will keine Antwort mehr, die noch unterwegs ist.
        betriebNr++
        _zustand.update { it.copy(laedt = false) }

        // Erst das Mikrofon loslassen, dann die Verbindung: Andersherum liefe der Aufnehmer
        // weiter und schickte in einen Hub, den es nicht mehr gibt.
        sprechenBeenden()
        lautsprecher.schliessen()
        nachladen?.cancel()
        nachladen = null
        draht.trennen()
    }

    /**
     * Im Hintergrund ruht der Takt — beim Zurückkommen wird sofort geladen, damit die
     * erste sichtbare Sekunde nicht fünf Sekunden alt ist.
     */
    fun sichtbarkeit(an: Boolean) {
        sichtbar = an
        if (an && takt != null) viewModelScope.launch { betriebLaden() }
    }

    /** Entprellt: Drei `LageNeu` im selben Schub sind ein Nachladen, nicht drei. */
    private var nachladen: Job? = null

    private fun bald() {
        if (nachladen?.isActive == true) return
        nachladen = viewModelScope.launch {
            delay(300)
            betriebLaden()
        }
    }

    /** Kein Hub ist kein Beinbruch: Der Takt trägt alles Nötige, mit fünf Sekunden Verzug. */
    private fun hubStarten() {
        if (draht.lage != Funkverbindung.Lage.Getrennt) return
        viewModelScope.launch(Dispatchers.IO) { runCatching { draht.verbinden() } }
    }

    /**
     * Der Server nimmt beim Anmelden niemanden in den Funkkreis — wer hört, muss es nach
     * jedem Verbinden neu sagen.
     */
    private suspend fun funkkreisMelden() {
        if (!_zustand.value.funkgeraet) return
        runCatching { draht.frage("FunkHoeren", wert(true)) }
    }

    // ------------------------------------------------------ Auswahl und Alarm

    /**
     * Zählt jede Anfrage und jedes Zuklappen: Nur die Antwort auf die jüngste Anfrage
     * wird übernommen — eine ältere (andere Lage, inzwischen zugeklappt) wird verworfen.
     */
    @Volatile private var auswahlZaehler = 0

    /**
     * Holt die Fahrzeugauswahl einer Lage — die Voraussetzung des Alarms und nicht nur
     * eine Liste: Der Server gibt dabei ein kurzlebiges Token aus, das der Alarm
     * zurückverlangt.
     */
    suspend fun auswahlLaden(lageId: String): String? {
        val k = kennung ?: return "Du hast noch keine Leitstelle."
        val nr = ++auswahlZaehler
        _zustand.update { it.copy(auswahlLaedt = true) }

        return try {
            val geholt = wege.auswahl(k, lageId)
            if (nr == auswahlZaehler) {
                _zustand.update { it.copy(auswahl = geholt, auswahlLage = lageId) }
            }
            null
        } catch (f: Exception) {
            if (nr != auswahlZaehler) return null
            _zustand.update { it.copy(auswahl = null, auswahlLage = null) }
            f.message ?: "Die Fahrzeugauswahl ließ sich nicht öffnen."
        } finally {
            if (nr == auswahlZaehler) _zustand.update { it.copy(auswahlLaedt = false) }
        }
    }

    /** Beim Zuklappen: Was nicht offen ist, soll auch kein Token mehr halten. */
    fun auswahlSchliessen() {
        auswahlZaehler++
        _zustand.update { it.copy(auswahl = null, auswahlLage = null, auswahlLaedt = false) }
    }

    /** Klappt die Auswahl nur zu, wenn sie zu dieser Lage gehört. */
    fun auswahlSchliessenFuer(lageId: String) {
        if (_zustand.value.auswahlLage == lageId) auswahlSchliessen()
    }

    /**
     * Das Token für eine Lage — und zwar frisch, wenn keines dazu vorliegt. Alarmiert wird
     * auch vom Fahrzeug aus; dort hat niemand eine Auswahl geöffnet, und sie wird im
     * selben Zug nachgeholt.
     */
    private suspend fun tokenFuer(lageId: String): String? {
        val z = _zustand.value
        if (z.auswahlLage == lageId && z.auswahl != null) return z.auswahl.token
        val k = kennung ?: return null
        return runCatching { wege.auswahl(k, lageId).token }.getOrNull()
    }

    /**
     * Schickt einen Alarm und holt bei einem entwerteten Token die Auswahl einmal nach.
     *
     * Wiederholt wird genau einmal und nur bei `auswahlNeu` — bei einer Drosselung (429)
     * oder einer Spielregel wäre eine Wiederholung eine zweite Ablehnung.
     */
    private suspend fun mitToken(lageId: String, senden: suspend (String) -> Unit): String? {
        val token = tokenFuer(lageId) ?: return "Die Fahrzeugauswahl ließ sich nicht öffnen."

        return try {
            senden(token)
            null
        } catch (f: Exception) {
            if (f !is Netzfehler || !f.auswahlNeu) {
                return f.message ?: "Das Alarmieren ging nicht."
            }

            auswahlSchliessenFuer(lageId)

            val zweites = tokenFuer(lageId) ?: return f.message
            try {
                senden(zweites)
                null
            } catch (nochmal: Exception) {
                nochmal.message ?: "Das Alarmieren ging nicht."
            }
        }
    }

    suspend fun alarmieren(lageId: String, fahrzeugIds: List<String>): String? {
        val k = kennung ?: return "Kein Fahrzeug gewählt."
        if (fahrzeugIds.isEmpty()) return "Kein Fahrzeug gewählt."

        val fehlschlag = mitToken(lageId) { token -> wege.alarmieren(k, lageId, fahrzeugIds, token) }

        // Ein verbrauchtes Token ist verbraucht — die Auswahl gehört danach zugeklappt.
        auswahlSchliessenFuer(lageId)
        betriebLaden()
        return fehlschlag
    }

    /** Alarmiert einen ganzen Zug — geschickt wird die Zug-Id, nicht die Fahrzeugliste. */
    suspend fun zugAlarmieren(lageId: String, zugId: String): String? {
        val k = kennung ?: return "Keine Leitstelle."
        val fehlschlag = mitToken(lageId) { token -> wege.zugAlarmieren(k, lageId, zugId, token) }
        auswahlSchliessenFuer(lageId)
        listOf(
            viewModelScope.async { betriebLaden() },
            viewModelScope.async { zuegeLaden() },
        ).awaitAll()
        return fehlschlag
    }

    /** Die eigenen Fahrzeuge wieder aus einer Lage entlassen — die Rücknahme zum Alarm. */
    suspend fun ausLageEntlassen(lageId: String): String? =
        aktion("Das Entlassen ging nicht.") { wege.lageEinruecken(it, lageId) }

    /** Die Anfahrt eines einzelnen Fahrzeugs abbrechen: Es dreht um und rückt ein. */
    suspend fun anfahrtAbbrechen(fahrzeugId: String): String? =
        aktion("Der Abbruch ging nicht.") { wege.anfahrtAbbrechen(it, fahrzeugId) }

    /** Gibt eine eigene Lage für alle frei — endgültig. */
    suspend fun freigeben(lageId: String): String? =
        aktion("Die Freigabe ging nicht.") { wege.lageFreigeben(it, lageId) }

    /** Schickt Fahrzeuge in den Bereitstellungsraum der Großlage. */
    suspend fun bereitstellen(fahrzeugIds: List<String>): String? {
        val k = kennung ?: return "Kein Großeinsatz angekündigt."
        val g = _zustand.value.grosslage ?: return "Kein Großeinsatz angekündigt."
        if (fahrzeugIds.isEmpty()) return "Kein Fahrzeug gewählt."
        return try {
            wege.bereitstellen(k, g.woche, fahrzeugIds)
            betriebLaden()
            grosslageLaden()
            null
        } catch (f: Exception) {
            f.message ?: "Das Vorschicken ging nicht."
        }
    }

    /** Dasselbe für den Bereitstellungsraum eines Events. */
    suspend fun eventBereitstellen(eventId: String, fahrzeugIds: List<String>): String? {
        val k = kennung ?: return "Keine Leitstelle."
        if (fahrzeugIds.isEmpty()) return "Kein Fahrzeug gewählt."
        return try {
            wege.eventBereitstellen(k, eventId, fahrzeugIds)
            betriebLaden()
            eventsLaden()
            null
        } catch (f: Exception) {
            f.message ?: "Das Vorschicken ging nicht."
        }
    }

    // ---------------------------------------------------------- Eigene Punkte

    /**
     * Setzt einen Punkt. Nach jeder Änderung wird neu geladen statt am Stand
     * herumzurechnen: Der Server deckelt Fläche und Zahl der Punkte.
     */
    suspend fun poiSetzen(
        name: String,
        art: String,
        lat: Double,
        lon: Double,
        flaeche: Double,
        gelaende: List<WeltEcke>? = null,
        gelaendeart: String? = null,
        strecke: Boolean = false,
    ): String? {
        val k = kennung ?: return "Keine Leitstelle."
        return try {
            wege.poiSetzen(k, name, art, lat, lon, flaeche, gelaende, gelaendeart, strecke)
            poisLaden()
            null
        } catch (f: Exception) {
            f.message ?: "Der Punkt ließ sich nicht setzen."
        }
    }

    suspend fun poiAendern(id: String, aenderung: WeltPoiAenderung): String? {
        val k = kennung ?: return "Keine Leitstelle."
        return try {
            wege.poiAendern(k, id, aenderung)
            poisLaden()
            null
        } catch (f: Exception) {
            f.message ?: "Die Änderung ging nicht."
        }
    }

    suspend fun poiEntfernen(id: String): String? {
        val k = kennung ?: return "Keine Leitstelle."
        return try {
            wege.poiEntfernen(k, id)
            poisLaden()
            null
        } catch (f: Exception) {
            f.message ?: "Das Entfernen ging nicht."
        }
    }

    // --------------------------------------------------------- Streifenrouten

    suspend fun streifenrouteSichern(name: String, stationen: List<WeltStreifenstation>): String? {
        val k = kennung ?: return "Keine Leitstelle."
        return try {
            wege.streifenrouteSichern(k, name, stationen)
            streifenroutenLaden()
            null
        } catch (f: Exception) {
            f.message ?: "Die Route ließ sich nicht sichern."
        }
    }

    suspend fun streifenrouteEntfernen(id: String): String? {
        val k = kennung ?: return "Keine Leitstelle."
        return try {
            wege.streifenrouteEntfernen(k, id)
            streifenroutenLaden()
            null
        } catch (f: Exception) {
            f.message ?: "Das Entfernen ging nicht."
        }
    }

    /** Legt eine gespeicherte Route auf ein Fahrzeug — geprüft wird auf dem Server. */
    suspend fun streifenrouteAuflegen(fahrzeugId: String, routeId: String): String? =
        aktion("Die Route ließ sich nicht auflegen.") {
            wege.streifenrouteAuflegen(it, fahrzeugId, routeId)
        }

    // --------------------------------------------------------- Allgemein

    /** Was nach einer Aktion neu geholt wird. */
    enum class Nachladen { Betrieb, Stand, Beides, Nichts }

    /**
     * Der gemeinsame Mantel um eine Aktion der Blenden: ausführen, nachladen, und den
     * Satz des Servers zurückgeben — oder `null`, wenn es geklappt hat.
     */
    suspend fun aktion(
        standard: String,
        nachher: Nachladen = Nachladen.Betrieb,
        tun: suspend (kennung: String) -> Unit,
    ): String? {
        val k = kennung ?: return "Du hast noch keine Leitstelle."
        return try {
            tun(k)
            when (nachher) {
                Nachladen.Betrieb -> betriebLaden()
                Nachladen.Stand -> standLaden()
                Nachladen.Beides -> {
                    standLaden()
                    betriebLaden()
                }
                Nachladen.Nichts -> Unit
            }
            null
        } catch (f: Exception) {
            f.message ?: standard
        }
    }

    /** Eine Abfrage für eine Blende — Ergebnis oder Satz. */
    suspend fun <T> holen(standard: String, tun: suspend (kennung: String) -> T): Result<T> {
        val k = kennung ?: return Result.failure(IllegalStateException("Du hast noch keine Leitstelle."))
        return try {
            Result.success(tun(k))
        } catch (f: Exception) {
            Result.failure(IllegalStateException(f.message ?: standard, f))
        }
    }

    /** Die auf der Karte angeklickte Lage — die Lagenliste markiert sie und rollt hin. */
    fun lageWaehlen(id: String?) = _zustand.update { it.copy(gewaehlteLage = id) }

    /** Welcher Ausrückebereich gerade gezeigt wird — `null` heißt „alle". */
    fun bereichWaehlen(name: String?) = _zustand.update { it.copy(gewaehlterBereich = name) }

    /** Die Gutschrift ist verloschen. */
    fun gutschriftVergessen() = _zustand.update { it.copy(gutschrift = null) }

    // ----------------------------------------------------------------- Chat

    /**
     * Hängt eine eintreffende Nachricht an — mit Dublettenschutz: Der Absender bekommt
     * seine Zeile über die Kontogruppe <em>und</em> als Aufrufer zurück.
     */
    private fun chatAnhaengen(n: WeltChatzeile) {
        _zustand.update { z ->
            if (z.chat.any { it.id == n.id }) return@update z
            val chat = (z.chat + n).takeLast(400)
            val mehr = !n.eigen && n.kanal == "Direkt" && !z.chatOffen
            z.copy(chat = chat, ungelesen = if (mehr) z.ungelesen + 1 else z.ungelesen)
        }
    }

    /** Ob der Schalter schon einmal von Hand oder aus der Vorgabe gesetzt wurde. */
    private var funkgeraetGesetzt = false

    suspend fun chatLaden() {
        val k = kennung ?: return
        val antwort = runCatching { wege.chat(k) }.getOrNull() ?: return
        _zustand.update {
            it.copy(
                chat = antwort.nachrichten,
                ungelesen = antwort.ungelesen,
                chatMaxLaenge = antwort.maxLaenge,
            )
        }

        // Die Vorgabe kommt vom Server und wird nur beim ersten Laden übernommen.
        if (!funkgeraetGesetzt) {
            funkgeraetGesetzt = true
            _zustand.update { it.copy(funkgeraet = antwort.funkgeraetVorgabe) }
            if (draht.lage == Funkverbindung.Lage.Verbunden) funkkreisMelden()
        }
    }

    /** Die Blende ist offen oder zu — beim Öffnen wird die Post abgehakt. */
    suspend fun chatGeoeffnet(offen: Boolean) {
        _zustand.update { it.copy(chatOffen = offen) }
        if (!offen) return

        _zustand.update { it.copy(angepingt = null) }
        val k = kennung ?: return
        if (_zustand.value.ungelesen == 0) return

        _zustand.update { it.copy(ungelesen = 0) }
        runCatching { wege.chatGelesen(k) }
    }

    /**
     * Schickt eine Nachricht über den Hub — ohne auf die eigene Zeile zu warten. Scheitert
     * die Prüfung, kommt `ChatAbgelehnt` und setzt `chatfehler`.
     */
    suspend fun chatSenden(kanal: String, anId: String?, text: String) {
        _zustand.update { it.copy(chatfehler = null) }

        if (draht.lage != Funkverbindung.Lage.Verbunden) {
            _zustand.update { it.copy(chatfehler = "Keine Verbindung zum Kanal.") }
            return
        }

        runCatching {
            draht.frage("ChatSenden", wert(kanal), anId?.let { wert(it) } ?: JsonNull, wert(text))
        }.onFailure {
            _zustand.update { it.copy(chatfehler = "Die Nachricht ging nicht hinaus.") }
        }
    }

    // ----------------------------------------------------------------- Funk

    /**
     * Das Funkgerät ein- oder ausschalten — der Server nimmt einen aus der Gruppe, und
     * der Puffer verstummt.
     */
    fun funkgeraetSetzen(an: Boolean) {
        funkgeraetGesetzt = true
        _zustand.update { it.copy(funkgeraet = an, spricht = if (an) it.spricht else null) }
        if (!an) lautsprecher.schliessen()

        if (draht.lage != Funkverbindung.Lage.Verbunden) return
        viewModelScope.launch { runCatching { draht.frage("FunkHoeren", wert(an)) } }
    }

    /**
     * Die Sprechtaste drücken. <b>Sprechen geht auch mit ausgeschaltetem Gerät</b> — der
     * Schalter heißt „hören".
     *
     * @return false, wenn nicht gesendet wird (keine Verbindung, Mikrofon).
     */
    fun sprechenStarten(): Boolean {
        if (_zustand.value.sendet) return true
        if (draht.lage != Funkverbindung.Lage.Verbunden) return false

        val los = mikrofon.starten { paket ->
            if (draht.lage == Funkverbindung.Lage.Verbunden) draht.rufen("FunkAudio", wert(paket))
        }
        if (!los) return false

        _zustand.update { it.copy(sendet = true) }
        draht.rufen("SprechenStarten")
        return true
    }

    fun sprechenBeenden() {
        if (!_zustand.value.sendet) return
        _zustand.update { it.copy(sendet = false) }
        mikrofon.stoppen()
        if (draht.lage == Funkverbindung.Lage.Verbunden) draht.rufen("SprechenBeenden")
    }

    // -------------------------------------------------------------- Icon-Pack

    /**
     * Die Icons des Packs, das gerade gilt — einmal beim Betreten, danach nur beim
     * Wechseln in den Einstellungen. Ein Fehler ist kein Grund, die Welt anzuhalten: Dann
     * gilt der gezeichnete Riss.
     */
    suspend fun packLaden() {
        val icons = runCatching { wege.iconpackAktiv() }.getOrNull() ?: return
        val server = ablage.server()
        val bilder = withContext(Dispatchers.IO) {
            icons.mapNotNull { icon ->
                bildLaden(server, icon.bild)?.let { icon.vorlageId to it }
            }.toMap()
        }
        _zustand.update {
            it.copy(pack = icons.associateBy { i -> i.vorlageId }, packstand = it.packstand + 1)
        }
        _packbilder.value = bilder
    }

    /** Das Pack gilt der Welt und nicht dem Konto: Wer sie verlässt, sieht wieder Risse. */
    fun packVergessen() {
        if (_zustand.value.pack.isEmpty()) return
        _zustand.update { it.copy(pack = emptyMap(), packstand = it.packstand + 1) }
        _packbilder.value = emptyMap()
    }

    private fun bildLaden(server: String, bild: String): ImageBitmap? = runCatching {
        if (bild.startsWith("data:")) {
            val daten = Base64.decode(bild.substringAfter(","), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(daten, 0, daten.size)?.asImageBitmap()
        } else {
            val adresse = if (bild.startsWith("http")) bild else "$server$bild"
            val verbindung = URL(adresse).openConnection() as HttpURLConnection
            verbindung.connectTimeout = 8_000
            verbindung.readTimeout = 12_000
            try {
                verbindung.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
            } finally {
                verbindung.disconnect()
            }
        }
    }.getOrNull()

    // ---------------------------------------------------------------- Leeren

    /** Alles zurück auf Anfang — damit das nächste Konto nichts erbt. */
    fun leeren() {
        taktBeenden()
        taktZaehler = 0
        funkgeraetGesetzt = false
        auswahlZaehler++
        fahrt.versatzMs = 0
        fahrt.standSeitMs = 0
        _packbilder.value = emptyMap()
        _zustand.value = Weltzustand()
    }

    override fun onCleared() {
        taktBeenden()
        super.onCleared()
    }

    // ---------------------------------------------------------------- Helfer

    private fun text(element: JsonElement): String? =
        if (element is JsonNull) null else runCatching { element.jsonPrimitive.content }.getOrNull()

    private fun zahl(element: JsonElement): Int? =
        runCatching { (element as? JsonPrimitive)?.doubleOrNull?.toInt() }.getOrNull()
}

/** Eine Gutschrift über den Hub — für das Aufleuchten am Kopf. `um` ist die lokale Uhr. */
data class Gutschriftblitz(val betrag: Int, val um: Long)

/** Der letzte Ping — gemerkt, nicht angezeigt. */
data class Ping(val von: String?, val text: String, val um: Long)

/** Wer gerade spricht. */
data class Sprecher(val kennung: String, val name: String?)

/** Was über die Welt bekannt ist. */
data class Weltzustand(
    val kennung: String? = null,
    val stand: Weltstand? = null,
    /** Warum der Stand nicht kam — `null`, wenn er kam oder es schlicht keine Leitstelle gibt. */
    val standFehler: String? = null,
    /** Ob die erste Anfrage nach dem Stand schon beantwortet ist. */
    val standGeprueft: Boolean = false,
    val betrieb: Weltbetrieb? = null,
    val grosslage: Weltgrosslage? = null,
    val laufbahn: List<WeltLaufbahnstufe> = emptyList(),
    val events: List<WeltEvent> = emptyList(),
    val pois: List<Weltpoi> = emptyList(),
    val streifenrouten: List<WeltStreifenroute> = emptyList(),
    val leitstellen: List<Weltleitstelle> = emptyList(),
    val laedt: Boolean = false,
    val fehler: String? = null,
    val gewaehlteLage: String? = null,
    val gewaehlterBereich: String? = null,
    val gutschrift: Gutschriftblitz? = null,
    val wochensiege: Int = 0,
    val versatzMs: Long = 0,
    val standSeitMs: Long = 0,
    val auswahl: WeltAuswahl? = null,
    val auswahlLage: String? = null,
    val auswahlLaedt: Boolean = false,
    val zuege: List<WeltZug> = emptyList(),
    val chat: List<WeltChatzeile> = emptyList(),
    val chatfehler: String? = null,
    val chatMaxLaenge: Int = 280,
    val ungelesen: Int = 0,
    val chatOffen: Boolean = false,
    val angepingt: Ping? = null,
    val funkgeraet: Boolean = false,
    val spricht: Sprecher? = null,
    val sendet: Boolean = false,
    val hub: Funkverbindung.Lage = Funkverbindung.Lage.Getrennt,
    /** Das aktive Icon-Pack, je Fahrzeugvorlage. */
    val pack: Map<String, WeltPackicon> = emptyMap(),
    /** Zählt jeden Packwechsel — die Karte zeichnet danach neu. */
    val packstand: Int = 0,
) {
    val hatLeitstelle: Boolean get() = stand != null

    /** Die Ausrückebereiche — aus dem Betrieb, denn der kommt alle fünf Sekunden. */
    val bereiche: List<WeltBereich> get() = betrieb?.bereiche ?: emptyList()

    /** Ob es überhaupt etwas umzuschalten gibt. */
    val mehrereBereiche: Boolean get() = bereiche.size > 1

    /** Zu welchem Bereich eine Wache gehört — der Name, wie ihn der Server mitschickt. */
    val bereichDerWache: Map<String, String>
        get() = stand?.wachen?.associate { it.id to it.bereich } ?: emptyMap()

    /** Zu welchem Bereich ein Fahrzeug gehört — gemessen an seiner Heimatwache. */
    fun bereichVonFahrzeug(f: WeltFahrzeug): String? = bereichDerWache[f.wacheId]

    fun fahrzeugImBereich(f: WeltFahrzeug): Boolean =
        gewaehlterBereich == null || bereichVonFahrzeug(f) == gewaehlterBereich

    fun lageImBereich(l: WeltLage): Boolean =
        gewaehlterBereich == null || l.bereich == gewaehlterBereich

    /** Nur die Fahrzeuge, die gerade auf der Wache stehen — die Zahl an der Leiste. */
    val freieFahrzeuge: List<WeltFahrzeug>
        get() = betrieb?.fahrzeuge?.filter { it.lage == "Wache" } ?: emptyList()

    /**
     * Ob dieses Fahrzeug gerade alarmiert werden darf — Wache, Rückfahrt, und der
     * Bereitstellungsraum nur auf einen Abschnitt seiner Großlage. Verschlissene und
     * Werkstattfahrten nicht.
     */
    fun istAlarmierbar(f: WeltFahrzeug, lage: WeltLage?): Boolean {
        val woche = grosslage?.woche
        return !f.verschlissen &&
            !f.inWerkstatt &&
            (
                f.lage == "Wache" ||
                    f.lage == "Rueckfahrt" ||
                    (
                        f.lage == "Bereitstellung" &&
                            lage?.ausGrosslage == true &&
                            woche != null &&
                            f.zielGrosslage == woche
                        )
                )
    }

    /**
     * Ob dieses Fahrzeug sich vormerken lässt: Es steht an einer Einsatzstelle oder ist
     * zu einer unterwegs und fährt zur nächsten, sobald seine Arbeit dort getan ist.
     */
    fun istVormerkbar(f: WeltFahrzeug): Boolean =
        (f.lage == "VorOrt" || f.lage == "Anfahrt" || f.lage == "Ausrueckt") &&
            f.lageId != null &&
            f.lehrgang == null &&
            !f.verschlissen &&
            !f.inWerkstatt

    fun alarmierbareFuer(lage: WeltLage?): List<WeltFahrzeug> =
        betrieb?.fahrzeuge?.filter { istAlarmierbar(it, lage) } ?: emptyList()

    /** Die sofort Verfügbaren oben, darunter die, die es tun, sobald sie fertig sind. */
    fun alarmierbareUndVormerkbaren(lage: WeltLage? = null): List<WeltFahrzeug> =
        alarmierbareFuer(lage) + (betrieb?.fahrzeuge?.filter { istVormerkbar(it) } ?: emptyList())

    /** Die Lagen nach Entfernung — was in Reichweite ist, steht oben. */
    val lagenNah: List<WeltLage>
        get() = betrieb?.lagen?.sortedBy { it.entfernungMeter } ?: emptyList()

    /** Dieselbe Liste, auf den gewählten Bereich eingeschränkt. */
    val lagenImBereich: List<WeltLage> get() = lagenNah.filter { lageImBereich(it) }

    /** Die Lagen, an denen nur noch gearbeitet wird — abgerechnet, aber nicht geräumt. */
    val inArbeit: List<WeltLage> get() = betrieb?.lagen?.filter { it.zustand == "Erledigt" } ?: emptyList()

    /** Die Lagen, auf denen noch etwas zu entscheiden ist. */
    val offeneLagen: List<WeltLage> get() = betrieb?.lagen?.filter { it.zustand != "Erledigt" } ?: emptyList()

    /** Wie viele Lagen auf meine Entscheidung warten — die Zahl am Reiter. */
    val meineOffenen: Int get() = offeneLagen.count { it.zustaendig && !it.freigegeben }

    val offene: Int get() = offeneLagen.size

    val guthaben: Int get() = betrieb?.guthaben ?: stand?.guthaben ?: 0
    val stufe: Int get() = betrieb?.stufe ?: stand?.stufe ?: 1
}
