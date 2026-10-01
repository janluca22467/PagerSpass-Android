package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Bretteintrag
import de.pagerspass.pagerspass.netz.Freundewege
import de.pagerspass.pagerspass.netz.Fremdprofil
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.GemeinschaftDetail
import de.pagerspass.pagerspass.netz.GemeinschaftsHilfsfrist
import de.pagerspass.pagerspass.netz.Landkreisvorschlag
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Suchtreffer
import de.pagerspass.pagerspass.netz.Vorschlag
import de.pagerspass.pagerspass.netz.Wacheneinstellungen
import de.pagerspass.pagerspass.netz.Wachenplatz
import de.pagerspass.pagerspass.netz.Wachenrang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Der Freundeskreis — Kontakte, fremde Profile und die Extras der Wache.
 *
 * Das Gegenstück zu den Teilen von `stores/freunde.ts`, `stores/brett.ts` und
 * `stores/gemeinschaften.ts`, die die App bis eben nicht kannte: suchen,
 * anfragen, blockieren, melden, fremde Profile, Vorschläge; dazu Einstellungen,
 * Tag, Aussehen, Shop, Rangliste und Laufbahn der Wache.
 *
 * <b>Warum ein eigenes ViewModel und nicht noch mehr in der Sitzung.</b> Die
 * Sitzung trägt schon jede Seite der Tableiste; was hier steht, hat seinen
 * eigenen Zustand (Suchtreffer, das gerade offene fremde Profil, die Rangliste)
 * und seine eigene Meldungszeile. Was die Sitzung davon wissen muss — die
 * Freundesliste ist neu zu holen, die Wache hat sich geändert, das Konto hat
 * Credits ausgegeben —, erfährt sie über die Haken unten, dieselbe Bauform wie
 * bei der Sozialschicht.
 *
 * <b>Die Meldung steht auf der Seite, nicht am Fuß.</b> Wie im Web
 * (`components/freunde/Meldung.vue`): Eine abgelehnte Anfrage gehört an die
 * Stelle, an der man sie gestellt hat, und sie lässt sich wegtippen.
 */
class Freundeskreis(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val wege = Freundewege(Netz(ablage))

    private val _stand = MutableStateFlow(Kreisstand())
    val stand: StateFlow<Kreisstand> = _stand.asStateFlow()

    /** Die Freundesliste hat sich geändert — die Sitzung holt sie neu. */
    var beiFreunden: (() -> Unit)? = null

    /** Die Wache hat sich geändert — Liste und, falls bekannt, Detail neu holen. */
    var beiWache: ((String?) -> Unit)? = null

    /** Der Server hat die frische Detailansicht gleich mitgeschickt. */
    var beiDetail: ((GemeinschaftDetail) -> Unit)? = null

    /** Credits sind ausgegeben oder dazugekommen — das Konto neu holen. */
    var beiKonto: (() -> Unit)? = null

    /** Ein Eintrag am Brett ist weg — das Brett neu holen. */
    var beiBrett: (() -> Unit)? = null

    /** Ein Geschenk wurde geöffnet — Verlauf und Besitzstand neu holen. */
    var beiGeschenk: (() -> Unit)? = null

    // --------------------------------------------------------------- Kontakte

    fun suchen(benutzername: String) = viewModelScope.launch {
        val begriff = benutzername.trim()
        if (begriff.isEmpty()) return@launch
        _stand.update { it.copy(treffer = null, meldung = null, sucht = true) }
        runCatching { wege.suchen(kennung(), begriff) }
            .onSuccess { t -> _stand.update { it.copy(treffer = t, sucht = false) } }
            .onFailure { f ->
                _stand.update { it.copy(sucht = false, meldung = f.message ?: "Die Suche ging nicht.") }
            }
    }

    fun vorschlaegeLaden(neu: Boolean = false) = viewModelScope.launch {
        val vorher = _stand.value.vorschlaege
        if (vorher.laedt || (!neu && vorher.geladen)) return@launch
        _stand.update { it.copy(vorschlaege = vorher.copy(laedt = true, fehler = null)) }
        runCatching { wege.vorschlaege(kennung()) }
            .onSuccess { l -> _stand.update { it.copy(vorschlaege = Bereich(l, geladen = true)) } }
            .onFailure { f ->
                _stand.update {
                    it.copy(vorschlaege = Bereich(fehler = f.message, geladen = true))
                }
            }
    }

    /**
     * Einen Vorschlag weglegen.
     *
     * Der Server hat dafür kein Gedächtnis und soll auch keins bekommen — „ich
     * mag diesen Menschen nicht" muss nirgends neben seinem Namen stehen. Im Web
     * merkt sich das der Browser; hier die Ablage des Geräts — über einen
     * Neustart hinweg —, und mit „Wieder zeigen" ist es jederzeit zurückzuholen.
     */
    fun weglegen(kennung: String) {
        _stand.update { it.copy(weggelegt = it.weggelegt + kennung) }
        weggelegteMerken()
    }

    fun zurueckholen() {
        _stand.update { it.copy(weggelegt = emptySet()) }
        weggelegteMerken()
    }

    private fun weggelegteMerken() {
        val jetzt = _stand.value.weggelegt
        viewModelScope.launch { runCatching { ablage.weggelegteVorschlaegeMerken(jetzt) } }
    }

    init {
        // Was vor dem letzten Beenden weggelegt war, bleibt weggelegt.
        viewModelScope.launch {
            val gemerkt = runCatching { ablage.weggelegteVorschlaege() }.getOrDefault(emptySet())
            if (gemerkt.isNotEmpty()) _stand.update { it.copy(weggelegt = it.weggelegt + gemerkt) }
        }
    }

    /**
     * Eine Anfrage stellen — und den Menschen gleich aus Treffer und Vorschlägen
     * nehmen: Eine gestellte Anfrage ist keine offene Empfehlung mehr.
     */
    fun anfragen(wen: String, danach: () -> Unit = {}) = mitMeldung(danach) {
        wege.anfragen(kennung(), wen)
        _stand.update { s ->
            s.copy(
                treffer = s.treffer?.takeIf { it.kennung != wen },
                vorschlaege = s.vorschlaege.copy(
                    inhalt = s.vorschlaege.inhalt?.filter { it.kennung != wen },
                ),
            )
        }
    }

    fun antworten(wen: String, annehmen: Boolean, danach: () -> Unit = {}) =
        mitMeldung(danach) { wege.antworten(kennung(), wen, annehmen) }

    fun blockieren(wen: String, danach: () -> Unit = {}) =
        mitMeldung(danach) { wege.blockieren(kennung(), wen) }

    /** Freundschaft beenden, Anfrage zurückziehen, Blockade aufheben. */
    fun loesen(wen: String, danach: () -> Unit = {}) =
        mitMeldung(danach) { wege.loesen(kennung(), wen) }

    fun melden(wen: String, grund: String, danach: () -> Unit = {}) = mitMeldung({
        _stand.update { it.copy(hinweis = "Meldung gesendet — danke für den Hinweis.") }
        danach()
    }) { wege.melden(kennung(), wen, grund) }

    // ----------------------------------------------------------------- Profil

    /**
     * Ein fremdes (oder das eigene) Profil samt seiner Zeitleiste am Brett.
     *
     * Beide in einem Zug: Das Brett braucht die Kennung aus dem Profil, und ein
     * Profil, dessen Zeitleiste einen Augenblick später von unten nachrückt,
     * springt unter dem Daumen.
     */
    fun profilLaden(benutzername: String) = viewModelScope.launch {
        val gleiches = _stand.value.profil.inhalt?.benutzername == benutzername
        _stand.update {
            it.copy(
                profilFuer = benutzername,
                profil = if (gleiches) it.profil.copy(laedt = true) else Bereich(laedt = true),
                profilBrett = if (gleiches) it.profilBrett else emptyList(),
            )
        }
        runCatching {
            val k = kennung()
            val p = wege.profil(k, benutzername)
            p to runCatching { wege.brettVon(k, p.kennung).eintraege }.getOrDefault(emptyList())
        }.onSuccess { (p, brett) ->
            if (_stand.value.profilFuer != benutzername) return@onSuccess
            _stand.update { it.copy(profil = Bereich(p, geladen = true), profilBrett = brett) }
        }.onFailure { f ->
            if (_stand.value.profilFuer != benutzername) return@onFailure
            _stand.update {
                it.copy(profil = Bereich(fehler = f.message ?: "Dieses Profil gibt es nicht.", geladen = true))
            }
        }
    }

    /** Die Quittung auf der Zeitleiste — sofort umlegen, dann zum Server. */
    fun profilQuittieren(nr: Long, tun: () -> Unit) {
        _stand.update { s ->
            s.copy(
                profilBrett = s.profilBrett.map {
                    if (it.nr != nr) it else it.copy(
                        vonMirQuittiert = !it.vonMirQuittiert,
                        quittungen = if (it.vonMirQuittiert) it.quittungen - 1 else it.quittungen + 1,
                    )
                },
            )
        }
        tun()
    }

    // ------------------------------------------------------------------ Brett

    /** Den eigenen Eintrag zurücknehmen. */
    fun eintragEntfernen(nr: Long, danach: () -> Unit = {}) = mitMeldung({
        _stand.update { s -> s.copy(profilBrett = s.profilBrett.filter { it.nr != nr }) }
        beiBrett?.invoke()
        danach()
    }) { wege.eintragEntfernen(kennung(), nr) }

    fun eintragMelden(nr: Long) = mitMeldung({
        _stand.update { it.copy(hinweis = "Gemeldet — die Moderation sieht es sich an.") }
    }) { wege.eintragMelden(kennung(), nr, "Am Brett gemeldet") }

    fun kommentarMelden(nr: Long) = mitMeldung({
        _stand.update { it.copy(hinweis = "Gemeldet — die Moderation sieht es sich an.") }
    }) { wege.kommentarMelden(kennung(), nr, "Kommentar am Brett gemeldet") }

    // -------------------------------------------------------------- Geschenke

    fun geschenkOeffnen(nr: Long) = viewModelScope.launch {
        if (_stand.value.oeffnetGeschenk != null) return@launch
        _stand.update { it.copy(oeffnetGeschenk = nr, meldung = null) }
        runCatching { wege.geschenkOeffnen(kennung(), nr) }
            .onSuccess { inhalt ->
                _stand.update {
                    it.copy(
                        oeffnetGeschenk = null,
                        hinweis = "Von ${inhalt.vonName}: ${inhalt.artikelName} — steht jetzt in deinem Profil zur Wahl.",
                    )
                }
                beiGeschenk?.invoke()
            }
            .onFailure { f ->
                _stand.update {
                    it.copy(oeffnetGeschenk = null, meldung = f.message ?: "Das ging gerade nicht.")
                }
            }
    }

    // ------------------------------------------------------------------ Wache

    /** Die öffentliche Liste, eingegrenzt — „Nur, wo ich aufgenommen werde" steht vorn an. */
    fun wachenSuchen(filter: Wachenfilter = _stand.value.filter) = viewModelScope.launch {
        _stand.update { it.copy(filter = filter, oeffentliche = it.oeffentliche.copy(laedt = true)) }
        runCatching {
            wege.oeffentliche(
                suche = filter.suche,
                landkreisId = filter.landkreisId,
                modus = filter.modus,
                nurFreie = filter.nurFreie,
                nurPassende = filter.nurPassende,
            )
        }.onSuccess { l ->
            _stand.update { it.copy(oeffentliche = Bereich(l, geladen = true)) }
        }.onFailure { f ->
            _stand.update { it.copy(oeffentliche = Bereich(fehler = f.message, geladen = true)) }
        }
    }

    fun landkreisvorschlagLaden() = viewModelScope.launch {
        if (_stand.value.landkreisvorschlag != null) return@launch
        runCatching { wege.landkreisvorschlag(kennung()) }
            .onSuccess { v -> _stand.update { it.copy(landkreisvorschlag = v) } }
    }

    fun gruenden(name: String, beschreibung: String?, landkreisId: String?, landkreis: String?) =
        wachenarbeit(null) { wege.gruenden(kennung(), name, beschreibung, landkreisId, landkreis) }

    fun einstellungenSpeichern(id: String, e: Wacheneinstellungen, danach: () -> Unit = {}) =
        wachenarbeit(id, danach) { beiDetail?.invoke(wege.einstellungenSpeichern(kennung(), id, e)) }

    fun codeErneuern(id: String) = wachenarbeit(id) { wege.codeErneuern(kennung(), id) }

    fun aufloesen(id: String, danach: () -> Unit = {}) =
        wachenarbeit(null, danach) { wege.aufloesen(kennung(), id) }

    fun tagSetzen(id: String, tag: String) =
        wachenarbeit(id) { beiDetail?.invoke(wege.tagSetzen(kennung(), id, tag)) }

    fun clanrundeSchliessen(id: String) =
        wachenarbeit(id) { wege.clanrundeSchliessen(kennung(), id) }

    fun ausbauKaufen(id: String, artikelId: String) = wachenarbeit(id, danach = { beiKonto?.invoke() }) {
        beiDetail?.invoke(wege.ausbauKaufen(kennung(), id, artikelId))
    }

    fun wunschSetzen(id: String, artikelId: String, an: Boolean) = wachenarbeit(id, nachladen = false) {
        beiDetail?.invoke(wege.wunschSetzen(kennung(), id, artikelId, an))
    }

    /** Einzahlen kostet eigene Credits — deshalb geht auch das Konto neu. */
    fun einzahlen(id: String, credits: Int) = wachenarbeit(id, danach = { beiKonto?.invoke() }) {
        beiDetail?.invoke(wege.einzahlen(kennung(), id, credits))
    }

    fun schmuckSetzen(id: String, art: String, stueckId: String?) = wachenarbeit(id) {
        beiDetail?.invoke(wege.schmuckSetzen(kennung(), id, art, stueckId))
    }

    fun farbeSetzen(id: String, farbe: Int) = wachenarbeit(id) {
        beiDetail?.invoke(wege.farbeSetzen(kennung(), id, farbe))
    }

    fun hilfsfristenLaden(id: String) = viewModelScope.launch {
        runCatching { wege.hilfsfristen(kennung(), id) }
            .onSuccess { l -> _stand.update { it.copy(hilfsfristen = l) } }
    }

    /** Die Laufbahn ist für alle gleich — einmal geholt, für immer behalten. */
    fun laufbahnLaden() = viewModelScope.launch {
        if (_stand.value.laufbahn.isNotEmpty()) return@launch
        runCatching { wege.wachenlaufbahn() }
            .onSuccess { l -> _stand.update { it.copy(laufbahn = l) } }
    }

    fun ranglisteLaden() = viewModelScope.launch {
        _stand.update { it.copy(rangliste = it.rangliste.copy(laedt = true, fehler = null)) }
        runCatching { wege.rangliste(ablage.kennung()) }
            .onSuccess { l -> _stand.update { it.copy(rangliste = Bereich(l, geladen = true)) } }
            .onFailure { f ->
                _stand.update { it.copy(rangliste = Bereich(fehler = f.message, geladen = true)) }
            }
    }

    fun meldungWegnehmen() = _stand.update { it.copy(meldung = null, hinweis = null) }

    /** Beim Abmelden: nichts vom alten Konto behalten. */
    // Die weggelegten Vorschläge gehören dem Gerät, nicht dem Konto — wie im Web.
    fun vergessen() = _stand.update { Kreisstand(laufbahn = it.laufbahn, weggelegt = it.weggelegt) }

    // --------------------------------------------------------------- Mäntel

    /**
     * Tun, bei Erfolg die Liste neu holen und `danach` rufen, bei Misserfolg
     * den Satz des Servers als Meldung zeigen — der Mantel aus `stores/freunde.ts`
     * (`mitMeldung`).
     */
    private fun mitMeldung(danach: () -> Unit, tun: suspend () -> Unit) = viewModelScope.launch {
        _stand.update { it.copy(meldung = null, hinweis = null, laeuft = true) }
        runCatching { tun() }
            .onSuccess {
                _stand.update { it.copy(laeuft = false) }
                beiFreunden?.invoke()
                danach()
            }
            .onFailure { f ->
                _stand.update {
                    it.copy(laeuft = false, meldung = f.message ?: "Das hat gerade nicht geklappt.")
                }
            }
    }

    /** Derselbe Mantel für die Wache — danach Wache (und Detail) frisch holen. */
    private fun wachenarbeit(
        id: String?,
        danach: () -> Unit = {},
        nachladen: Boolean = true,
        tun: suspend () -> Unit,
    ) = viewModelScope.launch {
        _stand.update { it.copy(meldung = null, hinweis = null, laeuft = true) }
        runCatching { tun() }
            .onSuccess {
                _stand.update { it.copy(laeuft = false) }
                if (nachladen) beiWache?.invoke(id)
                danach()
            }
            .onFailure { f ->
                _stand.update {
                    it.copy(laeuft = false, meldung = f.message ?: "Das hat gerade nicht geklappt.")
                }
            }
    }

    private suspend fun kennung(): String =
        ablage.kennung() ?: throw IllegalStateException("Kein Konto angemeldet.")
}

/** Womit sich die öffentliche Wachenliste eingrenzen lässt — alles auslassbar. */
data class Wachenfilter(
    val suche: String = "",
    val landkreisId: String? = null,
    /** `Offen`, `Antrag`, `Einladung` — oder `null`: egal. */
    val modus: String? = null,
    val nurFreie: Boolean = false,
    /**
     * Vorn an, wie im Web: Ohne diesen Haken besteht die Liste für ein junges
     * Konto zum größten Teil aus Wachen, deren Tür für es zu ist.
     */
    val nurPassende: Boolean = true,
) {
    val eingegrenzt: Boolean
        get() = suche.isNotBlank() || landkreisId != null || modus != null || nurFreie || !nurPassende
}

/** Was der Freundeskreis gerade hält. */
data class Kreisstand(
    val treffer: Suchtreffer? = null,
    val sucht: Boolean = false,
    val vorschlaege: Bereich<List<Vorschlag>> = Bereich(),
    val weggelegt: Set<String> = emptySet(),
    /** Für wen das offene Profil geladen wird — spätere Antworten für andere fallen weg. */
    val profilFuer: String? = null,
    val profil: Bereich<Fremdprofil?> = Bereich(),
    val profilBrett: List<Bretteintrag> = emptyList(),
    val oeffnetGeschenk: Long? = null,
    val filter: Wachenfilter = Wachenfilter(),
    val oeffentliche: Bereich<List<Gemeinschaft>> = Bereich(),
    val landkreisvorschlag: Landkreisvorschlag? = null,
    val hilfsfristen: List<GemeinschaftsHilfsfrist> = emptyList(),
    val laufbahn: List<Wachenrang> = emptyList(),
    val rangliste: Bereich<List<Wachenplatz>> = Bereich(),
    val laeuft: Boolean = false,
    /** Was schiefging — der Satz des Servers. */
    val meldung: String? = null,
    /** Was gut ging und trotzdem gesagt sein will („Meldung gesendet"). */
    val hinweis: String? = null,
)
