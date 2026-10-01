package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.Einrichtung
import de.pagerspass.pagerspass.netz.Einrichtungswege
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Was beim Start eines Kontos gefragt wird und danach im Konto einstellbar ist —
 * die Altersfrage, der Einrichtungsbogen und die Übersicht der Maßnahmen in PatSim.
 * Das Gegenstück zu den gleichnamigen Teilen des Spiel-Stores im Web (v6) und zu
 * `composables/massnahmenkatalog.ts`.
 *
 * <b>Eigener Dienst und nicht im Kontodienst</b>, weil er eine andere Regel hat:
 * Jeder Fehlschlag hier lässt die Frage *zu*. Wer gegen einen älteren Server läuft,
 * auf dem es die Wege noch nicht gibt, bekommt einen 404 — und spielt weiter, wie
 * bisher. Ein Dialog, der wegen eines misslungenen Abrufs den Start blockiert,
 * wäre schlimmer als ein Bogen, der beim nächsten Start kommt.
 *
 * Er hängt an der Sitzung wie der Kontodienst: dieselbe Anmeldung, derselbe
 * Lebenslauf, beim Abmelden vergessen.
 */
class Einrichtungsdienst(
    private val wege: Einrichtungswege,
    private val bereich: CoroutineScope,
    private val kennung: () -> String?,
    /** Das Konto neu holen — der Altersstand hängt am Konto. */
    private val kontoNeu: suspend () -> Unit,
) {
    private val _stand = MutableStateFlow(Einrichtungsstand())
    val stand: StateFlow<Einrichtungsstand> = _stand.asStateFlow()

    fun vergessen() {
        _stand.value = Einrichtungsstand()
    }

    /** Bogen und Maßnahmenübersicht einmal je Konto holen — still. */
    fun laden() = bereich.launch {
        val k = kennung() ?: return@launch
        runCatching { wege.einrichtung(k) }
            .onSuccess { e -> _stand.update { it.copy(einrichtung = e) } }
            .onFailure { _stand.update { it.copy(einrichtung = null) } }
        runCatching { wege.massnahmenkatalog(k) }
            .onSuccess { m -> _stand.update { it.copy(massnahmenAlle = m.alle, katalogBekannt = true) } }
    }

    // ---------------------------------------------------------- Altersfrage

    /** Den Link zum Bogen der Eltern holen — für den wartenden Dialog. */
    fun elternlinkLaden() = bereich.launch {
        val k = kennung() ?: return@launch
        runCatching { wege.alter(k) }
            .onSuccess { a -> _stand.update { it.copy(elternlink = a.elternlink) } }
    }

    /**
     * Beantwortet die Altersfrage und holt das Konto neu, damit der Stand stimmt.
     *
     * @param danach Bekommt den Link zum Bogen — bei „unter 18" geht es gleich
     *   dorthin weiter.
     */
    fun alterAngeben(volljaehrig: Boolean, danach: (String?) -> Unit = {}) = tat {
        val k = kennung() ?: return@tat
        val antwort = wege.alterAngeben(k, volljaehrig)
        _stand.update { it.copy(elternlink = antwort.elternlink) }
        kontoNeu()
        danach(antwort.elternlink)
    }

    /** „Schon unterschrieben?" — das Konto neu holen; steht es noch, sagt es der Dialog. */
    fun nachsehen(danach: () -> Unit = {}) = tat {
        kontoNeu()
        danach()
    }

    // ---------------------------------------------------- Einrichtungsbogen

    /** „Einrichtung wiederholen" im Konto — der Bogen geht noch einmal auf. */
    fun wiederholen() = _stand.update { it.copy(wiederholen = true) }

    /**
     * Schreibt die Antworten weg, die ans Konto gehen. Die Maßnahmenübersicht hat
     * ihren eigenen Weg (wie im Konto selbst); Eingabeweg, Kennung und Vorlesen
     * sind Einstellungen des Geräts und stehen schon, bevor das hier läuft.
     */
    fun abschliessen(
        anrufeAnnehmen: Boolean,
        spielrolle: String?,
        vorkenntnisse: String?,
        massnahmenAlle: Boolean?,
        danach: () -> Unit = {},
    ) = tat {
        val k = kennung() ?: return@tat
        if (massnahmenAlle != null) massnahmenSetzenJetzt(k, massnahmenAlle)
        val e = wege.einrichtungSpeichern(k, anrufeAnnehmen, spielrolle, vorkenntnisse, null)
        _stand.update { it.copy(einrichtung = e, wiederholen = false) }
        danach()
    }

    /**
     * „Anrufe annehmen" im Konto — dieselbe Antwort, ohne den Bogen. Zurückgeschickt
     * wird alles andere unverändert, damit der Schalter keine Antwort überschreibt.
     */
    fun anrufeSetzen(an: Boolean) = tat {
        val k = kennung() ?: return@tat
        val e = _stand.value.einrichtung ?: return@tat
        _stand.update { it.copy(einrichtung = e.copy(anrufeAnnehmen = an)) }
        runCatching { wege.einrichtungSpeichern(k, an, e.spielrolle, e.vorkenntnisse, e.patientensimulation) }
            .onSuccess { neu -> _stand.update { it.copy(einrichtung = neu) } }
            .onFailure { f ->
                _stand.update { it.copy(einrichtung = e) }
                throw f
            }
    }

    // -------------------------------------------------- Maßnahmen in PatSim

    /** Einfach oder erweitert — bei einem Fehler zurück auf den alten Wert. */
    fun massnahmenSetzen(alle: Boolean) = bereich.launch {
        val k = kennung() ?: return@launch
        massnahmenSetzenJetzt(k, alle)
    }

    private suspend fun massnahmenSetzenJetzt(k: String, alle: Boolean) {
        val vorher = _stand.value.massnahmenAlle
        _stand.update { it.copy(massnahmenAlle = alle) }
        runCatching { wege.massnahmenkatalogSetzen(k, alle) }
            .onSuccess { m -> _stand.update { it.copy(massnahmenAlle = m.alle, katalogBekannt = true) } }
            .onFailure { _stand.update { it.copy(massnahmenAlle = vorher) } }
    }

    // ------------------------------------------------------------- Mantel

    private fun tat(tun: suspend () -> Unit) = bereich.launch {
        if (_stand.value.laeuft) return@launch
        _stand.update { it.copy(laeuft = true, fehler = null) }
        runCatching { tun() }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message ?: "Das hat nicht geklappt.") } }
        _stand.update { it.copy(laeuft = false) }
    }
}

/**
 * Was der Einrichtungsdienst weiß.
 *
 * @param einrichtung `null`, solange nichts geladen ist — oder der Server den Weg
 *   nicht kennt. Dann gibt es keinen Bogen.
 * @param katalogBekannt Ob der Server die Maßnahmenübersicht kennt; sonst bleibt
 *   die Wahl im Konto weg.
 */
data class Einrichtungsstand(
    val einrichtung: Einrichtung? = null,
    val wiederholen: Boolean = false,
    val massnahmenAlle: Boolean = false,
    val katalogBekannt: Boolean = false,
    val elternlink: String? = null,
    val laeuft: Boolean = false,
    val fehler: String? = null,
) {
    /** Ob der Bogen beantwortet werden will — von selbst oder von Hand wieder geöffnet. */
    val bogenOffen: Boolean get() = wiederholen || einrichtung?.offen == true
}

/** Ob die Altersfrage offen ist oder das Konto auf die Eltern wartet. */
fun altersFrageOffen(altersstand: String?): Boolean =
    altersstand == "Offen" || altersstand == "WartetAufEltern"
