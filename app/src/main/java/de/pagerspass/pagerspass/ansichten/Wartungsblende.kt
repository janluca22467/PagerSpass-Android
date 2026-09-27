package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Wartungslage
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Die Wartung — `WartungsSperre.vue` und `WartungsBand.vue`.
 *
 * <b>Die Sperre deckt alles ab</b> und nimmt jeden Druck weg: Während der
 * Wartung antwortet der Server ohnehin nur mit 503, jeder Knopf dahinter wäre
 * eine Fehlermeldung mehr.
 */
@Composable
fun Wartungssperre(lage: Wartungslage) {
    val stand = lage.stand ?: return
    val aussparung = WindowInsets.safeDrawing.asPaddingValues()
    val gesamt = remember(stand.beginnUm, stand.endeUm) { dauerSekunden(stand.beginnUm, stand.endeUm) }
    val anteil = if (gesamt != null && gesamt > 0) 1f - lage.verbleibend.toFloat() / gesamt else null

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Farben.Bg)
            // Jeder Druck endet hier.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(aussparung)
            .padding(Abstand.Gross),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Text("WARTUNGSARBEITEN", style = Schrift.Etikett, color = Farben.Amber)
            Text(
                stand.titel?.takeIf { it.isNotBlank() } ?: "Wir sind gleich wieder da",
                style = Schrift.Titel,
                color = Farben.Text,
                textAlign = TextAlign.Center,
            )
            (stand.begruendung ?: stand.text)?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = Schrift.Normal, color = Farben.TextLeise, textAlign = TextAlign.Center)
            }
            if (anteil != null) Fortschritt(anteil = anteil, modifier = Modifier.fillMaxWidth())
            Text(restText(lage.verbleibend), style = Schrift.MonoNormal, color = Farben.AmberHell)
            Text(
                "Laufende Schichten sind gespeichert. Sobald wir fertig sind, geht es hier " +
                    "von selbst weiter — du musst nichts tun.",
                style = Schrift.Klein,
                color = Farben.TextLeise,
                textAlign = TextAlign.Center,
            )
            Text(
                "Im echten Notfall: 112.",
                style = Schrift.Klein,
                color = Farben.TextSehrLeise,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Das Ankündigungsband oben — ab zwanzig Minuten vor Beginn. In den letzten
 * fünf Minuten sagt es, worauf es ankommt.
 */
@Composable
fun Wartungsband(lage: Wartungslage, modifier: Modifier = Modifier) {
    val geplant = lage.ankuendigung ?: return
    if (lage.geschlossen) return
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val m = lage.bisBeginn / 60
    val s = lage.bisBeginn % 60

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = oben + Abstand.Klein)
            .padding(horizontal = Abstand.Normal)
            .flaeche(
                farbe = if (lage.meldet || lage.knapp) Farben.AmberTief else Farben.FlaecheHoch,
                randfarbe = Farben.Amber,
                ecke = 10.dp,
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Text(
            "Geplante Wartung in %d:%02d".format(m, s),
            style = Schrift.MonoKlein,
            color = Farben.Text,
        )
        val zeile = listOfNotNull(
            geplant.titel.takeIf { it.isNotBlank() },
            geplant.begruendung.takeIf { it.isNotBlank() },
            geplant.dauerMinuten.takeIf { it > 0 }?.let { "etwa $it Minuten" },
        ).joinToString(" · ")
        if (zeile.isNotBlank()) Text(zeile, style = Schrift.Klein, color = Farben.TextLeise)
        if (lage.knapp) {
            Text(
                "Bring laufende Einsätze zu Ende — deine Schicht wird gespeichert.",
                style = Schrift.Klein,
                color = Farben.AmberHell,
            )
        }
    }
}

private fun restText(sekunden: Int): String {
    val minuten = (sekunden + 59) / 60
    return when {
        sekunden <= 60 -> "Nur noch einen Augenblick"
        minuten < 60 -> "Noch etwa $minuten Minuten"
        else -> "Noch etwa ${minuten / 60} Std. ${minuten % 60} Min."
    }
}

private fun dauerSekunden(beginn: String?, ende: String?): Int? = runCatching {
    Duration.between(OffsetDateTime.parse(beginn), OffsetDateTime.parse(ende)).seconds.toInt()
}.getOrNull()
