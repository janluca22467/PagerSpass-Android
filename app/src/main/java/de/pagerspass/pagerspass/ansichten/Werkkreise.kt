package de.pagerspass.pagerspass.ansichten

import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.bundeslandname

/**
 * Kreise und Staaten für die Wahlen der Werkstatt (Übungen, Leitstellenbau) —
 * das Gegenstück zu `utils/kreisgruppen.ts` und `STAAT_VON` im Web.
 *
 * <b>Warum es das gibt.</b> Seit Österreich und die Schweiz dazugekommen sind,
 * stehen über fünfhundert Einträge im Katalog. Alphabetisch nach Land stünde
 * „Baden" (Niederösterreich) neben „Baden-Baden"; gruppiert trägt jede
 * Überschrift die Flagge ihres Staats: „🇦🇹 Tirol", „🇨🇭 Zürich", „🇩🇪 Bayern".
 *
 * <b>Eigene Datei und nicht in `netz/Spielmodelle.kt`</b>, damit die Werkstatt
 * nicht an der Bundesländertabelle der anderen Seiten dreht. Ein unbekanntes
 * Land fällt still auf Deutschland — dieselbe Regel wie im Web und auf einem
 * älteren Server, der noch keine Nachbarstaaten kennt.
 */

/** Die Staaten in der Reihenfolge der Wahl. */
internal val WERK_STAATEN = listOf("Deutschland", "Oesterreich", "Schweiz")

private val WERK_FLAGGE = mapOf(
    "Deutschland" to "🇩🇪",
    "Oesterreich" to "🇦🇹",
    "Schweiz" to "🇨🇭",
)

private val OESTERREICH = mapOf(
    "Burgenland" to "Burgenland",
    "Kaernten" to "Kärnten",
    "Niederoesterreich" to "Niederösterreich",
    "Oberoesterreich" to "Oberösterreich",
    "Salzburg" to "Salzburg",
    "Steiermark" to "Steiermark",
    "Tirol" to "Tirol",
    "Vorarlberg" to "Vorarlberg",
    "Wien" to "Wien",
)

private val SCHWEIZ = mapOf(
    "Aargau" to "Aargau",
    "AppenzellAusserrhoden" to "Appenzell Ausserrhoden",
    "AppenzellInnerrhoden" to "Appenzell Innerrhoden",
    "BaselLandschaft" to "Basel-Landschaft",
    "BaselStadt" to "Basel-Stadt",
    "Bern" to "Bern",
    "Freiburg" to "Freiburg",
    "Genf" to "Genf",
    "Glarus" to "Glarus",
    "Graubuenden" to "Graubünden",
    "Jura" to "Jura",
    "Luzern" to "Luzern",
    "Neuenburg" to "Neuenburg",
    "Nidwalden" to "Nidwalden",
    "Obwalden" to "Obwalden",
    "Schaffhausen" to "Schaffhausen",
    "Schwyz" to "Schwyz",
    "Solothurn" to "Solothurn",
    "StGallen" to "St. Gallen",
    "Tessin" to "Tessin",
    "Thurgau" to "Thurgau",
    "Uri" to "Uri",
    "Waadt" to "Waadt",
    "Wallis" to "Wallis",
    "Zug" to "Zug",
    "Zuerich" to "Zürich",
)

/** Der Staat eines Landes — ohne Land Deutschland, wie das erfundene Heidefeld. */
internal fun werkstaatVon(land: String?): String = when (land) {
    null, "" -> "Deutschland"
    in OESTERREICH -> "Oesterreich"
    in SCHWEIZ -> "Schweiz"
    else -> "Deutschland"
}

/** Der lesbare Name eines Landes, auch jenseits der Grenze. */
internal fun werklandname(land: String): String =
    OESTERREICH[land] ?: SCHWEIZ[land] ?: bundeslandname(land)

/**
 * Ob es eine Fahrzeugvorlage in diesem Staat gibt — in Tirol kein HLF 20, in
 * Celle kein RLF-A. Ein Server, der `staaten` nicht schickt, kennt nur Deutschland.
 */
internal fun Fahrzeugvorlage.imStaat(staat: String): Boolean =
    (staaten ?: listOf("Deutschland")).contains(staat)

/**
 * Kreise für eine Wahlblende, gebündelt nach Staat und Land: Deutschland,
 * Österreich, Schweiz; darin die Länder nach ihrem geschriebenen Namen, darin
 * die Kreise alphabetisch.
 */
internal fun werkKreisgruppen(kreise: List<Landkreis>): List<Pair<String?, List<Landkreis>>> =
    kreise.groupBy { it.bundesland }.toList()
        .sortedWith(
            compareBy<Pair<String, List<Landkreis>>> { WERK_STAATEN.indexOf(werkstaatVon(it.first)) }
                .thenBy(DEUTSCH) { werklandname(it.first) },
        )
        .map { (land, liste) ->
            "${WERK_FLAGGE[werkstaatVon(land)]} ${werklandname(land)}" to liste.sortedWith(compareBy(DEUTSCH) { it.name })
        }

/** Deutsch sortiert — „Österreich" nach „Oberösterreich", nicht hinter „Zürich". */
private val DEUTSCH: java.text.Collator = java.text.Collator.getInstance(java.util.Locale.GERMAN)
