package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.netz.OeffentlicheRunde
import de.pagerspass.pagerspass.netz.Tagesschicht
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die zwei Wege in fremde Runden — übertragen aus dem Startbereich des Web.
 *
 * <b>Öffentliche Runden</b>: die Liste derer, die gerade Verstärkung suchen.
 * <b>Schicht des Tages</b>: dieselbe Lage für alle, gewertet, einmal am Tag.
 */

/** Wie der Rundenstatus in der Liste heißt — `RUNDE_STATUS_LABEL` im Web. */
private fun statusText(state: String): String = when (state) {
    "Lobby" -> "Wartet auf Dienstbeginn"
    "Laeuft" -> "Im Dienst"
    "Beendet" -> "Beendet"
    else -> state
}

/** Wie der Spielmodus heißt, wo er nur benannt und nicht gewählt wird. */
private fun modusText(mode: String): String = when (mode) {
    "Zufall" -> "Zufallseinsätze"
    "Frei" -> "Freie Vergabe"
    "Ausbildung" -> "Ausbildungsschicht"
    "Tagesschicht" -> "Schicht des Tages"
    "Szenario" -> "Übung"
    else -> mode
}

@Composable
fun OeffentlicheRundenSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Bereichsstand<List<OeffentlicheRunde>> = Bereichsstand(),
    laeuft: Boolean = false,
    beiLaden: () -> Unit = {},
    beiBeitreten: (String) -> Unit = {},
    beiZuschauen: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Öffentliche Runden",
            unterzeile = "Wo gerade jemand Verstärkung sucht",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = stand.laedt,
            fehler = stand.fehler,
            inhalt = stand.inhalt,
            beiErneut = beiLaden,
        ) { runden ->
            if (runden.isEmpty()) {
                Leerhinweis("Gerade sucht niemand Verstärkung. Schau später wieder rein.")
            } else {
                runden.forEach { runde ->
                    Kasten(abstandInnen = Abstand.Klein) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = runde.leitstelle.ifBlank { runde.code },
                                style = Schrift.Gross,
                                color = Farben.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Marke(
                                statusText(runde.state),
                                farbe = if (runde.state == "Laeuft") Farben.Amber else Farben.TextLeise,
                            )
                        }
                        SehrLeise(
                            listOfNotNull(
                                runde.landkreis,
                                modusText(runde.mode),
                                "${runde.spieler}/${runde.maxSpieler} Spieler",
                                "${runde.freiePlaetze} frei".takeIf { runde.freiePlaetze > 0 },
                                "${runde.offeneEinsaetze} offene Einsätze"
                                    .takeIf { runde.offeneEinsaetze > 0 },
                                "Sandkasten — ungewertet".takeIf { runde.sandkasten },
                                "Leitstelle gesucht".takeIf { !runde.leitstelleBesetzt },
                                // Runde, Teil 1: voll heißt nicht zu — zuschauen geht.
                                "voll".takeIf { runde.freiePlaetze <= 0 },
                            ).joinToString(" · "),
                        )
                        // Runde, Teil 1: der Code zum Weitersagen und der Hinweis
                        // auf die Übertragung — gefragt wird beim Beitritt.
                        SehrLeise("Code ${runde.code}", mono = true)
                        if (runde.streamermodus) {
                            Text(
                                "Wird gestreamt — du wirst gefragt.",
                                style = Schrift.Klein,
                                color = Farben.AmberHell,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf(
                                "Beitreten",
                                { beiBeitreten(runde.code) },
                                aktiv = !laeuft && runde.freiePlaetze > 0 && runde.state != "Beendet",
                                kompakt = true,
                            )
                            Knopf(
                                "Zuschauen",
                                { beiZuschauen(runde.code) },
                                art = Knopfart.Leise,
                                aktiv = !laeuft && runde.state != "Beendet",
                                kompakt = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagesschichtSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Bereichsstand<Tagesschicht?> = Bereichsstand(),
    laeuft: Boolean = false,
    beiLaden: () -> Unit = {},
    beiStart: () -> Unit = {},
    beiZurueck: () -> Unit = {},
    // Runde, Teil 1: der Weg zur ganzen Tagesliste.
    beiDienstbuch: (() -> Unit)? = null,
) {
    LaunchedEffect(Unit) { beiLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Schicht des Tages",
            unterzeile = "Gewertet, für alle dieselbe",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = stand.laedt,
            fehler = stand.fehler,
            inhalt = stand.inhalt,
            beiErneut = beiLaden,
        ) { schicht ->
            Kasten(marke = true) {
                SehrLeise(
                    listOfNotNull(
                        schicht.landkreis?.let { "Heute: $it" },
                        "${schicht.gefahren} gefahren",
                    ).joinToString(" · "),
                )
                if (schicht.selbstGefahren) {
                    // Einmal am Tag — wer gefahren ist, sieht seinen Platz und
                    // keinen Knopf, der ohnehin abgewiesen würde.
                    Text(
                        text = listOfNotNull(
                            schicht.eigenerPlatz?.let { "Platz $it" },
                            schicht.eigenePunkte?.let { "$it Punkte" },
                        ).joinToString(" · ").ifBlank { "Heute schon gefahren." },
                        style = Schrift.Gross,
                        color = Farben.Amber,
                    )
                    SehrLeise(
                        "Gewertet wird die erste Fahrt des Tages. Du kannst danach weiter fahren — " +
                            "ohne Wertung, zum Üben.",
                    )
                    // Runde, Teil 1: Der Knopf bleibt — wie im Web.
                    Knopf("Noch einmal fahren", beiStart, art = Knopfart.Normal, aktiv = !laeuft)
                } else {
                    SehrLeise(
                        "Festes Skript, Bots als Besatzung, dieselbe Lage für alle. " +
                            "Die Punkte zählen für die Tagesliste.",
                    )
                    Knopf(
                        "Schicht fahren",
                        beiStart,
                        art = Knopfart.Haupt,
                        aktiv = !laeuft,
                    )
                }
            }

            Ueberschrift("Tagesliste")
            beiDienstbuch?.let { weg ->
                Knopf("Ganze Tagesliste im Dienstbuch ansehen", weg, art = Knopfart.Leise, kompakt = true)
            }
            if (schicht.beste.isEmpty()) {
                Leerhinweis("Noch niemand gefahren — sei der Erste.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    schicht.beste.forEach { platz ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "${platz.platz}.",
                                style = Schrift.MonoKlein,
                                color = if (platz.istEigenerEintrag) Farben.Amber
                                else Farben.TextSehrLeise,
                            )
                            Text(
                                text = platz.anzeigename,
                                style = Schrift.Klein,
                                color = if (platz.istEigenerEintrag) Farben.Amber else Farben.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${platz.punkte}",
                                style = Schrift.MonoKlein,
                                color = Farben.TextLeise,
                            )
                        }
                    }
                }
            }
        }
    }
}
