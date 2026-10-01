package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextAlign
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Zeichenknopf
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.mobil.Weltfahrt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltLage
import de.pagerspass.pagerspass.netz.WeltLehrgang
import de.pagerspass.pagerspass.netz.WeltStreifenroute
import de.pagerspass.pagerspass.netz.WeltStreifenstation
import de.pagerspass.pagerspass.netz.WeltVorlage
import de.pagerspass.pagerspass.netz.WeltWache
import de.pagerspass.pagerspass.netz.WeltWerkstatt
import de.pagerspass.pagerspass.netz.WeltWerkstattwahl
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.launch

private val LAGENRANG = mapOf(
    "VorOrt" to 0, "Anfahrt" to 1, "Ausrueckt" to 2, "Bereitstellung" to 3,
    "Rueckfahrt" to 4, "Streife" to 5, "Wache" to 6,
)

/**
 * Die Fahrzeuge — das Gegenstück zu `FahrzeugBlende.vue`.
 *
 * <b>Wer arbeitet, steht oben</b>: vor Ort, Anfahrt, ausrückend, dann die
 * Bereitstellung, die Rückfahrt, die Streife — und zuletzt, was auf der Wache
 * steht. Aufgeklappt stehen alle Griffe eines Fahrzeugs unter seiner eigenen
 * Zeile: Rufname, Einsatz, Streife, Umzug, Lehrgang, Werkstatt, Verkauf.
 */
@Composable
fun FahrzeugeSeite(welt: Welt, zustand: Weltzustand, werkbank: Werkbank, karte: Weltkartenstand) {
    val takt = sekundentakt()
    var suche by remember { mutableStateOf("") }
    @Suppress("UNUSED_EXPRESSION") takt

    Bereichsreiter(welt, zustand)
    Knopf("Fahrzeug kaufen", { werkbank.seite = Werkzeug.Fahrzeugkauf }, kompakt = true, zeichenVorn = {
        Icon(Weltzeichen.Kauf, contentDescription = null, tint = Farben.Text, modifier = Modifier.size(18.dp))
    })

    val bestand = zustand.fahrzeuge.filter(zustand::fahrzeugImBereich)
    // Das Suchfeld erst, wenn es etwas zu durchsuchen gibt — wie im Web ab sechs.
    if (bestand.size > 5) {
        Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Rufname, Typ, Fähigkeit …", stil = Schrift.MonoNormal)
    }
    fun passt(f: WeltFahrzeug): Boolean {
        val t = suche.trim().lowercase()
        if (t.isEmpty()) return true
        val v = zustand.vorlage(f.vorlageId)
        return t in f.funkrufname.lowercase() || t in f.typ.lowercase() ||
            v?.faehigkeiten?.any { t in it.lowercase() } == true || f.gelernt.any { t in it.lowercase() } ||
            zustand.stand?.wachen?.firstOrNull { it.id == f.wacheId }?.name?.lowercase()?.contains(t) == true
    }
    val liste = bestand.filter(::passt).sortedWith(compareBy({ LAGENRANG[it.lage] ?: 9 }, { it.funkrufname }))

    when {
        bestand.isEmpty() -> Leerhinweis("Noch kein Fahrzeug. Erst eine Wache bauen, dann Fahrzeuge kaufen.")
        liste.isEmpty() -> Leerhinweis("Kein Fahrzeug passt zu „$suche“.")
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        liste.forEach { f ->
            val offen = werkbank.offenesFahrzeug == f.id
            Column {
                Fahrzeugkopf(zustand, f, offen) {
                    werkbank.offenesFahrzeug = if (offen) null else f.id
                    if (!offen) {
                        werkbank.gewaehltesFahrzeug = f.id
                        if (f.lage != "Wache") karte.hinschauen(f.lat, f.lon)
                    }
                }
                if (offen) {
                    // Die Griffe hängen unter ihrer Zeile, eingerückt, mit dem Strich in Amber.
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Abstand.Normal)
                            .background(Farben.Flaeche, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                            .drawBehind { drawLine(Farben.AmberTief, Offset(0f, 0f), Offset(0f, size.height), 2.dp.toPx()) }
                            .padding(horizontal = Abstand.Klein, vertical = Abstand.Normal),
                    ) {
                        Fahrzeuggriffe(welt, zustand, werkbank, f)
                    }
                }
            }
        }
    }
}

/** Die Farbe des FMS-Status an Zeile und Punkt — `fahrzeugblende__status--N`. */
private fun statusfarbe(status: Int): Color? = when (status) {
    1 -> Farben.FmsSprechwunsch
    2 -> Farben.FmsFrei
    3 -> Farben.FmsAnfahrt
    4 -> Farben.FmsVorOrt
    6 -> Farben.FmsGebunden
    else -> null
}

/** Der runde Statuspunkt vorn an einer Fahrzeugzeile — `.fahrzeugblende__status`. */
@Composable
fun Statuspunkt(status: Int) {
    val farbe = statusfarbe(status)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(20.dp).background(farbe ?: Farben.FlaecheAktiv, CircleShape),
    ) {
        Text(
            "$status",
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
            color = if (farbe != null) Farben.AufFarbe else Farben.TextLeise,
        )
    }
}

@Composable
private fun Fahrzeugkopf(zustand: Weltzustand, f: WeltFahrzeug, offen: Boolean, beiDruck: () -> Unit) {
    val fahrt = zustand.fahrt
    val kante = (if (f.lage == "Ausrueckt") Farben.FmsAnfahrt else statusfarbe(f.status)) ?: Farben.AmberTief
    val form = if (offen) RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp) else RoundedCornerShape(14.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(form)
            .background(if (offen) Farben.FlaecheHoch else Farben.Flaeche)
            .border(1.dp, Farben.Rand, form)
            .drawBehind { drawRect(kante, size = Size(3.dp.toPx(), size.height)) }
            .clickable(onClick = beiDruck)
            .padding(start = Abstand.Normal, end = Abstand.Klein, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        Statuspunkt(f.status)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildAnnotatedString {
                    append("${f.typ} · ${taetigkeit(zustand, f)}")
                    if (f.lehrgang != null) append(" · ${f.lehrgang}")
                    else if (f.inWerkstatt && f.werkstattOrt != null) append(" · ${f.werkstattOrt}")
                    if (!f.inWerkstatt && f.verschlissen) {
                        append(" · ")
                        withStyle(SpanStyle(color = Farben.SignalHell, fontWeight = FontWeight.SemiBold)) { append("verschlissen") }
                    } else if (!f.inWerkstatt && f.werkstattFaellig) {
                        append(" · ")
                        withStyle(SpanStyle(color = Farben.AmberHell)) { append("bald fällig") }
                    }
                    if (f.naechsteLageId != null) append(" · fährt danach weiter")
                },
                style = Schrift.Klein,
                color = Farben.TextLeise,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (f.geliehen) Weltmarke("geliehen", Farben.BlauHell)
        val restText = restzeit(fahrt, f.lehrgangFertigUm)
            ?: restzeit(fahrt, f.werkstattFertigUm)
            ?: fahrt.restMinuten(f.ankunftUm)?.takeIf { f.lage != "Wache" }?.let { min ->
                if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
            }
            ?: geliehenRest(fahrt, f)
        if (restText != null) {
            Text(restText, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise, textAlign = TextAlign.End)
        }
        if (f.lage != "Wache" && f.lage != "Bereitstellung") {
            Box(
                Modifier
                    .width(48.dp)
                    .height(6.dp)
                    .background(Farben.BgTief, Rundung.Rund),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fahrt.anteil(f.losUm, f.ankunftUm).toFloat().coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Farben.AmberTief, Farben.AmberHell)), Rundung.Rund),
                )
            }
        }
        Text(
            "›",
            style = Schrift.Gross,
            color = if (offen) Farben.Amber else Farben.TextSehrLeise,
            modifier = Modifier.rotate(if (offen) 90f else 0f),
        )
    }
}

/** „noch 1 h 5 min“ bis zu einer Uhrzeit — Lehrgang und Werkstatt. */
private fun restzeit(fahrt: Weltfahrt, bis: String?): String? {
    val ms = fahrt.restMs(bis) ?: return null
    if (ms <= 0) return "gleich fertig"
    val min = Math.ceil(ms / 60_000.0).toInt()
    return if (min >= 60) "noch ${min / 60} h ${min % 60} min" else "noch $min min"
}

private fun geliehenRest(fahrt: Weltfahrt, f: WeltFahrzeug): String? {
    if (!f.geliehen) return null
    val ms = fahrt.restMs(f.geliehenBis) ?: return null
    if (ms <= 0) return "fährt heim"
    val h = Math.ceil(ms / 3_600_000.0).toInt()
    return if (h < 24) "noch $h h" else "noch ${tage(Math.ceil(h / 24.0).toInt())}"
}


@Composable
private fun Fahrzeuggriffe(welt: Welt, zustand: Weltzustand, werkbank: Werkbank, f: WeltFahrzeug) {
    val bereich = rememberCoroutineScope()
    var fehler by remember(f.id) { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var rufname by remember(f.id) { mutableStateOf(f.funkrufname) }
    var zielWache by remember(f.id) { mutableStateOf<WeltWache?>(null) }
    var zielLage by remember(f.id) { mutableStateOf<WeltLage?>(null) }
    var verkaufGefragt by remember(f.id) { mutableStateOf(false) }
    var rueckgabeGefragt by remember(f.id) { mutableStateOf(false) }
    var lehrgaenge by remember(f.id) { mutableStateOf<List<WeltLehrgang>>(emptyList()) }
    var lehrgang by remember(f.id) { mutableStateOf<WeltLehrgang?>(null) }
    var einrichtung by remember(f.id) { mutableStateOf<WeltWache?>(null) }
    var werkstattwahl by remember(f.id) { mutableStateOf<WeltWerkstattwahl?>(null) }
    var werkstatt by remember(f.id) { mutableStateOf<WeltWerkstatt?>(null) }

    LaunchedEffect(f.id) {
        val k = welt.kennung ?: return@LaunchedEffect
        // Geliehene lernen nichts und gehen nicht in die eigene Werkstatt —
        // das bleibt beim Besitzer.
        if (!f.geliehen) {
            lehrgaenge = runCatching { welt.wege.lehrgaenge(k, f.id) }.getOrDefault(emptyList())
            if (f.lage == "Wache") werkstattwahl = runCatching { welt.wege.werkstattwahl(k, f.id) }.getOrNull()
        }
    }

    fun tun(ersatz: String, schliessen: Boolean = true, block: suspend (String) -> Unit) {
        sendet = true
        fehler = null
        bereich.launch {
            fehler = welt.handlung(ersatz, block)
            sendet = false
            if (fehler == null && schliessen) werkbank.offenesFahrzeug = null
        }
    }

    // Was die Besatzung im Lehrgang dazugelernt hat — Marken wie im Web.
    val gelernt = lehrgaenge.filter { it.gelernt }.map { it.faehigkeit }
    if (gelernt.isNotEmpty()) {
        Umbruchreihe { gelernt.forEach { Weltmarke(it, Farben.BlauHell) } }
    }

    if (f.geliehen) {
        Leisesatz(
            "Geliehen (${geliehenRest(zustand.fahrt, f) ?: "endet gleich"}) — umbenennen, umsetzen und verkaufen kann " +
                "nur der Besitzer.",
            winzig = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            if (!rueckgabeGefragt) {
                Knopf("Zurückgeben", { rueckgabeGefragt = true }, kompakt = true, art = Knopfart.Leise, aktiv = !sendet)
            } else {
                Knopf("Ohne Erstattung zurückgeben", {
                    tun("Das ging nicht.") { welt.wege.leiheZurueckgeben(it, f.id); welt.betriebLaden() }
                }, kompakt = true, aktiv = !sendet)
                Knopf("Nein", { rueckgabeGefragt = false }, kompakt = true, art = Knopfart.Leise)
            }
        }
    } else {
        // ------------------------------------------------------------ Rufname
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Feld(
                wert = rufname,
                beiAenderung = { rufname = it.take(48) },
                platzhalter = f.funkrufname,
                stil = Schrift.MonoNormal,
                modifier = Modifier.weight(1f),
            )
            Knopf(if (rufname.isBlank()) "Wie im Buch" else "Übernehmen", {
                tun("Der Rufname ging nicht.", schliessen = false) { welt.wege.fahrzeugUmbenennen(it, f.id, rufname.trim()); welt.allesLaden() }
            }, kompakt = true, aktiv = !sendet && rufname.trim() != f.funkrufname)
        }
        if (f.rufnameVonHand) Leisesatz("Selbst vergeben — ein leeres Feld stellt den Namen aus dem Buch wieder her.", winzig = true)
    }

    // ------------------------------------------------------------- Einsatz
    val moegliche = zustand.lagenImBereich.filter {
        it.zustand != "Erledigt" && it.id != f.lageId && (zustand.istVormerkbar(f) || zustand.istAlarmierbar(f, it))
    }
    if (zustand.istAlarmierbar(f) || zustand.istVormerkbar(f) || moegliche.isNotEmpty()) {
        if (moegliche.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Auswahl(
                    etikett = "Einsatz",
                    eintraege = moegliche,
                    gewaehlt = zielLage,
                    aufschrift = { "${it.stichwort} · ${entfernungText(it.entfernungMeter)} · ${deckungstext(it)}" },
                    beiWahl = { zielLage = it },
                    platzhalter = if (zustand.istVormerkbar(f)) "Danach fahren zu …" else "Auf Einsatz schicken …",
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    when {
                        sendet -> "Alarmiert …"
                        zustand.istVormerkbar(f) -> "Vormerken"
                        else -> "Alarmieren"
                    },
                    {
                        val l = zielLage ?: return@Knopf
                        sendet = true
                        bereich.launch {
                            fehler = welt.alarmieren(l.id, listOf(f.id))
                            sendet = false
                            if (fehler == null) werkbank.offenesFahrzeug = null
                        }
                    },
                    kompakt = true,
                    aktiv = !sendet && zielLage != null,
                )
            }
        }
        Leisesatz(
            if (zustand.istVormerkbar(f) && moegliche.isNotEmpty()) "Es fährt los, sobald die Arbeit hier getan ist."
            else "Gerade kein offener Einsatz — neue kommen von selbst.",
            winzig = true,
        )
    }
    if (f.lage == "Ausrueckt" || f.lage == "Anfahrt") {
        Knopf("Anfahrt abbrechen", {
            tun("Der Abbruch ging nicht.") { welt.wege.anfahrtAbbrechen(it, f.id); welt.betriebLaden() }
        }, kompakt = true, art = Knopfart.Leise, aktiv = !sendet)
    }

    // ------------------------------------------------------------- Streife
    if (f.streifenfaehig && !f.geliehen) Streifengriffe(welt, zustand, werkbank, f)

    if (!f.geliehen && f.lage == "Wache") {
        // ---------------------------------------------------------- Umsetzen
        val andere = zustand.stand?.wachen.orEmpty().filter { it.id != f.wacheId && traegtFahrzeuge(it.art) }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
            Auswahl(
                etikett = "Zielwache",
                eintraege = andere,
                gewaehlt = zielWache,
                aufschrift = { "${it.name} · ${it.belegt}/${it.stellplaetze}" },
                beiWahl = { if (it.belegt < it.stellplaetze) zielWache = it },
                platzhalter = "Umsetzen nach …",
                modifier = Modifier.weight(1f),
            )
            Knopf(if (sendet) "Fährt …" else "Umsetzen", {
                val z = zielWache ?: return@Knopf
                tun("Der Umzug ging nicht.") { welt.wege.fahrzeugUmsetzen(it, f.id, z.id); welt.betriebLaden() }
            }, kompakt = true, aktiv = !sendet && zielWache != null)
        }

        // ---------------------------------------------------------- Lehrgang
        val einrichtungen = zustand.stand?.wachen.orEmpty().filter { it.art == "Lehrgangseinrichtung" }
        if (einrichtungen.isNotEmpty()) {
            val offen = lehrgaenge.filter { it.grund == null }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Auswahl(
                    etikett = "Lehrgang",
                    eintraege = offen,
                    gewaehlt = lehrgang,
                    aufschrift = { "${it.name} · ${zahl(it.kosten)} · ${Math.round(it.dauerMinuten / 60.0)} h" },
                    beiWahl = { lehrgang = it },
                    platzhalter = if (offen.isNotEmpty()) "Zum Lehrgang …" else "Kein Lehrgang offen",
                    modifier = Modifier.weight(1f),
                )
                Knopf(if (sendet) "Fährt …" else "Anmelden", {
                    val l = lehrgang ?: run { fehler = "Wähle einen Lehrgang."; return@Knopf }
                    val ziel = (einrichtung ?: einrichtungen.first()).id
                    tun("Die Anmeldung ging nicht.") { welt.wege.zumLehrgang(it, f.id, ziel, l.id); welt.betriebLaden() }
                }, kompakt = true, aktiv = !sendet && lehrgang != null)
            }
            if (einrichtungen.size > 1) {
                Auswahl(
                    etikett = "Einrichtung",
                    eintraege = listOf<WeltWache?>(null) + einrichtungen,
                    gewaehlt = einrichtung,
                    aufschrift = { it?.name ?: "Erste Einrichtung" },
                    beiWahl = { einrichtung = it },
                )
            }
            lehrgang?.let {
                Text(
                    buildAnnotatedString {
                        append("Danach kann die Besatzung ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it.faehigkeit) }
                        append(". Das Fahrzeug fehlt so lange zu Hause.")
                    },
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
            }
        }

        // ---------------------------------------------------------- Werkstatt
        werkstattwahl?.takeIf { f.zustand < 100 && it.werkstaetten.isNotEmpty() }?.let { w ->
            val frei = w.werkstaetten.filter { it.beanstandung == null }
            Text(
                "Zustand ${f.zustand} %" + (if (f.verschlissen) " — rückt nicht mehr aus" else "") +
                    ". Instandsetzung ${credits(w.preis)} · ${w.dauerMinuten} min.",
                style = Schrift.Winzig,
                color = if (f.verschlissen) Farben.SignalHell else Farben.TextSehrLeise,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                if (w.werkstaetten.size > 1) {
                    Auswahl(
                        etikett = "Werkstatt",
                        eintraege = listOf<WeltWerkstatt?>(null) + w.werkstaetten,
                        gewaehlt = werkstatt,
                        aufschrift = { it?.let { x -> "${x.name} · ${x.belegt}/${x.buehnen}" + (x.beanstandung?.let { b -> " — $b" } ?: "") } ?: "Erste freie Werkstatt" },
                        beiWahl = { if (it?.beanstandung == null) werkstatt = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                Knopf(if (frei.isEmpty()) "Bühnen belegt" else "In die Werkstatt", {
                    val ziel = (werkstatt ?: frei.firstOrNull())?.id ?: return@Knopf
                    tun("Die Fahrt in die Werkstatt ging nicht.") { welt.wege.zurWerkstatt(it, f.id, ziel); welt.betriebLaden() }
                }, kompakt = true, aktiv = !sendet && frei.isNotEmpty())
            }
        }

        // ---------------------------------------------------------- Verkauf
        val erloes = Math.round(f.preis / 2.0 * (f.zustand / 100.0))
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            if (!verkaufGefragt) {
                Knopf("Verkaufen für ${credits(erloes)}", { verkaufGefragt = true }, kompakt = true, art = Knopfart.Leise)
            } else {
                Knopf("Wirklich verkaufen", {
                    tun("Der Verkauf ging nicht.") { welt.wege.fahrzeugVerkaufen(it, f.id); welt.allesLaden() }
                }, kompakt = true, art = Knopfart.Gefahr, aktiv = !sendet)
                Knopf("Nein", { verkaufGefragt = false }, kompakt = true, art = Knopfart.Leise)
            }
        }
    }
    Warnsatz(fehler)
}

/**
 * Die Streife eines Polizeifahrzeugs — `.fahrzeugblende__streife`: Stationen aus
 * den vorgeschlagenen Orten, auf der Karte gesetzt oder als gespeicherte Route
 * aufgelegt.
 */
@Composable
private fun Streifengriffe(welt: Welt, zustand: Weltzustand, werkbank: Werkbank, f: WeltFahrzeug) {
    val bereich = rememberCoroutineScope()
    var orte by remember { mutableStateOf<List<WeltStreifenstation>>(emptyList()) }
    var fehler by remember(f.id) { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var routenName by remember { mutableStateOf("") }
    var route by remember { mutableStateOf<WeltStreifenroute?>(null) }
    val hoechstens = if (zustand.stand?.freischaltungen?.any { it.was == "Streifenausbau" && it.frei } == true) 12 else 8

    LaunchedEffect(f.id) {
        // Der Entwurf beginnt mit dem gesetzten Pfad — es sei denn, er gehört
        // schon diesem Fahrzeug (dann kommt man von der Karte zurück).
        if (werkbank.pfadFahrzeug != f.id) {
            werkbank.pfadFahrzeug = f.id
            werkbank.pfad = f.streifenpfad
        }
        val k = welt.kennung ?: return@LaunchedEffect
        orte = runCatching { welt.wege.streifenorte(k) }.getOrDefault(emptyList())
    }
    val entwurf = werkbank.pfad

    Text(
        buildAnnotatedString {
            append("Streife ")
            if (f.streifenpfad.isEmpty()) append("· keine")
            else withStyle(SpanStyle(fontFamily = Schrift.Mono)) { append("· " + f.streifenpfad.joinToString(" → ") { it.name }) }
        },
        style = Schrift.Klein,
        color = Farben.TextLeise,
    )
    fehler?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
    if (orte.isEmpty()) {
        Leerhinweis(
            "Keine Stationen in der Nähe. Sie kommen aus den Sonderobjekten und Kliniken deines Landkreises und " +
                "aus deinen eigenen Punkten — setze einen unter „Bauen“, dann steht er hier. Oder tippe die Punkte " +
                "gleich auf der Karte.",
        )
    } else {
        Umbruchreihe {
            orte.forEach { o ->
                val drin = entwurf.any { it.name == o.name }
                Pille(o.name, drin, {
                    fehler = null
                    werkbank.pfad = when {
                        drin -> entwurf.filter { it.name != o.name }
                        entwurf.size >= hoechstens -> { fehler = "Ein Streifenpfad hat höchstens $hoechstens Stationen."; entwurf }
                        else -> entwurf + o
                    }
                })
            }
        }
    }
    if (entwurf.isEmpty()) {
        Text("Noch kein Punkt. Setze welche auf der Karte oder nimm einen Ort von oben.", style = Schrift.Klein, color = Farben.TextSehrLeise)
    }
    entwurf.forEachIndexed { i, s ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text("${i + 1}", style = Schrift.MonoKlein, color = Farben.TextLeise)
            Text(s.name, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Knopf("↑", {
                werkbank.pfad = entwurf.toMutableList().also { l -> l[i] = l[i - 1].also { l[i - 1] = l[i] } }
            }, kompakt = true, aktiv = i > 0)
            Knopf("↓", {
                werkbank.pfad = entwurf.toMutableList().also { l -> l[i] = l[i + 1].also { l[i + 1] = l[i] } }
            }, kompakt = true, aktiv = i < entwurf.size - 1)
            Knopf("✕", { werkbank.pfad = entwurf.filterIndexed { j, _ -> j != i } }, kompakt = true)
        }
    }
    Umbruchreihe {
        Knopf("Auf der Karte setzen", { werkbank.karteWaehlen(Kartenmodus.Pfad) }, kompakt = true)
        Knopf(if (entwurf.isNotEmpty()) "Streife setzen" else "Streife beenden", {
            sendet = true
            fehler = null
            bereich.launch {
                fehler = welt.handlung("Der Pfad ließ sich nicht setzen.") {
                    welt.wege.streifeSetzen(it, f.id, entwurf)
                    welt.betriebLaden()
                }
                sendet = false
            }
        }, kompakt = true, aktiv = !sendet)
    }
    if (zustand.streifenrouten.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
            Auswahl(
                etikett = "Gespeicherte Route",
                eintraege = zustand.streifenrouten,
                gewaehlt = route,
                aufschrift = { "${it.name} · ${it.stationen.size} ${if (it.stationen.size == 1) "Station" else "Stationen"}" },
                beiWahl = { route = it },
                platzhalter = "Route auflegen …",
                modifier = Modifier.weight(1f),
            )
            Knopf("Auflegen", {
                val r = route ?: return@Knopf
                sendet = true
                bereich.launch {
                    fehler = welt.streifenrouteAuflegen(f.id, r.id)
                    sendet = false
                    if (fehler == null) werkbank.pfad = r.stationen
                }
            }, kompakt = true, aktiv = route != null && !sendet)
            Zeichenknopf({
                val r = route ?: return@Zeichenknopf
                bereich.launch {
                    fehler = welt.streifenrouteEntfernen(r.id)
                    route = null
                }
            }, "Route entfernen", kompakt = true, aktiv = route != null && !sendet) {
                Text("✕", style = Schrift.Klein, color = Farben.Text)
            }
        }
    }
    if (entwurf.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Feld(wert = routenName, beiAenderung = { routenName = it.take(60) }, platzhalter = "Route benennen …", modifier = Modifier.weight(1f))
            Knopf("Sichern", {
                if (routenName.isBlank()) { fehler = "Die Route braucht einen Namen."; return@Knopf }
                sendet = true
                bereich.launch {
                    fehler = welt.streifenrouteSichern(routenName.trim(), entwurf)
                    sendet = false
                    if (fehler == null) routenName = ""
                }
            }, kompakt = true, aktiv = !sendet)
        }
    }
}

/**
 * Der Fahrzeugkauf — `BauBlende.vue` mit `nur="fahrzeug"`, so wie das Web ihn
 * am Handy als eigene Seite öffnet.
 *
 * <b>Erst die Wache, dann die Liste.</b> Was angeboten wird, hängt an der
 * Bauart (was dort stehen darf) und an den Freischaltungen (Rettungsdienst,
 * Polizei, THW) — beides steht hier, damit nichts angeboten wird, was der
 * Server ablehnt. Ohne Wache steht in der Liste nichts.
 *
 * <b>Bewusst ohne Riss:</b> Das Web zeichnet vor jede Zeile die Seitenansicht
 * aus seinem Bauplan; die gibt es in der App nicht, und ein leeres Fach
 * davor sagte nichts.
 */
@Composable
fun FahrzeugkaufSeite(welt: Welt, zustand: Weltzustand, @Suppress("UNUSED_PARAMETER") werkbank: Werkbank) {
    val bereich = rememberCoroutineScope()
    // Ohne Lehrgangseinrichtung und Werkstatt: Dort steht kein Fahrzeug.
    val wachen = zustand.stand?.wachen.orEmpty().filter { traegtFahrzeuge(it.art) }
    var wacheId by remember { mutableStateOf<String?>(null) }
    var vorlage by remember { mutableStateOf<String?>(null) }
    var suche by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    val w = wachen.firstOrNull { it.id == wacheId }
    val frei = zustand.stand?.freischaltungen.orEmpty().filter { it.frei }.map { it.was }.toSet()
    val freiePlaetze = w?.let { it.stellplaetze - it.belegt } ?: 0

    Auswahl(
        etikett = "Wache",
        eintraege = wachen,
        gewaehlt = w,
        aufschrift = { "${it.name} · ${it.belegt}/${it.stellplaetze}" },
        unterschrift = { if (it.belegt >= it.stellplaetze) "voll" else null },
        platzhalter = "Wähle eine Wache",
        // Eine volle Wache ist im Web eine gesperrte Zeile — hier nimmt die Wahl sie nicht an.
        beiWahl = { if (it.belegt < it.stellplaetze) { wacheId = it.id } },
    )
    if (w != null) {
        // Jede Wache kauft aus dem Katalog ihres Staats — der Staat kommt vom
        // Server und folgt dem Standort.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Landesflagge(w.staat, 18.dp)
            Text(
                buildAnnotatedString {
                    append("Katalog ${staatName(w.staat)} · noch ")
                    withStyle(SpanStyle(fontFamily = Schrift.Mono)) { append("$freiePlaetze") }
                    append(" Plätze frei.")
                },
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
        }
    }

    val vorlagen = if (w == null) emptyList() else jeTypEine(zustand.vorlagen.values).filter { v ->
        (v.staaten.firstOrNull() ?: "Deutschland") == w.staat &&
            passtZurWache(w.art, v) && when (v.organisation) {
            "Feuerwehr" -> true
            "Rettungsdienst" -> "Rettungsdienst" in frei
            "Polizei" -> "Polizei" in frei
            "Thw" -> "Thw" in frei
            else -> false
        }
    }
    // Wechselt die Wache, verfällt eine Wahl, die dort nicht stehen darf.
    if (vorlage != null && vorlagen.none { it.id == vorlage }) vorlage = null

    when {
        w == null -> Text(
            "Wähle zuerst die Wache — was dort stehen darf, hängt an ihrer Bauart.",
            style = Schrift.Klein,
            color = Farben.TextSehrLeise,
        )
        vorlagen.isEmpty() -> Text(
            if (zustand.vorlagen.isEmpty()) "Der Katalog wird geladen …" else "Für diese Wache ist gerade nichts freigeschaltet.",
            style = Schrift.Klein,
            color = Farben.TextSehrLeise,
        )
    }
    if (vorlagen.size > 8) {
        Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Typ, Fähigkeit, Organisation …")
    }
    // Die Suche ändert die Anzeige, nicht den Bestand: Die Wahl bleibt, auch
    // wenn das gewählte Fahrzeug gerade nicht zum Suchwort passt.
    val gruppen = vorlagen.filter { v ->
        val t = suche.trim().lowercase()
        t.isEmpty() || t in v.typ.lowercase() || t in v.kategorie.lowercase() ||
            t in v.organisation.lowercase() || v.faehigkeiten.any { t in it.lowercase() }
    }.groupBy { it.kategorie }
    val guthaben = zustand.guthaben
    if (vorlagen.isNotEmpty() && gruppen.isEmpty()) {
        Text("Kein Fahrzeug passt zu „$suche“.", style = Schrift.Klein, color = Farben.TextSehrLeise)
    }
    gruppen.forEach { (kategorie, liste) ->
        Ueberschrift(kategorie, Modifier.padding(top = Abstand.Klein))
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
            liste.forEach { v ->
                val preis = zustand.stand?.fahrzeugpreise?.get(v.id) ?: 0
                Listenwahl(
                    an = vorlage == v.id,
                    beiDruck = { vorlage = v.id },
                    // Zu teuer heißt blass, nicht gesperrt: Wählen darf man trotzdem.
                    modifier = Modifier.alpha(if (preis > guthaben) 0.55f else 1f),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                        Text(
                            v.typ,
                            style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                            color = Farben.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            v.faehigkeiten.joinToString(" · ").ifEmpty { v.beschreibung },
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        credits(preis),
                        style = Schrift.MonoKlein,
                        color = Farben.AmberHell,
                        maxLines = 1,
                        modifier = Modifier.padding(start = Abstand.Normal),
                    )
                }
            }
        }
    }
    Warnsatz(fehler)
    Knopf(
        if (sendet) "Wird gekauft …" else "Kaufen",
        {
            val id = w?.id
            val gewaehlt = vorlage
            if (id == null || gewaehlt == null) { fehler = "Wähle Wache und Fahrzeug."; return@Knopf }
            sendet = true
            fehler = null
            bereich.launch {
                fehler = welt.handlung("Der Kauf ging nicht.") {
                    welt.wege.fahrzeugKaufen(it, id, gewaehlt)
                    welt.allesLaden()
                }
                sendet = false
            }
        },
        art = Knopfart.Haupt,
        breit = true,
        aktiv = !sendet && vorlage != null && freiePlaetze > 0,
    )
}
