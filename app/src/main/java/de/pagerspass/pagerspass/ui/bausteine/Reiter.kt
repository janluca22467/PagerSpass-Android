package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Die Reiterreihe — übertragen aus `.reiter` in `base.css`.
 *
 * <b>Alle Spalten gleich breit, und zwar wirklich.</b> Im Web stand hier
 * `minmax(0, 1fr)` und nicht `1fr`, und der Unterschied war nachgemessen: Ein
 * blankes `1fr` heißt `minmax(auto, 1fr)`, und dieses `auto` ist die Breite des
 * längsten Wortes. Sechs Spalten mit sechs verschiedenen Wörtern ergaben damit
 * sechs verschiedene Breiten — „Abzeichen" 80 Punkte, „Konto" 48 —, links
 * zusammengeschoben und rechts eine leere Lücke bis zur Kante. Das Gegenstück
 * hier ist `Modifier.weight(1f)` an jedem Reiter, und dass der Text im Reiter
 * abgeschnitten werden *darf*: Ohne das erzwingt der längste Text wieder seine
 * Spalte.
 */
@Composable
fun Reiterreihe(modifier: Modifier = Modifier, inhalt: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            // Die Kante der Reihe. Der Balken eines offenen Reiters liegt *auf*
            // ihr, nicht darunter — deshalb zeichnet der Reiter seinen Balken
            // ebenfalls an der Unterkante und überdeckt sie dort.
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(
                    color = Farben.Rand,
                    start = Offset(0f, size.height - strich / 2f),
                    end = Offset(size.width, size.height - strich / 2f),
                    strokeWidth = strich,
                )
            },
        content = inhalt,
    )
}

/**
 * Ein Reiter.
 *
 * <b>Er ist mal ein Weg und mal ein Umschalter — beides muss gleich aussehen.</b>
 * Wo er die Adresse wechselt (Dienstbuch, Freunde), führt er wirklich woandershin;
 * wo er nur die Ansicht umschaltet (Leitstelle, Zuschauer, Fahrzeug, Shop), darf
 * er nicht im Verlauf landen — sonst führt die Zurück-Taste aus der laufenden
 * Runde heraus. Dieser Baustein weiß davon nichts: Er bekommt „offen: ja/nein"
 * und einen Aufruf. Was der Aufruf tut, entscheidet die Stelle.
 */
@Composable
fun RowScope.Reiter(
    aufschrift: String,
    offen: Boolean,
    beiDruck: () -> Unit,
    modifier: Modifier = Modifier,
    zeichen: ImageVector? = null,
    marke: Int = 0,
) {
    val farbe = if (offen) Farben.Amber else Farben.TextLeise

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .weight(1f)
            .defaultMinSize(minHeight = Ziel.Normal)
            .clickable(
                onClick = beiDruck,
                role = Role.Tab,
                indication = null,
                interactionSource = null,
            )
            .drawBehind {
                if (!offen) return@drawBehind
                val balken = 2.dp.toPx()
                drawLine(
                    color = Farben.Amber,
                    start = Offset(0f, size.height - balken / 2f),
                    end = Offset(size.width, size.height - balken / 2f),
                    strokeWidth = balken,
                )
            }
            .padding(horizontal = Abstand.Winzig, vertical = Abstand.Klein),
    ) {
        if (zeichen != null) {
            Icon(
                imageVector = zeichen,
                contentDescription = null,
                tint = farbe,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = aufschrift,
            style = Schrift.Normal.copy(
                fontWeight = if (offen) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = farbe,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        // Die Zahl sitzt am Wort und nicht in der Ecke der Spalte: In der Ecke
        // stünde sie bei gleich breiten Spalten weit entfernt von dem, was sie
        // zählt.
        if (marke > 0) Markenzahl(marke)
    }
}
