package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.Bretteintrag
import de.pagerspass.pagerspass.netz.Brettkommentar
import de.pagerspass.pagerspass.netz.Freundewege
import de.pagerspass.pagerspass.netz.Spielwege
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Das Brett — das Gegenstück zu `web/src/stores/brett.ts`.
 *
 * <b>Je Kreis ein eigener Stapel.</b> Ein gemeinsamer wäre bequemer und falsch:
 * die drei Kreise (Freunde, Wache, Alle) haben verschiedene Ausschnitte, und wer
 * zwischen ihnen wechselt, soll nicht jedes Mal von vorn laden — und erst recht
 * nicht die Zeilen des einen im anderen sehen.
 *
 * <b>Live, nicht nachgeladen.</b> Vorher lud jedes Hub-Ereignis das ganze Brett
 * neu; jetzt landet eine neue Zeile oben, eine Quittung ändert nur ihre Zahl, und
 * ein Kommentar erscheint im offenen Eintrag — wie im Web.
 *
 * Entschieden wird nichts hier: Sichtbarkeit, Grenzen und Blockaden regelt der
 * Server, und was er ablehnt, geht über `meldung` an die Sozialschicht.
 */
class Brettstapel(
    private val bereich: CoroutineScope,
    private val wege: Freundewege,
    private val spielwege: Spielwege,
    private val kennung: () -> String?,
    private val meldung: (String?) -> Unit,
) {
    private val _stand = MutableStateFlow(Brettstand())
    val stand: StateFlow<Brettstand> = _stand.asStateFlow()

    /**
     * Wer außer dem Stapel noch Zeilen hält — die Zeitleiste eines Profils.
     * Sie bekommt dieselben Änderungen, damit eine Quittung dort nicht stehen
     * bleibt, während sie am Brett schon umgesprungen ist.
     */
    var beiZeile: ((Long, (Bretteintrag) -> Bretteintrag) -> Unit)? = null
    var beiEntfernt: ((Long) -> Unit)? = null

    // ------------------------------------------------------------------ Laden

    /** Einen Kreis zeigen — und nur laden, wenn er noch nicht da ist. */
    fun laden(welcher: String = _stand.value.reiter, erneut: Boolean = false) = bereich.launch {
        val k = kennung() ?: return@launch
        _stand.update { it.copy(reiter = welcher) }
        if (welcher in _stand.value.geladen && !erneut) return@launch

        _stand.update { it.copy(laeuft = true) }
        runCatching { spielwege.brett(k, welcher) }
            .onSuccess { seite ->
                _stand.update {
                    it.copy(
                        stapel = it.stapel + (welcher to seite.eintraege),
                        weiter = it.weiter + (welcher to seite.weiter),
                        geladen = it.geladen + welcher,
                    )
                }
            }
            .onFailure { f -> meldung(f.message ?: "Das Brett ließ sich nicht laden.") }
        _stand.update { it.copy(laeuft = false) }
    }

    /** Die nächste Seite anhängen — geblättert über `weiter`, nicht über einen Versatz. */
    fun mehr() = bereich.launch {
        val k = kennung() ?: return@launch
        val jetzt = _stand.value
        val welcher = jetzt.reiter
        val ab = jetzt.weiter[welcher] ?: return@launch
        if (jetzt.laeuft) return@launch

        _stand.update { it.copy(laeuft = true) }
        runCatching { spielwege.brett(k, welcher, vorNr = ab) }
            .onSuccess { seite ->
                _stand.update {
                    it.copy(
                        stapel = it.stapel + (welcher to (it.stapel[welcher].orEmpty() + seite.eintraege)),
                        weiter = it.weiter + (welcher to seite.weiter),
                    )
                }
            }
            .onFailure { f -> meldung(f.message ?: "Es kam nichts mehr nach.") }
        _stand.update { it.copy(laeuft = false) }
    }

    /**
     * Nach einer Funkstille der Sozialschicht nachholen, was offen ist.
     *
     * Der sichtbare Kreis holt seine erste Seite und mischt sie oben ein, statt
     * zu leeren — wer schon weiter unten gelesen hat, verliert die nachgeladenen
     * Seiten nicht. Die anderen Kreise gelten als ungeladen und holen beim
     * nächsten Wechsel frisch. Ein offener Eintrag holt seine Kommentare neu.
     */
    fun nachholen() = bereich.launch {
        val k = kennung() ?: return@launch
        val welcher = _stand.value.reiter
        val warGeladen = welcher in _stand.value.geladen
        _stand.update { it.copy(geladen = if (warGeladen) setOf(welcher) else emptySet()) }

        runCatching {
            if (warGeladen) {
                val seite = spielwege.brett(k, welcher)
                val neu = seite.eintraege.map { it.nr }.toSet()
                val aelteste = seite.eintraege.minOfOrNull { it.nr } ?: Long.MAX_VALUE
                _stand.update { s ->
                    val rest = s.stapel[welcher].orEmpty().filter { it.nr !in neu && it.nr < aelteste }
                    s.copy(
                        stapel = s.stapel + (welcher to (seite.eintraege + rest)),
                        weiter = if (rest.isEmpty()) s.weiter + (welcher to seite.weiter) else s.weiter,
                    )
                }
            }

            val offen = _stand.value.offenerEintrag
            if (offen != null) {
                val geholt = spielwege.brettKommentare(k, offen)
                _stand.update { if (it.offenerEintrag == offen) it.copy(kommentare = geholt) else it }
            }
        }
        // Kein Netz — der nächste Anlass versucht es wieder; der alte Stand bleibt lesbar.
    }

    /** Wirft den lokalen Stand weg — beim Abmelden oder Kontowechsel. */
    fun leeren() = _stand.update { Brettstand() }

    // -------------------------------------------------------------- Schreiben

    /**
     * Einen Beitrag anschlagen. Der eigene steht sofort oben; der Anstoß über den
     * Hub kommt zusätzlich und ersetzt ihn durch dieselbe Zeile — deshalb wird
     * nach Nummer gefiltert.
     */
    fun schreiben(
        text: String,
        sichtbarkeit: String,
        roomCode: String?,
        danach: (Boolean) -> Unit = {},
    ) = bereich.launch {
        val k = kennung() ?: return@launch
        if (text.isBlank()) return@launch
        meldung(null)
        _stand.update { it.copy(laeuft = true) }

        runCatching { wege.beitragSchreiben(k, text.trim(), sichtbarkeit, roomCode) }
            .onSuccess { eintrag ->
                _stand.update { s ->
                    s.copy(
                        stapel = s.stapel + (
                            "Freunde" to (listOf(eintrag) + s.stapel["Freunde"].orEmpty()
                                .filter { it.nr != eintrag.nr })
                            ),
                    )
                }
                danach(true)
            }
            .onFailure { f ->
                meldung(f.message ?: "Der Beitrag ging nicht raus.")
                danach(false)
            }
        _stand.update { it.copy(laeuft = false) }
    }

    /** Den eigenen Eintrag zurücknehmen — aus allen Stapeln. */
    fun entfernen(nr: Long, danach: (Boolean) -> Unit = {}) = bereich.launch {
        val k = kennung() ?: return@launch
        runCatching { wege.eintragEntfernen(k, nr) }
            .onSuccess {
                _stand.update { s ->
                    s.copy(stapel = s.stapel.mapValues { (_, l) -> l.filter { it.nr != nr } })
                }
                beiEntfernt?.invoke(nr)
                danach(true)
            }
            .onFailure { f ->
                meldung(f.message ?: "Das ging nicht.")
                danach(false)
            }
    }

    /**
     * Quittieren oder die eigene Quittung zurücknehmen — derselbe Aufruf für
     * beides. Gesetzt wird, was der Server antwortet, nicht was man vermutet.
     */
    fun quittung(nr: Long) = bereich.launch {
        val k = kennung() ?: return@launch
        runCatching { wege.quittieren(k, nr) }
            .onSuccess { q ->
                alleZeilen(nr) { it.copy(vonMirQuittiert = q.quittiert, quittungen = q.anzahl) }
            }
            .onFailure { f -> meldung(f.message ?: "Die Quittung ging nicht durch.") }
    }

    // ------------------------------------------------------------- Ein Eintrag

    /** Einen Eintrag samt Kommentaren öffnen — die Seite, auf die eine Mitteilung führt. */
    fun eintragOeffnen(nr: Long) = bereich.launch {
        val k = kennung() ?: return@launch
        _stand.update {
            it.copy(
                offenerEintrag = nr,
                eintrag = null,
                eintragLaedt = true,
                eintragFehler = null,
                kommentare = emptyList(),
            )
        }

        runCatching { spielwege.bretteintrag(k, nr) }
            .onSuccess { eintrag ->
                if (_stand.value.offenerEintrag != nr) return@onSuccess
                _stand.update { it.copy(eintrag = eintrag) }
                runCatching { spielwege.brettKommentare(k, nr) }
                    .onSuccess { liste ->
                        _stand.update { if (it.offenerEintrag == nr) it.copy(kommentare = liste) else it }
                    }
                    .onFailure { f ->
                        meldung(f.message ?: "Die Kommentare ließen sich nicht laden.")
                    }
            }
            .onFailure { f ->
                _stand.update {
                    if (it.offenerEintrag == nr) {
                        it.copy(eintragFehler = f.message ?: "Diesen Eintrag gibt es nicht.")
                    } else {
                        it
                    }
                }
            }
        _stand.update { if (it.offenerEintrag == nr) it.copy(eintragLaedt = false) else it }
    }

    /**
     * Der offene Eintrag gehört der Seite — sonst schriebe der Kommentar-Handler
     * weiter in eine Liste, die niemand mehr ansieht.
     */
    fun eintragSchliessen(nr: Long) = _stand.update {
        if (it.offenerEintrag == nr) {
            it.copy(offenerEintrag = null, eintrag = null, kommentare = emptyList())
        } else {
            it
        }
    }

    fun kommentarSchreiben(nr: Long, text: String, danach: (Boolean) -> Unit = {}) = bereich.launch {
        val k = kennung() ?: return@launch
        if (text.isBlank()) return@launch
        meldung(null)

        runCatching { spielwege.brettKommentieren(k, nr, text.trim()) }
            .onSuccess { kommentar ->
                _stand.update { s ->
                    if (s.offenerEintrag == nr) {
                        s.copy(kommentare = s.kommentare.filter { it.nr != kommentar.nr } + kommentar)
                    } else {
                        s
                    }
                }
                alleZeilen(nr) { it.copy(kommentare = it.kommentare + 1) }
                danach(true)
            }
            .onFailure { f ->
                meldung(f.message ?: "Der Kommentar ging nicht raus.")
                danach(false)
            }
    }

    /** Einen Eintrag oder einen Kommentar melden — mit festem Grund, wie im Web. */
    fun melden(nr: Long, grund: String, alsKommentar: Boolean = false) = bereich.launch {
        val k = kennung() ?: return@launch
        runCatching {
            if (alsKommentar) wege.kommentarMelden(k, nr, grund) else wege.eintragMelden(k, nr, grund)
        }.onFailure { f -> meldung(f.message ?: "Die Meldung ging nicht raus.") }
    }

    // ------------------------------------------------------------------- Live

    /**
     * Eine neue Zeile vom Hub. In welchen Kreis sie gehört, weiß der Server
     * besser — sie landet oben in den geladenen Kreisen, der Rest ergibt sich
     * beim nächsten Laden.
     */
    fun eintragEin(eintrag: Bretteintrag) = _stand.update { s ->
        var stapel = s.stapel
        for (r in KREISE) {
            if (r !in s.geladen) continue
            if (r == "Alle" && eintrag.sichtbarkeit != "Oeffentlich") continue
            if (r == "Wache" && eintrag.sichtbarkeit == "Freunde" && !eintrag.vonMir) continue
            stapel = stapel + (r to (listOf(eintrag) + stapel[r].orEmpty().filter { it.nr != eintrag.nr }))
        }
        s.copy(stapel = stapel)
    }

    /**
     * Nur die Zahl, nicht der eigene Haken: ob man selbst quittiert hat, weiß
     * dieser Client aus seiner eigenen Antwort — die Zahl kommt von allen anderen.
     */
    fun quittungEin(nr: Long, anzahl: Int) = alleZeilen(nr) { it.copy(quittungen = anzahl) }

    fun kommentarEin(kommentar: Brettkommentar) {
        // Der eigene Kommentar ist über die REST-Antwort schon gezählt.
        val schonDa = _stand.value.kommentare.any { it.nr == kommentar.nr }
        if (!schonDa) alleZeilen(kommentar.eintragNr) { it.copy(kommentare = it.kommentare + 1) }

        _stand.update { s ->
            if (s.offenerEintrag == kommentar.eintragNr) {
                s.copy(kommentare = s.kommentare.filter { it.nr != kommentar.nr } + kommentar)
            } else {
                s
            }
        }
    }

    /** Ob ein Eintrag bekanntermaßen einem selbst gehört — für die Mitteilung. */
    fun istEigener(nr: Long): Boolean? {
        val s = _stand.value
        s.eintrag?.takeIf { it.nr == nr }?.let { return it.vonMir }
        s.stapel.values.forEach { l -> l.firstOrNull { it.nr == nr }?.let { return it.vonMir } }
        return null
    }

    private fun alleZeilen(nr: Long, abbildung: (Bretteintrag) -> Bretteintrag) {
        _stand.update { s ->
            s.copy(
                stapel = s.stapel.mapValues { (_, l) -> l.map { if (it.nr == nr) abbildung(it) else it } },
                eintrag = s.eintrag?.let { if (it.nr == nr) abbildung(it) else it },
            )
        }
        beiZeile?.invoke(nr, abbildung)
    }

    private companion object {
        val KREISE = listOf("Freunde", "Wache", "Alle")
    }
}

/** Was das Brett gerade hält. */
data class Brettstand(
    /** Der gezeigte Kreis: `Freunde`, `Wache` oder `Alle`. */
    val reiter: String = "Freunde",
    val stapel: Map<String, List<Bretteintrag>> = emptyMap(),
    val weiter: Map<String, Long?> = emptyMap(),
    val geladen: Set<String> = emptySet(),
    val laeuft: Boolean = false,
    /** Der Eintrag, dessen Seite gerade offen ist — und sein Stand. */
    val offenerEintrag: Long? = null,
    val eintrag: Bretteintrag? = null,
    val eintragLaedt: Boolean = false,
    val eintragFehler: String? = null,
    val kommentare: List<Brettkommentar> = emptyList(),
) {
    val eintraege: List<Bretteintrag> get() = stapel[reiter].orEmpty()
    val kannMehr: Boolean get() = weiter[reiter] != null
    val istGeladen: Boolean get() = reiter in geladen
}
