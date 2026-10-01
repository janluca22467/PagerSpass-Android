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
 * <b>Die Liste zeigt Wachen, nicht Einstellungen.</b> Ausbauen, Umbenennen und
 * Abreißen stehen hinter einem Knopf an der Zeile; aufgeschlagen wird die Wache
 * mit ihrer eigenen Seite (Fahrzeuge, Züge, Wappen, Chronik).
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
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
        Text("${wachen.size}/$deckel", style = Schrift.MonoKlein, color = Farben.Text)
        Text("Wachen", style = Schrift.Klein, color = Farben.TextLeise)
        Text("${wachen.sumOf { it.belegt }}/${wachen.sumOf { it.stellplaetze }}", style = Schrift.MonoKlein, color = Farben.Text)
        Text("Stellplätze", style = Schrift.Klein, color = Farben.TextLeise)
    }
    Warnsatz(fehler)
    if (wachen.isEmpty()) {
        // Kein eigener Knopf hierher: Der Reiter „Bauen“ atmet, solange keine
        // Wache steht — ein zweiter Weg zum selben Ziel wäre einer zu viel.
        Leisesatz("Noch keine Wache. „Bauen“ in der Leiste setzt die erste.")
        return
    }
    val fahrt = zustand.fahrt

    wachen.forEach { w ->
        val offen = menue == w.id
        Weltkasten(an = offen) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.size(12.dp).background(wachenfarbe(w.art), androidx.compose.foundation.shape.RoundedCornerShape(3.dp)))
                Column(
                    Modifier.weight(1f).clickable {
                        werkbank.gewaehlteWache = w.id
                        karte.hinschauen(w.lat, w.lon)
                        werkbank.seite = Werkzeug.Wachenseite
                    },
                ) {
                    Text(w.name, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val plaetze = when (w.art) {
                        "Lehrgangseinrichtung" -> "${w.stellplaetze} ${if (w.stellplaetze == 1) "Lehrsaal" else "Lehrsäle"}"
                        "Werkstatt" -> "${w.buehnenBelegt}/${w.buehnen.takeIf { it > 0 } ?: w.stellplaetze} Bühnen"
                        else -> "${w.belegt}/${w.stellplaetze} Plätze"
                    }
                    Text(
                        "${artname(w.art)} · $plaetze" + if (zustand.mehrereBereiche) " · ${w.bereich}" else "",
                        style = Schrift.Winzig,
                        color = Farben.TextSehrLeise,
                    )
                }
                Knopf(if (offen) "Zu" else "…", {
                    menue = if (offen) null else w.id
                    gefragt = null
                    benennt = null
                }, kompakt = true, art = Knopfart.Leise)
            }
            w.ausbauFertigUm?.let { um ->
                val min = fahrt.restMinuten(um) ?: 0
                Text(
                    "Anbau läuft — " + if (min <= 0) "gleich fertig" else "fertig in ${dauer(min)}",
                    style = Schrift.MonoKlein,
                    color = Farben.Amber,
                )
            }
            if (offen) {
                Umbruchreihe {
                    Knopf("Wache aufschlagen", {
                        menue = null
                        werkbank.gewaehlteWache = w.id
                        werkbank.seite = Werkzeug.Wachenseite
                    }, kompakt = true)
                    if (w.ausbauPreis != null && w.ausbauFertigUm == null) {
                        Knopf("Ausbauen: +2 Plätze für ${zahl(w.ausbauPreis)} · ${dauerGrob(w.ausbauDauerMinuten ?: 0)}", {
                            baut = true
                            fehler = null
                            bereich.launch {
                                fehler = welt.handlung("Der Ausbau ging nicht.") { welt.wege.wacheAusbauen(it, w.id); welt.standLaden() }
                                baut = false
                                if (fehler == null) menue = null
                            }
                        }, kompakt = true, aktiv = !baut)
                    }
                    Knopf("Umbenennen", { benennt = w.id; neuerName = w.name }, kompakt = true, art = Knopfart.Leise)
                    Knopf("Abreißen", { gefragt = w.id }, kompakt = true, art = Knopfart.Leise)
                }
                if (benennt == w.id) {
                    Feld(wert = neuerName, beiAenderung = { neuerName = it.take(60) }, etikett = "Neuer Name")
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
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
                    }
                }
                if (gefragt == w.id) {
                    Rueckfrage(
                        frage = "${w.name} abreißen? Die Fahrzeuge darauf werden mit verkauft; ein Teil der Kaufpreise kommt zurück (wie viel, hängt von deiner Stufe ab).",
                        ja = "Abreißen",
                        beiJa = {
                            bereich.launch {
                                fehler = welt.handlung("Der Abriss ging nicht.") { welt.wege.wacheAbreissen(it, w.id); welt.allesLaden() }
                                if (fehler == null) { gefragt = null; menue = null }
                            }
                        },
                        beiNein = { gefragt = null },
                    )
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
 * Die Seite einer Wache — das Gegenstück zu `WachenseiteBlende.vue`: was auf dem
 * Hof steht, welche Züge daraus gebildet sind, das Wappen und die Chronik.
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

    if (id == null) {
        Leisesatz("Tipp eine Wache auf der Karte oder in der Liste an.")
        return
    }
    Warnsatz(fehler)
    val s = seite
    if (s == null || s.wache.id != id) {
        if (laedt) Ladezeile()
        return
    }
    val w = s.wache

    Weltkasten(randfarbe = Farben.AmberTief) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            // Dasselbe Kontobild wie im Profil und in jeder Liste (`WachenseiteBlende.vue`):
            // die Wachenkennung als Farbschlüssel, der Wachenname für die Initialen —
            // und beim Gestalten schon der Entwurf, damit man sieht, was man wählt.
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
                Text(
                    "${w.bauart.ifBlank { artname(w.art) }} · ${s.bereich.name}" + if (s.bereich.istZweigstelle) " · Zweigstelle" else "",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
            }
            Knopf(if (gestaltet) "Abbrechen" else "Gestalten", {
                if (gestaltet) {
                    zeichen = w.wappenZeichen; farbe = w.wappenFarbe; foto = w.fotoZeigen
                }
                gestaltet = !gestaltet
            }, kompakt = true, art = Knopfart.Leise)
        }
    }

    if (gestaltet) {
        Leisesatz("Das Wappen steht an der Wache auf der Karte und auf dieser Seite.", winzig = true)
        Auswahl(
            etikett = "Zeichen",
            eintraege = WAPPENZEICHEN,
            gewaehlt = WAPPENZEICHEN.firstOrNull { it.first == zeichen },
            aufschrift = { it.second },
            beiWahl = { zeichen = it.first },
        )
        Text("Farbe", style = Schrift.Klein, color = Farben.TextLeise)
        Umbruchreihe {
            Wappen.PALETTE.forEachIndexed { nummer, ton ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(ton, CircleShape)
                        .border(2.dp, if (farbe == nummer) Farben.Text else Farben.Rand, CircleShape)
                        .clickable { farbe = nummer },
                )
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

    Umbruchreihe {
        if (traegtFahrzeuge(w.art)) Kennzahl("${w.belegt}/${w.stellplaetze}", "Stellplätze")
        else Kennzahl(
            "${w.buehnenBelegt}/${w.buehnen.takeIf { it > 0 } ?: w.stellplaetze}",
            if (w.art == "Werkstatt") "Hebebühnen" else "Lehrsäle",
        )
        Kennzahl("${w.ausbaustufe}", "Ausbaustufe")
        Kennzahl("${s.einsaetze}", "Einsätze")
    }
    if (w.ausbauPreis != null) {
        Leisesatz("Nächste Ausbaustufe: +2 Plätze für ${credits(w.ausbauPreis)} · ${w.ausbauDauerMinuten ?: 0} min", winzig = true)
    }

    if (traegtFahrzeuge(w.art)) {
        Ueberschrift("Fahrzeuge")
        if (s.fahrzeuge.isEmpty()) Leisesatz("Noch kein Fahrzeug auf dieser Wache.")
        s.fahrzeuge.forEach { f ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Fmsplakette(f.status)
                Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(f.typ, style = Schrift.Winzig, color = Farben.TextLeise)
                when {
                    f.verschlissen -> Weltmarke("verschlissen (${f.zustand} %)", Farben.SignalHell)
                    f.werkstattFaellig -> Weltmarke("bald fällig (${f.zustand} %)", Farben.Amber)
                }
            }
        }
        Knopf("Fahrzeug kaufen", { werkbank.seite = Werkzeug.Fahrzeugkauf }, kompakt = true)

        // ------------------------------------------------------------ Züge
        Ueberschrift("Züge")
        Warnsatz(zugFehler)
        if (s.zuege.isEmpty()) Leisesatz("Noch kein Zug. Ein Zug alarmiert mehrere Fahrzeuge mit einem Griff.", winzig = true)
        s.zuege.sortedBy { it.vollstaendig }.forEach { z ->
            Weltkasten {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(z.name, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                    Weltmarke(if (z.vollstaendig) "steht" else "fehlt: ${z.offen.joinToString(", ")}", if (z.vollstaendig) Farben.GruenHell else Farben.Amber)
                }
                z.plaetze.forEach { p ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text(
                            p.bezeichnung + (p.beispiel?.let { " ($it)" } ?: "") + if (!p.pflicht) " · Kür" else "",
                            style = Schrift.Winzig,
                            color = Farben.TextLeise,
                            modifier = Modifier.weight(1f),
                        )
                        Text(p.fahrzeug ?: "— offen —", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = if (p.fahrzeug == null) Farben.TextSehrLeise else Farben.Text)
                    }
                }
                Umbruchreihe {
                    Knopf(if (offenerZug == z.id) "Fertig" else "Fahrzeuge wählen", {
                        offenerZug = if (offenerZug == z.id) null else z.id
                    }, kompakt = true, art = Knopfart.Leise)
                    Knopf("Auflösen", {
                        bereich.launch {
                            zugFehler = welt.handlung("Der Zug ließ sich nicht auflösen.") { welt.wege.zugAufloesen(it, z.id) }
                            if (offenerZug == z.id) offenerZug = null
                            laden()
                            welt.zuegeLaden()
                        }
                    }, kompakt = true, art = Knopfart.Leise)
                }
                if (offenerZug == z.id) {
                    s.fahrzeuge.forEach { f ->
                        val drin = f.id in z.fahrzeugIds
                        Wahlzeile(an = drin, beiWechsel = {
                            bereich.launch {
                                zugFehler = welt.handlung("Das Fahrzeug ließ sich nicht setzen.") {
                                    welt.wege.zugFahrzeug(it, z.id, f.id, !drin)
                                }
                                laden()
                                welt.zuegeLaden()
                            }
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                                Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                                Text(f.typ, style = Schrift.Winzig, color = Farben.TextLeise)
                            }
                        }
                    }
                }
            }
        }
        if (s.zugvorgaben.isNotEmpty()) {
            Auswahl(
                etikett = "Zugart",
                eintraege = s.zugvorgaben,
                gewaehlt = zugArt,
                aufschrift = { it.name },
                unterschrift = { v -> v.plaetze.joinToString(", ") { it.bezeichnung } },
                beiWahl = { zugArt = it },
            )
            Feld(wert = zugName, beiAenderung = { zugName = it.take(40) }, etikett = "Name (frei)", platzhalter = "1. Löschzug")
            Knopf("Zug aufstellen", {
                val art = zugArt ?: return@Knopf
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
                }
            }, kompakt = true, aktiv = zugArt != null)
        }
    }

    Ueberschrift("Chronik")
    if (s.chronik.isEmpty()) Leisesatz("Hier ist noch nichts geschehen.", winzig = true)
    s.chronik.forEach { z ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Top) {
            Text(chronikzeichen(z), style = Schrift.MonoKlein, color = Farben.Amber)
            Text(z.text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
            Text(tagUndUhr(z.um), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
        }
    }
}

@Composable
private fun Kennzahl(wert: String, wort: String) {
    Column(
        modifier = Modifier
            .background(Farben.BgTief, androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
            .border(1.dp, Farben.Rand, androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(wert, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        Text(wort, style = Schrift.Winzig, color = Farben.TextSehrLeise)
    }
}

