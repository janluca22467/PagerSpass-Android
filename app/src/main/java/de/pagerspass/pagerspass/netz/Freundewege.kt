package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege des Freundebereichs, die `Spielwege` noch fehlten — übertragen aus
 * `web/src/api/rest.ts` (Freunde, Brett, Geschenke).
 *
 * <b>Eine eigene Klasse neben `Spielwege`</b> und nicht darin: Suche, Blockade,
 * Meldungen und die Zeitleiste eines Profils gehören nur hierher, und `Spielwege`
 * ist schon die Datei, in der jede Seite ihre Wege sucht.
 *
 * Geprüft wird hier nichts — Blockaden, Grenzen und Gegenseitigkeit entscheidet
 * der Server, und was er ablehnt, kommt als `Netzfehler` mit seinem Satz zurück.
 */
class Freundewege(private val netz: Netz) {

    // ----------------------------------------------------------------- Freunde

    /** Ein Konto über seinen eindeutigen Benutzernamen suchen. */
    suspend fun suchen(kennung: String, benutzername: String): Suchtreffer =
        netz.hole(
            "/api/freunde/${teil(kennung)}/suche?benutzername=${teil(benutzername.trim())}",
        )

    /**
     * Eine Anfrage stellen. Hatte der andere schon gefragt, sind beide sofort
     * verbunden — dann kommt `Bestaetigt` zurück.
     */
    suspend fun anfragen(kennung: String, wen: String): Anfragestand = netz.hole(
        "/api/freunde/${teil(kennung)}/anfrage",
        "POST",
        buildJsonObject { put("kennung", wen) }.toString(),
    )

    suspend fun antworten(kennung: String, wen: String, annehmen: Boolean) = netz.ohneAntwort(
        "/api/freunde/${teil(kennung)}/antwort",
        "POST",
        buildJsonObject {
            put("kennung", wen)
            put("annehmen", annehmen)
        }.toString(),
    )

    /** Blockieren — beendet zugleich Freundschaft, Einladungen und Nachrichten. */
    suspend fun blockieren(kennung: String, wen: String) = netz.ohneAntwort(
        "/api/freunde/${teil(kennung)}/blockieren",
        "POST",
        buildJsonObject { put("kennung", wen) }.toString(),
    )

    /** Ein Konto melden — unabhängig von Freundschaft oder Blockade. */
    suspend fun melden(kennung: String, wen: String, grund: String) = netz.ohneAntwort(
        "/api/freunde/${teil(kennung)}/melden",
        "POST",
        buildJsonObject {
            put("kennung", wen)
            put("grund", grund)
        }.toString(),
    )

    /**
     * Freundschaft beenden, eigene Anfrage zurückziehen oder eigene Blockade
     * aufheben — alle drei räumen dieselbe Zeile weg.
     */
    suspend fun loesen(kennung: String, wen: String) =
        netz.ohneAntwort("/api/freunde/${teil(kennung)}/${teil(wen)}", "DELETE")

    /** Leute, mit denen man schon gefahren ist, aber noch nicht befreundet. */
    suspend fun vorschlaege(kennung: String): List<Vorschlag> =
        netz.hole("/api/freunde/${teil(kennung)}/vorschlaege")

    /** Ein Geschenk öffnen — erst hier erfährt man, was drin ist. */
    suspend fun geschenkOeffnen(kennung: String, nr: Long): Geschenkinhalt =
        netz.hole("/api/freunde/${teil(kennung)}/geschenke/$nr/oeffnen", "POST")

    // ------------------------------------------------------------------ Brett

    /** Die Einträge eines einzelnen Kontos — die Zeitleiste seines Profils. */
    suspend fun brettVon(kennung: String, wen: String, vorNr: Long? = null): BrettSeite =
        netz.hole(
            "/api/brett/${teil(kennung)}/von/${teil(wen)}" + (vorNr?.let { "?vorNr=$it" } ?: ""),
        )

    /**
     * Einen Beitrag anschlagen — mit Sichtbarkeit und, wenn gewählt, einer
     * angehängten eigenen Schicht (`roomCode`).
     */
    suspend fun beitragSchreiben(
        kennung: String,
        text: String,
        sichtbarkeit: String,
        roomCode: String?,
    ): Bretteintrag = netz.hole(
        "/api/brett/${teil(kennung)}",
        "POST",
        Netz.abgabe.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("text", text)
                put("sichtbarkeit", sichtbarkeit)
                put("roomCode", roomCode?.let { JsonPrimitive(it) } ?: JsonNull)
            },
        ),
    )

    /** Den eigenen Eintrag zurücknehmen. */
    suspend fun eintragEntfernen(kennung: String, nr: Long) =
        netz.ohneAntwort("/api/brett/${teil(kennung)}/eintrag/$nr", "DELETE")

    /** Quittieren — derselbe Aufruf nimmt die eigene Quittung auch wieder zurück. */
    suspend fun quittieren(kennung: String, nr: Long): Quittungsstand =
        netz.hole("/api/brett/${teil(kennung)}/eintrag/$nr/quittung", "POST")

    suspend fun eintragMelden(kennung: String, nr: Long, grund: String?) = netz.ohneAntwort(
        "/api/brett/${teil(kennung)}/eintrag/$nr/melden",
        "POST",
        grundRumpf(grund),
    )

    suspend fun kommentarMelden(kennung: String, nr: Long, grund: String?) = netz.ohneAntwort(
        "/api/brett/${teil(kennung)}/kommentar/$nr/melden",
        "POST",
        grundRumpf(grund),
    )

    private fun grundRumpf(grund: String?): String = Netz.abgabe.encodeToString(
        JsonObject.serializer(),
        buildJsonObject { put("grund", grund?.let { JsonPrimitive(it) } ?: JsonNull) },
    )
}

/** Wie in `Spielwege`: einmal kodieren statt an jeder Stelle. */
private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
