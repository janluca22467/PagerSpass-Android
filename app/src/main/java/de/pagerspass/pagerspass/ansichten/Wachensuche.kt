package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Bausteine der Wachensuche (`GemeinschaftenView.vue`, Web 5.0.0.26).
 *
 * <b>Die Treffer als Karten statt als Zeilen</b>: jede Wache in ihrem Ton, mit
 * einem Balken für „ist da noch Platz?" und den Aufnahmeregeln als Marken. In der
 * Zeilenliste standen Belegung, Kreis, Aufnahme und Level als eine graue
 * Nebenzeile — genau die vier Dinge, nach denen man entscheidet, las man zuletzt.
 */

/** Ab welchem Level man selbst gründen darf — Spiegel von `GRUENDEN_AB_LEVEL` im Web. */
internal const val GRUENDEN_AB_LEVEL = 3

/** Wie ein Aufnahmemodus heißt, wenn ein Mensch ihn liest. */
private fun aufnahme(modus: String): String = when (modus) {
    "Einladung" -> "Nur auf Einladung"
    "Antrag" -> "Auf Antrag"
    else -> "Offen für alle"
}

/** Ein Schild mit Initialen im Ton der Wache — für Zeilen, die nur Id und Namen kennen. */
@Composable
private fun Schild(id: String, name: String, farbe: Int = 0) {
    val ton = Wappen.ton(id, farbe)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp).background(ton, Rundung.Klein),
    ) {
        Text(Wappen.initialen(name, "W"), style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Wappen.schrift(ton))
    }
}

/** Eine Einladung oder eigene Bewerbung als Zeile: Schild, Name, Nebenzeile, Knöpfe. */
@Composable
fun Antragszeile(
    gemeinschaftId: String,
    name: String,
    unter: String?,
    letzte: Boolean,
    knoepfe: @Composable RowScope.() -> Unit,
) {
    Kartenzeile(letzte = letzte) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Schild(gemeinschaftId, name)
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = Schrift.Normal, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                unter?.let { SehrLeise(it) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), content = knoepfe)
    }
}

/**
 * Eine öffentliche Wache als Karte — Band im Ton, Schild, Name, Belegung als
 * Balken, Aufnahme und Mindestlevel als Marken, am Fuß der Weg hinein.
 *
 * Wer keinen Platz hat oder nur auf Einladung aufnimmt, bekommt einen Satz statt
 * eines Knopfs, der etwas verspricht, das nicht geht.
 */
@Composable
fun Wachenkarte(
    g: Gemeinschaft,
    level: Int,
    laeuft: Boolean,
    bewerbungOffen: Boolean,
    beiBewerbungOeffnen: (Boolean) -> Unit,
    griffe: WachenGriffe,
) {
    val ton = Wappen.ton(g.id, g.wappenfarbe)
    val voll = g.maxMitglieder > 0 && g.mitglieder >= g.maxMitglieder
    val form = RoundedCornerShape(14.dp)
    var nachricht by remember(g.id) { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .clip(form)
            .background(Farben.Flaeche, form)
            // Das Band: derselbe Farbverlauf wie im Wachenkopf, nur leiser.
            .background(Brush.verticalGradient(0f to ton.copy(alpha = 0.16f), 0.45f to Color.Transparent), form)
            .border(1.dp, Farben.Rand, form)
            .padding(Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wachenemblem(g, groesse = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                g.beiname.ifBlank { null }?.let {
                    Text(it, style = Schrift.Winzig, color = Farben.AmberHell, maxLines = 1)
                }
                Text(
                    g.name,
                    style = Schrift.Gross.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SehrLeise(g.landkreis?.ifBlank { null } ?: "Ohne festen Landkreis")
            }
        }
        g.beschreibung?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = Schrift.Klein, color = Farben.TextLeise, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${g.mitglieder}", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            SehrLeise(" / ${g.maxMitglieder} Mitglieder", modifier = Modifier.weight(1f))
            if (voll) Text("voll", style = Schrift.MonoKlein, color = Farben.SignalHell)
        }
        Fortschritt(
            anteil = if (g.maxMitglieder <= 0) 0f
            else (g.mitglieder.toFloat() / g.maxMitglieder).coerceIn(0.03f, 1f),
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Marke(
                aufnahme(g.beitrittModus),
                farbe = when (g.beitrittModus) {
                    "Offen" -> Farben.GruenHell
                    "Einladung" -> Farben.TextLeise
                    else -> Farben.AmberHell
                },
            )
            if (g.mindestLevel > 1) {
                Marke(
                    "ab Level ${g.mindestLevel}",
                    farbe = if (level >= g.mindestLevel) Farben.GruenHell else Farben.SignalHell,
                )
            }
        }

        when {
            voll -> SehrLeise("Gerade kein Platz frei — schau später wieder vorbei.")
            g.beitrittModus == "Einladung" -> SehrLeise("Nimmt nur auf Einladung auf — frag jemanden aus der Mannschaft.")
            g.beitrittModus == "Antrag" && bewerbungOffen -> {
                Feld(
                    wert = nachricht,
                    beiAenderung = { nachricht = it.take(300) },
                    platzhalter = "Kurz zu dir (freiwillig)",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Bewerbung senden", {
                        griffe.bewerben(g.id, nachricht.trim().ifBlank { null })
                        beiBewerbungOeffnen(false)
                    }, art = Knopfart.Haupt, aktiv = !laeuft, modifier = Modifier.weight(1f))
                    Knopf("Abbrechen", { beiBewerbungOeffnen(false) }, art = Knopfart.Leise)
                }
            }
            g.beitrittModus == "Antrag" -> Knopf("Bewerben …", { beiBewerbungOeffnen(true) }, aktiv = !laeuft, breit = true)
            else -> Knopf("Beitreten", { griffe.bewerben(g.id, null) }, art = Knopfart.Haupt, aktiv = !laeuft, breit = true)
        }
    }
}
