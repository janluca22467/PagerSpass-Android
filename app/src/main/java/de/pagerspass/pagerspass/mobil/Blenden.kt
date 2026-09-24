package de.pagerspass.pagerspass.mobil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.netz.Gluecksradfeld
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.ui.bausteine.Blende
import androidx.compose.ui.text.input.ImeAction
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Blenden, die vor allem anderen stehen.
 *
 * Zwei davon gibt es bisher: der Wiederherstellungscode direkt nach der
 * Kontoanlage und die Zustimmung zu einer neuen Fassung der Rechtstexte. Beide
 * liegen auf der obersten Ebene der Staffel („muss beantwortet sein, bevor
 * überhaupt etwas geht") und beide lassen sich **nicht** durch einen Druck
 * daneben schließen.
 */

/**
 * Der Wiederherstellungscode — einmal und nie wieder.
 *
 * <b>Er steht in genau einer Antwort des Servers</b>, der auf die Kontoanlage.
 * Wer diesen Bildschirm wegtippt, ohne ihn abzuschreiben, bekommt ihn nicht
 * zurück — deshalb hat die Blende keinen Weg daran vorbei außer dem Knopf, und
 * der Knopf sagt, was er bestätigt.
 *
 * Der Code steht in Monospace und gesperrt: Er wird abgeschrieben, und dabei
 * zählt jedes Zeichen einzeln. Ein Kopierknopf steht daneben, weil das die
 * ehrlichere Antwort auf „abschreiben" ist.
 */
@Composable
fun Wiederherstellungscode(code: String, beiVerstanden: () -> Unit) {
    val zwischenablage = LocalClipboardManager.current

    Blende(
        titel = "Dein Wiederherstellungscode",
        beiSchliessen = {},
        breite = Dialogbreite.Schmal,
        schliessenMoeglich = false,
        fuss = {
            Knopf("Ich habe ihn notiert", beiVerstanden, art = Knopfart.Haupt, breit = true)
        },
    ) {
        Text(
            text = "Mit ihm kommst du wieder an dein Konto, wenn du dein Passwort vergisst. " +
                "Wir zeigen ihn genau einmal — schreib ihn jetzt auf.",
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )

        Text(
            text = code,
            style = Schrift.MonoNormal.copy(fontSize = Schrift.TITEL, letterSpacing = 0.14.em),
            color = Farben.Amber,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.BgTief, Rundung.Klein)
                .border(1.dp, Farben.AmberTief, Rundung.Klein)
                .padding(Abstand.Normal),
        )

        Textweg("In die Zwischenablage kopieren", {
            zwischenablage.setText(AnnotatedString(code))
        })
    }
}

/**
 * Die Zustimmung zu einer neuen Fassung der Rechtstexte.
 *
 * <b>Zwei Antworten, und sie stehen untereinander.</b> Nebeneinander stünden
 * dort zwei gleich aussehende Knöpfe mit gegenteiliger Bedeutung — deshalb die
 * Spaltenfassung des Dialogfußes. Wer nicht zustimmen will, wird abgemeldet;
 * das ist keine Drohung, sondern die einzige ehrliche Alternative, denn ohne
 * Zustimmung gibt es keine Grundlage für den Betrieb des Kontos.
 *
 * <b>Sie lässt sich nicht wegtippen.</b> Eine Zustimmung, die man durch einen
 * Druck daneben umgeht, ist keine.
 */
@Composable
fun RechtsstandBlende(beiZustimmen: () -> Unit, beiAbmelden: () -> Unit) {
    val browser = LocalUriHandler.current

    Blende(
        titel = "Unsere Bedingungen haben sich geändert",
        beiSchliessen = {},
        breite = Dialogbreite.Normal,
        schliessenMoeglich = false,
        fussAlsSpalte = true,
        fuss = {
            Knopf("Zustimmen und weiter", beiZustimmen, art = Knopfart.Haupt, breit = true)
            Knopf("Ablehnen und abmelden", beiAbmelden, art = Knopfart.Gefahr, breit = true)
        },
    ) {
        Text(
            text = "Um weiterzuspielen, musst du der neuen Fassung zustimmen. " +
                "Lies sie dir bitte durch — sie öffnet sich im Browser.",
            style = Schrift.Normal,
            color = Farben.TextLeise,
        )

        Textweg("Nutzungsbedingungen", {
            browser.openUri(Rechtsstand.adresse(SERVERWEG, Rechtsstand.NUTZUNGSBEDINGUNGEN))
        })
        Textweg("Allgemeine Geschäftsbedingungen", {
            browser.openUri(Rechtsstand.adresse(SERVERWEG, Rechtsstand.AGB))
        })
        Textweg("Datenschutzerklärung", {
            browser.openUri(Rechtsstand.adresse(SERVERWEG, Rechtsstand.DATENSCHUTZ))
        })

        Text(text = Rechtsstand.ANSAGE, style = Schrift.Winzig, color = Farben.TextSehrLeise)
    }
}

/**
 * Wo die Rechtstexte zu lesen sind.
 *
 * <b>Ausdrücklich der Betrieb und nicht der eingestellte Server.</b> Wer beim
 * Entwickeln gegen `localhost` arbeitet, hat dort keine Rechtstexte liegen —
 * und ein toter Link ist an dieser Stelle schlechter als ein Link, der auf die
 * gültige Fassung zeigt. Sobald die App die Texte selbst zeigt (offener Punkt
 * vor der Veröffentlichung), entfällt diese Zeile.
 */
private const val SERVERWEG = de.pagerspass.pagerspass.netz.Server.BETRIEB

/**
 * Konto löschen — die Rückfrage mit dem Passwort.
 *
 * <b>Das Passwort ist hier kein Formalismus.</b> Der Server verlangt es, und das
 * aus gutem Grund: Ein Gerät, das jemand kurz aus der Hand gibt, ist angemeldet.
 * Ohne diese Frage wäre „Konto löschen" zwei Tipps entfernt und unwiderruflich.
 *
 * <b>Die Antworten stehen untereinander.</b> Nebeneinander stünden zwei gleich
 * aussehende Knöpfe mit gegenteiliger Bedeutung — und der falsche davon löscht.
 */
@Composable
fun Kontoloeschung(
    laeuft: Boolean,
    beiLoeschen: (String) -> Unit,
    beiSchliessen: () -> Unit,
) {
    var passwort by remember { mutableStateOf("") }

    Blende(
        titel = "Konto dauerhaft löschen",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fussAlsSpalte = true,
        fuss = {
            Knopf(
                aufschrift = "Endgültig löschen",
                beiDruck = { beiLoeschen(passwort) },
                art = Knopfart.Alarm,
                breit = true,
                aktiv = !laeuft && passwort.isNotBlank(),
            )
            Knopf("Abbrechen", beiSchliessen, art = Knopfart.Leise, breit = true)
        },
    ) {
        Text(
            text = "Dein Konto, dein Dienstbuch, deine Garage und deine Freundesliste werden " +
                "entfernt. Das lässt sich nicht rückgängig machen.",
            style = Schrift.Normal,
            color = Farben.TextLeise,
        )
        Feld(
            wert = passwort,
            beiAenderung = { passwort = it },
            etikett = "Zur Bestätigung: dein Passwort",
            geheim = true,
            weiterTaste = ImeAction.Done,
        )
    }
}

/**
 * Was der Tagesbonus ausgeworfen hat.
 *
 * <b>Er bekommt eine Blende und keine Zeile.</b> Ein Gewinn, der als Meldung am
 * Bildschirmrand erscheint und nach drei Sekunden verschwindet, ist kein
 * Ereignis, sondern eine Nebenbemerkung — und der Tagesbonus ist der eine
 * Grund, jeden Tag hereinzuschauen.
 */
@Composable
fun Bonusblende(gewinn: Gluecksradfeld, beiSchliessen: () -> Unit) {
    Blende(
        titel = "Tagesbonus",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = { Knopf("Danke", beiSchliessen, art = Knopfart.Haupt, breit = true) },
    ) {
        Text(
            text = gewinn.text.ifBlank { "Etwas ist dabei herausgekommen." },
            style = Schrift.MonoNormal.copy(fontSize = Schrift.TITEL),
            color = Farben.Amber,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.BgTief, Rundung.Klein)
                .border(1.dp, Farben.AmberTief, Rundung.Klein)
                .padding(Abstand.Gross),
        )
        Text(
            text = "Komm morgen wieder — die Serie zählt weiter.",
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
    }
}

/**
 * Eine Verwarnung der Verwaltung — bleibt stehen, bis sie bestätigt ist.
 *
 * Der Server liefert nur die unbestätigten; „Zur Kenntnis genommen" räumt sie
 * dort und hier weg. Nicht wegzutippen — das ist der Sinn.
 */
@Composable
fun Verwarnungsblende(
    verwarnung: de.pagerspass.pagerspass.netz.Verwarnung,
    beiBestaetigen: () -> Unit,
) {
    Blende(
        titel = "Verwarnung",
        beiSchliessen = {},
        schliessenMoeglich = false,
        fuss = {
            de.pagerspass.pagerspass.ui.bausteine.Knopf(
                aufschrift = "Zur Kenntnis genommen",
                beiDruck = beiBestaetigen,
                art = de.pagerspass.pagerspass.ui.bausteine.Knopfart.Haupt,
            )
        },
    ) {
        Text(
            text = verwarnung.grund,
            style = Schrift.Normal,
            color = Farben.Text,
        )
        Text(
            text = "ausgesprochen von ${verwarnung.von}",
            style = Schrift.Klein,
            color = Farben.TextSehrLeise,
        )
    }
}

/** Eine direkte Nachricht der Verwaltung — dasselbe Muster, freundlicherer Ton. */
@Composable
fun AdminNachrichtblende(
    nachricht: de.pagerspass.pagerspass.netz.AdminNachricht,
    beiBestaetigen: () -> Unit,
) {
    Blende(
        titel = nachricht.titel.ifBlank { "Nachricht der Verwaltung" },
        beiSchliessen = {},
        schliessenMoeglich = false,
        fuss = {
            de.pagerspass.pagerspass.ui.bausteine.Knopf(
                aufschrift = "Gelesen",
                beiDruck = beiBestaetigen,
                art = de.pagerspass.pagerspass.ui.bausteine.Knopfart.Haupt,
            )
        },
    ) {
        Text(text = nachricht.text, style = Schrift.Normal, color = Farben.Text)
        Text(
            text = "von ${nachricht.von}",
            style = Schrift.Klein,
            color = Farben.TextSehrLeise,
        )
    }
}
