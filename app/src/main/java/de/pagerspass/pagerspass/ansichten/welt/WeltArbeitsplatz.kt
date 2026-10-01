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
 * wenn eine offen ist, genau eine Seite. Am Handy gibt es keine schwebenden
 * Blenden nebeneinander; die Seite nimmt die untere Hälfte, damit die Karte
 * darüber sichtbar bleibt und ein Tipp auf „hinschauen“ etwas zeigt.
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
    val hoehe = LocalConfiguration.current.screenHeightDp.dp

    // Beim ersten Mal ohne Wache: gleich die Bauseite — sonst steht man vor
    // einer leeren Karte und weiß nicht, wo es losgeht.
    LaunchedEffect(Unit) {
        if (stand?.wachen.isNullOrEmpty()) werkbank.seite = Werkzeug.Bauen
    }

    // Zurück schließt erst die Seite, dann den Wahlmodus, dann die Welt.
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
            polster = PaddingValues(top = oben + 64.dp),
            beiLage = { id ->
                welt.lageWaehlen(id)
                werkbank.seite = Werkzeug.Lagen
            },
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
                    Kartenmodus.Ort -> {
                        werkbank.bauort = ort
                        werkbank.modus = Kartenmodus.Normal
                        werkbank.zurueckZurSeite()
                    }
                    Kartenmodus.Gelaende -> if (werkbank.ecken.size < 12) werkbank.ecken = werkbank.ecken + ort
                    Kartenmodus.Pfad -> bereich.launch {
                        if (werkbank.pfad.size >= 12) return@launch
                        val name = welt.kennung?.let { k ->
                            runCatching { welt.wege.ortsname(k, ort.lat, ort.lon).name }.getOrNull()
                        } ?: "%.4f / %.4f".format(ort.lat, ort.lon)
                        werkbank.pfad = werkbank.pfad + WeltStreifenstation(name, ort.lat, ort.lon)
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

        Weltkopf(
            zustand = zustand,
            beiLaufbahn = { werkbank.umschalten(Werkzeug.Laufbahn) },
            beiVerlassen = beiVerlassen,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = oben),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding(),
        ) {
            // Der Betrieb ist gerissen: Es steht dran, statt still eingefrorene
            // Daten zu zeigen.
            zustand.fehler?.let { f ->
                Text(
                    text = "$f — es wird weiter versucht.",
                    style = Schrift.MonoKlein,
                    color = Farben.Text,
                    modifier = Modifier
                        .padding(horizontal = Abstand.Normal)
                        .background(Farben.SignalTief, Rundung.Klein)
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                )
            }
            if (zustand.betrieb == null && zustand.betriebLaeuft && zustand.fehler == null) {
                Text(
                    "Die Welt wird geladen …",
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                    modifier = Modifier.padding(horizontal = Abstand.Normal),
                )
            }

            val seite = werkbank.seite
            // Die Einführung tritt zur Seite, solange eine Seite offen ist oder
            // die Karte etwas wählt — zwei Erklärungen übereinander sind keine.
            val zeigt = WeltEinfuehrung(
                zustand = zustand,
                werkbank = werkbank,
                zeigen = seite == null && werkbank.modus == Kartenmodus.Normal && werkbank.gewaehltesFahrzeug == null,
            )
            if (seite == null) {
                Modushinweis(welt, zustand, werkbank)
                Fahrzeugchip(zustand, werkbank)
            } else {
                Weltseite(
                    titel = seite.name_,
                    zahl = when (seite) {
                        Werkzeug.Lagen -> zustand.offeneLagen.size
                        Werkzeug.Fahrzeuge -> zustand.freieFahrzeuge.size
                        else -> null
                    },
                    hoehe = hoehe * 0.6f,
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

            Welttableiste(
                zustand = zustand,
                werkbank = werkbank,
                unten = unten,
                zeigt = zeigt?.let { if (it in Werkzeug.HINTER_MEHR) Werkzeug.Mehr else it },
            )
        }

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

/** Der Stand oben — Leitstelle, Guthaben, Stufe, Erfahrungskante (`Weltkopf.vue`). */
@Composable
private fun Weltkopf(
    zustand: Weltzustand,
    beiLaufbahn: () -> Unit,
    beiVerlassen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quelleAb = zustand.betrieb?.erfahrungStufeAb ?: zustand.stand?.erfahrungStufeAb ?: 0
    val quelleBis = if (zustand.betrieb != null) zustand.betrieb.erfahrungStufeBis else zustand.stand?.erfahrungStufeBis
    val erfahrung = zustand.betrieb?.erfahrung ?: zustand.stand?.erfahrung ?: 0
    val hoechste = quelleBis == null || quelleBis <= quelleAb
    val anteil = if (hoechste) 1f else ((erfahrung - quelleAb).toFloat() / (quelleBis!! - quelleAb)).coerceIn(0f, 1f)
    val restschuld = zustand.betrieb?.restschuld ?: 0

    // Die Gutschrift leuchtet vier Sekunden auf; mehrere in Folge zählen zusammen.
    var gezeigt by remember { mutableStateOf<Long?>(null) }
    var zuletzt by remember { mutableStateOf(0L) }
    LaunchedEffect(zustand.gutschrift) {
        val g = zustand.gutschrift ?: return@LaunchedEffect
        if (g.um == zuletzt) return@LaunchedEffect
        zuletzt = g.um
        gezeigt = (gezeigt ?: 0) + g.betrag
        delay(4_000)
        gezeigt = null
    }

    Column(
        modifier = modifier
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Klein)
            .fillMaxWidth()
            .flaeche(farbe = Farben.FlaecheHoch.copy(alpha = 0.94f), ecke = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Abstand.Klein, end = Abstand.Klein, top = Abstand.Klein),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = beiVerlassen)
                    .semantics { contentDescription = "World verlassen" },
            ) {
                Icon(Weltzeichen.Zurueck, contentDescription = null, tint = Farben.TextLeise, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    zustand.stand?.name.orEmpty(),
                    style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                zustand.stand?.kreis?.let {
                    Text(it, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$WAEHRUNG ${zahl(zustand.guthaben)}", style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
                    gezeigt?.let {
                        Text("+${zahl(it)}", style = Schrift.MonoKlein, color = Farben.GruenHell)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable(onClick = beiLaufbahn),
                ) {
                    if (restschuld > 0) Text("−${zahl(restschuld)}", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.SignalHell)
                    Text("Stufe ${zustand.stufe}", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(top = Abstand.Klein)
                .fillMaxWidth()
                .height(4.dp)
                .background(Farben.BgTief),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(anteil)
                    .height(4.dp)
                    .background(if (hoechste) Farben.AmberHell else Farben.Amber),
            )
        }
    }
}

/** Der Rahmen einer Seite — Titel, Zahl, Schließen, darunter der Inhalt, der rollt. */
@Composable
private fun Weltseite(
    titel: String,
    zahl: Int?,
    hoehe: androidx.compose.ui.unit.Dp,
    beiSchliessen: () -> Unit,
    inhalt: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = Abstand.Klein)
            .fillMaxWidth()
            .height(hoehe)
            .flaeche(farbe = Farben.Bg.copy(alpha = 0.98f), randfarbe = Farben.RandHell, ecke = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.padding(start = Abstand.Gross, end = Abstand.Klein, top = Abstand.Klein),
        ) {
            Text(titel, style = Schrift.Gross.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
            if (zahl != null && zahl > 0) Markenzahl(zahl)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = beiSchliessen)
                    .semantics { contentDescription = "$titel schließen" },
            ) {
                Text("✕", style = Schrift.Normal, color = Farben.TextLeise)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Abstand.Gross, end = Abstand.Gross, bottom = Abstand.Gross),
        ) { inhalt() }
    }
}

/**
 * Die Reiter der Welt — `Welttableiste.vue`: Lagen, Fahrzeuge, Wachen, Bauen,
 * Mehr. Die Marke an „Lagen“ wird gefüllt, sobald etwas nur bei mir liegt.
 */
@Composable
private fun Welttableiste(
    zustand: Weltzustand,
    werkbank: Werkbank,
    unten: androidx.compose.ui.unit.Dp,
    zeigt: Werkzeug? = null,
) {
    val nochNichts = zustand.stand?.wachen.isNullOrEmpty()
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFA263344), Color(0xFA101823))),
                Rundung.LeisteOben,
            )
            .padding(horizontal = 4.dp)
            .padding(bottom = unten),
    ) {
        Werkzeug.REITER.forEach { w ->
            val hier = when (w) {
                Werkzeug.Bauen -> werkbank.seite == Werkzeug.Bauen || werkbank.seite == Werkzeug.Fahrzeugkauf ||
                    (werkbank.seite == null && werkbank.modus != Kartenmodus.Normal)
                Werkzeug.Mehr -> werkbank.seite == Werkzeug.Mehr || werkbank.seite in Werkzeug.HINTER_MEHR
                Werkzeug.Wachen -> werkbank.seite == Werkzeug.Wachen || werkbank.seite == Werkzeug.Wachenseite
                else -> werkbank.seite == w
            }
            val marke = when (w) {
                Werkzeug.Lagen -> zustand.offeneLagen.size
                Werkzeug.Fahrzeuge -> zustand.freieFahrzeuge.size
                Werkzeug.Mehr -> zustand.ungelesen
                else -> 0
            }
            Reiterknopf(
                werkzeug = w,
                hier = hier,
                marke = marke,
                ruft = (w == Werkzeug.Bauen && nochNichts && !hier) ||
                    (w == Werkzeug.Lagen && zustand.meineOffenen > 0) || (w == zeigt && !hier),
                punkt = w == Werkzeug.Mehr && (zustand.grossAktiv || zustand.angepingt != null) && marke == 0,
                markeRot = w == Werkzeug.Lagen && zustand.meineOffenen > 0,
                beiDruck = {
                    if (w == Werkzeug.Bauen) werkbank.modus = Kartenmodus.Normal
                    werkbank.umschalten(w)
                },
            )
        }
    }
}

@Composable
private fun RowScope.Reiterknopf(
    werkzeug: Werkzeug,
    hier: Boolean,
    marke: Int,
    ruft: Boolean,
    punkt: Boolean,
    markeRot: Boolean,
    beiDruck: () -> Unit,
) {
    val farbe = when {
        hier -> Farben.Amber
        ruft -> Farben.AmberHell
        else -> Farben.TextLeise
    }
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 6.dp)
            .defaultMinSize(minHeight = 56.dp)
            .background(if (hier) Farben.HauchAmber else Color.Transparent, Rundung.Normal)
            .border(1.dp, if (hier) Farben.AmberTief else Color.Transparent, Rundung.Normal)
            .clickable(onClick = beiDruck, role = Role.Tab)
            .semantics { contentDescription = if (marke > 0) "${werkzeug.name_} — $marke" else werkzeug.name_ },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(top = Abstand.Klein),
        ) {
            Icon(werkzeug.zeichen, contentDescription = null, tint = farbe, modifier = Modifier.size(22.dp))
            Text(werkzeug.kurz, style = Schrift.Weg, color = farbe, maxLines = 1)
        }
        if (marke > 0) {
            Box(Modifier.align(Alignment.TopCenter).padding(start = 30.dp, top = 2.dp)) {
                Text(
                    text = if (marke > 99) "99+" else "$marke",
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                    color = if (markeRot) Farben.AufFarbe else Farben.Text,
                    modifier = Modifier
                        .background(if (markeRot) Farben.Signal else Farben.FlaecheAktiv, Rundung.Rund)
                        .padding(horizontal = 5.dp),
                )
            }
        } else if (punkt) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 28.dp, top = 6.dp)
                    .size(8.dp)
                    .background(Farben.Signal, CircleShape),
            )
        }
    }
}

/**
 * Der Hinweis über den Reitern, solange die Karte etwas wählt — was ein Tipp
 * gerade tut, und der Weg zurück zur Seite.
 */
@Composable
private fun Modushinweis(welt: Welt, zustand: Weltzustand, werkbank: Werkbank) {
    val text = when (werkbank.modus) {
        Kartenmodus.Normal -> return
        Kartenmodus.Ort -> when (werkbank.ortZweck) {
            "zweig" -> "Tipp auf die Karte: Hier entsteht die nächste Leitstelle."
            "poi" -> "Tipp auf die Karte: Hier liegt dein Punkt."
            else -> "Tipp auf die Karte: Hier entsteht die Wache. Der gestrichelte Kreis ist dein Baugebiet."
        }
        Kartenmodus.Gelaende -> {
            val noetig = if (werkbank.eckenStrecke) 2 else 3
            val n = werkbank.ecken.size
            val mass = if (werkbank.eckenStrecke) laengenwort(zuglaengeMeter(werkbank.ecken.map { it.lat to it.lon }))
            else flaechenwort(vieleckQuadratmeter(werkbank.ecken.map { it.lat to it.lon }))
            if (n < noetig) "$n von mindestens $noetig Punkten — tipp weiter auf die Karte."
            else "$n Punkte · $mass"
        }
        Kartenmodus.Pfad -> {
            val n = werkbank.pfad.size
            if (n == 0) "Tipp auf die Karte: Jeder Tipp ist eine Station der Streife."
            else "$n ${if (n == 1) "Station" else "Stationen"} — zuletzt ${werkbank.pfad.last().name}"
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .padding(horizontal = Abstand.Klein)
            .fillMaxWidth()
            .flaeche(farbe = Farben.FlaecheHoch.copy(alpha = 0.96f), randfarbe = Farben.AmberTief, ecke = 12.dp)
            .padding(Abstand.Normal),
    ) {
        Text(text, style = Schrift.Klein, color = Farben.Text)
        Umbruchreihe {
            if (werkbank.modus == Kartenmodus.Gelaende && werkbank.ecken.isNotEmpty()) {
                Knopf("Letzten Punkt weg", { werkbank.ecken = werkbank.ecken.dropLast(1) }, kompakt = true, art = Knopfart.Leise)
            }
            if (werkbank.modus == Kartenmodus.Pfad && werkbank.pfad.isNotEmpty()) {
                Knopf("Letzte Station weg", { werkbank.pfad = werkbank.pfad.dropLast(1) }, kompakt = true, art = Knopfart.Leise)
            }
            Knopf(
                if (werkbank.modus == Kartenmodus.Ort) "Abbrechen" else "Fertig",
                {
                    werkbank.modus = Kartenmodus.Normal
                    werkbank.zurueckZurSeite()
                },
                kompakt = true,
                art = if (werkbank.modus == Kartenmodus.Ort) Knopfart.Leise else Knopfart.Haupt,
            )
        }
    }
}

/** Ein angetipptes eigenes Fahrzeug — der Weg steht auf der Karte, das Nähere hier. */
@Composable
private fun Fahrzeugchip(zustand: Weltzustand, werkbank: Werkbank) {
    val f = zustand.fahrzeuge.firstOrNull { it.id == werkbank.gewaehltesFahrzeug } ?: return
    val lage = zustand.betrieb?.lagen?.firstOrNull { it.id == f.lageId }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = Abstand.Klein)
            .fillMaxWidth()
            .flaeche(farbe = Farben.FlaecheHoch.copy(alpha = 0.96f), ecke = 12.dp)
            .padding(Abstand.Normal),
    ) {
        Fmsplakette(f.status)
        Column(Modifier.weight(1f)) {
            Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(f.typ, taetigkeit(zustand, f), lage?.stichwort).joinToString(" · "),
                style = Schrift.Winzig,
                color = Farben.TextSehrLeise,
                maxLines = 2,
            )
        }
        Knopf("Öffnen", {
            werkbank.offenesFahrzeug = f.id
            werkbank.seite = Werkzeug.Fahrzeuge
        }, kompakt = true)
    }
}

/** „Mehr“ — die Werkzeuge ohne eigenen Reiter (`MehrBlende.vue`). */
@Composable
private fun MehrSeite(zustand: Weltzustand, werkbank: Werkbank, beiVerlassen: () -> Unit) {
    val erklaerung = mapOf(
        Werkzeug.Grosslage to "Die eine Lage der Woche — Zeit, Ort und Bereitstellung.",
        Werkzeug.Chat to "Mit allen in der Welt schreiben — und das Funkgerät.",
        Werkzeug.Kasse to "Woher das Geld kommt, wohin es geht — und der Kredit.",
        Werkzeug.Rangliste to "Wer diese Woche am meisten verdient hat.",
        Werkzeug.Laufbahn to "Alle Stufen — was wann freischaltet.",
        Werkzeug.Leihe to "Fahrzeuge mieten — oder eigene gegen Credits einstellen.",
        Werkzeug.Einstellungen to "Gangart und Karte — wie viel gleichzeitig los ist.",
    )
    Werkzeug.HINTER_MEHR.forEach { w ->
        val zusatz = when {
            w == Werkzeug.Grosslage && zustand.grossAktiv -> "läuft"
            w == Werkzeug.Chat && zustand.ungelesen > 0 -> "${zustand.ungelesen} neu"
            w == Werkzeug.Chat && zustand.angepingt != null -> "erwähnt"
            else -> null
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .flaeche(ecke = 12.dp)
                .clickable { werkbank.seite = w }
                .padding(Abstand.Normal),
        ) {
            Icon(w.zeichen, contentDescription = null, tint = Farben.TextLeise, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(w.name_, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                Text(erklaerung[w].orEmpty(), style = Schrift.Winzig, color = Farben.TextSehrLeise)
            }
            zusatz?.let { Weltmarke(it, Farben.SignalHell) }
        }
    }
    Knopf("World verlassen", beiVerlassen, art = Knopfart.Leise, breit = true)
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

