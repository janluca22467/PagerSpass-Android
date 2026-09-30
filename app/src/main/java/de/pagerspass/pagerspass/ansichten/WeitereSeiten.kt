package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Buchdaten
import de.pagerspass.pagerspass.netz.Abzeichen
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Schicht
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Markenzahl
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.schmuck.Profilzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Die fünf übrigen Wege der Tableiste, angebunden.
 *
 * <b>Jede Seite lädt beim Öffnen und nur, was fehlt.</b> Der Ladeaufruf steht in
 * einem `LaunchedEffect` und die Sitzung bricht ab, wenn schon etwas dasteht —
 * sonst holt jeder Wechsel zwischen zwei Reitern der Leiste alles neu.
 *
 * <b>Die Seiten kennen die Sitzung nicht.</b> Sie bekommen ihren Bereich und
 * zwei Rückrufe. Das ist dieselbe Trennung wie beim Startbildschirm: Was der
 * Server liefert und was die Seite zeigt, sind zwei Dinge, und die Naht dazwischen
 * gehört an eine Stelle (`PagerSpassApp`).
 */

// ---------------------------------------------------------------- Dienstbuch

// Das Dienstbuch steht in `DienstbuchSeiten.kt` — mit fünf Reitern ist es
// eine eigene Seite und kein Abschnitt dieser Datei mehr.

// -------------------------------------------------------------------- Wache

/**
 * Die Wache.
 *
 * <b>Entweder die eigene oder die Suche</b> — nie beides. Wer in einer
 * Gemeinschaft ist, will hinein; wer in keiner ist, zur Liste. Dieselbe Weiche
 * wie in der Tableiste des Webs: Derselbe Knopf soll auf beiden Zweigen
 * denselben Weg nehmen.
 */
// ------------------------------------------------------------------ Freunde

/**
 * Freunde — Liste, Anfragen, Vorschläge.
 *
 * <b>Eine Liste trägt alle drei Reiter.</b> Der Server liefert sie in einem Zug;
 * `stand` sagt, wohin jemand gehört, und bei einer offenen Anfrage entscheidet
 * `vonMir`, ob man wartet oder am Zug ist. Genau dieser Unterschied macht den
 * Reiter „Anfragen" nützlich: Nur die eine Hälfte davon kann man beantworten.
 */
@Composable
fun FreundeSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    freunde: Bereichsstand<List<Freund>> = Bereichsstand(),
    server: String = "",
    beiLaden: () -> Unit = {},
    beiAntwort: (String, Boolean) -> Unit = { _, _ -> },
    /** Ein Tipp auf einen bestätigten Freund öffnet das Gespräch. */
    beiGespraech: (Freund) -> Unit = {},
    brett: @Composable ColumnScope.() -> Unit = {},
) {
    var reiter by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(Unit) { beiLaden() }

    val alle = freunde.inhalt.orEmpty()
    val liste = alle.filter { it.bestaetigt }
    val anfragen = alle.filter { it.angefragt }
    val zuBeantworten = anfragen.count { !it.vonMir }
    val ungelesen = liste.sumOf { it.ungelesen }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(titel = "Freunde", unterzeile = "Brett, Gespräche und wer mit dir fährt")

        Reiterreihe {
            Reiter("Brett", offen = reiter == 0, beiDruck = { reiter = 0 })
            Reiter(
                "Freunde",
                offen = reiter == 1,
                beiDruck = { reiter = 1 },
                marke = ungelesen,
            )
            Reiter(
                "Anfragen",
                offen = reiter == 2,
                beiDruck = { reiter = 2 },
                marke = zuBeantworten,
            )
        }

        if (reiter == 0) {
            brett()
            return@Seite
        }

        Bereich(
            laedt = freunde.laedt,
            fehler = freunde.fehler,
            inhalt = freunde.inhalt,
            beiErneut = beiLaden,
        ) {
            if (reiter == 1) {
                if (liste.isEmpty()) {
                    Leerhinweis(
                        "Noch niemand auf der Liste. Im Web kannst du jemanden über seinen " +
                            "Benutzernamen suchen.",
                    )
                } else {
                    liste.forEach { freund ->
                        Freundzeile(freund, server, beiDruck = { beiGespraech(freund) })
                    }
                }
            } else {
                if (anfragen.isEmpty()) {
                    Leerhinweis("Keine offenen Anfragen.")
                } else {
                    anfragen.forEach { Freundzeile(it, server, beiAntwort) }
                }
            }
        }
    }
}

@Composable
private fun Freundzeile(
    freund: Freund,
    server: String,
    beiAntwort: ((String, Boolean) -> Unit)? = null,
    beiDruck: (() -> Unit)? = null,
) {
    val offenVonAnderen = freund.angefragt && !freund.vonMir

    Profilzeile(
        beiDruck = beiDruck,
        kennung = freund.kennung,
        anzeigename = freund.anzeigename.ifBlank { freund.benutzername },
        // Wer gerade fährt, ist die interessantere Auskunft als „zuletzt online"
        // — sie verdrängt diese, statt danebenzustehen.
        unterzeile = listOfNotNull(
            "Stufe ${freund.level}",
            freund.rang.ifBlank { null },
            freund.anwesenheit?.ansage
                ?: seither(freund.zuletztGesehen)?.let { "zuletzt $it" },
        ).joinToString(" · "),
        wappen = freund.wappen,
        wappenfarbe = freund.wappenfarbe,
        kopfmuster = freund.kopfmuster,
        profilrahmen = freund.profilrahmen,
        bildAdresse = bildweg(server, freund.profilbild),
        premium = freund.premium,
        teammitglied = freund.teammitglied,
        imDienst = freund.anwesenheit != null,
        hinten = {
            when {
                freund.ungelesen > 0 -> Markenzahl(freund.ungelesen)
                freund.angefragt && freund.vonMir -> Marke("Gesendet")
                freund.anwesenheit != null -> Marke("Im Dienst", farbe = Farben.GruenHell)
            }
        },
        unten = {
            if (offenVonAnderen && beiAntwort != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        "Annehmen",
                        { beiAntwort(freund.kennung, true) },
                        art = Knopfart.Haupt,
                        kompakt = true,
                    )
                    Knopf(
                        "Ablehnen",
                        { beiAntwort(freund.kennung, false) },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }
        },
    )
}

// Shop und Konto stehen in `ShopSeiten.kt` und `KontoSeiten.kt` — sie sind
// mit Premium, Postfach und den Nebenwegen des Kontos zu groß für diese Sammlung
// geworden.
