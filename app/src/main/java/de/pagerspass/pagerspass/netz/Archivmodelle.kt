package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Ein Anfahrtsabschnitt für die Wiedergabe einer archivierten Schicht —
 * `BewegungsabschnittDto` des Servers.
 *
 * Ein Fahrzeug verlässt bei [anfahrtBeginn] die Wache Richtung Einsatz und
 * erreicht ihn — sofern bekannt — bei [ankunft]. Die Position dazwischen
 * rechnet die App selbst linear aus, wie das Web; der Server wird dafür nicht
 * noch einmal gefragt.
 */
@Serializable
data class Bewegungsabschnitt(
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

/** Eine Zeile im Ablauf eines Einsatzes — „Alarmiert: HLF 1", „Lage an Leitstelle …". */
@Serializable
data class Chronikeintrag(
    val zeit: String = "",
    val text: String = "",
    val urheber: String? = null,
)
