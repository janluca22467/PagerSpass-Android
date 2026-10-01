package de.pagerspass.pagerspass.ansichten

import android.content.Context
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.mobil.Signalhorn
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import kotlinx.coroutines.delay

/**
 * Das Sondersignal-Bedienteil — das Kästchen am Armaturenbrett, mit dem die Besatzung
 * Blaulicht, Signalhorn, Heckwarner und Verkehrsleitanzeige schaltet. Übertragen aus
 * `Sondersignal.vue` (Web v6).
 *
 * <b>Der Aufbau ist der eines echten Bedienteils</b>, von oben nach unten: die
 * Heckansicht (zwei Blaulichtbalken, dazwischen die Matrix, darunter die
 * Heckwarnleiste), die Frontblitzer, die Matrixwahl, die Schalter, die Hornwahl und
 * ganz unten die Lautstärke. Die Anzeige oben ist die Rückmeldung: Man sieht am
 * Blinken, was läuft, ohne die Aufschriften zu lesen.
 *
 * <b>Genau ein Knopf ist mit dem Server verbunden: das Blaulicht.</b> Nach § 38 StVO
 * gibt Blaulicht mit Horn das Wegerecht; im Spiel hängt daran die Fahrpriorität. Horn,
 * Heckwarner und Matrix laufen lokal auf diesem Gerät — sonst wäre die Hornwahl ein
 * Geschwindigkeitsregler.
 *
 * <b>Und es zeigt, was das Fahrzeug hat.</b> Ein Hubschrauber hat kein Martinshorn,
 * ein Krad keine Heckwarnanlage, Druckluft hat nur, wer eine Druckluftanlage hat. Was
 * fehlt, steht trotzdem da — gesperrt und mit dem Grund.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.SondersignalBedienteil(meins: Rundenfahrzeug, beiSondersignal: (Boolean) -> Unit) {
    val aus = remember(meins.typ, meins.organisation, meins.istLuftfahrzeug) { ausstattungVon(meins) }
    if (!aus.blaulicht) return
    val zusammenhang = LocalContext.current
    val pult = remember { Pultstand.lesen(zusammenhang) }

    val polizei = meins.organisation == "Polizei"
    val bilder = if (polizei) MATRIX_POLIZEI else MATRIX_SICHERUNG
    // Wechselt die Organisation, verfällt das gewählte Bild — „BITTE FOLGEN" auf dem HLF
    // wäre genau das Bild, das die getrennten Listen verhindern sollen.
    var matrixAn by remember(polizei) { mutableStateOf(false) }
    var matrixBild by remember(polizei) { mutableStateOf("") }
    var matrixOffen by remember { mutableStateOf(false) }
    var matrixText by remember { mutableStateOf("") }

    var hornWahl by remember { mutableStateOf(pult.horn) }
    var lautstaerke by remember { mutableFloatStateOf(pult.laut.toFloat()) }
    var heckAn by remember { mutableStateOf(pult.heck) }
    var nacht by remember { mutableStateOf(pult.nacht) }
    var hornAn by remember { mutableStateOf(Signalhorn.laeuft() != null) }

    // Die Vorwahl gehört dem Gerät, nicht dem Besuch der Seite.
    LaunchedEffect(hornWahl, lautstaerke, heckAn, nacht) {
        Pultstand(hornWahl, lautstaerke.toInt(), heckAn, nacht).schreiben(zusammenhang)
    }

    // Der Wunsch bleibt stehen, auch wenn ihn das Fahrzeug nicht erfüllt — nach dem
    // Umsteigen zurück gilt wieder die alte Vorwahl.
    val hornKennung = if (hornVerbaut(hornWahl, meins, aus)) hornWahl else "stadt"
    val blaulichtAn = !meins.sondersignalAus
    val anhaltesignal = hornAn && hornKennung == "anhalt" && aus.matrix
    val faehrt = meins.status == 3 || meins.status == 7
    val heckLaeuft = heckAn && aus.heck

    // Ist die Fahrt vorbei, schweigt das Horn — wie draußen auch.
    LaunchedEffect(meins.status) {
        if (!faehrt && hornAn) {
            Signalhorn.stoppen()
            hornAn = false
        }
    }
    LaunchedEffect(lautstaerke) { Signalhorn.lautstaerke(lautstaerke / 100f) }
    // Beim Verlassen der Ansicht schweigt das Horn in jedem Fall.
    DisposableEffect(Unit) { onDispose { Signalhorn.stoppen() } }

    // ------------------------------------------------------------ Tafelinhalt
    val gewaehlt: Matrixbild? = if (matrixBild == "eigen") {
        matrixText.trim().takeIf { it.isNotEmpty() }?.let { Matrixbild("eigen", "Eigener Text", worte = listOf(it)) }
    } else {
        bilder.firstOrNull { it.id == matrixBild }
    }
    val tafelWorte = when {
        anhaltesignal -> listOf("STOP", "POLIZEI")
        !matrixAn || gewaehlt == null || gewaehlt.piktogramm != null -> emptyList()
        else -> gewaehlt.worte
    }
    val tafelPiktogramm = if (!anhaltesignal && matrixAn) gewaehlt?.piktogramm else null
    val tafelAn = tafelPiktogramm != null || tafelWorte.isNotEmpty()
    val tafelBlinkt = !anhaltesignal && tafelPiktogramm != null && gewaehlt?.blinkt == true
    val tafelBewegt = tafelAn && (tafelWorte.size > 1 || tafelWorte.any { bildBreite(wortBild(it)) > MATRIX_SPALTEN })

    // Der Takt der Tafel, in Dreißigstelsekunden — nur, wenn sich etwas bewegt.
    var takt by remember { mutableIntStateOf(0) }
    LaunchedEffect(tafelBewegt) {
        while (tafelBewegt) {
            delay(TAKT_MS)
            takt = (takt + 1) % 600_000
        }
    }
    val wortNummer = if (tafelWorte.size > 1) (takt / TAKTE_JE_WORT) % tafelWorte.size else 0
    val tafelBild: List<String> = when {
        tafelPiktogramm != null -> PIKTOGRAMME.getValue(tafelPiktogramm).split('/')
        tafelWorte.isNotEmpty() -> wortBild(tafelWorte[wortNummer.coerceIn(0, tafelWorte.lastIndex)])
        else -> emptyList()
    }
    val breite = bildBreite(tafelBild)
    val versatz = if (breite <= MATRIX_SPALTEN) (MATRIX_SPALTEN - breite) / 2 else MATRIX_SPALTEN - (takt % (MATRIX_SPALTEN + breite))

    // Die Uhr der Blitzer: 33 Sekunden sind das kleinste Vielfache von 1 s, 0,5 s und
    // 0,33 s — so laufen Doppelblitz, Warnblitz und Rotblitz ohne Sprung durch.
    val uhr by rememberInfiniteTransition(label = "sondersignal").animateFloat(
        initialValue = 0f,
        targetValue = 33f,
        animationSpec = infiniteRepeatable(tween(33_000, easing = LinearEasing), RepeatMode.Restart),
        label = "uhr",
    )
    fun doppelblitz(versatz: Float): Float {
        val p = (uhr + versatz) % 1f
        return if (p < 0.05f || (p >= 0.15f && p < 0.2f)) 1f else 0.14f
    }
    val warnblitz = ((uhr * 2f) % 1f).let { p -> if (p < 0.08f || (p >= 0.24f && p < 0.32f)) 1f else 0.18f }
    val rotblitz = if ((uhr / 0.33f) % 1f < 0.3f) 1f else 0.12f
    val tafelLicht = (if (tafelBlinkt && uhr % 1f >= 0.45f) 0.08f else 1f) * (if (nacht) 0.45f else 1f)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.FlaecheTief, Rundung.Klein)
            .border(1.dp, Farben.Rand, Rundung.Klein)
            .padding(Abstand.Klein),
    ) {
        Text(
            "Blaulicht schaltet die Fahrt — Matrix, Heckwarner und Signalhorn laufen lokal auf diesem Gerät.",
            style = Schrift.Winzig,
            color = Farben.TextLeise,
        )

        // -------------------------------------------------- Die Heckansicht
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth().background(PULT_TIEF, Rundung.Winzig).border(1.dp, PULT_RAND, Rundung.Winzig).padding(Abstand.Winzig),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Blaulichtbalken(blaulichtAn, doppelblitz(0f))
                Tafel(tafelBild, versatz, tafelAn, tafelLicht, anhaltesignal, gespiegelt = false, modifier = Modifier.weight(0.56f).fillMaxHeight())
                Blaulichtbalken(blaulichtAn, doppelblitz(0.5f))
            }
            // Drei Paar gelbe Leuchten, synchron (§ 52 Abs. 11 StVZO, ECE-R65).
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                repeat(2) { seite ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .background(if (heckLaeuft) LED_AN.copy(alpha = warnblitz) else Color(0xFF2E2415), RoundedCornerShape(2.dp)),
                            )
                        }
                    }
                    if (seite == 0) Box(Modifier.weight(0.36f))
                }
            }
        }

        // ------------------------------------------------- Die Frontblitzer
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth().background(PULT_TIEF, Rundung.Winzig).border(1.dp, PULT_RAND, Rundung.Winzig).padding(Abstand.Winzig),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Pultwort("VORNE")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    repeat(3) { Blitzer(if (blaulichtAn) BLAU_AN.copy(alpha = doppelblitz(0f)) else BLAU_AUS) }
                    // Der rote Blitz gehört zum Anhaltesignal und sonst nirgends hin.
                    Blitzer(if (anhaltesignal) Color(0xFFFF2A18).copy(alpha = rotblitz) else Color(0xFF2A1210))
                    repeat(3) { Blitzer(if (blaulichtAn) BLAU_AN.copy(alpha = doppelblitz(0.5f)) else BLAU_AUS) }
                }
            }
            // Die Lichtschrift nach vorn steht in Spiegelschrift — gelesen im Innenspiegel.
            if (anhaltesignal) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Text("ANHALTESIGNAL", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, letterSpacing = 1.2.sp), color = Color(0xFFFF6A58))
                    Tafel(tafelBild, versatz, true, 1f, true, gespiegelt = true, modifier = Modifier.weight(1f))
                }
            }
        }

        // --------------------------------------------------- Die Matrixzeile
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Ziel.Kompakt)
                .background(Farben.Flaeche, Rundung.Winzig)
                .border(1.dp, if (matrixOffen && aus.matrix) Farben.Amber else Farben.Rand, Rundung.Winzig)
                .clickable(enabled = aus.matrix, role = Role.Button, onClickLabel = "Verkehrsleitanzeige") { matrixOffen = !matrixOffen }
                .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
        ) {
            Text(
                "MATRIX",
                style = Schrift.Klein.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = if (aus.matrix) Farben.Text else Farben.TextSehrLeise,
            )
            Text(
                when {
                    !aus.matrix -> "nicht verbaut"
                    anhaltesignal -> "Anhaltesignal — STOP POLIZEI"
                    matrixAn -> gewaehlt?.name.orEmpty()
                    else -> "aus"
                },
                style = Schrift.Winzig,
                color = Farben.TextLeise,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(if (matrixOffen) "▾" else "▸", style = Schrift.Klein, color = Farben.TextLeise)
        }

        if (matrixOffen && aus.matrix) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                maxItemsInEachRow = 3,
                modifier = Modifier.fillMaxWidth(),
            ) {
                bilder.forEach { b ->
                    Pultknopf(b.name, an = matrixAn && matrixBild == b.id, modifier = Modifier.weight(1f)) {
                        matrixBild = b.id
                        matrixAn = true
                    }
                }
                Pultknopf("Aus", an = false, leise = true, modifier = Modifier.weight(1f)) { matrixAn = false }
            }
            // Der freie Text. Was breiter ist als die Tafel, läuft durch.
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                Feld(
                    wert = matrixText,
                    beiAenderung = { matrixText = it.take(24) },
                    platzhalter = "Eigener Text …",
                    weiterTaste = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
                Pultknopf("Anzeigen", an = matrixAn && matrixBild == "eigen", aktiv = matrixText.isNotBlank()) {
                    if (matrixText.isNotBlank()) {
                        matrixBild = "eigen"
                        matrixAn = true
                    }
                }
            }
            // Tag und Nacht: zwei Stufen, am Gerät ein Kippschalter.
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                Pultwort("HELLIGKEIT", Modifier.weight(1f))
                Pultknopf("Tag", an = !nacht) { nacht = false }
                Pultknopf("Nacht", an = nacht) { nacht = true }
            }
        }

        // ---------------------------------------------------- Die Schalter
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth()) {
            Schaltknopf("Blaulicht", ZEICHEN_BLAULICHT, an = blaulichtAn, aktiv = true) {
                beiSondersignal(blaulichtAn)
            }
            Schaltknopf("Signalhorn", ZEICHEN_HORN, an = hornAn, aktiv = aus.horn) {
                if (hornAn) {
                    Signalhorn.stoppen()
                    hornAn = false
                } else {
                    Signalhorn.lautstaerke(lautstaerke / 100f)
                    Signalhorn.starten(hornKennung)
                    hornAn = true
                }
            }
            Schaltknopf("Heckwarner", ZEICHEN_HECK, an = heckAn, aktiv = aus.heck) { heckAn = !heckAn }
        }
        if (!aus.horn) Text("Dieses Fahrzeug hat kein Martinshorn.", style = Schrift.Winzig, color = Farben.TextSehrLeise)
        if (heckLaeuft && faehrt) {
            Text(
                "Heckwarnanlage nur im Stand oder bei Schrittgeschwindigkeit (§ 52 Abs. 11 StVZO).",
                style = Schrift.Winzig,
                color = Farben.AmberHell,
            )
        }

        // ---------------------------------------------------- Die Hornwahl
        if (aus.horn) {
            Pultwort("HORNTON")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                maxItemsInEachRow = 3,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Signalhorn.ARTEN.forEach { h ->
                    val verbaut = hornVerbaut(h.id, meins, aus)
                    Pultknopf(h.name, an = hornKennung == h.id, aktiv = verbaut, modifier = Modifier.weight(1f)) {
                        if (!verbaut) return@Pultknopf
                        hornWahl = h.id
                        // Bei laufendem Horn hört man den Wechsel sofort.
                        if (hornAn) Signalhorn.starten(h.id)
                    }
                }
            }
            Text(Signalhorn.bauart(hornKennung).was, style = Schrift.Winzig, color = Farben.TextSehrLeise)
            Signalhorn.ARTEN.filter { !hornVerbaut(it.id, meins, aus) }.forEach {
                Text("${it.name}: ${hornGrund(it.id, meins)}", style = Schrift.Winzig, color = Farben.TextSehrLeise)
            }
        }

        // ------------------------------------------------- Die Lautstärke
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Icon(ZEICHEN_LAUT, contentDescription = null, tint = Farben.TextLeise, modifier = Modifier.size(20.dp))
            Slider(
                value = lautstaerke,
                onValueChange = { lautstaerke = (Math.round(it / 5f) * 5f).coerceIn(0f, 100f) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Farben.Amber,
                    activeTrackColor = Farben.Amber,
                    inactiveTrackColor = Farben.FlaecheAktiv,
                ),
                modifier = Modifier.weight(1f),
            )
            Text("${lautstaerke.toInt()}%", style = Schrift.MonoKlein, color = Farben.Text, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
        }
    }
}

// ----------------------------------------------------------------- Bauteile

private val PULT_TIEF = Color(0xFF12151A)
private val PULT_RAND = Color(0xFF2B3139)
private val LED_AN = Color(0xFFFFBE1A)
private val BLAU_AN = Color(0xFF2F6BFF)
private val BLAU_AUS = Color(0xFF24304A)

@Composable
private fun RowScope.Blaulichtbalken(an: Boolean, licht: Float) {
    Box(
        Modifier
            .weight(0.22f)
            .fillMaxHeight()
            .background(
                if (an) {
                    Brush.verticalGradient(listOf(Color(0xFF2F6BFF).copy(alpha = licht), Color(0xFF1B3FAE).copy(alpha = licht)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF24304A), Color(0xFF17203A)))
                },
                RoundedCornerShape(3.dp),
            ),
    )
}

@Composable
private fun Blitzer(farbe: Color) {
    Box(Modifier.size(width = 10.dp, height = 6.dp).background(farbe, RoundedCornerShape(2.dp)))
}

@Composable
private fun Pultwort(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, letterSpacing = 1.3.sp),
        color = Farben.TextSehrLeise,
        modifier = modifier,
    )
}

/** Eine Taste der Matrix- und Hornwahl — eingerastet in Amber, gesperrt gestrichelt. */
@Composable
private fun Pultknopf(
    text: String,
    an: Boolean,
    modifier: Modifier = Modifier,
    aktiv: Boolean = true,
    leise: Boolean = false,
    beiDruck: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = Ziel.Kompakt)
            .background(if (an) Farben.HauchAmber else Farben.Flaeche, Rundung.Winzig)
            .border(1.dp, if (an) Farben.Amber else Farben.Rand, Rundung.Winzig)
            .clickable(enabled = aktiv, role = Role.Button, onClick = beiDruck)
            .padding(Abstand.Winzig),
    ) {
        Text(
            text,
            style = Schrift.Winzig,
            color = when {
                !aktiv -> Farben.TextSehrLeise
                an -> Farben.Amber
                leise -> Farben.TextLeise
                else -> Farben.Text
            },
            textAlign = TextAlign.Center,
        )
    }
}

/** Einer der drei großen Schalter — Blaulicht, Signalhorn, Heckwarner. */
@Composable
private fun RowScope.Schaltknopf(
    name: String,
    zeichen: androidx.compose.ui.graphics.vector.ImageVector,
    an: Boolean,
    aktiv: Boolean,
    beiDruck: () -> Unit,
) {
    val farbe = when {
        !aktiv -> Farben.TextSehrLeise
        an -> Farben.BlauHell
        else -> Farben.Text
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig, Alignment.CenterVertically),
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 72.dp)
            .background(if (an) Farben.HauchHell else Farben.Flaeche, Rundung.Winzig)
            .border(1.dp, if (an) Farben.Blau else Farben.Rand, Rundung.Winzig)
            .clickable(enabled = aktiv, role = Role.Switch, onClickLabel = name, onClick = beiDruck)
            .padding(Abstand.Winzig),
    ) {
        Icon(zeichen, contentDescription = null, tint = farbe, modifier = Modifier.size(26.dp))
        Text(name, style = Schrift.Winzig.copy(fontWeight = FontWeight.SemiBold), color = farbe)
    }
}

/**
 * Die Tafel — ein Punktraster 44 × 7. Das dunkle Raster steht immer da: Auch eine
 * ausgeschaltete LED-Tafel zeigt ihre Lampen, und daran erkennt man sie als eine.
 */
@Composable
private fun Tafel(
    bild: List<String>,
    versatz: Int,
    an: Boolean,
    licht: Float,
    anhalt: Boolean,
    gespiegelt: Boolean,
    modifier: Modifier = Modifier,
) {
    val ledAus = if (anhalt) Color(0xFF2A100C) else Color(0xFF2A2210)
    val ledAn = if (anhalt) Color(0xFFFF3B28) else LED_AN
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(Color(0xFF05070A), RoundedCornerShape(2.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
            .padding(1.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(MATRIX_SPALTEN.toFloat() / MATRIX_ZEILEN)) {
            val zelle = size.width / MATRIX_SPALTEN
            scale(if (gespiegelt) -1f else 1f, 1f) {
                for (y in 0 until MATRIX_ZEILEN) for (x in 0 until MATRIX_SPALTEN) {
                    drawCircle(ledAus, zelle * 0.3f, Offset((x + 0.5f) * zelle, (y + 0.5f) * zelle))
                }
                if (an) {
                    bild.forEachIndexed { y, reihe ->
                        reihe.forEachIndexed { x, z ->
                            val sx = x + versatz
                            if (z == '#' && sx in 0 until MATRIX_SPALTEN) {
                                val mitte = Offset((sx + 0.5f) * zelle, (y + 0.5f) * zelle)
                                if (!gespiegelt) drawCircle(ledAn.copy(alpha = 0.22f * licht), zelle * 0.85f, mitte)
                                drawCircle(ledAn.copy(alpha = licht), zelle * 0.42f, mitte)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------- Die Schrift der Tafel

/** Punkte in der Höhe — sieben, wie die TR Funkstreifenwagen es für die Schrifthöhe verlangt. */
private const val MATRIX_ZEILEN = 7

/** Punkte in der Breite — vierzig sind eine Spalte zu knapp für „POLIZEI". */
private const val MATRIX_SPALTEN = 44

private const val TAKT_MS = 30L
private const val TAKTE_JE_WORT = 30

/** Ein Zeichen: sieben Reihen zu fünf Spalten, `#` ist eine brennende LED. */
private val SCHRIFT = mapOf(
    'A' to ".###./#...#/#...#/#####/#...#/#...#/#...#",
    'B' to "####./#...#/#...#/####./#...#/#...#/####.",
    'C' to ".###./#...#/#..../#..../#..../#...#/.###.",
    'D' to "####./#...#/#...#/#...#/#...#/#...#/####.",
    'E' to "#####/#..../#..../####./#..../#..../#####",
    'F' to "#####/#..../#..../####./#..../#..../#....",
    'G' to ".###./#...#/#..../#.###/#...#/#...#/.###.",
    'H' to "#...#/#...#/#...#/#####/#...#/#...#/#...#",
    'I' to ".###./..#../..#../..#../..#../..#../.###.",
    'J' to "..###/...#./...#./...#./...#./#..#./.##..",
    'K' to "#...#/#..#./#.#../##.../#.#../#..#./#...#",
    'L' to "#..../#..../#..../#..../#..../#..../#####",
    'M' to "#...#/##.##/#.#.#/#...#/#...#/#...#/#...#",
    'N' to "#...#/##..#/#.#.#/#..##/#...#/#...#/#...#",
    'O' to ".###./#...#/#...#/#...#/#...#/#...#/.###.",
    'P' to "####./#...#/#...#/####./#..../#..../#....",
    'Q' to ".###./#...#/#...#/#...#/#.#.#/#..#./.##.#",
    'R' to "####./#...#/#...#/####./#.#../#..#./#...#",
    'S' to ".####/#..../#..../.###./....#/....#/####.",
    'T' to "#####/..#../..#../..#../..#../..#../..#..",
    'U' to "#...#/#...#/#...#/#...#/#...#/#...#/.###.",
    'V' to "#...#/#...#/#...#/#...#/#...#/.#.#./..#..",
    'W' to "#...#/#...#/#...#/#.#.#/#.#.#/##.##/#...#",
    'X' to "#...#/#...#/.#.#./..#../.#.#./#...#/#...#",
    'Y' to "#...#/#...#/.#.#./..#../..#../..#../..#..",
    'Z' to "#####/....#/...#./..#../.#.../#..../#####",
    '0' to ".###./#...#/#..##/#.#.#/##..#/#...#/.###.",
    '1' to "..#../.##../..#../..#../..#../..#../.###.",
    '2' to ".###./#...#/....#/...#./..#../.#.../#####",
    '3' to "#####/...#./..##./....#/....#/#...#/.###.",
    '4' to "...#./..##./.#.#./#..#./#####/...#./...#.",
    '5' to "#####/#..../####./....#/....#/#...#/.###.",
    '6' to "..##./.#.../#..../####./#...#/#...#/.###.",
    '7' to "#####/....#/...#./..#../.#.../.#.../.#...",
    '8' to ".###./#...#/#...#/.###./#...#/#...#/.###.",
    '9' to ".###./#...#/#...#/.####/....#/...#./.##..",
    // Der Zwischenraum ist schmaler als ein Buchstabe — die Tafel ist ohnehin knapp.
    ' ' to "../../../../../../..",
    '-' to "...../...../...../.###./...../...../.....",
    '.' to "...../...../...../...../...../.##../.##..",
    ',' to "...../...../...../...../.##../.##../.#...",
    ':' to "...../.##../.##../...../.##../.##../.....",
    '!' to "..#../..#../..#../..#../..#../...../..#..",
    '/' to "....#/....#/...#./..#../.#.../#..../#....",
    '+' to "...../..#../..#../#####/..#../..#../.....",
    '?' to ".###./#...#/....#/...#./..#../...../..#..",
)

/** Die Piktogramme — dreizehn Punkte breit, damit der Pfeil eine Spitze bekommt. */
private val PIKTOGRAMME = mapOf(
    "pfeilLinks" to "...#........./..##........./.###........./#############/.###........./..##........./...#.........",
    "pfeilRechts" to ".........#.../.........##../.........###./#############/.........###./.........##../.........#...",
    "gefahr" to "......#....../.....#.#...../....#.#.#..../...#..#..#.../..#.......#../.#....#....#./#############",
)

/** Großbuchstaben, Umlaute als Buchstabenpaar — in sieben Punkten ist kein Platz für Tüpfel. */
private fun tafelschrift(text: String): String =
    text.uppercase()
        .replace("Ä", "AE").replace("Ö", "OE").replace("Ü", "UE").replace("ß", "SS").replace("ẞ", "SS")
        .map { z -> if (SCHRIFT.containsKey(z)) z else if (z.isWhitespace()) ' ' else '?' }
        .joinToString("")

/** Ein Wort als Punktfeld: sieben Reihen, zwischen den Zeichen eine dunkle Spalte. */
private fun wortBild(wort: String): List<String> {
    val reihen = Array(MATRIX_ZEILEN) { StringBuilder() }
    tafelschrift(wort).forEachIndexed { i, zeichen ->
        val muster = (SCHRIFT[zeichen] ?: SCHRIFT.getValue('?')).split('/')
        for (y in 0 until MATRIX_ZEILEN) {
            if (i > 0) reihen[y].append('.')
            reihen[y].append(muster.getOrElse(y) { "" })
        }
    }
    return reihen.map { it.toString() }
}

private fun bildBreite(bild: List<String>): Int = bild.firstOrNull()?.length ?: 0

private data class Matrixbild(
    val id: String,
    val name: String,
    val worte: List<String> = emptyList(),
    val piktogramm: String? = null,
    val blinkt: Boolean = false,
)

/**
 * Die Polizei spricht, die anderen sichern: Eine Polizeimatrix richtet sich an ein
 * Fahrzeug („BITTE FOLGEN"), die Tafel des Rüstwagens an alle dahinter.
 */
private val MATRIX_POLIZEI = listOf(
    Matrixbild("folgen", "Bitte folgen", worte = listOf("BITTE", "FOLGEN")),
    Matrixbild("stop", "Stop", worte = listOf("STOP", "POLIZEI")),
    Matrixbild("unfall", "Unfall", worte = listOf("UNFALL")),
    Matrixbild("links", "Pfeil links", piktogramm = "pfeilLinks", blinkt = true),
    Matrixbild("rechts", "Pfeil rechts", piktogramm = "pfeilRechts", blinkt = true),
)

private val MATRIX_SICHERUNG = listOf(
    Matrixbild("links", "Pfeil links", piktogramm = "pfeilLinks", blinkt = true),
    Matrixbild("rechts", "Pfeil rechts", piktogramm = "pfeilRechts", blinkt = true),
    Matrixbild("gefahr", "Gefahrstelle", piktogramm = "gefahr", blinkt = true),
    Matrixbild("unfall", "Unfall", worte = listOf("UNFALL")),
    Matrixbild("stau", "Stau", worte = listOf("STAU")),
    Matrixbild("gasse", "Rettungsgasse", worte = listOf("RETTUNGSGASSE")),
)

// ------------------------------------------------------------- Die Zeichen

private val ZEICHEN_BLAULICHT = fahrzeugzeichen(
    "Blaulicht",
    "M5 16h14M7 16v-3a5 5 0 0 1 10 0v3",
    "M12 6V4M6.5 8.5 5 7M17.5 8.5 19 7",
    "M4 19h16",
)
private val ZEICHEN_HORN = fahrzeugzeichen(
    "Signalhorn",
    "M4 10v4h3l5 4V6l-5 4H4Z",
    "M16 9a4 4 0 0 1 0 6M18.5 6.5a8 8 0 0 1 0 11",
)
private val ZEICHEN_HECK = fahrzeugzeichen(
    "Heckwarner",
    rechteck(3f, 9f, 18f, 6f, 1.5f),
    "M7 9v6M11 9v6M15 9v6",
)
private val ZEICHEN_LAUT = fahrzeugzeichen(
    "Lautstaerke",
    "M5 10v4h3l4 3V7l-4 3H5Z",
    "M15 10a3 3 0 0 1 0 4",
)

// ------------------------------------------------------- Was das Fahrzeug hat

/** Was dieses Fahrzeug trägt — hergeleitet aus der Bauart, nicht gepflegt. */
private data class Ausstattung(val blaulicht: Boolean, val horn: Boolean, val heck: Boolean, val matrix: Boolean, val lkw: Boolean)

private fun ausstattungVon(f: Rundenfahrzeug): Ausstattung {
    val typ = f.typ.trim().lowercase()
    val heliOhne = typ in BAU_HELI_OHNE
    val luft = f.istLuftfahrzeug || heliOhne || typ in BAU_HELI
    val schmal = typ in BAU_SCHMAL
    val boot = typ in BAU_BOOT
    val klasse = when {
        typ in BAU_LKW -> "lkw"
        typ in BAU_TRANSPORTER -> "transporter"
        typ in BAU_PKW -> "pkw"
        luft || schmal || boot -> ""
        // Ein unbekannter Typ fährt als Standardfahrzeug seiner Organisation.
        f.organisation == "Rettungsdienst" -> "transporter"
        f.organisation == "Polizei" -> "pkw"
        else -> "lkw"
    }
    val gross = klasse == "lkw" || klasse == "transporter"
    return Ausstattung(
        blaulicht = !heliOhne,
        // Am Boot ist es ein Typhon und am Hubschrauber gar nichts.
        horn = !luft && !boot,
        heck = !luft && !schmal && !boot,
        matrix = !luft && !schmal && !boot && (gross || f.organisation == "Polizei"),
        lkw = klasse == "lkw",
    )
}

/** Druckluft braucht eine Druckluftanlage; das Anhaltesignal gehört zur Polizei mit Tafel. */
private fun hornVerbaut(art: String, f: Rundenfahrzeug, aus: Ausstattung): Boolean = when (art) {
    "pressluft" -> aus.lkw || f.typ == "NEF"
    "anhalt" -> f.organisation == "Polizei" && aus.matrix
    else -> true
}

private fun hornGrund(art: String, f: Rundenfahrzeug): String = when (art) {
    "pressluft" -> "Braucht eine Druckluftanlage — nur am Lastwagen und am NEF"
    "anhalt" -> if (f.organisation == "Polizei") "Nur zusammen mit dem Anhaltesignalgeber — dieses Fahrzeug hat keine Tafel" else "Nur bei der Polizei"
    else -> ""
}

/**
 * Die Vorwahl des Pults — sie gehört dem Gerät, nicht dem Konto (im Web der
 * `localStorage` unter `pagerspass.sondersignal`). Der gespeicherte Wunsch wird
 * geprüft und nicht geglaubt.
 */
private data class Pultstand(val horn: String, val laut: Int, val heck: Boolean, val nacht: Boolean) {
    fun schreiben(zusammenhang: Context) {
        runCatching {
            zusammenhang.getSharedPreferences(DATEI, Context.MODE_PRIVATE).edit()
                .putString("horn", horn).putInt("laut", laut).putBoolean("heck", heck).putBoolean("nacht", nacht)
                .apply()
        }
    }

    companion object {
        private const val DATEI = "pagerspass_sondersignal"

        fun lesen(zusammenhang: Context): Pultstand = runCatching {
            val a = zusammenhang.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
            val horn = a.getString("horn", null)?.takeIf { h -> Signalhorn.ARTEN.any { it.id == h } } ?: "stadt"
            val laut = a.getInt("laut", 70).takeIf { it in 0..100 }?.let { Math.round(it / 5f) * 5 } ?: 70
            Pultstand(horn, laut, a.getBoolean("heck", false), a.getBoolean("nacht", false))
        }.getOrDefault(Pultstand("stadt", 70, heck = false, nacht = false))
    }
}

// Die Bauarten aus `utils/fahrzeugBauplan.ts` (Web v6), nur so weit, wie das Bedienteil
// sie braucht: Form (Hubschrauber, Boot, Krad/Quad) und Klasse (Lkw, Transporter, Pkw).

private val BAU_LKW = setOf(
    "tsf-w", "lf 8/6", "mlf", "lf 10", "lf 20", "lf 16-ts", "hlf 10", "hlf 20", "hlf (werk)",
    "tlf 16/25", "tlf 3000", "tlf 4000", "tlf 2000 unimog", "slf", "flf", "dlk 23/12",
    "dla(k) 18/12", "tmf 32", "gw-höhenrettung", "rw", "rw-kran", "fwk 30", "wlf mit ab",
    "gw-gefahrgut", "gw-gefahrgut (werk)", "gw-atemschutz", "dekon-p", "gw-wasserrettung", "elw 2",
    "gw-logistik", "wlf + ab rüst", "wlf + ab tank", "wlf + ab atem-/strahlenschutz",
    "wlf + ab atemschutz", "wlf + ab gefahrgut", "wlf + ab tel", "wlf + ab schlauch",
    "wlf + ab mulde", "wlf + ab notstrom", "wlf + ab dekon p", "wlf + ab öl", "wlf + ab sandsack",
    "wlf + ab betreuung", "wlf + ab rüst vegetation", "wlf + ab küche", "wlf", "gw-öl", "sw 1000",
    "sw 2000", "sw kats", "tsf", "hlf 20/16", "lf-kats", "tlf 2000", "dlk 12/9", "hab 42",
    "rw-schiene", "gw-bergung", "gw-l2", "gw-versorgung", "dekon-v", "gw-hygiene", "sw 2000-tr",
    "tlf-w", "gw-wald", "wlf + ab manv", "wlf + ab pumpe", "wlf + ab sonderlöschmittel",
    "wlf + ab hfs", "ulf", "gw-san", "s-rtw", "grtw", "gkw", "mzkw", "fgr sb", "fgr brb", "fgr i",
    "fgr n", "fgr r", "fgr tw", "fgr w", "fgr ö", "mlw 5", "fgr e", "fgr wp", "fgr log-v",
    "fgr log-mw", "fgr sp", "fgr bel", "fgr egs", "wawe 10", "sonderwagen", "elf", "klf-a", "lfa",
    "lfb-a2", "rlf-a 2000", "hlf 1", "hlf 2", "hlf 3", "hlf 4", "tlfa 2000", "tlfa 3000",
    "tlfa 4000", "gtlf", "tank (bf wien)", "srf", "rf", "krf", "kran (bf wien)", "dlk 23-12",
    "tmb", "wlf + mulde", "wlf + container schadstoff", "wlf + container atemschutz", "sf", "asf",
    "vf", "a-boot", "sew", "bergrettung einsatzfahrzeug", "einsatzfahrzeug wasserrettung", "tlf",
    "tlf (gross)", "lf", "kleinlöschfahrzeug", "hlf / ersteinsatzfahrzeug", "adl", "hrf", "pif",
    "strassenrettungsfahrzeug", "öl-/chemiewehrfahrzeug", "chemiewehr (c-wehr)",
    "atemschutzfahrzeug", "wasserwehrfahrzeug", "feuerwehrboot", "tlf (schutz & rettung zürich)",
    "zivilschutz pioniere", "zivilschutz betreuung", "grossereignis-fahrzeug",
    "samariter sanitätsposten", "rettungsstation ars",
)

private val BAU_TRANSPORTER = setOf(
    "forsttraktor mit löschanhänger", "gw-mess", "elw 1", "elw 1 a-dienst", "mtf",
    "drohnenstaffel", "mzf mit ladefläche", "mzf + anhänger gefahrgut",
    "mzf + anhänger strom/licht", "mzf + anhänger mzb", "klf", "vrw", "elw 1 (führungsgruppe)",
    "mzf", "cbrn-erkw", "gw-taucher", "mzf + anhänger wassertank", "mzf + anhänger feldküche",
    "rtw", "rtw (privat)", "ktw", "ktw (privat)", "nktw", "itw", "elw 1 (rd)", "btkw", "feldküche",
    "mtw verpflegung", "mtw hundestaffel", "rettungshundestaffel", "tauchergruppe", "ktw-b",
    "mtw bergwacht", "gw-strömungsrettung", "tauchergruppe (wasserwacht)", "mtw", "fgr fk",
    "fgr ortung", "fükw", "kriminaltechnik", "grukw", "sek", "mek", "polizeidrohne", "bfe",
    "gefkw", "grukw (bpol)", "kdo", "strahlenmessfahrzeug", "naw", "kat-zug", "wega",
    "bereitschaftseinheit", "eko cobra", "führungsfahrzeug", "strahlenwehr",
    "rtw (schutz & rettung zürich)", "ktw / verlegung", "redog suchteam", "ordnungsdienst",
    "sondereinheit enzian",
)

private val BAU_PKW = setOf(
    "kdow", "kdow c-dienst", "kdow b-dienst", "nef", "orgl", "elrd", "lna", "kit", "mantrailer",
    "fustw", "unfallaufnahme", "diensthundeführer", "zivilstreife", "kdd", "fustw autobahn",
    "kdof", "kdo-rk", "einsatzleitung (brw)", "funkstreifenwagen", "sektorstreife",
    "verkehrsstreife", "diensthundestreife", "kriminaldienst", "einsatzleitfahrzeug", "kdo-fz",
    "einsatzleiter sanität", "first-responder-fahrzeug", "patrouillenfahrzeug",
    "stadtpolizei patrouille", "verkehrspolizei", "kriminalpolizei",
)

private val BAU_SCHMAL = setOf(
    "quad (feuerwehr)", "quad", "krad",
)

private val BAU_BOOT = setOf(
    "rtb 2", "mrb", "rtb", "seenotrettungsboot", "seenotrettungskreuzer", "rettungsboot",
    "slrg rettungsboot",
)

private val BAU_HELI_OHNE = setOf(
    "rth", "ith", "nah", "rettungshelikopter", "rettungshelikopter (air zermatt)",
)

private val BAU_HELI = setOf(
    "polizeihubschrauber", "polizeihelikopter",
)
