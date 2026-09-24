package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Nachbesprechung — was die Schicht gebracht hat.
 *
 * Übertragen aus `web/src/views/DebriefingView.vue` in klein: Die Gutschrift
 * kommt als eigenes Ereignis (`Erfahrung`), sobald der Server abgerechnet hat —
 * erst fertig, dann einrücken, dann Geld. Ohne Gutschrift war die Runde
 * ungewertet (Sandkasten) oder zu kurz (unter fünf Minuten zählt nicht).
 */
@Composable
fun DebriefingSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Rundenstand = Rundenstand(),
    beiVerlassen: () -> Unit = {},
) {
    val gutschrift = stand.gutschrift
    val raum = stand.raum

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(titel = "Dienstende", unterzeile = "Die Schicht ist gefahren")

        if (gutschrift != null) {
            Kasten(marke = true, wartet = true, abstandInnen = Abstand.Normal) {
                Text(
                    text = "+${zahl(gutschrift.punkte)}",
                    style = Schrift.Anzeige,
                    color = Farben.AmberHell,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Punkte für diese Schicht",
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        Abstand.Klein,
                        Alignment.CenterHorizontally,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Marke("Stufe ${gutschrift.level}", farbe = Farben.AmberHell)
                    Marke(gutschrift.rang)
                    Marke("${zahl(gutschrift.gesamt)} gesamt")
                }
            }
        } else {
            Kasten(abstandInnen = Abstand.Klein) {
                Text("Keine Wertung", style = Schrift.Gross, color = Farben.Text)
                SehrLeise(
                    "Diese Schicht war ungewertet oder zu kurz — unter fünf Minuten " +
                        "zählt eine Runde nicht.",
                )
            }
        }

        if (raum != null) {
            Ueberschrift("Die Schicht")
            Kasten(abstandInnen = Abstand.Klein) {
                SehrLeise("Raum ${raum.code}")
                SehrLeise("${raum.incidents.size} Einsätze")
                SehrLeise("${raum.players.count { !it.istBot }} Menschen, " +
                    "${raum.players.count { it.istBot }} Bots")
            }
        }

        Knopf("Zum Start", beiVerlassen, art = Knopfart.Haupt, breit = true)
    }
}
