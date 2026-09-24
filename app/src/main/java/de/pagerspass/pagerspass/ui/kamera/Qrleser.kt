package de.pagerspass.pagerspass.ui.kamera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Das Kamerabild mit einem QR-Leser darauf.
 *
 * <b>Warum CameraX und ZXing und nicht ML Kit.</b> Der Barcode-Leser von ML Kit
 * kommt entweder als mehrere Megabyte Modell ins Paket oder er lädt sich zur
 * Laufzeit aus den Play-Diensten nach — auf einem Gerät ohne Play Store (und
 * auf dem Emulator) scheitert er dann still. `com.google.zxing:core` ist reines
 * Java, ein paar hundert Kilobyte, und liest genau das eine Format, um das es
 * hier geht.
 *
 * <b>Der Y-Kanal ist das Bild.</b> Die Kamera liefert YUV; für einen
 * Schwarz-Weiß-Code ist die Helligkeitsebene alles, was gebraucht wird — kein
 * Umrechnen, keine Bitmap, keine Kopie des ganzen Bildes. Die Zeilenlänge
 * (`rowStride`) ist dabei **nicht** die Bildbreite: Die Kamera füllt Zeilen
 * auf, und wer das übersieht, bekommt ein schräg geschertes Bild, in dem nie
 * ein Code gefunden wird.
 *
 * <b>Gefunden wird genau einmal.</b> Der Leser sieht jedes Bild an, solange die
 * Kamera hängt; ohne die Sperre riefe er den Fund dreißigmal in der Sekunde
 * aus, und die Kopplung liefe entsprechend oft los.
 *
 * <b>Gebunden wird einmal, nicht bei jedem Neuzeichnen.</b> Das Binden an den
 * Lebenslauf gehört in einen Effekt und nicht in `update` der `AndroidView` —
 * dort liefe es bei jeder Zustandsänderung der Seite erneut, und die Kamera
 * würde im Sekundentakt ab- und wieder angemeldet.
 */
@Composable
fun Qrleser(
    beiFund: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zusammenhang = LocalContext.current
    val lebenslauf = LocalLifecycleOwner.current
    val melden by rememberUpdatedState(beiFund)

    val sicht = remember { PreviewView(zusammenhang) }

    DisposableEffect(sicht) {
        val gefunden = AtomicBoolean(false)
        val faden = Executors.newSingleThreadExecutor()
        val zukunft = ProcessCameraProvider.getInstance(zusammenhang)

        zukunft.addListener({
            val anbieter = runCatching { zukunft.get() }.getOrNull() ?: return@addListener

            val vorschau = Preview.Builder().build()
            vorschau.setSurfaceProvider(sicht.surfaceProvider)

            val pruefung = ImageAnalysis.Builder()
                // Nur das jüngste Bild: Ein Rückstau alter Bilder macht den
                // Sucher träge, ohne die Trefferquote zu erhöhen.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            pruefung.setAnalyzer(faden) { bild ->
                if (!gefunden.get()) {
                    lesen(bild)?.let { wert ->
                        // Der Fund geht auf den Hauptfaden zurück: Was daran
                        // hängt, baut Ansichten auf und stellt Verbindungen her.
                        if (gefunden.compareAndSet(false, true)) sicht.post { melden(wert) }
                    }
                }
                bild.close()
            }

            runCatching {
                anbieter.unbindAll()
                anbieter.bindToLifecycle(
                    lebenslauf,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    vorschau,
                    pruefung,
                )
            }.onFailure { Log.w("Qrleser", "Kamera nicht zu bekommen", it) }
        }, ContextCompat.getMainExecutor(zusammenhang))

        onDispose {
            // Wer die Seite verlässt, gibt die Kamera frei — sonst bleibt der
            // Sucher belegt und die Leuchte des Geräts an.
            runCatching { zukunft.get().unbindAll() }
            faden.shutdown()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            sicht.apply {
                // Das Bild füllt die Fläche und wird beschnitten — ein Sucher
                // mit schwarzen Balken sieht aus wie ein Fehler.
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
    )
}

/**
 * Der Leser — einer je Faden.
 *
 * `MultiFormatReader` hält beim Lesen Zustand und ist ausdrücklich nicht für
 * mehrere Fäden gedacht. Die Bildprüfung läuft zwar auf einem einzigen, aber
 * ein `ThreadLocal` kostet hier nichts und macht die Annahme unnötig.
 */
private val leser = ThreadLocal.withInitial {
    MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                // Der Code hängt auf einem Bildschirm, oft schräg und mit
                // Spiegelungen — ein zweiter Anlauf lohnt sich hier.
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }
}

/** Ein Kamerabild ansehen — `null`, wenn kein Code darin steht. */
private fun lesen(bild: ImageProxy): String? {
    val ebene = bild.planes.firstOrNull() ?: return null
    val puffer = ebene.buffer
    val bytes = ByteArray(puffer.remaining())
    puffer.get(bytes)

    val zeilenlaenge = ebene.rowStride
    val quelle = runCatching {
        PlanarYUVLuminanceSource(
            bytes,
            zeilenlaenge,
            bild.height,
            0,
            0,
            bild.width.coerceAtMost(zeilenlaenge),
            bild.height,
            false,
        )
    }.getOrNull() ?: return null

    val lese = leser.get() ?: return null

    return runCatching { lese.decodeWithState(BinaryBitmap(HybridBinarizer(quelle))).text }
        .also { lese.reset() }
        .getOrNull()
}
