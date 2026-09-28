package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/*
 * ---------------------------------------------------------------------------
 * PagerSpass - World: die eine gemeinsame Welt — übertragen aus `web/src/types.ts`
 * (ab `Weltfreischaltung`) und gegengelesen an `Contracts/WeltDtos.cs`.
 *
 * Es gibt sie genau einmal, sie läuft immer, und alle Premium-Spieler stehen auf
 * derselben Karte. Deshalb gibt es hier keinen Raumcode und keinen Raumzustand — was
 * der Arbeitsplatz braucht, sind drei Antworten: der Besitzstand (ändert sich beim
 * Kaufen), der Betrieb (alle fünf Sekunden) und die Großlage der Woche.
 *
 * <b>Aufzählungen stehen als Text.</b> Wachenart, Fahrzeuglage, Lagenzustand, Grund
 * einer Buchung — der Server schickt die Namen seiner Aufzählungen
 * (`JsonStringEnumConverter`). Als `enum class` brächte jeder neue Wert auf dem
 * Server die ganze Antwort zum Absturz; als Text steht er schlimmstenfalls roh da.
 *
 * <b>Alle Namen tragen `Welt` vorn</b>, auch die Icon-Typen (`WeltIconpack`,
 * `WeltPackicon`, `WeltBlaulicht`): Die Icon-Bibliothek hat ihre eigenen Modelle, und
 * zwei Klassen gleichen Namens im selben Paket vertrügen sich nicht.
 *
 * <b>Zeitpunkte sind Texte</b> (ISO 8601 vom Server); gerechnet wird mit ihnen in
 * `mobil/Weltfahrt.kt` über `weltzeit()`.
 * ---------------------------------------------------------------------------
 */

// ------------------------------------------------------------ Karte und Stand

/** Eine Leitstelle auf der gemeinsamen Karte. */
@Serializable
data class Weltleitstelle(
    val besitzerId: String = "",
    /** Der Anzeigename des Besitzers — `null`, wenn er sich in der Welt nicht zeigt. */
    val besitzer: String? = null,
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val kreis: String? = null,
    /** Die Weltstufe — `null` aus demselben Grund wie `besitzer`. */
    val stufe: Int? = null,
    val wachen: Int = 0,
    val gegruendet: String = "",
)

/** Eine gebaute Wache im eigenen Bestand. */
@Serializable
data class WeltWache(
    val id: String = "",
    val name: String = "",
    /** Die Bauart (`Wachenart` im Web) — `Feuerwehrhaus`, `Rettungswache`, … */
    val art: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val stellplaetze: Int = 0,
    val belegt: Int = 0,
    val preis: Int = 0,
    /** Wie oft schon ausgebaut wurde — 0 heißt Grundausbau. */
    val ausbaustufe: Int = 0,
    /** Wann der laufende Anbau fertig wird; null, wenn keiner läuft. */
    val ausbauFertigUm: String? = null,
    /** Was die nächste Stufe kostet; null, wenn keine mehr geht. */
    val ausbauPreis: Int? = null,
    /** Und wie lange sie dauert, in Minuten; null wie beim Preis. */
    val ausbauDauerMinuten: Int? = null,
    /** Der Name der Bauart — er kommt aus `Weltnamen` und wird hier nicht gebaut. */
    val bauart: String = "",
    /** Das Zeichen im Wappen dieser Wache. */
    val wappenZeichen: String = "",
    /** Ihr Palettenton — dieselbe Palette wie am Konto. */
    val wappenFarbe: Int = 0,
    /** Ob sie das freigegebene Profilbild des Kontos zeigt. */
    val fotoZeigen: Boolean = false,
    /**
     * Zu welchem Ausrückebereich sie gehört — der Name, nicht die Kennung. Gerechnet
     * und nicht eingestellt: der nähere von Leitstelle und Zweigstelle.
     */
    val bereich: String = "",
    /** Die Hebebühnen — nur bei der Werkstatt, sonst beide 0. */
    val buehnen: Int = 0,
    val buehnenBelegt: Int = 0,
)

/**
 * Eine Station eines Streifenpfads — ein Ort mit Namen.
 *
 * Namen und keine Koordinaten allein: Eine Streife fährt zwischen Orten, die es
 * wirklich gibt, damit im Funkprotokoll ein Name steht und nicht ein Zahlenpaar.
 */
@Serializable
data class WeltStreifenstation(
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

/** Eine Ecke eines aufgezogenen Geländes. */
@Serializable
data class WeltEcke(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

/**
 * Ein selbst gesetzter Punkt auf der Weltkarte.
 *
 * <b>Zwei Schalter, und sie meinen Verschiedenes.</b> `sichtbar` gehört der Karte,
 * `aktiv` dem Geschehen: Erst damit entstehen hier Lagen.
 */
@Serializable
data class Weltpoi(
    val id: String = "",
    val name: String = "",
    /** Die Objektart (`Objektart` im Web). */
    val art: String = "",
    /** Die Bezeichnung der Art in Klartext — gerechnet auf dem Server. */
    val artText: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val flaeche: Double = 0.0,
    /** Wie viele Menschen bei einem Schadenereignis darin wären. */
    val betroffene: Int = 0,
    val sichtbar: Boolean = true,
    val aktiv: Boolean = true,
    /** Wie oft hier eine Lage entsteht, 1 bis 5. */
    val gewicht: Int = 1,
    /** Die von Hand gesetzte Personenzahl; null heißt „aus der Grundfläche gerechnet". */
    val betroffeneGesetzt: Int? = null,
    /** Ob nachts (22–6 Uhr) jemand darin ist. */
    val nachtsBelegt: Boolean = false,
    /** Eigene Einsatzplan-Zeilen; leer heißt: die der Art. */
    val hinweise: List<String> = emptyList(),
    /** Welche Organisationen hier Lagen bekommen; leer heißt alle. */
    val organisationen: List<String> = emptyList(),
    /** Der Palettenton der Marke; 0 ist „automatisch". */
    val farbe: Int = 0,
    /** Die Ecken des aufgezogenen Geländes — leer heißt: Der Punkt ist ein Punkt. */
    val gelaende: List<WeltEcke> = emptyList(),
    /** Wald, Feld, Autobahn, Schiene, Gewaesser — null heißt Gebäudegrundstück. */
    val gelaendeart: String? = null,
    /** Ob `gelaende` eine Strecke ist statt einer Fläche. */
    val strecke: Boolean = false,
    /** Die Länge der Strecke in Metern; 0 bei einer Fläche. */
    val laenge: Double = 0.0,
)

/** Eine gespeicherte Streifenroute — einmal angelegt, auf beliebige Wagen zu legen. */
@Serializable
data class WeltStreifenroute(
    val id: String = "",
    val name: String = "",
    val stationen: List<WeltStreifenstation> = emptyList(),
)

/**
 * Ein Fahrzeug im eigenen Bestand.
 *
 * `lat`/`lon` ist der Ort dieser Sekunde — die Weltuhr fährt jede Fahrt selbst ab.
 * Zwischen zwei Abrufen fährt die Karte von dort aus weiter: `routeIndex` sagt, wo
 * auf der Route sie steht, `tempoMs`, wie schnell (siehe `Weltfahrt.fahrzeugOrt`).
 */
@Serializable
data class WeltFahrzeug(
    val id: String = "",
    val wacheId: String = "",
    val vorlageId: String = "",
    val typ: String = "",
    val funkrufname: String = "",
    /** `Wache`, `Ausrueckt`, `Anfahrt`, `VorOrt`, `Bereitstellung`, `Streife`, `Rueckfahrt`. */
    val lage: String = "Wache",
    val status: Int = 2,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val zielLat: Double = 0.0,
    val zielLon: Double = 0.0,
    val losUm: String? = null,
    val ankunftUm: String? = null,
    val lageId: String? = null,
    val preis: Int = 0,
    /** Ob der Rufname von Hand gesetzt wurde — nur dann gibt es etwas zurückzunehmen. */
    val rufnameVonHand: Boolean = false,
    /** Die gefahrene Strecke als `lat,lon;lat,lon;…` — null ohne Route. */
    val route: String? = null,
    /** Ob die Marke blinkt — Anfahrt zu einer Lage, sonst nicht. */
    val blaulicht: Boolean = false,
    /** Die Woche der Großlage, in deren Bereitstellungsraum es fährt oder steht. */
    val zielGrosslage: Int? = null,
    /** Die Wache, zu der es gerade umzieht; null bei jeder anderen Fahrt. */
    val zielWache: String? = null,
    /** Auf welchen Stützpunkt der Route gerade zugefahren wird. */
    val routeIndex: Int = 0,
    /** Mit wie vielen Metern je Sekunde. 0 heißt: Es steht. */
    val tempoMs: Double = 0.0,
    /** Was die Besatzung in Lehrgängen dazugelernt hat. */
    val gelernt: List<String> = emptyList(),
    /** Ob dieses Fahrzeug nur geliehen ist. */
    val geliehen: Boolean = false,
    /** Wann die Leihe endet — nur bei `geliehen` gesetzt. */
    val geliehenBis: String? = null,
    /** Die Lage, zu der es fährt, sobald es hier fertig ist — der Anschlussauftrag. */
    val naechsteLageId: String? = null,
    /** Der laufende Lehrgang, sonst `null`. */
    val lehrgang: String? = null,
    /** Wann er fertig ist; `null`, solange das Fahrzeug noch hinfährt. */
    val lehrgangFertigUm: String? = null,
    /** An welcher Einrichtung — der Name der Wache. */
    val lehrgangOrt: String? = null,
    /** Der Zustand in Prozent — 100 ist fabrikneu. */
    val zustand: Int = 100,
    /** Ob es unter der Grenze steht und damit nicht mehr alarmiert werden kann. */
    val verschlissen: Boolean = false,
    /** Ob die Liste „bald fällig" sagen soll. */
    val werkstattFaellig: Boolean = false,
    /** Ob es gerade in der Werkstatt steht oder dorthin fährt. */
    val inWerkstatt: Boolean = false,
    /** Wann die Instandsetzung fertig ist; `null`, solange es noch hinfährt. */
    val werkstattFertigUm: String? = null,
    /** In welcher Werkstatt — der Name. */
    val werkstattOrt: String? = null,
    /** Was eine Instandsetzung jetzt kosten würde; 0 bei einem Fahrzeug in Ordnung. */
    val reparaturpreis: Int = 0,
    /** Ob dieses Fahrzeug überhaupt Streife fahren kann. */
    val streifenfaehig: Boolean = false,
    /** Der gesetzte Streifenpfad; leer heißt: keine Streife. */
    val streifenpfad: List<WeltStreifenstation> = emptyList(),
)

/** Ein Lehrgang, wie er in der Auswahl steht. */
@Serializable
data class WeltLehrgang(
    val id: String = "",
    val name: String = "",
    /** Was die Besatzung danach kann. */
    val faehigkeit: String = "",
    val fuer: String = "",
    val kosten: Int = 0,
    val dauerMinuten: Int = 0,
    /** Warum dieser Lehrgang hier nicht infrage kommt — `null`, wenn er es tut. */
    val grund: String? = null,
    /** Ob diese Besatzung ihn schon besucht hat. */
    val gelernt: Boolean = false,
)

/**
 * Die Fahrzeugauswahl einer Lage — was der Server anbietet, was die Lage fordert, und
 * das Papier (`token`), mit dem sich daraus ein Alarm machen lässt.
 */
@Serializable
data class WeltAuswahl(
    /** Das Handlungs-Token. Es gehört an den folgenden Alarm und gilt einmal. */
    val token: String = "",
    /** Wie lange das Token gilt, in Sekunden. */
    val gueltigSekunden: Double = 0.0,
    val gefordertFahrzeuge: Int = 0,
    /** Was die Lage verlangt — die Auskunft, die es nur hier gibt. */
    val gefordertFaehigkeiten: List<String> = emptyList(),
    /** Fahrzeuge, die jetzt losfahren. */
    val sofort: List<WeltFahrzeug> = emptyList(),
    /** Und die, die fahren, sobald ihre Arbeit an der aktuellen Stelle getan ist. */
    val vormerkbar: List<WeltFahrzeug> = emptyList(),
    /** Die Züge — ihre Fahrzeuge gehören zur selben gebundenen Menge. */
    val zuege: List<WeltZug> = emptyList(),
)

/** Eine Werkstatt, wie sie in der Auswahl steht. */
@Serializable
data class WeltWerkstatt(
    val id: String = "",
    val name: String = "",
    val buehnen: Int = 0,
    val belegt: Int = 0,
    val entfernungMeter: Double = 0.0,
    /** Der Satz, warum es hier nicht geht; `null`, wenn es geht. */
    val beanstandung: String? = null,
)

/** Was zur Instandsetzung eines Fahrzeugs zu sagen ist. */
@Serializable
data class WeltWerkstattwahl(
    val zustand: Int = 100,
    val preis: Int = 0,
    val dauerMinuten: Int = 0,
    val werkstaetten: List<WeltWerkstatt> = emptyList(),
)

/** Eine Zeile der Chronik — der Text kommt fertig vom Server. */
@Serializable
data class WeltChronikzeile(
    val id: Long = 0,
    /** `Gegruendet`, `WacheGebaut`, `FahrzeugGekauft`, … */
    val art: String = "",
    val wacheId: String? = null,
    val text: String = "",
    val um: String = "",
)

/** Ein Platz eines Zuges — die Aufgabe, was sie verlangt, und wer ihn besetzt. */
@Serializable
data class WeltZugplatz(
    val bezeichnung: String = "",
    val faehigkeit: String = "",
    val pflicht: Boolean = false,
    val beispiel: String? = null,
    val fahrzeugId: String? = null,
    val fahrzeug: String? = null,
)

/** Ein aufgestellter Zug. Die Besetzung rechnet der Server. */
@Serializable
data class WeltZug(
    val id: String = "",
    val name: String = "",
    val vorgabeId: String = "",
    val wacheId: String = "",
    val plaetze: List<WeltZugplatz> = emptyList(),
    val vollstaendig: Boolean = false,
    /** Die Pflichtplätze, die niemand besetzen konnte — im Klartext. */
    val offen: List<String> = emptyList(),
    /** Alle Fahrzeuge des Zuges, auch die auf keinem Platz. */
    val fahrzeugIds: List<String> = emptyList(),
)

/** Eine Zugart, die sich aufstellen lässt. */
@Serializable
data class WeltZugvorgabe(
    val id: String = "",
    val name: String = "",
    val plaetze: List<WeltZugplatz> = emptyList(),
)

/** Die Seite einer einzelnen Wache: Hof, Züge, Chronik. */
@Serializable
data class WeltWachenseite(
    val wache: WeltWache = WeltWache(),
    val bereich: WeltBereich = WeltBereich(),
    val fahrzeuge: List<WeltFahrzeug> = emptyList(),
    val chronik: List<WeltChronikzeile> = emptyList(),
    val zuege: List<WeltZug> = emptyList(),
    val zugvorgaben: List<WeltZugvorgabe> = emptyList(),
    /** An wie vielen Lagen die Fahrzeuge dieser Wache eingetroffen sind. */
    val einsaetze: Int = 0,
    /** Der Dateiname des freigegebenen Kontobildes — `null`, wenn keines gezeigt wird. */
    val profilbild: String? = null,
)

/** Eine Nachricht im Chat der Welt. */
@Serializable
data class WeltChatzeile(
    val id: Long = 0,
    /** `Allgemein` oder `Direkt`. */
    val kanal: String = "Allgemein",
    val vonId: String = "",
    val von: String? = null,
    val vonBenutzername: String? = null,
    val anId: String? = null,
    val an: String? = null,
    val text: String = "",
    val um: String = "",
    /** Ob sie von einem selbst stammt — gerechnet vom Server. */
    val eigen: Boolean = false,
    val gelesen: Boolean = false,
)

/** Der Chat beim Öffnen: der Verlauf und wie viel Post ungelesen ist. */
@Serializable
data class WeltChatstand(
    val nachrichten: List<WeltChatzeile> = emptyList(),
    val ungelesen: Int = 0,
    val maxLaenge: Int = 280,
    /** Ob das Funkgerät standardmäßig an ist — aus der Domäne. */
    val funkgeraetVorgabe: Boolean = false,
)

/** Ein fremdes Fahrzeug auf der gemeinsamen Karte — schlanker als das eigene. */
@Serializable
data class WeltFremdfahrzeug(
    val id: String = "",
    val besitzer: String? = null,
    val vorlageId: String = "",
    val typ: String = "",
    val funkrufname: String = "",
    val lage: String = "Anfahrt",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val zielLat: Double = 0.0,
    val zielLon: Double = 0.0,
    val losUm: String? = null,
    val ankunftUm: String? = null,
    val route: String? = null,
    val blaulicht: Boolean = false,
    val routeIndex: Int = 0,
    val tempoMs: Double = 0.0,
)

/** Eine fremde Wache — mit der Zahl der Fahrzeuge, die dort stehen. */
@Serializable
data class WeltFremdwache(
    val id: String = "",
    val besitzer: String? = null,
    val name: String = "",
    val art: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val fahrzeuge: Int = 0,
)

/**
 * Eine Lage auf der gemeinsamen Karte.
 *
 * Sie gehört niemandem: `eigene` sagt nur, ob *ich* zuerst alarmiert habe, und
 * `meineFahrzeuge`, wie viele von mir darauf laufen.
 */
@Serializable
data class WeltLage(
    val id: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val adresse: String = "",
    val ort: String = "",
    val stichwort: String = "",
    val prioritaet: Int = 1,
    val gefordertFahrzeuge: Int = 0,
    /** <b>Wie viele</b> Fähigkeiten diese Lage verlangt — nicht mehr, welche. */
    val gefordertFaehigkeiten: Int = 0,
    /**
     * Wie viele Fahrzeuge hier eingetroffen sind. <b>Nullbar</b>, obwohl der Server es
     * immer schicken soll: Am 08.09.2026 fehlte das Feld auf der Beta, und an jeder
     * Lage stand `undefined/3` (siehe `deckung.ts`). `null` heißt „unbekannt", nie 0.
     */
    val eingetroffen: Int? = null,
    /** `Offen`, `Laeuft`, `Erledigt`, `Verfallen`. */
    val zustand: String = "Offen",
    val verguetung: Int = 0,
    val entstandenUm: String = "",
    val verfaelltUm: String = "",
    val eigene: Boolean = false,
    val meineFahrzeuge: Int = 0,
    val ausGrosslage: Boolean = false,
    val entfernungMeter: Double = 0.0,
    /** Ob die Lage nachgefordert hat — „mehr, als gemeldet". */
    val nachgefordert: Boolean = false,
    /** Ob sie für meine Leitstelle gewürfelt wurde — nur ich kann sie freigeben. */
    val zustaendig: Boolean = false,
    /** Ob sie für alle offensteht. */
    val freigegeben: Boolean = false,
    /** Was zur Deckung noch fehlt, in einem Satz — `null` heißt „nichts mehr". */
    val fehlt: String? = null,
    /** Der Event-Einsatz, zu dem diese Lage gehört; `null` bei jeder anderen. */
    val eventId: String? = null,
    /** Wann das letzte eigene Fahrzeug hier fertig ist; `null`, wenn keines arbeitet. */
    val arbeitBis: String? = null,
    /** Ob dies die Tageslage ist — die eine größere je Tag, neun bis zwanzig Uhr. */
    val istTageslage: Boolean = false,
    /** Die Organisation des Stichworts — `null` heißt „keine Einschränkung". */
    val organisation: String? = null,
    /** Der Ausrückebereich, zu dem diese Lage gehört — der Name. */
    val bereich: String = "",
)

// ------------------------------------------------------------------ Leihmarkt

/** Ein Angebot im Leihmarkt, wie es in der Liste steht. */
@Serializable
data class WeltLeihangebot(
    val id: String = "",
    val fahrzeugId: String = "",
    val funkrufname: String = "",
    val typ: String = "",
    val von: String? = null,
    val vonBenutzername: String? = null,
    val vonMir: Boolean = false,
    val tage: Int = 0,
    val preis: Int = 0,
    val ort: String = "",
    val entfernungKm: Double = 0.0,
    val angebotenUm: String = "",
    val verfuegbar: Boolean = false,
    /** Die Katalogvorlage des Fahrzeugs — für die passende Zielwache beim Mieten. */
    val vorlageId: String? = null,
)

/** Ein Gesuch im Leihmarkt — jemand braucht ein Fahrzeug dieser Art. */
@Serializable
data class WeltGesuch(
    val id: String = "",
    /** Die Fahrzeugkategorie aus dem Katalog, z. B. „Krankentransport". */
    val kategorie: String = "",
    val tage: Int = 0,
    val preis: Int = 0,
    val von: String? = null,
    val vonBenutzername: String? = null,
    val vonMir: Boolean = false,
    /** Wohin das Fahrzeug fahren soll — der Name der Zielwache. */
    val wache: String = "",
    val entfernungKm: Double = 0.0,
    val erstelltUm: String = "",
)

/** Ein laufender Verleih — in beide Richtungen dieselbe Zeile. */
@Serializable
data class WeltLeihfahrzeug(
    val fahrzeugId: String = "",
    val funkrufname: String = "",
    val typ: String = "",
    /** Die Gegenseite: bei „verliehen" der Empfänger, bei „geliehen" der Besitzer. */
    val gegenueber: String? = null,
    val wache: String = "",
    val bis: String? = null,
    val gegenueberBenutzername: String? = null,
)

/** Der ganze Leihstand — die vier Listen der beiden Reiter. */
@Serializable
data class WeltLeihstand(
    val eingestellt: List<WeltLeihangebot> = emptyList(),
    val verliehen: List<WeltLeihfahrzeug> = emptyList(),
    val geliehen: List<WeltLeihfahrzeug> = emptyList(),
    val leihbar: List<WeltLeihangebot> = emptyList(),
    /** Je Fahrzeug, was eine Leihe davon höchstens kosten darf. */
    val hoechstpreise: Map<String, Int> = emptyMap(),
    val meineGesuche: List<WeltGesuch> = emptyList(),
    val gesuche: List<WeltGesuch> = emptyList(),
    /** Je Fahrzeugkategorie, was ein Gesuch höchstens bieten darf. */
    val gesuchshoechstpreise: Map<String, Int> = emptyMap(),
)

// --------------------------------------------------------------- Einstellungen

/**
 * Die eigene Gangart: `Ruhig`, `Normal`, `Fordernd` oder `Eigen` — und drei Schrauben
 * in Prozent der Vorgabe. `lohnProzent` kommt vom Server.
 */
@Serializable
data class WeltEinstellung(
    val gangart: String = "Normal",
    val dichte: Int = 100,
    val nachschub: Int = 100,
    val arbeitszeit: Int = 100,
    val lohnProzent: Int = 100,
    val kleinstes: Int = 50,
    val groesstes: Int = 200,
)

/** Ein Icon-Pack in der Liste der Einstellungen. */
@Serializable
data class WeltIconpack(
    val id: String = "",
    val name: String = "",
    val code: String? = null,
    val belegt: Int = 0,
    val bytes: Long = 0,
    val gesperrt: Boolean = false,
    /** Ob dieses Pack gerade auf der Karte gilt. */
    val aktiv: Boolean = false,
    val geaendertUm: String = "",
)

/**
 * Ein Blaulicht auf einem eigenen Icon — Mittelpunkt in 0–1 auf dem Bild. Alles außer
 * der Position ist optional, und `null` heißt immer „die Vorgabe".
 */
@Serializable
data class WeltBlaulicht(
    val x: Double = 0.5,
    val y: Double = 0.5,
    val b: Double? = null,
    val h: Double? = null,
    /** `rund` oder `eckig`. */
    val form: String? = null,
    /** `doppel`, `vierfach` oder `rundum`. */
    val art: String? = null,
    /** `a` oder `b` — `b` blitzt eine halbe Periode nach `a`. */
    val takt: String? = null,
    /** `blau` oder `gelb`. */
    val farbe: String? = null,
)

/** Ein einzelnes Icon eines Packs — `bild` ist eine Adresse oder eine Daten-URL. */
@Serializable
data class WeltPackicon(
    val vorlageId: String = "",
    val bild: String = "",
    val breite: Int? = null,
    val hoehe: Int? = null,
    /** Ob das Icon sich mit dem Kurs des Fahrzeugs dreht — Vorgabe nein. */
    val dreht: Boolean = false,
    /** Die Blaulichter — leer heißt: Es blitzt das ganze Bild. */
    val blaulichter: List<WeltBlaulicht> = emptyList(),
)

// ------------------------------------------------------------------- Laufbahn

/** Eine Stufe der Laufbahn — die ganze Leiter kommt fertig vom Server. */
@Serializable
data class WeltLaufbahnstufe(
    val nummer: Int = 0,
    /** Ab welcher Gesamterfahrung sie erreicht ist. */
    val abErfahrung: Int = 0,
    /** Wie viele Wachen auf ihr stehen dürfen. */
    val wachendeckel: Int = 0,
    /** Was genau auf dieser Stufe aufgeht — meist nichts. */
    val schaltetFrei: List<String> = emptyList(),
)

/** Was ab welcher Stufe offen ist — und ob es das schon ist. */
@Serializable
data class WeltFreischaltungsstand(
    val was: String = "",
    val abStufe: Int = 0,
    val frei: Boolean = false,
)

/** Eine Bauart mit Preis und Stellplätzen, damit der Bauknopf nichts rechnen muss. */
@Serializable
data class WeltBauart(
    val art: String = "",
    val name: String = "",
    val preis: Int = 0,
    val stellplaetze: Int = 0,
    val frei: Boolean = false,
)

/** Ein Ausrückebereich — Leitstelle oder Zweigstelle, für die Karte dasselbe. */
@Serializable
data class WeltBereich(
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val kreis: String? = null,
    val istZweigstelle: Boolean = false,
    /** Der Bauradius in Metern — die Karte zeichnet den Kreis daraus. */
    val bauradiusMeter: Double = 0.0,
)

/** Was über die Zweigstelle zu sagen ist — auch dann, wenn es noch keine gibt. */
@Serializable
data class WeltZweigstellenstand(
    /** Die erste Zweigstelle; `null`, wenn es noch keine gibt. */
    val zweigstelle: WeltBereich? = null,
    /** Ab welcher Stufe die **nächste** offen ist. */
    val abStufe: Int = 0,
    val preis: Int = 0,
    /** Ob gerade noch eine gegründet werden darf. */
    val frei: Boolean = false,
    /** Der Mindestabstand zum nächstgelegenen eigenen Bereich, in Metern. */
    val mindestabstandMeter: Double = 0.0,
    /** Wie viele schon stehen. */
    val schon: Int = 0,
    /** Wie viele auf dieser Stufe erlaubt sind. */
    val hoechstens: Int = 0,
)

/** Der Besitzstand: was mir gehört und was ich darf. */
@Serializable
data class Weltstand(
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val kreis: String? = null,
    val guthaben: Int = 0,
    val erfahrung: Int = 0,
    val stufe: Int = 1,
    val erfahrungStufeAb: Int = 0,
    val erfahrungStufeBis: Int? = null,
    val wachendeckel: Int = 0,
    val wachen: List<WeltWache> = emptyList(),
    val fahrzeuge: List<WeltFahrzeug> = emptyList(),
    val freischaltungen: List<WeltFreischaltungsstand> = emptyList(),
    val bauarten: List<WeltBauart> = emptyList(),
    /** Der Weltpreis je Fahrzeugvorlage — für die Kaufliste. */
    val fahrzeugpreise: Map<String, Int> = emptyMap(),
    /** Die eigenen Ausrückebereiche — Hauptbereich zuerst. */
    val bereiche: List<WeltBereich> = emptyList(),
    /** Was zur Zweigstelle zu sagen ist — auch, wenn es noch keine gibt. */
    val zweigstelle: WeltZweigstellenstand? = null,
)

/** Der laufende Betrieb: was gerade auf der Karte los ist. */
@Serializable
data class Weltbetrieb(
    val lagen: List<WeltLage> = emptyList(),
    val fahrzeuge: List<WeltFahrzeug> = emptyList(),
    /** Die fremden Fahrzeuge — alle, die gerade unterwegs sind. */
    val fremde: List<WeltFremdfahrzeug> = emptyList(),
    /** Die Wachen der anderen. */
    val fremdeWachen: List<WeltFremdwache> = emptyList(),
    val guthaben: Int = 0,
    val erfahrung: Int = 0,
    val stufe: Int = 1,
    /** Was vom Kredit noch offen ist — 0, wenn keiner läuft. */
    val restschuld: Int = 0,
    val erfahrungStufeAb: Int = 0,
    /** Auf der höchsten Stufe null — dort ist nichts mehr zu füllen. */
    val erfahrungStufeBis: Int? = null,
    /** Serverzeit — der Client rechnet daraus seinen Uhrenversatz. */
    val serverzeit: String = "",
    /** Die Ausrückebereiche — Hauptbereich zuerst, dahinter die Zweigstellen. */
    val bereiche: List<WeltBereich> = emptyList(),
)

// ----------------------------------------------------------- Großlage, Events

/** Eine Zeile des Bedarfsblatts am Bereitstellungsraum. */
@Serializable
data class WeltBedarf(
    val faehigkeit: String = "",
    val fehlt: Int = 0,
    val imRaum: Int = 0,
)

/** Eine Zeile der Voranforderung — was die Wellen anfordern werden. */
@Serializable
data class WeltAnforderung(
    val faehigkeit: String = "",
    val imRaum: Int = 0,
)

/** Das Ergebnis eines vergangenen Großeinsatzes. */
@Serializable
data class WeltGrosslageErgebnis(
    val woche: Int = 0,
    val name: String = "",
    val ort: String = "",
    val geschafft: Boolean = false,
    val gestellt: Int = 0,
    val bedarf: Int = 0,
    val bonus: Int = 0,
)

/** Die Großlage der Woche, wie sie im Fenster steht. */
@Serializable
data class Weltgrosslage(
    val woche: Int = 0,
    val name: String = "",
    val ort: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val beginntUm: String = "",
    val endetUm: String = "",
    /** `Angekuendigt`, `Laeuft`, `Vorbei`. */
    val zustand: String = "Angekuendigt",
    val bedarf: Int = 0,
    val gestellt: Int = 0,
    val offeneLagen: Int = 0,
    val beteiligt: Boolean = false,
    val bereitgestellt: Int = 0,
    val meineBereitgestellt: Int = 0,
    val anfahrtOffen: Boolean = false,
    val abschlussbonus: Int = 0,
    val vorwoche: WeltGrosslageErgebnis? = null,
    val gestellteKraefte: Int = 0,
    val bereitgestellteKraefte: Int = 0,
    val meineBereitgestelltenKraefte: Int = 0,
    val wellenStand: Int = 0,
    val wellen: Int = 0,
    val naechsteWelleUm: String? = null,
    val bedarfe: List<WeltBedarf> = emptyList(),
    val offenePlaetze: Int = 0,
    val angefordert: List<WeltAnforderung> = emptyList(),
)

/** Ein Event-Einsatz der World, wie ihn das Spiel sieht. */
@Serializable
data class WeltEvent(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String? = null,
    val bannerUrl: String? = null,
    val hintergrundUrl: String? = null,
    val farbe: String? = null,
    val beginntUm: String = "",
    val endetUm: String = "",
    /** `Entwurf`, `Angekuendigt`, `Laeuft`, `Vorbei`. */
    val zustand: String = "Angekuendigt",
    val ort: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val offeneLagen: Int = 0,
    val brName: String? = null,
    val brLat: Double? = null,
    val brLon: Double? = null,
    val brOffenAb: String? = null,
    val raumOffen: Boolean = false,
    val autoAbarbeiten: Boolean = false,
    val bereitgestellt: Int = 0,
    val meineBereitgestellt: Int = 0,
    val bedarfe: List<WeltBedarf> = emptyList(),
    val offenePlaetze: Int = 0,
    val angefordert: List<WeltAnforderung> = emptyList(),
)

/** Ein Wochenziel mit dem eigenen Stand darin. */
@Serializable
data class WeltZiel(
    val index: Int = 0,
    val titel: String = "",
    val zielwert: Int = 0,
    val stand: Int = 0,
    val belohnung: Int = 0,
    val erfuellt: Boolean = false,
)

/** Die drei Wochenziele samt eigenem Fortschritt. */
@Serializable
data class WeltZiele(
    val woche: Int = 0,
    val ziele: List<WeltZiel> = emptyList(),
)

// ---------------------------------------------------------------------- Kasse

/** Eine Zeile des Kassenblatts. */
@Serializable
data class WeltBuchung(
    /** `Gruendung`, `Lage`, `Erstzugriff`, … (`Weltgrund` im Web). */
    val grund: String = "",
    val betrag: Int = 0,
    val standDanach: Int = 0,
    val um: String = "",
    /** Worum es ging — `null` bei Gründen, die für sich sprechen. */
    val vermerk: String? = null,
)

/** Eine Summe je Grund — „woran verdiene ich eigentlich?". */
@Serializable
data class WeltSumme(
    val grund: String = "",
    val summe: Int = 0,
)

/** Ein Kredit, wie er im Fenster zur Auswahl steht. */
@Serializable
data class WeltKreditangebot(
    val betrag: Int = 0,
    val zinsprozent: Int = 0,
    val rueckzahlung: Int = 0,
    val abStufe: Int = 0,
    val frei: Boolean = false,
)

/** Der laufende Kredit. */
@Serializable
data class WeltKredit(
    val betrag: Int = 0,
    val zinsprozent: Int = 0,
    val rueckzahlung: Int = 0,
    val restschuld: Int = 0,
    val getilgt: Int = 0,
    val tilgungProzent: Int = 0,
    val aufgenommenUm: String = "",
)

/** Das Kassenblatt: Summen je Grund, die jüngsten Buchungen und der Kredit. */
@Serializable
data class WeltKassenblatt(
    val guthaben: Int = 0,
    val summenSiebenTage: List<WeltSumme> = emptyList(),
    val summenGesamt: List<WeltSumme> = emptyList(),
    val buchungen: List<WeltBuchung> = emptyList(),
    val kredit: WeltKredit? = null,
    val kreditangebote: List<WeltKreditangebot> = emptyList(),
)

// ------------------------------------------------------------------ Rangliste

/** Eine Zeile der Rangliste. */
@Serializable
data class WeltRang(
    val platz: Int = 0,
    val name: String = "",
    val leitstelle: String = "",
    val stufe: Int = 0,
    val credits: Int = 0,
    val lagenGedeckt: Int = 0,
    val wochensiege: Int = 0,
    val benutzername: String? = null,
)

/** Eine abgeschlossene Woche und ihr Sieger. */
@Serializable
data class WeltWochensieg(
    val woche: Int = 0,
    val name: String = "",
    val leitstelle: String = "",
    val credits: Int = 0,
    val lagenGedeckt: Int = 0,
    val preisgeld: Int = 0,
    val benutzername: String? = null,
)

/** Die Rangliste — die Woche und „seit je", nach verdienten Welt-Credits. */
@Serializable
data class WeltRangliste(
    val woche: Int = 0,
    val dieseWoche: List<WeltRang> = emptyList(),
    val gesamt: List<WeltRang> = emptyList(),
    val meinPlatzWoche: Int? = null,
    val meinPlatzGesamt: Int? = null,
    val letzteWoche: WeltWochensieg? = null,
    val meineWochensiege: Int = 0,
)

// ------------------------------------------------------------ Kleine Antworten

/** `{ ok }` — die Antwort der meisten Aktionen. */
@Serializable
data class WeltOk(val ok: Boolean = true)

/** `{ alarmiert, vorgemerkt }` — die Antwort auf einen Alarm. */
@Serializable
data class WeltAlarmantwort(
    val alarmiert: Int = 0,
    val vorgemerkt: Int = 0,
)

/** `{ losgefahren, vormerkungen }` — die Antwort auf das Entlassen aus einer Lage. */
@Serializable
data class WeltEinrueckantwort(
    val losgefahren: Int = 0,
    val vormerkungen: Int = 0,
)

/** `{ losgeschickt }` — die Antwort auf ein Bereitstellen. */
@Serializable
data class WeltBereitstellantwort(val losgeschickt: Int = 0)

/** `{ name }` — wie ein frei gesetzter Punkt heißt. */
@Serializable
data class WeltOrtsname(val name: String = "")

/** `{ id }` — der neu aufgestellte Zug. */
@Serializable
data class WeltNeueId(val id: String = "")

/** `{ abgehakt }` — wie viel Post als gelesen abgehakt wurde. */
@Serializable
data class WeltAbgehakt(val abgehakt: Int = 0)
