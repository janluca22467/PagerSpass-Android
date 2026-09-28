package de.pagerspass.pagerspass.ansichten

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.karte.Lagekarte
import de.pagerspass.pagerspass.ui.schmuck.Melderkatalog
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Melder — das Gerät, das den Alarm zeigt, in allen Bauformen des Webs.
 *
 * Übertragen aus `web/src/components/melder/`: Dienstgerät (`DmePager`), Klassik,
 * Farbe, Alarmfax, Quermelder, Lamellen-, Leucht- und Bogenmelder, Einsatzuhr und
 * Alarmmonitor. <b>Nicht die Pixel sind übertragen, sondern die Bauform</b>: Gehäuse,
 * Display, Tasten an ihrer Stelle, und die Bedienung der Tasten Zeile für Zeile wie im
 * Web — ein Quermelder quittiert oben auf dem Rücken, ein Fax mit dem Abriss.
 *
 * Was die Geräte anzeigen, steht nicht hier, sondern in [Melderanzeige]: Alle zeigen
 * dieselbe Meldung, und sie sollen sie gleich zeigen.
 *
 * <b>Der selbst gebaute Melder fehlt</b> (`EigenMelder`, gebaut in der
 * Gehäusewerkstatt am Rechner): Die Werkstatt gibt es am Handy nicht, und ihr
 * Bauplan liegt im Speicher des Browsers, nicht am Konto.
 */

/** Was jedes Gerät zum Arbeiten braucht — ein Bündel statt zwölf Parametern. */
class Meldergriffe(
    val quittieren: () -> Unit,
    val ausruecken: () -> Unit,
    /** Der Klick einer Taste am Gehäuse. */
    val taste: () -> Unit = {},
)

/**
 * Der Melderschacht — `MelderGeraet.vue`: das gewählte Gerät. Eine Bauform, die dem
 * Konto (noch) nicht offensteht, fällt zurück aufs Dienstgerät.
 */
@Composable
fun MelderGeraet(
    bauform: String,
    stufe: Int,
    premium: Boolean,
    gesichtId: String?,
    melder: Melderstand,
    verlauf: List<Alarmmeldung>,
    fahrzeug: Rundenfahrzeug,
    kennung: String,
    raum: Raumzustand,
    menue: Meldermenue,
    lautlos: Boolean,
    griffe: Meldergriffe,
    modifier: Modifier = Modifier,
) {
    val eintrag = Melderkatalog.BAUFORMEN.firstOrNull { it.id == bauform }
    val frei = eintrag != null && (if (eintrag.premium) premium else stufe >= eintrag.abLevel)
    val form = if (frei || bauform == "monitor") bauform else "dienst"
    val gesicht = Melderkatalog.GESICHTER.firstOrNull { it.id == gesichtId } ?: Melderkatalog.GESICHTER.first()
    val schleife = fahrzeug.schleifen.firstOrNull().orEmpty()

    // Ein neuer Alarm schließt das Menü — der Schirm gehört immer genau einer Sache.
    LaunchedEffect(melder.hatAlarm) { if (melder.hatAlarm) menue.schliessen() }

    val zeilen = when (form) {
        "klassik" -> 5
        "lamellen" -> 4
        "leucht" -> 3
        "fax", "monitor", "uhr" -> null
        else -> 6
    }
    val anzeige = rememberMelderanzeige(melder, verlauf, kennung, schleife, zeilen)
    val schleifen = fahrzeug.schleifen.joinToString(" / ").ifBlank { "keine Schleife" }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        when (form) {
            "klassik" -> Klassikmelder(anzeige, menue, gesicht, kennung, schleife, lautlos, griffe)
            "farbe" -> Farbmelder(anzeige, menue, gesicht, kennung, fahrzeug, lautlos, griffe)
            "fax" -> Alarmfax(anzeige, griffe)
            "quad" -> Quermelder(anzeige, menue, gesicht, kennung, schleife, lautlos, griffe)
            "lamellen" -> Lamellenmelder(anzeige, menue, gesicht, kennung, schleife, griffe)
            "leucht" -> Leuchtmelder(anzeige, menue, kennung, lautlos, griffe)
            "bogen" -> Bogenmelder(anzeige, menue, gesicht, kennung, schleife, lautlos, griffe)
            "uhr" -> Einsatzuhr(anzeige, gesicht, kennung, griffe)
            "monitor" -> Alarmmonitor(anzeige, raum, fahrzeug, kennung, griffe)
            else -> Dienstmelder(anzeige, menue, gesicht, kennung, schleife, griffe)
        }

        if (form != "fax") Melderspeicher(anzeige, wort = if (form == "monitor") "Einsätze der Schicht" else "Speicher")

        SehrLeise(
            when (form) {
                "klassik" -> {
                    val seiten = if (anzeige.zeilen.size > 5) "${(anzeige.zeilen.size + 4) / 5} Seiten · " else ""
                    "$seiten$schleifen"
                }
                "farbe" -> "FM-9c · $schleifen"
                "fax" -> "Fax · $schleifen"
                "quad" -> "QM-15 · $schleifen"
                "lamellen" -> "LM-4 · $schleifen"
                "leucht" -> "LT-4 · $schleifen"
                "bogen" -> "BG-22 · $schleifen"
                "uhr", "monitor" -> schleifen
                else -> "DME · $schleifen"
            },
            mono = true,
        )
    }
}

// --------------------------------------------------------------- Bausteine

/** Das Blinken der Alarmleuchte — ein Takt für alle Gehäuse. */
@Composable
private fun alarmblinken(an: Boolean): Float {
    if (!an) return 0f
    val takt = rememberInfiniteTransition(label = "melderleuchte")
    val wert by takt.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "melderleuchte-wert",
    )
    return wert
}

/** Eine Leuchte am Gehäuse — rot, wenn sie brennt. */
@Composable
private fun Leuchte(an: Boolean, farbe: Color = Farben.Signal, groesse: Dp = 10.dp) {
    val glut = alarmblinken(an)
    Box(
        Modifier
            .size(groesse)
            .background(if (an) farbe.copy(alpha = glut) else Color(0xFF2A2D33), CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape),
    )
}

/** Das Gehäuse eines Handgeräts — Schale, Rundung, Lichtkante oben. */
@Composable
private fun Gehaeuse(
    farbe: Color,
    modifier: Modifier = Modifier,
    ecke: Dp = 22.dp,
    breite: Dp = 300.dp,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val form = RoundedCornerShape(ecke)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = breite)
            .fillMaxWidth()
            .clip(form)
            .background(Brush.verticalGradient(listOf(farbe.heller(0.18f), farbe, farbe.dunkler(0.25f))), form)
            .border(1.dp, Color.White.copy(alpha = 0.10f), form)
            .padding(Abstand.Normal),
        content = inhalt,
    )
}

private fun Color.heller(anteil: Float) = Color(
    red + (1f - red) * anteil,
    green + (1f - green) * anteil,
    blue + (1f - blue) * anteil,
    alpha,
)

private fun Color.dunkler(anteil: Float) = Color(red * (1f - anteil), green * (1f - anteil), blue * (1f - anteil), alpha)

/** Ob eine Displayfarbe hell ist — dann steht dunkle Schrift darauf. */
private fun Color.istHell(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) > 0.5f

/** Eine Gehäusetaste. */
@Composable
private fun Taste(
    aufschrift: String,
    beiDruck: () -> Unit,
    modifier: Modifier = Modifier,
    farbe: Color = Color(0xFF3A3E46),
    schrift: Color = Farben.Text,
    aktiv: Boolean = true,
    dringend: Boolean = false,
    form: RoundedCornerShape = RoundedCornerShape(8.dp),
    hoehe: Dp = 40.dp,
) {
    val glut = alarmblinken(dringend)
    val grund = if (dringend) Farben.Signal.copy(alpha = 0.55f + 0.45f * glut) else farbe
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(hoehe)
            .clip(form)
            .background(
                Brush.verticalGradient(listOf(grund.heller(0.15f), grund.dunkler(0.2f))),
                form,
            )
            .border(1.dp, Color.Black.copy(alpha = 0.45f), form)
            .clickable(enabled = aktiv, role = Role.Button, onClick = beiDruck)
            .padding(horizontal = Abstand.Klein),
    ) {
        Text(
            text = aufschrift,
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
            color = if (aktiv) schrift else schrift.copy(alpha = 0.35f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Ein Display — Glas mit Rand, darin Zeilen in der Schrift des Geräts. */
@Composable
private fun Display(
    grund: Color,
    modifier: Modifier = Modifier,
    hoehe: Dp = 150.dp,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(hoehe)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(grund.heller(0.08f), grund, grund.dunkler(0.12f))))
            .border(2.dp, Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
        content = inhalt,
    )
}

/** Eine Displayzeile in Monospace. */
@Composable
private fun Zeile(text: String, farbe: Color, kopf: Boolean = false, groesse: Int = 13) {
    Text(
        text = text,
        style = Schrift.MonoKlein.copy(
            fontSize = groesse.sp,
            lineHeight = (groesse + 3).sp,
            fontWeight = if (kopf) FontWeight.Bold else FontWeight.Normal,
        ),
        color = farbe,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Das Menü auf einem Display — der Balken ist die Auswahl. */
@Composable
private fun ColumnScope.Menueliste(menue: Meldermenue, schrift: Color, balken: Color) {
    val sichtbar = 6
    val start = (menue.auswahl - sichtbar + 1).coerceAtLeast(0)
    menue.eintraege.drop(start).take(sichtbar).forEachIndexed { i, e ->
        val gewaehlt = start + i == menue.auswahl
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (gewaehlt) balken else Color.Transparent)
                .padding(horizontal = 2.dp),
        ) {
            Text(
                e.name,
                style = Schrift.MonoKlein.copy(fontSize = 12.sp),
                color = if (gewaehlt) (if (balken.istHell()) Color.Black else Color.White) else schrift,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (e.wert.isNotEmpty()) {
                Text(
                    e.wert,
                    style = Schrift.MonoKlein.copy(fontSize = 12.sp),
                    color = if (gewaehlt) (if (balken.istHell()) Color.Black else Color.White) else schrift,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Die Zeilen der Meldung oder das Ruhebild, mit Cursor und Blättermarken. */
@Composable
private fun ColumnScope.Meldungszeilen(
    anzeige: Melderanzeige,
    ruhe: List<String>,
    schrift: Color,
    groesse: Int = 13,
) {
    val zeilen = if (anzeige.meldungSteht) anzeige.angezeigt else ruhe
    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            zeilen.forEachIndexed { i, z -> Zeile(z, schrift, kopf = i == 0, groesse = groesse) }
            if (anzeige.laeuftEin) Zeile("▌", schrift, groesse = groesse)
        }
        if (anzeige.meldungSteht && (anzeige.mehrOben || anzeige.mehrUnten)) {
            Text(
                (if (anzeige.mehrOben) "▲" else "") + (if (anzeige.mehrUnten) "▼" else ""),
                style = Schrift.MonoKlein.copy(fontSize = 11.sp),
                color = schrift,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

/** Der Speicher: die letzten Alarme dieser Schicht, aufklappbar unter dem Gerät. */
@Composable
private fun Melderspeicher(anzeige: Melderanzeige, wort: String) {
    val zahl = anzeige.vergangene.size
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(Rundung.Rund)
            .border(1.dp, if (anzeige.speicherOffen) Farben.Amber else Farben.Rand, Rundung.Rund)
            .clickable(enabled = zahl > 0, role = Role.Button) { anzeige.speicherOffen = !anzeige.speicherOffen }
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig),
    ) {
        Text(wort, style = Schrift.MonoKlein, color = if (zahl > 0) Farben.Text else Farben.TextSehrLeise)
        Text(zahl.toString(), style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
    }
    if (anzeige.speicherOffen && zahl > 0) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .background(Farben.BgTief, Rundung.Klein)
                .border(1.dp, Farben.Rand, Rundung.Klein)
                .padding(Abstand.Klein),
        ) {
            anzeige.vergangene.forEach { a -> Verlaufszeile(a) }
        }
    }
}

/** Eine Zeile im Speicher: Zeit, Stichwort in der Farbe der Dringlichkeit, Ort. */
@Composable
internal fun Verlaufszeile(a: Alarmmeldung) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Text(uhrzeit(a.zeit), style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
        Text(
            a.stichwort,
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
            color = when {
                a.prioritaet >= 3 -> Farben.SignalHell
                a.prioritaet <= 1 -> Farben.TextLeise
                else -> Farben.AmberHell
            },
        )
        Text(a.adresse, style = Schrift.MonoKlein, color = Farben.TextLeise, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Die Ruhezeilen der meisten Gehäuse: Kennung, Meldungszahl, Profil (ohne Abo die Schleife). */
private fun ruhezeilen(anzeige: Melderanzeige, menue: Meldermenue, kennung: String, schleife: String): List<String> =
    listOf(
        kennung.ifBlank { "----" },
        if (anzeige.vergangene.isNotEmpty()) "${anzeige.vergangene.size} Meldungen" else "Keine Meldungen",
        if (menue.darfMenue) menue.kopfwort else schleife,
    ).filter { it.isNotBlank() }

/**
 * Die Mitteltaste der Drei-Tasten-Gehäuse: <b>der Alarm gewinnt, danach das
 * Menü</b> — quittiert bei Alarm, wählt im offenen Menü, quittiert eine stehende
 * Meldung, öffnet sonst das Menü (Abo).
 */
private fun mitteltaste(anzeige: Melderanzeige, menue: Meldermenue, griffe: Meldergriffe) {
    when {
        anzeige.hatAlarm -> griffe.quittieren()
        menue.offen -> menue.waehlen()
        anzeige.meldungSteht -> griffe.quittieren()
        menue.darfMenue -> menue.menueOeffnen()
    }
}

private fun mittelwort(anzeige: Melderanzeige, menue: Meldermenue): String = when {
    anzeige.hatAlarm -> "QUITTIEREN"
    menue.offen -> "WÄHLEN"
    anzeige.meldungSteht -> "QUITTIERT"
    menue.darfMenue -> "MENÜ"
    else -> "RESET"
}

private fun hoch(anzeige: Melderanzeige, menue: Meldermenue, griffe: Meldergriffe) {
    if (menue.offen) menue.bewegen(-1) else {
        griffe.taste()
        anzeige.blaettern(-1)
    }
}

private fun runter(anzeige: Melderanzeige, menue: Meldermenue, griffe: Meldergriffe) {
    if (menue.offen) menue.bewegen(1) else {
        griffe.taste()
        anzeige.blaettern(1)
    }
}

// ----------------------------------------------------------------- Geräte

/** Das Dienstgerät — flaches Gehäuse, LC-Display mit blauer Kopfleiste, drei Tasten. */
@Composable
private fun Dienstmelder(
    anzeige: Melderanzeige,
    menue: Meldermenue,
    gesicht: Melderkatalog.Gesicht,
    kennung: String,
    schleife: String,
    griffe: Meldergriffe,
) {
    val schrift = if (gesicht.lcd.istHell()) Color(0xFF1C2410) else Color(0xFFE9F0D0)
    Gehaeuse(gesicht.gehaeuse) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Lautsprechergitter(Modifier.weight(1f))
            Spacer(Modifier.width(Abstand.Normal))
            Leuchte(anzeige.hatAlarm)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF07080A), RoundedCornerShape(10.dp))
                .padding(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        ) {
            Display(gesicht.lcd, hoehe = 138.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E4F9C)).padding(horizontal = 3.dp),
                ) {
                    Text("${anzeige.datumKurz} ${anzeige.zeitKurz}", style = Schrift.MonoKlein.copy(fontSize = 11.sp), color = Color.White, modifier = Modifier.weight(1f))
                    if (menue.darfMenue) Text(menue.kopfwort, style = Schrift.MonoKlein.copy(fontSize = 11.sp), color = Color.White)
                }
                if (menue.offen) {
                    Menueliste(menue, schrift, schrift)
                } else {
                    Meldungszeilen(anzeige, ruhezeilen(anzeige, menue, kennung, schleife), schrift)
                }
            }
            Text("DME 1", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.End))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            Taste("▲", { hoch(anzeige, menue, griffe) }, Modifier.weight(1f))
            Taste(
                mittelwort(anzeige, menue),
                { mitteltaste(anzeige, menue, griffe) },
                Modifier.weight(2f),
                aktiv = !(anzeige.meldungSteht && !anzeige.hatAlarm) || menue.offen,
                dringend = anzeige.hatAlarm,
            )
            Taste("▼", { runter(anzeige, menue, griffe) }, Modifier.weight(1f))
        }
        Ladekontakte()
    }
}

/** Der Klassiker — kleiner Piepser mit gelber Quittungstaste und grünem Glas. */
@Composable
private fun Klassikmelder(
    anzeige: Melderanzeige,
    menue: Meldermenue,
    gesicht: Melderkatalog.Gesicht,
    kennung: String,
    schleife: String,
    lautlos: Boolean,
    griffe: Meldergriffe,
) {
    val glas = Color(0xFF9FB36A)
    val schrift = Color(0xFF1E2A0F)
    Gehaeuse(gesicht.gehaeuse, breite = 260.dp, ecke = 30.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Lautsprechergitter(Modifier.weight(1f))
            Spacer(Modifier.width(Abstand.Normal))
            Leuchte(anzeige.hatAlarm)
        }
        Display(glas, hoehe = 128.dp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("▮▮▮", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift)
                Text(if (anzeige.vergangene.isNotEmpty()) "✉" else " ", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift)
                Text(if (lautlos) "🔕" else "🔔", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift, modifier = Modifier.weight(1f))
                Text(anzeige.zeitKurz, style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift)
            }
            if (menue.offen) {
                Menueliste(menue, schrift, schrift)
            } else {
                Meldungszeilen(anzeige, ruhezeilen(anzeige, menue, kennung, schleife), schrift, groesse = 12)
            }
        }
        Text("MELDER FM-4", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.55f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Taste(
                if (anzeige.hatAlarm) "QUITTIEREN" else if (menue.offen) "WÄHLEN" else "●",
                { mitteltaste(anzeige, menue, griffe) },
                Modifier.weight(1f),
                farbe = Color(0xFFF2C230),
                schrift = Color(0xFF1A1200),
                aktiv = !(anzeige.meldungSteht && !anzeige.hatAlarm) &&
                    (anzeige.meldungSteht || menue.offen || menue.darfMenue),
                dringend = anzeige.hatAlarm,
                form = RoundedCornerShape(20.dp),
                hoehe = 46.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                Taste("▲", { hoch(anzeige, menue, griffe) }, Modifier.width(44.dp), form = CircleShape, hoehe = 32.dp)
                Taste("▼", { runter(anzeige, menue, griffe) }, Modifier.width(44.dp), form = CircleShape, hoehe = 32.dp)
            }
        }
    }
}

/** Der Farbmelder — Farbdisplay mit drei Softkeys, Steuerkreuz und Lautstärkewippen. */
@Composable
private fun Farbmelder(
    anzeige: Melderanzeige,
    menue: Meldermenue,
    gesicht: Melderkatalog.Gesicht,
    kennung: String,
    fahrzeug: Rundenfahrzeug,
    lautlos: Boolean,
    griffe: Meldergriffe,
) {
    val alarm = anzeige.aktuell
    val prio = alarm?.prioritaet ?: 1
    val prioFarbe = when {
        prio >= 3 -> Color(0xFFE5352B)
        prio <= 1 -> Color(0xFF3B82C4)
        else -> Color(0xFFF08C00)
    }
    Gehaeuse(gesicht.gehaeuse) {
        Text("FM-9c", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.6f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0B1220))
                .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().background(Color(0xFF16213A)).padding(horizontal = 4.dp),
            ) {
                Text("▂▄▆█", style = Schrift.MonoKlein.copy(fontSize = 9.sp), color = Color.White)
                Text(if (anzeige.vergangene.isNotEmpty()) "✉" else " ", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = Color.White)
                Text(if (lautlos) "🔕" else "🔔", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("▭", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E4F9C)).padding(horizontal = 4.dp)) {
                Text("${anzeige.datumKurz} ${anzeige.zeitKurz}", style = Schrift.MonoKlein.copy(fontSize = 11.sp), color = Color.White, modifier = Modifier.weight(1f))
                if (menue.darfMenue) Text(menue.kopfwort, style = Schrift.MonoKlein.copy(fontSize = 11.sp), color = Color.White)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxWidth().weight(1f).padding(4.dp),
            ) {
                when {
                    menue.offen -> Menueliste(menue, Color.White, Color(0xFF3B82C4))
                    alarm != null -> {
                        Row(modifier = Modifier.fillMaxWidth().background(prioFarbe).padding(horizontal = 3.dp)) {
                            Zeile("Ruf ${alarm.einsatznummer.ifBlank { "—" }} ${prioZeichen(prio)}", Color.White, kopf = true)
                        }
                        val blatt = listOf(
                            prioWort(prio).uppercase(),
                            fahrzeug.funkrufname.ifBlank { "—" },
                            "${alarm.stichwort} ${alarm.stichwortText}",
                            alarm.adresse,
                            alarm.ortsteil.orEmpty(),
                            alarm.meldebild,
                            anzeige.zusatz.orEmpty(),
                            alarm.schleife + (alarm.funkgruppe?.let { " · $it" } ?: ""),
                        ).filter { it.isNotBlank() }
                        blatt.forEach { Zeile(it, Color.White, groesse = 12) }
                    }
                    menueAus(menue) -> {
                        Zeile("Gerät aus", Color.White, kopf = true)
                        Zeile("Kein Ton, keine Vibration.", Color.White, groesse = 11)
                        Zeile("Meldungen kommen weiter an.", Color.White, groesse = 11)
                    }
                    else -> {
                        Text(anzeige.zeitKurz, style = Schrift.MonoNormal.copy(fontSize = 32.sp), color = Color.White)
                        Zeile(anzeige.datumKurz, Color.White)
                        Zeile(if (anzeige.vergangene.isNotEmpty()) "${anzeige.vergangene.size} Meldungen" else "Keine Meldungen", Color.White)
                        Zeile("${kennung.ifBlank { "----" }} · betriebsbereit", Color.White.copy(alpha = 0.7f), groesse = 11)
                    }
                }
            }
            // Die drei Softkeys — ihre Aufschrift steht auf dem Schirm, nicht auf der Taste.
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF16213A))) {
                val links: Pair<String, () -> Unit> = when {
                    menue.offen -> "Zurück" to { menue.zurueck() }
                    anzeige.meldungSteht -> "Annehmen" to griffe.ausruecken
                    else -> (if (menue.darfMenue) "Menü" else "") to { if (menue.darfMenue) menue.menueOeffnen() }
                }
                val mitte: Pair<String, () -> Unit> = if (menue.offen) {
                    "Menü" to { menue.menueOeffnen() }
                } else {
                    "▤" to { anzeige.speicherOffen = !anzeige.speicherOffen }
                }
                val rechts: Pair<String, () -> Unit> = if (menue.offen) {
                    "Wählen" to { menue.waehlen() }
                } else {
                    "Ablehnen" to { if (anzeige.hatAlarm) griffe.quittieren() }
                }
                listOf(links, mitte, rechts).forEach { (wort, tun) ->
                    Text(
                        wort,
                        style = Schrift.MonoKlein.copy(fontSize = 11.sp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clickable(role = Role.Button, onClick = tun).padding(vertical = 4.dp),
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Taste("▲", { hoch(anzeige, menue, griffe) }, Modifier.width(56.dp), hoehe = 30.dp)
                Text("◀  ●  ▶", style = Schrift.MonoKlein.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.4f))
                Taste("▼", { runter(anzeige, menue, griffe) }, Modifier.width(56.dp), hoehe = 30.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                Taste("+", { menue.lauter(0.2f) }, Modifier.width(44.dp), form = CircleShape, hoehe = 32.dp)
                Taste("−", { menue.lauter(-0.2f) }, Modifier.width(44.dp), form = CircleShape, hoehe = 32.dp)
            }
        }
    }
}

/** Ob das Gerät ausgeschaltet ist — steht im Kopfwort. */
private fun menueAus(menue: Meldermenue): Boolean = menue.kopfwort == "Aus"

/** Das Formzeichen der Dringlichkeit — `prioZeichen` in types.ts. */
internal fun prioZeichen(prio: Int): String = when {
    prio >= 3 -> "▲▲"
    prio <= 1 -> "▽"
    else -> ""
}

/** Und das Wort dazu. */
internal fun prioWort(prio: Int): String = when {
    prio >= 3 -> "Priorität hoch"
    prio <= 1 -> "Priorität gering"
    else -> "Priorität normal"
}

/** Das Alarmfax — druckt die Meldung; abreißen quittiert. */
@Composable
private fun Alarmfax(anzeige: Melderanzeige, griffe: Meldergriffe) {
    val alarm = anzeige.aktuell
    val imGeraet = if (anzeige.hatAlarm) alarm else null
    var geholt by remember { mutableStateOf<Alarmmeldung?>(null) }
    var korbOffen by remember { mutableStateOf(false) }
    val korb = if (alarm != null && !anzeige.hatAlarm) listOf(alarm) + anzeige.vergangene else anzeige.vergangene
    val abrissBereit = imGeraet != null && !anzeige.laeuftEin
    val zustand = when {
        anzeige.laeuftEin -> "EMPFANG…"
        imGeraet != null -> "BEREIT ZUM ABRISS"
        anzeige.meldungSteht -> "ABGERISSEN"
        else -> "BEREIT ${anzeige.zeitKurz}"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFD9D6CC), Color(0xFFB7B3A6))), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF8E8A7E), RoundedCornerShape(12.dp))
                .padding(Abstand.Normal),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("ALARMFAX AF-1", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2A2A2A), modifier = Modifier.weight(1f))
                Leuchte(anzeige.hatAlarm)
            }
            Text(
                zustand,
                style = Schrift.MonoKlein,
                color = Color(0xFF9FE870),
                modifier = Modifier.fillMaxWidth().background(Color(0xFF14200E), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFF2A2A2A), RoundedCornerShape(5.dp)))
        }
        Taste(
            if (anzeige.laeuftEin) "Druck läuft …" else if (imGeraet != null) "Papier abreißen" else "Kein Papier",
            {
                if (abrissBereit) {
                    griffe.taste()
                    griffe.quittieren()
                }
            },
            Modifier.fillMaxWidth(),
            aktiv = abrissBereit,
            dringend = abrissBereit,
        )
        if (imGeraet != null) {
            Faxbogen(imGeraet, anzeige.sichtbar)
        } else {
            SehrLeise(if (anzeige.meldungSteht) "Im Papierkorb ↓" else "Keine Meldung", mono = true)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(Rundung.Rund)
                .border(1.dp, if (korbOffen) Farben.Amber else Farben.Rand, Rundung.Rund)
                .clickable(enabled = korb.isNotEmpty(), role = Role.Button) { korbOffen = !korbOffen }
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig),
        ) {
            Text("🗑 Papierkorb", style = Schrift.MonoKlein, color = Farben.Text)
            Text(korb.size.toString(), style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
        }
        if (korbOffen && korb.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.fillMaxWidth()) {
                korb.forEach { a ->
                    Box(Modifier.fillMaxWidth().clickable(role = Role.Button) { geholt = a }) { Verlaufszeile(a) }
                }
            }
        }
        geholt?.takeIf { g -> korb.any { it.incidentId == g.incidentId } }?.let { blatt ->
            Faxbogen(blatt, Int.MAX_VALUE)
            Taste("Zurück in den Korb", { geholt = null }, Modifier.fillMaxWidth(), aktiv = true)
        }
    }
}

/** Ein bedruckter Faxbogen: Etikett und Wert je Zeile, soweit schon gedruckt. */
@Composable
private fun Faxbogen(alarm: Alarmmeldung, gedruckt: Int) {
    val zeilen = listOf(
        "ALARMFAX" to uhrzeit(alarm.zeit),
        "Schleife" to alarm.schleife,
        "Stichwort" to "${alarm.stichwort} ${alarm.stichwortText}",
        "Adresse" to alarm.adresse,
        "Ortsteil" to alarm.ortsteil.orEmpty(),
        "Meldebild" to alarm.meldebild,
        "Einheiten" to alarm.einheiten.joinToString(", "),
        "Leitstelle" to alarm.zusatztext.orEmpty(),
    ).filter { it.second.isNotBlank() }.take(gedruckt.coerceAtLeast(0))
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F4EA), RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFFD6D0BD), RoundedCornerShape(2.dp))
            .padding(Abstand.Normal),
    ) {
        zeilen.forEachIndexed { i, (etikett, wert) ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(etikett, style = Schrift.MonoKlein.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = Color(0xFF303030), modifier = Modifier.width(78.dp))
                Text(wert, style = Schrift.MonoKlein.copy(fontSize = 12.sp, fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal), color = Color(0xFF151515))
            }
        }
    }
}

/** Der Quermelder — liegendes Gehäuse, Tasten oben auf dem Rücken. */
@Composable
private fun Quermelder(
    anzeige: Melderanzeige,
    menue: Meldermenue,
    gesicht: Melderkatalog.Gesicht,
    kennung: String,
    schleife: String,
    lautlos: Boolean,
    griffe: Meldergriffe,
) {
    val schrift = if (gesicht.lcd.istHell()) Color(0xFF1C2410) else Color(0xFFE9F0D0)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth()) {
        // Die Oberleiste: breite Quittungstaste und zwei kleine.
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
            Taste(
                mittelwort(anzeige, menue),
                { mitteltaste(anzeige, menue, griffe) },
                Modifier.weight(2f),
                aktiv = !(anzeige.meldungSteht && !anzeige.hatAlarm && !menue.offen && !menue.darfMenue),
                dringend = anzeige.hatAlarm,
                form = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                hoehe = 30.dp,
            )
            Taste("▲", { hoch(anzeige, menue, griffe) }, Modifier.weight(1f), form = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp), hoehe = 30.dp)
            Taste("▼", { runter(anzeige, menue, griffe) }, Modifier.weight(1f), form = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp), hoehe = 30.dp)
        }
        Gehaeuse(gesicht.gehaeuse, breite = 380.dp, ecke = 28.dp) {
            Display(gesicht.lcd, hoehe = 118.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("▮▮▮", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift)
                    Text(if (anzeige.vergangene.isNotEmpty()) "✉" else " ", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift)
                    Text(if (lautlos) "🔕" else "🔔", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift)
                    Text(anzeige.zeitKurz, style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift, modifier = Modifier.weight(1f))
                    if (menue.darfMenue) Text(menue.kopfwort, style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift)
                }
                if (menue.offen) {
                    Menueliste(menue, schrift, schrift)
                } else {
                    val ruhe = listOf(anzeige.datumKurz) + ruhezeilen(anzeige, menue, kennung, schleife)
                    Meldungszeilen(anzeige, ruhe, schrift)
                }
            }
            Text("QUER 15", style = Schrift.MonoKlein.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color.White.copy(alpha = 0.5f))
        }
    }
}

/** Der Lamellenmelder — vier gewölbte Lamellen, Textdisplay, roter Quittungsblock. */
@Composable
private fun Lamellenmelder(
    anzeige: Melderanzeige,
    menue: Meldermenue,
    gesicht: Melderkatalog.Gesicht,
    kennung: String,
    schleife: String,
    griffe: Meldergriffe,
) {
    val schrift = Color(0xFF203010)
    val tastenwort = when {
        anzeige.hatAlarm -> "Quittieren"
        anzeige.meldungSteht -> "Quittiert"
        menue.offen -> "Wählen"
        menue.darfMenue -> "Menü"
        else -> "Zurücksetzen"
    }
    Gehaeuse(gesicht.gehaeuse, breite = 280.dp, ecke = 34.dp) {
        Text("LAMELLE", style = Schrift.MonoKlein.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color.White.copy(alpha = 0.55f))
        // Die Lamellen — die obere und die untere blättern.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .drawBehind {
                    val streifen = 4
                    val h = size.height / streifen
                    for (i in 0 until streifen) {
                        val oben = i * h
                        val pfad = Path().apply {
                            moveTo(0f, oben + h * 0.08f)
                            quadraticTo(size.width / 2, oben + h * 0.32f, size.width, oben)
                            lineTo(size.width, oben + h * 0.84f)
                            quadraticTo(size.width / 2, oben + h * 1.12f, 0f, oben + h * 0.92f)
                            close()
                        }
                        drawPath(pfad, Color.White.copy(alpha = 0.07f))
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.55f),
                            topLeft = Offset(size.width * 0.3f, oben + h * 0.42f),
                            size = Size(size.width * 0.4f, h * 0.14f),
                            cornerRadius = CornerRadius(6f, 6f),
                        )
                    }
                },
        ) {
            Column(Modifier.matchParentSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).clickable(role = Role.Button) { hoch(anzeige, menue, griffe) })
                Box(Modifier.fillMaxWidth().weight(1f))
                Box(Modifier.fillMaxWidth().weight(1f))
                Box(Modifier.fillMaxWidth().weight(1f).clickable(role = Role.Button) { runter(anzeige, menue, griffe) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Display(Color(0xFFB5C98A), modifier = Modifier.weight(1f), hoehe = 92.dp) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(anzeige.zeitKurz, style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift, modifier = Modifier.weight(1f))
                    Text(if (menue.darfMenue) menue.kopfwort else (if (schleife.isNotEmpty()) "Ad1" else "--"), style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift)
                }
                if (menue.offen) Menueliste(menue, schrift, schrift) else Meldungszeilen(anzeige, ruhezeilen(anzeige, menue, kennung, schleife), schrift, groesse = 12)
            }
            Taste(
                "⬤",
                {
                    when {
                        anzeige.hatAlarm -> griffe.quittieren()
                        menue.offen -> menue.waehlen()
                        anzeige.meldungSteht -> griffe.quittieren()
                        menue.darfMenue -> menue.menueOeffnen()
                    }
                },
                Modifier.width(56.dp),
                farbe = Color(0xFFC62D24),
                aktiv = !(anzeige.meldungSteht && !anzeige.hatAlarm && !menue.offen && !menue.darfMenue),
                dringend = anzeige.hatAlarm,
                hoehe = 56.dp,
            )
        }
        SehrLeise(tastenwort, mono = true)
        Text("LM 4", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.55f))
    }
}

/** Der Leuchtmelder — liegendes Kissen, grün hinterleuchtet, Pfeiltasten daneben. */
@Composable
private fun Leuchtmelder(
    anzeige: Melderanzeige,
    menue: Meldermenue,
    kennung: String,
    lautlos: Boolean,
    griffe: Meldergriffe,
) {
    val schrift = Color(0xFF0E2A12)
    val ruhe = listOf(
        kennung.ifBlank { "----" },
        "${anzeige.zeitKurz}   ${anzeige.datumKurz}",
        if (anzeige.vergangene.isNotEmpty()) "${anzeige.vergangene.size} MELDUNGEN" else "KEINE MELDUNG",
    )
    val punktwort = when {
        anzeige.hatAlarm -> "Quittieren"
        anzeige.meldungSteht -> "Quittiert"
        else -> "Zurücksetzen"
    }
    Gehaeuse(Color(0xFF15171B), breite = 380.dp, ecke = 40.dp) {
        Text("LT 4", style = Schrift.MonoKlein.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFFD4AF37), modifier = Modifier.align(Alignment.End))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Display(Color(0xFF8FE08A), modifier = Modifier.weight(1f), hoehe = 84.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("◉", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = if (anzeige.hatAlarm) Color(0xFFB00000) else schrift.copy(alpha = 0.4f))
                    Text(if (lautlos) "✕" else "◂))", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = schrift)
                }
                if (menue.offen) Menueliste(menue, schrift, schrift) else Meldungszeilen(anzeige, ruhe, schrift, groesse = 12)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                Taste("▲", { hoch(anzeige, menue, griffe) }, Modifier.width(44.dp), aktiv = true, hoehe = 36.dp)
                Taste("▼", { runter(anzeige, menue, griffe) }, Modifier.width(44.dp), aktiv = true, hoehe = 36.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            Taste("◀", { if (menue.offen) menue.zurueck() }, Modifier.weight(1f))
            Taste("▶", { if (menue.offen) menue.waehlen() else if (menue.darfMenue) menue.menueOeffnen() }, Modifier.weight(1f))
            Taste(
                "●",
                { if (anzeige.hatAlarm || anzeige.meldungSteht) griffe.quittieren() },
                Modifier.weight(1f),
                aktiv = anzeige.hatAlarm || anzeige.meldungSteht,
                dringend = anzeige.hatAlarm,
            )
        }
        SehrLeise(punktwort, mono = true)
    }
}

/** Der Bogenmelder — Farbschirm mit Kopfleiste, zwei Bogentasten und einer Wippe. */
@Composable
private fun Bogenmelder(
    anzeige: Melderanzeige,
    menue: Meldermenue,
    gesicht: Melderkatalog.Gesicht,
    kennung: String,
    schleife: String,
    lautlos: Boolean,
    griffe: Meldergriffe,
) {
    val kopfleiste = when {
        anzeige.hatAlarm -> anzeige.angezeigt.firstOrNull() ?: "ALARM"
        menue.offen -> menue.kopfwort.ifBlank { "Gerät" }
        else -> anzeige.zeitKurz
    }
    val obenwort = when {
        anzeige.hatAlarm -> "Quittieren"
        anzeige.meldungSteht -> "Quittiert"
        menue.offen -> "Wählen"
        menue.darfMenue -> "Menü"
        else -> "Zurücksetzen"
    }
    Gehaeuse(gesicht.gehaeuse, breite = 290.dp, ecke = 26.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Leuchte(!anzeige.hatAlarm, farbe = Farben.Gruen, groesse = 8.dp)
            Leuchte(anzeige.hatAlarm, groesse = 8.dp)
            Spacer(Modifier.weight(1f))
            Text("BOGEN 22", style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.55f))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF4F6F8))
                .border(2.dp, Color.Black, RoundedCornerShape(8.dp)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (anzeige.hatAlarm) Color(0xFFD7261E) else Color(0xFF5B6470))
                    .padding(horizontal = 4.dp),
            ) {
                Text(kopfleiste, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(if (lautlos) "🔕" else "🔔", style = Schrift.Klein, color = Color.White)
            }
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(4.dp)) {
                if (menue.offen) {
                    Menueliste(menue, Color(0xFF1B1F25), Color(0xFF1E4F9C))
                } else {
                    Meldungszeilen(anzeige, ruhezeilen(anzeige, menue, kennung, schleife), Color(0xFF1B1F25))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Taste("▲", { hoch(anzeige, menue, griffe) }, Modifier.width(40.dp), hoehe = 30.dp)
                Taste("▼", { runter(anzeige, menue, griffe) }, Modifier.width(40.dp), hoehe = 30.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig), modifier = Modifier.weight(1f)) {
                Taste(
                    obenwort,
                    { mitteltaste(anzeige, menue, griffe) },
                    Modifier.fillMaxWidth(),
                    aktiv = !(anzeige.meldungSteht && !anzeige.hatAlarm && !menue.offen && !menue.darfMenue),
                    dringend = anzeige.hatAlarm,
                    form = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                )
                Taste(
                    "Zurück",
                    { if (menue.offen) menue.zurueck() },
                    Modifier.fillMaxWidth(),
                    aktiv = menue.offen,
                    form = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                )
            }
        }
    }
}

/** Die Einsatzuhr — am Handgelenk: Stichwort, Ort, zwei Flächen. */
@Composable
private fun Einsatzuhr(
    anzeige: Melderanzeige,
    gesicht: Melderkatalog.Gesicht,
    kennung: String,
    griffe: Meldergriffe,
) {
    val alarm = anzeige.aktuell
    val oben = if (alarm == null) anzeige.zeitKurz else "${uhrzeit(alarm.zeit)} · ${alarm.schleife}"
    val gross = alarm?.stichwort ?: "BEREIT"
    val unten = alarm?.adresse ?: kennung.ifBlank { "----" }
    val beiwerk = anzeige.zusatz?.let { "LST: $it" } ?: alarm?.stichwortText ?: "keine Meldung"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 260.dp).fillMaxWidth()) {
        Box(Modifier.width(96.dp).height(26.dp).background(Color(0xFF26282D), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(48.dp))
                .background(gesicht.gehaeuse)
                .border(3.dp, Color(0xFF5A5E66), RoundedCornerShape(48.dp))
                .padding(Abstand.Gross),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black, RoundedCornerShape(32.dp))
                    .border(1.dp, if (anzeige.hatAlarm) Farben.Signal else Color(0xFF333333), RoundedCornerShape(32.dp))
                    .padding(Abstand.Normal),
            ) {
                Text(oben, style = Schrift.MonoKlein.copy(fontSize = 11.sp), color = Color(0xFFB0B8C4), maxLines = 1)
                Text(gross, style = Schrift.MonoNormal.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold), color = if (anzeige.hatAlarm) Farben.SignalHell else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(unten, style = Schrift.MonoKlein.copy(fontSize = 12.sp), color = Color.White, maxLines = 2, textAlign = TextAlign.Center)
                Text(beiwerk, style = Schrift.MonoKlein.copy(fontSize = 10.sp), color = Color(0xFF8C96A4), maxLines = 2, textAlign = TextAlign.Center)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
                Taste("Quittieren", griffe.quittieren, Modifier.weight(1f), aktiv = !(anzeige.meldungSteht && !anzeige.hatAlarm), dringend = anzeige.hatAlarm, form = RoundedCornerShape(18.dp))
                Taste("Ausrücken", griffe.ausruecken, Modifier.weight(1f), farbe = Color(0xFF2F9E44), aktiv = anzeige.meldungSteht, form = RoundedCornerShape(18.dp))
            }
        }
        Box(Modifier.width(96.dp).height(26.dp).background(Color(0xFF26282D), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)))
    }
}

/** Der Alarmmonitor — ein Wandbildschirm mit Karte; kein Piezo, keine Quittungspiep. */
@Composable
private fun Alarmmonitor(
    anzeige: Melderanzeige,
    raum: Raumzustand,
    fahrzeug: Rundenfahrzeug,
    kennung: String,
    griffe: Meldergriffe,
) {
    val alarm = anzeige.aktuell
    val stichwort = alarm?.let { "${it.stichwort} ${it.stichwortText}" } ?: "Kein Einsatz"
    val ort = alarm?.let { a -> a.ortsteil?.let { "${a.adresse} · OT $it" } ?: a.adresse } ?: kennung.ifBlank { "----" }
    val meldebild = alarm?.meldebild ?: "Wache betriebsbereit"
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0D10), RoundedCornerShape(10.dp))
            .border(6.dp, Color(0xFF1C1E22), RoundedCornerShape(10.dp))
            .padding(Abstand.Normal),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(if (anzeige.hatAlarm) Color(0xFFB3261E) else Color(0xFF1E2A3A)).padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(stichwort, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(anzeige.zeitLang, style = Schrift.MonoKlein, color = Color.White)
        }
        Text(ort, style = Schrift.MonoNormal, color = Farben.AmberHell)
        Lagekarte(raum = raum, eigenesFahrzeugId = fahrzeug.id, modifier = Modifier.fillMaxWidth().height(200.dp))
        Text(meldebild, style = Schrift.Normal, color = Farben.Text)
        anzeige.zusatz?.let { Text("LST: $it", style = Schrift.MonoKlein, color = Farben.Text) }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            Taste("Ausrücken", griffe.ausruecken, Modifier.weight(2f), farbe = Color(0xFF2F9E44), aktiv = anzeige.meldungSteht)
            Taste("Ton aus", griffe.quittieren, Modifier.weight(1f), aktiv = anzeige.hatAlarm, dringend = anzeige.hatAlarm)
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { Leuchte(anzeige.hatAlarm, groesse = 6.dp) }
    }
}

/** Das Lautsprechergitter am Kopf der Gehäuse — gezeichnet, nicht gestapelt. */
@Composable
private fun Lautsprechergitter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(10.dp)
            .drawBehind {
                val breite = 3.dp.toPx()
                val abstand = 6.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.55f),
                        topLeft = Offset(x, 0f),
                        size = Size(breite, size.height),
                        cornerRadius = CornerRadius(breite / 2, breite / 2),
                    )
                    x += abstand
                }
            },
    )
}

/** Die beiden Ladekontakte an der Unterkante. */
@Composable
private fun Ladekontakte() {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
        repeat(2) { Box(Modifier.size(width = 14.dp, height = 4.dp).background(Color(0xFFC9A94A), RoundedCornerShape(2.dp))) }
    }
}
