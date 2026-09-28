package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.mobil.weltzeit
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.WeltAuswahl
import de.pagerspass.pagerspass.netz.WeltEvent
import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltLage
import de.pagerspass.pagerspass.netz.WeltZug
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.schmuck.bildVon
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

/** Das Sieb über der Lagenliste — vier Wege, eine Zeile. */
private enum class Lagensieb(val wort: String) { Offen("Offen"), Nachforderung("Nachf."), Arbeit("Läuft"), Alle("Alle") }

/** Ab so vielen Fahrzeugen steht ein Suchfeld über der Auswahl. */
private const val SUCHE_AB = 8

/**
 * Die Lagen — übertragen aus `components/welt/LagenBlende.vue`.
 *
 * <b>Zwei Gruppen und nicht eine Liste mit Aufkleber:</b> „Deine Lagen" (nur du siehst sie)
 * und „Freigegeben" (wer zuerst da ist, bekommt den Zuschlag) sind zwei verschiedene
 * Fragen. Das Sieb darüber sagt, in welchem Zustand eine Lage ist — zwei Achsen, und beide
 * braucht man.
 *
 * <b>Aufgeklappt holt eine Lage ihre Fahrzeugauswahl</b> (`auswahlLaden`): Der Server sagt,
 * was sie fordert, wer in Frage kommt, und gibt das Token, das der Alarm zurückverlangt.
 */
@Composable
fun WeltLagenBlende(welt: Welt, stand: Weltzustand, katalog: Katalog?, griffe: Weltgriffe) {
    val bereich = rememberCoroutineScope()
    val vorlagen = remember(katalog) { katalog?.fahrzeuge.orEmpty().associateBy { it.id } }

    var gewaehlt by remember { mutableStateOf<String?>(null) }
    var angehakt by remember { mutableStateOf<Set<String>>(emptySet()) }
    var suche by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var sieb by remember { mutableStateOf(Lagensieb.Alle) }
    var freigabeGefragt by remember { mutableStateOf<String?>(null) }
    var entlaesst by remember { mutableStateOf<String?>(null) }
    var bricht by remember { mutableStateOf<String?>(null) }
    var entlassfehler by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var nachalarmOffen by remember { mutableStateOf(false) }

    // Ein Takt je Sekunde für die Restzeiten.
    val jetzt by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    val serverJetzt = jetzt + welt.fahrt.versatzMs

    // Die auf der Karte angeklickte Lage: markieren, aufklappen, Auswahl holen.
    LaunchedEffect(stand.gewaehlteLage) {
        val id = stand.gewaehlteLage ?: return@LaunchedEffect
        gewaehlt = id
        angehakt = emptySet()
        suche = ""
        fehler = null
        welt.lageWaehlen(null)
        if (stand.auswahlLage != id) bereich.launch { welt.auswahlLaden(id) }
        bereich.launch { welt.zuegeLaden() }
    }

    fun umschalten(lage: WeltLage) {
        gewaehlt = if (gewaehlt == lage.id) null else lage.id
        angehakt = emptySet()
        suche = ""
        fehler = null
        freigabeGefragt = null
        nachalarmOffen = false
        griffe.hinschauen(lage.lat, lage.lon)

        // Erst das Öffnen holt die Auswahl — und mit ihr das Token.
        if (gewaehlt != null) {
            bereich.launch { welt.auswahlLaden(lage.id) }
            bereich.launch { welt.zuegeLaden() }
        } else {
            welt.auswahlSchliessen()
        }
    }

    fun auswahlVon(lage: WeltLage): WeltAuswahl? = if (stand.auswahlLage == lage.id) stand.auswahl else null

    fun gefordert(lage: WeltLage): List<String> = auswahlVon(lage)?.gefordertFaehigkeiten.orEmpty()

    fun bringt(f: WeltFahrzeug, lage: WeltLage): List<String> {
        val forderung = gefordert(lage)
        if (forderung.isEmpty()) return emptyList()
        val vorlage = vorlagen[f.vorlageId]
        return forderung.filter { k -> vorlage?.faehigkeiten?.contains(k) == true || f.gelernt.contains(k) }
    }

    fun unterwegs(lage: WeltLage) = stand.betrieb?.fahrzeuge.orEmpty().filter { it.lageId == lage.id }
    fun nachalarmiert(lage: WeltLage) = stand.betrieb?.fahrzeuge.orEmpty().filter { it.naechsteLageId == lage.id }

    fun standart(f: WeltFahrzeug, lage: WeltLage): String = when {
        f.naechsteLageId == lage.id && f.lageId != lage.id -> "nachalarm"
        f.lage == "VorOrt" -> "vorOrt"
        welt.fahrt.ruecktAus(f.lage, f.losUm) -> "alarmiert"
        else -> "anfahrt"
    }

    fun beteiligte(lage: WeltLage): List<Pair<WeltFahrzeug, String>> {
        val rang = mapOf("vorOrt" to 0, "anfahrt" to 1, "alarmiert" to 2, "nachalarm" to 3)
        return (unterwegs(lage) + nachalarmiert(lage).filter { it.lageId != lage.id })
            .map { it to standart(it, lage) }
            .sortedWith(compareBy<Pair<WeltFahrzeug, String>> { rang[it.second] ?: 9 }.thenBy { it.first.funkrufname })
    }

    fun eigeneDran(lage: WeltLage) = beteiligte(lage).isNotEmpty()

    fun durchsSieb(liste: List<WeltLage>, s: Lagensieb): List<WeltLage> = when (s) {
        Lagensieb.Offen -> liste.filter { it.zustand != "Erledigt" && !eigeneDran(it) }
        Lagensieb.Nachforderung -> liste.filter { it.nachgefordert }
        Lagensieb.Arbeit -> liste.filter { eigeneDran(it) || it.zustand == "Erledigt" }
        Lagensieb.Alle -> liste
    }

    fun passtZurLage(f: WeltFahrzeug, lage: WeltLage): Boolean {
        val org = lage.organisation ?: return true
        if (bringt(f, lage).isNotEmpty()) return true
        val vorlage = vorlagen[f.vorlageId] ?: return true
        return vorlage.organisation == org
    }

    fun ausFremderLeitstelle(f: WeltFahrzeug, lage: WeltLage): Boolean {
        if (lage.bereich.isBlank()) return false
        val b = stand.bereichVonFahrzeug(f)
        return b != null && b != lage.bereich
    }

    fun entfernungZu(f: WeltFahrzeug, lage: WeltLage): Double {
        // 0/0 ist kein Ort, sondern ein ungesetztes Ziel.
        val zielGesetzt = f.zielLat != 0.0 || f.zielLon != 0.0
        val vonZiel = stand.istVormerkbar(f) && zielGesetzt
        val lat = if (vonZiel) f.zielLat else f.lat
        val lon = if (vonZiel) f.zielLon else f.lon
        return luftlinieMeter(lat, lon, lage.lat, lage.lon)
    }

    fun nachEignung(lage: WeltLage): Comparator<WeltFahrzeug> =
        compareBy<WeltFahrzeug> { if (ausFremderLeitstelle(it, lage)) 1 else 0 }
            .thenByDescending { bringt(it, lage).size }
            .thenBy { entfernungZu(it, lage) }

    fun wahlFuer(lage: WeltLage) = auswahlVon(lage)?.sofort.orEmpty().filter { passtZurLage(it, lage) }.sortedWith(nachEignung(lage))
    fun nachalarmFuer(lage: WeltLage) = auswahlVon(lage)?.vormerkbar.orEmpty().filter { passtZurLage(it, lage) }.sortedWith(nachEignung(lage))

    fun passtZurSuche(f: WeltFahrzeug): Boolean {
        val t = suche.trim().lowercase()
        if (t.isEmpty()) return true
        if (f.funkrufname.lowercase().contains(t) || f.typ.lowercase().contains(t)) return true
        if (vorlagen[f.vorlageId]?.faehigkeiten?.any { it.lowercase().contains(t) } == true) return true
        if (f.gelernt.any { it.lowercase().contains(t) }) return true
        val wache = stand.stand?.wachen?.firstOrNull { it.id == f.wacheId }
        return wache?.name?.lowercase()?.contains(t) == true
    }

    fun zuegeFuer(lage: WeltLage): List<Pair<WeltZug, Int>> {
        val erreichbar = (wahlFuer(lage) + nachalarmFuer(lage)).map { it.id }.toSet()
        return (auswahlVon(lage)?.zuege ?: stand.zuege)
            .map { z -> z to z.fahrzeugIds.count { it in erreichbar } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(5)
    }

    fun restText(lage: WeltLage): String {
        val ms = (weltzeit(lage.verfaelltUm) ?: return "") - serverJetzt
        if (ms <= 0) return "abgelaufen"
        val minuten = (ms / 60_000).toInt()
        return if (minuten >= 60) "${minuten / 60} h ${minuten % 60} min" else "$minuten min"
    }

    fun fristfarbe(lage: WeltLage): Color {
        if (lage.zustand == "Erledigt") return Farben.TextSehrLeise
        val ms = (weltzeit(lage.verfaelltUm) ?: return Farben.TextSehrLeise) - serverJetzt
        return when {
            ms <= 10 * 60_000 -> Farben.SignalHell
            ms <= 25 * 60_000 -> Farben.Amber
            else -> Farben.TextSehrLeise
        }
    }

    fun arbeitRest(lage: WeltLage): String? {
        val bis = weltzeit(lage.arbeitBis) ?: return null
        val ms = bis - serverJetzt
        if (ms <= 0) return "rückt gleich ein"
        val minuten = ceil(ms / 60_000.0).toInt()
        return if (minuten <= 1) "noch unter einer Minute" else "noch etwa $minuten Minuten"
    }

    fun arbeitRestVon(f: WeltFahrzeug): String? {
        val an = weltzeit(f.ankunftUm) ?: return null
        val ms = an - serverJetzt
        if (ms <= 0) return "gleich fertig"
        val minuten = ceil(ms / 60_000.0).toInt()
        return if (minuten <= 1) "unter 1 min" else "noch $minuten min"
    }

    fun standText(f: WeltFahrzeug): String {
        if (f.lage == "VorOrt") return "vor Ort"
        val min = welt.fahrt.restMinuten(f.ankunftUm)
        val wort = if (welt.fahrt.ruecktAus(f.lage, f.losUm)) "rückt aus" else "auf Anfahrt"
        return if (min == null) wort else "$wort · noch $min min"
    }

    fun nachalarmText(f: WeltFahrzeug): String =
        if (f.lage == "VorOrt") {
            "arbeitet noch${arbeitRestVon(f)?.let { " ($it)" }.orEmpty()} — fährt danach hierher"
        } else {
            "${standText(f)} zu anderem Einsatz — fährt danach hierher"
        }

    fun restZeit(f: WeltFahrzeug, art: String): String = when (art) {
        "vorOrt" -> ""
        "nachalarm" -> if (f.lage == "VorOrt") arbeitRestVon(f) ?: "an anderer Lage" else "anderer Einsatz"
        else -> welt.fahrt.restMinuten(f.ankunftUm)?.let { "noch $it min" }.orEmpty()
    }

    fun offenerBedarf(lage: WeltLage): Int? {
        val da = eingetroffen(lage) ?: return null
        val fahrende = unterwegs(lage).count { it.lage != "VorOrt" }
        return max(0, lage.gefordertFahrzeuge - da - fahrende)
    }

    fun alarmknopf(lage: WeltLage, nachalarm: Boolean): String {
        val auswahl = auswahlVon(lage)
        val sofort = auswahl?.sofort.orEmpty().count { it.id in angehakt }
        val spaeter = auswahl?.vormerkbar.orEmpty().count { it.id in angehakt }
        val wort = if (nachalarm) "Nachalarmieren" else "Alarmieren"
        return when {
            sofort == 0 && spaeter == 0 -> "$wort (0)"
            spaeter == 0 -> "$wort ($sofort)"
            sofort == 0 -> "Vormerken ($spaeter)"
            else -> "$wort ($sofort) + vormerken ($spaeter)"
        }
    }

    fun alarmieren(lage: WeltLage) {
        if (angehakt.isEmpty()) {
            fehler = "Wähle mindestens ein Fahrzeug."
            return
        }
        if (sendet) return
        sendet = true
        bereich.launch {
            fehler = welt.alarmieren(lage.id, angehakt.toList())
            sendet = false
            if (fehler == null) {
                gewaehlt = null
                angehakt = emptySet()
            }
        }
    }

    fun zugAlarmieren(lage: WeltLage, zug: WeltZug) {
        // Dieselbe Sperre wie beim Alarm: Ein Doppeltipp verbraucht sonst zwei Token.
        if (sendet) return
        sendet = true
        bereich.launch {
            fehler = welt.zugAlarmieren(lage.id, zug.id)
            sendet = false
            if (fehler == null) {
                gewaehlt = null
                angehakt = emptySet()
            }
        }
    }

    // ------------------------------------------------------------------ Bild

    val lagen = stand.lagenImBereich
    val laufendeEvents = stand.events.filter { it.zustand == "Laeuft" || (it.zustand == "Angekuendigt" && it.raumOffen) }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        Bereichsreiter(stand, beiWahl = { welt.bereichWaehlen(it) })

        laufendeEvents.forEach { e ->
            Eventkulisse(e) {
                Text(
                    text = "${e.ort} · " + if (e.offeneLagen == 1) "eine offene Lage" else "${e.offeneLagen} offene Lagen",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
                Eventraum(welt, stand, e)
            }
        }

        if (lagen.isEmpty()) {
            val b = stand.gewaehlterBereich
            Leerhinweis(
                if (b != null) {
                    "In $b ist gerade nichts los. Neue Lagen entstehen rund um deine Wachen dort."
                } else {
                    "Gerade ist nichts los. Neue Lagen entstehen rund um deine Wachen."
                },
            )
            return@Column
        }

        // Das Sieb — die Zahl steht am Weg, weil ein leeres Sieb sonst wie ein Ladefehler aussieht.
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth()) {
            Lagensieb.entries.forEach { s ->
                val an = sieb == s
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(if (an) Farben.HauchAmber else Color.Transparent, Rundung.Klein)
                        .border(1.dp, if (an) Farben.AmberTief else Farben.Rand, Rundung.Klein)
                        .clickable { sieb = s },
                ) {
                    Text(text = s.wort, style = Schrift.Klein, color = if (an) Farben.Amber else Farben.TextLeise, maxLines = 1)
                    Text(
                        text = durchsSieb(lagen, s).size.toString(),
                        style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG),
                        color = if (an) Farben.Amber else Farben.TextSehrLeise,
                    )
                }
            }
        }

        val gesiebt = durchsSieb(lagen, sieb)
        val offen = gesiebt.filter { it.zustand != "Erledigt" }
        val meine = offen.filter { it.zustaendig && !it.freigegeben }
        val frei = offen.filter { !it.zustaendig || it.freigegeben }
        val inArbeit = gesiebt.filter { it.zustand == "Erledigt" }

        val gruppen = buildList {
            add(Triple("Deine Lagen", "Gerade liegt nichts bei dir. Nur du siehst sie.", meine))
            add(Triple("Freigegeben", "Niemand hat gerade etwas freigegeben. Wer zuerst da ist, bekommt den Zuschlag.", frei))
            if (inArbeit.isNotEmpty()) add(Triple("Wird abgearbeitet", "", inArbeit))
        }

        gruppen.forEach { (titel, leer, eintraege) ->
            Gruppenkopf(titel, eintraege.size)
            if (eintraege.isEmpty()) {
                Text(text = leer, style = Schrift.Winzig, color = Farben.TextSehrLeise)
            }
            eintraege.forEach { lage ->
                Lagenzeile(
                    lage = lage,
                    event = lage.eventId?.let { id -> stand.events.firstOrNull { it.id == id } },
                    offen = gewaehlt == lage.id,
                    rest = if (lage.zustand == "Erledigt") arbeitRest(lage) ?: "fertig" else restText(lage),
                    restfarbe = fristfarbe(lage),
                    beiDruck = { umschalten(lage) },
                )

                if (gewaehlt != lage.id) return@forEach

                // ------------------------------------------------ aufgeklappt
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier
                        .fillMaxWidth()
                        .flaeche(farbe = Farben.Flaeche, ecke = 9.dp)
                        .padding(Abstand.Normal),
                ) {
                    val auswahl = auswahlVon(lage)
                    val angehakteFahrzeuge = (auswahl?.sofort.orEmpty() + auswahl?.vormerkbar.orEmpty()).filter { it.id in angehakt }

                    // Die Forderung als Prüfliste — grün, sobald die Auswahl sie mitbringt.
                    if (gefordert(lage).isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                        ) {
                            Text(text = "Gebraucht", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                            gefordert(lage).forEach { k ->
                                val da = lage.fehlt == null || angehakteFahrzeuge.any { bringt(it, lage).contains(k) }
                                Weltmarke(k, if (da) Farben.FmsFrei else Farben.TextLeise)
                            }
                        }
                    }

                    if ((eingetroffen(lage) ?: 0) > 0 && lage.fehlt != null) {
                        Text(text = lage.fehlt, style = Schrift.Klein, color = Farben.SignalHell)
                    }

                    // Wer schon auf dem Weg hierher ist.
                    val dran = beteiligte(lage)
                    if (dran.isNotEmpty()) {
                        val nur = nachalarmiert(lage).count { it.lageId != lage.id }
                        Text(
                            text = "Von dir alarmiert (${unterwegs(lage).size})" +
                                (if (nur > 0) ", dazu $nur nachalarmiert" else "") + ":",
                            style = Schrift.Klein,
                            color = Farben.TextLeise,
                        )
                        dran.forEach { (f, art) ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = f.funkrufname,
                                    style = Schrift.MonoKlein,
                                    color = Farben.Text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = when (art) {
                                        "nachalarm" -> "nachalarmiert"
                                        "vorOrt" -> "vor Ort"
                                        "alarmiert" -> "alarmiert"
                                        else -> "Anfahrt"
                                    },
                                    style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                                    color = when (art) {
                                        "vorOrt" -> Farben.FmsVorOrt
                                        "anfahrt" -> Farben.FmsAnfahrt
                                        "alarmiert" -> Farben.Amber
                                        else -> Farben.TextSehrLeise
                                    },
                                )
                                Text(text = restZeit(f, art), style = Schrift.Winzig, color = Farben.TextLeise)
                                // Nur, wer noch fährt oder am Tor steht, bricht eine Anfahrt ab.
                                if (art == "anfahrt" || art == "alarmiert") {
                                    Knopf(
                                        aufschrift = if (bricht == f.id) "Dreht um …" else "Abbrechen",
                                        beiDruck = {
                                            if (bricht == null && entlaesst == null) {
                                                bricht = f.id
                                                entlassfehler = entlassfehler - lage.id
                                                bereich.launch {
                                                    welt.anfahrtAbbrechen(f.id)?.let { entlassfehler = entlassfehler + (lage.id to it) }
                                                    bricht = null
                                                }
                                            }
                                        },
                                        kompakt = true,
                                        aktiv = entlaesst == null && bricht == null,
                                    )
                                }
                            }
                        }

                        // Der Weg zurück — die Aufschrift nennt die Zahl, weil der Knopf auch
                        // die Vorgemerkten anfasst.
                        Knopf(
                            aufschrift = when {
                                entlaesst == lage.id -> "Rückt ein …"
                                dran.size == 1 -> "Fahrzeug entlassen"
                                else -> "${dran.size} Fahrzeuge entlassen"
                            },
                            beiDruck = {
                                if (entlaesst == null) {
                                    entlaesst = lage.id
                                    entlassfehler = entlassfehler - lage.id
                                    bereich.launch {
                                        welt.ausLageEntlassen(lage.id)?.let { entlassfehler = entlassfehler + (lage.id to it) }
                                        entlaesst = null
                                    }
                                }
                            },
                            aktiv = entlaesst != lage.id && bricht == null,
                            kompakt = true,
                        )
                        Absage(entlassfehler[lage.id])
                    }

                    // Gedeckt, aber noch nicht fertig.
                    val gedeckt = lage.fehlt == null
                    val arbeit = arbeitRest(lage)
                    if (lage.zustand != "Erledigt" && gedeckt && arbeit != null) {
                        Text(
                            text = "Gedeckt — wird abgearbeitet ($arbeit). Danach rücken die Fahrzeuge ein, " +
                                "und die Vergütung wird gutgeschrieben.",
                            style = Schrift.Klein,
                            color = Farben.TextLeise,
                        )
                    }

                    if (lage.zustand == "Erledigt") {
                        Text(
                            text = if (arbeit != null) {
                                "Abgerechnet und bezahlt. Ein nachgerücktes Fahrzeug arbeitet noch ($arbeit), " +
                                    "dann rückt es ein."
                            } else {
                                "Abgerechnet und bezahlt — deine Fahrzeuge rücken ein."
                            },
                            style = Schrift.Klein,
                            color = Farben.TextLeise,
                        )
                        return@Column
                    }

                    val wahl = wahlFuer(lage)
                    val nachalarm = nachalarmFuer(lage)

                    when {
                        stand.auswahlLaedt && auswahl == null -> Ladezeile("Fahrzeugauswahl wird geöffnet …")
                        wahl.isEmpty() && nachalarm.isEmpty() && (auswahl?.sofort.orEmpty().isEmpty() && auswahl?.vormerkbar.orEmpty().isEmpty()) ->
                            Text(text = "Kein Fahrzeug einsatzbereit.", style = Schrift.Klein, color = Farben.SignalHell)
                        wahl.isEmpty() && nachalarm.isEmpty() ->
                            Text(
                                text = "Keines deiner einsatzbereiten Fahrzeuge passt zu dieser Lage.",
                                style = Schrift.Klein,
                                color = Farben.SignalHell,
                            )
                        else -> {
                            if (wahl.size + nachalarm.size >= SUCHE_AB) {
                                Feld(
                                    wert = suche,
                                    beiAenderung = { suche = it },
                                    platzhalter = "Rufname, Typ, Fähigkeit …",
                                    stil = Schrift.MonoKlein,
                                )
                            }
                            val wahlGefiltert = wahl.filter { passtZurSuche(it) }
                            val nachalarmGefiltert = nachalarm.filter { passtZurSuche(it) }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                val bedarf = offenerBedarf(lage)
                                Text(
                                    text = when (bedarf) {
                                        null -> ""
                                        0 -> "Bedarf gedeckt"
                                        1 -> "Noch ein Fahrzeug"
                                        else -> "Noch $bedarf Fahrzeuge"
                                    },
                                    style = Schrift.Winzig,
                                    color = Farben.TextSehrLeise,
                                    modifier = Modifier.weight(1f),
                                )
                                AlleWaehlen(wahlGefiltert.map { it.id }, angehakt) { angehakt = it }
                            }

                            // Die Züge dieser Lage — ein Griff statt fünf.
                            val zuege = zuegeFuer(lage)
                            if (zuege.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                                ) {
                                    zuege.forEach { (z, treffer) ->
                                        Knopf(
                                            aufschrift = "${z.name} ($treffer)",
                                            beiDruck = { zugAlarmieren(lage, z) },
                                            aktiv = !sendet,
                                            kompakt = true,
                                        )
                                    }
                                }
                            }

                            if (wahl.isEmpty()) {
                                Text(
                                    text = "Gerade ist nichts sofort frei — unter „Nachalarmieren\" lässt sich vormerken.",
                                    style = Schrift.Winzig,
                                    color = Farben.TextSehrLeise,
                                )
                            }

                            wahlGefiltert.forEach { f ->
                                Fahrzeugwahl(
                                    f = f,
                                    an = f.id in angehakt,
                                    entfernung = entfernungText(entfernungZu(f, lage)),
                                    fremderBereich = if (ausFremderLeitstelle(f, lage)) stand.bereichVonFahrzeug(f) else null,
                                    bringt = bringt(f, lage),
                                    zusatz = if (f.lage == "Rueckfahrt") "rückt ein" else null,
                                    beiWechsel = { angehakt = if (f.id in angehakt) angehakt - f.id else angehakt + f.id },
                                )
                            }

                            val weg = wahl.size + nachalarm.size - wahlGefiltert.size - nachalarmGefiltert.size
                            if (weg > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                                    Text(
                                        text = if (weg == 1) "Ein Fahrzeug passt nicht zur Suche." else "$weg Fahrzeuge passen nicht zur Suche.",
                                        style = Schrift.Winzig,
                                        color = Farben.TextSehrLeise,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Knopf("Suche zurücksetzen", { suche = "" }, art = Knopfart.Leise, kompakt = true)
                                }
                            }

                            val verborgen = (auswahl?.sofort?.size ?: 0) - wahl.size
                            if (verborgen > 0) {
                                Text(
                                    text = if (verborgen == 1) "Ein weiteres Fahrzeug passt hier nicht." else "$verborgen weitere Fahrzeuge passen hier nicht.",
                                    style = Schrift.Winzig,
                                    color = Farben.TextSehrLeise,
                                )
                            }

                            val werkstatt = stand.betrieb?.fahrzeuge.orEmpty().count { it.verschlissen || it.inWerkstatt }
                            if (werkstatt > 0) {
                                Text(
                                    text = if (werkstatt == 1) {
                                        "Ein Fahrzeug ist verschlissen oder in der Werkstatt — siehe Fahrzeuge."
                                    } else {
                                        "$werkstatt Fahrzeuge sind verschlissen oder in der Werkstatt — siehe Fahrzeuge."
                                    },
                                    style = Schrift.Winzig,
                                    color = Farben.TextSehrLeise,
                                )
                            }

                            // Das Nachalarmieren-Feld — zugeklappt, weil es die zweite Frage ist.
                            if (nachalarmGefiltert.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { nachalarmOffen = !nachalarmOffen }
                                        .padding(vertical = Abstand.Klein),
                                ) {
                                    Text(
                                        text = (if (nachalarmOffen) "▾ " else "▸ ") + "Nachalarmieren (${nachalarmGefiltert.size})",
                                        style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                                        color = Farben.Text,
                                    )
                                    Text(text = "gebunden — fahren, sobald frei", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                                }
                                if (nachalarmOffen) {
                                    nachalarmGefiltert.forEach { f ->
                                        Fahrzeugwahl(
                                            f = f,
                                            an = f.id in angehakt,
                                            entfernung = entfernungText(entfernungZu(f, lage)),
                                            fremderBereich = if (ausFremderLeitstelle(f, lage)) stand.bereichVonFahrzeug(f) else null,
                                            bringt = bringt(f, lage),
                                            zusatz = nachalarmText(f),
                                            beiWechsel = { angehakt = if (f.id in angehakt) angehakt - f.id else angehakt + f.id },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Absage(fehler)

                    Knopf(
                        aufschrift = if (sendet) "Alarmiert …" else alarmknopf(lage, unterwegs(lage).isNotEmpty()),
                        beiDruck = { alarmieren(lage) },
                        art = Knopfart.Haupt,
                        aktiv = !sendet,
                        breit = true,
                    )

                    // Die Freigabe — der Ruf nach Hilfe. Endgültig, deshalb zweistufig.
                    if (lage.zustaendig && !lage.freigegeben) {
                        Text(
                            text = "Freigegeben sehen sie alle — zurücknehmen geht nicht.",
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                        )
                        if (freigabeGefragt != lage.id) {
                            Knopf("Für alle freigeben", { freigabeGefragt = lage.id }, art = Knopfart.Leise, kompakt = true, aktiv = !sendet)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                                Knopf(
                                    aufschrift = if (sendet) "Gibt frei …" else "Wirklich freigeben",
                                    beiDruck = {
                                        sendet = true
                                        bereich.launch {
                                            fehler = welt.freigeben(lage.id)
                                            sendet = false
                                            if (fehler == null) freigabeGefragt = null
                                        }
                                    },
                                    art = Knopfart.Gefahr,
                                    kompakt = true,
                                    aktiv = !sendet,
                                )
                                Knopf("Nein", { freigabeGefragt = null }, art = Knopfart.Leise, kompakt = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Eine Zeile der Lagenliste: Priorität, Stichwort, Adresse — rechts Deckung, Lohn, Frist. */
@Composable
private fun Lagenzeile(
    lage: WeltLage,
    event: WeltEvent?,
    offen: Boolean,
    rest: String,
    restfarbe: Color,
    beiDruck: () -> Unit,
) {
    val gedeckt = lage.fehlt == null
    val meine = lage.zustaendig && !lage.freigegeben
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (offen) Farben.FlaecheAktiv else Farben.Flaeche, Rundung.Klein)
            .border(1.dp, if (offen) Farben.AmberTief else if (meine) Farben.AmberTief.copy(alpha = 0.6f) else Farben.Rand, Rundung.Klein)
            .clickable(onClick = beiDruck)
            .padding(Abstand.Klein),
    ) {
        val prio = when {
            lage.prioritaet >= 3 -> Farben.Signal
            lage.prioritaet == 2 -> Farben.FmsAnfahrt
            else -> Farben.Blau
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .then(if (lage.ausGrosslage) Modifier.border(2.dp, Farben.Signal, CircleShape).padding(3.dp) else Modifier)
                .background(prio, CircleShape),
        ) {
            Text(text = lage.prioritaet.toString(), style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.AufFarbe)
        }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                if (lage.eigene) Box(Modifier.size(7.dp).background(Farben.Amber, CircleShape))
                Text(
                    text = lage.stichwort,
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (event != null || lage.istTageslage || lage.nachgefordert) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    if (event != null) {
                        val ton = farbeAus(event.farbe) ?: Farben.Amber
                        Weltmarke(event.titel, ton, Modifier.widthIn(max = 140.dp), fuellung = ton.copy(alpha = 0.22f))
                    }
                    if (lage.istTageslage) Weltmarke("Täglich", Farben.AmberHell)
                    if (lage.nachgefordert) Weltmarke("Nachforderung", Farben.OrangeHell)
                }
            }
            Text(
                text = "${lage.adresse} · ${entfernungText(lage.entfernungMeter)}",
                style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG),
                color = Farben.TextLeise,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            Text(
                text = deckungstext(lage),
                style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG, fontWeight = FontWeight.Bold),
                color = if (gedeckt) Farben.FmsFrei else Farben.Text,
            )
            Credits(lage.verguetung, stil = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), zeichengroesse = 10.dp)
            Text(text = rest, style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = restfarbe)
        }
    }
}

/** Ein Fahrzeug in der Auswahl: FMS, Rufname, Typ — darunter Entfernung, Bereich, was es mitbringt. */
@Composable
private fun Fahrzeugwahl(
    f: WeltFahrzeug,
    an: Boolean,
    entfernung: String,
    fremderBereich: String?,
    bringt: List<String>,
    zusatz: String?,
    beiWechsel: () -> Unit,
) {
    Hakenkarte(an = an, beiWechsel = beiWechsel, vorn = { Fmsplakette(f.status) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = f.funkrufname,
                style = Schrift.MonoKlein,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(text = f.typ, style = Schrift.Winzig, color = Farben.TextLeise, maxLines = 1)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            Text(text = entfernung, style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextSehrLeise)
            if (fremderBereich != null) Weltmarke(fremderBereich, Farben.BlauHell)
            bringt.forEach { Weltmarke(it, Farben.FmsFrei) }
            if (zusatz != null) Text(text = zusatz, style = Schrift.Winzig, color = Farben.TextLeise)
        }
    }
}

// ---------------------------------------------------------------- Events

/**
 * Die Kulisse eines Event-Einsatzes — übertragen aus `components/event/EventKulisse.vue`:
 * Banner (oder ein Verlauf im Ton des Events), Titel, Fenster, und darunter, was die Welt
 * dazu zu sagen hat.
 */
@Composable
fun Eventkulisse(e: WeltEvent, inhalt: @Composable () -> Unit) {
    val ton = farbeAus(e.farbe) ?: Farben.Amber
    val server = serveradresse()
    val banner = e.bannerUrl?.trim()?.takeIf { bildBrauchbar(it) }?.let { if (it.startsWith("/")) server?.let { s -> "$s$it" } else it }
    val bild by bildVon(banner)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = ton.copy(alpha = 0.6f))
            .padding(Abstand.Normal),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(Rundung.Klein)
                .background(Brush.linearGradient(listOf(ton.copy(alpha = 0.3f), Farben.FlaecheHoch))),
        ) {
            bild?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(84.dp),
                )
            }
            if (e.zustand == "Laeuft") {
                Text(
                    text = "Läuft",
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.AufFarbe,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Abstand.Winzig)
                        .background(ton, Rundung.Rund)
                        .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
                )
            }
        }
        Text(text = e.titel, style = Schrift.Gross, color = Farben.Text)
        Text(
            text = when (e.zustand) {
                "Angekuendigt" -> "Beginnt ${datumzeit(e.beginntUm)}"
                "Laeuft" -> "Läuft bis ${datumzeit(e.endetUm)}"
                else -> "Vorbei seit ${datumzeit(e.endetUm)}"
            },
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
        inhalt()
    }
}

/** Ob eine Bildadresse taugt — ein Pfad vom eigenen Server oder `http(s)://`. */
private fun bildBrauchbar(url: String): Boolean {
    if (url.isBlank() || url.startsWith("//")) return false
    if (url.startsWith("/")) return true
    return url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)
}

/**
 * Der Bereitstellungsraum eines Events — übertragen aus `components/welt/EventRaum.vue`:
 * wie viele dort stehen, was die Abschnitte vermissen, und Fahrzeuge vorschicken.
 */
@Composable
fun Eventraum(welt: Welt, stand: Weltzustand, e: WeltEvent) {
    val name = e.brName ?: return
    val bereich = rememberCoroutineScope()
    var offen by remember { mutableStateOf(false) }
    var angehakt by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sendet by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    val frei = stand.freieFahrzeuge

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = name, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
            Text(
                text = "${e.bereitgestellt}" + if (e.meineBereitgestellt > 0) " · davon ${e.meineBereitgestellt} von dir" else "",
                style = Schrift.MonoKlein,
                color = Farben.TextLeise,
            )
        }
        if (!e.raumOffen) {
            Text(
                text = "Öffnet" + (e.brOffenAb?.let { " am ${datumzeit(it)}" } ?: "") + ".",
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
        }

        if (e.offeneLagen > 0) {
            if (e.offenePlaetze > 0) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Freie Fahrzeugplätze an den Abschnitten", style = Schrift.Klein, color = Farben.TextLeise, modifier = Modifier.weight(1f))
                    Text(text = e.offenePlaetze.toString(), style = Schrift.MonoKlein, color = Farben.TextLeise)
                }
            }
            e.bedarfe.forEach { b ->
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Weltmarke(b.faehigkeit, if (b.imRaum < b.fehlt) Farben.Amber else Farben.TextLeise)
                    Text(
                        text = "fehlt an ${b.fehlt} ${if (b.fehlt == 1) "Abschnitt" else "Abschnitten"} · ${b.imRaum} im Raum",
                        style = Schrift.Winzig,
                        color = Farben.TextSehrLeise,
                    )
                }
            }
        } else {
            if (e.angefordert.isNotEmpty()) {
                Text(
                    text = "Wird angefordert — die Zahl sagt, wie viele Fahrzeuge im Raum es mitbringen:",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    e.angefordert.forEach { a ->
                        Weltmarke("${a.faehigkeit} · ${a.imRaum}", if (a.imRaum == 0) Farben.Amber else Farben.TextLeise)
                    }
                }
            }

            if (!offen) {
                Knopf("Fahrzeuge vorschicken", { offen = true }, art = Knopfart.Haupt, aktiv = frei.isNotEmpty(), kompakt = true)
            } else {
                if (frei.isEmpty()) {
                    Text(text = "Kein Fahrzeug auf der Wache.", style = Schrift.Klein, color = Farben.SignalHell)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (frei.size == 1) "Ein Fahrzeug auf der Wache" else "${frei.size} Fahrzeuge auf der Wache",
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                            modifier = Modifier.weight(1f),
                        )
                        AlleWaehlen(frei.map { it.id }, angehakt) { angehakt = it }
                    }
                    frei.forEach { f ->
                        Hakenkarte(an = f.id in angehakt, beiWechsel = {
                            angehakt = if (f.id in angehakt) angehakt - f.id else angehakt + f.id
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                                Text(text = f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                                Text(text = f.typ, style = Schrift.Klein, color = Farben.TextLeise)
                            }
                        }
                    }
                }
                Absage(fehler)
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        aufschrift = if (sendet) "Fährt los …" else "Losschicken (${angehakt.size})",
                        beiDruck = {
                            if (angehakt.isEmpty()) {
                                fehler = "Wähle mindestens ein Fahrzeug."
                            } else {
                                sendet = true
                                bereich.launch {
                                    fehler = welt.eventBereitstellen(e.id, angehakt.toList())
                                    sendet = false
                                    if (fehler == null) {
                                        angehakt = emptySet()
                                        offen = false
                                    }
                                }
                            }
                        },
                        art = Knopfart.Haupt,
                        aktiv = !sendet && frei.isNotEmpty(),
                        kompakt = true,
                    )
                    Knopf("Abbrechen", { offen = false }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }

        Text(
            text = if (e.autoAbarbeiten) {
                "Fahrzeuge im Bereitstellungsraum arbeiten die Abschnitte von selbst ab — und kehren danach dorthin zurück."
            } else {
                "Bei diesem Termin wird von Hand disponiert: Die Fahrzeuge warten im Raum, bis du sie über die Lagenliste alarmierst."
            },
            style = Schrift.Winzig,
            color = Farben.TextSehrLeise,
        )
    }
}
