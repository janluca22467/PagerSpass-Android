package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.netz.Beteiligter
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Polizeiabfrage
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Der Abfrageplatz der Leitstelle: INPOL, EWO, ZFER, NWR und ZEVIS an einem Pult —
 * übertragen aus `Abfrageplatz.vue`, `AbfrageBand.vue` und `composables/abfrageplatz.ts`
 * (Web 5.0.0.26), in der Handyform: ein Vollbild, darin Eingang, Maske und Auskünfte
 * untereinander statt nebeneinander.
 *
 * <b>Richtig durchgeben, in beide Richtungen.</b> Was in die Maske getippt wird, fragt
 * das Register ab — ein Zahlendreher im Geburtsdatum ergibt „keine Person bekannt", und
 * das ist dann die Auskunft. Und was abgefragt ist, weiß die Streife erst, wenn es
 * durchgegeben ist: Bis dahin steht es unter „Durchgeben".
 *
 * <b>Die Fahrzeugprüfung sind drei Fragen.</b> Zulassung und Halter, Sachfahndung,
 * Versicherung — wer nur fragt, wem der Wagen gehört, erfährt nicht, dass er gestohlen ist.
 */

/** Für wen der Platz geöffnet ist — `Abfragefokus` im Web; null heißt freie Abfrage. */
data class Abfragefokus(val incidentId: String, val beteiligterId: String)

private enum class Abfragemaske(val titel: String) { Person("Person"), Fahrzeug("Fahrzeug") }

/** Ein Register mit dem Kürzel, das im Dienst fällt, und seiner Dauer in Sekunden. */
private class Register(val art: String, val kurz: String, val name: String, val dauer: Int)

private val REGISTER = mapOf(
    Abfragemaske.Person to listOf(
        Register("Person", "INPOL", "Person, Fahndung", 25),
        Register("Melderegister", "EWO", "Melderegister", 15),
        Register("Fuehrerschein", "ZFER", "Fahrerlaubnis", 15),
        Register("Waffenregister", "NWR", "Waffenregister", 20),
    ),
    Abfragemaske.Fahrzeug to listOf(
        Register("Halter", "ZEVIS", "Zulassung, Halter", 12),
        Register("Sachfahndung", "FAHND", "Gestohlen?", 10),
        Register("Versicherung", "VERS", "Versicherung", 8),
    ),
)

private val KURZ = REGISTER.values.flatten().associate { it.art to it.kurz }
private val FAHRZEUGARTEN = REGISTER.getValue(Abfragemaske.Fahrzeug).map { it.art }.toSet()
private val GEBURTSDATUM = Regex("""^\d{1,2}\.\d{1,2}\.\d{2,4}$""")

private class Abfragezeile(val einsatz: Einsatz, val person: Beteiligter)

private fun alle(raum: Raumzustand) =
    raum.incidents.flatMap { e -> e.beteiligte.map { Abfragezeile(e, it) } }

private fun wer(p: Beteiligter) = p.name ?: p.kennzeichen ?: p.rolleText

private fun lage(e: Einsatz) = "${e.stichwort} · ${e.adresse}"

/** Was am Platz wartet — die Zahl, die das Band trägt. */
fun abfragenWartend(raum: Raumzustand?): Int = raum?.let { r ->
    alle(r).sumOf { z ->
        z.person.abfragen.count { a -> a.durchzugebenAn != null || (a.angefragtVon != null && z.person.fragtAb != a.art) }
    }
} ?: 0

/**
 * Das Band der Polizeiabfragen — eine Zeile je Abfrage, die auf die Leitstelle wartet,
 * neben Feststellung und Anfragen. Eigensicherung zuerst und rot: Bewaffnet, gesucht,
 * gewaltbereit muss die Streife zuerst hören.
 */
@Composable
fun ColumnScope.Abfrageband(raum: Raumzustand, beiOeffnen: (Abfragefokus) -> Unit) {
    class Wartend(val z: Abfragezeile, val a: Polizeiabfrage, val ersuchen: Boolean, val streife: String)

    val wartend = alle(raum).flatMap { z ->
        z.person.abfragen.mapNotNull { a ->
            when {
                a.durchzugebenAn != null -> Wartend(z, a, false, a.durchzugebenAn)
                a.angefragtVon != null && z.person.fragtAb != a.art -> Wartend(z, a, true, a.angefragtVon)
                else -> null
            }
        }
    }.sortedByDescending { it.a.eigensicherung }

    wartend.forEach { w ->
        val farbe = if (w.a.eigensicherung) Farben.Signal else Farben.Blau
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(farbe.copy(alpha = 0.10f))
                .drawBehind { drawRect(farbe, size = size.copy(width = 3.dp.toPx())) }
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                Text(
                    ((if (w.ersuchen) "Abfrage" else "Durchgeben") +
                        if (w.a.eigensicherung) " · Eigensicherung" else "").uppercase(),
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                    color = if (w.a.eigensicherung) Farben.SignalHell else Farben.BlauHell,
                )
                Text(
                    if (w.ersuchen) {
                        "${w.streife} bittet um ${w.a.name} · ${wer(w.z.person)}"
                    } else {
                        "${w.a.name} zu ${wer(w.z.person)} liegt vor — an ${w.streife}"
                    },
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                )
            }
            Text(
                if (w.ersuchen) "Abfragen" else "Durchgeben",
                style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                color = if (w.a.eigensicherung) Color(0xFFFFC9C5) else Color(0xFFCFE6FB),
                modifier = Modifier
                    .clip(Rundung.Rund)
                    .background(farbe.copy(alpha = 0.22f))
                    .border(1.dp, farbe, Rundung.Rund)
                    .clickable { beiOeffnen(Abfragefokus(w.z.einsatz.id, w.z.person.id)) }
                    .defaultMinSize(minHeight = Ziel.Kompakt)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Normal),
            )
        }
    }
}

/**
 * Der Platz selbst — am Handy ein Vollbild. Der Fokus lebt beim Aufrufer, damit Band und
 * Einsatzbogen ihn mit einer Person vorgewählt öffnen können.
 */
@Composable
fun Abfrageplatz(
    raum: Raumzustand,
    fokus: Abfragefokus?,
    befehle: Raumbefehle,
    beiFokus: (Abfragefokus?) -> Unit,
    beiSchliessen: () -> Unit,
) {
    var maske by remember { mutableStateOf(Abfragemaske.Person) }
    var name by remember { mutableStateOf("") }
    var geburtsdatum by remember { mutableStateOf("") }
    var kennzeichen by remember { mutableStateOf("") }
    val jetzt = sekundentakt()

    val zeilen = alle(raum)
    val ersuchen = zeilen.flatMap { z ->
        z.person.abfragen.filter { it.angefragtVon != null && it.durchzugebenAn == null && z.person.fragtAb != it.art }
            .map { z to it }
    }
    val durchgeben = zeilen.flatMap { z -> z.person.abfragen.filter { it.durchzugebenAn != null }.map { z to it } }
        .sortedByDescending { it.second.eigensicherung }
    val auskuenfte = zeilen.filter { z -> z.person.abfragen.any { it.ergebnis != null } || (z.person.fragtAb != null && z.person.abfrageVon != null) }
    val ziel = fokus?.let { f -> zeilen.firstOrNull { it.einsatz.id == f.incidentId && it.person.id == f.beteiligterId } }

    fun aufnehmen(z: Abfragezeile, a: Polizeiabfrage) {
        beiFokus(Abfragefokus(z.einsatz.id, z.person.id))
        maske = if (a.art in FAHRZEUGARTEN) Abfragemaske.Fahrzeug else Abfragemaske.Person
        name = ""
        geburtsdatum = ""
        kennzeichen = ""
    }

    val vollstaendig = if (maske == Abfragemaske.Fahrzeug) {
        kennzeichen.isNotBlank()
    } else {
        name.isNotBlank() && GEBURTSDATUM.matches(geburtsdatum.trim())
    }

    fun anZiel(art: String) = ziel?.person?.abfragen?.firstOrNull { it.art == art }

    /** Warum ein Register gerade nicht geht; null, wenn es geht. */
    fun sperre(r: Register): String? {
        if (!vollstaendig) {
            return if (maske == Abfragemaske.Fahrzeug) "Kennzeichen eingeben." else "Name und Geburtsdatum (TT.MM.JJJJ) eingeben."
        }
        val p = ziel?.person ?: return null
        if (p.fragtAb != null) return "Für diese Person läuft schon eine Abfrage."
        val a = anZiel(r.art) ?: return "Diese Abfrage gibt bei dieser Person nichts her."
        if (a.ergebnis != null && !a.fehleingabe) return "Liegt schon vor."
        return null
    }

    fun abfragen(r: Register) {
        if (sperre(r) != null) return
        val n = if (maske == Abfragemaske.Person) name.trim() else null
        val g = if (maske == Abfragemaske.Person) geburtsdatum.trim() else null
        val k = if (maske == Abfragemaske.Fahrzeug) kennzeichen.trim() else null
        if (ziel != null) befehle.polizeiAbfrage(ziel.einsatz.id, ziel.person.id, r.art, n, g, k)
        else befehle.freieAbfrage(r.art, n, g, k)
    }

    fun stand(r: Register): String {
        val p = ziel?.person
        if (p?.fragtAb == r.art) return "${restSekunden(p.abfrageFertigUm, jetzt) ?: 0} s"
        val a = anZiel(r.art)
        return when {
            a?.fehleingabe == true -> "Fehleingabe?"
            a?.eigensicherung == true -> "Eigensicherung"
            a?.treffer == true -> "Treffer"
            a?.ergebnis != null -> "liegt vor"
            else -> "${r.dauer} s"
        }
    }

    Vollblende("Leitstelle · INPOL · EWO · ZFER · NWR · ZEVIS", "Abfrageplatz", beiSchliessen, schliessen = "Schließen") {
        // ------------------------------------------------------- Durchgeben
        if (durchgeben.isNotEmpty()) {
            Blockkopf("Durchgeben", durchgeben.size, warm = true)
            durchgeben.forEach { (z, a) ->
                Eingang(vorsicht = a.eigensicherung, warm = true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                        Registermarke(KURZ[a.art] ?: a.art)
                        Text(wer(z.person), style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                        if (a.eigensicherung) Schild("Eigensicherung") else if (a.treffer) Schild("Treffer")
                    }
                    a.ergebnis?.let { Text(it, style = Schrift.Klein, color = Farben.TextLeise) }
                    Text(
                        "An ${a.durchzugebenAn} durchgeben",
                        style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Rundung.Klein)
                            .background(if (a.eigensicherung) Farben.Signal.copy(alpha = 0.22f) else Farben.Amber.copy(alpha = 0.18f))
                            .clickable { befehle.auskunftDurchgeben(z.einsatz.id, z.person.id, a.art) }
                            .defaultMinSize(minHeight = Ziel.Kompakt)
                            .padding(Abstand.Normal),
                    )
                }
            }
        }

        // --------------------------------------------------- Ersuchen
        Blockkopf("Ersuchen der Streifen", ersuchen.size)
        if (ersuchen.isEmpty()) {
            Leerhinweis(
                "Keine offenen Ersuchen. Eine Streife bittet über Funk um eine Abfrage — oder du " +
                    "fragst unten frei ab, etwa ein Kennzeichen aus dem Notruf.",
            )
        }
        ersuchen.forEach { (z, a) ->
            val gewaehlt = ziel?.person?.id == z.person.id && ziel.einsatz.id == z.einsatz.id
            Eingang(gewaehlt = gewaehlt, beiDruck = { aufnehmen(z, a) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Registermarke(KURZ[a.art] ?: a.art)
                    Text(a.angefragtVon.orEmpty(), style = Schrift.MonoKlein, color = Farben.Text, modifier = Modifier.weight(1f))
                    Text("→", style = Schrift.Klein, color = Farben.TextSehrLeise)
                }
                Text(
                    if (a.art in FAHRZEUGARTEN) z.person.kennzeichen.orEmpty() else "${z.person.name}, ${z.person.geburtsdatum}",
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                )
                SehrLeise(lage(z.einsatz))
            }
        }

        // ------------------------------------------------------- Maske
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.Flaeche, Rundung.Normal)
                .border(1.dp, Farben.Rand, Rundung.Normal)
                .padding(Abstand.Normal),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Ueberschrift("Abfragemaske", Modifier.weight(1f))
                if (ziel != null) {
                    Text("${ziel.person.id} · ${ziel.person.rolleText}", style = Schrift.MonoKlein, color = Farben.AmberHell)
                    Text(
                        "×",
                        style = Schrift.Normal,
                        color = Farben.TextLeise,
                        modifier = Modifier
                            .clip(Rundung.Rund)
                            .clickable { beiFokus(null) }
                            .padding(horizontal = Abstand.Klein),
                    )
                } else {
                    SehrLeise("Freie Abfrage")
                }
            }
            ziel?.let { SehrLeise(lage(it.einsatz)) }

            Segment(
                seiten = Abfragemaske.entries.toList(),
                gewaehlt = maske,
                beiWahl = { maske = it },
                aufschrift = { it.titel },
                modifier = Modifier.fillMaxWidth(),
            )

            // Was über Funk kam — zum Übernehmen oder zum Abtippen.
            val durchgegeben = ziel?.person?.let { p ->
                if (maske == Abfragemaske.Fahrzeug) p.kennzeichen
                else if (p.name != null && p.geburtsdatum != null) "${p.name}, ${p.geburtsdatum}" else null
            }
            if (durchgegeben != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Rundung.Klein)
                        .background(Farben.Blau.copy(alpha = 0.10f))
                        .border(1.dp, Farben.Blau.copy(alpha = 0.45f), Rundung.Klein)
                        .clickable {
                            val p = ziel?.person ?: return@clickable
                            if (maske == Abfragemaske.Fahrzeug) {
                                kennzeichen = p.kennzeichen.orEmpty()
                            } else {
                                name = p.name.orEmpty()
                                geburtsdatum = p.geburtsdatum.orEmpty()
                            }
                        }
                        .defaultMinSize(minHeight = Ziel.Kompakt)
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Text("DURCHGEGEBEN", style = Schrift.Etikett, color = Farben.BlauHell)
                    Text(durchgegeben, style = Schrift.MonoKlein, color = Farben.Text, modifier = Modifier.weight(1f))
                    Text("übernehmen", style = Schrift.Klein, color = Farben.BlauHell)
                }
            }

            if (maske == Abfragemaske.Person) {
                Feld(name, { name = it }, etikett = "Name, Vorname", platzhalter = "Vorname Nachname")
                Feld(
                    geburtsdatum,
                    { geburtsdatum = it },
                    etikett = "Geburtsdatum",
                    platzhalter = "TT.MM.JJJJ",
                    tastatur = KeyboardType.Number,
                    stil = Schrift.MonoNormal,
                )
            } else {
                Feld(kennzeichen, { kennzeichen = it }, etikett = "Kennzeichen", platzhalter = "GÖ-AB 1234", stil = Schrift.MonoNormal)
            }

            // Die Register als Liste: Kürzel, Name und Stand in einer Zeile, wie das Menü
            // am echten Abfrageplatz. Eine laufende Abfrage zeigt ihren Balken darunter.
            val register = REGISTER.getValue(maske)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Rundung.Klein)
                    .border(1.dp, Farben.Rand, Rundung.Klein),
            ) {
                register.forEach { r ->
                    val a = anZiel(r.art)
                    val laeuft = ziel?.person?.fragtAb == r.art
                    val gesperrt = sperre(r) != null && !laeuft
                    val treffer = a?.treffer == true || a?.eigensicherung == true
                    val anteil = if (laeuft) {
                        (1f - (restSekunden(ziel?.person?.abfrageFertigUm, jetzt) ?: 0) / r.dauer.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    Box(Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when {
                                        treffer -> Farben.Signal.copy(alpha = 0.10f)
                                        laeuft -> Farben.Amber.copy(alpha = 0.08f)
                                        else -> Color.Transparent
                                    },
                                )
                                .clickable(enabled = !gesperrt) { abfragen(r) }
                                .defaultMinSize(minHeight = Ziel.Normal)
                                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                        ) {
                            Registermarke(r.kurz)
                            Text(
                                r.name,
                                style = Schrift.Klein,
                                color = if (gesperrt) Farben.TextSehrLeise else Farben.Text,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                stand(r),
                                style = Schrift.MonoKlein,
                                color = when {
                                    treffer -> Farben.SignalHell
                                    laeuft -> Farben.AmberHell
                                    a?.ergebnis != null -> Farben.GruenHell
                                    else -> Farben.TextSehrLeise
                                },
                            )
                        }
                        if (laeuft) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(anteil)
                                    .height(2.dp)
                                    .background(Farben.Amber),
                            )
                        }
                    }
                }
            }
            register.firstNotNullOfOrNull { sperre(it) }?.let { SehrLeise(sperre(register.first()) ?: it) }
        }

        // --------------------------------------------------- Auskünfte
        Blockkopf("Auskünfte am Pult", auskuenfte.size.takeIf { it > 0 })
        if (auskuenfte.isEmpty()) Leerhinweis("Noch nichts abgefragt.")
        auskuenfte.forEach { z ->
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.Flaeche, Rundung.Klein)
                    .border(1.dp, Farben.Rand, Rundung.Klein)
                    .padding(Abstand.Normal),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Text(z.person.id, style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                    Text(wer(z.person), style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                }
                SehrLeise(lage(z.einsatz))
                val p = z.person
                if (p.fragtAb != null && p.abfrageVon != null) {
                    Text(
                        "${KURZ[p.fragtAb] ?: p.fragtAb} läuft · ${restSekunden(p.abfrageFertigUm, jetzt) ?: 0} s",
                        style = Schrift.MonoKlein,
                        color = Farben.AmberHell,
                    )
                }
                p.abfragen.filter { it.ergebnis != null }.forEach { a ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Top) {
                        Registermarke(KURZ[a.art] ?: a.art)
                        Text(
                            a.ergebnis.orEmpty(),
                            style = Schrift.Klein,
                            color = if (a.treffer || a.eigensicherung) Farben.SignalHell else Farben.TextLeise,
                            modifier = Modifier.weight(1f),
                        )
                        if (a.durchzugebenAn != null) Text("offen", style = Schrift.Winzig, color = Farben.AmberHell)
                    }
                }
            }
        }
    }
}

/**
 * Die Personen einer Polizeilage im Einsatzbogen — „Beteiligte · Abfragen" aus
 * `EinsatzDetail.vue`. Je Person eine Zeile; die Arbeit selbst geschieht am Platz.
 */
@Composable
fun ColumnScope.BeteiligtePult(einsatz: Einsatz, beiAbfrageplatz: (Abfragefokus?) -> Unit) {
    if (einsatz.beteiligte.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Ueberschrift("Beteiligte · Abfragen", Modifier.weight(1f))
        de.pagerspass.pagerspass.ui.bausteine.Knopf("Abfrageplatz", { beiAbfrageplatz(null) }, kompakt = true)
    }
    einsatz.beteiligte.forEach { p ->
        val farbe = when {
            p.abfragen.any { it.eigensicherung } -> Farben.Signal
            p.abfragen.any { it.treffer } -> Farben.Amber
            else -> Farben.Rand
        }
        val wartet = p.abfragen.any { it.angefragtVon != null && p.fragtAb != it.art }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(Rundung.Klein)
                .border(1.dp, farbe, Rundung.Klein)
                .clickable { beiAbfrageplatz(Abfragefokus(einsatz.id, p.id)) }
                .defaultMinSize(minHeight = Ziel.Kompakt)
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            Text(p.id, style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
            Text(wer(p), style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
            SehrLeise(p.rolleText)
            if (wartet) Text("Ersuchen", style = Schrift.Winzig, color = Farben.BlauHell)
        }
    }
}

// ------------------------------------------------------------------ Teile

@Composable
private fun Blockkopf(titel: String, zahl: Int?, warm: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
        Ueberschrift(titel)
        if (zahl != null) {
            Text(
                "$zahl",
                style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                color = if (warm) Farben.AufAmber else Farben.TextLeise,
                modifier = Modifier
                    .background(if (warm) Farben.Amber else Farben.FlaecheHoch, Rundung.Rund)
                    .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
            )
        }
    }
}

@Composable
private fun Registermarke(kurz: String) {
    Text(
        kurz,
        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
        color = Farben.TextLeise,
        modifier = Modifier
            .background(Farben.FlaecheHoch, Rundung.Winzig)
            .padding(horizontal = Abstand.Winzig + Abstand.Haar, vertical = Abstand.Haar),
    )
}

@Composable
private fun Schild(text: String) {
    Text(
        text.uppercase(),
        style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
        color = Farben.SignalHell,
    )
}

/** Eine Karte im Eingang — eine Bitte zum Aufnehmen oder eine Auskunft zum Durchgeben. */
@Composable
private fun Eingang(
    vorsicht: Boolean = false,
    warm: Boolean = false,
    gewaehlt: Boolean = false,
    beiDruck: (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val rand = when {
        vorsicht -> Farben.Signal
        gewaehlt -> Farben.Amber
        warm -> Farben.Amber.copy(alpha = 0.55f)
        else -> Farben.Rand
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Rundung.Klein)
            .background(if (vorsicht) Farben.Signal.copy(alpha = 0.08f) else Farben.Flaeche)
            .border(1.dp, rand, Rundung.Klein)
            .then(if (beiDruck != null) Modifier.clickable(onClick = beiDruck) else Modifier)
            .padding(Abstand.Normal),
        content = inhalt,
    )
}
