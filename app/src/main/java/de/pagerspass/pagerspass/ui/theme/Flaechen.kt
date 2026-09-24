package de.pagerspass.pagerspass.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Die Flächen — was einen Kasten zu einem Gegenstand macht.
 *
 * Im Web ist das eine einzige CSS-Regel (`.flaeche`) und sie stand hinter 127
 * Fundstellen. Hier ist es ein Modifikator, und der Grund ist derselbe: Wer die
 * drei Zeilen an jeder Stelle neu schreibt, hat beim vierten Mal einen anderen
 * Rand.
 */

/**
 * Die Lichtkante.
 *
 * <b>Warum eine erhobene Fläche auf dunklem Grund zwei Teile hat.</b> Auf hellem
 * Grund genügt der Schatten. Auf dunklem verschluckt der Grund ihn — ein
 * schwarzer Schatten auf #080b10 ist fast nichts. Was die Höhe hier trägt, ist
 * die Kante, die dem Licht zugewandt ist: ein Punkt Höhe, acht Prozent Weiß, an
 * der Oberkante.
 *
 * Sie kostet weder Platz noch verschiebt sie etwas — im Web ist sie ein
 * `inset`-Schatten, hier wird sie über den Inhalt gezeichnet. Deshalb steht sie
 * als eigener Modifikator und nicht in jeder Erhebungsstufe mit drin: Nicht
 * jede erhobene Fläche will sie (ein Balken schon, ein Menü nicht).
 *
 * @param ecke Der Eckradius der Fläche darunter. Die Kante wird um ihn
 *   eingerückt, damit sie nicht über die Rundung hinausläuft — eine Linie, die
 *   in der Ecke die Kurve schneidet, sieht aus wie ein Zeichenfehler.
 */
fun Modifier.lichtkante(ecke: Dp = 14.dp): Modifier = drawWithContent {
    drawContent()
    val hoehe = 1.dp.toPx()
    val einzug = ecke.toPx()
    if (size.width <= einzug * 2) return@drawWithContent
    drawLine(
        color = Farben.HauchHell,
        start = Offset(einzug, hoehe / 2f),
        end = Offset(size.width - einzug, hoehe / 2f),
        strokeWidth = hoehe,
    )
}

/**
 * Der Kasten — Fläche, Rand, Rundung, Lichtkante.
 *
 * Das Gegenstück zu `.flaeche` im Web. Wer einen Kasten braucht, nimmt diesen
 * Modifikator; wer nur eine Farbe braucht, nimmt `background`.
 *
 * @param farbe Die Füllung. Vorgabe ist die Kastenfläche; eine Zeile *innerhalb*
 *   eines Kastens nimmt `FlaecheHoch`, damit sie sich abhebt.
 * @param randfarbe Der Rahmen.
 * @param ecke Die Rundung. Vorgabe ist die Kastenstufe (14).
 * @param mitLichtkante Ob die Oberkante das Licht bekommt. Aus, wenn der Kasten
 *   nicht erhoben liegt, sondern in eine Fläche eingelassen ist.
 */
fun Modifier.flaeche(
    farbe: Color = Farben.Flaeche,
    randfarbe: Color = Farben.Rand,
    ecke: Dp = 14.dp,
    mitLichtkante: Boolean = true,
): Modifier {
    val form: Shape = RoundedCornerShape(ecke)
    return this
        .background(farbe, form)
        .border(1.dp, randfarbe, form)
        .let { if (mitLichtkante) it.lichtkante(ecke) else it }
}

/**
 * Die Kante für „das hier gehört dir" — Dienstausweis, Einladung, Einweisung.
 *
 * Drei Punkte Amber an der linken Seite. Sie ordnet den Kasten ein, bevor man
 * ihn liest.
 *
 * <b>Sparsam einsetzen.</b> Wenn jeder zweite Kasten einer Seite die Kante
 * trägt, sagt sie nichts mehr; im Web stand sie in drei Ansichten je einmal, und
 * genau das ist die Dosis.
 *
 * @param wartet Ob der Kasten auf eine Antwort wartet — dann volles Amber statt
 *   des abgedunkelten.
 */
fun Modifier.flaechenmarke(wartet: Boolean = false): Modifier = drawWithContent {
    drawContent()
    drawRect(
        color = if (wartet) Farben.Amber else Farben.AmberTief,
        size = size.copy(width = 3.dp.toPx()),
    )
}

/**
 * Das Raster des Seitengrundes — zwei Haarlinien im Abstand einer Handbreit.
 *
 * Es liegt unter der Rollfläche jeder Seite. Die Deckkraft ist aus `HauchHell`
 * gemischt statt geraten: gut zweieinhalb Prozent Weiß — so wenig, dass es erst
 * auffällt, wenn man danach sucht, und zu wenig, um unter Text etwas auszumachen.
 *
 * Es steht als Modifikator und nicht als Bild, weil zwei Stellen es brauchen und
 * sie zueinander fluchten müssen: die Rollfläche einer Seite und die klebenden
 * Köpfe darin. Beide zeichnen vom selben Ursprung.
 */
fun Modifier.raster(mass: Dp = 72.dp): Modifier = drawWithContent {
    val farbe = Farben.HauchHell.copy(alpha = Farben.HauchHell.alpha * 0.32f)
    val schritt = mass.toPx()
    val strich = 1.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawLine(farbe, Offset(0f, y), Offset(size.width, y), strich)
        y += schritt
    }
    var x = 0f
    while (x < size.width) {
        drawLine(farbe, Offset(x, 0f), Offset(x, size.height), strich)
        x += schritt
    }
    drawContent()
}

/**
 * Die Gegenrichtung: was *in* die Fläche eingelassen ist, statt auf ihr zu liegen.
 *
 * Ein Feld, das Bett eines Balkens, eine Rille. Im Web sind das zwei
 * `inset`-Schatten; hier ein Verlauf von oben, der die Kante nach innen zieht.
 *
 * @param tief Für das, was wirklich Tiefe hat: Display, Schacht.
 */
fun Modifier.eingelassen(tief: Boolean = false): Modifier = drawWithContent {
    drawContent()
    val staerke = if (tief) 0.55f else 0.45f
    val hoehe = (if (tief) 6.dp else 3.dp).toPx()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = staerke), Color.Transparent),
            startY = 0f,
            endY = hoehe,
        ),
        size = size.copy(height = hoehe),
    )
}
