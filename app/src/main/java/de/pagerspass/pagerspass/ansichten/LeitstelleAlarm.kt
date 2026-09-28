package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Leitstellenstand
import de.pagerspass.pagerspass.netz.AaoVorlagenzeile
import de.pagerspass.pagerspass.netz.Aaosicherung
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Regler
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Der Alarmdialog — `AlarmDialog.vue`.
 *
 * <b>Er alarmiert nicht selbst.</b> Er sagt nur, was gewählt wurde; der Tisch
 * hält den Alarm dann fünf Sekunden im Rückholfenster (siehe `Rueckholer`).
 *
 * <b>Beim ersten Alarm ist die Ordnung vorgewählt, beim Nachalarmieren nicht:</b>
 * Wer das eine fehlende Fahrzeug will, soll nicht die halbe Ordnung dazu
 * angehakt bekommen. Die Vorschläge stehen dann als „Vorschlag" markiert, und
 * „AAO übernehmen" holt sie auf Wunsch.
 *
 * Voreingestellt ist die Sortierung nach <b>Eintreffzeit</b> — dieselbe Zahl,
 * nach der der Server seinen Vorschlag ordnet: Ausrücken plus Anfahrt.
 */
@Composable
internal fun Alarmblende(
    einsatz: Einsatz,
    raum: Raumzustand,
    katalog: Katalog?,
    konto: Konto?,
    daten: Leitstellenstand,
    griffe: LeitstellenGriffe,
    beiAlarmieren: (Alarmauftrag) -> Unit,
    beiSchliessen: () -> Unit,
) {
    val bereich = rememberCoroutineScope()
    val kennung = rememberKennung(raum)
    val kennzahl = kennungIstKennzahl()
    val mikrofonDa = rememberMikrofonpruefung()

    var gewaehlt by remember(einsatz.id) { mutableStateOf<Set<String>>(emptySet()) }
    var angefasst by remember(einsatz.id) { mutableStateOf(false) }
    var vorschlagIds by remember(einsatz.id) { mutableStateOf<List<String>>(emptyList()) }
    var eintreffSekunden by remember(einsatz.id) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var abAuswahl by remember(einsatz.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var zeigeOrdnung by remember { mutableStateOf(false) }
    var anzahl by remember(einsatz.id) { mutableIntStateOf(einsatz.empfohleneFahrzeuge) }
    var faehigkeiten by remember(einsatz.id) { mutableStateOf(einsatz.empfohleneFaehigkeiten) }
    var suche by remember { mutableStateOf("") }
    var sortierung by remember { mutableStateOf(Sortierung.Eintreffzeit) }
    var meldung by remember { mutableStateOf("") }
    var abwahlFuer by remember { mutableStateOf<String?>(null) }

    val aufnahme = remember { Alarmaufnahme() }
    val aktuelleGriffe by rememberUpdatedState(griffe)

    val aaoFrei = freigeschaltet(raum, konto, "EigeneAao")
    val nachalarmierung = einsatz.alarmierteFahrzeuge.isNotEmpty()

    // Was am Einsatz steht, gewinnt — auch wenn eine zweite Leitstelle die
    // Ordnung ändert oder der Server die Zahl beschneidet.
    LaunchedEffect(einsatz.empfohleneFahrzeuge, einsatz.empfohleneFaehigkeiten) {
        anzahl = einsatz.empfohleneFahrzeuge
        faehigkeiten = einsatz.empfohleneFaehigkeiten
    }

    val abVorlagen = katalog?.fahrzeuge.orEmpty().filter { it.kategorie == "Abrollbehälter" }
    val abIds = abVorlagen.map { it.id }.toSet()
    val verfuegbar = raum.vehicles.filter { it.einsatzId != einsatz.id && it.templateId !in abIds }

    suspend fun vorschlagHolen(uebernehmen: Boolean, nurUnberuehrt: Boolean) {
        val ids = aktuelleGriffe.alarmvorschlag(einsatz.id)
        eintreffSekunden = aktuelleGriffe.eintreffzeiten(einsatz.id)
        vorschlagIds = ids
        if (!uebernehmen || (nurUnberuehrt && angefasst)) return
        val frei = verfuegbar.map { it.id }.toSet()
        gewaehlt = gewaehlt + ids.filter { it in frei }
    }

    LaunchedEffect(einsatz.id) {
        vorschlagHolen(uebernehmen = !nachalarmierung, nurUnberuehrt = true)
    }

    // Die Aufnahme läuft nie ohne Dialog weiter, und was liegen bleibt, wird
    // verworfen — außer sie gehört inzwischen einem Alarm.
    DisposableEffect(Unit) {
        onDispose {
            val id = aufnahme.meldungId
            if (aufnahme.spricht && id != null) aktuelleGriffe.alarmMeldungAbschliessen(id)
            if (id != null) aktuelleGriffe.alarmMeldungVerwerfen(id)
        }
    }

    LaunchedEffect(aufnahme.spricht) {
        while (aufnahme.spricht && isActive) {
            delay(1_000)
            if (aufnahme.spricht) aufnahme.sekunden += 1
        }
    }

    // ------------------------------------------------------------- WLF

    val gewaehlteWlf = raum.vehicles.filter { it.id in gewaehlt && it.templateId == "wlfab" }
    val belegteAb = buildSet {
        raum.vehicles.forEach { f ->
            f.abrollbehaelterTemplateId?.let { add(it) }
            if (f.templateId in abIds) add(f.templateId)
        }
    }
    val gefordert = einsatz.empfohleneFaehigkeiten.map { it.lowercase() }.toSet()
    fun abPasst(ab: Fahrzeugvorlage) = ab.faehigkeiten.any { it.lowercase() in gefordert }
    fun abOptionen(wlfId: String): List<Fahrzeugvorlage> {
        val andere = abAuswahl.filterKeys { it != wlfId }.values.toSet()
        return abVorlagen
            .filter { it.id !in belegteAb && it.id !in andere }
            .sortedBy { if (abPasst(it)) 0 else 1 }
    }

    val wlfOhneAb = gewaehlteWlf.filter { abOptionen(it.id).isEmpty() }
    val abFehlt = gewaehlteWlf.any { abAuswahl[it.id].isNullOrEmpty() }
    val alarmSperre: String? = when {
        gewaehlt.isEmpty() -> "Kein Fahrzeug gewählt"
        wlfOhneAb.isNotEmpty() -> "Kein Abrollbehälter frei für ${wlfOhneAb.joinToString(", ") { kennung(it) }}"
        abFehlt -> "Zuerst für jedes WLF einen AB wählen"
        else -> null
    }

    // ------------------------------------------------------ Liste

    fun wegMeter(f: Rundenfahrzeug): Double {
        val eLat = einsatz.lat ?: return Double.POSITIVE_INFINITY
        val eLon = einsatz.lon ?: return Double.POSITIVE_INFINITY
        val fLat = f.lat ?: return Double.POSITIVE_INFINITY
        val fLon = f.lon ?: return Double.POSITIVE_INFINITY
        return distanzMeter(fLat, fLon, eLat, eLon)
    }

    fun eintreffRang(f: Rundenfahrzeug): Double = eintreffSekunden[f.id]?.toDouble() ?: wegMeter(f)

    val treffer = verfuegbar.filter { fahrzeugPasst(it, suche) }
    val gefiltert: List<Rundenfahrzeug> = when (sortierung) {
        Sortierung.Funkrufname -> treffer.sortedWith(rufnummerVergleich)
        Sortierung.Organisation -> gruppiereTableau(treffer).flatMap { it.fahrzeuge }
        Sortierung.Eintreffzeit -> treffer.sortedWith(
            Comparator<Rundenfahrzeug> { a, b -> eintreffRang(a).compareTo(eintreffRang(b)) }
                .then(rufnummerVergleich),
        )
    }
    val gruppenkoepfe = if (sortierung == Sortierung.Organisation) {
        gruppiereTableau(treffer).associateBy { it.fahrzeuge.first().id }
    } else {
        emptyMap()
    }

    val schleifen = verfuegbar
        .flatMap { f -> f.schleifen.map { it to f.id } }
        .groupBy({ it.first }, { it.second })
        .toSortedMap()

    fun umschalten(id: String) {
        angefasst = true
        if (id in gewaehlt) {
            gewaehlt = gewaehlt - id
            abAuswahl = abAuswahl - id
        } else {
            gewaehlt = gewaehlt + id
        }
    }

    fun schleifeWaehlen(ids: List<String>) {
        angefasst = true
        val alleDrin = ids.all { it in gewaehlt }
        if (alleDrin) {
            gewaehlt = gewaehlt - ids.toSet()
            abAuswahl = abAuswahl - ids.toSet()
        } else {
            gewaehlt = gewaehlt + ids
        }
    }

    fun alarmieren() {
        if (alarmSperre != null) return
        val id = aufnahme.meldungId
        if (aufnahme.spricht && id != null) {
            aufnahme.spricht = false
            aktuelleGriffe.alarmMeldungAbschliessen(id)
        }
        // Ohne Text keine Stimme — dieselbe Regel, die der Server durchsetzt.
        val mitgegeben = if (meldung.isNotBlank()) aufnahme.meldungId else null
        beiAlarmieren(
            Alarmauftrag(
                einsatzId = einsatz.id,
                fahrzeugIds = gewaehlt.toList(),
                abrollbehaelter = abAuswahl.filterKeys { k -> gewaehlteWlf.any { it.id == k } },
                zusatztext = meldung.trim().ifEmpty { null },
                meldungId = mitgegeben,
            ),
        )
        // Die Aufnahme gehört jetzt dem Alarm — der Abbau darf sie nicht verwerfen.
        if (mitgegeben != null) aufnahme.meldungId = null
        beiSchliessen()
    }

    fun sprechenStarten() {
        if (aufnahme.spricht) return
        aufnahme.fehler = null
        aufnahme.meldungId?.let { aktuelleGriffe.alarmMeldungVerwerfen(it) }
        aufnahme.meldungId = null
        aufnahme.sekunden = 0
        val id = UUID.randomUUID().toString()
        if (!aktuelleGriffe.alarmMeldungStarten(id)) {
            aufnahme.fehler = "Kein Mikrofonzugriff. Tippen geht trotzdem."
            return
        }
        aufnahme.meldungId = id
        aufnahme.spricht = true
    }

    fun sprechenBeenden() {
        if (!aufnahme.spricht) return
        aufnahme.spricht = false
        val id = aufnahme.meldungId ?: return
        bereich.launch {
            if (!aktuelleGriffe.alarmMeldungBeenden(id) && aufnahme.meldungId == id) {
                // Nichts angekommen — versehentlich angetippt oder stumm geblieben.
                aufnahme.meldungId = null
                aufnahme.sekunden = 0
            }
        }
    }

    val hatAufnahme = aufnahme.meldungId != null && !aufnahme.spricht && aufnahme.sekunden > 0

    Blende(
        titel = "${einsatz.stichwort} · ${einsatz.stichwortText}",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Breit,
        kopfknoepfe = { Knopf("Abbrechen", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
        fussAlsSpalte = true,
        fuss = {
            // Die eigene Meldung steht im Fuß — dort kann sie nicht herausfallen.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Etikett("Eigene Meldung")
                SehrLeise("· zusätzlich auf dem Melder")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gedrückt gehalten, nicht geschaltet: ein Mikrofon, das im
                // Alarmfenster offen stehen bleibt, will niemand.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 96.dp, minHeight = 44.dp)
                        .background(
                            if (aufnahme.spricht) Farben.SignalTief else Farben.FlaecheHoch,
                            Rundung.Klein,
                        )
                        .border(1.dp, if (aufnahme.spricht) Farben.SignalHell else Farben.RandHell, Rundung.Klein)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    // Ohne Erlaubnis wird nur gefragt — gesprochen wird
                                    // beim nächsten Druck, nicht nach dem Loslassen.
                                    if (mikrofonDa()) {
                                        sprechenStarten()
                                        tryAwaitRelease()
                                        sprechenBeenden()
                                    }
                                },
                            )
                        }
                        .padding(horizontal = Abstand.Normal),
                ) {
                    Text(
                        when {
                            aufnahme.spricht -> "● ${aufnahme.sekunden} s"
                            hatAufnahme -> "Neu sprechen"
                            else -> "Sprechen"
                        },
                        style = Schrift.Klein,
                        color = if (aufnahme.spricht) Farben.SignalHell else Farben.Text,
                    )
                }
                Feld(
                    wert = meldung,
                    beiAenderung = { meldung = it.take(120) },
                    platzhalter = "z. B. Person am Fenster, Zufahrt über den Hof …",
                    modifier = Modifier.weight(1f),
                )
            }
            val fehler = aufnahme.fehler
            when {
                fehler != null -> Text(fehler, style = Schrift.Klein, color = Farben.AmberHell)
                hatAufnahme && meldung.isBlank() -> Text(
                    "Schreib dazu, was du gesagt hast — sonst geht nur der Alarm hinaus, nicht deine Stimme.",
                    style = Schrift.Klein,
                    color = Farben.AmberHell,
                )
                hatAufnahme -> Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "● ${aufnahme.sekunden} s gesprochen — die Alarmierten hören deine Stimme.",
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                        modifier = Modifier.weight(1f),
                    )
                    Knopf(
                        "Verwerfen",
                        {
                            aufnahme.meldungId?.let { aktuelleGriffe.alarmMeldungVerwerfen(it) }
                            aufnahme.meldungId = null
                            aufnahme.sekunden = 0
                        },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }

            // Was den Alarm aufhält, steht dort, wo er ausgelöst wird.
            if (gewaehlteWlf.isNotEmpty()) {
                Etikett("WLF ausrüsten")
                gewaehlteWlf.forEach { f ->
                    val optionen = abOptionen(f.id)
                    if (optionen.isEmpty()) {
                        Text(
                            "${kennung(f)}: Kein Abrollbehälter frei — alle sind aufgesattelt oder vergeben.",
                            style = Schrift.Klein,
                            color = Farben.AmberHell,
                        )
                    } else {
                        val ab = abVorlagen.firstOrNull { it.id == abAuswahl[f.id] }
                        Auswahlzeile(
                            beschriftung = kennung(f),
                            wert = ab?.let { abText(it, abPasst(it)) },
                            platzhalter = "Abrollbehälter wählen …",
                            titel = "Abrollbehälter für ${f.funkrufname}",
                            optionen = optionen.map { it.id to abText(it, abPasst(it)) },
                            beiWahl = { abAuswahl = abAuswahl + (f.id to it) },
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (alarmSperre != null && gewaehlt.isNotEmpty()) {
                        alarmSperre
                    } else {
                        "${gewaehlt.size} ausgewählt" +
                            if (suche.isNotBlank()) " · ${gefiltert.size} von ${verfuegbar.size}" else ""
                    },
                    style = Schrift.Klein,
                    color = if (alarmSperre != null && gewaehlt.isNotEmpty()) Farben.AmberHell else Farben.TextLeise,
                    modifier = Modifier.weight(1f),
                )
                Knopf("Alarm auslösen", { alarmieren() }, art = Knopfart.Alarm, aktiv = alarmSperre == null)
            }
        },
    ) {
        Text(einsatz.adresse, style = Schrift.Klein, color = Farben.TextLeise)

        // Die Empfehlung lässt sich aufklappen und ändern — zugeklappt sieht die
        // Zeile aus wie vorher, damit der eilige Griff derselbe bleibt.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { zeigeOrdnung = !zeigeOrdnung }
                .padding(vertical = Abstand.Winzig),
        ) {
            Text(if (zeigeOrdnung) "▾" else "▸", style = Schrift.Klein, color = Farben.TextLeise)
            Text("Alarm- und Ausrückeordnung", style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text)
            einsatz.verband?.let { Text(it, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell) }
            Text(
                "${einsatz.empfohleneFahrzeuge} Fzg" +
                    if (einsatz.empfohleneFaehigkeiten.isNotEmpty()) ", ${einsatz.empfohleneFaehigkeiten.joinToString(", ")}" else "",
                style = Schrift.MonoKlein,
                color = Farben.TextSehrLeise,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!aaoFrei) Text("🔒", style = Schrift.Klein)
        }

        if (zeigeOrdnung) {
            AaoOrdnung(
                anzahl = anzahl,
                faehigkeiten = faehigkeiten,
                beiAnzahl = { anzahl = it },
                beiFaehigkeiten = { faehigkeiten = it },
                beiGeaendert = {
                    // Sofort an den Server — der Vorschlag rechnet aus der Ordnung.
                    if (aaoFrei) {
                        griffe.einsatzAendern(einsatz.id, null, null, null, null, anzahl, faehigkeiten)
                        bereich.launch {
                            delay(300)
                            vorschlagHolen(uebernehmen = !nachalarmierung, nurUnberuehrt = false)
                        }
                    }
                },
                raum = raum,
                katalog = katalog,
                konto = konto,
                daten = daten,
                griffe = griffe,
            )
            return@Blende
        }

        // Alarmschleifen als Sammelauswahl — so alarmiert man ganze Züge.
        if (schleifen.isNotEmpty()) {
            Etikett("Schleifen")
            Box(Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState())) {
                Pillenreihe {
                    schleifen.forEach { (name, ids) ->
                        Pille(
                            aufschrift = "$name (${ids.size})",
                            an = ids.all { it in gewaehlt },
                            beiDruck = { schleifeWaehlen(ids) },
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Feld(
                wert = suche,
                beiAenderung = { suche = it },
                platzhalter = "⌕ Rufname, Typ, Fähigkeit …",
                modifier = Modifier.weight(1f),
            )
            if (suche.isNotBlank() && gefiltert.isNotEmpty()) {
                Knopf(
                    "alle ${gefiltert.size}",
                    {
                        angefasst = true
                        val alleDrin = gefiltert.all { it.id in gewaehlt }
                        gewaehlt = if (alleDrin) gewaehlt - gefiltert.map { it.id }.toSet()
                        else gewaehlt + gefiltert.map { it.id }
                    },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SehrLeise("Sortiert nach")
            Segment(
                seiten = Sortierung.entries,
                gewaehlt = sortierung,
                beiWahl = { sortierung = it },
                aufschrift = { it.wort },
                modifier = Modifier.weight(1f),
            )
        }

        if (nachalarmierung || gewaehlt.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                if (nachalarmierung) {
                    Knopf(
                        "AAO übernehmen" + if (vorschlagIds.isNotEmpty()) " (${vorschlagIds.size})" else "",
                        { bereich.launch { vorschlagHolen(uebernehmen = true, nurUnberuehrt = false) } },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
                if (gewaehlt.isNotEmpty()) {
                    Knopf(
                        "Alle abwählen",
                        {
                            angefasst = true
                            gewaehlt = emptySet()
                            abAuswahl = emptyMap()
                        },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }
        }

        if (nachalarmierung && gewaehlt.isEmpty()) {
            SehrLeise(
                "Nachalarmierung: Es ist nichts vorgewählt. Die Fahrzeuge der Ordnung sind als " +
                    "Vorschlag markiert.",
            )
        }

        when {
            verfuegbar.isEmpty() -> SehrLeise("Alle Fahrzeuge laufen bereits auf diesem Einsatz.", mono = true)
            gefiltert.isEmpty() -> SehrLeise("Kein Fahrzeug passt zu „$suche“.", mono = true)
        }

        gefiltert.forEach { f ->
            gruppenkoepfe[f.id]?.let { g ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = Abstand.Klein),
                ) {
                    Box(Modifier.width(3.dp).size(3.dp, 14.dp).background(g.farbe))
                    Etikett(g.label)
                    SehrLeise("${g.fahrzeuge.size}", mono = true)
                }
            }
            Alarmzeile(
                fahrzeug = f,
                raum = raum,
                einsatz = einsatz,
                an = f.id in gewaehlt,
                vorschlag = f.id in vorschlagIds,
                eintreffSekunden = eintreffSekunden[f.id],
                kennung = kennung(f),
                typZeigen = kennzahl,
                beiDruck = { umschalten(f.id) },
            )
        }
    }
}

/** Die drei Ordnungen der Fahrzeugliste. */
internal enum class Sortierung(val wort: String) {
    Eintreffzeit("Eintreffzeit"),
    Funkrufname("Funkrufname"),
    Organisation("Organisation"),
}

/** Die gesprochene Meldung im Alarmdialog — ihr Stand über die Neuzeichnungen hinweg. */
private class Alarmaufnahme {
    var meldungId by mutableStateOf<String?>(null)
    var spricht by mutableStateOf(false)
    var sekunden by mutableIntStateOf(0)
    var fehler by mutableStateOf<String?>(null)
}

private fun abText(ab: Fahrzeugvorlage, passt: Boolean): String =
    (if (passt) "★ gefordert · " else "") + ab.typ.replace("WLF + ", "") +
        " · " + ab.faehigkeiten.joinToString(", ")

/**
 * Eine Fahrzeugzeile im Alarmdialog — Status, Kennung, Besatzung, dann Weg,
 * Ausrückzeit und die Summe aus beidem, nach der der Vorschlag entscheidet.
 */
@Composable
private fun Alarmzeile(
    fahrzeug: Rundenfahrzeug,
    raum: Raumzustand,
    einsatz: Einsatz,
    an: Boolean,
    vorschlag: Boolean,
    eintreffSekunden: Int?,
    kennung: String,
    typZeigen: Boolean,
    beiDruck: () -> Unit,
) {
    val f = fahrzeug
    val marke = besatzungsmarke(raum.players, f.playerId)
    val weg = if (einsatz.lat != null && einsatz.lon != null && f.lat != null && f.lon != null) {
        formatEntfernung(distanzMeter(f.lat, f.lon, einsatz.lat, einsatz.lon))
    } else {
        null
    }
    val ausruecken = when {
        f.flugHindernis != null -> "nicht flugbereit — ${f.flugHindernis}"
        !f.besatzungVerfuegbar -> "keine Besatzung"
        f.ausrueckzeitSekunden <= 0 -> null
        else -> "+${(f.ausrueckzeitSekunden / 60.0).roundToInt()} min"
    }
    val eintreffen = eintreffSekunden?.takeIf { it > 0 }?.let { "≈${max(1, (it / 60.0).roundToInt())} min" }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (an) Farben.FlaecheAktiv else Farben.FlaecheHoch,
                randfarbe = if (an) Farben.Amber else Rundentexte.organisationFarbe(f.organisation).copy(alpha = 0.5f),
                ecke = 9.dp,
            )
            .clickable(onClick = beiDruck)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).background(fmsFarbe(f.status), CircleShape),
        ) {
            Text(f.status.toString(), style = Schrift.MarkeZahl, color = Farben.AufFarbe)
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    kennung,
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                marke?.let { (mensch, text) ->
                    Text(
                        text,
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = if (mensch) Farben.GruenHell else Farben.TextSehrLeise,
                        maxLines = 1,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                if (typZeigen) SehrLeise(f.typ)
                weg?.let { SehrLeise(it, mono = true) }
                ausruecken?.let {
                    Text(
                        it,
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = if (!f.besatzungVerfuegbar || f.flugHindernis != null) Farben.SignalHell else Farben.TextLeise,
                    )
                }
                eintreffen?.let { Text(it, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.BlauHell) }
                if (vorschlag) {
                    Text("Vorschlag", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.Amber)
                } else if (f.einsatzId != null) {
                    Text("gebunden", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.OrangeHell)
                }
            }
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(if (an) Farben.Amber else Farben.BgTief, Rundung.Winzig)
                .border(1.dp, if (an) Farben.Amber else Farben.RandHell, Rundung.Winzig),
        )
    }
}

/** Ob eine Zusatzfunktion offensteht — die Runde darf alle Schranken abschalten. */
internal fun freigeschaltet(raum: Raumzustand?, konto: Konto?, was: String): Boolean {
    if (raum?.settings?.freischaltungenIgnorieren == true) return true
    return konto?.freischaltungen?.firstOrNull { it.was == was }?.offen ?: false
}

internal fun abRang(konto: Konto?, was: String): String =
    konto?.freischaltungen?.firstOrNull { it.was == was }?.abRang.orEmpty()

/** Was der Bogen einer Ordnung mitgeben kann, wenn sie als Stichwort gesichert wird. */
internal data class Bogenangaben(
    val stichwort: String,
    val stichwortText: String,
    val meldebild: String,
    val adresse: String,
)

/**
 * Die Alarm- und Ausrückeordnung — `AaoOrdnung.vue`, derselbe Block im
 * Alarmdialog und im Einsatzbogen.
 *
 * Zahl und Funktionen stellen, eine gespeicherte Ordnung übernehmen, die
 * zurechtgelegte sichern (für die Schicht, den Kreis oder alle Runden) und —
 * zugeklappt — die gespeicherten verwalten.
 */
@Composable
internal fun AaoOrdnung(
    anzahl: Int,
    faehigkeiten: List<String>,
    beiAnzahl: (Int) -> Unit,
    beiFaehigkeiten: (List<String>) -> Unit,
    beiGeaendert: () -> Unit,
    raum: Raumzustand,
    katalog: Katalog?,
    konto: Konto?,
    daten: Leitstellenstand,
    griffe: LeitstellenGriffe,
    umstufen: Boolean = false,
    bogen: Bogenangaben? = null,
    beiUebernommen: (AaoVorlagenzeile) -> Unit = {},
) {
    val frei = freigeschaltet(raum, konto, "EigeneAao")
    val landkreisId = raum.settings.landkreisId

    LaunchedEffect(Unit) { if (daten.bestand == null) griffe.ordnungenLaden() }

    var name by remember { mutableStateOf("") }
    var geltung by remember { mutableStateOf(if (landkreisId != null) "kreis" else "alle") }
    var alsStichwort by remember { mutableStateOf(true) }
    var vorlagenwahl by remember { mutableStateOf(false) }
    var gewaehlteVorlage by remember { mutableStateOf<String?>(null) }
    var zeigeVerwaltung by remember { mutableStateOf(false) }
    var lokaleMeldung by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lokaleMeldung) {
        if (lokaleMeldung != null) {
            delay(4_000)
            lokaleMeldung = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        if (!frei) {
            Text(
                "Die eigene Alarm- und Ausrückeordnung steht ab ${abRang(konto, "EigeneAao")} offen. Bis dahin " +
                    "gilt, was der Katalog zum Stichwort vorsieht — sichern und verwalten kannst du deine " +
                    "Ordnungen trotzdem.",
                style = Schrift.Klein,
                color = Farben.AmberHell,
            )
        }

        Regler(
            wert = anzahl,
            beiAenderung = { beiAnzahl(it) },
            von = 1,
            bis = max(MAX_FAHRZEUGE_AAO, anzahl),
            etikett = "Empfohlene Fahrzeuge: $anzahl",
            aktiv = frei,
            beiLoslassen = beiGeaendert,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Etikett("Geforderte Funktionen")
            SehrLeise("Danach schlägt die Alarmierung passende Fahrzeuge vor.")
            Pillenreihe {
                katalog?.faehigkeiten.orEmpty().forEach { k ->
                    Pille(
                        aufschrift = k,
                        an = k in faehigkeiten,
                        aktiv = frei,
                        beiDruck = {
                            beiFaehigkeiten(if (k in faehigkeiten) faehigkeiten - k else faehigkeiten + k)
                            beiGeaendert()
                        },
                    )
                }
            }
        }

        // Übernehmen und Sichern gehören beim Umstufen nicht hierher.
        if (umstufen) return@Column

        val schicht = raum.aaoVorlagen
        val dauer = daten.ordnungen
        if (schicht.isNotEmpty() || dauer.isNotEmpty()) {
            Wahlfeld(
                etikett = "Gespeicherte Ordnung übernehmen",
                wert = gewaehlteVorlage,
                platzhalter = "— eigene Angaben —",
                beiDruck = { vorlagenwahl = true },
                aktiv = frei,
            )
            SehrLeise("Eine eigene Ordnung wandert dabei in die Schicht — dann hat der zweite Disponent sie auch.")
        }

        Etikett("Als Ordnung sichern")
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
            Feld(
                wert = name,
                beiAenderung = { name = it.take(40) },
                platzhalter = "z. B. Zimmerbrand groß",
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "Sichern",
                {
                    val titel = name.trim()
                    if (titel.isNotEmpty() && !daten.sichert) {
                        griffe.schichtordnung(titel, anzahl, faehigkeiten)
                        if (geltung == "schicht") {
                            lokaleMeldung = "„$titel“ gilt für diese Schicht."
                        } else {
                            val bogenStichwort = bogen?.stichwort?.trim().orEmpty()
                            val mitStichwort = bogen != null && alsStichwort && bogenStichwort.isNotEmpty()
                            griffe.ordnungSichern(
                                Aaosicherung(
                                    name = titel,
                                    landkreisId = if (geltung == "kreis") landkreisId else null,
                                    empfohleneFahrzeuge = anzahl,
                                    empfohleneFaehigkeiten = faehigkeiten,
                                    stichwort = if (mitStichwort) bogenStichwort else null,
                                    stichwortText = if (mitStichwort) bogen?.stichwortText?.trim() else null,
                                    meldebild = bogen?.meldebild?.trim()?.ifEmpty { null },
                                    adresse = bogen?.adresse?.trim()?.ifEmpty { null },
                                    stichwortAendern = bogen != null,
                                    lageAendern = bogen != null,
                                ),
                            )
                        }
                        name = ""
                    }
                },
                art = Knopfart.Leise,
                aktiv = name.isNotBlank() && !daten.sichert,
                kompakt = true,
            )
        }

        val bogenStichwort = bogen?.stichwort?.trim().orEmpty()
        if (bogenStichwort.isNotEmpty() && geltung != "schicht") {
            Hakenzeile(
                "Auch als eigenes Stichwort $bogenStichwort ins Verzeichnis",
                alsStichwort,
                { alsStichwort = it },
            )
        }

        Pillenreihe {
            Pille("Diese Schicht", an = geltung == "schicht", beiDruck = { geltung = "schicht" })
            if (landkreisId != null) {
                Pille(
                    "Nur ${raum.settings.landkreis ?: landkreisId}",
                    an = geltung == "kreis",
                    beiDruck = { geltung = "kreis" },
                )
            }
            Pille("Alle Runden", an = geltung == "alle", beiDruck = { geltung = "alle" })
        }

        val meldung = lokaleMeldung?.let { it to true }
            ?: daten.ordnungsmeldung?.let { it.text to it.gut }
        meldung?.let { (text, gut) ->
            Text(text, style = Schrift.Klein, color = if (gut) Farben.GruenHell else Farben.SignalHell)
        }

        // Die Ordnungen dieser Schicht — sie gelten für alle am Tisch, bis die Schicht
        // endet oder jemand sie wieder herausnimmt.
        if (raum.aaoVorlagen.isNotEmpty()) {
            Etikett("Ordnungen dieser Schicht")
            raum.aaoVorlagen.forEach { v ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp)
                        .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(v.name, style = Schrift.Klein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        SehrLeise(
                            "${v.empfohleneFahrzeuge} Fzg" +
                                if (v.empfohleneFaehigkeiten.isNotEmpty()) " · ${v.empfohleneFaehigkeiten.joinToString(", ")}" else "",
                            mono = true,
                        )
                    }
                    Knopf("Herausnehmen", { griffe.schichtordnungLoeschen(v.name) }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }

        // Die Verwaltung — zugeklappt, weil sie im eiligen Griff nichts zu suchen hat.
        daten.bestand?.let { stand ->
            val voll = stand.anzahl >= stand.grenze
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { zeigeVerwaltung = !zeigeVerwaltung },
            ) {
                Text(if (zeigeVerwaltung) "▾" else "▸", style = Schrift.Klein, color = Farben.TextLeise)
                Text(
                    "Meine gespeicherten Ordnungen",
                    style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                    color = Farben.Text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${stand.anzahl} von ${stand.grenze}",
                    style = Schrift.MonoKlein,
                    color = if (voll) Farben.AmberHell else Farben.TextSehrLeise,
                )
            }
            if (zeigeVerwaltung) {
                if (voll) {
                    Text(
                        "Das Fach ist voll — für eine neue Ordnung muss erst eine weichen." +
                            if (!stand.premium && stand.premiumGrenze > stand.grenze) {
                                " Mit Premium sind es ${stand.premiumGrenze}."
                            } else {
                                ""
                            },
                        style = Schrift.Klein,
                        color = Farben.AmberHell,
                    )
                }
                if (stand.vorlagen.isEmpty()) {
                    SehrLeise("Noch keine gespeicherte Ordnung. Was du hier sicherst, steht in jeder weiteren Runde bereit.")
                }
                stand.vorlagen.forEach { v ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp)
                            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(v.name, style = Schrift.Klein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            SehrLeise(
                                "${v.empfohleneFahrzeuge} Fzg · " + geltungsname(v, raum) +
                                    (v.stichwort?.let { " · Stichwort $it" } ?: ""),
                                mono = true,
                            )
                        }
                        Knopf(
                            if (daten.loeschtGerade == v.id) "…" else "Löschen",
                            { griffe.ordnungLoeschen(v) },
                            art = Knopfart.Gefahr,
                            aktiv = daten.loeschtGerade == null,
                            kompakt = true,
                        )
                    }
                }
            }
        }
    }

    if (vorlagenwahl) {
        val eintraege: List<Pair<String?, List<Pair<String, String>>>> = listOfNotNull(
            raum.aaoVorlagen.takeIf { it.isNotEmpty() }?.let { liste ->
                "Diese Schicht" to liste.map { v ->
                    "schicht:${v.name}" to "${v.name} (${v.empfohleneFahrzeuge} Fzg" +
                        (if (v.empfohleneFaehigkeiten.isNotEmpty()) ", ${v.empfohleneFaehigkeiten.size} Funktion(en)" else "") + ")"
                }
            },
            daten.ordnungen.takeIf { it.isNotEmpty() }?.let { liste ->
                "Meine gespeicherten Ordnungen" to liste.map { v ->
                    "dauer:${v.id}" to "${v.name} (${v.empfohleneFahrzeuge} Fzg) · ${if (v.landkreisId != null) "nur hier" else "überall"}"
                }
            },
        )
        Wahlblende(
            titel = "Gespeicherte Ordnung",
            gruppen = eintraege,
            aufschrift = { it.second },
            beiWahl = { (schluessel, text) ->
                vorlagenwahl = false
                gewaehlteVorlage = text
                if (schluessel.startsWith("schicht:")) {
                    raum.aaoVorlagen.firstOrNull { it.name == schluessel.removePrefix("schicht:") }?.let { v ->
                        beiAnzahl(v.empfohleneFahrzeuge)
                        beiFaehigkeiten(v.empfohleneFaehigkeiten)
                        beiGeaendert()
                    }
                } else {
                    daten.ordnungen.firstOrNull { it.id == schluessel.removePrefix("dauer:") }?.let { v ->
                        beiAnzahl(v.empfohleneFahrzeuge)
                        beiFaehigkeiten(v.empfohleneFaehigkeiten)
                        beiGeaendert()
                        // Die eigene Ordnung wandert in die Schicht — dann hat der zweite
                        // Disponent sie auch.
                        griffe.schichtordnung(v.name, v.empfohleneFahrzeuge, v.empfohleneFaehigkeiten)
                        beiUebernommen(v)
                    }
                }
            },
            beiSchliessen = { vorlagenwahl = false },
        )
    }
}

/** Wie weit eine gespeicherte Ordnung reicht — „überall" oder der Kreis. */
private fun geltungsname(v: AaoVorlagenzeile, raum: Raumzustand): String = when {
    v.landkreisId == null -> "überall"
    v.landkreisId == raum.settings.landkreisId -> raum.settings.landkreis ?: v.landkreisId
    else -> v.landkreisId
}

/** Die obere Reglergrenze — `MAX_FAHRZEUGE` im Ordnungsblock. */
private const val MAX_FAHRZEUGE_AAO = 12
