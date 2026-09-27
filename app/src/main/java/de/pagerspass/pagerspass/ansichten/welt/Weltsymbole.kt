package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Farben

/*
 * Die festen Zeichen der Weltkarte — Wache, Leitstelle, eigener Punkt. Pfaddaten
 * zeichengleich aus `utils/fahrzeugIcons.ts` (`wacheSvg`, `leitstelleSvg`, `objektSvg`).
 *
 * Gezeichnet wird im Raster der Vorlage (28 bzw. 24 Einheiten) und auf die Größe des Webs
 * skaliert: Die Wache ist 24 Punkte groß, die Leitstelle 26, der eigene Punkt 18.
 */

private val pfade = HashMap<String, Path>()

/** Ein SVG-Pfad als Compose-Pfad — einmal zerlegt und gemerkt. */
private fun svg(d: String): Path = pfade.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/**
 * Zeichnet im Raster einer Vorlage: `raster` Einheiten werden zu `groesse` Punkten, mittig
 * auf `ort`.
 */
private inline fun DrawScope.imRaster(
    ort: Offset,
    raster: Float,
    groesse: Float,
    crossinline zeichnen: DrawScope.() -> Unit,
) {
    val faktor = groesse.dp.toPx() / raster
    translate(ort.x - raster * faktor / 2f, ort.y - raster * faktor / 2f) {
        scale(faktor, faktor, pivot = Offset.Zero) {
            zeichnen()
        }
    }
}

/**
 * Das Wachenhaus: Dach, Körper und im Körper das Tor — oder der Schraubenschlüssel der
 * Werkstatt, das Buch der Lehrgangseinrichtung.
 */
fun DrawScope.wachensymbol(ort: Offset, farbe: Color, zeichen: String, deckkraft: Float = 1f) {
    imRaster(ort, 28f, 24f) {
        // Ein dunkler Saum — auf dem Luftbild steht das Haus sonst nicht ab.
        drawPath(svg("M14 2 26 13 2 13Z"), Farben.BgTief.copy(alpha = 0.6f * deckkraft), style = Stroke(2f))
        drawRoundRect(
            Farben.BgTief.copy(alpha = 0.6f * deckkraft),
            topLeft = Offset(5f, 12f),
            size = Size(18f, 13f),
            cornerRadius = CornerRadius(1.5f, 1.5f),
            style = Stroke(2f),
        )
        drawPath(svg("M14 2 26 13 2 13Z"), farbe.copy(alpha = deckkraft))
        drawRoundRect(
            farbe.copy(alpha = deckkraft),
            topLeft = Offset(5f, 12f),
            size = Size(18f, 13f),
            cornerRadius = CornerRadius(1.5f, 1.5f),
        )
        val tor = Farben.BgTief.copy(alpha = deckkraft)
        when (zeichen) {
            "werkstatt" -> drawPath(
                svg(
                    "M19.6 14.6a3.1 3.1 0 0 0-4.1-3.9l1.9 1.9-1.8 1.8-1.9-1.9a3.1 3.1 0 0 0 3.9 " +
                        "4.1l-4.4 4.4 1.9 1.9 4.5-4.3z",
                ),
                tor,
            )
            "lehrgang" -> drawPath(
                svg("M13.3 15.6c-1.6-.8-3.3-.8-5 0v7c1.7-.8 3.4-.8 5 0zM14.7 15.6c1.6-.8 3.3-.8 5 0v7c-1.7-.8-3.4-.8-5 0z"),
                tor,
            )
            else -> drawRect(tor, topLeft = Offset(11.5f, 17f), size = Size(5f, 8f))
        }
    }
}

/**
 * Die Leitstelle: ein Mast mit Querband und zwei Wellen — hier steht kein Fahrzeug,
 * deshalb nicht das Wachenhaus.
 */
fun DrawScope.leitstellensymbol(ort: Offset, farbe: Color, deckkraft: Float = 1f, groesse: Float = 26f) {
    imRaster(ort, 28f, groesse) {
        val f = farbe.copy(alpha = deckkraft)
        drawPath(svg("M11 24 L13.2 9 h1.6 L17 24 h-2.2 l-.8-6h-.0 l-.8 6z"), f)
        drawRoundRect(f, topLeft = Offset(11.6f, 16f), size = Size(4.8f, 1.6f), cornerRadius = CornerRadius(0.6f, 0.6f))
        val welle = Stroke(width = 2f, cap = StrokeCap.Round)
        drawPath(svg("M9.4 4.2a7.5 7.5 0 0 0 0 8.6"), f.copy(alpha = f.alpha * 0.75f), style = welle)
        drawPath(svg("M18.6 4.2a7.5 7.5 0 0 1 0 8.6"), f.copy(alpha = f.alpha * 0.75f), style = welle)
        drawCircle(f, radius = 2.1f, center = Offset(14f, 8.5f))
    }
}

/**
 * Der eigene Punkt — dieselbe Form wie das Sonderobjekt der Lagekarte: ein Haus mit
 * Menschen darin. Stillgelegt steht er blass da.
 */
fun DrawScope.objektsymbol(ort: Offset, ton: Color, aktiv: Boolean = true) {
    val deckkraft = if (aktiv) 1f else 0.45f
    imRaster(ort, 24f, 18f) {
        val grund = Color(0xE00A0D12).copy(alpha = 0.88f * deckkraft)
        drawRoundRect(grund, topLeft = Offset(2f, 2f), size = Size(20f, 20f), cornerRadius = CornerRadius(3f, 3f))
        drawRoundRect(
            ton.copy(alpha = deckkraft),
            topLeft = Offset(2f, 2f),
            size = Size(20f, 20f),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(1.4f),
        )
        drawPath(svg("M12 5.5 19 11h-2.2v7.5H7.2V11H5z"), ton.copy(alpha = deckkraft))
        drawCircle(grund, radius = 1.5f, center = Offset(12f, 13.4f))
    }
}

/** Das Sammelzeichen des Bereitstellungsraums: drei Marken um einen Punkt. */
fun DrawScope.raumzeichen(ort: Offset, farbe: Color) {
    imRaster(ort, 24f, 18f) {
        val strich = Stroke(width = 1.8f, cap = StrokeCap.Round)
        drawCircle(farbe, radius = 2.5f, center = Offset(12f, 12f), style = strich)
        drawPath(svg("M12 3v3.5M12 17.5V21M3 12h3.5M17.5 12H21"), farbe, style = strich)
    }
}
