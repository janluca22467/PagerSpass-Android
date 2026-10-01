package de.pagerspass.pagerspass.ui.karte

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * Ein Punkt auf der [Punktkarte] — eine Wache, ein Einsatzort, ein Standort.
 *
 * @param gewaehlt Gewählt heißt voll gefüllt; ungewählt steht der Punkt hohl da
 *   (dieselbe Unterscheidung wie die Wachenmarken im Leitstellenbau des Webs).
 * @param ziel Der Einsatzort: größer, mit pulsendem Hof.
 */
data class Kartenpunkt(
    val id: String,
    val lat: Double,
    val lon: Double,
    val farbe: Color,
    val beschriftung: String = "",
    val gewaehlt: Boolean = true,
    val hervorgehoben: Boolean = false,
    val ziel: Boolean = false,
)

/**
 * Der Ausschnitt der Punktkarte — außerhalb der Karte, damit eine Liste daneben
 * „dort hinschauen" kann (wie `Weltkartenstand`).
 */
class Punktkartenstand(lat: Double, lon: Double, zoom: Double) {
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

    /** Der Ort unter einem Bildschirmpunkt — für „hier hinstellen". */
    fun ort(p: Offset): Pair<Double, Double> {
        val welt = weltGroesse()
        val x = lonZuWeltX(mitteLon, welt) + (p.x - groesse.width / 2f)
        val y = latZuWeltY(mitteLat, welt) + (p.y - groesse.height / 2f)
        return weltYZuLat(y, welt) to weltXZuLon(x, welt)
    }

    /** Alle Punkte ins Bild — `fitBounds` mit Rand und Zoomdeckel. */
    fun zuschneiden(punkte: List<Pair<Double, Double>>, deckel: Double = 14.0) {
        if (punkte.isEmpty() || groesse == IntSize.Zero) return
        val minLat = punkte.minOf { it.first }
        val maxLat = punkte.maxOf { it.first }
        val minLon = punkte.minOf { it.second }
        val maxLon = punkte.maxOf { it.second }
        var z = deckel
        while (z > 4.0) {
            val welt = 256.0 * 2.0.pow(z)
            val b = abs(lonZuWeltX(maxLon, welt) - lonZuWeltX(minLon, welt))
            val h = abs(latZuWeltY(minLat, welt) - latZuWeltY(maxLat, welt))
            if (b <= groesse.width - 60 && h <= groesse.height - 60) break
            z -= 0.25
        }
        hinschauen((minLat + maxLat) / 2, (minLon + maxLon) / 2, z)
    }
}

/**
 * Eine Kachelkarte mit Punkten — für die Stellen, an denen keine Runde läuft.
 *
 * Die [Lagekarte] zeichnet einen Raumzustand, die Weltkarte eine Welt; hier
 * braucht es nur Punkte: die Wachen eines Kreises im Leitstellenbau, den
 * Einsatzort auf dem Alarmmonitor. <b>Dieselben Kacheln, derselbe Speicher</b>
 * ([Kachelspeicher]), dieselbe Mathematik — und <b>dieselbe Einwilligung</b>:
 * Ohne „ja" wird keine Kachel geholt; die Punkte stehen dann auf dunklem Grund
 * trotzdem an der richtigen Stelle. Ein Gehäuse hebt keine Einwilligung auf.
 *
 * @param gesten Ob man schieben und zoomen darf. Der Monitor im Melder steht
 *   still — ein Wischen darüber meint die Seite, nicht die Karte.
 * @param beiPunkt Ein Tipp nahe an einem Punkt, mit seiner Kennung.
 * @param beiOrt Ein Tipp daneben, mit dem Ort darunter.
 * @param frage Ob die Karte selbst nach der Einwilligung fragt. Klein (im
 *   Melder) fragt sie nicht, sondern sagt nur, dass die Kacheln fehlen.
 */
@Composable
fun Punktkarte(
    punkte: List<Kartenpunkt>,
    stand: Punktkartenstand,
    modifier: Modifier = Modifier,
    gesten: Boolean = true,
    frage: Boolean = true,
    knoepfe: Boolean = true,
    beiPunkt: ((String) -> Unit)? = null,
    beiOrt: ((Double, Double) -> Unit)? = null,
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

    val aktuell by rememberUpdatedState(punkte)
    val aufPunkt by rememberUpdatedState(beiPunkt)
    val aufOrt by rememberUpdatedState(beiOrt)

    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Farben.BgTief)) {
        var leinwand = Modifier.fillMaxSize()
        if (gesten) {
            leinwand = leinwand.pointerInput(Unit) {
                detectTransformGestures { mitte, verschiebung, faktor, _ ->
                    val k = stand
                    if (faktor != 1f) {
                        val welt = k.weltGroesse()
                        val neuerZoom = (k.zoom + ln(faktor.toDouble()) / ln(2.0)).coerceIn(4.0, stil.maxZoom.toDouble())
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
                    k.mitteLat = weltYZuLat((latZuWeltY(k.mitteLat, nach) - verschiebung.y).coerceIn(0.0, nach), nach)
                }
            }
        }
        if (beiPunkt != null || beiOrt != null) {
            leinwand = leinwand.pointerInput(Unit) {
                detectTapGestures { p ->
                    val schwelle = with(dichte) { 22.dp.toPx() }
                    val treffer = aktuell
                        .minByOrNull { (stand.bildschirm(it.lat, it.lon) - p).getDistance() }
                        ?.takeIf { (stand.bildschirm(it.lat, it.lon) - p).getDistance() < schwelle }
                    val punkt = aufPunkt
                    val ort = aufOrt
                    when {
                        treffer != null && punkt != null -> punkt(treffer.id)
                        ort != null -> stand.ort(p).let { (la, lo) -> ort(la, lo) }
                    }
                }
            }
        }

        Canvas(modifier = leinwand) {
            stand.groesse = IntSize(size.width.toInt(), size.height.toInt())
            @Suppress("UNUSED_EXPRESSION")
            Kachelspeicher.stand

            if (freigabe == "ja") kacheln(stand, stil, bereich)

            // Erst die ungewählten, dann die gewählten, zuletzt die hervorgehobene —
            // was zählt, liegt oben.
            punkte.sortedBy { (if (it.gewaehlt) 1 else 0) + (if (it.hervorgehoben || it.ziel) 2 else 0) }.forEach { p ->
                val o = stand.bildschirm(p.lat, p.lon)
                if (o.x < -40 || o.y < -40 || o.x > size.width + 40 || o.y > size.height + 40) return@forEach
                if (p.ziel) {
                    drawCircle(p.farbe.copy(alpha = 0.25f), 14.dp.toPx(), o)
                    drawCircle(Farben.BgTief, 7.dp.toPx(), o)
                    drawCircle(p.farbe, 5.5.dp.toPx(), o)
                } else {
                    val r = if (p.hervorgehoben) 8.dp.toPx() else 6.dp.toPx()
                    if (p.hervorgehoben) drawCircle(Farben.Amber.copy(alpha = 0.35f), r + 5.dp.toPx(), o)
                    drawCircle(Farben.BgTief, r + 1.5.dp.toPx(), o)
                    if (p.gewaehlt) {
                        drawCircle(p.farbe, r, o)
                    } else {
                        drawCircle(p.farbe.copy(alpha = 0.18f), r, o)
                        drawCircle(p.farbe, r - 1.dp.toPx(), o, style = Stroke(1.5.dp.toPx()))
                    }
                }
                if (p.beschriftung.isNotBlank() && (p.gewaehlt || p.hervorgehoben || stand.zoom >= 12.5)) {
                    beschriften(messer, p.beschriftung, o + Offset(0f, 9.dp.toPx()), if (p.gewaehlt) Farben.Text else Farben.TextLeise)
                }
            }
        }

        if (knoepfe && gesten) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.align(Alignment.TopEnd).padding(Abstand.Klein),
            ) {
                Kartenknopf("＋") { stand.zoom = (stand.zoom + 1).coerceAtMost(stil.maxZoom.toDouble()) }
                Kartenknopf("－") { stand.zoom = (stand.zoom - 1).coerceAtLeast(4.0) }
                Kartenknopf("⛶") { stand.zuschneiden(punkte.map { it.lat to it.lon }) }
            }
        }

        // Die Quellenangabe muss sichtbar bleiben — ODbL ist keine Kür.
        if (freigabe == "ja") {
            Text(
                text = stil.attribution,
                style = Schrift.Winzig,
                color = Farben.TextSehrLeise,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Farben.BgTief.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }

        if (freigabe == null && frage) {
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
        } else if (freigabe != "ja" && freigabe != "laedt" && !frage) {
            // Klein fragt die Karte nicht — sie sagt nur, warum der Grund dunkel ist.
            Text(
                text = "ohne Kartenfreigabe",
                style = Schrift.Winzig,
                color = Farben.TextSehrLeise,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

private fun DrawScope.kacheln(k: Punktkartenstand, stil: Kartenstil, bereich: kotlinx.coroutines.CoroutineScope) {
    val kachelZ = floor(k.zoom).toInt().coerceIn(4, stil.maxZoom)
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

private fun DrawScope.beschriften(messer: TextMeasurer, text: String, oben: Offset, farbe: Color) {
    val t = messer.measure(text, TextStyle(color = farbe, fontSize = 10.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    val links = oben.x - t.size.width / 2f
    drawRoundRect(
        Farben.BgTief.copy(alpha = 0.72f),
        Offset(links - 3.dp.toPx(), oben.y),
        Size(t.size.width + 6.dp.toPx(), t.size.height.toFloat()),
        CornerRadius(3.dp.toPx()),
    )
    drawText(t, topLeft = Offset(links, oben.y))
}

@Composable
private fun Kartenknopf(zeichen: String, beiDruck: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp).flaeche(ecke = 9.dp).clickable(onClick = beiDruck),
    ) {
        Text(text = zeichen, style = Schrift.Normal, color = Farben.Text)
    }
}
