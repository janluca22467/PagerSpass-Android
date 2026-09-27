package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Gluecksradergebnis
import de.pagerspass.pagerspass.netz.Tagesbonus
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import kotlinx.coroutines.launch

/**
 * Das tägliche Glücksrad — übertragen aus dem Bonusteil von `ShopView.vue`.
 *
 * <b>Die sichtbaren Feldbreiten sind genau die Chancen</b>, die der Server
 * liefert (`gewicht`) — ein Rad, dessen Felder anders aussehen als sie treffen,
 * wäre eine Lüge in Farbe. Die Legende daneben nennt dieselben Zahlen in Prozent.
 *
 * <b>Der Gewinn steht fest, bevor das Rad sich dreht.</b> Der Server entscheidet;
 * das Rad läuft vier Runden und bleibt mit der Mitte des gewonnenen Feldes unter dem
 * Zeiger stehen. Erst dann steht der Gewinn als Satz da.
 */

private val RADFARBEN = listOf(
    Farben.Amber,
    Farben.Blau,
    Farben.Gruen,
    Farben.Violett,
    Farben.Signal,
    Farben.BlauHell,
    Farben.AmberTief,
)

@Composable
fun Gluecksrad(
    tagesbonus: Tagesbonus,
    beiDrehen: suspend () -> Result<Gluecksradergebnis>,
) {
    val bereich = rememberCoroutineScope()
    val drehung = remember { Animatable(0f) }
    var gewinn by remember { mutableStateOf<String?>(null) }
    var dreht by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }

    val felder = tagesbonus.felder
    val gesamt = felder.sumOf { it.gewicht }.takeIf { it > 0 } ?: 1

    fun drehen() {
        if (dreht) return
        dreht = true
        fehler = null
        gewinn = null
        bereich.launch {
            beiDrehen()
                .onSuccess { ergebnis ->
                    val neu = ergebnis.shop.tagesbonus.felder.ifEmpty { felder }
                    val summe = neu.sumOf { it.gewicht }.takeIf { it > 0 } ?: 1
                    var davor = 0
                    for (feld in neu) {
                        if (feld.id == ergebnis.gewinn.id) break
                        davor += feld.gewicht
                    }
                    val mitte = ((davor + ergebnis.gewinn.gewicht / 2f) / summe) * 360f
                    val jetzt = drehung.value
                    val ziel = jetzt + 1440f + ((360f - mitte - (jetzt % 360f) + 360f) % 360f)
                    drehung.animateTo(ziel, tween(durationMillis = 1800, easing = FastOutSlowInEasing))
                    gewinn = ergebnis.gewinn.text
                }
                .onFailure { fehler = it.message ?: "Der Bonus konnte nicht abgeholt werden." }
            dreht = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = if (tagesbonus.verfuegbar) Farben.AmberTief else Farben.Rand)
            .then(if (tagesbonus.verfuegbar) Modifier.flaechenmarke(wartet = true) else Modifier)
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Gross),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.size(width = 120.dp, height = 132.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 12.dp).size(120.dp),
                ) {
                    Canvas(modifier = Modifier.size(120.dp).rotate(drehung.value)) {
                        var bisher = 0f
                        if (felder.isEmpty()) {
                            drawCircle(Farben.FlaecheAktiv)
                        }
                        felder.forEachIndexed { i, feld ->
                            val bogen = feld.gewicht.toFloat() / gesamt * 360f
                            // Wie `conic-gradient` im Web: Null steht oben, gezählt im Uhrzeigersinn.
                            drawArc(
                                color = RADFARBEN[i % RADFARBEN.size],
                                startAngle = -90f + bisher,
                                sweepAngle = bogen,
                                useCenter = true,
                            )
                            bisher += bogen
                        }
                        drawCircle(Farben.BgTief, style = Stroke(width = 3.dp.toPx()))
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Farben.BgTief, CircleShape)
                            .border(2.dp, Farben.Amber, CircleShape),
                    ) {
                        Text("PS", style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
                    }
                }
                Text("▼", style = Schrift.Gross, color = Farben.Text)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.weight(1f),
            ) {
                Etikett("Tägliches Glücksrad")
                Text(
                    text = when {
                        gewinn != null -> "Gewonnen: $gewinn"
                        tagesbonus.verfuegbar -> "Dein Dreh ist bereit"
                        else -> "Für heute gedreht"
                    },
                    style = Schrift.Gross,
                    color = if (gewinn != null) Farben.AmberHell else Farben.Text,
                )
                Text(
                    "Serie: ${tagesbonus.serie} ${if (tagesbonus.serie == 1) "Tag" else "Tage"}",
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                )
                SehrLeise("Credits, Fahrzeuggutscheine und als Wachmitglied auch Gemeinschafts-Coins.")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            felder.forEachIndexed { i, feld ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).background(RADFARBEN[i % RADFARBEN.size], CircleShape))
                    Text(feld.text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                    Text("${feld.gewicht} %", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                }
            }
        }

        Knopf(
            aufschrift = when {
                dreht -> "Dreht …"
                tagesbonus.verfuegbar -> "Jetzt drehen"
                else -> "Gedreht ✓"
            },
            beiDruck = { drehen() },
            art = Knopfart.Haupt,
            aktiv = tagesbonus.verfuegbar && !dreht,
            breit = true,
        )
        fehler?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
    }
}

/** Der Ton je Auftragsstufe — Grün, Amber, Rot: „so schwer wird es". */
internal fun stufenton(stufe: String): Color = when (stufe) {
    "Leicht" -> Color(0xFF46C8A0)
    "Mittel" -> Farben.Amber
    "Schwer" -> Color(0xFFFF7890)
    else -> Farben.TextLeise
}
