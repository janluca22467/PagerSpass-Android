package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Gemeinschaftsmitglied
import de.pagerspass.pagerspass.netz.Gemeinschaftsnachricht
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.profilbildAdresse
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * Der Chat einer Wachengemeinschaft — übertragen aus `GemeinschaftsChat.vue`.
 *
 * <b>Drei Arten von Zeilen</b>, die verschieden aussehen, weil sie Verschiedenes
 * sind: Getipptes, Vermerke des Servers und der Aushang einer Clanrunde. Der
 * Aushang ist eine Kachel mit Knopf und keine Zeile mit Code zum Abtippen — er
 * ist eine Einladung, die man annimmt.
 *
 * <b>Zum Lesen aufbereitet:</b> Tagestrenner, wo der Tag wechselt („23:15" von
 * vorgestern las sich wie eines von heute Nacht), und Blöcke statt
 * Einzelblasen — wer dreimal binnen fünf Minuten schreibt, bekommt Wappen und
 * Namen nur einmal.
 *
 * <b>Gerollt wird wie im Funk:</b> automatisch nur, solange man unten steht. Wer
 * nach oben gerollt hat, liest nach — dem zieht die nächste Zeile nicht den
 * Boden weg; stattdessen erscheint „Neue Nachrichten ↓". Früher standen hier nur
 * die letzten sechzig Zeilen, ohne Rollbereich.
 */
@Composable
internal fun Wachenchat(
    verlauf: List<Gemeinschaftsnachricht>,
    ich: String,
    laufendeRunde: String?,
    mitglieder: Map<String, Gemeinschaftsmitglied>,
    server: String,
    beiSenden: (String) -> Unit,
    beiBeitreten: (String) -> Unit,
    beiOeffnen: (Gemeinschaftsnachricht) -> Unit,
) {
    var entwurf by remember { mutableStateOf("") }
    val rollen = rememberScrollState()
    val umfang = rememberCoroutineScope()
    val schwelle = with(LocalDensity.current) { 40.dp.toPx() }
    var unten by remember { mutableStateOf(true) }
    var neueUnten by remember { mutableStateOf(false) }
    val posten = remember(verlauf) { postenVon(verlauf) }

    // Ob man unten steht, entscheidet allein das Rollen — wächst nur der Inhalt,
    // bleibt der Stand, und die neue Zeile darf nachrollen.
    LaunchedEffect(rollen) {
        snapshotFlow { rollen.value }.collect { wert ->
            val ende = rollen.maxValue
            if (ende != Int.MAX_VALUE) {
                unten = wert >= ende - schwelle
                if (unten) neueUnten = false
            }
        }
    }

    val letzte = verlauf.lastOrNull()
    LaunchedEffect(letzte?.nr, verlauf.size) {
        // Erst nach dem Messen ist bekannt, wie weit „unten" ist.
        withFrameNanos { }
        val vonMir = letzte != null && letzte.von == ich
        if (unten || vonMir) {
            if (rollen.maxValue != Int.MAX_VALUE) rollen.animateScrollTo(rollen.maxValue)
            neueUnten = false
        } else if (letzte != null) {
            neueUnten = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rollen)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Normal),
            ) {
                if (verlauf.isEmpty()) {
                    Spacer(Modifier.height(160.dp))
                    SehrLeise(
                        "Noch nichts geschrieben. Der erste Satz gehört dir.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                posten.forEach { p ->
                    when (p) {
                        is Posten.Trenner -> Tagestrenner(p.text)
                        is Posten.Zeile -> Chatposten(
                            n = p.n,
                            erste = p.erste,
                            ich = ich,
                            laufendeRunde = laufendeRunde,
                            mitglied = p.n.von?.let { mitglieder[it] },
                            server = server,
                            beiBeitreten = beiBeitreten,
                            beiOeffnen = beiOeffnen,
                        )
                    }
                }
            }

            if (neueUnten) {
                Text(
                    text = "Neue Nachrichten ↓",
                    style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                    color = Farben.AufAmber,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Abstand.Klein)
                        .background(Farben.Amber, Rundung.Rund)
                        .clickable {
                            umfang.launch { rollen.animateScrollTo(rollen.maxValue) }
                            neueUnten = false
                        }
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(Farben.Rand, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                }
                .padding(Abstand.Normal),
        ) {
            Feld(
                wert = entwurf,
                beiAenderung = { entwurf = it.take(500) },
                platzhalter = "An die Wache schreiben …",
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "Senden",
                {
                    val text = entwurf.trim()
                    if (text.isNotEmpty()) {
                        // Das Feld wird sofort frei: Die Zeile kommt über den Hub zurück.
                        entwurf = ""
                        beiSenden(text)
                    }
                },
                aktiv = entwurf.isNotBlank(),
                kompakt = true,
            )
        }
    }
}

/** Ein Eintrag der Anzeige: ein Tagestrenner oder eine Zeile des Verlaufs. */
private sealed interface Posten {
    data class Trenner(val text: String) : Posten
    data class Zeile(val n: Gemeinschaftsnachricht, val erste: Boolean) : Posten
}

/** Fünf Minuten sind das Tempo eines Gesprächs — was später kommt, ist ein neuer Anlauf. */
private val GRUPPENFENSTER: Duration = Duration.ofMinutes(5)

private fun postenVon(verlauf: List<Gemeinschaftsnachricht>): List<Posten> = buildList {
    var letzterTag: java.time.LocalDate? = null
    var vorherige: Gemeinschaftsnachricht? = null

    for (n in verlauf) {
        val wann = wachenzeit(n.gesendetUm)
        val tag = wann?.toLocalDate()

        if (wann != null && tag != letzterTag) {
            add(Posten.Trenner(tagestext(wann)))
            letzterTag = tag
            // Über einen Tagestrenner hinweg gruppiert nichts.
            vorherige = null
        }

        val istBlase = n.art != "System" && n.art != "Clanrunde" && !n.entfernt
        val davor = vorherige
        val davorWann = davor?.let { wachenzeit(it.gesendetUm) }
        val erste = !istBlase ||
            davor == null ||
            davor.von != n.von ||
            wann == null ||
            davorWann == null ||
            Duration.between(davorWann, wann) > GRUPPENFENSTER

        add(Posten.Zeile(n, erste))

        // Vermerke und Kacheln unterbrechen einen Block.
        vorherige = if (istBlase) n else null
    }
}

@Composable
private fun Tagestrenner(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Abstand.Klein),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(Farben.Rand))
        SehrLeise(text)
        Box(Modifier.weight(1f).height(1.dp).background(Farben.Rand))
    }
}

@Composable
private fun Chatposten(
    n: Gemeinschaftsnachricht,
    erste: Boolean,
    ich: String,
    laufendeRunde: String?,
    mitglied: Gemeinschaftsmitglied?,
    server: String,
    beiBeitreten: (String) -> Unit,
    beiOeffnen: (Gemeinschaftsnachricht) -> Unit,
) {
    when {
        // Der Aushang einer Clanrunde: eine Einladung, kein Text zum Abtippen.
        n.art == "Clanrunde" -> Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = Abstand.Klein)
                .fillMaxWidth()
                .flaeche(
                    farbe = Farben.HauchAmber,
                    randfarbe = Farben.AmberTief,
                    ecke = 10.dp,
                    mitLichtkante = false,
                )
                .padding(Abstand.Normal),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Etikett("Clanrunde")
                Text(n.text, style = Schrift.Klein, color = Farben.Text)
                SehrLeise("${n.roomCode.orEmpty()} · ${wachenuhr(n.gesendetUm)}", mono = true)
            }
            val code = n.roomCode
            // Ein Knopf in eine längst beendete Runde führte nur zu „Diesen Raum
            // gibt es nicht (mehr)" — beitreten darf man nur der laufenden.
            if (code != null && code == laufendeRunde) {
                Knopf("Beitreten", { beiBeitreten(code) }, art = Knopfart.Haupt, kompakt = true)
            } else {
                SehrLeise("vorbei")
            }
        }

        n.art == "System" -> Vermerk("${n.text}  ${wachenuhr(n.gesendetUm)}")

        // Eine herausgenommene Zeile bleibt als Vermerk stehen — eine Lücke im
        // Verlauf läse sich wie ein Fehler.
        n.entfernt -> Vermerk("Zeile von ${n.vonName} herausgenommen.  ${wachenuhr(n.gesendetUm)}")

        else -> {
            val eigen = n.von == ich
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    Abstand.Klein,
                    if (eigen) Alignment.End else Alignment.Start,
                ),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (erste) Abstand.Klein else 0.dp),
            ) {
                if (!eigen) {
                    if (erste) {
                        Kontobild(
                            kennung = n.von ?: n.vonName,
                            anzeigename = n.vonName,
                            wappen = mitglied?.wappen ?: "Keines",
                            wappenfarbe = mitglied?.wappenfarbe ?: 0,
                            bildAdresse = profilbildAdresse(server, mitglied?.profilbild),
                            groesse = 32.dp,
                        )
                    } else {
                        Spacer(Modifier.width(32.dp))
                    }
                } else {
                    Zeilenknopf(n, beiOeffnen)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 300.dp)
                        .background(
                            if (eigen) Farben.Amber.copy(alpha = 0.13f) else Farben.FlaecheHoch,
                            RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    if (!eigen && erste) {
                        Text(
                            text = n.vonName.uppercase(),
                            style = Schrift.Etikett,
                            color = Farben.TextLeise,
                        )
                    }
                    Text(text = n.text, style = Schrift.Klein, color = Farben.Text)
                    Text(
                        text = wachenuhr(n.gesendetUm),
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = Farben.TextSehrLeise,
                        textAlign = if (eigen) TextAlign.End else TextAlign.Start,
                    )
                }

                if (!eigen) Zeilenknopf(n, beiOeffnen)
            }
        }
    }
}

/**
 * Das „⋯" neben jeder Blase — immer sichtbar, nicht erst beim Überfahren: Am
 * Finger gibt es kein „darüber", und eine Meldefunktion, die man nicht findet,
 * ist keine.
 */
@Composable
private fun Zeilenknopf(n: Gemeinschaftsnachricht, beiOeffnen: (Gemeinschaftsnachricht) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clickable { beiOeffnen(n) },
    ) {
        Text("⋯", style = Schrift.Gross, color = Farben.TextSehrLeise)
    }
}

@Composable
private fun Vermerk(text: String) {
    Text(
        text = text,
        style = Schrift.Klein,
        color = Farben.TextSehrLeise,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Abstand.Klein),
    )
}
