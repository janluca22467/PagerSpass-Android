package de.pagerspass.pagerspass.ui.fahrzeug

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Die Zeichnung eines Fahrzeugs aus seinem Bauplan — übertragen aus `utils/fahrzeugRiss.ts`.
 *
 * Jeder Katalogtyp hat einen eigenen Bauplan (siehe `Bauplan.kt`): Länge, Breite,
 * Kabine, Aufbau, Dachmodule, Markierung. Gezeichnet wird im Dezimeter-Raster 44 × 112
 * (= 4,4 m × 11,2 m), Front oben (0° = Norden), jedes Fahrzeug mittig auf (22 | 56).
 * Die Maße unten sind Zahl für Zahl die des Webs; wer dort etwas ändert, ändert es hier.
 *
 * Zwei Darstellungen aus einer Zeichnung: Wer sie eng beschnitten braucht (die
 * Lagekarte, wo alle Marken gleich groß erscheinen sollen), nimmt `kaestchenEng`; wer
 * echte Größenverhältnisse zeigen will, nimmt das gemeinsame Raster — dann ist der
 * FwK 30 fünfmal so lang wie das Krad.
 */

/** Breite und Länge des gemeinsamen Rasters in Dezimetern. */
const val RASTER_B = 44f
const val RASTER_L = 112f
private const val CX = 22f
private const val CY = 56f

/** Die Farbe des eigenen Standorts — `--eigenposition`, die Farbe aller Lichthöfe. */
private val EIGENPOSITION = Color(0xFF2F6BFF)

/** Ein Kästchen im Raster: linke obere Ecke, Breite, Länge. */
data class Rasterkaestchen(val x: Float, val y: Float, val breite: Float, val laenge: Float)

/**
 * Das enge Kästchen um genau dieses Fahrzeug.
 *
 * Auf der Lagekarte soll jede Marke gleich groß erscheinen — sonst verschwindet das
 * Krad, sobald man herauszoomt. Der Rand lässt Platz für das, was über die Karosserie
 * hinausragt: Spiegel, Blaulichthöfe, der abgelegte Leitersatz einer Drehleiter, der
 * Ausleger des Krans.
 */
fun kaestchenEng(bp: Bauplan): Rasterkaestchen {
    if (bp.form == Form.Heli) return Rasterkaestchen(CX - 22f, CY - 30f, 44f, 76f)
    val rand = if (bp.form == Form.Boot) 5f else 4f
    val ueberstand = when {
        Dachmodul.Drehleiter in bp.dach || Dachmodul.Mast in bp.dach -> 14f
        Dachmodul.Grosskran in bp.dach -> 10f
        else -> 0f
    }
    val breite = bp.breite + rand * 2f
    val laenge = bp.laenge + rand * 2f + ueberstand
    return Rasterkaestchen(CX - breite / 2f, CY - laenge / 2f, breite, laenge)
}

/** Das gemeinsame Raster — echte Größenverhältnisse für alle Typen. */
val FAHRZEUG_RASTER = Rasterkaestchen(0f, 0f, RASTER_B, RASTER_L)

/**
 * Seitenverhältnis des engen Kästchens: Breite zu Länge. Wer die Zeichnung auf eine
 * feste Höhe setzt, rechnet sich damit die Breite aus.
 */
fun fahrzeugSeitenverhaeltnis(bp: Bauplan): Float {
    val k = kaestchenEng(bp)
    return k.breite / k.laenge
}

/**
 * Zeichnet den Riss eines Bauplans.
 *
 * Die Länge des Kästchens (`kaestchen`, sonst das enge) wird `hoehe` Bildpunkte lang,
 * seine Mitte liegt auf `mitte`. `drehung` ist der Kompasskurs in Grad — die Zeichnung
 * schaut selbst nach oben, 90 legt sie quer mit der Front nach rechts.
 *
 * @param blaulicht Blendet Lichtbalken, Front- und Heckblitzer und den Schein auf der
 *   Fahrbahn ein — nur, wenn der Bauplan überhaupt Blaulicht zeigt (am RTH nicht).
 * @param jetzt Die Uhrzeit in Millisekunden für Blitztakt, Rotor und Bugwelle. Ohne
 *   sie steht alles still: Blitzer aus, Rotor in Ruhe, keine Welle.
 * @param deckkraft Die Deckkraft der ganzen Marke — fremde Fahrzeuge stehen blass da.
 *   Wie `opacity` im Web gilt sie für das Ganze, nicht für jedes Teil einzeln.
 */
fun DrawScope.fahrzeugZeichnen(
    bauplan: Bauplan,
    mitte: Offset = center,
    hoehe: Float = size.height,
    drehung: Float = 0f,
    blaulicht: Boolean = false,
    jetzt: Long? = null,
    deckkraft: Float = 1f,
    kaestchen: Rasterkaestchen = kaestchenEng(bauplan),
) {
    if (hoehe <= 0f || kaestchen.laenge <= 0f || deckkraft <= 0f) return
    val bewegt = jetzt != null &&
        ((blaulicht && bauplan.blaulicht) || bauplan.form == Form.Heli || bauplan.form == Form.Boot)
    if (bewegt) {
        rissLive(bauplan, mitte, hoehe, drehung, blaulicht, jetzt, deckkraft, kaestchen)
        return
    }
    // Was stillsteht, zeichnet sich einmal und wird danach nur noch als Bild gesetzt: Ein
    // Riss sind gut hundert Bauteile mit eigenen Verläufen, und auf der Weltkarte stehen
    // davon Hunderte gleichzeitig — dieselbe Rechnung, die im Web die geteilten `<defs>`
    // begründet (`utils/fahrzeugDefs.ts`).
    val bild = rissbild(bauplan, hoehe, kaestchen)
    rotate(drehung, pivot = mitte) {
        drawImage(bild, topLeft = Offset(mitte.x - bild.width / 2f, mitte.y - bild.height / 2f), alpha = deckkraft)
    }
}

/** Der Schlüssel eines fertigen Rissbilds: welcher Plan, wie groß, welches Kästchen. */
private data class Bildschluessel(val bauplan: Bauplan, val hoehe: Int, val kaestchen: Rasterkaestchen)

/** Die zuletzt gebrauchten Rissbilder — ein kleiner Vorrat, der Älteste geht zuerst. */
private val bildvorrat = object : LinkedHashMap<Bildschluessel, ImageBitmap>(64, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Bildschluessel, ImageBitmap>?): Boolean =
        size > 160
}

/** Der ruhende Riss als Bild, genau so groß, wie er gesetzt wird — im Kästchen beschnitten wie das `<svg>`. */
private fun DrawScope.rissbild(bauplan: Bauplan, hoehe: Float, kaestchen: Rasterkaestchen): ImageBitmap {
    val h = ceil(hoehe).toInt().coerceAtLeast(1)
    val schluessel = Bildschluessel(bauplan, h, kaestchen)
    return bildvorrat.getOrPut(schluessel) {
        val w = ceil(h * kaestchen.breite / kaestchen.laenge).toInt().coerceAtLeast(1)
        val bild = ImageBitmap(w, h)
        CanvasDrawScope().draw(this, layoutDirection, Canvas(bild), Size(w.toFloat(), h.toFloat())) {
            rissLive(bauplan, Offset(w / 2f, h / 2f), h.toFloat(), 0f, false, null, 1f, kaestchen)
        }
        bild
    }
}

/** Die eigentliche Zeichnung, Teil für Teil — ohne Vorrat. */
private fun DrawScope.rissLive(
    bauplan: Bauplan,
    mitte: Offset,
    hoehe: Float,
    drehung: Float,
    blaulicht: Boolean,
    jetzt: Long?,
    deckkraft: Float,
    kaestchen: Rasterkaestchen,
) {
    val massstab = hoehe / kaestchen.laenge
    translate(mitte.x, mitte.y) {
        rotate(drehung, pivot = Offset.Zero) {
            scale(massstab, massstab, pivot = Offset.Zero) {
                translate(-(kaestchen.x + kaestchen.breite / 2f), -(kaestchen.y + kaestchen.laenge / 2f)) {
                    val riss = Riss(this, bauplan, jetzt, massstab)
                    if (deckkraft < 1f) {
                        // Wie `opacity` am Element: erst ganz zeichnen, dann als Ganzes abblenden —
                        // sonst schiene die Karosserie durch jedes Anbauteil hindurch.
                        val rand = 24f
                        val bereich = Rect(
                            kaestchen.x - rand,
                            kaestchen.y - rand,
                            kaestchen.x + kaestchen.breite + rand,
                            kaestchen.y + kaestchen.laenge + rand,
                        )
                        val farbe = androidx.compose.ui.graphics.Paint().apply { alpha = deckkraft }
                        drawContext.canvas.saveLayer(bereich, farbe)
                        riss.zeichne(blaulicht)
                        drawContext.canvas.restore()
                    } else {
                        riss.zeichne(blaulicht)
                    }
                }
            }
        }
    }
}

/**
 * Der Lichthof um die ganze Marke (`.fzg-icon--sonder`) — der eigentliche Grund, warum
 * man das Sondersignal auf der Karte überhaupt sieht: Die Linsen auf einer 42 Punkte
 * langen Marke sind kleiner als ein Bildpunkt. Er wächst deshalb nicht mit der
 * Zeichnung, sondern hängt an `radius` in Bildpunkten, und schlägt im Takt der Anlage.
 */
fun DrawScope.fahrzeughofZeichnen(
    mitte: Offset,
    radius: Float,
    muster: Blitzmuster,
    jetzt: Long,
    deckkraft: Float = 1f,
) {
    val an = hofphase(muster, jetzt)
    val staerke = (0.12f + 0.88f * an) * deckkraft
    if (staerke <= 0f || radius <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFF9CC4FF).copy(alpha = 0.55f * staerke),
            0.35f to EIGENPOSITION.copy(alpha = 0.4f * staerke),
            1f to EIGENPOSITION.copy(alpha = 0f),
            center = mitte,
            radius = radius,
        ),
        radius = radius,
        center = mitte,
    )
}

/** Das Blitzmuster eines Bauplans — ohne Angabe je nach Form, wie im Web. */
fun blitzmusterVon(bp: Bauplan): Blitzmuster = bp.blitz ?: when (bp.form) {
    Form.Heli -> Blitzmuster.Vierfach
    Form.Boot -> Blitzmuster.Rundum
    else -> Blitzmuster.Doppel
}

/**
 * Eine Zeichnung in Arbeit: der Bauplan, die Fläche, die Uhr. Alle Maße sind
 * Rasterkoordinaten — die Umrechnung auf Bildpunkte steckt in der Transformation, die
 * `fahrzeugZeichnen` vorher setzt.
 */
private class Riss(
    private val d: DrawScope,
    private val bp: Bauplan,
    private val jetzt: Long?,
    /** Bildpunkte je Rastereinheit — für Leuchtschein, der im Web in Bildpunkten gemessen ist. */
    private val massstab: Float,
) {
    // ------------------------------------------------------------ Grundformen

    private fun stil(t: Teil): Stil = if (bp.hell) t.hell ?: t.stil else t.stil

    private fun fuellung(s: Stil): Color? = when (s.quelle) {
        Quelle.Fest -> s.fuellung
        Quelle.Lack -> bp.lack
        Quelle.Zier -> bp.zier
    }

    /** `<rect>` mit `rx` — wie im SVG getrennt auf die halbe Breite und Höhe begrenzt. */
    fun rect(t: Teil, x: Float, y: Float, w: Float, h: Float, rx: Float = 0f, deckung: Float = 1f) {
        if (w <= 0f || h <= 0f || deckung <= 0f) return
        val s = stil(t)
        val ecke = CornerRadius(min(rx, w / 2f), min(rx, h / 2f))
        fuellung(s)?.let { d.drawRoundRect(it, Offset(x, y), Size(w, h), ecke, alpha = deckung) }
        s.strich?.let {
            d.drawRoundRect(it, Offset(x, y), Size(w, h), ecke, style = Stroke(s.breite), alpha = deckung)
        }
    }

    fun kreis(t: Teil, cx: Float, cy: Float, r: Float, deckung: Float = 1f) {
        if (r <= 0f || deckung <= 0f) return
        val s = stil(t)
        fuellung(s)?.let { d.drawCircle(it, r, Offset(cx, cy), alpha = deckung) }
        s.strich?.let { d.drawCircle(it, r, Offset(cx, cy), alpha = deckung, style = Stroke(s.breite)) }
    }

    fun linie(t: Teil, x1: Float, y1: Float, x2: Float, y2: Float) {
        val s = stil(t)
        s.strich?.let { d.drawLine(it, Offset(x1, y1), Offset(x2, y2), strokeWidth = s.breite) }
    }

    fun pfad(t: Teil, p: Path, deckung: Float = 1f) {
        val s = stil(t)
        fuellung(s)?.let { d.drawPath(p, it, alpha = deckung) }
        s.strich?.let { d.drawPath(p, it, alpha = deckung, style = Stroke(s.breite)) }
    }

    /** Die Rundung im Querschnitt (`#woelbung`): dunkle Flanken, heller Grat links der Mitte. */
    private fun woelbungPinsel(links: Float, breite: Float): Brush = Brush.horizontalGradient(
        0f to Color.Black.copy(alpha = 0.34f),
        0.22f to Color.White.copy(alpha = 0.16f),
        0.52f to Color.White.copy(alpha = 0.02f),
        1f to Color.Black.copy(alpha = 0.4f),
        startX = links,
        endX = links + breite,
    )

    fun woelbungRect(x: Float, y: Float, w: Float, h: Float, rx: Float) {
        if (w <= 0f || h <= 0f) return
        d.drawRoundRect(woelbungPinsel(x, w), Offset(x, y), Size(w, h), CornerRadius(min(rx, w / 2f), min(rx, h / 2f)))
    }

    fun woelbungPfad(p: Path) {
        val b = p.getBounds()
        d.drawPath(p, woelbungPinsel(b.left, b.width))
    }

    /** Die Scheiben (`#glas`): oben der Himmel, unten der dunkle Innenraum. */
    fun glasPfad(p: Path) {
        val b = p.getBounds()
        d.drawPath(
            p,
            Brush.linearGradient(
                0f to Color(0x9F, 0xD0, 0xFF, 128),
                1f to Color(0x0A, 0x14, 0x20, 235),
                start = Offset(b.left, b.top),
                end = Offset(b.left + b.width * 0.6f, b.bottom),
            ),
        )
    }

    // ------------------------------------------------------------ Pfade

    /** Ein kleiner Pfadbauer in der Schreibweise des SVG: `M`, `L`, `C`, `Q`, `Z`. */
    private class P {
        val pfad = Path()
        fun m(x: Float, y: Float) = apply { pfad.moveTo(x, y) }
        fun l(x: Float, y: Float) = apply { pfad.lineTo(x, y) }
        fun c(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) =
            apply { pfad.cubicTo(x1, y1, x2, y2, x, y) }
        fun q(x1: Float, y1: Float, x: Float, y: Float) = apply { pfad.quadraticTo(x1, y1, x, y) }
        fun z(): Path { pfad.close(); return pfad }
        fun offen(): Path = pfad
    }

    /** Ein Viereck aus vier Punkten — Scheiben, Mulden. */
    private fun viereck(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float): Path =
        P().m(x1, y1).l(x2, y2).l(x3, y3).l(x4, y4).z()

    // ------------------------------------------------------------ Blaulicht

    /** Wie weit eine Lampe gerade an ist — ohne Uhr oder ohne Muster steht sie auf aus. */
    private fun an(muster: Blitzmuster?, takt: Char): Float {
        val j = jetzt ?: return 0f
        if (muster == null) return 0f
        return blitzphase(muster, takt, j)
    }

    /**
     * Die Linse eines Blitzers (`.fz-blau`): blassblau, mit einem Leuchten in der Farbe
     * des Lichts (`drop-shadow` 2 und 6 Punkte). Zwischen zwei Salven bleibt sie
     * sichtbar blau — ein Balken, der 86 % der Zeit schwarz ist, sieht aus wie gar kein
     * Blaulicht.
     */
    fun blau(muster: Blitzmuster?, takt: Char, cx: Float, cy: Float, r: Float) {
        val deckung = 0.55f + 0.45f * an(muster, takt)
        val schein = r + 6f * d.density / massstab.coerceAtLeast(0.01f)
        d.drawCircle(
            brush = Brush.radialGradient(
                0f to EIGENPOSITION.copy(alpha = 0.75f * deckung),
                (r / schein).coerceIn(0f, 1f) to EIGENPOSITION.copy(alpha = 0.6f * deckung),
                1f to EIGENPOSITION.copy(alpha = 0f),
                center = Offset(cx, cy),
                radius = schein,
            ),
            radius = schein,
            center = Offset(cx, cy),
        )
        kreis(Teil.Blau, cx, cy, r, deckung)
    }

    /** Der Lichthof eines Blitzers (`.fz-halo`) — ein Drittel so hell wie die Linse. */
    fun halo(muster: Blitzmuster?, takt: Char, cx: Float, cy: Float, r: Float) =
        kreis(Teil.Halo, cx, cy, r, 0.12f + 0.48f * an(muster, takt))

    /** Ein Frontblitzer im Scheinwerfer (`.fz-weissblitz`) — zwischen den Salven ganz aus. */
    fun weissblitz(muster: Blitzmuster?, takt: Char, x: Float, y: Float, w: Float, h: Float) {
        val deckung = an(muster, takt)
        if (deckung <= 0f) return
        rect(Teil.Weissblitz, x - 0.4f, y - 0.4f, w + 0.8f, h + 0.8f, 1.2f, 0.35f * deckung)
        rect(Teil.Weissblitz, x, y, w, h, 0.8f, deckung)
    }

    // ------------------------------------------------------------ Zeichnen

    fun zeichne(blaulichtGewuenscht: Boolean) {
        // Der Fahrzustand liefert das fachliche Sondersignal. Einzelne Bauarten können die
        // optische Anlage trotzdem unterdrücken — beim RTH bleibt der Zeitvorteil bestehen,
        // ohne dass am Hubschrauber blaues Blinken gezeichnet wird.
        val mitBlaulicht = blaulichtGewuenscht && bp.blaulicht
        when (bp.form) {
            Form.Heli -> return helikopter(mitBlaulicht)
            Form.Boot -> return boot(mitBlaulicht)
            Form.Moto -> return kraftrad(mitBlaulicht)
            Form.Quad -> return quad(mitBlaulicht)
            Form.Gespann -> return gespann(mitBlaulicht)
            Form.Strasse -> Unit
        }

        val laenge = bp.laenge
        val breite = bp.breite
        val top = CY - laenge / 2f
        val bot = CY + laenge / 2f
        val lx = CX - breite / 2f
        val rx = CX + breite / 2f
        val kl = bp.kabinenlaenge ?: when (bp.klasse) {
            Klasse.Lkw -> min(24f, laenge * 0.34f)
            Klasse.Pkw -> laenge * 0.42f
            else -> laenge * 0.31f
        }
        val y0 = top + kl
        val y1 = bot - (bp.heckfrei ?: 1.5f)

        if (mitBlaulicht) lichtschein(top, bot, lx, rx)
        raeder(bp, top, laenge, lx, rx)
        if (bp.klasse == Klasse.Pkw) {
            pkw(lx, rx, top, bot)
        } else {
            karosserie(lx, top, breite, laenge, bp.radius ?: 3f)
            aufbau(lx, rx, y0, y1)
            kabine(lx, rx, top, kl)
        }
        bp.dach.forEach { modul(it, lx, rx, y0, y1, top, bot) }
        markierung(lx, rx, top, bot)
        if (mitBlaulicht) blaulicht(lx, rx, top, bot, kl)
    }

    // ------------------------------------------------------------ Bausteine

    /** Reifen einer Achse, links und rechts, leicht über die Karosserie hinaus. */
    private fun achse(y: Float, lx: Float, rx: Float, breit: Float, lang: Float, zwilling: Boolean) {
        fun reifen(x: Float) {
            rect(Teil.Rad, x, y - lang / 2f, breit, lang, breit / 2.4f)
            if (zwilling) {
                val b2 = breit * 0.72f
                rect(Teil.Rad, x + breit * 0.55f, y - lang / 2f, b2, lang, b2 / 2.4f)
            }
        }
        reifen(lx - breit * 0.55f)
        reifen(rx - breit * (if (zwilling) 0.9f else 0.45f))
    }

    private fun raeder(plan: Bauplan, top: Float, laenge: Float, lx: Float, rx: Float) {
        val schwer = plan.klasse == Klasse.Lkw
        val breit = plan.reifen ?: if (schwer) 3.4f else 2.6f
        val lang = plan.reifenLang ?: if (schwer) 8.4f else 6.2f
        val stellen = plan.achsen ?: if (schwer) listOf(0.17f, 0.78f) else listOf(0.2f, 0.79f)
        // Beim Pkw verschwinden die Räder fast unter den Kotflügeln, beim Lkw stehen sie heraus.
        val versatz = if (plan.klasse == Klasse.Pkw) 0.9f else 0f
        stellen.forEachIndexed { i, anteil ->
            achse(top + laenge * anteil, lx + versatz, rx - versatz, breit, lang, schwer && i > 0 && plan.zwilling)
        }
    }

    /** Karosserie: Grundlack, Rundung im Querschnitt, Kante. */
    private fun karosserie(lx: Float, top: Float, breite: Float, laenge: Float, r: Float) {
        rect(Teil.Lack, lx, top, breite, laenge, r)
        woelbungRect(lx, top, breite, laenge, r)
        rect(Teil.Kante, lx, top, breite, laenge, r)
    }

    /**
     * Pkw-Umriss: vorne gerundete Haube, hinten kantiges Heck — ein Kastenwagen mit
     * runden Ecken sieht aus wie ein Transporter, deshalb ein eigener Pfad.
     */
    private fun pkwUmriss(lx: Float, rx: Float, top: Float, bot: Float): Path {
        val b = rx - lx
        return P()
            .m(CX, top)
            .c(lx + b * 0.86f, top, rx, top + 2.4f, rx, top + 7f)
            .l(rx, bot - 6f)
            .c(rx, bot - 1.2f, rx - 1.4f, bot, rx - 3.4f, bot)
            .l(lx + 3.4f, bot)
            .c(lx + 1.4f, bot, lx, bot - 1.2f, lx, bot - 6f)
            .l(lx, top + 7f)
            .c(lx, top + 2.4f, lx + b * 0.14f, top, CX, top)
            .z()
    }

    /** Pkw von oben: Haube mit Sicken, Kanzel, Heckklappe. */
    private fun pkw(lx: Float, rx: Float, top: Float, bot: Float) {
        val b = rx - lx
        val laenge = bot - top
        val umriss = pkwUmriss(lx, rx, top, bot)
        val haube = top + laenge * 0.3f
        val kanzel = top + laenge * 0.62f
        pfad(Teil.Lack, umriss)
        woelbungPfad(umriss)
        // Motorhaube mit zwei Sicken
        linie(Teil.Sicke, lx + b * 0.3f, top + 3f, lx + b * 0.26f, haube)
        linie(Teil.Sicke, rx - b * 0.3f, top + 3f, rx - b * 0.26f, haube)
        // Frontscheibe, Dach, Heckscheibe
        glasPfad(viereck(lx + 2.4f, haube, rx - 2.4f, haube, rx - 3.4f, haube + 5.5f, lx + 3.4f, haube + 5.5f))
        rect(Teil.Lack, lx + 2.2f, haube + 5.6f, b - 4.4f, kanzel - haube - 5.9f, 1.6f)
        rect(Teil.Dachglanz, lx + 2.2f, haube + 5.6f, b - 4.4f, kanzel - haube - 5.9f, 1.6f)
        glasPfad(viereck(lx + 3.2f, kanzel, rx - 3.2f, kanzel, rx - 2.6f, kanzel + 4.6f, lx + 2.6f, kanzel + 4.6f))
        // Seitenscheiben schmal an der Kanzel
        val seite = max(2f, kanzel - haube - 7f)
        rect(Teil.GlasKlein, lx + 1.5f, haube + 6.2f, 1.4f, seite, 0.6f)
        rect(Teil.GlasKlein, rx - 2.9f, haube + 6.2f, 1.4f, seite, 0.6f)
        // Scheinwerfer und Kühlergrill
        rect(Teil.Licht, lx + 2.2f, top + 0.9f, 3.4f, 1.7f, 0.8f)
        rect(Teil.Licht, rx - 5.6f, top + 0.9f, 3.4f, 1.7f, 0.8f)
        rect(Teil.Grill, CX - 3.4f, top + 0.6f, 6.8f, 1.6f, 0.7f)
        // Spiegel
        rect(Teil.Spiegel, lx - 1.9f, haube + 0.4f, 2.1f, 2.2f, 0.7f)
        rect(Teil.Spiegel, rx - 0.2f, haube + 0.4f, 2.1f, 2.2f, 0.7f)
    }

    /** Fahrerhaus: Frontscheibe, Kabinendach, Spiegel, Scheinwerfer, Stoßstange. */
    private fun kabine(lx: Float, rx: Float, top: Float, kl: Float) {
        val b = rx - lx
        rect(Teil.Stossstange, lx + 0.6f, top + 0.5f, b - 1.2f, 2.2f, 1f)
        rect(Teil.Licht, lx + 1.4f, top + 0.7f, 3f, 1.8f, 0.8f)
        rect(Teil.Licht, rx - 4.4f, top + 0.7f, 3f, 1.8f, 0.8f)
        // Frontscheibe (in der Draufsicht eine nach hinten breiter werdende Fläche)
        val sy = top + 3.4f
        val sh = min(6.5f, kl * 0.42f)
        glasPfad(viereck(lx + 2.6f, sy + sh, lx + 1.6f, sy, rx - 1.6f, sy, rx - 2.6f, sy + sh))
        pfad(Teil.Glanz, viereck(lx + 2.4f, sy + sh, lx + 1.9f, sy + 0.4f, lx + 4.6f, sy + 0.4f, lx + 5.6f, sy + sh))
        // Kabinendach
        rect(Teil.Dach, lx + 1.4f, sy + sh + 0.4f, b - 2.8f, max(2f, kl - sh - 4.6f), 1.4f)
        // Spiegel
        val my = top + 4.6f
        rect(Teil.Spiegel, lx - 2.3f, my, 2.4f, 2.6f, 0.7f)
        rect(Teil.Spiegel, rx - 0.1f, my, 2.4f, 2.6f, 0.7f)
    }

    /** Seitenfenster für Mannschaftskabinen und Busse. */
    private fun seitenfenster(lx: Float, rx: Float, y: Float, h: Float, reihen: Int) {
        val hh = h / reihen - 0.9f
        for (i in 0 until reihen) {
            val yy = y + i * (h / reihen)
            rect(Teil.GlasKlein, lx + 1.1f, yy, 2.2f, hh, 0.8f)
            rect(Teil.GlasKlein, rx - 3.3f, yy, 2.2f, hh, 0.8f)
        }
    }

    /** Geräteraum-Rollläden: von oben nur als Fugenkanten sichtbar. */
    private fun geraeteraeume(lx: Float, rx: Float, y0: Float, y1: Float, fach: Float) {
        val n = max(2, ((y1 - y0) / fach).roundToInt())
        for (i in 1 until n) {
            val y = y0 + (y1 - y0) * i / n
            linie(Teil.Fuge, lx + 0.5f, y, lx + 2.6f, y)
            linie(Teil.Fuge, rx - 2.6f, y, rx - 0.5f, y)
        }
    }

    /** Der Hakenarm des Wechselladers — offen nach vorn, als Bügel am Heck. */
    private fun hakenarm(y1: Float) {
        pfad(
            Teil.Hakenarm,
            P().m(CX - 2.6f, y1 - 4f).l(CX - 2.6f, y1 - 0.5f).l(CX + 2.6f, y1 - 0.5f).l(CX + 2.6f, y1 - 4f).offen(),
        )
    }

    // ------------------------------------------------------------ Aufbauten

    private fun aufbau(lx: Float, rx: Float, y0: Float, y1: Float) {
        val b = rx - lx
        when (bp.aufbau ?: Aufbauart.Kasten) {
            Aufbauart.Kasten -> {
                // Aufbau mit Dachpodest und Geräteräumen (LF, RW, GW …)
                rect(Teil.Aufbau, lx + 0.5f, y0, b - 1f, y1 - y0, 1.6f)
                rect(Teil.Podest, lx + 3f, y0 + 1.6f, b - 6f, y1 - y0 - 3.4f, 1.2f)
                geraeteraeume(lx, rx, y0 + 2f, y1 - 2f, 8f)
            }
            Aufbauart.Koffer -> {
                // Glatter Kofferaufbau (RTW, ITW, ELW 2, GW-Logistik)
                rect(Teil.Koffer, lx + 0.2f, y0, b - 0.4f, y1 - y0, 1.8f)
                rect(Teil.Klima, CX - 3.4f, y0 + 2.4f, 6.8f, 4.4f, 1f)
                linie(Teil.Fuge, lx + 1.2f, y0 + 1f, lx + 1.2f, y1 - 1f)
                linie(Teil.Fuge, rx - 1.2f, y0 + 1f, rx - 1.2f, y1 - 1f)
            }
            Aufbauart.Tank -> {
                // Tankaufbau: runder Kessel, Domdeckel, Geräteräume außen
                rect(Teil.Aufbau, lx + 0.5f, y0, b - 1f, y1 - y0, 1.6f)
                rect(Teil.Tank, CX - b * 0.28f, y0 + 1.4f, b * 0.56f, y1 - y0 - 3f, b * 0.28f)
                kreis(Teil.Dom, CX, y0 + (y1 - y0) * 0.32f, 2.4f)
                kreis(Teil.Dom, CX, y0 + (y1 - y0) * 0.68f, 1.6f)
                geraeteraeume(lx, rx, y0 + 2f, y1 - 2f, 9f)
            }
            Aufbauart.Pritsche -> {
                // Offene Ladefläche mit Ladung
                rect(Teil.Pritsche, lx + 0.6f, y0, b - 1.2f, y1 - y0, 1f)
                val n = max(2, ((y1 - y0) / 9f).roundToInt())
                val h = (y1 - y0 - 3f) / n - 1.4f
                for (i in 0 until n) {
                    rect(Teil.Ladung, lx + 2.4f, y0 + 1.6f + i * ((y1 - y0 - 3f) / n), b - 4.8f, h, 0.8f)
                }
            }
            Aufbauart.Ab -> {
                // Abrollbehälter auf Wechselladerfahrzeug
                rect(Teil.Container, lx + 1.2f, y0 + 0.6f, b - 2.4f, y1 - y0 - 5f, 0.8f)
                var y = y0 + 4f
                while (y < y1 - 7f) {
                    linie(Teil.Riffel, lx + 1.2f, y, rx - 1.2f, y)
                    y += 3.4f
                }
                hakenarm(y1)
            }
            Aufbauart.Mulde -> {
                // Flüssigkeitsdichte Mulde: offene, nach innen dunkler werdende Wanne auf dem
                // Hakenarm. Der umlaufende Rand unterscheidet sie vom geschlossenen Container.
                pfad(Teil.Mulde, viereck(lx + 1.2f, y0 + 1f, rx - 1.2f, y0 + 1f, rx - 2.4f, y1 - 5f, lx + 2.4f, y1 - 5f))
                pfad(Teil.MuldeInnen, viereck(lx + 3f, y0 + 3f, rx - 3f, y0 + 3f, rx - 3.8f, y1 - 7f, lx + 3.8f, y1 - 7f))
                hakenarm(y1)
            }
            Aufbauart.Bus -> {
                // Mannschaftsraum mit Fensterband
                rect(Teil.Koffer, lx + 0.2f, y0, b - 0.4f, y1 - y0, 1.8f)
                seitenfenster(lx, rx, y0 + 1.6f, y1 - y0 - 3.2f, max(2, ((y1 - y0) / 9f).roundToInt()))
            }
            Aufbauart.Gepanzert -> {
                // Sonderwagen: Gitter vor den Scheiben, kantiger Aufbau
                rect(Teil.Panzer, lx + 0.2f, y0, b - 0.4f, y1 - y0, 1f)
                var y = y0 + 2.5f
                while (y < y1 - 2f) {
                    linie(Teil.Gitter, lx + 1f, y, lx + 3.4f, y)
                    linie(Teil.Gitter, rx - 3.4f, y, rx - 1f, y)
                    y += 3.4f
                }
            }
            Aufbauart.Keiner -> Unit
        }
    }

    // ------------------------------------------------------------ Dachmodule

    private fun modul(name: Dachmodul, lx: Float, rx: Float, y0: Float, y1: Float, top: Float, bot: Float) {
        val b = rx - lx
        when (name) {
            Dachmodul.Steckleiter -> {
                // Steckleiter längs auf dem Dach
                val a = y0 + 2.5f
                val e = y1 - 2.5f
                val w = 4.6f
                rect(Teil.Holm, CX - w / 2f, a, 0.9f, e - a)
                rect(Teil.Holm, CX + w / 2f - 0.9f, a, 0.9f, e - a)
                var y = a + 2f
                while (y < e - 1f) {
                    linie(Teil.Sprosse, CX - w / 2f, y, CX + w / 2f, y)
                    y += 3.2f
                }
            }
            Dachmodul.Drehleiter -> {
                // Vier ineinander geschobene Leiterteile, Drehkranz, Rettungskorb
                val a = top - 7f
                val e = bot - 6f
                kreis(Teil.Drehkranz, CX, y0 + 6f, 5.2f)
                kreis(Teil.DrehkranzInnen, CX, y0 + 6f, 2f)
                listOf(
                    Triple(6.4f, a, e),
                    Triple(5.2f, a + 3f, e - 4f),
                    Triple(4.0f, a + 6f, e - 9f),
                    Triple(2.8f, a + 9f, e - 14f),
                ).forEach { (w, ya, yb) -> rect(Teil.Leiterteil, CX - w / 2f, ya, w, yb - ya, 0.6f) }
                var y = a + 2f
                while (y < e - 4f) {
                    linie(Teil.Sprosse, CX - 3f, y, CX + 3f, y)
                    y += 3.4f
                }
                rect(Teil.Korb, CX - 4.4f, a - 4.6f, 8.8f, 5f, 1.2f)
                // Abstützungen, im Fahrbetrieb eingefahren
                rect(Teil.Stuetze, lx - 1.4f, y0 + 1f, 1.6f, 4f, 0.6f)
                rect(Teil.Stuetze, rx - 0.2f, y0 + 1f, 1.6f, 4f, 0.6f)
                rect(Teil.Stuetze, lx - 1.4f, bot - 9f, 1.6f, 4f, 0.6f)
                rect(Teil.Stuetze, rx - 0.2f, bot - 9f, 1.6f, 4f, 0.6f)
            }
            Dachmodul.Mast -> {
                // Teleskopmast: kantige Segmente statt Leiter, Korb quer
                val a = top - 5f
                val e = bot - 8f
                kreis(Teil.Drehkranz, CX, y0 + 7f, 5.4f)
                listOf(Triple(7.2f, a + 2f, e), Triple(5.6f, a, e - 6f)).forEach { (w, ya, yb) ->
                    rect(Teil.Leiterteil, CX - w / 2f, ya, w, yb - ya, 1f)
                }
                rect(Teil.Korb, CX - 5.4f, a - 5f, 10.8f, 5.4f, 1.4f)
                rect(Teil.Stuetze, lx - 1.6f, y0 + 2f, 1.8f, 4.4f, 0.6f)
                rect(Teil.Stuetze, rx - 0.2f, y0 + 2f, 1.8f, 4.4f, 0.6f)
            }
            Dachmodul.Kran -> {
                // Ladekran hinter der Kabine, im Fahrbetrieb längs über den Aufbau gelegt
                val dy = y0 + 5f
                val px = CX + 2.2f
                rect(Teil.Stuetze, lx - 1.6f, dy - 2f, 1.8f, 4.4f, 0.6f)
                rect(Teil.Stuetze, rx - 0.2f, dy - 2f, 1.8f, 4.4f, 0.6f)
                kreis(Teil.Drehkranz, px, dy, 4f)
                kreis(Teil.DrehkranzInnen, px, dy, 1.6f)
                rect(Teil.Ausleger, px - 2.1f, dy + 2f, 4.2f, y1 - dy - 6f, 1f)
                rect(Teil.Ausleger2, px - 1.3f, dy + 8f, 2.6f, y1 - dy - 10f, 0.8f)
                kreis(Teil.Haken, px, y1 - 3f, 1.5f)
            }
            Dachmodul.Grosskran -> {
                // Teleskopausleger des Feuerwehrkrans, über das Heck hinaus abgelegt
                val dy = y0 + 12f
                rect(Teil.Stuetze, lx - 2.2f, dy - 5f, 2.4f, 5.4f, 0.7f)
                rect(Teil.Stuetze, rx - 0.2f, dy - 5f, 2.4f, 5.4f, 0.7f)
                rect(Teil.Stuetze, lx - 2.2f, bot - 12f, 2.4f, 5.4f, 0.7f)
                rect(Teil.Stuetze, rx - 0.2f, bot - 12f, 2.4f, 5.4f, 0.7f)
                rect(Teil.Gegengewicht, CX - 6f, dy - 8f, 12f, 7f, 1f)
                kreis(Teil.Drehkranz, CX, dy, 6.4f)
                kreis(Teil.DrehkranzInnen, CX, dy, 2.6f)
                rect(Teil.Ausleger, CX - 3.4f, dy, 6.8f, bot - dy - 2f, 1.2f)
                rect(Teil.Ausleger2, CX - 2.2f, dy + 16f, 4.4f, bot - dy - 4f, 1f)
                rect(Teil.Ausleger2, CX - 1.4f, dy + 30f, 2.8f, max(6f, bot - dy - 16f), 0.8f)
                kreis(Teil.Haken, CX, bot + 4f, 1.8f)
            }
            Dachmodul.Lichtmast -> {
                // Eingefahrener Lichtmast an der linken hinteren Kabinenecke
                rect(Teil.Lichtmast, lx + 1.4f, y0 + 1.4f, 4.2f, 4.2f, 1f)
                rect(Teil.Lichtkopf, lx + 2f, y0 + 2f, 1.3f, 2.8f, 0.4f)
                rect(Teil.Lichtkopf, lx + 3.7f, y0 + 2f, 1.3f, 2.8f, 0.4f)
            }
            Dachmodul.Antennen -> {
                linie(Teil.Antenne, lx + 2.4f, y0 + 2f, lx + 2.4f, y0 + 9f)
                linie(Teil.Antenne, rx - 2.4f, y0 + 3f, rx - 2.4f, y0 + 11f)
                kreis(Teil.Antennenfuss, lx + 2.4f, y0 + 9f, 0.9f)
                kreis(Teil.Antennenfuss, rx - 2.4f, y0 + 11f, 0.9f)
            }
            Dachmodul.Fernmeldemast -> {
                // Satellitenspiegel, eingefahrener Teleskopmast, zwei Antennen
                kreis(Teil.Sat, lx + 6f, y0 + 8f, 5f)
                kreis(Teil.SatInnen, lx + 6f, y0 + 8f, 1.6f)
                linie(Teil.Antenne, lx + 1f, y0 + 8f, lx + 11f, y0 + 8f)
                rect(Teil.Lichtmast, rx - 7f, y0 + 5f, 4.6f, 6.4f, 1f)
                linie(Teil.Antenne, rx - 3f, y0 + 14f, rx - 3f, y0 + 24f)
                kreis(Teil.Antennenfuss, rx - 3f, y0 + 24f, 1f)
                linie(Teil.Antenne, lx + 3f, y0 + 16f, lx + 3f, y0 + 27f)
                kreis(Teil.Antennenfuss, lx + 3f, y0 + 27f, 1f)
            }
            Dachmodul.Werfer -> {
                kreis(Teil.Werfer, CX, y0 + 4f, 3.2f)
                rect(Teil.Rohr, CX - 1f, y0 - 3f, 2f, 7f, 0.9f)
            }
            Dachmodul.Wasserwerfer -> {
                kreis(Teil.Werfer, CX, y0 + 6f, 5f)
                kreis(Teil.DrehkranzInnen, CX, y0 + 6f, 2f)
                rect(Teil.Rohr, CX - 1.4f, y0 - 4f, 2.8f, 10f, 1.2f)
                rect(Teil.Rohr, lx + 2f, top + 1f, 2.2f, 5f, 1f)
                rect(Teil.Rohr, rx - 4.2f, top + 1f, 2.2f, 5f, 1f)
            }
            Dachmodul.Boot -> {
                // Mehrzweckboot auf dem Dach: kürzer als der Aufbau, mit Bug und Konsole
                val laenge = min(34f, (y1 - y0) * 0.72f)
                val a = (y0 + y1) / 2f - laenge / 2f
                val e = a + laenge
                val w = 4.2f
                val rumpf = P()
                    .m(CX, a)
                    .c(CX + w * 0.7f, a + 2f, CX + w, a + 5f, CX + w, a + 8f)
                    .l(CX + w, e - 1.4f)
                    .q(CX + w, e, CX + w - 1.2f, e)
                    .l(CX - w + 1.2f, e)
                    .q(CX - w, e, CX - w, e - 1.4f)
                    .l(CX - w, a + 8f)
                    .c(CX - w, a + 5f, CX - w * 0.7f, a + 2f, CX, a)
                    .z()
                pfad(Teil.Bootrumpf, rumpf)
                rect(Teil.Bootdeck, CX - 2.4f, a + 8f, 4.8f, e - a - 11f, 1f)
                rect(Teil.Motor, CX - 1.6f, e - 1f, 3.2f, 2.6f, 0.9f)
            }
            Dachmodul.Dachstreifen -> {
                // Warnstreifen quer über das Dach des Kofferaufbaus — was von oben vom
                // roten Blockstreifen eines weißen Rettungswagens übrig bleibt.
                val m = (y0 + y1) / 2f
                rect(Teil.Dachstreifen, lx + 0.6f, m - 3.4f, b - 1.2f, 6.8f)
                rect(Teil.Dachkennung, CX - b * 0.22f, m - 1.2f, b * 0.44f, 2.4f, 0.6f)
            }
            Dachmodul.Haspel -> {
                kreis(Teil.Haspel, CX - 4.6f, y1 - 7f, 2.8f)
                kreis(Teil.Haspel, CX + 4.6f, y1 - 7f, 2.8f)
            }
            Dachmodul.Flaschen -> {
                for (i in 0 until 3) {
                    for (j in 0 until 2) kreis(Teil.Flasche, CX - 4f + j * 8f, y0 + 5f + i * 5.5f, 2f)
                }
            }
            Dachmodul.Aggregat -> {
                rect(Teil.Aggregat, CX - 6f, y0 + 3f, 12f, min(20f, y1 - y0 - 6f), 1.2f)
                linie(Teil.Riffel, CX - 4.6f, y0 + 6f, CX + 4.6f, y0 + 6f)
                linie(Teil.Riffel, CX - 4.6f, y0 + 9f, CX + 4.6f, y0 + 9f)
                linie(Teil.Riffel, CX - 4.6f, y0 + 12f, CX + 4.6f, y0 + 12f)
            }
            Dachmodul.Bagger -> {
                // Kettenbagger auf dem Tieflader
                rect(Teil.Kette, lx + 2f, y1 - 26f, 3.4f, 20f, 1.2f)
                rect(Teil.Kette, rx - 5.4f, y1 - 26f, 3.4f, 20f, 1.2f)
                kreis(Teil.Baggerhaus, CX, y1 - 16f, 6.4f)
                rect(Teil.Ausleger, CX - 2f, y1 - 34f, 4f, 18f, 1f)
                rect(Teil.Schaufel, CX - 3.4f, y1 - 38f, 6.8f, 4f)
            }
            Dachmodul.Hundeboxen -> {
                for (i in 0 until 2) {
                    for (j in 0 until 2) rect(Teil.Box, CX - 7f + j * 7.4f, y0 + 3f + i * 8f, 6.6f, 7f, 1f)
                }
            }
            Dachmodul.Zelt -> {
                rect(Teil.Zelt, CX - 7f, y0 + 3f, 14f, 5f, 2.5f)
                rect(Teil.Zelt, CX - 7f, y0 + 10f, 14f, 5f, 2.5f)
                rect(Teil.Zelt, CX - 5f, y0 + 17f, 10f, 4.4f, 2.2f)
            }
            Dachmodul.Seilwinde -> rect(Teil.Winde, CX - 5f, top - 1.6f, 10f, 2.8f, 1.2f)
            Dachmodul.Dachkoffer -> rect(Teil.Dachkoffer, CX - 4.6f, y0 + 1f, 9.2f, min(14f, y1 - y0 - 2f), 3f)
            Dachmodul.Ladebordwand -> rect(Teil.Bordwand, lx + 1.6f, y1 - 0.4f, b - 3.2f, 3.4f, 0.8f)
            Dachmodul.Messtechnik -> {
                rect(Teil.Messkopf, CX - 3f, y0 + 3f, 6f, 4f, 1f)
                linie(Teil.Antenne, CX, y0 + 7f, CX, y0 + 14f)
                kreis(Teil.Antennenfuss, CX, y0 + 14f, 1f)
            }
            Dachmodul.Tauchpumpe -> {
                kreis(Teil.Pumpe, CX, y0 + 6f, 3.6f)
                rect(Teil.Schlauch, CX - 6f, y0 + 12f, 12f, 4f, 2f)
            }
            Dachmodul.Kessel -> {
                // Zwei Kochkessel und der Abzug dazwischen — mit dem Schornstein ist die
                // Feldküche von den Zeltrollen des AB Betreuung zu unterscheiden.
                kreis(Teil.Kessel, CX - 4.4f, y0 + 7f, 3.4f)
                kreis(Teil.Kessel, CX + 4.4f, y0 + 7f, 3.4f)
                kreis(Teil.Kessel, CX - 4.4f, y0 + 15f, 3.4f)
                kreis(Teil.Kessel, CX + 4.4f, y0 + 15f, 3.4f)
                rect(Teil.Schornstein, CX - 1.4f, y0 + 3f, 2.8f, min(16f, y1 - y0 - 6f), 1.2f)
            }
        }
    }

    // ------------------------------------------------------------ Markierung

    /** Konturmarkierung nach DIN 30710: Streifen an den Flanken, Warnschraffur am Heck. */
    private fun markierung(lx: Float, rx: Float, top: Float, bot: Float) {
        val art = bp.markierung ?: Markierungsart.Streifen
        if (art == Markierungsart.Keine) return
        val pkwArt = bp.klasse == Klasse.Pkw

        val laenge = bot - top
        // Am Pkw läuft der Streifen nur über die Türen, am Lkw über den ganzen Aufbau.
        val sa = if (pkwArt) top + laenge * 0.3f else top + 6f
        val se = if (pkwArt) bot - laenge * 0.18f else bot - 6f
        // Die Polizei trägt ein durchgehendes Band, kein schmales Reflexband.
        val dick = if (art == Markierungsart.Band) (if (pkwArt) 2f else 2.6f) else if (pkwArt) 0.9f else 1.1f

        // Auf einem weißen Wagen ist ein weißer Reflexstreifen unsichtbar — dort trägt der
        // Blockstreifen die Kante allein.
        val weisserStreifen = art != Markierungsart.NurHeck && !(art == Markierungsart.Blockstreifen && bp.hell)
        if (weisserStreifen) {
            rect(Teil.Kontur, lx + 0.3f, sa, dick, se - sa, 0.4f)
            rect(Teil.Kontur, rx - 0.3f - dick, sa, dick, se - sa, 0.4f)
        }
        if (art == Markierungsart.Blockstreifen) {
            // Der durchgehende Blockstreifen des deutschen Rettungsdienstes — innen neben der
            // Konturmarkierung, über die ganze Länge des Aufbaus.
            val breit = if (pkwArt) 1.4f else 1.8f
            val versatz = if (weisserStreifen) 0.4f + dick else 0.4f
            rect(Teil.Block, lx + versatz, sa, breit, se - sa)
            rect(Teil.Block, rx - versatz - breit, sa, breit, se - sa)
        }

        // Heckwarnschraffur, am Umriss des Fahrzeugs beschnitten. Streifenwagen und zivile
        // Fahrzeuge tragen keine — die gibt es im Echten nur am Lkw und am RTW.
        if (bp.heckschraffur) {
            val hh = min(9f, laenge * (if (pkwArt) 0.1f else 0.14f))
            val zuschnitt = if (pkwArt) {
                pkwUmriss(lx, rx, top, bot)
            } else {
                val r = bp.radius ?: 3f
                Path().apply {
                    addRoundRect(RoundRect(lx, top, rx, bot, CornerRadius(min(r, (rx - lx) / 2f), min(r, laenge / 2f))))
                }
            }
            d.clipPath(zuschnitt) {
                rect(Teil.SchraffurGrund, lx, bot - hh, rx - lx, hh)
                val w = rx - lx
                for (i in -2 until 7) {
                    val x = lx + i * (w / 5f)
                    pfad(Teil.Schraffur, viereck(x, bot, x + w / 10f, bot, x + w / 10f + hh, bot - hh, x + hh, bot - hh))
                }
            }
        }
        // Rückleuchten
        rect(Teil.Rueckleuchte, lx + 1.2f, bot - 2.6f, 2.6f, 2f, 0.7f)
        rect(Teil.Rueckleuchte, rx - 3.8f, bot - 2.6f, 2.6f, 2f, 0.7f)
    }

    // ------------------------------------------------------------ Blaulicht

    /**
     * Der Lichtschein, den ein Fahrzeug mit Sondersignal auf die Fahrbahn wirft. Wird als
     * Erstes gezeichnet, damit er unter dem Fahrzeug liegt und nicht darauf.
     */
    private fun lichtschein(top: Float, bot: Float, lx: Float, rx: Float) {
        val rxs = (rx - lx) * 1.15f
        val rys = (bot - top) * 0.62f
        val mitte = Offset(CX, (top + bot) / 2f)
        // Der Schein schlägt im Doppeltakt, gleich welche Anlage (`fz-schein` im Web).
        val deckung = 0.28f + 0.72f * an(Blitzmuster.Doppel, 'a')
        d.scale(1f, rys / rxs, pivot = mitte) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to EIGENPOSITION.copy(alpha = 0.5f),
                    0.55f to EIGENPOSITION.copy(alpha = 0.16f),
                    1f to EIGENPOSITION.copy(alpha = 0f),
                    center = mitte,
                    radius = rxs,
                ),
                radius = rxs,
                center = mitte,
                alpha = deckung,
            )
        }
    }

    /**
     * Blaulicht: Balken auf dem Dach, Blitzer im Kühlergrill, Heckblitzer am Lkw.
     *
     * Der Balken hat vier Module — die beiden linken blitzen zusammen, die beiden rechten
     * zusammen —, dazu blitzen die Frontblitzer gegenläufig. Das ergibt kein Hin und Her,
     * sondern das unruhige Flackern einer echten Anlage.
     */
    private fun blaulicht(lx: Float, rx: Float, top: Float, bot: Float, kl: Float) {
        if (!bp.blaulicht) return
        val muster = blitzmusterVon(bp)
        val b = rx - lx
        val pkwArt = bp.klasse == Klasse.Pkw
        val y = if (pkwArt) top + (bot - top) * 0.44f else top + kl - 2.6f
        val bx = lx + 1.6f
        val bw = b - 3.2f

        rect(Teil.Lichtbalken, bx, y - 1.5f, bw, 3.2f, 1.3f)
        halo(muster, 'a', bx + 1.9f, y, 4.4f)
        halo(muster, 'b', bx + bw - 1.9f, y, 4.4f)
        blau(muster, 'a', bx + 1.9f, y, 1.7f)
        blau(muster, 'a', bx + bw * 0.37f, y, 1.3f)
        blau(muster, 'b', bx + bw * 0.63f, y, 1.3f)
        blau(muster, 'b', bx + bw - 1.9f, y, 1.7f)

        // Frontblitzer sitzen in den Scheinwerfern und laufen gegen den Balken.
        val fx1 = if (pkwArt) lx + 2.2f else lx + 1.4f
        val fx2 = if (pkwArt) rx - 5.6f else rx - 4.4f
        val fb = if (pkwArt) 3.4f else 3f
        weissblitz(muster, 'b', fx1, top + 0.7f, fb, 1.8f)
        weissblitz(muster, 'a', fx2, top + 0.7f, fb, 1.8f)

        // Am Lkw und am Transporter kommt das Heck dazu — dort steht man am Einsatzort.
        if (!pkwArt) {
            halo(muster, 'b', lx + 2.4f, bot - 3.4f, 3.4f)
            halo(muster, 'a', rx - 2.4f, bot - 3.4f, 3.4f)
            blau(muster, 'b', lx + 2.4f, bot - 3.4f, 1.5f)
            blau(muster, 'a', rx - 2.4f, bot - 3.4f, 1.5f)
        }
    }

    // ------------------------------------------------------------ Sonderformen

    private fun helikopter(mitBlau: Boolean) {
        // Kufen
        rect(Teil.Kufe, CX - 9f, CY - 12f, 1.8f, 26f, 0.9f)
        rect(Teil.Kufe, CX + 7.2f, CY - 12f, 1.8f, 26f, 0.9f)
        rect(Teil.Kufe, CX - 9f, CY - 9f, 18f, 1.4f, 0.7f)
        rect(Teil.Kufe, CX - 9f, CY + 9f, 18f, 1.4f, 0.7f)
        // Polizeiflieger tragen wie die Streifenwagen das verkehrsblaue Band. Der RTH hat
        // keine Bandmarkierung und bleibt dadurch unverändert gelb.
        if (bp.markierung == Markierungsart.Band) {
            rect(Teil.Kontur, CX - 8f, CY - 1f, 16f, 2.4f, 1.1f)
            rect(Teil.Kontur, CX - 2.1f, CY + 14f, 4.2f, 19f, 1f)
        }
        // Rumpf: vorne rund, hinten in den Heckausleger auslaufend
        val rumpf = P()
            .m(CX, CY - 24f)
            .c(CX + 9f, CY - 24f, CX + 10f, CY - 8f, CX + 8f, CY + 4f)
            .c(CX + 7f, CY + 12f, CX + 3f, CY + 16f, CX, CY + 16f)
            .c(CX - 3f, CY + 16f, CX - 7f, CY + 12f, CX - 8f, CY + 4f)
            .c(CX - 10f, CY - 8f, CX - 9f, CY - 24f, CX, CY - 24f)
            .z()
        pfad(Teil.Lack, rumpf)
        woelbungPfad(rumpf)
        // Heckausleger und Höhenleitwerk
        pfad(Teil.Lack, viereck(CX - 2.8f, CY + 10f, CX + 2.8f, CY + 10f, CX + 1.9f, CY + 37f, CX - 1.9f, CY + 37f))
        rect(Teil.Leitwerk, CX - 7f, CY + 28f, 14f, 2.2f, 1f)
        // Cockpitverglasung
        glasPfad(
            P()
                .m(CX, CY - 22f)
                .c(CX + 7f, CY - 22f, CX + 7.6f, CY - 12f, CX + 6.6f, CY - 7f)
                .l(CX - 6.6f, CY - 7f)
                .c(CX - 7.6f, CY - 12f, CX - 7f, CY - 22f, CX, CY - 22f)
                .z(),
        )
        linie(Teil.HolmFein, CX, CY - 22f, CX, CY - 7f)
        // Schiebetür und Kontur
        rect(Teil.Kontur, CX - 7.6f, CY - 5f, 1.1f, 10f, 0.5f)
        rect(Teil.Kontur, CX + 6.5f, CY - 5f, 1.1f, 10f, 0.5f)
        // Fenestron am Heck, der Heckrotor dreht darin
        kreis(Teil.Fenestron, CX, CY + 40f, 5f)
        val heck = Offset(CX, CY + 40f)
        d.rotate(drehwinkel(160L), pivot = heck) {
            kreis(Teil.HeckrotorNabe, CX, CY + 40f, 1.1f)
            listOf(0f, 45f, 90f, 135f).forEach { w ->
                val bogen = w * PI.toFloat() / 180f
                linie(Teil.Heckblatt, CX, CY + 40f, CX + 4.4f * cos(bogen), CY + 40f + 4.4f * sin(bogen))
            }
        }
        // Hauptrotor: Scheibe plus vier Blätter, die Blätter drehen
        kreis(Teil.Rotorscheibe, CX, CY - 8f, 21f)
        val nabe = Offset(CX, CY - 8f)
        d.rotate(drehwinkel(420L), pivot = nabe) {
            listOf(0f, 90f, 180f, 270f).forEach { w ->
                rotate(w, pivot = nabe) { rect(Teil.Blatt, CX - 1.1f, CY - 8f, 2.2f, 21f, 1f) }
            }
            kreis(Teil.Rotorkopf, CX, CY - 8f, 2.6f)
        }
        if (mitBlau) {
            val muster = blitzmusterVon(bp)
            halo(muster, 'a', CX - 7f, CY - 3f, 4f)
            halo(muster, 'b', CX + 7f, CY - 3f, 4f)
            blau(muster, 'a', CX - 7f, CY - 3f, 1.6f)
            blau(muster, 'b', CX + 7f, CY - 3f, 1.6f)
        }
    }

    /** Der Winkel eines Rotors, der in `umlauf` Millisekunden einmal herumkommt. */
    private fun drehwinkel(umlauf: Long): Float {
        val j = jetzt ?: return 0f
        return Math.floorMod(j, umlauf).toFloat() / umlauf * 360f
    }

    /**
     * Zugfahrzeug und Anhänger als eine einsatzbereite Einheit. Das Spiel disponiert
     * Kombinationen, nicht lose Anhänger; die Zeichnung zeigt deshalb beides in einem Riss
     * und hält trotzdem Kupplung, eigene Achse und Ladung klar auseinander.
     */
    private fun gespann(mitBlau: Boolean) {
        val top = CY - bp.laenge / 2f
        val bot = CY + bp.laenge / 2f
        val lx = CX - bp.breite / 2f
        val rx = CX + bp.breite / 2f
        val zugEnde = top + 48f
        val haengerTop = zugEnde + 7f
        val haengerBot = bot

        if (bp.traktor) {
            // Der Schlepper: Die Hinterräder sind mehr als doppelt so breit wie die vorderen
            // und stehen weit heraus, und die Kabine ist ein schmaler Kasten dazwischen.
            achse(top + 9f, lx, rx, 2.4f, 6f, false)
            achse(zugEnde - 12f, lx - 1.6f, rx + 1.6f, 5.4f, 12f, false)
            karosserie(lx + 2f, top, bp.breite - 4f, 48f, 4f)
            kabine(lx + 2f, rx - 2f, top, 17f)
            // Das Auspuffrohr an der rechten Kabinensäule — die eine Kleinigkeit, die den
            // Schlepper auch dann verrät, wenn die Räder in 42 px zusammenlaufen.
            rect(Teil.Auspuff, rx - 5.5f, top + 17f, 1.8f, 7f, 0.9f)
        } else {
            // Kompaktes MZF als Zugfahrzeug — dieselben Bausteine wie jeder Transporter.
            val zug = bp.copy(form = Form.Strasse, laenge = 48f, aufbau = Aufbauart.Bus, klasse = Klasse.Transporter)
            raeder(zug, top, 48f, lx, rx)
            karosserie(lx, top, bp.breite, 48f, 4f)
            Riss(d, zug, jetzt, massstab).aufbau(lx, rx, top + 15f, zugEnde - 1f)
            kabine(lx, rx, top, 15f)
            rect(Teil.Kontur, lx + 1f, top + 31f, bp.breite - 2f, 1.4f)
        }

        // Deichsel und eine eigene Achse machen die Trennung auch in 42 px Kartenhöhe lesbar.
        pfad(
            Teil.Deichsel,
            P().m(CX, zugEnde - 1f).l(CX - 4f, haengerTop + 2f).m(CX, zugEnde - 1f).l(CX + 4f, haengerTop + 2f).offen(),
        )
        achse(haengerBot - 10f, lx, rx, 3f, 7.5f, false)
        rect(Teil.Anhaenger, lx + 0.8f, haengerTop, bp.breite - 1.6f, haengerBot - haengerTop, 2f)

        when (bp.anhaenger) {
            Anhaengerart.Boot -> {
                val rumpf = P()
                    .m(CX, haengerTop + 2f)
                    .c(CX + 6f, haengerTop + 7f, rx - 2.5f, haengerTop + 13f, rx - 2.5f, haengerBot - 3f)
                    .l(lx + 2.5f, haengerBot - 3f)
                    .c(lx + 2.5f, haengerTop + 13f, CX - 6f, haengerTop + 7f, CX, haengerTop + 2f)
                    .z()
                pfad(Teil.Bootrumpf, rumpf)
                rect(Teil.Bootdeck, lx + 5f, haengerTop + 13f, bp.breite - 10f, haengerBot - haengerTop - 19f, 2f)
            }
            Anhaengerart.Strom -> {
                rect(Teil.Aggregat, lx + 3f, haengerTop + 4f, bp.breite - 6f, haengerBot - haengerTop - 9f, 1.4f)
                rect(Teil.Lichtmast, CX - 3f, haengerTop + 7f, 6f, 8f, 1f)
                rect(Teil.Lichtkopf, CX - 2f, haengerTop + 8f, 1.5f, 5.5f, 0.4f)
                rect(Teil.Lichtkopf, CX + 0.5f, haengerTop + 8f, 1.5f, 5.5f, 0.4f)
            }
            Anhaengerart.Wassertank -> {
                // Das liegende Fass mit seinen Spannbändern und der Pumpe am Heck. Die
                // Rundung unterscheidet es vom eckigen Gerätekasten der anderen Anhänger.
                val fassTop = haengerTop + 3f
                val fassBot = haengerBot - 7f
                val fassBreite = bp.breite - 5.2f
                rect(Teil.Tankfass, lx + 2.6f, fassTop, fassBreite, fassBot - fassTop, fassBreite / 2f)
                val drittel = (fassBot - fassTop) / 3f
                linie(Teil.Fuge, lx + 2.6f, fassTop + drittel, rx - 2.6f, fassTop + drittel)
                linie(Teil.Fuge, lx + 2.6f, fassTop + drittel * 2f, rx - 2.6f, fassTop + drittel * 2f)
                kreis(Teil.Domdeckel, CX, fassTop + 5f, 2.4f)
                kreis(Teil.Pumpe, CX, haengerBot - 4f, 2.8f)
            }
            Anhaengerart.Kueche -> {
                // Der Feldkochherd: zwei Kessel über dem Brenner, der Abzug in der Mitte.
                rect(Teil.Box, lx + 3f, haengerTop + 4f, bp.breite - 6f, haengerBot - haengerTop - 9f, 1.4f)
                kreis(Teil.Kessel, CX - 4.2f, haengerTop + 11f, 3.2f)
                kreis(Teil.Kessel, CX + 4.2f, haengerTop + 11f, 3.2f)
                rect(Teil.Schornstein, CX - 1.3f, haengerTop + 16f, 2.6f, haengerBot - haengerTop - 22f, 1.2f)
            }
            Anhaengerart.Gefahrgut, null -> {
                rect(Teil.Box, lx + 3f, haengerTop + 4f, bp.breite - 6f, haengerBot - haengerTop - 9f, 1.4f)
                kreis(Teil.Flasche, CX - 4f, haengerTop + 10f, 2.2f)
                kreis(Teil.Flasche, CX + 4f, haengerTop + 10f, 2.2f)
                kreis(Teil.Pumpe, CX, haengerBot - 9f, 3f)
            }
        }

        // Die Kennleuchte sitzt auf der Kabine — beim Schlepper auf der schmalen, deshalb
        // dieselben Ränder, mit denen sie oben gezeichnet wurde.
        if (mitBlau) {
            val kabinenrand = if (bp.traktor) 2f else 0f
            blaulicht(lx + kabinenrand, rx - kabinenrand, top, zugEnde, if (bp.traktor) 17f else 15f)
        }
    }

    private fun boot(mitBlau: Boolean) {
        val laenge = bp.laenge
        val breite = bp.breite
        val top = CY - laenge / 2f
        val bot = CY + laenge / 2f
        val lx = CX - breite / 2f
        val rx = CX + breite / 2f
        // Bugwelle — zwei Wellen, die nacheinander vom Bug weglaufen.
        bugwelle(0L, P().m(CX - 9f, top + 5f).q(CX, top - 5f, CX + 9f, top + 5f).offen(), top)
        bugwelle(800L, P().m(CX - 13f, top + 11f).q(CX, top - 2f, CX + 13f, top + 11f).offen(), top)
        // Rumpf mit spitzem Bug
        fun rumpf() = P()
            .m(CX, top)
            .c(CX + breite * 0.3f, top + laenge * 0.1f, rx, top + laenge * 0.24f, rx, top + laenge * 0.4f)
            .l(rx, bot - 2f)
            .q(rx, bot, rx - 2f, bot)
            .l(lx + 2f, bot)
            .q(lx, bot, lx, bot - 2f)
            .l(lx, top + laenge * 0.4f)
            .c(lx, top + laenge * 0.24f, CX - breite * 0.3f, top + laenge * 0.1f, CX, top)
            .z()
        val umriss = rumpf()
        pfad(Teil.Lack, umriss)
        woelbungPfad(umriss)
        // Scheuerleiste, Decksfläche, Steuerstand
        pfad(Teil.Scheuerleiste, umriss)
        rect(Teil.Deck, lx + 2.4f, top + laenge * 0.34f, breite - 4.8f, laenge * 0.56f, 2f)
        if (bp.kajuete) {
            rect(Teil.Kajuete, lx + 3.4f, top + laenge * 0.3f, breite - 6.8f, laenge * 0.34f, 3f)
            glasPfad(
                viereck(
                    lx + 4.6f, top + laenge * 0.36f, rx - 4.6f, top + laenge * 0.36f,
                    rx - 5.4f, top + laenge * 0.31f, lx + 5.4f, top + laenge * 0.31f,
                ),
            )
            linie(Teil.Antenne, CX, top + laenge * 0.3f, CX, top + laenge * 0.18f)
            kreis(Teil.Sat, CX, top + laenge * 0.22f, 2.6f)
        } else {
            rect(Teil.Konsole, CX - 3.4f, top + laenge * 0.42f, 6.8f, laenge * 0.12f, 1.4f)
            kreis(Teil.Sitz, CX - 3.6f, top + laenge * 0.62f, 2.2f)
            kreis(Teil.Sitz, CX + 3.6f, top + laenge * 0.62f, 2.2f)
        }
        if (bp.tochterboot) {
            pfad(
                Teil.Tochterboot,
                P().m(CX, bot - 22f).l(CX + 5f, bot - 15f).l(CX + 5f, bot - 3f).l(CX - 5f, bot - 3f).l(CX - 5f, bot - 15f).z(),
            )
        } else {
            rect(Teil.Motor, CX - 3f, bot - 1.5f, 6f, 5f, 1.4f)
        }
        // Blaulicht auf dem Steuerstand — nur bei Sondersignal
        if (mitBlau) {
            val muster = blitzmusterVon(bp)
            val y = top + laenge * 0.36f
            halo(muster, 'a', lx + 3.4f, y, 3.8f)
            halo(muster, 'b', rx - 3.4f, y, 3.8f)
            blau(muster, 'a', lx + 3.4f, y, 1.8f)
            blau(muster, 'b', rx - 3.4f, y, 1.8f)
        }
    }

    /**
     * Eine Bugwelle (`fz-welle`, 1,6 s): taucht auf, läuft ein Stück nach vorn und wird
     * dabei breiter, dann ist sie weg. Ohne Uhr gibt es keine Welle — wie im Web vor dem
     * ersten Bild.
     */
    private fun bugwelle(versatz: Long, welle: Path, bug: Float) {
        val j = jetzt ?: return
        val p = Math.floorMod(j - versatz, 1_600L).toFloat() / 1_600f
        val deckung = if (p < 0.35f) 0.8f * (p / 0.35f) else 0.8f * (1f - (p - 0.35f) / 0.65f)
        if (deckung <= 0f) return
        val lauf = 1f - (1f - p) * (1f - p) // ease-out
        val versatzY = 4f + (-7f - 4f) * lauf
        val groesse = 0.85f + (1.15f - 0.85f) * lauf
        d.translate(0f, versatzY) {
            scale(groesse, groesse, pivot = Offset(CX, bug)) { pfad(Teil.Welle, welle, deckung) }
        }
    }

    private fun kraftrad(mitBlau: Boolean) {
        val top = CY - bp.laenge / 2f
        val bot = CY + bp.laenge / 2f
        // Von oben ist ein Krad vor allem schmal: Vorderrad, Lenker mit Spiegeln, Tank,
        // Sitzbank, zwei Koffer mit den Blitzern darauf.
        val verkleidung = P()
            .m(CX, top + 1.5f)
            .c(CX + 2.4f, top + 2f, CX + 2.6f, top + 5f, CX + 2.4f, top + 8f)
            .l(CX - 2.4f, top + 8f)
            .c(CX - 2.6f, top + 5f, CX - 2.4f, top + 2f, CX, top + 1.5f)
            .z()
        rect(Teil.Rad, CX - 1.1f, top + 2.5f, 2.2f, 5f, 1f)
        rect(Teil.Rad, CX - 1.3f, bot - 6f, 2.6f, 5.4f, 1.2f)
        rect(Teil.Lenker, CX - 4f, top + 6.6f, 8f, 1.1f, 0.5f)
        rect(Teil.Spiegel, CX - 5.2f, top + 4.6f, 1.8f, 1.5f, 0.6f)
        rect(Teil.Spiegel, CX + 3.4f, top + 4.6f, 1.8f, 1.5f, 0.6f)
        pfad(Teil.Lack, verkleidung)
        woelbungPfad(verkleidung)
        rect(Teil.Licht, CX - 1.2f, top + 1.8f, 2.4f, 1.4f, 0.6f)
        rect(Teil.Lack, CX - 2.2f, top + 8f, 4.4f, 4.6f, 1.6f)
        rect(Teil.SitzBank, CX - 1.9f, top + 12f, 3.8f, 5.2f, 1.4f)
        rect(Teil.Seitenkoffer, CX - 4.6f, bot - 7.5f, 3.4f, 5f, 0.9f)
        rect(Teil.Seitenkoffer, CX + 1.2f, bot - 7.5f, 3.4f, 5f, 0.9f)
        rect(Teil.Kontur, CX - 4.4f, bot - 6.4f, 3f, 0.9f)
        rect(Teil.Kontur, CX + 1.4f, bot - 6.4f, 3f, 0.9f)
        if (mitBlau) {
            val muster = blitzmusterVon(bp)
            blau(muster, 'a', CX - 2.9f, bot - 4.4f, 1.2f)
            blau(muster, 'b', CX + 2.9f, bot - 4.4f, 1.2f)
            halo(muster, 'a', CX - 2.9f, bot - 4.4f, 2.6f)
            halo(muster, 'b', CX + 2.9f, bot - 4.4f, 2.6f)
        }
    }

    private fun quad(mitBlau: Boolean) {
        val laenge = bp.laenge
        val breite = bp.breite
        val top = CY - laenge / 2f
        val bot = CY + laenge / 2f
        val lx = CX - breite / 2f
        val rx = CX + breite / 2f
        // Kotflügel über den vier Rädern, dazwischen ein schmaler Rahmen
        val rumpf = P()
            .m(CX, top)
            .c(CX + 4f, top, CX + 4.6f, top + 4f, CX + 4.2f, top + 8f)
            .l(CX + 4.2f, bot - 8f)
            .c(CX + 4.6f, bot - 4f, CX + 4f, bot - 1f, CX, bot - 1f)
            .c(CX - 4f, bot - 1f, CX - 4.6f, bot - 4f, CX - 4.2f, bot - 8f)
            .l(CX - 4.2f, top + 8f)
            .c(CX - 4.6f, top + 4f, CX - 4f, top, CX, top)
            .z()
        rect(Teil.Rad, lx - 0.4f, top + 1.5f, 4.4f, 7f, 2f)
        rect(Teil.Rad, rx - 4f, top + 1.5f, 4.4f, 7f, 2f)
        rect(Teil.Rad, lx - 0.8f, bot - 9.5f, 4.8f, 7.4f, 2.2f)
        rect(Teil.Rad, rx - 4f, bot - 9.5f, 4.8f, 7.4f, 2.2f)
        pfad(Teil.Lack, rumpf)
        woelbungPfad(rumpf)
        rect(Teil.Lenker, CX - 4f, top + 6f, 8f, 1.1f, 0.5f)
        rect(Teil.SitzBank, CX - 2.4f, CY - 3f, 4.8f, 6.5f, 1.8f)
        rect(Teil.Rettungsbrett, CX - 4.4f, bot - 9f, 8.8f, 6f, 1.2f)
        if (mitBlau) {
            // Die Leuchten des Quads stehen im Web außerhalb jeder Blitzergruppe und bleiben
            // deshalb ruhig auf ihrem Grundwert — hier genauso (Muster `null`).
            blau(null, 'a', CX - 3.4f, top + 3.4f, 1.4f)
            blau(null, 'b', CX + 3.4f, top + 3.4f, 1.4f)
            halo(null, 'a', CX - 3.4f, top + 3.4f, 3f)
            halo(null, 'b', CX + 3.4f, top + 3.4f, 3f)
        }
    }
}
