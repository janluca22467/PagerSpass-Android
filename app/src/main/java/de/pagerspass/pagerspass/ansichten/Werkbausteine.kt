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
 */
@Composable
fun Kennzahlkachel(
    etikett: String,
    wert: String,
    unter: String,
    modifier: Modifier = Modifier,
    farbe: Color = Farben.Amber,
) {
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
 * Eine Karte mit Kopfleiste (`.db-karte` im Web): Titel in Versalien, daneben
 * eine leise Zahl, darunter der Inhalt mit eigenem Polster.
 */
@Composable
fun Buchkarte(
    titel: String,
    modifier: Modifier = Modifier,
    zahl: Int? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().flaeche()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Ziel.Kompakt)
                .drawBehind {
                    val strich = 1.dp.toPx()
                    drawLine(
                        color = Farben.Rand,
                        start = Offset(0f, size.height - strich / 2f),
                        end = Offset(size.width, size.height - strich / 2f),
                        strokeWidth = strich,
                    )
                }
                .padding(horizontal = Abstand.Gross, vertical = Abstand.Klein),
        ) {
            Text(
                titel.uppercase(),
                style = Schrift.Klein.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
                color = Farben.TextLeise,
            )
            if (zahl != null) Text(zahl.toString(), style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().padding(Abstand.Gross),
            content = inhalt,
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
