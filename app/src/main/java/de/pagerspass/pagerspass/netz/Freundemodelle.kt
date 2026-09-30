package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Was der Freundebereich und die Wache über das hinaus brauchen, was schon in
 * `Spielmodelle.kt` steht — übertragen aus `web/src/types.ts`.
 *
 * <b>Eigene Datei mit Absicht.</b> Die Modelle hier gehören zu Wegen, die die App
 * bis eben gar nicht ging (Suche, Vorschläge, fremdes Profil, Wachen-Shop,
 * Rangliste). Sie neben die alten zu legen hieße, eine Datei mit neunhundert
 * Zeilen noch weiter wachsen zu lassen, an der gleichzeitig andere arbeiten.
 *
 * Wie überall gilt: Jedes Feld hat eine Vorgabe. Ein älterer Server, der ein Feld
 * noch nicht schickt, soll eine Zeile ohne diese Angabe ergeben — nicht eine
 * Seite, die „konnte nicht geladen werden" meldet.
 */

// ------------------------------------------------------------------ Freunde

/**
 * Ein Treffer der Suche über den eindeutigen Benutzernamen.
 *
 * `level` und `rang` fehlen bei einem privaten Konto: Gefunden werden darf es,
 * sein Spielstand bleibt unter Freunden. `stand` ist `null`, solange man sich
 * nicht kennt — nur dann gibt es „Anfragen".
 */
@Serializable
data class Suchtreffer(
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
 * Ein vorgeschlagenes Konto — schon mitgefahren, aber noch nicht befreundet.
 *
 * <b>Kein `Freund`.</b> Die Zeile trägt den Grund, warum sie dasteht
 * (`gemeinsameSchichten`, `zuletztZusammen`); ohne ihn ist ein Vorschlag eine
 * Behauptung, mit ihm eine Erinnerung.
 */
@Serializable
data class Vorschlag(
    val kennung: String = "",
    val benutzername: String = "",
    val anzeigename: String = "",
    val level: Int = 0,
    val rang: String = "",
    val zuletztZusammen: String = "",
    val gemeinsameSchichten: Int = 0,
    val profilbild: String? = null,
    val premium: Boolean = false,
    val teammitglied: Boolean = false,
    val wachentag: String? = null,
)

/** Was beim Öffnen eines Geschenks herauskam. */
@Serializable
data class Geschenkinhalt(
    val artikelId: String = "",
    val artikelName: String = "",
    val vonName: String = "",
)

/** Ein Abzeichen in der Vitrine eines Profils. */
@Serializable
data class VitrinenAbzeichen(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String = "",
    val kategorie: String = "",
)

/**
 * Ein Profil, so wie es ein anderer sieht.
 *
 * <b>Nicht dasselbe Modell wie das eigene `Profil`</b>, obwohl derselbe Weg es
 * liefert: Hier zählt, wie man zueinander steht (`stand`, `vonMir`) und was die
 * Vitrine zeigt — beides braucht die eigene Profilseite nicht. Was fehlt, fehlt
 * absichtlich: Der Server lässt weg, was die Privatsphäre des Gezeigten
 * ausschließt, und die Ansicht blendet nichts nachträglich aus.
 */
@Serializable
data class Fremdprofil(
    val kennung: String = "",
    val benutzername: String = "",
    val anzeigename: String = "",
    val wappen: String = "Keines",
    val wappenfarbe: Int = 0,
    val premium: Boolean = false,
    val vorstellung: String? = null,
    val level: Int? = null,
    val rang: String? = null,
    val vitrine: List<VitrinenAbzeichen> = emptyList(),
    val schichten: Int? = null,
    val einsaetze: Int? = null,
    val gemeinschaftId: String? = null,
    val gemeinschaft: String? = null,
    val anwesenheit: Anwesenheit? = null,
    val zuletztGesehen: String? = null,
    val dabeiSeit: String = "",
    /** `Angefragt`, `Bestaetigt`, `Abgelehnt`, `Blockiert` — oder `null`: fremd. */
    val stand: String? = null,
    /** Ob eine offene Anfrage von einem selbst ausgeht. */
    val vonMir: Boolean = false,
    val ich: Boolean = false,
    val teammitglied: Boolean = false,
    val wachentag: String? = null,
    val profilrahmen: String = "keiner",
    val kopfmuster: String = "keines",
    val titel: String? = null,
    val profilbild: String? = null,
) {
    val befreundet: Boolean get() = stand == "Bestaetigt"
}

/** Der Landkreis, in dem dieses Konto ohnehin fährt — füllt das Gründen vor. */
@Serializable
data class Landkreisvorschlag(
    val landkreisId: String? = null,
    val landkreis: String? = null,
)

// -------------------------------------------------------------------- Wache

/**
 * Eine Stufe der Wachen-Laufbahn samt dem, was sie erlaubt — das Band unter der
 * Wache, das Gegenstück zum Pass der Konto-Laufbahn.
 */
@Serializable
data class Wachenrang(
    val stufe: Int = 0,
    /** Erfahrung, ab der die Stufe gilt. */
    val ab: Int = 0,
    val bezeichnung: String = "",
    val maxMitglieder: Int = 0,
    val clanrundenSperreMinuten: Int = 0,
    val maxTermine: Int = 0,
    val clanrundenCoins: Int = 0,
    /** Was der Aufstieg auf diese Stufe der Kasse einbringt. */
    val coins: Int = 0,
    /** Zierstücke, die genau mit dieser Stufe dazukommen — meist keine. */
    val belohnungen: List<String> = emptyList(),
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

/** Ein Mitglied in der internen Rangliste — wer seine Statistik nicht teilt, fehlt. */
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
    /** Der Weg von der Zeile ins Profil. */
    val benutzername: String? = null,
)

/** Ein Eintrag im Logbuch der Wache: eine gefahrene Clanrunde. */
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
 * Ein Stück der Auslage im Wachen-Shop, samt Kauf- und Wunschstand.
 *
 * `art` ist einer von `Mitgliederplaetze`, `Terminplaetze` (Ausbau) oder
 * `Kopfmuster`, `Emblemrahmen`, `Emblemzeichen`, `Wachenfarbe`, `Beiname`
 * (Zierde).
 */
@Serializable
data class Wachenartikel(
    val id: String = "",
    val art: String = "",
    val stueckId: String? = null,
    val name: String = "",
    val preis: Int = 0,
    val maxKaeufe: Int? = null,
    val gekauft: Int = 0,
    val wuensche: Int = 0,
    val vonMirGewuenscht: Boolean = false,
    val listenpreis: Int = 0,
    val angebot: Boolean = false,
) {
    /** Ob das Stück nur schmückt — dieselbe Trennung wie am Server. */
    val zierde: Boolean get() = art != "Mitgliederplaetze" && art != "Terminplaetze"

    val ausverkauft: Boolean get() = maxKaeufe != null && gekauft >= maxKaeufe
}

/**
 * Ein Zierstück der Wache samt Stand — für „Wache anpassen", nicht für die
 * Auslage. Die Liste ist vollständig: Was es gäbe, zieht mehr als was man hat.
 */
@Serializable
data class Wachenstueck(
    val art: String = "",
    val stueckId: String = "",
    val name: String = "",
    /** Preis in Coins; `null` bei den Stufenstücken, die in keinem Laden stehen. */
    val preis: Int? = null,
    val abStufe: Int = 1,
    /** Ob die Wache es tragen darf: gekauft oder erspielt. */
    val frei: Boolean = false,
    val imSortiment: Boolean = false,
)

/** Eine Zeile des Coin-Auszugs. */
@Serializable
data class Coinposten(
    val grund: String = "",
    val text: String = "",
    val betrag: Int = 0,
    val vonName: String? = null,
    val um: String = "",
)

/**
 * Kasse und Laden der Wache — nur für Mitglieder, von außen `null`.
 *
 * `artikel` ist die Auslage dieser Woche, nicht der ganze Katalog; was es sonst
 * noch gibt, steht in `schmuck` und gehört in „Wache anpassen".
 */
@Serializable
data class Wachenschatz(
    val coins: Int = 0,
    val darfKaufen: Boolean = false,
    val artikel: List<Wachenartikel> = emptyList(),
    val auszug: List<Coinposten> = emptyList(),
    val wechseltUm: String? = null,
    val angebotArtikelId: String? = null,
    val angebotRabattProzent: Int = 0,
    val schmuck: List<Wachenstueck> = emptyList(),
)

/** Ein Punkt der Hilfsfrist-Kurve eines Mitglieds. */
@Serializable
data class HilfsfristPunkt(
    val beendetUm: String = "",
    val hilfsfristSekunden: Double? = null,
)

/** Die Hilfsfrist-Kurve eines Mitglieds, für den Vergleich. */
@Serializable
data class GemeinschaftsHilfsfrist(
    val anzeigename: String = "",
    val verlauf: List<HilfsfristPunkt> = emptyList(),
)

/**
 * Die Stellschrauben der Wache — alle auf einmal, wie im Formular.
 *
 * <b>Bewusst kein Teilbogen</b>: Eine ausgelassene Angabe wäre am Server nicht
 * von einer bewusst geleerten zu unterscheiden. Deshalb stehen die Felder hier
 * ohne Vorgabe `null` — wer den Bogen baut, muss jedes davon füllen.
 */
@Serializable
data class Wacheneinstellungen(
    val name: String,
    val beschreibung: String?,
    val landkreisId: String?,
    val landkreis: String?,
    val oeffentlich: Boolean,
    val inRangliste: Boolean,
    val beitrittModus: String,
    val mindestLevel: Int,
    val mindestErfahrung: Int,
    val maxMitglieder: Int,
)
