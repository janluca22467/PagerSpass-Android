package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Dauer
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Das Eingabefeld — übertragen aus `.feld` in `base.css` und `mobil.css`.
 *
 * <b>46 Punkte hoch und nicht 44.</b> Ein Feld wird nicht nur getroffen, sondern
 * auch beschrieben; bei gleicher Höhe wie ein Knopf sieht es flacher aus, weil
 * sein Text auf der Grundlinie sitzt statt zentriert.
 *
 * <b>Der Schein im Fokus ist nicht Zierde.</b> Seit die Ränder insgesamt heller
 * sind, liegen `RandHell` im Ruhezustand und `AmberTief` im Fokus zu dicht
 * beieinander — der Rahmen allein gab das aktive Feld nicht mehr her. Was man
 * aus zwei Metern Abstand sieht, ist der Ring darum.
 *
 * <b>Und die Fläche ist dunkler als der Kasten darum.</b> Ein Feld ist in die
 * Fläche eingelassen, nicht auf sie gelegt.
 *
 * @param etikett Die Beschriftung darüber. Ein Feld ohne Etikett ist eines, bei
 *   dem der Platzhalter die Beschriftung tragen muss — und der verschwindet beim
 *   ersten Zeichen.
 * @param geheim Für Kennwörter. Setzt zugleich die passende Tastatur.
 */
@Composable
fun Feld(
    wert: String,
    beiAenderung: (String) -> Unit,
    modifier: Modifier = Modifier,
    etikett: String? = null,
    platzhalter: String? = null,
    geheim: Boolean = false,
    einzeilig: Boolean = true,
    tastatur: KeyboardType = KeyboardType.Text,
    weiterTaste: ImeAction = ImeAction.Next,
    stil: TextStyle = Schrift.Normal.copy(fontSize = Schrift.EINGABE),
    aktiv: Boolean = true,
    fehler: String? = null,
) {
    val beruehrung = remember { MutableInteractionSource() }
    val imFokus by beruehrung.collectIsFocusedAsState()

    val randfarbe by animateColorAsState(
        targetValue = when {
            fehler != null -> Farben.Signal
            imFokus -> Farben.Amber
            else -> Farben.RandHell
        },
        animationSpec = tween(Dauer.WECHSEL),
        label = "feld-rand",
    )

    val schein = if (imFokus) Farben.Amber.copy(alpha = 0.18f) else Color.Transparent

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (etikett != null) Etikett(etikett)

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Ziel.Feld)
                // Der Schein: drei Punkte Amber bei 18 Prozent, außerhalb des
                // Rahmens. Im Web ein `box-shadow`, hier ein zweiter Rand mit
                // Abstand — Compose kennt keinen Schatten ohne Weichzeichnung.
                //
                // <b>Der Ring ist immer da, nur ohne Fokus durchsichtig.</b> Früher
                // kam er per `if (imFokus)` in die Kette und ging wieder heraus.
                // Beim Wechsel von einem Feld ins nächste bauten damit zwei Felder
                // im selben Bild ihre Modifier-Kette um — genau während das
                // Textfeld seine Eingabesitzung umhängt und nach den Koordinaten
                // seiner Knoten fragt. Das riss die App ab. Eine Kette mit fester
                // Gestalt ändert nur noch eine Farbe.
                .border(3.dp, schein, Rundung.Klein)
                .padding(3.dp)
                // Dunkler als der Kasten darum — siehe oben. `rgb(5 9 14 / 82%)`
                // aus mobil.css; auf undurchsichtigem Grund ist das dieser Wert.
                .background(Farben.BgTief, Rundung.Klein)
                .border(1.dp, randfarbe, Rundung.Klein)
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            BasicTextField(
                value = wert,
                onValueChange = beiAenderung,
                enabled = aktiv,
                singleLine = einzeilig,
                textStyle = stil.copy(color = if (aktiv) Farben.Text else Farben.TextSehrLeise),
                cursorBrush = SolidColor(Farben.Amber),
                visualTransformation = if (geheim) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (geheim) KeyboardType.Password else tastatur,
                    imeAction = weiterTaste,
                ),
                interactionSource = beruehrung,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inneres ->
                    if (wert.isEmpty() && platzhalter != null) {
                        Text(platzhalter, style = stil, color = Farben.TextSehrLeise)
                    }
                    inneres()
                },
            )
        }

        // Der Fehler steht unter dem Feld und nicht als Meldung am Bildschirmrand:
        // Wer drei Felder ausfüllt und eine Meldung „Ungültig" bekommt, weiß nicht,
        // welches gemeint ist.
        if (fehler != null) {
            Text(fehler, style = Schrift.Klein, color = Farben.SignalHell)
        }
    }
}

/**
 * Das Feld für einen Raumcode — sechs Zeichen, groß und gesperrt.
 *
 * Eigene Bauform, weil hier die Zeichen einzeln zählen: Monospace, damit alle
 * gleich breit sind, Versalien, weil der Code welche ist, und mittig, weil er
 * allein auf seiner Zeile steht.
 */
@Composable
fun Codefeld(
    wert: String,
    beiAenderung: (String) -> Unit,
    modifier: Modifier = Modifier,
    etikett: String? = null,
    laenge: Int = 6,
) {
    Feld(
        wert = wert,
        beiAenderung = { neu -> beiAenderung(neu.uppercase().take(laenge)) },
        modifier = modifier,
        etikett = etikett,
        weiterTaste = ImeAction.Done,
        stil = Schrift.MonoNormal.copy(
            fontSize = Schrift.TITEL,
            letterSpacing = 0.18.em,
            textAlign = TextAlign.Center,
            color = Color.Unspecified,
        ),
    )
}
