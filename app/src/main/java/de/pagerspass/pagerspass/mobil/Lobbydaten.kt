package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Raumwache
import de.pagerspass.pagerspass.netz.Rundenvorlagenzeile
import de.pagerspass.pagerspass.netz.Rundenwege
import de.pagerspass.pagerspass.netz.Stichwortset
import de.pagerspass.pagerspass.netz.Wache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Was die Lobby über den Raumzustand hinaus nachlädt — Stichwort-Sets, die
 * eigenen Rundenvorlagen, die Wachen des Kreises und der Runde, ob der Server
 * KI-Funk anbietet.
 *
 * <b>Alles davon darf still scheitern.</b> Eine Lobby ohne Stichwort-Sets ist
 * eine Lobby mit dem Grundkatalog; eine ohne Wachenliste eine, in der man keine
 * Wache abwählt. Keine davon ist eine Fehlermeldung wert.
 */
class Lobbydaten(anwendung: Application) : AndroidViewModel(anwendung) {

    private val wege = Rundenwege(Netz(Ablage(anwendung)))

    private val _stand = MutableStateFlow(Lobbystand())
    val stand: StateFlow<Lobbystand> = _stand.asStateFlow()

    /** Einmal je Lobby: Stichwort-Sets und KI-Funk. */
    fun laden() {
        viewModelScope.launch {
            runCatching { wege.stichwortsets() }
                .onSuccess { s -> _stand.update { it.copy(stichwortsets = s) } }
        }
        viewModelScope.launch {
            runCatching { wege.kiFunkVerfuegbar() }
                .onSuccess { ki -> _stand.update { it.copy(kiFunkVerfuegbar = ki) } }
        }
    }

    // ------------------------------------------------------------ Rundenvorlagen

    /** Die eigenen Vorlagen — nur für die Leitstelle, nur mit Konto. */
    fun vorlagenLaden(kennung: String) = viewModelScope.launch {
        runCatching { wege.rundenvorlagen(kennung) }
            .onSuccess { v -> _stand.update { it.copy(vorlagen = v) } }
    }

    /**
     * Den Reglerstand dieser Lobby als Vorlage speichern — `id` überschreibt eine
     * bestehende. Danach steht der Code zum Weitergeben da.
     */
    fun vorlageSpeichern(kennung: String, id: String?, name: String, raumCode: String) =
        viewModelScope.launch {
            if (_stand.value.vorlageLaeuft || name.isBlank()) return@launch
            _stand.update { it.copy(vorlageLaeuft = true, vorlageMeldung = null, vorlageGespeichert = null) }
            runCatching { wege.rundenvorlageSichern(kennung, id, name.trim(), raumCode) }
                .onSuccess { zeile ->
                    _stand.update { it.copy(vorlageGespeichert = zeile) }
                    vorlagenLaden(kennung)
                }
                .onFailure { f ->
                    _stand.update { it.copy(vorlageMeldung = f.message ?: "Speichern fehlgeschlagen.") }
                }
            _stand.update { it.copy(vorlageLaeuft = false) }
        }

    // -------------------------------------------------------------------- Wachen

    /**
     * Die Wachen des Kreises für die Wachenmaske — einmal je Kreis, dann aus dem
     * Speicher. `null` in `kreiswachen` heißt „noch nicht geladen".
     */
    fun kreiswachenLaden(landkreisId: String) {
        if (_stand.value.kreiswachenFuer == landkreisId && _stand.value.kreiswachen != null) return
        _stand.update { it.copy(kreiswachenFuer = landkreisId, kreiswachen = null) }
        viewModelScope.launch {
            val liste = runCatching { wege.kreiswachen(landkreisId) }.getOrDefault(emptyList())
            _stand.update {
                if (it.kreiswachenFuer == landkreisId) it.copy(kreiswachen = liste) else it
            }
        }
    }

    /** Die Wachen *dieser* Runde — für den Wachendialog der Aufstellung. */
    fun raumwachenLaden(code: String) {
        _stand.update { it.copy(raumwachen = null, raumwachenFehler = null) }
        viewModelScope.launch {
            runCatching { wege.raumwachen(code) }
                .onSuccess { w -> _stand.update { it.copy(raumwachen = w) } }
                .onFailure {
                    _stand.update {
                        it.copy(
                            raumwachen = emptyList(),
                            raumwachenFehler = "Die Wachen dieser Runde sind gerade nicht abrufbar.",
                        )
                    }
                }
        }
    }

    // ------------------------------------------------------------------- Freunde

    /**
     * Einem Mitspieler eine Freundschaftsanfrage stellen. Die Meldung steht nur
     * bei einem Fehler — sonst ist die Anfrage einfach raus.
     */
    fun freundAnfragen(kennung: String, wen: String, danach: () -> Unit = {}) = viewModelScope.launch {
        _stand.update { it.copy(freundMeldung = null) }
        runCatching { wege.freundAnfragen(kennung, wen) }
            .onSuccess {
                _stand.update { it.copy(angefragt = it.angefragt + wen) }
                danach()
            }
            .onFailure { f -> _stand.update { it.copy(freundMeldung = f.message ?: "Die Anfrage ging nicht raus.") } }
    }

    fun meldungWegnehmen() = _stand.update { it.copy(freundMeldung = null, vorlageMeldung = null) }
}

/** Was die Lobby nachgeladen hat. */
data class Lobbystand(
    val stichwortsets: List<Stichwortset> = emptyList(),
    val kiFunkVerfuegbar: Boolean = false,
    val vorlagen: List<Rundenvorlagenzeile> = emptyList(),
    val vorlageLaeuft: Boolean = false,
    val vorlageMeldung: String? = null,
    val vorlageGespeichert: Rundenvorlagenzeile? = null,
    val kreiswachenFuer: String? = null,
    val kreiswachen: List<Wache>? = null,
    val raumwachen: List<Raumwache>? = null,
    val raumwachenFehler: String? = null,
    /** Wem in dieser Lobby schon eine Freundschaftsanfrage gestellt wurde. */
    val angefragt: Set<String> = emptySet(),
    val freundMeldung: String? = null,
)
