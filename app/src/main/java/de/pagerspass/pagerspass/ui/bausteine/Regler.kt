package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import kotlin.math.roundToInt

/**
 * Ein Schieberegler — das Gegenstück zu `<input type="range" class="regler">`.
 *
 * <b>Ganzzahlig und mit Schrittweite</b>, wie im Web: Die Zeitachse rastet in
 * halben Minuten, die Fahrzeugzahl in ganzen. Der Regler rechnet intern in
 * Kommazahlen (so will es Material), gibt aber nur ganze Werte heraus — sonst
 * stünde „+2:17" auf einer Achse, die nur :00 und :30 kennt.
 *
 * <b>`beiLoslassen` ist das `change` des Webs</b>, `beiAenderung` das `input`: Die
 * Zeitachse sortiert erst beim Loslassen um, sonst spränge der Eintrag unter dem
 * Finger weg.
 *
 * @param etikett steht darüber und trägt den Wert meist gleich mit („Zoom 150 %").
 */
@Composable
fun Regler(
    wert: Int,
    beiAenderung: (Int) -> Unit,
    von: Int,
    bis: Int,
    modifier: Modifier = Modifier,
    schritt: Int = 1,
    etikett: String? = null,
    aktiv: Boolean = true,
    beiLoslassen: (() -> Unit)? = null,
) {
    val spanne = (bis - von).coerceAtLeast(1)
    val stufen = (spanne / schritt.coerceAtLeast(1) - 1).coerceAtLeast(0)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (etikett != null) Etikett(etikett)

        Slider(
            value = wert.coerceIn(von, bis).toFloat(),
            onValueChange = { roh ->
                val neu = (von + ((roh - von) / schritt.coerceAtLeast(1)).roundToInt() * schritt)
                    .coerceIn(von, bis)
                if (neu != wert) beiAenderung(neu)
            },
            valueRange = von.toFloat()..bis.toFloat(),
            // Bei sehr vielen Rasten (die Zeitachse hat 120) zeichnet Material
            // einen Punkt je Stufe — ein grauer Balken aus Punkten. Dann rastet der
            // Regler selbst (oben) und die Schiene bleibt glatt.
            steps = if (stufen <= 24) stufen else 0,
            enabled = aktiv,
            onValueChangeFinished = beiLoslassen,
            colors = SliderDefaults.colors(
                thumbColor = Farben.Amber,
                activeTrackColor = Farben.Amber,
                inactiveTrackColor = Farben.FlaecheHoch,
                activeTickColor = Farben.AufAmber,
                inactiveTickColor = Farben.RandHell,
                disabledThumbColor = Farben.RandHell,
                disabledActiveTrackColor = Farben.Rand,
                disabledInactiveTrackColor = Farben.Flaeche,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
