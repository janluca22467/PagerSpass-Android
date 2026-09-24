package de.pagerspass.pagerspass.ansichten

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Der Melder — das Alarmbild, das den Dienst unterbricht.
 *
 * Übertragen aus dem Melder des Webs (`components/melder/KlassikMelder.vue`):
 * Er liegt über allem (`--ebene-alarm`), er piept, bis jemand reagiert, und er
 * hat genau zwei Ausgänge — quittieren oder wegtippen. <b>Wegtippen bestätigt
 * nicht:</b> Der Alarm bleibt am Fahrzeug offen (`alarmOffen`), und die
 * Leitstelle sieht weiter, dass niemand reagiert hat.
 *
 * <b>Der Ton kommt vom Gerät, nicht aus einer Datei.</b> Fünf kurze Töne, eine
 * Pause, von vorn — die Kadenz eines Funkmeldeempfängers. `ToneGenerator` auf
 * dem Alarmkanal des Systems heißt: Er folgt der Alarm-Lautstärke des Geräts,
 * nicht der Medienlautstärke, die beim Spielen oft auf null steht.
 */
@Composable
fun Melderblende(
    alarm: Alarmmeldung,
    beiQuittieren: () -> Unit,
    beiWegtippen: () -> Unit,
) {
    // Der Piepton, bis jemand reagiert.
    LaunchedEffect(alarm.incidentId) {
        val ton = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 90) }.getOrNull()
        try {
            while (isActive) {
                repeat(5) {
                    ton?.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
                    delay(240)
                }
                delay(1_200)
            }
        } finally {
            ton?.release()
        }
    }

    // Aufräumen, falls der Rahmen die Blende abbaut, ohne dass jemand tippte.
    DisposableEffect(Unit) { onDispose { } }

    val puls = rememberInfiniteTransition(label = "alarm")
    val glut by puls.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alarm-glut",
    )

    Dialog(
        onDismissRequest = beiWegtippen,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Farben.Ueberlagerung)
                .padding(Abstand.Gross),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.BgTief, Rundung.Normal)
                    .border(3.dp, Farben.Signal.copy(alpha = glut), Rundung.Normal)
                    .padding(Abstand.Gross),
            ) {
                Text(
                    text = "ALARM",
                    style = Schrift.Etikett.copy(letterSpacing = 0.3.em),
                    color = Farben.SignalHell,
                )

                Text(
                    text = alarm.stichwort,
                    style = Schrift.Anzeige,
                    color = Farben.Text,
                    textAlign = TextAlign.Center,
                )

                if (alarm.stichwortText.isNotBlank()) {
                    Text(
                        text = alarm.stichwortText,
                        style = Schrift.Normal,
                        color = Farben.TextLeise,
                        textAlign = TextAlign.Center,
                    )
                }

                if (alarm.meldebild.isNotBlank()) {
                    Text(
                        text = alarm.meldebild,
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                        textAlign = TextAlign.Center,
                    )
                }

                Text(
                    text = listOfNotNull(alarm.adresse.ifBlank { null }, alarm.ortsteil)
                        .joinToString(" · "),
                    style = Schrift.MonoNormal,
                    color = Farben.AmberHell,
                    textAlign = TextAlign.Center,
                )

                // Was die Leitstelle selbst dazugesagt hat. Mit Absender davor und in
                // der Schrift des Fließtexts: Hier spricht ein Mensch, darüber steht,
                // was der Anrufer gemeldet hat — wenn beide sich widersprechen, muss
                // man sehen, welcher Satz von wem ist.
                alarm.zusatztext?.takeIf { it.isNotBlank() }?.let { meldung ->
                    Text(
                        text = "LST: $meldung",
                        style = Schrift.Normal,
                        color = Farben.Text,
                        textAlign = TextAlign.Center,
                    )
                }

                if (alarm.einheiten.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(
                            Abstand.Klein,
                            Alignment.CenterHorizontally,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    ) {
                        alarm.einheiten.forEach { Marke(it, farbe = Farben.TextLeise) }
                    }
                }

                Knopf(
                    aufschrift = "Alarm quittieren",
                    beiDruck = beiQuittieren,
                    art = Knopfart.Alarm,
                    breit = true,
                )
            }
        }
    }
}
