package de.pagerspass.pagerspass.mobil

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.pagerspass.pagerspass.MainActivity
import de.pagerspass.pagerspass.R

/**
 * Die Systemmeldungen der Sozialschicht — Nachrichten, Kommentare, Wachenchat.
 *
 * <b>Warum lokal und nicht Push.</b> Der Server schickt Web-Push nur an Konten,
 * die gerade *keine* Leitung offen haben. Die App hält ihre Sozialleitung aber
 * auch im Hintergrund — für den Server ist man also da, und es käme nie eine
 * Meldung. Deshalb meldet die App selbst, was über die Leitung hereinkommt,
 * solange sie nicht zu sehen ist; die Schalter des Kontos gelten dabei genauso
 * wie beim Web-Push.
 *
 * <b>Ein Tipp führt an die richtige Stelle.</b> Die Meldung trägt ihren Weg in
 * der App als Zusatz (`EXTRA_WEG`); `SozialzielFolgen` liest ihn und navigiert.
 * Dieselben Wege, die das Web als Push-Ziel nennt: das Gespräch, der Eintrag,
 * die Wache.
 */
object Sozialmitteilung {

    /** Der Weg in der App, zu dem ein Tipp auf die Meldung führt — etwa `freunde/eintrag/42`. */
    const val EXTRA_WEG = "de.pagerspass.pagerspass.weg"

    private const val KANAL = "sozial"

    fun kanalAnlegen(zusammenhang: Context) {
        val verwalter = zusammenhang.getSystemService(NotificationManager::class.java) ?: return
        verwalter.createNotificationChannel(
            NotificationChannel(
                KANAL,
                "Freunde und Wache",
                // Wie die Hinweise: gewöhnlich wichtig, kein Alarmton — eine
                // Nachricht ist keine Alarmierung.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Direktnachrichten, Kommentare am Brett und der Wachenchat" },
        )
    }

    /**
     * Eine Meldung zeigen.
     *
     * @param schluessel Was dieselbe Meldung ist — eine zweite Nachricht desselben
     *   Freundes ersetzt die erste, statt eine Liste daneben aufzumachen.
     * @param weg Wohin ein Tipp führt.
     */
    fun zeigen(
        zusammenhang: Context,
        schluessel: String,
        titel: String,
        text: String,
        weg: String,
    ) {
        if (!Meldermeldung.erlaubt(zusammenhang)) return
        val verwalter = zusammenhang.getSystemService(NotificationManager::class.java) ?: return
        kanalAnlegen(zusammenhang)

        val nummer = schluessel.hashCode()

        // Je Meldung ein eigener Anfragecode: Android hält sonst zwei Absichten mit
        // verschiedenem Zusatz für dieselbe, und der Tipp auf die ältere Meldung
        // führte an das Ziel der jüngsten.
        val oeffnen = PendingIntent.getActivity(
            zusammenhang,
            nummer,
            Intent(zusammenhang, MainActivity::class.java)
                .putExtra(EXTRA_WEG, weg)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        verwalter.notify(
            nummer,
            NotificationCompat.Builder(zusammenhang, KANAL)
                .setSmallIcon(R.drawable.ic_melder)
                .setContentTitle(titel)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(oeffnen)
                .build(),
        )
    }
}

/**
 * Einem Tipp auf eine Sozialmeldung folgen.
 *
 * <b>Beide Wege, auf denen die Absicht ankommt:</b> beim kalten Start als
 * Absicht der Activity, und bei laufender App als neue Absicht an dieselbe
 * Activity (`SINGLE_TOP`). Der Zusatz wird nach dem Lesen entfernt — sonst führte
 * jedes Drehen des Geräts noch einmal an dieselbe Stelle.
 *
 * Navigiert wird einen Takt später, nicht im Hörer selbst: Beim kalten Start steht
 * der Navigationsgraph in diesem Augenblick noch nicht.
 */
@Composable
fun SozialzielFolgen(beiZiel: (String) -> Unit) {
    val zusammenhang = LocalContext.current
    val aktivitaet = remember(zusammenhang) { zusammenhang.aktivitaet() as? ComponentActivity }
    val aktuell by rememberUpdatedState(beiZiel)
    var ziel by remember { mutableStateOf<String?>(null) }

    DisposableEffect(aktivitaet) {
        if (aktivitaet == null) return@DisposableEffect onDispose {}

        fun pruefen(absicht: Intent?) {
            val weg = absicht?.getStringExtra(Sozialmitteilung.EXTRA_WEG) ?: return
            absicht.removeExtra(Sozialmitteilung.EXTRA_WEG)
            ziel = weg
        }

        pruefen(aktivitaet.intent)
        val hoerer = Consumer<Intent> { absicht ->
            // `setIntent`, damit auch ein späteres Lesen die neue Absicht sieht.
            aktivitaet.intent = absicht
            pruefen(absicht)
        }
        aktivitaet.addOnNewIntentListener(hoerer)
        onDispose { aktivitaet.removeOnNewIntentListener(hoerer) }
    }

    LaunchedEffect(ziel) {
        val weg = ziel ?: return@LaunchedEffect
        ziel = null
        runCatching { aktuell(weg) }
    }
}

/**
 * Die Sozialschicht an den Lebenslauf der App hängen: zu sehen oder nicht.
 *
 * Beim Zurückkommen holt sie eine aufgegebene Leitung zurück (wie
 * `visibilitychange` im Web); beim Gehen schaltet sie auf Systemmeldungen um.
 */
@Composable
fun SozialLebenslauf(sozial: Sozial) {
    val lebenslauf = LocalLifecycleOwner.current
    DisposableEffect(lebenslauf, sozial) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            when (ereignis) {
                Lifecycle.Event.ON_START -> sozial.vordergrund(true)
                Lifecycle.Event.ON_STOP -> sozial.vordergrund(false)
                else -> Unit
            }
        }
        lebenslauf.lifecycle.addObserver(beobachter)
        onDispose { lebenslauf.lifecycle.removeObserver(beobachter) }
    }
}

/** Die Activity hinter einem Zusammenhang — Compose reicht sie manchmal eingewickelt herein. */
private tailrec fun Context.aktivitaet(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.aktivitaet()
    else -> null
}
