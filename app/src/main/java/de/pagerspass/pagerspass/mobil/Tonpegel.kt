package de.pagerspass.pagerspass.mobil

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Was der Ton im Dienst anfasst — der Funkregler und „alles außer dem Melder stumm"
 * aus `Tonregler.vue`. Die Melderlautstärke liegt schon beim Gerät
 * (`Meldergeraet.lautstaerke`); hier steht, was der Lautsprecher des Funks hört.
 *
 * <b>Ein Wert auf Prozessebene</b>, wie die Geräteeinstellungen: eingestellt im Kopf
 * der Leitstelle, benutzt vom Lautsprecher der Runde. Er übersteht einen Neustart,
 * wie im Web der `melder`-Store einen Reload.
 */
object Tonpegel {

    private const val DATEI = "pagerspass_ton"

    /** Die Lautstärke des Funks, 0 bis 1 — Stimmen und Gerätetöne. */
    var funk by mutableFloatStateOf(1f)
        private set

    /** Alles außer dem Melder stumm — der Melder klingelt trotzdem. */
    var stummAusserMelder by mutableStateOf(false)
        private set

    /** Was der Lautsprecher wirklich anwendet. */
    val funkWirksam: Float get() = if (stummAusserMelder) 0f else funk

    @Volatile private var geladen = false

    fun laden(zusammenhang: Context) {
        if (geladen) return
        geladen = true
        runCatching {
            val d = zusammenhang.applicationContext.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
            funk = d.getFloat("funk", 1f).coerceIn(0f, 1f)
            stummAusserMelder = d.getBoolean("stumm", false)
        }
    }

    fun funkSetzen(zusammenhang: Context, wert: Float) {
        funk = wert.coerceIn(0f, 1f)
        runCatching {
            zusammenhang.applicationContext.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
                .edit().putFloat("funk", funk).apply()
        }
    }

    fun stummSetzen(zusammenhang: Context, an: Boolean) {
        stummAusserMelder = an
        runCatching {
            zusammenhang.applicationContext.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
                .edit().putBoolean("stumm", an).apply()
        }
    }
}
