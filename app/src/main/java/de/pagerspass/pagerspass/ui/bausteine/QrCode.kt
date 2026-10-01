package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Rundung

/**
 * Ein QR-Code, auf dem Gerät gezeichnet — das Gegenstück zu `QrCode.vue`.
 *
 * <b>Kein Netzwerkaufruf, keine fremde Stelle.</b> Wie im Web entsteht der Code
 * hier selbst; `com.google.zxing:core` liegt für den Sucher des Begleiters
 * ohnehin schon in der App und schreibt QR-Codes genauso, wie es sie liest.
 *
 * <b>Dunkel auf Weiß, auch im dunklen Design.</b> Viele Kamera-Apps lesen
 * invertierte Codes nicht — ein schwarzer Code auf schwarzem Grund wäre hübsch
 * und nutzlos. Deshalb dieselben Farben wie im Web (`#0b0f14` auf `#fff`) und ein
 * eigener weißer Rand, damit die Ruhezone nicht vom Seitengrund abhängt.
 *
 * @param wert Was der Code tragen soll — meist ein Link.
 * @param groesse Kantenlänge der Zeichenfläche ohne den weißen Rand.
 */
@Composable
fun QrCode(
    wert: String,
    modifier: Modifier = Modifier,
    groesse: Dp = 128.dp,
    beschreibung: String = "QR-Code zum Beitreten: $wert",
) {
    val matrix = remember(wert) { qrMatrix(wert) } ?: return

    Canvas(
        modifier = modifier
            .background(Color.White, Rundung.Klein)
            .padding(Abstand.Klein)
            .size(groesse)
            .semantics { contentDescription = beschreibung },
    ) {
        val seite = matrix.width
        val zelle = size.minDimension / seite
        // Ganze Pixel je Modul wären schärfer, ließen den Code aber je nach
        // Dichte um einige Pixel schrumpfen. Ein Hauch Überlappung verhindert
        // stattdessen die haarfeinen hellen Fugen zwischen Nachbarmodulen.
        val modul = Size(zelle + 0.5f, zelle + 0.5f)
        for (y in 0 until seite) {
            for (x in 0 until seite) {
                if (matrix[x, y]) {
                    drawRect(QR_DUNKEL, topLeft = Offset(x * zelle, y * zelle), size = modul)
                }
            }
        }
    }
}

/** Die Module des Codes — mit dem Rand eines Moduls wie im Web (`margin: 1`). */
private fun qrMatrix(wert: String): BitMatrix? {
    if (wert.isBlank()) return null
    return runCatching {
        QRCodeWriter().encode(
            wert,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8"),
        )
    }.getOrNull()
}

private val QR_DUNKEL = Color(0xFF0B0F14)
