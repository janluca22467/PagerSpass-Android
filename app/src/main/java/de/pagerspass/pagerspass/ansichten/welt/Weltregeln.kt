package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.ui.graphics.Color
import de.pagerspass.pagerspass.mobil.Weltfahrt
import de.pagerspass.pagerspass.netz.WeltLage
import de.pagerspass.pagerspass.netz.WeltVorlage
import de.pagerspass.pagerspass.ui.theme.Farben
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Die kleinen Regeln der Welt — übertragen aus `components/welt/`
 * (`stationierung.ts`, `wachenarten.ts`, `deckung.ts`, `masse.ts`).
 *
 * <b>Nur, was der Server nicht mitschickt.</b> Was eine Wache aufnehmen darf,
 * steht im Web als Anzeigeregel neben der Serverprüfung — dieselbe Zeile steht
 * hier, damit die Kaufliste nichts anbietet, was der Server ablehnt. Alles, was
 * der Server rechnet (Deckung, Fehlt-Satz, Preise), wird hier *nicht*
 * nachgerechnet.
 */

private val DEUTSCH = Locale.GERMANY

/** „25.000“ — Zahlen, wie sie im Web mit `toLocaleString('de-DE')` stehen. */
fun zahl(n: Number): String = NumberFormat.getIntegerInstance(DEUTSCH).format(n)

/** Die Welt-Credits als Zeichen: die Raute, dieselbe wie `Waehrung.vue`. */
const val WAEHRUNG = "◆"

fun credits(n: Number): String = "${zahl(n)} $WAEHRUNG"

/** Der Bauartname — wie `ARTNAME` in `wachenarten.ts`. */
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

/** Welche Organisationen in einer Bauart stehen dürfen — `stationierung.ts`. */
fun organisationenFuer(art: String): List<String> = when (art) {
    "Feuerwehrhaus", "Loeschgruppe", "Feuerwache", "Werkfeuerwehr", "Logistikzentrum",
    "Waldbrandstuetzpunkt",
    -> listOf("Feuerwehr")
    "Hilfsorganisation", "Rettungswache", "Aussenwache", "Rettungshubschrauber",
    "Bergwacht", "Rettungshundestaffel", "Seenotrettung",
    -> listOf("Rettungsdienst")
    "Hauptwache", "Wasserrettung" -> listOf("Feuerwehr", "Rettungsdienst")
    "Polizeiwache", "Bereitschaftspolizei", "Kriminaldirektion", "Polizeifliegerstaffel" -> listOf("Polizei")
    "Ortsverband" -> listOf("Thw")
    else -> emptyList()
}

/** Ob in dieser Bauart überhaupt Fahrzeuge stehen — nicht in Werkstatt und Lehrgang. */
fun traegtFahrzeuge(art: String): Boolean = organisationenFuer(art).isNotEmpty()

/** Kategorien, die eine eigene Bauart haben und anderswo nicht stehen. */
private val RESERVIERT = listOf("Wasserrettung", "Seenotrettung", "Abrollbehälter", "Einsatzeinheiten", "Spezialkräfte")

private fun kategorienFuer(art: String): List<String>? = when (art) {
    "Wasserrettung" -> listOf("Wasserrettung", "Seenotrettung")
    "Logistikzentrum" -> listOf("Abrollbehälter", "Wasserversorgung", "Logistik und Mannschaft", "Anhänger und Boote")
    "Bereitschaftspolizei" -> listOf("Einsatzeinheiten", "Spezialkräfte")
    "Rettungshundestaffel" -> listOf("Rettungshunde")
    "Bergwacht" -> listOf("Bergrettung")
    "Waldbrandstuetzpunkt" -> listOf("Vegetationsbrand")
    "Kriminaldirektion" -> listOf("Kriminaldienst")
    "Polizeifliegerstaffel" -> listOf("Luftunterstützung")
    "Seenotrettung" -> listOf("Seenotrettung")
    else -> null
}

fun fliegt(v: WeltVorlage): Boolean = v.faehigkeiten.any { it.equals("Lufttransport", ignoreCase = true) }

/** Ob eine Vorlage in dieser Bauart stehen darf — `passtZurWache` im Web. */
fun passtZurWache(art: String, v: WeltVorlage): Boolean {
    if (art == "Rettungshubschrauber") return fliegt(v) && v.organisation == "Rettungsdienst"
    if (fliegt(v) && v.organisation == "Rettungsdienst") return false
    if (v.organisation !in organisationenFuer(art)) return false
    val erlaubt = kategorienFuer(art)
    return erlaubt?.contains(v.kategorie) ?: (v.kategorie !in RESERVIERT)
}

/** Je Typ eine Vorlage — die mit der kürzesten Kennung (`jeTypEine`). */
fun jeTypEine(vorlagen: Collection<WeltVorlage>): List<WeltVorlage> {
    val beste = LinkedHashMap<String, WeltVorlage>()
    vorlagen.forEach { v ->
        val schluessel = v.typ.lowercase()
        val bisher = beste[schluessel]
        if (bisher == null || v.id.length < bisher.id.length || (v.id.length == bisher.id.length && v.id < bisher.id)) {
            beste[schluessel] = v
        }
    }
    return beste.values.toList()
}

/** Die Organisationsfarben — dieselben Werte wie `--org-*` im Web. */
fun orgFarbe(organisation: String?): Color = when (organisation) {
    "Feuerwehr" -> Farben.OrgFeuerwehr
    "Rettungsdienst" -> Farben.OrgRettungsdienst
    "Thw" -> Farben.OrgThw
    "Polizei" -> Farben.OrgPolizei
    else -> Color(0xFF8291A5)
}

fun wachenfarbe(art: String): Color = when (art) {
    "Werkstatt" -> Color(0xFF8291A5)
    "Lehrgangseinrichtung" -> Farben.Violett
    else -> orgFarbe(organisationenFuer(art).firstOrNull())
}

/** Die Farbe einer Priorität — `.weltlage--prio1..3` in `welt.css`: Blau, Orange, Rot. */
fun prioFarbe(prioritaet: Int): Color = when {
    prioritaet >= 3 -> Farben.Signal
    prioritaet == 2 -> Farben.FmsAnfahrt
    else -> Farben.Blau
}

/** Die FMS-Farbe einer Plakette — dieselbe Staffel wie im Rundenspiel. */
fun fmsFarbe(status: Int): Color = when (status) {
    1, 2 -> Farben.FmsFrei
    3 -> Farben.FmsAnfahrt
    4 -> Farben.FmsVorOrt
    5 -> Farben.FmsSprechwunsch
    6 -> Farben.FmsDefekt
    7, 8 -> Farben.FmsGebunden
    else -> Farben.TextSehrLeise
}

fun organisationsname(o: String): String = when (o) {
    "Thw" -> "THW"
    else -> o
}

// ------------------------------------------------------------------- Deckung

/** „3/5“ — Eingetroffene gegen Geforderte; ohne Zahl ein Strich, kein Raten. */
fun deckungstext(lage: WeltLage): String = "${lage.eingetroffen ?: "–"}/${lage.gefordertFahrzeuge}"

fun gedeckt(lage: WeltLage): Boolean = lage.fehlt == null

// --------------------------------------------------------------------- Maße

fun entfernungText(meter: Double): String =
    if (meter < 1000) "${meter.toInt()} m" else String.format(DEUTSCH, "%.1f km", meter / 1000)

fun laengenwort(meter: Double): String =
    if (meter >= 1000) String.format(DEUTSCH, "%.1f km", meter / 1000) else "${meter.toInt()} m"

fun flaechenwort(qm: Double): String =
    if (qm >= 10_000) String.format(DEUTSCH, "%.1f ha", qm / 10_000) else "${qm.toInt()} m²"

fun zuglaengeMeter(punkte: List<Pair<Double, Double>>): Double =
    punkte.zipWithNext().sumOf { (a, b) -> Weltfahrt.luftlinie(a.first, a.second, b.first, b.second) }

fun vieleckQuadratmeter(ecken: List<Pair<Double, Double>>): Double {
    if (ecken.size < 3) return 0.0
    val grad = 111_320.0
    val breite = kotlin.math.cos(ecken.sumOf { it.first } / ecken.size * Math.PI / 180)
    var summe = 0.0
    for (i in ecken.indices) {
        val a = ecken[i]
        val b = ecken[(i + 1) % ecken.size]
        summe += a.second * grad * breite * (b.first * grad) - b.second * grad * breite * (a.first * grad)
    }
    return kotlin.math.abs(summe) / 2
}

// -------------------------------------------------------------------- Zeiten

private val TAG_UHR = DateTimeFormatter.ofPattern("dd.MM., HH:mm", DEUTSCH)
private val UHR = DateTimeFormatter.ofPattern("HH:mm", DEUTSCH)
private val TERMIN = DateTimeFormatter.ofPattern("EEEE, dd.MM., HH:mm", DEUTSCH)
private val TERMIN_KURZ = DateTimeFormatter.ofPattern("EE dd.MM., HH:mm", DEUTSCH)

private fun alsZeit(roh: String?): java.time.ZonedDateTime? {
    val ms = roh?.takeIf { it.isNotBlank() }?.let { Weltfahrt.zeit(it) } ?: return null
    if (ms == 0L) return null
    return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
}

fun tagUndUhr(roh: String?): String = alsZeit(roh)?.let { TAG_UHR.format(it) } ?: "—"
fun uhrzeit(roh: String?): String = alsZeit(roh)?.let { UHR.format(it) } ?: "—"
fun termin(roh: String?): String = alsZeit(roh)?.let { TERMIN.format(it) } ?: "—"
fun terminKurz(roh: String?): String = alsZeit(roh)?.let { TERMIN_KURZ.format(it) } ?: "—"

/** „2 h 5 min“ oder „12 min“. */
fun dauer(minuten: Int): String = if (minuten >= 60) "${minuten / 60} h ${minuten % 60} min" else "$minuten min"

/** Der Ausbau dauert — „3 h“ oder „40 min“, gerundet wie im Web. */
fun dauerGrob(minuten: Int): String = if (minuten >= 60) "${Math.round(minuten / 60.0)} h" else "$minuten min"

fun tage(n: Int) = if (n == 1) "1 Tag" else "$n Tage"
