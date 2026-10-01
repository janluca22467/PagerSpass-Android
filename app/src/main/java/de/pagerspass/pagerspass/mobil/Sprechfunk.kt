package de.pagerspass.pagerspass.mobil

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Die Audio-Seite des Sprechfunks — das Gegenstück zu `web/src/audio/ptt.ts`.
 *
 * <b>Dasselbe Format wie das Web, absichtlich.</b> Der Server reicht die
 * Pakete unverändert weiter; wer hier ein anderes Format sendet, klingt bei
 * jedem Web-Spieler kaputt. Also: rohes PCM, 16 kHz, mono, Int16, in Paketen
 * von 640 Samples (40 ms), Base64-kodiert — kein Container, damit jedes Paket
 * für sich abspielbar bleibt.
 *
 * <b>Kein MediaRecorder.</b> Der schreibt Container (AAC, 3GP) — genau das,
 * was das Protokoll nicht will. `AudioRecord` liefert die rohen Samples.
 */
object Sprechfunk {
    /** Muss zur Gegenseite passen — `ABTASTRATE` in ptt.ts. */
    const val ABTASTRATE = 16_000

    /** 640 Samples bei 16 kHz = 40 ms — die Paketgröße des Web. */
    const val PAKET_SAMPLES = 640

    /** Wie das Web: 20 Sekunden, dann ist Schluss. */
    const val MAX_SENDEDAUER_MS = 20_050L

    /** Nach dem Limit: eine Minute Pause (FUNKSENDEPAUSE_SEKUNDEN im Web). */
    const val SENDEPAUSE_MS = 60_000L
}

/**
 * Das Mikrofon — nimmt auf und liefert Base64-Pakete, bis es gestoppt wird.
 *
 * <b>Ein eigener Faden, kein Coroutine-Dispatcher.</b> `AudioRecord.read`
 * blockiert im 40-ms-Takt; auf einem geteilten IO-Dispatcher hungert das
 * andere Netzarbeit aus, und ein verspäteter `read` heißt hier Knacken auf
 * dem Kanal.
 */
class Mikrofon {

    private var geraet: AudioRecord? = null
    private var faden: Thread? = null
    private val laeuft = AtomicBoolean(false)

    /** Der höchste Pegel der Durchsage — für den Stille-Hinweis (wie im Web). */
    @Volatile var spitzenpegel: Float = 0f
        private set

    /** Wie viele Pakete rausgingen — 0 nach einer Sekunde heißt: Mikrofon tot. */
    @Volatile var pakete: Int = 0
        private set

    /**
     * Aufnahme starten. `beiPaket` bekommt jedes 40-ms-Paket Base64-kodiert.
     *
     * @return false, wenn das Mikrofon nicht zu bekommen war — die Taste zeigt
     *   dann den Hinweis, statt stumm zu „senden".
     */
    @SuppressLint("MissingPermission") // Die Taste prüft die Berechtigung vor dem Druck.
    fun starten(beiPaket: (String) -> Unit): Boolean {
        if (laeuft.get()) return true
        spitzenpegel = 0f
        pakete = 0

        val mindestPuffer = AudioRecord.getMinBufferSize(
            Sprechfunk.ABTASTRATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (mindestPuffer <= 0) return false

        val aufnahme = runCatching {
            AudioRecord(
                // Wie `echoCancellation` und Co. im Web: die Sprachquelle bringt
                // die Aufbereitung des Systems mit.
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                Sprechfunk.ABTASTRATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(mindestPuffer, Sprechfunk.PAKET_SAMPLES * 2 * 4),
            )
        }.getOrNull() ?: return false

        if (aufnahme.state != AudioRecord.STATE_INITIALIZED) {
            aufnahme.release()
            return false
        }

        geraet = aufnahme
        laeuft.set(true)
        aufnahme.startRecording()

        faden = Thread {
            val puffer = ShortArray(Sprechfunk.PAKET_SAMPLES)
            while (laeuft.get()) {
                var gelesen = 0
                while (gelesen < puffer.size && laeuft.get()) {
                    val n = aufnahme.read(puffer, gelesen, puffer.size - gelesen)
                    if (n <= 0) break
                    gelesen += n
                }
                if (gelesen < puffer.size) continue

                var spitze = spitzenpegel
                val bytes = ByteArray(puffer.size * 2)
                puffer.forEachIndexed { i, s ->
                    // Little-endian, wie `Int16Array` im Web.
                    bytes[i * 2] = (s.toInt() and 0xFF).toByte()
                    bytes[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
                    val pegel = kotlin.math.abs(s.toInt()) / 32768f
                    if (pegel > spitze) spitze = pegel
                }
                spitzenpegel = spitze
                pakete += 1
                beiPaket(Base64.encodeToString(bytes, Base64.NO_WRAP))
            }
        }.apply {
            name = "Sprechfunk-Mikrofon"
            start()
        }
        return true
    }

    fun stoppen() {
        laeuft.set(false)
        faden?.join(300)
        faden = null
        geraet?.let { g ->
            runCatching { g.stop() }
            g.release()
        }
        geraet = null
    }
}

/**
 * Der Lautsprecher — spielt ankommende Pakete, wie sie kommen.
 *
 * <b>`AudioTrack` im Stream-Modus ist der Jitter-Puffer.</b> Das Web baut sich
 * einen eigenen Abspielzeiger mit 140 ms Vorlauf; hier übernimmt das der
 * Puffer des Tracks — Pakete werden angehängt, die Hardware zieht gleichmäßig.
 * Bei einer Lücke setzt der Track kurz aus und läuft weiter, ohne zu stapeln.
 */
class Lautsprecher(
    /** Der Pegel dieser Leitung, 0 … 1 — der Funkregler des Tonreglers (`Tonstand`). */
    private val pegel: () -> Float = { Tonstand.funk() },
) {

    private var spur: AudioTrack? = null

    private fun sicherstellen(): AudioTrack {
        spur?.let { if (it.state == AudioTrack.STATE_INITIALIZED) return it }

        val mindest = AudioTrack.getMinBufferSize(
            Sprechfunk.ABTASTRATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val neu = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(Sprechfunk.ABTASTRATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
            // Rund 200 ms Puffer — genug gegen Netz-Zittern, wenig genug, dass
            // eine Durchsage nicht spürbar nachhängt.
            maxOf(mindest, Sprechfunk.ABTASTRATE / 5 * 2),
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        neu.play()
        spur = neu
        return neu
    }

    /** Ein Base64-Paket abspielen — Reihenfolge ist Ankunftsreihenfolge. */
    fun abspielen(paketB64: String) {
        val bytes = runCatching { Base64.decode(paketB64, Base64.DEFAULT) }.getOrNull() ?: return
        abspielen(bytes)
    }

    /** Ein rohes Paket abspielen — so, wie es der Sprechkanal liefert (ohne Base64). */
    @Synchronized
    fun abspielen(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        runCatching {
            val spur = sicherstellen()
            // Der Funkregler und „alles außer dem Melder stumm" (`Tonstand`) — je
            // Paket, damit ein Schieben mitten in der Durchsage sofort greift.
            spur.setVolume(pegel())
            spur.write(bytes, 0, bytes.size, AudioTrack.WRITE_NON_BLOCKING)
        }.onFailure { Log.w("Sprechfunk", "Abspielen fehlgeschlagen", it) }
    }

    @Synchronized
    fun schliessen() {
        spur?.let { s ->
            runCatching { s.stop() }
            s.release()
        }
        spur = null
    }
}
