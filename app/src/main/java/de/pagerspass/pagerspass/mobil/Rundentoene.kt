package de.pagerspass.pagerspass.mobil

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Die kleinen Töne der Runde — das Gegenstück zu den Hinweistönen in
 * `web/src/audio/sounds.ts`, soweit die App sie braucht.
 *
 * <b>Der Ton kommt vom Gerät, nicht aus einer Datei</b> — wie beim Melder
 * (`Melderblende`). `ToneGenerator` auf dem Benachrichtigungskanal folgt der
 * Lautstärke, die der Spieler dafür eingestellt hat; ein Hinweis auf eine
 * Erwähnung soll nicht auf der Alarm-Lautstärke plärren.
 *
 * <b>Vibration ist ein Zusatz, keine Bedingung.</b> Fehlt die Berechtigung oder
 * der Motor, bleibt es still beim Ton — `runCatching` um jeden Griff.
 */
class Rundentoene(private val zusammenhang: Context) {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Der kurze Anruf: jemand hat dich im Lobby-Chat oder auf dem Draht genannt. */
    fun erwaehnung() {
        ton(AudioManager.STREAM_NOTIFICATION, ToneGenerator.TONE_PROP_ACK, 180)
        vibrieren(longArrayOf(0, 60))
    }

    /**
     * Die Bevölkerungswarnung — der Doppelstoß einer Warn-App, rund zwei Sekunden.
     *
     * Absichtlich weder Melderton noch Anredeton: Die Warnung ist folgenlos fürs
     * Spiel. Laut ist nicht wirksam.
     */
    fun warnApp() {
        bereich.launch {
            val geber = runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }.getOrNull()
                ?: return@launch
            try {
                repeat(2) {
                    geber.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
                    delay(900)
                }
            } finally {
                geber.release()
            }
        }
        vibrieren(longArrayOf(0, 400, 200, 400, 200, 400))
    }

    /** Kanal belegt — das kurze tiefe Warnsignal des Funkgeräts. */
    fun belegt() = ton(AudioManager.STREAM_NOTIFICATION, ToneGenerator.TONE_PROP_NACK, 220)

    /**
     * Die Melderprobe aus der Lobby: fünf Töne wie am echten Gerät, dann Ruhe.
     *
     * Auf dem Alarmkanal, denn genau darum geht es beim Testen — ob der Melder
     * bei der eingestellten Alarm-Lautstärke zu hören ist.
     */
    fun melderprobe() {
        bereich.launch {
            val geber = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 90) }.getOrNull()
                ?: return@launch
            try {
                repeat(5) {
                    geber.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
                    delay(240)
                }
                delay(400)
            } finally {
                geber.release()
            }
        }
        vibrieren(longArrayOf(0, 400, 150, 400))
    }

    private fun ton(strom: Int, art: Int, dauer: Int) {
        bereich.launch {
            val geber = runCatching { ToneGenerator(strom, 80) }.getOrNull() ?: return@launch
            try {
                geber.startTone(art, dauer)
                delay(dauer.toLong() + 60)
            } finally {
                geber.release()
            }
        }
    }

    private fun vibrieren(muster: LongArray) {
        runCatching {
            val motor: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (zusammenhang.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                    ?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                zusammenhang.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (motor?.hasVibrator() == true) {
                motor.vibrate(VibrationEffect.createWaveform(muster, -1))
            }
        }
    }
}
