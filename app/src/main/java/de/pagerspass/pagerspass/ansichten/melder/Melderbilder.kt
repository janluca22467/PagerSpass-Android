package de.pagerspass.pagerspass.ansichten.melder

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import de.pagerspass.pagerspass.melder.Bauplaene
import de.pagerspass.pagerspass.melder.Bauteil
import de.pagerspass.pagerspass.melder.Melderbauplan
import de.pagerspass.pagerspass.melder.Melderkatalog
import de.pagerspass.pagerspass.melder.Melderpalette
import de.pagerspass.pagerspass.netz.Alarmmeldung
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Was ein Melder anzeigt — der Kern von `composables/melderanzeige.ts`.
 *
 * <b>Eine Meldung, zehn Gehäuse.</b> Jedes Gerät nimmt sich davon, was auf sein
 * Glas passt: der Klassiker fünf Zeilen, die Uhr drei, der Monitor alles. Welche
 * Zeilen in welcher Reihenfolge, steht hier einmal — nicht in zehn Zeichnungen.
 */
data class Melderanzeige(
    val kopf: String,
    val stichwort: String,
    val zeilen: List<String>,
    val alarm: Boolean,
    val ort: String = "",
    val meldebild: String = "",
    val einheiten: String = "",
    val zusatz: String? = null,
    /**
     * Das offene Gerätemenü — dann zeigt jedes Display, das Zeilen führt, die
     * Menüzeilen mit dem Auswahlbalken statt der Meldung (siehe [Maler.zeilen]).
     * Der Schirm gehört immer genau einer Sache.
     */
    val menue: de.pagerspass.pagerspass.melder.Menuebild? = null,
    /**
     * Die Aufschriften der Tasten, die mit der Lage wechseln (`mittelwort` im
     * Web): „QUITT" im Alarm, „WÄHLEN" im Menü, „MENÜ" in Ruhe. Schlüssel sind
     * die Aufgaben der Tasten; was fehlt, trägt die Aufschrift der Zeichnung.
     */
    val tasten: Map<String, String> = emptyMap(),
) {
    companion object {
        fun aus(alarm: Alarmmeldung?, laeuft: Boolean, ruhe: String = "BEREIT"): Melderanzeige {
            val jetzt = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(Date())
            if (alarm == null) {
                return Melderanzeige(
                    kopf = jetzt,
                    stichwort = ruhe,
                    zeilen = listOf(ruhe, "Kein Einsatz", "Empfang gut"),
                    alarm = false,
                    ort = "Wache betriebsbereit",
                )
            }
            val zeit = Regex("T(\\d{2}:\\d{2})").find(alarm.zeit)?.groupValues?.get(1)
                ?: jetzt.substringAfter(' ')
            val ort = listOfNotNull(alarm.adresse.ifBlank { null }, alarm.ortsteil?.ifBlank { null }).joinToString(" · ")
            return Melderanzeige(
                kopf = listOf(zeit, alarm.schleife).filter { it.isNotBlank() }.joinToString(" · "),
                stichwort = alarm.stichwort.ifBlank { "ALARM" },
                zeilen = listOfNotNull(
                    "${alarm.stichwort} ${alarm.stichwortText}".trim(),
                    alarm.adresse.ifBlank { null },
                    alarm.ortsteil?.ifBlank { null }?.let { "OT $it" },
                    alarm.meldebild.ifBlank { null },
                    alarm.zusatztext?.ifBlank { null }?.let { "LST: $it" },
                    alarm.einheiten.takeIf { it.isNotEmpty() }?.joinToString(" "),
                ),
                alarm = laeuft,
                ort = ort,
                meldebild = alarm.meldebild,
                einheiten = alarm.einheiten.joinToString(" · "),
                zusatz = alarm.zusatztext?.ifBlank { null },
            )
        }
    }
}

/** Wie breit ein Gerät im Verhältnis zu seiner Höhe steht. */
fun seitenverhaeltnis(bauform: String, plan: Melderbauplan? = null): Float = when {
    plan != null -> planRahmen(plan).let { it.width / it.height }
    else -> when (bauform) {
        "klassik" -> 0.78f
        "farbe" -> 0.56f
        "fax" -> 0.95f
        "quad" -> 1.75f
        "lamellen" -> 0.69f
        "leucht" -> 1.5f
        "bogen" -> 0.6f
        "uhr" -> 0.72f
        "monitor" -> 1.55f
        else -> 0.62f
    }
}

/**
 * Wo das Gerät quittiert — in Hundertsteln der Breite, wie die Zeichnung.
 *
 * Jedes Gehäuse hat seine eine Stelle dafür: die gelbe Taste am Klassiker, der
 * rote Block am Lamellenmelder, das Blatt am Fax. Ein zweiter, gleichwertiger
 * Knopf daneben wäre zwei Wege für dieselbe Sache (siehe `AlarmFax.vue`).
 */
private fun quittierflaechen(bauform: String): List<Rect> = when (bauform) {
    "klassik" -> listOf(Rect(10f, 72f, 82f, 98f))
    "farbe" -> listOf(Rect(8f, 120f, 92f, 142f))
    "fax" -> listOf(Rect(14f, 0f, 86f, 52f))
    "quad" -> listOf(Rect(6f, 0f, 40f, 11f))
    "lamellen" -> listOf(Rect(72f, 58f, 90f, 98f))
    "leucht" -> listOf(Rect(32f, 44f, 50f, 58f))
    "bogen" -> listOf(Rect(10f, 102f, 90f, 136f))
    "uhr" -> listOf(Rect(16f, 90f, 84f, 108f))
    "monitor" -> listOf(Rect(6f, 44f, 44f, 53f))
    else -> listOf(Rect(32f, 104f, 68f, 126f))
}

/**
 * Die Tasten jedes Gehäuses und was sie tun — in Hundertsteln der Breite.
 *
 * <b>Die Aufgaben sind dieselben wie am selbst gebauten Melder</b>
 * (`Bauteil.aufgabe`): `mitte` (quittiert im Alarm, wählt im Menü, öffnet es in
 * Ruhe), `hoch`/`runter` (blättern, im Menü den Balken bewegen), `zurueck`,
 * `vor`, `speicher`, `quittieren`, `ausruecken`, `lauter`/`leiser`, `abriss`,
 * `stop`. Welche Taste welche Aufgabe trägt, steht beim jeweiligen Gerät des
 * Webs (`DmePager.vue`, `KlassikMelder.vue`, …) — hier nur ihre Lage.
 *
 * Die Reihenfolge zählt: Die erste Fläche, die den Tipp trifft, gewinnt.
 */
internal fun tastenflaechen(bauform: String): List<Pair<String, Rect>> = when (bauform) {
    "klassik" -> listOf(
        "mitte" to Rect(10f, 72f, 82f, 98f),
        "hoch" to Rect(14f, 104f, 38f, 116f),
        "runter" to Rect(54f, 104f, 78f, 116f),
    )
    "farbe" -> listOf(
        "links" to Rect(8f, 120f, 48f, 142f),
        "rechts" to Rect(52f, 120f, 92f, 142f),
        "hoch" to Rect(12f, 145f, 34f, 159f),
        "runter" to Rect(66f, 145f, 88f, 159f),
        "speicher" to Rect(35f, 148f, 65f, 158f),
        "lauter" to Rect(94f, 40f, 100f, 60f),
        "leiser" to Rect(94f, 62f, 100f, 82f),
    )
    "fax" -> listOf(
        "abriss" to Rect(14f, 0f, 86f, 52f),
        "stop" to Rect(64f, 63f, 88f, 76f),
    )
    "quad" -> listOf(
        "mitte" to Rect(6f, 0f, 40f, 11f),
        "hoch" to Rect(54f, 0f, 72f, 11f),
        "runter" to Rect(72f, 0f, 90f, 11f),
    )
    "lamellen" -> listOf(
        "mitte" to Rect(72f, 58f, 90f, 98f),
        "hoch" to Rect(69f, 100f, 81f, 111f),
        "runter" to Rect(81f, 100f, 93f, 111f),
    )
    "leucht" -> listOf(
        "hoch" to Rect(74f, 10f, 88f, 24f),
        "runter" to Rect(74f, 26f, 88f, 40f),
        "zurueck" to Rect(16f, 44f, 32f, 57f),
        "quittieren" to Rect(32f, 43f, 50f, 58f),
        "vor" to Rect(50f, 44f, 66f, 57f),
    )
    "bogen" -> listOf(
        "mitte" to Rect(10f, 100f, 50f, 138f),
        "zurueck" to Rect(50f, 100f, 90f, 138f),
        "hoch" to Rect(28f, 0f, 50f, 8f),
        "runter" to Rect(50f, 0f, 72f, 8f),
    )
    "uhr" -> listOf(
        "quittieren" to Rect(16f, 88f, 50f, 108f),
        "ausruecken" to Rect(50f, 88f, 84f, 108f),
        "hoch" to Rect(90f, 43f, 100f, 61f),
        "runter" to Rect(90f, 76f, 100f, 94f),
    )
    "monitor" -> listOf(
        "quittieren" to Rect(6f, 44f, 44f, 53f),
    )
    else -> listOf(
        "hoch" to Rect(8f, 106f, 32f, 124f),
        "mitte" to Rect(32f, 104f, 68f, 126f),
        "runter" to Rect(68f, 106f, 92f, 124f),
        "speicher" to Rect(18f, 129f, 82f, 143f),
    )
}

/**
 * Der Melder als Bild — eines der zehn Gehäuse oder der selbst gebaute.
 *
 * <b>Mit Canvas gezeichnet, nicht aus Bildern.</b> Die Gehäuse des Webs sind
 * CSS-Schichten (Verläufe, Rahmen, Schatten), deren Farben aus dem Gesicht
 * kommen; ein Bild je Gerät und Gesicht wären vierhundert Bilder. Hier ist jedes
 * Gerät eine Zeichnung in Hundertsteln seiner Breite, und das Gesicht färbt sie.
 *
 * @param laeuft Ob der Alarm gerade läuft — dann blinkt die Leuchte und die
 *   Quittiertaste leuchtet.
 * @param beiQuittieren Ein Tipp auf die Quittierstelle des Geräts. `null` in der
 *   Auswahl, wo das Bild nur zeigt.
 */
@Composable
fun Melderbild(
    bauform: String,
    gesicht: String,
    alarm: Alarmmeldung?,
    modifier: Modifier = Modifier,
    laeuft: Boolean = alarm != null,
    plan: Melderbauplan? = null,
    beiQuittieren: (() -> Unit)? = null,
    /**
     * Was das Display zeigt, wenn nicht einfach die Meldung — das Menü, das
     * Ruhebild mit Kennung, eine geblätterte Meldung (siehe `Melderschacht`).
     */
    anzeigeVorgabe: Melderanzeige? = null,
    /**
     * Ein Tipp auf eine Taste des Gehäuses, mit ihrer Aufgabe (siehe
     * [tastenflaechen]). Ist er gesetzt, bedient das Bild das Gerät; sonst
     * zählt nur die Quittierstelle.
     */
    beiTaste: ((String) -> Unit)? = null,
) {
    val messer = rememberTextMeasurer()
    val palette = Melderkatalog.palette(gesicht)
    val anzeige = anzeigeVorgabe ?: Melderanzeige.aus(alarm, laeuft)
    val tipp by rememberUpdatedState(beiQuittieren)
    val taste by rememberUpdatedState(beiTaste)

    val puls = if (laeuft) {
        val lauf = rememberInfiniteTransition(label = "melderpuls")
        lauf.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
            label = "melderpuls-wert",
        ).value
    } else {
        0f
    }

    val verhaeltnis = seitenverhaeltnis(bauform, plan)
    val name = plan?.let { "Selbst gebauter Melder" } ?: Melderkatalog.bauform(bauform)?.name ?: "Melder"

    Canvas(
        modifier = modifier
            .aspectRatio(verhaeltnis)
            .semantics { contentDescription = name }
            .pointerInput(bauform, plan) {
                detectTapGestures { p ->
                    taste?.let { aufTaste ->
                        val aufgabe = if (plan != null) {
                            val r = planRahmen(plan)
                            val u = size.width / r.width
                            plan.teile.filter { it.aufgabe != "keine" && it.aufgabe != "offen" }.firstOrNull { t ->
                                Rect((t.x - r.left) * u, (t.y - r.top) * u, (t.x + t.breite - r.left) * u, (t.y + t.hoehe - r.top) * u)
                                    .inflate(4f).contains(p)
                            }?.aufgabe
                        } else {
                            val u = size.width / 100f
                            tastenflaechen(bauform).firstOrNull { (_, f) ->
                                Rect(f.left * u, f.top * u, f.right * u, f.bottom * u).inflate(4f).contains(p)
                            }?.first
                        }
                        if (aufgabe != null) {
                            aufTaste(aufgabe)
                            return@detectTapGestures
                        }
                    }
                    val aufruf = tipp ?: return@detectTapGestures
                    val treffer = if (plan != null) {
                        val r = planRahmen(plan)
                        val u = size.width / r.width
                        plan.teile.filter { it.aufgabe == "quittieren" }.any { t ->
                            Rect((t.x - r.left) * u, (t.y - r.top) * u, (t.x + t.breite - r.left) * u, (t.y + t.hoehe - r.top) * u)
                                .inflate(4f).contains(p)
                        }
                    } else {
                        val u = size.width / 100f
                        quittierflaechen(bauform).any { f ->
                            Rect(f.left * u, f.top * u, f.right * u, f.bottom * u).inflate(6f).contains(p)
                        }
                    }
                    if (treffer) aufruf()
                }
            },
    ) {
        if (plan != null) {
            eigenerMelder(plan, anzeige, messer, puls)
            return@Canvas
        }
        val m = Maler(this, messer, 100f)
        m.menue = anzeige.menue
        when (bauform) {
            "klassik" -> m.klassik(palette, anzeige, puls)
            "farbe" -> m.farbe(palette, anzeige, puls)
            "fax" -> m.fax(palette, anzeige, puls)
            "quad" -> m.quad(palette, anzeige, puls)
            "lamellen" -> m.lamellen(palette, anzeige, puls)
            "leucht" -> m.leucht(palette, anzeige, puls)
            "bogen" -> m.bogen(palette, anzeige, puls)
            "uhr" -> m.uhr(palette, anzeige, puls)
            "monitor" -> m.monitor(palette, anzeige, puls)
            else -> m.dienst(palette, anzeige, puls)
        }
    }
}

// ============================================================ Zeichenhilfe

/**
 * Der Pinsel — alle Maße in Einheiten, eine Einheit ist ein Hundertstel der
 * Breite (beim eigenen Melder ein Feld).
 */
private class Maler(val d: DrawScope, val messer: TextMeasurer, einheiten: Float) {
    val u = d.size.width / einheiten

    fun kasten(x: Float, y: Float, w: Float, h: Float, farbe: Color, ecke: Float = 0f) =
        d.drawRoundRect(farbe, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(ecke * u))

    fun verlauf(x: Float, y: Float, w: Float, h: Float, oben: Color, unten: Color, ecke: Float = 0f) =
        d.drawRoundRect(
            Brush.verticalGradient(listOf(oben, unten), startY = y * u, endY = (y + h) * u),
            Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(ecke * u),
        )

    fun rahmen(x: Float, y: Float, w: Float, h: Float, farbe: Color, ecke: Float = 0f, staerke: Float = 0.8f) =
        d.drawRoundRect(farbe, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(ecke * u), style = Stroke(staerke * u))

    fun kreis(x: Float, y: Float, r: Float, farbe: Color) = d.drawCircle(farbe, r * u, Offset(x * u, y * u))

    fun ring(x: Float, y: Float, r: Float, farbe: Color, staerke: Float = 0.8f) =
        d.drawCircle(farbe, r * u, Offset(x * u, y * u), style = Stroke(staerke * u))

    fun linie(x1: Float, y1: Float, x2: Float, y2: Float, farbe: Color, staerke: Float = 0.6f) =
        d.drawLine(farbe, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), staerke * u)

    fun pfad(farbe: Color, staerke: Float? = null, bau: Path.() -> Unit) {
        val p = Path().apply(bau)
        if (staerke == null) d.drawPath(p, farbe) else d.drawPath(p, farbe, style = Stroke(staerke * u))
    }

    fun P(x: Float, y: Float) = Offset(x * u, y * u)

    /**
     * Eine Zeile Text, abgeschnitten an der Breite. Zu klein zum Lesen (in der
     * Miniatur der Auswahl) wird sie weggelassen — ein Pixelbrei auf dem Glas
     * sähe aus wie ein kaputtes Display.
     */
    fun text(
        t: String,
        x: Float,
        y: Float,
        w: Float,
        groesse: Float,
        farbe: Color,
        fett: Boolean = false,
        mono: Boolean = true,
        mitte: Boolean = false,
    ) {
        val px = groesse * u
        if (px < 5f || t.isBlank() || w <= 0f) return
        val stil = TextStyle(
            color = farbe,
            fontSize = with(d) { px.toSp() },
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (fett) FontWeight.Bold else FontWeight.Normal,
        )
        val satz = messer.measure(
            t,
            stil,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(maxWidth = (w * u).toInt().coerceAtLeast(1)),
        )
        val links = if (mitte) x * u + (w * u - satz.size.width) / 2f else x * u
        d.drawText(satz, topLeft = Offset(links, y * u))
    }

    /** Das offene Gerätemenü — dann zeigt [zeilen] die Menüzeilen statt der übergebenen. */
    var menue: de.pagerspass.pagerspass.melder.Menuebild? = null

    /**
     * So viele Zeilen, wie in die Höhe passen — die erste auf Wunsch fett.
     *
     * <b>Steht das Menü offen, zeichnet dieselbe Stelle das Menü</b>: mit dem
     * Balken auf der gewählten Zeile, und die Liste rollt darunter durch, damit
     * der Balken im Bild bleibt (`useMenueBlick` im Web). So bekommt jedes
     * Gehäuse sein Menü auf seinem eigenen Glas, ohne dass zehn Zeichnungen es
     * einzeln kennen müssten.
     */
    fun zeilen(liste: List<String>, x: Float, y: Float, w: Float, h: Float, groesse: Float, farbe: Color, kopfFett: Boolean = true, mono: Boolean = true) {
        val abstand = groesse * 1.32f
        val passen = (h / abstand).toInt().coerceAtLeast(1)
        menue?.let { m ->
            val anfang = (m.auswahl - passen + 1).coerceAtLeast(0)
                .coerceAtMost((m.eintraege.size - passen).coerceAtLeast(0))
            val hell = farbe.red * 0.3f + farbe.green * 0.59f + farbe.blue * 0.11f > 0.5f
            val invers = if (hell) Color(0xFF0B0D10) else Color(0xFFF4F8FC)
            m.eintraege.drop(anfang).take(passen).forEachIndexed { i, e ->
                val zy = y + i * abstand
                val gewaehlt = anfang + i == m.auswahl
                if (gewaehlt) kasten(x - 0.4f, zy - groesse * 0.08f, w + 0.8f, abstand, farbe, groesse * 0.15f)
                val tinte = if (gewaehlt) invers else farbe
                val wertBreite = if (e.wert.isBlank()) 0f else minOf(w * 0.4f, e.wert.length * groesse * 0.62f + 0.5f)
                text(e.name, x, zy, w - wertBreite, groesse, tinte, fett = gewaehlt, mono = mono)
                if (wertBreite > 0f) text(e.wert, x + w - wertBreite, zy, wertBreite, groesse, tinte, mono = mono)
            }
            return
        }
        liste.take(passen).forEachIndexed { i, z ->
            text(z, x, y + i * abstand, w, groesse, farbe, fett = kopfFett && i == 0, mono = mono)
        }
    }
}

private val LEUCHTE_AUS = Color(0xFF3A1113)
private val LEUCHTE_AN = Color(0xFFFF4A3D)
private val SIGNALORANGE = Color(0xFFFF8A1F)
private val SCHWARZGLAS = Color(0xFF0B0D10)

private fun Maler.gehaeuse(p: Melderpalette, x: Float, y: Float, w: Float, h: Float, ecke: Float) {
    kasten(x + 0.8f, y + 1.6f, w, h, Color.Black.copy(alpha = 0.35f), ecke)
    verlauf(x, y, w, h, p.gehHell, p.gehTief, ecke)
    rahmen(x, y, w, h, p.gehRand, ecke, 0.9f)
}

private fun Maler.leuchte(x: Float, y: Float, r: Float, puls: Float, an: Boolean) {
    if (an) {
        kreis(x, y, r * 2.2f, LEUCHTE_AN.copy(alpha = 0.18f + 0.3f * puls))
        kreis(x, y, r, lerp(LEUCHTE_AUS, LEUCHTE_AN, 0.4f + 0.6f * puls))
    } else {
        kreis(x, y, r, LEUCHTE_AUS)
    }
    kreis(x - r * 0.3f, y - r * 0.3f, r * 0.3f, Color.White.copy(alpha = 0.25f))
}

private fun Maler.taste(x: Float, y: Float, w: Float, h: Float, grund: Color, aufschrift: String, schrift: Color, groesse: Float = 4f, ecke: Float = h / 2) {
    kasten(x, y + 0.8f, w, h, Color.Black.copy(alpha = 0.4f), ecke)
    verlauf(x, y, w, h, lerp(grund, Color.White, 0.12f), lerp(grund, Color.Black, 0.18f), ecke)
    text(aufschrift, x, y + (h - groesse * 1.25f) / 2f, w, groesse, schrift, fett = true, mitte = true)
}

private fun Maler.lcd(p: Melderpalette, x: Float, y: Float, w: Float, h: Float, ecke: Float = 2f) {
    verlauf(x, y, w, h, p.lcdHell, p.lcdGrund, ecke)
    rahmen(x, y, w, h, p.lcdRand, ecke, 0.8f)
}

private fun Maler.gitter(x: Float, y: Float, w: Float, h: Float, reihen: Int, spalten: Int, farbe: Color) {
    val dx = w / spalten
    val dy = h / reihen
    for (r in 0 until reihen) for (s in 0 until spalten) {
        kreis(x + dx * (s + 0.5f), y + dy * (r + 0.5f), minOf(dx, dy) * 0.28f, farbe)
    }
}

// =============================================================== Die Geräte

/** Das Dienstgerät — `DmePager.vue`: Gitter und Leuchte oben, Hochglanzblende, drei Tasten. */
private fun Maler.dienst(p: Melderpalette, a: Melderanzeige, puls: Float) {
    gehaeuse(p, 2f, 2f, 96f, 157f, 12f)
    rahmen(4.5f, 4.5f, 91f, 152f, Color.Black.copy(alpha = 0.3f), 10f, 0.5f)
    gitter(12f, 9f, 52f, 11f, 3, 13, Color.Black.copy(alpha = 0.55f))
    leuchte(82f, 14.5f, 3.6f, puls, a.alarm)

    verlauf(8f, 25f, 84f, 76f, Color(0xFF1A1D22), SCHWARZGLAS, 5f)
    lcd(p, 13f, 30f, 74f, 63f, 2.5f)
    kasten(13f, 30f, 74f, 8f, Color(0xFF2B4F9E), 2.5f)
    text(a.kopf, 15f, 31.4f, 70f, 4.4f, Color.White)
    zeilen(a.zeilen, 15f, 41f, 70f, 50f, 5.4f, p.lcdTinte)
    text("DME 1", 13f, 94.5f, 30f, 3.6f, p.aufGehaeuse.copy(alpha = 0.7f), fett = true)

    val grau = p.gehTief
    taste(10f, 108f, 20f, 14f, grau, "▲", p.aufGehaeuse)
    taste(34f, 106f, 32f, 18f, if (a.alarm) lerp(SIGNALORANGE, Color(0xFFFFC04D), puls) else grau, a.tasten["mitte"] ?: if (a.alarm) "QUITT" else "OK", if (a.alarm) Color(0xFF1A1200) else p.aufGehaeuse, 4.6f)
    taste(70f, 108f, 20f, 14f, grau, "▼", p.aufGehaeuse)
    taste(20f, 131f, 60f, 10f, grau, "SPEICHER", p.aufGehaeuse, 3.6f)
    kreis(10f, 150f, 1.6f, p.gehRand)
    kreis(90f, 150f, 1.6f, p.gehRand)
}

/** Der Klassiker — kleiner Piepser mit Gürtelclip und großer gelber Quittungstaste. */
private fun Maler.klassik(p: Melderpalette, a: Melderanzeige, puls: Float) {
    verlauf(86f, 20f, 12f, 64f, p.gehMittel, p.gehTief, 3f)
    gehaeuse(p, 2f, 4f, 88f, 120f, 16f)
    leuchte(46f, 9.5f, 2.2f, puls, a.alarm)
    verlauf(8f, 13f, 76f, 52f, Color(0xFF1A1D22), SCHWARZGLAS, 5f)
    lcd(p, 11f, 16f, 70f, 46f, 3f)
    text(a.kopf, 13f, 17.5f, 66f, 3.8f, p.lcdTinte.copy(alpha = 0.75f))
    zeilen(a.zeilen, 13f, 23f, 66f, 38f, 5f, p.lcdTinte)
    val gelb = Color(0xFFF2C300)
    if (a.alarm) kasten(8f, 70f, 76f, 30f, gelb.copy(alpha = 0.2f + 0.3f * puls), 15f)
    taste(10f, 72f, 72f, 26f, gelb, a.tasten["mitte"] ?: if (a.alarm) "QUITTIEREN" else "", Color(0xFF2A2000), 5f, 13f)
    taste(14f, 104f, 24f, 12f, p.gehTief, "▲", p.aufGehaeuse)
    taste(54f, 104f, 24f, 12f, p.gehTief, "▼", p.aufGehaeuse)
}

/** Der Farbmelder — TFT mit Statuszeile, Annehmen und Ablehnen. */
private fun Maler.farbe(p: Melderpalette, a: Melderanzeige, puls: Float) {
    gehaeuse(p, 2f, 2f, 96f, 175f, 14f)
    kasten(8f, 11f, 84f, 101f, Color(0xFF05070A), 6f)
    kasten(11f, 14f, 78f, 95f, Color(0xFF0D1626), 3f)
    kasten(11f, 14f, 78f, 8f, Color(0xFF16233A), 3f)
    for (i in 0 until 4) kasten(14f + i * 2.4f, 20f - (i + 1) * 1.3f, 1.6f, (i + 1) * 1.3f, Color(0xFF8FD14F))
    rahmen(76f, 16f, 9f, 4.2f, Color(0xFFC9D2DC), 0.8f, 0.5f)
    kasten(77f, 17f, 6f, 2.2f, Color(0xFF8FD14F))
    text(a.kopf.substringAfterLast(' '), 30f, 15.4f, 40f, 4f, Color(0xFFC9D2DC), mitte = true)

    val kopfFarbe = if (a.alarm) lerp(Color(0xFFD62F24), Color(0xFFFF5A4D), puls) else Color(0xFF2F9E44)
    kasten(11f, 23f, 78f, 13f, kopfFarbe)
    text(if (a.alarm || a.stichwort != "BEREIT") a.stichwort else "BEREIT", 14f, 25.5f, 72f, 6.4f, Color.White, fett = true)
    zeilen(a.zeilen.drop(1).ifEmpty { a.zeilen }, 14f, 40f, 72f, 66f, 4.8f, Color(0xFFE7EDF5), kopfFett = false, mono = false)

    taste(8f, 120f, 40f, 22f, Color(0xFF2F9E44), a.tasten["links"] ?: "ANNEHMEN", Color.White, 4.2f, 8f)
    taste(52f, 120f, 40f, 22f, Color(0xFFC92A2A), a.tasten["rechts"] ?: "ABLEHNEN", Color.White, 4.2f, 8f)
    // Das Steuerkreuz unter den Softkeys: ▲▼ blättern, die Mitte ist der Speicher.
    taste(14f, 147f, 18f, 10f, p.gehTief, "▲", p.aufGehaeuse, 3.6f, 3f)
    taste(68f, 147f, 18f, 10f, p.gehTief, "▼", p.aufGehaeuse, 3.6f, 3f)
    kasten(35f, 152f, 30f, 3.5f, p.gehTief, 2f)
    // Die beiden Wippen an der Flanke: lauter und leiser.
    kasten(96.5f, 42f, 2.4f, 16f, p.gehTief, 1f)
    kasten(96.5f, 64f, 2.4f, 16f, p.gehTief, 1f)
    leuchte(50f, 164f, 2.6f, puls, a.alarm)
}

/** Das Alarmfax — die Meldung auf dem Blatt; abreißen quittiert. */
private fun Maler.fax(p: Melderpalette, a: Melderanzeige, puls: Float) {
    val papier = Color(0xFFF4F1E8)
    val tinte = Color(0xFF1B1B1B)
    val blatt = if (a.alarm || a.zeilen.size > 3) 50f else 12f
    val oben = 52f - blatt
    kasten(16.6f, oben + 1f, 68f, blatt, Color.Black.copy(alpha = 0.25f))
    kasten(16f, oben, 68f, blatt, papier)
    // Die Abrisskante: gezahnt, wie am echten Gerät.
    pfad(papier) {
        moveTo(P(16f, oben).x, P(16f, oben).y)
        var x = 16f
        while (x < 84f) {
            lineTo(P(x + 1.5f, oben - 1.5f).x, P(x + 1.5f, oben - 1.5f).y)
            lineTo(P(x + 3f, oben).x, P(x + 3f, oben).y)
            x += 3f
        }
        close()
    }
    if (blatt > 20f) {
        text("ALARMFAX", 19f, oben + 2f, 62f, 4f, tinte, fett = true)
        text(a.kopf, 19f, oben + 7f, 62f, 3.4f, tinte.copy(alpha = 0.7f))
        linie(19f, oben + 11.5f, 81f, oben + 11.5f, tinte.copy(alpha = 0.4f), 0.3f)
        zeilen(a.zeilen, 19f, oben + 13f, 62f, blatt - 15f, 3.9f, tinte)
    } else {
        text(a.stichwort, 19f, oben + 3.5f, 62f, 3.8f, tinte.copy(alpha = 0.6f), mitte = true)
    }
    gehaeuse(p, 4f, 50f, 92f, 52f, 8f)
    kasten(12f, 52f, 76f, 3.5f, Color(0xFF05070A), 1.5f)
    // Die Zahnleiste über dem Schlitz.
    var x = 12f
    while (x < 88f) {
        pfad(Color(0xFFB9C2CC)) {
            moveTo(P(x, 52f).x, P(x, 52f).y)
            lineTo(P(x + 1f, 50.6f).x, P(x + 1f, 50.6f).y)
            lineTo(P(x + 2f, 52f).x, P(x + 2f, 52f).y)
            close()
        }
        x += 2f
    }
    lcd(p, 12f, 63f, 36f, 13f, 2f)
    text(if (a.alarm) "EMPFANG" else "BEREIT", 13f, 66f, 34f, 4.4f, p.lcdTinte, fett = true, mitte = true)
    leuchte(57f, 69.5f, 2.6f, puls, a.alarm)
    taste(64f, 63f, 24f, 13f, p.gehTief, "STOP", p.aufGehaeuse, 3.8f, 3f)
    text("ALARMFAX 75", 12f, 88f, 50f, 3.4f, p.aufGehaeuse.copy(alpha = 0.6f), fett = true)
    if (a.alarm) text("▲ ABREISSEN", 55f, 88f, 33f, 3.2f, SIGNALORANGE.copy(alpha = 0.6f + 0.4f * puls), fett = true)
}

/** Der Quermelder — Querformat, gefaste Ecken, die Tasten oben auf dem Rücken. */
private fun Maler.quad(p: Melderpalette, a: Melderanzeige, puls: Float) {
    taste(8f, 1.5f, 30f, 7f, if (a.alarm) lerp(SIGNALORANGE, Color(0xFFFFC04D), puls) else p.gehTief, a.tasten["mitte"].orEmpty(), if (a.alarm) Color(0xFF1A1200) else p.aufGehaeuse, 3f, ecke = 2f)
    taste(56f, 2.5f, 14f, 6f, p.gehTief, "▲", p.aufGehaeuse, 2.8f, ecke = 2f)
    taste(74f, 2.5f, 14f, 6f, p.gehTief, "▼", p.aufGehaeuse, 2.8f, ecke = 2f)
    val fase = 5f
    fun koerper(farbe: Brush) {
        val pf = Path().apply {
            moveTo(P(4f + fase, 7f).x, P(4f + fase, 7f).y)
            lineTo(P(96f - fase, 7f).x, P(96f - fase, 7f).y)
            lineTo(P(96f, 7f + fase).x, P(96f, 7f + fase).y)
            lineTo(P(96f, 55f - fase).x, P(96f, 55f - fase).y)
            lineTo(P(96f - fase, 55f).x, P(96f - fase, 55f).y)
            lineTo(P(4f + fase, 55f).x, P(4f + fase, 55f).y)
            lineTo(P(4f, 55f - fase).x, P(4f, 55f - fase).y)
            lineTo(P(4f, 7f + fase).x, P(4f, 7f + fase).y)
            close()
        }
        d.drawPath(pf, farbe)
        d.drawPath(pf, p.gehRand, style = Stroke(0.7f * u))
    }
    koerper(Brush.verticalGradient(listOf(p.gehHell, p.gehTief), startY = 7f * u, endY = 55f * u))
    for (i in 0 until 7) linie(89.5f, 16f + i * 4.5f, 94f, 16f + i * 4.5f, Color.Black.copy(alpha = 0.45f), 1f)
    verlauf(7.5f, 11f, 80f, 40f, Color(0xFF1A1D22), SCHWARZGLAS, 3f)
    lcd(p, 9.5f, 13f, 76f, 36f, 2f)
    text(a.kopf, 11.5f, 14f, 72f, 3.2f, p.lcdTinte.copy(alpha = 0.75f))
    zeilen(a.zeilen, 11.5f, 18.5f, 72f, 30f, 4.4f, p.lcdTinte)
    leuchte(90.5f, 11f, 1.6f, puls, a.alarm)
}

/** Der Lamellenmelder — vier gewölbte Lamellen, Textdisplay und der rote Block. */
private fun Maler.lamellen(p: Melderpalette, a: Melderanzeige, puls: Float) {
    gehaeuse(p, 4f, 4f, 92f, 137f, 20f)
    for (i in 0 until 4) {
        val y = 14f + i * 9f
        pfad(Color.Black.copy(alpha = 0.45f), 4.2f) {
            moveTo(P(13f, y + 7f).x, P(13f, y + 7f).y)
            quadraticTo(P(50f, y - 3f).x, P(50f, y - 3f).y, P(87f, y + 7f).x, P(87f, y + 7f).y)
        }
        pfad(p.gehHell, 2.8f) {
            moveTo(P(13f, y + 6f).x, P(13f, y + 6f).y)
            quadraticTo(P(50f, y - 4f).x, P(50f, y - 4f).y, P(87f, y + 6f).x, P(87f, y + 6f).y)
        }
    }
    verlauf(8f, 56f, 62f, 44f, Color(0xFF1A1D22), SCHWARZGLAS, 4f)
    lcd(p, 10f, 58f, 58f, 40f, 2.5f)
    text(a.kopf, 12f, 59.5f, 54f, 3.6f, p.lcdTinte.copy(alpha = 0.75f))
    zeilen(a.zeilen, 12f, 65f, 54f, 32f, 4.8f, p.lcdTinte)
    val rot = if (a.alarm) lerp(Color(0xFFC62828), Color(0xFFFF5A4D), puls) else Color(0xFFA32020)
    taste(72f, 58f, 18f, 40f, rot, "", Color.White, ecke = 4f)
    kasten(78f, 76f, 6f, 1.2f, Color.White.copy(alpha = 0.7f), 0.6f)
    a.tasten["mitte"]?.let { text(it, 70f, 88f, 22f, 3f, Color.White.copy(alpha = 0.85f), fett = true, mitte = true) }
    taste(71f, 101f, 9f, 8f, p.gehTief, "▲", p.aufGehaeuse, 3.2f, 2f)
    taste(82f, 101f, 9f, 8f, p.gehTief, "▼", p.aufGehaeuse, 3.2f, 2f)
    for (i in 0 until 5) kasten(30f, 110f + i * 4.4f, 40f, 1.8f, Color.Black.copy(alpha = 0.5f), 0.9f)
    text("LM 4", 40f, 133f, 20f, 3.4f, p.aufGehaeuse.copy(alpha = 0.6f), fett = true, mitte = true)
    leuchte(22f, 116f, 2.4f, puls, a.alarm)
}

/** Der Leuchtmelder — liegendes Kissen, grün hinterleuchtet, Pfeiltasten. */
private fun Maler.leucht(p: Melderpalette, a: Melderanzeige, puls: Float) {
    gehaeuse(p, 3f, 3f, 94f, 61f, 26f)
    val gruen = Color(0xFF8BE36A)
    kasten(10f, 10f, 62f, 32f, Color(0xFF05070A), 5f)
    kasten(12f, 12f, 58f, 28f, gruen.copy(alpha = 0.25f), 4f)
    verlauf(13f, 13f, 56f, 26f, Color(0xFFA6F08A), gruen, 3f)
    text(a.kopf, 15f, 13.8f, 52f, 3.2f, Color(0xFF0E2A0A).copy(alpha = 0.75f))
    zeilen(a.zeilen, 15f, 18.2f, 52f, 20f, 4.6f, Color(0xFF0E2A0A))
    taste(76f, 12f, 10f, 10f, p.gehTief, "▲", p.aufGehaeuse, 3.6f)
    taste(76f, 28f, 10f, 10f, p.gehTief, "▼", p.aufGehaeuse, 3.6f)
    taste(18f, 46f, 12f, 9f, p.gehTief, "◀", p.aufGehaeuse, 3.4f)
    taste(34f, 45f, 14f, 11f, if (a.alarm) lerp(SIGNALORANGE, Color(0xFFFFC04D), puls) else p.gehTief, "OK", if (a.alarm) Color(0xFF1A1200) else p.aufGehaeuse, 3.6f)
    taste(52f, 46f, 12f, 9f, p.gehTief, "▶", p.aufGehaeuse, 3.4f)
    leuchte(80f, 50f, 2.4f, puls, a.alarm)
}

/** Der Bogenmelder — Farbschirm mit Kopfleiste, zwei Bogentasten, Wippe oben. */
private fun Maler.bogen(p: Melderpalette, a: Melderanzeige, puls: Float) {
    kasten(30f, 0.5f, 40f, 6f, p.gehTief, 3f)
    gehaeuse(p, 4f, 4f, 92f, 159f, 22f)
    kasten(10f, 14f, 80f, 84f, Color(0xFF05070A), 6f)
    kasten(13f, 17f, 74f, 78f, Color(0xFFF3F5F7), 3f)
    val kopf = if (a.alarm) lerp(Color(0xFFD9480F), Color(0xFFFF7A3D), puls) else Color(0xFF1C7ED6)
    kasten(13f, 17f, 74f, 13f, kopf, 3f)
    text(a.stichwort, 15f, 19.5f, 60f, 6f, Color.White, fett = true)
    text(a.kopf.substringAfterLast(' '), 70f, 21f, 16f, 3.4f, Color.White.copy(alpha = 0.85f))
    zeilen(a.zeilen.drop(1).ifEmpty { a.zeilen }, 15f, 33f, 70f, 60f, 4.6f, Color(0xFF1B1F25), kopfFett = false, mono = false)

    fun bogentaste(links: Boolean, farbe: Color, aufschrift: String) {
        val x0 = if (links) 12f else 52f
        val x1 = x0 + 36f
        pfad(farbe) {
            moveTo(P(x0, 110f).x, P(x0, 110f).y)
            quadraticTo(P((x0 + x1) / 2, if (links) 102f else 106f).x, P((x0 + x1) / 2, if (links) 102f else 106f).y, P(x1, if (links) 106f else 110f).x, P(x1, if (links) 106f else 110f).y)
            lineTo(P(x1, 128f).x, P(x1, 128f).y)
            quadraticTo(P((x0 + x1) / 2, 136f).x, P((x0 + x1) / 2, 136f).y, P(x0, 128f).x, P(x0, 128f).y)
            close()
        }
        text(aufschrift, x0, 115f, 36f, 3.8f, if (farbe.red > 0.8f) Color(0xFF1A1200) else p.aufGehaeuse, fett = true, mitte = true)
    }
    bogentaste(true, if (a.alarm) lerp(SIGNALORANGE, Color(0xFFFFC04D), puls) else p.gehTief, a.tasten["mitte"] ?: "QUITT")
    bogentaste(false, p.gehTief, a.tasten["zurueck"] ?: "MENÜ")
    gitter(34f, 142f, 32f, 12f, 3, 8, Color.Black.copy(alpha = 0.5f))
    leuchte(50f, 10f, 1.8f, puls, a.alarm)
}

/** Die Einsatzuhr — Stichwort, Ort, Zeit; zwei Flächen. */
private fun Maler.uhr(p: Melderpalette, a: Melderanzeige, puls: Float) {
    verlauf(22f, 0f, 56f, 30f, Color(0xFF2A2E35), Color(0xFF15181D), 4f)
    verlauf(22f, 109f, 56f, 30f, Color(0xFF15181D), Color(0xFF2A2E35), 4f)
    kasten(92f, 45f, 5f, 14f, p.gehTief, 1.5f)
    kasten(92f, 78f, 5f, 14f, p.gehTief, 1.5f)
    gehaeuse(p, 8f, 22f, 84f, 95f, 22f)
    kasten(14f, 28f, 72f, 83f, Color(0xFF05070A), 18f)
    if (a.alarm) rahmen(14f, 28f, 72f, 83f, SIGNALORANGE.copy(alpha = 0.3f + 0.6f * puls), 18f, 1.2f)
    text(a.kopf, 18f, 36f, 64f, 4.2f, Color(0xFFADBBCB), mitte = true)
    text(a.stichwort, 18f, 44f, 64f, 11f, if (a.alarm) SIGNALORANGE else Color.White, fett = true, mitte = true)
    text(a.ort.ifBlank { a.zeilen.getOrElse(1) { "" } }, 18f, 60f, 64f, 4.6f, Color(0xFFF4F8FC), mitte = true, mono = false)
    text(a.zusatz?.let { "LST: $it" } ?: a.meldebild, 18f, 67f, 64f, 4f, Color(0xFFADBBCB), mitte = true, mono = false)
    taste(18f, 90f, 31f, 16f, if (a.alarm) Color(0xFF2B3440) else Color(0xFF1B2028), "QUITT", Color(0xFFE7EDF5), 3.8f, 8f)
    taste(51f, 90f, 31f, 16f, if (a.alarm) lerp(Color(0xFF2F9E44), Color(0xFF51CF66), puls) else Color(0xFF1B2028), "AUSR.", Color.White, 3.8f, 8f)
}

/** Der Alarmmonitor im Flur — Stichwort, Adresse, daneben die Karte. */
private fun Maler.monitor(p: Melderpalette, a: Melderanzeige, puls: Float) {
    kasten(44f, 55f, 12f, 6f, Color(0xFF2A2E35))
    kasten(32f, 60.5f, 36f, 3.5f, Color(0xFF3A4149), 1.5f)
    verlauf(2f, 2f, 96f, 54f, p.gehHell, p.gehTief, 3f)
    kasten(5f, 5f, 90f, 48f, Color(0xFF0A0D12), 1f)
    val kopf = if (a.alarm) lerp(Color(0xFFC92A2A), Color(0xFFFF5A4D), puls) else Color(0xFF2B3440)
    kasten(5f, 5f, 90f, 8.5f, kopf, 1f)
    text(if (a.alarm || a.stichwort != "BEREIT") a.zeilen.firstOrNull() ?: a.stichwort else "Kein Einsatz", 7f, 6.6f, 70f, 4.4f, Color.White, fett = true, mono = false)
    text(a.kopf.substringAfterLast(' '), 80f, 7f, 14f, 3.6f, Color.White.copy(alpha = 0.85f))
    text(a.ort.ifBlank { "Wache betriebsbereit" }, 7f, 16f, 38f, 3.6f, Color(0xFFF4F8FC), fett = true, mono = false)
    text(a.meldebild, 7f, 21f, 38f, 3.2f, Color(0xFFADBBCB), mono = false)
    a.zusatz?.let { text("LST: $it", 7f, 25.5f, 38f, 3.2f, Color(0xFFF4F8FC), mono = false) }
    text(a.einheiten, 7f, 30.5f, 38f, 3.2f, Color(0xFFFFB020))
    // Die Karte: dasselbe Bild, das im Fahrzeug steht — hier nur angedeutet.
    kasten(47f, 15f, 46f, 36f, Color(0xFF1B232E))
    for (i in 1 until 6) linie(47f + i * 7.6f, 15f, 47f + i * 7.6f, 51f, Color(0xFF26303C), 0.3f)
    for (i in 1 until 5) linie(47f, 15f + i * 7.2f, 93f, 15f + i * 7.2f, Color(0xFF26303C), 0.3f)
    linie(47f, 40f, 93f, 26f, Color(0xFF3A4656), 1.4f)
    linie(62f, 15f, 70f, 51f, Color(0xFF3A4656), 1f)
    if (a.alarm) {
        kreis(70f, 33f, 4f, Color(0xFFE5352B).copy(alpha = 0.25f + 0.3f * puls))
        kreis(70f, 33f, 1.8f, Color(0xFFE5352B))
    }
    taste(7f, 44.5f, 36f, 7f, if (a.alarm) lerp(SIGNALORANGE, Color(0xFFFFC04D), puls) else Color(0xFF2B3440), "QUITTIEREN", if (a.alarm) Color(0xFF1A1200) else Color(0xFFC9D2DC), 3.2f, 2f)
}

// ========================================================= Der eigene Melder

/** Der Rahmen um Gehäuse und alle Teile — was übersteht (Antenne, Clip), gehört dazu. */
internal fun planRahmen(plan: Melderbauplan): Rect {
    var r = Rect(0f, 0f, plan.breite.toFloat(), plan.hoehe.toFloat())
    plan.teile.forEach { t ->
        r = Rect(
            minOf(r.left, t.x.toFloat()), minOf(r.top, t.y.toFloat()),
            maxOf(r.right, (t.x + t.breite).toFloat()), maxOf(r.bottom, (t.y + t.hoehe).toFloat()),
        )
    }
    // Ein Rand von einem Feld, damit der Schatten nicht abgeschnitten wird.
    return Rect(r.left - 1f, r.top - 1f, r.right + 1f, r.bottom + 1f)
}

/** Eine Farbe aus `#rrggbb` oder `#rrggbbaa` — was nicht passt, wird Grau statt eines Absturzes. */
internal fun hexfarbe(wert: String): Color = runCatching {
    val h = wert.removePrefix("#")
    val rgb = h.take(6).toLong(16)
    val alpha = if (h.length == 8) h.substring(6, 8).toLong(16) else 0xFF
    Color((alpha shl 24) or rgb)
}.getOrDefault(Color.Gray)

internal fun DrawScope.eigenerMelder(plan: Melderbauplan, a: Melderanzeige, messer: TextMeasurer, puls: Float, gewaehlt: String? = null) {
    val r = planRahmen(plan)
    val m = Maler(this, messer, r.width)
    translate(-r.left * m.u, -r.top * m.u) {
        val tm = Maler(this, messer, r.width)
        tm.menue = a.menue
        // Was außerhalb des Gehäuses liegt (Antenne, Clip), kommt zuerst — dahinter.
        val (aussen, innen) = plan.teile.partition { t -> t.art in setOf("antenne", "clip") }
        aussen.forEach { tm.teil(it, a, puls, plan) }
        tm.planGehaeuse(plan)
        innen.forEach { tm.teil(it, a, puls, plan) }
        gewaehlt?.let { id ->
            plan.teile.firstOrNull { it.id == id }?.let { t ->
                tm.rahmen(t.x - 0.4f, t.y - 0.4f, t.breite + 0.8f, t.hoehe + 0.8f, Color(0xFFFFB020), 0.6f, 0.45f)
            }
        }
    }
}

private fun Maler.planGehaeuse(plan: Melderbauplan) {
    val grund = hexfarbe(plan.gehaeuse)
    val b = plan.breite.toFloat()
    val h = plan.hoehe.toFloat()
    val e = plan.rundung.toFloat()
    kasten(0.3f, 0.6f, b, h, Color.Black.copy(alpha = 0.35f), e)
    when (plan.oberflaeche) {
        "glanz" -> {
            verlauf(0f, 0f, b, h, lerp(grund, Color.White, 0.2f), lerp(grund, Color.Black, 0.3f), e)
            kasten(1f, 1f, b - 2f, h * 0.35f, Color.White.copy(alpha = 0.06f), e)
        }
        "gebuerstet" -> {
            verlauf(0f, 0f, b, h, lerp(grund, Color.White, 0.08f), lerp(grund, Color.Black, 0.1f), e)
            var y = 1f
            while (y < h - 1f) {
                linie(e.coerceAtMost(3f), y, b - e.coerceAtMost(3f), y, Color.White.copy(alpha = 0.05f), 0.15f)
                y += 0.7f
            }
        }
        "gummi" -> verlauf(0f, 0f, b, h, lerp(grund, Color.Black, 0.05f), lerp(grund, Color.Black, 0.2f), e)
        else -> verlauf(0f, 0f, b, h, lerp(grund, Color.White, 0.1f), lerp(grund, Color.Black, 0.22f), e)
    }
    rahmen(0f, 0f, b, h, hexfarbe(plan.rand), e, 0.5f)
}

private fun Maler.teil(t: Bauteil, a: Melderanzeige, puls: Float, plan: Melderbauplan) {
    val x = t.x.toFloat()
    val y = t.y.toFloat()
    val w = t.breite.toFloat()
    val h = t.hoehe.toFloat()
    val e = t.rundung.toFloat().coerceAtMost(minOf(w, h) / 2)
    val f = hexfarbe(t.farbe)
    val z = hexfarbe(t.zweitfarbe)
    when (t.art) {
        "display" -> {
            verlauf(x, y, w, h, lerp(f, Color.White, 0.1f), f, e)
            rahmen(x, y, w, h, lerp(f, Color.Black, 0.4f), e, 0.35f)
            if (t.variante == 1) {
                var yy = y + 0.6f
                while (yy < y + h) { linie(x + 0.3f, yy, x + w - 0.3f, yy, Color.Black.copy(alpha = 0.06f), 0.12f); yy += 0.6f }
            }
            val zeilen = Bauplaene.displayZeilen(t.hoehe)
            val groesse = 1.8f
            zeilen(a.zeilen.take(zeilen + 1), x + 0.8f, y + 0.8f, w - 1.6f, h - 1.2f, groesse, z)
            if (t.variante == 2) kasten(x + 0.5f, y + 0.5f, w - 1f, h * 0.4f, Color.White.copy(alpha = 0.08f), e)
        }
        "statusleiste" -> {
            kasten(x, y, w, h, f, e)
            if (t.variante == 1) {
                for (i in 0 until 5) kreis(x + 1f + i * 1.3f, y + h / 2, 0.4f, z)
            } else {
                for (i in 0 until 4) kasten(x + 0.8f + i * 0.9f, y + h - 0.5f - (i + 1) * (h - 1f) / 4f, 0.6f, (i + 1) * (h - 1f) / 4f, z)
            }
            rahmen(x + w - 3.6f, y + h * 0.25f, 2.8f, h * 0.5f, z, 0.2f, 0.2f)
            if (a.kopf.isNotBlank()) text(a.kopf.substringAfterLast(' '), x + w / 2 - 4f, y + (h - 1.5f) / 2, 8f, 1.4f, z, mitte = true)
        }
        "leuchte" -> {
            val an = a.alarm
            val farbe = if (an) lerp(f, z, 0.4f + 0.6f * puls) else f
            when (t.variante) {
                1 -> kasten(x, y, w, h, farbe, e)
                2 -> ring(x + w / 2, y + h / 2, minOf(w, h) / 2 - 0.3f, farbe, 0.6f)
                else -> kreis(x + w / 2, y + h / 2, minOf(w, h) / 2, farbe)
            }
            if (an) kreis(x + w / 2, y + h / 2, maxOf(w, h), z.copy(alpha = 0.15f * puls))
        }
        "taste" -> {
            val leuchtet = a.alarm && t.aufgabe == "quittieren"
            val grund = if (leuchtet) lerp(f, Color(0xFFFFB020), 0.4f + 0.5f * puls) else f
            when (t.variante) {
                1 -> { kasten(x, y + 0.4f, w, h, Color.Black.copy(alpha = 0.4f), e); verlauf(x, y, w, h, lerp(grund, Color.White, 0.2f), lerp(grund, Color.Black, 0.2f), e) }
                2 -> { verlauf(x, y, w, h, lerp(grund, Color.White, 0.1f), lerp(grund, Color.Black, 0.1f), e); linie(x + 0.5f, y + h / 2, x + w - 0.5f, y + h / 2, Color.Black.copy(alpha = 0.4f), 0.2f) }
                else -> kasten(x, y, w, h, grund, e)
            }
            val groesse = minOf(h * 0.45f, 2.4f)
            text(t.text, x, y + (h - groesse * 1.25f) / 2, w, groesse, z, fett = true, mitte = true)
        }
        "steuerkreuz" -> {
            if (t.variante == 1) kasten(x, y, w, h, f, 1f) else kreis(x + w / 2, y + h / 2, minOf(w, h) / 2, f)
            val leuchtet = a.alarm && t.aufgabe == "quittieren"
            kreis(x + w / 2, y + h / 2, minOf(w, h) / 4.5f, if (leuchtet) lerp(lerp(f, Color.White, 0.2f), Color(0xFFFFB020), puls) else lerp(f, Color.White, 0.15f))
            text("▲", x, y + 0.4f, w, 1.8f, z, mitte = true)
            text("▼", x, y + h - 2.6f, w, 1.8f, z, mitte = true)
            text(t.text.ifBlank { "OK" }, x, y + h / 2 - 1f, w, 1.6f, z, fett = true, mitte = true)
        }
        "schalter" -> {
            kasten(x, y, w, h, f, e)
            if (t.variante == 1) kasten(x + 0.4f, y + 0.4f, w - 0.8f, h / 2 - 0.4f, z, e) else kasten(x + 0.4f, y + 0.4f, w / 2 - 0.4f, h - 0.8f, z, e)
        }
        "rad" -> {
            kreis(x + w / 2, y + h / 2, minOf(w, h) / 2, f)
            if (t.variante == 0) {
                for (i in 0 until 8) {
                    val wnk = i * Math.PI / 4
                    val rr = minOf(w, h) / 2
                    linie(x + w / 2 + (rr * 0.55f * Math.cos(wnk)).toFloat(), y + h / 2 + (rr * 0.55f * Math.sin(wnk)).toFloat(), x + w / 2 + (rr * 0.95f * Math.cos(wnk)).toFloat(), y + h / 2 + (rr * 0.95f * Math.sin(wnk)).toFloat(), z, 0.2f)
                }
            } else {
                linie(x + w / 2, y + 0.4f, x + w / 2, y + h / 2, z, 0.35f)
            }
        }
        "gitter" -> {
            kasten(x, y, w, h, f, e)
            when (t.variante) {
                1 -> { var yy = y + 0.8f; while (yy < y + h - 0.4f) { linie(x + 0.8f, yy, x + w - 0.8f, yy, lerp(z, Color.Black, 0.3f), 0.45f); yy += 1.1f } }
                else -> {
                    var yy = y + 0.8f
                    var reihe = 0
                    while (yy < y + h - 0.3f) {
                        var xx = x + 0.8f + if (t.variante == 2 && reihe % 2 == 1) 0.5f else 0f
                        while (xx < x + w - 0.3f) { kreis(xx, yy, 0.3f, Color.Black.copy(alpha = 0.7f)); xx += 1f }
                        yy += 0.9f; reihe++
                    }
                }
            }
        }
        "antenne" -> {
            verlauf(x, y, w, h, lerp(f, Color.White, 0.1f), lerp(f, Color.Black, 0.2f), e)
            if (t.variante == 2) { var yy = y + 1f; while (yy < y + h - 0.5f) { linie(x, yy, x + w, yy, z, 0.25f); yy += 1f } }
        }
        "blende" -> {
            if (f.alpha > 0f) kasten(x, y, w, h, f, e)
            when (t.variante) {
                1 -> rahmen(x, y, w, h, Color.Black.copy(alpha = 0.5f), e, 0.35f)
                2 -> rahmen(x, y, w, h, Color.White.copy(alpha = 0.12f), e, 0.35f)
            }
        }
        "wulst" -> {
            verlauf(x, y, w, h, f, lerp(f, Color.Black, 0.2f), e)
            if (t.variante == 1) { var yy = y + 1f; while (yy < y + h - 0.5f) { linie(x + 0.3f, yy, x + w - 0.3f, yy, z, 0.25f); yy += 1f } }
        }
        "fuge" -> {
            kasten(x, y, w, h * 0.6f, f)
            kasten(x, y + h * 0.6f, w, h * 0.4f, z.copy(alpha = 0.5f))
        }
        "clip" -> {
            verlauf(x, y, w, h, lerp(f, Color.White, 0.1f), lerp(f, Color.Black, 0.2f), e)
            kasten(x + w * 0.3f, y + 1f, w * 0.4f, h - 2f, z, e / 2)
        }
        "kontakte" -> {
            val n = if (t.variante == 1) 4 else 2
            val breite = w / (n * 2)
            for (i in 0 until n) kasten(x + breite / 2 + i * breite * 2, y, breite, h, z, 0.2f)
        }
        "schraube" -> {
            val rr = minOf(w, h) / 2
            kreis(x + w / 2, y + h / 2, rr, f)
            when (t.variante) {
                1 -> linie(x + w / 2 - rr * 0.6f, y + h / 2, x + w / 2 + rr * 0.6f, y + h / 2, z, 0.25f)
                2 -> kreis(x + w / 2, y + h / 2, rr * 0.4f, z)
                else -> { linie(x + w / 2 - rr * 0.6f, y + h / 2, x + w / 2 + rr * 0.6f, y + h / 2, z, 0.2f); linie(x + w / 2, y + h / 2 - rr * 0.6f, x + w / 2, y + h / 2 + rr * 0.6f, z, 0.2f) }
            }
        }
        "oese" -> {
            ring(x + w / 2, y + h / 2, minOf(w, h) / 2 - 0.4f, f, 0.8f)
            kreis(x + w / 2, y + h / 2, minOf(w, h) / 4, z)
        }
        "schrift" -> {
            if (f.alpha > 0f) kasten(x, y, w, h, f, e)
            val groesse = (h * 0.62f).coerceAtMost(3f)
            val gesperrt = if (t.variante == 2) t.text.toList().joinToString(" ") else t.text
            text(gesperrt, x + 0.3f, y + (h - groesse * 1.25f) / 2, w - 0.6f, groesse, z, fett = true, mitte = t.variante != 0)
        }
        "typenschild" -> {
            kasten(x, y, w, h, f, e)
            rahmen(x + 0.3f, y + 0.3f, w - 0.6f, h - 0.6f, z.copy(alpha = if (t.variante == 1) 0.8f else 0.4f), e, 0.2f)
            text(Bauplaene.HAUSMARKE, x, y + 0.6f, w, minOf(h * 0.3f, 1.6f), z, fett = true, mitte = true)
            text(t.text, x, y + h * 0.5f, w, minOf(h * 0.3f, 1.8f), z, mitte = true)
        }
    }
}
