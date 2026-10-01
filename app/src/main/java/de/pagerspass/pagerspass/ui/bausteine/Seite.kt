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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.seitengrund

/**
 * Die Rollfläche einer Ansicht — das Gegenstück zu `.seite` in `base.css`.
 *
 * <b>Der Grund ist eine ruhige Fläche.</b> Am Rechner trägt `.seite` im Web
 * drei Lagen — Haarlinienraster, warmer Schein von oben, Verlauf nach `BgTief`.
 * Am Handy fielen sie am 30.09.2026 weg (Tabletlook), und die App ist ein Handy:
 * eine Farbe, nichts leuchtet dahinter. Siehe `seitengrund()`.
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
            // Die ruhige Fläche des Tabletlooks — ohne Raster und ohne den
            // warmen Schein von oben, die das Web am Handy seit dem 30.09.2026
            // abgelegt hat (mobil.css, „Das Handy im Tabletlook").
            .seitengrund(),
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
    /**
     * Das Etikett über dem Titel — „Runden", „Ausrüstung". Jede Listenseite des Web
     * trägt eines; ohne es beginnt die Überschrift eine halbe Zeile weiter oben, und
     * beim Blättern von Seite zu Seite springt der Titel.
     */
    etikett: String? = null,
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
            if (etikett != null) Etikett(etikett)
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
