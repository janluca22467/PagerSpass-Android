package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.em
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.bausteine.Codefeld
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Kleine Bausteine, die Dienstbuch, Lehrgang, Übungen und Leitstellenbau
 * gemeinsam brauchen — und die es im Designsystem (`ui/bausteine`) noch nicht
 * gibt, weil bisher nur eine Stelle sie gebraucht hätte.
 *
 * <b>Sie bauen auf den vorhandenen auf</b> (Feld, Knopf, Kasten, Flächen) und
 * erfinden keine neue Grundform: Wer hier eine Farbe oder einen Radius sucht,
 * findet ihn in `ui/theme`.
 */

/**
 * Reiter, die seitwärts rollen.
 *
 * <b>Warum nicht `Reiterreihe`.</b> Die teilt die Breite gleichmäßig auf — bei
 * drei Reitern richtig, bei fünf am Handy nicht: „Auswertung" stünde dann als
 * „Auswe…" da. Das Dienstbuch des Webs löst das am Handy genauso, mit einer
 * Reihe, die man schieben kann.
 */
@Composable
fun Rollreiter(
    reiter: List<String>,
    offen: Int,
    beiWahl: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(
                    color = Farben.Rand,
                    start = Offset(0f, size.height - strich / 2f),
                    end = Offset(size.width, size.height - strich / 2f),
                    strokeWidth = strich,
                )
            }
            .horizontalScroll(rememberScrollState()),
    ) {
        reiter.forEachIndexed { nr, wort ->
            val an = nr == offen
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .defaultMinSize(minHeight = Ziel.Normal)
                    .clickable(
                        onClick = { beiWahl(nr) },
                        role = Role.Tab,
                        indication = null,
                        interactionSource = null,
                    )
                    .drawBehind {
                        if (!an) return@drawBehind
                        val balken = 2.dp.toPx()
                        drawLine(
                            color = Farben.Amber,
                            start = Offset(0f, size.height - balken / 2f),
                            end = Offset(size.width, size.height - balken / 2f),
                            strokeWidth = balken,
                        )
                    }
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text(
                    text = wort,
                    style = Schrift.Normal.copy(
                        fontWeight = if (an) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (an) Farben.Amber else Farben.TextLeise,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Eine Kennzahl der Tafel: Etikett, Wert groß, darunter die Zeile, die ihn
 * einordnet. Zwei davon nebeneinander — am Handy steht die Tafel zweispaltig.
 *
 * @param zeichen SVG-Pfaddaten auf dem 24er-Raster. Mit ihnen trägt die Kachel
 *   das Zeichenquadrat des Webs (`.db-kennzahl__zeichen`) neben dem Etikett, in
 *   `farbe` getönt — und keine Kante links: Am Handy steht die Farbe im Zeichen
 *   (mobil.css). Ohne bleibt die Kachel, wie sie war.
 */
@Composable
fun Kennzahlkachel(
    etikett: String,
    wert: String,
    unter: String,
    modifier: Modifier = Modifier,
    farbe: Color = Farben.Amber,
    zeichen: String? = null,
) {
    if (zeichen != null) {
        Tafelkachel(etikett, wert, unter, zeichen, modifier, farbe)
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = modifier
            .flaeche(ecke = 9.dp)
            .drawBehind {
                drawLine(
                    color = farbe,
                    start = Offset(0f, 9.dp.toPx()),
                    end = Offset(0f, size.height - 9.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                )
            }
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Etikett(etikett)
        Text(wert, style = Schrift.Titel, color = Farben.Text, maxLines = 1)
        Text(
            unter,
            style = Schrift.Winzig,
            color = Farben.TextSehrLeise,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Vier Kennzahlen, zwei mal zwei. */
@Composable
fun Kennzahltafel(zahlen: List<@Composable (Modifier) -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        zahlen.chunked(2).forEach { paar ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                paar.forEach { it(Modifier.weight(1f)) }
                if (paar.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

/** Etikett links, Wert rechts — eine Zeile in einem Kasten. */
@Composable
fun Wertzeile(was: String, wert: String, modifier: Modifier = Modifier, farbe: Color = Farben.Text) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = was,
            style = Schrift.Klein,
            color = Farben.TextLeise,
            modifier = Modifier.weight(1f),
        )
        Text(text = wert, style = Schrift.MonoNormal, color = farbe)
    }
}

/**
 * Ein waagerechter Balken mit Name und Wert — für „Erfahrung je Organisation",
 * „welchen Wagen du fährst" und die Wochen der Auswertung.
 */
@Composable
fun Balkenzeile(name: String, wert: String, anteil: Float, farbe: Color = Farben.Amber) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(name, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
            Text(wert, style = Schrift.MonoKlein, color = Farben.TextLeise)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Farben.BgTief, Rundung.Rund),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(anteil.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(farbe, Rundung.Rund),
            )
        }
    }
}

/**
 * Die Rückmeldung nach einer Aktion — Fehler rot, sonst ruhig.
 *
 * Sie steht dort, wo gehandelt wurde (unter der Knopfleiste des Editors, über
 * der Liste), und nicht am Fuß der App: „Gespeichert. Code ABC123" ist eine
 * Auskunft, die man abschreiben will, keine Meldung, die nach drei Sekunden
 * verschwindet.
 */
@Composable
fun Rueckmeldung(meldung: String?, fehler: String?) {
    val text = fehler ?: meldung ?: return
    Text(
        text = text,
        style = Schrift.Klein,
        color = if (fehler != null) Farben.SignalHell else Farben.GruenHell,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (fehler != null) Farben.HauchSignal else Farben.Flaeche,
                randfarbe = if (fehler != null) Farben.SignalTief else Farben.Rand,
                ecke = 9.dp,
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    )
}

/**
 * „Das eines anderen übernehmen" — sechs Zeichen, ein Knopf, ein Satz dazu.
 *
 * Dieselbe Karte steht bei Übungen, Leitstellen und Rundenvorlagen; der Satz
 * darunter sagt jeweils, dass man eine **Kopie** bekommt.
 */
@Composable
fun Codeuebernahme(
    etikett: String,
    erklaerung: String,
    aktiv: Boolean,
    beiUebernehmen: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    Kasten(abstandInnen = Abstand.Klein) {
        Codefeld(wert = code, beiAenderung = { code = it.uppercase() }, etikett = etikett)
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                "Übernehmen",
                {
                    beiUebernehmen(code)
                    code = ""
                },
                aktiv = aktiv && code.trim().length >= 6,
                kompakt = true,
            )
        }
        SehrLeise(erklaerung)
    }
}

/**
 * Ein Löschknopf, der zweimal gefragt werden will.
 *
 * Erst „Löschen", dann „Wirklich löschen?" in Rot — der Weg des Webs. Eine
 * Blende wäre für eine Zeile in einer Liste zu schwer, ein einziger Druck zu
 * leicht.
 */
@Composable
fun Loeschknopf(beiLoeschen: () -> Unit, aktiv: Boolean = true) {
    var gefragt by remember { mutableStateOf(false) }
    Knopf(
        aufschrift = if (gefragt) "Wirklich löschen?" else "Löschen",
        beiDruck = {
            if (gefragt) {
                gefragt = false
                beiLoeschen()
            } else {
                gefragt = true
            }
        },
        art = if (gefragt) Knopfart.Gefahr else Knopfart.Leise,
        aktiv = aktiv,
        kompakt = true,
    )
}

/**
 * Eine Zahl mit Minus und Plus — der Regler des Webs, am Handy mit Knöpfen.
 *
 * <b>Warum keine Schiene.</b> Ein Schieberegler von 0 bis 3600 Sekunden auf
 * 300 Punkten Breite trifft man auf zwölf Sekunden genau, nicht auf dreißig.
 * Zwei Knöpfe mit festem Schritt treffen immer, und das Feld dazwischen nimmt
 * die Zahl, wenn man sie schon weiß.
 */
@Composable
fun Stufenwahl(
    etikett: String,
    wert: Int,
    beiAenderung: (Int) -> Unit,
    schritt: Int,
    von: Int,
    bis: Int,
    anzeige: (Int) -> String = { it.toString() },
) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        Etikett(etikett)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Knopf("−", { beiAenderung((wert - schritt).coerceIn(von, bis)) }, aktiv = wert > von, kompakt = true)
            Text(
                text = anzeige(wert),
                style = Schrift.MonoNormal,
                color = Farben.Text,
                modifier = Modifier.width(96.dp),
            )
            Knopf("+", { beiAenderung((wert + schritt).coerceIn(von, bis)) }, aktiv = wert < bis, kompakt = true)
        }
    }
}

/** Ein Zahlenfeld — `null`, solange nichts oder keine Zahl drinsteht. */
@Composable
fun Zahlfeld(
    etikett: String,
    wert: Number?,
    beiAenderung: (String) -> Unit,
    modifier: Modifier = Modifier,
    platzhalter: String? = null,
) {
    // Das Feld hält seinen eigenen Text: „52." ist beim Tippen keine Zahl,
    // die man zurückschreiben dürfte — sonst stünde da sofort „52.0".
    var text by remember { mutableStateOf(wert?.toString().orEmpty()) }
    LaunchedEffect(wert) {
        if (text.replace(',', '.').toDoubleOrNull() != wert?.toDouble()) text = wert?.toString().orEmpty()
    }
    Feld(
        wert = text,
        beiAenderung = {
            text = it
            beiAenderung(it)
        },
        etikett = etikett,
        platzhalter = platzhalter,
        tastatur = KeyboardType.Decimal,
        modifier = modifier,
    )
}

/**
 * Ein Zeichen aus fertigen SVG-Pfaddaten, wie Kacheln und Fußzeile sie vom
 * Server bekommen.
 *
 * `gefuellt` für die Fußzeile (im Web `fill="currentColor"`), sonst als Strich
 * wie alle übrigen Zeichen der App. Ungültige Pfaddaten dürfen nicht die ganze
 * Seite reißen — dann steht schlicht kein Zeichen da.
 */
fun pfadzeichen(pfad: String, gefuellt: Boolean): ImageVector? = runCatching {
    ImageVector.Builder(
        name = "pfad",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        if (gefuellt) {
            addPath(pathData = addPathNodes(pfad), fill = SolidColor(Color.White))
        } else {
            addPath(
                pathData = addPathNodes(pfad),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
}.getOrNull()

/** Die Organisation als Farbe — dieselben Töne wie auf der Lagekarte. */
fun orgFarbe(organisation: String?): Color = when (organisation) {
    "Feuerwehr" -> Farben.OrgFeuerwehr
    "Rettungsdienst" -> Farben.OrgRettungsdienst
    "Thw" -> Farben.OrgThw
    "Polizei" -> Farben.OrgPolizei
    else -> Farben.Amber
}

/** Minuten und Sekunden — „6:42". */
fun dauer(sekunden: Double?): String? = sekunden?.let {
    val s = Math.round(it).toInt()
    "%d:%02d".format(s / 60, s % 60)
}

/** Wie `dauer`, aber unter einer Minute in Sekunden — für Rekorde. */
fun dauerMitEinheit(sekunden: Double): String {
    val s = Math.round(sekunden).toInt()
    return if (s >= 60) "${dauer(sekunden)} min" else "$s s"
}

/** Eine Kommazahl, deutsch, eine Stelle. */
fun kommazahl(wert: Double): String =
    if (wert % 1.0 == 0.0) wert.toInt().toString() else "%.1f".format(java.util.Locale.GERMANY, wert)

// ------------------------------------------------------- Dienstbuch-Stil (v6)

/**
 * Eine Kennzahl der Tafel im Dienstbuch-Stil des Webs (`.db-kennzahl`): Zeichen
 * und Etikett in einer Zeile, darunter der Wert groß und die Zeile, die ihn
 * einordnet.
 *
 * <b>Neben `Kennzahlkachel` und nicht an ihrer Stelle</b>, weil die Kachel mit
 * der Kante links im Dienstbuch steht und dort ihren eigenen Gang geht. Am Handy
 * trägt die Kachel des Webs keine Ecke in Farbe — die Farbe steht im Zeichen.
 */
@Composable
fun Tafelkachel(
    etikett: String,
    wert: String,
    unter: String,
    zeichenpfad: String,
    modifier: Modifier = Modifier,
    farbe: Color = Farben.Amber,
) {
    Column(
        modifier = modifier
            .flaeche()
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .background(farbe.copy(alpha = 0.16f), Rundung.Winzig),
            ) {
                pfadzeichen(zeichenpfad, gefuellt = false)?.let {
                    Icon(it, contentDescription = null, tint = farbe, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                etikett.uppercase(),
                style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.03.em),
                color = Farben.TextLeise,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            wert,
            style = Schrift.Schlagzeile.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.ExtraBold),
            color = Farben.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Abstand.Winzig),
        )
        Text(
            unter,
            style = Schrift.Winzig,
            color = Farben.TextSehrLeise,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Der grüne Lernbalken der Lehrgänge (`.balken`): sechs Punkte hoch, Grün für
 * das, was schon sitzt. Daneben, wenn gegeben, die Zahl zum Nachrechnen.
 */
@Composable
fun Lernbalken(anteil: Float, modifier: Modifier = Modifier, text: String? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .background(Farben.Rand, Rundung.Rund),
        ) {
            if (anteil > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(anteil.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Farben.Gruen, Rundung.Rund),
                )
            }
        }
        if (text != null) Text(text, style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
    }
}

/**
 * Die Zeichen der Kennzahlkacheln — dieselben Pfade wie `.db-kennzahl__zeichen`
 * in Dienstbuch, Garage, Shop, Wache, Profil und Rundenliste des Webs. Kreise und
 * Rechtecke sind als Pfade ausgeschrieben, weil `pfadzeichen` nur Pfade liest.
 */
object Tafelzeichen {
    const val KALENDER = "M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z M8 3v4M16 3v4M4 10h16"
    const val WARNUNG = "M12 3 2.5 20h19L12 3Z M12 10v4M12 17.5v.01"
    const val STOPPUHR = "M4 13a8 8 0 1 0 16 0a8 8 0 1 0-16 0 M12 9v4l2.5 2.5M10 2h4"
    const val STERN = "m12 3 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z"
    const val FAHRZEUG = "M3 15V9l3-4h10l4 4v6 M3 12h17 M5 15a2 2 0 1 0 4 0a2 2 0 1 0-4 0 M15 15a2 2 0 1 0 4 0a2 2 0 1 0-4 0"
    const val HALLE = "M3 10.5 12 4l9 6.5 M5 10.5V20h14v-9.5 M8.5 20v-6h7v6"
    const val GUTSCHEIN = "M3 7h18v4a2 2 0 0 0 0 4v4H3v-4a2 2 0 0 0 0-4V7Z M14 7v12"
    const val GLUECKSRAD = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 3v9l6.4 6.4M12 12 3.5 9"
    const val TASCHE = "M4 8h16l-1 13H5L4 8Z M8 8V6a4 4 0 0 1 8 0v2"
    const val GELDBOERSE = "M3 6h15a2 2 0 0 1 2 2v11H5a2 2 0 0 1-2-2V6Z M15 11h6v5h-6a2.5 2.5 0 0 1 0-5Z"
    const val HAUS = "M3 21V9l9-6 9 6v12 M9 21v-6h6v6"
    const val HERZ = "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10Z"
    const val KURVE = "M3 17l5-5 4 4 8-8 M15 8h5v5"
    const val UHR = "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0 M12 7.5V12l3 2"
    const val MELDER = "M8 3h8a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z " +
        "M9.5 6h5a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1Z M9 15h6M9 18h6"
    const val TON = "M11 5 6 9H3v6h3l5 4V5Z M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13"
    const val RINGE = "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0 M7.5 12a4.5 4.5 0 1 0 9 0a4.5 4.5 0 1 0-9 0"
    const val KACHELN = "M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z " +
        "M15 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z " +
        "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1Z " +
        "M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1Z"
    const val OFFEN = "M4 12h16M12 4v16 M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0"
    const val SONNE = "M12 3v3M5.6 5.6l2.1 2.1M3 12h3M18.4 5.6l-2.1 2.1M21 12h-3 M7 14a5 5 0 1 0 10 0a5 5 0 1 0-10 0"
    const val PERSON_PLUS = "M5.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0 M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6M18 8v6M15 11h6"
}
