package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Fwbereich
import de.pagerspass.pagerspass.netz.Fwlage
import de.pagerspass.pagerspass.netz.Fwtrupp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Das Lagebild der FwSim: die Einsatzstelle als Zeichnung, an der man arbeitet —
 * übertragen aus `FwLagebild.vue` (Web v6).
 *
 * <b>Warum gezeichnet und nicht als Liste.</b> Eine Liste von Bereichen mit Balken
 * ist ein Formular; ein Haus, aus dessen Fenster es qualmt, mit einem Trupp im ersten
 * Stock und einer Person, die am Dachfenster winkt, ist eine Lage. Man sieht, wo es
 * brennt, wohin der Rauch zieht und wer wo steht — und tippt den Bereich an, in den
 * man jemanden schickt.
 *
 * <b>Eine Zeichnung je Lageart</b>: Haus im Schnitt, Fahrzeug von der Seite, Fläche
 * von oben mit Wind, Unfallwrack mit dem, was schon ab ist, Gefahrstoff mit Lache und
 * Absperrring, Hilfeleistung als Ablauf. Was nicht erkundet ist, steht als
 * Fragezeichen.
 *
 * <b>Die Maße sind die des Web-SVG</b> (360 Einheiten breit) und werden auf die
 * Breite des Kastens gestreckt — wer die Zeichnung dort ändert, findet hier dieselben
 * Zahlen. Angetippt wird über dieselben Flächen: Jeder Bereich trägt eine
 * Trefferprüfung in Zeichnungseinheiten, die jüngste (oberste) gewinnt.
 */
@Composable
fun FwLagebild(
    lage: Fwlage,
    gewaehlt: String?,
    meinFahrzeugId: String?,
    beiWahl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rahmen = modifier
        .fillMaxWidth()
        .background(Farben.BgTief, Rundung.Klein)
        .border(1.dp, Farben.Rand, Rundung.Klein)

    if (lage.art == "Hilfeleistung") {
        Hilfeablauf(lage, rahmen)
        return
    }

    val bild = remember(lage) { lagebildVon(lage) }
    val messer = rememberTextMeasurer(cacheSize = 48)
    // Eine Uhr für alles, was sich bewegt — Flackern, Lodern, Rauch, Winken. Sie läuft
    // gleichmäßig, jede Bewegung rechnet sich ihre Phase daraus.
    val uhr by rememberInfiniteTransition(label = "lagebild").animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "uhr",
    )

    Box(rahmen.padding(1.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .aspectRatio(360f / bild.hoehe)
                .semantics { contentDescription = "Lagebild: ${lage.titel}" }
                .pointerInput(bild) {
                    detectTapGestures { p ->
                        val s = size.width / 360f
                        val punkt = Offset(p.x / s, p.y / s)
                        bild.treffer.lastOrNull { it.second(punkt) }?.let { beiWahl(it.first) }
                    }
                },
        ) {
            val z = Zeichner(this, size.width / 360f, uhr, messer)
            when (lage.art) {
                "Gebaeudebrand" -> z.gebaeude(lage, bild, gewaehlt, meinFahrzeugId)
                "Fahrzeugbrand" -> z.fahrzeug(lage, gewaehlt, meinFahrzeugId)
                "Vegetationsbrand" -> z.flaeche(lage, gewaehlt, meinFahrzeugId)
                "Verkehrsunfall" -> z.unfall(lage, gewaehlt, meinFahrzeugId)
                "Gefahrgut" -> z.gefahrgut(lage, bild, gewaehlt, meinFahrzeugId)
            }
        }
    }
}

// --------------------------------------------------------------- Geometrie

private const val GESCHOSS = 54f
private const val HAUS_X = 120f
private const val HAUS_B = 200f

/** Was aus der Lage für Zeichnung und Antippen folgt — einmal je Stand gerechnet. */
private class Lagebildmass(
    val hoehe: Float,
    val boden: Float = 0f,
    val oben: Int = 0,
    val geschosse: List<Fwbereich> = emptyList(),
    val rahmen: Map<String, Rect> = emptyMap(),
    val treffer: List<Pair<String, (Offset) -> Boolean>> = emptyList(),
    val lacheR: Float = 0f,
    val sperrR: Float? = null,
)

private fun lagebildVon(lage: Fwlage): Lagebildmass = when (lage.art) {
    "Gebaeudebrand" -> {
        val geschosse = lage.bereiche.filter { it.art == "Geschoss" }.sortedByDescending { it.ebene }
        val oben = geschosse.filter { it.ebene >= 0 }.map { it.ebene }.distinct().size
        val unten = geschosse.count { it.ebene < 0 }
        val dach = geschosse.any { it.id == "dg" }
        val boden = 30f + oben * GESCHOSS + if (dach) 10f else 0f
        val gewerbe = lage.variante == "gewerbe"
        val nebeneinander = geschosse.filter { it.ebene == 0 }
        val rahmen = geschosse.associate { b ->
            b.id to if (gewerbe && b.ebene == 0) {
                // Halle, Lager, Büro: nebeneinander im Erdgeschoss statt übereinander.
                val i = nebeneinander.indexOfFirst { it.id == b.id }
                val breite = HAUS_B / nebeneinander.size
                Rect(HAUS_X + i * breite, boden - GESCHOSS, HAUS_X + (i + 1) * breite, boden)
            } else {
                // Das Dachgeschoss ist das Dreieck — als Rahmen zählt das Rechteck darunter.
                val y = boden - (b.ebene + 1) * GESCHOSS - if (b.id == "dg") 10f else 0f
                Rect(HAUS_X, y, HAUS_X + HAUS_B, y + if (b.id == "dg") GESCHOSS + 10f else GESCHOSS)
            }
        }
        Lagebildmass(
            hoehe = boden + unten * GESCHOSS + 26f,
            boden = boden,
            oben = oben,
            geschosse = geschosse,
            rahmen = rahmen,
            // Der Name steht links neben dem Geschoss — auch er nimmt den Finger an.
            treffer = geschosse.map { b ->
                val r = rahmen.getValue(b.id)
                b.id to { p: Offset -> Rect(r.left - 70f, r.top, r.right + 10f, r.bottom).contains(p) }
            },
        )
    }

    "Fahrzeugbrand" -> Lagebildmass(
        hoehe = 170f,
        treffer = if (lage.variante == "klein") {
            lage.bereiche.map { b -> b.id to { p: Offset -> Rect(114f, 70f, 246f, 138f).contains(p) } }
        } else {
            lage.bereiche.mapNotNull { b ->
                val r = when (b.id) {
                    "motor" -> Rect(248f, 86f, 310f, 120f)
                    "innen" -> Rect(124f, 60f, 262f, 116f)
                    "akku" -> Rect(120f, 118f, 240f, 158f)
                    else -> null
                } ?: return@mapNotNull null
                b.id to { p: Offset -> r.contains(p) }
            }
        },
    )

    "Vegetationsbrand" -> Lagebildmass(
        hoehe = 240f,
        treffer = lage.bereiche.filter { it.art == "Flaeche" }.map { b ->
            val r = sektorRadius(lage, b) + 14f
            val mitte = ((SEKTOR[b.id] ?: 0f) + lage.wind.toFloat())
            b.id to { p: Offset ->
                val dx = p.x - 180f
                val dy = p.y - 120f
                // Der Winkel im Bild, von oben im Uhrzeigersinn — wie `SEKTOR`.
                val w = ((atan2(dy, dx) * 180f / PI.toFloat()) + 90f + 360f) % 360f
                val abstand = ((w - mitte) % 360f + 540f) % 360f - 180f
                hypot(dx, dy) <= r && kotlin.math.abs(abstand) <= 45f
            }
        },
    )

    "Verkehrsunfall" -> Lagebildmass(
        hoehe = 190f,
        treffer = listOf<Pair<String, (Offset) -> Boolean>>("wrack" to { p -> Rect(100f, 68f, 314f, 148f).contains(p) }) +
            lage.bereiche.filter { it.id == "motor" }.map { b -> b.id to { p: Offset -> Rect(268f, 94f, 316f, 132f).contains(p) } },
    )

    "Gefahrgut" -> {
        val st = lage.stoff
        val lache = 18f + (st?.ausbreitung ?: 0.0).toFloat() * 0.9f
        Lagebildmass(
            hoehe = 240f,
            lacheR = lache,
            // Die Zeichnung kennt keinen Maßstab — 300 m sind der Rand.
            sperrR = st?.absperrung?.takeIf { it > 0 }?.let { 40f + (it / 300f).toFloat() * 70f },
            treffer = listOf("gefahr" to { p -> hypot(p.x - 160f, p.y - 120f) <= maxOf(lache, 46f) }),
        )
    }

    else -> Lagebildmass(hoehe = 200f)
}

/** Die vier Abschnitte der Fläche als Segmente um die Mitte, gedreht nach dem Wind. */
private val SEKTOR = mapOf("kopf" to 0f, "rechts" to 90f, "ruecken" to 180f, "links" to 270f)

private fun sektorRadius(lage: Fwlage, b: Fwbereich): Float =
    46f + (b.feuer ?: 0.0).toFloat() * 0.42f + minOf(30f, lage.hektar.toFloat() * 4f)

/** 0 … 1, für Deckkraft. Unbekanntes Feuer zeigt sich nicht — dort steht ein Fragezeichen. */
private fun glut(b: Fwbereich): Float = ((b.feuer ?: 0.0) / 100).toFloat().coerceIn(0f, 1f)

private fun qualm(b: Fwbereich): Float = minOf(0.85f, (b.rauch / 100).toFloat() * 0.85f).coerceAtLeast(0f)

private val TRUPP_DRAUSSEN = setOf("Vorgehen", "Einsatz", "Rueckzug", "Notfall")

// ---------------------------------------------------------------- Zeichnen

/**
 * Der Stift — rechnet in Zeichnungseinheiten und streckt auf die Fläche.
 *
 * `uhr` läuft in Sekunden; jede Bewegung nimmt ihre eigene Periode daraus, so wie im
 * Web jede Animation ihre eigene Dauer hat.
 */
private class Zeichner(
    val d: DrawScope,
    val s: Float,
    val uhr: Float,
    val messer: androidx.compose.ui.text.TextMeasurer,
) {
    fun p(x: Float, y: Float) = Offset(x * s, y * s)
    fun r(x: Float, y: Float, b: Float, h: Float) = Pair(p(x, y), Size(b * s, h * s))

    /** Hin und her zwischen 0 und 1 — `alternate` im Web. */
    fun welle(periode: Float, versatz: Float = 0f): Float =
        (0.5f + 0.5f * sin(((uhr + versatz) / periode) * PI.toFloat())).coerceIn(0f, 1f)

    /** Der Flackerfaktor der Glut (Helligkeit 0,85 … 1,25 im Web). */
    fun flackern(periode: Float = 1.4f) = 0.78f + 0.22f * welle(periode)

    /** Blinkt hart — `steps(2)`. */
    fun blink(periode: Float) = if (((uhr / periode) % 1f) < 0.5f) 1f else 0.45f

    fun pulsen(periode: Float) = 1f - 0.55f * welle(periode)

    fun rechteck(farbe: Color, x: Float, y: Float, b: Float, h: Float, alpha: Float = 1f) {
        val (o, g) = r(x, y, b, h)
        d.drawRect(farbe, o, g, alpha = alpha.coerceIn(0f, 1f))
    }

    fun rand(farbe: Color, x: Float, y: Float, b: Float, h: Float, staerke: Float, strich: PathEffect? = null) {
        val (o, g) = r(x, y, b, h)
        d.drawRect(farbe, o, g, style = Stroke(staerke * s, pathEffect = strich))
    }

    fun gerundet(farbe: Color, x: Float, y: Float, b: Float, h: Float, ecke: Float, alpha: Float = 1f, stroke: Float? = null) {
        val (o, g) = r(x, y, b, h)
        d.drawRoundRect(
            farbe, o, g,
            androidx.compose.ui.geometry.CornerRadius(ecke * s),
            alpha = alpha.coerceIn(0f, 1f),
            style = stroke?.let { Stroke(it * s) } ?: androidx.compose.ui.graphics.drawscope.Fill,
        )
    }

    fun kreis(farbe: Color, x: Float, y: Float, radius: Float, alpha: Float = 1f, stroke: Float? = null, strich: PathEffect? = null) {
        d.drawCircle(
            farbe, radius * s, p(x, y),
            alpha = alpha.coerceIn(0f, 1f),
            style = stroke?.let { Stroke(it * s, pathEffect = strich) } ?: androidx.compose.ui.graphics.drawscope.Fill,
        )
    }

    fun linie(farbe: Color, x1: Float, y1: Float, x2: Float, y2: Float, staerke: Float, strich: PathEffect? = null, alpha: Float = 1f) {
        d.drawLine(farbe, p(x1, y1), p(x2, y2), staerke * s, cap = StrokeCap.Round, pathEffect = strich, alpha = alpha)
    }

    fun gestrichelt(an: Float, aus: Float, phase: Float = 0f) = PathEffect.dashPathEffect(floatArrayOf(an * s, aus * s), phase * s)

    fun pfad(vararg punkte: Float, schliessen: Boolean = true): Path = Path().apply {
        moveTo(punkte[0] * s, punkte[1] * s)
        var i = 2
        while (i < punkte.size) {
            lineTo(punkte[i] * s, punkte[i + 1] * s)
            i += 2
        }
        if (schliessen) close()
    }

    fun flaeche(pfad: Path, farbe: Color, alpha: Float = 1f) = d.drawPath(pfad, farbe, alpha = alpha.coerceIn(0f, 1f))
    fun flaeche(pfad: Path, pinsel: Brush, alpha: Float = 1f) = d.drawPath(pfad, pinsel, alpha = alpha.coerceIn(0f, 1f))
    fun umriss(pfad: Path, farbe: Color, staerke: Float, strich: PathEffect? = null) =
        d.drawPath(pfad, farbe, style = Stroke(staerke * s, pathEffect = strich))

    /** Text in Zeichnungseinheiten — `anker` wie `text-anchor`: links, mittig, rechts. */
    fun text(
        inhalt: String,
        x: Float,
        y: Float,
        farbe: Color = Farben.TextLeise,
        groesse: Float = 9.5f,
        fett: Boolean = false,
        anker: TextAlign = TextAlign.Start,
        mono: Boolean = true,
    ) {
        val stil = TextStyle(
            color = farbe,
            fontSize = with(d) { (groesse * s).toSp() },
            fontWeight = if (fett) FontWeight.Bold else FontWeight.Normal,
            fontFamily = if (mono) Schrift.Mono else Schrift.Sans,
        )
        val satz = messer.measure(inhalt, stil)
        val links = when (anker) {
            TextAlign.Center -> x * s - satz.size.width / 2f
            TextAlign.End -> x * s - satz.size.width
            else -> x * s
        }
        // `y` ist die Grundlinie, wie im SVG.
        d.drawText(satz, topLeft = Offset(links, y * s - satz.firstBaseline))
    }

    /** Der Verlauf des Feuers: unten Signal, darüber Amber, oben verglüht. */
    fun feuerpinsel(oben: Float, unten: Float, mitte: Float = 0.6f) = Brush.verticalGradient(
        0f to Farben.Amber.copy(alpha = 0f),
        (1f - mitte) to Farben.Amber,
        1f to Farben.Signal,
        startY = oben * s,
        endY = unten * s,
    )

    // ------------------------------------------------------------- Trupps

    fun trupp(t: Fwtrupp, x: Float, y: Float, mein: String?) {
        val not = t.status == "Notfall"
        val weg = t.status == "Rueckzug" || t.status == "Vorgehen"
        val alpha = if (weg) 0.6f else 1f
        val puls = if (not) pulsen(0.5f) else 1f
        kreis(if (not) Farben.Signal else Farben.FlaecheHoch, x, y, 11f, alpha * puls)
        kreis(
            when {
                not -> Farben.SignalHell
                t.vehicleId == mein -> Farben.Amber
                else -> Farben.TextLeise
            },
            x, y, 11f, alpha * puls,
            stroke = if (t.vehicleId == mein) 2.2f else 1.5f,
        )
        text(t.kurz, x, y + 3.5f, Farben.Text, groesse = 8.5f, fett = true, anker = TextAlign.Center)
    }

    fun truppsIn(lage: Fwlage, id: String) =
        lage.trupps.filter { it.bereichId == id && it.status in TRUPP_DRAUSSEN }

    fun auswahlrand(an: Boolean) = if (an) Farben.Amber to 2.6f else Farben.RandHell to 1.2f

    // -------------------------------------------------------------- Gebäude

    fun gebaeude(lage: Fwlage, m: Lagebildmass, gewaehlt: String?, mein: String?) {
        val boden = m.boden
        val hoehe = m.hoehe
        // Erdreich und Straße
        rechteck(Farben.FlaecheTief, 0f, boden, 360f, hoehe - boden)
        linie(Farben.RandHell, 0f, boden, 360f, boden, 1.5f)

        // Hydrant und Leitung — das Wasser fließt.
        if (lage.versorgung != "Tank") {
            gerundet(Farben.Signal, 8f, boden - 22f, 10f, 22f, 2f)
            val schlauch = Path().apply {
                moveTo(18f * s, (boden - 6f) * s)
                cubicTo(50f * s, (boden - 2f) * s, 70f * s, (boden - 2f) * s, 96f * s, (boden - 8f) * s)
            }
            umriss(schlauch, Farben.BlauHell, 3f, gestrichelt(8f, 4f, -(uhr / 0.8f % 1f) * 12f))
        }

        // Das Löschfahrzeug am Haus
        val fy = boden - 24f
        gerundet(Farben.SignalTief, 56f, fy + 4f, 48f, 18f, 3f)
        gerundet(Farben.Signal, 56f, fy + 4f, 48f, 18f, 3f, stroke = 1f)
        gerundet(Farben.Signal, 90f, fy, 14f, 12f, 2f)
        kreis(Farben.BgTief, 66f, fy + 23f, 4f)
        kreis(Farben.TextSehrLeise, 66f, fy + 23f, 4f, stroke = 2f)
        kreis(Farben.BgTief, 94f, fy + 23f, 4f)
        kreis(Farben.TextSehrLeise, 94f, fy + 23f, 4f, stroke = 2f)
        rechteck(Farben.BlauHell, 92f, fy - 3f, 8f, 3f, blink(0.35f))

        // Die Drehleiter, wenn sie steht
        if (lage.fahrzeuge.any { "dl_stellung" in it.erledigt }) {
            val spitze = boden - m.oben * GESCHOSS
            linie(Farben.TextLeise, HAUS_X + HAUS_B + 34f, boden - 8f, HAUS_X + HAUS_B - 6f, spitze + 6f, 2.5f)
            linie(Farben.TextLeise, HAUS_X + HAUS_B + 40f, boden - 8f, HAUS_X + HAUS_B, spitze + 8f, 2.5f)
            gerundet(Farben.SignalTief, HAUS_X + HAUS_B + 20f, boden - 12f, 34f, 12f, 2f)
            gerundet(Farben.Signal, HAUS_X + HAUS_B + 20f, boden - 12f, 34f, 12f, 2f, stroke = 1f)
        }

        // Die Geschosse
        m.geschosse.forEach { b ->
            val rr = m.rahmen.getValue(b.id)
            val x = rr.left
            val y = rr.top
            val bb = rr.width
            val h = rr.height
            val (randfarbe, randstaerke) = auswahlrand(gewaehlt == b.id)

            if (b.id == "dg") {
                val dach = pfad(x - 10f, y + h, x + bb / 2f, y, x + bb + 10f, y + h)
                flaeche(dach, Farben.FlaecheHoch)
                flaeche(dach, feuerpinsel(y, y + h), glut(b) * flackern())
                flaeche(dach, Farben.TextSehrLeise, qualm(b))
                umriss(dach, randfarbe, randstaerke)
            } else {
                val keller = b.ebene < 0
                rechteck(if (keller) Farben.FlaecheTief else Farben.Flaeche, x, y, bb, h)
                val (o, g) = r(x, y, bb, h)
                d.drawRect(feuerpinsel(y, y + h), o, g, alpha = (glut(b) * flackern()).coerceIn(0f, 1f))
                rechteck(Farben.TextSehrLeise, x, y, bb, h, qualm(b))
                rand(randfarbe, x, y, bb, h, randstaerke, if (keller && gewaehlt != b.id) gestrichelt(4f, 3f) else null)
            }

            // Fenster: glühen bei Feuer, stehen offen mit Abluft
            val fenster = fensterVon(b, rr)
            if (b.ebene >= 0) {
                fenster.forEachIndexed { i, f ->
                    when {
                        b.abluft && i == 0 -> gerundet(Farben.BlauHell, f.left, f.top, f.width, f.height, 1.5f, stroke = 1f)
                        (b.feuer ?: 0.0) > 20 -> gerundet(Farben.Amber, f.left, f.top, f.width, f.height, 1.5f, alpha = flackern(0.9f))
                        else -> {
                            gerundet(Farben.BgTief, f.left, f.top, f.width, f.height, 1.5f)
                            gerundet(Farben.RandHell, f.left, f.top, f.width, f.height, 1.5f, stroke = 1f)
                        }
                    }
                }
            }

            // Flammen, die herausschlagen
            if ((b.feuer ?: 0.0) > 35) {
                fenster.take(2).forEachIndexed { i, f ->
                    val lodern = 0.82f + 0.3f * welle(0.7f, i * 0.3f)
                    val spitze = (22f + (b.feuer ?: 0.0).toFloat() * 0.12f) * lodern
                    val flamme = Path().apply {
                        moveTo((f.left + 4f) * s, f.top * s)
                        quadraticTo((f.left + 13f) * s, (f.top - spitze) * s, (f.left + 22f) * s, f.top * s)
                        close()
                    }
                    flaeche(flamme, Farben.Amber, 0.9f)
                }
            }

            // Rauchwolke aus dem Dach oder dem obersten Fenster
            if (b.rauch > 50 && (b.id == "dg" || b.ebene == m.oben - 1)) {
                listOf(Triple(60f, -6f, 10f), Triple(76f, -16f, 13f), Triple(98f, -22f, 9f)).forEachIndexed { i, (dx, dy, rad) ->
                    val steigen = ((uhr + i * 0.8f) / 3.2f) % 1f
                    kreis(Farben.TextSehrLeise, x + dx, y + dy - steigen * 14f, rad, 0.55f - steigen * 0.45f)
                }
            }

            // Person am Fenster: winkt
            if (fenster.isNotEmpty()) {
                repeat(b.amFenster) { n ->
                    val f = fenster[n % fenster.size]
                    kreis(Farben.Text, f.left + 13f, f.top + 8f, 3.4f)
                    val winkel = (-20f + 40f * welle(0.6f, n * 0.2f)) * PI.toFloat() / 180f
                    val ax = f.left + 16f
                    val ay = f.top + 12f
                    // Der Arm dreht um die Schulter — 6 nach rechts, 10 nach oben im Ruhezustand.
                    val dx = 6f * cos(winkel) + 10f * sin(winkel)
                    val dy = 6f * sin(winkel) - 10f * cos(winkel)
                    linie(Farben.Text, ax, ay, ax + dx, ay + dy, 2f)
                }
            }

            // Vermisste drin: Umriss, solange niemand sie gefunden hat
            repeat(minOf(b.vermisste ?: 0, 4)) { i ->
                val gefunden = i < b.gefunden
                val px = x + 16f + i * 12f
                val py = y + h - 14f
                if (gefunden) {
                    kreis(Farben.Text, px, py, 3f)
                    gerundet(Farben.Text, px - 3f, py + 3f, 6f, 8f, 1f)
                } else {
                    kreis(Farben.Text, px, py, 3f, stroke = 1f, strich = gestrichelt(2f, 1.5f))
                    gerundet(Farben.Text, px - 3f, py + 3f, 6f, 8f, 1f, stroke = 1f)
                }
            }

            // Was nicht erkundet ist
            if (!b.erkundet) text("?", x + bb - 14f, y + h - 10f, Farben.TextSehrLeise, groesse = 13f, fett = true)

            // Riegel, kontrolliert
            if (b.riegel) linie(Farben.BlauHell, x, y + 1f, x + bb, y + 1f, 3f, gestrichelt(6f, 3f))
            if (b.kontrolliert) text("✓", x + bb - 16f, y + 16f, Farben.GruenHell, groesse = 11f, fett = true, mono = false)

            // Name
            text(b.name, x - 6f, y + h / 2f + 4f, anker = TextAlign.End)

            // Trupps im Geschoss
            truppsIn(lage, b.id).forEachIndexed { i, t -> trupp(t, x + bb - 30f - i * 30f, y + h - 18f, mein) }
        }

        // Der Lüfter vor der Tür
        if (lage.bereiche.any { it.belueftet }) {
            val lx = HAUS_X - 14f
            val ly = boden - 12f
            kreis(Farben.FlaecheHoch, lx, ly, 9f)
            kreis(Farben.BlauHell, lx, ly, 9f, stroke = 1f)
            d.rotate((uhr / 0.5f % 1f) * 360f, p(lx, ly)) {
                linie(Farben.BlauHell, lx, ly, lx, ly - 7f, 2f)
                linie(Farben.BlauHell, lx, ly, lx + 6f, ly + 4f, 2f)
                linie(Farben.BlauHell, lx, ly, lx - 6f, ly + 4f, 2f)
            }
        }
    }

    fun fensterVon(b: Fwbereich, rr: Rect): List<Rect> {
        if (b.id == "dg") return listOf(Rect(rr.left + rr.width / 2f - 12f, rr.bottom - 30f, rr.left + rr.width / 2f + 12f, rr.bottom - 12f))
        val zahl = maxOf(1, (rr.width / 66f).toInt())
        return List(zahl) { i ->
            val x = rr.left + (rr.width / zahl) * i + rr.width / zahl / 2f - 13f
            Rect(x, rr.top + 12f, x + 26f, rr.top + 34f)
        }
    }

    // ------------------------------------------------------- Fahrzeugbrand

    fun fahrzeug(lage: Fwlage, gewaehlt: String?, mein: String?) {
        rechteck(Farben.FlaecheTief, 0f, 138f, 360f, 32f)
        linie(Farben.RandHell, 0f, 138f, 360f, 138f, 1.5f)

        if (lage.variante == "klein") {
            lage.bereiche.forEach { b ->
                val (randfarbe, staerke) = auswahlrand(gewaehlt == b.id)
                gerundet(Farben.Flaeche, 120f, 76f, 120f, 62f, 3f)
                gerundet(randfarbe, 120f, 76f, 120f, 62f, 3f, stroke = staerke)
                gerundet(Farben.FlaecheHoch, 114f, 70f, 132f, 10f, 2f)
                gerundet(randfarbe, 114f, 70f, 132f, 10f, 2f, stroke = staerke)
                val hoch = 76f - (b.feuer ?: 0.0).toFloat() * 0.7f
                val flamme = Path().apply {
                    moveTo(130f * s, 76f * s)
                    quadraticTo(180f * s, hoch * s, 230f * s, 76f * s)
                    close()
                }
                flaeche(flamme, feuerpinsel(hoch, 76f, 0.7f), glut(b) * flackern())
                rechteck(Farben.TextSehrLeise, 120f, 76f, 120f, 62f, qualm(b) * 0.6f)
                truppsIn(lage, b.id).forEachIndexed { i, t -> trupp(t, 92f - i * 26f, 120f, mein) }
            }
            return
        }

        // Karosserie
        val karosserie = pfad(60f, 120f, 70f, 92f, 120f, 86f, 150f, 60f, 240f, 60f, 270f, 86f, 304f, 92f, 310f, 120f)
        flaeche(karosserie, Farben.Flaeche)
        umriss(karosserie, Farben.RandHell, 1.2f)
        listOf(105f, 265f).forEach { rx ->
            kreis(Farben.BgTief, rx, 124f, 15f)
            kreis(Farben.TextSehrLeise, rx, 124f, 15f, stroke = 2f)
        }

        lage.bereiche.forEach { b ->
            val (randfarbe, staerke) = auswahlrand(gewaehlt == b.id)
            when (b.id) {
                "motor" -> {
                    val zone = pfad(270f, 86f, 304f, 92f, 310f, 120f, 248f, 120f, 248f, 86f)
                    flaeche(zone, Farben.FlaecheHoch)
                    flaeche(zone, feuerpinsel(86f, 120f, 0.7f), glut(b) * flackern())
                    umriss(zone, if (gewaehlt == b.id) randfarbe else Farben.Rand, staerke)
                    if ((b.feuer ?: 0.0) > 25) {
                        val hoch = (60f - (b.feuer ?: 0.0).toFloat() * 0.3f) * (0.94f + 0.08f * welle(0.7f))
                        flaeche(Path().apply {
                            moveTo(256f * s, 86f * s)
                            quadraticTo(276f * s, hoch * s, 298f * s, 90f * s)
                            close()
                        }, Farben.Amber, 0.9f)
                    }
                    text("Motor", 278f, 114f, anker = TextAlign.Center)
                }
                "innen" -> {
                    val zone = pfad(150f, 64f, 238f, 64f, 262f, 88f, 262f, 116f, 124f, 116f, 124f, 88f)
                    flaeche(zone, Farben.FlaecheHoch)
                    flaeche(zone, feuerpinsel(64f, 116f, 0.7f), glut(b) * flackern())
                    flaeche(zone, Farben.TextSehrLeise, qualm(b) * 0.7f)
                    umriss(zone, if (gewaehlt == b.id) randfarbe else Farben.Rand, staerke)
                    text("Innenraum", 192f, 104f, anker = TextAlign.Center)
                }
                "akku" -> {
                    gerundet(Farben.FlaecheHoch, 120f, 120f, 120f, 10f, 2f)
                    val (o, g) = r(120f, 120f, 120f, 10f)
                    d.drawRect(feuerpinsel(120f, 130f, 0.7f), o, g, alpha = (glut(b) * flackern()).coerceIn(0f, 1f))
                    gerundet(if (gewaehlt == b.id) randfarbe else Farben.Amber, 120f, 120f, 120f, 10f, 2f, stroke = staerke)
                    text("⚡ Hochvoltbatterie", 180f, 152f, anker = TextAlign.Center)
                }
            }
            truppsIn(lage, b.id).forEachIndexed { i, t ->
                val tx = when (b.id) {
                    "motor" -> 330f
                    "akku" -> 96f
                    else -> 40f - i * 4f
                }
                val ty = if (b.id == "akku") 150f else 100f - i * 26f
                trupp(t, tx, ty, mein)
            }
        }
        if (lage.bereiche.any { it.rauch > 40 }) {
            listOf(Triple(200f, 44f, 12f), Triple(222f, 30f, 15f), Triple(248f, 20f, 10f)).forEachIndexed { i, (x, y, rad) ->
                val steigen = ((uhr + i * 0.8f) / 3.2f) % 1f
                kreis(Farben.TextSehrLeise, x, y - steigen * 14f, rad, 0.55f - steigen * 0.45f)
            }
        }
    }

    // ------------------------------------------------------------- Fläche

    fun sektorPfad(lage: Fwlage, id: String, radius: Float): Path {
        val winkel = (SEKTOR[id] ?: 0f) + lage.wind.toFloat()
        val von = winkel - 45f - 90f
        val vonRad = von * PI.toFloat() / 180f
        return Path().apply {
            moveTo(180f * s, 120f * s)
            lineTo((180f + radius * cos(vonRad)) * s, (120f + radius * sin(vonRad)) * s)
            arcTo(Rect(p(180f, 120f), radius * s), von, 90f, false)
            close()
        }
    }

    fun sektorMitte(lage: Fwlage, id: String, radius: Float): Offset {
        val w = ((SEKTOR[id] ?: 0f) + lage.wind.toFloat() - 90f) * PI.toFloat() / 180f
        return Offset(180f + radius * cos(w), 120f + radius * sin(w))
    }

    fun flaeche(lage: Fwlage, gewaehlt: String?, mein: String?) {
        rechteck(Farben.FlaecheTief, 0f, 0f, 360f, 240f)
        // Bäume
        for (n in 1..14) kreis(Farben.Gruen, ((n * 53) % 360).toFloat(), ((n * 97) % 220 + 10).toFloat(), 6f + (n % 3) * 2f, 0.25f)
        kreis(Farben.BgTief, 180f, 120f, 30f + lage.hektar.toFloat() * 6f, 0.8f)

        lage.bereiche.filter { it.art == "Flaeche" }.forEach { b ->
            val radius = sektorRadius(lage, b)
            val zone = sektorPfad(lage, b.id, radius)
            flaeche(zone, Farben.HauchSignal)
            val pinsel = Brush.radialGradient(
                0f to Farben.Amber,
                0.7f to Farben.Signal,
                1f to Farben.Signal.copy(alpha = 0.2f),
                center = p(180f, 120f),
                radius = radius * s,
            )
            val front = lage.front == b.id
            flaeche(zone, pinsel, glut(b) * flackern(if (front) 0.6f else 1.4f))
            umriss(zone, if (gewaehlt == b.id) Farben.Amber else Farben.Rand, if (gewaehlt == b.id) 2.6f else 1f)
            if (b.riegel) umriss(sektorPfad(lage, b.id, radius + 8f), Farben.BlauHell, 2.5f, gestrichelt(6f, 3f))
            val mitte = sektorMitte(lage, b.id, radius * 0.62f)
            text((if (front) "🔥 " else "") + b.name, mitte.x, mitte.y, anker = TextAlign.Center)
            val aussen = sektorMitte(lage, b.id, radius + 14f)
            truppsIn(lage, b.id).forEachIndexed { i, t -> trupp(t, aussen.x + i * 24f, aussen.y, mein) }
        }

        // Der Wind
        kreis(Farben.Flaeche, 326f, 34f, 20f)
        kreis(Farben.RandHell, 326f, 34f, 20f, stroke = 1f)
        d.rotate(lage.wind.toFloat() + 180f, p(326f, 34f)) {
            linie(Farben.BlauHell, 326f, 48f, 326f, 22f, 2.5f)
            linie(Farben.BlauHell, 320f, 29f, 326f, 21f, 2.5f)
            linie(Farben.BlauHell, 326f, 21f, 332f, 29f, 2.5f)
        }
        text("Wind", 326f, 70f, anker = TextAlign.Center)
        text("%.1f ha".format(lage.hektar), 12f, 228f)
    }

    // -------------------------------------------------------------- Unfall

    fun unfall(lage: Fwlage, gewaehlt: String?, mein: String?) {
        val erledigt = lage.erledigt.toSet()
        rechteck(Farben.FlaecheTief, 0f, 140f, 360f, 50f)
        linie(Farben.TextSehrLeise, 0f, 165f, 360f, 165f, 2f, gestrichelt(14f, 10f))
        // Der Baum
        rechteck(Farben.AmberTief, 318f, 40f, 14f, 100f)
        kreis(Farben.Gruen, 325f, 34f, 30f, 0.35f)

        // Warnleuchten, wenn abgesichert
        if ("absichern" in erledigt) {
            val a = blink(0.7f)
            flaeche(pfad(20f, 140f, 30f, 122f, 40f, 140f), Farben.Amber, a)
            flaeche(pfad(60f, 140f, 70f, 122f, 80f, 140f), Farben.Amber, a)
        }

        // Das Wrack
        val (randfarbe, staerke) = auswahlrand(gewaehlt == "wrack")
        val wrack = pfad(100f, 130f, 108f, 100f, 150f, 94f, 176f, 70f, 252f, 72f, 286f, 96f, 312f, 104f, 314f, 130f)
        flaeche(wrack, Farben.FlaecheHoch)
        umriss(wrack, randfarbe, staerke)
        if ("vu_dach" !in erledigt) {
            val dach = pfad(176f, 70f, 252f, 72f, 270f, 88f, 160f, 88f)
            // Solange das Glas drin ist, schimmert es blau.
            if ("vu_glas" !in erledigt) flaeche(dach, Farben.Blau, 0.35f) else flaeche(dach, Farben.FlaecheHoch)
            umriss(dach, Farben.Rand, 1f)
        } else {
            linie(Farben.Amber, 176f, 70f, 252f, 72f, 2f, gestrichelt(4f, 3f))
        }
        // Tür
        if ("vu_tuer" !in erledigt) {
            rechteck(Farben.FlaecheHoch, 186f, 90f, 44f, 36f)
            rand(Farben.RandHell, 186f, 90f, 44f, 36f, 1f)
        } else {
            rand(Farben.Amber, 186f, 90f, 44f, 36f, 1f, gestrichelt(4f, 3f))
        }
        // Unterbaukeile
        if ("vu_stabilisieren" in erledigt) {
            flaeche(pfad(122f, 140f, 136f, 130f, 150f, 140f), Farben.Amber)
            flaeche(pfad(262f, 140f, 276f, 130f, 290f, 140f), Farben.Amber)
        }
        listOf(134f, 282f).forEach { rx ->
            kreis(Farben.BgTief, rx, 134f, 13f)
            kreis(Farben.TextSehrLeise, rx, 134f, 13f, stroke = 2f)
        }
        // Der Patient
        val pt = lage.patient
        if (pt != null && !pt.befreit) {
            val kritisch = pt.zustand < 35
            val farbe = if (kritisch) Farben.SignalHell else Farben.Text
            val a = if (kritisch) pulsen(0.6f) else 1f
            kreis(farbe, 208f, 100f, 6f, a)
            gerundet(farbe, 202f, 107f, 12f, 14f, 2f, alpha = a)
        }
        if ("vu_batterie" in erledigt) text("⚡✓", 292f, 122f, Farben.GruenHell, groesse = 11f, fett = true, mono = false)

        // Motorraum: kann brennen
        lage.bereiche.filter { it.id == "motor" }.forEach { b ->
            val zone = pfad(286f, 96f, 312f, 104f, 314f, 130f, 270f, 130f, 270f, 96f)
            flaeche(zone, Farben.FlaecheHoch)
            flaeche(
                zone,
                Brush.verticalGradient(0f to Farben.Amber.copy(alpha = 0f), 1f to Farben.Signal, startY = 96f * s, endY = 130f * s),
                glut(b) * flackern(),
            )
            umriss(zone, if (gewaehlt == b.id) Farben.Amber else Farben.Rand, if (gewaehlt == b.id) 2.6f else 1f)
            if ((b.feuer ?: 0.0) > 10) {
                flaeche(Path().apply {
                    moveTo(276f * s, 98f * s)
                    quadraticTo(292f * s, (64f + 6f * welle(0.7f)) * s, 306f * s, 100f * s)
                    close()
                }, Farben.Amber, 0.9f)
            }
            truppsIn(lage, b.id).forEachIndexed { i, t -> trupp(t, 300f - i * 26f, 160f, mein) }
        }

        // Zustandsring des Patienten
        if (pt != null) {
            kreis(Farben.Flaeche, 44f, 52f, 26f)
            kreis(Farben.Rand, 44f, 52f, 26f, stroke = 5f)
            val farbe = when {
                pt.zustand < 35 -> Farben.Signal
                pt.zustand < 60 -> Farben.Amber
                else -> Farben.Gruen
            }
            d.drawArc(
                farbe, -90f, (pt.zustand / 100).toFloat().coerceIn(0f, 1f) * 360f, false,
                topLeft = p(44f - 26f, 52f - 26f), size = Size(52f * s, 52f * s),
                style = Stroke(5f * s, cap = StrokeCap.Round),
            )
            text(pt.zustand.roundToInt().toString(), 44f, 57f, Farben.Text, groesse = 13f, fett = true, anker = TextAlign.Center)
            text("Patient", 44f, 96f, anker = TextAlign.Center)
        }
    }

    // ----------------------------------------------------------- Gefahrgut

    fun gefahrgut(lage: Fwlage, m: Lagebildmass, gewaehlt: String?, mein: String?) {
        val st = lage.stoff
        rechteck(Farben.FlaecheTief, 0f, 0f, 360f, 240f)
        linie(Farben.TextSehrLeise, 0f, 120f, 360f, 120f, 2f, gestrichelt(14f, 10f))
        // Der Gully
        gerundet(Farben.BgTief, 250f, 150f, 22f, 14f, 2f)
        if (st?.aufgefangen == true) gerundet(Farben.Gruen, 250f, 150f, 22f, 14f, 2f, stroke = 2f)
        else gerundet(Farben.TextSehrLeise, 250f, 150f, 22f, 14f, 2f, stroke = 1f)
        text("Kanal", 261f, 178f, anker = TextAlign.Center)

        // Lache oder Wolke
        val gas = st?.gasfoermig == true
        val wabern = if (gas) 0.96f + 0.09f * welle(3f) else 1f
        kreis(if (gas) Farben.Gruen else Farben.Violett, 160f, 120f, m.lacheR * wabern, if (gas) 0.25f else 0.35f)
        if (gewaehlt == "gefahr") kreis(Farben.Amber, 160f, 120f, maxOf(m.lacheR, 46f), stroke = 2f)
        // Absperrung
        m.sperrR?.let { kreis(Farben.Signal, 160f, 120f, it, stroke = 2f, strich = gestrichelt(10f, 6f)) }
        // Das Fahrzeug mit dem Leck
        gerundet(Farben.Flaeche, 130f, 102f, 60f, 36f, 4f)
        gerundet(if (gewaehlt == "gefahr") Farben.Amber else Farben.RandHell, 130f, 102f, 60f, 36f, 4f, stroke = if (gewaehlt == "gefahr") 2.6f else 1.2f)
        rechteck(Farben.Amber, 148f, 110f, 24f, 18f)
        rand(Farben.BgTief, 148f, 110f, 24f, 18f, 1f)
        val un = st?.unNummer?.split('/')
        text(un?.getOrNull(0) ?: "??", 160f, 118f, Farben.BgTief, groesse = 6.5f, fett = true, anker = TextAlign.Center)
        text(un?.getOrNull(1) ?: "????", 160f, 126f, Farben.BgTief, groesse = 6.5f, fett = true, anker = TextAlign.Center)
        if ((st?.leck ?: 1.0) > 0) {
            val fall = (uhr % 1f)
            kreis(Farben.ViolettHell, 190f, 132f + fall * 8f, 2.4f, 1f - fall)
            kreis(Farben.ViolettHell, 194f, 138f + fall * 8f, 1.8f, 1f - fall)
        }
        truppsIn(lage, "gefahr").forEachIndexed { i, t -> trupp(t, 206f + i * 26f, 104f, mein) }

        // Dekonplatz
        if (st?.dekon == true) {
            val dek = pfad(32f, 220f, 54f, 190f, 76f, 220f)
            flaeche(dek, Farben.Blau, 0.6f)
            umriss(dek, Farben.BlauHell, 1f)
            text("Dekon", 54f, 234f, anker = TextAlign.Center)
        }
        text(st?.name ?: "Stoff unbekannt", 12f, 20f)
        st?.absperrung?.takeIf { it > 0 }?.let { text("Absperrung ${it.roundToInt()} m", 12f, 34f) }
    }
}

// ------------------------------------------------------------ Hilfeleistung

/** Die Schritte einer Hilfeleistung — als Ablauf, nicht als Zeichnung. */
private val SCHRITTE = mapOf(
    "oel" to listOf("absichern" to "Absichern", "th_bindemittel" to "Bindemittel", "th_aufnehmen" to "Aufnehmen"),
    "baum" to listOf("absichern" to "Absichern", "th_saege" to "Zerlegen", "th_raeumen" to "Räumen"),
    "wasser" to listOf("th_stromab" to "Strom ab", "th_pumpe" to "Pumpe", "th_sauger" to "Sauger"),
    "tuer" to listOf("th_klingeln" to "Erkunden", "th_oeffnen" to "Öffnen", "th_uebergeben" to "Übergeben"),
    "allgemein" to listOf("erkunden" to "Erkunden", "th_sichern" to "Sichern", "th_arbeiten" to "Arbeiten"),
)

@Composable
private fun Hilfeablauf(lage: Fwlage, modifier: Modifier) {
    val schritte = SCHRITTE[lage.variante ?: "allgemein"] ?: SCHRITTE.getValue("allgemein")
    val erledigt = lage.erledigt.toSet()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier.padding(horizontal = Abstand.Normal, vertical = Abstand.Gross),
    ) {
        schritte.forEachIndexed { i, (id, name) ->
            val fertig = id in erledigt
            val jetzt = !fertig && (i == 0 || schritte[i - 1].first in erledigt)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .weight(1f)
                    .background(Farben.Flaeche, Rundung.Klein)
                    .border(1.dp, if (jetzt) Farben.Amber else Farben.Rand, Rundung.Klein)
                    .padding(Abstand.Klein),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (fertig) Farben.Gruen else Color.Transparent, CircleShape)
                        .border(2.dp, if (fertig) Farben.Gruen else if (jetzt) Farben.Amber else Farben.RandHell, CircleShape),
                ) {
                    Text(
                        if (fertig) "✓" else "${i + 1}",
                        style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                        color = if (fertig) Farben.BgTief else if (jetzt) Farben.Amber else Farben.TextLeise,
                    )
                }
                Text(
                    name,
                    style = Schrift.Klein,
                    color = if (jetzt) Farben.Text else Farben.TextLeise,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
