package de.pagerspass.pagerspass.ansichten

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Einwilligungsbedarf
import de.pagerspass.pagerspass.netz.Kontowege
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.netz.Server
import de.pagerspass.pagerspass.netz.plattformname
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Codefeld
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Die Rechtstexte in der App — und die Dialoge, die an ihnen hängen.
 *
 * <b>Die Texte liegen im Paket und nicht im Netz.</b> Sie stehen unter
 * `assets/recht/` (je Seite eine `.md`), erzeugt aus `web/src/recht/rechtstexte.ts`. Der Grund ist
 * derselbe, aus dem das Web sie beim Bauen hineinnimmt: Der Text gehört zu der
 * Fassung, der jemand zustimmt, und darf sich nicht hinter dem Rücken der App
 * ändern — und er muss ohne Netz und ohne Anmeldung lesbar sein.
 *
 * <b>Gesetzt wird von Hand</b>, mit genau dem, was in diesen Texten vorkommt
 * (`recht/markdown.ts`): `## Überschrift`, Absätze, `- Listenpunkte` und
 * `**Hervorhebungen**`. Kein HTML, keine Webansicht.
 */

// ----------------------------------------------------------------- Setzer

/** Ein Block des Textes: Überschrift, Absatz (Zeile für Zeile) oder Liste. */
sealed interface Rechtsblock {
    data class Ueberschrift(val text: String) : Rechtsblock
    data class Absatz(val zeilen: List<String>) : Rechtsblock
    data class Liste(val punkte: List<String>) : Rechtsblock
}

/**
 * Zerlegt einen Text in Blöcke — `zuBloecken` aus `recht/markdown.ts`.
 *
 * <b>Ein Absatz trägt seine Zeilen einzeln.</b> Aneinandergehängt würde aus der
 * Anschrift im Impressum „Robert Stoll Reiherstieg 14 29525 Uelzen" — eine
 * Anschrift, die als Anschrift nicht mehr zu erkennen ist.
 */
fun zuBloecken(text: String): List<Rechtsblock> {
    val ergebnis = mutableListOf<Rechtsblock>()

    for (abschnitt in text.replace("\r\n", "\n").split(Regex("\\n\\s*\\n"))) {
        val zeilen = abschnitt.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (zeilen.isEmpty()) continue

        var punkte = mutableListOf<String>()
        var fliesstext = mutableListOf<String>()

        fun fliesstextAbschliessen() {
            if (fliesstext.isNotEmpty()) ergebnis += Rechtsblock.Absatz(fliesstext.toList())
            fliesstext = mutableListOf()
        }

        fun listeAbschliessen() {
            if (punkte.isNotEmpty()) ergebnis += Rechtsblock.Liste(punkte.toList())
            punkte = mutableListOf()
        }

        for (zeile in zeilen) {
            when {
                zeile.startsWith("## ") -> {
                    fliesstextAbschliessen()
                    listeAbschliessen()
                    ergebnis += Rechtsblock.Ueberschrift(zeile.removePrefix("## "))
                }
                zeile.startsWith("- ") -> {
                    fliesstextAbschliessen()
                    punkte += zeile.removePrefix("- ")
                }
                else -> {
                    listeAbschliessen()
                    fliesstext += zeile
                }
            }
        }
        fliesstextAbschliessen()
        listeAbschliessen()
    }

    return ergebnis
}

/**
 * Eine Zeile mit ihren `**Hervorhebungen**` — `zuStuecken` aus `markdown.ts`.
 * Unpaarige Sternchen bleiben stehen: lieber ein sichtbares `**` als ein
 * verschluckter Halbsatz.
 */
fun hervorgehoben(zeile: String): AnnotatedString = buildAnnotatedString {
    var rest = 0
    for (treffer in FETT.findAll(zeile)) {
        append(zeile.substring(rest, treffer.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Farben.Text)) {
            append(treffer.value.removePrefix("**").removeSuffix("**"))
        }
        rest = treffer.range.last + 1
    }
    append(zeile.substring(rest))
}

private val FETT = Regex("""\*\*[^*]+\*\*""")

/** Ein Rechtstext aus dem Paket — oder `null`, wenn es die Seite nicht gibt. */
fun rechtstextLesen(zusammenhang: Context, seite: String): String? {
    if (!seite.all { it.isLetterOrDigit() || it == '-' }) return null
    return runCatching {
        zusammenhang.assets.open("recht/$seite.md").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()
}

/** Einen Rechtstext im Hintergrund laden — `null`, solange er noch unterwegs ist. */
@Composable
fun rememberRechtstext(seite: String): Pair<Boolean, String?> {
    val zusammenhang = LocalContext.current.applicationContext
    val stand by produceState<Pair<Boolean, String?>>(false to null, seite) {
        val text = withContext(Dispatchers.IO) { rechtstextLesen(zusammenhang, seite) }
        value = true to text
    }
    return stand
}

/**
 * Der gesetzte Text. Überschriften fett, Listen mit Punkt, Absätze Zeile für
 * Zeile — und Lesetext-Zeilenhöhe, denn diese Seiten werden gelesen, nicht
 * überflogen.
 */
@Composable
fun Rechtstext(text: String, modifier: Modifier = Modifier) {
    val bloecke = remember(text) { zuBloecken(text) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier.fillMaxWidth(),
    ) {
        bloecke.forEach { block ->
            when (block) {
                is Rechtsblock.Ueberschrift -> Text(
                    text = block.text,
                    style = Schrift.Gross.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    modifier = Modifier.padding(top = Abstand.Klein),
                )

                is Rechtsblock.Absatz -> Text(
                    text = hervorgehoben(block.zeilen.joinToString("\n")),
                    style = Schrift.Lesetext,
                    color = Farben.TextLeise,
                )

                is Rechtsblock.Liste -> Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    block.punkte.forEach { punkt ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Text("•", style = Schrift.Lesetext, color = Farben.Amber)
                            Text(
                                text = hervorgehoben(punkt),
                                style = Schrift.Lesetext,
                                color = Farben.TextLeise,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- Die Seite

/**
 * Ein Rechtstext als Seite — `RechtView.vue` samt `Rechtstext.vue`.
 *
 * <b>Speichern und Teilen statt Drucken.</b> § 312i Abs. 1 Nr. 4 BGB verlangt,
 * dass die Bedingungen nicht nur lesbar, sondern auch speicherbar sind. Im Web
 * erledigt das der Druckdialog; hier der Dateidialog des Systems und das
 * Teilen-Blatt.
 *
 * <b>Ohne Konto erreichbar</b> — die Zustimmung bei der Registrierung verlinkt
 * hierher, und ein Rechtstext hinter einer Anmeldung wäre keiner.
 */
@Composable
fun RechtSeite(
    seite: String,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    beiZurueck: () -> Unit = {},
) {
    val zusammenhang = LocalContext.current
    val (fertig, text) = rememberRechtstext(seite)
    val titel = Rechtsstand.TITEL[seite]
    val speichern = rememberDateiSpeichern("text/markdown")

    Seite(modifier = modifier, unterrand = unterrand, breite = Mass.Lesebreite) {
        Seitenkopf(
            titel = titel ?: "Rechtstext",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        when {
            !fertig -> Ladezeile()
            text == null || titel == null -> Leerhinweis("Diese Seite gibt es nicht.")
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        "Speichern",
                        { speichern("pagerspass-$seite.md", "# $titel\n\n$text") },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                    Knopf(
                        "Teilen",
                        { textTeilen(zusammenhang, "$titel\n\n$text", titel) },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                    Knopf(
                        "Im Browser",
                        { imBrowser(zusammenhang, Rechtsstand.adresse(Server.BETRIEB, seite)) },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
                Kasten { Rechtstext(text) }
            }
        }
    }
}

/**
 * Ein Rechtstext in einer Blende — für die Stellen, an denen man die Seite nicht
 * verlassen soll (Registrierung, Rechtsstand, Datenverarbeitung).
 */
@Composable
fun Rechtstextblende(seite: String, beiSchliessen: () -> Unit, etikett: String? = null) {
    val (fertig, text) = rememberRechtstext(seite)

    Blende(
        titel = Rechtsstand.TITEL[seite] ?: etikett ?: "Rechtstext",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Breit,
        kopfknoepfe = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
    ) {
        when {
            !fertig -> Ladezeile()
            text == null -> Leerhinweis("Diese Seite gibt es nicht.")
            else -> Rechtstext(text)
        }
    }
}

/** `DatenverarbeitungDialog.vue` — was die Aufzeichnung zur Produktverbesserung umfasst. */
@Composable
fun Datenverarbeitungblende(beiSchliessen: () -> Unit) =
    Rechtstextblende("datenverarbeitung", beiSchliessen, etikett = "Datenverarbeitung")

// ------------------------------------------------------- Die Startfragen
//
// Die Reihenfolge steht in `PagerSpassApp` (wie in `MobilApp.vue`): Analyse →
// Rechtsstand → E-Mail-Pflicht → Verwarnung → Verwaltungsnachricht → Übertragung.
// Zwei Fragen übereinander beantwortet niemand.

/**
 * Die einmalige Frage nach der Aufzeichnung zur Produktverbesserung —
 * `AnalyseDialog.vue`.
 *
 * <b>Ohne Weg daran vorbei.</b> Kein Zurück, kein Druck daneben: Die Frage will
 * eine Antwort, und „weggeklickt" wäre keine. Zwei Knöpfe, beide gleich leicht zu
 * erreichen.
 */
@Composable
fun Analyseblende(beiAntwort: suspend (Boolean) -> Unit) {
    val v = rememberVorgang()
    val bereich = rememberCoroutineScope()
    var textOffen by remember { mutableStateOf(false) }

    Blende(
        titel = "Dürfen wir deine Daten für die Verbesserung unseres Produktes verwenden?",
        beiSchliessen = {},
        breite = Dialogbreite.Normal,
        schliessenMoeglich = false,
        fussAlsSpalte = true,
        fuss = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf(
                    "Nein, danke",
                    { bereich.vorgang(v) { beiAntwort(false) } },
                    modifier = Modifier.weight(1f),
                    aktiv = !v.laeuft,
                )
                Knopf(
                    "Ja, gerne",
                    { bereich.vorgang(v) { beiAntwort(true) } },
                    modifier = Modifier.weight(1f),
                    art = Knopfart.Haupt,
                    aktiv = !v.laeuft,
                )
            }
            Knopf("Datenverarbeitung anzeigen", { textOffen = true }, art = Knopfart.Leise, breit = true)
        },
    ) {
        Etikett("Eine Frage noch")
        Text(
            text = "Damit ist der Funkverkehr mit den Bots und die Notrufgespräche mit der " +
                "Leitstelle gemeint — was du funkst und was der Bot antwortet. Daran " +
                "verbessern wir beides.",
            style = Schrift.Normal,
            color = Farben.TextLeise,
        )
        SehrLeise(
            "Freiwillig, jederzeit widerrufbar unter Konto → Datenschutzeinstellungen. Ein " +
                "Widerruf löscht auch das bereits Aufgezeichnete. Ohne Zustimmung funktioniert " +
                "das Spiel vollständig weiter.",
        )
        v.fehler?.let { Warnzeile(it) }
    }

    if (textOffen) Datenverarbeitungblende { textOffen = false }
}

/**
 * Die Zustimmung zu einer geänderten Fassung der Rechtstexte —
 * `RechtsstandDialog.vue`.
 *
 * <b>Vertagen gibt es nicht.</b> Ohne Zustimmung gilt das Spiel nicht mehr
 * (Nutzungsbedingungen Ziffer 11); der Dialog geht nur über Zustimmen oder
 * Abmelden weg. Die Texte schlagen sich in der Blende auf — die Einleitung weicht
 * dann, damit vom Gesetzestext mehr als drei Zeilen übrig bleiben.
 */
@Composable
fun Rechtsstandblende(
    laeuft: Boolean,
    fehler: String?,
    beiZustimmen: () -> Unit,
    beiAbmelden: () -> Unit,
) {
    var offen by rememberSaveable { mutableStateOf<String?>(null) }

    Blende(
        titel = "Wir haben unsere Rechtstexte geändert",
        beiSchliessen = {},
        breite = Dialogbreite.Breit,
        schliessenMoeglich = false,
        fussAlsSpalte = true,
        fuss = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf(
                    "Nutzungsbedingungen",
                    {
                        offen = if (offen == Rechtsstand.NUTZUNGSBEDINGUNGEN) null else Rechtsstand.NUTZUNGSBEDINGUNGEN
                    },
                    modifier = Modifier.weight(1f),
                    art = Knopfart.Leise,
                    kompakt = true,
                )
                Knopf(
                    "Datenschutzerklärung",
                    { offen = if (offen == Rechtsstand.DATENSCHUTZ) null else Rechtsstand.DATENSCHUTZ },
                    modifier = Modifier.weight(1f),
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
            if (fehler != null) Warnzeile(fehler)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf("Abmelden", beiAbmelden, modifier = Modifier.weight(1f), aktiv = !laeuft)
                Knopf(
                    "Zustimmen",
                    beiZustimmen,
                    modifier = Modifier.weight(1f),
                    art = Knopfart.Haupt,
                    aktiv = !laeuft,
                )
            }
        },
    ) {
        Etikett("Aktualisiert")
        val gerade = offen
        if (gerade == null) {
            RechtsstandEinleitung()
        } else {
            Text(
                text = Rechtsstand.TITEL[gerade].orEmpty(),
                style = Schrift.MonoKlein,
                color = Farben.Amber,
            )
            val (fertig, text) = rememberRechtstext(gerade)
            when {
                !fertig -> Ladezeile()
                text != null -> Rechtstext(text)
            }
        }
    }
}

/**
 * Die Einleitung zur aktuellen Fassung. Sie gehört zu `Rechtsstand.AKTUELL` und
 * muss mit ihm mitgezogen werden — eine Zusammenfassung, die etwas anderes
 * ankündigt als der Text darunter, ist schlimmer als keine.
 */
@Composable
private fun ColumnScope.RechtsstandEinleitung() {
    val absaetze = listOf(
        "**Neu: Zu jedem Konto gehört eine E-Mail-Adresse.** Wir verwenden sie, damit du " +
            "wieder hineinkommst, wenn du dein Passwort vergisst, und du kannst dich mit ihr " +
            "statt mit dem Benutzernamen anmelden. Werbung bekommst du nur, wenn du den " +
            "Newsletter ausdrücklich bestellst. Hast du noch keine hinterlegt, musst du das " +
            "gleich nachholen — ohne Adresse geht es nicht weiter (Datenschutzerklärung " +
            "Ziffern 2, 3 und 3b, Nutzungsbedingungen Ziffer 3).",
        "**Außerdem:** Deine gespeicherten Daten kannst du jetzt selbst als Datei " +
            "herunterladen (Datenschutzerklärung Ziffer 17), und wir beschreiben genauer, was " +
            "der Schutz gegen das Durchprobieren von Passwörtern sich merkt (Ziffer 12).",
        "Aus der vorigen Fassung, falls du sie noch nicht gesehen hast: Wir haben unsere " +
            "Rechtstexte gegen die geltenden Gesetze geprüft und an mehreren Stellen berichtigt. " +
            "Das Wichtigste:",
        "**Premium: Dein Widerrufsrecht bleibt.** Bisher stand in den AGB, es erlösche mit " +
            "der Freischaltung. Für ein laufendes Abo stimmt das nicht. Du kannst vierzehn Tage " +
            "lang widerrufen und zahlst dann nur den Anteil bis zum Widerruf. Neu gibt es dafür " +
            "die Schaltflächen **„Vertrag widerrufen\"** und **„Verträge hier kündigen\"**, auch " +
            "ohne Anmeldung. Löschst du dein Konto, endet ein laufendes Abo jetzt auch " +
            "tatsächlich bei Stripe. Einen neuen Preis gibt es nur mit deiner Zustimmung (AGB, " +
            "Ziffern 3 bis 5).",
        "**Die Datenschutzerklärung nennt jetzt alles, was bisher fehlte:** den KI-Funk mit " +
            "Mistral AI (Ziffer 8 c), Apple-Push in der iPhone-App (Ziffer 9), das Luftbild von " +
            "Esri (Ziffer 11), was unser Discord-Bot zeigt (Ziffer 11a), das Forum samt " +
            "Anmeldedienst (Ziffer 12), die Gerätekennung im Browser (Ziffer 10) und die " +
            "Erkennung automatisierter Spielweise in World als Profiling (Ziffer 20).",
        "**Minderjährige:** Wer noch nicht 18 ist, braucht für die Nutzung das Einverständnis " +
            "der Erziehungsberechtigten, für Premium deren Einwilligung (Nutzungsbedingungen, " +
            "Ziffer 3). Außerdem kann jeder, auch ohne Konto, rechtswidrige Inhalte melden " +
            "(Ziffer 9).",
    )
    absaetze.forEach { absatz ->
        Text(text = hervorgehoben(absatz), style = Schrift.Normal, color = Farben.TextLeise)
    }
    SehrLeise(Rechtsstand.ANSAGE)
    SehrLeise(
        "Ohne deine Zustimmung können wir dir das Spiel nicht weiter anbieten — zustimmen " +
            "oder abmelden. Dein Konto kannst du weiterhin jederzeit löschen: in den " +
            "Einstellungen oder, wenn du sie von hier aus nicht mehr erreichst, mit einer " +
            "E-Mail an ${Rechtsstand.SUPPORT_EMAIL}.",
    )
}

/**
 * Die Pflicht, eine E-Mail-Adresse zu hinterlegen — `EmailPflichtDialog.vue`.
 *
 * <b>Er sperrt aus, wie der Rechtsstand.</b> Ohne Adresse lässt der Server
 * weder in eine Runde noch in World; der einzige Weg daran vorbei ist
 * „Abmelden". <b>Zwei Schritte:</b> erst Adresse und Passwort, dann der Code —
 * der zweite lässt sich auf „Später" vertagen, denn die Pflicht ist mit dem
 * Eintragen erfüllt.
 */
@Composable
fun EmailPflichtblende(
    bestaetigungOffen: Boolean,
    beiSpeichern: suspend (String, String) -> Unit,
    beiBestaetigen: suspend (String) -> Unit,
    beiNeuerCode: suspend () -> Unit,
    beiSpaeter: () -> Unit,
    beiAbmelden: () -> Unit,
) {
    val v = rememberVorgang()
    val bereich = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var passwort by remember { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var hinweis by rememberSaveable { mutableStateOf<String?>(null) }

    Blende(
        titel = if (bestaetigungOffen) "Adresse bestätigen" else "Hinterlege deine E-Mail-Adresse",
        beiSchliessen = {},
        breite = Dialogbreite.Schmal,
        schliessenMoeglich = false,
        fuss = {
            if (!bestaetigungOffen) {
                Knopf("Abmelden", beiAbmelden, art = Knopfart.Leise, aktiv = !v.laeuft)
                Knopf(
                    "Adresse speichern",
                    {
                        if (email.isBlank() || passwort.isEmpty()) {
                            v.fehler = "Bitte Adresse und Passwort angeben."
                        } else {
                            val adresse = email.trim()
                            bereich.vorgang(v) {
                                beiSpeichern(adresse, passwort)
                                passwort = ""
                                hinweis = "Wir haben einen sechsstelligen Code an $adresse geschickt."
                            }
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = !v.laeuft,
                )
            } else {
                Knopf(
                    "Nichts angekommen",
                    {
                        bereich.vorgang(v) {
                            beiNeuerCode()
                            hinweis = "Ein neuer Code ist unterwegs."
                        }
                    },
                    art = Knopfart.Leise,
                    aktiv = !v.laeuft,
                )
                Knopf("Später", beiSpaeter, aktiv = !v.laeuft)
                Knopf(
                    "Bestätigen",
                    { bereich.vorgang(v) { beiBestaetigen(code.trim()) } },
                    art = Knopfart.Haupt,
                    aktiv = !v.laeuft,
                )
            }
        },
    ) {
        Etikett(if (bestaetigungOffen) "Fast geschafft" else "Neu")
        if (!bestaetigungOffen) {
            Text(
                text = "Zu jedem Konto gehört jetzt eine E-Mail-Adresse — ohne sie kannst du nicht " +
                    "weiterspielen. Wir nutzen sie nur für zwei Dinge: damit du wieder hineinkommst, " +
                    "wenn du dein Passwort vergisst, und für den Newsletter, aber nur, wenn du ihn " +
                    "ausdrücklich bestellst.",
                style = Schrift.Normal,
                color = Farben.TextLeise,
            )
            Feld(
                wert = email,
                beiAenderung = { email = it.take(254) },
                etikett = "E-Mail-Adresse",
                platzhalter = "kim@beispiel.de",
                tastatur = KeyboardType.Email,
            )
            Feld(
                wert = passwort,
                beiAenderung = { passwort = it },
                etikett = "Dein Passwort zur Bestätigung",
                geheim = true,
                weiterTaste = ImeAction.Done,
            )
        } else {
            hinweis?.let { Erfolgszeile(it) }
            SehrLeise(
                "Erst eine bestätigte Adresse hilft beim vergessenen Passwort. Du kannst das auch " +
                    "später in den Kontoeinstellungen erledigen.",
            )
            Codefeld(wert = code, beiAenderung = { code = it.filter(Char::isDigit) }, etikett = "Code aus der E-Mail")
        }
        v.fehler?.let { Warnzeile(it) }
    }
}

// ------------------------------------------------------- Übertragung

/**
 * Die Frage vor dem Betreten einer übertragenen Schicht — `StreamerDialog.vue`.
 *
 * <b>Für die Runde gedacht.</b> Aufgerufen wird sie von dort, wo der Beitritt
 * mit `ok: false` und `einwilligung` abgewiesen wurde, oder wo der eigene Platz
 * nachträglich gefragt wird (Streamer-Modus eingeschaltet, während man sitzt):
 *
 * ```kotlin
 * frage?.let { f ->
 *     Streamerblende(
 *         frage = f,
 *         wege = sitzung.kontowege,
 *         kennung = konto.kennung,
 *         beiEingewilligt = { runde.beitreten(code, name) },   // oder: StreamerfreigabeNachtragen
 *         beiAbgelehnt = { frage = null },                     // wer schon saß: runde.verlassen()
 *     )
 * }
 * ```
 *
 * <b>Zwei Zustände, ein Dialog.</b> Nach der Antwort eines Minderjährigen bleibt
 * er offen und zeigt den Bogen für die Erziehungsberechtigten — der Vorgang ist
 * nicht abgeschlossen, sondern weitergereicht. „Ist er schon da?" fragt denselben
 * Weg noch einmal; erst wenn die Einwilligung gilt, kommt [beiEingewilligt].
 *
 * <b>Beide Knöpfe gleich groß.</b> Eine Einwilligung, bei der das „Ja" leuchtet
 * und das „Nein" ein grauer Link ist, ist keine freie Entscheidung. Und: kein
 * Zurück — wer nicht einwilligen will, drückt „Nicht einwilligen".
 */
@Composable
fun Streamerblende(
    frage: Einwilligungsbedarf,
    wege: Kontowege,
    kennung: String,
    beiEingewilligt: () -> Unit,
    beiAbgelehnt: () -> Unit,
) {
    val zusammenhang = LocalContext.current
    val zwischenablage = LocalClipboardManager.current
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()

    // Drei Zustände und nicht zwei: `null` heißt „noch nicht geantwortet".
    var volljaehrig by remember(frage.raumCode) { mutableStateOf<Boolean?>(null) }
    var gelesen by remember(frage.raumCode) { mutableStateOf(false) }
    // Steht ein Bogen, ist die Frage weitergereicht und nicht mehr zu beantworten.
    // Leer (aber nicht `null`) heißt: Es wartet, nur ohne Link.
    var elternbogen by remember(frage.raumCode) { mutableStateOf(frage.elternbogen) }
    var kopiert by remember { mutableStateOf(false) }

    val wohin = "${plattformname(frage.plattform)} · ${frage.kanal}"
    val wartetAufEltern = elternbogen != null
    val bereit = volljaehrig != null && gelesen && !v.laeuft

    Blende(
        titel = "Diese Schicht wird übertragen",
        beiSchliessen = {},
        breite = Dialogbreite.Breit,
        schliessenMoeglich = false,
        fuss = {
            Knopf("Nicht einwilligen", beiAbgelehnt, aktiv = !v.laeuft)
            if (!wartetAufEltern) {
                Knopf(
                    if (v.laeuft) "Wird gespeichert…" else "Einwilligen und beitreten",
                    {
                        val alt = volljaehrig == true
                        bereich.vorgang(v, "Das hat gerade nicht geklappt.") {
                            val erteilt = wege.einwilligungErteilen(kennung, frage.raumCode, alt)
                            if (erteilt.gilt) {
                                beiEingewilligt()
                            } else {
                                elternbogen = erteilt.elternbogen ?: ""
                            }
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = bereit,
                )
            }
        },
    ) {
        Etikett("Einwilligung")

        // Der Steckbrief zuerst: Er ist der Gegenstand der Einwilligung.
        Kasten(abstandInnen = Abstand.Klein) {
            Steckbriefzeile("Wer überträgt", frage.streamerName)
            Steckbriefzeile("Wohin", wohin)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SehrLeise("Bleibt es stehen?", modifier = Modifier.weight(1f))
                Marke(
                    text = if (frage.aufzeichnung) "Ja — wird aufgezeichnet" else "Nein — nur live",
                    farbe = if (frage.aufzeichnung) Farben.SignalHell else Farben.GruenHell,
                )
            }
        }

        if (wartetAufEltern) {
            Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
                Text(
                    text = hervorgehoben(
                        "**Jetzt sind deine Eltern dran.** Gib diesen Link an einen " +
                            "Erziehungsberechtigten weiter. Er füllt dort einen kurzen Bogen aus und " +
                            "unterschreibt. Sobald er eingegangen ist, kommst du in die Runde.",
                    ),
                    style = Schrift.Normal,
                    color = Farben.TextLeise,
                )
                val link = elternbogen.orEmpty()
                if (link.isNotBlank()) {
                    Text(text = link, style = Schrift.MonoKlein, color = Farben.BlauHell)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    if (link.isNotBlank()) {
                        Knopf(
                            if (kopiert) "Kopiert" else "Link kopieren",
                            {
                                zwischenablage.setText(AnnotatedString(link))
                                kopiert = true
                            },
                            kompakt = true,
                        )
                        Knopf("Teilen", { textTeilen(zusammenhang, link, "Bogen für die Erziehungsberechtigten") }, kompakt = true)
                    }
                    Knopf(
                        "Ist er schon da?",
                        {
                            bereich.vorgang(v) {
                                val stand = wege.einwilligungErteilen(kennung, frage.raumCode, false)
                                if (stand.gilt) {
                                    beiEingewilligt()
                                } else {
                                    v.fehler = "Der Bogen ist noch nicht eingegangen."
                                }
                            }
                        },
                        kompakt = true,
                        aktiv = !v.laeuft,
                    )
                }
                SehrLeise(
                    "Der Link gilt für diesen einen Vorgang. Du findest ihn außerdem jederzeit unter " +
                        "„Konto → Privatsphäre → Übertragungen\".",
                )
            }
        } else {
            val (fertig, text) = rememberRechtstext(Rechtsstand.UEBERTRAGUNG)
            when {
                !fertig -> Ladezeile()
                // Die Fassungszeile hängt nur an der Leseseite; hier steht sie unten
                // im Fuß — sonst stünde sie zweimal da.
                text != null -> Rechtstext(
                    text.lines().filterNot { it.startsWith("Stand: Fassung") }.joinToString("\n"),
                )
            }

            Etikett("Wie alt bist du?")
            Hakenzeile(
                text = "Ich bin ${frage.mindestalterAllein} oder älter",
                an = volljaehrig == true,
                beiWechsel = { volljaehrig = true },
            )
            Hakenzeile(
                text = "Ich bin jünger — dann brauchen wir noch die Unterschrift eines " +
                    "Erziehungsberechtigten; du bekommst danach einen Link dafür.",
                an = volljaehrig == false,
                beiWechsel = { volljaehrig = false },
            )

            Hakenzeile(
                text = hervorgehoben(
                    "Ich habe den Text gelesen und willige ein, dass mein Name, meine Stimme und " +
                        "was ich in dieser Runde schreibe über **$wohin** öffentlich übertragen " +
                        (if (frage.aufzeichnung) "und aufgezeichnet " else "") + "werden.",
                ),
                an = gelesen,
                beiWechsel = { gelesen = it },
            )
        }

        v.fehler?.let { Warnzeile(it) }
        SehrLeise(
            "Fassung ${Rechtsstand.UEBERTRAGUNG_FASSUNG} · nachzulesen unter „Übertragung einer Schicht\"",
            mono = true,
        )
    }
}

@Composable
private fun Steckbriefzeile(wer: String, was: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
        SehrLeise(wer, modifier = Modifier.weight(0.4f))
        Text(was, style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(0.6f))
    }
}
