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
 * Übertragen aus `ManvVersorgung.vue`. Sie steht im Fahrzeug für die medizinische
 * Führung vor Ort und im Einsatz-Tablet (`Einsatztablet.kt`) für die, die führt;
 * die Übernahme selbst ist `EinsatzleitungPanel` in `Fahrzeugkasten.kt`.
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
    /**
     * Wer einen Patienten behandelt — Patient, Fahrzeug (`null` = niemand), Notarztplatz.
     * Fehlt der Griff, fehlen die beiden Felder (v6, `PatientBehandlerZuweisen`).
     */
    beiBehandler: ((String, String?, Boolean) -> Unit)? = null,
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
                    beiBehandler = beiBehandler?.let { b -> { f: String?, na: Boolean -> b(p.id, f, na) } },
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
    beiBehandler: ((String?, Boolean) -> Unit)? = null,
) {
    var ortwahl by remember { mutableStateOf(false) }
    // Welcher Behandlerplatz gerade gewählt wird: `false` Rettungsmittel, `true` Notarzt.
    var behandlerwahl by remember { mutableStateOf<Boolean?>(null) }
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
            }
            // Wer ihn behandelt: ein Rettungsmittel, und ein Notarzt, wenn einer da ist.
            if (beiBehandler != null) {
                val (rd, na) = behandelnde(einsatz, raum)
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        aufschrift = patient.behandler ?: "— unbehandelt —",
                        beiDruck = { behandlerwahl = false },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                    if (na.isNotEmpty() || patient.notarztVehicleId != null) {
                        Knopf(
                            aufschrift = patient.notarzt?.let { "NA $it" } ?: "— kein NA —",
                            beiDruck = { behandlerwahl = true },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                }
                if (rd.isEmpty() && na.isEmpty()) SehrLeise("Kein Rettungsmittel in Status 4 oder 7 an der Lage.")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
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

    behandlerwahl?.let { notarzt ->
        val (rd, na) = behandelnde(einsatz, raum)
        val liste = if (notarzt) na else rd
        Wahlblende(
            titel = if (notarzt) "Notarzt für P-$nummer" else "Behandlung von P-$nummer",
            gruppen = listOf(null to (listOf<String?>(null) + liste.map { it.id })),
            aufschrift = { id ->
                id?.let { f -> raum.vehicles.firstOrNull { it.id == f }?.funkrufname ?: f }
                    ?: if (notarzt) "— kein NA —" else "— unbehandelt —"
            },
            gewaehlt = if (notarzt) patient.notarztVehicleId else patient.behandlerVehicleId,
            beiWahl = {
                beiBehandler?.invoke(it, notarzt)
                behandlerwahl = null
            },
            beiSchliessen = { behandlerwahl = null },
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
 * Wer an der Stelle behandeln kann: die Rettungsmittel dieser Lage in Status 4 oder 7,
 * getrennt nach Rettungsmittel- und Notarztplatz — je Patient einer von beiden
 * (Server: `Patientenbehandlung`). Die Einsatzleitung teilt beim Massenanfall zu.
 */
private fun behandelnde(einsatz: Einsatz, raum: Raumzustand): Pair<List<Rundenfahrzeug>, List<Rundenfahrzeug>> {
    val vorOrt = raum.vehicles.filter {
        it.einsatzId == einsatz.id && it.organisation == "Rettungsdienst" && (it.status == 4 || it.status == 7)
    }
    val istNa = { f: Rundenfahrzeug -> f.faehigkeiten.any { it.equals("Notarzt", ignoreCase = true) } }
    return vorOrt.filterNot(istNa) to vorOrt.filter(istNa)
}
