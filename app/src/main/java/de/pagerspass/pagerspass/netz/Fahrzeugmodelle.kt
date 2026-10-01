package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Was die Fahrzeugansicht seit Web 5.0.0.26 (Branch `v6`) mehr liest — übertragen
 * aus `web/src/types.ts`: PatSim mit Monitor und Gespräch, die Abfragen der
 * Polizeilage, FwSim an der Einsatzstelle der Feuerwehr und die Tafeln des
 * Einsatz-Tablets.
 *
 * <b>Jedes Feld hat eine Vorgabe.</b> `pagerspass.de` läuft womöglich noch auf dem
 * älteren Stand und schickt diese Felder gar nicht. Dann bleibt die Liste leer,
 * die Lage `null` — und die Ansicht blendet den Abschnitt aus, statt ihn halb zu
 * zeigen. Dieselbe Regel wie an `Raumzustand`: anzeigen, was kommt.
 */

/** Eine unbesetzte Stelle der Besatzung — „ohne Maschinist", „Trupp 1/2". */
@Serializable
data class Besatzungsluecke(
    val art: String = "",
    val name: String = "",
    val soll: Int = 1,
    val ist: Int = 0,
)

/** Eine Lagemeldung an der Lage — wer, wann, was. */
@Serializable
data class Lagemeldungseintrag(
    val zeit: String = "",
    val funkrufname: String = "",
    val text: String = "",
)

/** Eine Nachforderung, die bei der Einsatzleitung gesammelt wartet. */
@Serializable
data class OffeneNachforderung(
    val funkrufname: String = "",
    val text: String = "",
)

/** Eine Zeile der Atemschutzüberwachungstafel (`Atemschutzueberwachung.cs`). */
@Serializable
data class Atemschutztrupp(
    val funkrufname: String = "",
    val trupp: String = "",
    val auftrag: String = "",
    val angeschlossenUm: String = "",
    /** Zuletzt gemeldeter Flaschendruck in bar; 300 beim Anschließen. */
    val druck: Double = 300.0,
    val letzteMeldungUm: String = "",
    val rueckzugUm: String? = null,
    val abgelegtUm: String? = null,
    /** `ImEinsatz`, `Rueckzug` oder `Abgelegt`. */
    val stand: String = "ImEinsatz",
)

// ------------------------------------------------------------------ PatSim

/** Ein gemessener Wert — mit Alter, Trend und, beim EKG, der Kurve. */
@Serializable
data class Messwert(
    val was: String = "",
    val name: String = "",
    val anzeige: String = "",
    val um: String = "",
    val auffaellig: Boolean = false,
    /** `steigt`, `faellt`, `gleich`, `geaendert` — `null` bei der ersten Messung. */
    val trend: String? = null,
    val vorher: String? = null,
    /** `sinus`, `stemi`, `ves`, `vhf`, `avb`, `vf`, `asystolie`. */
    val kurve: String? = null,
    val frequenz: Double? = null,
)

/** Der angeschlossene Monitor — er misst fortlaufend, was er kann. */
@Serializable
data class Patientenmonitor(
    val herzfrequenz: Double? = null,
    val saettigung: Double? = null,
    val atemfrequenz: Double = 0.0,
    val kurve: String = "sinus",
    val pleth: Double = 0.0,
    val alarme: List<String> = emptyList(),
)

/** Eine Zeile im Verlaufsbogen des Patienten. */
@Serializable
data class Patientenvermerk(
    val um: String = "",
    val text: String = "",
    val von: String? = null,
)

/** Eine Frage an den Patienten und seine Antwort (KI, Premium). */
@Serializable
data class Patientenaeusserung(
    val frage: String = "",
    val antwort: String = "",
    val von: String = "",
    val zeit: String = "",
)

/** Die Antwort des Hubs auf `PatientFragen`/`PatientBesprechen`. */
@Serializable
data class KiPatientErgebnis(
    val text: String? = null,
    val fehler: String? = null,
    val ki: Boolean = false,
)

/** `GET /api/konto/{k}/massnahmenkatalog` — einfach oder erweitert. */
@Serializable
data class Massnahmenkatalog(val alle: Boolean = false)

// ------------------------------------------------------- Polizeiliche Abfragen

/**
 * Eine Person einer Polizeilage (`Abfragelauf` am Server). Wie am Patienten ist alles
 * schon gefiltert: Was nicht festgestellt ist, kommt gar nicht erst über die Leitung.
 */
@Serializable
data class Beteiligter(
    val id: String = "",
    val rolle: String = "",
    val rolleText: String = "",
    val beschreibung: String = "",
    val anwesend: Boolean = true,
    val name: String? = null,
    val geburtsdatum: String? = null,
    val anschrift: String? = null,
    val personalienFestgestellt: Boolean = false,
    val feststellungFertigUm: String? = null,
    val kennzeichen: String? = null,
    val hatFahrzeug: Boolean = false,
    val abfragen: List<Polizeiabfrage> = emptyList(),
    val fragtAb: String? = null,
    val abfrageFertigUm: String? = null,
    val abfrageVon: String? = null,
    val verhalten: String? = null,
    val massnahmen: List<Polizeimassnahme> = emptyList(),
    val abgeschlossen: Boolean = false,
    val einordnung: String? = null,
    val dokument: String? = null,
    val identitaetGeklaert: Boolean = false,
    val verweigert: Boolean = false,
    val dienststelleFertigUm: String? = null,
    val mitgenommenVon: String? = null,
)

@Serializable
data class Polizeiabfrage(
    val art: String = "",
    val name: String = "",
    val ergebnis: String? = null,
    val treffer: Boolean = false,
    val eigensicherung: Boolean = false,
    val hindernis: String? = null,
    val dauer: Double = 20.0,
    val angefragtVon: String? = null,
    val fehleingabe: Boolean = false,
    val beiLeitstelle: Boolean = false,
    val durchzugebenAn: String? = null,
)

@Serializable
data class Polizeimassnahme(
    val id: String = "",
    val name: String = "",
    val erledigt: Boolean = false,
    val hindernis: String? = null,
    val brauchtVerstaerkung: Boolean = false,
    val ergebnis: String? = null,
)

// ------------------------------------------------------------------- FwSim

/** Ein Bereich der Einsatzstelle — Geschoss, Fahrzeug, Fläche, Gefahrenbereich. */
@Serializable
data class Fwbereich(
    val id: String = "",
    val name: String = "",
    val art: String = "Geschoss",
    val ebene: Int = 0,
    val anleiterbar: Boolean = false,
    /** `null`, solange das Feuer dort weder erkundet noch von außen zu sehen ist. */
    val feuer: Double? = null,
    val rauch: Double = 0.0,
    val schaden: Double = 0.0,
    val erkundet: Boolean = false,
    val vermisste: Int? = null,
    val gefunden: Int = 0,
    val amFenster: Int = 0,
    val durchsucht: Double = 0.0,
    val gefaehrdung: Double? = null,
    val abluft: Boolean = false,
    val belueftet: Boolean = false,
    val riegel: Boolean = false,
    val warBrand: Boolean = false,
    val kontrolle: Double = 0.0,
    val kontrolliert: Boolean = false,
)

@Serializable
data class Fwtrupp(
    val id: String = "",
    val vehicleId: String = "",
    val funkrufname: String = "",
    val name: String = "",
    val kurz: String = "",
    val kannAtemschutz: Boolean = false,
    val unterAtemschutz: Boolean = false,
    val csa: Boolean = false,
    /** `null` ohne Atemschutzüberwachung. */
    val druck: Double? = null,
    /** Bar je echter Sekunde — damit der Druck zwischen zwei Ständen weiterläuft. */
    val abfall: Double = 0.0,
    val status: String = "Bereit",
    val statusBis: String? = null,
    val bereichId: String? = null,
    val taetigkeit: String? = null,
    val rohr: Boolean = false,
    val traegt: Int = 0,
    val kontaminiert: Boolean = false,
    val geholtVon: String? = null,
)

@Serializable
data class Fwauftrag(
    val id: String = "",
    val name: String = "",
    val gruppe: String = "",
    val dauer: Double = 0.0,
    /** `Keines`, `Trupp`, `Bereich`, `TruppUndBereich`. */
    val ziel: String = "Keines",
    val tipp: String? = null,
    /** Warum der Auftrag gerade nicht geht — `null`: er geht. */
    val sperre: String? = null,
)

@Serializable
data class Fwgriff(
    val auftragId: String = "",
    val name: String = "",
    val truppId: String? = null,
    val bereichId: String? = null,
)

@Serializable
data class Fwfahrzeug(
    val vehicleId: String = "",
    val funkrufname: String = "",
    val typ: String = "",
    val rolle: String = "",
    val tank: Double = 0.0,
    val tankMax: Double = 0.0,
    val schaum: Double = 0.0,
    val reserveflaschen: Int = 0,
    val bot: Boolean = false,
    val erledigt: List<String> = emptyList(),
    val auftraege: List<Fwauftrag> = emptyList(),
    val empfehlung: Fwgriff? = null,
)

@Serializable
data class Fwvorgang(
    val auftragId: String = "",
    val name: String = "",
    val vehicleId: String = "",
    val bereichId: String? = null,
    val beginnUm: String = "",
    val fertigUm: String = "",
)

@Serializable
data class Fwmeldung(
    val zeit: String = "",
    /** `Info`, `Erfolg`, `Warnung`, `Gefahr`. */
    val art: String = "Info",
    val text: String = "",
    val von: String? = null,
)

@Serializable
data class Fwpatient(
    val einklemmung: String? = null,
    val zustand: Double = 0.0,
    val befreit: Boolean = false,
    val verstorben: Boolean = false,
)

@Serializable
data class Fwstoff(
    val name: String? = null,
    val unNummer: String? = null,
    val gefahrenradius: Double? = null,
    val gasfoermig: Boolean = false,
    val leck: Double? = null,
    val ausbreitung: Double = 0.0,
    val erkannt: Boolean = false,
    // ------------------------------------------ seit Web 5.0.0.26 (v6), fürs Lagebild
    /** Absperrradius in Metern — `null`, solange niemand abgesperrt hat. */
    val absperrung: Double? = null,
    /** Ob der Kanal abgedichtet ist (Gully zu). */
    val aufgefangen: Boolean = false,
    val dekon: Boolean = false,
    val gewaesserErreicht: Boolean = false,
)

@Serializable
data class Fwbericht(
    val note: String = "",
    val punkte: Double = 0.0,
    val lob: List<String> = emptyList(),
    val kritik: List<String> = emptyList(),
    val minuten: Double = 0.0,
)

/**
 * FwSim — die Einsatzstelle der Feuerwehr (`Logic/Fwlauf` am Server). Eine Lage für
 * alle Fahrzeuge: Jedes sieht dieselben Bereiche und Trupps, aber seine eigenen
 * Aufträge.
 */
@Serializable
data class Fwlage(
    val art: String = "Gebaeudebrand",
    val titel: String = "",
    val lagebild: String = "",
    val variante: String? = null,
    val beginnUm: String = "",
    val minuten: Double = 0.0,
    val bereiche: List<Fwbereich> = emptyList(),
    val trupps: List<Fwtrupp> = emptyList(),
    val fahrzeuge: List<Fwfahrzeug> = emptyList(),
    val laufend: List<Fwvorgang> = emptyList(),
    val erledigt: List<String> = emptyList(),
    val lagelog: List<Fwmeldung> = emptyList(),
    /** `Tank`, `Hydrant`, `Pendelverkehr`, `LangeWegstrecke`. */
    val versorgung: String = "Tank",
    val patient: Fwpatient? = null,
    val stoff: Fwstoff? = null,
    val gerettet: Int = 0,
    val opfer: Int = 0,
    val verletzteKraefte: Int = 0,
    val offen: List<String> = emptyList(),
    val fertigUm: String? = null,
    val bericht: Fwbericht? = null,
    /** Wann der Server diesen Stand gerechnet hat. */
    val stand: String = "",
    // ------------------------------------------ seit Web 5.0.0.26 (v6), fürs Lagebild
    /** Woher der Wind weht, in Grad — dreht die Abschnitte der Fläche. */
    val wind: Double = 0.0,
    /** Der Abschnitt, an dem die Fläche gerade am schnellsten läuft. */
    val front: String? = null,
    val hektar: Double = 0.0,
    val hochvolt: Boolean = false,
)
