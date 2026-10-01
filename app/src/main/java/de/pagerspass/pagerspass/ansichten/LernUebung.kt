package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Lehrgangsmodul
import de.pagerspass.pagerspass.netz.Lernkarte
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Eine Übung im Lehrgang: Lernkarten, eine nach der anderen, mit der Antwort
 * gleich nach dem Tipp — das Gegenstück zu `components/lehrgang/LernUebung.vue`.
 *
 * <b>Lehren, nicht urteilen.</b> Die Wissensprüfung sagt am Ende, was falsch war;
 * die Übung sagt es sofort, mit dem Satz, warum — und legt die Karte hinten
 * wieder auf den Stapel. Durch ist die Übung erst, wenn jede Karte einmal richtig
 * beantwortet wurde. Wer etwas nicht weiß, lernt es hier, bevor die Prüfung
 * danach fragt.
 *
 * Drei Arten Karte: **Auswahl** (eine Antwort antippen), **Reihenfolge** (Schritte
 * mit ↑/↓ sortieren) und **Zuordnung** (links antippen, dann rechts das Passende).
 *
 * <b>Der Stapel lebt in einem `remember` am Modul</b> und nicht im ViewModel: Eine
 * Übung ist in zwei Minuten durch, und wer das Gerät dreht, fängt den Stapel neu
 * gemischt an — genau wie im Web beim Neuladen.
 */
@Composable
fun LernUebung(
    modul: Lehrgangsmodul,
    beiFertig: () -> Unit,
    beiSchliessen: () -> Unit,
) {
    val karten = modul.karten
    var lauf by remember(modul.id) { mutableStateOf(0) }

    // Der Stand einer Runde — bei „Nochmal üben" frisch.
    var stapel by remember(modul.id, lauf) { mutableStateOf(karten.map { it.id }.shuffled()) }
    var gekonnt by remember(modul.id, lauf) { mutableStateOf(emptySet<String>()) }
    var aufAnhieb by remember(modul.id, lauf) { mutableStateOf(emptySet<String>()) }
    var versucht by remember(modul.id, lauf) { mutableStateOf(emptySet<String>()) }
    var phase by remember(modul.id, lauf) { mutableStateOf(if (karten.isEmpty()) Phase.Ende else Phase.Frage) }
    var warRichtig by remember(modul.id, lauf) { mutableStateOf(false) }

    val aktuelle = karten.firstOrNull { it.id == stapel.firstOrNull() }

    // Jede gezogene Karte zählt einen Umlauf weiter. Liegt nach einem Fehler
    // dieselbe Karte wieder oben, ändert sich ihre Id nicht — der Umlauf schon.
    var umlauf by remember(modul.id, lauf) { mutableStateOf(0) }

    // Der Stand der aktuellen Karte — bei jeder gezogenen Karte frisch vorbereitet.
    var gewaehlt by remember(aktuelle?.id, lauf, umlauf) { mutableStateOf<Int?>(null) }
    var reihe by remember(aktuelle?.id, lauf, umlauf) { mutableStateOf(aktuelle?.let(::gemischteReihe).orEmpty()) }
    val rechtsGemischt = remember(aktuelle?.id, lauf, umlauf) { aktuelle?.paare?.map { it.rechts }?.shuffled().orEmpty() }
    var zuordnung by remember(aktuelle?.id, lauf, umlauf) { mutableStateOf(emptyMap<String, String>()) }
    var linksAktiv by remember(aktuelle?.id, lauf, umlauf) { mutableStateOf<String?>(null) }

    val bereit = when (aktuelle?.art) {
        null -> false
        "Auswahl" -> gewaehlt != null
        "Zuordnung" -> aktuelle.paare.all { zuordnung[it.links] != null }
        else -> true
    }

    fun pruefen() {
        val k = aktuelle ?: return
        if (phase != Phase.Frage || !bereit) return
        warRichtig = when (k.art) {
            "Auswahl" -> gewaehlt == k.richtig
            "Reihenfolge" -> reihe == k.schritte
            else -> k.paare.all { zuordnung[it.links] == it.rechts }
        }
        if (k.id !in versucht && warRichtig) aufAnhieb = aufAnhieb + k.id
        versucht = versucht + k.id
        if (warRichtig) gekonnt = gekonnt + k.id
        phase = Phase.Antwort
    }

    fun weiter() {
        val erste = stapel.firstOrNull() ?: return
        // Falsch beantwortet: hinten wieder auf den Stapel. Wer es nicht wusste,
        // kommt noch einmal dran, nachdem er die Erklärung gelesen hat.
        stapel = if (warRichtig) stapel.drop(1) else stapel.drop(1) + erste
        if (stapel.isEmpty()) {
            phase = Phase.Ende
            beiFertig()
            return
        }
        phase = Phase.Frage
        umlauf++
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().padding(top = Abstand.Klein),
    ) {
        Lernbalken(
            anteil = if (karten.isEmpty()) 0f else gekonnt.size.toFloat() / karten.size,
            text = "${gekonnt.size}/${karten.size}",
        )

        val k = aktuelle
        if (k != null && phase != Phase.Ende) {
            Text(k.frage, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)

            when (k.art) {
                "Auswahl" -> Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    k.antworten.forEachIndexed { i, antwort ->
                        Wahlzeile(
                            nr = "${i + 1}",
                            text = antwort,
                            zustand = when {
                                phase == Phase.Antwort && i == k.richtig -> Wahlzustand.Richtig
                                phase == Phase.Antwort && gewaehlt == i -> Wahlzustand.Falsch
                                gewaehlt == i -> Wahlzustand.Gewaehlt
                                else -> Wahlzustand.Offen
                            },
                            aktiv = phase == Phase.Frage,
                            beiDruck = { gewaehlt = i },
                        )
                    }
                }

                "Reihenfolge" -> Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    reihe.forEachIndexed { i, schritt ->
                        val rand = when {
                            phase != Phase.Antwort -> Farben.Rand
                            schritt == k.schritte.getOrNull(i) -> Farben.Gruen
                            else -> Farben.Signal
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(if (phase == Phase.Antwort) 2.dp else 1.dp, rand, Rundung.Klein)
                                .defaultMinSize(minHeight = Ziel.Normal)
                                .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                        ) {
                            Text("${i + 1}", style = Schrift.MonoKlein, color = Farben.TextSehrLeise, modifier = Modifier.width(20.dp))
                            Text(schritt, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                            if (phase == Phase.Frage) {
                                Knopf("↑", { reihe = getauscht(reihe, i, i - 1) }, art = Knopfart.Leise, aktiv = i > 0, kompakt = true)
                                Knopf("↓", { reihe = getauscht(reihe, i, i + 1) }, art = Knopfart.Leise, aktiv = i < reihe.size - 1, kompakt = true)
                            }
                        }
                    }
                }

                else -> Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    if (phase == Phase.Frage) SehrLeise("Links antippen, dann rechts das Passende.")
                    // Am Handy untereinander: erst die linke Spalte, dann die rechte
                    // (im Web unter 560 Punkten genauso).
                    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                        k.paare.forEachIndexed { i, p ->
                            val ziel = zuordnung[p.links]
                            Wahlzeile(
                                nr = "${i + 1}",
                                text = p.links,
                                unter = listOfNotNull(
                                    ziel?.let { "→ $it" },
                                    if (phase == Phase.Antwort && ziel != p.rechts) "richtig: ${p.rechts}" else null,
                                ),
                                zustand = when {
                                    phase == Phase.Antwort && ziel == p.rechts -> Wahlzustand.Richtig
                                    phase == Phase.Antwort -> Wahlzustand.Falsch
                                    linksAktiv == p.links -> Wahlzustand.Gewaehlt
                                    else -> Wahlzustand.Offen
                                },
                                aktiv = phase == Phase.Frage,
                                // Antippen wählt immer aus, nie ab.
                                beiDruck = { linksAktiv = p.links },
                            )
                        }
                    }
                    if (phase == Phase.Frage) {
                        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                            rechtsGemischt.forEach { r ->
                                val vergeben = k.paare.indexOfFirst { zuordnung[it.links] == r }.takeIf { it >= 0 }
                                Wahlzeile(
                                    nr = vergeben?.let { "${it + 1}" } ?: "·",
                                    text = r,
                                    zustand = Wahlzustand.Offen,
                                    aktiv = linksAktiv != null,
                                    gedaempft = vergeben != null,
                                    beiDruck = {
                                        val links = linksAktiv ?: return@Wahlzeile
                                        // Ein Rechts gehört zu genau einem Links — wer umsteckt,
                                        // löst die alte Verbindung.
                                        val neu = zuordnung.filterValues { it != r } + (links to r)
                                        zuordnung = neu
                                        // Weiter zum nächsten offenen Eintrag links.
                                        linksAktiv = k.paare.firstOrNull { neu[it.links] == null }?.links
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (phase == Phase.Antwort) {
                Text(
                    (if (warRichtig) "Richtig. " else "Noch nicht. ") + k.erklaerung +
                        if (!warRichtig) " Die Karte kommt gleich noch einmal." else "",
                    style = Schrift.Klein,
                    color = if (warRichtig) Farben.GruenHell else Farben.SignalHell,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Farben.HauchHell, Rundung.Klein)
                        .padding(Abstand.Klein),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Knopf("Später weiter", beiSchliessen, art = Knopfart.Leise, kompakt = true)
                if (phase == Phase.Frage) {
                    Knopf("Prüfen", { pruefen() }, art = Knopfart.Haupt, aktiv = bereit, kompakt = true)
                } else {
                    Knopf(
                        if (stapel.size == 1 && warRichtig) "Fertig" else "Weiter",
                        { weiter() },
                        art = Knopfart.Haupt,
                        kompakt = true,
                    )
                }
            }
        } else {
            Text(
                "Übung geschafft. ${aufAnhieb.size} von ${karten.size} Karten saßen beim ersten Versuch" +
                    (if (aufAnhieb.size == karten.size) " — alle, stark." else "."),
                style = Schrift.Klein,
                color = Farben.GruenHell,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Knopf("Nochmal üben", { lauf++ }, art = Knopfart.Leise, kompakt = true)
                Knopf("Weiter im Lehrgang", beiSchliessen, art = Knopfart.Haupt, kompakt = true)
            }
        }
    }
}

private enum class Phase { Frage, Antwort, Ende }

internal enum class Wahlzustand { Offen, Gewaehlt, Richtig, Falsch }

/**
 * Eine antippbare Zeile mit Nummer — die Antwort einer Auswahl, ein Eintrag der
 * Zuordnung, eine Antwort der Wissensprüfung. Gewählt trägt sie Amber, nach dem
 * Prüfen Grün oder Rot.
 */
@Composable
internal fun Wahlzeile(
    nr: String,
    text: String,
    zustand: Wahlzustand,
    aktiv: Boolean,
    beiDruck: () -> Unit,
    unter: List<String> = emptyList(),
    gedaempft: Boolean = false,
) {
    val rand: Color = when (zustand) {
        Wahlzustand.Offen -> Farben.Rand
        Wahlzustand.Gewaehlt -> Farben.Amber
        Wahlzustand.Richtig -> Farben.Gruen
        Wahlzustand.Falsch -> Farben.Signal
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (gedaempft) 0.75f else 1f)
            .border(if (zustand == Wahlzustand.Offen) 1.dp else 2.dp, rand, Rundung.Klein)
            .clickable(enabled = aktiv, role = Role.Button, onClick = beiDruck)
            .defaultMinSize(minHeight = Ziel.Normal)
            .padding(Abstand.Klein),
    ) {
        Text(nr, style = Schrift.MonoKlein, color = Farben.TextSehrLeise, modifier = Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(text, style = Schrift.Klein, color = Farben.Text)
            unter.forEach { zeile ->
                Text(
                    zeile,
                    style = Schrift.Winzig,
                    color = if (zeile.startsWith("richtig:")) Farben.GruenHell else Farben.TextLeise,
                )
            }
        }
    }
}

/** Nie schon sortiert anbieten — sonst wäre die Karte mit einem Tipp auf „Prüfen" gelöst. */
private fun gemischteReihe(k: Lernkarte): List<String> {
    if (k.art != "Reihenfolge") return emptyList()
    var gemischt = k.schritte.shuffled()
    repeat(5) { if (k.schritte.size > 1 && gemischt == k.schritte) gemischt = k.schritte.shuffled() }
    if (k.schritte.size > 1 && gemischt == k.schritte) gemischt = k.schritte.reversed()
    return gemischt
}

private fun getauscht(liste: List<String>, i: Int, j: Int): List<String> {
    if (j < 0 || j >= liste.size) return liste
    return liste.toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }
}
