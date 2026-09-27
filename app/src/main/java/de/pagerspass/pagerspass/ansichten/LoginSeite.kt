package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.netz.Kontowege
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    /** Die Wege ohne Anmeldung — „Passwort vergessen" (Bereich Konto). */
    wege: Kontowege? = null,
    laeuft: Boolean = false,
    fehler: String? = null,
    /** Die freundliche Auskunft unter dem Formular — „dein Konto wartet auf die Freischaltung". */
    hinweis: String? = null,
    /** Ob dieser Stand neue Konten erst freischalten lässt — die Regel der Beta. */
    kontofreischaltung: Boolean = false,
    server: String = Server.VORGABE,
    beiAnmelden: (String, String) -> Unit = { _, _ -> },
    /** Benutzername, Anzeigename, Passwort, E-Mail-Adresse. */
    beiKontoAnlegen: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    beiZweiFaktor: (String) -> Unit = {},
    beiZweiFaktorAbbrechen: () -> Unit = {},
    beiServerWechsel: (String) -> Unit = {},
    /** Eine Seite ohne Anmeldung öffnen — `"recht/impressum"`, `"vertrag/kuendigen"`. */
    beiSeite: (String) -> Unit = {},
    zweiFaktorZiel: String? = null,
) {
    val bereich = rememberCoroutineScope()
    var modus by rememberSaveable { mutableStateOf(Anmeldeart.Anmelden) }
    // Vier Zustände desselben Formulars und keine vier Seiten: Die Eingaben —
    // allen voran der Benutzername — sollen dabei stehen bleiben.
    var schritt by rememberSaveable { mutableStateOf("formular") }
    var benutzername by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var anzeigename by rememberSaveable { mutableStateOf("") }
    var passwort by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var vergessenName by rememberSaveable { mutableStateOf("") }
    var neuesPasswort by remember { mutableStateOf("") }
    var agb by rememberSaveable { mutableStateOf(false) }
    var nutzung by rememberSaveable { mutableStateOf(false) }
    var datenschutz by rememberSaveable { mutableStateOf(false) }
    // Volljährig — oder mit dem Einverständnis der Eltern. Ein Konto ist ein
    // Vertrag, und den schließt ein Minderjähriger nur mit Einwilligung seiner
    // Erziehungsberechtigten (§§ 107, 108 BGB). Nicht vorausgewählt.
    var alter by rememberSaveable { mutableStateOf(false) }
    var textOffen by rememberSaveable { mutableStateOf<String?>(null) }

    // Die eigene Meldung (Prüfung, Passwort vergessen) und die grüne Auskunft.
    var meldung by remember { mutableStateOf<String?>(null) }
    var bestaetigung by remember { mutableStateOf<String?>(null) }
    var eigenesLaeuft by remember { mutableStateOf(false) }

    val zustimmungVollstaendig = agb && nutzung && datenschutz && alter
    val emailSiehtGutAus = EMAIL_FORM.matches(email.trim())
    val sperrt = laeuft || eigenesLaeuft

    fun modusWechseln(neu: Anmeldeart) {
        modus = neu
        meldung = null
        bestaetigung = null
    }

    fun zurueck() {
        schritt = "formular"
        meldung = null
        bestaetigung = null
        code = ""
        neuesPasswort = ""
    }

    fun absenden() {
        meldung = null
        bestaetigung = null
        if (modus == Anmeldeart.Anmelden) {
            beiAnmelden(benutzername, passwort)
            return
        }
        val grund = when {
            !emailSiehtGutAus ->
                "Bitte gib eine gültige E-Mail-Adresse an — sie ist dein Weg zurück, falls du dein " +
                    "Passwort vergisst."
            anzeigename.trim().length < 2 -> "Bitte einen Anzeigenamen mit mindestens zwei Zeichen eingeben."
            !zustimmungVollstaendig ->
                "Bitte bestätige AGB, Nutzungsbedingungen, die Kenntnisnahme der Datenschutzerklärung " +
                    "und dass du volljährig bist oder deine Erziehungsberechtigten einverstanden sind."
            else -> null
        }
        meldung = grund
        if (grund == null) beiKontoAnlegen(benutzername, anzeigename, passwort, email.trim())
    }

    /** Ein Weg ohne Anmeldung — mit eigener Sperre, denn die Sitzung weiß nichts davon. */
    fun ohneAnmeldung(tat: suspend () -> Unit) {
        if (eigenesLaeuft) return
        eigenesLaeuft = true
        meldung = null
        bereich.launch {
            try {
                tat()
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (e: Exception) {
                meldung = e.message ?: "Das hat nicht geklappt."
            } finally {
                eigenesLaeuft = false
            }
        }
    }

    Seite(modifier = modifier, abstand = Abstand.Gross) {
        Empfangskopf()

        when {
            zweiFaktorZiel != null -> Formular("Anmeldeschutz") {
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
                    aktiv = !sperrt && code.length == 6,
                    breit = true,
                )
                Knopf(
                    aufschrift = "Abbrechen",
                    beiDruck = {
                        code = ""
                        beiZweiFaktorAbbrechen()
                    },
                    aktiv = !sperrt,
                    breit = true,
                )
                if (fehler != null) Meldung(fehler)
            }

            // Passwort vergessen, Schritt 1: wer bist du?
            schritt == "vergessen" -> Formular("Passwort vergessen") {
                Text(
                    text = "Gib deinen Benutzernamen oder deine hinterlegte E-Mail-Adresse an. Wenn dazu " +
                        "ein Konto mit bestätigter Adresse gehört, schicken wir einen Code.",
                    style = Schrift.Klein,
                    color = Farben.TextSehrLeise,
                )
                Feld(
                    wert = vergessenName,
                    beiAenderung = { vergessenName = it.take(254) },
                    etikett = "Benutzername oder E-Mail",
                    weiterTaste = ImeAction.Done,
                )
                Knopf(
                    aufschrift = "Code anfordern",
                    beiDruck = {
                        if (vergessenName.isBlank()) {
                            meldung = "Bitte deinen Benutzernamen oder deine E-Mail-Adresse angeben."
                        } else if (wege != null) {
                            // Auch wenn es das Konto nicht gibt, geht es weiter: Der Server
                            // antwortet in jedem Fall gleich, und ein Formular, das bei einem
                            // unbekannten Namen stehen bliebe, machte diese Zusage zunichte.
                            ohneAnmeldung {
                                val antwort = wege.passwortVergessen(vergessenName.trim())
                                bestaetigung = antwort.meldung
                                schritt = "neuesPasswort"
                            }
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = !sperrt,
                    breit = true,
                )
                Knopf("Zurück", { zurueck() }, aktiv = !sperrt, breit = true)
                meldung?.let { Meldung(it) }
            }

            // Passwort vergessen, Schritt 2: Code und neues Passwort.
            schritt == "neuesPasswort" -> Formular("Neues Passwort") {
                bestaetigung?.let { Hinweis(it) }
                Codefeld(wert = code, beiAenderung = { code = it }, etikett = "Code aus der E-Mail")
                Feld(
                    wert = neuesPasswort,
                    beiAenderung = { neuesPasswort = it },
                    etikett = "Neues Passwort",
                    geheim = true,
                    weiterTaste = ImeAction.Done,
                )
                Text("Mindestens 8 Zeichen.", style = Schrift.Klein, color = Farben.TextSehrLeise)
                Knopf(
                    aufschrift = "Passwort setzen",
                    beiDruck = {
                        if (wege != null) {
                            ohneAnmeldung {
                                wege.passwortNeuSetzen(vergessenName.trim(), code, neuesPasswort)
                                // Zurück ins Anmeldeformular, mit dem Namen schon eingetragen.
                                // Automatisch angemeldet wird bewusst nicht — der Server hat
                                // gerade alle Sitzungen dieses Kontos beendet.
                                benutzername = vergessenName.trim()
                                passwort = ""
                                code = ""
                                neuesPasswort = ""
                                schritt = "formular"
                                modus = Anmeldeart.Anmelden
                                bestaetigung = "Dein neues Passwort steht. Melde dich jetzt damit an."
                            }
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = !sperrt,
                    breit = true,
                )
                Knopf("Abbrechen", { zurueck() }, aktiv = !sperrt, breit = true)
                meldung?.let { Meldung(it) }
            }

            else -> Formular {
                Segment(
                    seiten = Anmeldeart.entries,
                    gewaehlt = modus,
                    beiWahl = { modusWechseln(it) },
                    aufschrift = { it.aufschrift },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Die Regel der Beta, bevor jemand ein Konto anlegt — nicht erst danach.
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

                // Beim Anmelden geht beides: der Benutzername oder die hinterlegte Adresse.
                // Der Server unterscheidet am „@" (ein Benutzername enthält nie eins).
                val anmelden = modus == Anmeldeart.Anmelden
                Feld(
                    wert = benutzername,
                    beiAenderung = { benutzername = it.take(if (anmelden) 254 else 32) },
                    etikett = if (anmelden) "Benutzername oder E-Mail" else "Benutzername",
                    platzhalter = if (anmelden) "kim112 oder kim@beispiel.de" else "z. B. kim112",
                )

                if (!anmelden) {
                    Feld(
                        wert = email,
                        beiAenderung = { email = it.take(254) },
                        etikett = "E-Mail-Adresse",
                        platzhalter = "kim@beispiel.de",
                        tastatur = KeyboardType.Email,
                    )
                    Text(
                        text = "Für „Passwort vergessen“ und — nur wenn du ihn bestellst — den " +
                            "Newsletter. Du bekommst einen Code zur Bestätigung.",
                        style = Schrift.Klein,
                        color = Farben.TextSehrLeise,
                    )
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

                if (!anmelden) {
                    Text(
                        text = "Mindestens 8 Zeichen. Dein Passwort wird nicht im Klartext gespeichert.",
                        style = Schrift.Klein,
                        color = Farben.TextSehrLeise,
                    )

                    Zustimmungen(
                        agb = agb,
                        nutzung = nutzung,
                        datenschutz = datenschutz,
                        alter = alter,
                        beiAgb = { agb = it },
                        beiNutzung = { nutzung = it },
                        beiDatenschutz = { datenschutz = it },
                        beiAlter = { alter = it },
                        beiText = { seite -> textOffen = seite },
                    )
                }

                Knopf(
                    aufschrift = modus.knopf,
                    beiDruck = { absenden() },
                    art = Knopfart.Haupt,
                    breit = true,
                    aktiv = !sperrt &&
                        benutzername.isNotBlank() &&
                        passwort.isNotEmpty() &&
                        (anmelden || (zustimmungVollstaendig && emailSiehtGutAus && passwort.length >= 8)),
                )

                // Nur beim Anmelden: Im Registrierformular wäre „Passwort vergessen"
                // die Frage nach einem Passwort, das es noch gar nicht gibt.
                if (anmelden) {
                    Textweg(
                        "Passwort vergessen?",
                        {
                            vergessenName = benutzername
                            schritt = "vergessen"
                            meldung = null
                            bestaetigung = null
                        },
                        aktiv = !sperrt,
                        farbe = Farben.TextLeise,
                    )
                }

                bestaetigung?.let { Hinweis(it) }
                if (hinweis != null) Hinweis(hinweis)
                (meldung ?: fehler)?.let { Meldung(it) }
            }
        }

        Fusszeile(
            server = server,
            beiServerWechsel = beiServerWechsel,
            beiSeite = beiSeite,
        )
    }

    // Die Texte schlagen sich über dem Formular auf — die Eingaben bleiben stehen.
    textOffen?.let { seite -> Rechtstextblende(seite, { textOffen = null }) }
}

/** Die grobe Form einer Adresse; die genaue Regel kommt vom Server. */
private val EMAIL_FORM = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

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
 * Die vier Zustimmungen vor der Kontoanlage.
 *
 * <b>Vier Haken und nicht einer.</b> AGB, Nutzungsbedingungen und Datenschutz
 * sind drei Erklärungen mit drei verschiedenen Rechtsfolgen; die Datenschutz-
 * erklärung wird zur Kenntnis genommen, nicht akzeptiert. Der vierte stellt die
 * Altersfrage dort, wo der Vertrag geschlossen wird.
 *
 * Die Texte öffnen sich in einer Blende über dem Formular — die Eingaben bleiben
 * stehen, wie im Web der neue Tab.
 */
@Composable
private fun Zustimmungen(
    agb: Boolean,
    nutzung: Boolean,
    datenschutz: Boolean,
    alter: Boolean,
    beiAgb: (Boolean) -> Unit,
    beiNutzung: (Boolean) -> Unit,
    beiDatenschutz: (Boolean) -> Unit,
    beiAlter: (Boolean) -> Unit,
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

        Hakenzeile(
            text = "Ich bin volljährig — oder meine Erziehungsberechtigten sind mit meiner Nutzung " +
                "von PagerSpass einverstanden.",
            an = alter,
            beiWechsel = beiAlter,
        )
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
    beiSeite: (String) -> Unit,
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

        // § 312k und § 356a BGB: beide Vertragswege ohne Anmeldung, also genau hier.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Textweg("Impressum", { beiSeite("recht/${Rechtsstand.IMPRESSUM}") })
            Textweg("Datenschutz", { beiSeite("recht/${Rechtsstand.DATENSCHUTZ}") })
            Textweg("Verträge hier kündigen", { beiSeite("vertrag/kuendigen") })
            Textweg("Vertrag widerrufen", { beiSeite("vertrag/widerrufen") })
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
