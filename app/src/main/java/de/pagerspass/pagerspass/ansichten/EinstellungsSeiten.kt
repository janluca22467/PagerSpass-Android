package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.netz.Betreibermitteilung
import de.pagerspass.pagerspass.netz.Privatsphaere
import de.pagerspass.pagerspass.netz.SICHTBARKEITEN
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Privatsphäre — wer was von einem sieht.
 *
 * <b>Der große Schalter steht oben und ist keiner von vielen.</b> „Privates
 * Konto" nimmt Bestenliste, Vorschläge und den offenen Spielstand in einem Zug
 * zurück; die sechzehn darunter sind die Feinheiten. Wer den großen umlegt, hat
 * die meisten davon schon entschieden — deshalb steht er getrennt, mit Abstand
 * und in einem eigenen Kasten.
 *
 * <b>Jede Angabe wird am Server geprüft.</b> Die Seite zeigt sie nur an, sie
 * setzt sie nicht durch. Das steht auch so da: Wer hier etwas ausschaltet, soll
 * wissen, dass er wirklich verschwindet, und nicht nur aus dieser App.
 */
@Composable
fun PrivatsphaereSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Bereichsstand<Privatsphaere?> = Bereichsstand(),
    beiLaden: () -> Unit = {},
    beiSetzen: (Privatsphaere) -> Unit = {},
    beiZurueck: () -> Unit = {},
    /** Was unter den Schaltern steht — Blockaden, Geräte, Rechte (`KontoSeiten.kt`). */
    zusatz: @Composable ColumnScope.() -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Privatsphäre",
            unterzeile = "Wer was von dir sieht",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = stand.laedt,
            fehler = stand.fehler,
            inhalt = stand.inhalt,
            beiErneut = beiLaden,
        ) { p ->
            Kasten(marke = true, abstandInnen = Abstand.Klein) {
                Schalterzeile(
                    titel = "Privates Konto",
                    unterzeile = "Nimmt Bestenliste, Vorschläge und den offenen Spielstand " +
                        "in einem Zug zurück.",
                    an = p.privatesKonto,
                    beiWechsel = { beiSetzen(p.copy(privatesKonto = it)) },
                )
            }

            Abschnitt("Gefunden werden") {
                Stufenwahl(
                    titel = "Wer dich über deinen Benutzernamen findet",
                    gewaehlt = p.auffindbarkeit,
                    beiWahl = { beiSetzen(p.copy(auffindbarkeit = it)) },
                )
                Stufenwahl(
                    titel = "Wer dir eine Freundschaftsanfrage schicken darf",
                    gewaehlt = p.anfragenVon,
                    beiWahl = { beiSetzen(p.copy(anfragenVon = it)) },
                )
            }

            Abschnitt("Was Freunde sehen") {
                Schalterzeile(
                    "In welcher Runde du gerade Dienst hast",
                    an = p.anwesenheitZeigen,
                    beiWechsel = { beiSetzen(p.copy(anwesenheitZeigen = it)) },
                )
                Schalterzeile(
                    "„Zuletzt online vor …“",
                    an = p.zuletztGesehenZeigen,
                    beiWechsel = { beiSetzen(p.copy(zuletztGesehenZeigen = it)) },
                )
                Schalterzeile(
                    "Einladungen in laufende Runden",
                    an = p.einladungenErlauben,
                    beiWechsel = { beiSetzen(p.copy(einladungenErlauben = it)) },
                )
                Schalterzeile(
                    "Lesebestätigungen senden",
                    an = p.lesebestaetigungenSenden,
                    beiWechsel = { beiSetzen(p.copy(lesebestaetigungenSenden = it)) },
                )
            }

            Abschnitt("Listen und Wertungen") {
                Schalterzeile(
                    "In der Bestenliste erscheinen",
                    an = p.inBestenliste,
                    beiWechsel = { beiSetzen(p.copy(inBestenliste = it)) },
                )
                Schalterzeile(
                    "Anderen als Mitspieler vorgeschlagen werden",
                    unterzeile = "Nach einer gemeinsamen Schicht.",
                    an = p.alsVorschlagErscheinen,
                    beiWechsel = { beiSetzen(p.copy(alsVorschlagErscheinen = it)) },
                )
                Schalterzeile(
                    "In der Welt sichtbar sein",
                    an = p.inWeltSichtbar,
                    beiWechsel = { beiSetzen(p.copy(inWeltSichtbar = it)) },
                )
            }

            Abschnitt("Wache") {
                Schalterzeile(
                    "Deine Wache im Profil zeigen",
                    an = p.gemeinschaftZeigen,
                    beiWechsel = { beiSetzen(p.copy(gemeinschaftZeigen = it)) },
                )
                Schalterzeile(
                    "In der Mitgliederliste erscheinen",
                    an = p.inGemeinschaftsliste,
                    beiWechsel = { beiSetzen(p.copy(inGemeinschaftsliste = it)) },
                )
                Schalterzeile(
                    "Einladungen in Wachen erlauben",
                    an = p.gemeinschaftseinladungenErlauben,
                    beiWechsel = { beiSetzen(p.copy(gemeinschaftseinladungenErlauben = it)) },
                )
                Schalterzeile(
                    "Deine Hilfsfrist zählt für die Wache mit",
                    an = p.wachenstatistikTeilen,
                    beiWechsel = { beiSetzen(p.copy(wachenstatistikTeilen = it)) },
                )
                Schalterzeile(
                    "Deine Chronik mit der Wache teilen",
                    an = p.chronikTeilen,
                    beiWechsel = { beiSetzen(p.copy(chronikTeilen = it)) },
                )
                Schalterzeile(
                    "Kommentare unter deinen Einträgen erlauben",
                    an = p.kommentareErlauben,
                    beiWechsel = { beiSetzen(p.copy(kommentareErlauben = it)) },
                )
            }

            SehrLeise(
                "Jede dieser Einstellungen wird am Server geprüft — was du hier " +
                    "ausschaltest, ist wirklich weg und nicht nur in dieser App verborgen.",
            )
        }

        zusatz()
    }
}

/** Eine Einstellung mit drei Stufen — Alle, Mitspieler, Niemand. */
@Composable
private fun Stufenwahl(titel: String, gewaehlt: String, beiWahl: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Etikett(titel)
        Segment(
            seiten = SICHTBARKEITEN,
            gewaehlt = gewaehlt,
            beiWahl = beiWahl,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Das Postfach — was der Betrieb zu sagen hat.
 *
 * <b>Es hängt an einem Betriebsschalter</b>, und der steht in der Vorgabe auf
 * „aus". Die App fragt ihn über `/api/version` ab und sagt es, statt einen Weg
 * anzubieten, der mit „nicht gefunden" antwortet. Ein abgeschalteter Dienst ist
 * eine Entscheidung und kein Fehler — das ist der Unterschied, den diese Seite
 * zeigen muss.
 *
 * Was es dagegen immer gibt, sind die Betreibermitteilungen: Wartungsfenster,
 * Neuigkeiten, Hinweise. Die stehen hier auch dann, wenn das Postfach zu ist.
 */
@Composable
fun PostfachSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    mitteilungen: Bereichsstand<List<Betreibermitteilung>> = Bereichsstand(),
    postfachFrei: Boolean = false,
    beiLaden: () -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }
    val browser = LocalUriHandler.current

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Postfach",
            unterzeile = "Nachrichten der Verwaltung",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        if (!postfachFrei) {
            Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
                Text("Noch nicht freigeschaltet", style = Schrift.Gross, color = Farben.Text)
                SehrLeise(
                    "Das persönliche Postfach ist noch abgeschaltet. Sobald es offen ist, " +
                        "stehen deine Nachrichten hier — ohne dass du etwas tun musst.",
                )
            }
        }

        Abschnitt("Vom Betrieb") {
            Bereich(
                laedt = mitteilungen.laedt,
                fehler = mitteilungen.fehler,
                inhalt = mitteilungen.inhalt,
                beiErneut = beiLaden,
            ) { liste ->
                if (liste.isEmpty()) {
                    Leerhinweis("Zurzeit gibt es nichts zu vermelden.")
                } else {
                    liste.forEach { m ->
                        Kasten(abstandInnen = Abstand.Klein) {
                            Text(m.titel, style = Schrift.Gross, color = Farben.Text)
                            Text(m.text, style = Schrift.Normal, color = Farben.TextLeise)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                SehrLeise(tag(m.ab), mono = true)
                                if (m.link != null) {
                                    Textweg(m.linkText ?: "Öffnen", { browser.openUri(m.link) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Die Mitteilungs-Einstellungen — sechs Schalter, das Gegenstück zu
 * `MitteilungenView.vue`.
 *
 * <b>Immer der ganze Satz, nie ein einzelnes Feld</b> — wie bei der
 * Privatsphäre. Durchgesetzt wird serverseitig beim Heraussuchen der
 * Empfänger, nicht beim Anzeigen.
 */
@Composable
fun MitteilungenSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Bereichsstand<de.pagerspass.pagerspass.netz.Mitteilungseinstellungen?> =
        Bereichsstand(),
    beiLaden: () -> Unit = {},
    beiSetzen: (de.pagerspass.pagerspass.netz.Mitteilungseinstellungen) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Mitteilungen",
            unterzeile = "Was aufs Gerät darf",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = stand.laedt,
            fehler = stand.fehler,
            inhalt = stand.inhalt,
            beiErneut = beiLaden,
        ) { e ->
            Kasten(marke = true, abstandInnen = Abstand.Klein) {
                Schalterzeile(
                    titel = "Alarm im Dienst",
                    unterzeile = "Der Melder als Systemmeldung, wenn die App im Hintergrund ist.",
                    an = e.alarm,
                    beiWechsel = { beiSetzen(e.copy(alarm = it)) },
                )
            }

            Abschnitt("Vom Spiel") {
                Schalterzeile(
                    "Einladungen",
                    unterzeile = "In Runden und Wachengemeinschaften.",
                    an = e.einladungen,
                    beiWechsel = { beiSetzen(e.copy(einladungen = it)) },
                )
                Schalterzeile(
                    "Direktnachrichten",
                    an = e.privatnachrichten,
                    beiWechsel = { beiSetzen(e.copy(privatnachrichten = it)) },
                )
                Schalterzeile(
                    "Wachenchat",
                    an = e.chat,
                    beiWechsel = { beiSetzen(e.copy(chat = it)) },
                )
                Schalterzeile(
                    "Brett-Kommentare",
                    an = e.brett,
                    beiWechsel = { beiSetzen(e.copy(brett = it)) },
                )
                Schalterzeile(
                    "Tageserinnerung",
                    unterzeile = "Einmal täglich um 12 Uhr, wenn du nicht im Dienst bist.",
                    an = e.tageserinnerung,
                    beiWechsel = { beiSetzen(e.copy(tageserinnerung = it)) },
                )
            }
        }
    }
}
