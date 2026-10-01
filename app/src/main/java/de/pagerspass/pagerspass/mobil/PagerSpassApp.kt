package de.pagerspass.pagerspass.mobil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.pagerspass.pagerspass.ansichten.BestenlisteSeite
import de.pagerspass.pagerspass.ansichten.DienstbuchSeite
import de.pagerspass.pagerspass.ansichten.GarageSeite
import de.pagerspass.pagerspass.ansichten.FreundeSeite
import de.pagerspass.pagerspass.ansichten.KontoSeite
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.pagerspass.pagerspass.ansichten.BegleiterSeite
import de.pagerspass.pagerspass.ansichten.DebriefingSeite
import de.pagerspass.pagerspass.ansichten.ZuschauerSeite
import de.pagerspass.pagerspass.ansichten.FahrzeugSeite
import de.pagerspass.pagerspass.ansichten.LeitstelleSeite
import de.pagerspass.pagerspass.ansichten.LobbySeite
import de.pagerspass.pagerspass.ansichten.Raumueberlagerungen
import de.pagerspass.pagerspass.ansichten.Melderblende
import de.pagerspass.pagerspass.ansichten.LoginSeite
import de.pagerspass.pagerspass.ansichten.PostfachSeite
import de.pagerspass.pagerspass.ansichten.PrivatsphaereSeite
import de.pagerspass.pagerspass.ansichten.PrivatsphaereZusatz
import de.pagerspass.pagerspass.ansichten.ProfilSeite
import de.pagerspass.pagerspass.ansichten.ShopSeite
import de.pagerspass.pagerspass.ansichten.StartSeite
import de.pagerspass.pagerspass.ansichten.WachenSeite
import de.pagerspass.pagerspass.ansichten.WachenGriffe
import de.pagerspass.pagerspass.ansichten.BrettTeil
import de.pagerspass.pagerspass.ansichten.EintragSeite
import de.pagerspass.pagerspass.ansichten.GespraechSeite
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.netz.Server
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Der Rahmen der Anwendung — das Gegenstück zu `web/src/mobil/MobilApp.vue`.
 *
 * Gleicher Inhalt, gleiche Daten, gleiche Wege; anders als am Rechner ist nur,
 * was drumherum steht: unten die Tableiste statt einer Fußzeile, und die
 * Meldungen sitzen **über** der Leiste statt am unteren Bildschirmrand.
 *
 * <b>Drei Zustände, und der mittlere wird gern vergessen.</b> Die App weiß beim
 * Start eine knappe Sekunde lang noch nicht, ob hier ein Konto liegt — sie muss
 * das gespeicherte Merkmal erst gegen den Server halten. In dieser Sekunde zeigt
 * sie **gar nichts**. Ohne diese Unterscheidung blitzt bei jedem Start die
 * Anmeldemaske auf, bevor der Startbildschirm sie ablöst, und das sieht aus, als
 * wäre man abgemeldet worden.
 *
 * <b>Die Reihenfolge, in der hier gestapelt wird, ist die Ebenenstaffel</b> aus
 * `Masse.kt`: die Seite ganz unten, darüber die Leiste (klebend), darüber die
 * Meldungen, darüber die Dialoge, die beantwortet sein müssen.
 */
@Composable
fun PagerSpassApp(
    sitzung: Sitzung,
    runde: Runde,
    sozial: Sozial,
    begleiter: Begleiter,
    werkstatt: Werkstatt,
    steuerung: NavHostController = rememberNavController(),
    welt: Welt? = null,
) {
    val stand by sitzung.stand.collectAsStateWithLifecycle()

    // Die Werkstatt hält Entwürfe und das Dienstbuch je Konto — ein Wechsel
    // des Kontos wirft beides weg. Als `SideEffect` und nicht als
    // `LaunchedEffect`: Es muss stehen, bevor die erste Seite ihr Laden
    // anstößt, und deren Effekte laufen erst danach an.
    SideEffect { werkstatt.kontoSetzen(stand.konto) }
    val rundenstand by runde.stand.collectAsStateWithLifecycle()
    val begleiterstand by begleiter.stand.collectAsStateWithLifecycle()
    val weltOffen = welt?.let { w -> w.stand.collectAsStateWithLifecycle().value.offen } ?: false

    // Abgemeldet oder ein anderes Konto: Die Welt des vorigen schließt — samt
    // Takt und Funkverbindung, die sonst unter fremdem Namen weiterliefen.
    LaunchedEffect(stand.konto?.kennung) {
        if (welt == null) return@LaunchedEffect
        val k = stand.konto?.kennung
        if (k == null || (welt.kennung != null && welt.kennung != k)) welt.schliessen()
    }

    // Zurück in die Runde, die vor einem Prozesstod lief — einmal je Konto.
    LaunchedEffect(stand.konto?.kennung) {
        val konto = stand.konto ?: return@LaunchedEffect
        runde.wiederAufnehmen(konto.anzeigename)
        // Und zurück an den Funkplatz, an dem das Gerät vorher hing — aus
        // demselben Grund wie die Runde, nur noch dringender: Der Begleiter ist
        // ein Melder (siehe Ablage.offenerBegleiter).
        begleiter.wiederAufnehmen()
        // Die Sozialschicht hängt am Konto, nicht an einer Seite — sie hört
        // auf Nachrichten, Brett und Wache, solange die App lebt.
        sozial.beiFreundesliste = { sitzung.freundeLaden(neu = true) }
        sozial.beiEinladungen = { sitzung.einladungenLaden(neu = true) }
        sozial.beiGemeinschaft = { sitzung.wacheLaden(neu = true) }
        sozial.beiBrett = { sitzung.brettLaden(neu = true) }
        sozial.verbinden(konto.kennung)
    }

    // Der Zwischenschritt der Anmeldung. Er gehört dem Rahmen und nicht der
    // Sitzung: Er überlebt das Drehen des Geräts, aber nicht das Beenden der
    // App — und genau so ist er gemeint.
    var zweiFaktor by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Farben.Bg)) {
        when {
            // Noch nichts wissen ist ein eigener Zustand — siehe oben.
            !stand.geprueft -> Unit

            !stand.angemeldet -> LoginSeite(
                laeuft = stand.laeuft,
                fehler = stand.fehler,
                hinweis = stand.hinweis,
                kontofreischaltung = stand.kontofreischaltung,
                server = stand.server,
                zweiFaktorZiel = zweiFaktor?.second,
                beiAnmelden = { name, wort ->
                    sitzung.anmelden(name, wort) { anfrage, ziel -> zweiFaktor = anfrage to ziel }
                },
                beiKontoAnlegen = sitzung::kontoAnlegen,
                beiZweiFaktor = { code ->
                    zweiFaktor?.let { (anfrage, _) -> sitzung.zweiFaktorBestaetigen(anfrage, code) }
                },
                beiServerWechsel = { adresse ->
                    zweiFaktor = null
                    sitzung.serverWechseln(adresse)
                },
                passwortVergessen = { vorbelegt, beiFertig, beiZurueck ->
                    val kontostand by sitzung.kontodienst.stand.collectAsStateWithLifecycle()
                    de.pagerspass.pagerspass.ansichten.PasswortVergessen(
                        stand = kontostand,
                        dienst = sitzung.kontodienst,
                        vorbelegt = vorbelegt,
                        beiFertig = beiFertig,
                        beiZurueck = beiZurueck,
                    )
                },
            )

            // <b>Die Runde verdrängt alles.</b> Wer in einer Lobby oder im Dienst
            // steht, sieht keine Tableiste und keine sechs Wege — im Raum hat
            // jede Ansicht ihre eigenen Reiter und braucht jede Zeile Höhe. Das
            // ist dieselbe Regel wie im Web (`leisteSichtbar` in MobilApp.vue).
            rundenstand.drin || rundenstand.laeuft -> Rundenrahmen(
                stand = rundenstand,
                sitzung = sitzung,
                runde = runde,
                daten = null,
            )

            // <b>Der gekoppelte Begleiter verdrängt genauso.</b> Er ist ein
            // Dienstbildschirm: drei Geräte, jedes braucht die ganze Fläche, und
            // eine Tableiste mit sechs Wegen darunter wäre eine Einladung, den
            // Funkplatz mitten in der Schicht zu verlassen. Nach der Runde
            // geprüft, weil man an einem Platz nicht zugleich fahren und ihn
            // begleiten kann.
            begleiterstand.gekoppelt -> Begleiterrahmen(begleiterstand, begleiter)

            // <b>Die Welt verdrängt ebenfalls</b> — sie trägt ihre eigene Leiste
            // (Lagen, Fahrzeuge, Wachen, Bauen, Mehr) und braucht die Karte ganz.
            weltOffen && welt != null -> de.pagerspass.pagerspass.ansichten.welt.WeltRahmen(
                welt = welt,
                konto = stand.konto,
                server = stand.server,
                beiVerlassen = { welt.schliessen() },
                beiKonto = {
                    welt.schliessen()
                    runCatching { zurWahl(steuerung, Weg.Konto) }
                },
                // Wie ein Name im Web: Die Route wechselt, und damit ist die Welt zu.
                beiProfil = { name ->
                    welt.schliessen()
                    runCatching { steuerung.navigate("$UNTERSEITE_FREMDPROFIL/$name") }
                },
            )

            else -> Angemeldet(stand, sitzung, runde, sozial, begleiter, werkstatt, steuerung, welt)
        }

        Verbindungsband(
            sichtbar = stand.laedt && stand.angemeldet,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Der Fehler steht am Fuß — aber nicht auf der Anmeldeseite: Dort steht
        // er im Formular, direkt unter dem Knopf, auf den er sich bezieht.
        Hinweis(
            text = if (stand.angemeldet) (rundenstand.fehler ?: stand.fehler) else null,
            ueberLeiste = stand.angemeldet,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // Was beantwortet sein muss, bevor überhaupt etwas geht — die oberste Ebene
    // der Staffel. Der Wiederherstellungscode steht zuerst, weil er nur ein
    // einziges Mal zu haben ist.
    stand.wiederherstellungscode?.let { code ->
        Wiederherstellungscode(code, sitzung::wiederherstellungscodeWegnehmen)
    }

    if (stand.rechtsstandOffen && stand.wiederherstellungscode == null) {
        RechtsstandBlende(
            beiZustimmen = sitzung::rechtsstandZustimmen,
            beiAbmelden = sitzung::abmelden,
        )
    }

    // Die Einwilligung in eine übertragene Schicht — hier und nicht im Raum:
    // Ein abgewiesener Beitritt steht noch draußen und hat keinen Rundenrahmen.
    val einwilligung = if (stand.angemeldet) {
        rundenstand.einwilligung?.bedarf ?: de.pagerspass.pagerspass.ansichten.einwilligungAmPlatz(
            rundenstand.raum,
            stand.konto?.kennung.orEmpty(),
            rundenstand.elternbogenAmPlatz,
        )
    } else {
        null
    }
    einwilligung?.let { frage ->
        de.pagerspass.pagerspass.ansichten.Einwilligungsblende(
            frage = frage,
            beiErteilen = { volljaehrig, beiEnde -> runde.einwilligungErteilen(frage, volljaehrig, beiEnde) },
            beiAblehnen = { runde.einwilligungAblehnen() },
        )
    }
}

/**
 * Der Rahmen um die laufende Runde.
 *
 * <b>Er verteilt nach Zustand und Rolle</b> — dieselbe Weiche wie `RaumView.vue`:
 * Beendet zeigt die Auswertung, die Lobby steht, solange die Runde nicht läuft
 * oder man keinen Platz hat, und danach entscheidet die Rolle zwischen
 * Leitstelle und Fahrzeug.
 *
 * <b>Und er trägt den Melder.</b> Das Alarm-Ereignis löst zweierlei aus: die
 * Blende mit Ton in der App und die Systemmeldung nach draußen — für den, der
 * zwischen zwei Einsätzen woanders ist. Die Berechtigung dafür wird beim
 * Betreten der Runde erfragt, nicht beim Start der App: Wer nie fährt, wird
 * nie gefragt.
 */
@Composable
private fun Rundenrahmen(
    stand: Rundenstand,
    sitzung: Sitzung,
    runde: Runde,
    daten: Seitenstand?,
) {
    val sitzungsstand by sitzung.stand.collectAsStateWithLifecycle()
    val seiten by sitzung.daten.collectAsStateWithLifecycle()
    val neben by runde.befehle.neben.collectAsStateWithLifecycle()
    val zusammenhang = LocalContext.current

    // Die eigene Garage entscheidet, welchen Platz man wählen kann — und ohne
    // Katalog wüsste weder die Lobby die Typen noch die Leitstelle die
    // Stichworte.
    LaunchedEffect(Unit) {
        sitzung.garageLaden()
        sitzung.katalogSicherstellen()
    }

    // Die Melder-Berechtigung — ab Android 13 eine eigene. Einmal beim Betreten
    // der Runde, denn hier wird sie gleich gebraucht.
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

    // Der Haken für die Systemmeldung — gesetzt, solange dieser Rahmen steht.
    DisposableEffect(Unit) {
        runde.beiAlarm = { alarm -> Meldermeldung.zeigen(zusammenhang, alarm) }
        onDispose { runde.beiAlarm = null }
    }

    val raum = stand.raum
    val ich = raum?.players?.firstOrNull { it.id == sitzungsstand.konto?.kennung }

    when {
        // Der Zuschauerplatz — vor den Spielerplätzen, denn `ich` gibt es hier
        // nicht: Der Zuschauer steht nie in `players`.
        // Wer als Zuschauer um einen Platz gebeten hat und angenommen wurde, steht
        // danach in `players` — dann ist er kein Zuschauer mehr (wie `istZuschauer`
        // im Web).
        stand.zuschauer && ich == null && raum?.beendet != true -> ZuschauerSeite(
            stand = stand,
            eigeneKennung = sitzungsstand.konto?.kennung.orEmpty(),
            premium = sitzungsstand.konto?.premiumAktiv == true,
            befehle = runde.befehle,
            beiVerlassen = { runde.verlassen() },
            regie = de.pagerspass.pagerspass.ansichten.RegieGriffe(
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
        )

        raum?.beendet == true -> DebriefingSeite(
            stand = stand,
            beiVerlassen = { runde.verlassen() },
        )

        raum?.laeuft == true && ich?.istLeitstelle == true -> LeitstelleSeite(
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
            beiAlarmieren = { einsatz, fahrzeuge, ab, zusatz, meldung ->
                runde.alarmieren(einsatz, fahrzeuge, ab, zusatz, meldung)
            },
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
            beiUeberspringen = { runde.ausbildungUeberspringen() },
            beiDienstende = { runde.dienstBeenden() },
            beiVerlassen = { runde.verlassen() },
            beiDraht = { runde.drahtSenden(it) },
            eigeneKennung = sitzungsstand.konto?.kennung.orEmpty(),
            befehle = runde.befehle,
            neben = neben,
        )

        // Wer mitten im Dienst ohne Platz dasteht, gehört in die Lobby — dort
        // übernimmt er ein Bot-Fahrzeug oder stellt ein eigenes in den Dienst.
        raum?.laeuft == true && ich?.role != "Unbestimmt" -> FahrzeugSeite(
            stand = stand,
            eigeneKennung = sitzungsstand.konto?.kennung.orEmpty(),
            katalog = seiten.katalog.inhalt,
            beiFms = { status, grund, dauer -> runde.fmsSetzen(status, grund, dauer) },
            beiSondersignal = { runde.sondersignal(it) },
            beiLagemeldung = { runde.lagemeldung(it) },
            beiFunk = { runde.funken(it) },
            beiEinsatzstelle = { runde.einsatzstelleSchreiben(it) },
            beiSprechstart = { runde.sprechenStarten() },
            beiSprechende = { runde.sprechenBeenden() },
            beiUeberspringen = { runde.ausbildungUeberspringen() },
            beiDienstende = { runde.dienstBeenden() },
            beiVerlassen = { runde.verlassen() },
            beiAlarmQuittieren = { runde.alarmQuittieren() },
            manv = de.pagerspass.pagerspass.ansichten.ManvGriffe(
                uebernehmen = { e, z -> runde.einsatzleitungUebernehmen(e, z) },
                abgeben = { e, z -> runde.einsatzleitungAbgeben(e, z) },
                abschnittBilden = { e, n, z -> runde.abschnittBilden(e, n, z) },
                abschnittZuteilen = { e, f, a, z -> runde.abschnittZuteilen(e, f, a, z) },
                auftrag = { e, a, f -> runde.manvauftragUebertragen(e, a, f) },
                anordnen = { e, art, g -> runde.versorgungsstelleAnordnen(e, art, g) },
                abbauen = { e, s, ab -> runde.versorgungsstelleAbbauen(e, s, ab) },
                verlegen = { e, p, s -> runde.patientVerlegen(e, p, s) },
                transportmittel = { e, p, f -> runde.patientTransportmittel(e, p, f) },
                zielklinik = { e, p, k -> runde.patientZielklinik(e, p, k) },
                transport = { e, p -> runde.transportEinleiten(e, p) },
                verstorbene = { runde.verstorbeneUebergeben(it) },
                triage = { runde.triageKoordinieren(it) },
            ),
            befehle = runde.befehle,
            neben = neben,
        )

        else -> LobbySeite(
            stand = stand,
            eigeneKennung = sitzungsstand.konto?.kennung.orEmpty(),
            garage = seiten.garage.inhalt?.stand?.fahrzeuge.orEmpty(),
            fahrzeuge = seiten.katalog.inhalt?.fahrzeuge.orEmpty(),
            beiRolle = { rolle, fahrzeug -> runde.rolleWaehlen(rolle, fahrzeug) },
            beiBereit = { runde.bereit(it) },
            beiBot = { runde.botHinzufuegen(it) },
            beiChat = { runde.chatSenden(it) },
            beiStart = { runde.dienstBeginnen() },
            beiVerlassen = { runde.verlassen() },
            befehle = runde.befehle,
            neben = neben,
        )
    }

    // Übergabe, Warnband und Einzelruf gehören zum Raum, nicht zu einer Ansicht.
    Box(modifier = Modifier.fillMaxSize()) {
        Raumueberlagerungen(
            raum = raum,
            neben = neben,
            eigeneKennung = sitzungsstand.konto?.kennung.orEmpty(),
            befehle = runde.befehle,
        )
    }

    // Der Melder in der App — über allem, mit Ton, bis jemand reagiert.
    stand.alarm?.let { alarm ->
        Melderblende(
            alarm = alarm,
            beiQuittieren = { runde.alarmQuittieren() },
            beiWegtippen = { runde.alarmWegtippen() },
            raum = stand.raum,
            eigeneKennung = sitzungsstand.konto?.kennung.orEmpty(),
            beiAusruecken = { runde.alarmQuittieren(); runde.fmsSetzen(3, null, null) },
        )
    }
}

/**
 * Der Rahmen um den mobilen Begleiter.
 *
 * <b>Er trägt dasselbe wie der Rundenrahmen — und aus denselben Gründen.</b>
 * Die Melder-Berechtigung wird hier erfragt und nicht beim Start der App (wer
 * nie koppelt, wird nie gefragt), und das Alarm-Ereignis geht als Systemmeldung
 * nach draußen: Ein Melder, der nur piept, während man ihn ansieht, ist keiner.
 * Im Web brauchte es dafür eigens Web Push.
 *
 * <b>Und er hält den Bildschirm wach.</b> Ein Melder liegt auf dem Tisch und ist
 * an. Nach dem Dienstende darf er wieder ausgehen — ein wachgehaltener toter
 * Funkplatz wäre nur ein leerer Akku am Morgen.
 */
@Composable
private fun Begleiterrahmen(stand: Begleiterstand, begleiter: Begleiter) {
    val zusammenhang = LocalContext.current
    val sicht = LocalView.current
    val lebenslauf = LocalLifecycleOwner.current
    var wachhalten by rememberSaveable { mutableStateOf(true) }

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
        begleiter.beiAlarm = { alarm -> Meldermeldung.zeigen(zusammenhang, alarm) }
        onDispose { begleiter.beiAlarm = null }
    }

    // `keepScreenOn` an der Ansicht und nicht `FLAG_KEEP_SCREEN_ON` am Fenster:
    // Dafür bräuchte es die Activity, und die aus einem Compose-Kontext
    // herauszuschälen ist genau die Art Griff, die in einem Dialog danebengeht.
    DisposableEffect(wachhalten, stand.beendet) {
        sicht.keepScreenOn = wachhalten && !stand.beendet
        onDispose { sicht.keepScreenOn = false }
    }

    /*
     * Ob der Bildschirm zu sehen ist, entscheidet am Server darüber, ob Push für
     * den Melder in der Tasche einspringen muss. Der Anwesenheitsstand des
     * *Rechnerplatzes* wird davon nicht angefasst: Tasche zu ist nicht Dienst
     * beendet.
     */
    DisposableEffect(lebenslauf) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            when (ereignis) {
                Lifecycle.Event.ON_START -> begleiter.sichtbarkeit(true)
                Lifecycle.Event.ON_STOP -> begleiter.sichtbarkeit(false)
                else -> Unit
            }
        }
        lebenslauf.lifecycle.addObserver(beobachter)
        onDispose { lebenslauf.lifecycle.removeObserver(beobachter) }
    }

    BegleiterSeite(
        stand = stand,
        wachhalten = wachhalten,
        beiFunk = { begleiter.funken(it) },
        beiSprechstart = { begleiter.sprechenStarten() },
        beiSprechende = { begleiter.sprechenBeenden() },
        beiQuittieren = { begleiter.alarmQuittieren() },
        beiFunkgruppe = { begleiter.funkgruppeSchalten(it) },
        beiWachhalten = { wachhalten = it },
        beiTrennen = { begleiter.trennen() },
    )

    // Der Melder über allem — dieselbe Blende wie im Dienst, mit Ton, bis
    // jemand reagiert. Ausdrücklich dieselbe: Ein Alarm sieht in dieser App
    // überall gleich aus, egal ob man selbst fährt oder den Platz begleitet.
    stand.alarm?.let { alarm ->
        Melderblende(
            alarm = alarm,
            beiQuittieren = { begleiter.alarmQuittieren() },
            beiWegtippen = { begleiter.alarmWegtippen() },
            geraete = stand.geraete,
        )
    }
}

/** Der angemeldete Teil: sechs Wege, eine Leiste, eine Navigation. */
@Composable
private fun Angemeldet(
    stand: Sitzungsstand,
    sitzung: Sitzung,
    runde: Runde,
    sozial: Sozial,
    begleiter: Begleiter,
    werkstatt: Werkstatt,
    steuerung: NavHostController,
    welt: Welt? = null,
) {
    val begleiterstand by begleiter.stand.collectAsStateWithLifecycle()
    val werk by werkstatt.stand.collectAsStateWithLifecycle()
    val daten by sitzung.daten.collectAsStateWithLifecycle()
    // Der Freundeskreis hängt an der Activity wie die Sitzung — hier geholt und
    // nicht in `MainActivity`, weil nur der angemeldete Zweig ihn braucht.
    val kreis: Freundeskreis = androidx.lifecycle.viewmodel.compose.viewModel()
    val kreisstand by kreis.stand.collectAsStateWithLifecycle()
    val sozialstand by sozial.stand.collectAsStateWithLifecycle()
    val eintrag by steuerung.currentBackStackEntryAsState()
    val hier = Weg.entries.firstOrNull { it.adresse == eintrag?.destination?.route }
    val browser = LocalUriHandler.current
    var loeschenOffen by remember { mutableStateOf(false) }
    val kontostand by sitzung.kontodienst.stand.collectAsStateWithLifecycle()
    val einrichtung by sitzung.einrichtungsdienst.stand.collectAsStateWithLifecycle()

    // Der Melder dieses Geräts erfährt Stufe und Abo, sobald das Konto da ist —
    // eine Bauform oder ein Ton, der nicht (mehr) offensteht, fällt zurück.
    val melderzusammenhang = LocalContext.current
    LaunchedEffect(stand.konto?.level, stand.konto?.premiumAktiv) {
        stand.konto?.let {
            de.pagerspass.pagerspass.melder.Meldergeraet.bereit(melderzusammenhang)
                .kontoMerken(it.level, it.premiumAktiv, null)
        }
    }
    // Der Bereich, in dem der Shop aufgehen soll — „Premium ansehen" im Konto.
    var shopWunsch by remember { mutableStateOf<String?>(null) }

    // Die Leiste steht auch auf den Unterseiten (Garage, Bestenliste) — sie sind
    // Teil des Buchs, kein eigener Zweig. Sie fiele erst weg, wenn eine Ansicht
    // ihre eigenen Reiter mitbringt; das ist bisher keine.
    val platz = Mass.LeisteHoehe

    // Wohin die Wege führen, die es in der App noch nicht gibt. Ausdrücklich der
    // eingestellte Server: Wer gegen `localhost` entwickelt, will auch dort
    // landen — anders als bei den Rechtstexten, die nur im Betrieb liegen.
    fun imWeb(seite: String) = browser.openUri("${stand.server}/play/mobile/$seite")

    // Premium wird nur auf der Webseite abgeschlossen — nie in der App.
    fun premiumImWeb() = imWeb("shop?bereich=premium")
    fun beitreten(code: String) = runde.beitreten(code, stand.konto?.anzeigename.orEmpty())
    fun zuschauen(code: String) = runde.zuschauen(code, stand.konto?.anzeigename.orEmpty())
    // Was der Freundeskreis der Sitzung zu sagen hat — dieselben Haken wie bei
    // der Sozialschicht. Je Konto neu gesetzt, und dabei wird vergessen, was
    // zum vorigen gehörte.
    LaunchedEffect(stand.konto?.kennung) {
        kreis.vergessen()
        kreis.beiFreunden = { sitzung.freundeLaden(neu = true) }
        kreis.beiWache = { id ->
            sitzung.wacheLaden(neu = true)
            id?.let { sitzung.wacheDetailLaden(it) }
        }
        kreis.beiDetail = { sitzung.wacheDetailSetzen(it) }
        kreis.beiKonto = { sitzung.kontoAuffrischen() }
        kreis.beiBrett = { sitzung.brettLaden(neu = true) }
        kreis.beiGeschenk = {
            sozial.stand.value.gespraechMit?.let { sozial.gespraechOeffnen(it) }
            sitzung.shopLaden(neu = true)
        }
    }

    val anzeigename = stand.konto?.anzeigename.orEmpty()
    fun profilOeffnen(benutzername: String) = steuerung.navigate("$UNTERSEITE_FREMDPROFIL/$benutzername")
    fun dazuschalten(code: String) = runde.beitreten(code, anzeigename)
    val brettgriffe = de.pagerspass.pagerspass.ansichten.Brettgriffe(
        profil = ::profilOeffnen,
        entfernen = { kreis.eintragEntfernen(it) },
        melden = { kreis.eintragMelden(it) },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = steuerung,
            startDestination = Weg.Dienst.adresse,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Weg.Dienst.adresse) {
                StartSeite(
                    unterrand = platz,
                    konto = stand.konto,
                    landkreise = daten.katalog.inhalt?.landkreise.orEmpty(),
                    laeuft = stand.laeuft,
                    beta = stand.beta,
                    version = stand.version,
                    beiKatalog = { sitzung.katalogSicherstellen() },
                    beiUmbenennen = { sitzung.umbenennen(it) },
                    beiBesetzen = { kreis -> sitzung.raumEroeffnen(kreis?.id) },
                    beiBesetzenMit = { kreis, leitstelle, bereich ->
                        sitzung.raumEroeffnen(kreis?.id, leitstelle, bereich)
                    },
                    leitstellen = daten.katalog.inhalt?.leitstellen.orEmpty(),
                    staaten = daten.katalog.inhalt?.staaten.orEmpty(),
                    beiAusbildung = { sitzung.ausbildungEroeffnen() },
                    beiBeitreten = { code ->
                        runde.beitreten(code, stand.konto?.anzeigename.orEmpty())
                    },
                    beiZuschauen = { code ->
                        runde.zuschauen(code, stand.konto?.anzeigename.orEmpty())
                    },
                    mitteilungen = daten.mitteilungen.inhalt.orEmpty(),
                    gelesen = daten.mitteilungenGelesen,
                    einladungen = daten.einladungen.inhalt.orEmpty(),
                    beiStartdaten = {
                        sitzung.mitteilungenLaden()
                        sitzung.einladungenLaden(neu = true)
                        sitzung.hinweiseLaden()
                    },
                    beiGelesen = { sitzung.mitteilungGelesen(it) },
                    beiEinladung = { e ->
                        if (e.alsZuschauer) {
                            runde.zuschauen(e.roomCode, stand.konto?.anzeigename.orEmpty())
                        } else {
                            runde.beitreten(e.roomCode, stand.konto?.anzeigename.orEmpty())
                        }
                    },
                    beiLink = { browser.openUri(it) },
                    beiTagesschicht = { steuerung.navigate(UNTERSEITE_TAGESSCHICHT) },
                    beiOeffentlicheRunden = { steuerung.navigate(UNTERSEITE_OEFFENTLICH) },
                    beiRechtstext = { seite ->
                        browser.openUri(Rechtsstand.adresse(Server.BETRIEB, seite))
                    },
                    beiImWeb = { imWeb("") },
                    beiStartweg = { weg ->
                        steuerung.navigate(
                            when (weg) {
                                de.pagerspass.pagerspass.ansichten.Startweg.Uebungen -> UNTERSEITE_UEBUNGEN
                                de.pagerspass.pagerspass.ansichten.Startweg.Leitstellenbau -> UNTERSEITE_LEITSTELLENBAU
                                else -> UNTERSEITE_LEHRGANG
                            },
                        )
                    },
                    vorlagen = if (stand.konto == null) null else {
                        {
                            de.pagerspass.pagerspass.ansichten.Vorlagenknopf(
                                anzahl = werk.rundenvorlagen.inhalt?.size ?: 0,
                                beiDruck = { steuerung.navigate(UNTERSEITE_RUNDENVORLAGEN) },
                            )
                        }
                    },
                    zusatz = {
                        LaunchedEffect(Unit) { werkstatt.startLaden() }
                        de.pagerspass.pagerspass.ansichten.Umfragekarte(
                            umfrage = werk.umfrage,
                            dank = werk.umfrageDank,
                            beiAntworten = { werkstatt.umfrageAntworten(it) },
                        )
                    },
                    kacheln = {
                        de.pagerspass.pagerspass.ansichten.Startkacheln(
                            kacheln = werk.startkacheln,
                            premium = stand.konto?.premiumAktiv == true,
                            laeuft = stand.laeuft,
                            beiKachel = { k ->
                                when {
                                    k.premiumNoetig && stand.konto?.premiumAktiv != true -> premiumImWeb()
                                    k.aktion == "link" -> browser.openUri(k.ziel)
                                    k.aktion == "raum" -> beitreten(k.ziel.trim().uppercase())
                                    // World läuft nativ — eine Kachel dorthin öffnet sie hier.
                                    k.ziel.trimStart('/').startsWith("welt") && welt != null -> welt.anzeigen()
                                    else -> when (val ziel = werkstattziel(k.ziel) ?: appziel(k.ziel)) {
                                        // Ein Raumcode im Ziel (`/raum/…`, `/code/…`) tritt bei.
                                        null -> raumcodeAus(k.ziel)?.let { beitreten(it) }
                                            ?: imWeb(k.ziel.trimStart('/'))
                                        else -> Weg.entries.firstOrNull { it.adresse == ziel }
                                            ?.let { zurWahl(steuerung, it) }
                                            ?: steuerung.navigate(ziel)
                                    }
                                }
                            },
                        )
                    },
                    fussknoepfe = {
                        de.pagerspass.pagerspass.ansichten.Fussknoepfe(werk.footer) { browser.openUri(it) }
                    },
                    beiWelt = welt?.let { w -> { w.anzeigen() } },
                )
            }

            composable(Weg.Dienstbuch.adresse) {
                LaunchedEffect(Unit) {
                    sitzung.katalogSicherstellen()
                    sitzung.buchLaden()
                }
                DienstbuchSeite(
                    unterrand = platz,
                    konto = stand.konto,
                    stand = werk,
                    fahrzeuge = daten.katalog.inhalt?.fahrzeuge.orEmpty(),
                    werkstatt = werkstatt,
                    beiSchicht = { steuerung.navigate("$UNTERSEITE_SCHICHT/$it") },
                    beiGarage = { steuerung.navigate(UNTERSEITE_GARAGE) },
                    beiDienst = { zurWahl(steuerung, Weg.Dienst) },
                    beiPremium = { premiumImWeb() },
                )
            }

            werkstattwege(
                platz = platz,
                steuerung = steuerung,
                werkstatt = werkstatt,
                sitzung = sitzung,
                beitreten = ::beitreten,
                zuschauen = ::zuschauen,
                imBrowser = { browser.openUri(it) },
            )

            composable(Weg.Wache.adresse) {
                // Landkreise fürs Gründen und die Suche, die eigenen Schichten für
                // die Hilfsfristkurve — beide nur, wenn sie noch fehlen.
                LaunchedEffect(Unit) {
                    sitzung.katalogSicherstellen()
                    sitzung.buchLaden()
                }
                WachenSeite(
                    unterrand = platz,
                    wache = daten.wache,
                    detail = daten.wacheDetail,
                    antraege = daten.wachenantraege,
                    sozial = sozialstand,
                    meineKennung = stand.konto?.kennung.orEmpty(),
                    konto = stand.konto,
                    kreis = kreisstand,
                    landkreise = daten.katalog.inhalt?.landkreise.orEmpty(),
                    schichten = daten.buch.inhalt?.schichten.orEmpty(),
                    server = stand.server,
                    beiLaden = { sitzung.wacheLaden() },
                    beiDetail = { sitzung.wacheDetailLaden(it) },
                    beiChatOeffnen = { sozial.wachenchatOeffnen(it) },
                    beiAntraege = { sitzung.wachenantraegeLaden() },
                    griffe = WachenGriffe(
                        chatSenden = { id, text -> sozial.wachenchatSenden(id, text) },
                        antragEntscheiden = { id, nr, ok -> sitzung.antragEntscheiden(id, nr, ok) },
                        rolleSetzen = { id, wen, rolle -> sitzung.rolleSetzen(id, wen, rolle) },
                        leitungUebergeben = { id, an -> sitzung.leitungUebergeben(id, an) },
                        mitgliedEntfernen = { id, wen -> sitzung.mitgliedEntfernen(id, wen) },
                        austreten = { sitzung.austreten(it) },
                        einladen = { id, wen -> sitzung.wacheEinladen(id, wen) },
                        pinnwand = { id, text -> sitzung.pinnwandSetzen(id, text) },
                        terminAnlegen = { id, titel, wann ->
                            sitzung.terminAnlegen(id, titel, wann)
                        },
                        terminAntworten = { id, nr, antwort ->
                            sitzung.terminAntworten(id, nr, antwort)
                        },
                        terminAbsagen = { id, nr -> sitzung.terminAbsagen(id, nr) },
                        zeileMelden = { id, nr, grund -> sitzung.zeileMelden(id, nr, grund) },
                        zeileEntfernen = { id, nr -> sitzung.zeileEntfernen(id, nr) },
                        meldungErledigt = { id, nr -> sitzung.meldungErledigt(id, nr) },
                        clanrunde = { sitzung.clanrundeStarten(it) },
                        beitreten = { sitzung.beitrittMitCode(it) },
                        gruenden = { name, text, kreisId, kreisName ->
                            kreis.gruenden(name, text, kreisId, kreisName)
                        },
                        bewerben = { id, text -> sitzung.bewerben(id, text) },
                        bewerbungZurueckziehen = { sitzung.bewerbungZurueckziehen(it) },
                        einladungAnnehmen = { sitzung.einladungAnnehmen(it) },
                        clanrundeSchliessen = { kreis.clanrundeSchliessen(it) },
                        dazustossen = ::dazuschalten,
                        profil = ::profilOeffnen,
                        tagSetzen = { id, tag -> kreis.tagSetzen(id, tag) },
                        einstellungen = { id, e -> kreis.einstellungenSpeichern(id, e) },
                        codeErneuern = { kreis.codeErneuern(it) },
                        aufloesen = { kreis.aufloesen(it) },
                        schmuck = { id, art, stueck -> kreis.schmuckSetzen(id, art, stueck) },
                        farbe = { id, n -> kreis.farbeSetzen(id, n) },
                        rangliste = { steuerung.navigate(UNTERSEITE_WACHENRANGLISTE) },
                        shop = { steuerung.navigate(UNTERSEITE_WACHENSHOP) },
                        hilfsfristen = { kreis.hilfsfristenLaden(it) },
                        laufbahn = { kreis.laufbahnLaden() },
                        suchen = { kreis.wachenSuchen(it) },
                        landkreisvorschlag = { kreis.landkreisvorschlagLaden() },
                        meldungWeg = { kreis.meldungWegnehmen() },
                    ),
                )
            }

            composable(Weg.Freunde.adresse) {
                LaunchedEffect(Unit) {
                    // Das eigene Profil trägt Wappen und Muster des Ausweises; die
                    // Wache entscheidet, ob es am Brett den Reiter „Wache" gibt.
                    sitzung.profilLaden()
                    sitzung.wacheLaden()
                    sitzung.einladungenLaden()
                }
                FreundeSeite(
                    unterrand = platz,
                    konto = stand.konto,
                    meinProfil = daten.profil.inhalt,
                    freunde = daten.freunde,
                    einladungen = daten.einladungen.inhalt.orEmpty(),
                    kreis = kreisstand,
                    server = stand.server,
                    griffe = de.pagerspass.pagerspass.ansichten.FreundeGriffe(
                        laden = { sitzung.freundeLaden() },
                        profil = ::profilOeffnen,
                        gespraech = { freund ->
                            steuerung.navigate(
                                "$UNTERSEITE_GESPRAECH/${freund.kennung}/${freund.anzeigename.ifBlank { freund.benutzername }}",
                            )
                        },
                        dazuschalten = ::dazuschalten,
                        antworten = { wen, annehmen -> kreis.antworten(wen, annehmen) },
                        anfragen = { kreis.anfragen(it) },
                        loesen = { kreis.loesen(it) },
                        suchen = { kreis.suchen(it) },
                        vorschlaegeLaden = { kreis.vorschlaegeLaden() },
                        weglegen = { kreis.weglegen(it) },
                        zurueckholen = { kreis.zurueckholen() },
                        einladungAnnehmen = { e ->
                            sozial.einladungBeantworten(e.nr, true) { ok ->
                                if (ok) {
                                    if (e.alsZuschauer) runde.zuschauen(e.roomCode, anzeigename)
                                    else runde.beitreten(e.roomCode, anzeigename)
                                }
                            }
                        },
                        einladungAblehnen = { e -> sozial.einladungBeantworten(e.nr, false) },
                        meldungWeg = { kreis.meldungWegnehmen() },
                    ),
                    brett = {
                        BrettTeil(
                            brett = daten.brett,
                            reiter = daten.brettReiter,
                            hatWache = daten.wache.inhalt?.eigene != null,
                            darfOeffentlich = (daten.buch.inhalt?.schichten?.size ?: 1) > 0,
                            beiReiter = { sitzung.brettLaden(reiter = it) },
                            beiLaden = { sitzung.brettLaden() },
                            beiMehr = { sitzung.brettMehr() },
                            beiSchreiben = { text, sicht -> sitzung.brettSchreiben(text, sicht) },
                            beiQuittieren = { sitzung.brettQuittieren(it) },
                            beiOeffnen = { steuerung.navigate("$UNTERSEITE_EINTRAG/$it") },
                            server = stand.server,
                            griffe = brettgriffe,
                        )
                    },
                )
            }

            composable(Weg.Shop.adresse) {
                // Die Garage sagt, wie viele Gutscheine offen sind — die Zahl am Reiter.
                LaunchedEffect(Unit) {
                    sitzung.garageLaden()
                    // Das Autohaus im Bereich „Fahrzeuge“ zeigt Kategorie und Fähigkeiten.
                    sitzung.katalogSicherstellen()
                }
                ShopSeite(
                    unterrand = platz,
                    konto = stand.konto,
                    shop = daten.shop,
                    gutscheine = daten.garage.inhalt?.stand?.offeneWahlen ?: 0,
                    server = stand.server,
                    laeuft = stand.laeuft,
                    stand = kontostand,
                    dienst = sitzung.kontodienst,
                    wunsch = shopWunsch,
                    beiWunschErfuellt = { shopWunsch = null },
                    beiLaden = { sitzung.shopLaden(neu = true) },
                    beiKauf = { sitzung.artikelKaufen(it) },
                    beiTagesbonus = { sitzung.tagesbonusHolen() },
                    beiAutohaus = { steuerung.navigate(UNTERSEITE_GARAGE) },
                    beiProfil = { steuerung.navigate(UNTERSEITE_PROFIL) },
                    beiAusbildung = { zurWahl(steuerung, Weg.Dienst) },
                    garage = daten.garage.inhalt,
                    katalog = daten.katalog.inhalt?.fahrzeuge.orEmpty(),
                    beiFahrzeugHolen = { vorlage, kaufen -> sitzung.fahrzeugHolen(vorlage, kaufen) },
                )
            }

            composable(Weg.Konto.adresse) {
                KontoSeite(
                    unterrand = platz,
                    konto = stand.konto,
                    profil = daten.profil,
                    server = stand.server,
                    postfachFrei = stand.postfachFrei,
                    stand = kontostand,
                    dienst = sitzung.kontodienst,
                    beiProfilLaden = { sitzung.profilLaden() },
                    beiKontoLaden = { sitzung.kontodienst.kontoLaden() },
                    beiAbmelden = { sitzung.abmelden() },
                    beiLoeschen = { loeschenOffen = true },
                    beiDiscord = { steuerung.navigate(UNTERSEITE_DISCORD) },
                    beiGemeinschaft = { zurWahl(steuerung, Weg.Wache) },
                    beiShop = { zurWahl(steuerung, Weg.Shop) },
                    beiPremium = {
                        shopWunsch = de.pagerspass.pagerspass.ansichten.PREMIUM
                        zurWahl(steuerung, Weg.Shop)
                    },
                    beiProfil = { steuerung.navigate(UNTERSEITE_PROFIL) },
                    beiPrivatsphaere = { steuerung.navigate(UNTERSEITE_PRIVATSPHAERE) },
                    beiPostfach = { steuerung.navigate(UNTERSEITE_POSTFACH) },
                    beiMitteilungen = { steuerung.navigate(UNTERSEITE_MITTEILUNGEN) },
                    beiBegleiter = { steuerung.navigate(UNTERSEITE_BEGLEITER) },
                    beiMelder = { steuerung.navigate(UNTERSEITE_MELDER) },
                    einrichtung = einrichtung,
                    einrichtungsdienst = sitzung.einrichtungsdienst,
                    beiProfilAnsehen = stand.konto?.benutzername?.takeIf { it.isNotBlank() }?.let { name ->
                        { profilOeffnen(name) }
                    },
                )
            }

            composable(UNTERSEITE_MELDER) {
                // Der Shop sagt, welche Töne und Gesichter gekauft sind — ohne ihn
                // stünde jedes gekaufte Stück gesperrt da (siehe Profilseite).
                LaunchedEffect(Unit) { sitzung.shopLaden() }
                de.pagerspass.pagerspass.ansichten.melder.MelderSeite(
                    unterrand = platz,
                    konto = stand.konto,
                    profil = daten.profil,
                    server = stand.server,
                    gekauft = daten.shop.inhalt?.imBesitz?.map { it.id }?.toSet(),
                    laeuft = stand.laeuft,
                    beiLaden = { sitzung.profilLaden() },
                    beiGesicht = {
                        sitzung.profilAendern(de.pagerspass.pagerspass.netz.Profilaenderung(melderGesicht = it))
                    },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable(UNTERSEITE_DISCORD) {
                de.pagerspass.pagerspass.ansichten.DiscordSeite(
                    unterrand = platz,
                    konto = stand.konto,
                    stand = kontostand,
                    dienst = sitzung.kontodienst,
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable(UNTERSEITE_BEGLEITER) {
                de.pagerspass.pagerspass.ansichten.BegleiterKopplung(
                    unterrand = platz,
                    laeuft = begleiterstand.laeuft,
                    fehler = begleiterstand.fehler,
                    beiKoppeln = { begleiter.koppeln(it) },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable("$UNTERSEITE_GESPRAECH/{kennung}/{name}") { eintrag ->
                val partnerKennung = eintrag.arguments?.getString("kennung").orEmpty()
                GespraechSeite(
                    unterrand = platz,
                    partnerKennung = partnerKennung,
                    partnerName = eintrag.arguments?.getString("name").orEmpty(),
                    partner = daten.freunde.inhalt?.firstOrNull { it.kennung == partnerKennung },
                    server = stand.server,
                    sozial = sozialstand,
                    meineKennung = stand.konto?.kennung.orEmpty(),
                    oeffnetGeschenk = kreisstand.oeffnetGeschenk,
                    meldung = kreisstand.meldung ?: sozialstand.meldung,
                    hinweis = kreisstand.hinweis,
                    beiOeffnen = { sozial.gespraechOeffnen(it) },
                    beiSchliessen = { sozial.gespraechSchliessen() },
                    beiSenden = { an, text -> sozial.senden(an, text) },
                    beiTermin = { nr, zusagen -> sozial.terminBeantworten(nr, zusagen) },
                    beiGeschenk = { kreis.geschenkOeffnen(it) },
                    beiProfil = ::profilOeffnen,
                    beiDazuschalten = ::dazuschalten,
                    beiMeldungWeg = {
                        kreis.meldungWegnehmen()
                        sozial.meldungWegnehmen()
                    },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable("$UNTERSEITE_EINTRAG/{nr}") { eintrag ->
                EintragSeite(
                    unterrand = platz,
                    nr = eintrag.arguments?.getString("nr")?.toLongOrNull() ?: 0L,
                    eintrag = daten.brettEintrag,
                    kommentare = daten.brettKommentare,
                    server = stand.server,
                    meldung = kreisstand.meldung,
                    hinweis = kreisstand.hinweis,
                    beiLaden = { sitzung.brettEintragLaden(it) },
                    beiQuittieren = { sitzung.brettQuittieren(it) },
                    beiKommentieren = { nr, text -> sitzung.brettKommentieren(nr, text) },
                    beiKommentarMelden = { kreis.kommentarMelden(it) },
                    beiMeldungWeg = { kreis.meldungWegnehmen() },
                    // Wer seinen Eintrag zurücknimmt, hat hier nichts mehr zu lesen.
                    griffe = de.pagerspass.pagerspass.ansichten.Brettgriffe(
                        profil = ::profilOeffnen,
                        entfernen = { nr -> kreis.eintragEntfernen(nr) { steuerung.popBackStack() } },
                        melden = { kreis.eintragMelden(it) },
                    ),
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable("$UNTERSEITE_FREMDPROFIL/{benutzername}") { eintrag ->
                val eigeneWache = daten.wache.inhalt?.eigene
                // Nach einer Anfrage, Antwort oder Trennung steht der neue Stand
                // der Beziehung erst im frisch geholten Profil.
                fun neuLaden() {
                    kreis.stand.value.profilFuer?.let { kreis.profilLaden(it) }
                }
                de.pagerspass.pagerspass.ansichten.FremdprofilSeite(
                    unterrand = platz,
                    benutzername = eintrag.arguments?.getString("benutzername").orEmpty(),
                    kreis = kreisstand,
                    server = stand.server,
                    griffe = de.pagerspass.pagerspass.ansichten.Profilgriffe(
                        laden = { kreis.profilLaden(it) },
                        anfragen = { wen -> kreis.anfragen(wen, ::neuLaden) },
                        antworten = { wen, ja -> kreis.antworten(wen, ja, ::neuLaden) },
                        beenden = { wen -> kreis.loesen(wen, ::neuLaden) },
                        // Wer blockiert, will das Gesicht nicht länger ansehen — zurück.
                        blockieren = { wen -> kreis.blockieren(wen) { steuerung.popBackStack() } },
                        melden = { wen, grund -> kreis.melden(wen, grund) },
                        gespraech = { p ->
                            steuerung.navigate("$UNTERSEITE_GESPRAECH/${p.kennung}/${p.anzeigename}")
                        },
                        dazuschalten = ::dazuschalten,
                        inWacheEinladen = eigeneWache?.takeIf { it.darfFuehren }?.let { w ->
                            { wen: String -> sitzung.wacheEinladen(w.id, wen) }
                        },
                        eigenesProfil = { steuerung.navigate(UNTERSEITE_PROFIL) },
                        quittieren = { nr -> kreis.profilQuittieren(nr) { sitzung.brettQuittieren(nr) } },
                        eintragOeffnen = { steuerung.navigate("$UNTERSEITE_EINTRAG/$it") },
                        meldungWeg = { kreis.meldungWegnehmen() },
                        zurueck = { steuerung.popBackStack() },
                    ),
                    brettgriffe = brettgriffe,
                )
            }

            composable(UNTERSEITE_WACHENRANGLISTE) {
                de.pagerspass.pagerspass.ansichten.WachenranglisteSeite(
                    unterrand = platz,
                    liste = kreisstand.rangliste,
                    beiLaden = { kreis.ranglisteLaden() },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable(UNTERSEITE_WACHENSHOP) {
                val eigene = daten.wache.inhalt?.eigene
                val detail = daten.wacheDetail.inhalt?.takeIf { it.gemeinschaft.id == eigene?.id }
                var aussehenOffen by remember { mutableStateOf(false) }
                de.pagerspass.pagerspass.ansichten.WachenShopSeite(
                    unterrand = platz,
                    gemeinschaft = eigene,
                    schatz = detail?.schatz,
                    meineCredits = stand.konto?.credits ?: 0,
                    laeuft = kreisstand.laeuft,
                    meldung = kreisstand.meldung,
                    hinweis = kreisstand.hinweis,
                    beiLaden = {
                        sitzung.wacheLaden()
                        eigene?.id?.let { sitzung.wacheDetailLaden(it) }
                    },
                    beiKaufen = { artikel -> eigene?.let { kreis.ausbauKaufen(it.id, artikel) } },
                    beiWunsch = { artikel, an -> eigene?.let { kreis.wunschSetzen(it.id, artikel, an) } },
                    beiEinzahlen = { credits -> eigene?.let { kreis.einzahlen(it.id, credits) } },
                    beiAnpassen = { aussehenOffen = true },
                    beiMeldungWeg = { kreis.meldungWegnehmen() },
                    beiZurueck = { steuerung.popBackStack() },
                )
                val schatz = detail?.schatz
                if (aussehenOffen && eigene != null && schatz != null) {
                    de.pagerspass.pagerspass.ansichten.AussehenBlende(
                        gemeinschaft = detail.gemeinschaft.copy(eigeneRolle = eigene.eigeneRolle),
                        schatz = schatz,
                        laeuft = kreisstand.laeuft,
                        beiSchmuck = { art, stueck -> kreis.schmuckSetzen(eigene.id, art, stueck) },
                        beiFarbe = { kreis.farbeSetzen(eigene.id, it) },
                        beiSchliessen = { aussehenOffen = false },
                    )
                }
            }

            composable(UNTERSEITE_OEFFENTLICH) {
                de.pagerspass.pagerspass.ansichten.OeffentlicheRundenSeite(
                    unterrand = platz,
                    stand = daten.oeffentlicheRunden,
                    laeuft = stand.laeuft,
                    beiLaden = { sitzung.oeffentlicheRundenLaden(neu = true) },
                    beiBeitreten = { code ->
                        runde.beitreten(code, stand.konto?.anzeigename.orEmpty())
                    },
                    beiZuschauen = { code ->
                        runde.zuschauen(code, stand.konto?.anzeigename.orEmpty())
                    },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable(UNTERSEITE_TAGESSCHICHT) {
                de.pagerspass.pagerspass.ansichten.TagesschichtSeite(
                    unterrand = platz,
                    stand = daten.tagesschicht,
                    laeuft = stand.laeuft,
                    beiLaden = { sitzung.tagesschichtLaden(neu = true) },
                    beiStart = { sitzung.tagesschichtEroeffnen() },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable(UNTERSEITE_MITTEILUNGEN) {
                de.pagerspass.pagerspass.ansichten.MitteilungenSeite(
                    unterrand = platz,
                    stand = daten.mitteilungsschalter,
                    beiLaden = { sitzung.mitteilungsschalterLaden() },
                    beiSetzen = { sitzung.mitteilungsschalterSetzen(it) },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable(UNTERSEITE_PROFIL) {
                // Der Shop sagt, was gekauft ist — und nur Gekauftes lässt sich
                // tragen. Deshalb lädt die Profilseite ihn mit, obwohl sie ihn
                // nicht zeigt: Ohne den Besitzstand stünde jedes gekaufte Stück
                // gesperrt da, auch das, was man längst hat.
                LaunchedEffect(Unit) { sitzung.shopLaden() }

                ProfilSeite(
                    unterrand = platz,
                    konto = stand.konto,
                    profil = daten.profil,
                    server = stand.server,
                    besitzt = daten.shop.inhalt?.imBesitz.orEmpty()
                        .map { it.stueckId }
                        .toSet(),
                    profilbild = daten.profilbild,
                    laeuft = stand.laeuft,
                    beiLaden = { sitzung.profilLaden() },
                    beiAendern = { sitzung.profilAendern(it) },
                    beiBildLaden = { sitzung.profilbildLaden() },
                    beiBildEinreichen = { name, typ, daten ->
                        sitzung.profilbildEinreichen(name, typ, daten)
                    },
                    beiBildEntfernen = { sitzung.profilbildEntfernen() },
                    beiZurueck = { steuerung.popBackStack() },
                    dienst = sitzung.kontodienst,
                )
            }

            composable(UNTERSEITE_PRIVATSPHAERE) {
                PrivatsphaereSeite(
                    unterrand = platz,
                    stand = daten.privatsphaere,
                    beiLaden = { sitzung.privatsphaereLaden() },
                    beiSetzen = { sitzung.privatsphaereSetzen(it) },
                    beiZurueck = { steuerung.popBackStack() },
                    zusatz = {
                        PrivatsphaereZusatz(
                            konto = stand.konto,
                            stand = kontostand,
                            dienst = sitzung.kontodienst,
                            freunde = daten.freunde.inhalt.orEmpty(),
                            beiFreundeLaden = { sitzung.freundeLaden(neu = true) },
                            beiKonto = { steuerung.popBackStack() },
                            beiMitteilungen = { steuerung.navigate(UNTERSEITE_MITTEILUNGEN) },
                            beiAbmelden = { sitzung.abmelden().join() },
                        )
                    },
                )
            }

            composable(UNTERSEITE_POSTFACH) {
                PostfachSeite(
                    unterrand = platz,
                    mitteilungen = daten.mitteilungen,
                    postfachFrei = stand.postfachFrei,
                    beiLaden = { sitzung.mitteilungenLaden() },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable(UNTERSEITE_GARAGE) {
                GarageSeite(
                    unterrand = platz,
                    garage = daten.garage,
                    laeuft = stand.laeuft,
                    beiLaden = { sitzung.garageLaden() },
                    beiHolen = { vorlage, kaufen -> sitzung.fahrzeugHolen(vorlage, kaufen) },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }

            composable(UNTERSEITE_BESTENLISTE) {
                BestenlisteSeite(
                    unterrand = platz,
                    liste = daten.bestenliste,
                    beiLaden = { sitzung.bestenlisteLaden() },
                    beiZurueck = { steuerung.popBackStack() },
                )
            }
        }

        Tableiste(
            // Auf einer Unterseite bleibt der Weg markiert, aus dem sie kommt —
            // die Garage gehört zum Buch. Ohne das stünde die Leiste dort ohne
            // jede Markierung, und man wüsste nicht mehr, wo man ist.
            hier = hier ?: unterseitenweg(eintrag?.destination?.route),
            marken = marken(daten),
            beiWahl = { weg -> zurWahl(steuerung, weg) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // Die Adresse einmal je Konto holen — sie entscheidet über die Pflichtfrage.
    LaunchedEffect(stand.konto?.kennung) {
        if (stand.konto != null) sitzung.kontodienst.postfachLaden()
    }

    // Bogen und Maßnahmenübersicht einmal je Konto — still; ohne sie gibt es nichts zu fragen.
    LaunchedEffect(stand.konto?.kennung) {
        if (stand.konto != null) sitzung.einrichtungsdienst.laden()
    }

    // Die Pflichtfragen in der Reihenfolge des Webs: erst der Rechtsstand (oben im
    // Rahmen), dann die Aufzeichnung, dann die Adresse, dann das Alter. Eine nach
    // der anderen — zwei Dialoge übereinander beantwortet niemand.
    val pflichtfrageOffen = when {
        stand.rechtsstandOffen || stand.konto == null -> stand.konto != null
        stand.konto.analyseZustimmung == null -> {
            de.pagerspass.pagerspass.ansichten.Analyseblende(kontostand, sitzung.kontodienst)
            true
        }
        kontostand.emailFehlt || kontostand.emailBestaetigungOffen -> {
            de.pagerspass.pagerspass.ansichten.EmailPflichtblende(
                kontostand,
                sitzung.kontodienst,
                beiAbmelden = { sitzung.abmelden() },
            )
            true
        }
        altersFrageOffen(stand.konto.altersstand) -> {
            de.pagerspass.pagerspass.ansichten.Altersfreigabeblende(
                wartet = stand.konto.altersstand == "WartetAufEltern",
                stand = einrichtung,
                dienst = sitzung.einrichtungsdienst,
                beiAbmelden = { sitzung.abmelden() },
            )
            true
        }
        else -> false
    }
    de.pagerspass.pagerspass.ansichten.Geschenkblende(kontostand, sitzung.kontodienst)

    if (loeschenOffen) {
        Kontoloeschung(
            laeuft = stand.laeuft,
            beiLoeschen = { passwort ->
                sitzung.kontoLoeschen(passwort)
                loeschenOffen = false
            },
            beiSchliessen = { loeschenOffen = false },
        )
    }

    // <b>Wer eine Runde eröffnet, geht sofort hinein.</b>
    //
    // Hier stand eine Blende mit dem Code und einem Knopf „In die Lobby". Sie
    // hat den Raum gekostet: Ein frisch eröffneter Raum wird wieder aufgelöst,
    // wenn der Beitritt nicht zügig folgt — im API-Protokoll steht dann „Leere
    // Lobby ABCDEF nach Prüfung aufgelöst", und die App meldet „Diesen Raum
    // gibt es nicht (mehr)". Gemessen: fünfzig Sekunden offene Blende genügten.
    //
    // Der Code ist damit nicht weg — er steht groß im Kopf der Lobby, wo man
    // ihn ohnehin weitergibt.
    LaunchedEffect(stand.raumcode) {
        val code = stand.raumcode ?: return@LaunchedEffect
        sitzung.raumcodeWegnehmen()
        runde.beitreten(code, stand.konto?.anzeigename.orEmpty())
    }

    // Ein Link von außen (`Tieflinks`) — erst hier, wo Konto, Shop und Wache
    // stehen. Bis dahin wartet er im Briefkasten.
    val tieflink by Tieflinks.offen.collectAsStateWithLifecycle()
    var linkcode by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(tieflink, stand.konto?.kennung) {
        val adresse = tieflink ?: return@LaunchedEffect
        if (stand.konto == null) return@LaunchedEffect
        Tieflinks.erledigt(adresse)
        when (val ziel = tiefzielAus(adresse)) {
            is Tiefziel.Code -> linkcode = ziel.code
            is Tiefziel.Funk -> begleiter.koppeln(ziel.adresse)
            is Tiefziel.Raum -> beitreten(ziel.code)
            is Tiefziel.Wache -> {
                sitzung.beitrittMitCode(ziel.code)
                runCatching { zurWahl(steuerung, Weg.Wache) }
            }
            is Tiefziel.Seite -> appziel(ziel.pfad)?.let { route ->
                runCatching {
                    Weg.entries.firstOrNull { it.adresse == route }?.let { zurWahl(steuerung, it) }
                        ?: steuerung.navigate(route)
                }
            }
            null -> Unit
        }
    }
    linkcode?.let { code ->
        de.pagerspass.pagerspass.ansichten.CodeBlende(
            code = code,
            stand = kontostand,
            dienst = sitzung.kontodienst,
            beiShop = {
                linkcode = null
                runCatching { zurWahl(steuerung, Weg.Shop) }
            },
            beiSchliessen = { linkcode = null },
        )
    }

    daten.bonusgewinn?.let { gewinn ->
        Bonusblende(gewinn = gewinn, beiSchliessen = { sitzung.bonusgewinnWegnehmen() })
    }

    // Verwarnungen zuerst, dann Verwaltungsnachrichten — eine nach der
    // anderen, bis alle bestätigt sind. Wie die Dialoge des Web.
    // Erst wenn die Pflichtfragen vom Tisch sind — wie `offeneVerwarnung` im Web.
    daten.verwarnungen.firstOrNull()?.takeIf { !pflichtfrageOffen }?.let { v ->
        Verwarnungsblende(
            verwarnung = v,
            beiBestaetigen = { sitzung.verwarnungBestaetigen(v.nr) },
        )
    } ?: daten.adminNachrichten.firstOrNull()?.takeIf { !pflichtfrageOffen }?.let { n ->
        AdminNachrichtblende(
            nachricht = n,
            beiBestaetigen = { sitzung.adminNachrichtBestaetigen(n.nr) },
        )
    }

    // Der Einrichtungsbogen — zuletzt in der Reihe, nach allem, was nicht warten
    // kann. Nie mitten in einer Runde: Dieser Zweig steht ohnehin nur ohne Raum.
    if (
        !pflichtfrageOffen &&
        einrichtung.bogenOffen &&
        daten.verwarnungen.isEmpty() &&
        daten.adminNachrichten.isEmpty()
    ) {
        de.pagerspass.pagerspass.ansichten.Einrichtungsblende(
            stand = einrichtung,
            dienst = sitzung.einrichtungsdienst,
            beiLehrgang = { runCatching { steuerung.navigate(UNTERSEITE_LEHRGANG) } },
        )
    }
}

private const val UNTERSEITE_GARAGE = "garage"
private const val UNTERSEITE_BESTENLISTE = "bestenliste"
private const val UNTERSEITE_PROFIL = "profil"
private const val UNTERSEITE_MELDER = "melder"
private const val UNTERSEITE_PRIVATSPHAERE = "privatsphaere"
private const val UNTERSEITE_POSTFACH = "postfach"
private const val UNTERSEITE_MITTEILUNGEN = "mitteilungen"
private const val UNTERSEITE_BEGLEITER = "begleiter"
private const val UNTERSEITE_DISCORD = "discord"
private const val UNTERSEITE_OEFFENTLICH = "oeffentlicheRunden"
private const val UNTERSEITE_TAGESSCHICHT = "tagesschicht"
private const val UNTERSEITE_GESPRAECH = "gespraech"
private const val UNTERSEITE_EINTRAG = "eintrag"
private const val UNTERSEITE_FREMDPROFIL = "freundprofil"
private const val UNTERSEITE_WACHENRANGLISTE = "wachenrangliste"
private const val UNTERSEITE_WACHENSHOP = "wachenshop"

/** Zu welchem Weg der Leiste eine Unterseite gehört. */
private fun unterseitenweg(route: String?): Weg? = when (route) {
    UNTERSEITE_GARAGE, UNTERSEITE_BESTENLISTE -> Weg.Dienstbuch
    UNTERSEITE_PROFIL, UNTERSEITE_MELDER, UNTERSEITE_PRIVATSPHAERE, UNTERSEITE_POSTFACH,
    UNTERSEITE_MITTEILUNGEN, UNTERSEITE_BEGLEITER, UNTERSEITE_DISCORD,
    -> Weg.Konto
    UNTERSEITE_OEFFENTLICH, UNTERSEITE_TAGESSCHICHT -> Weg.Dienst
    UNTERSEITE_WACHENRANGLISTE, UNTERSEITE_WACHENSHOP -> Weg.Wache
    else -> werkstattweg(route) ?: when {
        route?.startsWith(UNTERSEITE_GESPRAECH) == true -> Weg.Freunde
        route?.startsWith(UNTERSEITE_EINTRAG) == true -> Weg.Freunde
        route?.startsWith(UNTERSEITE_FREMDPROFIL) == true -> Weg.Freunde
        else -> null
    }
}

/**
 * Die roten Zahlen an der Leiste.
 *
 * <b>Sie kommen aus denselben Listen, die die Seiten zeigen.</b> Eine zweite
 * Quelle für „3 offen" wäre eine Zahl, die irgendwann etwas anderes sagt als die
 * Liste darunter. Solange ein Bereich nicht geladen ist, steht dort keine Zahl —
 * eine Null ist keine Auskunft, sondern eine Behauptung.
 */
private fun marken(daten: Seitenstand): Map<Weg, Int> = buildMap {
    daten.freunde.inhalt?.let { liste ->
        put(Weg.Freunde, liste.count { it.angefragt && !it.vonMir })
    }
    daten.garage.inhalt?.let { put(Weg.Shop, it.stand.offeneWahlen) }
}

/**
 * Die Wege aus `web/src/router.ts`, die die App selbst kennt — für Startkacheln,
 * deren Ziel ein Pfad der Webseite ist. `null` heißt: nicht in der App.
 *
 * <b>Was mit echtem Geld zu tun hat, bleibt draußen.</b> `/shop?bereich=premium`
 * führt deshalb ausdrücklich nicht in den Shop der App, sondern an die Webseite;
 * die Rechtstexte ebenso, sie müssen ohne Anmeldung erreichbar sein.
 */
internal fun appziel(ziel: String): String? {
    val roh = ziel.trim()
    val pfad = roh.substringBefore('?').trimEnd('/').removePrefix("/play/mobile").removePrefix("/play/desktop")
    val anfrage = roh.substringAfter('?', "")
    return when {
        pfad == "/shop" && "bereich=premium" in anfrage -> null
        pfad == "/shop" -> Weg.Shop.adresse
        pfad == "/konto" -> Weg.Konto.adresse
        pfad == "/konto/privatsphaere" -> UNTERSEITE_PRIVATSPHAERE
        pfad == "/konto/mitteilungen" -> UNTERSEITE_MITTEILUNGEN
        pfad == "/discord/verknuepfen" -> UNTERSEITE_DISCORD
        pfad == "/freunde" || pfad.startsWith("/freunde/") && !pfad.startsWith("/freunde/profil/") -> Weg.Freunde.adresse
        pfad.startsWith("/freunde/profil/") -> "$UNTERSEITE_FREMDPROFIL/${pfad.substringAfterLast('/')}"
        pfad == "/gemeinschaft" || pfad == "/gemeinschaften" -> Weg.Wache.adresse
        pfad == "/gemeinschaften/rangliste" -> UNTERSEITE_WACHENRANGLISTE
        pfad == "/gemeinschaft/shop" -> UNTERSEITE_WACHENSHOP
        pfad == "/oeffentliche-runden" -> UNTERSEITE_OEFFENTLICH
        pfad == "/scan" -> UNTERSEITE_BEGLEITER
        // Die alten Adressen aus der Zeit vor dem Dienstbuch — wie die Weiterleitungen im Web.
        pfad == "/garage" -> UNTERSEITE_GARAGE
        pfad == "/bestenliste" -> UNTERSEITE_BESTENLISTE
        pfad == "/chronik" || pfad == "/archiv" -> Weg.Dienstbuch.adresse
        pfad == "" -> Weg.Dienst.adresse
        else -> null
    }
}

/** Der Raumcode aus `/raum/ABC123` oder `/code/ABC123` — sonst `null`. */
internal fun raumcodeAus(ziel: String): String? {
    val pfad = ziel.trim().substringBefore('?').trimEnd('/').removePrefix("/play/mobile").removePrefix("/play/desktop")
    val treffer = Regex("^/(raum|code)/([A-Za-z0-9]{4,12})$").find(pfad) ?: return null
    return treffer.groupValues[2].uppercase()
}

/**
 * Der Wechsel zwischen den sechs Wegen.
 *
 * <b>Er legt keinen Verlauf an.</b> Wer viermal zwischen Dienst und Konto
 * wechselt, soll mit einem Druck auf Zurück die App verlassen und nicht viermal
 * rückwärts durch die Leiste laufen — die Leiste ist eine Wahl, kein Weg. Das
 * ist dasselbe Verhalten wie im Web, wo die sechs Wege einander in der Adresse
 * ersetzen.
 */
private fun zurWahl(steuerung: NavHostController, weg: Weg) {
    steuerung.navigate(weg.adresse) {
        popUpTo(steuerung.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Die eine Zeile, die die Anwendung über allem sagt.
 *
 * <b>Über der Tableiste, nicht darauf</b> — sonst verdeckt die Meldung genau die
 * Knöpfe, mit denen man auf sie reagieren würde. Wie hoch die Leiste baut, weiß
 * sie nicht selbst: Die Zahl steht in `Mass.LeisteHoehe`. Im Web stand hier
 * einmal eine abgeschriebene 72 — änderte die Leiste ihre Höhe, blieb die
 * Meldung an der alten Stelle kleben.
 */
@Composable
private fun Hinweis(text: String?, ueberLeiste: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier.padding(
            bottom = if (ueberLeiste) Mass.LeisteHoehe + 16.dp else 16.dp,
            start = Abstand.Normal,
            end = Abstand.Normal,
        ),
    ) {
        Text(
            text = text.orEmpty(),
            style = Schrift.MonoKlein,
            color = Farben.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .background(Farben.SignalTief, Rundung.Normal)
                .border(1.dp, Farben.Signal, Rundung.Normal)
                .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
        )
    }
}

/**
 * „Verbindung wird wiederhergestellt …" — oben, klein, in Amber.
 *
 * Oben und nicht unten, weil die Meldung darunter der Fehler ist und beide
 * gleichzeitig dastehen können. Amber, weil es kein Fehler ist: Es läuft gerade.
 */
@Composable
private fun Verbindungsband(sichtbar: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = sichtbar,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier.padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Abstand.Klein,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Farben.FlaecheHoch, Rundung.Rund)
                .border(1.dp, Farben.AmberTief, Rundung.Rund)
                .padding(horizontal = Abstand.Gross, vertical = Abstand.Klein),
        ) {
            Box(Modifier.size(8.dp).background(Farben.Amber, CircleShape))
            Text(
                text = "Verbindung wird hergestellt …",
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                color = Farben.Amber,
            )
        }
    }
}
