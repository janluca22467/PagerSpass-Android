package de.pagerspass.pagerspass.netz

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/** Die Antwort von `GET /api/android/server`. */
@Serializable
data class Zentralvorgabe(
    /** `main`, `beta` oder `v6`. */
    val ziel: String? = null,
    val adresse: String? = null,
)

/**
 * Die zentrale Android-Version — eingestellt in der Verwaltung unter
 * „Android-Version", abgefragt bei jedem Start der App.
 *
 * <b>Gefragt wird immer pagerspass.de, nie der Server, auf dem die App gerade
 * steht.</b> Die Frage „wohin gehöre ich" darf nur an eine Adresse gehen, die
 * sich nie ändert: Stünde die App auf der Beta und fragte die Beta, käme sie von
 * dort nie mehr herunter, sobald die beiden Stände verschieden antworten. Die
 * Verwaltung speichert die Wahl deshalb immer am Release.
 *
 * <b>Übernommen wird nur ein bekanntes Ziel, nie die Adresse aus der
 * Antwort.</b> `adresse` steht zur Anzeige darin; welche Server es gibt, weiß die
 * App selbst (`Server`). Sonst könnte eine einzige verfälschte Antwort jedes
 * Gerät samt Anmeldung an einen fremden Rechner schicken.
 *
 * <b>Kurzer Zeitablauf, kein Fehler.</b> Der Abruf steht vor dem ersten
 * Bildschirm. Antwortet pagerspass.de nicht, bleibt alles, wie es ist — eine
 * Meldung darüber wäre eine Störung, die niemand beheben kann, auf einem
 * Bildschirm, der ohne sie genauso funktioniert.
 */
object Zentralserver {

    /** Die eine feste Adresse der Abfrage. */
    const val ABFRAGE = "${Server.BETRIEB}/api/android/server"

    /** Drei Sekunden je Richtung — länger stünde ein leerer Bildschirm da. */
    private const val ZEITABLAUF = 3_000

    /**
     * Der zentral vorgegebene Server als eine der Adressen aus `Server`, oder
     * `null`, wenn die Frage scheitert oder ein unbekanntes Ziel zurückkommt.
     */
    suspend fun holen(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val draht = (URL(ABFRAGE).openConnection() as HttpURLConnection).apply {
                connectTimeout = ZEITABLAUF
                readTimeout = ZEITABLAUF
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (draht.responseCode !in 200..299) return@runCatching null
                val text = draht.inputStream.bufferedReader().use(BufferedReader::readText)
                adresse(Netz.abgabe.decodeFromString(Zentralvorgabe.serializer(), text).ziel)
            } finally {
                draht.disconnect()
            }
        }.onFailure { Log.w("Zentralserver", "Abfrage gescheitert", it) }.getOrNull()
    }

    /** Ein Ziel der Verwaltung als Adresse der App — die Namen sind die der API. */
    fun adresse(ziel: String?): String? = when (ziel?.trim()?.lowercase()) {
        "main" -> Server.BETRIEB
        "beta" -> Server.BETA
        "v6" -> Server.V6
        else -> null
    }
}
