package de.pagerspass.pagerspass.ui.karte

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.CoroutineScope
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
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

/*
 * Die allgemeine Karte — der Kern, den `Lagekarte` für die Runde hat, herausgelöst für
 * alles, was keine Runde ist: die Gründungskarte und die Weltkarte von World.
 *
 * <b>Kein Leaflet, kein WebView.</b> Dieselbe Rechnung wie in der Lagekarte:
 * Web-Mercator rechnet Grad in Weltpixel, ein Canvas zeichnet die Kacheln des
 * sichtbaren Ausschnitts und darüber, was der Aufrufer ihm gibt. Was Leaflet an Wert
 * hätte, sind seine Gesten — und die hat Compose selbst.
 *
 * <b>Anders als die Lagekarte rechnet sie in Punkten, nicht in Bildpunkten.</b> Eine
 * Kachel ist 256 dp groß, genau wie Leaflet in CSS-Pixeln rechnet — damit bedeutet
 * „Zoomstufe 11" hier dasselbe wie im Web, und die Zahlen aus der Vorlage (Klumpen bis
 * Stufe 9, Hinschauen auf 13) gelten unverändert.
 *
 * <b>Was sie kann</b>, und warum es genau das ist, was die Weltkarte verlangt:
 *  * Marken mit eigener Zeichnung (`Kartenmarke`) — gedrehte Fahrzeugrisse, Wachen,
 *    Lagen mit Zahl — samt Trefferprüfung beim Antippen,
 *  * Klumpen (`klumpen = true`) bis zu einer Zoomstufe, im Raster von 44 Punkten wie
 *    `klumpenBilden` in `Weltkarte.vue`,
 *  * Linien, Flächen und Kreise in Metern (`Kartenform`),
 *  * einen Tipp auf die freie Karte als Koordinate (`beiKartentipp`) — für Bauplatz,
 *    Streifenpfad und Geländeecken,
 *  * Einpassen (`Kartenzustand.zuschneiden`), Ansichtswahl und ein Ebenenmenü.
 *
 * <b>Kacheln erst nach Einwilligung.</b> Dieselbe Regel wie im Web
 * (`recht/kartenfreigabe.ts`): Ohne „ja" wird kein einziger Kachel-Abruf gemacht;
 * Marken, Strecken und Radien stehen trotzdem auf dunklem Grund.
 */

// -------------------------------------------------------------- Kachelquellen

/**
 * Die vier Ansichten — übertragen aus `KARTENANSICHTEN` in `recht/kartenfreigabe.ts`.
 *
 * <b>Zwei Quellen.</b> Die gezeichneten Ansichten („Dunkel", „Hell" und die Beschriftung
 * der Hybridansicht) kommen seit 25.09.2026 vom eigenen Kachelserver
 * (karte.pagerspass.de, TileServer GL, nur Europa). Das Luftbild kommt weiterhin von
 * Esri in den USA — dafür steht die Einwilligung.
 *
 * <b>Achtung bei einer neuen Ansicht:</b> Kommt ein Anbieter dazu, gehört er in den
 * Text der Freigabefrage (`KartenFreigabe` unten) und in Ziffer 11 der
 * Datenschutzerklärung.
 */
enum class Kartenstil(
    val titel: String,
    val maxZoom: Int,
    val quelle: String,
    /** Die zweite Ebene der Hybridkarte — ohne sie fehlen die Straßennamen. */
    val beschriftung: String? = null,
    /** Ob der Untergrund hell ist — die Marken brauchen dann einen dunklen Saum. */
    val hell: Boolean = false,
) {
    Dunkel("Dunkel", 19, KACHELSERVER + "/dunkel/{z}/{x}/{y}{r}.png?v=" + KACHEL_FASSUNG),
    Hell("Hell", 19, KACHELSERVER + "/hell/{z}/{x}/{y}{r}.png?v=" + KACHEL_FASSUNG, hell = true),

    // Das Luftbild hört eine Stufe früher auf als die gezeichnete Karte; darüber liefert
    // Esri nichts mehr. Esri stellt y vor x — die Falle dieser Vorlage.
    Satellit("Satellit", 18, ESRI_LUFTBILD, hell = true),
    Hybrid(
        "Hybrid",
        18,
        ESRI_LUFTBILD,
        KACHELSERVER + "/beschriftung/{z}/{x}/{y}{r}.png?v=" + KACHEL_FASSUNG,
        hell = true,
    ),
    ;

    /**
     * Die Adresse einer einzelnen Kachel.
     *
     * `{r}` holt die doppelte Auflösung — ein Handybildschirm ist immer ein scharfer.
     * Ein `{s}` zum Verteilen auf mehrere Namen braucht es nicht mehr: karte.pagerspass.de
     * spricht HTTP/2.
     */
    fun url(vorlage: String, z: Int, x: Int, y: Int): String = vorlage
        .replace("{z}", z.toString())
        .replace("{x}", x.toString())
        .replace("{y}", y.toString())
        .replace("{r}", "@2x")

    /** Die Quellenangabe — ODbL ist keine Kür. */
    val attribution: String
        get() = when (this) {
            Dunkel, Hell -> KACHEL_ATTRIBUTION
            Satellit -> ESRI_ATTRIBUTION
            Hybrid -> "$ESRI_ATTRIBUTION, $KACHEL_ATTRIBUTION"
        }
}

/** Der eigene Kachelserver (siehe `deploy-karte.sh` und `karte.pagerspass.de.conf`). */
private const val KACHELSERVER = "https://karte.pagerspass.de/styles"

/**
 * Das Zählwerk an der Kacheladresse — dieselbe Zahl wie `KACHEL_FASSUNG` im Web.
 * Hochzählen, wenn auf den Geräten falsche Kacheln liegen, die von selbst nicht mehr
 * weggehen: Eine neue Zahl ist eine neue Adresse, und die hat niemand im Speicher.
 */
private const val KACHEL_FASSUNG = "2"

private const val KACHEL_ATTRIBUTION = "© OpenStreetMap-Mitwirkende, © OpenMapTiles"

private const val ESRI_LUFTBILD =
    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"

private const val ESRI_ATTRIBUTION = "© Esri, Maxar, Earthstar Geographics"

/**
 * Der Kachelspeicher — lädt einmal, hält die letzten Kacheln, meldet sich über
 * Compose-State zurück, wenn eine fertig ist. Geteilt von allen Karten der App.
 */
internal object Kachelspeicher {
    val fertig = mutableStateMapOf<String, ImageBitmap>()

    /**
     * Der Zeichen-Anstoß. <b>Das Lesen fehlender Schlüssel aus der State-Map stößt das
     * Neuzeichnen nicht zuverlässig an</b> — gemessen: Kacheln kamen an, der Canvas
     * blieb schwarz, bis irgendein anderer Zustand ihn neu zeichnete. Dieser Zähler wird
     * bei jeder angekommenen Kachel erhöht und im Canvas gelesen — das reicht.
     */
    var stand by mutableStateOf(0)
        private set

    fun angekommen() {
        stand += 1
    }

    private val unterwegs = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val reihenfolge = ArrayDeque<String>()
    private val gleichzeitig = Semaphore(6)

    fun anfordern(url: String, bereich: CoroutineScope) {
        if (fertig.containsKey(url) || !unterwegs.add(url)) return
        bereich.launch(Dispatchers.IO) {
            gleichzeitig.withPermit {
                val bild = runCatching {
                    val verbindung = URL(url).openConnection() as HttpURLConnection
                    verbindung.connectTimeout = 8_000
                    verbindung.readTimeout = 8_000
                    verbindung.inputStream.use { BitmapFactory.decodeStream(it) }
                }.onFailure {
                    android.util.Log.w("Karte", "Kachel $url", it)
                }.getOrNull()?.asImageBitmap()

                if (bild != null) {
                    withContext(Dispatchers.Main) {
                        fertig[url] = bild
                        angekommen()
                        reihenfolge.addLast(url)
                        // Die ältesten fliegen raus — 160 Kacheln sind mehr als zwei
                        // volle Bildschirme in beiden Ebenen des Hybrids.
                        while (reihenfolge.size > 160) {
                            fertig.remove(reihenfolge.removeFirst())
                        }
                    }
                }
                unterwegs.remove(url)
            }
        }
    }
}

// ------------------------------------------------------------- Web-Mercator

internal fun mercatorX(lon: Double, welt: Double) = (lon + 180.0) / 360.0 * welt

internal fun mercatorY(lat: Double, welt: Double): Double {
    // Die Nordsüdgrenze von Web-Mercator — darüber wird die Projektion unendlich.
    val rad = lat.coerceIn(-85.05, 85.05) * PI / 180.0
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * welt
}

internal fun mercatorLon(x: Double, welt: Double) = x / welt * 360.0 - 180.0

internal fun mercatorLat(y: Double, welt: Double): Double {
    val n = PI - 2.0 * PI * y / welt
    return 180.0 / PI * atan(0.5 * (exp(n) - exp(-n)))
}

// ------------------------------------------------------------------ Zustand

/**
 * Was am Rand der Karte dauerhaft verdeckt ist, in Bildpunkten — Kopfstreifen oben,
 * Reiterleiste unten. `zuschneiden` und `hinschauen` rechnen es ein, sonst landet die
 * Lage, auf die man springt, hinter dem Guthaben (siehe `verdeckt` in `Weltkarte.vue`).
 */
data class Kartenrand(
    val links: Float = 0f,
    val oben: Float = 0f,
    val rechts: Float = 0f,
    val unten: Float = 0f,
)

/**
 * Der Zustand einer Karte: Ausschnitt, Größe, Ansicht und Einwilligung.
 *
 * <b>Er liegt außerhalb des Canvas</b>, damit die Seite darum ihn lenken kann — auf
 * eine Stadt fliegen, eine Lage in die Mitte holen, alles einpassen. Die Karte selbst
 * schreibt nur Größe und Dichte hinein.
 */
@Stable
class Kartenzustand(lat: Double, lon: Double, zoom: Double) {
    var mitteLat by mutableStateOf(lat)
    var mitteLon by mutableStateOf(lon)
    var zoom by mutableStateOf(zoom)

    /** Die Größe des Canvas in Bildpunkten — null, bis er einmal gemessen ist. */
    var groesse by mutableStateOf(IntSize.Zero)
        internal set

    /** Bildpunkte je Punkt — die Kachel ist 256 Punkte groß. */
    var dichte: Float = 1f
        internal set

    /** Die gewählte Ansicht — gilt geräteweit für alle Karten gemeinsam (`Ablage.karteStil`). */
    var stil by mutableStateOf(Kartenstil.Dunkel)

    /** Die Einwilligung: `"ja"`, `"nein"`, `null` (noch nicht gefragt) oder `"laedt"`. */
    var freigabe by mutableStateOf<String?>("laedt")

    /** Weiter heraus geht es nicht — bei Stufe 3 passt die Welt einmal auf den Schirm. */
    var minZoom: Double = 3.0

    val maxZoom: Double get() = stil.maxZoom.toDouble()

    /** Die Kantenlänge der ganzen Welt in Bildpunkten auf dieser Zoomstufe. */
    fun welt(z: Double = zoom): Double = 256.0 * dichte * 2.0.pow(z)

    /** Wo ein Ort gerade auf dem Bildschirm liegt. */
    fun bildschirm(lat: Double, lon: Double): Offset {
        val w = welt()
        val dx = mercatorX(lon, w) - mercatorX(mitteLon, w)
        val dy = mercatorY(lat, w) - mercatorY(mitteLat, w)
        return Offset(
            (groesse.width / 2.0 + dx).toFloat(),
            (groesse.height / 2.0 + dy).toFloat(),
        )
    }

    /** Welcher Ort unter einem Bildschirmpunkt liegt — `lat` zu `lon`. */
    fun ortVon(punkt: Offset): Pair<Double, Double> {
        val w = welt()
        val x = mercatorX(mitteLon, w) + (punkt.x - groesse.width / 2.0)
        val y = (mercatorY(mitteLat, w) + (punkt.y - groesse.height / 2.0)).coerceIn(0.0, w)
        return mercatorLat(y, w) to mercatorLon(x, w)
    }

    /**
     * Meter in Bildpunkte an dieser Breite — die Kreise der Bauradien und Absperrungen.
     *
     * Web-Mercator dehnt nach Norden: Ein Meter ist dort mehr Bildpunkte als am Äquator,
     * genau um `1 / cos(Breite)`.
     */
    fun meterZuPx(lat: Double, meter: Double): Float {
        val meterJePunkt = ERDUMFANG * cos(lat * PI / 180.0) / welt()
        return (meter / meterJePunkt).toFloat()
    }

    /** Um einen Faktor zoomen — der Punkt unter dem Finger bleibt unter dem Finger. */
    fun zoomen(faktor: Float, um: Offset) {
        if (faktor == 1f || groesse == IntSize.Zero) return
        val neu = (zoom + ln(faktor.toDouble()) / ln(2.0)).coerceIn(minZoom, maxZoom)
        zoomAuf(neu, um)
    }

    /** Auf eine Stufe zoomen, um einen Bildschirmpunkt herum. */
    fun zoomAuf(stufe: Double, um: Offset = Offset(groesse.width / 2f, groesse.height / 2f)) {
        val neu = stufe.coerceIn(minZoom, maxZoom)
        val w = welt()
        val anteil = 2.0.pow(neu - zoom)
        val fx = um.x - groesse.width / 2.0
        val fy = um.y - groesse.height / 2.0
        val neueWelt = welt(neu)
        val mx = mercatorX(mitteLon, w) + fx
        val my = mercatorY(mitteLat, w) + fy
        mitteLon = mercatorLon(mx * anteil - fx, neueWelt)
        mitteLat = mercatorLat((my * anteil - fy).coerceIn(0.0, neueWelt), neueWelt)
        zoom = neu
    }

    /** Die Karte um so viele Bildpunkte verschieben, wie der Finger gezogen hat. */
    fun verschieben(dx: Float, dy: Float) {
        val w = welt()
        mitteLon = mercatorLon(mercatorX(mitteLon, w) - dx, w).coerceIn(-180.0, 180.0)
        mitteLat = mercatorLat((mercatorY(mitteLat, w) - dy).coerceIn(0.0, w), w)
    }

    /**
     * Springt zu einem Punkt und legt ihn in die Mitte dessen, was man sieht — nicht in
     * die Mitte dessen, was die Karte für ihre Fläche hält (`hinschauen` im Web).
     */
    fun hinschauen(lat: Double, lon: Double, stufe: Double = zoom, rand: Kartenrand = Kartenrand()) {
        val z = stufe.coerceIn(minZoom, maxZoom)
        val w = welt(z)
        val dx = (rand.links - rand.rechts) / 2.0
        val dy = (rand.oben - rand.unten) / 2.0
        zoom = z
        mitteLon = mercatorLon(mercatorX(lon, w) - dx, w)
        mitteLat = mercatorLat((mercatorY(lat, w) - dy).coerceIn(0.0, w), w)
    }

    /**
     * Alle Punkte ins Bild — `fitBounds` mit Polster und Zoomdeckel.
     *
     * @param polster Um welchen Anteil die Grenzen ringsum wachsen (`pad(0.25)` im Web).
     */
    fun zuschneiden(
        punkte: List<Pair<Double, Double>>,
        hoechstens: Double = 13.0,
        polster: Double = 0.25,
        rand: Kartenrand = Kartenrand(),
    ) {
        if (punkte.isEmpty() || groesse == IntSize.Zero) return

        var minLat = punkte.minOf { it.first }
        var maxLat = punkte.maxOf { it.first }
        var minLon = punkte.minOf { it.second }
        var maxLon = punkte.maxOf { it.second }
        val dLat = (maxLat - minLat) * polster
        val dLon = (maxLon - minLon) * polster
        minLat -= dLat
        maxLat += dLat
        minLon -= dLon
        maxLon += dLon

        val breite = groesse.width - rand.links - rand.rechts
        val hoehe = groesse.height - rand.oben - rand.unten

        var z = hoechstens.coerceAtMost(maxZoom)
        while (z > minZoom) {
            val w = welt(z)
            val b = abs(mercatorX(maxLon, w) - mercatorX(minLon, w))
            val h = abs(mercatorY(minLat, w) - mercatorY(maxLat, w))
            if (b <= breite && h <= hoehe) break
            z -= 0.25
        }

        val w = welt(z)
        val mx = (mercatorX(minLon, w) + mercatorX(maxLon, w)) / 2.0
        val my = (mercatorY(minLat, w) + mercatorY(maxLat, w)) / 2.0
        val dx = (rand.links - rand.rechts) / 2.0
        val dy = (rand.oben - rand.unten) / 2.0

        zoom = z
        mitteLon = mercatorLon(mx - dx, w)
        mitteLat = mercatorLat((my - dy).coerceIn(0.0, w), w)
    }

    private companion object {
        /** Der Erdumfang am Äquator in Metern — die Grundzahl von Web-Mercator. */
        const val ERDUMFANG = 40_075_016.686
    }
}

@Composable
fun rememberKartenzustand(lat: Double, lon: Double, zoom: Double): Kartenzustand =
    remember { Kartenzustand(lat, lon, zoom) }

// -------------------------------------------------------------- Marken, Formen

/**
 * Was eine Marke zum Zeichnen bekommt — der Textmesser und der Zustand der Karte.
 */
class Kartenpinsel internal constructor(
    val messer: TextMeasurer,
    val zustand: Kartenzustand,
)

/**
 * Eine Marke auf der Karte.
 *
 * <b>Sie zeichnet sich selbst</b> (`zeichnen` bekommt den Ort auf dem Bildschirm), und
 * die Karte prüft beim Antippen nur den Abstand dorthin. Das ist die Form, die ein
 * `divIcon` in Leaflet hat, ohne dessen Umweg über das DOM.
 *
 * @param ebene Die Stapelhöhe — höher liegt oben und gewinnt beim Antippen
 *   (`zIndexOffset` im Web).
 * @param klumpen Ob die Marke bis `klumpenBisZoom` in einen Klumpen fallen darf.
 * @param klumpenEigen Ob ein Klumpen mit dieser Marke als „eigen" gilt (volles Amber).
 */
class Kartenmarke(
    val id: String,
    val lat: Double,
    val lon: Double,
    val ebene: Int = 0,
    val trefferDp: Float = 22f,
    val klumpen: Boolean = false,
    val klumpenEigen: Boolean = false,
    val beiTipp: (() -> Unit)? = null,
    val zeichnen: DrawScope.(ort: Offset, pinsel: Kartenpinsel) -> Unit,
)

/** Eine Form unter den Marken — Strecke, Fläche oder Kreis in Metern. */
sealed class Kartenform {

    /**
     * Ein Streckenzug.
     *
     * @param strich Das Strichmuster in Punkten (an, aus, …) — `null` ist durchgezogen.
     */
    class Linie(
        val punkte: List<Pair<Double, Double>>,
        val farbe: Color,
        val breite: Dp = 3.dp,
        val strich: List<Float>? = null,
        val deckkraft: Float = 1f,
    ) : Kartenform()

    /** Ein Vieleck. `beiTipp` macht es anfassbar — sonst gehört der Tipp der Karte. */
    class Flaeche(
        val ecken: List<Pair<Double, Double>>,
        val fuellung: Color,
        val rand: Color,
        val randbreite: Dp = 2.dp,
        val strich: List<Float>? = null,
        val beiTipp: (() -> Unit)? = null,
    ) : Kartenform()

    /** Ein Kreis mit einem Radius in Metern — Bauradius, Absperrung. */
    class Kreis(
        val lat: Double,
        val lon: Double,
        val meter: Double,
        val fuellung: Color,
        val rand: Color,
        val randbreite: Dp = 2.dp,
        val strich: List<Float>? = null,
    ) : Kartenform()
}

/** Eine Zeile im Ebenenmenü. */
class Kartenebene(
    val name: String,
    val an: Boolean,
    /** Ob es dazu gerade etwas zu zeigen gibt — sonst steht die Zeile blass da. */
    val vorhanden: Boolean = true,
    val setzen: (Boolean) -> Unit,
)

/** Ein Klumpen: mehrere Marken, die sich auf dieser Stufe überdecken. */
private class Klumpen(val lat: Double, val lon: Double, val anzahl: Int, val eigen: Boolean)

/**
 * Fasst überdeckende Marken zu Klumpen zusammen und gibt zurück, was einzeln bleibt.
 *
 * <b>Ein Raster und kein echtes Ballen</b> — wie `klumpenBilden` in `Weltkarte.vue`:
 * Jede Marke fällt in ein Fach von 44 Punkten, und wo mehr als eine liegt, ist ein
 * Klumpen. Gerechnet wird in Bildpunkten, nicht in Grad.
 */
private fun klumpenBilden(
    marken: List<Kartenmarke>,
    zustand: Kartenzustand,
    bisZoom: Double?,
): Pair<List<Kartenmarke>, List<Klumpen>> {
    if (bisZoom == null || zustand.zoom > bisZoom) return marken to emptyList()

    val raster = KLUMPEN_RASTER_DP * zustand.dichte
    val w = zustand.welt()
    val faecher = LinkedHashMap<String, MutableList<Kartenmarke>>()
    val einzeln = mutableListOf<Kartenmarke>()

    for (m in marken) {
        if (!m.klumpen) {
            einzeln += m
            continue
        }
        val x = (mercatorX(m.lon, w) / raster).roundToInt()
        val y = (mercatorY(m.lat, w) / raster).roundToInt()
        faecher.getOrPut("$x:$y") { mutableListOf() } += m
    }

    val klumpen = mutableListOf<Klumpen>()
    for (fach in faecher.values) {
        // Eine Marke allein ist kein Klumpen — sie soll ihr Fahrzeug bleiben.
        if (fach.size < 2) {
            einzeln += fach
            continue
        }
        klumpen += Klumpen(
            lat = fach.sumOf { it.lat } / fach.size,
            lon = fach.sumOf { it.lon } / fach.size,
            anzahl = fach.size,
            eigen = fach.any { it.klumpenEigen },
        )
    }

    return einzeln to klumpen
}

/** Die Rastergröße der Klumpen — eine Fahrzeugmarke ist rund 42 Punkte lang. */
private const val KLUMPEN_RASTER_DP = 44f

// ------------------------------------------------------------------- Karte

/**
 * Die allgemeine Karte.
 *
 * @param marken Was obenauf liegt — in der Reihenfolge ihrer `ebene` gezeichnet.
 * @param formen Was darunter liegt: Strecken, Flächen, Kreise.
 * @param klumpenBisZoom Bis zu welcher Stufe (einschließlich) `klumpen`-Marken
 *   zusammengefasst werden; `null` heißt nie.
 * @param beiKartentipp Ein Tipp neben jede Marke — als Koordinate.
 * @param beiEinpassen Der Knopf „alles ins Bild"; ohne ihn fehlt der Knopf.
 * @param ebenen Die Zeilen des Ebenenmenüs; leer heißt: kein Ebenenknopf.
 * @param randOben Wie weit die Knopfreihe von oben absteht (Kopfstreifen).
 * @param randUnten Wie weit Quellenangabe und Freigabe von unten abstehen (Reiterleiste).
 * @param freigabeAbdunkeln Ob die Einwilligungsfrage die Karte sperrt — nur auf der
 *   Gründungsseite, wo ohne Standort ohnehin nichts weitergeht.
 * @param zeichnenUnten Freies Zeichnen unter den Formen.
 * @param inhalt Was über der Karte liegt (Hinweise, Leisten).
 */
@Composable
fun Kartenflaeche(
    zustand: Kartenzustand,
    modifier: Modifier = Modifier,
    marken: List<Kartenmarke> = emptyList(),
    formen: List<Kartenform> = emptyList(),
    klumpenBisZoom: Double? = null,
    beiKartentipp: ((lat: Double, lon: Double) -> Unit)? = null,
    beiEinpassen: (() -> Unit)? = null,
    ebenen: List<Kartenebene> = emptyList(),
    randOben: Dp = 0.dp,
    randUnten: Dp = 0.dp,
    freigabeAbdunkeln: Boolean = false,
    zeichnenUnten: (DrawScope.(Kartenpinsel) -> Unit)? = null,
    inhalt: @Composable BoxScope.() -> Unit = {},
) {
    val zusammenhang = LocalContext.current
    val ablage = remember { Ablage(zusammenhang) }
    val bereich = rememberCoroutineScope()
    val dichte = LocalDensity.current
    val messer = rememberTextMeasurer()
    val pinsel = remember(messer, zustand) { Kartenpinsel(messer, zustand) }

    var menue by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (zustand.freigabe == "laedt") zustand.freigabe = ablage.karteFreigabe()
        zustand.stil = Kartenstil.entries.firstOrNull { it.name == ablage.karteStil() }
            ?: Kartenstil.Dunkel
    }

    // Die Gesten laufen in `pointerInput(Unit)` — was sie lesen, muss frisch sein.
    val aktuelleMarken by rememberUpdatedState(marken)
    val aktuelleFormen by rememberUpdatedState(formen)
    val aktuellerTipp by rememberUpdatedState(beiKartentipp)
    val aktuellerKlumpenzoom by rememberUpdatedState(klumpenBisZoom)

    Box(modifier = modifier.background(Farben.BgTief)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { mittelpunkt, verschiebung, zoomFaktor, _ ->
                        zustand.zoomen(zoomFaktor, mittelpunkt)
                        zustand.verschieben(verschiebung.x, verschiebung.y)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { punkt -> zustand.zoomAuf(zustand.zoom + 1.0, punkt) },
                    ) { tipp ->
                        // Ein offenes Menü geht beim Griff auf die Karte mit zu — wer auf die
                        // Karte fasst, will die Karte, nicht das Menü darüber.
                        menue = null
                        tippen(
                            tipp,
                            zustand,
                            aktuelleMarken,
                            aktuelleFormen,
                            aktuellerKlumpenzoom,
                            aktuellerTipp,
                        )
                    }
                },
        ) {
            zustand.groesse = IntSize(size.width.toInt(), size.height.toInt())
            zustand.dichte = dichte.density
            // Der Lese-Anker für angekommene Kacheln — siehe Kachelspeicher.
            @Suppress("UNUSED_VARIABLE")
            val anker = Kachelspeicher.stand

            if (zustand.freigabe == "ja") kachelnZeichnen(zustand, bereich)

            zeichnenUnten?.invoke(this, pinsel)

            aktuelleFormen.forEach { form -> formZeichnen(form, zustand) }

            val (einzeln, klumpen) = klumpenBilden(aktuelleMarken, zustand, aktuellerKlumpenzoom)
            einzeln.sortedBy { it.ebene }.forEach { m ->
                val ort = zustand.bildschirm(m.lat, m.lon)
                // Was weit draußen liegt, wird nicht gezeichnet — ein Rand von 80 Punkten
                // für Schilder, die über den Rand ragen.
                val rand = 80.dp.toPx()
                if (ort.x < -rand || ort.y < -rand || ort.x > size.width + rand ||
                    ort.y > size.height + rand
                ) {
                    return@forEach
                }
                m.zeichnen(this, ort, pinsel)
            }
            klumpen.forEach { k -> klumpenZeichnen(k, zustand.bildschirm(k.lat, k.lon), messer) }
        }

        // --------------------------------------------------------- Knöpfe
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = randOben + Abstand.Klein, end = Abstand.Klein),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                if (beiEinpassen != null) {
                    Kartenknopf(ZEICHEN_EINPASSEN, "Auf den eigenen Bereich einpassen") {
                        menue = null
                        beiEinpassen()
                    }
                }
                Kartenknopf(ZEICHEN_ANSICHT, "Kartenansicht wählen", an = menue == "ansicht") {
                    menue = if (menue == "ansicht") null else "ansicht"
                }
                if (ebenen.isNotEmpty()) {
                    Kartenknopf(ZEICHEN_EBENEN, "Ebenen ein- und ausblenden", an = menue == "ebenen") {
                        menue = if (menue == "ebenen") null else "ebenen"
                    }
                }
            }

            when (menue) {
                "ansicht" -> Menuefeld {
                    Kartenstil.entries.forEach { s ->
                        Menuezeile(
                            text = s.titel,
                            an = s == zustand.stil,
                            rund = true,
                        ) {
                            zustand.stil = s
                            if (zustand.zoom > s.maxZoom) zustand.zoom = s.maxZoom.toDouble()
                            bereich.launch { ablage.karteStilSetzen(s.name) }
                        }
                    }
                    // Ohne Freigabe ändert die Wahl nichts Sichtbares. Das gehört gesagt,
                    // sonst sieht die Ansicht kaputt aus, wo nur die Einwilligung fehlt.
                    if (zustand.freigabe != "ja") {
                        Text(
                            text = "Kartenhintergrund ist aus (Konto → Privatsphäre).",
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                            modifier = Modifier.widthIn(max = 220.dp).padding(Abstand.Klein),
                        )
                    }
                }

                "ebenen" -> Menuefeld {
                    ebenen.forEach { e ->
                        Menuezeile(
                            text = e.name,
                            an = e.an && e.vorhanden,
                            rund = false,
                            aktiv = e.vorhanden,
                        ) { e.setzen(!e.an) }
                    }
                }

                else -> Unit
            }

            // Die Zoomtasten rücken unter die Reihe — am Handy zoomt man mit zwei Fingern,
            // aber eine Hand am Lenkrad des Fahrrads hat nur einen.
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                Kartenknopf(ZEICHEN_PLUS, "Hineinzoomen") { zustand.zoomAuf(zustand.zoom + 1.0) }
                Kartenknopf(ZEICHEN_MINUS, "Herauszoomen") { zustand.zoomAuf(zustand.zoom - 1.0) }
            }
        }

        inhalt()

        // Die Quellenangabe muss sichtbar bleiben — ODbL ist keine Kür.
        if (zustand.freigabe == "ja") {
            Text(
                text = zustand.stil.attribution,
                style = Schrift.Winzig.copy(fontSize = 10.sp),
                color = Farben.TextSehrLeise,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = randUnten)
                    .background(Farben.BgTief.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        // ----------------------------------------------------- Einwilligung
        if (zustand.freigabe == null) {
            KartenFreigabe(
                abdunkeln = freigabeAbdunkeln,
                randUnten = randUnten,
                beiAntwort = { ja ->
                    val wert = if (ja) "ja" else "nein"
                    zustand.freigabe = wert
                    bereich.launch { ablage.karteFreigabeSetzen(wert) }
                },
            )
        }
    }
}

/**
 * Die Frage nach dem Kartenhintergrund — übertragen aus `KartenFreigabe.vue`.
 *
 * <b>Heute nur noch wegen des Luftbilds.</b> Die gezeichnete Karte kommt vom eigenen
 * Server in Deutschland; das Luftbild von Esri in den USA, ohne Vertrag mit uns — dafür
 * braucht es ein Ja (Art. 49 Abs. 1 lit. a DSGVO, Datenschutzerklärung Ziffer 11).
 *
 * @param abdunkeln Ob die Frage die Fläche dahinter sperrt. Überall sonst bleibt sie
 *   durchlässig: Die Karte darunter funktioniert auch ohne Hintergrund.
 */
@Composable
fun BoxScope.KartenFreigabe(
    abdunkeln: Boolean,
    beiAntwort: (Boolean) -> Unit,
    randUnten: Dp = 0.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = (
            if (abdunkeln) {
                Modifier
                    .matchParentSize()
                    .background(Farben.Ueberlagerung)
                    // Die Sperrfläche fängt den Tipp, der sonst ins Formular dahinter fiele.
                    .clickable(enabled = true, onClick = {}, indication = null, interactionSource = null)
            } else {
                Modifier.align(Alignment.BottomCenter)
            }
            )
            .padding(start = Abstand.Gross, end = Abstand.Gross, bottom = randUnten + Abstand.Gross),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .flaeche()
                .padding(Abstand.Gross),
        ) {
            Text(
                text = "KARTENHINTERGRUND",
                style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                color = Farben.Amber,
            )
            Text(
                text = "Die gezeichnete Karte kommt von unserem eigenen Kartenserver in " +
                    "Deutschland. Das Luftbild der Satelliten- und Hybridansicht lädt die App " +
                    "direkt bei Esri in den USA; dabei erhält Esri deine IP-Adresse und den " +
                    "Ausschnitt, den du dir ansiehst — darauf haben wir keinen Einfluss. Ohne " +
                    "Hintergrund läuft alles weiter: Einsätze, Fahrzeuge und Routen stehen an " +
                    "derselben Stelle.",
                style = Schrift.Klein,
                color = Farben.Text,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                Knopf("Ohne Karte", { beiAntwort(false) }, art = Knopfart.Leise, kompakt = true)
                Knopf("Karte laden", { beiAntwort(true) }, art = Knopfart.Haupt, kompakt = true)
            }
            Text(
                text = "Gilt für dieses Gerät und ist jederzeit änderbar (Konto → Privatsphäre). " +
                    "Datenschutz, Ziffer 11.",
                style = Schrift.Winzig,
                color = Farben.TextSehrLeise,
            )
        }
    }
}

// ----------------------------------------------------------------- Antippen

/**
 * Der Tipp auf die Karte: erst die Marken (die oberste gewinnt), dann die Klumpen, dann
 * die anfassbaren Flächen — und wenn nichts getroffen ist, die Koordinate.
 */
private fun tippen(
    tipp: Offset,
    zustand: Kartenzustand,
    marken: List<Kartenmarke>,
    formen: List<Kartenform>,
    klumpenBisZoom: Double?,
    beiKartentipp: ((Double, Double) -> Unit)?,
) {
    val (einzeln, klumpen) = klumpenBilden(marken, zustand, klumpenBisZoom)

    val treffer = einzeln
        .filter { it.beiTipp != null }
        .mapNotNull { m ->
            val abstand = (zustand.bildschirm(m.lat, m.lon) - tipp).getDistance()
            if (abstand <= m.trefferDp * zustand.dichte) m to abstand else null
        }
        .sortedWith(compareByDescending<Pair<Kartenmarke, Float>> { it.first.ebene }.thenBy { it.second })
        .firstOrNull()

    if (treffer != null) {
        treffer.first.beiTipp?.invoke()
        return
    }

    // Ein Klumpen ist eine Aufforderung: Wer ihn anfasst, will wissen, was darin steckt.
    val klumpenTreffer = klumpen.firstOrNull {
        (zustand.bildschirm(it.lat, it.lon) - tipp).getDistance() <= 22f * zustand.dichte
    }
    if (klumpenTreffer != null && klumpenBisZoom != null) {
        zustand.hinschauen(klumpenTreffer.lat, klumpenTreffer.lon, klumpenBisZoom + 3.0)
        return
    }

    val (lat, lon) = zustand.ortVon(tipp)

    formen.filterIsInstance<Kartenform.Flaeche>()
        .lastOrNull { it.beiTipp != null && imVieleck(lat, lon, it.ecken) }
        ?.let { f ->
            f.beiTipp?.invoke()
            return
        }

    beiKartentipp?.invoke(lat, lon)
}

/** Ob ein Punkt in einem Vieleck liegt — der Strahlentest, in Grad gerechnet. */
private fun imVieleck(lat: Double, lon: Double, ecken: List<Pair<Double, Double>>): Boolean {
    if (ecken.size < 3) return false
    var drin = false
    var j = ecken.size - 1
    for (i in ecken.indices) {
        val (ai, oi) = ecken[i]
        val (aj, oj) = ecken[j]
        if ((ai > lat) != (aj > lat) && lon < (oj - oi) * (lat - ai) / (aj - ai) + oi) drin = !drin
        j = i
    }
    return drin
}

// ----------------------------------------------------------------- Zeichnen

/** Die Kacheln des sichtbaren Ausschnitts — samt Beschriftungsebene beim Hybrid. */
private fun DrawScope.kachelnZeichnen(zustand: Kartenzustand, bereich: CoroutineScope) {
    val stil = zustand.stil
    val kachelZ = floor(zustand.zoom).toInt().coerceIn(1, stil.maxZoom)
    val kante = 256.0 * zustand.dichte
    val skala = 2.0.pow(zustand.zoom - kachelZ)
    val kachelPx = kante * skala
    val welt = zustand.welt()
    val linksWelt = mercatorX(zustand.mitteLon, welt) - size.width / 2.0
    val obenWelt = mercatorY(zustand.mitteLat, welt) - size.height / 2.0
    val vonX = floor(linksWelt / kachelPx).toInt()
    val vonY = floor(obenWelt / kachelPx).toInt()
    val bisX = floor((linksWelt + size.width) / kachelPx).toInt()
    val bisY = floor((obenWelt + size.height) / kachelPx).toInt()
    val jeReihe = 1 shl kachelZ

    for (x in vonX..bisX) for (y in vonY..bisY) {
        // Keine zweite Ausfertigung der Welt neben der ersten — `noWrap` im Web.
        if (y < 0 || y >= jeReihe || x < 0 || x >= jeReihe) continue
        val ziel = IntOffset(
            (x * kachelPx - linksWelt).toFloat().roundToInt(),
            (y * kachelPx - obenWelt).toFloat().roundToInt(),
        )
        val kantePx = (kachelPx + 1).toInt()
        listOfNotNull(stil.quelle, stil.beschriftung).forEach { vorlage ->
            val url = stil.url(vorlage, kachelZ, x, y)
            val bild = Kachelspeicher.fertig[url]
            if (bild == null) {
                Kachelspeicher.anfordern(url, bereich)
            } else {
                drawImage(image = bild, dstOffset = ziel, dstSize = IntSize(kantePx, kantePx))
            }
        }
    }
}

private fun strichmuster(strich: List<Float>?, dichte: Float): PathEffect? =
    strich?.takeIf { it.size >= 2 }?.let { m ->
        PathEffect.dashPathEffect(m.map { it * dichte }.toFloatArray())
    }

private fun DrawScope.formZeichnen(form: Kartenform, zustand: Kartenzustand) {
    when (form) {
        is Kartenform.Linie -> {
            if (form.punkte.size < 2) return
            val pfad = Path()
            form.punkte.forEachIndexed { i, (lat, lon) ->
                val o = zustand.bildschirm(lat, lon)
                if (i == 0) pfad.moveTo(o.x, o.y) else pfad.lineTo(o.x, o.y)
            }
            drawPath(
                pfad,
                form.farbe,
                alpha = form.deckkraft,
                style = Stroke(
                    width = form.breite.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = strichmuster(form.strich, density),
                ),
            )
        }

        is Kartenform.Flaeche -> {
            if (form.ecken.size < 3) return
            val pfad = Path()
            form.ecken.forEachIndexed { i, (lat, lon) ->
                val o = zustand.bildschirm(lat, lon)
                if (i == 0) pfad.moveTo(o.x, o.y) else pfad.lineTo(o.x, o.y)
            }
            pfad.close()
            drawPath(pfad, form.fuellung)
            drawPath(
                pfad,
                form.rand,
                style = Stroke(
                    width = form.randbreite.toPx(),
                    join = StrokeJoin.Round,
                    pathEffect = strichmuster(form.strich, density),
                ),
            )
        }

        is Kartenform.Kreis -> {
            val mitte = zustand.bildschirm(form.lat, form.lon)
            val r = zustand.meterZuPx(form.lat, form.meter)
            drawCircle(form.fuellung, radius = r, center = mitte)
            drawCircle(
                form.rand,
                radius = r,
                center = mitte,
                style = Stroke(
                    width = form.randbreite.toPx(),
                    pathEffect = strichmuster(form.strich, density),
                ),
            )
        }
    }
}

/**
 * Das Zeichen eines Klumpens: eine Scheibe mit der Zahl.
 *
 * Bewusst kein Fahrzeugriss mit einer Zahl daneben — ein Klumpen ist kein Fahrzeug. Die
 * Farbe sagt nur eines: ob eigene dabei sind.
 */
private fun DrawScope.klumpenZeichnen(k: Klumpen, ort: Offset, messer: TextMeasurer) {
    val flaeche = if (k.eigen) Farben.Amber else farbmischung(Farben.Amber, Farben.BgTief, 0.55f)
    val schrift = if (k.eigen) Farben.AufFarbe else Farben.Text
    kartenpille(
        messer = messer,
        text = k.anzahl.toString(),
        mitte = ort,
        flaeche = flaeche,
        schrift = schrift,
        hoehe = 28.dp,
        saum = Farben.BgTief.copy(alpha = 0.55f),
    )
}

/** Zwei Farben mischen — `color-mix` im Web. */
fun farbmischung(a: Color, b: Color, anteilB: Float): Color = Color(
    red = a.red + (b.red - a.red) * anteilB,
    green = a.green + (b.green - a.green) * anteilB,
    blue = a.blue + (b.blue - a.blue) * anteilB,
    alpha = a.alpha + (b.alpha - a.alpha) * anteilB,
)

/**
 * Eine Pille mit Text, mittig auf einem Punkt — die Bauform der Lagenzahl, des Klumpens
 * und der Schilder unter Fahrzeugen und Leitstellen.
 *
 * @param saum Eine Haarlinie ringsum — über einem hellen Luftbild steht die Pille sonst
 *   kaum ab.
 */
fun DrawScope.kartenpille(
    messer: TextMeasurer,
    text: String,
    mitte: Offset,
    flaeche: Color,
    schrift: Color,
    hoehe: Dp = 20.dp,
    groesse: TextUnit = 12.sp,
    fett: Boolean = true,
    mono: Boolean = true,
    saum: Color? = null,
    saumbreite: Dp = 1.dp,
    mindestbreite: Dp = 0.dp,
) {
    val gemessen = messer.measure(
        text,
        TextStyle(
            color = schrift,
            fontSize = groesse,
            fontWeight = if (fett) FontWeight.Bold else FontWeight.Medium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        ),
    )
    val h = hoehe.toPx()
    val b = max(max(gemessen.size.width + 8.dp.toPx(), h), mindestbreite.toPx())
    val links = Offset(mitte.x - b / 2f, mitte.y - h / 2f)
    val ecke = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
    if (saum != null) {
        val s = saumbreite.toPx()
        drawRoundRect(
            saum,
            topLeft = links - Offset(s, s),
            size = Size(b + 2 * s, h + 2 * s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f + s, h / 2f + s),
        )
    }
    drawRoundRect(flaeche, topLeft = links, size = Size(b, h), cornerRadius = ecke)
    drawText(
        gemessen,
        topLeft = Offset(mitte.x - gemessen.size.width / 2f, mitte.y - gemessen.size.height / 2f),
    )
}

/** Wie weit ein Punkt von einer Strecke weg ist, in Bildpunkten. */
fun abstandZurStrecke(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val laenge2 = dx * dx + dy * dy
    if (laenge2 == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / laenge2).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}

// ----------------------------------------------------------------- Bedienung

@Composable
private fun Kartenknopf(
    zeichen: ImageVector,
    beschreibung: String,
    an: Boolean = false,
    beiDruck: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .flaeche(
                farbe = if (an) Farben.FlaecheAktiv else Farben.Flaeche.copy(alpha = 0.92f),
                randfarbe = if (an) Farben.AmberTief else Farben.Rand,
                ecke = 9.dp,
            )
            .clickable(onClick = beiDruck),
    ) {
        Icon(
            imageVector = zeichen,
            contentDescription = beschreibung,
            tint = if (an) Farben.Amber else Farben.TextLeise,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Menuefeld(inhalt: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .widthIn(min = 180.dp, max = 260.dp)
            .flaeche(farbe = Farben.Flaeche.copy(alpha = 0.96f), ecke = 9.dp)
            .padding(Abstand.Winzig),
    ) {
        inhalt()
    }
}

@Composable
private fun Menuezeile(
    text: String,
    an: Boolean,
    rund: Boolean,
    aktiv: Boolean = true,
    beiDruck: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = aktiv, onClick = beiDruck)
            .padding(horizontal = Abstand.Klein, vertical = 10.dp),
    ) {
        val farbe = when {
            !aktiv -> Farben.TextSehrLeise
            an -> Farben.Amber
            else -> Farben.Text
        }
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(
                    1.5.dp,
                    farbe,
                    if (rund) {
                        androidx.compose.foundation.shape.CircleShape
                    } else {
                        androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (an) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            farbe,
                            if (rund) {
                                androidx.compose.foundation.shape.CircleShape
                            } else {
                                androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            },
                        ),
                )
            }
        }
        Text(text = text, style = Schrift.Klein, color = farbe)
    }
}

/** Ein Zeichen aus Strichen auf dem 24er-Raster — wie in `ui/zeichen/Zeichen.kt`. */
private fun kartenzeichen(name: String, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        pfade.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** Vier Ecken nach außen — der übliche Weg für „auf alles einpassen". */
private val ZEICHEN_EINPASSEN = kartenzeichen(
    "einpassen",
    "M9 4H4v5",
    "M15 4h5v5",
    "M15 20h5v-5",
    "M9 20H4v-5",
)

/** Gefaltete Karte: es geht um den Untergrund selbst. */
private val ZEICHEN_ANSICHT = kartenzeichen(
    "ansicht",
    "M9 4 3 6.5v13L9 17l6 3 6-2.5v-13L15 7Z",
    "M9 4v13",
    "M15 7v13",
)

/** Gestapelte Blätter: es geht um das, was auf der Karte liegt. */
private val ZEICHEN_EBENEN = kartenzeichen(
    "ebenen",
    "m12 3 9 5-9 5-9-5Z",
    "m3 12 9 5 9-5",
    "m3 16 9 5 9-5",
)

private val ZEICHEN_PLUS = kartenzeichen("plus", "M12 5v14", "M5 12h14")
private val ZEICHEN_MINUS = kartenzeichen("minus", "M5 12h14")
