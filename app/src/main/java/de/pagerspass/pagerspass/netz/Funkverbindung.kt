package de.pagerspass.pagerspass.netz

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Die Live-Verbindung in die Runde — das Gegenstück zu `web/src/api/signalr.ts`.
 *
 * <b>Warum ein eigener Klient und nicht `com.microsoft.signalr`.</b> Der
 * offizielle Java-Klient bringt RxJava mit und rechnet in `Single`/`Observable`;
 * die ganze App rechnet in Coroutinen. Das Protokoll selbst ist überschaubar:
 * ein Handschlag, danach JSON-Nachrichten, getrennt durch ein Steuerzeichen.
 * Was wirklich schwierig ist — die WebSocket-Verbindung — macht OkHttp.
 *
 * <b>Das Protokoll in vier Zeilen:</b>
 *
 *   1. Nach dem Verbinden `{"protocol":"json","version":1}` senden, mit dem
 *      Trennzeichen dahinter. Der Server antwortet mit `{}` — erst danach zählt
 *      alles Weitere.
 *   2. `{"type":1,"target":"Join","arguments":[…]}` ruft eine Hub-Methode auf.
 *      Mit `invocationId` kommt eine Antwort zurück (`type: 3`), ohne nicht.
 *   3. `{"type":1,"target":"RoomState","arguments":[…]}` vom Server ist ein
 *      Ereignis.
 *   4. `{"type":6}` ist ein Herzschlag und will einen zurück.
 *
 * <b>Das Merkmal muss URL-kodiert in die Abfrage.</b> Das ist die Falle, die wie
 * ein Heisenbug aussieht: Je nachdem, welches Merkmal gerade ausgewürfelt wurde,
 * enthält es ein `+` oder ein `/` — und dann antwortet der Hub mit „Bitte melde
 * dich zuerst an". Mit Kodierung geht es immer.
 */
class Funkverbindung(
    private val ablage: Ablage,
    /** Welcher Hub — das Spiel (`/hub/game`) oder das Soziale (`/hub/sozial`). */
    private val pfad: String = "/hub/game",
) {

    /** Wo die Verbindung gerade steht. */
    enum class Lage { Getrennt, Verbindet, Verbunden, Wiederverbinden }

    var beiLage: ((Lage) -> Unit)? = null

    /**
     * Nach jedem gelungenen Wiederaufbau — die Stelle für den erneuten Beitritt.
     *
     * <b>Die Verbindung allein holt niemanden in den Raum zurück.</b> Der Hub
     * merkt sich Raum und Platz an der Verbindungskennung, und die ist nach dem
     * Wiederaufbau eine neue: Ohne ein zweites `Join` stand die Leitung auf
     * „verbunden", und es kam nie wieder ein Ereignis. Das Web ruft genau hier
     * (`wiedereintreten` in `api/signalr.ts`).
     *
     * Gibt `false` zurück, wenn der Aufruf selbst scheiterte (Netz, Hubfehler) —
     * dann versucht es die Schleife weiter. Ein Nein des Servers ist `true`: Die
     * Frage ist beantwortet, nur eben mit Nein.
     */
    var beiWiederverbunden: (suspend () -> Boolean)? = null

    var lage: Lage = Lage.Getrennt
        private set(wert) {
            if (field == wert) return
            Log.i("Funk", "Lage: $field -> $wert")
            field = wert
            beiLage?.invoke(wert)
        }

    /** Was der Server von sich aus schickt — je Ereignisname ein Empfänger. */
    private val empfaenger = ConcurrentHashMap<String, (List<JsonElement>) -> Unit>()

    private val offen = ConcurrentHashMap<String, CompletableDeferred<JsonElement?>>()
    private val zaehler = AtomicLong(0)
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var draht: WebSocket? = null
    private var handschlag: CompletableDeferred<Unit>? = null

    /** Ob von selbst wieder verbunden werden soll. Ein `trennen()` schaltet es ab. */
    private var gewollt = false

    /**
     * Die laufende Wiederaufbau-Schleife — höchstens eine.
     *
     * <b>Ohne diesen Riegel liefen es bald mehrere.</b> Jeder gescheiterte
     * Versuch in der Schleife endet in `onFailure` und damit wieder in
     * `aufgeben()` — das startete eine zweite Schleife, die eine dritte, und
     * nach einem Funkloch riefen mehrere Schleifen gleichzeitig beim Server an.
     */
    @Volatile
    private var wiederaufbau: kotlinx.coroutines.Job? = null

    /** Kürzt die laufende Pause der Schleife ab — siehe `wiederaufnehmen`. */
    @Volatile
    private var abkuerzen: CompletableDeferred<Unit>? = null

    private val klient = OkHttpClient.Builder()
        // Der Hub schickt in kurzen Abständen einen Herzschlag; bleibt er aus,
        // ist die Leitung tot. Ohne diesen Takt merkt ein Handy im Funkloch
        // minutenlang nichts.
        .pingInterval(15, TimeUnit.SECONDS)
        // Eine offene Verbindung darf nicht in einen Lesezeitablauf laufen — sie
        // ist die meiste Zeit still, und das ist ihr Normalzustand.
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    /** Auf ein Ereignis des Servers hören. */
    fun auf(ereignis: String, empfang: (List<JsonElement>) -> Unit) {
        empfaenger[ereignis] = empfang
    }

    /**
     * Verbinden und den Handschlag abwarten.
     *
     * Erst danach darf gerufen werden — ein Aufruf vor dem Handschlag wirft der
     * Server weg, ohne sich zu beschweren.
     */
    suspend fun verbinden(imWiederaufbau: Boolean = false) {
        if (lage == Lage.Verbunden) return

        gewollt = true
        // Im Wiederaufbau bleibt es bei „wiederverbinden" — auch zwischen den
        // Versuchen. Sonst flackerte das Band zwischen zwei Aufschriften.
        lage = if (imWiederaufbau) Lage.Wiederverbinden else Lage.Verbindet

        val merkmal = ablage.merkmal().orEmpty()
        val adresse = ablage.server()
            .replace("https://", "wss://")
            .replace("http://", "ws://")
        val ziel = "$adresse$pfad?access_token=${URLEncoder.encode(merkmal, "UTF-8")}"

        val fertig = CompletableDeferred<Unit>()
        handschlag = fertig

        draht = klient.newWebSocket(
            Request.Builder().url(ziel).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send("""{"protocol":"json","version":1}""" + TRENNER)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    // Mehrere Nachrichten können in einem Paket ankommen — das
                    // Trennzeichen ist der einzige Weg, sie auseinanderzunehmen.
                    text.split(TRENNER).filter { it.isNotBlank() }.forEach { verarbeiten(it) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w("Funk", "Verbindung verloren (${response?.code})", t)
                    if (!fertig.isCompleted) fertig.completeExceptionally(t)
                    // Eine abgelöste Vorgängerin meldet sich hier noch nach — sie
                    // darf die frische Leitung nicht für tot erklären.
                    if (abgeloest(webSocket)) return
                    aufgeben()
                }

                /**
                 * Der Server will schließen — und will eine Antwort darauf.
                 *
                 * <b>Ohne diese Zeile bleibt die Verbindung halb offen.</b> Ein
                 * WebSocket wird von beiden Seiten geschlossen; wer den
                 * Schließ-Wunsch nicht bestätigt, hält die Gegenseite hin, und
                 * `onClosed` kommt erst nach dem Zeitablauf. Das sah hier aus
                 * wie eine Leitung, die grundlos abreißt.
                 */
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i("Funk", "Server schließt: $code $reason")
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i("Funk", "Geschlossen: $code $reason")
                    // Vor dem Handschlag geschlossen: Wer auf ihn wartet, darf
                    // nicht für immer warten — sonst hinge der Wiederaufbau hier.
                    if (!fertig.isCompleted) {
                        fertig.completeExceptionally(Netzfehler("Die Verbindung wurde geschlossen."))
                    }
                    if (abgeloest(webSocket)) return
                    aufgeben()
                }
            },
        )

        fertig.await()
        lage = Lage.Verbunden
    }

    /**
     * Eine Hub-Methode rufen und auf die Antwort warten.
     *
     * <b>Nicht jede Methode braucht das.</b> Wer eine `invocationId` mitschickt,
     * bekommt eine Antwort — auch bei einer Methode ohne Rückgabewert. Für alles,
     * dessen Ergebnis ohnehin als `RoomState` zurückkommt, ist `rufen` daneben
     * der richtige Weg: Es wartet nicht und hält die Oberfläche nicht auf.
     */
    suspend fun frage(methode: String, vararg argumente: JsonElement): JsonElement? {
        val nummer = zaehler.incrementAndGet().toString()
        val antwort = CompletableDeferred<JsonElement?>()
        offen[nummer] = antwort

        senden(
            buildJsonObject {
                put("type", 1)
                put("invocationId", nummer)
                put("target", methode)
                put("arguments", buildJsonArray { argumente.forEach { add(it) } })
            }.toString(),
        )

        return antwort.await()
    }

    /** Eine Hub-Methode rufen, ohne auf eine Antwort zu warten. */
    fun rufen(methode: String, vararg argumente: JsonElement) {
        senden(
            buildJsonObject {
                put("type", 1)
                put("target", methode)
                put("arguments", buildJsonArray { argumente.forEach { add(it) } })
            }.toString(),
        )
    }

    /** Die Verbindung absichtlich beenden — danach wird nicht wieder aufgebaut. */
    fun trennen() {
        gewollt = false
        wiederaufbau?.cancel()
        wiederaufbau = null
        draht?.close(1000, null)
        draht = null
        lage = Lage.Getrennt
    }

    /** Ob diese Leitung schon von einer neueren abgelöst ist. */
    private fun abgeloest(webSocket: WebSocket): Boolean {
        val aktuell = draht
        return aktuell != null && aktuell !== webSocket
    }

    private fun senden(rumpf: String) {
        draht?.send(rumpf + TRENNER)
    }

    private fun verarbeiten(rumpf: String) {
        // Die Antwort auf den Handschlag ist `{}` — und `{"error":"…"}`, wenn er
        // scheitert. Beides hat kein `type`, daran erkennt man sie.
        val knoten = runCatching { Netz.abgabe.parseToJsonElement(rumpf).jsonObject }.getOrNull()
            ?: return

        val art = knoten["type"]?.jsonPrimitive?.content?.toIntOrNull()

        if (art == null) {
            val fehler = knoten["error"]?.jsonPrimitive?.content
            if (fehler != null) handschlag?.completeExceptionally(Netzfehler(fehler))
            else handschlag?.complete(Unit)
            return
        }

        when (art) {
            // 1 = der Server ruft bei uns an.
            1 -> {
                val ziel = knoten["target"]?.jsonPrimitive?.content ?: return
                val argumente = knoten["arguments"]?.jsonArray?.toList().orEmpty()
                empfaenger[ziel]?.invoke(argumente)
            }

            // 3 = die Antwort auf eine unserer Fragen.
            3 -> {
                val nummer = knoten["invocationId"]?.jsonPrimitive?.content ?: return
                val warter = offen.remove(nummer) ?: return
                val fehler = knoten["error"]?.jsonPrimitive?.content

                if (fehler != null) warter.completeExceptionally(Netzfehler(fehler))
                else warter.complete(knoten["result"])
            }

            // 6 = Herzschlag. Er will einen zurück, sonst gilt die Leitung als tot.
            6 -> senden("""{"type":6}""")

            // 7 = der Server schließt.
            7 -> aufgeben()
        }
    }

    /**
     * Die Leitung ist weg — und kommt von selbst zurück, wenn sie soll.
     *
     * <b>Erst schnell, dann geduldiger.</b> Ein kurzer Funklochaussetzer soll
     * niemanden aus der Runde werfen; ein Server, der wirklich weg ist, soll
     * nicht im Sekundentakt angerufen werden. Dieselbe Staffel wie im Web.
     *
     * <b>Offene Fragen werden aufgelöst, nicht liegen gelassen.</b> Wer auf eine
     * Antwort wartet, die nie kommt, wartet sonst für immer — und die Ansicht
     * darüber zeigt bis zum Beenden der App einen drehenden Knopf.
     */
    private fun aufgeben() {
        offen.values.forEach { it.complete(null) }
        offen.clear()
        draht = null

        if (!gewollt) {
            lage = Lage.Getrennt
            return
        }

        // Läuft schon eine Schleife, ist das hier einer ihrer gescheiterten
        // Versuche — sie macht selbst weiter.
        if (wiederaufbau?.isActive == true) return

        wiederaufbauen()
    }

    /**
     * Die Schleife: warten, verbinden, wieder eintreten — bis es klappt oder die
     * Staffel durch ist.
     *
     * <b>Erst schnell, dann geduldiger.</b> Ein kurzer Funklochaussetzer soll
     * niemanden aus der Runde werfen; ein Server, der wirklich weg ist, soll
     * nicht im Sekundentakt angerufen werden. Die Wartezeiten sind die des Webs
     * hintereinander: erst SignalRs eigener Reconnect (0 … 15 s), dann dessen
     * Wiederaufbau (bis 30 s). Danach steht die Lage auf „getrennt", und es
     * bleibt bei den Anlässen von `wiederaufnehmen`.
     *
     * <b>Offene Fragen werden aufgelöst, nicht liegen gelassen</b> — das tut
     * `aufgeben()` vorher. Wer auf eine Antwort wartet, die nie kommt, wartet
     * sonst für immer.
     */
    private fun wiederaufbauen() {
        lage = Lage.Wiederverbinden
        wiederaufbau = bereich.launch {
            for (warten in WIEDERAUFBAU_MS) {
                if (!gewollt) return@launch
                val signal = CompletableDeferred<Unit>()
                abkuerzen = signal
                withTimeoutOrNull(warten) { signal.await() }
                abkuerzen = null
                if (!gewollt) return@launch

                if (runCatching { verbinden(imWiederaufbau = true) }.isFailure) {
                    lage = Lage.Wiederverbinden
                    continue
                }

                // Steht die Leitung, muss auch der Raum wieder stehen. Scheitert
                // schon der Aufruf, ist das kein Nein — dann weiter in der Staffel.
                val eingetreten = runCatching { beiWiederverbunden?.invoke() ?: true }
                    .getOrDefault(false)
                if (eingetreten) return@launch
                if (!gewollt) return@launch
            }
            if (gewollt && lage != Lage.Verbunden) lage = Lage.Getrennt
        }
    }

    /**
     * Holt eine aufgegebene Verbindung zurück — beim Zurückkommen in die App oder
     * auf „Neu verbinden".
     *
     * <b>Am Handy ist das der Normalfall.</b> Bildschirm aus, Aufzug, Funkloch:
     * Die Schleife hat irgendwann aufgegeben, die App sieht lebendig aus, und es
     * kommen keine Alarme mehr. Läuft die Schleife noch, wird nur ihre Pause
     * abgekürzt — ein zweiter Weg entsteht nicht. Ein bewusstes `trennen()`
     * bleibt davon unberührt.
     */
    fun wiederaufnehmen() {
        if (!gewollt) return
        if (wiederaufbau?.isActive == true) {
            abkuerzen?.complete(Unit)
            return
        }
        if (lage != Lage.Getrennt) return
        wiederaufbauen()
    }

    private companion object {
        /**
         * Das Trennzeichen zwischen zwei Nachrichten.
         *
         * `0x1E` — „record separator" aus ASCII. Es steht **hinter** jeder
         * Nachricht, nicht dazwischen: Wer es vergisst, wartet ewig auf eine
         * Antwort, die der Server nie schickt, weil er die Nachricht noch für
         * unvollständig hält.
         */
        const val TRENNER = ""

        /** Die Staffel des Wiederaufbaus — SignalRs Reconnect und der des Webs danach. */
        val WIEDERAUFBAU_MS = listOf(
            0L, 1_000L, 2_000L, 5_000L, 10_000L, 15_000L,
            20_000L, 30_000L, 30_000L, 30_000L, 30_000L,
        )
    }
}

/** Ein Text als JSON-Wert — für die Argumente der Hub-Aufrufe. */
fun wert(text: String): JsonElement = JsonPrimitive(text)

/** Eine Zahl als JSON-Wert. */
fun wert(zahl: Int): JsonElement = JsonPrimitive(zahl)

/** Ein Schalter als JSON-Wert. */
fun wert(schalter: Boolean): JsonElement = JsonPrimitive(schalter)
