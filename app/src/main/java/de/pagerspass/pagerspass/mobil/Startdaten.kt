package de.pagerspass.pagerspass.mobil

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Footerknopf
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.OffeneUmfrage
import de.pagerspass.pagerspass.netz.Rundenvorlagenzeile
import de.pagerspass.pagerspass.netz.Rundenwege
import de.pagerspass.pagerspass.netz.Startkachel
import de.pagerspass.pagerspass.netz.Vorlagenbesatzung
import de.pagerspass.pagerspass.netz.Vorlageninhalt
import de.pagerspass.pagerspass.netz.Wache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Was der Startbildschirm über die sechs festen Wege hinaus zeigt — die
 * gemerkten Rundenvorlagen, die offene Umfrage, die Kacheln und Knöpfe aus der
 * Verwaltung, der einmalige Hinweis auf die Ausbildungsschicht.
 *
 * <b>Getrennt von der `Sitzung`</b>, weil nichts davon zum Konto gehört und
 * alles still scheitern darf: Eine Umfrage, die nicht lädt, ist keine Meldung
 * wert — der Startbildschirm steht trotzdem.
 */
class Startdaten(anwendung: Application) : AndroidViewModel(anwendung) {

    private val wege = Rundenwege(Netz(Ablage(anwendung)))
    private val merker = anwendung.getSharedPreferences("pagerspass.start", Context.MODE_PRIVATE)

    private val _stand = MutableStateFlow(Startstand())
    val stand: StateFlow<Startstand> = _stand.asStateFlow()

    /**
     * Alles laden, was der Startbildschirm braucht. Jedes für sich — scheitert
     * eines, stehen die anderen trotzdem.
     */
    fun laden(kennung: String?) {
        viewModelScope.launch {
            runCatching { wege.startkacheln() }.onSuccess { k -> _stand.update { it.copy(kacheln = k) } }
        }
        viewModelScope.launch {
            runCatching { wege.footerknoepfe() }.onSuccess { k -> _stand.update { it.copy(footer = k) } }
        }
        if (kennung == null) return
        _stand.update {
            it.copy(ausbildungHinweisWeg = merker.getBoolean("$HINWEIS_WEG.$kennung", false))
        }
        vorlagenLaden(kennung)
        viewModelScope.launch {
            runCatching { wege.umfrage(kennung) }
                .onSuccess { u -> _stand.update { it.copy(umfrage = u, umfrageDank = false) } }
        }
    }

    // ------------------------------------------------------------ Rundenvorlagen

    fun vorlagenLaden(kennung: String) = viewModelScope.launch {
        runCatching { wege.rundenvorlagen(kennung) }
            .onSuccess { liste -> _stand.update { it.copy(vorlagen = liste) } }
    }

    /**
     * Eine Runde mit dieser Vorlage eröffnen — der Raumcode, oder ein Fehler mit
     * lesbarer Meldung. Beitreten tut der Aufrufer, wie bei jedem neuen Raum.
     */
    suspend fun vorlageStarten(kennung: String, id: String): String =
        wege.rundenvorlageStarten(kennung, id)

    /** Löschen — die Zeile verschwindet erst, wenn der Server zugestimmt hat. */
    fun vorlageLoeschen(kennung: String, id: String) = viewModelScope.launch {
        _stand.update { it.copy(vorlageLoescht = id, vorlagenMeldung = null) }
        runCatching { wege.rundenvorlageLoeschen(kennung, id) }
            .onSuccess { _stand.update { s -> s.copy(vorlagen = s.vorlagen.filter { it.id != id }) } }
            .onFailure { f ->
                _stand.update { it.copy(vorlagenMeldung = f.message ?: "Löschen fehlgeschlagen.") }
            }
        _stand.update { it.copy(vorlageLoescht = null) }
    }

    /** Eine fremde Vorlage per Code übernehmen — als eigene Kopie. */
    fun vorlageEinloesen(kennung: String, code: String) = viewModelScope.launch {
        if (code.isBlank()) return@launch
        _stand.update { it.copy(vorlagenMeldung = null) }
        runCatching { wege.rundenvorlageUebernehmen(kennung, code) }
            .onSuccess { kopie ->
                _stand.update { it.copy(vorlagenMeldung = "„${kopie.name}“ übernommen.") }
                vorlagenLaden(kennung)
            }
            .onFailure { f ->
                _stand.update {
                    it.copy(vorlagenMeldung = f.message ?: "Der Code ließ sich nicht einlösen.")
                }
            }
    }

    fun vorlagenMeldungWegnehmen() = _stand.update { it.copy(vorlagenMeldung = null) }

    /** Der Inhalt zum Bearbeiten — wirft mit lesbarer Meldung. */
    suspend fun vorlageninhalt(kennung: String, id: String): Vorlageninhalt =
        wege.vorlageninhalt(kennung, id)

    /** Die Wachen des Kreises zur Auswahl im Editor — leer, wenn es keine gibt. */
    suspend fun kreiswachen(landkreisId: String): List<Wache> = wege.kreiswachen(landkreisId)

    /** Speichern — die Zeile in der Liste zieht nach. */
    suspend fun vorlageninhaltSichern(
        kennung: String,
        id: String,
        name: String,
        maxSpieler: Int?,
        bestand: List<Vorlagenbesatzung>,
    ): Rundenvorlagenzeile {
        val zeile = wege.vorlageninhaltSichern(kennung, id, name, maxSpieler, bestand)
        _stand.update { s -> s.copy(vorlagen = s.vorlagen.map { if (it.id == zeile.id) zeile else it }) }
        return zeile
    }

    // -------------------------------------------------------------------- Umfrage

    /**
     * Die Antwort senden. Danach steht ein Dank statt der Karte; scheitert es,
     * verschwindet die Karte — eine Umfrage ist keine Fehlermeldung wert.
     */
    fun umfrageAntworten(kennung: String, option: Int) = viewModelScope.launch {
        val umfrage = _stand.value.umfrage ?: return@launch
        if (_stand.value.umfrageLaeuft) return@launch
        _stand.update { it.copy(umfrageLaeuft = true) }
        runCatching { wege.umfrageAntworten(kennung, umfrage.id, option) }
            .onSuccess { _stand.update { it.copy(umfrageDank = true) } }
            .onFailure { _stand.update { it.copy(umfrage = null) } }
        _stand.update { it.copy(umfrageLaeuft = false) }
    }

    // --------------------------------------------------------------- Clanrunde

    /**
     * Den Hinweis auf die laufende Clanrunde abnehmen — nur wer sie eröffnet hat
     * (oder die Leitung) darf. Danach lädt der Aufrufer die Wache neu.
     */
    suspend fun clanrundeSchliessen(kennung: String, gemeinschaftId: String) =
        wege.clanrundeSchliessen(kennung, gemeinschaftId)

    // --------------------------------------------------------- Ausbildungshinweis

    /** „Nicht mehr anzeigen" — gemerkt je Konto, auf diesem Gerät. */
    fun ausbildungHinweisWegnehmen(kennung: String) {
        merker.edit().putBoolean("$HINWEIS_WEG.$kennung", true).apply()
        _stand.update { it.copy(ausbildungHinweisWeg = true) }
    }

    private companion object {
        const val HINWEIS_WEG = "pagerspass.ausbildungHinweisWeg"
    }
}

/** Was der Startbildschirm über die festen Wege hinaus zeigt. */
data class Startstand(
    val vorlagen: List<Rundenvorlagenzeile> = emptyList(),
    val vorlagenMeldung: String? = null,
    /** Die Vorlage, deren Löschen gerade unterwegs ist. */
    val vorlageLoescht: String? = null,
    val umfrage: OffeneUmfrage? = null,
    val umfrageDank: Boolean = false,
    val umfrageLaeuft: Boolean = false,
    val kacheln: List<Startkachel> = emptyList(),
    val footer: List<Footerknopf> = emptyList(),
    /** Ob der einmalige Ausbildungshinweis auf diesem Gerät weggeklickt ist. */
    val ausbildungHinweisWeg: Boolean = false,
)
