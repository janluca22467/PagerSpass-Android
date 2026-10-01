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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.eingelassen
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
fun GrosslageSeite(welt: Welt, zustand: Weltzustand, @Suppress("UNUSED_PARAMETER") karte: Weltkartenstand) {
    val takt = sekundentakt()
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
        Leerhinweis("Für diese Woche ist noch nichts angekündigt. Der Termin wird montags gezogen.")
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Ueberschrift(g.name, Modifier.weight(1f))
            Weltmarke(
                when (g.zustand) {
                    "Laeuft" -> "läuft"
                    "Vorbei" -> "vorbei"
                    else -> "angekündigt"
                },
                when (g.zustand) {
                    "Laeuft" -> Farben.SignalHell
                    "Vorbei" -> Farben.TextSehrLeise
                    else -> Farben.TextLeise
                },
            )
        }
        val ziel = if (g.zustand == "Angekuendigt") g.beginntUm else g.endetUm
        val ms = fahrt.restMs(ziel) ?: 0
        // Der Countdown sitzt eingelassen im tiefen Grund — `.grossblende__countdown`.
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
            style = Schrift.Schlagzeile.copy(fontFamily = Schrift.Mono),
            color = Farben.AmberHell,
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.BgTief, Rundung.Klein)
                .eingelassen()
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        )
        Leisesatz("${termin(g.beginntUm)} · zwölf Stunden · ${g.ort}")

        Wertzeile("Einsatzkräfte an den Abschnitten", zahl(g.gestellteKraefte))
        Wertzeile("Fahrzeuge im Einsatz — von allen Leitstellen", "${g.gestellt} / ${g.bedarf}", leise = true)
        val anteil = if (g.bedarf == 0) 0 else minOf(100, Math.round(g.gestellt * 100f / g.bedarf))
        Weltbalken(anteil / 100f)
        Leisesatz(
            "${g.bedarf} Fahrzeuge fordert dieser Großeinsatz insgesamt — mehr, als eine Leitstelle allein " +
                "stellen kann. Bei $anteil % ist der Bonus zu $anteil % verdient; voll gibt es ihn bei gedecktem Bedarf.",
            winzig = true,
        )
        if (g.zustand == "Laeuft") {
            Wertzeile("Welle", "${g.wellenStand} / ${g.wellen}", leise = true)
            val naechste = g.naechsteWelleUm?.let { um ->
                val rest = fahrt.restMs(um) ?: 0
                if (rest <= 0) "gleich" else {
                    val h = rest / 3_600_000
                    val m = Math.ceil((rest % 3_600_000) / 60_000.0).toInt()
                    if (h > 0) "in $h h $m min" else "in $m min"
                }
            }
            Leisesatz(
                if (naechste != null) "Der Bedarf kommt in Wellen über die zwölf Stunden verteilt — die nächste Welle " +
                    "bringt $naechste neue Abschnitte."
                else "Alle Wellen sind abgeworfen — was jetzt offen ist, ist alles, was noch kommt.",
                winzig = true,
            )
            Wertzeile("Abschnitte, die noch Fahrzeuge brauchen", "${g.offeneLagen}", leise = true)
            if (g.offenePlaetze > 0) Wertzeile("Freie Fahrzeugplätze dort", "${g.offenePlaetze}", leise = true)
            Bedarfsblatt(g.bedarfe)
        }

        if (g.anfahrtOffen) {
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
            if (g.bedarfe.isEmpty()) Anforderung(g.angefordert)
            Vorschicken(zustand) { ids -> welt.bereitstellen(ids) }
        }
        Text(
            when {
                g.beteiligt -> "Du bist dabei — Bonus bis zu ${credits(g.abschlussbonus)}, voll bei gedecktem Bedarf."
                g.zustand == "Laeuft" -> "Die Lagen stehen auf der Karte und in der Lagenliste."
                else -> "Allein nicht zu schaffen — schick Fahrzeuge vor, sie warten dort bis zum Beginn."
            },
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
        if (g.anfahrtOffen) {
            Leisesatz(
                "Fahrzeuge im Bereitstellungsraum arbeiten die Abschnitte von selbst ab — und kehren danach " +
                    "dorthin zurück.",
            )
        }
        g.vorwoche?.let { v ->
            Text(
                buildAnnotatedString {
                    append("Letzte Woche: ${v.name} — ")
                    withStyle(
                        SpanStyle(
                            color = if (v.geschafft) Farben.GruenHell else Farben.SignalHell,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    ) { append(if (v.geschafft) "geschafft" else "verfehlt") }
                    append(" (${v.gestellt}/${v.bedarf} Kräfte, ${credits(v.bonus)} je Beteiligtem).")
                },
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
        }
    }

    // ------------------------------------------------------------ Wochenziele
    ziele?.let { z ->
        Ueberschrift("Diese Woche", Modifier.padding(top = Abstand.Klein))
        z.ziele.forEach { ziel ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (ziel.erfuellt) "✓" else "○",
                    style = Schrift.Klein,
                    color = if (ziel.erfuellt) Farben.GruenHell else Farben.TextSehrLeise,
                )
                Column(Modifier.weight(1f)) {
                    Text(ziel.titel, style = Schrift.Klein, color = Farben.Text)
                    Text("${ziel.stand}/${ziel.zielwert}", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                }
                Text(credits(ziel.belohnung), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.AmberHell)
            }
        }
    }
}

@Composable
private fun Bedarfsblatt(bedarfe: List<Weltbedarf>) {
    bedarfe.forEach { b ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Weltmarke(b.faehigkeit, if (b.imRaum < b.fehlt) Farben.Amber else Farben.TextLeise)
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
            Weltmarke("${a.faehigkeit} · ${a.imRaum}", if (a.imRaum == 0) Farben.Amber else Farben.TextLeise)
        }
    }
}

/**
 * Fahrzeuge in einen Bereitstellungsraum vorschicken — Großeinsatz und Event
 * (derselbe Block in `GrosslageBlende.vue` und `EventRaum.vue`). Dorthin fährt
 * nur, was zu Hause steht.
 */
@Composable
fun Vorschicken(zustand: Weltzustand, senden: suspend (List<String>) -> String?) {
    val bereich = rememberCoroutineScope()
    var offen by remember { mutableStateOf(false) }
    var angehakt by remember { mutableStateOf(setOf<String>()) }
    var sendet by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    val frei: List<WeltFahrzeug> = zustand.freieFahrzeuge

    if (!offen) {
        Knopf("Fahrzeuge vorschicken", { offen = true }, art = Knopfart.Haupt, aktiv = frei.isNotEmpty(), breit = true)
        return
    }
    if (frei.isEmpty()) {
        Text("Kein Fahrzeug auf der Wache.", style = Schrift.Klein, color = Farben.SignalHell)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Leisesatz(
                if (frei.size == 1) "Ein Fahrzeug auf der Wache" else "${frei.size} Fahrzeuge auf der Wache",
                modifier = Modifier.weight(1f),
                winzig = true,
            )
            AlleWaehlen(frei.map { it.id }, angehakt) { angehakt = it }
        }
        frei.forEach { f ->
            Hakenzeile(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = Schrift.Mono)) { append(f.funkrufname) }
                    withStyle(SpanStyle(color = Farben.TextLeise)) { append("  ${f.typ}") }
                },
                f.id in angehakt,
                { an -> angehakt = if (an) angehakt + f.id else angehakt - f.id },
            )
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
        }, art = Knopfart.Haupt, aktiv = !sendet && frei.isNotEmpty())
        Knopf("Abbrechen", { offen = false }, art = Knopfart.Leise)
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
 * (`EventKulisse.vue` und `EventRaum.vue`): oben das Banner, darunter Titel und
 * Zeitfenster, die Beschreibung, wo es ist, und unter einem Strich der Raum.
 */
@Composable
fun Eventkasten(welt: Welt, zustand: Weltzustand, e: WeltEvent) {
    val ton = e.farbe?.let(::farbeAus) ?: Farben.Amber
    Weltkasten(randfarbe = ton.copy(alpha = 0.5f)) {
        Eventkulisse(e, ton)
        Text(e.titel, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        Leisesatz(
            when (e.zustand) {
                "Angekuendigt" -> "Beginnt ${terminKurz(e.beginntUm)}"
                "Laeuft" -> "Läuft bis ${terminKurz(e.endetUm)}"
                else -> "Vorbei seit ${terminKurz(e.endetUm)}"
            },
        )
        e.beschreibung?.takeIf { it.isNotBlank() }?.let { Text(it, style = Schrift.Klein, color = Farben.Text) }
        Leisesatz(
            "${e.ort} · " + if (e.offeneLagen == 1) "eine offene Lage" else "${e.offeneLagen} offene Lagen",
            winzig = true,
        )
        val raum = e.brName ?: return@Weltkasten
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind { drawLine(Farben.Rand, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .padding(top = Abstand.Klein),
        ) {
            Wertzeile(
                raum,
                "${e.bereitgestellt}" + if (e.meineBereitgestellt > 0) " · davon ${e.meineBereitgestellt} von dir" else "",
            )
            if (!e.raumOffen) {
                Leisesatz("Öffnet" + (e.brOffenAb?.let { " am ${terminKurz(it)}" } ?: "") + ".")
            }
            if (e.offeneLagen > 0) {
                if (e.offenePlaetze > 0) Wertzeile("Freie Fahrzeugplätze an den Abschnitten", "${e.offenePlaetze}", leise = true)
                Bedarfsblatt(e.bedarfe)
            } else {
                Anforderung(e.angefordert)
                if (e.raumOffen) Vorschicken(zustand) { ids -> welt.eventBereitstellen(e.id, ids) }
            }
            Leisesatz(
                if (e.autoAbarbeiten) "Fahrzeuge im Bereitstellungsraum arbeiten die Abschnitte von selbst ab — und " +
                    "kehren danach dorthin zurück."
                else "Bei diesem Termin wird von Hand disponiert: Die Fahrzeuge warten im Raum, bis du sie über die " +
                    "Lagenliste alarmierst.",
                winzig = true,
            )
        }
    }
}
