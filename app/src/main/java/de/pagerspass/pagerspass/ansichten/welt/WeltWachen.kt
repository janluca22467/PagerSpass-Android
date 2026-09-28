package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import de.pagerspass.pagerspass.mobil.weltzeit
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.WeltChronikzeile
import de.pagerspass.pagerspass.netz.WeltWache
import de.pagerspass.pagerspass.netz.WeltWachenseite
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Schmuck
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.profilbildAdresse
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

// ------------------------------------------------------------------ Wachenliste

/**
 * Die Wachenliste — übertragen aus `components/welt/WachenBlende.vue`.
 *
 * <b>Die Liste zeigt Wachen, nicht Einstellungen.</b> Je Zeile ein Knopf, der ein kleines
 * Menü aufklappt: aufschlagen, umbenennen, ausbauen, abreißen. Eine laufende Baustelle
 * steht dagegen immer in der Zeile — sie ist ein Zustand, kein Vorgang.
 */
@Composable
fun WeltWachenBlende(welt: Welt, stand: Weltzustand, griffe: Weltgriffe) {
    val bereich = rememberCoroutineScope()

    var fehler by remember { mutableStateOf<String?>(null) }
    var gefragt by remember { mutableStateOf<String?>(null) }
    var benennt by remember { mutableStateOf<String?>(null) }
    var neuerName by remember { mutableStateOf("") }
    var menue by remember { mutableStateOf<String?>(null) }
    var baut by remember { mutableStateOf(false) }

    // Die Restzeit des Ausbaus — halbminütlich genügt.
    val jetzt by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }

    val rang = stand.bereiche.mapIndexed { i, b -> b.name to i }.toMap()
    val wachen = stand.stand?.wachen.orEmpty().sortedWith(
        compareBy<WeltWache> { rang[it.bereich] ?: rang.size }.thenBy { it.name.lowercase(Locale.GERMANY) },
    )
    val deckel = stand.stand?.wachendeckel ?: 0
    val belegtGesamt = wachen.sumOf { it.belegt }
    val plaetzeGesamt = wachen.sumOf { it.stellplaetze }

    fun ausbauRest(w: WeltWache): String? {
        val fertig = weltzeit(w.ausbauFertigUm) ?: return null
        val min = ceil((fertig - (jetzt + welt.fahrt.versatzMs)) / 60_000.0).toInt()
        if (min <= 0) return "gleich fertig"
        return if (min >= 60) "fertig in ${min / 60} h ${min % 60} min" else "fertig in $min min"
    }

    fun dauerText(minuten: Int): String = if (minuten >= 60) "${(minuten / 60.0).roundToInt()} h" else "$minuten min"

    fun tun(standard: String, nachher: Welt.Nachladen, danach: () -> Unit, was: suspend (String) -> Unit) {
        if (baut) return
        baut = true
        fehler = null
        bereich.launch {
            fehler = welt.aktion(standard, nachher, was)
            if (fehler == null) danach()
            baut = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        // Zwei Verhältnisse, zwei Felder — in einer Zeile las man sie als eine Angabe.
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Gross)) {
            Kopfzahl("${wachen.size}/$deckel", "Wachen")
            Kopfzahl("$belegtGesamt/$plaetzeGesamt", "Stellplätze")
        }

        Absage(fehler)

        if (wachen.isEmpty()) {
            Leerhinweis("Noch keine Wache. „Bauen\" unten in der Leiste setzt die erste.")
            return@Column
        }

        wachen.forEach { w ->
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    if (benennt == w.id) {
                        Feld(
                            wert = neuerName,
                            beiAenderung = { neuerName = it.take(60) },
                            modifier = Modifier.weight(1f),
                        )
                        Knopf(
                            "Speichern",
                            {
                                tun("Das Umbenennen ging nicht.", Welt.Nachladen.Stand, { benennt = null; menue = null }) {
                                    welt.wege.wacheUmbenennen(it, w.id, neuerName.trim())
                                }
                            },
                            kompakt = true,
                            aktiv = neuerName.trim().length >= 3 && !baut,
                        )
                        Knopf("Abbrechen", { benennt = null }, art = Knopfart.Leise, kompakt = true)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                            Text(
                                text = w.name,
                                style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                                color = Farben.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = buildString {
                                    append(artname(w.art)).append(" · ")
                                    if (traegtFahrzeuge(w.art)) append("${w.belegt}/${w.stellplaetze} Plätze")
                                    else append("${w.stellplaetze} ${if (w.stellplaetze == 1) "Lehrsaal" else "Lehrsäle"}")
                                    if (stand.mehrereBereiche) append(" · ").append(w.bereich)
                                },
                                style = Schrift.Klein,
                                color = Farben.TextLeise,
                            )
                        }
                        Knopf(
                            "···",
                            {
                                menue = if (menue == w.id) null else w.id
                                gefragt = null
                                benennt = null
                            },
                            art = if (menue == w.id) Knopfart.Haupt else Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                }

                ausbauRest(w)?.let {
                    Text(text = "Anbau läuft — $it", style = Schrift.MonoKlein, color = Farben.Amber)
                }

                if (menue == w.id) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            "Wache aufschlagen",
                            {
                                menue = null
                                griffe.wacheOeffnen(w.id)
                            },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                        Knopf(
                            "Umbenennen",
                            {
                                benennt = w.id
                                neuerName = w.name
                                fehler = null
                            },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                        val preis = w.ausbauPreis
                        if (w.ausbauFertigUm == null && preis != null && preis > 0) {
                            Knopf(
                                "Ausbauen: +2 Plätze für ${zahl(preis)} Credits · ${dauerText(w.ausbauDauerMinuten ?: 0)}",
                                {
                                    tun("Der Ausbau ging nicht.", Welt.Nachladen.Stand, { menue = null }) {
                                        welt.wege.wacheAusbauen(it, w.id)
                                    }
                                },
                                art = Knopfart.Leise,
                                kompakt = true,
                                aktiv = !baut,
                            )
                        }
                        if (gefragt != w.id) {
                            Knopf("Abreißen", { gefragt = w.id }, art = Knopfart.Leise, kompakt = true)
                        } else {
                            Knopf(
                                "Wirklich abreißen",
                                {
                                    tun("Der Abriss ging nicht.", Welt.Nachladen.Beides, { gefragt = null; menue = null }) {
                                        welt.wege.wacheAbreissen(it, w.id)
                                    }
                                },
                                art = Knopfart.Gefahr,
                                kompakt = true,
                                aktiv = !baut,
                            )
                            Knopf("Nein", { gefragt = null }, art = Knopfart.Leise, kompakt = true)
                        }
                    }
                    // Voll ausgebaut: ein Satz statt eines gesperrten Knopfes.
                    if (w.ausbauFertigUm == null && (w.ausbauPreis ?: 0) <= 0) {
                        Text(text = "Voll ausgebaut.", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                    }
                }
            }
        }
    }
}

@Composable
private fun Kopfzahl(zahl: String, wort: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
        Text(text = zahl, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        Text(text = wort, style = Schrift.Klein, color = Farben.TextLeise)
    }
}

// ------------------------------------------------------------------ Wachenseite

/** Das Zeichen einer Chronikzeile — dieselben wie in `WachenseiteBlende.vue`. */
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

private fun zeitText(iso: String): String {
    val ms = weltzeit(iso) ?: return iso
    return SimpleDateFormat("dd.MM., HH:mm", Locale.GERMANY).format(Date(ms))
}

/**
 * Die Seite einer Wache — übertragen aus `components/welt/WachenseiteBlende.vue`: Kopf
 * mit Wappen, Gestaltung, Zahlen, Fahrzeuge, Züge und Chronik.
 */
@Composable
fun WeltWachenseiteBlende(welt: Welt, stand: Weltzustand, katalog: Katalog?, wacheId: String?, griffe: Weltgriffe) {
    val bereich = rememberCoroutineScope()
    val server = serveradresse()

    var seite by remember { mutableStateOf<WeltWachenseite?>(null) }
    var laedt by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var gestaltet by remember { mutableStateOf(false) }
    var zeichenEntwurf by remember { mutableStateOf("Keines") }
    var farbeEntwurf by remember { mutableIntStateOf(0) }
    var fotoEntwurf by remember { mutableStateOf(false) }

    var zugArt by remember { mutableStateOf<String?>(null) }
    var zugName by remember { mutableStateOf("") }
    var zugFehler by remember { mutableStateOf<String?>(null) }
    var zugLaeuft by remember { mutableStateOf(false) }
    var offenerZug by remember { mutableStateOf<String?>(null) }

    suspend fun laden() {
        val id = wacheId ?: return
        laedt = true
        fehler = null
        welt.holen("Die Wache ließ sich nicht laden.") { welt.wege.wachenseite(it, id) }
            .onSuccess {
                seite = it
                zeichenEntwurf = it.wache.wappenZeichen
                farbeEntwurf = it.wache.wappenFarbe
                fotoEntwurf = it.wache.fotoZeigen
            }
            .onFailure { fehler = it.message }
        laedt = false
    }

    LaunchedEffect(wacheId) {
        seite = null
        gestaltet = false
        offenerZug = null
        laden()
    }

    fun zugTun(standard: String, was: suspend (String) -> Unit, danach: () -> Unit = {}) {
        zugFehler = null
        bereich.launch {
            val meldung = welt.aktion(standard, Welt.Nachladen.Nichts, was)
            if (meldung != null) {
                zugFehler = meldung
            } else {
                danach()
                laden()
                // Die Züge stehen auch im Lagendialog — dort sollen sie gleich stimmen.
                welt.zuegeLaden()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        Absage(fehler)
        val s = seite
        when {
            wacheId == null -> {
                Leerhinweis("Wähle eine Wache in der Wachenliste.")
                return@Column
            }
            s == null -> {
                if (laedt) Ladezeile()
                return@Column
            }
        }
        if (s == null) return@Column
        val wache = s.wache
        val istEinrichtung = wache.art == "Werkstatt" || wache.art == "Lehrgangseinrichtung"

        // ------------------------------------------------------- Kopf
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
        ) {
            val foto = if (gestaltet) fotoEntwurf else wache.fotoZeigen
            Kontobild(
                kennung = wache.id,
                anzeigename = wache.name,
                wappen = if (gestaltet) zeichenEntwurf else wache.wappenZeichen,
                wappenfarbe = if (gestaltet) farbeEntwurf else wache.wappenFarbe,
                bildAdresse = if (foto && server != null) profilbildAdresse(server, s.profilbild) else null,
                groesse = 64.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                Text(text = wache.name, style = Schrift.Gross, color = Farben.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${wache.bauart} · ${s.bereich.name}", style = Schrift.Klein, color = Farben.TextLeise)
                    if (s.bereich.istZweigstelle) Weltmarke("Zweigstelle", Farben.BlauHell)
                }
            }
            Knopf(
                if (gestaltet) "Abbrechen" else "Gestalten",
                {
                    if (gestaltet) {
                        zeichenEntwurf = wache.wappenZeichen
                        farbeEntwurf = wache.wappenFarbe
                        fotoEntwurf = wache.fotoZeigen
                        gestaltet = false
                    } else {
                        gestaltet = true
                    }
                },
                art = Knopfart.Leise,
                kompakt = true,
            )
        }

        // ------------------------------------------------------- Gestaltung
        if (gestaltet) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
            ) {
                Text(
                    text = "Wappen und Farbe stehen auf der Karte und in jeder Liste. Das Foto ist dein freigegebenes " +
                        "Profilbild — ein eigenes je Wache gibt es nicht.",
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                )
                Auswahlfeld(
                    platzhalter = "Zeichen",
                    wahlen = Schmuck.WAPPENZEICHEN.map { Wahl(it.id, if (it.id == "Keines") "ohne" else it.name) },
                    gewaehlt = zeichenEntwurf,
                    beiWahl = { zeichenEntwurf = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "Farbe", style = Schrift.Klein, color = Farben.TextLeise)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Wappen.PALETTE.forEachIndexed { nummer, ton ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(ton, CircleShape)
                                .border(if (farbeEntwurf == nummer) 3.dp else 1.dp, if (farbeEntwurf == nummer) Farben.Text else Farben.Rand, CircleShape)
                                .clickable { farbeEntwurf = nummer },
                        )
                    }
                }
                Hakenzeile(text = "Mein Profilbild an dieser Wache zeigen", an = fotoEntwurf, beiWechsel = { fotoEntwurf = it })
                Knopf(
                    "Übernehmen",
                    {
                        val zeichen = zeichenEntwurf
                        val farbe = farbeEntwurf
                        val foto = fotoEntwurf
                        bereich.launch {
                            fehler = welt.aktion("Das Wappen ließ sich nicht setzen.", Welt.Nachladen.Stand) {
                                welt.wege.wappenSetzen(it, wache.id, zeichen, farbe, foto)
                            }
                            if (fehler == null) {
                                gestaltet = false
                                laden()
                            }
                        }
                    },
                    art = Knopfart.Haupt,
                    kompakt = true,
                )
            }
        }

        // ------------------------------------------------------- Zahlen
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            if (!istEinrichtung) {
                Zahlfeld("${wache.belegt}/${wache.stellplaetze}", "Stellplätze", Modifier.weight(1f))
            } else {
                Zahlfeld(
                    "${wache.buehnenBelegt}/${if (wache.buehnen > 0) wache.buehnen else wache.stellplaetze}",
                    if (wache.art == "Werkstatt") "Hebebühnen" else "Lehrsäle",
                    Modifier.weight(1f),
                )
            }
            Zahlfeld(wache.ausbaustufe.toString(), "Ausbaustufe", Modifier.weight(1f))
            Zahlfeld(s.einsaetze.toString(), "Einsätze", Modifier.weight(1f))
        }
        wache.ausbauPreis?.let { preis ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Nächster Ausbau:", style = Schrift.Klein, color = Farben.TextLeise)
                Credits(preis)
                Text(text = "· ${wache.ausbauDauerMinuten ?: 0} min", style = Schrift.Klein, color = Farben.TextLeise)
            }
        }

        // ------------------------------------------------------- Fahrzeuge
        if (!istEinrichtung) {
            Gruppenkopf("Fahrzeuge", s.fahrzeuge.size)
            if (s.fahrzeuge.isEmpty()) {
                Leerhinweis("Noch kein Fahrzeug auf dieser Wache.")
            }
            s.fahrzeuge.forEach { f ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Text(text = f.funkrufname, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    Text(
                        text = f.typ,
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // Der Zustand nur dort, wo er etwas bedeutet.
                    if (f.verschlissen) {
                        Text(text = "verschlissen (${f.zustand} %)", style = Schrift.Winzig, color = Farben.SignalHell)
                    } else if (f.werkstattFaellig) {
                        Text(text = "bald fällig (${f.zustand} %)", style = Schrift.Winzig, color = Farben.Amber)
                    }
                }
            }

            // --------------------------------------------------- Züge
            Gruppenkopf("Züge", s.zuege.size)
            Absage(zugFehler)
            if (s.zuege.isEmpty()) {
                Leerhinweis("Noch kein Zug auf dieser Wache. Ein Zug fasst zusammen, was zusammen ausrückt — danach genügt ein Griff.")
            }
            s.zuege.forEach { z ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = z.name,
                            style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                            color = Farben.Text,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (z.vollstaendig) "steht" else "fehlt: ${z.offen.joinToString(", ")}",
                            style = Schrift.MonoKlein,
                            color = if (z.vollstaendig) Farben.TextLeise else Farben.Amber,
                        )
                    }
                    // Offen steht vorn: Besetzte Plätze treten zurück.
                    z.plaetze.forEach { p ->
                        val offen = p.pflicht && p.fahrzeug == null
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = buildString {
                                    append(p.bezeichnung)
                                    p.beispiel?.let { append(" ($it)") }
                                    if (!p.pflicht) append(" · Kür")
                                },
                                style = Schrift.Klein,
                                color = if (offen) Farben.Text else Farben.TextLeise,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = p.fahrzeug ?: "— offen —",
                                style = Schrift.MonoKlein,
                                color = if (offen) Farben.Amber else Farben.TextSehrLeise,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            if (offenerZug == z.id) "Fertig" else "Fahrzeuge wählen",
                            { offenerZug = if (offenerZug == z.id) null else z.id },
                            kompakt = true,
                        )
                        Knopf(
                            "Auflösen",
                            {
                                zugTun("Der Zug ließ sich nicht auflösen.", { welt.wege.zugAufloesen(it, z.id) }) {
                                    if (offenerZug == z.id) offenerZug = null
                                }
                            },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                    if (offenerZug == z.id) {
                        s.fahrzeuge.forEach { f ->
                            val drin = f.id in z.fahrzeugIds
                            Hakenzeile(
                                text = "${f.funkrufname} · ${f.typ}",
                                an = drin,
                                beiWechsel = {
                                    zugTun("Das Fahrzeug ließ sich nicht setzen.", { welt.wege.zugFahrzeugSetzen(it, z.id, f.id, !drin) })
                                },
                            )
                        }
                    }
                }
            }

            // Einen Zug aufstellen.
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
                Auswahlfeld(
                    platzhalter = "Zugart wählen",
                    wahlen = s.zugvorgaben.map { Wahl(it.id, it.name) },
                    gewaehlt = zugArt,
                    beiWahl = { zugArt = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Feld(wert = zugName, beiAenderung = { zugName = it.take(60) }, platzhalter = "Name (frei), z. B. Löschzug 1")
                Knopf(
                    "Zug aufstellen",
                    {
                        val art = zugArt
                        if (art != null && !zugLaeuft) {
                            zugLaeuft = true
                            val name = zugName.trim().ifEmpty { null }
                            zugTun("Der Zug ließ sich nicht aufstellen.", { welt.wege.zugAufstellen(it, wache.id, art, name) }) {
                                zugArt = null
                                zugName = ""
                            }
                            zugLaeuft = false
                        }
                    },
                    kompakt = true,
                    aktiv = zugArt != null && !zugLaeuft,
                )
            }
        }

        // ------------------------------------------------------- Chronik
        Gruppenkopf("Chronik")
        if (s.chronik.isEmpty()) {
            Leerhinweis("Hier steht noch nichts. Was an dieser Wache geschieht, landet hier.")
        }
        s.chronik.forEach { z ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Top) {
                Text(
                    text = chronikzeichen(z),
                    style = Schrift.MonoKlein,
                    color = Farben.Amber,
                    modifier = Modifier.width(18.dp),
                )
                Text(text = z.text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                Text(text = zeitText(z.um), style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextSehrLeise)
            }
        }
    }
}

@Composable
private fun Zahlfeld(zahl: String, wort: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = modifier.flaeche(ecke = 9.dp).padding(Abstand.Klein),
    ) {
        Text(text = zahl, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        Text(text = wort, style = Schrift.Winzig, color = Farben.TextLeise)
    }
}
