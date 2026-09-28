package de.pagerspass.pagerspass.ansichten

import androidx.compose.ui.graphics.Color
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.ui.theme.Farben

/**
 * Die Wörter der Runde — übertragen aus den Tafeln in `web/src/types.ts`.
 *
 * <b>Eine Stelle für alle Ansichten.</b> Lobby, Zuschauerplatz, Leitstelle und
 * Nachbesprechung nennen dieselben Modi, Organisationen und Stufen; stünden die
 * Tafeln je Ansicht, liefen sie beim nächsten Umbenennen auseinander.
 *
 * <b>Ein unbekannter Wert bleibt, wie er ist</b> — ein sechster Modus des
 * Servers stünde roh da, aber er stünde da.
 */
object Rundentexte {

    /** `MODUS_LABEL` — wie der Spielmodus heißt, wo er nur benannt wird. */
    fun modus(roh: String): String = when (roh) {
        "Zufall" -> "Zufallseinsätze"
        "Frei" -> "Freie Vergabe"
        "Ausbildung" -> "Ausbildungsschicht"
        "Tagesschicht" -> "Schicht des Tages"
        "Szenario" -> "Übung"
        else -> roh
    }

    /** Die vier Organisationen in der Reihenfolge des Servers. */
    val ORGANISATIONEN = listOf("Feuerwehr", "Rettungsdienst", "Thw", "Polizei")

    fun organisation(roh: String): String = when (roh) {
        "Thw" -> "THW"
        else -> roh
    }

    /** Die Kurzmarke einer Organisation — `FW`, `RD`, `THW`, `POL`. */
    fun organisationKurz(roh: String): String = when (roh) {
        "Feuerwehr" -> "FW"
        "Rettungsdienst" -> "RD"
        "Thw" -> "THW"
        "Polizei" -> "POL"
        else -> roh
    }

    fun organisationFarbe(roh: String): Color = when (roh) {
        "Feuerwehr" -> Farben.OrgFeuerwehr
        "Rettungsdienst" -> Farben.OrgRettungsdienst
        "Thw" -> Farben.OrgThw
        "Polizei" -> Farben.OrgPolizei
        else -> Farben.TextLeise
    }

    /** Alle wählbaren Träger — „Keine" ist kein Träger, sondern deren Abwesenheit. */
    val HIORGS = listOf(
        "Drk", "Juh", "Mhd", "Asb", "Dlrg", "Wasserwacht",
        "Bergwacht", "Dgzrs", "Brh", "Werkfeuerwehr", "Privat",
    )

    /** `HIORG_LABEL` — der Kurzname des Trägers. */
    fun traeger(roh: String): String = when (roh) {
        "Keine" -> ""
        "Drk" -> "DRK"
        "Juh" -> "Johanniter"
        "Mhd" -> "Malteser"
        "Asb" -> "ASB"
        "Dlrg" -> "DLRG"
        "Bergwacht" -> "Bergwacht"
        "Wasserwacht" -> "Wasserwacht"
        "Dgzrs" -> "Seenotretter"
        "Brh" -> "Rettungshunde"
        "Werkfeuerwehr" -> "Werkfeuerwehr"
        "Privat" -> "Privater RD"
        else -> roh
    }

    /** `HIORG_LANG` — der volle Name für Erklärtexte. */
    fun traegerLang(roh: String): String = when (roh) {
        "Drk" -> "Deutsches Rotes Kreuz"
        "Juh" -> "Johanniter-Unfall-Hilfe"
        "Mhd" -> "Malteser Hilfsdienst"
        "Asb" -> "Arbeiter-Samariter-Bund"
        "Dlrg" -> "Deutsche Lebens-Rettungs-Gesellschaft"
        "Dgzrs" -> "Deutsche Gesellschaft zur Rettung Schiffbrüchiger"
        "Brh" -> "Bundesverband Rettungshunde"
        "Privat" -> "Privater Rettungsdienst"
        else -> traeger(roh)
    }

    fun traegerFarbe(roh: String): Color = when (roh) {
        "Drk" -> Farben.HiorgDrk
        "Juh" -> Farben.HiorgJuh
        "Mhd" -> Farben.HiorgMhd
        "Asb" -> Farben.HiorgAsb
        "Dlrg" -> Farben.HiorgDlrg
        "Bergwacht" -> Farben.HiorgBergwacht
        "Wasserwacht" -> Farben.HiorgWasserwacht
        "Dgzrs" -> Farben.HiorgDgzrs
        "Brh" -> Farben.HiorgBrh
        "Werkfeuerwehr" -> Farben.HiorgWerkfeuerwehr
        "Privat" -> Farben.HiorgPrivat
        else -> Color.Transparent
    }

    /** Das Rufname-Wort der BOS-Systematik — `PRAEFIX_ORG` und `PRAEFIX_HIORG`. */
    fun rufwort(schluessel: String): String = when (schluessel) {
        "Feuerwehr" -> "Florian"
        "Rettungsdienst" -> "Rotkreuz"
        "Thw" -> "Heros"
        "Polizei" -> "Peter"
        "Drk" -> "Rotkreuz"
        "Juh" -> "Akkon"
        "Mhd" -> "Johannes"
        "Asb" -> "Sama"
        "Dlrg" -> "Pelikan"
        "Bergwacht" -> "Bergwacht"
        "Wasserwacht" -> "Wasserwacht"
        "Dgzrs" -> "Seenot"
        "Brh" -> "Rettungshund"
        "Werkfeuerwehr" -> "Florian"
        "Privat" -> "Rettung"
        else -> "Einheit"
    }

    /** Ob ein Schlüssel eine Organisation ist und kein Träger. */
    fun istOrganisation(schluessel: String): Boolean = schluessel in ORGANISATIONEN

    val BOT_TEMPO = listOf("Gemuetlich" to "Gemütlich", "Normal" to "Normal", "Zuegig" to "Zügig")

    val EINSATZDICHTE = listOf("Ruhig" to "Ruhig", "Normal" to "Normal", "Dicht" to "Dicht")

    fun einsatzdichteHinweis(roh: String): String = when (roh) {
        "Ruhig" -> "Alle drei Minuten eine Lage, höchstens zwei offen. Für die erste Schicht allein."
        "Dicht" -> "Alle anderthalb Minuten eine Lage, höchstens vier offen. Für die Leitstelle zu zweit."
        else -> "Alle zwei Minuten eine Lage, höchstens drei offen. Allein und geübt."
    }

    val GESPRAECHIGKEIT = listOf("Knapp" to "Knapp", "Normal" to "Normal", "Gespraechig" to "Gesprächig")

    fun gespraechigkeitHinweis(roh: String): String = when (roh) {
        "Knapp" -> "Nur Quittungen und Pflichtmeldungen."
        "Gespraechig" -> "Bots melden sich auch von sich aus und fragen nach."
        else -> "Gelegentliche Eigenmeldungen, Rückfragen wie immer."
    }

    val ARBEITSFUNK = listOf(
        "NurEinsatzleitung" to "Nur die Einsatzleitung",
        "AlleBesatzungen" to "Alle Besatzungen",
    )

    fun arbeitsfunkHinweis(roh: String): String = when (roh) {
        "AlleBesatzungen" ->
            "Jede Besatzung meldet sich von sich aus — Lage auf Sicht und jede fertige Aufgabe. " +
                "Immer angemeldet über den Sprechwunsch."
        else ->
            "Nur die führende Einheit meldet sich von sich aus — Lage, Nachforderung, Abschluss. " +
                "Alle anderen arbeiten still; ihre Aufgaben stehen in der Chronologie. Angesprochen " +
                "antwortet natürlich jede Besatzung."
    }

    val EINSATZENDE = listOf("Selbsttaetig" to "Selbsttätig", "NachFreigabe" to "Nach Freigabe")

    fun einsatzendeHinweis(roh: String): String = when (roh) {
        "NachFreigabe" -> "Die Kräfte bleiben, bis du die Abschlussmeldung quittierst oder abschließt."
        else -> "Auf die Abschlussmeldung hin rücken alle Kräfte ein, der Einsatz schließt sich."
    }

    val STOERUNG = listOf("Aus" to "Aus", "Selten" to "Selten", "Gelegentlich" to "Gelegentlich")

    fun stoerungHinweis(roh: String): String = when (roh) {
        "Selten" -> "Etwa einmal je Stunde kommt etwas dazwischen."
        "Gelegentlich" -> "Mehrmals je Stunde — für Schichten, die fordern sollen."
        else -> "Jede Alarmierung ist echt, jedes Fahrzeug fährt durch."
    }

    fun stoerung(roh: String): String = STOERUNG.firstOrNull { it.first == roh }?.second ?: roh

    val JAHRESZEITEN = listOf(
        "Fruehling" to "Frühling",
        "Sommer" to "Sommer",
        "Herbst" to "Herbst",
        "Winter" to "Winter",
    )

    fun jahreszeit(roh: String): String = JAHRESZEITEN.firstOrNull { it.first == roh }?.second ?: roh

    fun einsatzdichte(roh: String): String = EINSATZDICHTE.firstOrNull { it.first == roh }?.second ?: roh

    fun wetter(roh: String): String = when (roh) {
        "Glaette" -> "Glätte"
        else -> roh
    }

    /** `RUNDE_STATUS_LABEL` — wie der Rundenstatus heißt. */
    fun rundenstatus(roh: String): String = when (roh) {
        "Lobby" -> "Wartet auf Dienstbeginn"
        "Laeuft" -> "Im Dienst"
        "Beendet" -> "Beendet"
        else -> roh
    }

    /** Die Spielhilfe im Wiki — eine Stelle je Ansicht. */
    const val WIKI = "https://wiki.pagerspass.de"
}

// ------------------------------------------------------------ Fahrzeugwahl

/**
 * Eine Gruppe der Fahrzeugwahl — „Feuerwehr · Löschfahrzeuge", darunter die
 * Träger, wo es mehrere gibt. Übertragen aus `composables/fahrzeugGruppen.ts`.
 */
data class Fahrzeuggruppe(
    val schluessel: String,
    val organisation: String,
    val traeger: List<Traegergruppe>,
)

data class Traegergruppe(
    val hiOrg: String,
    val aufschrift: String,
    val fahrzeuge: List<Fahrzeugvorlage>,
)

/** Die Reihenfolge der Kategorien je Organisation — `KATEGORIEFOLGE` im Web. */
private val KATEGORIEFOLGE: Map<String, List<String>> = mapOf(
    "Feuerwehr" to listOf(
        "Löschfahrzeuge", "Tanklöschfahrzeuge", "Hubrettung", "Rüst- und Technikzug", "Führung",
        "Logistik und Mannschaft", "Gefahrgut und Messtechnik", "Wasserversorgung", "Vegetationsbrand",
        "Wasserrettung", "Anhänger und Boote", "Abrollbehälter",
    ),
    "Rettungsdienst" to listOf(
        "Rettungsmittel", "Notarztzubringer", "Krankentransport", "Führung",
        "Massenanfall und Betreuung", "Wasserrettung", "Seenotrettung", "Bergrettung", "Rettungshunde",
    ),
    "Thw" to listOf("Bergung", "Fachgruppen", "Führung und Mannschaft"),
    "Polizei" to listOf(
        "Streifendienst", "Bundespolizei", "Einsatzeinheiten", "Kriminaldienst", "Spezialkräfte",
        "Luftunterstützung",
    ),
)

/** `gruppiereFahrzeuge` — nach Organisation und Kategorie, darin nach Träger. */
fun gruppiereFahrzeuge(liste: List<Fahrzeugvorlage>): List<Fahrzeuggruppe> {
    val nach = linkedMapOf<String, MutableList<Fahrzeugvorlage>>()
    liste.forEach { f ->
        val schluessel = "${Rundentexte.organisation(f.organisation)} · ${f.kategorie}"
        nach.getOrPut(schluessel) { mutableListOf() }.add(f)
    }
    fun rang(f: Fahrzeugvorlage): Pair<Int, Int> {
        val org = Rundentexte.ORGANISATIONEN.indexOf(f.organisation).let {
            if (it < 0) Rundentexte.ORGANISATIONEN.size else it
        }
        val kat = KATEGORIEFOLGE[f.organisation]?.indexOf(f.kategorie)?.takeIf { it >= 0 } ?: Int.MAX_VALUE
        return org to kat
    }
    return nach.entries
        .sortedWith(
            compareBy<Map.Entry<String, MutableList<Fahrzeugvorlage>>>(
                { rang(it.value.first()).first },
                { rang(it.value.first()).second },
                { it.value.first().kategorie },
            ),
        )
        .map { (schluessel, fahrzeuge) ->
            Fahrzeuggruppe(
                schluessel = schluessel,
                organisation = fahrzeuge.first().organisation,
                traeger = gruppiereTraeger(fahrzeuge),
            )
        }
}

private fun gruppiereTraeger(fahrzeuge: List<Fahrzeugvorlage>): List<Traegergruppe> {
    val nach = linkedMapOf<String, MutableList<Fahrzeugvorlage>>()
    fahrzeuge.forEach { f -> nach.getOrPut(f.hiOrg.ifBlank { "Keine" }) { mutableListOf() }.add(f) }
    val alphabetisch = { l: List<Fahrzeugvorlage> -> l.sortedBy { it.typ.lowercase() } }
    if (nach.size <= 1) {
        return nach.map { (h, l) -> Traegergruppe(h, Rundentexte.traeger(h), alphabetisch(l)) }
    }
    return nach.entries
        .sortedBy { Rundentexte.traeger(it.key).ifBlank { "Sonstige" } }
        .map { (h, l) ->
            Traegergruppe(
                hiOrg = h,
                aufschrift = if (h == "Keine") "Öffentlicher Träger" else Rundentexte.traeger(h),
                fahrzeuge = alphabetisch(l),
            )
        }
}

/** `vorlagePasst` — gesucht wird über Typ, Beschreibung, Kategorie, Fähigkeit und Träger. */
fun vorlagePasst(f: Fahrzeugvorlage, begriff: String): Boolean {
    val t = begriff.trim().lowercase()
    if (t.isEmpty()) return true
    return f.typ.lowercase().contains(t) ||
        f.beschreibung.lowercase().contains(t) ||
        f.kategorie.lowercase().contains(t) ||
        f.faehigkeiten.any { it.lowercase().contains(t) } ||
        Rundentexte.traeger(f.hiOrg).lowercase().contains(t)
}

/** Eine Zahl mit Mindestbreite — „01" bei zwei Stellen. */
fun mitStellen(zahl: Int, stellen: Int): String =
    zahl.toString().padStart(stellen.coerceIn(1, 3), '0')
