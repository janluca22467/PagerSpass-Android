package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Fahrzeugkennung
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.mobil.rememberAlarmierungsart
import de.pagerspass.pagerspass.mobil.rememberKennungsform
import de.pagerspass.pagerspass.mobil.rememberMelderBauform
import de.pagerspass.pagerspass.mobil.rememberMelderTon
import de.pagerspass.pagerspass.mobil.rememberMelderwerk
import de.pagerspass.pagerspass.mobil.rememberTonwahl
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Teil
import de.pagerspass.pagerspass.ui.bausteine.Teilleiste
import de.pagerspass.pagerspass.ui.karte.Lagekarte
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import de.pagerspass.pagerspass.ui.theme.raster
import de.pagerspass.pagerspass.ui.zeichen.Zeichen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Die Griffe des Fahrzeugs — ein Bündel statt vierzig Parametern.
 *
 * Gefüllt im Rundenrahmen (`mobil/Fahrzeuggriffe.kt`) aus der `Runde`; hier steht
 * nur, <em>was</em> die Seite tun können muss, nicht wie es über die Leitung geht.
 */
class FahrzeugGriffe(
    val fms: (Int, String?, Int?) -> Unit = { _, _, _ -> },
    val sondersignal: (Boolean) -> Unit = {},
    val lagemeldung: (String) -> Unit = {},
    val nachfordern: (String) -> Unit = {},
    val funk: Funkgriffe = Funkgriffe(),
    val ueberspringen: () -> Unit = {},
    /** Fürs Dienstende stimmen — oder die Stimme zurücknehmen. */
    val dienstende: () -> Unit = {},
    /** Aussteigen, ohne weitere Rückfrage — gefragt hat die Seite schon. */
    val verlassen: () -> Unit = {},
    val hilfe: () -> Unit = {},
    /** `AcknowledgeAlarm` — der Server erfährt, dass jemand reagiert hat. */
    val alarmQuittieren: () -> Unit = {},
    /** Den Alarm der Runde abräumen, ohne zu quittieren (Status 3/4 schließt ihn am Server). */
    val alarmWeg: () -> Unit = {},
    val leitstelleUebernehmen: () -> Unit = {},
    /** Fahrzeug, an. */
    val streife: (String, Boolean) -> Unit = { _, _ -> },
    val streifeneinsatz: (Feststellungsmeldung) -> Unit = {},
    val wasserAufnehmen: () -> Unit = {},
    val aufgabeUebernehmen: (Int?) -> Unit = {},
    val patient: Patientengriffe = Patientengriffe(),
    val einzelruf: BegleiterGriffe = BegleiterGriffe(),
    val einzelrufZulassen: (Boolean) -> Unit = {},
    /** Der Tonregler auf die Lautsprecher der Runde (`Runde.tonAnwenden`). */
    val ton: (de.pagerspass.pagerspass.mobil.Tonstand) -> Unit = {},
    /** Das Mikrofon im Einzelruf zwischen zwei Menschen — an mit der Annahme, aus mit dem Ende. */
    val einzelrufMikrofon: (Boolean) -> Unit = {},
    /** Der QR-Zugang zum Funkbegleiter — liefert den Link fürs Handy. */
    val begleiterZugang: suspend () -> Result<String> = { Result.failure(IllegalStateException("Kein Zugang.")) },
    val premium: () -> Unit = {},
)

/**
 * Das Fahrzeug — der Dienst aus Sicht der Besatzung.
 *
 * Übertragen aus `web/src/views/FahrzeugView.vue` im Handyzweig: fester Kopf mit
 * Statusmarke und den Schaltern der Bedienung, darunter die Hinweisbänder, dann
 * die Reiter — **Melder** (Bauart DME) oder **Gerät** (Bauart „Im Funk"),
 * **Fahrzeug** und **Funk** mit der Zahl ungelesener Sprüche — und ganz rechts
 * der Weg in die Spielhilfe.
 *
 * <b>Der Melder gehört dem Gerät, nicht dem Server.</b> Welche Meldung auf dem
 * Display steht und ob sie quittiert ist, führt diese Seite ([Melderstand]); der
 * Server kennt nur `alarmOffen`. Quittieren stellt Ton und Vibration ab, die
 * Meldung bleibt bis Status 3 — man will auf der Fahrt noch lesen, wohin es geht.
 *
 * <b>Der Ablauf einer Fahrt ist FMS:</b> Der Melder weckt, Status 3 rückt aus,
 * Status 4 meldet die Ankunft, die Lagemeldung sagt der Leitstelle, was Sache ist,
 * Status 1 macht wieder frei. Die Engine fährt das Fahrzeug; die Besatzung meldet.
 */
@Composable
fun FahrzeugSeite(
    modifier: Modifier = Modifier,
    stand: Rundenstand = Rundenstand(),
    eigeneKennung: String = "",
    katalog: Katalog? = null,
    konto: Konto? = null,
    melderGesicht: String? = null,
    griffe: FahrzeugGriffe = FahrzeugGriffe(),
    manv: ManvGriffe = ManvGriffe(),
) {
    val raum = stand.raum
    val meins = raum?.vehicles?.firstOrNull { it.playerId == eigeneKennung && eigeneKennung.isNotEmpty() }
    val einsatz = raum?.incidents?.firstOrNull { it.id == meins?.einsatzId }
    val ich = raum?.players?.firstOrNull { it.id == eigeneKennung }
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val tonwahl = rememberTonwahl()
    val werk = rememberMelderwerk()
    val bauform by rememberMelderBauform()
    val ton by rememberMelderTon()
    val art by rememberAlarmierungsart()
    val form by rememberKennungsform()
    val jetzt = rememberSekundenuhr()

    val stufe = konto?.level ?: ich?.level ?: 1
    val premium = konto?.premiumAktiv ?: (ich?.premium == true)
    val melderAmHandy = stand.begleiterGekoppelt
    val alarmpegel = tonwahl.alarmpegel(art)
    val lautlos = tonwahl.geraetAus || art == "stumm" || art == "vibration" || tonwahl.melder <= 0f
    val kreis = raum?.settings?.landkreis.orEmpty()
    val kennung: (Rundenfahrzeug) -> String = { f ->
        Fahrzeugkennung.kennung(f.kurzname, f.typ, f.organisation, f.hiOrg, form, kreis)
    }

    // ------------------------------------------------------------ Der Melder

    val melder = remember { Melderstand() }

    // Ein neuer Alarm kommt als Ereignis — und nur das Ereignis darf den Melder wecken.
    LaunchedEffect(stand.alarm) { stand.alarm?.let { melder.neu(it) } }

    // Ein Einsatz, der diesem Fahrzeug nicht mehr gehört, gehört auch vom Display —
    // geprüft bei jedem neuen Raumstand, nicht bei jedem Positions-Tick.
    LaunchedEffect(raum?.version, meins?.einsatzId) {
        val oberste = melder.aktuell
        if (meins != null && oberste != null && meins.einsatzId != oberste.incidentId && raum?.version != 0L) {
            melder.raeumen()
        }
    }

    // Quittiert ist quittiert — auf jedem Bildschirm des Platzes. Fällt `alarmOffen`,
    // während hier noch ein Alarm tönt, hat jemand am gekoppelten Handy gedrückt.
    var warOffen by remember { mutableStateOf(meins?.alarmOffen) }
    LaunchedEffect(meins?.alarmOffen) {
        val offen = meins?.alarmOffen
        if (warOffen == true && offen == false && melder.hatAlarm) melder.quittieren()
        warOffen = offen
    }

    // Ton und Vibration — nicht, wenn ein Handy am Platz der Melder ist: Zwei
    // Melder, die dieselbe Schleife versetzt spielen, sind ein Echo.
    val tonAktiv = melder.hatAlarm && !melderAmHandy
    LaunchedEffect(tonAktiv, melder.aktuell, ton, alarmpegel) {
        val a = melder.aktuell
        if (tonAktiv && a != null) werk.alarmStarten(ton, a.prioritaet, alarmpegel) else werk.alarmStoppen()
    }
    LaunchedEffect(melder.aktuell?.zeit, melder.aktuell?.incidentId) {
        if (tonAktiv && !tonwahl.geraetAus && (art == "voll" || art == "vibration")) werk.vibrationAlarm()
    }

    // Die Ruferinnerung: eine quittierte, aber stehende Meldung meldet sich jede Minute.
    val erinnern = melder.meldungSteht && melder.quittiert && !melderAmHandy
    LaunchedEffect(erinnern, melder.aktuell?.incidentId) {
        while (erinnern && isActive) {
            delay(ERINNERUNG_TAKT_MS)
            werk.erinnerung(alarmpegel)
        }
    }

    // Der Tonregler ist derselbe wie am Leitstellentisch (`Tonregler.kt`): Dreht jemand
    // dort, zieht der Spiegel hier nach — und die Pegel der Runde folgen, auch die
    // Durchsage der Leitstelle beim Alarm.
    val tonstand by de.pagerspass.pagerspass.mobil.rememberTonstand()
    LaunchedEffect(tonstand) { tonwahl.uebernehmen(tonstand) }
    val rundenton = tonwahl.rundenton
    LaunchedEffect(rundenton) { griffe.ton(rundenton) }

    val quittieren: () -> Unit = {
        if (melder.meldungSteht && !melder.quittiert) {
            melder.quittieren()
            werk.alarmStoppen()
            werk.vibrationAus()
            if (bauform != "fax" && bauform != "monitor") werk.quittung(alarmpegel)
            griffe.alarmQuittieren()
        }
    }

    // Status 3 und 4 räumen den Melder — der Einsatz ist übernommen.
    val fms: (Int, String?, Int?) -> Unit = { status, grund, dauer ->
        if (status == 3 || status == 4) {
            melder.raeumen()
            werk.alarmStoppen()
            werk.vibrationAus()
            griffe.alarmWeg()
        }
        werk.statusVersand(tonwahl.funkpegel)
        griffe.fms(status, grund, dauer)
    }

    val ausruecken: () -> Unit = {
        if (melder.meldungSteht) {
            quittieren()
            fms(3, null, null)
        }
    }

    // ------------------------------------------------------------ Die Reiter

    val ersterReiter: String? = when {
        melderAmHandy -> null
        tonwahl.bauart == "dme" -> "melder"
        tonwahl.bauart == "funk" -> "geraet"
        else -> null
    }
    var reiter by remember { mutableStateOf("fahrzeug") }
    if ((reiter == "melder" || reiter == "geraet") && ersterReiter != reiter) reiter = "fahrzeug"
    var gelesen by remember { mutableIntStateOf(stand.funk.size) }
    val ungelesen = if (reiter == "funk") 0 else (stand.funk.size - gelesen).coerceAtLeast(0)
    LaunchedEffect(reiter, stand.funk.size) { if (reiter == "funk") gelesen = stand.funk.size }

    var verlassenFragen by remember { mutableStateOf(false) }
    var dienstendeFragen by remember { mutableStateOf(false) }
    var tonreglerOffen by remember { mutableStateOf(false) }
    var begleiterOffen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Brush.verticalGradient(listOf(Farben.Bg, Farben.BgTief))) }
                .raster(),
        ) {
            if (raum == null || meins == null) {
                Box(Modifier.padding(top = oben)) {
                    Ladezeile("Der Dienst wird geladen …", Modifier.padding(Abstand.Gross))
                }
                return@Column
            }

            Fahrzeugkopf(
                raum = raum,
                meins = meins,
                oben = oben,
                tonwahl = tonwahl,
                griffe = Kopfgriffe(
                    dienstende = {
                        val entscheidet = !raum.dienstendeEigeneStimme &&
                            raum.dienstendeStimmen + 1 >= raum.dienstendeSchwelle
                        if (entscheidet) dienstendeFragen = true else griffe.dienstende()
                    },
                    verlassen = { verlassenFragen = true },
                    hilfe = griffe.hilfe,
                    tonregler = { tonreglerOffen = true },
                    begleiter = { begleiterOffen = true },
                ),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig),
            ) {
                Fahrzeugbaender(raum, meins, jetzt, griffe.leitstelleUebernehmen)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Gross),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Abstand.Gross),
            ) {
                Ausbildungsleiste(stand, griffe.ueberspringen)

                when (reiter) {
                    "melder" -> MelderGeraet(
                        bauform = bauform,
                        stufe = stufe,
                        premium = premium,
                        gesichtId = melderGesicht,
                        melder = melder,
                        verlauf = stand.melderverlauf,
                        fahrzeug = meins,
                        kennung = kennung(meins),
                        raum = raum,
                        menue = rememberMeldermenue(premium, tonwahl, stand.melderverlauf) { werk.taste(alarmpegel) },
                        lautlos = lautlos,
                        griffe = Meldergriffe(
                            quittieren = quittieren,
                            ausruecken = ausruecken,
                            taste = { werk.taste(alarmpegel) },
                        ),
                    )

                    "geraet" -> Geraetefeld {
                        Handfunkgeraet(
                            stand = stand,
                            raum = raum,
                            meins = meins,
                            einsatz = einsatz,
                            alarmzeilen = melder.aktuell?.takeIf { melder.hatAlarm }?.let { meldungszeilen(it) },
                            jetzt = jetzt,
                            tonwahl = tonwahl,
                            beiTaste = { werk.taste(tonwahl.funkpegel) },
                            beiQuittieren = quittieren,
                            beiFms = fms,
                            griffe = griffe.funk,
                        )
                    }

                    "funk" -> FahrzeugFunkteil(
                        stand = stand,
                        raum = raum,
                        meins = meins,
                        einsatz = einsatz,
                        mitGeraet = ersterReiter != "geraet",
                        geraet = {
                            Handfunkgeraet(
                                stand = stand,
                                raum = raum,
                                meins = meins,
                                einsatz = einsatz,
                                alarmzeilen = if (tonwahl.bauart == "funk") {
                                    melder.aktuell?.takeIf { melder.hatAlarm }?.let { meldungszeilen(it) }
                                } else {
                                    null
                                },
                                jetzt = jetzt,
                                tonwahl = tonwahl,
                                beiTaste = { werk.taste(tonwahl.funkpegel) },
                                beiQuittieren = quittieren,
                                beiFms = fms,
                                griffe = griffe.funk,
                            )
                        },
                        griffe = griffe.funk,
                    )

                    else -> TeilFahrzeug(
                        stand = stand,
                        meins = meins,
                        katalog = katalog,
                        jetzt = jetzt,
                        kennung = kennung,
                        melderAmHandy = melderAmHandy,
                        tonwahl = tonwahl,
                        werk = werk,
                        fms = fms,
                        griffe = griffe,
                        manv = manv,
                    )
                }
            }

            FahrzeugEinzelruf(stand, tonwahl, griffe.einzelruf, griffe.einzelrufMikrofon)

            val teile = buildList {
                when (ersterReiter) {
                    "melder" -> add(Teil("melder", "Melder", Zeichen.Melder))
                    "geraet" -> add(Teil("geraet", "Gerät", Zeichen.Funk))
                }
                add(Teil("fahrzeug", "Fahrzeug", Zeichen.Fahrzeug))
                add(Teil("funk", "Funk", Zeichen.Kanal))
                add(Teil("hilfe", "Hilfe", Zeichen.Wiki))
            }
            Teilleiste(
                teile = teile,
                offen = reiter,
                beiWahl = { id -> if (id == "hilfe") griffe.hilfe() else reiter = id },
                marken = if (ungelesen > 0) mapOf("funk" to ungelesen) else emptyMap(),
                // Der Punkt sagt: da liegt ein Alarm — auch wer gerade auf den Tasten war.
                ruft = if (melder.meldungSteht && ersterReiter != null) setOf(ersterReiter) else emptySet(),
            )
        }

        // Die Alarm-App liegt über allem — nur bei der Bauart „App".
        val alarm = melder.aktuell
        if (raum != null && meins != null && alarm != null && tonwahl.bauart == "app" && !melderAmHandy) {
            AlarmAppUeberlagerung(
                alarm = alarm,
                quittiert = melder.quittiert,
                eigenerRufname = meins.funkrufname,
                jetzt = jetzt,
                beiQuittieren = quittieren,
                beiAusruecken = ausruecken,
            )
        }
    }

    if (raum != null && meins != null) {
        if (verlassenFragen) {
            VerlassenBlende(
                meins = meins,
                einsatz = einsatz,
                beiBleiben = { verlassenFragen = false },
                beiAussteigen = {
                    verlassenFragen = false
                    griffe.verlassen()
                },
            )
        }
        if (dienstendeFragen) {
            DienstendeBlende(
                raum = raum,
                beiWeiter = { dienstendeFragen = false },
                beiBeenden = {
                    dienstendeFragen = false
                    griffe.dienstende()
                },
            )
        }
        if (tonreglerOffen) {
            FahrzeugTonregler(
                tonwahl = tonwahl,
                einzelrufZulassen = ich?.einzelrufZulassen ?: true,
                beiEinzelrufZulassen = griffe.einzelrufZulassen,
                tonname = alarmtonName(ton),
                beiSchliessen = { tonreglerOffen = false },
            )
        }
        if (begleiterOffen) {
            FahrzeugBegleiterBlende(
                premium = ich?.premium == true || premium,
                gekoppelt = stand.begleiterGekoppelt,
                beiZugang = griffe.begleiterZugang,
                beiPremium = griffe.premium,
                beiSchliessen = { begleiterOffen = false },
            )
        }
    }
}

/**
 * Der Fahrzeug-Teil: Karte, Einsatz, FMS und Sondersignal, Lagemeldung, die
 * Feststellung der Streife, Führung und Patienten.
 *
 * <b>Wann die Karte oben steht:</b> ohne Auftrag und auf der Anfahrt (Status 3) —
 * dann ist sie das, worauf man sieht. Am Einsatzort zählen Tasten, Aufgaben und
 * Lagemeldung, und die Karte darf nach unten (`karteOben` im Web).
 */
@Composable
private fun ColumnScope.TeilFahrzeug(
    stand: Rundenstand,
    meins: Rundenfahrzeug,
    katalog: Katalog?,
    jetzt: Long,
    kennung: (Rundenfahrzeug) -> String,
    melderAmHandy: Boolean,
    tonwahl: de.pagerspass.pagerspass.mobil.Tonwahl,
    werk: de.pagerspass.pagerspass.mobil.Melderwerk,
    fms: (Int, String?, Int?) -> Unit,
    griffe: FahrzeugGriffe,
    manv: ManvGriffe,
) {
    val raum = stand.raum ?: return
    val einsatz = raum.incidents.firstOrNull { it.id == meins.einsatzId }
    val karteOben = einsatz == null || meins.status == 3
    val karte: @Composable () -> Unit = {
        Lagekarte(raum = raum, eigenesFahrzeugId = meins.id, modifier = Modifier.fillMaxWidth().height(220.dp))
    }

    if (karteOben) karte()

    FahrzeugEinsatzkarte(
        raum = raum,
        meins = meins,
        einsatz = einsatz,
        jetzt = jetzt,
        kennung = kennung,
        melderAmHandy = melderAmHandy,
        beiWasser = griffe.wasserAufnehmen,
        beiAufgabe = { griffe.aufgabeUebernehmen(it) },
    )

    FmsKasten(
        raum = raum,
        meins = meins,
        einsatz = einsatz,
        katalog = katalog,
        jetzt = jetzt,
        imFunk = tonwahl.bauart == "funk" && !melderAmHandy,
        tonwahl = tonwahl,
        werk = werk,
        beiFms = fms,
        beiSondersignal = griffe.sondersignal,
        beiStreife = { griffe.streife(meins.id, it) },
    )

    Lagemeldungskasten(
        einsatz = einsatz,
        katalog = katalog,
        beiLagemeldung = griffe.lagemeldung,
        beiNachfordern = griffe.nachfordern,
    )

    if (meins.aufStreife) {
        Feststellungskasten(meins = meins, katalog = katalog, beiAnlegen = griffe.streifeneinsatz)
    }

    ManvBereich(
        einsatz = einsatz,
        raum = raum,
        meins = meins,
        griffe = manv,
        kennung = kennung,
        beiNachfordern = griffe.nachfordern,
    )

    Patientenfenster(einsatz = einsatz, meins = meins, jetzt = jetzt, griffe = griffe.patient)

    if (!karteOben) karte()
}

/**
 * Die Hinweisleiste der Ausbildungsschicht.
 *
 * Der Server führt durch die Schicht — Schritt für Schritt, mit einem zweiten,
 * deutlicheren Hinweis, wenn einer länger offen bleibt. Die App zeigt an und
 * bietet das Überspringen; entschieden wird am Server.
 */
@Composable
fun ColumnScope.Ausbildungsleiste(stand: Rundenstand, beiUeberspringen: () -> Unit) {
    val ausbildung = stand.raum?.ausbildung ?: return
    if (ausbildung.abgeschlossen) return

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche()
            .flaechenmarke(wartet = true)
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = ausbildung.titel ?: "Ausbildung",
                style = Schrift.Gross,
                color = Farben.Text,
                modifier = Modifier.weight(1f),
            )
            Marke("${ausbildung.schritt + 1}/${ausbildung.schritte}")
        }

        (ausbildung.hinweis ?: ausbildung.text)?.let {
            Text(text = it, style = Schrift.Klein, color = Farben.TextLeise)
        }

        Row {
            Knopf("Überspringen", beiUeberspringen, art = Knopfart.Leise, kompakt = true)
        }
    }
}
