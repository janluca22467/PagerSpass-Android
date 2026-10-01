package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * PagerSpass - World — übertragen aus `web/src/types.ts` (Abschnitt „World“).
 *
 * <b>Es gibt die Welt genau einmal, sie läuft immer</b>, und alle Premium-Spieler
 * stehen auf derselben Karte. Deshalb gibt es hier keinen Raumcode und keinen
 * Vollstand über den Hub: Was der Arbeitsplatz braucht, sind drei Antworten —
 * der Besitzstand (ändert sich beim Kaufen), der Betrieb (alle fünf Sekunden)
 * und die Großlage der Woche. Der Hub sagt nur, *dass* sich etwas geändert hat.
 *
 * <b>Jedes Feld hat einen Vorgabewert.</b> Die Welt wächst auf dem Server
 * schneller als hier; ein Feld, das eine ältere API noch nicht schickt, darf den
 * Arbeitsplatz nicht umwerfen. Am 08.09.2026 stand im Web an jeder Lage
 * `undefined/3`, weil eine neue Oberfläche auf eine alte API traf — die Zahl
 * `eingetroffen` ist deshalb hier nullbar und nicht `0` (siehe `deckung.ts`).
 */

/** Eine Leitstelle auf der gemeinsamen Karte. */
@Serializable
data class Weltleitstelle(
    val besitzerId: String = "",
    /** `null`, wenn der Besitzer sich in der Welt nicht zeigt. */
    val besitzer: String? = null,
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val kreis: String? = null,
    val stufe: Int? = null,
    val wachen: Int = 0,
    val gegruendet: String = "",
)

/** Eine gebaute Wache im eigenen Bestand. */
@Serializable
data class WeltWache(
    val id: String = "",
    val name: String = "",
    val art: String = "Feuerwehrhaus",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val stellplaetze: Int = 0,
    val belegt: Int = 0,
    val preis: Int = 0,
    val ausbaustufe: Int = 0,
    val ausbauFertigUm: String? = null,
    val ausbauPreis: Int? = null,
    val ausbauDauerMinuten: Int? = null,
    /** Der Name der Bauart — er kommt vom Server und wird hier nicht gebaut. */
    val bauart: String = "",
    val wappenZeichen: String = "Keines",
    val wappenFarbe: Int = 0,
    val fotoZeigen: Boolean = false,
    /** Der Ausrückebereich — der Name, gerechnet auf dem Server. */
    val bereich: String = "",
    val buehnen: Int = 0,
    val buehnenBelegt: Int = 0,
    /**
     * In welchem Staat sie steht (seit PagerSpass 6) — er entscheidet, aus
     * welchem Katalog an ihr gekauft wird. Ein älterer Server schickt nichts:
     * dann Deutschland, wie bisher überall.
     */
    val staat: String = "Deutschland",
)

/** Eine Station eines Streifenpfads — ein Ort mit Namen. */
@Serializable
data class WeltStreifenstation(
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

@Serializable
data class WeltPunkt(val lat: Double = 0.0, val lon: Double = 0.0)

/**
 * Ein selbst gesetzter Punkt auf der Weltkarte.
 *
 * <b>Zwei Schalter, und sie meinen Verschiedenes.</b> `sichtbar` gehört der
 * Karte, `aktiv` dem Geschehen: Erst damit entstehen hier Lagen.
 */
@Serializable
data class Weltpoi(
    val id: String = "",
    val name: String = "",
    val art: String = "",
    val artText: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val flaeche: Double = 0.0,
    val betroffene: Int = 0,
    val sichtbar: Boolean = true,
    val aktiv: Boolean = true,
    val gewicht: Int = 3,
    val betroffeneGesetzt: Int? = null,
    val nachtsBelegt: Boolean = false,
    val hinweise: List<String> = emptyList(),
    val organisationen: List<String> = emptyList(),
    val farbe: Int = 0,
    val gelaende: List<WeltPunkt> = emptyList(),
    val gelaendeart: String? = null,
    val strecke: Boolean = false,
    val laenge: Double = 0.0,
)

/** Eine gespeicherte Streifenroute. */
@Serializable
data class WeltStreifenroute(
    val id: String = "",
    val name: String = "",
    val stationen: List<WeltStreifenstation> = emptyList(),
)

/**
 * Ein Fahrzeug im eigenen Bestand.
 *
 * `lat`/`lon` ist der Ort zum Zeitpunkt des Abrufs. Zwischen zwei Abrufen fährt
 * die Karte selbst weiter — `routeIndex` sagt, wo auf der Route, `tempoMs`, wie
 * schnell (siehe `Weltfahrt`).
 */
@Serializable
data class WeltFahrzeug(
    val id: String = "",
    val wacheId: String = "",
    val vorlageId: String = "",
    val typ: String = "",
    val funkrufname: String = "",
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
    val rufnameVonHand: Boolean = false,
    val route: String? = null,
    val blaulicht: Boolean = false,
    val zielGrosslage: Int? = null,
    val zielWache: String? = null,
    val routeIndex: Int = 0,
    val tempoMs: Double = 0.0,
    val gelernt: List<String> = emptyList(),
    val geliehen: Boolean = false,
    val geliehenBis: String? = null,
    val naechsteLageId: String? = null,
    val lehrgang: String? = null,
    val lehrgangFertigUm: String? = null,
    val lehrgangOrt: String? = null,
    val zustand: Int = 100,
    val verschlissen: Boolean = false,
    val werkstattFaellig: Boolean = false,
    val inWerkstatt: Boolean = false,
    val werkstattFertigUm: String? = null,
    val werkstattOrt: String? = null,
    val reparaturpreis: Int = 0,
    val streifenfaehig: Boolean = false,
    val streifenpfad: List<WeltStreifenstation> = emptyList(),
)

/** Ein Lehrgang, wie er in der Auswahl steht. */
@Serializable
data class WeltLehrgang(
    val id: String = "",
    val name: String = "",
    val faehigkeit: String = "",
    val fuer: String = "",
    val kosten: Int = 0,
    val dauerMinuten: Int = 0,
    /** Warum er nicht infrage kommt — `null`, wenn er es tut. */
    val grund: String? = null,
    val gelernt: Boolean = false,
)

/**
 * Die Fahrzeugauswahl einer Lage — samt dem Token, ohne das der Alarm
 * abgelehnt wird (siehe `Weltwache.TokenAbweisung` auf dem Server).
 */
@Serializable
data class WeltAuswahl(
    val token: String = "",
    val gueltigSekunden: Int = 120,
    val gefordertFahrzeuge: Int = 0,
    val gefordertFaehigkeiten: List<String> = emptyList(),
    val sofort: List<WeltFahrzeug> = emptyList(),
    val vormerkbar: List<WeltFahrzeug> = emptyList(),
    val zuege: List<WeltZug> = emptyList(),
)

@Serializable
data class WeltWerkstatt(
    val id: String = "",
    val name: String = "",
    val buehnen: Int = 0,
    val belegt: Int = 0,
    val entfernungMeter: Double = 0.0,
    val beanstandung: String? = null,
)

@Serializable
data class WeltWerkstattwahl(
    val zustand: Int = 100,
    val preis: Int = 0,
    val dauerMinuten: Int = 0,
    val werkstaetten: List<WeltWerkstatt> = emptyList(),
)

/** Eine Zeile der Chronik einer Wache — der Text kommt fertig vom Server. */
@Serializable
data class WeltChronikzeile(
    val id: Long = 0,
    val art: String = "",
    val wacheId: String? = null,
    val text: String = "",
    val um: String = "",
)

/** Ein Platz eines Zuges. */
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
    val offen: List<String> = emptyList(),
    val fahrzeugIds: List<String> = emptyList(),
)

@Serializable
data class WeltZugvorgabe(
    val id: String = "",
    val name: String = "",
    val plaetze: List<WeltZugplatz> = emptyList(),
)

@Serializable
data class WeltWachenseite(
    val wache: WeltWache = WeltWache(),
    val bereich: WeltBereich = WeltBereich(),
    val fahrzeuge: List<WeltFahrzeug> = emptyList(),
    val chronik: List<WeltChronikzeile> = emptyList(),
    val zuege: List<WeltZug> = emptyList(),
    val zugvorgaben: List<WeltZugvorgabe> = emptyList(),
    val einsaetze: Int = 0,
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
    val eigen: Boolean = false,
    val gelesen: Boolean = false,
)

@Serializable
data class WeltChatstand(
    val nachrichten: List<WeltChatzeile> = emptyList(),
    val ungelesen: Int = 0,
    val maxLaenge: Int = 300,
    val funkgeraetVorgabe: Boolean = false,
    /**
     * Wie lange eine eigene Zeile zurückgenommen werden kann (seit PagerSpass 6).
     * Ein älterer Server kennt das Zurücknehmen nicht und schickt nichts — dann
     * 0, und der Knopf erscheint nie.
     */
    val zuruecknahmeSekunden: Int = 0,
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
 * Eine Lage auf der gemeinsamen Karte. Sie gehört niemandem: `eigene` sagt nur,
 * ob *ich* zuerst alarmiert habe, und `meineFahrzeuge`, wie viele von mir
 * darauf laufen.
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
    /** Wie viele Fähigkeiten verlangt sind — welche, sagt erst die Auswahl. */
    val gefordertFaehigkeiten: Int = 0,
    /** Wie viele eingetroffen sind; `null`, wenn der Server es nicht schickt. */
    val eingetroffen: Int? = null,
    val zustand: String = "Offen",
    val verguetung: Int = 0,
    val entstandenUm: String = "",
    val verfaelltUm: String = "",
    val eigene: Boolean = false,
    val meineFahrzeuge: Int = 0,
    val ausGrosslage: Boolean = false,
    val entfernungMeter: Double = 0.0,
    val nachgefordert: Boolean = false,
    val zustaendig: Boolean = false,
    val freigegeben: Boolean = false,
    /** Was zur Deckung noch fehlt — `null` heißt „nichts mehr“. */
    val fehlt: String? = null,
    val eventId: String? = null,
    val arbeitBis: String? = null,
    val istTageslage: Boolean = false,
    val organisation: String? = null,
    val bereich: String = "",
)

/** Ein Angebot im Leihmarkt. */
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
    val verfuegbar: Boolean = true,
    val vorlageId: String? = null,
)

/** Ein Gesuch im Leihmarkt — die Nachfrageseite. */
@Serializable
data class WeltGesuch(
    val id: String = "",
    val kategorie: String = "",
    val tage: Int = 0,
    val preis: Int = 0,
    val von: String? = null,
    val vonBenutzername: String? = null,
    val vonMir: Boolean = false,
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
    val gegenueber: String? = null,
    val wache: String = "",
    val bis: String? = null,
    val gegenueberBenutzername: String? = null,
)

@Serializable
data class WeltLeihstand(
    val eingestellt: List<WeltLeihangebot> = emptyList(),
    val verliehen: List<WeltLeihfahrzeug> = emptyList(),
    val geliehen: List<WeltLeihfahrzeug> = emptyList(),
    val leihbar: List<WeltLeihangebot> = emptyList(),
    val hoechstpreise: Map<String, Int> = emptyMap(),
    val meineGesuche: List<WeltGesuch> = emptyList(),
    val gesuche: List<WeltGesuch> = emptyList(),
    val gesuchshoechstpreise: Map<String, Int> = emptyMap(),
)

/** Die eigene Gangart: drei Schrauben in Prozent der Vorgabe. */
@Serializable
data class WeltEinstellung(
    /** `Ruhig`, `Normal`, `Fordernd` oder `Eigen`. */
    val gangart: String = "Normal",
    val dichte: Int = 100,
    val nachschub: Int = 100,
    val arbeitszeit: Int = 100,
    val lohnProzent: Int = 100,
    val kleinstes: Int = 50,
    val groesstes: Int = 200,
)

@Serializable
data class WeltLaufbahnstufe(
    val nummer: Int = 0,
    val abErfahrung: Long = 0,
    val wachendeckel: Int = 0,
    val schaltetFrei: List<String> = emptyList(),
)

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
    val bauradiusMeter: Double = 35_000.0,
)

@Serializable
data class WeltZweigstellenstand(
    val zweigstelle: WeltBereich? = null,
    val abStufe: Int = 20,
    val preis: Int = 0,
    val frei: Boolean = false,
    val mindestabstandMeter: Double = 0.0,
    val schon: Int = 0,
    val hoechstens: Int = 0,
)

/** Der Besitzstand: was mir gehört und was ich darf. */
@Serializable
data class Weltstand(
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val kreis: String? = null,
    val guthaben: Long = 0,
    val erfahrung: Long = 0,
    val stufe: Int = 1,
    val erfahrungStufeAb: Long = 0,
    val erfahrungStufeBis: Long? = null,
    val wachendeckel: Int = 0,
    val wachen: List<WeltWache> = emptyList(),
    val fahrzeuge: List<WeltFahrzeug> = emptyList(),
    val freischaltungen: List<WeltFreischaltungsstand> = emptyList(),
    val bauarten: List<WeltBauart> = emptyList(),
    val fahrzeugpreise: Map<String, Int> = emptyMap(),
    val bereiche: List<WeltBereich> = emptyList(),
    val zweigstelle: WeltZweigstellenstand? = null,
    /** Der Staat der Leitstelle — die Flagge oben links im Kopf. */
    val staat: String = "Deutschland",
)

/** Der laufende Betrieb: was gerade auf der Karte los ist. */
@Serializable
data class Weltbetrieb(
    val lagen: List<WeltLage> = emptyList(),
    val fahrzeuge: List<WeltFahrzeug> = emptyList(),
    val fremde: List<WeltFremdfahrzeug> = emptyList(),
    val fremdeWachen: List<WeltFremdwache> = emptyList(),
    val guthaben: Long = 0,
    val erfahrung: Long = 0,
    val stufe: Int = 1,
    val restschuld: Long = 0,
    val erfahrungStufeAb: Long = 0,
    val erfahrungStufeBis: Long? = null,
    val serverzeit: String = "",
    val bereiche: List<WeltBereich> = emptyList(),
)

@Serializable
data class Weltbedarf(
    val faehigkeit: String = "",
    val fehlt: Int = 0,
    val imRaum: Int = 0,
)

@Serializable
data class Weltanforderung(
    val faehigkeit: String = "",
    val imRaum: Int = 0,
)

@Serializable
data class WeltgrosslageErgebnis(
    val woche: Int = 0,
    val name: String = "",
    val ort: String = "",
    val geschafft: Boolean = false,
    val gestellt: Int = 0,
    val bedarf: Int = 0,
    val bonus: Int = 0,
)

/** Die Großlage der Woche. */
@Serializable
data class Weltgrosslage(
    val woche: Int = 0,
    val name: String = "",
    val ort: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val beginntUm: String = "",
    val endetUm: String = "",
    /** `Angekuendigt`, `Laeuft` oder `Vorbei`. */
    val zustand: String = "Angekuendigt",
    val bedarf: Int = 0,
    val gestellt: Int = 0,
    val offeneLagen: Int = 0,
    val beteiligt: Boolean = false,
    val bereitgestellt: Int = 0,
    val meineBereitgestellt: Int = 0,
    val anfahrtOffen: Boolean = false,
    val abschlussbonus: Int = 0,
    val vorwoche: WeltgrosslageErgebnis? = null,
    val gestellteKraefte: Int = 0,
    val bereitgestellteKraefte: Int = 0,
    val meineBereitgestelltenKraefte: Int = 0,
    val wellenStand: Int = 0,
    val wellen: Int = 0,
    val naechsteWelleUm: String? = null,
    val bedarfe: List<Weltbedarf> = emptyList(),
    val offenePlaetze: Int = 0,
    val angefordert: List<Weltanforderung> = emptyList(),
)

/** Ein Event-Einsatz der World. */
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
    val bedarfe: List<Weltbedarf> = emptyList(),
    val offenePlaetze: Int = 0,
    val angefordert: List<Weltanforderung> = emptyList(),
)

@Serializable
data class WeltZiel(
    val index: Int = 0,
    val titel: String = "",
    val zielwert: Int = 0,
    val stand: Int = 0,
    val belohnung: Int = 0,
    val erfuellt: Boolean = false,
)

@Serializable
data class WeltZiele(
    val woche: Int = 0,
    val ziele: List<WeltZiel> = emptyList(),
)

@Serializable
data class WeltBuchung(
    val grund: String = "",
    val betrag: Long = 0,
    val standDanach: Long = 0,
    val um: String = "",
    val vermerk: String? = null,
)

@Serializable
data class WeltSumme(val grund: String = "", val summe: Long = 0)

@Serializable
data class WeltKreditangebot(
    val betrag: Long = 0,
    val zinsprozent: Int = 0,
    val rueckzahlung: Long = 0,
    val abStufe: Int = 0,
    val frei: Boolean = false,
)

@Serializable
data class WeltKredit(
    val betrag: Long = 0,
    val zinsprozent: Int = 0,
    val rueckzahlung: Long = 0,
    val restschuld: Long = 0,
    val getilgt: Long = 0,
    val tilgungProzent: Int = 0,
    val aufgenommenUm: String = "",
)

@Serializable
data class WeltKassenblatt(
    val guthaben: Long = 0,
    val summenSiebenTage: List<WeltSumme> = emptyList(),
    val summenGesamt: List<WeltSumme> = emptyList(),
    val buchungen: List<WeltBuchung> = emptyList(),
    val kredit: WeltKredit? = null,
    val kreditangebote: List<WeltKreditangebot> = emptyList(),
)

@Serializable
data class WeltRang(
    val platz: Int = 0,
    val name: String = "",
    val leitstelle: String = "",
    val stufe: Int = 0,
    val credits: Long = 0,
    val lagenGedeckt: Int = 0,
    val wochensiege: Int = 0,
    val benutzername: String? = null,
)

@Serializable
data class WeltWochensieg(
    val woche: Int = 0,
    val name: String = "",
    val leitstelle: String = "",
    val credits: Long = 0,
    val lagenGedeckt: Int = 0,
    val preisgeld: Long = 0,
    val benutzername: String? = null,
)

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

/**
 * Eine Fahrzeugvorlage mit den Feldern, die die Welt braucht.
 *
 * <b>Eine eigene Form neben `Fahrzeugvorlage`.</b> Die des Rundenspiels führt
 * keine Fähigkeiten — die Welt sortiert und prüft aber genau danach (was eine
 * Lage fordert, was eine Wache aufnehmen darf). Sie liest denselben Katalog
 * (`/api/catalog`) und lässt den Rest fallen.
 */
@Serializable
data class WeltVorlage(
    val id: String = "",
    val organisation: String = "",
    val hiOrg: String = "",
    val typ: String = "",
    val beschreibung: String = "",
    val besatzung: String = "",
    val faehigkeiten: List<String> = emptyList(),
    val kategorie: String = "",
    /** Die Staaten, in denen es die Fassung gibt — der erste ist ihre Heimat. */
    val staaten: List<String> = emptyList(),
)

@Serializable
data class WeltKatalog(val fahrzeuge: List<WeltVorlage> = emptyList())

@Serializable
data class WeltOrtsname(val name: String = "")

// ------------------------------------------------------------- Anfragerümpfe

@Serializable
internal data class WeltGruendung(val name: String, val lat: Double, val lon: Double)

@Serializable
internal data class WeltAlarm(
    val fahrzeugIds: List<String>,
    val token: String,
    val zugId: String? = null,
)

@Serializable
internal data class WeltWachenbau(val name: String, val art: String, val lat: Double, val lon: Double)

@Serializable
internal data class WeltFahrzeugkauf(val wacheId: String, val vorlageId: String)

@Serializable
internal data class WeltName(val name: String)

@Serializable
internal data class WeltUmsetzen(val zielWacheId: String)

@Serializable
internal data class WeltWerkstattziel(val werkstattId: String)

@Serializable
internal data class WeltLehrgangsziel(val einrichtungId: String, val lehrgangId: String)

@Serializable
internal data class WeltStreife(val stationen: List<WeltStreifenstation>)

@Serializable
internal data class WeltRoutensicherung(val name: String, val stationen: List<WeltStreifenstation>)

@Serializable
internal data class WeltBereitstellung(val woche: Int, val fahrzeugIds: List<String>)

@Serializable
internal data class WeltFahrzeugliste(val fahrzeugIds: List<String>)

@Serializable
internal data class WeltWappen(val zeichen: String?, val farbe: Int?, val fotoZeigen: Boolean)

@Serializable
internal data class WeltZugaufstellung(val wacheId: String, val vorgabeId: String, val name: String?)

@Serializable
internal data class WeltZugfahrzeug(val fahrzeugId: String, val dazu: Boolean)

@Serializable
internal data class WeltBetrag(val betrag: Long)

@Serializable
internal data class WeltEinstellungSetzen(
    val gangart: String,
    val dichte: Int,
    val nachschub: Int,
    val arbeitszeit: Int,
)

@Serializable
internal data class WeltLeiheAnbieten(val fahrzeugId: String, val tage: Int, val preis: Int)

@Serializable
internal data class WeltLeiheMieten(val zielWacheId: String)

@Serializable
internal data class WeltGesuchAufgeben(
    val kategorie: String,
    val zielWacheId: String,
    val tage: Int,
    val preis: Int,
)

@Serializable
internal data class WeltPasswort(val passwort: String)

@Serializable
internal data class WeltGesuchBedienen(val fahrzeugId: String)

@Serializable
internal data class WeltPoiNeu(
    val name: String,
    val art: String,
    val lat: Double,
    val lon: Double,
    val flaeche: Double,
    val gelaende: List<WeltPunkt>? = null,
    val gelaendeart: String? = null,
    val strecke: Boolean = false,
)

/**
 * Eine Änderung an einem eigenen Punkt — nur, was gesetzt ist, geht hinaus.
 *
 * `explicitNulls = false` in `Netz.abgabe` lässt die leeren Felder weg; der
 * Server unterscheidet „nicht mitgeschickt“ von „leer“. Die Geländeart braucht
 * deshalb ihren eigenen Schalter (`gelaendeartSetzen`), denn `null` heißt dort
 * „Gebäudegrundstück“.
 */
@Serializable
data class WeltPoiAenderung(
    val name: String? = null,
    val art: String? = null,
    val flaeche: Double? = null,
    val sichtbar: Boolean? = null,
    val aktiv: Boolean? = null,
    val gewicht: Int? = null,
    val nachtsBelegt: Boolean? = null,
    val organisationen: List<String>? = null,
    val farbe: Int? = null,
    val betroffene: Int? = null,
    val hinweise: String? = null,
    val gelaende: List<WeltPunkt>? = null,
    val gelaendeart: String? = null,
    val gelaendeartSetzen: Boolean? = null,
    val strecke: Boolean? = null,
)
