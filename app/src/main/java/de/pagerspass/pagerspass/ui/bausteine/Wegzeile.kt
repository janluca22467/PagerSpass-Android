package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Die Wegzeile — ein Menüeintrag mit Titel, Erklärung und Pfeil.
 *
 * Das Gegenstück zu `.wegzeile` in `base.css`. Sie ist der Baustein des
 * Startbildschirms und jeder Einstellungsliste: eine Zeile, die woandershin
 * führt.
 *
 * <b>Der Pfeil ist die zweite Auskunft darüber, dass die Zeile ein Weg ist.</b>
 * Die erste ist die Fläche mit Rand. Am Rechner kommt als dritte das Überfahren
 * dazu — am Handy, wo es das nicht gibt, sind es diese beiden, und deshalb steht
 * der Pfeil hier nicht zur Wahl.
 *
 * <b>Mehr Luft als eine gewöhnliche Auswahlzeile</b> (12/16 statt 8/12): Diese
 * hier ist zweizeilig.
 *
 * @param marke Offene Anfragen, ungelesene Nachrichten — die rote Zahl vor dem
 *   Pfeil.
 */
@Composable
fun Wegzeile(
    titel: String,
    beiDruck: () -> Unit,
    modifier: Modifier = Modifier,
    unterzeile: String? = null,
    zeichen: ImageVector? = null,
    marke: Int = 0,
    schild: String? = null,
    balkenfarbe: Color? = null,
    aktiv: Boolean = true,
) {
    val gedaempft = if (aktiv) 1f else 0.6f

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ziel.Normal)
            .background(Farben.Flaeche.copy(alpha = gedaempft), Rundung.Klein)
            .border(1.dp, Farben.Rand.copy(alpha = gedaempft), Rundung.Klein)
            // Der Balken links behält seine Farbe — sie ist die Information.
            // Ohne ihn ist die Zeile ein Kasten; mit ihm ist sie ein Weg.
            .drawBehind {
                drawRect(
                    color = (balkenfarbe ?: Farben.RandHell).copy(alpha = gedaempft),
                    size = size.copy(width = 3.dp.toPx()),
                )
            }
            .clickable(
                enabled = aktiv,
                onClick = beiDruck,
                role = Role.Button,
                indication = null,
                interactionSource = null,
            )
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        if (zeichen != null) Zeichenflaeche(zeichen)

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = titel,
                style = Schrift.Normal.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = Schrift.NORMAL * Schrift.ZEILE_KNAPP,
                ),
                color = Farben.Text.copy(alpha = gedaempft),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unterzeile != null) {
                Text(
                    text = unterzeile,
                    style = Schrift.Klein.copy(lineHeight = Schrift.KLEIN * Schrift.ZEILE_KNAPP),
                    color = Farben.TextSehrLeise.copy(alpha = gedaempft),
                )
            }
        }

        // Rechts steht genau eine Sache: das Schild, wenn es eines gibt („Premium",
        // „Bald"), sonst die rote Zahl, sonst der Pfeil wie bei jedem anderen Weg.
        when {
            schild != null -> Marke(schild)
            marke > 0 -> Markenzahl(marke)
            else -> Icon(
                imageVector = Zeichen.Weiter,
                contentDescription = null,
                tint = Farben.TextSehrLeise.copy(alpha = gedaempft),
                modifier = Modifier.size(21.dp),
            )
        }
    }
}
