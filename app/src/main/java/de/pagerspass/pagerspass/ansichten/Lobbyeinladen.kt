package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.melder.Melderspieler
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay

/**
 * Zwei Handgriffe der Lobby aus `LobbyView.vue` (5.0.0.26): Freunde in die Runde
 * holen — als Mitspieler oder als Zuschauer — und den eigenen Melder einmal hören.
 */

/**
 * Wer soll dazukommen, und welcher Freund. Die Vorauswahl richtet sich nach dem
 * Bestand: Ist kein Mitspielerplatz mehr frei, steht sie gleich auf „Zuschauer" —
 * sonst führte der erste Druck auf einen Freund zu einer Absage, die schon feststand.
 */
@Composable
fun ColumnScope.Freundeeinladen(
    raum: Raumzustand,
    freunde: List<Freund>,
    beiEinladen: (String, Boolean, (String?) -> Unit) -> Unit,
) {
    val menschen = raum.players.count { !it.istBot }
    val platzFrei = (raum.maxSpieler - menschen).coerceAtLeast(0)
    var alsZuschauer by remember { mutableStateOf(platzFrei == 0) }
    var eingeladen by remember { mutableStateOf(emptySet<String>()) }
    var meldung by remember { mutableStateOf<String?>(null) }

    Etikett("Wer soll dazukommen?")
    Pillenreihe {
        Pille("Mitspieler · $platzFrei frei", an = !alsZuschauer, aktiv = platzFrei > 0, beiDruck = { alsZuschauer = false })
        Pille("Zuschauer · ohne Platz", an = alsZuschauer, beiDruck = { alsZuschauer = true })
    }
    SehrLeise(
        when {
            alsZuschauer -> "Ein Zuschauerplatz belegt keinen Mitspielerplatz — er geht auch, wenn die Runde schon voll ist."
            platzFrei == 0 -> "Alle Mitspielerplätze sind belegt. Die Leitstelle kann sie in den Rundeneinstellungen " +
                "heben — oder du lädst als Zuschauer ein."
            else -> "Wer angenommen hat, sucht sich selbst eine Rolle: Leitstellenplatz oder Fahrzeug."
        },
    )

    val bestaetigte = freunde.filter { it.bestaetigt }
    if (bestaetigte.isEmpty()) {
        Leise("Noch keine Freunde. Wer einen Benutzernamen hat, findet dich — unter Freunde.")
    }
    bestaetigte.forEach { f ->
        val dabei = f.anwesenheit?.roomCode == raum.code
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(f.anzeigename, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                SehrLeise(if (f.anwesenheit != null) "schon in einer Runde" else "nicht im Dienst", mono = true)
            }
            Knopf(
                when {
                    dabei -> "Ist dabei"
                    f.kennung in eingeladen -> "Eingeladen"
                    else -> "Einladen"
                },
                {
                    meldung = null
                    beiEinladen(f.kennung, alsZuschauer) { fehler ->
                        if (fehler == null) eingeladen = eingeladen + f.kennung else meldung = fehler
                    }
                },
                kompakt = true,
                aktiv = !dabei && f.kennung !in eingeladen,
            )
        }
    }
    meldung?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
}

/**
 * „Melder testen" — jeder hört einmal, wonach er gleich suchen muss. Der eigene Ton,
 * nicht der Standardton; ein zweiter Druck hält die Probe an. Gestoppt wird nur die
 * eigene Probe, nie ein Alarm, der inzwischen eingegangen ist.
 */
@Composable
fun Meldertest() {
    val zusammenhang = LocalContext.current
    var laeuft by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { Melderspieler.stoppen("probe") } }
    LaunchedEffect(laeuft) {
        if (!laeuft) return@LaunchedEffect
        delay(6_000)
        laeuft = false
    }
    Knopf(
        if (laeuft) "Melder läuft …" else "Melder testen",
        {
            if (laeuft) {
                Melderspieler.stoppen("probe")
                Melderspieler.vibrationAus()
                laeuft = false
            } else {
                val geraet = Meldergeraet.bereit(zusammenhang)
                Melderspieler.probe(zusammenhang, geraet.ton)
                laeuft = true
            }
        },
        art = if (laeuft) Knopfart.Haupt else Knopfart.Leise,
        kompakt = true,
    )
}
