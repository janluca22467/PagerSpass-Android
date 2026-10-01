package de.pagerspass.pagerspass.melder

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.pagerspass.pagerspass.netz.Alarmmeldung
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Der Speicher des Melders — die letzten Meldungen, die dieses Gerät empfangen hat.
 *
 * Übertragen aus `melderverlauf` (`stores/spiel.ts`) und `vergangene` in
 * `composables/melderanzeige.ts`: <b>Ein echtes Gerät hält die letzten Meldungen
 * vor</b> — man will nachlesen können, was vorhin durchkam. Dreißig Stück, das
 * Neueste vorn; ein zweiter Alarm zum selben Einsatz ersetzt den ersten, statt
 * ihn zu verdoppeln.
 *
 * <b>Am Gerät und nicht am Konto</b>, dieselbe Linie wie der Rest des Melders
 * (siehe [Meldergeraet]): Die Meldungen liegen in den `SharedPreferences`
 * dieses Handys und gehen nirgendwohin. Anders als im Web überleben sie das
 * Schließen der App — ein Piepser, den man ausschaltet und wieder einschaltet,
 * hat seinen Speicher auch noch.
 *
 * <b>Der Papierkorb des Alarmfaxes ist derselbe Speicher</b>, nur mit dem
 * Handgriff des Geräts davor (`AlarmFax.vue`): Was abgerissen ist, liegt dort.
 * Zwei Listen für dieselben Meldungen liefen auseinander.
 */
object Melderspeicher {

    private const val DATEI = "melderspeicher"
    private const val HOECHSTZAHL = 30
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val form = ListSerializer(Alarmmeldung.serializer())

    private var ablage: SharedPreferences? = null

    /** Jüngste zuerst. */
    var meldungen by mutableStateOf(listOf<Alarmmeldung>())
        private set

    fun bereit(zusammenhang: Context): Melderspeicher {
        if (ablage != null) return this
        val a = zusammenhang.applicationContext.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
        ablage = a
        meldungen = a.getString("meldungen", null)
            ?.let { runCatching { json.decodeFromString(form, it) }.getOrNull() }
            .orEmpty()
            .take(HOECHSTZAHL)
        return this
    }

    /**
     * Eine eingegangene Meldung ablegen.
     *
     * Die Probemeldung bleibt draußen: Ein Probealarm ist ein Ton, keine
     * Meldung, und im Speicher stünde er zwischen echten Einsätzen wie ein
     * Einsatz, den es nie gab.
     */
    fun aufnehmen(alarm: Alarmmeldung) {
        if (alarm.incidentId == "probe" || alarm.incidentId.isBlank()) return
        val neu = listOf(alarm) + meldungen.filter { it.incidentId != alarm.incidentId }
        meldungen = neu.take(HOECHSTZAHL)
        schreiben()
    }

    /** Den Speicher leeren — am Gerät ein Menüpunkt, hier ein Knopf unter der Liste. */
    fun leeren() {
        meldungen = emptyList()
        schreiben()
    }

    /** Alles außer der Meldung, die gerade im Display steht (`vergangene`). */
    fun vergangene(aktuell: String?): List<Alarmmeldung> =
        meldungen.filter { it.incidentId != aktuell }

    private fun schreiben() {
        ablage?.edit()?.putString("meldungen", json.encodeToString(form, meldungen))?.apply()
    }
}
