package de.pagerspass.pagerspass.mobil

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pagerspass.pagerspass.ansichten.DebriefingSeite
import de.pagerspass.pagerspass.ansichten.FahrzeugSeite
import de.pagerspass.pagerspass.ansichten.LeitstelleSeite
import de.pagerspass.pagerspass.ansichten.LobbyGriffe
import de.pagerspass.pagerspass.ansichten.LobbySeite
import de.pagerspass.pagerspass.ansichten.Melderblende
import de.pagerspass.pagerspass.ansichten.RegieGriffe
import de.pagerspass.pagerspass.ansichten.Rundentexte
import de.pagerspass.pagerspass.ansichten.ZuschauerSeite
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Spieler
import de.pagerspass.pagerspass.netz.Uebergabe
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay

/**
 * Der Rahmen um die laufende Runde — `RaumView.vue` in der Handyform.
 *
 * <b>Aus `PagerSpassApp.kt` hierher gezogen</b> (Runde, Teil 1), damit die
 * Folgearbeiten an Leitstelle, Fahrzeug und Nachbesprechung nicht alle in der
 * einen heißen Datei landen. Die Weiche selbst ist dieselbe geblieben.
 *
 * <b>Er verteilt nach Zustand und Rolle.</b> Beendet zeigt die Auswertung; die
 * Lobby steht, solange die Runde nicht läuft <b>oder man noch keinen Platz
 * hat</b> — wer mitten im Dienst dazustößt, wählt dort seine Rolle (Lücke A1);
 * danach entscheidet die Rolle zwischen Leitstelle und Fahrzeug.
 *
 * <b>Und er trägt, was über jeder Ansicht liegt:</b> den Melder, die
 * Übergabefrage, das Warnband, das Verbindungsband, die Rückfrage beim
 * Verlassen. Der Bildschirm bleibt an, solange der Dienst läuft.
 */
@Composable
internal fun Rundenrahmen(
    stand: Rundenstand,
    sitzung: Sitzung,
    runde: Runde,
    sozial: Sozial,
) {
    val sitzungsstand by sitzung.stand.collectAsStateWithLifecycle()
    val seiten by sitzung.daten.collectAsStateWithLifecycle()
    val sozialstand by sozial.stand.collectAsStateWithLifecycle()
    val lobbydaten: Lobbydaten = viewModel()
    val lobbystand by lobbydaten.stand.collectAsStateWithLifecycle()
    val zusammenhang = LocalContext.current
    val sicht = LocalView.current
    val lebenslauf = LocalLifecycleOwner.current
    val browser = LocalUriHandler.current
    val toene = remember(zusammenhang) { Rundentoene(zusammenhang) }
    val kennung = sitzungsstand.konto?.kennung.orEmpty()
    var verlassenFragen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sitzung.garageLaden()
        sitzung.katalogSicherstellen()
    }

    val berechtigung = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Meldermeldung.erlaubt(zusammenhang)
        ) {
            berechtigung.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(Unit) {
        runde.beiAlarm = { alarm -> Meldermeldung.zeigen(zusammenhang, alarm) }
        onDispose { runde.beiAlarm = null }
    }

    val raum = stand.raum
    val ich = raum?.players?.firstOrNull { it.id == kennung }
    val laeuft = raum?.laeuft == true

    // Im Dienst bleibt der Bildschirm an — ein Melder, der mitten im Einsatz
    // dunkel wird, ist keiner. Danach darf er wieder ausgehen.
    DisposableEffect(laeuft) {
        sicht.keepScreenOn = laeuft
        onDispose { sicht.keepScreenOn = false }
    }

    // Vorder- und Hintergrund (Lücke A10): Der Server entscheidet daran, ob ein
    // Alarm zusätzlich als Mitteilung kommt. Beim Zurückkommen wird eine
    // abgerissene Leitung sofort neu versucht, statt die Staffel abzuwarten.
    DisposableEffect(lebenslauf) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            when (ereignis) {
                Lifecycle.Event.ON_START -> {
                    runde.sichtbarkeit(true)
                    runde.wiederaufnehmen()
                }
                Lifecycle.Event.ON_STOP -> runde.sichtbarkeit(false)
                else -> Unit
            }
        }
        lebenslauf.lifecycle.addObserver(beobachter)
        onDispose { lebenslauf.lifecycle.removeObserver(beobachter) }
    }

    // Zurück heißt im Raum „Runde verlassen?" — nie stillschweigend hinaus.
    BackHandler(enabled = stand.rausGrund == null && raum != null && !verlassenFragen) {
        if (raum?.beendet == true) runde.verlassen() else verlassenFragen = true
    }
    val verlassen: () -> Unit = {
        if (raum?.beendet == true) runde.verlassen() else verlassenFragen = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            stand.rausGrund != null -> Fehlerseite(
                grund = stand.rausGrund,
                beiZurueck = { runde.rausGrundWegnehmen() },
            )

            stand.zuschauer && raum?.beendet != true -> ZuschauerSeite(
                stand = stand,
                eigeneKennung = kennung,
                beiVerlassen = verlassen,
                regie = RegieGriffe(
                    start = { runde.dienstBeginnen() },
                    jetzt = { runde.regieEintragJetzt() },
                    ueberspringen = { runde.regieEintragUeberspringen() },
                    achse = { runde.regieZeitachse(it) },
                    lage = { st, text, meldebild, adresse, org, prio ->
                        runde.regieLage(st, text, meldebild, adresse, org, prio)
                    },
                    stoerung = { runde.regieStoerung(it) },
                    wetter = { runde.regieWetter(it) },
                    durchsage = { runde.regieDurchsage(it) },
                    beenden = { runde.regieUebungBeenden() },
                ),
                premium = sitzungsstand.konto?.premiumAktiv == true,
                beiPlatzBitten = { runde.beitrittAnfragen() },
                beiBitteZurueck = { runde.beitrittsanfrageZuruecknehmen() },
            )

            // Nachbesprechung: dieselben Bausteine wie die archivierte Schicht im Dienstbuch.
            raum?.beendet == true -> {
                val buch by sitzung.dienstbuch.stand.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { sitzung.freundeLaden() }
                DebriefingSeite(
                    stand = stand,
                    konto = sitzungsstand.konto,
                    freunde = seiten.freunde.inhalt.orEmpty(),
                    archiv = buch.runde,
                    beiArchivLaden = { sitzung.dienstbuch.rundeLaden(it) },
                    beiAnfragen = { wen ->
                        sitzung.dienstbuch.freundAnfragen(wen).also { if (it.isSuccess) sitzung.freundeLaden(neu = true) }
                    },
                    beiVerlassen = { runde.verlassen() },
                    beiHilfe = { browser.openUri(rundenhilfe(raum, ich, stand.zuschauer)) },
                    beiDienstbuch = {
                        Einsprung.oeffnen("dienstbuch")
                        runde.verlassen()
                    },
                    beiPremium = {
                        Einsprung.oeffnen(de.pagerspass.pagerspass.ansichten.Ladenbereich.Premium.weg)
                        runde.verlassen()
                    },
                )
            }

            laeuft && ich?.istLeitstelle == true -> LeitstelleSeite(
                stand = stand,
                katalog = seiten.katalog.inhalt,
                beiEinsatzAnlegen = { stichwort, meldebild, adresse, meldender, anrufId ->
                    runde.einsatzAnlegen(
                        stichwort = stichwort.stichwort,
                        stichwortText = stichwort.stichwortText,
                        meldebild = meldebild,
                        adresse = adresse,
                        organisation = stichwort.organisation,
                        prioritaet = stichwort.prioritaet,
                        meldender = meldender,
                        empfohleneFahrzeuge = stichwort.empfohleneFahrzeuge,
                        empfohleneFaehigkeiten = stichwort.empfohleneFaehigkeiten,
                        anrufId = anrufId,
                    )
                },
                beiAlarmieren = { einsatz, fahrzeuge -> runde.alarmieren(einsatz, fahrzeuge) },
                beiVorschlag = { runde.alarmvorschlag(it) },
                beiSchliessen = { runde.einsatzSchliessen(it) },
                beiSprechwunsch = { runde.sprechwunschBeantworten(it) },
                beiAnrufAnnehmen = { runde.anrufAnnehmen(it) },
                beiAnrufFrage = { anruf, frage -> runde.anrufFragen(anruf, frage) },
                beiAnrufBeenden = { runde.anrufBeenden(it) },
                beiAnrufAbweisen = { runde.anrufAbweisen(it) },
                beiVorschlagVerwerfen = { runde.vorschlagVerwerfen(it) },
                beiUmstufen = { einsatz, p -> runde.umstufen(einsatz, p) },
                beiFunk = { text, an -> runde.funken(text, an) },
                beiSprechstart = { runde.sprechenStarten() },
                beiSprechende = { runde.sprechenBeenden() },
                // Lücke A3: Der Leitstellendraht war nie verdrahtet.
                beiDraht = { runde.drahtSenden(it) },
                beiUeberspringen = { runde.ausbildungUeberspringen() },
                beiDienstende = { runde.dienstBeenden() },
                beiVerlassen = verlassen,
            )

            // Nur wer einen Platz hat, fährt — alle anderen wählen in der Lobby.
            // Fahrzeug: die Griffe stehen in `mobil/Fahrzeuggriffe.kt`.
            laeuft && ich != null && ich.role != "Unbestimmt" -> {
                LaunchedEffect(Unit) { sitzung.profilLaden() }
                FahrzeugSeite(
                    stand = stand,
                    eigeneKennung = kennung,
                    katalog = seiten.katalog.inhalt,
                    konto = sitzungsstand.konto,
                    melderGesicht = seiten.profil.inhalt?.melderGesicht,
                    griffe = fahrzeuggriffe(
                        runde = runde,
                        zusammenhang = zusammenhang,
                        kennung = kennung,
                        hilfe = { browser.openUri(rundenhilfe(raum, ich, stand.zuschauer)) },
                    ),
                    manv = manvgriffe(runde),
                )
            }

            else -> LobbySeite(
                stand = stand,
                eigeneKennung = kennung,
                konto = sitzungsstand.konto,
                server = sitzungsstand.server,
                garage = seiten.garage.inhalt?.stand,
                katalog = seiten.katalog.inhalt,
                daten = lobbystand,
                freunde = seiten.freunde.inhalt.orEmpty(),
                einladeMeldung = sozialstand.meldung,
                griffe = lobbygriffe(
                    raum = raum,
                    ich = ich,
                    kennung = kennung,
                    runde = runde,
                    sitzung = sitzung,
                    sozial = sozial,
                    lobbydaten = lobbydaten,
                    toene = toene,
                    hilfe = { browser.openUri(rundenhilfe(raum, ich, stand.zuschauer)) },
                    verlassen = verlassen,
                ),
            )
        }

        Rundenbaender(
            stand = stand,
            beiNeuVerbinden = { runde.wiederaufnehmen() },
            beiWarnungWeg = { runde.bevoelkerungswarnungWegnehmen() },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    // Die Übergabe des Leitstellentischs — gefragt wird nur, wem er angeboten ist.
    raum?.uebergabe?.takeIf { it.zielPlayerId == kennung && kennung.isNotBlank() }?.let { u ->
        Uebergabeblende(
            uebergabe = u,
            beiAntwort = { runde.uebergabeAntworten(it) },
        )
    }

    if (verlassenFragen) {
        RaumVerlassenBlende(
            laeuft = laeuft,
            istLeitstelle = ich?.istLeitstelle == true,
            beiBleiben = { verlassenFragen = false },
            beiVerlassen = {
                verlassenFragen = false
                runde.verlassen()
            },
        )
    }

    // Fahrzeug: Im Fahrzeug führt die Seite ihren Melder selbst (Bauart, Gerät, Ton);
    // die Blende bleibt für jede andere Lage, in der ein Alarm ankommt.
    val imFahrzeug = laeuft && ich != null && ich.role != "Unbestimmt" && !ich.istLeitstelle &&
        !stand.zuschauer && stand.rausGrund == null
    if (!imFahrzeug) stand.alarm?.let { alarm ->
        Melderblende(
            alarm = alarm,
            beiQuittieren = { runde.alarmQuittieren() },
            beiWegtippen = { runde.alarmWegtippen() },
        )
    }
}

/** Die Griffe der Lobby — an einer Stelle, damit der Rahmen lesbar bleibt. */
@Composable
private fun lobbygriffe(
    raum: Raumzustand?,
    ich: Spieler?,
    kennung: String,
    runde: Runde,
    sitzung: Sitzung,
    sozial: Sozial,
    lobbydaten: Lobbydaten,
    toene: Rundentoene,
    hilfe: () -> Unit,
    verlassen: () -> Unit,
): LobbyGriffe {
    // Die Griffe lesen den Raum beim Druck, nicht beim Bauen.
    val aktuellerRaum by rememberUpdatedState(raum)
    val aktuellIch by rememberUpdatedState(ich)
    return LobbyGriffe(
        rolle = { rolle, fahrzeug ->
            val r = aktuellerRaum
            // Sitzt schon jemand am Tisch, wird gefragt statt genommen.
            if (rolle == "Leitstelle" && r != null && aktuellIch?.istLeitstelle != true && r.leitstellen.isNotEmpty()) {
                runde.leitstellenplatzAnfragen()
            } else {
                runde.rolleWaehlen(rolle, fahrzeug)
            }
        },
        bereit = { runde.bereit(it) },
        bot = { vorlage, anzahl -> runde.botHinzufuegen(vorlage, anzahl) },
        botEntfernen = { runde.botEntfernen(it) },
        botUebernehmen = { runde.botUebernehmen(it) },
        chat = { runde.chatSenden(it) },
        start = { runde.dienstBeginnen() },
        verlassen = verlassen,
        einstellungen = { runde.einstellungen(it) },
        platzanfrageZuruecknehmen = { runde.platzanfrageZuruecknehmen() },
        platzanfrageEntscheiden = { wer, ja -> runde.platzanfrageEntscheiden(wer, ja) },
        beitrittEntscheiden = { wer, ja -> runde.beitrittEntscheiden(wer, ja) },
        kicken = { runde.spielerKicken(it) },
        freundAnfragen = { wen ->
            if (kennung.isNotBlank()) lobbydaten.freundAnfragen(kennung, wen) { sitzung.freundeLaden(neu = true) }
        },
        rufname = { id, name, kurz -> runde.funkrufnameAendern(id, name, kurz) },
        rufnameZuruecksetzen = { runde.funkrufnameZuruecksetzen(it) },
        wacheZuweisen = { id, wache -> runde.wacheZuweisen(id, wache) },
        raumwachenLaden = { aktuellerRaum?.code?.let { lobbydaten.raumwachenLaden(it) } },
        kreiswachenLaden = { lobbydaten.kreiswachenLaden(it) },
        vorlagenLaden = { if (kennung.isNotBlank()) lobbydaten.vorlagenLaden(kennung) },
        vorlageSpeichern = { id, name ->
            val code = aktuellerRaum?.code
            if (kennung.isNotBlank() && code != null) lobbydaten.vorlageSpeichern(kennung, id, name, code)
        },
        live = { runde.liveSetzen(it) },
        einladen = { wen, zuschauer ->
            aktuellerRaum?.code?.let { sozial.einladen(wen, it, zuschauer) }
        },
        freundeLaden = { sitzung.freundeLaden() },
        melderTesten = { toene.melderprobe() },
        hilfe = hilfe,
        laden = { lobbydaten.laden() },
    )
}

/**
 * Die passende Wiki-Seite zur Ansicht — dieselben Anker wie der „?"-Knopf im
 * Web. Für die Folgearbeiten an Leitstelle und Fahrzeug offen gelassen.
 */
internal fun rundenhilfe(raum: Raumzustand?, ich: Spieler?, zuschauer: Boolean): String {
    val pfad = when {
        zuschauer -> "/grundlagen/#nur-zuschauen"
        raum?.beendet == true -> "/dienstbuch/#die-nachbesprechung"
        raum?.laeuft == true && ich?.istLeitstelle == true -> "/rollen/#leitstelle"
        raum?.laeuft == true && ich != null && ich.role != "Unbestimmt" -> "/rollen/#fahrzeugbesatzung"
        else -> "/grundlagen/#ablauf-einer-runde"
    }
    return Rundentexte.WIKI + pfad
}

/**
 * Oben über allem: das Verbindungsband und die Bevölkerungswarnung.
 *
 * Die Warnung steht fünfzehn Sekunden — wie die Warn-App, die sie nachahmt —
 * und lässt sich früher wegtippen.
 */
@Composable
private fun Rundenbaender(
    stand: Rundenstand,
    beiNeuVerbinden: () -> Unit,
    beiWarnungWeg: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val warnung = stand.bevoelkerungswarnung
    LaunchedEffect(warnung?.um) {
        if (warnung != null) {
            delay(15_000)
            beiWarnungWeg()
        }
    }
    val verbindung = when {
        !stand.drin -> null
        stand.lage == Funkverbindung.Lage.Wiederverbinden -> "Verbindung wird wiederhergestellt …"
        stand.lage == Funkverbindung.Lage.Getrennt -> "Verbindung getrennt — neu verbinden"
        else -> null
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier.fillMaxWidth().padding(top = oben + Abstand.Klein).padding(horizontal = Abstand.Normal),
    ) {
        AnimatedVisibility(visible = verbindung != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = verbindung.orEmpty(),
                style = Schrift.MonoKlein,
                color = Farben.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(farbe = Farben.FlaecheHoch, randfarbe = Farben.AmberTief, ecke = 10.dp)
                    .clickable(enabled = stand.lage == Funkverbindung.Lage.Getrennt, onClick = beiNeuVerbinden)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            )
        }
        AnimatedVisibility(visible = warnung != null, enter = fadeIn(), exit = fadeOut()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.SignalTief, shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .clickable(onClick = beiWarnungWeg)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text("BEVÖLKERUNGSWARNUNG", style = Schrift.Etikett, color = Farben.Text)
                Text(warnung?.text.orEmpty(), style = Schrift.Normal, color = Farben.Text)
            }
        }
    }
}

/** „Runde verlassen?" — `RaumVerlassenDialog.vue`. */
@Composable
private fun RaumVerlassenBlende(
    laeuft: Boolean,
    istLeitstelle: Boolean,
    beiBleiben: () -> Unit,
    beiVerlassen: () -> Unit,
) {
    Blende(
        titel = "Runde verlassen?",
        beiSchliessen = beiBleiben,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Bleiben", beiBleiben, art = Knopfart.Leise)
            Knopf("Runde verlassen", beiVerlassen, art = Knopfart.Gefahr)
        },
    ) {
        Text(
            when {
                laeuft && istLeitstelle ->
                    "Du sitzt am Leitstellentisch. Gehst du, disponiert niemand mehr — offene Notrufe " +
                        "und Einsätze bleiben liegen, bis jemand den Tisch übernimmt."
                laeuft ->
                    "Dein Fahrzeug fällt aus dem Dienst. Die Schicht wird bis hierhin gewertet."
                else ->
                    "Du verlässt die Lobby. Mit dem Code kommst du jederzeit wieder hinein."
            },
            style = Schrift.Normal,
            color = Farben.TextLeise,
        )
    }
}

/**
 * Die Übergabe des Leitstellentischs — `UebergabeDialog.vue`. Das Protokoll
 * steht dabei: offene Einsätze, Fahrzeuge mit Hinweis, klingelnde Anrufe.
 */
@Composable
private fun Uebergabeblende(uebergabe: Uebergabe, beiAntwort: (Boolean) -> Unit) {
    Blende(
        titel = "${uebergabe.vonName} übergibt dir die Leitstelle",
        beiSchliessen = { beiAntwort(false) },
        fuss = {
            Knopf("Ablehnen", { beiAntwort(false) }, art = Knopfart.Leise)
            Knopf("Übernehmen", { beiAntwort(true) }, art = Knopfart.Haupt)
        },
    ) {
        SehrLeise("Übergabeprotokoll", mono = true)
        if (uebergabe.offeneEinsaetze.isEmpty()) {
            Text("Keine offenen Einsätze.", style = Schrift.Normal, color = Farben.TextLeise)
        }
        uebergabe.offeneEinsaetze.forEach { e ->
            Text(
                "${e.einsatznummer} · ${e.stichwort} · seit ${e.alterMinuten} min",
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
            e.hinweise.forEach { SehrLeise("– $it") }
        }
        uebergabe.fahrzeuge.forEach { f ->
            SehrLeise("${f.funkrufname}: ${f.hinweis}", mono = true)
        }
        if (uebergabe.klingelndeAnrufe > 0) {
            Text(
                "${uebergabe.klingelndeAnrufe} Anruf${if (uebergabe.klingelndeAnrufe == 1) "" else "e"} klingel${if (uebergabe.klingelndeAnrufe == 1) "t" else "n"} gerade.",
                style = Schrift.Klein,
                color = Farben.AmberHell,
            )
        }
        if (uebergabe.wetter.isNotBlank()) SehrLeise("Wetter: ${uebergabe.wetter}")
    }
}

/** Hinausgeworfen oder die Runde ist weg — eine Seite, ein Weg zurück. */
@Composable
private fun Fehlerseite(grund: String, beiZurueck: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Farben.Bg).padding(Abstand.Gross),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Runde beendet", style = Schrift.Titel, color = Farben.Text)
            Text(grund, style = Schrift.Normal, color = Farben.TextLeise, textAlign = TextAlign.Center)
            Knopf("Zurück zum Start", beiZurueck, art = Knopfart.Haupt)
        }
    }
}
