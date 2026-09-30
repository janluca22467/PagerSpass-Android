package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.WeltEvent
import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltZiele
import de.pagerspass.pagerspass.netz.Weltanforderung
import de.pagerspass.pagerspass.netz.Weltbedarf
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.launch

/**
 * Der Großeinsatz der Woche — das Gegenstück zu `GrosslageBlende.vue`.
 *
 * <b>Die Zahlen darüber sind ohne die Wellen nicht zu deuten.</b> „30 / 148“
 * nach zwei Stunden sieht aussichtslos aus, solange nirgends steht, dass erst
 * die zweite von vier Wellen gelaufen ist. Deshalb steht die Welle mit dabei.
 *
 * Darunter die Wochenziele — sie hängen an derselben Woche.
 */
@Composable
fun GrosslageSeite(welt: Welt, zustand: Weltzustand, karte: Weltkartenstand) {
    val takt = sekundentakt()
    val bereich = rememberCoroutineScope()
    var ziele by remember { mutableStateOf<WeltZiele?>(null) }
    LaunchedEffect(zustand.gutschrift) {
        welt.grosslageLaden()
        val k = welt.kennung ?: return@LaunchedEffect
        ziele = runCatching { welt.wege.ziele(k) }.getOrNull()
    }
    @Suppress("UNUSED_EXPRESSION") takt
    val fahrt = zustand.fahrt
    val g = zustand.grosslage

    if (g == null) {
        Leisesatz("Gerade ist kein Großeinsatz angekündigt. Er kommt einmal die Woche — hier steht dann, wann und wo.")
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text(g.name, style = Schrift.Gross.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
            Weltmarke(
                when (g.zustand) {
                    "Laeuft" -> "läuft"
                    "Vorbei" -> "vorbei"
                    else -> "angekündigt"
                },
                if (g.zustand == "Laeuft") Farben.SignalHell else Farben.TextLeise,
            )
        }
        val ziel = if (g.zustand == "Angekuendigt") g.beginntUm else g.endetUm
        val ms = fahrt.restMs(ziel) ?: 0
        Text(
            when {
                ms <= 0 -> if (g.zustand == "Angekuendigt") "jetzt" else "vorbei"
                else -> {
                    val h = ms / 3_600_000
                    val m = (ms % 3_600_000) / 60_000
                    when {
                        h >= 24 -> "in ${h / 24} Tagen, ${h % 24} h"
                        h > 0 -> "in $h h $m min"
                        else -> "in $m min"
                    }
                }
            },
            style = Schrift.Titel.copy(fontFamily = Schrift.Mono),
            color = Farben.Amber,
        )
        Leisesatz("${termin(g.beginntUm)} · zwölf Stunden · ${g.ort}")
        Knopf("Auf der Karte zeigen", { karte.hinschauen(g.lat, g.lon, 11.0) }, kompakt = true, art = Knopfart.Leise)

        if (g.zustand != "Angekuendigt") {
            Wertzeile("Einsatzkräfte an den Abschnitten", zahl(g.gestellteKraefte))
            Wertzeile("Fahrzeuge im Einsatz — von allen Leitstellen", "${g.gestellt} / ${g.bedarf}", leise = true)
            val anteil = if (g.bedarf == 0) 0 else minOf(100, Math.round(g.gestellt * 100f / g.bedarf))
            Weltbalken(anteil / 100f)
            Leisesatz(
                "${g.bedarf} Fahrzeuge fordert dieser Großeinsatz insgesamt — mehr, als eine Leitstelle allein " +
                    "stellen kann. Bei $anteil % ist der Bonus zu $anteil % verdient; voll gibt es ihn bei gedecktem Bedarf.",
                winzig = true,
            )
            if (g.wellen > 0) {
                Wertzeile("Welle", "${g.wellenStand} / ${g.wellen}", leise = true)
                g.naechsteWelleUm?.let { um ->
                    val rest = fahrt.restMs(um) ?: 0
                    val text = if (rest <= 0) "gleich" else {
                        val h = rest / 3_600_000
                        val m = Math.ceil((rest % 3_600_000) / 60_000.0).toInt()
                        if (h > 0) "in $h h $m min" else "in $m min"
                    }
                    Leisesatz("Die nächste Welle bringt $text neue Abschnitte.", winzig = true)
                }
            }
            Wertzeile("Abschnitte, die noch Fahrzeuge brauchen", "${g.offeneLagen}", leise = true)
            if (g.offenePlaetze > 0) Wertzeile("Freie Fahrzeugplätze dort", "${g.offenePlaetze}", leise = true)
            Bedarfsblatt(g.bedarfe)
        }

        Wertzeile(
            "Im Bereitstellungsraum",
            "${zahl(g.bereitgestellteKraefte)} Kräfte" +
                if (g.meineBereitgestelltenKraefte > 0) " · davon ${zahl(g.meineBereitgestelltenKraefte)} von dir" else "",
        )
        if (g.bereitgestellt > 0) {
            Wertzeile(
                "Fahrzeuge dort",
                "${g.bereitgestellt}" + if (g.meineBereitgestellt > 0) " · davon ${g.meineBereitgestellt} von dir" else "",
                leise = true,
            )
        }
        if (g.offeneLagen == 0) Anforderung(g.angefordert)

        if (g.anfahrtOffen) {
            Vorschicken(zustand, knopf = "Fahrzeuge vorschicken") { ids -> welt.bereitstellen(ids) }
            Leisesatz(
                "Vorgeschickte Fahrzeuge warten im Bereitstellungsraum und werden von dort den Abschnitten " +
                    "zugeteilt — oder du alarmierst sie selbst auf einen Abschnitt.",
                winzig = true,
            )
        }
        Text(
            "Bonus bei gedecktem Bedarf: ${credits(g.abschlussbonus)}",
            style = Schrift.Klein,
            color = Farben.AmberHell,
        )
        g.vorwoche?.let { v ->
            Leisesatz(
                "Letzte Woche: ${v.name} — ${if (v.geschafft) "geschafft" else "verfehlt"} " +
                    "(${v.gestellt}/${v.bedarf} Kräfte, ${zahl(v.bonus)} $WAEHRUNG).",
            )
        }
    }

    // ------------------------------------------------------------ Wochenziele
    ziele?.let { z ->
        Ueberschrift("Diese Woche")
        z.ziele.forEach { ziel ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), verticalAlignment = Alignment.CenterVertically) {
                Text(if (ziel.erfuellt) "✓" else "○", style = Schrift.Normal, color = if (ziel.erfuellt) Farben.GruenHell else Farben.TextSehrLeise)
                Column(Modifier.weight(1f)) {
                    Text(ziel.titel, style = Schrift.Klein, color = Farben.Text)
                    Text("${ziel.stand}/${ziel.zielwert}", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                }
                Text(credits(ziel.belohnung), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.Amber)
            }
        }
    }

    // ---------------------------------------------------------------- Events
    val events = zustand.events.filter { it.zustand != "Vorbei" && it.zustand != "Entwurf" }
    if (events.isNotEmpty()) {
        Ueberschrift("Events")
        events.forEach { Eventkasten(welt, zustand, it) }
    }
}

@Composable
private fun Bedarfsblatt(bedarfe: List<Weltbedarf>) {
    bedarfe.forEach { b ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Weltmarke(b.faehigkeit, if (b.imRaum < b.fehlt) Farben.SignalHell else Farben.TextLeise)
            Text(
                "fehlt an ${b.fehlt} ${if (b.fehlt == 1) "Abschnitt" else "Abschnitten"} · ${b.imRaum} im Raum",
                style = Schrift.Winzig,
                color = Farben.TextSehrLeise,
            )
        }
    }
}

@Composable
private fun Anforderung(angefordert: List<Weltanforderung>) {
    if (angefordert.isEmpty()) return
    Leisesatz("Wird angefordert — die Zahl sagt, wie viele Fahrzeuge im Raum es mitbringen:", winzig = true)
    Umbruchreihe {
        angefordert.forEach { a ->
            Weltmarke("${a.faehigkeit} · ${a.imRaum}", if (a.imRaum == 0) Farben.SignalHell else Farben.TextLeise)
        }
    }
}

/**
 * Fahrzeuge in einen Bereitstellungsraum vorschicken — Großeinsatz und Event.
 * Dorthin fährt nur, was zu Hause steht.
 */
@Composable
fun Vorschicken(zustand: Weltzustand, knopf: String, senden: suspend (List<String>) -> String?) {
    val bereich = rememberCoroutineScope()
    var offen by remember { mutableStateOf(false) }
    var angehakt by remember { mutableStateOf(setOf<String>()) }
    var sendet by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    val frei: List<WeltFahrzeug> = zustand.freieFahrzeuge

    if (!offen) {
        Knopf(knopf, { offen = true }, art = Knopfart.Haupt, aktiv = frei.isNotEmpty(), kompakt = true)
        if (frei.isEmpty()) Leisesatz("Kein Fahrzeug auf der Wache.", winzig = true)
        return
    }
    if (frei.isEmpty()) {
        Warnsatz("Kein Fahrzeug auf der Wache.")
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Leisesatz(
                if (frei.size == 1) "Ein Fahrzeug auf der Wache" else "${frei.size} Fahrzeuge auf der Wache",
                modifier = Modifier.weight(1f),
                winzig = true,
            )
            val alle = frei.all { it.id in angehakt }
            Knopf(if (alle) "Keine" else "Alle ${frei.size}", {
                angehakt = if (alle) emptySet() else frei.map { it.id }.toSet()
            }, kompakt = true, art = Knopfart.Leise)
        }
        frei.forEach { f ->
            Wahlzeile(an = f.id in angehakt, beiWechsel = {
                angehakt = if (f.id in angehakt) angehakt - f.id else angehakt + f.id
            }) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                    Text(f.typ, style = Schrift.Winzig, color = Farben.TextLeise)
                }
            }
        }
    }
    Warnsatz(fehler)
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Knopf(if (sendet) "Fährt los …" else "Losschicken (${angehakt.size})", {
            if (angehakt.isEmpty()) {
                fehler = "Wähle mindestens ein Fahrzeug."
                return@Knopf
            }
            sendet = true
            bereich.launch {
                fehler = senden(angehakt.toList())
                sendet = false
                if (fehler == null) {
                    angehakt = emptySet()
                    offen = false
                }
            }
        }, art = Knopfart.Haupt, kompakt = true, aktiv = !sendet && frei.isNotEmpty())
        Knopf("Abbrechen", { offen = false }, art = Knopfart.Leise, kompakt = true)
    }
}

/**
 * Ob eine Bildadresse benutzbar ist — dieselbe Prüfung wie `bildBrauchbar` in
 * `eventbilder.ts` (und `Eventeinsatz.BildInOrdnung` am Server): der hauseigene
 * Pfad oder eine vollständige http(s)-Adresse, nie ein Doppelschrägstrich am
 * Anfang — das wäre eine fremde Herkunft ohne Schema. Gibt die volle Adresse.
 */
internal fun eventbild(url: String?, server: String?): String? {
    val wert = url?.trim().orEmpty()
    return when {
        wert.isEmpty() || wert.startsWith("//") -> null
        wert.startsWith("/") -> server?.let { it.trimEnd('/') + wert }
        Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(wert) -> wert
        else -> null
    }
}

/**
 * Die Kulisse eines Events (`EventKulisse.vue`): das Banner, dahinter gedämpft der
 * Hintergrund, und wo beides fehlt oder nicht lädt, ein Verlauf in der Farbe des
 * Events — nicht das zerbrochene Bildsymbol, das nach „kaputt" aussähe statt nach
 * „ohne Bild".
 */
@Composable
private fun Eventkulisse(e: WeltEvent, ton: androidx.compose.ui.graphics.Color) {
    val zusammenhang = androidx.compose.ui.platform.LocalContext.current
    val server by androidx.compose.runtime.produceState<String?>(null) {
        value = runCatching { de.pagerspass.pagerspass.netz.Ablage(zusammenhang).server() }.getOrNull()
    }
    val banner by de.pagerspass.pagerspass.ui.schmuck.bildVon(eventbild(e.bannerUrl, server))
    val grund by de.pagerspass.pagerspass.ui.schmuck.bildVon(eventbild(e.hintergrundUrl, server))
    val form = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(form)
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(ton.copy(alpha = 0.32f), Farben.FlaecheHoch),
                    radius = 520f,
                ),
            ),
    ) {
        grund?.let {
            androidx.compose.foundation.Image(
                it,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = if (banner == null) 0.45f else 0.25f,
                modifier = Modifier.matchParentSize(),
            )
        }
        banner?.let {
            // Der Alternativtext bleibt leer: Was auf dem Bild steht, steht als
            // Titel darunter.
            androidx.compose.foundation.Image(
                it,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        if (e.zustand == "Laeuft") {
            Text(
                "Läuft",
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                color = Farben.AufAmber,
                modifier = Modifier
                    .padding(Abstand.Klein)
                    .background(ton, androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
                    .padding(horizontal = Abstand.Klein, vertical = 2.dp),
            )
        }
    }
}

/**
 * Ein Event-Einsatz der World — die Kulisse samt Bereitstellungsraum
 * (`EventKulisse.vue` und `EventRaum.vue`).
 */
@Composable
fun Eventkasten(welt: Welt, zustand: Weltzustand, e: WeltEvent) {
    val ton = e.farbe?.let(::farbeAus) ?: Farben.Violett
    Weltkasten(randfarbe = ton) {
        Eventkulisse(e, ton)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Farbpunkt(ton, 10)
            Text(e.titel, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
            Weltmarke(if (e.zustand == "Laeuft") "läuft" else "angekündigt", ton)
        }
        e.beschreibung?.takeIf { it.isNotBlank() }?.let { Leisesatz(it) }
        Leisesatz(
            "${e.ort} · " + (if (e.offeneLagen == 1) "eine offene Lage" else "${e.offeneLagen} offene Lagen") +
                " · ${terminKurz(e.beginntUm)} bis ${terminKurz(e.endetUm)}",
            winzig = true,
        )
        val raum = e.brName ?: return@Weltkasten
        Wertzeile(
            raum,
            "${e.bereitgestellt}" + if (e.meineBereitgestellt > 0) " · davon ${e.meineBereitgestellt} von dir" else "",
        )
        if (!e.raumOffen) {
            Leisesatz("Öffnet" + (e.brOffenAb?.let { " am ${terminKurz(it)}" } ?: "") + ".", winzig = true)
        }
        if (e.offeneLagen > 0) {
            if (e.offenePlaetze > 0) Wertzeile("Freie Fahrzeugplätze an den Abschnitten", "${e.offenePlaetze}", leise = true)
            Bedarfsblatt(e.bedarfe)
        } else {
            Anforderung(e.angefordert)
        }
        if (e.raumOffen) {
            Vorschicken(zustand, knopf = "Fahrzeuge vorschicken") { ids -> welt.eventBereitstellen(e.id, ids) }
        }
        Leisesatz(
            if (e.autoAbarbeiten) "Die Fahrzeuge fahren von dort selbst auf die Abschnitte."
            else "Von dort fährt niemand von selbst — alarmiere die Fahrzeuge an den Abschnitten.",
            winzig = true,
        )
    }
}
