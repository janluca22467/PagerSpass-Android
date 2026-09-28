package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Klinik
import de.pagerspass.pagerspass.netz.ManvPatient
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * MANV, Sichtung, Führung — die Kästen der Fahrzeugseite bei großen Lagen.
 *
 * Übertragen aus `EinsatzleitungPanel.vue`, `ManvVersorgung.vue` und dem
 * `EinsatzleiterTablet.vue` (siehe `FahrzeugTablet.kt`): <b>Vor der Übernahme der
 * eine Knopf, danach das Tablet.</b> Die Versorgung steht im Fahrzeugbereich nur
 * für die medizinische Führung, die nicht selbst führt — wer führt, findet sie im
 * Tablet, und zwei Wege zu derselben Arbeit wären einer zu viel.
 *
 * <b>Die Regeln, die man beim Anfassen nicht brechen darf:</b>
 *  - Kategorien vergibt nie der Spieler; die Sichtung würfelt sie.
 *  - Solange `betroffeneUngefaehr` steht, sind Sichtung und Patientenliste
 *    bewusst leer — die Lagegröße kommt aus der Zwischenstufe.
 *  - Schwarz wartet für immer auf die Sichtung: Er zählt nicht als „offen",
 *    hat keinen Bedarf und kein Ziel — beide Wege sind bei ihm gesperrt.
 */

/**
 * Die Griffe der Führungs- und MANV-Kästen samt Tablet — ein Bündel statt
 * zwanzig einzelner Parameter an der Fahrzeugseite. `zweig` ist immer
 * `Gesamt` oder `Rettungsdienst`.
 */
class ManvGriffe(
    val uebernehmen: (String, String) -> Unit = { _, _ -> },
    val abgeben: (String, String) -> Unit = { _, _ -> },
    val abschnittBilden: (String, String, String) -> Unit = { _, _, _ -> },
    val abschnittZuteilen: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    val auftrag: (String, String, String?) -> Unit = { _, _, _ -> },
    val anordnen: (String, String, Int) -> Unit = { _, _, _ -> },
    val abbauen: (String, String, Boolean) -> Unit = { _, _, _ -> },
    val verlegen: (String, String, String?) -> Unit = { _, _, _ -> },
    val transportmittel: (String, String, String?) -> Unit = { _, _, _ -> },
    val zielklinik: (String, String, String?) -> Unit = { _, _, _ -> },
    val transport: (String, String) -> Unit = { _, _ -> },
    val verstorbene: (String) -> Unit = {},
    val triage: (String) -> Unit = {},
    // --- Fahrzeug (Lücke A7): Umbenennen und Weiterreichen, dazu die Flächen und der Einsatzfunk.
    /** Einsatz, bisheriger Name, neuer Name, Zweig. */
    val abschnittUmbenennen: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    /** Einsatz, Zweig — die gesammelten Nachforderungen an die Leitstelle. */
    val nachforderungenWeiterreichen: (String, String) -> Unit = { _, _ -> },
    val bereitstellungsraumFestlegen: (String) -> Unit = {},
    /** Einsatz, Fahrzeug, halten. */
    val bereitstellungSetzen: (String, String, Boolean) -> Unit = { _, _, _ -> },
    val landeplatzFestlegen: (String) -> Unit = {},
    val landeplatzAusleuchten: (String) -> Unit = {},
    /** Einsatz, Name der neuen DMO-Gruppe. */
    val einsatzfunkgruppeOeffnen: (String, String) -> Unit = { _, _ -> },
    val einsatzfunkgruppeSchliessen: (String) -> Unit = {},
    /** Fahrzeug, Gruppe (`null` = zurück auf die Stammgruppe). */
    val funkgruppeZuweisen: (String, String?) -> Unit = { _, _ -> },
)

/** Alle Führungs- und MANV-Kästen der Fahrzeugseite an einer Stelle. */
@Composable
fun ColumnScope.ManvBereich(
    einsatz: Einsatz?,
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    griffe: ManvGriffe,
    kennung: (Rundenfahrzeug) -> String,
    beiNachfordern: (String) -> Unit,
) {
    if (einsatz == null) return
    EinsatzleitungPanel(einsatz, meins) { zweig -> griffe.uebernehmen(einsatz.id, zweig) }
    ManvVersorgung(raum = raum, einsatz = einsatz, meins = meins, imTablet = false, griffe = griffe)
    EinsatzleiterTablet(raum, meins, einsatz, kennung, griffe, beiNachfordern)
}

/** Ob die Lage groß genug für eine Einsatzleitung ist — die Web-Schwelle. */
fun lageGrossGenug(einsatz: Einsatz): Boolean =
    einsatz.prioritaet >= 3 || einsatz.manv || einsatz.alarmierteFahrzeuge.size >= 4

private val RANG = mapOf("Rot" to 0, "Gelb" to 1, "Gruen" to 2, "Schwarz" to 3)

// Die Ausweichleiter (`manvFach`) und die Namen der Fachabteilungen (`versorgungText`)
// stehen in `Leitstellenhilfen.kt` — dieselbe Leiter wie am Leitstellentisch.

/** Was trotz „Transport" niemanden in eine Klinik fährt. */
private val KEIN_PATIENTENTRANSPORT = listOf("betreuung", "ortung")

/**
 * Die Ordnung des Raumes bei einem Massenanfall — `ManvVersorgung.vue`:
 * Verletztenablagen und Behandlungsplatz anordnen, die gesichteten Verletzten
 * hineinlegen, Transportmittel und Zielklinik zuweisen, abtransportieren, und zuletzt
 * die Verstorbenen an die Polizei übergeben.
 */
@Composable
fun ColumnScope.ManvVersorgung(
    raum: Raumzustand,
    einsatz: Einsatz,
    meins: Rundenfahrzeug,
    imTablet: Boolean,
    griffe: ManvGriffe,
) {
    if (!einsatz.manv) return
    val fuehrtSelbst = meins.funkrufname.isNotBlank() &&
        (einsatz.einsatzleitung == meins.funkrufname || einsatz.einsatzleitungRd == meins.funkrufname)
    val darfOrdnen = when {
        fuehrtSelbst -> imTablet
        imTablet -> false
        else -> istMedizinischeFuehrung(meins) && meins.status == 4 && meins.einsatzId == einsatz.id
    }
    val sichtungsaufgabe = einsatz.aufgaben.firstOrNull { it.name == "Vorsichtung" && !it.fertig }
        ?: einsatz.aufgaben.firstOrNull { it.name == "Sichtung" && !it.fertig }
    val notaerzte = raum.vehicles.filter {
        it.einsatzId == einsatz.id && it.status == 4 && it.einsatzstelleErreicht && it.faehigkeiten.contains("Notarzt")
    }
    val darfTriage = !imTablet && meins.typ.lowercase() == "elw 2" && meins.status == 4 &&
        meins.einsatzstelleErreicht && meins.einsatzId == einsatz.id && sichtungsaufgabe != null
    if (!darfOrdnen && !darfTriage) return

    val inhalt: @Composable ColumnScope.() -> Unit = {
        if (darfTriage) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Etikett("ELW-2-Triagekoordination")
                    Leise("${notaerzte.size} Notarztfahrzeug(e) vor Ort · gemeinsam auf die Sichtung setzen")
                }
                Knopf("Notärzte koordinieren", { griffe.triage(einsatz.id) }, aktiv = notaerzte.isNotEmpty(), kompakt = true)
            }
        }
        if (darfOrdnen) Versorgungsordnung(raum, einsatz, griffe)
    }

    if (imTablet) {
        inhalt()
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
            content = inhalt,
        )
    }
}

@Composable
private fun ColumnScope.Versorgungsordnung(raum: Raumzustand, einsatz: Einsatz, griffe: ManvGriffe) {
    val stellen = einsatz.versorgungsstellen
    val hatBhp = stellen.any { it.art == "Behandlungsplatz" }
    val offeneStellen = stellen.filter { it.einsatzbereit && !it.imAbbau && (it.kapazitaet <= 0 || it.patienten.size < it.kapazitaet) }
    val sichtungFertig = einsatz.aufgaben.firstOrNull { it.name == "Sichtung" }?.fertig ?: true

    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Knopf(
            "Verletztenablage einrichten",
            { griffe.anordnen(einsatz.id, "Verletztenablage", 0) },
            aktiv = stellen.count { it.art == "Verletztenablage" } < 4,
            kompakt = true,
        )
        Knopf("BHP 25 aufbauen", { griffe.anordnen(einsatz.id, "Behandlungsplatz", 25) }, aktiv = !hatBhp, kompakt = true)
        Knopf("BHP 50 aufbauen", { griffe.anordnen(einsatz.id, "Behandlungsplatz", 50) }, aktiv = !hatBhp, kompakt = true)
    }

    if (stellen.isEmpty()) {
        SehrLeise("Noch keine Versorgungsstelle angeordnet. Der Aufbau kostet Kräfte an der Einsatzstelle.")
    }
    stellen.forEach { s ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(s.name, style = Schrift.MonoKlein, color = Farben.Text)
                val belegung = if (s.kapazitaet > 0) "${s.patienten.size}/${s.kapazitaet}" else "${s.patienten.size}"
                SehrLeise(
                    when {
                        s.imAbbau -> "im Abbau · ${(s.aufbaufortschritt * 100).toInt()} %"
                        s.einsatzbereit -> "steht"
                        else -> "im Aufbau · ${(s.aufbaufortschritt * 100).toInt()} %"
                    } + " · $belegung ${if (s.patienten.size == 1) "Patient" else "Patienten"}",
                )
            }
            Knopf(
                if (s.imAbbau) "Abbau abbrechen" else "Abbauen",
                { griffe.abbauen(einsatz.id, s.name, !s.imAbbau) },
                art = Knopfart.Leise,
                kompakt = true,
            )
        }
    }

    einsatz.betroffeneUngefaehr?.let {
        SehrLeise("Erkundung: ungefähr $it Betroffene. Die Kategorien stehen nach der Vorsichtung.")
    }

    // Das Lagebild in einer Zeile — alle, nicht nur die Wartenden. Schwarz steht immer dabei.
    if (einsatz.manvPatienten.any { it.kategorie != null }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            listOf("Rot", "Gelb", "Gruen", "Schwarz").forEach { k ->
                Marke("${kategorieWort(k)} ${einsatz.manvPatienten.count { it.kategorie == k }}", farbe = kategorieTon(k))
            }
        }
    }

    // Die Verstorbenen — zuletzt, wenn kein Lebender mehr an der Stelle liegt.
    val verstorbene = einsatz.manvPatienten.filter { it.kategorie == "Schwarz" && it.status != "Uebergeben" }
    if (verstorbene.isNotEmpty()) {
        val lebendeVersorgt = einsatz.manvPatienten.all { it.kategorie == "Schwarz" || it.status == "ImTransport" || it.status == "Uebergeben" }
        val polizei = raum.settings.organisationen.isEmpty() || raum.settings.organisationen.contains("Polizei")
        val streife = raum.vehicles.firstOrNull {
            it.einsatzId == einsatz.id && it.organisation == "Polizei" && it.status == 4 && it.einsatzstelleErreicht
        }
        val grund = when {
            !lebendeVersorgt -> "Erst wenn kein Verletzter mehr hier liegt — die Verstorbenen gehen zuletzt."
            streife == null && polizei -> "Dafür muss eine Streife an der Einsatzstelle stehen."
            else -> null
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Etikett("${verstorbene.size} ${if (verstorbene.size == 1) "Verstorbener" else "Verstorbene"} an der Einsatzstelle")
                Leise(
                    grund ?: streife?.let { "Übergabe an ${it.funkrufname}" }
                        ?: "Diese Runde fährt ohne Polizei — die Einsatzleitung regelt es selbst.",
                )
            }
            Knopf(
                if (polizei) "An die Polizei übergeben" else "Übergabe veranlassen",
                { griffe.verstorbene(einsatz.id) },
                aktiv = grund == null,
                kompakt = true,
            )
        }
    }

    val patienten = einsatz.manvPatienten
        .filter { it.kategorie != null && (it.status == "WartetAufSichtung" || it.status == "WartetAufTransport") }
        .sortedWith(compareBy<ManvPatient> { RANG[it.kategorie] ?: 9 }.thenBy { it.id })
    if (patienten.isNotEmpty()) {
        Etikett("Gesichtete Verletzte")
        patienten.forEach { p ->
            Patientenzeile(p, einsatz, raum, offeneStellen.map { it.name }, sichtungFertig, griffe)
        }
    }
}

@Composable
private fun Patientenzeile(
    patient: ManvPatient,
    einsatz: Einsatz,
    raum: Raumzustand,
    offeneStellen: List<String>,
    sichtungFertig: Boolean,
    griffe: ManvGriffe,
) {
    var ortwahl by remember { mutableStateOf(false) }
    var klinikwahl by remember { mutableStateOf(false) }
    val schwarz = patient.kategorie == "Schwarz"
    val transportfahrzeug = raum.vehicles.firstOrNull { it.id == patient.transportVehicleId }
    val klinik = raum.kliniken.firstOrNull { it.id == patient.zielklinikId }

    // Die Transportmittel an dieser Stelle — plus das, auf das er schon gebucht ist.
    val transportmittel = raum.vehicles.filter { v ->
        v.einsatzId == einsatz.id && v.organisation == "Rettungsdienst" && v.faehigkeiten.contains("Transport") &&
            v.faehigkeiten.none { it.lowercase() in KEIN_PATIENTENTRANSPORT } && v.status == 4
    }.let { liste ->
        if (transportfahrzeug != null && liste.none { it.id == transportfahrzeug.id }) liste + transportfahrzeug else liste
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth().padding(vertical = Abstand.Haar)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text(patient.id, style = Schrift.MonoKlein, color = Farben.Text)
            patient.kategorie?.let { Marke(kategorieWort(it), farbe = kategorieTon(it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                patient.stelle ?: transportfahrzeug?.funkrufname ?: "— im Gelände —",
                { ortwahl = true },
                art = Knopfart.Leise,
                aktiv = !schwarz,
                kompakt = true,
            )
            Knopf(
                klinik?.name ?: "— Klinik offen —",
                { klinikwahl = true },
                art = Knopfart.Leise,
                aktiv = !schwarz,
                kompakt = true,
            )
            if (sichtungFertig && patient.transportVehicleId != null && patient.zielklinikId != null &&
                patient.status == "WartetAufTransport"
            ) {
                Knopf("Transport", { griffe.transport(einsatz.id, patient.id) }, art = Knopfart.Alarm, kompakt = true)
            }
        }
    }

    if (ortwahl) {
        // Eine Liste, zwei Gruppen — wie das eine Auswahlfeld des Webs. Ein Wechsel sind
        // zwei Kommandos, und zwar erst setzen, dann lösen: scheitert das Setzen, bleibt
        // der Patient, wo er war.
        val stellen = (listOfNotNull(patient.stelle) + offeneStellen.filter { it != patient.stelle }).map { "st:$it" }
        Wahlblende(
            titel = "Ort von Patient ${patient.id}",
            gruppen = listOf(
                null to listOf("—"),
                "Versorgungsstelle" to stellen,
                "Transportmittel" to transportmittel.map { "fz:" + it.id },
            ),
            aufschrift = { eintrag ->
                when {
                    eintrag == "—" -> "— im Gelände —"
                    eintrag.startsWith("st:") -> eintrag.removePrefix("st:")
                    else -> raum.vehicles.firstOrNull { it.id == eintrag.removePrefix("fz:") }?.let { v ->
                        v.funkrufname + if (v.status != 4) " · Status ${v.status}" else ""
                    } ?: eintrag
                }
            },
            beiWahl = { eintrag ->
                when {
                    eintrag.startsWith("fz:") -> {
                        griffe.transportmittel(einsatz.id, patient.id, eintrag.removePrefix("fz:"))
                        if (patient.stelle != null) griffe.verlegen(einsatz.id, patient.id, null)
                    }
                    else -> {
                        val stelle = if (eintrag == "—") null else eintrag.removePrefix("st:")
                        griffe.verlegen(einsatz.id, patient.id, stelle)
                        if (patient.transportVehicleId != null) griffe.transportmittel(einsatz.id, patient.id, null)
                    }
                }
                ortwahl = false
            },
            beiSchliessen = { ortwahl = false },
        )
    }

    if (klinikwahl) {
        val kategorie = patient.kategorie
        // Das Umland steht nur offen, solange kein bodengebundenes Fahrzeug gebucht ist.
        val umlandOffen = transportfahrzeug?.istLuftfahrzeug != false
        val wahl = if (kategorie == null || kategorie == "Schwarz") {
            emptyList()
        } else {
            val lat = einsatz.lat
            val lon = einsatz.lon
            raum.kliniken
                .filter { (!it.imUmland || umlandOffen) && manvFach(it, kategorie) != null }
                .let { l -> if (lat != null && lon != null) l.sortedBy { distanzMeter(lat, lon, it.lat, it.lon) } else l }
        }
        Wahlblende(
            titel = "Zielklinik für Patient ${patient.id}",
            gruppen = listOf(null to (listOf<Klinik?>(null) + wahl)),
            aufschrift = { k ->
                if (k == null) {
                    "— Klinik offen —"
                } else {
                    val teile = mutableListOf(k.name)
                    if (k.imUmland) teile += "Umland" + (k.entfernungKm?.let { " ${it.toInt()} km" } ?: "")
                    kategorie?.let { manvFach(k, it) }?.let { fach ->
                        val betten = k.freieBetten[fach]
                        teile += versorgungText(fach) + (betten?.let { ", $it frei" } ?: "")
                    }
                    teile.joinToString(" · ")
                }
            },
            gewaehlt = klinik,
            beiWahl = { k ->
                griffe.zielklinik(einsatz.id, patient.id, k?.id)
                klinikwahl = false
            },
            beiSchliessen = { klinikwahl = false },
            suchbar = wahl.size > 8,
        )
    }
}
