package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.leuchtleiste
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Die Kästen des Reiters „Fahrzeug" — übertragen aus `EinsatzKarte.vue`,
 * `FmsTastatur.vue`, `Sondersignal.vue`, `LagemeldungForm.vue`,
 * `EinsatzleitungPanel.vue`, `VerlassenDialog.vue` und `DienstendeDialog.vue`
 * (Web 5.0.0.26), in der Reihenfolge, in der sie am Handy stehen.
 */

// ---------------------------------------------------------------- Anfahrt

/** Luftlinie in Metern (Haversine) — `distanzMeter` aus `utils/anfahrt.ts`. */
internal fun distanzMeter(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
    val rad = PI / 180
    val dLat = (bLat - aLat) * rad
    val dLon = (bLon - aLon) * rad
    val h = sin(dLat / 2).let { it * it } + cos(aLat * rad) * cos(bLat * rad) * sin(dLon / 2).let { it * it }
    return 6_371_000 * 2 * atan2(sqrt(h), sqrt(1 - h))
}

/** Was von der Strecke noch übrig ist — längs der Route, ohne Route in Luftlinie. */
private fun restMeter(f: Rundenfahrzeug, zielLat: Double, zielLon: Double): Double? {
    val lat = f.lat ?: return null
    val lon = f.lon ?: return null
    val punkte = f.route.drop(f.routeIndex.coerceAtMost(f.route.size))
    if (punkte.isEmpty()) return distanzMeter(lat, lon, zielLat, zielLon)
    var meter = distanzMeter(lat, lon, punkte[0].lat, punkte[0].lon)
    for (i in 1 until punkte.size) meter += distanzMeter(punkte[i - 1].lat, punkte[i - 1].lon, punkte[i].lat, punkte[i].lon)
    return meter
}

/** Das Fahrtempo in m/s — `geschwindigkeitMs` mal Zeitfaktor der Runde. */
private fun tempoMs(organisation: String, prioritaet: Int, raum: Raumzustand): Double {
    val basis = when (organisation) {
        "Rettungsdienst" -> 18.0
        "Polizei" -> 20.0
        "Feuerwehr" -> 15.0
        "Thw" -> 12.0
        else -> 14.0
    }
    val prio = if (prioritaet >= 3) 1.35 else if (prioritaet == 2) 1.15 else 1.0
    val zeit = if (raum.settings.zeitmodus == "Simulation") 3.0 else 1.0
    return basis * prio * zeit
}

internal fun entfernungText(meter: Double): String =
    if (meter < 1000) "${maxOf(1, meter.roundToInt())} m" else "%.1f km".format(meter / 1000).replace('.', ',')

private fun anfahrtszeitText(sekunden: Double): String {
    val min = (sekunden / 60).roundToInt()
    return if (min < 1) "< 1 min" else "$min min"
}

// --------------------------------------------------------- Die Einsatzkarte

/**
 * Die Einsatzkarte — Stichwort, Adresse, Meldebild und alles, was die Besatzung an
 * dieser Lage wissen muss. Sie steht offen und nicht hinter Aufklappern: Wer auf der
 * Anfahrt sitzt, hat eine Hand am Haltegriff.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.Einsatzkarte(raum: Raumzustand, einsatz: Einsatz?, meins: Rundenfahrzeug, befehle: Raumbefehle) {
    if (einsatz == null) {
        Text(
            "Kein Einsatzauftrag. Melder bleibt scharf.",
            style = Schrift.MonoKlein,
            color = Farben.TextSehrLeise,
            modifier = Modifier.fillMaxWidth().padding(vertical = Abstand.Klein),
        )
        return
    }
    val jetzt = sekundentakt()
    val dringlichkeit = when {
        einsatz.abgeschlossen -> einsatzFarbe(einsatz.state)
        einsatz.prioritaet >= 3 -> Farben.Signal
        einsatz.prioritaet == 2 -> Farben.Amber
        else -> Farben.FmsFrei
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = dringlichkeit.copy(alpha = 0.35f))
            .leuchtleiste(dringlichkeit, schein = 0.6f, glimmt = einsatz.prioritaet >= 3 && !einsatz.abgeschlossen)
            .padding(start = Abstand.Normal + 3.dp, end = Abstand.Normal, top = Abstand.Normal, bottom = Abstand.Normal),
    ) {
        // Kopf: das Stichwort als Kennung, daneben Titel und Nummer.
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text(
                einsatz.stichwort,
                style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                color = Farben.AufFarbe,
                modifier = Modifier.background(dringlichkeit, Rundung.Winzig).padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
            )
            Column(Modifier.weight(1f)) {
                Text(einsatz.stichwortText, style = Schrift.Gross.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                SehrLeise("${einsatz.einsatznummer} · ${einsatzZustand(einsatz.state)}", mono = true)
            }
        }
        Text(
            einsatz.adresse + (einsatz.ortsteil?.let { " · $it" } ?: ""),
            style = Schrift.MonoNormal,
            color = Farben.AmberHell,
        )
        if (einsatz.meldebild.isNotBlank()) Text(einsatz.meldebild, style = Schrift.Normal, color = Farben.Text)

        // Bestellte Fahrt: Termin und Zielhaus stehen von der Alarmierung an fest.
        einsatz.terminUm?.let { termin ->
            val rest = zeitpunktMs(termin)?.let { ((it - jetzt) / 60_000.0).roundToInt() }
            Etikett("Terminfahrt")
            Text(
                "Abholung bis ${uhrzeitKurz(termin)}" + (rest?.let { if (it >= 0) " — noch $it min" else " — überfällig" } ?: ""),
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
            einsatz.zielklinikVorgabe?.let { Text("Ziel $it", style = Schrift.MonoKlein, color = Farben.TextLeise) }
        }

        // Die eigene Suche.
        if ((einsatz.suchradiusMeter ?: 0.0) > 0.0) {
            val abschnitt = meins.suchabschnitt?.let { einsatz.suchabschnitte.getOrNull(it) }
            Etikett("Suchgebiet")
            Text(
                when {
                    einsatz.personGefundenUm != null -> "Person gefunden — Einsatzstelle ist jetzt die Fundstelle"
                    abschnitt != null -> "Abschnitt ${abschnitt.name} — ${(abschnitt.fortschritt * 100).roundToInt()} % abgesucht · Gebiet gesamt ${(einsatz.suchfortschritt * 100).roundToInt()} %"
                    else -> "Noch kein Abschnitt — die Leitstelle teilt zu, sonst der nächste freie."
                },
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
        }

        // Der Einsatzplan des Objekts.
        einsatz.objektName?.let { name ->
            Etikett("Objekt")
            Text(name + (einsatz.objektartText?.let { " · $it" } ?: ""), style = Schrift.MonoKlein, color = Farben.Text)
            if (einsatz.objektBetroffene > 0) Text("etwa ${einsatz.objektBetroffene} Personen im Objekt", style = Schrift.MonoKlein, color = Farben.AmberHell)
            einsatz.objektHinweise.forEach { Text("• $it", style = Schrift.Klein, color = Farben.TextLeise) }
        }

        // Der eigene Transportauftrag am Massenanfall.
        val meinPatient = if (einsatz.manv) einsatz.manvPatienten.firstOrNull { it.transportVehicleId == meins.id } else null
        val transportKlinik = if (meins.status == 7) raum.kliniken.firstOrNull { it.id == meins.zielklinikId } else null
        if (meinPatient != null) {
            Etikett("Transportauftrag")
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text("Patient ${meinPatient.id}", style = Schrift.MonoKlein, color = Farben.Text)
                meinPatient.kategorie?.let { Marke(kategorieLabel(it), farbe = kategorieFarbeSk(it)) }
                SehrLeise("— " + if (meinPatient.status == "ImTransport") "an Bord" else (meinPatient.stelle ?: "im Gelände"))
            }
            if (transportKlinik == null) {
                SehrLeise(
                    meinPatient.zielklinikId?.let { id -> raum.kliniken.firstOrNull { it.id == id }?.name }?.let { "Ziel $it" }
                        ?: "Ziel offen — die Einsatzleitung weist es zu.",
                )
            }
        }

        // Das Transportziel — Status 7.
        if (meins.status == 7) {
            Etikett("Transportziel")
            if (transportKlinik != null) {
                val meter = restMeter(meins, transportKlinik.lat, transportKlinik.lon)
                val prio = if (meins.sondersignalAus) 1 else einsatz.prioritaet
                Text(
                    transportKlinik.name + when {
                        meins.zielklinikErreicht -> " — erreicht, Übergabe möglich (Status 8)"
                        meter != null -> " — noch ${entfernungText(meter)} · ca. ${anfahrtszeitText(meter / tempoMs(meins.organisation, prio, raum))}"
                        else -> " — Anfahrt läuft"
                    },
                    style = Schrift.MonoKlein,
                    color = Farben.Text,
                )
            } else {
                Text(
                    "Noch kein Ziel zugewiesen — " + if (einsatz.manv) "die Einsatzleitung nennt die Klinik." else "die Leitstelle nennt die Klinik.",
                    style = Schrift.MonoKlein,
                    color = Farben.AmberHell,
                )
            }
        }

        // Löschwasser.
        if (raum.settings.loeschwasser && meins.tankLiter > 0) {
            val anteil = (meins.wasserLiter / meins.tankLiter).coerceIn(0.0, 1.0)
            val unterwegs = meins.entnahmestelleId != null && !meins.entnahmestelleErreicht
            val stellen = raum.entnahmestellen
            val ziel = stellen.firstOrNull { it.id == meins.entnahmestelleId } ?: stellen.minByOrNull { s ->
                distanzMeter(meins.lat ?: s.lat, meins.lon ?: s.lon, s.lat, s.lon)
            }
            val zielWeg = ziel?.let { s -> meins.lat?.let { la -> meins.lon?.let { lo -> entfernungText(distanzMeter(la, lo, s.lat, s.lon)) } } }
            val kannHolen = meins.status == 4 && meins.entnahmestelleId == null && anteil < 0.99 && !einsatz.wasserversorgungSteht && stellen.isNotEmpty()
            Etikett("Löschwasser")
            Laufbalken(anteil.toFloat(), farbe = if (anteil <= 0.35) Farben.Signal else Farben.BlauHell)
            Text(
                "${meins.wasserLiter.roundToInt()} l von ${meins.tankLiter.roundToInt()} l" + when {
                    meins.entnahmestelleErreicht -> " — wird aufgefüllt" + (ziel?.let { " (${it.name})" } ?: "")
                    unterwegs -> " — unterwegs zu ${ziel?.name ?: "Entnahmestelle"}" + (zielWeg?.let { ", noch $it" } ?: "")
                    einsatz.wasserversorgungSteht -> " — Versorgung steht"
                    else -> ""
                },
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
            if (kannHolen) {
                Row {
                    Knopf(
                        "Wasser aufnehmen" + (ziel?.let { " · ${it.name}" } ?: "") + (zielWeg?.let { " · $it" } ?: ""),
                        befehle::wasserAufnehmen,
                        kompakt = true,
                    )
                }
            }
        }

        // Die brennende Fläche — vor den Aufgaben: ob es größer wird, zuerst.
        if (einsatz.brandflaecheHektar > 0) {
            Etikett("Brandfläche")
            Text(
                "${hektar(einsatz.brandflaecheHektar)} ha — " +
                    if (einsatz.brandflaecheHektar >= einsatz.brandflaecheHoechstHektar) "breitet sich weiter aus" else "Höchststand ${hektar(einsatz.brandflaecheHoechstHektar)} ha",
                style = Schrift.MonoKlein,
                color = Farben.SignalHell,
            )
        }

        // Die Arbeit an der Stelle — die eigene Aufgabe hervorgehoben.
        if (einsatz.aufgaben.isNotEmpty()) {
            val vorOrt = meins.status == 4 && meins.einsatzstelleErreicht
            val wartend = einsatz.aufgaben.filter { !it.fertig && !it.begonnen }
            val dran = einsatz.aufgaben.filter { !it.fertig && it.begonnen }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Etikett("Aufgaben")
                SehrLeise("· ${einsatz.aufgaben.count { it.fertig }} von ${einsatz.aufgaben.size} erledigt")
            }
            when {
                einsatz.arbeitFertigUm != null -> Text("Einsatzstelle abgearbeitet — einrücken möglich (Status 1 oder 2)", style = Schrift.MonoKlein, color = Farben.GruenHell)
                wartend.isNotEmpty() && dran.isNotEmpty() -> SehrLeise("Zuerst: ${dran.first().name} — das Übrige ist danach an der Reihe.")
            }
            einsatz.aufgaben.forEach { a ->
                val meine = meins.aufgabe == a.nummer
                val waehlbar = vorOrt && !a.fertig && a.begonnen && !meine &&
                    (!a.zwingend || a.faehigkeit == null || a.faehigkeit in meins.faehigkeiten)
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (meine) Modifier.background(Farben.HauchAmber, Rundung.Winzig) else Modifier)
                        .padding(Abstand.Winzig),
                ) {
                    Row {
                        Text(
                            a.name,
                            style = Schrift.Klein.copy(fontWeight = if (meine) FontWeight.Bold else FontWeight.Normal),
                            color = if (!a.fertig && !a.begonnen) Farben.TextSehrLeise else Farben.Text,
                            modifier = Modifier.weight(1f),
                        )
                        Text(if (a.fertig) "erledigt" else "${(a.fortschritt * 100).roundToInt()} %", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
                    }
                    Laufbalken(a.fortschritt.toFloat(), farbe = if (a.fertig) Farben.Gruen else Farben.Amber)
                    when {
                        a.kraefteFehlen -> Text("Dafür sind wir zu wenige — ${a.mindestkraefte} Einsatzkräfte nötig.", style = Schrift.Winzig, color = Farben.AmberHell)
                        a.mittelFehlt -> Text("Dafür fehlt uns: ${a.faehigkeit ?: a.name}", style = Schrift.Winzig, color = Farben.AmberHell)
                    }
                    if (waehlbar) Row { Knopf("Übernehmen", { befehle.aufgabeUebernehmen(a.nummer) }, kompakt = true) }
                }
            }
        }

        // Die anderen auf demselben Einsatz.
        val mitAlarmierte = einsatz.alarmierteFahrzeuge.mapNotNull { id -> raum.vehicles.firstOrNull { it.id == id } }.filter { it.id != meins.id }
        if (mitAlarmierte.isNotEmpty()) {
            Etikett("Weitere Kräfte")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                mitAlarmierte.forEach { f ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                        modifier = Modifier.border(1.dp, fmsFarbe(f.status).copy(alpha = 0.6f), Rundung.Rund).padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
                    ) {
                        Text(f.status.toString(), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = fmsFarbe(f.status))
                        Text(kennungVon(f), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.Text)
                    }
                }
            }
        }

        // Die letzten drei Lagemeldungen.
        if (einsatz.lagemeldungen.isNotEmpty()) {
            Etikett("Bisherige Lagemeldungen")
            einsatz.lagemeldungen.takeLast(3).forEach { l ->
                Text("${l.funkrufname}: ${l.text}", style = Schrift.Klein, color = Farben.TextLeise)
            }
        }
    }
}

private fun hektar(wert: Double): String =
    if (wert < 10) "%.1f".format(wert).replace('.', ',') else wert.roundToInt().toString()

// ------------------------------------------------------------- FMS-Kasten

/** Die Gründe für Status 6 — mit der Dauer, nach der der Server wieder freigibt. */
private val AUSSER_DIENST = listOf(
    "Tanken" to 7,
    "Reinigung nach Einsatz" to 12,
    "Atemschutzgeräte tauschen" to 10,
    "Material ergänzen" to 15,
    "Verpflegungspause" to 20,
    "Technischer Defekt" to 40,
)

private val FMS_KURZ = mapOf(
    "Einsatzbereit über Funk" to "Frei über Funk",
    "Einsatzbereit auf Wache" to "Frei auf Wache",
    "Einsatzbereit auf Revier" to "Frei auf Revier",
    "Anfahrt Einsatzort" to "Anfahrt",
    "Ankunft Einsatzort" to "Am Einsatzort",
    "Nicht einsatzbereit" to "Nicht bereit",
    "Patient aufgenommen" to "Patient an Bord",
    "Am Transportziel" to "Am Ziel",
    "Bedingt verfügbar" to "Bedingt frei",
)

/**
 * Der FMS-Kasten — zwei Reiter, FMS-Status und Sondersignal. Bei „Im Funk" trägt das
 * Funkgerät die Tasten, dann bleibt nur das Sondersignal; ohne Blaulicht an Bord
 * (Hubschrauber) nur die Tasten.
 */
@Composable
fun ColumnScope.FmsKasten(
    raum: Raumzustand,
    einsatz: Einsatz?,
    meins: Rundenfahrzeug,
    katalog: Katalog?,
    imFunk: Boolean,
    befehle: Raumbefehle,
    beiFms: (Int, String?, Int?) -> Unit,
    beiSondersignal: (Boolean) -> Unit,
) {
    val blaulicht = !meins.istLuftfahrzeug
    if (imFunk && !blaulicht) return
    var reiter by remember { mutableStateOf("fms") }

    Kasten(innenraum = Abstand.Klein, abstandInnen = Abstand.Klein) {
        if (!imFunk && blaulicht) {
            Bogenreiter(listOf("fms" to "FMS-Status", "signal" to "Sondersignal"), reiter, { reiter = it })
        } else {
            Etikett(if (imFunk) "Sondersignal" else "FMS-Status")
        }
        if (blaulicht && (imFunk || reiter == "signal")) {
            SondersignalBedienteil(meins, beiSondersignal)
        } else {
            FmsTastatur(raum, einsatz, meins, katalog, befehle, beiFms)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.FmsTastatur(
    raum: Raumzustand,
    einsatz: Einsatz?,
    meins: Rundenfahrzeug,
    katalog: Katalog?,
    befehle: Raumbefehle,
    beiFms: (Int, String?, Int?) -> Unit,
) {
    val jetzt = sekundentakt()
    var ausserDienst by remember { mutableStateOf(false) }
    var eigenerGrund by remember { mutableStateOf("") }

    // Der Hakenarm des WLF.
    zeitpunktMs(meins.aufsattelnBis)?.let { bis ->
        if (meins.abrollbehaelterTemplateId != null && meins.einsatzId != null) {
            val rest = ((bis - jetzt + 999) / 1000).toInt().coerceAtLeast(0)
            val ab = katalog?.fahrzeuge?.firstOrNull { it.id == meins.abrollbehaelterTemplateId }?.typ?.replace("WLF + ", "") ?: "Abrollbehälter"
            Streifenzeile(
                if (rest > 0) "$ab wird aufgesattelt · noch $rest s · Status 3 gesperrt" else "$ab aufgenommen · ausrückbereit",
                if (rest > 0) Farben.Amber else Farben.GruenHell,
            )
        }
    }

    // Patient aufnehmen (Status 7) oder übergeben (Status 8).
    val vorgangBis = if (meins.status in setOf(7, 8, 5, 0)) meins.patientAufnahmeBis ?: meins.patientUebergabeBis else null
    val vorgangRest = restSekunden(vorgangBis, jetzt)
    if (vorgangBis != null && vorgangRest != null) {
        val aufnahme = meins.patientAufnahmeBis != null
        val text = when {
            vorgangRest > 0 -> if (aufnahme) "Patient wird aufgenommen · noch $vorgangRest s" else "Patient wird übergeben · noch $vorgangRest s"
            aufnahme -> "Patient aufgenommen · transportbereit"
            else -> "Patient abgeliefert · Status 1 melden"
        }
        val vorbei = vorgangRest == 0 && aufnahme && jetzt - (zeitpunktMs(vorgangBis) ?: jetzt) > 8000
        if (!vorbei) {
            Streifenzeile(text, if (vorgangRest > 0) Farben.Amber else Farben.GruenHell)
            if (vorgangRest > 0) Laufbalken(anteilZwischen(meins.patientVorgangSeit ?: vorgangBis, vorgangBis, jetzt))
        }
    }

    // Die Anfahrt — und der Heimweg.
    val anfahrt = when {
        meins.status == 1 && !meins.aufStreife -> {
            val heim = meins.wacheLat?.let { la -> meins.wacheLon?.let { lo -> restMeter(meins, la, lo) } }
            val aufWache = meins.lat != null && meins.wacheLat != null &&
                distanzMeter(meins.lat, meins.lon ?: 0.0, meins.wacheLat, meins.wacheLon ?: 0.0) <= 50
            when {
                aufWache -> "Wache erreicht — Status 2 melden" to true
                heim == null -> "Rückfahrt zur Wache" to false
                else -> "Noch ${entfernungText(heim)} bis zur Wache · ca. ${anfahrtszeitText(heim / tempoMs(meins.organisation, 1, raum))}" to false
            }
        }
        meins.status == 3 && einsatz?.lat != null && einsatz.lon != null -> when {
            meins.einsatzstelleErreicht -> "Einsatzstelle erreicht — Status 4 melden" to true
            else -> restMeter(meins, einsatz.lat, einsatz.lon)?.let { m ->
                "Noch ${entfernungText(m)} bis zur Einsatzstelle · ca. ${anfahrtszeitText(m / tempoMs(meins.organisation, if (meins.sondersignalAus) 1 else einsatz.prioritaet, raum))}"
            }?.let { it to false } ?: ("Anfahrt läuft" to false)
        }
        else -> null
    }
    anfahrt?.let { (text, da) -> Streifenzeile(text, if (da) Farben.FmsVorOrt else Farben.FmsAnfahrt, fett = da) }

    // Die Streife: der Griff und, solange sie läuft, wohin sie fährt.
    if (meins.streifenfaehig) {
        val moeglich = meins.aufStreife || (meins.einsatzId == null && (meins.status == 1 || meins.status == 2))
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            if (meins.aufStreife) {
                Text(
                    "⇢ Streife" + (meins.streifenziel?.let { " — $it" } ?: ""),
                    style = Schrift.MonoKlein,
                    color = Farben.BlauHell,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(Modifier.weight(1f))
            }
            Knopf(if (meins.aufStreife) "Streife beenden" else "Streife aufnehmen", { befehle.streifeSchicken(meins.id, !meins.aufStreife) }, aktiv = moeglich, kompakt = true)
        }
    }

    // Die Sprechaufforderung — das FMS-Zeichen J. Sie gilt zwei Minuten; danach ist
    // sie beantwortet oder vergessen, und ein ewig blinkendes J hieße nichts mehr.
    zeitpunktMs(meins.sprechaufforderungSeit)?.let { seit ->
        if (jetzt - seit < 2 * 60_000) Streifenzeile("J  Sprechaufforderung — Leitstelle bittet ums Wort", Farben.BlauHell, fett = true)
    }

    // Die neun Tasten — Zeichen, Zahl, Wort.
    val tasten = listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)
    val bezeichnung = katalog?.fmsStatus.orEmpty().associate { it.status to it.aufschrift(meins.organisation) }
    val vorgangLaeuft = (vorgangRest ?: 0) > 0
    fun erlaubt(s: Int): Boolean {
        if (meins.status == s) return false
        if (vorgangLaeuft && s != 5 && s != 0) return false
        return when (s) {
            3 -> meins.einsatzId != null && (zeitpunktMs(meins.aufsattelnBis)?.let { it <= jetzt } ?: true)
            4 -> meins.status == 3 && (einsatz?.lat == null || meins.einsatzstelleErreicht)
            7 -> meins.status == 4
            8 -> meins.status == 7
            else -> true
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        maxItemsInEachRow = 3,
        modifier = Modifier.fillMaxWidth(),
    ) {
        tasten.forEach { t ->
            val an = meins.status == t
            val farbe = fmsFarbe(t)
            val wort = bezeichnung[t].orEmpty()
            val aus = !erlaubt(t) && !an
            val null0 = t == 0
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = (if (null0) Modifier.fillMaxWidth() else Modifier.weight(1f))
                    .defaultMinSize(minHeight = if (null0) 46.dp else 56.dp)
                    .background(if (an) lerp(Farben.Flaeche, farbe, 0.22f) else Farben.Flaeche, Rundung.Klein)
                    .border(1.dp, if (an) farbe else Farben.Rand, Rundung.Klein)
                    .leuchtleiste(farbe, ecke = 9.dp, schein = 0f)
                    .clickable(role = Role.Button) {
                        if (t == 6) ausserDienst = true else {
                            ausserDienst = false
                            beiFms(t, null, null)
                        }
                    }
                    .padding(start = Abstand.Klein + 3.dp, end = Abstand.Klein, top = Abstand.Winzig, bottom = Abstand.Winzig),
            ) {
                val inhaltAlpha = if (aus) 0.6f else 1f
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    Fahrzeugzeichen.fms(t, meins.organisation)?.let {
                        Icon(it, contentDescription = null, tint = farbe.copy(alpha = inhaltAlpha), modifier = Modifier.size(18.dp))
                    }
                    if (!null0) Box(Modifier.weight(1f))
                    Text(t.toString(), style = Schrift.Titel.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.ExtraBold), color = farbe.copy(alpha = inhaltAlpha))
                    if (null0) Text(wort, style = Schrift.Winzig, color = Farben.TextLeise.copy(alpha = inhaltAlpha), maxLines = 1)
                }
                if (!null0) {
                    Text(FMS_KURZ[wort] ?: wort, style = Schrift.Winzig, color = Farben.TextLeise.copy(alpha = inhaltAlpha), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (ausserDienst) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text("Grund für Status 6?", style = Schrift.MonoKlein, color = Farben.Text)
            Pillenreihe {
                AUSSER_DIENST.forEach { (text, minuten) ->
                    Pille(text, an = false, beiDruck = {
                        ausserDienst = false
                        beiFms(6, text, minuten)
                    })
                }
                Pille("Ohne Angabe", an = false, farbe = Farben.TextLeise, beiDruck = {
                    ausserDienst = false
                    beiFms(6, null, null)
                })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = eigenerGrund,
                    beiAenderung = { eigenerGrund = it.take(60) },
                    platzhalter = "Eigener Grund …",
                    weiterTaste = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
                Knopf("Melden", {
                    ausserDienst = false
                    beiFms(6, eigenerGrund.trim(), null)
                    eigenerGrund = ""
                }, aktiv = eigenerGrund.isNotBlank(), kompakt = true)
            }
        }
    }
}

/** Eine Hinweiszeile mit farbiger Kante links — `.anfahrt`, `.aufsatteln`, `.patient`. */
@Composable
private fun Streifenzeile(text: String, farbe: Color, fett: Boolean = false) {
    Text(
        text,
        style = Schrift.MonoKlein.copy(fontWeight = if (fett) FontWeight.Bold else FontWeight.Normal),
        color = farbe,
        modifier = Modifier
            .fillMaxWidth()
            .background(farbe.copy(alpha = 0.12f), Rundung.Winzig)
            .leuchtleiste(farbe, ecke = 6.dp, schein = 0f)
            .padding(start = Abstand.Klein + 2.dp, end = Abstand.Klein, top = Abstand.Winzig, bottom = Abstand.Winzig),
    )
}

// ---------------------------------------------------------- Lagemeldung

/** Lagemeldung an die Leitstelle — mit den Sätzen des Stichworts und dem Nachfordern. */
@Composable
fun ColumnScope.Lagemeldungskasten(einsatz: Einsatz?, katalog: Katalog?, beiLagemeldung: (String) -> Unit, befehle: Raumbefehle) {
    var text by remember { mutableStateOf("") }
    var nachforderungOffen by remember { mutableStateOf(false) }
    var nachforderung by remember { mutableStateOf("") }
    val vorschlaege = remember(einsatz?.stichwort, katalog) {
        einsatz?.stichwort?.let { s -> katalog?.stichworte?.firstOrNull { it.stichwort == s }?.lagemeldungen }.orEmpty()
    }

    Kasten(innenraum = Abstand.Klein, abstandInnen = Abstand.Klein) {
        Feld(
            wert = text,
            beiAenderung = { text = it.take(400) },
            etikett = "Lagemeldung an die Leitstelle",
            platzhalter = "Was findet ihr vor?",
            einzeilig = false,
        )
        if (vorschlaege.isNotEmpty()) {
            Pillenreihe { vorschlaege.forEach { v -> Pille(v, an = text == v, beiDruck = { text = v }) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf("Lagemeldung absetzen", {
                beiLagemeldung(text.trim())
                text = ""
            }, art = Knopfart.Haupt, aktiv = text.isNotBlank(), kompakt = true, modifier = Modifier.weight(1f))
            Knopf("Nachfordern", { nachforderungOffen = !nachforderungOffen }, kompakt = true)
        }
        if (nachforderungOffen) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = nachforderung,
                    beiAenderung = { nachforderung = it.take(200) },
                    platzhalter = "Was wird benötigt? (z. B. zweiter RTW, Drehleiter)",
                    weiterTaste = ImeAction.Send,
                    modifier = Modifier.weight(1f),
                )
                Knopf("Anfordern", {
                    befehle.nachfordern(nachforderung.trim().ifBlank { "Weitere Kräfte erforderlich." })
                    nachforderung = ""
                    nachforderungOffen = false
                }, art = Knopfart.Alarm, kompakt = true)
            }
        }
    }
}

// ------------------------------------------------------- Einsatzleitung

/**
 * Vor der Übernahme gibt es nur diesen einen Weg. Das ausführliche Tablet erscheint
 * erst, wenn dieses Führungsfahrzeug die Lage führt.
 */
@Composable
fun ColumnScope.EinsatzleitungPanel(einsatz: Einsatz?, meins: Rundenfahrzeug, beiUebernehmen: (String) -> Unit) {
    if (einsatz == null) return
    val medizinisch = meins.typ.lowercase() in setOf("orgl", "elrd", "lna")
    val zweig = if (einsatz.manv && medizinisch) "Rettungsdienst" else "Gesamt"
    val fuehrung = if (zweig == "Rettungsdienst") einsatz.einsatzleitungRd else einsatz.einsatzleitung
    val darf = if (zweig == "Rettungsdienst") medizinisch else meins.faehigkeiten.any { it.equals("Führung", true) }
    val gross = einsatz.prioritaet >= 3 || einsatz.manv || einsatz.alarmierteFahrzeuge.size >= 4
    if (!darf || (meins.status != 3 && meins.status != 4) || !gross || fuehrung != null) return

    Kasten(innenraum = Abstand.Klein, abstandInnen = Abstand.Klein) {
        Row {
            Knopf((if (zweig == "Rettungsdienst") "Einsatzleitung Rettungsdienst" else "Einsatzleitung") + " übernehmen", { beiUebernehmen(zweig) }, kompakt = true)
        }
        SehrLeise("Abschnitte, Aufträge und Nachforderungen stehen danach im Einsatz-Tablet.")
    }
}

/**
 * Die Versorgung im Fahrzeug — für OrgL, ELRD oder LNA vor Ort, solange sie nicht
 * selbst führen (dann steht sie im Tablet), und den ELW 2 mit der Triage.
 */
@Composable
fun ColumnScope.ManvVersorgung(
    raum: Raumzustand,
    einsatz: Einsatz?,
    meins: Rundenfahrzeug,
    manv: ManvGriffe,
    befehle: Raumbefehle = Raumbefehle.Leer,
) {
    if (einsatz == null || !einsatz.manv) return
    val fuehrtSelbst = einsatz.einsatzleitung == meins.funkrufname || einsatz.einsatzleitungRd == meins.funkrufname
    if (fuehrtSelbst) return
    val medizinisch = meins.typ.lowercase() in setOf("orgl", "elrd", "lna") && meins.status == 4 && meins.einsatzId == einsatz.id
    val elw2 = meins.typ.lowercase() == "elw 2" && meins.status == 4 && meins.einsatzstelleErreicht
    if (!medizinisch && !elw2) return
    ManvKasten(
        einsatz = einsatz,
        raum = raum,
        meins = meins,
        beiAnordnen = { art, g -> manv.anordnen(einsatz.id, art, g) },
        beiAbbauen = { s, ab -> manv.abbauen(einsatz.id, s, ab) },
        beiVerlegen = { p, s -> manv.verlegen(einsatz.id, p, s) },
        beiTransportmittel = { p, f -> manv.transportmittel(einsatz.id, p, f) },
        beiZielklinik = { p, k -> manv.zielklinik(einsatz.id, p, k) },
        beiTransport = { p -> manv.transport(einsatz.id, p) },
        beiVerstorbene = { manv.verstorbene(einsatz.id) },
        beiTriage = { manv.triage(einsatz.id) },
        beiBehandler = { p, f, na -> befehle.patientBehandlerZuweisen(einsatz.id, p, f, na) },
    )
}

// ------------------------------------------------------------- Dialoge

/** „Fahrzeug verlassen?" — was am Platz hängt, bevor er frei wird. */
@Composable
fun VerlassenDialog(meins: Rundenfahrzeug?, einsatz: Einsatz?, beiSchliessen: () -> Unit, beiAussteigen: () -> Unit) {
    Blende(
        titel = "Fahrzeug verlassen?",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Im Fahrzeug bleiben", beiSchliessen, art = Knopfart.Leise)
            Knopf("Aussteigen", beiAussteigen, art = Knopfart.Alarm)
        },
    ) {
        Text(
            "Dein Platz wird frei, und ${meins?.funkrufname.orEmpty()} fällt aus der laufenden Schicht — " +
                "die Leitstelle kann damit nicht mehr planen.",
            style = Schrift.Normal,
            color = Farben.Text,
        )
        if (einsatz != null && !einsatz.abgeschlossen) {
            Text("Du bist auf ${einsatz.einsatznummer} gebunden — der Einsatz läuft noch.", style = Schrift.MonoKlein, color = Farben.AmberHell)
        }
        SehrLeise("Was du bis hierher gefahren hast, wird beim Aussteigen gutgeschrieben. Was danach passiert, zählt für dich nicht mehr.")
    }
}

/** „Dienst beenden?" — nur, wenn die eigene Stimme die letzte ist, die fehlt. */
@Composable
fun DienstendeDialog(raum: Raumzustand, beiSchliessen: () -> Unit, beiBeenden: () -> Unit) {
    val jetzt = sekundentakt()
    val zuKurz = zeitpunktMs(raum.gestartetUm)?.let { jetzt - it < 5 * 60 * 1000 } == true
    Blende(
        titel = "Dienst beenden?",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Weiter im Dienst", beiSchliessen, art = Knopfart.Leise)
            Knopf("Dienst beenden", beiBeenden, art = Knopfart.Alarm)
        },
    ) {
        Text(
            if (raum.dienstendeSchwelle > 1) "Deine Stimme ist die letzte, die fehlt — die Schicht endet damit für alle im Raum."
            else "Die Schicht endet, und alle Einsätze werden abgerechnet.",
            style = Schrift.Normal,
            color = Farben.Text,
        )
        if (zuKurz) {
            Text("Die Schicht läuft noch keine fünf Minuten und bringt bis dahin keine Punkte.", style = Schrift.MonoKlein, color = Farben.SignalHell)
        }
    }
}

/** Die Breite einer Hinweiszeile unter dem Kopf — `.hinweis` aus FahrzeugView. */
@Composable
internal fun Hinweisleiste(text: String, farbe: Color, grund: Color, beiDruck: (() -> Unit)? = null, blinkt: Boolean = false) {
    val alpha = if (blinkt) {
        rememberInfiniteTransition(label = "hinweis").animateFloat(0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "hinweis-wert").value
    } else {
        1f
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(grund)
            .then(if (beiDruck != null) Modifier.clickable(role = Role.Button, onClick = beiDruck).defaultMinSize(minHeight = 44.dp) else Modifier)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        if (blinkt) Text("◉ ", style = Schrift.MonoKlein, color = farbe.copy(alpha = alpha))
        Text(text, style = Schrift.MonoKlein, color = farbe, modifier = Modifier.weight(1f, fill = false))
    }
}
