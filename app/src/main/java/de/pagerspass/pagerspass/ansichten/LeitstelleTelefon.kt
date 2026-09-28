package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Nebenleitung
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.mobil.rememberEingabeweg
import de.pagerspass.pagerspass.netz.Anruf
import de.pagerspass.pagerspass.netz.AnrufjournalEintrag
import de.pagerspass.pagerspass.netz.Gespraechszeile
import de.pagerspass.pagerspass.netz.Ortung
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay

/**
 * Das Notrufpopup — `NotrufPopup.vue`. Genau eines zur Zeit, der älteste
 * wartende Anruf; die Restzeit bis zum Verpassen läuft sichtbar mit.
 */
@Composable
internal fun Notrufblende(
    anruf: Anruf,
    weitere: Int,
    imGespraech: Boolean,
    beiAnnehmen: () -> Unit,
    beiAbweisen: () -> Unit,
    beiSpaeter: () -> Unit,
) {
    val jetzt = rememberJetzt(250)
    val rest = ((((zeitMillis(anruf.klingeltBis) ?: jetzt) - jetzt) + 999) / 1000).coerceAtLeast(0)
    val knapp = rest <= 10

    Blende(
        titel = "☎ Notruf ${anruf.nummer}",
        beiSchliessen = beiSpaeter,
        breite = Dialogbreite.Schmal,
        kopfknoepfe = {
            Text(
                "$rest s",
                style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                color = if (knapp) Farben.SignalHell else Farben.AmberHell,
            )
        },
        fuss = {
            Knopf("Später", beiSpaeter, art = Knopfart.Leise)
            Knopf("Abweisen", beiAbweisen)
            Knopf("Annehmen", beiAnnehmen, art = Knopfart.Haupt, aktiv = !imGespraech)
        },
    ) {
        Etikett("Amtsleitung")
        Leise(
            "Eine Leitung klingelt. Wer abhebt, führt das Gespräch und legt hinterher den Einsatz an.",
            klein = false,
        )
        if (weitere > 0) {
            Text(
                "$weitere ${if (weitere == 1) "weiterer Anruf" else "weitere Anrufe"} in der Leitung",
                style = Schrift.MonoKlein,
                color = Farben.AmberHell,
            )
        }
        if (weitere > 0) {
            SehrLeise("Wegdrücken — bei mehreren Leitungen ist das Priorisieren und kostet nichts.")
        } else {
            SehrLeise("Wegdrücken kostet ein paar Punkte, der Anrufer meldet sich womöglich erneut.")
        }
    }
}

/** Die sechs Fragen am Apparat — Wortlaut und Kurzform aus `TelefonFenster.vue`. */
private val FRAGEN = listOf(
    Triple("Ort", "Wo genau ist der Notfallort?", "Wo?"),
    Triple("Was", "Was genau ist passiert?", "Was?"),
    Triple("Betroffene", "Sind Menschen betroffen oder verletzt?", "Betroffene?"),
    Triple("Ausmass", "Wie groß ist das Ganze?", "Ausmaß?"),
    Triple("Gefahren", "Gibt es besondere Gefahren?", "Gefahren?"),
    Triple("Anrufer", "Wer spricht da bitte?", "Wer?"),
)

/**
 * Das Telefonfenster — `TelefonFenster.vue`.
 *
 * <b>Wie gefragt wird, entscheidet das Gerät</b> (Konto → Notrufabfrage): die
 * sechs Frageknöpfe, eine getippte Frage oder die gesprochene. Gesprochen wird
 * nur die Frage — der Anrufer antwortet am Telefon, und seine Stimme kommt über
 * den eigenen Lautsprecher der Runde.
 *
 * Zurück legt auf — wie das Schließen im Web.
 */
@Composable
internal fun Telefonfenster(
    anruf: Anruf,
    stand: Rundenstand,
    griffe: LeitstellenGriffe,
) {
    val weg by rememberEingabeweg()
    val mitMikrofon = rememberMikrofonfrage()
    val aktuellerAnruf by rememberUpdatedState(anruf)
    var freitext by remember { mutableStateOf("") }
    var gesperrt by remember(anruf.id) { mutableStateOf<Set<String>>(emptySet()) }
    var fehler by remember { mutableStateOf<String?>(null) }
    val hoert = stand.nebenleitung == Nebenleitung.Notruf
    val rollen = rememberScrollState()

    LaunchedEffect(anruf.verlauf.size) { rollen.animateScrollTo(rollen.maxValue) }

    // Eine kurze Sperre je Frage: Ein Doppeltipp stellte dieselbe Frage zweimal.
    LaunchedEffect(gesperrt) {
        if (gesperrt.isNotEmpty()) {
            delay(1_200)
            gesperrt = emptySet()
        }
    }

    val ortung = anruf.ortung
    val auflegen = { griffe.anrufBeenden(anruf.id, false) }

    Blende(
        titel = "Am Apparat",
        beiSchliessen = auflegen,
        breite = Dialogbreite.Breit,
        kopfknoepfe = {
            Knopf("Orten", { griffe.anrufOrten(anruf.id) }, art = Knopfart.Leise, aktiv = ortung == null, kompakt = true)
            Knopf("Abbrechen", { griffe.anrufBeenden(anruf.id, true) }, art = Knopfart.Leise, kompakt = true)
            Knopf("Auflegen", auflegen, art = Knopfart.Alarm, kompakt = true)
        },
    ) {
        Etikett("Notruf ${anruf.nummer}")
        SehrLeise("Der Funk ist stumm, solange das Gespräch läuft — nach dem Auflegen hörst du ihn wieder.")

        ortungstext(ortung, journal = false)?.let { text ->
            Text(
                text,
                style = Schrift.MonoKlein,
                color = if (ortung?.laeuft == true) Farben.AmberHell else Farben.Text,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 320.dp)
                .flaeche(farbe = Farben.BgTief, ecke = 9.dp)
                .verticalScroll(rollen)
                .padding(Abstand.Normal),
        ) {
            if (anruf.verlauf.isEmpty()) SehrLeise("Das Gespräch beginnt — stell die erste Frage.")
            anruf.verlauf.forEach { Gespraechszeilenanzeige(it) }
        }

        fehler?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }

        when (weg) {
            "tippen" -> Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.Bottom,
            ) {
                Feld(
                    wert = freitext,
                    beiAenderung = { freitext = it.take(200) },
                    platzhalter = "Frage eintippen, z. B. „Wo genau ist das?“",
                    weiterTaste = ImeAction.Send,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Fragen",
                    {
                        griffe.anrufFrageFrei(anruf.id, freitext.trim())
                        freitext = ""
                    },
                    art = Knopfart.Haupt,
                    aktiv = freitext.isNotBlank(),
                    kompakt = true,
                )
            }

            "sprechen" -> {
                // Gedrückt halten und fragen — die Erkennung macht der Server.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .background(if (hoert) Farben.SignalTief else Farben.FlaecheHoch, Rundung.Klein)
                        .border(1.dp, if (hoert) Farben.SignalHell else Farben.RandHell, Rundung.Klein)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    var los = false
                                    mitMikrofon {
                                        fehler = null
                                        los = griffe.notrufSprechenStarten()
                                        if (!los) fehler = "Kein Mikrofonzugriff. Tippen geht trotzdem."
                                    }
                                    tryAwaitRelease()
                                    if (los) griffe.notrufSprechenBeenden(aktuellerAnruf.id)
                                },
                            )
                        },
                ) {
                    Text(
                        when {
                            hoert -> "Hört zu …"
                            stand.notrufWirdVerstanden -> "Wird verstanden …"
                            else -> "Gedrückt halten und fragen"
                        },
                        style = Schrift.Normal,
                        color = if (hoert) Farben.SignalHell else Farben.Text,
                    )
                }
                SehrLeise("Gesprochen wird nur die Frage — der Anrufer antwortet dir am Telefon.")
            }

            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                FRAGEN.forEach { (art, _, kurz) ->
                    val gefragt = art in anruf.erfragt
                    Knopf(
                        kurz + if (gefragt) " ✓" else "",
                        {
                            if (art !in gesperrt) {
                                gesperrt = gesperrt + art
                                griffe.anrufFrage(anruf.id, art)
                            }
                        },
                        art = if (gefragt) Knopfart.Leise else Knopfart.Normal,
                        aktiv = art !in gesperrt,
                        kompakt = true,
                    )
                }
            }
        }
    }
}

/** Eine Zeile des Gesprächs — wer spricht, steht vorn. */
@Composable
private fun Gespraechszeilenanzeige(zeile: Gespraechszeile) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Text(
            if (zeile.vonLeitstelle) "Leitstelle" else "Anrufer",
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
            color = if (zeile.vonLeitstelle) Farben.AmberHell else Farben.BlauHell,
        )
        Text(
            zeile.text,
            style = Schrift.Klein,
            color = when {
                !zeile.verstanden -> Farben.TextSehrLeise
                zeile.vonLeitstelle -> Farben.TextLeise
                else -> Farben.Text
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/** Was die Ortung gerade sagt — am Apparat oder im Journal. */
private fun ortungstext(o: Ortung?, journal: Boolean): String? {
    o ?: return null
    return when {
        o.laeuft && journal -> "Nachträgliche Ortung läuft — die Verbindung steht nicht mehr, das dauert."
        o.laeuft -> "Ortung läuft — das dauert einen Moment …"
        o.erfolgreich != true && journal -> "Nachträgliche Ortung fehlgeschlagen — keine verwertbaren Verbindungsdaten."
        o.erfolgreich != true -> "Ortung fehlgeschlagen — kein verwertbares Signal."
        journal -> "Anschluss geortet: ±${o.radiusMeter ?: 0} m, Kreis liegt auf der Lagekarte."
        else -> "Anrufer geortet: ±${o.radiusMeter ?: 0} m, Kreis liegt auf der Lagekarte."
    }
}

/** `AUSGANG_LABEL` — wie ein Anruf ausging. */
private fun ausgangText(ausgang: String): String = when (ausgang) {
    "EinsatzAngelegt" -> "Einsatz angelegt"
    "Verworfen" -> "Verworfen"
    "Verpasst" -> "Verpasst"
    "Abgewiesen" -> "Abgewiesen"
    "Presse" -> "Presse"
    "Zugeordnet" -> "Zu laufendem Einsatz"
    else -> ausgang
}

/**
 * Das Anrufjournal der Schicht — `Anrufjournal.vue`. Jedes Gespräch bleibt
 * nachlesbar; ein abgebrochener Anruf lässt sich noch orten, und aus der
 * Ortung wird ein Bogen mit dem georteten Punkt als Ort.
 */
@Composable
internal fun Anrufjournalblende(
    raum: Raumzustand,
    anfangsAnruf: String?,
    griffe: LeitstellenGriffe,
    beiSchliessen: () -> Unit,
    beiEinsatz: (incidentId: String) -> Unit,
    beiOrtungUebernehmen: (Geortet) -> Unit,
) {
    var aufgeklappt by remember { mutableStateOf(anfangsAnruf) }
    val eintraege = raum.anrufjournal.reversed()

    fun einsatznummer(id: String?): String? =
        id?.let { i -> raum.incidents.firstOrNull { it.id == i }?.einsatznummer }

    fun geortet(e: AnrufjournalEintrag): Geortet? {
        val o = e.ortung ?: return null
        if (o.laeuft || o.erfolgreich != true) return null
        val lat = o.lat ?: return null
        val lon = o.lon ?: return null
        return Geortet(e.anrufId, lat, lon, o.ortstext ?: "Geortete Position")
    }

    Blende(
        titel = "Anrufjournal der Schicht",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Breit,
        kopfknoepfe = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
    ) {
        Etikett("Leitstelle")
        if (eintraege.isEmpty()) {
            SehrLeise("Noch keine abgeschlossenen Anrufe — das Journal füllt sich mit jedem Gespräch.", mono = true)
        }
        eintraege.forEach { e ->
            val offen = aufgeklappt == e.anrufId
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { aufgeklappt = if (offen) null else e.anrufId },
                ) {
                    Text("Notruf ${e.nummer}", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    SehrLeise(uhrzeit(e.eingangUm), mono = true)
                    Text(
                        ausgangText(e.ausgang) + (einsatznummer(e.incidentId)?.let { " · $it" } ?: ""),
                        style = Schrift.MonoKlein,
                        color = when (e.ausgang) {
                            "EinsatzAngelegt", "Zugeordnet" -> Farben.GruenHell
                            "Verpasst", "Abgewiesen" -> Farben.SignalHell
                            else -> Farben.TextLeise
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SehrLeise(if (e.verlauf.isNotEmpty()) "${e.verlauf.size} Zeilen" else "kein Gespräch", mono = true)
                    Text(if (offen) "▾" else "▸", style = Schrift.Klein, color = Farben.TextLeise)
                }
                if (offen) {
                    if (e.verlauf.isEmpty()) {
                        SehrLeise("Es kam kein Gespräch zustande.", mono = true)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                            e.verlauf.forEach { Gespraechszeilenanzeige(it) }
                        }
                    }
                    ortungstext(e.ortung, journal = true)?.let { text ->
                        Text(
                            text,
                            style = Schrift.MonoKlein,
                            color = if (e.ortung?.laeuft == true) Farben.AmberHell else Farben.Text,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    ) {
                        if (e.ortbar) {
                            Knopf("Anschluss nachträglich orten", { griffe.journalOrten(e.anrufId) }, art = Knopfart.Leise, kompakt = true)
                        }
                        geortet(e)?.let { g ->
                            Knopf("Einsatz an dieser Position anlegen", { beiOrtungUebernehmen(g) }, kompakt = true)
                        }
                        val nummer = einsatznummer(e.incidentId)
                        val id = e.incidentId
                        if (id != null && nummer != null) {
                            Knopf("Zum Einsatz $nummer", { beiEinsatz(id) }, art = Knopfart.Leise, kompakt = true)
                        }
                    }
                }
            }
        }
    }
}
