package de.pagerspass.pagerspass.ansichten

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

/**
 * Der Melder — das Alarmbild, das den Dienst unterbricht.
 *
 * Übertragen aus dem Melder des Webs (`components/melder/KlassikMelder.vue`):
 * Er liegt über allem (`--ebene-alarm`), er piept, bis jemand reagiert, und er
 * hat genau zwei Ausgänge — quittieren oder wegtippen. <b>Wegtippen bestätigt
 * nicht:</b> Der Alarm bleibt am Fahrzeug offen (`alarmOffen`), und die
 * Leitstelle sieht weiter, dass niemand reagiert hat.
 *
 * <b>Der Ton ist der gewählte.</b> Bis hierher piepte die Blende fünfmal mit dem
 * `ToneGenerator` des Systems; jetzt spielt sie den Alarmton aus dem Konto
 * (`Melderwerk`, dieselben Zahlen wie im Web), endlos und auf dem Alarmkanal —
 * und hält sich an die Alarmierungsart: Vibration heißt stumm, stumm heißt
 * weder Ton noch Motor.
 */
@Composable
fun Melderblende(
    alarm: Alarmmeldung,
    beiQuittieren: () -> Unit,
    beiWegtippen: () -> Unit,
    // Begleiter (Tonregler): der Melderregler, 0–100 — wie bisher 90, wenn keiner dreht.
    lautstaerke: Int = 90,
) {
    // Der Alarmton, bis jemand reagiert — der im Konto gewählte, in der
    // Dringlichkeit der Meldung, und nur, wenn die Alarmierungsart ihn vorsieht.
    val werk = de.pagerspass.pagerspass.mobil.rememberMelderwerk()
    val ton by de.pagerspass.pagerspass.mobil.rememberMelderTon()
    val art by de.pagerspass.pagerspass.mobil.rememberAlarmierungsart()
    val pegel = if (art == "vibration" || art == "stumm") 0f else lautstaerke.coerceIn(0, 100) / 100f
    LaunchedEffect(alarm.incidentId, alarm.zeit, ton, pegel) {
        werk.alarmStarten(ton, alarm.prioritaet, pegel)
    }
    LaunchedEffect(alarm.incidentId, alarm.zeit) {
        if (art == "voll" || art == "vibration") werk.vibrationAlarm()
    }

    // Aufräumen, falls der Rahmen die Blende abbaut, ohne dass jemand tippte.
    DisposableEffect(Unit) {
        onDispose {
            werk.alarmStoppen()
            werk.vibrationAus()
        }
    }

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
