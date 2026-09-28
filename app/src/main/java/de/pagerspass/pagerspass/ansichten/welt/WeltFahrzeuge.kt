package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.mobil.weltzeit
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltLehrgang
import de.pagerspass.pagerspass.netz.WeltStreifenstation
import de.pagerspass.pagerspass.netz.WeltWerkstattwahl
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Was die Fahrzeugblende über ihr eigenes Schließen hinaus behält.
 *
 * <b>Am Handy klappt die Seite zu, während auf der Karte gezeichnet wird</b> — und kommt
 * mit dem nächsten Punkt wieder. Läge der Entwurf in der Seite, wäre er beim
 * Wiederkommen weg (dieselben Modulgrößen wie `<script>` oben in `FahrzeugBlende.vue`).
 */
private object Fahrzeugsitzung {
    var gewaehlt by mutableStateOf<String?>(null)
    var zeichnet by mutableStateOf(false)
    var pfadFuer by mutableStateOf<String?>(null)
    var pfadEntwurf by mutableStateOf<List<WeltStreifenstation>>(emptyList())
}

/** Wo ein Fahrzeug in seinem Ablauf steht — als Wort. */
private val LAGENTEXT = mapOf(
    "Wache" to "auf Wache",
    "Ausrueckt" to "rückt aus",
    "Anfahrt" to "auf Anfahrt",
    "VorOrt" to "vor Ort",
    "Bereitstellung" to "im Bereitstellungsraum",
    "Streife" to "auf Streife",
    "Rueckfahrt" to "rückt ein",
)

/** Die Reihenfolge der Liste: wer arbeitet, oben; wer steht, unten. */
private val LAGENRANG = mapOf(
    "VorOrt" to 0,
    "Anfahrt" to 1,
    "Ausrueckt" to 2,
    "Bereitstellung" to 3,
    "Rueckfahrt" to 4,
    "Streife" to 5,
    "Wache" to 6,
)

/**
 * Die Fahrzeuge — übertragen aus `components/welt/FahrzeugBlende.vue`.
 *
 * Eine Zeile je Fahrzeug mit Status, Riss, Tätigkeit und Restzeit; aufgeklappt alles,
 * was man mit ihm tun kann: umbenennen, auf einen Einsatz schicken, die Anfahrt
 * abbrechen, Streife setzen, umsetzen, zum Lehrgang, in die Werkstatt, verkaufen — und
 * geliehene Fahrzeuge zurückgeben.
 */
@Composable
fun WeltFahrzeugBlende(welt: Welt, stand: Weltzustand, katalog: Katalog?, modi: Kartenmodi, griffe: Weltgriffe) {
    val bereich = rememberCoroutineScope()
    val vorlagen = remember(katalog) { katalog?.fahrzeuge.orEmpty().associateBy { it.id } }
    val packbilder by welt.packbilder.collectAsState()

    val jetzt by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(500)
            value = System.currentTimeMillis()
        }
    }
    val serverJetzt = jetzt + welt.fahrt.versatzMs

    var suche by remember { mutableStateOf("") }
    var zielWache by remember { mutableStateOf<String?>(null) }
    var zielLage by remember { mutableStateOf<String?>(null) }
    var verkaufGefragt by remember { mutableStateOf(false) }
    var rueckgabeGefragt by remember { mutableStateOf(false) }
    var sendet by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var rufname by remember { mutableStateOf("") }
    var streifenorte by remember { mutableStateOf<List<WeltStreifenstation>>(emptyList()) }
    var streifeFehler by remember { mutableStateOf<String?>(null) }
    var streifeSendet by remember { mutableStateOf(false) }
    var routenName by remember { mutableStateOf("") }
    var gewaehlteRoute by remember { mutableStateOf<String?>(null) }
    var lehrgaenge by remember { mutableStateOf<List<WeltLehrgang>>(emptyList()) }
    var lehrgang by remember { mutableStateOf<String?>(null) }
    var lehrgangWache by remember { mutableStateOf<String?>(null) }
    var werkstattwahl by remember { mutableStateOf<WeltWerkstattwahl?>(null) }
    var werkstattZiel by remember { mutableStateOf<String?>(null) }

    val sitzung = Fahrzeugsitzung
    val gewaehlt = sitzung.gewaehlt

    // Während gezeichnet wird, ist der Pfad der Karte der Entwurf — der Arbeitsplatz hängt
    // jeden Tipp an. Höchstens acht Stationen: Was darüber hinausgeht, fällt wieder weg.
    val entwurf = if (sitzung.zeichnet) modi.pfad else sitzung.pfadEntwurf
    LaunchedEffect(modi.pfad, sitzung.zeichnet) {
        if (!sitzung.zeichnet) return@LaunchedEffect
        if (modi.pfad.size > 8) {
            streifeFehler = "Ein Streifenpfad hat höchstens acht Stationen."
            griffe.pfadSetzen(modi.pfad.take(8))
        }
        sitzung.pfadEntwurf = modi.pfad.take(8)
    }

    fun entwurfSetzen(neu: List<WeltStreifenstation>) {
        sitzung.pfadEntwurf = neu
        if (sitzung.zeichnet) griffe.pfadSetzen(neu)
    }

    fun wachenort(f: WeltFahrzeug): Pair<Double, Double>? =
        stand.stand?.wachen?.firstOrNull { it.id == f.wacheId }?.let { it.lat to it.lon }

    fun streifenorteLaden() {
        if (streifenorte.isNotEmpty()) return
        bereich.launch {
            welt.holen("") { welt.wege.streifenorte(it) }.onSuccess { streifenorte = it }
        }
    }

    fun lehrgaengeLaden(f: WeltFahrzeug) {
        lehrgaenge = emptyList()
        bereich.launch {
            welt.holen("") { welt.wege.lehrgaenge(it, f.id) }.onSuccess {
                if (sitzung.gewaehlt == f.id) lehrgaenge = it
            }
        }
    }

    fun werkstattLaden(f: WeltFahrzeug) {
        werkstattwahl = null
        werkstattZiel = null
        bereich.launch {
            welt.holen("") { welt.wege.werkstattwahl(it, f.id) }.onSuccess {
                if (sitzung.gewaehlt == f.id) werkstattwahl = it
            }
        }
    }

    fun zeileUmschalten(f: WeltFahrzeug) {
        sitzung.gewaehlt = if (sitzung.gewaehlt == f.id) null else f.id
        val offen = sitzung.gewaehlt != null
        zielWache = null
        zielLage = null
        lehrgang = null
        lehrgangWache = null
        lehrgaenge = emptyList()
        verkaufGefragt = false
        rueckgabeGefragt = false
        fehler = null
        rufname = if (offen) f.funkrufname else ""
        streifeFehler = null
        sitzung.pfadEntwurf = if (offen) f.streifenpfad else emptyList()
        if (offen && f.streifenfaehig) streifenorteLaden()
        if (sitzung.zeichnet) {
            sitzung.zeichnet = false
            sitzung.pfadFuer = null
            griffe.pfadModus(false, null)
        }
        if (offen) {
            lehrgaengeLaden(f)
            if (f.lage == "Wache" && !f.geliehen) werkstattLaden(f)
        }
    }

    // Zurück aus dem Zeichnen auf der Karte: Die Zeile des Fahrzeugs steht wieder offen,
    // und was zum Fahrzeug gehört, wird nachgeholt. Den Entwurf behält die Sitzung.
    LaunchedEffect(Unit) {
        val id = sitzung.pfadFuer
        if (!sitzung.zeichnet || id == null) return@LaunchedEffect
        val f = stand.betrieb?.fahrzeuge?.firstOrNull { it.id == id }
        if (f == null) {
            sitzung.zeichnet = false
            sitzung.pfadFuer = null
            griffe.pfadModus(false, null)
            return@LaunchedEffect
        }
        sitzung.gewaehlt = f.id
        rufname = f.funkrufname
        if (f.streifenfaehig) streifenorteLaden()
        lehrgaengeLaden(f)
    }

    fun zeichnenUmschalten(f: WeltFahrzeug) {
        sitzung.zeichnet = !sitzung.zeichnet
        sitzung.pfadFuer = if (sitzung.zeichnet) f.id else null
        if (sitzung.zeichnet) {
            griffe.pfadModus(true, wachenort(f))
            griffe.pfadSetzen(sitzung.pfadEntwurf)
            // Am Handy liegt die Seite über der Karte — sie klappt zu, damit man tippen kann.
            griffe.karteFreigeben()
        } else {
            griffe.pfadModus(false, null)
        }
    }

    fun zeichnenBeenden() {
        sitzung.zeichnet = false
        sitzung.pfadFuer = null
        griffe.pfadModus(false, null)
    }

    fun ortUmschalten(ort: WeltStreifenstation) {
        streifeFehler = null
        val liste = entwurf
        if (liste.any { it.name == ort.name }) {
            entwurfSetzen(liste.filter { it.name != ort.name })
            return
        }
        if (liste.size >= 8) {
            streifeFehler = "Ein Streifenpfad hat höchstens acht Stationen."
            return
        }
        entwurfSetzen(liste + ort)
    }

    fun stationSchieben(index: Int, richtung: Int) {
        val ziel = index + richtung
        val kopie = entwurf.toMutableList()
        if (ziel < 0 || ziel >= kopie.size) return
        val a = kopie[index]
        kopie[index] = kopie[ziel]
        kopie[ziel] = a
        entwurfSetzen(kopie)
    }

    /** Ein Aufruf mit Sperre, Fehler und Nachladen — die Form aller Knöpfe hier. */
    fun tun(standard: String, danachZu: Boolean = true, nachher: Welt.Nachladen = Welt.Nachladen.Betrieb, was: suspend (String) -> Unit) {
        if (sendet) return
        sendet = true
        fehler = null
        bereich.launch {
            fehler = welt.aktion(standard, nachher, was)
            sendet = false
            if (fehler == null && danachZu) sitzung.gewaehlt = null
        }
    }

    fun rest(ms: Long): String {
        if (ms <= 0) return "gleich fertig"
        val min = ceil(ms / 60_000.0).toInt()
        return if (min >= 60) "noch ${min / 60} h ${min % 60} min" else "noch $min min"
    }

    fun restzeile(f: WeltFahrzeug): String? {
        weltzeit(f.lehrgangFertigUm)?.let { return rest(it - serverJetzt) }
        weltzeit(f.werkstattFertigUm)?.let { return rest(it - serverJetzt) }
        if (f.lage != "Wache") {
            welt.fahrt.restMinuten(f.ankunftUm)?.let { min ->
                return if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
            }
        }
        if (f.geliehen) {
            val bis = weltzeit(f.geliehenBis) ?: return null
            val ms = bis - serverJetzt
            if (ms <= 0) return "fährt heim"
            val stunden = ceil(ms / 3_600_000.0).toInt()
            if (stunden < 24) return "noch $stunden h"
            val tage = ceil(stunden / 24.0).toInt()
            return "noch $tage ${if (tage == 1) "Tag" else "Tage"}"
        }
        return null
    }

    fun taetigkeit(f: WeltFahrzeug): String = when {
        f.lehrgang != null -> if (f.lehrgangFertigUm != null) "im Lehrgang" else "fährt zum Lehrgang"
        f.inWerkstatt -> if (f.werkstattFertigUm != null) "in der Werkstatt" else "fährt in die Werkstatt"
        f.zielWache != null -> "zieht um"
        welt.fahrt.ruecktAus(f.lage, f.losUm) -> "rückt aus"
        else -> LAGENTEXT[f.lage] ?: f.lage
    }

    fun passtZurSuche(f: WeltFahrzeug): Boolean {
        val t = suche.trim().lowercase()
        if (t.isEmpty()) return true
        if (f.funkrufname.lowercase().contains(t) || f.typ.lowercase().contains(t)) return true
        val v = vorlagen[f.vorlageId] ?: return false
        return v.typ.lowercase().contains(t) ||
            v.beschreibung.lowercase().contains(t) ||
            v.kategorie.lowercase().contains(t) ||
            v.faehigkeiten.any { it.lowercase().contains(t) }
    }

    val bestand = stand.betrieb?.fahrzeuge.orEmpty().filter { stand.fahrzeugImBereich(it) }
    val fahrzeuge = bestand
        .filter { passtZurSuche(it) }
        .sortedWith(compareBy<WeltFahrzeug> { LAGENRANG[it.lage] ?: 9 }.thenBy { it.funkrufname })
    val einrichtungen = stand.stand?.wachen.orEmpty().filter { it.art == "Lehrgangseinrichtung" }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        Bereichsreiter(stand, beiWahl = { welt.bereichWaehlen(it) })

        Knopf(
            aufschrift = "Fahrzeug kaufen",
            beiDruck = { griffe.oeffnen(Werkzeug.Fahrzeugkauf) },
            kompakt = true,
            zeichenVorn = {
                androidx.compose.material3.Icon(
                    imageVector = Weltzeichen.Fahrzeugkauf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )

        if (bestand.size > 5) {
            Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Rufname, Typ, Fähigkeit …", stil = Schrift.MonoKlein)
        }

        when {
            bestand.isEmpty() -> {
                Leerhinweis("Noch kein Fahrzeug. Erst eine Wache bauen, dann Fahrzeuge kaufen.")
                return@Column
            }
            fahrzeuge.isEmpty() -> {
                Leerhinweis("Kein Fahrzeug passt zu „$suche\".")
                return@Column
            }
        }

        fahrzeuge.forEach { f ->
            val offen = gewaehlt == f.id
            val gelerntes = if (offen) lehrgaenge.filter { it.gelernt }.map { it.faehigkeit } else emptyList()

            // ------------------------------------------------------ Zeile
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(farbe = if (offen) Farben.FlaecheAktiv else Farben.Flaeche, ecke = 9.dp)
                    .drawBehind {
                        drawRect(fmsFarbe(f.status), size = size.copy(width = 3.dp.toPx()))
                    }
                    .clickable { zeileUmschalten(f) }
                    .padding(start = Abstand.Normal, end = Abstand.Klein, top = Abstand.Klein, bottom = Abstand.Klein),
            ) {
                Fmsplakette(f.status)
                // Der Riss liegt quer — so passt er in die Zeile.
                Canvas(modifier = Modifier.size(width = 46.dp, height = 24.dp)) {
                    val plan = rissplan(vorlagen[f.vorlageId], f.typ)
                    fahrzeugmarkeZeichnen(
                        ort = Offset(size.width / 2f, size.height / 2f),
                        kurs = 90.0,
                        plan = plan,
                        blaulicht = false,
                        jetzt = jetzt,
                        bild = packbilder[f.vorlageId],
                        icon = stand.pack[f.vorlageId],
                        hoehe = 40f,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                    Text(
                        text = f.funkrufname,
                        style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val unter = buildString {
                        append(f.typ).append(" · ").append(taetigkeit(f))
                        if (f.lehrgang != null) append(" · ").append(f.lehrgang)
                        else if (f.inWerkstatt && f.werkstattOrt != null) append(" · ").append(f.werkstattOrt)
                        if (f.naechsteLageId != null) append(" · fährt danach weiter")
                    }
                    Text(text = unter, style = Schrift.Winzig, color = Farben.TextLeise, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!f.inWerkstatt && (f.verschlissen || f.werkstattFaellig)) {
                        Text(
                            text = if (f.verschlissen) "verschlissen" else "bald fällig",
                            style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                            color = if (f.verschlissen) Farben.SignalHell else Farben.Amber,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                        if (f.geliehen) Weltmarke("geliehen", Farben.BlauHell)
                        gelerntes.forEach { Weltmarke(it, Farben.FmsFrei) }
                    }
                    if (f.lage != "Wache" && f.lage != "Bereitstellung") {
                        val anteil = welt.fahrt.fortschritt(f.losUm, f.ankunftUm).toFloat()
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Farben.BgTief, Rundung.Rund),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(anteil.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(Farben.Amber, Rundung.Rund),
                            )
                        }
                    }
                }
                restzeile(f)?.let {
                    Text(text = it, style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextLeise)
                }
            }

            if (!offen) return@forEach

            // --------------------------------------------------- Griffe
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Farben.Rand, Rundung.Klein)
                    .padding(Abstand.Normal),
            ) {
                if (f.geliehen) {
                    Text(
                        text = "Geliehen (${restzeile(f) ?: "endet gleich"}) — umbenennen, umsetzen und verkaufen kann nur der Besitzer.",
                        style = Schrift.Winzig,
                        color = Farben.TextSehrLeise,
                    )
                    if (!rueckgabeGefragt) {
                        Knopf("Zurückgeben", { rueckgabeGefragt = true }, art = Knopfart.Leise, kompakt = true, aktiv = !sendet)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf(
                                "Ohne Erstattung zurückgeben",
                                {
                                    tun("Das ging nicht.", danachZu = false) { welt.wege.leiheZurueckgeben(it, f.id) }
                                    rueckgabeGefragt = false
                                },
                                kompakt = true,
                                aktiv = !sendet,
                            )
                            Knopf("Nein", { rueckgabeGefragt = false }, art = Knopfart.Leise, kompakt = true)
                        }
                    }
                }

                // Der Rufname — ein leeres Feld stellt den Namen aus dem Buch wieder her.
                if (!f.geliehen) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                        Feld(
                            wert = rufname,
                            beiAenderung = { rufname = it.take(48) },
                            platzhalter = f.funkrufname,
                            stil = Schrift.MonoKlein,
                            modifier = Modifier.weight(1f),
                        )
                        Knopf(
                            aufschrift = if (rufname.trim().isNotEmpty()) "Übernehmen" else "Wie im Buch",
                            beiDruck = {
                                if (!sendet) {
                                    sendet = true
                                    fehler = null
                                    bereich.launch {
                                        fehler = welt.aktion("Der Rufname ging nicht.", Welt.Nachladen.Beides) {
                                            welt.wege.fahrzeugUmbenennen(it, f.id, rufname.trim())
                                        }
                                        welt.zustand.value.betrieb?.fahrzeuge?.firstOrNull { it.id == f.id }?.let { rufname = it.funkrufname }
                                        sendet = false
                                    }
                                }
                            },
                            kompakt = true,
                            aktiv = !sendet && rufname.trim() != f.funkrufname,
                        )
                    }
                    if (f.rufnameVonHand) {
                        Text(
                            text = "Selbst vergeben — ein leeres Feld stellt den Namen aus dem Buch wieder her.",
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                        )
                    }
                }

                // Auf einen Einsatz schicken — oder vormerken, wenn es noch arbeitet.
                val vormerkbar = stand.istVormerkbar(f)
                val lagen = stand.lagenNah.filter {
                    it.zustand != "Erledigt" && it.id != f.lageId && (vormerkbar || stand.istAlarmierbar(f, it))
                }
                if (stand.istAlarmierbar(f, null) || vormerkbar || lagen.isNotEmpty()) {
                    if (lagen.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Auswahlfeld(
                                platzhalter = if (vormerkbar) "Danach fahren zu …" else "Auf Einsatz schicken …",
                                wahlen = lagen.map { Wahl(it.id, "${it.stichwort} · ${entfernungText(it.entfernungMeter)} · ${deckungstext(it)}") },
                                gewaehlt = zielLage,
                                beiWahl = { zielLage = it },
                                modifier = Modifier.weight(1f),
                            )
                            Knopf(
                                aufschrift = when {
                                    sendet -> "Alarmiert …"
                                    vormerkbar -> "Vormerken"
                                    else -> "Alarmieren"
                                },
                                beiDruck = {
                                    val ziel = zielLage
                                    if (ziel == null) {
                                        fehler = "Wähle den Einsatz."
                                    } else if (!sendet) {
                                        sendet = true
                                        fehler = null
                                        bereich.launch {
                                            fehler = welt.alarmieren(ziel, listOf(f.id))
                                            if (fehler == null) {
                                                zielLage = null
                                                sitzung.gewaehlt = null
                                            }
                                            sendet = false
                                        }
                                    }
                                },
                                kompakt = true,
                                aktiv = !sendet && zielLage != null,
                            )
                        }
                    }
                    Text(
                        text = if (vormerkbar && lagen.isNotEmpty()) {
                            "Es fährt los, sobald die Arbeit hier getan ist."
                        } else if (lagen.isEmpty()) {
                            "Gerade kein offener Einsatz — neue kommen von selbst."
                        } else {
                            ""
                        },
                        style = Schrift.Winzig,
                        color = Farben.TextSehrLeise,
                    )
                }

                if (f.lage == "Ausrueckt" || f.lage == "Anfahrt") {
                    Knopf(
                        "Anfahrt abbrechen",
                        { tun("Der Abbruch ging nicht.") { welt.wege.anfahrtAbbrechen(it, f.id) } },
                        art = Knopfart.Leise,
                        kompakt = true,
                        aktiv = !sendet,
                    )
                }

                // ------------------------------------------------- Streife
                if (f.streifenfaehig) {
                    Text(
                        text = "Streife · " + if (f.streifenpfad.isNotEmpty()) f.streifenpfad.joinToString(" → ") { it.name } else "keine",
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                    )
                    Absage(streifeFehler)
                    if (streifenorte.isEmpty()) {
                        Text(
                            text = "Keine Stationen in der Nähe. Sie kommen aus den Sonderobjekten und Kliniken " +
                                "deines Landkreises und aus deinen eigenen Punkten — setze einen unter „Bauen\", " +
                                "dann steht er hier. Oder tippe die Punkte gleich auf der Karte.",
                            style = Schrift.Klein,
                            color = Farben.TextSehrLeise,
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                            streifenorte.forEach { o ->
                                de.pagerspass.pagerspass.ui.bausteine.Pille(
                                    aufschrift = o.name,
                                    an = entwurf.any { it.name == o.name },
                                    beiDruck = { ortUmschalten(o) },
                                )
                            }
                        }
                    }

                    if (entwurf.isNotEmpty()) {
                        entwurf.forEachIndexed { i, st ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "${i + 1}", style = Schrift.MonoKlein, color = Farben.Eigenposition)
                                Text(text = st.name, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                                Knopf("↑", { stationSchieben(i, -1) }, kompakt = true, aktiv = i > 0)
                                Knopf("↓", { stationSchieben(i, 1) }, kompakt = true, aktiv = i < entwurf.size - 1)
                                Knopf("✕", { entwurfSetzen(entwurf.filterIndexed { j, _ -> j != i }) }, kompakt = true)
                            }
                        }
                    } else {
                        Text(
                            text = "Noch kein Punkt. Setze welche auf der Karte oder nimm einen Ort von oben.",
                            style = Schrift.Klein,
                            color = Farben.TextSehrLeise,
                        )
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            if (sitzung.zeichnet) "Fertig gezeichnet" else "Auf der Karte setzen",
                            { zeichnenUmschalten(f) },
                            art = if (sitzung.zeichnet) Knopfart.Haupt else Knopfart.Normal,
                            kompakt = true,
                        )
                        if (sitzung.zeichnet) {
                            Knopf("Weiter auf der Karte", { griffe.karteFreigeben() }, kompakt = true)
                        }
                        Knopf(
                            if (entwurf.isNotEmpty()) "Streife setzen" else "Streife beenden",
                            {
                                if (!streifeSendet) {
                                    streifeSendet = true
                                    streifeFehler = null
                                    val stationen = entwurf
                                    bereich.launch {
                                        streifeFehler = welt.aktion("Der Pfad ließ sich nicht setzen.") {
                                            welt.wege.streifeSetzen(it, f.id, stationen)
                                        }
                                        if (streifeFehler == null && sitzung.zeichnet) zeichnenBeenden()
                                        streifeSendet = false
                                    }
                                }
                            },
                            kompakt = true,
                            aktiv = !streifeSendet,
                        )
                    }

                    if (stand.streifenrouten.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Auswahlfeld(
                                platzhalter = "Route auflegen …",
                                wahlen = stand.streifenrouten.map {
                                    Wahl(it.id, "${it.name} · ${it.stationen.size} ${if (it.stationen.size == 1) "Station" else "Stationen"}")
                                },
                                gewaehlt = gewaehlteRoute,
                                beiWahl = { gewaehlteRoute = it },
                                modifier = Modifier.weight(1f),
                            )
                            Knopf(
                                "Auflegen",
                                {
                                    val route = gewaehlteRoute
                                    if (route != null && !streifeSendet) {
                                        streifeSendet = true
                                        bereich.launch {
                                            streifeFehler = welt.streifenrouteAuflegen(f.id, route)
                                            if (streifeFehler == null) {
                                                stand.streifenrouten.firstOrNull { it.id == route }?.let { entwurfSetzen(it.stationen) }
                                            }
                                            streifeSendet = false
                                        }
                                    }
                                },
                                kompakt = true,
                                aktiv = !streifeSendet && gewaehlteRoute != null,
                            )
                            Knopf(
                                "✕",
                                {
                                    val route = gewaehlteRoute
                                    if (route != null) {
                                        bereich.launch {
                                            streifeFehler = welt.streifenrouteEntfernen(route)
                                            if (streifeFehler == null) gewaehlteRoute = null
                                        }
                                    }
                                },
                                kompakt = true,
                                aktiv = !streifeSendet && gewaehlteRoute != null,
                            )
                        }
                    }

                    if (entwurf.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Feld(
                                wert = routenName,
                                beiAenderung = { routenName = it.take(60) },
                                platzhalter = "Route benennen …",
                                modifier = Modifier.weight(1f),
                            )
                            Knopf(
                                "Sichern",
                                {
                                    when {
                                        entwurf.isEmpty() -> streifeFehler = "Eine Route ohne Stationen ist keine."
                                        routenName.trim().isEmpty() -> streifeFehler = "Die Route braucht einen Namen."
                                        else -> {
                                            streifeSendet = true
                                            val stationen = entwurf
                                            bereich.launch {
                                                streifeFehler = welt.streifenrouteSichern(routenName.trim(), stationen)
                                                streifeSendet = false
                                                if (streifeFehler == null) routenName = ""
                                            }
                                        }
                                    }
                                },
                                kompakt = true,
                                aktiv = !streifeSendet,
                            )
                        }
                    }
                }

                // ------------------------------------------------- Umsetzen
                if (f.lage == "Wache" && !f.geliehen) {
                    val andere = stand.stand?.wachen.orEmpty().filter { it.id != f.wacheId && traegtFahrzeuge(it.art) }
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                        Auswahlfeld(
                            platzhalter = "Umsetzen nach …",
                            wahlen = andere.map { Wahl(it.id, "${it.name} · ${it.belegt}/${it.stellplaetze}", aktiv = it.belegt < it.stellplaetze) },
                            gewaehlt = zielWache,
                            beiWahl = { zielWache = it },
                            modifier = Modifier.weight(1f),
                        )
                        Knopf(
                            if (sendet) "Fährt …" else "Umsetzen",
                            {
                                val ziel = zielWache
                                if (ziel == null) fehler = "Wähle die Zielwache."
                                else tun("Der Umzug ging nicht.") { welt.wege.fahrzeugUmsetzen(it, f.id, ziel) }
                            },
                            kompakt = true,
                            aktiv = !sendet && zielWache != null,
                        )
                    }
                }

                // ------------------------------------------------- Lehrgang
                if (f.lage == "Wache" && !f.geliehen && einrichtungen.isNotEmpty()) {
                    val moeglich = lehrgaenge.filter { it.grund == null }
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                        Auswahlfeld(
                            platzhalter = if (moeglich.isNotEmpty()) "Zum Lehrgang …" else "Kein Lehrgang offen",
                            wahlen = moeglich.map {
                                Wahl(it.id, "${it.name} · ${zahl(it.kosten)} · ${(it.dauerMinuten / 60.0).roundToInt()} h")
                            },
                            gewaehlt = lehrgang,
                            beiWahl = { lehrgang = it },
                            aktiv = moeglich.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        )
                        Knopf(
                            if (sendet) "Fährt …" else "Anmelden",
                            {
                                val l = lehrgang
                                val ziel = lehrgangWache ?: einrichtungen.firstOrNull()?.id
                                if (l == null) fehler = "Wähle einen Lehrgang."
                                else if (ziel != null) tun("Die Anmeldung ging nicht.") { welt.wege.zumLehrgang(it, f.id, ziel, l) }
                            },
                            kompakt = true,
                            aktiv = !sendet && lehrgang != null,
                        )
                    }
                    if (einrichtungen.size > 1) {
                        Auswahlfeld(
                            platzhalter = "Erste Einrichtung",
                            wahlen = listOf(Wahl<String?>(null, "Erste Einrichtung")) + einrichtungen.map { Wahl<String?>(it.id, it.name) },
                            gewaehlt = lehrgangWache,
                            beiWahl = { lehrgangWache = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    lehrgang?.let { id ->
                        Text(
                            text = "Danach kann die Besatzung ${lehrgaenge.firstOrNull { it.id == id }?.faehigkeit.orEmpty()}. " +
                                "Das Fahrzeug fehlt so lange zu Hause.",
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                        )
                    }
                }

                // ------------------------------------------------- Werkstatt
                val wahl = werkstattwahl
                if (f.lage == "Wache" && !f.geliehen && f.zustand < 100 && wahl != null && wahl.werkstaetten.isNotEmpty()) {
                    val freie = wahl.werkstaetten.filter { it.beanstandung == null }
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Zustand ${f.zustand} %" + (if (f.verschlissen) " — rückt nicht mehr aus" else "") + ". Instandsetzung",
                            style = Schrift.Winzig,
                            color = if (f.verschlissen) Farben.SignalHell else Farben.TextSehrLeise,
                        )
                        Credits(wahl.preis, stil = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), zeichengroesse = 10.dp)
                        Text(text = "· ${wahl.dauerMinuten} min.", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                        if (wahl.werkstaetten.size > 1) {
                            Auswahlfeld(
                                platzhalter = "Erste freie Werkstatt",
                                wahlen = listOf(Wahl<String?>(null, "Erste freie Werkstatt")) + wahl.werkstaetten.map {
                                    Wahl<String?>(it.id, "${it.name} · ${it.belegt}/${it.buehnen}", it.beanstandung, it.beanstandung == null)
                                },
                                gewaehlt = werkstattZiel,
                                beiWahl = { werkstattZiel = it },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Knopf(
                            if (freie.isEmpty()) "Bühnen belegt" else "In die Werkstatt",
                            {
                                val ziel = werkstattZiel ?: freie.firstOrNull()?.id
                                if (ziel == null) fehler = "Wähle eine Werkstatt."
                                else tun("Die Fahrt in die Werkstatt ging nicht.") { welt.wege.zurWerkstatt(it, f.id, ziel) }
                            },
                            kompakt = true,
                            aktiv = !sendet && freie.isNotEmpty(),
                        )
                    }
                }

                // ------------------------------------------------- Verkauf
                if (f.lage == "Wache" && !f.geliehen) {
                    if (!verkaufGefragt) {
                        Knopf(
                            "Verkaufen für ${zahl(((f.preis / 2.0) * (f.zustand / 100.0)).roundToInt())} Welt-Credits",
                            { verkaufGefragt = true },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf(
                                "Wirklich verkaufen",
                                { tun("Der Verkauf ging nicht.", nachher = Welt.Nachladen.Beides) { welt.wege.fahrzeugVerkaufen(it, f.id) } },
                                art = Knopfart.Gefahr,
                                kompakt = true,
                                aktiv = !sendet,
                            )
                            Knopf("Nein", { verkaufGefragt = false }, art = Knopfart.Leise, kompakt = true)
                        }
                    }
                }

                Absage(fehler)
            }
        }
    }
}
