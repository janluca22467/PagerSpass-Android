package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Was der Raumzustand über die Grundmodelle hinaus trägt — übertragen aus
 * `web/src/types.ts`.
 *
 * <b>Getrennt von `Rundenmodelle.kt`</b>, weil das die Teile sind, an denen die
 * Handgriffe hängen: Einzelruf, Feststellung, Anrufjournal, Platz- und
 * Beitrittsanfragen, Übergabe, AAO-Vorlagen, Patientenbogen. Die Grunddatei sagt,
 * was eine Runde *ist*; diese, was man in ihr *tun* kann.
 *
 * Wie überall gilt: `ignoreUnknownKeys` lässt fallen, was hier nicht steht, und
 * jeder Wert hat eine Vorgabe — ein fehlendes Feld des Servers darf die Runde nicht
 * unlesbar machen.
 */

/**
 * Ein Einzelruf — das Telefonat über Funk, Gerät zu Gerät.
 *
 * `zielPlayerId = null` heißt: an die Leitstelle; dann klingelt es an jedem
 * Leitstellenplatz. `mitBot` sagt, ob am anderen Ende eine Bot-Besatzung sitzt —
 * dann wird getippt oder gesprochen und der Server schreibt mit, statt Stimme zu
 * übertragen.
 */
@Serializable
data class Einzelruf(
    val id: String = "",
    /** `Klingelt` oder `Laeuft`. */
    val zustand: String = "Klingelt",
    val vonPlayerId: String = "",
    val vonName: String = "",
    val zielPlayerId: String? = null,
    val zielName: String = "",
    val angenommenVonPlayerId: String? = null,
    val eingangUm: String = "",
    val angenommenUm: String? = null,
    val mitBot: Boolean = false,
    val verlauf: List<Einzelrufzeile>? = null,
) {
    val klingelt: Boolean get() = zustand == "Klingelt"
    val laeuft: Boolean get() = zustand == "Laeuft"
}

@Serializable
data class Einzelrufzeile(
    val id: String = "",
    val zeit: String = "",
    val vonPlayerId: String = "",
    val vonName: String = "",
    val text: String = "",
)

/**
 * Eine Eigenfeststellung einer Streife — eine Meldung, aus der noch kein Einsatz
 * geworden ist. Sie verfällt von selbst (`verfaelltBis`), ohne Punktabzug.
 */
@Serializable
data class Feststellung(
    val id: String = "",
    val vehicleId: String = "",
    val funkrufname: String = "",
    val stichwort: String = "",
    val stichwortText: String = "",
    val meldebild: String = "",
    val ort: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val prioritaet: Int = 2,
    val gemeldetUm: String = "",
    val verfaelltBis: String = "",
)

/** Die Ortung eines Anrufers — am Apparat oder nachträglich aus dem Journal. */
@Serializable
data class Ortung(
    val laeuft: Boolean = false,
    val fertigUm: String = "",
    val erfolgreich: Boolean? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val radiusMeter: Double? = null,
    val ortstext: String? = null,
)

/** Ein abgeschlossenes Gespräch im Anrufjournal der Schicht. */
@Serializable
data class AnrufjournalEintrag(
    val anrufId: String = "",
    val nummer: Int = 0,
    val eingangUm: String = "",
    val abgeschlossenUm: String = "",
    /** `EinsatzAngelegt`, `Verworfen`, `Verpasst`, `Abgewiesen`, `Presse`, `Zugeordnet` … */
    val ausgang: String = "",
    val incidentId: String? = null,
    val verlauf: List<Gespraechszeile> = emptyList(),
    val erfragt: List<String> = emptyList(),
    val ortung: Ortung? = null,
    /** Ob sich dieser Anruf nachträglich noch orten lässt. */
    val ortbar: Boolean = false,
)

/** Eine Alarm- und Ausrückeordnung der laufenden Schicht (`raum.aaoVorlagen`). */
@Serializable
data class AaoVorlage(
    val name: String = "",
    val empfohleneFahrzeuge: Int = 0,
    val empfohleneFaehigkeiten: List<String> = emptyList(),
)

/** Eine Bitte um den Leitstellentisch — oder, bei `beitrittsanfragen`, um einen Platz. */
@Serializable
data class Platzanfrage(
    val playerId: String = "",
    val name: String = "",
    val um: String = "",
)

/** Das Übergabeprotokoll der Dienstübergabe. */
@Serializable
data class Uebergabe(
    val vonPlayerId: String = "",
    val vonName: String = "",
    val zielPlayerId: String = "",
    val offeneEinsaetze: List<UebergabeEinsatz> = emptyList(),
    val fahrzeuge: List<UebergabeFahrzeug> = emptyList(),
    val klingelndeAnrufe: Int = 0,
    val wetter: String = "",
)

@Serializable
data class UebergabeEinsatz(
    val einsatznummer: String = "",
    val stichwort: String = "",
    val alterMinuten: Int = 0,
    val hinweise: List<String> = emptyList(),
)

@Serializable
data class UebergabeFahrzeug(
    val funkrufname: String = "",
    val hinweis: String = "",
)

/** Der Hubschrauberlandeplatz einer Einsatzstelle — Fläche und Licht getrennt. */
@Serializable
data class Landeplatz(
    val hergerichtet: Boolean = false,
    val ausgeleuchtet: Boolean = false,
    val fortschritt: Double = 0.0,
    val lichtfortschritt: Double = 0.0,
)

/** Eine Maßnahme am Patienten — `moeglich` falsch etwa, solange der Notarzt fehlt. */
@Serializable
data class Patientenmassnahme(
    val id: String = "",
    val name: String = "",
    val dauer: Int = 0,
    val brauchtArzt: Boolean = false,
    val laeuft: Boolean = false,
    val moeglich: Boolean = true,
)

/** Ein abgearbeitetes Untersuchungsschema mit seinen Punkten. */
@Serializable
data class Befundschema(
    val schema: String = "",
    val name: String = "",
    val punkte: List<Befundpunkt> = emptyList(),
)

/** Leerer Befund heißt unauffällig — und wird auch so angezeigt. */
@Serializable
data class Befundpunkt(
    val schluessel: String = "",
    val frage: String = "",
    val befund: String = "",
)

// ---------------------------------------------------------------- REST im Raum

/**
 * Eine Wache, auf der in *dieser* Runde Fahrzeuge stehen können
 * (`GET /api/rooms/{code}/wachen`) — samt der Fahrzeuge, die dort schon stehen.
 */
@Serializable
data class Raumwache(
    val kennung: String = "",
    val name: String = "",
    val organisation: String = "",
    val zugnummer: Int = 0,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val fahrzeuge: List<String> = emptyList(),
)

/** Ein Stichwort-Set (`GET /api/stichwortsets`). */
@Serializable
data class Stichwortset(
    val id: String = "",
    val name: String = "",
    val bundesland: String? = null,
    val lagen: Int = 0,
)

/** Eine gespeicherte Rundenvorlage — der Reglerstand einer Lobby. */
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

/** Eine dauerhafte Alarm- und Ausrückeordnung am Konto (`/api/aao`). */
@Serializable
data class AaoVorlagenzeile(
    val id: String = "",
    val name: String = "",
    val landkreisId: String? = null,
    val empfohleneFahrzeuge: Int = 0,
    val empfohleneFaehigkeiten: List<String> = emptyList(),
    val geaendertUm: String = "",
    val stichwort: String? = null,
    val stichwortText: String? = null,
    val meldebild: String? = null,
    val adresse: String? = null,
)

/** Die Antwort auf einen Speicherversuch — mit Zähler und Grenze. */
@Serializable
data class AaoSpeicherergebnis(
    val zeile: AaoVorlagenzeile = AaoVorlagenzeile(),
    val anzahl: Int = 0,
    val grenze: Int = 0,
)
