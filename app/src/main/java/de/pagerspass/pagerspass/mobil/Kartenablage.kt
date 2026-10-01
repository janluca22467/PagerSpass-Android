package de.pagerspass.pagerspass.mobil

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Die Ablage für geteilte Bilder — die Schichtkarte, die an Discord und Co. geht.
 *
 * <b>Warum ein eigener Anbieter und kein FileProvider.</b> Das Teilen braucht eine
 * `content://`-Adresse, die eine andere App lesen darf; ein Dateipfad geht seit
 * Android 7 nicht mehr hinaus. FileProvider brächte dafür eine Pfadbeschreibung in
 * XML mit, die mehr freigeben kann, als gemeint ist. Dieser hier kann genau eins:
 * ein PNG aus `cache/karten/` lesend herausgeben, und nur an die App, der das
 * Teilen-Blatt die Adresse mit `FLAG_GRANT_READ_URI_PERMISSION` reicht
 * (`exported="false"`). Ein Name mit Pfadteilen wird abgewiesen.
 */
class Kartenablage : ContentProvider() {

    override fun onCreate(): Boolean = true

    private fun datei(uri: Uri): File {
        val name = uri.lastPathSegment.orEmpty()
        if (!NAME.matches(name)) throw FileNotFoundException("Unbekannte Karte.")
        val f = File(ordner(context ?: throw FileNotFoundException("Kein Zusammenhang.")), name)
        if (!f.isFile) throw FileNotFoundException("Die Karte gibt es nicht mehr.")
        return f
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("Nur lesend.")
        return ParcelFileDescriptor.open(datei(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "image/png"

    /** Name und Größe — manche Ziele zeigen sonst „unbenannt" und 0 Byte. */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val f = datei(uri)
        val spalten = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val zeiger = MatrixCursor(spalten)
        zeiger.addRow(
            spalten.map {
                when (it) {
                    OpenableColumns.DISPLAY_NAME -> f.name
                    OpenableColumns.SIZE -> f.length()
                    else -> null
                }
            },
        )
        return zeiger
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private val NAME = Regex("""^[A-Za-z0-9_-]{1,64}\.png$""")

        private fun ordner(zusammenhang: Context) = File(zusammenhang.cacheDir, "karten").apply { mkdirs() }

        /** Ein Bild ablegen und die Adresse zum Teilen zurückgeben. */
        fun ablegen(zusammenhang: Context, name: String, bild: Bitmap): Uri {
            val sauber = name.replace(Regex("[^A-Za-z0-9_-]"), "").take(60).ifBlank { "karte" } + ".png"
            val f = File(ordner(zusammenhang), sauber)
            f.outputStream().use { bild.compress(Bitmap.CompressFormat.PNG, 100, it) }
            return Uri.Builder()
                .scheme("content")
                .authority("${zusammenhang.packageName}.karten")
                .appendPath(sauber)
                .build()
        }
    }
}
