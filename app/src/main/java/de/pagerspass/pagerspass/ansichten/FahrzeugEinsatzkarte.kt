package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlin.math.roundToInt

/**
 * Die Einsatzkarte der Besatzung — `EinsatzKarte.vue`.
 *
 * Alles, was man auf der Anfahrt und an der Stelle wissen muss, <b>offen und nicht
 * hinter Aufklappern</b>: Wer auf der Anfahrt sitzt, hat eine Hand am Haltegriff,
 * und wer an der Stelle steht, eine am Gerät. Terminfahrt, Suchgebiet, Objekt,
 * Transportauftrag und -ziel, Löschwasser, Brandfläche, Aufgaben, die anderen
 * Kräfte und die letzten Lagemeldungen.
 */
@Composable
fun ColumnScope.FahrzeugEinsatzkarte(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    einsatz: Einsatz?,
    jetzt: Long,
    kennung: (Rundenfahrzeug) -> String,
    melderAmHandy: Boolean,
    beiWasser: () -> Unit,
    beiAufgabe: (Int) -> Unit,
) {
    if (einsatz == null) {
        Leerhinweis(
            "Kein Einsatzauftrag. " +
                if (melderAmHandy) "Der Melder liegt auf deinem Handy." else "Melder bleibt scharf.",
        )
        return
    }

    val prioFarbe = when {
        einsatz.prioritaet >= 3 -> Farben.Signal
        einsatz.prioritaet <= 1 -> Farben.Blau
        else -> Farben.FmsAnfahrt
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = prioFarbe.copy(alpha = 0.7f))
            .padding(Abstand.Normal),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), verticalAlignment = Alignment.Top) {
            Text(einsatz.stichwort, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = prioFarbe)
            Column(modifier = Modifier.weight(1f)) {
                Text(einsatz.stichwortText, style = Schrift.Gross, color = Farben.Text)
                SehrLeise("${einsatz.einsatznummer} · ${einsatzZustand(einsatz.state)}", mono = true)
            }
        }
        Text(
            einsatz.adresse + (einsatz.ortsteil?.let { " · $it" } ?: ""),
            style = Schrift.MonoNormal,
            color = Farben.AmberHell,
        )
        if (einsatz.meldebild.isNotBlank()) Text(einsatz.meldebild, style = Schrift.Normal, color = Farben.TextLeise)

        Terminfahrt(einsatz, jetzt)
        Suchgebiet(einsatz, meins)
        Objekt(einsatz)
        val transportziel = Transportauftrag(raum, einsatz, meins)
        Transportziel(raum, einsatz, meins, transportziel)
        Loeschwasser(raum, einsatz, meins, beiWasser)
        Brandflaeche(einsatz)
        Aufgaben(einsatz, meins, beiAufgabe)
        WeitereKraefte(raum, einsatz, meins, kennung)

        if (einsatz.lagemeldungen.isNotEmpty()) {
            Etikett("Bisherige Lagemeldungen")
            einsatz.lagemeldungen.takeLast(3).forEach { l ->
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    Text("${l.funkrufname}:", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                    Text(l.text, style = Schrift.Klein, color = Farben.Text)
                }
            }
        }
    }
}

/** Bestellte Fahrt: Termin und Zielhaus stehen von der Alarmierung an fest. */
@Composable
private fun Terminfahrt(e: Einsatz, jetzt: Long) {
    val termin = zeitMillis(e.terminUm) ?: return
    val rest = ((termin - jetzt) / 60_000.0).roundToInt()
    Abschnittsblock("Terminfahrt") {
        Text(
            "Abholung bis ${uhrzeit(e.terminUm.orEmpty())} — " + if (rest >= 0) "noch $rest min" else "überfällig",
            style = Schrift.MonoKlein,
            color = if (rest < 0) Farben.SignalHell else Farben.Text,
        )
        e.zielklinikVorgabe?.let { Text("Ziel $it", style = Schrift.MonoKlein, color = Farben.TextLeise) }
    }
}

/** Die eigene Suche — der Abschnitt steht offen: Wer im Gelände sucht, will wissen, wo er ist. */
@Composable
private fun Suchgebiet(e: Einsatz, f: Rundenfahrzeug) {
    if ((e.suchradiusMeter ?: 0.0) <= 0.0) return
    val abschnitt = f.suchabschnitt?.let { e.suchabschnitte.getOrNull(it) }
    Abschnittsblock("Suchgebiet") {
        val gesamt = (e.suchfortschritt * 100).roundToInt()
        Text(
            when {
                e.personGefundenUm != null -> "Person gefunden — Einsatzstelle ist jetzt die Fundstelle"
                abschnitt != null ->
                    "Abschnitt ${abschnitt.name} — ${(abschnitt.fortschritt * 100).roundToInt()} % abgesucht · Gebiet gesamt $gesamt %"
                else -> "Noch kein Abschnitt — die Leitstelle teilt zu, sonst der nächste freie."
            },
            style = Schrift.MonoKlein,
            color = Farben.Text,
        )
    }
}

/** Der Einsatzplan des Objekts — offen, nicht hinter einem Aufklapper. */
@Composable
private fun Objekt(e: Einsatz) {
    val name = e.objektName ?: return
    Abschnittsblock("Objekt") {
        Text(name + (e.objektartText?.let { " · $it" } ?: ""), style = Schrift.MonoKlein, color = Farben.Text)
        if (e.objektBetroffene > 0) {
            Text("etwa ${e.objektBetroffene} Personen im Objekt", style = Schrift.MonoKlein, color = Farben.AmberHell)
        }
        e.objektHinweise.forEach { Text("• $it", style = Schrift.Klein, color = Farben.TextLeise) }
    }
}

/**
 * Der Transportauftrag am Massenanfall: wer gemeint ist, wo er liegt, wohin er soll.
 *
 * @return ob das Ziel schon im Block „Transportziel" darunter steht.
 */
@Composable
private fun Transportauftrag(raum: Raumzustand, e: Einsatz, f: Rundenfahrzeug): Boolean {
    val klinikDa = f.status == 7 && raum.kliniken.any { it.id == f.zielklinikId }
    if (!e.manv) return klinikDa
    val p = e.manvPatienten.firstOrNull { it.transportVehicleId == f.id } ?: return klinikDa
    Abschnittsblock("Transportauftrag") {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text("Patient ${p.id}", style = Schrift.MonoKlein, color = Farben.Text)
            p.kategorie?.let { de.pagerspass.pagerspass.ui.bausteine.Marke(kategorieWort(it), farbe = kategorieTon(it)) }
            SehrLeise("— " + if (p.status == "ImTransport") "an Bord" else (p.stelle ?: "im Gelände"))
        }
        if (!klinikDa) {
            val klinik = p.zielklinikId?.let { id -> raum.kliniken.firstOrNull { it.id == id }?.name }
            SehrLeise(if (klinik != null) "Ziel $klinik" else "Ziel offen — die Einsatzleitung weist es zu.")
        }
    }
    return klinikDa
}

/**
 * Das Transportziel, sobald ein Patient an Bord ist (Status 7) — mit Entfernung
 * und Minuten, gerechnet wie die Anfahrt. Wer das Haus nennt, hängt an der Lage.
 */
@Composable
private fun Transportziel(raum: Raumzustand, e: Einsatz, f: Rundenfahrzeug, @Suppress("UNUSED_PARAMETER") klinikDa: Boolean) {
    if (f.status != 7) return
    val klinik = raum.kliniken.firstOrNull { it.id == f.zielklinikId }
    Abschnittsblock("Transportziel") {
        if (klinik == null) {
            Text(
                "Noch kein Ziel zugewiesen — " +
                    if (e.manv) "die Einsatzleitung nennt die Klinik." else "die Leitstelle nennt die Klinik.",
                style = Schrift.MonoKlein,
                color = Farben.AmberHell,
            )
        } else {
            val lat = f.lat
            val lon = f.lon
            val zusatz = when {
                f.zielklinikErreicht -> " — erreicht, Übergabe möglich (Status 8)"
                lat != null && lon != null -> {
                    val meter = restEntfernungMeter(lat, lon, f.route, f.routeIndex, klinik.lat, klinik.lon)
                    val prio = if (f.sondersignalAus) 1 else e.prioritaet
                    " — noch ${formatEntfernung(meter)} · ca. ${formatAnfahrtszeit(meter / tempoMs(f.organisation, prio, raum))}"
                }
                else -> " — Anfahrt läuft"
            }
            Text(klinik.name + zusatz, style = Schrift.MonoKlein, color = Farben.Text)
        }
    }
}

/** Der Löschwasserstand — nur mit Tank und nur, wenn die Runde mit Wasser rechnet. */
@Composable
private fun Loeschwasser(raum: Raumzustand, e: Einsatz, f: Rundenfahrzeug, beiWasser: () -> Unit) {
    if (f.tankLiter <= 0 || !raum.settings.loeschwasser) return
    val anteil = f.wasserLiter.toFloat() / f.tankLiter
    val abgebende = if (e.wasserbedarfProMinute > 0) {
        e.alarmierteFahrzeuge.mapNotNull { id -> raum.vehicles.firstOrNull { it.id == id } }.count { v ->
            v.status == 4 && v.einsatzstelleErreicht && v.tankLiter > 0 && v.wasserLiter > 0 &&
                v.faehigkeiten.contains("Löschangriff")
        }
    } else {
        0
    }
    val verbrauch = if (!e.wasserversorgungSteht && abgebende > 0 && f.status == 4 && f.wasserLiter > 0) {
        e.wasserbedarfProMinute.toDouble() / abgebende
    } else {
        0.0
    }
    val stellen = raum.entnahmestellen
    val lat = f.lat
    val lon = f.lon
    val ziel = f.entnahmestelleId?.let { id -> stellen.firstOrNull { it.id == id } }
        ?: if (lat != null && lon != null) stellen.minByOrNull { distanzMeter(lat, lon, it.lat, it.lon) } else null
    val zielWeg = if (ziel != null && lat != null && lon != null) formatEntfernung(distanzMeter(lat, lon, ziel.lat, ziel.lon)) else null
    val kannHolen = f.status == 4 && f.entnahmestelleId == null && anteil < 0.99f &&
        !e.wasserversorgungSteht && stellen.isNotEmpty()

    Abschnittsblock("Löschwasser") {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(Rundung.Rund)
                .background(Farben.BgTief),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(anteil.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(if (anteil <= 0.35f) Farben.Signal else Farben.Blau, Rundung.Rund),
            )
        }
        val zeile = "${f.wasserLiter} l von ${f.tankLiter} l" + when {
            f.entnahmestelleErreicht -> " — wird aufgefüllt" + (ziel?.let { " (${it.name})" } ?: "")
            f.entnahmestelleId != null -> " — unterwegs zu ${ziel?.name ?: "Entnahmestelle"}" + (zielWeg?.let { ", noch $it" } ?: "")
            e.wasserversorgungSteht -> " — Versorgung steht"
            verbrauch > 0 -> " — noch etwa ${maxOf(1, (f.wasserLiter / verbrauch).roundToInt())} min"
            else -> ""
        }
        Text(zeile, style = Schrift.MonoKlein, color = Farben.Text)
        if (kannHolen) {
            Knopf(
                "Wasser aufnehmen" + (ziel?.let { " · ${it.name}" + (zielWeg?.let { w -> " · $w" } ?: "") } ?: ""),
                beiWasser,
                art = Knopfart.Normal,
                kompakt = true,
            )
        }
    }
}

/** „3,5" oder „12" — Hektar, wie sie überall im Spiel geschrieben werden. */
internal fun hektarText(hektar: Double): String =
    if (hektar >= 10) hektar.roundToInt().toString() else "%.1f".format(java.util.Locale.GERMAN, hektar)

/** Die brennende Fläche — die eine Zahl, die schlechter wird, während man daneben steht. */
@Composable
private fun Brandflaeche(e: Einsatz) {
    if (e.brandflaecheHektar <= 0.0) return
    val waechst = e.brandflaecheHektar >= e.brandflaecheHoechstHektar
    Abschnittsblock("Brandfläche") {
        Text(
            "${hektarText(e.brandflaecheHektar)} ha — " +
                if (waechst) "breitet sich weiter aus" else "Höchststand ${hektarText(e.brandflaecheHoechstHektar)} ha",
            style = Schrift.MonoKlein,
            color = if (waechst) Farben.SignalHell else Farben.Text,
        )
    }
}

/**
 * Die Aufgaben der Einsatzstelle aus Sicht dieses Fahrzeugs. Übernehmen darf man
 * erst an der Stelle — wer auf der Anfahrt sitzt, verteilt keine Trupps.
 */
@Composable
private fun Aufgaben(e: Einsatz, f: Rundenfahrzeug, beiAufgabe: (Int) -> Unit) {
    if (e.aufgaben.isEmpty()) return
    val vorOrt = f.status == 4 && f.einsatzstelleErreicht
    val wartend = e.aufgaben.filter { !it.fertig && !it.begonnen }
    val dran = e.aufgaben.filter { !it.fertig && it.begonnen }
    val zuerst = if (wartend.isNotEmpty()) dran.firstOrNull()?.name else null
    val fertig = e.aufgaben.count { it.fertig }

    Abschnittsblock("Aufgaben · $fertig von ${e.aufgaben.size} erledigt") {
        if (e.arbeitFertigUm != null) {
            Text("Einsatzstelle abgearbeitet — einrücken möglich (Status 1 oder 2)", style = Schrift.MonoKlein, color = Farben.GruenHell)
        } else if (zuerst != null) {
            SehrLeise("Zuerst: $zuerst — das Übrige ist danach an der Reihe.")
        }
        e.aufgaben.forEach { a ->
            val meine = f.aufgabe == a.nummer
            val wartet = !a.fertig && !a.begonnen
            val prozent = (a.fortschritt * 100).roundToInt()
            val waehlbar = vorOrt && !a.fertig && a.begonnen && f.aufgabe != a.nummer &&
                (!a.zwingend || a.faehigkeit == null || f.faehigkeiten.contains(a.faehigkeit))
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (meine) Farben.HauchAmber else Color.Transparent, Rundung.Winzig)
                    .padding(Abstand.Winzig),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(
                        a.name,
                        style = Schrift.Klein.copy(fontWeight = if (meine) FontWeight.Bold else FontWeight.Normal),
                        color = if (wartet) Farben.TextSehrLeise else Farben.Text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(if (a.fertig) "erledigt" else "$prozent %", style = Schrift.MonoKlein, color = Farben.TextLeise)
                }
                Fortschritt(anteil = a.fortschritt.toFloat())
                when {
                    a.kraefteFehlen -> Text("Dafür sind wir zu wenige — ${a.mindestkraefte} Einsatzkräfte nötig.", style = Schrift.Klein, color = Farben.AmberHell)
                    a.mittelFehlt -> Text("Dafür fehlt uns: ${a.faehigkeit ?: a.name}", style = Schrift.Klein, color = Farben.AmberHell)
                }
                if (waehlbar) Knopf("Übernehmen", { beiAufgabe(a.nummer) }, kompakt = true)
            }
        }
    }
}

/** Die anderen auf demselben Einsatz — Status und Kennung. */
@Composable
private fun WeitereKraefte(raum: Raumzustand, e: Einsatz, f: Rundenfahrzeug, kennung: (Rundenfahrzeug) -> String) {
    val andere = e.alarmierteFahrzeuge.mapNotNull { id -> raum.vehicles.firstOrNull { it.id == id } }.filter { it.id != f.id }
    if (andere.isEmpty()) return
    Abschnittsblock("Weitere Kräfte") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
            andere.forEach { v ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .background(Farben.FlaecheHoch, Rundung.Winzig)
                        .padding(horizontal = Abstand.Klein, vertical = 2.dp),
                ) {
                    Text(v.status.toString(), style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = fmsFarbe(v.status))
                    Text(kennung(v), style = Schrift.MonoKlein, color = Farben.Text)
                }
            }
        }
    }
}

@Composable
private fun Abschnittsblock(etikett: String, inhalt: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth()) {
        Etikett(etikett)
        inhalt()
    }
}

/** Das Wort einer Sichtungskategorie. */
internal fun kategorieWort(k: String): String = when (k) {
    "Gruen" -> "Grün"
    else -> k
}

/** Die Farbe einer Sichtungskategorie. */
internal fun kategorieTon(k: String?): Color = when (k) {
    "Rot" -> Color(0xFFE5484D)
    "Gelb" -> Farben.Amber
    "Gruen" -> Color(0xFF2F9E44)
    "Schwarz" -> Color(0xFF6B7684)
    else -> Farben.TextSehrLeise
}
