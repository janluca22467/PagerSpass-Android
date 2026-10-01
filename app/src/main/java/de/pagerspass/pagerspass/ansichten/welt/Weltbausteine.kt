package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ansichten.Wahlblende
import de.pagerspass.pagerspass.ansichten.Wahlfeld
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die kleinen Bausteine der Welt-Seiten.
 *
 * <b>Sie setzen die Bausteine der App zusammen und erfinden keine neuen.</b>
 * Ein `<select>` des Webs ist hier `Wahlfeld` plus `Wahlblende`, eine Zeile
 * „Etikett — Zahl“ ist ein Text mit Monospace rechts; was hier steht, ist nur
 * die Zusammensetzung, die in zwölf Seiten sonst zwölfmal stünde.
 */

/** Ein `<select>`: das Feld, und beim Antippen die Liste. */
@Composable
fun <T> Auswahl(
    etikett: String,
    eintraege: List<T>,
    gewaehlt: T?,
    aufschrift: (T) -> String,
    beiWahl: (T) -> Unit,
    modifier: Modifier = Modifier,
    unterschrift: (T) -> String? = { null },
    platzhalter: String = "Bitte wählen",
    aktiv: Boolean = true,
    gruppe: ((T) -> String?)? = null,
) {
    var offen by remember { mutableStateOf(false) }
    Wahlfeld(
        etikett = etikett,
        wert = gewaehlt?.let(aufschrift),
        beiDruck = { offen = true },
        modifier = modifier,
        platzhalter = if (eintraege.isEmpty()) "Nichts zur Wahl" else platzhalter,
        aktiv = aktiv && eintraege.isNotEmpty(),
    )
    if (offen) {
        val gruppen = if (gruppe == null) listOf<Pair<String?, List<T>>>(null to eintraege)
        else eintraege.groupBy(gruppe).map { (name, liste) -> name to liste }
        Wahlblende(
            titel = etikett,
            gruppen = gruppen,
            aufschrift = aufschrift,
            beiWahl = { beiWahl(it); offen = false },
            beiSchliessen = { offen = false },
            gewaehlt = gewaehlt,
            unterschrift = unterschrift,
            suchbar = eintraege.size > 12,
        )
    }
}

/** „Etikett ……… Wert“ — die Zeile der Kasse, der Großlage, der Wachenseite. */
@Composable
fun Wertzeile(etikett: String, wert: String, modifier: Modifier = Modifier, leise: Boolean = false, farbe: Color? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = etikett,
            style = Schrift.Klein,
            color = if (leise) Farben.TextSehrLeise else Farben.TextLeise,
            modifier = Modifier.weight(1f),
        )
        Text(text = wert, style = Schrift.MonoKlein, color = farbe ?: if (leise) Farben.TextLeise else Farben.Text)
    }
}

/** Ein Satz in Signalfarbe — was schiefging, direkt unter dem Knopf, der es auslöste. */
@Composable
fun Warnsatz(text: String?, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    Text(text = text, style = Schrift.MonoKlein, color = Farben.SignalHell, modifier = modifier)
}

@Composable
fun Leisesatz(text: String, modifier: Modifier = Modifier, winzig: Boolean = false) {
    Text(
        text = text,
        style = if (winzig) Schrift.Winzig else Schrift.Klein,
        color = if (winzig) Farben.TextSehrLeise else Farben.TextLeise,
        modifier = modifier,
    )
}

/** Eine kleine Marke — Stichwortmarke, Fähigkeit, Zustand. */
@Composable
fun Weltmarke(text: String, farbe: Color = Farben.TextLeise, gefuellt: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
        color = if (gefuellt) Farben.AufFarbe else farbe,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(if (gefuellt) farbe else farbe.copy(alpha = 0.10f), Rundung.Rund)
            .border(1.dp, farbe.copy(alpha = if (gefuellt) 1f else 0.5f), Rundung.Rund)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/** Eine Reihe, die umbricht — für Knöpfe und Marken. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Umbruchreihe(modifier: Modifier = Modifier, inhalt: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier.fillMaxWidth(),
    ) { inhalt() }
}

/** Ein Kasten in der Seite — Lage, Fahrzeug, Wache: eine Zeile, die aufklappt. */
@Composable
fun Weltkasten(
    modifier: Modifier = Modifier,
    an: Boolean = false,
    randfarbe: Color? = null,
    beiDruck: (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .fillMaxWidth()
            .flaeche(randfarbe = if (an) Farben.AmberTief else randfarbe ?: Farben.Rand)
            .then(if (beiDruck != null) Modifier.clickable(onClick = beiDruck) else Modifier)
            .padding(Abstand.Normal),
        content = inhalt,
    )
}

/** Eine Zahl eingeben — ein `Feld` mit Zifferntastatur. */
@Composable
fun Zahlfeld(
    etikett: String,
    wert: Int,
    beiAenderung: (Int) -> Unit,
    modifier: Modifier = Modifier,
    kleinstes: Int = 0,
    groesstes: Int = Int.MAX_VALUE,
) {
    var text by remember(wert) { mutableStateOf(wert.toString()) }
    Feld(
        wert = text,
        beiAenderung = { neu ->
            text = neu.filter { it.isDigit() }.take(9)
            text.toIntOrNull()?.let { beiAenderung(it.coerceIn(kleinstes, groesstes)) }
        },
        etikett = etikett,
        tastatur = KeyboardType.Number,
        modifier = modifier,
    )
}

/** Ein Balken mit Anteil — Fortschritt einer Fahrt, eines Kredits, einer Stufe. */
@Composable
fun Weltbalken(anteil: Float, farbe: Color = Farben.Amber, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Farben.BgTief, Rundung.Rund),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(anteil.coerceIn(0f, 1f))
                .height(6.dp)
                .background(farbe, Rundung.Rund),
        )
    }
}

/** Die FMS-Plakette ganz vorn an einer Fahrzeugzeile. */
@Composable
fun Fmsplakette(status: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .background(fmsFarbe(status), Rundung.Winzig),
    ) {
        Text(
            text = "$status",
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
            color = Farben.AufFarbe,
        )
    }
}

/** Ein Punkt in einer Farbe — für Zustände ohne Text. */
@Composable
fun Farbpunkt(farbe: Color, groesse: Int = 8) {
    Box(Modifier.size(groesse.dp).background(farbe, CircleShape))
}

/** Die Frage vor einer endgültigen Handlung: Ja und Nein nebeneinander. */
@Composable
fun Rueckfrage(
    frage: String,
    ja: String,
    beiJa: () -> Unit,
    beiNein: () -> Unit,
    aktiv: Boolean = true,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.HauchSignal, Rundung.Klein)
            .border(1.dp, Farben.SignalTief, Rundung.Klein)
            .padding(Abstand.Normal),
    ) {
        Text(frage, style = Schrift.Klein, color = Farben.Text)
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(ja, beiJa, art = Knopfart.Gefahr, kompakt = true, aktiv = aktiv)
            Knopf("Doch nicht", beiNein, art = Knopfart.Leise, kompakt = true)
        }
    }
}

/** Die Abstandszeile zwischen zwei Blöcken einer Seite. */
@Composable
fun Luecke(hoehe: Int = 4) {
    Box(Modifier.height(hoehe.dp).width(1.dp))
}

/** Ein Wahlpunkt in einer Liste mit Haken — die Zeile für Mehrfachwahl. */
@Composable
fun Wahlzeile(
    an: Boolean,
    beiWechsel: () -> Unit,
    modifier: Modifier = Modifier,
    inhalt: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .background(if (an) Farben.HauchAmber else Color.Transparent, Rundung.Klein)
            .border(1.dp, if (an) Farben.AmberTief else Farben.Rand, Rundung.Klein)
            .clickable(onClick = beiWechsel)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(18.dp)
                .background(if (an) Farben.Amber else Color.Transparent, Rundung.Winzig)
                .border(1.dp, if (an) Farben.Amber else Farben.RandHell, Rundung.Winzig),
        ) {
            if (an) Text("✓", style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold), color = Farben.AufAmber)
        }
        Box(Modifier.weight(1f)) { inhalt() }
    }
}

/**
 * Eine Zeile, die auswählt — `.listenwahl` aus `base.css`.
 *
 * Fläche mit Rand, links ein Balken von drei Punkten; gewählt trägt sie Amber
 * ringsum und den Hauch darunter. Anders als die `Wahlzeile` ohne Haken: Hier
 * wird eines aus vielen gewählt, nicht mehreres angekreuzt.
 */
@Composable
fun Listenwahl(
    an: Boolean,
    beiDruck: () -> Unit,
    modifier: Modifier = Modifier,
    balken: Color? = null,
    inhalt: @Composable RowScope.() -> Unit,
) {
    val balkenfarbe = balken ?: if (an) Farben.Amber else Farben.RandHell
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clip(Rundung.Klein)
            .background(if (an) Farben.HauchAmber else Farben.Flaeche)
            .border(1.dp, if (an) Farben.Amber else Farben.Rand, Rundung.Klein)
            .drawBehind { drawRect(balkenfarbe, size = Size(3.dp.toPx(), size.height)) }
            .clickable(onClick = beiDruck)
            .padding(start = Abstand.Normal + 2.dp, end = Abstand.Normal, top = Abstand.Klein, bottom = Abstand.Klein),
        content = inhalt,
    )
}

/**
 * „Alle wählen (n)“ / „Auswahl aufheben“ — `AlleWaehlen.vue`. Erst ab zwei
 * Fahrzeugen: Bei einem ist der Haken selbst der kürzere Weg.
 */
@Composable
fun AlleWaehlen(ids: List<String>, angehakt: Set<String>, beiSetzen: (Set<String>) -> Unit) {
    if (ids.size <= 1) return
    val alle = ids.all { it in angehakt }
    Knopf(
        if (alle) "Auswahl aufheben" else "Alle wählen (${ids.size})",
        { beiSetzen(if (alle) angehakt - ids.toSet() else angehakt + ids) },
        art = Knopfart.Leise,
        kompakt = true,
    )
}

/**
 * Die Reiterreihe in der Welt — `.reiter` mit dem Welt-Ton aus `welt.css`: eine
 * eingelassene Leiste, gleich breite Wege, der gewählte in Cyan statt Bernstein.
 * Bernstein heißt in der Welt „Geld“ und „Bauen“ — ein bernsteinfarbenes „Alle“
 * über einer Liste mit Beträgen las sich wie ein weiterer Betrag.
 */
@Composable
fun <T> Weltreiter(
    wege: List<T>,
    gewaehlt: T,
    beiWahl: (T) -> Unit,
    aufschrift: (T) -> String,
    modifier: Modifier = Modifier,
    zahl: ((T) -> Int)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = modifier
            .fillMaxWidth()
            .background(Weltfarben.GlasTief, Rundung.Klein)
            .border(1.dp, Weltfarben.Kante, Rundung.Klein)
            .padding(Abstand.Haar),
    ) {
        wege.forEach { w ->
            val an = w == gewaehlt
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .then(
                        if (an) Modifier
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Weltfarben.Hauch, Color.Transparent)),
                                Rundung.Winzig,
                            )
                            .border(1.dp, Weltfarben.AkzentTief, Rundung.Winzig)
                        else Modifier,
                    )
                    .clickable { beiWahl(w) }
                    .padding(horizontal = Abstand.Haar, vertical = Abstand.Winzig),
            ) {
                Text(
                    aufschrift(w),
                    style = Schrift.Winzig.copy(fontWeight = if (an) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (an) Weltfarben.Akzent else Farben.TextSehrLeise,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (zahl != null) {
                    Text(
                        "${zahl(w)}",
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = if (an) Weltfarben.Akzent else Farben.TextLeise,
                        modifier = Modifier
                            .background(Farben.FlaecheAktiv, Rundung.Rund)
                            .padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }
}
