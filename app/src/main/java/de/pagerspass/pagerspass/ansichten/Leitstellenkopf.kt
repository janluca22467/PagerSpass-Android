package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Anruf
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Zeichenknopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Erhebung
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.kopfverlauf
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Der Kopf des Leitstellenplatzes — übertragen aus `Leitstellenkopf.vue`
 * (zweite Fassung vom 01.10.2026) in seiner Handyform.
 *
 * <b>Zwei Zeilen statt drei.</b> Oben Name, die Einsatzwerkzeuge der Freien
 * Vergabe, „Werkzeuge" und Dienstende; darunter Uhr, Lagetafel und Wetter. Die
 * übrigen Werkzeuge stehen hinter „Werkzeuge" in einem Fach, **mit Beschriftung** —
 * ein Zeichen ohne Wort ist am Finger eine Frage, und in einem Fach ist für das
 * Wort Platz.
 *
 * <b>Was hier nicht mehr steht.</b> Die Bevölkerungswarnung wohnt im Einsatzbogen
 * (gewarnt wird wegen einer Lage), das Anrufjournal bei den Einsätzen, und die
 * Notrufe klingeln in der Telefonanlage unten links. Dienstende trägt als einziger
 * Knopf ein Wort: Er hat die größten Folgen, also soll er nicht geraten werden müssen.
 */
@Composable
fun Leitstellenkopf(
    raum: Raumzustand?,
    katalog: Katalog?,
    oben: Dp,
    beiNeuerEinsatz: () -> Unit,
    beiWuerfeln: () -> Unit,
    beiBesatzung: () -> Unit,
    beiVerlassen: (() -> Unit)?,
    beiDienstende: () -> Unit,
) {
    var fachOffen by remember { mutableStateOf(false) }

    // Ein Takt je Sekunde, für Uhr und Dienstzeit zugleich. Er gehört dem Kopf: Die
    // Uhr ist Anzeige, keine Spiellogik, und läuft nur, solange sie zu sehen ist.
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            jetzt = System.currentTimeMillis()
            delay(1000)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .kopfverlauf()
            .drawBehind {
                // Die auslaufende Amberlinie, die jeden Arbeitskopf absetzt.
                val strich = 1.dp.toPx()
                drawLine(Farben.Rand, Offset(0f, size.height - strich / 2f), Offset(size.width, size.height - strich / 2f), strich)
                drawLine(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Farben.Amber.copy(alpha = 0.7f), Color.Transparent),
                        endX = size.width * 0.6f,
                    ),
                    start = Offset(0f, size.height - strich / 2f),
                    end = Offset(size.width * 0.6f, size.height - strich / 2f),
                    strokeWidth = strich,
                )
            }
            .padding(top = oben)
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        // 1 · Wer — und die Bedienung daneben.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(Kopfzeichen.Garnitur, contentDescription = null, tint = Farben.Amber, modifier = Modifier.size(26.dp))
                Box(
                    Modifier
                        .size(7.dp)
                        .background(if (raum?.laeuft == true) Farben.GruenHell else Farben.TextSehrLeise, CircleShape),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(
                        raum?.settings?.leitstelle ?: "Leitstelle",
                        raum?.settings?.leitstellensitz?.let { "Sitz $it" },
                    ).joinToString(" · "),
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (raum != null) {
                    // Die Nachbarkreise nur, wenn es welche gibt — in den meisten
                    // Kreisen wäre die Angabe leer. Der Wind zählt nur für Gefahrgut.
                    val mitkreise = raum.settings.mitkreise.mapNotNull { id -> katalog?.landkreise?.firstOrNull { it.id == id }?.name }
                    val gefahrgut = raum.settings.gefahrgutlagen &&
                        raum.incidents.any { !it.abgeschlossen && it.absperrradiusMeter != null }
                    Text(
                        text = listOfNotNull(
                            raum.code,
                            RAUM_MODUS[raum.settings.mode] ?: raum.settings.mode,
                            mitkreise.takeIf { it.isNotEmpty() }?.let { "mit ${it.joinToString(", ")}" },
                            raum.settings.windText?.takeIf { gefahrgut }?.let { "Wind aus $it" },
                        ).joinToString(" · "),
                        style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG),
                        color = Farben.TextSehrLeise,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Die Einsatzwerkzeuge der Freien Vergabe: nicht im Fach, denn mit ihnen
            // arbeitet man in diesem Modus ständig.
            if (raum?.settings?.mode == "Frei") {
                Zeichenknopf(beiNeuerEinsatz, "Neuen Einsatz aufnehmen", art = Knopfart.Haupt, kompakt = true) {
                    Icon(Kopfzeichen.HoererPlus, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Zeichenknopf(beiWuerfeln, "Einsatz auswürfeln", art = Knopfart.Leise, kompakt = true) {
                    Icon(Kopfzeichen.Wuerfel, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }

            Zeichenknopf(
                { fachOffen = !fachOffen },
                "Werkzeuge",
                art = if (fachOffen) Knopfart.Normal else Knopfart.Leise,
                kompakt = true,
            ) {
                Icon(Kopfzeichen.Pult, contentDescription = null, tint = if (fachOffen) Farben.Amber else Farben.TextLeise, modifier = Modifier.size(20.dp))
            }

            // Das Dienstende ist eine Abstimmung, keine Anweisung. Der Zwischenstand
            // steht am Knopf, sobald jemand gestimmt hat; allein bleibt er schlicht.
            if (raum != null) {
                val stand = if (raum.dienstendeStimmen > 0 && raum.dienstendeSchwelle > 1) {
                    " ${raum.dienstendeStimmen}/${raum.dienstendeSchwelle}"
                } else {
                    ""
                }
                Knopf(
                    "Dienstende$stand",
                    beiDienstende,
                    art = if (raum.dienstendeEigeneStimme) Knopfart.Gefahr else Knopfart.Leise,
                    kompakt = true,
                    zeichenVorn = { Icon(Kopfzeichen.Aus, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }

        // 2 · Wann und wie es steht.
        if (raum != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Uhrfeld(jetzt, raum)
                Lagetafel(raum)
                Zuschauerzaehler(raum)
                raum.wetter?.let { Wetteranzeige(it) }
            }
        }

        // Das Fach — die Werkzeuge mit ihrem Wort. Ein Werkzeug, das einen Dialog
        // öffnet, schließt das Fach gleich mit.
        if (fachOffen) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.BgTief.copy(alpha = 0.6f), Rundung.Klein)
                    .border(1.dp, Farben.Rand, Rundung.Klein)
                    .padding(Abstand.Klein),
            ) {
                Werkzeug("Besatzung", Kopfzeichen.Besatzung) {
                    fachOffen = false
                    beiBesatzung()
                }
                if (beiVerlassen != null) {
                    Werkzeug("Verlassen", Kopfzeichen.Verlassen) {
                        fachOffen = false
                        beiVerlassen()
                    }
                }
            }
        }
    }
}

/** Ein Werkzeug im Fach — Zeichen über Wort, beides eine Zielfläche. */
@Composable
private fun Werkzeug(wort: String, zeichen: ImageVector, beiDruck: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 44.dp)
            .clickable(onClick = beiDruck, role = Role.Button, indication = null, interactionSource = null)
            .padding(Abstand.Winzig),
    ) {
        Icon(zeichen, contentDescription = null, tint = Farben.TextLeise, modifier = Modifier.size(22.dp))
        Text(wort, style = Schrift.Winzig, color = Farben.TextLeise)
    }
}

/** Uhrzeit und Dienstzeit in einem eingelassenen Anzeigefeld. */
@Composable
private fun Uhrfeld(jetzt: Long, raum: Raumzustand) {
    val uhr = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        .format(Instant.ofEpochMilli(jetzt).atZone(java.time.ZoneId.systemDefault()))
    // Nur während sie läuft: In der Lobby und nach dem Ende gibt es keine Dienstzeit.
    val dienst = zeitInMillis(raum.gestartetUm)?.takeIf { raum.laeuft }?.let { start ->
        val s = ((jetzt - start) / 1000).coerceAtLeast(0)
        "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Farben.BgTief, Rundung.Klein)
            .border(1.dp, Farben.Rand, Rundung.Klein)
            .padding(horizontal = Abstand.Klein, vertical = 1.dp),
    ) {
        Text(uhr, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        if (dienst != null) {
            Text("Dienst $dienst", style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextSehrLeise)
        }
    }
}

/**
 * Die Lagetafel — drei Pillen: Punkt, Zahl, Wort. Bei null ist eine Pille grau
 * und tritt zurück; wartet etwas, wird sie rot, und bei „unquittiert" pulsiert der
 * Punkt. „frei" wird amber, wenn kein Fahrzeug mehr frei ist.
 */
@Composable
private fun Lagetafel(raum: Raumzustand) {
    val offen = raum.incidents.count { it.state == "Offen" }
    val unquittiert = raum.vehicles.count { it.alarmOffen }
    val fahrzeuge = raum.vehicles.size
    val frei = raum.vehicles.count { it.status in 1..2 }

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        Lagepille(offen.toString(), "offen", if (offen > 0) Farben.SignalHell else null)
        Lagepille(unquittiert.toString(), "unquittiert", if (unquittiert > 0) Farben.SignalHell else null, puls = unquittiert > 0)
        Lagepille(
            "$frei/$fahrzeuge",
            "frei",
            if (fahrzeuge > 0 && frei == 0) Farben.AmberHell else Farben.GruenHell,
            nurPunkt = fahrzeuge == 0 || frei > 0,
        )
    }
}

@Composable
private fun Lagepille(zahl: String, wort: String, farbe: Color?, puls: Boolean = false, nurPunkt: Boolean = false) {
    val laut = farbe != null && !nurPunkt
    val punktfarbe = farbe ?: Farben.TextSehrLeise
    val deckkraft = if (puls) {
        val takt = rememberInfiniteTransition(label = "lagepuls")
        val wert by takt.animateFloat(1f, 0.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "lagepuls-punkt")
        wert
    } else {
        1f
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(if (laut) punktfarbe.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f), Rundung.Rund)
            .border(1.dp, if (laut) punktfarbe.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f), Rundung.Rund)
            .padding(horizontal = Abstand.Klein, vertical = 1.dp),
    ) {
        Box(Modifier.size(6.dp).alpha(deckkraft).background(punktfarbe, CircleShape))
        Text(zahl, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = if (laut) punktfarbe else Farben.TextLeise)
        Text(wort, style = Schrift.Winzig, color = if (laut) punktfarbe else Farben.TextSehrLeise)
    }
}

/**
 * Das Wetter — es bremst jede Anfahrt im Kreis. Wer nicht weiß, warum das HLF sechs
 * statt vier Minuten braucht, hält es für einen Fehler; deshalb steht die Bremse als
 * Zahl daneben. Leer bei klarer Lage: „Anfahrt ±0 %" ist keine Angabe.
 */
@Composable
private fun Wetteranzeige(w: de.pagerspass.pagerspass.netz.Wetterstand) {
    val zeichen = when (w.aktuell) {
        "Regen" -> "☂"
        "Glaette" -> "❅"
        "Sturm" -> "≈"
        else -> "○"
    }
    val bremse = if (w.bremsfaktor < 1.0) "−${Math.round((1 - w.bremsfaktor) * 100)} %" else null
    val farbe = when {
        w.welle -> Farben.SignalHell
        w.angekuendigtText != null -> Farben.AmberHell
        else -> Farben.TextLeise
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(zeichen, style = Schrift.Klein, color = farbe)
        Text(w.aktuellText.ifBlank { w.aktuell }, style = Schrift.Winzig, color = farbe)
        if (bremse != null) Text("Anfahrt $bremse", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.AmberHell)
        if (w.welle) Text("Unwetterlage", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.SignalHell)
        w.angekuendigtText?.let { Text("⚠ $it in Kürze", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.AmberHell) }
    }
}

/** ISO-8601 mit Zone → Millisekunden; Unsinn wird `null`, nie eine Ausnahme. */
internal fun zeitInMillis(roh: String?): Long? {
    if (roh.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(roh).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { Instant.parse(roh).toEpochMilli() }.getOrNull()
}

// ======================================================================
// Die Telefonanlage
// ======================================================================

/**
 * Die Telefonanlage — wo die Notrufe klingeln. Übertragen aus `Telefonanlage.vue`.
 *
 * <b>Wie es dazu kam.</b> Erst ein Band oben, dann ein Dialog mitten auf dem Schirm,
 * dann ein eigener Reiter „Notruf". Der Dialog deckte Karte und Einsatz zu, der
 * Reiter versteckte das Klingeln hinter einer Zahl. Jetzt ist es ein Gerät in der
 * Ecke unten: Jede klingelnde Leitung hat ihre eigene Taste mit ihrer Restzeit, die
 * älteste steht groß oben mit einem Ring, der abläuft. Angenommen werden kann jede
 * Leitung, nicht nur die älteste — Priorisieren ist die Aufgabe.
 *
 * <b>Eingeklappt bleibt ein Hörer</b>, pulsierend, mit der Zahl der wartenden
 * Anrufe. Der nächste neue Anruf klappt die Anlage von selbst wieder auf.
 *
 * Am Handy steht sie über die ganze Breite, damit die Knöpfe groß bleiben.
 */
@Composable
fun BoxScope.Telefonanlage(
    anrufe: List<Anruf>,
    eingeklappt: Boolean,
    imGespraech: Boolean,
    beiAnnehmen: (String) -> Unit,
    beiAbweisen: (String) -> Unit,
    beiWegklappen: () -> Unit,
    beiAufklappen: () -> Unit,
    unten: Dp = 0.dp,
) {
    if (anrufe.isEmpty()) return
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            jetzt = System.currentTimeMillis()
            delay(250)
        }
    }

    fun rest(a: Anruf): Int {
        val ende = zeitInMillis(a.klingeltBis) ?: return 0
        return ((ende - jetzt + 999) / 1000).toInt().coerceAtLeast(0)
    }

    fun anteil(a: Anruf): Float {
        val ende = zeitInMillis(a.klingeltBis) ?: return 1f
        val anfang = zeitInMillis(a.eingangUm) ?: return 1f
        val gesamt = ende - anfang
        if (gesamt <= 0) return 0f
        return ((ende - jetzt).toFloat() / gesamt).coerceIn(0f, 1f)
    }

    val erster = anrufe.first()
    val restErster = rest(erster)
    // Ohne Ablaufzeit (älterer Server) gibt es keine Uhr — dann ist nichts „knapp".
    val mitUhr = erster.klingeltBis != null
    val knapp = mitUhr && restErster <= 10
    val farbe = if (knapp) Farben.Signal else Farben.Amber

    val lage = Modifier
        .align(Alignment.BottomStart)
        .padding(start = Abstand.Gross, end = Abstand.Gross, bottom = Abstand.Gross + unten)

    if (eingeklappt) {
        val takt = rememberInfiniteTransition(label = "hoerer")
        val gross by takt.animateFloat(1f, 1.08f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "hoerer-puls")
        Box(
            contentAlignment = Alignment.Center,
            modifier = lage
                .size(56.dp * gross)
                .shadow(Erhebung.Alarm, CircleShape)
                .background(farbe, CircleShape)
                .clickable(onClick = beiAufklappen, role = Role.Button, indication = null, interactionSource = null),
        ) {
            Text("☎", style = Schrift.Titel, color = Farben.AufFarbe)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(20.dp)
                    .background(Farben.Signal, CircleShape),
            ) {
                Text("${anrufe.size}", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = lage
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .shadow(Erhebung.Alarm, Rundung.Normal)
            .background(Farben.Flaeche, Rundung.Normal)
            .border(1.dp, Farben.RandHell, Rundung.Normal)
            .drawBehind {
                // Die obere Kante in der Farbe der Anlage — amber, rot wenn es knapp wird.
                drawRect(farbe, size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx()))
            }
            .padding(Abstand.Normal),
    ) {
        // Kopf: Lampe, „Notruf 112", Zahl der Leitungen, Einklappen.
        val lampe = rememberInfiniteTransition(label = "lampe")
        val an by lampe.animateFloat(1f, 0.2f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "lampe-blinken")
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.size(10.dp).alpha(an).background(Farben.Signal, CircleShape))
            Text("NOTRUF 112", style = Schrift.Etikett, color = Farben.TextLeise)
            Text(
                "${anrufe.size} ${if (anrufe.size == 1) "Leitung" else "Leitungen"}",
                style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG),
                color = Farben.TextSehrLeise,
                modifier = Modifier.weight(1f),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = beiWegklappen, role = Role.Button, indication = null, interactionSource = null),
            ) {
                Text("▾", style = Schrift.Gross, color = Farben.TextLeise)
            }
        }

        // Die älteste Leitung groß — mit dem Ring, der abläuft.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val geduld = anteil(erster)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                Canvas(Modifier.size(56.dp)) {
                    val dicke = 4.dp.toPx()
                    drawCircle(Farben.Rand, radius = size.minDimension / 2 - dicke / 2, style = Stroke(dicke))
                    drawArc(
                        color = farbe,
                        startAngle = -90f,
                        sweepAngle = 360f * geduld,
                        useCenter = false,
                        topLeft = Offset(dicke / 2, dicke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - dicke, size.height - dicke),
                        style = Stroke(dicke, cap = StrokeCap.Round),
                    )
                }
                Text("☎", style = Schrift.Titel, color = farbe)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Notruf ${erster.nummer}", style = Schrift.Gross, color = Farben.Text)
                SehrLeise("Anrufer wartet in der Leitung")
            }
            if (mitUhr) {
                Text("$restErster s", style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = farbe)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            Knopf("✕  Abweisen", { beiAbweisen(erster.id) }, art = Knopfart.Gefahr, modifier = Modifier.weight(1f))
            Knopf(
                "☎  Annehmen",
                { beiAnnehmen(erster.id) },
                art = Knopfart.Haupt,
                aktiv = !imGespraech,
                modifier = Modifier.weight(1f),
            )
        }
        if (imGespraech) SehrLeise("Erst das laufende Gespräch beenden.")

        // Die weiteren Leitungen als Tasten — jede einzeln annehmbar.
        anrufe.drop(1).forEach { a ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.BgTief, Rundung.Klein)
                    .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
            ) {
                Box(Modifier.size(8.dp).alpha(an).background(Farben.Signal, CircleShape))
                Text("Notruf ${a.nummer}", style = Schrift.Klein, color = Farben.Text)
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(Farben.Rand, Rundung.Rund),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(anteil(a))
                            .height(4.dp)
                            .background(Farben.Amber, Rundung.Rund),
                    )
                }
                if (a.klingeltBis != null) Text("${rest(a)} s", style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextLeise)
                Zeichenknopf({ beiAbweisen(a.id) }, "Notruf ${a.nummer} abweisen", art = Knopfart.Gefahr, kompakt = true) {
                    Text("✕", style = Schrift.Normal)
                }
                Zeichenknopf({ beiAnnehmen(a.id) }, "Notruf ${a.nummer} annehmen", art = Knopfart.Haupt, aktiv = !imGespraech, kompakt = true) {
                    Text("☎", style = Schrift.Normal)
                }
            }
        }
    }
}

// ======================================================================
// Rückfragen am Ausgang
// ======================================================================

/** „Leitstelle verlassen?" — der Platz wird frei, die Schicht läuft weiter. */
@Composable
fun Leitstelleverlassenblende(beiBleiben: () -> Unit, beiVerlassen: () -> Unit) {
    Blende(
        titel = "Leitstelle verlassen?",
        beiSchliessen = beiBleiben,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("In der Leitstelle bleiben", beiBleiben)
            Knopf("Leitstelle verlassen", beiVerlassen, art = Knopfart.Alarm)
        },
    ) {
        Text(
            "Dein Leitstellenplatz wird frei. Die laufende Schicht und alle Einsätze bleiben für die " +
                "übrigen Besatzungen bestehen.",
            style = Schrift.Normal,
            color = Farben.Text,
        )
        SehrLeise(
            "Was du bis hierher gefahren hast, wird beim Aussteigen gutgeschrieben. Was danach " +
                "passiert, zählt für dich nicht mehr.",
        )
    }
}

/**
 * „Dienst beenden?" — nur, wenn die eigene Stimme die letzte ist, die fehlt. Sonst
 * ist der Druck eine Stimme unter mehreren und braucht keine Rückfrage.
 */
@Composable
fun Dienstendeblende(raum: Raumzustand, beiWeiter: () -> Unit, beiBeenden: () -> Unit) {
    val zuKurz = zeitInMillis(raum.gestartetUm)?.let { System.currentTimeMillis() - it < 5 * 60_000 } == true
    Blende(
        titel = "Dienst beenden?",
        beiSchliessen = beiWeiter,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Weiter im Dienst", beiWeiter)
            Knopf("Dienst beenden", beiBeenden, art = Knopfart.Alarm)
        },
    ) {
        Text(
            if (raum.dienstendeSchwelle > 1) {
                "Deine Stimme ist die letzte, die fehlt — die Schicht endet damit für alle im Raum."
            } else {
                "Die Schicht endet, und alle Einsätze werden abgerechnet."
            },
            style = Schrift.Normal,
            color = Farben.Text,
        )
        if (zuKurz) {
            Text(
                "Die Schicht läuft noch keine fünf Minuten und bringt bis dahin keine Punkte.",
                style = Schrift.MonoKlein,
                color = Farben.SignalHell,
            )
        }
    }
}

// ======================================================================
// Die Zeichen des Kopfs — dieselben Pfade wie im Web
// ======================================================================

private object Kopfzeichen {
    /** Hörsprechgarnitur: Bügel, zwei Muscheln, Mikrofonarm. */
    val Garnitur = strich(
        "garnitur",
        "M4.5 14v-2a7.5 7.5 0 0 1 15 0v2",
        "M5.1 13.5h2.8a.6.6 0 0 1 .6.6v4.8a.6.6 0 0 1-.6.6H5.1a1.6 1.6 0 0 1-1.6-1.6v-2.8a1.6 1.6 0 0 1 1.6-1.6z",
        "M16.5 13.5h2.4a1.6 1.6 0 0 1 1.6 1.6v2.8a1.6 1.6 0 0 1-1.6 1.6h-2.4z",
        "M18.5 19.5c0 1.4-1.6 2.5-4 2.5h-1.5",
    )

    /** Hörer mit Plus: ein Notruf, den die Leitstelle selbst anlegt. */
    val HoererPlus = strich(
        "hoererplus",
        "M15.5 21a12 12 0 0 1-12-12 2 2 0 0 1 2-2h2.2a1 1 0 0 1 1 .8l.6 2.6a1 1 0 0 1-.5 1.1l-1.4.8a10 10 0 0 0 4.5 4.5l.8-1.4a1 1 0 0 1 1.1-.5l2.6.6a1 1 0 0 1 .8 1V19a2 2 0 0 1-2 2Z",
        "M18 3v6",
        "M15 6h6",
    )

    /** Würfel mit fünf Augen. */
    val Wuerfel = strich(
        "wuerfel",
        "M7 3.5h10a3.5 3.5 0 0 1 3.5 3.5v10a3.5 3.5 0 0 1-3.5 3.5H7a3.5 3.5 0 0 1-3.5-3.5V7A3.5 3.5 0 0 1 7 3.5z",
        "M8.5 8.4v.2", "M15.5 8.4v.2", "M12 11.9v.2", "M8.5 15.4v.2", "M15.5 15.4v.2",
        staerke = 2.2f,
    )

    /** Zwei Schieberegler: das Pult. */
    val Pult = strich("pult", "M4 7h9M17 7h3M4 17h3M11 17h9", "M15 5a2 2 0 1 0 0 4 2 2 0 0 0 0-4z", "M9 15a2 2 0 1 0 0 4 2 2 0 0 0 0-4z")

    /** Zwei Köpfe: wer auf welchem Fahrzeug sitzt. */
    val Besatzung = strich(
        "besatzung",
        "M9.5 5.3a3.2 3.2 0 1 0 0 6.4 3.2 3.2 0 0 0 0-6.4z",
        "M3.8 19.5a5.7 5.7 0 0 1 11.4 0",
        "M16 5.8a3.2 3.2 0 0 1 0 5.4",
        "M17.4 14.6a5.7 5.7 0 0 1 2.8 4.9",
    )

    /** Tür mit Pfeil: den Platz verlassen. */
    val Verlassen = strich("verlassen", "M10 4H5a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h5", "M14 8l4 4-4 4", "M8 12h10")

    /** Ausschalter: Bogen mit Strich — das Ende der Schicht. */
    val Aus = strich("aus", "M12 3.5v7.5", "M7.4 6.6a7.5 7.5 0 1 0 9.2 0")
}

private fun strich(name: String, vararg pfade: String, staerke: Float = 1.6f): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        pfade.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = staerke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
