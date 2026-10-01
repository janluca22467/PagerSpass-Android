package de.pagerspass.pagerspass.ansichten.welt

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.ui.theme.Rundung
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
        "Eine leere Wache rückt nicht aus. Unter „Fahrzeuge“ → „Fahrzeug kaufen“ kaufst du das erste — ein " +
            "Löschfahrzeug ist der Anfang, alles Weitere schaltet sich mit deiner Stufe frei.",
        Werkzeug.Fahrzeuge,
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

    // Die Karte aus `WeltEinfuehrung.vue`: oben der Zähler, die Punkte (Anzeige,
    // kein Weg) und das Kreuz; darunter Titel und Text; unten zwei Wege hinaus —
    // „Nicht mehr zeigen“ als Wort, damit niemand fünfmal überspringen muss.
    val fertig = lektion == null
    Column(
        modifier = Modifier
            .padding(horizontal = Abstand.Gross)
            .fillMaxWidth()
            .flaeche(
                farbe = Farben.FlaecheHoch.copy(alpha = 0.96f),
                randfarbe = if (fertig) Farben.GruenHell else Farben.AmberTief,
                ecke = 18.dp,
            )
            .padding(start = Abstand.Gross, end = Abstand.Gross, top = Abstand.Normal, bottom = Abstand.Gross),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.padding(bottom = Abstand.Klein),
        ) {
            Text(
                if (fertig) "EINFÜHRUNG" else "SCHRITT ${geschafft + 1} VON ${LEKTIONEN.size}",
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, letterSpacing = 0.8.sp),
                color = if (fertig) Farben.GruenHell else Farben.Amber,
                modifier = Modifier
                    .background(if (fertig) Farben.Gruen.copy(alpha = 0.12f) else Farben.HauchAmber, Rundung.Rund)
                    .border(1.dp, if (fertig) Farben.Gruen else Farben.AmberTief, Rundung.Rund)
                    .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                LEKTIONEN.indices.forEach { i ->
                    val an = !fertig && i == geschafft
                    Box(
                        Modifier
                            .size(width = if (an) 18.dp else 6.dp, height = 6.dp)
                            .background(
                                when {
                                    an -> Farben.Amber
                                    fertig -> Farben.Gruen
                                    i < geschafft -> Farben.AmberTief
                                    else -> Farben.RandHell
                                },
                                Rundung.Rund,
                            ),
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clickable { beendet = true; sichern() }
                    .semantics { contentDescription = "Einführung beenden" },
            ) {
                Icon(Weltzeichen.Kreuz, contentDescription = null, tint = Farben.TextSehrLeise, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            if (fertig) "Geschafft — der Rest ist Disponieren" else lektion!!.titel,
            style = Schrift.Gross.copy(fontWeight = FontWeight.Bold),
            color = Farben.Text,
            modifier = Modifier.padding(bottom = Abstand.Winzig),
        )
        Text(
            if (fertig) {
                "Lagen entstehen jetzt von allein rund um deine Wachen. Was du verdienst, steht oben im " +
                    "Kopf; mit der Stufe kommen neue Bauarten, Organisationen und Fahrzeuge dazu."
            } else {
                lektion!!.text
            },
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = Abstand.Normal),
        ) {
            if (fertig) {
                Knopf("Alles klar", { beendet = true; sichern() }, art = Knopfart.Haupt, kompakt = true)
            } else {
                Knopf("Diesen Schritt überspringen", {
                    geschafft++
                    sichern()
                }, kompakt = true, art = Knopfart.Leise)
                Text(
                    "Nicht mehr zeigen",
                    style = Schrift.Klein,
                    color = Farben.TextSehrLeise,
                    modifier = Modifier
                        .clickable { beendet = true; sichern() }
                        .padding(vertical = Abstand.Normal, horizontal = Abstand.Winzig),
                )
            }
        }
    }
    return lektion?.ziel
}
