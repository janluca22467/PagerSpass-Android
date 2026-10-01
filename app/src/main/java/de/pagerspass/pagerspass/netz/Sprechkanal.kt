package de.pagerspass.pagerspass.netz

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Der eigene Kanal für Sprache — das Gegenstück zu `web/src/api/sprechkanal.ts`.
 *
 * <b>Warum ein zweiter WebSocket.</b> Über den Hub geht jedes 40-ms-Paket als
 * JSON mit Base64 darin, in derselben Leitung wie Betriebsstände und jeder
 * andere Aufruf; ein großer Zustand, und die Stimme wartet dahinter. Der
 * Sprechkanal (`/hub/sprechfunk`) ist roh und binär, und nichts steht vor ihr.
 *
 * <b>Der Hub bleibt der Rückfall — für jedes einzelne Paket.</b> Solange diese
 * Leitung nicht steht (Aufbau, Netzwechsel, ein Proxy, der keinen zweiten
 * WebSocket durchlässt), liefert [senden] `false`, und der Aufrufer schickt
 * wie bisher über den Hub. Kaputt gehen kann dadurch nichts, was vorher ging.
 *
 * <b>Angemeldet wird mit einem Schein</b>, den die Hubverbindung ausstellt
 * (`SprechkanalOeffnen`). Die Leitung spricht im Namen genau dieser
 * Hubverbindung; nach jedem Wiederverbinden des Hubs holt der Besitzer einen
 * neuen ([anbinden]).
 *
 * Das Rahmenformat steht in `api/PagerSpass.Api/Sprechkanal/Sprechrahmen.cs`
 * und muss mit den Zahlen hier übereinstimmen:
 * senden `[Weg][Länge Zusatz][Zusatz][PCM …]`, empfangen
 * `[Weg][Länge Absender][Absender][Länge Gruppe][Gruppe][PCM …]`.
 */
class Sprechkanal(private val ablage: Ablage, private val name: String) {

    /** Welche Leitung ein Rahmen meint — dieselben Zahlen wie `Sprechweg` am Server. */
    object Weg {
        const val FUNK = 1
        const val DRAHT = 2
        const val EINSATZSTELLE = 3
        const val EINZELRUF = 4
        const val NOTRUF = 5
        const val ALARMMELDUNG = 6
        const val WELT = 7
    }

    /** Eingehende Sprache — Weg, Absender, Gruppe, rohes PCM (Int16, little-endian). */
    var empfang: ((Int, String, String?, ByteArray) -> Unit)? = null

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val klient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile private var sockel: WebSocket? = null
    @Volatile private var bereitGemeldet = false

    /** Zählt bei jedem `anbinden`/`loesen` hoch — ein älterer Aufbau erkennt, dass er überholt ist. */
    private val generation = AtomicInteger(0)
    @Volatile private var scheinHolen: (suspend () -> String?)? = null
    private var versuch = 0
    private var wiederaufbau: Job? = null

    private val schrankennummer = AtomicInteger(0)
    private val offeneSchranken = ConcurrentHashMap<Int, CompletableDeferred<Unit>>()

    /** Pakete seit der letzten Schranke — ohne sie muss das Loslassen auf nichts warten. */
    private val seitSchranke = AtomicInteger(0)

    /** Ob Sprache gerade über diese Leitung geht. */
    val bereit: Boolean get() = bereitGemeldet && sockel != null

    /**
     * Die Leitung an eine (neue) Hubverbindung binden. `holen` fragt den Hub nach
     * einem Schein; liefert es nichts (älterer Server, nicht im Raum), bleibt es
     * beim Hub.
     */
    fun anbinden(holen: suspend () -> String?) {
        loesen()
        scheinHolen = holen
        versuch = 0
        val nr = generation.get()
        bereich.launch { oeffnen(nr) }
    }

    /** Die Leitung schließen und nicht wieder aufbauen — beim Verlassen. */
    fun loesen() {
        generation.incrementAndGet()
        scheinHolen = null
        wiederaufbau?.cancel()
        wiederaufbau = null
        schliessen()
    }

    /**
     * Ein Paket über diese Leitung schicken. `false` heißt: Die Leitung steht
     * gerade nicht (oder staut) — dann geht es über den Hub.
     */
    fun senden(weg: Int, pcm: ByteArray, zusatz: String = ""): Boolean {
        val s = sockel
        if (!bereit || s == null) return false
        if (s.queueSize() > MAX_RUECKSTAU) return false

        val zusatzBytes = zusatz.toByteArray(Charsets.UTF_8)
        if (zusatzBytes.size > 255) return false

        val rahmen = ByteArray(2 + zusatzBytes.size + pcm.size)
        rahmen[0] = weg.toByte()
        rahmen[1] = zusatzBytes.size.toByte()
        System.arraycopy(zusatzBytes, 0, rahmen, 2, zusatzBytes.size)
        System.arraycopy(pcm, 0, rahmen, 2 + zusatzBytes.size, pcm.size)

        if (!s.send(rahmen.toByteString())) return false
        seitSchranke.incrementAndGet()
        return true
    }

    /**
     * Wartet, bis der Server alles verarbeitet hat, was bis hierher über diese
     * Leitung hinausging — vor jedem Loslassen der Sprechtaste.
     *
     * <b>Warum.</b> Das Loslassen geht über den Hub, also über eine andere
     * Verbindung als die Stimme, und könnte die letzten Pakete überholen: Der
     * Kanal wäre frei, bevor der Satz angekommen ist. Kommt die Quittung nicht
     * binnen einer Sekunde, geht das Loslassen trotzdem hinaus — eine hängende
     * Sprechtaste wäre schlimmer als eine abgeschnittene letzte Silbe.
     */
    suspend fun schranke() {
        val s = sockel
        if (seitSchranke.get() == 0 || !bereit || s == null) return
        seitSchranke.set(0)

        val nummer = schrankennummer.incrementAndGet()
        val quittung = CompletableDeferred<Unit>()
        offeneSchranken[nummer] = quittung
        val rahmen = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
            .put(SCHRANKE.toByte()).putInt(nummer).array()
        if (s.send(rahmen.toByteString())) {
            withTimeoutOrNull(SCHRANKENFRIST_MS) { quittung.await() }
        }
        offeneSchranken.remove(nummer)
    }

    // ---------------------------------------------------------------- intern

    private suspend fun oeffnen(nr: Int) {
        val holen = scheinHolen ?: return
        val schein = runCatching { holen() }.getOrNull()
        if (nr != generation.get()) return

        // Kein Schein: ein Server ohne Sprechkanal oder gerade kein Platz. Dann
        // bleibt es beim Hub, bis das nächste `anbinden` kommt.
        if (schein.isNullOrBlank()) return

        val adresse = ablage.server()
            .replace("https://", "wss://")
            .replace("http://", "ws://")
        val ziel = "$adresse/hub/sprechfunk?schein=${URLEncoder.encode(schein, "UTF-8")}"

        val neu = runCatching {
            klient.newWebSocket(Request.Builder().url(ziel).build(), Hoerer(nr))
        }.getOrElse {
            Log.w("Sprechkanal", "($name) ließ sich nicht öffnen", it)
            wiederaufbauPlanen(nr)
            return
        }
        bereitGemeldet = false
        sockel = neu
    }

    private inner class Hoerer(private val nr: Int) : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            if (webSocket !== sockel) return
            empfangen(bytes.toByteArray())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = verloren(webSocket)

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w("Sprechkanal", "($name) Verbindung verloren (${response?.code})", t)
            verloren(webSocket)
        }

        private fun verloren(webSocket: WebSocket) {
            if (webSocket !== sockel) return
            sockel = null
            bereitGemeldet = false
            schrankenFreigeben()
            wiederaufbauPlanen(nr)
        }
    }

    private fun empfangen(rahmen: ByteArray) {
        if (rahmen.isEmpty()) return
        val art = rahmen[0].toInt() and 0xFF

        if (art == BEREIT) {
            bereitGemeldet = true
            // Erst ein Rahmen vom Server heißt „es steht wirklich“ — dann darf
            // der nächste Abbruch wieder schnell neu versucht werden.
            versuch = 0
            return
        }
        if (art == QUITTUNG) {
            if (rahmen.size != 5) return
            val nummer = ByteBuffer.wrap(rahmen, 1, 4).order(ByteOrder.LITTLE_ENDIAN).int
            offeneSchranken[nummer]?.complete(Unit)
            return
        }
        if (art !in Weg.FUNK..Weg.WELT) return
        val ziel = empfang ?: return

        var stelle = 1
        val absenderLaenge = rahmen[stelle++].toInt() and 0xFF
        if (rahmen.size < stelle + absenderLaenge + 1) return
        val absender = String(rahmen, stelle, absenderLaenge, Charsets.UTF_8)
        stelle += absenderLaenge

        val gruppeLaenge = rahmen[stelle++].toInt() and 0xFF
        if (rahmen.size < stelle + gruppeLaenge) return
        val gruppe = if (gruppeLaenge == 0) null else String(rahmen, stelle, gruppeLaenge, Charsets.UTF_8)
        stelle += gruppeLaenge

        if (stelle >= rahmen.size) return
        ziel(art, absender, gruppe, rahmen.copyOfRange(stelle, rahmen.size))
    }

    private fun wiederaufbauPlanen(nr: Int) {
        if (nr != generation.get() || scheinHolen == null) return
        if (versuch >= WIEDERAUFBAU_MS.size) return
        val warten = WIEDERAUFBAU_MS[versuch++]
        wiederaufbau = bereich.launch {
            delay(warten)
            if (nr == generation.get()) oeffnen(nr)
        }
    }

    /** Wer auf eine Quittung wartet, die nicht mehr kommen kann, geht weiter. */
    private fun schrankenFreigeben() {
        offeneSchranken.values.forEach { it.complete(Unit) }
        offeneSchranken.clear()
        seitSchranke.set(0)
    }

    private fun schliessen() {
        val s = sockel
        sockel = null
        bereitGemeldet = false
        schrankenFreigeben()
        runCatching { s?.close(1000, null) }
    }

    private companion object {
        const val BEREIT = 0x70
        const val SCHRANKE = 0x71
        const val QUITTUNG = 0x72

        /** Über diesem Rückstau (rund zwei Sekunden Sprache) geht das Paket lieber über den Hub. */
        const val MAX_RUECKSTAU = 64L * 1024

        const val SCHRANKENFRIST_MS = 1_000L

        /** Wartezeiten zwischen Wiederaufbauversuchen nach einem Abbruch. */
        val WIEDERAUFBAU_MS = longArrayOf(500, 1_500, 4_000, 10_000, 20_000)
    }
}
