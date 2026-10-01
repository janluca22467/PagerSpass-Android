package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Eine Zeile mit einem Haken davor.
 *
 * <b>Die ganze Zeile schaltet, nicht nur das Kästchen.</b> Im Web ist das ein
 * `<label>` um beides; hier `toggleable` am Zeilenrahmen. Der Unterschied ist
 * am Daumen der zwischen einem 18-Punkte-Ziel und einem, das über die ganze
 * Breite geht — und die drei Zustimmungen der Registrierung sind genau die
 * Stelle, an der niemand danebengreifen soll.
 *
 * <b>Warum ein eigener Haken und keine `Checkbox`.</b> Materials Kästchen bringt
 * seine eigenen Maße, seinen eigenen Rand und einen Ripple mit, den sonst nichts
 * in dieser Anwendung hat. Achtzehn Punkte, Amber, unser Radius — das ist die
 * Vorlage, und sie ist mit drei Zeilen genauer getroffen als mit einer
 * Übersteuerung.
 */
@Composable
fun Hakenzeile(
    text: AnnotatedString,
    an: Boolean,
    beiWechsel: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    aktiv: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = Ziel.Normal)
            .toggleable(
                value = an,
                enabled = aktiv,
                onValueChange = beiWechsel,
                role = Role.Checkbox,
                indication = null,
                interactionSource = null,
            ),
    ) {
        Haken(an = an, aktiv = aktiv)
        Text(
            text = text,
            style = Schrift.Klein,
            color = if (aktiv) Farben.Text else Farben.TextSehrLeise,
        )
    }
}

/** Dieselbe Zeile für einfachen Text. */
@Composable
fun Hakenzeile(
    text: String,
    an: Boolean,
    beiWechsel: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    aktiv: Boolean = true,
) = Hakenzeile(AnnotatedString(text), an, beiWechsel, modifier, aktiv)

/** Das Kästchen selbst — 18 Punkte, wie im Web. */
@Composable
private fun Haken(an: Boolean, aktiv: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(18.dp)
            .background(
                color = if (an) Farben.Amber else Color.Transparent,
                shape = Rundung.Winzig,
            )
            .border(
                width = 1.dp,
                color = when {
                    !aktiv -> Farben.Rand
                    an -> Farben.Amber
                    else -> Farben.RandHell
                },
                shape = Rundung.Winzig,
            ),
    ) {
        if (an) {
            Icon(
                imageVector = HAKEN,
                contentDescription = null,
                tint = Farben.AufAmber,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Der Haken — auf demselben 24er-Raster wie alle anderen Zeichen. */
/**
 * Der runde Haken der modernen Fahrzeugkarten — im Alarmdialog rechts an jeder
 * Karte. Gewählt ist er ein voller Amberkreis, sonst ein leerer Ring.
 */
@Composable
fun RunderHaken(an: Boolean, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(26.dp)
            .background(if (an) Farben.Amber else Color.Transparent, Rundung.Rund)
            .border(1.5.dp, if (an) Farben.Amber else Farben.RandHell, Rundung.Rund),
    ) {
        if (an) {
            Icon(
                imageVector = HAKEN,
                contentDescription = null,
                tint = Farben.AufAmber,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private val HAKEN: ImageVector = ImageVector.Builder(
    name = "haken",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    addPath(
        pathData = addPathNodes("M5 12.5 10 17.5 19 7"),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.6f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}.build()
