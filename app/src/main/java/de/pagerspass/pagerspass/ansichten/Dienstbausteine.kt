package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Funkzeile
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.leuchtleiste
import de.pagerspass.pagerspass.ui.theme.statusquadrat

/**
 * Die Bausteine, die Fahrzeug und Leitstelle teilen.
 *
 * Funkprotokoll, Statusfarben, Einsatz- und Fahrzeugzeilen — was in beiden
 * Ansichten steht, steht hier einmal. Der Unterschied der beiden Ansichten ist,
 * *was* sie damit tun, nicht *wie* es aussieht.
 */

/**
 * Die Farbe eines FMS-Status — die Zuordnung kennt jeder aus dem Tableau.
 *
 * Zeichengleich `fmsFarbe()` aus `types.ts`: 1/2 frei, 3 Anfahrt, 4 vor Ort,
 * 0/5 Sprechwunsch, 6 außer Dienst, 7/8 gebunden.
 */
fun fmsFarbe(status: Int): Color = when (status) {
    1, 2 -> Farben.FmsFrei
    3 -> Farben.FmsAnfahrt
    4 -> Farben.FmsVorOrt
    0, 5 -> Farben.FmsSprechwunsch
    6 -> Farben.FmsDefekt
    7, 8 -> Farben.FmsGebunden
    else -> Farben.TextSehrLeise
}

/** Die Farbe eines Einsatzzustands — dieselbe Reihe, vom Zustand aus gedacht. */
fun einsatzFarbe(state: String): Color = when (state) {
    "Offen" -> Farben.SignalHell
    "Alarmiert" -> Farben.AmberHell
    "Anfahrt" -> Farben.FmsAnfahrt
    "VorOrt" -> Farben.FmsVorOrt
    "InArbeit" -> Farben.BlauHell
    "Abgeschlossen" -> Farben.GruenHell
    else -> Farben.TextSehrLeise
}

/** Wie ein Einsatzzustand heißt, wenn ein Mensch ihn liest. */
fun einsatzZustand(state: String): String = when (state) {
    "Offen" -> "Offen"
    "Alarmiert" -> "Alarmiert"
    "Anfahrt" -> "Anfahrt"
    "VorOrt" -> "Vor Ort"
    "InArbeit" -> "In Arbeit"
    "Abgeschlossen" -> "Abgeschlossen"
    else -> state
}

/**
 * Das Funkprotokoll — mitlesen und (als Text) senden.
 *
 * <b>Der Funk ist hier Text, keine Stimme.</b> Im Web spricht man über die
 * Sprechtaste, und eine Erkennung macht Text daraus; die App geht den geraden
 * Weg über `SendRadio` — dieselbe Nachricht, die auch die Erkennung erzeugt
 * hätte. Sprechfunk mit echter Stimme kommt später; das Spiel läuft ohne ihn,
 * denn die Bots lesen den Text.
 *
 * <b>Die jüngste Zeile steht unten</b> — wie in jedem Funkprotokoll: Man liest
 * mit, man blättert nicht.
 */
@Composable
fun ColumnScope.Funkprotokoll(
    zeilen: List<Funkzeile>,
    eigenerRufname: String?,
    laeuft: Boolean,
    beiSenden: (String) -> Unit,
    /** Der Zuschauer liest nur — die Eingabezeile fällt weg. */
    nurLesen: Boolean = false,
    /** Die Fahrzeuge der Runde — für Namen in der eingestellten Kennung (v6). */
    fahrzeuge: List<Rundenfahrzeug> = emptyList(),
    /** Ohne Funkschein in einer Runde mit Mitspielern ist die freie Rede zu (v6). */
    gesperrt: Boolean = false,
) {
    var satz by remember { mutableStateOf("") }

    if (zeilen.isEmpty()) {
        SehrLeise("Noch kein Funkverkehr.")
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth(),
        ) {
            zeilen.takeLast(FUNK_SICHTBAR).forEach { zeile -> Funkzeile(zeile, eigenerRufname, fahrzeuge) }
        }
    }

    if (nurLesen) return

    // Der Grund steht über der Eingabe — als Satz und nicht als Verweis, denn ein Klick
    // darauf risse mitten aus der laufenden Schicht.
    if (gesperrt) {
        Text(
            "Mit anderen Spielern funkst du erst nach dem Lehrgang „Sprechfunk\" (nach der Schicht unter " +
                "Lehrgänge). Status, Lagemeldung und Nachforderung gehen trotzdem.",
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Feld(
            wert = satz,
            beiAenderung = { satz = it.take(200) },
            platzhalter = "Funkspruch …",
            weiterTaste = ImeAction.Send,
            modifier = Modifier.weight(1f),
        )
        Knopf(
            aufschrift = "Senden",
            beiDruck = {
                beiSenden(satz)
                satz = ""
            },
            aktiv = !laeuft && !gesperrt && satz.isNotBlank(),
            kompakt = true,
        )
    }
}

/**
 * Eine Zeile im Protokoll — als Nachrichtenkarte.
 *
 * Die Art färbt die Innenkante: System leise, Alarm signalrot, Lagemeldung
 * amber, Nachforderung orange, Funk blau. Ein Alarm glimmt als ganze Karte
 * (moderner Funkverkehr im Web, 01.10.2026). Die Uhrzeit steht als Pille rechts
 * oben. <b>Die eigene Kennung wird hervorgehoben</b> — wer gerufen wird, soll die
 * Zeile finden, ohne jede zu lesen.
 */
@Composable
private fun Funkzeile(zeile: Funkzeile, eigenerRufname: String?, fahrzeuge: List<Rundenfahrzeug>) {
    val (kante, schrift) = when (zeile.kind) {
        "System" -> Farben.TextSehrLeise to Farben.TextSehrLeise
        "Alarm" -> Farben.Signal to Farben.SignalHell
        "Lagemeldung" -> Farben.Amber to Farben.AmberHell
        "Nachforderung" -> Farben.FmsAnfahrt to Farben.OrangeHell
        else -> Farben.BlauHell to Farben.Text
    }
    val alarm = zeile.kind == "Alarm"

    val anMich = eigenerRufname != null &&
        (zeile.an == eigenerRufname || zeile.text.contains(eigenerRufname))

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (alarm) Farben.SignalTief.copy(alpha = 0.35f) else Farben.Flaeche,
                randfarbe = when {
                    alarm -> Farben.Signal.copy(alpha = 0.55f)
                    anMich -> Farben.Amber.copy(alpha = 0.55f)
                    else -> Color.White.copy(alpha = 0.07f)
                },
                ecke = 12.dp,
            )
            .leuchtleiste(kante, ecke = 12.dp, schein = if (alarm) 0.6f else 0f, glimmt = alarm)
            .padding(start = Abstand.Normal + 3.dp, end = Abstand.Klein, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = funkanzeige(zeile.von, fahrzeuge) + (zeile.an?.let { " → ${funkanzeige(it, fahrzeuge)}" } ?: ""),
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                color = if (anMich) Farben.Amber else Farben.TextSehrLeise,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Zeitpille(uhrzeit(zeile.zeit))
        }
        Text(text = funkanzeige(zeile.text, fahrzeuge), style = Schrift.Klein, color = schrift)
    }
}

/** Die Uhrzeit einer Nachricht als ruhige Pille. */
@Composable
private fun Zeitpille(zeit: String) {
    Text(
        text = zeit,
        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
        color = Farben.TextSehrLeise,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
            .padding(horizontal = Abstand.Klein, vertical = 1.dp),
    )
}

/**
 * Eine ruhige Marke für Nebenkennungen — Fahrzeugtyp, BOT.
 *
 * Gefüllt mit sechs Prozent Weiß, ohne Rand: `.kachel__typ` und `.mini` im Web.
 * Sie liest sich, ohne mit den farbigen Marken (Zustand, Sprechwunsch) um
 * Aufmerksamkeit zu konkurrieren.
 */
@Composable
fun Kennmarke(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = Schrift.Winzig.copy(fontWeight = FontWeight.SemiBold),
        color = Farben.TextLeise,
        maxLines = 1,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
    )
}

/**
 * Eine Einsatzzeile — die Bauform der Einsatzliste.
 *
 * Seit dem modernen Anstrich der Leitstelle (01.10.2026) eine weiche Karte: Die
 * Leuchtleiste links trägt den Zustand, der Schein dahinter die Organisation,
 * der Zustand selbst steht als Pille rechts, und unten zeigt ein runder Balken,
 * wie viele der empfohlenen Fahrzeuge alarmiert sind. Ein Einsatz mit Priorität
 * 3 und mehr glimmt.
 */
@Composable
fun Einsatzzeile(
    einsatz: Einsatz,
    beiDruck: (() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 12.dp, randfarbe = Color.White.copy(alpha = 0.07f))
            .leuchtleiste(
                einsatzFarbe(einsatz.state),
                ecke = 12.dp,
                glimmt = einsatz.prioritaet >= 3 && !einsatz.abgeschlossen,
            )
            .then(
                if (beiDruck != null) {
                    Modifier.clickable(onClick = beiDruck)
                } else {
                    Modifier
                }
            )
            .padding(start = Abstand.Normal + 3.dp, end = Abstand.Normal, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = einsatz.stichwort,
                    style = Schrift.MonoNormal,
                    color = Farben.Text,
                )
                Text(
                    text = einsatz.stichwortText,
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            SehrLeise(
                listOfNotNull(
                    einsatz.adresse.ifBlank { null },
                    einsatz.ortsteil,
                    "${einsatz.alarmierteFahrzeuge.size}/${einsatz.empfohleneFahrzeuge} Fahrzeuge"
                        .takeIf { einsatz.empfohleneFahrzeuge > 0 },
                ).joinToString(" · "),
            )

            // Das Sichtungsband — drei Varianten je Kenntnisstand, wie im
            // Leitstellenbogen des Web: volle Marken, Vorsichtung, oder nur
            // die Tatsache MANV. Schwarz steht hier nur bei > 0.
            if (einsatz.manv) {
                val s = einsatz.sichtung
                Text(
                    text = when {
                        s != null -> listOfNotNull(
                            "MANV${einsatz.manvStufe?.let { " $it" } ?: ""}",
                            "${s.rot} Rot",
                            "${s.gelb} Gelb",
                            "${s.gruen} Grün",
                            "${s.schwarz} Schwarz".takeIf { s.schwarz > 0 },
                        ).joinToString(" · ")
                        einsatz.betroffeneUngefaehr != null ->
                            "ca. ${einsatz.betroffeneUngefaehr} Betroffene – Vorsichtung läuft"
                        else -> "MANV – Sichtung steht aus"
                    },
                    style = Schrift.Winzig,
                    color = Farben.SignalHell,
                )
            }

            // Wie weit die Alarmierung ist — runder Verlaufsbalken in der
            // Zustandsfarbe, nur wenn es eine Empfehlung gibt.
            if (einsatz.empfohleneFahrzeuge > 0 && !einsatz.abgeschlossen) {
                Verlaufsbalken(
                    anteil = einsatz.alarmierteFahrzeuge.size.toFloat() / einsatz.empfohleneFahrzeuge,
                    farbe = einsatzFarbe(einsatz.state),
                    modifier = Modifier.padding(top = Abstand.Winzig),
                )
            }
        }

        Marke(einsatzZustand(einsatz.state), farbe = einsatzFarbe(einsatz.state))
    }
}

/**
 * Der schmale, runde Verlaufsbalken der modernen Leitstelle — Alarmierung,
 * Löschwasser, Aufgaben. Leiser als `Fortschritt`: keine Zahl, kein Schein,
 * vier Punkte hoch.
 */
@Composable
fun Verlaufsbalken(anteil: Float, farbe: Color, modifier: Modifier = Modifier) {
    val gezeigt = anteil.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(999.dp)),
    ) {
        if (gezeigt > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(gezeigt)
                    .fillMaxHeight()
                    .background(farbe, RoundedCornerShape(999.dp)),
            )
        }
    }
}

/**
 * Eine Fahrzeugzeile mit Statusquadrat — für Tableau und Alarmauswahl.
 *
 * Die Bauform der Fahrzeugliste nach ihrem zweiten Durchgang (01.10.2026): der
 * Status als abgerundetes Quadrat in FMS-Farbe mit Lichtkante und Schein, der
 * Typ als ruhige Marke oben rechts, „BOT" als eigene Pille, ein Hauch der
 * Organisationsfarbe an der Kante.
 */
@Composable
fun Dienstfahrzeugzeile(
    fahrzeug: Rundenfahrzeug,
    hinten: (@Composable () -> Unit)? = null,
    beiDruck: (() -> Unit)? = null,
    gewaehlt: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = Farben.Flaeche,
                randfarbe = if (gewaehlt) Farben.Amber.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.07f),
                ecke = 12.dp,
            )
            .leuchtleiste(Farben.org(fahrzeug.organisation), ecke = 12.dp, schein = 0.35f)
            .then(
                if (beiDruck != null) {
                    Modifier.clickable(onClick = beiDruck)
                } else {
                    Modifier
                }
            )
            .padding(start = Abstand.Normal + 3.dp, end = Abstand.Normal, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        // Das Statusquadrat: die Zahl auf der Statusfarbe. Das ist das Tableau
        // in klein — wer FMS kennt, liest die Farbe schneller als das Wort.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .statusquadrat(fmsFarbe(fahrzeug.status), ecke = 11.dp),
        ) {
            Text(
                text = fahrzeug.status.toString(),
                style = Schrift.MarkeZahl,
                color = Farben.AufFarbe,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fahrzeug.funkrufname,
                    style = Schrift.MonoNormal,
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (fahrzeug.playerId == null) Kennmarke("BOT")
            }
            // Der Statustext in Textfarbe, darunter leise, was sonst zählt.
            if (fahrzeug.statusText.isNotBlank()) {
                Text(
                    text = fahrzeug.statusText,
                    style = Schrift.Klein,
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (fahrzeug.alarmOffen) {
                Text(text = "nicht quittiert", style = Schrift.Winzig, color = Farben.SignalHell)
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        ) {
            if (fahrzeug.typ.isNotBlank()) Kennmarke(fahrzeug.typ)
            if (fahrzeug.sprechwunschSeit != null) {
                Marke(
                    text = if (fahrzeug.sprechwunschVorrang) "Sprechwunsch!" else "Sprechwunsch",
                    farbe = Farben.FmsSprechwunsch,
                )
            }
        }

        hinten?.invoke()
    }
}

/** „14:32" aus einem ISO-Zeitstempel — für die Funkzeile reicht die Uhrzeit. */
fun uhrzeit(roh: String): String {
    val t = zeitpunkt(roh)
    return if (t.length >= 5) t.takeLast(5) else t
}

/** Wie viele Funkzeilen sichtbar stehen — mitlesen, nicht archivieren. */
private const val FUNK_SICHTBAR = 30

/**
 * Der Leitstellendraht — Leitstelle zu Leitstelle, ohne Fahrzeuge dazwischen.
 *
 * Absender ist der Spielername, nicht die Leitstelle: Alle Plätze teilen sich
 * denselben Namen, und wer spricht, will als Mensch erkennbar sein.
 */
@Composable
fun ColumnScope.Drahtfaden(
    stand: de.pagerspass.pagerspass.mobil.Rundenstand,
    beiSenden: (String) -> Unit,
) {
    var satz by remember { mutableStateOf("") }
    val zeilen = stand.drahtGesamt

    Ueberschrift("Leitstellendraht")

    if (zeilen.isEmpty()) {
        SehrLeise("Noch nichts auf dem Draht.")
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
        ) {
            zeilen.takeLast(FUNK_SICHTBAR).forEach { zeile ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = uhrzeit(zeile.zeit),
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = Farben.TextSehrLeise,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = zeile.vonName,
                            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                            color = Farben.TextSehrLeise,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (zeile.nurGesprochen) "🎙️ Sprachdurchsage" else zeile.text,
                            style = Schrift.Klein,
                            color = Farben.Text,
                        )
                    }
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Feld(
            wert = satz,
            beiAenderung = { satz = it.take(500) },
            platzhalter = "An die anderen Leitstellen …",
            weiterTaste = ImeAction.Send,
            modifier = Modifier.weight(1f),
        )
        Knopf(
            aufschrift = "Senden",
            beiDruck = {
                beiSenden(satz)
                satz = ""
            },
            aktiv = satz.isNotBlank(),
            kompakt = true,
        )
    }
}

/**
 * Der Einsatzstellenfunk — wer mit Status 4 an der Lage steht, spricht hier.
 *
 * Absender ist der Funkrufname: Vor Ort kennt man sich am Fahrzeug. Neue
 * Zeilen kommen mit dem Raumzustand, nicht als eigenes Ereignis.
 */
@Composable
fun ColumnScope.Einsatzstellenfaden(
    zeilen: List<de.pagerspass.pagerspass.netz.Einsatzstellennachricht>,
    beiSenden: (String) -> Unit,
) {
    var satz by remember { mutableStateOf("") }

    Ueberschrift("Einsatzstelle")

    if (zeilen.isEmpty()) {
        SehrLeise("Noch kein Funkverkehr an der Einsatzstelle.")
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
        ) {
            zeilen.takeLast(FUNK_SICHTBAR).forEach { zeile ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = uhrzeit(zeile.zeit),
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = Farben.TextSehrLeise,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = zeile.vonFunkrufname,
                            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                            color = Farben.TextSehrLeise,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (zeile.nurGesprochen) "🎙️ Sprachdurchsage" else zeile.text,
                            style = Schrift.Klein,
                            color = Farben.Text,
                        )
                    }
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Feld(
            wert = satz,
            beiAenderung = { satz = it.take(500) },
            platzhalter = "An die Einsatzstelle …",
            weiterTaste = ImeAction.Send,
            modifier = Modifier.weight(1f),
        )
        Knopf(
            aufschrift = "Senden",
            beiDruck = {
                beiSenden(satz)
                satz = ""
            },
            aktiv = satz.isNotBlank(),
            kompakt = true,
        )
    }
}

/**
 * Die Sprechtaste — Halten sendet, Loslassen beendet. Das Gegenstück zu
 * `SprechTaste.vue`.
 *
 * <b>Die Aufschrift bleibt einzeilig.</b> Dieselbe Regel wie im Web, aus
 * demselben Grund: Eine zweite Zeile hier ist eine Zeile weniger im Protokoll
 * darüber. Lieber abgeschnitten als das Protokoll erdrückt.
 *
 * <b>Die Berechtigung wird beim ersten Druck geholt.</b> Wer ablehnt, sieht
 * den Hinweis auf der Taste; der Funk als Text daneben geht immer.
 */
@Composable
fun Sprechtaste(
    sendet: Boolean,
    wirdVerstanden: Boolean,
    belegtVon: String?,
    gesperrtBis: Long?,
    beiDruck: () -> Unit,
    beiLoslassen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zusammenhang = androidx.compose.ui.platform.LocalContext.current
    var erlaubt by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                zusammenhang,
                android.Manifest.permission.RECORD_AUDIO,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val frage = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { erlaubt = it }

    // Der Countdown der Sendepause zählt sichtbar herunter — eine stumme
    // Sperre sieht aus wie eine kaputte Taste.
    var jetzt by remember { mutableStateOf(System.currentTimeMillis()) }
    val gesperrt = gesperrtBis != null && gesperrtBis > jetzt
    androidx.compose.runtime.LaunchedEffect(gesperrtBis) {
        while (gesperrtBis != null && gesperrtBis > System.currentTimeMillis()) {
            jetzt = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
        jetzt = System.currentTimeMillis()
    }

    // Der Puls beim Senden — dieselbe Sprache wie die Melderblende.
    val uebergang = androidx.compose.animation.core.rememberInfiniteTransition(label = "ptt")
    val puls by uebergang.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "pttPuls",
    )

    val aufschrift = when {
        sendet -> "Sendet …"
        wirdVerstanden -> "Wird verstanden …"
        !erlaubt -> "Sprechen erlauben (tippen)"
        belegtVon != null -> "Kanal belegt"
        gesperrt -> "Sendepause · noch ${restzeit(gesperrtBis - jetzt)}"
        else -> "Sprechen (halten)"
    }

    val benutzbar = erlaubt && belegtVon == null && !gesperrt

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .flaeche(ecke = 10.dp)
            .background(
                if (sendet) Farben.Signal.copy(alpha = 0.28f * puls) else Color.Transparent,
                androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            )
            .padding(horizontal = Abstand.Gross)
            .pttGeste(
                aktivierbar = benutzbar || !erlaubt,
                beiDruck = {
                    if (!erlaubt) frage.launch(android.Manifest.permission.RECORD_AUDIO)
                    else beiDruck()
                },
                beiLoslassen = { if (erlaubt) beiLoslassen() },
            ),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (sendet) Farben.SignalHell.copy(alpha = puls) else Farben.TextSehrLeise,
                    CircleShape,
                ),
        )
        Text(
            text = aufschrift,
            style = Schrift.Klein,
            color = when {
                sendet -> Farben.SignalHell
                !benutzbar -> Farben.TextLeise
                else -> Farben.Text
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Die Halten-Geste — gedrückt sendet, losgelassen (oder abgebrochen) beendet. */
private fun Modifier.pttGeste(
    aktivierbar: Boolean,
    beiDruck: () -> Unit,
    beiLoslassen: () -> Unit,
): Modifier = pointerInput(aktivierbar) {
    detectTapGestures(
        onPress = {
            if (!aktivierbar) return@detectTapGestures
            beiDruck()
            // Loslassen wie Abrutschen beenden die Durchsage gleichermaßen —
            // dieselbe Entscheidung wie `setPointerCapture` im Web.
            tryAwaitRelease()
            beiLoslassen()
        },
    )
}

/** `m:ss` für den Sendepausen-Countdown. */
private fun restzeit(millis: Long): String {
    val s = (millis / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
