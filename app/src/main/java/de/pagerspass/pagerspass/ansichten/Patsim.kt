package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.netz.Befundschema
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.ManvPatient
import de.pagerspass.pagerspass.netz.Messwert
import de.pagerspass.pagerspass.netz.Patientenmassnahme
import de.pagerspass.pagerspass.netz.Patientenmonitor
import de.pagerspass.pagerspass.netz.Patientenvermerk
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin

/**
 * PatSim — der Patient am Fahrzeug, übertragen aus `PatientenFenster.vue`,
 * `PatientBogen.vue`, `PatientMonitor.vue` und `EkgKurve.vue` (Web 5.0.0.26).
 *
 * <b>Ein eigener Kasten, kein Teil der Einsatzkarte.</b> Am Patienten arbeitet man
 * nicht nebenbei, sondern eine Weile am Stück; mitten in der Einsatzkarte wanderte
 * der Bogen bei jedem Blick weg. „Erweitern" legt ihn über die ganze Fläche — am
 * Handy das Gegenstück zum gelösten Fenster am Rechner.
 *
 * <b>Älterer Server.</b> Kommen statt `messwerte` nur die alten `werte`, baut die
 * Kachel ihre Anzeige daraus; ohne Monitor, Gespräch und Verlauf bleiben die
 * Abschnitte leer und sagen das.
 */

/** Die Kennzeichen der Sichtung — `KATEGORIE_LABEL`. */
internal fun kategorieLabel(k: String?): String = when (k) {
    "Rot" -> "SK I · rot"
    "Gelb" -> "SK II · gelb"
    "Gruen" -> "SK III · grün"
    "Schwarz" -> "SK IV · tot"
    else -> k.orEmpty()
}

internal fun kategorieFarbeSk(k: String?): Color = when (k) {
    "Rot" -> Color(0xFFE5484D)
    "Gelb" -> Farben.Amber
    "Gruen" -> Farben.Gruen
    "Schwarz" -> Color(0xFF6B7684)
    else -> Farben.Rand
}

/**
 * Einfach oder erweitert — ein gemeinsamer Stand für die ganze Sitzung, wie
 * `massnahmenkatalog.ts`. Einfach ist die Vorgabe, solange der Server nichts
 * anderes sagt: rund neunzig Maßnahmen sind für den ersten Einsatz eine Wand.
 */
private object Massnahmenstand {
    var alle by mutableStateOf(false)
    var geladen = false
}

/** Die Werte in der Reihenfolge der Erstuntersuchung — Kürzel, Einheit, Dauer. */
private data class Messbar(val id: String, val name: String, val kurz: String, val einheit: String, val dauer: Int)

private val MESSBAR = listOf(
    Messbar("Bewusstsein", "Bewusstsein", "GCS", "", 0),
    Messbar("Atemfrequenz", "Atemfrequenz", "AF", "/min", 10),
    Messbar("Sauerstoffsaettigung", "Sättigung", "SpO₂", "%", 8),
    Messbar("Puls", "Puls", "Puls", "/min", 6),
    // Ohne Einheit: RR ist immer mmHg, und „151/95 mmHg" passt in keine Handykachel.
    Messbar("Blutdruck", "Blutdruck", "RR", "", 20),
    Messbar("Blutzucker", "Blutzucker", "BZ", "mg/dl", 15),
    Messbar("Temperatur", "Temperatur", "Temp", "°C", 12),
    Messbar("Schmerz", "Schmerz", "NRS", "/10", 5),
    Messbar("Ekg", "EKG", "EKG", "", 15),
)

/** Was der Monitor fortlaufend misst — die Kacheln dafür fallen weg, solange er hängt. */
private val AM_MONITOR = setOf("Puls", "Sauerstoffsaettigung", "Atemfrequenz")

private val SCHEMATA = listOf(
    Triple("XAbcde", "xABCDE", "Erstuntersuchung — was sofort bedroht"),
    Triple("Sampler", "SAMPLER", "Anamnese — was vorher war"),
    Triple("Opqrst", "OPQRST", "Schmerz beschreiben"),
)

private val SCHNELLFRAGEN = listOf(
    "Was ist passiert?",
    "Haben Sie Schmerzen? Wo?",
    "Seit wann geht es Ihnen so?",
    "Nehmen Sie Medikamente?",
    "Haben Sie Allergien?",
    "Haben Sie Vorerkrankungen?",
)

private val TREND = mapOf("steigt" to "↑", "faellt" to "↓", "gleich" to "→", "geaendert" to "⇄")

private const val NUR_RETTUNGSDIENST = "Das ist Sache des Rettungsdienstes — ohne ihn gibt es nur Erste Hilfe."

/** Die Zahl ohne Einheit — die steht klein daneben. */
private fun zahlOhneEinheit(id: String, anzeige: String?): String {
    if (anzeige.isNullOrBlank()) return ""
    if (id == "Ekg") return anzeige
    return anzeige.replace(Regex("""\s*(mmHg|/min|%|mg/dl|°C|/10)$"""), "")
        .replace(Regex("""^(GCS|NRS)\s*"""), "")
        .trim()
}

/** Die Messwerte — am älteren Server aus `werte` gebaut, ohne Alter und Trend. */
private fun messwerteVon(p: ManvPatient): List<Messwert> =
    p.messwerte ?: p.werte.orEmpty().map { (was, anzeige) ->
        Messwert(
            was = was,
            name = MESSBAR.firstOrNull { it.id == was }?.name ?: was,
            anzeige = anzeige,
            auffaellig = was in p.auffaelligeWerte.orEmpty(),
        )
    }

/**
 * Der Kasten PatSim — erscheint nur, wenn die Simulation läuft und die eigene
 * Besatzung am Patienten ist (Status 4) oder ihn an Bord hat (Status 7). Die
 * eigenen Patienten zuerst: An einer Lage mit zwanzig Bögen ist das die erste
 * Frage der Besatzung.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.PatientenFenster(
    einsatz: Einsatz,
    meins: Rundenfahrzeug,
    premium: Boolean,
    server: String,
    befehle: Raumbefehle,
) {
    if (meins.status != 4 && meins.status != 7) return
    val patienten = einsatz.manvPatienten
        .filter { it.simuliert }
        .map { p ->
            p to (p.transportVehicleId == meins.id || p.behandlerVehicleId == meins.id || p.notarztVehicleId == meins.id)
        }
        .sortedWith(compareBy({ !it.second }, { it.first.id }))
    if (patienten.isEmpty()) return

    var gewaehlt by remember(einsatz.id) { mutableStateOf<String?>(null) }
    var erweitert by remember(einsatz.id) { mutableStateOf(false) }
    val aktuell = patienten.firstOrNull { it.first.id == gewaehlt } ?: patienten.first()

    LaunchedEffect(Unit) {
        if (!Massnahmenstand.geladen) {
            befehle.massnahmenkatalogLaden()?.let {
                Massnahmenstand.alle = it
                Massnahmenstand.geladen = true
            }
        }
    }

    val auswahl: @Composable () -> Unit = {
        if (patienten.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                patienten.forEach { (p, mein) ->
                    Auswahlblatt(
                        kennung = p.id,
                        unterzeile = listOfNotNull(
                            p.kategorie?.let { kategorieLabel(it).substringBefore(" ·") },
                            when {
                                mein -> "dir"
                                p.behandler != null -> p.behandler
                                else -> "frei"
                            },
                        ).joinToString(" · "),
                        an = aktuell.first.id == p.id,
                        farbe = p.kategorie?.let { kategorieFarbeSk(it) },
                        beiDruck = { gewaehlt = p.id },
                        unterFarbe = if (mein) Farben.AmberHell else Farben.TextSehrLeise,
                    )
                }
            }
        }
    }

    Kasten(innenraum = Abstand.Normal, abstandInnen = Abstand.Klein) {
        Bogenkopf(
            "PatSim" + if (patienten.size > 1) " · ${patienten.size} Patienten" else "",
            beiErweitern = { erweitert = true },
        )
        auswahl()
        if (!erweitert) {
            PatientBogen(einsatz, aktuell.first, meins, aktuell.second, gross = false, premium, server, befehle)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                SehrLeise("PatSim ist aufgeklappt.", mono = true)
                Textweg("zuklappen", { erweitert = false })
            }
        }
    }

    if (erweitert) {
        Vollblende(
            etikett = "PatSim · ${meins.funkrufname}",
            titel = "Patient ${aktuell.first.id}",
            beiSchliessen = { erweitert = false },
        ) {
            auswahl()
            PatientBogen(einsatz, aktuell.first, meins, aktuell.second, gross = true, premium, server, befehle)
        }
    }
}

private enum class Bogenseite(val titel: String) {
    Befunde("Befunde"),
    Gespraech("Gespräch"),
    Therapie("Therapie"),
    Diagnose("Diagnose"),
    Verlauf("Verlauf"),
}

/**
 * Der Bogen eines Patienten. Oben, was man fortwährend liest (Kopf, Monitor,
 * Kacheln, laufende Maßnahme), darunter, was man tut — in Reitern, und in der
 * großen Form alles untereinander.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.PatientBogen(
    einsatz: Einsatz,
    p: ManvPatient,
    meins: Rundenfahrzeug,
    zugewiesen: Boolean,
    gross: Boolean,
    premium: Boolean,
    server: String,
    befehle: Raumbefehle,
) {
    val jetzt = sekundentakt()
    var seite by remember(p.id) { mutableStateOf(Bogenseite.Befunde) }

    val nurErsteHilfe = meins.organisation != "Rettungsdienst"
    val binNotarzt = meins.faehigkeiten.any { it.equals("Notarzt", ignoreCase = true) }
    val platzId = if (binNotarzt) p.notarztVehicleId else p.behandlerVehicleId
    val platzName = if (binNotarzt) p.notarzt else p.behandler
    val meinPlatz = platzId == meins.id
    // Warum diese Besatzung hier nicht arbeiten kann — dieselben Sätze wie am Server.
    val belegtGrund = when {
        nurErsteHilfe || meinPlatz -> null
        !binNotarzt && p.transportVehicleId == meins.id -> null
        platzId != null -> "${p.id} wird schon von $platzName behandelt."
        einsatz.manv -> "Beim Massenanfall teilt die Einsatzleitung die Patienten zu."
        else -> null
    }
    val gesperrt = nurErsteHilfe || belegtGrund != null
    val sperrgrund = belegtGrund ?: NUR_RETTUNGSDIENST
    val kannNaNachfordern = !nurErsteHilfe && !binNotarzt && belegtGrund == null && p.notarzt == null

    val messwerte = messwerteVon(p).associateBy { it.was }
    val misst = p.misstGerade
    val messRest = restSekunden(p.messungFertigUm, jetzt)
    val messAnteil = messRest?.let { r -> MESSBAR.firstOrNull { it.id == misst }?.dauer?.takeIf { it > 0 }?.let { 1f - r.toFloat() / it } ?: 0.5f }

    val massnahmen = p.massnahmen.orEmpty()
    val laufende = massnahmen.firstOrNull { it.id == p.massnahmeLaeuft } ?: massnahmen.firstOrNull { it.laeuft }
    val behandlungRest = restSekunden(p.massnahmeFertigUm, jetzt)
    val gemessen = p.gemessen ?: messwerte.size
    val mindestens = p.mindestensGemessen ?: 3
    val genugGemessen = gemessen >= mindestens
    val schemaErhoben = p.befunde.orEmpty().any { it.vollstaendig }
    val genugUntersucht = genugGemessen && schemaErhoben
    val person = listOfNotNull(
        p.geschlecht,
        when (p.alter) {
            null -> null
            0 -> "Neugeborenes"
            else -> "${p.alter} J."
        },
    ).joinToString(", ")

    // ------------------------------------------------------------------ Kopf
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        ) {
            Etikett("Patient ${p.id}" + if (person.isNotEmpty()) " · $person" else "")
            p.kategorie?.let { Marke(kategorieLabel(it), farbe = kategorieFarbeSk(it)) }
            if (p.stabilisiert) Marke("stabil", farbe = Farben.GruenHell)
            if (zugewiesen) Marke("dir zugewiesen", farbe = Farben.AmberHell)
        }
        Text(
            when {
                p.diagnose != null -> "Diagnose: ${p.diagnose}"
                p.verdachtsdiagnose != null -> "Verdacht: ${p.verdachtsdiagnose}"
                else -> "Noch nicht eingeordnet"
            },
            style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold),
            color = Farben.Text,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        ) {
            Text(
                "Behandelt von ${p.behandler ?: "niemand"}" + (p.notarzt?.let { " · Notarzt $it" } ?: ""),
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
            if (kannNaNachfordern) {
                Knopf(
                    if (p.notarztNachgefordert) "NA nachgefordert" else "NA nachfordern",
                    { befehle.patientNotarztNachfordern(einsatz.id, p.id) },
                    aktiv = !p.notarztNachgefordert,
                    kompakt = true,
                )
            }
            if (!nurErsteHilfe && !einsatz.manv) {
                if (meinPlatz) {
                    Knopf("Abgeben", { befehle.patientUebernehmen(einsatz.id, p.id, true) }, art = Knopfart.Leise, kompakt = true)
                } else if (platzId == null) {
                    Knopf("Übernehmen", { befehle.patientUebernehmen(einsatz.id, p.id, false) }, kompakt = true)
                }
            }
        }
        belegtGrund?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }
    }

    // ---------------------------------------------------------------- Monitor
    val monitorEintrag = massnahmen.firstOrNull { it.id == "monitor" }
    Monitor(
        monitor = p.monitor,
        blutdruck = messwerte["Blutdruck"],
        nibpRest = if (misst == "Blutdruck") messRest else null,
        messungBelegt = misst != null || gesperrt,
        anschliessenGesperrt = when {
            gesperrt -> sperrgrund
            monitorEintrag == null -> "kein Monitor an Bord"
            monitorEintrag.moeglich -> null
            else -> monitorEintrag.grund ?: "geht gerade nicht"
        },
        schliesstAn = p.massnahmeLaeuft == "monitor",
        gross = gross,
        beiAnschliessen = { befehle.patientMassnahme(einsatz.id, p.id, "monitor") },
        beiNibp = { if (misst == null && !gesperrt) befehle.patientMessen(einsatz.id, p.id, "Blutdruck") },
    )

    // ----------------------------------------------------------------- Kacheln
    val kacheln = MESSBAR
        .filter { !(p.monitor != null && it.id in AM_MONITOR) }
        .map { it to messwerte[it.id] }
        .sortedBy { (_, w) -> if (w == null) 2 else if (w.auffaellig) 0 else 1 }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        maxItemsInEachRow = 3,
        modifier = Modifier.fillMaxWidth(),
    ) {
        kacheln.forEach { (m, w) ->
            val laeuftHier = misst == m.id && messRest != null
            val breit = m.id == "Ekg" && w?.kurve != null && !laeuftHier
            Wertkachel(
                name = m.kurz,
                zahl = when {
                    laeuftHier -> if ((messRest ?: 0) > 0) "$messRest s" else "…"
                    w != null -> zahlOhneEinheit(m.id, w.anzeige) + m.einheit
                    else -> "—"
                },
                ecke = if (w != null && !laeuftHier) alterText(w.um, jetzt).ifBlank { null } else null,
                fuss = when {
                    laeuftHier -> "misst …"
                    w == null -> if (m.dauer > 0) "${m.dauer} s" else "ansprechen"
                    m.id == "Ekg" -> null
                    w.trend != null && w.trend != "gleich" && w.vorher != null ->
                        "${TREND[w.trend] ?: ""} ${zahlOhneEinheit(m.id, w.vorher)}"
                    w.trend == "gleich" -> "→ unverändert"
                    else -> null
                },
                auffaellig = w?.auffaellig == true,
                leer = w == null && !laeuftHier,
                laeuft = if (laeuftHier) messAnteil else null,
                aktiv = !gesperrt && (misst == null || misst == m.id),
                beiDruck = { if (misst == null && !gesperrt) befehle.patientMessen(einsatz.id, p.id, m.id) },
                modifier = if (breit) Modifier.fillMaxWidth() else Modifier.weight(1f),
                inhalt = if (breit) {
                    { Ekgkurve(w!!.kurve!!, (w.frequenz ?: 0.0).toFloat(), sekunden = 3f, hoehe = 48.dp) }
                } else {
                    null
                },
            )
        }
    }

    // ------------------------------------------------------- laufende Maßnahme
    // Die Zeile steht immer da — sonst spränge der Bogen darunter bei jeder Maßnahme.
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier.fillMaxWidth().flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false).padding(Abstand.Klein),
    ) {
        if (laufende != null && behandlungRest != null) {
            Text("Läuft: ${laufende.name} · $behandlungRest s", style = Schrift.Klein, color = Farben.Text)
            Laufbalken(if (laufende.dauer > 0) 1f - behandlungRest.toFloat() / laufende.dauer else 0.5f)
        } else {
            SehrLeise("Keine Maßnahme läuft.")
        }
    }

    if (nurErsteHilfe) {
        Text(
            "Erste Hilfe. Messen, untersuchen und behandeln übernimmt der Rettungsdienst — " +
                "ihr rettet, lagert, stillt Blutungen und betreut.",
            style = Schrift.Klein,
            color = Farben.AmberHell,
        )
    }

    // ------------------------------------------------------------ Die Arbeit
    if (!gross) {
        Bogenreiter(Bogenseite.entries.map { it to it.titel }, seite, { seite = it })
    }
    val zeige = { s: Bogenseite -> gross || seite == s }

    if (zeige(Bogenseite.Gespraech)) {
        Gespraech(einsatz.id, p, premium, server, gross, befehle)
    }
    if (zeige(Bogenseite.Befunde)) {
        if (gross) Etikett("Befunde")
        Befunde(einsatz.id, p, messwerte["Ekg"], gesperrt, nurErsteHilfe, belegtGrund, jetzt, befehle)
    }
    if (zeige(Bogenseite.Therapie)) {
        if (gross) Etikett("Therapie")
        Therapie(einsatz.id, p, massnahmen, nurErsteHilfe, belegtGrund, sperrgrund, befehle)
    }
    if (zeige(Bogenseite.Diagnose)) {
        if (gross) Etikett("Diagnose")
        Diagnose(einsatz, p, gemessen, mindestens, genugGemessen, schemaErhoben, genugUntersucht, gesperrt, nurErsteHilfe, belegtGrund, binNotarzt, premium, messwerte.values.toList(), befehle)
    }
    if (zeige(Bogenseite.Verlauf)) {
        if (gross) Etikett("Verlauf")
        Verlaufsliste(p.verlaufsbogen.orEmpty(), jetzt)
    }
}

/** Der Monitor — Kurven links, Zahlen rechts; nicht angeschlossen eine Zeile und der Weg dorthin. */
@Composable
private fun Monitor(
    monitor: Patientenmonitor?,
    blutdruck: Messwert?,
    nibpRest: Int?,
    messungBelegt: Boolean,
    anschliessenGesperrt: String?,
    schliesstAn: Boolean,
    gross: Boolean,
    beiAnschliessen: () -> Unit,
    beiNibp: () -> Unit,
) {
    if (monitor == null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SehrLeise(if (schliesstAn) "Monitor wird angeschlossen …" else "Monitor nicht angeschlossen.", Modifier.weight(1f))
            if (!schliesstAn) Knopf("Anschließen", beiAnschliessen, aktiv = anschliessenGesperrt == null, kompakt = true)
        }
        return
    }
    val hf = monitor.herzfrequenz?.toInt()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.BgTief, Rundung.Klein)
            .border(1.dp, Farben.Rand, Rundung.Klein)
            .padding(Abstand.Klein),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            SehrLeise("II", mono = true)
            Ekgkurve(monitor.kurve, (hf ?: 0).toFloat(), sekunden = if (gross) 6f else 4f, hoehe = if (gross) 90.dp else 54.dp, farbe = Farben.GruenHell, laufend = true)
            SehrLeise("Pleth", mono = true)
            Ekgkurve(monitor.kurve, (hf ?: 0).toFloat(), sekunden = if (gross) 6f else 4f, hoehe = if (gross) 60.dp else 34.dp, farbe = Farben.SuchFarbe, laufend = true, pleth = monitor.pleth.toFloat())
        }
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.width(84.dp)) {
            Monitorwert("HF", hf?.toString() ?: "---", Farben.GruenHell, "Herzfrequenz" in monitor.alarme)
            Monitorwert("SpO₂", monitor.saettigung?.toInt()?.toString() ?: "--", Farben.SuchFarbe, "Saettigung" in monitor.alarme)
            Monitorwert("AF", monitor.atemfrequenz.toInt().toString(), Farben.AmberHell, "Atemfrequenz" in monitor.alarme)
            Monitorwert(
                "NIBP",
                when {
                    nibpRest != null -> if (nibpRest > 0) "$nibpRest s" else "…"
                    else -> blutdruck?.anzeige?.replace(Regex("""\s*mmHg$"""), "") ?: "--/--"
                },
                Farben.Text,
                false,
                beiDruck = if (!messungBelegt || nibpRest != null) beiNibp else null,
            )
        }
    }
}

@Composable
private fun Monitorwert(name: String, zahl: String, farbe: Color, alarm: Boolean, beiDruck: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (alarm) Farben.SignalTief else Color.Transparent, Rundung.Winzig)
            .then(if (beiDruck != null) Modifier.clickable(onClick = beiDruck, role = Role.Button) else Modifier)
            .padding(horizontal = Abstand.Winzig),
    ) {
        Text(name, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = farbe)
        Text(zahl, style = Schrift.Gross.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = farbe, maxLines = 1)
    }
}

// ------------------------------------------------------------------------ EKG

private data class Schlag(val t: Float, val ves: Boolean)

/** Wie stark die Zacken in einer Ableitung ausfallen (`Ableitung` in ekgKurven.ts). */
private data class Ableitung(val name: String, val p: Float, val r: Float, val s: Float, val t: Float, val st: Float)

private val MONITORABLEITUNG = Ableitung("II", 0.12f, 1.0f, 0.2f, 0.3f, 0.12f)
private val AUSDRUCK = listOf(
    Ableitung("II", 0.12f, 1.0f, 0.2f, 0.3f, 0.08f),
    Ableitung("V2", 0.06f, 0.5f, 0.9f, 0.45f, 0.45f),
    Ableitung("V5", 0.1f, 1.2f, 0.15f, 0.35f, 0.22f),
)

private fun glocke(t: Float, mitte: Float, breite: Float, hoehe: Float): Float {
    val x = (t - mitte) / breite
    return hoehe * exp(-x * x)
}

private fun wuerfel(n: Int): Float {
    val x = sin(n * 12.9898 + 78.233) * 43758.5453
    return (x - floor(x)).toFloat()
}

private fun schlaege(kurve: String, frequenz: Float, bis: Float): List<Schlag> {
    if (kurve == "vf" || kurve == "asystolie" || frequenz <= 0f) return emptyList()
    val rr = 60f / maxOf(20f, frequenz)
    val aus = mutableListOf<Schlag>()
    var t = 0.2f
    var i = 0
    while (t < bis + rr) {
        val ves = kurve == "ves" && i % 4 == 3
        aus += Schlag(t, ves)
        var abstand = rr
        if (kurve == "vhf") abstand = rr * (0.7f + wuerfel(i) * 0.6f)
        if (kurve == "ves" && i % 4 == 2) abstand = rr * 0.65f
        if (kurve == "ves" && i % 4 == 3) abstand = rr * 1.35f
        t += abstand
        i++
    }
    return aus
}

private fun ekgWert(kurve: String, t: Float, takt: List<Schlag>, a: Ableitung): Float {
    val rauschen = sin(t * 50f) * 0.008f + sin(t * 7.3f) * 0.006f
    if (kurve == "asystolie") return rauschen + sin(t * 0.8f) * 0.02f
    if (kurve == "vf") {
        val huelle = 0.35f + 0.2f * sin(t * 0.9f)
        val zwei = 2 * PI.toFloat()
        return huelle * (sin(t * zwei * 5.1f) + 0.6f * sin(t * zwei * 3.7f + 1.3f) + 0.3f * sin(t * zwei * 7.9f)) + rauschen
    }
    var y = rauschen
    if (kurve == "vhf") y += 0.04f * sin(t * 2 * PI.toFloat() * 6.3f) + 0.03f * sin(t * 2 * PI.toFloat() * 4.7f + 0.5f)
    for (s in takt) {
        val d = t - s.t
        if (d < -0.4f || d > 0.6f) continue
        if (s.ves) {
            y += glocke(d, 0f, 0.045f, a.r * 1.3f) + glocke(d, 0.08f, 0.04f, -a.s * 1.5f) + glocke(d, 0.3f, 0.07f, -a.t * 1.2f)
            continue
        }
        val pAbstand = if (kurve == "avb") 0.34f else 0.18f
        if (kurve != "vhf") y += glocke(d, -pAbstand, 0.03f, a.p)
        y += glocke(d, -0.03f, 0.009f, -0.08f) + glocke(d, 0f, 0.011f, a.r) + glocke(d, 0.03f, 0.012f, -a.s)
        y += glocke(d, 0.26f, 0.055f, a.t)
        if (kurve == "stemi") y += a.st * (1f / (1f + exp(-(d - 0.045f) * 90f))) * (1f / (1f + exp((d - 0.3f) * 30f)))
    }
    return y
}

private fun plethWert(t: Float, takt: List<Schlag>, hoehe: Float): Float {
    var y = 0f
    for (s in takt) {
        val d = t - s.t - 0.22f
        if (d < 0f || d > 0.9f) continue
        val staerke = if (s.ves) 0.35f else 1f
        y += staerke * hoehe * (d / 0.12f) * exp(1f - d / 0.12f) * 0.9f + staerke * hoehe * glocke(d, 0.38f, 0.06f, 0.18f)
    }
    return y
}

/**
 * Eine Kurve — EKG oder Pulswelle, gerechnet wie in `ekgKurven.ts`. `laufend` lässt
 * sie wie am Monitor durchs Bild wandern; der Ausdruck steht still auf Raster.
 */
@Composable
private fun Ekgkurve(
    kurve: String,
    frequenz: Float,
    sekunden: Float,
    hoehe: androidx.compose.ui.unit.Dp,
    farbe: Color = Farben.GruenHell,
    laufend: Boolean = false,
    pleth: Float? = null,
    ableitung: Ableitung = MONITORABLEITUNG,
    raster: Boolean = false,
) {
    val versatz = if (laufend) {
        val lauf = rememberInfiniteTransition(label = "ekg")
        lauf.animateFloat(0f, 60f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "ekg-zeit").value
    } else {
        0f
    }
    val takt = remember(kurve, frequenz) { schlaege(kurve, frequenz, 64f) }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(hoehe)
            .then(if (raster) Modifier.background(Color(0xFFFFF4F1), Rundung.Winzig) else Modifier),
    ) {
        if (raster) {
            val schritt = size.width / (sekunden * 5f)
            var x = 0f
            while (x <= size.width) {
                drawLine(Color(0xFFF3B6AE), Offset(x, 0f), Offset(x, size.height), 1f)
                x += schritt
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(Color(0xFFF3B6AE), Offset(0f, y), Offset(size.width, y), 1f)
                y += schritt
            }
        }
        val punkte = 240
        val pfad = Path()
        val mitte = size.height * if (pleth != null) 0.85f else 0.6f
        val mass = size.height * if (pleth != null) 0.7f else 0.45f
        for (i in 0..punkte) {
            val t = versatz + sekunden * i / punkte
            val wert = if (pleth != null) plethWert(t, takt, (pleth / 100f).coerceIn(0.2f, 1f)) else ekgWert(kurve, t, takt, ableitung)
            val x = size.width * i / punkte
            val y = mitte - wert * mass
            if (i == 0) pfad.moveTo(x, y) else pfad.lineTo(x, y)
        }
        drawPath(pfad, if (raster) Color(0xFF1A1A1A) else farbe, style = Stroke(width = 1.6.dp.toPx()))
    }
}

// --------------------------------------------------------------------- Felder

@Composable
private fun ColumnScope.Gespraech(einsatzId: String, p: ManvPatient, premium: Boolean, server: String, gross: Boolean, befehle: Raumbefehle) {
    val browser = LocalUriHandler.current
    val bereich = rememberCoroutineScope()
    var frage by remember(p.id) { mutableStateOf("") }
    var fragt by remember(p.id) { mutableStateOf(false) }
    var fehler by remember(p.id) { mutableStateOf<String?>(null) }

    fun fragen(text: String, ausFeld: Boolean) {
        val inhalt = text.trim()
        if (inhalt.isEmpty() || fragt) return
        fragt = true
        fehler = null
        bereich.launch {
            val e = befehle.patientFragen(einsatzId, p.id, inhalt)
            if (e.fehler != null) fehler = e.fehler else if (ausFeld) frage = ""
            fragt = false
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        if (gross) Etikett("Gespräch")
        Marke("KI · Premium", farbe = Farben.ViolettHell)
    }
    val gespraech = p.gespraech.orEmpty()
    if (gespraech.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            gespraech.forEach { a ->
                Column {
                    Text("${a.von}: ${a.frage}", style = Schrift.Klein, color = Farben.TextLeise)
                    Text("„${a.antwort}“", style = Schrift.Normal, color = Farben.Text)
                }
            }
        }
    } else if (premium) {
        SehrLeise("Frag den Patienten selbst — er antwortet so, wie es ihm gerade geht.")
    }
    if (premium) {
        Pillenreihe {
            SCHNELLFRAGEN.forEach { f -> Pille(f, an = false, aktiv = !fragt, beiDruck = { fragen(f, false) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
            Feld(
                wert = frage,
                beiAenderung = { frage = it.take(200) },
                platzhalter = "Frage an den Patienten …",
                weiterTaste = ImeAction.Send,
                aktiv = !fragt,
                modifier = Modifier.weight(1f),
            )
            Knopf(if (fragt) "Hört zu …" else "Fragen", { fragen(frage, true) }, aktiv = !fragt && frage.isNotBlank(), kompakt = true)
        }
    } else {
        SehrLeise("Mit dem Patienten frei sprechen und nach der Verdachtsdiagnose eine Fallbesprechung bekommen —")
        // Premium wird nur auf der Webseite abgeschlossen.
        Textweg("mit Premium", { browser.openUri("${server.trimEnd('/')}/play/mobile/shop?bereich=premium") })
    }
    fehler?.let { Text(it, style = Schrift.Klein.copy(fontFamily = Schrift.Mono), color = Farben.SignalHell) }
}

@Composable
private fun ColumnScope.Befunde(
    einsatzId: String,
    p: ManvPatient,
    ekg: Messwert?,
    gesperrt: Boolean,
    nurErsteHilfe: Boolean,
    belegtGrund: String?,
    jetzt: Long,
    befehle: Raumbefehle,
) {
    // Der Ausdruck des 12-Kanal-EKGs — drei Ableitungen auf Millimeterpapier.
    if (ekg?.kurve != null) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth().flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false).padding(Abstand.Klein),
        ) {
            Row {
                Text("12-Kanal-EKG", style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                SehrLeise(alterText(ekg.um, jetzt))
            }
            AUSDRUCK.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    Text(a.name, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise, modifier = Modifier.width(24.dp))
                    Box(Modifier.weight(1f)) {
                        Ekgkurve(ekg.kurve, (ekg.frequenz ?: 0.0).toFloat(), sekunden = 4f, hoehe = 44.dp, ableitung = a, raster = true)
                    }
                }
            }
            Text("Gerätedeutung: ${ekg.anzeige}", style = Schrift.Klein, color = Farben.TextLeise)
        }
    }

    val befunde = p.befunde.orEmpty()
    val laufendes = befunde.firstOrNull { !it.vollstaendig }
    val schrittRest = restSekunden(laufendes?.naechsterFertigUm, jetzt)
    SCHEMATA.forEach { (id, name, wofuer) ->
        val b: Befundschema? = befunde.firstOrNull { it.schema == id }
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier
                .fillMaxWidth()
                .flaeche(
                    farbe = Farben.FlaecheHoch,
                    randfarbe = if (laufendes?.schema == id) Farben.Amber else Farben.Rand,
                    ecke = 9.dp,
                    mitLichtkante = false,
                )
                .then(
                    if (b == null && laufendes == null && !gesperrt) {
                        Modifier.clickable(role = Role.Button) { befehle.patientSchema(einsatzId, p.id, id) }
                    } else {
                        Modifier
                    },
                )
                .padding(Abstand.Klein),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(name, style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Text)
                Text(wofuer, style = Schrift.Winzig, color = Farben.TextSehrLeise, modifier = Modifier.weight(1f))
                if (b == null) {
                    Text(
                        when {
                            nurErsteHilfe -> "Rettungsdienst"
                            belegtGrund != null -> "belegt"
                            laufendes != null -> "nach dem laufenden"
                            else -> "beginnen"
                        },
                        style = Schrift.Klein,
                        color = if (!gesperrt && laufendes == null) Farben.Amber else Farben.TextSehrLeise,
                    )
                }
            }
            if (b != null) {
                b.punkte.forEach { pt ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text(pt.schluessel, style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = if (pt.befund.isNotBlank()) Farben.SignalHell else Farben.TextSehrLeise, modifier = Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pt.frage, style = Schrift.Winzig, color = Farben.TextSehrLeise)
                            Text(pt.befund.ifBlank { "unauffällig" }, style = Schrift.Klein, color = Farben.Text)
                        }
                    }
                }
                val naechster = b.naechster
                if (naechster != null && schrittRest != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text(naechster.schluessel, style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Amber, modifier = Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(naechster.frage, style = Schrift.Winzig, color = Farben.TextSehrLeise)
                            Text(if (schrittRest > 0) "wird erhoben … $schrittRest s" else "gleich …", style = Schrift.Klein.copy(fontFamily = Schrift.Mono), color = Farben.AmberHell)
                        }
                    }
                } else if (!b.offen.isNullOrEmpty()) {
                    SehrLeise("Offen: ${b.offen.joinToString(" ")}", mono = true)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.Therapie(
    einsatzId: String,
    p: ManvPatient,
    massnahmen: List<Patientenmassnahme>,
    nurErsteHilfe: Boolean,
    belegtGrund: String?,
    sperrgrund: String,
    befehle: Raumbefehle,
) {
    var suche by remember(p.id) { mutableStateOf("") }
    var gruppeGewaehlt by remember(p.id) { mutableStateOf<String?>(null) }
    val alle = Massnahmenstand.alle

    val darf = { m: Patientenmassnahme -> belegtGrund == null && (!nurErsteHilfe || m.ersteHilfe) }
    val sichtbar = massnahmen.filter { m ->
        when {
            nurErsteHilfe && !m.ersteHilfe -> m.erledigt || m.laeuft
            nurErsteHilfe && !alle -> true
            !alle -> m.vorgeschlagen || m.erledigt || m.laeuft
            gruppeGewaehlt != null && m.gruppe.ifBlank { "Weitere" } != gruppeGewaehlt -> false
            else -> suche.isBlank() || m.name.contains(suche.trim(), ignoreCase = true)
        }
    }
    val alleGruppen = massnahmen.filter { !nurErsteHilfe || it.ersteHilfe }.map { it.gruppe.ifBlank { "Weitere" } }.distinct()

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
        if (nurErsteHilfe) {
            Marke("Erste Hilfe")
            SehrLeise("was ihr ohne Rettungsdienst tun dürft")
        } else {
            Marke(if (alle) "Erweitert" else "Einfach")
            SehrLeise(
                (if (alle) "alle ${massnahmen.size} Maßnahmen" else "vorgeschlagene Maßnahmen") +
                    " · umstellen unter Konto → Spiel & Bedienung",
            )
        }
    }
    if (alle && !nurErsteHilfe) {
        Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Maßnahme suchen …")
        Pillenreihe {
            Pille("Alle", an = gruppeGewaehlt == null, beiDruck = { gruppeGewaehlt = null })
            alleGruppen.forEach { g -> Pille(g, an = gruppeGewaehlt == g, beiDruck = { gruppeGewaehlt = if (gruppeGewaehlt == g) null else g }) }
        }
    }
    if (sichtbar.isEmpty()) SehrLeise("Keine Maßnahme passt zur Suche.")

    sichtbar.groupBy { it.gruppe.ifBlank { "Weitere" } }.forEach { (gruppe, liste) ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Etikett(gruppe)
            SehrLeise(liste.size.toString(), mono = true)
        }
        liste.forEach { m ->
            val fertig = m.erledigt && !m.wiederholbar
            val gesperrtHier = (!m.moeglich || !darf(m)) && !m.erledigt && !m.laeuft
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(
                        farbe = Farben.FlaecheHoch.copy(alpha = if (gesperrtHier || fertig) 0.5f else 1f),
                        randfarbe = if (m.laeuft) Farben.Amber else Farben.Rand,
                        ecke = 9.dp,
                        mitLichtkante = false,
                    )
                    .padding(Abstand.Klein),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                    Text(m.name, style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold), color = if (fertig) Farben.TextSehrLeise else Farben.Text)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                        Text("${m.dauer} s", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                        m.voraussetzungen.forEach { v -> Text(v, style = Schrift.Winzig, color = Farben.AmberHell) }
                        if (m.brauchtArzt) Text("Notarzt", style = Schrift.Winzig, color = Farben.AmberHell)
                        if (m.anzahl > 1) Text("${m.anzahl}×", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
                    }
                    // Der Grund steht sichtbar da — nicht erst nach dem Drücken.
                    when {
                        !darf(m) && !m.laeuft && !fertig -> Text(sperrgrund, style = Schrift.Winzig, color = Farben.AmberHell)
                        !m.moeglich && !m.laeuft && m.grund != null && !fertig -> Text(m.grund, style = Schrift.Winzig, color = Farben.AmberHell)
                    }
                }
                when {
                    m.laeuft -> Text("läuft", style = Schrift.Klein.copy(fontFamily = Schrift.Mono), color = Farben.Amber)
                    fertig -> Text("✓", style = Schrift.Gross, color = Farben.GruenHell)
                    else -> Knopf(
                        if (m.erledigt) "Erneut" else "Beginnen",
                        { befehle.patientMassnahme(einsatzId, p.id, m.id) },
                        aktiv = m.moeglich && darf(m),
                        kompakt = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Diagnose(
    einsatz: Einsatz,
    p: ManvPatient,
    gemessen: Int,
    mindestens: Int,
    genugGemessen: Boolean,
    schemaErhoben: Boolean,
    genugUntersucht: Boolean,
    gesperrt: Boolean,
    nurErsteHilfe: Boolean,
    belegtGrund: String?,
    hatNotarzt: Boolean,
    premium: Boolean,
    messwerte: List<Messwert>,
    befehle: Raumbefehle,
) {
    val bereich = rememberCoroutineScope()
    var verdacht by remember(p.id) { mutableStateOf("") }
    var aendern by remember(p.id) { mutableStateOf(false) }
    var diagnose by remember(p.id) { mutableStateOf("") }
    var besprechung by remember(p.id) { mutableStateOf<String?>(null) }
    var bespricht by remember(p.id) { mutableStateOf(false) }
    var kiFehler by remember(p.id) { mutableStateOf<String?>(null) }

    Pruefzeile(genugGemessen, if (genugGemessen) "$gemessen Messwerte" else "Messwerte $gemessen von $mindestens")
    Pruefzeile(schemaErhoben, "ein Schema erhoben")

    Etikett("Verdachtsdiagnose")
    when {
        p.verdachtsdiagnose != null && !aendern -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.verdachtsdiagnose, style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f))
            if (!gesperrt) Knopf("Ändern", { aendern = true }, art = Knopfart.Leise, kompakt = true)
        }
        gesperrt -> SehrLeise(if (nurErsteHilfe) "Den Verdacht stellt der Rettungsdienst." else belegtGrund.orEmpty())
        else -> Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
            Feld(
                wert = verdacht,
                beiAenderung = { verdacht = it.take(120) },
                platzhalter = "Was vermutest du?",
                aktiv = genugUntersucht,
                weiterTaste = ImeAction.Done,
                modifier = Modifier.weight(1f),
            )
            Knopf("Festhalten", {
                befehle.patientVerdacht(einsatz.id, p.id, verdacht)
                verdacht = ""
                aendern = false
            }, aktiv = genugUntersucht && verdacht.isNotBlank(), kompakt = true)
        }
    }

    // Die Fallbesprechung (Premium) — erst nach dem Verdacht.
    if (premium && p.verdachtsdiagnose != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Etikett("Fallbesprechung")
            Marke("KI · Premium", farbe = Farben.ViolettHell)
        }
        Row {
            Knopf(
                when {
                    bespricht -> "Wird besprochen …"
                    besprechung != null -> "Neu besprechen"
                    else -> "Fall besprechen"
                },
                {
                    bespricht = true
                    kiFehler = null
                    bereich.launch {
                        val e = befehle.patientBesprechen(einsatz.id, p.id)
                        if (e.fehler != null) kiFehler = e.fehler else besprechung = e.text
                        bespricht = false
                    }
                },
                aktiv = !bespricht,
                kompakt = true,
            )
        }
        besprechung?.let { Text(it, style = Schrift.Klein, color = Farben.Text) }
        kiFehler?.let { Text(it, style = Schrift.Klein.copy(fontFamily = Schrift.Mono), color = Farben.SignalHell) }
    }

    // Die Übergabe nach MIST — zum Vorlesen an der Klinik oder beim Notarzt.
    Etikett("Übergabe (MIST)")
    val person = listOfNotNull(p.geschlecht, p.alter?.let { if (it == 0) "Neugeborenes" else "$it J." }).joinToString(", ")
    val kopf = listOfNotNull(person.ifBlank { null }, p.verdachtsdiagnose?.let { "Verdacht $it" }).joinToString(" · ")
    if (kopf.isNotBlank()) Text(kopf, style = Schrift.Klein, color = Farben.Text)
    val befunde = p.befunde.orEmpty().flatMap { b -> b.punkte.filter { it.befund.isNotBlank() }.map { "${it.schluessel}: ${it.befund}" } }
    val werte = messwerte.filter { it.was != "Ekg" }.map { "${it.name} ${it.anzeige}" } +
        listOfNotNull(messwerte.firstOrNull { it.was == "Ekg" }?.let { "EKG ${it.anzeige}" })
    val therapie = p.massnahmen.orEmpty().filter { it.erledigt }.map { if (it.anzahl > 1) "${it.name} (${it.anzahl}×)" else it.name }
    listOf(
        Triple("M", "Mechanismus", einsatz.meldebild.ifBlank { "—" }),
        Triple("I", "Befunde", befunde.joinToString("; ").ifBlank { "nichts Auffälliges erhoben" }),
        Triple("S", "Werte", werte.joinToString(", ").ifBlank { "nichts gemessen" }),
        Triple("T", "Therapie", therapie.joinToString(", ").ifBlank { "keine" }),
    ).forEach { (b, was, text) ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text(b, style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Amber, modifier = Modifier.width(16.dp))
            Text("$was: $text", style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
        }
    }

    // Die Diagnose gehört dem Notarzt.
    Etikett("Diagnose (Notarzt)")
    if (p.diagnose != null) {
        Text(p.diagnose, style = Schrift.Normal, color = Farben.Text)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
            Feld(
                wert = diagnose,
                beiAenderung = { diagnose = it.take(120) },
                platzhalter = "Eigener Wortlaut, leer: Klartext des Bildes",
                aktiv = hatNotarzt && genugUntersucht,
                weiterTaste = ImeAction.Done,
                modifier = Modifier.weight(1f),
            )
            Knopf("Stellen", {
                befehle.patientDiagnose(einsatz.id, p.id, diagnose)
                diagnose = ""
            }, aktiv = hatNotarzt && genugUntersucht, kompakt = true)
        }
        if (!hatNotarzt && !nurErsteHilfe) SehrLeise("Die Diagnose stellt der Notarzt — dieses Fahrzeug hat keinen an Bord.")
    }
}

@Composable
private fun Verlaufsliste(verlauf: List<Patientenvermerk>, jetzt: Long) {
    if (verlauf.isEmpty()) {
        SehrLeise("Noch nichts geschehen.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        verlauf.forEach { v ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(alterText(v.um, jetzt), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise, modifier = Modifier.width(44.dp))
                Text(v.text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                v.von?.let { Text(it, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise) }
            }
        }
    }
}
