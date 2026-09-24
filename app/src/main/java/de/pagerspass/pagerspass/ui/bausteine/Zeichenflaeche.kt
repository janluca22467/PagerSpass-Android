package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung

/**
 * Ein Zeichen in einer getönten Fläche — der Kopf einer Karte, der Anfang einer
 * Wegzeile, der Merkposten neben einer Überschrift.
 *
 * <b>Warum es das gibt.</b> Ein Strichzeichen allein neben einer Überschrift ist
 * ein Strich neben Text und verschwindet. In einer Fläche wird es ein
 * Gegenstand, und eine Seite aus fünf solchen Gegenständen liest sich als Liste
 * von Dingen statt als Liste von Zeilen.
 *
 * <b>Die Maße sind Formen, keine Zielflächen.</b> Gedrückt wird die Zeile darum,
 * nicht das Bild darin. Dass 36 und 44 dieselben Zahlen tragen wie die
 * Zielflächenstaffel, ist Zufall der Anschauung — sie sollen neben einem Knopf
 * derselben Reihe nicht aus dem Takt fallen.
 *
 * @param gross Die Fassung für den Kopf einer Karte, in der noch ein Formular
 *   steht: größer, mit amberfarbenem Rand und getönter Fläche.
 */
@Composable
fun Zeichenflaeche(
    zeichen: ImageVector,
    modifier: Modifier = Modifier,
    gross: Boolean = false,
    farbe: Color = Farben.Amber,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(if (gross) 44.dp else 36.dp)
            .background(
                color = if (gross) Farben.HauchAmber else Farben.FlaecheHoch,
                shape = Rundung.Klein,
            )
            .border(
                width = 1.dp,
                color = if (gross) Farben.AmberTief else Farben.RandHell,
                shape = Rundung.Klein,
            ),
    ) {
        Icon(
            imageVector = zeichen,
            contentDescription = null,
            tint = farbe,
            modifier = Modifier.size(if (gross) 24.dp else 22.dp),
        )
    }
}
