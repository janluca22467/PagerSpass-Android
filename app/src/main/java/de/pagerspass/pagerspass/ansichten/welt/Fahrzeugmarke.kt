package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.WeltPackicon
import de.pagerspass.pagerspass.ui.fahrzeug.Bauplan
import de.pagerspass.pagerspass.ui.fahrzeug.Blitzmuster
import de.pagerspass.pagerspass.ui.fahrzeug.Form
import de.pagerspass.pagerspass.ui.fahrzeug.Lackfarbe
import de.pagerspass.pagerspass.ui.fahrzeug.bauplanFuer
import de.pagerspass.pagerspass.ui.fahrzeug.blitzmusterVon
import de.pagerspass.pagerspass.ui.fahrzeug.blitzphase
import de.pagerspass.pagerspass.ui.fahrzeug.fahrzeughofZeichnen
import de.pagerspass.pagerspass.ui.fahrzeug.fahrzeugZeichnen
import kotlin.math.roundToInt

/*
 * Die Fahrzeugmarke der Weltkarte — Riss von oben, gedreht in Fahrtrichtung, mit
 * Blaulicht; oder das Bild aus dem eigenen Icon-Pack mit seinen Blaulichtpunkten
 * (`fahrzeugMarkup` in `components/welt/iconpack.ts`).
 *
 * Der Riss selbst ist die volle Zeichnung aus `ui/fahrzeug` (im Web `fahrzeugSvg` aus
 * `utils/fahrzeugIcons.ts`): je Typ ein eigener Bauplan mit Aufbau, Dachmodulen,
 * Markierung und Blitzmuster. Jede Marke ist gleich lang (42 Punkte), egal ob Krad oder
 * Feuerwehrkran — auf einer Karte mit vierzig Fahrzeugen zählt, dass man jedes erkennt.
 */

/** Die Lackfarben — dieselben Werte wie `--lack-*` in `styles/fahrzeuge.css` (vollständig in `Lackfarbe`). */
object Lack {
    val Feuerwehr = Lackfarbe.Feuerwehr
    val Weiss = Lackfarbe.Weiss
    val Leuchtrot = Lackfarbe.Leuchtrot
    val Thw = Lackfarbe.Thw
    val Silber = Lackfarbe.Silber
    val Polizeiblau = Lackfarbe.Polizeiblau
    val Grau = Lackfarbe.Grau
    val Rth = Lackfarbe.Rth
    val ZierHell = Lackfarbe.ZierHell
    val ZierDunkel = Lackfarbe.ZierDunkel
    val Blau = Color(0xFF3D7BFF)
    val Gelb = Color(0xFFFFB020)
}

/**
 * Der Plan, aus dem eine Marke zeichnet — der Bauplan des Typs. Die Felder daneben sind
 * Abkürzungen in ihn hinein — für alle, die nur Farbe, Maße oder Blitzmuster brauchen.
 */
data class Rissplan(val bauplan: Bauplan) {
    val lack: Color get() = bauplan.lack
    val zier: Color get() = bauplan.zier
    /** Länge und Breite in Dezimetern — ein LF 20 ist 76 × 25. */
    val laenge: Float get() = bauplan.laenge
    val breite: Float get() = bauplan.breite
    val hell: Boolean get() = bauplan.hell
    val blaulicht: Boolean get() = bauplan.blaulicht
    /** `doppel`, `vierfach` oder `rundum`. */
    val blitz: String get() = blitzmusterVon(bauplan).wort
    val fliegt: Boolean get() = bauplan.form == Form.Heli
}

/**
 * Der Plan zu einer Vorlage — der Bauplan ihres Typs (`bauplanFuer` im Web). Kennt der
 * Katalog den Typ nicht, fährt das Fahrzeug als Standardfahrzeug seiner Organisation.
 */
fun rissplan(vorlage: Fahrzeugvorlage?, typ: String): Rissplan =
    Rissplan(bauplanFuer(typ.ifBlank { vorlage?.typ.orEmpty() }, vorlage?.organisation ?: "Feuerwehr"))

/**
 * Wie hell ein Blaulichtpunkt eines Icon-Packs gerade ist (`.fzg-icon__blau`): voll an
 * im Blitz, sonst auf gut einem Drittel — dieselben Takte wie die gezeichneten Linsen.
 *
 * Takt `b` läuft eine halbe Periode versetzt — das Wechselblitzen echter Anlagen.
 */
fun blitzstaerke(muster: String, takt: String, jetzt: Long): Float =
    0.35f + 0.65f * blitzphase(Blitzmuster.von(muster), if (takt == "b") 'b' else 'a', jetzt)

/**
 * Zeichnet eine Fahrzeugmarke — mittig auf `ort`, gedreht auf `kurs` (Norden 0).
 *
 * @param bild Das Bild aus dem Icon-Pack, falls es eines für diese Vorlage gibt.
 * @param icon Die Angaben zum Bild (Drehen, Blaulichtpunkte).
 * @param deckkraft Fremde Fahrzeuge stehen blass da — 0,55 wie `.weltfzg--fremd`.
 */
fun DrawScope.fahrzeugmarkeZeichnen(
    ort: Offset,
    kurs: Double,
    plan: Rissplan,
    blaulicht: Boolean,
    jetzt: Long,
    bild: ImageBitmap? = null,
    icon: WeltPackicon? = null,
    deckkraft: Float = 1f,
    hoehe: Float = 42f,
) {
    val h = hoehe.dp.toPx()

    if (bild != null && icon != null) {
        val verhaeltnis = if ((icon.breite ?: 0) > 0 && (icon.hoehe ?: 0) > 0) {
            icon.breite!!.toFloat() / icon.hoehe!!.toFloat()
        } else {
            1f
        }
        val b = h * verhaeltnis
        val winkel = if (icon.dreht) kurs.toFloat() else 0f
        rotate(winkel, pivot = ort) {
            val links = Offset(ort.x - b / 2f, ort.y - h / 2f)
            drawImage(
                image = bild,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bild.width, bild.height),
                dstOffset = IntOffset(links.x.roundToInt(), links.y.roundToInt()),
                dstSize = IntSize(b.roundToInt(), h.roundToInt()),
                alpha = deckkraft,
            )
            val sichtbar = blaulicht && (plan.blaulicht || icon.blaulichter.isNotEmpty())
            if (sichtbar && icon.blaulichter.isNotEmpty()) {
                icon.blaulichter.forEach { p ->
                    val staerke = blitzstaerke(p.art ?: plan.blitz, p.takt ?: "a", jetzt)
                    val farbe = if (p.farbe == "gelb") Lack.Gelb else Lack.Blau
                    val pb = ((p.b ?: 0.09) * b).toFloat()
                    val ph = ((p.h ?: 0.09) * h / verhaeltnis * verhaeltnis).toFloat()
                    val mitte = Offset(links.x + (p.x * b).toFloat(), links.y + (p.y * h).toFloat())
                    if (p.form == "eckig") {
                        drawRect(
                            farbe.copy(alpha = staerke * deckkraft),
                            topLeft = mitte - Offset(pb / 2f, ph / 2f),
                            size = Size(pb, ph),
                        )
                    } else {
                        drawOval(
                            farbe.copy(alpha = staerke * deckkraft),
                            topLeft = mitte - Offset(pb / 2f, ph / 2f),
                            size = Size(pb, ph),
                        )
                    }
                }
            } else if (sichtbar) {
                blaulichthof(ort, h * 0.62f, plan.blitz, jetzt, deckkraft)
            }
        }
        return
    }

    if (blaulicht && plan.blaulicht) blaulichthof(ort, h * 0.62f, plan.blitz, jetzt, deckkraft)
    fahrzeugZeichnen(
        bauplan = plan.bauplan,
        mitte = ort,
        hoehe = h,
        drehung = kurs.toFloat(),
        blaulicht = blaulicht,
        jetzt = jetzt,
        deckkraft = deckkraft,
    )
}

/** Der Lichthof des Sondersignals — eine Scheibe unter der Marke, im Takt der Anlage. */
private fun DrawScope.blaulichthof(ort: Offset, radius: Float, blitz: String, jetzt: Long, deckkraft: Float) {
    fahrzeughofZeichnen(ort, radius, Blitzmuster.von(blitz), jetzt, deckkraft)
}
