package de.pagerspass.pagerspass.mobil

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Die Einstellungen, die nur für dieses Gerät gelten — „Deine Bedienung“ in
 * `KontoView.vue`: der Eingabeweg am Notruftelefon (`composables/eingabeweg.ts`)
 * und wie Fahrzeuge in den Listen kurz heißen (`composables/fahrzeugkennung.ts`).
 *
 * <b>Bewusst im Gerät und nicht im Konto.</b> Es sind persönliche Vorlieben,
 * keine Regeln der Runde: Wer lieber tippt, tippt auch in fremden Runden. Im Web
 * stehen sie im `localStorage`, hier in einer eigenen Gerätedatei.
 *
 * <b>Ein Wert auf Prozessebene, kein Wert je Seite</b> — aus demselben Grund wie
 * im Web: eingestellt wird im Konto, benutzt am Telefon und auf der Karte. Zwei
 * eigene Kopien wären bis zum nächsten Start verschieden.
 */
object Geraeteeinstellungen {

    private const val DATEI = "pagerspass_geraet"
    private const val EINGABEWEG = "notruf.eingabeweg"
    private const val KENNUNGSFORM = "kennung.form"

    /** `fragen`, `tippen` oder `sprechen` — dieselben Wörter wie im Web. */
    val EINGABEWEGE = listOf("fragen", "tippen", "sprechen")

    /** `kennzahl`, `typ` oder `orga`. */
    val KENNUNGSFORMEN = listOf("kennzahl", "typ", "orga")

    private val _eingabeweg = MutableStateFlow("fragen")
    val eingabeweg: StateFlow<String> = _eingabeweg.asStateFlow()

    private val _kennungsform = MutableStateFlow("kennzahl")
    val kennungsform: StateFlow<String> = _kennungsform.asStateFlow()

    @Volatile private var geladen = false

    /** Einmal je Prozess aus der Gerätedatei lesen — Unbekanntes fällt auf die Vorgabe. */
    fun laden(zusammenhang: Context) {
        if (geladen) return
        geladen = true
        val datei = runCatching {
            zusammenhang.applicationContext.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
        }.getOrNull() ?: return
        _eingabeweg.value = datei.getString(EINGABEWEG, null)?.takeIf { it in EINGABEWEGE } ?: "fragen"
        _kennungsform.value = datei.getString(KENNUNGSFORM, null)?.takeIf { it in KENNUNGSFORMEN } ?: "kennzahl"
    }

    fun eingabewegSetzen(zusammenhang: Context, weg: String) {
        if (weg !in EINGABEWEGE) return
        _eingabeweg.value = weg
        schreiben(zusammenhang, EINGABEWEG, weg)
    }

    fun kennungsformSetzen(zusammenhang: Context, form: String) {
        if (form !in KENNUNGSFORMEN) return
        _kennungsform.value = form
        schreiben(zusammenhang, KENNUNGSFORM, form)
    }

    /** Nach „lokale Daten löschen“: zurück auf die Vorgaben, als wäre nie etwas gewählt. */
    fun vergessen() {
        _eingabeweg.value = "fragen"
        _kennungsform.value = "kennzahl"
    }

    private fun schreiben(zusammenhang: Context, schluessel: String, wert: String) {
        runCatching {
            zusammenhang.applicationContext.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
                .edit().putString(schluessel, wert).apply()
        }
    }

    /**
     * Die Kennung, die dieser Spieler sehen will — `kennung()` im Web.
     *
     * <b>Der Funk bleibt außen vor.</b> Im Funkprotokoll und überall, wo ein
     * Fahrzeug gerufen wird, steht weiter der volle Funkrufname; der ist keine
     * Anzeige, sondern das, was am Mikrofon gesagt wird.
     */
    fun kennung(
        kurzname: String,
        typ: String,
        organisation: String?,
        kreis: String?,
        form: String = _kennungsform.value,
    ): String = when (form) {
        "typ" -> typkennung(kurzname, typ)
        "orga" -> listOf(ORGAKUERZEL[organisation].orEmpty(), kreis.orEmpty(), kurzname)
            .filter { it.isNotEmpty() }
            .joinToString("-")
        else -> kurzname
    }

    /**
     * Die Kurzkennung mit dem Fahrzeugtyp anstelle der Kennzahl: „1/HLF 20-1“.
     * Passt die Kurzform in kein Muster, bleibt sie, wie sie ist — wer seine
     * Kennung selbst gesetzt hat, hat sich dabei etwas gedacht.
     */
    fun typkennung(kurzname: String, typ: String): String {
        DREITEILIG.matchEntire(kurzname)?.let { return "${it.groupValues[1]}/$typ-${it.groupValues[3]}" }
        ZWEITEILIG.matchEntire(kurzname)?.let { return "$typ-${it.groupValues[2]}" }
        return kurzname
    }

    private val DREITEILIG = Regex("""^(\d+)/(\d+)/(\d+)$""")
    private val ZWEITEILIG = Regex("""^(\d+)/(\d+)$""")

    private val ORGAKUERZEL = mapOf(
        "Feuerwehr" to "FW",
        "Rettungsdienst" to "RD",
        "Thw" to "THW",
        "Polizei" to "POL",
    )
}

/** Die Einstellungen als Compose-Zustand — liest beim ersten Mal die Gerätedatei. */
@Composable
fun eingabewegState(): State<String> {
    val zusammenhang = LocalContext.current
    remember { Geraeteeinstellungen.laden(zusammenhang) }
    return Geraeteeinstellungen.eingabeweg.collectAsState()
}

@Composable
fun kennungsformState(): State<String> {
    val zusammenhang = LocalContext.current
    remember { Geraeteeinstellungen.laden(zusammenhang) }
    return Geraeteeinstellungen.kennungsform.collectAsState()
}
