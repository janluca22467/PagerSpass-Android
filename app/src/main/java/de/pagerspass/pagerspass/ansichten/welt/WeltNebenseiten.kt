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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.theme.flaeche
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import de.pagerspass.pagerspass.ui.karte.Kartenstil
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import androidx.compose.foundation.layout.ColumnScope
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
 * <b>Direkt ist eine Antwort, kein eigener Bereich.</b> „Antworten“ an einer
 * Zeile schreibt dem Absender direkt; „an alle“ nimmt es zurück. Der Name
 * nennt den Absender im Entwurf (`@name`). Wen man nicht mehr lesen will,
 * schaltet man stumm — nur auf diesem Gerät; eigene Zeilen lassen sich in den
 * ersten Minuten für alle zurücknehmen (seit PagerSpass 6, sonst fehlt der
 * Knopf). Das Funkgerät heißt „hören“ — sprechen geht auch, wenn es aus ist.
 */
@Composable
fun ChatSeite(welt: Welt, zustand: Weltzustand) {
    var entwurf by remember { mutableStateOf("") }
    var an by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Die Uhr für „Zurücknehmen“: Der Knopf verschwindet, sobald die Frist um ist.
    var jetzt by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            jetzt = System.currentTimeMillis()
        }
    }

    DisposableEffect(Unit) {
        welt.chatGeoeffnet(true)
        onDispose {
            welt.chatGeoeffnet(false)
            welt.sprechenBeenden()
        }
    }

    // Der Kopf trägt beides: den Funkschalter und die Anzeige, wer spricht —
    // „X spricht“ woanders als der Schalter fragte, warum man nichts hört.
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.Flaeche, Rundung.Normal)
            .padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                if (zustand.funkgeraet) "Funk an" else "Funk aus",
                { welt.funkgeraetSetzen(!zustand.funkgeraet) },
                art = if (zustand.funkgeraet) Knopfart.Haupt else Knopfart.Normal,
                kompakt = true,
            )
            when {
                zustand.spricht != null && zustand.funkgeraet ->
                    Text("${zustand.spricht.name ?: "Jemand"} spricht", style = Schrift.MonoKlein, color = Farben.Amber)
                !zustand.funkgeraet -> Text("Du hörst nicht mit.", style = Schrift.Klein, color = Farben.TextLeise)
            }
        }
        Sprechtaste(
            sendet = zustand.sendet,
            wirdVerstanden = false,
            belegtVon = zustand.spricht?.takeIf { !zustand.sendet }?.let { it.name ?: "jemand" },
            gesperrtBis = null,
            beiDruck = { welt.sprechenStarten() },
            beiLoslassen = { welt.sprechenBeenden() },
        )
    }
    if (!zustand.verbunden) Leisesatz("Keine Verbindung zum Kanal — es wird weiter versucht.", winzig = true)

    // Stummgeschaltet wird nur auf diesem Gerät — und es steht dran, damit es
    // nicht vergessen wird.
    val zeilen = zustand.chat.filter { it.vonId !in zustand.stumm }
    if (zustand.stumm.isNotEmpty()) {
        val verborgen = zustand.chat.size - zeilen.size
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text(
                (if (zustand.stumm.size == 1) "Eine Person" else "${zustand.stumm.size} Personen") +
                    " stummgeschaltet" + if (verborgen > 0) " · $verborgen Zeilen verborgen" else "",
                style = Schrift.Klein,
                color = Farben.TextLeise,
                modifier = Modifier.weight(1f),
            )
            Knopf("wieder hören", { zustand.stumm.forEach { welt.stummSetzen(it, false) } }, kompakt = true, art = Knopfart.Leise)
        }
    }

    if (zustand.aeltereVorhanden) {
        Knopf(
            if (zustand.aeltereLaedt) "Lädt …" else "Ältere Nachrichten laden",
            { welt.chatAeltereLaden() },
            kompakt = true,
            art = Knopfart.Leise,
            aktiv = !zustand.aeltereLaedt,
        )
    }
    if (zeilen.isEmpty()) Leisesatz("Noch nichts gesagt. Schreib die erste Zeile.")
    zeilen.takeLast(200).forEach { z ->
        Chatzeile(
            z,
            zuruecknehmbar = welt.zuruecknehmbar(z, jetzt),
            beiAntworten = { an = z.vonId to (z.von ?: "Ohne Namen") },
            beiStumm = {
                welt.stummSetzen(z.vonId, true)
                // War er gerade der Empfänger, geht die nächste Zeile wieder an alle.
                if (an?.first == z.vonId) an = null
            },
            beiZuruecknehmen = { welt.chatZuruecknehmen(z.id) },
            beiAnpingen = z.vonBenutzername?.takeIf { !z.eigen }?.let { name ->
                {
                    // Ein Ping gilt einem Menschen: „@name“ vorn im Entwurf.
                    val marke = "@$name "
                    if (marke !in entwurf) entwurf = marke + entwurf
                }
            },
        )
    }

    Warnsatz(zustand.chatfehler)
    // Der Empfänger steht über dem Feld: Er gilt für die nächsten Nachrichten.
    an?.let { (_, name) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text("An", style = Schrift.Klein, color = Farben.TextLeise)
            Text(name, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
            Knopf("an alle", { an = null }, kompakt = true, art = Knopfart.Leise)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
        Feld(
            wert = entwurf,
            beiAenderung = { entwurf = it.take(zustand.chatMax) },
            platzhalter = an?.let { "Nachricht an ${it.second}" } ?: "An alle in der Welt — @ nennt jemanden …",
            modifier = Modifier.weight(1f),
        )
        Knopf("Senden", {
            val text = entwurf.trim()
            if (text.isNotEmpty() && welt.chatSenden(an?.first, text)) entwurf = ""
        }, kompakt = true, art = Knopfart.Haupt, aktiv = entwurf.isNotBlank())
    }
    if (entwurf.length > zustand.chatMax - 50) {
        Text(
            "${zustand.chatMax - entwurf.length}",
            style = Schrift.MonoKlein,
            color = if (entwurf.length >= zustand.chatMax) Farben.SignalHell else Farben.TextLeise,
        )
    }
}

/** Die Erwähnungen „@name“ einer Zeile — amber hervorgehoben wie im Web. */
private val ERWAEHNUNG = Regex("(^|\\s)(@[\\p{L}\\p{N}_.-]+)")

private fun mitErwaehnungen(text: String): androidx.compose.ui.text.AnnotatedString =
    androidx.compose.ui.text.buildAnnotatedString {
        var ab = 0
        ERWAEHNUNG.findAll(text).forEach { t ->
            val g = t.groups[2] ?: return@forEach
            if (g.range.first > ab) append(text.substring(ab, g.range.first))
            pushStyle(androidx.compose.ui.text.SpanStyle(color = Farben.Amber, fontWeight = FontWeight.Bold))
            append(g.value)
            pop()
            ab = g.range.last + 1
        }
        if (ab < text.length) append(text.substring(ab))
    }

/**
 * Eine Zeile des Weltchats — `weltchat__zeile`: Kopf mit Name (er nennt den
 * Absender im Entwurf), Marke der Direktnachricht, Uhrzeit und den Griffen der
 * Zeile; darunter der Text.
 */
@Composable
private fun Chatzeile(
    z: WeltChatzeile,
    zuruecknehmbar: Boolean,
    beiAntworten: () -> Unit,
    beiStumm: () -> Unit,
    beiZuruecknehmen: () -> Unit,
    beiAnpingen: (() -> Unit)?,
) {
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
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text(
                if (z.eigen) "Du" else z.von ?: "Ohne Namen",
                style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                color = if (z.eigen) Farben.Amber else Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .let { m -> if (beiAnpingen != null) m.clickable(onClick = beiAnpingen) else m },
            )
            // Die Direktnachricht steht in derselben Liste und muss trotzdem
            // auf einen Blick von einer offenen Zeile zu unterscheiden sein.
            if (z.kanal == "Direkt") {
                Text(
                    if (z.eigen) "an ${z.an ?: "Ohne Namen"}" else "nur an dich",
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                    color = Farben.BlauHell,
                    maxLines = 1,
                )
            }
            Text(uhrzeit(z.um), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
        }
        Text(mitErwaehnungen(z.text), style = Schrift.Klein, color = Farben.Text)
        if (!z.eigen) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Antworten", beiAntworten, kompakt = true, art = Knopfart.Leise)
                Knopf("Stumm", beiStumm, kompakt = true, art = Knopfart.Leise)
            }
        } else if (zuruecknehmbar) {
            Knopf("Zurücknehmen", beiZuruecknehmen, kompakt = true, art = Knopfart.Leise)
        }
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

/**
 * Die Kasse — `KasseBlende.vue`: oben der Kredit (laufend oder die Angebote),
 * darunter die Summen je Grund für sieben Tage oder seit je, zuletzt die
 * Buchungen.
 */
@Composable
fun KasseSeite(welt: Welt, zustand: Weltzustand) {
    val bereich = rememberCoroutineScope()
    var blatt by remember { mutableStateOf<WeltKassenblatt?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var kreditfehler by remember { mutableStateOf<String?>(null) }
    var zeitraum by remember { mutableStateOf("sieben") }
    var arbeitet by remember { mutableStateOf(false) }
    var tilgung by remember { mutableStateOf("") }

    suspend fun laden() {
        val k = welt.kennung ?: return
        runCatching { welt.wege.kassenblatt(k) }
            .onSuccess { blatt = it; fehler = null }
            .onFailure { fehler = it.message ?: "Das Kassenblatt ließ sich nicht laden." }
    }
    LaunchedEffect(zustand.gutschrift) { laden() }

    if (fehler != null) {
        Warnsatz(fehler)
        return
    }
    val b = blatt ?: run {
        Leerhinweis("Das Kassenblatt wird geladen …")
        return
    }

    // ----------------------------------------------------------------- Kredit
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Klein),
    ) {
        val k = b.kredit
        if (k != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Restschuld", style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text, modifier = Modifier.weight(1f))
                Text(credits(k.restschuld), style = Schrift.Gross.copy(fontFamily = Schrift.Mono), color = Farben.Amber)
            }
            Weltbalken(if (k.rueckzahlung > 0) k.getilgt.toFloat() / k.rueckzahlung else 0f, Farben.Amber)
            Leisesatz(
                "${zahl(k.getilgt)} von ${zahl(k.rueckzahlung)} getilgt · ${k.zinsprozent} % Aufschlag · " +
                    "${k.tilgungProzent} % jeder Gutschrift tilgen von selbst.",
                winzig = true,
            )
            // Leer heißt alles — der Platzhalter sagt, wie viel das ist.
            val betrag = tilgung.filter(Char::isDigit).toLongOrNull()?.coerceAtMost(k.restschuld) ?: k.restschuld
            val gedeckt = (zustand.betrieb?.guthaben ?: b.guthaben) >= betrag
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Feld(
                    wert = tilgung,
                    beiAenderung = { tilgung = it.filter(Char::isDigit).take(9) },
                    platzhalter = zahl(k.restschuld),
                    tastatur = androidx.compose.ui.text.input.KeyboardType.Number,
                    stil = Schrift.MonoNormal,
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
                }, art = Knopfart.Haupt, aktiv = !arbeitet && betrag > 0 && gedeckt)
            }
            if (!gedeckt) Leisesatz("Dafür fehlen Welt-Credits — es geht auch weniger.", winzig = true)
        } else {
            Text("Kredit aufnehmen", style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text)
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                b.kreditangebote.forEach { a ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Farben.FlaecheHoch, Rundung.Winzig)
                            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(credits(a.betrag), style = Schrift.MonoKlein, color = Farben.Text)
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
            }
            Leisesatz("Einer zur Zeit — getilgt wird von selbst, ein Viertel jeder Gutschrift.", winzig = true)
        }
        Warnsatz(kreditfehler)
    }

    // ---------------------------------------------------------------- Summen
    Pillenreihe {
        Pille("7 Tage", zeitraum == "sieben", { zeitraum = "sieben" })
        Pille("Seit je", zeitraum == "gesamt", { zeitraum = "gesamt" })
    }
    val summen = if (zeitraum == "sieben") b.summenSiebenTage else b.summenGesamt
    if (summen.isEmpty()) {
        Leerhinweis("In diesem Zeitraum hat sich nichts bewegt.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            summen.forEach { s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Farben.Flaeche, Rundung.Winzig)
                        .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                ) {
                    Text(GRUNDTEXT[s.grund] ?: s.grund, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                    Text(
                        (if (s.summe >= 0) "+" else "") + credits(s.summe),
                        style = Schrift.MonoKlein,
                        color = if (s.summe >= 0) Farben.GruenHell else Farben.SignalHell,
                    )
                }
            }
        }
    }

    Ueberschrift("Letzte Buchungen", Modifier.padding(top = Abstand.Klein))
    b.buchungen.forEach { z ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(GRUNDTEXT[z.grund] ?: z.grund, style = Schrift.Klein, color = Farben.Text)
                z.vermerk?.let { Text(it, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 2) }
                Text(tagUndUhr(z.um), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (z.betrag >= 0) "+" else "") + zahl(z.betrag),
                    style = Schrift.MonoKlein,
                    color = if (z.betrag >= 0) Farben.GruenHell else Farben.SignalHell,
                )
                Text("→ ${zahl(z.standDanach)}", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
            }
        }
    }
}

// ================================================================ Rangliste

/**
 * Die Rangliste — `RanglisteBlende.vue`: diese Woche und seit je, darüber in der
 * Woche die Krone der letzten, darunter der eigene Platz.
 */
@Composable
fun RanglisteSeite(welt: Welt, zustand: Weltzustand) {
    var liste by remember { mutableStateOf<WeltRangliste?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sicht by remember { mutableStateOf("woche") }
    LaunchedEffect(zustand.wochensiege) {
        while (true) {
            val k = welt.kennung ?: break
            runCatching { welt.wege.rangliste(k) }
                .onSuccess { liste = it; fehler = null }
                .onFailure { fehler = it.message ?: "Die Rangliste ließ sich nicht laden." }
            delay(60_000)
        }
    }
    if (fehler != null) {
        Warnsatz(fehler)
        return
    }
    val l = liste
    Pillenreihe {
        Pille("Diese Woche", sicht == "woche", { sicht = "woche" })
        Pille("Seit je", sicht == "gesamt", { sicht = "gesamt" })
    }
    if (l == null) {
        Ladezeile()
        return
    }
    val s = l.letzteWoche
    if (sicht == "woche" && s != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text("♛", style = Schrift.Gross, color = Farben.Amber)
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        append("Letzte Woche gewann ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Farben.Text)) { append(s.name) }
                    },
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                    modifier = Modifier.zumProfil(s.benutzername),
                )
                Text(
                    "${s.leitstelle} · ${zahl(s.credits)} verdient · ${zahl(s.preisgeld)} Preisgeld",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
            }
        }
    }
    val zeilen = if (sicht == "woche") l.dieseWoche else l.gesamt
    val mein = if (sicht == "woche") l.meinPlatzWoche else l.meinPlatzGesamt
    if (zeilen.isEmpty()) {
        Leerhinweis("Noch hat niemand etwas verdient. Der erste Platz ist zu haben.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            zeilen.forEach { r ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (r.platz == mein) Farben.HauchAmber else Farben.Flaeche, Rundung.Winzig)
                        .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                ) {
                    Text(
                        "${r.platz}",
                        style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                        color = when (r.platz) {
                            1 -> Farben.Amber
                            2 -> Farben.TextLeise
                            3 -> Farben.OrangeHell
                            else -> Farben.TextSehrLeise
                        },
                        modifier = Modifier.width(28.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                r.name,
                                style = Schrift.Klein,
                                color = Farben.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false).zumProfil(r.benutzername),
                            )
                            if (r.wochensiege > 0) Text("♛ ${r.wochensiege}", style = Schrift.Winzig, color = Farben.Amber)
                        }
                        Text(
                            "${r.leitstelle} · Stufe ${r.stufe} · ${r.lagenGedeckt} Lagen",
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                            maxLines = 1,
                        )
                    }
                    Text(credits(r.credits), style = Schrift.MonoKlein, color = Farben.AmberHell)
                }
            }
        }
    }
    Text(
        buildAnnotatedString {
            if (mein != null) {
                append("Dein Platz: ")
                withStyle(SpanStyle(fontFamily = Schrift.Mono)) { append("$mein") }
                if (l.meineWochensiege > 0) {
                    append(" · gewonnene Wochen: ")
                    withStyle(SpanStyle(fontFamily = Schrift.Mono)) { append("${l.meineWochensiege}") }
                }
            } else {
                append("Sobald du etwas verdienst, stehst du mit drauf.")
            }
        },
        style = Schrift.Klein,
        color = Farben.TextLeise,
    )
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

/**
 * Die Laufbahn — `LaufbahnBlende.vue`: oben die zwei Größen gegenübergestellt
 * (Erfahrung wächst nur, Credits gibt man aus), dann das Nächste, dann alle
 * Stufen.
 */
@Composable
fun LaufbahnSeite(welt: Welt, zustand: Weltzustand) {
    LaunchedEffect(Unit) { welt.laufbahnLaden() }
    val meine = zustand.stufe
    val erfahrung = zustand.betrieb?.erfahrung ?: zustand.stand?.erfahrung ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.height(IntrinsicSize.Min)) {
        Groesse(
            "Erfahrung",
            Weltzeichen.Erfahrung,
            zahl(erfahrung),
            Farben.BlauHell,
            "wächst nur · bestimmt die Stufe",
            Modifier.weight(1f).fillMaxHeight(),
        )
        Groesse(
            "Welt-Credits",
            Weltzeichen.Waehrung,
            zahl(zustand.guthaben),
            Farben.AmberHell,
            "gibst du aus · ändert die Stufe nicht",
            Modifier.weight(1f).fillMaxHeight(),
        )
    }
    Leisesatz("Hundert verdiente Credits sind zehn Erfahrung — und die schrumpft nie.")
    zustand.laufbahn.firstOrNull { it.nummer > meine && it.schaltetFrei.isNotEmpty() }?.let { n ->
        Text(
            buildAnnotatedString {
                append("Als Nächstes: ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(n.schaltetFrei.joinToString(" und ") { FREISCHALTNAMEN[it] ?: it })
                }
                append(" ab Stufe ${n.nummer} — ")
                withStyle(SpanStyle(fontFamily = Schrift.Mono)) { append("noch ${zahl(maxOf(0, n.abErfahrung - erfahrung))}") }
                append(" Erfahrung.")
            },
            style = Schrift.Klein,
            color = Farben.Text,
            modifier = Modifier.fillMaxWidth().background(Farben.FlaecheHoch, Rundung.Normal).padding(Abstand.Klein),
        )
    }
    if (zustand.laufbahn.isEmpty()) {
        Text("Die Laufbahn ließ sich nicht laden.", style = Schrift.Klein, color = Farben.TextSehrLeise)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        zustand.laufbahn.forEach { s ->
            val erreicht = s.nummer <= meine
            val aktuell = s.nummer == meine
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (erreicht) 1f else 0.7f)
                    .then(
                        if (aktuell) Modifier.background(Farben.FlaecheAktiv, Rundung.Normal).border(1.dp, Farben.AmberTief, Rundung.Normal)
                        else Modifier,
                    )
                    .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
            ) {
                Text(
                    "${s.nummer}",
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = if (aktuell) Farben.AmberHell else Farben.TextLeise,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(36.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text("${s.wachendeckel} Wachen", style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("ab", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                            androidx.compose.material3.Icon(Weltzeichen.Erfahrung, contentDescription = null, tint = Farben.TextSehrLeise, modifier = Modifier.size(11.dp))
                            Text(zahl(s.abErfahrung), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                        }
                    }
                    when {
                        s.schaltetFrei.isNotEmpty() -> Text(
                            s.schaltetFrei.joinToString(" · ") { FREISCHALTNAMEN[it] ?: it },
                            style = Schrift.Winzig,
                            color = Farben.AmberHell,
                        )
                        aktuell -> Text("Hier stehst du.", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                    }
                }
            }
        }
    }
}

/** Eine der zwei Größen oben auf der Laufbahn — `.laufbahn__groesse`. */
@Composable
private fun Groesse(
    titel: String,
    zeichen: androidx.compose.ui.graphics.vector.ImageVector,
    wert: String,
    farbe: androidx.compose.ui.graphics.Color,
    unter: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier.background(Farben.FlaecheHoch, Rundung.Normal).padding(Abstand.Klein),
    ) {
        Text(titel, style = Schrift.Klein, color = Farben.TextLeise)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            androidx.compose.material3.Icon(zeichen, contentDescription = null, tint = farbe, modifier = Modifier.size(18.dp))
            Text(wert, style = Schrift.Gross.copy(fontFamily = Schrift.Mono), color = farbe)
        }
        Text(unter, style = Schrift.Winzig, color = Farben.TextSehrLeise)
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
 * Die Einstellungen — `EinstellungBlende.vue`: Gangart, Karte, Fahrzeug-Icons,
 * Ebenen.
 *
 * <b>Mehr Durchsatz kostet Lohn.</b> Jede Schraube hebt den Durchsatz, und in
 * demselben Maß nimmt die Vergütung je Lage ab — die Zahl dazu kommt vom Server.
 * Die eigenen Schrauben speichern sich selbst, kurz nachdem man loslässt; ein
 * „Übernehmen“ dahinter wäre ein Schritt, den man vergisst.
 *
 * <b>Bewusst nicht hier:</b> Zurücksetzen steht unter dem Konto (wie im Web),
 * und den Ton des Funkgeräts gibt es in der App nicht als eigenen Regler.
 */
@Composable
fun EinstellungSeite(welt: Welt, zustand: Weltzustand, ebenen: Weltebenen) {
    val bereich = rememberCoroutineScope()
    val zusammenhang = androidx.compose.ui.platform.LocalContext.current
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

    // Die eigenen Schrauben speichern sich, wenn eine Weile nichts mehr kommt.
    LaunchedEffect(dichte, nachschub, arbeitszeit) {
        val s = stand ?: return@LaunchedEffect
        if (gangart != "Eigen") return@LaunchedEffect
        if (s.gangart == "Eigen" && s.dichte == dichte && s.nachschub == nachschub && s.arbeitszeit == arbeitszeit) return@LaunchedEffect
        delay(400)
        speichern("Eigen")
    }

    Warnsatz(fehler)

    Ueberschrift("Gangart")
    Leisesatz("Wie viel gleichzeitig los ist — am Stundenverdienst ändert es nichts.", winzig = true)
    Pillenreihe {
        GANGARTEN.forEach { (id, _) ->
            Pille(id, gangart == id, { gangart = id; speichern(id) }, aktiv = !sendet)
        }
    }
    Leisesatz(GANGARTEN.firstOrNull { it.first == gangart }?.second.orEmpty(), winzig = true)
    val s = stand
    if (gangart == "Eigen") {
        listOf(
            Triple("Einsatzdichte", "Wie viele Lagen in deinem Bereich gleichzeitig offenstehen.", 0),
            Triple("Zeit zwischen Alarmierungen", "Wie schnell nachkommt, wenn du abgearbeitet hast. Niedrig heißt: einzeln statt in Schüben.", 1),
            Triple("Arbeitszeit vor Ort", "Wie lange deine Fahrzeuge an der Einsatzstelle gebunden sind.", 2),
        ).forEach { (name, was, nr) ->
            val wert = when (nr) { 0 -> dichte; 1 -> nachschub; else -> arbeitszeit }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Etikett(name, Modifier.weight(1f))
                Text("$wert %", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
            }
            Regler(wert, (s?.kleinstes ?: 50)..(s?.groesstes ?: 200), 5, { neu ->
                when (nr) { 0 -> dichte = neu; 1 -> nachschub = neu; else -> arbeitszeit = neu }
            })
            Leisesatz(was, winzig = true)
        }
    }
    s?.lohnProzent?.takeIf { it < 100 }?.let {
        Text(
            buildAnnotatedString {
                append("Jede Lage bringt ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$it %") }
                append(" der Vergütung — mehr zu tun heißt je Lage weniger.")
            },
            style = Schrift.Klein,
            color = Farben.Text,
        )
    }

    Ueberschrift("Karte", Modifier.padding(top = Abstand.Klein))
    LaunchedEffect(Unit) {
        if (ebenen.stil == null) {
            val roh = runCatching { de.pagerspass.pagerspass.netz.Ablage(zusammenhang).karteStil() }.getOrNull()
            ebenen.stil = Kartenstil.entries.firstOrNull { it.name == roh } ?: Kartenstil.Dunkel
        }
    }
    Pillenreihe {
        Kartenstil.entries.forEach { st ->
            Pille(st.titel, (ebenen.stil ?: Kartenstil.Dunkel) == st, {
                ebenen.stil = st
                bereich.launch { runCatching { de.pagerspass.pagerspass.netz.Ablage(zusammenhang).karteStilSetzen(st.name) } }
            })
        }
    }

    // Die Fahrzeug-Icons: wählen hier, verwalten in der Bibliothek darüber.
    IconpackWahl()

    Ueberschrift("Ebenen", Modifier.padding(top = Abstand.Klein))
    Leisesatz("Was auf der Karte gezeichnet wird — dieselben Schalter wie am Ebenenknopf.", winzig = true)
    val b = zustand.betrieb
    val zeilen = listOf(
        Triple("Lagen", (b?.lagen?.size ?: 0) > 0, 0),
        Triple("Eigene Fahrzeuge", true, 1),
        Triple("Eigene Wachen", (zustand.stand?.wachen?.size ?: 0) > 0, 2),
        Triple("Eigene Punkte", zustand.pois.isNotEmpty(), 3),
        Triple("Fahrweg beim Antippen", true, 4),
        Triple("Fremde Fahrzeuge", (b?.fremde?.size ?: 0) > 0, 5),
        Triple("Fremde Wachen", (b?.fremdeWachen?.size ?: 0) > 0, 6),
        Triple("Großeinsatz und Events", zustand.grosslage != null || zustand.events.any { it.brLat != null }, 7),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        zeilen.forEach { (name, vorhanden, nr) ->
            val an = when (nr) {
                0 -> ebenen.lagen; 1 -> ebenen.eigene; 2 -> ebenen.wachen; 3 -> ebenen.pois
                4 -> ebenen.wege; 5 -> ebenen.fremde; 6 -> ebenen.fremdeWachen; else -> ebenen.grosslage
            }
            fun setzen(neu: Boolean) = when (nr) {
                0 -> ebenen.lagen = neu; 1 -> ebenen.eigene = neu; 2 -> ebenen.wachen = neu; 3 -> ebenen.pois = neu
                4 -> ebenen.wege = neu; 5 -> ebenen.fremde = neu; 6 -> ebenen.fremdeWachen = neu; else -> ebenen.grosslage = neu
            }
            // Eine Ebene ohne Inhalt steht grau da, statt zu verschwinden.
            Listenwahl(
                an = an && vorhanden,
                beiDruck = { if (vorhanden) setzen(!an) },
                modifier = Modifier.alpha(if (vorhanden) 1f else 0.5f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(18.dp).border(1.5.dp, if (an) Farben.Amber else Farben.TextSehrLeise, Rundung.Winzig),
                ) {
                    if (an) Text("✓", style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
                }
                Text(name, style = Schrift.Klein, color = Farben.Text)
            }
        }
    }
}

// ==================================================================== Leihe

/**
 * Der Leihmarkt — `LeiheBlende.vue`, in zwei Reitern.
 *
 * <b>Vier Listen und nicht eine.</b> „Was habe ich eingestellt“, „was ist
 * gerade draußen", „was steht bei mir und gehört mir nicht“ und „was kann ich
 * mieten" sind vier Sachverhalte mit vier Handlungen. Die beiden Formulare
 * (Gesuch, Einstellen) sind zu, bis man sie öffnet — sie sind der seltenere Weg.
 */
@Composable
fun LeiheSeite(welt: Welt, zustand: Weltzustand) {
    val bereich = rememberCoroutineScope()
    var stand by remember { mutableStateOf<WeltLeihstand?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var reiter by remember { mutableStateOf("ausleihen") }

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

    Pillenreihe {
        Pille("Ausleihen", reiter == "ausleihen", { reiter = "ausleihen" })
        Pille("Verleihen", reiter == "verleihen", { reiter = "verleihen" })
    }
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
        "${tage(t)} · ${if (preis > 0) "${zahl(preis)} Credits" else "kostenlos"} · ${zahl(Math.round(km))} km"

    if (reiter == "ausleihen") {
        // ------------------------------------------------------------ Geliehen
        if (s.geliehen.isNotEmpty()) {
            Ueberschrift("Geliehen")
            var rueckgabeGefragt by remember { mutableStateOf<String?>(null) }
            s.geliehen.forEach { f ->
                Leihzeile(
                    inhalt = {
                        Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                        Namenssatz("${f.typ} · von ", f.gegenueber ?: "einer Leitstelle", f.gegenueberBenutzername, " · ${f.wache}", winzig = false)
                    },
                    griffe = {
                        val r = rest(f.bis)
                        Text(r, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                        if (r != "kommt zurück") {
                            if (rueckgabeGefragt != f.fahrzeugId) {
                                Knopf("Zurückgeben", { rueckgabeGefragt = f.fahrzeugId }, kompakt = true, art = Knopfart.Leise, aktiv = !sendet)
                            } else {
                                Knopf("Ohne Erstattung", {
                                    tun("Das ging nicht.") { welt.wege.leiheZurueckgeben(it, f.fahrzeugId); welt.betriebLaden() }
                                    rueckgabeGefragt = null
                                }, kompakt = true, aktiv = !sendet)
                                Knopf("Nein", { rueckgabeGefragt = null }, kompakt = true, art = Knopfart.Leise)
                            }
                        }
                    },
                )
            }
        }

        // ------------------------------------------------------------- Leihbar
        Ueberschrift("Leihbar", Modifier.padding(top = Abstand.Klein))
        Leisesatz("Sofort bezahlt, zählt nicht auf deine Stellplätze, fährt nach Ablauf allein heim.", winzig = true)
        var zielWache by remember { mutableStateOf<WeltWache?>(null) }
        var kategorie by remember { mutableStateOf("") }
        var sortierung by remember { mutableStateOf("entfernung") }
        fun vorlageVon(a: WeltLeihangebot) = a.vorlageId?.let { zustand.vorlage(it) }
        fun zielFuer(a: WeltLeihangebot): WeltWache? {
            val v = vorlageVon(a)
            val passende = if (v != null) eigeneWachen.filter { passtZurWache(it.art, v) } else eigeneWachen
            return passende.firstOrNull { it.id == zielWache?.id } ?: passende.firstOrNull()
        }
        val sichtbar = s.leihbar
            .filter { kategorie.isEmpty() || vorlageVon(it)?.kategorie == kategorie }
            .let { l ->
                when (sortierung) {
                    "preis" -> l.sortedBy { it.preis }
                    "neu" -> l.sortedByDescending { it.angebotenUm }
                    else -> l.sortedBy { it.entfernungKm }
                }
            }
        if (eigeneWachen.size > 1 && sichtbar.isNotEmpty()) {
            Auswahl("Fährt zu", listOf<WeltWache?>(null) + eigeneWachen, zielWache, { it?.name ?: "Erste passende Wache" }, { zielWache = it })
        }
        if (s.leihbar.size > 3) {
            val kategorien = s.leihbar.mapNotNull { vorlageVon(it)?.kategorie }.distinct().sorted()
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Auswahl("Fahrzeugart", listOf("") + kategorien, kategorie, { it.ifEmpty { "Alle Fahrzeugarten" } }, { kategorie = it }, modifier = Modifier.weight(1f))
                Auswahl(
                    "Ordnen",
                    listOf("entfernung", "preis", "neu"),
                    sortierung,
                    { when (it) { "preis" -> "Günstigste zuerst"; "neu" -> "Neueste zuerst"; else -> "Nächste zuerst" } },
                    { sortierung = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        when {
            s.leihbar.isEmpty() -> Text(
                "Gerade steht nichts im Markt — gib ein Gesuch auf, dann bringt es dir jemand.",
                style = Schrift.Klein,
                color = Farben.TextSehrLeise,
            )
            sichtbar.isEmpty() -> Text("Für diese Fahrzeugart steht gerade nichts im Markt.", style = Schrift.Klein, color = Farben.TextSehrLeise)
        }
        sichtbar.forEach { a ->
            Leihzeile(
                inhalt = {
                    Text(a.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                    Text("${a.typ} · ${angebotstext(a.tage, a.preis, a.entfernungKm)}", style = Schrift.Klein, color = Farben.TextLeise)
                    Namenssatz("${a.ort} · ", a.von ?: "eine Leitstelle", a.vonBenutzername)
                    if (eigeneWachen.size > 1) Leisesatz("fährt zu ${zielFuer(a)?.name ?: "—"}", winzig = true)
                },
                griffe = {
                    Knopf("Leihen", {
                        val ziel = zielFuer(a) ?: run { fehler = "Du hast keine Wache, auf der dieses Fahrzeug stehen dürfte."; return@Knopf }
                        tun("Das Mieten ging nicht.") { welt.wege.leiheMieten(it, a.id, ziel.id); welt.betriebLaden() }
                    }, kompakt = true, aktiv = !sendet)
                },
            )
        }

        // --------------------------------------------------------- Gesuche
        var gesuchOffen by remember { mutableStateOf(false) }
        Blockkopf("Gesuch aufgeben", gesuchOffen) { gesuchOffen = !gesuchOffen }
        if (gesuchOffen) {
            var gesuchWache by remember { mutableStateOf<WeltWache?>(null) }
            var gesuchKategorie by remember { mutableStateOf<String?>(null) }
            var gesuchTage by remember { mutableStateOf(2) }
            var gesuchPreis by remember { mutableStateOf(100) }
            Leisesatz("Sag, was dir fehlt — bezahlt wird erst, wenn eines losfährt.", winzig = true)
            Auswahl("Fährt zu", eigeneWachen, gesuchWache, { it.name }, { gesuchWache = it; gesuchKategorie = null }, platzhalter = "Wähle eine Wache")
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
                aktiv = gesuchWache != null,
            )
            val deckel = gesuchKategorie?.let { s.gesuchshoechstpreise[it] } ?: 0
            if (deckel in 1 until gesuchPreis) gesuchPreis = deckel
            Etikett("Für ${tage(gesuchTage)}")
            Regler(gesuchTage, 1..7, 1, { gesuchTage = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Etikett("Du zahlst", Modifier.weight(1f))
                Text(
                    "${zahl(gesuchPreis)} Credits" + if (deckel > 0) " · höchstens ${zahl(deckel)}" else "",
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                    color = Farben.TextLeise,
                )
            }
            Regler(gesuchPreis, 0..maxOf(1, deckel), maxOf(10, Math.round((if (deckel > 0) deckel else 100) / 100.0).toInt() * 10), { gesuchPreis = it })
            Knopf(if (sendet) "Wird aufgegeben …" else "Gesuch aufgeben", {
                val w = gesuchWache ?: return@Knopf
                val kat = gesuchKategorie ?: return@Knopf
                tun("Das Gesuch ging nicht raus.") { welt.wege.gesuchAufgeben(it, kat, w.id, gesuchTage, gesuchPreis) }
            }, art = Knopfart.Haupt, aktiv = !sendet && gesuchWache != null && gesuchKategorie != null)
        }
        if (s.meineGesuche.isNotEmpty()) {
            Ueberschrift("Deine Gesuche", Modifier.padding(top = Abstand.Klein))
            s.meineGesuche.forEach { g ->
                Leihzeile(
                    wartet = true,
                    inhalt = {
                        Text(g.kategorie, style = Schrift.MonoKlein, color = Farben.Text)
                        Text(
                            "${tage(g.tage)} · ${if (g.preis > 0) "${zahl(g.preis)} Credits" else "kostenlos"} · zu ${g.wache}",
                            style = Schrift.Klein,
                            color = Farben.TextLeise,
                        )
                    },
                    griffe = {
                        Knopf("Zurückziehen", { tun("Das ging nicht.") { welt.wege.gesuchZurueckziehen(it, g.id) } }, kompakt = true, art = Knopfart.Leise, aktiv = !sendet)
                    },
                )
            }
        }
    } else {
        // ----------------------------------------------------------- Einstellen
        var einstellenOffen by remember { mutableStateOf(false) }
        Blockkopf("Einstellen", einstellenOffen) { einstellenOffen = !einstellenOffen }
        if (einstellenOffen) {
            val drin = s.eingestellt.map { it.fahrzeugId }.toSet()
            val einstellbar = zustand.fahrzeuge.filter { it.lage == "Wache" && !it.geliehen && it.id !in drin }
            var fahrzeug by remember { mutableStateOf<WeltFahrzeug?>(null) }
            var anzahlTage by remember { mutableStateOf(2) }
            var preis by remember { mutableStateOf(200) }
            Leisesatz("Bleibt einsatzbereit, bis jemand mietet — erst dann fährt es los.", winzig = true)
            if (einstellbar.isEmpty()) {
                Text("Eingestellt werden kann nur, was gerade auf seiner Wache steht.", style = Schrift.Klein, color = Farben.TextSehrLeise)
            } else {
                Auswahl("Fahrzeug", einstellbar, fahrzeug, { "${it.funkrufname} · ${it.typ}" }, { fahrzeug = it }, platzhalter = "Wähle ein Fahrzeug")
                val hoechst = fahrzeug?.let { s.hoechstpreise[it.id] } ?: 0
                if (hoechst in 1 until preis) preis = hoechst
                Etikett("Für ${tage(anzahlTage)}")
                Regler(anzahlTage, 1..7, 1, { anzahlTage = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Etikett("Preis", Modifier.weight(1f))
                    Text(
                        "${zahl(preis)} Credits" + if (hoechst > 0) " · höchstens ${zahl(hoechst)}" else "",
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = Farben.TextLeise,
                    )
                }
                Regler(preis, 0..maxOf(1, hoechst), maxOf(10, Math.round((if (hoechst > 0) hoechst else 100) / 100.0).toInt() * 10), { preis = it })
                Knopf(if (sendet) "Wird eingestellt …" else "In den Markt stellen", {
                    val f = fahrzeug ?: return@Knopf
                    tun("Das Einstellen ging nicht.") { welt.wege.leiheAnbieten(it, f.id, anzahlTage, preis) }
                    fahrzeug = null
                }, art = Knopfart.Haupt, aktiv = !sendet && fahrzeug != null)
            }
        }

        if (s.gesuche.isNotEmpty()) {
            Ueberschrift("Gesucht", Modifier.padding(top = Abstand.Klein))
            Leisesatz("Das brauchen andere — hinschicken, Preis kassieren, kommt allein zurück.", winzig = true)
            s.gesuche.forEach { g ->
                val passende = zustand.fahrzeuge.filter { f ->
                    f.lage == "Wache" && !f.geliehen && zustand.vorlage(f.vorlageId)?.kategorie == g.kategorie
                }
                var wahl by remember(g.id) { mutableStateOf<WeltFahrzeug?>(null) }
                Leihzeile(
                    inhalt = {
                        Text(g.kategorie, style = Schrift.MonoKlein, color = Farben.Text)
                        Text(angebotstext(g.tage, g.preis, g.entfernungKm), style = Schrift.Klein, color = Farben.TextLeise)
                        Namenssatz("${g.wache} · ", g.von ?: "eine Leitstelle", g.vonBenutzername)
                        when {
                            passende.size > 1 -> Auswahl("Fahrzeug", passende, wahl ?: passende.first(), { "${it.funkrufname} · ${it.typ}" }, { wahl = it })
                            passende.isEmpty() -> Text("Dein passendes Fahrzeug ist gerade unterwegs.", style = Schrift.Winzig, color = Farben.SignalHell)
                        }
                    },
                    griffe = {
                        Knopf("Hinschicken", {
                            val f = wahl ?: passende.firstOrNull() ?: run {
                                fehler = "Gerade steht kein passendes Fahrzeug auf einer deiner Wachen."
                                return@Knopf
                            }
                            tun("Das Hinschicken ging nicht.") { welt.wege.gesuchBedienen(it, g.id, f.id); welt.betriebLaden() }
                        }, kompakt = true, aktiv = !sendet && passende.isNotEmpty())
                    },
                )
            }
        }

        if (s.eingestellt.isNotEmpty()) {
            Ueberschrift("Im Markt", Modifier.padding(top = Abstand.Klein))
            s.eingestellt.forEach { a ->
                Leihzeile(
                    wartet = true,
                    inhalt = {
                        Text(a.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                        Text("${a.typ} · ${angebotstext(a.tage, a.preis, a.entfernungKm)}", style = Schrift.Klein, color = Farben.TextLeise)
                        if (!a.verfuegbar) {
                            Text("Gerade unterwegs — solange greift niemand darauf zu.", style = Schrift.Winzig, color = Farben.SignalHell)
                        }
                    },
                    griffe = {
                        Knopf("Zurückziehen", { tun("Das ging nicht.") { welt.wege.leiheZuruecknehmen(it, a.id) } }, kompakt = true, art = Knopfart.Leise, aktiv = !sendet)
                    },
                )
            }
        }
        if (s.verliehen.isNotEmpty()) {
            Ueberschrift("Verliehen", Modifier.padding(top = Abstand.Klein))
            s.verliehen.forEach { f ->
                Leihzeile(
                    inhalt = {
                        Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                        Namenssatz("${f.typ} · bei ", f.gegenueber ?: "einer Leitstelle", f.gegenueberBenutzername, " · ${f.wache}", winzig = false)
                    },
                    griffe = {
                        Text(rest(f.bis), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                    },
                )
            }
        }
    }
}

/** Eine Zeile des Leihmarkts — `.leihe__zeile.flaeche--marke`: Amberkante links, Griffe rechts. */
@Composable
private fun Leihzeile(
    wartet: Boolean = false,
    inhalt: @Composable ColumnScope.() -> Unit,
    griffe: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(Rundung.Normal)
            .flaeche()
            .flaechenmarke(wartet = wartet)
            .padding(start = Abstand.Normal, end = Abstand.Klein, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar), content = inhalt)
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) { griffe() }
    }
}

/** Überschrift mit „Öffnen“/„Schließen“ daneben — `.leihe__blockkopf`. */
@Composable
private fun Blockkopf(titel: String, offen: Boolean, beiDruck: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = Abstand.Klein)) {
        Ueberschrift(titel, Modifier.weight(1f))
        Knopf(if (offen) "Schließen" else "Öffnen", beiDruck, kompakt = true, art = Knopfart.Leise)
    }
}


/**
 * Ein Name, der ins Profil führt — wo es einen Benutzernamen und einen Weg gibt.
 * Die Zielfläche ist die Zeile des Namens; ein eigener Knopf daneben wäre ein
 * zweiter Weg zu derselben Person (siehe `Kontoname.vue`).
 */
@Composable
private fun Modifier.zumProfil(benutzername: String?): Modifier {
    val profil = LocalWeltProfil.current
    val ziel = benutzername?.takeIf { it.isNotBlank() } ?: return this
    if (profil == null) return this
    return this.clickable(onClickLabel = "Profil ansehen") { profil(ziel) }
}
