package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.mobil.weltzeit
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltGesuch
import de.pagerspass.pagerspass.netz.WeltLeihangebot
import de.pagerspass.pagerspass.netz.WeltLeihstand
import de.pagerspass.pagerspass.netz.WeltWache
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

private fun tagewort(tage: Int): String = "$tage ${if (tage == 1) "Tag" else "Tage"}"

private fun angebotstext(tage: Int, preis: Int, entfernungKm: Double): String {
    val geld = if (preis > 0) "${zahl(preis)} Credits" else "kostenlos"
    val km = if (entfernungKm == Math.floor(entfernungKm)) entfernungKm.toLong().toString() else komma(entfernungKm, 1)
    return "${tagewort(tage)} · $geld · $km km"
}

/** Die Schrittweite eines Preisreglers — ein Hundertstel des Deckels, mindestens zehn. */
private fun preisschritt(deckel: Int): Int = maxOf(10, ((if (deckel > 0) deckel else 100) / 100.0).roundToInt() * 10)

/**
 * Der Leihmarkt — übertragen aus `components/welt/LeiheBlende.vue`.
 *
 * Zwei Reiter: <b>Ausleihen</b> (geliehen, leihbar, Gesuch aufgeben, eigene Gesuche) und
 * <b>Verleihen</b> (einstellen, fremde Gesuche bedienen, im Markt, verliehen). Jede
 * Minute neu, denn der Markt ist aller Leute Markt.
 */
@Composable
fun WeltLeiheBlende(welt: Welt, stand: Weltzustand, katalog: Katalog?, griffe: Weltgriffe) {
    val bereich = rememberCoroutineScope()
    val vorlagen = remember(katalog) { katalog?.fahrzeuge.orEmpty().associateBy { it.id } }

    var markt by remember { mutableStateOf<WeltLeihstand?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var ausleihen by remember { mutableStateOf(true) }

    var fahrzeug by remember { mutableStateOf<String?>(null) }
    var tage by remember { mutableIntStateOf(2) }
    var preis by remember { mutableIntStateOf(200) }
    var zielWache by remember { mutableStateOf<String?>(null) }
    var filterKategorie by remember { mutableStateOf<String?>(null) }
    var sortierung by remember { mutableStateOf("entfernung") }
    var gesuchWache by remember { mutableStateOf<String?>(null) }
    var gesuchKategorie by remember { mutableStateOf<String?>(null) }
    var gesuchTage by remember { mutableIntStateOf(2) }
    var gesuchPreis by remember { mutableIntStateOf(100) }
    var bedienWahl by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var rueckgabeGefragt by remember { mutableStateOf<String?>(null) }
    var gesuchOffen by remember { mutableStateOf(false) }
    var einstellenOffen by remember { mutableStateOf(false) }

    suspend fun laden() {
        welt.holen("Der Leihmarkt ließ sich nicht laden.") { welt.wege.leihen(it) }
            .onSuccess {
                markt = it
                fehler = null
            }
            .onFailure { fehler = it.message }
    }

    LaunchedEffect(Unit) {
        while (true) {
            laden()
            delay(60_000)
        }
    }

    val jetzt by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }

    fun tun(standard: String, nachher: Welt.Nachladen = Welt.Nachladen.Nichts, danach: () -> Unit = {}, was: suspend (String) -> Unit) {
        if (sendet) return
        sendet = true
        fehler = null
        bereich.launch {
            fehler = welt.aktion(standard, nachher, was)
            if (fehler == null) {
                danach()
                laden()
            }
            sendet = false
        }
    }

    fun restText(bis: String?): String {
        val ende = weltzeit(bis) ?: return ""
        val ms = ende - (jetzt + welt.fahrt.versatzMs)
        if (ms <= 0) return "kommt zurück"
        val stunden = ceil(ms / 3_600_000.0).toInt()
        if (stunden < 24) return "noch $stunden h"
        val t = ceil(stunden / 24.0).toInt()
        return "noch ${tagewort(t)}"
    }

    val eigeneWachen = stand.stand?.wachen.orEmpty().filter { traegtFahrzeuge(it.art) }

    fun vorlageVon(a: WeltLeihangebot): Fahrzeugvorlage? = a.vorlageId?.let { vorlagen[it] }

    fun zielFuer(a: WeltLeihangebot): WeltWache? {
        val v = vorlageVon(a)
        val passende = if (v != null) eigeneWachen.filter { passtZurWache(it.art, v) } else eigeneWachen
        return passende.firstOrNull { it.id == zielWache } ?: passende.firstOrNull()
    }

    fun passendeFahrzeuge(g: WeltGesuch): List<WeltFahrzeug> =
        stand.betrieb?.fahrzeuge.orEmpty().filter { f ->
            f.lage == "Wache" && !f.geliehen && vorlagen[f.vorlageId]?.kategorie == g.kategorie
        }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Pille("Ausleihen", ausleihen, { ausleihen = true })
            Pille("Verleihen", !ausleihen, { ausleihen = false })
        }
        Absage(fehler)

        val m = markt
        if (m == null) {
            if (fehler == null) Ladezeile("Der Leihmarkt wird geladen …")
            return@Column
        }

        if (ausleihen) {
            // --------------------------------------------------- Geliehen
            if (m.geliehen.isNotEmpty()) {
                Gruppenkopf("Geliehen", m.geliehen.size)
                m.geliehen.forEach { f ->
                    val rest = restText(f.bis)
                    Zeilenkarte {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                                Rufname(f.funkrufname)
                                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                                    Klein("${f.typ} · von")
                                    Gegenueber(f.gegenueber, f.gegenueberBenutzername, "einer Leitstelle", griffe)
                                }
                                Klein(f.wache)
                            }
                            Text(text = rest, style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextLeise)
                        }
                        if (rest != "kommt zurück") {
                            if (rueckgabeGefragt != f.fahrzeugId) {
                                Knopf("Zurückgeben", { rueckgabeGefragt = f.fahrzeugId }, art = Knopfart.Leise, kompakt = true, aktiv = !sendet)
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                                    Knopf(
                                        "Ohne Erstattung",
                                        { tun("Das ging nicht.", Welt.Nachladen.Betrieb, { rueckgabeGefragt = null }) { welt.wege.leiheZurueckgeben(it, f.fahrzeugId) } },
                                        kompakt = true,
                                        aktiv = !sendet,
                                    )
                                    Knopf("Nein", { rueckgabeGefragt = null }, art = Knopfart.Leise, kompakt = true)
                                }
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------- Leihbar
            Gruppenkopf("Leihbar", m.leihbar.size)
            Satz("Sofort bezahlt, zählt nicht auf deine Stellplätze, fährt nach Ablauf allein heim.")

            val kategorien = m.leihbar.mapNotNull { vorlageVon(it)?.kategorie }.distinct().sortedBy { it.lowercase() }
            var liste = m.leihbar
            filterKategorie?.let { k -> liste = liste.filter { vorlageVon(it)?.kategorie == k } }
            val sichtbar = when (sortierung) {
                "preis" -> liste.sortedBy { it.preis }
                "neu" -> liste.sortedByDescending { weltzeit(it.angebotenUm) ?: 0L }
                else -> liste.sortedBy { it.entfernungKm }
            }

            if (eigeneWachen.size > 1 && sichtbar.isNotEmpty()) {
                Auswahlfeld(
                    platzhalter = "Fährt zu: erste passende Wache",
                    wahlen = listOf(Wahl<String?>(null, "Erste passende Wache")) + eigeneWachen.map { Wahl<String?>(it.id, it.name) },
                    gewaehlt = zielWache,
                    beiWahl = { zielWache = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (m.leihbar.size > 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Auswahlfeld(
                        platzhalter = "Alle Fahrzeugarten",
                        wahlen = listOf(Wahl<String?>(null, "Alle Fahrzeugarten")) + kategorien.map { Wahl<String?>(it, it) },
                        gewaehlt = filterKategorie,
                        beiWahl = { filterKategorie = it },
                        modifier = Modifier.weight(1f),
                    )
                    Auswahlfeld(
                        platzhalter = "Ordnung",
                        wahlen = listOf(
                            Wahl("entfernung", "Nächste zuerst"),
                            Wahl("preis", "Günstigste zuerst"),
                            Wahl("neu", "Neueste zuerst"),
                        ),
                        gewaehlt = sortierung,
                        beiWahl = { sortierung = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            when {
                m.leihbar.isEmpty() -> Satz("Gerade steht nichts im Markt — gib ein Gesuch auf, dann bringt es dir jemand.")
                sichtbar.isEmpty() -> Satz("Für diese Fahrzeugart steht gerade nichts im Markt.")
            }
            sichtbar.forEach { a ->
                Zeilenkarte {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                            Rufname(a.funkrufname)
                            Klein("${a.typ} · ${angebotstext(a.tage, a.preis, a.entfernungKm)}")
                            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                                Satz("${a.ort} ·")
                                Gegenueber(a.von, a.vonBenutzername, "eine Leitstelle", griffe)
                            }
                            if (eigeneWachen.size > 1) Satz("fährt zu ${zielFuer(a)?.name ?: "—"}")
                        }
                        Knopf(
                            "Leihen",
                            {
                                val ziel = zielFuer(a)
                                if (ziel == null) {
                                    fehler = "Du hast keine Wache, auf der dieses Fahrzeug stehen dürfte."
                                } else {
                                    tun("Das Mieten ging nicht.", Welt.Nachladen.Betrieb) { welt.wege.leiheMieten(it, a.id, ziel.id) }
                                }
                            },
                            kompakt = true,
                            aktiv = !sendet,
                        )
                    }
                }
            }

            // --------------------------------------------------- Gesuch
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Breit { Gruppenkopf("Gesuch aufgeben") }
                Knopf(if (gesuchOffen) "Schließen" else "Öffnen", { gesuchOffen = !gesuchOffen }, art = Knopfart.Leise, kompakt = true)
            }
            if (gesuchOffen) {
                Satz("Sag, was dir fehlt — bezahlt wird erst, wenn eines losfährt.")
                val wache = eigeneWachen.firstOrNull { it.id == gesuchWache }
                val deckelTafel = m.gesuchshoechstpreise
                val gesuchKategorien = if (wache == null) {
                    emptyList()
                } else {
                    deckelTafel.keys.filter { k ->
                        katalog?.fahrzeuge.orEmpty().any { it.kategorie == k && passtZurWache(wache.art, it) }
                    }.sortedBy { it.lowercase() }
                }
                LaunchedEffect(gesuchKategorien) {
                    if (gesuchKategorie !in gesuchKategorien) gesuchKategorie = gesuchKategorien.firstOrNull()
                }
                val deckel = gesuchKategorie?.let { deckelTafel[it] } ?: 0
                LaunchedEffect(deckel) {
                    if (deckel > 0 && gesuchPreis > deckel) gesuchPreis = deckel
                }

                Auswahlfeld(
                    platzhalter = "Fährt zu: Wähle eine Wache",
                    wahlen = eigeneWachen.map { Wahl(it.id, it.name) },
                    gewaehlt = gesuchWache,
                    beiWahl = { gesuchWache = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Auswahlfeld(
                    platzhalter = if (gesuchWache != null) "Wähle eine Fahrzeugart" else "Erst die Wache wählen",
                    wahlen = gesuchKategorien.map { Wahl(it, it) },
                    gewaehlt = gesuchKategorie,
                    beiWahl = { gesuchKategorie = it },
                    aktiv = gesuchWache != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Schieber("Für ${tagewort(gesuchTage)}", null, gesuchTage, 1, 7, 1, true) { gesuchTage = it }
                Schieber(
                    "Du zahlst",
                    "${zahl(gesuchPreis)} Credits" + if (deckel > 0) " · höchstens ${zahl(deckel)}" else "",
                    gesuchPreis,
                    0,
                    maxOf(deckel, 1),
                    preisschritt(deckel),
                    gesuchKategorie != null,
                ) { gesuchPreis = it }
                Knopf(
                    if (sendet) "Wird aufgegeben …" else "Gesuch aufgeben",
                    {
                        val w = gesuchWache
                        val k = gesuchKategorie
                        if (w != null && k != null) {
                            val t = gesuchTage
                            val p = gesuchPreis
                            tun("Das Gesuch ging nicht raus.") { welt.wege.gesuchAufgeben(it, k, w, t, p) }
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = !sendet && gesuchWache != null && gesuchKategorie != null,
                )
            }

            if (m.meineGesuche.isNotEmpty()) {
                Gruppenkopf("Deine Gesuche", m.meineGesuche.size)
                m.meineGesuche.forEach { g ->
                    Zeilenkarte {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                                Rufname(g.kategorie)
                                Klein("${tagewort(g.tage)} · ${if (g.preis > 0) "${zahl(g.preis)} Credits" else "kostenlos"} · zu ${g.wache}")
                            }
                            Knopf(
                                "Zurückziehen",
                                { tun("Das ging nicht.") { welt.wege.gesuchZurueckziehen(it, g.id) } },
                                art = Knopfart.Leise,
                                kompakt = true,
                                aktiv = !sendet,
                            )
                        }
                    }
                }
            }
        } else {
            // --------------------------------------------------- Einstellen
            val drin = m.eingestellt.map { it.fahrzeugId }.toSet()
            val einstellbar = stand.betrieb?.fahrzeuge.orEmpty().filter { it.lage == "Wache" && !it.geliehen && it.id !in drin }
            val deckel = fahrzeug?.let { m.hoechstpreise[it] } ?: 0
            LaunchedEffect(deckel) {
                if (deckel > 0 && preis > deckel) preis = deckel
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Breit { Gruppenkopf("Einstellen") }
                Knopf(if (einstellenOffen) "Schließen" else "Öffnen", { einstellenOffen = !einstellenOffen }, art = Knopfart.Leise, kompakt = true)
            }
            if (einstellenOffen) {
                Satz("Bleibt einsatzbereit, bis jemand mietet — erst dann fährt es los.")
                if (einstellbar.isEmpty()) {
                    Satz("Eingestellt werden kann nur, was gerade auf seiner Wache steht.")
                } else {
                    Auswahlfeld(
                        platzhalter = "Wähle ein Fahrzeug",
                        wahlen = einstellbar.map { Wahl(it.id, "${it.funkrufname} · ${it.typ}") },
                        gewaehlt = fahrzeug,
                        beiWahl = { fahrzeug = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Schieber("Für ${tagewort(tage)}", null, tage, 1, 7, 1, true) { tage = it }
                    Schieber(
                        "Preis",
                        "${zahl(preis)} Credits" + if (deckel > 0) " · höchstens ${zahl(deckel)}" else "",
                        preis,
                        0,
                        maxOf(deckel, 1),
                        preisschritt(deckel),
                        fahrzeug != null,
                    ) { preis = it }
                    Knopf(
                        if (sendet) "Wird eingestellt …" else "In den Markt stellen",
                        {
                            val f = fahrzeug
                            if (f != null) {
                                val t = tage
                                val p = preis
                                tun("Das Einstellen ging nicht.", danach = { fahrzeug = null }) { welt.wege.leiheAnbieten(it, f, t, p) }
                            }
                        },
                        art = Knopfart.Haupt,
                        aktiv = !sendet && fahrzeug != null,
                    )
                }
            }

            // --------------------------------------------------- Gesucht
            if (m.gesuche.isNotEmpty()) {
                Gruppenkopf("Gesucht", m.gesuche.size)
                Satz("Das brauchen andere — hinschicken, Preis kassieren, kommt allein zurück.")
                m.gesuche.forEach { g ->
                    val passende = passendeFahrzeuge(g)
                    Zeilenkarte {
                        Rufname(g.kategorie)
                        Klein(angebotstext(g.tage, g.preis, g.entfernungKm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                            Satz("${g.wache} ·")
                            Gegenueber(g.von, g.vonBenutzername, "eine Leitstelle", griffe)
                        }
                        if (passende.size > 1) {
                            Auswahlfeld(
                                platzhalter = "Fahrzeug wählen",
                                wahlen = passende.map { Wahl(it.id, "${it.funkrufname} · ${it.typ}") },
                                gewaehlt = bedienWahl[g.id] ?: passende.first().id,
                                beiWahl = { bedienWahl = bedienWahl + (g.id to it) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else if (passende.isEmpty()) {
                            Text(text = "Dein passendes Fahrzeug ist gerade unterwegs.", style = Schrift.Winzig, color = Farben.SignalHell)
                        }
                        Knopf(
                            "Hinschicken",
                            {
                                val gewaehlt = bedienWahl[g.id] ?: passende.firstOrNull()?.id
                                if (gewaehlt == null) {
                                    fehler = "Gerade steht kein passendes Fahrzeug auf einer deiner Wachen."
                                } else {
                                    tun("Das Hinschicken ging nicht.", Welt.Nachladen.Betrieb) { welt.wege.gesuchBedienen(it, g.id, gewaehlt) }
                                }
                            },
                            kompakt = true,
                            aktiv = !sendet && passende.isNotEmpty(),
                        )
                    }
                }
            }

            // --------------------------------------------------- Im Markt
            if (m.eingestellt.isNotEmpty()) {
                Gruppenkopf("Im Markt", m.eingestellt.size)
                m.eingestellt.forEach { a ->
                    Zeilenkarte {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                                Rufname(a.funkrufname)
                                Klein("${a.typ} · ${angebotstext(a.tage, a.preis, a.entfernungKm)}")
                                if (!a.verfuegbar) {
                                    Text(text = "Gerade unterwegs — solange greift niemand darauf zu.", style = Schrift.Winzig, color = Farben.Amber)
                                }
                            }
                            Knopf(
                                "Zurückziehen",
                                { tun("Das ging nicht.") { welt.wege.leiheZuruecknehmen(it, a.id) } },
                                art = Knopfart.Leise,
                                kompakt = true,
                                aktiv = !sendet,
                            )
                        }
                    }
                }
            }

            // --------------------------------------------------- Verliehen
            if (m.verliehen.isNotEmpty()) {
                Gruppenkopf("Verliehen", m.verliehen.size)
                m.verliehen.forEach { f ->
                    Zeilenkarte {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                                Rufname(f.funkrufname)
                                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                                    Klein("${f.typ} · bei")
                                    Gegenueber(f.gegenueber, f.gegenueberBenutzername, "einer Leitstelle", griffe)
                                }
                                Klein(f.wache)
                            }
                            Text(text = restText(f.bis), style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextLeise)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Bausteine

@Composable
private fun Zeilenkarte(inhalt: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
        content = inhalt,
    )
}

/** Eine Zeile, die den Rest der Breite nimmt — für Überschriften neben einem Knopf. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.Breit(inhalt: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) { inhalt() }
}

@Composable
private fun Rufname(text: String) {
    Text(text = text, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
}

@Composable
private fun Klein(text: String) {
    Text(text = text, style = Schrift.Klein, color = Farben.TextLeise)
}

@Composable
private fun Satz(text: String) {
    Text(text = text, style = Schrift.Winzig, color = Farben.TextSehrLeise)
}

/** Der Name des Gegenübers — antippbar, wenn es ein Profil gibt. */
@Composable
private fun Gegenueber(name: String?, benutzername: String?, sonst: String, griffe: Weltgriffe) {
    if (name == null) {
        Klein(sonst)
        return
    }
    Text(
        text = name,
        style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
        color = if (benutzername != null) Farben.BlauHell else Farben.TextLeise,
        modifier = if (benutzername != null) Modifier.clickable { griffe.profil(benutzername) } else Modifier,
    )
}

/** Ein ganzzahliger Regler mit Aufschrift und Wert. */
@Composable
private fun Schieber(
    titel: String,
    wert: String?,
    zahl: Int,
    von: Int,
    bis: Int,
    schritt: Int,
    aktiv: Boolean,
    beiWert: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = titel, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
            if (wert != null) Text(text = wert, style = Schrift.MonoKlein, color = Farben.TextLeise)
        }
        // Raster nur, wo es aufgeht — sonst gleitet der Regler, und gerundet wird beim Wert.
        val schritte = if (bis > von && (bis - von) % schritt == 0) ((bis - von) / schritt - 1).coerceAtLeast(0) else 0
        Slider(
            value = zahl.toFloat().coerceIn(von.toFloat(), bis.toFloat()),
            onValueChange = { roh ->
                val gerastert = von + (((roh - von) / schritt).roundToInt() * schritt)
                beiWert(gerastert.coerceIn(von, bis))
            },
            valueRange = von.toFloat()..maxOf(bis, von + 1).toFloat(),
            steps = schritte,
            enabled = aktiv,
            colors = SliderDefaults.colors(
                thumbColor = Farben.Amber,
                activeTrackColor = Farben.Amber,
                inactiveTrackColor = Farben.Rand,
            ),
        )
    }
}
