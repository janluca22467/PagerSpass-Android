package de.pagerspass.pagerspass.ansichten.welt

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay

/**
 * Die Farben der Welt — `.weltplatz` in `styles/welt.css` (PagerSpass 6).
 *
 * <b>Warum die Welt eigene Marken hat.</b> Sie trägt ein Radar-Cyan für alles,
 * was <em>ausgewählt</em> ist (gewählter Reiter, offene Seite, Fortschritt).
 * Bernstein bleibt, was es im ganzen Spiel ist: Geld und die eine Handlung, die
 * zählt (Bauen). Alle Marken sind aus den Grundfarben gemischt — keine neue
 * Farbe in der Palette, wie im Web.
 */
object Weltfarben {
    /** `--welt-akzent` = `--such-farbe`. */
    val Akzent = Farben.SuchFarbe

    /** `--welt-akzent-tief`: 38 % Cyan in `--bg-tief`. */
    val AkzentTief = Color(0xFF245059)

    /** `--welt-hauch`: 13 % Cyan, durchscheinend. */
    val Hauch = Farben.SuchFarbe.copy(alpha = 0.13f)

    /** Das Glas: die Flächen aus PagerSpass Dunkel, durchscheinend. */
    val Glas = Farben.Flaeche.copy(alpha = 0.88f)
    val GlasHoch = Farben.FlaecheHoch.copy(alpha = 0.92f)
    val GlasTief = Farben.BgTief.copy(alpha = 0.90f)
    val Kante = Farben.Rand
}

/** Die beiden Höhen der Handyform — `--kopfhoehe` und `--barhoehe` in `WeltView.vue`. */
object Weltmass {
    /** Eine schwebende Karte von 56 und ihr Rand. */
    val Kopfhoehe = 72.dp

    /** So hoch wie die Tableiste der übrigen App — sie steht an derselben Stelle. */
    val Leistenhoehe = 70.dp
}

// ======================================================================= Kopf

/**
 * Der Stand oben — `Weltkopf.vue`: Flagge, Name und Kreis, Guthaben, Stufe, und
 * die Erfahrung als Kante unten.
 *
 * <b>Er ist kein zweiter Wegweiser.</b> Der einzige Griff darin ist die Stufe —
 * sie öffnet die Laufbahn. Der Ausgang aus der Welt steht unter „Mehr“, wie im
 * Web; ein Pfeil hier oben sähe aus wie „zurück zur Karte“ und führte hinaus.
 */
@Composable
fun Weltkopf(
    zustand: Weltzustand,
    beiLaufbahn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Der Betrieb ist der frischere; wo es ihn gibt, gilt er ganz — auf der
    // höchsten Stufe ist `erfahrungStufeBis` dort gültig null.
    val ab = zustand.betrieb?.erfahrungStufeAb ?: zustand.stand?.erfahrungStufeAb ?: 0
    val bis = if (zustand.betrieb != null) zustand.betrieb.erfahrungStufeBis else zustand.stand?.erfahrungStufeBis
    val erfahrung = zustand.betrieb?.erfahrung ?: zustand.stand?.erfahrung ?: 0
    val hoechste = bis == null || bis <= ab
    val anteil = if (hoechste) 1f else ((erfahrung - ab).toFloat() / (bis!! - ab)).coerceIn(0f, 1f)
    val restschuld = zustand.betrieb?.restschuld ?: 0
    val erfahrungTitel = if (hoechste) {
        "Höchste Stufe erreicht · ${zahl(erfahrung)} Erfahrung"
    } else {
        val inDer = (erfahrung - ab).coerceAtLeast(0)
        val kosten = bis!! - ab
        "${zahl(inDer)} von ${zahl(kosten)} Erfahrung — noch ${zahl(kosten - inDer)} bis Stufe ${zustand.stufe + 1}"
    }

    // Die Gutschrift leuchtet vier Sekunden auf; mehrere in Folge zählen zusammen.
    // Am Handy wiegt sie schwerer als am Rechner: Steht eine Seite offen, ist
    // der Kopf der einzige Ort, an dem sie ankommt.
    var gezeigt by remember { mutableStateOf<Long?>(null) }
    var zuletzt by remember { mutableStateOf(0L) }
    LaunchedEffect(zustand.gutschrift) {
        val g = zustand.gutschrift ?: return@LaunchedEffect
        if (g.um == zuletzt) return@LaunchedEffect
        zuletzt = g.um
        gezeigt = (gezeigt ?: 0) + g.betrag
        delay(4_000)
        gezeigt = null
    }

    val wer = zustand.stand
    Box(
        modifier = modifier
            .padding(Abstand.Klein)
            .fillMaxWidth()
            .height(Weltmass.Kopfhoehe - Abstand.Klein * 2)
            .background(Weltfarben.Glas, Rundung.Normal)
            .border(1.dp, Weltfarben.Kante, Rundung.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(start = Abstand.Normal, end = Abstand.Klein),
        ) {
            if (wer != null) {
                // Oben links die Flagge: in welchem Land die eigene Leitstelle steht.
                Landesflagge(wer.staat, 18.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .padding(end = Abstand.Winzig)
                                .size(7.dp)
                                .background(Farben.Gruen, CircleShape),
                        )
                        Text(
                            wer.name,
                            style = Schrift.Normal.copy(fontWeight = FontWeight.W700, lineHeight = Schrift.NORMAL),
                            color = Farben.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    wer.kreis?.let {
                        Text(it, style = Schrift.Winzig.copy(lineHeight = Schrift.WINZIG), color = Farben.TextSehrLeise, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                Box(Modifier.weight(1f))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier.semantics { contentDescription = "${zahl(zustand.guthaben)} Welt-Credits" },
                ) {
                    Icon(Weltzeichen.Waehrung, contentDescription = null, tint = Farben.Amber, modifier = Modifier.size(14.dp))
                    Text(
                        zahl(zustand.guthaben),
                        style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold, lineHeight = Schrift.NORMAL),
                        color = Farben.AmberHell,
                    )
                    gezeigt?.let {
                        Text("+${zahl(it)}", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.GruenHell)
                    }
                }
                // Die Stufe ist am Handy der Griff zur Laufbahn — nicht der
                // Balken, den trifft kein Finger.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier
                        .background(Weltfarben.GlasTief, Rundung.Rund)
                        .border(1.dp, Weltfarben.Kante, Rundung.Rund)
                        .clickable(onClick = beiLaufbahn, role = Role.Button)
                        .semantics { contentDescription = "$erfahrungTitel — öffnet die Laufbahn" }
                        .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
                ) {
                    // Amber und nicht Rot: Die Schuld ist kein Fehler, sondern ein
                    // selbst gewählter Zustand.
                    if (restschuld > 0) {
                        Text("−${zahl(restschuld)}", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.Amber)
                    }
                    Text(
                        "Stufe ${zustand.stufe}",
                        style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                        color = Weltfarben.Akzent,
                    )
                }
            }
        }
        // Die Erfahrung liegt auf der Unterkante, über die volle Breite — Blau
        // zu Cyan; auf der höchsten Stufe grün, ein Zustand statt Fortschritt.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = Abstand.Normal)
                .fillMaxWidth()
                .height(3.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(anteil)
                    .fillMaxHeight()
                    .background(
                        if (hoechste) Brush.horizontalGradient(listOf(Farben.Gruen, Farben.Gruen))
                        else Brush.horizontalGradient(listOf(Farben.Blau, Weltfarben.Akzent)),
                        Rundung.Rund,
                    ),
            )
        }
    }
}

/**
 * Die Flagge eines Staats — `ui/Landesflagge.vue`. Drei Streifen für
 * Deutschland und Österreich, das Kreuz für die Schweiz (quadratisch).
 */
@Composable
fun Landesflagge(staat: String, groesse: Dp) {
    val schweiz = staat == "Schweiz"
    Canvas(
        Modifier
            .size(width = if (schweiz) groesse * 0.75f else groesse, height = groesse * 0.75f)
            .semantics { contentDescription = "Flagge ${staatName(staat)}" },
    ) {
        val b = size.width
        val h = size.height
        when (staat) {
            "Oesterreich" -> {
                drawRect(Color(0xFFC8102E), size = Size(b, h / 3))
                drawRect(Color.White, Offset(0f, h / 3), Size(b, h / 3))
                drawRect(Color(0xFFC8102E), Offset(0f, h * 2 / 3), Size(b, h / 3))
            }
            "Schweiz" -> {
                drawRect(Color(0xFFDA291C))
                drawRect(Color.White, Offset(b * 0.4f, h * 0.2f), Size(b * 0.2f, h * 0.6f))
                drawRect(Color.White, Offset(b * 0.2f, h * 0.4f), Size(b * 0.6f, h * 0.2f))
            }
            else -> {
                drawRect(Color.Black, size = Size(b, h / 3))
                drawRect(Color(0xFFDD0000), Offset(0f, h / 3), Size(b, h / 3))
                drawRect(Color(0xFFFFCE00), Offset(0f, h * 2 / 3), Size(b, h / 3))
            }
        }
        drawRect(Color.Black.copy(alpha = 0.25f), style = Stroke(1.dp.toPx()))
    }
}

/** `STAAT_LABEL` — der Name zum Schlüssel des Servers. */
fun staatName(staat: String): String = when (staat) {
    "Oesterreich" -> "Österreich"
    "Schweiz" -> "Schweiz"
    else -> "Deutschland"
}

// ====================================================================== Seite

/**
 * Eine Seite der Welt am Handy — `Weltseite.vue`.
 *
 * <b>Vom Kopf bis zur Reiterleiste, deckend.</b> Eine Landkarte, die durch eine
 * Liste hindurchscheint, ist nur noch Unruhe hinter dem Text. Oben ein Pfeil und
 * kein Kreuz: Zurück führt auf die Karte, eine Ebene höher — „Schließen“
 * beschriebe ein Fenster. Daneben das Zeichen des Reiters, zu dem die Seite
 * gehört, der Titel und die Zahl, die auch am Reiter steht.
 *
 * Steht die Tastatur offen, endet die Seite über ihr statt über der Leiste —
 * sonst schriebe man im Chat in ein Feld, das man nicht sieht.
 */
@Composable
fun Weltseite(
    titel: String,
    zeichen: ImageVector?,
    zahl: Int?,
    oben: Dp,
    beiSchliessen: () -> Unit,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val form = Rundung.LeisteOben
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = oben)
            .windowInsetsPadding(
                WindowInsets.ime.union(WindowInsets.navigationBars.add(WindowInsets(bottom = Weltmass.Leistenhoehe))),
            )
            .background(Brush.verticalGradient(0f to Farben.Flaeche, 0.4f to Farben.Bg), form)
            .border(1.dp, Weltfarben.Kante, form),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        Weltfarben.Kante,
                        Offset(0f, size.height - 0.5f),
                        Offset(size.width, size.height - 0.5f),
                        1.dp.toPx(),
                    )
                }
                .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = beiSchliessen, role = Role.Button)
                    .semantics { contentDescription = "Zurück zur Karte" },
            ) {
                Icon(Weltzeichen.Zurueck, contentDescription = null, tint = Farben.TextLeise, modifier = Modifier.size(20.dp))
            }
            if (zeichen != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Weltfarben.Hauch, Rundung.Klein)
                        .border(1.dp, Weltfarben.AkzentTief, Rundung.Klein),
                ) {
                    Icon(zeichen, contentDescription = null, tint = Weltfarben.Akzent, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                titel,
                style = Schrift.Titel.copy(fontWeight = FontWeight.W700),
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (zahl != null && zahl > 0) {
                Zahlmarke(zahl, Modifier.padding(end = Abstand.Klein))
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = Abstand.Normal, end = Abstand.Normal, top = Abstand.Normal, bottom = Abstand.Gross),
            content = inhalt,
        )
    }
}

/**
 * Die Zahlmarke der Welt — gefüllt in Amber, beschriftet in `--auf-farbe`;
 * dieselbe Form am Reiter, im Seitenkopf und unter „Mehr“. `ruft` färbt sie
 * Signalrot: Es liegt etwas bei dir, und seine Frist läuft.
 */
@Composable
fun Zahlmarke(zahl: Int, modifier: Modifier = Modifier, ruft: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .let { if (ruft) it.border(3.dp, Farben.HauchSignal, Rundung.Rund) else it }
            .defaultMinSize(minWidth = 20.dp)
            .height(20.dp)
            .background(if (ruft) Farben.Signal else Farben.Amber, Rundung.Rund)
            .padding(horizontal = Abstand.Winzig),
    ) {
        Text(
            if (zahl > 99) "99+" else "$zahl",
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 12.sp),
            color = Farben.AufFarbe,
        )
    }
}

// ===================================================================== Leiste

/**
 * Die Reiter der Welt — `Welttableiste.vue`: Lagen, Fahrzeuge, Wachen, Bauen,
 * Mehr. Zeichen über Wort, der laufende Reiter in Cyan mit Balken; „Bauen“
 * trägt sein Zeichen auf einer Bernsteinscheibe — die eine Handlung unter vier
 * Nachschlagezielen.
 *
 * <b>Zwei Fälle, in denen offene Seite und markierter Reiter nicht dasselbe
 * sind:</b> „Bauen“ gilt auch, solange auf der Karte ein Ort gewählt wird, und
 * „Mehr“ für alles dahinter.
 */
@Composable
fun Welttableiste(
    zustand: Weltzustand,
    werkbank: Werkbank,
    unten: Dp,
    zeigt: Werkzeug? = null,
) {
    val nochNichts = zustand.stand?.wachen.isNullOrEmpty()
    // Die Lektionen kennen auch Seiten ohne eigenen Reiter — der Ring wandert
    // dann auf „Mehr“, wo der Weg wirklich anfängt.
    val zeigtAuf = zeigt?.let { if (it in Werkzeug.HINTER_MEHR) Werkzeug.Mehr else it }
    val chatPost = zustand.ungelesen + if (zustand.angepingt != null) 1 else 0
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Weltfarben.Glas)
            .drawBehind { drawLine(Weltfarben.Kante, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(horizontal = 4.dp)
            .padding(bottom = unten)
            .height(Weltmass.Leistenhoehe),
    ) {
        Werkzeug.REITER.forEach { w ->
            val hier = when (w) {
                Werkzeug.Bauen -> werkbank.seite == Werkzeug.Bauen ||
                    (werkbank.seite == null && werkbank.modus != Kartenmodus.Normal)
                Werkzeug.Mehr -> werkbank.seite == Werkzeug.Mehr || werkbank.seite in Werkzeug.HINTER_MEHR
                else -> werkbank.seite == w
            }
            val marke = when (w) {
                Werkzeug.Lagen -> zustand.offeneLagen.size
                Werkzeug.Fahrzeuge -> zustand.freieFahrzeuge.size
                Werkzeug.Mehr -> chatPost
                else -> 0
            }
            val aufschrift = when {
                w == Werkzeug.Mehr && chatPost > 0 ->
                    "Mehr — $chatPost ungelesen im Chat" + if (zustand.grossAktiv) ", Großeinsatz läuft" else ""
                w == Werkzeug.Mehr && zustand.grossAktiv -> "Mehr — Großeinsatz läuft"
                w == Werkzeug.Lagen && zustand.meineOffenen > 0 ->
                    "Lagen — ${zustand.offeneLagen.size}, davon ${zustand.meineOffenen} bei dir"
                marke > 0 -> "${w.name_} — $marke"
                else -> w.name_
            }
            Reiterknopf(
                werkzeug = w,
                aufschrift = aufschrift,
                hier = hier,
                marke = marke,
                markeRuft = w == Werkzeug.Lagen && zustand.meineOffenen > 0,
                punkt = w == Werkzeug.Mehr && zustand.grossAktiv && marke == 0,
                atmet = w == Werkzeug.Bauen && nochNichts && !hier && zeigtAuf != w,
                gezeigt = zeigtAuf == w,
                beiDruck = {
                    if (w == Werkzeug.Bauen) werkbank.modus = Kartenmodus.Normal
                    werkbank.umschalten(w)
                },
            )
        }
    }
}

@Composable
private fun RowScope.Reiterknopf(
    werkzeug: Werkzeug,
    aufschrift: String,
    hier: Boolean,
    marke: Int,
    markeRuft: Boolean,
    punkt: Boolean,
    atmet: Boolean,
    gezeigt: Boolean,
    beiDruck: () -> Unit,
) {
    val bauen = werkzeug == Werkzeug.Bauen
    val farbe = when {
        bauen -> Farben.AmberHell
        hier -> Weltfarben.Akzent
        gezeigt -> Farben.AmberHell
        atmet -> Farben.Amber
        else -> Farben.TextSehrLeise
    }
    // Der Ring: atmet, solange keine Wache steht; wächst, wo die Einführung
    // hinzeigt. Nie beides — zwei Takte übereinander sehen kaputt aus.
    val ring = if (atmet || gezeigt) {
        val takt = rememberInfiniteTransition(label = "reiter")
        takt.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(if (gezeigt) 1_800 else 2_400, easing = LinearEasing), RepeatMode.Restart),
            label = "ring",
        ).value
    } else {
        null
    }
    val grund = when {
        hier && bauen -> Farben.HauchAmber
        hier -> Weltfarben.Hauch
        else -> Color.Transparent
    }
    val kante = when {
        hier && bauen -> Farben.AmberTief
        hier -> Weltfarben.AkzentTief
        else -> Color.Transparent
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(vertical = 6.dp)
            .drawBehind {
                if (ring != null) {
                    val r = 14.dp.toPx()
                    val weite = (if (gezeigt) 14.dp else 10.dp).toPx() * ring
                    drawRoundRect(
                        color = Farben.Amber.copy(alpha = (if (gezeigt) 0.55f else 0.45f) * (1f - ring)),
                        topLeft = Offset(-weite, -weite),
                        size = Size(size.width + weite * 2, size.height + weite * 2),
                        cornerRadius = CornerRadius(r + weite),
                        style = Stroke(2.dp.toPx() + weite * 0.4f),
                    )
                }
            }
            .background(grund, Rundung.Normal)
            .border(1.dp, kante, Rundung.Normal)
            .clickable(onClick = beiDruck, role = Role.Tab)
            .semantics { contentDescription = aufschrift },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            if (bauen) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .background(Brush.verticalGradient(listOf(Farben.AmberHell, Farben.Amber)), CircleShape),
                ) {
                    Icon(werkzeug.zeichen, contentDescription = null, tint = Farben.AufFarbe, modifier = Modifier.size(22.dp))
                }
            } else {
                Icon(werkzeug.zeichen, contentDescription = null, tint = farbe, modifier = Modifier.size(22.dp))
            }
            Text(werkzeug.kurz, style = Schrift.Weg.copy(fontSize = 12.sp, lineHeight = 12.sp), color = farbe, maxLines = 1)
        }
        // Der Balken unten zeigt, wo man steht — wie in der Tableiste der App.
        if (hier) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .fillMaxWidth(0.22f)
                    .height(3.dp)
                    .background(if (bauen) Farben.Amber else Weltfarben.Akzent, Rundung.Rund),
            )
        }
        if (marke > 0) {
            Zahlmarke(
                marke,
                ruft = markeRuft,
                modifier = Modifier.align(Alignment.TopCenter).offset(x = 14.dp, y = 2.dp),
            )
        } else if (punkt) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 12.dp, y = 6.dp)
                    .size(8.dp)
                    .background(Farben.Signal, CircleShape),
            )
        }
    }
}

// ======================================================================= Mehr

/**
 * „Mehr“ — `MehrBlende.vue`: die Nachschlagewerke ohne eigenen Reiter, je mit
 * einem Halbsatz, was man dort findet; abgesetzt und leise darunter der Ausgang.
 */
@Composable
fun MehrSeite(zustand: Weltzustand, werkbank: Werkbank, beiVerlassen: () -> Unit) {
    val erklaerung = mapOf(
        Werkzeug.Grosslage to "Die eine Lage der Woche — Zeit, Ort und Bereitstellung.",
        Werkzeug.Kasse to "Woher das Geld kommt, wohin es geht — und der Kredit.",
        Werkzeug.Rangliste to "Wer diese Woche am meisten verdient hat.",
        Werkzeug.Laufbahn to "Alle dreißig Stufen — was wann freischaltet.",
        Werkzeug.Einstellungen to "Gangart und Karte — wie viel gleichzeitig los ist.",
        Werkzeug.Leihe to "Fahrzeuge mieten — oder eigene gegen Credits einstellen.",
    )
    val chatPost = zustand.ungelesen + if (zustand.angepingt != null) 1 else 0
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Werkzeug.HINTER_MEHR.forEach { w ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .background(Farben.Flaeche, Rundung.Klein)
                    .border(1.dp, Farben.Rand, Rundung.Klein)
                    .clickable(role = Role.Button) { werkbank.seite = w }
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Icon(w.zeichen, contentDescription = null, tint = Farben.Amber, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                    Text(w.name_, style = Schrift.Normal.copy(fontWeight = FontWeight.W600, lineHeight = Schrift.NORMAL * 1.2f), color = Farben.Text)
                    erklaerung[w]?.let { Text(it, style = Schrift.Winzig, color = Farben.TextSehrLeise) }
                }
                // Es gibt genau einen Großeinsatz je Woche — eine Zahl wäre immer
                // „1“, also steht hier ein Wort.
                if (w == Werkzeug.Grosslage && zustand.grossAktiv) {
                    Text(
                        "LÄUFT",
                        style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                        color = Farben.AufFarbe,
                        modifier = Modifier
                            .background(Farben.Signal, Rundung.Rund)
                            .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
                    )
                } else if (w == Werkzeug.Chat && chatPost > 0) {
                    Zahlmarke(chatPost, Modifier.semantics { contentDescription = "$chatPost ungelesen" })
                }
                Icon(Weltzeichen.Weiter, contentDescription = null, tint = Farben.TextSehrLeise, modifier = Modifier.size(18.dp))
            }
        }
    }
    // Der Ausgang heißt, was er tut: Er verlässt die Welt, und nur er.
    Box(Modifier.padding(top = Abstand.Klein)) {
        Knopf(
            "Welt verlassen",
            beiVerlassen,
            art = Knopfart.Leise,
            zeichenVorn = {
                Icon(Weltzeichen.Zurueck, contentDescription = null, tint = Farben.TextLeise, modifier = Modifier.size(18.dp))
            },
        )
    }
}

// ============================================================ Kartenhinweis

/**
 * Was ein Tipp auf die Karte gerade tut — `.weltkarte__hinweis` samt der
 * Zeichenleiste aus `Weltkarte.vue`.
 *
 * Nur ein Satz in Amber über den Reitern; zurück geht es über den Reiter
 * „Bauen“ (bzw. „Fahrzeuge“). Beim Aufziehen eines Geländes kommen die beiden
 * Griffe dazu, die man beim Tippen braucht: einen Punkt zurück und „Fertig“.
 */
@Composable
fun Kartenhinweis(werkbank: Werkbank) {
    val text = when (werkbank.modus) {
        Kartenmodus.Normal -> return
        Kartenmodus.Ort -> when (werkbank.ortZweck) {
            "zweig" -> "Tipp auf die Karte, wo die zweite Leitstelle stehen soll."
            "poi" -> "Tipp auf die Karte, wo dein Punkt liegen soll."
            else -> "Tipp auf die Karte, wo die Wache stehen soll."
        }
        Kartenmodus.Gelaende -> {
            val strecke = werkbank.eckenStrecke
            val noetig = if (strecke) 2 else 3
            val n = werkbank.ecken.size
            when {
                n == 0 -> if (strecke) "Tipp die Strecke ab." else "Tipp die Ecken des Geländes ab."
                n < noetig -> "$n von mindestens $noetig Punkten — weitertippen."
                else -> {
                    val punkte = werkbank.ecken.map { it.lat to it.lon }
                    "$n Punkte · " + if (strecke) laengenwort(zuglaengeMeter(punkte)) else flaechenwort(vieleckQuadratmeter(punkte))
                }
            }
        }
        Kartenmodus.Pfad -> {
            val n = werkbank.pfad.size
            if (n == 0) "Tipp die Stationen der Streife ab." else "$n ${if (n == 1) "Station" else "Stationen"} — höchstens acht."
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.padding(horizontal = Abstand.Klein).fillMaxWidth(),
    ) {
        Text(
            text,
            style = Schrift.MonoKlein,
            color = Farben.AmberHell,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.FlaecheHoch.copy(alpha = 0.92f), Rundung.Rund)
                .border(1.dp, Farben.Amber, Rundung.Rund)
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        )
        if (werkbank.modus == Kartenmodus.Gelaende) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf(
                    "Punkt zurück",
                    { werkbank.ecken = werkbank.ecken.dropLast(1) },
                    modifier = Modifier.weight(1f),
                    aktiv = werkbank.ecken.isNotEmpty(),
                    kompakt = true,
                    breit = true,
                )
                Knopf(
                    "Fertig",
                    {
                        werkbank.modus = Kartenmodus.Normal
                        werkbank.zurueckZurSeite()
                    },
                    modifier = Modifier.weight(1f),
                    art = Knopfart.Haupt,
                    kompakt = true,
                    breit = true,
                )
            }
        }
    }
}

// ============================================================== Chathinweis

/**
 * Die Benachrichtigung aus dem Weltchat — `ChatHinweis.vue`.
 *
 * <b>Warum es sie gibt.</b> Der Chat steht am Handy hinter „Mehr“; Post an
 * einen selbst war dort nur eine Zahl, und wer disponiert, schaut auf die Karte.
 * Jetzt erscheint oben, unter dem Kopf und über die ganze Breite, wer
 * geschrieben hat, wo, wann und die ersten Worte. Ein Tipp öffnet den Chat,
 * das Kreuz nimmt nur die Meldung weg; nach acht Sekunden geht sie von selbst.
 */
@Composable
fun ChatHinweis(
    welt: Welt,
    zustand: Weltzustand,
    beiOeffnen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val h = zustand.chatHinweis ?: return
    LaunchedEffect(h) {
        delay(8_000)
        welt.chatHinweisWeg()
    }
    val ping = h.art == "ping"
    val name = h.von ?: "Jemand"
    val kopf = when (h.art) {
        "direkt" -> "$name schreibt dir"
        "ping" -> "$name hat dich erwähnt"
        else -> name
    }
    val wo = if (h.art == "direkt") "Direktnachricht" else "Weltchat"
    val zeit = java.time.Instant.ofEpochMilli(h.um).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    val kante = if (ping) Farben.Signal else Farben.Amber
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .padding(horizontal = Abstand.Normal)
            .fillMaxWidth()
            .background(Farben.FlaecheHoch.copy(alpha = 0.94f), Rundung.Normal)
            .border(1.dp, if (ping) Farben.Rand else Farben.AmberTief, Rundung.Normal)
            .drawBehind {
                drawRoundRect(kante, size = Size(3.dp.toPx(), size.height), cornerRadius = CornerRadius(3.dp.toPx()))
            }
            .padding(Abstand.Klein)
            .semantics { contentDescription = "$kopf · $wo · $zeit: ${h.text}" },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .weight(1f)
                .clickable {
                    welt.chatHinweisWeg()
                    beiOeffnen()
                },
        ) {
            Icon(Weltzeichen.Chat, contentDescription = null, tint = kante, modifier = Modifier.padding(top = 2.dp).size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(kopf, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$wo · $zeit", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                Text(h.text, style = Schrift.Klein, color = Farben.TextLeise, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clickable(role = Role.Button) { welt.chatHinweisWeg() }
                .semantics { contentDescription = "Benachrichtigung schließen" },
        ) {
            Icon(Weltzeichen.Kreuz, contentDescription = null, tint = Farben.TextLeise, modifier = Modifier.size(16.dp))
        }
    }
}
