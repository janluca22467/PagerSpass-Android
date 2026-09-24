package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.raster

/**
 * Die Rollfläche einer Ansicht — das Gegenstück zu `.seite` in `base.css`.
 *
 * <b>Der Grund einer Seite besteht aus drei Lagen</b>, und alle drei sind im Web
 * dieselbe `background`-Angabe: das Raster aus zwei Haarlinien, ein warmer
 * Schein von oben (Amber bei 10 %, breit gestreut) und darunter ein Verlauf von
 * `Bg` nach `BgTief`. Zusammen ergeben sie den abgedunkelten Arbeitsplatz. Jede
 * einzelne Lage für sich sieht nach nichts aus; das ist Absicht.
 *
 * <b>Die Aussparungen des Geräts gehören zum Rand, nicht zum Inhalt.</b> Der
 * Grund läuft unter Uhr und Wischstreifen durch, das Polster hält den Inhalt
 * davon frei. Andersherum stünde die erste Überschrift unter der Uhr.
 *
 * @param breite Wie breit der Inhaltsstreifen höchstens werden darf. Der
 *   Regelfall ist die Liste. Wer ein Raster hat, sagt es — nicht umgekehrt: Eine
 *   Zeilenliste, die versehentlich 1240 Punkte breit wird, zerreißt zwischen
 *   Anfang und Ende.
 * @param unterrand Zusätzlicher Platz am Fuß. Wer eine Leiste über der Seite
 *   liegen hat, gibt hier deren Höhe an — sonst endet die letzte Zeile darunter.
 */
@Composable
fun Seite(
    modifier: Modifier = Modifier,
    breite: Dp = Mass.Listenbreite,
    unterrand: Dp = 0.dp,
    abstand: Dp = Abstand.Gross,
    rollen: Boolean = true,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val richtung = LocalLayoutDirection.current
    val aussparung: PaddingValues = WindowInsets.safeDrawing.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Lage 3, ganz unten: der Verlauf.
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Farben.Bg, Farben.BgTief),
                        startY = 0f,
                        endY = size.height,
                    )
                )
                // Lage 2: der warme Schein von oben. Im Web „1200 × 560 bei
                // 50 % / −12 %" — also ein breiter, flacher Fleck, dessen Mitte
                // oberhalb des Bildschirms liegt. Sichtbar ist nur sein unterer
                // Rand, und genau das ist gemeint: Licht, das von oben kommt.
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Farben.Amber.copy(alpha = 0.10f), androidx.compose.ui.graphics.Color.Transparent),
                        center = Offset(size.width / 2f, -size.height * 0.12f),
                        radius = maxOf(size.width, size.height) * 0.9f,
                    )
                )
            }
            // Lage 1: das Raster. Es liegt über dem Verlauf, damit seine Linien
            // oben so hell sind wie unten.
            .raster(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .then(if (rollen) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(
                    start = Abstand.Gross + aussparung.calculateStartPadding(richtung),
                    end = Abstand.Gross + aussparung.calculateEndPadding(richtung),
                    top = Abstand.Gross + aussparung.calculateTopPadding(),
                    bottom = Abstand.SehrGross + aussparung.calculateBottomPadding() + unterrand,
                ),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(abstand),
                modifier = Modifier.fillMaxWidth().widthIn(max = breite),
                content = inhalt,
            )
        }
    }
}

/**
 * Die Kopfzeile einer Listenseite — Titel links, Knöpfe rechts.
 *
 * <b>Sie rollt mit.</b> Das unterscheidet sie von der Dienstleiste, die oben
 * stehen bleibt und eine Kante nach unten trägt. Im Web hießen beide in fünf
 * Ansichten `.kopf`, und wer die eine anfasste, verschob die andere — eine Regel
 * für die Kopfzeile griff auf die Leiste der laufenden Runde mit durch. Deshalb
 * stehen sie hier als zwei Bausteine mit zwei Namen.
 */
@Composable
fun Seitenkopf(
    titel: String,
    modifier: Modifier = Modifier,
    unterzeile: String? = null,
    knoepfe: @Composable (() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.weight(1f),
        ) {
            // <b>Beide Zeilen dürfen abgeschnitten werden.</b> Der Titel ist
            // meist ein Wort, die Unterzeile aber trägt regelmäßig einen
            // Kontonamen — und der ist bis zu 24 Zeichen lang und vom Spieler
            // gewählt. Ohne Deckel lief er in der Schlagzeilengröße rechts aus
            // dem Bild; die Spalte hat `weight(1f)`, aber Text ohne `maxLines`
            // wächst über seine Spalte hinaus, statt umzubrechen.
            Text(
                text = titel,
                style = Schrift.Schlagzeile,
                color = Farben.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (unterzeile != null) {
                Text(
                    text = unterzeile,
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        knoepfe?.invoke()
    }
}
