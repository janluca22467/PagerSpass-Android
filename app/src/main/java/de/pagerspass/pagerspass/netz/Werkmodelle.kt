package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Modelle der Werkstatt — Dienstbuch im Ganzen, Lehrgang, Übungen,
 * Leitstellenbau, Rundenvorlagen und das, was der Startbildschirm außer den
 * festen Wegen trägt. Übertragen aus `web/src/types.ts`.
 *
 * <b>Eigene Datei und nicht `Spielmodelle.kt`</b>, weil das hier alles Dinge
 * sind, die ein Spieler *baut* oder *nachliest* — nicht Dinge, die eine Runde
 * trägt. Die Grenze ist dieselbe wie im Server, wo Szenarien, Vorlagen und
 * Lehrgänge eigene Endpunktdateien haben.
 *
 * <b>Enums stehen als Zeichenketten.</b> Der Server schickt sie so (`Lage`,
 * `Stoerung`, `Klar`, `Glaette` …), und eine Kotlin-Aufzählung, die einen neuen
 * Wert nicht kennt, brächte beim Einlesen die ganze Liste zu Fall. Die
 * lesbaren Namen stehen unten in den Beschriftungstabellen.
 */

// --------------------------------------------------------------- Dienstbuch

/** Eine beendete Schicht, wie das Archiv sie führt — ohne die eigene Ausbeute. */
@Serializable
data class Archiveintrag(
    val code: String = "",
    val ort: String = "",
    val leitstelle: String = "",
    val landkreis: String? = null,
    val mode: String = "Zufall",
    val gestartetUm: String? = null,
    val beendetUm: String? = null,
    val einsaetze: Int = 0,
    val abgeschlosseneEinsaetze: Int = 0,
    val spieler: Int = 0,
    val fahrzeuge: Int = 0,
    val funksprueche: Int = 0,
)

/**
 * Eine Zeile im Dienstbuch — Archiv und Chronik zusammengeführt.
 *
 * <b>Warum zusammengeführt.</b> Das Archiv weiß, *welche* Schichten es gab; die
 * Chronik, was man selbst darin war und bekam. Erst beide zusammen ergeben die
 * Zeile, die man sucht — dieselbe Naht wie in `stores/dienstbuch.ts`.
 */
data class Schichtzeile(
    val code: String,
    val beendetUm: String?,
    val ort: String,
    val landkreis: String?,
    val einsaetze: Int,
    val hilfsfristSekunden: Int?,
    val rolle: String?,
    val funkrufname: String?,
    val fahrzeugtyp: String?,
    val punkte: Int?,
)

/** Wofür es in einer Schicht Punkte gab. */
@Serializable
data class Erfahrungsposten(
    val grund: String = "",
    val text: String = "",
    val punkte: Int = 0,
)

/** Die Rekorde eines Spielers über die zuletzt gefahrenen Schichten. */
@Serializable
data class Spielerrekorde(
    val schnellsteAusrueckzeitSekunden: Double? = null,
    val kuerzesteHilfsfristSekunden: Double? = null,
    val kuerzesteHilfsfristStichwort: String? = null,
    val meisteLagemeldungenSchicht: Int = 0,
)

/** Die Bilanz eines Mitspielers — aus der Statistik und aus der Auswertung einer Runde. */
@Serializable
data class Spielerbilanz(
    val playerId: String = "",
    val name: String = "",
    val rolle: String = "Unbestimmt",
    val runden: Int = 0,
    val einsaetze: Int = 0,
    val lagemeldungen: Int = 0,
    val funksprueche: Int = 0,
    val ausrueckzeitSekunden: Double? = null,
    val ausserDienstSekunden: Double = 0.0,
    val rekorde: Spielerrekorde = Spielerrekorde(),
)

/** Eine Runde als Punkt im Hilfsfristverlauf. */
@Serializable
data class Rundenkennzahl(
    val code: String = "",
    val ort: String = "",
    val beendetUm: String? = null,
    val einsaetze: Int = 0,
    val hilfsfristSekunden: Double? = null,
    val dispositionszeitSekunden: Double? = null,
)

/** Die Statistik über die letzten Schichten: Verlauf und Mitspieler. */
@Serializable
data class Archivstatistik(
    val runden: Int = 0,
    val spieler: List<Spielerbilanz> = emptyList(),
    val verlauf: List<Rundenkennzahl> = emptyList(),
)

@Serializable
data class Hilfsfristpunkt(
    val beendetUm: String = "",
    val hilfsfristSekunden: Double? = null,
)

/** Die Kurve eines Wachenmitglieds — für den Vergleich im Verlauf. */
@Serializable
data class Mitgliedshilfsfrist(
    val anzeigename: String = "",
    val verlauf: List<Hilfsfristpunkt> = emptyList(),
)

/** Eine Stufe der Laufbahn. `fahrzeuge` sind die Gutscheine, die sie bringt. */
@Serializable
data class Rang(
    val level: Int = 0,
    val bezeichnung: String = "",
    val ab: Int = 0,
    val fahrzeuge: Int = 0,
)

@Serializable
data class Saisonplatz(
    val platz: Int = 0,
    val anzeigename: String = "",
    val punkte: Int = 0,
    val istEigenerEintrag: Boolean = false,
)

/**
 * Eine Zusatzfunktion des Kontos — eigene Leitstelle, eigene AAO, Sandkasten.
 *
 * `offen` heißt freigeschaltet. Sie steht am Konto; die App liest hier nur die
 * Liste, nicht das ganze Konto ein zweites Mal.
 */
@Serializable
data class Freischaltung(
    val was: String = "",
    val bezeichnung: String = "",
    val abLevel: Int = 0,
    val abRang: String = "",
    val offen: Boolean = false,
)

@Serializable
data class Kontofreischaltungen(val freischaltungen: List<Freischaltung> = emptyList())

/** Das eigene Profil, nur so weit, wie die Vitrine es braucht. */
@Serializable
data class Vitrinenprofil(val vitrine: List<Abzeichen> = emptyList())

@Serializable
data class Vitrinenaenderung(val vitrine: List<String>)

/** Woher die Erfahrung kam — je Organisation, aus der Garage gelesen. */
@Serializable
data class Orgerfahrung(val organisation: String = "", val erfahrung: Int = 0)

@Serializable
data class Garagenauszug(
    val fahrzeuge: List<String> = emptyList(),
    val offeneWahlen: Int = 0,
    val proOrganisation: List<Orgerfahrung> = emptyList(),
)

@Serializable
data class Dienstwoche(
    val beginn: String = "",
    val schichten: Int = 0,
    val einsaetze: Int = 0,
    val punkte: Int = 0,
    val hilfsfristSekunden: Double? = null,
)

@Serializable
data class Dienstfahrzeug(
    val typ: String = "",
    val schichten: Int = 0,
    val einsaetze: Int = 0,
)

/** Die Auswertung über alle Schichten — die Abo-Leistung des Dienstbuchs. */
@Serializable
data class Dienstauswertung(
    val schichten: Int = 0,
    val einsaetze: Int = 0,
    val punkte: Int = 0,
    val leitstellenschichten: Int = 0,
    val fahrzeugschichten: Int = 0,
    val ersteSchicht: String? = null,
    val letzteSchicht: String? = null,
    val hilfsfristSekunden: Double? = null,
    val besteHilfsfristSekunden: Double? = null,
    val verlauf: List<Dienstwoche> = emptyList(),
    val fahrzeuge: List<Dienstfahrzeug> = emptyList(),
)

// ------------------------------------------------- Eine Schicht im Archiv

/**
 * Ein Einsatz einer vergangenen Schicht.
 *
 * <b>Nicht `Einsatz` aus den Rundenmodellen</b>: Der trägt, was der Dienst
 * braucht (MANV, Abschnitte, Aufgaben); hier zählt, was man nachher wissen will
 * — wann alarmiert, wann vor Ort, wie lang die Hilfsfrist.
 */
@Serializable
data class Archiveinsatz(
    val id: String = "",
    val einsatznummer: String = "",
    val stichwort: String = "",
    val stichwortText: String = "",
    val meldebild: String = "",
    val adresse: String = "",
    val organisation: String = "Feuerwehr",
    val prioritaet: Int = 2,
    val state: String = "Offen",
    val alarmierteFahrzeuge: List<String> = emptyList(),
    val nachforderungen: Int = 0,
    val eingangUm: String? = null,
    val erstAlarmUm: String? = null,
    val erstesFahrzeugVorOrtUm: String? = null,
    val abgeschlossenUm: String? = null,
    val dispositionszeitSekunden: Double? = null,
    val hilfsfristSekunden: Double? = null,
)

@Serializable
data class Befund(
    /** `Lob`, `Hinweis` oder `Mangel`. */
    val grad: String = "Hinweis",
    val titel: String = "",
    val text: String = "",
    val incidentId: String? = null,
)

@Serializable
data class Doppelmeldungen(
    val mehrfachGemeldeteLagen: Int = 0,
    val zusammengefuehrt: Int = 0,
    val falschzugeordnet: Int = 0,
    val doppelalarmierungen: Int = 0,
    val nennenswert: Boolean = false,
)

@Serializable
data class Rundenauswertung(
    val befunde: List<Befund> = emptyList(),
    val spieler: List<Spielerbilanz> = emptyList(),
    val doppelmeldungen: Doppelmeldungen = Doppelmeldungen(),
)

@Serializable
data class Archivspieler(
    val id: String = "",
    val name: String = "",
    val role: String = "Unbestimmt",
    val vehicleId: String? = null,
    val istBot: Boolean = false,
)

@Serializable
data class Archivfahrzeug(
    val id: String = "",
    val funkrufname: String = "",
    val typ: String = "",
    val organisation: String = "",
)

@Serializable
data class Archiveinstellungen(
    val mode: String = "Zufall",
    val leitstelle: String = "",
    val ort: String = "",
    val landkreis: String? = null,
)

/** Eine eigene Schicht vollständig — mit dem ganzen Funkprotokoll. */
@Serializable
data class Archivrunde(
    val code: String = "",
    val settings: Archiveinstellungen = Archiveinstellungen(),
    val gestartetUm: String? = null,
    val beendetUm: String? = null,
    val players: List<Archivspieler> = emptyList(),
    val vehicles: List<Archivfahrzeug> = emptyList(),
    val incidents: List<Archiveinsatz> = emptyList(),
    val funkprotokoll: List<Funkzeile> = emptyList(),
    val auswertung: Rundenauswertung = Rundenauswertung(),
)

// ---------------------------------------------------------------- Lehrgang

@Serializable
data class Lehrgangsmodul(
    val id: String = "",
    /** `Lesestoff`, `Lektion` oder `Pruefung`. */
    val art: String = "Lesestoff",
    val titel: String = "",
    val text: String = "",
    val wikiseite: String? = null,
    val erledigt: Boolean = false,
    val bestanden: Boolean = false,
    val schichtCode: String? = null,
    val erledigtUm: String? = null,
)

@Serializable
data class Lehrgang(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String = "",
    val module: List<Lehrgangsmodul> = emptyList(),
    val bestanden: Boolean = false,
    val bestandenUm: String? = null,
    val zeugnisSchicht: String? = null,
    val gesperrt: Boolean = false,
    val voraussetzungen: List<String> = emptyList(),
)

// ----------------------------------------------------------------- Übungen

@Serializable
data class Szenariozeile(
    val id: String = "",
    val name: String = "",
    val beschreibung: String = "",
    val landkreisId: String? = null,
    val code: String = "",
    val eintraege: Int = 0,
    val geaendertUm: String = "",
)

/**
 * Ein Eintrag der Zeitachse einer Übung.
 *
 * Die Felder einer Lage sind genau die des Einsatzbogens; eine Störung trägt nur
 * ihre Art, ein Wetterwechsel Wetter und Wind. Was nicht dazugehört, bleibt
 * `null` — und wird beim Senden weggelassen (`explicitNulls = false`).
 */
@Serializable
data class Szenarioeintrag(
    /** `Lage`, `Stoerung` oder `Wetterwechsel`. */
    val art: String = "Lage",
    val nachSekunden: Int = 0,
    val stichwort: String? = null,
    val stichwortText: String? = null,
    val meldebild: String? = null,
    val adresse: String? = null,
    val organisation: String? = null,
    val prioritaet: Int? = null,
    val ortsteil: String? = null,
    val meldender: String? = null,
    val empfohleneFahrzeuge: Int? = null,
    val empfohleneFaehigkeiten: List<String>? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val stoerung: String? = null,
    val wetter: String? = null,
    val windrichtung: Int? = null,
)

@Serializable
data class Szenarioeinstellungen(
    val tagesalarmstaerke: Boolean? = true,
    val loeschwasser: Boolean? = true,
    val sonderobjekte: Boolean? = true,
    val wiederherstellung: Boolean? = true,
    val einsatzarbeit: Boolean? = true,
    val suchlagen: Boolean? = true,
    val vegetationsbraende: Boolean? = true,
    val gefahrgutlagen: Boolean? = true,
    val telefonischeLeitstelle: Boolean? = false,
    val wetter: String? = "Klar",
    val windrichtung: Int? = 0,
    val jahreszeit: String? = null,
)

/** Eine Übung mit allem, was drinsteht — so, wie der Editor sie bearbeitet. */
@Serializable
data class Szenario(
    val id: String = "",
    val name: String = "",
    val beschreibung: String = "",
    val landkreisId: String? = null,
    val code: String = "",
    val aufstellung: List<String> = emptyList(),
    val zeitachse: List<Szenarioeintrag> = emptyList(),
    val einstellungen: Szenarioeinstellungen = Szenarioeinstellungen(),
    val geaendertUm: String = "",
)

/** Was wann geübt wurde. Übungen bringen keine Punkte — deshalb steht das hier und nicht im Dienstbuch. */
@Serializable
data class Uebungsfahrt(
    val name: String = "",
    val roomCode: String = "",
    val beendetUm: String = "",
    val rolle: String = "Unbestimmt",
    val funkrufname: String? = null,
    val einsaetze: Int = 0,
)

// ----------------------------------------------------------- Leitstellenbau

@Serializable
data class Vorlagenzeile(
    val id: String = "",
    val name: String = "",
    val beschreibung: String = "",
    val landkreisId: String? = null,
    val code: String = "",
    val wachen: Int = 0,
    val maxSpieler: Int = 0,
    val geaendertUm: String = "",
)

/**
 * Eine bespielte Wache im Sandkasten.
 *
 * `lat`, `lon` und `organisation` sind **Abweichungen** vom Kartenabzug — `null`
 * heißt „wie im Abzug". Eine selbst gebaute Wache (Kennung mit `eigen-` davor)
 * hat keinen Abzug und trägt deshalb alles selbst.
 */
@Serializable
data class Wachenwahl(
    val kennung: String = "",
    val name: String? = null,
    val zugnummer: Int = 1,
    val lat: Double? = null,
    val lon: Double? = null,
    val organisation: String? = null,
    val traeger: String? = null,
)

/** Eine Funkgruppe der eigenen Leitstelle. */
@Serializable
data class Baufunkgruppe(
    val id: String = "",
    val nummer: String = "",
    val name: String = "",
    val bezeichnung: String = "",
    val marke: String = "",
    val organisationen: List<String> = emptyList(),
    val hiOrgs: List<String> = emptyList(),
    val fuehrung: Boolean = false,
    val einsatzId: String? = null,
)

@Serializable
data class Vorlageneinstellungen(
    val mode: String? = "Zufall",
    val tagesalarmstaerke: Boolean? = true,
    val loeschwasser: Boolean? = true,
    val sonderobjekte: Boolean? = true,
    val wiederherstellung: Boolean? = true,
    val einsatzarbeit: Boolean? = true,
    val suchlagen: Boolean? = true,
    val vegetationsbraende: Boolean? = true,
    val gefahrgutlagen: Boolean? = true,
    val telefonischeLeitstelle: Boolean? = false,
    val oeffentlich: Boolean? = false,
    val wetter: String? = null,
    val windrichtung: Int? = null,
    val jahreszeit: String? = null,
)

/** Eine selbst gebaute Leitstelle mit allem, was drinsteht. */
@Serializable
data class Leitstellenvorlage(
    val id: String = "",
    val name: String = "",
    val beschreibung: String = "",
    val landkreisId: String? = null,
    val ort: String = "",
    val leitstelle: String = "",
    val code: String = "",
    val maxSpieler: Int = 0,
    val wachen: List<Wachenwahl> = emptyList(),
    val rufnamenpraefixe: Map<String, String> = emptyMap(),
    val kennzahlen: Map<String, String> = emptyMap(),
    val festeFunkrufnamen: Map<String, String> = emptyMap(),
    val funkgruppen: List<Baufunkgruppe> = emptyList(),
    val einstellungen: Vorlageneinstellungen = Vorlageneinstellungen(),
    val geaendertUm: String = "",
)

/** Eine echte Wache eines Landkreises aus dem Kartenabzug. */
@Serializable
data class Kreiswache(
    val kennung: String = "",
    val name: String = "",
    val organisation: String = "Feuerwehr",
    val traeger: String = "Keine",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val bespielt: Boolean = false,
    val wachnummer: Int = 0,
)

// ------------------------------------------------------------ Rundenvorlagen

@Serializable
data class Rundenvorlagenzeile(
    val id: String = "",
    val name: String = "",
    val landkreisId: String? = null,
    val leitstelleId: String? = null,
    val ganzerBereich: Boolean = false,
    val code: String = "",
    val geaendertUm: String = "",
)

@Serializable
data class Vorlagenbesatzung(
    val vorlageId: String = "",
    val funkrufname: String? = null,
    val kurzname: String? = null,
    val wacheKennung: String? = null,
)

@Serializable
data class Vorlageninhalt(
    val name: String = "",
    val landkreisId: String? = null,
    val maxSpieler: Int? = null,
    val bestand: List<Vorlagenbesatzung> = emptyList(),
)

// --------------------------------------------------------- Startbildschirm

/**
 * Eine frei konfigurierbare Kachel des Hauptmenüs.
 *
 * `pfad` sind fertige SVG-Pfaddaten auf dem 24er-Raster — der Server liefert sie
 * mit, damit das Spiel keine eigene Symbolbibliothek führen muss.
 */
@Serializable
data class Startkachel(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String? = null,
    val pfad: String = "",
    /** `route`, `raum` oder `link`. */
    val aktion: String = "link",
    val ziel: String = "",
    val premiumNoetig: Boolean = false,
    val marke: String? = null,
)

@Serializable
data class Footerknopf(
    val beschriftung: String = "",
    val ziel: String = "",
    val pfad: String = "",
    /** `beide`, `mobil` oder `desktop`. */
    val geraete: String = "beide",
)

@Serializable
data class OffeneUmfrage(
    val id: String = "",
    val frage: String = "",
    val optionen: List<String> = emptyList(),
)

@Serializable
data class Umfragestand(val umfrage: OffeneUmfrage? = null)

@Serializable
data class Umfrageantwort(val option: Int)

@Serializable
data class Raumcode(val code: String = "")

// --------------------------------------------------------- Beschriftungen

val ORG_NAME: Map<String, String> = mapOf(
    "Feuerwehr" to "Feuerwehr",
    "Rettungsdienst" to "Rettungsdienst",
    "Thw" to "THW",
    "Polizei" to "Polizei",
)

val ORG_KURZ: Map<String, String> = mapOf(
    "Feuerwehr" to "FW",
    "Rettungsdienst" to "RD",
    "Thw" to "THW",
    "Polizei" to "POL",
)

val ORGANISATIONEN: List<String> = listOf("Feuerwehr", "Rettungsdienst", "Thw", "Polizei")

val HIORG_NAME: Map<String, String> = mapOf(
    "Keine" to "",
    "Drk" to "DRK",
    "Juh" to "Johanniter",
    "Mhd" to "Malteser",
    "Asb" to "ASB",
    "Dlrg" to "DLRG",
    "Bergwacht" to "Bergwacht",
    "Wasserwacht" to "Wasserwacht",
    "Dgzrs" to "Seenotretter",
    "Brh" to "Rettungshunde",
    "Werkfeuerwehr" to "Werkfeuerwehr",
    "Privat" to "Privater RD",
)

val WETTER_NAME: Map<String, String> = mapOf(
    "Klar" to "Klar",
    "Regen" to "Regen",
    "Glaette" to "Glätte",
    "Sturm" to "Sturm",
)

val JAHRESZEIT_NAME: Map<String, String> = mapOf(
    "Fruehling" to "Frühling",
    "Sommer" to "Sommer",
    "Herbst" to "Herbst",
    "Winter" to "Winter",
)

val STOERUNG_NAME: Map<String, String> = mapOf(
    "BlinderAlarm" to "Blinder Alarm",
    "BoeswilligerNotruf" to "Böswilliger Notruf",
    "FehlalarmRueckruf" to "Fehlalarm-Rückruf",
    "Fahrzeugdefekt" to "Fahrzeug bleibt liegen",
    "Funkloch" to "Funkloch",
    "Schaulustige" to "Schaulustige",
    "Drohne" to "Drohne über der Einsatzstelle",
    "Presseanruf" to "Presse ruft an",
)

val SZENARIOART_NAME: Map<String, String> = mapOf(
    "Lage" to "Lage",
    "Stoerung" to "Störung",
    "Wetterwechsel" to "Wetter",
)

/** Die Rufnamenwörter nach BOS-Systematik — was gilt, wenn nichts eingetragen ist. */
val PRAEFIX_ORG: Map<String, String> = mapOf(
    "Feuerwehr" to "Florian",
    "Rettungsdienst" to "Rotkreuz",
    "Thw" to "Heros",
    "Polizei" to "Peter",
)

val PRAEFIX_HIORG: Map<String, String> = mapOf(
    "Drk" to "Rotkreuz",
    "Juh" to "Akkon",
    "Mhd" to "Johannes",
    "Asb" to "Sama",
    "Dlrg" to "Pelikan",
    "Bergwacht" to "Bergwacht",
    "Wasserwacht" to "Wasserwacht",
    "Dgzrs" to "Seenot",
    "Brh" to "Rettungshund",
    "Werkfeuerwehr" to "Florian",
    "Privat" to "Rettung",
)

/** Woran eine selbst gebaute Wache zu erkennen ist — siehe `Wachenwahl`. */
const val EIGEN_PRAEFIX = "eigen-"

fun istEigeneWache(kennung: String): Boolean = kennung.startsWith(EIGEN_PRAEFIX)
