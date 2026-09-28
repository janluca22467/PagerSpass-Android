package de.pagerspass.pagerspass.ansichten.welt

import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.WeltLage
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Die Regeln der Welt, die die Anzeige kennen muss — übertragen aus
 * `components/welt/stationierung.ts`, `wachenarten.ts`, `deckung.ts` und `masse.ts`.
 *
 * <b>Entschieden wird auf dem Server</b> (`Weltstationierung`, `Weltspeicher`). Was hier
 * steht, sorgt nur dafür, dass die Kaufliste nicht anbietet, was der Server gleich darauf
 * ablehnt — und dass Maße in Worten dastehen.
 */

// ---------------------------------------------------------------- Wachenarten

/** Wie eine Bauart heißt, wenn ein Mensch sie liest — `ARTNAME` im Web. */
val ARTNAME: Map<String, String> = mapOf(
    "Feuerwehrhaus" to "Feuerwehrhaus",
    "Loeschgruppe" to "Löschgruppenhaus",
    "Hilfsorganisation" to "Rettungswache (HiOrg)",
    "Rettungswache" to "Rettungswache",
    "Aussenwache" to "Rettungswachen-Außenstelle",
    "Ortsverband" to "THW-Ortsverband",
    "Polizeiwache" to "Polizeiwache",
    "Hauptwache" to "Feuer- und Rettungswache",
    "Feuerwache" to "Feuerwache (BF)",
    "Werkfeuerwehr" to "Werkfeuerwehr",
    "Rettungshubschrauber" to "Luftrettungsstation",
    "Lehrgangseinrichtung" to "Lehrgangseinrichtung",
    "Wasserrettung" to "Wasserrettungsstation",
    "Logistikzentrum" to "Logistikzentrum",
    "Bereitschaftspolizei" to "Bereitschaftspolizei",
    "Werkstatt" to "Fahrzeugwerkstatt",
    "Rettungshundestaffel" to "Rettungshundestaffel",
    "Bergwacht" to "Bergwacht-Station",
    "Waldbrandstuetzpunkt" to "Waldbrand-Stützpunkt",
    "Kriminaldirektion" to "Kriminaldirektion",
    "Polizeifliegerstaffel" to "Polizeifliegerstaffel",
    "Seenotrettung" to "Seenotrettungsstation",
)

fun artname(art: String): String = ARTNAME[art] ?: art

/**
 * Welche Organisationen eine Bauart aufnimmt — die Hauptwache und die
 * Wasserrettungsstation tragen zwei, Lehrgangseinrichtung und Werkstatt keine.
 */
fun organisationenFuer(art: String): List<String> = when (art) {
    "Feuerwehrhaus", "Loeschgruppe", "Feuerwache", "Werkfeuerwehr", "Logistikzentrum" ->
        listOf("Feuerwehr")
    "Hilfsorganisation", "Rettungswache", "Aussenwache", "Rettungshubschrauber" ->
        listOf("Rettungsdienst")
    "Hauptwache", "Wasserrettung" -> listOf("Feuerwehr", "Rettungsdienst")
    "Polizeiwache", "Bereitschaftspolizei", "Kriminaldirektion", "Polizeifliegerstaffel" ->
        listOf("Polizei")
    "Ortsverband" -> listOf("Thw")
    "Waldbrandstuetzpunkt" -> listOf("Feuerwehr")
    "Bergwacht", "Rettungshundestaffel", "Seenotrettung" -> listOf("Rettungsdienst")
    else -> emptyList()
}

/**
 * Die Kategorien, die einer Sonderwache gehören — keine andere Bauart nimmt sie auf.
 * Die sechs Häuser von 5.0.0.41 stehen bewusst nicht darin.
 */
private val RESERVIERT = listOf(
    "Wasserrettung",
    "Seenotrettung",
    "Abrollbehälter",
    "Einsatzeinheiten",
    "Spezialkräfte",
)

/** Die Kategorien einer Sonderwache — `null` heißt: jede, die keiner Sonderwache gehört. */
private fun kategorienFuer(art: String): List<String>? = when (art) {
    "Wasserrettung" -> listOf("Wasserrettung", "Seenotrettung")
    "Logistikzentrum" ->
        listOf("Abrollbehälter", "Wasserversorgung", "Logistik und Mannschaft", "Anhänger und Boote")
    "Bereitschaftspolizei" -> listOf("Einsatzeinheiten", "Spezialkräfte")
    "Rettungshundestaffel" -> listOf("Rettungshunde")
    "Bergwacht" -> listOf("Bergrettung")
    "Waldbrandstuetzpunkt" -> listOf("Vegetationsbrand")
    "Kriminaldirektion" -> listOf("Kriminaldienst")
    "Polizeifliegerstaffel" -> listOf("Luftunterstützung")
    "Seenotrettung" -> listOf("Seenotrettung")
    else -> null
}

/** Ob überhaupt ein Fahrzeug auf diese Bauart darf. */
fun traegtFahrzeuge(art: String): Boolean = organisationenFuer(art).isNotEmpty()

/** Ob die Vorlage fliegt — Luftrettung gehört auf die Luftrettungsstation. */
fun fliegt(vorlage: Fahrzeugvorlage): Boolean =
    vorlage.faehigkeiten.any { it.equals("Lufttransport", ignoreCase = true) }

/**
 * Ob diese Vorlage auf diese Wache darf — `Weltstationierung.PasstZurWache` im Spiegel.
 */
fun passtZurWache(art: String, vorlage: Fahrzeugvorlage): Boolean {
    if (art == "Rettungshubschrauber") return fliegt(vorlage) && vorlage.organisation == "Rettungsdienst"
    if (fliegt(vorlage) && vorlage.organisation == "Rettungsdienst") return false
    if (vorlage.organisation !in organisationenFuer(art)) return false

    val erlaubt = kategorienFuer(art)
    return erlaubt?.contains(vorlage.kategorie) ?: (vorlage.kategorie !in RESERVIERT)
}

/**
 * Je Typ genau eine Vorlage — die mit der kürzesten Kennung, bei Gleichstand die im
 * Alphabet erste. Der Katalog führt manche Typen mehrfach (je Hilfsorganisation).
 */
fun jeTypEine(vorlagen: List<Fahrzeugvorlage>): List<Fahrzeugvorlage> {
    val beste = LinkedHashMap<String, Fahrzeugvorlage>()
    for (v in vorlagen) {
        val schluessel = v.typ.lowercase()
        val bisher = beste[schluessel]
        if (bisher == null ||
            v.id.length < bisher.id.length ||
            (v.id.length == bisher.id.length && v.id < bisher.id)
        ) {
            beste[schluessel] = v
        }
    }
    return beste.values.toList()
}

/** Die Farbklasse einer Wache — Werkstatt und Lehrgang in Amber, sonst die Organisation. */
fun wachenorganisation(art: String): String? = when (art) {
    "Werkstatt", "Lehrgangseinrichtung" -> null
    else -> organisationenFuer(art).firstOrNull()
}

/** Das Zeichen im Wachenhaus: Tor, Schraubenschlüssel oder Buch. */
fun wachenzeichen(art: String): String = when (art) {
    "Werkstatt" -> "werkstatt"
    "Lehrgangseinrichtung" -> "lehrgang"
    else -> "tor"
}

// -------------------------------------------------------------------- Deckung

/**
 * Wie viele Fahrzeuge eingetroffen sind — `null`, wenn der Server es nicht sagt.
 *
 * <b>Nicht `?: 0`.</b> Eine Null behauptete „keiner da", obwohl nur die Auskunft fehlt —
 * und eine grüne Marke ohne Zahl dahinter wäre die freundlichste Art, jemanden in die
 * Irre zu schicken.
 */
fun eingetroffen(lage: WeltLage): Int? = lage.eingetroffen

/** Was an der Marke steht: „3/4", oder „–/4", wenn die Zahl fehlt. */
fun deckungstext(lage: WeltLage): String = "${eingetroffen(lage) ?: "–"}/${lage.gefordertFahrzeuge}"

// ---------------------------------------------------------------------- Maße

/** Eine Entfernung in Worten: unter einem Kilometer in Metern. */
fun entfernungText(meter: Double): String =
    if (meter < 1000) "${meter.roundToInt()} m" else "${komma(meter / 1000.0, 1)} km"

fun laengenwort(meter: Double): String =
    if (meter >= 1000) "${komma(meter / 1000.0, 1)} km" else "${meter.roundToInt()} m"

fun flaechenwort(quadratmeter: Double): String =
    if (quadratmeter >= 10_000) "${komma(quadratmeter / 10_000.0, 1)} ha" else "${quadratmeter.roundToInt()} m²"

/** Die Luftlinie zwischen zwei Punkten — Haversine, auf den Meter genug. */
fun luftlinieMeter(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(bLat - aLat)
    val dLon = Math.toRadians(bLon - aLon)
    val m = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * asin(min(1.0, sqrt(m)))
}

/** Die Länge eines Streckenzugs in Metern. */
fun zuglaengeMeter(punkte: List<Pair<Double, Double>>): Double {
    var summe = 0.0
    for (i in 1 until punkte.size) {
        summe += luftlinieMeter(punkte[i - 1].first, punkte[i - 1].second, punkte[i].first, punkte[i].second)
    }
    return summe
}

/** Die Fläche eines Vielecks in Quadratmetern — die Gaußsche Trapezformel, flach gerechnet. */
fun vieleckQuadratmeter(ecken: List<Pair<Double, Double>>): Double {
    if (ecken.size < 3) return 0.0
    val meterJeGrad = 111_320.0
    val breitenfaktor = cos(Math.toRadians(ecken.sumOf { it.first } / ecken.size))

    var summe = 0.0
    for (i in ecken.indices) {
        val a = ecken[i]
        val b = ecken[(i + 1) % ecken.size]
        summe += a.second * meterJeGrad * breitenfaktor * (b.first * meterJeGrad) -
            b.second * meterJeGrad * breitenfaktor * (a.first * meterJeGrad)
    }
    return abs(summe) / 2.0
}

// ------------------------------------------------------------------- Zahlen

/** Eine ganze Zahl mit Tausenderpunkt — `toLocaleString('de-DE')`. */
fun zahl(n: Int): String = String.format(Locale.GERMANY, "%,d", n)

fun zahl(n: Long): String = String.format(Locale.GERMANY, "%,d", n)

/** Eine Kommazahl mit so vielen Stellen — „1,5". */
fun komma(wert: Double, stellen: Int): String = String.format(Locale.GERMANY, "%.${stellen}f", wert)

/** Eine Dauer in Minuten in Worten: „45 min", „2 h 10 min", „1 T 3 h". */
fun dauerwort(minuten: Int): String = when {
    minuten < 60 -> "$minuten min"
    minuten < 24 * 60 -> {
        val h = minuten / 60
        val m = minuten % 60
        if (m == 0) "$h h" else "$h h $m min"
    }
    else -> {
        val t = minuten / (24 * 60)
        val h = (minuten % (24 * 60)) / 60
        if (h == 0) "$t T" else "$t T $h h"
    }
}

/** Eine Restzeit in Sekunden als Uhr: „4:05", „1:02:03". */
fun uhrwort(sekunden: Long): String {
    val s = sekunden.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val r = s % 60
    return if (h > 0) String.format(Locale.GERMANY, "%d:%02d:%02d", h, m, r)
    else String.format(Locale.GERMANY, "%d:%02d", m, r)
}
