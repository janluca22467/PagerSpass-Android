package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.mobil.Begleiterstand
import de.pagerspass.pagerspass.mobil.Nebenleitung
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.mobil.Tonwahl
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Einzelruf
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay

/**
 * Der Funk des Fahrzeugs — Funkverkehr, Einsatzstelle und das Handfunkgerät.
 *
 * Übertragen aus dem Funkbereich von `FahrzeugView.vue` mit `Kanalzeile.vue`,
 * `FunkChat.vue`, `EinsatzstellenChat.vue` und `FunkgeraetVoll.vue`. Die
 * Einsatzstelle schaltet mit Status 4 an der Lage frei — vorher steht ihr Reiter
 * gesperrt da und sagt warum.
 */

/** Die Griffe des Funks — Sprechtasten, Zeilen, Gerät. */
class Funkgriffe(
    val funk: (String) -> Unit = {},
    val einsatzstelle: (String) -> Unit = {},
    val sprechstart: () -> Unit = {},
    val sprechende: () -> Unit = {},
    val einsatzstelleSprechstart: () -> Unit = {},
    val einsatzstelleSprechende: () -> Unit = {},
    val notruf: () -> Unit = {},
    /** Fahrzeug, Gruppe — `null` ist die Stammgruppe. */
    val funkgruppe: (String, String?) -> Unit = { _, _ -> },
    val einzelrufStarten: (String?) -> Unit = {},
    val einzelrufAnnehmen: (String) -> Unit = {},
    val einzelrufAbweisen: (String) -> Unit = {},
    val einzelrufBeenden: (String) -> Unit = {},
)

/** Ob der Einsatzstellenfunk offensteht — dieselbe Grenze wie am Server. */
fun einsatzstelleFrei(meins: Rundenfahrzeug, einsatz: Einsatz?): Boolean =
    meins.status == 4 && meins.einsatzstelleErreicht && einsatz != null

/**
 * Der Funk-Teil der Seite.
 *
 * @param mitGeraet Ob der Reiter „Handfunkgerät" dasteht — bei der Bauart „Im Funk"
 *   liegt das Gerät schon im ersten Reiter der Seite.
 */
@Composable
fun ColumnScope.FahrzeugFunkteil(
    stand: Rundenstand,
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    einsatz: Einsatz?,
    mitGeraet: Boolean,
    geraet: @Composable () -> Unit,
    griffe: Funkgriffe,
) {
    val frei = einsatzstelleFrei(meins, einsatz)
    var leitung by remember { mutableStateOf("funk") }
    if (!frei && leitung == "einsatzstelle") leitung = "funk"
    if (!mitGeraet && leitung == "geraet") leitung = "funk"

    Reiterreihe {
        Reiter("Funkverkehr", offen = leitung == "funk", beiDruck = { leitung = "funk" })
        Reiter(
            if (frei) "Einsatzstelle" else "Einsatzstelle · S4",
            offen = leitung == "einsatzstelle",
            beiDruck = { if (frei) leitung = "einsatzstelle" },
        )
        if (mitGeraet) Reiter("Handfunkgerät", offen = leitung == "geraet", beiDruck = { leitung = "geraet" })
    }

    when (leitung) {
        "einsatzstelle" -> {
            val sprecher = stand.einsatzstellenSprecher
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SehrLeise("Nur eingetroffene Kräfte dieser Lage lesen mit.", modifier = Modifier.weight(1f))
                if (sprecher != null) {
                    Text("● ${sprecher.name} spricht", style = Schrift.MonoKlein, color = Farben.AmberHell)
                } else {
                    Text("STATUS 4", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                }
            }
            Einsatzstellenfaden(zeilen = raum.einsatzstellenchat, beiSenden = griffe.einsatzstelle)
            // Die eigene Sprechtaste der Einsatzstelle — eine dritte Halbduplex-Leitung.
            Sprechtaste(
                sendet = stand.nebenleitung == Nebenleitung.Einsatzstelle,
                wirdVerstanden = false,
                belegtVon = sprecher?.takeIf { it.playerId != stand.eigeneKennung }?.name,
                gesperrtBis = null,
                beiDruck = griffe.einsatzstelleSprechstart,
                beiLoslassen = griffe.einsatzstelleSprechende,
            )
        }
        "geraet" -> geraet()
        else -> {
            Kanalzeile(stand, raum, meins)
            Funkprotokoll(
                zeilen = stand.funk,
                eigenerRufname = meins.funkrufname,
                laeuft = stand.laeuft,
                beiSenden = { griffe.funk(it) },
            )
            stand.funkhinweis?.let { SehrLeise(it) }
            Sprechtaste(
                sendet = stand.sendet,
                wirdVerstanden = stand.wirdVerstanden,
                belegtVon = stand.sprecher[raum.sendegruppe ?: meins.funkgruppe ?: ""],
                gesperrtBis = stand.funkGesperrtBis,
                beiDruck = griffe.sprechstart,
                beiLoslassen = griffe.sprechende,
            )
        }
    }
}

/**
 * Die Kanalzeile — `Kanalzeile.vue`: Du sendest, jemand spricht oder Kanal frei, und
 * bei getrennten Funkgruppen, auf welcher gesendet wird.
 */
@Composable
private fun Kanalzeile(stand: Rundenstand, raum: Raumzustand, meins: Rundenfahrzeug) {
    val gruppe = raum.sendegruppe ?: meins.funkgruppe
    val belegt = stand.sprecher[gruppe ?: ""]
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val farbe = when {
            stand.sendet -> Farben.SignalHell
            belegt != null -> Farben.AmberHell
            else -> Farben.GruenHell
        }
        Box(Modifier.size(9.dp).background(farbe, CircleShape))
        Text(
            when {
                stand.sendet -> "Du sendest"
                belegt != null -> "$belegt spricht"
                else -> "Kanal frei"
            },
            style = Schrift.MonoKlein,
            color = farbe,
            modifier = Modifier.weight(1f),
        )
        if (raum.settings.funkgruppen.isNotEmpty()) {
            val name = raum.settings.funkgruppen.firstOrNull { it.id == gruppe }?.bezeichnung ?: "Kreiskanal"
            Text("auf $name", style = Schrift.MonoKlein, color = Farben.TextLeise, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------- Einzelruf

/** Der Einzelruf, der bei mir klingelt. */
fun eingehenderEinzelruf(raum: Raumzustand?, ich: String): Einzelruf? =
    raum?.einzelrufe?.firstOrNull { it.klingelt && it.zielPlayerId == ich && ich.isNotEmpty() }

/** Mein eigener Rufversuch, solange die Gegenstelle noch nicht abgenommen hat. */
fun ausgehenderEinzelruf(raum: Raumzustand?, ich: String): Einzelruf? =
    raum?.einzelrufe?.firstOrNull { it.klingelt && it.vonPlayerId == ich && ich.isNotEmpty() }

/** Das laufende Gespräch, an dem dieser Platz beteiligt ist. */
fun laufenderEinzelruf(raum: Raumzustand?, ich: String): Einzelruf? =
    raum?.einzelrufe?.firstOrNull {
        it.laeuft && ich.isNotEmpty() && (it.vonPlayerId == ich || it.angenommenVonPlayerId == ich)
    }

/**
 * Der Rundenstand, übersetzt in die Sprache der Einzelrufleiste des Begleiters —
 * damit die eine Leiste an beiden Stellen dieselbe ist (`EinzelrufLeiste.vue`).
 * Ist ein Handy als Begleiter gekoppelt, führt das Handy den Einzelruf.
 */
fun einzelrufstand(stand: Rundenstand, tonwahl: Tonwahl): Begleiterstand = Begleiterstand(
    gekoppelt = true,
    raum = stand.raum,
    spielerId = stand.eigeneKennung,
    istFunkplatz = !stand.begleiterGekoppelt,
    // Die rastende Taste gibt es nur im Gespräch mit einer Bot-Besatzung — mit einem
    // Menschen ist das Mikrofon ohnehin offen (`einzelrufMikrofon`).
    einzelrufHoert = stand.nebenleitung == Nebenleitung.Einzelruf && stand.eigenerEinzelruf?.mitBot == true,
    einzelrufErkennung = stand.einzelrufWirdVerstanden,
    tonFunk = tonwahl.funk,
    stumm = tonwahl.stumm,
)

// ------------------------------------------------------------ Handfunkgerät

/** Die Farbe des Displays — grün hinterleuchtet, wie ein HRT. */
private val DISPLAY = Color(0xFF9CC28A)
private val DISPLAYSCHRIFT = Color(0xFF14240E)

/**
 * Das Handfunkgerät — `FunkgeraetVoll.vue` (HRT-90).
 *
 * Display mit der geschalteten Gruppe, Sprechaufforderung und Gesprächspartner;
 * Steuerkreuz mit Menü (Rufgruppen, Einzelruf, letzte Rufe, Lautstärke), grüne und
 * rote Hörertaste, Ziffernfeld — <b>kurz</b> ist eine Ziffer (Gruppennummer wählen),
 * <b>gehalten</b> ein Status —, die Notruftaste (gehalten) und die Sprechtaste.
 * Bei der Bauart „Im Funk" steht hier auch der Alarm.
 */
@Composable
fun Handfunkgeraet(
    stand: Rundenstand,
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    einsatz: Einsatz?,
    alarmzeilen: List<String>?,
    jetzt: Long,
    tonwahl: Tonwahl,
    beiTaste: () -> Unit,
    beiQuittieren: () -> Unit,
    beiFms: (Int, String?, Int?) -> Unit,
    griffe: Funkgriffe,
) {
    val ich = stand.eigeneKennung
    val eingehend = eingehenderEinzelruf(raum, ich)
    val ausgehend = ausgehenderEinzelruf(raum, ich)
    val laufend = laufenderEinzelruf(raum, ich)
    val einzelrufHier = !stand.begleiterGekoppelt
    val gruppe = meins.funkgruppe
    val eigeneGruppe = raum.settings.funkgruppen.firstOrNull { it.id == gruppe }
    val waehlbar = raum.settings.funkgruppen.filter { it.einsatzId == null || it.einsatzId == meins.einsatzId }
    val sprecher = stand.sprecher[gruppe ?: ""]

    var seite by remember { mutableStateOf("zu") }
    var index by remember { mutableStateOf(0) }
    var eingabe by remember { mutableStateOf("") }
    var stern by remember { mutableStateOf(false) }
    var hinweis by remember { mutableStateOf("") }
    var notrufAb by remember { mutableStateOf(false) }

    LaunchedEffect(hinweis) {
        if (hinweis.isNotEmpty()) {
            delay(2_200)
            hinweis = ""
        }
    }
    LaunchedEffect(notrufAb) {
        if (notrufAb) {
            delay(30_000)
            notrufAb = false
        }
    }

    val letzteRufe = stand.funk.reversed()
        .filter { it.kind == "Funk" && it.von.isNotBlank() && it.von != meins.funkrufname }
        .filter { it.funkgruppe == null || it.funkgruppe == gruppe }
        .map { it.von }
        .distinct()
        .take(6)
    val ziele: List<Pair<String?, String>> = listOf<Pair<String?, String>>(null to "Leitstelle") +
        raum.vehicles.filter { v -> v.playerId != null && v.playerId != ich && raum.players.any { it.id == v.playerId } }
            .map { it.id to it.funkrufname }
    val lautstufen = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

    val eintraege: List<Pair<String, String>> = when (seite) {
        "wurzel" -> listOfNotNull(
            "gruppen" to "Rufgruppen" + (eigeneGruppe?.nummer?.let { "  $it" } ?: ""),
            if (einzelrufHier) "einzelruf" to "Einzelruf" else null,
            "rufe" to "Letzte Rufe",
            "lautstaerke" to "Lautstärke  ${(tonwahl.funk * 100).toInt()} %",
        )
        "einzelruf" -> ziele.map { (it.first ?: "ls") to it.second }
        "gruppen" -> {
            val zeilen = waehlbar.map { it.id to (it.bezeichnung + if (gruppe == it.id) "  ●" else "  ${it.nummer}") }
            if (raum.settings.funkgruppen.isEmpty()) zeilen else listOf("stamm" to ("Stammgruppe" + if (!meins.funkgruppeAufgeschaltet) "  ●" else "")) + zeilen
        }
        "rufe" -> if (letzteRufe.isEmpty()) listOf("leer" to "Noch nichts gehört") else letzteRufe.mapIndexed { i, n -> "r$i" to n }
        "lautstaerke" -> lautstufen.map { s -> s.toString() to ("${(s * 100).toInt()} %" + if (kotlin.math.abs(tonwahl.funk - s) < 0.01f) "  ●" else "") }
        else -> emptyList()
    }
    val titel = when (seite) {
        "einzelruf" -> "Einzelruf wählen"
        "gruppen" -> "Rufgruppen"
        "rufe" -> "Letzte Rufe"
        "lautstaerke" -> "Lautstärke"
        else -> "Menü"
    }

    fun oeffnen(ziel: String) {
        seite = ziel
        index = 0
    }

    fun waehlen() {
        val (id, _) = eintraege.getOrNull(index) ?: return
        when (seite) {
            "wurzel" -> oeffnen(id)
            "einzelruf" -> {
                griffe.einzelrufStarten(if (id == "ls") null else id)
                seite = "zu"
            }
            "gruppen" -> {
                griffe.funkgruppe(meins.id, if (id == "stamm") null else id)
                seite = "zu"
            }
            "lautstaerke" -> id.toFloatOrNull()?.let { tonwahl.funkSetzen(it) }
            else -> seite = "zu"
        }
    }

    fun taste(z: String) {
        if (stern && z != "#" && z != "*") {
            stern = false
            z.toIntOrNull()?.let { beiFms(it, null, null) }
            return
        }
        beiTaste()
        when (z) {
            "#" -> if (einzelrufHier) oeffnen("einzelruf")
            "*" -> stern = true
            else -> if (eingabe.length < 8) eingabe += z
        }
    }

    fun gehalten(z: String) {
        val status = z.toIntOrNull() ?: return taste(z)
        if (status == 0 || !fmsErlaubt(meins, einsatz, status, jetzt)) return taste(z)
        beiTaste()
        if (status == 6) {
            // Status 6 fragt am Bedienteil nach dem Grund — am Gerät geht er ohne Angabe.
            beiFms(6, null, null)
        } else {
            beiFms(status, null, null)
        }
    }

    fun gruen() {
        beiTaste()
        when {
            eingehend != null && einzelrufHier -> griffe.einzelrufAnnehmen(eingehend.id)
            seite != "zu" -> waehlen()
            eingabe.isNotEmpty() -> {
                val g = raum.settings.funkgruppen.firstOrNull { it.nummer == eingabe }
                if (g == null) hinweis = "KEIN EINTRAG" else griffe.funkgruppe(meins.id, g.id)
                eingabe = ""
            }
        }
    }

    fun rot() {
        beiTaste()
        when {
            laufend != null && einzelrufHier -> griffe.einzelrufBeenden(laufend.id)
            eingehend != null && einzelrufHier -> griffe.einzelrufAbweisen(eingehend.id)
            ausgehend != null && einzelrufHier -> griffe.einzelrufBeenden(ausgehend.id)
            seite != "zu" -> {
                seite = if (seite == "wurzel") "zu" else "wurzel"
                index = 0
            }
            else -> {
                stern = false
                eingabe = ""
            }
        }
    }

    fun gruppeBlaettern(richtung: Int) {
        beiTaste()
        if (waehlbar.isEmpty()) return
        val aktuell = waehlbar.indexOfFirst { it.id == gruppe }
        val naechste = waehlbar[(aktuell + richtung + waehlbar.size) % waehlbar.size]
        griffe.funkgruppe(meins.id, naechste.id)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2C3036), Color(0xFF16181C))))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("HRT-90", style = Schrift.MonoKlein.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.5f), modifier = Modifier.weight(1f))
            // Die Notruftaste — gedrückt halten.
            Haltetaste(
                aufschrift = if (notrufAb) "NOTRUF AB" else "NOTRUF",
                farbe = Color(0xFFE08A00),
                beiKurz = { hinweis = "NOTRUF: HALTEN" },
                beiLang = {
                    griffe.notruf()
                    notrufAb = true
                },
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .background(DISPLAY, RoundedCornerShape(6.dp))
                .border(2.dp, Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(Abstand.Klein),
        ) {
            val kopf = when {
                alarmzeilen != null -> "Alarm"
                laufend != null -> "Einzelruf"
                seite != "zu" -> titel
                else -> "Gruppenruf"
            }
            Text(kopf, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = DISPLAYSCHRIFT)
            when {
                alarmzeilen != null -> alarmzeilen.forEach { Text(it, style = Schrift.MonoKlein.copy(fontSize = 12.sp), color = DISPLAYSCHRIFT, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                laufend != null -> {
                    val gegen = if (laufend.vonPlayerId == ich) laufend.zielName else laufend.vonName
                    val beginn = zeitMillis(laufend.angenommenUm) ?: jetzt
                    val sek = ((jetzt - beginn) / 1000).coerceAtLeast(0)
                    Text(gegen, style = Schrift.MonoNormal.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = DISPLAYSCHRIFT)
                    Text("${sek / 60}:${(sek % 60).toString().padStart(2, '0')}", style = Schrift.MonoKlein, color = DISPLAYSCHRIFT)
                }
                seite != "zu" -> {
                    val start = (index - 4).coerceAtLeast(0)
                    eintraege.drop(start).take(5).forEachIndexed { i, (_, name) ->
                        val gewaehlt = start + i == index
                        Text(
                            name,
                            style = Schrift.MonoKlein.copy(fontSize = 12.sp),
                            color = if (gewaehlt) DISPLAY else DISPLAYSCHRIFT,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().background(if (gewaehlt) DISPLAYSCHRIFT else Color.Transparent).padding(horizontal = 2.dp),
                        )
                    }
                }
                else -> {
                    Text(eigeneGruppe?.bezeichnung ?: "Kreiskanal", style = Schrift.MonoKlein, color = DISPLAYSCHRIFT, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (sprechaufforderungSteht(meins, jetzt)) {
                        Text("J · LST BITTET UMS WORT", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = DISPLAYSCHRIFT)
                    }
                    val gross = when {
                        stand.sendet -> "SPRECHEN"
                        hinweis.isNotEmpty() -> hinweis
                        eingabe.isNotEmpty() -> eingabe
                        ausgehend != null -> "${ausgehend.zielName} …"
                        sprecher != null -> sprecher
                        else -> "— — —"
                    }
                    Text(gross, style = Schrift.MonoNormal.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = DISPLAYSCHRIFT, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (stern) "* Status …" else meins.funkrufname, style = Schrift.MonoKlein, color = DISPLAYSCHRIFT)
                }
            }
        }

        if (alarmzeilen != null) {
            Geraetetaste("QUITTIEREN", Modifier.fillMaxWidth(), farbe = Farben.Signal) {
                beiTaste()
                beiQuittieren()
            }
        }

        // Grün, Steuerkreuz, Rot.
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Geraetetaste("✆", Modifier.weight(1f), farbe = Color(0xFF2F9E44)) { gruen() }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(2f)) {
                Geraetetaste("▲", Modifier.fillMaxWidth(0.4f)) {
                    beiTaste()
                    if (seite != "zu" && eintraege.isNotEmpty()) index = (index - 1 + eintraege.size) % eintraege.size
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Geraetetaste("◀", Modifier.weight(1f)) { gruppeBlaettern(-1) }
                    Geraetetaste("≡", Modifier.weight(1f)) {
                        beiTaste()
                        if (seite != "zu") waehlen() else oeffnen("wurzel")
                    }
                    Geraetetaste("▶", Modifier.weight(1f)) { gruppeBlaettern(1) }
                }
                Geraetetaste("▼", Modifier.fillMaxWidth(0.4f)) {
                    beiTaste()
                    if (seite != "zu" && eintraege.isNotEmpty()) index = (index + 1) % eintraege.size
                }
            }
            Geraetetaste("✆", Modifier.weight(1f), farbe = Farben.Signal) { rot() }
        }

        // Das Ziffernfeld — kurz eine Ziffer, gehalten ein Status.
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#")).forEach { reihe ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
                reihe.forEach { z ->
                    Haltetaste(
                        aufschrift = z,
                        farbe = if (z == "*" && stern) Farben.AmberTief else Color(0xFF3A3E46),
                        modifier = Modifier.weight(1f),
                        beiKurz = { taste(z) },
                        beiLang = { if (z == "*" || z == "#") taste(z) else gehalten(z) },
                    )
                }
            }
        }
        SehrLeise("Kurz: Ziffer · Halten: Status melden", mono = true)

        Sprechtaste(
            sendet = stand.sendet,
            wirdVerstanden = stand.wirdVerstanden,
            belegtVon = sprecher,
            gesperrtBis = stand.funkGesperrtBis,
            beiDruck = griffe.sprechstart,
            beiLoslassen = griffe.sprechende,
        )
        stand.funkhinweis?.let { Leise(it) }
    }
}

/** Eine Taste am Gerät. */
@Composable
private fun Geraetetaste(aufschrift: String, modifier: Modifier = Modifier, farbe: Color = Color(0xFF3A3E46), beiDruck: () -> Unit) {
    Haltetaste(aufschrift, farbe, modifier, beiKurz = beiDruck, beiLang = beiDruck)
}

/** Eine Taste, die kurz etwas anderes tut als gehalten — `useHaltetasten`. */
@Composable
private fun Haltetaste(
    aufschrift: String,
    farbe: Color,
    modifier: Modifier = Modifier,
    beiKurz: () -> Unit,
    beiLang: () -> Unit,
) {
    val kurz by rememberUpdatedState(beiKurz)
    val lang by rememberUpdatedState(beiLang)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(farbe.copy(alpha = 0.95f), farbe.copy(alpha = 0.7f))))
            .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { kurz() },
                    onLongPress = { lang() },
                )
            }
            .padding(horizontal = Abstand.Klein),
    ) {
        Text(aufschrift, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Color.White, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** Ein Rahmen für den Einzelruf am unteren Rand — dieselbe Leiste wie am Begleiter. */
@Composable
fun FahrzeugEinzelruf(
    stand: Rundenstand,
    tonwahl: Tonwahl,
    griffe: BegleiterGriffe,
    beiMikrofon: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val b = einzelrufstand(stand, tonwahl)
    // Vollduplex zwischen zwei Menschen: Das Mikrofon geht mit der Annahme auf und
    // mit dem Auflegen zu — derselbe Griff wie am Leitstellentisch.
    val mikrofon by rememberUpdatedState(beiMikrofon)
    val ruf = b.laufenderEinzelruf?.takeIf { b.einzelrufHier }
    val menschlich = ruf != null && !ruf.mitBot
    LaunchedEffect(ruf?.id, menschlich) { mikrofon(menschlich) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { mikrofon(false) } }
    if (!b.einzelrufHier) return
    if (b.laufenderEinzelruf == null && b.eingehenderEinzelruf == null && b.ausgehenderEinzelruf == null) return
    Box(modifier = modifier.fillMaxWidth().padding(Abstand.Normal), contentAlignment = Alignment.Center) {
        Einzelrufleiste(stand = b, griffe = griffe)
    }
}

/** Eine Fläche wie die Kästen der Seite — für das Gerät im eigenen Reiter. */
@Composable
fun Geraetefeld(inhalt: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().flaeche(farbe = Farben.BgTief, ecke = 14.dp).padding(Abstand.Normal),
        content = inhalt,
    )
}
