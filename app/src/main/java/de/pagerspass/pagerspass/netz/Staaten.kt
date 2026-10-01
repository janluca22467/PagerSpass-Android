package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die drei Staaten über den Ländern — Deutschland, Österreich, Schweiz (v6).
 *
 * Übertragen aus `web/src/types.ts` (`STAATEN`, `STAAT_VON`, `BUNDESLAND_LABEL`)
 * und dem `staaten`-Feld des Katalogs. Wer in Tirol besetzt, besetzt eine andere
 * Leitstelle: andere Notrufnummern, andere Fahrzeuge, andere Rufnamen, „Bezirk"
 * statt „Landkreis". Ein älterer Server kennt nur Deutschland — dann liefert der
 * Katalog keine österreichischen und schweizerischen Kreise, und der Schalter
 * bleibt weg.
 */
object Staaten {
    const val DEUTSCHLAND = "Deutschland"
    const val OESTERREICH = "Oesterreich"
    const val SCHWEIZ = "Schweiz"

    val ALLE = listOf(DEUTSCHLAND, OESTERREICH, SCHWEIZ)

    /** Flagge und Kürzel — drei ausgeschriebene Namen passen am Handy nicht nebeneinander. */
    fun kurz(staat: String): String = when (staat) {
        OESTERREICH -> "🇦🇹 AT"
        SCHWEIZ -> "🇨🇭 CH"
        else -> "🇩🇪 DE"
    }

    fun name(staat: String): String = when (staat) {
        OESTERREICH -> "Österreich"
        SCHWEIZ -> "Schweiz"
        else -> "Deutschland"
    }

    private val IN_OESTERREICH = setOf(
        "Burgenland", "Kaernten", "Niederoesterreich", "Oberoesterreich", "Salzburg",
        "Steiermark", "Tirol", "Vorarlberg", "Wien",
    )

    private val IN_DER_SCHWEIZ = setOf(
        "Aargau", "AppenzellAusserrhoden", "AppenzellInnerrhoden", "BaselLandschaft", "BaselStadt",
        "Bern", "Freiburg", "Genf", "Glarus", "Graubuenden", "Jura", "Luzern", "Neuenburg",
        "Nidwalden", "Obwalden", "Schaffhausen", "Schwyz", "Solothurn", "StGallen", "Tessin",
        "Thurgau", "Uri", "Waadt", "Wallis", "Zug", "Zuerich",
    )

    /** Der Staat eines Landes — dieselbe Zuordnung wie `Laender.StaatVon` im Server. */
    fun von(land: String?): String = when (land) {
        null -> DEUTSCHLAND
        in IN_OESTERREICH -> OESTERREICH
        in IN_DER_SCHWEIZ -> SCHWEIZ
        else -> DEUTSCHLAND
    }

    /**
     * Wo ein Staat beim ersten Umschalten anfängt: Celle wie immer, in Österreich
     * Innsbruck-Land, in der Schweiz Zürich.
     */
    fun vorgabe(staat: String): Pair<String, String> = when (staat) {
        OESTERREICH -> "Tirol" to "at-innsbruck-land"
        SCHWEIZ -> "Zuerich" to "ch-zuerich"
        else -> "Niedersachsen" to "celle"
    }

    /** Die lesbaren Namen der österreichischen und schweizerischen Länder. */
    val LAENDER: Map<String, String> = mapOf(
        "Burgenland" to "Burgenland",
        "Kaernten" to "Kärnten",
        "Niederoesterreich" to "Niederösterreich",
        "Oberoesterreich" to "Oberösterreich",
        "Salzburg" to "Salzburg",
        "Steiermark" to "Steiermark",
        "Tirol" to "Tirol",
        "Vorarlberg" to "Vorarlberg",
        "Wien" to "Wien",
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
}

/**
 * Was ein Staat anders macht — so, wie der Server es im Katalog mitschickt
 * (`StaatDto`). Hier braucht die App nur die beiden Wörter für Land und Kreis.
 */
@Serializable
data class Staatsprofil(
    val staat: String = Staaten.DEUTSCHLAND,
    val name: String = "",
    val kuerzel: String = "",
    /** „Bundesland" oder „Kanton". */
    val landwort: String = "Bundesland",
    /** „Landkreis", „Bezirk" oder „Kanton". */
    val kreiswort: String = "Landkreis",
)
