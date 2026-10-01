package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.netz.Einzelruf
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Uebergabe
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Erhebung
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay

/**
 * Was zum Raum gehört und nicht zu einer Ansicht — übertragen aus `RaumView.vue`:
 * Übergabe-Dialog, Warnband und Einzelruf-Leiste.
 *
 * <b>Warum über allen Ansichten.</b> Das Übergabeangebot muss auch den erreichen,
 * der gerade am Fahrzeug arbeitet; die Bevölkerungswarnung gilt ausdrücklich auch
 * der Besatzung; und ein Einzelruf klingelt, egal auf welchem Reiter man steht.
 */
@Composable
fun BoxScope.Raumueberlagerungen(
    raum: Raumzustand?,
    neben: Raumneben,
    eigeneKennung: String,
    befehle: Raumbefehle,
) {
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Klingeln und Warnton — sie hängen am selben Zustand wie Leiste und Band.
    de.pagerspass.pagerspass.mobil.Raumtoene(raum, neben.warnungUm, eigeneKennung)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = oben + 64.dp)
            .padding(horizontal = Abstand.Gross),
    ) {
        neben.warnung?.let { text ->
            Warnband(text, neben.warnungUm, befehle::warnungWegnehmen)
        }
        if (raum != null) {
            Einzelrufleiste(raum, neben, eigeneKennung, befehle)
        }
    }

    raum?.uebergabe?.takeIf { it.zielPlayerId == eigeneKennung }?.let { angebot ->
        Uebergabeblende(angebot, befehle::uebergabeAntworten)
    }
}

/**
 * Das Warn-App-Band. Es verschwindet nach einer Minute von selbst — eine
 * Warnung, die für immer stehen bleibt, liest nach dem dritten Blick niemand mehr.
 */
@Composable
private fun Warnband(text: String, um: Long, beiWeg: () -> Unit) {
    LaunchedEffect(um) {
        delay(60_000)
        beiWeg()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .shadow(Erhebung.Alarm, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .flaeche(farbe = Farben.FlaecheHoch, randfarbe = Farben.SignalHell, ecke = 12.dp)
            .padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "⚠ Warnung der Leitstelle",
                style = Schrift.Gross,
                color = Farben.SignalHell,
                modifier = Modifier.weight(1f),
            )
            Knopf("OK", beiWeg, art = Knopfart.Leise, kompakt = true)
        }
        Text(text = text, style = Schrift.Normal, color = Farben.Text)
    }
}

/**
 * Die Einzelruf-Leiste — genau eine Lage zur Zeit, in der Rangfolge des Geräts: Ein
 * laufendes Gespräch verdeckt jedes Klingeln, ein Klingeln den eigenen Rufversuch.
 */
@Composable
private fun Einzelrufleiste(
    raum: Raumzustand,
    neben: Raumneben,
    ich: String,
    befehle: Raumbefehle,
) {
    val ichLeitstelle = raum.players.firstOrNull { it.id == ich }?.istLeitstelle == true

    val laufend = raum.einzelrufe.firstOrNull {
        it.laeuft && (it.vonPlayerId == ich || it.angenommenVonPlayerId == ich)
    }
    val eingehend = raum.einzelrufe.firstOrNull {
        it.klingelt && (it.zielPlayerId == ich || (it.zielPlayerId == null && ichLeitstelle))
    }
    val ausgehend = raum.einzelrufe.firstOrNull { it.klingelt && it.vonPlayerId == ich }

    when {
        laufend != null -> LaufenderEinzelruf(laufend, neben, ich, befehle, raum.vehicles)

        eingehend != null -> Rufzeile(
            titel = funkanzeige(eingehend.vonName, raum.vehicles),
            unter = "Einzelruf",
            punkt = Farben.GruenHell,
        ) {
            Knopf("Annehmen", { befehle.einzelrufAnnehmen(eingehend.id) }, art = Knopfart.Haupt, kompakt = true)
            Knopf("Abweisen", { befehle.einzelrufAbweisen(eingehend.id) }, art = Knopfart.Gefahr, kompakt = true)
        }

        ausgehend != null -> Rufzeile(
            titel = funkanzeige(ausgehend.zielName, raum.vehicles),
            unter = "wird gerufen …",
            punkt = Farben.AmberHell,
        ) {
            Knopf("Abbrechen", { befehle.einzelrufBeenden(ausgehend.id) }, art = Knopfart.Gefahr, kompakt = true)
        }
    }
}

@Composable
private fun Rufzeile(
    titel: String,
    unter: String,
    punkt: androidx.compose.ui.graphics.Color,
    knoepfe: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .shadow(Erhebung.Alarm, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .flaeche(farbe = Farben.FlaecheHoch, randfarbe = Farben.RandHell, ecke = 12.dp)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Box(Modifier.size(10.dp).background(punkt, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(titel, style = Schrift.Normal, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            SehrLeise(unter)
        }
        knoepfe()
    }
}

/**
 * Das laufende Gespräch.
 *
 * Zwischen zwei Menschen trägt die Leitung Stimme — das Mikrofon ist offen, solange
 * das Gespräch läuft. Mit einer Bot-Besatzung wird getippt oder mit der rastenden
 * Taste gesprochen; der Server schreibt mit.
 */
@Composable
private fun LaufenderEinzelruf(
    ruf: Einzelruf,
    neben: Raumneben,
    ich: String,
    befehle: Raumbefehle,
    fahrzeuge: List<de.pagerspass.pagerspass.netz.Rundenfahrzeug> = emptyList(),
) {
    // Namen in der eingestellten Fahrzeugkennung (siehe `funkanzeige`, v6).
    val gegenstelle = funkanzeige(if (ruf.vonPlayerId == ich) ruf.zielName else ruf.vonName, fahrzeuge)
    var satz by remember(ruf.id) { mutableStateOf("") }

    // Die Gesprächsdauer — eine eigene Uhr, der Server schickt keine Sekunden.
    var sekunden by remember(ruf.id) { mutableLongStateOf(0L) }
    LaunchedEffect(ruf.id) {
        while (true) {
            delay(1_000)
            sekunden++
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .shadow(Erhebung.Alarm, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .flaeche(farbe = Farben.FlaecheHoch, randfarbe = Farben.GruenHell, ecke = 12.dp)
            .padding(Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).background(Farben.GruenHell, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(gegenstelle, style = Schrift.Normal, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                SehrLeise(
                    "Einzelruf · ${sekunden / 60}:${(sekunden % 60).toString().padStart(2, '0')}" +
                        if (!ruf.mitBot) " · Mikrofon offen" else "",
                    mono = true,
                )
            }
            Knopf("Auflegen", { befehle.einzelrufBeenden(ruf.id) }, art = Knopfart.Gefahr, kompakt = true)
        }

        if (ruf.mitBot) {
            val zeilen = ruf.verlauf.orEmpty().takeLast(6)
            if (zeilen.isEmpty()) {
                SehrLeise("Verbunden. Sprich oder schreib — die Besatzung hört mit.")
            } else {
                zeilen.forEach { z ->
                    Text(
                        text = "${funkanzeige(z.vonName, fahrzeuge)}: ${funkanzeige(z.text, fahrzeuge)}",
                        style = Schrift.Klein,
                        color = if (z.vonPlayerId == ich) Farben.AmberHell else Farben.Text,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Die Taste rastet: ein Druck öffnet das Mikrofon für eine Äußerung,
                // der zweite schließt es — die Grenze der Äußerung braucht die Erkennung.
                val hoert = neben.sendetAuf == "einzelrufBot"
                Knopf(
                    aufschrift = when {
                        hoert -> "Hört zu …"
                        neben.einzelrufVersteht -> "…"
                        else -> "Sprechen"
                    },
                    beiDruck = {
                        if (hoert) befehle.einzelrufSprechenBeenden(ruf.id)
                        else befehle.einzelrufSprechenStarten()
                    },
                    art = if (hoert) Knopfart.Alarm else Knopfart.Normal,
                    kompakt = true,
                )
                Feld(
                    wert = satz,
                    beiAenderung = { satz = it.take(500) },
                    platzhalter = "Sagen …",
                    weiterTaste = ImeAction.Send,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Senden",
                    {
                        befehle.einzelrufSagen(ruf.id, satz)
                        satz = ""
                    },
                    aktiv = satz.isNotBlank(),
                    kompakt = true,
                )
            }
        }
    }
}

/**
 * Die Dienstübergabe aus Sicht des Übernehmenden: das Protokoll, dann die
 * Entscheidung. Kein Wegtippen — die Frage verlangt eine Antwort.
 */
@Composable
private fun Uebergabeblende(angebot: Uebergabe, beiAntwort: (Boolean) -> Unit) {
    Blende(
        titel = "${angebot.vonName} übergibt dir die Leitstelle",
        beiSchliessen = {},
        schliessenMoeglich = false,
        fuss = {
            Knopf("Ablehnen", { beiAntwort(false) }, art = Knopfart.Leise)
            Knopf("Leitstelle übernehmen", { beiAntwort(true) }, art = Knopfart.Haupt)
        },
    ) {
        if (angebot.offeneEinsaetze.isEmpty()) {
            Leerhinweis("Keine offenen Einsätze — ruhige Lage.")
        } else {
            Ueberschrift("Offene Einsätze")
            angebot.offeneEinsaetze.forEach { e ->
                Text(
                    text = "${e.einsatznummer} ${e.stichwort} · seit ${e.alterMinuten} min" +
                        e.hinweise.joinToString("") { " · $it" },
                    style = Schrift.Klein,
                    color = Farben.Text,
                )
            }
        }

        if (angebot.fahrzeuge.isNotEmpty()) {
            Ueberschrift("Fahrzeuge")
            angebot.fahrzeuge.forEach { f ->
                Text("${f.funkrufname} — ${f.hinweis}", style = Schrift.Klein, color = Farben.Text)
            }
        }

        if (angebot.klingelndeAnrufe > 0) {
            Text(
                text = "${angebot.klingelndeAnrufe} " +
                    (if (angebot.klingelndeAnrufe == 1) "Anruf klingelt" else "Anrufe klingeln") + " gerade.",
                style = Schrift.Klein,
                color = Farben.SignalHell,
            )
        }
        SehrLeise("Wetterlage: ${angebot.wetter}")
        SehrLeise(
            "Dein Fahrzeug fährt mit Bot-Besatzung nahtlos weiter — am Fahrzeug ändert " +
                "sich nichts, nur wer drinsitzt.",
        )
    }
}
