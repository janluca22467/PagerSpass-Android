package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import de.pagerspass.pagerspass.mobil.Dienstbuchstand
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import kotlin.math.max

/**
 * Die Auswertung des eigenen Dienstbuchs — über alle Schichten statt über die
 * letzten zehn. Übertragen aus `views/dienstbuch/AuswertungView.vue`.
 *
 * <b>Sie ist Rückschau und nichts sonst.</b> Keine Zahl hier greift in eine
 * laufende Schicht ein — es sind die eigenen Zahlen, addiert. Genau deshalb darf
 * sie am Abo hängen.
 *
 * <b>Ohne Abo steht hier die Werbung und keine Fehlermeldung</b> — mit denselben
 * Überschriften, die man hinterher sieht: Wer nicht weiß, was er bekommt, kauft es
 * auch nicht.
 */
@Composable
fun DienstbuchAuswertung(
    unterrand: Dp,
    konto: Konto?,
    stand: Dienstbuchstand,
    beiReiter: (Buchreiter) -> Unit,
    beiLaden: () -> Unit,
    beiPremium: () -> Unit,
) {
    LaunchedEffect(Unit) { beiLaden() }
    var werbung by remember { mutableStateOf(false) }
    val bereich = stand.auswertung

    Dienstbuchrahmen(unterrand = unterrand, konto = konto, hier = Buchreiter.Auswertung, beiReiter = beiReiter) {
        val a = bereich.inhalt
        when {
            bereich.laedt && a == null && !stand.auswertungGesperrt -> Ladezeile("Die Auswertung wird gerechnet …")

            stand.auswertungGesperrt -> Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(randfarbe = Farben.AmberTief)
                    .flaechenmarke(wartet = true)
                    .padding(Abstand.Gross),
            ) {
                Etikett("Auswertung")
                Text("Dein ganzes Dienstbuch auf einen Blick", style = Schrift.Titel, color = Farben.Text)
                Text(
                    "Die Übersicht zeigt deine letzten zehn Schichten. Die Auswertung zeigt alle — " +
                        "wie oft du Dienst tust, wie sich deine Hilfsfrist über die Monate entwickelt " +
                        "und welchen Wagen du am liebsten fährst.",
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                )
                listOf(
                    "Schichten je Woche über ein halbes Jahr",
                    "Deine Hilfsfrist im Verlauf, nicht nur die letzte",
                    "Leitstelle oder Fahrzeug — und welches",
                ).forEach { Text("• $it", style = Schrift.Klein, color = Farben.Text) }
                Knopf("Was ist Premium?", { werbung = true }, art = Knopfart.Haupt)
            }

            bereich.fehler != null && a == null -> Fehlerzeile(bereich.fehler, beiLaden)

            a == null -> Unit

            a.schichten <= 0 -> Leerhinweis("Hier steht deine Auswertung, sobald du die erste Schicht gefahren hast.")

            else -> {
                Ueberschrift("Seit dem ${tag(a.ersteSchicht)}")
                Kachelpaar {
                    Kennzahlkachel(
                        etikett = "Schichten",
                        wert = a.schichten.toString(),
                        unter = "${a.leitstellenschichten}× Leitstelle · ${a.fahrzeugschichten}× Fahrzeug",
                        modifier = Modifier.weight(1f),
                    )
                    Kennzahlkachel(
                        etikett = "Einsätze",
                        wert = a.einsaetze.toString(),
                        unter = "${eineStelle(a.einsaetze.toDouble() / a.schichten)} je Schicht",
                        modifier = Modifier.weight(1f),
                        farbe = Farben.Blau,
                    )
                }
                Kachelpaar {
                    Kennzahlkachel(
                        etikett = "Ø Hilfsfrist",
                        wert = minutenSekunden(a.hilfsfristSekunden),
                        unter = a.besteHilfsfristSekunden?.let { "beste ${minutenSekunden(it)}" } ?: "—",
                        modifier = Modifier.weight(1f),
                        farbe = Farben.Gruen,
                    )
                    Kennzahlkachel(
                        etikett = "Punkte",
                        wert = zahl(a.punkte),
                        unter = "letzte Schicht ${tag(a.letzteSchicht)}",
                        modifier = Modifier.weight(1f),
                    )
                }

                Buchkarte(titel = "Die letzten Wochen") {
                    Wochenverlauf(a.verlauf)
                }

                if (a.fahrzeuge.isNotEmpty()) {
                    val spitze = max(1, a.fahrzeuge.maxOf { it.schichten })
                    Buchkarte(titel = "Womit du fährst") {
                        a.fahrzeuge.forEach { f ->
                            Balkenzeile(
                                name = f.typ,
                                wert = "${f.schichten}× · ${f.einsaetze} Einsätze",
                                anteil = f.schichten.toFloat() / spitze,
                            )
                        }
                    }
                }
            }
        }
    }

    if (werbung) {
        Abosperre(
            titel = "Dein ganzes Dienstbuch",
            satz = "Die Übersicht zeigt die letzten zehn Schichten. Die Auswertung zeigt alle — " +
                "über Monate, mit Verlauf.",
            punkte = listOf(
                "Schichten je Woche über ein halbes Jahr, mit den Lücken dazwischen.",
                "Deine Hilfsfrist im Verlauf statt nur der letzten zehn Schichten.",
                "Welchen Wagen du wie oft gefahren hast — und wie oft du in der Leitstelle saßt.",
            ),
            beiSchliessen = { werbung = false },
            beiPremium = {
                werbung = false
                beiPremium()
            },
        )
    }
}

/**
 * Der eine Dialog für „das gehört zu Premium" — `PremiumSperrDialog.vue`.
 *
 * <b>Zeigen, sperren, erklären:</b> Die Funktion steht da, wo sie später auch
 * stehen wird, und auf Druck sagt dieser Dialog, was dahinterliegt — mit einem Weg
 * in den Laden und einem zurück. Die ehrliche Zeile gehört an jede Stelle, an der
 * zum ersten Mal Geld ins Spiel kommt.
 */
@Composable
fun Abosperre(
    titel: String,
    satz: String,
    punkte: List<String>,
    beiSchliessen: () -> Unit,
    beiPremium: () -> Unit,
) {
    Blende(
        titel = titel,
        beiSchliessen = beiSchliessen,
        kopfknoepfe = { Text("★", style = Schrift.Titel, color = Farben.Amber) },
        fuss = {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Knopf("Später", beiSchliessen, art = Knopfart.Leise)
                Knopf("Premium ansehen", beiPremium, art = Knopfart.Haupt)
            }
        },
    ) {
        Text(
            "PAGERSPASS · PREMIUM",
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
            color = Farben.AmberHell,
        )
        Text(satz, style = Schrift.Normal, color = Farben.Text)
        punkte.forEach { Text("• $it", style = Schrift.Klein, color = Farben.TextLeise) }
        SehrLeise(
            "Premium ändert nichts an der Schicht: keine schnelleren Fahrzeuge, keine Punkte, " +
                "kein Vordrängen im Funk.",
        )
    }
}
