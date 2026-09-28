package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.AaoBestand
import de.pagerspass.pagerspass.netz.AaoVorlagenzeile
import de.pagerspass.pagerspass.netz.Aaosicherung
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Begleitergeraete
import de.pagerspass.pagerspass.netz.Leitstellenwege
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Rundenwege
import de.pagerspass.pagerspass.netz.Spielevent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Was der Leitstellentisch über den Raumzustand hinaus nachlädt — die eigenen
 * Ordnungen, die Straßen des Kreises, die Event-Einsätze, den Zugang des
 * Funkbegleiters.
 *
 * <b>Fast alles davon darf still scheitern</b>, wie bei den `Lobbydaten`: Ohne
 * Straßenliste gilt das Ortsverzeichnis des Katalogs, ohne Events fehlt nur die
 * Kulisse. Laut wird nur, was jemand ausdrücklich angestoßen hat — Sichern,
 * Löschen, der QR-Code.
 */
class Leitstellendaten(anwendung: Application) : AndroidViewModel(anwendung) {

    private val netz = Netz(Ablage(anwendung))
    private val wege = Leitstellenwege(netz)
    private val rundenwege = Rundenwege(netz)

    private val _stand = MutableStateFlow(Leitstellenstand())
    val stand: StateFlow<Leitstellenstand> = _stand.asStateFlow()

    // ------------------------------------------------------------ Ordnungen

    /**
     * Holt die Ordnungen, die in dieser Runde gelten, und den ganzen Bestand —
     * `dauerVorlagenLaden` im Store. Ohne Konto passiert nichts.
     */
    fun ordnungenLaden(kennung: String, landkreisId: String?) {
        if (kennung.isBlank()) return
        viewModelScope.launch {
            runCatching { wege.aaoVorlagen(kennung, landkreisId) }
                .onSuccess { v -> _stand.update { it.copy(ordnungen = v) } }
        }
        viewModelScope.launch {
            runCatching { wege.aaoBestand(kennung) }
                .onSuccess { b -> _stand.update { it.copy(bestand = b) } }
        }
    }

    /**
     * Eine Ordnung dauerhaft sichern. Die Antwort ist kein `void` — was der
     * Server zu sagen hat, landet in `ordnungsmeldung`.
     */
    fun ordnungSichern(kennung: String, landkreisId: String?, vorlage: Aaosicherung) {
        if (kennung.isBlank()) {
            melden(Ordnungsmeldung(false, "Dafür braucht es ein angemeldetes Konto."))
            return
        }
        viewModelScope.launch {
            _stand.update { it.copy(sichert = true) }
            runCatching { wege.aaoVorlageSichern(kennung, vorlage) }
                .onSuccess {
                    melden(Ordnungsmeldung(true, "„${vorlage.name}\" ist gespeichert."))
                    ordnungenLaden(kennung, landkreisId)
                }
                .onFailure { f -> melden(Ordnungsmeldung(false, f.message ?: "Speichern fehlgeschlagen.")) }
            _stand.update { it.copy(sichert = false) }
        }
    }

    fun ordnungLoeschen(kennung: String, vorlage: AaoVorlagenzeile) {
        if (kennung.isBlank()) return
        viewModelScope.launch {
            _stand.update { it.copy(loeschtGerade = vorlage.id) }
            runCatching { wege.aaoVorlageLoeschen(kennung, vorlage.id) }
                .onSuccess {
                    // Beide Listen: die der Runde, damit die Auswahl stimmt, und der
                    // Bestand, damit der Zähler nicht eine zu hoch stehen bleibt.
                    _stand.update { s ->
                        val uebrig = s.bestand?.vorlagen.orEmpty().filter { it.id != vorlage.id }
                        s.copy(
                            ordnungen = s.ordnungen.filter { it.id != vorlage.id },
                            bestand = s.bestand?.copy(vorlagen = uebrig, anzahl = uebrig.size),
                        )
                    }
                    melden(Ordnungsmeldung(true, "„${vorlage.name}\" ist gelöscht."))
                }
                .onFailure { f -> melden(Ordnungsmeldung(false, f.message ?: "Löschen fehlgeschlagen.")) }
            _stand.update { it.copy(loeschtGerade = null) }
        }
    }

    /** Eine Meldung, die nach vier (gut) oder neun (schlecht) Sekunden von selbst geht. */
    fun melden(meldung: Ordnungsmeldung) {
        _stand.update { it.copy(ordnungsmeldung = meldung) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(if (meldung.gut) 4_000 else 9_000)
            _stand.update { if (it.ordnungsmeldung === meldung) it.copy(ordnungsmeldung = null) else it }
        }
    }

    // --------------------------------------------------------------- Straßen

    /** Die echten Straßen des Kreises — einmal je Kreis, dann aus dem Speicher. */
    fun strassenSicherstellen(landkreisId: String) {
        if (_stand.value.strassen.containsKey(landkreisId)) return
        _stand.update { it.copy(strassen = it.strassen + (landkreisId to emptyList())) }
        viewModelScope.launch {
            val liste = runCatching { rundenwege.strassen(landkreisId) }.getOrDefault(emptyList())
            _stand.update { it.copy(strassen = it.strassen + (landkreisId to liste)) }
        }
    }

    // ---------------------------------------------------------------- Events

    /** Die Event-Einsätze — einmal je Sitzung, nicht je Einsatz. */
    fun eventsSicherstellen() {
        if (_stand.value.eventsGeholt) return
        _stand.update { it.copy(eventsGeholt = true) }
        viewModelScope.launch {
            runCatching { wege.spielevents() }
                .onSuccess { e -> _stand.update { it.copy(events = e) } }
                .onFailure { _stand.update { it.copy(eventsGeholt = false) } }
        }
    }

    // ------------------------------------------------------- Funkbegleiter

    /** Den QR-Zugang für ein zweites Handy erzeugen. */
    fun begleiterErzeugen(kennung: String, code: String, geraete: Begleitergeraete) {
        if (kennung.isBlank() || code.isBlank()) return
        _stand.update { it.copy(begleiter = Begleiterzugangsstand(laedt = true)) }
        viewModelScope.launch {
            runCatching { wege.funkbegleiterErzeugen(kennung, code, geraete) }
                .onSuccess { z ->
                    val adresse = "${Ablage(getApplication()).server()}/play/mobile/funk/${z.token}"
                    _stand.update { it.copy(begleiter = Begleiterzugangsstand(link = adresse)) }
                }
                .onFailure { f ->
                    _stand.update {
                        it.copy(
                            begleiter = Begleiterzugangsstand(
                                fehler = f.message ?: "Der QR-Code konnte nicht erzeugt werden.",
                            ),
                        )
                    }
                }
        }
    }

    /** Der Funk ist auf das gekoppelte Handy ausgelagert — für diesen Raum. */
    fun auslagern(code: String?) = _stand.update { it.copy(ausgelagertFuer = code) }
}

/** Was der Leitstellentisch nachgeladen hat. */
data class Leitstellenstand(
    /** Die Ordnungen, die in dieser Runde gelten. */
    val ordnungen: List<AaoVorlagenzeile> = emptyList(),
    /** Der ganze Bestand — `null`, solange nicht geladen (oder ohne Konto). */
    val bestand: AaoBestand? = null,
    val sichert: Boolean = false,
    val loeschtGerade: String? = null,
    val ordnungsmeldung: Ordnungsmeldung? = null,
    /** Straßen je Kreis-Id; eine leere Liste heißt „keine" oder „noch unterwegs". */
    val strassen: Map<String, List<String>> = emptyMap(),
    val events: List<Spielevent> = emptyList(),
    val eventsGeholt: Boolean = false,
    val begleiter: Begleiterzugangsstand = Begleiterzugangsstand(),
    /** Für welchen Raum der Funk auf dem gekoppelten Handy liegt. */
    val ausgelagertFuer: String? = null,
) {
    /** Die eigenen Stichwörter: die Ordnungen, die eines tragen. */
    val eigeneStichworte: List<AaoVorlagenzeile> get() = ordnungen.filter { !it.stichwort.isNullOrBlank() }
}

data class Ordnungsmeldung(val gut: Boolean, val text: String)

data class Begleiterzugangsstand(
    val laedt: Boolean = false,
    val link: String? = null,
    val fehler: String? = null,
)
