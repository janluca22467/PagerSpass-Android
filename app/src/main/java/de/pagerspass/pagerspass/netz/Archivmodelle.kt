package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Eine abgeschlossene Schicht zum Nachlesen — `ArchivRunde` aus `web/src/types.ts`.
 *
 * <b>Anders als im Spiel steht hier alles:</b> das vollständige Funkprotokoll,
 * das ganze Anrufjournal, die Auswertung und die Anfahrten fürs Replay. Die
 * laufende Runde trägt nur die letzten hundertfünfzig Funksprüche mit sich.
 *
 * <b>Spieler, Fahrzeuge und Funkzeilen sind dieselben Modelle wie im Dienst</b>
 * (`Spieler`, `Rundenfahrzeug`, `Funkzeile`): Der Server schickt sie mit
 * denselben DTOs. Die Einsätze bekommen ein eigenes Modell, weil das Archiv an
 * ihnen mehr liest als der Dienst — Eingang, Chronologie, die beiden Zeiten.
 *
 * <b>Die Auswertung heißt hier `ArchivAuswertung`</b> und nicht `Auswertung`:
 * Die Nachbesprechung der gerade beendeten Runde bekommt ihr eigenes Modell, und
 * zwei gleichnamige Klassen im selben Paket lassen sich nicht bauen.
 */
@Serializable
data class ArchivRunde(
    val code: String = "",
    val settings: Rundeneinstellungen = Rundeneinstellungen(),
    val gestartetUm: String? = null,
    val beendetUm: String? = null,
    val players: List<Spieler> = emptyList(),
    val vehicles: List<Rundenfahrzeug> = emptyList(),
    val incidents: List<ArchivEinsatz> = emptyList(),
    val funkprotokoll: List<Funkzeile> = emptyList(),
    val anrufjournal: List<ArchivAnruf> = emptyList(),
    val auswertung: ArchivAuswertung = ArchivAuswertung(),
    /** Anfahrtsabschnitte fürs Replay. */
    val bewegungsabschnitte: List<ArchivAnfahrt> = emptyList(),
    /** Die Fahrten, die es nicht gab — fehlt bei älteren Archivrunden. */
    val schattenspuren: List<ArchivSchattenspur> = emptyList(),
)

/** Ein Einsatz, wie das Archiv ihn liest. */
@Serializable
data class ArchivEinsatz(
    val id: String = "",
    val einsatznummer: String = "",
    val stichwort: String = "",
    val stichwortText: String = "",
    val meldebild: String = "",
    val adresse: String = "",
    val ortsteil: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val organisation: String = "Feuerwehr",
    val prioritaet: Int = 2,
    /** `Offen`, `Alarmiert`, `Anfahrt`, `VorOrt`, `Abgeschlossen` … */
    val state: String = "Offen",
    val alarmierteFahrzeuge: List<String> = emptyList(),
    val chronologie: List<ArchivChronikzeile> = emptyList(),
    val eingangUm: String = "",
    /** Wann der Einsatz geschlossen wurde — das Replay blendet ihn danach aus. */
    val abgeschlossenUm: String? = null,
    val dispositionszeitSekunden: Double? = null,
    val hilfsfristSekunden: Double? = null,
)

/** Eine Zeile in der Chronologie eines Einsatzes. */
@Serializable
data class ArchivChronikzeile(
    val zeit: String = "",
    val text: String = "",
    val urheber: String? = null,
)

/** Ein abgeschlossener Anruf im Journal — das Gespräch bleibt nachlesbar. */
@Serializable
data class ArchivAnruf(
    val anrufId: String = "",
    val nummer: Int = 0,
    val eingangUm: String = "",
    val abgeschlossenUm: String = "",
    /** `EinsatzAngelegt`, `Verworfen`, `Verpasst`, `Abgewiesen`, `Presse`, `Zugeordnet`. */
    val ausgang: String = "",
    val incidentId: String? = null,
    val verlauf: List<Gespraechszeile> = emptyList(),
)

/** Was die Nachbesprechung über die Schicht zu sagen hat. */
@Serializable
data class ArchivAuswertung(
    val befunde: List<ArchivBefund> = emptyList(),
    val zeitachsen: List<ArchivZeitachse> = emptyList(),
    val spieler: List<Schichtbilanz> = emptyList(),
    val doppelmeldungen: ArchivDoppelmeldungen? = null,
)

/** Ein Punkt der Nachbesprechung — Klartext statt Punktzahl. */
@Serializable
data class ArchivBefund(
    /** `Lob`, `Hinweis`, `Mangel`. */
    val grad: String = "Hinweis",
    val titel: String = "",
    val text: String = "",
    /** `null` bei Befunden zur ganzen Schicht. */
    val incidentId: String? = null,
)

/** Die Marken eines Einsatzes auf der Zeitleiste. Nur der Notruf steht immer fest. */
@Serializable
data class ArchivZeitachse(
    val incidentId: String = "",
    val notruf: String = "",
    val alarm: String? = null,
    val ausgerueckt: String? = null,
    val vorOrt: String? = null,
    val ersteLagemeldung: String? = null,
    val abschluss: String? = null,
)

/** Was aus den mehrfach gemeldeten Lagen wurde. */
@Serializable
data class ArchivDoppelmeldungen(
    val mehrfachGemeldeteLagen: Int = 0,
    val zusammengefuehrt: Int = 0,
    val falschzugeordnet: Int = 0,
    val doppelalarmierungen: Int = 0,
    val nennenswert: Boolean = false,
)

/**
 * Ein Anfahrtsabschnitt fürs Replay: Das Fahrzeug verlässt bei `anfahrtBeginn`
 * die Wache und erreicht bei `ankunft` den Einsatz. Dazwischen wird hier geradlinig
 * gerechnet — ohne beim Server nachzufragen.
 */
@Serializable
data class ArchivAnfahrt(
    val vehicleId: String = "",
    val incidentId: String = "",
    val anfahrtBeginn: String = "",
    /** `null`, solange unbekannt ist, wann (oder ob) es ankam — dann bleibt es auf der Wache. */
    val ankunft: String? = null,
    val wacheLat: Double = 0.0,
    val wacheLon: Double = 0.0,
    val zielLat: Double = 0.0,
    val zielLon: Double = 0.0,
)

/** Die Fahrt, die es nicht gab — ein Fahrzeug, das früher dagewesen wäre. */
@Serializable
data class ArchivSchattenspur(
    val incidentId: String = "",
    val vehicleId: String = "",
    val funkrufname: String = "",
    val typ: String = "",
    val anfahrtBeginn: String = "",
    val ankunft: String = "",
    val wacheLat: Double = 0.0,
    val wacheLon: Double = 0.0,
    val zielLat: Double = 0.0,
    val zielLon: Double = 0.0,
    val ersparnisSekunden: Double = 0.0,
)
