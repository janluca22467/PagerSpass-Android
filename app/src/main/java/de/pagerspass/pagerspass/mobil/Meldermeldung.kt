package de.pagerspass.pagerspass.mobil

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.pagerspass.pagerspass.MainActivity
import de.pagerspass.pagerspass.netz.Alarmmeldung

/**
 * Die Systemmeldung des Melders — die „Mitteilung", die aufs Gerät geht.
 *
 * <b>Der Melder in der App reicht nur, solange die App vorn ist.</b> Wer
 * zwischen zwei Einsätzen in eine andere App wechselt, hört sonst nichts — und
 * ein Melder, der nur piept, wenn man ihn ansieht, ist keiner. Die
 * Systemmeldung trägt den Alarm nach draußen: hohe Wichtigkeit, Alarmton des
 * Systems, Vibration, und ein Tipp darauf führt zurück in den Dienst.
 *
 * <b>Ein Kanal, einmal angelegt.</b> Android merkt sich die Einstellungen je
 * Kanal — wer den Melder leise stellen will, tut das in den Systemeinstellungen,
 * und die App überstimmt ihn nicht.
 */
object Meldermeldung {

    private const val KANAL = "melder"

    fun kanalAnlegen(zusammenhang: Context) {
        val verwalter = zusammenhang.getSystemService(NotificationManager::class.java) ?: return

        val kanal = NotificationChannel(
            KANAL,
            "Melder",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alarmierungen während des Dienstes"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 600)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }

        verwalter.createNotificationChannel(kanal)
    }

    /** Ob die Meldung gezeigt werden darf — ab Android 13 eine eigene Berechtigung. */
    fun erlaubt(zusammenhang: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                zusammenhang,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    fun zeigen(zusammenhang: Context, alarm: Alarmmeldung) {
        if (!erlaubt(zusammenhang)) return
        val verwalter = zusammenhang.getSystemService(NotificationManager::class.java) ?: return

        val zurueck = PendingIntent.getActivity(
            zusammenhang,
            0,
            Intent(zusammenhang, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val meldung = NotificationCompat.Builder(zusammenhang, KANAL)
            .setSmallIcon(de.pagerspass.pagerspass.R.drawable.ic_melder)
            .setContentTitle("${alarm.stichwort} — ${alarm.stichwortText}".trim(' ', '—'))
            .setContentText(
                listOfNotNull(alarm.adresse.ifBlank { null }, alarm.ortsteil)
                    .joinToString(" · "),
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(zurueck)
            .build()

        // Eine Kennung je Einsatz: Ein zweiter Alarm zur selben Lage ersetzt
        // die Meldung, statt eine zweite daneben zu stellen.
        verwalter.notify(alarm.incidentId.hashCode(), meldung)
    }
}
