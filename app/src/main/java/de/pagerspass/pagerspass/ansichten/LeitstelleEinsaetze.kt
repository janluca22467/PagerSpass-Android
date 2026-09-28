package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Leitstellenstand
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Klinik
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.netz.Spielevent
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.bildVon
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlin.math.roundToInt

// ============================================================ Einsatzliste

/**
 * Die Einsatzliste — `EinsatzListe.vue`.
 *
 * Offene Lagen zuerst, darin die dringendsten oben. Die Wartezeit wird gesetzt,
 * je länger eine Lage unbearbeitet liegt: Zwanzig kleine Lagen einer
 * Unwetterwelle sehen alle gleich aus, und die eine, die seit acht Minuten auf
 * ihr erstes Fahrzeug wartet, soll man sehen. Es rutscht nichts nach oben —
 * priorisieren macht der Mensch.
 */
@Composable
internal fun Einsatzliste(
    raum: Raumzustand,
    ausgewaehlt: String?,
    beiWahl: (String) -> Unit,
    journalSichtbar: Boolean,
    beiJournal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val jetzt = rememberJetzt()
    val einsaetze = raum.incidents.sortedWith(
        compareBy<Einsatz> { if (it.abgeschlossen) 1 else 0 }
            .thenByDescending { it.prioritaet }
            .thenByDescending { it.eingangUm },
    )

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Abstand.Gross, vertical = Abstand.Winzig),
        ) {
            Ueberschrift("Einsätze", Modifier.weight(1f))
            // Das Journal steht bei den Einsätzen: neben dem, worüber es Auskunft gibt.
            if (journalSichtbar) Knopf("Journal", beiJournal, art = Knopfart.Leise, kompakt = true)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Abstand.Gross, vertical = Abstand.Winzig),
        ) {
            if (einsaetze.isEmpty()) {
                SehrLeise("Keine Einsätze. Ruhiger Dienst.", mono = true)
            }
            einsaetze.forEach { e ->
                Einsatzeintrag(e, gewaehlt = e.id == ausgewaehlt, jetzt = jetzt, beiDruck = { beiWahl(e.id) })
            }
        }
    }
}

/** Ab dieser Restzeit verhält sich eine Terminfahrt wie eine offene Lage. */
private const val VORWARNUNG_MS = 3 * 60_000L

/** Ob die Lage noch nicht disponiert ist — eine Terminfahrt erst kurz vor ihrem Termin. */
private fun istUndisponiert(e: Einsatz, jetzt: Long): Boolean {
    if (e.state != "Offen") return false
    val termin = zeitMillis(e.terminUm) ?: return true
    return termin - jetzt <= VORWARNUNG_MS
}

/** `frisch`, `wartet`, `liegt` — die Stufe, in der die Wartezeit gesetzt wird. */
private fun alter(e: Einsatz, jetzt: Long): Int {
    val termin = zeitMillis(e.terminUm)
    if (termin != null && !e.abgeschlossen) {
        val bis = (termin - jetzt) / 60_000.0
        return when {
            bis < 0 -> 2
            bis <= VORWARNUNG_MS / 60_000.0 -> 1
            else -> 0
        }
    }
    if (!istUndisponiert(e, jetzt)) return 0
    val minuten = (jetzt - (zeitMillis(e.eingangUm) ?: jetzt)) / 60_000.0
    return when {
        minuten >= 8 -> 2
        minuten >= 3 -> 1
        else -> 0
    }
}

@Composable
private fun Einsatzeintrag(e: Einsatz, gewaehlt: Boolean, jetzt: Long, beiDruck: () -> Unit) {
    val org = Rundentexte.organisationFarbe(e.organisation)
    val neu = istUndisponiert(e, jetzt)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (gewaehlt) Farben.FlaecheAktiv else Farben.Flaeche,
                randfarbe = when {
                    gewaehlt -> Farben.Amber
                    neu -> Farben.SignalHell
                    else -> org.copy(alpha = 0.55f)
                },
                ecke = 9.dp,
            )
            .clickable(onClick = beiDruck)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(3.dp).height(16.dp).background(org))
            // Das Formzeichen neben der Farbe: Wer Rot und Amber nicht auseinander-
            // hält, liest die Dringlichkeit an der Form.
            Text(
                text = listOf(prioZeichen(e.prioritaet), e.stichwort).filter { it.isNotEmpty() }.joinToString(" "),
                style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                color = prioFarbe(e.prioritaet),
            )
            if (e.internerAuftrag) {
                Marke("Intern", farbe = Farben.ViolettHell)
            } else if (e.terminUm != null) {
                Marke("Termin", farbe = Farben.BlauHell)
            }
            Box(Modifier.weight(1f))
            SehrLeise(e.einsatznummer, mono = true)
        }
        Text(
            e.stichwortText,
            style = Schrift.Klein,
            color = if (e.abgeschlossen) Farben.TextSehrLeise else Farben.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            e.adresse,
            style = Schrift.MonoKlein,
            color = Farben.TextLeise,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        ) {
            Marke(einsatzZustand(e.state), farbe = einsatzFarbe(e.state))
            if (!e.abgeschlossen) {
                val zeit = if (e.terminUm != null) {
                    val rest = ((zeitMillis(e.terminUm) ?: jetzt) - jetzt) / 1000
                    if (rest < 0) "überfällig" else minSek(rest)
                } else {
                    wartezeit(e.eingangUm, jetzt)
                }
                Text(
                    zeit,
                    style = Schrift.MonoKlein.copy(
                        fontWeight = if (alter(e, jetzt) > 0) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = when (alter(e, jetzt)) {
                        2 -> Farben.SignalHell
                        1 -> Farben.AmberHell
                        else -> Farben.TextSehrLeise
                    },
                )
            }
            if (e.alarmierteFahrzeuge.isNotEmpty()) SehrLeise("${e.alarmierteFahrzeuge.size} Fzg", mono = true)
            when {
                e.suchradiusMeter != null ->
                    SehrLeise("${(e.suchfortschritt * 100).roundToInt()} % abgesucht", mono = true)
                e.brandflaecheHektar > 0 ->
                    Text("${hektarText(e.brandflaecheHektar)} ha", style = Schrift.MonoKlein, color = Farben.OrangeHell)
                e.aufgaben.isNotEmpty() -> SehrLeise(
                    if (e.arbeitFertigUm != null) "abgearbeitet"
                    else "${(e.arbeitsfortschritt * 100).roundToInt()} % erledigt",
                    mono = true,
                )
            }
        }
    }
}

// =========================================================== Einsatzbogen

/**
 * Der Einsatzbogen — `EinsatzDetail.vue`.
 *
 * Oben der Kopf (Stichwort, Nummer, Zustand), darunter was die Lage ausmacht —
 * Termin, Suchgebiet, Brandfläche, Arbeiten, Sichtung, Versorgungsstellen,
 * Löschwasser, Gefahrgut, Einsatzleitung, Flächen, Sonderobjekt. Dann die
 * Handgriffe, die Kräfte, die Transportziele, die letzte Lagemeldung und die
 * Zeiten.
 *
 * <b>Abschließen schließt die laufende Lage</b> (`CloseIncident`) — gesperrt,
 * solange nichts alarmiert ist oder ein Massenanfall noch Patienten hat, und
 * der Grund steht unter dem Knopf, nicht in einem vier Sekunden langen Streifen.
 */
@Composable
internal fun ColumnScope.Einsatzdetail(
    einsatz: Einsatz,
    raum: Raumzustand,
    daten: Leitstellenstand,
    griffe: LeitstellenGriffe,
    beiAlarmieren: () -> Unit,
    beiUmstufen: () -> Unit,
    beiFolgeeinsatz: () -> Unit,
    beiGespraech: (anrufId: String) -> Unit,
) {
    val jetzt = rememberJetzt()
    val kennung = rememberKennung(raum)
    val org = Rundentexte.organisationFarbe(einsatz.organisation)

    LaunchedEffect(einsatz.eventId) { if (einsatz.eventId != null) griffe.eventsLaden() }

    val fahrzeuge = einsatz.alarmierteFahrzeuge.mapNotNull { id -> raum.vehicles.firstOrNull { it.id == id } }
    val imEinsatz = fahrzeuge.filter { it.einsatzId == einsatz.id }
    val ausgefallen = fahrzeuge.filter { it.einsatzId != einsatz.id && it.status == 6 }
    val abgedeckt = imEinsatz.flatMap { it.faehigkeiten }.toSet()
    val offeneFunktionen = einsatz.empfohleneFaehigkeiten.filter { it !in abgedeckt }
    val offeneZugplaetze = einsatz.zugplaetze.filter { it.pflicht && it.fahrzeugId == null }
    val unterbesetzt = !einsatz.abgeschlossen && ausgefallen.isNotEmpty() &&
        imEinsatz.size < einsatz.empfohleneFahrzeuge
    val gespraech = raum.anrufjournal.firstOrNull { it.incidentId == einsatz.id }
    val ursprung = einsatz.ursprungEinsatzId?.let { u -> raum.incidents.firstOrNull { it.id == u } }
    val event = einsatz.eventId?.let { id -> daten.events.firstOrNull { it.id == id } }

    val abschlussSperre: String? = when {
        !einsatz.manv -> null
        einsatz.betroffeneUngefaehr != null && einsatz.betroffeneUngefaehr > 0 ->
            "Ungefähr ${einsatz.betroffeneUngefaehr} Betroffene, die Vorsichtung läuft noch."
        else -> {
            val offen = einsatz.manvPatienten.count { it.status != "Uebergeben" && it.kategorie != "Schwarz" }
            when (offen) {
                0 -> null
                1 -> "Ein Patient wartet noch auf Transport oder Übergabe."
                else -> "$offen Patienten warten noch auf Transport oder Übergabe."
            }
        }
    }

    // ------------------------------------------------ Event-Kulisse
    if (event != null) {
        Eventkulisse(event, einsatz, raum)
    }

    // --------------------------------------------------------- Kopf
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.width(4.dp).height(40.dp).background(org, Rundung.Winzig))
        Text(
            einsatz.stichwort,
            style = Schrift.Titel.copy(fontFamily = Schrift.Mono),
            color = prioFarbe(einsatz.prioritaet),
        )
        Column(Modifier.weight(1f)) {
            Text(einsatz.stichwortText, style = Schrift.Gross, color = Farben.Text)
            SehrLeise(
                listOfNotNull(
                    einsatz.einsatznummer,
                    Rundentexte.organisation(einsatz.organisation),
                    "INTERN".takeIf { einsatz.internerAuftrag },
                    "Eingang ${uhrzeit(einsatz.eingangUm)}",
                ).joinToString(" · "),
                mono = true,
            )
        }
        Marke(einsatzZustand(einsatz.state), farbe = einsatzFarbe(einsatz.state))
    }

    Text(
        einsatz.adresse + (einsatz.ortsteil?.let { " · $it" } ?: ""),
        style = Schrift.MonoNormal,
        color = Farben.Text,
    )
    if (einsatz.meldebild.isNotBlank()) Text(einsatz.meldebild, style = Schrift.Normal, color = Farben.TextLeise)
    einsatz.meldender?.takeIf { it.isNotBlank() }?.let { SehrLeise(it, mono = true) }

    if (gespraech != null) {
        Text(
            "☎ Gespräch zu Notruf ${gespraech.nummer} nachlesen",
            style = Schrift.MonoKlein.copy(textDecoration = TextDecoration.Underline),
            color = Farben.BlauHell,
            modifier = Modifier.clickable { beiGespraech(gespraech.anrufId) }.padding(vertical = Abstand.Winzig),
        )
    }

    // ------------------------------------------------------- Termin
    zeitMillis(einsatz.terminUm)?.let { termin ->
        val restMs = termin - jetzt
        val ueberfaellig = restMs < 0
        Abschnittskasten(randfarbe = if (ueberfaellig) Farben.SignalHell else Farben.BlauHell) {
            Text(
                "Termin ${uhrzeit(einsatz.terminUm.orEmpty())}" +
                    if (ueberfaellig) " · überfällig" else " · in ${(restMs / 60_000.0).roundToInt()} min",
                style = Schrift.MonoKlein,
                color = if (ueberfaellig) Farben.SignalHell else Farben.Text,
            )
            Text(
                "Abholung ${einsatz.adresse}" + (einsatz.zielklinikVorgabe?.let { " → $it" } ?: ""),
                style = Schrift.MonoKlein,
                color = Farben.TextLeise,
            )
            SehrLeise("Das Haus erwartet den Patienten — ein Transportziel muss nicht zugewiesen werden.")
        }
    }

    // ------------------------------------------------------ Suchgebiet
    einsatz.suchradiusMeter?.let { radius ->
        val suchtrupps = fahrzeuge.filter { it.einsatzId == einsatz.id && it.status == 4 }
        Abschnittskasten(randfarbe = Farben.SuchFarbe) {
            Text(
                "Suchgebiet ${(radius / 50).roundToInt() * 50} m · ${(einsatz.suchfortschritt * 100).roundToInt()} % abgesucht",
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
            if (einsatz.personGefundenUm != null) {
                Text(
                    "Person gefunden — die Einsatzstelle liegt jetzt an der Fundstelle",
                    style = Schrift.MonoKlein,
                    color = Farben.GruenHell,
                )
            } else {
                einsatz.suchabschnitte.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.name, style = Schrift.MonoKlein, color = Farben.TextLeise, modifier = Modifier.width(88.dp))
                        Fortschritt(a.fortschritt.toFloat(), Modifier.weight(1f))
                    }
                }
            }
            suchtrupps.forEach { f ->
                Auswahlzeile(
                    beschriftung = kennung(f),
                    wert = f.suchabschnitt?.let { n ->
                        einsatz.suchabschnitte.firstOrNull { it.nummer == n }?.name
                    },
                    platzhalter = "Abschnitt …",
                    titel = "Suchabschnitt für ${f.funkrufname}",
                    optionen = einsatz.suchabschnitte.map { a ->
                        a.nummer to "${a.name} · ${(a.fortschritt * 100).roundToInt()} %"
                    },
                    beiWahl = { griffe.suchabschnitt(f.id, it) },
                )
            }
        }
    }

    // ------------------------------------------------------ Brandfläche
    if (einsatz.brandflaecheHektar > 0) {
        val waechst = einsatz.brandflaecheHektar >= einsatz.brandflaecheHoechstHektar
        Abschnittskasten(randfarbe = Farben.OrangeHell) {
            Text(
                "Brandfläche ${hektarText(einsatz.brandflaecheHektar)} ha" +
                    if (einsatz.brandflaecheHoechstHektar > einsatz.brandflaecheHektar) {
                        " · Höchststand ${hektarText(einsatz.brandflaecheHoechstHektar)} ha"
                    } else {
                        ""
                    },
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
            Text(
                if (waechst) "Wächst weiter — es steht zu wenig dagegen." else "Wird zurückgedrängt.",
                style = Schrift.Klein,
                color = if (waechst) Farben.SignalHell else Farben.GruenHell,
            )
        }
    }

    // --------------------------------------------------------- Arbeiten
    if (einsatz.aufgaben.isNotEmpty()) {
        val zaehlende = einsatz.aufgaben.filter { !it.entfallen }
        val erledigt = zaehlende.count { it.fertig }
        val trupps = fahrzeuge.filter { it.einsatzId == einsatz.id && it.status == 4 }
        Abschnittskasten(randfarbe = Farben.Rand) {
            Text(
                "Arbeiten $erledigt von ${zaehlende.size} · ${(einsatz.arbeitsfortschritt * 100).roundToInt()} %",
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
            if (einsatz.arbeitFertigUm != null) {
                Text(
                    "Einsatzstelle abgearbeitet — die Kräfte können einrücken",
                    style = Schrift.MonoKlein,
                    color = Farben.GruenHell,
                )
            }
            einsatz.aufgaben.forEach { a ->
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            a.name,
                            style = Schrift.Klein,
                            color = when {
                                a.entfallen -> Farben.TextSehrLeise
                                a.fertig -> Farben.GruenHell
                                !a.begonnen -> Farben.TextSehrLeise
                                else -> Farben.Text
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            when {
                                a.entfallen -> "entfallen"
                                a.fertig -> "fertig"
                                else -> "${(a.fortschritt * 100).roundToInt()} %"
                            },
                            style = Schrift.MonoKlein,
                            color = Farben.TextLeise,
                        )
                    }
                    Fortschritt(a.fortschritt.toFloat())
                    val grund: Pair<String, Boolean>? = when {
                        a.entfallen -> "zurückgenommen" to false
                        a.wartetAuf != null -> "wartet auf: ${a.wartetAuf}" to false
                        a.kraefteFehlen -> "zu wenige vor Ort: ${a.mindestkraefte} nötig" to true
                        a.mittelFehlt -> "fehlt vor Ort: ${a.faehigkeit ?: a.name}" to true
                        !a.fertig && a.zwingend && a.faehigkeit != null -> "benötigt: ${a.faehigkeit}" to false
                        else -> null
                    }
                    grund?.let { (text, fehlt) ->
                        Text(text, style = Schrift.Winzig, color = if (fehlt) Farben.SignalHell else Farben.TextSehrLeise)
                    }
                }
            }
            trupps.forEach { f ->
                // Zwingende ohne die Fähigkeit stehen gar nicht erst zur Wahl.
                val waehlbar = einsatz.aufgaben.filter { a ->
                    !a.fertig && a.begonnen &&
                        (!a.zwingend || a.faehigkeit == null || a.faehigkeit in f.faehigkeiten)
                }
                Auswahlzeile(
                    beschriftung = kennung(f),
                    wert = f.aufgabe?.let { n -> einsatz.aufgaben.firstOrNull { it.nummer == n }?.name },
                    platzhalter = "Aufgabe …",
                    titel = "Aufgabe für ${f.funkrufname}",
                    optionen = waehlbar.map { a -> a.nummer to "${a.name} · ${(a.fortschritt * 100).roundToInt()} %" },
                    beiWahl = { griffe.aufgabe(f.id, it) },
                )
            }
        }
    }

    // ------------------------------------------------------------ MANV
    val sichtung = einsatz.sichtung
    when {
        sichtung != null -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        ) {
            einsatz.manvStufe?.let { Marke("MANV $it", farbe = Farben.SignalHell) }
            Marke("${sichtung.rot} Rot", farbe = Color(0xFFE5484D))
            Marke("${sichtung.gelb} Gelb", farbe = Color(0xFFF0C040))
            Marke("${sichtung.gruen} Grün", farbe = Farben.GruenHell)
            if (sichtung.schwarz > 0) Marke("${sichtung.schwarz} Schwarz", farbe = Farben.TextLeise)
        }
        einsatz.betroffeneUngefaehr != null && einsatz.betroffeneUngefaehr > 0 -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        ) {
            einsatz.manvStufe?.let { Marke("MANV $it", farbe = Farben.SignalHell) }
            Text(
                "ca. ${einsatz.betroffeneUngefaehr} Betroffene – Vorsichtung läuft",
                style = Schrift.MonoKlein,
                color = Farben.SignalHell,
            )
        }
        einsatz.manv -> Text(
            "MANV${einsatz.manvStufe?.let { " $it" } ?: ""} – Sichtung steht aus",
            style = Schrift.MonoKlein,
            color = Farben.SignalHell,
        )
    }
    if (einsatz.manvPatienten.isNotEmpty()) {
        val patienten = einsatz.manvPatienten
        val lebend = patienten.filter { it.kategorie != "Schwarz" }
        val teile = listOf(
            lebend.count { it.status == "WartetAufSichtung" || it.status == "WartetAufTransport" } to "zu versorgen",
            lebend.count { it.status == "ImTransport" } to "im Transport",
            lebend.count { it.status == "Uebergeben" } to "übergeben",
            (patienten.size - lebend.size) to "verstorben",
        ).filter { it.first > 0 }
        Text(
            (listOf("${patienten.size} ${if (patienten.size == 1) "Patient" else "Patienten"}") +
                teile.map { "${it.first} ${it.second}" }).joinToString(" · "),
            style = Schrift.MonoKlein,
            color = Farben.TextLeise,
        )
    }

    // Die Versorgungsstellen — für die Leitstelle reine Auskunft.
    einsatz.versorgungsstellen.forEach { s ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text(s.name, style = Schrift.MonoKlein, color = Farben.Text)
            when {
                s.imAbbau -> Text(
                    "im Abbau ${(s.aufbaufortschritt * 100).roundToInt()} %",
                    style = Schrift.MonoKlein,
                    color = Farben.OrangeHell,
                )
                s.einsatzbereit -> Text("steht", style = Schrift.MonoKlein, color = Farben.GruenHell)
                else -> SehrLeise("im Aufbau ${(s.aufbaufortschritt * 100).roundToInt()} %", mono = true)
            }
            SehrLeise(
                "· ${s.patienten.size}" + if (s.kapazitaet > 0) " / ${s.kapazitaet}" else "",
                mono = true,
            )
        }
    }

    // Löschwasser — erst, wenn die Einsatzstelle es gemeldet hat.
    einsatz.wasserlage?.let {
        Text(
            "Löschwasser: ${einsatz.wasserlageText.orEmpty()}" +
                if (einsatz.wasserversorgungSteht) " · Versorgung steht" else "",
            style = Schrift.MonoKlein,
            color = if (einsatz.wasserversorgungSteht) Farben.GruenHell else Farben.BlauHell,
        )
    }

    // Gefahrgut — der Absperrbereich wächst, solange kein Messtrupp vor Ort ist.
    if (einsatz.gefahrgutlage) {
        Text(
            buildString {
                append("Absperrbereich ${((einsatz.absperrradiusMeter ?: 0.0) / 10).roundToInt() * 10} m")
                einsatz.gefahrstoff?.let { append(" · $it") }
                append(" · Wind aus ${raum.settings.windText}")
                append(if (einsatz.messtruppVorOrt) " · vermessen" else " · wächst, Messtrupp fehlt")
            },
            style = Schrift.MonoKlein,
            color = if (einsatz.messtruppVorOrt) Farben.AmberHell else Farben.OrangeHell,
        )
    }
    if (einsatz.schaulustige) {
        Text(
            "Zufahrt durch Schaulustige erschwert — eine Streife löst die Menge am schnellsten",
            style = Schrift.MonoKlein,
            color = Farben.OrangeHell,
        )
    }
    if (einsatz.drohne) {
        Text(
            "Drohne über der Einsatzstelle — Luftraum nicht frei, kein RTH-Start auf diese Lage",
            style = Schrift.MonoKlein,
            color = Farben.OrangeHell,
        )
    }

    // Einsatzleitung vor Ort — geführt wird dort, nicht hier.
    einsatz.einsatzleitung?.let { el ->
        Abschnittskasten(randfarbe = Farben.Rand) {
            Text("Einsatzleitung: $el", style = Schrift.MonoKlein, color = Farben.Text)
            einsatz.abschnitte.forEach { a ->
                Text(
                    a.name + if (a.funkrufnamen.isNotEmpty()) " · ${a.funkrufnamen.joinToString(", ")}" else "",
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                )
            }
        }
    }

    // Die Flächen der Einsatzstelle — sie bleiben, auch wenn die Führung wechselt.
    if (einsatz.bereitstellungsraum || einsatz.landeplatz != null) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            if (einsatz.bereitstellungsraum) {
                Text(
                    "Bereitstellungsraum eingerichtet" +
                        if (einsatz.inBereitstellung.isNotEmpty()) " · hält: ${einsatz.inBereitstellung.joinToString(", ")}" else "",
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                )
            }
            einsatz.landeplatz?.let { l ->
                Text(
                    "Landeplatz " +
                        (if (l.hergerichtet) "hergerichtet" else "im Aufbau ${(l.fortschritt * 100).roundToInt()} %") +
                        (if (l.ausgeleuchtet) " · ausgeleuchtet, Nachtlandung möglich" else ""),
                    style = Schrift.MonoKlein,
                    color = if (l.hergerichtet) Farben.GruenHell else Farben.TextLeise,
                )
            }
        }
    }

    // Sonderobjekt und Einsatzplan — offen, nicht in einem Tooltip.
    einsatz.objektName?.let { name ->
        Abschnittskasten(randfarbe = Farben.ViolettHell) {
            Text(
                name + (einsatz.objektartText?.let { " · $it" } ?: ""),
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
            if (einsatz.objektBetroffene > 0) {
                Text("etwa ${einsatz.objektBetroffene} Personen im Objekt", style = Schrift.MonoKlein, color = Farben.AmberHell)
            }
            einsatz.objektHinweise.forEach { h -> Text("• $h", style = Schrift.Klein, color = Farben.TextLeise) }
        }
    }

    ursprung?.let { u ->
        SehrLeise(
            "Folgeeinsatz von E-${u.einsatznummer.padStart(4, '0')} · ${u.stichwort} ${u.stichwortText}",
        )
    }

    // -------------------------------------------------------- Handgriffe
    if (!einsatz.abgeschlossen) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        ) {
            Knopf(
                if (einsatz.alarmierteFahrzeuge.isNotEmpty()) "Nachalarmieren" else "Alarmieren",
                beiAlarmieren,
                art = Knopfart.Alarm,
            )
            Knopf("Umstufen", beiUmstufen)
            Knopf("Folgeeinsatz", beiFolgeeinsatz, art = Knopfart.Leise)
            Knopf(
                "Abschließen",
                { griffe.einsatzSchliessen(einsatz.id) },
                aktiv = einsatz.alarmierteFahrzeuge.isNotEmpty() && abschlussSperre == null,
            )
        }
        abschlussSperre?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }
    }

    // ------------------------------------------------------------ Kräfte
    Kraefteblock(
        einsatz = einsatz,
        raum = raum,
        fahrzeuge = fahrzeuge,
        imEinsatz = imEinsatz,
        ausgefallen = ausgefallen,
        unterbesetzt = unterbesetzt,
        offeneFunktionen = offeneFunktionen,
        offeneZugplaetze = offeneZugplaetze.size,
        kennung = kennung,
        griffe = griffe,
    )

    // --------------------------------------------------- Transportziele
    Transportziele(einsatz = einsatz, raum = raum, fahrzeuge = fahrzeuge, kennung = kennung, griffe = griffe)

    // Die jüngste Lagemeldung als Einzeiler; der Rest steht im Funkprotokoll.
    einsatz.lagemeldungen.lastOrNull()?.let { l ->
        Etikett("Zuletzt gemeldet")
        Text("${l.funkrufname}: ${l.text}", style = Schrift.Klein, color = Farben.Text)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Gross)) {
        Zeitfeld("Disposition", dauer(einsatz.dispositionszeitSekunden))
        Zeitfeld("Hilfsfrist", dauer(einsatz.hilfsfristSekunden))
        Zeitfeld("Meldungen", einsatz.lagemeldungen.size.toString())
    }
}

/** Eine Dauer in Sekunden als „m:ss min" oder „n s" — `dauer()` im Bogen. */
private fun dauer(sekunden: Double?): String {
    if (sekunden == null) return "—"
    val s = sekunden.roundToInt()
    return if (s >= 60) "${s / 60}:${(s % 60).toString().padStart(2, '0')} min" else "$s s"
}

@Composable
private fun Zeitfeld(etikett: String, wert: String) {
    Column {
        Etikett(etikett)
        Text(wert, style = Schrift.MonoNormal, color = Farben.Text)
    }
}

/** Ein gerahmter Block im Bogen — Termin, Suche, Brand, Arbeit, Objekt. */
@Composable
private fun Abschnittskasten(randfarbe: Color, inhalt: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(farbe = Farben.FlaecheHoch, randfarbe = randfarbe.copy(alpha = 0.7f), ecke = 9.dp)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        content = inhalt,
    )
}

/**
 * Eine Auswahl in einer Zeile — der Ersatz für das `<select>` des Webs: Links
 * wofür, rechts das Feld, das die Liste aufklappt.
 */
@Composable
internal fun <T> Auswahlzeile(
    beschriftung: String,
    wert: String?,
    platzhalter: String,
    titel: String,
    optionen: List<Pair<T, String>>,
    beiWahl: (T) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(beschriftung, style = Schrift.MonoKlein, color = Farben.Text, modifier = Modifier.weight(1f))
        Text(
            (wert ?: platzhalter) + "  ▾",
            style = Schrift.Klein,
            color = if (wert == null) Farben.TextSehrLeise else Farben.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1.4f)
                .background(Farben.BgTief, Rundung.Klein)
                .border(1.dp, Farben.RandHell, Rundung.Klein)
                .clickable(enabled = optionen.isNotEmpty()) { offen = true }
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        )
    }
    if (offen) {
        Wahlblende(
            titel = titel,
            gruppen = listOf(null to optionen),
            aufschrift = { it.second },
            beiWahl = {
                offen = false
                beiWahl(it.first)
            },
            beiSchliessen = { offen = false },
        )
    }
}

/**
 * Die Kräfte der Lage — kompakte Marken, dazu der Rückruf-Modus.
 *
 * <b>Ein eigener Modus und keine dauerhaft anklickbaren Marken:</b> Ein Tipp,
 * der still ein Fahrzeug abzieht, wäre genau der Fehlgriff, gegen den der Knopf
 * gebaut wurde. Erst der Modus, dann die Auswahl, dann der Rückruf.
 */
@Composable
private fun Kraefteblock(
    einsatz: Einsatz,
    raum: Raumzustand,
    fahrzeuge: List<Rundenfahrzeug>,
    imEinsatz: List<Rundenfahrzeug>,
    ausgefallen: List<Rundenfahrzeug>,
    unterbesetzt: Boolean,
    offeneFunktionen: List<String>,
    offeneZugplaetze: Int,
    kennung: (Rundenfahrzeug) -> String,
    griffe: LeitstellenGriffe,
) {
    var rueckruf by remember(einsatz.id) { mutableStateOf(false) }
    var auswahl by remember(einsatz.id) { mutableStateOf<Set<String>>(emptySet()) }
    var gesperrtBis by remember { mutableStateOf(0L) }

    fun abziehbar(f: Rundenfahrzeug) =
        f.einsatzId == einsatz.id || (f.einsatzId == null && f.status in listOf(1, 2, 6))

    val darfZurueckrufen = fahrzeuge.any { abziehbar(it) }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Etikett("Kräfte")
            SehrLeise("${imEinsatz.size} / ${einsatz.empfohleneFahrzeuge}", mono = true)
            if (einsatz.nachforderungen > 0) Marke("nachgefordert", farbe = Farben.OrangeHell)
            Box(Modifier.weight(1f))
            if (darfZurueckrufen) {
                Knopf(
                    if (rueckruf) "abbrechen" else "zurückrufen",
                    {
                        rueckruf = !rueckruf
                        auswahl = emptySet()
                    },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        }

        if (ausgefallen.isNotEmpty()) {
            Text(
                "Ausgefallen: ${ausgefallen.joinToString(", ") { kennung(it) }}" +
                    if (unterbesetzt) " — unterbesetzt, ${imEinsatz.size} von ${einsatz.empfohleneFahrzeuge}" else "",
                style = Schrift.MonoKlein,
                color = Farben.SignalHell,
            )
        }
        if (offeneFunktionen.isNotEmpty()) {
            Text("Fehlt noch: ${offeneFunktionen.joinToString(", ")}", style = Schrift.MonoKlein, color = Farben.AmberHell)
        }

        // Der Verband — die Ausrückeordnung als Liste von Plätzen statt als Zahl.
        if (einsatz.zugplaetze.isNotEmpty()) {
            Text(
                "${einsatz.verband.orEmpty()} " +
                    if (offeneZugplaetze > 0) "— $offeneZugplaetze offen" else "— steht",
                style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                color = if (offeneZugplaetze > 0) Farben.AmberHell else Farben.GruenHell,
            )
            einsatz.zugplaetze.forEach { p ->
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(
                        p.bezeichnung,
                        style = Schrift.MonoKlein,
                        color = if (p.pflicht) Farben.TextLeise else Farben.TextSehrLeise,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        p.funkrufname ?: if (p.pflicht) "offen" else "nicht besetzt",
                        style = Schrift.MonoKlein,
                        color = when {
                            p.fahrzeugId != null -> Farben.Text
                            p.pflicht -> Farben.SignalHell
                            else -> Farben.TextSehrLeise
                        },
                    )
                }
            }
        }

        if (fahrzeuge.isEmpty()) {
            SehrLeise("Noch nichts alarmiert.", mono = true)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
            ) {
                fahrzeuge.forEach { f ->
                    val waehlbar = rueckruf && abziehbar(f)
                    val gewaehlt = f.id in auswahl
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (gewaehlt) Farben.SignalTief else Farben.FlaecheHoch,
                                Rundung.Winzig,
                            )
                            .border(
                                1.dp,
                                when {
                                    gewaehlt -> Farben.SignalHell
                                    waehlbar -> Farben.Amber
                                    else -> Rundentexte.organisationFarbe(f.organisation).copy(alpha = 0.6f)
                                },
                                Rundung.Winzig,
                            )
                            .clickable(enabled = waehlbar) {
                                auswahl = if (gewaehlt) auswahl - f.id else auswahl + f.id
                            }
                            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                    ) {
                        Text(
                            f.status.toString(),
                            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                            color = fmsFarbe(f.status),
                        )
                        Text(kennung(f), style = Schrift.MonoKlein, color = Farben.Text)
                    }
                }
            }
        }

        if (rueckruf) {
            SehrLeise(
                "Die gewählten Fahrzeuge sind auf dieser Lage nicht mehr gebunden und melden sich " +
                    "einsatzbereit. Wer schon eingerückt ist, verliert nur die Zuordnung und lässt sich " +
                    "danach neu alarmieren. Der Einsatz läuft weiter.",
            )
            Knopf(
                "${if (auswahl.isEmpty()) "" else "${auswahl.size} "}zurückrufen",
                {
                    val jetzt = System.currentTimeMillis()
                    if (auswahl.isNotEmpty() && jetzt >= gesperrtBis) {
                        gesperrtBis = jetzt + 1_500
                        griffe.zurueckrufen(einsatz.id, auswahl.toList())
                        rueckruf = false
                        auswahl = emptySet()
                    }
                },
                art = Knopfart.Haupt,
                aktiv = auswahl.isNotEmpty(),
                kompakt = true,
            )
        }
    }
}

/**
 * Die Transportziele — sichtbar, sobald ein Rettungsmittel dieser Lage einen
 * Patienten an Bord hat (Status 7). Nach Fahrzeit sortiert; ungeeignete Häuser
 * stehen blass, abgemeldete Abteilungen stehen dabei.
 */
@Composable
private fun Transportziele(
    einsatz: Einsatz,
    raum: Raumzustand,
    fahrzeuge: List<Rundenfahrzeug>,
    kennung: (Rundenfahrzeug) -> String,
    griffe: LeitstellenGriffe,
) {
    fun machtPatiententransport(f: Rundenfahrzeug) =
        f.organisation == "Rettungsdienst" && "Transport" in f.faehigkeiten &&
            f.faehigkeiten.none { it == "Betreuung" || it == "Ortung" }

    val transporte = fahrzeuge.filter {
        it.einsatzId == einsatz.id && it.status == 7 && machtPatiententransport(it)
    }
    if (transporte.isEmpty()) return

    var gewaehlteId by remember(einsatz.id) { mutableStateOf<String?>(null) }
    // Immer das erste Fahrzeug ohne Ziel vorschlagen — auf das wartet gerade jemand.
    val ziel = transporte.firstOrNull { it.id == gewaehlteId }
        ?: transporte.firstOrNull { it.zielklinikId == null }
        ?: transporte.first()

    val manvPatient = einsatz.manvPatienten.firstOrNull {
        it.status == "ImTransport" && it.transportVehicleId == ziel.id
    }
    val kategorie = manvPatient?.kategorie
    val bedarf = manvPatient?.bedarf ?: einsatz.benoetigteVersorgung
    val umlandOffen = ziel.istLuftfahrzeug && (einsatz.manv || einsatz.prioritaet >= 3)

    fun nimmtAn(k: Klinik): Boolean {
        if (kategorie != null) return manvFach(k, kategorie) != null
        val b = einsatz.benoetigteVersorgung ?: return true
        if (raum.kliniken.none { b in it.abteilungen && b !in it.abgemeldet }) return true
        return b in k.abteilungen && b !in k.abgemeldet
    }

    fun meter(k: Klinik): Double {
        val lat = ziel.lat ?: return 0.0
        val lon = ziel.lon ?: return 0.0
        return distanzMeterAsin(lat, lon, k.lat, k.lon) * 1.3
    }

    fun bettenText(k: Klinik): String? {
        val fach = if (kategorie != null) manvFach(k, kategorie) else einsatz.benoetigteVersorgung
        fach ?: return null
        val betten = k.freieBetten[fach] ?: return null
        val name = if (kategorie != null) "${versorgungText(fach)}: " else ""
        return "$name$betten Bett${if (betten == 1) "" else "en"} frei"
    }

    val kliniken = raum.kliniken
        .filter { !it.imUmland || umlandOffen }
        .map { it to meter(it) }
        .sortedBy { it.second }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Etikett("Transportziel")
            bedarf?.let { Marke("${versorgungText(it)} nötig", farbe = Farben.AmberHell) }
        }
        if (transporte.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                transporte.forEach { f ->
                    Knopf(
                        kennung(f) + if (f.zielklinikId == null) " · wartet" else "",
                        { gewaehlteId = f.id },
                        art = if (f.id == ziel.id) Knopfart.Haupt else Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }
        }
        SehrLeise(
            "${ziel.funkrufname} — " +
                if (ziel.zielklinikId != null) "unterwegs; ein zweiter Tipp leitet um" else "wartet auf ein Transportziel",
            mono = true,
        )
        kliniken.forEach { (k, m) ->
            val passt = nimmtAn(k)
            val istZiel = ziel.zielklinikId == k.id
            val prio = if (ziel.sondersignalAus) 1 else einsatz.prioritaet
            val minuten = kotlin.math.max(1, (m / tempoMs(raum, ziel.organisation, prio) / 60).roundToInt())
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(
                        farbe = if (istZiel) Farben.FlaecheAktiv else Farben.FlaecheHoch,
                        randfarbe = if (istZiel) Farben.Amber else Farben.Rand,
                        ecke = 9.dp,
                    )
                    .clickable { griffe.zielklinik(ziel.id, k.id) }
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text(
                    buildString {
                        append(k.name)
                        if (k.istUeberregional) append(" · überregional")
                        if (k.imUmland) append(" · Umland${k.entfernungKm?.let { " · $it km" } ?: ""}")
                        if (k.hatLandeplatz) append(" · Landeplatz")
                    },
                    style = Schrift.Klein,
                    color = if (passt) Farben.Text else Farben.TextSehrLeise,
                )
                Text(
                    "$minuten min · ${if (m >= 1000) "%.1f km".format(m / 1000) else "${m.roundToInt()} m"}",
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                )
                if (k.abgemeldet.isNotEmpty()) {
                    Text(
                        "abgemeldet: ${k.abgemeldet.joinToString(", ") { versorgungText(it) }}",
                        style = Schrift.MonoKlein,
                        color = Farben.SignalHell,
                    )
                } else {
                    Text(
                        (bettenText(k)?.let { "$it · " } ?: "") + "alle Abteilungen angemeldet",
                        style = Schrift.MonoKlein,
                        color = Farben.GruenHell,
                    )
                }
            }
        }
    }
}

/**
 * Die Kulisse eines Event-Einsatzes — über dem Kopf, nicht darin: Sie ist der
 * Rahmen, in dem die Lage steht. Darunter, was das ganze Event noch braucht.
 */
@Composable
private fun Eventkulisse(event: Spielevent, einsatz: Einsatz, raum: Raumzustand) {
    val ton = event.farbe?.let { farbeVon(it) } ?: Farben.Violett
    val banner by bildVon(event.bannerUrl?.takeIf { it.startsWith("http") })

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(farbe = Farben.FlaecheHoch, randfarbe = ton, ecke = 10.dp)
            .padding(Abstand.Normal),
    ) {
        banner?.let { b ->
            Image(
                bitmap = b,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(72.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text(event.titel, style = Schrift.Gross, color = Farben.Text, modifier = Modifier.weight(1f))
            if (event.zustand == "Laeuft") Marke("Läuft", farbe = ton)
        }
        SehrLeise("${zeitpunkt(event.beginntUm)} – ${zeitpunkt(event.endetUm)}")
        event.beschreibung?.takeIf { it.isNotBlank() }?.let { Text(it, style = Schrift.Klein, color = Farben.TextLeise) }
        SehrLeise("Event-Einsatz · ${event.stichwort} ${event.stichwortText}")

        // Was das ganze Event noch braucht — die Frage, wohin der freie Wagen gehört.
        val geschwister = raum.incidents.filter { it.eventId == einsatz.eventId && !it.abgeschlossen }
        val bedarf = linkedMapOf<String, MutableList<String>>()
        geschwister.forEach { e ->
            val abgedeckt = raum.vehicles.filter { it.einsatzId == e.id }.flatMap { it.faehigkeiten }.toSet()
            e.empfohleneFaehigkeiten.filter { it !in abgedeckt }.forEach { f ->
                bedarf.getOrPut(f) { mutableListOf() }.add(e.einsatznummer)
            }
        }
        if (bedarf.isNotEmpty()) {
            val offen = bedarf.values.flatten().toSet().size
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Etikett("Noch gebraucht im Event")
                SehrLeise("$offen von ${geschwister.size} Lagen")
            }
            bedarf.entries
                .sortedWith(compareByDescending<Map.Entry<String, MutableList<String>>> { it.value.size }.thenBy { it.key })
                .forEach { (funktion, stellen) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text(funktion, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                        Text(
                            stellen.joinToString(" "),
                            style = Schrift.MonoKlein,
                            color = if (einsatz.einsatznummer in stellen) Farben.Amber else Farben.TextSehrLeise,
                        )
                    }
                }
        }
    }
}

/** Eine CSS-Farbe „#rrggbb" als Farbe — `null`, wenn sie nicht zu lesen ist. */
private fun farbeVon(css: String): Color? = runCatching {
    val hex = css.trim().removePrefix("#")
    when (hex.length) {
        6 -> Color(("FF$hex").toLong(16))
        8 -> Color(hex.toLong(16))
        else -> null
    }
}.getOrNull()
