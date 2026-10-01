package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Zeichenknopf
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import de.pagerspass.pagerspass.netz.WeltChronikzeile
import de.pagerspass.pagerspass.netz.WeltWache
import de.pagerspass.pagerspass.netz.WeltWachenseite
import de.pagerspass.pagerspass.netz.WeltZugvorgabe
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ansichten.bildweg
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Die eigenen Wachen — das Gegenstück zu `WachenBlende.vue`.
 *
 * <b>Die Liste zeigt Wachen, nicht Einstellungen.</b> Aufschlagen, Umbenennen,
 * Ausbauen und Abreißen stehen hinter dem Knopf „…“ an der Zeile, untereinander
 * als Fortsetzung der Zeile; aufgeschlagen wird die Wache mit ihrer eigenen
 * Seite (Fahrzeuge, Züge, Wappen, Chronik).
 */
@Composable
fun WachenSeite(welt: Welt, zustand: Weltzustand, werkbank: Werkbank, karte: Weltkartenstand) {
    val bereich = rememberCoroutineScope()
    var menue by remember { mutableStateOf<String?>(null) }
    var gefragt by remember { mutableStateOf<String?>(null) }
    var benennt by remember { mutableStateOf<String?>(null) }
    var neuerName by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    var baut by remember { mutableStateOf(false) }
    var takt by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            takt++
        }
    }
    @Suppress("UNUSED_EXPRESSION") takt

    val rang = zustand.bereiche.mapIndexed { i, b -> b.name to i }.toMap()
    val wachen = zustand.stand?.wachen.orEmpty().sortedWith(
        compareBy<WeltWache>({ rang[it.bereich] ?: rang.size }).thenBy { it.name },
    )
    val deckel = zustand.stand?.wachendeckel ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.Bottom) {
            Text("${wachen.size}/$deckel", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Text("Wachen", style = Schrift.Klein, color = Farben.TextLeise)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.Bottom) {
            Text("${wachen.sumOf { it.belegt }}/${wachen.sumOf { it.stellplaetze }}", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Text("Stellplätze", style = Schrift.Klein, color = Farben.TextLeise)
        }
    }
    Warnsatz(fehler)
    if (wachen.isEmpty()) {
        // Kein eigener Knopf hierher: Der Reiter „Bauen“ atmet, solange keine
        // Wache steht — ein zweiter Weg zum selben Ziel wäre einer zu viel.
        Leerhinweis("Noch keine Wache. „Bauen“ in der Bar setzt die erste.")
        return
    }
    val fahrt = zustand.fahrt

    wachen.forEach { w ->
        val offen = menue == w.id
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier
                .fillMaxWidth()
                .clip(Rundung.Normal)
                .flaeche()
                .flaechenmarke()
                .padding(start = Abstand.Normal, end = Abstand.Klein, top = Abstand.Klein, bottom = Abstand.Klein),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (benennt == w.id) {
                    Feld(
                        wert = neuerName,
                        beiAenderung = { neuerName = it.take(60) },
                        platzhalter = "Neuer Name",
                        modifier = Modifier.weight(1f),
                    )
                    Knopf("Speichern", {
                        bereich.launch {
                            fehler = welt.handlung("Das Umbenennen ging nicht.") {
                                welt.wege.wacheUmbenennen(it, w.id, neuerName.trim())
                                welt.standLaden()
                            }
                            if (fehler == null) { benennt = null; menue = null }
                        }
                    }, kompakt = true, aktiv = neuerName.trim().length >= 3)
                    Knopf("Abbrechen", { benennt = null }, kompakt = true, art = Knopfart.Leise)
                } else {
                    Column(Modifier.weight(1f)) {
                        Text(w.name, style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val plaetze = when (w.art) {
                            "Lehrgangseinrichtung" -> "${w.stellplaetze} ${if (w.stellplaetze == 1) "Lehrsaal" else "Lehrsäle"}"
                            "Werkstatt" -> "${w.buehnenBelegt}/${w.buehnen.takeIf { it > 0 } ?: w.stellplaetze} Bühnen"
                            else -> "${w.belegt}/${w.stellplaetze} Plätze"
                        }
                        Text(
                            "${artname(w.art)} · $plaetze" + if (zustand.mehrereBereiche) " · ${w.bereich}" else "",
                            style = Schrift.Klein,
                            color = Farben.TextLeise,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Zeichenknopf(
                        {
                            menue = if (offen) null else w.id
                            gefragt = null
                            benennt = null
                        },
                        "${w.name} bearbeiten",
                        kompakt = true,
                        art = if (offen) Knopfart.Haupt else Knopfart.Normal,
                    ) {
                        Icon(Weltzeichen.Mehr, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            w.ausbauFertigUm?.let { um ->
                val min = fahrt.restMinuten(um) ?: 0
                Text(
                    "Anbau läuft — " + when {
                        min <= 0 -> "gleich fertig"
                        min >= 60 -> "fertig in ${min / 60} h ${min % 60} min"
                        else -> "fertig in $min min"
                    },
                    style = Schrift.MonoKlein,
                    color = Farben.AmberHell,
                )
            }
            if (offen) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind { drawLine(Farben.Rand, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                        .padding(top = Abstand.Winzig),
                ) {
                    Knopf("Wache aufschlagen", {
                        menue = null
                        werkbank.gewaehlteWache = w.id
                        karte.hinschauen(w.lat, w.lon)
                        werkbank.seite = Werkzeug.Wachenseite
                    }, kompakt = true, art = Knopfart.Leise, breit = true)
                    Knopf("Umbenennen", { benennt = w.id; neuerName = w.name; fehler = null }, kompakt = true, art = Knopfart.Leise, breit = true)
                    if (w.ausbauFertigUm == null && w.ausbauPreis != null) {
                        Knopf("Ausbauen: +2 Plätze für ${credits(w.ausbauPreis)} · ${dauerGrob(w.ausbauDauerMinuten ?: 0)}", {
                            baut = true
                            fehler = null
                            bereich.launch {
                                fehler = welt.handlung("Der Ausbau ging nicht.") { welt.wege.wacheAusbauen(it, w.id); welt.standLaden() }
                                baut = false
                                if (fehler == null) menue = null
                            }
                        }, kompakt = true, art = Knopfart.Leise, breit = true, aktiv = !baut)
                    } else if (w.ausbauFertigUm == null) {
                        Leisesatz("Voll ausgebaut.", Modifier.padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig), winzig = true)
                    }
                    if (gefragt != w.id) {
                        Knopf("Abreißen", { gefragt = w.id }, kompakt = true, art = Knopfart.Leise, breit = true)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf("Wirklich abreißen", {
                                bereich.launch {
                                    fehler = welt.handlung("Der Abriss ging nicht.") { welt.wege.wacheAbreissen(it, w.id); welt.allesLaden() }
                                    if (fehler == null) { gefragt = null; menue = null }
                                }
                            }, kompakt = true, art = Knopfart.Gefahr)
                            Knopf("Nein", { gefragt = null }, kompakt = true, art = Knopfart.Leise)
                        }
                    }
                }
            }
        }
    }
}

private val WAPPENZEICHEN = listOf(
    "Keines" to "ohne", "Feuerwehr" to "Feuerwehr", "Rettungsdienst" to "Rettungsdienst", "Thw" to "THW",
    "Polizei" to "Polizei", "Drehleiter" to "Drehleiter", "Rth" to "Rettungshubschrauber", "Boot" to "Rettungsboot",
    "Funkmast" to "Funkmast", "Leitstelle" to "Leitstelle", "Bergwacht" to "Bergwacht", "Wasserrettung" to "Wasserrettung",
)

private fun chronikzeichen(z: WeltChronikzeile): String = when (z.art) {
    "Gegruendet", "Zweigstelle" -> "★"
    "Stufe" -> "▲"
    "WacheGebaut", "WacheAusgebaut" -> "⌂"
    "WacheAbgerissen" -> "×"
    "Werkstatt" -> "⚙"
    "Lehrgang" -> "✓"
    "Grosslage" -> "!"
    "Wochensieg" -> "♛"
    "Streife" -> "◉"
    else -> "·"
}

/**
 * Die Seite einer Wache — das Gegenstück zu `WachenseiteBlende.vue`: Kopf mit
 * Wappen, die drei Zahlen, was auf dem Hof steht, welche Züge daraus gebildet
 * sind, und die Chronik.
 */
@Composable
fun WachenDetailSeite(welt: Welt, zustand: Weltzustand, werkbank: Werkbank) {
    val bereich = rememberCoroutineScope()
    val id = werkbank.gewaehlteWache
    var seite by remember { mutableStateOf<WeltWachenseite?>(null) }
    var laedt by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var gestaltet by remember { mutableStateOf(false) }
    var zeichen by remember { mutableStateOf("Keines") }
    var farbe by remember { mutableStateOf(0) }
    var foto by remember { mutableStateOf(false) }
    var zugArt by remember { mutableStateOf<WeltZugvorgabe?>(null) }
    var zugName by remember { mutableStateOf("") }
    var zugFehler by remember { mutableStateOf<String?>(null) }
    var zugLaeuft by remember { mutableStateOf(false) }
    var offenerZug by remember { mutableStateOf<String?>(null) }

    suspend fun laden() {
        val k = welt.kennung ?: return
        val w = id ?: return
        laedt = true
        runCatching { welt.wege.wachenseite(k, w) }
            .onSuccess {
                seite = it
                zeichen = it.wache.wappenZeichen
                farbe = it.wache.wappenFarbe
                foto = it.wache.fotoZeigen
                fehler = null
            }
            .onFailure { fehler = it.message ?: "Die Wache ließ sich nicht laden." }
        laedt = false
    }
    LaunchedEffect(id) { laden() }

    Warnsatz(fehler)
    if (id == null) {
        Leerhinweis("Wähle eine Wache in der Wachenliste.")
        return
    }
    val s = seite
    if (s == null || s.wache.id != id) {
        if (laedt) Leerhinweis("Wird geladen …")
        return
    }
    val w = s.wache
    val istEinrichtung = !traegtFahrzeuge(w.art)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Rundung.Normal)
            .flaeche()
            .flaechenmarke()
            .padding(start = Abstand.Normal, end = Abstand.Klein, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        // Dasselbe Kontobild wie im Profil und in jeder Liste: die Wachenkennung
        // als Farbschlüssel, der Wachenname für die Initialen — und beim
        // Gestalten schon der Entwurf, damit man sieht, was man wählt.
        Kontobild(
            kennung = w.id,
            anzeigename = w.name,
            wappen = if (gestaltet) zeichen else w.wappenZeichen,
            wappenfarbe = if (gestaltet) farbe else w.wappenFarbe,
            bildAdresse = if (if (gestaltet) foto else w.fotoZeigen) bildweg(LocalWeltServer.current, s.profilbild) else null,
            groesse = 56.dp,
        )
        Column(Modifier.weight(1f)) {
            Text(w.name, style = Schrift.Gross.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                Text("${w.bauart.ifBlank { artname(w.art) }} · ${s.bereich.name}", style = Schrift.Klein, color = Farben.TextLeise)
                if (s.bereich.istZweigstelle) Weltmarke("Zweigstelle", Farben.AmberHell)
            }
        }
        Knopf(if (gestaltet) "Abbrechen" else "Gestalten", {
            if (gestaltet) {
                zeichen = w.wappenZeichen; farbe = w.wappenFarbe; foto = w.fotoZeigen
            }
            gestaltet = !gestaltet
        }, kompakt = true, art = Knopfart.Leise)
    }

    if (gestaltet) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Klein),
        ) {
            Leisesatz(
                "Wappen und Farbe stehen auf der Karte und in jeder Liste. Das Foto ist dein freigegebenes " +
                    "Profilbild — ein eigenes je Wache gibt es nicht.",
            )
            Auswahl(
                etikett = "Zeichen",
                eintraege = WAPPENZEICHEN,
                gewaehlt = WAPPENZEICHEN.firstOrNull { it.first == zeichen },
                aufschrift = { it.second },
                beiWahl = { zeichen = it.first },
            )
            Text("Farbe", style = Schrift.Klein, color = Farben.Text)
            Umbruchreihe {
                Wappen.PALETTE.forEachIndexed { nummer, ton ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp).clickable(onClickLabel = "Farbe ${nummer + 1}") { farbe = nummer },
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(ton, CircleShape)
                                .border(2.dp, if (farbe == nummer) Farben.Text else Farben.Rand, CircleShape),
                        )
                    }
                }
            }
            Hakenzeile("Mein Profilbild an dieser Wache zeigen", foto, { foto = it })
            Knopf("Übernehmen", {
                bereich.launch {
                    fehler = welt.handlung("Das Wappen ließ sich nicht setzen.") {
                        welt.wege.wappenSetzen(it, w.id, zeichen, farbe, foto)
                    }
                    if (fehler == null) {
                        gestaltet = false
                        laden()
                        welt.standLaden()
                    }
                }
            }, art = Knopfart.Haupt, kompakt = true)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        if (!istEinrichtung) Kennzahl("${w.belegt}/${w.stellplaetze}", "Stellplätze", Modifier.weight(1f))
        else Kennzahl(
            "${w.buehnenBelegt}/${w.buehnen.takeIf { it > 0 } ?: w.stellplaetze}",
            if (w.art == "Werkstatt") "Hebebühnen" else "Lehrsäle",
            Modifier.weight(1f),
        )
        Kennzahl("${w.ausbaustufe}", "Ausbaustufe", Modifier.weight(1f))
        Kennzahl("${s.einsaetze}", "Einsätze", Modifier.weight(1f))
    }
    if (w.ausbauPreis != null) {
        Leisesatz("Nächster Ausbau: ${credits(w.ausbauPreis)} · ${w.ausbauDauerMinuten ?: 0} min")
    }

    if (!istEinrichtung) {
        Ueberschrift("Fahrzeuge", Modifier.padding(top = Abstand.Klein))
        if (s.fahrzeuge.isEmpty()) Leerhinweis("Noch kein Fahrzeug auf dieser Wache.")
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
            s.fahrzeuge.forEach { f ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().flaeche().padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(f.typ, style = Schrift.Klein, color = Farben.TextLeise, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    when {
                        f.verschlissen -> Text("verschlissen (${f.zustand} %)", style = Schrift.Klein, color = Farben.SignalHell)
                        f.werkstattFaellig -> Text("bald fällig (${f.zustand} %)", style = Schrift.Klein, color = Farben.Amber)
                    }
                }
            }
        }

        // ------------------------------------------------------------ Züge
        Ueberschrift("Züge", Modifier.padding(top = Abstand.Klein))
        zugFehler?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
        if (s.zuege.isEmpty()) {
            Leerhinweis(
                "Noch kein Zug auf dieser Wache. Ein Zug fasst zusammen, was zusammen ausrückt — danach genügt ein Griff.",
            )
        }
        s.zuege.forEach { z ->
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Klein),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(z.name, style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text, modifier = Modifier.weight(1f))
                    Text(
                        if (z.vollstaendig) "steht" else "fehlt: ${z.offen.joinToString(", ")}",
                        style = Schrift.MonoKlein,
                        color = if (z.vollstaendig) Farben.TextLeise else Farben.Amber,
                    )
                }
                z.plaetze.forEach { p ->
                    val offen = p.pflicht && p.fahrzeug == null
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text(
                            buildAnnotatedString {
                                append(p.bezeichnung)
                                p.beispiel?.let { withStyle(SpanStyle(color = Farben.TextLeise)) { append(" ($it)") } }
                                if (!p.pflicht) withStyle(SpanStyle(color = Farben.TextSehrLeise)) { append(" Kür") }
                            },
                            style = Schrift.Klein,
                            color = if (offen) Farben.Amber else Farben.Text,
                            modifier = Modifier.weight(1f),
                        )
                        Text(p.fahrzeug ?: "— offen —", style = Schrift.MonoKlein, color = if (p.fahrzeug == null) Farben.TextSehrLeise else Farben.Text)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(if (offenerZug == z.id) "Fertig" else "Fahrzeuge wählen", {
                        offenerZug = if (offenerZug == z.id) null else z.id
                    }, kompakt = true)
                    Knopf("Auflösen", {
                        bereich.launch {
                            zugFehler = welt.handlung("Der Zug ließ sich nicht auflösen.") { welt.wege.zugAufloesen(it, z.id) }
                            if (offenerZug == z.id) offenerZug = null
                            laden()
                            welt.zuegeLaden()
                        }
                    }, kompakt = true)
                }
                if (offenerZug == z.id) {
                    s.fahrzeuge.forEach { f ->
                        val drin = f.id in z.fahrzeugIds
                        Hakenzeile(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontFamily = Schrift.Mono)) { append(f.funkrufname) }
                                withStyle(SpanStyle(color = Farben.TextLeise)) { append("  ${f.typ}") }
                            },
                            drin,
                            {
                                bereich.launch {
                                    zugFehler = welt.handlung("Das Fahrzeug ließ sich nicht setzen.") {
                                        welt.wege.zugFahrzeug(it, z.id, f.id, !drin)
                                    }
                                    laden()
                                    welt.zuegeLaden()
                                }
                            },
                        )
                    }
                }
            }
        }
        Auswahl(
            etikett = "Zugart",
            eintraege = s.zugvorgaben,
            gewaehlt = zugArt,
            aufschrift = { it.name },
            unterschrift = { v -> v.plaetze.joinToString(", ") { it.bezeichnung } },
            beiWahl = { zugArt = it },
        )
        Feld(wert = zugName, beiAenderung = { zugName = it.take(40) }, etikett = "Name (frei)", platzhalter = "Löschzug 1")
        Knopf("Zug aufstellen", {
            val art = zugArt ?: return@Knopf
            zugLaeuft = true
            bereich.launch {
                zugFehler = welt.handlung("Der Zug ließ sich nicht aufstellen.") {
                    welt.wege.zugAufstellen(it, w.id, art.id, zugName.trim().ifBlank { null })
                }
                if (zugFehler == null) {
                    zugArt = null
                    zugName = ""
                }
                laden()
                welt.zuegeLaden()
                zugLaeuft = false
            }
        }, kompakt = true, aktiv = zugArt != null && !zugLaeuft)
    }

    Ueberschrift("Chronik", Modifier.padding(top = Abstand.Klein))
    if (s.chronik.isEmpty()) Leerhinweis("Hier steht noch nichts. Was an dieser Wache geschieht, landet hier.")
    s.chronik.forEach { z ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Top) {
            Text(chronikzeichen(z), style = Schrift.MonoKlein, color = Farben.Amber)
            Text(z.text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
            Text(tagUndUhr(z.um), style = Schrift.Klein.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
        }
    }
}

@Composable
private fun Kennzahl(wert: String, wort: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.flaeche().padding(horizontal = Abstand.Normal, vertical = Abstand.Klein)) {
        Text(wert, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        Text(wort, style = Schrift.Klein, color = Farben.TextLeise)
    }
}
