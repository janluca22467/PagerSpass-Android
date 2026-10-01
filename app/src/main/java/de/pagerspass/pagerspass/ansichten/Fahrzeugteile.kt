package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.pagerspass.pagerspass.mobil.Geraeteeinstellungen
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.kopfverlauf

/**
 * Was die Bögen am Fahrzeug teilen — PatSim, FwSim, die Polizeiabfragen und das
 * Einsatz-Tablet. Im Web steht dasselbe als `.bogen`, `.wert`, `.wert__balken` und
 * `.reiter` in den Stilblättern der Komponenten; hier einmal, damit die vier Bögen
 * gleich aussehen, wo sie gleich gemeint sind.
 */

// ------------------------------------------------------------------- Zeit

/** „jetzt", „40 s", „3 min" — das Alter einer Messung, knapp genug für eine Kachelecke. */
internal fun alterText(um: String?, jetzt: Long): String {
    val zeit = zeitpunktMs(um) ?: return ""
    val sekunden = ((jetzt - zeit) / 1000).coerceAtLeast(0)
    return when {
        sekunden < 5 -> "jetzt"
        sekunden < 60 -> "$sekunden s"
        else -> "${sekunden / 60} min"
    }
}

/** Restsekunden bis `bis` — `null`, wenn nichts läuft. */
internal fun restSekunden(bis: String?, jetzt: Long): Int? {
    val ende = zeitpunktMs(bis) ?: return null
    return (((ende - jetzt) + 999) / 1000).toInt().coerceAtLeast(0)
}

/** Wie weit ein Vorgang von `von` nach `bis` ist, 0 bis 1. */
internal fun anteilZwischen(von: String?, bis: String?, jetzt: Long): Float {
    val a = zeitpunktMs(von) ?: return 1f
    val b = zeitpunktMs(bis) ?: return 1f
    if (b <= a) return 1f
    return ((jetzt - a).toFloat() / (b - a)).coerceIn(0f, 1f)
}

/** „12:04" — Minuten und Sekunden, ab einer Stunde mit Stunden davor (`dauer()` im Web). */
internal fun dauerText(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val zwei = { n: Long -> n.toString().padStart(2, '0') }
    return if (h > 0) "$h:${zwei(m)}:${zwei(s % 60)}" else "${zwei(m)}:${zwei(s % 60)}"
}

/** „14:32" — die Uhrzeit einer Meldung in der Zeitzone des Geräts. */
internal fun uhrzeitKurz(roh: String?): String {
    val ms = zeitpunktMs(roh) ?: return "—"
    val t = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
    return "%02d:%02d".format(t.hour, t.minute)
}

/** Die Kennung, die dieser Spieler sehen will — `kennung()` aus `fahrzeugkennung.ts`. */
internal fun kennungVon(f: Rundenfahrzeug): String =
    Geraeteeinstellungen.kennung(f.kurzname.ifBlank { f.funkrufname }, f.typ, f.organisation, null)

// ------------------------------------------------------------------ Zeichen

/**
 * Ein Strichzeichen auf dem 24er-Raster — dieselbe Form wie `Zeichen.kt`, für die
 * Zeichen, die nur am Fahrzeug stehen (FMS-Tasten, Ausschalter, Tür).
 */
internal fun fahrzeugzeichen(name: String, vararg pfade: String, staerke: Float = 1.8f): ImageVector =
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
                strokeLineWidth = staerke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** Ein Kreis als Bogenpfad — Pfaddaten kennen `<circle>` nicht. */
internal fun kreis(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

/** Ein rundes Rechteck als Pfad — `<rect rx>` aus dem SVG. */
internal fun rechteck(x: Float, y: Float, b: Float, h: Float, r: Float): String =
    "M${x + r} ${y}H${x + b - r}A$r $r 0 0 1 ${x + b} ${y + r}V${y + h - r}" +
        "A$r $r 0 0 1 ${x + b - r} ${y + h}H${x + r}A$r $r 0 0 1 $x ${y + h - r}V${y + r}A$r $r 0 0 1 ${x + r} ${y}z"

internal object Fahrzeugzeichen {
    /** Ausschalter — Bogen mit Strich, dasselbe Zeichen wie in der Leitstelle. */
    val Ende = fahrzeugzeichen("ende", "M12 3.5v7.5", "M7.4 6.6a7.5 7.5 0 1 0 9.2 0")

    /** Tür mit Pfeil hinaus — das Spiegelbild von „Einer Runde beitreten". */
    val Raus = fahrzeugzeichen(
        "raus",
        "M10.5 4.5H6a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h4.5",
        "M20 12h-9.5",
        "M16.5 8.5 20 12l-3.5 3.5",
    )

    val Zurueck = fahrzeugzeichen("zurueck", "M15 5l-7 7 7 7")

    /** Die FMS-Zeichen aus `FmsZeichen.vue` — je Status eins, 7 und 8 nach Organisation. */
    private val fms = mapOf(
        0 to fahrzeugzeichen(
            "fms0",
            rechteck(7f, 3f, 6f, 10f, 3f),
            "M4.5 11a5.5 5.5 0 0 0 11 0",
            "M10 16.5V21",
            "M19.5 4.5 23.6 12.5 15.4 12.5 Z",
            "M19.5 7.4v2.6",
            "M19.5 11.2v.4",
        ),
        1 to fahrzeugzeichen("fms1", "M12 10.5v10", "M8.5 20.5h7", "M8.4 9.4a5 5 0 0 1 7.2 0", "M5.6 6.4a9 9 0 0 1 12.8 0"),
        2 to fahrzeugzeichen("fms2", "M3.5 10.5 12 4l8.5 6.5", "M5.5 9.8V20h13V9.8", "M9.5 20v-5.5h5V20"),
        3 to fahrzeugzeichen(
            "fms3",
            "M3 12h11",
            "M10.5 8.5 14 12l-3.5 3.5",
            "M19 5.5a3 3 0 0 0-3 3c0 2.2 3 5.5 3 5.5s3-3.3 3-5.5a3 3 0 0 0-3-3Z",
            "M19 18.5h2",
        ),
        4 to fahrzeugzeichen(
            "fms4",
            "M12 2.5c-3.6 0-6.5 2.9-6.5 6.5 0 4.8 6.5 12 6.5 12s6.5-7.2 6.5-12c0-3.6-2.9-6.5-6.5-6.5Z",
            "m9.2 9.2 2 2 3.6-3.6",
        ),
        5 to fahrzeugzeichen("fms5", rechteck(9f, 3f, 6f, 10f, 3f), "M6.5 11a5.5 5.5 0 0 0 11 0", "M12 16.5V21", "M9 21h6"),
        6 to fahrzeugzeichen("fms6", kreis(12f, 12f, 8.5f), "m6 6 12 12"),
    )
    private val fms7Rd = fahrzeugzeichen(
        "fms7rd", "M3.5 14.5h17", "M6 14.5v3.5", "M18 14.5v3.5", kreis(8f, 8.5f, 2f), "M11 11.5h7", "M4.5 11.5h2.5",
    )
    private val fms7 = fahrzeugzeichen(
        "fms7",
        "M9.8 14.2a3.5 3.5 0 0 1 0-5l2.4-2.4a3.5 3.5 0 1 1 5 5l-1.2 1.2",
        "M14.2 9.8a3.5 3.5 0 0 1 0 5l-2.4 2.4a3.5 3.5 0 1 1-5-5l1.2-1.2",
    )
    private val fms8Rd = fahrzeugzeichen("fms8rd", "M4.5 20.5V8.2L12 3.5l7.5 4.7v12.3", "M3 20.5h18", "M12 9.5v5", "M9.5 12h5")
    private val fms8 = fahrzeugzeichen("fms8", kreis(12f, 12f, 8.5f), "M12 3.5v17")

    fun fms(status: Int, organisation: String?): ImageVector? {
        val rd = organisation == "Rettungsdienst"
        return when (status) {
            7 -> if (rd) fms7Rd else fms7
            8 -> if (rd) fms8Rd else fms8
            else -> fms[status]
        }
    }
}

// ----------------------------------------------------------------- Bausteine

/**
 * Der dünne Balken unter einer laufenden Messung, Abfrage oder Maßnahme —
 * `.wert__balken` im Web. Er füllt sich, statt nur zu blinken: Wer zwanzig
 * Sekunden auf einen Blutdruck wartet, will sehen, wie viel noch fehlt.
 */
@Composable
internal fun Laufbalken(anteil: Float, modifier: Modifier = Modifier, farbe: Color = Farben.Amber) {
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(Rundung.Rund)
            .background(Farben.HauchHell),
    ) {
        Box(
            Modifier
                .fillMaxWidth(anteil.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(farbe),
        )
    }
}

/**
 * Die Reiterreihe eines Bogens — `.bogen__reiter`. Dieselbe Bauform wie die Reiter
 * der Seite, nur im Bogen: Wer am Patienten zwischen Befund und Therapie wechselt,
 * soll dieselbe Geste kennen.
 */
@Composable
internal fun <T> Bogenreiter(
    seiten: List<Pair<T, String>>,
    offen: T,
    beiWahl: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Reiterreihe(modifier) {
        seiten.forEach { (id, name) -> Reiter(name, offen == id, { beiWahl(id) }) }
    }
}

/**
 * Eine Kachel — `.wert` im Web: Kopf mit Namen und Ecke, groß die Zahl, klein der
 * Fuß, darunter der Balken, wenn etwas läuft. Jede Kachel ist ihr eigener Knopf.
 */
@Composable
internal fun Wertkachel(
    name: String,
    zahl: String,
    modifier: Modifier = Modifier,
    ecke: String? = null,
    fuss: String? = null,
    auffaellig: Boolean = false,
    leer: Boolean = false,
    laeuft: Float? = null,
    aktiv: Boolean = true,
    beiDruck: () -> Unit = {},
    inhalt: (@Composable () -> Unit)? = null,
) {
    val rand = when {
        laeuft != null -> Farben.Amber
        auffaellig -> Farben.Signal.copy(alpha = 0.7f)
        else -> Farben.Rand
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .flaeche(
                farbe = if (auffaellig) Farben.HauchSignal.compositeOver(Farben.Flaeche) else Farben.FlaecheHoch.copy(alpha = if (leer) 0.45f else 1f),
                randfarbe = rand,
                ecke = 9.dp,
                mitLichtkante = false,
            )
            .clickable(
                enabled = aktiv,
                onClick = beiDruck,
                role = Role.Button,
                indication = null,
                interactionSource = null,
            )
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                name,
                style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                color = if (auffaellig) Farben.SignalHell else Farben.TextLeise,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ecke?.let { Text(it, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise) }
        }
        if (inhalt != null) {
            inhalt()
        } else {
            Text(
                zahl,
                style = Schrift.Gross.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                color = when {
                    leer -> Farben.TextSehrLeise
                    auffaellig -> Farben.SignalHell
                    else -> Farben.Text
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        fuss?.let {
            Text(it, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (laeuft != null) Laufbalken(laeuft)
    }
}

private fun Color.compositeOver(grund: Color): Color {
    val a = alpha
    return Color(
        red = red * a + grund.red * (1 - a),
        green = green * a + grund.green * (1 - a),
        blue = blue * a + grund.blue * (1 - a),
        alpha = 1f,
    )
}

/**
 * Der Kopf eines Bogens mit dem Weg „Erweitern" — `.patienten__kopf`, `.fwf__kopf`.
 */
@Composable
internal fun Bogenkopf(etikett: String, beiErweitern: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Etikett(etikett, Modifier.weight(1f))
        if (beiErweitern != null) Knopf("Erweitern", beiErweitern, art = Knopfart.Leise, kompakt = true)
    }
}

/**
 * Die große Form eines Bogens am Handy — `.dialog--voll` im Web: über die ganze
 * Fläche, mit Etikett, Titel und „Zuklappen" im Kopf. Der Inhalt rollt, der Kopf
 * bleibt stehen.
 */
@Composable
internal fun Vollblende(
    etikett: String,
    titel: String,
    beiSchliessen: () -> Unit,
    schliessen: String = "Zuklappen",
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = beiSchliessen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val rand = WindowInsets.safeDrawing.asPaddingValues()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Farben.Bg)
                .padding(top = rand.calculateTopPadding(), bottom = rand.calculateBottomPadding()),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .kopfverlauf()
                    .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
            ) {
                Column(Modifier.weight(1f)) {
                    Etikett(etikett)
                    Text(titel, style = Schrift.Titel, color = Farben.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Knopf(schliessen, beiSchliessen, art = Knopfart.Leise, kompakt = true)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Abstand.Gross),
                content = inhalt,
            )
        }
    }
}

/**
 * Ein Blatt der Auswahlleiste über einem Bogen — `.patienten__blatt`: Kennung,
 * darunter ein Wort, und die Kante in der Farbe dessen, was es meldet.
 */
@Composable
internal fun Auswahlblatt(
    kennung: String,
    unterzeile: String?,
    an: Boolean,
    farbe: Color?,
    beiDruck: () -> Unit,
    unterFarbe: Color = Farben.TextSehrLeise,
) {
    Column(
        modifier = Modifier
            .defaultMinSize(minHeight = Ziel.Kompakt, minWidth = 64.dp)
            .background(if (an) Farben.FlaecheAktiv else Farben.FlaecheHoch, Rundung.Klein)
            .border(1.dp, if (an) Farben.Amber else (farbe ?: Farben.Rand), Rundung.Klein)
            .clickable(onClick = beiDruck, role = Role.Tab, indication = null, interactionSource = null)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
    ) {
        Text(kennung, style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Text)
        unterzeile?.let { Text(it, style = Schrift.Winzig, color = unterFarbe, maxLines = 1) }
    }
}

/** Eine Zeile „○ / ✓ Bedingung" — `.pruefung` im Web: der Stand statt eines grauen Feldes. */
@Composable
internal fun Pruefzeile(erfuellt: Boolean, text: String) {
    Text(
        (if (erfuellt) "✓ " else "○ ") + text,
        style = Schrift.Klein,
        color = if (erfuellt) Farben.GruenHell else Farben.TextSehrLeise,
    )
}
