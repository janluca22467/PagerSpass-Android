package de.pagerspass.pagerspass.ui.teilen

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Eine Datei ins Teilen-Blatt geben — `navigator.share({ files })` des Webs.
 *
 * <b>Als Datei, nicht als Text.</b> Die Schichtkarte ist ein Bild, und der
 * Schichtenauszug soll als `.csv` in der Tabellenkalkulation oder im Discord
 * landen, nicht als Textblock in einer Nachricht. Deshalb schreibt diese Stelle
 * den Inhalt in den Cache-Ordner `geteilt/` und reicht ihn über den
 * FileProvider (`<paket>.dateien`, siehe AndroidManifest und
 * `res/xml/dateien.xml`) mit befristeter Leseerlaubnis weiter.
 *
 * <b>Der Ordner räumt sich selbst auf:</b> Vor jedem Teilen wird er geleert. Was
 * das letzte Mal geteilt wurde, hat die Ziel-App längst gelesen — und der Cache
 * darf ohnehin jederzeit vom System geleert werden.
 */
object Dateiteilen {

    /** Der Ordner im Cache — derselbe Name wie `path` in `res/xml/dateien.xml`. */
    private const val ORDNER = "geteilt"

    /** Die Kennung des FileProviders — `${applicationId}.dateien` im Manifest. */
    private fun kennung(zusammenhang: Context): String = "${zusammenhang.packageName}.dateien"

    /**
     * Schreibt [inhalt] als [name] und öffnet das Teilen-Blatt.
     *
     * Blockiert kurz fürs Schreiben — bei einer Karte von 1200 × 630 und einem
     * Auszug von ein paar Hundert Zeilen ist das nicht der Rede wert.
     *
     * @param art Der Medientyp, etwa `image/png` oder `text/csv`.
     * @param titel Die Überschrift des Blatts und der Betreff, wo die Ziel-App einen kennt.
     * @return Ein Fehler nur, wenn die Datei nicht entstand oder kein Blatt aufging.
     */
    fun teilen(
        zusammenhang: Context,
        name: String,
        art: String,
        titel: String,
        inhalt: ByteArray,
    ): Result<Unit> = runCatching {
        val ordner = File(zusammenhang.cacheDir, ORDNER)
        ordner.deleteRecursively()
        if (!ordner.mkdirs() && !ordner.isDirectory) error("Die Datei ließ sich nicht schreiben.")
        val datei = File(ordner, name)
        datei.writeBytes(inhalt)

        val adresse = FileProvider.getUriForFile(zusammenhang, kennung(zusammenhang), datei)
        val senden = Intent(Intent.ACTION_SEND).apply {
            type = art
            putExtra(Intent.EXTRA_STREAM, adresse)
            putExtra(Intent.EXTRA_SUBJECT, titel)
            putExtra(Intent.EXTRA_TITLE, titel)
            // Die Leseerlaubnis reist mit dem ClipData — sonst sieht die Ziel-App
            // hinter dem Blatt nur eine Adresse, die sie nicht öffnen darf.
            clipData = ClipData.newRawUri(name, adresse)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val blatt = Intent.createChooser(senden, titel)
        if (zusammenhang !is Activity) blatt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        zusammenhang.startActivity(blatt)
    }
}
