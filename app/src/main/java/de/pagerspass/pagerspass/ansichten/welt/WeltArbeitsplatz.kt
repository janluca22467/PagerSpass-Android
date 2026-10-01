package de.pagerspass.pagerspass.ansichten.welt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.WeltPunkt
import de.pagerspass.pagerspass.netz.WeltStreifenstation
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Markenzahl
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Die Werkzeuge des Arbeitsplatzes — `werkzeuge.ts`.
 *
 * Fünf stehen an der Leiste (`HANDY_REITER`), der Rest hinter „Mehr“
 * (`HINTER_MEHR`); Fahrzeugkauf und Wachenseite öffnen sich aus anderen Seiten.
 */
enum class Werkzeug(val name_: String, val kurz: String, val zeichen: ImageVector) {
    Lagen("Lagen", "Lagen", Weltzeichen.Lagen),
    Fahrzeuge("Fahrzeuge", "Fahrzeuge", Weltzeichen.Fahrzeuge),
    Wachen("Wachen", "Wachen", Weltzeichen.Wachen),
    Bauen("Wache bauen", "Bauen", Weltzeichen.Bauen),
    Mehr("Mehr", "Mehr", Weltzeichen.Mehr),
    Fahrzeugkauf("Fahrzeug kaufen", "Kaufen", Weltzeichen.Kauf),
    Wachenseite("Wache", "Wache", Weltzeichen.Wache),
    Grosslage("Großeinsatz", "Großeinsatz", Weltzeichen.Grosslage),
    Chat("Chat", "Chat", Weltzeichen.Chat),
    Kasse("Kasse", "Kasse", Weltzeichen.Kasse),
    Rangliste("Rangliste", "Rangliste", Weltzeichen.Rangliste),
    Laufbahn("Laufbahn", "Laufbahn", Weltzeichen.Laufbahn),
    Leihe("Verleih", "Verleih", Weltzeichen.Leihe),
    Einstellungen("Einstellungen", "Einstellungen", Weltzeichen.Einstellungen),
    ;

    companion object {
        val REITER = listOf(Lagen, Fahrzeuge, Wachen, Bauen, Mehr)
        val HINTER_MEHR = listOf(Grosslage, Chat, Kasse, Rangliste, Laufbahn, Leihe, Einstellungen)
    }
}

/**
 * Was die Seiten und die Karte gemeinsam halten — offene Seite, Kartenmodus,
 * Bauort, Zeichnungen.
 *
 * <b>Am Handy liegt die Seite über der Karte.</b> Wer einen Ort wählt, braucht
 * die Karte frei: Die Seite geht zu, die Karte nimmt den Tipp, und danach kommt
 * genau die Seite zurück, von der man kam (`rueckkehr`) — mit allem, was man
 * dort schon eingetragen hatte (die Entwürfe stehen hier und nicht in der Seite).
 */
class Werkbank {
    var seite by mutableStateOf<Werkzeug?>(null)
    var modus by mutableStateOf(Kartenmodus.Normal)
    var rueckkehr by mutableStateOf<Werkzeug?>(null)

    /** Wofür der Ort gewählt wird: `wache`, `zweig` oder `poi`. */
    var ortZweck by mutableStateOf("wache")
    var bauort by mutableStateOf<WeltPunkt?>(null)

    /** Die Ecken eines Geländes und ob es eine Strecke ist. */
    var ecken by mutableStateOf(listOf<WeltPunkt>())
    var eckenStrecke by mutableStateOf(false)

    /** Der Streifenpfad im Entwurf und für welches Fahrzeug. */
    var pfad by mutableStateOf(listOf<WeltStreifenstation>())
    var pfadFahrzeug by mutableStateOf<String?>(null)

    var gewaehlteWache by mutableStateOf<String?>(null)
    var gewaehltesFahrzeug by mutableStateOf<String?>(null)
    var offenesFahrzeug by mutableStateOf<String?>(null)
    var fremdwache by mutableStateOf<String?>(null)
    var fremdfahrzeug by mutableStateOf<String?>(null)
    var bauVorgang by mutableStateOf("keiner")

    // Die Entwürfe der Bauseite. Sie stehen hier, weil die Seite beim Wählen
    // eines Orts zugeht — und mit ihr alles, was nur in ihr gemerkt wäre.
    var bauName by mutableStateOf("")
    var bauArt by mutableStateOf<String?>(null)
    var zweigName by mutableStateOf("")
    var poiForm by mutableStateOf("Punkt")
    var poiName by mutableStateOf("")
    var poiArt by mutableStateOf("Schule")
    var poiGelaendeart by mutableStateOf("")
    var poiFlaeche by mutableStateOf(1200)
    var offenerPunkt by mutableStateOf<String?>(null)
    var gelaendeFuer by mutableStateOf<String?>(null)

    fun umschalten(was: Werkzeug) {
        if (seite == was) {
            seite = null
            return
        }
        seite = was
        if (modus != Kartenmodus.Normal && was != rueckkehr) modus = Kartenmodus.Normal
    }

    /** Die Karte freigeben und in einen Wahlmodus gehen. */
    fun karteWaehlen(neu: Kartenmodus) {
        rueckkehr = seite
        modus = neu
        seite = null
    }

    /** Zurück an die Seite, von der man kam. */
    fun zurueckZurSeite() {
        seite = rueckkehr ?: seite
    }

    companion object {
        /**
         * Was das Drehen des Geräts überstehen muss: die offene Seite und was in
         * ihr gewählt ist. Wahlmodus und Entwürfe gehen mit — ein halb
         * gezeichnetes Gelände nach dem Drehen neu anzufangen wäre ärgerlicher
         * als eine Liste, die wieder oben steht.
         */
        val SICHERUNG: Saver<Werkbank, Any> = listSaver<Werkbank, String>(
            save = { w ->
                listOf(
                    w.seite?.name.orEmpty(), w.rueckkehr?.name.orEmpty(), w.modus.name,
                    w.gewaehlteWache.orEmpty(), w.gewaehltesFahrzeug.orEmpty(), w.offenesFahrzeug.orEmpty(),
                    w.bauVorgang, w.bauName, w.bauArt.orEmpty(), w.zweigName, w.poiForm, w.poiName, w.poiArt,
                    w.poiGelaendeart, w.poiFlaeche.toString(), "", w.ortZweck,
                    w.bauort?.let { "${it.lat};${it.lon}" }.orEmpty(),
                )
            },
            restore = { l ->
                fun t(i: Int) = l.getOrNull(i).orEmpty()
                fun n(i: Int) = t(i).ifEmpty { null }
                Werkbank().apply {
                    seite = n(0)?.let { name -> Werkzeug.entries.firstOrNull { it.name == name } }
                    rueckkehr = n(1)?.let { name -> Werkzeug.entries.firstOrNull { it.name == name } }
                    modus = Kartenmodus.entries.firstOrNull { it.name == t(2) } ?: Kartenmodus.Normal
                    gewaehlteWache = n(3)
                    gewaehltesFahrzeug = n(4)
                    offenesFahrzeug = n(5)
                    bauVorgang = n(6) ?: "keiner"
                    bauName = t(7)
                    bauArt = n(8)
                    zweigName = t(9)
                    poiForm = n(10) ?: "Punkt"
                    poiName = t(11)
                    poiArt = n(12) ?: "Schule"
                    poiGelaendeart = t(13)
                    poiFlaeche = t(14).toIntOrNull() ?: 1200
                    ortZweck = n(16) ?: "wache"
                    bauort = n(17)?.split(";")?.let { p ->
                        val lat = p.getOrNull(0)?.toDoubleOrNull()
                        val lon = p.getOrNull(1)?.toDoubleOrNull()
                        if (lat != null && lon != null) WeltPunkt(lat, lon) else null
                    }
                }
            },
        )
    }
}

/**
 * Der Arbeitsplatz — das Gegenstück zu `WeltView.vue` in der Handyform.
 *
 * <b>Oben der Stand, unten die Reiter, dazwischen die Karte</b> — und darüber,
 * wenn eine offen ist, genau eine Seite: vom Kopf bis zur Reiterleiste, deckend
 * (`Weltseite.vue`). Eine Seite über 60 % der Höhe verdeckte die Karte fast ganz
 * und zeigte trotzdem nur zwei Drittel ihres eigenen Inhalts — das Schlechteste
 * aus beidem; das Web hat es deshalb aufgegeben, und die App mit ihm.
 *
 * <b>Man kommt auf der Karte an</b>, auch ohne Wache: Der Reiter „Bauen“ atmet
 * dann, und die Einführung zeigt auf ihn. Eine Seite, die beim Ankommen über
 * der ganzen Karte liegt, nimmt einem genau das weg, was die Welt ist.
 */
@Composable
fun WeltArbeitsplatz(welt: Welt, zustand: Weltzustand, beiVerlassen: () -> Unit) {
    // Die offene Seite übersteht das Drehen des Geräts (die Activity wird dabei
    // neu gebaut); die Ebenen überstehen sogar den Neustart — wie im Web.
    val werkbank = rememberSaveable(saver = Werkbank.SICHERUNG) { Werkbank() }
    val zusammenhang = LocalContext.current
    val ebenen = remember { Weltebenen().also { Ebenenablage.laden(zusammenhang, it) } }
    LaunchedEffect(ebenen) {
        snapshotFlow {
            listOf(
                ebenen.lagen, ebenen.eigene, ebenen.wachen, ebenen.pois,
                ebenen.wege, ebenen.fremde, ebenen.fremdeWachen, ebenen.grosslage,
            )
        }.drop(1).collect { Ebenenablage.merken(zusammenhang, ebenen) }
    }
    val stand = zustand.stand
    val karte = remember {
        Weltkartenstand(stand?.lat ?: 51.2, stand?.lon ?: 10.4, 12.0)
    }
    val bereich = rememberCoroutineScope()
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val unten = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val kopfhoehe = oben + Weltmass.Kopfhoehe

    // Zurück schließt erst den Steckbrief, dann die Seite (hinter „Mehr“ eine
    // Ebene zurück), dann den Wahlmodus, dann den Fahrweg, dann die Welt.
    BackHandler {
        when {
            werkbank.fremdwache != null -> werkbank.fremdwache = null
            werkbank.fremdfahrzeug != null -> werkbank.fremdfahrzeug = null
            werkbank.seite != null && werkbank.seite in Werkzeug.HINTER_MEHR -> werkbank.seite = Werkzeug.Mehr
            werkbank.seite != null -> werkbank.seite = null
            werkbank.modus != Kartenmodus.Normal -> {
                werkbank.modus = Kartenmodus.Normal
                werkbank.zurueckZurSeite()
            }
            werkbank.gewaehltesFahrzeug != null -> werkbank.gewaehltesFahrzeug = null
            else -> beiVerlassen()
        }
    }

    Box(Modifier.fillMaxSize().background(Farben.BgTief)) {
        Weltkarte(
            zustand = zustand,
            kartenstand = karte,
            ebenen = ebenen,
            modifier = Modifier.fillMaxSize(),
            modus = werkbank.modus,
            bauradien = werkbank.modus == Kartenmodus.Ort || werkbank.modus == Kartenmodus.Gelaende ||
                werkbank.seite == Werkzeug.Bauen,
            markiert = werkbank.bauort.takeIf { werkbank.seite == Werkzeug.Bauen || werkbank.modus == Kartenmodus.Ort },
            zeichnung = when (werkbank.modus) {
                Kartenmodus.Gelaende -> werkbank.ecken
                Kartenmodus.Pfad -> werkbank.pfad.map { WeltPunkt(it.lat, it.lon) }
                else -> if (werkbank.seite == Werkzeug.Bauen) werkbank.ecken else emptyList()
            },
            zeichnungGeschlossen = werkbank.modus == Kartenmodus.Gelaende && !werkbank.eckenStrecke,
            gewaehlteLage = zustand.auswahlLage,
            gewaehltesFahrzeug = werkbank.gewaehltesFahrzeug,
            polster = PaddingValues(top = kopfhoehe),
            beiLage = { id ->
                welt.lageWaehlen(id)
                werkbank.seite = Werkzeug.Lagen
            },
            // Ein eigenes Fahrzeug antippen zeigt seinen Fahrweg — und nur das,
            // wie im Web. Was es tut, steht unter „Fahrzeuge“.
            beiFahrzeug = { werkbank.gewaehltesFahrzeug = it },
            beiWache = { id ->
                werkbank.gewaehlteWache = id
                werkbank.seite = Werkzeug.Wachenseite
            },
            beiFremdwache = { werkbank.fremdwache = it },
            beiFremdfahrzeug = { werkbank.fremdfahrzeug = it },
            beiPoi = {
                werkbank.bauVorgang = "poi"
                werkbank.seite = Werkzeug.Bauen
            },
            beiOrt = { ort ->
                when (werkbank.modus) {
                    // Ein Tipp, ein Punkt, zurück ins Formular (`bauortGesetzt`).
                    Kartenmodus.Ort -> {
                        werkbank.bauort = ort
                        werkbank.modus = Kartenmodus.Normal
                        werkbank.zurueckZurSeite()
                    }
                    // Eine Fläche hat drei bis zwölf Ecken: Man bleibt auf der
                    // Karte, bis „Fertig“ auf der Zeichenleiste.
                    Kartenmodus.Gelaende -> if (werkbank.ecken.size < 12) werkbank.ecken = werkbank.ecken + ort
                    // Eine Station, dann zurück in die Fahrzeugseite, wo die Liste
                    // wächst (`pfadpunktGesetzt`) — weiter geht es mit demselben Knopf.
                    Kartenmodus.Pfad -> bereich.launch {
                        if (werkbank.pfad.size < 12) {
                            val name = welt.kennung?.let { k ->
                                runCatching { welt.wege.ortsname(k, ort.lat, ort.lon).name }.getOrNull()
                            } ?: "%.4f / %.4f".format(ort.lat, ort.lon)
                            werkbank.pfad = werkbank.pfad + WeltStreifenstation(name, ort.lat, ort.lon)
                        }
                        werkbank.modus = Kartenmodus.Normal
                        werkbank.zurueckZurSeite()
                    }
                    Kartenmodus.Normal -> Unit
                }
            },
            beiLangdruck = { ort ->
                // Lang drücken setzt den Bauort — der kürzeste Weg von „hier
                // hätte ich gern eine Wache" zur Bauseite.
                werkbank.bauort = ort
                werkbank.bauVorgang = "wache"
                werkbank.seite = Werkzeug.Bauen
            },
        )

        // Der erste Abruf läuft noch: ein Hinweis mittig über der leeren Karte,
        // statt dass „nichts los“ und „noch nicht geladen“ gleich aussehen.
        if (zustand.betrieb == null && zustand.betriebLaeuft && zustand.fehler == null) {
            Text(
                "Die Welt wird geladen …",
                style = Schrift.MonoKlein,
                color = Farben.TextLeise,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        val seite = werkbank.seite
        if (seite != null) {
            Weltseite(
                titel = seite.name_,
                zeichen = seite.zeichen,
                zahl = when (seite) {
                    Werkzeug.Lagen -> zustand.offeneLagen.size
                    Werkzeug.Fahrzeuge -> zustand.freieFahrzeuge.size
                    else -> null
                },
                oben = kopfhoehe,
                // Der Weg zurück führt eine Ebene hoch, nicht bis nach Hause:
                // Wer über „Mehr“ in die Kasse ging, kommt dorthin zurück.
                beiSchliessen = {
                    werkbank.seite = if (seite in Werkzeug.HINTER_MEHR) Werkzeug.Mehr else null
                },
            ) {
                when (seite) {
                    Werkzeug.Lagen -> LagenSeite(welt, zustand, werkbank, karte)
                    Werkzeug.Fahrzeuge -> FahrzeugeSeite(welt, zustand, werkbank, karte)
                    Werkzeug.Fahrzeugkauf -> FahrzeugkaufSeite(welt, zustand, werkbank)
                    Werkzeug.Wachen -> WachenSeite(welt, zustand, werkbank, karte)
                    Werkzeug.Wachenseite -> WachenDetailSeite(welt, zustand, werkbank)
                    Werkzeug.Bauen -> BauSeite(welt, zustand, werkbank)
                    Werkzeug.Grosslage -> GrosslageSeite(welt, zustand, karte)
                    Werkzeug.Chat -> ChatSeite(welt, zustand)
                    Werkzeug.Kasse -> KasseSeite(welt, zustand)
                    Werkzeug.Rangliste -> RanglisteSeite(welt, zustand)
                    Werkzeug.Laufbahn -> LaufbahnSeite(welt, zustand)
                    Werkzeug.Leihe -> LeiheSeite(welt, zustand)
                    Werkzeug.Einstellungen -> EinstellungSeite(welt, zustand, ebenen)
                    Werkzeug.Mehr -> MehrSeite(zustand, werkbank, beiVerlassen)
                }
            }
        }

        Weltkopf(
            zustand = zustand,
            beiLaufbahn = { werkbank.umschalten(Werkzeug.Laufbahn) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = oben),
        )

        // Der Takt ist gerissen — es steht dran, statt still eingefrorene Daten
        // zu zeigen. Unter dem Kopf, damit Guthaben und Stufe lesbar bleiben.
        zustand.fehler?.let { f ->
            Text(
                text = "$f — es wird weiter versucht.",
                style = Schrift.MonoKlein,
                color = Farben.Text,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = kopfhoehe + Abstand.Klein, start = Abstand.Normal, end = Abstand.Normal)
                    .background(Farben.SignalTief, Rundung.Klein)
                    .border(1.dp, Farben.Signal, Rundung.Klein)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            )
        }

        // Unten: der Hinweis des Wahlmodus samt Zeichenleiste, die Einführung,
        // darunter die Reiter. Beim Standortwählen tritt die Einführung zur
        // Seite — zwei Erklärungen auf demselben Streifen sind keine.
        val standortWaehlen = seite == null && werkbank.modus != Kartenmodus.Normal
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            if (seite == null) Kartenhinweis(werkbank)
            val zeigt = WeltEinfuehrung(zustand = zustand, werkbank = werkbank, zeigen = !standortWaehlen)
            Welttableiste(zustand = zustand, werkbank = werkbank, unten = unten, zeigt = zeigt)
        }

        // Post und Pings aus dem Chat, solange er zu ist — `ChatHinweis.vue`.
        ChatHinweis(
            welt = welt,
            zustand = zustand,
            beiOeffnen = { werkbank.seite = Werkzeug.Chat },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = kopfhoehe + Abstand.Klein),
        )

        werkbank.fremdfahrzeug?.let { id ->
            val f = zustand.betrieb?.fremde?.firstOrNull { it.id == id }
            if (f == null) {
                werkbank.fremdfahrzeug = null
            } else {
                Blende(titel = f.funkrufname, beiSchliessen = { werkbank.fremdfahrzeug = null }) {
                    Wertzeile("Typ", f.typ)
                    Wertzeile("Leitstelle", f.besitzer ?: "zeigt sich nicht")
                    Wertzeile(
                        "Gerade",
                        when (f.lage) {
                            "Anfahrt" -> if (f.blaulicht) "auf Anfahrt mit Sondersignal" else "auf Anfahrt"
                            "VorOrt" -> "vor Ort"
                            "Bereitstellung" -> "im Bereitstellungsraum"
                            "Streife" -> "auf Streife"
                            "Rueckfahrt" -> "rückt ein"
                            else -> f.lage
                        },
                    )
                    zustand.fahrt.restMinuten(f.ankunftUm)?.takeIf { it > 0 }?.let { Wertzeile("Ankunft", "in ${dauer(it)}") }
                }
            }
        }

        werkbank.fremdwache?.let { id ->
            val w = zustand.betrieb?.fremdeWachen?.firstOrNull { it.id == id }
            if (w == null) {
                werkbank.fremdwache = null
            } else {
                Blende(titel = w.name, beiSchliessen = { werkbank.fremdwache = null }) {
                    Wertzeile("Bauart", artname(w.art))
                    Wertzeile("Leitstelle", w.besitzer ?: "zeigt sich nicht")
                    Wertzeile("Fahrzeuge auf dem Hof", "${w.fahrzeuge}")
                    Leisesatz(
                        "Eine Wache einer anderen Leitstelle. Ihre Lagen und deine liegen auf derselben Karte — " +
                            "wer zuerst da ist, bekommt den Zuschlag.",
                        winzig = true,
                    )
                }
            }
        }
    }
}

/** Was ein Fahrzeug gerade tut — `taetigkeit` in `FahrzeugBlende.vue`. */
fun taetigkeit(zustand: Weltzustand, f: de.pagerspass.pagerspass.netz.WeltFahrzeug): String = when {
    f.lehrgang != null -> if (f.lehrgangFertigUm != null) "im Lehrgang" else "fährt zum Lehrgang"
    f.inWerkstatt -> if (f.werkstattFertigUm != null) "in der Werkstatt" else "fährt in die Werkstatt"
    f.zielWache != null -> "zieht um"
    zustand.fahrt.ruecktAus(f.lage, f.losUm) -> "rückt aus"
    else -> when (f.lage) {
        "Wache" -> "auf Wache"
        "Ausrueckt" -> "rückt aus"
        "Anfahrt" -> "auf Anfahrt"
        "VorOrt" -> "vor Ort"
        "Bereitstellung" -> "im Bereitstellungsraum"
        "Streife" -> "auf Streife"
        "Rueckfahrt" -> "rückt ein"
        else -> f.lage
    }
}

