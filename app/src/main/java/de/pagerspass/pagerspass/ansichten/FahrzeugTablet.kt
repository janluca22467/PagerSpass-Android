package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.ManvPatient
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Führung vor Ort — `EinsatzleitungPanel.vue` und `EinsatzleiterTablet.vue` —
 * und der Patientenbogen (`PatientenFenster.vue`, `PatientBogen.vue`).
 *
 * <b>Die Führungsarbeit hat einen Ort, und das ist das Tablet.</b> Vor der Übernahme
 * steht nur der eine Knopf; danach verschwindet er, und alles Weitere — Lage, Kräfte,
 * Nachforderungen, Einsatzfunk, Abschnitte, Versorgung — steht in den Reitern des
 * Tablets. Alles hier ist Anzeige und Absicht; entschieden wird am Server.
 */

/** Die Fahrzeugtypen, die vor Ort medizinisch führen — wie `Manv.Fuehrungstypen`. */
internal val MEDIZINISCHE_FUEHRUNG = listOf("orgl", "elrd", "lna")

internal fun istMedizinischeFuehrung(f: Rundenfahrzeug): Boolean = f.typ.lowercase() in MEDIZINISCHE_FUEHRUNG

/**
 * Der kurze Weg zur Einsatzleitung — sichtbar nur für ein berechtigtes
 * Führungsfahrzeug an einer großen Lage, und nur, solange es noch nicht führt.
 * Status 3 oder 4 genügt: Das Tablet gibt auf der Anfahrt am meisten her.
 */
@Composable
fun EinsatzleitungPanel(einsatz: Einsatz, meins: Rundenfahrzeug, beiUebernehmen: (String) -> Unit) {
    val fuehrungsfahrzeug = meins.faehigkeiten.any { it.lowercase() == "führung" }
    val medizinisch = istMedizinischeFuehrung(meins)
    val zweig = if (einsatz.manv && medizinisch) "Rettungsdienst" else "Gesamt"
    val zweigname = if (zweig == "Rettungsdienst") "Einsatzleitung Rettungsdienst" else "Einsatzleitung"
    val fuehrung = if (zweig == "Rettungsdienst") einsatz.einsatzleitungRd else einsatz.einsatzleitung
    val darf = if (zweig == "Rettungsdienst") medizinisch else fuehrungsfahrzeug
    val kann = darf && (meins.status == 3 || meins.status == 4) && lageGrossGenug(einsatz) && fuehrung == null
    if (!kann) return

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        Knopf("$zweigname übernehmen", { beiUebernehmen(zweig) }, breit = true)
        SehrLeise("Abschnitte, Aufträge und Nachforderungen stehen danach im Einsatz-Tablet.")
    }
}

/** Wer auf einem Fahrzeug sitzt — Bot oder Mitspieler; für die Führung keine Nebensache. */
private fun besatzungsmarke(raum: Raumzustand, playerId: String?): Pair<Boolean, String>? {
    val p = raum.players.firstOrNull { it.id == playerId } ?: return null
    return if (p.istBot) false to "BOT" else true to p.name
}

/**
 * Das Einsatz-Tablet — der Arbeitsplatz der übernommenen Einsatzführung.
 *
 * Sechs Reiter wie im Web (Versorgung nur beim Massenanfall). Am Handy ist das
 * Tablet nicht ausheftbar; es steht als Gerät im Bedienteil.
 */
@Composable
fun EinsatzleiterTablet(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    einsatz: Einsatz,
    kennung: (Rundenfahrzeug) -> String,
    griffe: ManvGriffe,
    beiNachfordern: (String) -> Unit,
) {
    val zweig = when (meins.funkrufname) {
        einsatz.einsatzleitung -> "Gesamt"
        einsatz.einsatzleitungRd -> "Rettungsdienst"
        else -> null
    } ?: return
    val zweigname = if (zweig == "Rettungsdienst") "Einsatzleitung Rettungsdienst" else "Einsatzleitung"

    val kraefte = einsatz.alarmierteFahrzeuge.mapNotNull { id -> raum.vehicles.firstOrNull { it.id == id } }
        .filter { it.einsatzId == einsatz.id }
    val anfahrt = kraefte.filter { it.status == 3 }
    val vorOrt = kraefte.filter { it.status == 4 }
    val transport = kraefte.filter { it.status == 7 || it.status == 8 }
    val sonstige = kraefte.filter { it.status !in listOf(3, 4, 7, 8) }
    val abschnitte = if (zweig == "Rettungsdienst") einsatz.abschnitteRd else einsatz.abschnitte
    val offene = if (zweig == "Rettungsdienst") einsatz.offeneNachforderungenRd else einsatz.offeneNachforderungen
    val funkgruppen = raum.settings.funkgruppen.filter { it.einsatzId == einsatz.id }
    val vorhanden = kraefte.flatMap { k -> k.faehigkeiten.map { it.lowercase() } }.toSet()
    val fehlend = einsatz.empfohleneFaehigkeiten.filter { it.lowercase() !in vorhanden }

    var ansicht by remember(einsatz.id) { mutableStateOf("lage") }
    var nachalarmOffen by remember(einsatz.id) { mutableStateOf(false) }
    var nachforderungText by remember(einsatz.id) { mutableStateOf("") }
    val vorschlag = if (fehlend.isNotEmpty()) "Benötigen: ${fehlend.joinToString(", ")}." else "Weitere Kräfte erforderlich."

    fun nachalarmOeffnen() {
        ansicht = "nachforderungen"
        nachalarmOffen = true
        nachforderungText = vorschlag
    }

    fun abschnittVon(rufname: String): String? = abschnitte.firstOrNull { it.funkrufnamen.contains(rufname) }?.name

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF15181D), RoundedCornerShape(20.dp))
            .border(2.dp, Color(0xFF2B3038), RoundedCornerShape(20.dp))
            .padding(Abstand.Klein),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.Bg, RoundedCornerShape(14.dp))
                .padding(Abstand.Normal),
        ) {
            // Die Statusleiste des Tablets.
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Farben.GruenHell, CircleShape))
                Text("Einsatznetz", style = Schrift.MonoKlein, color = Farben.TextSehrLeise, modifier = Modifier.weight(1f))
                Text("LIVE · ${meins.funkrufname}", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Column(modifier = Modifier.weight(1f)) {
                    Etikett("Aktiver Einsatz")
                    Text(einsatz.stichwort, style = Schrift.Titel, color = Farben.Text)
                    Text(einsatz.adresse, style = Schrift.Klein, color = Farben.TextLeise)
                }
                Marke(einsatz.state, farbe = einsatzFarbe(einsatz.state))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Pille("Lage", an = ansicht == "lage", beiDruck = { ansicht = "lage" })
                Pille("Kräfte", an = ansicht == "kraefte", zahl = kraefte.size, beiDruck = { ansicht = "kraefte" })
                Pille("Leitstelle", an = ansicht == "nachforderungen", zahl = offene.size.takeIf { it > 0 }, beiDruck = { ansicht = "nachforderungen" })
                Pille("DMO", an = ansicht == "funk", zahl = funkgruppen.size.takeIf { it > 0 }, beiDruck = { ansicht = "funk" })
                Pille("Führung", an = ansicht == "fuehrung", beiDruck = { ansicht = "fuehrung" })
                if (einsatz.manv) Pille("Versorgung", an = ansicht == "versorgung", beiDruck = { ansicht = "versorgung" })
            }

            when (ansicht) {
                "kraefte" -> TabletKraefte(raum, kraefte, anfahrt, vorOrt, transport, sonstige, kennung)
                "nachforderungen" -> TabletNachforderungen(
                    raum = raum,
                    einsatz = einsatz,
                    offene = offene.map { it.funkrufname to it.text },
                    zweigname = zweigname,
                    nachalarmOffen = nachalarmOffen,
                    nachforderungText = nachforderungText,
                    beiText = { nachforderungText = it },
                    beiOeffnen = { nachalarmOeffnen() },
                    beiAbbrechen = { nachalarmOffen = false },
                    beiSenden = {
                        val text = nachforderungText.trim()
                        if (text.isNotEmpty()) {
                            beiNachfordern(text)
                            nachalarmOffen = false
                            nachforderungText = ""
                        }
                    },
                    beiWeiterreichen = { griffe.nachforderungenWeiterreichen(einsatz.id, zweig) },
                )
                "funk" -> TabletFunk(raum, meins, einsatz, kraefte, vorOrt, funkgruppen, kennung, griffe)
                "versorgung" -> {
                    Etikett("Massenanfall")
                    Text("Verletztenablage und Behandlungsplatz", style = Schrift.Gross, color = Farben.Text)
                    SehrLeise("${einsatz.versorgungsstellen.size} Stellen", mono = true)
                    ManvVersorgung(raum = raum, einsatz = einsatz, meins = meins, imTablet = true, griffe = griffe)
                }
                "fuehrung" -> TabletFuehrung(
                    raum = raum,
                    meins = meins,
                    einsatz = einsatz,
                    zweig = zweig,
                    zweigname = zweigname,
                    kraefte = kraefte,
                    vorOrt = vorOrt,
                    kennung = kennung,
                    griffe = griffe,
                    abschnittVon = ::abschnittVon,
                )
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Etikett("Lageübersicht")
                            Text(einsatz.meldebild, style = Schrift.Normal, color = Farben.Text)
                        }
                        Knopf("+ Nachfordern", { nachalarmOeffnen() }, kompakt = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
                        Zahlkachel(kraefte.size, "Alarmiert", Farben.Text, Modifier.weight(1f))
                        Zahlkachel(anfahrt.size, "Anfahrt", fmsFarbe(3), Modifier.weight(1f))
                        Zahlkachel(vorOrt.size, "Vor Ort", fmsFarbe(4), Modifier.weight(1f))
                        Zahlkachel(transport.size, "Transport", fmsFarbe(7), Modifier.weight(1f))
                    }
                    val manvText = manvZeile(einsatz)
                    manvText?.let { Text(it, style = Schrift.MonoKlein, color = Farben.AmberHell) }
                    if (fehlend.isNotEmpty()) {
                        Text("Fehlende Funktion: ${fehlend.joinToString(", ")}", style = Schrift.Klein, color = Farben.SignalHell)
                    }
                    if (manvText == null && fehlend.isEmpty()) SehrLeise("Aktuell sind keine besonderen Hinweise offen.")
                }
            }
        }
    }
}

/** „MANV 2 · 7 von 12 Patienten offen · 1 verstorben" — Verstorbene zählen nicht als offen. */
private fun manvZeile(e: Einsatz): String? {
    if (!e.manv) return null
    val stufe = e.manvStufe?.toString().orEmpty()
    e.betroffeneUngefaehr?.let { return "MANV $stufe · ca. $it Betroffene · Vorsichtung läuft" }
    val offen = e.manvPatienten.count { it.status != "Uebergeben" && it.kategorie != "Schwarz" }
    val tot = e.manvPatienten.count { it.kategorie == "Schwarz" }
    return "MANV $stufe · $offen von ${e.manvPatienten.size} Patienten offen" + if (tot > 0) " · $tot verstorben" else ""
}

@Composable
private fun Zahlkachel(zahl: Int, wort: String, farbe: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.flaeche(ecke = 9.dp).padding(vertical = Abstand.Klein),
    ) {
        Text(zahl.toString(), style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = farbe)
        Text(wort, style = Schrift.Winzig, color = Farben.TextSehrLeise)
    }
}

@Composable
private fun ColumnScope.TabletKraefte(
    raum: Raumzustand,
    kraefte: List<Rundenfahrzeug>,
    anfahrt: List<Rundenfahrzeug>,
    vorOrt: List<Rundenfahrzeug>,
    transport: List<Rundenfahrzeug>,
    sonstige: List<Rundenfahrzeug>,
    kennung: (Rundenfahrzeug) -> String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Etikett("Einsatzmittel")
            Text("Kräfte im Einsatz", style = Schrift.Gross, color = Farben.Text)
        }
        SehrLeise("${kraefte.size} gesamt", mono = true)
    }
    listOf(
        "Auf Anfahrt" to anfahrt,
        "An der Einsatzstelle" to vorOrt,
        "Im Transport" to transport,
        "Weitere Kräfte" to sonstige,
    ).filter { it.second.isNotEmpty() }.forEach { (titel, liste) ->
        Etikett("$titel ${liste.size}")
        liste.forEach { f -> Kraftzeile(raum, f, kennung) }
    }
}

@Composable
private fun Kraftzeile(raum: Raumzustand, f: Rundenfahrzeug, kennung: (Rundenfahrzeug) -> String, hinten: @Composable (() -> Unit)? = null) {
    val marke = besatzungsmarke(raum, f.playerId)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp, randfarbe = Rundentexte.organisationFarbe(f.organisation).copy(alpha = 0.5f)).padding(Abstand.Klein),
    ) {
        Text(f.status.toString(), style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = fmsFarbe(f.status))
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                Text(kennung(f), style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                marke?.let { (mensch, text) -> Marke(text, farbe = if (mensch) Farben.GruenHell else Farben.TextSehrLeise) }
            }
            Text(f.typ, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        hinten?.invoke()
    }
}

@Composable
private fun ColumnScope.TabletNachforderungen(
    raum: Raumzustand,
    einsatz: Einsatz,
    offene: List<Pair<String, String>>,
    zweigname: String,
    nachalarmOffen: Boolean,
    nachforderungText: String,
    beiText: (String) -> Unit,
    beiOeffnen: () -> Unit,
    beiAbbrechen: () -> Unit,
    beiSenden: () -> Unit,
    beiWeiterreichen: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Etikett("Kommunikation")
            Text("Leitstelle", style = Schrift.Gross, color = Farben.Text)
        }
        SehrLeise("${offene.size} offen", mono = true)
    }
    if (offene.isNotEmpty()) {
        Etikett("Bei der $zweigname eingegangen")
        offene.forEach { (von, text) ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(von, style = Schrift.MonoKlein, color = Farben.AmberHell)
                Text(text, style = Schrift.Klein, color = Farben.Text)
            }
        }
        Knopf(
            "${offene.size} ${if (offene.size == 1) "Nachforderung" else "Nachforderungen"} an Leitstelle weiterreichen",
            beiWeiterreichen,
            art = Knopfart.Alarm,
            breit = true,
        )
    } else {
        SehrLeise("Keine Nachforderungen bei der $zweigname offen.")
    }

    // Was bereits bei der Leitstelle angekommen ist — sonst wirkte eine selbst
    // abgesendete Nachforderung, als sei sie verschwunden.
    val gesendet = raum.funkprotokoll.filter {
        it.kind == "Nachforderung" && it.incidentId == einsatz.id && it.an == raum.settings.leitstelle
    }.takeLast(8).reversed()
    if (gesendet.isNotEmpty()) {
        Etikett("Bereits an die Leitstelle übermittelt")
        gesendet.forEach { m ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(m.von, style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                Text(m.text, style = Schrift.Klein, color = Farben.TextLeise)
            }
        }
    }

    if (!nachalarmOffen) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal)) {
            Text("Weitere Kräfte erforderlich?", style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Leise("Die Leitstelle prüft und alarmiert passende Einsatzmittel.")
            Knopf("Nachforderung erstellen", beiOeffnen, art = Knopfart.Alarm)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Etikett("Nachforderung an die Leitstelle", Modifier.weight(1f))
            Knopf("Abbrechen", beiAbbrechen, art = Knopfart.Leise, kompakt = true)
        }
        Feld(wert = nachforderungText, beiAenderung = { beiText(it.take(220)) }, einzeilig = false, weiterTaste = ImeAction.Default)
        Knopf("Nachforderung senden", beiSenden, art = Knopfart.Alarm, aktiv = nachforderungText.isNotBlank(), breit = true)
    }
}

@Composable
private fun ColumnScope.TabletFunk(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    einsatz: Einsatz,
    kraefte: List<Rundenfahrzeug>,
    vorOrt: List<Rundenfahrzeug>,
    funkgruppen: List<de.pagerspass.pagerspass.netz.Funkgruppe>,
    kennung: (Rundenfahrzeug) -> String,
    griffe: ManvGriffe,
) {
    var neueGruppe by remember { mutableStateOf("") }
    var wahlFuer by remember { mutableStateOf<String?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Etikett("Einsatzstellenfunk")
            Text("DMO-Rufgruppen", style = Schrift.Gross, color = Farben.Text)
        }
        SehrLeise("${funkgruppen.size}/3", mono = true)
    }
    if (funkgruppen.size < 3) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
            Feld(
                wert = neueGruppe,
                beiAenderung = { neueGruppe = it.take(40) },
                etikett = "Neue Gruppe",
                platzhalter = "Einsatzstelle ${funkgruppen.size + 1}",
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "DMO öffnen",
                {
                    griffe.einsatzfunkgruppeOeffnen(einsatz.id, neueGruppe.trim())
                    neueGruppe = ""
                },
                art = Knopfart.Alarm,
                kompakt = true,
            )
        }
    }
    if (funkgruppen.isEmpty()) {
        SehrLeise("Noch keine Einsatzfunkgruppe geöffnet. Der ELW kann bis zu drei Gruppen parallel mithören.")
    }
    funkgruppen.forEach { g ->
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(g.nummer, style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                Text(g.name, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                Knopf(
                    if (raum.sendegruppe == g.id) "Sendekanal" else "Hier senden",
                    { griffe.funkgruppeZuweisen(meins.id, g.id) },
                    aktiv = raum.sendegruppe != g.id,
                    kompakt = true,
                    art = Knopfart.Leise,
                )
                Knopf("Schließen", { griffe.einsatzfunkgruppeSchliessen(g.id) }, kompakt = true, art = Knopfart.Leise)
            }
            Text(
                "Aufgeschaltet: " + kraefte.filter { it.funkgruppe == g.id }.joinToString(", ") { kennung(it) }.ifBlank { "noch niemand" },
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
            Knopf("Kraft vor Ort aufschalten …", { wahlFuer = g.id }, art = Knopfart.Leise, kompakt = true)
        }
    }
    wahlFuer?.let { gruppe ->
        val waehlbar = vorOrt.filter { it.funkgruppe != gruppe }
        Wahlblende(
            titel = "Kraft vor Ort wählen",
            gruppen = listOf(null to waehlbar),
            aufschrift = { kennung(it) },
            beiWahl = { f ->
                griffe.funkgruppeZuweisen(f.id, gruppe)
                wahlFuer = null
            },
            beiSchliessen = { wahlFuer = null },
        )
    }
}

/** Die Aufträge, die eine Einsatzleitung Rettungsdienst weitergeben kann. */
private val AUFTRAEGE = listOf("Vorsichtung", "Sichtung", "Transportorganisation")

@Composable
private fun ColumnScope.TabletFuehrung(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    einsatz: Einsatz,
    zweig: String,
    zweigname: String,
    kraefte: List<Rundenfahrzeug>,
    vorOrt: List<Rundenfahrzeug>,
    kennung: (Rundenfahrzeug) -> String,
    griffe: ManvGriffe,
    abschnittVon: (String) -> String?,
) {
    val abschnitte = if (zweig == "Rettungsdienst") einsatz.abschnitteRd else einsatz.abschnitte
    var auftragswahl by remember { mutableStateOf<String?>(null) }
    var zuordnungFahrzeug by remember { mutableStateOf<Rundenfahrzeug?>(null) }
    var zuordnungAbschnitt by remember { mutableStateOf<String?>(null) }
    var fahrzeugwahl by remember { mutableStateOf(false) }
    var abschnittwahl by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Etikett(zweigname)
            Text("Abschnitte", style = Schrift.Gross, color = Farben.Text)
        }
        SehrLeise("${abschnitte.size}/4", mono = true)
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal)) {
        Column(modifier = Modifier.weight(1f)) {
            Etikett("Führungswechsel")
            Text("$zweigname übergeben", style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            SehrLeise("Die Leitstelle übernimmt die Führung wieder.")
        }
        Knopf("Übergeben", { griffe.abgeben(einsatz.id, zweig) }, kompakt = true)
    }

    // Die drei Aufträge des ELRD — ein Massenanfall hat drei Dinge, die gleichzeitig laufen müssen.
    if (einsatz.manv && zweig == "Rettungsdienst") {
        Etikett("Aufträge übertragen")
        AUFTRAEGE.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(a, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                Knopf(einsatz.manvauftraege[a] ?: "— selbst —", { auftragswahl = a }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }

    // Die Flächen der Einsatzstelle: Bereitstellungsraum und Landeplatz.
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal)) {
        Etikett("Bereitstellungsraum")
        if (!einsatz.bereitstellungsraum) {
            Knopf("Bereitstellungsraum einrichten", { griffe.bereitstellungsraumFestlegen(einsatz.id) }, kompakt = true)
        } else {
            SehrLeise("Eingerichtet · ${einsatz.inBereitstellung.size} in Bereitstellung")
            val bereitstellbar = vorOrt.filter { it.funkrufname != meins.funkrufname }
            if (bereitstellbar.isEmpty()) SehrLeise("Noch keine Kraft an der Einsatzstelle.")
            bereitstellbar.forEach { f ->
                val haelt = einsatz.inBereitstellung.contains(f.funkrufname)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(kennung(f), style = Schrift.MonoKlein, color = Farben.Text, modifier = Modifier.weight(1f))
                    Pille("Im BR", an = haelt, farbe = Farben.GruenHell, beiDruck = { griffe.bereitstellungSetzen(einsatz.id, f.id, true) })
                    Pille("An E-Stelle", an = !haelt, farbe = Farben.GruenHell, beiDruck = { griffe.bereitstellungSetzen(einsatz.id, f.id, false) })
                }
            }
        }

        Etikett("Hubschrauberlandeplatz")
        val platz = einsatz.landeplatz
        if (platz == null) {
            Knopf("Landeplatz festlegen", { griffe.landeplatzFestlegen(einsatz.id) }, kompakt = true)
        } else {
            SehrLeise("Fläche: " + if (platz.hergerichtet) "hergerichtet" else "im Aufbau · ${(platz.fortschritt * 100).toInt()} %")
            SehrLeise(
                "Licht: " + when {
                    platz.ausgeleuchtet -> "ausgeleuchtet — Nachtlandung möglich"
                    platz.lichtfortschritt > 0 -> "im Aufbau · ${(platz.lichtfortschritt * 100).toInt()} %"
                    else -> "nicht angeordnet"
                },
            )
            if (!platz.ausgeleuchtet && platz.lichtfortschritt == 0.0) {
                Knopf("Ausleuchten anordnen", { griffe.landeplatzAusleuchten(einsatz.id) }, kompakt = true)
            }
            val landend = kraefte.filter { it.faehigkeiten.contains("Lufttransport") && (it.status == 4 || it.status == 7) }
            if (landend.isEmpty()) SehrLeise("Noch kein Luftfahrzeug auf der Fläche.")
            landend.forEach { f ->
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(kennung(f), style = Schrift.MonoKlein, color = Farben.Text)
                    Text(f.status.toString(), style = Schrift.MonoKlein, color = fmsFarbe(f.status))
                }
            }
        }
    }

    // Zuordnung Fahrzeug → Abschnitt.
    val zuordenbar = zuordnungAbschnitt?.let { a -> kraefte.filter { abschnittVon(it.funkrufname) != a } } ?: kraefte
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Knopf(zuordnungFahrzeug?.let { "${kennung(it)} · S${it.status}" } ?: "Fahrzeug wählen …", { fahrzeugwahl = true }, Modifier.weight(1f), art = Knopfart.Leise, kompakt = true)
        Text("→", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
        Knopf(zuordnungAbschnitt ?: "Abschnitt wählen …", { abschnittwahl = true }, Modifier.weight(1f), art = Knopfart.Leise, kompakt = true, aktiv = abschnitte.isNotEmpty())
    }
    Knopf(
        "Zuordnen",
        {
            val f = zuordnungFahrzeug
            val a = zuordnungAbschnitt
            if (f != null && a != null) {
                griffe.abschnittZuteilen(einsatz.id, f.id, a, zweig)
                zuordnungFahrzeug = null
            }
        },
        art = Knopfart.Alarm,
        aktiv = zuordnungFahrzeug != null && zuordnungAbschnitt != null,
        breit = true,
    )

    // Vier feste Plätze — sie halten die Führungsansicht lesbar und entsprechen der Serverregel.
    (0 until 4).forEach { platz ->
        val abschnitt = abschnitte.getOrNull(platz)
        Abschnittsplatz(
            nummer = platz + 1,
            name = abschnitt?.name,
            fahrzeuge = abschnitt?.let { a -> kraefte.filter { abschnittVon(it.funkrufname) == a.name } }.orEmpty(),
            raum = raum,
            einsatz = einsatz,
            kennung = kennung,
            beiAnlegen = { griffe.abschnittBilden(einsatz.id, it, zweig) },
            beiUmbenennen = { bisher, neu -> griffe.abschnittUmbenennen(einsatz.id, bisher, neu, zweig) },
        )
    }

    val ohne = kraefte.filter { abschnittVon(it.funkrufname) == null }
    if (ohne.isNotEmpty()) SehrLeise("Nicht zugeteilt: " + ohne.joinToString(", ") { kennung(it) })

    auftragswahl?.let { auftrag ->
        val passende = kraefte.filter { v ->
            when (auftrag) {
                "Vorsichtung" -> v.faehigkeiten.contains("Vorsichtung")
                "Sichtung" -> v.faehigkeiten.contains("Sichtung")
                else -> istMedizinischeFuehrung(v)
            }
        }
        Wahlblende(
            titel = auftrag,
            gruppen = listOf(null to (listOf<Rundenfahrzeug?>(null) + passende)),
            aufschrift = { it?.let(kennung) ?: "— selbst —" },
            beiWahl = { f ->
                griffe.auftrag(einsatz.id, auftrag, f?.id)
                auftragswahl = null
            },
            beiSchliessen = { auftragswahl = null },
        )
    }
    if (fahrzeugwahl) {
        Wahlblende(
            titel = "Fahrzeug wählen",
            gruppen = listOf(null to zuordenbar),
            aufschrift = { "${kennung(it)} · Status ${it.status}" },
            beiWahl = {
                zuordnungFahrzeug = it
                fahrzeugwahl = false
            },
            beiSchliessen = { fahrzeugwahl = false },
        )
    }
    if (abschnittwahl) {
        Wahlblende(
            titel = "Abschnitt wählen",
            gruppen = listOf(null to abschnitte.map { it.name }),
            aufschrift = { it },
            beiWahl = { a ->
                zuordnungAbschnitt = a
                // Steht das gewählte Fahrzeug dort schon, gilt die Wahl nicht mehr.
                if (zuordnungFahrzeug?.let { abschnittVon(it.funkrufname) == a } == true) zuordnungFahrzeug = null
                abschnittwahl = false
            },
            beiSchliessen = { abschnittwahl = false },
        )
    }
}

/** Ein Abschnittsplatz: leer mit Namensfeld, belegt mit umbenennbarem Namen und Tabelle. */
@Composable
private fun Abschnittsplatz(
    nummer: Int,
    name: String?,
    fahrzeuge: List<Rundenfahrzeug>,
    raum: Raumzustand,
    einsatz: Einsatz,
    kennung: (Rundenfahrzeug) -> String,
    beiAnlegen: (String) -> Unit,
    beiUmbenennen: (String, String) -> Unit,
) {
    var eingabe by remember(name) { mutableStateOf(name.orEmpty()) }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text(nummer.toString(), style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell, modifier = Modifier.width(18.dp))
            Feld(
                wert = eingabe,
                beiAenderung = { eingabe = it.take(40) },
                platzhalter = "Abschnitt $nummer benennen",
                weiterTaste = ImeAction.Done,
                modifier = Modifier.weight(1f),
            )
            if (name == null) {
                Knopf(
                    "Anlegen",
                    {
                        val n = eingabe.trim()
                        if (n.isNotEmpty()) beiAnlegen(n)
                    },
                    aktiv = eingabe.isNotBlank(),
                    kompakt = true,
                )
            } else {
                // Umbenennen — nur, wenn wirklich ein anderer Name dasteht.
                Knopf(
                    "Umbenennen",
                    {
                        val neu = eingabe.trim()
                        if (neu.isNotEmpty() && neu != name) beiUmbenennen(name, neu) else eingabe = name
                    },
                    aktiv = eingabe.isNotBlank() && eingabe.trim() != name,
                    kompakt = true,
                    art = Knopfart.Leise,
                )
                SehrLeise(fahrzeuge.size.toString(), mono = true)
            }
        }
        if (name == null) {
            SehrLeise("Name eingeben, um diesen Abschnitt anzulegen.")
        } else {
            if (fahrzeuge.isEmpty()) SehrLeise("Noch kein Fahrzeug zugeteilt.")
            fahrzeuge.forEach { f ->
                val marke = besatzungsmarke(raum, f.playerId)
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Text(kennung(f), style = Schrift.MonoKlein, color = Farben.Text, modifier = Modifier.weight(1f))
                    marke?.let { (mensch, text) -> Marke(text, farbe = if (mensch) Farben.GruenHell else Farben.TextSehrLeise) }
                    Text(f.status.toString(), style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = fmsFarbe(f.status))
                    Text(uhrzeit(f.statusSeit).ifBlank { "—" }, style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                    Text(
                        f.aufgabe?.let { n -> einsatz.aufgaben.firstOrNull { it.nummer == n }?.name ?: "Auftrag läuft" } ?: "—",
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------- Patienten

/**
 * Die Patienten dieser Lage, an denen gearbeitet werden kann — `PatientenFenster.vue`.
 * Nur mit laufender Simulation und nur, wenn das eigene Fahrzeug an der Stelle steht
 * (Status 4) oder ihn an Bord hat (Status 7). Der eigene zuerst.
 */
@Composable
fun ColumnScope.Patientenfenster(
    einsatz: Einsatz?,
    meins: Rundenfahrzeug,
    jetzt: Long,
    griffe: Patientengriffe,
) {
    if (einsatz == null || (meins.status != 4 && meins.status != 7)) return
    val liste = einsatz.manvPatienten.filter { it.simuliert }
        .map { it to (it.transportVehicleId == meins.id) }
        .sortedWith(compareByDescending<Pair<ManvPatient, Boolean>> { it.second }.thenBy { it.first.id })
    if (liste.isEmpty()) return
    Etikett("Patienten" + if (liste.size > 1) " (${liste.size})" else "")
    liste.forEach { (p, mein) ->
        Patientenbogen(einsatz.id, p, mein, meins, jetzt, griffe)
    }
}

/** Die Griffe am Patienten. */
class Patientengriffe(
    val messen: (String, String, String) -> Unit = { _, _, _ -> },
    val schema: (String, String, String) -> Unit = { _, _, _ -> },
    val massnahme: (String, String, String) -> Unit = { _, _, _ -> },
    val verdacht: (String, String, String) -> Unit = { _, _, _ -> },
    val diagnose: (String, String, String?) -> Unit = { _, _, _ -> },
)

/** Was man messen kann — in der Reihenfolge, in der man misst. */
private val MESSBAR = listOf(
    Triple("Bewusstsein", "Bewusstsein ansprechen", "Bewusstsein"),
    Triple("Atemfrequenz", "Atemfrequenz", "Atemfrequenz"),
    Triple("Puls", "Puls", "Puls"),
    Triple("Sauerstoffsaettigung", "Sättigung", "Sättigung"),
    Triple("Blutdruck", "Blutdruck", "Blutdruck"),
    Triple("Blutzucker", "Blutzucker", "Blutzucker"),
    Triple("Temperatur", "Temperatur", "Temperatur"),
)

private val SCHEMATA = listOf("XAbcde" to "xABCDE", "Sampler" to "SAMPLER", "Opqrst" to "OPQRST")

private fun wertname(id: String): String = MESSBAR.firstOrNull { it.first == id }?.third ?: id

/**
 * Der Bogen eines Patienten — messen, untersuchen, behandeln, einordnen. <b>Erst, was
 * nicht stimmt</b>: Auffällige Werte stehen oben, innerhalb beider Gruppen bleibt die
 * Ordnung der Erstuntersuchung.
 */
@Composable
private fun Patientenbogen(
    einsatzId: String,
    p: ManvPatient,
    zugewiesen: Boolean,
    meins: Rundenfahrzeug,
    jetzt: Long,
    griffe: Patientengriffe,
) {
    var verdacht by remember(p.id) { mutableStateOf("") }
    var diagnosetext by remember(p.id) { mutableStateOf("") }
    val werte = p.werte.orEmpty()
    val auffaellig = p.auffaelligeWerte.orEmpty().toSet()
    val bekannt = MESSBAR.filter { it.first in werte }.map { Triple(it.first, it.third, werte[it.first].orEmpty()) }
    val unbekannt = werte.keys.filter { k -> MESSBAR.none { it.first == k } }.map { Triple(it, it, werte[it].orEmpty()) }
    val alle = bekannt + unbekannt
    val geordnet = alle.filter { it.first in auffaellig } + alle.filter { it.first !in auffaellig }
    val messungRest = zeitMillis(p.messungFertigUm)?.let { ((it - jetzt) / 1000.0).toLong().coerceAtLeast(0) }
    val genug = werte.size >= 3 && (p.befunde?.size ?: 0) > 0
    val hatNotarzt = meins.faehigkeiten.any { it.lowercase() == "notarzt" }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = if (zugewiesen) Farben.Amber else Farben.Rand)
            .padding(Abstand.Normal),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Etikett("Patient ${p.id}")
            p.kategorie?.let { Marke(kategorieWort(it), farbe = kategorieTon(it)) }
            if (zugewiesen) Marke("dir zugewiesen", farbe = Farben.AmberHell)
        }
        if (geordnet.isEmpty()) SehrLeise("Noch nichts gemessen.")
        geordnet.forEach { (id, name, text) ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(name, style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                Text(
                    text,
                    style = Schrift.MonoKlein.copy(fontWeight = if (id in auffaellig) FontWeight.Bold else FontWeight.Normal),
                    color = if (id in auffaellig) Farben.SignalHell else Farben.Text,
                )
            }
        }
        if (messungRest != null) {
            Leise(
                if (messungRest > 0) "${wertname(p.misstGerade.orEmpty())} läuft — noch $messungRest s"
                else "${wertname(p.misstGerade.orEmpty())} — wird abgelesen …",
            )
        }
        Pillenreihe {
            MESSBAR.forEach { (id, name, _) ->
                Pille(name, an = false, aktiv = messungRest == null && id !in werte, beiDruck = { griffe.messen(einsatzId, p.id, id) })
            }
        }
        Pillenreihe {
            SCHEMATA.forEach { (id, name) ->
                val erhoben = p.befunde.orEmpty().any { it.schema == id }
                Pille(name, an = erhoben, aktiv = !erhoben, beiDruck = { griffe.schema(einsatzId, p.id, id) })
            }
        }
        p.befunde.orEmpty().forEach { b ->
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                Leise(b.name)
                b.punkte.forEach { pkt ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                        Text(pkt.schluessel, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
                        Text("${pkt.frage}:", style = Schrift.Klein, color = Farben.TextSehrLeise)
                        Text(pkt.befund.ifBlank { "unauffällig" }, style = Schrift.Klein, color = Farben.Text)
                    }
                }
            }
        }
        val massnahmen = p.massnahmen.orEmpty()
        if (massnahmen.isNotEmpty()) {
            Pillenreihe {
                massnahmen.forEach { m ->
                    Pille(m.name, an = m.laeuft, aktiv = m.moeglich, farbe = Farben.GruenHell, beiDruck = { griffe.massnahme(einsatzId, p.id, m.id) })
                }
            }
            if (massnahmen.any { it.brauchtArzt && !it.moeglich }) SehrLeise("Ärztliche Maßnahmen brauchen den Notarzt vor Ort.")
        }
        val vd = p.verdachtsdiagnose
        if (vd != null) {
            Text("Verdacht: $vd", style = Schrift.Klein, color = Farben.Text)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = verdacht,
                    beiAenderung = { verdacht = it.take(120) },
                    platzhalter = "Verdachtsdiagnose …",
                    aktiv = genug,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Festhalten",
                    {
                        griffe.verdacht(einsatzId, p.id, verdacht.trim())
                        verdacht = ""
                    },
                    aktiv = genug && verdacht.isNotBlank(),
                    kompakt = true,
                )
            }
            if (!genug) SehrLeise("Erst untersuchen: drei Messwerte und ein Schema.")
        }
        val dg = p.diagnose
        if (dg != null) {
            Text("Diagnose: $dg", style = Schrift.Klein, color = Farben.Text)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = diagnosetext,
                    beiAenderung = { diagnosetext = it.take(120) },
                    platzhalter = "Diagnose (leer: Klartext des Bildes) …",
                    aktiv = hatNotarzt,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Diagnose stellen",
                    {
                        griffe.diagnose(einsatzId, p.id, diagnosetext.trim().ifBlank { null })
                        diagnosetext = ""
                    },
                    aktiv = hatNotarzt,
                    kompakt = true,
                )
            }
            if (!hatNotarzt) SehrLeise("Die Diagnose stellt der Notarzt — dieses Fahrzeug hat keinen an Bord.")
        }
    }
}
