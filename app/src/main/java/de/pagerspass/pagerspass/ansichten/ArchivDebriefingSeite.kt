package de.pagerspass.pagerspass.ansichten

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.netz.ArchivAnfahrt
import de.pagerspass.pagerspass.netz.ArchivBefund
import de.pagerspass.pagerspass.netz.ArchivEinsatz
import de.pagerspass.pagerspass.netz.ArchivRunde
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Funkzeile
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.karte.Lagekarte
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Die Nachbesprechung einer archivierten Schicht — aufgerufen aus dem Dienstbuch.
 *
 * Übertragen aus `views/DebriefingView.vue` in dem Zweig, den das Web für eine
 * gereichte `runde` nimmt: dieselbe Auswertung, dieselbe Darstellung, nur die
 * Quelle ist das Archiv (`GET /api/archiv/{code}`). Anders als in der frisch
 * beendeten Runde steht hier die <b>ganze</b> Schicht: das vollständige
 * Funkprotokoll, das Anrufjournal, und das Replay im Zeitraffer.
 *
 * <b>Die Gutschrift fehlt mit Absicht.</b> Sie gehört zur gerade gefahrenen
 * Schicht, nicht zu einer archivierten — beim Blättern im Archiv wäre sie eine Zahl
 * aus einem anderen Abend. Was die Schicht gebracht hat, steht an ihrer Zeile im
 * Dienstbuch.
 */
@Composable
fun ArchivDebriefingSeite(
    unterrand: Dp,
    code: String,
    konto: Konto?,
    stand: Bereichsstand<ArchivRunde>,
    freunde: List<Freund>,
    beiLaden: () -> Unit,
    beiZurueck: () -> Unit,
    beiAnfragen: suspend (String) -> Result<Unit>,
    beiPremium: () -> Unit,
) {
    LaunchedEffect(code) { beiLaden() }
    var mottowerbung by remember { mutableStateOf(false) }

    Seite(unterrand = unterrand) {
        Knopf("← Zurück zu den Schichten", beiZurueck, art = Knopfart.Leise, kompakt = true)

        val runde = stand.inhalt?.takeIf { it.code.equals(code, ignoreCase = true) }
        if (runde == null) {
            val fehler = stand.fehler
            if (fehler != null) Fehlerzeile(fehler, beiLaden) else Ladezeile("Die Schicht wird geholt …")
            return@Seite
        }

        Nachbesprechung(
            runde = runde,
            konto = konto,
            freunde = freunde,
            beiAnfragen = beiAnfragen,
            beiMottowerbung = { mottowerbung = true },
        )
    }

    if (mottowerbung) {
        Abosperre(
            titel = "Deine Zeile auf der Schichtkarte",
            satz = "Unter dem Namen der Leitstelle steht mit Premium ein Satz, den du selbst " +
                "schreibst — auf jedem Bild, das du nach der Schicht teilst.",
            punkte = listOf(
                "Dein Motto bleibt stehen und gilt für jede weitere Schicht.",
                "Dazu die Abo-Gestaltungen der Karte — oben mit einem Stern gekennzeichnet.",
                "Die Kennzahlen darauf sind dieselben wie ohne Abo. Geschönt wird nichts.",
            ),
            beiSchliessen = { mottowerbung = false },
            beiPremium = {
                mottowerbung = false
                beiPremium()
            },
        )
    }
}

/** Die Wörter der Schweregrade — Klartext statt Punktzahl. */
private fun gradname(grad: String) = when (grad) {
    "Lob" -> "Gut gelaufen"
    "Hinweis" -> "Zum Nachdenken"
    "Mangel" -> "Fachlich daneben"
    else -> grad
}

private fun gradfarbe(grad: String) = when (grad) {
    "Lob" -> Farben.GruenHell
    "Mangel" -> Farben.SignalHell
    else -> Farben.AmberHell
}

/** Zuerst das, was fehlte — das Lob steht am Ende und bleibt im Kopf. */
private fun sortiert(befunde: List<ArchivBefund>): List<ArchivBefund> {
    val folge = listOf("Mangel", "Hinweis", "Lob")
    return befunde.sortedBy { folge.indexOf(it.grad).let { i -> if (i < 0) folge.size else i } }
}

private fun ausgangname(ausgang: String) = when (ausgang) {
    "EinsatzAngelegt" -> "Einsatz angelegt"
    "Verworfen" -> "Verworfen"
    "Verpasst" -> "Verpasst"
    "Abgewiesen" -> "Abgewiesen"
    "Zugeordnet" -> "Zu laufendem Einsatz"
    "Presse" -> "Presse"
    else -> ausgang
}

private fun schnitt(werte: List<Double?>): Double? {
    val gueltig = werte.filterNotNull()
    return if (gueltig.isEmpty()) null else gueltig.average()
}

/** „2 h 14 min" oder „38 min" — die Dienstdauer im Kopf. */
private fun dienstdauer(runde: ArchivRunde): String {
    val von = zeitVon(runde.gestartetUm) ?: return "—"
    val bis = zeitVon(runde.beendetUm) ?: return "—"
    val min = ((bis.toEpochMilli() - von.toEpochMilli()) / 1000 / 60).toInt()
    return if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
}

@Composable
private fun Nachbesprechung(
    runde: ArchivRunde,
    konto: Konto?,
    freunde: List<Freund>,
    beiAnfragen: suspend (String) -> Result<Unit>,
    beiMottowerbung: () -> Unit,
) {
    val zusammenhang = LocalContext.current
    val einsaetze = runde.incidents.sortedBy { it.eingangUm }
    val protokoll = runde.funkprotokoll
    val abgearbeitet = einsaetze.count { it.state == "Abgeschlossen" }
    val disposition = schnitt(einsaetze.map { it.dispositionszeitSekunden })
    val hilfsfrist = schnitt(einsaetze.map { it.hilfsfristSekunden })
    val leitstelle = runde.settings.leitstelle ?: "Leitstelle"
    var karteOffen by remember { mutableStateOf(false) }
    var sicherungsfehler by remember { mutableStateOf<String?>(null) }

    // Das Funkprotokoll als Textdatei — aus dem Archiv, also die ganze Schicht.
    val sichern = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { ziel ->
        if (ziel == null) return@rememberLauncherForActivityResult
        sicherungsfehler = runCatching {
            zusammenhang.contentResolver.openOutputStream(ziel)?.use { strom ->
                strom.write(protokollAlsText(protokoll, leitstelle, runde).toByteArray(Charsets.UTF_8))
            } ?: error("Die Datei ließ sich nicht schreiben.")
        }.exceptionOrNull()?.let {
            "Das vollständige Protokoll ließ sich nicht sichern: ${it.message}"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        Etikett("Nachbesprechung")
        Text("Vergangene Schicht", style = Schrift.Schlagzeile, color = Farben.Text)
        Text(
            "$leitstelle · Raum ${runde.code} · Dienstdauer ${dienstdauer(runde)}",
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
    }

    Knopf(
        aufschrift = if (karteOffen) "Schichtkarte schließen" else "Schichtkarte",
        beiDruck = { karteOffen = !karteOffen },
        art = Knopfart.Leise,
        kompakt = true,
    )

    if (karteOffen) {
        val beendet = runde.beendetUm ?: runde.gestartetUm
        Schichtkartenbereich(
            daten = Schichtkartendaten(
                leitstelle = leitstelle,
                code = runde.code,
                datum = tag(beendet),
                dienstdauer = dienstdauer(runde),
                einsaetze = einsaetze.size,
                abgeschlossen = abgearbeitet,
                disposition = dauerText(disposition),
                hilfsfrist = dauerText(hilfsfrist),
                funksprueche = protokoll.size,
            ),
            stufe = konto?.level ?: 1,
            premiumAktiv = konto?.premiumAktiv == true,
            beiMottowerbung = beiMottowerbung,
        )
    }

    Kachelpaar {
        Kennzahlkachel("Einsätze", einsaetze.size.toString(), "$abgearbeitet abgeschlossen", Modifier.weight(1f))
        Kennzahlkachel(
            "Ø Dispositionszeit", dauerText(disposition), "Notruf bis Alarm", Modifier.weight(1f),
            farbe = Farben.Blau,
        )
    }
    Kachelpaar {
        Kennzahlkachel(
            "Ø Hilfsfrist", dauerText(hilfsfrist), "Alarm bis Eintreffen", Modifier.weight(1f),
            farbe = Farben.Gruen,
        )
        Kennzahlkachel("Funksprüche", protokoll.size.toString(), "im Protokoll", Modifier.weight(1f))
    }

    Replay(runde)

    val schichtbefunde = sortiert(runde.auswertung.befunde.filter { it.incidentId == null })
    if (schichtbefunde.isNotEmpty()) {
        Ueberschrift("Zur Schicht")
        schichtbefunde.forEach { Befundzeile(it) }
    }

    // Erst hier wird ausgesprochen, was während der Schicht niemand erfahren
    // durfte: welche Lage mehrfach gemeldet wurde und ob die Zuordnung gepasst hat.
    val doppel = runde.auswertung.doppelmeldungen
    if (doppel != null && doppel.nennenswert) {
        Ueberschrift("Doppelmeldungen")
        Kachelpaar {
            Kennzahlkachel(
                "Mehrfach gemeldet", doppel.mehrfachGemeldeteLagen.toString(),
                "Lagen mit mehr als einem Anrufer", Modifier.weight(1f),
            )
            Kennzahlkachel(
                "Zusammengeführt", doppel.zusammengefuehrt.toString(),
                "Anrufe am richtigen Einsatz", Modifier.weight(1f), farbe = Farben.Gruen,
            )
        }
        Kachelpaar {
            Kennzahlkachel(
                "Doppelt alarmiert", doppel.doppelalarmierungen.toString(),
                "zweiter Einsatz zur selben Lage", Modifier.weight(1f), farbe = Farben.Signal,
            )
            Kennzahlkachel(
                "Falsch zugeordnet", doppel.falschzugeordnet.toString(),
                "gemeldete Lage ohne Einsatz", Modifier.weight(1f), farbe = Farben.Signal,
            )
        }
    }

    val bilanzen = runde.auswertung.spieler
    if (bilanzen.isNotEmpty()) {
        Ueberschrift("Wer was getan hat")
        bilanzen.forEach { b ->
            val spieler = runde.players.firstOrNull { it.id == b.playerId }
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Kontoname(
                        name = b.name,
                        premium = spieler?.premium == true,
                        teammitglied = spieler?.teammitglied == true,
                        modifier = Modifier.weight(1f),
                    )
                    if (b.rolle == "Leitstelle") SehrLeise("Leitstelle")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                    Wertzelle("Einsätze", b.einsaetze.toString())
                    Wertzelle("Lagemeldungen", b.lagemeldungen.toString())
                    Wertzelle("Funksprüche", b.funksprueche.toString())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                    Wertzelle("Ø Ausrückzeit", dauerText(b.ausrueckzeitSekunden))
                    Wertzelle("Status 6", if (b.ausserDienstSekunden > 0) dauerText(b.ausserDienstSekunden) else "—")
                }
            }
        }
    }

    Mannschaft(runde, konto?.kennung, freunde, beiAnfragen)

    Ueberschrift("Einsätze")
    if (einsaetze.isEmpty()) Leerhinweis("In dieser Schicht ist nichts passiert.")
    einsaetze.forEach { e ->
        Einsatzkarte(
            e = e,
            achse = runde.auswertung.zeitachsen.firstOrNull { it.incidentId == e.id },
            befunde = sortiert(runde.auswertung.befunde.filter { it.incidentId == e.id }),
        )
    }

    // Das Anrufjournal: jedes Telefongespräch der Schicht, jüngste zuerst.
    val journal = runde.anrufjournal.reversed()
    if (journal.isNotEmpty()) {
        Ueberschrift("Anrufjournal")
        Anrufjournal(journal, runde.incidents)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Ueberschrift("Funkprotokoll", Modifier.weight(1f))
        Knopf(
            aufschrift = "Als Textdatei sichern",
            beiDruck = {
                sicherungsfehler = null
                sichern.launch("funkprotokoll-${runde.code}.txt")
            },
            art = Knopfart.Leise,
            kompakt = true,
        )
    }
    sicherungsfehler?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        if (protokoll.isEmpty()) SehrLeise("Kein Funkspruch in dieser Schicht.")
        protokoll.forEach { Protokollzeile(it) }
    }
}

@Composable
private fun Wertzelle(was: String, wert: String) {
    Column(modifier = Modifier.widthIn(min = 92.dp)) {
        Text(wert, style = Schrift.MonoKlein, color = Farben.Text)
        Text(was, style = Schrift.Winzig, color = Farben.TextSehrLeise)
    }
}

@Composable
private fun Befundzeile(b: ArchivBefund, eng: Boolean = false) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 9.dp, randfarbe = gradfarbe(b.grad).copy(alpha = 0.5f))
            .padding(if (eng) Abstand.Klein else Abstand.Normal),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text(
                gradname(b.grad),
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                color = gradfarbe(b.grad),
            )
            Text(b.titel, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        }
        Text(b.text, style = Schrift.Klein, color = Farben.TextLeise)
    }
}

/** Menschliche Mitspieler dieser Schicht, ohne einen selbst — mit „+ Freund". */
@Composable
private fun Mannschaft(
    runde: ArchivRunde,
    eigene: String?,
    freunde: List<Freund>,
    beiAnfragen: suspend (String) -> Result<Unit>,
) {
    val mitspieler = runde.players.filter { !it.istBot && it.id != eigene }
    if (mitspieler.isEmpty()) return
    val bereich = rememberCoroutineScope()
    var angefragt by remember { mutableStateOf(emptySet<String>()) }
    var meldung by remember { mutableStateOf<String?>(null) }

    fun schonBefreundet(kennung: String): Boolean =
        freunde.firstOrNull { it.kennung == kennung }?.stand
            ?.let { it in setOf("Bestaetigt", "Angefragt", "Blockiert") } == true

    Ueberschrift("Mannschaft")
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        mitspieler.forEach { p ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Kontoname(name = p.name, premium = p.premium, teammitglied = p.teammitglied)
                    if (p.rang.isNotBlank()) SehrLeise(p.rang)
                }
                if (eigene != null && !schonBefreundet(p.id)) {
                    val schon = p.id in angefragt
                    Knopf(
                        aufschrift = if (schon) "Angefragt" else "+ Freund",
                        beiDruck = {
                            meldung = null
                            bereich.launch {
                                beiAnfragen(p.id)
                                    .onSuccess { angefragt = angefragt + p.id }
                                    .onFailure { meldung = it.message ?: "Das hat gerade nicht geklappt." }
                            }
                        },
                        art = Knopfart.Leise,
                        aktiv = !schon,
                        kompakt = true,
                    )
                }
            }
        }
    }
    meldung?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
}

/** Ein Einsatz der Schicht: Kopf, Zeiten, Adresse, Ablauf, Befunde, Chronologie. */
@Composable
private fun Einsatzkarte(
    e: ArchivEinsatz,
    achse: de.pagerspass.pagerspass.netz.ArchivZeitachse?,
    befunde: List<ArchivBefund>,
) {
    val farbe = orgTon(e.organisation)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = farbe.copy(alpha = 0.6f))
            .padding(Abstand.Normal),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), verticalAlignment = Alignment.Top) {
            Text(
                e.stichwort,
                style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                color = farbe,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(e.stichwortText, style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text)
                SehrLeise(
                    "${e.einsatznummer} · ${orgName(e.organisation)} · ${buchUhrzeit(e.eingangUm)} · " +
                        einsatzZustand(e.state),
                    mono = true,
                )
            }
        }
        Text(
            "Disp. ${dauerText(e.dispositionszeitSekunden)} · Hilfsfrist ${dauerText(e.hilfsfristSekunden)}",
            style = Schrift.MonoKlein,
            color = Farben.AmberHell,
        )
        Text(e.adresse, style = Schrift.MonoKlein, color = Farben.TextLeise)

        if (achse != null) Zeitstrahl(achse, e.prioritaet)

        befunde.forEach { Befundzeile(it, eng = true) }

        e.chronologie.forEach { c ->
            // Eine Störung wird hervorgehoben — genau hier liegt der Unterschied
            // zwischen Störung und eigenem Fehler.
            val stoerung = c.text.startsWith("Störung (")
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (stoerung) Farben.HauchSignal else Color.Transparent, Rundung.Winzig),
            ) {
                Text(buchUhrzeit(c.zeit), style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                Text(c.text, style = Schrift.Klein, color = if (stoerung) Farben.SignalHell else Farben.TextLeise)
            }
        }
    }
}

@Composable
private fun Anrufjournal(
    journal: List<de.pagerspass.pagerspass.netz.ArchivAnruf>,
    einsaetze: List<ArchivEinsatz>,
) {
    var offen by remember { mutableStateOf<String?>(null) }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        journal.forEach { a ->
            val nummer = a.incidentId?.let { id -> einsaetze.firstOrNull { it.id == id }?.einsatznummer }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = { offen = if (offen == a.anrufId) null else a.anrufId },
                        role = Role.Button,
                        indication = null,
                        interactionSource = null,
                    )
                    .padding(vertical = Abstand.Winzig),
            ) {
                Text("Notruf ${a.nummer}", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                Text(buchUhrzeit(a.eingangUm), style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                Text(
                    ausgangname(a.ausgang) + (nummer?.let { " · $it" } ?: ""),
                    style = Schrift.MonoKlein,
                    color = when (a.ausgang) {
                        "Verpasst", "Abgewiesen" -> Farben.SignalHell
                        "EinsatzAngelegt", "Zugeordnet" -> Farben.GruenHell
                        else -> Farben.TextLeise
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(if (offen == a.anrufId) "▾" else "▸", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
            }
            if (offen == a.anrufId) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier.padding(start = Abstand.Normal, bottom = Abstand.Klein),
                ) {
                    if (a.verlauf.isEmpty()) SehrLeise("Es kam kein Gespräch zustande.", mono = true)
                    a.verlauf.forEach { z ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Text(
                                if (z.vonLeitstelle) "Leitstelle" else "Anrufer",
                                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                                color = if (z.vonLeitstelle) Farben.BlauHell else Farben.AmberHell,
                                modifier = Modifier.widthIn(min = 72.dp),
                            )
                            Text(z.text, style = Schrift.Klein, color = Farben.TextLeise)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Protokollzeile(m: Funkzeile) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Text(buchUhrzeit(m.zeit), style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
        Text(
            m.von,
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
            color = Farben.Text,
            modifier = Modifier.widthIn(max = 140.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(m.text, style = Schrift.MonoKlein, color = Farben.TextLeise)
    }
}

/** Das Funkprotokoll als Text — eine Zeile je Meldung, Tabulator dazwischen. */
private fun protokollAlsText(meldungen: List<Funkzeile>, leitstelle: String, runde: ArchivRunde): String {
    val zeilen = mutableListOf(
        "Funkprotokoll $leitstelle — Raum ${runde.code}",
        runde.beendetUm?.let { "Dienstende ${zeitGenau(it)}" } ?: "Dienst noch offen",
        "${meldungen.size} Meldungen",
        "",
    )
    meldungen.forEach { m -> zeilen.add("${zeitGenau(m.zeit)}\t${m.von}\t${m.an ?: "alle"}\t${m.text}") }
    return zeilen.joinToString("\n")
}

// ------------------------------------------------------------------ Replay

private val TEMPI = listOf(1, 4, 16)

/**
 * Die Schicht im Zeitraffer nacherleben: Der Regler hält Sekunden seit
 * Schichtbeginn; Funkprotokoll und Lagekarte blenden bis zu diesem Zeitpunkt ein,
 * rein lesend. Die Fahrzeuge fahren geradlinig von der Wache zur Einsatzstelle —
 * dieselbe Rechnung wie im Web (`replayPosition`).
 */
@Composable
private fun Replay(runde: ArchivRunde) {
    val start = zeitVon(runde.gestartetUm) ?: return
    val ende = zeitVon(runde.beendetUm) ?: return
    val dauer = max(0.0, (ende.toEpochMilli() - start.toEpochMilli()) / 1000.0)
    // Aufgerundet: Bei einer kurzen Schicht wäre das Ende sonst nicht erreichbar.
    val obergrenze = ceil(dauer).toFloat()

    var sekunden by remember(runde.code) { mutableFloatStateOf(0f) }
    var spielt by remember(runde.code) { mutableStateOf(false) }
    var tempo by remember(runde.code) { mutableStateOf(1) }

    LaunchedEffect(spielt, tempo) {
        while (spielt) {
            delay(200)
            val naechste = sekunden + 0.2f * tempo
            if (naechste >= dauer) {
                sekunden = dauer.toFloat()
                spielt = false
            } else {
                sekunden = naechste
            }
        }
    }

    val jetzt = start.plusMillis((min(sekunden.toDouble(), dauer) * 1000).toLong())
    val karte = remember(runde.code, jetzt) { replayRaum(runde, jetzt) }
    val bisher = runde.funkprotokoll.filter { m -> zeitVon(m.zeit)?.let { !it.isAfter(jetzt) } ?: false }

    Ueberschrift("Einsatz-Replay")
    Lagekarte(raum = karte, modifier = Modifier.fillMaxWidth().height(320.dp))

    if (runde.schattenspuren.isNotEmpty()) {
        SehrLeise(
            "Die Gegenprobe dieser Schicht: ${runde.schattenspuren.size}× stand ein Fahrzeug zur Alarmminute " +
                "frei, das früher an der Einsatzstelle gewesen wäre — " +
                runde.schattenspuren.joinToString(", ") { "${it.funkrufname} (${dauerText(it.ersparnisSekunden)} früher)" } +
                ". Sie sagt, wer wann dagewesen wäre — nicht, wie es dann ausgegangen wäre.",
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Knopf(
                aufschrift = if (spielt) "⏸" else "▶",
                beiDruck = {
                    val los = !spielt
                    // Am Ende angekommen: Abspielen beginnt wieder von vorn.
                    if (los && sekunden >= dauer) sekunden = 0f
                    spielt = los
                },
                kompakt = true,
            )
            Text(zeitanzeige(sekunden.toDouble()), style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
            Text("/ ${zeitanzeige(dauer)}", style = Schrift.MonoKlein, color = Farben.TextSehrLeise, modifier = Modifier.weight(1f))
            TEMPI.forEach { stufe ->
                Knopf(
                    aufschrift = "$stufe×",
                    beiDruck = { tempo = stufe },
                    art = if (tempo == stufe) Knopfart.Haupt else Knopfart.Leise,
                    kompakt = true,
                )
            }
        }
        Slider(
            value = sekunden.coerceIn(0f, max(obergrenze, 0f)),
            onValueChange = {
                spielt = false
                sekunden = min(it.toDouble(), dauer).toFloat()
            },
            valueRange = 0f..max(obergrenze, 1f),
            colors = SliderDefaults.colors(
                thumbColor = Farben.Amber,
                activeTrackColor = Farben.Amber,
                inactiveTrackColor = Farben.Rand,
            ),
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        if (bisher.isEmpty()) SehrLeise("Noch keine Meldung zu diesem Zeitpunkt der Schicht.")
        // Die jüngsten dreißig — der Zeitraffer ist zum Mitlesen da, nicht zum Archivieren;
        // die ganze Schicht steht unten im Funkprotokoll.
        bisher.takeLast(30).forEach { Protokollzeile(it) }
    }
}

/** „1:02:07" oder „12:07". */
private fun zeitanzeige(sekunden: Double): String {
    val ganz = sekunden.toInt()
    val std = ganz / 3600
    val min = (ganz % 3600) / 60
    val sek = ganz % 60
    return if (std > 0) {
        "$std:${min.toString().padStart(2, '0')}:${sek.toString().padStart(2, '0')}"
    } else {
        "$min:${sek.toString().padStart(2, '0')}"
    }
}

/**
 * Der Raum zum Replay-Zeitpunkt, so wie die Lagekarte ihn liest: Einsätze, die
 * schon eingingen und noch nicht geschlossen waren; Fahrzeuge auf der Wache vor der
 * Anfahrt, dazwischen geradlinig gerechnet, danach an der Einsatzstelle.
 */
private fun replayRaum(runde: ArchivRunde, jetzt: Instant): Raumzustand {
    val einsaetze = runde.incidents.filter { e ->
        val eingang = zeitVon(e.eingangUm)
        val schluss = zeitVon(e.abgeschlossenUm)
        eingang != null && !eingang.isAfter(jetzt) && (schluss == null || schluss.isAfter(jetzt)) &&
            e.lat != null && e.lon != null
    }.map { e ->
        Einsatz(
            id = e.id,
            einsatznummer = e.einsatznummer,
            stichwort = e.stichwort,
            stichwortText = e.stichwortText,
            meldebild = e.meldebild,
            adresse = e.adresse,
            ortsteil = e.ortsteil,
            organisation = e.organisation,
            prioritaet = e.prioritaet,
            state = if (e.state == "Abgeschlossen") "InArbeit" else e.state,
            lat = e.lat,
            lon = e.lon,
        )
    }

    val fahrzeuge = runde.vehicles.map { f ->
        val lage = replayLage(runde.bewegungsabschnitte, f.id, jetzt)
        f.copy(
            lat = lage?.first ?: f.wacheLat ?: f.lat,
            lon = lage?.second ?: f.wacheLon ?: f.lon,
            einsatzstelleErreicht = false,
        )
    }

    return Raumzustand(
        code = runde.code,
        state = "Beendet",
        settings = runde.settings,
        players = runde.players,
        incidents = einsaetze,
        vehicles = fahrzeuge,
    )
}

private fun replayLage(abschnitte: List<ArchivAnfahrt>, fahrzeug: String, jetzt: Instant): Pair<Double, Double>? {
    val abschnitt = abschnitte
        .filter { it.vehicleId == fahrzeug && zeitVon(it.anfahrtBeginn)?.isAfter(jetzt) == false }
        .maxByOrNull { zeitVon(it.anfahrtBeginn)?.toEpochMilli() ?: 0L }
        ?: return null

    val ankunft = zeitVon(abschnitt.ankunft) ?: return abschnitt.wacheLat to abschnitt.wacheLon
    if (!jetzt.isBefore(ankunft)) return abschnitt.zielLat to abschnitt.zielLon

    val beginn = zeitVon(abschnitt.anfahrtBeginn)?.toEpochMilli() ?: return abschnitt.wacheLat to abschnitt.wacheLon
    val spanne = ankunft.toEpochMilli() - beginn
    val anteil = if (spanne > 0) (jetzt.toEpochMilli() - beginn).toDouble() / spanne else 1.0
    return (abschnitt.wacheLat + (abschnitt.zielLat - abschnitt.wacheLat) * anteil) to
        (abschnitt.wacheLon + (abschnitt.zielLon - abschnitt.wacheLon) * anteil)
}
