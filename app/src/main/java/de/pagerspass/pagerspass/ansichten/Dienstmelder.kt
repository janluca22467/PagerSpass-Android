package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ansichten.melder.Melderanzeige
import de.pagerspass.pagerspass.ansichten.melder.Melderschacht
import de.pagerspass.pagerspass.ansichten.melder.einsatzort
import de.pagerspass.pagerspass.melder.Geraetegeraeusche
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.melder.funkzeit
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Der Melder im Dienst — der erste Reiter der Handyansicht im Web
 * (`FahrzeugView.vue`, `reiter === 'melder'`). Er steht nur bei der Bauart
 * „DME"; die Alarm-App liegt beim Alarm über allem, und „Im Funk" trägt der erste
 * Reiter das Funkgerät (`Funkdisplay`). Die Wahl selbst steht im Kopf
 * (`Melderwahl`).
 */
@Composable
fun ColumnScope.Dienstmelder(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    alarm: Alarmmeldung?,
) {
    val zusammenhang = LocalContext.current
    val geraet = remember { Meldergeraet.bereit(zusammenhang) }

    when (geraet.bauart) {
        "dme" -> Melderschacht(
            // Der laufende Alarm steht in der Blende darüber; hier bleibt das
            // Gerät in Ruhe und trägt Speicher und Menü.
            alarm = null,
            kennung = meins.funkrufname,
            standort = (meins.lat ?: meins.wacheLat)?.let { la -> (meins.lon ?: meins.wacheLon)?.let { lo -> la to lo } },
            ziel = einsatzort(raum, meins.einsatzId),
            modifier = Modifier.fillMaxWidth(),
        )
        "app" -> SehrLeise("Alarm-App: Der Alarm liegt beim Eingang in großer Schrift über dem ganzen Schirm.")
        else -> SehrLeise(
            if (alarm != null) "Der Alarm steht auf dem Funkgerät — im Reiter „Funk\"."
            else "Kein eigener Melder: Der Alarm steht auf dem Display des Funkgeräts im Reiter „Funk\".",
        )
    }
}

/**
 * Das Display des Handfunkgeräts — `FunkgeraetVoll.vue` in der Handyform.
 *
 * <b>Bauart „Im Funk": kein zweites Gerät.</b> Der Alarm steht auf dem Display,
 * das ohnehin in der Hand liegt — so, wie ein TETRA-Handgerät im echten Leben
 * alarmiert. Er verdrängt alles, nicht aus Platzmangel, sondern weil in diesem
 * Moment nichts anderes zählt; der Quittieren-Knopf steht gleich darunter, wo
 * man ihn im Laufen trifft, und nur solange das Gerät alarmiert.
 *
 * In Ruhe zeigt das Display, was ein Funkgerät in Ruhe zeigt: die geschaltete
 * Gruppe und den eigenen Rufnamen.
 */
@Composable
fun Funkdisplay(
    alarm: Alarmmeldung?,
    rufname: String,
    gruppe: String?,
    sprecher: String?,
    beiQuittieren: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val puls = if (alarm != null) {
        val lauf = rememberInfiniteTransition(label = "funkalarm")
        lauf.animateFloat(0.35f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "funkalarm-wert").value
    } else {
        0f
    }
    val gehaeuse = RoundedCornerShape(18.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF2B2F36), Color(0xFF14161A))), gehaeuse)
            .border(
                if (alarm != null) 2.dp else 1.dp,
                if (alarm != null) Farben.Signal.copy(alpha = puls) else Color(0xFF3A3F48),
                gehaeuse,
            )
            .padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("HRT-90", style = Schrift.MonoKlein, color = Color(0xFF8A94A3), modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .padding(end = Abstand.Winzig)
                    .background(
                        if (alarm != null) Farben.Signal.copy(alpha = 0.4f + 0.6f * puls) else Color(0xFF3A1113),
                        RoundedCornerShape(50),
                    )
                    .padding(5.dp),
            )
        }

        // Das Display: hinterleuchtetes Glas, Kopfzeile, darunter der Inhalt.
        val glas = RoundedCornerShape(6.dp)
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFB9D8C0), Color(0xFF93BFA0))), glas)
                .border(1.dp, Color(0xFF0B0D10), glas)
                .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
        ) {
            val tinte = Color(0xFF10261A)
            Text(
                text = if (alarm != null) "Alarm" else "Gruppenruf",
                style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                color = if (alarm != null) Color.White else tinte,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (alarm != null) Color(0xFFB3261E) else tinte.copy(alpha = 0.12f))
                    .padding(horizontal = 4.dp),
            )
            if (alarm != null) {
                // Dieselben Zeilen wie der Melder — eine Meldung, jedes Gerät.
                val zeilen = listOf("*ALARM*  ${funkzeit(alarm.zeit)}") + Melderanzeige.aus(alarm, true).zeilen
                zeilen.forEach { z ->
                    Text(z, style = Schrift.MonoKlein, color = tinte, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(gruppe ?: "Kanal", style = Schrift.MonoKlein, color = tinte, maxLines = 1)
                Text(
                    sprecher ?: "— — —",
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = tinte,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = Abstand.Winzig),
                )
                Text(rufname, style = Schrift.MonoKlein, color = tinte, maxLines = 1)
            }
        }

        if (alarm != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(lerpRot(puls), RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) {
                        Geraetegeraeusche.taste()
                        beiQuittieren()
                    },
            ) {
                Text("QUITTIEREN", style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
    }
}

private fun lerpRot(puls: Float): Color =
    androidx.compose.ui.graphics.lerp(Color(0xFF9E1F18), Color(0xFFE5352B), puls)

/**
 * Die Melderwahl — `MelderToggle.vue`: Piepser, Alarm-App oder „Im Funk". Sie steht
 * im Fahrzeugkopf und nicht im Melder-Reiter, denn bei „App" gibt es diesen Reiter
 * gar nicht — die Wahl wäre sonst unerreichbar.
 */
@Composable
fun Melderwahl(modifier: Modifier = Modifier) {
    val zusammenhang = LocalContext.current
    val geraet = remember { Meldergeraet.bereit(zusammenhang) }
    Segment(
        seiten = listOf("dme", "app", "funk"),
        gewaehlt = geraet.bauart,
        beiWahl = { geraet.bauartSetzen(it) },
        aufschrift = { when (it) { "dme" -> "DME"; "app" -> "App"; else -> "Im Funk" } },
        modifier = modifier,
    )
}

/** Die gewählte Bauart — `dme`, `app` oder `funk`. */
@Composable
fun melderbauart(): String {
    val zusammenhang = LocalContext.current
    return remember { Meldergeraet.bereit(zusammenhang) }.bauart
}

/** Ob der Alarm auf dem Funkgerät steht (Bauart „Im Funk") — für Reiter und Leiste. */
@Composable
fun alarmImFunk(): Boolean {
    val zusammenhang = LocalContext.current
    return remember { Meldergeraet.bereit(zusammenhang) }.bauart == "funk"
}
