package de.pagerspass.pagerspass.ansichten

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.ArchivAnruf
import de.pagerspass.pagerspass.netz.ArchivAuswertung
import de.pagerspass.pagerspass.netz.ArchivBefund
import de.pagerspass.pagerspass.netz.ArchivChronikzeile
import de.pagerspass.pagerspass.netz.ArchivDoppelmeldungen
import de.pagerspass.pagerspass.netz.ArchivEinsatz
import de.pagerspass.pagerspass.netz.ArchivRunde
import de.pagerspass.pagerspass.netz.ArchivZeitachse
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Funkzeile
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Schichtbilanz
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Seite

/**
 * Die Nachbesprechung — was die Schicht gebracht hat, gleich nach dem Dienstende.
 *
 * Übertragen aus `web/src/views/DebriefingView.vue` im Zweig ohne gereichte Runde:
 * Schichtkarte zum Teilen, Kennzahlen, Gutschrift mit „Dienstbuch ansehen", die
 * Befunde zur Schicht, Doppelmeldungen, wer was getan hat, die Mannschaft mit
 * „+ Freund", die Einsätze mit Zeitstrahl, das Anrufjournal und das Funkprotokoll
 * zum Sichern.
 *
 * <b>Dieselbe Darstellung wie im Dienstbuch</b> (`Nachbesprechung` in
 * `DebriefingBausteine.kt`). Die Quelle ist zuerst der Raum der gerade beendeten
 * Runde ([alsArchivrunde]); sobald die Archivfassung nachgeladen ist (`GET
 * /api/archiv/{code}` — wie im Web beim Öffnen), gilt sie: dieselbe Schicht, nur
 * vollständig. Die Gutschrift bleibt an der Sitzung hängen — sie kommt als eigenes
 * Ereignis (`Erfahrung`), sobald der Server abgerechnet hat.
 */
@Composable
fun DebriefingSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Rundenstand = Rundenstand(),
    konto: Konto? = null,
    freunde: List<Freund> = emptyList(),
    archiv: Bereichsstand<ArchivRunde> = Bereichsstand(),
    beiArchivLaden: (String) -> Unit = {},
    beiAnfragen: suspend (String) -> Result<Unit> = { Result.success(Unit) },
    beiVerlassen: () -> Unit = {},
    beiHilfe: () -> Unit = {},
    beiDienstbuch: () -> Unit = {},
    beiPremium: () -> Unit = {},
) {
    val raum = stand.raum
    var mottowerbung by remember { mutableStateOf(false) }

    // Wie im Web: gleich beim Öffnen die vollständige Fassung holen — sie gilt dann
    // als Quelle (ganzes Protokoll, ganzes Journal).
    LaunchedEffect(raum?.code, raum?.beendetUm) {
        val code = raum?.code
        if (code != null && raum.beendetUm != null) beiArchivLaden(code)
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        if (raum == null) {
            Ladezeile("Die Nachbesprechung wird vorbereitet …")
            return@Seite
        }
        val nachgeladen = archiv.inhalt?.takeIf { it.code.equals(raum.code, ignoreCase = true) }
        val schicht = nachgeladen ?: raum.alsArchivrunde(stand.funk)

        Nachbesprechung(
            runde = schicht,
            konto = konto,
            freunde = freunde,
            beiAnfragen = beiAnfragen,
            beiMottowerbung = { mottowerbung = true },
            live = Livebesprechung(
                gutschrift = stand.gutschrift,
                konto = konto,
                vollstaendig = nachgeladen,
                vollstaendigLaedt = archiv.laedt,
                vollstaendigFehler = archiv.fehler,
                beiVollstaendigLaden = { beiArchivLaden(raum.code) },
                beiStart = beiVerlassen,
                beiHilfe = beiHilfe,
                beiDienstbuch = beiDienstbuch,
            ),
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

/**
 * Die gerade beendete Runde als Archivrunde — das neutrale Modell der
 * Nachbesprechung.
 *
 * <b>Übersetzt wird nur, was die Runde schon trägt.</b> Anfahrtsabschnitte und
 * Gegenprobe gibt es live nicht (dort gibt es auch kein Replay), und das
 * Funkprotokoll ist der letzte Ausschnitt — Zustand plus zwischenzeitliche Sprüche.
 */
fun Raumzustand.alsArchivrunde(funk: List<Funkzeile>): ArchivRunde = ArchivRunde(
    code = code,
    settings = settings,
    gestartetUm = gestartetUm,
    beendetUm = beendetUm,
    players = players,
    vehicles = vehicles,
    incidents = incidents.map { e ->
        ArchivEinsatz(
            id = e.id,
            einsatznummer = e.einsatznummer,
            stichwort = e.stichwort,
            stichwortText = e.stichwortText,
            meldebild = e.meldebild,
            adresse = e.adresse,
            ortsteil = e.ortsteil,
            lat = e.lat,
            lon = e.lon,
            organisation = e.organisation,
            prioritaet = e.prioritaet,
            state = e.state,
            alarmierteFahrzeuge = e.alarmierteFahrzeuge,
            chronologie = e.chronologie.map { ArchivChronikzeile(it.zeit, it.text, it.urheber) },
            eingangUm = e.eingangUm,
            abgeschlossenUm = e.abgeschlossenUm,
            dispositionszeitSekunden = e.dispositionszeitSekunden,
            hilfsfristSekunden = e.hilfsfristSekunden,
        )
    },
    funkprotokoll = funk,
    anrufjournal = anrufjournal.map { a ->
        ArchivAnruf(
            anrufId = a.anrufId,
            nummer = a.nummer,
            eingangUm = a.eingangUm,
            abgeschlossenUm = a.abgeschlossenUm,
            ausgang = a.ausgang,
            incidentId = a.incidentId,
            verlauf = a.verlauf,
        )
    },
    auswertung = auswertung?.let { w ->
        ArchivAuswertung(
            befunde = w.befunde.map { ArchivBefund(it.grad, it.titel, it.text, it.incidentId) },
            zeitachsen = w.zeitachsen.map {
                ArchivZeitachse(it.incidentId, it.notruf, it.alarm, it.ausgerueckt, it.vorOrt, it.ersteLagemeldung, it.abschluss)
            },
            spieler = w.spieler.map { b ->
                Schichtbilanz(
                    playerId = b.playerId,
                    name = b.name,
                    rolle = b.rolle,
                    runden = b.runden,
                    einsaetze = b.einsaetze,
                    lagemeldungen = b.lagemeldungen,
                    funksprueche = b.funksprueche,
                    ausrueckzeitSekunden = b.ausrueckzeitSekunden,
                    ausserDienstSekunden = b.ausserDienstSekunden,
                )
            },
            doppelmeldungen = w.doppelmeldungen.let { d ->
                ArchivDoppelmeldungen(
                    mehrfachGemeldeteLagen = d.mehrfachGemeldeteLagen,
                    zusammengefuehrt = d.zusammengefuehrt,
                    falschzugeordnet = d.falschzugeordnet,
                    doppelalarmierungen = d.doppelalarmierungen,
                    nennenswert = d.nennenswert,
                )
            },
        )
    } ?: ArchivAuswertung(),
)
