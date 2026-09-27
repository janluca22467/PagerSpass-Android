package de.pagerspass.pagerspass.mobil

import android.content.SharedPreferences
import de.pagerspass.pagerspass.netz.Bretteintrag
import de.pagerspass.pagerspass.netz.Einladung
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Freundewege
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.netz.Spielwege
import de.pagerspass.pagerspass.netz.Suchtreffer
import de.pagerspass.pagerspass.netz.Vorschlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

/**
 * Freundesliste, Einladungen, Vorschläge, Suche und fremde Profile — das
 * Gegenstück zu `web/src/stores/freunde.ts`.
 *
 * <b>Er gehört der Sozialschicht, nicht der Sitzung.</b> Die Sitzung hält eine
 * eigene Freundesliste für die Stellen, die sie schon immer hatten (Wache,
 * Lobby); dieser Kreis hält, was der Freundebereich darüber hinaus braucht, und
 * wird von denselben Hub-Ereignissen frisch gehalten.
 *
 * Geprüft wird nichts davon hier — Blockaden, Grenzen und die Gegenseitigkeit
 * entscheidet der Server, und was er ablehnt, geht über `meldung` an die Seite.
 */
class Freundeskreis(
    private val bereich: CoroutineScope,
    private val wege: Freundewege,
    private val spielwege: Spielwege,
    private val kennung: () -> String?,
    private val meldung: (String?) -> Unit,
    /** Wo die weggelegten Vorschläge liegen — nur auf diesem Gerät, wie im Web. */
    private val ablage: SharedPreferences?,
) {
    private val _stand = MutableStateFlow(Kreisstand(weggelegt = weggelegteLesen()))
    val stand: StateFlow<Kreisstand> = _stand.asStateFlow()

    /** Für die Nachbarn: Nach jeder Änderung soll auch die Liste der Sitzung frisch werden. */
    var beiGeaendert: (() -> Unit)? = null

    // ------------------------------------------------------------------ Laden

    /** Die Freundesliste neu holen — `true`, wenn es geklappt hat. */
    suspend fun aktualisieren(): Boolean {
        val k = kennung() ?: return false
        _stand.update { it.copy(laedt = true) }
        val ergebnis = runCatching { spielwege.freunde(k) }
        // Inzwischen abgemeldet oder anderes Konto — dann gehört die Antwort niemandem mehr.
        if (kennung() != k) {
            _stand.update { it.copy(laedt = false) }
            return false
        }
        ergebnis
            .onSuccess { liste ->
                _stand.update { it.copy(freunde = liste, geladen = true, laedt = false, fehler = null) }
            }
            .onFailure { f ->
                _stand.update {
                    it.copy(laedt = false, geladen = true, fehler = f.message ?: "Konnte nicht geladen werden.")
                }
            }
        return ergebnis.isSuccess
    }

    suspend fun einladungenAktualisieren(): Boolean {
        val k = kennung() ?: return false
        val ergebnis = runCatching { spielwege.einladungen(k) }
        if (kennung() != k) return false
        ergebnis.onSuccess { liste -> _stand.update { it.copy(einladungen = liste) } }
        return ergebnis.isSuccess
    }

    suspend fun vorschlaegeAktualisieren(): Boolean {
        val k = kennung() ?: return false
        val ergebnis = runCatching { wege.vorschlaege(k) }
        if (kennung() != k) return false
        ergebnis.onSuccess { liste -> _stand.update { it.copy(vorschlaege = liste) } }
        return ergebnis.isSuccess
    }

    /** Die drei Listen gemeinsam — unabhängig voneinander, deshalb nebeneinander. */
    fun standHolen() = bereich.launch {
        val a = async { aktualisieren() }
        val b = async { einladungenAktualisieren() }
        val c = async { vorschlaegeAktualisieren() }
        a.await()
        b.await()
        c.await()
    }

    fun laden() = bereich.launch { aktualisieren() }

    /** Wirft den Stand weg — beim Abmelden oder Kontowechsel. */
    fun leeren() = _stand.update { Kreisstand(weggelegt = it.weggelegt) }

    // ------------------------------------------------------------------ Suche

    fun suchen(benutzername: String) = bereich.launch {
        val k = kennung() ?: return@launch
        if (benutzername.isBlank()) return@launch
        meldung(null)
        _stand.update { it.copy(treffer = null, sucht = true) }
        runCatching { wege.suchen(k, benutzername.trim()) }
            .onSuccess { t -> _stand.update { it.copy(treffer = t) } }
            .onFailure { f -> meldung(f.message ?: "Die Suche ging nicht.") }
        _stand.update { it.copy(sucht = false) }
    }

    // ------------------------------------------------------------- Aktionen

    /**
     * Eine Aktion ausführen, danach neu laden — und eine Ablehnung als Meldung
     * zeigen. `danach` läuft nur, wenn es geklappt hat.
     */
    private fun mitMeldung(
        danach: (() -> Unit)?,
        arbeit: suspend (String) -> Unit,
    ) = bereich.launch {
        val k = kennung() ?: return@launch
        meldung(null)
        runCatching { arbeit(k) }
            .onSuccess {
                aktualisieren()
                beiGeaendert?.invoke()
                danach?.invoke()
            }
            .onFailure { f -> meldung(f.message ?: "Das hat gerade nicht geklappt.") }
    }

    fun anfragen(wen: String, danach: (() -> Unit)? = null) = mitMeldung(danach) { k ->
        wege.anfragen(k, wen)
        // Optimistisch raus aus den Vorschlägen — eine gestellte Anfrage ist keine
        // offene Empfehlung mehr, und die Suche hat ihren Zweck erfüllt.
        _stand.update { s ->
            s.copy(
                treffer = null,
                vorschlaege = s.vorschlaege.filter { it.kennung != wen },
            )
        }
    }

    fun antworten(wen: String, annehmen: Boolean, danach: (() -> Unit)? = null) =
        mitMeldung(danach) { k -> wege.antworten(k, wen, annehmen) }

    fun blockieren(wen: String, danach: (() -> Unit)? = null) =
        mitMeldung(danach) { k -> wege.blockieren(k, wen) }

    /** Freundschaft beenden, Anfrage zurückziehen, Blockade aufheben. */
    fun loesen(wen: String, danach: (() -> Unit)? = null) =
        mitMeldung(danach) { k -> wege.loesen(k, wen) }

    fun melden(wen: String, grund: String, danach: (() -> Unit)? = null) =
        mitMeldung(danach) { k -> wege.melden(k, wen, grund.trim().take(500)) }

    // ------------------------------------------------------------- Einladungen

    /** Eine neue Einladung vom Hub — oben einsortiert, eine ältere mit derselben Nummer ersetzt. */
    fun einladungEin(einladung: Einladung) = _stand.update { s ->
        s.copy(einladungen = listOf(einladung) + s.einladungen.filter { it.nr != einladung.nr })
    }

    /** Der Zettel ist vom Tisch — angenommen oder abgelehnt. */
    fun einladungWeg(nr: Int) = _stand.update { s ->
        s.copy(einladungen = s.einladungen.filter { it.nr != nr })
    }

    // ------------------------------------------------------------- Vorschläge

    /**
     * Einen Vorschlag weglegen. Der Server hat kein Gedächtnis dafür und soll
     * keins bekommen — „ich mag diesen Menschen nicht" gehört nicht in eine
     * Datenbank. Weggelegt wird auf diesem Gerät.
     */
    fun weglegen(wen: String) {
        _stand.update { it.copy(weggelegt = it.weggelegt + wen) }
        weggelegteMerken()
    }

    fun zurueckholen() {
        _stand.update { it.copy(weggelegt = emptyList()) }
        weggelegteMerken()
    }

    private fun weggelegteLesen(): List<String> = runCatching {
        ablage?.getString(SPEICHER_WEG, null)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }.getOrDefault(emptyList())

    private fun weggelegteMerken() {
        runCatching {
            ablage?.edit()?.putString(SPEICHER_WEG, _stand.value.weggelegt.joinToString("\n"))?.apply()
        }
    }

    // ---------------------------------------------------------- Fremde Profile

    /**
     * Ein Profil samt Zeitleiste laden.
     *
     * Gehalten wird je Benutzername: Wer vom einen Profil zum nächsten springt und
     * wieder zurück, soll nicht das zweite unter dem Namen des ersten sehen.
     */
    fun profilLaden(benutzername: String) = bereich.launch {
        val k = kennung() ?: return@launch
        val schluessel = benutzername.lowercase()
        profilAendern(schluessel) { it.copy(laedt = true, fehler = null) }

        runCatching {
            val profil = spielwege.profil(k, benutzername)
            val seite = wege.brettVon(k, profil.kennung)
            profil to seite.eintraege
        }
            .onSuccess { (profil, eintraege) ->
                profilAendern(schluessel) {
                    it.copy(profil = profil, eintraege = eintraege, laedt = false, geladen = true)
                }
            }
            .onFailure { f ->
                profilAendern(schluessel) {
                    it.copy(
                        laedt = false,
                        geladen = true,
                        fehler = f.message ?: "Dieses Profil gibt es nicht.",
                    )
                }
            }
    }

    /** Ob die Einladung aus diesem Profil heraus schon raus ist — der Knopf sagt dann „Eingeladen". */
    fun profilVermerk(benutzername: String, rundeEingeladen: Boolean? = null, wacheEingeladen: Boolean? = null, gemeldet: Boolean? = null) =
        profilAendern(benutzername.lowercase()) {
            it.copy(
                rundeEingeladen = rundeEingeladen ?: it.rundeEingeladen,
                wacheEingeladen = wacheEingeladen ?: it.wacheEingeladen,
                gemeldet = gemeldet ?: it.gemeldet,
            )
        }

    /** In die eigene Wache einladen — Leitung und Zugführer dürfen das. */
    fun inWacheEinladen(benutzername: String, gemeinschaftId: String, wen: String) = bereich.launch {
        val k = kennung() ?: return@launch
        meldung(null)
        runCatching { spielwege.gemeinschaftEinladen(k, gemeinschaftId, wen) }
            .onSuccess { profilVermerk(benutzername, wacheEingeladen = true) }
            .onFailure { f -> meldung(f.message ?: "Die Einladung ging nicht raus.") }
    }

    /** Eine Zeile der Zeitleisten ändern — die Quittung, die am Brett umsprang. */
    fun profilzeileAendern(nr: Long, abbildung: (Bretteintrag) -> Bretteintrag) = _stand.update { s ->
        s.copy(
            profile = s.profile.mapValues { (_, p) ->
                p.copy(eintraege = p.eintraege.map { if (it.nr == nr) abbildung(it) else it })
            },
        )
    }

    fun profilzeileEntfernen(nr: Long) = _stand.update { s ->
        s.copy(
            profile = s.profile.mapValues { (_, p) -> p.copy(eintraege = p.eintraege.filter { it.nr != nr }) },
        )
    }

    private fun profilAendern(schluessel: String, aenderung: (Profilansicht) -> Profilansicht) =
        _stand.update { s ->
            s.copy(profile = s.profile + (schluessel to aenderung(s.profile[schluessel] ?: Profilansicht())))
        }

    private companion object {
        const val SPEICHER_WEG = "vorschlaege.weg"
    }
}

/** Was der Freundebereich gerade hält. */
data class Kreisstand(
    val freunde: List<Freund> = emptyList(),
    /** Ob die Liste einmal da war — erst dann ist „kein Freund" ein Urteil. */
    val geladen: Boolean = false,
    val laedt: Boolean = false,
    val fehler: String? = null,
    val einladungen: List<Einladung> = emptyList(),
    val vorschlaege: List<Vorschlag> = emptyList(),
    /** Die weggelegten Vorschläge — Kennungen, nur auf diesem Gerät. */
    val weggelegt: List<String> = emptyList(),
    val treffer: Suchtreffer? = null,
    val sucht: Boolean = false,
    /** Fremde (und das eigene) Profile, je Benutzername in Kleinschrift. */
    val profile: Map<String, Profilansicht> = emptyMap(),
) {
    /**
     * Bestätigte Freunde als Gesprächsliste: Ungelesenes zuerst, dann wer gerade
     * im Dienst sitzt, dann das jüngste Gespräch, dann der Name. Die Reihenfolge,
     * in der man die Liste tatsächlich abarbeitet.
     */
    val bestaetigte: List<Freund>
        get() = freunde.filter { it.bestaetigt }.sortedWith(
            compareByDescending<Freund> { it.ungelesen > 0 }
                .thenByDescending { it.anwesenheit != null }
                .thenByDescending { zeitwert(it.letzteNachrichtUm) }
                .thenBy { it.anzeigename.lowercase() },
        )

    /** Anfragen, die auf die eigene Antwort warten. */
    val offeneAnfragen: List<Freund> get() = freunde.filter { it.angefragt && !it.vonMir }
    val gestellteAnfragen: List<Freund> get() = freunde.filter { it.angefragt && it.vonMir }
    val blockierte: List<Freund> get() = freunde.filter { it.stand.equals("Blockiert", true) }
    val imDienst: List<Freund> get() = bestaetigte.filter { it.anwesenheit != null }
    val ungelesen: Int get() = freunde.filter { it.bestaetigt }.sumOf { it.ungelesen }

    /** Was an der Tableiste eine Marke verdient — ungelesen, Anfragen, Einladungen. */
    val offenesGesamt: Int get() = ungelesen + offeneAnfragen.size + einladungen.size

    val offeneVorschlaege: List<Vorschlag> get() = vorschlaege.filter { it.kennung !in weggelegt }

    fun freund(kennung: String): Freund? = freunde.firstOrNull { it.kennung == kennung }

    fun profil(benutzername: String): Profilansicht =
        profile[benutzername.lowercase()] ?: Profilansicht()
}

/** Ein geöffnetes Profil — mit seiner Zeitleiste und den Knöpfen, die schon gedrückt sind. */
data class Profilansicht(
    val profil: Profil? = null,
    val eintraege: List<Bretteintrag> = emptyList(),
    val laedt: Boolean = false,
    val geladen: Boolean = false,
    val fehler: String? = null,
    val rundeEingeladen: Boolean = false,
    val wacheEingeladen: Boolean = false,
    val gemeldet: Boolean = false,
)

/** Ein Zeitstempel als Zahl zum Sortieren — Unlesbares zählt als ganz alt. */
private fun zeitwert(roh: String?): Long = roh?.let {
    runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
} ?: 0L
