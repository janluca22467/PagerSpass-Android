package de.pagerspass.pagerspass.ansichten

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Garagendaten
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.fahrzeug.FahrzeugSymbol
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Garage: was einem gehört. Nur das — übertragen aus
 * `views/dienstbuch/GarageView.vue`.
 *
 * <b>Der Hof ist nach Organisationen aufgeteilt wie eine Wache nach Hallen:</b>
 * Man geht in das Tor, das man sucht, statt an allem anderen vorbeizurollen. Über
 * den Toren steht die Suche, die quer durch alle geht.
 *
 * <b>Gekauft wird hier nicht mehr.</b> Das Autohaus wohnt im Shop — beschafft wird
 * dort, mit dem Gutschein aus dem Aufstieg oder gegen Credits. Ein Wink oben trägt
 * den Weg, mehr nicht: Zwei Kaufstellen wären zwei Wegweiser zum selben Ort.
 */
@Composable
fun DienstbuchGarage(
    unterrand: Dp,
    konto: Konto?,
    garage: de.pagerspass.pagerspass.mobil.Bereich<Garagendaten>,
    katalog: List<Fahrzeugvorlage>,
    beiReiter: (Buchreiter) -> Unit,
    beiLaden: () -> Unit,
    beiShop: () -> Unit,
) {
    LaunchedEffect(Unit) { beiLaden() }
    val zusammenhang = LocalContext.current

    var tor by rememberSaveable { mutableStateOf("alle") }
    var suche by rememberSaveable { mutableStateOf("") }
    // Kacheln oder Zeilen — die Wahl bleibt über Sitzungen hinweg stehen.
    var ansicht by remember { mutableStateOf(ansichtLesen(zusammenhang)) }

    val stand = garage.inhalt?.stand
    val laedt = garage.ersteLadung || (katalog.isEmpty() && garage.laedt)

    Dienstbuchrahmen(
        unterrand = unterrand,
        konto = konto,
        hier = Buchreiter.Garage,
        beiReiter = beiReiter,
        laedt = laedt,
        fehler = if (stand == null) garage.fehler else null,
        beiErneut = beiLaden,
    ) {
        val bestand = katalog.filter { stand?.fahrzeuge?.contains(it.id) == true }
        val fortschritt = stand?.proOrganisation.orEmpty()
        val gutscheine = stand?.offeneWahlen ?: 0

        Row(verticalAlignment = Alignment.CenterVertically) {
            Ueberschrift("Fuhrpark", Modifier.weight(1f))
            SehrLeise(mitWort(bestand.size, "Fahrzeug", "Fahrzeuge"))
        }

        Buchwink(
            titel = if (gutscheine > 0) {
                "$gutscheine Fahrzeuggutschein${if (gutscheine == 1) "" else "e"} offen"
            } else {
                "Neue Fahrzeuge gibt es im Shop"
            },
            zeile = if (gutscheine > 0) {
                "Jeder Aufstieg bringt einen — einlösen im Shop."
            } else {
                "Einlösen mit Fahrzeuggutschein aus dem Aufstieg oder kaufen mit Credits."
            },
            beiDruck = beiShop,
            wartet = gutscheine > 0,
        )

        // Die Hallentore: ein Tor je Organisation, mit Bestand und verdienten Punkten.
        val tore = BUCH_ORGANISATIONEN.map { org ->
            Triple(
                org,
                bestand.count { it.organisation == org },
                fortschritt.firstOrNull { it.organisation == org }?.erfahrung ?: 0,
            )
        }.filter { (_, anzahl, erfahrung) -> anzahl > 0 || erfahrung > 0 }

        if (tore.isNotEmpty()) {
            Pillenreihe {
                Pille("Alle", an = tor == "alle", beiDruck = { tor = "alle" }, zahl = bestand.size)
                tore.forEach { (org, anzahl, erfahrung) ->
                    Pille(
                        aufschrift = "${orgName(org)} · ${zahl(erfahrung)} P",
                        an = tor == org,
                        beiDruck = { tor = org },
                        farbe = orgTon(org),
                        zahl = anzahl,
                    )
                }
            }
        }

        Feld(
            wert = suche,
            beiAenderung = { suche = it },
            platzhalter = "Fahrzeug, Fähigkeit oder Träger suchen …",
        )
        Segment(
            seiten = listOf("kacheln", "liste"),
            gewaehlt = ansicht,
            beiWahl = {
                ansicht = it
                ansichtMerken(zusammenhang, it)
            },
            aufschrift = { if (it == "kacheln") "Kacheln" else "Liste" },
            modifier = Modifier.fillMaxWidth(),
        )

        val sucht = suche.isNotBlank()
        if (sucht) SehrLeise("Die Suche geht durch alle Hallen.")

        val sichtbar = bestand
            .filter { sucht || tor == "alle" || it.organisation == tor }
            .filter { vorlagePasst(it, suche) }
        val gruppen = garageGruppieren(sichtbar)

        if (gruppen.isEmpty()) {
            SehrLeise(
                if (sucht) "Nichts gefunden zu „$suche“." else "In dieser Halle steht noch nichts.",
                mono = true,
            )
        }

        gruppen.forEach { gruppe ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Abstand.Klein)) {
                Text(
                    gruppe.schluessel,
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    modifier = Modifier.weight(1f),
                )
                SehrLeise(gruppe.traeger.sumOf { it.fahrzeuge.size }.toString(), mono = true)
            }
            gruppe.traeger.forEach { cluster ->
                if (gruppe.traeger.size > 1) Traegertitel(cluster.hiOrg, cluster.name)
                if (ansicht == "kacheln") {
                    cluster.fahrzeuge.chunked(2).forEach { paar ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            paar.forEach { FahrzeugKachel(it, Modifier.weight(1f)) }
                            if (paar.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                } else {
                    cluster.fahrzeuge.forEach { FahrzeugReihe(it) }
                }
            }
        }
    }
}

/** Der Kopf einer Trägergruppe — Punkt in der Farbe des Trägers, Name daneben. */
@Composable
internal fun Traegertitel(hiOrg: String, name: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(traegerTon(hiOrg), CircleShape))
        Text(name, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextLeise)
    }
}

/**
 * Eine Fahrzeugkarte (`FahrzeugKarte.vue` mit `symbol`): Farbstreifen der Organisation,
 * links die Silhouette von der Lagekarte — dasselbe Fahrzeug, dasselbe Bild —, daneben
 * Typ, Träger mit eigener Farbe, Beschreibung, Besatzung und die ersten drei Fähigkeiten.
 */
@Composable
internal fun FahrzeugKachel(f: Fahrzeugvorlage, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .clip(Rundung.Klein)
            .flaeche(ecke = 9.dp)
            .drawBehind { drawRect(orgTon(f.organisation), size = Size(3.dp.toPx(), size.height)) }
            .padding(start = Abstand.Normal, end = Abstand.Klein, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        FahrzeugSymbol(f.typ, f.organisation, Modifier.padding(top = Abstand.Haar), groesse = 38.dp)
        FahrzeugKachelDaten(f, Modifier.weight(1f))
    }
}

/** Die Angaben der Fahrzeugkarte — rechts neben der Silhouette. */
@Composable
private fun FahrzeugKachelDaten(f: Fahrzeugvorlage, modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = modifier,
    ) {
        Text(
            f.typ,
            style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
            color = Farben.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (f.hiOrg.isNotBlank() && f.hiOrg != "Keine") Traegertitel(f.hiOrg, traegerName(f.hiOrg))
        Text(
            f.beschreibung,
            style = Schrift.Winzig,
            color = Farben.TextLeise,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        ) {
            if (f.besatzung.isNotBlank()) Minimarke(f.besatzung, mono = true)
            f.faehigkeiten.take(3).forEach { Minimarke(it) }
        }
    }
}

/** Dasselbe Fahrzeug als Zeile — wer dreißig hat, will sie überfliegen können. */
@Composable
internal fun FahrzeugReihe(f: Fahrzeugvorlage) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(Rundung.Klein)
            .flaeche(ecke = 9.dp)
            .drawBehind { drawRect(orgTon(f.organisation), size = Size(3.dp.toPx(), size.height)) }
            .padding(start = Abstand.Normal, end = Abstand.Klein, top = Abstand.Klein, bottom = Abstand.Klein),
    ) {
        FahrzeugSymbol(f.typ, f.organisation, groesse = 26.dp)
        Text(f.typ, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        if (f.hiOrg.isNotBlank() && f.hiOrg != "Keine") Traegertitel(f.hiOrg, traegerName(f.hiOrg))
        Text(
            f.beschreibung,
            style = Schrift.Winzig,
            color = Farben.TextLeise,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(f.besatzung, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
    }
}

/** Eine kleine Marke — Besatzung, Fähigkeit. */
@Composable
internal fun Minimarke(text: String, mono: Boolean = false) {
    Text(
        text = text,
        style = if (mono) Schrift.Winzig.copy(fontFamily = Schrift.Mono) else Schrift.Winzig,
        color = Farben.TextLeise,
        modifier = Modifier
            .border(1.dp, Farben.Rand, Rundung.Rund)
            .padding(horizontal = Abstand.Klein, vertical = 1.dp),
    )
}

// -------------------------------------------------------- Gruppen und Suche

/** Eine Trägergruppe innerhalb einer Kategorie. */
internal data class GarageTraegergruppe(val hiOrg: String, val name: String, val fahrzeuge: List<Fahrzeugvorlage>)

/** Eine Kategorie einer Organisation — „Feuerwehr · Löschfahrzeuge". */
internal data class GarageFahrzeuggruppe(val schluessel: String, val organisation: String, val traeger: List<GarageTraegergruppe>)

/** Die feste Reihenfolge der Kategorien je Organisation — aus `fahrzeugGruppen.ts`. */
private val KATEGORIEFOLGE = mapOf(
    "Feuerwehr" to listOf(
        "Löschfahrzeuge", "Tanklöschfahrzeuge", "Hubrettung", "Rüst- und Technikzug", "Führung",
        "Logistik und Mannschaft", "Gefahrgut und Messtechnik", "Wasserversorgung", "Vegetationsbrand",
        "Wasserrettung", "Anhänger und Boote", "Abrollbehälter",
    ),
    "Rettungsdienst" to listOf(
        "Rettungsmittel", "Notarztzubringer", "Krankentransport", "Führung", "Massenanfall und Betreuung",
        "Wasserrettung", "Seenotrettung", "Bergrettung", "Rettungshunde",
    ),
    "Thw" to listOf("Bergung", "Fachgruppen", "Führung und Mannschaft"),
    "Polizei" to listOf(
        "Streifendienst", "Bundespolizei", "Einsatzeinheiten", "Kriminaldienst", "Spezialkräfte",
        "Luftunterstützung",
    ),
)

/**
 * Nach Organisation, Kategorie und — wo mehrere vertreten sind — Träger gebündelt.
 *
 * Nur aufgeteilt, wenn eine Gruppe wirklich mehr als einen Träger enthält: sonst
 * stünde bei einer reinen Feuerwehrgruppe ein sinnloser Kopf „Kein Träger" darüber.
 */
internal fun garageGruppieren(liste: List<Fahrzeugvorlage>): List<GarageFahrzeuggruppe> {
    val nach = linkedMapOf<String, MutableList<Fahrzeugvorlage>>()
    liste.forEach { f -> nach.getOrPut("${orgName(f.organisation)} · ${f.kategorie}") { mutableListOf() }.add(f) }

    fun rang(org: String, kategorie: String): Pair<Int, Int> {
        val o = BUCH_ORGANISATIONEN.indexOf(org).let { if (it < 0) BUCH_ORGANISATIONEN.size else it }
        val k = KATEGORIEFOLGE[org]?.indexOf(kategorie)?.takeIf { it >= 0 } ?: Int.MAX_VALUE
        return o to k
    }

    return nach.entries
        .map { (schluessel, fahrzeuge) ->
            val erste = fahrzeuge.first()
            Triple(schluessel, erste, rang(erste.organisation, erste.kategorie))
        }
        .sortedWith(
            compareBy<Triple<String, Fahrzeugvorlage, Pair<Int, Int>>> { it.third.first }
                .thenBy { it.third.second }
                .thenBy { it.second.kategorie },
        )
        .map { (schluessel, erste, _) ->
            GarageFahrzeuggruppe(schluessel, erste.organisation, traegerGruppen(nach.getValue(schluessel)))
        }
}

private fun traegerGruppen(fahrzeuge: List<Fahrzeugvorlage>): List<GarageTraegergruppe> {
    val nach = linkedMapOf<String, MutableList<Fahrzeugvorlage>>()
    fahrzeuge.forEach { f -> nach.getOrPut(f.hiOrg.ifBlank { "Keine" }) { mutableListOf() }.add(f) }

    if (nach.size <= 1) {
        return nach.map { (hiOrg, liste) -> GarageTraegergruppe(hiOrg, traegerName(hiOrg), liste.sortedBy { it.typ }) }
    }
    return nach.entries
        .sortedBy { traegerName(it.key).ifBlank { "Sonstige" } }
        .map { (hiOrg, liste) ->
            GarageTraegergruppe(
                hiOrg,
                if (hiOrg == "Keine") "Öffentlicher Träger" else traegerName(hiOrg),
                liste.sortedBy { it.typ },
            )
        }
}

// ------------------------------------------------------------- Die Ablage

private const val ABLAGE = "pagerspass.dienstbuch"
private const val ANSICHT = "garage.ansicht"

/** Gesperrter Speicher ist kein Grund, die Garage nicht zu zeigen. */
private fun ansichtLesen(zusammenhang: Context): String = runCatching {
    zusammenhang.getSharedPreferences(ABLAGE, Context.MODE_PRIVATE).getString(ANSICHT, null)
}.getOrNull().let { if (it == "liste") "liste" else "kacheln" }

private fun ansichtMerken(zusammenhang: Context, ansicht: String) {
    runCatching {
        zusammenhang.getSharedPreferences(ABLAGE, Context.MODE_PRIVATE).edit().putString(ANSICHT, ansicht).apply()
    }
}
