package de.pagerspass.pagerspass.ansichten

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.netz.Atemschutztrupp
import de.pagerspass.pagerspass.netz.Aufgabe
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kapselreihe
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Markenzahl
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.kopfverlauf

/**
 * Das Einsatz-Tablet — der Arbeitsplatz der übernommenen Einsatzführung, übertragen
 * aus `EinsatzleiterTablet.vue` und den Seiten in `tablet/` (Web 5.0.0.26) in seine Handyform.
 *
 * <b>Im Fahrzeug steht nur die Karte, die das Tablet öffnet.</b> Sie trägt, was man
 * auch ohne Öffnen wissen muss: Einsatzdauer, Kräfte, wer unter Atemschutz ist und
 * ob Nachforderungen warten. Geöffnet liegt das Tablet über dem Fahrzeug, mit
 * eigenem Kopf, der Leiste unten (Lage · Kräfte · ASÜ · Funk · Mehr) und „Melden"
 * für Lagemeldung und Nachforderung.
 *
 * <b>Vor der Übernahme gibt es nur einen Weg</b> — den Knopf `Einsatzleitung
 * übernehmen` (`EinsatzleitungPanel`). Abschnitte, Aufträge und Nachforderungen
 * stehen erst danach hier.
 */

/** Die Fahrzeugtypen, die vor Ort medizinisch führen — wie `Manv.Fuehrungstypen`. */
private val MEDIZINISCHE_FUEHRUNG = setOf("orgl", "elrd", "lna")

/** „1/1/6" — Führung und Mannschaft aus der Besatzungsangabe. */
private fun staerke(besatzung: String): Pair<Int, Int> {
    val teile = besatzung.split('/').mapNotNull { it.trim().toIntOrNull() }
    if (teile.isEmpty()) return 0 to 0
    val mannschaft = if (teile.size > 1) teile.last() else 0
    val fuehrung = teile.dropLast(1).sum().takeIf { it > 0 } ?: if (teile.size == 1) teile[0] else 0
    return fuehrung to mannschaft
}

/** Die Lage aus Sicht des Tablets — `useTabletLage()` im Web. */
private class Tabletlage(val raum: Raumzustand, val einsatz: Einsatz, val meins: Rundenfahrzeug) {
    val alleKraefte = einsatz.alarmierteFahrzeuge.mapNotNull { id -> raum.vehicles.firstOrNull { it.id == id } }
        .filter { it.einsatzId == einsatz.id }
    val zweig: String? = when (meins.funkrufname) {
        einsatz.einsatzleitung -> "Gesamt"
        einsatz.einsatzleitungRd -> "Rettungsdienst"
        else -> null
    }
    val rettungsdienst = zweig == "Rettungsdienst"
    val zweigname = if (rettungsdienst) "Einsatzleitung Rettungsdienst" else "Einsatzleitung"
    val andereFuehrung = if (rettungsdienst) einsatz.einsatzleitung else einsatz.einsatzleitungRd
    val vertritt = zweig != null && andereFuehrung == null
    fun zweigVon(f: Rundenfahrzeug) = if (f.organisation == "Rettungsdienst") "Rettungsdienst" else "Gesamt"
    fun fuehrtKraft(f: Rundenfahrzeug) = zweig != null && (vertritt || zweigVon(f) == zweig)
    fun eigeneAufgabe(a: Aufgabe) = zweig == null || vertritt || a.rettungsdienst == rettungsdienst
    val kraefte = alleKraefte.filter(::fuehrtKraft)
    val abschnitte = if (rettungsdienst) einsatz.abschnitteRd else einsatz.abschnitte
    val offeneNachforderungen = if (rettungsdienst) einsatz.offeneNachforderungenRd else einsatz.offeneNachforderungen
    fun abschnittVon(rufname: String) = abschnitte.firstOrNull { rufname in it.funkrufnamen }?.name
    fun haelt(rufname: String) = rufname in einsatz.inBereitstellung
    val anfahrt = kraefte.filter { it.status == 3 }
    val vorOrt = kraefte.filter { it.status == 4 }
    val transport = kraefte.filter { it.status == 7 || it.status == 8 }
    val gesamtstaerke = kraefte.map { staerke(it.besatzung) }.fold(0 to 0) { s, f -> (s.first + f.first) to (s.second + f.second) }
    val fehlend: List<String> = run {
        val da = alleKraefte.flatMap { f -> f.faehigkeiten.map { it.lowercase() } }.toSet()
        einsatz.empfohleneFaehigkeiten.filter { it.lowercase() !in da }
    }
    fun aufgabeVon(f: Rundenfahrzeug) = f.aufgabe?.let { n -> einsatz.aufgaben.firstOrNull { it.nummer == n } }
    val kuerzel = when {
        rettungsdienst -> "EL RD"
        meins.organisation == "Feuerwehr" -> "EL FW"
        else -> "EL"
    }
}

private const val EINSATZZEIT = 30 * 60_000L

private fun atemschutzLage(t: Atemschutztrupp, jetzt: Long): Pair<String, Color> {
    if (t.stand == "Rueckzug") return "Auf dem Rückweg" to Farben.Amber
    val start = zeitpunktMs(t.angeschlossenUm) ?: jetzt
    val zeit = (zeitpunktMs(t.abgelegtUm) ?: jetzt) - start
    val letzte = (zeitpunktMs(t.letzteMeldungUm) ?: start) - start
    return when {
        zeit >= EINSATZZEIT -> "Einsatzzeit überschritten — sofort zurückholen" to Farben.Signal
        t.druck <= 150 -> "Halber Druck — Rückzug antreten" to Farben.Signal
        zeit >= EINSATZZEIT * 2 / 3 && letzte < EINSATZZEIT * 2 / 3 -> "2/3 der Zeit — Druck abfragen" to Farben.Amber
        zeit >= EINSATZZEIT / 3 && letzte < EINSATZZEIT / 3 -> "1/3 der Zeit — Druck abfragen" to Farben.Amber
        else -> "Im Einsatz" to Farben.GruenHell
    }
}

private enum class Tabletseite(val titel: String, val kurz: String, val pfad: String) {
    Lage("Lage", "Lage", "M12 21s-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11zM12 12.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5"),
    Struktur("Kräfte", "Kräfte", "M4 5h4v14H4zM10 5h4v9h-4zM16 5h4v12h-4z"),
    Atemschutz("Atemschutz", "ASÜ", "M12 22a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM12 10v4l2.5 1.5M9.5 2h5"),
    Versorgung("Versorgung", "Versorgung", "M9 4h6v5h5v6h-5v5H9v-5H4V9h5z"),
    Raum("Raum", "Raum", "M4 20V8l8-4 8 4v12M9 20v-6h6v6"),
    Funk("Funk", "Funk", "M5 11a7 7 0 0 1 14 0M8.5 11a3.5 3.5 0 0 1 7 0M12 11v10"),
    Tagebuch("Tagebuch", "Tagebuch", "M6 3h12v18H6zM9 8h6M9 12h6M9 16h3"),
    ;

    val zeichen: ImageVector by lazy { fahrzeugzeichen("tb-$name", pfad) }
}

private val ZEICHEN_MEHR = fahrzeugzeichen("tb-mehr", "M5 12h.01M12 12h.01M19 12h.01", staerke = 3f)
private val ZEICHEN_MELDEN = fahrzeugzeichen("tb-melden", "M4 12h3l3-7 4 14 3-7h3")

/**
 * Die Karte im Fahrzeug, die das Tablet öffnet — nur, wenn dieses Fahrzeug die Lage
 * führt.
 */
@Composable
fun ColumnScope.Tabletstarter(raum: Raumzustand, einsatz: Einsatz, meins: Rundenfahrzeug, manv: ManvGriffe, befehle: Raumbefehle) {
    val lage = Tabletlage(raum, einsatz, meins)
    if (lage.zweig == null) return
    var offen by remember(einsatz.id) { mutableStateOf(false) }
    val jetzt = sekundentakt()
    val dauer = dauerText(jetzt - (zeitpunktMs(einsatz.erstAlarmUm ?: einsatz.eingangUm) ?: jetzt))
    val unterPa = einsatz.atemschutztrupps.filter { it.stand != "Abgelegt" }
    val paAlarm = unterPa.any { atemschutzLage(it, jetzt).second == Farben.Signal }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = Farben.AmberTief)
            .clickable(role = Role.Button) { offen = true }
            .padding(Abstand.Normal),
    ) {
        Stichwortmarke(einsatz)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            Text("Einsatz-Tablet · ${lage.kuerzel}", style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Text(einsatz.stichwortText.ifBlank { einsatz.adresse }, style = Schrift.Klein, color = Farben.TextLeise, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                val mono = Schrift.Winzig.copy(fontFamily = Schrift.Mono)
                Text(dauer, style = mono, color = Farben.TextLeise)
                Text("${lage.kraefte.size} Kräfte", style = mono, color = Farben.TextLeise)
                if (unterPa.isNotEmpty() && !lage.rettungsdienst) Text("${unterPa.size} unter PA", style = mono, color = if (paAlarm) Farben.SignalHell else Farben.TextLeise)
                if (lage.offeneNachforderungen.isNotEmpty()) Text("${lage.offeneNachforderungen.size} Nachf.", style = mono, color = Farben.AmberHell)
            }
        }
        Text("Öffnen", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
    }

    if (offen) Tablet(lage, jetzt, manv, befehle, beiSchliessen = { offen = false })
}

@Composable
private fun Stichwortmarke(einsatz: Einsatz) {
    Text(
        einsatz.stichwort,
        style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
        color = Farben.AufFarbe,
        modifier = Modifier
            .background(if (einsatz.organisation == "Rettungsdienst") Farben.OrgRettungsdienst else Farben.Signal, Rundung.Winzig)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
    )
}

@Composable
private fun Tablet(lage: Tabletlage, jetzt: Long, manv: ManvGriffe, befehle: Raumbefehle, beiSchliessen: () -> Unit) {
    val einsatz = lage.einsatz
    var seite by remember(einsatz.id) { mutableStateOf(Tabletseite.Lage) }
    var blatt by remember { mutableStateOf<String?>(null) }
    var meldeart by remember { mutableStateOf("lage") }

    val unterPa = einsatz.atemschutztrupps.filter { it.stand != "Abgelegt" }
    val paAlarm = unterPa.any { atemschutzLage(it, jetzt).second == Farben.Signal }
    val seiten = buildList {
        add(Tabletseite.Lage)
        add(Tabletseite.Struktur)
        if (!lage.rettungsdienst) add(Tabletseite.Atemschutz)
        if (einsatz.manv && (lage.rettungsdienst || lage.vertritt)) add(Tabletseite.Versorgung)
        add(Tabletseite.Raum)
        add(Tabletseite.Funk)
        add(Tabletseite.Tagebuch)
    }
    if (seite !in seiten) seite = Tabletseite.Lage
    val vorn = seiten.filter {
        it in setOf(Tabletseite.Lage, Tabletseite.Struktur, Tabletseite.Atemschutz, Tabletseite.Funk) ||
            (it == Tabletseite.Versorgung && lage.rettungsdienst)
    }
    val hinten = seiten - vorn.toSet()
    fun zahl(s: Tabletseite): Int = when (s) {
        Tabletseite.Struktur -> lage.kraefte.size
        Tabletseite.Atemschutz -> unterPa.size
        Tabletseite.Funk -> lage.offeneNachforderungen.size
        else -> 0
    }
    fun warnt(s: Tabletseite) = (s == Tabletseite.Atemschutz && paAlarm) || (s == Tabletseite.Funk && lage.offeneNachforderungen.isNotEmpty())

    Dialog(onDismissRequest = beiSchliessen, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val rand = WindowInsets.safeDrawing.asPaddingValues()
        Box(
            Modifier
                .fillMaxSize()
                .background(Farben.Bg)
                .padding(top = rand.calculateTopPadding(), bottom = rand.calculateBottomPadding()),
        ) {
            Column(Modifier.fillMaxSize()) {
                // ------------------------------------------------------ App-Kopf
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().kopfverlauf().padding(horizontal = Abstand.Klein, vertical = Abstand.Klein),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(Ziel.Normal)
                            .clickable(role = Role.Button, onClickLabel = "Zurück zum Fahrzeug", onClick = beiSchliessen),
                    ) {
                        Icon(Fahrzeugzeichen.Zurueck, contentDescription = "Tablet schließen", tint = Farben.Text, modifier = Modifier.size(22.dp))
                    }
                    Stichwortmarke(einsatz)
                    Column(Modifier.weight(1f)) {
                        Text(einsatz.stichwortText.ifBlank { einsatz.stichwort }, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(einsatz.adresse, style = Schrift.Winzig, color = Farben.TextLeise, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        dauerText(jetzt - (zeitpunktMs(einsatz.erstAlarmUm ?: einsatz.eingangUm) ?: jetzt)),
                        style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                        color = Farben.Amber,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Gross),
                    modifier = Modifier.fillMaxWidth().background(Farben.Flaeche).padding(horizontal = Abstand.Gross, vertical = Abstand.Winzig),
                ) {
                    val mono = Schrift.Winzig.copy(fontFamily = Schrift.Mono)
                    Text("${lage.kuerzel} ${kennungVon(lage.meins)}", style = mono, color = Farben.TextLeise)
                    val (fu, ma) = lage.gesamtstaerke
                    Text("Stärke $fu/$ma/${fu + ma}", style = mono, color = Farben.TextLeise)
                }

                // --------------------------------------------------------- Inhalt
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = Abstand.Gross, end = Abstand.Gross, top = Abstand.Gross, bottom = 72.dp),
                ) {
                    when (seite) {
                        Tabletseite.Lage -> TabletLage(lage, befehle)
                        Tabletseite.Struktur -> TabletStruktur(lage, manv, befehle)
                        Tabletseite.Atemschutz -> TabletAtemschutz(lage, jetzt, befehle)
                        Tabletseite.Versorgung -> ManvKasten(
                            einsatz = einsatz,
                            raum = lage.raum,
                            meins = lage.meins,
                            beiAnordnen = { art, g -> manv.anordnen(einsatz.id, art, g) },
                            beiAbbauen = { s, ab -> manv.abbauen(einsatz.id, s, ab) },
                            beiVerlegen = { p, s -> manv.verlegen(einsatz.id, p, s) },
                            beiTransportmittel = { p, f -> manv.transportmittel(einsatz.id, p, f) },
                            beiZielklinik = { p, k -> manv.zielklinik(einsatz.id, p, k) },
                            beiTransport = { p -> manv.transport(einsatz.id, p) },
                            beiVerstorbene = { manv.verstorbene(einsatz.id) },
                            beiTriage = { manv.triage(einsatz.id) },
                        )
                        Tabletseite.Raum -> TabletRaum(lage, befehle)
                        Tabletseite.Funk -> TabletFunk(lage, befehle)
                        Tabletseite.Tagebuch -> TabletTagebuch(lage)
                    }
                }

                // ------------------------------------------------ Leiste unten
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Farben.Flaeche)
                        .border(1.dp, Farben.Rand),
                ) {
                    vorn.forEach { s ->
                        Unterweg(s.zeichen, s.kurz, seite == s && blatt != "mehr", zahl(s), warnt(s)) {
                            seite = s
                            blatt = null
                        }
                    }
                    if (hinten.isNotEmpty()) {
                        Unterweg(ZEICHEN_MEHR, "Mehr", blatt == "mehr" || seite in hinten, 0, hinten.any(::warnt)) {
                            blatt = if (blatt == "mehr") null else "mehr"
                        }
                    }
                }
            }

            // ---------------------------------------------------------- Melden
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Abstand.Gross, bottom = 76.dp)
                    .background(if (blatt == "melden") Farben.AmberHell else Farben.Amber, Rundung.Rund)
                    .clickable(role = Role.Button) { blatt = if (blatt == "melden") null else "melden" }
                    .defaultMinSize(minHeight = Ziel.Normal)
                    .padding(horizontal = Abstand.Gross),
            ) {
                Icon(ZEICHEN_MELDEN, contentDescription = null, tint = Farben.AufAmber, modifier = Modifier.size(20.dp))
                Text("Melden", style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.AufAmber)
            }

            // ---------------------------------------------------------- Blätter
            if (blatt != null) {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Farben.Ueberlagerung)
                        .clickable(indication = null, interactionSource = null) { blatt = null },
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Farben.Flaeche, Rundung.LeisteOben)
                            .border(1.dp, Farben.RandHell, Rundung.LeisteOben)
                            .clickable(enabled = false) {}
                            .verticalScroll(rememberScrollState())
                            .padding(Abstand.Gross),
                    ) {
                        if (blatt == "melden") {
                            Kapselreihe(
                                seiten = listOf("lage", "nachforderung"),
                                gewaehlt = meldeart,
                                beiWahl = { meldeart = it },
                                aufschrift = { if (it == "lage") "Lagemeldung" else "Nachforderung" },
                            )
                            if (meldeart == "lage") {
                                Lagemeldungsformular(befehle, beiFertig = { blatt = null })
                            } else {
                                Nachforderungsformular(lage, befehle, beiFertig = { blatt = null })
                            }
                        } else {
                            Etikett("Weitere Seiten")
                            hinten.forEach { s ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = Ziel.Normal)
                                        .background(if (seite == s) Farben.FlaecheAktiv else Color.Transparent, Rundung.Klein)
                                        .clickable(role = Role.Button) {
                                            seite = s
                                            blatt = null
                                        }
                                        .padding(horizontal = Abstand.Normal),
                                ) {
                                    Icon(s.zeichen, contentDescription = null, tint = Farben.TextLeise, modifier = Modifier.size(20.dp))
                                    Text(s.titel, style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f))
                                    if (zahl(s) > 0) Markenzahl(zahl(s))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Unterweg(
    zeichen: ImageVector,
    name: String,
    an: Boolean,
    zahl: Int,
    warnt: Boolean,
    beiDruck: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 56.dp)
            .background(if (an) Farben.FlaecheAktiv else Color.Transparent)
            .clickable(role = Role.Tab, onClick = beiDruck)
            .padding(vertical = Abstand.Winzig),
    ) {
        Box {
            Icon(zeichen, contentDescription = null, tint = if (an) Farben.Amber else Farben.TextLeise, modifier = Modifier.size(22.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Haar), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = Schrift.Winzig.copy(fontWeight = if (an) FontWeight.Bold else FontWeight.Normal), color = if (an) Farben.Text else Farben.TextLeise, maxLines = 1)
            if (zahl > 0) {
                Text(zahl.toString(), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = if (warnt) Farben.SignalHell else Farben.AmberHell)
            } else if (warnt) {
                Text("!", style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
            }
        }
    }
}

/** Eine Karte im Tablet — `.tb-karte`: Etikett, Überschrift, rechts eine Pille. */
@Composable
private fun Tabletkarte(
    etikett: String?,
    titel: String?,
    pille: String? = null,
    pilleFarbe: Color = Farben.TextLeise,
    rechts: (@Composable () -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Kasten(innenraum = Abstand.Normal, abstandInnen = Abstand.Klein) {
        if (etikett != null || titel != null || pille != null || rechts != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Column(Modifier.weight(1f)) {
                    etikett?.let { Etikett(it) }
                    titel?.let { Text(it, style = Schrift.Gross.copy(fontWeight = FontWeight.Bold), color = Farben.Text) }
                }
                pille?.let { Marke(it, farbe = pilleFarbe) }
                rechts?.invoke()
            }
        }
        inhalt()
    }
}

// ------------------------------------------------------------------- Lage

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.TabletLage(lage: Tabletlage, befehle: Raumbefehle) {
    val e = lage.einsatz
    var erledigteZeigen by remember { mutableStateOf(false) }
    var ansetzenFuer by remember { mutableStateOf<Int?>(null) }

    // Sichtung: nur an einer MANV-Lage.
    if (e.manv) {
        val p = e.manvPatienten
        Kasten(innenraum = Abstand.Normal, abstandInnen = Abstand.Klein) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                listOf(
                    Triple("Rot", "SK I", Color(0xFFE5484D)),
                    Triple("Gelb", "SK II", Farben.Amber),
                    Triple("Gruen", "SK III", Farben.Gruen),
                    Triple("Schwarz", "SK IV", Color(0xFF6B7684)),
                ).forEach { (k, name, farbe) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).background(farbe.copy(alpha = 0.16f), Rundung.Klein).border(1.dp, farbe, Rundung.Klein).padding(Abstand.Winzig),
                    ) {
                        Text(p.count { it.kategorie == k }.toString(), style = Schrift.Titel.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Text)
                        Text(name, style = Schrift.Winzig, color = Farben.TextLeise)
                    }
                }
            }
            SehrLeise(
                e.betroffeneUngefaehr?.let { "ca. $it Betroffene · Vorsichtung läuft" }
                    ?: "${p.size} Patienten · ${p.count { it.kategorie == null }} ungesichtet · ${p.count { it.status == "Uebergeben" || it.status == "ImTransport" }} abtransportiert",
                mono = true,
            )
        }
    }

    // Die Schnittstelle zur anderen Führung.
    val andererZweig = if (lage.rettungsdienst) "EL Feuerwehr" else "EL Rettungsdienst"
    if (lage.andereFuehrung != null) {
        val fremde = lage.alleKraefte.count { !lage.fuehrtKraft(it) }
        Text(
            "$andererZweig ${lage.andereFuehrung} führt " + (if (fremde == 1) "eine Kraft" else "$fremde Kräfte") +
                if (lage.rettungsdienst) " · Raum, Atemschutz und Feuerwehrkräfte laufen dort." else " · Sichtung, Versorgung und Abtransport laufen dort.",
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
    } else if (e.manv && lage.kraefte.any { (it.organisation == "Rettungsdienst") != lage.rettungsdienst }) {
        Text(
            "Du führst ${if (lage.rettungsdienst) "die Feuerwehrkräfte" else "den Rettungsdienst"} mit, bis eine $andererZweig übernimmt.",
            style = Schrift.Klein,
            color = Farben.AmberHell,
        )
    }

    // Die Lage in vier Zahlen, bevor irgendein Text kommt.
    val aufgaben = e.aufgaben.filter { !it.entfallen && lage.eigeneAufgabe(it) }.sortedBy { it.rang }
    val offen = aufgaben.filter { !it.fertig }
    val erledigt = aufgaben.filter { it.fertig }
    val unterPa = e.atemschutztrupps.count { it.stand != "Abgelegt" }
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Blickzahl("vor Ort", lage.vorOrt.size)
        Blickzahl("Anfahrt", lage.anfahrt.size)
        if (!lage.rettungsdienst) Blickzahl("unter PA", unterPa, warnt = unterPa > 0) else Blickzahl("Transport", lage.transport.size)
        if (aufgaben.isNotEmpty()) Blickzahl("offen", offen.size, warnt = offen.any { it.zwingend }) else Blickzahl("im BR", e.inBereitstellung.size)
    }

    Tabletkarte("Meldebild", null) {
        Text(e.meldebild, style = Schrift.Normal, color = Farben.Text)
        val wasser = e.wasserlage
        if (e.objektName != null || e.objektHinweise.isNotEmpty() || e.gefahrgutlage || wasser != null) {
            e.objektName?.let { Text("Objekt $it" + (e.objektartText?.let { a -> " · $a" } ?: ""), style = Schrift.Klein, color = Farben.Text) }
            e.objektHinweise.forEach { Text("• $it", style = Schrift.Klein, color = Farben.TextLeise) }
            if (e.gefahrgutlage) {
                Text(
                    "Gefahrgut ${e.gefahrstoff ?: "unbekannter Stoff"} · Absperrung ${(e.absperrradiusMeter ?: 0.0).toInt()} m" +
                        if (!e.messtruppVorOrt) " · kein Messtrupp" else "",
                    style = Schrift.Klein,
                    color = Farben.SignalHell,
                )
            }
            if (wasser != null) {
                Text(
                    "Wasser ${e.wasserlageText ?: wasser} · " + (if (e.wasserversorgungSteht) "Versorgung steht" else "Versorgung steht noch nicht") +
                        if (e.wasserbedarfProMinute > 0) " · Bedarf ${e.wasserbedarfProMinute.toInt()} l/min" else "",
                    style = Schrift.Klein,
                    color = if (e.wasserversorgungSteht) Farben.GruenHell else Farben.AmberHell,
                )
            }
        }
    }

    Tabletkarte("Letzte Lagemeldung", null) {
        val letzte = e.lagemeldungen.lastOrNull()
        if (letzte == null) {
            SehrLeise("Noch keine Lagemeldung. Die erste gehört nach dem Erkunden an die Leitstelle.")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(uhrzeitKurz(letzte.zeit), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                Column {
                    Text(letzte.funkrufname, style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Text)
                    Text(letzte.text, style = Schrift.Klein, color = Farben.Text)
                }
            }
        }
    }

    // Die Transportlage: was wartet, was frei ist.
    if (e.manv && (lage.rettungsdienst || lage.vertritt)) {
        val wartend = e.manvPatienten.filter { it.status == "WartetAufTransport" }
        val rot = wartend.count { it.kategorie == "Rot" }
        val frei = lage.vorOrt.count { f -> "Transport" in f.faehigkeiten && e.manvPatienten.none { it.transportVehicleId == f.id } }
        val anfahrend = lage.anfahrt.count { "Transport" in it.faehigkeiten }
        Tabletkarte("Transportorganisation", "Wartet auf Transport", "$frei Wagen frei", if (rot > frei) Farben.SignalHell else Farben.GruenHell) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                SehrLeise("$rot rot", mono = true)
                SehrLeise("${wartend.count { it.kategorie == "Gelb" }} gelb", mono = true)
                SehrLeise("${wartend.count { it.kategorie == "Gruen" }} grün", mono = true)
                SehrLeise("$frei Wagen frei vor Ort", mono = true)
                SehrLeise("$anfahrend Wagen auf Anfahrt", mono = true)
                SehrLeise("${lage.vorOrt.count { "Notarzt" in it.faehigkeiten }} Notärzte vor Ort", mono = true)
            }
            if (rot > frei + anfahrend) {
                Text("Mehr rote Patienten als Wagen. Rettungsmittel nachfordern oder einen RTH anfordern.", style = Schrift.Klein, color = Farben.SignalHell)
            }
        }
    }

    // Die Aufträge der Einsatzstelle.
    if (aufgaben.isNotEmpty()) {
        val titel = when {
            lage.vertritt -> "Aufgaben"
            lage.rettungsdienst -> "Aufgaben Rettungsdienst"
            else -> "Aufgaben Feuerwehr"
        }
        Tabletkarte("Einsatzauftrag", titel, "${erledigt.size}/${aufgaben.size} erledigt") {
            (if (erledigteZeigen) offen + erledigt else offen).forEach { a ->
                val arbeiter = lage.alleKraefte.filter { it.aufgabe == a.nummer }
                val ansetzbar = lage.vorOrt.filter { f ->
                    f.aufgabe != a.nummer && !lage.haelt(f.funkrufname) && "Führung" !in f.faehigkeiten &&
                        (a.faehigkeit == null || !a.zwingend || a.faehigkeit in f.faehigkeiten)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text(a.name, style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold), color = if (a.fertig) Farben.TextSehrLeise else Farben.Text, modifier = Modifier.weight(1f))
                        if (a.zwingend && !a.fertig) Marke("Pflicht", farbe = Farben.SignalHell)
                        Text(if (a.fertig) "erledigt" else "${(a.fortschritt * 100).toInt()} %", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
                    }
                    Laufbalken(if (a.fertig) 1f else a.fortschritt.toFloat(), farbe = if (a.fertig) Farben.Gruen else Farben.Amber)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when {
                                a.wartetAuf != null -> "Wartet auf: ${a.wartetAuf}"
                                arbeiter.isNotEmpty() -> arbeiter.joinToString(", ") { kennungVon(it) }
                                !a.fertig -> "Niemand angesetzt" + (a.faehigkeit?.let { " · braucht $it" } ?: "")
                                else -> ""
                            },
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                            modifier = Modifier.weight(1f),
                        )
                        if (!a.fertig && ansetzbar.isNotEmpty()) Knopf("+ Kraft", { ansetzenFuer = a.nummer }, kompakt = true)
                    }
                }
            }
            if (erledigt.isNotEmpty()) {
                Textweg(if (erledigteZeigen) "Erledigte ausblenden" else "${erledigt.size} erledigt · anzeigen", { erledigteZeigen = !erledigteZeigen }, farbe = Farben.TextLeise)
            }
        }
    }

    ansetzenFuer?.let { nummer ->
        val a = e.aufgaben.firstOrNull { it.nummer == nummer }
        val ansetzbar = lage.vorOrt.filter { f ->
            a != null && f.aufgabe != a.nummer && !lage.haelt(f.funkrufname) && "Führung" !in f.faehigkeiten &&
                (a.faehigkeit == null || !a.zwingend || a.faehigkeit in f.faehigkeiten)
        }
        Wahlblende(
            titel = "Kraft für ${a?.name.orEmpty()} ansetzen",
            gruppen = listOf(null to ansetzbar),
            aufschrift = { kennungVon(it) },
            unterschrift = { it.typ },
            beiWahl = {
                befehle.aufgabeZuteilen(it.id, nummer)
                ansetzenFuer = null
            },
            beiSchliessen = { ansetzenFuer = null },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Blickzahl(name: String, zahl: Int, warnt: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .flaeche(randfarbe = if (warnt) Farben.AmberTief else Farben.Rand, ecke = 9.dp, mitLichtkante = false)
            .padding(vertical = Abstand.Winzig),
    ) {
        Text(zahl.toString(), style = Schrift.Titel.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = if (warnt) Farben.AmberHell else Farben.Text)
        Text(name, style = Schrift.Winzig, color = Farben.TextLeise, maxLines = 1)
    }
}

// ----------------------------------------------------------------- Kräfte

@Composable
private fun ColumnScope.TabletStruktur(lage: Tabletlage, manv: ManvGriffe, befehle: Raumbefehle) {
    val e = lage.einsatz
    val zweig = lage.zweig ?: "Gesamt"
    var gewaehlt by remember(e.id) { mutableStateOf<String?>(null) }
    var neuerAbschnitt by remember(e.id) { mutableStateOf("") }
    var umbenennen by remember(e.id) { mutableStateOf<String?>(null) }
    var neuerName by remember { mutableStateOf("") }
    var auftragWahl by remember { mutableStateOf<String?>(null) }
    val g = lage.kraefte.firstOrNull { it.id == gewaehlt }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().flaeche(randfarbe = Farben.AmberTief).padding(Abstand.Normal),
    ) {
        Column(Modifier.weight(1f)) {
            Etikett(lage.zweigname)
            Text(kennungVon(lage.meins), style = Schrift.Normal.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Text)
        }
        Knopf("Führung an die Leitstelle zurückgeben", { manv.abgeben(e.id, zweig) }, art = Knopfart.Leise, kompakt = true)
    }

    // Aufträge übertragen — nur am RD-Zweig eines MANV.
    if (e.manv && zweig == "Rettungsdienst") {
        Tabletkarte("Führungsorganisation RD", "Aufträge übertragen") {
            listOf(
                Triple("Vorsichtung", "Erste Einteilung nach mSTaRT") { f: Rundenfahrzeug -> "Vorsichtung" in f.faehigkeiten },
                Triple("Sichtung", "Ärztliche Sichtung, SK I–IV") { f: Rundenfahrzeug -> "Sichtung" in f.faehigkeiten },
                Triple("Transportorganisation", "Patientenablage → Rettungsmittel → Klinik") { f: Rundenfahrzeug -> f.typ.lowercase() in MEDIZINISCHE_FUEHRUNG },
            ).forEach { (auftrag, text, _) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Column(Modifier.weight(1f)) {
                        Text(auftrag, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                        SehrLeise(text)
                    }
                    Knopf(e.manvauftraege[auftrag] ?: "— selbst —", { auftragWahl = auftrag }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
    }

    SehrLeise(if (g != null) "${kennungVon(g)} gewählt · Ziel unten wählen." else "Fahrzeug antippen, dann unten das Ziel wählen.")

    @Composable
    fun Spalte(titel: String, nummer: String?, liste: List<Rundenfahrzeug>, leer: String, beiUmbenennen: (() -> Unit)? = null) {
        Kasten(innenraum = Abstand.Klein, abstandInnen = Abstand.Winzig) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                nummer?.let { Text(it, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Farben.Amber) }
                Text(titel, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                if (beiUmbenennen != null) Textweg("Umbenennen", beiUmbenennen, farbe = Farben.TextLeise)
                Marke(liste.size.toString())
            }
            if (liste.isEmpty()) SehrLeise(leer)
            liste.forEach { f ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Ziel.Kompakt)
                        .flaeche(
                            farbe = if (gewaehlt == f.id) Farben.FlaecheAktiv else Farben.FlaecheHoch,
                            randfarbe = if (gewaehlt == f.id) Farben.Amber else Farben.Rand,
                            ecke = 9.dp,
                            mitLichtkante = false,
                        )
                        .clickable(role = Role.Button) { gewaehlt = if (gewaehlt == f.id) null else f.id }
                        .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                ) {
                    Text(
                        f.status.toString(),
                        style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                        color = Farben.AufFarbe,
                        modifier = Modifier.background(fmsFarbe(f.status), Rundung.Winzig).padding(horizontal = Abstand.Winzig),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(kennungVon(f), style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = if (f.id == lage.meins.id) Farben.AmberHell else Farben.Text)
                        Text(f.typ, style = Schrift.Winzig, color = Farben.TextSehrLeise)
                        lage.aufgabeVon(f)?.let { Text(it.name, style = Schrift.Winzig, color = Farben.AmberHell) }
                    }
                }
            }
        }
    }

    Spalte("Ohne Abschnitt", null, lage.kraefte.filter { lage.abschnittVon(it.funkrufname) == null && !lage.haelt(it.funkrufname) }, "Alle Kräfte sind eingeteilt.")
    lage.abschnitte.forEachIndexed { i, a ->
        Spalte(a.name, "EA ${i + 1}", lage.kraefte.filter { lage.abschnittVon(it.funkrufname) == a.name && !lage.haelt(it.funkrufname) }, "Noch niemand eingeteilt.") {
            umbenennen = a.name
            neuerName = a.name
        }
    }
    if (lage.abschnitte.size < 4) {
        Kasten(innenraum = Abstand.Klein, abstandInnen = Abstand.Klein) {
            Text("EA ${lage.abschnitte.size + 1} · Neuer Abschnitt", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = neuerAbschnitt,
                    beiAenderung = { neuerAbschnitt = it.take(40) },
                    platzhalter = if (zweig == "Rettungsdienst") "z. B. Patientenablage, Transport" else "z. B. Innenangriff, Wasserversorgung",
                    weiterTaste = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
                Knopf("Abschnitt bilden", {
                    manv.abschnittBilden(e.id, neuerAbschnitt.trim(), zweig)
                    neuerAbschnitt = ""
                }, aktiv = neuerAbschnitt.isNotBlank(), kompakt = true)
            }
            SehrLeise("Höchstens vier Abschnitte bleiben auf einem Fahrzeug-Tablet führbar.")
        }
    }
    if (e.bereitstellungsraum) {
        Spalte("Bereitstellungsraum", null, lage.kraefte.filter { lage.haelt(it.funkrufname) }, "Niemand hält im BR. Nur Kräfte in Status 4 lassen sich hineinstellen.")
    }

    // Die Ziele der gewählten Karte — am Handy unten statt Ziehen.
    if (g != null) {
        val eigen = g.id == lage.meins.id
        val ziele = buildList<Pair<String, () -> Unit>> {
            if (lage.abschnittVon(g.funkrufname) != null || lage.haelt(g.funkrufname)) {
                add("Ohne Abschnitt" to {
                    if (lage.haelt(g.funkrufname)) befehle.bereitstellungSetzen(e.id, g.id, false)
                    if (lage.abschnittVon(g.funkrufname) != null) manv.abschnittZuteilen(e.id, g.id, null, zweig)
                })
            }
            lage.abschnitte.forEachIndexed { i, a ->
                if (lage.abschnittVon(g.funkrufname) != a.name || lage.haelt(g.funkrufname)) {
                    add("EA ${i + 1} · ${a.name}" to {
                        if (lage.haelt(g.funkrufname)) befehle.bereitstellungSetzen(e.id, g.id, false)
                        manv.abschnittZuteilen(e.id, g.id, a.name, zweig)
                    })
                }
            }
            if (e.bereitstellungsraum && g.status == 4 && !eigen && !lage.haelt(g.funkrufname)) {
                add("Bereitstellungsraum" to { befehle.bereitstellungSetzen(e.id, g.id, true) })
            }
        }
        Tabletkarte(null, null) {
            Text("${kennungVon(g)} umsetzen nach", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            if (ziele.isEmpty()) SehrLeise("Kein Ziel frei.")
            Pillenreihe {
                ziele.forEach { (name, tun) ->
                    Pille(name, an = false, beiDruck = {
                        tun()
                        gewaehlt = null
                    })
                }
            }
            Textweg("Abbrechen", { gewaehlt = null }, farbe = Farben.TextLeise)
        }
    }

    umbenennen?.let { bisher ->
        de.pagerspass.pagerspass.ui.bausteine.Blende(
            titel = "Abschnitt umbenennen",
            beiSchliessen = { umbenennen = null },
            fuss = {
                Knopf("Abbrechen", { umbenennen = null }, art = Knopfart.Leise)
                Knopf("Übernehmen", {
                    if (neuerName.isNotBlank() && neuerName.trim() != bisher) befehle.abschnittUmbenennen(e.id, bisher, neuerName.trim(), zweig)
                    umbenennen = null
                }, art = Knopfart.Haupt)
            },
        ) {
            Feld(wert = neuerName, beiAenderung = { neuerName = it.take(40) }, etikett = "Name")
        }
    }

    auftragWahl?.let { auftrag ->
        val passend = lage.kraefte.filter { f ->
            when (auftrag) {
                "Vorsichtung" -> "Vorsichtung" in f.faehigkeiten
                "Sichtung" -> "Sichtung" in f.faehigkeiten
                else -> f.typ.lowercase() in MEDIZINISCHE_FUEHRUNG
            }
        }
        Wahlblende(
            titel = auftrag,
            gruppen = listOf(null to (listOf<Rundenfahrzeug?>(null) + passend)),
            aufschrift = { it?.let(::kennungVon) ?: "— selbst —" },
            beiWahl = {
                manv.auftrag(e.id, auftrag, it?.id)
                auftragWahl = null
            },
            beiSchliessen = { auftragWahl = null },
        )
    }
}

// ------------------------------------------------------------- Atemschutz

private val TRUPPS = listOf("Angriffstrupp", "Wassertrupp", "Schlauchtrupp", "Sicherheitstrupp")

@Composable
private fun ColumnScope.TabletAtemschutz(lage: Tabletlage, jetzt: Long, befehle: Raumbefehle) {
    val e = lage.einsatz
    val drin = e.atemschutztrupps.filter { it.stand != "Abgelegt" }
    val draussen = e.atemschutztrupps.filter { it.stand == "Abgelegt" }.asReversed()
    val traeger = lage.vorOrt.filter { "Atemschutz" in it.faehigkeiten && !lage.haelt(it.funkrufname) }
    var formularOffen by remember { mutableStateOf(false) }
    var fahrzeugWahl by remember { mutableStateOf<Rundenfahrzeug?>(null) }
    var fahrzeugAuswahl by remember { mutableStateOf(false) }
    var trupp by remember { mutableStateOf(TRUPPS.first()) }
    var auftrag by remember { mutableStateOf("") }
    val druckEingabe = remember { mutableStateMapOf<String, String>() }

    fun fahrzeugVon(t: Atemschutztrupp) = lage.raum.vehicles.firstOrNull { it.funkrufname == t.funkrufname }

    Tabletkarte("Atemschutzüberwachung", "${drin.size} unter PA", rechts = {
        Knopf(if (formularOffen) "Zuklappen" else "Anschließen", { formularOffen = !formularOffen }, kompakt = true)
    }) {
        if (formularOffen) {
            Wahlfeld("Fahrzeug", fahrzeugWahl?.let(::kennungVon), { fahrzeugAuswahl = true }, platzhalter = "Fahrzeug wählen …", aktiv = traeger.isNotEmpty())
            Etikett("Trupp")
            Pillenreihe { TRUPPS.forEach { t -> Pille(t, an = trupp == t, beiDruck = { trupp = t }) } }
            Feld(wert = auftrag, beiAenderung = { auftrag = it.take(40) }, etikett = "Auftrag", platzhalter = "z. B. Menschenrettung 2. OG")
            Row {
                Knopf("Anschließen · Uhr starten", {
                    val f = fahrzeugWahl ?: return@Knopf
                    befehle.atemschutzMelden(e.id, f.id, trupp, "Angeschlossen", auftrag.trim().ifBlank { null })
                    auftrag = ""
                    formularOffen = false
                }, art = Knopfart.Alarm, aktiv = fahrzeugWahl != null, kompakt = true)
            }
            if (traeger.isEmpty()) SehrLeise("Kein Fahrzeug mit Atemschutzgeräteträgern in Status 4 an der Einsatzstelle.")
        }
    }

    if (drin.any { it.trupp != "Sicherheitstrupp" } && drin.none { it.trupp == "Sicherheitstrupp" }) {
        Text(
            "Kein Sicherheitstrupp eingetragen. Nach FwDV 7 steht für jeden vorgehenden Trupp ein Sicherheitstrupp angeschlossen bereit.",
            style = Schrift.Klein,
            color = Farben.AmberHell,
        )
    }

    if (drin.isEmpty()) SehrLeise("Niemand unter Atemschutz. Ein Trupp erscheint hier, sobald er angeschlossen hat.")
    drin.forEach { t ->
        val (text, farbe) = atemschutzLage(t, jetzt)
        val schluessel = "${t.funkrufname}|${t.trupp}"
        val laufzeit = (zeitpunktMs(t.abgelegtUm) ?: jetzt) - (zeitpunktMs(t.angeschlossenUm) ?: jetzt)
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth().flaeche(randfarbe = farbe.copy(alpha = 0.7f)).padding(Abstand.Normal),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${t.trupp} · ${fahrzeugVon(t)?.let(::kennungVon) ?: t.funkrufname}", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    SehrLeise("${t.auftrag} · angeschlossen ${uhrzeitKurz(t.angeschlossenUm)}")
                }
                Text(dauerText(laufzeit), style = Schrift.Titel.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = farbe)
            }
            Laufbalken((laufzeit.toFloat() / EINSATZZEIT).coerceIn(0f, 1f), farbe = farbe)
            Row {
                Text(text, style = Schrift.Klein, color = farbe, modifier = Modifier.weight(1f))
                Text("${t.druck.toInt()} bar · ${uhrzeitKurz(t.letzteMeldungUm)}", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                if (t.stand == "ImEinsatz") {
                    Feld(
                        wert = druckEingabe[schluessel].orEmpty(),
                        beiAenderung = { druckEingabe[schluessel] = it.filter(Char::isDigit).take(3) },
                        platzhalter = "bar",
                        tastatur = KeyboardType.Number,
                        modifier = Modifier.width(76.dp),
                    )
                    Knopf("Druck", {
                        val f = fahrzeugVon(t) ?: return@Knopf
                        befehle.atemschutzMelden(e.id, f.id, t.trupp, "Druck", druckEingabe[schluessel])
                        druckEingabe[schluessel] = ""
                    }, aktiv = !druckEingabe[schluessel].isNullOrBlank(), kompakt = true)
                    Knopf("Rückzug", { fahrzeugVon(t)?.let { befehle.atemschutzMelden(e.id, it.id, t.trupp, "Rueckzug", null) } }, art = Knopfart.Alarm, kompakt = true)
                }
            }
            Row {
                Knopf("Draußen · abgelegt", { fahrzeugVon(t)?.let { befehle.atemschutzMelden(e.id, it.id, t.trupp, "Abgelegt", null) } }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }

    if (draussen.isNotEmpty()) {
        Tabletkarte("Abgelegt", null) {
            draussen.forEach { t ->
                val laufzeit = (zeitpunktMs(t.abgelegtUm) ?: 0L) - (zeitpunktMs(t.angeschlossenUm) ?: 0L)
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(uhrzeitKurz(t.abgelegtUm), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                    Text("${t.trupp} · ${t.funkrufname}", style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                    Text("${dauerText(laufzeit)} · ${t.druck.toInt()} bar", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
                }
            }
        }
    }

    if (fahrzeugAuswahl) {
        Wahlblende(
            titel = "Fahrzeug",
            gruppen = listOf(null to traeger),
            aufschrift = { kennungVon(it) },
            unterschrift = { it.typ },
            gewaehlt = fahrzeugWahl,
            beiWahl = {
                fahrzeugWahl = it
                fahrzeugAuswahl = false
            },
            beiSchliessen = { fahrzeugAuswahl = false },
        )
    }
}

// ------------------------------------------------------------------ Raum

@Composable
private fun ColumnScope.TabletRaum(lage: Tabletlage, befehle: Raumbefehle) {
    val e = lage.einsatz
    val ordnet = !lage.rettungsdienst || lage.vertritt
    val imBr = lage.alleKraefte.filter { lage.haelt(it.funkrufname) }

    Tabletkarte(
        "Ordnung des Raumes",
        "Bereitstellungsraum",
        if (e.bereitstellungsraum) "eingerichtet" else "keiner",
        if (e.bereitstellungsraum) Farben.GruenHell else Farben.TextLeise,
    ) {
        SehrLeise("Nachrückende Kräfte melden sich hier und warten auf einen Auftrag, statt die Zufahrt zu verstopfen.")
        if (!ordnet) Text("Die Flächen legt die EL Feuerwehr fest. Bedarf über Funk anmelden.", style = Schrift.Klein, color = Farben.AmberHell)
        when {
            !e.bereitstellungsraum && ordnet -> Row { Knopf("Bereitstellungsraum einrichten", { befehle.bereitstellungsraumFestlegen(e.id) }, kompakt = true) }
            imBr.isNotEmpty() -> imBr.forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(f.status.toString(), style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = fmsFarbe(f.status))
                    Text(kennungVon(f), style = Schrift.Klein.copy(fontFamily = Schrift.Mono), color = Farben.Text, modifier = Modifier.weight(1f))
                    if (lage.fuehrtKraft(f)) {
                        Textweg("Vorziehen", { befehle.bereitstellungSetzen(e.id, f.id, false) })
                    } else {
                        SehrLeise(if (f.organisation == "Rettungsdienst") "EL RD" else "EL FW")
                    }
                }
            }
            e.bereitstellungsraum -> SehrLeise("Niemand hält im BR. Umsetzen geht auf der Seite Kräfte.")
        }
    }

    val lp = e.landeplatz
    Tabletkarte(
        "Luftrettung",
        "Hubschrauberlandeplatz",
        lp?.let { if (it.hergerichtet) "hergerichtet" else "${(it.fortschritt * 100).toInt()} %" },
        if (lp?.hergerichtet == true) Farben.GruenHell else Farben.TextLeise,
    ) {
        if (lp == null) {
            if (ordnet) Row { Knopf("Landeplatz festlegen", { befehle.landeplatzFestlegen(e.id) }, kompakt = true) }
            else SehrLeise("Noch kein Landeplatz. Braucht der RTH einen, bei der EL FW anfragen.")
        } else {
            SehrLeise("Fläche frei und abgesperrt")
            Laufbalken(lp.fortschritt.toFloat(), farbe = if (lp.hergerichtet) Farben.Gruen else Farben.Amber)
            SehrLeise("Ausgeleuchtet · Nachtlandung")
            Laufbalken(lp.lichtfortschritt.toFloat(), farbe = if (lp.ausgeleuchtet) Farben.Gruen else Farben.Amber)
            if (ordnet && !lp.ausgeleuchtet && lp.lichtfortschritt == 0.0) {
                Row { Knopf("Ausleuchten anordnen", { befehle.landeplatzAusleuchten(e.id) }, kompakt = true) }
            }
            val gelandet = lage.alleKraefte.filter { "Lufttransport" in it.faehigkeiten && (it.status == 4 || it.status == 7) }
            if (gelandet.isEmpty()) SehrLeise("Noch kein Luftfahrzeug auf der Fläche.")
            gelandet.forEach { f -> Text("${f.status} ${kennungVon(f)} · gelandet", style = Schrift.Klein.copy(fontFamily = Schrift.Mono), color = Farben.Text) }
        }
    }
}

// ------------------------------------------------------------------ Funk

@Composable
private fun ColumnScope.TabletFunk(lage: Tabletlage, befehle: Raumbefehle) {
    val e = lage.einsatz
    val r = lage.raum
    var formularOffen by remember { mutableStateOf(false) }
    var neueGruppe by remember { mutableStateOf("") }
    var aufschaltenFuer by remember { mutableStateOf<String?>(null) }
    val zweig = lage.zweig ?: "Gesamt"
    val anLeitstelle = r.funkprotokoll
        .filter { it.kind == "Nachforderung" && it.incidentId == e.id && it.an == r.settings.leitstelle }
        .takeLast(6).asReversed()

    Tabletkarte(
        "Nach oben",
        "Leitstelle",
        "${lage.offeneNachforderungen.size} offen",
        if (lage.offeneNachforderungen.isNotEmpty()) Farben.AmberHell else Farben.TextLeise,
    ) {
        if (lage.offeneNachforderungen.isNotEmpty()) {
            Etikett("Bei der ${lage.zweigname} gesammelt")
            lage.offeneNachforderungen.forEach { f ->
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(f.funkrufname, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
                    Text(f.text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                }
            }
            Row {
                Knopf(
                    (if (lage.offeneNachforderungen.size == 1) "Nachforderung" else "${lage.offeneNachforderungen.size} Nachforderungen") + " gebündelt weiterreichen",
                    { befehle.nachforderungenWeiterreichen(e.id, zweig) },
                    art = Knopfart.Alarm,
                    kompakt = true,
                )
            }
        }
        if (formularOffen) {
            Nachforderungsformular(lage, befehle, beiFertig = { formularOffen = false })
        } else {
            Row { Knopf("Nachforderung schreiben", { formularOffen = true }, kompakt = true) }
            if (lage.fehlend.isNotEmpty()) Text("Fehlt an der Lage: ${lage.fehlend.joinToString(", ")}", style = Schrift.Klein, color = Farben.AmberHell)
        }
        if (anLeitstelle.isNotEmpty()) {
            Etikett("Schon übermittelt")
            anLeitstelle.forEach { m ->
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(m.von, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
                    Text(m.text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    val gruppen = r.settings.funkgruppen.filter { it.einsatzId == e.id }
    Tabletkarte("Nach unten", "Einsatzstellenfunk (DMO)", "${gruppen.size}/3") {
        if (gruppen.size < 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = neueGruppe,
                    beiAenderung = { neueGruppe = it.take(40) },
                    platzhalter = "Einsatzstelle ${gruppen.size + 1}",
                    modifier = Modifier.weight(1f),
                )
                Knopf("DMO öffnen", {
                    befehle.einsatzfunkgruppeOeffnen(e.id, neueGruppe.trim())
                    neueGruppe = ""
                }, kompakt = true)
            }
        }
        if (gruppen.isEmpty()) SehrLeise("Noch keine Einsatzfunkgruppe. Bis zu drei laufen parallel; die Einsatzleitung hört alle mit.")
        gruppen.forEach { g ->
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.fillMaxWidth().flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false).padding(Abstand.Klein),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(g.nummer, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.Amber)
                    Text(g.name, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    val sendet = r.sendegruppe == g.id
                    Textweg(if (sendet) "Sendekanal" else "Hier senden", { befehle.funkgruppeZuweisen(lage.meins.id, g.id) }, aktiv = !sendet)
                    Textweg("Schließen", { befehle.einsatzfunkgruppeSchliessen(g.id) }, farbe = Farben.TextLeise)
                }
                SehrLeise(lage.kraefte.filter { it.funkgruppe == g.id }.joinToString(", ") { kennungVon(it) }.ifBlank { "Noch niemand aufgeschaltet." })
                Row { Knopf("Kraft aufschalten", { aufschaltenFuer = g.id }, art = Knopfart.Leise, kompakt = true) }
            }
        }
    }

    aufschaltenFuer?.let { gruppeId ->
        Wahlblende(
            titel = "Kraft vor Ort aufschalten",
            gruppen = listOf(null to lage.vorOrt.filter { it.funkgruppe != gruppeId }),
            aufschrift = { kennungVon(it) },
            unterschrift = { it.typ },
            beiWahl = {
                befehle.funkgruppeZuweisen(it.id, gruppeId)
                aufschaltenFuer = null
            },
            beiSchliessen = { aufschaltenFuer = null },
        )
    }
}

// ---------------------------------------------------------------- Tagebuch

private val FUEHRUNGSWORTE = listOf("Einsatzleitung", "Abschnitt", "Zugeteilt", "Zuteilung", "Bereitstellung", "Landeplatz", "Nachforderung", "übernimmt")

@Composable
private fun ColumnScope.TabletTagebuch(lage: Tabletlage) {
    val e = lage.einsatz
    var filter by remember { mutableStateOf("alle") }
    Kapselreihe(
        seiten = listOf("alle", "fuehrung", "atemschutz", "lage"),
        gewaehlt = filter,
        beiWahl = { filter = it },
        aufschrift = {
            when (it) {
                "alle" -> "Alles"
                "fuehrung" -> "Führung"
                "atemschutz" -> "ASÜ"
                else -> "Lage"
            }
        },
    )
    val zeilen: List<Triple<String, String, String?>> = if (filter == "lage") {
        e.lagemeldungen.asReversed().map { Triple(it.zeit, it.text, it.funkrufname) }
    } else {
        e.chronologie.asReversed()
            .filter { c ->
                when (filter) {
                    "atemschutz" -> c.text.startsWith("[ASÜ]")
                    "fuehrung" -> FUEHRUNGSWORTE.any { it in c.text }
                    else -> true
                }
            }
            .map { Triple(it.zeit, it.text.replace(Regex("^\\[ASÜ] "), "Atemschutz · ").replace(Regex("^\\[RD] "), "RD · "), it.urheber) }
    }
    if (zeilen.isEmpty()) SehrLeise("Noch nichts eingetragen.")
    zeilen.forEach { (zeit, text, von) ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text(uhrzeitKurz(zeit), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise, modifier = Modifier.width(40.dp))
            Text(text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
            von?.let { Text(it, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise) }
        }
    }
}

// --------------------------------------------------------------- Melden

/** Die Lagemeldung in drei Feldern — „Ich sehe · Ich mache · Ich brauche". */
@Composable
private fun ColumnScope.Lagemeldungsformular(befehle: Raumbefehle, beiFertig: () -> Unit) {
    var sehe by remember { mutableStateOf("") }
    var mache by remember { mutableStateOf("") }
    var brauche by remember { mutableStateOf("") }
    val meldung = listOfNotNull(
        sehe.trim().ifBlank { null }?.let { "Lage: $it" },
        mache.trim().ifBlank { null }?.let { "Maßnahmen: $it" },
        brauche.trim().ifBlank { null }?.let { "Benötigen: $it" },
    ).joinToString(". ")
    Feld(wert = sehe, beiAenderung = { sehe = it.take(120) }, etikett = "Ich sehe", platzhalter = "Wohnungsbrand 2. OG, eine Person vermisst")
    Feld(wert = mache, beiAenderung = { mache = it.take(120) }, etikett = "Ich mache", platzhalter = "Menschenrettung unter PA, 2. Rettungsweg über DLK")
    Feld(wert = brauche, beiAenderung = { brauche = it.take(80) }, etikett = "Ich brauche", platzhalter = "nichts / 1 RTW / weitere AGT", weiterTaste = ImeAction.Done)
    SehrLeise(meldung.ifBlank { "Geht an die Leitstelle, in dieser Reihenfolge." })
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Knopf("Abbrechen", beiFertig, art = Knopfart.Leise, kompakt = true)
        Knopf("Absetzen", {
            befehle.lagemeldungAbsetzen(meldung)
            beiFertig()
        }, art = Knopfart.Alarm, aktiv = meldung.isNotBlank(), kompakt = true)
    }
}

@Composable
private fun ColumnScope.Nachforderungsformular(lage: Tabletlage, befehle: Raumbefehle, beiFertig: () -> Unit) {
    var text by remember { mutableStateOf(if (lage.fehlend.isNotEmpty()) "Benötigen: ${lage.fehlend.joinToString(", ")}." else "") }
    Etikett("Nachforderung an die Leitstelle")
    Feld(
        wert = text,
        beiAenderung = { text = it.take(220) },
        platzhalter = "Was, wie viel, wohin — z. B. 1 HLF und 2 AGT-Trupps zum Bereitstellungsraum Nord",
        einzeilig = false,
    )
    if (lage.fehlend.isNotEmpty()) Text("Fehlt an der Lage: ${lage.fehlend.joinToString(", ")}", style = Schrift.Klein, color = Farben.AmberHell)
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Knopf("Abbrechen", beiFertig, art = Knopfart.Leise, kompakt = true)
        Knopf("Senden", {
            befehle.nachfordern(text.trim())
            beiFertig()
        }, art = Knopfart.Alarm, aktiv = text.isNotBlank(), kompakt = true)
    }
}
