package de.pagerspass.pagerspass.netz

import java.io.ByteArrayOutputStream

/**
 * Ein `multipart/form-data`-Rumpf mit beliebig vielen Teilen — das Gegenstück zu
 * `FormData` im Web.
 *
 * <b>Warum neben `Netz.hochladen`.</b> Dort steht genau ein Dateiteil, und das
 * reicht fürs Profilbild. Das Icon eines Packs braucht zwei — die Datei und
 * daneben das Textfeld `dreht` — und wird mit `PUT` geschickt, nicht mit `POST`.
 * Beides in `hochladen` hineinzubiegen hätte die eine Stelle umgebaut, an der
 * das Profilbild hängt.
 *
 * <b>Die Form ist die ganze Kunst:</b> je Teil die Trennmarke, ein Kopf, eine
 * Leerzeile, der Inhalt; am Ende die Marke mit zwei Bindestrichen dahinter. Die
 * Marke steht außerdem im Inhaltstyp — ohne sie findet der Server keinen Teil.
 */
class Mehrteil {

    private val marke = "----pagerspass${System.nanoTime()}"
    private val rumpf = ByteArrayOutputStream()

    /** Der Inhaltstyp samt Trennmarke — genau so gehört er in die Kopfzeile. */
    val inhaltstyp: String get() = "multipart/form-data; boundary=$marke"

    /** Ein Textfeld, etwa `dreht=true`. */
    fun feld(name: String, wert: String): Mehrteil = apply {
        schreiben(
            "--$marke\r\n" +
                "Content-Disposition: form-data; name=\"$name\"\r\n\r\n" +
                "$wert\r\n",
        )
    }

    /**
     * Eine Datei. Der Dateiname fährt mit — beim Zip-Import trägt er den Namen des
     * Packs (`Pack_NAME.zip`), er ist dort also Inhalt und keine Beigabe.
     */
    fun datei(name: String, dateiname: String, typ: String, daten: ByteArray): Mehrteil = apply {
        // Anführungszeichen und Zeilenumbrüche im Dateinamen würden den Kopf
        // zerreißen — sie haben dort nichts verloren.
        val sauber = dateiname.replace("\"", "").replace("\r", "").replace("\n", "")
        schreiben(
            "--$marke\r\n" +
                "Content-Disposition: form-data; name=\"$name\"; filename=\"$sauber\"\r\n" +
                "Content-Type: $typ\r\n\r\n",
        )
        rumpf.write(daten)
        schreiben("\r\n")
    }

    /** Der fertige Rumpf — mit der Schlussmarke. */
    fun bytes(): ByteArray {
        val fertig = ByteArrayOutputStream()
        fertig.write(rumpf.toByteArray())
        fertig.write("--$marke--\r\n".toByteArray(Charsets.UTF_8))
        return fertig.toByteArray()
    }

    private fun schreiben(text: String) {
        rumpf.write(text.toByteArray(Charsets.UTF_8))
    }
}

/**
 * Einen mehrteiligen Rumpf schicken und die Antwort lesen.
 *
 * Derselbe Weg wie `Netz.rohBinaer` — mit dessen Umgang mit 401, Wartung und
 * Fehlersatz —, nur mit wählbarem Verfahren.
 */
suspend inline fun <reified T> Netz.mehrteilig(
    pfad: String,
    teile: Mehrteil,
    verfahren: String = "POST",
): T = Netz.abgabe.decodeFromString(
    rohBinaer(pfad, teile.inhaltstyp, teile.bytes(), verfahren) ?: "null",
)
