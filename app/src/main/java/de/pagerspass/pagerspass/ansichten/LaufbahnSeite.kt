package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Dienstbuchdaten
import de.pagerspass.pagerspass.mobil.Dienstbuchstand
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kontofreischaltung
import de.pagerspass.pagerspass.netz.Laufbahnstufe
import de.pagerspass.pagerspass.netz.Schichtbilanz
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Kataloge
import de.pagerspass.pagerspass.ui.schmuck.Schmuck
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.max

/**
 * Die Laufbahn — übertragen aus `views/dienstbuch/LaufbahnView.vue`.
 *
 * Drei Blicke auf dasselbe: wo stehe ich in meiner eigenen Laufbahn, wo unter
 * allen, und mit wem war ich unterwegs. Aus der Mannschaftsliste heraus lässt sich
 * anfragen, wer noch kein Freund ist — dasselbe wie nach einer Runde in der
 * Nachbesprechung, nur über die ganze Historie.
 */

/** Was eine Stufe freischaltet — als Zeile an der Stufenkarte. */
private data class Passgabe(
    /** `fahrzeug`, `funktion`, `geraet`, `gesicht`, `schmuck`. */
    val art: String,
    val text: String,
    val gehaeuse: Color? = null,
    val lcd: Color? = null,
)

/**
 * Die Belohnungen je Stufe, zusammengetragen aus dem, was die App ohnehin kennt:
 * Fahrzeugwahlen aus der Laufbahn selbst, Funktionen aus den Freischaltungen des
 * Kontos, alles Übrige aus den Katalogen. Die Regeln wohnen beim Server — hier
 * wird nur beschriftet.
 *
 * <b>Nur, was die Stufe verleiht.</b> Alles mit einem Preis gehört dem Shop und
 * wird gekauft, nicht erreicht — dieselbe Trennung wie am Server
 * (`PreisCredits is not null`).
 */
private fun gaben(r: Laufbahnstufe, freischaltungen: List<Kontofreischaltung>): List<Passgabe> = buildList {
    if (r.level == 1) {
        add(Passgabe("fahrzeug", "Grundausstattung: 7 Fzg."))
    } else if (r.fahrzeuge > 0) {
        add(
            Passgabe(
                "fahrzeug",
                if (r.fahrzeuge == 1) "+1 Fahrzeuggutschein" else "+${r.fahrzeuge} Fahrzeuggutscheine",
            ),
        )
    }

    freischaltungen.filter { it.abLevel == r.level }.forEach { add(Passgabe("funktion", it.bezeichnung)) }

    fun erspielt(abLevel: Int, preis: Int?) = abLevel == r.level && preis == null

    // Die Meldergehäuse zuerst — eine Karte trägt drei Zeilen, und wer auf
    // Stufe 10 aufsteigt, soll „Meldergerät" lesen und nicht „+1 weitere".
    Kataloge.MELDER_BAUFORMEN.forEach { b ->
        if (erspielt(b.abLevel, null) && b.abLevel > 1) add(Passgabe("geraet", "Meldergerät „${b.name}“"))
    }
    Kataloge.MELDER_GESICHTER.forEach { g ->
        if (erspielt(g.abLevel, g.preisCredits) && g.id != "standard" && g.id != "premium") {
            add(Passgabe("gesicht", "Melder „${g.name}“", g.gehaeuse, g.lcd))
        }
    }
    Schmuck.RAHMEN.forEach { s ->
        if (erspielt(s.abLevel, s.preisCredits) && s.id != "keiner" && s.id != "premium") {
            add(Passgabe("schmuck", "Rahmen „${s.name}“"))
        }
    }
    Schmuck.KOPFMUSTER.forEach { s ->
        if (erspielt(s.abLevel, s.preisCredits) && s.id != "keines" && s.id != "premium") {
            add(Passgabe("schmuck", "Kopfmuster „${s.name}“"))
        }
    }
    Kataloge.WAPPEN_AB_LEVEL.forEach { (zeichen, ab) ->
        if (ab == r.level && ab > 1) {
            add(Passgabe("schmuck", "Wappen „${Kataloge.WAPPEN_LABEL[zeichen] ?: zeichen}“"))
        }
    }
    if (Kataloge.GOLDFARBE_AB_LEVEL == r.level) add(Passgabe("schmuck", "Wappenfarbe Gold"))
    Kataloge.MELDER_TOENE.forEach { s ->
        if (s.abLevel == r.level && s.abLevel > 1) add(Passgabe("schmuck", "Alarmton „${s.name}“"))
    }
    Kataloge.SCHICHTKARTEN_DESIGNS.forEach { s ->
        if (s.abLevel == r.level && s.abLevel > 1) add(Passgabe("schmuck", "Schichtkarte „${s.name}“"))
    }
}

/**
 * Wie viele Freischaltungen eine Stufenkarte zeigt, bevor der Rest zusammengefasst
 * wird. Drei plus eine Sammelzeile — bei genau einer zu viel steht lieber die Zeile.
 */
private const val GABEN_SICHTBAR = 3

private fun passgaben(alle: List<Passgabe>): Pair<List<Passgabe>, Int> =
    if (alle.size <= GABEN_SICHTBAR + 1) alle to 0 else alle.take(GABEN_SICHTBAR) to (alle.size - GABEN_SICHTBAR)

/** Ob eine Stufe ein Gerät oder eine Funktion bringt — dann ist sie ein Meilenstein. */
private fun istMeilenstein(alle: List<Passgabe>) = alle.any { it.art == "geraet" || it.art == "funktion" }

private fun gabenfarbe(art: String): Color = when (art) {
    "fahrzeug" -> Farben.BlauHell
    "funktion" -> Farben.GruenHell
    "geraet" -> Farben.AmberHell
    "gesicht" -> Farben.AmberHell
    else -> Farben.ViolettHell
}

@Composable
fun DienstbuchLaufbahn(
    unterrand: Dp,
    konto: Konto?,
    stand: Dienstbuchstand,
    freunde: List<Freund>,
    beiReiter: (Buchreiter) -> Unit,
    beiLaden: () -> Unit,
    beiAnfragen: suspend (String) -> Result<Unit>,
) {
    LaunchedEffect(Unit) { beiLaden() }
    val buch = stand.buch

    Dienstbuchrahmen(
        unterrand = unterrand,
        konto = konto,
        hier = Buchreiter.Laufbahn,
        beiReiter = beiReiter,
        laedt = buch.ersteLadung,
        fehler = if (buch.inhalt == null) buch.fehler else null,
        beiErneut = beiLaden,
    ) {
        val daten = buch.inhalt ?: Dienstbuchdaten()

        if (daten.laufbahn.isNotEmpty()) {
            if (konto != null) Laufbahnkarte(konto, daten)
            Meilensteine(konto, daten)
            Pass(konto, daten)
        }

        daten.tagesschicht?.let { t ->
            Buchkarte(titel = "Schicht des Tages", zahl = t.landkreis) {
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
                    t.beste.forEach { b ->
                        Rangzeile(b.platz, b.anzeigename, null, zahl(b.punkte), b.istEigenerEintrag)
                    }
                }
            }
        }

        if (daten.saison.isNotEmpty()) {
            val eigener = daten.saison.any { it.istEigenerEintrag }
            Buchkarte(titel = "Saisonwertung", zahl = monatJahr(Instant.now().toString())) {
                if (konto != null && !eigener) {
                    SehrLeise("Du stehst diesen Monat noch nicht unter den ersten ${daten.saison.size}.")
                }
                daten.saison.forEach { b ->
                    Rangzeile(b.platz, b.anzeigename, null, zahl(b.punkte), b.istEigenerEintrag)
                }
            }
        }

        if (daten.bestenliste.isNotEmpty()) {
            Buchkarte(titel = "Bestenliste", zahl = "aller Zeiten") {
                if (konto != null && daten.eigenerPlatz == null) {
                    SehrLeise("Du stehst noch nicht unter den ersten ${daten.bestenliste.size}.")
                }
                daten.bestenliste.forEach { b ->
                    Rangzeile(b.platz, b.anzeigename, b.rang, zahl(b.erfahrung), b.istEigenerEintrag)
                }
            }
        }

        val statistik = daten.statistik
        if (statistik != null && statistik.spieler.isNotEmpty()) {
            Mannschaftsliste(
                runden = statistik.runden,
                bilanzen = statistik.spieler,
                eigeneKennung = konto?.kennung,
                freunde = freunde,
                beiAnfragen = beiAnfragen,
            )
        }
    }
}

/**
 * Wie weit die laufende Stufe gefüllt ist — aus der Laufbahn selbst gerechnet,
 * wie im Web: zwischen dem Beginn dieser und dem der nächsten Stufe.
 */
private fun passFortschritt(konto: Konto, laufbahn: List<Laufbahnstufe>): Float? {
    val i = laufbahn.indexOfFirst { it.level == konto.level }
    if (i < 0) return null
    val naechste = laufbahn.getOrNull(i + 1) ?: return 1f
    val aktuelle = laufbahn[i]
    val spanne = naechste.ab - aktuelle.ab
    if (spanne <= 0) return 1f
    return ((konto.erfahrung - aktuelle.ab).toFloat() / spanne).coerceIn(0f, 1f)
}

/**
 * Die Laufbahnkarte: wo man steht, wie weit die Stufe gefüllt ist und was die
 * nächste bringt.
 */
@Composable
private fun Laufbahnkarte(konto: Konto, daten: Dienstbuchdaten) {
    val anteil = passFortschritt(konto, daten.laufbahn) ?: 0f
    val aktuell = daten.laufbahn.firstOrNull { it.level == konto.level }
    val naechster = daten.laufbahn.firstOrNull { it.level == konto.level + 1 }
    val naechsteGaben = naechster?.let { gaben(it, daten.freischaltungen) }.orEmpty()

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = Farben.AmberTief)
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Gross),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Stufenring(anteil = anteil, stufe = konto.level)
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.weight(1f),
            ) {
                Etikett("Deine Laufbahn")
                Text(aktuell?.bezeichnung ?: konto.rang, style = Schrift.Titel, color = Farben.Text)
                Text(
                    "${zahl(konto.erfahrung)} Punkte · ${buchStufenziel(konto.bisZumNaechsten, konto.level + 1)}",
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                )
                Fortschritt(anteil = anteil)
            }
        }

        if (naechster != null) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                Etikett("Mit Stufe ${naechster.level}")
                if (naechsteGaben.isEmpty()) {
                    SehrLeise(naechster.bezeichnung)
                } else {
                    naechsteGaben.take(3).forEach { g ->
                        Text("• ${g.text}", style = Schrift.Klein, color = gabenfarbe(g.art))
                    }
                    if (naechsteGaben.size > 3) SehrLeise("+${naechsteGaben.size - 3} weitere")
                }
            }
        }
    }
}

/**
 * Die nächsten Meilensteine: Stufen, die ein Gerät oder eine Funktion
 * freischalten. Die meisten Stufen bringen einen Gutschein oder ein Zierstück —
 * schön, aber kein Ziel, auf das man eine Woche hinfährt.
 */
@Composable
private fun Meilensteine(konto: Konto?, daten: Dienstbuchdaten) {
    if (konto == null) return
    val liste = daten.laufbahn
        .filter { it.level > konto.level }
        .mapNotNull { r ->
            val alle = gaben(r, daten.freischaltungen)
            val gabe = alle.firstOrNull { it.art == "geraet" || it.art == "funktion" } ?: return@mapNotNull null
            Triple(r, gabe, max(0, r.ab - konto.erfahrung))
        }
        .take(4)
    if (liste.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Etikett("Die nächsten Meilensteine")
        liste.forEach { (r, gabe, fehlt) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(ecke = 9.dp)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text(
                    r.level.toString(),
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = gabenfarbe(gabe.art),
                    modifier = Modifier.widthIn(min = 32.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(gabe.text, style = Schrift.Klein, color = Farben.Text)
                    SehrLeise("noch ${zahl(fehlt)} Punkte", mono = true)
                }
            }
        }
    }
}

/**
 * Das Band der Stufen — rollen statt blättern, und an jeder Karte steht, was sie
 * freischaltet. Es beginnt bei der eigenen Stufe, nicht bei Stufe 1: Wer auf 23
 * steht, will sehen, was als Nächstes kommt.
 */
@Composable
private fun Pass(konto: Konto?, daten: Dienstbuchdaten) {
    val band = rememberLazyListState()
    val bereich = rememberCoroutineScope()
    val eigene = daten.laufbahn.indexOfFirst { it.level == konto?.level }
    val fortschritt = konto?.let { passFortschritt(it, daten.laufbahn) }

    LaunchedEffect(eigene) {
        if (eigene >= 0) band.scrollToItem(max(0, eigene - 1))
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Ueberschrift("Alle Stufen", Modifier.weight(1f))
        Knopf("‹", {
            bereich.launch { band.animateScrollBy(-band.layoutInfo.viewportSize.width * 0.8f) }
        }, art = Knopfart.Leise, kompakt = true)
        Knopf("Meine Stufe", {
            if (eigene >= 0) bereich.launch { band.animateScrollToItem(max(0, eigene - 1)) }
        }, art = Knopfart.Leise, kompakt = true)
        Knopf("›", {
            bereich.launch { band.animateScrollBy(band.layoutInfo.viewportSize.width * 0.8f) }
        }, art = Knopfart.Leise, kompakt = true)
    }

    LazyRow(
        state = band,
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(daten.laufbahn, key = { it.level }) { r ->
            Stufenkarte(
                r = r,
                alle = gaben(r, daten.freischaltungen),
                konto = konto,
                fortschritt = fortschritt,
            )
        }
    }
}

/** Eine Stufe im Band — feste Größe, damit das Band ruhig bleibt. */
@Composable
private fun Stufenkarte(r: Laufbahnstufe, alle: List<Passgabe>, konto: Konto?, fortschritt: Float?) {
    val aktuell = konto != null && r.level == konto.level
    val erreicht = konto != null && r.level < konto.level
    val meilenstein = istMeilenstein(alle)
    val (sichtbar, rest) = passgaben(alle)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .width(158.dp)
            .height(200.dp)
            .flaeche(
                ecke = 12.dp,
                farbe = if (aktuell) Farben.FlaecheHoch else Farben.Flaeche,
                randfarbe = when {
                    aktuell -> Farben.Amber
                    meilenstein -> Farben.AmberTief
                    else -> Farben.Rand
                },
            )
            .padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                r.level.toString(),
                style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                color = if (aktuell) Farben.Amber else if (erreicht) Farben.TextLeise else Farben.Text,
                modifier = Modifier.weight(1f),
            )
            when {
                erreicht -> Text("✓", style = Schrift.Klein, color = Farben.GruenHell)
                konto != null && r.level > konto.level -> Text(
                    "🔒",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
            }
        }
        Text(
            r.bezeichnung,
            style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
            color = if (erreicht) Farben.TextLeise else Farben.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text("ab ${r.ab} P.", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            sichtbar.forEach { g ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (g.art) {
                        "gesicht" -> Minimelder(g.gehaeuse ?: Farben.FlaecheAktiv, g.lcd ?: Farben.Rand)
                        "geraet" -> Minimelder(Color.Transparent, Color.Transparent, leer = true)
                        else -> Box(Modifier.size(6.dp).background(gabenfarbe(g.art), CircleShape))
                    }
                    Text(
                        g.text,
                        style = Schrift.Winzig,
                        color = gabenfarbe(g.art),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (rest > 0) Text("+$rest weitere", style = Schrift.Winzig, color = Farben.TextSehrLeise)
        }

        // Der Balken nur an der laufenden Stufe: wie weit sie gefüllt ist.
        if (aktuell && fortschritt != null) Fortschritt(anteil = fortschritt)
    }
}

/** Ein Melder im Kleinformat — Gehäuse und Anzeige, oder nur der Umriss. */
@Composable
internal fun Minimelder(gehaeuse: Color, lcd: Color, leer: Boolean = false, breite: Dp = 12.dp) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .size(width = breite, height = breite * 1.4f)
            .background(gehaeuse, Rundung.Winzig)
            .border(1.dp, if (leer) Farben.RandHell else gehaeuse, Rundung.Winzig)
            .padding(top = breite * 0.2f),
    ) {
        Box(
            Modifier
                .size(width = breite * 0.7f, height = breite * 0.45f)
                .background(if (leer) Color.Transparent else lcd)
                .border(if (leer) 1.dp else 0.dp, if (leer) Farben.RandHell else Color.Transparent),
        )
    }
}

/** Eine Zeile einer Rangliste — Gold, Silber, Bronze für die ersten drei. */
@Composable
private fun Rangzeile(platz: Int, name: String, unter: String?, wert: String, eigen: Boolean) {
    val medaille = when (platz) {
        1 -> Color(0xFFE0B84F)
        2 -> Color(0xFFD8DFE7)
        3 -> Color(0xFFC07A4A)
        else -> null
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (eigen) Farben.HauchAmber else Color.Transparent, Rundung.Klein)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
    ) {
        Text(
            platz.toString(),
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
            color = medaille ?: if (eigen) Farben.Amber else Farben.TextSehrLeise,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = Schrift.Klein,
                color = if (eigen) Farben.Amber else Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unter != null) SehrLeise(unter)
        }
        Text(wert, style = Schrift.MonoKlein, color = if (eigen) Farben.Amber else Farben.TextLeise)
    }
}

/**
 * „Mit wem du gefahren bist" — sieben Zahlen je Person. Am Rechner eine Tabelle;
 * am Handy eine Zeile je Person mit ihren Zahlen darunter, die Spaltennamen an
 * jeder Zahl, weil der Tabellenkopf fehlt.
 */
@Composable
private fun Mannschaftsliste(
    runden: Int,
    bilanzen: List<Schichtbilanz>,
    eigeneKennung: String?,
    freunde: List<Freund>,
    beiAnfragen: suspend (String) -> Result<Unit>,
) {
    val bereich = rememberCoroutineScope()
    var angefragt by remember { mutableStateOf(emptySet<String>()) }
    var meldung by remember { mutableStateOf<String?>(null) }

    fun schonBefreundet(kennung: String): Boolean =
        freunde.firstOrNull { it.kennung == kennung }?.stand
            ?.let { it in setOf("Bestaetigt", "Angefragt", "Blockiert") } == true

    Buchkarte(
        titel = "Mit wem du gefahren bist",
        zahl = mitWort(runden, "Schicht", "Schichten"),
    ) {
        bilanzen.forEach { b ->
            val eigen = b.playerId == eigeneKennung
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (eigen) Farben.HauchAmber else Color.Transparent, Rundung.Klein)
                    .padding(Abstand.Klein),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(28.dp).background(Farben.FlaecheAktiv, CircleShape),
                    ) {
                        Text(kuerzel(b.name), style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    }
                    Text(
                        b.name,
                        style = Schrift.Normal,
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!eigen && eigeneKennung != null && !schonBefreundet(b.playerId)) {
                        val schon = b.playerId in angefragt
                        Knopf(
                            aufschrift = if (schon) "Angefragt" else "+ Freund",
                            beiDruck = {
                                meldung = null
                                bereich.launch {
                                    beiAnfragen(b.playerId)
                                        .onSuccess { angefragt = angefragt + b.playerId }
                                        .onFailure { meldung = it.message ?: "Das hat gerade nicht geklappt." }
                                }
                            },
                            art = Knopfart.Leise,
                            aktiv = !schon,
                            kompakt = true,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                    Zahlzelle("Schichten", b.runden.toString())
                    Zahlzelle("Einsätze", b.einsaetze.toString())
                    Zahlzelle("Lagemeld.", b.lagemeldungen.toString())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                    Zahlzelle("Funksprüche", b.funksprueche.toString())
                    Zahlzelle("Ø Ausrückzeit", dauerText(b.ausrueckzeitSekunden))
                    Zahlzelle("Status 6", if (b.ausserDienstSekunden > 0) dauerText(b.ausserDienstSekunden) else "—")
                }
            }
        }
        meldung?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
    }
}

@Composable
private fun Zahlzelle(was: String, wert: String) {
    Column(modifier = Modifier.widthIn(min = 88.dp)) {
        Text(wert, style = Schrift.MonoKlein, color = Farben.Text)
        Text(was, style = Schrift.Winzig, color = Farben.TextSehrLeise)
    }
}

/** Zwei Buchstaben für das Namensschild. */
private fun kuerzel(name: String): String {
    val teile = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val zeichen = if (teile.size > 1) {
        "${teile.first().first()}${teile.last().first()}"
    } else {
        (teile.firstOrNull() ?: "?").take(2)
    }
    return zeichen.uppercase()
}
