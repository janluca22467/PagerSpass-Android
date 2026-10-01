package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import de.pagerspass.pagerspass.mobil.packFahrzeug
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.mobil.fahrt
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.WeltPunkt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.karte.Kachelspeicher
import de.pagerspass.pagerspass.ui.karte.Kartenstil
import de.pagerspass.pagerspass.ui.karte.latZuWeltY
import de.pagerspass.pagerspass.ui.karte.lonZuWeltX
import de.pagerspass.pagerspass.ui.karte.weltXZuLon
import de.pagerspass.pagerspass.ui.karte.weltYZuLat
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * Der Ausschnitt der Weltkarte — Mitte, Zoom, Größe.
 *
 * <b>Er gehört dem Arbeitsplatz, nicht der Karte.</b> Die Lagenliste will „dort
 * hinschauen", die Gründung will nach der Stadtsuche hinfliegen — beides geht
 * nur, wenn der Ausschnitt außerhalb der Karte steht.
 */
class Weltkartenstand(lat: Double, lon: Double, zoom: Double) {
    var mitteLat by mutableStateOf(lat)
    var mitteLon by mutableStateOf(lon)
    var zoom by mutableStateOf(zoom)
    var groesse by mutableStateOf(IntSize.Zero)

    fun hinschauen(lat: Double, lon: Double, stufe: Double? = null) {
        mitteLat = lat
        mitteLon = lon
        if (stufe != null) zoom = stufe
    }

    fun weltGroesse() = 256.0 * 2.0.pow(zoom)

    fun bildschirm(lat: Double, lon: Double): Offset {
        val welt = weltGroesse()
        val dx = lonZuWeltX(lon, welt) - lonZuWeltX(mitteLon, welt)
        val dy = latZuWeltY(lat, welt) - latZuWeltY(mitteLat, welt)
        return Offset((groesse.width / 2f + dx).toFloat(), (groesse.height / 2f + dy).toFloat())
    }

    fun ort(p: Offset): WeltPunkt {
        val welt = weltGroesse()
        val x = lonZuWeltX(mitteLon, welt) + (p.x - groesse.width / 2f)
        val y = latZuWeltY(mitteLat, welt) + (p.y - groesse.height / 2f)
        return WeltPunkt(weltYZuLat(y, welt), weltXZuLon(x, welt))
    }

    /** Alle Punkte ins Bild — `fitBounds` mit Rand und Zoomdeckel. */
    fun zuschneiden(punkte: List<Pair<Double, Double>>, deckel: Double = 14.0) {
        if (punkte.isEmpty() || groesse == IntSize.Zero) return
        val minLat = punkte.minOf { it.first }
        val maxLat = punkte.maxOf { it.first }
        val minLon = punkte.minOf { it.second }
        val maxLon = punkte.maxOf { it.second }
        var z = deckel
        while (z > 3.0) {
            val welt = 256.0 * 2.0.pow(z)
            val b = abs(lonZuWeltX(maxLon, welt) - lonZuWeltX(minLon, welt))
            val h = abs(latZuWeltY(minLat, welt) - latZuWeltY(maxLat, welt))
            if (b <= groesse.width - 100 && h <= groesse.height - 160) break
            z -= 0.25
        }
        hinschauen((minLat + maxLat) / 2, (minLon + maxLon) / 2, z)
    }
}

/**
 * Welche Ebenen gezeichnet werden — dieselben acht wie `ebenen.ts`.
 *
 * Eine volle Karte aufzuräumen, ohne die Welt zu ändern: Ausgeblendet heißt
 * nicht weg, und die Zähler an der Leiste zählen weiter alles.
 */
class Weltebenen {
    var lagen by mutableStateOf(true)
    var eigene by mutableStateOf(true)
    var wachen by mutableStateOf(true)
    var pois by mutableStateOf(true)
    var wege by mutableStateOf(true)
    var fremde by mutableStateOf(true)
    var fremdeWachen by mutableStateOf(true)
    var grosslage by mutableStateOf(true)
}

/** Was ein Tipp auf die Karte gerade bedeutet. */
enum class Kartenmodus { Normal, Ort, Pfad, Gelaende }

/**
 * Die Weltkarte — nativ nachgebaut nach `Weltkarte.vue`.
 *
 * <b>Kein Leaflet, dieselbe Mathematik wie die Lagekarte.</b> Die Kacheln teilt
 * sie mit ihr (`Kachelspeicher`), damit ein Wechsel zwischen Runde und Welt
 * nicht zweimal dieselben Bilder lädt.
 *
 * <b>Die Fahrzeuge fahren zwischen zwei Abrufen selbst</b> (siehe `Weltfahrt`):
 * Die Karte zeichnet sich fünfmal je Sekunde neu und fragt jede Marke nach
 * ihrem Ort in diesem Augenblick.
 *
 * <b>Treffer in der Reihenfolge der Wichtigkeit</b>: erst Lagen, dann eigene
 * Fahrzeuge, dann eigene Wachen, eigene Punkte, fremde Wachen. Ein Tipp, der
 * zwischen einer Lage und einer Wache landet, meint fast immer die Lage.
 */
@Composable
fun Weltkarte(
    zustand: Weltzustand,
    kartenstand: Weltkartenstand,
    ebenen: Weltebenen,
    modifier: Modifier = Modifier,
    modus: Kartenmodus = Kartenmodus.Normal,
    bauradien: Boolean = false,
    markiert: WeltPunkt? = null,
    zeichnung: List<WeltPunkt> = emptyList(),
    zeichnungGeschlossen: Boolean = false,
    gewaehlteLage: String? = null,
    gewaehltesFahrzeug: String? = null,
    staedte: Boolean = false,
    polster: PaddingValues = PaddingValues(0.dp),
    beiLage: (String) -> Unit = {},
    beiFahrzeug: (String?) -> Unit = {},
    beiWache: (String) -> Unit = {},
    beiFremdwache: (String) -> Unit = {},
    beiFremdfahrzeug: (String) -> Unit = {},
    beiPoi: (String) -> Unit = {},
    beiOrt: (WeltPunkt) -> Unit = {},
    beiLangdruck: ((WeltPunkt) -> Unit)? = null,
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
        stil = Kartenstil.entries.firstOrNull { it.name == ablage.karteStil() } ?: Kartenstil.Dunkel
    }
    // Das Icon-Pack, das auf dieser Karte gilt — einmal beim Aufbau; eine neue
    // Wahl in den Einstellungen lädt es selbst nach.
    LaunchedEffect(Unit) { de.pagerspass.pagerspass.mobil.Iconpack.laden(zusammenhang) }
    var stilwahl by remember { mutableStateOf(false) }
    var ebenenwahl by remember { mutableStateOf(false) }

    // Fünfmal je Sekunde: der Takt, an dem die Fahrzeuge weiterfahren.
    var uhr by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            uhr = System.currentTimeMillis()
            delay(200)
        }
    }

    val aktuell by rememberUpdatedState(zustand)
    val aktModus by rememberUpdatedState(modus)
    val aufLage by rememberUpdatedState(beiLage)
    val aufFahrzeug by rememberUpdatedState(beiFahrzeug)
    val aufWache by rememberUpdatedState(beiWache)
    val aufFremdwache by rememberUpdatedState(beiFremdwache)
    val aufFremdfahrzeug by rememberUpdatedState(beiFremdfahrzeug)
    val aufPoi by rememberUpdatedState(beiPoi)
    val aufOrt by rememberUpdatedState(beiOrt)
    val aufLangdruck by rememberUpdatedState(beiLangdruck)

    Box(modifier = modifier.background(Farben.BgTief)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { mitte, verschiebung, faktor, _ ->
                        val k = kartenstand
                        if (faktor != 1f) {
                            val welt = k.weltGroesse()
                            val neuerZoom = (k.zoom + ln(faktor.toDouble()) / ln(2.0)).coerceIn(3.0, 18.0)
                            val anteil = 2.0.pow(neuerZoom - k.zoom)
                            val fx = mitte.x - size.width / 2f
                            val fy = mitte.y - size.height / 2f
                            val neueWelt = 256.0 * 2.0.pow(neuerZoom)
                            val mx = lonZuWeltX(k.mitteLon, welt) + fx
                            val my = latZuWeltY(k.mitteLat, welt) + fy
                            k.mitteLon = weltXZuLon(mx * anteil - fx, neueWelt)
                            k.mitteLat = weltYZuLat(my * anteil - fy, neueWelt)
                            k.zoom = neuerZoom
                        }
                        val nach = k.weltGroesse()
                        k.mitteLon = weltXZuLon(lonZuWeltX(k.mitteLon, nach) - verschiebung.x, nach)
                        k.mitteLat = weltYZuLat(
                            (latZuWeltY(k.mitteLat, nach) - verschiebung.y).coerceIn(0.0, nach),
                            nach,
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { p ->
                            if (aktModus == Kartenmodus.Normal) aufLangdruck?.invoke(kartenstand.ort(p))
                        },
                        onTap = { p ->
                            val z = aktuell
                            if (aktModus != Kartenmodus.Normal) {
                                aufOrt(kartenstand.ort(p))
                                return@detectTapGestures
                            }
                            val schwelle = with(dichte) { 22.dp.toPx() }
                            fun <T> naechster(liste: List<T>, ort: (T) -> Offset): T? =
                                liste.minByOrNull { (ort(it) - p).getDistance() }
                                    ?.takeIf { (ort(it) - p).getDistance() < schwelle }

                            if (ebenen.lagen) {
                                naechster(z.betrieb?.lagen.orEmpty()) { kartenstand.bildschirm(it.lat, it.lon) }
                                    ?.let { aufLage(it.id); return@detectTapGestures }
                            }
                            if (ebenen.eigene) {
                                val fahrt = z.fahrt
                                naechster(z.fahrzeuge.filter { it.lage != "Wache" }) {
                                    val l = fahrt.lage(it.fahrt())
                                    kartenstand.bildschirm(l.lat, l.lon)
                                }?.let { aufFahrzeug(it.id); return@detectTapGestures }
                            }
                            if (ebenen.fremde) {
                                val fahrt = z.fahrt
                                naechster(z.betrieb?.fremde.orEmpty()) {
                                    val l = fahrt.lage(it.fahrt())
                                    kartenstand.bildschirm(l.lat, l.lon)
                                }?.let { aufFremdfahrzeug(it.id); return@detectTapGestures }
                            }
                            if (ebenen.wachen) {
                                naechster(z.stand?.wachen.orEmpty()) { kartenstand.bildschirm(it.lat, it.lon) }
                                    ?.let { aufWache(it.id); return@detectTapGestures }
                            }
                            if (ebenen.pois) {
                                naechster(z.pois.filter { it.sichtbar }) { kartenstand.bildschirm(it.lat, it.lon) }
                                    ?.let { aufPoi(it.id); return@detectTapGestures }
                            }
                            if (ebenen.fremdeWachen) {
                                naechster(z.betrieb?.fremdeWachen.orEmpty()) { kartenstand.bildschirm(it.lat, it.lon) }
                                    ?.let { aufFremdwache(it.id); return@detectTapGestures }
                            }
                            aufFahrzeug(null)
                        },
                    )
                },
        ) {
            kartenstand.groesse = IntSize(size.width.toInt(), size.height.toInt())
            @Suppress("UNUSED_EXPRESSION")
            Kachelspeicher.stand
            @Suppress("UNUSED_EXPRESSION")
            uhr
            val k = kartenstand
            fun o(lat: Double, lon: Double) = k.bildschirm(lat, lon)

            // ------------------------------------------------------ Kacheln
            if (freigabe == "ja") kachelnZeichnen(k, stil, bereich)

            // ------------------------------------------------- Städtenamen
            if (staedte) staedteZeichnen(k, messer)

            // --------------------------------------------------- Bauradien
            if (bauradien) {
                val stand = zustand.stand
                val liste = stand?.bereiche?.ifEmpty { null }
                    ?.map { Triple(it.lat, it.lon, it.bauradiusMeter) }
                    ?: stand?.let { listOf(Triple(it.lat, it.lon, 35_000.0)) }.orEmpty()
                liste.forEach { (lat, lon, r) ->
                    val px = meterZuPx(lat, r, k.zoom)
                    drawCircle(Farben.Amber.copy(alpha = 0.06f), px, o(lat, lon))
                    drawCircle(
                        Farben.Amber.copy(alpha = 0.7f), px, o(lat, lon),
                        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
                    )
                }
            }

            // ---------------------------------------------- Fremde Leitstellen
            zustand.leitstellen.forEach { l ->
                val p = o(l.lat, l.lon)
                if (!imBild(p)) return@forEach
                drawCircle(Farben.BgTief, 6.dp.toPx(), p)
                drawCircle(Color(0xFF8291A5), 4.5.dp.toPx(), p)
                if (k.zoom >= 9) beschriften(messer, l.name, p + Offset(0f, 8.dp.toPx()), Farben.TextSehrLeise, 10f)
            }

            // --------------------------------------------------- Eigene Punkte
            if (ebenen.pois) {
                zustand.pois.filter { it.sichtbar }.forEach { poi ->
                    // Der Palettenton der Marke wie im Web (`poiSymbol`): 0 heißt
                    // automatisch — dann entscheidet die Kennung.
                    val farbe = if (poi.aktiv) de.pagerspass.pagerspass.ui.schmuck.Wappen.ton(poi.id, poi.farbe) else Farben.TextSehrLeise
                    if (poi.gelaende.size >= 2) {
                        val pfad = Path()
                        poi.gelaende.forEachIndexed { i, e ->
                            val p = o(e.lat, e.lon)
                            if (i == 0) pfad.moveTo(p.x, p.y) else pfad.lineTo(p.x, p.y)
                        }
                        if (!poi.strecke) {
                            pfad.close()
                            drawPath(pfad, farbe.copy(alpha = 0.12f))
                        }
                        drawPath(pfad, farbe, style = Stroke(2.dp.toPx()))
                    }
                    val p = o(poi.lat, poi.lon)
                    if (!imBild(p)) return@forEach
                    rotate(45f, p) {
                        drawRect(Farben.BgTief, p - Offset(6.dp.toPx(), 6.dp.toPx()), Size(12.dp.toPx(), 12.dp.toPx()))
                        drawRect(farbe, p - Offset(4.5.dp.toPx(), 4.5.dp.toPx()), Size(9.dp.toPx(), 9.dp.toPx()))
                    }
                    if (k.zoom >= 13) beschriften(messer, poi.name, p + Offset(0f, 9.dp.toPx()), farbe, 10f)
                }
            }

            // ------------------------------------------------ Großlage, Events
            if (ebenen.grosslage) {
                zustand.grosslage?.takeIf { it.zustand != "Vorbei" && (it.lat != 0.0 || it.lon != 0.0) }?.let { g ->
                    val p = o(g.lat, g.lon)
                    drawCircle(Farben.Signal.copy(alpha = 0.22f), 20.dp.toPx(), p)
                    drawCircle(Farben.BgTief, 12.dp.toPx(), p)
                    drawCircle(Farben.Signal, 11.dp.toPx(), p, style = Stroke(2.5.dp.toPx()))
                    beschriften(messer, "!", p - Offset(0f, 7.dp.toPx()), Farben.SignalHell, 14f, fett = true, grund = false)
                    beschriften(messer, g.name, p + Offset(0f, 14.dp.toPx()), Farben.SignalHell, 11f)
                }
                zustand.events.filter { it.zustand != "Vorbei" && it.zustand != "Entwurf" }.forEach { e ->
                    val ton = e.farbe?.let { farbeAus(it) } ?: Farben.Violett
                    val lat = e.brLat
                    val lon = e.brLon
                    if (lat != null && lon != null) {
                        val p = o(lat, lon)
                        drawRect(ton, p - Offset(7.dp.toPx(), 7.dp.toPx()), Size(14.dp.toPx(), 14.dp.toPx()), style = Stroke(2.dp.toPx()))
                        beschriften(messer, e.brName ?: "Bereitstellungsraum", p + Offset(0f, 10.dp.toPx()), ton, 10f)
                    }
                    if (e.lat != 0.0 || e.lon != 0.0) {
                        val p = o(e.lat, e.lon)
                        drawCircle(ton.copy(alpha = 0.2f), 16.dp.toPx(), p)
                        drawCircle(ton, 9.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                        beschriften(messer, e.titel, p + Offset(0f, 12.dp.toPx()), ton, 10f)
                    }
                }
            }

            // -------------------------------------------------- Fremde Wachen
            if (ebenen.fremdeWachen) {
                zustand.betrieb?.fremdeWachen.orEmpty().forEach { w ->
                    val p = o(w.lat, w.lon)
                    if (!imBild(p)) return@forEach
                    val s = 9.dp.toPx()
                    drawRect(Farben.BgTief, p - Offset(s / 2 + 1, s / 2 + 1), Size(s + 2, s + 2))
                    drawRect(Color(0xFF8291A5), p - Offset(s / 2, s / 2), Size(s, s), style = Stroke(1.5.dp.toPx()))
                    if (w.fahrzeuge > 0 && k.zoom >= 11) {
                        beschriften(messer, "${w.fahrzeuge}", p + Offset(9.dp.toPx(), -14.dp.toPx()), Farben.TextSehrLeise, 9f)
                    }
                }
            }

            // ----------------------------------------------------- Eigene Wachen
            if (ebenen.wachen) {
                val daheim = zustand.fahrzeuge.filter { it.lage == "Wache" }.groupingBy { it.wacheId }.eachCount()
                zustand.stand?.wachen.orEmpty().forEach { w ->
                    val p = o(w.lat, w.lon)
                    if (!imBild(p)) return@forEach
                    val farbe = wachenfarbe(w.art)
                    val s = 14.dp.toPx()
                    drawRoundRect(Farben.BgTief, p - Offset(s / 2 + 2, s / 2 + 2), Size(s + 4, s + 4), CornerRadius(4.dp.toPx()))
                    drawRoundRect(farbe, p - Offset(s / 2, s / 2), Size(s, s), CornerRadius(3.dp.toPx()))
                    // Das Tor im Haus — dieselbe Aussage wie das Zeichen im Web.
                    drawRect(Farben.BgTief.copy(alpha = 0.6f), p - Offset(3.dp.toPx(), -0.5.dp.toPx()), Size(6.dp.toPx(), 5.dp.toPx()))
                    val zahl = daheim[w.id] ?: 0
                    if (zahl > 0) {
                        val z = Offset(p.x + 9.dp.toPx(), p.y - 9.dp.toPx())
                        drawCircle(Farben.BgTief, 7.dp.toPx(), z)
                        drawCircle(farbe, 6.dp.toPx(), z)
                        beschriften(messer, "$zahl", z - Offset(0f, 6.dp.toPx()), Farben.AufFarbe, 9f, fett = true, grund = false)
                    }
                    if (k.zoom >= 12) beschriften(messer, w.name, p + Offset(0f, 10.dp.toPx()), Farben.Text, 10f)
                }
            }

            // -------------------------------------------- Leitstelle, Zweigstellen
            val eigeneBereiche = zustand.stand?.bereiche?.ifEmpty { null }
                ?: zustand.stand?.let { listOf(de.pagerspass.pagerspass.netz.WeltBereich(it.name, it.lat, it.lon)) }.orEmpty()
            eigeneBereiche.forEach { b ->
                val p = o(b.lat, b.lon)
                if (!imBild(p)) return@forEach
                drawCircle(Farben.BgTief, 11.dp.toPx(), p)
                drawCircle(Farben.Amber, 9.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                beschriften(messer, "★", p - Offset(0f, 8.dp.toPx()), Farben.Amber, 12f, grund = false)
                if (k.zoom >= 8) beschriften(messer, b.name, p + Offset(0f, 12.dp.toPx()), Farben.AmberHell, 10f)
            }

            // ------------------------------------------------- Fremde Fahrzeuge
            val fahrt = zustand.fahrt
            if (ebenen.fremde) {
                zustand.betrieb?.fremde.orEmpty().forEach { f ->
                    val l = fahrt.lage(f.fahrt())
                    val p = o(l.lat, l.lon)
                    if (!imBild(p)) return@forEach
                    if (packFahrzeug(f.vorlageId, p, l.kurs.toFloat(), f.blaulicht, (uhr / 400) % 2 == 0L)) return@forEach
                    drawCircle(Farben.BgTief, 5.dp.toPx(), p)
                    drawCircle(Color(0xFF8291A5), 3.5.dp.toPx(), p)
                    if (f.blaulicht && (uhr / 400) % 2 == 0L) {
                        drawCircle(Color(0xFF4D8DFF), 6.5.dp.toPx(), p, style = Stroke(1.5.dp.toPx()))
                    }
                }
            }

            // ------------------------------------------------- Gewählter Weg
            val gewaehlt = zustand.fahrzeuge.firstOrNull { it.id == gewaehltesFahrzeug }
            if (ebenen.wege && gewaehlt != null && gewaehlt.lage != "Wache") {
                val punkte = de.pagerspass.pagerspass.mobil.Weltfahrt.strecke(gewaehlt.route)
                val jetzt = fahrt.stand(gewaehlt.fahrt())
                val pfad = Path()
                val start = o(jetzt.lat, jetzt.lon)
                pfad.moveTo(start.x, start.y)
                if (punkte.size > 1) {
                    punkte.drop(jetzt.index.coerceAtMost(punkte.size)).forEach { q ->
                        val p = o(q[0], q[1]); pfad.lineTo(p.x, p.y)
                    }
                } else {
                    val p = o(gewaehlt.zielLat, gewaehlt.zielLon); pfad.lineTo(p.x, p.y)
                }
                drawPath(pfad, Farben.Amber, style = Stroke(3.dp.toPx(), pathEffect = if (punkte.size > 1) null else PathEffect.dashPathEffect(floatArrayOf(14f, 12f))), alpha = 0.85f)
            }

            // ------------------------------------------------- Eigene Fahrzeuge
            if (ebenen.eigene) {
                zustand.fahrzeuge.filter { it.lage != "Wache" }.forEach { f ->
                    val l = fahrt.lage(f.fahrt())
                    val p = o(l.lat, l.lon)
                    if (!imBild(p)) return@forEach
                    val farbe = orgFarbe(zustand.vorlage(f.vorlageId)?.organisation)
                    // Das Icon des gewählten Packs — oder, wo es keins gibt, der Pfeil.
                    if (!packFahrzeug(f.vorlageId, p, l.kurs.toFloat(), f.blaulicht, (uhr / 400) % 2 == 0L)) {
                        rotate(l.kurs.toFloat(), p) {
                            val pfeil = Path().apply {
                                moveTo(p.x, p.y - 9.dp.toPx())
                                lineTo(p.x + 6.dp.toPx(), p.y + 6.dp.toPx())
                                lineTo(p.x, p.y + 3.dp.toPx())
                                lineTo(p.x - 6.dp.toPx(), p.y + 6.dp.toPx())
                                close()
                            }
                            drawPath(pfeil, Farben.BgTief, style = Stroke(3.dp.toPx()))
                            drawPath(pfeil, farbe, style = Fill)
                        }
                        if (f.blaulicht && (uhr / 400) % 2 == 0L) {
                            drawCircle(Color(0xFF4D8DFF), 11.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                        }
                    }
                    if (f.id == gewaehltesFahrzeug) {
                        drawCircle(Farben.Amber, 13.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                    }
                    if (k.zoom >= 11) beschriften(messer, f.funkrufname, p + Offset(0f, 10.dp.toPx()), Farben.Text, 9.5f)
                }
            }

            // ----------------------------------------------------------- Lagen
            if (ebenen.lagen) {
                zustand.betrieb?.lagen.orEmpty().sortedBy { it.id == gewaehlteLage }.forEach { lage ->
                    val p = o(lage.lat, lage.lon)
                    if (!imBild(p)) return@forEach
                    val farbe = prioFarbe(lage.prioritaet)
                    val erledigt = lage.zustand == "Erledigt"
                    val meine = lage.zustaendig && !lage.freigegeben
                    val an = lage.id == gewaehlteLage
                    val alpha = if (erledigt) 0.5f else 1f
                    if (lage.prioritaet >= 3 && !erledigt) drawCircle(farbe.copy(alpha = 0.22f), 20.dp.toPx(), p)
                    if (meine) drawCircle(Farben.Amber, 14.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                    val text = messer.measure(
                        deckungstext(lage),
                        TextStyle(color = if (an) Farben.AufFarbe else farbe, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    )
                    val breite = maxOf(text.size.width + 10.dp.toPx(), 22.dp.toPx())
                    val hoehe = 18.dp.toPx()
                    val ecke = p - Offset(breite / 2, hoehe / 2)
                    drawRoundRect(Farben.BgTief, ecke - Offset(1.5f, 1.5f), Size(breite + 3, hoehe + 3), CornerRadius(hoehe))
                    drawRoundRect(
                        farbe.copy(alpha = alpha),
                        ecke,
                        Size(breite, hoehe),
                        CornerRadius(hoehe),
                        style = if (an || gedeckt(lage)) Fill else Stroke(2.dp.toPx()),
                        alpha = alpha,
                    )
                    drawText(
                        text,
                        color = if (an || gedeckt(lage)) Farben.AufFarbe else farbe,
                        topLeft = p - Offset(text.size.width / 2f, text.size.height / 2f),
                        alpha = alpha,
                    )
                    if (lage.istTageslage || lage.ausGrosslage) {
                        drawCircle(Farben.Signal, 4.dp.toPx(), ecke + Offset(breite, 0f))
                    }
                    if (an || k.zoom >= 13) {
                        beschriften(messer, lage.stichwort, p + Offset(0f, 12.dp.toPx()), Farben.Text, 10f)
                    }
                }
            }

            // ------------------------------------------------ Zeichnung, Bauort
            if (zeichnung.isNotEmpty()) {
                val pfad = Path()
                zeichnung.forEachIndexed { i, e ->
                    val p = o(e.lat, e.lon)
                    if (i == 0) pfad.moveTo(p.x, p.y) else pfad.lineTo(p.x, p.y)
                }
                if (zeichnungGeschlossen && zeichnung.size >= 3) {
                    pfad.close()
                    drawPath(pfad, Farben.Amber.copy(alpha = 0.12f))
                }
                drawPath(pfad, Farben.Amber, style = Stroke(2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))))
                zeichnung.forEachIndexed { i, e ->
                    val p = o(e.lat, e.lon)
                    drawCircle(Farben.BgTief, 9.dp.toPx(), p)
                    drawCircle(Farben.Amber, 8.dp.toPx(), p)
                    beschriften(messer, "${i + 1}", p - Offset(0f, 6.dp.toPx()), Farben.AufAmber, 9f, fett = true, grund = false)
                }
            }
            markiert?.let { m ->
                val p = o(m.lat, m.lon)
                drawCircle(Farben.Amber.copy(alpha = 0.25f), 18.dp.toPx(), p)
                drawCircle(Farben.BgTief, 13.dp.toPx(), p)
                drawCircle(Farben.Amber, 12.dp.toPx(), p, style = Stroke(2.5.dp.toPx()))
                beschriften(messer, "★", p - Offset(0f, 9.dp.toPx()), Farben.Amber, 14f, grund = false)
            }
        }

        // ------------------------------------------------------------ Knöpfe
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            horizontalAlignment = Alignment.End,
            modifier = Modifier.align(Alignment.TopEnd).padding(polster).padding(Abstand.Normal),
        ) {
            Kartenknopf("+") { kartenstand.zoom = (kartenstand.zoom + 1).coerceAtMost(18.0) }
            Kartenknopf("−") { kartenstand.zoom = (kartenstand.zoom - 1).coerceAtLeast(3.0) }
            if (zustand.stand != null) {
                Kartenknopf("⌖") {
                    val s = zustand.stand
                    val punkte = s.wachen.map { it.lat to it.lon } + (s.lat to s.lon)
                    kartenstand.zuschneiden(punkte, 13.0)
                }
            }
            Kartenknopf("◐") { stilwahl = !stilwahl; ebenenwahl = false }
            if (zustand.stand != null) Kartenknopf("≣") { ebenenwahl = !ebenenwahl; stilwahl = false }
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
                    modifier = Modifier.flaeche(ecke = 9.dp).padding(Abstand.Klein).widthIn(max = 240.dp),
                ) {
                    @Composable
                    fun zeile(name: String, an: Boolean, setzen: (Boolean) -> Unit) {
                        Text(
                            text = (if (an) "☑ " else "☐ ") + name,
                            style = Schrift.Klein,
                            color = Farben.Text,
                            modifier = Modifier.clickable { setzen(!an) }.padding(Abstand.Klein),
                        )
                    }
                    zeile("Lagen", ebenen.lagen) { ebenen.lagen = it }
                    zeile("Eigene Fahrzeuge", ebenen.eigene) { ebenen.eigene = it }
                    zeile("Eigene Wachen", ebenen.wachen) { ebenen.wachen = it }
                    zeile("Eigene Punkte", ebenen.pois) { ebenen.pois = it }
                    zeile("Fahrweg beim Antippen", ebenen.wege) { ebenen.wege = it }
                    zeile("Fremde Fahrzeuge", ebenen.fremde) { ebenen.fremde = it }
                    zeile("Fremde Wachen", ebenen.fremdeWachen) { ebenen.fremdeWachen = it }
                    zeile("Großeinsatz und Events", ebenen.grosslage) { ebenen.grosslage = it }
                }
            }
        }

        // Die Quellenangabe muss sichtbar bleiben — ODbL ist keine Kür.
        if (freigabe == "ja") {
            Text(
                text = stil.attribution,
                style = Schrift.Winzig,
                color = Farben.TextSehrLeise,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(polster)
                    .background(Farben.BgTief.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        // ----------------------------------------------------- Einwilligung
        if (freigabe == null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Normal, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Farben.BgTief.copy(alpha = 0.92f))
                    .padding(Abstand.Gross),
            ) {
                Text(
                    text = "Die Kartenkacheln kommen von CARTO und Esri — dabei geht deine IP-Adresse an deren Server.",
                    style = Schrift.Klein,
                    color = Farben.Text,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                    Knopf("Karte laden", {
                        freigabe = "ja"
                        bereich.launch { ablage.karteFreigabeSetzen("ja") }
                    }, kompakt = true)
                    Knopf("Ohne Karte", {
                        freigabe = "nein"
                        bereich.launch { ablage.karteFreigabeSetzen("nein") }
                    }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
    }
}

// ----------------------------------------------------------- Zeichen-Helfer

private fun DrawScope.imBild(p: Offset): Boolean =
    p.x > -60 && p.y > -60 && p.x < size.width + 60 && p.y < size.height + 60

private fun DrawScope.kachelnZeichnen(
    k: Weltkartenstand,
    stil: Kartenstil,
    bereich: kotlinx.coroutines.CoroutineScope,
) {
    val kachelZ = floor(k.zoom).toInt().coerceIn(3, stil.maxZoom)
    val skala = 2.0.pow(k.zoom - kachelZ)
    val kachelPx = (256.0 * skala).toFloat()
    val welt = k.weltGroesse()
    val links = lonZuWeltX(k.mitteLon, welt) - size.width / 2.0
    val oben = latZuWeltY(k.mitteLat, welt) - size.height / 2.0
    val vonX = floor(links / (256.0 * skala)).toInt()
    val vonY = floor(oben / (256.0 * skala)).toInt()
    val bisX = floor((links + size.width) / (256.0 * skala)).toInt()
    val bisY = floor((oben + size.height) / (256.0 * skala)).toInt()
    val jeReihe = 1 shl kachelZ
    for (x in vonX..bisX) for (y in vonY..bisY) {
        if (y < 0 || y >= jeReihe) continue
        val xn = ((x % jeReihe) + jeReihe) % jeReihe
        val ziel = Offset((x * kachelPx - links).toFloat(), (y * kachelPx - oben).toFloat())
        listOfNotNull(
            stil.url(stil.quelle, kachelZ, xn, y),
            stil.beschriftung?.let { stil.url(it, kachelZ, xn, y) },
        ).forEach { url ->
            val bild = Kachelspeicher.fertig[url]
            if (bild == null) {
                Kachelspeicher.anfordern(url, bereich)
            } else {
                drawImage(
                    image = bild,
                    dstOffset = IntOffset(ziel.x.toInt(), ziel.y.toInt()),
                    dstSize = IntSize((kachelPx + 1).toInt(), (kachelPx + 1).toInt()),
                )
            }
        }
    }
}

/**
 * Die Städtenamen der Gründungskarte — nach Rang und Zoom, und ein Name, der
 * einen schon gesetzten überdecken würde, bleibt weg (wie `ortsnamenAnhaengen`).
 */
private fun DrawScope.staedteZeichnen(k: Weltkartenstand, messer: TextMeasurer) {
    val gesetzt = mutableListOf<androidx.compose.ui.geometry.Rect>()
    STAEDTE.forEach { s ->
        if (k.zoom < stadtAbZoom(s.rang)) return@forEach
        val p = k.bildschirm(s.lat, s.lon)
        if (!imBild(p)) return@forEach
        val text = messer.measure(
            s.name.uppercase(),
            TextStyle(
                color = Farben.TextLeise,
                fontSize = if (s.rang == 1) 11.sp else 9.5.sp,
                fontWeight = if (s.rang <= 2) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.6.sp,
            ),
        )
        val kasten = androidx.compose.ui.geometry.Rect(
            p.x - 5, p.y - text.size.height / 2f, p.x + 8 + text.size.width, p.y + text.size.height / 2f,
        )
        if (gesetzt.any { it.overlaps(kasten) }) return@forEach
        gesetzt += kasten
        drawCircle(Farben.TextLeise, 2.5.dp.toPx(), p)
        drawRect(
            Farben.BgTief.copy(alpha = 0.55f),
            Offset(p.x + 5, p.y - text.size.height / 2f),
            Size(text.size.width + 4f, text.size.height.toFloat()),
        )
        drawText(text, topLeft = Offset(p.x + 7, p.y - text.size.height / 2f))
    }
}

/** Eine Beschriftung, mittig unter einem Punkt, auf halbdunklem Grund. */
private fun DrawScope.beschriften(
    messer: TextMeasurer,
    text: String,
    oben: Offset,
    farbe: Color,
    groesse: Float,
    fett: Boolean = false,
    grund: Boolean = true,
) {
    if (text.isBlank()) return
    val t = messer.measure(
        text,
        TextStyle(color = farbe, fontSize = groesse.sp, fontWeight = if (fett) FontWeight.Bold else FontWeight.Medium),
        maxLines = 1,
    )
    val links = oben.x - t.size.width / 2f
    if (grund) {
        drawRoundRect(
            Farben.BgTief.copy(alpha = 0.72f),
            Offset(links - 3.dp.toPx(), oben.y),
            Size(t.size.width + 6.dp.toPx(), t.size.height.toFloat()),
            CornerRadius(3.dp.toPx()),
        )
    }
    drawText(t, topLeft = Offset(links, oben.y))
}

private fun DrawScope.meterZuPx(lat: Double, meter: Double, zoom: Double): Float {
    val welt = 256.0 * 2.0.pow(zoom)
    return (meter / 111_320.0 * welt / 360.0 / cos(lat * PI / 180.0)).toFloat()
}

/** `#rrggbb` aus den Eventfarben; alles andere fällt auf Violett zurück. */
internal fun farbeAus(roh: String): Color? = runCatching {
    val hex = roh.trim().removePrefix("#")
    if (hex.length != 6) return null
    Color(0xFF000000 or hex.toLong(16))
}.getOrNull()

@Composable
private fun Kartenknopf(zeichen: String, beiDruck: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp).flaeche(ecke = 9.dp).clickable(onClick = beiDruck),
    ) {
        Text(text = zeichen, style = Schrift.Normal, color = Farben.Text)
    }
}
