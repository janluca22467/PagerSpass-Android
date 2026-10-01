package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Erhebung
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Ein Teil einer Dienstansicht — Name und Zeichen eines Reiters.
 *
 * @param ruft Ob der Reiter dringend ist, obwohl er nicht offen ist. Ein Reiter,
 *   der ruft, ist nicht „ausgewählt" — deshalb Signalfarbe und nicht Amber:
 *   Amber heißt „hier bist du".
 */
data class Teil(
    val id: String,
    val titel: String,
    val zeichen: ImageVector,
)

/**
 * Die Reiterleiste einer Dienstansicht — Lobby, Fahrzeug, Leitstelle.
 *
 * <b>Eigene Bauform, nicht die Tableiste.</b> Das sind keine Wege — man verlässt
 * die Ansicht nicht, man sieht sie anders an. Im Web tragen sie aus demselben
 * Grund eigene Klassennamen (`lreiter` statt `weg`).
 *
 * Sie stand zuerst als Privatsache der Lobby; mit Fahrzeug und Leitstelle wäre
 * sie dreimal abgeschrieben worden — und die dritte Abschrift hätte eine andere
 * Höhe gehabt. Genau die Geschichte, die `base.css` seitenweise erzählt.
 */
@Composable
fun Teilleiste(
    teile: List<Teil>,
    offen: String,
    beiWahl: (String) -> Unit,
    modifier: Modifier = Modifier,
    marken: Map<String, Int> = emptyMap(),
    ruft: Set<String> = emptySet(),
) {
    val geraeterand = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(Erhebung.Leiste, Rundung.LeisteOben)
            .background(Farben.Flaeche, Rundung.LeisteOben)
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(
                    color = Farben.RandHell.copy(alpha = 0.76f),
                    start = Offset(0f, strich / 2f),
                    end = Offset(size.width, strich / 2f),
                    strokeWidth = strich,
                )
            }
            .padding(horizontal = 4.dp)
            .padding(bottom = geraeterand),
    ) {
        teile.forEach { teil ->
            Teilknopf(
                teil = teil,
                hier = teil.id == offen,
                marke = marken[teil.id] ?: 0,
                ruft = teil.id in ruft && teil.id != offen,
                beiDruck = { beiWahl(teil.id) },
            )
        }
    }
}

@Composable
private fun RowScope.Teilknopf(
    teil: Teil,
    hier: Boolean,
    marke: Int,
    ruft: Boolean,
    beiDruck: () -> Unit,
) {
    // Wie die Tableiste im Tabletlook: hier = helle Fläche, weiße Schrift,
    // amberfarbenes Zeichen.
    val farbe = when {
        hier -> Farben.Amber
        ruft -> Farben.SignalHell
        else -> Farben.TextLeise
    }
    val schriftfarbe = if (hier) Farben.Text else farbe

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 6.dp)
            .defaultMinSize(minHeight = Mass.LeisteHoehe - 12.dp)
            .then(
                if (hier) {
                    Modifier.background(Farben.FlaecheAktiv, Rundung.Normal)
                } else {
                    Modifier
                }
            )
            .clickable(
                onClick = beiDruck,
                role = Role.Tab,
                indication = null,
                interactionSource = null,
            )
            .padding(horizontal = Abstand.Haar, vertical = Abstand.Winzig),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(top = Abstand.Klein),
        ) {
            Icon(
                imageVector = teil.zeichen,
                contentDescription = null,
                tint = farbe,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = teil.titel,
                style = Schrift.Weg,
                color = schriftfarbe,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }

        if (marke > 0) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(start = 26.dp, top = 2.dp)) {
                Markenzahl(marke)
            }
        }
    }
}
