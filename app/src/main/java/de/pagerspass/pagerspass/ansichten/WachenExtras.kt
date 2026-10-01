package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.ui.theme.Rundung
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.GemeinschaftsHilfsfrist
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Schicht
import de.pagerspass.pagerspass.netz.Wacheneinstellungen
import de.pagerspass.pagerspass.netz.Wachenartikel
import de.pagerspass.pagerspass.netz.Wachenplatz
import de.pagerspass.pagerspass.netz.Wachenrang
import de.pagerspass.pagerspass.netz.Wachenschatz
import de.pagerspass.pagerspass.netz.Wachenstatistik
import de.pagerspass.pagerspass.netz.Wachenstueck
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.schmuck.Schmuck
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Die Extras der Wache — übertragen aus `components/gemeinschaft/`,
 * `views/WachenranglisteView.vue`, `views/WachenShopView.vue` und
 * `components/dienstbuch/HilfsfristVerlauf.vue`.
 *
 * <b>Getrennt von `WachenSeiten.kt`</b>, weil das hier Bausteine sind, die an mehr
 * als einer Stelle stehen: Das Emblem steht im Wachenkopf, in der Vorschau von
 * „Wache anpassen" und im Kopf des Shops; die Stufe im Kopf und in der Laufbahn.
 */

// ------------------------------------------------------------------- Emblem

/**
 * Das Emblem der Wache — das gewählte Zeichen (Helm, Florianskreuz, …) oder die
 * Initialen im Tonkreis, mit dem gekauften Rahmen.
 *
 * Das Zeichen füllt wie im Web 60 % des Schilds und trägt die Schriftfarbe des
 * Tons — dieselbe Tinte, die sonst die Initialen hätten.
 */
@Composable
fun Wachenemblem(gemeinschaft: Gemeinschaft, groesse: Dp = 52.dp) {
    val ton = Wappen.ton(gemeinschaft.id, gemeinschaft.wappenfarbe)
    val rahmen = rahmenton(gemeinschaft.emblemrahmen)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(groesse)
            .then(
                if (rahmen != null) Modifier.border(3.dp, rahmen, CircleShape).padding(4.dp)
                else Modifier,
            )
            .background(ton, CircleShape)
            .border(1.5.dp, ton.copy(alpha = 0.8f), CircleShape),
    ) {
        if (de.pagerspass.pagerspass.ui.schmuck.Wachenzeichen.hat(gemeinschaft.emblemzeichen)) {
            de.pagerspass.pagerspass.ui.schmuck.WachenzeichenBild(
                name = gemeinschaft.emblemzeichen,
                farbe = Wappen.schrift(ton),
                modifier = Modifier.fillMaxSize(0.6f),
            )
        } else {
            Text(
                text = Wappen.initialen(gemeinschaft.name, "W"),
                style = if (groesse >= 48.dp) Schrift.Gross else Schrift.Klein,
                color = Wappen.schrift(ton),
            )
        }
    }
}

/** Der Ton des Emblemrahmens — `null` heißt: keiner getragen. */
private fun rahmenton(id: String?): Color? = when {
    id.isNullOrBlank() || id == "keiner" -> null
    id == "stahlkranz" -> Color(0xFF8794A6)
    else -> Schmuck.rahmenfarbe(id)
}

/** Die Plakette des Wachentags — steht vor dem Namen wie der Team-Haken. */
@Composable
fun Wachentagplakette(tag: String) {
    Text(
        text = tag,
        style = Schrift.MonoKlein,
        color = Farben.AufAmber,
        maxLines = 1,
        modifier = Modifier
            .background(Farben.AmberHell, RoundedCornerShape(4.dp))
            .padding(horizontal = Abstand.Winzig),
    )
}

// -------------------------------------------------------------------- Stufe

/**
 * Stufe, Fortschritt und Kennzahlen der Wache — die Identität der Wache, die zu
 * ihrem Namen gehört und nicht in eine Karte irgendwo darunter.
 */
@Composable
fun WachenStufe(s: Wachenstatistik) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Etikett("Stufe ${s.stufe}")
                Text(s.bezeichnung, style = Schrift.Gross, color = Farben.Text)
            }
            Text("${zahl(s.erfahrung)} Punkte", style = Schrift.MonoKlein, color = Farben.TextLeise)
        }
        Fortschritt(anteil = s.anteil)
        SehrLeise(
            if (s.bisZurNaechsten != null) {
                "Noch ${zahl(s.bisZurNaechsten)} Punkte bis Stufe ${s.stufe + 1}." +
                    (s.naechsteFreischaltung?.let { " $it." } ?: "")
            } else {
                "Höchste Stufe erreicht."
            },
        )
        SehrLeise(
            "Auf dieser Stufe: bis zu ${s.maxMitglieder} Mitglieder · Clanrunde alle " +
                "${s.clanrundenSperreMinuten} Minuten · ${s.maxTermine} " +
                (if (s.maxTermine == 1) "geplanter Dienst" else "geplante Dienste") +
                " · bis zu ${s.clanrundenCoins} Coins je Clanrunde" +
                if (s.stufenstuecke > 0) {
                    " · ${s.stufenstuecke} " +
                        (if (s.stufenstuecke == 1) "Zierstück" else "Zierstücke") + " erspielt"
                } else {
                    ""
                },
        )
        // Die Zahlen stehen nicht mehr hier, sondern als Tafel unter dem Kopf
        // (`Wachentafel`) — dieselbe Handschrift wie oben im Dienstbuch.
    }
}

/**
 * Die Kennzahlen der Wache als Tafel — dieselbe wie oben auf der Übersicht des
 * Dienstbuchs. Bis 5.0.0.26 standen sie als vier nackte Zahlen im Fuß des Kopfs;
 * wer vom eigenen Dienstbuch herüberkam, las dieselbe Auskunft in einer zweiten
 * Handschrift.
 */
@Composable
fun Wachentafel(s: Wachenstatistik) {
    // „m:ss" aus ganzen Sekunden — sonst stünde bei 59,6 Sekunden „0:60".
    val frist = s.hilfsfristSekunden?.let { hilfsfrist(Math.round(it).toInt()) }
    val jeSchicht = if (s.schichten > 0) kommazahl(s.einsaetze.toDouble() / s.schichten) else null
    val zuletzt = s.letzteSchicht?.let { tag(it).takeIf { t -> t != "—" }?.take(5) }
    Kennzahltafel(
        listOf(
            { m -> Kennzahlkachel("Schichten", zahl(s.schichten), zuletzt?.let { "zuletzt $it" } ?: "noch keine", m, zeichen = Tafelzeichen.KALENDER) },
            { m ->
                Kennzahlkachel("Einsätze", zahl(s.einsaetze), jeSchicht?.let { "$it je Schicht" } ?: "—", m, farbe = Farben.BlauHell, zeichen = Tafelzeichen.WARNUNG)
            },
            { m ->
                Kennzahlkachel("Ø Hilfsfrist", frist?.let { "$it min" } ?: "—", "über alle Einsätze", m, farbe = Farben.GruenHell, zeichen = Tafelzeichen.STOPPUHR)
            },
            { m ->
                Kennzahlkachel(
                    "30 Tage",
                    "${zahl(s.aktivitaetPunkte)} P",
                    "${s.aktivitaetSchichten} ${if (s.aktivitaetSchichten == 1) "Schicht" else "Schichten"}",
                    m,
                    farbe = Farben.ViolettHell,
                    zeichen = Tafelzeichen.KURVE,
                )
            },
        ),
    )
}

// ---------------------------------------------------------------- Wachentag

/** Ab welcher Wachenstufe eine Wache ihren Tag trägt — Spiegel von `Gemeinschaftsregeln.TagAbStufe`. */
private const val WACHENTAG_AB_STUFE = 40

/**
 * Der Wachentag — das Ziel, auf das die Laufbahn zuläuft, und, einmal erreicht,
 * das, was die Wache im ganzen Spiel sichtbar macht.
 */
@Composable
fun WachenTag(
    gemeinschaft: Gemeinschaft,
    stufe: Int,
    name: String,
    premium: Boolean,
    laeuft: Boolean,
    beiSpeichern: (String) -> Unit,
) {
    val aktuell = gemeinschaft.tag.orEmpty()
    val erreicht = stufe >= WACHENTAG_AB_STUFE
    val fehlen = maxOf(0, WACHENTAG_AB_STUFE - stufe)
    var bearbeitet by remember { mutableStateOf(false) }
    var eingabe by remember(aktuell) { mutableStateOf(aktuell) }
    val gueltig = eingabe.isEmpty() || eingabe.length >= 2
    val vorschau = (if (bearbeitet) eingabe else aktuell).ifBlank { "TAG" }

    // Eine Karte wie die übrigen der Wachenseite. Das Etikett „Wachentag", das
    // bis dahin über dem Satz stand, ist jetzt ihr Titel.
    Buchkarte("Wachentag", geraeumig = true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wachentagplakette(aktuell.ifBlank { vorschau })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        erreicht && aktuell.isNotBlank() -> "Eure Wache trägt $aktuell"
                        erreicht -> "Noch kein Tag gesetzt"
                        else -> "Ab Stufe $WACHENTAG_AB_STUFE"
                    },
                    style = Schrift.Normal,
                    color = Farben.Text,
                )
            }
        }
        SehrLeise(
            if (erreicht) {
                "Er steht vor dem Namen jedes Mitglieds — im Dienst, in der Lobby, am Brett und " +
                    "in den Kontakten."
            } else {
                "Noch $fehlen ${if (fehlen == 1) "Stufe" else "Stufen"}. Dann trägt jedes Mitglied " +
                    "eures Teams das Kürzel eurer Wache vor seinem Namen — überall im Spiel."
            },
        )
        if (!erreicht) Fortschritt(anteil = stufe.toFloat() / WACHENTAG_AB_STUFE)

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SehrLeise("So steht es an dir:")
            Wachentagplakette(vorschau)
            Kontoname(name = name, premium = premium, stil = Schrift.Klein)
        }

        if (erreicht && gemeinschaft.darfFuehren) {
            if (bearbeitet) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Feld(
                        wert = eingabe,
                        beiAenderung = { roh ->
                            eingabe = roh.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' || it in "ÄÖÜ" }
                                .take(5)
                        },
                        etikett = "2 bis 5 Buchstaben oder Ziffern",
                        platzhalter = "z. B. NORD",
                        fehler = if (!gueltig) "Mindestens 2 Zeichen." else null,
                        modifier = Modifier.weight(1f),
                    )
                    Knopf(
                        "Speichern",
                        {
                            beiSpeichern(eingabe)
                            bearbeitet = false
                        },
                        art = Knopfart.Haupt,
                        aktiv = !laeuft && gueltig && eingabe != aktuell,
                        kompakt = true,
                    )
                }
                Knopf("Abbrechen", {
                    eingabe = aktuell
                    bearbeitet = false
                }, art = Knopfart.Leise, kompakt = true)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        if (aktuell.isNotBlank()) "Tag ändern" else "Tag festlegen",
                        {
                            eingabe = aktuell
                            bearbeitet = true
                        },
                        aktiv = !laeuft,
                        kompakt = true,
                    )
                    if (aktuell.isNotBlank()) {
                        Knopf("Abnehmen", { beiSpeichern("") }, art = Knopfart.Leise, aktiv = !laeuft, kompakt = true)
                    }
                }
            }
        } else if (erreicht) {
            SehrLeise("Festlegen können ihn Leitung und Zugführer.")
        }
    }
}

// ----------------------------------------------------------------- Laufbahn

/**
 * Die Laufbahn der Wache — ein Band zum Wischen, jede Stufe eine Karte mit dem,
 * was sie freischaltet. Dieselbe Rechnung wie `gaben()` in `WachenLaufbahn.vue`:
 * An einer Stufe steht nur, was sich gegenüber der vorigen ändert.
 */
@Composable
fun WachenLaufbahn(statistik: Wachenstatistik, stufen: List<Wachenrang>) {
    if (stufen.isEmpty()) return
    val band = rememberScrollState()
    val karte = with(androidx.compose.ui.platform.LocalDensity.current) { (KARTENBREITE + Abstand.Klein).roundToPx() }

    // Die eigene Stufe in die Mitte rollen — sonst begänne das Band bei Stufe 1,
    // und eine Wache auf Stufe 30 sähe zuerst, was sie längst hinter sich hat.
    LaunchedEffect(stufen.size, statistik.stufe) {
        val index = stufen.indexOfFirst { it.stufe == statistik.stufe }.coerceAtLeast(0)
        band.scrollTo((index * karte).coerceAtMost(band.maxValue))
    }

    // Eine Karte wie die übrigen der Wachenseite: Kopf mit Titel, darunter der
    // Stand und das Band, das seitwärts rollt.
    Buchkarte("Laufbahn der Wache", geraeumig = true) {
        SehrLeise(
            "Stufe ${statistik.stufe} · ${zahl(statistik.erfahrung)} Punkte · " +
                (statistik.bisZurNaechsten?.let { "noch ${zahl(it)} bis Stufe ${statistik.stufe + 1}" }
                    ?: "höchste Stufe erreicht"),
            mono = true,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().horizontalScroll(band),
        ) {
            stufen.forEachIndexed { i, r ->
                val davor = stufen.getOrNull(i - 1)
                val aktuell = r.stufe == statistik.stufe
                val erreicht = r.stufe < statistik.stufe
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier
                        .width(KARTENBREITE)
                        .flaeche(
                            ecke = 9.dp,
                            randfarbe = if (aktuell) Farben.Amber else Farben.Rand,
                        )
                        .padding(Abstand.Normal),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text(
                            r.stufe.toString(),
                            style = Schrift.MonoNormal,
                            color = if (aktuell) Farben.Amber else Farben.Text,
                        )
                        Text(
                            when {
                                erreicht -> "✓"
                                r.stufe > statistik.stufe -> "🔒"
                                else -> ""
                            },
                            style = Schrift.Klein,
                            color = Farben.GruenHell,
                        )
                    }
                    Text(
                        text = if (davor == null || r.bezeichnung != davor.bezeichnung) r.bezeichnung else "",
                        style = Schrift.Klein,
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SehrLeise("ab ${zahl(r.ab)} P. · +${r.coins} Coins", mono = true)
                    gaben(r, davor).forEach { gabe ->
                        Text(
                            text = gabe,
                            style = Schrift.Winzig,
                            color = if (gabe.startsWith("Zierde") || gabe.startsWith("Titel")) Farben.AmberHell
                            else Farben.TextLeise,
                        )
                    }
                    if (aktuell) Fortschritt(anteil = statistik.anteil)
                }
            }
        }
    }
}

/** Die Breite einer Stufenkarte — auch fürs Anrollen der eigenen Stufe. */
private val KARTENBREITE = 168.dp

private fun gaben(r: Wachenrang, davor: Wachenrang?): List<String> {
    if (davor == null) {
        return listOf(
            "Bis zu ${r.maxMitglieder} Mitglieder",
            "Clanrunde alle ${r.clanrundenSperreMinuten} Minuten",
            "1 geplanter Dienst",
        )
    }
    return buildList {
        if (r.bezeichnung != davor.bezeichnung) add("Titel „${r.bezeichnung}“")
        r.belohnungen.forEach { add("Zierde „$it“") }
        if (r.clanrundenSperreMinuten < davor.clanrundenSperreMinuten) {
            add("Clanrunde alle ${r.clanrundenSperreMinuten} Minuten")
        }
        if (r.maxTermine > davor.maxTermine) add("${r.maxTermine} geplante Dienste gleichzeitig")
        if (r.clanrundenCoins > davor.clanrundenCoins) add("Bis zu ${r.clanrundenCoins} Coins je Clanrunde")
        if (r.maxMitglieder > davor.maxMitglieder) add("Bis zu ${r.maxMitglieder} Mitglieder")
    }
}

// ---------------------------------------------------------- Hilfsfristkurve

/** Höchstens drei Vergleichskurven — sonst wird die Fläche unlesbar. */
private const val HOECHSTENS_MITGLIEDER = 3

private val KURVENFARBEN = listOf(Farben.Gruen, Farben.Blau, Farben.HiorgBrh)

/**
 * Die Hilfsfrist über die zuletzt gefahrenen Schichten — die eigene Kurve und die
 * der anderen aus der Wache daneben, zum Vergleich.
 *
 * <b>Die x-Achse ist echte Zeit, kein Index</b>: Nur so liegen zwei Kurven
 * verschiedener Leute an der Stelle, an der ihre Schichten tatsächlich endeten.
 * Beschriftet wird nur der letzte eigene Wert — eine Zahl an jedem Punkt liest
 * niemand.
 */
@Composable
fun HilfsfristVerlauf(eigene: List<Schicht>, mitglieder: List<GemeinschaftsHilfsfrist>) {
    data class Serie(
        val name: String,
        val farbe: Color,
        val punkte: List<Pair<Long, Double>>,
        val eigen: Boolean = false,
    )

    val eigeneSerie = Serie(
        "Du",
        Farben.Amber,
        eigene.mapNotNull { s ->
            val t = zeitwert(s.beendetUm) ?: return@mapNotNull null
            s.hilfsfristSekunden?.let { t to it.toDouble() }
        }.sortedBy { it.first },
        eigen = true,
    )
    val andere = mitglieder
        .map { m ->
            m.anzeigename to m.verlauf.mapNotNull { p ->
                val t = zeitwert(p.beendetUm) ?: return@mapNotNull null
                p.hilfsfristSekunden?.let { t to it }
            }.sortedBy { it.first }
        }
        .filter { it.second.isNotEmpty() }
        .sortedByDescending { it.second.size }
        .take(HOECHSTENS_MITGLIEDER)
        .mapIndexed { i, (name, punkte) -> Serie(name, KURVENFARBEN[i], punkte) }
    val serien = (if (eigeneSerie.punkte.isNotEmpty()) listOf(eigeneSerie) else emptyList()) + andere

    val alle = serien.flatMap { it.punkte }
    // Erst ab zwei Punkten über alle Serien zusammen ergibt sich ein Verlauf.
    if (alle.size < 2) return

    val hoechst = maxOf(60.0, Math.ceil(alle.maxOf { it.second } / 60.0) * 60.0)
    val von = alle.minOf { it.first }
    val bis = alle.maxOf { it.first }
    // Der angetippte Punkt — die Zeile unter der Kurve nennt Name, Zeit und
    // Wert wie das Schildchen im Web.
    var aktiv by remember(eigene, mitglieder) { mutableStateOf<Pair<Serie, Pair<Long, Double>>?>(null) }

    Abschnitt("Ø Hilfsfrist · letzte Schichten") {
        if (serien.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                serien.forEach { s ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).background(s.farbe, CircleShape))
                        SehrLeise(s.name)
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.height(150.dp).padding(end = Abstand.Klein),
                ) {
                    listOf(1.0, 0.5, 0.0).forEach { anteil ->
                        SehrLeise(hilfsfrist((hoechst * anteil).toInt()) ?: "", mono = true)
                    }
                }
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        .pointerInput(serien, hoechst) {
                            detectTapGestures { ort ->
                                val rand = 8.dp.toPx()
                                val breite = size.width.toFloat()
                                val h = size.height - 2 * rand
                                fun x(t: Long) = if (bis == von) breite / 2 else (t - von).toFloat() / (bis - von) * breite
                                fun y(sek: Double) = rand + h - (sek / hoechst).toFloat() * h
                                // Der nächste Punkt im Umkreis eines Fingers.
                                aktiv = serien
                                    .flatMap { serie -> serie.punkte.map { serie to it } }
                                    .map { it to (Offset(x(it.second.first), y(it.second.second)) - ort).getDistance() }
                                    .filter { it.second <= 24.dp.toPx() }
                                    .minByOrNull { it.second }
                                    ?.first
                            }
                        },
                ) {
                    val rand = 8.dp.toPx()
                    val h = size.height - 2 * rand
                    fun x(t: Long) = if (bis == von) size.width / 2 else (t - von).toFloat() / (bis - von) * size.width
                    fun y(sek: Double) = rand + h - (sek / hoechst).toFloat() * h

                    listOf(0.0, 0.5, 1.0).forEach { a ->
                        val yy = y(hoechst * a)
                        drawLine(Farben.Rand, Offset(0f, yy), Offset(size.width, yy), strokeWidth = 1f)
                    }
                    serien.forEach { s ->
                        val eigen = s.eigen
                        val pfad = Path()
                        s.punkte.forEachIndexed { i, (t, sek) ->
                            if (i == 0) pfad.moveTo(x(t), y(sek)) else pfad.lineTo(x(t), y(sek))
                        }
                        drawPath(
                            pfad,
                            s.farbe.copy(alpha = if (eigen) 1f else 0.7f),
                            style = Stroke(width = if (eigen) 2.5.dp.toPx() else 1.5.dp.toPx()),
                        )
                        s.punkte.forEach { (t, sek) ->
                            drawCircle(
                                s.farbe,
                                radius = if (eigen) 4.dp.toPx() else 3.dp.toPx(),
                                center = Offset(x(t), y(sek)),
                            )
                        }
                    }
                    aktiv?.let { (s, p) ->
                        val mitte = Offset(x(p.first), y(p.second))
                        drawCircle(s.farbe.copy(alpha = 0.25f), 9.dp.toPx(), mitte)
                        drawCircle(s.farbe, 5.dp.toPx(), mitte)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                SehrLeise(kurzdatum(von), mono = true, modifier = Modifier.weight(1f))
                SehrLeise(kurzdatum(bis), mono = true)
            }
            val gewaehlt = aktiv
            if (gewaehlt != null) {
                val (s, p) = gewaehlt
                Text(
                    "${s.name} · ${
                        DateTimeFormatter.ofPattern("dd.MM., HH:mm")
                            .format(Instant.ofEpochMilli(p.first).atZone(ZoneId.systemDefault()))
                    } · ${dauer(p.second)}",
                    style = Schrift.MonoKlein,
                    color = s.farbe,
                )
            } else {
                eigeneSerie.punkte.lastOrNull()?.let { (_, sek) ->
                    SehrLeise("Zuletzt bei dir: ${dauer(sek)} · Punkt antippen zeigt den Wert.")
                }
            }
        }
    }
}

private fun kurzdatum(ms: Long): String =
    DateTimeFormatter.ofPattern("dd.MM").format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

private fun dauer(sekunden: Double): String {
    val s = Math.round(sekunden).toInt()
    return if (s >= 60) "%d:%02d min".format(s / 60, s % 60) else "$s s"
}

// ------------------------------------------------------------ Einstellungen

/**
 * Die Stellschrauben der Wache — nur für die Leitung.
 *
 * <b>Alles auf einmal, wie im Formular</b> — mit „Übernehmen" am Fuß; ein
 * Teilbogen wäre am Server nicht von bewusst geleerten Feldern zu unterscheiden.
 * Darunter der Beitrittscode zum Neuwürfeln und ganz unten, mit Rückfrage, das
 * Auflösen.
 */
@Composable
fun EinstellungenBlende(
    gemeinschaft: Gemeinschaft,
    landkreise: List<Landkreis>,
    server: String,
    laeuft: Boolean,
    beiSpeichern: (Wacheneinstellungen) -> Unit,
    beiCodeErneuern: () -> Unit,
    beiAufloesen: () -> Unit,
    beiSchliessen: () -> Unit,
) {
    var name by remember { mutableStateOf(gemeinschaft.name) }
    var beschreibung by remember { mutableStateOf(gemeinschaft.beschreibung.orEmpty()) }
    var landkreisId by remember { mutableStateOf(gemeinschaft.landkreisId) }
    var oeffentlich by remember { mutableStateOf(gemeinschaft.oeffentlich) }
    var inRangliste by remember { mutableStateOf(gemeinschaft.inRangliste) }
    var modus by remember { mutableStateOf(gemeinschaft.beitrittModus) }
    var mindestLevel by remember { mutableStateOf(gemeinschaft.mindestLevel.toString()) }
    var mindestErfahrung by remember { mutableStateOf(gemeinschaft.mindestErfahrung.toString()) }
    var maxMitglieder by remember { mutableStateOf(gemeinschaft.maxMitglieder.toString()) }
    var kreiswahl by remember { mutableStateOf(false) }
    var aufloesenGefragt by remember { mutableStateOf(false) }
    val ablage = androidx.compose.ui.platform.LocalClipboardManager.current
    var kopiert by remember { mutableStateOf(false) }

    Blende(
        titel = "Einstellungen",
        beiSchliessen = beiSchliessen,
        fuss = {
            Knopf("Übernehmen", {
                val kreis = landkreise.firstOrNull { it.id == landkreisId }
                beiSpeichern(
                    Wacheneinstellungen(
                        name = name.trim(),
                        beschreibung = beschreibung.trim().ifBlank { null },
                        landkreisId = kreis?.id,
                        landkreis = kreis?.name,
                        oeffentlich = oeffentlich,
                        inRangliste = inRangliste,
                        beitrittModus = modus,
                        mindestLevel = mindestLevel.toIntOrNull() ?: gemeinschaft.mindestLevel,
                        mindestErfahrung = mindestErfahrung.toIntOrNull() ?: gemeinschaft.mindestErfahrung,
                        maxMitglieder = maxMitglieder.toIntOrNull() ?: gemeinschaft.maxMitglieder,
                    ),
                )
            }, art = Knopfart.Haupt, aktiv = !laeuft && name.isNotBlank(), kompakt = true)
        },
    ) {
        Feld(wert = name, beiAenderung = { name = it.take(40) }, etikett = "Name")
        Feld(
            wert = beschreibung,
            beiAenderung = { beschreibung = it.take(300) },
            etikett = "Beschreibung",
            einzeilig = false,
        )
        Wahlfeld(
            etikett = "Landkreis",
            wert = landkreise.firstOrNull { it.id == landkreisId }?.name,
            platzhalter = "Keiner",
            beiDruck = { kreiswahl = true },
        )
        SehrLeise("Hier laufen eure Clanrunden.")

        Etikett("Öffentlich gelistet")
        Segment(listOf(true, false), oeffentlich, { oeffentlich = it }, aufschrift = { if (it) "An" else "Aus" })
        SehrLeise(
            "Name, Beschreibung, Landkreis und Mitgliederzahl sind dann für alle angemeldeten " +
                "Konten sichtbar. Wer in der Mitgliederliste steht, entscheidet jeder selbst in " +
                "seinen Privatsphäre-Einstellungen.",
        )
        Etikett("In der Rangliste stehen")
        Segment(listOf(true, false), inRangliste, { inRangliste = it }, aufschrift = { if (it) "An" else "Aus" })
        SehrLeise(
            "Ob eure Wache in der Gemeinschafts-Rangliste auftaucht. Getrennt vom öffentlichen " +
                "Listen: das eine ist eine Einladung an Neue, das andere ein Vergleich mit anderen Wachen.",
        )
        Etikett("Aufnahme")
        Segment(
            listOf("Einladung", "Antrag", "Offen"),
            modus,
            { modus = it },
            aufschrift = {
                when (it) {
                    "Einladung" -> "Einladung"
                    "Antrag" -> "Antrag"
                    else -> "Offen"
                }
            },
        )
        SehrLeise(
            "„Offen“ nimmt jeden auf, der die Schwellen darunter erfüllt. Über den Beitrittscode " +
                "kommt man in jedem Fall herein.",
        )
        Feld(
            wert = mindestLevel,
            beiAenderung = { mindestLevel = it.filter(Char::isDigit).take(3) },
            etikett = "Mindestlevel",
            tastatur = KeyboardType.Number,
        )
        Feld(
            wert = mindestErfahrung,
            beiAenderung = { mindestErfahrung = it.filter(Char::isDigit).take(7) },
            etikett = "Mindesterfahrung",
            tastatur = KeyboardType.Number,
        )
        Feld(
            wert = maxMitglieder,
            beiAenderung = { maxMitglieder = it.filter(Char::isDigit).take(2) },
            etikett = "Höchstens Mitglieder",
            tastatur = KeyboardType.Number,
        )

        gemeinschaft.beitrittscode?.let { code ->
            // Dieselbe Adresse, die im Web hinter dem QR-Code steht.
            val link = "$server/gemeinschaften?code=$code"
            Ueberschrift("Beitrittscode")
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                de.pagerspass.pagerspass.ui.bausteine.QrCode(link)
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                    Text(code, style = Schrift.Titel.copy(fontFamily = Schrift.Mono), color = Farben.Amber)
                    SehrLeise("Wer diesen Code hat, tritt ohne Rückfrage bei. Abfotografieren genügt.")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf(if (kopiert) "Link kopiert" else "Link kopieren", {
                    ablage.setText(androidx.compose.ui.text.AnnotatedString(link))
                    kopiert = true
                }, art = Knopfart.Leise, kompakt = true)
                Knopf("Code neu würfeln", beiCodeErneuern, art = Knopfart.Leise, aktiv = !laeuft, kompakt = true)
            }
        }

        Ueberschrift("Gemeinschaft auflösen")
        SehrLeise(
            "Mitgliedschaften, Chatverlauf und alle offenen Anträge werden gelöscht. Das lässt " +
                "sich nicht rückgängig machen.",
        )
        if (!aufloesenGefragt) {
            Knopf("Auflösen", { aufloesenGefragt = true }, art = Knopfart.Gefahr, kompakt = true)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Ja, endgültig auflösen", beiAufloesen, art = Knopfart.Gefahr, aktiv = !laeuft, kompakt = true)
                Knopf("Abbrechen", { aufloesenGefragt = false }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }

    if (kreiswahl) {
        Landkreiswahl(
            landkreise = landkreise,
            gewaehlt = landkreisId,
            mitKeinem = "Keiner",
            beiWahl = {
                landkreisId = it
                kreiswahl = false
            },
            beiSchliessen = { kreiswahl = false },
        )
    }
}

/**
 * Die Wahl eines Landkreises — durchsuchbar, nach Bundesland gruppiert, mit
 * einer Zeile „keiner" obenauf.
 */
@Composable
fun Landkreiswahl(
    landkreise: List<Landkreis>,
    gewaehlt: String?,
    mitKeinem: String,
    beiWahl: (String?) -> Unit,
    beiSchliessen: () -> Unit,
) {
    val keiner = Landkreis(id = "", name = mitKeinem)
    val gruppen = listOf<Pair<String?, List<Landkreis>>>(null to listOf(keiner)) +
        landkreise.groupBy { it.bundesland.ifBlank { "Sonstige" } }
            .toSortedMap()
            .map { (land, kreise) -> land to kreise.sortedBy { it.name } }

    Wahlblende(
        titel = "Landkreis",
        gruppen = gruppen,
        aufschrift = { it.name },
        beiWahl = { beiWahl(it.id.ifBlank { null }) },
        beiSchliessen = beiSchliessen,
        gewaehlt = landkreise.firstOrNull { it.id == gewaehlt } ?: keiner.takeIf { gewaehlt == null },
        suchbar = true,
    )
}

// ------------------------------------------------------------------ Aussehen

/** Die Reiter des Anpassen-Dialogs — erst der Farbton, er färbt alles andere. */
private val BEREICHE = listOf("Wachenfarbe", "Kopfmuster", "Emblemrahmen", "Emblemzeichen", "Beiname")

/** Wie ein Platz am Reiter heißt. */
private val REITERNAME = mapOf(
    "Wachenfarbe" to "Farbe",
    "Kopfmuster" to "Muster",
    "Emblemrahmen" to "Rahmen",
    "Emblemzeichen" to "Zeichen",
    "Beiname" to "Beiname",
)

/** Wie ein Platz in der Überschrift heißt. */
private fun platzname(art: String): String = when (art) {
    "Kopfmuster" -> "Kopfmuster"
    "Emblemrahmen" -> "Emblem-Rahmen"
    "Emblemzeichen" -> "Emblem-Zeichen"
    "Beiname" -> "Beiname"
    "Wachenfarbe" -> "Farbton"
    else -> "Ausbau"
}

/** Was ein Platz trägt, wenn nichts gewählt ist — drei Wörter für dieselbe Abwesenheit. */
private fun getragen(g: Gemeinschaft, art: String): String? = when (art) {
    "Kopfmuster" -> g.kopfmuster.takeIf { it != "keines" }
    "Emblemrahmen" -> g.emblemrahmen.takeIf { it != "keiner" }
    "Emblemzeichen" -> g.emblemzeichen.takeIf { it != "keines" }
    "Beiname" -> g.beiname.ifBlank { null }
    else -> null
}

/**
 * Wache anpassen — Farbton, Kopfmuster, Rahmen, Zeichen, Beiname.
 *
 * <b>Die Vorschau steht oben</b>: derselbe Kopf wie auf der Wachenseite, nur
 * klein. Wer zwanzig Muster durchprobiert, will das Ergebnis sehen, ohne jedes
 * Mal nach oben zu rollen. Gesperrtes steht mit seinem Grund da („Ab Stufe 30",
 * „120 Coins · im Laden") — was es gäbe, zieht mehr als was man hat.
 */
@Composable
fun AussehenBlende(
    gemeinschaft: Gemeinschaft,
    schatz: Wachenschatz,
    laeuft: Boolean,
    beiSchmuck: (String, String?) -> Unit,
    beiFarbe: (Int) -> Unit,
    beiSchliessen: () -> Unit,
) {
    val darf = schatz.darfKaufen
    val ton = Wappen.ton(gemeinschaft.id, gemeinschaft.wappenfarbe)
    val beiname = schatz.schmuck.firstOrNull { it.art == "Beiname" && it.stueckId == gemeinschaft.beiname }?.name
    // Ein Platz zur Zeit, als Reiter — vorher standen alle fünf untereinander, und
    // wer ein Muster suchte, rollte an allen Farben und Rahmen vorbei.
    var bereich by remember { mutableStateOf("Wachenfarbe") }

    Blende(titel = "Wache anpassen", beiSchliessen = beiSchliessen) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .flaeche(ecke = 12.dp)
                .kopfband(gemeinschaft.kopfmuster, ton)
                .padding(Abstand.Normal),
        ) {
            Wachenemblem(gemeinschaft, 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                beiname?.let { Text(it, style = Schrift.Winzig, color = Farben.AmberHell) }
                Text(gemeinschaft.name, style = Schrift.Gross, color = Farben.Text)
                SehrLeise("So sieht eure Wache aus.")
            }
        }

        // Was die Wache gerade trägt, auf einen Blick — und jede Zeile führt zu
        // ihrem Reiter. Die Vorschau zeigt es, diese Liste sagt es.
        val farbname = when {
            gemeinschaft.wappenfarbe == 0 -> "Automatisch"
            gemeinschaft.wappenfarbe < 8 -> "Farbton ${gemeinschaft.wappenfarbe}"
            else -> schatz.schmuck.firstOrNull { it.art == "Wachenfarbe" && it.stueckId.toIntOrNull() == gemeinschaft.wappenfarbe }
                ?.name ?: "Farbton ${gemeinschaft.wappenfarbe}"
        }
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            BEREICHE.forEach { art ->
                val wert = if (art == "Wachenfarbe") {
                    farbname
                } else {
                    schatz.schmuck.firstOrNull { it.art == art && it.stueckId == getragen(gemeinschaft, art) }?.name ?: "Keines"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Tab) { bereich = art },
                ) {
                    SehrLeise(REITERNAME[art] ?: art, modifier = Modifier.width(88.dp))
                    Text(wert, style = Schrift.Klein, color = Farben.BlauHell, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (!darf) {
            SehrLeise("Das Aussehen der Wache wählen Leitung und Zugführer. Ansehen darf es jeder.")
        }

        Rollreiter(
            reiter = BEREICHE.map { REITERNAME[it] ?: it },
            offen = BEREICHE.indexOf(bereich).coerceAtLeast(0),
            beiWahl = { bereich = BEREICHE[it] },
        )

        if (bereich == "Wachenfarbe") {
        Ueberschrift("Farbton")
        SehrLeise("Die ersten acht gehören jeder Wache. Was danach kommt, steht im Laden.")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        ) {
            (0 until 8).forEach { n ->
                Farbfeld(
                    farbe = Wappen.ton(gemeinschaft.id, n),
                    aufschrift = if (n == 0) "A" else "",
                    gewaehlt = gemeinschaft.wappenfarbe == n,
                    aktiv = darf && !laeuft,
                    beiDruck = { if (gemeinschaft.wappenfarbe != n) beiFarbe(n) },
                )
            }
            schatz.schmuck.filter { it.art == "Wachenfarbe" }.forEach { f ->
                val nummer = f.stueckId.toIntOrNull() ?: return@forEach
                Farbfeld(
                    farbe = Wappen.PALETTE[nummer % Wappen.PALETTE.size],
                    aufschrift = if (f.frei) "" else "🔒",
                    gewaehlt = gemeinschaft.wappenfarbe == nummer,
                    aktiv = darf && !laeuft && f.frei,
                    beiDruck = { if (gemeinschaft.wappenfarbe != nummer) beiFarbe(nummer) },
                )
            }
        }

        }

        listOf("Kopfmuster", "Emblemrahmen", "Emblemzeichen", "Beiname").filter { it == bereich }.forEach { art ->
            val alle = schatz.schmuck.filter { it.art == art }
            if (alle.isEmpty()) {
                SehrLeise("Für diesen Platz gibt es noch nichts.")
                return@forEach
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Ueberschrift(platzname(art))
                SehrLeise("${alle.count { it.frei }} von ${alle.size} offen")
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                alle.forEach { s ->
                    Stueckwahl(
                        stueck = s,
                        an = getragen(gemeinschaft, art) == s.stueckId,
                        aktiv = darf && !laeuft && s.frei,
                        beiDruck = {
                            beiSchmuck(art, if (getragen(gemeinschaft, art) == s.stueckId) null else s.stueckId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Farbfeld(
    farbe: Color,
    aufschrift: String,
    gewaehlt: Boolean,
    aktiv: Boolean,
    beiDruck: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .background(farbe.copy(alpha = if (aktiv || gewaehlt) 1f else 0.45f), CircleShape)
            .border(if (gewaehlt) 3.dp else 1.dp, if (gewaehlt) Farben.Text else Farben.Rand, CircleShape)
            .clickable(enabled = aktiv, role = Role.Button, onClick = beiDruck),
    ) {
        if (aufschrift.isNotEmpty()) Text(aufschrift, style = Schrift.Klein, color = Wappen.schrift(farbe))
    }
}

@Composable
private fun Stueckwahl(stueck: Wachenstueck, an: Boolean, aktiv: Boolean, beiDruck: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .width(148.dp)
            .flaeche(ecke = 9.dp, randfarbe = if (an) Farben.Amber else Farben.Rand)
            .clickable(enabled = aktiv, role = Role.Button, onClick = beiDruck)
            .padding(Abstand.Klein),
    ) {
        // Ein Emblem-Zeichen zeigt sich selbst — der Name allein sagt bei
        // „Ehrenzeichen" nicht, wie es aussieht.
        if (stueck.art == "Emblemzeichen" && de.pagerspass.pagerspass.ui.schmuck.Wachenzeichen.hat(stueck.stueckId)) {
            de.pagerspass.pagerspass.ui.schmuck.WachenzeichenBild(
                name = stueck.stueckId,
                farbe = if (stueck.frei) Farben.Amber else Farben.TextSehrLeise,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            stueck.name,
            style = Schrift.Klein,
            color = if (stueck.frei) Farben.Text else Farben.TextSehrLeise,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        when {
            an -> Text("Getragen", style = Schrift.Winzig, color = Farben.Amber)
            !stueck.frei -> SehrLeise(
                when {
                    stueck.preis == null -> "Ab Stufe ${stueck.abStufe}"
                    stueck.imSortiment -> "${stueck.preis} Coins · im Laden"
                    else -> "${stueck.preis} Coins"
                },
            )
            stueck.preis == null && stueck.abStufe > 1 -> Text("Erspielt", style = Schrift.Winzig, color = Farben.AmberHell)
        }
    }
}

// ------------------------------------------------------------ Wachenwege

/** Welcher der drei Wege des Wachenbereichs offen ist. */
enum class Wachenweg { Wache, Rangliste, Shop }

/**
 * Die Reiterreihe des Wachenbereichs (`WachenNavigation.vue`): „Meine Wache"
 * oder „Wache finden", „Rangliste" und — nur mit eigener Wache — „Wachen-Shop",
 * jeder mit seinem Zeichen. Sie steht über der Suche, der Rangliste und dem Shop;
 * auf der eigenen Wachenseite nicht, dort tragen Knöpfe im Kopf dieselben Wege.
 */
@Composable
fun Wachenwege(
    hier: Wachenweg,
    hatWache: Boolean,
    beiWache: () -> Unit,
    beiRangliste: () -> Unit,
    beiShop: () -> Unit,
) {
    Reiterreihe {
        Reiter(
            if (hatWache) "Meine Wache" else "Wache finden",
            offen = hier == Wachenweg.Wache,
            beiDruck = { if (hier != Wachenweg.Wache) beiWache() },
            zeichen = pfadzeichen("M4 20V8l8-4 8 4v12 M8 20v-6h8v6M3 20h18 M9 9h6", gefuellt = false),
        )
        Reiter(
            "Rangliste",
            offen = hier == Wachenweg.Rangliste,
            beiDruck = { if (hier != Wachenweg.Rangliste) beiRangliste() },
            zeichen = pfadzeichen("M5 20V11h4v9M10 20V5h4v15M15 20v-7h4v7M3 20h18", gefuellt = false),
        )
        if (hatWache) {
            Reiter(
                "Wachen-Shop",
                offen = hier == Wachenweg.Shop,
                beiDruck = { if (hier != Wachenweg.Shop) beiShop() },
                zeichen = pfadzeichen("M4 9h16l-1 11H5L4 9Z M8 9V7a4 4 0 0 1 8 0v2", gefuellt = false),
            )
        }
    }
}

// --------------------------------------------------------------- Rangliste

/**
 * Die Rangliste der Wachen — sortiert nach der Gesamterfahrung; „30 Tage" zeigt,
 * wer gerade fährt. Die eigene Wache steht auch dann da, wenn sie sich aus der
 * Liste genommen hat — mit „—" statt eines Platzes.
 */
@Composable
fun WachenranglisteSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    liste: Bereichsstand<List<Wachenplatz>> = Bereichsstand(),
    beiLaden: () -> Unit = {},
    beiZurueck: () -> Unit = {},
    /** Die Reiterreihe des Wachenbereichs (`Wachenwege`) unter dem Kopf. */
    wege: (@Composable () -> Unit)? = null,
) {
    LaunchedEffect(Unit) { beiLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Rangliste",
            unterzeile = "Wachengemeinschaften",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )
        wege?.invoke()
        SehrLeise(
            "Eine Wache sammelt, was ihre Mitglieder verdienen. Sortiert nach der Gesamterfahrung; " +
                "„30 Tage“ zeigt, wer gerade fährt.",
        )
        Bereich(
            laedt = liste.laedt,
            fehler = liste.fehler,
            inhalt = liste.inhalt,
            beiErneut = beiLaden,
        ) { plaetze ->
            if (plaetze.isEmpty()) {
                Leerhinweis("Noch keine Wache hat Punkte gesammelt.")
                return@Bereich
            }
            val podest = plaetze.filter { it.platz in 1..3 }.sortedBy { it.platz }
            val rest = plaetze.filter { it !in podest }
            val eigene = plaetze.firstOrNull { it.istEigene }

            // Die eigene Wache als Wink über der Liste — der Grund, warum man sie
            // öffnet. Steht sie auf dem Podest, sieht man sie dort.
            if (eigene != null && eigene !in podest) {
                val vorne = plaetze.firstOrNull { it.platz == eigene.platz - 1 }
                val platz = if (eigene.platz > 0) eigene.platz.toString() else "—"
                Buchwink(zeichen = emptyList(), wartet = true, marke = platz) {
                    Text(
                        "${eigene.name} steht auf Platz $platz",
                        style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                    )
                    SehrLeise(
                        "Stufe ${eigene.stufe} · ${zahl(eigene.erfahrung)} Punkte" +
                            when {
                                eigene.platz <= 0 -> " · nicht in der Wertung"
                                vorne != null -> " · ${zahl(vorne.erfahrung - eigene.erfahrung)} hinter ${vorne.name}"
                                else -> ""
                            },
                    )
                }
            }

            // Das Podest: Silber links, Gold in der Mitte und höher, Bronze rechts.
            if (podest.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(2, 1, 3).forEach { p ->
                        val w = podest.firstOrNull { it.platz == p }
                        if (w == null) {
                            Box(Modifier.weight(1f))
                        } else {
                            Podeststufe(w, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Ab Platz 4 — dieselbe Rangliste wie im Dienstbuch, die eigene Wache
            // mit amberner Kante; der Balken zeigt, wer gerade fährt.
            if (rest.isNotEmpty()) {
                val spitze = plaetze.maxOf { it.aktivitaetPunkte }.coerceAtLeast(1)
                Buchkarte("Ab Platz 4", zahl = "nach Gesamterfahrung", dicht = true, abstandInnen = 0.dp) {
                    rest.forEachIndexed { i, w ->
                        Rangzeile(
                            platz = w.platz,
                            wert = zahl(w.erfahrung),
                            letzte = i == rest.lastIndex,
                            eigen = w.istEigene,
                            vorn = { Wachenschild(w.id, w.name, 28.dp) },
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    w.name,
                                    style = Schrift.Normal,
                                    color = if (w.istEigene) Farben.Amber else Farben.Text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (w.istEigene) Marke("Eure", farbe = Farben.AmberHell)
                            }
                            SehrLeise(
                                listOfNotNull(
                                    "Stufe ${w.stufe} · ${w.bezeichnung}",
                                    "${w.mitglieder} ${if (w.mitglieder == 1) "Mitglied" else "Mitglieder"}",
                                    w.landkreis,
                                ).joinToString(" · "),
                                mono = true,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.width(56.dp)) {
                                    Fortschritt(anteil = w.aktivitaetPunkte.toFloat() / spitze)
                                }
                                SehrLeise("${zahl(w.aktivitaetPunkte)} / 30 T.", mono = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Ein Schild mit den Initialen einer Wache in ihrem Ton — für Ranglisten. */
@Composable
private fun Wachenschild(id: String, name: String, groesse: Dp) {
    val ton = Wappen.ton(id)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(groesse).background(ton, RoundedCornerShape(groesse / 4)),
    ) {
        Text(
            Wappen.initialen(name, "W"),
            style = (if (groesse >= 40.dp) Schrift.Gross else Schrift.Winzig).copy(fontWeight = FontWeight.Bold),
            color = Wappen.schrift(ton),
        )
    }
}

/** Eine Stufe des Podests — Medaille, Schild, Name, Punkte. Gold steht höher. */
@Composable
private fun Podeststufe(w: Wachenplatz, modifier: Modifier) {
    val medaille = when (w.platz) {
        1 -> Farben.Amber
        2 -> Farben.TextLeise
        else -> Farben.OrangeHell
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = modifier
            .flaeche(randfarbe = if (w.istEigene) Farben.Amber else Farben.Rand)
            .padding(
                horizontal = Abstand.Klein,
                vertical = if (w.platz == 1) Abstand.Gross else Abstand.Normal,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(26.dp).background(medaille.copy(alpha = 0.22f), CircleShape),
        ) {
            Text("${w.platz}", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.ExtraBold), color = medaille)
        }
        Wachenschild(w.id, w.name, 44.dp)
        Text(
            w.name,
            style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
            color = Farben.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        SehrLeise("Stufe ${w.stufe}")
        Text(zahl(w.erfahrung), style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        SehrLeise("${zahl(w.aktivitaetPunkte)} / 30 T.", mono = true)
        if (w.istEigene) Marke("Eure", farbe = Farben.AmberHell)
    }
}

// ------------------------------------------------------------- Wachen-Shop

/**
 * Der Wachen-Shop — Ausbau, die Auslage der Woche, Einzahlen, Auszug.
 *
 * <b>Hier geht nur Spielwährung über den Tisch</b>: Coins der Wache, und eigene
 * Credits, die zehn zu eins in die Kasse wandern. Echtes Geld gibt es in diesem
 * Laden nicht — deshalb darf er in der App stehen.
 *
 * <b>Kaufen fragt im zweiten Tipp nach</b> („Wirklich für 120 Coins kaufen?"),
 * wie im Web: Die Kasse gehört der ganzen Mannschaft.
 */
@Composable
fun WachenShopSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    gemeinschaft: Gemeinschaft? = null,
    schatz: Wachenschatz? = null,
    meineCredits: Int = 0,
    laeuft: Boolean = false,
    meldung: String? = null,
    hinweis: String? = null,
    beiLaden: () -> Unit = {},
    beiKaufen: (String) -> Unit = {},
    beiWunsch: (String, Boolean) -> Unit = { _, _ -> },
    beiEinzahlen: (Int) -> Unit = {},
    beiAnpassen: () -> Unit = {},
    beiMeldungWeg: () -> Unit = {},
    beiZurueck: () -> Unit = {},
    /** Die Reiterreihe des Wachenbereichs (`Wachenwege`) unter dem Kopf. */
    wege: (@Composable () -> Unit)? = null,
) {
    LaunchedEffect(Unit) { beiLaden() }
    var scharf by remember { mutableStateOf<String?>(null) }
    var einzahlung by remember { mutableStateOf("") }
    // Ton und Initialen der eigenen Wache — die Proben der Auslage stehen auf ihrem Grund.
    val wachenton = gemeinschaft?.let { Wappen.ton(it.id, it.wappenfarbe) } ?: Farben.Amber
    val wacheninitialen = gemeinschaft?.let { Wappen.initialen(it.name, "W") } ?: "W"

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Wachen-Shop",
            unterzeile = gemeinschaft?.name ?: "Wachengemeinschaft",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )
        wege?.invoke()

        // Derselbe Eingang wie im Konto-Shop, nur mit der anderen Kasse. Der Stand
        // steht schon da, bevor die Ware geladen ist.
        Kasten(abstandInnen = Abstand.Klein) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Etikett("Wachengemeinschaft")
                    Text("Wachen-Shop", style = Schrift.Titel, color = Farben.Text)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(zahl(schatz?.coins ?: 0), style = Schrift.Titel.copy(fontFamily = Schrift.Mono), color = Farben.Amber)
                    SehrLeise("Coins")
                }
            }
            SehrLeise(
                "Coins verdient die Wache mit Stufenaufstiegen, Clanrunden und Einzahlungen aus der " +
                    "Mannschaft — zehn Credits werden ein Coin. Die Auslage wechselt jede Woche.",
            )
        }

        Meldungszeile(meldung, hinweis, beiMeldungWeg)

        if (schatz == null) {
            SehrLeise("Der Wachen-Shop öffnet gerade …")
            return@Seite
        }

        // Wer was darf, und der Weg zum Anpassen — als Wink über der Ware. Wer den
        // Laden öffnet, hat oft schon etwas und will es anziehen, nicht kaufen.
        Buchwink(
            zeichen = listOf("M12 3 4 6v6c0 4.4 3.4 8 8 9 4.6-1 8-4.6 8-9V6l-8-3Z", "m9 12 2 2 4-4"),
        ) {
            Text(
                if (schatz.darfKaufen) "Du darfst für die Wache kaufen." else "Kaufen dürfen Leitung und Zugführer.",
                style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                color = Farben.Text,
            )
            SehrLeise(
                if (schatz.darfKaufen) {
                    "Bezahlt wird aus der Wachenkasse. Was die Wache hat, legst du unter „Wache anpassen“ an."
                } else {
                    "Wünschen darfst du dir alles — dein Wunsch steht am Stück, für alle sichtbar."
                },
            )
        }
        Knopf("Wache anpassen", beiAnpassen, breit = true)

        val ausbau = schatz.artikel.filter { !it.zierde }
        val zierde = schatz.artikel.filter { it.zierde }

        // Die Tafel: was der Laden diese Woche hergibt und was die Mannschaft will.
        // Der Kassenstand steht nicht darauf — er steht groß im Eingang darüber.
        val ausgebaut = ausbau.sumOf { it.gekauft }
        val offeneWuensche = schatz.artikel
            .filter { !it.ausverkauft && !(it.zierde && it.gekauft > 0) }
            .sumOf { it.wuensche }
        Kennzahltafel(
            listOf(
                { m ->
                    Kennzahlkachel(
                        "Auslage",
                        "${zierde.size} ${if (zierde.size == 1) "Stück" else "Stücke"}",
                        restzeit(schatz.wechseltUm)?.let { "wechselt $it" } ?: "wechselt wöchentlich",
                        m,
                        farbe = Farben.BlauHell,
                        zeichen = Tafelzeichen.TASCHE,
                    )
                },
                { m ->
                    Kennzahlkachel(
                        "Ausbauten",
                        zahl(ausgebaut),
                        "${ausbau.size} ${if (ausbau.size == 1) "Art" else "Arten"} im Laden",
                        m,
                        farbe = Farben.GruenHell,
                        zeichen = Tafelzeichen.HAUS,
                    )
                },
                { m -> Kennzahlkachel("Offene Wünsche", zahl(offeneWuensche), "aus der Mannschaft", m, farbe = Farben.ViolettHell, zeichen = Tafelzeichen.HERZ) },
                { m ->
                    Kennzahlkachel(
                        "Deine Credits",
                        zahl(meineCredits),
                        "reicht für ${meineCredits / 10} ${if (meineCredits / 10 == 1) "Coin" else "Coins"}",
                        m,
                        zeichen = Tafelzeichen.GELDBOERSE,
                    )
                },
            ),
        )

        if (ausbau.isNotEmpty()) {
            Buchkarte("Ausbau", zahl = ausbau.size.toString()) {
                Warengitter(ausbau) { a, m ->
                    Ware(a, schatz, scharf, laeuft, ausbau = true, beiKaufen = {
                        if (scharf != a.id) scharf = a.id else {
                            scharf = null
                            beiKaufen(a.id)
                        }
                    }, beiWunsch = beiWunsch, modifier = m, ton = wachenton, initialen = wacheninitialen)
                }
            }
        }

        if (zierde.isNotEmpty()) {
            Buchkarte("Diese Woche im Laden", zahl = zierde.size.toString()) {
                // Kein Countdown auf die Sekunde: Ein Sortiment, das eine Woche steht,
                // braucht keine laufende Uhr.
                restzeit(schatz.wechseltUm)?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }
                SehrLeise(
                    "Zehn Stücke je Woche, eines davon im Angebot. Gekauft ist gekauft — was ihr habt, " +
                        "bleibt euch, auch wenn es nächste Woche nicht mehr hier steht.",
                )
                Textweg("Den ganzen Katalog ansehen", beiAnpassen)
                Warengitter(zierde) { a, m ->
                    Ware(a, schatz, scharf, laeuft, ausbau = false, beiKaufen = {
                        if (scharf != a.id) scharf = a.id else {
                            scharf = null
                            beiKaufen(a.id)
                        }
                    }, beiWunsch = beiWunsch, modifier = m, ton = wachenton, initialen = wacheninitialen)
                }
            }
        }

        Buchkarte(
            "Einzahlen",
            kopfweg = { SehrLeise("10 Credits = 1 Coin", mono = true) },
        ) {
            val credits = einzahlung.toIntOrNull() ?: 0
            val coins = credits / 10
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                // Schnellbeträge — nur die, die man sich leisten kann.
                val schnell = listOf(100, 500, 1000).filter { it <= meineCredits }
                if (schnell.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        schnell.forEach { b ->
                            de.pagerspass.pagerspass.ui.bausteine.Pille(
                                b.toString(),
                                an = credits == b,
                                beiDruck = { einzahlung = b.toString() },
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Feld(
                        wert = einzahlung,
                        beiAenderung = { einzahlung = it.filter(Char::isDigit).take(7) },
                        etikett = "Aus deinen Credits in die Wachenkasse",
                        platzhalter = "Credits",
                        tastatur = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    Knopf(
                        "Einzahlen",
                        {
                            beiEinzahlen(credits)
                            einzahlung = ""
                        },
                        aktiv = !laeuft && coins >= 1 && coins * 10 <= meineCredits,
                        kompakt = true,
                    )
                }
                SehrLeise(
                    if (coins > 0) {
                        "${coins * 10} deiner Credits werden $coins ${if (coins == 1) "Coin" else "Coins"}" +
                            (if (credits - coins * 10 > 0) ", ${credits - coins * 10} bleiben bei dir" else "") + "."
                    } else {
                        "Ab 10 Credits — du hast $meineCredits."
                    },
                )
            }
        }

        if (schatz.auszug.isNotEmpty()) {
            // Der Auszug: warum eigentlich 80 Coins? Datum, Text, Betrag rechtsbündig;
            // grün herein, rot hinaus.
            Buchkarte("Letzte Bewegungen", zahl = schatz.auszug.size.toString(), dicht = true, abstandInnen = 0.dp) {
                Column {
                    schatz.auszug.forEachIndexed { i, p ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                            modifier = Modifier
                                .fillMaxWidth()
                                .zeilenstrich(i == schatz.auszug.lastIndex)
                                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                        ) {
                            SehrLeise(kurzdatumIso(p.um), mono = true)
                            Text(p.text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                            Text(
                                (if (p.betrag > 0) "+" else "") + p.betrag,
                                style = Schrift.MonoKlein,
                                color = if (p.betrag < 0) Farben.SignalHell else Farben.GruenHell,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Das Raster der Auslage (`WarenGitter.vue`): so viele Spalten, wie Kacheln von
 * mindestens 220 Punkten nebeneinander passen. Am schmalen Handy ist das eine —
 * genau wie im Web, wo `minmax(220px, 1fr)` dort auch nur eine Spalte ergibt.
 */
@Composable
private fun <T> Warengitter(waren: List<T>, kachel: @Composable (T, Modifier) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val spalten = ((maxWidth + Abstand.Normal) / (WARE_MINDESTBREITE + Abstand.Normal)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            waren.chunked(spalten).forEach { reihe ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max),
                ) {
                    reihe.forEach { kachel(it, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(spalten - reihe.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private val WARE_MINDESTBREITE = 220.dp

/**
 * Der Ton einer Gattung (`GATTUNGSTON` in `utils/wachenschmuck.ts`): Jede Gattung
 * tönt ihre Bühne, damit man schon aus dem Augenwinkel sieht, was da steht.
 */
private fun gattungston(art: String): Color = when (art) {
    "Kopfmuster" -> Color(0xFF46C8A0)
    "Emblemrahmen" -> Color(0xFF5F8CFF)
    "Emblemzeichen" -> Color(0xFFD98CF0)
    "Beiname" -> Color(0xFFF0A35F)
    "Wachenfarbe" -> Color(0xFF7FD0E8)
    else -> Farben.Amber
}

/**
 * Eine Ware der Auslage als Kachel (`WareKachel.vue`): Gattung oben links in
 * ihrem Ton, der Besitz in der Ecke, das Stück auf einer getönten Bühne, der Name
 * darunter, dann Wirkung und Wünsche — und am Fuß der eine Knopf, der zur Ware passt.
 * Der Fuß sitzt unten, damit die Knöpfe einer Reihe auf einer Linie liegen.
 */
@Composable
private fun Ware(
    a: Wachenartikel,
    schatz: Wachenschatz,
    scharf: String?,
    laeuft: Boolean,
    ausbau: Boolean,
    beiKaufen: () -> Unit,
    beiWunsch: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** Der Ton der Wache — eine Probe auf fremdem Blau probiert das falsche Stück. */
    ton: Color = Farben.Amber,
    initialen: String = "W",
) {
    val fehlt = maxOf(0, a.preis - schatz.coins)
    val akzent = gattungston(a.art)
    val besitz = when {
        ausbau && a.gekauft > 0 -> "${a.gekauft} ×"
        !ausbau && a.gekauft > 0 -> "✓ Im Besitz"
        else -> null
    }

    Box(modifier = modifier.flaeche().padding(Abstand.Normal)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                (if (ausbau) "Ausbau" else platzname(a.art)).uppercase(),
                style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
                color = akzent,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Start).padding(end = if (besitz != null) 72.dp else 0.dp),
            )
            // Die Bühne: das Stück selbst unter einem leisen Strahler, kein Sinnbild.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 92.dp)
                    .background(Farben.BgTief, Rundung.Klein)
                    .background(
                        Brush.radialGradient(listOf(akzent.copy(alpha = 0.18f), Color.Transparent)),
                        Rundung.Klein,
                    ),
            ) {
                Wachenprobe(a, ton = ton, initialen = initialen, akzent = akzent)
            }
            Text(
                a.name,
                style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                color = Farben.Text,
                textAlign = TextAlign.Center,
            )
            // Das Stück der Woche trägt seinen alten Preis durchgestrichen daneben.
            if (a.angebot) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("−${schatz.angebotRabattProzent} %", style = Schrift.MonoKlein, color = Farben.AmberHell)
                    Text(
                        "${a.listenpreis}",
                        style = Schrift.MonoKlein.copy(textDecoration = TextDecoration.LineThrough),
                        color = Farben.TextSehrLeise,
                    )
                }
            }
            Text(wirkung(a.art), style = Schrift.Klein, color = Farben.TextSehrLeise, textAlign = TextAlign.Center)
            if (a.wuensche > 0 && (ausbau || a.gekauft == 0)) {
                Text(
                    "${a.wuensche} ${if (a.wuensche == 1) "Wunsch" else "Wünsche"} aus der Mannschaft",
                    style = Schrift.Klein,
                    color = Farben.BlauHell,
                    textAlign = TextAlign.Center,
                )
            }
            Box(Modifier.weight(1f, fill = false))
            Column(modifier = Modifier.fillMaxWidth()) {
                when {
                    ausbau && a.ausverkauft -> SehrLeise("Ausgebaut", Modifier.align(Alignment.CenterHorizontally))
                    !ausbau && a.gekauft > 0 -> SehrLeise("Im Besitz", Modifier.align(Alignment.CenterHorizontally))
                    schatz.darfKaufen -> Knopf(
                        when {
                            fehlt > 0 -> "Es fehlen $fehlt Coins"
                            scharf == a.id -> "Wirklich für ${a.preis} Coins kaufen?"
                            else -> "${a.preis} Coins"
                        },
                        beiKaufen,
                        art = if (scharf == a.id) Knopfart.Haupt else Knopfart.Normal,
                        aktiv = !laeuft && fehlt == 0,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    else -> Knopf(
                        if (a.vonMirGewuenscht) "Gewünscht ✓" else "Wünschen",
                        { beiWunsch(a.id, !a.vonMirGewuenscht) },
                        art = Knopfart.Leise,
                        aktiv = !laeuft,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (besitz != null) {
            Text(
                besitz,
                style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG, fontWeight = FontWeight.Bold),
                color = akzent,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(akzent.copy(alpha = 0.2f).compositeOver(Farben.BgTief), CircleShape)
                    .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
            )
        }
    }
}

/**
 * Wie ein Zierstück an der Wache aussehen wird (`WachenProbe.vue`): das Band des
 * Musters, das Schild mit Rahmen, Zeichen oder Farbe, der Beiname als Zeile — und
 * beim Ausbau, der kein Stück hat, die Zahl, die er bringt.
 */
@Composable
private fun Wachenprobe(a: Wachenartikel, ton: Color, initialen: String, akzent: Color) {
    val schild = RoundedCornerShape(10.dp)
    when (a.art) {
        "Kopfmuster" -> Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Abstand.Normal)
                .height(44.dp)
                .background(ton.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .kopfband(a.stueckId.orEmpty(), ton),
        )
        "Emblemrahmen", "Emblemzeichen", "Wachenfarbe" -> {
            val grund = if (a.art == "Wachenfarbe") Wappen.ton("", a.stueckId?.toIntOrNull() ?: 0) else ton
            val rahmen = if (a.art == "Emblemrahmen") rahmenton(a.stueckId) else null
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .then(if (rahmen != null) Modifier.border(3.dp, rahmen, schild).padding(4.dp) else Modifier)
                    .size(46.dp)
                    .background(grund, schild),
            ) {
                if (a.art == "Emblemzeichen" && de.pagerspass.pagerspass.ui.schmuck.Wachenzeichen.hat(a.stueckId)) {
                    de.pagerspass.pagerspass.ui.schmuck.WachenzeichenBild(
                        name = a.stueckId.orEmpty(),
                        farbe = Wappen.schrift(grund),
                        modifier = Modifier.fillMaxSize(0.62f),
                    )
                } else {
                    Text(
                        initialen,
                        style = Schrift.Gross.copy(fontWeight = FontWeight.ExtraBold),
                        color = Wappen.schrift(grund),
                    )
                }
            }
        }
        "Beiname" -> Text(
            a.name.uppercase(),
            style = Schrift.Klein.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
            color = akzent,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Abstand.Klein),
        )
        else -> Text(
            if (a.art == "Mitgliederplaetze") "+2" else "+1",
            style = Schrift.Schlagzeile.copy(fontWeight = FontWeight.ExtraBold),
            color = akzent,
        )
    }
}

private fun wirkung(art: String): String = when (art) {
    "Mitgliederplaetze" -> "Zwei Plätze mehr in der Mannschaft — mehrfach kaufbar, bis 50."
    "Terminplaetze" -> "Ein geplanter Dienst mehr gleichzeitig im Dienstplan."
    "Emblemrahmen" -> "Ein Ring um das Wachen-Emblem."
    "Emblemzeichen" -> "Ein Zeichen im Emblem, an Stelle der Initialen."
    "Wachenfarbe" -> "Ein Farbton über die freie Palette hinaus."
    "Beiname" -> "Eine Zeile über dem Wachennamen."
    else -> "Ein Muster im Kopf der Wachenseite."
}

/** „noch 3 Tage" — kein Countdown auf die Sekunde, eine Auslage steht eine Woche. */
private fun restzeit(bis: String?): String? {
    val dann = bis?.let(::zeitwert) ?: return null
    val ms = dann - System.currentTimeMillis()
    if (ms <= 0) return null
    val stunden = Duration.ofMillis(ms).toHours()
    val wann = DateTimeFormatter.ofPattern("EEEE, dd.MM.", Locale.GERMAN)
        .format(Instant.ofEpochMilli(dann).atZone(ZoneId.systemDefault()))
    val rest = when {
        stunden >= 48 -> "noch ${stunden / 24} Tage"
        stunden >= 24 -> "noch 1 Tag"
        stunden >= 2 -> "noch $stunden Stunden"
        stunden >= 1 -> "noch 1 Stunde"
        else -> "weniger als eine Stunde"
    }
    return "$rest · $wann"
}

private fun kurzdatumIso(iso: String): String = zeitwert(iso)?.let(::kurzdatum) ?: "—"
