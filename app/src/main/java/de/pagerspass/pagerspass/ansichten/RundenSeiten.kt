package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Segment
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
    /** „Eine Übung vorbereiten" im leeren Fall — ohne Angabe bleibt der Knopf weg. */
    beiUebungen: (() -> Unit)? = null,
) {
    // Die Liste zieht von selbst nach, alle acht Sekunden wie im Web — wer eine
    // volle Runde sieht, soll den freien Platz bemerken, ohne neu zu laden.
    LaunchedEffect(Unit) {
        while (true) {
            beiLaden()
            kotlinx.coroutines.delay(8_000)
        }
    }

    var zustand by rememberSaveable { mutableStateOf("alle") }
    var nurFreie by rememberSaveable { mutableStateOf(false) }
    val runden = stand.inhalt.orEmpty()

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            etikett = "Runden",
            titel = "Öffentliche Runden",
            unterzeile = "Runden, deren Leitstelle sie für Fremde geöffnet hat. Einen Raumcode gibst " +
                "du auf dem Start ein.",
            knoepfe = { Livezeichen() },
        )

        // Drei Zahlen, bevor man liest: ob sich das Stöbern gerade lohnt.
        val geladen = stand.inhalt != null
        val offen = runden.count { it.state != "Beendet" }
        val imDienst = runden.count { it.state == "Laeuft" }
        val frei = runden.filter { beitretbar(it) }.sumOf { it.freiePlaetze }
        Kennzahltafel(
            listOf(
                { m -> Kennzahlkachel("Offen", if (geladen) "$offen" else "–", "öffentliche Runden", m) },
                { m -> Kennzahlkachel("Im Dienst", if (geladen) "$imDienst" else "–", "laufen gerade", m) },
                { m -> Kennzahlkachel("Frei", if (geladen) "$frei" else "–", "Plätze insgesamt", m, farbe = Farben.Gruen) },
            ),
        )

        Segment(
            seiten = listOf("alle", "Lobby", "Laeuft"),
            gewaehlt = zustand,
            beiWahl = { zustand = it },
            aufschrift = { ZUSTAENDE[it] ?: it },
            modifier = Modifier.fillMaxWidth(),
        )
        Hakenzeile("Nur mit freien Plätzen", nurFreie, { nurFreie = it })

        Bereich(
            laedt = stand.laedt && stand.inhalt == null,
            fehler = stand.fehler,
            inhalt = stand.inhalt,
            beiErneut = beiLaden,
        ) { alle ->
            // Wer hinein kann, steht vorn; darunter laufende vor wartenden, volle vor leeren.
            val sichtbar = alle
                .filter { zustand == "alle" || it.state == zustand }
                .filter { !nurFreie || beitretbar(it) }
                .sortedWith(
                    compareByDescending<OeffentlicheRunde> { beitretbar(it) }
                        .thenByDescending { it.state == "Laeuft" }
                        .thenByDescending { it.spieler },
                )
            when {
                alle.isEmpty() -> Kasten(abstandInnen = Abstand.Klein) {
                    Leise(
                        "Gerade ist keine öffentliche Runde offen. Frag in deiner Wache nach einem " +
                            "Raumcode — oder besetze selbst die Leitstelle und öffne deine Runde für Fremde.",
                        klein = false,
                    )
                    if (beiUebungen != null) Knopf("Eine Übung vorbereiten", beiUebungen, kompakt = true)
                }
                sichtbar.isEmpty() -> Kasten(abstandInnen = Abstand.Klein) {
                    Leise("Zu dieser Auswahl passt gerade keine Runde.", klein = false)
                    Knopf(
                        "Alle Runden zeigen",
                        {
                            zustand = "alle"
                            nurFreie = false
                        },
                        kompakt = true,
                    )
                }
                else -> sichtbar.forEach { r ->
                    Rundenkarte(r, laeuft, beiBeitreten, beiZuschauen)
                }
            }
        }
    }
}

private val ZUSTAENDE = mapOf("alle" to "Alle", "Lobby" to "Wartet", "Laeuft" to "Im Dienst")

private fun beitretbar(r: OeffentlicheRunde) = r.state != "Beendet" && r.freiePlaetze > 0

/** Der Punkt mit „Live" — die Liste zieht von selbst nach. */
@Composable
private fun Livezeichen() {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(Farben.Gruen, CircleShape))
        Text("LIVE", style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.GruenHell)
    }
}

/**
 * Eine öffentliche Runde — Status und Code im Kopf, Leitstelle und Bereich, die
 * Belegung als Balken („komme ich noch hinein?" ist die erste Frage), die Merkmale
 * als Marken und der eine Knopf. Was man vor dem Beitritt gelesen haben soll
 * („nicht gewertet", „wird gestreamt"), steht in Amber, nicht in Signalfarbe.
 */
@Composable
private fun Rundenkarte(
    r: OeffentlicheRunde,
    laeuft: Boolean,
    beiBeitreten: (String) -> Unit,
    beiZuschauen: (String) -> Unit,
) {
    val offen = beitretbar(r)
    Kasten(abstandInnen = Abstand.Klein) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val imDienst = r.state == "Laeuft"
            Box(Modifier.size(8.dp).background(if (imDienst) Farben.Amber else Farben.Gruen, CircleShape))
            Text(
                statusText(r.state),
                style = Schrift.Klein,
                color = if (imDienst) Farben.AmberHell else Farben.TextLeise,
                modifier = Modifier.weight(1f),
            )
            Text(r.code, style = Schrift.MonoNormal, color = Farben.TextSehrLeise)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            Text(
                r.leitstelle.ifBlank { r.code },
                style = Schrift.Gross,
                color = Farben.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SehrLeise("${r.landkreis ?: "Erfundener Bereich"} · ${modusText(r.mode)}")
        }
        val anteil = if (r.maxSpieler <= 0) 0f else (r.spieler.toFloat() / r.maxSpieler).coerceIn(0.03f, 1f)
        Fortschritt(
            anteil = anteil,
            text = "${r.spieler} / ${r.maxSpieler} an Bord · " +
                if (r.freiePlaetze > 0) "${r.freiePlaetze} frei" else "voll",
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        ) {
            Marke(
                if (r.leitstelleBesetzt) "Leitstelle besetzt" else "Leitstelle frei",
                farbe = if (r.leitstelleBesetzt) Farben.TextLeise else Farben.TextSehrLeise,
            )
            if (r.state == "Laeuft") {
                Marke(
                    "${r.offeneEinsaetze} ${if (r.offeneEinsaetze == 1) "offener Einsatz" else "offene Einsätze"}",
                    farbe = Farben.TextLeise,
                )
            }
            if (r.sandkasten) Marke("Sandkasten · nicht gewertet", farbe = Farben.AmberHell)
            if (r.streamermodus) Marke("Wird gestreamt", farbe = Farben.AmberHell)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                when {
                    offen -> "Beitreten"
                    r.state == "Beendet" -> "Beendet"
                    else -> "Voll"
                },
                { beiBeitreten(r.code) },
                art = if (offen) Knopfart.Haupt else Knopfart.Normal,
                aktiv = !laeuft && offen,
                modifier = Modifier.weight(1f),
            )
            // Nur in der App: still dazuschauen — kein Platz, keine Rolle.
            Knopf(
                "Zuschauen",
                { beiZuschauen(r.code) },
                art = Knopfart.Leise,
                aktiv = !laeuft && r.state != "Beendet",
            )
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
                    SehrLeise("Du bist die Schicht heute schon gefahren — morgen gibt es eine neue.")
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
