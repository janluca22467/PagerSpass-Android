package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Eine Auswahl, die aussieht wie ein Feld und sich öffnet wie ein Dialog.
 *
 * <b>Warum kein Aufklappmenü.</b> Im Web ist das ein `<select>`, und der Browser
 * gibt ihm am Handy von sich aus eine bildschirmfüllende Liste. Compose hat
 * dieses Gegenstück nicht: Ein `DropdownMenu` mit 401 Landkreisen wäre eine
 * Rollliste, die halb über der Seite hängt und deren Anfang man nicht sieht.
 * Eine Blende ist das, was der Browser am Handy ohnehin daraus macht.
 *
 * <b>Und sie hat ein Suchfeld</b>, sobald die Liste lang wird. 401 Kreise
 * durchzurollen, um „Westerwaldkreis" zu finden, ist keine Auswahl, sondern eine
 * Übung.
 */

/**
 * Die Zeile, die die Auswahl trägt.
 *
 * Sie sieht aus wie ein Feld — dieselbe Fläche, derselbe Rand, dieselbe Höhe —
 * und trägt rechts den Winkel, der sagt: Hier öffnet sich etwas.
 */
@Composable
fun Wahlfeld(
    etikett: String,
    wert: String?,
    beiDruck: () -> Unit,
    modifier: Modifier = Modifier,
    platzhalter: String = "Bitte wählen",
    aktiv: Boolean = true,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier.fillMaxWidth(),
    ) {
        Etikett(etikett)

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Ziel.Feld)
                .background(Farben.BgTief, Rundung.Klein)
                .border(1.dp, if (aktiv) Farben.RandHell else Farben.Rand, Rundung.Klein)
                .clickable(
                    enabled = aktiv,
                    onClick = beiDruck,
                    role = Role.Button,
                    indication = null,
                    interactionSource = null,
                )
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            Text(
                text = wert ?: platzhalter,
                style = Schrift.Normal.copy(fontSize = Schrift.EINGABE),
                color = when {
                    !aktiv -> Farben.TextSehrLeise
                    wert == null -> Farben.TextSehrLeise
                    else -> Farben.Text
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Zeichen.Weiter,
                contentDescription = null,
                tint = Farben.TextSehrLeise,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Die Blende mit der Liste.
 *
 * @param gruppen Beschriftete Abschnitte. <b>Die Beschriftung ist keine
 *   Verzierung:</b> Bei den Landkreisen trennt sie die mit echten Wachen- und
 *   Straßendaten von den übrigen. Eine stillschweigend umsortierte Liste sähe
 *   aus wie eine alphabetische, in der ein paar Namen an der falschen Stelle
 *   stehen.
 */
@Composable
fun <T> Wahlblende(
    titel: String,
    gruppen: List<Pair<String?, List<T>>>,
    aufschrift: (T) -> String,
    beiWahl: (T) -> Unit,
    beiSchliessen: () -> Unit,
    gewaehlt: T? = null,
    unterschrift: (T) -> String? = { null },
    suchbar: Boolean = false,
) {
    var suche by remember { mutableStateOf("") }

    val gefiltert = gruppen.map { (name, eintraege) ->
        name to eintraege.filter { suche.isBlank() || aufschrift(it).contains(suche, true) }
    }.filter { it.second.isNotEmpty() }

    Blende(titel = titel, beiSchliessen = beiSchliessen) {
        if (suchbar) {
            Feld(
                wert = suche,
                beiAenderung = { suche = it },
                platzhalter = "Suchen …",
            )
        }

        if (gefiltert.isEmpty()) {
            SehrLeise("Nichts gefunden.")
            return@Blende
        }

        gefiltert.forEach { (name, eintraege) ->
            if (name != null) {
                Ueberschrift(name, Modifier.padding(top = Abstand.Klein))
            }
            eintraege.forEach { eintrag ->
                Wahlzeileninhalt(
                    aufschrift = aufschrift(eintrag),
                    unterschrift = unterschrift(eintrag),
                    gewaehlt = eintrag == gewaehlt,
                    beiDruck = { beiWahl(eintrag) },
                )
            }
        }
    }
}

/**
 * Eine Zeile in der Auswahl.
 *
 * Der Farbbalken links ist derselbe wie an jeder Auswahlzeile im Web: Er trägt
 * die Markierung „das ist gewählt", ohne dass dafür eine zweite Form nötig wäre.
 */
@Composable
private fun Wahlzeileninhalt(
    aufschrift: String,
    unterschrift: String?,
    gewaehlt: Boolean,
    beiDruck: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ziel.Normal)
            .background(
                color = if (gewaehlt) Farben.HauchAmber else Farben.Flaeche,
                shape = Rundung.Klein,
            )
            .border(
                width = 1.dp,
                color = if (gewaehlt) Farben.Amber else Farben.Rand,
                shape = Rundung.Klein,
            )
            .drawBehind {
                drawRect(
                    color = if (gewaehlt) Farben.Amber else Farben.RandHell,
                    size = size.copy(width = 3.dp.toPx()),
                )
            }
            .clickable(
                onClick = beiDruck,
                role = Role.RadioButton,
                indication = null,
                interactionSource = null,
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = aufschrift,
                style = Schrift.Normal,
                color = if (gewaehlt) Farben.Amber else Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unterschrift != null) SehrLeise(unterschrift)
        }
        if (gewaehlt) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(Farben.Amber, CircleShape),
            )
        }
    }
}
