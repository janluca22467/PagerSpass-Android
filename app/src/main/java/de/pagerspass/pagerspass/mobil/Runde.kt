package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.netz.Beitrittsergebnis
import de.pagerspass.pagerspass.netz.Drahtnachricht
import de.pagerspass.pagerspass.netz.Funkzeile
import de.pagerspass.pagerspass.netz.Gutschrift
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Lobbynachricht
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.UEBERTRAGUNG_FASSUNG
import de.pagerspass.pagerspass.netz.UEBERTRAGUNG_MINDESTALTER
import de.pagerspass.pagerspass.netz.Uebertragungsfrage
import de.pagerspass.pagerspass.netz.Uebertragungswege
import de.pagerspass.pagerspass.netz.Uebertragungszusage
import de.pagerspass.pagerspass.netz.wert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Die laufende Runde — das Gegenstück zum Runden-Teil von `stores/spiel.ts`.
 *
 * <b>Sie ist bewusst von der `Sitzung` getrennt.</b> Der Sitzungsstand ändert
 * sich selten (Anmeldung, Konto); der Raumzustand kommt bei jeder Änderung
 * vollständig neu — bei einem Funkspruch, einem Statuswechsel, einer Bewegung
 * eines Fahrzeugs. Lägen beide in einem Objekt, zeichnete jeder Funkspruch die
 * Tableiste neu.
 *
 * <b>Der Server schickt keinen Unterschied, sondern den ganzen Stand.</b> Das
 * ist die einfachste Art, zwei Seiten synchron zu halten — und der Grund, warum
 * hier nichts fortgeschrieben wird: Was ankommt, ersetzt.
 *
 * <b>Die Hub-Befehle stehen zum größten Teil nebenan</b>, in `Rundenbefehle.kt`
 * — als Erweiterungen, die über `senden`/`fragen` gehen. Hier bleibt, was
 * Zustand braucht: Beitritt, Wiedereintritt, Ereignisse, die Sprechtasten.
 */
class Runde(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val draht = Funkverbindung(ablage)
    private val uebertragung = Uebertragungswege(Netz(ablage))
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val toene = Rundentoene(anwendung)

    private val mikrofon = Mikrofon()
    private val lautsprecher = Lautsprecher()
    private var funklimit: Job? = null

    /*
     * Die Nebenleitungen haben je einen eigenen Lautsprecher — wie im Web je
     * einen eigenen Abspielzeiger. Teilten sie sich einen, hingen Draht und
     * Funk hintereinander statt nebeneinander.
     */
    private val drahtLautsprecher = Lautsprecher()
    private val einsatzstellenLautsprecher = Lautsprecher()
    private val einzelrufLautsprecher = Lautsprecher()
    private val telefonLautsprecher = Lautsprecher()
    private val durchsageLautsprecher = Lautsprecher()

    /**
     * Das zweite Mikrofon — für alles, was nicht die Sprechtaste des Kreiskanals
     * ist: Draht, Einsatzstelle, Einzelruf, Notruftelefon, Alarmmeldung. Es
     * läuft nie gleichzeitig mit dem ersten; das verhindert `nebenleitung`.
     */
    private val nebenmikrofon = Mikrofon()
    private var nebenlimit: Job? = null

    private val _stand = MutableStateFlow(Rundenstand())
    val stand: StateFlow<Rundenstand> = _stand.asStateFlow()

    /**
     * Der Haken für die Benachrichtigung.
     *
     * Der Rahmen hängt sich hier ein und zeigt die Systemmeldung mit Ton — das
     * ViewModel selbst fasst kein `NotificationManager` an: Es überlebt die
     * Activity, und eine Benachrichtigung braucht deren Fenster nicht, wohl aber
     * deren Berechtigungslage.
     */
    var beiAlarm: ((Alarmmeldung) -> Unit)? = null

    /**
     * Wo man zuletzt eingetreten ist — für den Wiedereintritt nach einem Abbruch.
     *
     * <b>Erst nach bestätigtem Beitritt gesetzt.</b> Sonst löste ein Wiederaufbau
     * nach einem abgelehnten Beitritt (Raum voll, beendet) einen neuen, ungültigen
     * Versuch auf diesen Raum aus — dieselbe Regel wie in `api/signalr.ts`.
     */
    @Volatile
    private var eintritt: Eintritt? = null

    /** Ob die App gerade im Vordergrund steht — geht nach jedem Wiedereintritt mit. */
    @Volatile
    private var sichtbar: Boolean = true

    init {
        draht.beiLage = { lage -> _stand.update { it.copy(lage = lage) } }
        draht.beiWiederverbunden = { wiedereintreten() }

        // Der ganze Raumzustand, bei jeder Änderung.
        draht.auf("RoomState") { argumente ->
            argumente.firstOrNull()?.let { setzen(it) }
        }

        // <b>Eine Absage ist kein Fehler der Verbindung.</b> „Diesen Raum gibt es
        // nicht (mehr)", „Der Raum ist voll" — der Hub schickt sie als eigenes
        // Ereignis, und sie gehört in die Meldung, nicht ins Protokoll. Nach vier
        // Sekunden räumt sie sich selbst weg, wie im Web.
        draht.auf("Rejected") { argumente ->
            val grund = argumente.firstOrNull()?.let { text(it) } ?: "Der Beitritt wurde abgelehnt."
            _stand.update { it.copy(fehler = grund, laeuft = false) }
            if (_stand.value.sendet && ("Sendepause" in grund || "Funklimit" in grund)) {
                sprechenBeenden()
            }
            bereich.launch {
                delay(4_000)
                _stand.update { if (it.fehler == grund) it.copy(fehler = null) else it }
            }
        }

        // <b>Der Alarm ist ein Ereignis, kein Zustand.</b> Der Raumzustand sagt,
        // dass ein Alarm offen ist; dieses Ereignis sagt, was gerade gekommen
        // ist — und nur das Ereignis darf den Melder auslösen. Wer den Melder am
        // Zustand aufhängt, weckt ihn bei jedem Neuladen wieder.
        draht.auf("Alarm") { argumente ->
            val alarm = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Alarmmeldung.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update { alt ->
                alt.copy(
                    alarm = alarm,
                    // Der Verlauf hält die letzten dreißig Meldungen; ein zweiter
                    // Alarm zum selben Einsatz ersetzt den ersten.
                    melderverlauf = (listOf(alarm) + alt.melderverlauf.filter {
                        it.incidentId != alarm.incidentId
                    }).take(MELDERVERLAUF),
                )
            }
            beiAlarm?.invoke(alarm)
        }

        // Die gesprochene Meldung der Leitstelle zu einem Alarm — nur, solange
        // der Alarm noch im Verlauf steht. Kein Funkverkehr, kein Sprecher.
        draht.auf("AlarmDurchsage") { argumente ->
            val einsatz = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            if (_stand.value.melderverlauf.none { it.incidentId == einsatz }) return@auf
            durchsageLautsprecher.abspielen(paket)
        }

        // Eine Funkzeile zwischen zwei Zustandsständen. Sie wird angehängt und
        // beim nächsten `RoomState` wieder mit dessen Protokoll abgeglichen —
        // die Id verhindert, dass dieselbe Zeile zweimal steht.
        draht.auf("Radio") { argumente ->
            val zeile = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Funkzeile.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update { alt ->
                if (alt.funk.any { it.id == zeile.id }) alt
                else alt.copy(funk = (alt.funk + zeile).takeLast(FUNKZEILEN))
            }
        }

        // Eine neue Zeile im Lobby-Chat — und ein kurzer Ton, wenn sie einen
        // selbst nennt. Welche Ids „erwähnt" heißt, hat der Server entschieden.
        draht.auf("LobbyChat") { argumente ->
            val zeile = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Lobbynachricht.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            val ich = _stand.value.eigeneKennung
            _stand.update { alt ->
                if (alt.lobbyzeilen.any { it.id == zeile.id }) alt
                else alt.copy(lobbyzeilen = (alt.lobbyzeilen + zeile).takeLast(LOBBYZEILEN))
            }
            if (zeile.vonId != ich && ich in zeile.erwaehnte) toene.erwaehnung()
        }

        // Ein Teammitglied hat den Lobby-Chat geleert (`/leeren`). Der leere
        // Verlauf kommt mit dem Raumzustand; hier fallen die örtlichen Zeilen.
        draht.auf("LobbyChatGeleert") {
            _stand.update { it.copy(lobbyzeilen = emptyList()) }
        }

        // Die Bevölkerungswarnung — an alle im Raum, folgenlos fürs Spiel.
        draht.auf("Bevoelkerungswarnung") { argumente ->
            val text = argumente.firstOrNull()?.let { text(it) } ?: return@auf
            _stand.update {
                it.copy(bevoelkerungswarnung = Bevoelkerungswarnung(text, System.currentTimeMillis()))
            }
            toene.warnApp()
        }

        // Nach Dienstende: was die Schicht dem eigenen Konto eingebracht hat.
        draht.auf("Erfahrung") { argumente ->
            val gutschrift = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Gutschrift.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update { it.copy(gutschrift = gutschrift) }
        }

        // Entfernt — von der Leitstelle, wegen einer Sperre oder weil man in der
        // Lobby zu lange weg war. Den Wortlaut liefert der Server; die Seite zeigt
        // ihn mit „Zurück zum Start", statt still auf den Start zu springen.
        draht.auf("KickedFromRoom") { argumente ->
            val grund = argumente.firstOrNull()?.let { text(it) }
                ?: "Du wurdest von der Leitstelle aus dem Raum entfernt."
            eintritt = null
            abraeumen()
            _stand.value = Rundenstand(rausGrund = grund, eigeneKennung = _stand.value.eigeneKennung)
            draht.trennen()
            bereich.launch { ablage.rundeMerken(null) }
        }

        // <b>Positions-Ticks kommen am Vollstand vorbei</b> — mehrmals je
        // Sekunde nur `{vehicleId, lat, lon, …}`. Sie werden in den letzten
        // Raumzustand hineingeflickt; der nächste Vollstand ersetzt ohnehin.
        draht.auf("Positionen") { argumente ->
            val ticks = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(
                            de.pagerspass.pagerspass.netz.FahrzeugPosition.serializer(),
                        ),
                        it,
                    )
                }.getOrNull()
            } ?: return@auf

            _stand.update { alt ->
                val raum = alt.raum ?: return@update alt
                val jeFahrzeug = ticks.associateBy { it.vehicleId }
                alt.copy(
                    raum = raum.copy(
                        vehicles = raum.vehicles.map { f ->
                            jeFahrzeug[f.id]?.let { t ->
                                f.copy(
                                    lat = t.lat,
                                    lon = t.lon,
                                    einsatzstelleErreicht = t.einsatzstelleErreicht,
                                    routeIndex = t.routeIndex,
                                )
                            } ?: f
                        },
                    ),
                )
            }
        }

        // Die Fahrstrecke, sobald der Routingdienst sie hat — bis dahin
        // zeichnet die Karte die Luftlinie.
        draht.auf("Route") { argumente ->
            val strecke = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(
                        de.pagerspass.pagerspass.netz.FahrzeugRoute.serializer(),
                        it,
                    )
                }.getOrNull()
            } ?: return@auf

            _stand.update { alt ->
                val raum = alt.raum ?: return@update alt
                alt.copy(
                    raum = raum.copy(
                        vehicles = raum.vehicles.map { f ->
                            if (f.id == strecke.vehicleId) {
                                f.copy(route = strecke.route, routeIndex = strecke.routeIndex)
                            } else {
                                f
                            }
                        },
                    ),
                )
            }
        }

        // Eine Drahtzeile zwischen zwei Zustandsständen — dieselbe Mechanik
        // wie `Radio`: anhängen, beim nächsten Vollstand abgleichen. Bei fremden
        // Zeilen ein kurzer Ton: Der Draht liegt hinter einem Reiter.
        draht.auf("Draht") { argumente ->
            val zeile = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Drahtnachricht.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update { alt ->
                if (alt.drahtzeilen.any { it.id == zeile.id }) alt
                else alt.copy(drahtzeilen = (alt.drahtzeilen + zeile).takeLast(200))
            }
            if (zeile.vonId != _stand.value.eigeneKennung) toene.erwaehnung()
        }

        // ------------------------------------------------------- Sprechfunk
        //
        // Live-Audio läuft am Raumzustand vorbei: Der Server reicht die
        // 40-ms-Pakete unverändert weiter, und jeder Umweg wäre hörbar.

        // Ein Paket von einem anderen Sprecher — direkt in den Lautsprecher.
        draht.auf("Audio") { argumente ->
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            lautsprecher.abspielen(paket)
        }

        // Jemand hält die Sprechtaste — die eigene zeigt dann „Kanal belegt".
        draht.auf("TransmissionStarted") { argumente ->
            val name = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            val gruppe = argumente.getOrNull(2)?.let { text(it) } ?: ""
            _stand.update { it.copy(sprecher = it.sprecher + (gruppe to name)) }
        }

        draht.auf("TransmissionEnded") { argumente ->
            val gruppe = argumente.getOrNull(1)?.let { text(it) } ?: ""
            _stand.update { it.copy(sprecher = it.sprecher - gruppe) }
        }

        // Der Server weist den Sendeversuch ab — lokalen Zustand zurückrollen,
        // sonst steht „Sendet …" auf der Taste, während die Pakete im Nichts
        // landen. Dieselbe Falle wie im Web.
        draht.auf("ChannelBusy") { argumente ->
            val name = argumente.firstOrNull()?.let { text(it) }
            sprechenAbbrechen()
            toene.belegt()
            _stand.update {
                it.copy(funkhinweis = "Kanal belegt – ${name ?: "jemand"} spricht.")
            }
        }

        // Der Text, den der Server aus der Durchsage verstanden hat. Er kommt
        // nur zum Sprecher zurück — erst `funken` macht daraus den Spruch, den
        // alle lesen.
        draht.auf("FunkErkannt") { argumente ->
            val erkannt = argumente.firstOrNull()?.let { text(it) }
            _stand.update { it.copy(wirdVerstanden = false) }
            if (erkannt.isNullOrBlank()) {
                _stand.update {
                    it.copy(funkhinweis = "Nichts verstanden — der Spruch steht nicht im Protokoll.")
                }
            } else {
                funken(erkannt)
            }
        }

        // ------------------------------------------------ Leitstellendraht
        //
        // Alle vier kommen nur an den Leitstellenplätzen an — der Server
        // verschickt sie gezielt. Keine zweite Rollenprüfung hier.

        draht.auf("DrahtSprecher") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val name = argumente.getOrNull(1)?.let { text(it) } ?: "Unbekannt"
            _stand.update { it.copy(drahtSprecher = Sprecherangabe(wer, name)) }
        }

        draht.auf("DrahtSprecherEnde") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            // Nur, wenn wirklich der laufende Sprecher fertig ist — zwei Disponenten
            // lösen sich schnell ab, und die Ereignisse überholen sich.
            _stand.update { if (it.drahtSprecher?.playerId == wer) it.copy(drahtSprecher = null) else it }
        }

        draht.auf("DrahtBelegt") { argumente ->
            val name = argumente.firstOrNull()?.let { text(it) } ?: "jemand"
            toene.belegt()
            _stand.update { it.copy(funkhinweis = "Draht belegt – $name spricht.") }
            if (_stand.value.nebenleitung == Nebenleitung.Draht) nebenleitungAbbrechen()
        }

        draht.auf("DrahtAudio") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            if (wer != _stand.value.drahtSprecher?.playerId) return@auf
            drahtLautsprecher.abspielen(paket)
        }

        // ------------------------------------------- Funk an der Einsatzstelle
        //
        // Dieselben vier Handgriffe wie beim Draht. Der Sender ist hier der
        // Funkrufname: Vor Ort ruft man ein Fahrzeug, keinen Kontonamen.

        draht.auf("EinsatzstelleSprecher") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val name = argumente.getOrNull(1)?.let { text(it) } ?: "Unbekannt"
            _stand.update { it.copy(einsatzstellenSprecher = Sprecherangabe(wer, name)) }
        }

        draht.auf("EinsatzstelleSprecherEnde") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            _stand.update {
                if (it.einsatzstellenSprecher?.playerId == wer) it.copy(einsatzstellenSprecher = null) else it
            }
        }

        draht.auf("EinsatzstelleBelegt") { argumente ->
            val name = argumente.firstOrNull()?.let { text(it) } ?: "jemand"
            toene.belegt()
            _stand.update { it.copy(funkhinweis = "Einsatzstelle belegt – $name spricht.") }
            if (_stand.value.nebenleitung == Nebenleitung.Einsatzstelle) nebenleitungAbbrechen()
        }

        draht.auf("EinsatzstelleAudio") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            if (wer != _stand.value.einsatzstellenSprecher?.playerId) return@auf
            einsatzstellenLautsprecher.abspielen(paket)
        }

        // --------------------------------------------------------- Einzelruf

        // Vollduplex: nur die Gegenstelle des eigenen laufenden Gesprächs kommt
        // durch — ein spätes Paket eines eben beendeten Rufs soll nicht ins
        // nächste Gespräch hineinsprechen.
        draht.auf("EinzelrufAudio") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            val ruf = _stand.value.eigenerEinzelruf ?: return@auf
            val ich = _stand.value.eigeneKennung
            val gegenstelle = if (ruf.vonPlayerId == ich) ruf.angenommenVonPlayerId else ruf.vonPlayerId
            if (wer != gegenstelle) return@auf
            einzelrufLautsprecher.abspielen(paket)
        }

        // Der erkannte Satz einer Äußerung im Einzelruf mit einer Bot-Besatzung.
        draht.auf("EinzelrufErkannt") { argumente ->
            val ruf = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val erkannt = argumente.getOrNull(1)?.let { text(it) }
            _stand.update { it.copy(einzelrufWirdVerstanden = false) }
            if (erkannt.isNullOrBlank()) {
                _stand.update { it.copy(funkhinweis = "Nichts verstanden — der Satz wurde nicht gesagt.") }
                return@auf
            }
            // Wer inzwischen aufgelegt hat, darf seinen Satz nicht im nächsten
            // Gespräch wiederfinden.
            if (ruf != _stand.value.eigenerEinzelruf?.id) return@auf
            draht.rufen("EinzelrufSagen", wert(ruf), wert(erkannt))
        }

        // ---------------------------------------------------- Notruftelefon

        // Die Stimme des Anrufers — nur an die Leitstelle, eigener Lautsprecher.
        draht.auf("TelefonAudio") { argumente ->
            val anruf = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val paket = argumente.getOrNull(1)?.let { text(it) } ?: return@auf
            if (anruf != _stand.value.aktiverAnruf) return@auf
            telefonLautsprecher.abspielen(paket)
        }

        // Die gesprochene Rückfrage am Notruftelefon, vom Server verstanden.
        draht.auf("NotrufErkannt") { argumente ->
            val anruf = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val erkannt = argumente.getOrNull(1)?.let { text(it) }
            _stand.update { it.copy(notrufWirdVerstanden = false) }
            if (erkannt.isNullOrBlank()) {
                _stand.update { it.copy(funkhinweis = "Nichts verstanden — die Frage wurde nicht gestellt.") }
                return@auf
            }
            if (anruf != _stand.value.aktiverAnruf) return@auf
            draht.rufen("AnrufFrage", wert(anruf), JsonNull, wert(erkannt), JsonNull)
        }

        // Ob am eigenen Platz ein Handy als Funkbegleiter hängt.
        draht.auf("BegleiterStatus") { argumente ->
            val wer = argumente.getOrNull(0)?.let { text(it) } ?: return@auf
            val aktiv = argumente.getOrNull(1)?.let { text(it) } == "true"
            if (wer == _stand.value.eigeneKennung) _stand.update { it.copy(begleiterGekoppelt = aktiv) }
        }
    }

    // ------------------------------------------------------------ Beitritt

    /**
     * Einer Runde beitreten.
     *
     * <b>Der Beitritt muss der Verbindung zügig folgen.</b> Ein frisch
     * eröffneter Raum wird wieder aufgelöst, wenn niemand beitritt — im
     * API-Protokoll steht dann „Leere Lobby ABCDEF nach Prüfung aufgelöst", und
     * die App zeigt „Diesen Raum gibt es nicht (mehr)". Deshalb stehen Verbinden
     * und Beitreten hier in einem Aufruf und nicht in zweien.
     */
    fun beitreten(code: String, name: String) = arbeiten { eintreten(code, name, alsZuschauer = false) }

    /**
     * Als Zuschauer beitreten — kein Platz, keine Rolle, nur die Sicht.
     *
     * <b>Der Zuschauer ist kein Spieler.</b> Er steht in `zuschauer`, nicht in
     * `players`, zählt nicht gegen `maxSpieler` und kann nichts senden — alle
     * Kommandos weisen ihn strukturell ab. Wer die Übung angelegt hat, bekommt
     * hier den Regieplatz.
     */
    fun zuschauen(code: String, name: String) = arbeiten { eintreten(code, name, alsZuschauer = true) }

    /**
     * Der gemeinsame Weg in einen Raum — als Spieler oder als Zuschauer.
     *
     * <b>Ein Beitritt, der allein an der Übertragungseinwilligung scheitert, ist
     * eine offene Frage</b> und kein Fehlschlag: Die Frage wird gemerkt
     * (`einwilligung`), und die Blende darüber stellt sie. Geworfen wird trotzdem
     * — wer beitreten wollte, ist nicht drin.
     */
    private suspend fun eintreten(code: String, name: String, alsZuschauer: Boolean) {
        val sauber = code.trim().uppercase()
        val kennung = ablage.kennung().orEmpty()
        _stand.update { it.copy(code = sauber, rausGrund = null, eigeneKennung = kennung) }

        draht.verbinden()

        val ergebnis = beitrittFragen(sauber, name, alsZuschauer)
            ?: throw IllegalStateException("Die Leitstelle antwortet nicht.")
        val raum = ergebnis.state

        if (!ergebnis.ok || raum == null) {
            val frage = ergebnis.einwilligung
            if (frage != null) {
                _stand.update {
                    it.copy(einwilligung = OffeneEinwilligung(frage, sauber, name, alsZuschauer))
                }
            } else if (_stand.value.raum == null) {
                // Draußen und nichts offen: keine Leitung stehen lassen, die beim
                // nächsten Funkloch einen Raum anruft, in dem man nie war.
                draht.trennen()
            }
            throw Absage(
                ergebnis.fehler ?: if (alsZuschauer) {
                    "Zuschauen ist hier gerade nicht möglich."
                } else {
                    "Diesen Raum gibt es nicht (mehr)."
                },
            )
        }

        eintritt = Eintritt(sauber, name, alsZuschauer)
        abraeumen()
        _stand.update {
            it.copy(
                raum = raum,
                code = sauber,
                fehler = null,
                zuschauer = alsZuschauer,
                einwilligung = null,
                elternbogenAmPlatz = null,
                lobbyzeilen = emptyList(),
                drahtzeilen = emptyList(),
                funk = raum.funkprotokoll,
                alarm = null,
                melderverlauf = emptyList(),
                gutschrift = null,
                rausGrund = null,
            )
        }

        // Für die Rückkehr nach einem Prozesstod — siehe `wiederAufnehmen`. Mit
        // Vorzeichen beim Zuschauer: Die Wiederaufnahme muss wissen, als was sie
        // zurückkommt.
        ablage.rundeMerken(if (alsZuschauer) "Z:$sauber" else sauber)
        nachDemEintritt(alsZuschauer)
    }

    /**
     * Was nach jedem Eintritt dazugehört — auch nach dem Wiedereintritt.
     *
     * <b>Der Server merkt sich beides an der Verbindungskennung</b>, und die ist
     * nach einem Wiederaufbau eine andere. Deshalb bei jedem Eintritt und nicht
     * einmal beim Verbinden.
     */
    private suspend fun nachDemEintritt(alsZuschauer: Boolean) {
        // Die App hat keine eigene Spracherkennung — der Server soll Durchsagen
        // mit Whisper in Text übersetzen (wie Firefox).
        draht.rufen("FunkerkennungWuenschen", wert(true))
        draht.rufen("SetzeSichtbarkeit", wert(sichtbar))
        if (!alsZuschauer) {
            // Ob schon ein Handy am Platz hängt, weiß nur der Server.
            val gekoppelt = runCatching { draht.frage("BegleiterGekoppelt") }.getOrNull()
            _stand.update { it.copy(begleiterGekoppelt = (gekoppelt as? JsonPrimitive)?.content == "true") }
        }
    }

    private suspend fun beitrittFragen(code: String, name: String, alsZuschauer: Boolean): Beitrittsergebnis? {
        val antwort = draht.frage(if (alsZuschauer) "JoinAsSpectator" else "Join", wert(code), wert(name))
        return antwort?.let {
            runCatching { Netz.abgabe.decodeFromJsonElement(Beitrittsergebnis.serializer(), it) }
                .getOrNull()
        }
    }

    /**
     * Nach einem Abbruch den Raum erneut betreten — der Kontext am Server ist neu.
     *
     * `false` heißt: Der Aufruf selbst ist gescheitert — vorübergehend, die
     * Schleife der `Funkverbindung` versucht es weiter. Nur ein `ok = false` ist
     * das Nein des Servers (Runde beendet, Platz weg): Dann wird abgeräumt wie
     * beim Rauswurf, und die Seite sagt, warum. Ohne das stand die alte Lage
     * weiter da, die Leitung hieß „verbunden", und es kam nie wieder etwas.
     */
    private suspend fun wiedereintreten(): Boolean {
        val ziel = eintritt ?: return true

        val ergebnis = try {
            beitrittFragen(ziel.code, ziel.name, ziel.alsZuschauer)
        } catch (e: Exception) {
            return false
        } ?: return false

        val raum = ergebnis.state
        if (!ergebnis.ok || raum == null) {
            eintritt = null
            abraeumen()
            _stand.value = Rundenstand(
                rausGrund = ergebnis.fehler ?: "Die Verbindung zur Runde ist verloren gegangen.",
                eigeneKennung = _stand.value.eigeneKennung,
            )
            draht.trennen()
            ablage.rundeMerken(null)
            return true
        }

        // Der frische Stand gilt, auch wenn seine Nummer kleiner ist: Nach einem
        // Neustart des Servers zählt sie von vorn.
        _stand.update { it.copy(raum = raum, code = raum.code, funk = raum.funkprotokoll) }
        nachStand(raum)

        // Eine während des Abbruchs noch gedrückte Taste: Hält der Server den
        // Kanal nicht mehr für diesen Platz, gehört sie losgelassen.
        val ich = _stand.value.eigeneKennung
        if (_stand.value.sendet && raum.funkkanal.senderPlayerId != ich) sprechenAbbrechen()
        val neben = _stand.value.nebenleitung
        if (neben == Nebenleitung.Draht && raum.drahtkanal.senderPlayerId != ich) nebenleitungAbbrechen()
        if (neben == Nebenleitung.Einsatzstelle && raum.einsatzstellenkanal.senderPlayerId != ich) {
            nebenleitungAbbrechen()
        }

        nachDemEintritt(ziel.alsZuschauer)
        return true
    }

    /**
     * Holt eine aufgegebene Verbindung zurück — beim Zurückkommen in die App oder
     * auf „Neu verbinden" im Verbindungsband.
     */
    fun wiederaufnehmen() = draht.wiederaufnehmen()

    /**
     * Zurück in die Runde, die vor einem Prozesstod lief.
     *
     * <b>Der Server hält den Platz.</b> Die Spieler-Id ist die Kontokennung; ein
     * zweiter `Join` mit demselben Konto setzt einen auf denselben Platz zurück
     * — das Web macht beim Neuladen dasselbe. Gibt es den Raum nicht mehr, wird
     * der gemerkte Code still weggeräumt: „Diesen Raum gibt es nicht (mehr)" ist
     * beim Zurückkommen aus der Hosentasche keine Meldung wert.
     */
    fun wiederAufnehmen(name: String) = viewModelScope.launch {
        if (_stand.value.drin) return@launch
        val gemerkt = ablage.offeneRunde() ?: return@launch
        val alsZuschauer = gemerkt.startsWith("Z:")
        val code = gemerkt.removePrefix("Z:")

        runCatching { eintreten(code, name, alsZuschauer) }
            .onFailure { f ->
                // Kein Netz beim Aufwachen, oder den Raum gibt es nicht mehr — in
                // beiden Fällen ohne Meldung. Nur im zweiten Fall ist der Code weg:
                // Ohne Netz versucht es der nächste Start wieder.
                if (f is Absage && _stand.value.einwilligung == null) ablage.rundeMerken(null)
                _stand.update { it.copy(fehler = null) }
                if (_stand.value.raum == null && _stand.value.einwilligung == null) draht.trennen()
            }
    }

    // ---------------------------------------------- Übertragung einer Schicht

    /**
     * Die Einwilligung erteilen und dort weitermachen, wo es unterbrochen wurde.
     *
     * <b>Die Zusage kann gültig sein oder nicht</b> — bei „unter 18" wartet sie
     * auf den Bogen der Erziehungsberechtigten; dann bleibt die Blende offen und
     * zeigt ihn. Der Aufrufer liest `gilt`. Wirft mit einer lesbaren Meldung.
     */
    suspend fun einwilligungErteilen(volljaehrig: Boolean): Uebertragungszusage {
        val frage = _stand.value.einwilligungsfrage
            ?: throw IllegalStateException("Es ist gerade keine Einwilligung offen.")
        val kennung = ablage.kennung() ?: throw IllegalStateException("Bitte melde dich zuerst an.")

        val zusage = uebertragung.einwilligen(kennung, frage.raumCode, volljaehrig)

        if (!zusage.gilt) {
            // Noch nicht durch: Der Bogen wandert in die offene Frage, damit die
            // Blende ihn zeigen kann.
            _stand.update { alt ->
                val offen = alt.einwilligung
                if (offen != null) {
                    alt.copy(
                        einwilligung = offen.copy(
                            frage = offen.frage.copy(wartetAufEltern = true, elternbogen = zusage.elternbogen),
                        ),
                    )
                } else {
                    alt.copy(elternbogenAmPlatz = zusage.elternbogen)
                }
            }
            return zusage
        }

        _stand.update { it.copy(elternbogenAmPlatz = null) }

        val offen = _stand.value.einwilligung
        if (offen != null) {
            // Der Beitritt, der vorhin abgewiesen wurde — jetzt noch einmal.
            _stand.update { it.copy(laeuft = true) }
            try {
                eintreten(offen.code, offen.name, offen.alsZuschauer)
            } finally {
                _stand.update { it.copy(laeuft = false) }
            }
        } else {
            // Der Fall „saß schon": Der Platz bleibt und bekommt die Erlaubnis
            // nachgetragen. Der Server schlägt selbst nach.
            draht.frage("StreamerfreigabeNachtragen")
        }
        return zusage
    }

    /**
     * Die Frage ablehnen. <b>Ein abgewiesener Beitritt wird nur vergessen</b> —
     * man stand ohnehin draußen. <b>Wer schon saß, verlässt die Runde</b>: Ein
     * „nein" muss folgenlos bleiben dürfen, und folgenlos ist es nur, wenn danach
     * nichts mehr von einem übertragen werden kann.
     */
    fun einwilligungAblehnen() {
        if (_stand.value.einwilligung != null) {
            _stand.update { it.copy(einwilligung = null) }
            if (_stand.value.raum == null) draht.trennen()
            return
        }
        _stand.update { it.copy(elternbogenAmPlatz = null) }
        if (_stand.value.raum != null) verlassen()
    }

    // ----------------------------------------------------------- Die Lobby

    /**
     * Die Rolle wählen.
     *
     * <b>Fahrzeugbesatzung braucht ein Fahrzeug aus der eigenen Garage.</b> Ohne
     * eines steht man ohne Platz da, und der Dienst kann nicht beginnen — der
     * Hub lässt ihn dann gar nicht erst starten. Die Lobby sagt das vorher.
     */
    fun rolleWaehlen(rolle: String, fahrzeug: String? = null) {
        draht.rufen("ChooseRole", wert(rolle), fahrzeug?.let { wert(it) } ?: leer())
    }

    fun bereit(bereit: Boolean) = draht.rufen("SetReady", wert(bereit))

    fun botHinzufuegen(vorlage: String, anzahl: Int = 1) =
        draht.rufen("AddBot", wert(vorlage), wert(anzahl))

    fun botEntfernen(botId: String) = draht.rufen("RemoveBot", wert(botId))

    fun chatSenden(text: String) {
        if (text.isBlank()) return
        draht.rufen("SendLobbyChat", wert(text.trim()))
    }

    fun dienstBeginnen() = draht.rufen("StartRound")

    /**
     * Fürs Dienstende stimmen — oder die eigene Stimme zurückziehen. Bei einer
     * Schwelle von 1 ist es kein Abstimmen, sondern ein Beenden.
     */
    fun dienstBeenden() = draht.rufen("EndRound")

    /**
     * Ob die App gerade im Vordergrund steht.
     *
     * Der Server entscheidet daran, ob er einen Alarm zusätzlich als Mitteilung
     * nachschickt, und über die Abwesenheitsfrist in der Lobby. Er bekommt es bei
     * jedem Wechsel und nach jedem Eintritt — die Verbindungskennung ist nach
     * einem Wiederaufbau eine andere.
     */
    fun sichtbarkeit(sichtbar: Boolean) {
        this.sichtbar = sichtbar
        if (_stand.value.drin) draht.rufen("SetzeSichtbarkeit", wert(sichtbar))
    }

    // -------------------------------------------------------------- Der Dienst
    //
    // Alles Weitere kommt als `RoomState` zurück — deshalb `rufen` statt
    // `frage`: Es gibt keine Antwort, auf die sich das Warten lohnte.

    /** Den Alarm am Melder bestätigen. */
    fun alarmQuittieren() {
        draht.rufen("AcknowledgeAlarm")
        _stand.update { it.copy(alarm = null) }
    }

    /** Den Melder wegtippen, ohne zu bestätigen — der Zustand bleibt offen. */
    fun alarmWegtippen() = _stand.update { it.copy(alarm = null) }

    /**
     * Den FMS-Status setzen.
     *
     * @param grund Nur bei Status 6 (außer Dienst): warum, und wie lange. Der
     *   Server verlangt beides nicht, aber die Leitstelle liest es.
     */
    fun fmsSetzen(status: Int, grund: String? = null, dauerMinuten: Int? = null) =
        draht.rufen(
            "SetFms",
            wert(status),
            grund?.let { wert(it) } ?: JsonNull,
            dauerMinuten?.let { wert(it) } ?: JsonNull,
        )

    fun sondersignal(aus: Boolean) = draht.rufen("SondersignalSetzen", wert(aus))

    fun lagemeldung(text: String) {
        if (text.isBlank()) return
        draht.rufen("SendLagemeldung", wert(text.trim()))
    }

    /**
     * Einen Funkspruch als Text senden.
     *
     * @param an Der Rufname des Ziels — `null` heißt: an die Leitstelle bzw. an
     *   alle, je nachdem wer spricht. Dasselbe Feld wie im Web.
     */
    fun funken(text: String, an: String? = null) {
        if (text.isBlank()) return
        draht.rufen("SendRadio", wert(text.trim()), an?.let { wert(it) } ?: JsonNull)
    }

    /**
     * Eine Zeile auf den Leitstellendraht — Leitstelle zu Leitstelle.
     *
     * Der Server kürzt hart auf 500 Zeichen; die App schneidet vorher, damit
     * niemand tippt, was hinten abfällt.
     */
    fun drahtSenden(text: String) {
        if (text.isBlank()) return
        draht.rufen("DrahtSenden", wert(text.trim().take(500)))
    }

    /**
     * Eine Zeile in den Einsatzstellenfunk — nur mit Status 4 an der Lage.
     *
     * <b>Es gibt kein Push-Ereignis dafür:</b> Neue Zeilen kommen mit dem
     * nächsten Raumzustand. Über 500 Zeichen lehnt der Server ab (anders als
     * beim Draht, der kürzt) — deshalb auch hier der Schnitt vorher.
     */
    fun einsatzstelleSchreiben(text: String) {
        if (text.isBlank()) return
        draht.rufen("EinsatzstelleSchreiben", wert(text.trim().take(500)))
    }

    /** Der offene Lektionsschritt der Ausbildungsschicht — überspringen. */
    fun ausbildungUeberspringen() = draht.rufen("AusbildungUeberspringen")

    // ------------------------------------------------------------ Sprechfunk

    /**
     * Die Sprechtaste ist gedrückt — Durchsage beginnen.
     *
     * Derselbe Dreischritt wie im Web: `StartTransmission`, dann alle 40 ms ein
     * `SendAudio`-Paket, am Ende `EndTransmission`. Die App hat keine eigene
     * Spracherkennung — deshalb wünscht sie beim Beitritt die des Servers
     * (`FunkerkennungWuenschen(true)`), und der erkannte Text kommt als
     * `FunkErkannt` zurück.
     *
     * @return false, wenn nicht gesendet wird (belegt, Sendepause, Mikrofon).
     */
    fun sprechenStarten(): Boolean {
        val jetzt = System.currentTimeMillis()
        val s = _stand.value
        if (s.sendet) return true
        if (s.nebenleitung != null) return false
        // Belegt ist nur der eigene Sendekanal — wer zwei Häuser weiter auf
        // einer anderen Gruppe spricht, sperrt die eigene Taste nicht.
        val belegtVon = s.sprecher[s.raum?.sendegruppe ?: ""]
        if (belegtVon != null) {
            toene.belegt()
            _stand.update { it.copy(funkhinweis = "Kanal belegt – $belegtVon spricht.") }
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

        // Die Uhr des Clients — der Server hat seine eigene und vollstreckt
        // notfalls nachträglich. Zwei Uhren, absichtlich.
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

    /** Die Sprechtaste ist losgelassen — Durchsage beenden. */
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
            // `true` = diese Seite liefert selbst keinen Text; die Antwort sagt,
            // ob noch ein `FunkErkannt` unterwegs ist.
            val antwort = runCatching { draht.frage("EndTransmission", wert(true)) }.getOrNull()
            val erkanntFolgt = (antwort as? JsonPrimitive)?.content == "true"
            if (erkanntFolgt) _stand.update { it.copy(wirdVerstanden = true) }
        }
    }

    /** Rollback nach einer Server-Absage — ohne `EndTransmission`. */
    private fun sprechenAbbrechen() {
        if (!_stand.value.sendet) return
        funklimit?.cancel()
        mikrofon.stoppen()
        _stand.update { it.copy(sendet = false) }
    }

    fun funkhinweisWegnehmen() = _stand.update { it.copy(funkhinweis = null) }

    // -------------------------------------------------------- Nebenleitungen
    //
    // Draht, Einsatzstelle, Einzelruf, Notruftelefon und die gesprochene
    // Alarmmeldung: fünfmal derselbe Dreischritt wie beim Funk — Taste drücken,
    // Häppchen schicken, loslassen —, nur auf anderen Wegen. Es spricht immer
    // höchstens einer davon, und nie zugleich mit der Sprechtaste des Kreises.

    /**
     * Auf dem Leitstellendraht sprechen — nur, wenn er offensteht und frei ist.
     *
     * @return false, wenn nicht gesprochen wird.
     */
    fun drahtSprechenStarten(): Boolean {
        val s = _stand.value
        val raum = s.raum ?: return false
        if (!raum.drahtOffen) return false
        val halter = s.drahtSprecher
        if (halter != null && halter.playerId != s.eigeneKennung) {
            toene.belegt()
            return false
        }
        return nebenleitungStarten(Nebenleitung.Draht, vorher = { draht.rufen("DrahtSprechenStarten") }) {
            paket -> draht.rufen("DrahtAudio", wert(paket))
        }
    }

    /** Der Draht ist losgelassen — `true`: diese Seite liefert keinen Text. */
    fun drahtSprechenBeenden() {
        if (_stand.value.nebenleitung != Nebenleitung.Draht) return
        nebenleitungAbbrechen()
        draht.rufen("DrahtSprechenBeenden", wert(true))
    }

    /** Auf dem Einsatzstellenfunk sprechen — nur mit Status 4 an der Lage. */
    fun einsatzstelleSprechenStarten(): Boolean {
        val s = _stand.value
        val halter = s.einsatzstellenSprecher
        if (halter != null && halter.playerId != s.eigeneKennung) {
            toene.belegt()
            return false
        }
        return nebenleitungStarten(
            Nebenleitung.Einsatzstelle,
            vorher = { draht.rufen("EinsatzstelleSprechenStarten") },
        ) { paket -> draht.rufen("EinsatzstelleAudio", wert(paket)) }
    }

    fun einsatzstelleSprechenBeenden() {
        if (_stand.value.nebenleitung != Nebenleitung.Einsatzstelle) return
        nebenleitungAbbrechen()
        draht.rufen("EinsatzstelleSprechenBeenden", wert(true))
    }

    /**
     * Im Einzelruf sprechen. Die Taste ist keine Kanalsperre, sondern Anfang und
     * Ende <em>einer Äußerung</em> — mit einem Menschen geht die Stimme hinüber,
     * mit einer Bot-Besatzung wird sie am Server erkannt.
     */
    fun einzelrufSprechenStarten(): Boolean =
        nebenleitungStarten(Nebenleitung.Einzelruf, vorher = { draht.rufen("EinzelrufSprechenStarten") }) {
            paket -> draht.rufen("EinzelrufAudio", wert(paket))
        }

    fun einzelrufSprechenBeenden(rufId: String) {
        if (_stand.value.nebenleitung != Nebenleitung.Einzelruf) return
        nebenleitungAbbrechen()
        bereich.launch {
            val antwort = runCatching { draht.frage("EinzelrufSprechenBeenden", wert(rufId)) }.getOrNull()
            if ((antwort as? JsonPrimitive)?.content == "true") {
                _stand.update { it.copy(einzelrufWirdVerstanden = true) }
            }
        }
    }

    /** Eine Rückfrage am Notruftelefon sprechen — der Server erkennt, was gesagt wurde. */
    fun notrufSprechenStarten(): Boolean =
        nebenleitungStarten(Nebenleitung.Notruf, vorher = { draht.rufen("NotrufSprechenStarten") }) {
            paket -> draht.rufen("NotrufAudio", wert(paket))
        }

    fun notrufSprechenBeenden(anrufId: String) {
        if (_stand.value.nebenleitung != Nebenleitung.Notruf) return
        nebenleitungAbbrechen()
        bereich.launch {
            val antwort = runCatching { draht.frage("NotrufSprechenBeenden", wert(anrufId)) }.getOrNull()
            if ((antwort as? JsonPrimitive)?.content == "true") {
                _stand.update { it.copy(notrufWirdVerstanden = true) }
            }
        }
    }

    /**
     * Die Meldung zur Alarmierung sprechen. Gehört wird sie erst beim Alarmieren,
     * und nur von den Alarmierten — dazwischen liegt der Ton am Server und wartet
     * auf seine Kennung (`meldungId`, vom Client vergeben).
     */
    fun alarmMeldungSprechenStarten(meldungId: String): Boolean =
        nebenleitungStarten(
            Nebenleitung.Alarmmeldung,
            vorher = { draht.rufen("AlarmMeldungSprechenStarten", wert(meldungId)) },
        ) { paket -> draht.rufen("AlarmMeldungAudio", wert(meldungId), wert(paket)) }

    /** @return ob der Server eine Aufnahme zu dieser Kennung hat. */
    suspend fun alarmMeldungSprechenBeenden(meldungId: String): Boolean {
        if (_stand.value.nebenleitung == Nebenleitung.Alarmmeldung) nebenleitungAbbrechen()
        val antwort = runCatching { draht.frage("AlarmMeldungSprechenBeenden", wert(meldungId)) }.getOrNull()
        return (antwort as? JsonPrimitive)?.content == "true"
    }

    fun alarmMeldungVerwerfen(meldungId: String) =
        draht.rufen("AlarmMeldungVerwerfen", wert(meldungId))

    /**
     * Welcher Anruf gerade am Ohr liegt — für Telefonstimme und erkannte Fragen.
     * `null` nimmt das Gespräch, das man selbst führt.
     */
    fun telefonAnrufSetzen(anrufId: String?) = _stand.update { it.copy(telefonAnruf = anrufId) }

    private fun nebenleitungStarten(
        leitung: Nebenleitung,
        vorher: () -> Unit,
        beiPaket: (String) -> Unit,
    ): Boolean {
        val s = _stand.value
        if (s.nebenleitung == leitung) return true
        if (s.sendet || s.nebenleitung != null) return false

        val los = nebenmikrofon.starten(beiPaket)
        if (!los) {
            _stand.update { it.copy(funkhinweis = "Das Mikrofon war nicht zu bekommen.") }
            return false
        }
        vorher()
        _stand.update { it.copy(nebenleitung = leitung, funkhinweis = null) }

        // Dieselben zwanzig Sekunden wie am Kreiskanal — dann ist Schluss.
        nebenlimit = bereich.launch {
            delay(Sprechfunk.MAX_SENDEDAUER_MS)
            if (_stand.value.nebenleitung == leitung) {
                when (leitung) {
                    Nebenleitung.Draht -> drahtSprechenBeenden()
                    Nebenleitung.Einsatzstelle -> einsatzstelleSprechenBeenden()
                    else -> nebenleitungAbbrechen()
                }
            }
        }
        return true
    }

    private fun nebenleitungAbbrechen() {
        if (_stand.value.nebenleitung == null) return
        nebenlimit?.cancel()
        nebenmikrofon.stoppen()
        _stand.update { it.copy(nebenleitung = null) }
    }

    // ------------------------------------------------------------ Leitstelle

    fun einsatzAnlegen(
        stichwort: String,
        stichwortText: String,
        meldebild: String,
        adresse: String,
        organisation: String,
        prioritaet: Int,
        meldender: String?,
        empfohleneFahrzeuge: Int?,
        empfohleneFaehigkeiten: List<String>,
        anrufId: String? = null,
        ortsteil: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        ursprungEinsatzId: String? = null,
    ) = draht.rufen(
        "CreateIncident",
        wert(stichwort),
        wert(stichwortText),
        wert(meldebild),
        wert(adresse),
        wert(organisation),
        wert(prioritaet),
        ortsteil?.let { wert(it) } ?: JsonNull,
        meldender?.let { wert(it) } ?: JsonNull,
        empfohleneFahrzeuge?.let { wert(it) } ?: JsonNull,
        liste(empfohleneFaehigkeiten),
        lat?.let { JsonPrimitive(it) } ?: JsonNull,
        lon?.let { JsonPrimitive(it) } ?: JsonNull,
        anrufId?.let { wert(it) } ?: JsonNull,
        ursprungEinsatzId?.let { wert(it) } ?: JsonNull,
    )

    /**
     * Fahrzeuge alarmieren — <b>immer alle fünf Argumente.</b>
     *
     * SignalR bindet nach Stelle; drei statt fünf verwarf der Hub still
     * („Invocation provides 3 argument(s) but target expects 5"). Das Web
     * schickt `{}`, `null`, `null`, wenn nichts dazukommt — hier genauso.
     *
     * @param abrollbehaelter WLF-Id → Abrollbehälter-Vorlage, die er aufnimmt.
     * @param zusatztext Die eigene Meldung der Leitstelle am Melder.
     * @param meldungId Die Kennung einer gesprochenen Meldung (`alarmMeldungSprechenStarten`).
     */
    fun alarmieren(
        incidentId: String,
        fahrzeuge: List<String>,
        abrollbehaelter: Map<String, String> = emptyMap(),
        zusatztext: String? = null,
        meldungId: String? = null,
    ) = draht.rufen(
        "AlarmVehicles",
        wert(incidentId),
        liste(fahrzeuge),
        kotlinx.serialization.json.buildJsonObject {
            abrollbehaelter.forEach { (wlf, ab) -> put(wlf, JsonPrimitive(ab)) }
        },
        zusatztext?.takeIf { it.isNotBlank() }?.let { wert(it.trim()) } ?: JsonNull,
        meldungId?.let { wert(it) } ?: JsonNull,
    )

    /**
     * Der Alarmvorschlag des Servers — welche Fahrzeuge zu dieser Lage passen.
     *
     * <b>Er ist eine Frage, kein Befehl:</b> Die Antwort füllt die Vorauswahl
     * des Alarmdialogs, alarmiert wird erst mit `alarmieren`.
     */
    suspend fun alarmvorschlag(incidentId: String): List<String> {
        val antwort = draht.frage("Alarmvorschlag", wert(incidentId)) ?: return emptyList()
        return runCatching {
            (antwort as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
        }.getOrDefault(emptyList())
    }

    fun einsatzSchliessen(incidentId: String) = draht.rufen("CloseIncident", wert(incidentId))

    fun sprechwunschBeantworten(vehicleId: String) =
        draht.rufen("AnswerSprechwunsch", wert(vehicleId))

    // ------------------------------------------------- Führung und Abschnitte
    //
    // <b>`zweig` wird immer mitgeschickt.</b> SignalR bildet Aufrufe über die
    // Zahl der Stellen ab, nicht über Vorgaben — ein fehlendes Argument
    // verwirft den ganzen Aufruf still. `Gesamt` oder `Rettungsdienst`.

    fun einsatzleitungUebernehmen(incidentId: String, zweig: String = "Gesamt") =
        draht.rufen("EinsatzleitungUebernehmen", wert(incidentId), wert(zweig))

    fun einsatzleitungAbgeben(incidentId: String, zweig: String = "Gesamt") =
        draht.rufen("EinsatzleitungAbgeben", wert(incidentId), wert(zweig))

    fun abschnittBilden(incidentId: String, name: String, zweig: String = "Gesamt") =
        draht.rufen("AbschnittBilden", wert(incidentId), wert(name.take(40)), wert(zweig))

    fun abschnittUmbenennen(incidentId: String, bisher: String, neu: String, zweig: String = "Gesamt") =
        draht.rufen(
            "AbschnittUmbenennen",
            wert(incidentId), wert(bisher), wert(neu.take(40)), wert(zweig),
        )

    /** `abschnitt = null` nimmt die Zuteilung zurück. */
    fun abschnittZuteilen(
        incidentId: String,
        vehicleId: String,
        abschnitt: String?,
        zweig: String = "Gesamt",
    ) = draht.rufen(
        "AbschnittZuteilen",
        wert(incidentId), wert(vehicleId),
        abschnitt?.let { wert(it) } ?: JsonNull,
        wert(zweig),
    )

    fun nachforderungenWeiterreichen(incidentId: String, zweig: String = "Gesamt") =
        draht.rufen("NachforderungenWeiterreichen", wert(incidentId), wert(zweig))

    // ------------------------------------------------------------------- MANV
    //
    // Kategorien vergibt nie der Spieler — die Sichtung würfelt sie, die
    // Freigabe schaltet sie frei. Gesteuert wird über Delegation und Stellen.

    fun manvauftragUebertragen(incidentId: String, auftrag: String, vehicleId: String?) =
        draht.rufen(
            "ManvauftragUebertragen",
            wert(incidentId), wert(auftrag),
            vehicleId?.let { wert(it) } ?: JsonNull,
        )

    fun versorgungsstelleAnordnen(incidentId: String, art: String, groesse: Int = 0) =
        draht.rufen("VersorgungsstelleAnordnen", wert(incidentId), wert(art), wert(groesse))

    fun versorgungsstelleAbbauen(incidentId: String, stelle: String, abbauen: Boolean) =
        draht.rufen("VersorgungsstelleAbbauen", wert(incidentId), wert(stelle), wert(abbauen))

    /** `stelle = null` heißt: zurück ins Gelände. */
    fun patientVerlegen(incidentId: String, patientId: String, stelle: String?) =
        draht.rufen(
            "PatientVerlegen",
            wert(incidentId), wert(patientId),
            stelle?.let { wert(it) } ?: JsonNull,
        )

    fun patientTransportmittel(incidentId: String, patientId: String, vehicleId: String?) =
        draht.rufen(
            "PatientTransportmittelZuweisen",
            wert(incidentId), wert(patientId),
            vehicleId?.let { wert(it) } ?: JsonNull,
        )

    fun patientZielklinik(incidentId: String, patientId: String, klinikId: String?) =
        draht.rufen(
            "PatientZielklinikZuweisen",
            wert(incidentId), wert(patientId),
            klinikId?.let { wert(it) } ?: JsonNull,
        )

    fun transportEinleiten(incidentId: String, patientId: String) =
        draht.rufen("TransportEinleiten", wert(incidentId), wert(patientId))

    fun triageKoordinieren(incidentId: String) =
        draht.rufen("TriageKoordinieren", wert(incidentId))

    fun verstorbeneUebergeben(incidentId: String) =
        draht.rufen("VerstorbeneUebergeben", wert(incidentId))

    // ------------------------------------------------------------------ Regie
    //
    // Der Regieplatz: ein Zuschauer mit einem schmalen Streifen Macht. Er
    // wirft Lagen ein, löst Störungen aus und dreht den Wind — er disponiert
    // nicht. Der Server prüft, wer ihn hat.

    fun regieEintragJetzt() = draht.rufen("RegieEintragJetzt")

    fun regieEintragUeberspringen() = draht.rufen("RegieEintragUeberspringen")

    /** Immer alle acht Argumente — C#-Standardwerte gelten über SignalR nicht. */
    fun regieLage(
        stichwort: String,
        stichwortText: String,
        meldebild: String,
        adresse: String,
        organisation: String,
        prioritaet: Int,
    ) = draht.rufen(
        "RegieLage",
        wert(stichwort), wert(stichwortText), wert(meldebild), wert(adresse),
        wert(organisation), wert(prioritaet), JsonNull, JsonNull,
    )

    fun regieStoerung(art: String) = draht.rufen("RegieStoerung", wert(art))

    fun regieWetter(lage: String) = draht.rufen("RegieWetter", wert(lage), JsonNull)

    fun regieDurchsage(text: String) {
        if (text.isBlank()) return
        draht.rufen("RegieDurchsage", wert(text.take(200).trim()))
    }

    fun regieZeitachse(angehalten: Boolean) =
        draht.rufen("RegieZeitachse", wert(angehalten))

    fun regieUebungBeenden() = draht.rufen("RegieUebungBeenden")

    // ----------------------------------------------------------------- Notruf

    fun anrufAnnehmen(anrufId: String) = draht.rufen("AnrufAnnehmen", wert(anrufId))

    /**
     * Eine Frage an den Anrufer.
     *
     * `frage` ist eine der sechs Faktenarten. Die freie Frage (getippt oder
     * gesprochen) geht über `anrufFrageFrei` in `Rundenbefehle.kt`.
     */
    fun anrufFragen(anrufId: String, frage: String) = draht.rufen(
        "AnrufFrage",
        wert(anrufId),
        wert(frage),
        JsonNull,
        JsonNull,
    )

    fun anrufBeenden(anrufId: String) =
        draht.rufen("AnrufBeenden", wert(anrufId), wert(false))

    fun anrufAbweisen(anrufId: String) = draht.rufen("AnrufAbweisen", wert(anrufId))

    /** Den Vorschlag eines beendeten Anrufs verwerfen — es wird kein Einsatz. */
    fun vorschlagVerwerfen(anrufId: String) =
        draht.rufen("NotrufVorschlagVerwerfen", wert(anrufId))

    /**
     * Einen laufenden Einsatz umstufen.
     *
     * Schon eine geänderte Dringlichkeit zählt — die alarmierten Kräfte sehen
     * die Umstufung auf dem Kanal. Alles außer der Priorität bleibt, wie es ist
     * (`null` heißt: nicht anfassen). Das volle Umstufen steht in
     * `Rundenbefehle.kt` (`einsatzAktualisieren`).
     */
    fun umstufen(incidentId: String, prioritaet: Int) = draht.rufen(
        "UpdateIncident",
        wert(incidentId),
        JsonNull,
        JsonNull,
        JsonNull,
        wert(prioritaet),
        JsonNull,
        JsonNull,
    )

    /** Die Runde verlassen und die Leitung schließen. */
    fun verlassen() = viewModelScope.launch {
        eintritt = null
        sprechenAbbrechen()
        nebenleitungAbbrechen()
        lautsprecherSchliessen()
        runCatching { draht.rufen("Leave") }
        draht.trennen()
        ablage.rundeMerken(null)
        _stand.value = Rundenstand(eigeneKennung = _stand.value.eigeneKennung)
    }

    fun fehlerWegnehmen() = _stand.update { it.copy(fehler = null) }

    /** Die Fehlerseite nach einem Rauswurf ist gelesen — zurück zum Start. */
    fun rausGrundWegnehmen() = _stand.update { it.copy(rausGrund = null) }

    /** Das Warnband ist weggetippt oder abgelaufen. */
    fun bevoelkerungswarnungWegnehmen() = _stand.update { it.copy(bevoelkerungswarnung = null) }

    // ------------------------------------------- Leitstelle: der Tonregler
    //
    // Der Stand liegt am Gerät (`Tonregler.kt`); hier wird er nur auf die
    // Lautsprecher gelegt. Funkregler und „stumm" treffen alle Stimmen, der
    // Melderregler die gesprochene Alarmmeldung, der DMO-Schalter den
    // Einsatzstellenfunk — dieselbe Aufteilung wie `melder.ts` im Web.

    fun tonAnwenden(ton: Tonstand) {
        val funk = ton.funkpegel
        lautsprecher.lautstaerke(funk)
        drahtLautsprecher.lautstaerke(funk)
        einzelrufLautsprecher.lautstaerke(funk)
        telefonLautsprecher.lautstaerke(funk)
        einsatzstellenLautsprecher.lautstaerke(if (ton.dmoStumm) 0f else funk)
        durchsageLautsprecher.lautstaerke(if (ton.durchsage) ton.melder.coerceIn(0f, 1f) else 0f)
    }

    /**
     * Das Mikrofon im Einzelruf zwischen zwei Menschen — vollduplex, ohne Taste:
     * an mit der Annahme, aus mit dem Auflegen (`einzelrufMikroFuer` im Web).
     *
     * <b>Ohne die Zwanzig-Sekunden-Grenze der Nebenleitungen</b> — ein Telefonat ist
     * keine Durchsage. Mit einer Bot-Besatzung läuft es nicht dauerhaft; dort gilt
     * die rastende Taste (`einzelrufSprechenStarten`).
     */
    fun einzelrufMikrofon(an: Boolean) {
        val s = _stand.value
        if (!an) {
            if (s.nebenleitung == Nebenleitung.Einzelruf) {
                nebenmikrofon.stoppen()
                _stand.update { it.copy(nebenleitung = null) }
            }
            // Was noch im Puffer liegt, gehört zu einem Gespräch, das es nicht mehr gibt.
            einzelrufLautsprecher.schliessen()
            return
        }
        if (s.nebenleitung != null) return
        if (s.sendet) sprechenBeenden()
        val los = nebenmikrofon.starten { paket -> draht.rufen("EinzelrufAudio", wert(paket)) }
        if (!los) {
            _stand.update { it.copy(funkhinweis = "Das Mikrofon war nicht zu bekommen.") }
            return
        }
        _stand.update { it.copy(nebenleitung = Nebenleitung.Einzelruf) }
    }

    // ------------------------------------------------ Für `Rundenbefehle.kt`

    /** Eine Hub-Methode rufen, ohne auf Antwort zu warten. */
    internal fun senden(methode: String, vararg argumente: JsonElement) =
        draht.rufen(methode, *argumente)

    /** Eine Hub-Methode rufen und die Antwort abwarten; `null` bei toter Leitung. */
    internal suspend fun fragen(methode: String, vararg argumente: JsonElement): JsonElement? =
        runCatching { draht.frage(methode, *argumente) }.getOrNull()

    /** Den Stand ändern — für Befehle, die etwas Örtliches mitführen. */
    internal fun standAendern(tun: (Rundenstand) -> Rundenstand) = _stand.update(tun)

    // ----------------------------------------------------------------- Intern

    private fun setzen(roh: JsonElement) {
        val raum = runCatching {
            Netz.abgabe.decodeFromJsonElement(Raumzustand.serializer(), roh)
        }.getOrNull() ?: return

        // Der Server verschickt außerhalb seiner Raumsperre; zwei Stände können
        // sich überholen. Einen älteren zu übernehmen hieße, ihn bis zur
        // nächsten Änderung anzuzeigen — also verwerfen.
        val alt = _stand.value.raum
        if (alt != null && alt.code == raum.code && raum.version < alt.version) return

        // Das Funkprotokoll des Zustands ist die Wahrheit; die zwischenzeitlich
        // angehängten Radio-Ereignisse werden daran abgeglichen. Live trägt der
        // Zustand ohnehin nur die letzten 150 Zeilen — Kennzahlen daraus wären
        // falsch, aber zum Mitlesen reicht es.
        _stand.update { it.copy(raum = raum, code = raum.code, funk = raum.funkprotokoll) }
        nachStand(raum)
    }

    /**
     * Was jeder neue Stand geraderücken darf.
     *
     * <b>Die Sprecher hängen sonst nur an flüchtigen Ereignissen.</b> Geht ein
     * `TransmissionEnded` verloren (Funkloch, App im Hintergrund), bliebe der
     * Kanal für immer belegt. Der Server ist die Wahrheit: `funkkanaele` nennt
     * die belegten, alles andere ist frei.
     */
    private fun nachStand(raum: Raumzustand) {
        val ich = _stand.value.eigeneKennung
        _stand.update { alt ->
            val eigenes = raum.vehicles.firstOrNull { it.playerId == ich && ich.isNotEmpty() }
            alt.copy(
                sprecher = raum.funkkanaele
                    .filter { it.senderPlayerId != null }
                    .associate { (it.funkgruppe ?: "") to (it.senderName ?: "Unbekannt") },
                drahtSprecher = raum.drahtkanal.senderPlayerId
                    ?.takeIf { raum.drahtOffen }
                    ?.let { Sprecherangabe(it, raum.drahtkanal.senderName ?: "Unbekannt") },
                einsatzstellenSprecher = raum.einsatzstellenkanal.senderPlayerId
                    ?.let { Sprecherangabe(it, raum.einsatzstellenkanal.senderName ?: "Unbekannt") },
                drahtzeilen = if (raum.drahtOffen) alt.drahtzeilen else emptyList(),
                // Ein Einsatz, der diesem Fahrzeug nicht mehr gehört, gehört
                // auch vom Melder — gefragt wird nach dem Einsatz, nicht nach
                // `alarmOffen`: Das Quittieren schließt den Alarm am Server ja
                // auch, und die Meldung soll bis Status 3 stehen bleiben.
                alarm = alt.alarm?.takeUnless { eigenes != null && eigenes.einsatzId != it.incidentId },
            )
        }

        val s = _stand.value
        // Steht der Draht nicht mehr offen, ist auch die eigene Taste dort los.
        if (s.nebenleitung == Nebenleitung.Draht && !raum.drahtOffen) drahtSprechenBeenden()
        // Wer nicht mehr an der Einsatzstelle steht, spricht dort auch nicht mehr.
        if (s.nebenleitung == Nebenleitung.Einsatzstelle) {
            val eigenes = raum.vehicles.firstOrNull { it.playerId == ich }
            if (eigenes == null || eigenes.status != 4 || !eigenes.einsatzstelleErreicht) {
                einsatzstelleSprechenBeenden()
            }
        }
    }

    /** Die Lautsprecher und Nebenleitungen eines verlassenen Raums schließen. */
    private fun abraeumen() {
        sprechenAbbrechen()
        nebenleitungAbbrechen()
    }

    private fun lautsprecherSchliessen() {
        lautsprecher.schliessen()
        drahtLautsprecher.schliessen()
        einsatzstellenLautsprecher.schliessen()
        einzelrufLautsprecher.schliessen()
        telefonLautsprecher.schliessen()
        durchsageLautsprecher.schliessen()
    }

    private fun text(roh: JsonElement): String? =
        (roh as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

    /**
     * Ein ausdrückliches „nichts" als Argument.
     *
     * `ChooseRole` nimmt zwei Werte, und der zweite darf leer sein — aber er muss
     * dastehen. Ein Aufruf mit der falschen Argumentzahl verwirft der Hub
     * **still**; man sieht keinen Fehler, es passiert nur nichts.
     */
    private fun leer(): JsonElement = JsonNull

    /** Eine Liste von Texten als JSON-Feld. */
    private fun liste(werte: List<String>): JsonElement =
        buildJsonArray { werte.forEach { add(JsonPrimitive(it)) } }

    /** Derselbe Mantel wie in `Sitzung` — Laufzustand setzen, Fehler in einen Satz. */
    private fun arbeiten(tun: suspend () -> Unit) = viewModelScope.launch {
        _stand.update { it.copy(laeuft = true, fehler = null) }
        runCatching { tun() }
            .onFailure { f ->
                _stand.update { it.copy(fehler = f.message ?: "Etwas ist schiefgegangen.") }
            }
        _stand.update { it.copy(laeuft = false) }
    }

    override fun onCleared() {
        abraeumen()
        lautsprecherSchliessen()
        super.onCleared()
    }
}

/** Das Nein des Servers zu einem Beitritt — kein Netzfehler, sondern eine Antwort. */
private class Absage(meldung: String) : IllegalStateException(meldung)

/** Wohin der Wiedereintritt nach einem Abbruch geht. */
private data class Eintritt(val code: String, val name: String, val alsZuschauer: Boolean)

/** Wer eine Halbduplex-Leitung gerade hält — Spieler-Id und Anzeigename. */
data class Sprecherangabe(val playerId: String, val name: String)

/** Die Nebenleitungen, auf denen man außer dem Kreiskanal sprechen kann. */
enum class Nebenleitung { Draht, Einsatzstelle, Einzelruf, Notruf, Alarmmeldung }

/** Die Bevölkerungswarnung der Leitstelle — Text und Ankunft (Epoch-Millis). */
data class Bevoelkerungswarnung(val text: String, val um: Long)

/**
 * Eine offene Einwilligungsfrage aus einem abgewiesenen Beitritt — und wie es
 * nach dem Erteilen weitergeht.
 */
data class OffeneEinwilligung(
    val frage: Uebertragungsfrage,
    val code: String,
    val name: String,
    val alsZuschauer: Boolean,
)

/** Was über die laufende Runde bekannt ist. */
data class Rundenstand(
    val raum: Raumzustand? = null,
    /** Der Code, auch bevor der Raum steht — die Lobby zeigt ihn beim Verbinden. */
    val code: String = "",
    val lage: Funkverbindung.Lage = Funkverbindung.Lage.Getrennt,
    val laeuft: Boolean = false,
    val fehler: String? = null,
    /** Der Alarm, der gerade am Melder hängt — `null` heißt: keiner. */
    val alarm: Alarmmeldung? = null,
    /** Das Funkprotokoll — Zustand plus zwischenzeitliche Radio-Ereignisse. */
    val funk: List<Funkzeile> = emptyList(),
    /** Was nach Dienstende gutgeschrieben wurde. */
    val gutschrift: Gutschrift? = null,
    /** Ob die eigene Sprechtaste gerade sendet. */
    val sendet: Boolean = false,
    /** Nach dem Loslassen: der Server übersetzt die Durchsage noch. */
    val wirdVerstanden: Boolean = false,
    /** Wer gerade auf welcher Funkgruppe spricht — leer heißt: Kanal frei. */
    val sprecher: Map<String, String> = emptyMap(),
    /** Bis wann die Sendepause nach dem Funklimit gilt (Epoch-Millis). */
    val funkGesperrtBis: Long? = null,
    /** Ein Hinweis der Funkschicht — „Kanal belegt", Stille-Warnung. */
    val funkhinweis: String? = null,
    /** Ob dieser Platz ein Zuschauerplatz ist. */
    val zuschauer: Boolean = false,
    /** Drahtzeilen aus Push-Ereignissen — beim Vollstand abgeglichen. */
    val drahtzeilen: List<Drahtnachricht> = emptyList(),
    // --------------------------------------------------------------------
    // Runde, Teil 1 — was die Ansichten danach lesen.
    // --------------------------------------------------------------------
    /** Die eigene Spieler-Id — sie **ist** die Kontokennung (siehe `GameHub.Join`). */
    val eigeneKennung: String = "",
    /**
     * Warum man den Raum verloren hat — Rauswurf oder gescheiterter
     * Wiedereintritt. Solange er steht, zeigt der Rahmen die Fehlerseite mit
     * „Zurück zum Start".
     */
    val rausGrund: String? = null,
    /** Lobby-Chat-Zeilen aus Push-Ereignissen — beim Vollstand abgeglichen. */
    val lobbyzeilen: List<Lobbynachricht> = emptyList(),
    /** Die letzten dreißig Alarme am Melder — neueste zuerst. */
    val melderverlauf: List<Alarmmeldung> = emptyList(),
    /** Die Bevölkerungswarnung, die gerade als Band stehen soll. */
    val bevoelkerungswarnung: Bevoelkerungswarnung? = null,
    /** Wer auf dem Leitstellendraht spricht. */
    val drahtSprecher: Sprecherangabe? = null,
    /** Wer an der eigenen Einsatzstelle spricht. */
    val einsatzstellenSprecher: Sprecherangabe? = null,
    /** Auf welcher Nebenleitung man selbst gerade spricht — `null`: auf keiner. */
    val nebenleitung: Nebenleitung? = null,
    /** Der Server erkennt gerade eine gesprochene Frage am Notruftelefon. */
    val notrufWirdVerstanden: Boolean = false,
    /** Der Server erkennt gerade einen gesprochenen Satz im Einzelruf. */
    val einzelrufWirdVerstanden: Boolean = false,
    /** Der Anruf, der gerade am Ohr liegt — siehe `Runde.telefonAnrufSetzen`. */
    val telefonAnruf: String? = null,
    /** Ob am eigenen Platz ein Handy als Funkbegleiter hängt. */
    val begleiterGekoppelt: Boolean = false,
    /** Eine offene Einwilligungsfrage aus einem abgewiesenen Beitritt. */
    val einwilligung: OffeneEinwilligung? = null,
    /** Der Elternbogen im Fall „saß schon" — er kommt aus der Antwort, nicht aus dem Raum. */
    val elternbogenAmPlatz: String? = null,
) {
    /** Der Draht, wie er angezeigt wird: Vollstand plus Zwischenzeilen. */
    val drahtGesamt: List<Drahtnachricht>
        get() {
            val stand = raum?.leitstellendraht.orEmpty()
            val bekannt = stand.map { it.id }.toSet()
            return (stand + drahtzeilen.filter { it.id !in bekannt }).takeLast(200)
        }

    /** Der Lobby-Chat, wie er angezeigt wird: Vollstand plus Zwischenzeilen. */
    val lobbychatGesamt: List<Lobbynachricht>
        get() {
            val stand = raum?.lobbychat.orEmpty()
            val bekannt = stand.map { it.id }.toSet()
            return (stand + lobbyzeilen.filter { it.id !in bekannt }).takeLast(LOBBYZEILEN)
        }

    val drin: Boolean get() = raum != null

    /** Der eigene Platz — `null` am Zuschauerplatz und vor dem Beitritt. */
    val ich: de.pagerspass.pagerspass.netz.Spieler?
        get() = raum?.players?.firstOrNull { it.id == eigeneKennung && eigeneKennung.isNotEmpty() }

    /** Das eigene Fahrzeug — `null` an der Leitstelle. */
    val eigenesFahrzeug: de.pagerspass.pagerspass.netz.Rundenfahrzeug?
        get() = raum?.vehicles?.firstOrNull { it.playerId == eigeneKennung && eigeneKennung.isNotEmpty() }

    /** Der Einzelruf, an dem dieser Platz gerade beteiligt ist. */
    val eigenerEinzelruf: de.pagerspass.pagerspass.netz.Einzelruf?
        get() = raum?.einzelrufe?.firstOrNull { r ->
            eigeneKennung.isNotEmpty() && (
                r.vonPlayerId == eigeneKennung ||
                    r.zielPlayerId == eigeneKennung ||
                    r.angenommenVonPlayerId == eigeneKennung
                )
        }

    /** Der Anruf, dem Telefonstimme und erkannte Fragen gelten. */
    val aktiverAnruf: String?
        get() = telefonAnruf ?: raum?.anrufe?.firstOrNull {
            it.imGespraech && it.bearbeiterPlayerId == eigeneKennung && eigeneKennung.isNotEmpty()
        }?.id

    /** Ob die eigene Bitte um einen Leitstellenplatz noch aussteht. */
    val eigenePlatzanfrage: Boolean
        get() = raum?.platzanfragen?.any { it.playerId == eigeneKennung } == true

    /** Ob die eigene Bitte um einen Platz in der vollen Runde noch aussteht. */
    val eigeneBeitrittsanfrage: Boolean
        get() = raum?.beitrittsanfragen?.any { it.playerId == eigeneKennung } == true

    /**
     * Die eine Frage, die die Einwilligungsblende stellt — aus welcher der zwei
     * Quellen auch immer.
     *
     * <b>Der abgewiesene Beitritt hat Vorrang:</b> Wer draußen steht, hat keinen
     * Platz im Raum. Die zweite Quelle ist gerechnet: Die Leitstelle hat den
     * Streamer-Modus eingeschaltet oder den Kanal gewechselt, während man schon
     * saß — Kanal, Plattform und Name stehen im Raumzustand.
     */
    val einwilligungsfrage: Uebertragungsfrage?
        get() {
            einwilligung?.let { return it.frage }
            val r = raum ?: return null
            if (zuschauer) return null
            val kanal = r.settings.streamerkanal
            if (!r.settings.streamermodus || kanal.isNullOrBlank()) return null
            val mich = ich ?: return null
            if (mich.streamerfreigabe) return null
            // Der Streamer erklärt seine Übertragung, er willigt nicht in sie ein.
            if (mich.id == r.settings.streamerKontoId) return null
            return Uebertragungsfrage(
                raumCode = r.code,
                streamerName = r.players.firstOrNull { it.id == r.settings.streamerKontoId }?.name
                    ?: "Die Leitstelle",
                plattform = r.settings.streamerplattform,
                kanal = kanal,
                aufzeichnung = r.settings.streameraufzeichnung,
                fassung = UEBERTRAGUNG_FASSUNG,
                mindestalterAllein = UEBERTRAGUNG_MINDESTALTER,
                wartetAufEltern = elternbogenAmPlatz != null,
                elternbogen = elternbogenAmPlatz,
            )
        }
}

/**
 * Wie viele Funkzeilen die App hält.
 *
 * Der Server schickt live ohnehin nur die letzten 150; alles darüber wäre
 * Speicher für Zeilen, die nie jemand sieht.
 */
private const val FUNKZEILEN = 200

/** Wie viele Lobby-Chat-Zeilen die App hält — der Server schickt 100. */
private const val LOBBYZEILEN = 200

/** Wie viele Meldungen der Melderverlauf hält — dieselbe Zahl wie im Web. */
private const val MELDERVERLAUF = 30
