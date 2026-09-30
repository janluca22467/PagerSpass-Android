package de.pagerspass.pagerspass.mobil

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Iconwege
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Packicon
import de.pagerspass.pagerspass.ui.schmuck.bildHolen
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

/**
 * Das Icon-Pack, das auf der Weltkarte gilt — `components/welt/iconpack.ts`.
 *
 * <b>Ein Pack ist Zier.</b> Kommt die Liste nicht (kein Abo, kein Netz, Pack
 * gesperrt), bleibt die Karte bei ihren gezeichneten Pfeilen — eine Meldung
 * gibt es dafür nicht. Dieselbe Haltung wie im Web.
 *
 * <b>Die Bilder liegen fertig bereit, bevor gezeichnet wird.</b> Die Karte
 * zeichnet fünfmal je Sekunde; sie darf dabei nicht auf ein Bild warten. Hier
 * wird jedes Icon einmal geholt (über den Zwischenspeicher des Bildladers), und
 * die Karte fragt nur nach, was schon da ist.
 */
object Iconpack {

    /** Die Icons nach Fahrzeugtyp (`vorlageId`). */
    var icons by mutableStateOf<Map<String, Packicon>>(emptyMap())
        private set

    /** Die geladenen Grafiken nach Fahrzeugtyp. */
    var bilder by mutableStateOf<Map<String, ImageBitmap>>(emptyMap())
        private set

    private var server = ""

    fun wege(zusammenhang: Context): Iconwege = Iconwege(Netz(Ablage(zusammenhang.applicationContext)))

    /** Die volle Adresse eines Bildes — der Server liefert `/api/icon/…`. */
    fun adresse(bild: String): String = if (bild.startsWith("http")) bild else "${server.trimEnd('/')}$bild"

    suspend fun serverKennen(zusammenhang: Context): String {
        server = Ablage(zusammenhang.applicationContext).server()
        return server
    }

    /** Das gültige Pack laden — nach dem Betreten der Welt und nach jeder Wahl. */
    suspend fun laden(zusammenhang: Context) {
        serverKennen(zusammenhang)
        val liste = runCatching { wege(zusammenhang).aktiv() }.getOrNull() ?: return
        icons = liste.associateBy { it.vorlageId }
        bilder = coroutineScope {
            liste.map { icon -> async { bildHolen(adresse(icon.bild))?.let { icon.vorlageId to it } } }
                .awaitAll()
                .filterNotNull()
                .toMap()
        }
    }

    fun vergessen() {
        icons = emptyMap()
        bilder = emptyMap()
    }
}

private val BLAU = Color(0xFF4D8DFF)
private val GELB = Color(0xFFFFC247)

/**
 * Ein Fahrzeug als Icon des Packs zeichnen — `fahrzeugMarkup` im Web.
 *
 * Gibt `false` zurück, wenn für diesen Typ nichts im Pack liegt; dann zeichnet
 * die Karte ihren Pfeil wie bisher. Gedreht wird nur, was der Besitzer auf
 * „dreht" gestellt hat. Mit gesetzten Blaulichtern blitzt es genau dort — Takt
 * `b` eine halbe Periode nach `a`, das Wechselblitzen echter Anlagen; ohne
 * Lichter blitzt wie bisher ein Ring ums ganze Bild.
 *
 * @param blink Die Phase des Blitzens — die Karte wechselt sie selbst.
 */
fun DrawScope.packFahrzeug(
    vorlageId: String,
    p: Offset,
    kurs: Float,
    blaulicht: Boolean,
    blink: Boolean,
): Boolean {
    val icon = Iconpack.icons[vorlageId] ?: return false
    val bild = Iconpack.bilder[vorlageId] ?: return false
    val hoehe = 30.dp.toPx()
    val verhaeltnis = if ((icon.hoehe ?: 0) > 0 && icon.breite != null) icon.breite.toFloat() / icon.hoehe!! else bild.width.toFloat() / bild.height.coerceAtLeast(1)
    val breite = hoehe * verhaeltnis
    val links = p.x - breite / 2
    val oben = p.y - hoehe / 2

    rotate(if (icon.dreht) kurs else 0f, p) {
        drawImage(
            bild,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bild.width, bild.height),
            dstOffset = IntOffset(links.roundToInt(), oben.roundToInt()),
            dstSize = IntSize(breite.roundToInt().coerceAtLeast(1), hoehe.roundToInt().coerceAtLeast(1)),
        )
        if (blaulicht && icon.blaulichter.isNotEmpty()) {
            icon.blaulichter.forEach { l ->
                val an = if (l.takt == "b") !blink else blink
                if (!an) return@forEach
                val farbe = if (l.farbe == "gelb") GELB else BLAU
                val mitte = Offset(links + (l.x * breite).toFloat(), oben + (l.y * hoehe).toFloat())
                val bb = ((l.b ?: 0.16) * breite).toFloat()
                val hh = ((l.h ?: 0.16) * hoehe).toFloat()
                drawCircle(farbe.copy(alpha = 0.35f), maxOf(bb, hh) * 1.2f, mitte)
                if (l.form == "eckig") {
                    drawRect(farbe, mitte - Offset(bb / 2, hh / 2), androidx.compose.ui.geometry.Size(bb, hh))
                } else {
                    drawCircle(farbe, maxOf(bb, hh) / 2, mitte)
                }
            }
        }
    }
    if (blaulicht && icon.blaulichter.isEmpty() && blink) {
        drawCircle(BLAU, maxOf(breite, hoehe) / 2 + 3.dp.toPx(), p, style = Stroke(2.dp.toPx()))
    }
    return true
}
