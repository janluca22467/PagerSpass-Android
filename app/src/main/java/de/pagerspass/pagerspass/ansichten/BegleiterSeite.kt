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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.mobil.Begleiterstand
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Teil
import de.pagerspass.pagerspass.ui.bausteine.Teilleiste
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.raster
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Der mobile Begleiter — ein Gerät je Bildschirm, umgeschaltet über die Leiste.
 *
 * Übertragen aus `web/src/views/BegleiterView.vue`. Dort standen Funkgerät,
 * Funkchat und Melder erst als drei Kacheln untereinander; auf einem Handy hieß
 * das: alles gleichzeitig sehen wollen und nichts richtig sehen. Jetzt bekommt
 * jedes Gerät die ganze Fläche, und was auf den anderen beiden aufläuft, sagen
 * die Zahlen an der Leiste.
 *
 * <b>Die Leiste schaltet nur die Sicht.</b> Sie baut die Geräte nicht ab — der
 * Funk läuft weiter und der Melder piepst weiter, auch wenn gerade der Chat
 * vorn liegt. Deshalb sitzt der Zustand am ViewModel und nicht im gezeigten
 * Zweig.
 *
 * <b>Keine Gerätewahl auf diesem Bildschirm.</b> Bauart, Bauform, Gehäuse,
 * Gesicht und Alarmton wählt man im Konto; der Begleiter *spiegelt* diese Wahl
 * nur (siehe `Begleitergeraete`). Ein Dienstbildschirm ist kein
 * Einstellungsmenü — aber er zeigt genau das, was am Rechner stünde, und zwar
 * auch dann, wenn man mitten in der Schicht umstellt.
 */
@Composable
fun BegleiterSeite(
    modifier: Modifier = Modifier,
    stand: Begleiterstand = Begleiterstand(),
    wachhalten: Boolean = true,
    beiFunk: (String) -> Unit = {},
    beiSprechstart: () -> Unit = {},
    beiSprechende: () -> Unit = {},
    beiQuittieren: () -> Unit = {},
    beiFunkgruppe: (String?) -> Unit = {},
    beiWachhalten: (Boolean) -> Unit = {},
    beiTrennen: () -> Unit = {},
) {
    var reiter by remember { mutableStateOf(Begleiterteil.Funkgeraet) }

    /*
     * Die Lesestände für die Zahlen an der Leiste.
     *
     * <b>Was vor der Kopplung lief, ist nicht ungelesen.</b> Der Raumzustand
     * bringt das ganze bisherige Funkprotokoll mit — wer sein Handy mitten in
     * der Schicht scannte, stand sonst vor einer „9" für Sprüche, die längst am
     * Rechner vorbeigelaufen sind. Gezählt wird ab dem Augenblick, in dem das
     * Gerät in der Hand liegt.
     */
    var funkGelesen by remember { mutableIntStateOf(-1) }
    var melderGesehen by remember { mutableIntStateOf(0) }

    LaunchedEffect(stand.gekoppelt) {
        if (stand.gekoppelt && funkGelesen < 0) funkGelesen = stand.funk.size
    }

    // Liegt ein Gerät vorn, zieht sein Zähler laufend mit — sonst bekäme man
    // beim Weggehen eine Zahl für alles, was man gerade mitgelesen hat.
    LaunchedEffect(stand.funk.size, stand.melderverlauf.size, reiter) {
        if (reiter == Begleiterteil.Funkchat) funkGelesen = stand.funk.size
        if (reiter == Begleiterteil.Melder) melderGesehen = stand.melderverlauf.size
    }

    val ungelesen = mapOf(
        Begleiterteil.Funkchat.name to
            (stand.funk.size - funkGelesen.coerceAtLeast(0)).coerceAtLeast(0),
        Begleiterteil.Melder.name to
            (stand.melderverlauf.size - melderGesehen).coerceAtLeast(0),
    )

    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val unten = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(Brush.verticalGradient(listOf(Farben.Bg, Farben.BgTief))) }
            .raster(),
    ) {
        Begleiterkopf(stand, oben, beiTrennen)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                stand.laeuft && !stand.gekoppelt ->
                    Ladezeile("Funkplatz wird gekoppelt …", Modifier.padding(Abstand.Gross))

                !stand.gekoppelt -> Meldung(
                    titel = "Kopplung beendet",
                    text = stand.fehler ?: "Dieses Gerät hängt an keinem Funkplatz mehr.",
                    hinweis = "Am Rechner über das Handy-Symbol einen neuen QR-Code erzeugen.",
                )

                // Das Ende der Schicht. Ohne diesen Zweig stünden Funkgerät und
                // Melder einfach weiter da — sichtbar, aber tot, und wer nur
                // aufs Handy sieht, hielte das für einen Hänger.
                stand.beendet -> Meldung(
                    titel = "Dienstende",
                    text = "Die Runde ist beendet — Funk und Melder sind außer Dienst.",
                    hinweis = "Die Nachbesprechung mit den Punkten läuft am Rechner.",
                )

                else -> when (reiter) {
                    // Das Funkgerät rollt nicht: Es ist ein Gegenstand, der in
                    // die Hand gehört, und es füllt die Fläche selbst.
                    Begleiterteil.Funkgeraet -> Handfunkgeraet(
                        stand = stand,
                        beiSprechstart = beiSprechstart,
                        beiSprechende = beiSprechende,
                        beiFunkgruppe = beiFunkgruppe,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Abstand.Gross)
                            .padding(bottom = unten),
                    )

                    Begleiterteil.Funkchat -> Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Abstand.Gross),
                    ) {
                        Funkprotokoll(
                            zeilen = stand.funk,
                            eigenerRufname = stand.meinRufname,
                            laeuft = false,
                            beiSenden = beiFunk,
                        )
                        stand.funkhinweis?.let { SehrLeise(it) }
                        Sprechtaste(
                            sendet = stand.sendet,
                            wirdVerstanden = stand.wirdVerstanden,
                            belegtVon = stand.belegtVon,
                            gesperrtBis = stand.funkGesperrtBis,
                            beiDruck = beiSprechstart,
                            beiLoslassen = beiSprechende,
                        )
                    }

                    Begleiterteil.Melder -> Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Gross),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Abstand.Gross),
                    ) {
                        Melderteil(stand, wachhalten, beiQuittieren, beiWachhalten)
                    }
                }
            }
        }

        Teilleiste(
            teile = Begleiterteil.entries.map { Teil(it.name, it.titel, it.zeichen) },
            offen = reiter.name,
            marken = ungelesen,
            // Ein offener Alarm ruft nach dem Melder — dort steht, worum es geht.
            ruft = if (stand.alarm != null) setOf(Begleiterteil.Melder.name) else emptySet(),
            beiWahl = { id -> reiter = Begleiterteil.valueOf(id) },
        )
    }
}

private enum class Begleiterteil(
    val titel: String,
    val zeichen: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Funkgeraet("Funkgerät", Zeichen.Funk),
    Funkchat("Funkchat", Zeichen.Kanal),
    Melder("Melder", Zeichen.Melder),
}

/**
 * Der Streifen oben — derselbe Baustein wie in Fahrzeug und Leitstelle.
 *
 * <b>Drei Zustände, nicht zwei.</b> „VERBINDET" für den Aufbau, „GETRENNT" für
 * den Abriss. Auf einem Handy in der Tasche ist der Abriss der Regelfall und die
 * eine Auskunft, die zählt: Der Melder piepst gerade nicht mehr.
 */
@Composable
private fun Begleiterkopf(
    stand: Begleiterstand,
    oben: Dp,
    beiTrennen: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Farben.FlaecheHoch, Farben.Flaeche)))
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(
                    color = Farben.Rand,
                    start = Offset(0f, size.height - strich / 2f),
                    end = Offset(size.width, size.height - strich / 2f),
                    strokeWidth = strich,
                )
            }
            .padding(top = oben)
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "Funkbegleiter",
                style = Schrift.Gross,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SehrLeise(
                listOfNotNull(
                    stand.raum?.code?.let { "Raum $it" },
                    stand.meinRufname.ifBlank { null },
                ).joinToString(" · ").ifBlank { "…" },
                mono = true,
            )
        }

        Lagepille(stand.lage, stand.gekoppelt)

        Knopf("Trennen", beiTrennen, art = Knopfart.Gefahr, kompakt = true)
    }
}

/**
 * Die Pille redet von der Kopplung, nicht von der Leitung.
 *
 * Über einem abgelaufenen QR-Link stand im Web einmal „LIVE", weil die Leitung
 * zum Server ja stand — nur eben ohne Platz dahinter.
 */
@Composable
private fun Lagepille(lage: Funkverbindung.Lage, gekoppelt: Boolean) {
    val (wort, farbe) = when {
        !gekoppelt -> "AUS" to Farben.TextSehrLeise
        lage == Funkverbindung.Lage.Verbunden -> "LIVE" to Farben.GruenHell
        lage == Funkverbindung.Lage.Getrennt -> "GETRENNT" to Farben.SignalHell
        else -> "VERBINDET" to Farben.TextSehrLeise
    }

    Text(
        text = wort,
        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
        color = farbe,
        modifier = Modifier
            .border(1.dp, farbe.copy(alpha = 0.7f), Rundung.Rund)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
    )
}

/** Eine ganze Fläche für einen Satz — Ladefehler, Dienstende, gelöste Kopplung. */
@Composable
private fun Meldung(titel: String, text: String, hinweis: String? = null) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(Abstand.SehrGross),
    ) {
        Box(Modifier.weight(1f))
        Text(text = titel, style = Schrift.Titel, color = Farben.Text)
        Text(
            text = text,
            style = Schrift.Normal,
            color = Farben.TextLeise,
            textAlign = TextAlign.Center,
        )
        if (hinweis != null) {
            Text(
                text = hinweis,
                style = Schrift.Klein,
                color = Farben.TextSehrLeise,
                textAlign = TextAlign.Center,
            )
        }
        Box(Modifier.weight(1f))
    }
}

// ------------------------------------------------------------- Das Funkgerät

/**
 * Das Handfunkgerät — ein ganzes Gerät, kein Kanalschild.
 *
 * Am Rechner steht der Funk neben Karte und Protokoll; dort genügt das kompakte
 * Gehäuse um die Sprechtaste. Auf dem gekoppelten Handy *ist* der Funk der
 * ganze Bildschirm, und das Handy liegt in der Hand wie ein HRT — also sieht es
 * aus wie eines: Antenne, Drehknopf, Lautsprechergitter, Display mit der
 * geschalteten Rufgruppe, Wahltasten und die große Sprechtaste.
 *
 * <b>Das Display trägt, was am Rechner die Kanalzeile trägt.</b> Bei Halbduplex
 * ist „wer spricht gerade" die eine Auskunft, die immer sichtbar gehört — sonst
 * ist sie nur an der ausgegrauten Sprechtaste zu erraten.
 *
 * <b>Die letzten Sprüche stehen mit darin, und das ist kein zweiter
 * Funkchat</b>: keine Eingabezeile, kein Rollen, oben abgeschnitten wie auf
 * einem Gerätedisplay. Es beantwortet die Frage, die man mit dem Gerät in der
 * Hand hat — was lief gerade?
 */
@Composable
private fun Handfunkgeraet(
    stand: Begleiterstand,
    beiSprechstart: () -> Unit,
    beiSprechende: () -> Unit,
    beiFunkgruppe: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rufgruppe = when {
        !stand.funkgruppenGetrennt -> stand.meinRufname.ifBlank { "FUNK" }
        else -> stand.funkgruppeVon(stand.sendegruppe)?.marke ?: "RUNDRUF"
    }

    val zustand = when {
        stand.sendet -> "DU SENDEST"
        stand.belegtVon != null -> "${stand.belegtVon} SPRICHT".uppercase()
        else -> "KANAL FREI"
    }

    val zustandsfarbe = when {
        stand.sendet -> Farben.SignalHell
        stand.belegtVon != null -> Farben.AmberHell
        else -> Farben.GruenHell
    }

    // Der Kanalwähler gehört nur einem Gerät, das einen Kanal hat: Die
    // Besatzung schaltet ihr eines Gerät um, der Disponent legt seinen Platz auf
    // eine Gruppe. Ohne getrennte Funkgruppen gibt es nichts zu schalten.
    val wahlSichtbar = stand.funkgruppenGetrennt &&
        (stand.istLeitstelle || stand.meinFahrzeug != null)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .weight(1f),
        ) {
            Geraeteoberteil(stand.sendet)

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp, 22.dp, 14.dp, 14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Farben.FlaecheAktiv,
                                Farben.Flaeche,
                                Farben.BgTief,
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        Farben.RandHell,
                        RoundedCornerShape(22.dp, 22.dp, 14.dp, 14.dp),
                    )
                    .padding(
                        start = Abstand.Klein,
                        end = Abstand.Klein,
                        top = Abstand.Gross,
                        bottom = Abstand.Klein,
                    ),
            ) {
                Geraetekopf(stand.lage == Funkverbindung.Lage.Verbunden)

                // Das Display bekommt die freie Höhe: Der Verkehr wächst darin
                // nach oben und wird an der Kante abgeschnitten.
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(Rundung.Klein)
                        .background(ANZEIGE_GRUND)
                        .border(1.dp, Farben.Rand, Rundung.Klein)
                        .padding(Abstand.Normal),
                ) {
                    Text(
                        text = rufgruppe,
                        style = Schrift.MonoNormal.copy(
                            fontSize = Schrift.TITEL,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.em,
                        ),
                        color = ANZEIGE_SCHRIFT,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // Ohne Funkgruppen ist die Rufgruppe schon der Rufname —
                    // derselbe Text zweimal untereinander sähe aus wie ein
                    // hängendes Display.
                    if (stand.meinRufname.isNotBlank() && stand.meinRufname != rufgruppe) {
                        Text(
                            text = stand.meinRufname,
                            style = Schrift.MonoKlein,
                            color = ANZEIGE_SCHRIFT.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(zustandsfarbe, CircleShape),
                        )
                        Text(
                            text = zustand,
                            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                            color = zustandsfarbe,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Der angeschnittene Verkehr: unten der neueste Spruch, oben
                    // abgeschnitten, was nicht mehr hineinpasst.
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxWidth().weight(1f).clip(Rundung.Winzig),
                    ) {
                        stand.funk.takeLast(10).forEach { zeile ->
                            Text(
                                text = "${uhrzeit(zeile.zeit)}  ${zeile.von}: ${zeile.text}",
                                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                                color = ANZEIGE_SCHRIFT.copy(alpha = 0.78f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = Abstand.Haar),
                            )
                        }
                    }
                }

                if (wahlSichtbar) {
                    Kanalwahl(stand, beiFunkgruppe)
                }

                Sprechtaste(
                    sendet = stand.sendet,
                    wirdVerstanden = stand.wirdVerstanden,
                    belegtVon = stand.belegtVon,
                    gesperrtBis = stand.funkGesperrtBis,
                    beiDruck = beiSprechstart,
                    beiLoslassen = beiSprechende,
                )
            }
        }

        stand.funkhinweis?.let {
            Text(
                text = it,
                style = Schrift.Winzig,
                color = Farben.AmberHell,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = Abstand.Klein),
            )
        }
    }
}

/** Antenne und Drehknopf — sie wachsen aus der Kante des Gehäuses heraus. */
@Composable
private fun Geraeteoberteil(sendet: Boolean) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (sendet) Farben.SignalTief else Farben.RandHell,
                            Farben.BgTief,
                        ),
                    ),
                ),
        )
        Box(Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(40.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp))
                .background(Farben.FlaecheHoch)
                .border(1.dp, Farben.Rand, RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp)),
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(11.dp)
                    .background(Farben.Amber, Rundung.Rund),
            )
        }
    }
}

/** Lautsprechergitter und Betriebsleuchte. */
@Composable
private fun Geraetekopf(verbunden: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Abstand.Klein),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .drawBehind {
                    // Ein Gitter aus Schlitzen — gezeichnet und nicht gestapelt:
                    // ein Dutzend Kästchen im Baum wären ein Dutzend Knoten für
                    // etwas, das niemand anfasst.
                    val breite = 3.dp.toPx()
                    val abstand = 5.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawRect(
                            color = Farben.BgTief,
                            topLeft = Offset(x, 0f),
                            size = androidx.compose.ui.geometry.Size(breite, size.height),
                        )
                        x += abstand
                    }
                },
        )
        Box(
            Modifier
                .size(9.dp)
                .background(
                    if (verbunden) Farben.GruenHell else Farben.TextSehrLeise,
                    CircleShape,
                ),
        )
    }
}

/**
 * Die Kanaltasten.
 *
 * <b>„Stamm" gibt es nur im Fahrzeug.</b> Es ist der Kanal, auf dem das Gerät
 * ohne Zutun steht und auf dem man gefunden wird. Ein Leitstellenplatz hat
 * keinen — er sitzt auf den Gruppen, die er sich nimmt.
 *
 * <b>Zwei verschiedene Markierungen.</b> Laut ist, worauf gesendet wird; leise,
 * dass nichts umgeschaltet wurde. Beides zugleich hell hieße „zwei geschaltete
 * Kanäle", und das gibt es an einem Gerät nicht.
 */
@Composable
private fun Kanalwahl(stand: Begleiterstand, beiFunkgruppe: (String?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Abstand.Klein),
    ) {
        if (!stand.istLeitstelle) {
            Kanaltaste(
                marke = "Stamm",
                an = false,
                leise = stand.stehtAufStamm,
                beiDruck = { beiFunkgruppe(null) },
            )
        }

        stand.waehlbareFunkgruppen.forEach { gruppe ->
            Kanaltaste(
                marke = gruppe.marke.ifBlank { gruppe.name },
                an = gruppe.id == stand.sendegruppe,
                leise = false,
                beiDruck = { beiFunkgruppe(gruppe.id) },
            )
        }
    }
}

@Composable
private fun Kanaltaste(
    marke: String,
    an: Boolean,
    leise: Boolean,
    beiDruck: () -> Unit,
) {
    val farbe = when {
        an -> Farben.Amber
        leise -> Farben.TextLeise
        else -> Farben.TextSehrLeise
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 62.dp, minHeight = 38.dp)
            .clip(Rundung.Winzig)
            .background(if (an) Farben.HauchAmber else Farben.FlaecheHoch)
            .border(1.dp, if (an) Farben.Amber else Farben.Rand, Rundung.Winzig)
            .clickable(onClick = beiDruck)
            .padding(horizontal = Abstand.Klein),
    ) {
        Text(
            text = marke,
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
            color = farbe,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ----------------------------------------------------------------- Der Melder

/**
 * Der Melder — Piepser oder Alarm-App, je nachdem, was am Rechner steht.
 *
 * <b>Die Bauart entscheidet, nicht dieser Bildschirm.</b> Wer am Rechner auf
 * „App" gestellt hat, soll hier nicht plötzlich einen DME in der Hand halten:
 * Die Alarm-App ist kein Gehäuse, das man hinstellen kann — sie liegt über der
 * ganzen Ansicht und zeigt sich nur, wenn etwas anliegt. Bis dahin gibt es
 * nichts hinzustellen, und deshalb steht hier auch kein Einsatzspeicher: Den
 * haben die Gehäuse, nicht die App.
 */
@Composable
private fun ColumnScope.Melderteil(
    stand: Begleiterstand,
    wachhalten: Boolean,
    beiQuittieren: () -> Unit,
    beiWachhalten: (Boolean) -> Unit,
) {
    // Die 420 Punkte gelten dem Handgerät: Ein Piepser bleibt ein Piepser, auch
    // auf einem Tablet. Wachalarm und Alarmmonitor sind aber keine Handgeräte —
    // der eine hängt an der Wand, der andere im Flur, beide im Querformat.
    val breite = if (stand.geraete.querformat) Modifier.fillMaxWidth() else Modifier.widthIn(max = 420.dp)

    // Zwei Bauarten stellen gar kein Gehäuse hin: die Alarm-App und „Im Funk". Der
    // Alarm kommt in beiden Fällen als Blende über allem (siehe `PagerSpassApp`) —
    // hier steht nur, woran man ist, solange nichts anliegt.
    if (stand.geraete.ohneGehaeuse) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = breite.fillMaxWidth().flaeche().padding(Abstand.SehrGross),
        ) {
            Text(
                text = if (stand.geraete.imFunk) "IM FUNKGERÄT" else "ALARM-APP",
                style = Schrift.Etikett,
                color = Farben.TextSehrLeise,
            )
            Text(
                text = if (stand.geraete.imFunk) {
                    "Kein eigener Melder — der Alarm steht am Rechner auf dem Funkgerät " +
                        "und kommt hier als Meldung über den Schirm."
                } else {
                    "Scharf — sie meldet sich von selbst, sobald ein Alarm anliegt."
                },
                style = Schrift.Klein,
                color = Farben.TextLeise,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Piepser(
            alarm = stand.alarm ?: stand.melderverlauf.lastOrNull(),
            offen = stand.alarm != null,
            geraetename = stand.geraete.bauformName,
            beiQuittieren = beiQuittieren,
            modifier = breite.fillMaxWidth(),
        )
    }

    SehrLeise(
        "Gehäuse, Gesicht und Alarmton kommen aus deinem Konto — dieses Gerät " +
            "zeigt, was am Rechner steht.",
    )

    /*
     * <b>Ein Melder liegt auf dem Tisch und ist an.</b> Geht der Bildschirm aus,
     * hört man den Funk zwar weiter, sieht aber nichts mehr — und wer das Gerät
     * hinlegt, will genau das nicht.
     *
     * <b>Der Schalter steht hier und nicht im Kopf</b> (anders als im Web): Auf
     * einem 390er-Handy trägt der Streifen oben schon Titel, Raum, Kopplungs-
     * stand und „Trennen"; ein sechstes Ding darin wäre ein Zeichen, das man
     * erst deuten muss. Er gehört zum Melder, und man sucht ihn genau dann, wenn
     * man den Melder ansieht.
     */
    Schalterzeile(
        titel = "Bildschirm anlassen",
        an = wachhalten,
        beiWechsel = beiWachhalten,
        unterzeile = "Damit Funkgerät und Melder sichtbar durchlaufen — kostet Akku.",
        modifier = breite.fillMaxWidth(),
    )

    // Der Einsatzspeicher. Er beginnt bei der Kopplung: Alarme kommen nur an
    // verbundene Begleiter, und was vorher lief, hat dieses Gerät nie gehört.
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = breite.fillMaxWidth(),
    ) {
        Ueberschrift("Einsatzspeicher")
        if (stand.melderverlauf.isEmpty()) {
            SehrLeise("Seit der Kopplung kam kein Alarm.")
        } else {
            stand.melderverlauf.reversed().forEach { alarm ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    modifier = Modifier
                        .fillMaxWidth()
                        .flaeche(ecke = 9.dp)
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uhrzeit(alarm.zeit),
                            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                            color = Farben.TextSehrLeise,
                        )
                        Text(
                            text = alarm.stichwort,
                            style = Schrift.MonoNormal,
                            color = Farben.Text,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (alarm.schleife.isNotBlank()) {
                            Marke(alarm.schleife, farbe = Farben.TextSehrLeise)
                        }
                    }
                    SehrLeise(
                        listOfNotNull(
                            alarm.stichwortText.ifBlank { null },
                            alarm.adresse.ifBlank { null },
                            alarm.ortsteil,
                            alarm.zusatztext?.takeIf { it.isNotBlank() }?.let { "LST: $it" },
                        ).joinToString(" · "),
                    )
                }
            }
        }
    }
}

/**
 * Der Piepser.
 *
 * Ein Gegenstand, kein Bedienelement: Gehäuse, Gitter, Anzeige, eine Taste. Er
 * zeigt den offenen Alarm oder — wenn keiner anliegt — den letzten und sonst
 * seinen Bereitschaftsstand. Ein Gerät, das nichts anzeigt, sieht aus wie ein
 * ausgeschaltetes.
 */
@Composable
private fun Piepser(
    alarm: Alarmmeldung?,
    offen: Boolean,
    geraetename: String,
    beiQuittieren: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val puls = rememberInfiniteTransition(label = "melder")
    val glut by puls.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "melder-glut",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Farben.FlaecheHoch, Farben.BgTief)))
            .border(
                if (offen) 2.dp else 1.dp,
                if (offen) Farben.Signal.copy(alpha = glut) else Farben.Rand,
                RoundedCornerShape(18.dp),
            )
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = geraetename.uppercase(),
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                color = Farben.TextSehrLeise,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier
                    .size(9.dp)
                    .background(
                        if (offen) Farben.Signal.copy(alpha = glut) else Farben.GruenHell,
                        CircleShape,
                    ),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier
                .fillMaxWidth()
                .clip(Rundung.Klein)
                .background(ANZEIGE_GRUND)
                .border(1.dp, Farben.Rand, Rundung.Klein)
                .padding(Abstand.Normal),
        ) {
            if (alarm == null) {
                Text(
                    text = "BEREIT",
                    style = Schrift.MonoNormal.copy(letterSpacing = 0.2.em),
                    color = ANZEIGE_SCHRIFT,
                )
                Text(
                    text = "Kein Alarm. Der Melder bleibt scharf, solange dieses Gerät hängt.",
                    style = Schrift.Winzig,
                    color = ANZEIGE_SCHRIFT.copy(alpha = 0.7f),
                )
            } else {
                Text(
                    text = if (offen) "ALARM" else "LETZTER ALARM · ${uhrzeit(alarm.zeit)}",
                    style = Schrift.Winzig.copy(
                        fontFamily = Schrift.Mono,
                        letterSpacing = 0.2.em,
                    ),
                    color = if (offen) Farben.SignalHell else ANZEIGE_SCHRIFT.copy(alpha = 0.7f),
                )
                Text(
                    text = alarm.stichwort,
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = ANZEIGE_SCHRIFT,
                )
                if (alarm.stichwortText.isNotBlank()) {
                    Text(
                        text = alarm.stichwortText,
                        style = Schrift.Klein,
                        color = ANZEIGE_SCHRIFT.copy(alpha = 0.85f),
                    )
                }
                Text(
                    text = listOfNotNull(
                        alarm.adresse.ifBlank { null },
                        alarm.ortsteil,
                    ).joinToString(" · "),
                    style = Schrift.MonoKlein,
                    color = ANZEIGE_SCHRIFT.copy(alpha = 0.85f),
                )
                if (alarm.einheiten.isNotEmpty()) {
                    Text(
                        text = alarm.einheiten.joinToString(" · "),
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = ANZEIGE_SCHRIFT.copy(alpha = 0.7f),
                    )
                }

                // Die eigene Meldung der Leitstelle — als letzte Zeile und voll
                // deckend: Sie ist die einzige auf dieser Anzeige, die nicht aus dem
                // Einsatzbogen stammt.
                alarm.zusatztext?.takeIf { it.isNotBlank() }?.let { meldung ->
                    Text(
                        text = "LST: $meldung",
                        style = Schrift.MonoKlein,
                        color = ANZEIGE_SCHRIFT,
                    )
                }
            }
        }

        if (offen) {
            Knopf(
                aufschrift = "Alarm quittieren",
                beiDruck = beiQuittieren,
                art = Knopfart.Alarm,
                breit = true,
            )
        }
    }
}

/**
 * Die Anzeige der Geräte.
 *
 * Zwei Farben, an einer Stelle. Das ist die Anzeige eines Geräts und keine
 * Fläche der Anwendung — sie folgt deshalb nicht der Flächenstaffel, sondern
 * sieht aus wie beleuchtetes Glas.
 */
private val ANZEIGE_GRUND = Color(0xFF071013)
private val ANZEIGE_SCHRIFT = Color(0xFFAFE3C8)
