package de.pagerspass.pagerspass.mobil

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.pagerspass.pagerspass.ui.schmuck.Melderkatalog

/**
 * Der Tonregler des Dienstes und die Bauart des Melders — gemerkt am Gerät.
 *
 * Übertragen aus dem Teil von `web/src/stores/melder.ts`, den das Fahrzeug im
 * Dienst anfasst: **Bauart** (`dme` Piepser, `app` Alarm-App, `funk` im
 * Funkgerät), die drei Regler **Melder · Funk · Umgebung**, „alles außer dem
 * Melder stumm", die **Durchsage der Leitstelle beim Alarm** und das Mithören
 * des Einsatzstellenfunks. Bauform, Alarmton, Alarmierungsart und Profil stehen
 * dagegen in der [de.pagerspass.pagerspass.netz.Ablage] (siehe `Bedienung.kt`) —
 * sie werden im Konto eingestellt, diese hier im Dienst.
 *
 * <b>Melder, Funk, Stumm, Durchsage und DMO liegen im [Tonregler]</b> — demselben
 * Stand, den die Leitstelle dreht; hier stehen sie nur als Spiegel, damit die
 * Fahrzeugseite sie beim Zeichnen liest. Geschrieben wird immer dorthin. Eigen sind
 * dieser Klasse nur Bauart, Umgebung und „Gerät aus".
 *
 * <b>Eine Instanz je Prozess.</b> Der Regler im Kopf des Fahrzeugs und die
 * Blende des Melders lesen dieselben Werte; zwei Instanzen mit je eigenem
 * Zustand liefen auseinander, sobald eine davon dreht.
 *
 * <b>`SharedPreferences` statt DataStore</b>, weil der Wert beim ersten Zeichnen
 * schon dastehen muss: Ein Melder, der eine Zehntelsekunde lang mit der Vorgabe
 * piept, bevor die gemerkte Lautstärke eintrifft, piept eben mit der Vorgabe.
 */
class Tonwahl private constructor(
    private val zusammenhang: Context,
    private val ablage: SharedPreferences,
) {

    /** `dme`, `app` oder `funk` — wie der Alarm am Platz erscheint. */
    var bauart by mutableStateOf(ablage.getString("bauart", "dme").takeIf { it in BAUARTEN } ?: "dme")
        private set

    private val ton: Tonstand
        get() {
            Tonregler.laden(zusammenhang)
            return Tonregler.stand.value
        }

    /** Der Melderregler, 0–1. */
    var melder by mutableFloatStateOf(ton.melder)
        private set

    /** Stimmen und Gerätetöne des Funkgeräts, 0–1 — auch das Klingeln des Einzelrufs. */
    var funk by mutableFloatStateOf(ton.funk)
        private set

    /** Sirene, Signalhorn, Bedientöne am Fahrzeug, 0–1. */
    var umgebung by mutableFloatStateOf(ablage.getFloat("umgebung", 1f))
        private set

    /** Alles außer dem Melder stumm. */
    var stumm by mutableStateOf(ton.stumm)
        private set

    /** Die gesprochene Meldung der Leitstelle beim Alarm (`AlarmDurchsage`). */
    var durchsage by mutableStateOf(ton.durchsage)
        private set

    /** Den Sprechfunk der eigenen Einsatzstelle (DMO) nicht mithören. */
    var dmoStumm by mutableStateOf(ton.dmoStumm)
        private set

    /**
     * Ob der Melder ausgeschaltet ist — aus dem Gerätemenü. <b>Flüchtig</b>, wie im
     * Web: Nach einem Neustart ist das Gerät wieder an, statt mit gemerktem Profil
     * stumm zu bleiben.
     */
    var geraetAus by mutableStateOf(false)

    fun bauartSetzen(wert: String) {
        if (wert !in BAUARTEN) return
        bauart = wert
        ablage.edit().putString("bauart", wert).apply()
    }

    fun melderSetzen(wert: Float) {
        melder = wert.coerceIn(0f, 1f)
        Tonregler.setzen(zusammenhang, ton.copy(melder = melder))
    }

    fun funkSetzen(wert: Float) {
        funk = wert.coerceIn(0f, 1f)
        Tonregler.setzen(zusammenhang, ton.copy(funk = funk))
    }

    fun umgebungSetzen(wert: Float) {
        umgebung = wert.coerceIn(0f, 1f)
        ablage.edit().putFloat("umgebung", umgebung).apply()
    }

    fun stummSetzen(wert: Boolean) {
        stumm = wert
        Tonregler.setzen(zusammenhang, ton.copy(stumm = wert))
    }

    fun durchsageSetzen(wert: Boolean) {
        durchsage = wert
        Tonregler.setzen(zusammenhang, ton.copy(durchsage = wert))
    }

    fun dmoStummSetzen(wert: Boolean) {
        dmoStumm = wert
        Tonregler.setzen(zusammenhang, ton.copy(dmoStumm = wert))
    }

    /** Den Spiegel nachziehen — die Leitstelle hat am selben Regler gedreht. */
    fun uebernehmen(t: Tonstand) {
        melder = t.melder
        funk = t.funk
        stumm = t.stumm
        durchsage = t.durchsage
        dmoStumm = t.dmoStumm
    }

    /**
     * Was auf die Lautsprecher der Runde kommt (`Runde.tonAnwenden`): der Stand des
     * Reglers — mit ausgeschaltetem Gerät auch ohne die Durchsage beim Alarm.
     */
    val rundenton: Tonstand
        get() = Tonstand(melder = melder, funk = funk, stumm = stumm, durchsage = durchsage && !geraetAus, dmoStumm = dmoStumm)

    /**
     * Der Pegel nach einem neuen Profil — `pegelNachfuehren` im Web.
     *
     * Ein Profil ist Alarmierungsart <em>und</em> Lautstärke; die Art liegt in der
     * Ablage, die Lautstärke hier. Angewandt wird nur ein <b>neues</b> Profil: Wer
     * den Regler danach von Hand dreht, soll ihn beim nächsten Öffnen des Fahrzeugs
     * nicht wieder auf der Profilstufe finden.
     */
    fun profilNachfuehren(profilId: String) {
        if (ablage.getString("profilAngewandt", null) == profilId) return
        ablage.edit().putString("profilAngewandt", profilId).apply()
        val profil = Melderkatalog.PROFILE.firstOrNull { it.id == profilId } ?: return
        if (profil.lautstaerke > 0f) melderSetzen(profil.lautstaerke)
    }

    /** Wie laut der Alarm tönt — null, wenn die Art keinen Ton hat oder das Gerät aus ist. */
    fun alarmpegel(alarmierungsart: String): Float =
        if (geraetAus || alarmierungsart == "vibration" || alarmierungsart == "stumm") 0f else melder

    /** Der wirksame Funkpegel — „stumm" nimmt alles außer dem Melder weg. */
    val funkpegel: Float get() = if (stumm) 0f else funk

    /** Der wirksame Pegel der Umgebung. */
    val umgebungspegel: Float get() = if (stumm) 0f else umgebung

    companion object {
        /** Die drei Bauarten, in der Reihenfolge des Schalters. */
        val BAUARTEN = listOf("dme", "app", "funk")

        @Volatile
        private var einzige: Tonwahl? = null

        fun von(zusammenhang: Context): Tonwahl = einzige ?: synchronized(this) {
            einzige ?: Tonwahl(
                zusammenhang.applicationContext,
                zusammenhang.applicationContext.getSharedPreferences("pagerspass-ton", Context.MODE_PRIVATE),
            ).also { einzige = it }
        }
    }
}

/** Der Tonregler dieses Geräts — dieselbe Instanz an jeder Stelle. */
@Composable
fun rememberTonwahl(): Tonwahl {
    val zusammenhang = LocalContext.current.applicationContext
    return remember(zusammenhang) { Tonwahl.von(zusammenhang) }
}
