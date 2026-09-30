package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import de.pagerspass.pagerspass.ansichten.Sprechtaste
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.WeltChatzeile
import de.pagerspass.pagerspass.netz.WeltEinstellung
import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltKassenblatt
import de.pagerspass.pagerspass.netz.WeltLeihangebot
import de.pagerspass.pagerspass.netz.WeltLeihstand
import de.pagerspass.pagerspass.netz.WeltRangliste
import de.pagerspass.pagerspass.netz.WeltWache
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Ein Regler in der Farbe der App — für Tage, Preise und die drei Schrauben. */
@Composable
fun Regler(wert: Int, bereich: IntRange, schritt: Int, beiAenderung: (Int) -> Unit, modifier: Modifier = Modifier) {
    val stufen = ((bereich.last - bereich.first) / schritt.coerceAtLeast(1) - 1).coerceAtLeast(0)
    Slider(
        value = wert.coerceIn(bereich).toFloat(),
        onValueChange = { v ->
            val gerundet = bereich.first + Math.round((v - bereich.first) / schritt) * schritt
            beiAenderung(gerundet.coerceIn(bereich))
        },
        valueRange = bereich.first.toFloat()..bereich.last.coerceAtLeast(bereich.first + 1).toFloat(),
        steps = stufen,
        colors = SliderDefaults.colors(
            thumbColor = Farben.Amber,
            activeTrackColor = Farben.Amber,
            inactiveTrackColor = Farben.FlaecheAktiv,
            activeTickColor = Farben.AmberTief,
            inactiveTickColor = Farben.Rand,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

// ===================================================================== Chat

/**
 * Der Chat der Welt — das Gegenstück zu `ChatBlende.vue`, samt Funkgerät.
 *
 * <b>Direkt ist eine Antwort, kein eigener Bereich.</b> Wer auf eine Zeile
 * tippt, schreibt dem Absender direkt; „An alle“ nimmt es zurück. Das
 * Funkgerät heißt „hören“ — sprechen geht auch, wenn es aus ist.
 */
@Composable
fun ChatSeite(welt: Welt, zustand: Weltzustand) {
    var entwurf by remember { mutableStateOf("") }
    var an by remember { mutableStateOf<Pair<String, String>?>(null) }

    DisposableEffect(Unit) {
        welt.chatGeoeffnet(true)
        onDispose {
            welt.chatGeoeffnet(false)
            welt.sprechenBeenden()
        }
    }

    Schalterzeile(
        titel = "Funkgerät",
        an = zustand.funkgeraet,
        beiWechsel = { welt.funkgeraetSetzen(it) },
        unterzeile = if (zustand.funkgeraet) "Du hörst den Sprechfunk der Welt." else "Aus — du hörst nichts, sprechen geht trotzdem.",
    )
    zustand.spricht?.let { Text("Spricht: ${it.name ?: "jemand"}", style = Schrift.MonoKlein, color = Farben.Amber) }
    Sprechtaste(
        sendet = zustand.sendet,
        wirdVerstanden = false,
        belegtVon = zustand.spricht?.takeIf { !zustand.sendet }?.let { it.name ?: "jemand" },
        gesperrtBis = null,
        beiDruck = { welt.sprechenStarten() },
        beiLoslassen = { welt.sprechenBeenden() },
    )
    if (!zustand.verbunden) Leisesatz("Keine Verbindung zum Kanal — es wird weiter versucht.", winzig = true)

    if (zustand.chat.isEmpty()) Leisesatz("Noch keine Nachrichten. Schreib die erste.")
    zustand.chat.takeLast(120).forEach { z ->
        Chatzeile(
            z,
            beiAntworten = { if (!z.eigen) an = z.vonId to (z.von ?: "Ohne Namen") },
            beiAnpingen = z.vonBenutzername?.takeIf { !z.eigen }?.let { name ->
                {
                    // Ein Ping gilt einem Menschen: „@name“ vorn im Entwurf.
                    val marke = "@$name "
                    if (marke !in entwurf) entwurf = marke + entwurf
                }
            },
        )
    }
    Leisesatz("Zeile antippen: direkt antworten · @ tippen: im offenen Kanal erwähnen.", winzig = true)

    Warnsatz(zustand.chatfehler)
    an?.let { (_, name) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text("Direkt an $name", style = Schrift.Klein, color = Farben.BlauHell, modifier = Modifier.weight(1f))
            Knopf("An alle", { an = null }, kompakt = true, art = Knopfart.Leise)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
        Feld(
            wert = entwurf,
            beiAenderung = { entwurf = it.take(zustand.chatMax) },
            platzhalter = if (an == null) "An alle in der Welt …" else "Direktnachricht …",
            modifier = Modifier.weight(1f),
        )
        Knopf("Senden", {
            val text = entwurf.trim()
            if (text.isNotEmpty() && welt.chatSenden(an?.first, text)) entwurf = ""
        }, kompakt = true, aktiv = entwurf.isNotBlank())
    }
}

@Composable
private fun Chatzeile(z: WeltChatzeile, beiAntworten: () -> Unit, beiAnpingen: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    z.eigen -> Farben.HauchAmber
                    z.kanal == "Direkt" -> Farben.Blau.copy(alpha = 0.10f)
                    else -> Farben.Flaeche
                },
                Rundung.Klein,
            )
            .clickable(enabled = !z.eigen, onClick = beiAntworten)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text(
                (if (z.eigen) "Du" else z.von ?: "Ohne Namen") +
                    if (z.kanal == "Direkt") " → ${if (z.eigen) z.an ?: "direkt" else "dich"}" else "",
                style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                color = if (z.eigen) Farben.Amber else Farben.TextLeise,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (beiAnpingen != null) {
                Text(
                    "@",
                    style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.BlauHell,
                    modifier = Modifier.clickable(onClick = beiAnpingen).padding(horizontal = Abstand.Klein),
                )
            }
            Text(uhrzeit(z.um), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
        }
        Text(z.text, style = Schrift.Klein, color = Farben.Text)
    }
}

// ==================================================================== Kasse

private val GRUNDTEXT = mapOf(
    "Gruendung" to "Startguthaben", "Lage" to "Lagen", "Erstzugriff" to "Erstzugriffe",
    "Hilfsfrist" to "Hilfsfristen", "Grosslage" to "Großeinsätze", "WacheGebaut" to "Wachen gebaut",
    "WacheAbgerissen" to "Abrisse (Erstattung)", "FahrzeugGekauft" to "Fahrzeuge gekauft",
    "FahrzeugVerkauft" to "Fahrzeuge verkauft", "Verfall" to "Verfallene Lagen", "Ausbau" to "Ausbauten",
    "Wochenziel" to "Wochenziele", "Kredit" to "Kredit aufgenommen", "Tilgung" to "Tilgungen",
    "Lehrgang" to "Lehrgangsgebühren", "LeiheEinnahme" to "Verliehen (Einnahme)",
    "LeiheAusgabe" to "Geliehen (Miete)", "Werkstatt" to "Instandsetzungen",
    "Zweigstelle" to "Zweigstelle gegründet", "Wochensieg" to "Wochensiege (Preisgeld)",
)

/** Die Kasse — `KasseBlende.vue`: Kredit, Summen je Grund, die letzten Buchungen. */
@Composable
fun KasseSeite(welt: Welt, zustand: Weltzustand) {
    val bereich = rememberCoroutineScope()
    var blatt by remember { mutableStateOf<WeltKassenblatt?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var kreditfehler by remember { mutableStateOf<String?>(null) }
    var zeitraum by remember { mutableStateOf("7 Tage") }
    var arbeitet by remember { mutableStateOf(false) }
    var tilgung by remember { mutableStateOf("") }

    suspend fun laden() {
        val k = welt.kennung ?: return
        runCatching { welt.wege.kassenblatt(k) }
            .onSuccess { blatt = it; fehler = null }
            .onFailure { fehler = it.message ?: "Das Kassenblatt ließ sich nicht laden." }
    }
    LaunchedEffect(zustand.gutschrift) { laden() }

    Warnsatz(fehler)
    val b = blatt ?: run {
        if (fehler == null) Ladezeile("Das Kassenblatt wird geladen …")
        return
    }
    Wertzeile("Guthaben", credits(b.guthaben), farbe = Farben.Amber)

    // ----------------------------------------------------------------- Kredit
    val k = b.kredit
    if (k != null) {
        Weltkasten(randfarbe = Farben.SignalTief) {
            Wertzeile("Restschuld", zahl(k.restschuld), farbe = Farben.SignalHell)
            Weltbalken(if (k.rueckzahlung > 0) k.getilgt.toFloat() / k.rueckzahlung else 0f, Farben.GruenHell)
            Leisesatz(
                "${zahl(k.getilgt)} von ${zahl(k.rueckzahlung)} getilgt · ${k.zinsprozent} % Aufschlag · " +
                    "${k.tilgungProzent} % jeder Gutschrift tilgen von selbst.",
                winzig = true,
            )
            val betrag = tilgung.filter(Char::isDigit).toLongOrNull()?.coerceAtMost(k.restschuld) ?: k.restschuld
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = tilgung,
                    beiAenderung = { tilgung = it.filter(Char::isDigit).take(9) },
                    etikett = "Betrag (leer = alles)",
                    tastatur = androidx.compose.ui.text.input.KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                Knopf("${zahl(betrag)} tilgen", {
                    arbeitet = true
                    kreditfehler = null
                    bereich.launch {
                        kreditfehler = welt.handlung("Die Tilgung ging nicht.") { welt.wege.kreditTilgen(it, betrag); welt.betriebLaden() }
                        tilgung = ""
                        laden()
                        arbeitet = false
                    }
                }, kompakt = true, aktiv = !arbeitet && betrag > 0 && b.guthaben >= betrag)
            }
            if (b.guthaben < betrag) Leisesatz("Dafür reicht das Guthaben gerade nicht.", winzig = true)
        }
    } else if (b.kreditangebote.isNotEmpty()) {
        Ueberschrift("Kredit aufnehmen")
        b.kreditangebote.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                Column(Modifier.weight(1f)) {
                    Text(credits(a.betrag), style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    Text("${a.zinsprozent} % Aufschlag · zurück ${zahl(a.rueckzahlung)}", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                }
                Knopf(if (a.frei) "Aufnehmen" else "ab Stufe ${a.abStufe}", {
                    arbeitet = true
                    kreditfehler = null
                    bereich.launch {
                        kreditfehler = welt.handlung("Der Kredit ging nicht.") { welt.wege.kreditAufnehmen(it, a.betrag); welt.betriebLaden() }
                        laden()
                        arbeitet = false
                    }
                }, kompakt = true, aktiv = a.frei && !arbeitet)
            }
        }
        Leisesatz("Getilgt wird von selbst aus jeder Gutschrift — oder früher von Hand.", winzig = true)
    }
    Warnsatz(kreditfehler)

    // ---------------------------------------------------------------- Summen
    Segment(listOf("7 Tage", "Gesamt"), zeitraum, { zeitraum = it })
    val summen = if (zeitraum == "7 Tage") b.summenSiebenTage else b.summenGesamt
    if (summen.isEmpty()) Leisesatz("In diesem Zeitraum hat sich nichts bewegt.")
    summen.forEach { s ->
        Wertzeile(
            GRUNDTEXT[s.grund] ?: s.grund,
            (if (s.summe >= 0) "+" else "") + zahl(s.summe),
            farbe = if (s.summe >= 0) Farben.GruenHell else Farben.SignalHell,
        )
    }

    Ueberschrift("Letzte Buchungen")
    b.buchungen.forEach { z ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(GRUNDTEXT[z.grund] ?: z.grund, style = Schrift.Klein, color = Farben.Text)
                z.vermerk?.let { Text(it, style = Schrift.Winzig, color = Farben.TextLeise, maxLines = 2) }
                Text(tagUndUhr(z.um), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (z.betrag >= 0) "+" else "") + zahl(z.betrag),
                    style = Schrift.MonoKlein,
                    color = if (z.betrag >= 0) Farben.GruenHell else Farben.SignalHell,
                )
                Text("→ ${zahl(z.standDanach)}", style = Schrift.Winzig, color = Farben.TextSehrLeise)
            }
        }
    }
}

// ================================================================ Rangliste

/** Die Rangliste — `RanglisteBlende.vue`: diese Woche und seit je. */
@Composable
fun RanglisteSeite(welt: Welt, zustand: Weltzustand) {
    var liste by remember { mutableStateOf<WeltRangliste?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sicht by remember { mutableStateOf("Diese Woche") }
    LaunchedEffect(zustand.wochensiege) {
        while (true) {
            val k = welt.kennung ?: break
            runCatching { welt.wege.rangliste(k) }
                .onSuccess { liste = it; fehler = null }
                .onFailure { fehler = it.message ?: "Die Rangliste ließ sich nicht laden." }
            delay(60_000)
        }
    }
    Warnsatz(fehler)
    val l = liste ?: run {
        if (fehler == null) Ladezeile()
        return
    }
    l.letzteWoche?.let { s ->
        Weltkasten(randfarbe = Farben.AmberTief) {
            Text("♛ Woche ${s.woche}: ${s.name}", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
            Leisesatz("${s.leitstelle} · ${zahl(s.credits)} verdient · ${s.lagenGedeckt} Lagen · Preisgeld ${credits(s.preisgeld)}", winzig = true)
        }
    }
    if (l.meineWochensiege > 0) Leisesatz("Du hast schon ${l.meineWochensiege} ${if (l.meineWochensiege == 1) "Woche" else "Wochen"} gewonnen.")
    Segment(listOf("Diese Woche", "Gesamt"), sicht, { sicht = it })
    val zeilen = if (sicht == "Diese Woche") l.dieseWoche else l.gesamt
    val mein = if (sicht == "Diese Woche") l.meinPlatzWoche else l.meinPlatzGesamt
    Leisesatz(mein?.let { "Dein Platz: $it" } ?: "Du stehst noch nicht in der Liste.")
    if (zeilen.isEmpty()) Leisesatz("Noch niemand in der Liste.")
    zeilen.forEach { r ->
        val ich = r.platz == mein
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (ich) Farben.HauchAmber else Farben.Flaeche, Rundung.Klein)
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            Text("${r.platz}.", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = if (r.platz <= 3) Farben.Amber else Farben.TextLeise)
            Column(Modifier.weight(1f)) {
                Text(r.name + if (r.wochensiege > 0) " ♛${r.wochensiege}" else "", style = Schrift.Klein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${r.leitstelle} · Stufe ${r.stufe} · ${r.lagenGedeckt} Lagen", style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 1)
            }
            Text(zahl(r.credits), style = Schrift.MonoKlein, color = Farben.Amber)
        }
    }
}

// ================================================================= Laufbahn

private val FREISCHALTNAMEN = mapOf(
    "Loeschgruppe" to "Löschgruppenhaus", "Rettungsdienst" to "Rettungsdienst",
    "Aussenstelle" to "Rettungswachen-Außenstelle", "Hilfsorganisation" to "Hilfsorganisationen als Träger",
    "Polizei" to "Polizei", "Thw" to "THW", "Luftrettung" to "Luftrettung", "Ausbildung" to "Lehrgangseinrichtung",
    "Berufsfeuerwehr" to "Feuerwache (Berufsfeuerwehr)", "Grossanbau" to "Großanbau — vierte Ausbaustufe",
    "Leihmarkt" to "Leihmarkt-Ausbau — zwei Plätze mehr", "Erweiterungsbau" to "Erweiterungsbau — fünfte Ausbaustufe",
    "Grosskredit" to "Großkredit — 150 000 auf einmal", "Werkfeuerwehr" to "Werkfeuerwehr — zehn Stellplätze",
    "Wasserrettung" to "Wasserrettungsstation — Boote und Taucher",
    "Werkstatt" to "Fahrzeugwerkstatt — instand setzen statt ersetzen",
    "Logistikzentrum" to "Logistikzentrum — Abrollbehälter und Logistik",
    "Bereitschaftspolizei" to "Bereitschaftspolizei — geschlossene Einheiten",
    "Zweigstelle" to "Zweigstelle — ein zweiter Ausrückebereich", "Grossraumhalle" to "Großraumhalle — sechste Ausbaustufe",
    "Werkstattausbau" to "Werkstattausbau — doppelte Bühnen, halbe Standzeit",
    "Rettungshunde" to "Rettungshundestaffel — Flächensuche mit Hund",
    "Aufbaulehrgaenge" to "Aufbaulehrgänge — Höhenrettung, Tauchen, Strahlenschutz",
    "Bergwacht" to "Bergwacht-Station — Gelände- und Höhenrettung", "LeihmarktII" to "Leihmarkt-Ausbau II — acht Plätze je Seite",
    "Waldbrandstuetzpunkt" to "Waldbrand-Stützpunkt — vier Plätze für die Fläche",
    "Zugvorlagen" to "Rüstzug und zwei Löschzüge als Vorlage", "ZweigstelleII" to "Zweite Zweigstelle — ein dritter Ausrückebereich",
    "Kriminaldienst" to "Kriminaldirektion — Ermittlung und Kriminaltechnik",
    "WerkstattausbauII" to "Werkstattausbau II — vierfache Bühnen, viertel Standzeit",
    "Landesfeuerwehrschule" to "Landesfeuerwehrschule — doppelte Lehrsäle, halbe Dauer",
    "GrosskreditII" to "Großkredit II — 500 000 auf einmal", "ZweigstelleIII" to "Dritte Zweigstelle — ein vierter Ausrückebereich",
    "Polizeiflieger" to "Polizeifliegerstaffel — ein Haus für den Hubschrauber",
    "Reservestellplatz" to "Reservestellplatz — ein Fahrzeug über dem Deckel",
    "Seenotrettung" to "Seenotrettungsstation — Kreuzer und Tochterboot", "Streifenausbau" to "Streifenausbau — zwölf Stationen je Pfad",
    "ZweigstelleIV" to "Vierte Zweigstelle — ein fünfter Ausrückebereich",
    "Fuehrungslehrgaenge" to "Führungslehrgänge — Führung A und B, Verpflegung, Behandlungsplatz",
    "Verwertung" to "Verwertung — sechzig statt fünfzig Prozent zurück", "GrosskreditIII" to "Großkredit III — eine Million auf einmal",
    "Massenanfall" to "Massenanfall — MANV-Lagen entstehen auch bei dir", "Landesleitstelle" to "Landesleitstelle — die siebte Ausbaustufe",
)

/** Die Laufbahn — `LaufbahnBlende.vue`: alle Stufen, was wann aufgeht. */
@Composable
fun LaufbahnSeite(welt: Welt, zustand: Weltzustand) {
    LaunchedEffect(Unit) { welt.laufbahnLaden() }
    val meine = zustand.stufe
    val erfahrung = zustand.betrieb?.erfahrung ?: zustand.stand?.erfahrung ?: 0
    Wertzeile("Stufe", "$meine")
    Wertzeile("Erfahrung", zahl(erfahrung))
    if (zustand.laufbahn.isEmpty()) {
        Ladezeile()
        return
    }
    zustand.laufbahn.firstOrNull { it.nummer > meine && it.schaltetFrei.isNotEmpty() }?.let { n ->
        Weltkasten(randfarbe = Farben.AmberTief) {
            Text("Als Nächstes auf Stufe ${n.nummer}", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
            n.schaltetFrei.forEach { Leisesatz(FREISCHALTNAMEN[it] ?: it) }
            Leisesatz("Noch ${zahl(maxOf(0, n.abErfahrung - erfahrung))} Erfahrung.", winzig = true)
        }
    }
    zustand.laufbahn.forEach { s ->
        val erreicht = s.nummer <= meine
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (s.nummer == meine) Farben.HauchAmber else androidx.compose.ui.graphics.Color.Transparent, Rundung.Klein)
                .padding(horizontal = Abstand.Klein, vertical = 4.dp),
        ) {
            Text(
                "${s.nummer}",
                style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                color = if (erreicht) Farben.Amber else Farben.TextSehrLeise,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "ab ${zahl(s.abErfahrung)} · ${s.wachendeckel} Wachen",
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                    color = if (erreicht) Farben.TextLeise else Farben.TextSehrLeise,
                )
                s.schaltetFrei.forEach {
                    Text(FREISCHALTNAMEN[it] ?: it, style = Schrift.Klein, color = if (erreicht) Farben.Text else Farben.TextLeise)
                }
            }
            if (!erreicht) Text("noch ${zahl(maxOf(0, s.abErfahrung - erfahrung))}", style = Schrift.Winzig, color = Farben.TextSehrLeise)
        }
    }
}

// ============================================================ Einstellungen

private val GANGARTEN = listOf(
    "Ruhig" to "Wenig gleichzeitig, lange vor Ort. Zeit zum Zusehen.",
    "Normal" to "So, wie die Welt bemessen ist.",
    "Fordernd" to "Viel gleichzeitig, kurze Arbeit. Wer disponieren will.",
    "Eigen" to "Die drei Schrauben von Hand.",
)

/**
 * Die Einstellungen — `EinstellungBlende.vue`: Gangart und Karte.
 *
 * <b>Mehr Durchsatz kostet Lohn.</b> Jede Schraube hebt den Durchsatz, und in
 * demselben Maß nimmt die Vergütung je Lage ab — die Zahl dazu kommt vom Server.
 * Die Icon-Packs bearbeitet und wählt man im Web; die App zeichnet die
 * Fahrzeuge als Pfeile.
 */
@Composable
fun EinstellungSeite(welt: Welt, zustand: Weltzustand, ebenen: Weltebenen) {
    val bereich = rememberCoroutineScope()
    var stand by remember { mutableStateOf<WeltEinstellung?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var gangart by remember { mutableStateOf("Normal") }
    var dichte by remember { mutableStateOf(100) }
    var nachschub by remember { mutableStateOf(100) }
    var arbeitszeit by remember { mutableStateOf(100) }
    var sendet by remember { mutableStateOf(false) }

    suspend fun laden() {
        val k = welt.kennung ?: return
        runCatching { welt.wege.einstellung(k) }
            .onSuccess { s ->
                stand = s; gangart = s.gangart; dichte = s.dichte; nachschub = s.nachschub; arbeitszeit = s.arbeitszeit
                fehler = null
            }
            .onFailure { fehler = it.message ?: "Die Einstellungen ließen sich nicht laden." }
    }
    LaunchedEffect(Unit) { laden() }

    fun speichern(neu: String) {
        sendet = true
        fehler = null
        bereich.launch {
            fehler = welt.handlung("Das Speichern ging nicht.") {
                welt.wege.einstellungSetzen(it, neu, dichte, nachschub, arbeitszeit)
            }
            laden()
            sendet = false
        }
    }

    Ueberschrift("Gangart")
    GANGARTEN.forEach { (id, was) ->
        Wahlzeile(an = gangart == id, beiWechsel = {
            gangart = id
            speichern(id)
        }) {
            Column {
                Text(id, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                Text(was, style = Schrift.Winzig, color = Farben.TextSehrLeise)
            }
        }
    }
    val s = stand
    if (gangart == "Eigen" && s != null) {
        listOf(
            Triple("Einsatzdichte", "Wie viele Lagen in deinem Bereich gleichzeitig offenstehen.", 0),
            Triple("Zeit zwischen Alarmierungen", "Wie schnell nachkommt, wenn du abgearbeitet hast. Niedrig heißt: einzeln statt in Schüben.", 1),
            Triple("Arbeitszeit vor Ort", "Wie lange deine Fahrzeuge an der Einsatzstelle gebunden sind.", 2),
        ).forEach { (name, was, nr) ->
            val wert = when (nr) { 0 -> dichte; 1 -> nachschub; else -> arbeitszeit }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                Text("$wert %", style = Schrift.MonoKlein, color = Farben.Amber)
            }
            Leisesatz(was, winzig = true)
            Regler(wert, s.kleinstes..s.groesstes, 5, { neu ->
                when (nr) { 0 -> dichte = neu; 1 -> nachschub = neu; else -> arbeitszeit = neu }
            })
        }
        val geaendert = s.gangart != "Eigen" || s.dichte != dichte || s.nachschub != nachschub || s.arbeitszeit != arbeitszeit
        Knopf(if (sendet) "Wird gespeichert …" else "Übernehmen", { speichern("Eigen") }, kompakt = true, aktiv = geaendert && !sendet)
    }
    s?.lohnProzent?.takeIf { it < 100 }?.let {
        Text("Vergütung je Lage: $it % — mehr Durchsatz kostet Lohn.", style = Schrift.Klein, color = Farben.Amber)
    }
    Warnsatz(fehler)

    Ueberschrift("Karte")
    Leisesatz("Die Kartenansicht wechselst du mit ◐ auf der Karte. Hier legst du fest, was sie zeigt.", winzig = true)
    Schalterzeile("Lagen", ebenen.lagen, { ebenen.lagen = it })
    Schalterzeile("Eigene Fahrzeuge", ebenen.eigene, { ebenen.eigene = it })
    Schalterzeile("Eigene Wachen", ebenen.wachen, { ebenen.wachen = it })
    Schalterzeile("Eigene Punkte", ebenen.pois, { ebenen.pois = it })
    Schalterzeile("Fahrweg beim Antippen", ebenen.wege, { ebenen.wege = it })
    Schalterzeile("Fremde Fahrzeuge", ebenen.fremde, { ebenen.fremde = it })
    Schalterzeile("Fremde Wachen", ebenen.fremdeWachen, { ebenen.fremdeWachen = it })
    Schalterzeile("Großeinsatz und Events", ebenen.grosslage, { ebenen.grosslage = it })
    Leisesatz("Eigene Fahrzeug-Icons (Icon-Packs) malst und wählst du im Browser unter World → Icons.", winzig = true)

    // ------------------------------------------------------ Gefahrenbereich
    var resetOffen by remember { mutableStateOf(false) }
    var passwort by remember { mutableStateOf("") }
    var resetMeldung by remember { mutableStateOf<String?>(null) }
    Ueberschrift("Gefahrenbereich")
    Leisesatz(
        "PagerSpass - World zurücksetzen: Leitstelle, Wachen, Fahrzeuge, Guthaben und Buchungsblatt werden " +
            "gelöscht. Dein Konto und alles andere bleiben. Danach wählst du einen neuen Standort.",
        winzig = true,
    )
    if (!resetOffen) {
        Knopf("Welt zurücksetzen", { resetOffen = true }, kompakt = true, art = Knopfart.Gefahr)
    } else {
        Feld(wert = passwort, beiAenderung = { passwort = it }, etikett = "Passwort zur Bestätigung", geheim = true)
        Warnsatz(resetMeldung)
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf("Welt dauerhaft zurücksetzen", {
                sendet = true
                bereich.launch {
                    resetMeldung = welt.handlung("Das Zurücksetzen ging nicht.") { welt.wege.zuruecksetzen(it, passwort) }
                    sendet = false
                    if (resetMeldung == null) welt.erneutBetreten()
                }
            }, kompakt = true, art = Knopfart.Gefahr, aktiv = passwort.isNotBlank() && !sendet)
            Knopf("Abbrechen", { resetOffen = false; passwort = "" }, kompakt = true, art = Knopfart.Leise)
        }
    }
}

// ==================================================================== Leihe

/**
 * Der Leihmarkt — `LeiheBlende.vue`, in zwei Reitern.
 *
 * <b>Vier Listen und nicht eine.</b> „Was habe ich eingestellt“, „was ist
 * gerade draußen", „was steht bei mir und gehört mir nicht“ und „was kann ich
 * mieten" sind vier Sachverhalte mit vier Handlungen.
 */
@Composable
fun LeiheSeite(welt: Welt, zustand: Weltzustand) {
    val bereich = rememberCoroutineScope()
    var stand by remember { mutableStateOf<WeltLeihstand?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var reiter by remember { mutableStateOf("Ausleihen") }

    suspend fun laden() {
        val k = welt.kennung ?: return
        runCatching { welt.wege.leihen(k) }
            .onSuccess { stand = it; fehler = null }
            .onFailure { fehler = it.message ?: "Der Leihmarkt ließ sich nicht laden." }
    }
    LaunchedEffect(Unit) {
        while (true) {
            laden()
            delay(60_000)
        }
    }
    fun tun(ersatz: String, block: suspend (String) -> Unit) {
        sendet = true
        fehler = null
        bereich.launch {
            fehler = welt.handlung(ersatz, block)
            laden()
            sendet = false
        }
    }

    Segment(listOf("Ausleihen", "Verleihen"), reiter, { reiter = it })
    Warnsatz(fehler)
    val s = stand ?: run {
        if (fehler == null) Ladezeile()
        return
    }
    val eigeneWachen = zustand.stand?.wachen.orEmpty().filter { traegtFahrzeuge(it.art) }
    val fahrt = zustand.fahrt
    fun rest(bis: String?): String {
        val ms = fahrt.restMs(bis) ?: return ""
        if (ms <= 0) return "kommt zurück"
        val h = Math.ceil(ms / 3_600_000.0).toInt()
        return if (h < 24) "noch $h h" else "noch ${tage(Math.ceil(h / 24.0).toInt())}"
    }
    fun angebotstext(t: Int, preis: Int, km: Double) =
        "${tage(t)} · ${if (preis > 0) "${zahl(preis)} Credits" else "kostenlos"} · ${Math.round(km)} km"

    if (reiter == "Ausleihen") {
        if (s.geliehen.isNotEmpty()) {
            Ueberschrift("Geliehen")
            s.geliehen.forEach { f ->
                Weltkasten {
                    Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                    Leisesatz("${f.typ} · von ${f.gegenueber ?: "—"} · ${f.wache} · ${rest(f.bis)}", winzig = true)
                    Knopf("Zurückgeben", { tun("Das ging nicht.") { welt.wege.leiheZurueckgeben(it, f.fahrzeugId); welt.betriebLaden() } }, kompakt = true, art = Knopfart.Leise, aktiv = !sendet)
                }
            }
        }

        Ueberschrift("Leihbar")
        var zielWache by remember { mutableStateOf<WeltWache?>(null) }
        var kategorie by remember { mutableStateOf("") }
        var sortierung by remember { mutableStateOf("Nächste") }
        fun vorlageVon(a: WeltLeihangebot) = a.vorlageId?.let { zustand.vorlage(it) }
        fun zielFuer(a: WeltLeihangebot): WeltWache? {
            val v = vorlageVon(a)
            val passende = if (v != null) eigeneWachen.filter { passtZurWache(it.art, v) } else eigeneWachen
            return passende.firstOrNull { it.id == zielWache?.id } ?: passende.firstOrNull()
        }
        if (s.leihbar.isNotEmpty()) {
            Auswahl("Fährt zu", listOf<WeltWache?>(null) + eigeneWachen, zielWache, { it?.name ?: "Erste passende Wache" }, { zielWache = it })
            val kategorien = s.leihbar.mapNotNull { vorlageVon(it)?.kategorie }.distinct().sorted()
            Auswahl("Fahrzeugart", listOf("") + kategorien, kategorie, { it.ifEmpty { "Alle Fahrzeugarten" } }, { kategorie = it })
            Segment(listOf("Nächste", "Günstigste", "Neueste"), sortierung, { sortierung = it })
        }
        val sichtbar = s.leihbar
            .filter { kategorie.isEmpty() || vorlageVon(it)?.kategorie == kategorie }
            .let { l ->
                when (sortierung) {
                    "Günstigste" -> l.sortedBy { it.preis }
                    "Neueste" -> l.sortedByDescending { it.angebotenUm }
                    else -> l.sortedBy { it.entfernungKm }
                }
            }
        when {
            s.leihbar.isEmpty() -> Leisesatz("Gerade bietet niemand etwas an.")
            sichtbar.isEmpty() -> Leisesatz("Nichts in dieser Fahrzeugart.")
        }
        sichtbar.forEach { a ->
            Weltkasten {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Column(Modifier.weight(1f)) {
                        Text(a.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                        Leisesatz("${a.typ} · ${angebotstext(a.tage, a.preis, a.entfernungKm)}", winzig = true)
                        Leisesatz("${a.ort} · von ${a.von ?: "—"} · fährt zu ${zielFuer(a)?.name ?: "—"}", winzig = true)
                    }
                    Knopf("Mieten", {
                        val ziel = zielFuer(a) ?: run { fehler = "Du hast keine Wache, auf der dieses Fahrzeug stehen dürfte."; return@Knopf }
                        tun("Das Mieten ging nicht.") { welt.wege.leiheMieten(it, a.id, ziel.id); welt.betriebLaden() }
                    }, kompakt = true, aktiv = !sendet)
                }
            }
        }

        // --------------------------------------------------------- Gesuche
        Ueberschrift("Gesuch aufgeben")
        var gesuchWache by remember { mutableStateOf<WeltWache?>(null) }
        var gesuchKategorie by remember { mutableStateOf<String?>(null) }
        var gesuchTage by remember { mutableStateOf(2) }
        var gesuchPreis by remember { mutableStateOf(100) }
        Leisesatz("Du brauchst ein Fahrzeug, das niemand anbietet? Häng ein Gesuch aus — wer eines hat, schickt es.", winzig = true)
        Auswahl("Fährt zu", eigeneWachen, gesuchWache, { it.name }, { gesuchWache = it; gesuchKategorie = null })
        val kategorien = gesuchWache?.let { w ->
            s.gesuchshoechstpreise.keys.filter { k -> zustand.vorlagen.values.any { it.kategorie == k && passtZurWache(w.art, it) } }.sorted()
        }.orEmpty()
        Auswahl(
            "Fahrzeugart",
            kategorien,
            gesuchKategorie,
            { it },
            { gesuchKategorie = it },
            platzhalter = if (gesuchWache == null) "Erst die Wache wählen" else "Wähle eine Fahrzeugart",
        )
        val deckel = gesuchKategorie?.let { s.gesuchshoechstpreise[it] } ?: 0
        if (deckel in 1 until gesuchPreis) gesuchPreis = deckel
        Text("Für ${tage(gesuchTage)}", style = Schrift.Klein, color = Farben.TextLeise)
        Regler(gesuchTage, 1..7, 1, { gesuchTage = it })
        Wertzeile("Du zahlst", "${zahl(gesuchPreis)} Credits" + if (deckel > 0) " · höchstens ${zahl(deckel)}" else "")
        Regler(gesuchPreis, 0..maxOf(1, deckel), maxOf(10, Math.round((if (deckel > 0) deckel else 100) / 100.0).toInt() * 10), { gesuchPreis = it })
        Knopf(if (sendet) "Wird aufgegeben …" else "Gesuch aufgeben", {
            val w = gesuchWache ?: return@Knopf
            val kat = gesuchKategorie ?: return@Knopf
            tun("Das Gesuch ging nicht raus.") { welt.wege.gesuchAufgeben(it, kat, w.id, gesuchTage, gesuchPreis) }
        }, kompakt = true, art = Knopfart.Haupt, aktiv = !sendet && gesuchWache != null && gesuchKategorie != null)
        if (s.meineGesuche.isNotEmpty()) {
            Ueberschrift("Deine Gesuche")
            s.meineGesuche.forEach { g ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(g.kategorie, style = Schrift.MonoKlein, color = Farben.Text)
                        Leisesatz("${tage(g.tage)} · ${if (g.preis > 0) "${zahl(g.preis)} Credits" else "kostenlos"} · zu ${g.wache}", winzig = true)
                    }
                    Knopf("Zurückziehen", { tun("Das ging nicht.") { welt.wege.gesuchZurueckziehen(it, g.id) } }, kompakt = true, art = Knopfart.Leise, aktiv = !sendet)
                }
            }
        }
    } else {
        // ----------------------------------------------------------- Einstellen
        Ueberschrift("Einstellen")
        val drin = s.eingestellt.map { it.fahrzeugId }.toSet()
        val einstellbar = zustand.fahrzeuge.filter { it.lage == "Wache" && !it.geliehen && it.id !in drin }
        var fahrzeug by remember { mutableStateOf<WeltFahrzeug?>(null) }
        var anzahlTage by remember { mutableStateOf(2) }
        var preis by remember { mutableStateOf(200) }
        Leisesatz("Ein Fahrzeug, das gerade nichts zu tun hat, verdient so etwas — es fährt erst los, wenn jemand mietet.", winzig = true)
        if (einstellbar.isEmpty()) {
            Leisesatz("Kein Fahrzeug steht gerade frei auf einer Wache.")
        } else {
            Auswahl("Fahrzeug", einstellbar, fahrzeug, { "${it.funkrufname} · ${it.typ}" }, { fahrzeug = it })
            val hoechst = fahrzeug?.let { s.hoechstpreise[it.id] } ?: 0
            if (hoechst in 1 until preis) preis = hoechst
            Text("Für ${tage(anzahlTage)}", style = Schrift.Klein, color = Farben.TextLeise)
            Regler(anzahlTage, 1..7, 1, { anzahlTage = it })
            Wertzeile("Preis", "${zahl(preis)} Credits" + if (hoechst > 0) " · höchstens ${zahl(hoechst)}" else "")
            Regler(preis, 0..maxOf(1, hoechst), maxOf(10, Math.round((if (hoechst > 0) hoechst else 100) / 100.0).toInt() * 10), { preis = it })
            Knopf(if (sendet) "Wird eingestellt …" else "In den Markt stellen", {
                val f = fahrzeug ?: return@Knopf
                tun("Das Einstellen ging nicht.") { welt.wege.leiheAnbieten(it, f.id, anzahlTage, preis) }
                fahrzeug = null
            }, kompakt = true, art = Knopfart.Haupt, aktiv = !sendet && fahrzeug != null)
        }

        if (s.gesuche.isNotEmpty()) {
            Ueberschrift("Gesucht")
            Leisesatz("Andere suchen ein Fahrzeug, das du hast — schick eines hin und verdiene die Miete.", winzig = true)
            s.gesuche.forEach { g ->
                val passende = zustand.fahrzeuge.filter { f ->
                    f.lage == "Wache" && !f.geliehen && zustand.vorlage(f.vorlageId)?.kategorie == g.kategorie
                }
                var wahl by remember(g.id) { mutableStateOf<WeltFahrzeug?>(null) }
                Weltkasten {
                    Text(g.kategorie, style = Schrift.MonoKlein, color = Farben.Text)
                    Leisesatz("${angebotstext(g.tage, g.preis, g.entfernungKm)} · ${g.wache} · von ${g.von ?: "—"}", winzig = true)
                    if (passende.size > 1) Auswahl("Fahrzeug", passende, wahl ?: passende.first(), { "${it.funkrufname} · ${it.typ}" }, { wahl = it })
                    Knopf("Hinschicken", {
                        val f = wahl ?: passende.firstOrNull() ?: run {
                            fehler = "Gerade steht kein passendes Fahrzeug auf einer deiner Wachen."
                            return@Knopf
                        }
                        tun("Das Hinschicken ging nicht.") { welt.wege.gesuchBedienen(it, g.id, f.id); welt.betriebLaden() }
                    }, kompakt = true, aktiv = !sendet && passende.isNotEmpty())
                }
            }
        }

        if (s.eingestellt.isNotEmpty()) {
            Ueberschrift("Im Markt")
            s.eingestellt.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(a.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                        Leisesatz("${a.typ} · ${angebotstext(a.tage, a.preis, a.entfernungKm)}" + if (!a.verfuegbar) " · gerade im Einsatz" else "", winzig = true)
                    }
                    Knopf("Zurückziehen", { tun("Das ging nicht.") { welt.wege.leiheZuruecknehmen(it, a.id) } }, kompakt = true, art = Knopfart.Leise, aktiv = !sendet)
                }
            }
        }
        if (s.verliehen.isNotEmpty()) {
            Ueberschrift("Verliehen")
            s.verliehen.forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                        Leisesatz("${f.typ} · bei ${f.gegenueber ?: "—"} · ${f.wache}", winzig = true)
                    }
                    Text(rest(f.bis), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                }
            }
        }
    }
}

