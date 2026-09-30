package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Werkstand
import de.pagerspass.pagerspass.netz.Lehrgang
import de.pagerspass.pagerspass.netz.Lehrgangsmodul
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Lehrgänge — das Gegenstück zu `web/src/views/LehrgangView.vue`.
 *
 * Lesen, üben, prüfen — in dieser Reihenfolge. **Wiederholen darf man immer.**
 * Das nächste offene Modul trägt den Hauptknopf; alles andere bleibt
 * erreichbar, nur leiser.
 *
 * <b>Drei Arten Modul, drei Wege.</b> Lesestoff öffnet die Wikiseite und hakt
 * sich damit selbst ab. Die Lektion ist die Ausbildungsschicht, die Prüfung
 * eine eigene Prüfungsschicht — beide erledigen sich durch Fahren, dafür gibt
 * es bewusst keinen Haken zum Setzen.
 */
@Composable
fun LehrgangSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Werkstand = Werkstand(),
    beiLaden: (Boolean) -> Unit = {},
    beiLesen: (Lehrgang, Lehrgangsmodul) -> Unit = { _, _ -> },
    beiLektion: () -> Unit = {},
    beiPruefung: (Lehrgang, Lehrgangsmodul) -> Unit = { _, _ -> },
    beiSchicht: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden(false) }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Lehrgänge",
            unterzeile = "Lesen, üben, prüfen — in dieser Reihenfolge. Wiederholen darfst du immer.",
            knoepfe = { Knopf("← Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Rueckmeldung(null, stand.fehler)

        Bereich(
            laedt = stand.lehrgaenge.ersteLadung,
            fehler = stand.lehrgaenge.fehler,
            inhalt = stand.lehrgaenge.inhalt,
            beiErneut = { beiLaden(true) },
        ) { lehrgaenge ->
            if (lehrgaenge.isEmpty()) {
                Leerhinweis("Auf diesem Server sind keine Lehrgänge hinterlegt.")
            }
            lehrgaenge.forEach { l ->
                Kurs(l, stand.laeuft, beiLesen, beiLektion, beiPruefung, beiSchicht)
            }
        }
    }
}

private val ARTBEZEICHNUNG = mapOf("Lesestoff" to "Lesen", "Lektion" to "Lektion", "Pruefung" to "Prüfung")

@Composable
private fun Kurs(
    l: Lehrgang,
    laeuft: Boolean,
    beiLesen: (Lehrgang, Lehrgangsmodul) -> Unit,
    beiLektion: () -> Unit,
    beiPruefung: (Lehrgang, Lehrgangsmodul) -> Unit,
    beiSchicht: (String) -> Unit,
) {
    val dran = if (l.gesperrt) null else l.module.firstOrNull { !it.erledigt }?.id

    Kasten(abstandInnen = Abstand.Klein, marke = l.bestanden) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (l.gesperrt) "🔒 " else "") + l.titel,
                style = Schrift.Gross,
                color = if (l.gesperrt) Farben.TextLeise else Farben.Text,
                modifier = Modifier.weight(1f),
            )
            when {
                l.bestanden -> Marke("Bestanden", farbe = Farben.GruenHell)
                l.gesperrt -> Marke("Gesperrt")
            }
        }
        Leise(l.beschreibung)

        // Die Sperre nennt ihren Grund — ein ausgegrauter Knopf ohne Satz
        // daneben sieht aus wie ein Fehler.
        if (l.gesperrt) {
            Text(sperrsatz(l), style = Schrift.Klein, color = Farben.AmberHell)
        }

        if (l.bestanden) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(farbe = Farben.HauchAmber, randfarbe = Farben.AmberTief, ecke = 9.dp)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text("${l.titel} — bestanden am ${tag(l.bestandenUm)}.", style = Schrift.Klein, color = Farben.Text)
                l.zeugnisSchicht?.let { code -> Textweg("Prüfungsschicht ansehen →", { beiSchicht(code) }) }
            }
        }

        l.module.forEach { m ->
            val istDran = m.id == dran
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(
                        farbe = Farben.FlaecheHoch,
                        randfarbe = if (istDran) Farben.Amber else Farben.Rand,
                        ecke = 9.dp,
                    )
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text(
                    if (m.erledigt) "✓" else "○",
                    style = Schrift.Gross,
                    color = if (m.erledigt) Farben.GruenHell else Farben.TextSehrLeise,
                )
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                    SehrLeise(ARTBEZEICHNUNG[m.art] ?: m.art, mono = true)
                    Text(m.titel, style = Schrift.Normal, color = Farben.Text)
                    SehrLeise(m.text)
                    // Am Versuch aufgehängt, nicht an `erledigt`: Eine nicht
                    // bestandene Prüfung gilt ausdrücklich als nicht erledigt —
                    // und genau dann muss dieser Satz stehen.
                    if (m.art == "Pruefung" && m.erledigtUm != null) {
                        Text(
                            (if (m.bestanden) "Bestanden" else "Noch nicht bestanden — fahr sie noch einmal.") +
                                " · ${tag(m.erledigtUm)}",
                            style = Schrift.Klein,
                            color = if (m.bestanden) Farben.GruenHell else Farben.SignalHell,
                        )
                    }
                    val art = if (istDran) Knopfart.Haupt else Knopfart.Normal
                    when (m.art) {
                        "Lesestoff" -> Knopf(
                            if (m.erledigt) "Nochmal lesen" else "Lesen",
                            { beiLesen(l, m) },
                            art = art,
                            aktiv = !l.gesperrt && m.wikiseite != null,
                            kompakt = true,
                        )
                        "Lektion" -> Knopf(
                            if (m.erledigt) "Nochmal fahren" else "Fahren",
                            beiLektion,
                            art = art,
                            aktiv = !l.gesperrt && !laeuft,
                            kompakt = true,
                        )
                        else -> Knopf(
                            if (m.bestanden) "Nochmal prüfen" else "Prüfung fahren",
                            { beiPruefung(l, m) },
                            art = art,
                            aktiv = !l.gesperrt && !laeuft,
                            kompakt = true,
                        )
                    }
                }
            }
        }
    }
}

private fun sperrsatz(l: Lehrgang): String {
    if (l.voraussetzungen.isEmpty()) return "Dieser Lehrgang ist noch gesperrt."
    val namen = l.voraussetzungen.map { "„$it“" }
    val liste = if (namen.size == 1) namen[0] else "${namen.dropLast(1).joinToString(", ")} und ${namen.last()}"
    return "Erst nach $liste."
}
