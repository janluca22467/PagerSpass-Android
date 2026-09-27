package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Lehrgang
import de.pagerspass.pagerspass.netz.Lehrgangsmodul
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Szenario
import de.pagerspass.pagerspass.netz.Szenarioanfrage
import de.pagerspass.pagerspass.netz.Szenarioeinstellungen
import de.pagerspass.pagerspass.netz.Szenariozeile
import de.pagerspass.pagerspass.netz.Uebungsfahrt
import de.pagerspass.pagerspass.netz.Uebungswege
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Die vorbereiteten Übungen — Liste und Editor, das Gegenstück zum Skriptteil von
 * `web/src/views/UebungenView.vue`.
 *
 * <b>Ein ViewModel je Seite, nicht an der Activity.</b> Die Liste und der Editor
 * bekommen je ihre eigene Instanz (sie hängt am Eintrag der Navigation): Der
 * Entwurf im Editor überlebt damit das Drehen des Geräts und den Abstecher in eine
 * Runde, verschwindet aber, sobald man die Seite verlässt — wie im Web, wo er an
 * der Adresse hängt und nicht an einem Speicher.
 *
 * <b>Die Kennung kommt aus der Ablage</b>, nicht aus einem Aufruf der Seite: Sie
 * ist dieselbe, die die Sitzung dort abgelegt hat, und die Seiten müssen sie
 * dann nicht durchreichen.
 */
class Uebungen(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val wege = Uebungswege(Netz(ablage))

    private val _stand = MutableStateFlow(Uebungsseite())
    val stand: StateFlow<Uebungsseite> = _stand.asStateFlow()

    // -------------------------------------------------------------- Die Liste

    /**
     * Die eigene Liste — und ob sie wirklich beim Server war.
     *
     * Ein Fehlschlag wurde im Web einmal still zu einer leeren Liste, und die Seite
     * sagte „Noch keine Übung", während zwanzig dastanden. Deshalb `geladen` erst
     * nach einer echten Antwort (siehe `Bereich`).
     */
    fun listeLaden() = viewModelScope.launch {
        val kennung = ablage.kennung() ?: return@launch
        _stand.update { it.copy(liste = it.liste.copy(laedt = true, fehler = null)) }

        runCatching { wege.szenarien(kennung) }
            .onSuccess { liste -> _stand.update { it.copy(liste = Bereich(liste, geladen = true)) } }
            .onFailure { f ->
                _stand.update {
                    it.copy(
                        liste = it.liste.copy(laedt = false, geladen = false),
                        fehler = f.message ?: "Deine Übungen ließen sich nicht laden.",
                    )
                }
            }

        // Der Verlauf ist Beiwerk: Bleibt er aus, bleibt er leer.
        runCatching { wege.verlauf(kennung) }
            .onSuccess { verlauf -> _stand.update { it.copy(verlauf = verlauf) } }
    }

    // ------------------------------------------------------------- Der Editor

    /**
     * Eine gespeicherte Übung in den Editor holen.
     *
     * Steht sie schon offen (Drehen, Rückkehr aus der Runde), bleibt der Entwurf
     * stehen — mit allem, was seit dem letzten Speichern geändert wurde.
     */
    fun oeffnen(id: String) = viewModelScope.launch {
        if (_stand.value.offen?.id == id) return@launch
        val kennung = ablage.kennung() ?: return@launch

        _stand.update { it.copy(oeffnet = true, fehler = null) }
        runCatching { wege.szenario(id, kennung) }
            .onSuccess { s -> _stand.update { it.copy(offen = s, oeffnet = false) } }
            .onFailure { f ->
                _stand.update {
                    it.copy(
                        oeffnet = false,
                        fehler = f.message ?: "Diese Übung ließ sich nicht öffnen.",
                    )
                }
            }
    }

    /**
     * Eine neue Übung entsteht leer, aber nicht nackt: Name und Kreis stehen schon,
     * damit das erste Speichern nicht an einer Kleinigkeit scheitert.
     */
    fun neu(katalog: Katalog?) {
        if (_stand.value.offen != null) return
        val fahrzeuge = katalog?.fahrzeuge.orEmpty().map { it.id }.toSet()

        _stand.update {
            it.copy(
                offen = Szenario(
                    id = "",
                    name = "Neue Übung",
                    beschreibung = "",
                    landkreisId = katalog?.landkreise?.firstOrNull()?.id,
                    code = "",
                    aufstellung = listOf("hlf20", "lf10", "rtw", "nef").filter { f -> f in fahrzeuge },
                    zeitachse = emptyList(),
                    einstellungen = Szenarioeinstellungen(
                        tagesalarmstaerke = true,
                        loeschwasser = true,
                        sonderobjekte = true,
                        wiederherstellung = true,
                        einsatzarbeit = true,
                        suchlagen = true,
                        vegetationsbraende = true,
                        gefahrgutlagen = true,
                        telefonischeLeitstelle = false,
                        wetter = "Klar",
                        windrichtung = 0,
                        jahreszeit = null,
                    ),
                    geaendertUm = java.time.Instant.now().toString(),
                ),
            )
        }
    }

    /** Den Entwurf ändern — die Seite reicht nur die Änderung herein. */
    fun aendern(aenderung: (Szenario) -> Szenario) =
        _stand.update { s -> s.copy(offen = s.offen?.let(aenderung)) }

    /**
     * Speichern — und den Code in die Bestätigung schreiben.
     *
     * Er gehört dorthin und nicht nur in den Kopf: Wer gerade gespeichert hat,
     * will ihn weitergeben — und steht dabei am Fuß der Seite.
     */
    fun sichern() = viewModelScope.launch {
        val s = _stand.value.offen ?: return@launch
        val kennung = ablage.kennung() ?: return@launch
        if (_stand.value.speichert) return@launch

        _stand.update { it.copy(speichert = true, meldung = null, fehler = null) }
        runCatching {
            wege.sichern(
                kennung,
                Szenarioanfrage(
                    id = s.id.ifBlank { null },
                    name = s.name,
                    beschreibung = s.beschreibung,
                    landkreisId = s.landkreisId,
                    aufstellung = s.aufstellung,
                    zeitachse = s.zeitachse,
                    einstellungen = s.einstellungen,
                ),
            )
        }.onSuccess { gespeichert ->
            // Erst jetzt hat sie eine Kennung — und ein zweites Speichern schreibt
            // dieselbe Übung fort, statt eine zweite anzulegen.
            _stand.update {
                it.copy(
                    offen = gespeichert,
                    speichert = false,
                    meldung = "Gespeichert. Code zum Weitergeben: ${gespeichert.code}",
                )
            }
        }.onFailure { f ->
            _stand.update {
                it.copy(speichert = false, fehler = f.message ?: "Das hat nicht geklappt.")
            }
        }
    }

    // ------------------------------------------------------------- Handgriffe

    /**
     * Löschen in zwei Schritten — gegen den Fehlgriff.
     *
     * Eine Übung ist ein Abend Arbeit, und es gibt keinen Papierkorb. „Löschen"
     * steht direkt neben „Bearbeiten"; der erste Druck fragt nur nach.
     *
     * @param danach läuft nur nach dem zweiten Druck — etwa: den Editor schließen.
     */
    fun loeschen(id: String, danach: () -> Unit = {}) = viewModelScope.launch {
        val kennung = ablage.kennung() ?: return@launch

        if (_stand.value.loeschGefragt != id) {
            _stand.update { it.copy(loeschGefragt = id) }
            return@launch
        }

        _stand.update { it.copy(loeschGefragt = null) }
        runCatching { wege.loeschen(id, kennung) }
        danach()
        listeLaden()
    }

    /** Eine fremde Übung per Code übernehmen — als eigene Kopie. */
    fun uebernehmen(code: String, danach: () -> Unit = {}) = viewModelScope.launch {
        val sauber = code.trim().uppercase()
        val kennung = ablage.kennung() ?: return@launch
        if (sauber.isEmpty() || _stand.value.unterwegs) return@launch

        _stand.update { it.copy(unterwegs = true, meldung = null, fehler = null) }
        runCatching { wege.uebernehmen(sauber, kennung) }
            .onSuccess { kopie ->
                danach()
                _stand.update {
                    it.copy(unterwegs = false, meldung = "„${kopie.name}\" liegt jetzt in deiner Liste.")
                }
                listeLaden()
            }
            .onFailure { f ->
                _stand.update {
                    it.copy(unterwegs = false, fehler = f.message ?: "Zu diesem Code gibt es nichts.")
                }
            }
    }

    /**
     * Eine Runde eröffnen, die diese Übung fährt — und den Code weitergeben.
     *
     * Ob man sie *leitet* (als Zuschauer mit Regieplatz) oder *selbst fährt*
     * (als Spieler), entscheidet der Aufrufer: Die Runde entsteht beide Male gleich.
     */
    fun rundeAnlegen(id: String, beiCode: (String) -> Unit) = viewModelScope.launch {
        val kennung = ablage.kennung() ?: return@launch
        if (_stand.value.unterwegs) return@launch

        _stand.update { it.copy(unterwegs = true, fehler = null) }
        runCatching { wege.rundeAnlegen(id, kennung) }
            .onSuccess { code ->
                _stand.update { it.copy(unterwegs = false) }
                beiCode(code)
            }
            .onFailure { f ->
                _stand.update {
                    it.copy(unterwegs = false, fehler = f.message ?: "Die Runde ließ sich nicht eröffnen.")
                }
            }
    }

    fun meldungenWegnehmen() = _stand.update { it.copy(meldung = null, fehler = null) }
}

/**
 * Was die Übungsseiten zeigen.
 *
 * <b>Ein eigener Name und nicht „Uebungsstand"</b> — den gibt es schon: Er ist der
 * Stand einer *laufenden* Übung im Raum (`netz/Rundenmodelle.kt`).
 */
data class Uebungsseite(
    val liste: Bereich<List<Szenariozeile>> = Bereich(),
    val verlauf: List<Uebungsfahrt> = emptyList(),
    /** Der Entwurf im Editor — `id` leer, solange er nie gespeichert wurde. */
    val offen: Szenario? = null,
    /** Ob gerade eine gespeicherte Übung geholt wird. */
    val oeffnet: Boolean = false,
    val speichert: Boolean = false,
    /** Läuft ein Aufruf, der eine Runde eröffnet oder eine Kopie holt? Gegen den Doppeldruck. */
    val unterwegs: Boolean = false,
    /** Die Übung, deren Löschen auf die Bestätigung wartet. */
    val loeschGefragt: String? = null,
    val meldung: String? = null,
    val fehler: String? = null,
)

// --------------------------------------------------------------- Lehrgänge

/**
 * Die Lehrgänge — das Gegenstück zum Skriptteil von `LehrgangView.vue`.
 *
 * Abgehakt wird hier nur der **Lesestoff**. Eine Lektion ist erledigt, wenn sie
 * gefahren wurde, und eine Prüfung, wenn sie bestanden ist — beides entscheidet
 * das Dienstende. Ein Knopf, mit dem sich ein Bestanden setzen ließe, wäre ein
 * Zeugnis auf Zuruf.
 */
class Lehrgaenge(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val wege = Uebungswege(Netz(ablage))

    private val _stand = MutableStateFlow(Lehrgangsseite())
    val stand: StateFlow<Lehrgangsseite> = _stand.asStateFlow()

    fun laden() = viewModelScope.launch {
        val kennung = ablage.kennung() ?: return@launch
        _stand.update { it.copy(liste = it.liste.copy(laedt = true, fehler = null)) }

        runCatching { wege.lehrgaenge(kennung) }
            .onSuccess { liste -> _stand.update { it.copy(liste = Bereich(liste, geladen = true)) } }
            .onFailure { f ->
                _stand.update {
                    it.copy(
                        liste = it.liste.copy(
                            laedt = false,
                            geladen = false,
                            fehler = f.message ?: "Die Lehrgänge sind gerade nicht erreichbar.",
                        ),
                    )
                }
            }
    }

    /**
     * Den Lesestoff abhaken — nachdem er geöffnet wurde.
     *
     * Erst öffnen, dann abhaken: Wer den Weg nach draußen nimmt, kommt vielleicht
     * nicht zurück — der Haken soll trotzdem sitzen.
     */
    fun gelesen(lehrgang: Lehrgang, modul: Lehrgangsmodul) = viewModelScope.launch {
        val kennung = ablage.kennung() ?: return@launch
        runCatching { wege.modulErledigen(lehrgang.id, modul.id, kennung) }
        laden()
    }

    /** Die Prüfungsschicht eröffnen — gegen den Doppeldruck, sonst zwei Runden. */
    fun pruefung(lehrgang: Lehrgang, modul: Lehrgangsmodul, beiCode: (String) -> Unit) =
        viewModelScope.launch {
            val kennung = ablage.kennung() ?: return@launch
            if (_stand.value.startet) return@launch

            _stand.update { it.copy(startet = true, fehler = null) }
            runCatching { wege.pruefungAnlegen(lehrgang.id, modul.id, kennung) }
                .onSuccess { code ->
                    _stand.update { it.copy(startet = false) }
                    beiCode(code)
                }
                .onFailure { f ->
                    _stand.update {
                        it.copy(
                            startet = false,
                            fehler = f.message ?: "Die Prüfungsschicht ließ sich nicht eröffnen.",
                        )
                    }
                }
        }
}

/** Was die Lehrgangsseite zeigt. */
data class Lehrgangsseite(
    val liste: Bereich<List<Lehrgang>> = Bereich(),
    /** Wird gerade eine Schicht eröffnet? */
    val startet: Boolean = false,
    val fehler: String? = null,
)
