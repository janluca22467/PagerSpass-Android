package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Bausteine des Dienstbuchs — `.db-karte`, `.db-wink`, `.db-rang` aus
 * `web/src/styles/dienstbuch.css`.
 *
 * <b>Warum sie nicht mehr nur dem Dienstbuch gehören.</b> Seit 5.0.0.26 tragen
 * auch Wache, Wachen-Shop und Freunde dieselben Karten mit Kopf: Nebeneinander
 * geblättert sah man vorher zwei Anwendungen — im Dienstbuch Karten mit Kopf
 * und Tafeln, eine Tab-Taste weiter lose Überschriften über Kästen. Die Werte
 * sind die der Handy-Regel (`max-width: 560px`) in `dienstbuch.css`: Kopf mit
 * 4 × 12 Polster, Inhalt mit 8, kein Schatten (das Handy im Tabletlook).
 */

/** Ecke einer Karte — dieselbe Stufe wie jeder Kasten (`--radius`). */
private val KARTENECKE = 14.dp

/**
 * Die Kopfzeile der Karte: `color-mix(flaeche-hoch 35 %, flaeche)`. Einmal
 * gemischt, nicht bei jedem Zeichnen.
 */
private val KOPFFLAECHE = Farben.FlaecheHoch.copy(alpha = 0.35f).compositeOver(Farben.Flaeche)

/**
 * Eine Karte mit Kopf — Titel in Versalien, rechts daneben ein Zähler und ein
 * Weg, darunter der Inhalt.
 *
 * @param zahl Steht hinter dem Titel in Mono („3", „2 / 3"): die erste Frage an
 *   die Liste darunter, deshalb im Kopf und nicht in einer eigenen Zeile.
 * @param akzent Die amberne Kante links — hier wartet etwas auf eine Antwort
 *   (`.karte--akzent`). Sparsam: Anträge, Meldungen, der Aushang.
 * @param kante Eine andere Farbe für dieselbe Kante — der Austritt trägt sie in
 *   Signal (`.austritt`).
 * @param dicht Ohne Innenraum — für Zeilen, die von Kante zu Kante laufen
 *   (`.db-karte__inhalt--dicht`).
 * @param kopfweg Rechts im Kopf: „Einladen", „Ändern", „alle zeigen".
 * @param geraeumig Die ältere, luftigere Form aus dem Dienstbuch: Kopf ohne
 *   eigene Fläche, Kopf und Inhalt mit dem großen Polster, die Lichtkante der
 *   Flächen. Bis 5.2 war das eine zweite `Buchkarte` in `Werkbausteine.kt`, die
 *   je nach Aufruf still statt dieser gewählt wurde; jetzt sagt es der Aufruf.
 */
@Composable
fun Buchkarte(
    titel: String,
    modifier: Modifier = Modifier,
    zahl: String? = null,
    akzent: Boolean = false,
    kante: Color? = null,
    dicht: Boolean = false,
    abstandInnen: androidx.compose.ui.unit.Dp = Abstand.Klein,
    kopfweg: @Composable (RowScope.() -> Unit)? = null,
    geraeumig: Boolean = false,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val form = RoundedCornerShape(KARTENECKE)
    val kantenfarbe = kante ?: if (akzent) Farben.Amber else null
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (geraeumig) {
                    Modifier.flaeche()
                } else {
                    Modifier
                        .clip(form)
                        .background(Farben.Flaeche, form)
                        .border(1.dp, Farben.Rand, form)
                },
            )
            .then(
                if (kantenfarbe != null) {
                    // Als Strich über dem Inhalt und nicht als breiterer Rand: Ein
                    // Rand schöbe den Inhalt drei Punkte aus der Flucht der Karten
                    // darunter (`inset`-Schatten im Web).
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(color = kantenfarbe, size = size.copy(width = 3.dp.toPx()))
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Ziel.Kompakt)
                .then(if (geraeumig) Modifier else Modifier.background(KOPFFLAECHE))
                .drawWithContent {
                    drawContent()
                    val strich = 1.dp.toPx()
                    drawRect(
                        color = Farben.Rand,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - strich),
                        size = size.copy(height = strich),
                    )
                }
                .then(
                    if (geraeumig) {
                        Modifier.padding(horizontal = Abstand.Gross, vertical = Abstand.Klein)
                    } else {
                        Modifier.padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig)
                    },
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = titel.uppercase(),
                    style = Schrift.Klein.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
                    color = Farben.TextLeise,
                    maxLines = if (geraeumig) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (zahl != null) {
                    Text(
                        text = zahl,
                        style = if (geraeumig) Schrift.MonoKlein else Schrift.MonoKlein.copy(fontWeight = FontWeight.SemiBold),
                        color = Farben.TextSehrLeise,
                        maxLines = 1,
                    )
                }
            }
            kopfweg?.invoke(this)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(abstandInnen),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        dicht -> Modifier
                        geraeumig -> Modifier.padding(Abstand.Gross)
                        else -> Modifier.padding(Abstand.Klein)
                    },
                ),
            content = inhalt,
        )
    }
}

/**
 * Eine Zeile in einer dichten Karte — von Kante zu Kante, mit Trennstrich
 * darunter (`.zeilen > li` im Web). Die letzte Zeile lässt den Strich weg.
 */
@Composable
fun Kartenzeile(
    letzte: Boolean,
    modifier: Modifier = Modifier,
    hinterlegt: Color? = null,
    beiDruck: (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .fillMaxWidth()
            .then(if (hinterlegt != null) Modifier.background(hinterlegt) else Modifier)
            .then(if (beiDruck != null) Modifier.clickable(role = Role.Button, onClick = beiDruck) else Modifier)
            .drawWithContent {
                drawContent()
                if (letzte) return@drawWithContent
                val strich = 1.dp.toPx()
                drawRect(
                    color = Farben.Rand,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - strich),
                    size = size.copy(height = strich),
                )
            }
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        content = inhalt,
    )
}

/** Der Trennstrich unter einer Zeile in einer dichten Karte — die letzte lässt ihn weg. */
fun Modifier.zeilenstrich(letzte: Boolean): Modifier = drawWithContent {
    drawContent()
    if (letzte) return@drawWithContent
    val strich = 1.dp.toPx()
    drawRect(
        color = Farben.Rand,
        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - strich),
        size = size.copy(height = strich),
    )
}

/**
 * Der Wink — ein Satz mit Zeichen und Wegen (`.db-wink`). Keine Leerstelle,
 * sondern eine Auskunft über das, was da ist: „Clanrunde läuft", „Nächster
 * Dienst".
 *
 * @param wartet Ob etwas auf einen wartet — dann amberne Kante und Hauch
 *   (`.db-wink--wartet`; am Handy ohne den Verlauf, nur die Kante).
 * @param zeichen SVG-Pfaddaten auf dem 24er-Raster, als Strich gezeichnet.
 */
@Composable
fun Buchwink(
    zeichen: List<String>,
    modifier: Modifier = Modifier,
    wartet: Boolean = false,
    /** Statt eines Zeichens ein kurzer Text im Feld — der eigene Platz einer Rangliste. */
    marke: String? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val form = RoundedCornerShape(KARTENECKE)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier
            .fillMaxWidth()
            .background(Farben.Flaeche, form)
            .then(
                if (wartet) {
                    Modifier.background(
                        Brush.horizontalGradient(0f to Farben.HauchAmber, 0.7f to Color.Transparent),
                        form,
                    )
                } else {
                    Modifier
                },
            )
            .border(
                1.dp,
                if (wartet) Farben.Amber.copy(alpha = 0.45f).compositeOver(Farben.Rand) else Farben.Rand,
                form,
            )
            .padding(Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(Farben.HauchAmber, Rundung.Klein),
            ) {
                zeichen.mapNotNull { pfadzeichen(it, gefuellt = false) }.forEach { bild ->
                    Icon(bild, contentDescription = null, tint = Farben.Amber, modifier = Modifier.size(20.dp))
                }
                marke?.let {
                    Text(it, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.ExtraBold), color = Farben.Amber, maxLines = 1)
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier.weight(1f),
                content = inhalt,
            )
        }
    }
}

/**
 * Eine Zeile einer Rangliste (`.db-rang`): Platz im Kreis — Gold, Silber,
 * Bronze für die ersten drei —, Name, Wert. Die eigene Zeile in Amber mit Kante:
 * Man sucht zuerst sich selbst.
 */
@Composable
fun Rangzeile(
    platz: Int,
    wert: String,
    letzte: Boolean,
    modifier: Modifier = Modifier,
    eigen: Boolean = false,
    beiDruck: (() -> Unit)? = null,
    vorn: @Composable (() -> Unit)? = null,
    name: @Composable ColumnScope.() -> Unit,
) {
    val (grund, ziffer) = when (platz) {
        1 -> Farben.Amber.copy(alpha = 0.22f) to Farben.Amber
        2 -> Farben.TextLeise.copy(alpha = 0.16f) to Farben.Text
        3 -> Farben.OrgRettungsdienst.copy(alpha = 0.18f) to Farben.OrangeHell
        else -> Color.Transparent to Farben.TextSehrLeise
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ziel.Kompakt)
            .then(if (eigen) Modifier.background(Farben.HauchAmber) else Modifier)
            .then(if (beiDruck != null) Modifier.clickable(role = Role.Button, onClick = beiDruck) else Modifier)
            .drawWithContent {
                drawContent()
                if (eigen) drawRect(color = Farben.Amber, size = size.copy(width = 3.dp.toPx()))
                if (!letzte) {
                    val strich = 1.dp.toPx()
                    drawRect(
                        color = Farben.Rand,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - strich),
                        size = size.copy(height = strich),
                    )
                }
            }
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(30.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(30.dp).background(grund, Rundung.Rund),
            ) {
                Text(
                    "$platz",
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.ExtraBold),
                    color = ziffer,
                    maxLines = 1,
                )
            }
        }
        vorn?.invoke()
        Column(modifier = Modifier.weight(1f), content = name)
        Text(
            wert,
            style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
            color = if (eigen) Farben.Amber else Farben.Text,
            maxLines = 1,
        )
    }
}

/** Kalender — das Zeichen des Winks „Gemeinsam fahren", wenn nichts läuft. */
val ZEICHEN_KALENDER = listOf(
    "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z",
    "M3 10h18M8 3v4M16 3v4",
)

/** Funkwellen — eine laufende Clanrunde. */
val ZEICHEN_FUNK = listOf(
    "M9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0Z",
    "M7.8 7.8a6 6 0 0 0 0 8.4M16.2 7.8a6 6 0 0 1 0 8.4M5 5a10 10 0 0 0 0 14M19 5a10 10 0 0 1 0 14",
)
