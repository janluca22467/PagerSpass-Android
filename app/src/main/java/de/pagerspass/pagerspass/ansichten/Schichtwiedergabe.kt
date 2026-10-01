package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Archivrunde
import de.pagerspass.pagerspass.netz.Bewegungsabschnitt
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundeneinstellungen
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.karte.Lagekarte
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Die Wiedergabe einer archivierten Schicht und ihr Anrufjournal — übertragen
 * aus `DebriefingView.vue` (Abschnitt „Replay" und „Anrufjournal").
 *
 * Getrennt von `DienstbuchSeiten.kt`, weil es eigene Uhr, eigene Karte und
 * eigenen Zustand trägt; die Schichtseite dort setzt die beiden Teile nur ein.
 */

/** Im Takt des Web: fünfmal je Sekunde, in drei Geschwindigkeiten. */
private const val TAKT_MS = 200L
private val STUFEN = listOf(1, 4, 16)

/**
 * Die Schicht im Zeitraffer — Karte, Regler, Funk bis zum gewählten Zeitpunkt.
 *
 * <b>Rein lesend.</b> Der Regler hält Sekunden seit Schichtbeginn; die Karte
 * zeigt die Einsätze, die zu diesem Zeitpunkt eingegangen und noch offen waren,
 * und die Fahrzeuge dort, wo sie laut Anfahrtsabschnitt standen. Am Archiv
 * ändert sich nichts.
 *
 * <b>Dieselbe Karte wie im Dienst.</b> Die Wiedergabe baut sich einen
 * [Raumzustand] zum gewählten Zeitpunkt und reicht ihn an [Lagekarte] — eine
 * zweite Karte nur fürs Archiv wäre eine zweite Stelle, an der Kacheln,
 * Einwilligung und Gesten gepflegt werden müssten.
 *
 * Kliniken, Löschwasser und Objekte bleiben weg wie im Web: Welche damals
 * galten, ist nicht archiviert.
 */
@Composable
fun Schichtwiedergabe(runde: Archivrunde) {
    val start = wiedergabezeit(runde.gestartetUm) ?: return
    val ende = wiedergabezeit(runde.beendetUm) ?: return
    val dauer = ((ende.toEpochMilli() - start.toEpochMilli()) / 1000f).coerceAtLeast(0f)
    if (dauer <= 0f) return

    // Beim Öffnen am Schichtanfang — ein leeres Protokoll, wie die Runde damals begann.
    var sekunden by remember(runde.code) { mutableFloatStateOf(0f) }
    var spielt by remember(runde.code) { mutableStateOf(false) }
    var stufe by remember(runde.code) { mutableIntStateOf(1) }

    LaunchedEffect(spielt, stufe) {
        while (spielt) {
            delay(TAKT_MS)
            val naechste = sekunden + TAKT_MS / 1000f * stufe
            if (naechste >= dauer) {
                sekunden = dauer
                spielt = false
            } else {
                sekunden = naechste
            }
        }
    }

    val jetzt = start.plusMillis((sekunden * 1000).toLong())
    val raum = remember(runde, jetzt) { wiedergabestand(runde, jetzt) }
    val funk = remember(runde, jetzt) {
        runde.funkprotokoll.filter { z -> wiedergabezeit(z.zeit)?.let { !it.isAfter(jetzt) } ?: false }
    }

    Abschnitt("Schicht nacherleben", weiterweg = { SehrLeise(uhrMitSekunden(jetzt), mono = true) }) {
        Lagekarte(
            raum = raum,
            modifier = Modifier.fillMaxWidth().height(320.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Knopf(
                if (spielt) "❚❚ Anhalten" else "▶ Abspielen",
                {
                    if (!spielt && sekunden >= dauer) sekunden = 0f
                    spielt = !spielt
                },
                art = Knopfart.Haupt,
                kompakt = true,
            )
            STUFEN.forEach { s ->
                Pille("${s}×", an = stufe == s, beiDruck = { stufe = s })
            }
        }
        Slider(
            value = sekunden.coerceIn(0f, dauer),
            onValueChange = {
                spielt = false
                sekunden = it.coerceIn(0f, dauer)
            },
            valueRange = 0f..dauer,
            colors = SliderDefaults.colors(
                thumbColor = Farben.Amber,
                activeTrackColor = Farben.Amber,
                inactiveTrackColor = Farben.FlaecheAktiv,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            SehrLeise(laufzeit(sekunden), mono = true, modifier = Modifier.weight(1f))
            SehrLeise(laufzeit(dauer), mono = true)
        }

        // Der Funk bis hierher, jüngster zuerst — wie ein Chat, der sich füllt.
        // Ein kurzes Fenster reicht: Wer alles lesen will, hat das ganze
        // Protokoll weiter unten.
        Kasten(abstandInnen = Abstand.Winzig, farbe = Farben.BgTief) {
            if (funk.isEmpty()) {
                SehrLeise("Noch kein Funkspruch.", mono = true)
            } else {
                funk.takeLast(6).reversed().forEach { z ->
                    Text(
                        "${wiedergabezeit(z.zeit)?.let(::uhrMitSekunden).orEmpty()}  ${z.von}: ${z.text}",
                        style = Schrift.MonoKlein,
                        color = if (z.kind == "System") Farben.TextSehrLeise else Farben.TextLeise,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

/**
 * Der Stand der Schicht zum Zeitpunkt [jetzt] — als [Raumzustand] für die Karte.
 *
 * Die Positionen spiegeln `replayPosition` aus `Lagekarte.vue`: auf der Wache
 * vor der Anfahrt, linear zur Einsatzstelle während der Anfahrt, an der
 * Einsatzstelle danach.
 */
private fun wiedergabestand(runde: Archivrunde, jetzt: Instant): Raumzustand {
    val einsaetze = runde.incidents
        .filter { e ->
            val eingang = wiedergabezeit(e.eingangUm)
            val schluss = wiedergabezeit(e.abgeschlossenUm)
            e.lat != null && e.lon != null &&
                eingang != null && !eingang.isAfter(jetzt) &&
                (schluss == null || schluss.isAfter(jetzt))
        }
        .map { e ->
            Einsatz(
                id = e.id,
                einsatznummer = e.einsatznummer,
                stichwort = e.stichwort,
                stichwortText = e.stichwortText,
                adresse = e.adresse,
                organisation = e.organisation,
                prioritaet = e.prioritaet,
                // Offen heißt hier: zu diesem Zeitpunkt noch nicht abgeschlossen.
                state = "InArbeit",
                lat = e.lat,
                lon = e.lon,
            )
        }

    val fahrzeuge = runde.vehicles.map { f ->
        val ort = position(runde.bewegungsabschnitte, f.id, jetzt)
        Rundenfahrzeug(
            id = f.id,
            funkrufname = f.funkrufname,
            kurzname = f.kurzname,
            typ = f.typ,
            organisation = f.organisation,
            lat = ort?.first ?: f.wacheLat,
            lon = ort?.second ?: f.wacheLon,
            wacheLat = f.wacheLat,
            wacheLon = f.wacheLon,
        )
    }

    // Die Mitte der Karte: der Schwerpunkt aller Einsätze, sonst der Wachen.
    val orte = runde.incidents.mapNotNull { e -> e.lat?.let { la -> e.lon?.let { lo -> la to lo } } }
        .ifEmpty { runde.vehicles.mapNotNull { f -> f.wacheLat?.let { la -> f.wacheLon?.let { lo -> la to lo } } } }

    return Raumzustand(
        code = runde.code,
        state = "Laeuft",
        settings = Rundeneinstellungen(
            lat = orte.takeIf { it.isNotEmpty() }?.map { it.first }?.average(),
            lon = orte.takeIf { it.isNotEmpty() }?.map { it.second }?.average(),
        ),
        incidents = einsaetze,
        vehicles = fahrzeuge,
    )
}

/** Wo ein Fahrzeug zum Zeitpunkt stand — `null`: noch nie ausgerückt, also auf der Wache. */
private fun position(abschnitte: List<Bewegungsabschnitt>, fahrzeug: String, jetzt: Instant): Pair<Double, Double>? {
    val a = abschnitte
        .filter { it.vehicleId == fahrzeug }
        .mapNotNull { a -> wiedergabezeit(a.anfahrtBeginn)?.let { a to it } }
        .filter { (_, beginn) -> !beginn.isAfter(jetzt) }
        .maxByOrNull { (_, beginn) -> beginn }
        ?: return null
    val (abschnitt, beginn) = a
    val ankunft = wiedergabezeit(abschnitt.ankunft) ?: return abschnitt.wacheLat to abschnitt.wacheLon
    if (!jetzt.isBefore(ankunft)) return abschnitt.zielLat to abschnitt.zielLon

    val gesamt = (ankunft.toEpochMilli() - beginn.toEpochMilli()).toDouble()
    val anteil = if (gesamt > 0) (jetzt.toEpochMilli() - beginn.toEpochMilli()) / gesamt else 1.0
    return (abschnitt.wacheLat + (abschnitt.zielLat - abschnitt.wacheLat) * anteil) to
        (abschnitt.wacheLon + (abschnitt.zielLon - abschnitt.wacheLon) * anteil)
}

// ------------------------------------------------------------ Anrufjournal

private val AUSGANG = mapOf(
    "EinsatzAngelegt" to "Einsatz angelegt",
    "Verworfen" to "Verworfen",
    "Verpasst" to "Verpasst",
    "Abgewiesen" to "Abgewiesen",
    "Zugeordnet" to "Zu laufendem Einsatz",
)

/**
 * Das Anrufjournal: jedes Telefongespräch der Schicht, jüngstes zuerst,
 * aufklappbar — nur bei telefonischer Leitstelle gefüllt.
 */
@Composable
fun Anrufjournal(runde: Archivrunde) {
    if (runde.anrufjournal.isEmpty()) return
    var offen by remember(runde.code) { mutableStateOf<String?>(null) }

    Abschnitt("Anrufjournal", weiterweg = { SehrLeise("${runde.anrufjournal.size}") }) {
        runde.anrufjournal.reversed().forEach { a ->
            val auf = offen == a.anrufId
            val einsatz = a.incidentId?.let { id -> runde.incidents.firstOrNull { it.id == id }?.einsatznummer }
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(ecke = 9.dp)
                    .clickable(role = Role.Button) { offen = if (auf) null else a.anrufId }
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Notruf ${a.nummer}", style = Schrift.MonoKlein, color = Farben.Text)
                    SehrLeise(wiedergabezeit(a.eingangUm)?.let(::uhrMitSekunden).orEmpty(), mono = true)
                    Text(
                        (AUSGANG[a.ausgang] ?: a.ausgang) + (einsatz?.let { " · $it" } ?: ""),
                        style = Schrift.MonoKlein,
                        color = if (a.ausgang == "Verpasst" || a.ausgang == "Abgewiesen") Farben.SignalHell
                        else Farben.AmberHell,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    SehrLeise(if (auf) "▾" else "▸", mono = true)
                }
                if (auf) {
                    if (a.verlauf.isEmpty()) {
                        SehrLeise("Es kam kein Gespräch zustande.", mono = true)
                    }
                    a.verlauf.forEach { z ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Text(
                                if (z.vonLeitstelle) "Leitstelle" else "Anrufer",
                                style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                                color = if (z.vonLeitstelle) Farben.AmberHell else Farben.TextLeise,
                            )
                            Text(
                                z.text,
                                style = Schrift.Klein,
                                color = if (z.vonLeitstelle) Farben.Text else Farben.TextLeise,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Zeitwerte

private fun wiedergabezeit(roh: String?): Instant? =
    roh?.takeIf { it.isNotBlank() }?.let {
        runCatching { Instant.parse(it) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
    }

private fun uhrMitSekunden(t: Instant): String =
    DateTimeFormatter.ofPattern("HH:mm:ss").format(t.atZone(ZoneId.systemDefault()))

/** „4:05" oder „1:04:05" — Sekunden seit Schichtbeginn, wie der Regler im Web. */
private fun laufzeit(sekunden: Float): String {
    val s = sekunden.toInt()
    val std = s / 3600
    val min = (s % 3600) / 60
    val sek = s % 60
    return if (std > 0) "%d:%02d:%02d".format(std, min, sek) else "%d:%02d".format(min, sek)
}
