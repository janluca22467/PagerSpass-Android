package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Garagendaten
import de.pagerspass.pagerspass.mobil.Geraeteeinstellungen
import de.pagerspass.pagerspass.mobil.katalogstaatState
import de.pagerspass.pagerspass.netz.Staaten
import de.pagerspass.pagerspass.ui.bausteine.Segment
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import de.pagerspass.pagerspass.netz.Bestenlistenplatz
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import de.pagerspass.pagerspass.netz.HIORG_NAME
import de.pagerspass.pagerspass.netz.ORG_NAME
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die beiden Seiten hinter dem Dienstbuch.
 *
 * Sie sind keine Wege der Tableiste, sondern liegen eine Ebene tiefer — deshalb
 * tragen sie einen Zurück-Knopf und die Leiste bleibt stehen.
 */

/**
 * Die Garage — der eigene Fuhrpark.
 *
 * <b>Der Server nennt nur Ids</b> (`["hlf20", "dlk23", …]`); wie ein Fahrzeug
 * heißt, was es kann und wie viele draufpassen, steht im Katalog. Beides wird in
 * der Sitzung zusammengeführt, damit die Seite nicht zwei Quellen verrechnen muss.
 *
 * <b>Zwei Wege zu einem Fahrzeug, und sie sind nicht dasselbe.</b> Eine *Wahl*
 * kommt aus einem Aufstieg, kostet nichts und ist an `offeneWahlen` gebunden;
 * ein *Kauf* kostet Credits und geht immer. Deshalb stehen sie als zwei
 * Abschnitte da — und die Wahl vor dem Kauf, damit niemand Credits ausgibt für
 * etwas, das er umsonst bekäme.
 */
@Composable
fun GarageSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    garage: Bereichsstand<Garagendaten> = Bereichsstand(),
    laeuft: Boolean = false,
    /** Der Fahrzeugkatalog — für Organisation, Kategorie und Träger der eigenen Wagen. */
    katalog: List<de.pagerspass.pagerspass.netz.Fahrzeugvorlage> = emptyList(),
    beiLaden: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") beiHolen: (String, Boolean) -> Unit = { _, _ -> },
    /** Zum Shop — dort wird beschafft, hier steht nur der Wink. */
    beiShop: () -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }
    var tor by rememberSaveable { mutableStateOf("alle") }
    var suche by rememberSaveable { mutableStateOf("") }
    val zusammenhang = LocalContext.current
    // Kacheln oder Zeilen — die Wahl bleibt über Sitzungen hinweg stehen (im Web `localStorage`).
    var ansicht by remember { mutableStateOf(Geraeteeinstellungen.garagenansicht(zusammenhang)) }
    val staat by katalogstaatState()

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Garage",
            unterzeile = "Deine Fahrzeuge",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = garage.laedt,
            fehler = garage.fehler,
            inhalt = garage.inhalt,
            beiErneut = beiLaden,
        ) { daten ->
            val plaene = katalog.associateBy { it.id }
            // Der Anteil zählt nur den eingestellten Katalog (`alleVorlagen` im Web).
            val katalogDesStaats = katalog.count { (it.staaten?.firstOrNull() ?: Staaten.DEUTSCHLAND) == staat }
            val bestand = daten.fahrzeuge
            val gutscheine = daten.stand.offeneWahlen
            val fortschritt = daten.stand.proOrganisation
            val tore = listOf("Feuerwehr", "Rettungsdienst", "Thw", "Polizei").map { org ->
                Triple(
                    org,
                    bestand.count { it.organisation == org },
                    fortschritt.firstOrNull { it.organisation == org }?.erfahrung ?: 0,
                )
            }.filter { it.second > 0 || it.third > 0 }
            val groesste = tore.maxByOrNull { it.second }?.takeIf { it.second > 0 }

            // Die Tafel: vier Zahlen, jede mit der Zeile, die sie einordnet —
            // dieselbe wie auf der Übersicht, damit die Garage nicht aus dem Buch fällt.
            Kennzahltafel(
                listOf(
                    { m ->
                        Kennzahlkachel(
                            "Fahrzeuge",
                            zahl(bestand.size),
                            if (katalogDesStaats > 0) "${Math.round(bestand.size * 100.0 / katalogDesStaats)} % des Katalogs" else "—",
                            m,
                            farbe = Farben.BlauHell,
                            zeichen = Tafelzeichen.FAHRZEUG,
                        )
                    },
                    { m ->
                        Kennzahlkachel(
                            "Hallen",
                            tore.count { it.second > 0 }.toString(),
                            groesste?.let { "größte: ${ORG_NAME[it.first] ?: it.first}" } ?: "noch leer",
                            m,
                            farbe = Farben.ViolettHell,
                            zeichen = Tafelzeichen.HALLE,
                        )
                    },
                    { m ->
                        Kennzahlkachel(
                            "Gutscheine",
                            gutscheine.toString(),
                            when {
                                gutscheine > 0 -> "einlösen im Shop"
                                daten.stand.bisZurNaechstenWahl == null -> "Laufbahn abgeschlossen"
                                else -> "nächster in ${zahl(daten.stand.bisZurNaechstenWahl)} P"
                            },
                            m,
                            zeichen = Tafelzeichen.GUTSCHEIN,
                        )
                    },
                    { m ->
                        Kennzahlkachel(
                            "Erfahrung",
                            zahl(fortschritt.sumOf { it.erfahrung }),
                            "über alle Organisationen",
                            m,
                            farbe = Farben.GruenHell,
                            zeichen = Tafelzeichen.STERN,
                        )
                    },
                ),
            )

            // Beschafft wird im Shop — hier steht nur der Wink dorthin, mit der Zahl
            // der wartenden Gutscheine. Vorher standen Wahl und Autohaus ein zweites
            // Mal hier, neben demselben Angebot im Shop.
            Buchwink(
                zeichen = listOf("M4 8h16l-1 13H5L4 8Z", "M8 8V6a4 4 0 0 1 8 0v2"),
                wartet = gutscheine > 0,
                modifier = Modifier.clickable(role = Role.Button, onClick = beiShop),
            ) {
                Text(
                    if (gutscheine > 0) {
                        "$gutscheine Fahrzeuggutschein${if (gutscheine == 1) "" else "e"} offen"
                    } else {
                        "Neue Fahrzeuge gibt es im Shop"
                    },
                    style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                )
                SehrLeise(
                    if (gutscheine > 0) {
                        "Jeder Aufstieg bringt einen — einlösen im Shop."
                    } else {
                        "Einlösen mit Fahrzeuggutschein aus dem Aufstieg oder kaufen mit Credits."
                    },
                )
            }

            // Die Hallentore: ein Tor je Organisation, mit Bestand und verdienten Punkten.
            if (tore.isNotEmpty()) {
                Pillenreihe {
                    Pille("Alle", an = tor == "alle", beiDruck = { tor = "alle" }, zahl = bestand.size)
                    tore.forEach { (org, anzahl, _) ->
                        // Am Handy ohne Punkte (mobil.css): Die stehen wortgleich in der
                        // Übersicht, als zweite Zahl am Filter brauchten fünf Tore drei Zeilen.
                        Pille(
                            ORG_NAME[org] ?: org,
                            an = tor == org,
                            beiDruck = { tor = org },
                            zahl = anzahl,
                            farbe = organisationsfarbe(org),
                        )
                    }
                }
            }
            Feld(
                wert = suche,
                beiAenderung = { suche = it.take(40) },
                platzhalter = "⌕  Fahrzeug, Fähigkeit oder Träger suchen …",
            )
            // Am Handy unter dem Feld und über die ganze Breite (`.umschalter` bei 560 Punkten).
            Segment(
                seiten = listOf(ANSICHT_KACHELN, ANSICHT_LISTE),
                gewaehlt = ansicht,
                beiWahl = {
                    ansicht = it
                    Geraeteeinstellungen.garagenansichtSetzen(zusammenhang, it)
                },
                aufschrift = { if (it == ANSICHT_LISTE) "Liste" else "Kacheln" },
                modifier = Modifier.fillMaxWidth(),
            )

            // Eine Suche geht durch alle Tore: Wer „Drehleiter" eintippt, will sie
            // finden und nicht erst wissen, in welcher Halle sie steht.
            val begriff = suche.trim().lowercase()
            val sichtbar = bestand
                .filter { begriff.isNotEmpty() || tor == "alle" || it.organisation == tor }
                .filter {
                    begriff.isEmpty() ||
                        it.typ.lowercase().contains(begriff) ||
                        it.beschreibung.lowercase().contains(begriff) ||
                        (plaene[it.id]?.hiOrg?.lowercase()?.contains(begriff) == true)
                }

            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Text(
                    "${sichtbar.size} ${if (sichtbar.size == 1) "Fahrzeug" else "Fahrzeuge"}",
                    style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                )
                when {
                    begriff.isNotEmpty() -> SehrLeise("Die Suche geht durch alle Hallen.")
                    tor != "alle" -> SehrLeise("in der Halle ${ORG_NAME[tor] ?: tor}")
                }
            }

            if (sichtbar.isEmpty()) {
                Leerhinweis(
                    if (begriff.isNotEmpty()) "Nichts gefunden zu „${suche.trim()}“."
                    else if (bestand.isEmpty()) "Noch kein Fahrzeug. Jeder Aufstieg bringt einen Gutschein — einlösen im Shop."
                    else "In dieser Halle steht noch nichts.",
                )
            }

            // Eine Karte je Organisation und Kategorie; die Kante links trägt die
            // Farbe der Organisation, der Kopf Name und Zahl.
            sichtbar
                .groupBy { "${ORG_NAME[it.organisation] ?: it.organisation} · ${plaene[it.id]?.kategorie.orEmpty()}".trimEnd(' ', '·') }
                .toList()
                .sortedWith(
                    compareBy<Pair<String, List<de.pagerspass.pagerspass.mobil.Fahrzeugzeile>>> {
                        listOf("Feuerwehr", "Rettungsdienst", "Thw", "Polizei").indexOf(it.second.first().organisation)
                            .let { i -> if (i < 0) 9 else i }
                    }.thenBy { it.first },
                )
                .forEach { (schluessel, wagen) ->
                    val liste = ansicht == ANSICHT_LISTE
                    Buchkarte(
                        schluessel,
                        zahl = wagen.size.toString(),
                        kante = organisationsfarbe(wagen.first().organisation),
                        dicht = liste,
                        abstandInnen = if (liste) 0.dp else Abstand.Normal,
                    ) {
                        wagen.sortedBy { it.typ.lowercase() }.forEachIndexed { i, f ->
                            val traeger = plaene[f.id]?.hiOrg?.takeIf { it != "Keine" }?.let { HIORG_NAME[it] }?.ifBlank { null }
                            if (liste) {
                                Fahrzeugzeile(
                                    f,
                                    modifier = Modifier.zeilenstrich(i == wagen.lastIndex),
                                    randlos = true,
                                    traeger = traeger,
                                )
                            } else {
                                Fahrzeugkachel(f, traeger = traeger, faehigkeiten = plaene[f.id]?.faehigkeiten.orEmpty())
                            }
                        }
                    }
                }
        }
    }
}

/** Die beiden Ansichten der Garage — die Wörter, die auch in der Gerätedatei stehen. */
private const val ANSICHT_KACHELN = "kacheln"
private const val ANSICHT_LISTE = "liste"

/**
 * Ein Fahrzeug als Kachel (`FahrzeugKarte.vue`): der Streifen links in der Farbe
 * der Organisation, Typ und Träger im Kopf, die Beschreibung auf zwei Zeilen,
 * darunter Besatzung und die ersten drei Fähigkeiten als kleine Marken.
 *
 * <b>Eine Kachel je Zeile.</b> Das Raster des Webs (`minmax(min(300px, 100%), 1fr)`)
 * ergibt am Handy genau eine Spalte — mehr als eine wäre „Hilfeleistungslöschgrupp…".
 */
@Composable
private fun Fahrzeugkachel(
    fahrzeug: de.pagerspass.pagerspass.mobil.Fahrzeugzeile,
    traeger: String?,
    faehigkeiten: List<String>,
) {
    val org = organisationsfarbe(fahrzeug.organisation)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(farbe = Farben.FlaecheHoch, ecke = 14.dp, mitLichtkante = false)
            .drawBehind {
                // Die leuchtende Kante: eingerückt, rechts gerundet wie im Web.
                val breite = 3.dp.toPx()
                val rand = Abstand.Normal.toPx()
                drawRoundRect(
                    color = org,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, rand),
                    size = androidx.compose.ui.geometry.Size(breite, (size.height - 2 * rand).coerceAtLeast(0f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(breite, breite),
                )
            }
            .padding(start = Abstand.Gross + Abstand.Winzig, end = Abstand.Normal, top = Abstand.Normal, bottom = Abstand.Normal),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    fahrzeug.typ,
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (traeger != null) SehrLeise(traeger, mono = true)
                if (fahrzeug.tagesangebot) Marke("Heute", farbe = Farben.Amber)
            }
            if (fahrzeug.beschreibung.isNotBlank()) {
                Text(
                    fahrzeug.beschreibung,
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val marken = listOfNotNull(fahrzeug.besatzung.ifBlank { null }) + faehigkeiten.take(3)
            if (marken.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier.padding(top = Abstand.Haar),
                ) {
                    marken.forEach { wort ->
                        Text(
                            wort,
                            style = Schrift.Winzig,
                            color = Farben.TextLeise,
                            maxLines = 1,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.07f), androidx.compose.foundation.shape.RoundedCornerShape(50))
                                .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Wie viele Baupläne die Liste zeigt.
 *
 * Der Katalog führt weit über hundert; sie alle in eine rollende Spalte zu
 * setzen kostet dieselbe Sekunde wie die 991 Abzeichen. Was darüber liegt, steht
 * als Zahl darunter — ein stiller Schnitt sähe aus, als gäbe es nicht mehr.
 */
private const val ANGEBOT_KAUF = 40

/** Bei der Gutschein-Wahl reicht eine kurze Reihe: Sie ist ein Vorschlag, keine Liste. */
private const val ANGEBOT_WAHL = 8

/** Eine Fahrzeugzeile — Typ, Beschreibung, Organisation und optional ein Knopf. */
@Composable
private fun Fahrzeugzeile(
    fahrzeug: de.pagerspass.pagerspass.mobil.Fahrzeugzeile,
    modifier: Modifier = Modifier,
    /** Als Zeile einer Karte, ohne eigenen Kasten. */
    randlos: Boolean = false,
    /** Der Träger (DRK, JUH, …), wo die Karte mehrere vereint. */
    traeger: String? = null,
    knopf: (@Composable () -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (randlos) {
                    Modifier
                } else {
                    Modifier.flaeche(
                        ecke = 9.dp,
                        randfarbe = if (fahrzeug.tagesangebot) Farben.AmberTief else Farben.Rand,
                    )
                },
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fahrzeug.typ,
                    style = Schrift.Normal,
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (fahrzeug.tagesangebot) Marke("Heute", farbe = Farben.Amber)
            }
            SehrLeise(
                listOfNotNull(
                    fahrzeug.beschreibung.ifBlank { null },
                    fahrzeug.besatzung.ifBlank { null }?.let { "Besatzung $it" },
                    traeger,
                    // Der reguläre Preis steht daneben, nicht durchgestrichen:
                    // Ein durchgestrichener Preis in einer Nebenzeile liest sich
                    // schlechter als der Satz, der sagt, was er war.
                    fahrzeug.regulaer?.let { "sonst $it C" },
                ).joinToString(" · "),
            )
        }

        if (fahrzeug.organisation.isNotBlank() && !randlos) {
            Marke(
                text = fahrzeug.organisation,
                farbe = organisationsfarbe(fahrzeug.organisation),
            )
        }

        knopf?.invoke()
    }
}

/**
 * Die Bestenliste.
 *
 * <b>Der eigene Platz ist hervorgehoben, nicht herausgelöst.</b> Wer ihn oben
 * einzeln zeigt, nimmt der Liste ihre Aussage — man will sehen, *zwischen* wem
 * man steht.
 */
@Composable
fun BestenlisteSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    liste: Bereichsstand<List<Bestenlistenplatz>> = Bereichsstand(),
    beiLaden: () -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Bestenliste",
            unterzeile = "Nach Erfahrung",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = liste.laedt,
            fehler = liste.fehler,
            inhalt = liste.inhalt,
            beiErneut = beiLaden,
        ) { plaetze ->
            if (plaetze.isEmpty()) {
                Leerhinweis("Die Liste ist noch leer.")
            } else {
                plaetze.forEach { Platzzeile(it) }
            }
        }
    }
}

@Composable
private fun Platzzeile(platz: Bestenlistenplatz) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                ecke = 9.dp,
                farbe = if (platz.istEigenerEintrag) Farben.FlaecheHoch else Farben.Flaeche,
                randfarbe = if (platz.istEigenerEintrag) Farben.Amber else Farben.Rand,
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        // Die Platzziffer in Monospace und mit fester Mindestbreite: Sonst
        // rückt der Name bei zweistelligen Plätzen ein Stück nach rechts, und
        // eine Liste, deren Zeilen nicht fluchten, liest sich schlechter, als
        // sie aussieht.
        Text(
            text = platz.platz.toString(),
            style = Schrift.MonoNormal,
            color = if (platz.istEigenerEintrag) Farben.Amber else Farben.TextSehrLeise,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 28.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = platz.anzeigename,
                style = Schrift.Normal,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SehrLeise("Stufe ${platz.level} · ${platz.rang}")
        }
        Marke(
            text = "${zahl(platz.erfahrung)} XP",
            farbe = if (platz.istEigenerEintrag) Farben.Amber else Farben.TextLeise,
        )
    }
}

@Composable
private fun Kennzahlzeile(was: String, wert: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = was,
            style = Schrift.Klein,
            color = Farben.TextLeise,
            modifier = Modifier.weight(1f),
        )
        Text(text = wert, style = Schrift.MonoNormal, color = Farben.Text)
    }
}

/**
 * Die Farbe einer Organisation.
 *
 * Sie ist Information und kein Schmuck: Wer eine Liste aus vier Organisationen
 * überfliegt, unterscheidet sie am Farbton, bevor er die Marke liest. Ein
 * unbekannter Name bekommt die leise Textfarbe statt einer geratenen.
 */
internal fun organisationsfarbe(organisation: String): Color = when (organisation.lowercase()) {
    "feuerwehr" -> Farben.OrgFeuerwehr
    "rettungsdienst" -> Farben.OrgRettungsdienst
    "thw" -> Farben.OrgThw
    "polizei" -> Farben.OrgPolizei
    else -> Farben.TextLeise
}
