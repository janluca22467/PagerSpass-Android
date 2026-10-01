package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.Anruf
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.FAKTENARTEN
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Stichwort
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.RunderHaken
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Teil
import de.pagerspass.pagerspass.ui.bausteine.Teilleiste
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.kopfverlauf
import de.pagerspass.pagerspass.ui.theme.seitengrund
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Die Leitstelle — der Dienst aus Sicht des Disponenten.
 *
 * Übertragen aus `web/src/views/LeitstelleView.vue` in die Handyform: fester
 * Kopf, ein Teil, Reiter unten — Einsätze · Fahrzeuge · Notruf · Funk · Mehr.
 * Am Rechner stehen Lagekarte und Tableau nebeneinander; auf einer Handbreit
 * zeigt eine Seite einen Teil, und die Reiter tragen die Marken: Wo es
 * klingelt, steht eine Zahl.
 *
 * <b>Der Kreislauf der Leitstelle:</b> Der Notruf kommt herein, das Gespräch
 * füllt den Vorschlag, der Vorschlag wird ein Einsatz, der Einsatz alarmiert
 * Fahrzeuge — und das Funkprotokoll erzählt, wie es weitergeht.
 */
@Composable
fun LeitstelleSeite(
    modifier: Modifier = Modifier,
    stand: Rundenstand = Rundenstand(),
    katalog: Katalog? = null,
    beiEinsatzAnlegen: (Stichwort, String, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    beiAlarmieren: (String, List<String>, Map<String, String>, String?, String?) -> Unit =
        { _, _, _, _, _ -> },
    beiVorschlag: suspend (String) -> List<String> = { emptyList() },
    beiSchliessen: (String) -> Unit = {},
    beiSprechwunsch: (String) -> Unit = {},
    beiAnrufAnnehmen: (String) -> Unit = {},
    beiAnrufFrage: (String, String) -> Unit = { _, _ -> },
    beiAnrufBeenden: (String) -> Unit = {},
    beiAnrufAbweisen: (String) -> Unit = {},
    beiVorschlagVerwerfen: (String) -> Unit = {},
    beiUmstufen: (String, Int) -> Unit = { _, _ -> },
    beiFunk: (String, String?) -> Unit = { _, _ -> },
    beiSprechstart: () -> Unit = {},
    beiSprechende: () -> Unit = {},
    beiDraht: (String) -> Unit = {},
    beiUeberspringen: () -> Unit = {},
    beiDienstende: () -> Unit = {},
    beiVerlassen: () -> Unit = {},
    eigeneKennung: String = "",
    befehle: Raumbefehle = Raumbefehle.Leer,
    neben: Raumneben = Raumneben(),
) {
    var reiter by remember { mutableStateOf(Leitstellenteil.Einsaetze) }
    var kartenwahl by remember { mutableStateOf<String?>(null) }
    var neuOffen by remember { mutableStateOf(false) }
    var alarmFuer by remember { mutableStateOf<Einsatz?>(null) }
    var gespraech by remember { mutableStateOf<String?>(null) }
    var bogenFuer by remember { mutableStateOf<String?>(null) }
    var fahrzeugFuer by remember { mutableStateOf<String?>(null) }
    var besatzungOffen by remember { mutableStateOf(false) }
    var warnungOffen by remember { mutableStateOf(false) }

    val raum = stand.raum
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val klingelnde = raum?.anrufe?.count { it.klingelt } ?: 0
    val sprechwuensche = raum?.vehicles?.count { it.sprechwunschSeit != null } ?: 0
    val offene = raum?.incidents?.count { !it.abgeschlossen } ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .seitengrund(),
    ) {
        Leitstellenkopf(raum, oben, beiVerlassen)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (raum == null) {
                Ladezeile("Der Arbeitsplatz wird geladen …", Modifier.padding(Abstand.Gross))
            } else if (reiter == Leitstellenteil.Karte) {
                // Die Karte füllt den Teil und rollt nicht — Wischen gehört
                // ihr, nicht der Seite.
                de.pagerspass.pagerspass.ui.karte.Lagekarte(
                    raum = raum,
                    ausgewaehlt = kartenwahl,
                    beiWahl = { kartenwahl = it },
                    modifier = Modifier.fillMaxSize().padding(Abstand.Gross),
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Gross),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Abstand.Gross),
                ) {
                    Ausbildungsleiste(stand, beiUeberspringen)

                    when (reiter) {
                        Leitstellenteil.Einsaetze -> {
                            Feststellungsband(raum, befehle)
                            TeilEinsaetze(
                                raum = raum,
                                beiNeu = { neuOffen = true },
                                // Ein offener Einsatz will alarmiert werden — dorthin
                                // direkt; ein laufender öffnet seinen Bogen.
                                beiAlarm = { e -> if (e.offen) alarmFuer = e else bogenFuer = e.id },
                                beiSchliessen = beiSchliessen,
                                beiWuerfeln = if (raum.settings.mode == "Frei") befehle::einsatzWuerfeln else null,
                            )
                        }

                        // Die Karte wird oben ohne Rollspalte gezeichnet.
                        Leitstellenteil.Karte -> Unit

                        Leitstellenteil.Fahrzeuge -> TeilFahrzeuge(
                            raum = raum,
                            beiSprechwunsch = beiSprechwunsch,
                            beiFahrzeug = { fahrzeugFuer = it.id },
                        )

                        Leitstellenteil.Notruf -> {
                            TeilNotruf(
                                raum = raum,
                                beiAnnehmen = { id ->
                                    beiAnrufAnnehmen(id)
                                    gespraech = id
                                },
                                beiOeffnen = { gespraech = it },
                                beiAbweisen = beiAnrufAbweisen,
                            )
                            Anrufjournal(raum, befehle)
                        }

                        Leitstellenteil.Funk -> TeilLeitstellenfunk(
                            stand, raum, beiFunk, beiSprechstart, beiSprechende, beiDraht,
                            neben, befehle,
                        )

                        Leitstellenteil.Mehr -> TeilMehrLeitstelle(
                            raum = raum,
                            befehle = befehle,
                            beiDienstende = beiDienstende,
                            beiBesatzung = { besatzungOffen = true },
                            beiWarnung = { warnungOffen = true },
                        )
                    }
                }
            }
        }

        Teilleiste(
            teile = Leitstellenteil.entries.map { Teil(it.name, it.titel, it.zeichen) },
            offen = reiter.name,
            marken = mapOf(
                Leitstellenteil.Notruf.name to klingelnde,
                Leitstellenteil.Fahrzeuge.name to sprechwuensche,
                Leitstellenteil.Einsaetze.name to offene,
                Leitstellenteil.Mehr.name to if (raum?.istHost == true) {
                    raum.platzanfragen.size + raum.beitrittsanfragen.size
                } else {
                    0
                },
            ),
            // Ein klingelnder Notruf ruft — er ist das Dringendste, was eine
            // Leitstelle haben kann, und er wartet nicht.
            ruft = if (klingelnde > 0) setOf(Leitstellenteil.Notruf.name) else emptySet(),
            beiWahl = { id -> reiter = Leitstellenteil.valueOf(id) },
        )
    }

    if (neuOffen && katalog != null) {
        // Die echten Straßen des Kreises — einmal geladen, danach aus dem Speicher.
        val kreisId = raum?.settings?.landkreisId
        LaunchedEffect(kreisId) { kreisId?.let { befehle.strassenLaden(it) } }
        EinsatzAnlegen(
            katalog = katalog,
            strassen = if (kreisId != null && neben.strassenFuer == kreisId) neben.strassen else emptyList(),
            beiAnlegen = { stichwort, meldebild, adresse, meldender ->
                beiEinsatzAnlegen(stichwort, meldebild, adresse, meldender, null)
                neuOffen = false
            },
            beiSchliessen = { neuOffen = false },
        )
    }

    // Der Bogen folgt dem Raumzustand — gemerkt ist nur die Id, sonst zeigte er
    // den Stand vom Öffnen.
    bogenFuer?.let { id ->
        val einsatz = raum?.incidents?.firstOrNull { it.id == id }
        if (einsatz == null || raum == null) {
            bogenFuer = null
        } else {
            Einsatzblende(
                einsatz = einsatz,
                raum = raum,
                befehle = befehle,
                beiAlarmieren = {
                    bogenFuer = null
                    alarmFuer = einsatz
                },
                beiAbraeumen = {
                    beiSchliessen(einsatz.id)
                    bogenFuer = null
                },
                beiZu = { bogenFuer = null },
            )
        }
    }

    fahrzeugFuer?.let { id ->
        val fahrzeug = raum?.vehicles?.firstOrNull { it.id == id }
        if (fahrzeug == null || raum == null) {
            fahrzeugFuer = null
        } else {
            Fahrzeuggriffe(
                f = fahrzeug,
                raum = raum,
                eigeneKennung = eigeneKennung,
                befehle = befehle,
                beiSprechwunsch = beiSprechwunsch,
                beiZu = { fahrzeugFuer = null },
            )
        }
    }

    if (besatzungOffen && raum != null) {
        Besatzungsblende(
            raum = raum,
            eigeneKennung = eigeneKennung,
            katalog = katalog?.fahrzeuge.orEmpty(),
            befehle = befehle,
            beiZu = { besatzungOffen = false },
        )
    }

    if (warnungOffen) {
        Warnungsblende(befehle) { warnungOffen = false }
    }

    alarmFuer?.let { gemerkt ->
        // Wie beim Bogen: der frische Stand, damit eine nachgeschärfte Ordnung
        // sofort dasteht.
        val einsatz = raum?.incidents?.firstOrNull { it.id == gemerkt.id } ?: gemerkt
        Alarmblende(
            einsatz = einsatz,
            raum = raum,
            katalog = katalog,
            neben = neben,
            befehle = befehle,
            beiVorschlag = beiVorschlag,
            beiAlarmieren = { fahrzeuge, ab, zusatz, meldung ->
                beiAlarmieren(einsatz.id, fahrzeuge, ab, zusatz, meldung)
                alarmFuer = null
            },
            beiUmstufen = { p -> beiUmstufen(einsatz.id, p) },
            beiSchliessen = { alarmFuer = null },
        )
    }

    gespraech?.let { anrufId ->
        val anruf = raum?.anrufe?.firstOrNull { it.id == anrufId }
        // <b>Beendet heißt nicht vorbei.</b> Der Vorschlag entsteht erst beim
        // Auflegen — die Blende bleibt offen, bis er übernommen oder verworfen
        // ist. Wer sie beim Beenden schließt, wirft das Ergebnis des Gesprächs
        // weg, bevor es jemand gesehen hat.
        if (anruf == null || (anruf.zustand == "Beendet" && anruf.vorschlag == null) ||
            anruf.zustand == "Abgewiesen" || anruf.zustand == "Verpasst"
        ) {
            gespraech = null
        } else {
            Gespraechsblende(
                anruf = anruf,
                katalog = katalog,
                beiFrage = { beiAnrufFrage(anruf.id, it) },
                beiUebernehmen = { vorschlag ->
                    // Der Vorschlag wird der Einsatz — mit der Anruf-Id, damit
                    // der Server Gespräch und Lage verknüpft. Das Auflegen
                    // gehört dazu: Ein Anruf, dessen Einsatz läuft, ist fertig.
                    val stichwort = Stichwort(
                        stichwort = vorschlag.stichwort,
                        stichwortText = vorschlag.stichwortText,
                        organisation = vorschlag.organisation,
                        prioritaet = vorschlag.prioritaet,
                        empfohleneFahrzeuge = vorschlag.empfohleneFahrzeuge,
                        empfohleneFaehigkeiten = vorschlag.empfohleneFaehigkeiten,
                    )
                    beiEinsatzAnlegen(
                        stichwort,
                        vorschlag.meldebild,
                        vorschlag.adresse,
                        vorschlag.meldender,
                        anruf.id,
                    )
                    gespraech = null
                },
                beiAuflegen = {
                    // Auflegen schließt nicht — es holt den Vorschlag. Die
                    // Blende wechselt von selbst in die Vorschlagsansicht,
                    // sobald der Zustand umspringt.
                    beiAnrufBeenden(anruf.id)
                },
                beiVerwerfen = {
                    beiVorschlagVerwerfen(anruf.id)
                    gespraech = null
                },
                werkzeug = {
                    if (raum != null) {
                        Telefonwerkzeug(anruf, raum, neben, befehle) { gespraech = null }
                    }
                },
            )
        }
    }
}

private enum class Leitstellenteil(
    val titel: String,
    val zeichen: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Einsaetze("Einsätze", Zeichen.Lage),
    Karte("Karte", Zeichen.Karte),
    Fahrzeuge("Fahrzeuge", Zeichen.Fahrzeug),
    Notruf("Notruf", Zeichen.Notruf),
    Funk("Funk", Zeichen.Funk),
    Mehr("Mehr", Zeichen.LobbyMehr),
}

@Composable
private fun Leitstellenkopf(
    raum: Raumzustand?,
    oben: androidx.compose.ui.unit.Dp,
    beiVerlassen: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .kopfverlauf()
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(
                    color = Farben.Rand,
                    start = Offset(0f, size.height - strich / 2f),
                    end = Offset(size.width, size.height - strich / 2f),
                    strokeWidth = strich,
                )
            }
            .padding(top = oben)
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "Leitstelle",
                style = Schrift.Gross,
                color = Farben.Text,
            )
            SehrLeise(
                listOfNotNull(
                    raum?.settings?.leitstelle,
                    raum?.settings?.landkreis ?: raum?.settings?.ort,
                ).joinToString(" · ").ifBlank { "…" },
                mono = true,
            )
            if (raum != null) Lagepillen(raum, Modifier.padding(top = Abstand.Winzig))
        }

        Knopf("Verlassen", beiVerlassen, art = Knopfart.Gefahr, kompakt = true)
    }
}

/**
 * Die Lagezahlen — drei kleine Pillen statt einer Tafel (Web, 01.10.2026).
 *
 * Punkt · Zahl · Wort. Bei null sind sie grau und treten zurück; wartet etwas,
 * werden sie rot, und bei „unquittiert" pulsiert der Punkt. „frei" trägt einen
 * grünen Punkt und wird amber, wenn kein Fahrzeug mehr frei ist.
 */
@Composable
private fun Lagepillen(raum: Raumzustand, modifier: Modifier = Modifier) {
    val offen = raum.incidents.count { it.state == "Offen" }
    val unquittiert = raum.vehicles.count { it.alarmOffen }
    val fahrzeuge = raum.vehicles.size
    val frei = raum.vehicles.count { it.status in 1..2 }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = modifier,
    ) {
        Lagezahlpille(
            zahl = offen.toString(),
            wort = "offen",
            farbe = if (offen > 0) Farben.SignalHell else null,
        )
        Lagezahlpille(
            zahl = unquittiert.toString(),
            wort = "unquittiert",
            farbe = if (unquittiert > 0) Farben.SignalHell else null,
            puls = unquittiert > 0,
        )
        Lagezahlpille(
            zahl = "$frei/$fahrzeuge",
            wort = "frei",
            farbe = if (fahrzeuge > 0 && frei == 0) Farben.AmberHell else Farben.GruenHell,
            nurPunkt = fahrzeuge == 0 || frei > 0,
        )
    }
}

/**
 * Eine Lagezahlpille.
 *
 * @param farbe `null` = ruhig (grau). Sonst trägt die Pille die Farbe in Punkt,
 *   Zahl und Hauch.
 * @param nurPunkt Nur der Punkt farbig, die Pille bleibt ruhig — „frei" im
 *   Regelfall.
 */
@Composable
private fun Lagezahlpille(
    zahl: String,
    wort: String,
    farbe: Color?,
    puls: Boolean = false,
    nurPunkt: Boolean = false,
) {
    val laut = farbe != null && !nurPunkt
    val punktfarbe = farbe ?: Farben.TextSehrLeise
    val deckkraft = if (puls) {
        val takt = rememberInfiniteTransition(label = "lagepuls")
        val wert by takt.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "lagepuls-punkt",
        )
        wert
    } else {
        1f
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                if (laut) punktfarbe.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                Rundung.Rund,
            )
            .border(
                1.dp,
                if (laut) punktfarbe.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                Rundung.Rund,
            )
            .padding(horizontal = Abstand.Klein, vertical = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(deckkraft)
                .background(punktfarbe, CircleShape),
        )
        Text(
            text = zahl,
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
            color = if (laut) punktfarbe else Farben.TextLeise,
        )
        Text(
            text = wort,
            style = Schrift.Winzig,
            color = if (laut) punktfarbe else Farben.TextSehrLeise,
        )
    }
}

/** Teil 1 — die Einsatzliste, das Herz des Arbeitsplatzes. */
@Composable
private fun ColumnScope.TeilEinsaetze(
    raum: Raumzustand,
    beiNeu: () -> Unit,
    beiAlarm: (Einsatz) -> Unit,
    beiSchliessen: (String) -> Unit,
    beiWuerfeln: (() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Ueberschrift("Einsätze", Modifier.weight(1f))
        // Der Würfel gehört der freien Vergabe: Im Zufallsmodus nähme er der
        // Schicht das Warten, von dem sie lebt.
        if (beiWuerfeln != null) Knopf("Würfeln", beiWuerfeln, art = Knopfart.Leise, kompakt = true)
        Knopf("Neuer Einsatz", beiNeu, art = Knopfart.Haupt, kompakt = true)
    }

    // Die Laufenden zuerst, die Offenen darin ganz oben — abgeräumt wird unten.
    val (laufend, fertig) = raum.incidents.partition { !it.abgeschlossen }
    val sortiert = laufend.sortedWith(
        compareBy({ it.state != "Offen" }, { -it.prioritaet }),
    )

    if (raum.incidents.isEmpty()) {
        Leerhinweis(
            "Keine Lage. Der nächste Notruf kommt bestimmt — oder du legst selbst " +
                "einen Einsatz an.",
        )
    }

    sortiert.forEach { einsatz ->
        Einsatzzeile(einsatz, beiDruck = { beiAlarm(einsatz) })
    }

    if (fertig.isNotEmpty()) {
        Ueberschrift("Abgeschlossen")
        fertig.forEach { einsatz ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.weight(1f)) { Einsatzzeile(einsatz) }
                Knopf(
                    "Abräumen",
                    { beiSchliessen(einsatz.id) },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        }
    }
}

/** Teil 2 — das Tableau: jedes Fahrzeug, sein Status, seine Sprechwünsche. */
@Composable
private fun ColumnScope.TeilFahrzeuge(
    raum: Raumzustand,
    beiSprechwunsch: (String) -> Unit,
    beiFahrzeug: (Rundenfahrzeug) -> Unit = {},
) {
    val (wollen, still) = raum.vehicles.partition { it.sprechwunschSeit != null }

    if (wollen.isNotEmpty()) {
        Ueberschrift("Sprechwünsche")
        wollen.forEach { fahrzeug ->
            Dienstfahrzeugzeile(
                fahrzeug = fahrzeug,
                hinten = {
                    Knopf(
                        "Kommen",
                        { beiSprechwunsch(fahrzeug.id) },
                        art = Knopfart.Haupt,
                        kompakt = true,
                    )
                },
            )
        }
    }

    Ueberschrift("Tableau (${raum.vehicles.size})")
    if (raum.vehicles.isEmpty()) {
        Leerhinweis("Kein Fahrzeug im Dienst.")
    } else {
        SehrLeise("Antippen: anrufen, auf Streife schicken, Funkgruppe schalten.")
        raum.vehicles
            .sortedWith(compareBy({ it.status !in 1..2 }, { it.funkrufname }))
            .forEach { f -> Dienstfahrzeugzeile(f, beiDruck = { beiFahrzeug(f) }) }
    }
}

/** Teil 3 — der Notruf: was klingelt, was läuft, was verpasst wurde. */
@Composable
private fun ColumnScope.TeilNotruf(
    raum: Raumzustand,
    beiAnnehmen: (String) -> Unit,
    beiOeffnen: (String) -> Unit,
    beiAbweisen: (String) -> Unit,
) {
    Ueberschrift("Notruf")

    val aktive = raum.anrufe.filter { it.klingelt || it.imGespraech }

    if (aktive.isEmpty()) {
        Leerhinweis("Kein Anruf in der Leitung.")
    }

    aktive.forEach { anruf ->
        Kasten(marke = anruf.klingelt, wartet = anruf.klingelt, abstandInnen = Abstand.Klein) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Notruf ${anruf.nummer}",
                    style = Schrift.Gross,
                    color = Farben.Text,
                    modifier = Modifier.weight(1f),
                )
                Marke(
                    text = if (anruf.klingelt) "Klingelt" else "Im Gespräch",
                    farbe = if (anruf.klingelt) Farben.SignalHell else Farben.GruenHell,
                )
            }

            SehrLeise("eingegangen ${uhrzeit(anruf.eingangUm)}", mono = true)

            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                if (anruf.klingelt) {
                    Knopf(
                        "Annehmen",
                        { beiAnnehmen(anruf.id) },
                        art = Knopfart.Haupt,
                        kompakt = true,
                    )
                    Knopf(
                        "Abweisen",
                        { beiAbweisen(anruf.id) },
                        art = Knopfart.Gefahr,
                        kompakt = true,
                    )
                } else {
                    Knopf("Zum Gespräch", { beiOeffnen(anruf.id) }, kompakt = true)
                }
            }
        }
    }
}

/** Teil 4 — der Funk, mit wählbarem Ziel. */
@Composable
private fun ColumnScope.TeilLeitstellenfunk(
    stand: Rundenstand,
    raum: Raumzustand,
    beiFunk: (String, String?) -> Unit,
    beiSprechstart: () -> Unit,
    beiSprechende: () -> Unit,
    beiDraht: (String) -> Unit,
    neben: Raumneben = Raumneben(),
    befehle: Raumbefehle = Raumbefehle.Leer,
) {
    var ziel by remember { mutableStateOf<String?>(null) }
    var zielwahl by remember { mutableStateOf(false) }
    var leitung by remember { mutableStateOf("funk") }

    // Der Draht öffnet sich erst mit der zweiten Leitstelle — und fällt
    // zurück auf den Funk, wenn sie wieder geht.
    if (!raum.drahtOffen && leitung == "draht") leitung = "funk"

    if (raum.drahtOffen) {
        Reiterreihe {
            Reiter(
                "Funkverkehr",
                offen = leitung == "funk",
                beiDruck = { leitung = "funk" },
                modifier = Modifier.weight(1f),
            )
            Reiter(
                "Leitstellen",
                offen = leitung == "draht",
                beiDruck = { leitung = "draht" },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (leitung == "draht") {
        Drahtfaden(stand = stand, beiSenden = beiDraht)
        // Der Draht spricht auch — eine feste Leitung zwischen den Tischen.
        Sprechtaste(
            sendet = neben.sendetAuf == "draht",
            wirdVerstanden = false,
            belegtVon = neben.drahtSprecher,
            gesperrtBis = null,
            beiDruck = { befehle.drahtSprechenStarten() },
            beiLoslassen = { befehle.drahtSprechenBeenden() },
        )
        return
    }

    Ueberschrift("Funk")

    Kanalwahl(raum, befehle)

    Wahlfeld(
        etikett = "An",
        wert = ziel ?: "Alle Fahrzeuge",
        beiDruck = { zielwahl = true },
    )

    Funkprotokoll(
        zeilen = stand.funk,
        eigenerRufname = "Leitstelle",
        laeuft = stand.laeuft,
        beiSenden = { text -> beiFunk(text, ziel) },
    )

    stand.funkhinweis?.let { SehrLeise(it) }
    Sprechtaste(
        sendet = stand.sendet,
        wirdVerstanden = stand.wirdVerstanden,
        belegtVon = stand.sprecher.values.firstOrNull(),
        gesperrtBis = stand.funkGesperrtBis,
        beiDruck = beiSprechstart,
        beiLoslassen = beiSprechende,
    )

    if (zielwahl) {
        Wahlblende(
            titel = "Funkziel",
            gruppen = listOf(null to (listOf<String?>(null) + raum.vehicles.map { it.funkrufname })),
            aufschrift = { it ?: "Alle Fahrzeuge" },
            gewaehlt = ziel,
            beiWahl = {
                ziel = it
                zielwahl = false
            },
            beiSchliessen = { zielwahl = false },
            suchbar = raum.vehicles.size > 8,
        )
    }
}

@Composable
private fun ColumnScope.TeilMehrLeitstelle(
    raum: Raumzustand,
    beiDienstende: () -> Unit,
    beiBesatzung: () -> Unit = {},
    beiWarnung: () -> Unit = {},
    befehle: Raumbefehle = Raumbefehle.Leer,
) {
    // Wer mitten im Dienst um den Tisch oder einen Platz bittet, wartet auf den
    // Host — und der sitzt hier, nicht in der Lobby.
    Anfragenkasten(raum, befehle)

    Ueberschrift("Mannschaft")
    SehrLeise("Mitspieler werfen, die Leitstelle übergeben, Bot-Besatzungen einteilen.")
    Row { Knopf("Besatzungen einteilen", beiBesatzung) }

    Ueberschrift("Bevölkerung warnen")
    SehrLeise("Die Warn-App der Leitstelle — nur zum Spaß, folgenlos fürs Spiel.")
    Row { Knopf("Warnung senden", beiWarnung, art = Knopfart.Leise) }

    Ueberschrift("Dienstende")
    SehrLeise(
        "Das Dienstende ist eine Abstimmung — jeder stimmt mit. Danach steht die " +
            "Auswertung, und die Schicht wandert ins Dienstbuch.",
    )
    // Der Zwischenstand steht am Knopf: Ohne ihn drückte man, es geschähe nichts
    // Sichtbares, und man hielte es für einen Fehler.
    val stand = if (raum.dienstendeStimmen > 0 && raum.dienstendeSchwelle > 1) {
        " (${raum.dienstendeStimmen}/${raum.dienstendeSchwelle})"
    } else {
        ""
    }
    Row {
        Knopf(
            if (raum.dienstendeEigeneStimme) "Stimme zurücknehmen$stand" else "Dienst beenden$stand",
            beiDienstende,
            art = Knopfart.Gefahr,
        )
    }
}

/**
 * Einen Einsatz von Hand anlegen.
 *
 * <b>Das Stichwort ist die halbe Arbeit:</b> Es bringt Text, Priorität,
 * Empfehlung und die Meldebilder mit. Was bleibt, sind Meldebild und Adresse —
 * und die Adresse ist ein freies Feld, denn sie kommt vom Anrufer, nicht aus
 * einem Katalog.
 */
@Composable
private fun EinsatzAnlegen(
    katalog: Katalog,
    strassen: List<String> = emptyList(),
    beiAnlegen: (Stichwort, String, String, String?) -> Unit,
    beiSchliessen: () -> Unit,
) {
    var stichwort by remember { mutableStateOf<Stichwort?>(null) }
    var stichwortwahl by remember { mutableStateOf(false) }
    var meldebild by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }

    Blende(
        titel = "Neuer Einsatz",
        beiSchliessen = beiSchliessen,
        fuss = {
            Knopf(
                aufschrift = "Anlegen",
                beiDruck = {
                    stichwort?.let { beiAnlegen(it, meldebild, adresse, null) }
                },
                art = Knopfart.Haupt,
                aktiv = stichwort != null && adresse.isNotBlank(),
                breit = true,
            )
        },
    ) {
        Wahlfeld(
            etikett = "Stichwort",
            wert = stichwort?.let { "${it.stichwort} — ${it.stichwortText}" },
            beiDruck = { stichwortwahl = true },
        )

        stichwort?.let { s ->
            if (s.meldebilder.isNotEmpty()) {
                SehrLeise("Meldebild — was der Anrufer sieht:")
                Pillenreihe {
                    s.meldebilder.forEach { bild ->
                        Pille(bild, an = meldebild == bild, beiDruck = { meldebild = bild })
                    }
                }
            }
        }

        Feld(
            wert = adresse,
            beiAenderung = { adresse = it.take(120) },
            etikett = "Adresse",
            platzhalter = "Straße und Hausnummer",
        )

        // Das Ortsverzeichnis: echte Straßen des Kreises, sofern für ihn OSM-Daten
        // vorliegen (`NeuerEinsatzDialog.vue`). Ein Kreis bringt schnell dreihundert
        // mit — durch die scrollt in einer Alarmierung niemand, deshalb nur die
        // passenden zum Getippten und höchstens acht. Die Hausnummer würfelt die
        // Wahl dazu, wie im Web.
        if (strassen.isNotEmpty()) {
            val getippt = adresse.trim().lowercase()
            val passend = strassen
                .filter { getippt.isEmpty() || it.lowercase().contains(getippt) }
                .filter { it != adresse.trim() }
                .take(8)
            if (passend.isNotEmpty() && !strassen.any { adresse.startsWith("$it ") }) {
                SehrLeise(if (getippt.isEmpty()) "Straßen im Kreis — antippen übernimmt sie:" else "Passende Straßen:")
                Pillenreihe {
                    passend.forEach { s ->
                        Pille(s, an = false, beiDruck = { adresse = "$s ${(1..119).random()}" })
                    }
                }
            }
            Knopf("Würfeln", {
                strassen.randomOrNull()?.let { adresse = "$it ${(1..119).random()}" }
            }, art = Knopfart.Leise, kompakt = true)
        }

        stichwort?.let { s ->
            SehrLeise(
                "Priorität ${s.prioritaet} · empfohlen: ${s.empfohleneFahrzeuge} Fahrzeuge" +
                    (s.empfohleneFaehigkeiten.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ", prefix = " (", postfix = ")") ?: ""),
            )
        }
    }

    if (stichwortwahl) {
        // Nach Organisation gruppiert — dieselbe Ordnung wie im Web: Wer „B2"
        // sucht, sucht bei der Feuerwehr, nicht im Alphabet.
        val gruppen = katalog.stichworte
            .groupBy { it.organisation }
            .map { (org, liste) -> org to liste.sortedBy { it.stichwort } }

        Wahlblende(
            titel = "Stichwort",
            gruppen = gruppen,
            aufschrift = { "${it.stichwort} — ${it.stichwortText}" },
            unterschrift = { "Priorität ${it.prioritaet} · ${it.empfohleneFahrzeuge} Fahrzeuge" },
            gewaehlt = stichwort,
            beiWahl = {
                stichwort = it
                meldebild = it.meldebilder.firstOrNull().orEmpty()
                stichwortwahl = false
            },
            beiSchliessen = { stichwortwahl = false },
            suchbar = true,
        )
    }
}

/**
 * Der Alarmdialog — welche Fahrzeuge zu dieser Lage fahren.
 *
 * <b>Der Vorschlag des Servers füllt die Vorauswahl</b>, entschieden wird hier:
 * Jede Zeile ist ein Haken, die Verfügbaren stehen oben. Die Pflichtangabe —
 * wie viele gewählt sind — steht am Fuß neben dem Knopf, nicht im rollenden
 * Inhalt: Was dort hinten steht, fällt bei knapper Höhe heraus.
 *
 * <b>Darüber die Ordnung:</b> Wie viele Fahrzeuge die Lage fordert, lässt sich
 * nachschärfen, aus einer Vorlage übernehmen und als Vorlage sichern — dort, wo
 * sie entsteht, vor dem Tableau. Darunter die eigene Meldung der Leitstelle,
 * getippt oder gesprochen, und für Wechsellader der Behälter, mit dem sie fahren.
 */
@Composable
private fun Alarmblende(
    einsatz: Einsatz,
    raum: Raumzustand?,
    katalog: Katalog?,
    neben: Raumneben,
    befehle: Raumbefehle,
    beiVorschlag: suspend (String) -> List<String>,
    beiAlarmieren: (List<String>, Map<String, String>, String?, String?) -> Unit,
    beiUmstufen: (Int) -> Unit,
    beiSchliessen: () -> Unit,
) {
    var gewaehlt by remember { mutableStateOf<Set<String>>(emptySet()) }
    var vorschlagGeladen by remember { mutableStateOf(false) }
    var nachladen by remember { mutableIntStateOf(0) }
    var zusatztext by remember { mutableStateOf("") }
    var abrollbehaelter by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var abWahlFuer by remember { mutableStateOf<Rundenfahrzeug?>(null) }
    var vorlageWahl by remember { mutableStateOf(false) }
    var vorlageName by remember(einsatz.id) { mutableStateOf(einsatz.stichwort) }
    var meldungHat by remember { mutableStateOf(false) }
    val meldungId = remember(einsatz.id) { java.util.UUID.randomUUID().toString() }
    val bereich = rememberCoroutineScope()

    // Der Vorschlag kommt beim Öffnen und nach jeder geänderten Ordnung — und nur in
    // die Vorauswahl: Was der Mensch danach ändert, bleibt geändert.
    LaunchedEffect(einsatz.id, nachladen) {
        gewaehlt = beiVorschlag(einsatz.id).toSet()
        vorschlagGeladen = true
    }
    LaunchedEffect(Unit) { befehle.dauerVorlagenLaden(raum?.settings?.landkreisId) }

    val fahrzeuge = raum?.vehicles.orEmpty()
        .filter { it.einsatzId == null || it.einsatzId == einsatz.id }
        .sortedWith(compareBy({ it.status !in 1..2 }, { it.funkrufname }))
    val behaelter = katalog?.fahrzeuge.orEmpty().filter { it.kategorie == "Abrollbehälter" }

    Blende(
        titel = "${einsatz.stichwort} alarmieren",
        beiSchliessen = {
            if (meldungHat) befehle.alarmMeldungVerwerfen(meldungId)
            beiSchliessen()
        },
        breite = Dialogbreite.Normal,
        fuss = {
            SehrLeise("${gewaehlt.size} gewählt · empfohlen ${einsatz.empfohleneFahrzeuge}")
            Knopf(
                aufschrift = "Alarmieren",
                beiDruck = {
                    beiAlarmieren(
                        gewaehlt.toList(),
                        abrollbehaelter.filterKeys { it in gewaehlt },
                        zusatztext,
                        meldungId.takeIf { meldungHat },
                    )
                },
                art = Knopfart.Alarm,
                aktiv = gewaehlt.isNotEmpty(),
            )
        },
    ) {
        Text(
            text = listOfNotNull(einsatz.stichwortText, einsatz.adresse.ifBlank { null })
                .joinToString(" · "),
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )

        if (einsatz.empfohleneFaehigkeiten.isNotEmpty()) {
            SehrLeise("Gebraucht: ${einsatz.empfohleneFaehigkeiten.joinToString(", ")}")
        }

        // Die Umstufung — eine geänderte Dringlichkeit geht sofort auf den
        // Kanal, die alarmierten Kräfte lesen sie mit.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SehrLeise("Priorität:")
            (1..3).forEach { p ->
                Pille(
                    aufschrift = p.toString(),
                    an = einsatz.prioritaet == p,
                    beiDruck = { beiUmstufen(p) },
                )
            }
        }

        // Die Ordnung — Regler in Schritten, Vorlage, Sichern.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SehrLeise("Ordnung: ${einsatz.empfohleneFahrzeuge} Fahrzeuge", modifier = Modifier.weight(1f))
            Knopf("−", {
                befehle.ordnungAendern(einsatz.id, (einsatz.empfohleneFahrzeuge - 1).coerceAtLeast(1), einsatz.empfohleneFaehigkeiten)
                nachladen++
            }, art = Knopfart.Leise, kompakt = true)
            Knopf("+", {
                befehle.ordnungAendern(einsatz.id, (einsatz.empfohleneFahrzeuge + 1).coerceAtMost(12), einsatz.empfohleneFaehigkeiten)
                nachladen++
            }, art = Knopfart.Leise, kompakt = true)
            Knopf("Vorlage", { vorlageWahl = true }, art = Knopfart.Leise, kompakt = true)
        }

        if (!vorschlagGeladen) Ladezeile("Der Vorschlag wird geholt …")

        fahrzeuge.forEach { fahrzeug ->
            val an = fahrzeug.id in gewaehlt
            val verfuegbar = fahrzeug.status in 1..2 && fahrzeug.einsatzId == null

            // Fahrzeugkarten wie im Tableau (Web, 01.10.2026): Statusquadrat,
            // Leuchtleiste, runder Haken, amberner Rand bei Auswahl. Nicht
            // Verfügbares steht gedämpft, bleibt aber wählbar — nachalarmieren
            // ist eine Entscheidung der Leitstelle, nicht des Dialogs.
            Box(modifier = Modifier.alpha(if (verfuegbar || an) 1f else 0.6f)) {
                Dienstfahrzeugzeile(
                    fahrzeug = fahrzeug,
                    gewaehlt = an,
                    beiDruck = {
                        gewaehlt = if (an) gewaehlt - fahrzeug.id else gewaehlt + fahrzeug.id
                    },
                    hinten = { RunderHaken(an) },
                )
            }

            // Ein Wechsellader fährt mit dem Behälter, der hier gewählt ist — sonst
            // mit dem, der gerade aufsitzt.
            if (an && fahrzeug.templateId == "wlfab" && behaelter.isNotEmpty()) {
                val gewaehlterAb = abrollbehaelter[fahrzeug.id] ?: fahrzeug.abrollbehaelterTemplateId
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SehrLeise("Behälter: ${abBehaelterName(gewaehlterAb, behaelter)}", modifier = Modifier.weight(1f))
                    Knopf("Wählen", { abWahlFuer = fahrzeug }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }

        if (fahrzeuge.isEmpty()) {
            Leerhinweis("Kein Fahrzeug frei — alles ist gebunden.")
        }

        // Die eigene Meldung — sie steht auf dem Melder unter der Automatik.
        Feld(
            wert = zusatztext,
            beiAenderung = { zusatztext = it.take(200) },
            etikett = "Eigene Meldung (optional)",
            platzhalter = "z. B. Zufahrt über den Hof",
        )
        Sprechtaste(
            sendet = neben.sendetAuf == "meldung",
            wirdVerstanden = false,
            belegtVon = null,
            gesperrtBis = null,
            beiDruck = {
                if (meldungHat) befehle.alarmMeldungVerwerfen(meldungId)
                meldungHat = false
                befehle.alarmMeldungSprechenStarten(meldungId)
            },
            beiLoslassen = {
                bereich.launch { meldungHat = befehle.alarmMeldungSprechenBeenden(meldungId) }
            },
        )
        if (meldungHat) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SehrLeise("Gesprochene Meldung liegt bereit — sie geht mit dem Alarm hinaus.", modifier = Modifier.weight(1f))
                Knopf("Verwerfen", {
                    befehle.alarmMeldungVerwerfen(meldungId)
                    meldungHat = false
                }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }

    abWahlFuer?.let { f ->
        val belegt = raum?.vehicles.orEmpty()
            .filter { it.id != f.id }
            .mapNotNull { it.abrollbehaelterTemplateId }
            .toSet() + abrollbehaelter.filterKeys { it != f.id }.values
        Wahlblende(
            titel = "Behälter für ${f.funkrufname}",
            gruppen = listOf(null to behaelter.filter { it.id !in belegt }),
            aufschrift = { it.typ.removePrefix("WLF + ") },
            unterschrift = { it.faehigkeiten.joinToString(", ").ifBlank { null } },
            beiWahl = {
                abrollbehaelter = abrollbehaelter + (f.id to it.id)
                abWahlFuer = null
            },
            beiSchliessen = { abWahlFuer = null },
        )
    }

    if (vorlageWahl) {
        Aaoblende(
            einsatz = einsatz,
            raum = raum,
            neben = neben,
            befehle = befehle,
            name = vorlageName,
            beiName = { vorlageName = it },
            beiUebernommen = { nachladen++ },
            beiZu = { vorlageWahl = false },
        )
    }
}

/**
 * Die Alarm- und Ausrückeordnung als Vorlage — übernehmen oder sichern.
 *
 * <b>Zwei Ablagen in einer Liste.</b> Die der Schicht sehen alle Disponenten am Tisch
 * und sie endet mit ihr; die dauerhaften gehören dem Konto. Wer eine dauerhafte
 * übernimmt, schiebt sie zugleich in die Schicht — dann hat sie der zweite
 * Disponent auch.
 */
@Composable
private fun Aaoblende(
    einsatz: Einsatz,
    raum: Raumzustand?,
    neben: Raumneben,
    befehle: Raumbefehle,
    name: String,
    beiName: (String) -> Unit,
    beiUebernommen: () -> Unit,
    beiZu: () -> Unit,
) {
    val schicht = raum?.aaoVorlagen.orEmpty()

    Blende(titel = "Alarm- und Ausrückeordnung", beiSchliessen = beiZu) {
        SehrLeise(
            "Jetzt: ${einsatz.empfohleneFahrzeuge} Fahrzeuge" +
                einsatz.empfohleneFaehigkeiten.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ", prefix = " · ").orEmpty(),
        )

        if (schicht.isNotEmpty()) {
            Ueberschrift("Vorlagen dieser Schicht")
            schicht.forEach { v ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(v.name, style = Schrift.Normal, color = Farben.Text)
                        SehrLeise("${v.empfohleneFahrzeuge} Fahrzeuge · ${v.empfohleneFaehigkeiten.joinToString(", ")}")
                    }
                    Knopf("Übernehmen", {
                        befehle.ordnungAendern(einsatz.id, v.empfohleneFahrzeuge, v.empfohleneFaehigkeiten)
                        beiUebernommen()
                        beiZu()
                    }, kompakt = true)
                    Knopf("×", { befehle.aaoVorlageLoeschen(v.name) }, art = Knopfart.Gefahr, kompakt = true)
                }
            }
        }

        if (neben.dauerVorlagen.isNotEmpty()) {
            Ueberschrift("Gespeichert am Konto")
            neben.dauerVorlagen.forEach { v ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(v.name, style = Schrift.Normal, color = Farben.Text)
                        SehrLeise(
                            "${v.empfohleneFahrzeuge} Fahrzeuge · " +
                                (if (v.landkreisId == null) "alle Runden" else "dieser Kreis"),
                        )
                    }
                    Knopf("Übernehmen", {
                        befehle.ordnungAendern(einsatz.id, v.empfohleneFahrzeuge, v.empfohleneFaehigkeiten)
                        befehle.aaoVorlageSpeichern(v.name, v.empfohleneFahrzeuge, v.empfohleneFaehigkeiten)
                        beiUebernommen()
                        beiZu()
                    }, kompakt = true)
                    Knopf("×", { befehle.dauerVorlageLoeschen(v.id) }, art = Knopfart.Gefahr, kompakt = true)
                }
            }
        }

        if (schicht.isEmpty() && neben.dauerVorlagen.isEmpty()) {
            SehrLeise("Noch keine Vorlage — sichere die jetzige Ordnung unter einem Namen.")
        }

        Ueberschrift("Jetzige Ordnung sichern")
        Feld(wert = name, beiAenderung = { beiName(it.take(60)) }, etikett = "Name")
        Pillenreihe {
            Pille("Für diese Schicht", an = false, aktiv = name.isNotBlank(), beiDruck = {
                befehle.aaoVorlageSpeichern(name, einsatz.empfohleneFahrzeuge, einsatz.empfohleneFaehigkeiten)
            })
            Pille("Am Konto · alle Runden", an = false, aktiv = name.isNotBlank(), beiDruck = {
                befehle.dauerVorlageSichern(name, einsatz.empfohleneFahrzeuge, einsatz.empfohleneFaehigkeiten, null)
            })
            raum?.settings?.landkreisId?.let { kreis ->
                Pille("Am Konto · dieser Kreis", an = false, aktiv = name.isNotBlank(), beiDruck = {
                    befehle.dauerVorlageSichern(name, einsatz.empfohleneFahrzeuge, einsatz.empfohleneFaehigkeiten, kreis)
                })
            }
        }
        neben.vorlageMeldung?.let { SehrLeise(it, mono = true) }
    }
}

/**
 * Das Notrufgespräch.
 *
 * <b>Das Gespräch ist das Rätsel:</b> Der Anrufer weiß etwas, die sechs Fragen
 * holen es heraus, und aus dem Erfragten baut der Server den Vorschlag — nur
 * daraus. Die Güte daneben sagt, wie vollständig gefragt wurde; wer bei 40 %
 * alarmiert, alarmiert ins Blaue.
 */
@Composable
private fun Gespraechsblende(
    anruf: Anruf,
    katalog: Katalog?,
    beiFrage: (String) -> Unit,
    beiUebernehmen: (de.pagerspass.pagerspass.netz.Notrufvorschlag) -> Unit,
    beiAuflegen: () -> Unit,
    beiVerwerfen: () -> Unit,
    werkzeug: @Composable ColumnScope.() -> Unit = {},
) {
    val beendet = anruf.zustand == "Beendet"

    Blende(
        titel = "Notruf ${anruf.nummer}",
        beiSchliessen = if (beendet) beiVerwerfen else beiAuflegen,
        breite = Dialogbreite.Normal,
        kopfknoepfe = {
            if (!beendet) {
                Knopf("Auflegen", beiAuflegen, art = Knopfart.Gefahr, kompakt = true)
            }
        },
    ) {
        // Der Verlauf — die Leitstelle rechts wäre am Handy verschenkter Platz;
        // wer spricht, steht vorn an der Zeile.
        if (anruf.verlauf.isEmpty()) {
            SehrLeise("Das Gespräch beginnt — stell die erste Frage.")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
            ) {
                anruf.verlauf.forEach { zeile ->
                    Text(
                        text = (if (zeile.vonLeitstelle) "Du: " else "Anrufer: ") + zeile.text,
                        style = Schrift.Klein,
                        color = when {
                            !zeile.verstanden -> Farben.TextSehrLeise
                            zeile.vonLeitstelle -> Farben.AmberHell
                            else -> Farben.Text
                        },
                    )
                }
            }
        }

        // Der Eingabeweg ist eine Geräteeinstellung (Konto → Deine Bedienung):
        // Fragen anklicken, selbst tippen oder sprechen — wie im Web.
        val eingabeweg by de.pagerspass.pagerspass.mobil.eingabewegState()
        if (!beendet && eingabeweg == "fragen") {
            Ueberschrift("Fragen")
            Pillenreihe {
                FAKTENARTEN.forEach { (art, frage) ->
                    Pille(
                        aufschrift = frage,
                        an = art in anruf.erfragt,
                        beiDruck = { beiFrage(art) },
                        aktiv = art !in anruf.erfragt,
                    )
                }
            }
        }

        werkzeug()

        anruf.vorschlag?.let { v ->
            Ueberschrift("Vorschlag")
            Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${v.stichwort} — ${v.stichwortText}",
                        style = Schrift.Normal,
                        color = Farben.Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Marke("${(v.guete * 100).toInt()} %", farbe = Farben.AmberHell)
                }
                SehrLeise(
                    listOfNotNull(v.adresse.ifBlank { null }, v.meldebild.ifBlank { null })
                        .joinToString(" · "),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        "Einsatz anlegen",
                        { beiUebernehmen(v) },
                        art = Knopfart.Haupt,
                        kompakt = true,
                        aktiv = v.adresse.isNotBlank(),
                    )
                    Knopf("Verwerfen", beiVerwerfen, art = Knopfart.Leise, kompakt = true)
                }
                if (v.adresse.isBlank()) {
                    SehrLeise(
                        "Ohne Ort fährt niemand — der Anrufer wurde nie gefragt, wo es " +
                            "brennt.",
                    )
                }
            }
        }
    }
}
