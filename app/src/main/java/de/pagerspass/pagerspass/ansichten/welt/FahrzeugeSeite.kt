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
        Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Rufname, Typ, Fähigkeit …")
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
        bestand.isEmpty() -> Leisesatz("Noch kein Fahrzeug. Erst eine Wache bauen, dann Fahrzeuge kaufen.")
        liste.isEmpty() -> Leisesatz("Kein Fahrzeug passt zu „${suche.trim()}“.")
    }

    liste.forEach { f ->
        val offen = werkbank.offenesFahrzeug == f.id
        Weltkasten(an = offen) {
            Fahrzeugkopf(zustand, f) {
                werkbank.offenesFahrzeug = if (offen) null else f.id
                if (!offen) {
                    werkbank.gewaehltesFahrzeug = f.id
                    if (f.lage != "Wache") karte.hinschauen(f.lat, f.lon)
                }
            }
            if (offen) Fahrzeuggriffe(welt, zustand, werkbank, f)
        }
    }
}

@Composable
private fun Fahrzeugkopf(zustand: Weltzustand, f: WeltFahrzeug, beiDruck: () -> Unit) {
    val fahrt = zustand.fahrt
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().clickable(onClick = beiDruck),
    ) {
        Fmsplakette(f.status)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(f.funkrufname, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val teile = buildList {
                add(f.typ)
                add(taetigkeit(zustand, f))
                f.lehrgang?.let { add(it) }
                if (f.inWerkstatt) f.werkstattOrt?.let { add(it) }
            }
            Text(teile.joinToString(" · "), style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 2)
            Umbruchreihe {
                when {
                    f.verschlissen -> Weltmarke("verschlissen", Farben.SignalHell)
                    f.werkstattFaellig -> Weltmarke("Werkstatt bald", Farben.Amber)
                }
                if (f.geliehen) {
                    val rest = fahrt.restMs(f.geliehenBis)?.let { ms ->
                        if (ms <= 0) "fährt heim" else {
                            val h = Math.ceil(ms / 3_600_000.0).toInt()
                            if (h < 24) "noch $h h" else "noch ${tage(Math.ceil(h / 24.0).toInt())}"
                        }
                    }
                    Weltmarke("geliehen" + (rest?.let { " · $it" } ?: ""), Farben.BlauHell)
                }
                f.lehrgangFertigUm?.let { um ->
                    fahrt.restMinuten(um)?.let { Weltmarke(if (it <= 0) "gleich fertig" else "noch ${dauer(it)}", Farben.ViolettHell) }
                }
                f.werkstattFertigUm?.let { um ->
                    fahrt.restMinuten(um)?.let { Weltmarke(if (it <= 0) "gleich fertig" else "noch ${dauer(it)}", Farben.TextLeise) }
                }
            }
            if (f.losUm != null && f.ankunftUm != null && f.lage != "Wache" && f.lage != "VorOrt") {
                Weltbalken(fahrt.anteil(f.losUm, f.ankunftUm).toFloat())
            }
        }
        fahrt.restMinuten(f.ankunftUm)?.takeIf { f.lage != "Wache" && f.lage != "VorOrt" }?.let {
            Text(dauer(it), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
        }
    }
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

    // Was die Besatzung dazugelernt hat — die Vorlage steht am Typ.
    val gelernt = (f.gelernt + lehrgaenge.filter { it.gelernt }.map { it.faehigkeit }).distinct()
    if (gelernt.isNotEmpty()) Leisesatz("Gelernt: ${gelernt.joinToString(", ")}", winzig = true)

    if (f.geliehen) {
        Leisesatz(
            "Geliehen — alarmieren geht, Rufname, Umzug, Lehrgang und Verkauf bleiben beim Besitzer.",
            winzig = true,
        )
        if (rueckgabeGefragt) {
            Rueckfrage(
                frage = "Vorzeitig zurückgeben? Es fährt heim, die Miete bleibt bezahlt.",
                ja = "Zurückgeben",
                beiJa = { tun("Das ging nicht.") { welt.wege.leiheZurueckgeben(it, f.id); welt.betriebLaden() } },
                beiNein = { rueckgabeGefragt = false },
                aktiv = !sendet,
            )
        } else {
            Knopf("Zurückgeben", { rueckgabeGefragt = true }, kompakt = true, art = Knopfart.Leise)
        }
    } else {
        // ------------------------------------------------------------ Rufname
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
            Feld(wert = rufname, beiAenderung = { rufname = it.take(40) }, etikett = "Rufname", modifier = Modifier.weight(1f))
            Knopf(if (rufname.isBlank()) "Wie im Buch" else "Übernehmen", {
                tun("Der Rufname ging nicht.", schliessen = false) { welt.wege.fahrzeugUmbenennen(it, f.id, rufname.trim()); welt.allesLaden() }
            }, kompakt = true, aktiv = !sendet && (rufname.trim() != f.funkrufname || rufname.isBlank()))
        }
        if (f.rufnameVonHand) Leisesatz("Von Hand gesetzt — leer lassen und übernehmen nimmt ihn zurück.", winzig = true)
    }

    // ------------------------------------------------------------- Einsatz
    val moegliche = zustand.lagenImBereich.filter {
        it.zustand != "Erledigt" && it.id != f.lageId && (zustand.istVormerkbar(f) || zustand.istAlarmierbar(f, it))
    }
    if (zustand.istAlarmierbar(f) || zustand.istVormerkbar(f) || moegliche.isNotEmpty()) {
        if (moegliche.isEmpty()) {
            Leisesatz("Gerade keine Lage, auf die es fahren könnte.", winzig = true)
        } else {
            Auswahl(
                etikett = if (zustand.istVormerkbar(f)) "Danach fahren zu …" else "Auf Einsatz schicken …",
                eintraege = moegliche,
                gewaehlt = zielLage,
                aufschrift = { "${it.stichwort} · ${entfernungText(it.entfernungMeter)} · ${deckungstext(it)}" },
                beiWahl = { zielLage = it },
            )
            Knopf(if (zustand.istVormerkbar(f)) "Vormerken" else "Alarmieren", {
                val l = zielLage ?: run { fehler = "Wähle den Einsatz."; return@Knopf }
                sendet = true
                bereich.launch {
                    fehler = welt.alarmieren(l.id, listOf(f.id))
                    sendet = false
                    if (fehler == null) werkbank.offenesFahrzeug = null
                }
            }, art = Knopfart.Alarm, kompakt = true, aktiv = !sendet && zielLage != null)
        }
    }
    if (f.lage == "Ausrueckt" || f.lage == "Anfahrt") {
        Knopf("Anfahrt abbrechen", {
            tun("Der Abbruch ging nicht.") { welt.wege.anfahrtAbbrechen(it, f.id); welt.betriebLaden() }
        }, kompakt = true, art = Knopfart.Leise, aktiv = !sendet)
    }

    // ------------------------------------------------------------- Streife
    if (f.streifenfaehig && !f.geliehen) Streifengriffe(welt, zustand, werkbank, f)

    if (!f.geliehen) {
        // ---------------------------------------------------------- Umsetzen
        val andere = zustand.stand?.wachen.orEmpty().filter { it.id != f.wacheId && traegtFahrzeuge(it.art) }
        if (andere.isNotEmpty() && f.lage == "Wache") {
            Auswahl(
                etikett = "Umsetzen nach …",
                eintraege = andere,
                gewaehlt = zielWache,
                aufschrift = { "${it.name} · ${it.belegt}/${it.stellplaetze}" },
                beiWahl = { zielWache = it },
            )
            Knopf(if (sendet) "Fährt …" else "Umsetzen", {
                val z = zielWache ?: run { fehler = "Wähle die Zielwache."; return@Knopf }
                tun("Der Umzug ging nicht.") { welt.wege.fahrzeugUmsetzen(it, f.id, z.id); welt.betriebLaden() }
            }, kompakt = true, aktiv = !sendet && zielWache != null)
        }

        // ---------------------------------------------------------- Lehrgang
        val einrichtungen = zustand.stand?.wachen.orEmpty().filter { it.art == "Lehrgangseinrichtung" }
        if (einrichtungen.isNotEmpty() && lehrgaenge.isNotEmpty() && f.lage == "Wache" && f.lehrgang == null) {
            val offen = lehrgaenge.filter { it.grund == null }
            Auswahl(
                etikett = if (offen.isNotEmpty()) "Zum Lehrgang …" else "Kein Lehrgang offen",
                eintraege = lehrgaenge,
                gewaehlt = lehrgang,
                aufschrift = { "${it.name} · ${zahl(it.kosten)} · ${Math.round(it.dauerMinuten / 60.0)} h" },
                unterschrift = { it.grund ?: "lernt: ${it.faehigkeit}" },
                beiWahl = { if (it.grund == null) lehrgang = it },
            )
            if (einrichtungen.size > 1) {
                Auswahl(
                    etikett = "Einrichtung",
                    eintraege = einrichtungen,
                    gewaehlt = einrichtung ?: einrichtungen.first(),
                    aufschrift = { it.name },
                    beiWahl = { einrichtung = it },
                )
            }
            lehrgang?.let { Leisesatz("Danach kann die Besatzung: ${it.faehigkeit}.", winzig = true) }
            Knopf(if (sendet) "Fährt …" else "Anmelden", {
                val l = lehrgang ?: run { fehler = "Wähle einen Lehrgang."; return@Knopf }
                val ziel = (einrichtung ?: einrichtungen.first()).id
                tun("Die Anmeldung ging nicht.") { welt.wege.zumLehrgang(it, f.id, ziel, l.id); welt.betriebLaden() }
            }, kompakt = true, aktiv = !sendet && lehrgang != null)
        }

        // ---------------------------------------------------------- Werkstatt
        werkstattwahl?.takeIf { it.preis > 0 && it.werkstaetten.isNotEmpty() }?.let { w ->
            val frei = w.werkstaetten.filter { it.beanstandung == null }
            Leisesatz(
                "Zustand ${f.zustand} %" + (if (f.verschlissen) " — nicht mehr alarmierbar" else "") +
                    ". Instandsetzung: ${zahl(w.preis)} $WAEHRUNG · ${w.dauerMinuten} min.",
            )
            Auswahl(
                etikett = "Werkstatt",
                eintraege = w.werkstaetten,
                gewaehlt = werkstatt ?: frei.firstOrNull(),
                aufschrift = { "${it.name} · ${it.belegt}/${it.buehnen}" },
                unterschrift = { it.beanstandung ?: entfernungText(it.entfernungMeter) },
                beiWahl = { if (it.beanstandung == null) werkstatt = it },
            )
            Knopf(if (frei.isEmpty()) "Bühnen belegt" else "In die Werkstatt", {
                val ziel = (werkstatt ?: frei.firstOrNull())?.id ?: run { fehler = "Wähle eine Werkstatt."; return@Knopf }
                tun("Die Fahrt in die Werkstatt ging nicht.") { welt.wege.zurWerkstatt(it, f.id, ziel); welt.betriebLaden() }
            }, kompakt = true, aktiv = !sendet && frei.isNotEmpty())
        }

        // ---------------------------------------------------------- Verkauf
        val erloes = Math.round(f.preis / 2.0 * (f.zustand / 100.0))
        if (verkaufGefragt) {
            Rueckfrage(
                frage = "${f.funkrufname} verkaufen? Zurück kommen etwa ${zahl(erloes)} $WAEHRUNG.",
                ja = "Verkaufen",
                beiJa = { tun("Der Verkauf ging nicht.") { welt.wege.fahrzeugVerkaufen(it, f.id); welt.allesLaden() } },
                beiNein = { verkaufGefragt = false },
                aktiv = !sendet,
            )
        } else if (f.lage == "Wache") {
            Knopf("Verkaufen für ${zahl(erloes)} $WAEHRUNG", { verkaufGefragt = true }, kompakt = true, art = Knopfart.Leise)
        }
    }
    Warnsatz(fehler)
}

/**
 * Die Streife eines Polizeifahrzeugs — Stationen aus den vorgeschlagenen
 * Orten, auf der Karte gesetzt oder als gespeicherte Route aufgelegt.
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

    Ueberschrift("Streife")
    Leisesatz(
        if (f.streifenpfad.isEmpty()) "Keine Streife gesetzt."
        else "Pfad: " + f.streifenpfad.joinToString(" → ") { it.name },
        winzig = true,
    )
    if (orte.isEmpty()) {
        Leisesatz("Keine Orte in der Nähe vorgeschlagen — setz die Stationen auf der Karte.", winzig = true)
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
    entwurf.forEachIndexed { i, s ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text("${i + 1}", style = Schrift.MonoKlein, color = Farben.Amber)
            Text(s.name, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Knopf("↑", {
                if (i > 0) werkbank.pfad = entwurf.toMutableList().also { l -> l[i] = l[i - 1].also { l[i - 1] = l[i] } }
            }, kompakt = true, art = Knopfart.Leise, aktiv = i > 0)
            Knopf("✕", { werkbank.pfad = entwurf.filterIndexed { j, _ -> j != i } }, kompakt = true, art = Knopfart.Leise)
        }
    }
    Umbruchreihe {
        Knopf("Auf der Karte setzen", { werkbank.karteWaehlen(Kartenmodus.Pfad) }, kompakt = true)
        if (entwurf.isNotEmpty()) Knopf("Leeren", { werkbank.pfad = emptyList() }, kompakt = true, art = Knopfart.Leise)
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
        }, kompakt = true, art = Knopfart.Haupt, aktiv = !sendet)
    }
    if (zustand.streifenrouten.isNotEmpty()) {
        Auswahl(
            etikett = "Route auflegen …",
            eintraege = zustand.streifenrouten,
            gewaehlt = route,
            aufschrift = { "${it.name} · ${it.stationen.size} ${if (it.stationen.size == 1) "Station" else "Stationen"}" },
            beiWahl = { route = it },
        )
        Umbruchreihe {
            Knopf("Auflegen", {
                val r = route ?: return@Knopf
                sendet = true
                bereich.launch {
                    fehler = welt.streifenrouteAuflegen(f.id, r.id)
                    sendet = false
                    if (fehler == null) werkbank.pfad = r.stationen
                }
            }, kompakt = true, aktiv = route != null && !sendet)
            Knopf("Route löschen", {
                val r = route ?: return@Knopf
                bereich.launch {
                    fehler = welt.streifenrouteEntfernen(r.id)
                    route = null
                }
            }, kompakt = true, art = Knopfart.Leise, aktiv = route != null)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
        Feld(wert = routenName, beiAenderung = { routenName = it.take(40) }, etikett = "Als Route sichern", platzhalter = "Innenstadt Nacht", modifier = Modifier.weight(1f))
        Knopf("Sichern", {
            if (entwurf.isEmpty()) { fehler = "Eine Route ohne Stationen ist keine."; return@Knopf }
            if (routenName.isBlank()) { fehler = "Die Route braucht einen Namen."; return@Knopf }
            sendet = true
            bereich.launch {
                fehler = welt.streifenrouteSichern(routenName.trim(), entwurf)
                sendet = false
                if (fehler == null) routenName = ""
            }
        }, kompakt = true, aktiv = !sendet)
    }
    Warnsatz(fehler)
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
