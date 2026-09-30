package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.netz.Server
import de.pagerspass.pagerspass.ui.bausteine.Codefeld
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.PagerSpassTheme
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Anmeldung — das Gegenstück zu `web/src/views/LoginView.vue`.
 *
 * <b>Ein Formular in zwei Zuständen, keine zwei Seiten.</b> „Anmelden" und
 * „Konto erstellen" teilen sich Benutzername und Passwort; die Registrierung
 * legt Anzeigename und drei Zustimmungen dazu. Als zwei Seiten gebaut wären es
 * zwei Formulare, die beim nächsten Umbau auseinanderlaufen — und für ein
 * Vorleseprogramm zwei Knöpfe, von denen einer zufällig anders aussieht.
 *
 * <b>Der Anmeldeschutz ist ein eigener Schritt.</b> Nicht ein weiteres Feld
 * unten im Formular: Dort ist genau eine Sache zu tun, und Benutzername,
 * Passwort und Zustimmungshaken haben damit nichts zu tun.
 *
 * <b>Und unten steht, mit welchem Server gesprochen wird.</b> Das ist im Web
 * eine Entwicklungshilfe und hier mehr als das: Eine App kann nicht am Server
 * erkennen, wo sie läuft. Steht dort die falsche Adresse, ist das der
 * Unterschied zwischen „die App ist kaputt" und „sie redet mit dem falschen
 * Rechner".
 */
@Composable
fun LoginSeite(
    modifier: Modifier = Modifier,
    laeuft: Boolean = false,
    fehler: String? = null,
    /** Die freundliche Auskunft unter dem Formular — „dein Konto wartet auf die Freischaltung". */
    hinweis: String? = null,
    /** Ob dieser Stand neue Konten erst freischalten lässt — die Regel der Beta. */
    kontofreischaltung: Boolean = false,
    server: String = Server.VORGABE,
    beiAnmelden: (String, String) -> Unit = { _, _ -> },
    beiKontoAnlegen: (String, String, String) -> Unit = { _, _, _ -> },
    beiZweiFaktor: (String) -> Unit = {},
    beiServerWechsel: (String) -> Unit = {},
    zweiFaktorZiel: String? = null,
    /**
     * „Passwort vergessen?" — der Ablauf steht in `KontoSeiten.kt` (`PasswortVergessen`)
     * und bekommt den getippten Namen, einen Rückweg mit Name und Satz und einen
     * Abbruch. Ohne ihn steht die Zeile nicht da.
     */
    passwortVergessen: (@Composable (
        vorbelegt: String,
        beiFertig: (String, String) -> Unit,
        beiZurueck: () -> Unit,
    ) -> Unit)? = null,
) {
    var modus by remember { mutableStateOf(Anmeldeart.Anmelden) }
    var benutzername by remember { mutableStateOf("") }
    var anzeigename by remember { mutableStateOf("") }
    var passwort by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var agb by remember { mutableStateOf(false) }
    var nutzung by remember { mutableStateOf(false) }
    var datenschutz by remember { mutableStateOf(false) }

    var vergessen by remember { mutableStateOf(false) }
    var bestaetigung by remember { mutableStateOf<String?>(null) }

    val zustimmungVollstaendig = agb && nutzung && datenschutz
    val browser = LocalUriHandler.current

    Seite(modifier = modifier, abstand = Abstand.Gross) {
        Empfangskopf()

        if (vergessen && passwortVergessen != null && zweiFaktorZiel == null) {
            passwortVergessen(
                benutzername,
                { name, satz ->
                    // Zurück ins Anmeldeformular, mit dem Namen schon eingetragen —
                    // angemeldet wird bewusst nicht automatisch.
                    benutzername = name
                    passwort = ""
                    modus = Anmeldeart.Anmelden
                    bestaetigung = satz
                    vergessen = false
                },
                { vergessen = false },
            )
        } else if (zweiFaktorZiel != null) {
            Formular("Anmeldeschutz") {
                Text(
                    text = "Wir haben einen sechsstelligen Code an $zweiFaktorZiel geschickt.",
                    style = Schrift.Klein,
                    color = Farben.TextSehrLeise,
                )
                Codefeld(wert = code, beiAenderung = { code = it }, etikett = "Code")
                Knopf(
                    aufschrift = "Anmelden",
                    beiDruck = { beiZweiFaktor(code) },
                    art = Knopfart.Haupt,
                    aktiv = !laeuft && code.length == 6,
                    breit = true,
                )
                if (fehler != null) Meldung(fehler)
            }
        } else {
            Formular {
                Segment(
                    seiten = Anmeldeart.entries,
                    gewaehlt = modus,
                    beiWahl = { modus = it },
                    aufschrift = { it.aufschrift },
                    modifier = Modifier.fillMaxWidth(),
                )

                Feld(
                    wert = benutzername,
                    beiAenderung = { benutzername = it.take(32) },
                    etikett = "Benutzername",
                    platzhalter = "z. B. kim112",
                )

                // Die Regel der Beta, bevor jemand ein Konto anlegt — nicht erst danach.
                // Der Server sagt sie mit der Version; auf pagerspass.de steht der
                // Absatz nie.
                if (modus == Anmeldeart.Registrieren && kontofreischaltung) {
                    Text(
                        text = "Auf der Beta wird jedes neue Konto vom Team freigeschaltet: Leg " +
                            "dein Konto an und stell dann im Discord-Server unter #beta-antrag " +
                            "einen Antrag mit deinem Benutzernamen. Bis dahin kannst du dich " +
                            "noch nicht anmelden.",
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                    )
                }

                if (modus == Anmeldeart.Registrieren) {
                    Feld(
                        wert = anzeigename,
                        beiAenderung = { anzeigename = it.take(24) },
                        etikett = "Anzeigename",
                        platzhalter = "Name im Funk",
                    )
                }

                Feld(
                    wert = passwort,
                    beiAenderung = { passwort = it },
                    etikett = "Passwort",
                    geheim = true,
                    weiterTaste = ImeAction.Done,
                )

                if (modus == Anmeldeart.Registrieren) {
                    Text(
                        text = "Mindestens 8 Zeichen. Dein Passwort wird nicht im Klartext gespeichert.",
                        style = Schrift.Klein,
                        color = Farben.TextSehrLeise,
                    )

                    Zustimmungen(
                        agb = agb,
                        nutzung = nutzung,
                        datenschutz = datenschutz,
                        beiAgb = { agb = it },
                        beiNutzung = { nutzung = it },
                        beiDatenschutz = { datenschutz = it },
                        beiText = { seite -> browser.openUri(Rechtsstand.adresse(server, seite)) },
                    )
                }

                Knopf(
                    aufschrift = modus.knopf,
                    beiDruck = {
                        if (modus == Anmeldeart.Anmelden) {
                            beiAnmelden(benutzername, passwort)
                        } else {
                            beiKontoAnlegen(benutzername, anzeigename, passwort)
                        }
                    },
                    art = Knopfart.Haupt,
                    breit = true,
                    aktiv = !laeuft &&
                        benutzername.isNotBlank() &&
                        passwort.length >= 8 &&
                        (modus == Anmeldeart.Anmelden ||
                            (anzeigename.isNotBlank() && zustimmungVollstaendig)),
                )

                // Nur beim Anmelden: Im Registrierformular wäre es die Frage nach
                // einem Passwort, das es noch gar nicht gibt. Ein Textweg und keine
                // amberne Fläche — die Handlung hier ist das Anmelden.
                if (modus == Anmeldeart.Anmelden && passwortVergessen != null) {
                    Textweg("Passwort vergessen?", {
                        bestaetigung = null
                        vergessen = true
                    })
                }

                bestaetigung?.let { Hinweis(it) }
                if (hinweis != null) Hinweis(hinweis)
                if (fehler != null) Meldung(fehler)
            }
        }

        Fusszeile(server = server, beiServerWechsel = beiServerWechsel, beiText = { seite ->
            browser.openUri(Rechtsstand.adresse(server, seite))
        })
    }
}

/** Die zwei Zustände desselben Formulars. */
enum class Anmeldeart(val aufschrift: String, val knopf: String) {
    Anmelden("Anmelden", "Anmelden"),
    Registrieren("Konto erstellen", "Konto erstellen"),
}

/**
 * Der Empfang über dem Formular.
 *
 * <b>Er sieht aus wie der auf dem Startbildschirm, und das soll er.</b> Dasselbe
 * Zeichen, derselbe Schriftzug, dieselbe Zeile darunter — wer sich eben
 * angemeldet hat, soll nicht auf einer fremden Seite ankommen. Deshalb steht er
 * als gemeinsamer Baustein und nicht zweimal.
 */
@Composable
private fun Empfangskopf(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier.fillMaxWidth().padding(vertical = Abstand.SehrGross),
    ) {
        Empfang()

        // Wo der Untertitel bricht, ist nicht gleichgültig: Ohne den
        // Zusammenhalt stand da „… — für die" und darunter „ganze Wache" — ein
        // Satz, der mitten in einer Wendung auseinanderfällt.
        Text(
            text = "Leitstelle · Funk · Melder — für die ganze Wache",
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
            color = Farben.TextSehrLeise,
            textAlign = TextAlign.Center,
        )
    }
}

/** Der Kasten, in dem das Formular steht. */
@Composable
private fun Formular(
    titel: String? = null,
    inhalt: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Gross),
    ) {
        if (titel != null) Text(titel, style = Schrift.Titel, color = Farben.Text)
        inhalt()
    }
}

/**
 * Die drei Zustimmungen vor der Kontoanlage.
 *
 * <b>Drei Haken und nicht einer.</b> AGB, Nutzungsbedingungen und Datenschutz
 * sind drei Erklärungen mit drei verschiedenen Rechtsfolgen; ein
 * Sammelhäkchen („Ich akzeptiere alles") wäre bequemer und als Einwilligung
 * angreifbar. Der dritte ist bewusst anders formuliert — die
 * Datenschutzerklärung wird zur Kenntnis genommen, nicht akzeptiert.
 *
 * Die verlinkten Wörter öffnen den Text im Browser. Dass die App sie noch nicht
 * selbst zeigt, ist eine der offenen Aufgaben vor der Veröffentlichung.
 */
@Composable
private fun Zustimmungen(
    agb: Boolean,
    nutzung: Boolean,
    datenschutz: Boolean,
    beiAgb: (Boolean) -> Unit,
    beiNutzung: (Boolean) -> Unit,
    beiDatenschutz: (Boolean) -> Unit,
    beiText: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        Etikett("Bevor es losgeht")

        Hakenzeile(
            text = verlinkt("Ich akzeptiere die ", "Allgemeinen Geschäftsbedingungen", "."),
            an = agb,
            beiWechsel = beiAgb,
        )
        Textweg("AGB lesen", { beiText(Rechtsstand.AGB) })

        Hakenzeile(
            text = verlinkt("Ich akzeptiere die ", "Nutzungsbedingungen", "."),
            an = nutzung,
            beiWechsel = beiNutzung,
        )
        Textweg("Nutzungsbedingungen lesen", { beiText(Rechtsstand.NUTZUNGSBEDINGUNGEN) })

        Hakenzeile(
            text = verlinkt("Ich habe die ", "Datenschutzerklärung", " gelesen."),
            an = datenschutz,
            beiWechsel = beiDatenschutz,
        )
        Textweg("Datenschutzerklärung lesen", { beiText(Rechtsstand.DATENSCHUTZ) })
    }
}

/**
 * Ein Satz mit einem hervorgehobenen Wort darin.
 *
 * <b>Das Wort ist hier kein Link, obwohl es im Web einer ist.</b> Ein Link
 * mitten in der Aufschrift eines Hakens ist am Daumen eine Falle: Wer ihn
 * treffen will, schaltet den Haken; wer den Haken schalten will, öffnet den
 * Browser. Deshalb steht der Weg zum Text darunter als eigene Zeile mit eigener
 * Zielfläche — dieselbe Auskunft, zwei getrennte Ziele.
 */
private fun verlinkt(vorher: String, wort: String, nachher: String) = buildAnnotatedString {
    append(vorher)
    withStyle(SpanStyle(color = Farben.BlauHell, textDecoration = TextDecoration.Underline)) {
        append(wort)
    }
    append(nachher)
}

/** Die Meldung unter dem Formular — in Monospace, wie im Web. */
@Composable
private fun Meldung(text: String) {
    Text(text = text, style = Schrift.MonoKlein, color = Farben.SignalHell)
}

/**
 * Die freundliche Auskunft — das Gegenstück zur Meldung, wie `.hinweis` im Web:
 * grüner Text in der Farbe für „erledigt", kein zweiter Kasten.
 */
@Composable
private fun Hinweis(text: String) {
    Text(text = text, style = Schrift.Klein, color = Farben.GruenHell)
}

/**
 * Der Fuß.
 *
 * Derselbe Satz wie auf dem Startbildschirm — „Ein Spiel, kein Einsatzmittel" —
 * und die zwei Texte, die nach § 5 DDG von überall her erreichbar sein müssen.
 * Dazu die Zeile, die im Web nur der Entwicklung dient und hier den Server
 * benennt und wechselt.
 */
@Composable
private fun Fusszeile(
    server: String,
    beiServerWechsel: (String) -> Unit,
    beiText: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().padding(top = Abstand.Gross),
    ) {
        Text(
            text = "Ein Spiel, kein Einsatzmittel. Bei echten Notfällen: 112.",
            style = Schrift.Klein.copy(letterSpacing = 0.06.em),
            color = Farben.TextSehrLeise,
            textAlign = TextAlign.Center,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            Textweg("Impressum", { beiText(Rechtsstand.IMPRESSUM) })
            Textweg("Datenschutz", { beiText(Rechtsstand.DATENSCHUTZ) })
        }

        Etikett("Server")
        Segment(
            seiten = Server.AUSWAHL,
            gewaehlt = Server.AUSWAHL.firstOrNull { it == server } ?: Server.AUSWAHL.first(),
            beiWahl = beiServerWechsel,
            aufschrift = { adresse -> adresse.substringAfter("://").substringBefore(":") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = server,
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
            color = Farben.TextSehrLeise,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080B10, heightDp = 1000)
@Composable
private fun LoginVorschau() {
    PagerSpassTheme { LoginSeite() }
}
