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
 * Die Toneinstellungen im Dienst — der Stand hinter `components/ui/Tonregler.vue`.
 *
 * <b>Am Gerät, nicht in der Runde.</b> Im Web liegen sie im `melder`-Store und
 * überstehen einen Neuladen; hier in den `SharedPreferences`, aus demselben
 * Grund: Wer leiser gedreht hat, will es in der nächsten Schicht nicht wieder
 * tun.
 *
 * <b>Was fehlt, und warum.</b> Der Webregler hat noch „Umgebung" und den
 * Leitstellenhintergrund — beides steuert ein Hintergrundbett aus Tondateien,
 * das die App nicht mitbringt. Ein Schieber, der nichts bewegt, wäre schlechter
 * als keiner.
 */
data class Tonstand(
    /** Der Melderregler, 0–1 — trägt auch die gesprochene Alarmmeldung. */
    val melder: Float = 0.9f,
    /** Stimmen und Gerätetöne des Funkgeräts, 0–1 — auch Klingeln und Telefon. */
    val funk: Float = 1f,
    /** Alles außer dem Melder stumm. */
    val stumm: Boolean = false,
    /** Ob die gesprochene Meldung der Leitstelle beim Alarm zu hören ist. */
    val durchsage: Boolean = true,
    /** Den Direktbetrieb (DMO) der Einsatzstellen nicht mithören. */
    val dmoStumm: Boolean = false,
) {
    /** Der Pegel für alles, was am Funkregler hängt — 0 bei „stumm". */
    val funkpegel: Float get() = if (stumm) 0f else funk.coerceIn(0f, 1f)

    /** Der Pegel für Tongeber (`ToneGenerator`), 0–100. */
    val tongeberpegel: Int get() = (funkpegel * 100).toInt().coerceIn(0, 100)
}

/** Der eine Tonstand des Geräts — geladen beim ersten Zugriff, gesichert bei jeder Änderung. */
object Tonregler {

    private const val ABLAGE = "pagerspass-tonregler"

    private val _stand = MutableStateFlow(Tonstand())
    val stand: StateFlow<Tonstand> = _stand.asStateFlow()

    @Volatile
    private var geladen = false

    /** Einmal aus der Ablage lesen — spätere Aufrufe tun nichts. */
    fun laden(zusammenhang: Context) {
        if (geladen) return
        geladen = true
        val ablage = zusammenhang.applicationContext.getSharedPreferences(ABLAGE, Context.MODE_PRIVATE)
        _stand.value = Tonstand(
            melder = ablage.getFloat("melder", 0.9f),
            funk = ablage.getFloat("funk", 1f),
            stumm = ablage.getBoolean("stumm", false),
            durchsage = ablage.getBoolean("durchsage", true),
            dmoStumm = ablage.getBoolean("dmoStumm", false),
        )
    }

    /** Einen neuen Stand setzen und sichern. */
    fun setzen(zusammenhang: Context, neu: Tonstand) {
        _stand.value = neu
        zusammenhang.applicationContext.getSharedPreferences(ABLAGE, Context.MODE_PRIVATE)
            .edit()
            .putFloat("melder", neu.melder)
            .putFloat("funk", neu.funk)
            .putBoolean("stumm", neu.stumm)
            .putBoolean("durchsage", neu.durchsage)
            .putBoolean("dmoStumm", neu.dmoStumm)
            .apply()
    }
}

/** Der Tonstand zum Lesen in einer Ansicht — lädt beim ersten Mal aus der Ablage. */
@Composable
fun rememberTonstand(): State<Tonstand> {
    val zusammenhang = LocalContext.current
    remember(zusammenhang) { Tonregler.laden(zusammenhang) }
    return Tonregler.stand.collectAsState()
}
