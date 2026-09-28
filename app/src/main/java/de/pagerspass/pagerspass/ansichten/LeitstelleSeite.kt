package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.pagerspass.pagerspass.mobil.Leitstellenstand
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Teil
import de.pagerspass.pagerspass.ui.bausteine.Teilleiste
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.raster
import de.pagerspass.pagerspass.ui.zeichen.Zeichen
import kotlinx.coroutines.delay

/**
 * Die Leitstelle — der Dienst aus Sicht des Disponenten.
 *
 * Übertragen aus `web/src/views/LeitstelleView.vue` im Handyzweig: Kopf mit
 * Wetter, Zählern und Knöpfen; darunter die Bänder, die zeitkritisch sind
 * (Lektion, Sprechwünsche, Notrufe, Feststellungen); dann ein Teil zur Zeit —
 * Einsätze (Liste oben, Bogen darunter), Karte, Fahrzeuge, Funk.
 *
 * <b>Der Kreislauf der Leitstelle:</b> Der Notruf klingelt (Band und Blende),
 * das Gespräch füllt den Bogen, der Bogen wird ein Einsatz, der Einsatz
 * alarmiert — und der Alarm liegt fünf Sekunden im Rückholfenster, bevor er
 * hinausgeht. Das Funkprotokoll erzählt, wie es weitergeht.
 *
 * <b>Was die Ansicht selbst weiß:</b> welcher Einsatz gewählt ist, welche Blende
 * offen steht, der wartende Alarm und der Lesestand am Funk. Alles andere kommt
 * aus dem Raumzustand und geht über die [LeitstellenGriffe] zurück.
 */
@Composable
fun LeitstelleSeite(
    modifier: Modifier = Modifier,
    stand: Rundenstand = Rundenstand(),
    katalog: Katalog? = null,
    konto: Konto? = null,
    daten: Leitstellenstand = Leitstellenstand(),
    griffe: LeitstellenGriffe = LeitstellenGriffe(),
) {
    val raum = stand.raum
    val ich = stand.eigeneKennung

    var reiter by remember { mutableStateOf(Leitstellenteil.Einsaetze) }
    var gewaehlteId by remember { mutableStateOf<String?>(null) }
    var alarmFuer by remember { mutableStateOf<String?>(null) }
    var umstufenFuer by remember { mutableStateOf<String?>(null) }
    var folgeFuer by remember { mutableStateOf<String?>(null) }
    var neuOffen by remember { mutableStateOf(false) }
    var journalOffen by remember { mutableStateOf(false) }
    var journalAnruf by remember { mutableStateOf<String?>(null) }
    var geortet by remember { mutableStateOf<Geortet?>(null) }
    var notrufWeggeklappt by remember { mutableStateOf<String?>(null) }
    var gelesenFunk by remember { mutableIntStateOf(stand.funk.size) }
    var gelesenDraht by remember { mutableIntStateOf(stand.drahtGesamt.size) }
    var leitung by remember { mutableStateOf(Funkleitung.Funk) }

    // ------------------------------------------------ Was gerade ansteht

    val offene = raum?.incidents.orEmpty().filter { !it.abgeschlossen }
    val anrufe = raum?.anrufe.orEmpty()
    val klingelnde = anrufe.filter { it.klingelt }
    val laufendesGespraech = anrufe.firstOrNull {
        it.imGespraech && it.bearbeiterPlayerId == ich && ich.isNotEmpty()
    }
    val offenerVorschlag = anrufe.firstOrNull {
        it.zustand == "Beendet" && it.vorschlag != null && it.bearbeiterPlayerId == ich && ich.isNotEmpty()
    }
    val amDraht = raum?.drahtOffen == true && stand.ich?.istLeitstelle == true
    val funkAmHandy = stand.begleiterGekoppelt && raum != null && daten.ausgelagertFuer == raum.code
    val nurDraht = funkAmHandy && amDraht

    // Ohne Auswahl den dringendsten offenen Einsatz zeigen — ebenso, wenn der
    // gewählte gerade abgeschlossen wurde. Wer einen abgeschlossenen bewusst
    // antippt, behält ihn.
    val offeneIds = offene.map { it.id }
    var vorherOffen by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(offeneIds) {
        val id = gewaehlteId
        val weg = id == null || raum?.incidents?.none { it.id == id } != false
        val geradeAbgeschlossen = id != null && id in vorherOffen && id !in offeneIds
        if (weg || geradeAbgeschlossen) gewaehlteId = offeneIds.firstOrNull()
        vorherOffen = offeneIds
    }

    // Der Lesestand läuft mit, solange die Leitung offen vor einem liegt — nicht
    // nur beim Umschalten, sonst gäbe es beim Wechsel eine Zahl für alles, was man
    // die ganze Zeit mitgelesen hat.
    val funkSichtbar = reiter == Leitstellenteil.Funk
    LaunchedEffect(stand.funk.size, stand.drahtGesamt.size, funkSichtbar, leitung) {
        if (!funkSichtbar) return@LaunchedEffect
        if (leitung == Funkleitung.Draht) gelesenDraht = stand.drahtGesamt.size
        else if (leitung == Funkleitung.Funk) gelesenFunk = stand.funk.size
    }
    val ungelesenFunk =
        if (funkSichtbar && leitung == Funkleitung.Funk) 0 else (stand.funk.size - gelesenFunk).coerceAtLeast(0)
    val ungelesenDraht =
        if (funkSichtbar && leitung == Funkleitung.Draht) 0
        else if (amDraht) (stand.drahtGesamt.size - gelesenDraht).coerceAtLeast(0) else 0

    // ------------------------------------------- Alarm mit Rückholfrist

    val rueckholer = remember { Rueckholer() }
    val aktuelleGriffe by rememberUpdatedState(griffe)
    rueckholer.absenden = { auftrag -> aktuelleGriffe.alarmieren(auftrag) }
    rueckholer.verwerfen = { id -> aktuelleGriffe.alarmMeldungVerwerfen(id) }

    LaunchedEffect(rueckholer.wartend?.frist) {
        val w = rueckholer.wartend ?: return@LaunchedEffect
        while (true) {
            val rest = ((w.frist - System.currentTimeMillis() + 999) / 1000).toInt()
            rueckholer.rest = rest.coerceAtLeast(0)
            if (rest <= 0) {
                rueckholer.jetztAbsenden()
                break
            }
            delay(250)
        }
    }

    // Ein gesperrtes Handy friert nichts ein, aber der Moment zum Zurückholen ist
    // mit dem Wegstecken vorbei — der wartende Alarm geht dann gleich hinaus, und
    // ebenso beim Verlassen der Ansicht (verworfen wird nur ausdrücklich).
    val lebenslauf = LocalLifecycleOwner.current
    DisposableEffect(lebenslauf) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            if (ereignis == Lifecycle.Event.ON_STOP) rueckholer.jetztAbsenden()
        }
        lebenslauf.lifecycle.addObserver(beobachter)
        onDispose {
            lebenslauf.lifecycle.removeObserver(beobachter)
            rueckholer.jetztAbsenden()
        }
    }

    Leitstellentoene(raum = raum, klingelt = klingelnde.isNotEmpty() && laufendesGespraech == null)
    val partner = rememberFunkpartner(stand)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Brush.verticalGradient(listOf(Farben.Bg, Farben.BgTief))) }
                .raster(),
        ) {
            if (raum == null) {
                Ladezeile("Der Arbeitsplatz wird geladen …", Modifier.padding(Abstand.Gross))
                return@Column
            }

            Leitstellenkopf(
                stand = stand,
                raum = raum,
                katalog = katalog,
                konto = konto,
                daten = daten,
                griffe = griffe,
                beiNeuerEinsatz = { neuOffen = true },
            )

            Leitstellenbaender(
                stand = stand,
                raum = raum,
                klingelnde = klingelnde,
                imGespraech = laufendesGespraech != null,
                griffe = griffe,
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (reiter) {
                    Leitstellenteil.Einsaetze -> Column(Modifier.fillMaxSize()) {
                        val gewaehlt = raum.incidents.firstOrNull { it.id == gewaehlteId }
                        Einsatzliste(
                            raum = raum,
                            ausgewaehlt = gewaehlteId,
                            beiWahl = { gewaehlteId = it },
                            journalSichtbar = raum.settings.telefonischeLeitstelle,
                            beiJournal = {
                                journalAnruf = null
                                journalOffen = true
                            },
                            modifier = if (gewaehlt != null) {
                                Modifier.fillMaxWidth().heightIn(max = 190.dp)
                            } else {
                                Modifier.fillMaxWidth().weight(1f)
                            },
                        )
                        if (gewaehlt != null) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(Abstand.Gross),
                            ) {
                                Einsatzdetail(
                                    einsatz = gewaehlt,
                                    raum = raum,
                                    daten = daten,
                                    griffe = griffe,
                                    beiAlarmieren = { alarmFuer = gewaehlt.id },
                                    beiUmstufen = { umstufenFuer = gewaehlt.id },
                                    beiFolgeeinsatz = { folgeFuer = gewaehlt.id },
                                    beiGespraech = { anrufId ->
                                        journalAnruf = anrufId
                                        journalOffen = true
                                    },
                                )
                            }
                        }
                    }

                    Leitstellenteil.Karte -> de.pagerspass.pagerspass.ui.karte.Lagekarte(
                        raum = raum,
                        ausgewaehlt = gewaehlteId,
                        beiWahl = { id -> if (id != null) gewaehlteId = id },
                        modifier = Modifier.fillMaxSize().padding(Abstand.Normal),
                    )

                    Leitstellenteil.Fahrzeuge -> Fahrzeugtableau(
                        raum = raum,
                        stand = stand,
                        katalog = katalog,
                        hervorheben = raum.incidents.firstOrNull { it.id == gewaehlteId }
                            ?.alarmierteFahrzeuge.orEmpty(),
                        griffe = griffe,
                        modifier = Modifier.fillMaxSize(),
                    )

                    Leitstellenteil.Funk -> Leitstellenfunk(
                        stand = stand,
                        raum = raum,
                        katalog = katalog,
                        partner = partner,
                        leitung = leitung,
                        beiLeitung = { leitung = it },
                        amDraht = amDraht,
                        funkAmHandy = funkAmHandy,
                        nurDraht = nurDraht,
                        ungelesenFunk = ungelesenFunk,
                        ungelesenDraht = ungelesenDraht,
                        griffe = griffe,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                rueckholer.wartend?.let { w ->
                    Rueckholband(
                        wartend = w,
                        rest = rueckholer.rest,
                        beiRueckholen = { rueckholer.rueckholen() },
                        beiSofort = { rueckholer.jetztAbsenden() },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(Abstand.Normal),
                    )
                }
            }

            Teilleiste(
                teile = Leitstellenteil.entries.map { Teil(it.name, it.titel, it.zeichen) },
                offen = reiter.name,
                marken = mapOf(Leitstellenteil.Funk.name to ungelesenFunk + ungelesenDraht),
                // Ein klingelnder Notruf ruft — der Band steht oben, aber wer auf der
                // Karte ist, soll auch unten sehen, dass etwas wartet.
                ruft = if (klingelnde.isNotEmpty()) setOf(Leitstellenteil.Einsaetze.name) else emptySet(),
                beiWahl = { id -> reiter = Leitstellenteil.valueOf(id) },
            )
        }

        // Der Einzelruf schwebt über der Arbeit, solange er dauert. Liegt der Funk
        // am gekoppelten Handy, führt das Handy ihn — dort steht dann die Leiste.
        if (raum != null && !funkAmHandy) {
            LeitstellenEinzelruf(
                stand = stand,
                griffe = griffe,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp, start = Abstand.Normal, end = Abstand.Normal),
            )
        }
    }

    if (raum == null) return

    // ------------------------------------------------------------ Blenden

    raum.incidents.firstOrNull { it.id == alarmFuer }?.let { einsatz ->
        Alarmblende(
            einsatz = einsatz,
            raum = raum,
            katalog = katalog,
            konto = konto,
            daten = daten,
            griffe = griffe,
            beiAlarmieren = { auftrag ->
                rueckholer.planen(auftrag, einsatz.stichwort)
            },
            beiSchliessen = { alarmFuer = null },
        )
    }

    if (neuOffen) {
        Einsatzbogenblende(
            raum = raum,
            katalog = katalog,
            konto = konto,
            daten = daten,
            griffe = griffe,
            beiSchliessen = { neuOffen = false },
        )
    }

    raum.incidents.firstOrNull { it.id == folgeFuer }?.let { ursprung ->
        Einsatzbogenblende(
            raum = raum,
            katalog = katalog,
            konto = konto,
            daten = daten,
            griffe = griffe,
            ursprung = ursprung,
            beiSchliessen = { folgeFuer = null },
        )
    }

    raum.incidents.firstOrNull { it.id == umstufenFuer }?.let { einsatz ->
        Einsatzbogenblende(
            raum = raum,
            katalog = katalog,
            konto = konto,
            daten = daten,
            griffe = griffe,
            umstufen = einsatz,
            beiSchliessen = { umstufenFuer = null },
        )
    }

    geortet?.let { g ->
        Einsatzbogenblende(
            raum = raum,
            katalog = katalog,
            konto = konto,
            daten = daten,
            griffe = griffe,
            geortet = g,
            anrufId = g.anrufId,
            beiSchliessen = { geortet = null },
        )
    }

    // Nach dem Auflegen geht es direkt in den Bogen — vorbelegt mit dem, was
    // erfragt wurde. Schließen heißt hier: Der Vorschlag wird verworfen.
    if (laufendesGespraech == null && geortet == null) {
        offenerVorschlag?.let { anruf ->
            val vorschlag = anruf.vorschlag
            if (vorschlag != null) {
                Einsatzbogenblende(
                    raum = raum,
                    katalog = katalog,
                    konto = konto,
                    daten = daten,
                    griffe = griffe,
                    vorschlag = vorschlag,
                    anrufId = anruf.id,
                    beiSchliessen = { abgeschickt ->
                        if (!abgeschickt) griffe.vorschlagVerwerfen(anruf.id)
                    },
                )
            }
        }
    }

    // Genau ein Popup zur Zeit, der älteste wartende Anruf. „Später" klappt es
    // weg, ohne zu entscheiden — der Anruf klingelt weiter, das Band bleibt.
    val naechster = klingelnde.firstOrNull()
    if (naechster != null && notrufWeggeklappt != naechster.id && laufendesGespraech == null) {
        Notrufblende(
            anruf = naechster,
            weitere = klingelnde.size - 1,
            imGespraech = false,
            beiAnnehmen = { griffe.anrufAnnehmen(naechster.id) },
            beiAbweisen = { griffe.anrufAbweisen(naechster.id) },
            beiSpaeter = { notrufWeggeklappt = naechster.id },
        )
    }

    laufendesGespraech?.let { anruf ->
        Telefonfenster(
            anruf = anruf,
            stand = stand,
            griffe = griffe,
        )
    }

    if (journalOffen) {
        Anrufjournalblende(
            raum = raum,
            anfangsAnruf = journalAnruf,
            griffe = griffe,
            beiSchliessen = { journalOffen = false },
            beiEinsatz = { id ->
                gewaehlteId = id
                journalOffen = false
                reiter = Leitstellenteil.Einsaetze
            },
            beiOrtungUebernehmen = { g ->
                geortet = g
                journalOffen = false
            },
        )
    }
}

/** Die vier Teile des Tischs — dieselben Reiter wie im Handyzweig des Webs. */
internal enum class Leitstellenteil(
    val titel: String,
    val zeichen: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Einsaetze("Einsätze", Zeichen.Lage),
    Karte("Karte", Zeichen.Karte),
    Fahrzeuge("Fahrzeuge", Zeichen.Fahrzeug),
    Funk("Funk", Zeichen.Funk),
}

/** Welche Leitung im Funkteil vorn steht — Funkverkehr, Draht oder das Handgerät. */
internal enum class Funkleitung(val wort: String) {
    Funk("Funkverkehr"),
    Draht("Leitstellen"),
    Geraet("Handfunkgerät"),
}

/** Ein nachträglich georteter Anruf — daraus wird ein Bogen mit diesem Punkt als Ort. */
internal data class Geortet(
    val anrufId: String,
    val lat: Double,
    val lon: Double,
    val text: String,
)

/** Ein Alarm im Rückholfenster — samt Stichwort fürs Band und seiner Frist. */
internal data class WartenderAlarm(
    val auftrag: Alarmauftrag,
    val stichwort: String,
    val frist: Long,
)

/**
 * Das Rückholfenster: fünf Sekunden zwischen „Alarm auslösen" und dem
 * Hinausgehen — gegen den vertippten Einsatz und die falsche Schleife.
 *
 * <b>Bewusst rein in der App:</b> Der Server kennt kein „Alarm zurück". Was bei
 * ihm ankommt, ist alarmiert. Es wartet höchstens einer; wer alarmiert, während
 * das Fenster offen steht, schickt den wartenden damit sofort los.
 */
internal class Rueckholer {
    var wartend by mutableStateOf<WartenderAlarm?>(null)
        private set
    var rest by mutableIntStateOf(RUECKHOL_SEKUNDEN)

    var absenden: (Alarmauftrag) -> Unit = {}
    var verwerfen: (String) -> Unit = {}

    fun planen(auftrag: Alarmauftrag, stichwort: String) {
        jetztAbsenden()
        rest = RUECKHOL_SEKUNDEN
        wartend = WartenderAlarm(
            auftrag = auftrag,
            stichwort = stichwort,
            frist = System.currentTimeMillis() + RUECKHOL_SEKUNDEN * 1000L,
        )
    }

    fun jetztAbsenden() {
        val w = wartend ?: return
        wartend = null
        absenden(w.auftrag)
    }

    /** Zurückgeholt heißt: Es ist nichts hinausgegangen — also auch keine Stimme. */
    fun rueckholen() {
        val w = wartend ?: return
        wartend = null
        w.auftrag.meldungId?.let { verwerfen(it) }
    }

    companion object {
        const val RUECKHOL_SEKUNDEN = 5
    }
}

/** Ein leerer Platzhalter, solange der Teil nichts hat — dieselbe Form überall. */
@Composable
internal fun Leerteil(text: String) {
    Box(Modifier.fillMaxWidth().padding(Abstand.Gross)) { Leerhinweis(text) }
}
