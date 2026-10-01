package de.pagerspass.pagerspass.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
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
 * Der Seitengrund — eine ruhige, dunkle Fläche.
 *
 * <b>Seit dem 30.09.2026 ohne Raster und Lichthof.</b> Das Handy spricht im Web
 * die Formensprache des Einsatz-Tablets (`mobil.css`, „Das Handy im
 * Tabletlook"): Die Seite ist eine Farbe, nichts leuchtet dahinter. Das Raster
 * und der warme Schein von oben fielen dort weg, und hier mit ihnen — die App
 * ist ein Handy. Kein Farbwert ändert sich, nur wo er steht.
 */
fun Modifier.seitengrund(): Modifier = background(Farben.Bg)

/**
 * Der Kopfverlauf — der schräge Verlauf des Tablet-Kopfs.
 *
 * `linear-gradient(115deg, var(--flaeche-hoch), var(--flaeche))`: Im Web tragen
 * ihn die Dienstleiste, der Leitstellen- und der Fahrzeugkopf, die Köpfe von
 * Dienstbuch und Freunden und der Shop-Eingang. 115 Grad heißt: von links oben
 * nach rechts unten, etwas flacher als die Diagonale.
 */
fun Modifier.kopfverlauf(form: Shape = RectangleShape): Modifier = drawBehind {
    val outline = form.createOutline(size, layoutDirection, this)
    drawOutline(
        outline = outline,
        brush = Brush.linearGradient(
            colors = listOf(Farben.FlaecheHoch, Farben.Flaeche),
            start = Offset.Zero,
            end = Offset(size.width, size.width * 0.47f),
        ),
    )
}

/**
 * Die Kapsel — eine eingelassene, runde Fläche für Suche, Eingabe und Reiter.
 *
 * Im Web `rgb(0 0 0 / 28%)` mit einem Haarrand aus sechs Prozent Weiß, seit dem
 * Umbau der Fahrzeugliste (01.10.2026) überall dort, wo vorher ein kantiges Feld
 * oder ein Kasten stand: die Suche im Tableau, die Funkeingabe, der Umschalter
 * Funkverkehr/Handfunkgerät.
 *
 * @param an Hervorgehoben — Amberhauch und Amberrand, wie der gedrückte
 *   Dicht-Schalter.
 */
fun Modifier.kapsel(form: Shape = RoundedCornerShape(999.dp), an: Boolean = false): Modifier =
    this
        .background(if (an) Farben.HauchAmber else Color.Black.copy(alpha = 0.28f), form)
        .border(
            1.dp,
            if (an) Farben.Amber.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f),
            form,
        )

/**
 * Die Leuchtleiste — eine farbige Innenkante links, die nach innen ausläuft.
 *
 * Die Bildsprache der modernen Leitstelle (01.10.2026): Einsatzkarten tragen die
 * Farbe ihres Zustands, Funknachrichten die ihrer Art, die Fahrzeugkarte die
 * ihrer Organisation. Drei Punkte voller Farbe an der Kante, dahinter ein Hauch
 * derselben Farbe, der nach rechts verschwindet — der „Schein".
 *
 * Sie wird *über* den Hintergrund und unter den Inhalt gezeichnet und respektiert
 * die Rundung der Fläche: Die Kante folgt der Ecke, statt über sie zu ragen.
 *
 * @param schein Wie weit der Hauch hineinreicht, als Anteil der Breite. Null
 *   schaltet ihn ab, dann bleibt nur die Kante.
 * @param glimmt Für das, was dringend ist (Alarm, Priorität 1) — der Hauch wird
 *   kräftiger.
 */
fun Modifier.leuchtleiste(
    farbe: Color,
    ecke: Dp = 14.dp,
    schein: Float = 0.45f,
    glimmt: Boolean = false,
): Modifier = drawWithContent {
    val form = RoundedCornerShape(ecke).createOutline(size, layoutDirection, this)
    val pfad = Path().apply { addOutline(form) }
    clipPath(pfad) {
        if (schein > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(farbe.copy(alpha = if (glimmt) 0.20f else 0.10f), Color.Transparent),
                    startX = 0f,
                    endX = size.width * schein,
                ),
            )
        }
        drawRect(color = farbe, size = size.copy(width = 3.dp.toPx()))
    }
    drawContent()
}

/**
 * Der Schein — ein weicher Lichthof in einer Farbe um eine Form.
 *
 * Das Gegenstück zu `box-shadow: 0 6px 16px color-mix(… 40%, transparent)`:
 * Auf dunklem Grund trägt ein farbiger Schatten die Höhe, ein schwarzer ginge
 * unter. Gezeichnet als einige weiche, nach außen schwächer werdende Lagen —
 * ohne Unschärfefilter, damit es auch auf älteren Geräten nichts kostet.
 */
fun Modifier.schein(farbe: Color, ecke: Dp, weite: Dp = 8.dp, staerke: Float = 0.40f, versatz: Dp = 3.dp): Modifier =
    drawBehind {
        val w = weite.toPx()
        val dy = versatz.toPx()
        val schritte = 5
        for (i in schritte downTo 1) {
            val aussen = w * i / schritte
            drawRoundRect(
                color = farbe.copy(alpha = staerke / (schritte * 1.6f)),
                topLeft = Offset(-aussen, -aussen + dy),
                size = Size(size.width + aussen * 2, size.height + aussen * 2),
                cornerRadius = CornerRadius(ecke.toPx() + aussen),
            )
        }
    }

/**
 * Das Statusquadrat — die FMS-Farbe als abgerundetes Quadrat, wie ein App-Symbol.
 *
 * Gewünscht am 01.10.2026 („diese Kreise mit dem Status"), gewählt wurde das
 * abgerundete Quadrat. Gefüllt mit einem Verlauf von der aufgehellten Farbe oben
 * links zur vollen Farbe, eine Lichtkante oben, eine Schattenkante unten und ein
 * Schein in der Statusfarbe darunter.
 */
fun Modifier.statusquadrat(farbe: Color, ecke: Dp): Modifier {
    val form = RoundedCornerShape(ecke)
    val hell = lerp(farbe, Color.White, 0.22f)
    return this
        .schein(farbe, ecke, weite = 6.dp, staerke = 0.45f)
        .background(
            brush = Brush.linearGradient(listOf(hell, farbe, farbe)),
            shape = form,
        )
        .drawWithContent {
            val outline = form.createOutline(size, layoutDirection, this)
            clipPath(Path().apply { addOutline(outline) }) {
                val oben = 1.dp.toPx()
                val unten = 2.dp.toPx()
                drawRect(Color.White.copy(alpha = 0.35f), size = Size(size.width, oben))
                drawRect(
                    Color.Black.copy(alpha = 0.18f),
                    topLeft = Offset(0f, size.height - unten),
                    size = Size(size.width, unten),
                )
            }
            drawContent()
        }
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
