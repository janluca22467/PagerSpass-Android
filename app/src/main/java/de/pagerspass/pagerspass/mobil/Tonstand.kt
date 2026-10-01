package de.pagerspass.pagerspass.mobil

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Die Toneinstellungen im Dienst — der Teil des `melder`-Stores im Web, den der
 * Tonregler im Fahrzeugkopf stellt (`ui/Tonregler.vue`): Funk- und Umgebungspegel,
 * Mithören des Direktbetriebs und der Stumm-Knopf. Der Melderpegel liegt beim
 * Melder selbst (`Meldergeraet.lautstaerke`).
 *
 * <b>Ein Wert auf Prozessebene.</b> Die Lautsprecher der Leitungen lesen ihn bei jedem
 * Paket — ein Regler, der erst beim nächsten Funkspruch wirkt, fühlt sich kaputt an.
 * Gespeichert wird im Gerät, wie im Web im Browser: Am fremden Gerät steht der Ton
 * anders als am eigenen.
 */
object Tonstand {

    private const val DATEI = "pagerspass_ton"

    private val _funkPegel = MutableStateFlow(1f)
    val funkPegel: StateFlow<Float> = _funkPegel.asStateFlow()

    private val _umgebungPegel = MutableStateFlow(1f)
    val umgebungPegel: StateFlow<Float> = _umgebungPegel.asStateFlow()

    /** „Alles außer dem Melder stumm" — der Knopf neben dem Regler. */
    private val _stumm = MutableStateFlow(false)
    val stumm: StateFlow<Boolean> = _stumm.asStateFlow()

    /** Den Direktbetrieb fremder Einsatzstellen mithören — ein Schalter, kein Schieber. */
    private val _dmoMithoeren = MutableStateFlow(true)
    val dmoMithoeren: StateFlow<Boolean> = _dmoMithoeren.asStateFlow()

    @Volatile private var geladen = false

    fun laden(zusammenhang: Context) {
        if (geladen) return
        geladen = true
        runCatching {
            val a = zusammenhang.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
            _funkPegel.value = a.getFloat("funk", 1f).coerceIn(0f, 1f)
            _umgebungPegel.value = a.getFloat("umgebung", 1f).coerceIn(0f, 1f)
            _stumm.value = a.getBoolean("stumm", false)
            _dmoMithoeren.value = a.getBoolean("dmo", true)
        }
    }

    /** Der wirksame Funkpegel — stumm heißt null. */
    fun funk(): Float = if (_stumm.value) 0f else _funkPegel.value

    /** Der wirksame Pegel des Direktbetriebs: Funkpegel, solange mitgehört wird. */
    fun dmo(): Float = if (_dmoMithoeren.value) funk() else 0f

    /** Der wirksame Umgebungspegel — Signalhorn und was sonst im Hintergrund läuft. */
    fun umgebung(): Float = if (_stumm.value) 0f else _umgebungPegel.value

    fun funkSetzen(zusammenhang: Context, wert: Float) {
        _funkPegel.value = wert.coerceIn(0f, 1f)
        schreiben(zusammenhang) { putFloat("funk", _funkPegel.value) }
    }

    fun umgebungSetzen(zusammenhang: Context, wert: Float) {
        _umgebungPegel.value = wert.coerceIn(0f, 1f)
        schreiben(zusammenhang) { putFloat("umgebung", _umgebungPegel.value) }
        Signalhorn.pegelNeu()
    }

    fun stummSetzen(zusammenhang: Context, an: Boolean) {
        _stumm.value = an
        schreiben(zusammenhang) { putBoolean("stumm", an) }
        Signalhorn.pegelNeu()
    }

    fun dmoSetzen(zusammenhang: Context, an: Boolean) {
        _dmoMithoeren.value = an
        schreiben(zusammenhang) { putBoolean("dmo", an) }
    }

    private fun schreiben(zusammenhang: Context, tun: android.content.SharedPreferences.Editor.() -> Unit) {
        runCatching { zusammenhang.getSharedPreferences(DATEI, Context.MODE_PRIVATE).edit().apply(tun).apply() }
    }
}
