package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Der Einzelruf — das Telefonat über das Funkgerät, von Platz zu Platz.
 *
 * Übertragen aus `Einzelruf` in `web/src/types.ts`. Der Server führt nur
 * klingelnde und laufende Rufe im Raumzustand: Was nicht mehr in der Liste steht,
 * ist vorbei.
 */
@Serializable
data class Einzelruf(
    val id: String = "",
    /** `Klingelt` oder `Laeuft`. */
    val zustand: String = "",
    val vonPlayerId: String = "",
    val vonName: String = "",
    /** `null` heißt: an die Leitstelle — dann klingelt es an jedem Leitstellenplatz. */
    val zielPlayerId: String? = null,
    val zielName: String = "",
    val angenommenVonPlayerId: String? = null,
    val eingangUm: String = "",
    val angenommenUm: String? = null,
    /**
     * Ob am anderen Ende eine Bot-Besatzung sitzt. Dann läuft das Gespräch in
     * Zeilen statt mit Stimme — sie hört zu, sie hat nur keine Stimme auf der Leitung.
     */
    val mitBot: Boolean = false,
    /** Was gesagt wurde — nur im Gespräch mit einer Bot-Besatzung und nur für die Beteiligten. */
    val verlauf: List<Einzelrufzeile>? = null,
) {
    val klingelt: Boolean get() = zustand == "Klingelt"
    val laeuft: Boolean get() = zustand == "Laeuft"
}

/** Eine Zeile im Gespräch mit einer Bot-Besatzung. */
@Serializable
data class Einzelrufzeile(
    val id: String = "",
    val zeit: String = "",
    val vonPlayerId: String = "",
    val vonName: String = "",
    val text: String = "",
)
