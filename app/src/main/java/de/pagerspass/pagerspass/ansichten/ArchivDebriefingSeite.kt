package de.pagerspass.pagerspass.ansichten

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.netz.ArchivRunde
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Seite

/**
 * Die Nachbesprechung einer archivierten Schicht — aufgerufen aus dem Dienstbuch.
 *
 * Übertragen aus `views/DebriefingView.vue` in dem Zweig, den das Web für eine
 * gereichte `runde` nimmt: dieselbe Auswertung, dieselbe Darstellung, nur die
 * Quelle ist das Archiv (`GET /api/archiv/{code}`). Anders als in der frisch
 * beendeten Runde steht hier die <b>ganze</b> Schicht: das vollständige
 * Funkprotokoll, das Anrufjournal, und das Replay im Zeitraffer.
 *
 * <b>Die Gutschrift fehlt mit Absicht.</b> Sie gehört zur gerade gefahrenen
 * Schicht, nicht zu einer archivierten — beim Blättern im Archiv wäre sie eine Zahl
 * aus einem anderen Abend. Was die Schicht gebracht hat, steht an ihrer Zeile im
 * Dienstbuch.
 *
 * <b>Die Darstellung steht in `DebriefingBausteine.kt`</b> — dieselbe, die auch die
 * gerade beendete Schicht zeigt (`DebriefingSeite`); hier bleibt nur die Seite drum
 * herum: Laden, Fehler, Weg zurück, Motto-Werbung.
 */
@Composable
fun ArchivDebriefingSeite(
    unterrand: Dp,
    code: String,
    konto: Konto?,
    stand: Bereichsstand<ArchivRunde>,
    freunde: List<Freund>,
    beiLaden: () -> Unit,
    beiZurueck: () -> Unit,
    beiAnfragen: suspend (String) -> Result<Unit>,
    beiPremium: () -> Unit,
) {
    LaunchedEffect(code) { beiLaden() }
    var mottowerbung by remember { mutableStateOf(false) }

    Seite(unterrand = unterrand) {
        Knopf("← Zurück zu den Schichten", beiZurueck, art = Knopfart.Leise, kompakt = true)

        val runde = stand.inhalt?.takeIf { it.code.equals(code, ignoreCase = true) }
        if (runde == null) {
            val fehler = stand.fehler
            if (fehler != null) Fehlerzeile(fehler, beiLaden) else Ladezeile("Die Schicht wird geholt …")
            return@Seite
        }

        Nachbesprechung(
            runde = runde,
            konto = konto,
            freunde = freunde,
            beiAnfragen = beiAnfragen,
            beiMottowerbung = { mottowerbung = true },
        )
    }

    if (mottowerbung) {
        Abosperre(
            titel = "Deine Zeile auf der Schichtkarte",
            satz = "Unter dem Namen der Leitstelle steht mit Premium ein Satz, den du selbst " +
                "schreibst — auf jedem Bild, das du nach der Schicht teilst.",
            punkte = listOf(
                "Dein Motto bleibt stehen und gilt für jede weitere Schicht.",
                "Dazu die Abo-Gestaltungen der Karte — oben mit einem Stern gekennzeichnet.",
                "Die Kennzahlen darauf sind dieselben wie ohne Abo. Geschönt wird nichts.",
            ),
            beiSchliessen = { mottowerbung = false },
            beiPremium = {
                mottowerbung = false
                beiPremium()
            },
        )
    }
}
