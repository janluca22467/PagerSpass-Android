package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.netz.Beteiligter
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Polizeiabfrage
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Beteiligten einer Polizeilage — übertragen aus `BeteiligtenFenster.vue` und
 * `BeteiligtenBogen.vue` (Web 5.0.0.26), im Fahrzeug. Das Gegenstück zu PatSim an
 * der Polizeilage: die Personen, ihre Personalien und was die Register über sie
 * wissen. Er zeigt sich nur mit eingeschalteter Simulation und nur einer Streife.
 *
 * <b>Im Fahrzeug ist jede Kachel eine Bitte an die Leitstelle über Funk.</b> Die
 * Register fragt die Leitstelle ab; nach dem Ausweis fragen, handeln und einordnen
 * kann nur die Streife vor Ort.
 */

/** Die Register mit ihren Kürzeln, wie sie am Gerät im Wagen stehen. */
private val REGISTER = mapOf(
    "Person" to "INPOL",
    "Melderegister" to "EWO",
    "Fuehrerschein" to "ZFER",
    "Waffenregister" to "NWR",
    "Halter" to "ZEVIS",
    "Sachfahndung" to "FAHND",
    "Versicherung" to "VERS",
)

private fun zustandFarbe(p: Beteiligter): Color? = when {
    p.abfragen.any { it.eigensicherung } -> Farben.Signal
    p.abfragen.any { it.treffer } -> Farben.Amber
    p.abgeschlossen -> Farben.Gruen
    else -> null
}

private fun wartet(p: Beteiligter) = p.abfragen.any { it.beiLeitstelle || (it.angefragtVon != null && it.ergebnis == null) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.BeteiligtenFenster(einsatz: Einsatz, meins: Rundenfahrzeug, befehle: Raumbefehle) {
    if (meins.organisation != "Polizei") return
    val personen = einsatz.beteiligte
    if (personen.isEmpty()) return
    val vorOrt = meins.status == 4 || meins.status == 7
    var gewaehlt by remember(einsatz.id) { mutableStateOf<String?>(null) }
    var erweitert by remember(einsatz.id) { mutableStateOf(false) }
    val aktuell = personen.firstOrNull { it.id == gewaehlt } ?: personen.first()

    val auswahl: @Composable () -> Unit = {
        if (personen.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                personen.forEach { p ->
                    Auswahlblatt(
                        kennung = p.id,
                        unterzeile = p.rolleText + if (wartet(p)) " · Funk" else "",
                        an = aktuell.id == p.id,
                        farbe = zustandFarbe(p),
                        beiDruck = { gewaehlt = p.id },
                    )
                }
            }
        }
    }

    Kasten(innenraum = Abstand.Normal, abstandInnen = Abstand.Klein) {
        Bogenkopf(
            "Polizei · Abfragen" + if (personen.size > 1) " · ${personen.size} Personen" else "",
            beiErweitern = { erweitert = true },
        )
        if (!vorOrt) SehrLeise("Abfragen gehen schon auf der Anfahrt — nach dem Ausweis fragen und handeln erst vor Ort.")
        auswahl()
        if (!erweitert) {
            BeteiligtenBogen(einsatz, aktuell, vorOrt, gross = false, befehle)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                SehrLeise("Die Abfragen sind aufgeklappt.", mono = true)
                Textweg("zuklappen", { erweitert = false })
            }
        }
    }

    if (erweitert) {
        Vollblende("Polizei · ${meins.funkrufname}", "${aktuell.id} · ${aktuell.rolleText}", { erweitert = false }) {
            auswahl()
            BeteiligtenBogen(einsatz, aktuell, vorOrt, gross = true, befehle)
        }
    }
}

private enum class Personenseite(val titel: String) {
    Auskuenfte("Auskünfte"),
    Massnahmen("Maßnahmen"),
    Einordnen("Einordnen"),
    Verlauf("Verlauf"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.BeteiligtenBogen(einsatz: Einsatz, p: Beteiligter, vorOrt: Boolean, gross: Boolean, befehle: Raumbefehle) {
    val jetzt = sekundentakt()
    var seite by remember(p.id) { mutableStateOf(Personenseite.Auskuenfte) }
    var einordnung by remember(p.id) { mutableStateOf("") }
    val handeln = vorOrt

    val ausweisRest = restSekunden(p.feststellungFertigUm, jetzt)
    val dienststelleRest = restSekunden(p.dienststelleFertigUm, jetzt)
    val abfrageRest = restSekunden(p.abfrageFertigUm, jetzt)
    val abfrageDauer = p.abfragen.firstOrNull { it.art == p.fragtAb }?.dauer ?: 20.0
    val eigensicherung = p.abfragen.any { it.eigensicherung }

    fun sperre(a: Polizeiabfrage): String? = when {
        a.ergebnis != null -> null
        a.beiLeitstelle -> "Die Auskunft liegt der Leitstelle vor — sie kommt über Funk."
        a.angefragtVon != null -> "Schon bei der Leitstelle angefragt."
        a.hindernis != null && !a.hindernis.startsWith("Es läuft schon") -> a.hindernis
        else -> null
    }
    val ausweisGeht = handeln && p.anwesend && p.mitgenommenVon == null && !p.personalienFestgestellt && ausweisRest == null

    // ------------------------------------------------------------------ Kopf
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        Etikett("${p.id} · ${p.rolleText}")
        when {
            !p.anwesend -> Marke("nicht vor Ort")
            p.mitgenommenVon != null -> Marke("mitgenommen · ${p.mitgenommenVon}", farbe = Farben.BlauHell)
            p.verhalten != null -> Marke(
                p.verhalten,
                farbe = when (p.verhalten) {
                    "ruhig" -> Farben.GruenHell
                    "angespannt" -> Farben.AmberHell
                    else -> Farben.SignalHell
                },
            )
        }
        if (eigensicherung) Marke("Eigensicherung", farbe = Farben.SignalHell)
        if (p.abgeschlossen) Marke("abgeschlossen", farbe = Farben.GruenHell)
    }
    Text(p.name ?: p.beschreibung, style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold), color = if (p.name != null) Farben.Text else Farben.TextLeise)

    // --------------------------------------------------------------- Ausweis
    // Eine Karte statt einer Zeile, weil er das ist, wonach man fragt. Vorher ist die
    // ganze Karte der Knopf — mit Uhr, und mit dem Grund, wenn es gerade nicht geht.
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = Farben.FlaecheHoch,
                randfarbe = when {
                    p.personalienFestgestellt && p.identitaetGeklaert -> Farben.Gruen
                    p.personalienFestgestellt -> Farben.Amber
                    ausweisRest != null || dienststelleRest != null -> Farben.Amber
                    else -> Farben.Rand
                },
                ecke = 9.dp,
                mitLichtkante = false,
            )
            .clickable(enabled = ausweisGeht, role = Role.Button) { befehle.personalienFeststellen(einsatz.id, p.id) }
            .padding(Abstand.Klein),
    ) {
        when {
            dienststelleRest != null -> {
                SehrLeise("Identität auf der Dienststelle …")
                Text(if (dienststelleRest > 0) "${dienststelleRest / 60}:${(dienststelleRest % 60).toString().padStart(2, '0')}" else "…", style = Schrift.Gross.copy(fontFamily = Schrift.Mono), color = Farben.Text)
                SehrLeise("Von ${p.mitgenommenVon ?: "der Streife"} mitgenommen.")
                Laufbalken(1f - dienststelleRest / 180f)
            }
            p.personalienFestgestellt -> {
                Row {
                    SehrLeise(p.dokument.orEmpty(), Modifier.weight(1f))
                    Text(
                        if (p.identitaetGeklaert) "✓ geprüft" else "mündlich, ungeprüft",
                        style = Schrift.Winzig,
                        color = if (p.identitaetGeklaert) Farben.GruenHell else Farben.AmberHell,
                    )
                }
                Text(p.name.orEmpty(), style = Schrift.Gross.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                Text(
                    "geb. ${p.geburtsdatum.orEmpty()}" + (p.anschrift?.let { " · $it" } ?: ""),
                    style = Schrift.Klein.copy(fontFamily = Schrift.Mono),
                    color = Farben.TextLeise,
                )
            }
            ausweisRest != null -> {
                SehrLeise("Nach dem Ausweis gefragt …")
                Text(if (ausweisRest > 0) "$ausweisRest s" else "…", style = Schrift.Gross.copy(fontFamily = Schrift.Mono), color = Farben.Text)
                Laufbalken(1f - ausweisRest / 30f)
            }
            else -> {
                Row {
                    SehrLeise(if (p.verweigert) "Angaben verweigert" else "Personalien", Modifier.weight(1f))
                    if (ausweisGeht) {
                        Text(
                            if (p.verweigert) "noch einmal fragen" else "nach Ausweis fragen · 30 s",
                            style = Schrift.Winzig,
                            color = Farben.Amber,
                        )
                    }
                }
                Text(
                    if (p.verweigert) "Erst beruhigen — oder Identität auf der Dienststelle." else "nicht festgestellt",
                    style = Schrift.Normal,
                    color = Farben.TextSehrLeise,
                )
                when {
                    !p.anwesend -> SehrLeise("Nicht vor Ort — nur über die Register erreichbar.")
                    !vorOrt -> SehrLeise("Erst an der Einsatzstelle.")
                }
            }
        }
    }
    p.kennzeichen?.let { Marke(it, farbe = Farben.Text) }

    SehrLeise("Abfragen gehen über die Leitstelle — tippen heißt: über Funk anfragen.")

    // -------------------------------------------------------------- Register
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        maxItemsInEachRow = 3,
        modifier = Modifier.fillMaxWidth(),
    ) {
        p.abfragen.forEach { a ->
            val laeuft = p.fragtAb == a.art && abfrageRest != null
            Wertkachel(
                name = REGISTER[a.art] ?: a.art,
                zahl = when {
                    laeuft -> if ((abfrageRest ?: 0) > 0) "$abfrageRest s" else "…"
                    a.ergebnis != null -> when {
                        a.eigensicherung && !a.treffer -> "Vorsicht"
                        a.treffer -> "Treffer"
                        else -> "o. E."
                    }
                    a.beiLeitstelle -> "bei LS"
                    a.angefragtVon != null -> "angefragt"
                    else -> "—"
                },
                fuss = when {
                    laeuft -> "fragt ab …"
                    a.ergebnis != null -> a.ergebnis
                    a.beiLeitstelle -> "kommt über Funk"
                    a.angefragtVon != null -> a.angefragtVon
                    else -> "anfragen"
                },
                auffaellig = a.treffer || a.eigensicherung,
                leer = a.ergebnis == null && !laeuft && a.angefragtVon == null && !a.beiLeitstelle,
                laeuft = if (laeuft) (1f - (abfrageRest ?: 0) / abfrageDauer.toFloat()).coerceIn(0f, 1f) else null,
                beiDruck = {
                    when {
                        a.ergebnis != null -> seite = Personenseite.Auskuenfte
                        sperre(a) == null -> befehle.abfrageAnfordern(einsatz.id, p.id, a.art)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    // Was gerade läuft — oder warum nichts geht. Die Zeile steht immer da.
    val laufendeAbfrage = p.fragtAb?.let { art ->
        val name = p.abfragen.firstOrNull { it.art == art }?.name ?: art
        if (p.abfrageVon != null) "$name · ${p.abfrageVon}" else name
    }
    val hinweis = p.abfragen.filter { it.ergebnis == null }.mapNotNull { sperre(it) }.distinct().joinToString(" ").ifBlank { null }
    when {
        laufendeAbfrage != null && abfrageRest != null -> Text("Läuft: $laufendeAbfrage · $abfrageRest s", style = Schrift.Klein, color = Farben.Text)
        hinweis != null -> Text(hinweis, style = Schrift.Klein, color = Farben.AmberHell)
        else -> SehrLeise("Keine Abfrage läuft.")
    }

    // ------------------------------------------------------------- die Arbeit
    if (!gross) Bogenreiter(Personenseite.entries.map { it to it.titel }, seite, { seite = it })
    val zeige = { s: Personenseite -> gross || seite == s }
    val auskuenfte = p.abfragen.filter { it.ergebnis != null }
        .sortedWith(compareByDescending<Polizeiabfrage> { it.eigensicherung }.thenByDescending { it.treffer })
    val befunde = p.massnahmen.filter { it.ergebnis != null }

    if (zeige(Personenseite.Auskuenfte)) {
        if (gross) Etikett("Auskünfte")
        Text(p.beschreibung, style = Schrift.Klein, color = Farben.TextLeise)
        if (auskuenfte.isEmpty() && befunde.isEmpty()) SehrLeise("Noch nichts abgefragt. Tippe oben auf ein Register.")
        auskuenfte.forEach { a ->
            Column(
                modifier = Modifier.fillMaxWidth().flaeche(
                    farbe = Farben.FlaecheHoch,
                    randfarbe = if (a.eigensicherung) Farben.Signal else if (a.treffer) Farben.Amber else Farben.Rand,
                    ecke = 9.dp,
                    mitLichtkante = false,
                ).padding(Abstand.Klein),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Text(REGISTER[a.art] ?: a.art, style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Text)
                    Text(a.name, style = Schrift.Winzig, color = Farben.TextSehrLeise, modifier = Modifier.weight(1f))
                    if (a.eigensicherung) Marke("Eigensicherung", farbe = Farben.SignalHell) else if (a.treffer) Marke("Treffer", farbe = Farben.AmberHell)
                }
                Text(a.ergebnis.orEmpty(), style = Schrift.Klein, color = Farben.Text)
            }
        }
        befunde.forEach { m ->
            Column(Modifier.fillMaxWidth().flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false).padding(Abstand.Klein)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text("Maßnahme", style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Text)
                    Text(m.name, style = Schrift.Winzig, color = Farben.TextSehrLeise)
                }
                Text(m.ergebnis.orEmpty(), style = Schrift.Klein, color = Farben.Text)
            }
        }
    }

    if (zeige(Personenseite.Massnahmen)) {
        if (gross) Etikett("Maßnahmen")
        if (!vorOrt) SehrLeise("Maßnahmen erst an der Einsatzstelle.")
        p.massnahmen.forEach { m ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().flaeche(
                    farbe = Farben.FlaecheHoch.copy(alpha = if (!m.erledigt && (m.hindernis != null || !handeln)) 0.5f else 1f),
                    ecke = 9.dp,
                    mitLichtkante = false,
                ).padding(Abstand.Klein),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text)
                    if (m.id == "identitaet" && !m.erledigt) SehrLeise("dauert 3 min · die Streife nimmt die Person mit")
                    if (m.brauchtVerstaerkung) Text("zweite Streife", style = Schrift.Winzig, color = Farben.AmberHell)
                    m.ergebnis?.let { SehrLeise(it) }
                    if (!m.erledigt && m.hindernis != null && handeln) Text(m.hindernis, style = Schrift.Winzig, color = Farben.AmberHell)
                }
                if (m.erledigt) {
                    Text("✓ erledigt", style = Schrift.Klein, color = Farben.GruenHell)
                } else {
                    Knopf("Treffen", { befehle.polizeiMassnahme(einsatz.id, p.id, m.id) }, aktiv = m.hindernis == null && handeln, kompakt = true)
                }
            }
        }
    }

    if (zeige(Personenseite.Einordnen)) {
        if (gross) Etikett("Einordnen")
        val genug = auskuenfte.isNotEmpty()
        Pruefzeile(p.personalienFestgestellt || !p.anwesend, "Personalien")
        Pruefzeile(p.identitaetGeklaert || !p.anwesend, "Identität geprüft")
        Pruefzeile(genug, "eine Abfrage")
        Etikett("Tatvorwurf / Einordnung")
        if (p.einordnung != null) {
            Text(p.einordnung, style = Schrift.Normal, color = Farben.Text)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = einordnung,
                    beiAenderung = { einordnung = it.take(120) },
                    platzhalter = "z. B. Körperverletzung § 223 StGB",
                    aktiv = genug && handeln,
                    weiterTaste = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
                Knopf("Festhalten", {
                    befehle.beteiligterEinordnen(einsatz.id, p.id, einordnung)
                    einordnung = ""
                }, aktiv = genug && handeln && einordnung.isNotBlank(), kompakt = true)
            }
            if (!genug) SehrLeise("Erst überprüfen: mindestens eine Abfrage.")
        }
    }

    if (zeige(Personenseite.Verlauf)) {
        if (gross) Etikett("Verlauf")
        val vorsatz = "${p.id}: "
        val verlauf = einsatz.chronologie.filter { it.text.startsWith(vorsatz) }.asReversed().take(40)
        if (verlauf.isEmpty()) SehrLeise("Noch nichts geschehen.")
        verlauf.forEach { c ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(uhrzeitMitSekunden(c.zeit), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise, modifier = Modifier.width(60.dp))
                Text(c.text.removePrefix(vorsatz), style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                c.urheber?.let { Text(it, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise) }
            }
        }
    }
}
