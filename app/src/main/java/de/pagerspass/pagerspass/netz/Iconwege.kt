package de.pagerspass.pagerspass.netz

import kotlinx.serialization.encodeToString
import java.io.ByteArrayOutputStream
import java.net.URLEncoder

/**
 * Die Wege der eigenen Fahrzeug-Icons — der Icon-Abschnitt von `web/src/api/rest.ts`.
 *
 * <b>Ohne Kennung im Pfad.</b> Anders als die übrigen Weltwege nimmt der Server
 * die Kennung hier aus der Sitzung (`IconEndpunkte.Wer`); die ganze Gruppe
 * steht hinter dem Premium-Filter. Die Bilder selbst (`/api/icon/…`) liegen
 * davor und ohne Anmeldung — eine Karte lädt sie wie jedes andere Bild.
 */
class Iconwege(private val netz: Netz) {

    private fun p(teil: String) = URLEncoder.encode(teil, "UTF-8").replace("+", "%20")
    private inline fun <reified T> json(wert: T): String = Netz.abgabe.encodeToString(wert)

    suspend fun typen(): List<Icontyp> = netz.hole("/api/icons/typen")

    suspend fun grenzen(): IconGrenzen = netz.hole("/api/icons/grenzen")

    suspend fun packs(): List<Iconpack> = netz.hole("/api/icons/packs")

    suspend fun anlegen(name: String): Iconpack = netz.hole("/api/icons/packs", "POST", json(Packname(name)))

    suspend fun umbenennen(packId: String, name: String) =
        netz.ohneAntwort("/api/icons/packs/${p(packId)}", "PUT", json(Packname(name)))

    suspend fun loeschen(packId: String) = netz.ohneAntwort("/api/icons/packs/${p(packId)}", "DELETE")

    /** Der Code entsteht erst, wenn ihn jemand holt — vorher belegt er keinen Platz. */
    suspend fun code(packId: String): String =
        netz.hole<Packcode>("/api/icons/packs/${p(packId)}/code", "POST").code

    /** Ein fremdes Pack übernehmen — als Kopie mit eigenen Dateien und eigenem Code. */
    suspend fun uebernehmen(code: String): Iconpack =
        netz.hole("/api/icons/code/${p(code.trim().uppercase())}", "POST")

    /** Ein ganzes Pack als Zip — der Dateiname muss `Pack_NAME.zip` sein, der Server liest den Namen daraus. */
    suspend fun importieren(dateiname: String, daten: ByteArray): Packimport =
        netz.hochladen("/api/icons/packs/import", "datei", dateiname, "application/zip", daten)

    suspend fun icons(packId: String): List<Packicon> = netz.hole("/api/icons/packs/${p(packId)}/icons")

    /**
     * Ein Icon ablegen oder ersetzen — ein Formular mit der Grafik und, wenn
     * gesetzt, der Drehung.
     *
     * `dreht = null` heißt beim Ersetzen: Die vorherige Wahl bleibt stehen. Wer
     * nur ein Bild austauscht, hat seinen Schalter nicht widerrufen.
     */
    suspend fun iconSetzen(packId: String, vorlageId: String, webp: ByteArray, dreht: Boolean?): Packicon {
        val marke = "----pagerspass${System.nanoTime()}"
        val rumpf = ByteArrayOutputStream()
        fun schreib(text: String) = rumpf.write(text.toByteArray(Charsets.UTF_8))
        schreib("--$marke\r\nContent-Disposition: form-data; name=\"datei\"; filename=\"icon.webp\"\r\n")
        schreib("Content-Type: image/webp\r\n\r\n")
        rumpf.write(webp)
        schreib("\r\n")
        if (dreht != null) {
            schreib("--$marke\r\nContent-Disposition: form-data; name=\"dreht\"\r\n\r\n$dreht\r\n")
        }
        schreib("--$marke--\r\n")
        val antwort = netz.rohBinaer(
            "/api/icons/packs/${p(packId)}/icons/${p(vorlageId)}",
            "multipart/form-data; boundary=$marke",
            rumpf.toByteArray(),
            verfahren = "PUT",
        )
        return Netz.abgabe.decodeFromString(antwort ?: "null")
    }

    /** Allein die Drehung umlegen — ohne das Bild noch einmal zu schicken. */
    suspend fun drehung(packId: String, vorlageId: String, dreht: Boolean) =
        netz.ohneAntwort("/api/icons/packs/${p(packId)}/icons/${p(vorlageId)}/drehung", "PUT", json(Icondrehung(dreht)))

    /** Die Blaulichter setzen — die ganze Liste auf einmal; eine leere nimmt sie weg. */
    suspend fun blaulicht(packId: String, vorlageId: String, punkte: List<Blaulichtpunkt>) =
        netz.ohneAntwort("/api/icons/packs/${p(packId)}/icons/${p(vorlageId)}/blaulicht", "PUT", json(Iconblaulicht(punkte)))

    suspend fun iconLoeschen(packId: String, vorlageId: String) =
        netz.ohneAntwort("/api/icons/packs/${p(packId)}/icons/${p(vorlageId)}", "DELETE")

    /** Die Icons des Packs, das gerade gilt — leer heißt: die gezeichneten Fahrzeuge. */
    suspend fun aktiv(): List<Packicon> = netz.hole("/api/icons/aktiv")

    suspend fun waehlen(packId: String?) = netz.ohneAntwort("/api/icons/aktiv", "PUT", json(Packwahl(packId)))
}
