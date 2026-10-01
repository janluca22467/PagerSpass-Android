package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.WeltBauart
import de.pagerspass.pagerspass.netz.WeltPoiAenderung
import de.pagerspass.pagerspass.netz.Weltpoi
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.launch

private val POI_ARTEN = listOf(
    "Schule" to "Schule", "Kindergarten" to "Kindertagesstätte", "Pflegeheim" to "Pflegeheim",
    "Arztpraxis" to "Arztpraxis", "Beherbergung" to "Beherbergung", "Verkaufsstaette" to "Verkaufsstätte",
    "Industrie" to "Industrie", "Versammlungsstaette" to "Versammlungsstätte", "Bahnhof" to "Bahnhof",
    "Sportstaette" to "Sportstätte", "Hochhaus" to "Hochhaus", "Tankstelle" to "Tankstelle",
    "Landwirtschaft" to "Landwirtschaftlicher Betrieb", "Campingplatz" to "Campingplatz",
    "Parkhaus" to "Parkhaus", "Energieanlage" to "Energieanlage",
)

private val GELAENDEARTEN = listOf(
    "" to "Grundstück eines Gebäudes", "Wald" to "Wald", "Feld" to "Feld oder Wiese",
    "Autobahn" to "Autobahn", "Schiene" to "Bahnstrecke", "Gewaesser" to "Gewässer",
)

private val ORGANISATIONEN = listOf("Feuerwehr" to "Feuerwehr", "Rettungsdienst" to "Rettungsdienst", "Polizei" to "Polizei", "Thw" to "THW")

/**
 * Bauen — das Gegenstück zu `BauBlende.vue` mit `nur="wache"`, so wie das Web
 * sie am Handy öffnet: ohne Reiterreihe „Wache | Fahrzeug“, denn der Kauf hat
 * seine eigene Seite (aus „Fahrzeuge“ und von der Wache aus).
 *
 * <b>Erst eine Übersicht, dann ein Vorgang.</b> Vorher war jeder Blick in das
 * Fenster ein angefangener Bauvorgang: Die Karte deutete jeden Tipp als
 * Standortwahl, obwohl man nur schauen wollte, was Bauen kostet. Jetzt nennt
 * jeder Weg seinen Preis und wartet auf den Knopf.
 *
 * <b>Der Standort kommt von der Karte</b> — über „Standort auf der Karte
 * wählen" (die Seite geht dafür zu) oder lang gedrückt auf der Karte.
 */
@Composable
fun BauSeite(welt: Welt, zustand: Weltzustand, werkbank: Werkbank) {
    val zweigMoeglich = zweigMoeglich(zustand)
    // Fällt die Zweigstelle weg, während ihr Formular offen steht (die letzte
    // erlaubte ist gegründet), geht es zurück zur Übersicht — wie im Web.
    val vorgang = if (werkbank.bauVorgang == "zweig" && !zweigMoeglich) "keiner" else werkbank.bauVorgang
    when (vorgang) {
        "wache" -> Wachenbau(welt, zustand, werkbank)
        "zweig" -> Zweigstellenbau(welt, zustand, werkbank)
        "poi" -> Punkte(welt, zustand, werkbank)
        else -> Bauuebersicht(zustand, werkbank, zweigMoeglich)
    }
}

/** Ob die Übersicht die nächste Leitstelle anbietet — `zweigMoeglich`. */
private fun zweigMoeglich(zustand: Weltzustand): Boolean {
    val z = zustand.stand?.zweigstelle ?: return false
    return !(z.hoechstens > 0 && z.schon >= z.hoechstens)
}

@Composable
private fun Bauuebersicht(zustand: Weltzustand, werkbank: Werkbank, zweigMoeglich: Boolean) {
    val bauarten = zustand.stand?.bauarten.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Weg(
            titel = "Wache bauen",
            text = "Ab ${zahl(bauarten.firstOrNull()?.preis ?: 0)} Credits — der Standort kommt danach per " +
                "Tipp auf die Karte.",
            knopf = "Bauen",
            haupt = true,
        ) { werkbank.bauVorgang = "wache" }
        Weg(
            titel = "Eigenen Punkt setzen",
            text = "Eine Schule, ein Heim, ein Betrieb — dort entstehen passende Einsätze. Kostenlos, " +
                "höchstens 40 Stück.",
            knopf = "Punkte",
        ) { werkbank.bauVorgang = "poi" }
        val zweig = zustand.stand?.zweigstelle
        if (zweigMoeglich && zweig != null) {
            // „2. Leitstelle“ statt „Zweigstelle“: So fragen die Spieler danach.
            Weg(
                titel = "${zweig.schon + 2}. Leitstelle gründen",
                text = if (zweig.frei) "Ein weiterer Ausrückebereich für ${zahl(zweig.preis)} Credits."
                else "Öffnet ab Stufe ${zweig.abStufe} — was dann geht, steht hinter dem Knopf.",
                knopf = "Ansehen",
            ) { werkbank.bauVorgang = "zweig" }
        }
    }
}

/** Ein Weg der Übersicht — `.baublende__weg`: Titel und Preis links, der Knopf rechts. */
@Composable
private fun Weg(titel: String, text: String, knopf: String, haupt: Boolean = false, beiDruck: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Klein),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            Text(titel, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Text(text, style = Schrift.Winzig, color = Farben.TextSehrLeise)
        }
        Knopf(knopf, beiDruck, kompakt = true, art = if (haupt) Knopfart.Haupt else Knopfart.Normal)
    }
}

/**
 * Die Brücke zur Karte — `Bauortwahl.vue`: darüber, was gesetzt ist, darunter
 * ein breiter Knopf mit der Stecknadel in Amber.
 */
@Composable
private fun Bauortwahl(werkbank: Werkbank, zweck: String, wort: String = "Standort") {
    val ort = werkbank.bauort
    if (ort != null) {
        Text(
            buildAnnotatedString {
                append("$wort gesetzt: ")
                withStyle(SpanStyle(fontFamily = Schrift.Mono)) { append("%.4f, %.4f".format(ort.lat, ort.lon)) }
            },
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ziel.Normal)
            .background(Farben.FlaecheHoch, Rundung.Klein)
            .border(1.dp, Farben.AmberTief, Rundung.Klein)
            .clickable {
                werkbank.ortZweck = zweck
                werkbank.karteWaehlen(Kartenmodus.Ort)
            }
            .padding(horizontal = Abstand.Gross),
    ) {
        Icon(Weltzeichen.Stecknadel, contentDescription = null, tint = Farben.Amber, modifier = Modifier.size(18.dp))
        Text(
            if (ort != null) "$wort ändern" else "$wort auf der Karte wählen",
            style = Schrift.Knopf,
            color = Farben.Amber,
        )
    }
}

@Composable
private fun Wachenbau(welt: Welt, zustand: Weltzustand, werkbank: Werkbank) {
    val bereich = rememberCoroutineScope()
    val bauarten = zustand.stand?.bauarten.orEmpty()
    val name = werkbank.bauName
    val art: WeltBauart? = bauarten.firstOrNull { it.art == werkbank.bauArt } ?: bauarten.firstOrNull { it.frei }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }

    Feld(wert = name, beiAenderung = { werkbank.bauName = it.take(60) }, etikett = "Name", platzhalter = "Feuerwache Mitte")
    Auswahl(
        etikett = "Bauart",
        eintraege = bauarten,
        gewaehlt = art,
        aufschrift = { it.name },
        unterschrift = { b ->
            val plaetze = if (b.art == "Lehrgangseinrichtung") (if (b.stellplaetze == 1) "Lehrsaal" else "Lehrsäle")
            else "Plätze"
            "${zahl(b.preis)} Credits · ${b.stellplaetze} $plaetze" + if (b.frei) "" else " (gesperrt)"
        },
        beiWahl = { if (it.frei) werkbank.bauArt = it.art },
    )
    Bauortwahl(werkbank, "wache")
    Warnsatz(fehler)
    Knopf(
        if (sendet) "Wird gebaut …" else "Bauen für ${credits(art?.preis ?: 0)}",
        {
            val ort = werkbank.bauort ?: run { fehler = "Setz zuerst den Standort auf der Karte."; return@Knopf }
            val b = art ?: return@Knopf
            sendet = true
            fehler = null
            bereich.launch {
                fehler = welt.handlung("Der Bau ging nicht.") {
                    welt.wege.wacheBauen(it, name.trim(), b.art, ort.lat, ort.lon)
                    welt.allesLaden()
                }
                sendet = false
                if (fehler == null) {
                    werkbank.bauName = ""
                    werkbank.bauort = null
                    werkbank.bauVorgang = "keiner"
                }
            }
        },
        art = Knopfart.Haupt,
        breit = true,
        aktiv = !sendet && art?.frei == true && werkbank.bauort != null && name.trim().length >= 3,
    )
    Knopf("Abbrechen", { werkbank.bauVorgang = "keiner"; fehler = null }, art = Knopfart.Leise, kompakt = true)
}

@Composable
private fun Zweigstellenbau(welt: Welt, zustand: Weltzustand, werkbank: Werkbank) {
    val bereich = rememberCoroutineScope()
    val z = zustand.stand?.zweigstelle
    val name = werkbank.zweigName
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    val km = Math.round((z?.mindestabstandMeter?.takeIf { it > 0 } ?: 35_000.0) / 1000)

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Klein),
    ) {
        Leisesatz(
            "Eine weitere Leitstelle (die Zweigstelle): ein eigener Ausrückebereich mit eigenem Namen, eigenem " +
                "Landkreis für die Straßennamen dort und einem eigenen Bauradius. Sie muss mindestens $km km von " +
                "jedem deiner Bereiche entfernt liegen. Sobald dort Wachen stehen, entstehen dort auch Einsätze. " +
                "Guthaben, Erfahrung und Stufe bleiben gemeinsam." +
                if ((z?.hoechstens ?: 0) > 0) " Auf deiner Stufe sind ${z?.hoechstens} erlaubt, du hast ${z?.schon ?: 0}." else "",
            winzig = true,
        )
        if (z?.frei != true) {
            // Die Sperre ist die Antwort, keine Fußnote: Etikett darüber, kein Grau auf Grau.
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                Etikett("Ab Stufe ${z?.abStufe ?: 20}")
                Text(
                    "Du bist auf Stufe ${zustand.stufe} — mit Stufe ${z?.abStufe ?: 20} kannst du hier für " +
                        "${zahl(z?.preis ?: 0)} Welt-Credits den nächsten Ausrückebereich gründen.",
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                )
            }
        } else {
            Feld(wert = name, beiAenderung = { werkbank.zweigName = it.take(60) }, platzhalter = "Name des zweiten Bereichs")
            Bauortwahl(werkbank, "zweig")
            Warnsatz(fehler)
            Knopf(
                if (sendet) "Wird gegründet …" else "Gründen für ${credits(z.preis)}",
                {
                    val ort = werkbank.bauort ?: run {
                        fehler = "Setz zuerst den Standort mit einem Tipp auf die Karte."
                        return@Knopf
                    }
                    sendet = true
                    fehler = null
                    bereich.launch {
                        fehler = welt.handlung("Die Gründung ging nicht.") {
                            welt.wege.zweigstelleGruenden(it, name.trim(), ort.lat, ort.lon)
                            welt.allesLaden()
                        }
                        sendet = false
                        if (fehler == null) {
                            werkbank.zweigName = ""
                            werkbank.bauort = null
                            werkbank.bauVorgang = "keiner"
                        }
                    }
                },
                art = Knopfart.Haupt,
                breit = true,
                aktiv = !sendet && werkbank.bauort != null && name.trim().length >= 3,
            )
            if (werkbank.bauort == null) {
                Leisesatz(
                    "Der Standort kommt vom selben Tipp auf die Karte wie beim Wachenbau — er muss mindestens " +
                        "$km km von jedem deiner Bereiche entfernt liegen.",
                    winzig = true,
                )
            }
        }
        Knopf("Zurück", { werkbank.bauVorgang = "keiner" }, art = Knopfart.Leise, kompakt = true)
    }
}

/**
 * Eigene Punkte — Punkt, Fläche oder Strecke (`.baublende__poi`). Die Liste
 * steht unter dem Formular in derselben Fläche: Wer einen Punkt stilllegen
 * will, sucht ihn dort, wo er ihn angelegt hat.
 */
@Composable
private fun Punkte(welt: Welt, zustand: Weltzustand, werkbank: Werkbank) {
    val bereich = rememberCoroutineScope()
    val form = werkbank.poiForm
    val name = werkbank.poiName
    val art = POI_ARTEN.firstOrNull { it.first == werkbank.poiArt } ?: POI_ARTEN.first()
    val gelaendeart = GELAENDEARTEN.firstOrNull { it.first == werkbank.poiGelaendeart } ?: GELAENDEARTEN.first()
    val flaeche = werkbank.poiFlaeche
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    val offenerPunkt = werkbank.offenerPunkt
    val gelaendeFuer = werkbank.gelaendeFuer

    val zeichnet = form != "Punkt"
    val strecke = form == "Strecke"
    val noetig = if (strecke) 2 else 3
    val ziehtNeues = werkbank.ziehtAuf && gelaendeFuer == null
    val ecken = werkbank.ecken.map { it.lat to it.lon }

    fun zeichnenBeginnen(fuer: String?, alsStrecke: Boolean) {
        werkbank.gelaendeFuer = fuer
        werkbank.ziehtAuf = true
        werkbank.ecken = emptyList()
        werkbank.eckenStrecke = alsStrecke
        fehler = null
        werkbank.karteWaehlen(Kartenmodus.Gelaende)
    }

    fun zeichnenBeenden() {
        werkbank.ziehtAuf = false
        werkbank.gelaendeFuer = null
        werkbank.ecken = emptyList()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Klein),
    ) {
        Etikett("Was du setzt")
        Pillenreihe {
            POI_FORMEN.forEach { f ->
                Pille(f, form == f, { werkbank.poiForm = f; zeichnenBeenden() }, aktiv = !werkbank.ziehtAuf)
            }
        }
        Leisesatz(
            when (form) {
                "Punkt" -> "Eine Adresse: Jede Lage liegt an derselben Stelle, mit derselben Anfahrt."
                "Fläche" -> "Eine Fläche, die du umfährst — Werksgelände, Waldstück, Bahnhofsvorfeld. Die Lage " +
                    "entsteht irgendwo darin, mal vorn an der Pforte und mal hinten am Lager."
                else -> "Eine Linie, die du abfährst — Bahnstrecke, Autobahnabschnitt, Flussufer. Die Lage " +
                    "entsteht irgendwo darauf. Als Fläche müsstest du die Gleise hin und zurück umfahren."
            },
            winzig = true,
        )
        Feld(wert = name, beiAenderung = { werkbank.poiName = it.take(60) }, etikett = "Name", platzhalter = "Grundschule am Park")
        if (zeichnet) {
            Auswahl(
                "Was für ein Gelände", GELAENDEARTEN, gelaendeart, { it.second }, { werkbank.poiGelaendeart = it.first },
                aktiv = !werkbank.ziehtAuf,
            )
            Leisesatz(
                "Ein Grundstück bekommt die Lagen seiner Gebäudeart — Brandmeldeanlage, Menschen im Gebäude. Wald, " +
                    "Feld, Autobahn, Bahnstrecke und Gewässer bekommen ihre eigenen: Vegetationsbrand, Vermisstensuche, " +
                    "Verkehrsunfall, Zugunglück, Person im Wasser. Für die meisten Landkreise fehlen diese Orte noch " +
                    "ganz — ein selbst gezogenes Waldstück ist dort der einzige Wald, den deine Welt hat.",
                winzig = true,
            )
            if (gelaendeart.first == "Gewaesser") {
                Leisesatz(
                    "Beim Gewässer sagt die Form, was es ist: Eine Fläche ist ein See, eine Strecke ein Fluss. Eine " +
                        "Seelage sucht sich zuerst einen See — gibt es nur Flüsse, nimmt sie einen davon, statt ins " +
                        "Nichts zu fallen.",
                    winzig = true,
                )
            }
        }
        if (!zeichnet || gelaendeart.first.isEmpty()) {
            Auswahl("Art des Gebäudes", POI_ARTEN, art, { it.second }, { werkbank.poiArt = it.first })
        }
        if (!zeichnet) {
            Zahlfeld("Grundfläche in m²", flaeche, { werkbank.poiFlaeche = it }, groesstes = 50_000)
            Leisesatz(
                "Aus ihr folgt, wie viele Menschen bei einem Schadenereignis darin sind — und damit, wie viel ein " +
                    "Brand dort fordert.",
                winzig = true,
            )
            Bauortwahl(werkbank, "poi", "Ort")
        } else {
            Etikett(if (strecke) "Verlauf" else "Umriss")
            Leisesatz(
                when {
                    !ziehtNeues -> "Noch nichts gezeichnet. " +
                        if (strecke) "Zwei Punkte sind das Mindeste, zwölf das Meiste."
                        else "Drei Ecken sind das Mindeste, zwölf das Meiste."
                    ecken.size < noetig -> "${ecken.size} von mindestens $noetig Punkten — tipp weiter auf die Karte."
                    else -> "${ecken.size} Punkte · " + masswort(ecken, werkbank.eckenStrecke)
                },
                winzig = true,
            )
            Umbruchreihe {
                if (!ziehtNeues) {
                    Knopf("Zeichnen", { zeichnenBeginnen(null, strecke) }, kompakt = true, aktiv = !werkbank.ziehtAuf)
                } else {
                    Knopf("Weiter auf der Karte", { werkbank.karteWaehlen(Kartenmodus.Gelaende) }, kompakt = true)
                    Knopf("Punkt zurück", { werkbank.ecken = werkbank.ecken.dropLast(1) }, kompakt = true, aktiv = ecken.isNotEmpty())
                    Knopf("Verwerfen", { zeichnenBeenden() }, kompakt = true)
                }
            }
        }
        Warnsatz(fehler)
        Knopf(
            if (sendet) "Wird gesetzt …" else "$form setzen",
            {
                if (name.isBlank()) { fehler = "Der Punkt braucht einen Namen."; return@Knopf }
                val gelaende = if (zeichnet) werkbank.ecken else null
                val sitz = gelaende?.firstOrNull() ?: werkbank.bauort ?: run {
                    fehler = "Setz zuerst den Ort auf der Karte."
                    return@Knopf
                }
                sendet = true
                fehler = null
                bereich.launch {
                    fehler = welt.poiSetzen(
                        name = name.trim(),
                        art = art.first,
                        lat = sitz.lat,
                        lon = sitz.lon,
                        flaeche = flaeche.toDouble(),
                        gelaende = gelaende,
                        gelaendeart = if (gelaende != null) gelaendeart.first.ifEmpty { null } else null,
                        strecke = strecke,
                    )
                    sendet = false
                    if (fehler == null) {
                        werkbank.poiName = ""
                        zeichnenBeenden()
                        werkbank.bauort = null
                    }
                }
            },
            art = Knopfart.Haupt,
            breit = true,
            aktiv = !sendet && if (zeichnet) ziehtNeues && ecken.size >= noetig else werkbank.bauort != null,
        )
        Knopf("Zurück", { werkbank.bauVorgang = "keiner"; zeichnenBeenden() }, art = Knopfart.Leise, kompakt = true)

        zustand.pois.forEach { p ->
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind { drawLine(Farben.Rand, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                    .padding(Abstand.Winzig),
            ) {
                Text(p.name, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = if (p.aktiv) Farben.Text else Farben.TextSehrLeise)
                Text(
                    buildString {
                        append(p.artText.ifBlank { p.art })
                        if (p.gelaende.isNotEmpty()) append(" · ").append(groesse(p))
                        if (p.betroffene > 0) append(" · etwa ${p.betroffene} Personen")
                        if (!p.aktiv) append(" · stillgelegt")
                    },
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
                Umbruchreihe {
                    Knopf(if (p.sichtbar) "Ausblenden" else "Einblenden", {
                        bereich.launch { fehler = welt.poiAendern(p.id, WeltPoiAenderung(sichtbar = !p.sichtbar)) }
                    }, kompakt = true)
                    Knopf(if (p.aktiv) "Stilllegen" else "Anschalten", {
                        bereich.launch { fehler = welt.poiAendern(p.id, WeltPoiAenderung(aktiv = !p.aktiv)) }
                    }, kompakt = true)
                    // Gedrückt, solange der Block offen steht.
                    Knopf("Einstellen", {
                        werkbank.offenerPunkt = if (offenerPunkt == p.id) null else p.id
                    }, kompakt = true, art = if (offenerPunkt == p.id) Knopfart.Haupt else Knopfart.Normal)
                    Knopf("Entfernen", { bereich.launch { fehler = welt.poiEntfernen(p.id) } }, kompakt = true)
                }
                if (offenerPunkt == p.id) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind { drawLine(Farben.Rand, Offset(0f, 0f), Offset(0f, size.height), 2.dp.toPx()) }
                            .padding(start = Abstand.Klein, top = Abstand.Klein, bottom = Abstand.Klein),
                    ) {
                        Punktform(
                            welt, p, werkbank,
                            ziehtAuf = werkbank.ziehtAuf && gelaendeFuer == p.id,
                            andererZieht = werkbank.ziehtAuf,
                            beiBeginnen = { alsStrecke -> zeichnenBeginnen(p.id, alsStrecke) },
                            beiBeenden = { zeichnenBeenden() },
                        ) { fehler = it }
                    }
                }
            }
        }
    }
}

private val POI_FORMEN = listOf("Punkt", "Fläche", "Strecke")

private fun masswort(ecken: List<Pair<Double, Double>>, strecke: Boolean): String =
    if (strecke) laengenwort(zuglaengeMeter(ecken)) else flaechenwort(vieleckQuadratmeter(ecken))

private fun groesse(p: Weltpoi): String =
    if (p.strecke) laengenwort(p.laenge) else flaechenwort(vieleckQuadratmeter(p.gelaende.map { it.lat to it.lon }))

/** Die Einstellungen eines Punkts — `.baublende__poiform`. */
@Composable
private fun Punktform(
    welt: Welt,
    p: Weltpoi,
    werkbank: Werkbank,
    ziehtAuf: Boolean,
    andererZieht: Boolean,
    beiBeginnen: (Boolean) -> Unit,
    beiBeenden: () -> Unit,
    beiFehler: (String?) -> Unit,
) {
    val bereich = rememberCoroutineScope()
    var betroffene by remember(p.id) { mutableStateOf(p.betroffeneGesetzt?.toString().orEmpty()) }
    var hinweise by remember(p.id) { mutableStateOf(p.hinweise.joinToString("\n")) }
    val gelaendeart = GELAENDEARTEN.firstOrNull { it.first == werkbank.poiGelaendeart } ?: GELAENDEARTEN.first()
    var sendet by remember { mutableStateOf(false) }
    fun aendern(a: WeltPoiAenderung) = bereich.launch { beiFehler(welt.poiAendern(p.id, a)) }
    fun wortFuer(a: String) = GELAENDEARTEN.firstOrNull { it.first == a }?.second ?: a

    Etikett("Gelände", Modifier.padding(top = Abstand.Klein))
    if (ziehtAuf) {
        val ecken = werkbank.ecken
        val noetig = if (werkbank.eckenStrecke) 2 else 3
        Leisesatz(
            if (ecken.size < noetig) "${ecken.size} von mindestens $noetig Punkten — tipp auf die Karte."
            else "${ecken.size} Punkte · " + masswort(ecken.map { it.lat to it.lon }, werkbank.eckenStrecke),
            winzig = true,
        )
        Auswahl("Was für ein Gelände", GELAENDEARTEN, gelaendeart, { it.second }, { werkbank.poiGelaendeart = it.first })
        Umbruchreihe {
            Knopf("Weiter auf der Karte", { werkbank.karteWaehlen(Kartenmodus.Gelaende) }, kompakt = true)
            Knopf("Punkt zurück", { werkbank.ecken = ecken.dropLast(1) }, kompakt = true, aktiv = ecken.isNotEmpty())
            Knopf("Übernehmen", {
                sendet = true
                bereich.launch {
                    val f = welt.poiAendern(
                        p.id,
                        WeltPoiAenderung(
                            gelaende = ecken,
                            gelaendeart = gelaendeart.first.ifEmpty { null },
                            gelaendeartSetzen = true,
                            strecke = werkbank.eckenStrecke,
                        ),
                    )
                    sendet = false
                    beiFehler(f)
                    if (f == null) beiBeenden()
                }
            }, kompakt = true, aktiv = !sendet && ecken.size >= noetig)
            Knopf("Verwerfen", beiBeenden, kompakt = true)
        }
    } else {
        Leisesatz(
            if (p.gelaende.isNotEmpty()) "${p.gelaende.size} Punkte · ${groesse(p)}" +
                (p.gelaendeart?.let { a -> " · " + wortFuer(a) } ?: "") +
                ". Lagen entstehen irgendwo ${if (p.strecke) "darauf" else "darin"} statt immer an derselben Stelle."
            else "Bisher ein Punkt: Jede Lage liegt an derselben Adresse. Zeichne eine Fläche, wenn es ein " +
                "Werksgelände oder ein Waldstück ist — oder eine Strecke, wenn es eine Bahnlinie oder ein " +
                "Autobahnabschnitt ist.",
            winzig = true,
        )
        Umbruchreihe {
            Knopf("Fläche zeichnen", {
                werkbank.poiGelaendeart = p.gelaendeart ?: ""; beiBeginnen(false)
            }, kompakt = true, aktiv = !andererZieht)
            Knopf("Strecke zeichnen", {
                werkbank.poiGelaendeart = p.gelaendeart ?: ""; beiBeginnen(true)
            }, kompakt = true, aktiv = !andererZieht)
            if (p.gelaende.isNotEmpty()) {
                Knopf("Zurücknehmen", { aendern(WeltPoiAenderung(gelaende = emptyList())) }, kompakt = true)
            }
        }
    }

    Etikett("Häufigkeit · ${p.gewicht}", Modifier.padding(top = Abstand.Klein))
    Segment(listOf(1, 2, 3, 4, 5), p.gewicht, { aendern(WeltPoiAenderung(gewicht = it)) })
    Leisesatz(
        "Wie oft hier etwas ist, im Verhältnis zu deinen anderen Punkten. 1 ist der Regelfall, 5 heißt fünfmal so oft.",
        winzig = true,
    )
    val gelaendeartDesPunkts = p.gelaendeart
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
        Feld(
            wert = betroffene,
            beiAenderung = { betroffene = it.filter(Char::isDigit).take(4) },
            etikett = if (gelaendeartDesPunkts == null) "Personen im Objekt" else "Menschen ständig vor Ort",
            platzhalter = if (gelaendeartDesPunkts == null) "aus der Fläche: ${p.betroffene}" else "niemand",
            tastatur = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        Knopf(
            "Setzen",
            { aendern(WeltPoiAenderung(betroffene = betroffene.toIntOrNull()?.coerceIn(0, 5000) ?: -1)) },
            kompakt = true,
        )
    }
    if (gelaendeartDesPunkts == null) {
        Leisesatz(
            "Leer lassen heißt: aus der Grundfläche gerechnet. Wer sein Heim kennt, muss die Bettenzahl nicht " +
                "über Quadratmeter erraten.",
            winzig = true,
        )
        Schalterzeile(
            "Nachts belegt",
            p.nachtsBelegt,
            { aendern(WeltPoiAenderung(nachtsBelegt = it)) },
            unterzeile = "Aus heißt: zwischen 22 und 6 Uhr ist niemand darin. Eine Schule um drei Uhr morgens ist " +
                "dasselbe Gebäude, aber eine andere Lage.",
        )
    } else {
        Leisesatz(
            "Auf ${wortFuer(gelaendeartDesPunkts).lowercase()} wohnt niemand: Wer dort zu Schaden kommt, kommt aus " +
                "der Lage selbst — die Insassen des Fahrzeugs, die Reisenden des Zuges, der vermisste " +
                "Spaziergänger. Leer lassen ist deshalb der Regelfall. Trag nur etwas ein, wenn dort wirklich " +
                "ständig Menschen sind: ein Zeltplatz im Wald, eine Baustelle an der Strecke.",
            winzig = true,
        )
    }
    Etikett("Lagen für", Modifier.padding(top = Abstand.Klein))
    Umbruchreihe {
        ORGANISATIONEN.forEach { (id, wort) ->
            val an = id in p.organisationen
            Hakenzeile(wort, an, { neu ->
                aendern(WeltPoiAenderung(organisationen = if (neu) p.organisationen + id else p.organisationen - id))
            })
        }
    }
    Leisesatz(
        "Nichts angekreuzt heißt alle. Das Altenheim für den Rettungsdienst, das Lager für die Feuerwehr — so " +
            "bleibt die Absicht erhalten, mit der du den Punkt gesetzt hast.",
        winzig = true,
    )
    Feld(
        wert = hinweise,
        beiAenderung = { hinweise = it.take(600) },
        etikett = "Einsatzplan",
        platzhalter = "Eine Zeile je Hinweis — Zufahrt, Schlüsseldepot, Besonderheiten",
        einzeilig = false,
    )
    // Das Web speichert beim Verlassen des Felds; hier sagt es ein Knopf, weil
    // das Zuklappen der Tastatur kein Verlassen ist. Er steht nur da, wenn es
    // etwas zu speichern gibt.
    if (hinweise != p.hinweise.joinToString("\n")) {
        Knopf("Einsatzplan speichern", { aendern(WeltPoiAenderung(hinweise = hinweise)) }, kompakt = true)
    }
    Leisesatz(
        "Was die Besatzung auf der Anfahrt liest. Leer heißt: die allgemeinen Hinweise " +
            (if (gelaendeartDesPunkts != null) "dieser Geländeart — Zufahrt, Wasser, Sperrung" else "dieser Objektart") + ".",
        winzig = true,
    )
    Markenfarbe(p) { aendern(WeltPoiAenderung(farbe = it)) }
}

/**
 * Die Farbe der Marke — „Farbe der Marke“ in `BauBlende.vue`.
 *
 * <b>0 ist automatisch:</b> Dann entscheidet die Kennung des Punkts, und zwei
 * Punkte nebeneinander sehen trotzdem verschieden aus (`wappenton`). Das Web
 * nimmt eine Zahl von 0 bis 20; hier stehen die Töne selbst zur Wahl, denn
 * über 13 hinaus wiederholt sich die Palette nur. Jeder Ton ist eine 44er-Fläche.
 */
@Composable
private fun Markenfarbe(p: Weltpoi, beiWahl: (Int) -> Unit) {
    Etikett("Farbe der Marke", Modifier.padding(top = Abstand.Klein))
    Umbruchreihe {
        (0..Wappen.PALETTE.size).forEach { nummer ->
            val gewaehlt = p.farbe == nummer || (nummer == 0 && p.farbe !in 0..Wappen.PALETTE.size)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClickLabel = if (nummer == 0) "Automatisch" else "Farbe $nummer") { beiWahl(nummer) },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            if (nummer == 0) Farben.FlaecheHoch else Wappen.ton(p.id, nummer),
                            androidx.compose.foundation.shape.CircleShape,
                        )
                        .border(
                            2.dp,
                            if (gewaehlt) Farben.Text else Farben.Rand,
                            androidx.compose.foundation.shape.CircleShape,
                        ),
                ) {
                    if (nummer == 0) Text("A", style = Schrift.Winzig, color = Wappen.ton(p.id, 0))
                }
            }
        }
    }
    Leisesatz("A ist automatisch. Damit lassen sich mehrere eigene Punkte auf der Karte auseinanderhalten.", winzig = true)
}
