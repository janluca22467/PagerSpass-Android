package de.pagerspass.pagerspass.ansichten.welt

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/** Eine Lektion der Einführung — `LEKTIONEN` in `einfuehrung.ts`. */
private data class Lektion(
    val titel: String,
    val text: String,
    val ziel: Werkzeug,
    val erledigt: (Weltzustand, Set<Werkzeug>) -> Boolean,
)

private val LEKTIONEN = listOf(
    Lektion(
        "Baue deine erste Wache",
        "Ohne Wache passiert in der Welt nichts: Erst wo eine steht, entstehen Lagen in der Umgebung — " +
            "und je mehr Wachen du hast, desto mehr davon. Öffne „Bauen“, drück auf „Bauen“, wähle eine " +
            "Bauart und tipp dann auf die Karte, wo sie stehen soll.",
        Werkzeug.Bauen,
    ) { z, _ -> !z.stand?.wachen.isNullOrEmpty() },
    Lektion(
        "Stell ein Fahrzeug hinein",
        "Eine leere Wache rückt nicht aus. Unter „Bauen“ → „Fahrzeug kaufen“ kaufst du das erste — ein " +
            "Löschfahrzeug ist der Anfang, alles Weitere schaltet sich mit deiner Stufe frei.",
        Werkzeug.Bauen,
    ) { z, _ -> z.fahrzeuge.isNotEmpty() },
    Lektion(
        "Alarmiere deine erste Lage",
        "Unter „Lagen“ stehen deine Einsätze, der nächstgelegene oben. Zeile antippen, Fahrzeug anhaken, " +
            "alarmieren. Was in deinem Bereich entsteht, siehst zuerst nur du — schaffst du es nicht allein, " +
            "gibst du die Lage für alle frei.",
        Werkzeug.Lagen,
    ) { z, _ -> z.fahrzeuge.any { it.lage != "Wache" } },
    Lektion(
        "Behalte die Anfahrt im Blick",
        "Unter „Fahrzeuge“ siehst du, was jeder Wagen gerade tut und wie lange er noch braucht. Auf der " +
            "Karte fährt er die echte Straße — deshalb entscheidet der Standort deiner Wache über alles.",
        Werkzeug.Fahrzeuge,
    ) { _, gesehen -> Werkzeug.Fahrzeuge in gesehen },
    Lektion(
        "Der Großeinsatz der Woche",
        "Einmal in der Woche gibt es eine Lage, die niemand allein deckt — für alle dieselbe. Unter " +
            "„Mehr“ → „Großeinsatz“ steht, wann und wo es losgeht und wie du Fahrzeuge vorher schon hinschickst.",
        Werkzeug.Grosslage,
    ) { _, gesehen -> Werkzeug.Grosslage in gesehen },
)

/**
 * Die Einführung — das Gegenstück zu `WeltEinfuehrung.vue` und `einfuehrung.ts`.
 *
 * <b>Sie zählt, was geschafft ist, und nicht, was angeklickt wurde.</b> Eine
 * Lektion gilt, sobald der Stand sie erfüllt (eine Wache steht, ein Fahrzeug
 * fährt) — wer schon weiter ist, sieht sie nie. Gemerkt wird je Konto auf dem
 * Gerät, wie im Web im Browser.
 *
 * @return das Werkzeug, auf das die Lektion gerade zeigt — die Leiste hebt es hervor.
 */
@Composable
fun WeltEinfuehrung(zustand: Weltzustand, werkbank: Werkbank, zeigen: Boolean): Werkzeug? {
    val zusammenhang = LocalContext.current
    val kennung = zustand.kennung ?: return null
    val ablage = remember { zusammenhang.getSharedPreferences("pagerspass_welt", Context.MODE_PRIVATE) }
    var geschafft by remember(kennung) { mutableIntStateOf(ablage.getInt("einfuehrung.$kennung.g", 0).coerceIn(0, LEKTIONEN.size)) }
    var beendet by remember(kennung) { mutableStateOf(ablage.getBoolean("einfuehrung.$kennung.b", false)) }
    var gesehen by remember { mutableStateOf(setOf<Werkzeug>()) }

    fun sichern() {
        ablage.edit().putInt("einfuehrung.$kennung.g", geschafft).putBoolean("einfuehrung.$kennung.b", beendet).apply()
    }

    LaunchedEffect(werkbank.seite) { werkbank.seite?.let { gesehen = gesehen + it } }
    LaunchedEffect(zustand.stand, zustand.betrieb, gesehen) {
        if (beendet) return@LaunchedEffect
        val vorher = geschafft
        while (geschafft < LEKTIONEN.size && LEKTIONEN[geschafft].erledigt(zustand, gesehen)) geschafft++
        if (geschafft != vorher) sichern()
    }

    if (beendet) return null
    val lektion = LEKTIONEN.getOrNull(geschafft)
    if (!zeigen) return lektion?.ziel

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .padding(horizontal = Abstand.Klein)
            .fillMaxWidth()
            .flaeche(farbe = Farben.FlaecheHoch.copy(alpha = 0.96f), randfarbe = Farben.AmberTief, ecke = 12.dp)
            .padding(Abstand.Normal),
    ) {
        if (lektion == null) {
            Text("Einführung geschafft", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
            Leisesatz("Du kennst jetzt die Handgriffe. Der Rest ist Disponieren — viel Erfolg in der Welt.")
            Knopf("Schließen", { beendet = true; sichern() }, kompakt = true)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Schritt ${geschafft + 1} von ${LEKTIONEN.size} · ${lektion.titel}",
                    style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Amber,
                    modifier = Modifier.weight(1f),
                )
            }
            Leisesatz(lektion.text, winzig = true)
            Umbruchreihe {
                Knopf("Diesen Schritt überspringen", {
                    geschafft++
                    sichern()
                }, kompakt = true, art = Knopfart.Leise)
                Knopf("Einführung beenden", { beendet = true; sichern() }, kompakt = true, art = Knopfart.Leise)
            }
        }
    }
    return lektion?.ziel
}
