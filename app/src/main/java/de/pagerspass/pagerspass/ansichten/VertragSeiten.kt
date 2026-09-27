package de.pagerspass.pagerspass.ansichten

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Erklaerungsquittung
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kontowege
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Schrift
import java.time.LocalDate

/**
 * „Verträge hier kündigen" (§ 312k BGB) und „Vertrag widerrufen" (§ 356a BGB) —
 * `VertragKuendigenView.vue` und `VertragWiderrufenView.vue`.
 *
 * <b>Ohne Anmeldung.</b> Beide Schaltflächen müssen „unmittelbar und leicht
 * zugänglich" sein; wer kündigen will, hat sein Passwort oft gerade nicht zur
 * Hand. Ist jemand angemeldet, stehen Name und Benutzername schon da — verlangt
 * wird das Konto nicht.
 *
 * <b>Die Aufschriften sind vorgeschrieben:</b> „jetzt kündigen" und „Widerruf
 * bestätigen", wörtlich.
 *
 * <b>Was nicht hier steht:</b> ob es den Benutzernamen gibt oder ob er Premium
 * hat. Sonst wäre die Seite ein Verzeichnis zahlender Konten.
 */
@Composable
fun VertragKuendigenSeite(
    wege: Kontowege,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    beiZurueck: () -> Unit = {},
    beiZumSpiel: () -> Unit = {},
    beiWiderrufen: () -> Unit = {},
) {
    val zusammenhang = LocalContext.current
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()

    var art by rememberSaveable { mutableStateOf("ordentlich") }
    var grund by rememberSaveable { mutableStateOf("") }
    var zumDatum by rememberSaveable { mutableStateOf(false) }
    var datum by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var benutzername by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var quittung by rememberSaveable(stateSaver = QuittungSpeicher) { mutableStateOf<Erklaerungsquittung?>(null) }

    // Vorbelegen, wenn ein Konto da ist — und still, wenn nicht.
    LaunchedEffect(konto?.kennung) {
        if (konto != null) {
            if (benutzername.isBlank()) benutzername = konto.benutzername
            if (name.isBlank()) name = konto.anzeigename
        }
    }

    fun fehlt(): String? = when {
        art == "ausserordentlich" && grund.isBlank() -> "Für eine außerordentliche Kündigung brauchen wir den Grund."
        zumDatum && datum.isBlank() -> "Bitte wähle das gewünschte Datum."
        name.trim().length < 2 -> "Bitte gib deinen Namen an."
        benutzername.isBlank() -> "Bitte gib deinen Benutzernamen an."
        '@' !in email -> "Bitte gib die E-Mail-Adresse für die Bestätigung an."
        else -> null
    }

    Seite(modifier = modifier, unterrand = unterrand, breite = Mass.Lesebreite) {
        Seitenkopf(
            titel = "Verträge hier kündigen",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        val q = quittung
        if (q != null) {
            Quittung(q, beiZumSpiel)
            return@Seite
        }

        Kasten {
            Leise(
                "Hier kündigst du dein Abo für PagerSpass Premium. Eine Anmeldung brauchst du " +
                    "dafür nicht. Nach dem Absenden bekommst du sofort eine Bestätigung — hier zum " +
                    "Speichern und per E-Mail.",
            )

            Etikett("Art der Kündigung")
            Hakenzeile("Ordentliche Kündigung", art == "ordentlich", { art = "ordentlich" })
            Hakenzeile(
                "Außerordentliche Kündigung (aus wichtigem Grund)",
                art == "ausserordentlich",
                { art = "ausserordentlich" },
            )

            if (art == "ausserordentlich") {
                Feld(
                    wert = grund,
                    beiAenderung = { grund = it.take(2000) },
                    etikett = "Kündigungsgrund",
                    einzeilig = false,
                    weiterTaste = ImeAction.Default,
                )
            }

            Etikett("Gewünschter Zeitpunkt")
            Hakenzeile("Zum nächstmöglichen Zeitpunkt", !zumDatum, { zumDatum = false })
            Hakenzeile("Zu einem bestimmten Datum", zumDatum, { zumDatum = true })
            if (zumDatum) {
                Datumswahl(
                    wert = datum,
                    beiWahl = { datum = it },
                    frueheste = LocalDate.now(),
                    zusammenhang = zusammenhang,
                )
            }

            Feld(wert = name, beiAenderung = { name = it.take(120) }, etikett = "Dein Name")
            Feld(
                wert = benutzername,
                beiAenderung = { benutzername = it.take(64) },
                etikett = "Benutzername in PagerSpass",
            )
            SehrLeise("Daran erkennen wir, welcher Vertrag gemeint ist.")
            Feld(
                wert = email,
                beiAenderung = { email = it.take(254) },
                etikett = "E-Mail-Adresse für die Bestätigung",
                tastatur = KeyboardType.Email,
                weiterTaste = ImeAction.Done,
            )

            v.fehler?.let { Warnzeile(it) }

            Knopf(
                aufschrift = if (v.laeuft) "Einen Moment …" else "jetzt kündigen",
                beiDruck = {
                    val grundSatz = fehlt()
                    if (grundSatz != null) {
                        v.fehler = grundSatz
                    } else {
                        bereich.vorgang(v, "Das hat gerade nicht geklappt.") {
                            quittung = wege.vertragKuendigen(
                                kuendigungsart = art,
                                zeitpunkt = if (zumDatum) datum else "naechstmoeglich",
                                grund = grund.trim().ifBlank { null },
                                name = name.trim(),
                                benutzername = benutzername.trim(),
                                email = email.trim(),
                            )
                        }
                    }
                },
                art = Knopfart.Haupt,
                breit = true,
                aktiv = !v.laeuft,
            )

            SehrLeise(
                "Du möchtest den Vertrag innerhalb von vierzehn Tagen nach Abschluss rückgängig " +
                    "machen? Das ist ein Widerruf:",
            )
            Textweg("Vertrag widerrufen", beiWiderrufen)
        }
    }
}

/** „Vertrag widerrufen" — dieselbe Bauart wie die Kündigung. */
@Composable
fun VertragWiderrufenSeite(
    wege: Kontowege,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    beiZurueck: () -> Unit = {},
    beiZumSpiel: () -> Unit = {},
    beiKuendigen: () -> Unit = {},
    beiRechtstext: (String) -> Unit = {},
) {
    val zusammenhang = LocalContext.current
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()

    var name by rememberSaveable { mutableStateOf("") }
    var benutzername by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var abgeschlossenAm by rememberSaveable { mutableStateOf("") }
    var quittung by rememberSaveable(stateSaver = QuittungSpeicher) { mutableStateOf<Erklaerungsquittung?>(null) }

    LaunchedEffect(konto?.kennung) {
        if (konto != null) {
            if (benutzername.isBlank()) benutzername = konto.benutzername
            if (name.isBlank()) name = konto.anzeigename
        }
    }

    fun fehlt(): String? = when {
        name.trim().length < 2 -> "Bitte gib deinen Namen an."
        benutzername.isBlank() -> "Bitte gib deinen Benutzernamen an."
        '@' !in email -> "Bitte gib die E-Mail-Adresse für die Bestätigung an."
        else -> null
    }

    Seite(modifier = modifier, unterrand = unterrand, breite = Mass.Lesebreite) {
        Seitenkopf(
            titel = "Vertrag widerrufen",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        val q = quittung
        if (q != null) {
            Quittung(q, beiZumSpiel)
            return@Seite
        }

        Kasten {
            Leise(
                "Hier widerrufst du deinen Vertrag über PagerSpass Premium. Eine Anmeldung " +
                    "brauchst du dafür nicht. Nach dem Absenden bekommst du sofort eine Bestätigung — " +
                    "hier zum Speichern und per E-Mail.",
            )

            Feld(wert = name, beiAenderung = { name = it.take(120) }, etikett = "Dein Name")
            Feld(
                wert = benutzername,
                beiAenderung = { benutzername = it.take(64) },
                etikett = "Benutzername in PagerSpass",
            )
            SehrLeise("Daran erkennen wir, welcher Vertrag gemeint ist.")
            Feld(
                wert = email,
                beiAenderung = { email = it.take(254) },
                etikett = "E-Mail-Adresse für die Bestätigung",
                tastatur = KeyboardType.Email,
                weiterTaste = ImeAction.Done,
            )
            SehrLeise("Am schnellsten geht es mit der Adresse, die du beim Bezahlen angegeben hast.")

            Etikett("Abgeschlossen am (freiwillig)")
            Datumswahl(
                wert = abgeschlossenAm,
                beiWahl = { abgeschlossenAm = it },
                spaeteste = LocalDate.now(),
                zusammenhang = zusammenhang,
                leerErlaubt = true,
            )

            SehrLeise(
                "Du hattest verlangt, dass Premium sofort beginnt. Deshalb zahlen wir dir alles " +
                    "zurück außer dem Anteil für die Zeit bis zu deinem Widerruf — siehe die " +
                    "Widerrufsbelehrung.",
            )
            Textweg("Widerrufsbelehrung (AGB)", { beiRechtstext(Rechtsstand.AGB) })

            v.fehler?.let { Warnzeile(it) }

            Knopf(
                aufschrift = if (v.laeuft) "Einen Moment …" else "Widerruf bestätigen",
                beiDruck = {
                    val grundSatz = fehlt()
                    if (grundSatz != null) {
                        v.fehler = grundSatz
                    } else {
                        bereich.vorgang(v, "Das hat gerade nicht geklappt.") {
                            quittung = wege.vertragWiderrufen(
                                name = name.trim(),
                                benutzername = benutzername.trim(),
                                email = email.trim(),
                                abgeschlossenAm = abgeschlossenAm.ifBlank { null },
                            )
                        }
                    }
                },
                art = Knopfart.Haupt,
                breit = true,
                aktiv = !v.laeuft,
            )

            SehrLeise("Du möchtest das Abo nur nicht verlängern?")
            Textweg("Verträge hier kündigen", beiKuendigen)
        }
    }
}

/**
 * Die Quittung nach Kündigung oder Widerruf — `Erklaerungsquittung.vue`.
 *
 * <b>Sie muss gespeichert werden können</b> (§ 312k Abs. 3, § 356a Abs. 2 BGB):
 * mit Datum und Uhrzeit der Abgabe, auf einem dauerhaften Datenträger. Im Web
 * über den Druckdialog; hier als Textdatei über den Dateidialog — und über das
 * Teilen-Blatt, etwa an sich selbst per Mail.
 */
@Composable
fun Quittung(q: Erklaerungsquittung, beiZumSpiel: () -> Unit) {
    val zusammenhang = LocalContext.current
    val speichern = rememberDateiSpeichern("text/plain")
    val schaltflaeche = if (q.istKuendigung) "jetzt kündigen" else "Widerruf bestätigen"
    val titel = if (q.istKuendigung) "Deine Kündigung ist eingegangen" else "Dein Widerruf ist eingegangen"
    val alsText = buildString {
        appendLine("Eingangsbestätigung — PagerSpass")
        appendLine(titel)
        appendLine()
        q.inhalt.forEach { appendLine("${it.feld}: ${it.wert}") }
        appendLine("Abgegeben am: ${q.eingang}")
        appendLine("Vorgangsnummer: ${q.id}")
        appendLine()
        appendLine("Abgegeben über die Schaltfläche „$schaltflaeche\" auf pagerspass.de.")
        appendLine()
        appendLine(q.meldung)
    }

    Kasten(marke = true) {
        Etikett("Eingangsbestätigung")
        Text(titel, style = Schrift.Titel, color = Farben.Text)

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            q.inhalt.forEach { Quittungszeile(it.feld, it.wert) }
            Quittungszeile("Abgegeben am", q.eingang, mono = true)
            Quittungszeile("Vorgangsnummer", q.id, mono = true)
        }

        Text(
            text = "Abgegeben über die Schaltfläche „$schaltflaeche\" auf pagerspass.de.",
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
        Erfolgszeile(q.meldung)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Knopf(
                "Speichern",
                { speichern("pagerspass-${if (q.istKuendigung) "kuendigung" else "widerruf"}-${q.id}.txt", alsText) },
                art = Knopfart.Haupt,
            )
            Knopf("Teilen", { textTeilen(zusammenhang, alsText, titel) })
            Knopf("Zum Spiel", beiZumSpiel, art = Knopfart.Leise)
        }
    }
}

@Composable
private fun Quittungszeile(feld: String, wert: String, mono: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        SehrLeise(feld)
        Text(wert, style = if (mono) Schrift.MonoKlein else Schrift.Normal, color = Farben.Text)
    }
}

/**
 * Ein Datum wählen — der Kalender des Systems statt eines Textfeldes, in das man
 * `JJJJ-MM-TT` tippen müsste. Der Wert ist das ISO-Datum, wie der Server ihn will.
 */
@Composable
private fun Datumswahl(
    wert: String,
    beiWahl: (String) -> Unit,
    zusammenhang: Context,
    frueheste: LocalDate? = null,
    spaeteste: LocalDate? = null,
    leerErlaubt: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Knopf(
            aufschrift = if (wert.isBlank()) "Datum wählen" else tagAusIso(wert),
            beiDruck = {
                val start = runCatching { LocalDate.parse(wert) }.getOrNull() ?: LocalDate.now()
                val dialog = DatePickerDialog(
                    zusammenhang,
                    { _, jahr, monat, tag -> beiWahl(LocalDate.of(jahr, monat + 1, tag).toString()) },
                    start.year,
                    start.monthValue - 1,
                    start.dayOfMonth,
                )
                frueheste?.let { dialog.datePicker.minDate = millis(it) }
                spaeteste?.let { dialog.datePicker.maxDate = millis(it) }
                dialog.show()
            },
            kompakt = true,
        )
        if (leerErlaubt && wert.isNotBlank()) {
            Knopf("Leeren", { beiWahl("") }, art = Knopfart.Leise, kompakt = true)
        }
    }
}

private fun millis(tag: LocalDate): Long =
    tag.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

/** `2026-09-27` → `27.09.2026`. */
private fun tagAusIso(iso: String): String = runCatching {
    val d = LocalDate.parse(iso)
    "%02d.%02d.%04d".format(d.dayOfMonth, d.monthValue, d.year)
}.getOrDefault(iso)

/**
 * Die Quittung übersteht das Drehen des Geräts — sie ist der Beleg, und ein
 * Beleg, der beim Drehen verschwindet, ist keiner. Gespeichert als JSON.
 */
private val QuittungSpeicher = androidx.compose.runtime.saveable.Saver<Erklaerungsquittung?, String>(
    save = { q -> q?.let { de.pagerspass.pagerspass.netz.Netz.abgabe.encodeToString(Erklaerungsquittung.serializer(), it) } ?: "" },
    restore = { roh ->
        if (roh.isBlank()) {
            null
        } else {
            runCatching {
                de.pagerspass.pagerspass.netz.Netz.abgabe.decodeFromString(Erklaerungsquittung.serializer(), roh)
            }.getOrNull()
        }
    },
)
