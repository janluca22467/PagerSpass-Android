package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Gemeinschaftsnachricht
import de.pagerspass.pagerspass.netz.Nachricht
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Spielwege
import de.pagerspass.pagerspass.netz.wert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Die Sozialschicht — Direktnachrichten, Brett-Ereignisse, Wachenchat.
 *
 * <b>Ein eigener Hub, mit Absicht getrennt vom Spiel.</b> `/hub/sozial` trägt
 * alles, was nicht zur laufenden Runde gehört; reißt diese Leitung ab, fährt
 * die Schicht ungestört weiter — dieselbe Trennung wie im Web.
 *
 * <b>Der Handschlag ist zweistufig:</b> erst die WebSocket-Verbindung, dann
 * genau ein `Anmelden` ohne Argumente — die Kennung zieht der Server aus dem
 * Merkmal. Erst danach kommen Ereignisse.
 */
class Sozial(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val wege = Spielwege(Netz(ablage))
    private val draht = Funkverbindung(ablage, pfad = "/hub/sozial")

    private val _stand = MutableStateFlow(Sozialstand())
    val stand: StateFlow<Sozialstand> = _stand.asStateFlow()

    /** Die Haken für die Nachbarn: Wer neu laden muss, erfährt es hier. */
    var beiFreundesliste: (() -> Unit)? = null
    var beiEinladungen: (() -> Unit)? = null
    var beiGemeinschaft: (() -> Unit)? = null
    var beiBrett: (() -> Unit)? = null

    /** Bereich Wachengemeinschaft: ob die Wachenseite gerade zu sehen ist (siehe oben). */
    var wachenseiteOffen: Boolean = false

    private var kennung: String = ""
    private var angemeldet = false

    init {
        // Eine Direktnachricht — an Empfänger UND Absender (Mehrgerät). Eine
        // beantwortete Terminzeile kommt als Ersatz derselben `nr`.
        draht.auf("Nachricht") { argumente ->
            val nachricht = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Nachricht.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update { alt ->
                val partner = if (nachricht.von == kennung) nachricht.an else nachricht.von
                val imOffenen = alt.gespraechMit != null && partner == alt.gespraechMit
                val neuerVerlauf = if (imOffenen) {
                    val ohne = alt.verlauf.filter { it.nr != nachricht.nr }
                    (ohne + nachricht).sortedBy { it.nr }
                } else {
                    alt.verlauf
                }
                alt.copy(verlauf = neuerVerlauf)
            }

            // Im offenen Gespräch gilt Ankommen als Lesen — wie im Web.
            val partner = if (nachricht.von == kennung) nachricht.an else nachricht.von
            if (_stand.value.gespraechMit == partner && nachricht.von != kennung) {
                draht.rufen("Gelesen", wert(partner))
            }
            beiFreundesliste?.invoke()
        }

        // Der Partner hat gelesen — alle eigenen Zeilen an ihn bekommen den Haken.
        draht.auf("NachrichtenGelesen") { argumente ->
            val von = argumente.getOrNull(0)?.let { (it as? JsonPrimitive)?.content } ?: return@auf
            val um = argumente.getOrNull(1)?.let { (it as? JsonPrimitive)?.content } ?: ""
            _stand.update { alt ->
                alt.copy(
                    verlauf = alt.verlauf.map {
                        if (it.an == von && it.gelesenUm == null) it.copy(gelesenUm = um) else it
                    },
                )
            }
        }

        draht.auf("Freundesliste") { beiFreundesliste?.invoke() }
        draht.auf("Einladung") { beiEinladungen?.invoke() }
        draht.auf("Einladungen") { beiEinladungen?.invoke() }

        draht.auf("BrettEintrag") { beiBrett?.invoke() }
        draht.auf("BrettQuittung") { beiBrett?.invoke() }
        draht.auf("BrettKommentar") { beiBrett?.invoke() }

        // Eine Zeile im Wachenchat — neu oder ersetzt (Entfernen sendet
        // dasselbe Ereignis noch einmal, mit leerem Text).
        draht.auf("Gemeinschaftsnachricht") { argumente ->
            val zeile = argumente.firstOrNull()?.let {
                runCatching {
                    Netz.abgabe.decodeFromJsonElement(Gemeinschaftsnachricht.serializer(), it)
                }.getOrNull()
            } ?: return@auf

            _stand.update { alt ->
                if (alt.wachenchatId != zeile.gemeinschaftId) return@update alt
                val ohne = alt.wachenchat.filter { it.nr != zeile.nr }
                alt.copy(wachenchat = (ohne + zeile).sortedBy { it.nr }.takeLast(200))
            }
            // Bereich Wachengemeinschaft: Gelesen ist eine Zeile nur, solange die
            // Wachenseite offen ist. Sonst — und für jede andere Wache — holt der
            // Anstoß die Kurzform samt Ungelesen-Marke für die Tableiste.
            if (wachenseiteOffen && _stand.value.wachenchatId == zeile.gemeinschaftId) {
                draht.rufen("GemeinschaftGelesen", wert(zeile.gemeinschaftId))
            } else {
                beiGemeinschaft?.invoke()
            }
        }

        draht.auf("Gemeinschaften") { beiGemeinschaft?.invoke() }
        draht.auf("Gemeinschaft") { beiGemeinschaft?.invoke() }
        draht.auf("Gemeinschaftsantrag") { beiGemeinschaft?.invoke() }
        draht.auf("Gemeinschaftsaufloesung") { argumente ->
            val name = argumente.getOrNull(1)?.let { (it as? JsonPrimitive)?.content }
            _stand.update {
                it.copy(
                    meldung = "„${name ?: "Deine Wache"}“ wurde aufgelöst.",
                    wachenchat = emptyList(),
                    wachenchatId = null,
                )
            }
            beiGemeinschaft?.invoke()
        }
    }

    /** Verbinden und anmelden — idempotent, mehrfach rufen ist erlaubt. */
    fun verbinden(eigeneKennung: String) = viewModelScope.launch {
        kennung = eigeneKennung
        if (angemeldet && draht.lage == Funkverbindung.Lage.Verbunden) return@launch
        runCatching {
            draht.verbinden()
            draht.frage("Anmelden")
            angemeldet = true
        }
    }

    // -------------------------------------------------------------- Gespräch

    fun gespraechOeffnen(mit: String) = viewModelScope.launch {
        _stand.update { it.copy(gespraechMit = mit, verlauf = emptyList(), laedt = true) }
        runCatching { wege.nachrichtenVerlauf(kennung, mit) }
            .onSuccess { zeilen ->
                _stand.update { it.copy(verlauf = zeilen, laedt = false) }
                draht.rufen("Gelesen", wert(mit))
                beiFreundesliste?.invoke()
            }
            .onFailure { f -> _stand.update { it.copy(laedt = false, meldung = f.message) } }
    }

    /** Schließen gehört dazu — sonst quittiert der Handler weiter als „gelesen". */
    fun gespraechSchliessen() = _stand.update { it.copy(gespraechMit = null, verlauf = emptyList()) }

    fun senden(an: String, text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        val antwort = runCatching {
            draht.frage("Senden", wert(an), wert(text.trim().take(500)), JsonNull, JsonNull)
        }.getOrNull()
        val fehler = (antwort as? JsonPrimitive)?.content
        if (!fehler.isNullOrBlank() && fehler != "null") {
            _stand.update { it.copy(meldung = fehler) }
        }
    }

    fun terminBeantworten(nr: Long, zusagen: Boolean) =
        draht.rufen("TerminBeantworten", JsonPrimitive(nr), wert(zusagen))

    /** In die eigene Runde einladen — der Rückweg des Eingeladenen ist der GameHub. */
    fun einladen(an: String, roomCode: String, alsZuschauer: Boolean = false) =
        viewModelScope.launch {
            val antwort = runCatching {
                draht.frage("Einladen", wert(an), wert(roomCode), wert(alsZuschauer))
            }.getOrNull()
            val fehler = (antwort as? JsonPrimitive)?.content
            _stand.update {
                it.copy(
                    meldung = if (fehler.isNullOrBlank() || fehler == "null") {
                        "Einladung ist raus."
                    } else {
                        fehler
                    },
                )
            }
        }

    // ------------------------------------------------------------ Wachenchat

    fun wachenchatOeffnen(id: String) = viewModelScope.launch {
        _stand.update { it.copy(wachenchatId = id, wachenchat = emptyList()) }
        runCatching { wege.gemeinschaftChat(kennung, id) }
            .onSuccess { zeilen -> _stand.update { it.copy(wachenchat = zeilen) } }
        runCatching {
            draht.frage("GemeinschaftBetreten", wert(id))
            draht.rufen("GemeinschaftGelesen", wert(id))
        }
        beiGemeinschaft?.invoke()
    }

    fun wachenchatSenden(id: String, text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        val antwort = runCatching {
            draht.frage("GemeinschaftSenden", wert(id), wert(text.trim().take(500)))
        }.getOrNull()
        val fehler = (antwort as? JsonPrimitive)?.content
        if (!fehler.isNullOrBlank() && fehler != "null") {
            _stand.update { it.copy(meldung = fehler) }
        }
    }

    fun meldungWegnehmen() = _stand.update { it.copy(meldung = null) }
}

/** Was die Sozialschicht gerade hält. */
data class Sozialstand(
    /** Mit wem das Gespräch offen ist — `null` heißt: keins. */
    val gespraechMit: String? = null,
    val verlauf: List<Nachricht> = emptyList(),
    val laedt: Boolean = false,
    /** Welcher Wachenchat mitgelesen wird. */
    val wachenchatId: String? = null,
    val wachenchat: List<Gemeinschaftsnachricht> = emptyList(),
    val meldung: String? = null,
)
