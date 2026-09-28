package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.pagerspass.pagerspass.mobil.Tonwahl
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Regler
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.Zeichenknopf
import de.pagerspass.pagerspass.ui.schmuck.Melderkatalog
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Kopf, Bänder und Blenden des Fahrzeugs — der obere Rand von `FahrzeugView.vue`.
 *
 * Im Kopf steht, was man im Dienst dauernd braucht und nie suchen will: die
 * Statusmarke mit dem Zeichen der Taste, die man dafür gedrückt hat, Rufname,
 * Statustext und Typ — und daneben die Schalter der Bedienung: Melderbauart,
 * Tonregler, Funkbegleiter, Zuschauer, Dienstende und Aussteigen.
 */

// ---------------------------------------------------------------- FMS-Zeichen

/**
 * Das Strichzeichen zu einem FMS-Status — `FmsZeichen.vue`.
 *
 * <b>Status 7 und 8 kennen die Organisation</b>: Beim Rettungsdienst heißen sie
 * „Patient aufgenommen" und „Am Transportziel" und tragen Trage und Klinik; bei
 * allen anderen „Einsatzgebunden" und „Bedingt verfügbar" mit Kettenglied und
 * halbem Kreis. Gezeichnet auf dem 24er-Raster wie jedes Zeichen der App.
 */
fun fmsZeichen(status: Int, organisation: String?): ImageVector {
    val rd = organisation == "Rettungsdienst"
    val pfade: List<String> = when (status) {
        0 -> listOf(
            rundrechteck(7f, 3f, 6f, 10f, 3f),
            "M4.5 11a5.5 5.5 0 0 0 11 0", "M10 16.5V21",
            "M19.5 4.5 23.6 12.5 15.4 12.5 Z", "M19.5 7.4v2.6", "M19.5 11.2v.4",
        )
        1 -> listOf("M12 10.5v10", "M8.5 20.5h7", "M8.4 9.4a5 5 0 0 1 7.2 0", "M5.6 6.4a9 9 0 0 1 12.8 0")
        2 -> listOf("M3.5 10.5 12 4l8.5 6.5", "M5.5 9.8V20h13V9.8", "M9.5 20v-5.5h5V20")
        3 -> listOf(
            "M3 12h11", "M10.5 8.5 14 12l-3.5 3.5",
            "M19 5.5a3 3 0 0 0-3 3c0 2.2 3 5.5 3 5.5s3-3.3 3-5.5a3 3 0 0 0-3-3Z", "M19 18.5h2",
        )
        4 -> listOf(
            "M12 2.5c-3.6 0-6.5 2.9-6.5 6.5 0 4.8 6.5 12 6.5 12s6.5-7.2 6.5-12c0-3.6-2.9-6.5-6.5-6.5Z",
            "m9.2 9.2 2 2 3.6-3.6",
        )
        5 -> listOf(rundrechteck(9f, 3f, 6f, 10f, 3f), "M6.5 11a5.5 5.5 0 0 0 11 0", "M12 16.5V21", "M9 21h6")
        6 -> listOf(kreis(12f, 12f, 8.5f), "m6 6 12 12")
        7 -> if (rd) {
            listOf("M3.5 14.5h17", "M6 14.5v3.5", "M18 14.5v3.5", kreis(8f, 8.5f, 2f), "M11 11.5h7", "M4.5 11.5h2.5")
        } else {
            listOf(
                "M9.8 14.2a3.5 3.5 0 0 1 0-5l2.4-2.4a3.5 3.5 0 1 1 5 5l-1.2 1.2",
                "M14.2 9.8a3.5 3.5 0 0 1 0 5l-2.4 2.4a3.5 3.5 0 1 1-5-5l1.2-1.2",
            )
        }
        8 -> if (rd) {
            listOf("M4.5 20.5V8.2L12 3.5l7.5 4.7v12.3", "M3 20.5h18", "M12 9.5v5", "M9.5 12h5")
        } else {
            listOf(kreis(12f, 12f, 8.5f), "M12 3.5v17")
        }
        else -> emptyList()
    }
    return ImageVector.Builder(
        name = "fms-$status",
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
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
}

private fun kreis(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun rundrechteck(x: Float, y: Float, b: Float, h: Float, r: Float): String =
    "M${x + r} ${y}H${x + b - r}A$r $r 0 0 1 ${x + b} ${y + r}V${y + h - r}" +
        "A$r $r 0 0 1 ${x + b - r} ${y + h}H${x + r}A$r $r 0 0 1 $x ${y + h - r}V${y + r}A$r $r 0 0 1 ${x + r} ${y}z"

/** Das Zeichen, eingefärbt — dort, wo es steht. */
@Composable
fun FmsZeichen(status: Int, organisation: String?, farbe: Color, groesse: Dp = 18.dp) {
    val zeichen = remember(status, organisation) { fmsZeichen(status, organisation) }
    Icon(zeichen, contentDescription = null, tint = farbe, modifier = Modifier.size(groesse))
}

// ------------------------------------------------------------------ Kopf

/** Was der Kopf können muss — die Griffe dahinter liegen an der Seite. */
class Kopfgriffe(
    val dienstende: () -> Unit,
    val verlassen: () -> Unit,
    val hilfe: () -> Unit,
    val tonregler: () -> Unit,
    val begleiter: () -> Unit,
)

/**
 * Der Kopf des Fahrzeugs.
 *
 * <b>Zwei Reihen und nicht eine.</b> Im Web steht alles in einer Leiste; am Handy
 * brach die schon mit einer Zahl am Dienstende-Knopf so um, dass der Weg hinaus
 * unter die Kante rutschte. Oben stehen Status und Rufname mit dem Aussteigen,
 * darunter die Schalter der Bedienung.
 */
@Composable
fun Fahrzeugkopf(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    oben: Dp,
    tonwahl: Tonwahl,
    griffe: Kopfgriffe,
) {
    val orgfarbe = Rundentexte.organisationFarbe(meins.organisation)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Farben.FlaecheHoch, Farben.Flaeche)))
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(Farben.Rand, Offset(0f, size.height - strich / 2f), Offset(size.width, size.height - strich / 2f), strich)
                // Die Organisation als Kante links — dieselbe Farbe wie auf der Karte.
                drawRect(orgfarbe, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height))
            }
            .padding(top = oben)
            .padding(start = Abstand.Gross, end = Abstand.Normal, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Die Statusmarke: Zeichen und Zahl in der Farbe des Status.
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(fmsFarbe(meins.status), Rundung.Klein)
                    .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
            ) {
                FmsZeichen(meins.status, meins.organisation, Farben.AufFarbe)
                Text(meins.status.toString(), style = Schrift.MarkeZahl, color = Farben.AufFarbe)
            }

            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                Text(
                    text = meins.funkrufname.ifBlank { "—" },
                    style = Schrift.MonoNormal.copy(fontSize = Schrift.GROSS),
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    Text(
                        meins.statusText.ifBlank { "…" },
                        style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                        color = fmsFarbe(meins.status),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    SehrLeise("· ${meins.typ}")
                }
            }

            Zeichenknopf(
                beiDruck = griffe.verlassen,
                beschreibung = "Fahrzeug verlassen",
                art = Knopfart.Leise,
                kompakt = true,
            ) {
                Icon(TUER_HINAUS, contentDescription = null, tint = Farben.Text, modifier = Modifier.size(20.dp))
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Die Melderbauart — bleibt stehen, auch wenn ein Handy gekoppelt ist:
            // Die Wahl wird zum Begleiter gespiegelt, der Schalter ist ihr einziger Weg.
            Segment(
                seiten = Tonwahl.BAUARTEN,
                gewaehlt = tonwahl.bauart,
                beiWahl = { tonwahl.bauartSetzen(it) },
                aufschrift = { if (it == "dme") "DME" else if (it == "app") "App" else "Im Funk" },
            )
            Box(Modifier.weight(1f))
            Zuschauerzaehler(raum)
            Zeichenknopf(
                beiDruck = { tonwahl.stummSetzen(!tonwahl.stumm) },
                beschreibung = if (tonwahl.stumm) "Stumm aufheben" else "Alles außer dem Melder stummschalten",
                art = if (tonwahl.stumm) Knopfart.Gefahr else Knopfart.Leise,
                kompakt = true,
            ) {
                Icon(if (tonwahl.stumm) LAUTSPRECHER_AUS else LAUTSPRECHER, contentDescription = null, tint = Farben.Text, modifier = Modifier.size(18.dp))
            }
            Zeichenknopf(beiDruck = griffe.tonregler, beschreibung = "Toneinstellungen", art = Knopfart.Leise, kompakt = true) {
                Icon(REGLER, contentDescription = null, tint = Farben.Text, modifier = Modifier.size(18.dp))
            }
            if (raum.laeuft) {
                Zeichenknopf(beiDruck = griffe.begleiter, beschreibung = "Handy als Funkgerät und Melder koppeln", art = Knopfart.Leise, kompakt = true) {
                    Icon(BEGLEITER, contentDescription = null, tint = Farben.Text, modifier = Modifier.size(18.dp))
                }
                val stimmstand = raum.dienstendeSchwelle > 1 && raum.dienstendeStimmen > 0
                Zeichenknopf(
                    beiDruck = griffe.dienstende,
                    beschreibung = dienstendeTitel(raum),
                    art = if (raum.dienstendeEigeneStimme) Knopfart.Gefahr else Knopfart.Leise,
                    kompakt = true,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(AUSSCHALTER, contentDescription = null, tint = Farben.Text, modifier = Modifier.size(18.dp))
                        if (stimmstand) {
                            Text("${raum.dienstendeStimmen}/${raum.dienstendeSchwelle}", style = Schrift.MonoKlein, color = Farben.Text)
                        }
                    }
                }
            }
        }
    }
}

/** Die Aufschrift des Dienstende-Knopfs — `dienstendeTitel` im Web. */
fun dienstendeTitel(raum: Raumzustand): String {
    if (raum.istPruefung) return "Prüfung abschließen"
    if (raum.dienstendeSchwelle <= 1) return "Dienst beenden"
    val stand = "${raum.dienstendeStimmen} von ${raum.dienstendeSchwelle}"
    return if (raum.dienstendeEigeneStimme) {
        "Du hast fürs Dienstende gestimmt ($stand) — noch einmal drücken nimmt die Stimme zurück"
    } else {
        "Fürs Dienstende stimmen ($stand nötig)"
    }
}

/** Ob die Schicht noch keine fünf Minuten läuft — dann bringt sie keine Punkte. */
fun dienstZuKurz(raum: Raumzustand, jetzt: Long = System.currentTimeMillis()): Boolean {
    val start = raum.gestartetUm?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: return false
    return raum.laeuft && jetzt - start < 5 * 60 * 1000
}

private fun strichzeichen(name: String, vararg pfade: String): ImageVector =
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
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** Tür mit Pfeil hinaus — das Spiegelbild von „Einer Runde beitreten". */
private val TUER_HINAUS = strichzeichen("tuer", "M10.5 4.5H6a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h4.5", "M20 12h-9.5", "M16.5 8.5 20 12l-3.5 3.5")

/** Ausschalter: Bogen mit Strich — dasselbe Zeichen wie in der Leitstelle. */
private val AUSSCHALTER = strichzeichen("aus", "M12 3.5v7.5", "M7.4 6.6a7.5 7.5 0 1 0 9.2 0")

private val LAUTSPRECHER = strichzeichen("laut", "M11.5 5 7 9H3.5v6H7l4.5 4Z", "M15.2 9.2a4 4 0 0 1 0 5.6", "M17.9 6.6a7.8 7.8 0 0 1 0 10.8")

private val LAUTSPRECHER_AUS = strichzeichen("stumm", "M11.5 5 7 9H3.5v6H7l4.5 4Z", "m16.5 9.5 5 5m0-5-5 5")

private val REGLER = strichzeichen(
    "regler",
    "M5 5v5m0 4v5M12 5v9m0 4v1M19 5v2m0 4v8",
    "M3.1 12a1.9 1.9 0 1 0 3.8 0 1.9 1.9 0 1 0-3.8 0z",
    "M10.1 16a1.9 1.9 0 1 0 3.8 0 1.9 1.9 0 1 0-3.8 0z",
    "M17.1 9a1.9 1.9 0 1 0 3.8 0 1.9 1.9 0 1 0-3.8 0z",
)

private val BEGLEITER = strichzeichen(
    "begleiter",
    "M10 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
    "M10.5 6h3M11 18.5h2",
    "M6 9.5a4.5 4.5 0 0 0 0 5M3.6 7.6a8 8 0 0 0 0 8.8",
    "M18 9.5a4.5 4.5 0 0 1 0 5M20.4 7.6a8 8 0 0 1 0 8.8",
)

// ----------------------------------------------------------------- Bänder

/**
 * Die Hinweisbänder unter dem Kopf — verwaiste Leitstelle, Wiederherstellung,
 * Notarzt, Besatzungslücken, Sprechwunsch.
 *
 * <b>Der Notweg ist eine Zeile in der Fläche</b>, kein schwebender Knopf: Ein
 * Fehlgriff hieße hier „Leitstelle übernommen", nicht „Fenster auf".
 */
@Composable
fun ColumnScope.Fahrzeugbaender(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    jetzt: Long,
    beiLeitstelleUebernehmen: () -> Unit,
) {
    // Die zwei Minuten spiegeln `Uebergabe.Schonfrist` am Server.
    val verwaist = raum.leitstelleVerwaistSeit
        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?.let { jetzt - it >= 2 * 60 * 1000 } == true
    if (verwaist) {
        Band(
            "Die Leitstelle ist verwaist — tippen, um sie zu übernehmen",
            Farben.AmberTief,
            beiDruck = beiLeitstelleUebernehmen,
        )
    }

    meins.wiederherstellungBis
        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?.let { ((it - jetzt) / 1000.0).roundToInt() }
        ?.takeIf { it > 0 }
        ?.let { rest ->
            val wort = if (rest < 60) "$rest s" else "${(rest + 59) / 60} min"
            Band(
                "${meins.ausserDienstGrund ?: "Wiederherstellung"} — noch $wort, dann wieder einsatzbereit",
                Farben.FmsDefekt.copy(alpha = 0.55f),
            )
        }

    meins.notarztBei?.let {
        Band("Notarzt begleitet $it — bis zur Übergabe ohne Notarzt unterwegs", Farben.FmsDefekt.copy(alpha = 0.55f))
    }

    meins.besatzungsluecken.forEach { l ->
        val was = if (l.soll > 1) "${l.name} ${l.ist}/${l.soll}" else "ohne ${l.name}"
        Band("⚑ $was — diese Arbeit kann euer Fahrzeug heute nicht übernehmen", Farben.SignalTief)
    }

    if (meins.status == 5 || meins.status == 0) {
        Band("◉ Sprechwunsch angemeldet — die Leitstelle meldet sich", Farben.FmsSprechwunsch.copy(alpha = 0.6f))
    }
}

@Composable
private fun Band(text: String, farbe: Color, beiDruck: (() -> Unit)? = null) {
    Text(
        text = text,
        style = Schrift.MonoKlein,
        color = Farben.Text,
        modifier = Modifier
            .fillMaxWidth()
            .background(farbe, Rundung.Klein)
            .then(if (beiDruck != null) Modifier.clickable(role = Role.Button, onClick = beiDruck) else Modifier)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    )
}

// ----------------------------------------------------------------- Blenden

/**
 * Die Rückfrage vor dem Aussteigen — `VerlassenDialog.vue`. Aussteigen ist nicht
 * rückgängig zu machen; was bis dahin gefahren wurde, ist dagegen sicher.
 */
@Composable
fun VerlassenBlende(
    meins: Rundenfahrzeug,
    einsatz: Einsatz?,
    beiBleiben: () -> Unit,
    beiAussteigen: () -> Unit,
) {
    Blende(
        titel = "Fahrzeug verlassen?",
        beiSchliessen = beiBleiben,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Im Fahrzeug bleiben", beiBleiben, art = Knopfart.Leise)
            Knopf("Aussteigen", beiAussteigen, art = Knopfart.Alarm)
        },
    ) {
        Etikett(meins.funkrufname)
        Text(
            "Dein Platz wird frei, und ${meins.funkrufname} fällt aus der laufenden Schicht — " +
                "die Leitstelle kann damit nicht mehr planen.",
            style = Schrift.Normal,
            color = Farben.TextLeise,
        )
        if (einsatz != null && !einsatz.abgeschlossen) {
            Text(
                "Du bist auf ${einsatz.einsatznummer} gebunden — der Einsatz läuft noch.",
                style = Schrift.MonoKlein,
                color = Farben.SignalHell,
            )
        }
        SehrLeise(
            "Was du bis hierher gefahren hast, wird beim Aussteigen gutgeschrieben. " +
                "Was danach passiert, zählt für dich nicht mehr.",
        )
    }
}

/**
 * Die Rückfrage vor dem Dienstende — `DienstendeDialog.vue`. Nur, wenn dieser Druck
 * die Schicht wirklich beendet: allein im Dienst oder die letzte fehlende Stimme.
 */
@Composable
fun DienstendeBlende(
    raum: Raumzustand,
    beiWeiter: () -> Unit,
    beiBeenden: () -> Unit,
) {
    val pruefung = raum.istPruefung
    val zuKurz = remember { dienstZuKurz(raum) }
    Blende(
        titel = if (pruefung) "Prüfung abschließen?" else "Dienst beenden?",
        beiSchliessen = beiWeiter,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Weiter im Dienst", beiWeiter, art = Knopfart.Leise)
            Knopf(if (pruefung) "Prüfung abschließen" else "Dienst beenden", beiBeenden, art = Knopfart.Alarm)
        },
    ) {
        Etikett("Dienstende")
        Text(
            when {
                pruefung -> "Die Prüfung wird abgeschlossen und bewertet. Weiterfahren geht danach nicht mehr."
                raum.dienstendeSchwelle > 1 ->
                    "Deine Stimme ist die letzte, die fehlt — die Schicht endet damit für alle im Raum."
                else -> "Die Schicht endet, und alle Einsätze werden abgerechnet."
            },
            style = Schrift.Normal,
            color = Farben.TextLeise,
        )
        when {
            pruefung && !raum.pruefungAbschlussbereit -> Text(
                "Noch nicht alle Prüfungslagen sind vollständig abgearbeitet.",
                style = Schrift.MonoKlein,
                color = Farben.SignalHell,
            )
            zuKurz -> Text(
                "Die Schicht läuft noch keine fünf Minuten und bringt bis dahin keine Punkte.",
                style = Schrift.MonoKlein,
                color = Farben.SignalHell,
            )
        }
    }
}

/**
 * Der Tonregler — `Tonregler.vue`: lauter, leiser, still, und die drei Schalter, die
 * man während einer Schicht wirklich anfasst.
 */
@Composable
fun FahrzeugTonregler(
    tonwahl: Tonwahl,
    einzelrufZulassen: Boolean,
    beiEinzelrufZulassen: (Boolean) -> Unit,
    tonname: String,
    beiSchliessen: () -> Unit,
) {
    Blende(
        titel = "Toneinstellungen",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = { Knopf("Fertig", beiSchliessen, art = Knopfart.Haupt) },
    ) {
        Schalterzeile(
            titel = "Alles außer dem Melder stumm",
            an = tonwahl.stumm,
            beiWechsel = { tonwahl.stummSetzen(it) },
        )
        Etikett("Lautstärke")
        Regler(
            wert = (tonwahl.melder * 100).roundToInt(),
            beiAenderung = { tonwahl.melderSetzen(it / 100f) },
            von = 0,
            bis = 100,
            etikett = "Melder",
        )
        Regler(
            wert = (tonwahl.funk * 100).roundToInt(),
            beiAenderung = { tonwahl.funkSetzen(it / 100f) },
            von = 0,
            bis = 100,
            etikett = "Funk",
            aktiv = !tonwahl.stumm,
        )
        SehrLeise("Stimmen und Gerätetöne des Funkgeräts.")
        Regler(
            wert = (tonwahl.umgebung * 100).roundToInt(),
            beiAenderung = { tonwahl.umgebungSetzen(it / 100f) },
            von = 0,
            bis = 100,
            etikett = "Umgebung",
            aktiv = !tonwahl.stumm,
        )
        Hakenzeile("Einzelrufe von Fahrzeugen annehmen", einzelrufZulassen, beiEinzelrufZulassen)
        SehrLeise("Der Ruhe-Schalter des eigenen Platzes — die Leitstelle kommt immer durch.")
        Hakenzeile("Durchsage der Leitstelle beim Alarm", tonwahl.durchsage, { tonwahl.durchsageSetzen(it) })
        Hakenzeile("Einsatzstellenfunk (DMO) mithören", !tonwahl.dmoStumm, { tonwahl.dmoStummSetzen(!it) })
        SehrLeise("Alarmton: $tonname — zu ändern im Konto unter „Profil → Bearbeiten → Dein Melder“.")
    }
}

/**
 * Der Handy-Funkbegleiter — `BegleiterDialog.vue`. Ein QR-Code, mit dem ein zweites
 * Handy Funkgerät und Melder dieses Platzes übernimmt. Ohne Abo die Werbung dafür.
 */
@Composable
fun FahrzeugBegleiterBlende(
    premium: Boolean,
    gekoppelt: Boolean,
    beiZugang: suspend () -> Result<String>,
    beiPremium: () -> Unit,
    beiSchliessen: () -> Unit,
) {
    if (!premium) {
        Abosperre(
            titel = "Dein Handy wird zum Melder",
            satz = "Scanne einen QR-Code, und das Handy neben dir ist Funkgerät und Piepser deines " +
                "Platzes — ohne zweite Anmeldung, ohne zweites Konto.",
            punkte = listOf(
                "Der Melder piepst am Handy, auch bei dunklem Bildschirm.",
                "Sprechen und Hören über ein echtes Handfunkgerät — fünf Ausführungen zur Wahl.",
                "Der Funkbereich hier verschwindet dafür und macht der Karte Platz.",
            ),
            beiSchliessen = beiSchliessen,
            beiPremium = {
                beiSchliessen()
                beiPremium()
            },
        )
        return
    }

    var link by remember { mutableStateOf<String?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var kopiert by remember { mutableStateOf(false) }
    val ablage = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        beiZugang()
            .onSuccess { link = it }
            .onFailure { fehler = it.message ?: "Der QR-Code konnte nicht erzeugt werden." }
    }

    Blende(
        titel = "Handy-Funkbegleiter",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise) },
    ) {
        Etikett("Premium")
        Text(
            "Mit dem Handy scannen: Funkgerät, Funkchat und Melder öffnen sich direkt — ohne " +
                "erneute Anmeldung. Der Zugang gilt nur für deinen Platz in dieser Runde.",
            style = Schrift.Normal,
            color = Farben.TextLeise,
        )
        val l = link
        when {
            fehler != null -> Text(fehler.orEmpty(), style = Schrift.MonoKlein, color = Farben.SignalHell)
            l == null -> SehrLeise("QR-Code wird erzeugt …")
            else -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) { Qrbild(l, 220.dp) }
                Text(
                    if (gekoppelt) "Handy gekoppelt ✓" else "Warte auf die Kopplung …",
                    style = Schrift.MonoKlein,
                    color = if (gekoppelt) Farben.GruenHell else Farben.TextSehrLeise,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Knopf(
                    if (kopiert) "Link kopiert ✓" else "Direkten Link kopieren",
                    {
                        ablage.setText(AnnotatedString(l))
                        kopiert = true
                    },
                    art = Knopfart.Haupt,
                    breit = true,
                )
                SehrLeise("Der Link verfällt nach acht Stunden. Teile ihn nicht weiter.")
            }
        }
    }
}

// --------------------------------------------------------------- Alarm-App

/**
 * Die Alarm-App — `AlarmAppOverlay.vue`: der Alarm über der ganzen Ansicht, mit
 * Laufzeit, Einsatzort, Meldebild, der Meldung der Leitstelle und den Einheiten.
 *
 * <b>Wegklappen gibt es nur hier.</b> Quittieren stellt nur den Ton ab; die Meldung
 * bleibt bis Status 3. Am DME stört das niemanden, die App dagegen liegt über allem —
 * bliebe sie liegen, käme die Besatzung an Funk und Statustasten nicht heran.
 */
@Composable
fun AlarmAppUeberlagerung(
    alarm: Alarmmeldung,
    quittiert: Boolean,
    eigenerRufname: String,
    jetzt: Long,
    beiQuittieren: () -> Unit,
    beiAusruecken: () -> Unit,
) {
    var weggeklappt by remember(alarm) { mutableStateOf(false) }
    if (weggeklappt) return

    val beginn = runCatching { Instant.parse(alarm.zeit).toEpochMilli() }.getOrNull() ?: jetzt
    val sek = ((jetzt - beginn) / 1000).coerceAtLeast(0)
    val laufzeit = "${sek / 60}:${(sek % 60).toString().padStart(2, '0')}"
    val prio = alarm.prioritaet
    val balken = when {
        quittiert -> Farben.FlaecheAktiv
        prio >= 3 -> Farben.Signal
        prio <= 1 -> Farben.Blau
        else -> Farben.FmsAnfahrt
    }

    Dialog(
        onDismissRequest = { if (quittiert) weggeklappt = true },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Farben.BgTief)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(balken).padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
            ) {
                Box(Modifier.size(10.dp).background(Color.White, CircleShape))
                Text("${if (quittiert) "QUITTIERT" else "ALARM"} · $laufzeit", style = Schrift.MonoNormal, color = Color.White)
                if (prioZeichen(prio).isNotEmpty()) Text(prioZeichen(prio), style = Schrift.MonoNormal, color = Color.White)
                Box(Modifier.weight(1f))
                Text(alarm.schleife, style = Schrift.MonoKlein, color = Color.White, maxLines = 1)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Abstand.Gross),
            ) {
                Text(alarm.stichwort, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
                Text(alarm.stichwortText, style = Schrift.Schlagzeile, color = Farben.Text)
                Appkarte("Einsatzort") {
                    Text(alarm.adresse, style = Schrift.Gross, color = Farben.Text)
                    alarm.ortsteil?.let { Leise(it) }
                }
                Appkarte("Meldebild") { Text(alarm.meldebild, style = Schrift.Normal, color = Farben.Text) }
                alarm.zusatztext?.takeIf { it.isNotBlank() }?.let { z ->
                    Appkarte("Meldung der Leitstelle", Farben.AmberTief) { Text(z, style = Schrift.Normal, color = Farben.Text) }
                }
                Appkarte("Alarmierte Einheiten") {
                    alarm.einheiten.forEach { e ->
                        Text(
                            e,
                            style = Schrift.MonoKlein.copy(fontWeight = if (e == eigenerRufname) FontWeight.Bold else FontWeight.Normal),
                            color = if (e == eigenerRufname) Farben.AmberHell else Farben.TextLeise,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                modifier = Modifier.fillMaxWidth().padding(Abstand.Gross),
            ) {
                if (!quittiert) {
                    Knopf("Nur quittieren", beiQuittieren, Modifier.weight(1f), art = Knopfart.Leise)
                } else {
                    Knopf("Wegklappen", { weggeklappt = true }, Modifier.weight(1f), art = Knopfart.Leise)
                }
                Knopf("Rücke aus", beiAusruecken, Modifier.weight(1f), art = Knopfart.Alarm)
            }
        }
    }
}

@Composable
private fun Appkarte(etikett: String, rand: Color = Farben.Rand, inhalt: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier.fillMaxWidth().flaeche(randfarbe = rand).padding(Abstand.Normal),
    ) {
        Etikett(etikett)
        inhalt()
    }
}

/** Der Name des Alarmtons — für die Zeile unter dem Tonregler. */
fun alarmtonName(id: String): String = Melderkatalog.TOENE.firstOrNull { it.id == id }?.name ?: "Eigener Ton"
