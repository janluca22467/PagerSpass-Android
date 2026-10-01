package de.pagerspass.pagerspass.mobil

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Was der Ton im Dienst anfasst — der Funkregler und „alles außer dem Melder stumm"
 * aus `Tonregler.vue`, so wie der Kopf der Leitstelle ihn zeigt.
 *
 * <b>Kein eigener Speicher.</b> Der Wert gehört `Tonstand`, den auch das Fahrzeug
 * stellt — zwei Regler mit eigenem Stand hätten am selben Lautsprecher gezogen, und
 * wer im Fahrzeug leiser dreht, hätte es in der Leitstelle lauter wiedergefunden.
 * Hier steht nur ein Spiegel als Compose-Zustand, damit der Regler beim Schieben
 * neu zeichnet; geschrieben wird immer durch `Tonstand`.
 */
object Tonpegel {
    var funk by mutableFloatStateOf(1f)
        private set
    var stummAusserMelder by mutableStateOf(false)
        private set

    /** Was der Lautsprecher tatsächlich nimmt. */
    val funkWirksam: Float get() = Tonstand.funk()

    fun laden(zusammenhang: Context) {
        Tonstand.laden(zusammenhang.applicationContext)
        spiegeln()
    }

    fun funkSetzen(zusammenhang: Context, wert: Float) {
        Tonstand.funkSetzen(zusammenhang.applicationContext, wert)
        spiegeln()
    }

    fun stummSetzen(zusammenhang: Context, an: Boolean) {
        Tonstand.stummSetzen(zusammenhang.applicationContext, an)
        spiegeln()
    }

    /** Den Spiegel nachziehen — auch nach einer Änderung über den Fahrzeugregler. */
    fun spiegeln() {
        funk = Tonstand.funkPegel.value
        stummAusserMelder = Tonstand.stumm.value
    }
}
