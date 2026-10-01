package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Fwauftrag
import de.pagerspass.pagerspass.netz.Fwlage
import de.pagerspass.pagerspass.netz.Fwtrupp
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlin.math.roundToInt

/**
 * FwSim — die Einsatzstelle der Feuerwehr, übertragen aus `FeuerwehrFenster.vue`
 * und `FwsimBogen.vue` (Web 5.0.0.26). Für jedes Fahrzeug andere Aufträge, für alle
 * dieselbe Lage. Er zeigt sich nur einer Feuerwehrbesatzung mit Status 4 an einer
 * Lage, für die der Server eine Simulation führt.
 *
 * <b>Das Lagebild ist gezeichnet</b> wie in `FwLagebild.vue` — Haus im Schnitt,
 * Fahrzeug von der Seite, Fläche mit Wind (siehe `FwLagebild.kt`). Angetippt wird
 * der Bereich in der Zeichnung; was man über ihn weiß, steht darunter.
 */

private val ART = mapOf(
    "Gebaeudebrand" to "Gebäudebrand",
    "Fahrzeugbrand" to "Fahrzeugbrand",
    "Vegetationsbrand" to "Vegetationsbrand",
    "Verkehrsunfall" to "Technische Rettung",
    "Gefahrgut" to "Gefahrgut",
    "Hilfeleistung" to "Hilfeleistung",
)

private val VERSORGUNG = mapOf(
    "Tank" to "nur Tankwasser",
    "Hydrant" to "Hydrant",
    "Pendelverkehr" to "Pendelverkehr",
    "LangeWegstrecke" to "Förderstrecke",
)

private val TRUPPSTATUS = mapOf(
    "Bereit" to "am Fahrzeug",
    "Ausruesten" to "rüstet aus",
    "Vorgehen" to "geht vor",
    "Einsatz" to "im Einsatz",
    "Rueckzug" to "Rückzug",
    "Notfall" to "MAYDAY",
    "Sicherheit" to "Sicherheitstrupp",
    "Erholung" to "Pause",
    "Verletzt" to "verletzt",
)

private val TAETIGKEIT = mapOf(
    "Brandbekaempfung" to "Brandbekämpfung",
    "Suche" to "Menschensuche",
    "Riegelstellung" to "Riegelstellung",
    "Nachloeschen" to "Nachkontrolle",
    "Leckabdichtung" to "Leck abdichten",
    "Rettung" to "holt Kameraden",
)

/** Die Truppaufträge, die einen Bereich brauchen, in der Reihenfolge der Arbeit. */
private val BEREICHSAUFTRAEGE = listOf("loeschen", "suchen", "riegel", "nachloeschen", "leck")
private val TRUPPAUFTRAEGE = listOf("ausruesten", "retten", "rueckzug", "flaschenwechsel", "sicherheitstrupp", "st_einsetzen")
private val BESATZUNG_AM_BEREICH = listOf("dl_retten", "abluft", "luefter", "schaum", "vb_pumproll", "vb_wundstreifen")
private val REIHENFOLGE = listOf("Führung", "Wasser", "Drehleiter", "Technik", "Gefahrgut", "Fläche", "Versorgung")

/** Der Kasten FwSim im Fahrzeug — mit „Erweitern" über die ganze Fläche. */
@Composable
fun ColumnScope.FeuerwehrFenster(einsatz: Einsatz, meins: Rundenfahrzeug, befehle: Raumbefehle) {
    val lage = einsatz.feuerwehr ?: return
    if (meins.organisation != "Feuerwehr" || meins.status != 4) return
    var erweitert by remember(einsatz.id) { mutableStateOf(false) }

    Kasten(innenraum = Abstand.Normal, abstandInnen = Abstand.Klein) {
        Bogenkopf("FwSim · Einsatzstelle", beiErweitern = { erweitert = true })
        if (!erweitert) {
            FwsimBogen(einsatz.id, lage, meins, gross = false, befehle)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                SehrLeise("FwSim ist aufgeklappt.", mono = true)
                Textweg("zuklappen", { erweitert = false })
            }
        }
    }

    if (erweitert) {
        Vollblende("FwSim · ${meins.funkrufname}", lage.titel, { erweitert = false }) {
            FwsimBogen(einsatz.id, lage, meins, gross = true, befehle)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.FwsimBogen(einsatzId: String, lage: Fwlage, meins: Rundenfahrzeug, gross: Boolean, befehle: Raumbefehle) {
    val jetzt = sekundentakt()
    // Sekunden seit dem letzten Stand — gedeckelt, damit eine schiefe Uhr nicht davonläuft.
    val seitStand = ((jetzt - (zeitpunktMs(lage.stand) ?: jetzt)) / 1000f).coerceIn(0f, 4f)
    val mein = lage.fahrzeuge.firstOrNull { it.vehicleId == meins.id }
    val meineTrupps = lage.trupps.filter { it.vehicleId == mein?.vehicleId }
    val andereTrupps = lage.trupps.filter { it.vehicleId != mein?.vehicleId }
    val fertig = lage.fertigUm != null

    var bereichId by remember(einsatzId) { mutableStateOf<String?>(null) }
    var truppId by remember(einsatzId) { mutableStateOf<String?>(null) }
    var reiter by remember(einsatzId) { mutableStateOf<String?>(null) }
    var berichtZu by remember(lage.fertigUm) { mutableStateOf(false) }
    LaunchedEffect(lage.bereiche.size) { if (lage.bereiche.size == 1) bereichId = lage.bereiche.first().id }

    val bereich = lage.bereiche.firstOrNull { it.id == bereichId }
    val trupp = meineTrupps.firstOrNull { it.id == truppId }

    fun druck(t: Fwtrupp): Double? = t.druck?.let { (it - t.abfall * seitStand).coerceAtLeast(0.0) }
    fun bereichName(id: String?) = lage.bereiche.firstOrNull { it.id == id }?.name.orEmpty()
    fun auftrag(id: String) = mein?.auftraege?.firstOrNull { it.id == id }
    val notfaelle = lage.trupps.filter { it.status == "Notfall" && it.geholtVon == null }
    val meinSicherheitstrupp = meineTrupps.firstOrNull { it.status == "Sicherheit" }
    val asue = "asue" in lage.erledigt

    fun passtZumTrupp(id: String, t: Fwtrupp): Boolean {
        val draussen = t.status == "Bereit" || t.status == "Erholung"
        return when (id) {
            "ausruesten" -> t.kannAtemschutz && !t.unterAtemschutz && t.status == "Bereit"
            "retten" -> t.status == "Einsatz" && (lage.bereiche.firstOrNull { it.id == t.bereichId }?.gefunden ?: 0) > 0
            "rueckzug" -> t.status == "Vorgehen" || t.status == "Einsatz"
            "flaschenwechsel" -> t.unterAtemschutz && draussen && (druck(t) ?: 0.0) < 295
            "sicherheitstrupp" -> t.unterAtemschutz && t.status == "Bereit"
            "st_einsetzen" -> t.status == "Sicherheit" && notfaelle.isNotEmpty()
            else -> t.status == "Bereit" || t.status == "Einsatz" || t.status == "Sicherheit"
        }
    }
    val truppImBereich = if (trupp == null) emptyList() else BEREICHSAUFTRAEGE.mapNotNull(::auftrag).filter { passtZumTrupp(it.id, trupp) }
    val truppSonst = if (trupp == null) emptyList() else TRUPPAUFTRAEGE.mapNotNull(::auftrag).filter { passtZumTrupp(it.id, trupp) }
    val besatzungAmBereich = BESATZUNG_AM_BEREICH.mapNotNull(::auftrag)
    val meinVorgang = lage.laufend.firstOrNull { it.vehicleId == mein?.vehicleId }

    fun geben(a: Fwauftrag?, id: String? = a?.id, t: String? = null, b: String? = null, ausWahl: Boolean = true) {
        val auftragId = id ?: return
        val ziel = a?.ziel ?: auftrag(auftragId)?.ziel ?: "Keines"
        val tWahl = if (!ausWahl) t else if (ziel == "Trupp" || ziel == "TruppUndBereich") truppId else null
        val bWahl = if (!ausWahl) b else if (ziel == "Bereich" || ziel == "TruppUndBereich") bereichId else null
        befehle.fwAuftrag(einsatzId, auftragId, tWahl, bWahl)
    }
    fun zielFehlt(a: Fwauftrag): String? = when {
        (a.ziel == "Bereich" || a.ziel == "TruppUndBereich") && bereich == null -> "Bereich antippen."
        (a.ziel == "Trupp" || a.ziel == "TruppUndBereich") && trupp == null -> "Trupp wählen."
        else -> null
    }

    // -------------------------------------------------------------------- Kopf
    val s = (lage.minuten * 60 + seitStand).roundToInt().coerceAtLeast(0)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        Marke(ART[lage.art] ?: lage.art, farbe = Farben.SignalHell)
        Text(lage.titel, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
        val klein = Schrift.Winzig.copy(fontFamily = Schrift.Mono)
        Text("⏱ ${s / 60}:${(s % 60).toString().padStart(2, '0')}", style = klein, color = Farben.TextLeise)
        Text("💧 ${VERSORGUNG[lage.versorgung] ?: lage.versorgung}", style = klein, color = if (lage.versorgung == "Tank") Farben.AmberHell else Farben.TextLeise)
        if (lage.gerettet > 0) Text("✚ ${lage.gerettet} gerettet", style = klein, color = Farben.GruenHell)
        if (lage.opfer > 0) Text("✝ ${lage.opfer}", style = klein, color = Farben.SignalHell)
        if (lage.verletzteKraefte > 0) Text("⚠ ${lage.verletzteKraefte} verletzt", style = klein, color = Farben.SignalHell)
    }

    // ------------------------------------------------------------------ MAYDAY
    notfaelle.forEach { n ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth().background(Farben.SignalTief, Rundung.Klein).border(1.dp, Farben.Signal, Rundung.Klein).padding(Abstand.Klein),
        ) {
            Text(
                "MAYDAY — ${n.kurz} ${n.funkrufname} im ${bereichName(n.bereichId)}" + (druck(n)?.let { " · ${it.roundToInt()} bar" } ?: ""),
                style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            if (meinSicherheitstrupp != null) {
                Knopf("${meinSicherheitstrupp.kurz} als Sicherheitstrupp einsetzen", {
                    geben(null, "st_einsetzen", meinSicherheitstrupp.id, null, ausWahl = false)
                }, art = Knopfart.Alarm, kompakt = true)
            } else {
                Text("Kein eigener Sicherheitstrupp bereit.", style = Schrift.Klein, color = Color.White)
            }
        }
    }

    // ------------------------------------------------------------------ Bericht
    lage.bericht?.takeIf { !berichtZu }?.let { b ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth().flaeche(farbe = Farben.FlaecheHoch, randfarbe = Farben.Amber, ecke = 9.dp, mitLichtkante = false).padding(Abstand.Klein),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(b.note, style = Schrift.Titel.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
                Column(Modifier.weight(1f)) {
                    Etikett("Einsatzbericht · ${b.minuten.roundToInt()} min")
                    Text("${b.punkte.roundToInt()} Punkte", style = Schrift.Klein.copy(fontFamily = Schrift.Mono), color = Farben.Text)
                }
            }
            b.lob.forEach { Text("✓ $it", style = Schrift.Klein, color = Farben.GruenHell) }
            b.kritik.forEach { Text("✗ $it", style = Schrift.Klein, color = Farben.SignalHell) }
            Row { Knopf("Schließen", { berichtZu = true }, art = Knopfart.Leise, kompakt = true) }
        }
    }

    // ---------------------------------------------------------------- Lagebild
    FwLagebild(
        lage = lage,
        gewaehlt = bereichId,
        meinFahrzeugId = mein?.vehicleId,
        beiWahl = { id -> bereichId = if (bereichId == id && truppId == null) null else id },
    )
    Text(lage.lagebild, style = Schrift.Klein, color = Farben.TextLeise)

    if (bereich != null) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth().flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false).padding(Abstand.Klein),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                Text(bereich.name, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                if (bereich.abluft) Marke("Abluft")
                if (bereich.belueftet) Marke("Lüfter")
                if (bereich.riegel) Marke("Riegel")
                if (bereich.kontrolliert) Marke("kalt", farbe = Farben.GruenHell)
                if (!bereich.erkundet) Marke("nicht erkundet", farbe = Farben.TextSehrLeise)
            }
            Balkenzeile("Feuer", bereich.feuer, Farben.Signal)
            Balkenzeile("Rauch", bereich.rauch, Farben.TextSehrLeise)
            if (bereich.art == "Geschoss") Balkenzeile("Durchsucht", bereich.durchsucht, Farben.BlauHell)
            bereich.gefaehrdung?.let { Balkenzeile("Gefährdung", it, Farben.Amber) }
            if (bereich.vermisste != null || bereich.amFenster > 0) {
                Text(
                    buildString {
                        if ((bereich.vermisste ?: 0) > 0) {
                            append("${bereich.vermisste} vermisst")
                            if (bereich.gefunden > 0) append(", ${bereich.gefunden} gefunden")
                            append(". ")
                        }
                        if (bereich.amFenster > 0) append("${bereich.amFenster} am Fenster. ")
                        if (bereich.vermisste == 0 && bereich.amFenster == 0) append("Niemand vermisst.")
                    },
                    style = Schrift.Klein,
                    color = Farben.Text,
                )
            }
            if (trupp != null && !fertig && truppImBereich.isNotEmpty()) {
                SehrLeise("${trupp.kurz} hierher:")
                Pillenreihe {
                    truppImBereich.forEach { a -> Pille(a.name, an = false, aktiv = a.sperre == null, beiDruck = { geben(a) }) }
                }
            } else if (trupp != null && !fertig) {
                SehrLeise("${trupp.kurz} ist gerade nicht verfügbar (${TRUPPSTATUS[trupp.status] ?: trupp.status}).")
            } else if (meineTrupps.isNotEmpty() && !fertig) {
                SehrLeise("Trupp wählen, um ihn hierher zu schicken.")
            }
            if (besatzungAmBereich.isNotEmpty() && !fertig) {
                SehrLeise("Besatzung:")
                Pillenreihe {
                    besatzungAmBereich.forEach { a -> Pille(a.name, an = false, aktiv = a.sperre == null && meinVorgang == null, beiDruck = { geben(a) }) }
                }
            }
        }
    } else {
        SehrLeise("Bereich antippen — dort steht, was man weiß und wen man schicken kann.")
    }

    // ------------------------------------------------------------ eigene Trupps
    if (meineTrupps.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
            meineTrupps.forEachIndexed { i, t ->
                val d = druck(t)
                val stufe = when {
                    d == null -> Farben.TextSehrLeise
                    d <= 60 -> Farben.Signal
                    d <= 150 -> Farben.Amber
                    else -> Farben.GruenHell
                }
                val an = t.id == truppId
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .flaeche(
                            farbe = if (t.status == "Notfall") Farben.SignalTief else if (an) Farben.FlaecheAktiv else Farben.FlaecheHoch,
                            randfarbe = if (an) Farben.Amber else Farben.Rand,
                            ecke = 9.dp,
                            mitLichtkante = false,
                        )
                        .clickable(role = Role.Tab, indication = null, interactionSource = null) { truppId = if (truppId == t.id) null else t.id }
                        .padding(Abstand.Klein),
                ) {
                    Text(
                        t.kurz,
                        style = Schrift.Normal.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                        color = stufe,
                        modifier = Modifier.width(36.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text("${i + 1} ${t.name}", style = Schrift.Klein, color = Farben.Text)
                        Text(
                            (TRUPPSTATUS[t.status] ?: t.status) +
                                (if (t.bereichId != null && t.status != "Bereit") " · ${bereichName(t.bereichId)}" else "") +
                                (if (t.taetigkeit != null && t.status == "Einsatz") " · ${TAETIGKEIT[t.taetigkeit] ?: t.taetigkeit}" else "") +
                                (restSekunden(t.statusBis, jetzt)?.let { " · $it s" } ?: ""),
                            style = Schrift.Winzig,
                            color = Farben.TextLeise,
                        )
                        Text(
                            when {
                                !t.unterAtemschutz -> if (t.kannAtemschutz) "ohne PA" else "kein PA"
                                d != null -> "${d.roundToInt()} bar"
                                else -> "Druck ? — keine ASÜ"
                            } + (if (t.rohr) " · Rohr" else "") + (if (t.traegt > 0) " · trägt Person" else "") + (if (t.csa) " · CSA" else ""),
                            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                            color = stufe,
                        )
                    }
                }
            }
        }
    }
    if (trupp != null && !fertig && truppSonst.isNotEmpty()) {
        SehrLeise("${trupp.kurz}:")
        Pillenreihe {
            truppSonst.forEach { a ->
                Pille(a.name, an = false, aktiv = a.sperre == null, farbe = if (a.id == "rueckzug") Farben.SignalHell else Farben.Amber, beiDruck = { geben(a) })
            }
        }
    }
    if (andereTrupps.isNotEmpty()) {
        var offen by remember { mutableStateOf(false) }
        Textweg("Andere Trupps an der Stelle (${andereTrupps.size})", { offen = !offen }, farbe = Farben.TextLeise)
        if (offen) {
            andereTrupps.forEach { t ->
                Text(
                    "${t.kurz} ${t.funkrufname} — ${TRUPPSTATUS[t.status] ?: t.status}" +
                        (t.bereichId?.let { " · ${bereichName(it)}" } ?: "") +
                        (druck(t)?.let { " · ${it.roundToInt()} bar" } ?: ""),
                    style = Schrift.Winzig,
                    color = Farben.TextLeise,
                )
            }
        }
    }

    // --------------------------------------------------- Rat und laufende Arbeit
    val empfehlung = mein?.empfehlung
    if (empfehlung != null && !fertig) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().flaeche(farbe = Farben.FlaecheHoch, randfarbe = Farben.AmberTief, ecke = 9.dp, mitLichtkante = false).padding(Abstand.Klein),
        ) {
            Column(Modifier.weight(1f)) {
                SehrLeise("Gruppenführer rät")
                Text(
                    empfehlung.name +
                        (empfehlung.truppId?.let { id -> meineTrupps.firstOrNull { it.id == id }?.kurz?.let { " · $it" } } ?: "") +
                        (empfehlung.bereichId?.let { " · ${bereichName(it)}" } ?: ""),
                    style = Schrift.Klein,
                    color = Farben.Text,
                )
            }
            Knopf("Los", {
                geben(null, empfehlung.auftragId, empfehlung.truppId, empfehlung.bereichId, ausWahl = false)
            }, art = Knopfart.Haupt, kompakt = true)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        if (meinVorgang != null) {
            Text(
                meinVorgang.name + (meinVorgang.bereichId?.let { " · ${bereichName(it)}" } ?: "") +
                    (restSekunden(meinVorgang.fertigUm, jetzt)?.let { " · $it s" } ?: ""),
                style = Schrift.Klein,
                color = Farben.Text,
            )
            Laufbalken(anteilZwischen(meinVorgang.beginnUm, meinVorgang.fertigUm, jetzt))
        } else {
            SehrLeise("Besatzung frei.")
        }
    }

    // ------------------------------------------------------- Aufträge der Besatzung
    if (mein == null) {
        SehrLeise("Dein Fahrzeug ist noch nicht an der Einsatzstelle angemeldet.")
    } else if (!fertig) {
        val alle = mein.auftraege.filter { it.gruppe != "Trupps" }
        val gruppen = REIHENFOLGE.map { g -> g to alle.filter { it.gruppe == g } }.filter { it.second.isNotEmpty() }
        if (gruppen.isNotEmpty()) {
            val aktiv = gruppen.firstOrNull { it.first == reiter } ?: gruppen.first()
            Pillenreihe {
                gruppen.forEach { (name, _) -> Pille(name, an = aktiv.first == name, beiDruck = { reiter = name }) }
            }
            aktiv.second.forEach { a ->
                val erledigt = a.sperre == "Erledigt."
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(a.name, style = Schrift.Klein, color = if (erledigt) Farben.TextSehrLeise else Farben.Text)
                        val zeile = listOfNotNull(if (a.dauer > 0) "${a.dauer.roundToInt()} s" else null, a.tipp).joinToString(" · ")
                        if (zeile.isNotBlank()) Text(zeile, style = Schrift.Winzig, color = Farben.TextSehrLeise)
                        val grund = if (a.sperre != null && !erledigt) a.sperre else zielFehlt(a)
                        grund?.let { Text(it, style = Schrift.Winzig, color = Farben.AmberHell) }
                    }
                    if (erledigt) {
                        Text("✓", style = Schrift.Gross, color = Farben.GruenHell)
                    } else {
                        Knopf(
                            if (a.ziel == "Bereich" && bereich != null) "→ ${bereich.name}" else "Los",
                            { geben(a) },
                            aktiv = a.sperre == null && zielFehlt(a) == null,
                            kompakt = true,
                        )
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------- eigene Mittel
    if (mein != null) {
        if (mein.tankMax > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                SehrLeise("Tank")
                Laufbalken((mein.tank / mein.tankMax).toFloat(), Modifier.weight(1f), farbe = Farben.BlauHell)
                SehrLeise("${mein.tank.roundToInt()} / ${mein.tankMax.roundToInt()} l", mono = true)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            if (mein.schaum > 0) SehrLeise("Schaum ${mein.schaum.roundToInt()} l", mono = true)
            if (mein.reserveflaschen > 0 || meineTrupps.any { it.kannAtemschutz }) {
                SehrLeise("Reserveflaschen ${if ("as_sammelplatz" in lage.erledigt) "∞" else mein.reserveflaschen}", mono = true)
            }
            if (!asue && meineTrupps.any { it.unterAtemschutz }) {
                Text("Keine Atemschutzüberwachung!", style = Schrift.Winzig, color = Farben.SignalHell)
            }
        }
    }
    lage.patient?.let { pt ->
        Text(
            "Patient: " + when {
                pt.befreit -> "befreit"
                pt.verstorben -> "verstorben"
                else -> "eingeklemmt (${pt.einklemmung ?: "unklar — erkunden"})"
            } + " · Zustand ${pt.zustand.roundToInt()}",
            style = Schrift.Klein,
            color = Farben.Text,
        )
    }
    lage.stoff?.let { st ->
        Text(
            (st.name ?: "Stoff unbekannt — messen") +
                (st.unNummer?.let { " · UN $it" } ?: "") +
                (st.gefahrenradius?.let { " · mind. ${it.roundToInt()} m" } ?: "") +
                (st.leck?.let { " · Leck ${it.roundToInt()} %" } ?: "") +
                " · ${if (st.gasfoermig) "Wolke" else "Lache"} ${st.ausbreitung.roundToInt()} %",
            style = Schrift.Klein,
            color = Farben.Text,
        )
    }
    if (lage.offen.isNotEmpty() && !fertig) {
        var offen by remember { mutableStateOf(gross) }
        Textweg("Noch offen (${lage.offen.size})", { offen = !offen }, farbe = Farben.TextLeise)
        if (offen) lage.offen.forEach { Text("• $it", style = Schrift.Winzig, color = Farben.TextLeise) }
    }

    // ---------------------------------------------------------------- Lagelog
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        lage.lagelog.asReversed().take(if (gross) 60 else 12).forEach { m ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(uhrzeitMitSekunden(m.zeit), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                Text(
                    (m.von?.let { "$it · " } ?: "") + m.text,
                    style = Schrift.Winzig,
                    color = when (m.art) {
                        "Erfolg" -> Farben.GruenHell
                        "Warnung" -> Farben.AmberHell
                        "Gefahr" -> Farben.SignalHell
                        else -> Farben.TextLeise
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    SehrLeise(
        "An der Stelle: " + lage.fahrzeuge.joinToString(", ") { f ->
            "${f.funkrufname} (${f.rolle}${if (f.bot) ", Bot" else ""})"
        },
    )
}

@Composable
private fun Balkenzeile(name: String, wert: Double?, farbe: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = Schrift.Winzig, color = Farben.TextLeise, modifier = Modifier.width(76.dp))
        Laufbalken(((wert ?: 0.0) / 100).toFloat(), Modifier.weight(1f), farbe = farbe)
        Text(wert?.roundToInt()?.toString() ?: "?", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise, modifier = Modifier.width(28.dp))
    }
}

/** „14:32:05" — die Zeit einer Zeile im Lagelog. */
internal fun uhrzeitMitSekunden(roh: String?): String {
    val ms = zeitpunktMs(roh) ?: return ""
    val t = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
    return "%02d:%02d:%02d".format(t.hour, t.minute, t.second)
}
