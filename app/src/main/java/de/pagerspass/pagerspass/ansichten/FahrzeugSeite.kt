package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.netz.Server
import de.pagerspass.pagerspass.ui.bausteine.Kapselreihe
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import de.pagerspass.pagerspass.ui.theme.kopfverlauf
import de.pagerspass.pagerspass.ui.theme.seitengrund
import de.pagerspass.pagerspass.ui.theme.statusquadrat

/**
 * Das Fahrzeug — der Dienst aus Sicht der Besatzung, übertragen aus
 * `web/src/views/FahrzeugView.vue` (Web 5.0.0.26) in der Form, die es am Handy hat
 * (`/play/mobile`, Regeln bei `max-width: 700px`).
 *
 * <b>Die Gliederung ist die des Webs:</b> der Kopf mit Statusmarke, Rufname,
 * Dienstende und Aussteigen; darunter die Hinweiszeilen; dann die Reiterreihe
 * oben — Melder (bzw. Gerät bei „Im Funk") · Fahrzeug · Funk · „?" für die
 * Spielhilfe. Eine Tableiste gibt es im Raum nicht, wie im Web.
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
    beiFms: (Int, String?, Int?) -> Unit = { _, _, _ -> },
    beiSondersignal: (Boolean) -> Unit = {},
    beiLagemeldung: (String) -> Unit = {},
    beiFunk: (String) -> Unit = {},
    beiEinsatzstelle: (String) -> Unit = {},
    beiSprechstart: () -> Unit = {},
    beiSprechende: () -> Unit = {},
    beiUeberspringen: () -> Unit = {},
    beiDienstende: () -> Unit = {},
    beiVerlassen: () -> Unit = {},
    manv: ManvGriffe = ManvGriffe(),
    befehle: Raumbefehle = Raumbefehle.Leer,
    neben: Raumneben = Raumneben(),
    /** Den Alarm am Funkgerät quittieren (Bauart „Im Funk"). */
    beiAlarmQuittieren: () -> Unit = {},
    /** Der Server dieser Sitzung — für die Wege, die ins Web führen (Premium). */
    server: String = Server.BETRIEB,
) {
    val bauart = melderbauart()
    val imFunk = bauart == "funk"
    // Der erste Reiter trägt das Gerät, das den Alarm zeigt: Piepser oder Funkgerät.
    // Bei „App" gibt es ihn nicht — der Alarm liegt dann über allem.
    val ersterReiter = bauart == "dme" || imFunk
    var gewaehlterReiter by remember { mutableStateOf("fahrzeug") }
    var kommunikation by remember { mutableStateOf("funk") }
    var gelesen by remember { mutableIntStateOf(stand.funk.size) }
    var verlassenGefragt by remember { mutableStateOf(false) }
    var dienstendeGefragt by remember { mutableStateOf(false) }
    val browser = LocalUriHandler.current

    // Fällt der erste Reiter weg (Bauart „App"), steht man im Fahrzeug.
    val reiter = if (!ersterReiter && gewaehlterReiter == "melder") "fahrzeug" else gewaehlterReiter
    // „Im Funk" steht der Alarm auf dem Funkgerät — dorthin, wenn er kommt.
    LaunchedEffect(stand.alarm?.incidentId, imFunk) {
        if (imFunk && stand.alarm != null) gewaehlterReiter = "melder"
    }
    LaunchedEffect(reiter, stand.funk.size) {
        if (reiter == "funk") gelesen = stand.funk.size
    }
    val ungelesen = if (reiter == "funk") 0 else (stand.funk.size - gelesen).coerceAtLeast(0)

    val raum = stand.raum
    val meins = raum?.vehicles?.firstOrNull { it.playerId == eigeneKennung }
    val einsatz = raum?.incidents?.firstOrNull { it.id == meins?.einsatzId }
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (raum == null || meins == null) {
        Box(modifier.fillMaxSize().seitengrund().padding(top = oben)) {
            if (raum == null) Ladezeile("Der Dienst wird geladen …", Modifier.padding(Abstand.Gross))
            else SehrLeise("Kein Fahrzeug zugeordnet.", Modifier.align(Alignment.Center), mono = true)
        }
        return
    }

    val premium = raum.players.firstOrNull { it.id == eigeneKennung }?.premium == true

    Column(modifier = modifier.fillMaxSize().background(Farben.BgTief)) {
        Fahrzeugkopf(
            raum = raum,
            meins = meins,
            oben = oben,
            beiDienstende = {
                val entscheidet = !raum.dienstendeEigeneStimme && raum.dienstendeStimmen + 1 >= raum.dienstendeSchwelle
                if (entscheidet) dienstendeGefragt = true else beiDienstende()
            },
            beiVerlassen = { verlassenGefragt = true },
            eigeneKennung = eigeneKennung,
            premium = premium,
            server = server,
            befehle = befehle,
        )

        Hinweise(raum, meins, befehle)

        Reiterreihe(Modifier.background(Farben.Flaeche)) {
            if (ersterReiter) {
                Reiter(
                    if (imFunk) "Gerät" else "Melder",
                    reiter == "melder",
                    { gewaehlterReiter = "melder" },
                    // Der Punkt sagt: Da liegt ein Alarm.
                    marke = if (stand.alarm != null && reiter != "melder") 1 else 0,
                )
            }
            Reiter("Fahrzeug", reiter == "fahrzeug", { gewaehlterReiter = "fahrzeug" })
            Reiter("Funk", reiter == "funk", { gewaehlterReiter = "funk" }, marke = ungelesen)
            // Kein Reiter, sondern ein Weg nach draußen — in die Spielhilfe.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(46.dp)
                    .defaultMinSize(minHeight = Ziel.Normal)
                    .drawBehind {
                        drawLine(Farben.Rand, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
                    }
                    .clickable(role = Role.Button, onClickLabel = "Spielhilfe öffnen") {
                        browser.openUri("https://wiki.pagerspass.de/rollen/#fahrzeugbesatzung")
                    },
            ) {
                Text("?", style = Schrift.Gross.copy(fontWeight = FontWeight.ExtraBold), color = Farben.TextSehrLeise)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (reiter == "funk") Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(Abstand.Klein),
        ) {
            Ausbildungsleiste(stand, beiUeberspringen)

            when (reiter) {
                "melder" -> if (imFunk) {
                    Funkgeraet(raum, meins, stand, neben, eigeneKennung, befehle, beiAlarmQuittieren, beiSprechstart, beiSprechende)
                } else {
                    Dienstmelder(raum, meins, stand.alarm)
                }

                "fahrzeug" -> TeilFahrzeug(
                    raum, einsatz, meins, katalog, imFunk, premium, server, manv, befehle,
                    beiFms, beiSondersignal, beiLagemeldung,
                )

                else -> TeilFunk(
                    raum, meins, stand, neben, eigeneKennung, befehle,
                    kommunikation = kommunikation,
                    beiKommunikation = { kommunikation = it },
                    mitGeraet = !imFunk,
                    beiFunk = beiFunk,
                    beiEinsatzstelle = beiEinsatzstelle,
                    beiAlarmQuittieren = beiAlarmQuittieren,
                    beiSprechstart = beiSprechstart,
                    beiSprechende = beiSprechende,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (verlassenGefragt) {
        VerlassenDialog(meins, einsatz, beiSchliessen = { verlassenGefragt = false }, beiAussteigen = {
            verlassenGefragt = false
            beiVerlassen()
        })
    }
    if (dienstendeGefragt) {
        DienstendeDialog(raum, beiSchliessen = { dienstendeGefragt = false }, beiBeenden = {
            dienstendeGefragt = false
            beiDienstende()
        })
    }
}

/**
 * Der Kopf — Statusmarke, Rufname, Dienstende, Aussteigen; in der zweiten Zeile
 * Zuschauer, Melderwahl, Tonregler und Funkbegleiter (`FahrzeugView.vue`, v6).
 *
 * <b>Die Statusmarke trägt Zeichen und Zahl in der Farbe des Status</b> — dasselbe
 * Zeichen wie die Taste, die man dafür gedrückt hat. Der Balken links trägt die
 * Farbe der Organisation: Im Vorbeisehen weiß man, in wessen Fahrzeug man sitzt.
 */
@Composable
private fun Fahrzeugkopf(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    oben: androidx.compose.ui.unit.Dp,
    beiDienstende: () -> Unit,
    beiVerlassen: () -> Unit,
    eigeneKennung: String,
    premium: Boolean,
    server: String,
    befehle: Raumbefehle,
) {
    val org = orgFarbe(meins.organisation)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .kopfverlauf()
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(Farben.Rand, Offset(0f, size.height - strich / 2f), Offset(size.width, size.height - strich / 2f), strich)
                drawRect(org, size = size.copy(width = 4.dp.toPx()))
            }
            .padding(top = oben)
            .padding(start = Abstand.Normal + 4.dp, end = Abstand.Normal, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            val farbe = fmsFarbe(meins.status)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .defaultMinSize(minWidth = 58.dp)
                    .height(40.dp)
                    .statusquadrat(farbe, ecke = 14.dp)
                    .padding(horizontal = Abstand.Klein),
            ) {
                Fahrzeugzeichen.fms(meins.status, meins.organisation)?.let {
                    Icon(it, contentDescription = null, tint = Farben.AufFarbe, modifier = Modifier.size(18.dp))
                }
                Text(meins.status.toString(), style = Schrift.Titel.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.ExtraBold), color = Farben.AufFarbe)
            }
            Column(Modifier.weight(1f)) {
                Text(meins.funkrufname, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                Row {
                    Text(
                        meins.statusText.ifBlank { "…" },
                        style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                        color = farbe,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(" · ${meins.typ}", style = Schrift.Klein, color = Farben.TextSehrLeise, maxLines = 1)
                }
            }
            if (raum.laeuft) {
                val zeigeStand = raum.dienstendeSchwelle > 1 && raum.dienstendeStimmen > 0
                Kopfknopf(
                    zeichen = Fahrzeugzeichen.Ende,
                    beschreibung = if (raum.dienstendeEigeneStimme) "Stimme fürs Dienstende zurücknehmen" else "Dienst beenden",
                    dafuer = raum.dienstendeEigeneStimme,
                    text = if (zeigeStand) "${raum.dienstendeStimmen}/${raum.dienstendeSchwelle}" else null,
                    beiDruck = beiDienstende,
                )
            }
            Kopfknopf(Fahrzeugzeichen.Raus, "Fahrzeug verlassen", dafuer = false, text = null, beiDruck = beiVerlassen)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            if (raum.zuschauer.isNotEmpty()) Marke("👁 ${raum.zuschauer.size}")
            Melderwahl(Modifier.weight(1f))
            // Ton und Begleiter teilen sich die zweite Zeile mit der Melderwahl (v6).
            DienstTonregler(raum, eigeneKennung, befehle)
            BegleiterKopfknopf(raum, premium, befehle, server)
        }
    }
}

@Composable
private fun Kopfknopf(
    zeichen: androidx.compose.ui.graphics.vector.ImageVector,
    beschreibung: String,
    dafuer: Boolean,
    text: String?,
    beiDruck: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .defaultMinSize(minWidth = Ziel.Normal, minHeight = Ziel.Normal)
            .background(if (dafuer) Farben.HauchSignal else Farben.FlaecheHoch, Rundung.Klein)
            .border(1.dp, if (dafuer) Farben.Signal else Farben.Rand, Rundung.Klein)
            .clickable(role = Role.Button, onClickLabel = beschreibung, onClick = beiDruck)
            .padding(horizontal = Abstand.Klein),
    ) {
        Icon(zeichen, contentDescription = beschreibung, tint = if (dafuer) Farben.SignalHell else Farben.Text, modifier = Modifier.size(20.dp))
        text?.let { Text(it, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text) }
    }
}

/** Die Zeilen unter dem Kopf — über die volle Breite, nicht als Kästen im Inhalt. */
@Composable
private fun Hinweise(raum: Raumzustand, meins: Rundenfahrzeug, befehle: Raumbefehle) {
    val jetzt = sekundentakt()
    val verwaist = zeitpunktMs(raum.leitstelleVerwaistSeit)?.let { jetzt - it >= 2 * 60 * 1000 } == true
    if (verwaist) {
        // Eine Zeile in der Fläche, kein schwebender Knopf — ein Fehlgriff hieße hier
        // „Leitstelle übernommen", nicht „Fenster auf".
        Hinweisleiste(
            "Die Leitstelle ist verwaist — tippen, um sie zu übernehmen",
            Farben.Amber,
            Farben.HauchAmber,
            beiDruck = befehle::leitstelleUebernehmen,
        )
    } else if (raum.kiLeitstelleWirksam) {
        Hinweisleiste("KI-Leitstelle disponiert · am Funk ansprechbar", Farben.TextLeise, Color.Transparent)
    }
    restSekunden(meins.wiederherstellungBis, jetzt)?.takeIf { it > 0 }?.let { rest ->
        Hinweisleiste(
            "${meins.ausserDienstGrund ?: "Wiederherstellung"} — noch ${if (rest < 60) "$rest s" else "${(rest + 59) / 60} min"}, dann wieder einsatzbereit",
            Farben.TextLeise,
            Farben.HauchHell,
        )
    }
    meins.notarztBei?.let {
        Hinweisleiste("Notarzt begleitet $it — bis zur Übergabe ohne Notarzt unterwegs", Farben.TextLeise, Farben.HauchHell)
    }
    meins.besatzungsluecken.forEach { l ->
        Hinweisleiste(
            "⚑ ${if (l.soll > 1) "${l.name} ${l.ist}/${l.soll}" else "ohne ${l.name}"} — diese Arbeit kann euer Fahrzeug heute nicht übernehmen",
            Farben.Amber,
            Farben.HauchHell,
        )
    }
    if (meins.status == 5 || meins.status == 0) {
        Hinweisleiste("Sprechwunsch angemeldet — die Leitstelle meldet sich", Farben.BlauHell, Farben.Blau.copy(alpha = 0.16f), blinkt = true)
    }
}

/**
 * Der Reiter „Fahrzeug" — in der Reihenfolge, die der Handyzweig zeigt. Ohne Auftrag
 * und auf der Anfahrt steht die Karte oben (dort sieht man während der Fahrt hin),
 * sonst unter der Arbeit (`karteOben` in FahrzeugView.vue).
 */
@Composable
private fun ColumnScope.TeilFahrzeug(
    raum: Raumzustand,
    einsatz: de.pagerspass.pagerspass.netz.Einsatz?,
    meins: Rundenfahrzeug,
    katalog: Katalog?,
    imFunk: Boolean,
    premium: Boolean,
    server: String,
    manv: ManvGriffe,
    befehle: Raumbefehle,
    beiFms: (Int, String?, Int?) -> Unit,
    beiSondersignal: (Boolean) -> Unit,
    beiLagemeldung: (String) -> Unit,
) {
    val karteOben = einsatz == null || meins.status == 3
    val karte: @Composable () -> Unit = {
        de.pagerspass.pagerspass.ui.karte.Lagekarte(
            raum = raum,
            eigenesFahrzeugId = meins.id,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
    }
    val fms: @Composable ColumnScope.() -> Unit = { FmsKasten(raum, einsatz, meins, katalog, imFunk, befehle, beiFms, beiSondersignal) }
    val bogen: @Composable ColumnScope.() -> Unit = {
        ManvVersorgung(raum, einsatz, meins, manv, befehle)
        if (einsatz != null) {
            PatientenFenster(einsatz, meins, premium, server, befehle)
            BeteiligtenFenster(einsatz, meins, befehle)
            FeuerwehrFenster(einsatz, meins, befehle)
            Tabletstarter(raum, einsatz, meins, manv, befehle)
        }
    }
    val leitung: @Composable ColumnScope.() -> Unit = {
        EinsatzleitungPanel(einsatz, meins) { zweig -> einsatz?.let { manv.uebernehmen(it.id, zweig) } }
    }

    if (karteOben) {
        karte()
        Feststellungskasten(meins, katalog, befehle)
        bogen()
        fms()
        Einsatzkarte(raum, einsatz, meins, befehle)
        Lagemeldungskasten(einsatz, katalog, beiLagemeldung, befehle)
        leitung()
    } else {
        Einsatzkarte(raum, einsatz, meins, befehle)
        fms()
        Feststellungskasten(meins, katalog, befehle)
        leitung()
        bogen()
        karte()
        Lagemeldungskasten(einsatz, katalog, beiLagemeldung, befehle)
    }
}

/** Das Handfunkgerät — Display, Sprechtaste, Einzelruf und Notruf (`FunkgeraetVoll.vue`). */
@Composable
private fun ColumnScope.Funkgeraet(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    stand: Rundenstand,
    neben: Raumneben,
    eigeneKennung: String,
    befehle: Raumbefehle,
    beiAlarmQuittieren: () -> Unit,
    beiSprechstart: () -> Unit,
    beiSprechende: () -> Unit,
) {
    Funkdisplay(
        alarm = if (alarmImFunk()) stand.alarm else null,
        rufname = meins.funkrufname,
        gruppe = raum.settings.funkgruppen.firstOrNull { it.id == meins.funkgruppe }?.let { it.bezeichnung.ifBlank { it.name } },
        sprecher = stand.sprecher.values.firstOrNull(),
        beiQuittieren = beiAlarmQuittieren,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    Sprechtaste(
        sendet = stand.sendet,
        wirdVerstanden = stand.wirdVerstanden,
        belegtVon = stand.sprecher.values.firstOrNull(),
        gesperrtBis = stand.funkGesperrtBis,
        beiDruck = beiSprechstart,
        beiLoslassen = beiSprechende,
    )
    stand.funkhinweis?.let { SehrLeise(it) }
    Geraetegriffe(raum, meins, eigeneKennung, befehle)
}

/**
 * Der Reiter „Funk": Funkverkehr · Einsatzstelle · Handfunkgerät.
 *
 * Die Einsatzstelle schaltet mit Status 4 an der Lage frei — vorher steht sie
 * gesperrt da und sagt warum („S4"). Liegt das Gerät schon im ersten Reiter
 * (Bauart „Im Funk"), fällt „Handfunkgerät" hier weg.
 */
@Composable
private fun ColumnScope.TeilFunk(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    stand: Rundenstand,
    neben: Raumneben,
    eigeneKennung: String,
    befehle: Raumbefehle,
    kommunikation: String,
    beiKommunikation: (String) -> Unit,
    mitGeraet: Boolean,
    beiFunk: (String) -> Unit,
    beiEinsatzstelle: (String) -> Unit,
    beiAlarmQuittieren: () -> Unit,
    beiSprechstart: () -> Unit,
    beiSprechende: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stelleFrei = meins.status == 4 && meins.einsatzstelleErreicht && meins.einsatzId != null
    val rolle = rememberScrollState()
    // Die jüngste Zeile steht unten — wer mitliest, soll nicht erst blättern.
    LaunchedEffect(stand.funk.size, raum.einsatzstellenchat.size, kommunikation) {
        rolle.animateScrollTo(rolle.maxValue)
    }
    val wahl = when {
        kommunikation == "einsatzstelle" && !stelleFrei -> "funk"
        kommunikation == "geraet" && !mitGeraet -> "funk"
        else -> kommunikation
    }
    Kapselreihe(
        seiten = listOfNotNull("funk", "einsatzstelle", "geraet".takeIf { mitGeraet }),
        gewaehlt = wahl,
        beiWahl = beiKommunikation,
        aufschrift = {
            when {
                it == "funk" -> "Funkverkehr"
                it == "geraet" -> "Handfunkgerät"
                stelleFrei -> "Einsatzstelle"
                else -> "Einsatzstelle · S4"
            }
        },
        gesperrt = { it == "einsatzstelle" && !stelleFrei },
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .flaeche(ecke = 14.dp)
            .verticalScroll(rolle)
            .padding(Abstand.Klein),
    ) {
        when (wahl) {
            "einsatzstelle" -> {
                Einsatzstellenfaden(zeilen = raum.einsatzstellenchat, beiSenden = beiEinsatzstelle)
                // Vor Ort wird auch gesprochen — DMO, Gerät zu Gerät.
                Sprechtaste(
                    sendet = neben.sendetAuf == "stelle",
                    wirdVerstanden = false,
                    belegtVon = neben.stellenSprecher,
                    gesperrtBis = null,
                    beiDruck = { befehle.einsatzstelleSprechenStarten() },
                    beiLoslassen = { befehle.einsatzstelleSprechenBeenden() },
                )
            }
            "geraet" -> Funkgeraet(raum, meins, stand, neben, eigeneKennung, befehle, beiAlarmQuittieren, beiSprechstart, beiSprechende)
            else -> {
                Kanalzeile(raum, meins, stand)
                Funkprotokoll(
                    zeilen = stand.funk,
                    eigenerRufname = meins.funkrufname,
                    laeuft = stand.laeuft,
                    beiSenden = beiFunk,
                    fahrzeuge = raum.vehicles,
                    gesperrt = funkGesperrt(raum, eigeneKennung),
                )
                stand.funkhinweis?.let { SehrLeise(it) }
            }
        }
    }
}

/** Die Kanalzeile — wer gerade spricht, und auf welchem Kanal ich sende. */
@Composable
private fun Kanalzeile(raum: Raumzustand, meins: Rundenfahrzeug, stand: Rundenstand) {
    val sprecher = stand.sprecher.values.firstOrNull()
    val (wort, farbe) = when {
        stand.sendet -> "Du sendest" to Farben.Signal
        sprecher != null -> "$sprecher spricht" to Farben.Amber
        else -> "Kanal frei" to Farben.GruenHell
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(farbe, Rundung.Rund))
        Text(wort, style = Schrift.MonoKlein, color = farbe, modifier = Modifier.weight(1f))
        if (raum.settings.funkgruppen.isNotEmpty()) {
            val gruppe = raum.settings.funkgruppen.firstOrNull { it.id == (raum.sendegruppe ?: meins.funkgruppe) }
            SehrLeise("auf ${gruppe?.let { g -> g.bezeichnung.ifBlank { g.name } } ?: "Kreiskanal"}", mono = true)
        }
    }
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
