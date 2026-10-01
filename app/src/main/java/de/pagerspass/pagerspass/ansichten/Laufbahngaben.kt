package de.pagerspass.pagerspass.ansichten

import androidx.compose.ui.graphics.Color
import de.pagerspass.pagerspass.melder.Melderkatalog
import de.pagerspass.pagerspass.ui.schmuck.Schmuck

/**
 * Die Zierstücke, die eine Laufbahnstufe verleiht — für die Stufenkarten im
 * Dienstbuch, übertragen aus `gaben()` in `LaufbahnView.vue`.
 *
 * <b>Die Regeln wohnen beim Server, die Kataloge beim Client.</b> Welche Stufe
 * welches Gehäuse, welchen Rahmen oder welchen Ton öffnet, steht im Web in
 * Katalogen des Clients und hier in [Melderkatalog] und [Schmuck]. Diese Datei
 * beschriftet nur — sie entscheidet nichts.
 *
 * <b>Nur, was die Stufe verleiht.</b> Alles mit einem Preis gehört dem Shop und
 * wird gekauft, nicht erreicht; alles am Abo hängt an keiner Stufe. Ohne diese
 * Trennung stünde auf der Karte für Stufe 1 der halbe Laden — die kaufbaren
 * Stücke tragen `abLevel = 1`, weil sie an keine Stufe gebunden sind.
 *
 * <b>Die Reihenfolge ist die des Web:</b> Gehäuse zuerst, dann Gesichter, dann
 * der übrige Schmuck. Eine Karte zeigt nur drei Zeilen, und wer auf Stufe 10
 * aufsteigt, soll „Meldergerät" lesen und nicht „+1 weitere".
 */
internal data class Zierstueck(
    val text: String,
    /** Nur bei Melder-Gesichtern: die zwei Vorschautöne. */
    val gehaeuse: Color? = null,
    val lcd: Color? = null,
)

internal fun zierstueckeDerStufe(level: Int): List<Zierstueck> = buildList {
    Melderkatalog.BAUFORMEN
        .filter { it.abLevel == level && level > 1 && !it.premium }
        .forEach { add(Zierstueck("Meldergerät „${it.name}“")) }

    Melderkatalog.GESICHTER
        .filter { it.abLevel == level && it.preisCredits == null && !it.premium && it.id != "standard" }
        .forEach { add(Zierstueck("Melder „${it.name}“", it.gehaeuse, it.lcd)) }

    Schmuck.RAHMEN
        .filter { erspielt(it, level) && it.id != "keiner" }
        .forEach { add(Zierstueck("Rahmen „${it.name}“")) }

    Schmuck.KOPFMUSTER
        .filter { erspielt(it, level) && it.id != "keines" }
        .forEach { add(Zierstueck("Kopfmuster „${it.name}“")) }

    WAPPEN_AB_STUFE
        .filter { (_, ab) -> ab == level }
        .forEach { (name, _) -> add(Zierstueck("Wappen „$name“")) }

    if (level == GOLDFARBE_AB_STUFE) add(Zierstueck("Wappenfarbe Gold"))

    Melderkatalog.TOENE
        .filter { it.abLevel == level && level > 1 && it.preisCredits == null && !it.premium }
        .forEach { add(Zierstueck("Alarmton „${it.name}“")) }

    SCHICHTKARTEN_AB_STUFE
        .filter { (_, ab) -> ab == level }
        .forEach { (name, _) -> add(Zierstueck("Schichtkarte „$name“")) }
}

private fun erspielt(s: Schmuck.Stueck, level: Int): Boolean =
    s.abLevel == level && s.preisCredits == null && !s.premium

/**
 * Die Wappenzeichen, die die Laufbahn verleiht — `WAPPEN_AB_LEVEL` samt
 * `WAPPEN_LABEL` des Web. Die übrigen gehören jedem oder dem Abo.
 */
private val WAPPEN_AB_STUFE = listOf(
    "Drehleiter" to 15,
    "Rettungshubschrauber" to 30,
    "Rettungsboot" to 45,
    "Funkmast" to 58,
)

/** Die goldene Wappenfarbe (Nummer 8 der Palette) — `GOLDFARBE_AB_LEVEL`. */
private const val GOLDFARBE_AB_STUFE = 70

/** Die erspielten Schichtkarten aus `SCHICHTKARTEN_DESIGNS`; der Rest hängt am Abo. */
private val SCHICHTKARTEN_AB_STUFE = listOf(
    "Bernstein-LCD" to 25,
    "Heller Aushang" to 50,
)
