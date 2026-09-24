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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.ManvPatient
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * MANV, Sichtung, Führung — die Kästen der Fahrzeugseite bei großen Lagen.
 *
 * Übertragen aus `EinsatzleitungPanel.vue`, `ManvVersorgung.vue` und dem
 * Führungs-Reiter des `EinsatzleiterTablet.vue`. Am Handy ist das Tablet nicht
 * aushertbar — alles steht als Kästen im Einsatz-Teil, wie im Web-Handyzweig.
 *
 * <b>Die Regeln, die man beim Anfassen nicht brechen darf:</b>
 *  - Kategorien vergibt nie der Spieler; die Sichtung würfelt sie.
 *  - Solange `betroffeneUngefaehr` steht, sind Sichtung und Patientenliste
 *    bewusst leer — die Lagegröße kommt aus der Zwischenstufe.
 *  - Schwarz wartet für immer auf die Sichtung: Er zählt nicht als „offen",
 *    hat keinen Bedarf und kein Ziel — beide Wege sind bei ihm gesperrt.
 */

/**
 * Die Griffe der Führungs- und MANV-Kästen — ein Bündel statt zwölf einzelner
 * Parameter an der Fahrzeugseite.
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
)

/** Alle MANV- und Führungs-Kästen der Fahrzeugseite an einer Stelle. */
@Composable
fun ColumnScope.ManvBereich(
    einsatz: Einsatz?,
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    griffe: ManvGriffe,
) {
    if (einsatz == null) return

    EinsatzleitungKasten(
        einsatz = einsatz,
        meins = meins,
        beiUebernehmen = { zweig -> griffe.uebernehmen(einsatz.id, zweig) },
        beiAbgeben = { zweig -> griffe.abgeben(einsatz.id, zweig) },
    )

    val fuehrt = einsatz.einsatzleitung == meins.funkrufname
    val fuehrtRd = einsatz.einsatzleitungRd == meins.funkrufname

    if (einsatz.manv && (fuehrt || fuehrtRd || fuehrungsdienstRd(meins))) {
        ManvKasten(
            einsatz = einsatz,
            raum = raum,
            meins = meins,
            beiAnordnen = { art, groesse -> griffe.anordnen(einsatz.id, art, groesse) },
            beiAbbauen = { stelle, ab -> griffe.abbauen(einsatz.id, stelle, ab) },
            beiVerlegen = { p, stelle -> griffe.verlegen(einsatz.id, p, stelle) },
            beiTransportmittel = { p, f -> griffe.transportmittel(einsatz.id, p, f) },
            beiZielklinik = { p, k -> griffe.zielklinik(einsatz.id, p, k) },
            beiTransport = { p -> griffe.transport(einsatz.id, p) },
            beiVerstorbene = { griffe.verstorbene(einsatz.id) },
            beiTriage = { griffe.triage(einsatz.id) },
        )
    }

    if (fuehrt) {
        FuehrungKasten(
            einsatz = einsatz,
            raum = raum,
            zweig = "Gesamt",
            beiBilden = { name -> griffe.abschnittBilden(einsatz.id, name, "Gesamt") },
            beiZuteilen = { f, a -> griffe.abschnittZuteilen(einsatz.id, f, a, "Gesamt") },
            beiAuftrag = { _, _ -> },
        )
    }
    if (fuehrtRd) {
        FuehrungKasten(
            einsatz = einsatz,
            raum = raum,
            zweig = "Rettungsdienst",
            beiBilden = { name -> griffe.abschnittBilden(einsatz.id, name, "Rettungsdienst") },
            beiZuteilen = { f, a ->
                griffe.abschnittZuteilen(einsatz.id, f, a, "Rettungsdienst")
            },
            beiAuftrag = { auftrag, f -> griffe.auftrag(einsatz.id, auftrag, f) },
        )
    }
}

/** Ob die Lage groß genug für eine Einsatzleitung ist — die Web-Schwelle. */
fun lageGrossGenug(einsatz: Einsatz): Boolean =
    einsatz.prioritaet >= 3 || einsatz.manv || einsatz.alarmierteFahrzeuge.size >= 4

/** Ob dieses Fahrzeug die RD-Führung übernehmen darf — OrgL, ELRD oder LNA. */
private fun fuehrungsdienstRd(f: Rundenfahrzeug): Boolean =
    listOf("OrgL", "ELRD", "LNA").any { f.typ.contains(it, ignoreCase = true) }

/** Die Übernahme — ein Knopf und ein Hinweissatz, sonst nichts. */
@Composable
fun ColumnScope.EinsatzleitungKasten(
    einsatz: Einsatz,
    meins: Rundenfahrzeug,
    beiUebernehmen: (String) -> Unit,
    beiAbgeben: (String) -> Unit,
) {
    if (!lageGrossGenug(einsatz)) return
    val vorOrt = meins.status == 4 || meins.einsatzstelleErreicht
    val ichFuehre = einsatz.einsatzleitung == meins.funkrufname
    val ichFuehreRd = einsatz.einsatzleitungRd == meins.funkrufname

    Kasten {
        Ueberschrift("Einsatzleitung")
        when {
            ichFuehre -> {
                SehrLeise("Du führst diese Lage.")
                Knopf("Übergeben", { beiAbgeben("Gesamt") }, kompakt = true)
            }
            einsatz.einsatzleitung == null && vorOrt -> {
                SehrLeise("Die Lage ist groß genug für eine Führung vor Ort.")
                Knopf("Einsatzleitung übernehmen", { beiUebernehmen("Gesamt") }, kompakt = true)
            }
            einsatz.einsatzleitung != null ->
                SehrLeise("Einsatzleitung: ${einsatz.einsatzleitung}")
            else -> SehrLeise("Die Einsatzleitung wird vor Ort übernommen (Status 4).")
        }

        if (einsatz.manv && fuehrungsdienstRd(meins)) {
            when {
                ichFuehreRd -> {
                    SehrLeise("Du führst den Rettungsdienst.")
                    Knopf("EL RD übergeben", { beiAbgeben("Rettungsdienst") }, kompakt = true)
                }
                einsatz.einsatzleitungRd == null && vorOrt -> Knopf(
                    "Einsatzleitung Rettungsdienst übernehmen",
                    { beiUebernehmen("Rettungsdienst") },
                    kompakt = true,
                )
                einsatz.einsatzleitungRd != null ->
                    SehrLeise("EL RD: ${einsatz.einsatzleitungRd}")
            }
        }
    }
}

private fun kategorieFarbe(kategorie: String?): Color = when (kategorie) {
    "Rot" -> Color(0xFFE5484D)
    "Gelb" -> Farben.Amber
    "Gruen" -> Color(0xFF2F9E44)
    "Schwarz" -> Color(0xFF6B7684)
    else -> Farben.TextSehrLeise
}

private fun kategorieText(kategorie: String?): String = when (kategorie) {
    "Gruen" -> "Grün"
    null -> "—"
    else -> kategorie
}

/**
 * Die Versorgung — Stellen anordnen, Patienten verlegen, Transporte einleiten.
 *
 * Sichtbar für OrgL/ELRD/LNA vor Ort und für die Führung — wie im Web.
 */
@Composable
fun ColumnScope.ManvKasten(
    einsatz: Einsatz,
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    beiAnordnen: (String, Int) -> Unit,
    beiAbbauen: (String, Boolean) -> Unit,
    beiVerlegen: (String, String?) -> Unit,
    beiTransportmittel: (String, String?) -> Unit,
    beiZielklinik: (String, String?) -> Unit,
    beiTransport: (String) -> Unit,
    beiVerstorbene: () -> Unit,
    beiTriage: () -> Unit,
) {
    if (!einsatz.manv) return

    // „Sichtung abgeschlossen" braucht den Vorbehalt: Ohne Aufgabenplan gibt es
    // gar keine Sichtungsaufgabe — dann gilt die vor Ort gemeldete Sichtung.
    val sichtungsaufgabe = einsatz.aufgaben.firstOrNull { it.name == "Sichtung" }
    val sichtungFertig = sichtungsaufgabe?.fertig ?: true

    Kasten {
        Ueberschrift("Massenanfall")

        // Die Zwischenstufe — solange sie steht, gibt es keine Patienten.
        einsatz.betroffeneUngefaehr?.let {
            SehrLeise("ungefähr $it Betroffene – Vorsichtung läuft")
        }

        // Sichtungsübersicht: alle Patienten, nicht nur wartende. Schwarz
        // steht hier immer, auch als 0.
        if (einsatz.manvPatienten.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                listOf("Rot", "Gelb", "Gruen", "Schwarz").forEach { k ->
                    val zahl = einsatz.manvPatienten.count { it.kategorie == k }
                    Marke("$zahl ${kategorieText(k)}", farbe = kategorieFarbe(k))
                }
            }
        }

        // ELW-2-Triagekoordination — beschleunigt die Sichtung.
        if (meins.typ.contains("ELW 2") && meins.status == 4 && sichtungsaufgabe?.fertig == false) {
            Knopf("Notärzte koordinieren", beiTriage, kompakt = true)
        }

        // Anordnen — die Ablage ist ab vier Stellen gesperrt, wie im Web.
        val stellen = einsatz.versorgungsstellen
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                "Verletztenablage einrichten",
                { beiAnordnen("Verletztenablage", 0) },
                aktiv = stellen.size < 4,
                kompakt = true,
            )
            Knopf("BHP 25 aufbauen", { beiAnordnen("Behandlungsplatz", 25) }, kompakt = true)
            Knopf("BHP 50 aufbauen", { beiAnordnen("Behandlungsplatz", 50) }, kompakt = true)
        }

        stellen.forEach { stelle ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stelle.name, style = Schrift.Klein, color = Farben.Text)
                    SehrLeise(
                        when {
                            stelle.imAbbau -> "im Abbau · ${(stelle.aufbaufortschritt * 100).toInt()} %"
                            stelle.einsatzbereit ->
                                "steht · ${stelle.patienten.size}/${stelle.kapazitaet}"
                            else -> "im Aufbau · ${(stelle.aufbaufortschritt * 100).toInt()} %"
                        },
                    )
                }
                Knopf(
                    if (stelle.imAbbau) "Abbau abbrechen" else "Abbauen",
                    { beiAbbauen(stelle.name, !stelle.imAbbau) },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        }

        // Die Verstorbenen — der Grundsatz: Erst wenn die Lebenden versorgt
        // sind. Der Server prüft; der Knopf sagt nur, was er tun würde.
        val verstorbene = einsatz.manvPatienten.count { it.kategorie == "Schwarz" }
        if (verstorbene > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SehrLeise("$verstorbene Verstorbene", modifier = Modifier.weight(1f))
                Knopf("An die Polizei übergeben", beiVerstorbene, art = Knopfart.Leise, kompakt = true)
            }
        }

        // Gesichtete Verletzte — Ort, Ziel, Transport. Beides bei Schwarz
        // gesperrt: kein Bedarf, kein Ziel, mit Absicht.
        val gesichtet = einsatz.manvPatienten.filter { it.kategorie != null }
        if (gesichtet.isNotEmpty()) {
            Ueberschrift("Gesichtete Verletzte")
            gesichtet.forEachIndexed { i, p ->
                Patientenzeile(
                    nummer = i + 1,
                    patient = p,
                    einsatz = einsatz,
                    raum = raum,
                    sichtungFertig = sichtungFertig,
                    beiVerlegen = { beiVerlegen(p.id, it) },
                    beiTransportmittel = { beiTransportmittel(p.id, it) },
                    beiZielklinik = { beiZielklinik(p.id, it) },
                    beiTransport = { beiTransport(p.id) },
                )
            }
        }
    }
}

@Composable
private fun Patientenzeile(
    nummer: Int,
    patient: ManvPatient,
    einsatz: Einsatz,
    raum: Raumzustand,
    sichtungFertig: Boolean,
    beiVerlegen: (String?) -> Unit,
    beiTransportmittel: (String?) -> Unit,
    beiZielklinik: (String?) -> Unit,
    beiTransport: () -> Unit,
) {
    var ortwahl by remember { mutableStateOf(false) }
    var klinikwahl by remember { mutableStateOf(false) }
    val schwarz = patient.kategorie == "Schwarz"
    val transportfahrzeug = raum.vehicles.firstOrNull { it.id == patient.transportVehicleId }
    val klinik = raum.kliniken.firstOrNull { it.id == patient.zielklinikId }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().padding(vertical = Abstand.Haar),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Marke(kategorieText(patient.kategorie), farbe = kategorieFarbe(patient.kategorie))
            Text("P-$nummer", style = Schrift.MonoKlein, color = Farben.Text)
            patient.bedarf?.let { SehrLeise(it) }
            SehrLeise(
                when (patient.status) {
                    "ImTransport" -> "im Transport"
                    "Uebergeben" -> "übergeben"
                    else -> ""
                },
            )
        }

        if (!schwarz && patient.status != "Uebergeben") {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf(
                    aufschrift = patient.stelle
                        ?: transportfahrzeug?.funkrufname
                        ?: "— im Gelände —",
                    beiDruck = { ortwahl = true },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
                Knopf(
                    aufschrift = klinik?.name ?: "Zielklinik …",
                    beiDruck = { klinikwahl = true },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
                if (sichtungFertig &&
                    patient.transportVehicleId != null &&
                    patient.zielklinikId != null &&
                    patient.status == "WartetAufTransport"
                ) {
                    Knopf("Transport", beiTransport, kompakt = true)
                }
            }
        }
    }

    if (ortwahl) {
        // Eine Liste, zwei Gruppen — Versorgungsstellen und Transportmittel,
        // wie das eine <select> des Web.
        val stellen = einsatz.versorgungsstellen.filter { it.einsatzbereit }.map { "st:" + it.name }
        val fahrzeuge = raum.vehicles
            .filter { it.einsatzId == einsatz.id && it.einsatzstelleErreicht }
            .map { "fz:" + it.id }
        Wahlblende(
            titel = "Wohin mit P-$nummer?",
            gruppen = listOf(
                null to listOf("—"),
                "Versorgungsstelle" to stellen,
                "Transportmittel" to fahrzeuge,
            ),
            aufschrift = { eintrag ->
                when {
                    eintrag == "—" -> "— im Gelände —"
                    eintrag.startsWith("st:") -> eintrag.removePrefix("st:")
                    else -> raum.vehicles.firstOrNull { it.id == eintrag.removePrefix("fz:") }
                        ?.funkrufname ?: eintrag
                }
            },
            beiWahl = { eintrag ->
                when {
                    eintrag == "—" -> {
                        beiVerlegen(null)
                        beiTransportmittel(null)
                    }
                    eintrag.startsWith("st:") -> beiVerlegen(eintrag.removePrefix("st:"))
                    else -> beiTransportmittel(eintrag.removePrefix("fz:"))
                }
                ortwahl = false
            },
            beiSchliessen = { ortwahl = false },
        )
    }

    if (klinikwahl) {
        Wahlblende(
            titel = "Zielklinik für P-$nummer",
            gruppen = listOf(null to (listOf<String?>(null) + raum.kliniken.map { it.id })),
            aufschrift = { id ->
                id?.let { k -> raum.kliniken.firstOrNull { it.id == k }?.name ?: k }
                    ?: "— keine —"
            },
            gewaehlt = patient.zielklinikId,
            beiWahl = {
                beiZielklinik(it)
                klinikwahl = false
            },
            beiSchliessen = { klinikwahl = false },
            suchbar = raum.kliniken.size > 8,
        )
    }
}

/**
 * Der Führungs-Kasten — Abschnitte bilden und zuteilen, Aufträge delegieren.
 *
 * Nur für die Führung sichtbar; `zweig` entscheidet, welcher Satz Abschnitte
 * gemeint ist. Höchstens vier je Zweig, Namen bis 40 Zeichen — der Server
 * prüft, die Kästen sagen es vorher.
 */
@Composable
fun ColumnScope.FuehrungKasten(
    einsatz: Einsatz,
    raum: Raumzustand,
    zweig: String,
    beiBilden: (String) -> Unit,
    beiZuteilen: (String, String?) -> Unit,
    beiAuftrag: (String, String?) -> Unit,
) {
    val abschnitte = if (zweig == "Rettungsdienst") einsatz.abschnitteRd else einsatz.abschnitte
    var neuerName by remember { mutableStateOf("") }
    var fahrzeugwahl by remember { mutableStateOf<String?>(null) }

    Kasten {
        Ueberschrift(
            if (zweig == "Rettungsdienst") "Abschnitte Rettungsdienst" else "Abschnitte",
        )
        SehrLeise("${abschnitte.size}/4")

        abschnitte.forEach { a ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(a.name, style = Schrift.Klein, color = Farben.Text)
                SehrLeise(
                    if (a.funkrufnamen.isEmpty()) "keine Fahrzeuge"
                    else a.funkrufnamen.joinToString(", "),
                )
                Knopf(
                    "Fahrzeug zuordnen",
                    { fahrzeugwahl = a.name },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        }

        if (abschnitte.size < 4) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.Bottom,
            ) {
                Feld(
                    wert = neuerName,
                    beiAenderung = { neuerName = it.take(40) },
                    platzhalter = "Abschnitt ${abschnitte.size + 1} benennen",
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Anlegen",
                    {
                        beiBilden(neuerName.trim())
                        neuerName = ""
                    },
                    aktiv = neuerName.isNotBlank(),
                    kompakt = true,
                )
            }
        }

        // Delegation — nur am RD-Zweig eines MANV, wie im Web.
        if (zweig == "Rettungsdienst" && einsatz.manv) {
            Ueberschrift("Aufträge übertragen")
            listOf("Vorsichtung", "Sichtung", "Transportorganisation").forEach { auftrag ->
                var wahl by remember(auftrag) { mutableStateOf(false) }
                // Der Server liefert Funkrufnamen, die Auswahl braucht Ids —
                // ohne die Übersetzung stünde hier dauerhaft „— selbst —".
                val beauftragt = einsatz.manvauftraege[auftrag]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SehrLeise(auftrag, modifier = Modifier.weight(1f))
                    Knopf(
                        beauftragt ?: "— selbst —",
                        { wahl = true },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
                if (wahl) {
                    val passende = raum.vehicles.filter { f ->
                        f.einsatzId == einsatz.id && when (auftrag) {
                            "Vorsichtung" -> f.faehigkeiten.contains("Vorsichtung")
                            "Sichtung" -> f.faehigkeiten.contains("Sichtung")
                            else -> fuehrungsdienstRd(f)
                        }
                    }
                    Wahlblende(
                        titel = auftrag,
                        gruppen = listOf(null to (listOf<String?>(null) + passende.map { it.id })),
                        aufschrift = { id ->
                            id?.let { v -> raum.vehicles.firstOrNull { it.id == v }?.funkrufname }
                                ?: "— selbst —"
                        },
                        beiWahl = {
                            beiAuftrag(auftrag, it)
                            wahl = false
                        },
                        beiSchliessen = { wahl = false },
                    )
                }
            }
        }
    }

    fahrzeugwahl?.let { abschnitt ->
        val zuordenbar = raum.vehicles.filter { f ->
            f.einsatzId == einsatz.id &&
                abschnitte.firstOrNull { it.name == abschnitt }
                    ?.funkrufnamen?.contains(f.funkrufname) != true
        }
        Wahlblende(
            titel = "Fahrzeug für $abschnitt",
            gruppen = listOf(null to zuordenbar.map { it.id }),
            aufschrift = { id ->
                raum.vehicles.firstOrNull { it.id == id }?.funkrufname ?: id
            },
            beiWahl = { id ->
                beiZuteilen(id, abschnitt)
                fahrzeugwahl = null
            },
            beiSchliessen = { fahrzeugwahl = null },
        )
    }
}
