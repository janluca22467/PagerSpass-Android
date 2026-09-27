package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.netz.ArchivZeitachse
import de.pagerspass.pagerspass.netz.Dienstwoche
import de.pagerspass.pagerspass.netz.RundenKennzahl
import de.pagerspass.pagerspass.netz.Wachenkurve
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Die Bilder des Dienstbuchs — gezeichnet, nicht zusammengesetzt.
 *
 * Übertragen aus `HilfsfristVerlauf.vue`, dem Wochenverlauf der Auswertung und
 * `components/ui/Zeitstrahl.vue`. Im Web sind es SVG und Flexbox; hier ist es
 * `Canvas`, mit denselben Rändern, Marken und Farben.
 *
 * <b>Gezeichnet wird in echten Bildpunkten</b>, nicht in einem gestreckten
 * Koordinatensystem — das machte im Web aus 375 Punkten Breite ovale Punkte und
 * winzige Beschriftung. Die Ränder stehen deshalb in dp und werden erst beim
 * Zeichnen umgerechnet.
 */

// ------------------------------------------------------------------ Stufenring

/**
 * Der Ring um die Stufe — wie weit die laufende Stufe gefüllt ist, nicht der
 * Anteil an der ganzen Laufbahn.
 */
@Composable
fun Stufenring(
    anteil: Float,
    stufe: Int,
    modifier: Modifier = Modifier,
    groesse: Dp = 72.dp,
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(groesse)) {
        Canvas(modifier = Modifier.size(groesse)) {
            val strich = 6.dp.toPx()
            val rand = strich / 2f
            val flaeche = Size(size.width - strich, size.height - strich)
            drawArc(
                color = Farben.BgTief,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(rand, rand),
                size = flaeche,
                style = Stroke(width = strich),
            )
            drawArc(
                color = Farben.Amber,
                startAngle = -90f,
                sweepAngle = 360f * anteil.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(rand, rand),
                size = flaeche,
                style = Stroke(width = strich, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stufe.toString(),
                style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                color = Farben.Amber,
            )
            Text(text = "Stufe", style = Schrift.Winzig, color = Farben.TextSehrLeise)
        }
    }
}

// --------------------------------------------------------------- Balkenzeile

/**
 * Eine Zeile mit Namen, Wert und Balken — „Erfahrung je Organisation" in der
 * Übersicht und „Womit du fährst" in der Auswertung. Der längste Balken ist voll,
 * die anderen stehen im Verhältnis dazu.
 */
@Composable
fun Balkenzeile(
    name: String,
    wert: String,
    anteil: Float,
    modifier: Modifier = Modifier,
    farbe: Color = Farben.Amber,
    punkt: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (punkt) Box(Modifier.size(8.dp).background(farbe, CircleShape))
            Text(
                text = name,
                style = Schrift.Klein,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = wert, style = Schrift.MonoKlein, color = Farben.TextLeise)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Farben.BgTief, Rundung.Rund),
        ) {
            if (anteil > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(anteil.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(farbe, Rundung.Rund),
                )
            }
        }
    }
}

// ---------------------------------------------------------- Hilfsfristverlauf

/** Ein Punkt der Kurve — mit allem, was die Zeile darunter über ihn sagt. */
private data class Kurvenpunkt(
    val zeit: Long,
    val roh: String,
    val sekunden: Double,
    val code: String?,
    val ort: String?,
    val name: String,
    val farbe: Color,
    val klickbar: Boolean,
)

private data class Kurve(
    val name: String,
    val farbe: Color,
    val klickbar: Boolean,
    val punkte: List<Kurvenpunkt>,
)

/** Höchstens drei Vergleichskurven — sonst wird die Fläche unlesbar. */
private const val HOECHSTENS_WACHE = 3

private val WACHENFARBEN = listOf(Farben.Gruen, Farben.Blau, Farben.HiorgBrh)

private val KURVE_HOEHE = 170.dp
private val RAND_OBEN = 16.dp
private val RAND_RECHTS = 14.dp
private val RAND_UNTEN = 24.dp
private val RAND_LINKS = 44.dp

/**
 * Die mittlere Hilfsfrist über die zuletzt gefahrenen Schichten — die eigene
 * Kurve und, falls vorhanden, bis zu drei aus der eigenen Wache daneben.
 *
 * <b>Die x-Achse ist echte Zeit, kein Index:</b> Nur so liegen zwei Kurven
 * verschiedener Leute an der Stelle, an der ihre Schichten tatsächlich endeten.
 *
 * <b>Nur die eigenen Punkte öffnen eine Schicht</b> — die der anderen sagen beim
 * Antippen, wem sie gehören, und mehr nicht: Deren Nachbesprechung darf man nicht
 * lesen.
 */
@Composable
fun HilfsfristVerlauf(
    verlauf: List<RundenKennzahl>,
    mitglieder: List<Wachenkurve>,
    beiOeffnen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kurven = remember(verlauf, mitglieder) {
        val eigene = Kurve(
            name = "Du",
            farbe = Farben.Amber,
            klickbar = true,
            punkte = verlauf.mapNotNull { r ->
                val s = r.hilfsfristSekunden ?: return@mapNotNull null
                val z = zeitVon(r.beendetUm) ?: return@mapNotNull null
                Kurvenpunkt(z.toEpochMilli(), r.beendetUm.orEmpty(), s, r.code, r.ort, "Du", Farben.Amber, true)
            }.sortedBy { it.zeit },
        )
        val wache = mitglieder
            .map { m ->
                m.anzeigename to m.verlauf.mapNotNull { p ->
                    val s = p.hilfsfristSekunden ?: return@mapNotNull null
                    val z = zeitVon(p.beendetUm) ?: return@mapNotNull null
                    Triple(z.toEpochMilli(), p.beendetUm, s)
                }
            }
            .filter { it.second.isNotEmpty() }
            .sortedByDescending { it.second.size }
            .take(HOECHSTENS_WACHE)
            .mapIndexed { i, (name, roh) ->
                val farbe = WACHENFARBEN[i % WACHENFARBEN.size]
                Kurve(
                    name = name,
                    farbe = farbe,
                    klickbar = false,
                    punkte = roh.map { (zeit, text, s) ->
                        Kurvenpunkt(zeit, text, s, null, null, name, farbe, false)
                    }.sortedBy { it.zeit },
                )
            }
        if (eigene.punkte.isNotEmpty()) listOf(eigene) + wache else wache
    }

    val alle = kurven.flatMap { it.punkte }
    // Erst ab zwei Punkten — über alle Kurven zusammen — ergibt sich ein Verlauf.
    if (alle.size <= 1) return

    val hoechstwert = max(60.0, ceil((alle.maxOf { it.sekunden }) / 60.0) * 60.0)
    val zeitMin = alle.minOf { it.zeit }
    val zeitMax = alle.maxOf { it.zeit }
    val messer = rememberTextMeasurer()
    var aktiv by remember { mutableStateOf<Kurvenpunkt?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier.fillMaxWidth(),
    ) {
        // Legende nur ab zwei Kurven — bei einer sagt die Überschrift schon, was
        // gezeichnet ist.
        if (kurven.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                kurven.forEach { k ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(width = 12.dp, height = 3.dp).background(k.farbe, Rundung.Rund))
                        Text(k.name, style = Schrift.Winzig, color = Farben.TextLeise)
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(KURVE_HOEHE)
                .pointerInput(kurven) {
                    detectTapGestures { tipp ->
                        val treffer = 13.dp.toPx()
                        val naechster = kurven.flatMap { k ->
                            k.punkte.map { p ->
                                p to kurvenlage(
                                    p, zeitMin, zeitMax, hoechstwert,
                                    size.width.toFloat(), size.height.toFloat(),
                                    RAND_LINKS.toPx(), RAND_RECHTS.toPx(), RAND_OBEN.toPx(), RAND_UNTEN.toPx(),
                                )
                            }
                        }.minByOrNull { (_, o) -> (o - tipp).getDistance() }
                        val (punkt, lage) = naechster ?: return@detectTapGestures
                        if ((lage - tipp).getDistance() > treffer) {
                            aktiv = null
                            return@detectTapGestures
                        }
                        aktiv = punkt
                        if (punkt.klickbar && punkt.code != null) beiOeffnen(punkt.code)
                    }
                },
        ) {
            val links = RAND_LINKS.toPx()
            val rechts = RAND_RECHTS.toPx()
            val oben = RAND_OBEN.toPx()
            val unten = RAND_UNTEN.toPx()
            val innen = size.height - oben - unten
            val klein = TextStyle(color = Farben.TextSehrLeise, fontSize = 10.sp, fontFamily = Schrift.Mono)

            // Drei Marken reichen: null, Mitte, Höchstwert.
            listOf(0.0, 0.5, 1.0).forEach { anteil ->
                val y = oben + innen * (1f - anteil.toFloat())
                drawLine(Farben.Rand, Offset(links, y), Offset(size.width - rechts, y), 1.dp.toPx())
                val text = messer.measure(minutenSekunden(hoechstwert * anteil), klein)
                drawText(text, topLeft = Offset(links - 8.dp.toPx() - text.size.width, y - text.size.height / 2f))
            }

            kurven.forEach { k ->
                val lagen = k.punkte.map { p ->
                    kurvenlage(
                        p, zeitMin, zeitMax, hoechstwert,
                        size.width, size.height, links, rechts, oben, unten,
                    )
                }
                if (lagen.size > 1) {
                    val pfad = Path().apply {
                        moveTo(lagen.first().x, lagen.first().y)
                        lagen.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        pfad,
                        color = if (k.klickbar) k.farbe else k.farbe.copy(alpha = 0.75f),
                        style = Stroke(
                            width = (if (k.klickbar) 2.dp else 1.5.dp).toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
                k.punkte.forEachIndexed { i, p ->
                    val gewaehlt = aktiv?.name == p.name && aktiv?.zeit == p.zeit
                    drawCircle(
                        color = k.farbe,
                        radius = (if (k.klickbar) 4.5.dp else 3.5.dp).toPx() * (if (gewaehlt) 1.4f else 1f),
                        center = lagen[i],
                    )
                }
            }

            val datumsstil = klein
            val vonText = messer.measure(tagMonatVon(zeitMin), datumsstil)
            drawText(vonText, topLeft = Offset(links, size.height - vonText.size.height - 2.dp.toPx()))
            val bisText = messer.measure(tagMonatVon(zeitMax), datumsstil)
            drawText(
                bisText,
                topLeft = Offset(size.width - rechts - bisText.size.width, size.height - bisText.size.height - 2.dp.toPx()),
            )

            // Nur der eigene, letzte Wert wird beschriftet; eine Zahl an jedem
            // Punkt liest niemand.
            kurven.firstOrNull { it.klickbar }?.punkte?.lastOrNull()?.let { letzter ->
                val o = kurvenlage(
                    letzter, zeitMin, zeitMax, hoechstwert,
                    size.width, size.height, links, rechts, oben, unten,
                )
                val text = messer.measure(
                    dauerText(letzter.sekunden),
                    TextStyle(color = Farben.AmberHell, fontSize = 11.sp, fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                )
                drawText(
                    text,
                    topLeft = Offset(
                        max(0f, o.x - text.size.width),
                        max(0f, o.y - 11.dp.toPx() - text.size.height),
                    ),
                )
            }
        }

        val gewaehlt = aktiv
        if (gewaehlt != null) {
            Text(
                text = listOfNotNull(
                    gewaehlt.name,
                    gewaehlt.ort,
                    kurzzeitpunkt(gewaehlt.roh),
                    dauerText(gewaehlt.sekunden),
                ).joinToString(" · "),
                style = Schrift.MonoKlein,
                color = gewaehlt.farbe,
            )
        } else {
            SehrLeise("Punkt antippen öffnet die Schicht.", mono = true)
        }
    }
}

/** Wo ein Punkt der Kurve sitzt, in Bildpunkten der Zeichenfläche. */
private fun kurvenlage(
    p: Kurvenpunkt,
    zeitMin: Long,
    zeitMax: Long,
    hoechstwert: Double,
    breite: Float,
    hoehe: Float,
    links: Float,
    rechts: Float,
    oben: Float,
    unten: Float,
): Offset {
    val spanne = breite - links - rechts
    val x = if (zeitMax == zeitMin) {
        links + spanne / 2f
    } else {
        links + ((p.zeit - zeitMin).toFloat() / (zeitMax - zeitMin).toFloat()) * spanne
    }
    val innen = hoehe - oben - unten
    val y = oben + innen - (p.sekunden / hoechstwert).toFloat() * innen
    return Offset(x, y)
}

private fun tagMonatVon(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM."))

// ------------------------------------------------------------ Wochenverlauf

/**
 * Der Wochenverlauf der Auswertung: Balken für die Zahl der Schichten, darüber
 * die Hilfsfrist als Punktreihe.
 *
 * <b>Beides in einem Bild</b>, weil man genau ihren Zusammenhang sucht — ob die
 * Frist besser wird, wenn man regelmäßig fährt. <b>Die Frist beginnt nicht bei
 * null:</b> Zwischen 6:30 und 7:10 liegt der ganze Unterschied, und auf einer
 * Achse ab null wären beide derselbe Strich weit oben. Eine kurze Frist ist die
 * bessere und steht deshalb oben.
 */
@Composable
fun Wochenverlauf(verlauf: List<Dienstwoche>, modifier: Modifier = Modifier) {
    val spitze = max(1, verlauf.maxOfOrNull { it.schichten } ?: 0)
    val fristen = verlauf.mapNotNull { it.hilfsfristSekunden }
    val boden = fristen.minOrNull()
    val decke = fristen.maxOrNull()?.let { d -> if (boden != null && d - boden < 10.0) boden + 10.0 else d }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier.fillMaxWidth(),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            if (verlauf.isEmpty()) return@Canvas
            val spalte = size.width / verlauf.size
            val luecke = min(4.dp.toPx(), spalte * 0.25f)
            val balkenbreite = spalte - luecke

            verlauf.forEachIndexed { i, w ->
                val x = i * spalte + luecke / 2f
                val hoehe = size.height * (w.schichten.toFloat() / spitze)
                if (w.schichten == 0) {
                    // Die leere Woche als Strich am Boden — die Lücke ist eine Auskunft.
                    drawRect(
                        Farben.Rand,
                        topLeft = Offset(x, size.height - 2.dp.toPx()),
                        size = Size(balkenbreite, 2.dp.toPx()),
                    )
                } else {
                    drawRect(
                        Farben.Amber.copy(alpha = 0.55f),
                        topLeft = Offset(x, size.height - hoehe),
                        size = Size(balkenbreite, hoehe),
                    )
                }

                val s = w.hilfsfristSekunden
                if (s != null && boden != null && decke != null) {
                    val oben = 100.0 - ((s - boden) / (decke - boden)) * 100.0
                    val y = size.height - (size.height * (oben / 100.0)).toFloat()
                    drawCircle(
                        Farben.GruenHell,
                        radius = 3.5.dp.toPx(),
                        center = Offset(x + balkenbreite / 2f, y.coerceIn(4.dp.toPx(), size.height - 4.dp.toPx())),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(wochenmarke(verlauf.firstOrNull()?.beginn), style = Schrift.MonoKlein, color = Farben.TextLeise)
            Text(
                text = "Balken: Schichten je Woche · Punkt: Ø Hilfsfrist" +
                    if (boden != null && decke != null) {
                        " (${minutenSekunden(boden)} – ${minutenSekunden(decke)})"
                    } else {
                        ""
                    },
                style = Schrift.Winzig,
                color = Farben.TextSehrLeise,
                modifier = Modifier.weight(1f).padding(horizontal = Abstand.Winzig),
            )
            Text(wochenmarke(verlauf.lastOrNull()?.beginn), style = Schrift.MonoKlein, color = Farben.TextLeise)
        }
    }
}

/** „12.02." aus „2026-02-12" — die Wochen stehen eng, der Monat reicht als Anker. */
internal fun wochenmarke(beginn: String?): String {
    val teile = beginn.orEmpty().split("-")
    return if (teile.size == 3) "${teile[2]}.${teile[1]}." else ""
}

// ---------------------------------------------------------------- Zeitstrahl

/** Die Marken in der Reihenfolge, in der ein Einsatz sie durchläuft. */
private data class Zeitmarke(val index: Int, val name: String, val phase: String?, val zeit: Instant)

private data class Zeitabschnitt(
    val name: String,
    val sekunden: Double,
    val anteil: Float,
    val ueberSchwelle: Boolean,
)

/**
 * Der Ablauf eines Einsatzes als waagerechte Leiste: Notruf → Alarm → Status 3 →
 * Status 4 → erste Lagemeldung → Abschluss.
 *
 * <b>Die Abschnitte sind maßstäblich</b> — eine Disposition, die zwei Minuten
 * gedauert hat, ist doppelt so breit wie eine, die eine Minute gedauert hat.
 * Farbe trägt hier einen Zustand, keine Identität: Hervorgehoben wird nur die
 * Disposition, die über ihrer Schwelle liegt (dieselbe Marke wie
 * `Auswertung.Dispositionsschwelle` am Server).
 */
@Composable
fun Zeitstrahl(achse: ArchivZeitachse, prioritaet: Int, modifier: Modifier = Modifier) {
    val marken = listOfNotNull(
        zeitVon(achse.notruf)?.let { Zeitmarke(0, "Notruf", null, it) },
        zeitVon(achse.alarm)?.let { Zeitmarke(1, "Alarm", "Disposition", it) },
        zeitVon(achse.ausgerueckt)?.let { Zeitmarke(2, "Status 3", "Ausrücken", it) },
        zeitVon(achse.vorOrt)?.let { Zeitmarke(3, "Status 4", "Anfahrt", it) },
        zeitVon(achse.ersteLagemeldung)?.let { Zeitmarke(4, "Lagemeldung", "Erkundung", it) },
        zeitVon(achse.abschluss)?.let { Zeitmarke(5, "Abschluss", "Abarbeitung", it) },
    )

    val gesamt = if (marken.size < 2) 0.0 else sekundenZwischen(marken.first().zeit, marken.last().zeit)
    val schwelle = when {
        prioritaet >= 3 -> 60.0
        prioritaet == 2 -> 120.0
        else -> 180.0
    }

    val abschnitte = if (marken.size < 2 || gesamt <= 0.0) {
        emptyList()
    } else {
        marken.zipWithNext { von, bis ->
            val dauer = max(0.0, sekundenZwischen(von.zeit, bis.zeit))
            // Fehlt eine Marke dazwischen, überspannt der Abschnitt zwei Phasen —
            // dann sagt „Alarm → Status 4" mehr als ein halb stimmender Name.
            val luecke = bis.index != von.index + 1
            Zeitabschnitt(
                name = if (!luecke && bis.phase != null) bis.phase else "${von.name} → ${bis.name}",
                sekunden = dauer,
                anteil = (dauer / gesamt).toFloat(),
                ueberSchwelle = bis.phase == "Disposition" && dauer > schwelle,
            )
        }
    }

    if (abschnitte.isEmpty()) {
        SehrLeise("Zu diesem Einsatz gibt es keinen Ablauf — es blieb beim Notruf.", modifier, mono = true)
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth().height(22.dp),
        ) {
            abschnitte.forEach { a ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(max(a.anteil, 0.02f))
                        .fillMaxHeight()
                        .background(
                            if (a.ueberSchwelle) Farben.HauchSignal else Farben.FlaecheAktiv,
                            Rundung.Winzig,
                        ),
                ) {
                    // Unter einem Sechstel der Breite trägt die Liste darunter die Zahl.
                    if (a.anteil >= 0.17f) {
                        Text(
                            text = kurzdauer(a.sekunden),
                            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                            color = if (a.ueberSchwelle) Farben.SignalHell else Farben.Text,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        ) {
            abschnitte.forEach { a ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(6.dp).background(
                            if (a.ueberSchwelle) Farben.Signal else Farben.RandHell,
                            CircleShape,
                        ),
                    )
                    Text(
                        text = "${a.name} ${kurzdauer(a.sekunden)}",
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = if (a.ueberSchwelle) Farben.SignalHell else Farben.TextLeise,
                    )
                }
            }
        }
    }
}

private fun sekundenZwischen(von: Instant, bis: Instant): Double =
    (bis.toEpochMilli() - von.toEpochMilli()) / 1000.0

/** „2:05" ab einer Minute, sonst „42 s". */
private fun kurzdauer(sekunden: Double): String {
    val ganz = sekunden.toInt()
    return if (ganz >= 60) "${ganz / 60}:${(ganz % 60).toString().padStart(2, '0')}" else "$ganz s"
}
