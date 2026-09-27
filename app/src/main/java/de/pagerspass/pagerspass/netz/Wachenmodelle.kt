package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Modelle der Wachengemeinschaft — übertragen aus `web/src/types.ts`.
 *
 * <b>Eine eigene Datei neben `Spielmodelle.kt`</b>, weil die Wache mit Laufbahn,
 * Rangliste, Logbuch und Kasse inzwischen mehr Modelle hat als jeder andere
 * Bereich. Was schon in `Spielmodelle.kt` stand (Gemeinschaft, Mitglied, Antrag,
 * Termin), bleibt dort; hier steht, was erst mit dem vollen Wachenbereich kam.
 *
 * Dieselben Regeln wie dort: alles mit Vorgabe, damit ein fehlendes Feld die
 * Seite nicht umwirft, sondern nur weniger zeigt.
 */

/** Ein Mitglied in der internen Rangliste — „Wer fährt am meisten". */
@Serializable
data class Wachenbeitrag(
    val kennung: String = "",
    val anzeigename: String = "",
    val punkte: Int = 0,
    val schichten: Int = 0,
    val einsaetze: Int = 0,
    val letzteSchicht: String? = null,
    val premium: Boolean = false,
    val teammitglied: Boolean = false,
    /** Der eindeutige Benutzername — damit die Zeile ins Profil führt. */
    val benutzername: String? = null,
)

/** Eine Wache in der Rangliste. `platz` ist 0, wenn sie sich herausgenommen hat. */
@Serializable
data class Wachenplatz(
    val platz: Int = 0,
    val id: String = "",
    val name: String = "",
    val landkreis: String? = null,
    val mitglieder: Int = 0,
    val erfahrung: Int = 0,
    val stufe: Int = 0,
    val bezeichnung: String = "",
    val aktivitaetPunkte: Int = 0,
    val istEigene: Boolean = false,
)

/**
 * Eine Stufe der Wachen-Laufbahn samt dem, was sie erlaubt — fürs Stufen-Band.
 *
 * <b>Kommt vom Server und nicht aus einer Tabelle hier</b>: Die Regeln wohnen in
 * `Wachenstufe.cs`, und eine zweite Abschrift wäre beim ersten Nachbessern eine
 * zweite Wahrheit.
 */
@Serializable
data class Wachenrang(
    val stufe: Int = 0,
    /** Erfahrung, ab der die Stufe gilt — die Schwelle. */
    val ab: Int = 0,
    val bezeichnung: String = "",
    val maxMitglieder: Int = 0,
    val clanrundenSperreMinuten: Int = 0,
    val maxTermine: Int = 0,
    val clanrundenCoins: Int = 0,
    /** Was der Aufstieg auf diese Stufe der Kasse einbringt. */
    val coins: Int = 0,
    /** Zierstücke, die genau mit dieser Stufe dazukommen — meist leer. */
    val belohnungen: List<String> = emptyList(),
)

/** Ein Eintrag im Wachen-Logbuch: eine gefahrene Clanrunde. */
@Serializable
data class Wachenrunde(
    val nr: Long = 0,
    val roomCode: String = "",
    val landkreis: String? = null,
    val vonName: String? = null,
    val gestartetUm: String = "",
    /** `null`, solange die Runde noch läuft. */
    val beendetUm: String? = null,
    val teilnehmer: Int = 0,
    val einsaetze: Int = 0,
    val punkte: Int = 0,
)

/**
 * Ein Artikel der Auslage — Ausbau oder Zierstück, samt Kauf- und Wunschstand.
 *
 * `art` ist einer von `Mitgliederplaetze`, `Terminplaetze`, `Kopfmuster`,
 * `Emblemrahmen`, `Emblemzeichen`, `Wachenfarbe`, `Beiname`.
 */
@Serializable
data class Wachenartikel(
    val id: String = "",
    val art: String = "",
    /** Bei Zierstücken die Stück-Id fürs Anzeigen; beim Ausbau `null`. */
    val stueckId: String? = null,
    val name: String = "",
    /** Was er diese Woche kostet — beim Stück der Woche weniger als `listenpreis`. */
    val preis: Int = 0,
    /** Höchstzahl der Käufe; `null`, wenn allein die harte Grenze deckelt. */
    val maxKaeufe: Int? = null,
    val gekauft: Int = 0,
    val wuensche: Int = 0,
    val vonMirGewuenscht: Boolean = false,
    val listenpreis: Int = 0,
    val angebot: Boolean = false,
) {
    /** Ob das Stück nur schmückt — dieselbe Trennung wie am Server. */
    val istZierde: Boolean get() = art != "Mitgliederplaetze" && art != "Terminplaetze"

    val ausverkauft: Boolean get() = maxKaeufe != null && gekauft >= maxKaeufe
}

/**
 * Ein Zierstück der Wache samt seinem Stand — der ganze Katalog für den
 * Anpassen-Dialog, nicht nur die Auslage der Woche.
 */
@Serializable
data class Wachenstueck(
    val art: String = "",
    val stueckId: String = "",
    val name: String = "",
    /** Preis in Coins; `null` bei den Stufenstücken, die in keinem Laden stehen. */
    val preis: Int? = null,
    /** Die Stufe, ab der es freisteht — 1 bei allem, was man kauft. */
    val abStufe: Int = 1,
    /** Ob die Wache es tragen darf: gekauft, oder erspielt. */
    val frei: Boolean = false,
    /** Ob es gerade in der Auslage steht. */
    val imSortiment: Boolean = false,
)

/** Eine Zeile des Coin-Auszugs; `vonName` nur, wenn ein Konto gehandelt hat. */
@Serializable
data class Coinposten(
    /** `Wachenstufe`, `Clanrunde`, `Einzahlung`, `Kauf`, `Gluecksrad`. */
    val grund: String = "",
    val text: String = "",
    val betrag: Int = 0,
    val vonName: String? = null,
    val um: String = "",
)

/**
 * Kasse und Laden der Wache. `darfKaufen` steuert nur die Anzeige — entschieden
 * wird am Server.
 *
 * `artikel` ist die <b>Auslage dieser Woche</b> (Ausbau plus zehn Zierstücke),
 * `schmuck` der ganze Zierkatalog für den Anpassen-Dialog.
 */
@Serializable
data class Wachenschatz(
    val coins: Int = 0,
    val darfKaufen: Boolean = false,
    val artikel: List<Wachenartikel> = emptyList(),
    val auszug: List<Coinposten> = emptyList(),
    /** Wann das Sortiment das nächste Mal wechselt (ISO). */
    val wechseltUm: String = "",
    val angebotArtikelId: String? = null,
    val angebotRabattProzent: Int = 0,
    val schmuck: List<Wachenstueck> = emptyList(),
)

/** Der Landkreis, in dem dieses Konto ohnehin fährt — Vorbelegung beim Gründen. */
@Serializable
data class Landkreisvorschlag(
    val landkreisId: String? = null,
    val landkreis: String? = null,
)

/**
 * Ein Treffer der Suche über den Benutzernamen — für die Einladung in die Wache.
 *
 * <b>Bewusst nur die Felder, die der Einladen-Dialog liest.</b> Der volle
 * `Suchtreffer` gehört dem Freundesbereich; dieser eigene Name hält die beiden
 * Bereiche beim Zusammenführen auseinander.
 */
@Serializable
data class Wachensuchtreffer(
    val kennung: String = "",
    val benutzername: String = "",
    val anzeigename: String = "",
    val level: Int? = null,
    val rang: String? = null,
    val stand: String? = null,
    val profilbild: String? = null,
    val premium: Boolean = false,
    val teammitglied: Boolean = false,
    val wachentag: String? = null,
)

/**
 * Alle Stellschrauben der Leitung auf einmal — der Rumpf von
 * `PUT /api/gemeinschaften/{kennung}/{id}`.
 *
 * <b>Ausdrücklich mit `null` gesendet</b>, nicht weggelassen: Ein Teilbogen wäre
 * am Server nicht von einem bewusst geleerten Feld zu unterscheiden. Deshalb baut
 * `Wachenwege` den Rumpf von Hand und nicht über `Netz.abgabe`, das `null` weglässt.
 */
data class Gemeinschaftseinstellungen(
    val name: String,
    val beschreibung: String?,
    val landkreisId: String?,
    val landkreis: String?,
    val wappenfarbe: Int,
    val oeffentlich: Boolean,
    val inRangliste: Boolean,
    /** `Einladung`, `Antrag` oder `Offen`. */
    val beitrittModus: String,
    val mindestLevel: Int,
    val mindestErfahrung: Int,
    val maxMitglieder: Int,
)

/** Womit sich die öffentliche Liste eingrenzen lässt — alles auslassbar. */
data class Gemeinschaftsfilter(
    val suche: String = "",
    val landkreisId: String? = null,
    /** `Einladung`, `Antrag`, `Offen` — oder `null`: egal. */
    val modus: String? = null,
    /** Volle Wachen ausblenden. */
    val nurFreie: Boolean = false,
    /**
     * Nur Wachen, deren Aufnahmeregeln das eigene Konto erfüllt — voreingestellt
     * an, wie im Web: Ohne es besteht die Liste für ein junges Konto zur Hälfte
     * aus Wachen, die es nicht aufnehmen.
     */
    val nurPassende: Boolean = true,
) {
    /** Ob gerade überhaupt etwas eingegrenzt ist — nur dann lohnt „Zurücksetzen". */
    val eingegrenzt: Boolean
        get() = suche.isNotBlank() || landkreisId != null || modus != null || nurFreie || !nurPassende
}

/** Die Regeln, die der Client spiegelt — Quelle ist `Gemeinschaftsregeln` am Server. */
object Wachenregeln {
    /** Ab welcher Wachenstufe eine Wache ihren Tag trägt. */
    const val TAG_AB_STUFE = 40

    const val TAG_MINDESTENS = 2
    const val TAG_HOECHSTENS = 5
}
