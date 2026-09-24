package de.pagerspass.pagerspass.ui.schmuck

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Die Kopfmuster eines Profils — das Band hinter Wappen und Name.
 *
 * Übertragen aus `web/src/styles/kopfmuster.css`. Dort ist jedes Muster ein
 * Stapel aus `repeating-linear-gradient`, `radial-gradient` und
 * `repeating-conic-gradient`; Compose kennt davon nur den linearen und den
 * radialen Verlauf. Gezeichnet wird deshalb von Hand — mit denselben Winkeln,
 * denselben Abständen und denselben Deckkräften.
 *
 * <b>Das Band liegt hinter dem Inhalt, nicht über ihm.</b> Im Web ist das
 * `z-index: -1`; hier ist es `drawBehind`. Andersherum läge der Knopf rechts
 * unter einer Fläche, die nur Farbe ist.
 *
 * <b>Zwei Stärken, und der Unterschied ist gemessen.</b> Ein Kopf ist eine
 * Fläche zum *Ansehen*: Darauf steht ein Name in Weiß und fett, sonst nichts.
 * Eine Zeile ist eine Fläche zum *Lesen*: Darauf stehen ein Rang, eine Lage,
 * eine Uhrzeit — alles in den leisen Tönen. Gemessen über die ganze
 * Wappenpalette gegen die Kastenfläche:
 *
 *     volle Deckkraft   sehr leise 1,54:1   leise 2,38:1
 *     45 %              sehr leise 3,26:1   leise 5,04:1
 *
 * Der schlechteste Ton ist immer das Orange. Die 45 % sind die Stelle, an der
 * die *leise* Stufe wieder über 4,5 liegt; auf die sehr leise Stufe käme man
 * nur mit einem Viertel, und ein Viertel ist kein Band mehr, sondern ein Hauch.
 * <b>Wer eine Zeile mit Band baut, hebt darum die leiseste Schrift darauf eine
 * Stufe an</b> — das tut `Profilzeile`.
 *
 * Nebeneffekt und ausdrücklich gewollt: Zehn Zeilen mit vollem Muster
 * untereinander sind eine Tapete. Als Tönung sagt jede Zeile weiterhin, wem sie
 * gehört, ohne dass die Liste aufhört, eine Liste zu sein.
 */
fun Modifier.kopfband(
    muster: String,
    ton: Color,
    zeile: Boolean = false,
): Modifier = drawBehind {
    val staerke = if (zeile) 0.45f else 1f

    // <b>Alles bleibt im eigenen Rechteck.</b> Der schräge Verlauf zeichnet
    // eine dreifach überdimensionierte, gedrehte Fläche — ohne dieses Clipping
    // malt jede Vorschaukachel der Profilseite über den halben Bildschirm.
    // Genau so sah es aus: riesige Farbflächen über fremden Kacheln.
    clipRect {
        // Lage 1, immer: der Verlauf aus dem Wappenton. Er trägt das Band auch
        // dann, wenn gar kein Muster gewählt ist — „Glatt" ist kein leeres
        // Band, sondern dieser Verlauf allein.
        val grundstaerke = when (muster) {
            "keines" -> 0.40f
            "karbon" -> 0.72f
            "goldverlauf" -> 0.40f
            "flecktarn" -> 0.60f
            else -> 0.55f
        }
        schraegerVerlauf(ton.copy(alpha = grundstaerke * staerke), grad = 150f)

        // Lage 2: das Muster darüber.
        when (muster) {
            "streifen" -> streifen(staerke)
            "battenburg" -> battenburg(staerke)
            "karbon" -> karbon(staerke)
            "goldverlauf" -> goldverlauf(staerke)
            "warnschraffur" -> warnschraffur(staerke)
            "punktraster" -> punktraster(staerke)
            "wellen" -> wellen(staerke)
            "gitter" -> gitter(staerke)
            "sparren" -> sparren(staerke)
            "funkwellen" -> funkwellen(staerke)
            "nadelstreifen" -> nadelstreifen(staerke)
            "flecktarn" -> flecktarn(staerke)
            "blitz" -> blitz(staerke)
            "topografie" -> topografie(staerke)
            "signalband" -> signalband(staerke)
            "sternbild" -> sternbild(staerke)

            // Die Abo-Muster. Sie sind im Web zum Teil bewegt (Lichtband,
            // Sternenflug, Atemzug); hier stehen sie still. Bewegung auf einer
            // Listenzeile, die zehnmal untereinander steht, ist kein Schmuck
            // mehr, sondern Unruhe — und im Kopf allein wäre es ein zweites
            // Muster unter demselben Namen.
            "premium", "premium-lichtband" -> lichtband(staerke)
            "premium-wabe" -> wabe(staerke)
            "premium-schraffur" -> warnschraffur(staerke)
            "premium-sternenflug" -> sternbild(staerke)
            "premium-morgenschicht" -> morgenschicht(staerke)
            "premium-polarband" -> polarband(staerke)
            "premium-leuchtsuch" -> funkwellen(staerke)
            "premium-atemzug" -> lichtband(staerke)

            // „Eigene Lage" zeichnet im Web echte Kartenkacheln hinter den Kopf.
            // Die App lädt keine Kacheln — sie zeigt den Verlauf allein, statt
            // ein Muster zu erfinden, das es nicht gibt.
            "premium-lagekarte", "premium-karte" -> Unit

            // „keines" und alles Unbekannte: nur der Verlauf. Ein neues Muster
            // des Servers erscheint damit schlicht ohne Zeichnung — nicht als
            // Fehler und nicht als geratenes Bild.
            else -> Unit
        }
    }
}

// -------------------------------------------------------------- Die Zeichnung
//
// Jede Zeichnung entspricht einer CSS-Regel. Die Zahlen sind die des Webs; wo
// eine steht, die dort nicht steht, ist sie in der Umrechnung entstanden und
// steht als Kommentar dabei.

/** `linear-gradient(<grad>, ton, transparent 62%)` — der Grundverlauf. */
private fun DrawScope.schraegerVerlauf(farbe: Color, grad: Float) {
    // CSS zählt den Winkel von „nach oben" im Uhrzeigersinn; Compose braucht
    // Anfangs- und Endpunkt. Bei 150° läuft der Verlauf von links oben nach
    // rechts unten — die Diagonale reicht als Länge in jedem Fall.
    val laenge = hypot(size.width, size.height)
    rotate(degrees = grad - 90f, pivot = Offset.Zero) {
        drawRect(
            brush = Brush.linearGradient(
                0f to farbe,
                0.62f to Color.Transparent,
                start = Offset.Zero,
                end = Offset(laenge, 0f),
            ),
            topLeft = Offset(-laenge, -laenge),
            size = Size(laenge * 3, laenge * 3),
        )
    }
}

/**
 * Schräge Streifen in einem Winkel — der Bauplan hinter der Hälfte aller Muster.
 *
 * Gezeichnet wird über eine gedrehte Fläche, die groß genug ist, dass ihre Ecken
 * auch nach der Drehung noch außerhalb liegen. Ohne diesen Zuschlag bleiben nach
 * der Drehung zwei unbemalte Dreiecke stehen.
 */
private fun DrawScope.schraegstreifen(
    grad: Float,
    farbe: Color,
    breite: Float,
    abstand: Float,
    versatz: Float = 0f,
) {
    val weite = hypot(size.width, size.height)
    rotate(grad) {
        var x = -weite + versatz
        while (x < weite) {
            drawRect(
                color = farbe,
                topLeft = Offset(x, -weite),
                size = Size(breite, weite * 3),
            )
            x += abstand
        }
    }
}

private fun DrawScope.streifen(s: Float) =
    schraegstreifen(135f, Color.White.copy(alpha = 0.13f * s), 10.dp.toPx(), 26.dp.toPx())

private fun DrawScope.nadelstreifen(s: Float) {
    val abstand = 10.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawRect(Color.White.copy(alpha = 0.13f * s), Offset(x, 0f), Size(1.dp.toPx(), size.height))
        x += abstand
    }
}

private fun DrawScope.karbon(s: Float) {
    val breite = 3.dp.toPx()
    schraegstreifen(45f, Color.Black.copy(alpha = 0.32f * s), breite, breite * 2)
    schraegstreifen(-45f, Color.White.copy(alpha = 0.07f * s), breite, breite * 2)
}

/**
 * Warnschraffur — rot und weiß, lückenlos abwechselnd.
 *
 * Zwei Reihen desselben Rasters, die zweite um genau eine Streifenbreite
 * versetzt: So schließen sie aneinander an, statt eine Lücke zu lassen. Im Web
 * macht das ein einziger `repeating-linear-gradient` mit zwei Farbstopps.
 */
private fun DrawScope.warnschraffur(s: Float) {
    val streifen = 17.dp.toPx()
    schraegstreifen(45f, Color(0xFFE03C3C).copy(alpha = 0.17f * s), streifen, streifen * 2)
    schraegstreifen(
        grad = 45f,
        farbe = Color.White.copy(alpha = 0.07f * s),
        breite = streifen,
        abstand = streifen * 2,
        versatz = streifen,
    )
}

/** Signalband — Amber, eine helle Fuge, dann eine dunkle Bahn. */
private fun DrawScope.signalband(s: Float) {
    val takt = 28.dp.toPx()
    val amber = 13.dp.toPx()
    val fuge = 2.dp.toPx()
    schraegstreifen(45f, Color(0xFFFFB020).copy(alpha = 0.20f * s), amber, takt)
    schraegstreifen(45f, Color.White.copy(alpha = 0.08f * s), fuge, takt, versatz = amber)
    schraegstreifen(
        grad = 45f,
        farbe = Color(0xFF0A0C10).copy(alpha = 0.26f * s),
        breite = takt - amber - fuge,
        abstand = takt,
        versatz = amber + fuge,
    )
}

private fun DrawScope.goldverlauf(s: Float) {
    val laenge = hypot(size.width, size.height)
    rotate(degrees = 120f - 90f, pivot = Offset.Zero) {
        drawRect(
            brush = Brush.linearGradient(
                0f to Color(0xFFE0B84F).copy(alpha = 0.42f * s),
                0.55f to Color(0xFFA8861D).copy(alpha = 0.10f * s),
                0.75f to Color.Transparent,
                start = Offset.Zero,
                end = Offset(laenge, 0f),
            ),
            topLeft = Offset(-laenge, -laenge),
            size = Size(laenge * 3, laenge * 3),
        )
    }
}

private fun DrawScope.lichtband(s: Float) {
    val laenge = hypot(size.width, size.height)
    rotate(degrees = 105f - 90f, pivot = Offset.Zero) {
        drawRect(
            brush = Brush.linearGradient(
                0.30f to Color.Transparent,
                0.50f to Color.White.copy(alpha = 0.20f * s),
                0.70f to Color.Transparent,
                start = Offset.Zero,
                end = Offset(laenge, 0f),
            ),
            topLeft = Offset(-laenge, -laenge),
            size = Size(laenge * 3, laenge * 3),
        )
    }
}

private fun DrawScope.punktraster(s: Float) {
    val gitter = 18.dp.toPx()
    val punkt = 2.dp.toPx()
    var y = gitter / 2
    while (y < size.height) {
        var x = gitter / 2
        while (x < size.width) {
            drawCircle(Color.White.copy(alpha = 0.14f * s), punkt, Offset(x, y))
            x += gitter
        }
        y += gitter
    }
}

/**
 * Sterne — hell, unterschiedlich groß, in fester Streuung.
 *
 * <b>Fest gestreut und nicht gewürfelt.</b> Im Web stehen sie als sechzehn
 * einzelne `radial-gradient`-Stellen; ein Zufallsgenerator hier ergäbe bei jedem
 * Neuzeichnen einen anderen Himmel, und eine Liste, deren Muster beim Rollen
 * flimmert, ist keine Zierde.
 */
private fun DrawScope.sternbild(s: Float) {
    val sterne = listOf(
        Triple(0.14f, 0.26f, 1.8f), Triple(0.27f, 0.58f, 1.1f),
        Triple(0.41f, 0.18f, 1.5f), Triple(0.55f, 0.71f, 1.0f),
        Triple(0.63f, 0.34f, 1.9f), Triple(0.76f, 0.62f, 1.2f),
        Triple(0.88f, 0.22f, 1.4f), Triple(0.33f, 0.86f, 1.0f),
        Triple(0.07f, 0.66f, 1.3f), Triple(0.94f, 0.79f, 1.1f),
        Triple(0.49f, 0.44f, 0.9f), Triple(0.70f, 0.09f, 1.2f),
    )
    sterne.forEach { (fx, fy, r) ->
        drawCircle(
            color = Color.White.copy(alpha = 0.75f * s),
            radius = r * density,
            center = Offset(size.width * fx, size.height * fy),
        )
    }
}

private fun DrawScope.gitter(s: Float) {
    val fein = 20.dp.toPx()
    val grob = 100.dp.toPx()
    val strich = 1.dp.toPx()

    fun linien(abstand: Float, deckung: Float) {
        val farbe = Color.White.copy(alpha = deckung * s)
        var y = 0f
        while (y < size.height) {
            drawRect(farbe, Offset(0f, y), Size(size.width, strich)); y += abstand
        }
        var x = 0f
        while (x < size.width) {
            drawRect(farbe, Offset(x, 0f), Size(strich, size.height)); x += abstand
        }
    }

    linien(fein, 0.09f)
    linien(grob, 0.16f)
}

/**
 * Konzentrische Kreise um einen Punkt — Funkwellen, Wellen, Topografie.
 *
 * @param breitung Wie stark die Ringe waagerecht gedehnt werden. Im Web sind
 *   das die Ellipsen der `repeating-radial-gradient` („ellipse 120% 70%"); hier
 *   ist es eine Skalierung um die Mitte, damit die Ringe nicht wandern.
 */
private fun DrawScope.ringe(
    mitte: Offset,
    abstand: Float,
    strich: Float,
    farbe: Color,
    breitung: Float = 1f,
) {
    val weite = hypot(size.width, size.height) * 1.5f
    withTransform({ scale(breitung, 1f, mitte) }) {
        var r = abstand
        while (r < weite) {
            drawCircle(color = farbe, radius = r, center = mitte, style = Stroke(strich))
            r += abstand
        }
    }
}

private fun DrawScope.funkwellen(s: Float) = ringe(
    mitte = Offset(size.width * 0.08f, size.height * 0.5f),
    abstand = 17.dp.toPx(),
    strich = 3.dp.toPx(),
    farbe = Color.White.copy(alpha = 0.13f * s),
)

private fun DrawScope.wellen(s: Float) = ringe(
    mitte = Offset(size.width * 0.22f, size.height * 1.68f),
    abstand = 22.dp.toPx(),
    strich = 4.dp.toPx(),
    farbe = Color.White.copy(alpha = 0.16f * s),
    breitung = 1.6f,
)

private fun DrawScope.topografie(s: Float) = ringe(
    mitte = Offset(size.width * 0.35f, size.height * 0.6f),
    abstand = 13.dp.toPx(),
    strich = 2.dp.toPx(),
    farbe = Color.White.copy(alpha = 0.11f * s),
    breitung = 1.7f,
)

private fun DrawScope.polarband(s: Float) {
    val laenge = hypot(size.width, size.height)
    rotate(degrees = 20f, pivot = Offset.Zero) {
        drawRect(
            brush = Brush.linearGradient(
                0.20f to Color.Transparent,
                0.45f to Color(0xFF6EDCD6).copy(alpha = 0.22f * s),
                0.60f to Color(0xFFB197FC).copy(alpha = 0.18f * s),
                0.85f to Color.Transparent,
                start = Offset.Zero,
                end = Offset(laenge, 0f),
            ),
            topLeft = Offset(-laenge, -laenge),
            size = Size(laenge * 3, laenge * 3),
        )
    }
}

private fun DrawScope.morgenschicht(s: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFFFB020).copy(alpha = 0.22f * s),
            0.45f to Color(0xFFFF8A3D).copy(alpha = 0.10f * s),
            1f to Color.Transparent,
        ),
    )
}

/** Waben — sechseckig, versetzt gesetzt. */
private fun DrawScope.wabe(s: Float) {
    val r = 16.dp.toPx()
    val farbe = Color.White.copy(alpha = 0.10f * s)
    val strich = 1.5f * density
    val hoehe = r * 1.5f
    val breite = r * 1.732f

    var reihe = 0
    var y = 0f
    while (y < size.height + r) {
        var x = if (reihe % 2 == 0) 0f else breite / 2
        while (x < size.width + breite) {
            sechseck(Offset(x, y), r, farbe, strich)
            x += breite
        }
        y += hoehe
        reihe++
    }
}

private fun DrawScope.sechseck(mitte: Offset, r: Float, farbe: Color, strich: Float) {
    val pfad = androidx.compose.ui.graphics.Path()
    for (i in 0 until 6) {
        val winkel = Math.toRadians((60.0 * i) - 30.0)
        val punkt = Offset(
            mitte.x + r * kotlin.math.cos(winkel).toFloat(),
            mitte.y + r * kotlin.math.sin(winkel).toFloat(),
        )
        if (i == 0) pfad.moveTo(punkt.x, punkt.y) else pfad.lineTo(punkt.x, punkt.y)
    }
    pfad.close()
    drawPath(pfad, farbe, style = Stroke(strich))
}

/** Battenburg — das gelb-grüne Schachbrett der Einsatzfahrzeuge. */
private fun DrawScope.battenburg(s: Float) {
    val kante = 28.dp.toPx()
    val gelb = Color(0xFFFFDC00).copy(alpha = 0.16f * s)
    val gruen = Color(0xFF46C85A).copy(alpha = 0.14f * s)

    var reihe = 0
    var y = 0f
    while (y < size.height) {
        var spalte = 0
        var x = 0f
        while (x < size.width) {
            drawRect(
                color = if ((reihe + spalte) % 2 == 0) gelb else gruen,
                topLeft = Offset(x, y),
                size = Size(kante, kante),
            )
            x += kante
            spalte++
        }
        y += kante
        reihe++
    }
}

/** Sparren — die Winkel, die hinten auf jedem Einsatzfahrzeug stehen. */
private fun DrawScope.sparren(s: Float) {
    val kante = 34.dp.toPx()
    val farbe = Color.White.copy(alpha = 0.14f * s)
    val strich = 3f * density

    var y = -kante
    while (y < size.height + kante) {
        var x = -kante
        while (x < size.width + kante) {
            val pfad = androidx.compose.ui.graphics.Path().apply {
                moveTo(x, y + kante / 2)
                lineTo(x + kante / 2, y)
                lineTo(x + kante, y + kante / 2)
            }
            drawPath(pfad, farbe, style = Stroke(strich))
            x += kante
        }
        y += kante / 2
    }
}

/** Flecktarn — vier weiche Flecken in fester Streuung. */
private fun DrawScope.flecktarn(s: Float) {
    val flecken = listOf(
        Triple(Offset(0.24f, 0.30f), 0.34f, Color.Black.copy(alpha = 0.28f * s)),
        Triple(Offset(0.62f, 0.66f), 0.28f, Color.Black.copy(alpha = 0.20f * s)),
        Triple(Offset(0.44f, 0.18f), 0.22f, Color.White.copy(alpha = 0.11f * s)),
        Triple(Offset(0.82f, 0.40f), 0.26f, Color.White.copy(alpha = 0.07f * s)),
        Triple(Offset(0.08f, 0.78f), 0.20f, Color.Black.copy(alpha = 0.18f * s)),
    )
    val bezug = hypot(size.width, size.height)

    flecken.forEach { (ort, anteil, farbe) ->
        val mitte = Offset(size.width * ort.x, size.height * ort.y)
        val r = bezug * anteil
        drawCircle(
            brush = Brush.radialGradient(
                0f to farbe,
                0.6f to farbe,
                1f to Color.Transparent,
                center = mitte,
                radius = r,
            ),
            radius = r,
            center = mitte,
        )
    }
}

/** Zwei helle Blitze, oben und unten, gegenläufig. */
private fun DrawScope.blitz(s: Float) {
    fun strahl(grad: Float, oben: Boolean) {
        val laenge = hypot(size.width, size.height)
        val hoehe = size.height * 0.52f
        clipRect(
            top = if (oben) 0f else size.height - hoehe,
            bottom = if (oben) hoehe else size.height,
        ) {
            rotate(degrees = grad - 90f, pivot = Offset(size.width / 2, size.height / 2)) {
                drawRect(
                    brush = Brush.linearGradient(
                        0.46f to Color.Transparent,
                        0.494f to Color(0xFFFFFCE8).copy(alpha = 0.70f * s),
                        0.514f to Color(0xFFFFFCE8).copy(alpha = 0.70f * s),
                        0.55f to Color.Transparent,
                        start = Offset.Zero,
                        end = Offset(laenge, 0f),
                    ),
                    topLeft = Offset(-laenge, -laenge),
                    size = Size(laenge * 3, laenge * 3),
                )
            }
        }
    }

    strahl(104f, oben = true)
    strahl(76f, oben = false)
}
