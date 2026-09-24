package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke

/**
 * Der Kasten — das Gegenstück zu `.flaeche` in `base.css`.
 *
 * Fläche, Rand, Rundung, Lichtkante und der Innenraum aus der Staffel. Wer nur
 * die Fläche braucht und den Innenraum selbst setzt (weil eine Liste bis an die
 * Kante läuft), nimmt `Modifier.flaeche()` direkt.
 *
 * @param marke Die Kante für „das hier gehört dir" — Dienstausweis, Einladung,
 *   Einweisung. <b>Sparsam einsetzen:</b> Wenn jeder zweite Kasten einer Seite
 *   sie trägt, sagt sie nichts mehr.
 * @param wartet Ob der markierte Kasten auf eine Antwort wartet — volles Amber
 *   statt des abgedunkelten. Ohne `marke` wirkungslos.
 */
@Composable
fun Kasten(
    modifier: Modifier = Modifier,
    farbe: Color = Farben.Flaeche,
    innenraum: Dp = Abstand.Gross,
    marke: Boolean = false,
    wartet: Boolean = false,
    abstandInnen: Dp = Abstand.Normal,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(abstandInnen),
        modifier = modifier
            .fillMaxWidth()
            .flaeche(farbe = farbe)
            .then(if (marke) Modifier.flaechenmarke(wartet) else Modifier)
            .padding(innenraum),
        content = inhalt,
    )
}

/**
 * Ein Abschnitt einer Seite.
 *
 * Im Web stand er als Kopie in fünfzehn Ansichten — buchstäblich dieselben sechs
 * Zeilen, mit einem Wert Abweichung hier und dort: 22 Punkte Blockabstand im
 * Dienstbuch, 18 in den Abzeichen, 20 in einem Dialog. Wer zwei dieser Seiten
 * hintereinander benutzt, sieht keinen Grund für den Unterschied — er sieht nur,
 * dass die Anwendung an jeder Stelle etwas anders atmet.
 */
@Composable
fun Abschnitt(
    ueberschrift: String? = null,
    modifier: Modifier = Modifier,
    weiterweg: @Composable (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier.fillMaxWidth().padding(top = Abstand.SehrGross),
    ) {
        if (ueberschrift != null || weiterweg != null) {
            Titelzeile(ueberschrift, weiterweg)
        }
        inhalt()
    }
}

/**
 * Überschrift links, Weiterweg rechts — die häufigste Kopfzeile eines Abschnitts.
 */
@Composable
fun Titelzeile(
    ueberschrift: String?,
    weiterweg: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (ueberschrift != null) {
            Ueberschrift(ueberschrift, Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        weiterweg?.invoke()
    }
}

/**
 * Die Überschrift eines Abschnitts.
 *
 * <b>In `TextLeise` und nicht leiser.</b> Im Web stand sie in `--text-sehr-leise`,
 * also **leiser als der Absatz, den sie überschreibt**. Am Handy ist das mit
 * 2.21.0 behoben worden, am Rechner blieb es stehen. Eine Überschrift, die man
 * später liest als ihren Inhalt, ordnet nichts.
 */
@Composable
fun Ueberschrift(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = Schrift.Klein.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
        color = Farben.TextLeise,
        modifier = modifier,
    )
}

/**
 * „Hier steht noch nichts."
 *
 * <b>Der gestrichelte Rahmen ist der eigentliche Punkt.</b> Er zeigt, dass an
 * dieser Stelle etwas stehen wird. Ohne ihn sieht eine leere Liste aus wie das
 * Ende der Seite.
 *
 * <b>Und er darf einen Ausweg haben.</b> „Noch nichts im Buch. Nach deinem ersten
 * Dienstende steht die erste Schicht hier" ist eine wahre Auskunft und eine
 * Sackgasse: Wer das Dienstbuch am ersten Tag öffnet, sieht dreimal denselben
 * Satz und nirgends einen Weg zum ersten Dienst. Wer einen nächsten Schritt
 * anzubieten hat, stellt einen Knopf in `ausweg`; wer keinen hat („In dieser
 * Schicht ist nichts passiert"), lässt es und bekommt genau den Satz.
 *
 * Der Text steht ausdrücklich **nicht** in Monospace. Die schreibt die Technik —
 * Zeiten, Rufnamen, Kennungen. Ein Satz, der einem Menschen etwas erklärt, ist
 * keins davon, und gesperrt über die ganze Breite liest er sich wie eine
 * Fehlermeldung.
 */
@Composable
fun Leerhinweis(
    text: String,
    modifier: Modifier = Modifier,
    ausweg: @Composable (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            // Gestrichelt von Hand: `Modifier.border` kann keine Strichelung —
            // `BorderStroke` nimmt einen Pinsel, aber keinen `PathEffect`. Die
            // Strichelung ist hier aber nicht Zierde, sondern die ganze Aussage
            // des Bausteins, also wird sie gezeichnet statt weggelassen.
            .drawBehind {
                val strich = 1.dp.toPx()
                val ecke = 14.dp.toPx()
                drawRoundRect(
                    color = Farben.Rand,
                    topLeft = androidx.compose.ui.geometry.Offset(strich / 2f, strich / 2f),
                    size = Size(size.width - strich, size.height - strich),
                    cornerRadius = CornerRadius(ecke, ecke),
                    style = Stroke(
                        width = strich,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                        ),
                    ),
                )
            }
            .padding(Abstand.Gross),
    ) {
        Text(text, style = Schrift.Normal, color = Farben.TextLeise)
        ausweg?.invoke()
    }
}
