package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import de.pagerspass.pagerspass.netz.UEBERTRAGUNG_TEXT
import de.pagerspass.pagerspass.netz.Uebertragungsfrage
import de.pagerspass.pagerspass.netz.Uebertragungszusage
import de.pagerspass.pagerspass.netz.plattformname
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.launch

/**
 * Die Einwilligung in eine Übertragung — `StreamerDialog.vue`.
 *
 * <b>Eigene Datei, mit Absicht.</b> Der Bereich Konto baut eine eigene
 * Einwilligungsansicht für die Einstellungen; wer beide zusammenlegt, findet
 * diese hier vollständig und ohne Abhängigkeit zu ihr.
 *
 * <b>Sie lässt sich nicht wegwischen.</b> Wegwischen wäre weder ja noch nein —
 * und der Beitritt hinge in der Luft. Es gibt genau zwei Knöpfe.
 *
 * @param beiEinwilligen `Runde.einwilligungErteilen` — wirft mit lesbarer Meldung.
 * @param beiAblehnen `Runde.einwilligungAblehnen`.
 */
@Composable
fun Uebertragungsblende(
    frage: Uebertragungsfrage,
    beiEinwilligen: suspend (Boolean) -> Uebertragungszusage,
    beiAblehnen: () -> Unit,
) {
    val lauf = rememberCoroutineScope()
    val zwischenablage = LocalClipboardManager.current
    var volljaehrig by remember { mutableStateOf<Boolean?>(null) }
    var gelesen by remember { mutableStateOf(false) }
    var laeuft by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var bogenFehlt by remember { mutableStateOf(false) }
    var kopiert by remember { mutableStateOf(false) }
    val alter = frage.mindestalterAllein

    fun einwilligen(ja: Boolean) {
        if (laeuft) return
        laeuft = true
        fehler = null
        bogenFehlt = false
        lauf.launch {
            runCatching { beiEinwilligen(ja) }
                .onSuccess { zusage -> if (!zusage.gilt && frage.wartetAufEltern) bogenFehlt = true }
                .onFailure { fehler = it.message ?: "Die Einwilligung ließ sich nicht speichern." }
            laeuft = false
        }
    }

    Blende(
        titel = if (frage.wartetAufEltern) "Einwilligung" else "Diese Schicht wird übertragen",
        beiSchliessen = {},
        schliessenMoeglich = false,
        fussAlsSpalte = true,
        fuss = {
            if (!frage.wartetAufEltern) {
                Knopf(
                    aufschrift = if (laeuft) "Wird gespeichert…" else "Einwilligen und beitreten",
                    beiDruck = { volljaehrig?.let { einwilligen(it) } },
                    art = Knopfart.Haupt,
                    aktiv = !laeuft && volljaehrig != null && gelesen,
                    breit = true,
                )
            }
            Knopf("Nicht einwilligen", beiAblehnen, art = Knopfart.Leise, breit = true, aktiv = !laeuft)
        },
    ) {
        Etikett("Wer überträgt")
        Text(frage.streamerName.ifBlank { "Die Leitstelle" }, style = Schrift.Normal, color = Farben.Text)
        Etikett("Wohin")
        Text(
            "${plattformname(frage.plattform)} · ${frage.kanal}",
            style = Schrift.Normal,
            color = Farben.Text,
        )
        Etikett("Bleibt es stehen?")
        Text(
            if (frage.aufzeichnung) {
                "Ja — die Übertragung wird aufgezeichnet und bleibt abrufbar."
            } else {
                "Nein — nur live, ohne Aufzeichnung."
            },
            style = Schrift.Normal,
            color = Farben.Text,
        )

        if (frage.wartetAufEltern) {
            Etikett("Jetzt sind deine Eltern dran.")
            Text(
                "Du bist jünger als $alter. Damit die Einwilligung gilt, müssen deine " +
                    "Erziehungsberechtigten zustimmen — über den Bogen hinter diesem Link.",
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
            frage.elternbogen?.let { bogen ->
                SehrLeise(bogen, mono = true)
                Knopf(
                    if (kopiert) "Kopiert ✓" else "Link kopieren",
                    {
                        zwischenablage.setText(AnnotatedString(bogen))
                        kopiert = true
                    },
                    kompakt = true,
                )
            }
            Knopf(
                if (laeuft) "Wird nachgesehen…" else "Ist er schon da?",
                { einwilligen(false) },
                art = Knopfart.Normal,
                aktiv = !laeuft,
            )
            if (bogenFehlt) {
                Text("Der Bogen ist noch nicht eingegangen.", style = Schrift.Klein, color = Farben.AmberHell)
            }
        } else {
            Wortlaut()
            Etikett("Dein Alter")
            Pillenreihe {
                Pille(aufschrift = "Ich bin $alter oder älter", an = volljaehrig == true, beiDruck = { volljaehrig = true })
                Pille(aufschrift = "Ich bin jünger als $alter", an = volljaehrig == false, beiDruck = { volljaehrig = false })
            }
            if (volljaehrig == false) {
                SehrLeise(
                    "Dann kommen deine Erziehungsberechtigten dazu: Nach dem Speichern bekommst du " +
                        "einen Link für sie. Erst wenn sie zugestimmt haben, fährst du mit.",
                )
            }
            Hakenzeile(
                text = "Ich habe gelesen, was übertragen wird, und willige ein, dass mein Spielname, " +
                    "meine Funksprüche und mein Platz in dieser Runde gezeigt werden.",
                an = gelesen,
                beiWechsel = { gelesen = it },
            )
        }

        fehler?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
        SehrLeise("Fassung ${frage.fassung.ifBlank { "—" }} · nachzulesen unter „Übertragung einer Schicht“")
    }
}

/** Der Einwilligungstext — `## ` Zwischenüberschrift, `- ` Punkt, `**…**` fett. */
@Composable
private fun Wortlaut() {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.padding(vertical = Abstand.Klein),
    ) {
        UEBERTRAGUNG_TEXT.trim().split("\n\n").forEach { absatz ->
            val zeilen = absatz.trim()
            when {
                zeilen.startsWith("## ") -> Etikett(zeilen.removePrefix("## "))
                zeilen.lines().all { it.trimStart().startsWith("- ") } -> zeilen.lines().forEach { punkt ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text("•", style = Schrift.Klein, color = Farben.TextLeise)
                        Text(fett(punkt.trimStart().removePrefix("- ")), style = Schrift.Klein, color = Farben.TextLeise)
                    }
                }
                else -> Text(fett(zeilen.replace('\n', ' ')), style = Schrift.Klein, color = Farben.TextLeise)
            }
        }
    }
}

private fun fett(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { i, teil ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Farben.Text)) { append(teil) } else append(teil)
    }
}
