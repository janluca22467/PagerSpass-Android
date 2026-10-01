package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Der Empfang — drei sendende Balken und der Schriftzug.
 *
 * <b>Er steht in dieser eigenen Datei, weil ihn zwei Seiten tragen</b> und zwar
 * bewusst gleich: die Anmeldung und der Startbildschirm. Wer sich eben angemeldet
 * hat, soll nicht auf einer fremden Seite ankommen — dasselbe Zeichen, derselbe
 * Schriftzug, dieselbe Zeile darunter. Zweimal abgeschrieben wäre das ab dem
 * ersten Umbau zweimal etwas anderes.
 */
@Composable
fun Empfang(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Sendezeichen()
        Schriftzug()
    }
}

/**
 * Der Schriftzug — „Pager" in Textfarbe, „Spass" in Amber.
 *
 * Die negative Sperrung ist keine Spielerei: Bei 26 Punkten steht der Name sonst
 * auseinandergezogen da und wirkt wie zwei Wörter. Er ist eines.
 */
@Composable
fun Schriftzug(modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            append("Pager")
            withStyle(SpanStyle(color = Farben.Amber)) { append("Spass") }
            append(Markenzusatz.zusatz)
        },
        style = Schrift.Schlagzeile.copy(letterSpacing = (-0.04).em),
        color = Farben.Text,
        modifier = modifier,
    )
}

/**
 * Drei Balken wie die Sendeanzeige eines Funkgeräts.
 *
 * <b>Sie senden wirklich.</b> Im Web ist das eine Endlosanimation von 1,4
 * Sekunden, jeder Balken um 0,18 Sekunden versetzt: Deckkraft von 0,3 auf 1 und
 * die Höhe auf 60 Prozent gestaucht. Das ist die einzige Bewegung auf beiden
 * Seiten und sie hat einen Zweck — sie sagt in einer Sekunde, worum es hier geht,
 * bevor irgendjemand den Untertitel gelesen hat.
 *
 * Die Höhen (9 · 19 · 13) sind die des Webs und ergeben zusammen den kurzen
 * Ausschlag, den ein Funkgerät beim Sprechen zeigt — nicht die gleichmäßige
 * Treppe, die man zeichnen würde, wenn man nicht hinsähe.
 */
@Composable
fun Sendezeichen(modifier: Modifier = Modifier) {
    val takt = rememberInfiniteTransition(label = "senden")

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Haar),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.height(19.dp),
    ) {
        listOf(9.dp to 0, 19.dp to 180, 13.dp to 360).forEach { (hoehe, versatz) ->
            val puls by takt.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, delayMillis = versatz),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "balken-$versatz",
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(hoehe)
                    .scale(scaleX = 1f, scaleY = 0.6f + 0.4f * puls)
                    .background(
                        color = Farben.Amber.copy(alpha = 0.3f + 0.7f * puls),
                        shape = Rundung.Winzig,
                    ),
            )
        }
    }
}

/**
 * Der Zusatz hinter dem Namen — „ 6“ auf dem Zweigstand v6.pagerspass.de, sonst nichts
 * (`marke.ts` im Web). Wer gegen den Zweigstand spielt, soll das am Schriftzug sehen
 * und nicht erst am Fehler, den es dort noch gibt. Gesetzt vom Rahmen, sobald der
 * eingestellte Server feststeht.
 */
object Markenzusatz {
    var server by mutableStateOf("")

    val zusatz: String
        get() = if (server.trimEnd('/') == de.pagerspass.pagerspass.netz.Server.V6) " 6" else ""
}
