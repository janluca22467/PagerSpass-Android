package de.pagerspass.pagerspass.ui.zeichen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Die Zeichen — übertragen aus `web/src/haupt/Zeichen.vue`.
 *
 * <b>Alle auf demselben 24er-Raster, alle als Strich.</b> Ein gefülltes Zeichen
 * neben einem gestrichenen sieht aus wie zwei Sätze, und die Anwendung zeigt zwei
 * Dutzend davon nebeneinander. Die einzige Ausnahme steht unten bei `Dienst` und
 * hat einen Grund, der dort erklärt ist.
 *
 * <b>Kreise sind als Bogenpfade geschrieben</b> und nicht als eigene Grundform.
 * So ist jedes Zeichen dieselbe Datenform, und der Bauplan unten braucht genau
 * eine Schleife statt einer Fallunterscheidung je Form. Die Pfaddaten sind
 * zeichengleich die des Webs — wer dort einen Strich verschiebt, verschiebt ihn
 * hier mit.
 *
 * <b>Die Farbe kommt von der Stelle, an der das Zeichen steht.</b> Im Web heißt
 * das `currentColor`; hier werden die Vektoren in Weiß gebaut und beim Zeichnen
 * eingefärbt (`Icon(..., tint = …)`). Wer ein Zeichen ohne Tönung setzt, bekommt
 * Weiß — und das ist an keiner Stelle dieser Anwendung richtig.
 *
 * <b>Warum eine feste Liste und keine Abfrage.</b> Die Symbolbibliothek des
 * Servers (`Data/footericons.json`) gehört den Fußzeilen-Knöpfen und den Kacheln
 * der Medienseite: Dort wählt jemand in der Verwaltung ein Zeichen aus. Hier
 * wählt niemand etwas aus — jedes Zeichen steht fest an seinem Abschnitt und
 * ändert sich nur, wenn dieser Abschnitt sich ändert.
 */
object Zeichen {

    // -------------------------------------------------- Die Zeichen der Seiten

    /** Leitstelle — der Kopfhörer mit Bügelmikrofon. */
    val Leitstelle = strich(
        "leitstelle",
        "M4 14v-2a8 8 0 0 1 16 0v2",
        "M4 14h2.5a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H5.5A1.5 1.5 0 0 1 4 17.5z",
        "M20 14h-2.5a1 1 0 0 0-1 1v3a1 1 0 0 0 1 1H18.5A1.5 1.5 0 0 0 20 17.5z",
        "M12 21h3a2 2 0 0 0 2-2",
    )

    /** Fahrzeug — Kabine, Aufbau, zwei Räder. */
    val Fahrzeug = strich(
        "fahrzeug",
        "M2.5 16.5V7.5h11v9",
        "M13.5 10.5h3.5l3.5 4v2",
        "M7 16.5a2 2 0 1 0 0 4 2 2 0 0 0 0-4z",
        "M17 16.5a2 2 0 1 0 0 4 2 2 0 0 0 0-4z",
    )

    /** Funk — Handsprechfunkgerät mit ausgesendeten Wellen. */
    val Funk = strich(
        "funk",
        "M7 10h8a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1z",
        "M13 10V6",
        "M9 14h4",
        "M17.5 4.5a5 5 0 0 1 0 6",
        "M20 2.5a8.5 8.5 0 0 1 0 10",
    )

    /** Melder — Gehäuse, Display, zwei Zeilen Text. */
    val Melder = strich(
        "melder",
        "M8 2.5h8a1 1 0 0 1 1 1v17a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1v-17a1 1 0 0 1 1-1z",
        "M9.5 5.5h5v5h-5z",
        "M9.5 14h5",
        "M9.5 17.5h5",
    )

    /** Karte — die gefaltete Lagekarte. */
    val Karte = strich(
        "karte",
        "M9 3 3 5.5v15L9 18l6 3 6-2.5v-15L15 6z",
        "M9 3v15",
        "M15 6v15",
    )

    /** Bots — der Kasten mit den zwei Augen. */
    val Bot = strich(
        "bot",
        "M6 8.5h12a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1z",
        "M12 5.5v3",
        "M9.5 12.5v1.5",
        "M14.5 12.5v1.5",
        "M2.5 11.5v4",
        "M21.5 11.5v4",
    )

    /** World — der Globus mit Äquator und Meridian. */
    val Welt = strich(
        "welt",
        "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z",
        "M3.2 9.5h17.6",
        "M3.2 14.5h17.6",
        "M12 3c2.5 2.6 3.8 5.6 3.8 9s-1.3 6.4-3.8 9c-2.5-2.6-3.8-5.6-3.8-9S9.5 5.6 12 3z",
    )

    /** Dienstbuch — das aufgeschlagene Buch mit zwei Zeilen. */
    val Dienstbuch = strich(
        "dienstbuch",
        "M5 4.5A1.5 1.5 0 0 1 6.5 3H19v18H6.5A1.5 1.5 0 0 1 5 19.5z",
        "M19 17.5H6.5A1.5 1.5 0 0 0 5 19",
        "M9 7.5h6",
        "M9 11h6",
    )

    /** Gemeinschaft — zwei Leute, eine davon halb dahinter. */
    val Gemeinschaft = strich(
        "gemeinschaft",
        "M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z",
        "M2.5 20a6.5 6.5 0 0 1 13 0",
        "M16.5 10.5a3 3 0 1 0 0-6",
        "M17 13.7a6.5 6.5 0 0 1 4.5 6.3",
    )

    /** Lehrgang — der Hut mit Quaste. */
    val Lehrgang = strich(
        "lehrgang",
        "M12 4 2.5 8.5 12 13l9.5-4.5z",
        "M6.5 10.7V16c0 1.7 2.5 3 5.5 3s5.5-1.3 5.5-3v-5.3",
        "M21.5 8.5V15",
    )

    /** Handy — das Gerät in der Hosentasche. */
    val Handy = strich(
        "handy",
        "M8 2.5h8a1.5 1.5 0 0 1 1.5 1.5v16a1.5 1.5 0 0 1-1.5 1.5H8A1.5 1.5 0 0 1 6.5 20V4A1.5 1.5 0 0 1 8 2.5z",
        "M10.5 18.5h3",
    )

    /** Notruf — der Hörer. */
    val Notruf = strich(
        "notruf",
        "M7.5 3.5 10 8l-2 2c1 2.5 3.5 5 6 6l2-2 4.5 2.5-1 3.5c-.3 1-1.3 1.6-2.3 1.4C10.5 20.3 3.7 13.5 2.6 6.8 2.4 5.8 3 4.8 4 4.5z",
    )

    /** Lage — die Flamme. */
    val Lage = strich(
        "lage",
        "M12 2.5c4 4 6.5 6.8 6.5 10.5a6.5 6.5 0 0 1-13 0c0-2 1-3.6 2.5-5 .2 1.4.9 2.4 2 2.9-.6-3.4.3-6.1 2-8.4z",
    )

    /** Übung — die Uhr auf der Zeitachse. */
    val Uebung = strich(
        "uebung",
        "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z",
        "M12 7v5.5l3.5 2",
    )

    /** Wiki — das Blatt mit umgeschlagener Ecke. */
    val Wiki = strich(
        "wiki",
        "M5.5 2.5h8L19 8v13.5h-13.5z",
        "M13.5 2.5V8H19",
        "M8.5 12.5h7",
        "M8.5 16h7",
    )

    /** Kanäle — das Sendezeichen. */
    val Kanal = strich(
        "kanal",
        "M12 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4z",
        "M8.2 8.2a5.5 5.5 0 0 0 0 7.6",
        "M15.8 8.2a5.5 5.5 0 0 1 0 7.6",
        "M5.4 5.4a9.5 9.5 0 0 0 0 13.2",
        "M18.6 5.4a9.5 9.5 0 0 1 0 13.2",
    )

    /** Forum — die Sprechblase. */
    val Forum = strich(
        "forum",
        "M3.5 5.5h17v11h-9L7 21v-4.5H3.5z",
        "M7.5 9.5h9",
        "M7.5 12.5h6",
    )

    /** Der Weg hinaus — der Winkel nach rechts. */
    val Weiter = strich("weiter", "m9.5 6 6 6-6 6")

    /**
     * Der Weg zurück — derselbe Winkel, gespiegelt.
     *
     * Im Web gibt es ihn nicht als Zeichen: Dort trägt der Browser den
     * Zurück-Knopf. Unter Android trägt ihn jede Seite selbst, also braucht die
     * Liste ihn. Gespiegelt und nicht neu gezeichnet, damit beide Winkel
     * denselben Schwung haben.
     */
    val Zurueck = strich("zurueck", "m14.5 6-6 6 6 6")

    // ------------------------------------------- Die sechs Wege der Tableiste
    //
    // Sie stehen getrennt von der Liste darüber, weil sie im Web auch getrennt
    // stehen: Die Zeichen oben gehören der Hauptseite und werden dort über ihren
    // Namen ausgewählt; diese hier sind in die Leiste hineingezeichnet und
    // gehören genau ihr. Ihr Strich ist um ein Zehntel kräftiger (1,7 statt
    // 1,6) — sie stehen kleiner und weiter unten als alles andere.

    /**
     * Dienst — die drei sendenden Balken.
     *
     * <b>Das eine gefüllte Zeichen.</b> Es ist dasselbe wie über dem Login und im
     * Programmsymbol; wer es dort gesehen hat, erkennt hier den Weg nach Hause.
     * Als Strichzeichnung wäre es das nicht mehr — bei drei parallelen Balken auf
     * 22 Punkten fallen Kontur und Fläche zusammen und es wird ein grauer Fleck.
     */
    val WegDienst = flaeche(
        "weg-dienst",
        rundesRechteck(4.5f, 13f, 4f, 7f, 1.4f),
        rundesRechteck(10f, 7f, 4f, 13f, 1.4f),
        rundesRechteck(15.5f, 10.5f, 4f, 9.5f, 1.4f),
    )

    /** Dienstbuch — das aufgeschlagene Buch. */
    val WegBuch = strich(
        "weg-buch",
        "M4 5.5h5a3 3 0 0 1 3 3V20a2.5 2.5 0 0 0-2.5-2.5H4Z",
        "M20 5.5h-5a3 3 0 0 0-3 3V20a2.5 2.5 0 0 1 2.5-2.5H20Z",
        staerke = 1.7f,
    )

    /** Wache — die Halle mit Tor. */
    val WegWache = strich(
        "weg-wache",
        "M3 10.5 12 4l9 6.5",
        "M5 10.5V20h14v-9.5",
        "M9.5 20v-5.5h5V20",
        staerke = 1.7f,
    )

    /** Freunde — zwei Köpfe, einer halb dahinter. */
    val WegFreunde = strich(
        "weg-freunde",
        "M6 8.5a3.5 3.5 0 1 0 7 0 3.5 3.5 0 1 0-7 0z",
        "M3.5 20a6 6 0 0 1 12 0",
        "M16 5.6a3.5 3.5 0 0 1 0 5.8",
        "M17.5 14.9a6 6 0 0 1 3 5.1",
        staerke = 1.7f,
    )

    /** Shop — die Einkaufstasche mit Henkel. */
    val WegShop = strich(
        "weg-shop",
        "M5 8h14l-1 12H6Z",
        "M9 10.5V6.5a3 3 0 0 1 6 0v4",
        staerke = 1.7f,
    )

    // ------------------------------------------- Die fünf Reiter der Lobby
    //
    // Sie stehen getrennt von den Wegen der Tableiste, weil sie keine Wege sind:
    // Man verlässt die Lobby nicht, man sieht sie anders an. Im Web tragen sie
    // aus demselben Grund eigene Klassennamen (`lreiter` statt `weg`).

    /** Rolle — das Zielkreuz: wo stehe ich? */
    val LobbyRolle = strich(
        "lobby-rolle",
        "M12 3.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 0 0 0-17z",
        "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z",
        "M12 3.5V9",
        "m4.6 16.2 3.9-2.2",
        "m19.4 16.2-3.9-2.2",
        staerke = 1.7f,
    )

    /** Runde — die Einstellungen als Schieberegler. */
    val LobbyRunde = strich(
        "lobby-runde",
        "M5 7h14",
        "M5 12h14",
        "M5 17h14",
        "M7 7a2 2 0 1 0 4 0 2 2 0 1 0-4 0z",
        "M13 12a2 2 0 1 0 4 0 2 2 0 1 0-4 0z",
        "M8.5 17a2 2 0 1 0 4 0 2 2 0 1 0-4 0z",
        staerke = 1.7f,
    )

    /** Mannschaft — dieselben zwei Köpfe wie bei den Freunden. */
    val LobbyMannschaft = WegFreunde

    /** Chat — die Sprechblase mit Schwanz. */
    val LobbyChat = strich(
        "lobby-chat",
        "M20.5 12.2c0 4-3.8 7.2-8.5 7.2-1 0-2-.15-2.9-.42L4 20.5l1.6-3.6A6.9 6.9 0 0 1 3.5 12.2" +
            "C3.5 8.2 7.3 5 12 5s8.5 3.2 8.5 7.2Z",
        staerke = 1.7f,
    )

    /** Mehr — die drei Punkte. */
    val LobbyMehr = strich(
        "lobby-mehr",
        "M4.4 12a1.6 1.6 0 1 0 3.2 0 1.6 1.6 0 1 0-3.2 0z",
        "M10.4 12a1.6 1.6 0 1 0 3.2 0 1.6 1.6 0 1 0-3.2 0z",
        "M16.4 12a1.6 1.6 0 1 0 3.2 0 1.6 1.6 0 1 0-3.2 0z",
        staerke = 1.7f,
    )

    /** Konto — der Dienstausweis. */
    val WegKonto = strich(
        "weg-konto",
        rundesRechteck(3f, 5f, 18f, 14f, 2.5f),
        "M7 11a2 2 0 1 0 4 0 2 2 0 1 0-4 0z",
        "M5.8 16.2a3.6 3.6 0 0 1 6.4 0",
        "M14.5 10h4",
        "M14.5 13.5h4",
        staerke = 1.7f,
    )

    /**
     * Begleiter — der QR-Code: drei Ecken und ein loses Feld. Nur mit Premium in
     * der Leiste, wie `/scan` in `MobilTableiste.vue`.
     */
    val WegScan = strich(
        "weg-scan",
        rundesRechteck(4f, 4f, 6.5f, 6.5f, 1.2f),
        rundesRechteck(13.5f, 4f, 6.5f, 6.5f, 1.2f),
        rundesRechteck(4f, 13.5f, 6.5f, 6.5f, 1.2f),
        "M14 14h2.6v2.6H14Z",
        "M17.5 17.5H20V20h-2.5Z",
        staerke = 1.7f,
    )
}

/**
 * Ein Zeichen aus Strichen.
 *
 * Die Vorgaben sind die des Webs: 24er-Raster, `fill: none`, runde Enden und
 * runde Ecken. Ohne die runden Enden bricht jeder Winkel des Satzes spitz ab
 * und die Zeichen sehen billiger aus, als sie gezeichnet sind.
 */
private fun strich(name: String, vararg pfade: String, staerke: Float = 1.6f): ImageVector =
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

/** Ein Zeichen aus Flächen — siehe die Begründung an `Zeichen.WegDienst`. */
private fun flaeche(name: String, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        pfade.forEach { d -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color.White)) }
    }.build()

/**
 * Ein rundes Rechteck als Pfaddaten.
 *
 * Im Web steht dafür `<rect rx="…">`; Pfaddaten kennen diese Grundform nicht.
 * Statt die Zahlen von Hand auszurechnen und den Rechenweg im Quelltext zu
 * verlieren, rechnet die Zeile hier — dann steht am Aufrufpunkt dasselbe da wie
 * im SVG: Ort, Größe, Radius.
 */
private fun rundesRechteck(x: Float, y: Float, breite: Float, hoehe: Float, r: Float): String =
    "M${x + r} $y" +
        "H${x + breite - r}A$r $r 0 0 1 ${x + breite} ${y + r}" +
        "V${y + hoehe - r}A$r $r 0 0 1 ${x + breite - r} ${y + hoehe}" +
        "H${x + r}A$r $r 0 0 1 $x ${y + hoehe - r}" +
        "V${y + r}A$r $r 0 0 1 ${x + r} ${y}z"
