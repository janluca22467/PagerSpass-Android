package de.pagerspass.pagerspass.ui.schmuck

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Das Kontobild — Wappen, Initialen oder das eigene Bild.
 *
 * Übertragen aus `web/src/components/Kontobild.vue`. Es steht überall dort, wo
 * eine Person genannt wird: im Kopf des Profils, in der Freundesliste, in der
 * Bestenliste, an einer Wachenzeile.
 *
 * <b>Liegt ein Bild vor, steht es allein.</b> Wappen und Initialen werden dann
 * gar nicht erst gezeichnet. Im Web lag das Bild einmal nur *davor* im Baum, und
 * weil der Text `position: relative` trägt, malte der Browser ihn zuletzt: Die
 * Initialen standen auf jedem Profilbild obenauf.
 *
 * <b>Fällt das Bild weg, kommt das Wappen zurück</b> — abgenommen, gelöscht,
 * nicht ladbar. Ohne Meldung: Ein Profilbild, das der Server gerade nicht
 * ausliefert, ist kein Fehler der Seite, auf der es steht.
 *
 * @param groesse Die Kantenlänge. 36 in einer Listenzeile — klein genug für die
 *   Zeile, groß genug, um die Initialen zu lesen; 84 im Kopf. <b>Es ist kein
 *   Bedienelement:</b> Gedrückt wird die Zeile darum, nicht das Bild darin.
 * @param imDienst Der grüne Ring außen. Er liegt *außen* und nimmt dem Bild
 *   nichts weg — mit einem Rand würde das Wappen bei gleicher Fläche kleiner,
 *   und eine Liste, in der die Bilder verschieden groß aussehen, je nachdem wer
 *   gerade Dienst hat, ist unruhig.
 */
@Composable
fun Kontobild(
    kennung: String,
    anzeigename: String,
    modifier: Modifier = Modifier,
    wappen: String = "Keines",
    wappenfarbe: Int = 0,
    bildAdresse: String? = null,
    rahmen: String = "keiner",
    groesse: Dp = 36.dp,
    imDienst: Boolean = false,
) {
    val ton = Wappen.ton(kennung, wappenfarbe)
    val tinte = Wappen.schrift(ton)
    val bild by bildVon(bildAdresse)
    val rahmenfarbe = Schmuck.rahmenfarbe(rahmen)
    val gross = groesse >= 60.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            // Die Ringe von außen nach innen: erst der Dienstring, dann der
            // gekaufte Rahmen. Zwei Ringe übereinander sind Absicht — der eine
            // sagt „ist gerade da", der andere „hat sich das verdient".
            .then(
                if (imDienst) {
                    Modifier
                        .border((if (gross) 3 else 2).dp, Farben.Bg, CircleShape)
                        .padding((if (gross) 3 else 2).dp)
                        .border((if (gross) 2 else 2).dp, Farben.Gruen, CircleShape)
                        .padding((if (gross) 2 else 2).dp)
                } else {
                    Modifier
                }
            )
            .then(
                if (rahmenfarbe != null) {
                    Modifier.border(2.dp, rahmenfarbe, CircleShape).padding(2.dp)
                } else {
                    Modifier
                }
            )
            .size(groesse)
            .clip(CircleShape)
            .background(if (bild == null) ton else Farben.FlaecheHoch),
    ) {
        val gerade = bild
        when {
            gerade != null -> Image(
                bitmap = gerade,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(groesse).clip(CircleShape),
            )

            wappen != "Keines" && WAPPENZEICHEN.containsKey(wappen) -> Icon(
                imageVector = WAPPENZEICHEN.getValue(wappen),
                contentDescription = null,
                tint = tinte.copy(alpha = 0.9f),
                modifier = Modifier.size(groesse * 0.55f),
            )

            else -> Text(
                text = Wappen.initialen(anzeigename),
                style = (if (gross) Schrift.Anzeige else Schrift.Klein).copy(
                    fontFamily = Schrift.Sans,
                    fontWeight = FontWeight.Bold,
                ),
                color = tinte,
            )
        }
    }
}

/**
 * Die Zeichen der Organisationen im Wappen.
 *
 * <b>Sie liegen über den Initialen, nicht neben ihnen</b> — nebeneinander
 * bräuchte es die doppelte Breite in jeder Listenzeile. Wer ein Zeichen trägt,
 * zeigt kein Kürzel; das ist die Wahl, die man im Profil trifft.
 *
 * Die Pfade sind zeichengleich die aus `Kontobild.vue`. Anders als die Zeichen
 * in `ui/zeichen/` sind sie **Flächen**, keine Striche: In einem 36 Punkte
 * großen Kreis fiele eine Kontur mit der Fläche zusammen.
 */
private val WAPPENZEICHEN: Map<String, ImageVector> = mapOf(
    "Feuerwehr" to flaechenzeichen(
        "feuerwehr",
        "M12 3c2 3.5-1 4.5-1 7a3 3 0 0 0 6 0c0-1-.3-2-.8-2.8C17.7 9 19 11.3 19 13.5a7 7 0 1 1-14 0" +
            "C5 9 9 7 12 3Z",
    ),
    "Rettungsdienst" to flaechenzeichen(
        "rettungsdienst",
        "M9.5 3h5v6.5H21v5h-6.5V21h-5v-6.5H3v-5h6.5Z",
    ),
    "Thw" to flaechenzeichen(
        "thw",
        "M12 8.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7Zm9 2.2-2.3-.4a6.9 6.9 0 0 0-.8-2l1.4-1.9-2-2" +
            "-1.9 1.4c-.6-.4-1.3-.7-2-.8L13 3h-2l-.4 2.3c-.7.2-1.4.4-2 .8L6.7 4.7l-2 2 1.4 1.9c-.4.6" +
            "-.6 1.3-.8 2L3 11v2l2.3.4c.2.7.4 1.4.8 2l-1.4 1.9 2 2 1.9-1.4c.6.4 1.3.6 2 .8l.4 2.3h2" +
            "l.4-2.3c.7-.2 1.4-.4 2-.8l1.9 1.4 2-2-1.4-1.9c.4-.6.6-1.3.8-2l2.3-.4Z",
    ),
    "Polizei" to flaechenzeichen(
        "polizei",
        "M12 2 4 5.5v6c0 5 3.4 9.4 8 10.5 4.6-1.1 8-5.5 8-10.5v-6Z",
    ),
    "Drehleiter" to flaechenzeichen(
        "drehleiter",
        "m4.2 19.5 12-14 1.7 1.4-12 14Zm4.3 2 12-14 1.7 1.5-12 14ZM7 16.2l1.5-1.7 3.3 2.8-1.4 1.7Z" +
            "m3-3.5 1.4-1.7 3.3 2.8-1.4 1.7Zm3-3.5 1.4-1.7 3.3 2.8-1.5 1.7Zm3-3.5L17.4 4l3.3 2.8" +
            "-1.4 1.7Z",
    ),
    // Bereich Konto: die übrigen Zeichen aus `Kontobild.vue` — Laufbahn und Abo.
    "Rth" to flaechenzeichen(
        "rth",
        "M3 4.5h18v1.7H3Zm8 1.7h2V8.5h-2ZM7.5 8.5h6.2c2.1 0 3.8 1.6 3.8 3.6 0 2-1.7 3.4-3.8 3.4" +
            "H9.5L6 13.3c-1-.6-1.2-2.1-.4-3 .5-.5 1.2-.8 1.9-.8Zm9.8 1.2 3.7-1v4.6l-3.5-1ZM6.5 17h11" +
            "v1.6h-11Zm2-1.5h1.6V17H8.5Zm6 0h1.6V17h-1.6Z",
    ),
    "Boot" to flaechenzeichen(
        "boot",
        "M3 13h18l-2.6 4.4a2 2 0 0 1-1.7 1H7.3a2 2 0 0 1-1.7-1Zm8-7.5 6.5 6H11ZM9.5 7v4.5H6.8Z",
    ),
    "Funkmast" to flaechenzeichen(
        "funkmast",
        "M11.1 8.8h1.8L15.6 21h-2l-.5-2.6h-2.2L10.4 21h-2Zm.2 7.6h1.4L12 12.9ZM12 5.2a1.9 1.9 0 1 1" +
            " 0 3.8 1.9 1.9 0 0 1 0-3.8ZM7.2 3.4l1.2 1.2a5.2 5.2 0 0 0 0 5l-1.2 1.2a6.9 6.9 0 0 1 0" +
            "-7.4Zm9.6 0a6.9 6.9 0 0 1 0 7.4l-1.2-1.2a5.2 5.2 0 0 0 0-5Z",
    ),
    "Leitstelle" to flaechenzeichen(
        "leitstelle",
        "M4 6h16a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1h-6.2l.6 2H16v1.6H8V19h1.6l.6-2H4a1 1 0 0 1-1-1V7a1" +
            " 1 0 0 1 1-1Zm2.6 2.4v6.2h1.7v-2.1h2.1v2.1h1.7V8.4h-1.7v2.4h-2.1V8.4Zm7.6 0v6.2h4.2v-1.6" +
            "h-2.5V8.4Z",
    ),
    "Bergwacht" to flaechenzeichen(
        "bergwacht",
        "M9.4 4.6 14 12.2l1.6-2.4L21 19H3ZM9.4 8l-1.9 3.1h3.8Z",
    ),
    "Wasserrettung" to flaechenzeichen(
        "wasserrettung",
        "M12 3.4a6 6 0 1 1 0 12 6 6 0 0 1 0-12Zm0 2.2a3.8 3.8 0 1 0 0 7.6 3.8 3.8 0 0 0 0-7.6ZM3" +
            " 17.6c1.6 0 1.6 1.5 3.2 1.5s1.6-1.5 3.2-1.5 1.6 1.5 3.2 1.5 1.6-1.5 3.2-1.5 1.6 1.5 3.2" +
            " 1.5V21c-1.6 0-1.6-1.4-3.2-1.4s-1.6 1.4-3.2 1.4-1.6-1.4-3.2-1.4-1.6 1.4-3.2 1.4S4.6 19.6" +
            " 3 19.6Z",
    ),
)

private fun flaechenzeichen(name: String, pfad: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(pfad),
            fill = SolidColor(Color.White),
            stroke = null,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
