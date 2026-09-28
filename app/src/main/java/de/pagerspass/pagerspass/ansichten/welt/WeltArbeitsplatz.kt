package de.pagerspass.pagerspass.ansichten.welt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltablage
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.WeltFremdwache
import de.pagerspass.pagerspass.netz.WeltStreifenstation
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.karte.Kartenrand
import de.pagerspass.pagerspass.ui.karte.Kartenzustand
import de.pagerspass.pagerspass.ui.karte.rememberKartenzustand
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Was die Blenden des Arbeitsplatzes an ihm auslösen dürfen — Seiten öffnen, auf die
 * Karte schauen, Modi der Karte setzen.
 *
 * Eine Sammlung statt zwölf Parametern je Blende: Die Blenden sind Geschwister der Karte,
 * keine Kinder. Was beide brauchen, hält der Arbeitsplatz (dieselbe Aufteilung wie
 * `WeltView.vue`).
 */
class Weltgriffe(
    /** Eine Seite aufschlagen (oder zuklappen, wenn sie schon offen ist). */
    val oeffnen: (Werkzeug) -> Unit,
    /** Auf einen Punkt schauen — die Karte springt dorthin. */
    val hinschauen: (Double, Double) -> Unit,
    /** Die Seite einer eigenen Wache aufschlagen. */
    val wacheOeffnen: (String) -> Unit,
    /** Ein Profil aufschlagen — nach Benutzername. */
    val profil: (String) -> Unit,
    /** Die Icon-Bibliothek. */
    val icons: () -> Unit,
    /** Die Welt verlassen. */
    val hinaus: () -> Unit,
    /** Der Baumodus: an oder aus — und für was (`wache`, `zweig`, `poi`). */
    val bauModus: (Boolean, String?) -> Unit,
    /** Am Handy: Seite zu, Karte frei, Baumodus an — der nächste Tipp setzt den Punkt. */
    val ortWaehlen: () -> Unit,
    /** Die Karte freigeben, ohne einen Modus zu bestellen (Streife, Gelände). */
    val karteFreigeben: () -> Unit,
    /** Der Streifenpfad-Modus — mit der Wache, von der aus die Streife losfährt. */
    val pfadModus: (Boolean, Pair<Double, Double>?) -> Unit,
    /** Den Pfad ersetzen (umsortieren, entfernen, laden). */
    val pfadSetzen: (List<WeltStreifenstation>) -> Unit,
    /** Der Geländemodus — Fläche oder Strecke. */
    val gelaendeModus: (Boolean, Boolean) -> Unit,
    /** Die Ecken ersetzen (zurück, leeren). */
    val gelaendeSetzen: (List<Pair<Double, Double>>) -> Unit,
    /** Die Karte selbst — für die Ansicht, die die Einstellungen umschalten. */
    val karte: Kartenzustand,
)

/**
 * Der Arbeitsplatz der einen Welt am Handy — übertragen aus `views/WeltView.vue`
 * (Handyform).
 *
 * <b>Dort gibt es keine Fenster, sondern Reiter und Seiten:</b> unten eine Leiste mit fünf
 * Zielen (`Welttableiste.vue`), oben ein Streifen mit dem Stand (`Weltkopf.vue`), und wer
 * einen Reiter antippt, bekommt eine formatfüllende Seite (`Weltseite.vue`) — genau eine,
 * nie zwei.
 *
 * <b>Man kommt auf der Karte an.</b> „Eine Karte, alle Leitstellen" steht auf dem Eintrag,
 * der hierher führt; die Lagen sind einen Reiter entfernt, und ihr Reiter trägt die Zahl
 * der offenen.
 */
@Composable
fun WeltArbeitsplatz(
    welt: Welt,
    kennung: String?,
    katalog: Katalog?,
    beiGruendung: () -> Unit,
    beiHinaus: () -> Unit,
    beiProfil: (String) -> Unit,
    beiIcons: () -> Unit,
) {
    val stand by welt.zustand.collectAsState()
    val bereich = rememberCoroutineScope()
    val zusammenhang = LocalContext.current
    val weltablage = remember { Weltablage(zusammenhang) }
    val dichte = LocalDensity.current
    val lebenslauf = LocalLifecycleOwner.current

    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val unten = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val kopfhoehe = oben + KOPFHOEHE
    val barhoehe = unten + BARHOEHE

    val karte = rememberKartenzustand(stand.stand?.lat ?: 51.2, stand.stand?.lon ?: 10.4, if (stand.stand != null) 11.0 else 6.0)

    // --------------------------------------------------------------- Betreten
    var versuch by remember { mutableIntStateOf(0) }
    LaunchedEffect(kennung, versuch) {
        welt.kennungSetzen(kennung)
        if (kennung == null) return@LaunchedEffect
        when (welt.betreten()) {
            Welt.Betreten.Gruendung -> beiGruendung()
            else -> Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose { welt.verlassen() }
    }

    // Im Hintergrund ruht der Takt; beim Zurückkommen wird sofort geladen.
    DisposableEffect(lebenslauf) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            when (ereignis) {
                Lifecycle.Event.ON_START -> welt.sichtbarkeit(true)
                Lifecycle.Event.ON_STOP -> welt.sichtbarkeit(false)
                else -> Unit
            }
        }
        lebenslauf.lifecycle.addObserver(beobachter)
        onDispose { lebenslauf.lifecycle.removeObserver(beobachter) }
    }

    // ----------------------------------------------------------------- Ebenen
    var ebenen by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    LaunchedEffect(Unit) { ebenen = weltablage.ebenen() }

    // ----------------------------------------------------------------- Seiten
    var seiteId by rememberSaveable { mutableStateOf<String?>(null) }
    val seite = Werkzeug.von(seiteId)

    var bauModus by remember { mutableStateOf(false) }
    var bauZweck by remember { mutableStateOf("wache") }
    var bauort by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var pfadModus by remember { mutableStateOf(false) }
    var pfad by remember { mutableStateOf<List<WeltStreifenstation>>(emptyList()) }
    var pfadStart by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var gelaendeModus by remember { mutableStateOf(false) }
    var gelaende by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var gelaendeStrecke by remember { mutableStateOf(false) }
    var gewaehlteWache by rememberSaveable { mutableStateOf<String?>(null) }
    var fremdwache by remember { mutableStateOf<WeltFremdwache?>(null) }

    val rand = with(dichte) { Kartenrand(oben = kopfhoehe.toPx(), unten = barhoehe.toPx()) }

    fun schliessen(was: Werkzeug) {
        if (Werkzeug.von(seiteId) == was) seiteId = null
        if (was == Werkzeug.Bauen) bauModus = false
    }

    fun umschalten(was: Werkzeug) {
        if (Werkzeug.von(seiteId) == was) {
            schliessen(was)
            return
        }
        seiteId = was.id
        // Wer eine andere Seite aufschlägt, sucht keinen Standort mehr.
        if (was != Werkzeug.Bauen && was != Werkzeug.Fahrzeugkauf) bauModus = false
        if (was == Werkzeug.Grosslage) bereich.launch { welt.grosslageLaden() }
    }

    /**
     * Der Weg zurück aus einer Seite — eine Ebene, nicht bis nach Hause: Wer über „Mehr"
     * in die Kasse gegangen ist, will dorthin zurück.
     */
    fun seiteSchliessen(was: Werkzeug) {
        schliessen(was)
        if (was in Werkzeug.HINTER_MEHR) umschalten(Werkzeug.Mehr)
    }

    val griffe = Weltgriffe(
        oeffnen = { umschalten(it) },
        hinschauen = { lat, lon -> karte.hinschauen(lat, lon, maxOf(karte.zoom, 13.0), rand) },
        wacheOeffnen = { id ->
            gewaehlteWache = id
            seiteId = Werkzeug.Wachenseite.id
        },
        profil = beiProfil,
        icons = beiIcons,
        hinaus = beiHinaus,
        bauModus = { an, zweck ->
            bauModus = an
            if (an && zweck != null) bauZweck = zweck
        },
        ortWaehlen = {
            seiteId = null
            bauModus = true
        },
        karteFreigeben = { seiteId = null },
        pfadModus = { an, start ->
            pfadModus = an
            pfadStart = if (an) start else null
            if (!an) pfad = emptyList()
        },
        pfadSetzen = { pfad = it },
        gelaendeModus = { an, strecke ->
            gelaendeModus = an
            gelaendeStrecke = an && strecke
        },
        gelaendeSetzen = { gelaende = it },
        karte = karte,
    )

    val modi = Kartenmodi(
        bauModus = bauModus,
        bauZweck = bauZweck,
        bauort = bauort,
        pfadModus = pfadModus,
        pfad = pfad,
        pfadStart = pfadStart,
        gelaendeModus = gelaendeModus,
        gelaende = gelaende,
        gelaendeStrecke = gelaendeStrecke,
        seiteOffen = seite != null,
    )

    // ------------------------------------------------------------ Einführung
    var geschafft by remember { mutableIntStateOf(0) }
    var beendet by remember { mutableStateOf(true) }
    var einfuehrungBereit by remember { mutableStateOf(false) }

    LaunchedEffect(kennung) {
        val k = kennung ?: return@LaunchedEffect
        val (g, b) = weltablage.einfuehrung(k)
        geschafft = g.coerceIn(0, LEKTIONEN.size)
        beendet = b
        einfuehrungBereit = true
    }

    val lernstand = Einfuehrungsstand(
        wachen = stand.stand?.wachen?.size ?: 0,
        fahrzeuge = stand.betrieb?.fahrzeuge?.size ?: 0,
        unterwegs = stand.betrieb?.fahrzeuge?.count { it.lage != "Wache" } ?: 0,
        offen = listOfNotNull(seite),
    )

    fun einfuehrungSichern() {
        val k = kennung ?: return
        bereich.launch { weltablage.einfuehrungSetzen(k, geschafft, beendet) }
    }

    // Wer die Welt schon eingerichtet hat, soll nicht bei „Baue deine erste Wache"
    // anfangen — jeder neue Stand prüft alles auf einmal.
    LaunchedEffect(einfuehrungBereit, lernstand) {
        if (!einfuehrungBereit || beendet) return@LaunchedEffect
        val vorher = geschafft
        while (geschafft < LEKTIONEN.size && LEKTIONEN[geschafft].erledigt(lernstand)) geschafft++
        if (geschafft != vorher) einfuehrungSichern()
    }

    val lektion = if (einfuehrungBereit && !beendet) LEKTIONEN.getOrNull(geschafft) else null
    val einfuehrungFertig = einfuehrungBereit && !beendet && geschafft >= LEKTIONEN.size

    // Beim Standortwählen tritt die Einführung zur Seite — beide lägen sonst auf demselben
    // Streifen über der Reiterleiste.
    val standortWaehlen = (bauModus || gelaendeModus) && seite == null

    BackHandler(enabled = seite != null) {
        seite?.let { seiteSchliessen(it) }
    }

    // ------------------------------------------------------------------ Bild
    Box(modifier = Modifier.fillMaxSize().background(Farben.BgTief)) {
        Weltkarte(
            welt = welt,
            stand = stand,
            karte = karte,
            katalog = katalog,
            ebenen = ebenen,
            beiEbene = { id, an ->
                ebenen = ebenen + (id to an)
                val neu = ebenen
                bereich.launch { weltablage.ebenenSetzen(neu) }
            },
            modi = modi,
            rand = rand,
            randOben = kopfhoehe,
            randUnten = barhoehe,
            beiLage = { id ->
                welt.lageWaehlen(id)
                if (seite != Werkzeug.Lagen) umschalten(Werkzeug.Lagen)
            },
            beiBauort = { lat, lon ->
                bauort = lat to lon
                // Am Handy führt das zurück in die Bauen-Seite: Dort stehen Name, Bauart und
                // Knopf.
                if (seite != Werkzeug.Bauen) umschalten(Werkzeug.Bauen)
            },
            beiPfadpunkt = { lat, lon ->
                bereich.launch {
                    // Benannt wird auf dem Server — er kennt Sonderobjekte, Kliniken und
                    // Straßen. Schlägt das fehl, bekommt der Punkt seine Koordinate.
                    var name = String.format(Locale.US, "%.4f / %.4f", lat, lon)
                    val k = kennung
                    if (k != null) {
                        runCatching { welt.wege.ortsname(k, lat, lon).name }
                            .getOrNull()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { name = it }
                    }
                    pfad = pfad + WeltStreifenstation(name, lat, lon)
                    // Zurück in die Fahrzeugseite: Dort steht die Liste, die gerade wächst.
                    if (seiteId != Werkzeug.Fahrzeuge.id) umschalten(Werkzeug.Fahrzeuge)
                }
            },
            beiGelaendeecke = { lat, lon ->
                // Zwölf Ecken, dann ist Schluss — der Deckel steht auch auf dem Server.
                if (gelaende.size < 12) gelaende = gelaende + (lat to lon)
            },
            beiGelaendeZurueck = { gelaende = gelaende.dropLast(1) },
            beiGelaendeFertig = { if (seite != Werkzeug.Bauen) umschalten(Werkzeug.Bauen) },
            beiWache = { id -> griffe.wacheOeffnen(id) },
            beiFremdwache = { id -> fremdwache = stand.betrieb?.fremdeWachen?.firstOrNull { it.id == id } },
        )

        Weltkopf(
            stand = stand,
            oben = oben,
            beiLaufbahn = { umschalten(Werkzeug.Laufbahn) },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Die eine Seite zwischen Kopf und Leiste.
        if (seite != null) {
            Weltseite(
                titel = seite.titel,
                zahl = when (seite) {
                    Werkzeug.Lagen -> stand.offene
                    Werkzeug.Fahrzeuge -> stand.freieFahrzeuge.size
                    else -> null
                },
                beiSchliessen = { seiteSchliessen(seite) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = kopfhoehe, bottom = barhoehe),
            ) {
                when (seite) {
                    Werkzeug.Lagen -> WeltLagenBlende(welt, stand, katalog, griffe)
                    Werkzeug.Fahrzeuge -> WeltFahrzeugBlende(welt, stand, katalog, modi, griffe)
                    Werkzeug.Fahrzeugkauf -> WeltBauBlende(welt, stand, katalog, modi, griffe, nurFahrzeug = true)
                    Werkzeug.Wachen -> WeltWachenBlende(welt, stand, griffe)
                    Werkzeug.Wachenseite -> WeltWachenseiteBlende(welt, stand, katalog, gewaehlteWache, griffe)
                    Werkzeug.Chat -> WeltChatBlende(welt, stand, griffe)
                    Werkzeug.Grosslage -> WeltGrosslageBlende(welt, stand, griffe)
                    Werkzeug.Kasse -> WeltKasseBlende(welt, stand)
                    Werkzeug.Rangliste -> WeltRanglisteBlende(welt, stand, griffe)
                    Werkzeug.Laufbahn -> WeltLaufbahnBlende(welt, stand)
                    Werkzeug.Einstellungen -> WeltEinstellungBlende(welt, stand, griffe, ebenen) { id, an ->
                        ebenen = ebenen + (id to an)
                        val neu = ebenen
                        bereich.launch { weltablage.ebenenSetzen(neu) }
                    }
                    Werkzeug.Leihe -> WeltLeiheBlende(welt, stand, katalog, griffe)
                    Werkzeug.Mehr -> WeltMehrBlende(stand, griffe)
                    Werkzeug.Bauen -> WeltBauBlende(welt, stand, katalog, modi, griffe, nurFahrzeug = false)
                }
            }
        }

        // Der erste Abruf läuft noch — ein kurzer Hinweis statt einer Karte, auf der
        // „nichts los" und „noch nicht geladen" gleich aussehen.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = kopfhoehe + 60.dp, start = Abstand.Gross, end = Abstand.Gross),
        ) {
            val fehler = stand.fehler
            val standFehler = stand.standFehler
            when {
                stand.laedt && stand.betrieb == null -> Streifen("Die Welt wird geladen …", Farben.Text)
                // Der Takt ist gerissen — Netz weg, Server weg, Premium abgelaufen.
                fehler != null -> Streifen("$fehler — es wird weiter versucht.", Farben.SignalHell)
                // Schon der Stand kam nicht: Hier läuft noch kein Takt, der es weiter versucht.
                standFehler != null && !stand.hatLeitstelle -> {
                    Streifen(standFehler, Farben.SignalHell)
                    Knopf("Erneut versuchen", { versuch++ }, kompakt = true)
                }
            }
        }

        if (!standortWaehlen) {
            if (lektion != null || einfuehrungFertig) {
                Einfuehrungskarte(
                    lektion = lektion,
                    fertig = einfuehrungFertig,
                    nummer = minOf(geschafft + 1, LEKTIONEN.size),
                    beiWeiter = {
                        if (geschafft < LEKTIONEN.size) geschafft++
                        einfuehrungSichern()
                    },
                    beiSchliessen = {
                        beendet = true
                        einfuehrungSichern()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = barhoehe + Abstand.Normal, start = Abstand.Normal, end = Abstand.Normal),
                )
            }
        }

        Welttableiste(
            offen = seite,
            stand = stand,
            bauModus = bauModus || gelaendeModus,
            nochNichts = (stand.stand?.wachen?.size ?: 0) == 0,
            zeigt = lektion?.ziel,
            unten = unten,
            beiWahl = { umschalten(it) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    fremdwache?.let { w ->
        FremdwacheDialog(wache = w, beiProfil = beiProfil, beiSchliessen = { fremdwache = null })
    }
}

/** Der Kopfstreifen baut 48 Punkte hoch — zwei Zeilen plus Luft. */
private val KOPFHOEHE = 48.dp

/** Die Reiterleiste — 64 Punkte, die Leiste der App ist 70. */
private val BARHOEHE = 64.dp

@Composable
private fun Streifen(text: String, farbe: Color) {
    Text(
        text = text,
        style = Schrift.MonoKlein,
        color = farbe,
        modifier = Modifier
            .background(Farben.Flaeche.copy(alpha = 0.95f), RoundedCornerShape(9.dp))
            .border(1.dp, Farben.Rand, RoundedCornerShape(9.dp))
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    )
}

// ------------------------------------------------------------------ Kopf

/**
 * Der Kopfstreifen — übertragen aus `Weltkopf.vue`: Name und Kreis links, Guthaben und
 * Stufe rechts, die Erfahrung als Kante unten.
 *
 * <b>Die Stufe ist der Griff zur Laufbahn</b> — nicht der Balken: Der liegt als vier
 * Punkte hohe Kante auf der Unterkante, und mit dem Finger trifft man ihn nicht.
 *
 * <b>Eine Gutschrift leuchtet vier Sekunden grün auf</b>; zwei kurz nacheinander
 * addieren sich, sonst überschriebe die zweite die erste, bevor sie jemand gelesen hat.
 */
@Composable
private fun Weltkopf(stand: Weltzustand, oben: Dp, beiLaufbahn: () -> Unit, modifier: Modifier = Modifier) {
    val quelle = stand.betrieb
    val erfahrung = quelle?.erfahrung ?: stand.stand?.erfahrung ?: 0
    val ab = quelle?.erfahrungStufeAb ?: stand.stand?.erfahrungStufeAb ?: 0
    val bis = if (quelle != null) quelle.erfahrungStufeBis else stand.stand?.erfahrungStufeBis
    val hoechste = bis == null || bis <= ab
    val anteil = if (hoechste) 1f else ((erfahrung - ab).coerceAtLeast(0).toFloat() / (bis!! - ab)).coerceIn(0f, 1f)
    val restschuld = quelle?.restschuld ?: 0

    var gezeigt by remember { mutableStateOf<Int?>(null) }
    val gutschrift = stand.gutschrift
    LaunchedEffect(gutschrift) {
        if (gutschrift == null) return@LaunchedEffect
        gezeigt = (gezeigt ?: 0) + gutschrift.betrag
        delay(4_000)
        gezeigt = null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Farben.FlaecheHoch.copy(alpha = 0.92f), Farben.Bg.copy(alpha = 0.9f)),
                ),
            )
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(Farben.Rand, Offset(0f, size.height - strich / 2f), Offset(size.width, size.height - strich / 2f), strich)
                // Der Erfahrungsbalken liegt auf der Unterkante — die Kante ist ohnehin da.
                drawRect(
                    if (hoechste) Farben.GruenHell else Farben.Amber,
                    topLeft = Offset(0f, size.height - 3.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width * anteil, 3.dp.toPx()),
                )
            }
            .padding(top = oben)
            .height(KOPFHOEHE)
            .padding(horizontal = Abstand.Klein),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            val s = stand.stand
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                if (s != null) {
                    Text(
                        text = s.name,
                        style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    s.kreis?.let {
                        Text(text = it, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    Credits(stand.guthaben, stil = Schrift.MonoNormal, zeichengroesse = 13.dp)
                    gezeigt?.let {
                        Text(text = "+${zahl(it)}", style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.GruenHell)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier.clickable(onClick = beiLaufbahn),
                ) {
                    if (restschuld > 0) {
                        Text(text = "−${zahl(restschuld)}", style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.Amber)
                    }
                    Text(text = "Stufe ${stand.stufe}", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Seite

/**
 * Eine formatfüllende Seite — übertragen aus `Weltseite.vue`: ein Pfeil zurück, der Titel,
 * die Zahl, darunter der Inhalt.
 *
 * <b>Ein Pfeil und kein Kreuz.</b> Zurück führt nicht ins Nichts, sondern auf die Karte —
 * „Schließen" beschriebe ein Fenster; das hier ist eine Ebene tiefer.
 */
@Composable
private fun Weltseite(
    titel: String,
    zahl: Int?,
    beiSchliessen: () -> Unit,
    modifier: Modifier = Modifier,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.background(Farben.Bg)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.Flaeche)
                .drawBehind {
                    val strich = 1.dp.toPx()
                    drawLine(Farben.Rand, Offset(0f, size.height - strich / 2f), Offset(size.width, size.height - strich / 2f), strich)
                }
                .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = beiSchliessen),
            ) {
                Icon(
                    imageVector = Weltzeichen.Zurueck,
                    contentDescription = "Zurück zur Karte",
                    tint = Farben.TextLeise,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = titel,
                style = Schrift.Gross,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (zahl != null && zahl > 0) {
                Text(
                    text = zahl.toString(),
                    style = Schrift.MarkeZahl,
                    color = Farben.AufFarbe,
                    modifier = Modifier
                        .padding(end = Abstand.Klein)
                        .background(Farben.Amber, CircleShape)
                        .padding(horizontal = Abstand.Klein, vertical = 1.dp),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(Abstand.Gross),
            content = inhalt,
        )
    }
}

// ------------------------------------------------------------ Reiterleiste

/**
 * Die fünf Reiter — übertragen aus `Welttableiste.vue`.
 *
 * Die Zahlmarke ist gefüllt; sie wird rot, sobald eine Lage auf eine Entscheidung wartet,
 * die nur diese Leitstelle treffen kann. Der Großeinsatz hat keinen eigenen Reiter — dass
 * er läuft, steht als Punkt an „Mehr". „Bauen" ruft, solange noch keine Wache steht, und
 * der Reiter, auf den die Einführung zeigt, leuchtet.
 */
@Composable
private fun Welttableiste(
    offen: Werkzeug?,
    stand: Weltzustand,
    bauModus: Boolean,
    nochNichts: Boolean,
    zeigt: Werkzeug?,
    unten: Dp,
    beiWahl: (Werkzeug) -> Unit,
    modifier: Modifier = Modifier,
) {
    val grossAktiv = stand.grosslage?.zustand == "Laeuft"
    val jetzt by androidx.compose.runtime.produceState(0L) {
        while (true) {
            delay(100)
            value = System.currentTimeMillis()
        }
    }

    fun hier(w: Werkzeug): Boolean = when {
        w == Werkzeug.Bauen && bauModus -> true
        w == Werkzeug.Mehr && offen != null && offen in Werkzeug.HINTER_MEHR -> true
        else -> offen == w
    }

    val zeigtAuf = if (zeigt != null && zeigt in Werkzeug.HINTER_MEHR) Werkzeug.Mehr else zeigt

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFA263344), Color(0xFA101823))))
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(Farben.RandHell.copy(alpha = 0.76f), Offset(0f, strich / 2f), Offset(size.width, strich / 2f), strich)
            }
            .padding(bottom = unten)
            .height(BARHOEHE)
            .padding(horizontal = 4.dp),
    ) {
        Werkzeug.HANDY_REITER.forEach { w ->
            val an = hier(w)
            val ruft = w == Werkzeug.Bauen && nochNichts && !an
            val leuchtet = zeigtAuf == w
            val marke = when (w) {
                Werkzeug.Lagen -> stand.offene
                Werkzeug.Fahrzeuge -> stand.freieFahrzeuge.size
                else -> 0
            }
            Reiterknopf(
                werkzeug = w,
                an = an,
                ruft = ruft,
                leuchtet = leuchtet,
                marke = marke,
                markeRuft = w == Werkzeug.Lagen && stand.meineOffenen > 0,
                punkt = w == Werkzeug.Mehr && grossAktiv,
                jetzt = jetzt,
                beiDruck = { beiWahl(w) },
            )
        }
    }
}

@Composable
private fun RowScope.Reiterknopf(
    werkzeug: Werkzeug,
    an: Boolean,
    ruft: Boolean,
    leuchtet: Boolean,
    marke: Int,
    markeRuft: Boolean,
    punkt: Boolean,
    jetzt: Long,
    beiDruck: () -> Unit,
) {
    val farbe = when {
        an -> Farben.Amber
        leuchtet -> Farben.AmberHell
        ruft -> Farben.Amber
        else -> Farben.TextLeise
    }
    // Der Reiter, auf den die Einführung zeigt, pulsiert; „Bauen" ruft, solange nichts steht.
    val puls = if (leuchtet || ruft) {
        val periode = if (leuchtet) 1_800L else 2_400L
        ((jetzt % periode).toFloat() / periode)
    } else {
        -1f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(vertical = 6.dp)
            .drawBehind {
                val ecke = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx())
                if (an) {
                    drawRoundRect(Farben.HauchAmber, cornerRadius = ecke)
                    drawRoundRect(Farben.AmberTief, cornerRadius = ecke, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    drawRoundRect(
                        Farben.Amber,
                        topLeft = Offset(size.width * 0.39f, size.height - 6.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.22f, 3.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    )
                }
                if (puls >= 0f) {
                    val weite = (if (leuchtet) 14.dp.toPx() else 10.dp.toPx()) * puls
                    drawRoundRect(
                        Farben.Amber.copy(alpha = 0.5f * (1f - puls)),
                        topLeft = Offset(-weite, -weite),
                        size = androidx.compose.ui.geometry.Size(size.width + 2 * weite, size.height + 2 * weite),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx() + weite, 14.dp.toPx() + weite),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
                    )
                }
            }
            .clickable(onClick = beiDruck),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(imageVector = werkzeug.zeichen, contentDescription = null, tint = farbe, modifier = Modifier.size(22.dp))
            Text(text = werkzeug.kurz, style = Schrift.Weg, color = farbe, maxLines = 1, overflow = TextOverflow.Clip)
        }
        if (marke > 0) {
            Text(
                text = marke.toString(),
                style = Schrift.MarkeZahl,
                color = Farben.AufFarbe,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 30.dp, top = 2.dp)
                    .defaultMinSize(minWidth = 20.dp)
                    .background(if (markeRuft) Farben.Signal else Farben.Amber, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        } else if (punkt) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 22.dp, top = 6.dp)
                    .size(8.dp)
                    .background(Farben.Signal, CircleShape),
            )
        }
    }
}

// --------------------------------------------------------------- Einführung

/** Was die Einführung prüft — wie viele Wachen, Fahrzeuge, Fahrten, und was offen ist. */
data class Einfuehrungsstand(
    val wachen: Int,
    val fahrzeuge: Int,
    val unterwegs: Int,
    val offen: List<Werkzeug>,
)

/** Eine Lektion: Titel, Text, der Reiter, auf den sie zeigt, und wann sie erledigt ist. */
class Lektion(
    val id: String,
    val titel: String,
    val text: String,
    val ziel: Werkzeug,
    val erledigt: (Einfuehrungsstand) -> Boolean,
)

/** Die fünf Schritte — übertragen aus `components/welt/einfuehrung.ts`. */
val LEKTIONEN: List<Lektion> = listOf(
    Lektion(
        id = "wache",
        titel = "Baue deine erste Wache",
        text = "Ohne Wache passiert in der Welt nichts: Erst wo eine steht, entstehen Lagen in " +
            "der Umgebung — und je mehr Wachen du hast, desto mehr davon. Öffne „Bauen\", drück " +
            "auf „Bauen\", wähle eine Bauart und tipp dann auf die Karte, wo sie stehen soll.",
        ziel = Werkzeug.Bauen,
        erledigt = { it.wachen > 0 },
    ),
    Lektion(
        id = "fahrzeug",
        titel = "Stell ein Fahrzeug hinein",
        text = "Eine leere Wache rückt nicht aus. Im Reiter „Fahrzeug\" derselben Blende kaufst " +
            "du das erste — ein Löschfahrzeug ist der Anfang, alles Weitere schaltet sich mit " +
            "deiner Stufe frei.",
        ziel = Werkzeug.Bauen,
        erledigt = { it.fahrzeuge > 0 },
    ),
    Lektion(
        id = "alarm",
        titel = "Alarmiere deine erste Lage",
        text = "Unter „Lagen\" stehen deine Einsätze, der nächstgelegene oben. Zeile antippen, " +
            "Fahrzeug anhaken, alarmieren. Was in deinem Bereich entsteht, siehst zuerst nur " +
            "du — schaffst du es nicht allein, gibst du die Lage für alle frei.",
        ziel = Werkzeug.Lagen,
        erledigt = { it.unterwegs > 0 },
    ),
    Lektion(
        id = "unterwegs",
        titel = "Behalte die Anfahrt im Blick",
        text = "Unter „Fahrzeuge\" siehst du, was jeder Wagen gerade tut und wie lange er noch " +
            "braucht. Auf der Karte fährt er die echte Straße — deshalb entscheidet der " +
            "Standort deiner Wache über alles.",
        ziel = Werkzeug.Fahrzeuge,
        erledigt = { Werkzeug.Fahrzeuge in it.offen },
    ),
    Lektion(
        id = "grosslage",
        titel = "Der Großeinsatz der Woche",
        text = "Einmal in der Woche gibt es eine Lage, die niemand allein deckt — für alle " +
            "dieselbe. Unter „Großeinsatz\" steht, wann und wo es losgeht und wie du Fahrzeuge " +
            "vorher schon hinschickst.",
        ziel = Werkzeug.Grosslage,
        erledigt = { Werkzeug.Grosslage in it.offen },
    ),
)

/**
 * Die Karte der Einführung — übertragen aus `WeltEinfuehrung.vue`.
 *
 * <b>Zwei Wege hinaus statt eineinhalb.</b> „Diesen Schritt überspringen" geht eine
 * Lektion weiter; „Nicht mehr zeigen" macht Schluss — als Wort, nicht nur als kleines ✕.
 */
@Composable
private fun Einfuehrungskarte(
    lektion: Lektion?,
    fertig: Boolean,
    nummer: Int,
    beiWeiter: () -> Unit,
    beiSchliessen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .background(Farben.Flaeche.copy(alpha = 0.97f), RoundedCornerShape(14.dp))
            .border(1.dp, Farben.AmberTief, RoundedCornerShape(14.dp))
            .padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text(
                text = if (fertig) "Einführung" else "Schritt $nummer von ${LEKTIONEN.size}",
                style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG),
                color = Farben.Amber,
            )
            // Die Punkte sind Anzeige und kein Weg.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                LEKTIONEN.forEachIndexed { i, _ ->
                    val durch = fertig || i < nummer - 1
                    val jetztDran = !fertig && i == nummer - 1
                    Box(
                        Modifier
                            .size(if (jetztDran) 8.dp else 6.dp)
                            .background(
                                when {
                                    jetztDran -> Farben.Amber
                                    durch -> Farben.AmberTief
                                    else -> Farben.Rand
                                },
                                CircleShape,
                            ),
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).clickable(onClick = beiSchliessen),
            ) {
                Icon(
                    imageVector = Weltzeichen.Schliessen,
                    contentDescription = "Einführung beenden",
                    tint = Farben.TextSehrLeise,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = if (fertig) "Geschafft — der Rest ist Disponieren" else lektion?.titel.orEmpty(),
            style = Schrift.Gross,
            color = Farben.Text,
        )
        Text(
            text = if (fertig) {
                "Lagen entstehen jetzt von allein rund um deine Wachen. Was du verdienst, steht " +
                    "oben im Kopf; mit der Stufe kommen neue Bauarten, Organisationen und " +
                    "Fahrzeuge dazu."
            } else {
                lektion?.text.orEmpty()
            },
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), verticalAlignment = Alignment.CenterVertically) {
            if (fertig) {
                Knopf("Alles klar", beiSchliessen, art = Knopfart.Haupt, kompakt = true)
            } else {
                Knopf("Diesen Schritt überspringen", beiWeiter, art = Knopfart.Leise, kompakt = true)
                Textweg("Nicht mehr zeigen", beiSchliessen, farbe = Farben.TextSehrLeise)
            }
        }
    }
}

// ----------------------------------------------------------------- Fremdwache

/**
 * Der Steckbrief einer fremden Wache — übertragen aus `FremdwacheDialog.vue`. Mehr gibt
 * eine fremde Wache nicht preis.
 */
@Composable
private fun FremdwacheDialog(wache: WeltFremdwache, beiProfil: (String) -> Unit, beiSchliessen: () -> Unit) {
    Blende(
        titel = wache.name,
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        kopfknoepfe = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
    ) {
        de.pagerspass.pagerspass.ui.bausteine.Etikett(artname(wache.art))
        val besitzer = wache.besitzer
        if (besitzer != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Gehört", style = Schrift.Normal, color = Farben.Text)
                Textweg(besitzer, { beiProfil(besitzer) })
            }
        } else {
            Text(text = "Der Besitzer zeigt seinen Namen nicht.", style = Schrift.Normal, color = Farben.TextSehrLeise)
        }
        Text(
            text = when (wache.fahrzeuge) {
                0 -> "Zurzeit steht dort kein Fahrzeug."
                1 -> "Zurzeit steht dort 1 Fahrzeug."
                else -> "Zurzeit stehen dort ${wache.fahrzeuge} Fahrzeuge."
            },
            style = Schrift.Normal,
            color = Farben.Text,
        )
        Text(
            text = "Mehr gibt eine fremde Wache nicht preis — Fahrzeugliste, Ausbau und Chronik " +
                "sieht nur ihr Besitzer.",
            style = Schrift.Winzig,
            color = Farben.TextSehrLeise,
        )
    }
}
