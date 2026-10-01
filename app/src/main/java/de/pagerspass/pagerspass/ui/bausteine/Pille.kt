package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.schein

/**
 * Die Pille — ein Filter, den man an- und ausschaltet.
 *
 * <b>Warum sie nicht die Marke ist.</b> Die Marke ist ein Schildchen — man liest
 * sie, man drückt sie nicht. Im Web standen zehn Pillen im Baum, mit vier
 * Polsterungen und drei verschiedenen Tönungen für „an". Nebeneinander sieht man
 * das nie — die Organisationsfilter der Lobby und die Stichwortfilter im
 * Einsatzdialog liegen zwei Berührungen auseinander. Hintereinander benutzt
 * merkt man nur, dass nichts sitzt.
 *
 * @param farbe Rand und Schrift im *eingeschalteten* Zustand. Sie ist
 *   ausdrücklich übersteuerbar: Die Filter der Lobby tragen die Farbe ihrer
 *   Organisation, und die ist kein Schmuck, sondern die Information.
 * @param zahl Der Bestand hinter dem Filternamen — „Feuerwehr **2**". Fett, weil
 *   sie die Auskunft ist: Der Name sagt, wovon; die Zahl sagt, wie viel.
 */
@Composable
fun Pille(
    aufschrift: String,
    an: Boolean,
    beiDruck: () -> Unit,
    modifier: Modifier = Modifier,
    farbe: Color = Farben.Amber,
    zahl: Int? = null,
    aktiv: Boolean = true,
    zeichenVorn: @Composable (() -> Unit)? = null,
) {
    val schrift = when {
        !aktiv -> Farben.TextSehrLeise
        an -> farbe
        else -> Farben.TextLeise
    }
    val rand = when {
        !aktiv -> Farben.Rand.copy(alpha = 0.6f)
        an -> farbe
        else -> Color.White.copy(alpha = 0.09f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = Ziel.Kompakt)
            // Moderner Anstrich (01.10.2026): gefüllte Kapseln statt Ränder um
            // nichts — ausgeschaltet ein Hauch Weiß, eingeschaltet die Farbe mit
            // einem Schein darum.
            .then(if (an && aktiv) Modifier.schein(farbe, ecke = 999.dp, weite = 6.dp, staerke = 0.25f, versatz = 0.dp) else Modifier)
            .background(if (an) farbe.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f), Rundung.Rund)
            .border(1.dp, rand, Rundung.Rund)
            .clickable(
                enabled = aktiv,
                onClick = beiDruck,
                role = Role.Tab,
                indication = null,
                interactionSource = null,
            )
            .padding(horizontal = Abstand.Normal),
    ) {
        CompositionLocalProvider(LocalContentColor provides schrift) {
            zeichenVorn?.invoke()
            Text(
                text = aufschrift,
                style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                color = schrift,
                maxLines = 1,
            )
            if (zahl != null) {
                Text(
                    text = zahl.toString(),
                    style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                    color = schrift,
                )
            }
        }
    }
}

/**
 * Die Filterreihe: mehrere Pillen nebeneinander, und sie bricht um.
 *
 * <b>Warum umbrechen und nicht rollen.</b> Sie rollte im Web waagerecht, solange
 * die Seite die volle Bildschirmbreite hatte — dann passten alle Filter
 * nebeneinander und man merkte es nie. Auf der Arbeitsbreite wurde daraus ein
 * abgeschnittenes „Einsatza…" am rechten Rand, ohne Rollbalken und ohne Hinweis,
 * dass dahinter noch etwas steht. Ein Filter, den man nicht sieht, ist keiner —
 * und Platz nach unten ist genug.
 *
 * Die Luft darum bleibt Sache der Stelle: Wie viel Abstand die Reihe zu ihren
 * Nachbarn hält, weiß nur die Seite, auf der sie steht.
 */
@Composable
fun Pillenreihe(modifier: Modifier = Modifier, inhalt: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier,
    ) {
        inhalt()
    }
}
