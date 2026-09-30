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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.pagerspass.pagerspass.ansichten.melder.Melderbild
import de.pagerspass.pagerspass.melder.Melderbauplan
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.melder.Melderkatalog
import de.pagerspass.pagerspass.melder.Melderspieler
import de.pagerspass.pagerspass.netz.Begleitergeraete

/**
 * Der Melder — das Alarmbild, das den Dienst unterbricht.
 *
 * Übertragen aus dem Melder des Webs (`components/melder/KlassikMelder.vue`):
 * Er liegt über allem (`--ebene-alarm`), er piept, bis jemand reagiert, und er
 * hat genau zwei Ausgänge — quittieren oder wegtippen. <b>Wegtippen bestätigt
 * nicht:</b> Der Alarm bleibt am Fahrzeug offen (`alarmOffen`), und die
 * Leitstelle sieht weiter, dass niemand reagiert hat.
 *
 * <b>Der Ton ist der gewählte Melderton</b> (`Melderspieler`, auf dem
 * Alarmkanal des Systems): Er folgt der Alarm-Lautstärke des Geräts, nicht der
 * Medienlautstärke, die beim Spielen oft auf null steht. Die Alarmierungsart
 * entscheidet, ob er klingt und ob das Handy vibriert.
 *
 * <b>Zwei Gestalten, wie im Web</b> (`MelderToggle.vue`): Als Piepser (`dme`)
 * steht das gewählte Gerät da — quittiert wird an seiner Quittierstelle oder am
 * Knopf darunter. Als Alarm-App (und „Im Funk", das am Handy kein Funkdisplay
 * hat) liegt die Meldung in großer Schrift über allem.
 */
@Composable
fun Melderblende(
    alarm: Alarmmeldung,
    beiQuittieren: () -> Unit,
    beiWegtippen: () -> Unit,
    /** Am Begleiter: der Gerätestand des Rechners, der hier gespiegelt wird. */
    geraete: Begleitergeraete? = null,
) {
    val zusammenhang = LocalContext.current
    val geraet = remember { Meldergeraet.bereit(zusammenhang) }

    // Was gilt: am Begleiter der Stand des Rechners, sonst die Wahl dieses Geräts.
    val bauart = geraete?.bauart ?: geraet.bauart
    val bauform = geraete?.bauform?.takeIf { Melderkatalog.bauform(it) != null } ?: geraet.wirksameBauform()
    val gesicht = geraete?.gesicht ?: geraet.gesicht
    val ton = geraete?.melderton?.takeIf { t -> Melderkatalog.TOENE.any { it.id == t } } ?: geraet.ton
    val art = Melderkatalog.alarmierungsart(geraet.alarmierungsart)

    // Der Ton, bis jemand reagiert — der gewählte Melderton auf dem Alarmkanal,
    // und die Vibration, wenn die Alarmierungsart sie will.
    LaunchedEffect(alarm.incidentId) {
        if (art.ton) Melderspieler.starten(zusammenhang, ton, alarm.prioritaet.coerceIn(1, 3))
        if (art.vibration) Melderspieler.vibrieren(zusammenhang)
    }

    // Aufräumen, falls der Rahmen die Blende abbaut, ohne dass jemand tippte.
    DisposableEffect(Unit) {
        onDispose {
            Melderspieler.stoppen("alarm")
            Melderspieler.vibrationAus()
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
            if (bauart == "dme") {
                Geraeteblende(
                    alarm = alarm,
                    bauform = bauform,
                    gesicht = gesicht,
                    plan = if (geraete == null) geraet.eigenerPlan().takeIf { bauform.startsWith("eigen:") } else null,
                    beiQuittieren = beiQuittieren,
                    beiWegtippen = beiWegtippen,
                )
                return@Box
            }
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

/**
 * Der Alarm als Gerät — das gewählte Gehäuse, groß, mit der Meldung auf seinem
 * Glas. Der Knopf darunter ist dieselbe Handlung wie die Quittierstelle am
 * Gerät: An einem gezeichneten Knopf vorbeizutippen darf im Einsatz nicht
 * heißen, dass der Alarm weiterläuft.
 */
@Composable
private fun Geraeteblende(
    alarm: Alarmmeldung,
    bauform: String,
    gesicht: String,
    plan: Melderbauplan?,
    beiQuittieren: () -> Unit,
    beiWegtippen: () -> Unit,
) {
    val quer = Melderkatalog.bauform(bauform)?.quer == true || (plan != null && plan.breite > plan.hoehe)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "ALARM",
            style = Schrift.Etikett.copy(letterSpacing = 0.3.em),
            color = Farben.SignalHell,
        )
        Melderbild(
            bauform = bauform,
            gesicht = gesicht,
            alarm = alarm,
            plan = plan,
            beiQuittieren = beiQuittieren,
            modifier = Modifier.widthIn(max = if (quer) 460.dp else 300.dp).heightIn(max = 440.dp),
        )
        Knopf(
            aufschrift = "Alarm quittieren",
            beiDruck = beiQuittieren,
            art = Knopfart.Alarm,
            breit = true,
        )
        Knopf(
            aufschrift = "Wegtippen — bleibt offen",
            beiDruck = beiWegtippen,
            art = Knopfart.Leise,
            kompakt = true,
        )
    }
}
