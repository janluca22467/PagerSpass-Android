package de.pagerspass.pagerspass.ui.karte

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.ansichten.rememberKennung
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.fahrzeug.bauplanFuer
import de.pagerspass.pagerspass.ui.fahrzeug.blitzmusterVon
import de.pagerspass.pagerspass.ui.fahrzeug.fahrzeugZeichnen
import de.pagerspass.pagerspass.ui.fahrzeug.fahrzeughofZeichnen
import de.pagerspass.pagerspass.ui.fahrzeug.rememberFahrzeuguhr
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/*
 * Die Lagekarte — nativ nachgebaut nach `Lagekarte.vue`.
 *
 * <b>Kein Leaflet, kein WebView.</b> Eine Slippy-Karte ist Mathematik plus
 * Kacheln: Web-Mercator rechnet Grad in Weltpixel, ein Canvas zeichnet die
 * Kacheln des sichtbaren Ausschnitts und darüber die Marker. Was Leaflet an
 * Wert hätte, sind seine Gesten — und die hat Compose selbst.
 *
 * <b>Kacheln erst nach Einwilligung.</b> Dieselbe Regel wie im Web
 * (Datenschutzerklärung Ziffer 11): Ohne „ja" wird kein einziger Kachel-Abruf
 * gemacht; Marker, Strecken und Radien stehen trotzdem auf dunklem Grund.
 *
 * Kachelquellen (`Kartenstil`), Kachelspeicher und die Einwilligungsfrage stehen
 * seit World in `Kartenflaeche.kt` — dieselben für alle Karten der App.
 */

// ------------------------------------------------------------- Web-Mercator

private fun lonZuWeltX(lon: Double, welt: Double) = (lon + 180.0) / 360.0 * welt

private fun latZuWeltY(lat: Double, welt: Double): Double {
    val rad = lat * PI / 180.0
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * welt
}

private fun weltXZuLon(x: Double, welt: Double) = x / welt * 360.0 - 180.0

private fun weltYZuLat(y: Double, welt: Double): Double {
    val n = PI - 2.0 * PI * y / welt
    return 180.0 / PI * atan(0.5 * (exp(n) - exp(-n)))
}

/** Die Organisationsfarben — dieselben Werte wie `--org-*` in base.css. */
private fun orgFarbe(organisation: String): Color = when (organisation) {
    "Feuerwehr" -> Color(0xFFE5352B)
    "Rettungsdienst" -> Color(0xFFFF8A3D)
    "Thw" -> Color(0xFF1552D1)
    "Polizei" -> Color(0xFF2F9E44)
    else -> Color(0xFF8291A5)
}

private fun prioFarbe(prioritaet: Int): Color = when {
    prioritaet >= 3 -> Farben.SignalHell
    prioritaet == 2 -> Farben.Amber
    else -> Color(0xFF2F9E44)
}

/** Welche Ebenen gezeichnet werden — dieselben sechs wie im Web, Objekte aus. */
class Kartenebenen {
    var einsaetze by mutableStateOf(true)
    var fahrzeuge by mutableStateOf(true)
    var wachen by mutableStateOf(true)
    var kliniken by mutableStateOf(true)
    var wasser by mutableStateOf(true)
    var objekte by mutableStateOf(false)
}

@Composable
fun Lagekarte(
    raum: Raumzustand,
    modifier: Modifier = Modifier,
    ausgewaehlt: String? = null,
    beiWahl: (String?) -> Unit = {},
    /** Das eigene Fahrzeug — die Navigation startet zentriert darauf. */
    eigenesFahrzeugId: String? = null,
) {
    val zusammenhang = LocalContext.current
    val ablage = remember { Ablage(zusammenhang) }
    val bereich = rememberCoroutineScope()
    val dichte = LocalDensity.current
    val messer = rememberTextMeasurer()

    var freigabe by remember { mutableStateOf<String?>("laedt") }
    var stil by remember { mutableStateOf(Kartenstil.Dunkel) }
    LaunchedEffect(Unit) {
        freigabe = ablage.karteFreigabe()
        stil = Kartenstil.entries.firstOrNull { it.name == ablage.karteStil() }
            ?: Kartenstil.Dunkel
    }

    // Der Ausschnitt: Mitte in Grad, Zoom stufenlos. Startpunkt ist das eigene
    // Fahrzeug (Navigation) oder die Kreismitte, wie im Web.
    val eigenes = raum.vehicles.firstOrNull { it.id == eigenesFahrzeugId }
    var mitteLat by remember {
        mutableStateOf(eigenes?.lat ?: raum.settings.lat ?: 52.6205)
    }
    var mitteLon by remember {
        mutableStateOf(eigenes?.lon ?: raum.settings.lon ?: 10.077)
    }
    var zoom by remember { mutableStateOf(if (eigenesFahrzeugId != null) 13.0 else 12.0) }
    var groesse by remember { mutableStateOf(IntSize.Zero) }
    var gewaehltesFahrzeug by remember { mutableStateOf<String?>(null) }
    var stilwahl by remember { mutableStateOf(false) }
    var ebenenwahl by remember { mutableStateOf(false) }
    val ebenen = remember { Kartenebenen() }

    // --- Leitstelle: Riss mit Kurs und Blaulicht, Kennung am Schild, Ortungskreise.
    val kennung = rememberKennung(raum)
    val kurse = remember { mutableMapOf<String, Float>() }
    val letztePositionen = remember { mutableMapOf<String, Pair<Double, Double>>() }
    val blaulichtDa = raum.vehicles.any { it.lat != null && ausgerueckt(it) && mitBlaulicht(it) }
    // Die Uhr nur, solange irgendwo ein Blaulicht blitzt — sonst zeichnet die Karte
    // nicht jeden Rahmen neu.
    val uhr = if (blaulichtDa) rememberFahrzeuguhr() else null
    val ortungen = ortungenVon(raum)
    val ohneKoordinaten = raum.incidents.count { !it.abgeschlossen && it.lat == null }

    // Beim ersten Zeichnen mit Ziel: Fahrzeug und Einsatz gemeinsam ins Bild —
    // das einmalige Auto-Zentrieren der NavigationKarte.
    var zentriert by remember { mutableStateOf(false) }
    LaunchedEffect(eigenes?.einsatzId, groesse) {
        if (zentriert || groesse == IntSize.Zero || eigenes == null) return@LaunchedEffect
        val ziel = raum.incidents.firstOrNull { it.id == eigenes.einsatzId }
        if (eigenes.lat != null && eigenes.lon != null && ziel?.lat != null && ziel.lon != null) {
            val punkte = listOf(
                eigenes.lat to eigenes.lon,
                ziel.lat to ziel.lon,
            )
            zuschneiden(punkte, groesse) { la, lo, z ->
                mitteLat = la; mitteLon = lo; zoom = z
            }
        }
        zentriert = true
    }

    val ortungsIds = ortungen.map { it.id }
    var gesehenOrtungen by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(ortungsIds, groesse) {
        if (groesse == IntSize.Zero) return@LaunchedEffect
        val neu = ortungen.filter { it.id !in gesehenOrtungen }
        neu.lastOrNull()?.let { o ->
            val grad = o.radius / 111_320.0
            zuschneiden(
                listOf(o.lat - grad to o.lon - grad, o.lat + grad to o.lon + grad),
                groesse,
            ) { la, lo, z -> mitteLat = la; mitteLon = lo; zoom = z }
        }
        gesehenOrtungen = ortungsIds.toSet()
    }

    fun weltGroesse() = 256.0 * 2.0.pow(zoom)

    fun bildschirm(lat: Double, lon: Double): Offset {
        val welt = weltGroesse()
        val dx = lonZuWeltX(lon, welt) - lonZuWeltX(mitteLon, welt)
        val dy = latZuWeltY(lat, welt) - latZuWeltY(mitteLat, welt)
        return Offset(
            (groesse.width / 2f + dx).toFloat(),
            (groesse.height / 2f + dy).toFloat(),
        )
    }

    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Farben.BgTief)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { mittelpunkt, verschiebung, zoomFaktor, _ ->
                        val welt = weltGroesse()
                        // Erst der Zoom um den Fingerpunkt, dann die Verschiebung.
                        if (zoomFaktor != 1f) {
                            val neuerZoom = (zoom + ln(zoomFaktor.toDouble()) / ln(2.0))
                                .coerceIn(4.0, stil.maxZoom.toDouble())
                            val anteil = 2.0.pow(neuerZoom - zoom)
                            // Der Punkt unterm Finger bleibt unterm Finger.
                            val fx = mittelpunkt.x - size.width / 2f
                            val fy = mittelpunkt.y - size.height / 2f
                            val neueWelt = 256.0 * 2.0.pow(neuerZoom)
                            val mx = lonZuWeltX(mitteLon, welt) + fx
                            val my = latZuWeltY(mitteLat, welt) + fy
                            mitteLon = weltXZuLon(mx * anteil - fx, neueWelt)
                            mitteLat = weltYZuLat(my * anteil - fy, neueWelt)
                            zoom = neuerZoom
                        }
                        val nach = weltGroesse()
                        mitteLon = weltXZuLon(
                            lonZuWeltX(mitteLon, nach) - verschiebung.x, nach,
                        )
                        mitteLat = weltYZuLat(
                            (latZuWeltY(mitteLat, nach) - verschiebung.y)
                                .coerceIn(0.0, nach),
                            nach,
                        )
                    }
                }
                .pointerInput(raum.incidents, raum.vehicles) {
                    detectTapGestures { tipp ->
                        // Marker-Treffer: erst Einsätze (oben), dann Fahrzeuge.
                        val schwelle = with(dichte) { 24.dp.toPx() }
                        val einsatz = raum.incidents
                            .filter { !it.abgeschlossen && it.lat != null }
                            .minByOrNull {
                                (bildschirm(it.lat!!, it.lon!!) - tipp).getDistance()
                            }
                        if (einsatz != null &&
                            (bildschirm(einsatz.lat!!, einsatz.lon!!) - tipp)
                                .getDistance() < schwelle
                        ) {
                            beiWahl(if (ausgewaehlt == einsatz.id) null else einsatz.id)
                            return@detectTapGestures
                        }
                        val fahrzeug = raum.vehicles
                            .filter { it.lat != null && ausgerueckt(it) }
                            .minByOrNull {
                                (bildschirm(it.lat!!, it.lon!!) - tipp).getDistance()
                            }
                        if (fahrzeug != null &&
                            (bildschirm(fahrzeug.lat!!, fahrzeug.lon!!) - tipp)
                                .getDistance() < schwelle
                        ) {
                            gewaehltesFahrzeug =
                                if (gewaehltesFahrzeug == fahrzeug.id) null else fahrzeug.id
                        } else {
                            beiWahl(null)
                            gewaehltesFahrzeug = null
                        }
                    }
                },
        ) {
            groesse = IntSize(size.width.toInt(), size.height.toInt())
            zoomFuerZeichnen = zoom
            // Der Lese-Anker für angekommene Kacheln — siehe Kachelspeicher.
            @Suppress("UNUSED_EXPRESSION")
            Kachelspeicher.stand

            // ------------------------------------------------------ Kacheln
            if (freigabe == "ja") {
                val kachelZ = floor(zoom).toInt().coerceIn(4, stil.maxZoom)
                val skala = 2.0.pow(zoom - kachelZ)
                val kachelPx = (256.0 * skala).toFloat()
                val welt = weltGroesse()
                val linksWelt = lonZuWeltX(mitteLon, welt) - size.width / 2.0
                val obenWelt = latZuWeltY(mitteLat, welt) - size.height / 2.0
                val vonX = floor(linksWelt / (256.0 * skala)).toInt()
                val vonY = floor(obenWelt / (256.0 * skala)).toInt()
                val bisX = floor((linksWelt + size.width) / (256.0 * skala)).toInt()
                val bisY = floor((obenWelt + size.height) / (256.0 * skala)).toInt()
                val kachelnJeReihe = 1 shl kachelZ

                for (x in vonX..bisX) for (y in vonY..bisY) {
                    if (y < 0 || y >= kachelnJeReihe) continue
                    val xNorm = ((x % kachelnJeReihe) + kachelnJeReihe) % kachelnJeReihe
                    val ziel = Offset(
                        (x * kachelPx - linksWelt).toFloat(),
                        (y * kachelPx - obenWelt).toFloat(),
                    )
                    val ebenenUrls = listOfNotNull(
                        stil.url(stil.quelle, kachelZ, xNorm, y),
                        stil.beschriftung?.let { stil.url(it, kachelZ, xNorm, y) },
                    )
                    ebenenUrls.forEach { url ->
                        val bild = Kachelspeicher.fertig[url]
                        if (bild == null) {
                            Kachelspeicher.anfordern(url, bereich)
                        } else {
                            drawImage(
                                image = bild,
                                dstOffset = IntOffset(ziel.x.toInt(), ziel.y.toInt()),
                                dstSize = IntSize(
                                    (kachelPx + 1).toInt(),
                                    (kachelPx + 1).toInt(),
                                ),
                            )
                        }
                    }
                }
            }

            // ------------------------------------------- Flächen und Radien
            raum.incidents.filter { !it.abgeschlossen && it.lat != null }.forEach { e ->
                val ort = bildschirm(e.lat!!, e.lon!!)
                e.absperrradiusMeter?.let { r ->
                    kreisMitFahne(
                        ort, e.lat, r, Color(0xFFFF9D3B),
                        raum.settings.windrichtung,
                    )
                }
                e.brandradiusMeter?.let { r ->
                    kreis(ort, e.lat, r, Color(0xFFE5484D))
                }
                e.suchradiusMeter?.let { r ->
                    e.suchabschnitte.forEach { a ->
                        sektor(
                            ort, e.lat, r,
                            a.nummer * 45.0, 45.0,
                            Color(0xFF4FC3D9).copy(
                                alpha = 0.06f + (a.fortschritt.toFloat() * 0.28f),
                            ),
                        )
                    }
                }
            }

            // ------------------------------------------------------ Strecke
            val strecke = raum.vehicles.firstOrNull { it.id == gewaehltesFahrzeug }
            if (strecke?.lat != null && strecke.lon != null) {
                if (strecke.route.size > 1) {
                    val pfad = Path()
                    strecke.route.drop(strecke.routeIndex.coerceAtMost(strecke.route.size - 1))
                        .forEachIndexed { i, p ->
                            val o = bildschirm(p.lat, p.lon)
                            if (i == 0) pfad.moveTo(o.x, o.y) else pfad.lineTo(o.x, o.y)
                        }
                    drawPath(pfad, Farben.Amber, style = Stroke(width = 4.dp.toPx() / 2), alpha = 0.9f)
                } else {
                    val ziel = raum.incidents.firstOrNull { it.id == strecke.einsatzId }
                    if (ziel?.lat != null && ziel.lon != null) {
                        drawLine(
                            color = Color(0xFF8291A5),
                            start = bildschirm(strecke.lat, strecke.lon),
                            end = bildschirm(ziel.lat, ziel.lon),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 16f)),
                        )
                    }
                }
            }

            // ------------------------------------------------------- Wachen
            if (ebenen.wachen) {
                raum.vehicles
                    .filter { it.wacheLat != null }
                    .groupBy { "%.4f/%.4f".format(it.wacheLat, it.wacheLon) }
                    .values.forEach { amStandort ->
                        val w = amStandort.first()
                        val o = bildschirm(w.wacheLat!!, w.wacheLon!!)
                        drawRect(
                            color = Farben.BgTief,
                            topLeft = o - Offset(5.dp.toPx(), 5.dp.toPx()),
                            size = Size(10.dp.toPx(), 10.dp.toPx()),
                        )
                        drawRect(
                            color = Color(0xFF8291A5),
                            topLeft = o - Offset(5.dp.toPx(), 5.dp.toPx()),
                            size = Size(10.dp.toPx(), 10.dp.toPx()),
                            style = Stroke(1.5.dp.toPx()),
                        )
                    }
            }

            // ------------------------------------------------- Feste Ziele
            if (ebenen.kliniken) {
                raum.kliniken.forEach { k ->
                    val o = bildschirm(k.lat, k.lon)
                    kreuz(o, Color(0xFFE5484D))
                }
            }
            if (ebenen.wasser) {
                raum.entnahmestellen.forEach { e ->
                    drawCircle(
                        Color(0xFF4FC3D9),
                        radius = 4.dp.toPx(),
                        center = bildschirm(e.lat, e.lon),
                        alpha = 0.9f,
                    )
                }
            }
            if (ebenen.objekte) {
                raum.sonderobjekte.forEach { s ->
                    val o = bildschirm(s.lat, s.lon)
                    rotate(45f, o) {
                        drawRect(
                            Color(0xFF9C7BD4),
                            topLeft = o - Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = Size(8.dp.toPx(), 8.dp.toPx()),
                        )
                    }
                }
            }

            // ------------------------------------------------ Ortungen
            // Als Kreis, nicht als Punkt: Die Ungenauigkeit muss man sehen.
            ortungen.forEach { o ->
                val ort = bildschirm(o.lat, o.lon)
                val r = meterZuPx(o.lat, o.radius, zoomFuerZeichnen)
                drawCircle(Farben.Amber.copy(alpha = 0.08f), radius = r, center = ort)
                drawCircle(
                    Farben.Amber,
                    radius = r,
                    center = ort,
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
                )
                val text = messer.measure(
                    "Ortung Notruf ${o.nummer} (±${o.radius.toInt()} m)",
                    TextStyle(color = Farben.AmberHell, fontSize = 9.sp),
                )
                drawText(text, topLeft = ort - Offset(text.size.width / 2f, r + text.size.height))
            }

            // ---------------------------------------------------- Fahrzeuge
            // Der Riss des Fahrzeugs, gedreht in Fahrtrichtung, mit Blaulicht bei
            // Status 3 und 7. Wer auf demselben Fleck steht, wird auf einen kleinen
            // Kreis verteilt — im Bildschirm, damit der Abstand auf jeder Stufe bleibt.
            if (ebenen.fahrzeuge) {
                val draussen = raum.vehicles.filter { it.lat != null && ausgerueckt(it) }
                val stapel = draussen.groupBy { "%.4f,%.4f".format(it.lat, it.lon) }
                    .mapValues { (_, liste) -> liste.map { it.id }.sorted() }
                val jetztMs = uhr?.value
                draussen.forEach { f ->
                    val lat = f.lat!!
                    val lon = f.lon!!
                    val vorher = letztePositionen[f.id]
                    if (vorher != null && (vorher.first != lat || vorher.second != lon)) {
                        kurse[f.id] = peilung(vorher.first, vorher.second, lat, lon)
                    }
                    letztePositionen[f.id] = lat to lon

                    val gruppe = stapel["%.4f,%.4f".format(lat, lon)].orEmpty()
                    var o = bildschirm(lat, lon)
                    if (gruppe.size > 1) {
                        val platz = gruppe.indexOf(f.id).coerceAtLeast(0)
                        val r = (9 + min(gruppe.size, 8)).dp.toPx()
                        val w = 2 * PI * platz / gruppe.size - PI / 2
                        o += Offset((cos(w) * r).toFloat(), (sin(w) * r).toFloat())
                    }
                    val eigen = f.id == eigenesFahrzeugId
                    val bauplan = bauplanFuer(f.typ, f.organisation)
                    val blau = mitBlaulicht(f)
                    val hoehe = 30.dp.toPx()

                    if (eigen || f.id == gewaehltesFahrzeug) {
                        drawCircle(Farben.Amber, radius = 19.dp.toPx(), center = o, style = Stroke(1.5.dp.toPx()))
                    }
                    if (blau && bauplan.blaulicht && jetztMs != null) {
                        fahrzeughofZeichnen(o, hoehe * 0.62f, blitzmusterVon(bauplan), jetztMs)
                    }
                    fahrzeugZeichnen(
                        bauplan = bauplan,
                        mitte = o,
                        hoehe = hoehe,
                        drehung = kurse[f.id] ?: 0f,
                        blaulicht = blau,
                        jetzt = jetztMs,
                    )

                    val text = messer.measure(
                        kennung(f),
                        TextStyle(
                            color = if (f.id == gewaehltesFahrzeug) Farben.Amber else Farben.Text,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    val schild = o + Offset(-text.size.width / 2f, hoehe / 2f + 2.dp.toPx())
                    drawRect(
                        Farben.BgTief.copy(alpha = 0.75f),
                        topLeft = schild - Offset(3.dp.toPx(), 0f),
                        size = Size(text.size.width + 6.dp.toPx(), text.size.height.toFloat()),
                    )
                    drawText(text, topLeft = schild)

                    // Die NA-Marke steht am Schild: Ein mitgedrehtes „NA" stünde die
                    // Hälfte der Zeit auf dem Kopf.
                    if (raum.vehicles.any { it.notarztBei == f.funkrufname }) {
                        val na = messer.measure(
                            "NA",
                            TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        )
                        val naOrt = schild + Offset(text.size.width + 5.dp.toPx(), 0f)
                        drawRect(Farben.Signal, topLeft = naOrt, size = Size(na.size.width + 4.dp.toPx(), na.size.height.toFloat()))
                        drawText(na, topLeft = naOrt + Offset(2.dp.toPx(), 0f))
                    }
                }
            }

            // ----------------------------------------------------- Einsätze
            if (ebenen.einsaetze) {
                raum.incidents.filter { !it.abgeschlossen && it.lat != null }.forEach { e ->
                    val o = bildschirm(e.lat!!, e.lon!!)
                    val farbe = prioFarbe(e.prioritaet)
                    val gewaehlt = e.id == ausgewaehlt

                    if (e.prioritaet >= 3) {
                        drawCircle(farbe.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = o)
                    }
                    drawCircle(Farben.BgTief, radius = 9.dp.toPx(), center = o)
                    drawCircle(
                        farbe,
                        radius = 8.dp.toPx(),
                        center = o,
                        style = if (gewaehlt) androidx.compose.ui.graphics.drawscope.Fill
                        else Stroke(2.5.dp.toPx()),
                    )
                    val text = messer.measure(
                        e.stichwort,
                        TextStyle(
                            color = if (gewaehlt) Farben.BgTief else farbe,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    drawText(
                        text,
                        topLeft = o - Offset(text.size.width / 2f, text.size.height / 2f),
                    )
                }
            }
        }

        // --------------------------------------------------------- Knöpfe
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Abstand.Normal),
        ) {
            Kartenknopf("⛶") {
                val punkte = raum.incidents
                    .filter { !it.abgeschlossen && it.lat != null }
                    .map { it.lat!! to it.lon!! } +
                    raum.vehicles.filter { it.lat != null && ausgerueckt(it) }
                        .map { it.lat!! to it.lon!! }
                zuschneiden(punkte.ifEmpty { listOf(mitteLat to mitteLon) }, groesse) { la, lo, z ->
                    mitteLat = la; mitteLon = lo; zoom = z
                }
            }
            // Zum gewählten Einsatz springen — praktisch, wenn er außerhalb liegt.
            raum.incidents.firstOrNull { it.id == ausgewaehlt && it.lat != null }?.let { e ->
                Kartenknopf("◎") {
                    mitteLat = e.lat!!
                    mitteLon = e.lon!!
                    zoom = 15.0
                }
            }
            Kartenknopf("◐") { stilwahl = !stilwahl; ebenenwahl = false }
            Kartenknopf("≣") { ebenenwahl = !ebenenwahl; stilwahl = false }
            if (stilwahl) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.flaeche(ecke = 9.dp).padding(Abstand.Klein),
                ) {
                    Kartenstil.entries.forEach { s ->
                        Text(
                            text = (if (s == stil) "● " else "○ ") + s.titel,
                            style = Schrift.Klein,
                            color = if (s == stil) Farben.Amber else Farben.Text,
                            modifier = Modifier
                                .clickable {
                                    stil = s
                                    stilwahl = false
                                    bereich.launch { ablage.karteStilSetzen(s.name) }
                                }
                                .padding(Abstand.Klein),
                        )
                    }
                }
            }
            if (ebenenwahl) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.flaeche(ecke = 9.dp).padding(Abstand.Klein),
                ) {
                    @Composable
                    fun zeile(name: String, an: Boolean, setzen: (Boolean) -> Unit) {
                        Text(
                            text = (if (an) "☑ " else "☐ ") + name,
                            style = Schrift.Klein,
                            color = Farben.Text,
                            modifier = Modifier
                                .clickable { setzen(!an) }
                                .padding(Abstand.Klein),
                        )
                    }
                    zeile("Einsätze", ebenen.einsaetze) { ebenen.einsaetze = it }
                    zeile("Fahrzeuge", ebenen.fahrzeuge) { ebenen.fahrzeuge = it }
                    zeile("Wachen", ebenen.wachen) { ebenen.wachen = it }
                    zeile("Kliniken", ebenen.kliniken) { ebenen.kliniken = it }
                    zeile("Löschwasser", ebenen.wasser) { ebenen.wasser = it }
                    zeile("Objekte", ebenen.objekte) { ebenen.objekte = it }
                }
            }
        }

        // --- Leitstelle: was der Tooltip im Web sagt, steht hier unten links —
        // am Finger gibt es kein „darüber".
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Abstand.Normal)
                .padding(bottom = 16.dp),
        ) {
            raum.vehicles.firstOrNull { it.id == gewaehltesFahrzeug }?.let { f ->
                val begleitung = raum.vehicles.firstOrNull { it.notarztBei == f.funkrufname }?.funkrufname
                Column(
                    modifier = Modifier.flaeche(ecke = 9.dp).padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                    Text(f.statusText, style = Schrift.Klein, color = Farben.TextLeise)
                    if (begleitung != null) {
                        Text("Notarzt an Bord — Begleitung durch $begleitung", style = Schrift.Klein, color = Farben.SignalHell)
                    }
                }
            }
            if (ohneKoordinaten > 0) {
                Text(
                    text = if (ohneKoordinaten == 1) "1 Einsatz ohne Koordinaten" else "$ohneKoordinaten Einsätze ohne Koordinaten",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                    modifier = Modifier
                        .background(Farben.BgTief.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        // Die Quellenangabe muss sichtbar bleiben — ODbL ist keine Kür.
        if (freigabe == "ja") {
            Text(
                text = stil.attribution,
                style = Schrift.Winzig,
                color = Farben.TextSehrLeise,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Farben.BgTief.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        // ----------------------------------------------------- Einwilligung
        if (freigabe == null) {
            KartenFreigabe(
                abdunkeln = false,
                beiAntwort = { ja ->
                    val wert = if (ja) "ja" else "nein"
                    freigabe = wert
                    bereich.launch { ablage.karteFreigabeSetzen(wert) }
                },
            )
        }
    }
}

// ------------------------------------------------ Leitstelle: Riss und Ortung

/** Ob die Marke blitzt — Status 3 und 7 mit Sondersignal (`faehrtMitSondersignal`). */
private fun mitBlaulicht(f: Rundenfahrzeug): Boolean =
    (f.status == 3 || f.status == 7) && !f.sondersignalAus

/** Die Peilung von einem Punkt zum nächsten, in Grad ab Norden. */
private fun peilung(vonLat: Double, vonLon: Double, nachLat: Double, nachLon: Double): Float {
    val rad = PI / 180.0
    val y = sin((nachLon - vonLon) * rad) * cos(nachLat * rad)
    val x = cos(vonLat * rad) * sin(nachLat * rad) -
        sin(vonLat * rad) * cos(nachLat * rad) * cos((nachLon - vonLon) * rad)
    return ((kotlin.math.atan2(y, x) / rad + 360.0) % 360.0).toFloat()
}

/** Eine fertige, erfolgreiche Ortung — als Kreis auf der Karte. */
private data class Kartenortung(
    val id: String,
    val nummer: Int,
    val lat: Double,
    val lon: Double,
    val radius: Double,
)

/**
 * Die Ortungen, die auf die Karte gehören — aus dem laufenden Anruf und aus dem
 * Journal, solange daraus noch kein Einsatz geworden ist. Sie ist oft die einzige
 * Spur zu diesem Notfall.
 */
private fun ortungenVon(raum: Raumzustand): List<Kartenortung> {
    val laufende = raum.anrufe.map { Triple(it.id, it.nummer, it.ortung) }
    val journal = raum.anrufjournal.filter { it.incidentId == null }.map { Triple(it.anrufId, it.nummer, it.ortung) }
    return (laufende + journal).mapNotNull { (id, nummer, o) ->
        if (o == null || o.laeuft || o.erfolgreich != true) return@mapNotNull null
        val lat = o.lat ?: return@mapNotNull null
        val lon = o.lon ?: return@mapNotNull null
        Kartenortung(id, nummer, lat, lon, (o.radiusMeter ?: 0).toDouble())
    }
}

/** Ob ein Fahrzeug „draußen" ist — dieselbe Schwelle wie im Web (~30 m). */
private fun ausgerueckt(f: Rundenfahrzeug): Boolean {
    if (f.einsatzstelleErreicht) return false
    val wLat = f.wacheLat ?: return true
    val wLon = f.wacheLon ?: return true
    return abs((f.lat ?: wLat) - wLat) > 0.0003 || abs((f.lon ?: wLon) - wLon) > 0.0003
}

/** Alle Punkte ins Bild — `fitBounds` mit Rand und Zoomdeckel 15. */
private fun zuschneiden(
    punkte: List<Pair<Double, Double>>,
    groesse: IntSize,
    setzen: (Double, Double, Double) -> Unit,
) {
    if (punkte.isEmpty() || groesse == IntSize.Zero) return
    val minLat = punkte.minOf { it.first }
    val maxLat = punkte.maxOf { it.first }
    val minLon = punkte.minOf { it.second }
    val maxLon = punkte.maxOf { it.second }
    val mitteLat = (minLat + maxLat) / 2
    val mitteLon = (minLon + maxLon) / 2

    var zoom = 15.0
    while (zoom > 4.0) {
        val welt = 256.0 * 2.0.pow(zoom)
        val breite = abs(lonZuWeltX(maxLon, welt) - lonZuWeltX(minLon, welt))
        val hoehe = abs(latZuWeltY(minLat, welt) - latZuWeltY(maxLat, welt))
        if (breite <= groesse.width - 80 && hoehe <= groesse.height - 80) break
        zoom -= 0.25
    }
    setzen(mitteLat, mitteLon, zoom)
}

// -------------------------------------------------------- Zeichen-Helfer

/** Meter in Bildschirm-Pixel an dieser Breite — die flache Näherung des Web. */
private fun DrawScope.meterZuPx(lat: Double, meter: Double, zoom: Double): Float {
    val welt = 256.0 * 2.0.pow(zoom)
    val gradJeMeter = 1.0 / 111_320.0
    val weltJeGrad = welt / 360.0 / cos(lat * PI / 180.0)
    return (meter * gradJeMeter * weltJeGrad).toFloat()
}

private var zoomFuerZeichnen = 12.0

private fun DrawScope.kreis(ort: Offset, lat: Double, meter: Double, farbe: Color) {
    val r = meterZuPx(lat, meter, zoomFuerZeichnen)
    drawCircle(farbe.copy(alpha = 0.10f), radius = r, center = ort)
    drawCircle(
        farbe,
        radius = r,
        center = ort,
        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}

private fun DrawScope.kreisMitFahne(
    ort: Offset,
    lat: Double,
    meter: Double,
    farbe: Color,
    windrichtung: Int?,
) {
    kreis(ort, lat, meter, farbe)
    // Die Fahne zeigt, wohin der Wind trägt: (Richtung + 180) % 360.
    windrichtung?.let { w ->
        sektor(ort, lat, meter, ((w + 180) % 360) - 30.0, 60.0, farbe.copy(alpha = 0.18f))
    }
}

private fun DrawScope.sektor(
    ort: Offset,
    lat: Double,
    meter: Double,
    vonGrad: Double,
    weite: Double,
    farbe: Color,
) {
    val r = meterZuPx(lat, meter, zoomFuerZeichnen)
    drawArc(
        color = farbe,
        // Compose zählt ab 3 Uhr, die Windrose ab Norden — um 90° versetzt.
        startAngle = (vonGrad - 90).toFloat(),
        sweepAngle = weite.toFloat(),
        useCenter = true,
        topLeft = ort - Offset(r, r),
        size = Size(r * 2, r * 2),
    )
}

private fun DrawScope.kreuz(ort: Offset, farbe: Color) {
    drawCircle(Color.White, radius = 6.dp.toPx(), center = ort)
    drawRect(
        farbe,
        topLeft = ort - Offset(4.dp.toPx(), 1.25.dp.toPx()),
        size = Size(8.dp.toPx(), 2.5.dp.toPx()),
    )
    drawRect(
        farbe,
        topLeft = ort - Offset(1.25.dp.toPx(), 4.dp.toPx()),
        size = Size(2.5.dp.toPx(), 8.dp.toPx()),
    )
}

@Composable
private fun Kartenknopf(zeichen: String, beiDruck: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .flaeche(ecke = 9.dp)
            .clickable(onClick = beiDruck),
    ) {
        Text(text = zeichen, style = Schrift.Normal, color = Farben.Text)
    }
}
