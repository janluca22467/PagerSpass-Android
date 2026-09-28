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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Melderwerk
import de.pagerspass.pagerspass.mobil.Tonwahl
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Ort
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Das Bedienteil des Fahrzeugs — FMS-Tastatur, Sondersignal, Lagemeldung,
 * Nachforderung und die eigene Feststellung der Streife.
 *
 * Übertragen aus `FmsTastatur.vue`, `Sondersignal.vue`, `LagemeldungForm.vue` und
 * `FeststellungForm.vue` samt `composables/fmsstatus.ts`: dieselben Regeln, welche
 * Taste gerade etwas nützt, derselbe Rückfrage-Schritt bei Status 6, dieselbe
 * Anfahrtszeile. <b>Verbindlich entscheidet der Server</b> — hier stehen die Regeln
 * nur, damit kein Knopf dasteht, der verlässlich eine Absage erzeugt.
 */

// ------------------------------------------------------------------- Anfahrt
//
// Entfernung, Restweg, Tempo und Zeitstempel stehen in `Leitstellenhilfen.kt` —
// dieselbe Rechnung für beide Plätze, damit Tastatur, Karte und Tableau einander
// nicht widersprechen.

// ------------------------------------------------------------------ FMS

/** Die Gründe für Status 6 — mit der Dauer, die der Griff im Dienst wirklich braucht. */
private val AUSSER_DIENST = listOf(
    "Tanken" to 10,
    "Reinigung nach Einsatz" to 20,
    "Atemschutzgeräte tauschen" to 15,
    "Material ergänzen" to 25,
    "Verpflegungspause" to 30,
    "Technischer Defekt" to 60,
)

/** Die Reihenfolge der Tasten — wie auf einem FMS-Gerät, die 0 am Ende. */
val FMS_TASTEN = listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)

/**
 * Ob eine Taste gerade etwas nützt — `erlaubt` in `composables/fmsstatus.ts`, als
 * Spiegel von `FmsRules` am Server.
 */
fun fmsErlaubt(f: Rundenfahrzeug, einsatz: Einsatz?, status: Int, jetzt: Long): Boolean {
    if (f.status == status) return false
    return when (status) {
        3 -> {
            if (f.templateId == "wlfab") {
                if (f.abrollbehaelterTemplateId == null) return false
                val bis = zeitMillis(f.aufsattelnBis)
                if (bis != null && bis > jetzt) return false
            }
            f.einsatzId != null
        }
        4 -> {
            if (f.status != 3) return false
            val hatOrt = einsatz?.lat != null && einsatz.lon != null
            !hatOrt || f.einsatzstelleErreicht
        }
        7 -> f.status == 4
        8 -> f.status == 7
        else -> true
    }
}

/** Wie lange eine Sprechaufforderung am Gerät steht — die Frist des Servers. */
private const val SPRECHAUFFORDERUNG_MS = 2 * 60_000L

/** Ob gerade eine Sprechaufforderung gilt — das J am Bedienteil. */
fun sprechaufforderungSteht(f: Rundenfahrzeug, jetzt: Long): Boolean {
    val seit = zeitMillis(f.sprechaufforderungSeit) ?: return false
    return jetzt - seit < SPRECHAUFFORDERUNG_MS
}

/**
 * Die FMS-Tastatur — neun Kacheln mit Zeichen, Zahl und Wort, darüber die
 * Anfahrtszeile, die Streife und die Sprechaufforderung, darunter die Rückfrage
 * nach dem Grund für Status 6.
 */
@Composable
fun ColumnScope.FmsTastatur(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    einsatz: Einsatz?,
    katalog: Katalog?,
    jetzt: Long,
    beiFms: (Int, String?, Int?) -> Unit,
    beiStreife: (Boolean) -> Unit,
    beiTaste: () -> Unit,
) {
    var grundOffen by remember { mutableStateOf(false) }
    var eigenerGrund by remember { mutableStateOf("") }

    // Aufsatteln am Wechselladerfahrzeug: Status 3 ist gesperrt, solange der Hakenarm arbeitet.
    if (meins.templateId == "wlfab" && meins.abrollbehaelterTemplateId != null && meins.einsatzId != null) {
        val rest = zeitMillis(meins.aufsattelnBis)?.let { ((it - jetzt + 999) / 1000).coerceAtLeast(0) } ?: 0
        val abName = katalog?.fahrzeuge?.firstOrNull { it.id == meins.abrollbehaelterTemplateId }?.typ
            ?.replace("WLF + ", "") ?: "Abrollbehälter"
        Text(
            if (rest > 0) "$abName wird aufgesattelt · noch $rest s · Status 3 gesperrt" else "$abName aufgenommen · ausrückbereit",
            style = Schrift.MonoKlein,
            color = if (rest > 0) Farben.AmberHell else Farben.GruenHell,
        )
    }

    anfahrtszeile(raum, meins, einsatz)?.let { (text, da) ->
        Text(
            text,
            style = Schrift.MonoKlein.copy(fontWeight = if (da) FontWeight.Bold else FontWeight.Normal),
            color = if (da) Farben.GruenHell else Farben.TextLeise,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (da) Farben.Gruen.copy(alpha = 0.16f) else Color.Transparent, Rundung.Winzig)
                .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
        )
    }

    // Die Streife: der Griff und, solange sie läuft, wohin sie fährt.
    if (meins.streifenfaehig) {
        val moeglich = meins.aufStreife || (meins.einsatzId == null && (meins.status == 1 || meins.status == 2))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (meins.aufStreife) "⇢ Streife" + (meins.streifenziel?.let { " — $it" } ?: "") else "",
                style = Schrift.MonoKlein,
                color = Farben.BlauHell,
                modifier = Modifier.weight(1f),
            )
            Knopf(
                if (meins.aufStreife) "Streife beenden" else "Streife aufnehmen",
                { beiStreife(!meins.aufStreife) },
                aktiv = moeglich,
                kompakt = true,
            )
        }
        if (!moeglich) SehrLeise("Dafür muss das Fahrzeug einsatzbereit und ohne Auftrag sein.")
    }

    if (sprechaufforderungSteht(meins, jetzt)) {
        Blinkzeile("J", "Sprechaufforderung — Leitstelle bittet ums Wort")
    }

    val bezeichnung = katalog?.fmsStatus.orEmpty().associate { it.status to it.aufschrift(meins.organisation) }
    FMS_TASTEN.chunked(3).forEach { reihe ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            reihe.forEach { t ->
                val an = meins.status == t
                val aus = !fmsErlaubt(meins, einsatz, t, jetzt) && !an
                val farbe = fmsFarbe(t)
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 62.dp)
                        .flaeche(
                            farbe = if (an) farbe else Farben.Flaeche,
                            randfarbe = if (aus) Farben.Rand else farbe,
                            ecke = 10.dp,
                            mitLichtkante = !an,
                        )
                        .clickable(role = Role.Button) {
                            if (t == 6) {
                                beiTaste()
                                grundOffen = true
                            } else {
                                grundOffen = false
                                beiFms(t, null, null)
                            }
                        }
                        .padding(Abstand.Klein),
                ) {
                    val schrift = when {
                        an -> Farben.AufFarbe
                        aus -> Farben.TextSehrLeise.copy(alpha = 0.6f)
                        else -> Farben.Text
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                        FmsZeichen(t, meins.organisation, if (an) Farben.AufFarbe else if (aus) schrift else farbe)
                        Text(t.toString(), style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = if (an) Farben.AufFarbe else if (aus) schrift else farbe)
                    }
                    Text(
                        bezeichnung[t].orEmpty(),
                        style = Schrift.Winzig,
                        color = schrift,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (grundOffen) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche(randfarbe = Farben.FmsDefekt).padding(Abstand.Normal),
        ) {
            Text("Grund für Status 6?", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Pillenreihe {
                AUSSER_DIENST.forEach { (text, dauer) ->
                    Pille(text, an = false, beiDruck = {
                        grundOffen = false
                        beiFms(6, text, dauer)
                    })
                }
                Pille("Ohne Angabe", an = false, farbe = Farben.TextLeise, beiDruck = {
                    grundOffen = false
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
                Knopf(
                    "Melden",
                    {
                        val getippt = eigenerGrund.trim()
                        if (getippt.isNotEmpty()) {
                            eigenerGrund = ""
                            grundOffen = false
                            beiFms(6, getippt, null)
                        }
                    },
                    aktiv = eigenerGrund.isNotBlank(),
                    kompakt = true,
                )
            }
        }
    }
}

/**
 * Die Anfahrtszeile — für beide Fahrten: zur Einsatzstelle (Status 3) und heim
 * (Status 1). Das Paar sagt dazu, ob das Ziel erreicht ist; dann steht dort die
 * nächste Handlung statt einer Entfernung.
 */
fun anfahrtszeile(raum: Raumzustand, f: Rundenfahrzeug, e: Einsatz?): Pair<String, Boolean>? {
    if (f.status == 1 && !f.aufStreife) {
        val lat = f.lat
        val lon = f.lon
        val wLat = f.wacheLat
        val wLon = f.wacheLon
        if (lat == null || lon == null || wLat == null || wLon == null) return "Rückfahrt zur Wache" to false
        if (distanzMeter(lat, lon, wLat, wLon) <= 50) return "Wache erreicht — Status 2 melden" to true
        val heim = restEntfernungMeter(lat, lon, f.route, f.routeIndex, wLat, wLon)
        val dauer = heim / tempoMs(raum, f.organisation, 1)
        return "Noch ${formatEntfernung(heim)} bis zur Wache · ca. ${formatAnfahrtszeit(dauer)}" to false
    }
    if (f.status != 3 || e?.lat == null || e.lon == null) return null
    if (f.einsatzstelleErreicht) return "Einsatzstelle erreicht — Status 4 melden" to true
    val lat = f.lat
    val lon = f.lon
    if (lat == null || lon == null) return "Anfahrt läuft" to false
    val m = restEntfernungMeter(lat, lon, f.route, f.routeIndex, e.lat, e.lon)
    val sek = m / tempoMs(raum, f.organisation, if (f.sondersignalAus) 1 else e.prioritaet)
    return "Noch ${formatEntfernung(m)} bis zur Einsatzstelle · ca. ${formatAnfahrtszeit(sek)}" to false
}

/** Eine blinkende Zeile mit Zeichen — die Sprechaufforderung. */
@Composable
internal fun Blinkzeile(zeichen: String, text: String) {
    val takt = rememberInfiniteTransition(label = "blinkt")
    val a by takt.animateFloat(0.45f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "blinkt-wert")
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.FmsSprechwunsch.copy(alpha = 0.25f * a), Rundung.Winzig)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(22.dp).background(Farben.FmsSprechwunsch, CircleShape),
        ) {
            Text(zeichen, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        Text(text, style = Schrift.MonoKlein, color = Farben.Text.copy(alpha = a))
    }
}

/**
 * Der FMS-Kasten: zwei Reiter, „FMS-Status" und „Sondersignal". Bei der Bauart
 * „Im Funk" trägt das Funkgerät die Tasten — dann bleibt nur das Sondersignal; hat
 * das Fahrzeug kein Blaulicht (Hubschrauber, ziviler Wagen), bleibt nur die Tastatur.
 */
@Composable
fun FmsKasten(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    einsatz: Einsatz?,
    katalog: Katalog?,
    jetzt: Long,
    imFunk: Boolean,
    tonwahl: Tonwahl,
    werk: Melderwerk,
    beiFms: (Int, String?, Int?) -> Unit,
    beiSondersignal: (Boolean) -> Unit,
    beiStreife: (Boolean) -> Unit,
) {
    val blaulicht = hatBlaulicht(meins)
    if (imFunk && !blaulicht) return
    var reiter by remember { mutableStateOf("fms") }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        if (!imFunk && blaulicht) {
            Reiterreihe {
                Reiter("FMS-Status", offen = reiter == "fms", beiDruck = { reiter = "fms" })
                Reiter("Sondersignal", offen = reiter == "signal", beiDruck = { reiter = "signal" })
            }
        } else {
            Etikett(if (imFunk) "Sondersignal" else "FMS-Status")
        }
        if (blaulicht && (imFunk || reiter == "signal")) {
            Sondersignalteil(meins, tonwahl, werk, beiSondersignal)
        } else {
            FmsTastatur(raum, meins, einsatz, katalog, jetzt, beiFms, beiStreife, beiTaste = { werk.taste(tonwahl.funkpegel) })
        }
    }
}

// ---------------------------------------------------------------- Sondersignal

/** Was das eigene Fahrzeug an Sondersignal trägt — `ausstattung` in `Sondersignal.vue`. */
data class Signalausstattung(val blaulicht: Boolean, val horn: Boolean, val heck: Boolean, val matrix: Boolean)

/**
 * Die Ausstattung aus dem Bauplan des Fahrzeugs: Der Bauplan darf das Blaulicht
 * abbestellen (ziviler Pkw); am Boot ist das Horn ein Typhon und am Hubschrauber
 * gar nichts; eine Heckwarnanlage braucht ein breites Heck; die Matrix haben der
 * Streifenwagen und die Großfahrzeuge.
 */
fun signalausstattung(f: Rundenfahrzeug): Signalausstattung {
    val bp = de.pagerspass.pagerspass.ui.fahrzeug.bauplanFuer(f.typ, f.organisation)
    val form = bp.form
    val luft = f.istLuftfahrzeug || form == de.pagerspass.pagerspass.ui.fahrzeug.Form.Heli
    val schmal = form == de.pagerspass.pagerspass.ui.fahrzeug.Form.Moto || form == de.pagerspass.pagerspass.ui.fahrzeug.Form.Quad
    val boot = form == de.pagerspass.pagerspass.ui.fahrzeug.Form.Boot
    val gross = bp.klasse == de.pagerspass.pagerspass.ui.fahrzeug.Klasse.Lkw ||
        bp.klasse == de.pagerspass.pagerspass.ui.fahrzeug.Klasse.Transporter
    return Signalausstattung(
        blaulicht = bp.blaulicht,
        horn = !luft && !boot,
        heck = !luft && !schmal && !boot,
        matrix = !luft && !schmal && !boot && (gross || f.organisation == "Polizei"),
    )
}

/** Ob das Fahrzeug überhaupt Blaulicht trägt — die Schranke des Bedienteils. */
fun hatBlaulicht(f: Rundenfahrzeug): Boolean = signalausstattung(f).blaulicht

/** Was die Matrixtafel zeigen kann — die Polizei spricht, die anderen sichern. */
private fun matrixbilder(organisation: String): List<Pair<String, String>> =
    if (organisation == "Polizei") {
        listOf("BITTE FOLGEN" to "Bitte folgen", "STOP POLIZEI" to "Stop", "UNFALL" to "Unfall", "◀◀◀" to "Pfeil links", "▶▶▶" to "Pfeil rechts")
    } else {
        listOf("◀◀◀" to "Pfeil links", "▶▶▶" to "Pfeil rechts", "⚠" to "Gefahrstelle", "UNFALL" to "Unfall", "STAU" to "Stau", "RETTUNGSGASSE" to "Rettungsgasse")
    }

/**
 * Das Sondersignal — die Entscheidung des Fahrzeugführers.
 *
 * <b>Am Server hängt genau ein Knopf: das Blaulicht.</b> Ohne Blau fährt der Server
 * das Fahrzeug mit dem Verkehr. Signalhorn, Heckwarner und Matrixtafel laufen lokal
 * auf diesem Gerät — kein Mitspieler sieht oder hört sie, sie sind das Gefühl am
 * Bedienteil. Wer ein Fahrzeug betritt, betritt es dunkel.
 */
@Composable
private fun ColumnScope.Sondersignalteil(
    meins: Rundenfahrzeug,
    tonwahl: Tonwahl,
    werk: Melderwerk,
    beiSondersignal: (Boolean) -> Unit,
) {
    val blau = !meins.sondersignalAus
    var horn by remember { mutableStateOf(false) }
    var hornArt by remember { mutableStateOf("stadt") }
    var heck by remember { mutableStateOf(false) }
    var matrix by remember { mutableStateOf<String?>(null) }
    var matrixOffen by remember { mutableStateOf(false) }
    var matrixText by remember { mutableStateOf("") }
    var nacht by remember { mutableStateOf(false) }
    var laut by remember { mutableStateOf(70) }
    val ausstattung = remember(meins.typ, meins.organisation, meins.istLuftfahrzeug) { signalausstattung(meins) }
    val hatHorn = ausstattung.horn
    val hatHeck = ausstattung.heck

    // Das Horn tönt, solange es an ist — und nur mit Blaulicht, wie im echten Fahrzeug.
    LaunchedEffect(horn, hornArt, laut, blau, tonwahl.umgebungspegel) {
        if (horn && blau) werk.hornStarten(hornArt, laut / 100f * tonwahl.umgebungspegel) else werk.hornStoppen()
    }
    LaunchedEffect(blau) { if (!blau) horn = false }

    val takt = rememberInfiniteTransition(label = "blau")
    val blitz by takt.animateFloat(0f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "blau-wert")

    Text(
        "Blaulicht schaltet die Fahrt — Matrix, Heckwarner und Signalhorn laufen lokal auf diesem Gerät.",
        style = Schrift.Klein,
        color = Farben.TextLeise,
    )

    // Die Lichter: vorne die Blitzer, hinten Heckwarner und Tafel.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0C10), Rundung.Klein).padding(Abstand.Klein),
    ) {
        repeat(3) { i ->
            val an = blau && (if (i % 2 == 0) blitz > 0.5f else blitz <= 0.5f)
            Box(Modifier.size(16.dp).background(if (an) Color(0xFF3D7BFF) else Color(0xFF1B2433), RoundedCornerShape(4.dp)))
        }
        Text(
            when {
                matrix != null -> matrix.orEmpty()
                else -> ""
            },
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
            color = if (nacht) Color(0xFFB07A10) else Color(0xFFFFB020),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF14110A), RoundedCornerShape(3.dp))
                .padding(vertical = 3.dp),
        )
        repeat(3) { i ->
            val an = heck && (if (i % 2 == 0) blitz > 0.5f else blitz <= 0.5f)
            Box(Modifier.size(16.dp).background(if (an) Color(0xFFFFA000) else Color(0xFF2A2210), RoundedCornerShape(4.dp)))
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        Signalknopf("Blaulicht", blau, Modifier.weight(1f)) { beiSondersignal(blau) }
        Signalknopf("Signalhorn", horn, Modifier.weight(1f), aktiv = hatHorn && blau) { horn = !horn }
        Signalknopf("Heckwarner", heck, Modifier.weight(1f), aktiv = hatHeck) { heck = !heck }
    }
    if (heck && (meins.status == 3 || meins.status == 1) && !meins.einsatzstelleErreicht) {
        Text(
            "Heckwarnanlage nur im Stand oder bei Schrittgeschwindigkeit (§ 52 Abs. 11 StVZO).",
            style = Schrift.Winzig,
            color = Farben.SignalHell,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = ausstattung.matrix, role = Role.Button) { matrixOffen = !matrixOffen }
            .padding(vertical = Abstand.Winzig),
    ) {
        Text("MATRIX", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.TextLeise)
        Text(
            if (!ausstattung.matrix) {
                "nicht verbaut"
            } else {
                matrix?.let { m -> matrixbilder(meins.organisation).firstOrNull { it.first == m }?.second ?: "Eigener Text" } ?: "aus"
            },
            style = Schrift.MonoKlein,
            color = Farben.Text,
            modifier = Modifier.weight(1f),
        )
        Text(if (matrixOffen) "▾" else "▸", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
    }
    if (matrixOffen && ausstattung.matrix) {
        Pillenreihe {
            matrixbilder(meins.organisation).forEach { (wort, name) ->
                Pille(name, an = matrix == wort, beiDruck = { matrix = wort })
            }
            Pille("Aus", an = matrix == null, farbe = Farben.TextLeise, beiDruck = { matrix = null })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
            Feld(
                wert = matrixText,
                beiAenderung = { matrixText = it.take(24) },
                platzhalter = "Eigener Text …",
                weiterTaste = ImeAction.Done,
                modifier = Modifier.weight(1f),
            )
            Knopf("Anzeigen", { matrix = matrixText.trim().uppercase() }, aktiv = matrixText.isNotBlank(), kompakt = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text("HELLIGKEIT", style = Schrift.MonoKlein, color = Farben.TextLeise)
            Pille("Tag", an = !nacht, beiDruck = { nacht = false })
            Pille("Nacht", an = nacht, beiDruck = { nacht = true })
        }
    }

    if (hatHorn) {
        Text("HORNTON", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.TextLeise)
        Pillenreihe {
            Pille("Stadt", an = hornArt == "stadt", beiDruck = { hornArt = "stadt" })
            Pille("Land", an = hornArt == "land", beiDruck = { hornArt = "land" })
        }
        SehrLeise(if (hornArt == "stadt") "Die schnelle Folge — im Stadtverkehr." else "Die tiefe, lange Folge — über Land.")
        de.pagerspass.pagerspass.ui.bausteine.Regler(
            wert = laut,
            beiAenderung = { laut = it },
            von = 0,
            bis = 100,
            schritt = 5,
            etikett = "Lautstärke des Signalhorns",
        )
    }
}

@Composable
private fun Signalknopf(
    name: String,
    an: Boolean,
    modifier: Modifier = Modifier,
    aktiv: Boolean = true,
    beiDruck: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .flaeche(
                farbe = if (an) Farben.Blau.copy(alpha = 0.35f) else Farben.FlaecheHoch,
                randfarbe = if (an) Farben.BlauHell else Farben.Rand,
                ecke = 10.dp,
            )
            .clickable(enabled = aktiv, role = Role.Button, onClick = beiDruck)
            .padding(vertical = Abstand.Normal),
    ) {
        Box(Modifier.size(10.dp).background(if (an) Farben.BlauHell else Farben.Rand, CircleShape))
        Text(name, style = Schrift.Klein, color = if (aktiv) Farben.Text else Farben.TextSehrLeise)
    }
}

// ------------------------------------------------------- Lagemeldung

/**
 * Lagemeldung an die Leitstelle — `LagemeldungForm.vue`. Die Vorschläge kommen aus
 * dem Stichwort; das freie Feld bleibt daneben. Darunter die Nachforderung.
 */
@Composable
fun Lagemeldungskasten(
    einsatz: Einsatz?,
    katalog: Katalog?,
    beiLagemeldung: (String) -> Unit,
    beiNachfordern: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var nachforderung by remember { mutableStateOf("") }
    var nachforderungOffen by remember { mutableStateOf(false) }
    val vorschlaege = remember(einsatz?.stichwort, katalog) {
        katalog?.stichworte?.firstOrNull { it.stichwort == einsatz?.stichwort }?.lagemeldungen.orEmpty()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        Etikett("Lagemeldung an die Leitstelle")
        Feld(
            wert = text,
            beiAenderung = { text = it.take(400) },
            platzhalter = "Was findet ihr vor?",
            einzeilig = false,
            weiterTaste = ImeAction.Default,
        )
        if (vorschlaege.isNotEmpty()) {
            Pillenreihe {
                vorschlaege.forEach { v -> Pille(v, an = text == v, beiDruck = { text = v }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                "Lagemeldung absetzen",
                {
                    val inhalt = text.trim()
                    if (inhalt.isNotEmpty()) {
                        beiLagemeldung(inhalt)
                        text = ""
                    }
                },
                art = Knopfart.Haupt,
                aktiv = text.isNotBlank(),
                kompakt = true,
            )
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
                Knopf(
                    "Anfordern",
                    {
                        beiNachfordern(nachforderung.trim().ifBlank { "Weitere Kräfte erforderlich." })
                        nachforderung = ""
                        nachforderungOffen = false
                    },
                    art = Knopfart.Alarm,
                    kompakt = true,
                )
            }
        }
    }
}

// --------------------------------------------------------- Feststellung

/** Was eine Streife als eigene Feststellung anlegt. */
data class Feststellungsmeldung(
    val stichwort: String,
    val stichwortText: String,
    val meldebild: String,
    val adresse: String,
    val prioritaet: Int,
    val empfohleneFahrzeuge: Int,
    val empfohleneFaehigkeiten: List<String>,
)

/**
 * Die eigene Maske der Streife — `FeststellungForm.vue`: Was die Besatzung gerade
 * sieht, wird ein Einsatz. Nur Stichworte der eigenen Organisation; der Ort ist
 * vorbelegt mit dem Streifenziel; Koordinaten bleiben leer — der Server nimmt die
 * eigene Position, und die ist genauer als alles, was hier zu tippen wäre.
 */
@Composable
fun Feststellungskasten(
    meins: Rundenfahrzeug,
    katalog: Katalog?,
    beiAnlegen: (Feststellungsmeldung) -> Unit,
) {
    var gewaehlt by remember { mutableStateOf<String?>(null) }
    var meldebild by remember { mutableStateOf("") }
    var ort by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    var gesendet by remember { mutableStateOf(false) }
    var wahlOffen by remember { mutableStateOf(false) }

    val stichworte = remember(katalog, meins.organisation) {
        katalog?.stichworte.orEmpty().filter { it.organisation == meins.organisation }.sortedBy { it.stichwort }
    }
    val vorlage = stichworte.firstOrNull { it.stichwort == gewaehlt }
    val meldebilder = vorlage?.meldebilder.orEmpty()
    val ortVorschlag = meins.streifenziel.orEmpty()
    val bereit = vorlage != null && (ort.isNotBlank() || ortVorschlag.isNotBlank())

    LaunchedEffect(gesendet) {
        if (gesendet) {
            kotlinx.coroutines.delay(3_000)
            gesendet = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        Etikett("Eigene Feststellung")
        Leise("Was ihr hier seht, wird ein Einsatz. Einer je Streifenabschnitt — fahrt weiter und meldet den nächsten von dort.")
        Etikett("Stichwort")
        Knopf(
            vorlage?.let { "${it.stichwort} — ${it.stichwortText}" } ?: "— wählen —",
            { wahlOffen = true },
            art = Knopfart.Leise,
            breit = true,
        )
        Etikett("Wo")
        Feld(wert = ort, beiAenderung = { ort = it.take(120) }, platzhalter = ortVorschlag.ifBlank { "Straße, Objekt, Kreuzung …" })
        Etikett("Was ihr seht")
        Feld(
            wert = meldebild,
            beiAenderung = { meldebild = it.take(300) },
            platzhalter = meldebilder.firstOrNull() ?: "Kurz, was los ist",
            einzeilig = false,
            weiterTaste = ImeAction.Default,
        )
        if (meldebilder.isNotEmpty()) {
            Pillenreihe { meldebilder.forEach { b -> Pille(b, an = meldebild == b, beiDruck = { meldebild = b }) } }
        }
        fehler?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
        if (fehler == null && gesendet) Text("Einsatz angelegt.", style = Schrift.Klein, color = Farben.GruenHell)
        Knopf(
            "Streifeneinsatz anlegen",
            {
                val v = vorlage
                if (v != null) {
                    val adresse = ort.trim().ifBlank { ortVorschlag }.trim()
                    if (adresse.isEmpty()) {
                        fehler = "Wo habt ihr es gesehen?"
                    } else {
                        fehler = null
                        beiAnlegen(
                            Feststellungsmeldung(
                                stichwort = v.stichwort,
                                stichwortText = v.stichwortText,
                                meldebild = meldebild.trim().ifBlank { meldebilder.firstOrNull() ?: v.stichwortText },
                                adresse = adresse,
                                prioritaet = v.prioritaet,
                                empfohleneFahrzeuge = v.empfohleneFahrzeuge,
                                empfohleneFaehigkeiten = v.empfohleneFaehigkeiten,
                            ),
                        )
                        gewaehlt = null
                        meldebild = ""
                        ort = ""
                        gesendet = true
                    }
                }
            },
            art = Knopfart.Haupt,
            aktiv = bereit,
            breit = true,
        )
    }

    if (wahlOffen) {
        Wahlblende(
            titel = "Stichwort",
            gruppen = listOf(null to stichworte),
            aufschrift = { "${it.stichwort} — ${it.stichwortText}" },
            gewaehlt = vorlage,
            beiWahl = {
                gewaehlt = it.stichwort
                wahlOffen = false
            },
            beiSchliessen = { wahlOffen = false },
            suchbar = stichworte.size > 8,
        )
    }
}
