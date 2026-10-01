package de.pagerspass.pagerspass.ansichten.welt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.WeltPunkt
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Karte
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.zeichen.Zeichen
import kotlinx.coroutines.launch

/**
 * Der Rahmen um PagerSpass - World — das Gegenstück zu `WeltGruendungView.vue`
 * und `WeltView.vue` samt der Weiche davor.
 *
 * <b>Drei Riegel, bevor die Welt aufgeht</b>, in der Reihenfolge des Servers:
 *
 *  1. **Premium.** Ohne aktives Abo antwortet jeder Weg mit 403. Abgeschlossen
 *     wird das Abo nur auf der Webseite — die App öffnet dorthin und verkauft
 *     selbst nichts.
 *  2. **Die Einweisung.** Solange sie aussteht, gibt es nur die
 *     Ausbildungsschicht (dieselbe Regel wie auf dem Startbildschirm).
 *  3. **Die bestätigte E-Mail-Adresse.** Die prüft der Server
 *     (`EmailPflichtFilter`) — sein Satz steht hier so, wie er kommt.
 *
 * Danach entscheidet der Stand: 404 heißt „noch keine Leitstelle“ und führt zur
 * Gründung, sonst an den Arbeitsplatz.
 *
 * <b>Ein Vollbildmodus wie die Runde.</b> Der Rahmen der App nimmt die
 * Tableiste weg, solange die Welt offen ist; die Zurück-Taste führt hinaus.
 */
@Composable
fun WeltRahmen(
    welt: Welt,
    konto: Konto?,
    server: String,
    beiVerlassen: () -> Unit,
    beiKonto: () -> Unit = beiVerlassen,
    /**
     * Ins Profil eines anderen Spielers — der Name in Rangliste, Leihmarkt und
     * an einer fremden Wache führt dorthin (siehe `LocalWeltProfil`).
     */
    beiProfil: ((String) -> Unit)? = null,
) {
    val zustand by welt.stand.collectAsStateWithLifecycle()
    val browser = LocalUriHandler.current
    val premium = konto?.premiumAktiv == true
    val einweisung = konto?.einweisungOffen == true

    LaunchedEffect(konto?.kennung, premium, einweisung) {
        val kennung = konto?.kennung ?: return@LaunchedEffect
        if (premium && !einweisung) welt.oeffnen(kennung)
    }

    // Im Hintergrund ruht der Takt — eine Karte, die niemand sieht, braucht
    // keinen Betrieb alle fünf Sekunden.
    val lebenslauf = LocalLifecycleOwner.current
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

    fun shop() = browser.openUri("$server/play/mobile/shop?bereich=premium")

    CompositionLocalProvider(LocalWeltServer provides server, LocalWeltProfil provides beiProfil) {
    when {
        konto == null -> Unit
        !premium -> Weltriegel(
            titel = "Eine Karte, auf der alle spielen",
            text = "Eine Runde ist eine Schicht: Sie fängt an, sie hört auf, danach ist der Kreis wieder leer. " +
                "World ist das Gegenteil davon. Du gründest einmal eine eigene Leitstelle — irgendwo auf der " +
                "Deutschlandkarte, auf der auch alle anderen stehen — und die bleibt.\n\n" +
                "Von da an baust du auf: Wachen setzen, Fahrzeuge kaufen, an jedem Einsatz verdienen. Lagen " +
                "entstehen rund um deine Wachen; was du nicht selbst schaffst, gibst du für alle frei. Einmal in " +
                "der Woche kommt ein Großeinsatz dazu, den keine Leitstelle allein deckt.\n\n" +
                "Der Zugang gehört zu PagerSpass Premium. Das Abo schließt du auf der Webseite ab — in der App " +
                "wird nichts verkauft.",
            beiZurueck = beiVerlassen,
        ) {
            Knopf("Premium auf der Webseite ansehen", { shop() }, art = Knopfart.Haupt, breit = true)
        }

        einweisung -> Weltriegel(
            titel = "Erst die Einweisung",
            text = "Bevor es in die Welt geht, steht die Ausbildungsschicht an — Melder, FMS, " +
                "Notruf und Funk. Danach ist World offen.",
            beiZurueck = beiVerlassen,
        )

        zustand.standFehler != null && !zustand.hatLeitstelle -> {
            val email = zustand.standFehlerStand == 403 &&
                zustand.standFehler?.contains("Mail", ignoreCase = true) == true
            val abo = zustand.standFehlerStand == 403 && !email
            Weltriegel(
                titel = if (email) "E-Mail-Adresse hinterlegen" else "World ist gerade nicht erreichbar",
                text = zustand.standFehler.orEmpty(),
                beiZurueck = beiVerlassen,
            ) {
                when {
                    email -> Knopf("Zum Konto", beiKonto, art = Knopfart.Haupt, breit = true)
                    abo -> Knopf("Premium auf der Webseite ansehen", { shop() }, art = Knopfart.Haupt, breit = true)
                }
                Knopf("Erneut versuchen", { welt.erneutBetreten() }, art = Knopfart.Leise, breit = true)
            }
        }

        !zustand.standGeprueft -> Weltriegel(
            titel = "World",
            text = null,
            beiZurueck = beiVerlassen,
        ) { Ladezeile("Die Welt wird geladen …") }

        !zustand.hatLeitstelle -> {
            BackHandler(onBack = beiVerlassen)
            WeltGruendung(welt = welt, zustand = zustand, beiZurueck = beiVerlassen)
        }

        else -> WeltArbeitsplatz(welt = welt, zustand = zustand, beiVerlassen = beiVerlassen)
    }
    }
}

/** Eine Seite vor der Welt — Premium, Einweisung, E-Mail, Laden. */
@Composable
private fun Weltriegel(
    titel: String,
    text: String?,
    beiZurueck: () -> Unit,
    knoepfe: @Composable () -> Unit = {},
) {
    BackHandler(onBack = beiZurueck)
    Seite(breite = 560.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true)
        }
        Kennung()
        Karte(titel = titel, zeichen = Zeichen.Welt, text = text, haupt = true) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) { knoepfe() }
        }
    }
}

/** „PAGERSPASS · WORLD“ — die Kennung über jeder Seite der Welt. */
@Composable
fun Kennung(modifier: Modifier = Modifier) {
    Text(
        text = "PAGERSPASS · WORLD",
        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
        color = Farben.Amber,
        modifier = modifier
            .background(Farben.HauchAmber, Rundung.Rund)
            .padding(horizontal = Abstand.Klein, vertical = 2.dp),
    )
}

/**
 * Die Gründung — das Gegenstück zu `WeltGruendungView.vue`.
 *
 * <b>Oben die Karte, unten das Blatt</b>: Oben tippt man, unten schreibt man.
 * Die Städte stehen auf der Karte, und die Suche fliegt hin — beides ohne
 * fremden Suchdienst.
 */
@Composable
private fun WeltGruendung(welt: Welt, zustand: Weltzustand, beiZurueck: () -> Unit) {
    val bereich = rememberCoroutineScope()
    val karte = remember { Weltkartenstand(51.2, 10.4, 5.6) }
    val ebenen = remember { Weltebenen() }
    var gewaehlt by remember { mutableStateOf<WeltPunkt?>(null) }
    var name by remember { mutableStateOf("") }
    var suche by remember { mutableStateOf("") }
    var sucheLeer by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    val hoehe = LocalConfiguration.current.screenHeightDp.dp
    val oben = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    fun stadtSuchen(eingabe: String = suche) {
        val stadt = stadtFinden(eingabe)
        sucheLeer = stadt == null && eingabe.isNotBlank()
        if (stadt == null) return
        suche = stadt.name
        gewaehlt = WeltPunkt(stadt.lat, stadt.lon)
        fehler = null
        // Die Stadt kommt oberhalb des Blatts zu stehen, nicht darunter.
        karte.hinschauen(stadt.lat - 0.06, stadt.lon, 11.0)
    }

    Box(Modifier.fillMaxSize().background(Farben.BgTief)) {
        Weltkarte(
            zustand = zustand,
            kartenstand = karte,
            ebenen = ebenen,
            modifier = Modifier.fillMaxSize(),
            modus = Kartenmodus.Ort,
            markiert = gewaehlt,
            staedte = true,
            polster = androidx.compose.foundation.layout.PaddingValues(top = oben),
            beiOrt = { gewaehlt = it; fehler = null },
        )

        Box(Modifier.padding(top = oben + Abstand.Normal, start = Abstand.Normal)) {
            Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = hoehe * 0.58f)
                .flaeche(farbe = Farben.FlaecheHoch.copy(alpha = 0.97f), ecke = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Abstand.Gross)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            Kennung()
            Text("Deine Leitstelle in der Welt", style = Schrift.Titel, color = Farben.Text)
            Leisesatz(
                "Eine Karte, alle Spieler. Wähle einen Ort — er entscheidet über jede Anfahrt und jeden " +
                    "Nachbarn. Es gibt keinen Mindestabstand: Deine Heimatstadt geht auch dann, wenn dort " +
                    "schon jemand sitzt.",
            )
            if (zustand.leitstellen.isNotEmpty()) {
                Leisesatz(
                    "${zustand.leitstellen.size} " +
                        (if (zustand.leitstellen.size == 1) "Leitstelle steht" else "Leitstellen stehen") +
                        " schon auf der Karte — die grauen Punkte.",
                    winzig = true,
                )
            }
            Feld(
                wert = name,
                beiAenderung = { name = it.take(60) },
                etikett = "Name der Leitstelle",
                platzhalter = "Leitstelle Musterstadt",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = suche,
                    beiAenderung = { suche = it; sucheLeer = false },
                    etikett = "Stadt suchen",
                    platzhalter = "z. B. Kassel",
                    weiterTaste = ImeAction.Search,
                    modifier = Modifier.weight(1f),
                )
                Knopf("Hin", { stadtSuchen() }, kompakt = true)
            }
            val vorschlaege = staedteVorschlagen(suche).filter { it.name != suche }
            if (vorschlaege.isNotEmpty()) {
                Umbruchreihe {
                    vorschlaege.forEach { s -> Pille(s.name, false, { stadtSuchen(s.name) }) }
                }
            }
            if (sucheLeer) Leisesatz("Die Stadt kenne ich nicht — zoom hinein und tipp auf die Karte.")
            Text(
                text = gewaehlt?.let { "✓ Standort gesetzt — %.4f, %.4f".format(it.lat, it.lon) }
                    ?: "Noch kein Standort — such eine Stadt oder tipp auf die Karte.",
                style = Schrift.Klein,
                color = if (gewaehlt != null) Farben.AmberHell else Farben.TextLeise,
            )
            Warnsatz(fehler)
            Knopf(
                if (sendet) "Wird gegründet …" else "Leitstelle gründen",
                {
                    val ort = gewaehlt ?: return@Knopf
                    sendet = true
                    fehler = null
                    bereich.launch {
                        fehler = welt.gruenden(name.trim(), ort.lat, ort.lon)
                        sendet = false
                    }
                },
                art = Knopfart.Haupt,
                breit = true,
                aktiv = !sendet && gewaehlt != null && name.trim().length >= 3,
            )
            Leisesatz(
                "Du startest mit 25 000 Welt-Credits. Davon kaufst du Wachen und Fahrzeuge; was du an " +
                    "Lagen verdienst, kommt dazu.",
                winzig = true,
            )
        }
    }
}
