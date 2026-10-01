package de.pagerspass.pagerspass.ui.schmuck

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Die Zeichen im Wachen-Emblem — übertragen aus `WachenZeichen.vue`.
 *
 * <b>Gezeichnete Linien, keine Emoji</b> — dieselbe Entscheidung wie im Web und
 * aus demselben Grund: Emoji sehen je Gerät anders aus, und das Emblem einer
 * Wache soll überall dasselbe sein. Die Ids sind die des Servers
 * (`Domain/Logic/Wachenschmuck.Emblemzeichen`).
 *
 * <b>Kreise und Rechtecke stehen hier als Pfade.</b> Das SVG des Web benutzt
 * `<circle>` und `<rect>`; ein `ImageVector` kennt nur Pfade. Umgerechnet mit
 * [kreis] und [kasten], damit die Maße aus dem Web unverändert lesbar bleiben.
 */
object Wachenzeichen {

    private val PFADE: Map<String, List<String>> = mapOf(
        // Helm: die Kalotte mit Nackenschutz, von der Seite.
        "helm" to listOf(
            "M5 15a7 7 0 0 1 14 0",
            "M3.5 15h17",
            "M12 8V5",
            "M9 15c0-3 1.3-5 3-5s3 2 3 5",
        ),
        // Florianskreuz: acht Spitzen als Achteckstern.
        "florianskreuz" to listOf(
            "M12 2.5 14.4 8 20 5.6 17.6 11.2 23 12l-5.4 1.6L20 19l-5.6-2.4L12 22l-2.4-5.4L4 19" +
                "l2.4-5.8L1 12l5.4-.8L4 5.6 9.6 8Z",
        ),
        // Hydrant: Haube, Körper, zwei Abgänge.
        "hydrant" to listOf(
            "M9 9a3 3 0 0 1 6 0v9H9Z",
            "M9.5 6.5h5",
            "M7 11h2M15 11h2",
            "M7.5 20.5h9",
        ),
        // Drehleiter: der ausgefahrene Leiterpark über dem Fahrgestell.
        "drehleiter" to listOf(
            "M4 19h16",
            "m5 16 12-9",
            "m7.5 14 1.8 2.4M10.5 11.8l1.8 2.4M13.5 9.6l1.8 2.4",
            "m3.8 16.4 3-2.2",
            kreis(6f, 19f, 1.6f),
            kreis(17f, 19f, 1.6f),
        ),
        // Funkmast: Gitterturm mit abgehenden Wellen.
        "funkmast" to listOf(
            "M9 21 12 6l3 15",
            "M10 15h4M9.4 18h5.2",
            "M7.5 8a6 6 0 0 1 0-4M16.5 8a6 6 0 0 0 0-4",
            "M5 10a9 9 0 0 1 0-8M19 10a9 9 0 0 0 0-8",
        ),
        // Anker: für alles auf dem Wasser.
        "anker" to listOf(
            kreis(12f, 5f, 2f),
            "M12 7v13",
            "M8 10h8",
            "M4.5 14a7.5 7.5 0 0 0 15 0",
            "M4.5 14v-2.5M19.5 14v-2.5",
        ),
        // Rettungsdienst: Kreuz im Ring.
        "kreuz" to listOf(
            kreis(12f, 12f, 8.5f),
            "M12 7.5v9M7.5 12h9",
        ),
        // Technische Hilfe: Zahnrad.
        "zahnrad" to listOf(
            kreis(12f, 12f, 3.2f),
            "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3",
            "m5.3 5.3 2.1 2.1M16.6 16.6l2.1 2.1M18.7 5.3l-2.1 2.1M7.4 16.6l-2.1 2.1",
        ),
        // Hubschrauber: Rotor über der Kanzel, Heckausleger.
        "rth" to listOf(
            "M4 5.5h16",
            "M12 5.5v2.5",
            "M7 12.5a4.5 4.5 0 0 1 4.5-4.5h1.5c2.5 0 4 1.7 4.5 4l3.5 1.5H7Z",
            "M15 14.2 20.5 18",
            "M19 18h3",
            "M8.5 14v3M14 14v3",
            "M7 17.5h8",
        ),
        "flamme" to listOf(
            "M12 2.5c3.5 4 5.5 6.4 5.5 10a5.5 5.5 0 0 1-11 0c0-2.2 1-3.9 2.5-5.5.4 1.2 1.1 2 2 2.3" +
                "-.4-2.4.3-4.6 1-6.8Z",
        ),
        // Stern — für die, die keins der anderen wollen.
        "stern" to listOf(
            "m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9Z",
        ),
        // Leitstelle (ab Stufe 25): Bildschirm mit Funkbogen darüber.
        "leitstelle" to listOf(
            kasten(3.5f, 8.5f, 17f, 10f, 1.5f),
            "M8 22h8M12 18.5V22",
            "M7 15h4M7 12h7",
            "M8.5 5.5a6 6 0 0 1 7 0",
            kreis(12f, 2.8f, 1f),
        ),
        // Ehrenzeichen (ab Stufe 40): Medaille am Band.
        "ehrenzeichen" to listOf(
            "M8 2.5 10.5 9M16 2.5 13.5 9",
            kreis(12f, 15f, 6.5f),
            "m12 11.3 1.4 2.9 3.1.4-2.3 2.2.6 3.1L12 18.4l-2.8 1.5.6-3.1-2.3-2.2 3.1-.4Z",
        ),
    )

    private val gebaut = mutableMapOf<String, ImageVector>()

    /** Ob es zu dieser Id eine Zeichnung gibt — sonst stehen die Initialen. */
    fun hat(name: String?): Boolean = name != null && name in PFADE

    /**
     * Die Zeichnung — weiß, damit ein `ColorFilter` sie färbt wie `currentColor`
     * im Web. `null` für „keines" und für Ids, die diese Fassung nicht kennt.
     */
    fun bild(name: String?): ImageVector? {
        val pfade = PFADE[name ?: return null] ?: return null
        return gebaut.getOrPut(name) {
            ImageVector.Builder(
                name = "wachenzeichen-$name",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                pfade.forEach { d ->
                    addPath(
                        pathData = addPathNodes(d),
                        fill = null,
                        stroke = SolidColor(Color.White),
                        strokeLineWidth = 1.7f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }.build()
        }
    }

    /** `<circle cx cy r>` als Pfad aus zwei Halbbögen. */
    private fun kreis(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"

    /** `<rect x y width height rx>` als Pfad mit gerundeten Ecken. */
    private fun kasten(x: Float, y: Float, b: Float, h: Float, r: Float): String =
        "M${x + r} ${y}h${b - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
            "h${-(b - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}Z"
}

/**
 * Ein Wachenzeichen in einer Farbe — im Schild die Schrift auf dem Wachenton,
 * in der Auswahl die Farbe des Textes.
 */
@Composable
fun WachenzeichenBild(name: String, farbe: Color, modifier: Modifier = Modifier) {
    val bild = Wachenzeichen.bild(name) ?: return
    Image(
        imageVector = bild,
        contentDescription = null,
        colorFilter = ColorFilter.tint(farbe),
        modifier = modifier,
    )
}
