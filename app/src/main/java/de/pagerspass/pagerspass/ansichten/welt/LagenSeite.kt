package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import de.pagerspass.pagerspass.mobil.Weltfahrt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.WeltAuswahl
import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltLage
import de.pagerspass.pagerspass.netz.WeltZug
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Ein Sekundentakt für Restzeiten — gelesen, damit die Seite sich neu zeichnet. */
@Composable
fun sekundentakt(): Int {
    var takt by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            takt++
        }
    }
    return takt
}

/**
 * Der Bereichsreiter — „Alle“ und je Ausrückebereich einer (`Bereichsreiter.vue`).
 * Ohne Zweigstelle zeichnet er nichts: Eine Reiterreihe mit einem Reiter ist keine.
 */
@Composable
fun Bereichsreiter(welt: Welt, zustand: Weltzustand) {
    if (!zustand.mehrereBereiche) return
    Umbruchreihe {
        Pille("Alle", zustand.gewaehlterBereich == null, { welt.bereichWaehlen(null) })
        zustand.bereiche.forEach { b ->
            Pille(
                (if (b.istZweigstelle) "◇ " else "★ ") + b.name,
                zustand.gewaehlterBereich == b.name,
                { welt.bereichWaehlen(b.name) },
            )
        }
    }
}

private enum class Sieb(val wort: String) { Offen("Offen"), Nachforderung("Nachf."), Arbeit("Läuft"), Alle("Alle") }

/**
 * Die Lagen — das Gegenstück zu `LagenBlende.vue`.
 *
 * <b>Zwei Gruppen und nicht eine Liste mit Aufkleber.</b> „Deine“ (nur du siehst
 * sie, bis du sie freigibst) und „Freigegeben“ (wer zuerst da ist) sind zwei
 * verschiedene Fragen. Darüber das Sieb: Es sagt, in welchem Zustand eine Lage
 * ist, die Gruppen sagen, wem sie gehört.
 *
 * <b>Aufklappen holt die Fahrzeugauswahl</b> samt Token vom Server — ohne sie
 * wird der Alarm abgelehnt (seit 5.0.0.32). Wer sofort fährt und wer erst nach
 * seiner Arbeit, sagt der Server, nicht diese Seite.
 */
@Composable
fun LagenSeite(welt: Welt, zustand: Weltzustand, werkbank: Werkbank, karte: Weltkartenstand) {
    val takt = sekundentakt()
    val bereich = rememberCoroutineScope()
    var gewaehlt by remember { mutableStateOf<String?>(zustand.auswahlLage) }
    var sieb by remember { mutableStateOf(Sieb.Alle) }

    // Von der Karte angetippt: aufklappen und die Auswahl holen.
    LaunchedEffect(zustand.gewaehlteLage) {
        val id = zustand.gewaehlteLage ?: return@LaunchedEffect
        gewaehlt = id
        welt.lageWaehlen(null)
        if (zustand.auswahlLage != id) welt.auswahlLaden(id)
        welt.zuegeLaden()
    }

    val lagen = zustand.lagenImBereich
    fun eigeneDran(l: WeltLage) = zustand.fahrzeuge.any { it.lageId == l.id || it.naechsteLageId == l.id }
    fun gesiebt(s: Sieb) = when (s) {
        Sieb.Offen -> lagen.filter { it.zustand != "Erledigt" && !eigeneDran(it) }
        Sieb.Nachforderung -> lagen.filter { it.nachgefordert }
        Sieb.Arbeit -> lagen.filter { eigeneDran(it) || it.zustand == "Erledigt" }
        Sieb.Alle -> lagen
    }

    Bereichsreiter(welt, zustand)

    zustand.events.filter { it.zustand == "Laeuft" || (it.zustand == "Angekuendigt" && it.raumOffen) }.forEach { e ->
        Eventkasten(welt, zustand, e)
    }

    if (lagen.isEmpty()) {
        Leisesatz(
            zustand.gewaehlterBereich?.let {
                "In $it ist gerade nichts los. Neue Lagen entstehen rund um deine Wachen dort."
            } ?: "Gerade ist nichts los. Neue Lagen entstehen rund um deine Wachen.",
        )
        return
    }

    Umbruchreihe {
        Sieb.entries.forEach { s -> Pille(s.wort, sieb == s, { sieb = s }, zahl = gesiebt(s).size) }
    }

    val liste = gesiebt(sieb)
    val offen = liste.filter { it.zustand != "Erledigt" }
    val gruppen = buildList {
        add(Triple("Deine Lagen", "Gerade liegt nichts bei dir. Nur du siehst sie.", offen.filter { it.zustaendig && !it.freigegeben }))
        add(
            Triple(
                "Freigegeben",
                "Niemand hat gerade etwas freigegeben. Wer zuerst da ist, bekommt den Zuschlag.",
                offen.filter { !it.zustaendig || it.freigegeben },
            ),
        )
        val arbeit = liste.filter { it.zustand == "Erledigt" }
        if (arbeit.isNotEmpty()) add(Triple("Wird abgearbeitet", "", arbeit))
    }

    gruppen.forEach { (titel, leer, eintraege) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Ueberschrift(titel)
            if (eintraege.isNotEmpty()) Weltmarke("${eintraege.size}")
        }
        if (eintraege.isEmpty()) Leisesatz(leer, winzig = true)
        eintraege.forEach { lage ->
            Lagenzeile(
                welt = welt,
                zustand = zustand,
                lage = lage,
                offen = gewaehlt == lage.id,
                takt = takt,
                beiDruck = {
                    val neu = if (gewaehlt == lage.id) null else lage.id
                    gewaehlt = neu
                    karte.hinschauen(lage.lat, lage.lon)
                    if (neu != null) {
                        werkbank.gewaehltesFahrzeug = null
                        welt.auswahlSchliessen()
                        bereich.launch {
                            welt.auswahlLaden(neu)
                            welt.zuegeLaden()
                        }
                    } else {
                        welt.auswahlSchliessen()
                    }
                },
                beiErledigt = { gewaehlt = null },
            )
        }
    }
}

/** Die Restzeit bis zum Verfall, oder was an einer abgearbeiteten Lage zählt. */
private fun restText(fahrt: Weltfahrt, lage: WeltLage): String {
    val ms = fahrt.restMs(lage.verfaelltUm) ?: return ""
    if (ms <= 0) return "abgelaufen"
    val minuten = (ms / 60_000).toInt()
    return if (minuten >= 60) "${minuten / 60} h ${minuten % 60} min" else "$minuten min"
}

private fun arbeitRest(fahrt: Weltfahrt, bis: String?): String? {
    val ms = fahrt.restMs(bis) ?: return null
    if (ms <= 0) return "rückt gleich ein"
    val min = Math.ceil(ms / 60_000.0).toInt()
    return if (min <= 1) "noch unter einer Minute" else "noch etwa $min Minuten"
}

@Composable
private fun Lagenzeile(
    welt: Welt,
    zustand: Weltzustand,
    lage: WeltLage,
    offen: Boolean,
    takt: Int,
    beiDruck: () -> Unit,
    beiErledigt: () -> Unit,
) {
    @Suppress("UNUSED_EXPRESSION") takt
    val fahrt = zustand.fahrt
    val farbe = prioFarbe(lage.prioritaet)
    val meine = lage.zustaendig && !lage.freigegeben
    val erledigt = lage.zustand == "Erledigt"
    val ereignis = lage.eventId?.let { id -> zustand.events.firstOrNull { it.id == id } }
    val fristMs = fahrt.restMs(lage.verfaelltUm) ?: Long.MAX_VALUE

    Weltkasten(an = offen, randfarbe = if (meine) Farben.AmberTief else null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().clickable(onClick = beiDruck),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .background(if (lage.ausGrosslage) Farben.Signal else farbe.copy(alpha = 0.18f), Rundung.Winzig),
            ) {
                Text("${lage.prioritaet}", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = if (lage.ausGrosslage) Farben.AufFarbe else farbe)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    (if (lage.eigene) "● " else "") + lage.stichwort,
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = if (erledigt) Farben.TextLeise else Farben.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (ereignis != null || lage.istTageslage || lage.nachgefordert) {
                    Umbruchreihe {
                        ereignis?.let { Weltmarke(it.titel, it.farbe?.let(::farbeAus) ?: Farben.ViolettHell) }
                        if (lage.istTageslage) Weltmarke("Täglich", Farben.AmberHell)
                        if (lage.nachgefordert) Weltmarke("Nachforderung", Farben.SignalHell)
                    }
                }
                Text(
                    "${lage.adresse} · ${entfernungText(lage.entfernungMeter)}",
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                    color = Farben.TextSehrLeise,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(deckungstext(lage), style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = if (gedeckt(lage)) Farben.GruenHell else Farben.Text)
                Text("${zahl(lage.verguetung)} $WAEHRUNG", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.Amber)
                Text(
                    if (erledigt) arbeitRest(fahrt, lage.arbeitBis) ?: "fertig" else restText(fahrt, lage),
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                    color = when {
                        erledigt -> Farben.TextSehrLeise
                        fristMs <= 10 * 60_000 -> Farben.SignalHell
                        fristMs <= 25 * 60_000 -> Farben.Amber
                        else -> Farben.TextSehrLeise
                    },
                )
            }
        }
        if (offen) Lagenauswahl(welt, zustand, lage, beiErledigt)
    }
}

/** Was ein Fahrzeug von dem mitbringt, was die Lage fordert. */
private fun bringt(zustand: Weltzustand, f: WeltFahrzeug, gefordert: List<String>): List<String> {
    if (gefordert.isEmpty()) return emptyList()
    val kann = zustand.vorlage(f.vorlageId)?.faehigkeiten.orEmpty()
    return gefordert.filter { it in kann || it in f.gelernt }
}

@Composable
private fun Lagenauswahl(welt: Welt, zustand: Weltzustand, lage: WeltLage, beiErledigt: () -> Unit) {
    val bereich = rememberCoroutineScope()
    val fahrt = zustand.fahrt
    var angehakt by remember(lage.id) { mutableStateOf(setOf<String>()) }
    var suche by remember(lage.id) { mutableStateOf("") }
    var fehler by remember(lage.id) { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var entlaesst by remember { mutableStateOf(false) }
    var bricht by remember { mutableStateOf<String?>(null) }
    var freigabeGefragt by remember(lage.id) { mutableStateOf(false) }
    var nachalarmOffen by remember(lage.id) { mutableStateOf(false) }

    val auswahl: WeltAuswahl? = zustand.auswahl.takeIf { zustand.auswahlLage == lage.id }
    val gefordert = auswahl?.gefordertFaehigkeiten.orEmpty()

    fun passtZurLage(f: WeltFahrzeug): Boolean {
        val org = lage.organisation ?: return true
        if (bringt(zustand, f, gefordert).isNotEmpty()) return true
        return zustand.vorlage(f.vorlageId)?.organisation?.let { it == org } ?: true
    }
    fun ausFremderLeitstelle(f: WeltFahrzeug): Boolean {
        if (lage.bereich.isBlank()) return false
        val b = zustand.bereichVonFahrzeug(f)
        return b != null && b != lage.bereich
    }
    fun entfernung(f: WeltFahrzeug): Double {
        val vormerk = zustand.istVormerkbar(f)
        val ziel = f.zielLat != 0.0 || f.zielLon != 0.0
        val (la, lo) = if (vormerk && ziel) f.zielLat to f.zielLon else f.lat to f.lon
        return Weltfahrt.luftlinie(la, lo, lage.lat, lage.lon)
    }
    val eignung = compareBy<WeltFahrzeug> { ausFremderLeitstelle(it) }
        .thenByDescending { bringt(zustand, it, gefordert).size }
        .thenBy { entfernung(it) }
    val sofort = auswahl?.sofort.orEmpty().filter(::passtZurLage).sortedWith(eignung)
    val vormerk = auswahl?.vormerkbar.orEmpty().filter(::passtZurLage).sortedWith(eignung)
    fun passtZurSuche(f: WeltFahrzeug): Boolean {
        val t = suche.trim().lowercase()
        if (t.isEmpty()) return true
        if (t in f.funkrufname.lowercase() || t in f.typ.lowercase()) return true
        if (zustand.vorlage(f.vorlageId)?.faehigkeiten?.any { t in it.lowercase() } == true) return true
        if (f.gelernt.any { t in it.lowercase() }) return true
        return zustand.stand?.wachen?.firstOrNull { it.id == f.wacheId }?.name?.lowercase()?.contains(t) == true
    }

    val unterwegs = zustand.fahrzeuge.filter { it.lageId == lage.id }
    val nachalarmiert = zustand.fahrzeuge.filter { it.naechsteLageId == lage.id && it.lageId != lage.id }
    val erledigt = lage.zustand == "Erledigt"

    // Die Forderung als Prüfliste: grün, sobald die Auswahl sie mitbringt.
    if (gefordert.isNotEmpty()) {
        val gewaehlte = (auswahl?.sofort.orEmpty() + auswahl?.vormerkbar.orEmpty()).filter { it.id in angehakt }
        Umbruchreihe {
            Text("Gebraucht", style = Schrift.Winzig, color = Farben.TextSehrLeise, modifier = Modifier.padding(top = 2.dp))
            gefordert.forEach { k ->
                val da = lage.fehlt == null || gewaehlte.any { k in bringt(zustand, it, gefordert) }
                Weltmarke(k, if (da) Farben.GruenHell else Farben.TextLeise, gefuellt = da)
            }
        }
    }
    if ((lage.eingetroffen ?: 0) > 0 && lage.fehlt != null) {
        Text(lage.fehlt, style = Schrift.Klein, color = Farben.Amber)
    }

    // ---------------------------------------------------------- Beteiligte
    if (unterwegs.isNotEmpty() || nachalarmiert.isNotEmpty()) {
        Leisesatz(
            "Von dir alarmiert (${unterwegs.size})" +
                (if (nachalarmiert.isNotEmpty()) ", dazu ${nachalarmiert.size} nachalarmiert" else "") + ":",
        )
        val rang = mapOf("vorOrt" to 0, "anfahrt" to 1, "alarmiert" to 2, "nachalarm" to 3)
        (unterwegs + nachalarmiert)
            .map { f ->
                val art = when {
                    f.naechsteLageId == lage.id && f.lageId != lage.id -> "nachalarm"
                    f.lage == "VorOrt" -> "vorOrt"
                    fahrt.ruecktAus(f.lage, f.losUm) -> "alarmiert"
                    else -> "anfahrt"
                }
                f to art
            }
            .sortedWith(compareBy({ rang[it.second] }, { it.first.funkrufname }))
            .forEach { (f, art) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Weltmarke(
                        when (art) {
                            "nachalarm" -> "nachalarmiert"
                            "vorOrt" -> "vor Ort"
                            "alarmiert" -> "alarmiert"
                            else -> "Anfahrt"
                        },
                        when (art) {
                            "vorOrt" -> Farben.GruenHell
                            "nachalarm" -> Farben.TextSehrLeise
                            else -> Farben.Amber
                        },
                    )
                    val rest = when (art) {
                        "vorOrt" -> ""
                        "nachalarm" -> if (f.lage == "VorOrt") arbeitRestKurz(fahrt, f.ankunftUm) else "anderer Einsatz"
                        else -> fahrt.restMinuten(f.ankunftUm)?.let { "noch $it min" }.orEmpty()
                    }
                    if (rest.isNotEmpty()) Text(rest, style = Schrift.Winzig, color = Farben.TextSehrLeise)
                    if (art == "anfahrt" || art == "alarmiert") {
                        Knopf(
                            if (bricht == f.id) "Dreht um …" else "Abbrechen",
                            {
                                bricht = f.id
                                bereich.launch {
                                    fehler = welt.anfahrtAbbrechen(f.id)
                                    bricht = null
                                }
                            },
                            kompakt = true,
                            art = Knopfart.Leise,
                            aktiv = bricht == null && !entlaesst,
                        )
                    }
                }
            }
        val n = unterwegs.size + nachalarmiert.size
        Knopf(
            if (entlaesst) "Rückt ein …" else if (n == 1) "Fahrzeug entlassen" else "$n Fahrzeuge entlassen",
            {
                entlaesst = true
                bereich.launch {
                    fehler = welt.ausLageEntlassen(lage.id)
                    entlaesst = false
                }
            },
            kompakt = true,
            aktiv = !entlaesst && bricht == null,
        )
    }

    if (!erledigt && gedeckt(lage)) {
        arbeitRest(fahrt, lage.arbeitBis)?.let {
            Leisesatz("Gedeckt — wird abgearbeitet ($it). Danach rücken die Fahrzeuge ein, und die Vergütung wird gutgeschrieben.")
        }
    }

    if (erledigt) {
        Leisesatz(
            arbeitRest(fahrt, lage.arbeitBis)?.let {
                "Abgerechnet und bezahlt. Ein nachgerücktes Fahrzeug arbeitet noch ($it), dann rückt es ein."
            } ?: "Abgerechnet und bezahlt — deine Fahrzeuge rücken ein.",
        )
        Warnsatz(fehler)
        return
    }

    // ------------------------------------------------------------- Auswahl
    when {
        zustand.auswahlLaedt && auswahl == null -> Leisesatz("Die Fahrzeugauswahl wird geholt …")
        auswahl == null -> Knopf("Fahrzeugauswahl öffnen", {
            bereich.launch { fehler = welt.auswahlLaden(lage.id) }
        }, kompakt = true)
        auswahl.sofort.isEmpty() && auswahl.vormerkbar.isEmpty() -> Warnsatz("Kein Fahrzeug einsatzbereit.")
        sofort.isEmpty() && vormerk.isEmpty() -> Warnsatz("Keines deiner einsatzbereiten Fahrzeuge passt zu dieser Lage.")
        else -> {
            if (sofort.size + vormerk.size >= 8) {
                Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Rufname, Typ, Fähigkeit …")
            }
            val sichtbar = sofort.filter(::passtZurSuche)
            val sichtbarVormerk = vormerk.filter(::passtZurSuche)
            val bedarf = lage.eingetroffen?.let { da ->
                maxOf(0, lage.gefordertFahrzeuge - da - unterwegs.count { it.lage != "VorOrt" })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    when (bedarf) {
                        null -> ""
                        0 -> "Bedarf gedeckt"
                        1 -> "Noch ein Fahrzeug"
                        else -> "Noch $bedarf Fahrzeuge"
                    },
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                    modifier = Modifier.weight(1f),
                )
                if (sichtbar.isNotEmpty()) {
                    val alle = sichtbar.all { it.id in angehakt }
                    Knopf(
                        if (alle) "Keine" else "Alle ${sichtbar.size}",
                        { angehakt = if (alle) angehakt - sichtbar.map { it.id }.toSet() else angehakt + sichtbar.map { it.id } },
                        kompakt = true,
                        art = Knopfart.Leise,
                    )
                }
            }

            // Die Züge — ein Griff statt fünf. Die Zahl sagt, wie viele des Zuges in Frage kommen.
            val erreichbar = (sofort + vormerk).map { it.id }.toSet()
            val zuege = (auswahl.zuege.ifEmpty { zustand.zuege })
                .map { it to it.fahrzeugIds.count { id -> id in erreichbar } }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .take(5)
            if (zuege.isNotEmpty()) {
                Umbruchreihe {
                    zuege.forEach { (zug: WeltZug, treffer) ->
                        Knopf("${zug.name} ($treffer)", {
                            sendet = true
                            bereich.launch {
                                fehler = welt.zugAlarmieren(lage.id, zug.id)
                                sendet = false
                                if (fehler == null) beiErledigt()
                            }
                        }, kompakt = true, aktiv = !sendet)
                    }
                }
            }

            if (sofort.isEmpty()) Leisesatz("Gerade ist nichts sofort frei — unter „Nachalarmieren“ lässt sich vormerken.", winzig = true)
            sichtbar.forEach { f ->
                Wagenzeile(zustand, f, lage, gefordert, f.id in angehakt, vormerken = false) {
                    angehakt = if (f.id in angehakt) angehakt - f.id else angehakt + f.id
                }
            }
            val vonSucheWeg = sofort.size + vormerk.size - sichtbar.size - sichtbarVormerk.size
            if (vonSucheWeg > 0) Leisesatz("$vonSucheWeg weitere passen nicht zur Suche.", winzig = true)
            val verborgen = auswahl.sofort.size - sofort.size
            if (verborgen > 0) Leisesatz("$verborgen einsatzbereite Fahrzeuge anderer Organisationen sind ausgeblendet.", winzig = true)

            if (vormerk.isNotEmpty()) {
                Knopf(
                    (if (nachalarmOffen) "▾ " else "▸ ") + "Nachalarmieren (${vormerk.size})",
                    { nachalarmOffen = !nachalarmOffen },
                    kompakt = true,
                    art = Knopfart.Leise,
                )
                if (nachalarmOffen) {
                    Leisesatz("Diese arbeiten noch an anderen Stellen und fahren danach hierher.", winzig = true)
                    sichtbarVormerk.forEach { f ->
                        Wagenzeile(zustand, f, lage, gefordert, f.id in angehakt, vormerken = true) {
                            angehakt = if (f.id in angehakt) angehakt - f.id else angehakt + f.id
                        }
                    }
                }
            }

            val nSofort = auswahl.sofort.count { it.id in angehakt }
            val nSpaeter = auswahl.vormerkbar.count { it.id in angehakt }
            val wort = if (unterwegs.isNotEmpty()) "Nachalarmieren" else "Alarmieren"
            val aufschrift = when {
                sendet -> "Wird alarmiert …"
                nSpaeter == 0 -> "$wort ($nSofort)"
                nSofort == 0 -> "Vormerken ($nSpaeter)"
                else -> "$wort ($nSofort) + vormerken ($nSpaeter)"
            }
            Knopf(
                aufschrift,
                {
                    if (angehakt.isEmpty()) {
                        fehler = "Wähle mindestens ein Fahrzeug."
                        return@Knopf
                    }
                    sendet = true
                    bereich.launch {
                        fehler = welt.alarmieren(lage.id, angehakt.toList())
                        sendet = false
                        if (fehler == null) {
                            angehakt = emptySet()
                            beiErledigt()
                        }
                    }
                },
                art = Knopfart.Alarm,
                breit = true,
                aktiv = !sendet && angehakt.isNotEmpty(),
            )
        }
    }

    val werkstatt = zustand.fahrzeuge.count { it.verschlissen || it.inWerkstatt }
    if (werkstatt > 0) {
        Leisesatz(
            "$werkstatt ${if (werkstatt == 1) "Fahrzeug steht" else "Fahrzeuge stehen"} nicht zur Wahl — verschlissen oder in der Werkstatt.",
            winzig = true,
        )
    }

    // Die Freigabe: Die Lage liegt bis dahin allein bei mir. Endgültig.
    if (lage.zustaendig && !lage.freigegeben) {
        if (freigabeGefragt) {
            Rueckfrage(
                frage = "Für alle freigeben? Dann darf jede Leitstelle hinfahren — zurücknehmen lässt es sich nicht.",
                ja = "Freigeben",
                beiJa = {
                    sendet = true
                    bereich.launch {
                        fehler = welt.freigeben(lage.id)
                        sendet = false
                        if (fehler == null) freigabeGefragt = false
                    }
                },
                beiNein = { freigabeGefragt = false },
                aktiv = !sendet,
            )
        } else {
            Knopf("Für alle freigeben", { freigabeGefragt = true }, kompakt = true, art = Knopfart.Leise)
        }
    }
    Warnsatz(fehler)
}

private fun arbeitRestKurz(fahrt: Weltfahrt, bis: String?): String {
    val ms = fahrt.restMs(bis) ?: return "an anderer Lage"
    if (ms <= 0) return "gleich fertig"
    val min = Math.ceil(ms / 60_000.0).toInt()
    return if (min <= 1) "unter 1 min" else "noch $min min"
}

/** Eine Fahrzeugzeile der Auswahl: FMS vorn, Name und Typ, darunter Entfernung und Marken. */
@Composable
private fun Wagenzeile(
    zustand: Weltzustand,
    f: WeltFahrzeug,
    lage: WeltLage,
    gefordert: List<String>,
    an: Boolean,
    vormerken: Boolean,
    beiWechsel: () -> Unit,
) {
    val fahrt = zustand.fahrt
    val bringt = bringt(zustand, f, gefordert)
    val vormerk = zustand.istVormerkbar(f)
    val ziel = f.zielLat != 0.0 || f.zielLon != 0.0
    val (la, lo) = if (vormerk && ziel) f.zielLat to f.zielLon else f.lat to f.lon
    val weite = Weltfahrt.luftlinie(la, lo, lage.lat, lage.lon)
    val fremd = lage.bereich.isNotBlank() && zustand.bereichVonFahrzeug(f)?.let { it != lage.bereich } == true
    Wahlzeile(an = an, beiWechsel = beiWechsel) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Fmsplakette(f.status)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Text(f.typ, style = Schrift.Winzig, color = Farben.TextLeise, maxLines = 1)
                }
                Umbruchreihe {
                    Text(entfernungText(weite), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                    if (vormerken) {
                        Text(
                            if (f.lage == "VorOrt") "arbeitet noch — fährt danach hierher"
                            else "${if (fahrt.ruecktAus(f.lage, f.losUm)) "rückt aus" else "auf Anfahrt"} zu anderem Einsatz — fährt danach hierher",
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                        )
                    }
                    if (f.lage == "Rueckfahrt") Weltmarke("rückt ein", Farben.TextSehrLeise)
                    if (f.lage == "Bereitstellung") Weltmarke("aus dem Bereitstellungsraum", Farben.Violett)
                    if (fremd) Weltmarke("anderer Bereich", Farben.TextSehrLeise)
                    if (f.geliehen) Weltmarke("geliehen", Farben.BlauHell)
                    bringt.forEach { Weltmarke(it, Farben.GruenHell) }
                    if (f.werkstattFaellig) Weltmarke("Werkstatt bald", Farben.Amber)
                }
            }
        }
    }
}
