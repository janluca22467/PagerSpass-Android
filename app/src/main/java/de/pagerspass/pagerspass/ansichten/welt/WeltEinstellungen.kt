package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.WeltEinstellung
import de.pagerspass.pagerspass.netz.WeltIconpack
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.karte.Kartenstil
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Die vier Gangarten — mit dem, was sie bedeuten. */
private val GANGARTEN = listOf(
    Triple("Ruhig", "Ruhig", "Wenig gleichzeitig, lange vor Ort. Zeit zum Zusehen."),
    Triple("Normal", "Normal", "So, wie die Welt bemessen ist."),
    Triple("Fordernd", "Fordernd", "Viel gleichzeitig, kurze Arbeit. Wer disponieren will."),
    Triple("Eigen", "Eigen", "Die drei Schrauben von Hand."),
)

/**
 * Die Einstellungen der Welt — übertragen aus `components/welt/EinstellungBlende.vue`:
 * Gangart (bei „Eigen" drei Regler), Kartenansicht, Fahrzeug-Icons und Ebenen.
 *
 * <b>Der Tonregler fehlt:</b> Die App hat für den Funk keinen eigenen Lautstärkeregler —
 * es gilt die Medienlautstärke des Geräts.
 */
@Composable
fun WeltEinstellungBlende(
    welt: Welt,
    stand: Weltzustand,
    griffe: Weltgriffe,
    ebenen: Map<String, Boolean>,
    beiEbene: (String, Boolean) -> Unit,
) {
    val bereich = rememberCoroutineScope()
    val zusammenhang = LocalContext.current
    val ablage = remember { Ablage(zusammenhang) }

    var einstellung by remember { mutableStateOf<WeltEinstellung?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var gangart by remember { mutableStateOf("Normal") }
    var dichte by remember { mutableIntStateOf(100) }
    var nachschub by remember { mutableIntStateOf(100) }
    var arbeitszeit by remember { mutableIntStateOf(100) }
    var packs by remember { mutableStateOf<List<WeltIconpack>>(emptyList()) }
    var aktivesPack by remember { mutableStateOf<String?>(null) }

    suspend fun laden() {
        welt.holen("Die Einstellungen ließen sich nicht laden.") { welt.wege.einstellung(it) }
            .onSuccess {
                einstellung = it
                gangart = it.gangart
                dichte = it.dichte
                nachschub = it.nachschub
                arbeitszeit = it.arbeitszeit
                fehler = null
            }
            .onFailure { fehler = it.message }
    }

    LaunchedEffect(Unit) {
        laden()
        runCatching { welt.wege.iconpacks() }.onSuccess { liste ->
            packs = liste
            aktivesPack = liste.firstOrNull { it.aktiv }?.id
        }
    }

    fun speichern(neu: String) {
        if (sendet) return
        sendet = true
        fehler = null
        val d = dichte
        val n = nachschub
        val a = arbeitszeit
        bereich.launch {
            fehler = welt.aktion("Das Speichern ging nicht.", Welt.Nachladen.Nichts) {
                welt.wege.einstellungSetzen(it, neu, d, n, a)
            }
            laden()
            sendet = false
        }
    }

    fun packWaehlen(id: String?) {
        if (sendet) return
        sendet = true
        fehler = null
        bereich.launch {
            try {
                welt.wege.iconpackWaehlen(id)
                aktivesPack = id
                welt.packLaden()
            } catch (f: Exception) {
                fehler = f.message ?: "Das Icon-Pack ließ sich nicht setzen."
            }
            sendet = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        Absage(fehler)

        // ------------------------------------------------------- Gangart
        Ueberschrift("Gangart")
        Leise("Wie viel gleichzeitig los ist — am Stundenverdienst ändert es nichts.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            GANGARTEN.forEach { (id, name, _) ->
                Pille(
                    aufschrift = name,
                    an = gangart == id,
                    beiDruck = {
                        gangart = id
                        // Bei „Eigen" entscheiden erst die Regler — gespeichert wird mit ihnen.
                        speichern(id)
                    },
                    aktiv = !sendet,
                )
            }
        }
        Leise(GANGARTEN.firstOrNull { it.first == gangart }?.third.orEmpty())

        // Die Regler nur bei „Eigen": Bei einer groben Wahl entscheidet der Server.
        if (gangart == "Eigen") {
            val kleinstes = einstellung?.kleinstes ?: 50
            val groesstes = einstellung?.groesstes ?: 200
            Regler("Einsatzdichte", "Wie viele Lagen in deinem Bereich gleichzeitig offenstehen.", dichte, kleinstes, groesstes, { dichte = it }) { speichern("Eigen") }
            Regler(
                "Zeit zwischen Alarmierungen",
                "Wie schnell nachkommt, wenn du abgearbeitet hast. Niedrig heißt: einzeln statt in Schüben.",
                nachschub, kleinstes, groesstes, { nachschub = it },
            ) { speichern("Eigen") }
            Regler("Arbeitszeit vor Ort", "Wie lange deine Fahrzeuge an der Einsatzstelle gebunden sind.", arbeitszeit, kleinstes, groesstes, { arbeitszeit = it }) { speichern("Eigen") }
        }

        // Die Gegenrechnung — direkt unter den Schrauben, die sie auslösen.
        val lohn = einstellung?.lohnProzent ?: 100
        if (lohn < 100) {
            Text(
                text = "Jede Lage bringt $lohn % der Vergütung — mehr zu tun heißt je Lage weniger.",
                style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                color = Farben.Amber,
            )
        }

        // ------------------------------------------------------- Karte
        Ueberschrift("Karte")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Kartenstil.entries.forEach { s ->
                Pille(
                    aufschrift = s.titel,
                    an = griffe.karte.stil == s,
                    beiDruck = {
                        griffe.karte.stil = s
                        bereich.launch { ablage.karteStilSetzen(s.name) }
                    },
                )
            }
        }

        // ------------------------------------------------------- Icons
        Ueberschrift("Fahrzeug-Icons")
        Leise("Nur du siehst sie — auf deiner Karte gelten sie für alle Fahrzeuge.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Pille(aufschrift = "Standard", an = aktivesPack == null, beiDruck = { packWaehlen(null) }, aktiv = !sendet)
            packs.forEach { p ->
                Pille(
                    aufschrift = p.name,
                    an = aktivesPack == p.id,
                    beiDruck = { packWaehlen(p.id) },
                    zahl = p.belegt,
                    aktiv = !sendet && !p.gesperrt && p.belegt > 0,
                )
            }
        }
        Leise(
            if (packs.isEmpty()) "Du hast noch kein Pack. Du musst nicht alle Fahrzeuge malen — ein einziges reicht."
            else "Ein Pack ersetzt nur, was es hinterlegt — der Rest bleibt gezeichnet.",
        )
        Knopf(if (packs.isEmpty()) "Pack anlegen" else "Icons bearbeiten", griffe.icons, kompakt = true)

        // ------------------------------------------------------- Ebenen
        Ueberschrift("Ebenen")
        Leise("Was auf der Karte gezeichnet wird — dieselben Schalter wie am Ebenenknopf.")
        WELTEBENEN.forEach { (id, name) ->
            Hakenzeile(text = name, an = ebenen[id] ?: true, beiWechsel = { beiEbene(id, it) })
        }
    }
}

@Composable
private fun Leise(text: String) {
    Text(text = text, style = Schrift.Winzig, color = Farben.TextSehrLeise)
}

/** Ein Regler in Schritten von fünf Prozent — gespeichert wird beim Loslassen. */
@Composable
private fun Regler(
    name: String,
    was: String,
    wert: Int,
    von: Int,
    bis: Int,
    beiWert: (Int) -> Unit,
    beiFertig: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
            Text(text = "$wert %", style = Schrift.MonoKlein, color = Farben.TextLeise)
        }
        val schritte = ((bis - von) / 5 - 1).coerceAtLeast(0)
        Slider(
            value = wert.toFloat().coerceIn(von.toFloat(), bis.toFloat()),
            onValueChange = { beiWert(((it / 5f).roundToInt() * 5).coerceIn(von, bis)) },
            valueRange = von.toFloat()..bis.toFloat(),
            steps = schritte,
            onValueChangeFinished = beiFertig,
            colors = SliderDefaults.colors(
                thumbColor = Farben.Amber,
                activeTrackColor = Farben.Amber,
                inactiveTrackColor = Farben.Rand,
            ),
        )
        Leise(was)
    }
}
