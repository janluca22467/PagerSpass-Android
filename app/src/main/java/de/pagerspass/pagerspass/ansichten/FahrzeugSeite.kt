package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.FmsTaste
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kapselreihe
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Teil
import de.pagerspass.pagerspass.ui.bausteine.Teilleiste
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import de.pagerspass.pagerspass.ui.theme.kopfverlauf
import de.pagerspass.pagerspass.ui.theme.leuchtleiste
import de.pagerspass.pagerspass.ui.theme.seitengrund
import de.pagerspass.pagerspass.ui.theme.statusquadrat
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Das Fahrzeug — der Dienst aus Sicht der Besatzung.
 *
 * Übertragen aus `web/src/views/FahrzeugView.vue` in die Handyform: fester Kopf
 * mit Rufname und Status, ein Teil in der Mitte, Reiter unten — Einsatz ·
 * Status · Funk · Mehr. Dieselbe Bauform wie die Lobby.
 *
 * <b>Der Ablauf einer Fahrt ist FMS:</b> Der Melder weckt (Alarm quittieren),
 * Status 3 rückt aus, Status 4 meldet die Ankunft, die Lagemeldung sagt der
 * Leitstelle, was Sache ist, Status 1 macht wieder frei. Die Engine fährt das
 * Fahrzeug; die Besatzung meldet — genau wie im echten Funk.
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
) {
    var reiter by remember { mutableStateOf(Fahrzeugteil.Einsatz) }

    val raum = stand.raum
    val meins = raum?.vehicles?.firstOrNull { it.playerId == eigeneKennung }
    val einsatz = raum?.incidents?.firstOrNull { it.id == meins?.einsatzId }
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .seitengrund(),
    ) {
        Fahrzeugkopf(meins, oben, beiVerlassen)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (raum == null || meins == null) {
                Ladezeile("Der Dienst wird geladen …", Modifier.padding(Abstand.Gross))
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
                        Fahrzeugteil.Einsatz -> {
                            // Die Navigation — 220 pt wie im Web. Ohne Auftrag
                            // und auf Anfahrt steht sie oben, sonst unter dem
                            // Einsatz (`karteOben` in FahrzeugView.vue).
                            val karteOben = einsatz == null || meins.status == 3
                            if (karteOben) {
                                de.pagerspass.pagerspass.ui.karte.Lagekarte(
                                    raum = raum,
                                    eigenesFahrzeugId = meins.id,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                )
                            }
                            TeilEinsatz(
                                einsatz = einsatz,
                                meins = meins,
                                katalog = katalog,
                                beiLagemeldung = beiLagemeldung,
                                beiFms = beiFms,
                            )
                            ManvBereich(
                                einsatz = einsatz,
                                raum = raum,
                                meins = meins,
                                griffe = manv,
                            )
                            if (!karteOben) {
                                de.pagerspass.pagerspass.ui.karte.Lagekarte(
                                    raum = raum,
                                    eigenesFahrzeugId = meins.id,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                )
                            }
                        }

                        Fahrzeugteil.Status -> TeilStatus(
                            meins = meins,
                            tasten = katalog?.fmsStatus.orEmpty(),
                            beiFms = beiFms,
                            beiSondersignal = beiSondersignal,
                        )

                        Fahrzeugteil.Funk -> {
                            // Der Einsatzstellenfunk schaltet mit Status 4 an
                            // der Lage frei — vorher steht der Reiter gesperrt
                            // da und sagt warum (dieselbe Regel wie im Web).
                            val stelleFrei = meins.status == 4 &&
                                meins.einsatzstelleErreicht &&
                                meins.einsatzId != null
                            var leitung by remember { mutableStateOf("funk") }
                            if (!stelleFrei && leitung == "einsatzstelle") leitung = "funk"

                            // Funkverkehr/Einsatzstelle als Kapselreihe, wie
                            // im Web seit dem 01.10.2026.
                            Kapselreihe(
                                seiten = listOf("funk", "einsatzstelle"),
                                gewaehlt = leitung,
                                beiWahl = { leitung = it },
                                aufschrift = {
                                    when {
                                        it == "funk" -> "Funkverkehr"
                                        stelleFrei -> "Einsatzstelle"
                                        else -> "Einsatzstelle · S4"
                                    }
                                },
                                gesperrt = { it == "einsatzstelle" && !stelleFrei },
                            )

                            if (leitung == "einsatzstelle") {
                                Einsatzstellenfaden(
                                    zeilen = raum.einsatzstellenchat,
                                    beiSenden = beiEinsatzstelle,
                                )
                            } else {
                                Funkprotokoll(
                                    zeilen = stand.funk,
                                    eigenerRufname = meins.funkrufname,
                                    laeuft = stand.laeuft,
                                    beiSenden = { beiFunk(it) },
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
                            }
                        }

                        Fahrzeugteil.Mehr -> TeilMehrFahrzeug(beiDienstende)
                    }
                }
            }
        }

        Teilleiste(
            teile = Fahrzeugteil.entries.map { Teil(it.name, it.titel, it.zeichen) },
            offen = reiter.name,
            // Ein offener Alarm ruft nach dem Einsatz-Teil — dort steht, wohin
            // es geht.
            ruft = if (meins?.alarmOffen == true) setOf(Fahrzeugteil.Einsatz.name) else emptySet(),
            beiWahl = { id -> reiter = Fahrzeugteil.valueOf(id) },
        )
    }
}

private enum class Fahrzeugteil(
    val titel: String,
    val zeichen: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Einsatz("Einsatz", Zeichen.Lage),
    Status("Status", Zeichen.Melder),
    Funk("Funk", Zeichen.Funk),
    Mehr("Mehr", Zeichen.LobbyMehr),
}

/**
 * Der Kopf — Rufname, Status, Verlassen.
 *
 * Der Statuspunkt trägt die FMS-Farbe: Wer aufs Gerät sieht, weiß in einer
 * Sekunde, ob er frei, unterwegs oder gebunden ist.
 */
@Composable
private fun Fahrzeugkopf(
    meins: Rundenfahrzeug?,
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
        if (meins != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .statusquadrat(fmsFarbe(meins.status), ecke = 12.dp),
            ) {
                Text(
                    text = meins.status.toString(),
                    style = Schrift.MarkeZahl,
                    color = Farben.AufFarbe,
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = meins?.funkrufname ?: "—",
                // Eine Stufe kleiner als früher (Web, „Handy Runde 2"): Lange
                // Rufnamen brachen am Handy um.
                style = Schrift.MonoNormal,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SehrLeise(meins?.statusText.orEmpty().ifBlank { "…" }, mono = true)
        }

        Knopf("Verlassen", beiVerlassen, art = Knopfart.Gefahr, kompakt = true)
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

/**
 * Der Einsatz-Teil: wohin es geht und was zu melden ist.
 *
 * <b>Die Lagemeldungen kommen aus dem Stichwort.</b> Niemand tippt „Feuer unter
 * Kontrolle" auf einer Handytastatur, wenn der Satz einen Fingertipp entfernt
 * steht — der Katalog führt zu jedem Stichwort die Sätze, die zu dieser Lage
 * passen. Das freie Feld bleibt daneben: Es gibt immer eine Lage, die der
 * Katalog nicht kennt.
 */
@Composable
private fun ColumnScope.TeilEinsatz(
    einsatz: Einsatz?,
    meins: Rundenfahrzeug,
    katalog: Katalog?,
    beiLagemeldung: (String) -> Unit,
    beiFms: (Int, String?, Int?) -> Unit,
) {
    if (einsatz == null) {
        Leerhinweis(
            "Kein Einsatz. Du bist frei auf Wache — der Melder weckt dich, wenn es " +
                "so weit ist.",
        )
        return
    }

    // Die Einsatzkarte mit Dringlichkeitsschein (Web, 01.10.2026): Die
    // Leuchtleiste trägt die Priorität, ein dringender Einsatz glimmt.
    val dringlichkeit = when {
        einsatz.abgeschlossen -> einsatzFarbe(einsatz.state)
        einsatz.prioritaet >= 3 -> Farben.Signal
        einsatz.prioritaet == 2 -> Farben.Amber
        else -> Farben.FmsFrei
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = dringlichkeit.copy(alpha = 0.35f))
            .leuchtleiste(
                dringlichkeit,
                schein = 0.6f,
                glimmt = einsatz.prioritaet >= 3 && !einsatz.abgeschlossen,
            )
            .padding(start = Abstand.Gross + 3.dp, end = Abstand.Gross, top = Abstand.Gross, bottom = Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = einsatz.stichwort,
                style = Schrift.Anzeige.copy(fontSize = Schrift.SCHLAGZEILE),
                color = Farben.Text,
                modifier = Modifier.weight(1f),
            )
            Marke(einsatzZustand(einsatz.state), farbe = einsatzFarbe(einsatz.state))
        }

        Text(einsatz.stichwortText, style = Schrift.Normal, color = Farben.TextLeise)
        if (einsatz.meldebild.isNotBlank()) {
            Text(einsatz.meldebild, style = Schrift.Klein, color = Farben.TextLeise)
        }

        Text(
            text = listOfNotNull(einsatz.adresse.ifBlank { null }, einsatz.ortsteil)
                .joinToString(" · "),
            style = Schrift.MonoNormal,
            color = Farben.AmberHell,
        )

        einsatz.meldender?.let { SehrLeise("gemeldet von $it") }

        if (einsatz.empfohleneFahrzeuge > 0 && !einsatz.abgeschlossen) {
            Verlaufsbalken(
                anteil = einsatz.alarmierteFahrzeuge.size.toFloat() / einsatz.empfohleneFahrzeuge,
                farbe = einsatzFarbe(einsatz.state),
            )
            SehrLeise("${einsatz.alarmierteFahrzeuge.size}/${einsatz.empfohleneFahrzeuge} Fahrzeuge alarmiert")
        }
    }

    // Die zwei Meldungen, die jede Fahrt braucht — als Knöpfe, nicht als
    // Merksatz: Status 4 bei Ankunft, Status 1 nach getaner Arbeit.
    Ueberschrift("Melden")
    Pillenreihe {
        Pille("Status 3 — Anfahrt", an = meins.status == 3, beiDruck = { beiFms(3, null, null) })
        Pille("Status 4 — vor Ort", an = meins.status == 4, beiDruck = { beiFms(4, null, null) })
        Pille("Status 1 — frei", an = meins.status == 1, beiDruck = { beiFms(1, null, null) })
    }

    Ueberschrift("Lagemeldung")
    val saetze = remember(einsatz.stichwort, katalog) {
        katalog?.stichworte?.firstOrNull { it.stichwort == einsatz.stichwort }
            ?.lagemeldungen.orEmpty()
    }

    if (saetze.isNotEmpty()) {
        Pillenreihe {
            saetze.forEach { satz ->
                Pille(satz, an = false, beiDruck = { beiLagemeldung(satz) })
            }
        }
    }

    FreiesMeldefeld(beiLagemeldung)
}

/** Das freie Feld für die Lage, die der Katalog nicht kennt. */
@Composable
private fun FreiesMeldefeld(beiSenden: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Feld(
            wert = text,
            beiAenderung = { text = it.take(200) },
            platzhalter = "Eigene Lagemeldung …",
            weiterTaste = ImeAction.Send,
            modifier = Modifier.weight(1f),
        )
        Knopf(
            aufschrift = "Melden",
            beiDruck = {
                beiSenden(text)
                text = ""
            },
            aktiv = text.isNotBlank(),
            kompakt = true,
        )
    }
}

/**
 * Der Status-Teil: das FMS-Tastenfeld.
 *
 * <b>Die Aufschriften kommen aus dem Katalog</b> — je Organisation, denn drei
 * Status heißen nicht überall gleich. Die gerade gültige Taste ist gefüllt,
 * alle anderen tragen nur ihre Farbe als Rand: Das ist das Display eines
 * Funkgeräts, keine Knopfreihe.
 */
@Composable
private fun ColumnScope.TeilStatus(
    meins: Rundenfahrzeug,
    tasten: List<FmsTaste>,
    beiFms: (Int, String?, Int?) -> Unit,
    beiSondersignal: (Boolean) -> Unit,
) {
    Ueberschrift("FMS-Status")

    if (tasten.isEmpty()) {
        Ladezeile("Das Tastenfeld wird geladen …")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        tasten.sortedBy { it.status }.forEach { taste ->
            val an = meins.status == taste.status
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .background(
                        color = if (an) fmsFarbe(taste.status) else Farben.Flaeche,
                        shape = Rundung.Klein,
                    )
                    .flaeche(
                        farbe = if (an) fmsFarbe(taste.status) else Farben.Flaeche,
                        randfarbe = fmsFarbe(taste.status),
                        ecke = 9.dp,
                        mitLichtkante = false,
                    )
                    .clickable(
                        onClick = { beiFms(taste.status, null, null) },
                        role = Role.Button,
                        indication = null,
                        interactionSource = null,
                    )
                    .padding(horizontal = Abstand.Normal),
            ) {
                Text(
                    text = taste.status.toString(),
                    style = Schrift.MonoNormal,
                    color = if (an) Farben.AufFarbe else fmsFarbe(taste.status),
                )
                Text(
                    text = taste.aufschrift(meins.organisation),
                    style = Schrift.Normal,
                    color = if (an) Farben.AufFarbe else Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    Ueberschrift("Fahrt")
    Schalterzeile(
        titel = "Sondersignal",
        unterzeile = "Ohne fährt es sich wie im Berufsverkehr — weil es das ist.",
        an = !meins.sondersignalAus,
        beiWechsel = { an -> beiSondersignal(!an) },
    )
}

/** Der Mehr-Teil: das Dienstende. */
@Composable
private fun ColumnScope.TeilMehrFahrzeug(beiDienstende: () -> Unit) {
    Ueberschrift("Dienstende")
    SehrLeise(
        "Das Dienstende ist eine Abstimmung — jeder stimmt mit, die Leitstelle " +
            "zählt. Nach dem Ende steht die Auswertung.",
    )
    Row {
        Knopf("Dienst beenden", beiDienstende, art = Knopfart.Gefahr)
    }
}
