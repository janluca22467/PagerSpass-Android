package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Dauer
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.eingelassen

/**
 * Eine Zeile mit einem Schalter rechts.
 *
 * Die Bauform für Einstellungslisten: links steht, worum es geht, rechts der
 * Schalter. <b>Die ganze Zeile schaltet</b>, nicht nur der Schieber — am Daumen
 * ist der Unterschied der zwischen einem 46 Punkte breiten Ziel und einem, das
 * über die ganze Breite geht.
 *
 * <b>Warum kein Material-`Switch`.</b> Der bringt seine eigenen Maße, seine
 * eigene Farbgebung und einen Ripple mit, den sonst nichts in dieser Anwendung
 * hat — und er ist mit 52 × 32 Punkten deutlich größer als das, was das Web
 * zeigt. Der Schieber hier ist die Bahn aus der Erhebungsstaffel („eingelassen")
 * mit einem Knopf darauf, und er trägt dieselbe Amber-Bedeutung wie alles
 * andere, was „das gilt gerade" heißt.
 */
@Composable
fun Schalterzeile(
    titel: String,
    an: Boolean,
    beiWechsel: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    unterzeile: String? = null,
    aktiv: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ziel.Normal)
            .toggleable(
                value = an,
                enabled = aktiv,
                onValueChange = beiWechsel,
                role = Role.Switch,
                indication = null,
                interactionSource = null,
            )
            .padding(vertical = Abstand.Winzig),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = titel,
                style = Schrift.Normal,
                color = if (aktiv) Farben.Text else Farben.TextSehrLeise,
            )
            if (unterzeile != null) SehrLeise(unterzeile)
        }

        Schieber(an = an, aktiv = aktiv)
    }
}

/** Der Schieber selbst — Bahn, Knopf, und eine Bewegung von 120 Millisekunden. */
@Composable
private fun Schieber(an: Boolean, aktiv: Boolean) {
    val bahnbreite = 46.dp
    val hoehe = 26.dp
    val knopf = 20.dp

    val ort by animateDpAsState(
        targetValue = if (an) bahnbreite - knopf - 3.dp else 3.dp,
        animationSpec = tween(Dauer.WECHSEL),
        label = "schieber",
    )
    val bahnfarbe by animateColorAsState(
        targetValue = when {
            !aktiv -> Farben.Flaeche
            an -> Farben.AmberTief
            else -> Farben.BgTief
        },
        animationSpec = tween(Dauer.WECHSEL),
        label = "schieber-bahn",
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .width(bahnbreite)
            .height(hoehe)
            .background(bahnfarbe, Rundung.Rund)
            .border(1.dp, if (an && aktiv) Farben.Amber else Farben.Rand, Rundung.Rund)
            .eingelassen(),
    ) {
        Box(
            modifier = Modifier
                .offset(x = ort)
                .size(knopf)
                .background(
                    color = when {
                        !aktiv -> Farben.Rand
                        an -> Farben.Amber
                        else -> Farben.RandHell
                    },
                    shape = CircleShape,
                ),
        )
    }
}
