package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Die Zeichen der Welt-Werkzeuge — die Pfade aus `components/welt/werkzeuge.ts`.
 *
 * <b>Dieselben Striche wie im Web, übernommen und nicht nachgezeichnet.</b> Wer
 * zwischen Browser und App wechselt, soll dieselben Reiter an denselben Zeichen
 * wiedererkennen. Sie stehen hier und nicht in `Zeichen.kt`, weil nur die Welt
 * sie benutzt.
 */
object Weltzeichen {
    val Lagen = strich("lagen", "M12 4 21.5 20h-19Z", "M12 10v4.5", "M12 17.4h.01")
    val Fahrzeuge = strich(
        "fahrzeuge",
        "M3 5h11v10H3Z",
        "M14 8.5h3.4l3.6 3.5V15h-3.5",
        "M6.8 15a1.9 1.9 0 1 1-.01 0Z",
        "M17.5 15a1.9 1.9 0 1 1-.01 0Z",
    )
    val Wachen = strich("wachen", "m3 11 9-7 9 7", "M5 10v10h14V10", "M10 20v-5h4v5")
    val Grosslage = strich("grosslage", "M12 3a9 9 0 1 1-.01 0Z", "M12 8v5", "M12 16.6h.01")
    val Kasse = strich("kasse", "M4 4h16v16H4Z", "m12 8 3.5 4-3.5 4-3.5-4Z")
    val Rangliste = strich("rangliste", "M9 19V5h6v14", "M3 19v-9h6v9", "M15 19v-6h6v6", "M2.5 19h19")
    val Leihe = strich("leihe", "M3 9h14", "m14 6 3 3-3 3", "M21 15H7", "m10 18-3-3 3-3")
    val Einstellungen = strich(
        "einstellungen",
        "M12 3.5a8.5 8.5 0 1 1-.01 0Z",
        "M12 8.5a3.5 3.5 0 1 1-.01 0Z",
        "M12 2.5v2",
        "M12 19.5v2",
        "M2.5 12h2",
        "M19.5 12h2",
    )
    val Chat = strich("chat", "M3.5 4.5h17v11H10l-4.5 4v-4H3.5Z", "M7 8.5h10", "M7 11.5h6")
    val Bauen = strich("bauen", "M12 5v14", "M5 12h14")
    val Kauf = strich(
        "kauf",
        "M2.5 17.5h11v-5h2.5l2 2v3h-1.5",
        "M5 17.5a1.5 1.5 0 1 0 3 0 1.5 1.5 0 1 0-3 0",
        "M13 17.5a1.5 1.5 0 1 0 3 0 1.5 1.5 0 1 0-3 0",
        "M18.5 5v6",
        "M15.5 8h6",
    )
    val Laufbahn = strich("laufbahn", "M3 19.5h5.5v-5H14v-5h5.5v-5", "M3 19.5h18")
    val Mehr = strich("mehr", "M5 12h.01", "M12 12h.01", "M19 12h.01", staerke = 2.6f)
    val Zurueck = strich("zurueck", "M19 12H5", "m11 6-6 6 6 6")
    val Wache = strich("wache", "m3 11 9-7 9 7", "M5 10v10h14V10", "M12 13.5a2.5 2.5 0 1 1-.01 0Z")

    /** Die Stecknadel aus `Bauortwahl.vue`: der Punkt, um den es geht. */
    val Stecknadel = strich(
        "stecknadel",
        "M12 21s7-6.3 7-11a7 7 0 1 0-14 0c0 4.7 7 11 7 11Z",
        "M12 12.5a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5Z",
    )

    /** Der Winkel der Erfahrung — `Erfahrungszeichen.vue`. */
    val Erfahrung = strich("erfahrung", "M4.5 16.5 12 8l7.5 8.5", staerke = 2.2f)

    /** Die Raute der Welt-Credits — `Waehrung.vue`. */
    val Waehrung = strich("waehrung", "M12 3.5 20.5 12 12 20.5 3.5 12Z")

    /** Der Winkel rechts an einer Menüzeile: „hier geht es weiter“. */
    val Weiter = strich("weiter", "m9 6 6 6-6 6")
    val Kreuz = strich("kreuz", "m6 6 12 12", "M18 6 6 18")

    // Die drei Kartenknöpfe aus `Weltkarte.vue`: einpassen, Ansicht, Ebenen.
    val Einpassen = strich("einpassen", "M9 4H4v5", "M15 4h5v5", "M15 20h5v-5", "M9 20H4v-5")
    val Ansicht = strich("ansicht", "M9 4 3 6.5v13L9 17l6 3 6-2.5v-13L15 7Z", "M9 4v13", "M15 7v13")
    val Ebenen = strich("ebenen", "m12 3 9 5-9 5-9-5Z", "m3 12 9 5 9-5", "m3 16 9 5 9-5")
}

private fun strich(name: String, vararg pfade: String, staerke: Float = 1.7f): ImageVector =
    ImageVector.Builder(
        name = name,
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
                strokeLineWidth = staerke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
