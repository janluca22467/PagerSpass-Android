package de.pagerspass.pagerspass.ui.fahrzeug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Der Fahrzeugriss, ruhig gestellt für Garage, Shop und Übungen — `FahrzeugSymbol.vue`.
 *
 * Geteilt wird die Zeichnung (`fahrzeugZeichnen`), nicht ihre Darstellung: auf der Karte
 * ist der Riss gedreht und blitzt bei Anfahrt blau — hier steht er still. Nur was sich
 * im Web auch hier bewegt, bewegt sich: der Rotor des Hubschraubers und die Bugwelle
 * des Boots.
 *
 * Jeder Riss füllt sein eigenes enges Kästchen, damit alle gleich groß erscheinen.
 *
 * @param groesse Die Länge des Fahrzeugs in dp; die Breite ergibt sich aus dem
 *   Seitenverhältnis. Stehend ist das die Höhe, quer die Breite.
 * @param quer Legt den Riss um eine Vierteldrehung hin (Front nach rechts) — in einer
 *   Zeile wäre er sonst ein senkrechtes Streichholz. Das ist kein Kurs, sondern eine
 *   Ausgleichsdrehung für die Zeichnung, die nordwärts steht.
 * @param blaulicht Zeigt die Sondersignalanlage blitzend, wie auf der Karte.
 */
@Composable
fun FahrzeugSymbol(
    typ: String,
    organisation: String,
    modifier: Modifier = Modifier,
    groesse: Dp = 34.dp,
    quer: Boolean = false,
    blaulicht: Boolean = false,
) {
    val bauplan = remember(typ, organisation) { bauplanFuer(typ, organisation) }
    FahrzeugSymbol(bauplan, modifier, groesse, quer, blaulicht)
}

/** Dasselbe Symbol zu einem schon bekannten Bauplan. */
@Composable
fun FahrzeugSymbol(
    bauplan: Bauplan,
    modifier: Modifier = Modifier,
    groesse: Dp = 34.dp,
    quer: Boolean = false,
    blaulicht: Boolean = false,
) {
    val breite = groesse * fahrzeugSeitenverhaeltnis(bauplan)
    val bewegt = blaulicht || bauplan.form == Form.Heli || bauplan.form == Form.Boot
    val uhr: State<Long>? = if (bewegt) rememberFahrzeuguhr() else null
    Canvas(
        modifier = modifier.size(
            width = if (quer) groesse else breite,
            height = if (quer) breite else groesse,
        ),
    ) {
        fahrzeugZeichnen(
            bauplan = bauplan,
            mitte = center,
            hoehe = groesse.toPx(),
            drehung = if (quer) 90f else 0f,
            blaulicht = blaulicht,
            // Gelesen erst beim Zeichnen: Die Uhr stößt nur das Neuzeichnen an, nicht die
            // Zusammensetzung.
            jetzt = uhr?.value,
        )
    }
}

/**
 * Eine Uhr im Bildtakt für Rotor, Bugwelle und Blitzer — in Millisekunden, wie `jetzt`
 * in `fahrzeugZeichnen`. Sie läuft nur, solange das Symbol zu sehen ist.
 */
@Composable
fun rememberFahrzeuguhr(): State<Long> = produceState(0L) {
    while (true) {
        withFrameMillis { value = it }
    }
}
