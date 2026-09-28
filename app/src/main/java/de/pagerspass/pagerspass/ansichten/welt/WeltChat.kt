package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.ansichten.Sprechtaste
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.mobil.weltzeit
import de.pagerspass.pagerspass.netz.WeltChatzeile
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun uhrzeit(iso: String): String {
    val ms = weltzeit(iso) ?: return ""
    return SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(ms))
}

private fun absender(z: WeltChatzeile): String = if (z.eigen) "Du" else z.von ?: "Ohne Namen"

/**
 * Chat und Funk der Welt — übertragen aus `components/welt/ChatBlende.vue`.
 *
 * Oben der Funkschalter mit der Anzeige, wer spricht, und die Sprechtaste (dieselbe wie
 * im Dienst, `ansichten/Dienstbausteine.kt`). Darunter das Feld und die Nachrichten.
 *
 * <b>Am Handy steht das Neueste oben, gleich unter dem Feld.</b> Die Seite rollt von
 * oben; eine Liste, die unten wächst, hieße nach jeder Nachricht ans Ende rollen — und
 * das Feld stünde dann jenseits der Tastatur.
 */
@Composable
fun WeltChatBlende(welt: Welt, stand: Weltzustand, griffe: Weltgriffe) {
    val bereich = rememberCoroutineScope()
    var entwurf by remember { mutableStateOf("") }
    var anId by remember { mutableStateOf<String?>(null) }
    var anName by remember { mutableStateOf<String?>(null) }

    // Geöffnet hakt die Post ab; geschlossen endet auch eine hängende Durchsage.
    DisposableEffect(Unit) {
        welt.viewModelScope.launch { welt.chatGeoeffnet(true) }
        onDispose {
            welt.viewModelScope.launch { welt.chatGeoeffnet(false) }
            welt.sprechenBeenden()
        }
    }

    fun senden() {
        val text = entwurf.trim()
        if (text.isEmpty()) return
        val an = anId
        bereich.launch {
            welt.chatSenden(if (an != null) "Direkt" else "Allgemein", an, text)
            if (welt.zustand.value.chatfehler == null) entwurf = ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        // ------------------------------------------------------- Funk
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Knopf(
                    if (stand.funkgeraet) "Funk an" else "Funk aus",
                    { welt.funkgeraetSetzen(!stand.funkgeraet) },
                    art = if (stand.funkgeraet) Knopfart.Haupt else Knopfart.Normal,
                    kompakt = true,
                )
                val sprecher = stand.spricht
                Text(
                    text = when {
                        !stand.funkgeraet -> "Du hörst nicht mit."
                        sprecher != null -> "${sprecher.name ?: "Jemand"} spricht"
                        else -> "Kanal frei"
                    },
                    style = Schrift.MonoKlein,
                    color = if (sprecher != null && stand.funkgeraet) Farben.Amber else Farben.TextLeise,
                )
            }
            // Sprechen geht auch mit ausgeschaltetem Gerät — der Schalter heißt „hören".
            Sprechtaste(
                sendet = stand.sendet,
                wirdVerstanden = false,
                belegtVon = if (!stand.sendet && stand.funkgeraet) stand.spricht?.let { it.name ?: "Jemand" } else null,
                gesperrtBis = null,
                beiDruck = { welt.sprechenStarten() },
                beiLoslassen = { welt.sprechenBeenden() },
            )
        }

        // ------------------------------------------------------- Schreiben
        Absage(stand.chatfehler)
        if (anId != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "An", style = Schrift.Klein, color = Farben.TextLeise)
                Text(text = anName.orEmpty(), style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                Knopf("an alle", { anId = null; anName = null }, art = Knopfart.Leise, kompakt = true)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Feld(
                wert = entwurf,
                beiAenderung = { entwurf = it.take(stand.chatMaxLaenge) },
                platzhalter = if (anId != null) "Nachricht an $anName" else "An alle in der Welt …",
                modifier = Modifier.weight(1f),
            )
            Knopf("Senden", { senden() }, art = Knopfart.Haupt, kompakt = true, aktiv = entwurf.isNotBlank())
        }

        // ------------------------------------------------------- Nachrichten
        if (stand.chat.isEmpty()) {
            Leerhinweis("Noch nichts gesagt. Schreib die erste Zeile.")
        }
        stand.chat.asReversed().forEach { z ->
            val direkt = z.kanal == "Direkt"
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(
                        farbe = if (z.eigen) Farben.FlaecheHoch else Farben.Flaeche,
                        randfarbe = if (direkt) Farben.Violett else Farben.Rand,
                        ecke = 9.dp,
                        mitLichtkante = false,
                    )
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    // Der Name setzt ein @ in den Entwurf — der Empfänger wird angepingt.
                    val benutzer = z.vonBenutzername
                    Text(
                        text = absender(z),
                        style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                        color = if (z.eigen) Farben.Amber else Farben.BlauHell,
                        modifier = if (!z.eigen && benutzer != null) {
                            Modifier.clickable {
                                val marke = "@$benutzer "
                                if (!entwurf.contains(marke)) entwurf = (marke + entwurf).take(stand.chatMaxLaenge)
                            }
                        } else {
                            Modifier
                        },
                    )
                    if (direkt) {
                        Weltmarke(if (z.eigen) "an ${z.an ?: "Ohne Namen"}" else "nur an dich", Farben.ViolettHell)
                    }
                    Text(
                        text = uhrzeit(z.um),
                        style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG),
                        color = Farben.TextSehrLeise,
                        modifier = Modifier.weight(1f),
                    )
                    if (!z.eigen) {
                        Knopf(
                            "Antworten",
                            {
                                anId = z.vonId
                                anName = z.von ?: "Ohne Namen"
                            },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                }
                Text(text = z.text, style = Schrift.Normal, color = Farben.Text)
            }
        }
    }
}
