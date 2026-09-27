package de.pagerspass.pagerspass.netz

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege der eigenen Fahrzeug-Icons — übertragen aus `web/src/api/rest.ts`
 * (`iconTypen` … `iconLoeschen`).
 *
 * <b>Ohne `kennung` im Abfrageteil</b>, anders als die übrigen Wege: Diese Gruppe
 * liest die geprüfte Kennung aus der Sitzung (`IconEndpunkte.Wer`) — sie muss
 * also gar nicht erst behauptet und dann widerlegt werden.
 *
 * <b>Premium steht am Server davor.</b> Ohne Abo antwortet jeder dieser Wege mit
 * 403; die Seiten führen deshalb schon vorher in den Shop (siehe `ExtrasWege`).
 */
class Iconwege(private val netz: Netz) {

    /** Die Fahrzeugtypen, für die es ein Icon geben kann — je Typ genau einer. */
    suspend fun typen(): List<Icontyp> = netz.hole("/api/icons/typen")

    /** Die Grenzen: wie viele Packs, wie groß ein Icon, wie groß ein Pack. */
    suspend fun grenzen(): IconGrenzen = netz.hole("/api/icons/grenzen")

    suspend fun packs(): List<Iconpack> = netz.hole("/api/icons/packs")

    suspend fun anlegen(name: String): Iconpack =
        netz.hole("/api/icons/packs", "POST", buildJsonObject { put("name", name) }.toString())

    /**
     * Ein ganzes Pack als Zip einspielen. Der Dateiname fährt mit — er trägt den
     * Packnamen (`Pack_NAME.zip`).
     */
    suspend fun importieren(dateiname: String, daten: ByteArray): Packimport =
        netz.mehrteilig(
            "/api/icons/packs/import",
            Mehrteil().datei("datei", dateiname, "application/zip", daten),
        )

    suspend fun umbenennen(packId: String, name: String) {
        netz.ohneAntwort(
            "/api/icons/packs/${teil(packId)}",
            "PUT",
            buildJsonObject { put("name", name) }.toString(),
        )
    }

    suspend fun loeschen(packId: String) {
        netz.ohneAntwort("/api/icons/packs/${teil(packId)}", "DELETE")
    }

    /** Der Code zum Weitergeben — er entsteht erst beim ersten Fragen und bleibt dann. */
    suspend fun code(packId: String): String {
        val antwort = netz.hole<JsonObject>("/api/icons/packs/${teil(packId)}/code", "POST")
        return (antwort["code"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            ?: throw Netzfehler("Es kam kein Code zurück.")
    }

    /** Ein fremdes Pack übernehmen — als Kopie mit eigenen Dateien und eigenem Code. */
    suspend fun uebernehmen(code: String): Iconpack =
        netz.hole("/api/icons/code/${teil(code.trim().uppercase())}", "POST")

    suspend fun icons(packId: String): List<Packicon> =
        netz.hole("/api/icons/packs/${teil(packId)}/icons")

    /**
     * Legt ein Icon ab — oder ersetzt das, das für diesen Typ schon da war.
     *
     * `dreht` darf fehlen: Beim Ersetzen bleibt dann die vorherige Wahl stehen, bei
     * einer neuen Grafik heißt es „steht still".
     */
    suspend fun setzen(packId: String, vorlageId: String, webp: ByteArray, dreht: Boolean? = null): Packicon {
        val teile = Mehrteil().datei("datei", "icon.webp", "image/webp", webp)
        if (dreht != null) teile.feld("dreht", dreht.toString())
        return netz.mehrteilig(
            "/api/icons/packs/${teil(packId)}/icons/${teil(vorlageId)}",
            teile,
            verfahren = "PUT",
        )
    }

    /** Legt allein die Drehung um — ohne das Bild noch einmal zu schicken. */
    suspend fun drehung(packId: String, vorlageId: String, dreht: Boolean) {
        netz.ohneAntwort(
            "/api/icons/packs/${teil(packId)}/icons/${teil(vorlageId)}/drehung",
            "PUT",
            buildJsonObject { put("dreht", dreht) }.toString(),
        )
    }

    /** Setzt die Blaulichter eines Icons — die ganze Liste; eine leere nimmt sie weg. */
    suspend fun blaulicht(packId: String, vorlageId: String, punkte: List<Blaulicht>) {
        val liste = LICHTER.encodeToString(ListSerializer(Blaulicht.serializer()), punkte)
        netz.ohneAntwort(
            "/api/icons/packs/${teil(packId)}/icons/${teil(vorlageId)}/blaulicht",
            "PUT",
            """{"punkte":$liste}""",
        )
    }

    /** Nimmt ein Icon wieder heraus — für diesen Typ zeichnet das Spiel dann wieder. */
    suspend fun iconLoeschen(packId: String, vorlageId: String) {
        netz.ohneAntwort("/api/icons/packs/${teil(packId)}/icons/${teil(vorlageId)}", "DELETE")
    }

    private companion object {
        /**
         * Die Lage eines Lichts muss immer mit — auch ein Licht genau in der Mitte,
         * dessen `x` und `y` ihrer Vorgabe gleichen. Der Server verlangt beide
         * (`JsonRequired`); `null` bleibt dagegen draußen und heißt „die Vorgabe".
         */
        val LICHTER = Json(from = Netz.abgabe) { encodeDefaults = true }

        fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
    }
}
