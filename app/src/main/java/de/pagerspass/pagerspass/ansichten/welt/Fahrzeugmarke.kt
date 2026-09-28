package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.WeltPackicon
import kotlin.math.roundToInt

/*
 * Die Fahrzeugmarke der Weltkarte — Riss von oben, gedreht in Fahrtrichtung, mit
 * Blaulicht; oder das Bild aus dem eigenen Icon-Pack mit seinen Blaulichtpunkten
 * (`fahrzeugMarkup` in `components/welt/iconpack.ts`).
 *
 * <b>Der Riss hier ist die einfache Fassung.</b> Das Web zeichnet aus `fahrzeugRiss.ts`
 * und `fahrzeugBauplan.ts` je Typ eine eigene Silhouette (Aufbau, Dachmodule,
 * Markierung). Diese Datei zeichnet Lack, Fahrerhaus, Scheibe und Blaulichtbalken nach
 * Organisation und Kategorie — dieselbe Größe (42 Punkte lang), dieselbe Drehung, dasselbe
 * Blitzen. Die volle Übertragung ersetzt `rissZeichnen`, ohne dass die Karte sich ändert.
 */

/** Die Lackfarben — dieselben Werte wie `--lack-*` in `styles/fahrzeuge.css`. */
object Lack {
    val Feuerwehr = Color(0xFFB32821)
    val Weiss = Color(0xFFEFF2F7)
    val Leuchtrot = Color(0xFFE2231A)
    val Thw = Color(0xFF24316F)
    val Silber = Color(0xFFC2C8D2)
    val Polizeiblau = Color(0xFF0D3B8C)
    val Grau = Color(0xFF7C8794)
    val Rth = Color(0xFFF2C200)
    val ZierHell = Color(0xFFF4F7FB)
    val ZierDunkel = Color(0xFF1A222E)
    val Blau = Color(0xFF3D7BFF)
    val Gelb = Color(0xFFFFB020)
}

/** Die Maße und Farben, aus denen die einfache Marke zeichnet. */
data class Rissplan(
    val lack: Color,
    val zier: Color,
    /** Länge und Breite in Dezimetern — ein LF 20 ist 76 × 25. */
    val laenge: Float,
    val breite: Float,
    val hell: Boolean = false,
    val blaulicht: Boolean = true,
    /** `doppel`, `vierfach` oder `rundum`. */
    val blitz: String = "doppel",
    val fliegt: Boolean = false,
)

/**
 * Der Plan zu einer Vorlage — nach Organisation und Kategorie, wie die Lackierung im
 * Echten (Stand 2026): Feuerwehr Feuerrot, Rettungsdienst Reinweiß mit leuchtroter
 * Markierung, THW Ultramarinblau, Polizei Silber mit Verkehrsblau.
 */
fun rissplan(vorlage: Fahrzeugvorlage?, typ: String): Rissplan {
    val org = vorlage?.organisation ?: "Feuerwehr"
    val kategorie = vorlage?.kategorie.orEmpty().lowercase()
    val fliegt = vorlage?.let { fliegt(it) } == true ||
        typ.contains("RTH", ignoreCase = true) || typ.contains("ITH", ignoreCase = true) ||
        typ.contains("Hubschrauber", ignoreCase = true)

    val (laenge, breite) = when {
        fliegt -> 130f to 30f
        kategorie.contains("pkw") || kategorie.contains("führung") || kategorie.contains("streife") ->
            48f to 19f
        kategorie.contains("anhänger") -> 50f to 22f
        kategorie.contains("boot") || kategorie.contains("wasser") -> 60f to 22f
        org == "Rettungsdienst" -> 62f to 22f
        org == "Polizei" -> 50f to 19f
        else -> 76f to 25f
    }

    return when {
        fliegt && org == "Rettungsdienst" -> Rissplan(Lack.Rth, Lack.ZierDunkel, laenge, breite, hell = true, fliegt = true)
        org == "Rettungsdienst" -> Rissplan(Lack.Weiss, Lack.Leuchtrot, laenge, breite, hell = true)
        org == "Thw" -> Rissplan(Lack.Thw, Lack.ZierHell, laenge, breite, blitz = "rundum")
        org == "Polizei" -> Rissplan(Lack.Silber, Lack.Polizeiblau, laenge, breite, hell = true, blitz = "vierfach", fliegt = fliegt)
        else -> Rissplan(Lack.Feuerwehr, Lack.ZierHell, laenge, breite)
    }
}

/**
 * Ob das Blaulicht in diesem Augenblick an ist — nach Muster und Takt.
 *
 * `doppel` blitzt zweimal kurz je Periode, `vierfach` viermal, `rundum` blendet auf und
 * ab. Takt `b` läuft eine halbe Periode versetzt — das Wechselblitzen echter Anlagen.
 */
fun blitzstaerke(muster: String, takt: String, jetzt: Long): Float {
    val periode = when (muster) {
        "vierfach" -> 900L
        "rundum" -> 1_200L
        else -> 800L
    }
    val versatz = if (takt == "b") periode / 2 else 0L
    val phase = ((jetzt + versatz) % periode).toFloat() / periode
    return when (muster) {
        "rundum" -> (0.5f + 0.5f * kotlin.math.sin(phase * 2f * Math.PI.toFloat()))
        "vierfach" -> if ((phase * 8).toInt() % 2 == 0 && phase < 0.5f) 1f else 0.15f
        else -> if (phase < 0.12f || (phase in 0.2f..0.32f)) 1f else 0.15f
    }
}

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
    rissZeichnen(ort, kurs.toFloat(), plan, blaulicht, jetzt, h, deckkraft)
}

/** Der Lichthof des Sondersignals — eine Scheibe unter dem Riss, im Takt der Anlage. */
private fun DrawScope.blaulichthof(ort: Offset, radius: Float, blitz: String, jetzt: Long, deckkraft: Float) {
    val staerke = blitzstaerke(blitz, "a", jetzt)
    drawCircle(Lack.Blau.copy(alpha = 0.28f * staerke * deckkraft), radius = radius, center = ort)
}

/**
 * Der einfache Riss: Karosserie im Lack, Fahrerhaus vorn mit Scheibe, Markierungsstreifen,
 * Blaulichtbalken auf dem Dach. Vorn ist oben — gedreht wird auf den Kurs.
 */
private fun DrawScope.rissZeichnen(
    ort: Offset,
    kurs: Float,
    plan: Rissplan,
    blaulicht: Boolean,
    jetzt: Long,
    h: Float,
    deckkraft: Float,
) {
    val laenge = h
    val breite = h * (plan.breite / plan.laenge).coerceIn(0.2f, 0.6f)
    val kante = if (plan.hell) Lack.ZierDunkel.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.45f)

    rotate(kurs, pivot = ort) {
        val links = ort.x - breite / 2f
        val oben = ort.y - laenge / 2f

        if (plan.fliegt) {
            // Der Hubschrauber: Rumpf, Heckausleger, Rotorkreis.
            drawOval(plan.lack.copy(alpha = deckkraft), Offset(links, oben), Size(breite, laenge * 0.55f))
            drawRect(
                plan.lack.copy(alpha = deckkraft),
                topLeft = Offset(ort.x - breite * 0.12f, oben + laenge * 0.45f),
                size = Size(breite * 0.24f, laenge * 0.5f),
            )
            drawCircle(
                Color.Black.copy(alpha = 0.35f * deckkraft),
                radius = laenge * 0.42f,
                center = Offset(ort.x, oben + laenge * 0.28f),
                style = Stroke(1.2.dp.toPx()),
            )
            return@rotate
        }

        val ecke = CornerRadius(breite * 0.18f, breite * 0.18f)
        // Der Saum — ohne ihn steht ein weißer Wagen auf heller Karte nicht ab.
        drawRoundRect(
            kante.copy(alpha = kante.alpha * deckkraft),
            topLeft = Offset(links - 1.dp.toPx(), oben - 1.dp.toPx()),
            size = Size(breite + 2.dp.toPx(), laenge + 2.dp.toPx()),
            cornerRadius = ecke,
        )
        drawRoundRect(plan.lack.copy(alpha = deckkraft), Offset(links, oben), Size(breite, laenge), ecke)

        // Das Fahrerhaus vorn — etwas dunkler, mit Scheibe.
        val kabine = laenge * 0.24f
        drawRoundRect(
            Color.Black.copy(alpha = 0.12f * deckkraft),
            Offset(links, oben),
            Size(breite, kabine),
            ecke,
        )
        drawRoundRect(
            Color(0xFF1B2733).copy(alpha = 0.85f * deckkraft),
            Offset(links + breite * 0.12f, oben + kabine * 0.12f),
            Size(breite * 0.76f, kabine * 0.3f),
            CornerRadius(breite * 0.08f, breite * 0.08f),
        )

        // Die Markierung: zwei Streifen längs des Aufbaus.
        val streifen = breite * 0.1f
        drawRect(
            plan.zier.copy(alpha = deckkraft),
            topLeft = Offset(links, oben + kabine + laenge * 0.05f),
            size = Size(streifen, laenge - kabine - laenge * 0.1f),
        )
        drawRect(
            plan.zier.copy(alpha = deckkraft),
            topLeft = Offset(links + breite - streifen, oben + kabine + laenge * 0.05f),
            size = Size(streifen, laenge - kabine - laenge * 0.1f),
        )

        // Der Blaulichtbalken auf dem Fahrerhausdach — zwei Hälften im Wechsel.
        if (plan.blaulicht) {
            val balkenOben = oben + kabine * 0.52f
            val balkenHoehe = kabine * 0.2f
            val an = blaulicht
            val a = if (an) blitzstaerke(plan.blitz, "a", jetzt) else 0.35f
            val b = if (an) blitzstaerke(plan.blitz, "b", jetzt) else 0.35f
            drawRect(
                Lack.Blau.copy(alpha = a * deckkraft),
                topLeft = Offset(links + breite * 0.12f, balkenOben),
                size = Size(breite * 0.36f, balkenHoehe),
            )
            drawRect(
                Lack.Blau.copy(alpha = b * deckkraft),
                topLeft = Offset(links + breite * 0.52f, balkenOben),
                size = Size(breite * 0.36f, balkenHoehe),
            )
        }
    }
}
