package de.pagerspass.pagerspass.mobil

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.pagerspass.pagerspass.netz.Raumzustand
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Die Töne im Raum, die nicht der Melder sind — das Klingeln eines ankommenden
 * Einzelrufs und der Warnton der Bevölkerungswarnung. Übertragen aus
 * `audio/sounds.ts` (`tetraRufStarten`, `warnAppTon`, `vibrationWarnung`).
 *
 * <b>Drei Sorten, drei Signaturen.</b> Der Melder piept fünfmal (siehe
 * `Melderblende`), der Einzelruf trillert, die Warnung stößt dreimal auf. Wer das
 * Gerät in der Tasche hat, soll sie auseinanderhalten können, ohne hinzusehen —
 * deshalb auch drei verschiedene Vibrationsfolgen.
 *
 * <b>Gerechnet und nicht aus Dateien.</b> Wie im Web entstehen die Töne aus
 * Formeln: Eine Datei müsste mitgeliefert werden, und die Klänge sollen denen im
 * Browser gleichen — also dieselben Frequenzen, dieselben Dauern.
 */
object Raumtoene {

    private const val RATE = 22_050

    /** Das Klingeln läuft, bis angenommen, abgewiesen oder aufgelegt wird. */
    private var ruf: AudioTrack? = null

    /**
     * Der Warnton einer Warn-App: drei Stöße, jeder ein aufsteigender Zweiklang
     * (740 auf 988 Hz, dann 988 Hz), zwei Sekunden, dann ist Ruhe. Rechteck wie im
     * Web — eine Warnung, die klingt wie eine Anrede, ist keine.
     */
    fun warnung(zusammenhang: Context) {
        val proben = FloatArray((RATE * 1.9).toInt())
        repeat(3) { i ->
            val ab = i * 0.62
            ton(proben, 740.0, 988.0, ab, 0.2, 0.3, rechteck = true)
            ton(proben, 988.0, 988.0, ab + 0.22, 0.26, 0.3, rechteck = true)
        }
        einmal(proben, AudioAttributes.USAGE_ALARM)
        vibrieren(zusammenhang, longArrayOf(0, 120, 90, 120, 90, 420))
    }

    /**
     * Der ankommende Einzelruf — zwei Sekunden gewobbeltes Trillern um 775 Hz
     * (±85 Hz, 14-mal je Sekunde), zwei Sekunden Pause, endlos. Das ist der Ton,
     * den man als TETRA-Klang wiedererkennt, noch bevor man weiß, wessen Gerät da
     * liegt.
     */
    fun rufStarten(zusammenhang: Context) {
        if (ruf != null) return
        val proben = FloatArray(RATE * 4)
        val dauer = 2.0
        var phase = 0.0
        for (n in 0 until (RATE * dauer).toInt()) {
            val t = n.toDouble() / RATE
            // Dreieck als Wobbel — derselbe Verlauf wie der `triangle`-Oszillator.
            val dreieck = 1 - 4 * abs(((t * 14) % 1.0) - 0.5)
            val frequenz = 775 + 85 * dreieck
            phase += 2 * PI * frequenz / RATE
            val huelle = minOf(1.0, t / 0.01, (dauer - t) / 0.01)
            proben[n] = (sin(phase) * 0.2 * huelle).toFloat()
        }
        ruf = spur(proben, AudioAttributes.USAGE_NOTIFICATION_RINGTONE, schleife = true)?.also {
            runCatching { it.play() }
        }
        vibrieren(zusammenhang, longArrayOf(0, 400, 200, 400, 3000), wiederholen = 0)
    }

    fun rufStoppen(zusammenhang: Context) {
        ruf?.let { s ->
            runCatching { s.stop() }
            s.release()
        }
        if (ruf != null) vibratorVon(zusammenhang)?.cancel()
        ruf = null
    }

    // ------------------------------------------------------------ Werkzeug

    private fun ton(
        proben: FloatArray,
        von: Double,
        bis: Double,
        ab: Double,
        dauer: Double,
        pegel: Double,
        rechteck: Boolean,
    ) {
        val start = (ab * RATE).toInt()
        val laenge = (dauer * RATE).toInt()
        var phase = 0.0
        for (n in 0 until laenge) {
            val i = start + n
            if (i >= proben.size) break
            val anteil = n.toDouble() / laenge
            phase += 2 * PI * (von + (bis - von) * anteil) / RATE
            val welle = if (rechteck) (if (sin(phase) >= 0) 1.0 else -1.0) * 0.5 else sin(phase)
            val huelle = minOf(1.0, n / (0.005 * RATE), (laenge - n) / (0.02 * RATE))
            proben[i] += (welle * pegel * huelle).toFloat()
        }
    }

    private fun einmal(proben: FloatArray, zweck: Int) {
        val s = spur(proben, zweck, schleife = false) ?: return
        runCatching { s.play() }
        // Nach dem Ende freigeben — ein fertiger statischer Track hält sonst seinen
        // Puffer, bis der Prozess geht.
        Thread {
            Thread.sleep((proben.size * 1000L / RATE) + 300)
            runCatching { s.stop() }
            s.release()
        }.start()
    }

    private fun spur(proben: FloatArray, zweck: Int, schleife: Boolean): AudioTrack? = runCatching {
        val pcm = ShortArray(proben.size) { (proben[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
        val spur = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(zweck)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
            pcm.size * 2,
            AudioTrack.MODE_STATIC,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        spur.write(pcm, 0, pcm.size)
        if (schleife) spur.setLoopPoints(0, pcm.size, -1)
        spur
    }.onFailure { Log.w("Raumtoene", "Ton nicht zu spielen", it) }.getOrNull()

    @Suppress("DEPRECATION")
    private fun vibratorVon(zusammenhang: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (zusammenhang.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            zusammenhang.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private fun vibrieren(zusammenhang: Context, muster: LongArray, wiederholen: Int = -1) {
        runCatching {
            vibratorVon(zusammenhang)?.vibrate(VibrationEffect.createWaveform(muster, wiederholen))
        }
    }
}

/**
 * Hängt die Töne an den Raumzustand — am Zustand und nicht an einem Ereignis,
 * aus demselben Grund wie im Web: Solange etwas klingelt, klingelt es hörbar, und
 * es hört genau dann auf, wenn der Ruf nicht mehr klingelt — egal, wer ihn beendet.
 */
@Composable
fun Raumtoene(raum: Raumzustand?, warnungUm: Long, eigeneKennung: String) {
    val zusammenhang = LocalContext.current.applicationContext
    val ichLeitstelle = raum?.players?.firstOrNull { it.id == eigeneKennung }?.istLeitstelle == true
    // Klingelt es bei mir? Bei einem Leitstellenruf an jedem Leitstellenplatz.
    val klingelt = raum?.einzelrufe?.any {
        it.klingelt && (it.zielPlayerId == eigeneKennung || (it.zielPlayerId == null && ichLeitstelle))
    } == true

    LaunchedEffect(klingelt) {
        if (klingelt) Raumtoene.rufStarten(zusammenhang) else Raumtoene.rufStoppen(zusammenhang)
    }
    DisposableEffect(Unit) { onDispose { Raumtoene.rufStoppen(zusammenhang) } }

    // Die Warnung klingt einmal je Eingang — `warnungUm` wechselt mit jeder neuen.
    val gehoert = remember { longArrayOf(warnungUm) }
    LaunchedEffect(warnungUm) {
        if (warnungUm > 0 && warnungUm != gehoert[0]) Raumtoene.warnung(zusammenhang)
        gehoert[0] = warnungUm
    }
}
