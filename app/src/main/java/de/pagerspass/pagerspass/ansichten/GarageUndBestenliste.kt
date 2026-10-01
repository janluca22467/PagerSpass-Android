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
import de.pagerspass.pagerspass.netz.Bestenlistenplatz
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
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
    beiLaden: () -> Unit = {},
    beiHolen: (String, Boolean) -> Unit = { _, _ -> },
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

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
            Kasten(abstandInnen = Abstand.Klein) {
                Kennzahlzeile("Fahrzeuge", daten.fahrzeuge.size.toString())
                Kennzahlzeile("Credits", zahl(daten.stand.credits))
                if (daten.stand.offeneWahlen > 0) {
                    Kennzahlzeile("Offene Gutscheine", daten.stand.offeneWahlen.toString())
                }
                daten.stand.bisZurNaechstenWahl?.let {
                    Kennzahlzeile("Bis zur nächsten Wahl", "${zahl(it)} XP")
                }
            }

            Abschnitt("Auf deinem Hof") {
                if (daten.fahrzeuge.isEmpty()) {
                    Leerhinweis(
                        "Noch kein Fahrzeug. Beim ersten Aufstieg darfst du dir eines aussuchen.",
                    )
                } else {
                    daten.fahrzeuge.forEach { Fahrzeugzeile(it) }
                }
            }

            // <b>Der Gutschein steht vor dem Autohaus.</b> Wer eine Wahl offen
            // hat, soll sie nicht übersehen und stattdessen Credits ausgeben —
            // eine Wahl kostet nichts und ist mehr wert als jeder Kauf.
            if (daten.stand.offeneWahlen > 0) {
                Abschnitt("Deine Wahl") {
                    SehrLeise(
                        "Du hast ${daten.stand.offeneWahlen} Gutschein" +
                            (if (daten.stand.offeneWahlen == 1) "" else "e") +
                            " offen — such dir ein Fahrzeug aus, es kostet nichts.",
                    )
                    daten.angebot.take(ANGEBOT_WAHL).forEach { fahrzeug ->
                        Fahrzeugzeile(
                            fahrzeug = fahrzeug,
                            knopf = {
                                Knopf(
                                    aufschrift = "Aussuchen",
                                    beiDruck = { beiHolen(fahrzeug.id, false) },
                                    art = Knopfart.Haupt,
                                    aktiv = !laeuft,
                                    kompakt = true,
                                )
                            },
                        )
                    }
                }
            }

            Abschnitt("Autohaus") {
                if (daten.angebot.isEmpty()) {
                    Leerhinweis("Zurzeit steht nichts zum Kauf.")
                } else {
                    SehrLeise("Preise rechnet der Server je Konto — sie stehen an der Zeile.")
                    daten.angebot.take(ANGEBOT_KAUF).forEach { fahrzeug ->
                        val preis = fahrzeug.preis ?: 0
                        val fehlt = preis - daten.stand.credits

                        Fahrzeugzeile(
                            fahrzeug = fahrzeug,
                            knopf = {
                                if (fehlt > 0) {
                                    Knopf("$fehlt C fehlen", {}, aktiv = false, kompakt = true)
                                } else {
                                    Knopf(
                                        aufschrift = "$preis C",
                                        beiDruck = { beiHolen(fahrzeug.id, true) },
                                        art = Knopfart.Haupt,
                                        aktiv = !laeuft,
                                        kompakt = true,
                                    )
                                }
                            },
                        )
                    }

                    if (daten.angebot.size > ANGEBOT_KAUF) {
                        SehrLeise(
                            "… und ${zahl(daten.angebot.size - ANGEBOT_KAUF)} weitere Baupläne",
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
    knopf: (@Composable () -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                ecke = 9.dp,
                randfarbe = if (fahrzeug.tagesangebot) Farben.AmberTief else Farben.Rand,
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
                    // Der reguläre Preis steht daneben, nicht durchgestrichen:
                    // Ein durchgestrichener Preis in einer Nebenzeile liest sich
                    // schlechter als der Satz, der sagt, was er war.
                    fahrzeug.regulaer?.let { "sonst $it C" },
                ).joinToString(" · "),
            )
        }

        if (fahrzeug.organisation.isNotBlank()) {
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
