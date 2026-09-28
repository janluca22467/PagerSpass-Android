package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable
import java.net.URLEncoder

/**
 * Die REST-Wege des Fahrzeugplatzes — übertragen aus `web/src/api/rest.ts`.
 *
 * Bisher genau einer: der Zugang für den Handy-Funkbegleiter. Alles andere am
 * Fahrzeug läuft über den Hub (`Rundenbefehle.kt`).
 */
class Fahrzeugwege(private val netz: Netz) {

    /**
     * Erzeugt den kurzlebigen QR-Zugang zum Funkbegleiter dieser Runde.
     *
     * Der Gerätestand fährt mit: Das gekoppelte Handy soll die Wahl dieses Geräts
     * spiegeln (Bauform, Bauart, Ton), nicht selbst eine anbieten.
     */
    suspend fun funkbegleiter(kennung: String, code: String, geraete: Begleitergeraete): Begleiterzugang =
        netz.hole(
            "/api/konto/${teil(kennung)}/raum/${teil(code)}/funkbegleiter",
            "POST",
            Netz.abgabe.encodeToString(Begleitergeraete.serializer(), geraete),
        )
}

/** Was der Server für den Begleiter herausgibt — der Token gilt acht Stunden. */
@Serializable
data class Begleiterzugang(
    val token: String = "",
    val gueltigBis: String = "",
)

private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
