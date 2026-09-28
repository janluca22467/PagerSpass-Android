package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Begleiterstand
import de.pagerspass.pagerspass.mobil.Nebenleitung
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.mobil.rememberTonstand
import de.pagerspass.pagerspass.netz.Funkzeile
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Zeichenknopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================ Funkpartner

/**
 * Wer zuletzt Gesprächspartner am Funk war — `funkpartner` im Store.
 *
 * Sobald ein Fahrzeug der Leitstelle antwortet (auch nur „Kommen"), muss sie den
 * Rufnamen nicht in jeder weiteren Zeile wiederholen. Nach drei Minuten Stille
 * oder einem schließenden „Ende" gilt das Gespräch als beendet — danach geht ein
 * Spruch ohne Rufnamen als Rundruf hinaus.
 */
internal class Funkpartner {
    var name by mutableStateOf<String?>(null)
        private set
    private var seit by mutableLongStateOf(0L)

    fun merken(rufname: String?) {
        name = rufname
        seit = System.currentTimeMillis()
    }

    /** Wem ein jetzt getippter Spruch ohne eigenen Rufnamen gälte. */
    fun jetzt(): String? = name?.takeIf { System.currentTimeMillis() - seit < GESPRAECHSFENSTER_MS }

    fun loesen() = merken(null)

    companion object {
        const val GESPRAECHSFENSTER_MS = 3 * 60 * 1000L
    }
}

/** Wörter, nach denen ein „Ende" kein Gesprächsschluss ist, sondern Teil des Satzes. */
private val KEIN_SCHLUSS_DAVOR = setOf("am", "im", "zum", "ans", "ins", "das", "zu", "bis", "gegen", "vor", "nach", "seit")

/** Ob dieser Spruch das Gespräch schließt — ein „Ende" am Satzende. */
internal fun beendetGespraech(text: String): Boolean {
    val woerter = text.lowercase().split(Regex("[^a-zäöüß]+")).filter { it.isNotEmpty() }
    if (woerter.lastOrNull() != "ende") return false
    val davor = woerter.getOrNull(woerter.size - 2)
    return davor == null || davor !in KEIN_SCHLUSS_DAVOR
}

/**
 * Den Funkpartner mitführen — über alle Teile des Tischs hinweg, auch wenn der
 * Funk gerade nicht vorn liegt.
 */
@Composable
internal fun rememberFunkpartner(stand: Rundenstand): Funkpartner {
    val partner = remember { Funkpartner() }
    val letzte = stand.funk.lastOrNull()
    val raum = stand.raum
    LaunchedEffect(letzte?.id) {
        letzte ?: return@LaunchedEffect
        // Auch Lagemeldung und Nachforderung sind Wortmeldungen an die Leitstelle.
        if (letzte.kind != "Funk" && letzte.kind != "Lagemeldung" && letzte.kind != "Nachforderung") {
            return@LaunchedEffect
        }
        if (letzte.an != raum?.settings?.leitstelle) return@LaunchedEffect
        if (raum?.vehicles?.none { it.funkrufname == letzte.von } != false) return@LaunchedEffect
        partner.merken(if (beendetGespraech(letzte.text)) null else letzte.von)
    }
    return partner
}

/**
 * Wen der Text meint, wenn kein Empfänger gewählt wurde — zuerst die Kennung,
 * wie sie im Tableau steht, dann der volle Funkrufname; der längste Treffer
 * gewinnt („1/RTW-1" steckt in „11/RTW-1").
 */
private fun zielAusText(text: String, raum: Raumzustand?, kennung: (de.pagerspass.pagerspass.netz.Rundenfahrzeug) -> String): String? {
    val t = text.lowercase()
    val fahrzeuge = raum?.vehicles.orEmpty()
    fahrzeuge
        .filter { kennung(it).isNotBlank() && t.contains(kennung(it).lowercase()) }
        .maxByOrNull { kennung(it).length }
        ?.let { return it.funkrufname }
    return fahrzeuge
        .filter { it.funkrufname.isNotBlank() && t.contains(it.funkrufname.lowercase()) }
        .maxByOrNull { it.funkrufname.length }
        ?.funkrufname
}

// ============================================================ Funkteil

/**
 * Der Funk der Leitstelle — das rechte Feld aus `LeitstelleView.vue` im
 * Handyzweig: eine Reihe mit den Leitungen (Funkverkehr, Leitstellen, das
 * Handfunkgerät), darunter die gewählte.
 *
 * <b>Liegt der Funk auf dem gekoppelten Handy</b>, trägt das Feld nur noch den
 * Draht — der Funkbegleiter trägt ausdrücklich nur Funk und Melder.
 */
@Composable
internal fun Leitstellenfunk(
    stand: Rundenstand,
    raum: Raumzustand,
    katalog: Katalog?,
    partner: Funkpartner,
    leitung: Funkleitung,
    beiLeitung: (Funkleitung) -> Unit,
    amDraht: Boolean,
    funkAmHandy: Boolean,
    nurDraht: Boolean,
    ungelesenFunk: Int,
    ungelesenDraht: Int,
    griffe: LeitstellenGriffe,
    modifier: Modifier = Modifier,
) {
    val leitungen = buildList {
        if (!nurDraht) add(Funkleitung.Funk)
        if (amDraht) add(Funkleitung.Draht)
        if (!funkAmHandy) add(Funkleitung.Geraet)
    }

    // Schließt sich der Draht oder wandert der Funk aufs Handy, steht man sonst
    // vor einem Reiter, den es nicht mehr gibt.
    LaunchedEffect(leitungen) {
        if (leitung !in leitungen) beiLeitung(leitungen.firstOrNull() ?: Funkleitung.Funk)
    }

    Column(modifier = modifier) {
        if (funkAmHandy && !amDraht) {
            Leerteil("Funk und Einzelruf liegen auf dem gekoppelten Handy.")
            return@Column
        }
        if (leitungen.size > 1) {
            Reiterreihe(Modifier.padding(horizontal = Abstand.Normal)) {
                leitungen.forEach { l ->
                    Reiter(
                        aufschrift = l.wort,
                        offen = leitung == l,
                        beiDruck = { beiLeitung(l) },
                        marke = when (l) {
                            Funkleitung.Funk -> ungelesenFunk
                            Funkleitung.Draht -> ungelesenDraht
                            Funkleitung.Geraet -> 0
                        },
                    )
                }
            }
        }

        when (leitung) {
            Funkleitung.Funk -> Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.weight(1f).padding(Abstand.Normal),
            ) {
                Kanalzeile(stand, raum, partner, griffe)
                Funkchat(stand, raum, partner, griffe, Modifier.weight(1f))
                Sprechtaste(
                    sendet = stand.sendet,
                    wirdVerstanden = stand.wirdVerstanden,
                    belegtVon = stand.sprecher[raum.sendegruppe ?: ""],
                    gesperrtBis = stand.funkGesperrtBis,
                    beiDruck = griffe.sprechStart,
                    beiLoslassen = griffe.sprechEnde,
                )
            }

            Funkleitung.Draht -> Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.weight(1f).padding(Abstand.Normal),
            ) {
                Drahtchat(stand, raum, griffe, Modifier.weight(1f))
            }

            Funkleitung.Geraet -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Abstand.Normal),
            ) {
                Handfunkgeraet(stand, raum, griffe)
            }
        }
    }
}

/**
 * Die Kanalzeile — `Kanalzeile.vue`: wer gerade spricht, auf welcher Gruppe man
 * sendet, und an der Leitstelle die Gruppen selbst: mithören an/aus, senden auf.
 */
@Composable
private fun Kanalzeile(stand: Rundenstand, raum: Raumzustand, partner: Funkpartner, griffe: LeitstellenGriffe) {
    val sendegruppe = raum.sendegruppe
    val belegt = stand.sprecher[sendegruppe ?: ""]
    val getrennt = raum.settings.funkgruppen.isNotEmpty()
    val gehoert = raum.gehoerteFunkgruppen

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(8.dp).background(
                    when {
                        stand.sendet -> Farben.SignalHell
                        belegt != null -> Farben.AmberHell
                        else -> Farben.GruenHell
                    },
                    CircleShape,
                ),
            )
            Text(
                when {
                    stand.sendet -> "Du sendest"
                    belegt != null -> "$belegt spricht"
                    else -> "Kanal frei"
                },
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
            if (getrennt) {
                SehrLeise("auf ${funkgruppeVon(raum, sendegruppe)?.bezeichnung ?: "Kreiskanal"}", mono = true)
            }
        }

        if (getrennt) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                kreisfunkgruppen(raum).forEach { g ->
                    val hoert = g.id in gehoert
                    val sendet = g.id == sendegruppe
                    val sprecher = stand.sprecher[g.id]
                    val farbe = Farben.kanal(funkgruppeRang(raum, g.id))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(if (sendet) farbe.copy(alpha = 0.16f) else Farben.FlaecheHoch, Rundung.Klein)
                            .border(1.dp, if (hoert) farbe else Farben.Rand, Rundung.Klein),
                    ) {
                        Zeichenknopf(
                            beiDruck = {
                                // Die letzte gehörte Gruppe bleibt — ganz taub geht nicht.
                                if (hoert && gehoert.size <= 1) return@Zeichenknopf
                                val neu = if (hoert) gehoert - g.id else gehoert + g.id
                                val senden = sendegruppe?.takeIf { it in neu }
                                griffe.funkgruppenPlatz(neu, senden)
                            },
                            beschreibung = if (hoert) "${g.bezeichnung} wird mitgehört — antippen zum Abgeben" else "${g.bezeichnung} dazunehmen",
                            art = Knopfart.Leise,
                            kompakt = true,
                        ) {
                            Icon(
                                if (hoert) Leitstellenzeichen.Laut else Leitstellenzeichen.Stumm,
                                contentDescription = null,
                                tint = if (hoert) farbe else Farben.TextSehrLeise,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .clickable(enabled = hoert && !sendet) { griffe.funkgruppenPlatz(gehoert, g.id) }
                                .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                        ) {
                            Text(
                                g.marke.ifBlank { g.name },
                                style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                                color = if (hoert) farbe else Farben.TextSehrLeise,
                            )
                            Text(
                                when {
                                    sendet -> "Du sendest"
                                    sprecher != null -> "$sprecher spricht"
                                    else -> "frei"
                                },
                                style = Schrift.Winzig,
                                color = Farben.TextLeise,
                            )
                        }
                    }
                }
            }
        }

        partner.jetzt()?.let { p ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Im Gespräch mit $p", style = Schrift.MonoKlein, color = Farben.AmberHell, modifier = Modifier.weight(1f))
                Knopf("beenden", { partner.loesen() }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
}

/**
 * Der Funkverkehr — `FunkChat.vue`: das Protokoll mit Gruppenmarke, wer an wen,
 * und darunter die Eingabe. <b>„@" wählt den Empfänger</b>, und der Vorlese-
 * Schalter lässt getippte Sprüche auf dem Kanal vorlesen.
 */
@Composable
private fun Funkchat(
    stand: Rundenstand,
    raum: Raumzustand,
    partner: Funkpartner,
    griffe: LeitstellenGriffe,
    modifier: Modifier = Modifier,
) {
    val kennung = rememberKennung(raum)
    val meinRufname = raum.settings.leitstelle
    var eingabe by remember { mutableStateOf("") }
    var gewaehltesZiel by remember { mutableStateOf<String?>(null) }
    val rollen = rememberScrollState()
    val bereich = androidx.compose.runtime.rememberCoroutineScope()
    val getrennt = raum.settings.funkgruppen.isNotEmpty()
    val vorlesen = stand.ich?.funkVorlesen == true
    val zeilen = stand.funk

    // Mitrollen, solange man unten ist — wer hochgerollt hat, liest und wird
    // nicht weggerissen; dann steht „Neuer Funkverkehr ↓" da.
    var untenBeiLetztem by remember { mutableStateOf(true) }
    var neueUnten by remember { mutableStateOf(false) }
    LaunchedEffect(zeilen.size) {
        if (untenBeiLetztem) rollen.animateScrollTo(rollen.maxValue) else neueUnten = true
    }
    LaunchedEffect(rollen.value, rollen.maxValue) {
        untenBeiLetztem = rollen.value >= rollen.maxValue - 40
        if (untenBeiLetztem) neueUnten = false
    }

    // Das angefangene Wort nach „@" — daraus werden die Vorschläge.
    val token = Regex("@([^\\s@]*)$").find(eingabe)?.groupValues?.get(1)
    val rufnamen = buildList {
        raum.vehicles.forEach { v -> add(v.funkrufname to kennung(v).takeIf { it != v.funkrufname }.orEmpty()) }
    }.filter { it.first.isNotBlank() && it.first != meinRufname }
    val vorschlaege = token?.let { t ->
        rufnamen.filter { (name, zusatz) ->
            name.lowercase().contains(t.lowercase()) || zusatz.lowercase().contains(t.lowercase())
        }.take(6)
    }.orEmpty()

    fun senden() {
        val text = eingabe.trim()
        if (text.isEmpty()) return
        val ziel = gewaehltesZiel
        val an = ziel?.takeIf { text.contains(it) }
            ?: zielAusText(text, raum, kennung)
            ?: partner.jetzt()
        if (beendetGespraech(text)) partner.merken(null) else if (an != null) partner.merken(an)
        griffe.funken(text, an)
        eingabe = ""
        gewaehltesZiel = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = modifier) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(farbe = Farben.BgTief, ecke = 9.dp)
                    .verticalScroll(rollen)
                    .padding(Abstand.Normal),
            ) {
                if (zeilen.isEmpty()) SehrLeise("Noch kein Funkverkehr.", mono = true)
                zeilen.forEach { z -> Funkzeilenanzeige(z, raum, meinRufname, getrennt) }
            }
            if (neueUnten) {
                Knopf(
                    "Neuer Funkverkehr ↓",
                    {
                        neueUnten = false
                        bereich.launch { rollen.animateScrollTo(rollen.maxValue) }
                    },
                    kompakt = true,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(Abstand.Klein),
                )
            }
        }

        if (vorschlaege.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                vorschlaege.forEach { (name, zusatz) ->
                    Pille(
                        aufschrift = "@$name" + if (zusatz.isNotEmpty()) " · $zusatz" else "",
                        an = false,
                        beiDruck = {
                            eingabe = eingabe.replace(Regex("@([^\\s@]*)$"), "$name ")
                            gewaehltesZiel = name
                        },
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Feld(
                wert = eingabe,
                beiAenderung = { eingabe = it.take(280) },
                platzhalter = "Funkspruch … (@ wählt den Empfänger)",
                weiterTaste = ImeAction.Send,
                modifier = Modifier.weight(1f),
            )
            Zeichenknopf(
                beiDruck = { griffe.funkVorlesen(!vorlesen) },
                beschreibung = if (vorlesen) {
                    "Vorlesen abschalten — deine Funksprüche werden gerade auf dem Kanal vorgelesen"
                } else {
                    "Getippte Funksprüche vorlesen lassen, damit die anderen sie hören"
                },
                art = if (vorlesen) Knopfart.Haupt else Knopfart.Leise,
                kompakt = true,
            ) {
                Icon(
                    if (vorlesen) Leitstellenzeichen.VorlesenAn else Leitstellenzeichen.VorlesenAus,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Knopf("Senden", { senden() }, aktiv = eingabe.isNotBlank(), kompakt = true)
        }
    }
}

/** Eine Zeile im Protokoll — Zeit, Gruppenmarke, von → an, Text in der Farbe der Art. */
@Composable
private fun Funkzeilenanzeige(z: Funkzeile, raum: Raumzustand, meinRufname: String?, getrennt: Boolean) {
    val farbe = when (z.kind) {
        "System" -> if (z.von == "PagerSpass-Team") Farben.ViolettHell else Farben.TextSehrLeise
        "Alarm" -> Farben.SignalHell
        "Lagemeldung" -> Farben.AmberHell
        "Nachforderung" -> Farben.OrangeHell
        "Sprache" -> Farben.BlauHell
        else -> Farben.Text
    }
    val anMich = z.an != null && z.an == meinRufname
    val gruppe = if (getrennt) funkgruppeVon(raum, z.funkgruppe) else null

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (anMich) Modifier.background(Farben.HauchAmber, Rundung.Winzig) else Modifier),
    ) {
        Text(uhrzeit(z.zeit), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
        gruppe?.let { g ->
            Text(
                g.marke.ifBlank { g.name },
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                color = Farben.kanal(funkgruppeRang(raum, g.id)),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                z.von + (z.an?.let { " → $it" } ?: ""),
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                color = if (anMich) Farben.Amber else Farben.TextSehrLeise,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(z.text, style = Schrift.Klein, color = farbe)
        }
    }
}

/**
 * Der Leitstellendraht — `DrahtChat.vue`. Nur die Leitstellen lesen hier mit;
 * der Funk hört nicht zu. Gesprochen wird mit der eigenen Taste darunter.
 */
@Composable
private fun Drahtchat(stand: Rundenstand, raum: Raumzustand, griffe: LeitstellenGriffe, modifier: Modifier = Modifier) {
    val ich = stand.eigeneKennung
    val zeilen = stand.drahtGesamt
    val selbst = stand.nebenleitung == Nebenleitung.Draht
    val fremd = stand.drahtSprecher?.takeIf { it.playerId != ich }
    var eingabe by remember { mutableStateOf("") }
    val rollen = rememberScrollState()

    LaunchedEffect(zeilen.size) { rollen.animateScrollTo(rollen.maxValue) }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(8.dp).background(
                    when {
                        selbst -> Farben.SignalHell
                        fremd != null -> Farben.AmberHell
                        else -> Farben.GruenHell
                    },
                    CircleShape,
                ),
            )
            Text(
                when {
                    selbst -> "Du sprichst"
                    fremd != null -> "${fremd.name} spricht"
                    else -> "Draht frei"
                },
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .flaeche(farbe = Farben.BgTief, ecke = 9.dp)
                .verticalScroll(rollen)
                .padding(Abstand.Normal),
        ) {
            if (zeilen.isEmpty()) SehrLeise("Nur ihr am Leitstellentisch lest hier mit. Der Funk hört nicht zu.")
            zeilen.forEach { n ->
                val eigen = n.vonId == ich
                Column(
                    horizontalAlignment = if (eigen) Alignment.End else Alignment.Start,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (!eigen) {
                        Text(n.vonName, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                    }
                    Text(
                        if (n.nurGesprochen) "🎙️ ${n.text.ifBlank { "Sprachdurchsage" }}" else n.text,
                        style = Schrift.Klein,
                        color = if (n.nurGesprochen) Farben.TextLeise else Farben.Text,
                        textAlign = if (eigen) TextAlign.End else TextAlign.Start,
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .background(if (eigen) Farben.HauchAmber else Farben.FlaecheHoch, Rundung.Klein)
                            .padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig),
                    )
                    Text(uhrzeit(n.zeit), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Feld(
                wert = eingabe,
                beiAenderung = { eingabe = it.take(500) },
                platzhalter = "An die anderen Leitstellen …",
                weiterTaste = ImeAction.Send,
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "Senden",
                {
                    griffe.draht(eingabe.trim())
                    eingabe = ""
                },
                aktiv = eingabe.isNotBlank(),
                kompakt = true,
            )
        }
        Sprechtaste(
            sendet = selbst,
            wirdVerstanden = false,
            belegtVon = fremd?.name,
            gesperrtBis = null,
            beiDruck = griffe.drahtSprechStart,
            beiLoslassen = griffe.drahtSprechEnde,
        )
    }
}

// ======================================================== Handfunkgerät

private enum class Menueseite { Zu, Wurzel, Einzelruf, Gruppen, Rufe, Lautstaerke }

private data class Menueeintrag(val id: String, val name: String, val wert: String = "")

private val LAUTSTUFEN = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

/**
 * Das Handfunkgerät — `FunkgeraetVoll.vue` am Leitstellenplatz.
 *
 * Display, Steuerkreuz, grüne und rote Taste, Ziffernfeld. Grün nimmt einen
 * Einzelruf an oder schaltet auf die getippte Gruppennummer; rot legt auf oder
 * geht zurück; „#" öffnet den Einzelruf, die Mitte das Menü (Rufgruppen,
 * Einzelruf, letzte Rufe, Lautstärke). Die Notruftaste trägt nur ein
 * Fahrzeuggerät — am Leitstellentisch gibt es keine.
 */
@Composable
private fun Handfunkgeraet(stand: Rundenstand, raum: Raumzustand, griffe: LeitstellenGriffe) {
    val ton by rememberTonstand()
    val mitMikrofon = rememberMikrofonfrage()
    val ich = stand.eigeneKennung
    val istLeitstelle = stand.ich?.istLeitstelle == true
    val meinRufname = raum.settings.leitstelle.orEmpty()
    val gruppen = kreisfunkgruppen(raum)
    val geschaltet = raum.sendegruppe
    val eigeneGruppe = funkgruppeVon(raum, geschaltet)
    val gruppenwort = eigeneGruppe?.bezeichnung ?: "Kreiskanal"
    val sprecher = stand.sprecher[geschaltet ?: ""]
    val rufe = raum.einzelrufe
    val laufend = rufe.firstOrNull { it.laeuft && (it.vonPlayerId == ich || it.angenommenVonPlayerId == ich) }
    val eingehend = rufe.firstOrNull {
        it.klingelt && (it.zielPlayerId == ich || (it.zielPlayerId == null && istLeitstelle))
    }
    val ausgehend = rufe.firstOrNull { it.klingelt && it.vonPlayerId == ich }
    val jetzt = rememberJetzt()

    var seite by remember { mutableStateOf(Menueseite.Zu) }
    var index by remember { mutableIntStateOf(0) }
    var eingabe by remember { mutableStateOf("") }
    var hinweis by remember { mutableStateOf("") }

    LaunchedEffect(hinweis) {
        if (hinweis.isNotEmpty()) {
            delay(2_200)
            hinweis = ""
        }
    }

    fun gruppeWechseln(id: String?) {
        id ?: return
        griffe.funkgruppenPlatz((raum.gehoerteFunkgruppen + id).distinct(), id)
    }

    val letzteRufe = stand.funk.asReversed()
        .asSequence()
        .filter { it.kind == "Funk" && it.von.isNotBlank() && it.von != meinRufname }
        .filter { it.funkgruppe == null || it.funkgruppe == geschaltet }
        .map { it.von }
        .distinct()
        .take(6)
        .toList()

    val einzelrufziele = listOf<Pair<String?, String>>(null to "Leitstelle") +
        raum.vehicles
            .filter { v -> v.playerId != null && v.playerId != ich && raum.players.any { it.id == v.playerId } }
            .map { it.id to it.funkrufname }

    val eintraege: List<Menueeintrag> = when (seite) {
        Menueseite.Wurzel -> listOf(
            Menueeintrag("gruppen", "Rufgruppen", eigeneGruppe?.nummer.orEmpty()),
            Menueeintrag("einzelruf", "Einzelruf"),
            Menueeintrag("rufe", "Letzte Rufe"),
            Menueeintrag("lautstaerke", "Lautstärke", "${(ton.funk * 100).toInt()} %"),
        )
        Menueseite.Einzelruf -> einzelrufziele.map { (id, name) -> Menueeintrag(id ?: "ls", name) }
        Menueseite.Gruppen -> gruppen.map { g ->
            Menueeintrag(g.id, g.bezeichnung, if (geschaltet == g.id) "●" else g.nummer)
        }
        Menueseite.Rufe -> letzteRufe.mapIndexed { i, n -> Menueeintrag("r$i", n) }
            .ifEmpty { listOf(Menueeintrag("leer", "Noch nichts gehört")) }
        Menueseite.Lautstaerke -> LAUTSTUFEN.map { s ->
            Menueeintrag(s.toString(), "${(s * 100).toInt()} %", if (kotlin.math.abs(ton.funk - s) < 0.01f) "●" else "")
        }
        Menueseite.Zu -> emptyList()
    }
    val menueOffen = seite != Menueseite.Zu
    val titel = when (seite) {
        Menueseite.Einzelruf -> "Einzelruf wählen"
        Menueseite.Gruppen -> "Rufgruppen"
        Menueseite.Rufe -> "Letzte Rufe"
        Menueseite.Lautstaerke -> "Lautstärke"
        else -> "Menü"
    }

    fun oeffnen(s: Menueseite) {
        seite = s
        index = 0
    }

    fun waehlen() {
        val e = eintraege.getOrNull(index) ?: return
        when (seite) {
            Menueseite.Wurzel -> oeffnen(
                when (e.id) {
                    "gruppen" -> Menueseite.Gruppen
                    "einzelruf" -> Menueseite.Einzelruf
                    "rufe" -> Menueseite.Rufe
                    else -> Menueseite.Lautstaerke
                },
            )
            Menueseite.Einzelruf -> {
                val ziel = if (e.id == "ls") null else e.id
                mitMikrofon { griffe.einzelrufStarten(ziel) }
                seite = Menueseite.Zu
            }
            Menueseite.Gruppen -> {
                gruppeWechseln(e.id)
                seite = Menueseite.Zu
            }
            Menueseite.Lautstaerke -> e.id.toFloatOrNull()?.let { griffe.tonSetzen(ton.copy(funk = it)) }
            else -> seite = Menueseite.Zu
        }
    }

    fun bewegen(schritt: Int) {
        val n = eintraege.size
        if (n == 0) return
        index = ((index + schritt) % n + n) % n
    }

    fun blaettern(richtung: Int) {
        if (gruppen.isEmpty()) return
        val aktuell = gruppen.indexOfFirst { it.id == geschaltet }
        val naechste = gruppen[((aktuell + richtung) % gruppen.size + gruppen.size) % gruppen.size]
        gruppeWechseln(naechste.id)
    }

    fun gruen() {
        if (eingehend != null) {
            val ruf = eingehend.id
            mitMikrofon { griffe.einzelrufAnnehmen(ruf) }
            return
        }
        if (menueOffen) {
            waehlen()
            return
        }
        if (eingabe.isEmpty()) return
        val g = raum.settings.funkgruppen.firstOrNull { it.nummer == eingabe }
        if (g == null) hinweis = "KEIN EINTRAG" else gruppeWechseln(g.id)
        eingabe = ""
    }

    fun rot() {
        when {
            laufend != null -> griffe.einzelrufBeenden(laufend.id)
            eingehend != null -> griffe.einzelrufAbweisen(eingehend.id)
            ausgehend != null -> griffe.einzelrufBeenden(ausgehend.id)
            menueOffen -> {
                seite = if (seite == Menueseite.Wurzel) Menueseite.Zu else Menueseite.Wurzel
                index = 0
            }
            else -> eingabe = ""
        }
    }

    fun taste(z: String) {
        when (z) {
            "#" -> oeffnen(Menueseite.Einzelruf)
            "*" -> Unit
            else -> if (eingabe.length < 8) eingabe += z
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .background(Color(0xFF1B1D22), Rundung.Normal)
            .border(1.dp, Farben.RandHell, Rundung.Normal)
            .padding(Abstand.Gross),
    ) {
        Text("HRT-90", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)

        // Das Display.
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp)
                .background(Color(0xFF9DB08A), Rundung.Klein)
                .padding(Abstand.Normal),
        ) {
            val lcd = Color(0xFF1C2416)
            Text(
                when {
                    laufend != null -> "Einzelruf"
                    menueOffen -> titel
                    else -> "Gruppenruf"
                },
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                color = lcd,
            )
            when {
                laufend != null -> {
                    val gegenstelle = if (laufend.vonPlayerId == ich) laufend.zielName else laufend.vonName
                    Text(gegenstelle, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = lcd)
                    val beginn = zeitMillis(laufend.angenommenUm)
                    Text(
                        if (beginn == null) "0:00" else minSek((jetzt - beginn) / 1000),
                        style = Schrift.MonoKlein,
                        color = lcd,
                    )
                }
                menueOffen -> eintraege.forEachIndexed { i, e ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (i == index) lcd else Color.Transparent)
                            .clickable {
                                index = i
                                waehlen()
                            }
                            .padding(horizontal = Abstand.Winzig),
                    ) {
                        Text(
                            e.name,
                            style = Schrift.MonoKlein,
                            color = if (i == index) Color(0xFF9DB08A) else lcd,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (e.wert.isNotEmpty()) {
                            Text(e.wert, style = Schrift.MonoKlein, color = if (i == index) Color(0xFF9DB08A) else lcd)
                        }
                    }
                }
                else -> {
                    Text(gruppenwort, style = Schrift.MonoKlein, color = lcd, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            stand.sendet -> "SPRECHEN"
                            hinweis.isNotEmpty() -> hinweis
                            eingabe.isNotEmpty() -> eingabe
                            ausgehend != null -> "${ausgehend.zielName} …"
                            eingehend != null -> "${eingehend.vonName} ruft"
                            sprecher != null -> sprecher
                            else -> "— — —"
                        },
                        style = Schrift.Gross.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                        color = lcd,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(meinRufname, style = Schrift.MonoKlein, color = lcd, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Steuerkreuz zwischen grüner und roter Taste.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Geraetetaste("✆", Farben.Gruen, "Annehmen / Ausführen") { gruen() }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Geraetetaste("▲", null, "Im Menü nach oben") { if (menueOffen) bewegen(-1) }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    Geraetetaste("◀", null, "Vorige Funkgruppe") { blaettern(-1) }
                    Geraetetaste("≡", null, if (menueOffen) "Auswählen" else "Menü öffnen") {
                        if (menueOffen) waehlen() else oeffnen(Menueseite.Wurzel)
                    }
                    Geraetetaste("▶", null, "Nächste Funkgruppe") { blaettern(1) }
                }
                Geraetetaste("▼", null, "Im Menü nach unten") { if (menueOffen) bewegen(1) }
            }
            Geraetetaste("✆", Farben.Signal, "Auflegen / Zurück") { rot() }
        }

        // Das Ziffernfeld.
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#")).forEach { reihe ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                reihe.forEach { z -> Geraetetaste(z, null, "Taste $z", breit = true) { taste(z) } }
            }
        }

        Sprechtaste(
            sendet = stand.sendet,
            wirdVerstanden = stand.wirdVerstanden,
            belegtVon = sprecher,
            gesperrtBis = stand.funkGesperrtBis,
            beiDruck = griffe.sprechStart,
            beiLoslassen = griffe.sprechEnde,
        )
    }
}

@Composable
private fun Geraetetaste(
    zeichen: String,
    farbe: Color?,
    beschreibung: String,
    breit: Boolean = false,
    beiDruck: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = if (breit) 64.dp else 44.dp, minHeight = 44.dp)
            .background(farbe?.copy(alpha = 0.85f) ?: Color(0xFF2A2D34), Rundung.Klein)
            .border(1.dp, Farben.Rand, Rundung.Klein)
            .clickable(onClick = beiDruck)
            .padding(horizontal = Abstand.Klein),
    ) {
        Text(zeichen, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
    }
}

// ============================================================ Einzelruf

/**
 * Die Einzelrufleiste der Leitstelle — dieselbe Bauform wie am Begleiter
 * (`Einzelrufleiste`), gefüttert aus dem Rundenstand.
 *
 * <b>Vollduplex mit Menschen:</b> Das Mikrofon geht mit der Annahme an und mit
 * dem Auflegen aus — ohne Taste. Mit einer Bot-Besatzung gilt die rastende
 * Taste der Leiste, und der Server erkennt den Satz.
 */
@Composable
internal fun LeitstellenEinzelruf(stand: Rundenstand, griffe: LeitstellenGriffe, modifier: Modifier = Modifier) {
    val ton by rememberTonstand()
    val aktuelleGriffe by rememberUpdatedState(griffe)
    val ruf = stand.eigenerEinzelruf?.takeIf { it.laeuft }
    val menschlich = ruf != null && !ruf.mitBot

    LaunchedEffect(ruf?.id, menschlich) {
        aktuelleGriffe.einzelrufMikrofon(menschlich)
    }
    DisposableEffect(Unit) {
        onDispose { aktuelleGriffe.einzelrufMikrofon(false) }
    }

    val begleiterstand = Begleiterstand(
        raum = stand.raum,
        spielerId = stand.eigeneKennung,
        istFunkplatz = true,
        einzelrufHoert = stand.nebenleitung == Nebenleitung.Einzelruf && ruf?.mitBot == true,
        einzelrufErkennung = stand.einzelrufWirdVerstanden,
        tonFunk = ton.funk,
        stumm = ton.stumm,
    )
    Einzelrufleiste(
        stand = begleiterstand,
        griffe = BegleiterGriffe(
            einzelrufStarten = griffe.einzelrufStarten,
            einzelrufAnnehmen = griffe.einzelrufAnnehmen,
            einzelrufAbweisen = griffe.einzelrufAbweisen,
            einzelrufBeenden = griffe.einzelrufBeenden,
            einzelrufSagen = griffe.einzelrufSagen,
            einzelrufSprechenStarten = griffe.einzelrufSprechenStarten,
            einzelrufSprechenBeenden = griffe.einzelrufSprechenBeenden,
            einzelrufZulassen = griffe.einzelrufZulassen,
        ),
        modifier = modifier,
    )
}
