package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Werkstand
import de.pagerspass.pagerspass.netz.Lehrgang
import de.pagerspass.pagerspass.netz.Lehrgangsmodul
import de.pagerspass.pagerspass.netz.Theorieurteil
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Lehrgänge — das Gegenstück zu `web/src/views/LehrgangView.vue` (5.0.0.26).
 *
 * Lesen, üben, prüfen — in dieser Reihenfolge. **Wiederholen darf man immer.**
 * Das nächste offene Modul trägt den Hauptknopf; alles andere bleibt
 * erreichbar, nur leiser.
 *
 * <b>Fünf Arten Modul.</b> Lesestoff öffnet die Wikiseite und hakt sich damit
 * selbst ab. Die **Übung** (`LernUebung`) ist durch, wenn jede Karte einmal saß,
 * und hakt sich dann ebenso ab. Die **Wissensprüfung** gibt ihre Kreuze beim
 * Server ab, der allein die Lösung kennt. Lektion und Prüfungsschicht erledigen
 * sich durch Fahren — dafür gibt es bewusst keinen Haken zum Setzen.
 *
 * <b>Oben der Ausbildungsstand</b>: wie viele Lehrgänge bestanden sind und ein
 * Knopf „Weiterlernen", der zum angefangenen (sonst ersten offenen) Lehrgang
 * springt.
 */
@Composable
fun LehrgangSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Werkstand = Werkstand(),
    beiLaden: (Boolean) -> Unit = {},
    beiLesen: (Lehrgang, Lehrgangsmodul) -> Unit = { _, _ -> },
    beiLektion: () -> Unit = {},
    beiPruefung: (Lehrgang, Lehrgangsmodul) -> Unit = { _, _ -> },
    beiSchicht: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
    einweisungOffen: Boolean = false,
    beiUebungGeschafft: (Lehrgang, Lehrgangsmodul) -> Unit = { _, _ -> },
    beiTheorie: (Lehrgang, Lehrgangsmodul, Map<String, Int>, (Theorieurteil) -> Unit) -> Unit = { _, _, _, _ -> },
) {
    LaunchedEffect(Unit) { beiLaden(false) }

    // Immer nur ein Modul offen — eine Übung oder eine Wissensprüfung.
    var offenesModul by rememberSaveable { mutableStateOf<String?>(null) }
    // Für „Weiterlernen": wohin die Seite springt — mit Zähler, damit derselbe
    // Lehrgang ein zweites Mal angesprungen werden kann.
    var sprung by remember { mutableStateOf(0 to "") }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Lehrgänge",
            knoepfe = { Knopf("← Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )
        Leise("Lesen, üben, prüfen — in dieser Reihenfolge. Wiederholen darfst du immer.")

        val lehrgaenge = stand.lehrgaenge.inhalt.orEmpty()
        if (stand.lehrgaenge.geladen && lehrgaenge.isNotEmpty()) {
            Ausbildungsstand(lehrgaenge, beiWeiter = { offenesModul = null; sprung = (sprung.first + 1) to it.id })
        }

        Rueckmeldung(null, stand.fehler)

        Bereich(
            laedt = stand.lehrgaenge.ersteLadung,
            fehler = stand.lehrgaenge.fehler,
            inhalt = stand.lehrgaenge.inhalt,
            beiErneut = { beiLaden(true) },
        ) { liste ->
            if (liste.isEmpty()) {
                Leerhinweis("Auf diesem Server sind keine Lehrgänge hinterlegt.")
            }
            val einstieg = einstieg(liste, stand.spielrolle)
            liste.forEach { l ->
                Kurs(
                    l = l,
                    laeuft = stand.laeuft,
                    empfohlen = l.id == einstieg,
                    einweisungOffen = einweisungOffen,
                    sprung = if (sprung.second == l.id) sprung.first else 0,
                    offenesModul = offenesModul,
                    beiOeffnen = { offenesModul = it },
                    beiLesen = beiLesen,
                    beiLektion = beiLektion,
                    beiPruefung = beiPruefung,
                    beiSchicht = beiSchicht,
                    beiUebungGeschafft = beiUebungGeschafft,
                    beiTheorie = beiTheorie,
                )
            }
        }
    }
}

/**
 * Der Ausbildungsstand: wie viele Lehrgänge stehen, und wo es weitergeht. Ein
 * Knopf statt einer Liste — die Liste steht ohnehin darunter.
 */
@Composable
private fun Ausbildungsstand(lehrgaenge: List<Lehrgang>, beiWeiter: (Lehrgang) -> Unit) {
    val bestanden = lehrgaenge.count { it.bestanden }
    // Der Lehrgang, an dem man als Nächstes weitermacht: der erste angefangene,
    // sonst der erste offene.
    val offen = lehrgaenge.filter { !it.bestanden && !it.gesperrt }
    val weiter = offen.firstOrNull { l -> l.module.any { it.erledigt } } ?: offen.firstOrNull()

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Dein Ausbildungsstand",
                style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                color = Farben.Text,
                modifier = Modifier.weight(1f),
            )
            Text("$bestanden von ${lehrgaenge.size} bestanden", style = Schrift.MonoKlein, color = Farben.Text)
        }
        Lernbalken(anteil = bestanden.toFloat() / lehrgaenge.size)
        when {
            weiter != null -> Knopf("Weiterlernen: ${weiter.titel}", { beiWeiter(weiter) }, art = Knopfart.Haupt, kompakt = true)
            bestanden == lehrgaenge.size -> Text(
                "Alle Lehrgänge bestanden — du hast alles gelernt, was es hier zu lernen gibt.",
                style = Schrift.Klein,
                color = Farben.GruenHell,
            )
        }
    }
}

private val ARTBEZEICHNUNG = mapOf(
    "Lesestoff" to "Lesen",
    "Lektion" to "Lektion",
    "Pruefung" to "Prüfung",
    "Theorie" to "Prüfung",
    "Uebung" to "Übung",
)

/**
 * Welcher Lehrgang „Dein Einstieg" ist: der erste, der für die Rolle aus dem
 * Einrichtungsbogen empfohlen wird. „Beides" und ein offener Bogen heben die
 * Leitstelle hervor — sie ist der empfohlene erste Lehrgang für alle.
 */
private fun einstieg(liste: List<Lehrgang>, spielrolle: String?): String? {
    val gesucht = if (spielrolle == "Fahrzeug") "Fahrzeug" else "Leitstelle"
    val erster = liste.firstOrNull { it.empfohlen == gesucht } ?: return null
    return erster.id.takeIf { !erster.bestanden && !erster.gesperrt }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Kurs(
    l: Lehrgang,
    laeuft: Boolean,
    empfohlen: Boolean,
    einweisungOffen: Boolean,
    sprung: Int,
    offenesModul: String?,
    beiOeffnen: (String?) -> Unit,
    beiLesen: (Lehrgang, Lehrgangsmodul) -> Unit,
    beiLektion: () -> Unit,
    beiPruefung: (Lehrgang, Lehrgangsmodul) -> Unit,
    beiSchicht: (String) -> Unit,
    beiUebungGeschafft: (Lehrgang, Lehrgangsmodul) -> Unit,
    beiTheorie: (Lehrgang, Lehrgangsmodul, Map<String, Int>, (Theorieurteil) -> Unit) -> Unit,
) {
    val dran = if (l.gesperrt) null else l.module.firstOrNull { !it.erledigt }?.id
    val anfrage = remember { BringIntoViewRequester() }
    LaunchedEffect(sprung) { if (sprung > 0) anfrage.bringIntoView() }
    val anteil = if (l.module.isEmpty()) 0f else l.module.count { it.erledigt }.toFloat() / l.module.size

    // Gesperrt ist kein Fehler, sondern eine Reihenfolge — deshalb der leise,
    // gestrichelte Rand und keine Signalfarbe. Der Einstieg trägt Amber.
    val rahmen = when {
        l.gesperrt -> Modifier.drawBehind {
            drawRoundRect(
                color = Farben.Rand,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                ),
            )
        }.padding(1.dp).flaeche(randfarbe = Farben.Flaeche)
        empfohlen -> Modifier.flaeche(randfarbe = Farben.Amber)
        else -> Modifier.flaeche()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(anfrage).then(rahmen).padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                Text(
                    (if (l.gesperrt) "🔒 " else "") + l.titel,
                    style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                )
                Leise(l.beschreibung)
                // Solange die Ausbildungsschicht noch Pflicht ist: Dieser Lehrgang ist
                // der andere Weg dorthin.
                if (l.ersetztEinweisung && !l.bestanden && einweisungOffen) {
                    Text(
                        "Bestanden ersetzt er die Ausbildungsschicht.",
                        style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                        modifier = Modifier.padding(top = Abstand.Klein),
                    )
                }
            }
            when {
                l.bestanden -> Marke("Bestanden", farbe = Farben.GruenHell)
                l.gesperrt -> Marke("Gesperrt")
                empfohlen -> Marke("Dein Einstieg", farbe = Farben.Amber)
            }
        }

        // Die Sperre nennt ihren Grund — ein ausgegrauter Knopf ohne Satz
        // daneben sieht aus wie ein Fehler.
        if (l.gesperrt) {
            Leise(sperrsatz(l))
        } else if (!l.bestanden) {
            Lernbalken(anteil, text = "${Math.round(anteil * 100)} %")
        }

        if (l.bestanden) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Farben.Gruen, Rundung.Klein)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text("${l.titel} — bestanden am ${tag(l.bestandenUm)}.", style = Schrift.Klein, color = Farben.Text)
                l.zeugnisSchicht?.let { code -> Textweg("Prüfungsschicht ansehen →", { beiSchicht(code) }) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
            l.module.forEach { m ->
                Modul(
                    l = l,
                    m = m,
                    istDran = m.id == dran,
                    laeuft = laeuft,
                    offen = offenesModul == m.id,
                    beiOeffnen = beiOeffnen,
                    beiLesen = beiLesen,
                    beiLektion = beiLektion,
                    beiPruefung = beiPruefung,
                    beiUebungGeschafft = beiUebungGeschafft,
                    beiTheorie = beiTheorie,
                )
            }
        }
    }
}

@Composable
private fun Modul(
    l: Lehrgang,
    m: Lehrgangsmodul,
    istDran: Boolean,
    laeuft: Boolean,
    offen: Boolean,
    beiOeffnen: (String?) -> Unit,
    beiLesen: (Lehrgang, Lehrgangsmodul) -> Unit,
    beiLektion: () -> Unit,
    beiPruefung: (Lehrgang, Lehrgangsmodul) -> Unit,
    beiUebungGeschafft: (Lehrgang, Lehrgangsmodul) -> Unit,
    beiTheorie: (Lehrgang, Lehrgangsmodul, Map<String, Int>, (Theorieurteil) -> Unit) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (istDran) Farben.Amber else Farben.Rand, Rundung.Klein)
            .padding(Abstand.Klein),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Top) {
            Text(
                if (m.erledigt) "✓" else "○",
                style = Schrift.Normal,
                color = if (m.erledigt) Farben.Gruen else Farben.TextSehrLeise,
                modifier = Modifier.width(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                    Text(ARTBEZEICHNUNG[m.art] ?: m.art, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                    Text(
                        m.titel,
                        style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                        color = if (m.erledigt) Farben.TextLeise else Farben.Text,
                    )
                }
                Leise(m.text)
                // Am Versuch aufgehängt, nicht an `erledigt`: Eine nicht
                // bestandene Prüfung gilt ausdrücklich als nicht erledigt —
                // und genau dann muss dieser Satz stehen.
                if ((m.art == "Pruefung" || m.art == "Theorie") && m.erledigtUm != null) {
                    Text(
                        (if (m.bestanden) "Bestanden" else "Noch nicht bestanden — fahr sie noch einmal.") +
                            " · ${tag(m.erledigtUm)}",
                        style = Schrift.Klein,
                        color = if (m.bestanden) Farben.Gruen else Farben.SignalHell,
                    )
                }
                val art = if (istDran) Knopfart.Haupt else Knopfart.Normal
                Row(Modifier.padding(top = Abstand.Winzig)) {
                    when (m.art) {
                        "Lesestoff" -> Knopf(
                            if (m.erledigt) "Nochmal lesen" else "Lesen",
                            { beiLesen(l, m) },
                            art = art,
                            aktiv = !l.gesperrt && m.wikiseite != null,
                            kompakt = true,
                        )
                        "Uebung" -> Knopf(
                            if (m.erledigt) "Nochmal üben" else "Üben",
                            { beiOeffnen(m.id) },
                            art = art,
                            aktiv = !l.gesperrt && !offen,
                            kompakt = true,
                        )
                        "Lektion" -> Knopf(
                            if (m.erledigt) "Nochmal fahren" else "Fahren",
                            beiLektion,
                            art = art,
                            aktiv = !l.gesperrt && !laeuft,
                            kompakt = true,
                        )
                        // Die Wissensprüfung verlangt dieselbe Reihenfolge wie die
                        // Ansicht zeigt: Erst wenn sie dran ist (oder schon bestanden),
                        // geht sie auf. Der Server weist eine zu frühe Abgabe ohnehin ab.
                        "Theorie" -> Knopf(
                            if (m.bestanden) "Nochmal prüfen" else "Prüfung beginnen",
                            { beiOeffnen(m.id) },
                            art = art,
                            aktiv = !l.gesperrt && (istDran || m.bestanden) && !offen,
                            kompakt = true,
                        )
                        else -> Knopf(
                            if (m.bestanden) "Nochmal prüfen" else "Prüfung fahren",
                            { beiPruefung(l, m) },
                            art = art,
                            aktiv = !l.gesperrt && !laeuft,
                            kompakt = true,
                        )
                    }
                }
            }
        }

        // Die Fragen stehen im Modul selbst und nicht in einer Blende: Wer eine
        // Erklärung nachlesen will, soll dabei den Lehrgang sehen, zu dem sie gehört.
        if (offen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Abstand.Klein)
                    .drawBehind {
                        drawLine(Farben.Rand, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                    },
            ) {
                when (m.art) {
                    "Uebung" -> LernUebung(
                        modul = m,
                        beiFertig = { beiUebungGeschafft(l, m) },
                        beiSchliessen = { beiOeffnen(null) },
                    )
                    "Theorie" -> Wissenspruefung(
                        m = m,
                        laeuft = laeuft,
                        beiAbgeben = { antworten, beiUrteil -> beiTheorie(l, m, antworten, beiUrteil) },
                        beiSchliessen = { beiOeffnen(null) },
                    )
                }
            }
        }
    }
}

/** Die Wissensprüfung: alle Fragen, je eine Antwort, dann das Urteil vom Server. */
@Composable
private fun Wissenspruefung(
    m: Lehrgangsmodul,
    laeuft: Boolean,
    beiAbgeben: (Map<String, Int>, (Theorieurteil) -> Unit) -> Unit,
    beiSchliessen: () -> Unit,
) {
    var versuch by remember(m.id) { mutableStateOf(0) }
    val antworten = remember(m.id, versuch) { mutableStateMapOf<String, Int>() }
    var urteil by remember(m.id, versuch) { mutableStateOf<Theorieurteil?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().padding(top = Abstand.Klein),
    ) {
        val u = urteil
        if (u == null) {
            Leise(
                "${antworten.size}/${m.fragen.size} beantwortet · ${m.fragen.size} Fragen, je eine Antwort richtig. " +
                    if (m.erlaubteFehler == 0) "Kein Fehler erlaubt." else "Bis zu ${m.erlaubteFehler} Fehler sind erlaubt.",
            )
            m.fragen.forEachIndexed { nr, f ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(Farben.Rand, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                        }
                        .padding(vertical = Abstand.Klein),
                ) {
                    Text("${nr + 1}. ${f.frage}", style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    f.antworten.forEachIndexed { i, antwort ->
                        Wahlzeile(
                            nr = if (antworten[f.id] == i) "●" else "○",
                            text = antwort,
                            zustand = if (antworten[f.id] == i) Wahlzustand.Gewaehlt else Wahlzustand.Offen,
                            aktiv = true,
                            beiDruck = { antworten[f.id] = i },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Spacer(Modifier.weight(1f))
                Knopf("Abbrechen", beiSchliessen, art = Knopfart.Leise, kompakt = true)
                Knopf(
                    "Abgeben",
                    { beiAbgeben(antworten.toMap()) { urteil = it } },
                    art = Knopfart.Haupt,
                    aktiv = !laeuft && m.fragen.all { antworten[it.id] != null },
                    kompakt = true,
                )
            }
        } else {
            val fehler = u.fehler.size
            Text(
                (if (u.bestanden) "Bestanden." else "Noch nicht bestanden.") +
                    when {
                        fehler == 0 -> " Alles richtig."
                        else -> " " + (if (fehler == 1) "Eine Antwort war" else "$fehler Antworten waren") +
                            " nicht richtig" + (if (!u.bestanden) " — erlaubt sind ${u.erlaubteFehler}" else "") + "."
                    },
                style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                color = if (u.bestanden) Farben.Gruen else Farben.SignalHell,
            )
            u.fehler.forEach { f ->
                Column(Modifier.padding(start = Abstand.Normal)) {
                    Text(f.frage, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    Leise(f.erklaerung)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Spacer(Modifier.weight(1f))
                Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true)
                // Nochmal: dieselben Fragen, leere Kreuze. Die Erklärungen hat man gelesen.
                if (!u.bestanden) Knopf("Noch einmal", { versuch++ }, art = Knopfart.Haupt, kompakt = true)
            }
        }
    }
}

/**
 * Was über einem gesperrten Lehrgang steht — aus den Titeln, die der Server
 * mitschickt, nicht aus einer eigenen Liste hier. Die Oder-Voraussetzung:
 * einer der genannten genügt.
 */
private fun sperrsatz(l: Lehrgang): String {
    val oder = l.voraussetzungenOder
    if (l.voraussetzungen.isEmpty() && oder.isEmpty()) return "Dieser Lehrgang ist noch gesperrt."
    val namen = l.voraussetzungen.map { "„$it“" }
    val alle = if (namen.size <= 1) namen.firstOrNull().orEmpty() else "${namen.dropLast(1).joinToString(", ")} und ${namen.last()}"
    val wahl = oder.joinToString(" oder ") { "„$it“" }
    return when {
        wahl.isEmpty() -> "Erst nach $alle."
        alle.isEmpty() -> "Erst nach $wahl — einer von beiden genügt."
        else -> "Erst nach $alle sowie $wahl."
    }
}
