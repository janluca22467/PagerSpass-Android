package de.pagerspass.pagerspass.mobil

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Ein gewähltes Bild auf Icon-Maß bringen — bevor es überhaupt losgeschickt wird.
 *
 * Das Gegenstück zu `web/src/utils/iconbild.ts`. <b>Warum das am Gerät geschieht
 * und nicht am Server:</b> Die Grenzen sind eng (ein Viertelmegabyte je Icon,
 * höchstens 1024 Punkte je Kante). Ein Foto vom Telefon reißt beide, und der
 * Absender bekäme eine Absage für etwas, das er nicht beeinflussen kann.
 * Eingepasst kommt immer etwas an, das durchgeht. Der Server prüft trotzdem
 * noch einmal alles — diese Datei ist Bequemlichkeit, keine Sicherung.
 */
object Iconbild {

    /**
     * Die Kantenlänge, auf die eingepasst wird — `FELD` im Web.
     *
     * Eine Marke steht auf der Karte 42 Punkte hoch; 512 sind großzügig genug für
     * scharfe Ränder bei dreifacher Punktdichte und klein genug, dass ein WebP
     * davon gewöhnlich unter fünfzig Kilobyte bleibt.
     */
    const val FELD = 512

    /** Dieselbe Güte wie im Web (`toBlob(…, 'image/webp', 0.92)`). */
    private const val GUETE = 92

    /** Was beim Einpassen herauskam — die Maße stehen dabei, damit der Hinweis sie nennen kann. */
    class Eingepasst(
        val webp: ByteArray,
        val breite: Int,
        val hoehe: Int,
        /** Ob überhaupt verkleinert wurde. Sonst ist es nur eine Umwandlung nach WebP. */
        val verkleinert: Boolean,
    )

    /**
     * Passt ein Bild in das Feld ein: verkleinert, wenn nötig, und rechnet nach WebP.
     *
     * <b>Ohne zu vergrößern und ohne zu beschneiden.</b> Ein Icon von 96 Punkten
     * wird nicht auf 512 gerechnet, und ein breites Wappen bleibt breit.
     *
     * <b>`ImageDecoder` und nicht `BitmapFactory`:</b> Er verkleinert schon beim
     * Lesen (ein 12-Megapixel-Foto kommt gar nicht erst voll in den Speicher) und
     * richtet ein Foto nach seiner EXIF-Lage auf — so, wie es der Browser auch tut.
     *
     * Läuft blockierend — nur aus einem Nebenlauf rufen.
     */
    fun einpassen(aufloeser: ContentResolver, quelle: Uri): Eingepasst {
        var faktor = 1.0
        var breite = 0
        var hoehe = 0

        val bild = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(aufloeser, quelle)) { dekoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                faktor = min(1.0, min(FELD.toDouble() / w, FELD.toDouble() / h))
                breite = max(1, (w * faktor).roundToInt())
                hoehe = max(1, (h * faktor).roundToInt())
                dekoder.setTargetSize(breite, hoehe)
                // Aus dem Arbeitsspeicher und nicht von der Grafikkarte: Ein Bild
                // auf der Grafikkarte lässt sich nicht zuverlässig wieder packen.
                dekoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrElse {
            throw IllegalArgumentException("Das ist kein Bild, das dieses Gerät lesen kann.")
        }

        val strom = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            // Vor Android 11 gibt es nur das eine WebP — mit Güte unter 100 ist
            // auch das verlustbehaftet, also dasselbe Ergebnis.
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        val gelungen = bild.compress(format, GUETE, strom)
        bild.recycle()
        if (!gelungen) throw IllegalStateException("Das Bild ließ sich nicht umwandeln.")

        return Eingepasst(
            webp = strom.toByteArray(),
            breite = breite,
            hoehe = hoehe,
            verkleinert = faktor < 1.0,
        )
    }

    /**
     * Eine gewählte Datei ganz lesen — samt dem Namen, unter dem sie liegt.
     *
     * Der Name zählt beim Zip-Import: `Pack_NAME.zip` trägt den Namen des Packs.
     */
    fun lesen(aufloeser: ContentResolver, quelle: Uri): Pair<String, ByteArray> {
        val name = runCatching {
            aufloeser.query(quelle, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { zeiger ->
                    if (zeiger.moveToFirst()) zeiger.getString(0) else null
                }
        }.getOrNull() ?: "Pack.zip"

        val daten = aufloeser.openInputStream(quelle)?.use { it.readBytes() }
            ?: throw IllegalStateException("Die Datei ließ sich nicht öffnen.")

        return name to daten
    }
}
