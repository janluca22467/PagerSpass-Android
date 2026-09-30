package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import de.pagerspass.pagerspass.netz.WeltBauart
import de.pagerspass.pagerspass.netz.WeltPoiAenderung
import de.pagerspass.pagerspass.netz.Weltpoi
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
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
 * Bauen — das Gegenstück zum Wache-Reiter von `BauBlende.vue`.
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
    when (werkbank.bauVorgang) {
        "wache" -> Wachenbau(welt, zustand, werkbank)
        "zweig" -> Zweigstellenbau(welt, zustand, werkbank)
        "poi" -> Punkte(welt, zustand, werkbank)
        else -> Bauuebersicht(zustand, werkbank)
    }
}

@Composable
private fun Bauuebersicht(zustand: Weltzustand, werkbank: Werkbank) {
    val bauarten = zustand.stand?.bauarten.orEmpty()
    if (zustand.stand?.wachen.isNullOrEmpty()) {
        Weltkasten(randfarbe = Farben.AmberTief) {
            Text("Deine erste Wache", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
            Leisesatz(
                "Ohne Wache keine Fahrzeuge, ohne Fahrzeuge keine Lagen. Bau ein Feuerwehrhaus in deinem " +
                    "Baugebiet, kauf ein Löschfahrzeug dazu — dann entstehen rund um die Wache die ersten Einsätze.",
            )
        }
    }
    Weg(
        titel = "Wache bauen",
        text = "Ab ${zahl(bauarten.minOfOrNull { it.preis } ?: 0)} Credits — der Standort kommt danach per Tipp auf die Karte.",
        knopf = "Bauen",
        haupt = true,
    ) { werkbank.bauVorgang = "wache" }
    Weg(
        titel = "Fahrzeug kaufen",
        text = "Für eine deiner Wachen — was dort stehen darf, zeigt die Liste.",
        knopf = "Kaufen",
    ) { werkbank.seite = Werkzeug.Fahrzeugkauf }
    Weg(
        titel = "Eigenen Punkt setzen",
        text = "Eine Schule, ein Heim, ein Betrieb — dort entstehen passende Einsätze.",
        knopf = "Punkte",
    ) { werkbank.bauVorgang = "poi" }
    val zweig = zustand.stand?.zweigstelle
    if (zweig != null && !(zweig.hoechstens > 0 && zweig.schon >= zweig.hoechstens)) {
        Weg(
            titel = "${zweig.schon + 2}. Leitstelle gründen",
            text = if (zweig.frei) "${zahl(zweig.preis)} Credits."
            else "Öffnet ab Stufe ${zweig.abStufe} — was dann geht, steht hinter dem Knopf.",
            knopf = "Ansehen",
        ) { werkbank.bauVorgang = "zweig" }
    }
}

@Composable
private fun Weg(titel: String, text: String, knopf: String, haupt: Boolean = false, beiDruck: () -> Unit) {
    Weltkasten {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            Column(Modifier.weight(1f)) {
                Text(titel, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                Text(text, style = Schrift.Winzig, color = Farben.TextSehrLeise)
            }
            Knopf(knopf, beiDruck, kompakt = true, art = if (haupt) Knopfart.Haupt else Knopfart.Normal)
        }
    }
}

/** „Standort: gesetzt / wählen“ — die Brücke zur Karte (`Bauortwahl.vue`). */
@Composable
private fun Bauortwahl(werkbank: Werkbank, zweck: String, wort: String = "Standort") {
    val ort = werkbank.bauort
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Text(
            ort?.let { "$wort gesetzt: %.4f, %.4f".format(it.lat, it.lon) } ?: "Noch kein $wort gewählt.",
            style = Schrift.Klein,
            color = if (ort != null) Farben.AmberHell else Farben.TextLeise,
            modifier = Modifier.weight(1f),
        )
        Knopf(if (ort == null) "Auf der Karte wählen" else "Ändern", {
            werkbank.ortZweck = zweck
            werkbank.karteWaehlen(Kartenmodus.Ort)
        }, kompakt = true)
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
            else if (b.art == "Werkstatt") "Hebebühnen" else "Plätze"
            "${zahl(b.preis)} Credits · ${b.stellplaetze} $plaetze" + if (b.frei) "" else " · gesperrt"
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

    Leisesatz(
        "Eine weitere Leitstelle (die Zweigstelle): ein eigener Ausrückebereich mit eigenem Namen, eigenem " +
            "Landkreis für die Straßennamen dort und einem eigenen Bauradius. Sie muss mindestens $km km von " +
            "jedem deiner Bereiche entfernt liegen. Sobald dort Wachen stehen, entstehen dort auch Einsätze. " +
            "Guthaben, Erfahrung und Stufe bleiben gemeinsam." +
            if ((z?.hoechstens ?: 0) > 0) " Auf deiner Stufe sind ${z?.hoechstens} erlaubt, du hast ${z?.schon ?: 0}." else "",
        winzig = true,
    )
    if (z?.frei != true) {
        Text(
            "Ab Stufe ${z?.abStufe ?: 20}: Du bist auf Stufe ${zustand.stufe} — mit Stufe ${z?.abStufe ?: 20} kannst du hier " +
                "für ${zahl(z?.preis ?: 0)} Welt-Credits den nächsten Ausrückebereich gründen.",
            style = Schrift.Klein,
            color = Farben.Amber,
        )
    } else {
        Feld(wert = name, beiAenderung = { werkbank.zweigName = it.take(60) }, etikett = "Name", platzhalter = "Name des zweiten Bereichs")
        Bauortwahl(werkbank, "zweig")
        Warnsatz(fehler)
        Knopf(
            if (sendet) "Wird gegründet …" else "Gründen für ${credits(z.preis)}",
            {
                val ort = werkbank.bauort ?: return@Knopf
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
    }
    Knopf("Zurück", { werkbank.bauVorgang = "keiner" }, art = Knopfart.Leise, kompakt = true)
}

/**
 * Eigene Punkte — Punkt, Fläche oder Strecke. Die Liste steht unter dem
 * Formular: Wer einen Punkt stilllegen will, sucht ihn dort, wo er ihn angelegt
 * hat.
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
    val ecken = werkbank.ecken.map { it.lat to it.lon }

    Text("Was du setzt", style = Schrift.Klein, color = Farben.TextLeise)
    Segment(listOf("Punkt", "Fläche", "Strecke"), form, { werkbank.poiForm = it; werkbank.ecken = emptyList() })
    Leisesatz(
        when (form) {
            "Punkt" -> "Ein Gebäude an einer Stelle — jede Lage entsteht genau dort."
            "Fläche" -> "Ein Gelände, das du umfährst: Werk, Wald, Feld. Lagen entstehen irgendwo darin."
            else -> "Etwas Langes und Schmales: Autobahn, Bahnstrecke, Fluss. Lagen entstehen irgendwo darauf."
        },
        winzig = true,
    )
    Feld(wert = name, beiAenderung = { werkbank.poiName = it.take(60) }, etikett = "Name", platzhalter = "Grundschule am Park")
    if (zeichnet) {
        Auswahl("Was für ein Gelände", GELAENDEARTEN, gelaendeart, { it.second }, { werkbank.poiGelaendeart = it.first })
    }
    if (!zeichnet || gelaendeart.first.isEmpty()) {
        Auswahl("Art des Gebäudes", POI_ARTEN, art, { it.second }, { werkbank.poiArt = it.first })
    }
    if (!zeichnet) {
        Zahlfeld("Grundfläche in m²", flaeche, { werkbank.poiFlaeche = it }, groesstes = 50_000)
        Leisesatz("Sie entscheidet, wie viele Menschen darin sind — und damit, wie viel ein Brand dort fordert.", winzig = true)
        Bauortwahl(werkbank, "poi", "Ort")
    } else if (gelaendeFuer == null) {
        Text(if (strecke) "Verlauf" else "Umriss", style = Schrift.Klein, color = Farben.TextLeise)
        Leisesatz(
            if (ecken.size < noetig) "${ecken.size} von mindestens $noetig Punkten."
            else "${ecken.size} Punkte · " + if (strecke) laengenwort(zuglaengeMeter(ecken)) else flaechenwort(vieleckQuadratmeter(ecken)),
            winzig = true,
        )
        Umbruchreihe {
            Knopf(if (ecken.isEmpty()) "Auf der Karte zeichnen" else "Weiter zeichnen", {
                werkbank.eckenStrecke = strecke
                werkbank.karteWaehlen(Kartenmodus.Gelaende)
            }, kompakt = true)
            if (ecken.isNotEmpty()) Knopf("Verwerfen", { werkbank.ecken = emptyList() }, kompakt = true, art = Knopfart.Leise)
        }
    }
    Warnsatz(fehler)
    Knopf(
        if (sendet) "Wird gesetzt …" else "$form setzen",
        {
            if (name.isBlank()) { fehler = "Der Punkt braucht einen Namen."; return@Knopf }
            val gelaende = if (zeichnet) werkbank.ecken else null
            if (gelaende != null && gelaende.size < noetig) {
                fehler = if (strecke) "Eine Strecke braucht mindestens zwei Punkte." else "Eine Fläche braucht mindestens drei Ecken."
                return@Knopf
            }
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
                    werkbank.ecken = emptyList()
                    werkbank.bauort = null
                }
            }
        },
        art = Knopfart.Haupt,
        breit = true,
        aktiv = !sendet && gelaendeFuer == null,
    )
    Knopf("Zurück", { werkbank.bauVorgang = "keiner"; werkbank.ecken = emptyList() }, art = Knopfart.Leise, kompakt = true)

    if (zustand.pois.isNotEmpty()) Ueberschrift("Deine Punkte")
    zustand.pois.forEach { p ->
        Weltkasten(an = offenerPunkt == p.id) {
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
                }, kompakt = true, art = Knopfart.Leise)
                Knopf(if (p.aktiv) "Stilllegen" else "Anschalten", {
                    bereich.launch { fehler = welt.poiAendern(p.id, WeltPoiAenderung(aktiv = !p.aktiv)) }
                }, kompakt = true, art = Knopfart.Leise)
                Knopf(if (offenerPunkt == p.id) "Zu" else "Mehr", {
                    werkbank.offenerPunkt = if (offenerPunkt == p.id) null else p.id
                }, kompakt = true, art = Knopfart.Leise)
                Knopf("Entfernen", { bereich.launch { fehler = welt.poiEntfernen(p.id) } }, kompakt = true, art = Knopfart.Leise)
            }
            if (offenerPunkt == p.id) {
                Punktform(welt, p, werkbank, gelaendeFuer == p.id, { werkbank.gelaendeFuer = it }) { fehler = it }
            }
        }
    }
}

private fun groesse(p: Weltpoi): String =
    if (p.strecke) laengenwort(p.laenge) else flaechenwort(vieleckQuadratmeter(p.gelaende.map { it.lat to it.lon }))

/** Die Einstellungen eines Punkts — Gelände, Häufigkeit, Personen, Nacht, Einsatzplan, Organisationen. */
@Composable
private fun Punktform(
    welt: Welt,
    p: Weltpoi,
    werkbank: Werkbank,
    ziehtAuf: Boolean,
    beiAufziehen: (String?) -> Unit,
    beiFehler: (String?) -> Unit,
) {
    val bereich = rememberCoroutineScope()
    var betroffene by remember(p.id) { mutableStateOf(p.betroffeneGesetzt?.toString().orEmpty()) }
    var hinweise by remember(p.id) { mutableStateOf(p.hinweise.joinToString("\n")) }
    val gelaendeart = GELAENDEARTEN.firstOrNull { it.first == werkbank.poiGelaendeart } ?: GELAENDEARTEN.first()
    fun aendern(a: WeltPoiAenderung) = bereich.launch { beiFehler(welt.poiAendern(p.id, a)) }

    Text("Gelände", style = Schrift.Klein, color = Farben.TextLeise)
    if (ziehtAuf) {
        val ecken = werkbank.ecken
        val noetig = if (werkbank.eckenStrecke) 2 else 3
        Leisesatz("${ecken.size} von mindestens $noetig Punkten.", winzig = true)
        Auswahl("Was für ein Gelände", GELAENDEARTEN, gelaendeart, { it.second }, { werkbank.poiGelaendeart = it.first })
        Umbruchreihe {
            Knopf("Auf der Karte zeichnen", { werkbank.karteWaehlen(Kartenmodus.Gelaende) }, kompakt = true)
            Knopf("Übernehmen", {
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
                    beiFehler(f)
                    if (f == null) {
                        werkbank.ecken = emptyList()
                        beiAufziehen(null)
                    }
                }
            }, kompakt = true, aktiv = ecken.size >= noetig)
            Knopf("Abbrechen", { werkbank.ecken = emptyList(); beiAufziehen(null) }, kompakt = true, art = Knopfart.Leise)
        }
    } else {
        Leisesatz(
            if (p.gelaende.isNotEmpty()) "${p.gelaende.size} Punkte · ${groesse(p)}" +
                (p.gelaendeart?.let { a -> " · " + (GELAENDEARTEN.firstOrNull { it.first == a }?.second ?: a) } ?: "") +
                ". Lagen entstehen irgendwo ${if (p.strecke) "darauf" else "darin"} statt immer an derselben Stelle."
            else "Ein Punkt — jede Lage entsteht genau hier.",
            winzig = true,
        )
        Umbruchreihe {
            Knopf("Fläche ziehen", {
                werkbank.ecken = emptyList(); werkbank.eckenStrecke = false
                werkbank.poiGelaendeart = p.gelaendeart ?: ""; beiAufziehen(p.id)
            }, kompakt = true, art = Knopfart.Leise)
            Knopf("Strecke ziehen", {
                werkbank.ecken = emptyList(); werkbank.eckenStrecke = true
                werkbank.poiGelaendeart = p.gelaendeart ?: ""; beiAufziehen(p.id)
            }, kompakt = true, art = Knopfart.Leise)
            if (p.gelaende.isNotEmpty()) {
                Knopf("Zum Punkt machen", { aendern(WeltPoiAenderung(gelaende = emptyList())) }, kompakt = true, art = Knopfart.Leise)
            }
        }
    }

    Text("Häufigkeit", style = Schrift.Klein, color = Farben.TextLeise)
    Segment(listOf(1, 2, 3, 4, 5), p.gewicht, { aendern(WeltPoiAenderung(gewicht = it)) })
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
        Feld(
            wert = betroffene,
            beiAenderung = { betroffene = it.filter(Char::isDigit).take(6) },
            etikett = "Personen (leer = aus der Fläche)",
            tastatur = androidx.compose.ui.text.input.KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        Knopf("Setzen", { aendern(WeltPoiAenderung(betroffene = betroffene.toIntOrNull() ?: -1)) }, kompakt = true)
    }
    Schalterzeile("Nachts belegt (22–6 Uhr)", p.nachtsBelegt, { aendern(WeltPoiAenderung(nachtsBelegt = it)) })
    Feld(wert = hinweise, beiAenderung = { hinweise = it.take(600) }, etikett = "Einsatzplan (eine Zeile je Hinweis)", einzeilig = false)
    Knopf("Einsatzplan speichern", { aendern(WeltPoiAenderung(hinweise = hinweise)) }, kompakt = true, art = Knopfart.Leise)
    Text("Organisationen (keine gewählt heißt alle)", style = Schrift.Klein, color = Farben.TextLeise)
    Umbruchreihe {
        ORGANISATIONEN.forEach { (id, wort) ->
            val an = id in p.organisationen
            Pille(wort, an, {
                aendern(WeltPoiAenderung(organisationen = if (an) p.organisationen - id else p.organisationen + id))
            })
        }
    }
}
