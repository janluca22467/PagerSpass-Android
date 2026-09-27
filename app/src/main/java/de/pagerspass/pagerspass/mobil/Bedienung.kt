package de.pagerspass.pagerspass.mobil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.pagerspass.pagerspass.netz.Ablage

/**
 * „Deine Bedienung" — die Einstellungen, die nur für dieses Gerät gelten.
 *
 * <b>Für die Rundenansichten gedacht.</b> Eingestellt wird im Konto (Notrufabfrage,
 * Fahrzeugkennung) und im Melder-Reiter des Profileditors (Bauform, Alarmton,
 * Alarmierungsart, Profil); gelesen wird im Dienst. Beide Seiten gehen über die
 * [Ablage] — dieselbe Quelle, und jede Änderung kommt über den Fluss sofort an:
 *
 * ```kotlin
 * val weg by rememberEingabeweg()          // "fragen" | "tippen" | "sprechen"
 * val form by rememberKennungsform()       // "kennzahl" | "typ" | "orga"
 * Text(Fahrzeugkennung.kennung(kurzname, typ, organisation, hiOrg, form, kreisname))
 * ```
 *
 * <b>Bewusst am Gerät und nicht in der Runde:</b> Wer lieber tippt, tippt auch in
 * fremden Runden — dieselbe Überlegung wie `useEingabeweg` im Web.
 */

/** Der Eingabeweg am Notruftelefon: `fragen` (Vorgabe), `tippen` oder `sprechen`. */
@Composable
fun rememberEingabeweg(): State<String> {
    val ablage = rememberAblage()
    val fluss = remember(ablage) { ablage.eingabewegFluss() }
    return fluss.collectAsState(initial = Ablage.EINGABEWEGE.first())
}

/** Wie Fahrzeuge in den Listen heißen: `kennzahl` (Vorgabe), `typ` oder `orga`. */
@Composable
fun rememberKennungsform(): State<String> {
    val ablage = rememberAblage()
    val fluss = remember(ablage) { ablage.kennungsformFluss() }
    return fluss.collectAsState(initial = Ablage.KENNUNGSFORMEN.first())
}

/** Die Melder-Bauform dieses Geräts (Id aus [Melderkatalog.BAUFORMEN]), Vorgabe `dienst`. */
@Composable
fun rememberMelderBauform(): State<String> {
    val ablage = rememberAblage()
    val fluss = remember(ablage) { ablage.melderBauformFluss() }
    return fluss.collectAsState(initial = "dienst")
}

/** Der Alarmton dieses Geräts (Id aus [Melderkatalog.TOENE]), Vorgabe `zweiklang`. */
@Composable
fun rememberMelderTon(): State<String> {
    val ablage = rememberAblage()
    val fluss = remember(ablage) { ablage.melderTonFluss() }
    return fluss.collectAsState(initial = "zweiklang")
}

/**
 * Wie der Melder einen Alarm ankündigt: `voll` (Vorgabe), `ton`, `vibration`,
 * `stumm`. Die einzige Einstellung hier mit Folgen im Dienst — wer auf Vibration
 * oder stumm steht, hört den Alarm nicht.
 */
@Composable
fun rememberAlarmierungsart(): State<String> {
    val ablage = rememberAblage()
    val fluss = remember(ablage) { ablage.alarmierungsartFluss() }
    return fluss.collectAsState(initial = Ablage.ALARMIERUNGSARTEN.first())
}

/** Das Melderprofil (Id aus [Melderkatalog.PROFILE]), Vorgabe `vollalarm`. */
@Composable
fun rememberMelderprofil(): State<String> {
    val ablage = rememberAblage()
    val fluss = remember(ablage) { ablage.melderprofilFluss() }
    return fluss.collectAsState(initial = "vollalarm")
}

/**
 * Die Ablage dieses Geräts, einmal je Ansicht.
 *
 * `applicationContext` und nicht die Activity: Die Ablage überlebt jede Ansicht,
 * und ein Verweis auf die Activity darin wäre ein Leck beim Drehen des Geräts.
 */
@Composable
fun rememberAblage(): Ablage {
    val zusammenhang = LocalContext.current.applicationContext
    return remember(zusammenhang) { Ablage(zusammenhang) }
}

/**
 * Die Kurzkennung eines Fahrzeugs, wie dieser Spieler sie sehen will — übertragen
 * aus `web/src/composables/fahrzeugkennung.ts`.
 *
 * `kennzahl` ist die BOS-Kurzform („1/44/1"), `typ` setzt den Fahrzeugtyp an die
 * Stelle der Kennzahl („1/HLF 20-1"), `orga` stellt Träger und Kreis davor
 * („RK-Celle-3/91/1").
 *
 * <b>Der Funk bleibt außen vor.</b> Wo ein Fahrzeug *gerufen* wird, steht weiter
 * der volle Funkrufname — den kann man sich nicht nach Geschmack umbenennen.
 */
object Fahrzeugkennung {

    private val DREITEILIG = Regex("""^(\d+)/(\d+)/(\d+)$""")
    private val ZWEITEILIG = Regex("""^(\d+)/(\d+)$""")

    private val TRAEGERKUERZEL = mapOf(
        "Keine" to "",
        "Drk" to "RK",
        "Juh" to "JH",
        "Mhd" to "MH",
        "Asb" to "ASB",
        "Dlrg" to "DLRG",
        "Bergwacht" to "BW",
        "Wasserwacht" to "WW",
        "Dgzrs" to "SAR",
        "Brh" to "BRH",
        "Werkfeuerwehr" to "WF",
        "Privat" to "PRV",
    )

    private val ORGAKUERZEL = mapOf(
        "Feuerwehr" to "FW",
        "Rettungsdienst" to "RD",
        "Thw" to "THW",
        "Polizei" to "POL",
    )

    /**
     * Die Kurzkennung mit dem Fahrzeugtyp anstelle der Kennzahl. Passt die
     * Kurzform in kein Muster, bleibt sie, wie sie ist — die hat die Leitstelle
     * von Hand gesetzt.
     */
    fun typkennung(kurzname: String, typ: String): String {
        DREITEILIG.find(kurzname)?.let { treffer ->
            val (wache, _, nummer) = treffer.destructured
            return "$wache/$typ-$nummer"
        }
        ZWEITEILIG.find(kurzname)?.let { treffer ->
            val (_, nummer) = treffer.destructured
            return "$typ-$nummer"
        }
        return kurzname
    }

    /**
     * Die Kennung mit Träger und Kreis davor: „RK-Celle-3/91/1". Ohne Kreis (eine
     * Runde ohne echten Landkreis) fällt der Kreisteil weg.
     */
    fun orgakennung(kurzname: String, organisation: String?, traeger: String?, kreisname: String): String {
        val wer = traeger?.let { TRAEGERKUERZEL[it] }?.takeIf { it.isNotEmpty() }
            ?: organisation?.let { ORGAKUERZEL[it] }
            ?: ""
        return listOf(wer, kreisname, kurzname).filter { it.isNotEmpty() }.joinToString("-")
    }

    /** Die Kennung in der gewählten Form (`kennzahl`, `typ`, `orga`). */
    fun kennung(
        kurzname: String,
        typ: String,
        organisation: String?,
        traeger: String?,
        form: String,
        kreisname: String = "",
    ): String = when (form) {
        "typ" -> typkennung(kurzname, typ)
        "orga" -> orgakennung(kurzname, organisation, traeger, kreisname)
        else -> kurzname
    }
}
