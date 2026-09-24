package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Dauer
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Der Fortschrittsbalken — Bahn, Füllung und die Zahl daneben.
 *
 * <b>Die Bahn ist eine Vertiefung, kein Strich.</b> Sie ist dunkler als die
 * Fläche darum, damit die Füllung *darin* liegt; vorher war sie heller als der
 * Grund, und der leere Teil stach damit mehr hervor als der erreichte. Die
 * Haarlinie ringsum ist nicht Zierde, sondern der Grund, warum man die leere
 * Bahn überhaupt sieht: `BgTief` auf `Bg` ist ein Unterschied von drei Werten.
 * Ohne sie las sich der Balken bei 0 % als versehentlicher Strich quer über die
 * Seite statt als Strecke, die noch zu gehen ist.
 *
 * <b>Das Erreichte glüht.</b> Amber ist im ganzen Spiel die Farbe für „das gilt
 * gerade" — hier heißt es „so weit bist du".
 *
 * <b>Und die Zahl daneben ist keine Nebenangabe.</b> Sie stand im Web in der
 * leisesten Farbe der Staffel, für den einzigen Satz auf der Seite, der sagt,
 * wie weit es noch ist. Ein Balken allein gibt nichts zum Nachrechnen; genau
 * deshalb steht die Zahl dort. Also darf man sie auch lesen — `TextLeise`.
 *
 * @param anteil Zwischen 0 und 1. Werte darüber oder darunter werden beschnitten,
 *   nicht gemeldet: Ein Balken ist kein Ort für eine Fehlermeldung.
 */
@Composable
fun Fortschritt(
    anteil: Float,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val gezeigt by animateFloatAsState(
        targetValue = anteil.coerceIn(0f, 1f),
        animationSpec = tween(Dauer.WECHSEL_LANG),
        label = "fortschritt",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(Farben.BgTief, Rundung.Rund)
                .border(1.dp, Farben.HauchHell, Rundung.Rund)
                .clip(Rundung.Rund),
        ) {
            if (gezeigt > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(gezeigt)
                        .fillMaxHeight()
                        .shadow(
                            elevation = 4.dp,
                            shape = Rundung.Rund,
                            ambientColor = Farben.Amber,
                            spotColor = Farben.Amber,
                        )
                        .background(Farben.Amber, Rundung.Rund),
                )
            }
        }

        if (text != null) {
            Text(text = text, style = Schrift.Klein, color = Farben.TextLeise)
        }
    }
}
