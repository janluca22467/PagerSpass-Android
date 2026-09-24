package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Marke as MarkeMass
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Das Etikett über einem Feld oder einer Gruppe — „SCHICHTEN", „AUSRÜSTUNG".
 *
 * Versalien und Sperrung sind hier Bedeutung, nicht Schmuck: Das Etikett ist
 * keine Überschrift, die man liest, sondern eine Beschriftung, die man im
 * Vorbeisehen erfasst. Großgeschrieben wird hier und nicht am Aufrufpunkt —
 * sonst steht im Quelltext „SCHICHTEN" und beim nächsten Mal „Schichten", und
 * ein Vorleseprogramm buchstabiert das erste.
 */
@Composable
fun Etikett(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = Schrift.Etikett,
        color = Farben.TextLeise,
        modifier = modifier,
    )
}

/**
 * Die Marke — ein Schildchen, das man liest.
 *
 * <b>Man drückt sie nicht.</b> Wer etwas zum Drücken braucht, nimmt `Pille`. Der
 * Unterschied stand im Web hinter einunddreißig Regeln, die diese Bauform
 * nachbauten: Die Marke trägt ihren Rand in der eigenen Farbe und ihre Schrift in
 * Versalien, weil sie eine *Kennung* ist. Dass eine Ansicht ihre Marke zurück auf
 * eckig stellen musste, um sie als Reiter zu benutzen, war der Beweis — wer eine
 * Bauform überlädt, baut sie beim nächsten Mal wieder auseinander.
 *
 * @param farbe Rand und Schrift. Sie ist an vielen Stellen die Information
 *   selbst (Organisation, Status, Rang) und deshalb ausdrücklich übersteuerbar.
 */
@Composable
fun Marke(
    text: String,
    modifier: Modifier = Modifier,
    farbe: Color = Farben.TextLeise,
    zeichenVorn: @Composable (() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .border(1.dp, farbe, Rundung.Rund)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
    ) {
        zeichenVorn?.invoke()
        Text(
            text = text.uppercase(),
            style = Schrift.Winzig.copy(
                fontFamily = Schrift.Mono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.04.em,
            ),
            color = farbe,
        )
    }
}

/**
 * Die Zahl an einem Weg — ungelesene Nachrichten, offene Anfragen.
 *
 * Ein roter Kreis mit einer Ziffer. <b>Er ist mindestens 20 Punkte breit, auch
 * für eine einstellige Zahl:</b> Ein Kreis, der seiner Zahl gerade eben passt,
 * sieht aus wie ein Druckfehler.
 *
 * Die Zahl steht in Monospace, damit eine 1 und eine 8 denselben Kreis füllen —
 * sonst hüpft die Marke, wenn der Zähler weiterläuft.
 */
@Composable
fun Markenzahl(zahl: Int, modifier: Modifier = Modifier) {
    if (zahl <= 0) return
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = MarkeMass.Zahl)
            .height(MarkeMass.Zahl)
            .background(Farben.Signal, Rundung.Rund)
            .padding(horizontal = Abstand.Winzig),
    ) {
        Text(text = zahl.toString(), style = Schrift.MarkeZahl, color = Color.White)
    }
}

/**
 * Eine Nebenangabe — Zeit, Status, Erklärzeile unter einem Weg.
 *
 * Im Web sind das die Klassen `.leise` und `.sehr-leise`. Sie stehen hier als
 * Baustein und nicht als Farbe am Aufrufpunkt, damit die Wahl zwischen den
 * beiden Stufen eine Entscheidung bleibt und keine Gewohnheit wird: `Leise` ist
 * für das, was man liest; `SehrLeise` für das, was nur dasteht.
 */
@Composable
fun Leise(text: String, modifier: Modifier = Modifier, klein: Boolean = true) {
    Text(
        text = text,
        style = if (klein) Schrift.Klein else Schrift.Normal,
        color = Farben.TextLeise,
        modifier = modifier,
    )
}

/** Die leiseste Stufe — Zeitangaben, Zähler, Statuszeilen. */
@Composable
fun SehrLeise(text: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    Text(
        text = text,
        style = if (mono) Schrift.MonoKlein else Schrift.Klein,
        color = Farben.TextSehrLeise,
        modifier = modifier,
    )
}
