package de.pagerspass.pagerspass.ansichten

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.mobil.Dienstbuchdaten
import de.pagerspass.pagerspass.mobil.Dienstbuchstand
import de.pagerspass.pagerspass.mobil.Schichtzeile
import de.pagerspass.pagerspass.mobil.schichtOrganisation
import de.pagerspass.pagerspass.netz.Buchungsposten
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Garage
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import java.time.LocalDate
import kotlin.math.max

/**
 * Das Dienstbuch — sechs Seiten unter einem Kopf.
 *
 * Übertragen aus `components/dienstbuch/DienstbuchKopf.vue`,
 * `DienstbuchSeite.vue`, `views/dienstbuch/UebersichtView.vue`,
 * `SchichtenView.vue` und `components/dienstbuch/SchichtZeile.vue`.
 *
 * <b>Sechs Wege, nicht sechs Reiter in einer Seite.</b> Im Web hat jede Seite
 * ihre Adresse (`/dienstbuch/schichten` …), und genau so hier: Zurück führt dahin,
 * wo man herkam, und die Tableiste markiert auf jeder den Weg „Buch". Der Kopf mit
 * Ausweis, Balken und Reitern steht auf allen sechs gleich — beim Blättern bleiben
 * Name, Rang und der Stand bis zur nächsten Stufe stehen, statt bei jedem Wechsel
 * neu aufzutauchen.
 */

/** Die sechs Seiten des Dienstbuchs und ihre Wege. */
enum class Buchreiter(val weg: String, val titel: String) {
    Uebersicht("dienstbuch", "Übersicht"),
    Schichten("dienstbuch/schichten", "Schichten"),
    Laufbahn("dienstbuch/laufbahn", "Laufbahn"),
    Garage("dienstbuch/garage", "Garage"),
    Abzeichen("dienstbuch/abzeichen", "Abzeichen"),

    /** Zuletzt: Es ist die Zusammenfassung von allem davor — und ohne Abo ein Angebot. */
    Auswertung("dienstbuch/auswertung", "Auswertung"),
}

/** „noch 1 500 bis Stufe 13" — oder die Ansage, dass es keine höhere gibt. */
internal fun buchStufenziel(bis: Int?, naechsteStufe: Int): String =
    if (bis == null) "höchste Stufe erreicht" else "noch ${zahl(bis)} bis Stufe $naechsteStufe"

/**
 * Der Kopf des Dienstbuchs: Stufe, Bereich, die Wege zu den sechs Seiten.
 *
 * <b>Am Handy sagt er, wo man ist, nicht wer man ist</b> (`mobil.css`): Die
 * Marke mit der Stufe, „Dienstbuch" als Überschrift und der Name leise daneben.
 * Rang, Punkte und Balken fallen weg — jede dieser Angaben steht auf der Seite
 * darunter, und ein Kopf, der auf sechs Seiten dieselbe Auskunft wiederholt, kostet
 * ein Drittel der Bildschirmhöhe.
 *
 * <b>Die Reiter stehen in zwei Reihen zu dreien.</b> Sechs in einer Reihe sind am
 * Handy je sechzig Punkte breit, und „Übersicht", „Schichten" und „Auswertung"
 * stünden übereinandergeschoben. Eine rollende Leiste wäre keine Lösung: Ein Weg,
 * den man erst durch Wischen findet, ist keiner.
 */
@Composable
fun Dienstbuchkopf(
    konto: Konto?,
    hier: Buchreiter,
    beiReiter: (Buchreiter) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .border(1.dp, if (konto != null) Farben.Amber else Farben.RandHell, CircleShape),
            ) {
                Text(
                    text = konto?.level?.toString() ?: "–",
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (konto != null) Farben.Amber else Farben.TextSehrLeise,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.weight(1f),
            ) {
                Text("Dienstbuch", style = Schrift.Gross.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                Text(
                    text = konto?.anzeigename ?: "Deine Schichten",
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Buchreiter.entries.chunked(3).forEach { reihe ->
            Reiterreihe {
                reihe.forEach { r ->
                    Reiter(r.titel, offen = r == hier, beiDruck = { if (r != hier) beiReiter(r) })
                }
            }
        }
    }
}

/**
 * Der gemeinsame Rahmen der sechs Seiten: Seite, Kopf, dann der Inhalt — oder,
 * solange der Bestand fehlt, der Hinweis darauf.
 */
@Composable
fun Dienstbuchrahmen(
    unterrand: Dp,
    konto: Konto?,
    hier: Buchreiter,
    beiReiter: (Buchreiter) -> Unit,
    laedt: Boolean = false,
    fehler: String? = null,
    beiErneut: (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Seite(unterrand = unterrand) {
        Dienstbuchkopf(konto, hier, beiReiter)
        when {
            laedt -> Ladezeile("Das Dienstbuch wird geholt …")
            fehler != null -> Fehlerzeile(fehler, beiErneut)
            else -> inhalt()
        }
    }
}

/**
 * Eine Karte mit Kopfzeile — Titel, eine Zahl daneben, rechts ein Weg weiter.
 * Dieselbe Form für alle Abschnitte des Buchs (`db-karte` im Web).
 */
@Composable
fun Buchkarte(
    titel: String,
    modifier: Modifier = Modifier,
    zahl: String? = null,
    /** Der Weg weiter — „alle 12" rechts im Kopf; ohne Aufschrift steht keiner. */
    weiterText: String? = null,
    beiWeiter: () -> Unit = {},
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier
            .fillMaxWidth()
            .flaeche()
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = titel,
                style = Schrift.Gross,
                color = Farben.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (zahl != null) {
                Text(zahl, style = Schrift.MonoKlein, color = Farben.TextSehrLeise, maxLines = 1)
            }
            Box(Modifier.weight(1f))
            if (weiterText != null) Textweg(weiterText, beiWeiter)
        }
        inhalt()
    }
}

/** Eine Kachel der Tafel: Etikett, große Zahl, die Zeile, die sie einordnet. */
@Composable
fun Kennzahlkachel(
    etikett: String,
    wert: String,
    unter: String,
    modifier: Modifier = Modifier,
    einheit: String? = null,
    farbe: Color = Farben.Amber,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = modifier
            .flaeche()
            .padding(Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(farbe, CircleShape))
            Etikett(etikett)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = wert,
                style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                color = Farben.Text,
                maxLines = 1,
            )
            if (einheit != null) {
                Text(
                    text = " $einheit",
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        Text(unter, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 2)
    }
}

/** Zwei Kacheln nebeneinander — eine Zeile der Tafel am Handy. */
@Composable
fun Kachelpaar(inhalt: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth(),
        content = inhalt,
    )
}

/** Ein Wink auf eine andere Seite — Zeichen, zwei Zeilen, Pfeil (`db-wink`). */
@Composable
fun Buchwink(
    titel: String,
    zeile: String,
    beiDruck: () -> Unit,
    wartet: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = if (wartet) Farben.AmberTief else Farben.Rand)
            .then(if (wartet) Modifier.flaechenmarke(wartet = true) else Modifier)
            .clickable(onClick = beiDruck, role = Role.Button, indication = null, interactionSource = null)
            .padding(Abstand.Gross),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titel, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Text(zeile, style = Schrift.Klein, color = Farben.TextLeise)
        }
        Text("›", style = Schrift.Titel, color = Farben.TextSehrLeise)
    }
}

// --------------------------------------------------------------- Übersicht

/**
 * Die Übersicht: was die Schichten zusammen ergeben haben.
 *
 * Sie beantwortet drei Fragen in dieser Reihenfolge — wo stehe ich, wie viel habe
 * ich gefahren, wie lief es zuletzt. Oben der Stand der Laufbahn, dann die Tafel,
 * dann links, was war (Schichten, Kurve), und rechts, was man gesammelt hat —
 * am Handy in genau dieser Reihenfolge untereinander.
 */
@Composable
fun DienstbuchUebersicht(
    unterrand: Dp,
    konto: Konto?,
    stand: Dienstbuchstand,
    garage: Garage?,
    fahrzeuge: List<Fahrzeugvorlage>,
    beiReiter: (Buchreiter) -> Unit,
    beiLaden: () -> Unit,
    beiSchicht: (String) -> Unit,
    beiErsteSchicht: () -> Unit,
) {
    LaunchedEffect(Unit) { beiLaden() }
    val buch = stand.buch

    Dienstbuchrahmen(
        unterrand = unterrand,
        konto = konto,
        hier = Buchreiter.Uebersicht,
        beiReiter = beiReiter,
        laedt = buch.ersteLadung,
        fehler = if (buch.inhalt == null) buch.fehler else null,
        beiErneut = beiLaden,
    ) {
        val daten = buch.inhalt ?: Dienstbuchdaten()
        val schichten = daten.schichten

        if (konto != null) Standkarte(konto) { beiReiter(Buchreiter.Laufbahn) }

        Tafel(daten)

        Buchkarte(
            titel = "Zuletzt gefahren",
            weiterText = if (schichten.isNotEmpty()) "alle ${schichten.size}" else null,
            beiWeiter = { beiReiter(Buchreiter.Schichten) },
        ) {
            if (schichten.isEmpty()) {
                Leerhinweis("Noch nichts im Buch. Nach deinem ersten Dienstende steht die erste Schicht hier.") {
                    Knopf("Erste Schicht fahren", beiErsteSchicht, art = Knopfart.Haupt)
                }
            } else {
                schichten.take(5).forEach { s ->
                    SchichtZeile(
                        schicht = s,
                        fahrzeuge = fahrzeuge,
                        kompakt = true,
                        beiOeffnen = { beiSchicht(s.code) },
                        beiBuchungen = { beiSchicht(s.code) },
                    )
                }
            }
        }

        val verlauf = daten.statistik?.verlauf.orEmpty()
        val zeigbar = verlauf.count { it.hilfsfristSekunden != null && it.beendetUm != null } +
            stand.wachenkurven.sumOf { k -> k.verlauf.count { it.hilfsfristSekunden != null } } > 1
        if (zeigbar) {
            Buchkarte(titel = "Ø Hilfsfrist · letzte Schichten") {
                HilfsfristVerlauf(
                    verlauf = verlauf,
                    mitglieder = stand.wachenkurven,
                    beiOeffnen = beiSchicht,
                )
            }
        }

        Rekorde(daten, konto?.kennung)

        if (daten.abzeichen.isNotEmpty()) Abzeichenkarte(daten) { beiReiter(Buchreiter.Abzeichen) }

        val fortschritt = garage?.proOrganisation.orEmpty()
        if (fortschritt.isNotEmpty()) {
            val spitze = max(1, fortschritt.maxOf { it.erfahrung })
            Buchkarte(titel = "Erfahrung je Organisation") {
                fortschritt.forEach { p ->
                    Balkenzeile(
                        name = orgName(p.organisation),
                        wert = "${zahl(p.erfahrung)} P",
                        anteil = p.erfahrung.toFloat() / spitze,
                        farbe = orgTon(p.organisation),
                        punkt = true,
                    )
                }
            }
        }

        // Die Zeile führt zum Bestand; eingelöst und gekauft wird im Shop.
        val gutscheine = garage?.offeneWahlen ?: 0
        Buchwink(
            titel = "Garage",
            zeile = if (garage != null) {
                "${garage.fahrzeuge.size} Fahrzeuge" +
                    if (gutscheine > 0) " · $gutscheine Gutschein${if (gutscheine == 1) "" else "e"} offen" else ""
            } else {
                "Dein Fuhrpark"
            },
            beiDruck = { beiReiter(Buchreiter.Garage) },
            wartet = gutscheine > 0,
        )
    }
}

/**
 * Der Stand der Laufbahn — die eine Stelle, an der „wie weit bin ich" groß
 * beantwortet wird. Ein Druck führt in die Laufbahn.
 */
@Composable
private fun Standkarte(konto: Konto, beiDruck: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Gross),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = Farben.AmberTief)
            .clickable(onClick = beiDruck, role = Role.Button, indication = null, interactionSource = null)
            .padding(Abstand.Gross),
    ) {
        Stufenring(anteil = konto.stufenanteil, stufe = konto.level)
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.weight(1f),
        ) {
            Etikett("Dein Stand")
            Text(konto.rang, style = Schrift.Gross, color = Farben.Text, maxLines = 2)
            Fortschritt(anteil = konto.stufenanteil)
            Text(
                text = "${zahl(konto.erfahrung)} Punkte · ${buchStufenziel(konto.bisZumNaechsten, konto.level + 1)}",
                style = Schrift.MonoKlein,
                color = Farben.TextLeise,
            )
        }
        Text("Laufbahn ›", style = Schrift.Klein, color = Farben.AmberHell)
    }
}

/** Die Tafel: vier Zahlen, jede mit der Zeile, die sie einordnet. */
@Composable
private fun Tafel(daten: Dienstbuchdaten) {
    val schichten = daten.schichten

    // Gewichtet nach Einsätzen: Eine Schicht mit zwölf Einsätzen sagt mehr über
    // einen als eine mit einem.
    var summe = 0.0
    var gewicht = 0
    schichten.forEach { s ->
        val frist = s.hilfsfristSekunden
        if (frist != null && s.einsaetze > 0) {
            summe += frist * s.einsaetze
            gewicht += s.einsaetze
        }
    }
    val mittel = if (gewicht > 0) summe / gewicht else null
    val besteSchicht = schichten.mapNotNull { it.punkte }.maxOrNull()
    val jeSchicht = if (schichten.isNotEmpty()) daten.einsaetzeGesamt.toDouble() / schichten.size else null

    Kachelpaar {
        Kennzahlkachel(
            etikett = "Schichten",
            wert = zahl(schichten.size),
            unter = schichten.firstOrNull()?.beendetUm?.let { "zuletzt ${buchTagMonat(it)}" } ?: "noch keine",
            modifier = Modifier.weight(1f),
        )
        Kennzahlkachel(
            etikett = "Einsätze",
            wert = zahl(daten.einsaetzeGesamt),
            unter = jeSchicht?.let { "${eineStelle(it)} je Schicht" } ?: "—",
            modifier = Modifier.weight(1f),
            farbe = Farben.Blau,
        )
    }
    Kachelpaar {
        Kennzahlkachel(
            etikett = "Ø Hilfsfrist",
            wert = minutenSekunden(mittel),
            einheit = if (mittel != null) "min" else null,
            unter = "über alle Schichten",
            modifier = Modifier.weight(1f),
            farbe = Farben.Gruen,
        )
        Kennzahlkachel(
            etikett = "Punkte",
            wert = zahlMitMinus(daten.punkteGesamt),
            unter = besteSchicht?.let { "beste Schicht +${zahl(it)}" } ?: "—",
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Die besten je erreichten Werte, nicht die Durchschnitte. `statistik.spieler`
 * listet jeden, der zuletzt mitgefahren ist; hier zählt nur die eigene Zeile.
 */
@Composable
private fun Rekorde(daten: Dienstbuchdaten, kennung: String?) {
    val r = daten.statistik?.spieler?.firstOrNull { it.playerId == kennung }?.rekorde ?: return
    val liste = buildList<Triple<String, String?, String>> {
        r.schnellsteAusrueckzeitSekunden?.let { add(Triple("⚡ Schnellste Ausrückzeit", null, dauerText(it))) }
        r.kuerzesteHilfsfristSekunden?.let {
            add(Triple("🚨 Kürzeste Hilfsfrist", r.kuerzesteHilfsfristStichwort, dauerText(it)))
        }
        if (r.meisteLagemeldungenSchicht > 0) {
            add(Triple("📻 Meiste Lagemeldungen", "in einer Schicht", r.meisteLagemeldungenSchicht.toString()))
        }
    }
    if (liste.isEmpty()) return

    Buchkarte(titel = "Persönliche Rekorde") {
        liste.forEach { (name, zusatz, wert) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = Schrift.Klein, color = Farben.Text)
                    if (zusatz != null) SehrLeise(zusatz)
                }
                Text(wert, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
            }
        }
    }
}

/**
 * Die Abzeichen als Karte: Anteil und die sechs zuletzt erreichten. Bei rund
 * tausend Abzeichen wäre die volle Liste hier eine Wand statt eines Überblicks.
 */
@Composable
private fun Abzeichenkarte(daten: Dienstbuchdaten, beiAlle: () -> Unit) {
    val erreicht = daten.abzeichen.count { it.erreicht }
    val vorschau = daten.abzeichen
        .filter { it.erreicht && it.erreichtAm != null }
        .sortedByDescending { zeitVon(it.erreichtAm)?.toEpochMilli() ?: 0L }
        .take(6)

    Buchkarte(
        titel = "Abzeichen",
        zahl = "${zahl(erreicht)} / ${zahl(daten.abzeichen.size)}",
        weiterText = "alle",
        beiWeiter = beiAlle,
    ) {
        Fortschritt(anteil = erreicht.toFloat() / max(1, daten.abzeichen.size))
        if (vorschau.isEmpty()) {
            SehrLeise("Noch keins erreicht — das erste kommt meist mit der ersten Schicht.")
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                vorschau.forEach { a ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .border(1.dp, Farben.AmberTief, Rundung.Rund)
                            .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
                    ) {
                        Text("★", style = Schrift.Klein, color = Farben.Amber)
                        Text(a.titel, style = Schrift.Klein, color = Farben.Text, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------- Schichtzeile

/** Wie eine Rolle heißt, wenn man sie liest. */
private fun rollenname(rolle: String?): String? = when (rolle) {
    null -> null
    "Unbestimmt" -> "ohne Rolle"
    "Leitstelle" -> "Leitstelle"
    "Fahrzeugbesatzung" -> "Besatzung"
    else -> rolle
}

/**
 * Eine Zeile je gefahrener Schicht — auf der Übersicht (die letzten) und in der
 * vollen Liste dieselbe.
 *
 * <b>Zwei Griffe an einer Zeile:</b> Die Zeile selbst öffnet die Nachbesprechung,
 * die Punkte rechts klappen auf, wofür es sie gab. Links ein Kalenderblatt, weil
 * man eine Schicht am Tag wiederfindet; die Kante trägt die Farbe der Organisation
 * (Violett für die Leitstelle).
 */
@Composable
fun SchichtZeile(
    schicht: Schichtzeile,
    fahrzeuge: List<Fahrzeugvorlage>,
    beiOeffnen: () -> Unit,
    beiBuchungen: () -> Unit,
    kompakt: Boolean = false,
    offen: Boolean = false,
    posten: List<Buchungsposten> = emptyList(),
    gesperrt: Boolean = false,
) {
    val organisation = schichtOrganisation(schicht, fahrzeuge)
    val kante = orgTon(organisation)
    val markentext = when (organisation) {
        null -> rollenname(schicht.rolle)
        "Leitstelle" -> "Leitstelle"
        else -> orgName(organisation)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Rundung.Klein)
            .flaeche(ecke = 9.dp, randfarbe = if (offen) Farben.RandHell else Farben.Rand),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                // Die Kante der Organisation, so hoch wie die Zeile — gezeichnet
                // statt als Kasten, weil die Zeile ihre Höhe erst beim Messen kennt.
                .drawBehind {
                    drawRect(kante, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        enabled = !gesperrt,
                        onClick = beiOeffnen,
                        role = Role.Button,
                        indication = null,
                        interactionSource = null,
                    )
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 34.dp),
                ) {
                    Text(
                        text = tagImMonat(schicht.beendetUm),
                        style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = Farben.Text,
                    )
                    Text(monatKurz(schicht.beendetUm), style = Schrift.Winzig, color = Farben.TextSehrLeise)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = schicht.ort,
                        style = Schrift.Normal,
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    ) {
                        if (markentext != null) {
                            Text(
                                text = markentext,
                                style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                                color = if (organisation != null) kante else Farben.TextLeise,
                            )
                        }
                        when {
                            schicht.funkrufname != null -> Text(
                                schicht.funkrufname,
                                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                                color = Farben.TextLeise,
                            )
                            schicht.rolle == null -> Text(
                                "Raum ${schicht.code}",
                                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                                color = Farben.TextLeise,
                            )
                        }
                        Text(
                            text = if (kompakt) {
                                buchUhrzeit(schicht.beendetUm)
                            } else {
                                "${wochentagKurz(schicht.beendetUm)}, ${buchUhrzeit(schicht.beendetUm)}"
                            },
                            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                            color = Farben.TextSehrLeise,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        schicht.einsaetze.toString(),
                        style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                    )
                    Text(
                        if (schicht.einsaetze == 1) "Einsatz" else "Einsätze",
                        style = Schrift.Winzig,
                        color = Farben.TextSehrLeise,
                    )
                    if (!kompakt) {
                        Text(
                            minutenSekunden(schicht.hilfsfristSekunden),
                            style = Schrift.MonoKlein,
                            color = Farben.TextLeise,
                        )
                        Text("Ø Hilfsfrist", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                    }
                }
            }

            // Die Punkte sind ein eigener Griff — in einer Spalte fester Breite,
            // damit „+60" und „+101" untereinander fluchten.
            val punkte = schicht.punkte
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(64.dp)
                    .defaultMinSize(minHeight = 64.dp)
                    .background(if (offen) Farben.FlaecheHoch else Color.Transparent)
                    .then(
                        if (punkte != null) {
                            Modifier.clickable(
                                enabled = !gesperrt,
                                onClick = beiBuchungen,
                                role = Role.Button,
                                indication = null,
                                interactionSource = null,
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                if (punkte != null) {
                    Text(
                        mitVorzeichen(punkte),
                        style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                        color = if (punkte < 0) Farben.SignalHell else Farben.AmberHell,
                        textAlign = TextAlign.Center,
                    )
                    Text("Punkte", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                } else {
                    Text("—", style = Schrift.MonoNormal, color = Farben.TextSehrLeise)
                }
            }
        }

        if (offen) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.BgTief.copy(alpha = 0.4f))
                    .padding(horizontal = Abstand.Gross, vertical = Abstand.Klein),
            ) {
                if (posten.isEmpty()) {
                    SehrLeise("Zu dieser Schicht liegen keine Einzelbuchungen vor.")
                }
                posten.forEach { p ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(p.text, style = Schrift.Klein, color = Farben.TextLeise, modifier = Modifier.weight(1f))
                        Text(
                            mitVorzeichen(p.punkte),
                            style = Schrift.MonoKlein,
                            color = if (p.punkte < 0) Farben.SignalHell else Farben.GruenHell,
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------- Schichten

/**
 * Alle gefahrenen Schichten.
 *
 * Grundlage ist die Archivliste, nicht die Punktbuchung — wer eine Runde vor
 * seinem Konto gefahren hat, findet sie hier trotzdem, nur eben ohne Punkte.
 *
 * <b>Suche, Kreis und Organisation</b>: Die Suche findet, was man schon weiß; ein
 * Filter beantwortet die Frage, die man nicht in ein Feld tippen kann — „was bin
 * ich eigentlich im Heidekreis gefahren". Die Liste ist nach Monaten gebündelt:
 * Der Monat ist die Einheit, in der man sich an Dienste erinnert.
 */
@Composable
fun DienstbuchSchichten(
    unterrand: Dp,
    konto: Konto?,
    stand: Dienstbuchstand,
    fahrzeuge: List<Fahrzeugvorlage>,
    beiReiter: (Buchreiter) -> Unit,
    beiLaden: () -> Unit,
    beiSchicht: (String) -> Unit,
    beiBuchungen: (String) -> Unit,
    beiErsteSchicht: () -> Unit,
) {
    LaunchedEffect(Unit) { beiLaden() }
    val buch = stand.buch
    val zusammenhang = LocalContext.current

    var suche by rememberSaveable { mutableStateOf("") }
    var kreis by rememberSaveable { mutableStateOf("") }
    var organisation by rememberSaveable { mutableStateOf("") }
    var aufgeklappt by rememberSaveable { mutableStateOf<String?>(null) }
    var kreiswahl by remember { mutableStateOf(false) }
    var orgwahl by remember { mutableStateOf(false) }
    var auszugsfehler by remember { mutableStateOf<String?>(null) }

    val schichten = buch.inhalt?.schichten.orEmpty()

    fun orgVon(s: Schichtzeile): String? = schichtOrganisation(s, fahrzeuge)?.let { orgName(it) }

    val kreise = schichten.mapNotNull { it.landkreis }.distinct().sorted()
    val organisationen = schichten.mapNotNull { orgVon(it) }.distinct().sorted()

    val begriff = suche.trim().lowercase()
    val gefiltert = schichten.filter { s ->
        (kreis.isEmpty() || s.landkreis == kreis) &&
            (organisation.isEmpty() || orgVon(s) == organisation) &&
            (
                begriff.isEmpty() ||
                    s.ort.lowercase().contains(begriff) ||
                    s.code.lowercase().contains(begriff) ||
                    (s.funkrufname?.lowercase()?.contains(begriff) ?: false)
                )
    }
    val filtertAktiv = begriff.isNotEmpty() || kreis.isNotEmpty() || organisation.isNotEmpty()

    // Ausgegeben wird, was gerade gefiltert dasteht — nicht immer alles.
    val speichern = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { ziel ->
        if (ziel == null) return@rememberLauncherForActivityResult
        auszugsfehler = runCatching {
            zusammenhang.contentResolver.openOutputStream(ziel)?.use { strom ->
                strom.write(auszugAlsCsv(gefiltert, ::orgVon).toByteArray(Charsets.UTF_8))
            } ?: error("Die Datei ließ sich nicht schreiben.")
        }.exceptionOrNull()?.message
    }

    Dienstbuchrahmen(
        unterrand = unterrand,
        konto = konto,
        hier = Buchreiter.Schichten,
        beiReiter = beiReiter,
        laedt = buch.ersteLadung,
        fehler = if (buch.inhalt == null) buch.fehler else null,
        beiErneut = beiLaden,
    ) {
        if (schichten.isEmpty()) {
            Leerhinweis("Noch nichts im Buch. Nach deinem ersten Dienstende steht die erste Schicht hier.") {
                Knopf("Erste Schicht fahren", beiErsteSchicht, art = Knopfart.Haupt)
            }
            return@Dienstbuchrahmen
        }

        Feld(
            wert = suche,
            beiAenderung = { suche = it },
            platzhalter = "Ort, Raumcode oder Funkrufname …",
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Wahlfeld(
                etikett = "Kreis",
                wert = kreis.ifEmpty { "Alle Kreise" },
                beiDruck = { kreiswahl = true },
                modifier = Modifier.weight(1f),
            )
            Wahlfeld(
                etikett = "Organisation",
                wert = organisation.ifEmpty { "Alle Organisationen" },
                beiDruck = { orgwahl = true },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Knopf(
                aufschrift = "⤓ Auszug als Datei",
                beiDruck = {
                    auszugsfehler = null
                    speichern.launch("pagerspass-schichten-${LocalDate.now()}.csv")
                },
                aktiv = gefiltert.isNotEmpty(),
                kompakt = true,
            )
            // Am Handy übers Teilen-Blatt — dorthin, wo die Datei weiter soll.
            Knopf(
                aufschrift = "Teilen",
                beiDruck = {
                    val senden = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "PagerSpass-Schichten")
                        putExtra(Intent.EXTRA_TEXT, auszugAlsCsv(gefiltert, ::orgVon))
                    }
                    runCatching {
                        zusammenhang.startActivity(Intent.createChooser(senden, "PagerSpass-Schichten"))
                    }
                },
                art = Knopfart.Leise,
                aktiv = gefiltert.isNotEmpty(),
                kompakt = true,
            )
        }
        auszugsfehler?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }

        // Was die aktuelle Auswahl zusammen ergibt.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val punkte = gefiltert.sumOf { it.punkte ?: 0 }
            Text(
                text = buildString {
                    append(mitWort(gefiltert.size, "Schicht", "Schichten"))
                    append(" · ${zahl(gefiltert.sumOf { it.einsaetze })} Einsätze")
                    append(" · ${zahlMitMinus(punkte)} Punkte")
                    if (filtertAktiv) append(" · von ${zahl(schichten.size)}")
                },
                style = Schrift.MonoKlein,
                color = Farben.TextLeise,
                modifier = Modifier.weight(1f),
            )
            if (filtertAktiv) {
                Knopf(
                    aufschrift = "Filter zurücksetzen",
                    beiDruck = {
                        suche = ""
                        kreis = ""
                        organisation = ""
                    },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        }

        if (gefiltert.isEmpty()) {
            Leerhinweis(
                if (suche.isNotBlank()) "Nichts gefunden zu „$suche“." else "In dieser Auswahl steht keine Schicht.",
            )
        }

        // Nach Monaten gebündelt — die Reihenfolge der Liste bleibt, jüngste zuerst.
        val monate = mutableListOf<Pair<String, MutableList<Schichtzeile>>>()
        gefiltert.forEach { s ->
            val z = zeitVon(s.beendetUm)
            val schluessel = z?.let { monatJahr(s.beendetUm) } ?: "Ohne Datum"
            val letzte = monate.lastOrNull()
            if (letzte == null || letzte.first != schluessel) {
                monate.add(schluessel to mutableListOf(s))
            } else {
                letzte.second.add(s)
            }
        }

        monate.forEach { (titel, liste) ->
            val punkte = liste.sumOf { it.punkte ?: 0 }
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = Abstand.Klein),
                ) {
                    Text(titel, style = Schrift.Gross, color = Farben.Text, modifier = Modifier.weight(1f))
                    Text(
                        text = "${mitWort(liste.size, "Schicht", "Schichten")} · ${mitVorzeichen(punkte)} P",
                        style = Schrift.MonoKlein,
                        color = Farben.TextSehrLeise,
                    )
                }
                liste.forEach { s ->
                    SchichtZeile(
                        schicht = s,
                        fahrzeuge = fahrzeuge,
                        offen = aufgeklappt == s.code,
                        posten = stand.posten[s.code].orEmpty(),
                        beiOeffnen = { beiSchicht(s.code) },
                        beiBuchungen = {
                            val war = aufgeklappt == s.code
                            aufgeklappt = if (war) null else s.code
                            if (!war) beiBuchungen(s.code)
                        },
                    )
                }
            }
        }
    }

    if (kreiswahl) {
        Wahlblende(
            titel = "Kreis",
            gruppen = listOf(null to (listOf("") + kreise)),
            aufschrift = { it.ifEmpty { "Alle Kreise" } },
            beiWahl = {
                kreis = it
                kreiswahl = false
            },
            beiSchliessen = { kreiswahl = false },
            gewaehlt = kreis,
            suchbar = kreise.size > 8,
        )
    }

    if (orgwahl) {
        Wahlblende(
            titel = "Organisation",
            gruppen = listOf(null to (listOf("") + organisationen)),
            aufschrift = { it.ifEmpty { "Alle Organisationen" } },
            beiWahl = {
                organisation = it
                orgwahl = false
            },
            beiSchliessen = { orgwahl = false },
            gewaehlt = organisation,
        )
    }
}

/**
 * Der Auszug als CSV — Semikolon und BOM, weil die Datei fast immer in einer
 * Tabellenkalkulation landet: Excel liest Komma-CSV in deutscher Umgebung als
 * eine einzige Spalte, und ohne BOM werden aus Umlauten Fragezeichen.
 */
private fun auszugAlsCsv(schichten: List<Schichtzeile>, organisation: (Schichtzeile) -> String?): String {
    val kopf = listOf(
        "Beendet", "Raumcode", "Ort", "Landkreis", "Rolle", "Organisation",
        "Funkrufname", "Einsaetze", "Hilfsfrist (s)", "Punkte",
    )

    fun feld(wert: Any?): String =
        if (wert == null) "" else "\"" + wert.toString().replace("\"", "\"\"") + "\""

    val zeilen = schichten.map { s ->
        listOf(
            s.beendetUm,
            s.code,
            s.ort,
            s.landkreis,
            s.rolle,
            organisation(s),
            s.funkrufname,
            s.einsaetze,
            s.hilfsfristSekunden?.let { Math.round(it) },
            s.punkte,
        ).joinToString(";") { feld(it) }
    }

    return "﻿" + (listOf(kopf.joinToString(";")) + zeilen).joinToString("\r\n")
}

