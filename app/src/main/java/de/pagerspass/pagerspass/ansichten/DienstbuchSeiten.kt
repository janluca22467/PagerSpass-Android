package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Dienstbuchdaten
import de.pagerspass.pagerspass.mobil.Werkstand
import de.pagerspass.pagerspass.mobil.Werkstatt
import de.pagerspass.pagerspass.netz.Abzeichen
import de.pagerspass.pagerspass.netz.Archivrunde
import de.pagerspass.pagerspass.netz.Befund
import de.pagerspass.pagerspass.netz.Erfahrungsposten
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Mitgliedshilfsfrist
import de.pagerspass.pagerspass.netz.ORG_NAME
import de.pagerspass.pagerspass.netz.Rang
import de.pagerspass.pagerspass.netz.Rundenkennzahl
import de.pagerspass.pagerspass.netz.Schichtzeile
import de.pagerspass.pagerspass.netz.Spielerbilanz
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.zeichen.Zeichen
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Das Dienstbuch — das Gegenstück zu `web/src/views/dienstbuch/`.
 *
 * <b>Eine Seite mit fünf Reitern</b> (Übersicht, Schichten, Laufbahn,
 * Abzeichen, Auswertung), die sich einen Bestand teilen — dieselbe Aufteilung
 * wie im Web, wo die Reiter eigene Adressen haben, aber einen Speicher. Die
 * Garage bleibt eine eigene Unterseite: Sie hat Knöpfe, die Geld kosten, und
 * gehört nicht zwischen zwei Listen zum Nachlesen.
 *
 * <b>Eine Schicht öffnen heißt: ihre Nachbesprechung.</b> Im Web ersetzt die
 * geöffnete Schicht die Seite ganz; hier ist sie eine Unterseite mit eigenem
 * Zurück (`SchichtSeite`).
 */
@Composable
fun DienstbuchSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    stand: Werkstand = Werkstand(),
    fahrzeuge: List<Fahrzeugvorlage> = emptyList(),
    werkstatt: Werkstatt? = null,
    beiSchicht: (String) -> Unit = {},
    beiGarage: () -> Unit = {},
    beiDienst: () -> Unit = {},
    beiPremium: () -> Unit = {},
) {
    var reiter by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(Unit) { werkstatt?.buchLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(titel = "Dienstbuch", unterzeile = "Was du gefahren bist")

        Rollreiter(
            reiter = listOf("Übersicht", "Schichten", "Laufbahn", "Abzeichen", "Auswertung"),
            offen = reiter,
            beiWahl = { reiter = it },
        )

        if (reiter == 4) {
            Auswertung(stand, beiPremium, beiLaden = { werkstatt?.auswertungLaden() })
            return@Seite
        }

        Bereich(
            laedt = stand.buch.ersteLadung,
            fehler = stand.buch.fehler,
            inhalt = stand.buch.inhalt,
            beiErneut = { werkstatt?.buchLaden(neu = true) },
        ) { daten ->
            when (reiter) {
                0 -> Uebersicht(konto, daten, stand.mitglieder, beiSchicht, beiGarage, beiDienst) {
                    reiter = it
                }
                1 -> Schichten(
                    daten = daten,
                    fahrzeuge = fahrzeuge,
                    posten = stand.posten,
                    beiBuchungen = { werkstatt?.buchungenLaden(it) },
                    beiSchicht = beiSchicht,
                    beiDienst = beiDienst,
                )
                2 -> Laufbahn(konto, daten)
                else -> Abzeichenwand(
                    abzeichen = daten.abzeichen,
                    vitrine = stand.vitrine,
                    meldung = stand.vitrinenmeldung,
                    beiLaden = { werkstatt?.vitrineLaden() },
                    beiVitrine = { werkstatt?.vitrineUmschalten(it) },
                )
            }
        }
    }
}

// ------------------------------------------------------------- Übersicht

/**
 * Die Übersicht — was aus den Schichten zu rechnen ist.
 *
 * Oben der Stand der Laufbahn groß, darunter die Tafel mit vier Zahlen, dann
 * links im Web „was war" (letzte Schichten, Hilfsfristverlauf), rechts „was man
 * gesammelt hat" (Rekorde, Abzeichen, Organisationen, Garage). Am Handy stehen
 * beide Spalten untereinander, in dieser Reihenfolge.
 */
@Composable
private fun Uebersicht(
    konto: Konto?,
    daten: Dienstbuchdaten,
    mitglieder: List<Mitgliedshilfsfrist>,
    beiSchicht: (String) -> Unit,
    beiGarage: () -> Unit,
    beiDienst: () -> Unit,
    beiReiter: (Int) -> Unit,
) {
    val schichten = daten.schichten
    val einsaetze = schichten.sumOf { it.einsaetze }
    val punkte = schichten.sumOf { it.punkte ?: 0 }

    if (konto != null) {
        Kasten(
            abstandInnen = Abstand.Klein,
            modifier = Modifier.clickable(role = Role.Button) { beiReiter(2) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Etikett("Dein Stand")
                    Text(konto.rang, style = Schrift.Gross, color = Farben.Text)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${konto.level}", style = Schrift.Anzeige, color = Farben.Amber)
                    SehrLeise("Stufe")
                }
            }
            Fortschritt(anteil = konto.stufenanteil)
            SehrLeise("${zahl(konto.erfahrung)} Punkte · ${konto.stufentext}", mono = true)
            Textweg("Laufbahn ›", { beiReiter(2) })
        }
    }

    // Die Hilfsfrist im Mittel ist nach Einsätzen gewichtet — eine Schicht mit
    // einem Einsatz zählt nicht so viel wie eine mit zwölf.
    var summe = 0.0
    var gewicht = 0
    schichten.forEach { s ->
        val hf = s.hilfsfristSekunden ?: return@forEach
        if (s.einsaetze <= 0) return@forEach
        summe += hf.toDouble() * s.einsaetze
        gewicht += s.einsaetze
    }
    val mittel = if (gewicht > 0) summe / gewicht else null
    val beste = schichten.mapNotNull { it.punkte }.maxOrNull()

    Kennzahltafel(
        listOf(
            { m ->
                Kennzahlkachel(
                    "Schichten",
                    zahl(schichten.size),
                    schichten.firstOrNull()?.beendetUm?.let { "zuletzt ${tagKurz(it)}" } ?: "noch keine",
                    m,
                    zeichen = Tafelzeichen.KALENDER,
                )
            },
            { m ->
                Kennzahlkachel(
                    "Einsätze",
                    zahl(einsaetze),
                    if (schichten.isEmpty()) "—" else "${kommazahl(einsaetze.toDouble() / schichten.size)} je Schicht",
                    m,
                    farbe = Farben.Blau,
                    zeichen = Tafelzeichen.WARNUNG,
                )
            },
            { m ->
                Kennzahlkachel(
                    "Ø Hilfsfrist",
                    dauer(mittel)?.let { "$it min" } ?: "—",
                    "über alle Schichten",
                    m,
                    farbe = Farben.Gruen,
                    zeichen = Tafelzeichen.STOPPUHR,
                )
            },
            { m ->
                Kennzahlkachel(
                    "Punkte",
                    zahl(punkte),
                    beste?.let { "beste Schicht +${zahl(it)}" } ?: "—",
                    m,
                    zeichen = Tafelzeichen.STERN,
                )
            },
        ),
    )

    // Karten mit Kopf (`.db-karte`) — Titel in Versalien, rechts der Weg weiter.
    Buchkarte(
        "Zuletzt gefahren",
        dicht = schichten.isNotEmpty(),
        abstandInnen = 0.dp,
        kopfweg = if (schichten.isNotEmpty()) {
            { Textweg("alle ${schichten.size}", { beiReiter(1) }) }
        } else {
            null
        },
    ) {
        if (schichten.isEmpty()) {
            Leerhinweis("Noch nichts im Buch. Nach deinem ersten Dienstende steht die erste Schicht hier.") {
                Knopf("Erste Schicht fahren", beiDienst, art = Knopfart.Haupt)
            }
        } else {
            val fuenf = schichten.take(5)
            fuenf.forEachIndexed { i, s ->
                Schichtzeile(s, null, beiDruck = { beiSchicht(s.code) }, letzte = i == fuenf.lastIndex)
            }
        }
    }

    Hilfsfristverlauf(daten.statistik?.verlauf.orEmpty(), mitglieder, beiSchicht)

    val eigene = daten.statistik?.spieler?.firstOrNull { it.playerId == konto?.kennung }?.rekorde
    if (eigene != null) {
        val rekorde = buildList {
            eigene.schnellsteAusrueckzeitSekunden?.let {
                add(Triple("⚡ Schnellste Ausrückzeit", null, dauerMitEinheit(it)))
            }
            eigene.kuerzesteHilfsfristSekunden?.let {
                add(Triple("🚨 Kürzeste Hilfsfrist", eigene.kuerzesteHilfsfristStichwort, dauerMitEinheit(it)))
            }
            if (eigene.meisteLagemeldungenSchicht > 0) {
                add(Triple("📻 Meiste Lagemeldungen", "in einer Schicht", eigene.meisteLagemeldungenSchicht.toString()))
            }
        }
        if (rekorde.isNotEmpty()) {
            Buchkarte("Persönliche Rekorde", geraeumig = true) {
                rekorde.forEach { (name, zusatz, wert) ->
                    Wertzeile(listOfNotNull(name, zusatz).joinToString(" · "), wert)
                }
            }
        }
    }

    if (daten.abzeichen.isNotEmpty()) {
        val erreicht = daten.abzeichen.count { it.erreicht }
        Buchkarte(
            "Abzeichen",
            zahl = "${zahl(erreicht)} / ${zahl(daten.abzeichen.size)}",
            kopfweg = { Textweg("alle", { beiReiter(3) }) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Fortschritt(anteil = erreicht.toFloat() / daten.abzeichen.size)
                val vorschau = daten.abzeichen
                    .filter { it.erreicht && it.erreichtAm != null }
                    .sortedByDescending { it.erreichtAm }
                    .take(6)
                if (vorschau.isEmpty()) {
                    SehrLeise("Noch keins erreicht — das erste kommt meist mit der ersten Schicht.")
                } else {
                    vorschau.forEach { a ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Text("★", style = Schrift.Normal, color = Farben.Amber)
                            Text(a.titel, style = Schrift.Klein, color = Farben.Text)
                        }
                    }
                }
            }
        }
    }

    val orgs = daten.garage?.proOrganisation.orEmpty()
    if (orgs.isNotEmpty()) {
        val spitze = orgs.maxOf { it.erfahrung }.coerceAtLeast(1)
        Buchkarte("Erfahrung je Organisation", geraeumig = true) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                orgs.forEach { p ->
                    Balkenzeile(
                        name = ORG_NAME[p.organisation] ?: p.organisation,
                        wert = "${zahl(p.erfahrung)} P",
                        anteil = p.erfahrung.toFloat() / spitze,
                        farbe = orgFarbe(p.organisation),
                    )
                }
            }
        }
    }

    // Die Garage als Wink — amber, wenn Gutscheine warten.
    val gutscheine = daten.garage?.offeneWahlen ?: 0
    Buchwink(
        zeichen = listOf("M3 10.5 12 4l9 6.5", "M5 10.5V20h14v-9.5", "M8.5 20v-6h7v6"),
        wartet = gutscheine > 0,
        modifier = Modifier.clickable(role = Role.Button, onClick = beiGarage),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Garage", style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                SehrLeise(
                    daten.garage?.let { g ->
                        "${g.fahrzeuge.size} Fahrzeuge" +
                            if (gutscheine > 0) " · $gutscheine Gutschein${if (gutscheine == 1) "" else "e"} offen" else ""
                    } ?: "Dein Fuhrpark",
                )
            }
            Text("›", style = Schrift.Gross, color = Farben.TextLeise)
        }
    }
}

/**
 * Der Hilfsfristverlauf — die eigenen letzten Schichten als Linie, bis zu drei
 * Wachenkameraden daneben.
 *
 * <b>Die Achse beginnt bei null</b>, anders als in der Auswertung: Hier stehen
 * mehrere Menschen nebeneinander, und eine abgeschnittene Achse ließe zwei
 * Minuten Unterschied wie das Doppelte aussehen.
 */
@Composable
private fun Hilfsfristverlauf(
    verlauf: List<Rundenkennzahl>,
    mitglieder: List<Mitgliedshilfsfrist>,
    beiSchicht: (String) -> Unit,
) {
    data class Punkt(val zeit: Long, val sekunden: Double, val code: String?, val ort: String? = null)
    data class Serie(val name: String, val farbe: Color, val punkte: List<Punkt>)

    val eigene = Serie(
        "Du",
        Farben.Amber,
        verlauf.mapNotNull { r ->
            val s = r.hilfsfristSekunden ?: return@mapNotNull null
            val t = zeitwert(r.beendetUm) ?: return@mapNotNull null
            Punkt(t, s, r.code, r.ort.ifBlank { null })
        }.sortedBy { it.zeit },
    )
    val freundfarben = listOf(Farben.Gruen, Farben.Blau, Farben.HiorgBrh)
    val freunde = mitglieder
        .map { m ->
            m.anzeigename to m.verlauf.mapNotNull { p ->
                val s = p.hilfsfristSekunden ?: return@mapNotNull null
                val t = zeitwert(p.beendetUm) ?: return@mapNotNull null
                Punkt(t, s, null)
            }.sortedBy { it.zeit }
        }
        .filter { it.second.isNotEmpty() }
        .sortedByDescending { it.second.size }
        .take(3)
        .mapIndexed { i, (name, punkte) -> Serie(name, freundfarben[i], punkte) }
    val serien = (if (eigene.punkte.isNotEmpty()) listOf(eigene) else emptyList()) + freunde
    val alle = serien.flatMap { it.punkte }

    Buchkarte("Ø Hilfsfrist · letzte Schichten", geraeumig = true) {
        if (alle.size < 2) {
            Leerhinweis("Nach zwei Schichten mit Einsätzen steht hier, wie sich deine Hilfsfrist entwickelt.")
            return@Buchkarte
        }
        val hoechst = (Math.ceil(alle.maxOf { it.sekunden } / 60.0) * 60.0).coerceAtLeast(60.0)
        val tMin = alle.minOf { it.zeit }
        val tMax = alle.maxOf { it.zeit }
        // Der angetippte Punkt samt Serie — die Zeile unter der Kurve nennt ihn,
        // wie das Schildchen im Web (`HilfsfristVerlauf.vue`).
        var aktiv by remember(verlauf, mitglieder) { mutableStateOf<Pair<Serie, Punkt>?>(null) }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.height(150.dp),
                ) {
                    listOf(hoechst, hoechst / 2, 0.0).forEach {
                        Text(dauer(it).orEmpty(), style = Schrift.Winzig, color = Farben.TextSehrLeise)
                    }
                }
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        // Ein Finger trifft keinen Punkt von sieben Pixeln: Es
                        // gilt der nächste Punkt im Umkreis von 24 dp, wie die
                        // unsichtbaren Trefferkreise im Web.
                        .pointerInput(serien, hoechst) {
                            detectTapGestures { ort ->
                                val rand = 8.dp.toPx()
                                val breite = size.width.toFloat()
                                val hoehe = size.height.toFloat()
                                fun x(t: Long) = if (tMax == tMin) breite / 2 else
                                    rand + (t - tMin).toFloat() / (tMax - tMin) * (breite - 2 * rand)
                                fun y(s: Double) = rand + (1f - (s / hoechst).toFloat()) * (hoehe - 2 * rand)
                                val naechster = serien
                                    .flatMap { serie -> serie.punkte.map { serie to it } }
                                    .minByOrNull { (_, p) -> (Offset(x(p.zeit), y(p.sekunden)) - ort).getDistance() }
                                    ?.takeIf { (_, p) ->
                                        (Offset(x(p.zeit), y(p.sekunden)) - ort).getDistance() <= 24.dp.toPx()
                                    }
                                // Ein zweites Tippen auf die eigene, schon
                                // gewählte Schicht öffnet sie — der Klick im Web.
                                val vorher = aktiv
                                if (naechster != null && vorher != null && naechster.second == vorher.second &&
                                    naechster.second.code != null
                                ) {
                                    beiSchicht(naechster.second.code!!)
                                } else {
                                    aktiv = naechster
                                }
                            }
                        },
                ) {
                    val rand = 8.dp.toPx()
                    fun x(t: Long) = if (tMax == tMin) size.width / 2 else
                        rand + (t - tMin).toFloat() / (tMax - tMin) * (size.width - 2 * rand)
                    fun y(s: Double) = rand + (1f - (s / hoechst).toFloat()) * (size.height - 2 * rand)
                    listOf(0.0, 0.5, 1.0).forEach { a ->
                        val yy = y(hoechst * a)
                        drawLine(
                            Farben.Rand,
                            Offset(0f, yy),
                            Offset(size.width, yy),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                        )
                    }
                    serien.forEach { serie ->
                        serie.punkte.zipWithNext().forEach { (a, b) ->
                            drawLine(
                                serie.farbe,
                                Offset(x(a.zeit), y(a.sekunden)),
                                Offset(x(b.zeit), y(b.sekunden)),
                                strokeWidth = 2.dp.toPx(),
                            )
                        }
                        serie.punkte.forEach { p ->
                            drawCircle(serie.farbe, 3.5.dp.toPx(), Offset(x(p.zeit), y(p.sekunden)))
                        }
                    }
                    aktiv?.let { (serie, p) ->
                        val mitte = Offset(x(p.zeit), y(p.sekunden))
                        drawCircle(serie.farbe.copy(alpha = 0.25f), 9.dp.toPx(), mitte)
                        drawCircle(serie.farbe, 5.dp.toPx(), mitte)
                    }
                }
            }
            val gewaehlt = aktiv
            if (gewaehlt != null) {
                val (serie, p) = gewaehlt
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = serie.farbe, fontWeight = FontWeight.Bold)) { append(serie.name) }
                        p.ort?.let { append(" · $it") }
                        append(" · ${verlaufsdatum(p.zeit)} · ")
                        withStyle(SpanStyle(color = Farben.Text)) { append(dauer(p.sekunden).orEmpty()) }
                    },
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                )
                p.code?.let { code -> Textweg("Diese Schicht ansehen ›", { beiSchicht(code) }) }
            } else {
                SehrLeise("Punkt antippen zeigt den Wert.", mono = true)
            }
            if (serien.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                    serien.forEach { s ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).background(s.farbe, CircleShape))
                            Text(s.name, style = Schrift.Winzig, color = Farben.TextLeise, maxLines = 1)
                        }
                    }
                }
            }
            // Punkte in einer Kurve sind am Handy kein Ziel für einen Finger.
            // Die eigenen Schichten stehen deshalb darunter als Zeile zum
            // Antippen — derselbe Weg, den im Web der Klick auf den Punkt nimmt.
            if (gewaehlt == null) {
                eigene.punkte.lastOrNull()?.code?.let { code ->
                    Textweg("Letzte Schicht in der Kurve ansehen ›", { beiSchicht(code) })
                }
            }
        }
    }
}

// ------------------------------------------------------------- Schichten

/**
 * Alle Schichten — mit Suche, Kreis- und Organisationsfilter, nach Monaten
 * gruppiert, und dem Auszug als Datei.
 *
 * Die Einzelbuchungen klappen an der Zeile auf; die ganze Nachbesprechung
 * öffnet der Knopf darin.
 */
@Composable
private fun Schichten(
    daten: Dienstbuchdaten,
    fahrzeuge: List<Fahrzeugvorlage>,
    posten: Map<String, List<Erfahrungsposten>>,
    beiBuchungen: (String) -> Unit,
    beiSchicht: (String) -> Unit,
    beiDienst: () -> Unit,
) {
    val schichten = daten.schichten
    if (schichten.isEmpty()) {
        Leerhinweis("Noch nichts im Buch. Nach deinem ersten Dienstende steht die erste Schicht hier.") {
            Knopf("Erste Schicht fahren", beiDienst, art = Knopfart.Haupt)
        }
        return
    }

    var suche by rememberSaveable { mutableStateOf("") }
    var kreis by rememberSaveable { mutableStateOf<String?>(null) }
    var org by rememberSaveable { mutableStateOf<String?>(null) }
    var wahl by remember { mutableStateOf<String?>(null) }
    var offen by rememberSaveable { mutableStateOf<String?>(null) }
    val zusammenhang = LocalContext.current

    fun organisation(s: Schichtzeile): String? = when {
        s.rolle == "Leitstelle" -> "Leitstelle"
        s.fahrzeugtyp == null -> null
        else -> fahrzeuge.firstOrNull { it.typ == s.fahrzeugtyp }?.organisation?.let { ORG_NAME[it] ?: it }
    }

    val kreise = schichten.mapNotNull { it.landkreis }.distinct().sorted()
    val organisationen = schichten.mapNotNull { organisation(it) }.distinct().sorted()
    val gefiltert = schichten.filter { s ->
        if (kreis != null && s.landkreis != kreis) return@filter false
        if (org != null && organisation(s) != org) return@filter false
        val t = suche.trim().lowercase()
        t.isEmpty() || s.ort.lowercase().contains(t) || s.code.lowercase().contains(t) ||
            s.funkrufname?.lowercase()?.contains(t) == true
    }
    val filtert = suche.isNotBlank() || kreis != null || org != null

    Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Ort, Raumcode oder Funkrufname …")
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Wahlfeld("Kreis", kreis, { wahl = "kreis" }, Modifier.weight(1f), platzhalter = "Alle Kreise")
        Wahlfeld("Organisation", org, { wahl = "org" }, Modifier.weight(1f), platzhalter = "Alle")
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val punkte = gefiltert.sumOf { it.punkte ?: 0 }
        SehrLeise(
            "${zahl(gefiltert.size)} ${if (gefiltert.size == 1) "Schicht" else "Schichten"} · " +
                "${zahl(gefiltert.sumOf { it.einsaetze })} Einsätze · " +
                "${if (punkte < 0) "−" else ""}${zahl(Math.abs(punkte))} Punkte" +
                if (filtert) " · von ${zahl(schichten.size)}" else "",
            modifier = Modifier.weight(1f),
        )
        if (filtert) {
            Knopf("Zurücksetzen", {
                suche = ""
                kreis = null
                org = null
            }, art = Knopfart.Leise, kompakt = true)
        }
    }
    // Der Auszug wird eine Datei, deren Ort man selbst wählt — wie der
    // Datenauszug im Konto. Ein Teilen-Blatt mit dem CSV als Text landete in
    // Mail-Apps als Fließtext und stieße bei langen Büchern an die
    // Größengrenze eines Intents. Semikolon und BOM wie im Web
    // (`SchichtenView.vue`): Excel in deutscher Umgebung liest sonst eine
    // einzige Spalte und die Umlaute falsch.
    var auszug by remember { mutableStateOf<String?>(null) }
    val ablegen = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv"),
    ) { ziel ->
        val text = auszug
        if (ziel != null && text != null) {
            runCatching {
                zusammenhang.contentResolver.openOutputStream(ziel)?.use {
                    it.write(("\uFEFF" + text).toByteArray(Charsets.UTF_8))
                }
            }
        }
        auszug = null
    }
    Knopf(
        "⤓ Als CSV speichern",
        {
            auszug = schichtenauszug(gefiltert, ::organisation)
            runCatching { ablegen.launch("pagerspass-schichten-${java.time.LocalDate.now()}.csv") }
        },
        aktiv = gefiltert.isNotEmpty(),
        kompakt = true,
    )

    if (gefiltert.isEmpty()) {
        Leerhinweis(if (suche.isNotBlank()) "Nichts gefunden zu „$suche\"." else "Keine Schicht passt zu diesem Filter.")
    }

    val monat = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.GERMAN)
    gefiltert.groupBy { s ->
        instant(s.beendetUm)?.atZone(ZoneId.systemDefault())?.let { monat.format(it) } ?: "Ohne Datum"
    }.forEach { (titel, liste) ->
        val summe = liste.sumOf { it.punkte ?: 0 }
        Abschnitt(titel.replaceFirstChar { it.uppercase() }, weiterweg = {
            SehrLeise("${liste.size} · ${if (summe >= 0) "+" else "−"}${zahl(Math.abs(summe))} P", mono = true)
        }) {
            liste.forEach { s ->
                Schichtzeile(
                    schicht = s,
                    organisation = organisation(s),
                    beiDruck = {
                        offen = if (offen == s.code) null else s.code
                        if (offen != null) beiBuchungen(s.code)
                    },
                )
                if (offen == s.code) {
                    Buchungen(posten[s.code], s.punkte)
                    Knopf("Schicht ansehen", { beiSchicht(s.code) }, kompakt = true)
                }
            }
        }
    }

    when (wahl) {
        "kreis" -> Wahlblende(
            titel = "Kreis",
            gruppen = listOf(null to listOf("") + kreise),
            aufschrift = { it.ifBlank { "Alle Kreise" } },
            gewaehlt = kreis ?: "",
            beiWahl = {
                kreis = it.ifBlank { null }
                wahl = null
            },
            beiSchliessen = { wahl = null },
            suchbar = kreise.size > 8,
        )
        "org" -> Wahlblende(
            titel = "Organisation",
            gruppen = listOf(null to listOf("") + organisationen),
            aufschrift = { it.ifBlank { "Alle Organisationen" } },
            gewaehlt = org ?: "",
            beiWahl = {
                org = it.ifBlank { null }
                wahl = null
            },
            beiSchliessen = { wahl = null },
        )
    }
}

/**
 * Der Auszug — dieselben Spalten wie im Web, Semikolon getrennt, damit eine
 * deutsche Tabellenkalkulation ihn ohne Nachfrage in Spalten legt.
 */
private fun schichtenauszug(liste: List<Schichtzeile>, organisation: (Schichtzeile) -> String?): String {
    fun feld(wert: Any?) = if (wert == null) "" else "\"${wert.toString().replace("\"", "\"\"")}\""
    val kopf = listOf(
        "Beendet", "Raumcode", "Ort", "Landkreis", "Rolle", "Organisation",
        "Funkrufname", "Einsaetze", "Hilfsfrist (s)", "Punkte",
    ).joinToString(";")
    val zeilen = liste.map { s ->
        listOf(
            s.beendetUm, s.code, s.ort, s.landkreis, s.rolle, organisation(s),
            s.funkrufname, s.einsaetze, s.hilfsfristSekunden, s.punkte,
        ).joinToString(";") { feld(it) }
    }
    return (listOf(kopf) + zeilen).joinToString("\r\n")
}

/**
 * Eine Schicht im Buch.
 *
 * Ort und Kreis oben, darunter in Monospace, was Technik dazu schreibt: Datum,
 * Rufname, Einsätze, Hilfsfrist. Rechts die Punkte — sie sind der Grund, warum
 * jemand die Zeile überhaupt sucht. Der farbige Rand links ist die
 * Organisation, wie im Web (TASK-149).
 */
@Composable
private fun Schichtzeile(
    schicht: Schichtzeile,
    organisation: String?,
    beiDruck: () -> Unit,
    /** Gesetzt, wenn die Zeile in einer Karte steht: randlos, mit Trennstrich bis auf die letzte. */
    letzte: Boolean? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (letzte == null) Modifier.flaeche(ecke = 9.dp) else Modifier.zeilenstrich(letzte))
            .clickable(role = Role.Button, onClick = beiDruck)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = schicht.ort.ifBlank { schicht.code },
                style = Schrift.Normal,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SehrLeise(
                text = listOfNotNull(
                    zeitpunkt(schicht.beendetUm),
                    if (schicht.rolle == "Leitstelle") "Leitstelle" else schicht.funkrufname,
                    organisation?.takeIf { it != "Leitstelle" },
                    "${schicht.einsaetze} Einsätze",
                    hilfsfrist(schicht.hilfsfristSekunden)?.let { "HF $it" },
                ).joinToString(" · "),
                mono = true,
            )
        }
        val p = schicht.punkte
        if (p != null) {
            Marke(
                text = "${if (p >= 0) "+" else ""}$p",
                farbe = if (p >= 0) Farben.GruenHell else Farben.SignalHell,
            )
        }
    }
}

/** Wofür es Punkte gab — beim Aufklappen einer Schicht. */
@Composable
private fun Buchungen(posten: List<Erfahrungsposten>?, punkte: Int?) {
    Kasten(abstandInnen = Abstand.Winzig, farbe = Farben.BgTief) {
        when {
            posten == null -> Ladezeile("Buchungen werden geholt …")
            posten.isEmpty() && punkte == null ->
                SehrLeise("Für diese Schicht wurde nichts gebucht — sie war ungewertet.")
            posten.isEmpty() -> SehrLeise("Die Einzelbuchungen dieser Schicht sind nicht nachzulesen.")
            else -> posten.forEach { p ->
                Wertzeile(
                    p.text,
                    "${if (p.punkte >= 0) "+" else ""}${p.punkte}",
                    farbe = if (p.punkte >= 0) Farben.GruenHell else Farben.SignalHell,
                )
            }
        }
    }
}

// -------------------------------------------------------------- Laufbahn

/** Was eine Stufe bringt — als Zeilen an der Stufenkarte. */
private data class Gabe(
    val funktion: Boolean,
    val text: String,
    /** Nur bei Melder-Gesichtern: die zwei Vorschautöne. */
    val gehaeuse: Color? = null,
    val lcd: Color? = null,
)

private fun gaben(r: Rang, daten: Dienstbuchdaten): List<Gabe> = buildList {
    when {
        r.level == 1 -> add(Gabe(false, "Grundausstattung: 7 Fzg."))
        r.fahrzeuge == 1 -> add(Gabe(false, "+1 Fahrzeuggutschein"))
        r.fahrzeuge > 1 -> add(Gabe(false, "+${r.fahrzeuge} Fahrzeuggutscheine"))
    }
    daten.freischaltungen.filter { it.abLevel == r.level }.forEach { add(Gabe(true, it.bezeichnung)) }
    zierstueckeDerStufe(r.level).forEach { add(Gabe(false, it.text, it.gehaeuse, it.lcd)) }
}

/**
 * Die Laufbahn — wo man steht, was die nächste Stufe bringt, alle Stufen als
 * Band, dann Tagesliste, Saisonwertung, Bestenliste und die Mitspieler.
 *
 * Die Stufenkarten tragen neben Gutscheinen und Funktionen auch die Zierstücke
 * (Gehäuse, Melder-Gesichter, Rahmen, Kopfmuster, Wappen, Töne, Schichtkarten)
 * — aus denselben Katalogen wie im Web, siehe `Laufbahngaben.kt`.
 */
@Composable
private fun Laufbahn(konto: Konto?, daten: Dienstbuchdaten) {
    val laufbahn = daten.laufbahn
    if (laufbahn.isNotEmpty() && konto != null) {
        val i = laufbahn.indexOfFirst { it.level == konto.level }
        val aktuell = laufbahn.getOrNull(i)
        val naechste = laufbahn.getOrNull(i + 1)
        val anteil = if (aktuell == null) 0f else if (naechste == null) 1f else
            ((konto.erfahrung - aktuell.ab).toFloat() / (naechste.ab - aktuell.ab)).coerceIn(0f, 1f)

        Kasten(abstandInnen = Abstand.Klein, marke = true) {
            Etikett("Deine Laufbahn")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (aktuell?.bezeichnung ?: konto.rang).replace("/", "/\u200b"),
                    style = Schrift.Titel,
                    color = Farben.Text,
                    modifier = Modifier.weight(1f),
                )
                Marke("Stufe ${konto.level}", farbe = Farben.AmberHell)
            }
            Fortschritt(anteil = anteil)
            SehrLeise("${zahl(konto.erfahrung)} Punkte · ${konto.stufentext}", mono = true)
            if (naechste != null) {
                val g = gaben(naechste, daten)
                Etikett("Mit Stufe ${naechste.level}")
                if (g.isEmpty()) SehrLeise(naechste.bezeichnung)
                g.take(3).forEach { Text("· ${it.text}", style = Schrift.Klein, color = Farben.TextLeise) }
            }
        }

        val meilensteine = laufbahn
            .filter { it.level > konto.level && gaben(it, daten).any { g -> g.funktion } }
            .take(4)
        if (meilensteine.isNotEmpty()) {
            Abschnitt("Die nächsten Meilensteine") {
                meilensteine.forEach { r ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .flaeche(ecke = 9.dp)
                            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                    ) {
                        Text("${r.level}", style = Schrift.Titel, color = Farben.Amber)
                        Column(Modifier.weight(1f)) {
                            Text(gaben(r, daten).first { it.funktion }.text, style = Schrift.Normal, color = Farben.Text)
                            SehrLeise("noch ${zahl((r.ab - konto.erfahrung).coerceAtLeast(0))} Punkte", mono = true)
                        }
                    }
                }
            }
        }

        Abschnitt("Alle Stufen") {
            val band = rememberLazyListState(initialFirstVisibleItemIndex = (i - 1).coerceAtLeast(0))
            LazyRow(
                state = band,
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                contentPadding = PaddingValues(vertical = Abstand.Winzig),
            ) {
                items(laufbahn, key = { it.level }) { r ->
                    Stufenkarte(r, konto, gaben(r, daten), if (r.level == konto.level) anteil else null)
                }
            }
        }
    }

    daten.tagesschicht?.let { t ->
        Buchkarte("Schicht des Tages", zahl = t.landkreis) {
            SehrLeise(
                "Heute gefahren: ${t.gefahren} — " + when {
                    t.eigenerPlatz != null -> "du stehst auf Platz ${t.eigenerPlatz}."
                    t.selbstGefahren -> "du bist dabei; dein erster Versuch zählt."
                    else -> "du noch nicht. Der erste gewertete Versuch zählt."
                },
            )
            if (t.beste.isEmpty()) {
                Leerhinweis("Heute ist noch niemand gefahren — die Liste gehört dir.")
            } else {
                Rangliste(t.beste.map { Triple(it.platz, it.anzeigename, zahl(it.punkte)) to it.istEigenerEintrag })
            }
        }
    }

    if (daten.saison.isNotEmpty()) {
        val monat = DateTimeFormatter.ofPattern("LLLL", Locale.GERMAN).format(java.time.LocalDate.now())
        Buchkarte("Saisonwertung", zahl = monat.replaceFirstChar { it.uppercase() }) {
            if (konto != null && daten.saison.none { it.istEigenerEintrag }) {
                SehrLeise("Du stehst diesen Monat noch nicht unter den ersten ${daten.saison.size}.")
            }
            Rangliste(daten.saison.map { Triple(it.platz, it.anzeigename, zahl(it.punkte)) to it.istEigenerEintrag })
        }
    }

    if (daten.bestenliste.isNotEmpty()) {
        Buchkarte("Bestenliste", zahl = "aller Zeiten") {
            if (konto != null && daten.bestenliste.none { it.istEigenerEintrag }) {
                SehrLeise("Du stehst noch nicht unter den ersten ${daten.bestenliste.size}.")
            }
            Rangliste(
                daten.bestenliste.map {
                    Triple(it.platz, "${it.anzeigename} · ${it.rang}", zahl(it.erfahrung)) to it.istEigenerEintrag
                },
            )
        }
    }

    val statistik = daten.statistik
    if (statistik != null && statistik.spieler.isNotEmpty()) {
        Buchkarte(
            "Mit wem du gefahren bist",
            zahl = "${statistik.runden} ${if (statistik.runden == 1) "Schicht" else "Schichten"}",
        ) {
            statistik.spieler.forEach { Bilanzzeile(it) }
        }
    }
}

@Composable
private fun Stufenkarte(r: Rang, konto: Konto, gaben: List<Gabe>, anteil: Float?) {
    val erreicht = r.level < konto.level
    val aktuell = r.level == konto.level
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .width(150.dp)
            .height(170.dp)
            .flaeche(
                ecke = 9.dp,
                randfarbe = when {
                    aktuell -> Farben.Amber
                    gaben.any { it.funktion } -> Farben.AmberTief
                    else -> Farben.Rand
                },
            )
            .padding(Abstand.Klein),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${r.level}",
                style = Schrift.Gross.copy(fontWeight = FontWeight.Bold),
                color = if (aktuell) Farben.Amber else Farben.Text,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (erreicht) "✓" else if (!aktuell) "🔒" else "",
                style = Schrift.Klein,
                color = if (erreicht) Farben.GruenHell else Farben.TextSehrLeise,
            )
        }
        Text(r.bezeichnung, style = Schrift.Klein, color = Farben.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        SehrLeise("ab ${zahl(r.ab)} P.", mono = true)
        val sichtbar = if (gaben.size <= 3) gaben else gaben.take(2)
        sichtbar.forEach {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Haar),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Ein Melder-Gesicht zeigt seine zwei Töne — Gehäuse und
                // Display —, wie die kleine Probe auf der Karte im Web.
                if (it.gehaeuse != null && it.lcd != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(width = 14.dp, height = 10.dp).background(it.gehaeuse, Rundung.Winzig),
                    ) {
                        Box(Modifier.size(width = 9.dp, height = 5.dp).background(it.lcd, RoundedCornerShape(1.dp)))
                    }
                }
                Text(
                    it.text,
                    style = Schrift.Winzig,
                    color = if (it.funktion) Farben.AmberHell else Farben.TextLeise,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (gaben.size > 3) SehrLeise("+${gaben.size - 2} weitere")
        if (anteil != null) {
            Box(Modifier.weight(1f))
            Fortschritt(anteil = anteil)
        }
    }
}

/**
 * Eine Rangliste (`.db-rangliste`) — Platz im Kreis, Gold, Silber, Bronze für die
 * ersten drei, die eigene Zeile in Amber mit Kante.
 */
@Composable
private fun Rangliste(zeilen: List<Pair<Triple<Int, String, String>, Boolean>>) {
    Column {
        zeilen.forEachIndexed { i, (z, eigen) ->
            val (platz, name, wert) = z
            Rangzeile(platz = platz, wert = wert, letzte = i == zeilen.lastIndex, eigen = eigen) {
                Text(
                    name,
                    style = Schrift.Klein,
                    color = if (eigen) Farben.Amber else Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Die Bilanz eines Mitspielers — sieben Zahlen, am Handy als zwei Zeilen statt einer Tabelle. */
@Composable
fun Bilanzzeile(b: Spielerbilanz) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 9.dp)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Row {
            Text(b.name, style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f))
            SehrLeise(if (b.rolle == "Leitstelle") "Leitstelle" else "Besatzung")
        }
        SehrLeise(
            listOfNotNull(
                if (b.runden > 1) "${b.runden} Schichten" else null,
                "${b.einsaetze} Einsätze",
                "${b.lagemeldungen} Lagemeldungen",
                "${b.funksprueche} Funksprüche",
                b.ausrueckzeitSekunden?.let { "Ausrücken ${dauerMitEinheit(it)}" },
                if (b.ausserDienstSekunden >= 60) "außer Dienst ${dauer(b.ausserDienstSekunden)} min" else null,
            ).joinToString(" · "),
            mono = true,
        )
    }
}

// ------------------------------------------------------------- Abzeichen

private val KATEGORIEN = listOf(
    "Meilensteine", "Feuerwehr", "Rettungsdienst", "THW", "Polizei",
    "Zeiten", "Einsatzarten", "Fuhrpark", "Landkreise",
)

private fun kategorierang(k: String): Int = KATEGORIEN.indexOf(k).let { if (it < 0) KATEGORIEN.size else it }

/**
 * Die Abzeichenwand — nach Kategorien, mit Suche und Vitrine.
 *
 * <b>Gedeckelt je Kategorie, und der Deckel steht dabei.</b> Der Server
 * liefert rund tausend Abzeichen; je Kategorie stehen zuerst die erreichten
 * und höchstens 24, darunter ein Knopf für den Rest. Sucht man, fällt der
 * Deckel weg — wer etwas sucht, will es finden.
 *
 * Der Stern an einem erreichten Abzeichen stellt es in die Vitrine des Profils
 * (höchstens sechs).
 */
@Composable
private fun Abzeichenwand(
    abzeichen: List<Abzeichen>,
    vitrine: List<String>,
    meldung: String?,
    beiLaden: () -> Unit,
    beiVitrine: (Abzeichen) -> Unit,
) {
    LaunchedEffect(Unit) { beiLaden() }
    if (abzeichen.isEmpty()) {
        Leerhinweis("Noch keine Abzeichen geladen.")
        return
    }
    var tor by rememberSaveable { mutableStateOf<String?>(null) }
    var suche by rememberSaveable { mutableStateOf("") }
    var aufgeklappt by remember { mutableStateOf(setOf<String>()) }

    val erreicht = abzeichen.count { it.erreicht }
    Kasten(abstandInnen = Abstand.Klein) {
        Wertzeile("Erreicht", "${zahl(erreicht)} von ${zahl(abzeichen.size)}")
        Fortschritt(anteil = erreicht.toFloat() / abzeichen.size)
        SehrLeise("In der Vitrine: ${vitrine.size} von ${Werkstatt.VITRINE_HOECHSTENS} — der Stern stellt ein Abzeichen in dein Profil.")
    }
    if (meldung != null) Rueckmeldung(null, meldung)

    val zuletzt = abzeichen.filter { it.erreicht && it.erreichtAm != null }.sortedByDescending { it.erreichtAm }.take(8)
    if (zuletzt.isNotEmpty()) {
        Buchkarte("Zuletzt erreicht", geraeumig = true) {
            zuletzt.forEach { Abzeichenzeile(it, it.id in vitrine, beiVitrine) }
        }
    }

    Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Abzeichen suchen …")

    val kategorien = abzeichen.groupBy { it.kategorie }.toList().sortedBy { kategorierang(it.first) }
    Pillenreihe {
        Pille("Alle", tor == null, { tor = null }, zahl = erreicht)
        kategorien.forEach { (k, liste) ->
            Pille(k, tor == k, { tor = k }, zahl = liste.count { it.erreicht })
        }
    }

    val sucht = suche.isNotBlank()
    val sichtbar = abzeichen.filter { a ->
        (sucht || tor == null || a.kategorie == tor) &&
            (!sucht || a.titel.contains(suche, true) || a.beschreibung.contains(suche, true))
    }
    if (sichtbar.isEmpty()) Leerhinweis("Kein Abzeichen passt zu „$suche\".")

    sichtbar.groupBy { it.kategorie }.toList().sortedBy { kategorierang(it.first) }.forEach { (k, liste) ->
        val gekuerzt = tor == null && !sucht && k !in aufgeklappt
        val zeigen = if (gekuerzt) liste.sortedByDescending { it.erreicht }.take(JE_KATEGORIE) else liste
        Abschnitt(k, weiterweg = { SehrLeise("${liste.count { it.erreicht }} / ${liste.size}") }) {
            zeigen.forEach { Abzeichenzeile(it, it.id in vitrine, beiVitrine) }
            if (gekuerzt && liste.size > JE_KATEGORIE) {
                Textweg("Alle ${liste.size} zeigen", { aufgeklappt = aufgeklappt + k })
            }
        }
    }
}

private const val JE_KATEGORIE = 24

@Composable
private fun Abzeichenzeile(abzeichen: Abzeichen, inVitrine: Boolean, beiVitrine: (Abzeichen) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 9.dp, randfarbe = if (abzeichen.erreicht) Farben.AmberTief else Farben.Rand)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
            Text(
                abzeichen.titel,
                style = Schrift.Normal,
                color = if (abzeichen.erreicht) Farben.Text else Farben.TextLeise,
            )
            SehrLeise(abzeichen.beschreibung)
            if (abzeichen.erreichtAm != null) SehrLeise("erreicht am ${tag(abzeichen.erreichtAm)}", mono = true)
        }
        if (abzeichen.erreicht) {
            Knopf(
                if (inVitrine) "★" else "☆",
                { beiVitrine(abzeichen) },
                art = if (inVitrine) Knopfart.Haupt else Knopfart.Leise,
                kompakt = true,
            )
        }
    }
}

// ------------------------------------------------------------ Auswertung

/**
 * Die Auswertung über alle Schichten — nur mit Abo.
 *
 * <b>Ohne Abo steht das Angebot da, nicht die Zahlen</b> — mit denselben
 * Überschriften, die man hinterher sieht, und einem Weg zur Webseite. Das Abo
 * selbst wird dort abgeschlossen und nirgends in der App.
 */
@Composable
private fun Auswertung(stand: Werkstand, beiPremium: () -> Unit, beiLaden: () -> Unit) {
    LaunchedEffect(Unit) { beiLaden() }
    val a = stand.auswertung

    when {
        !a.geladen -> Ladezeile("Die Auswertung wird gerechnet …")

        stand.auswertungGesperrt -> Kasten(abstandInnen = Abstand.Klein, marke = true) {
            Etikett("Auswertung")
            Text("Dein ganzes Dienstbuch auf einen Blick", style = Schrift.Titel, color = Farben.Text)
            Text(
                "Die Übersicht zeigt deine letzten zehn Schichten. Die Auswertung zeigt alle — " +
                    "wie oft du Dienst tust, wie sich deine Hilfsfrist über die Monate entwickelt " +
                    "und welchen Wagen du am liebsten fährst.",
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
            listOf(
                "Schichten je Woche über ein halbes Jahr",
                "Deine Hilfsfrist im Verlauf, nicht nur die letzte",
                "Leitstelle oder Fahrzeug — und welches",
            ).forEach { Text("· $it", style = Schrift.Klein, color = Farben.Text) }
            SehrLeise("Die Auswertung gehört zu Premium. Premium schließt du auf der Webseite ab.")
            Knopf("Premium auf der Webseite", beiPremium, art = Knopfart.Haupt)
        }

        a.fehler != null -> Rueckmeldung(null, a.fehler)

        a.inhalt == null || a.inhalt.schichten <= 0 ->
            Leerhinweis("Hier steht deine Auswertung, sobald du die erste Schicht gefahren hast.")

        else -> {
            val w = a.inhalt
            Abschnitt("Seit dem ${tag(w.ersteSchicht)}") {
                Kennzahltafel(
                    listOf(
                        { m ->
                            Kennzahlkachel(
                                "Schichten", "${w.schichten}",
                                "${w.leitstellenschichten}× Leitstelle · ${w.fahrzeugschichten}× Fahrzeug", m,
                            )
                        },
                        { m ->
                            Kennzahlkachel(
                                "Einsätze", "${w.einsaetze}",
                                "${kommazahl(w.einsaetze.toDouble() / w.schichten)} je Schicht", m,
                                farbe = Farben.Blau,
                            )
                        },
                        { m ->
                            Kennzahlkachel(
                                "Ø Hilfsfrist", dauer(w.hilfsfristSekunden) ?: "—",
                                w.besteHilfsfristSekunden?.let { "beste ${dauer(it)}" } ?: "—", m,
                                farbe = Farben.Gruen,
                            )
                        },
                        { m ->
                            Kennzahlkachel("Punkte", zahl(w.punkte), "letzte Schicht ${tag(w.letzteSchicht)}", m)
                        },
                    ),
                )
            }

            if (w.verlauf.isNotEmpty()) {
                Buchkarte("Die letzten Wochen", geraeumig = true) {
                    Wochenbild(w.verlauf)
                }
            }

            if (w.fahrzeuge.isNotEmpty()) {
                val spitze = w.fahrzeuge.maxOf { it.schichten }.coerceAtLeast(1)
                Buchkarte("Womit du fährst", geraeumig = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        w.fahrzeuge.forEach { f ->
                            Balkenzeile(
                                f.typ,
                                "${f.schichten} Schichten · ${f.einsaetze} Einsätze",
                                f.schichten.toFloat() / spitze,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Die Wochen: Balken für die Zahl der Schichten, darüber die Hilfsfrist als
 * Punktreihe — in einem Bild, weil man genau ihren Zusammenhang sucht.
 *
 * Die Fristachse beginnt nicht bei null: Zwischen 6:30 und 7:10 liegt der ganze
 * Unterschied, den man sehen will. Eine kurze Frist ist die bessere und steht
 * oben.
 */
@Composable
private fun Wochenbild(verlauf: List<de.pagerspass.pagerspass.netz.Dienstwoche>) {
    val spitze = verlauf.maxOf { it.schichten }.coerceAtLeast(1)
    val fristen = verlauf.mapNotNull { it.hilfsfristSekunden }
    val min = fristen.minOrNull()
    val max = fristen.maxOrNull()?.let { if (min != null && it - min < 10) min + 10 else it }

    Kasten(abstandInnen = Abstand.Klein) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val breite = size.width / verlauf.size
            verlauf.forEachIndexed { i, woche ->
                val h = woche.schichten.toFloat() / spitze * (size.height * 0.9f)
                drawRect(
                    Farben.AmberTief,
                    topLeft = Offset(i * breite + breite * 0.15f, size.height - h),
                    size = androidx.compose.ui.geometry.Size(breite * 0.7f, h),
                )
            }
            if (min != null && max != null) {
                val punkte = verlauf.mapIndexedNotNull { i, woche ->
                    woche.hilfsfristSekunden?.let { s ->
                        Offset(
                            i * breite + breite / 2,
                            ((s - min) / (max - min)).toFloat() * (size.height * 0.8f) + size.height * 0.1f,
                        )
                    }
                }
                punkte.zipWithNext().forEach { (a, b) -> drawLine(Farben.GruenHell, a, b, strokeWidth = 2.dp.toPx()) }
                punkte.forEach { drawCircle(Farben.GruenHell, 3.dp.toPx(), it) }
            }
        }
        Row {
            SehrLeise(wochentag(verlauf.first().beginn), modifier = Modifier.weight(1f))
            SehrLeise(wochentag(verlauf.last().beginn))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Farben.AmberTief))
                SehrLeise("Schichten (höchstens $spitze)")
            }
            if (min != null && max != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Farben.GruenHell, CircleShape))
                    SehrLeise("Hilfsfrist ${dauer(min)}–${dauer(max)}")
                }
            }
        }
    }
}

private fun wochentag(beginn: String): String {
    val teile = beginn.take(10).split("-")
    return if (teile.size == 3) "${teile[2]}.${teile[1]}." else beginn
}

// ----------------------------------------------------- Eine Schicht öffnen

/**
 * Eine vergangene Schicht — die Nachbesprechung aus dem Archiv.
 *
 * Übertragen aus dem, was `DebriefingView.vue` mit einer `ArchivRunde` zeigt:
 * Kennzahlen, die eigenen Buchungen, die Befunde der Auswertung, jeder Einsatz
 * mit seiner Zeitachse und seinem Ablauf, das Anrufjournal, die Mitspieler und
 * das Funkprotokoll — und die Wiedergabe der Fahrwege auf der Lagekarte
 * (`Schichtwiedergabe.kt`).
 */
@Composable
fun SchichtSeite(
    code: String,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Werkstand = Werkstand(),
    werkstatt: Werkstatt? = null,
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(code) { werkstatt?.archivrundeLaden(code) }
    var funkAlle by remember { mutableStateOf(false) }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = stand.archivrunde.inhalt?.settings?.let { it.landkreis ?: it.ort }?.ifBlank { null } ?: "Schicht",
            unterzeile = "Raum $code",
            knoepfe = { Knopf("← Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = stand.archivrunde.laedt,
            fehler = stand.archivrunde.fehler,
            inhalt = stand.archivrunde.inhalt,
            beiErneut = { werkstatt?.archivrundeLaden(code) },
        ) { runde ->
            Archivkopf(runde)

            val posten = stand.posten[code]
            if (!posten.isNullOrEmpty()) {
                Abschnitt("Deine Punkte", weiterweg = { SehrLeise("${posten.sumOf { it.punkte }} P", mono = true) }) {
                    Buchungen(posten, posten.sumOf { it.punkte })
                }
            }

            Schichtwiedergabe(runde)

            val befunde = runde.auswertung.befunde
            if (befunde.isNotEmpty()) {
                Abschnitt("Was auffiel") {
                    listOf("Mangel", "Hinweis", "Lob").forEach { grad ->
                        befunde.filter { it.grad == grad }.forEach { Befundzeile(it, runde) }
                    }
                }
            }

            val d = runde.auswertung.doppelmeldungen
            if (d.nennenswert) {
                Kasten(abstandInnen = Abstand.Winzig) {
                    Etikett("Doppelmeldungen")
                    Wertzeile("Mehrfach gemeldete Lagen", "${d.mehrfachGemeldeteLagen}")
                    Wertzeile("Zusammengeführt", "${d.zusammengefuehrt}")
                    Wertzeile("Falsch zugeordnet", "${d.falschzugeordnet}")
                    Wertzeile("Doppelt alarmiert", "${d.doppelalarmierungen}")
                }
            }

            if (runde.incidents.isNotEmpty()) {
                Abschnitt("Einsätze", weiterweg = { SehrLeise("${runde.incidents.size}") }) {
                    runde.incidents.sortedBy { it.eingangUm }.forEach { e ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                            modifier = Modifier
                                .fillMaxWidth()
                                .flaeche(ecke = 9.dp, randfarbe = orgFarbe(e.organisation).copy(alpha = 0.6f))
                                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                        ) {
                            Row {
                                Text(
                                    "${e.stichwort} ${e.stichwortText}".trim(),
                                    style = Schrift.Normal,
                                    color = Farben.Text,
                                    modifier = Modifier.weight(1f),
                                )
                                if (e.hilfsfristSekunden != null) {
                                    Marke("HF ${dauer(e.hilfsfristSekunden)}", farbe = Farben.GruenHell)
                                }
                            }
                            if (e.adresse.isNotBlank()) SehrLeise(e.adresse)
                            SehrLeise(
                                listOfNotNull(
                                    uhrzeit(e.eingangUm)?.let { "Notruf $it" },
                                    uhrzeit(e.erstAlarmUm)?.let { "Alarm $it" },
                                    uhrzeit(e.erstesFahrzeugVorOrtUm)?.let { "vor Ort $it" },
                                    uhrzeit(e.abgeschlossenUm)?.let { "Ende $it" },
                                ).joinToString(" → "),
                                mono = true,
                            )
                            if (e.alarmierteFahrzeuge.isNotEmpty()) {
                                SehrLeise(
                                    e.alarmierteFahrzeuge.joinToString(", ") { id ->
                                        runde.vehicles.firstOrNull { it.id == id }?.funkrufname?.ifBlank { null } ?: id
                                    },
                                )
                            }
                            // Der Ablauf des Einsatzes, Zeile für Zeile — die Chronik im Web.
                            e.chronologie.forEach { c ->
                                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                                    SehrLeise(uhrzeit(c.zeit).orEmpty(), mono = true)
                                    Text(
                                        c.text,
                                        style = Schrift.Klein,
                                        color = Farben.TextLeise,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Anrufjournal(runde)

            val bilanz = runde.auswertung.spieler
            if (bilanz.isNotEmpty()) {
                Abschnitt("Wer dabei war") { bilanz.forEach { Bilanzzeile(it) } }
            }

            if (runde.funkprotokoll.isNotEmpty()) {
                val zeigen = if (funkAlle) runde.funkprotokoll else runde.funkprotokoll.take(40)
                Abschnitt("Funkprotokoll", weiterweg = { SehrLeise("${runde.funkprotokoll.size}") }) {
                    Kasten(abstandInnen = Abstand.Winzig, farbe = Farben.BgTief) {
                        zeigen.forEach { z ->
                            Text(
                                "${uhrzeit(z.zeit) ?: ""}  ${z.von}${z.an?.let { " → $it" } ?: ""}: ${z.text}",
                                style = Schrift.MonoKlein,
                                color = if (z.kind == "System") Farben.TextSehrLeise else Farben.TextLeise,
                            )
                        }
                    }
                    if (!funkAlle && runde.funkprotokoll.size > 40) {
                        Textweg("Alle ${runde.funkprotokoll.size} Funksprüche zeigen", { funkAlle = true })
                    }
                }
            }
        }
    }
}

@Composable
private fun Archivkopf(runde: Archivrunde) {
    val einsaetze = runde.incidents.size
    val fertig = runde.incidents.count { it.state == "Abgeschlossen" }
    val fristen = runde.incidents.mapNotNull { it.hilfsfristSekunden }
    Kennzahltafel(
        listOf(
            { m -> Kennzahlkachel("Einsätze", "$einsaetze", "$fertig abgeschlossen", m) },
            { m ->
                Kennzahlkachel(
                    "Ø Hilfsfrist",
                    dauer(fristen.takeIf { it.isNotEmpty() }?.average()) ?: "—",
                    "über ${fristen.size} Einsätze", m, farbe = Farben.Gruen,
                )
            },
            { m ->
                Kennzahlkachel(
                    "Besatzung",
                    "${runde.players.count { !it.istBot }}",
                    "Menschen · ${runde.players.count { it.istBot }} Bots", m, farbe = Farben.Blau,
                )
            },
            { m ->
                Kennzahlkachel(
                    "Dauer",
                    spanne(runde.gestartetUm, runde.beendetUm) ?: "—",
                    zeitpunkt(runde.beendetUm), m,
                )
            },
        ),
    )
    SehrLeise(
        listOfNotNull(
            runde.settings.leitstelle.ifBlank { null },
            runde.settings.ort.ifBlank { null },
            when (runde.settings.mode) {
                "Frei" -> "Freie Vergabe"
                "Ausbildung" -> "Ausbildung"
                "Tagesschicht" -> "Schicht des Tages"
                "Szenario" -> "Übung"
                else -> null
            },
        ).joinToString(" · "),
    )
}

@Composable
private fun Befundzeile(b: Befund, runde: Archivrunde) {
    val farbe = when (b.grad) {
        "Lob" -> Farben.GruenHell
        "Mangel" -> Farben.SignalHell
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
            Text(b.titel, style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f))
        }
        SehrLeise(b.text)
        b.incidentId?.let { id ->
            runde.incidents.firstOrNull { it.id == id }?.let { e ->
                SehrLeise("${e.stichwort} · ${e.adresse}", mono = true)
            }
        }
    }
}

// ------------------------------------------------------------- Zeitwerte

private fun instant(roh: String?): Instant? =
    roh?.takeIf { it.isNotBlank() }?.let {
        runCatching { Instant.parse(it) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
    }

private fun zeitwert(roh: String?): Long? = instant(roh)?.toEpochMilli()

private fun tagKurz(roh: String): String =
    instant(roh)?.atZone(ZoneId.systemDefault())?.let { DateTimeFormatter.ofPattern("dd.MM.").format(it) } ?: "—"

private fun uhrzeit(roh: String?): String? =
    instant(roh)?.atZone(ZoneId.systemDefault())?.let { DateTimeFormatter.ofPattern("HH:mm").format(it) }

private fun spanne(von: String?, bis: String?): String? {
    val a = instant(von) ?: return null
    val b = instant(bis) ?: return null
    val minuten = java.time.Duration.between(a, b).toMinutes()
    return if (minuten >= 60) "${minuten / 60} h ${minuten % 60} min" else "$minuten min"
}

/** „24.09., 18:40" — Tag und Uhrzeit eines Punkts im Hilfsfristverlauf. */
private fun verlaufsdatum(ms: Long): String =
    DateTimeFormatter.ofPattern("dd.MM., HH:mm", Locale.GERMAN)
        .format(java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
