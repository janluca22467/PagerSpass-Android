package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Modelle der Ansichten — übertragen aus `web/src/types.ts`.
 *
 * <b>Nur, was die App auch zeigt.</b> `types.ts` hat über 5.000 Zeilen; hier
 * stehen die Felder, die eine der sechs Seiten liest. Ein Feld, das niemand
 * anzeigt, ist ein Feld, das nie gegen einen echten Rumpf geprüft wird — und
 * damit eine Zusage, die man nicht halten kann.
 *
 * <b>Alles mit Vorgabe.</b> `ignoreUnknownKeys` fängt neue Felder des Servers
 * ab; Vorgaben fangen fehlende ab. Beides zusammen heißt: Eine App, die einen
 * Tag älter ist als der Server, stürzt nicht ab, sondern zeigt weniger.
 */

// ------------------------------------------------------------------ Dienstbuch

/** Eine gefahrene Schicht — eine Zeile im Dienstbuch. */
@Serializable
data class Schicht(
    val roomCode: String = "",
    val beendetUm: String = "",
    val ort: String = "",
    val landkreis: String? = null,
    val rolle: String = "",
    val funkrufname: String? = null,
    val fahrzeugtyp: String? = null,
    val einsaetze: Int = 0,
    val hilfsfristSekunden: Int? = null,
    val punkte: Int = 0,
)

/**
 * Ein Meilenstein.
 *
 * `erreicht` und `erreichtAm` sind getrennt, weil ein Abzeichen erreicht sein
 * kann, ohne dass der Zeitpunkt bekannt ist — bei allem, was vor der Einführung
 * der Zeitstempel geschafft wurde.
 */
@Serializable
data class Abzeichen(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String = "",
    val kategorie: String = "",
    val erreicht: Boolean = false,
    val erreichtAm: String? = null,
)

/** Ein Platz auf der Bestenliste. */
@Serializable
data class Bestenlistenplatz(
    val platz: Int = 0,
    val anzeigename: String = "",
    val erfahrung: Int = 0,
    val level: Int = 0,
    val rang: String = "",
    val istEigenerEintrag: Boolean = false,
)

// ---------------------------------------------------------------------- Garage

/**
 * Der Fuhrpark eines Kontos.
 *
 * <b>`offeneWahlen` ist die Zahl an der Tableiste.</b> Sie zählt, wie viele
 * Fahrzeuge sich das Konto gerade aussuchen darf — eingelöst wird im Shop, und
 * deshalb hängt die Marke dort und nicht an der Garage.
 */
@Serializable
data class Garage(
    val fahrzeuge: List<String> = emptyList(),
    val offeneWahlen: Int = 0,
    val bisZurNaechstenWahl: Int? = null,
    val credits: Int = 0,
    val preise: Map<String, Int> = emptyMap(),
    val tagesangebot: String? = null,
    val tagesangebotRegulaer: Int? = null,
)

/** Ein Fahrzeugbauplan aus dem Katalog. */
@Serializable
data class Fahrzeugvorlage(
    val id: String = "",
    val organisation: String = "",
    val hiOrg: String = "",
    val typ: String = "",
    val beschreibung: String = "",
    val besatzung: String = "",
    val kategorie: String = "",
    val kennzahl: String = "",
)

// ------------------------------------------------------------------------ Shop

@Serializable
data class Shop(
    val credits: Int = 0,
    val sortiment: List<Shopartikel> = emptyList(),
    val naechsterWechsel: String = "",
    val imBesitz: List<Shopartikel> = emptyList(),
    val tagesbonus: Tagesbonus = Tagesbonus(),
    /** Das Fahrzeug des Tages — ein Viertel billiger, nur heute. */
    val tagesangebot: Tagesangebot? = null,
    /** Die drei Dienstaufträge der Woche (Modelle in `Kontomodelle.kt`). */
    val auftraege: List<Auftrag> = emptyList(),
    val zulagen: Zulagen = Zulagen(),
    /** Der Credit-Auszug — Gutschrift positiv, Kauf negativ. */
    val auszug: List<Creditposten> = emptyList(),
)

@Serializable
data class Shopartikel(
    val id: String = "",
    val art: String = "",
    val stueckId: String = "",
    val name: String = "",
    val preis: Int = 0,
    val gekauft: Boolean = false,
)

/**
 * Der Tagesbonus.
 *
 * `serie` ist die Zahl der Tage hintereinander — sie ist der eigentliche Anreiz
 * und gehört deshalb neben den Knopf, nicht in eine Nebenzeile.
 */
@Serializable
data class Tagesbonus(
    val verfuegbar: Boolean = false,
    val serie: Int = 0,
    /** Die Felder des Glücksrads samt ihrer Chance in Prozent. */
    val felder: List<Gluecksradfeld> = emptyList(),
)

// --------------------------------------------------------------------- Freunde

@Serializable
data class Freund(
    val kennung: String = "",
    val benutzername: String = "",
    val anzeigename: String = "",
    val level: Int = 0,
    val rang: String = "",
    /**
     * Der Stand der Beziehung.
     *
     * <b>Die vier Werte heißen `Angefragt`, `Bestaetigt`, `Abgelehnt`,
     * `Blockiert`</b> — nicht „Offen"/„Angenommen", wie man beim Bauen der
     * Reiter vermuten würde. Wer hier rät, bekommt zwei leere Listen und keinen
     * Fehler; die Namen stehen in `types.ts` als `Freundschaftsstand`.
     */
    val stand: String = "",
    /** Bei offener Anfrage: wartet man selbst, oder ist man am Zug? */
    val vonMir: Boolean = false,
    val ungelesen: Int = 0,
    /**
     * Wo jemand gerade steckt — <b>ein Objekt, kein Wort.</b>
     *
     * Genau hieran ist die Freundesliste einmal gescheitert: Als `String?`
     * angelegt, warf das Einlesen bei jedem Freund, der gerade in einer Runde
     * saß, und die ganze Seite meldete „konnte nicht geladen werden". `null`
     * heißt: in keiner Runde — „online" bedeutet hier nicht „hat die Seite
     * offen", sondern „sitzt auf der Wache".
     */
    val anwesenheit: Anwesenheit? = null,
    val zuletztGesehen: String? = null,
    // Der Schmuck. Er kommt an der Freundesliste mit, damit jede Zeile so
    // aussieht wie das Profil dahinter — dieselben Felder wie im Web.
    val profilbild: String? = null,
    val premium: Boolean = false,
    val teammitglied: Boolean = false,
    val wappen: String = "Keines",
    val wappenfarbe: Int = 0,
    val profilrahmen: String = "keiner",
    val kopfmuster: String = "keines",
) {
    /** Steht diese Freundschaft, oder ist sie noch eine Anfrage? */
    val bestaetigt: Boolean get() = stand.equals("Bestaetigt", true)

    /** Eine Anfrage, die noch offen ist. */
    val angefragt: Boolean get() = stand.equals("Angefragt", true)
}

/** Wo jemand gerade fährt. */
@Serializable
data class Anwesenheit(
    val roomCode: String = "",
    val ort: String = "",
    val landkreis: String? = null,
    val zustand: String = "",
    val rolle: String = "",
    val funkrufname: String? = null,
    val spieler: Int = 0,
    val maxSpieler: Int = 0,
    val platzFrei: Boolean = false,
) {
    /** „im Dienst in Uelzen · 3/12" — die eine Zeile, die es dazu zu sagen gibt. */
    val ansage: String
        get() = buildString {
            append("im Dienst")
            if (ort.isNotBlank()) append(" in $ort")
            if (maxSpieler > 0) append(" · $spieler/$maxSpieler")
        }
}

// ---------------------------------------------------------------- Gemeinschaft

@Serializable
data class Gemeinschaft(
    val id: String = "",
    val name: String = "",
    val beschreibung: String? = null,
    val landkreisId: String? = null,
    val landkreis: String? = null,
    val wappenfarbe: Int = 0,
    val kopfmuster: String = "keines",
    val emblemrahmen: String = "keiner",
    val emblemzeichen: String = "keines",
    val beiname: String = "",
    val mitglieder: Int = 0,
    val maxMitglieder: Int = 0,
    val oeffentlich: Boolean = false,
    /** `Einladung`, `Antrag` oder `Offen`. */
    val beitrittModus: String = "Antrag",
    val mindestLevel: Int = 0,
    val mindestErfahrung: Int = 0,
    /** `Mitglied`, `Zugfuehrer`, `Leitung` — oder `null`: nicht dabei. */
    val eigeneRolle: String? = null,
    val ungelesen: Int = 0,
    /** Nur ab Zugführer gefüllt — sonst `null`. */
    val beitrittscode: String? = null,
    val gegruendetUm: String = "",
    val inRangliste: Boolean = false,
    /** Der Aushang an der Pinnwand. */
    val ankuendigung: String? = null,
    val ankuendigungUm: String? = null,
    val laufendeRundeCode: String? = null,
    val darfRundeSchliessen: Boolean = false,
    /** Nur für Entscheider gefüllt, sonst 0. */
    val offeneAntraege: Int = 0,
    val offeneMeldungen: Int = 0,
) {
    val istLeitung: Boolean get() = eigeneRolle == "Leitung"
    val darfFuehren: Boolean get() = eigeneRolle == "Leitung" || eigeneRolle == "Zugfuehrer"
}

// -------------------------------------------------------------------- Katalog

/**
 * Der Katalog — alles, was für jede Runde gleich ist.
 *
 * <b>Er ist groß</b> (Fahrzeuge, Stichworte, FMS-Tasten, Orte, alle 401 Kreise,
 * alle Leitstellen) und ändert sich zwischen zwei Freigaben nicht. Deshalb wird
 * er einmal geladen und behalten, nicht bei jedem Öffnen der Seite.
 *
 * Gelesen werden hier bisher nur die Landkreise — die anderen Felder kommen mit
 * den Ansichten, die sie brauchen.
 */
@Serializable
data class Katalog(
    val landkreise: List<Landkreis> = emptyList(),
    val fahrzeuge: List<Fahrzeugvorlage> = emptyList(),
    val stichworte: List<Stichwort> = emptyList(),
    val fmsStatus: List<FmsTaste> = emptyList(),
    val meldende: List<String> = emptyList(),
)

/**
 * Ein Einsatzstichwort — die Vorlage, aus der die Leitstelle einen Einsatz baut.
 *
 * `meldebilder` und `lagemeldungen` gehören dazu: Das eine füllt den Einsatz
 * beim Anlegen, das andere gibt der Besatzung vor Ort die Sätze, die zu dieser
 * Lage passen — niemand tippt „Feuer unter Kontrolle" auf einer Handytastatur,
 * wenn der Satz einen Fingertipp entfernt steht.
 */
@Serializable
data class Stichwort(
    val id: String = "",
    val organisation: String = "Feuerwehr",
    val stichwort: String = "",
    val stichwortText: String = "",
    val prioritaet: Int = 2,
    val empfohleneFahrzeuge: Int = 0,
    val empfohleneFaehigkeiten: List<String> = emptyList(),
    val meldebilder: List<String> = emptyList(),
    val lagemeldungen: List<String> = emptyList(),
)

/**
 * Eine FMS-Taste.
 *
 * <b>Die Aufschrift je Organisation kommt vom Server.</b> Drei Status heißen
 * nicht überall gleich; welche das sind, entscheidet `FmsRules` dort — hier
 * steht keine zweite Tabelle.
 */
@Serializable
data class FmsTaste(
    val status: Int = 0,
    val bezeichnung: String = "",
    val jeOrganisation: Map<String, String> = emptyMap(),
) {
    fun aufschrift(organisation: String): String = jeOrganisation[organisation] ?: bezeichnung
}

/**
 * Ein Landkreis.
 *
 * <b>`hatDaten` ist die wichtigste Angabe darin.</b> Die Liste führt alle 401
 * Kreise, echte Wachen- und Straßendaten liegen aber nur für einen Teil vor. Wer
 * blind einen aussucht, landet mit hoher Wahrscheinlichkeit im erfundenen
 * Standardbereich und liest den Hinweis darauf wie eine Fehlermeldung. Deshalb
 * stehen die Kreise mit echten Daten in der Auswahl **zuerst und beschriftet** —
 * eine stillschweigend umsortierte Liste sähe aus wie eine alphabetische, in der
 * ein paar Namen an der falschen Stelle stehen.
 */
@Serializable
data class Landkreis(
    val id: String = "",
    val name: String = "",
    val kreisstadt: String = "",
    val hatDaten: Boolean = false,
    val wachen: Int = 0,
    val maxSpieler: Int = 0,
    val bundesland: String = "",
    val art: String = "",
) {
    /**
     * Wie der Kreis in der Liste steht.
     *
     * Eine kreisfreie Stadt bekommt „(Stadt)" dahinter; wo die Kreisstadt anders
     * heißt als der Kreis, steht sie in Klammern. Beides steht wortgleich so im
     * Web — es beantwortet die Frage „ist das der Kreis oder die Stadt?", die
     * sich bei rund achtzig Namen stellt.
     */
    val aufschrift: String
        get() = when {
            art == "KreisfreieStadt" -> "$name (Stadt)"
            kreisstadt.isNotBlank() && kreisstadt != name -> "$name ($kreisstadt)"
            else -> name
        }
}

/**
 * Wie ein Bundesland heißt, wenn ein Mensch es liest.
 *
 * Der Server schickt den Namen der Aufzählung — `RheinlandPfalz`,
 * `NordrheinWestfalen`. Das steht in keiner Auswahlliste: Der Bindestrich ist
 * Teil des Namens, und „NordrheinWestfalen" liest sich wie ein Tippfehler.
 * Dieselbe Tabelle steht im Web als `BUNDESLAND_LABEL`.
 *
 * <b>Ein unbekannter Wert bleibt, wie er ist.</b> Käme ein siebzehntes Land
 * dazu, stünde es hier roh — aber es stünde da, statt zu fehlen.
 */
val BUNDESLAENDER: Map<String, String> = mapOf(
    "BadenWuerttemberg" to "Baden-Württemberg",
    "Bayern" to "Bayern",
    "Berlin" to "Berlin",
    "Brandenburg" to "Brandenburg",
    "Bremen" to "Bremen",
    "Hamburg" to "Hamburg",
    "Hessen" to "Hessen",
    "MecklenburgVorpommern" to "Mecklenburg-Vorpommern",
    "Niedersachsen" to "Niedersachsen",
    "NordrheinWestfalen" to "Nordrhein-Westfalen",
    "RheinlandPfalz" to "Rheinland-Pfalz",
    "Saarland" to "Saarland",
    "Sachsen" to "Sachsen",
    "SachsenAnhalt" to "Sachsen-Anhalt",
    "SchleswigHolstein" to "Schleswig-Holstein",
    "Thueringen" to "Thüringen",
)

/** Der lesbare Name eines Bundeslands. */
fun bundeslandname(roh: String): String = BUNDESLAENDER[roh] ?: roh

/** Die Antwort auf „Leitstelle besetzen". */
@Serializable
data class Raum(val code: String = "")

/** Was `/api/rooms/{code}` über einen Raum verrät, bevor man beitritt. */
@Serializable
data class Rauminfo(
    val code: String = "",
    val landkreis: String? = null,
    val ort: String? = null,
    val spieler: Int = 0,
    val maxSpieler: Int = 0,
    val laeuft: Boolean = false,
)

// -------------------------------------------------------------------- Profil

/**
 * Ein Profil — das eigene wie ein fremdes.
 *
 * <b>Es ist nicht dasselbe wie das Konto.</b> Das Konto trägt, was einen
 * anmeldet und was man besitzt (Merkmal, Credits, Premium); das Profil trägt,
 * was andere sehen (Wappen, Muster, Vorstellung). Deshalb zwei Wege und zwei
 * Modelle — und deshalb braucht das eigene Banner beide.
 */
@Serializable
data class Profil(
    val kennung: String = "",
    val benutzername: String = "",
    val anzeigename: String = "",
    val wappen: String = "Keines",
    val wappenfarbe: Int = 0,
    val premium: Boolean = false,
    val vorstellung: String? = null,
    val level: Int? = null,
    val rang: String? = null,
    val schichten: Int? = null,
    val einsaetze: Int? = null,
    val gemeinschaft: String? = null,
    val anwesenheit: Anwesenheit? = null,
    val zuletztGesehen: String? = null,
    val dabeiSeit: String = "",
    val ich: Boolean = false,
    val teammitglied: Boolean = false,
    val melderGesicht: String = "",
    val profilrahmen: String = "keiner",
    val kopfmuster: String = "keines",
    val profilbild: String? = null,
    val titel: String? = null,
)

/**
 * Was am Profil geändert werden darf.
 *
 * <b>Nur gesetzte Felder gehen mit.</b> `explicitNulls = false` in `Netz.abgabe`
 * lässt weg, was `null` ist — der Server unterscheidet zwischen „nicht
 * geändert" und „ausdrücklich geleert", und ein durchgereichtes `null` an einem
 * Feld, das man gar nicht angefasst hat, löschte es.
 */
@Serializable
data class Profilaenderung(
    val anzeigename: String? = null,
    val vorstellung: String? = null,
    val wappen: String? = null,
    val wappenfarbe: Int? = null,
    val melderGesicht: String? = null,
    val profilrahmen: String? = null,
    val kopfmuster: String? = null,
    val titel: String? = null,
)

/**
 * Der Stand des eigenen Profilbildes.
 *
 * <b>Ein Bild wird nicht gesetzt, sondern eingereicht.</b> Zwischen dem
 * Hochladen und dem Sichtbarwerden liegt eine Freigabe durch die Verwaltung —
 * deshalb antwortet kein Aufruf mit einem Profil, sondern mit diesem Stand.
 *
 * <b>Ein bereits sichtbares Bild bleibt stehen</b>, solange das neue wartet.
 * Abgelöst wird es erst mit der Freigabe; wer ein zweites einreicht, ist nicht
 * plötzlich bildlos.
 */
@Serializable
data class Profilbildstand(
    /** `Wartet`, `Freigegeben`, `Abgelehnt`, `Gesperrt` — oder nichts. */
    val zustand: String? = null,
    /** Das sichtbare, freigegebene Bild — `null` heißt, das Wappen gilt. */
    val bild: String? = null,
    /** Das wartende Bild; nur gesetzt, solange es wartet. */
    val eingereicht: String? = null,
    val eingereichtUm: String? = null,
    /** Der Wortlaut der Ablehnung oder Sperre, falls einer angegeben wurde. */
    val grund: String? = null,
    val maxBytes: Long = 0,
    val minKante: Int = 0,
    val erlaubteTypen: List<String> = emptyList(),
) {
    val wartet: Boolean get() = zustand.equals("Wartet", true)
    val abgelehnt: Boolean get() = zustand.equals("Abgelehnt", true) || zustand.equals("Gesperrt", true)
}

/** Was beim Tagesbonus herauskam. */
@Serializable
data class Gluecksradergebnis(
    val gewinn: Gluecksradfeld = Gluecksradfeld(),
    val shop: Shop = Shop(),
)

@Serializable
data class Gluecksradfeld(
    val id: String = "",
    val art: String = "",
    val betrag: Int = 0,
    /** Die Chance dieses Feldes in Prozent — sie steht in der Legende des Rads. */
    val gewicht: Int = 0,
    val text: String = "",
)

// ------------------------------------------------------------- Mitteilungen

/** Eine Mitteilung des Betriebs — Wartung, Neuigkeit, Hinweis. */
@Serializable
data class Betreibermitteilung(
    val id: String = "",
    val titel: String = "",
    val text: String = "",
    val link: String? = null,
    val linkText: String? = null,
    val ab: String? = null,
    val bis: String? = null,
)

/**
 * Welche Mitteilungen aufs Gerät dürfen — sechs Schalter, immer der ganze
 * Satz, nie ein einzelnes Feld (wie bei der Privatsphäre).
 */
@Serializable
data class Mitteilungseinstellungen(
    /** Melder geht, App im Hintergrund — das bisherige Verhalten. */
    val alarm: Boolean = true,
    /** Einmal täglich 12:00 Uhr (Europe/Berlin). Vorgabe: aus. */
    val tageserinnerung: Boolean = false,
    /** Jemand lädt in eine Runde oder Wachengemeinschaft ein. */
    val einladungen: Boolean = true,
    /** Neue Zeile im Chat der eigenen Gemeinschaft und Meldungen der Wache. */
    val chat: Boolean = true,
    /** Direktnachricht von einem Freund. */
    val privatnachrichten: Boolean = true,
    /** Kommentare auf eigene Bretteinträge. */
    val brett: Boolean = true,
)

/** Eine Rundeneinladung von einem Freund — die Karte auf dem Start. */
@Serializable
data class Einladung(
    val nr: Int = 0,
    val von: String = "",
    val vonName: String = "",
    val roomCode: String = "",
    val ort: String = "",
    val landkreis: String? = null,
    val zustand: String? = null,
    val spieler: Int = 0,
    val maxSpieler: Int = 0,
    val mitspieler: List<String> = emptyList(),
    val erstelltUm: String = "",
    /** Ob ein Beitritt jetzt noch möglich ist. */
    val annehmbar: Boolean = true,
    /** Warum nicht — volle Runde, beendete Schicht. */
    val hinweis: String? = null,
    /** Ein Zuschauerplatz statt eines vollwertigen Mitspielerplatzes. */
    val alsZuschauer: Boolean = false,
)

/**
 * Eine Verwarnung, die ein Mensch ausgesprochen hat. Der Server liefert nur
 * die noch nicht zur Kenntnis genommenen.
 */
@Serializable
data class Verwarnung(
    val nr: Int = 0,
    val grund: String = "",
    val von: String = "",
    val erteiltUm: String = "",
    val gelesenUm: String? = null,
)

/** Eine direkte Nachricht der Verwaltung — Popup bis zur Bestätigung. */
@Serializable
data class AdminNachricht(
    val nr: Int = 0,
    val titel: String = "",
    val text: String = "",
    val von: String = "",
    val gesendetUm: String = "",
    val gelesenUm: String? = null,
)

// ---------------------------------------------------------------- Soziales

/**
 * Eine Direktnachricht — die Zeile eines Gesprächs.
 *
 * `terminText`/`terminZeitpunkt` machen aus der Zeile einen Terminvorschlag;
 * `terminStatus` (`Offen`, `Zugesagt`, `Abgesagt`) trägt die Antwort. Eine
 * beantwortete Zeile kommt als Ersatz derselben `nr` noch einmal.
 */
@Serializable
data class Nachricht(
    val nr: Long = 0,
    val von: String = "",
    val an: String = "",
    val text: String = "",
    val gesendetUm: String = "",
    val gelesenUm: String? = null,
    val terminText: String? = null,
    val terminZeitpunkt: String? = null,
    val terminStatus: String? = null,
    val geschenk: Geschenk? = null,
)

@Serializable
data class Geschenk(
    val nr: Long = 0,
    val geoeffnet: Boolean = false,
    val artikelName: String? = null,
    val fuerMich: Boolean = false,
)

/** Wer einen Bretteintrag oder Kommentar geschrieben hat. */
@Serializable
data class BrettVerfasser(
    val kennung: String = "",
    val benutzername: String = "",
    val anzeigename: String = "",
    val wappen: String? = null,
    val wappenfarbe: Int = 0,
    val profilbild: String? = null,
    val premium: Boolean = false,
    val teammitglied: Boolean = false,
)

/**
 * Ein Bretteintrag — getippter Beitrag oder automatischer Erfolg.
 *
 * `art` unterscheidet: `Beitrag`, `Schicht`, `Befoerderung`, `Abzeichen`,
 * `Rekord`, `Wachenstufe`, `Gemeinschaftsbeitritt`. Die Quittung ist die
 * einzige Reaktionsart — bewusst.
 */
@Serializable
data class Bretteintrag(
    val nr: Long = 0,
    val verfasser: BrettVerfasser = BrettVerfasser(),
    val art: String = "Beitrag",
    val titel: String? = null,
    val text: String? = null,
    val sichtbarkeit: String = "Freunde",
    val bezugArt: String? = null,
    val bezugId: String? = null,
    val erstelltUm: String = "",
    val quittungen: Int = 0,
    val kommentare: Int = 0,
    val vonMirQuittiert: Boolean = false,
    val vonMir: Boolean = false,
)

@Serializable
data class Brettkommentar(
    val nr: Long = 0,
    val eintragNr: Long = 0,
    val verfasser: BrettVerfasser = BrettVerfasser(),
    val text: String = "",
    val erstelltUm: String = "",
    val vonMir: Boolean = false,
)

/** Eine Seite des Bretts — geblättert wird über `weiter` (Nummer, kein Offset). */
@Serializable
data class BrettSeite(
    val eintraege: List<Bretteintrag> = emptyList(),
    val weiter: Long? = null,
)

// ------------------------------------------------------------- Gemeinschaft

/** Ein Mitglied der Wachengemeinschaft — sortiert kommt es vom Server. */
@Serializable
data class Gemeinschaftsmitglied(
    val kennung: String = "",
    val benutzername: String = "",
    val anzeigename: String = "",
    val wappen: String? = null,
    val wappenfarbe: Int = 0,
    /** `Mitglied`, `Zugfuehrer` oder `Leitung` — die drei Stufen. */
    val rolle: String = "Mitglied",
    val beigetretenUm: String = "",
    val level: Int? = null,
    val rang: String? = null,
    val anwesenheit: Anwesenheit? = null,
    val profilbild: String? = null,
    val premium: Boolean? = null,
    val teammitglied: Boolean? = null,
)

@Serializable
data class Gemeinschaftsantrag(
    val nr: Long = 0,
    val gemeinschaftId: String = "",
    val gemeinschaftName: String = "",
    val kennung: String = "",
    val anzeigename: String = "",
    val von: String? = null,
    val vonName: String? = null,
    /** `Einladung` oder `Bewerbung`. */
    val richtung: String = "Bewerbung",
    val stand: String = "Offen",
    val nachricht: String? = null,
    val erstelltUm: String = "",
    val benutzername: String? = null,
)

/** Eine Zeile im Wachenchat — Text, System oder Clanrunden-Kachel. */
@Serializable
data class Gemeinschaftsnachricht(
    val nr: Long = 0,
    val gemeinschaftId: String = "",
    val von: String? = null,
    val vonName: String = "",
    /** Leer, wenn `entfernt`. */
    val text: String = "",
    /** `Text`, `System` oder `Clanrunde`. */
    val art: String = "Text",
    val roomCode: String? = null,
    val gesendetUm: String = "",
    val entfernt: Boolean = false,
    val entferntVon: String? = null,
)

@Serializable
data class Gemeinschaftsmeldung(
    val nr: Long = 0,
    val nachrichtNr: Long = 0,
    val text: String = "",
    val verfasserName: String = "",
    val verfasserKennung: String? = null,
    val melderName: String = "",
    val grund: String? = null,
    val gemeldetUm: String = "",
    val entfernt: Boolean = false,
)

@Serializable
data class Terminzusage(
    val kennung: String = "",
    val anzeigename: String = "",
    /** `Offen`, `Zugesagt`, `Abgesagt`. */
    val antwort: String = "Offen",
)

@Serializable
data class Wachentermin(
    val nr: Long = 0,
    val titel: String = "",
    val wann: String = "",
    val von: String = "",
    val vonName: String? = null,
    val roomCode: String? = null,
    val abgesagt: Boolean = false,
    val zusagen: List<Terminzusage> = emptyList(),
)

/** Der Stufenstand der Wache — Erfahrung, Titel, Freischaltungen. */
@Serializable
data class Wachenstatistik(
    val erfahrung: Int = 0,
    val stufe: Int = 0,
    val bezeichnung: String = "",
    val bisZurNaechsten: Int? = null,
    val schwelle: Int? = null,
    val schichten: Int = 0,
    val einsaetze: Int = 0,
    val maxMitglieder: Int = 0,
    val maxTermine: Int = 0,
)

/** Der volle Blick auf die eigene Gemeinschaft — Anträge nur für Entscheider. */
@Serializable
data class GemeinschaftDetail(
    val gemeinschaft: Gemeinschaft = Gemeinschaft(),
    val mitglieder: List<Gemeinschaftsmitglied> = emptyList(),
    val antraege: List<Gemeinschaftsantrag> = emptyList(),
    val statistik: Wachenstatistik = Wachenstatistik(),
    val termine: List<Wachentermin> = emptyList(),
    val meldungen: List<Gemeinschaftsmeldung> = emptyList(),
)

/** Eine Runde, die gerade Verstärkung sucht — die öffentliche Liste. */
@Serializable
data class OeffentlicheRunde(
    val code: String = "",
    /** `Lobby`, `Laeuft`, `Beendet`. */
    val state: String = "Lobby",
    /** `Zufall`, `Frei`, `Ausbildung`, `Tagesschicht`, `Szenario`. */
    val mode: String = "Zufall",
    val landkreis: String? = null,
    val leitstelle: String = "",
    val spieler: Int = 0,
    val maxSpieler: Int = 0,
    val freiePlaetze: Int = 0,
    val offeneEinsaetze: Int = 0,
    val leitstelleBesetzt: Boolean = false,
    /** Ein selbst gebauter Ausrückebereich — die Runde ist nicht gewertet. */
    val sandkasten: Boolean = false,
)

/** Ein Platz auf der Tagesliste der Schicht des Tages. */
@Serializable
data class Tagesschichtplatz(
    val platz: Int = 0,
    val anzeigename: String = "",
    val punkte: Int = 0,
    val istEigenerEintrag: Boolean = false,
)

/** Der Stand der Schicht des Tages: Tag, Kreis, Tagesliste, eigener Platz. */
@Serializable
data class Tagesschicht(
    val datum: String = "",
    val landkreis: String? = null,
    val gefahren: Int = 0,
    val selbstGefahren: Boolean = false,
    val eigenerPlatz: Int? = null,
    val eigenePunkte: Int? = null,
    val beste: List<Tagesschichtplatz> = emptyList(),
)

// ------------------------------------------------------------- Privatsphäre

/**
 * Wer was von einem sieht.
 *
 * <b>Jede dieser Angaben wird am Server geprüft</b> — die Ansicht zeigt sie nur
 * an, sie setzt sie nicht durch. Wer hier etwas ausschaltet, verschwindet
 * wirklich aus der Liste; die App verbirgt nichts nachträglich.
 *
 * Die drei Stufen heißen `Alle`, `Mitspieler`, `Niemand`. Sie stehen als
 * Zeichenkette und nicht als Aufzählung, weil eine vierte Stufe des Servers die
 * App sonst zum Absturz brächte statt zu einer unbekannten Auswahl.
 */
@Serializable
data class Privatsphaere(
    /** Der große Schalter: nimmt Bestenliste, Vorschläge und den offenen Spielstand zurück. */
    val privatesKonto: Boolean = false,
    /** Wer dieses Konto über seinen Benutzernamen finden darf. */
    val auffindbarkeit: String = "Alle",
    /** Wer eine Freundschaftsanfrage schicken darf. */
    val anfragenVon: String = "Alle",
    val einladungenErlauben: Boolean = true,
    val anwesenheitZeigen: Boolean = true,
    val zuletztGesehenZeigen: Boolean = true,
    val inBestenliste: Boolean = true,
    val alsVorschlagErscheinen: Boolean = true,
    val chronikTeilen: Boolean = true,
    val gemeinschaftseinladungenErlauben: Boolean = true,
    val gemeinschaftZeigen: Boolean = true,
    val inGemeinschaftsliste: Boolean = true,
    val wachenstatistikTeilen: Boolean = true,
    val kommentareErlauben: Boolean = true,
    val lesebestaetigungenSenden: Boolean = true,
    val inWeltSichtbar: Boolean = true,
)

/** Die drei Stufen, in denen sich Sichtbarkeit einstellen lässt. */
val SICHTBARKEITEN = listOf("Alle", "Mitspieler", "Niemand")
