package de.pagerspass.pagerspass.ansichten

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Fahrzeugkennung
import de.pagerspass.pagerspass.mobil.rememberKennungsform
import de.pagerspass.pagerspass.netz.Klinik
import de.pagerspass.pagerspass.netz.Ort
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.netz.Spieler
import de.pagerspass.pagerspass.ui.theme.Farben
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Die kleinen Rechnungen des Leitstellentischs — übertragen aus `utils/anfahrt.ts`,
 * `composables/tableauGruppen.ts`, `composables/besatzungsart.ts`,
 * `composables/fahrzeugSuche.ts` und den Tafeln in `types.ts`.
 *
 * <b>Eine Stelle für alle Teile des Tischs.</b> Einsatzliste, Einsatzbogen,
 * Alarmdialog und Tableau zeigen dieselben Entfernungen, dieselben Kennungen und
 * dieselbe Ordnung der Abteilungen; stünde die Rechnung je Teil, stünde am
 * Alarmdialog eine andere Fahrzeit als an der Kachel desselben Wagens.
 */

// ------------------------------------------------------------------- Zeit

/** Ein ISO-Zeitstempel des Servers in Millisekunden — `null`, wenn er unlesbar ist. */
internal fun zeitMillis(roh: String?): Long? {
    if (roh.isNullOrBlank()) return null
    return runCatching { Instant.parse(roh).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(roh).toInstant().toEpochMilli() }.getOrNull()
}

/**
 * Eine Uhr, die im Takt weiterläuft.
 *
 * <b>Der Raumzustand allein reicht nicht</b> — dieselbe Falle wie im Web: Er
 * ändert sich nicht, solange niemand funkt, und eine Wartezeit daran stünde still
 * und behauptete „0:04", während jemand seit zwei Minuten wartet.
 */
@Composable
internal fun rememberJetzt(takt: Long = 1_000L): Long {
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(takt) {
        while (true) {
            jetzt = System.currentTimeMillis()
            delay(takt)
        }
    }
    return jetzt
}

/** „m:ss" aus Sekunden — Wartezeiten und Restzeiten. */
internal fun minSek(sekunden: Long): String {
    val s = sekunden.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** Wie lange seit einem Zeitstempel vergangen ist, als „m:ss". */
internal fun wartezeit(seit: String?, jetzt: Long): String {
    val dann = zeitMillis(seit) ?: return "0:00"
    return minSek((jetzt - dann) / 1000)
}

/**
 * Ob das Mikrofon erlaubt ist — und wenn nicht, die Frage danach.
 *
 * <b>Für die gehaltenen Tasten</b> (Alarmmeldung, Notrufsprechen): Dort darf die
 * Erlaubnis nicht erst nach dem Loslassen ankommen und dann ein Mikrofon öffnen,
 * das niemand mehr schließt. Ohne Erlaubnis wird nur gefragt; gesprochen wird beim
 * nächsten Druck.
 */
@Composable
internal fun rememberMikrofonpruefung(): () -> Boolean {
    val zusammenhang = LocalContext.current
    val frage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        val da = ContextCompat.checkSelfPermission(zusammenhang, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!da) frage.launch(Manifest.permission.RECORD_AUDIO)
        da
    }
}

// ------------------------------------------------------ Weg und Anfahrt

/** Luftlinie in Metern — `distanzMeter` aus `utils/anfahrt.ts`. */
internal fun distanzMeter(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
    val rad = Math.PI / 180.0
    val dLat = (bLat - aLat) * rad
    val dLon = (bLon - aLon) * rad
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(aLat * rad) * cos(bLat * rad) * sin(dLon / 2) * sin(dLon / 2)
    return 6_371_000.0 * 2 * atan2(sqrt(h), sqrt(1 - h))
}

/** Die Luftlinie nach der Formel des Einsatzbogens (Haversine mit `asin`). */
internal fun distanzMeterAsin(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
    val r = 6_371_000.0
    val rad = { g: Double -> g * Math.PI / 180.0 }
    val dLat = rad(bLat - aLat)
    val dLon = rad(bLon - aLon)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(rad(aLat)) * cos(rad(bLat)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * asin(min(1.0, sqrt(a)))
}

/** Die restliche Strecke entlang der Route — sonst die Luftlinie. */
internal fun restEntfernungMeter(
    vonLat: Double,
    vonLon: Double,
    route: List<Ort>,
    routeIndex: Int,
    zielLat: Double,
    zielLon: Double,
): Double {
    val punkte = route.drop(min(routeIndex, route.size))
    if (punkte.isEmpty()) return distanzMeter(vonLat, vonLon, zielLat, zielLon)

    var meter = distanzMeter(vonLat, vonLon, punkte[0].lat, punkte[0].lon)
    for (i in 1 until punkte.size) {
        meter += distanzMeter(punkte[i - 1].lat, punkte[i - 1].lon, punkte[i].lat, punkte[i].lon)
    }
    return meter
}

/** Das Grundtempo je Organisation und Priorität in m/s — `geschwindigkeitMs`. */
internal fun geschwindigkeitMs(organisation: String, prioritaet: Int): Double {
    val basis = when (organisation) {
        "Rettungsdienst" -> 18.0
        "Polizei" -> 20.0
        "Feuerwehr" -> 15.0
        "Thw" -> 12.0
        else -> 14.0
    }
    val prio = when {
        prioritaet >= 3 -> 1.35
        prioritaet == 2 -> 1.15
        else -> 1.0
    }
    return basis * prio
}

/**
 * Das Tempo, mit dem der Server wirklich fährt — Wetter und Zeitfaktor mit drin
 * (`useFahrttempo`). Ein fester Wert hier versprach eine andere Fahrzeit als die
 * Kachel desselben Wagens.
 */
internal fun tempoMs(raum: Raumzustand?, organisation: String, prioritaet: Int): Double {
    val brems = raum?.wetter?.bremsfaktor ?: 1.0
    val zeitfaktor = if (raum?.settings?.zeitmodus == "Simulation") 3.0 else 1.0
    return geschwindigkeitMs(organisation, prioritaet) * brems * max(zeitfaktor, 0.01)
}

internal fun formatEntfernung(meter: Double): String =
    if (meter < 1000) "${max(1, meter.roundToInt())} m"
    else "%.1f km".format(meter / 1000.0)

internal fun formatAnfahrtszeit(sekunden: Double): String {
    val minuten = (sekunden / 60.0).roundToInt()
    return if (minuten < 1) "< 1 min" else "$minuten min"
}

// ------------------------------------------------------------ Tafeln

/** Das Formzeichen zur Priorität — nur die Ausnahmen tragen eines. */
internal fun prioZeichen(prio: Int): String = when {
    prio >= 3 -> "▲▲"
    prio <= 1 -> "▽"
    else -> ""
}

internal fun prioWort(prio: Int): String = when {
    prio >= 3 -> "Priorität hoch"
    prio <= 1 -> "Priorität gering"
    else -> "Priorität normal"
}

/** Die Farbe einer Priorität — rot, amber, grün. */
internal fun prioFarbe(prio: Int): Color = when {
    prio >= 3 -> Farben.SignalHell
    prio == 2 -> Farben.AmberHell
    else -> Farben.GruenHell
}

/** Hektar mit einer Nachkommastelle, ab zehn ohne — wie der Server sie schreibt. */
internal fun hektarText(hektar: Double): String =
    if (hektar >= 10) hektar.roundToInt().toString()
    else "%.1f".format(hektar).replace('.', ',')

/** `WETTER_ZEICHEN` — ein Zeichen je Wetterlage. */
internal fun wetterZeichen(lage: String): String = when (lage) {
    "Klar" -> "○"
    "Regen" -> "☂"
    "Glaette" -> "❅"
    "Sturm" -> "≈"
    else -> "○"
}

/** `VERSORGUNG_LABEL` — wie eine Fachabteilung am Tisch heißt. */
internal fun versorgungText(stufe: String): String = when (stufe) {
    "Grundversorgung" -> "Grundversorgung"
    "Chirurgie" -> "Chirurgie"
    "Neurologie" -> "Stroke Unit"
    "Kardiologie" -> "Herzkatheter"
    "Trauma" -> "Traumazentrum"
    "Verbrennung" -> "Verbrennungszentrum"
    "Kinder" -> "Kinderklinik"
    else -> stufe
}

/** Die Ausweichleiter je Sichtungskategorie — `AUSWEICHSTUFEN`. */
private val AUSWEICHSTUFEN: Map<String, List<String>> = mapOf(
    "Rot" to listOf("Trauma", "Chirurgie", "Grundversorgung"),
    "Gelb" to listOf("Chirurgie", "Grundversorgung"),
    "Gruen" to listOf("Grundversorgung"),
    "Schwarz" to emptyList(),
)

/** Welches Bett dieses Haus einem Patienten dieser Kategorie stellt — `manvFach`. */
internal fun manvFach(klinik: Klinik, kategorie: String): String? =
    AUSWEICHSTUFEN[kategorie].orEmpty().firstOrNull {
        it in klinik.abteilungen && it !in klinik.abgemeldet
    }

// ------------------------------------------------------------ Kennung

/**
 * Die Kennung eines Fahrzeugs in der Form, die dieses Gerät eingestellt hat —
 * `kennung(f)` im Web. Der Kreis der Runde gehört zur Form „Träger und Kreis".
 */
@Composable
internal fun rememberKennung(raum: Raumzustand?): (Rundenfahrzeug) -> String {
    val form by rememberKennungsform()
    val kreis = raum?.settings?.landkreis.orEmpty()
    return remember(form, kreis) {
        { f: Rundenfahrzeug ->
            Fahrzeugkennung.kennung(f.kurzname, f.typ, f.organisation, f.hiOrg, form, kreis)
                .ifBlank { f.funkrufname }
        }
    }
}

/** Ob die eingestellte Kennung die Kennzahl ist — dann steht der Typ daneben. */
@Composable
internal fun kennungIstKennzahl(): Boolean {
    val form by rememberKennungsform()
    return form != "typ" && form != "orga"
}

// ------------------------------------------------------------ Besatzung

/** Wer auf diesem Platz sitzt — ein Mensch, ein Bot oder niemand. */
internal fun besatzungVon(spieler: List<Spieler>?, playerId: String?): Spieler? =
    playerId?.let { id -> spieler?.firstOrNull { it.id == id } }

/**
 * Die Marke neben dem Rufnamen — „BOT" oder der Name des Menschen, `null` bei
 * einem leeren Platz (`besatzungsmarke`).
 */
internal fun besatzungsmarke(spieler: List<Spieler>?, playerId: String?): Pair<Boolean, String>? {
    val p = besatzungVon(spieler, playerId) ?: return null
    return if (p.istBot) false to "BOT" else true to p.name
}

// ------------------------------------------------------------ Tableau

/** Eine Abteilung im Tableau — `TableauGruppe`. */
internal data class Tableaugruppe(
    val schluessel: String,
    val label: String,
    val farbe: Color,
    val fahrzeuge: List<Rundenfahrzeug>,
)

/** Die Sondereinheiten der Hilfsorganisationen — eine eigene Abteilung. */
private val HIORG_EINHEITEN = setOf("Dlrg", "Wasserwacht", "Bergwacht", "Dgzrs", "Brh")

private data class Abteilung(val schluessel: String, val label: String, val farbe: Color)

/** Die Reihenfolge der Abteilungen — Rettungsdienst zuerst, wie im Web. */
private val ABTEILUNGEN = listOf(
    Abteilung("Rettungsdienst", "Rettungsdienst", Farben.OrgRettungsdienst),
    Abteilung("Feuerwehr", "Feuerwehr", Farben.OrgFeuerwehr),
    Abteilung("HiOrg", "Hilfsorganisationen", Farben.HiorgDlrg),
    Abteilung("Thw", "THW", Farben.OrgThw),
    Abteilung("Polizei", "Polizei", Farben.OrgPolizei),
)

internal fun abteilungVon(f: Rundenfahrzeug): String =
    if (f.hiOrg in HIORG_EINHEITEN) "HiOrg" else f.organisation

/**
 * Funkrufnamen natürlich vergleichen — „1/44/2" vor „1/44/10"
 * (`localeCompare(…, { numeric: true })`).
 */
internal fun natuerlichVergleichen(a: String, b: String): Int {
    val teileA = Regex("\\d+|\\D+").findAll(a.lowercase()).map { it.value }.toList()
    val teileB = Regex("\\d+|\\D+").findAll(b.lowercase()).map { it.value }.toList()
    for (i in 0 until min(teileA.size, teileB.size)) {
        val x = teileA[i]
        val y = teileB[i]
        val zx = x.toBigIntegerOrNull()
        val zy = y.toBigIntegerOrNull()
        val vergleich = if (zx != null && zy != null) zx.compareTo(zy) else x.compareTo(y)
        if (vergleich != 0) return vergleich
    }
    return teileA.size - teileB.size
}

internal val rufnummerVergleich: Comparator<Rundenfahrzeug> = Comparator { a, b ->
    natuerlichVergleichen(a.kurzname, b.kurzname).takeIf { it != 0 }
        ?: natuerlichVergleichen(a.funkrufname, b.funkrufname)
}

/** Nach Abteilungen gegliedert, darin nach Rufnummer — `gruppiereTableau`. */
internal fun gruppiereTableau(fahrzeuge: List<Rundenfahrzeug>): List<Tableaugruppe> {
    val nach = fahrzeuge.groupBy { abteilungVon(it) }
    return ABTEILUNGEN.mapNotNull { a ->
        val liste = nach[a.schluessel].orEmpty()
        if (liste.isEmpty()) null
        else Tableaugruppe(a.schluessel, a.label, a.farbe, liste.sortedWith(rufnummerVergleich))
    }
}

/** Die Farbe der Kachel — der Träger, wo es einen gibt, sonst die Organisation. */
internal fun kachelFarbe(f: Rundenfahrzeug): Color {
    val traeger = Rundentexte.traegerFarbe(f.hiOrg)
    return if (f.hiOrg != "Keine" && traeger != Color.Transparent) traeger
    else Rundentexte.organisationFarbe(f.organisation)
}

internal fun traegerLabel(f: Rundenfahrzeug): String =
    if (f.hiOrg == "Keine") "" else Rundentexte.traeger(f.hiOrg)

/** Rufname, Kennung, Typ, Fähigkeit, Alarmschleife — `fahrzeugPasst`. */
internal fun fahrzeugPasst(f: Rundenfahrzeug, begriff: String): Boolean {
    val t = begriff.trim().lowercase()
    if (t.isEmpty()) return true
    return f.funkrufname.lowercase().contains(t) ||
        f.kurzname.lowercase().contains(t) ||
        f.typ.lowercase().contains(t) ||
        f.faehigkeiten.any { it.lowercase().contains(t) } ||
        f.schleifen.any { it.lowercase().contains(t) }
}

// --------------------------------------------------------- Funkgruppen

/** Die Kreisgruppen ohne die DMO-Gruppen der Einsatzstellen — die sieht die Leitstelle nicht. */
internal fun kreisfunkgruppen(raum: Raumzustand?) =
    raum?.settings?.funkgruppen.orEmpty().filter { it.einsatzId == null }

/** Der Rang einer Gruppe in der Liste — die Kanalfarbe hängt daran. */
internal fun funkgruppeRang(raum: Raumzustand?, id: String?): Int =
    if (id == null) -1 else raum?.settings?.funkgruppen.orEmpty().indexOfFirst { it.id == id }

internal fun funkgruppeVon(raum: Raumzustand?, id: String?) =
    id?.let { g -> raum?.settings?.funkgruppen.orEmpty().firstOrNull { it.id == g } }

// ------------------------------------------------------------ Zeichen

/**
 * Die Zeichen des Leitstellenkopfs — dieselben Striche wie die SVGs in
 * `LeitstelleView.vue`, auf dem 24er-Raster. Sie stehen hier und nicht in
 * `ui/zeichen/Zeichen.kt`, weil nur dieser Tisch sie trägt.
 */
internal object Leitstellenzeichen {
    /** Hörer mit Plus: ein Notruf, den die Leitstelle selbst anlegt. */
    val NeuerEinsatz = strichzeichen(
        "M15.5 21a12 12 0 0 1-12-12 2 2 0 0 1 2-2h2.2a1 1 0 0 1 1 .8l.6 2.6a1 1 0 0 1-.5 1.1l-1.4.8a10 10 0 0 0 4.5 4.5l.8-1.4a1 1 0 0 1 1.1-.5l2.6.6a1 1 0 0 1 .8 1V19a2 2 0 0 1-2 2Z",
        "M18 3v6",
        "M15 6h6",
    )

    /** Würfel mit fünf Augen. */
    val Wuerfel = strichzeichen(
        "M7 3.5h10a3.5 3.5 0 0 1 3.5 3.5v10a3.5 3.5 0 0 1-3.5 3.5H7A3.5 3.5 0 0 1 3.5 17V7A3.5 3.5 0 0 1 7 3.5z",
        "M8.5 8.5h.01",
        "M15.5 8.5h.01",
        "M12 12h.01",
        "M8.5 15.5h.01",
        "M15.5 15.5h.01",
        staerke = 1.6f,
        punktstaerke = 2.4f,
    )

    /** Warndreieck: die Warn-App der Leitstelle. */
    val Warnung = strichzeichen("M12 3.5 21.5 20h-19Z", "M12 10v4.5", "M12 17.3h.01")

    /** Zwei Köpfe: wer auf welchem Fahrzeug sitzt. */
    val Besatzung = strichzeichen(
        "M9.5 5.3a3.2 3.2 0 1 0 0 6.4 3.2 3.2 0 1 0 0-6.4z",
        "M3.8 19.5a5.7 5.7 0 0 1 11.4 0",
        "M16 5.8a3.2 3.2 0 0 1 0 5.4",
        "M17.4 14.6a5.7 5.7 0 0 1 2.8 4.9",
    )

    /** Tür mit Pfeil: den Platz verlassen. */
    val Verlassen = strichzeichen("M10 4H5a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h5", "M14 8l4 4-4 4", "M8 12h10")

    /** Ausschalter: das Ende der Schicht. */
    val Dienstende = strichzeichen("M12 3.5v7.5", "M7.4 6.6a7.5 7.5 0 1 0 9.2 0")

    /** Das Handy mit Wellen — der Funkbegleiter. */
    val Begleiter = strichzeichen(
        "M10 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        "M10.5 6h3M11 18.5h2",
        "M6 9.5a4.5 4.5 0 0 0 0 5M3.6 7.6a8 8 0 0 0 0 8.8",
        "M18 9.5a4.5 4.5 0 0 1 0 5M20.4 7.6a8 8 0 0 1 0 8.8",
    )

    /** Lautsprecher an. */
    val Laut = strichzeichen(
        "M11.5 5 7 9H3.5v6H7l4.5 4Z",
        "M15.2 9.2a4 4 0 0 1 0 5.6",
        "M17.9 6.6a7.8 7.8 0 0 1 0 10.8",
    )

    /** Lautsprecher stumm. */
    val Stumm = strichzeichen("M11.5 5 7 9H3.5v6H7l4.5 4Z", "m16.5 9.5 5 5m0-5-5 5")

    /** Drei Regler — die Toneinstellungen. */
    val Regler = strichzeichen(
        "M5 5v5m0 4v5M12 5v9m0 4v1M19 5v2m0 4v8",
        "M5 10.1a1.9 1.9 0 1 0 0 3.8 1.9 1.9 0 1 0 0-3.8z",
        "M12 14.1a1.9 1.9 0 1 0 0 3.8 1.9 1.9 0 1 0 0-3.8z",
        "M19 7.1a1.9 1.9 0 1 0 0 3.8 1.9 1.9 0 1 0 0-3.8z",
    )

    /** Sprechblase — getippten Funk vorlesen lassen (aus). */
    val VorlesenAus = strichzeichen(
        "M20 14.5a2.5 2.5 0 0 1-2.5 2.5H9l-4 3v-3H6.5A2.5 2.5 0 0 1 4 14.5v-7A2.5 2.5 0 0 1 6.5 5h11A2.5 2.5 0 0 1 20 7.5Z",
        "M8.5 11h7",
    )

    /** Sprechblase mit Schallwellen — getippter Funk wird vorgelesen. */
    val VorlesenAn = strichzeichen(
        "M20 14.5a2.5 2.5 0 0 1-2.5 2.5H9l-4 3v-3H6.5A2.5 2.5 0 0 1 4 14.5v-7A2.5 2.5 0 0 1 6.5 5h11A2.5 2.5 0 0 1 20 7.5Z",
        "M9.5 9.5v3",
        "M12 8v6",
        "M14.5 9.5v3",
    )
}

/**
 * Ein Zeichen aus Strichen — dieselben Vorgaben wie `strich` in `Zeichen.kt`:
 * 24er-Raster, keine Füllung, runde Enden und Ecken.
 */
private fun strichzeichen(
    vararg pfade: String,
    staerke: Float = 1.6f,
    punktstaerke: Float? = null,
): ImageVector = ImageVector.Builder(
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    pfade.forEach { d ->
        // Ein Pfad aus nur einem Hundertstelschritt ist ein Auge — er bekommt
        // die dickere Feder, damit er als Punkt steht und nicht als Strich.
        val istPunkt = d.endsWith("h.01")
        addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = if (istPunkt) (punktstaerke ?: staerke) else staerke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()
