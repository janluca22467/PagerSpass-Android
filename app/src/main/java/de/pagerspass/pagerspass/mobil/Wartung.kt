package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Rundenwege
import de.pagerspass.pagerspass.netz.Wartungsankuendigung
import de.pagerspass.pagerspass.netz.Wartungsstand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Die Wartung — das Gegenstück zu `web/src/stores/wartung.ts`.
 *
 * <b>Zwei Wege führen hierher.</b> Jeder Abruf, der mit 503 und einer beigelegten
 * Wartung antwortet (`Netz.beiWartung` → `Sitzungsstand.wartung`), und der eigene
 * Takt auf `/api/wartung`. Der erste reicht nicht: Wer nur in einer Runde steht,
 * macht keine REST-Anfragen, und die Sperre käme nie — die Ankündigung davor
 * schon gar nicht, denn die hat keinen 503.
 *
 * <b>Der Takt passt sich an.</b> Offen: alle zwei Minuten. Angekündigt: alle
 * fünfzehn Sekunden — sonst liefe der Zähler zu lange gegen die eigene Uhr.
 * Gesperrt: alle zehn Sekunden, damit die Tür aufgeht, sobald sie offen ist.
 * Im Hintergrund fragt niemand; beim Zurückkommen sofort.
 */
class Wartung(anwendung: Application) : AndroidViewModel(anwendung) {

    private val wege = Rundenwege(Netz(Ablage(anwendung)))

    private val _stand = MutableStateFlow(Wartungslage())
    val stand: StateFlow<Wartungslage> = _stand.asStateFlow()

    private var takt: Job? = null
    private var uhr: Job? = null
    private var sichtbar = true
    private var anfragen = 0

    /** Einmal anwerfen — mehrfach rufen ist harmlos. */
    fun beobachten() {
        if (takt != null) return
        nachsehen()

        // Die Uhr zwischen zwei Abrufen: Sie zählt herunter, der nächste Abruf
        // setzt sie wieder auf die Zahl des Servers.
        uhr = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _stand.update { alt ->
                    val bisBeginn = (alt.bisBeginn - 1).coerceAtLeast(0)
                    val neu = alt.copy(
                        verbleibend = (alt.verbleibend - 1).coerceAtLeast(0),
                        bisBeginn = if (alt.geplant != null) bisBeginn else 0,
                    )
                    neu.copy(meldet = neu.meldet && neu.meldetBis > System.currentTimeMillis())
                        .mitMarke(alt.marke)
                }
                // Auf null heruntergezählt: Der Server hat gerade zugemacht — nicht
                // bis zum nächsten Takt warten.
                val s = _stand.value
                if (s.geplant != null && s.bisBeginn == 0) nachsehen()
            }
        }

        takt = viewModelScope.launch {
            while (isActive) {
                val s = _stand.value
                delay(
                    when {
                        s.geschlossen -> TAKT_GESCHLOSSEN
                        s.ankuendigung != null -> TAKT_ANGEKUENDIGT
                        else -> TAKT_OFFEN
                    },
                )
                if (sichtbar) nachsehen()
            }
        }
    }

    /** Vorder- oder Hintergrund — beim Zurückkommen wird sofort nachgesehen. */
    fun sichtbarkeit(ja: Boolean) {
        sichtbar = ja
        if (ja) nachsehen()
    }

    /** Eine Wartung aus einem 503 — sie steht damit, bevor der Takt nachfragt. */
    fun ausAntwort(stand: Wartungsstand?) {
        if (stand == null) return
        anfragen++
        uebernehmen(stand)
    }

    /** Eine einzelne Nachfrage — veraltete Antworten fallen still weg. */
    fun nachsehen() {
        val nr = ++anfragen
        viewModelScope.launch {
            val antwort = runCatching { wege.wartung() }.getOrNull() ?: return@launch
            if (nr != anfragen) return@launch
            uebernehmen(antwort.wartung)
            ankuendigungUebernehmen(antwort.geplant)
        }
    }

    private fun uebernehmen(neu: Wartungsstand?) {
        if (neu != null) {
            _stand.update {
                it.copy(
                    stand = neu,
                    verbleibend = neu.verbleibendSekunden,
                    geplant = null,
                    bisBeginn = 0,
                    warGesperrt = true,
                )
            }
            return
        }
        // Wieder offen, nachdem gesperrt war: Das Web lädt die Seite neu. Hier
        // zählt `wiederOffen` hoch, und der Rahmen holt Konto und Runde zurück.
        _stand.update { alt ->
            if (alt.stand != null && alt.warGesperrt) {
                alt.copy(stand = null, wiederOffen = alt.wiederOffen + 1)
            } else {
                alt.copy(stand = null)
            }
        }
    }

    private fun ankuendigungUebernehmen(neu: Wartungsankuendigung?) {
        _stand.update { alt ->
            alt.copy(geplant = neu, bisBeginn = neu?.beginntInSekunden ?: 0).mitMarke(alt.marke)
        }
    }

    /**
     * Wechselt die Marke (20, 10, 5 oder eine Minute vorher), meldet sich das Band
     * zwölf Sekunden lang deutlicher.
     */
    private fun Wartungslage.mitMarke(vorher: Int?): Wartungslage {
        val jetzt = marke
        return if (jetzt != null && jetzt != vorher) {
            copy(meldet = true, meldetBis = System.currentTimeMillis() + 12_000)
        } else {
            this
        }
    }

    private companion object {
        const val TAKT_OFFEN = 120_000L
        const val TAKT_GESCHLOSSEN = 10_000L
        const val TAKT_ANGEKUENDIGT = 15_000L
    }
}

/**
 * Was über die Wartung bekannt ist.
 *
 * `ankuendigung` steht erst zwanzig Minuten vor Beginn — vorher ist eine
 * angekündigte Wartung eine Auskunft, die niemanden betrifft.
 */
data class Wartungslage(
    /** Das laufende Fenster — dann ist zu. */
    val stand: Wartungsstand? = null,
    /** Sekunden bis zum Ende, heruntergezählt. */
    val verbleibend: Int = 0,
    val geplant: Wartungsankuendigung? = null,
    /** Sekunden bis zum Beginn, heruntergezählt. */
    val bisBeginn: Int = 0,
    /** Ob das Band gerade deutlicher auf sich aufmerksam macht. */
    val meldet: Boolean = false,
    val meldetBis: Long = 0,
    val warGesperrt: Boolean = false,
    /** Zählt hoch, sooft eine Sperre wieder aufgeht. */
    val wiederOffen: Int = 0,
) {
    val geschlossen: Boolean get() = stand != null

    val ankuendigung: Wartungsankuendigung?
        get() = geplant?.takeIf { bisBeginn <= VORLAUF_SEKUNDEN }

    /** Die nächste Marke, unter der der Zähler steht — `null` ohne Ankündigung. */
    val marke: Int?
        get() = if (ankuendigung == null) null else MARKEN.firstOrNull { bisBeginn <= it }

    /** In den letzten fünf Minuten zählt nur noch, den Einsatz zu Ende zu bringen. */
    val knapp: Boolean get() = bisBeginn <= 5 * 60
}

private const val VORLAUF_SEKUNDEN = 20 * 60
private val MARKEN = listOf(60, 5 * 60, 10 * 60, 20 * 60)
