package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege rund um die Runde — übertragen aus `web/src/api/rest.ts`.
 *
 * <b>Getrennt von `Spielwege`</b>, weil das dort die sechs Seiten sind und hier
 * das, was vor, in und neben einer Runde gebraucht wird: Rundenvorlagen, Wachen,
 * Stichwort-Sets, Startkacheln, Fußzeile, Umfrage, Wartung.
 */
class Rundenwege(private val netz: Netz) {

    // ------------------------------------------------------------ Rundenvorlagen

    /** Die eigenen Rundenvorlagen — gespeicherte Lobby-Reglerstände. */
    suspend fun rundenvorlagen(kennung: String): List<Rundenvorlagenzeile> =
        netz.hole("/api/rundenvorlagen?kennung=${teil(kennung)}")

    /**
     * Den Reglerstand einer laufenden Lobby als Vorlage speichern — mit `id` wird
     * eine bestehende fortgeschrieben. Der Inhalt geht nicht mit: Der Server liest
     * ihn selbst aus dem Raum, unter dessen Sperre.
     */
    suspend fun rundenvorlageSichern(
        kennung: String,
        id: String?,
        name: String,
        raumCode: String,
    ): Rundenvorlagenzeile = netz.hole(
        "/api/rundenvorlagen?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            if (id != null) put("id", id)
            put("name", name)
            put("raumCode", raumCode)
        }.toString(),
    )

    suspend fun rundenvorlageLoeschen(kennung: String, id: String) =
        netz.ohneAntwort("/api/rundenvorlagen/${teil(id)}?kennung=${teil(kennung)}", "DELETE")

    /** Der Inhalt einer Vorlage zum Bearbeiten — Name, Plätze, Aufstellung. */
    suspend fun vorlageninhalt(kennung: String, id: String): Vorlageninhalt =
        netz.hole("/api/rundenvorlagen/${teil(id)}/inhalt?kennung=${teil(kennung)}")

    /** Und zurück: Name, Plätze und Aufstellung ersetzen, die Regler bleiben. */
    suspend fun vorlageninhaltSichern(
        kennung: String,
        id: String,
        name: String,
        maxSpieler: Int?,
        bestand: List<Vorlagenbesatzung>,
    ): Rundenvorlagenzeile = netz.hole(
        "/api/rundenvorlagen/${teil(id)}/inhalt?kennung=${teil(kennung)}",
        "PUT",
        buildJsonObject {
            put("name", name)
            put("maxSpieler", maxSpieler?.let { JsonPrimitive(it) } ?: JsonNull)
            put(
                "bestand",
                buildJsonArray {
                    bestand.forEach { b ->
                        add(
                            buildJsonObject {
                                put("vorlageId", b.vorlageId)
                                put("funkrufname", b.funkrufname?.let { JsonPrimitive(it) } ?: JsonNull)
                                put("kurzname", b.kurzname?.let { JsonPrimitive(it) } ?: JsonNull)
                                put("wacheKennung", b.wacheKennung?.let { JsonPrimitive(it) } ?: JsonNull)
                            },
                        )
                    }
                },
            )
        }.toString(),
    )

    /** Eine fremde Rundenvorlage per Code übernehmen — als eigene Kopie. */
    suspend fun rundenvorlageUebernehmen(kennung: String, code: String): Rundenvorlagenzeile =
        netz.hole(
            "/api/rundenvorlagen/code/${teil(code.trim().uppercase())}?kennung=${teil(kennung)}",
            "POST",
        )

    /** Eine gewöhnliche Runde mit dem Reglerstand dieser Vorlage eröffnen — der Raumcode. */
    suspend fun rundenvorlageStarten(kennung: String, id: String): String {
        val antwort = netz.hole<JsonObject>(
            "/api/rooms/rundenvorlage?id=${teil(id)}&kennung=${teil(kennung)}",
            "POST",
        )
        return (antwort["code"] as? JsonPrimitive)?.content
            ?: throw Netzfehler("Die Leitstelle ist nicht erreichbar.")
    }

    // -------------------------------------------------------------------- Wachen

    /** Die echten Wachen eines Landkreises — leer, solange keine Geodaten vorliegen. */
    suspend fun kreiswachen(landkreisId: String): List<Wache> =
        netz.hole("/api/landkreise/${teil(landkreisId)}/wachen")

    /** Die Wachen, auf denen in *dieser* Runde Fahrzeuge stehen können. */
    suspend fun raumwachen(code: String): List<Raumwache> =
        netz.hole("/api/rooms/${teil(code)}/wachen")

    /** Die echten Straßennamen eines Kreises — leer ohne OSM-Daten. */
    suspend fun strassen(landkreisId: String): List<String> =
        netz.hole("/api/landkreise/${teil(landkreisId)}/strassen")

    /** Die Stichwort-Sets, die in der Lobby zur Wahl stehen — nur die Köpfe. */
    suspend fun stichwortsets(): List<Stichwortset> = netz.hole("/api/stichwortsets")

    // --------------------------------------------------------- Startbildschirm

    suspend fun startkacheln(): List<Startkachel> = netz.hole("/api/startkacheln")

    suspend fun footerknoepfe(): List<Footerknopf> = netz.hole("/api/footer")

    /** Die Umfrage, die dieses Konto gerade beantworten kann — `null`, wenn keine. */
    suspend fun umfrage(kennung: String): OffeneUmfrage? =
        netz.hole<Umfrageumschlag>("/api/konto/${teil(kennung)}/umfrage").umfrage

    suspend fun umfrageAntworten(kennung: String, umfrageId: String, option: Int) =
        netz.ohneAntwort(
            "/api/konto/${teil(kennung)}/umfrage/${teil(umfrageId)}",
            "POST",
            buildJsonObject { put("option", option) }.toString(),
        )

    /**
     * Den Hinweis auf die laufende Clanrunde abnehmen. Die Runde läuft weiter —
     * sie verschwindet nur aus Startbildschirm und Wachenseite.
     */
    suspend fun clanrundeSchliessen(kennung: String, gemeinschaftId: String) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/${teil(gemeinschaftId)}/clanrunde",
            "DELETE",
        )

    // ------------------------------------------------------------------- Wartung

    /**
     * Ob gerade gewartet wird — und ob eine angekündigt ist. Der eine Weg, der
     * während einer Wartung ausdrücklich antwortet, ohne Anmeldung.
     */
    suspend fun wartung(): Wartungsumschlag = netz.hole("/api/wartung")

    // ------------------------------------------------------------------ Lobby

    /** Einem Mitspieler eine Freundschaftsanfrage stellen. */
    suspend fun freundAnfragen(kennung: String, wen: String) =
        netz.ohneAntwort(
            "/api/freunde/${teil(kennung)}/anfrage",
            "POST",
            buildJsonObject { put("kennung", wen) }.toString(),
        )

    /**
     * Ob der Server den KI-Funk anbietet (`kiFunk` in `/api/version`). Ein Haken,
     * der still nichts tut, wäre schlimmer als keiner.
     */
    suspend fun kiFunkVerfuegbar(): Boolean {
        val antwort = netz.hole<JsonObject>("/api/version")
        return (antwort["kiFunk"] as? JsonPrimitive)?.content == "true"
    }
}

private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
