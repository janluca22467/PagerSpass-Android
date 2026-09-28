package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.WeltEcke
import de.pagerspass.pagerspass.netz.WeltPoiAenderung
import de.pagerspass.pagerspass.netz.Weltpoi
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Welcher Bauvorgang läuft und was im Entwurf steht — über das Schließen der Seite hinaus.
 *
 * <b>Am Handy schließt „Standort auf der Karte wählen" die Seite</b>, und nach dem Tipp
 * auf die Karte kommt sie neu. Läge das hier in der Seite, stünde man danach vor einem
 * leeren Namensfeld, und ein angefangenes Gelände liefe ins Leere (dieselben Modulgrößen
 * wie oben in `BauBlende.vue`).
 */
private object Bausitzung {
    /** `keiner`, `wache`, `zweig` oder `poi`. */
    var vorgang by mutableStateOf("keiner")
    var name by mutableStateOf("")
    var art by mutableStateOf("Feuerwehrhaus")
    var zweigName by mutableStateOf("")
    var poiName by mutableStateOf("")
    var poiArt by mutableStateOf("Schule")
    var poiFlaeche by mutableIntStateOf(1200)

    /** `punkt`, `flaeche` oder `strecke`. */
    var poiForm by mutableStateOf("punkt")

    /** Leer heißt: Grundstück eines Gebäudes. */
    var poiGelaendeart by mutableStateOf("")
    var ziehtAuf by mutableStateOf(false)
    var zeichnetStrecke by mutableStateOf(false)
    var gelaendeFuer by mutableStateOf<String?>(null)
    var offenerPunkt by mutableStateOf<String?>(null)
}

/** Die Gebäudearten eigener Punkte, in der Reihenfolge des Katalogs. */
private val POI_ARTEN = listOf(
    "Schule" to "Schule",
    "Kindergarten" to "Kindertagesstätte",
    "Pflegeheim" to "Pflegeheim",
    "Arztpraxis" to "Arztpraxis",
    "Beherbergung" to "Beherbergung",
    "Verkaufsstaette" to "Verkaufsstätte",
    "Industrie" to "Industrie",
    "Versammlungsstaette" to "Versammlungsstätte",
    "Bahnhof" to "Bahnhof",
    "Sportstaette" to "Sportstätte",
    "Hochhaus" to "Hochhaus",
    "Tankstelle" to "Tankstelle",
    "Landwirtschaft" to "Landwirtschaftlicher Betrieb",
    "Campingplatz" to "Campingplatz",
    "Parkhaus" to "Parkhaus",
    "Energieanlage" to "Energieanlage",
)

/** Punkt, Fläche oder Strecke. */
private val POI_FORMEN = listOf("punkt" to "Punkt", "flaeche" to "Fläche", "strecke" to "Strecke")

/** Die Geländearten, wie der Abzug sie kennt. */
private val GELAENDEARTEN = listOf(
    "Wald" to "Wald",
    "Feld" to "Feld oder Wiese",
    "Autobahn" to "Autobahn",
    "Schiene" to "Bahnstrecke",
    "Gewaesser" to "Gewässer",
)

private val POI_ORGANISATIONEN = listOf(
    "Feuerwehr" to "Feuerwehr",
    "Rettungsdienst" to "Rettungsdienst",
    "Polizei" to "Polizei",
    "Thw" to "THW",
)

private fun gelaendewort(art: String): String = GELAENDEARTEN.firstOrNull { it.first == art }?.second ?: art

/** Wie groß ein gesetztes Gelände ist — aus den Ecken, nicht aus der gedeckelten Fläche. */
private fun groesse(p: Weltpoi): String =
    if (p.strecke) laengenwort(p.laenge) else flaechenwort(vieleckQuadratmeter(p.gelaende.map { it.lat to it.lon }))

private fun koordinate(ort: Pair<Double, Double>): String =
    String.format(Locale.US, "%.4f, %.4f", ort.first, ort.second)

/**
 * Bauen und kaufen — übertragen aus `components/welt/BauBlende.vue`.
 *
 * Am Handy sind es zwei Einstiege: „Bauen" (Wache, eigene Punkte, weitere Leitstelle) und
 * „Fahrzeug kaufen" — `nurFahrzeug` sagt, welcher. <b>Das Formular kommt erst mit dem
 * Entschluss:</b> Die Übersicht nennt jeden Weg mit Preis, und erst der Knopf schaltet
 * die Karte in den Baumodus.
 */
@Composable
fun WeltBauBlende(
    welt: Welt,
    stand: Weltzustand,
    katalog: Katalog?,
    modi: Kartenmodi,
    griffe: Weltgriffe,
    nurFahrzeug: Boolean,
) {
    val s = Bausitzung
    val bereich = rememberCoroutineScope()
    val reiterWache = !nurFahrzeug

    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }

    val ecken = modi.gelaende
    val noetigePunkte = if (s.zeichnetStrecke) 2 else 3
    val ziehtNeues = s.ziehtAuf && s.gelaendeFuer == null
    val masswort = if (s.zeichnetStrecke) laengenwort(zuglaengeMeter(ecken)) else flaechenwort(vieleckQuadratmeter(ecken))
    val bauort = modi.bauort

    /** Wofür die Karte gerade einen Punkt erwartet — `null`: für nichts. */
    val bauzweck: String? = when {
        !reiterWache -> null
        s.ziehtAuf -> null
        s.vorgang == "wache" -> "wache"
        s.vorgang == "zweig" -> "zweig"
        s.vorgang == "poi" && s.poiForm == "punkt" -> "poi"
        else -> null
    }

    fun gelaendeBeenden() {
        s.ziehtAuf = false
        s.gelaendeFuer = null
        griffe.gelaendeModus(false, false)
        griffe.gelaendeSetzen(emptyList())
    }

    // Der Baumodus hängt am begonnenen Vorgang. Läuft ein Aufziehen, meldet es sich beim
    // Wiederkommen der Seite neu an — und wer die Punkte verlässt, zieht nichts mehr auf.
    LaunchedEffect(bauzweck, s.vorgang, reiterWache) {
        griffe.bauModus(bauzweck != null, bauzweck)
        if (s.ziehtAuf && !(reiterWache && s.vorgang == "poi")) {
            gelaendeBeenden()
        } else if (s.ziehtAuf) {
            griffe.gelaendeModus(true, s.zeichnetStrecke)
        }
    }

    fun vorgangAbbrechen() {
        if (s.ziehtAuf) gelaendeBeenden()
        s.vorgang = "keiner"
        fehler = null
    }

    fun gelaendeBeginnen(fuer: String?, alsStrecke: Boolean?) {
        s.gelaendeFuer = fuer
        s.ziehtAuf = true
        fehler = null
        val punkt = fuer?.let { id -> stand.pois.firstOrNull { it.id == id } }
        if (punkt != null) s.poiGelaendeart = punkt.gelaendeart ?: ""
        s.zeichnetStrecke = alsStrecke ?: punkt?.strecke ?: (s.poiForm == "strecke")
        griffe.gelaendeSetzen(emptyList())
        griffe.gelaendeModus(true, s.zeichnetStrecke)
        // Die Seite liegt über der Karte, auf die jetzt getippt werden soll.
        griffe.karteFreigeben()
    }

    fun poiAendern(id: String, aenderung: WeltPoiAenderung) {
        bereich.launch { fehler = welt.poiAendern(id, aenderung) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        if (nurFahrzeug) {
            Fahrzeugkauf(welt, stand, katalog)
            return@Column
        }

        val zweigstand = stand.stand?.zweigstelle
        val alleZweigstellen = zweigstand != null && zweigstand.hoechstens > 0 && zweigstand.schon >= zweigstand.hoechstens
        val zweigMoeglich = zweigstand != null && !alleZweigstellen

        // Fällt die Zweigstelle weg, während ihr Formular offen steht: zurück zur Übersicht.
        LaunchedEffect(zweigMoeglich) {
            if (!zweigMoeglich && s.vorgang == "zweig") s.vorgang = "keiner"
        }

        when (s.vorgang) {
            // ------------------------------------------------------- Übersicht
            "keiner" -> {
                val bauarten = stand.stand?.bauarten.orEmpty()
                Bauweg(
                    titel = "Wache bauen",
                    text = "Ab ${zahl(bauarten.firstOrNull()?.preis ?: 0)} Credits — der Standort kommt danach per Tipp auf die Karte.",
                    knopf = "Bauen",
                    haupt = true,
                    beiDruck = { s.vorgang = "wache" },
                )
                Bauweg(
                    titel = "Eigenen Punkt setzen",
                    text = "Eine Schule, ein Heim, ein Betrieb — dort entstehen passende Einsätze. Kostenlos, höchstens 40 Stück.",
                    knopf = "Punkte",
                    beiDruck = { s.vorgang = "poi" },
                )
                if (zweigMoeglich) {
                    Bauweg(
                        titel = "${(zweigstand?.schon ?: 0) + 2}. Leitstelle gründen",
                        text = if (zweigstand != null && zweigstand.frei) {
                            "Ein weiterer Ausrückebereich für ${zahl(zweigstand.preis)} Credits."
                        } else {
                            "Öffnet ab Stufe ${zweigstand?.abStufe ?: 20} — was dann geht, steht hinter dem Knopf."
                        },
                        knopf = "Ansehen",
                        beiDruck = { s.vorgang = "zweig" },
                    )
                }
            }

            // ------------------------------------------------------- Wache
            "wache" -> {
                val bauarten = stand.stand?.bauarten.orEmpty()
                val gewaehlteArt = bauarten.firstOrNull { it.art == s.art }
                Feld(
                    wert = s.name,
                    beiAenderung = { s.name = it.take(60) },
                    etikett = "Name",
                    platzhalter = "Feuerwache Mitte",
                )
                Auswahlfeld(
                    platzhalter = "Bauart",
                    wahlen = bauarten.map { b ->
                        val einheit = if (b.art == "Lehrgangseinrichtung") {
                            if (b.stellplaetze == 1) "Lehrsaal" else "Lehrsäle"
                        } else {
                            "Plätze"
                        }
                        Wahl(
                            b.art,
                            "${b.name} · ${zahl(b.preis)} Credits · ${b.stellplaetze} $einheit" + if (b.frei) "" else " (gesperrt)",
                            aktiv = b.frei,
                        )
                    },
                    gewaehlt = s.art,
                    beiWahl = { s.art = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Bauortwahl(bauort, beiWaehlen = griffe.ortWaehlen)
                Absage(fehler)
                Knopf(
                    aufschrift = if (sendet) "Wird gebaut …" else "Bauen für ${zahl(gewaehlteArt?.preis ?: 0)} Credits",
                    beiDruck = {
                        val ort = bauort
                        when {
                            ort == null -> fehler = "Setz zuerst den Standort mit einem Tipp auf die Karte."
                            s.name.trim().length < 3 -> fehler = "Der Name braucht mindestens drei Zeichen."
                            !sendet -> {
                                sendet = true
                                fehler = null
                                val name = s.name.trim()
                                val art = s.art
                                bereich.launch {
                                    fehler = welt.aktion("Der Bau ging nicht.", Welt.Nachladen.Beides) {
                                        welt.wege.wacheBauen(it, name, art, ort.first, ort.second)
                                    }
                                    if (fehler == null) s.name = ""
                                    sendet = false
                                }
                            }
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = !sendet && gewaehlteArt?.frei == true && bauort != null,
                    breit = true,
                )
                Knopf("Abbrechen", { vorgangAbbrechen() }, art = Knopfart.Leise)
            }

            // ------------------------------------------------------- Zweigstelle
            "zweig" -> {
                val z = zweigstand
                val abstandKm = ((z?.mindestabstandMeter ?: 35_000.0) / 1000.0).roundToInt()
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
                ) {
                    Text(
                        text = "Eine weitere Leitstelle (die Zweigstelle): ein eigener Ausrückebereich mit eigenem " +
                            "Namen, eigenem Landkreis für die Straßennamen dort und einem eigenen Bauradius. Sie muss " +
                            "mindestens $abstandKm km von jedem deiner Bereiche entfernt liegen. Sobald dort Wachen " +
                            "stehen, entstehen dort auch Einsätze. Guthaben, Erfahrung und Stufe bleiben gemeinsam." +
                            if ((z?.hoechstens ?: 0) > 0) " Auf deiner Stufe sind ${z?.hoechstens} erlaubt, du hast ${z?.schon ?: 0}." else "",
                        style = Schrift.Winzig,
                        color = Farben.TextSehrLeise,
                    )
                    if (z == null || !z.frei) {
                        Text(
                            text = "Ab Stufe ${z?.abStufe ?: 20}: Du bist auf Stufe ${stand.stufe} — mit Stufe " +
                                "${z?.abStufe ?: 20} kannst du hier für ${zahl(z?.preis ?: 0)} Welt-Credits den nächsten " +
                                "Ausrückebereich gründen.",
                            style = Schrift.Klein,
                            color = Farben.TextLeise,
                        )
                    } else {
                        Feld(
                            wert = s.zweigName,
                            beiAenderung = { s.zweigName = it.take(60) },
                            platzhalter = "Name des zweiten Bereichs",
                        )
                        Bauortwahl(bauort, beiWaehlen = griffe.ortWaehlen)
                        Absage(fehler)
                        Knopf(
                            aufschrift = "Gründen für ${zahl(z.preis)} Credits",
                            beiDruck = {
                                val ort = bauort
                                if (ort == null) {
                                    fehler = "Setz zuerst den Standort mit einem Tipp auf die Karte."
                                } else if (!sendet) {
                                    sendet = true
                                    fehler = null
                                    val name = s.zweigName.trim()
                                    bereich.launch {
                                        fehler = welt.aktion("Die Gründung ging nicht.", Welt.Nachladen.Beides) {
                                            welt.wege.zweigstelleGruenden(it, name, ort.first, ort.second)
                                        }
                                        if (fehler == null) s.zweigName = ""
                                        sendet = false
                                    }
                                }
                            },
                            art = Knopfart.Haupt,
                            aktiv = !sendet && bauort != null && s.zweigName.trim().length >= 3,
                            breit = true,
                        )
                        if (bauort == null) {
                            Text(
                                text = "Der Standort kommt vom selben Tipp auf die Karte wie beim Wachenbau — er muss " +
                                    "mindestens $abstandKm km von jedem deiner Bereiche entfernt liegen.",
                                style = Schrift.Winzig,
                                color = Farben.TextSehrLeise,
                            )
                        }
                    }
                    Knopf("Zurück", { vorgangAbbrechen() }, art = Knopfart.Leise)
                }
            }

            // ------------------------------------------------------- Eigene Punkte
            "poi" -> {
                val wirdGezeichnet = s.poiForm != "punkt"
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
                ) {
                    Etikettzeile("Was du setzt")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        POI_FORMEN.forEach { (id, wort) ->
                            Pille(aufschrift = wort, an = s.poiForm == id, beiDruck = { s.poiForm = id }, aktiv = !s.ziehtAuf)
                        }
                    }
                    Leisetext(
                        when (s.poiForm) {
                            "punkt" -> "Eine Adresse: Jede Lage liegt an derselben Stelle, mit derselben Anfahrt."
                            "flaeche" -> "Eine Fläche, die du umfährst — Werksgelände, Waldstück, Bahnhofsvorfeld. Die Lage " +
                                "entsteht irgendwo darin, mal vorn an der Pforte und mal hinten am Lager."
                            else -> "Eine Linie, die du abfährst — Bahnstrecke, Autobahnabschnitt, Flussufer. Die Lage " +
                                "entsteht irgendwo darauf. Als Fläche müsstest du die Gleise hin und zurück umfahren."
                        },
                    )

                    Feld(
                        wert = s.poiName,
                        beiAenderung = { s.poiName = it.take(60) },
                        etikett = "Name",
                        platzhalter = "Grundschule am Park",
                    )

                    if (wirdGezeichnet) {
                        Etikettzeile("Was für ein Gelände")
                        Auswahlfeld(
                            platzhalter = "Grundstück eines Gebäudes",
                            wahlen = listOf(Wahl("", "Grundstück eines Gebäudes")) + GELAENDEARTEN.map { Wahl(it.first, it.second) },
                            gewaehlt = s.poiGelaendeart,
                            beiWahl = { s.poiGelaendeart = it },
                            aktiv = !s.ziehtAuf,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Leisetext(
                            "Ein Grundstück bekommt die Lagen seiner Gebäudeart — Brandmeldeanlage, Menschen im Gebäude. " +
                                "Wald, Feld, Autobahn, Bahnstrecke und Gewässer bekommen ihre eigenen: Vegetationsbrand, " +
                                "Vermisstensuche, Verkehrsunfall, Zugunglück, Person im Wasser. Für die meisten Landkreise " +
                                "fehlen diese Orte noch ganz — ein selbst gezogenes Waldstück ist dort der einzige Wald, " +
                                "den deine Welt hat.",
                        )
                        if (s.poiGelaendeart == "Gewaesser") {
                            Leisetext(
                                "Beim Gewässer sagt die Form, was es ist: Eine Fläche ist ein See, eine Strecke ein Fluss. " +
                                    "Eine Seelage sucht sich zuerst einen See — gibt es nur Flüsse, nimmt sie einen davon.",
                            )
                        }
                    }

                    if (!wirdGezeichnet || s.poiGelaendeart == "") {
                        Etikettzeile("Art des Gebäudes")
                        Auswahlfeld(
                            platzhalter = "Art des Gebäudes",
                            wahlen = POI_ARTEN.map { Wahl(it.first, it.second) },
                            gewaehlt = s.poiArt,
                            beiWahl = { s.poiArt = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (!wirdGezeichnet) {
                        Feld(
                            wert = s.poiFlaeche.toString(),
                            beiAenderung = { t -> t.filter { it.isDigit() }.take(5).toIntOrNull()?.let { s.poiFlaeche = it.coerceAtMost(50_000) } ?: run { if (t.isEmpty()) s.poiFlaeche = 0 } },
                            etikett = "Grundfläche in m²",
                            tastatur = KeyboardType.Number,
                        )
                        Leisetext("Aus ihr folgt, wie viele Menschen bei einem Schadenereignis darin sind — und damit, wie viel ein Brand dort fordert.")
                        Bauortwahl(bauort, wort = "Ort", beiWaehlen = griffe.ortWaehlen)
                    } else {
                        Etikettzeile(if (s.poiForm == "strecke") "Verlauf" else "Umriss")
                        Leisetext(
                            when {
                                !ziehtNeues -> "Noch nichts gezeichnet. " +
                                    if (s.poiForm == "strecke") "Zwei Punkte sind das Mindeste, zwölf das Meiste." else "Drei Ecken sind das Mindeste, zwölf das Meiste."
                                ecken.size < noetigePunkte -> "${ecken.size} von mindestens $noetigePunkte Punkten — tipp weiter auf die Karte."
                                else -> "${ecken.size} Punkte · $masswort"
                            },
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            if (!ziehtNeues) {
                                Knopf("Zeichnen", { gelaendeBeginnen(null, null) }, kompakt = true, aktiv = !s.ziehtAuf)
                            } else {
                                Knopf("Weiter auf der Karte", { griffe.karteFreigeben() }, kompakt = true)
                                Knopf("Punkt zurück", { griffe.gelaendeSetzen(ecken.dropLast(1)) }, kompakt = true, aktiv = ecken.isNotEmpty())
                                Knopf("Verwerfen", { gelaendeBeenden() }, kompakt = true)
                            }
                        }
                    }

                    Absage(fehler)

                    Knopf(
                        aufschrift = when {
                            sendet -> "Wird gesetzt …"
                            s.poiForm == "strecke" -> "Strecke setzen"
                            s.poiForm == "flaeche" -> "Fläche setzen"
                            else -> "Punkt setzen"
                        },
                        beiDruck = {
                            if (wirdGezeichnet) {
                                if (ecken.size < noetigePunkte) {
                                    fehler = if (s.zeichnetStrecke) "Eine Strecke braucht mindestens zwei Punkte." else "Eine Fläche braucht mindestens drei Ecken."
                                    return@Knopf
                                }
                            } else if (bauort == null) {
                                fehler = "Setz zuerst den Ort mit einem Tipp auf die Karte."
                                return@Knopf
                            }
                            if (s.poiName.trim().isEmpty()) {
                                fehler = "Der Punkt braucht einen Namen."
                                return@Knopf
                            }
                            if (sendet) return@Knopf
                            sendet = true
                            fehler = null
                            // Beim Gelände sagen die Ecken, wo der Punkt steht — hier geht die erste mit.
                            val gelaende = if (wirdGezeichnet) ecken.map { WeltEcke(it.first, it.second) } else null
                            val sitz = gelaende?.firstOrNull()?.let { it.lat to it.lon } ?: bauort
                            val name = s.poiName.trim()
                            val art = s.poiArt
                            val flaeche = s.poiFlaeche.toDouble()
                            val gelaendeart = if (gelaende != null && s.poiGelaendeart != "") s.poiGelaendeart else null
                            val strecke = s.zeichnetStrecke
                            bereich.launch {
                                val meldung = if (sitz == null) {
                                    "Setz zuerst den Ort mit einem Tipp auf die Karte."
                                } else {
                                    welt.poiSetzen(name, art, sitz.first, sitz.second, flaeche, gelaende, gelaendeart, strecke)
                                }
                                sendet = false
                                if (meldung != null) {
                                    fehler = meldung
                                } else {
                                    s.poiName = ""
                                    if (gelaende != null) gelaendeBeenden()
                                }
                            }
                        },
                        art = Knopfart.Haupt,
                        aktiv = !sendet && if (wirdGezeichnet) ziehtNeues && ecken.size >= noetigePunkte else bauort != null,
                        breit = true,
                    )
                    Knopf("Zurück", { vorgangAbbrechen() }, art = Knopfart.Leise)
                }

                // Die Liste der eigenen Punkte — dort, wo man sie angelegt hat.
                stand.pois.forEach { p ->
                    Punktzeile(
                        p = p,
                        offen = s.offenerPunkt == p.id,
                        ziehtHier = s.ziehtAuf && s.gelaendeFuer == p.id,
                        ziehtAuf = s.ziehtAuf,
                        ecken = ecken,
                        noetigePunkte = noetigePunkte,
                        masswort = masswort,
                        sendet = sendet,
                        beiUmschalten = { s.offenerPunkt = if (s.offenerPunkt == p.id) null else p.id },
                        beiAenderung = { poiAendern(p.id, it) },
                        beiEntfernen = { bereich.launch { fehler = welt.poiEntfernen(p.id) } },
                        beiZeichnen = { strecke -> gelaendeBeginnen(p.id, strecke) },
                        beiKarte = griffe.karteFreigeben,
                        beiEckeZurueck = { griffe.gelaendeSetzen(ecken.dropLast(1)) },
                        beiVerwerfen = { gelaendeBeenden() },
                        beiUebernehmen = {
                            if (!sendet) {
                                sendet = true
                                val aenderung = WeltPoiAenderung(
                                    gelaende = ecken.map { WeltEcke(it.first, it.second) },
                                    gelaendeart = s.poiGelaendeart.ifEmpty { null },
                                    gelaendeartSetzen = true,
                                    strecke = s.zeichnetStrecke,
                                )
                                bereich.launch {
                                    fehler = welt.poiAendern(p.id, aenderung)
                                    sendet = false
                                    if (fehler == null) gelaendeBeenden()
                                }
                            }
                        },
                        gelaendeart = s.poiGelaendeart,
                        beiGelaendeart = { s.poiGelaendeart = it },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Bausteine

/** Ein Weg der Übersicht: Name, Preis in einem Satz, ein Knopf. */
@Composable
private fun Bauweg(titel: String, text: String, knopf: String, beiDruck: () -> Unit, haupt: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
            Text(text = titel, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Text(text = text, style = Schrift.Winzig, color = Farben.TextSehrLeise)
        }
        Knopf(knopf, beiDruck, art = if (haupt) Knopfart.Haupt else Knopfart.Normal, kompakt = true)
    }
}

@Composable
private fun Etikettzeile(text: String) {
    Text(text = text, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.TextLeise)
}

@Composable
private fun Leisetext(text: String) {
    Text(text = text, style = Schrift.Winzig, color = Farben.TextSehrLeise)
}

/**
 * Der Weg zur Karte am Handy — `Bauortwahl.vue`: der gesetzte Punkt und der Knopf, der
 * die Seite dafür zuklappt. Kein Hauptknopf, aber die volle Breite: Ohne ihn geht es
 * nicht weiter.
 */
@Composable
private fun Bauortwahl(ort: Pair<Double, Double>?, wort: String = "Standort", beiWaehlen: () -> Unit) {
    if (ort != null) {
        Text(text = "$wort gesetzt: ${koordinate(ort)}", style = Schrift.MonoKlein, color = Farben.TextLeise)
    }
    Knopf(
        aufschrift = if (ort != null) "$wort ändern" else "$wort auf der Karte wählen",
        beiDruck = beiWaehlen,
        breit = true,
    )
}

/** Eine Zeile der eigenen Punkte — aufgeklappt mit allen Einstellungen. */
@Composable
private fun Punktzeile(
    p: Weltpoi,
    offen: Boolean,
    ziehtHier: Boolean,
    ziehtAuf: Boolean,
    ecken: List<Pair<Double, Double>>,
    noetigePunkte: Int,
    masswort: String,
    sendet: Boolean,
    beiUmschalten: () -> Unit,
    beiAenderung: (WeltPoiAenderung) -> Unit,
    beiEntfernen: () -> Unit,
    beiZeichnen: (Boolean) -> Unit,
    beiKarte: () -> Unit,
    beiEckeZurueck: () -> Unit,
    beiVerwerfen: () -> Unit,
    beiUebernehmen: () -> Unit,
    gelaendeart: String,
    beiGelaendeart: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
    ) {
        Text(
            text = p.name,
            style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
            color = if (p.aktiv) Farben.Text else Farben.TextSehrLeise,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Leisetext(
            buildString {
                append(p.artText)
                if (p.gelaende.isNotEmpty()) append(" · ").append(groesse(p))
                if (p.betroffene > 0) append(" · etwa ${p.betroffene} Personen")
                if (!p.aktiv) append(" · stillgelegt")
            },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(if (p.sichtbar) "Ausblenden" else "Einblenden", { beiAenderung(WeltPoiAenderung(sichtbar = !p.sichtbar)) }, kompakt = true)
            Knopf(if (p.aktiv) "Stilllegen" else "Anschalten", { beiAenderung(WeltPoiAenderung(aktiv = !p.aktiv)) }, kompakt = true)
            Knopf("Einstellen", beiUmschalten, art = if (offen) Knopfart.Haupt else Knopfart.Normal, kompakt = true)
            Knopf("Entfernen", beiEntfernen, art = Knopfart.Leise, kompakt = true)
        }

        if (offen) Punkteinstellungen(
            p, ziehtHier, ziehtAuf, ecken, noetigePunkte, masswort, sendet, beiAenderung, beiZeichnen,
            beiKarte, beiEckeZurueck, beiVerwerfen, beiUebernehmen, gelaendeart, beiGelaendeart,
        )
    }
}

/**
 * Die Einstellungen eines Punktes. Jede ändert etwas im Spiel, und was sie bewirkt,
 * steht als Satz daneben — am Handy sieht niemand einen Tooltip.
 */
@Composable
private fun ColumnScope.Punkteinstellungen(
    p: Weltpoi,
    ziehtHier: Boolean,
    ziehtAuf: Boolean,
    ecken: List<Pair<Double, Double>>,
    noetigePunkte: Int,
    masswort: String,
    sendet: Boolean,
    beiAenderung: (WeltPoiAenderung) -> Unit,
    beiZeichnen: (Boolean) -> Unit,
    beiKarte: () -> Unit,
    beiEckeZurueck: () -> Unit,
    beiVerwerfen: () -> Unit,
    beiUebernehmen: () -> Unit,
    gelaendeart: String,
    beiGelaendeart: (String) -> Unit,
) {
    var personen by remember(p.id, p.betroffeneGesetzt) { mutableStateOf(p.betroffeneGesetzt?.toString() ?: "") }
    var hinweise by remember(p.id, p.hinweise) { mutableStateOf(p.hinweise.joinToString("\n")) }

    // Das Gelände obenan: Es braucht als Einziges die Karte.
    Etikettzeile("Gelände")
    if (ziehtHier) {
        Leisetext(
            if (ecken.size < noetigePunkte) "${ecken.size} von mindestens $noetigePunkte Punkten — tipp auf die Karte."
            else "${ecken.size} Punkte · $masswort",
        )
        Auswahlfeld(
            platzhalter = "Was für ein Gelände",
            wahlen = listOf(Wahl("", "Grundstück eines Gebäudes")) + GELAENDEARTEN.map { Wahl(it.first, it.second) },
            gewaehlt = gelaendeart,
            beiWahl = beiGelaendeart,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf("Weiter auf der Karte", beiKarte, kompakt = true)
            Knopf("Punkt zurück", beiEckeZurueck, kompakt = true, aktiv = ecken.isNotEmpty())
            Knopf("Übernehmen", beiUebernehmen, art = Knopfart.Haupt, kompakt = true, aktiv = !sendet && ecken.size >= noetigePunkte)
            Knopf("Verwerfen", beiVerwerfen, art = Knopfart.Leise, kompakt = true)
        }
    } else {
        val art = p.gelaendeart
        Leisetext(
            if (p.gelaende.isNotEmpty()) {
                "${p.gelaende.size} Punkte · ${groesse(p)}" + (if (art != null) " · ${gelaendewort(art)}" else "") +
                    ". Lagen entstehen irgendwo ${if (p.strecke) "darauf" else "darin"} statt immer an derselben Stelle."
            } else {
                "Bisher ein Punkt: Jede Lage liegt an derselben Adresse. Zeichne eine Fläche, wenn es ein Werksgelände " +
                    "oder ein Waldstück ist — oder eine Strecke, wenn es eine Bahnlinie oder ein Autobahnabschnitt ist."
            },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf("Fläche zeichnen", { beiZeichnen(false) }, kompakt = true, aktiv = !ziehtAuf)
            Knopf("Strecke zeichnen", { beiZeichnen(true) }, kompakt = true, aktiv = !ziehtAuf)
            if (p.gelaende.isNotEmpty()) {
                Knopf("Zurücknehmen", { beiAenderung(WeltPoiAenderung(gelaende = emptyList())) }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }

    // Häufigkeit 1 bis 5 — im Web ein Schieber, hier fünf Pillen: Fünf Stufen trifft
    // der Finger sicherer als einen Schieber.
    Etikettzeile("Häufigkeit · ${p.gewicht}")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        (1..5).forEach { g ->
            Pille(aufschrift = g.toString(), an = p.gewicht == g, beiDruck = { if (p.gewicht != g) beiAenderung(WeltPoiAenderung(gewicht = g)) })
        }
    }
    Leisetext("Wie oft hier etwas ist, im Verhältnis zu deinen anderen Punkten. 1 ist der Regelfall, 5 heißt fünfmal so oft.")

    val art = p.gelaendeart
    Etikettzeile(if (art == null) "Personen im Objekt" else "Menschen ständig vor Ort")
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
        Feld(
            wert = personen,
            beiAenderung = { t -> personen = t.filter { it.isDigit() }.take(4) },
            platzhalter = if (art == null) "aus der Fläche: ${p.betroffene}" else "niemand",
            tastatur = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        Knopf(
            "Übernehmen",
            {
                val zahl = if (personen.isBlank()) -1 else (personen.toIntOrNull() ?: return@Knopf).coerceAtMost(5000)
                beiAenderung(WeltPoiAenderung(betroffene = zahl))
            },
            kompakt = true,
            aktiv = personen != (p.betroffeneGesetzt?.toString() ?: ""),
        )
    }
    if (art == null) {
        Leisetext("Leer lassen heißt: aus der Grundfläche gerechnet. Wer sein Heim kennt, muss die Bettenzahl nicht über Quadratmeter erraten.")
        Hakenzeile(
            text = "Nachts belegt",
            an = p.nachtsBelegt,
            beiWechsel = { beiAenderung(WeltPoiAenderung(nachtsBelegt = it)) },
        )
        Leisetext("Aus heißt: zwischen 22 und 6 Uhr ist niemand darin. Eine Schule um drei Uhr morgens ist dasselbe Gebäude, aber eine andere Lage.")
    } else {
        Leisetext(
            "Auf ${gelaendewort(art).lowercase()} wohnt niemand: Wer dort zu Schaden kommt, kommt aus der Lage selbst — " +
                "die Insassen des Fahrzeugs, die Reisenden des Zuges, der vermisste Spaziergänger. Leer lassen ist " +
                "deshalb der Regelfall. Trag nur etwas ein, wenn dort wirklich ständig Menschen sind.",
        )
    }

    Etikettzeile("Lagen für")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        POI_ORGANISATIONEN.forEach { (id, wort) ->
            val an = id in p.organisationen
            Pille(
                aufschrift = wort,
                an = an,
                farbe = organisationsfarbe(id),
                beiDruck = {
                    val neu = if (an) p.organisationen.filter { it != id } else p.organisationen + id
                    beiAenderung(WeltPoiAenderung(organisationen = neu))
                },
            )
        }
    }
    Leisetext("Nichts angekreuzt heißt alle. Das Altenheim für den Rettungsdienst, das Lager für die Feuerwehr — so bleibt die Absicht erhalten, mit der du den Punkt gesetzt hast.")

    Etikettzeile("Einsatzplan")
    Feld(
        wert = hinweise,
        beiAenderung = { hinweise = it.take(2000) },
        platzhalter = "Eine Zeile je Hinweis — Zufahrt, Schlüsseldepot, Besonderheiten",
        einzeilig = false,
    )
    Knopf(
        "Einsatzplan sichern",
        { beiAenderung(WeltPoiAenderung(hinweise = hinweise)) },
        kompakt = true,
        aktiv = hinweise != p.hinweise.joinToString("\n"),
    )
    Leisetext(
        "Was die Besatzung auf der Anfahrt liest. Leer heißt: die allgemeinen Hinweise " +
            if (art != null) "dieser Geländeart — Zufahrt, Wasser, Sperrung." else "dieser Objektart.",
    )

    Etikettzeile("Farbe der Marke · ${p.farbe}")
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
        Knopf("−", { beiAenderung(WeltPoiAenderung(farbe = p.farbe - 1)) }, kompakt = true, aktiv = p.farbe > 0)
        Knopf("+", { beiAenderung(WeltPoiAenderung(farbe = p.farbe + 1)) }, kompakt = true, aktiv = p.farbe < 20)
    }
    Leisetext("0 ist automatisch. Damit lassen sich mehrere eigene Punkte auf der Karte auseinanderhalten.")
}

// ------------------------------------------------------------------ Fahrzeugkauf

/**
 * Der Fahrzeugkauf — auf <em>eine</em> Wache. Die Liste zeigt nur, was dort stehen darf
 * und freigeschaltet ist, gruppiert nach Kategorie, mit Riss und Preis.
 */
@Composable
private fun Fahrzeugkauf(welt: Welt, stand: Weltzustand, katalog: Katalog?) {
    val bereich = rememberCoroutineScope()
    val packbilder by welt.packbilder.collectAsState()

    var wacheFuerKauf by remember { mutableStateOf<String?>(null) }
    var vorlage by remember { mutableStateOf<String?>(null) }
    var suche by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }

    // Ohne Lehrgangseinrichtung und Werkstatt: Dort steht kein Fahrzeug.
    val wachen = stand.stand?.wachen.orEmpty().filter { traegtFahrzeuge(it.art) }
    val kaufwache = wachen.firstOrNull { it.id == wacheFuerKauf }
    val frei = stand.stand?.freischaltungen.orEmpty().filter { it.frei }.map { it.was }.toSet()

    val fahrzeugvorlagen: List<Fahrzeugvorlage> = remember(kaufwache?.art, frei, katalog) {
        val w = kaufwache ?: return@remember emptyList()
        jeTypEine(katalog?.fahrzeuge.orEmpty()).filter { f ->
            if (!passtZurWache(w.art, f)) return@filter false
            when (f.organisation) {
                "Feuerwehr" -> true
                "Rettungsdienst" -> "Rettungsdienst" in frei
                "Polizei" -> "Polizei" in frei
                "Thw" -> "Thw" in frei
                else -> false
            }
        }
    }

    // Wechselt die Wache, verfällt eine Wahl, die dort nicht stehen darf.
    LaunchedEffect(fahrzeugvorlagen) {
        if (vorlage != null && fahrzeugvorlagen.none { it.id == vorlage }) vorlage = null
    }

    val freiePlaetze = kaufwache?.let { it.stellplaetze - it.belegt } ?: 0
    val guthaben = stand.guthaben

    fun passt(f: Fahrzeugvorlage): Boolean {
        val t = suche.trim().lowercase()
        if (t.isEmpty()) return true
        return f.typ.lowercase().contains(t) ||
            f.beschreibung.lowercase().contains(t) ||
            f.kategorie.lowercase().contains(t) ||
            f.hiOrg.lowercase().contains(t) ||
            f.faehigkeiten.any { it.lowercase().contains(t) }
    }

    val gruppen = fahrzeugvorlagen.filter { passt(it) }.groupBy { it.kategorie }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        Etikettzeile("Wache")
        Auswahlfeld(
            platzhalter = "Wähle eine Wache",
            wahlen = wachen.map { Wahl(it.id, "${it.name} · ${it.belegt}/${it.stellplaetze}", aktiv = it.belegt < it.stellplaetze) },
            gewaehlt = wacheFuerKauf,
            beiWahl = { wacheFuerKauf = it },
            modifier = Modifier.fillMaxWidth(),
        )

        when {
            wachen.isEmpty() -> Leisetext("Noch keine Wache, auf der ein Fahrzeug stehen kann. Erst bauen, dann kaufen.")
            kaufwache == null -> Leisetext("Wähle zuerst die Wache — was dort stehen darf, hängt an ihrer Bauart.")
            else -> {
                Text(text = "Noch $freiePlaetze Plätze frei.", style = Schrift.Klein, color = Farben.TextLeise)
                if (fahrzeugvorlagen.isEmpty()) Leisetext("Für diese Wache ist gerade nichts freigeschaltet.")
            }
        }

        if (fahrzeugvorlagen.size > 8) {
            Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Typ, Fähigkeit, Organisation …", stil = Schrift.MonoKlein)
        }

        if (fahrzeugvorlagen.isNotEmpty() && gruppen.isEmpty()) {
            Leisetext("Kein Fahrzeug passt zu „$suche\".")
        }

        val jetzt = System.currentTimeMillis()
        gruppen.forEach { (kategorie, liste) ->
            Gruppenkopf(kategorie)
            liste.forEach { f ->
                val preis = stand.stand?.fahrzeugpreise?.get(f.id) ?: 0
                val an = vorlage == f.id
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (preis > guthaben) 0.55f else 1f)
                        .flaeche(
                            farbe = if (an) Farben.FlaecheAktiv else Farben.Flaeche,
                            randfarbe = if (an) Farben.Amber else Farben.Rand,
                            ecke = 9.dp,
                        )
                        .clickable { vorlage = f.id }
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Canvas(modifier = Modifier.size(width = 46.dp, height = 24.dp)) {
                        fahrzeugmarkeZeichnen(
                            ort = Offset(size.width / 2f, size.height / 2f),
                            kurs = 90.0,
                            plan = rissplan(f, f.typ),
                            blaulicht = false,
                            jetzt = jetzt,
                            bild = packbilder[f.id],
                            icon = stand.pack[f.id],
                            hoehe = 40f,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                        Text(text = f.typ, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                        Text(
                            text = f.faehigkeiten.joinToString(" · ").ifEmpty { f.beschreibung },
                            style = Schrift.Winzig,
                            color = Farben.TextSehrLeise,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Credits(preis, stil = Schrift.MonoKlein)
                }
            }
        }

        Absage(fehler)

        Knopf(
            aufschrift = if (sendet) "Wird gekauft …" else "Kaufen",
            beiDruck = {
                val w = wacheFuerKauf
                val v = vorlage
                if (w == null || v == null) {
                    fehler = "Wähle Wache und Fahrzeug."
                } else if (!sendet) {
                    sendet = true
                    fehler = null
                    bereich.launch {
                        fehler = welt.aktion("Der Kauf ging nicht.", Welt.Nachladen.Beides) {
                            welt.wege.fahrzeugKaufen(it, w, v)
                        }
                        sendet = false
                    }
                }
            },
            art = Knopfart.Haupt,
            aktiv = !sendet && freiePlaetze > 0 && vorlage != null,
            breit = true,
        )
    }
}
