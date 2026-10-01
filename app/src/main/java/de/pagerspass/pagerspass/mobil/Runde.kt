package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.netz.Beitrittsergebnis
import de.pagerspass.pagerspass.netz.Funkzeile
import de.pagerspass.pagerspass.netz.Gutschrift
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.wert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 */
class Runde(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val draht = Funkverbindung(ablage)
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mikrofon = Mikrofon()
    private val lautsprecher = Lautsprecher().also { Tonpegel.laden(anwendung) }
    private var funklimit: kotlinx.coroutines.Job? = null

    private val _stand = MutableStateFlow(Rundenstand())
    val stand: StateFlow<Rundenstand> = _stand.asStateFlow()

    /**
     * Die Handgriffe über Rolle, Status und Funk hinaus — Lobby-Regler, Übergabe,
     * Einzelruf, Patientenbogen, Nebenleitungen. Sie teilen sich diese Verbindung;
     * eine Absage landet in derselben Meldung wie alle anderen.
     */
    val befehle = Raumbefehle(
        leitung = object : Raumbefehle.Leitung {
            override fun rufen(methode: String, vararg argumente: JsonElement) =
                draht.rufen(methode, *argumente)

            override suspend fun frage(methode: String, vararg argumente: JsonElement) =
                draht.frage(methode, *argumente)

            override fun auf(ereignis: String, empfang: (List<JsonElement>) -> Unit) =
                draht.auf(ereignis, empfang)
        },
        bereich = bereich,
        wege = de.pagerspass.pagerspass.netz.Raumwege(Netz(ablage)),
        eigeneKennung = { ablage.kennung() },
        beiFehler = { satz -> _stand.update { it.copy(fehler = satz) } },
    )

    /**
     * Der Haken für die Benachrichtigung.
     *
     * Der Rahmen hängt sich hier ein und zeigt die Systemmeldung mit Ton — das
     * ViewModel selbst fasst kein `NotificationManager` an: Es überlebt die
     * Activity, und eine Benachrichtigung braucht deren Fenster nicht, wohl aber
     * deren Berechtigungslage.
     */
    var beiAlarm: ((Alarmmeldung) -> Unit)? = null

    init {
        draht.beiLage = { lage -> _stand.update { it.copy(lage = lage) } }

        // Der ganze Raumzustand, bei jeder Änderung. Er ist das einzige
        // Ereignis, das die Lobby braucht — alles andere (Funk, Alarm,
        // Positionen) gehört zu Ansichten, die noch kommen.
        draht.auf("RoomState") { argumente ->
            argumente.firstOrNull()?.let { setzen(it) }
        }

        // <b>Eine Absage ist kein Fehler der Verbindung.</b> „Diesen Raum gibt es
        // nicht (mehr)", „Der Raum ist voll" — der Hub schickt sie als eigenes
        // Ereignis, und sie gehört in die Meldung, nicht ins Protokoll.
        draht.auf("Rejected") { argumente ->
            val grund = argumente.firstOrNull()?.let { text(it) }
            _stand.update { it.copy(fehler = grund ?: "Der Beitritt wurde abgelehnt.", laeuft = false) }
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

            _stand.update { it.copy(alarm = alarm) }
            beiAlarm?.invoke(alarm)
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

        // Nach Dienstende: was die Schicht dem eigenen Konto eingebracht hat.
        draht.auf("Erfahrung") { argumente ->
            val gutschrift = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Gutschrift.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update { it.copy(gutschrift = gutschrift) }
        }

        draht.auf("KickedFromRoom") { argumente ->
            val grund = argumente.firstOrNull()?.let { text(it) }
            _stand.update {
                Rundenstand(fehler = grund ?: "Die Leitstelle hat dich aus dem Raum entfernt.")
            }
            befehle.zuruecksetzen()
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
        // wie `Radio`: anhängen, beim nächsten Vollstand abgleichen.
        draht.auf("Draht") { argumente ->
            val zeile = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(
                        de.pagerspass.pagerspass.netz.Drahtnachricht.serializer(),
                        it,
                    )
                }.getOrNull()
            } ?: return@auf

            _stand.update { alt ->
                if (alt.drahtzeilen.any { it.id == zeile.id }) alt
                else alt.copy(drahtzeilen = (alt.drahtzeilen + zeile).takeLast(200))
            }
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
            _stand.update {
                it.copy(funkhinweis = "Kanal belegt – ${name ?: "jemand"} spricht.")
            }
        }

        // Der Text, den der Server aus der Durchsage verstanden hat. Er kommt
        // nur zum Sprecher zurück — erst `funken` macht daraus den Spruch, den
        // alle lesen. `null` heißt: nichts verstanden, der Platzhalter
        // „🎙️ Sprachdurchsage" steht dann schon im Protokoll.
        draht.auf("FunkErkannt") { argumente ->
            val erkannt = argumente.firstOrNull()?.let { text(it) }
            _stand.update { it.copy(wirdVerstanden = false) }
            if (!erkannt.isNullOrBlank()) funken(erkannt)
        }
    }

    /**
     * Einer Runde beitreten.
     *
     * <b>Der Beitritt muss der Verbindung zügig folgen.</b> Ein frisch
     * eröffneter Raum wird wieder aufgelöst, wenn niemand beitritt — im
     * API-Protokoll steht dann „Leere Lobby ABCDEF nach Prüfung aufgelöst", und
     * die App zeigt „Diesen Raum gibt es nicht (mehr)". Deshalb stehen Verbinden
     * und Beitreten hier in einem Aufruf und nicht in zweien.
     */
    fun beitreten(code: String, name: String, anrufeVorgabe: Boolean? = null) = arbeiten {
        val sauber = code.trim().uppercase()
        _stand.update { it.copy(code = sauber) }

        draht.verbinden()

        val antwort = draht.frage("Join", wert(sauber), wert(name))
        val ergebnis = antwort?.let {
            runCatching { Netz.abgabe.decodeFromJsonElement(Beitrittsergebnis.serializer(), it) }
                .getOrNull()
        }

        // Allein an der Übertragungseinwilligung gescheitert: eine offene Frage,
        // kein Fehlschlag. Gesetzt *vor* dem Wurf — der Dialog steht damit schon.
        einwilligungMerken(ergebnis, sauber, name, alsZuschauer = false)

        when {
            ergebnis == null -> throw IllegalStateException("Die Leitstelle antwortet nicht.")
            !ergebnis.ok -> throw IllegalStateException(
                ergebnis.fehler ?: "Diesen Raum gibt es nicht (mehr).",
            )
            else -> {
                _stand.update {
                    it.copy(raum = ergebnis.state, fehler = null, zuschauer = false, elternbogenAmPlatz = null)
                }
                // Für die Rückkehr nach einem Prozesstod — siehe `wiederAufnehmen`.
                ablage.rundeMerken(sauber)
                // Die App hat keine eigene Spracherkennung — der Server soll
                // Durchsagen mit Whisper in Text übersetzen (wie Firefox).
                draht.rufen("FunkerkennungWuenschen", wert(true))
                vorlesenAnmelden()
                // Die Vorgabe aus dem Einrichtungsbogen: Notrufe als Anruf — ja oder
                // nein. Nur für eine selbst eröffnete Runde und nur einmal, gleich
                // nach dem Eröffnen; danach gehört der Schalter der Lobby.
                val raum = ergebnis.state
                if (anrufeVorgabe != null && raum != null && raum.settings.telefonischeLeitstelle != anrufeVorgabe) {
                    befehle.einstellungen(Einstellungsaenderung(telefonischeLeitstelle = anrufeVorgabe))
                }
            }
        }
    }

    /**
     * Das Vorlesen kennt der Server nur für die Dauer der Runde. Wer es auf diesem
     * Gerät eingeschaltet hat (`funk.vorlesen`), meldet es beim Beitritt gleich wieder
     * an — sonst funkt er die erste halbe Schicht stumm. Nur der Einschaltfall: Ein
     * „aus" ist schon der Anfangszustand des Spielers.
     */
    private fun vorlesenAnmelden() {
        Geraeteeinstellungen.laden(getApplication())
        if (Geraeteeinstellungen.funkVorlesen.value == true) draht.rufen("FunkVorlesen", wert(true))
    }

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

        runCatching {
            draht.verbinden()
            val antwort = draht.frage(
                if (alsZuschauer) "JoinAsSpectator" else "Join",
                wert(code),
                wert(name),
            )
            val ergebnis = antwort?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Beitrittsergebnis.serializer(), it)
                }.getOrNull()
            }

            if (ergebnis?.ok == true) {
                _stand.update {
                    it.copy(
                        raum = ergebnis.state,
                        code = code,
                        fehler = null,
                        zuschauer = alsZuschauer,
                    )
                }
                if (!alsZuschauer) {
                    draht.rufen("FunkerkennungWuenschen", wert(true))
                    vorlesenAnmelden()
                }
            } else {
                ablage.rundeMerken(null)
                draht.trennen()
            }
        }.onFailure {
            // Kein Netz beim Aufwachen — der Code bleibt liegen, der nächste
            // Start versucht es wieder.
            draht.trennen()
        }
    }

    /**
     * Als Zuschauer beitreten — kein Platz, keine Rolle, nur die Sicht.
     *
     * <b>Der Zuschauer ist kein Spieler.</b> Er steht in `zuschauer`, nicht in
     * `players`, zählt nicht gegen `maxSpieler` und kann nichts senden — alle
     * Kommandos weisen ihn strukturell ab. Wer die Übung angelegt hat, bekommt
     * hier den Regieplatz.
     */
    fun zuschauen(code: String, name: String) = arbeiten {
        val sauber = code.trim().uppercase()
        _stand.update { it.copy(code = sauber) }

        draht.verbinden()
        val antwort = draht.frage("JoinAsSpectator", wert(sauber), wert(name))
        val ergebnis = antwort?.let {
            runCatching { Netz.abgabe.decodeFromJsonElement(Beitrittsergebnis.serializer(), it) }
                .getOrNull()
        }
        // Wie beim Beitritt als Spieler — der Name eines Zuschauers steht in der
        // Liste, und die Liste steht auf dem übertragenen Bild.
        einwilligungMerken(ergebnis, sauber, name, alsZuschauer = true)
        when {
            ergebnis == null -> throw IllegalStateException("Die Leitstelle antwortet nicht.")
            !ergebnis.ok -> throw IllegalStateException(
                ergebnis.fehler ?: "Zuschauen ist hier gerade nicht möglich.",
            )
            else -> {
                _stand.update {
                    it.copy(raum = ergebnis.state, fehler = null, zuschauer = true, elternbogenAmPlatz = null)
                }
                // Mit Vorzeichen gemerkt: Die Wiederaufnahme muss wissen, dass
                // sie als Zuschauer zurückkommt, nicht als Spieler.
                ablage.rundeMerken("Z:$sauber")
            }
        }
    }

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

    fun dienstBeenden() = draht.rufen("EndRound")

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
        if (s.sprecher.isNotEmpty()) {
            _stand.update { it.copy(funkhinweis = "Kanal belegt – ${s.sprecher.values.first()} spricht.") }
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
            kotlinx.coroutines.delay(Sprechfunk.MAX_SENDEDAUER_MS)
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
    ) = draht.rufen(
        "CreateIncident",
        wert(stichwort),
        wert(stichwortText),
        wert(meldebild),
        wert(adresse),
        wert(organisation),
        wert(prioritaet),
        JsonNull, // ortsteil
        meldender?.let { wert(it) } ?: JsonNull,
        empfohleneFahrzeuge?.let { wert(it) } ?: JsonNull,
        liste(empfohleneFaehigkeiten),
        JsonNull, // lat
        JsonNull, // lon
        anrufId?.let { wert(it) } ?: JsonNull,
        JsonNull, // ursprungEinsatzId
    )

    /**
     * Alarmieren — immer alle fünf Stellen von `AlarmVehicles`.
     *
     * <b>Hier standen früher nur drei.</b> SignalR bindet nach Stelle und
     * verwirft einen Aufruf mit falscher Argumentzahl im Ganzen; der Alarm aus der
     * App ging damit nie hinaus, ohne dass irgendwo eine Meldung stand.
     *
     * @param abrollbehaelter Fahrzeug-Id → Behälter-Vorlage für Wechsellader.
     * @param zusatztext Die eigene Meldung der Leitstelle unter der Automatik.
     * @param meldungId Die Kennung einer gesprochenen Meldung (siehe `Raumbefehle`).
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
            abrollbehaelter.forEach { (f, ab) -> put(f, JsonPrimitive(ab)) }
        },
        zusatztext?.trim()?.ifBlank { null }?.let { wert(it) } ?: JsonNull,
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
     * `frage` ist eine der sechs Faktenarten; `text` wäre die freie Frage aus
     * der Spracherkennung, die es in der App nicht gibt.
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
     * (`null` heißt: nicht anfassen).
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
        sprechenAbbrechen()
        lautsprecher.schliessen()
        befehle.zuruecksetzen()
        runCatching { draht.rufen("Leave") }
        draht.trennen()
        ablage.rundeMerken(null)
        _stand.value = Rundenstand()
    }

    fun fehlerWegnehmen() = _stand.update { it.copy(fehler = null) }

    private fun setzen(roh: JsonElement) {
        val raum = runCatching {
            Netz.abgabe.decodeFromJsonElement(Raumzustand.serializer(), roh)
        }.getOrNull() ?: return

        // Das Funkprotokoll des Zustands ist die Wahrheit; die zwischenzeitlich
        // angehängten Radio-Ereignisse werden daran abgeglichen. Live trägt der
        // Zustand ohnehin nur die letzten 150 Zeilen — Kennzahlen daraus wären
        // falsch, aber zum Mitlesen reicht es.
        _stand.update { it.copy(raum = raum, code = raum.code, funk = raum.funkprotokoll) }
        befehle.raumGeaendert(raum)
        if (_stand.value.zuschauer) zuschauerAufgenommen(raum)
    }

    /**
     * Ein Zuschauer, dessen Bitte um einen Platz angenommen wurde, steht ab jetzt
     * in `players` — und ist damit Spieler.
     *
     * <b>Das muss auch die Ablage erfahren.</b> Dort stand der Raum mit `Z:` davor;
     * nach einem Prozesstod käme das Gerät sonst als Zuschauer zurück und säße
     * neben seinem eigenen, verwaisten Platz. Im Web räumt `beitreten` den
     * Zuschauer-Merker ab — hier geschieht der Übergang ohne neuen Beitritt,
     * deshalb am Raumzustand.
     */
    private fun zuschauerAufgenommen(raum: Raumzustand) = bereich.launch {
        val kennung = ablage.kennung() ?: return@launch
        if (raum.players.none { it.id == kennung }) return@launch
        if (!_stand.value.zuschauer) return@launch
        _stand.update { it.copy(zuschauer = false) }
        ablage.rundeMerken(raum.code)
        // Als Spieler funkt er jetzt selbst — die Übersetzung der Durchsagen
        // gehört dazu wie beim gewöhnlichen Beitritt.
        draht.rufen("FunkerkennungWuenschen", wert(true))
    }

    // ------------------------------------------------- Übertragung einer Schicht

    private fun einwilligungMerken(
        ergebnis: Beitrittsergebnis?,
        code: String,
        name: String,
        alsZuschauer: Boolean,
    ) {
        val bedarf = ergebnis?.einwilligung
        _stand.update {
            it.copy(
                einwilligung = if (ergebnis?.ok == false && bedarf != null) {
                    Einwilligungsfrage(bedarf, alsZuschauer, code, name)
                } else {
                    null
                },
            )
        }
    }

    /**
     * Erteilt die Einwilligung und macht dort weiter, wo es unterbrochen wurde —
     * das Gegenstück zu `einwilligungErteilen` in `stores/spiel.ts`.
     *
     * <b>Sie kann gültig sein oder nicht.</b> Bei „unter 18" wartet sie auf den
     * Bogen der Erziehungsberechtigten; dann bleibt der Dialog offen und zeigt ihn.
     * `beiEnde` bekommt, ob sie gilt, und bei einem Fehlschlag den Satz dazu.
     */
    fun einwilligungErteilen(
        frage: de.pagerspass.pagerspass.netz.Einwilligungsbedarf,
        volljaehrig: Boolean,
        beiEnde: (gilt: Boolean, fehler: String?) -> Unit,
    ) = viewModelScope.launch {
        val gilt = runCatching {
            val kennung = ablage.kennung() ?: throw IllegalStateException("Bitte melde dich zuerst an.")
            val erteilt = de.pagerspass.pagerspass.netz.Kontowege(Netz(ablage))
                .einwilligungErteilen(kennung, frage.raumCode, volljaehrig, frage.fassung)

            if (!erteilt.gilt) {
                // Noch nicht durch: Der Bogen wandert in die offene Frage, damit der
                // Dialog ihn zeigen kann — auch im Fall „am Platz".
                _stand.update { s ->
                    val offen = s.einwilligung
                    if (offen != null) {
                        s.copy(
                            einwilligung = offen.copy(
                                bedarf = offen.bedarf.copy(wartetAufEltern = true, elternbogen = erteilt.elternbogen),
                            ),
                        )
                    } else {
                        s.copy(elternbogenAmPlatz = erteilt.elternbogen)
                    }
                }
                return@runCatching false
            }

            _stand.update { it.copy(elternbogenAmPlatz = null) }
            val offen = _stand.value.einwilligung
            if (offen != null) {
                // Der Beitritt, der vorhin abgewiesen wurde — jetzt noch einmal.
                if (offen.alsZuschauer) zuschauen(offen.code, offen.name) else beitreten(offen.code, offen.name)
            } else {
                befehle.streamerfreigabeNachtragen()
            }
            true
        }
        beiEnde(gilt.getOrDefault(false), gilt.exceptionOrNull()?.let { it.message ?: "Das hat gerade nicht geklappt." })
    }

    /**
     * Die Frage abbrechen. Ein abgewiesener Beitritt wird nur vergessen — man
     * stand ohnehin draußen. <b>Wer schon saß, verlässt die Runde</b>: Ein „nein"
     * ist nur folgenlos, wenn danach nichts mehr von einem übertragen werden kann.
     */
    fun einwilligungAblehnen() {
        if (_stand.value.einwilligung != null) {
            _stand.update { it.copy(einwilligung = null) }
            return
        }
        if (_stand.value.raum != null) verlassen()
    }

    private fun text(roh: JsonElement): String? = (roh as? JsonPrimitive)?.content

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
}

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
    val drahtzeilen: List<de.pagerspass.pagerspass.netz.Drahtnachricht> = emptyList(),
    /** Die offene Einwilligungsfrage eines abgewiesenen Beitritts (`Streamerdialog.kt`). */
    val einwilligung: Einwilligungsfrage? = null,
    /** Der Elternbogen im Fall „saß schon" — er kommt aus der Antwort des Servers. */
    val elternbogenAmPlatz: String? = null,
) {
    /** Der Draht, wie er angezeigt wird: Vollstand plus Zwischenzeilen. */
    val drahtGesamt: List<de.pagerspass.pagerspass.netz.Drahtnachricht>
        get() {
            val stand = raum?.leitstellendraht.orEmpty()
            val bekannt = stand.map { it.id }.toSet()
            return (stand + drahtzeilen.filter { it.id !in bekannt }).takeLast(200)
        }

    val drin: Boolean get() = raum != null
}

/**
 * Wie viele Funkzeilen die App hält.
 *
 * Der Server schickt live ohnehin nur die letzten 150; alles darüber wäre
 * Speicher für Zeilen, die nie jemand sieht.
 */
private const val FUNKZEILEN = 200

/**
 * Eine offene Einwilligungsfrage aus einem abgewiesenen Beitritt — und wie es nach
 * dem Erteilen weitergeht: als Spieler oder als Zuschauer, mit Code und Namen.
 *
 * <b>Sie steht an der Runde und nicht in der Ansicht, die beitreten wollte.</b> Es
 * sind viele Wege, die beitreten (Start, öffentliche Runden, Einladungen, Wache,
 * Gespräch); ein Dialog in jedem davon wäre derselbe Dialog mit derselben
 * Wiederholung hinterher.
 */
data class Einwilligungsfrage(
    val bedarf: de.pagerspass.pagerspass.netz.Einwilligungsbedarf,
    val alsZuschauer: Boolean,
    val code: String,
    val name: String,
)
