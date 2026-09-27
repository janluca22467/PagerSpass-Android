package de.pagerspass.pagerspass.ansichten

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Was die Seiten des Kontobereichs gemeinsam brauchen.
 *
 * <b>Die Seiten halten ihre Vorgänge selbst</b> — wie die Ansichten im Web, wo
 * jede Karte der Kontoseite ihr eigenes `laeuft`, `meldung` und `erfolg` hat. Ein
 * Fehler beim Werbecode soll nicht unter der Passwortänderung stehen, und ein
 * laufender Newsletter-Knopf nicht den Anmeldeschutz sperren. Deshalb ein
 * [Vorgang] je Karte statt eines Zustands für die ganze Seite.
 */

/**
 * Ein Vorgang auf einer Seite: ob er läuft, was schiefging, was gelang.
 *
 * Er ist ein Zustandshalter für Compose — die drei Felder sind beobachtbar und
 * zeichnen die Karte neu, sobald sie sich ändern.
 */
class Vorgang {
    var laeuft by mutableStateOf(false)
    var fehler by mutableStateOf<String?>(null)
    var erfolg by mutableStateOf<String?>(null)

    fun leeren() {
        fehler = null
        erfolg = null
    }
}

@Composable
fun rememberVorgang(): Vorgang = remember { Vorgang() }

/**
 * Einen Vorgang ausführen — mit Sperre gegen den zweiten, ungeduldigen Druck.
 *
 * Der Fehlertext ist der Satz des Servers (siehe `Netzfehler`); fehlt er, steht
 * [rueckfall] da. Ein Abbruch der Koroutine (Seite verlassen) ist kein Fehler
 * und wird weitergereicht.
 */
fun CoroutineScope.vorgang(
    v: Vorgang,
    rueckfall: String = "Das hat nicht geklappt.",
    tat: suspend () -> Unit,
) {
    if (v.laeuft) return
    v.laeuft = true
    v.leeren()
    launch {
        try {
            tat()
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (e: Exception) {
            v.fehler = e.message ?: rueckfall
        } finally {
            v.laeuft = false
        }
    }
}

/** Die Warnung unter einer Karte — in Monospace und Signalrot, wie `.warnung` im Web. */
@Composable
fun Warnzeile(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = Schrift.MonoKlein, color = Farben.SignalHell, modifier = modifier)
}

/** Die grüne Auskunft — `.erfolg` im Web: „Gespeichert.", „Deine Adresse ist bestätigt." */
@Composable
fun Erfolgszeile(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = Schrift.Klein, color = Farben.GruenHell, modifier = modifier)
}

/** Fehler und Erfolg eines Vorgangs, untereinander — was davon gerade gilt. */
@Composable
fun Vorgangszeilen(v: Vorgang) {
    v.erfolg?.let { Erfolgszeile(it) }
    v.fehler?.let { Warnzeile(it) }
}

// ------------------------------------------------------------- Dateien

/**
 * Eine Datei speichern — über den Dateidialog des Systems.
 *
 * <b>Warum der Dialog und kein fester Ordner.</b> Seit Android 10 schreibt eine
 * App nicht mehr einfach in „Downloads"; der Dialog („Speichern unter …") ist der
 * Weg, der ohne jede Berechtigung auskommt — und der Nutzer sieht, wo die Datei
 * landet. Im Web tut das der Download-Link.
 *
 * Zurück kommt eine Funktion `(dateiname, inhalt) -> Unit`: Sie merkt sich den
 * Inhalt, öffnet den Dialog und schreibt, sobald ein Ziel gewählt ist.
 */
@Composable
fun rememberDateiSpeichern(inhaltstyp: String): (String, String) -> Unit {
    val zusammenhang = LocalContext.current
    var wartend by remember { mutableStateOf<String?>(null) }

    val dialog = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(inhaltstyp),
    ) { ziel: Uri? ->
        val inhalt = wartend
        wartend = null
        if (ziel == null || inhalt == null) return@rememberLauncherForActivityResult

        val geschrieben = runCatching {
            zusammenhang.contentResolver.openOutputStream(ziel)?.use {
                it.write(inhalt.toByteArray(Charsets.UTF_8))
            } != null
        }.getOrDefault(false)

        Toast.makeText(
            zusammenhang,
            if (geschrieben) "Gespeichert." else "Die Datei ließ sich nicht schreiben.",
            Toast.LENGTH_SHORT,
        ).show()
    }

    return remember(dialog) {
        { dateiname: String, inhalt: String ->
            wartend = inhalt
            try {
                dialog.launch(dateiname)
            } catch (_: ActivityNotFoundException) {
                wartend = null
                Toast.makeText(zusammenhang, "Auf diesem Gerät gibt es keinen Dateidialog.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}

/** Einen Text über das Teilen-Blatt weitergeben — Mail, Messenger, Notizen. */
fun textTeilen(zusammenhang: Context, text: String, betreff: String? = null) {
    val absicht = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (betreff != null) putExtra(Intent.EXTRA_SUBJECT, betreff)
    }
    try {
        zusammenhang.startActivity(Intent.createChooser(absicht, betreff ?: "Teilen"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(zusammenhang, "Auf diesem Gerät lässt sich nichts teilen.", Toast.LENGTH_SHORT).show()
    }
}

/** Eine E-Mail beginnen — `mailto:` mit Betreff, wie der Knopf im Web. */
fun mailSchreiben(zusammenhang: Context, an: String, betreff: String) {
    val absicht = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$an?subject=${Uri.encode(betreff)}"))
    try {
        zusammenhang.startActivity(absicht)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(zusammenhang, "Kein E-Mail-Programm gefunden — schreib an $an.", Toast.LENGTH_LONG)
            .show()
    }
}

/** Eine Adresse im Browser öffnen — und sagen, wenn es keinen gibt. */
fun imBrowser(zusammenhang: Context, adresse: String) {
    try {
        zusammenhang.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adresse)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(zusammenhang, "Kein Browser gefunden.", Toast.LENGTH_SHORT).show()
    }
}

// ----------------------------------------------------------------- Auswahl

/**
 * Eine Auswahl aus einer langen Liste — das `<select>` des Web.
 *
 * Kurze Listen stehen als Pillen in der Seite; diese Blende ist für die langen
 * (rund 400 Kreise) und hat deshalb ein Suchfeld oben.
 */
@Composable
fun <T> Auswahlblende(
    titel: String,
    eintraege: List<T>,
    aufschrift: (T) -> String,
    beiWahl: (T) -> Unit,
    beiSchliessen: () -> Unit,
    unterzeile: ((T) -> String?)? = null,
    suche: Boolean = eintraege.size > 12,
) {
    var filter by remember { mutableStateOf("") }
    val gezeigt = if (filter.isBlank()) {
        eintraege
    } else {
        eintraege.filter { aufschrift(it).contains(filter.trim(), ignoreCase = true) }
    }

    Blende(
        titel = titel,
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Normal,
        kopfknoepfe = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
    ) {
        if (suche) {
            Feld(wert = filter, beiAenderung = { filter = it }, platzhalter = "Suchen …")
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Höchstens 120 Zeilen auf einmal: Eine Liste von 400 Wegzeilen baut
            // die Blende spürbar langsamer auf, und wer so weit rollt, sucht besser.
            gezeigt.take(120).forEach { eintrag ->
                Wegzeile(
                    titel = aufschrift(eintrag),
                    beiDruck = { beiWahl(eintrag) },
                    unterzeile = unterzeile?.invoke(eintrag),
                )
            }
            if (gezeigt.size > 120) SehrLeise("… und ${gezeigt.size - 120} weitere — grenz die Suche ein.")
            if (gezeigt.isEmpty()) SehrLeise("Nichts gefunden.")
        }
    }
}
