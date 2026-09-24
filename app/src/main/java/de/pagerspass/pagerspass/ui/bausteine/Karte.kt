package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke

/**
 * Die Karte — ein Kasten mit Kopf, Inhalt und einer Knopfreihe am Fuß.
 *
 * Sie ist der Baustein für „hier ist etwas zu tun, und zwar mehr als ein Druck":
 * „Dienst aufnehmen" (Bundesland, Landkreis, besetzen), „Einer Runde beitreten"
 * (Raumcode, beitreten). Der Unterschied zur Wegzeile ist genau der: Die Wegzeile
 * führt woandershin, die Karte will vorher etwas wissen.
 *
 * @param haupt Ob die Karte den Hauptweg trägt — sie bekommt dann die
 *   amberfarbene Kante links. <b>Höchstens eine je Bildschirm:</b> „Ein Menü, in
 *   dem alle Einträge gleich laut sind, hat keinen ersten."
 */
@Composable
fun Karte(
    titel: String,
    modifier: Modifier = Modifier,
    zeichen: ImageVector? = null,
    text: String? = null,
    haupt: Boolean = false,
    knoepfe: @Composable (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier
            .fillMaxWidth()
            .flaeche()
            .then(if (haupt) Modifier.flaechenmarke(wartet = true) else Modifier)
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (zeichen != null) Zeichenflaeche(zeichen, gross = true)
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = titel, style = Schrift.Gross, color = Farben.Text)
                if (text != null) {
                    Text(text = text, style = Schrift.Klein, color = Farben.TextLeise)
                }
            }
        }

        inhalt()

        // Die Knöpfe brechen um statt hinauszulaufen — am Handy stehen hier
        // regelmäßig zwei („Leitstelle besetzen", „Vorlagen"), und der zweite
        // passt neben dem ersten nicht mehr auf eine Handbreit.
        if (knoepfe != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                knoepfe()
            }
        }
    }
}
