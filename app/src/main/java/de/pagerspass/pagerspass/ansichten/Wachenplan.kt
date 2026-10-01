package de.pagerspass.pagerspass.ansichten

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Wachentermin
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Gemeinsam fahren — jetzt oder später. Übertragen aus `GemeinsamFahren.vue`,
 * `DienstPlanenDialog.vue` und `Dienstplan.vue` (Web 5.0.0.26).
 *
 * <b>Warum ein eigenes Band.</b> Die Clanrunde war ein Knopf zwischen „Aussehen",
 * „Einstellungen", „Rangliste" und „Shop" — fünf gleich große Wege, von denen
 * einer der Grund ist, warum es eine Wache gibt. Der Dienstplan stand am Fuß des
 * dritten Reiters. Dabei beantworten beide dieselbe Frage: Wann fahren wir
 * zusammen? Jetzt stehen sie nebeneinander über den Reitern.
 */

private val DEUTSCH = Locale.GERMANY

/** Ein Zeitstempel des Servers als Instant — Unlesbares gibt null statt einer Ausnahme. */
private fun instantAus(roh: String?): Instant? = roh?.takeIf { it.isNotBlank() }?.let {
    runCatching { Instant.parse(it) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
}

/** „in 3 Std." / „in 2 Tagen" — dieselbe Rundung wie im Web. */
private fun abstandText(bis: Instant, jetzt: Instant = Instant.now()): String {
    val stunden = Math.round(Duration.between(jetzt, bis).toMinutes() / 60.0)
    if (stunden <= 0) return "jetzt"
    if (stunden < 24) return "in $stunden Std."
    val tage = Math.round(stunden / 24.0)
    return "in $tage ${if (tage == 1L) "Tag" else "Tagen"}"
}

/** „Do., 1. Okt., 19:30 · in 3 Tagen" — Tag und Abstand, weil man beides fragt. */
private fun bandzeit(roh: String): String {
    val dann = instantAus(roh) ?: return "—"
    val text = DateTimeFormatter.ofPattern("EE, d. MMM, HH:mm", DEUTSCH)
        .format(dann.atZone(ZoneId.systemDefault()))
    return "$text · ${abstandText(dann)}"
}

/**
 * Der Dienst, der ins Band gehört: einer, zu dem schon eine Runde offen ist, vor
 * allen anderen — dort kann man jetzt einsteigen. Sonst der nächste, der noch
 * nicht vorbei ist.
 */
private fun naechsterDienst(termine: List<Wachentermin>): Wachentermin? {
    termine.firstOrNull { it.roomCode != null }?.let { return it }
    val jetzt = Instant.now()
    return termine
        .filter { (instantAus(it.wann) ?: Instant.MIN) > jetzt }
        .minByOrNull { instantAus(it.wann) ?: Instant.MAX }
}

/**
 * Das Band unter dem Wachenkopf.
 *
 *   1. Läuft eine Clanrunde, ist das die Nachricht — mit „Dazustoßen".
 *   2. Sonst der nächste geplante Dienst, mit der eigenen Antwort daneben.
 *   3. Sonst ein Satz, dass gerade nichts ansteht.
 *
 * Starten und Planen dürfen Leitung und Zugführer; alle anderen sehen den Stand.
 */
@Composable
fun GemeinsamFahren(
    laufendeRunde: String?,
    termine: List<Wachentermin>,
    ich: String,
    darfFuehren: Boolean,
    darfRundeSchliessen: Boolean,
    landkreis: String?,
    maxTermine: Int,
    laeuft: Boolean,
    beiStarten: () -> Unit,
    beiPlanen: () -> Unit,
    beiBeitreten: (String) -> Unit,
    beiSchliessen: () -> Unit,
    beiAntworten: (Long, String) -> Unit,
) {
    val naechster = naechsterDienst(termine)
    val meine = naechster?.zusagen?.firstOrNull { it.kennung == ich }?.antwort
    val zusagen = naechster?.zusagen?.count { it.antwort == "Zugesagt" } ?: 0
    val planVoll = termine.size >= maxTermine

    Buchwink(
        zeichen = if (laufendeRunde != null) ZEICHEN_FUNK else ZEICHEN_KALENDER,
        wartet = laufendeRunde != null || naechster?.roomCode != null,
    ) {
        when {
            laufendeRunde != null -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).background(Farben.Amber, CircleShape))
                    Text("CLANRUNDE LÄUFT", style = Schrift.Etikett, color = Farben.Amber)
                }
                Text(
                    "Raum $laufendeRunde",
                    style = Schrift.Gross.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                )
                SehrLeise("Die Mannschaft ist eingeladen — steig ein, solange sie läuft.")
            }
            naechster != null -> {
                Etikett(if (naechster.roomCode != null) "Dienst hat begonnen" else "Nächster Dienst")
                Text(
                    naechster.titel,
                    style = Schrift.Gross.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                )
                SehrLeise(
                    "${bandzeit(naechster.wann)} · $zusagen ${if (zusagen == 1) "Zusage" else "Zusagen"}",
                )
            }
            else -> {
                Etikett("Gemeinsam fahren")
                Text(
                    "Gerade steht nichts an",
                    style = Schrift.Gross.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                )
                SehrLeise(
                    if (darfFuehren) {
                        "Starte eine Clanrunde" + (landkreis?.ifBlank { null }?.let { " in $it" } ?: "") +
                            " oder setz einen Dienst an, zu dem alle zusagen können."
                    } else {
                        "Leitung und Zugführer starten Clanrunden und planen Dienste."
                    },
                )
            }
        }
    }

    // Die Knöpfe stehen unter dem Satz — am Handy: Antwort bzw. Beitreten über die
    // ganze Zeile, darunter Planen und Starten halb-halb.
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        when {
            laufendeRunde != null -> Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf(
                    "Dazustoßen",
                    { beiBeitreten(laufendeRunde) },
                    art = Knopfart.Haupt,
                    modifier = Modifier.weight(1f),
                )
                // Wer sie eröffnet hat, nimmt den Hinweis auch wieder ab — die Runde
                // läuft dann weiter, sie lädt nur niemanden mehr dazu ein.
                if (darfRundeSchliessen) {
                    Knopf(
                        "Nicht mehr anzeigen",
                        beiSchliessen,
                        art = Knopfart.Leise,
                        aktiv = !laeuft,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            naechster?.roomCode != null -> Knopf(
                "Zum Dienst beitreten",
                { beiBeitreten(naechster.roomCode) },
                art = Knopfart.Haupt,
                breit = true,
            )
            naechster != null -> Segment(
                seiten = listOf("Zugesagt", "Abgesagt"),
                gewaehlt = meine ?: "Offen",
                beiWahl = { beiAntworten(naechster.nr, it) },
                aufschrift = { if (it == "Zugesagt") "Dabei" else "Nicht dabei" },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (darfFuehren) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf("Dienst planen", beiPlanen, aktiv = !planVoll, modifier = Modifier.weight(1f))
                if (laufendeRunde == null) {
                    Knopf(
                        "Clanrunde starten",
                        beiStarten,
                        art = if (naechster?.roomCode == null) Knopfart.Haupt else Knopfart.Normal,
                        aktiv = !laeuft,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            // Am Finger gibt es kein `title` — der Grund steht deshalb sichtbar da.
            if (planVoll) {
                SehrLeise(
                    "Der Plan ist voll: höchstens $maxTermine ${if (maxTermine == 1) "Dienst" else "Dienste"} " +
                        "gleichzeitig auf dieser Stufe.",
                )
            }
        }
    }
}

// ------------------------------------------------------------ Dienst planen

private val VORSCHLAEGE = listOf("Gemeinsamer Dienst", "Großschadenslage", "Nachtschicht", "Übungsabend")

/** Frühestens in einer Stunde, auf die volle Viertelstunde — dieselbe Regel wie im Web. */
private fun fruehester(): LocalDateTime {
    val d = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES)
    val rest = (15 - d.minute % 15) % 15
    return d.plusMinutes(rest.toLong())
}

/**
 * Einen Dienst in den Plan der Wache eintragen.
 *
 * <b>Die Schnellwahl ist der eigentliche Gewinn:</b> Fast jeder Dienst heißt
 * eines von vier Dingen und liegt an einem von vier Zeitpunkten. Beides ist ein
 * Tipp; Datum und Uhrzeit darunter öffnen die Wähler des Systems — dieselben,
 * die das `datetime-local`-Feld des Webs am Handy öffnet.
 */
@Composable
fun DienstPlanenBlende(
    belegt: Int,
    maxTermine: Int,
    laeuft: Boolean,
    beiPlanen: (titel: String, wann: String) -> Unit,
    beiSchliessen: () -> Unit,
) {
    val kontext = LocalContext.current
    var titel by remember { mutableStateOf("") }
    var zeitpunkt by remember { mutableStateOf(fruehester()) }
    val frueh = fruehester()

    val heute = java.time.LocalDate.now()
    val schnell = listOf(
        "Heute 20:00" to heute.atTime(20, 0),
        "Morgen 19:30" to heute.plusDays(1).atTime(19, 30),
        "Samstag 18:00" to heute.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).atTime(18, 0),
        "In einer Woche" to heute.plusDays(7).atTime(19, 30),
    ).filter { !it.second.isBefore(frueh) }

    val zuFrueh = zeitpunkt.isBefore(frueh)
    val auskunft = run {
        val text = DateTimeFormatter.ofPattern("EEEE, d. MMMM, HH:mm", DEUTSCH).format(zeitpunkt)
        val stunden = Math.round(Duration.between(LocalDateTime.now(), zeitpunkt).toMinutes() / 60.0)
        val bis = if (stunden < 24) "in ${maxOf(1L, stunden)} Std." else "in ${Math.round(stunden / 24.0)} Tagen"
        "$text Uhr · $bis"
    }

    fun eintragen() {
        if (zuFrueh || laeuft) return
        val wann = zeitpunkt.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
        beiPlanen(titel.trim().ifBlank { "Gemeinsamer Dienst" }, wann)
    }

    Blende(
        titel = "Dienst planen",
        beiSchliessen = beiSchliessen,
        fuss = {
            Knopf("Abbrechen", beiSchliessen, art = Knopfart.Leise, kompakt = true)
            Knopf("Eintragen", { eintragen() }, art = Knopfart.Haupt, aktiv = !laeuft && !zuFrueh, kompakt = true)
        },
    ) {
        Etikett("Dienstplan · $belegt von $maxTermine belegt")

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Etikett("Wofür?")
            Pillenreihe {
                VORSCHLAEGE.forEach { t -> Pille(t, an = titel == t, beiDruck = { titel = t }) }
            }
            Feld(
                wert = titel,
                beiAenderung = { titel = it.take(120) },
                platzhalter = "Oder selbst benennen (z. B. Waldbrandlage)",
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Etikett("Wann?")
            if (schnell.isNotEmpty()) {
                Pillenreihe {
                    schnell.forEach { (name, wann) ->
                        Pille(name, an = zeitpunkt == wann, beiDruck = { zeitpunkt = wann })
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf(
                    DateTimeFormatter.ofPattern("EE, dd.MM.yyyy", DEUTSCH).format(zeitpunkt),
                    {
                        DatePickerDialog(
                            kontext,
                            { _, j, m, t -> zeitpunkt = zeitpunkt.withYear(j).withMonth(m + 1).withDayOfMonth(t) },
                            zeitpunkt.year,
                            zeitpunkt.monthValue - 1,
                            zeitpunkt.dayOfMonth,
                        ).apply {
                            datePicker.minDate = frueh.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                                Duration.ofDays(1).toMillis()
                        }.show()
                    },
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    DateTimeFormatter.ofPattern("HH:mm").format(zeitpunkt),
                    {
                        TimePickerDialog(
                            kontext,
                            { _, h, min -> zeitpunkt = zeitpunkt.withHour(h).withMinute(min) },
                            zeitpunkt.hour,
                            zeitpunkt.minute,
                            true,
                        ).show()
                    },
                )
            }
            Text(
                if (zuFrueh) "Frühestens in einer Stunde — die Mannschaft soll es noch lesen können." else auskunft,
                style = Schrift.Klein,
                color = if (zuFrueh) Farben.SignalHell else Farben.Amber,
            )
        }

        SehrLeise(
            "Die Mannschaft sieht den Dienst im Plan und sagt zu oder ab. Zur Zeit steht dort ein " +
                "„Beitreten“, sobald jemand die Runde eröffnet hat.",
        )
    }
}

// ---------------------------------------------------------------- Dienstplan

/**
 * Die Dienste der Wache als Zeilen in ihrer Karte — Datumskachel, Titel, wer
 * kommt, die eigene Antwort als Umschalter. Eingetragen wird nicht hier, sondern
 * im Band „Gemeinsam fahren".
 *
 * <b>Absagen in zwei Schritten:</b> Der Knopf stand direkt neben „Kann nicht",
 * und wer nur für sich absagen wollte, nahm mit einem Griff daneben den Dienst
 * der ganzen Mannschaft aus dem Plan.
 */
@Composable
fun Dienstplan(
    termine: List<Wachentermin>,
    ich: String,
    darfPlanen: Boolean,
    beiAntworten: (Long, String) -> Unit,
    beiAbsagen: (Long) -> Unit,
    beiBeitreten: (String) -> Unit,
) {
    var absageGefragt by remember { mutableStateOf<Long?>(null) }
    val sortiert = termine.sortedBy { instantAus(it.wann) ?: Instant.MAX }

    if (sortiert.isEmpty()) {
        Box(Modifier.padding(horizontal = Abstand.Normal, vertical = Abstand.Klein)) {
            SehrLeise(
                "Noch nichts geplant." +
                    if (darfPlanen) " „Dienst planen“ steht oben im Band „Gemeinsam fahren“." else "",
            )
        }
        return
    }

    sortiert.forEachIndexed { i, t ->
        val meine = t.zusagen.firstOrNull { it.kennung == ich }?.antwort
        val dabei = t.zusagen.filter { it.antwort == "Zugesagt" }
        val zone = instantAus(t.wann)?.atZone(ZoneId.systemDefault())
        // Grüne Kante: Die Runde läuft. Amber: Da bin ich dabei. Grün geht vor.
        val kante = when {
            t.roomCode != null -> Farben.Gruen
            meine == "Zugesagt" -> Farben.Amber
            else -> null
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (t.roomCode != null) Modifier.background(Farben.Gruen.copy(alpha = 0.08f)) else Modifier)
                .drawWithContent {
                    drawContent()
                    if (kante != null) drawRect(color = kante, size = size.copy(width = 3.dp.toPx()))
                    if (i < sortiert.lastIndex) {
                        val strich = 1.dp.toPx()
                        drawRect(
                            color = Farben.Rand,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - strich),
                            size = size.copy(height = strich),
                        )
                    }
                }
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            // Die Datumskachel: Tag und Monat; die Zeile daneben Wochentag und Uhrzeit.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .defaultMinSize(minWidth = Ziel.Normal)
                    .background(Farben.BgTief, Rundung.Klein)
                    .padding(vertical = Abstand.Winzig, horizontal = Abstand.Winzig),
            ) {
                Text(
                    zone?.dayOfMonth?.toString() ?: "—",
                    style = Schrift.Gross.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.ExtraBold),
                    color = Farben.Amber,
                )
                Text(
                    zone?.let { DateTimeFormatter.ofPattern("MMM", DEUTSCH).format(it) } ?: "",
                    style = Schrift.Winzig,
                    color = Farben.TextLeise,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.titel, style = Schrift.Normal, color = Farben.Text)
                        SehrLeise(
                            (zone?.let { DateTimeFormatter.ofPattern("EE, HH:mm", DEUTSCH).format(it) } ?: "—") +
                                (t.vonName?.let { " · von $it" } ?: ""),
                            mono = true,
                        )
                    }
                    t.roomCode?.let { code ->
                        Knopf("Beitreten", { beiBeitreten(code) }, art = Knopfart.Haupt, kompakt = true)
                    }
                }
                // Wer kommt, als Stapel aus Kürzeln — das Auge zählt fünf Punkte
                // schneller, als es fünf Namen liest. Die Namen stehen daneben.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dabei.isNotEmpty()) {
                        Row(modifier = Modifier.padding(start = Abstand.Winzig)) {
                            dabei.take(5).forEachIndexed { n, z ->
                                val ton = Wappen.ton(z.kennung)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .offset(x = (-4 * n).dp)
                                        .size(24.dp)
                                        .background(Farben.Flaeche, CircleShape)
                                        .padding(1.5.dp)
                                        .background(ton, CircleShape),
                                ) {
                                    Text(
                                        Wappen.initialen(z.anzeigename),
                                        style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold, fontSize = Schrift.WINZIG * 0.8f),
                                        color = Wappen.schrift(ton),
                                    )
                                }
                            }
                        }
                        SehrLeise("Dabei: " + dabei.joinToString(", ") { it.anzeigename })
                    } else {
                        SehrLeise("Noch niemand zugesagt.")
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Segment(
                        seiten = listOf("Zugesagt", "Abgesagt"),
                        gewaehlt = meine ?: "Offen",
                        beiWahl = { beiAntworten(t.nr, it) },
                        aufschrift = { if (it == "Zugesagt") "Ich komme" else "Kann nicht" },
                    )
                    if (darfPlanen || t.von == ich) {
                        val gefragt = absageGefragt == t.nr
                        Knopf(
                            if (gefragt) "Wirklich absagen?" else "Dienst absagen",
                            {
                                if (gefragt) {
                                    absageGefragt = null
                                    beiAbsagen(t.nr)
                                } else {
                                    absageGefragt = t.nr
                                }
                            },
                            art = if (gefragt) Knopfart.Gefahr else Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                }
            }
        }
    }
}
