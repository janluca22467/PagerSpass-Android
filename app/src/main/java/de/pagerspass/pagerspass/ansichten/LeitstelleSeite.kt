package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Teil
import de.pagerspass.pagerspass.ui.bausteine.Teilleiste
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.raster
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
    beiAlarmieren: (String, List<String>) -> Unit = { _, _ -> },
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
) {
    var reiter by remember { mutableStateOf(Leitstellenteil.Einsaetze) }
    var kartenwahl by remember { mutableStateOf<String?>(null) }
    var neuOffen by remember { mutableStateOf(false) }
    var alarmFuer by remember { mutableStateOf<Einsatz?>(null) }
    var gespraech by remember { mutableStateOf<String?>(null) }

    val raum = stand.raum
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val klingelnde = raum?.anrufe?.count { it.klingelt } ?: 0
    val sprechwuensche = raum?.vehicles?.count { it.sprechwunschSeit != null } ?: 0
    val offene = raum?.incidents?.count { !it.abgeschlossen } ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(Brush.verticalGradient(listOf(Farben.Bg, Farben.BgTief))) }
            .raster(),
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
                        Leitstellenteil.Einsaetze -> TeilEinsaetze(
                            raum = raum,
                            beiNeu = { neuOffen = true },
                            beiAlarm = { alarmFuer = it },
                            beiSchliessen = beiSchliessen,
                        )

                        // Die Karte wird oben ohne Rollspalte gezeichnet.
                        Leitstellenteil.Karte -> Unit

                        Leitstellenteil.Fahrzeuge -> TeilFahrzeuge(raum, beiSprechwunsch)

                        Leitstellenteil.Notruf -> TeilNotruf(
                            raum = raum,
                            beiAnnehmen = { id ->
                                beiAnrufAnnehmen(id)
                                gespraech = id
                            },
                            beiOeffnen = { gespraech = it },
                            beiAbweisen = beiAnrufAbweisen,
                        )

                        Leitstellenteil.Funk -> TeilLeitstellenfunk(
                            stand, raum, beiFunk, beiSprechstart, beiSprechende, beiDraht,
                        )

                        Leitstellenteil.Mehr -> TeilMehrLeitstelle(beiDienstende)
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
            ),
            // Ein klingelnder Notruf ruft — er ist das Dringendste, was eine
            // Leitstelle haben kann, und er wartet nicht.
            ruft = if (klingelnde > 0) setOf(Leitstellenteil.Notruf.name) else emptySet(),
            beiWahl = { id -> reiter = Leitstellenteil.valueOf(id) },
        )
    }

    if (neuOffen && katalog != null) {
        EinsatzAnlegen(
            katalog = katalog,
            beiAnlegen = { stichwort, meldebild, adresse, meldender ->
                beiEinsatzAnlegen(stichwort, meldebild, adresse, meldender, null)
                neuOffen = false
            },
            beiSchliessen = { neuOffen = false },
        )
    }

    alarmFuer?.let { einsatz ->
        Alarmblende(
            einsatz = einsatz,
            raum = raum,
            beiVorschlag = beiVorschlag,
            beiAlarmieren = { fahrzeuge ->
                beiAlarmieren(einsatz.id, fahrzeuge)
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
            .background(Brush.verticalGradient(listOf(Farben.FlaecheHoch, Farben.Flaeche)))
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
        }

        Knopf("Verlassen", beiVerlassen, art = Knopfart.Gefahr, kompakt = true)
    }
}

/** Teil 1 — die Einsatzliste, das Herz des Arbeitsplatzes. */
@Composable
private fun ColumnScope.TeilEinsaetze(
    raum: Raumzustand,
    beiNeu: () -> Unit,
    beiAlarm: (Einsatz) -> Unit,
    beiSchliessen: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Ueberschrift("Einsätze", Modifier.weight(1f))
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
private fun ColumnScope.TeilFahrzeuge(raum: Raumzustand, beiSprechwunsch: (String) -> Unit) {
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
        raum.vehicles
            .sortedWith(compareBy({ it.status !in 1..2 }, { it.funkrufname }))
            .forEach { Dienstfahrzeugzeile(it) }
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
        return
    }

    Ueberschrift("Funk")

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
private fun ColumnScope.TeilMehrLeitstelle(beiDienstende: () -> Unit) {
    Ueberschrift("Dienstende")
    SehrLeise(
        "Das Dienstende ist eine Abstimmung — jeder stimmt mit. Danach steht die " +
            "Auswertung, und die Schicht wandert ins Dienstbuch.",
    )
    Row {
        Knopf("Dienst beenden", beiDienstende, art = Knopfart.Gefahr)
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
 */
@Composable
private fun Alarmblende(
    einsatz: Einsatz,
    raum: Raumzustand?,
    beiVorschlag: suspend (String) -> List<String>,
    beiAlarmieren: (List<String>) -> Unit,
    beiUmstufen: (Int) -> Unit,
    beiSchliessen: () -> Unit,
) {
    var gewaehlt by remember { mutableStateOf<Set<String>>(emptySet()) }
    var vorschlagGeladen by remember { mutableStateOf(false) }

    // Der Vorschlag kommt einmal, beim Öffnen — und nur in die Vorauswahl:
    // Was der Mensch danach ändert, bleibt geändert.
    LaunchedEffect(einsatz.id) {
        if (!vorschlagGeladen) {
            gewaehlt = beiVorschlag(einsatz.id).toSet()
            vorschlagGeladen = true
        }
    }

    val fahrzeuge = raum?.vehicles.orEmpty()
        .filter { it.einsatzId == null || it.einsatzId == einsatz.id }
        .sortedWith(compareBy({ it.status !in 1..2 }, { it.funkrufname }))

    Blende(
        titel = "${einsatz.stichwort} alarmieren",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Normal,
        fuss = {
            SehrLeise("${gewaehlt.size} gewählt · empfohlen ${einsatz.empfohleneFahrzeuge}")
            Knopf(
                aufschrift = "Alarmieren",
                beiDruck = { beiAlarmieren(gewaehlt.toList()) },
                art = Knopfart.Alarm,
                aktiv = gewaehlt.isNotEmpty(),
            )
        },
    ) {
        Text(
            text = listOfNotNull(einsatz.stichwortText, einsatz.adresse.ifBlank { null })
                .joinToString (" · "),
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

        if (!vorschlagGeladen) Ladezeile("Der Vorschlag wird geholt …")

        fahrzeuge.forEach { fahrzeug ->
            val an = fahrzeug.id in gewaehlt
            val verfuegbar = fahrzeug.status in 1..2 && fahrzeug.einsatzId == null

            Hakenzeile(
                text = "${fahrzeug.funkrufname} · ${fahrzeug.typ}" +
                    if (!verfuegbar) " — ${fahrzeug.statusText}" else "",
                an = an,
                beiWechsel = { neu ->
                    gewaehlt = if (neu) gewaehlt + fahrzeug.id else gewaehlt - fahrzeug.id
                },
            )
        }

        if (fahrzeuge.isEmpty()) {
            Leerhinweis("Kein Fahrzeug frei — alles ist gebunden.")
        }
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

        if (!beendet) {
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
