package de.pagerspass.pagerspass.mobil

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Der Ton des Melders im Dienst — der echte Alarmton statt eines Pieps.
 *
 * Übertragen aus `alarmStarten`, `alarmStoppen`, `vibrationAlarm` und den
 * Bedientönen in `web/src/audio/sounds.ts`. Bis hierher piepte die App fünfmal
 * mit dem `ToneGenerator` des Systems, egal welcher Ton im Konto gewählt war;
 * jetzt spielt sie den gewählten — dieselben Zahlen wie im Web
 * ([Meldertonprobe]), in der Dringlichkeit der Meldung, **endlos**, bis jemand
 * quittiert.
 *
 * <b>Die Spur läuft in der Schleife.</b> Gerechnet werden ganze Zyklen
 * ([Meldertonprobe.alarmschleife]), die `AudioTrack` im statischen Betrieb mit
 * Schleifenpunkten spielt — kein Zeitgeber, der bei dunklem Bildschirm gedrosselt
 * wird und dann Lücken reißt (dieselbe Falle, die das Web mit seinem
 * Vorausplanen umgeht).
 *
 * <b>Auf dem Alarmkanal.</b> Der Melder folgt der Alarm-Lautstärke des Geräts,
 * nicht der Medienlautstärke, die beim Spielen oft auf null steht — der Regler
 * im Tonregler kommt obendrauf.
 */
class Melderwerk(private val zusammenhang: Context) {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var spur: AudioTrack? = null
    private var auftrag: Job? = null

    /** Was gerade tönt — derselbe Alarm ein zweites Mal ist schon der richtige. */
    @Volatile
    private var laufend: Triple<String, Int, Float>? = null

    /**
     * Den Alarmton starten — oder weiterlaufen lassen, wenn es schon derselbe ist.
     *
     * @param pegel 0–1; bei 0 bleibt es still (Vibration oder stumm).
     */
    fun alarmStarten(ton: String, prioritaet: Int, pegel: Float) {
        val schluessel = Triple(ton, prioritaet, pegel)
        if (laufend == schluessel) return
        alarmStoppen()
        laufend = schluessel
        if (pegel <= 0f) return

        auftrag = bereich.launch {
            val puffer = runCatching { Meldertonprobe.alarmschleife(ton, prioritaet) }.getOrNull()
                ?: return@launch
            val neu = runCatching { schleife(puffer, pegel) }.getOrNull() ?: return@launch
            // Wer inzwischen gestoppt hat, bekommt keine Spur mehr untergeschoben.
            if (laufend != schluessel) {
                freigeben(neu)
                return@launch
            }
            spur = neu
        }
    }

    /** Ton aus — hart, ohne Ausklingen, wie der Tastendruck am echten Gerät. */
    fun alarmStoppen() {
        laufend = null
        auftrag?.cancel()
        auftrag = null
        spur?.let { freigeben(it) }
        spur = null
    }

    /** Die Vibration des Alarms — lang, kurz, lang, wie im Web. */
    fun vibrationAlarm() = vibrieren(longArrayOf(0, 500, 180, 500, 180, 900))

    /** Vibration aus. */
    fun vibrationAus() {
        runCatching { motor()?.cancel() }
    }

    /** Der Quittungspiep des Piezos — die Antwort des Geräts, dass es die Taste angenommen hat. */
    fun quittung(pegel: Float) = piep(pegel, ToneGenerator.TONE_PROP_BEEP, 70)

    /** Der Tastenklick am Gehäuse. */
    fun taste(pegel: Float) = piep(pegel, ToneGenerator.TONE_PROP_ACK, 40)

    /** Die Ruferinnerung: Eine Meldung liegt unerledigt im Gerät. */
    fun erinnerung(pegel: Float) = piep(pegel, ToneGenerator.TONE_PROP_BEEP2, 140)

    /** Der Versandton des Funkgeräts nach einer Statusmeldung. */
    fun statusVersand(pegel: Float) = piep(pegel, ToneGenerator.TONE_PROP_ACK, 90)

    /** Der Notruf ist abgesetzt — drei hohe Töne. */
    fun notrufAbgesetzt(pegel: Float) {
        val laut = (pegel.coerceIn(0f, 1f) * 100).roundToInt()
        if (laut <= 0) return
        bereich.launch {
            val geber = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, laut) }.getOrNull() ?: return@launch
            try {
                repeat(3) {
                    geber.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 160)
                    delay(220)
                }
            } finally {
                geber.release()
            }
        }
    }

    // ---------------------------------------------------------- Signalhorn

    @Volatile
    private var horn: AudioTrack? = null

    /**
     * Das Martinshorn — lokal, wie im Web: Kein Mitspieler hört es, es ist die
     * Bedienung des eigenen Fahrzeugs. `stadt` ist die Folge a′–d″ (440/585 Hz),
     * `land` die tiefere Folge (360/480 Hz); je Ton 0,6 s beziehungsweise 1 s.
     */
    fun hornStarten(art: String, pegel: Float) {
        hornStoppen()
        if (pegel <= 0f) return
        val (tief, hoch, dauer) = if (art == "land") Triple(360.0, 480.0, 1.0) else Triple(440.0, 585.0, 0.6)
        bereich.launch {
            val laenge = (dauer * 2 * Meldertonprobe.RATE).roundToInt()
            val halb = laenge / 2
            var phase = 0.0
            val puffer = ShortArray(laenge) { i ->
                val f = if (i < halb) tief else hoch
                phase += f / Meldertonprobe.RATE
                // Ein Hauch Rechteck im Sinus — das Horn klingt nach Horn, nicht nach Flöte.
                val w = sin(2 * PI * phase) * 0.8 + (if ((phase % 1.0) < 0.5) 0.2 else -0.2)
                (w * 0.55 * Short.MAX_VALUE).roundToInt().toShort()
            }
            val neu = runCatching { schleife(puffer, pegel, AudioAttributes.USAGE_MEDIA) }.getOrNull()
                ?: return@launch
            horn = neu
        }
    }

    fun hornStoppen() {
        horn?.let { freigeben(it) }
        horn = null
    }

    /** Alles abräumen — die Seite geht. */
    fun schliessen() {
        alarmStoppen()
        hornStoppen()
        vibrationAus()
        bereich.cancel()
    }

    // ---------------------------------------------------------------- Intern

    private fun schleife(puffer: ShortArray, pegel: Float, zweck: Int = AudioAttributes.USAGE_ALARM): AudioTrack {
        val neu = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(zweck)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(Meldertonprobe.RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(puffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        neu.write(puffer, 0, puffer.size)
        neu.setLoopPoints(0, puffer.size, -1)
        neu.setVolume(pegel.coerceIn(0f, 1f))
        neu.play()
        return neu
    }

    private fun freigeben(s: AudioTrack) {
        runCatching { s.stop() }
        runCatching { s.release() }
    }

    private fun piep(pegel: Float, art: Int, dauer: Int) {
        val laut = (pegel.coerceIn(0f, 1f) * 100).roundToInt()
        if (laut <= 0) return
        bereich.launch {
            val geber = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, laut) }.getOrNull() ?: return@launch
            try {
                geber.startTone(art, dauer)
                delay(dauer.toLong() + 60)
            } finally {
                geber.release()
            }
        }
    }

    private fun motor(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (zusammenhang.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            zusammenhang.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private fun vibrieren(muster: LongArray) {
        runCatching {
            val m = motor()
            if (m?.hasVibrator() == true) m.vibrate(VibrationEffect.createWaveform(muster, -1))
        }
    }
}

/** Ein Melderwerk, das mit der Seite geht: Wer sie verlässt, nimmt keinen Ton mit. */
@Composable
fun rememberMelderwerk(): Melderwerk {
    val zusammenhang = LocalContext.current.applicationContext
    val werk = remember(zusammenhang) { Melderwerk(zusammenhang) }
    DisposableEffect(werk) { onDispose { werk.schliessen() } }
    return werk
}
