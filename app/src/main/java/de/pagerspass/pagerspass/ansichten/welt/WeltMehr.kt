package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Zeile unter dem Namen — was man dort findet, in einem Halbsatz (`ERKLAERUNG` in
 * `MehrBlende.vue`). Ein Verzeichnis ohne Erklärzeilen ist eine Liste von Wörtern.
 */
private val ERKLAERUNG = mapOf(
    Werkzeug.Grosslage to "Die eine Lage der Woche — Zeit, Ort und Bereitstellung.",
    Werkzeug.Chat to "Nachrichten an alle, an Nachbarn — und der Funk.",
    Werkzeug.Kasse to "Woher das Geld kommt, wohin es geht — und der Kredit.",
    Werkzeug.Rangliste to "Wer diese Woche am meisten verdient hat.",
    Werkzeug.Laufbahn to "Alle dreißig Stufen — was wann freischaltet.",
    Werkzeug.Einstellungen to "Gangart und Karte — wie viel gleichzeitig los ist.",
    Werkzeug.Leihe to "Fahrzeuge mieten — oder eigene gegen Credits einstellen.",
)

/**
 * Das Verzeichnis hinter dem fünften Reiter — übertragen aus `MehrBlende.vue`.
 *
 * Großeinsatz, Kasse und Rangliste sind Nachschlagewerke: Man geht einmal hin, liest und
 * geht zurück. Der Ausgang steht abgesetzt darunter, weil er kein Bereich der Welt ist,
 * sondern aus ihr heraus führt.
 */
@Composable
fun WeltMehrBlende(stand: Weltzustand, griffe: Weltgriffe) {
    val grossAktiv = stand.grosslage?.zustand == "Laeuft"

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Gross), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Werkzeug.HINTER_MEHR.forEach { w ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Ziel.Normal + Abstand.Klein)
                        .flaeche(ecke = 9.dp)
                        .clickable { griffe.oeffnen(w) }
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Icon(
                        imageVector = w.zeichen,
                        contentDescription = null,
                        tint = Farben.Amber,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                        Text(
                            text = w.titel,
                            style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold),
                            color = Farben.Text,
                        )
                        ERKLAERUNG[w]?.let {
                            Text(text = it, style = Schrift.Winzig, color = Farben.TextSehrLeise)
                        }
                    }
                    // Es gibt genau einen Großeinsatz je Woche — eine Zahl wäre immer „1",
                    // also steht hier ein Wort.
                    if (w == Werkzeug.Grosslage && grossAktiv) {
                        Text(
                            text = "LÄUFT",
                            style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                            color = Farben.AufFarbe,
                            modifier = Modifier
                                .background(Farben.Signal, Rundung.Rund)
                                .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
                        )
                    }
                    Icon(
                        imageVector = Weltzeichen.Weiter,
                        contentDescription = null,
                        tint = Farben.TextSehrLeise,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Der Ausgang, abgesetzt und leise — die Aufschrift sagt, was passiert.
        Knopf(
            aufschrift = "Welt verlassen",
            beiDruck = griffe.hinaus,
            art = Knopfart.Leise,
            zeichenVorn = {
                Icon(
                    imageVector = Weltzeichen.Zurueck,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
}
