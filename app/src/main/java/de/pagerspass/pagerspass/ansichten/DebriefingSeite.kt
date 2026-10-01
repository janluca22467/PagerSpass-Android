package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Archivrunde
import de.pagerspass.pagerspass.netz.Befund
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Werkwege
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Nachbesprechung — übertragen aus `web/src/views/DebriefingView.vue` (5.0.0.26)
 * in ihrer Handyform.
 *
 * <b>Erst der Spielstand, dann die Ablage.</b> Beim Öffnen wird die Schicht einmal
 * aus dem Archiv geholt (`/api/archiv/runde`): Dort steht sie vollständig — mit
 * Auswertung, Chronik je Einsatz und dem ganzen Funkprotokoll. Klappt es nicht (noch
 * nicht abgelegt, Netz hakt), bleibt es beim Raumzustand: Eine Zahl aus einem
 * Ausschnitt ist besser als eine leere Ansicht.
 *
 * <b>Die Reihenfolge des Webs:</b> Kennzahlen, was es fürs Dienstbuch gab, was zur
 * Schicht auffiel, Doppelmeldungen, wer was getan hat, die Mannschaft, die Einsätze
 * mit ihrer Chronik, das Anrufjournal und das Funkprotokoll.
 *
 * Die Schichtkarte (das teilbare Bild) gibt es im Web; sie ist hier noch nicht dabei.
 */
@Composable
fun DebriefingSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Rundenstand = Rundenstand(),
    beiVerlassen: () -> Unit = {},
    eigeneKennung: String = "",
) {
    val gutschrift = stand.gutschrift
    val raum = stand.raum
    val zusammenhang = LocalContext.current

    val archiv by produceState<Archivrunde?>(null, raum?.code, eigeneKennung) {
        val code = raum?.code
        value = if (code.isNullOrBlank() || eigeneKennung.isBlank()) {
            null
        } else {
            runCatching { Werkwege(Netz(Ablage(zusammenhang))).archivrunde(code, eigeneKennung) }.getOrNull()
        }
    }
    val runde = archiv

    // Die Kennzahlen: aus dem Archiv, sonst aus dem Raum.
    val einsaetze = runde?.incidents?.size ?: raum?.incidents?.size ?: 0
    val abgearbeitet = runde?.incidents?.count { it.state == "Abgeschlossen" } ?: raum?.incidents?.count { it.abgeschlossen } ?: 0
    fun schnitt(werte: List<Double?>): Double? = werte.filterNotNull().takeIf { it.isNotEmpty() }?.average()
    val dispo = schnitt(runde?.incidents?.map { it.dispositionszeitSekunden }.orEmpty())
    val hilfsfrist = schnitt(runde?.incidents?.map { it.hilfsfristSekunden }.orEmpty())
    val funk = runde?.funkprotokoll ?: stand.funk

    val beginn = zeitInMillis(runde?.gestartetUm ?: raum?.gestartetUm)
    val ende = zeitInMillis(runde?.beendetUm)
    val dienstdauer = if (beginn != null && ende != null) {
        val min = ((ende - beginn) / 60_000).toInt()
        if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
    } else {
        "—"
    }
    // Unter fünf Minuten bringt eine Schicht keine Punkte — dieselbe Grenze wie
    // `Erfahrung.Mindestdienstzeit` am Server. „+0" allein sähe aus wie ein Fehler.
    val zuKurz = beginn != null && ende != null && ende - beginn < 5 * 60_000

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Dienst beendet",
            unterzeile = listOfNotNull(
                raum?.settings?.leitstelle?.ifBlank { null },
                raum?.code?.let { "Raum $it" },
                "Dienstdauer $dienstdauer",
            ).joinToString(" · "),
            knoepfe = { Knopf("Zum Start", beiVerlassen, kompakt = true) },
        )

        Kennzahltafel(
            listOf(
                { m -> Kennzahlkachel("Einsätze", "$einsaetze", "$abgearbeitet abgeschlossen", m) },
                { m -> Kennzahlkachel("Ø Dispositionszeit", dauerText(dispo), "Notruf bis Alarm", m) },
                { m -> Kennzahlkachel("Ø Hilfsfrist", dauerText(hilfsfrist), "Alarm bis Eintreffen", m) },
                { m -> Kennzahlkachel("Funksprüche", "${funk.size}", "im Protokoll", m) },
            ),
        )

        // Was der Abend fürs eigene Dienstbuch gebracht hat — zurückhaltend, aber sichtbar.
        if (gutschrift != null) {
            Kasten(marke = true, wartet = !zuKurz, abstandInnen = Abstand.Klein) {
                Etikett("Fürs Dienstbuch")
                Text("+${zahl(gutschrift.punkte)}", style = Schrift.Anzeige, color = Farben.AmberHell)
                Text("${gutschrift.rang} · ${zahl(gutschrift.gesamt)} Punkte insgesamt", style = Schrift.Normal, color = Farben.Text)
                if (zuKurz) {
                    Text(
                        "Die Schicht stand keine fünf Minuten — sie steht im Dienstbuch, bringt aber keine " +
                            "Punkte. Ab fünf Minuten Dienstzeit zählt sie ganz normal.",
                        style = Schrift.Klein,
                        color = Farben.AmberHell,
                    )
                } else {
                    SehrLeise("Stufe ${gutschrift.level}")
                }
            }
        } else if (raum?.settings?.sandkasten == true) {
            Kasten(abstandInnen = Abstand.Klein) {
                Text("Keine Wertung", style = Schrift.Gross, color = Farben.Text)
                SehrLeise("Im Sandkasten gibt es keine Erfahrung, keine Rangliste und keine Saisonwertung.")
            }
        }

        if (runde == null) {
            SehrLeise("Die vollständige Auswertung wird aus dem Archiv geholt …")
        }

        val befunde = runde?.auswertung?.befunde.orEmpty()
        val schichtbefunde = sortiert(befunde.filter { it.incidentId == null })
        if (schichtbefunde.isNotEmpty()) {
            Abschnitt("Zur Schicht") { schichtbefunde.forEach { Befundkarte(it) } }
        }

        // Erst hier wird ausgesprochen, was während der Schicht niemand erfahren durfte:
        // welche Lage mehrfach gemeldet wurde und ob die Zuordnung gepasst hat.
        val d = runde?.auswertung?.doppelmeldungen
        if (d != null && d.nennenswert) {
            Abschnitt("Doppelmeldungen") {
                Kennzahltafel(
                    listOf(
                        { m -> Kennzahlkachel("Mehrfach gemeldet", "${d.mehrfachGemeldeteLagen}", "Lagen mit mehr als einem Anrufer", m) },
                        { m -> Kennzahlkachel("Zusammengeführt", "${d.zusammengefuehrt}", "Anrufe am richtigen Einsatz", m) },
                        { m -> Kennzahlkachel("Doppelt alarmiert", "${d.doppelalarmierungen}", "zweiter Einsatz zur selben Lage", m) },
                        { m -> Kennzahlkachel("Falsch zugeordnet", "${d.falschzugeordnet}", "gemeldete Lage ohne Einsatz", m) },
                    ),
                )
            }
        }

        val bilanz = runde?.auswertung?.spieler.orEmpty()
        if (bilanz.isNotEmpty()) {
            Abschnitt("Wer was getan hat") { bilanz.forEach { Bilanzzeile(it) } }
        }

        val mitspieler = raum?.players?.filter { !it.istBot && it.id != eigeneKennung }.orEmpty()
        if (mitspieler.isNotEmpty()) {
            Abschnitt("Mannschaft") {
                Kasten(abstandInnen = Abstand.Winzig) {
                    mitspieler.forEach { p ->
                        Row {
                            Text(p.name, style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f))
                            SehrLeise(p.rang)
                        }
                    }
                }
            }
        }

        Abschnitt("Einsätze") {
            val liste = runde?.incidents?.sortedBy { it.eingangUm }
            when {
                liste != null && liste.isEmpty() -> Leerhinweis("In dieser Schicht ist nichts passiert.")
                liste != null -> liste.forEach { e ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                        modifier = Modifier
                            .fillMaxWidth()
                            .flaeche(ecke = 9.dp, randfarbe = orgFarbe(e.organisation).copy(alpha = 0.6f))
                            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Text(e.stichwort, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = orgFarbe(e.organisation))
                            Text(e.stichwortText, style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f))
                        }
                        SehrLeise(
                            listOfNotNull(e.einsatznummer.ifBlank { null }, nachbesprechungsuhr(e.eingangUm), einsatzstand(e.state)).joinToString(" · "),
                            mono = true,
                        )
                        SehrLeise("Disp. ${dauerText(e.dispositionszeitSekunden)} · Hilfsfrist ${dauerText(e.hilfsfristSekunden)}", mono = true)
                        if (e.adresse.isNotBlank()) Leise(e.adresse)
                        sortiert(befunde.filter { it.incidentId == e.id }).forEach { Befundkarte(it) }
                        e.chronologie.forEach { c ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                                SehrLeise(nachbesprechungsuhr(c.zeit).orEmpty(), mono = true)
                                Text(
                                    c.text,
                                    style = Schrift.Klein,
                                    // Störungen im Ablauf stehen amber — sie erklären eine schlechte Zeit.
                                    color = if (Regex("Störung|liegengeblieben|Funkloch|Fehlalarm|böswillig").containsMatchIn(c.text)) {
                                        Farben.AmberHell
                                    } else {
                                        Farben.TextLeise
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                else -> raum?.incidents?.forEach { e -> Einsatzzeile(e) }
            }
        }

        if (runde != null && runde.anrufjournal.isNotEmpty()) {
            Anrufjournal(runde)
        }

        var funkAlle by remember { mutableStateOf(false) }
        if (funk.isNotEmpty()) {
            Abschnitt("Funkprotokoll", weiterweg = { SehrLeise("${funk.size}") }) {
                Kasten(abstandInnen = Abstand.Winzig, farbe = Farben.BgTief) {
                    (if (funkAlle) funk else funk.take(60)).forEach { z ->
                        Text(
                            "${nachbesprechungsuhr(z.zeit) ?: ""}  ${z.von}${z.an?.let { " → $it" } ?: ""}: ${z.text}",
                            style = Schrift.MonoKlein,
                            color = if (z.kind == "System") Farben.TextSehrLeise else Farben.TextLeise,
                        )
                    }
                }
                if (!funkAlle && funk.size > 60) {
                    Textweg("Alle ${funk.size} Funksprüche zeigen", { funkAlle = true })
                }
            }
        }

        Knopf("Zum Start", beiVerlassen, art = Knopfart.Haupt, breit = true)
    }
}

/** Erst die Mängel, dann die Hinweise, zuletzt das Lob. */
private fun sortiert(liste: List<Befund>): List<Befund> {
    val reihe = listOf("Mangel", "Hinweis", "Lob")
    return liste.sortedBy { reihe.indexOf(it.grad).let { i -> if (i < 0) reihe.size else i } }
}

@Composable
private fun Befundkarte(b: Befund) {
    val farbe = when (b.grad) {
        "Mangel" -> Farben.SignalHell
        "Lob" -> Farben.GruenHell
        else -> Farben.AmberHell
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 9.dp, randfarbe = farbe.copy(alpha = 0.5f))
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Marke(b.grad, farbe = farbe)
            Text(b.titel, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
        }
        if (b.text.isNotBlank()) Leise(b.text)
    }
}

private fun dauerText(sekunden: Double?): String {
    if (sekunden == null) return "—"
    val s = Math.round(sekunden).toInt()
    return if (s >= 60) "%d:%02d min".format(s / 60, s % 60) else "$s s"
}

private fun nachbesprechungsuhr(roh: String?): String? = zeitInMillis(roh)?.let {
    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        .format(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()))
}

private fun einsatzstand(state: String): String = when (state) {
    "Offen" -> "offen"
    "Alarmiert" -> "alarmiert"
    "Anfahrt" -> "auf Anfahrt"
    "VorOrt" -> "vor Ort"
    "InArbeit" -> "in Arbeit"
    "Abgeschlossen" -> "abgeschlossen"
    else -> state
}
