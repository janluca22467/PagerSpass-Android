package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.Codevorschau
import de.pagerspass.pagerspass.netz.Discordstatus
import de.pagerspass.pagerspass.netz.Einwilligung
import de.pagerspass.pagerspass.netz.Geschenkinhalt
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kontowege
import de.pagerspass.pagerspass.netz.Postfach
import de.pagerspass.pagerspass.netz.Premiumstand
import de.pagerspass.pagerspass.netz.Schenkfreund
import de.pagerspass.pagerspass.netz.Shop
import de.pagerspass.pagerspass.netz.Sitzungsuebersicht
import de.pagerspass.pagerspass.netz.Werbung
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Der Kontodienst — was die Konto-Zentrale, der Shop und die Privatsphäre über
 * die Sitzung hinaus wissen und tun. Das Gegenstück zu den Konto-Abschnitten von
 * `KontoView.vue`, `ShopView.vue` und `PrivatsphaereView.vue`.
 *
 * <b>Er hängt an der Sitzung und nicht an der Oberfläche.</b> Die Sitzung legt ihn
 * an und reicht ihm ihren Lebenslauf (`bereich`); damit überlebt er das Drehen des
 * Geräts wie sie, und beim Abmelden räumt sie ihn mit weg (`vergessen`). Ein
 * eigenes ViewModel wäre ein zweiter Ort, der wissen müsste, wer angemeldet ist.
 *
 * <b>Jede Karte hat ihre eigene Rückmeldung.</b> Im Web steht der Fehler einer
 * Karte in der Karte und nicht am Fuß der Seite — wer im Postfach einen falschen
 * Code eingibt, soll die Meldung unter dem Codefeld lesen und nicht über der
 * Tableiste. Deshalb trägt der Stand eine Rückmeldung je Schlüssel (`"postfach"`,
 * `"passwort"`, `"werbung"` …) statt eines einzigen Fehlers.
 *
 * <b>Was er nicht selbst hält, meldet er zurück:</b> ein neues Konto (Benutzername,
 * Premium, Credits), einen frischen Shop, eine veränderte Garage oder Freundesliste.
 * Das gehört der Sitzung, und zwei Kopien desselben Kontos zeigten früher oder
 * später zwei Kontostände.
 */
class Kontodienst(
    private val wege: Kontowege,
    private val bereich: CoroutineScope,
    private val kennung: () -> String?,
    private val kontoAendern: ((Konto) -> Konto) -> Unit,
    private val kontoSetzen: (Konto) -> Unit,
    private val shopSetzen: (Shop) -> Unit,
    private val garageNeu: () -> Unit,
    private val freundeNeu: () -> Unit,
) {
    private val _stand = MutableStateFlow(Kontostand())
    val stand: StateFlow<Kontostand> = _stand.asStateFlow()

    /** Alles vergessen — beim Abmelden, Löschen und Serverwechsel. */
    fun vergessen() {
        _stand.value = Kontostand()
    }

    fun rueckmeldungWeg(schluessel: String) =
        _stand.update { it.copy(rueckmeldungen = it.rueckmeldungen - schluessel) }

    // ------------------------------------------------------------- Konto

    /**
     * Das Konto still nachladen — Credits, Premium, Benutzername. Fehler bleiben
     * stumm: Es steht ja schon ein Konto da, und ein Hinweis „nicht erreichbar"
     * über der Kontoseite wäre eine Meldung ohne Handlung.
     */
    fun kontoLaden() = bereich.launch {
        val k = kennung() ?: return@launch
        runCatching { wege.konto(k) }.onSuccess(kontoSetzen)
    }

    // ------------------------------------------------------------- Premium

    /**
     * Den Abo-Stand holen — und ihn am Konto nachziehen.
     *
     * <b>Immer neu, nie aus dem Speicher.</b> Die Seite ruft das beim Öffnen und
     * jedes Mal, wenn die App aus dem Hintergrund zurückkommt: Genau dann kann sich
     * etwas geändert haben, denn gekauft und gekündigt wird auf der Webseite.
     */
    fun premiumLaden() = laden(
        holen = { _stand.value.premium },
        setzen = { b -> _stand.update { it.copy(premium = b) } },
        tun = { k ->
            wege.premium(k).also { p ->
                kontoAendern { it.copy(premiumAktiv = p.aktiv, premiumBis = p.bis) }
            }
        },
    )

    // ------------------------------------------------------------ Postfach

    fun postfachLaden() = laden(
        holen = { _stand.value.postfach },
        setzen = { b -> _stand.update { it.copy(postfach = b) } },
        tun = { wege.postfach(it) },
    )

    /**
     * Eine Adresse eintragen; danach steht sofort das Codefeld offen.
     *
     * @param pflicht Ob das aus dem Pflichtdialog kommt — nur dann hält er sich für
     *   den Code offen. Aus der Kontokarte heraus steht das Codefeld in der Karte.
     */
    fun emailSetzen(email: String, passwort: String, pflicht: Boolean = false, danach: () -> Unit = {}) =
        tat(POSTFACH, danach) { k ->
            val antwort = wege.emailSetzen(k, email.trim(), passwort)
            _stand.update {
                it.copy(
                    postfach = Bereich(antwort.postfach, geladen = true),
                    emailBestaetigungOffen = pflicht || it.emailBestaetigungOffen,
                )
            }
            antwort.hinweis ?: "Der Bestätigungscode ist unterwegs."
        }

    fun emailCodeAnfordern() = tat(POSTFACH) { k ->
        wege.emailCodeAnfordern(k)
        "Ein neuer Code ist unterwegs."
    }

    fun emailBestaetigen(code: String, danach: () -> Unit = {}) = tat(POSTFACH, danach) { k ->
        val postfach = wege.emailBestaetigen(k, code.trim())
        _stand.update {
            it.copy(postfach = Bereich(postfach, geladen = true), emailBestaetigungOffen = false)
        }
        "Deine Adresse ist bestätigt."
    }

    /** „Später" — die Pflicht ist mit dem Eintragen erfüllt, bestätigt wird im Konto. */
    fun emailBestaetigungSpaeter() = _stand.update { it.copy(emailBestaetigungOffen = false) }

    fun newsletter(an: Boolean) = tat(POSTFACH) { k ->
        val postfach = wege.newsletter(k, an)
        _stand.update { it.copy(postfach = Bereich(postfach, geladen = true)) }
        if (an) {
            "Du bekommst den Newsletter. Abmelden geht jederzeit — auch über den Link in " +
                "jeder Ausgabe."
        } else {
            "Du bekommst keinen Newsletter mehr."
        }
    }

    /** Der alte Anmeldeschutz — einschalten lässt er sich nicht mehr, nur aus. */
    fun anmeldeschutzAus(passwort: String, danach: () -> Unit = {}) = tat(POSTFACH, danach) { k ->
        val postfach = wege.zweiFaktor(k, false, passwort)
        _stand.update { it.copy(postfach = Bereich(postfach, geladen = true)) }
        "Der Anmeldeschutz ist aus."
    }

    // --------------------------------------------------------- Sicherheit

    fun passwortAendern(aktuelles: String, neues: String, wiederholt: String, danach: () -> Unit) {
        if (neues != wiederholt) {
            melden(PASSWORT, "Die neuen Passwörter stimmen nicht überein.", fehler = true)
            return
        }
        tat(PASSWORT, danach) { k ->
            wege.passwortAendern(k, aktuelles, neues)
            "Dein Passwort ist geändert."
        }
    }

    fun benutzernameAendern(neu: String, danach: () -> Unit) = tat(BENUTZERNAME, danach) { k ->
        kontoSetzen(wege.benutzernameAendern(k, neu.trim()))
        "Dein Benutzername ist geändert."
    }

    // ---------------------------------------------------------- Spielweise

    fun simulationLaden() = bereich.launch {
        val k = kennung() ?: return@launch
        runCatching { wege.patientensimulation(k) }
            .onSuccess { s -> _stand.update { it.copy(patientensimulation = s.an) } }
    }

    /**
     * Den Schalter umlegen — und bei einem Fehler zurückstellen. Sonst behauptet
     * er einen Zustand, den der Server nie bekommen hat.
     */
    fun simulationSetzen(an: Boolean) {
        if (_stand.value.laeuft != null) return
        _stand.update { it.copy(patientensimulation = an) }
        tat(SIMULATION, beiFehler = { _stand.update { it.copy(patientensimulation = !an) } }) { k ->
            val s = wege.patientensimulationSetzen(k, an)
            _stand.update { it.copy(patientensimulation = s.an) }
            null
        }
    }

    /** Die Antwort auf die Aufzeichnungsfrage — am Konto nachgezogen, damit sie nicht wiederkommt. */
    fun analyseSetzen(zugestimmt: Boolean) = tat(ANALYSE) { k ->
        val stand = wege.analyseSetzen(k, zugestimmt)
        kontoAendern { it.copy(analyseZustimmung = stand.zugestimmt ?: zugestimmt) }
        null
    }

    // ---------------------------------------------------------------- Welt

    /** Ob es eine Leitstelle gibt — nur mit Premium, sonst antwortet die Welt mit 403. */
    fun weltPruefen(premium: Boolean) = bereich.launch {
        val k = kennung() ?: return@launch
        val da = premium && wege.weltVorhanden(k)
        _stand.update { it.copy(weltVorhanden = da) }
    }

    fun weltZuruecksetzen(passwort: String, danach: () -> Unit) = tat(WELT, danach) { k ->
        wege.weltZuruecksetzen(k, passwort)
        _stand.update { it.copy(weltVorhanden = false) }
        "Deine Welt ist zurückgesetzt. Du kannst neu gründen."
    }

    // --------------------------------------------------------------- Bugs

    fun bugMelden(titel: String, beschreibung: String, hergang: String, seite: String, danach: () -> Unit) =
        tat(BUG, danach) { k ->
            wege.bugMelden(k, titel.trim(), beschreibung.trim(), hergang.trim(), seite.trim())
            "Danke — deine Meldung ist angekommen."
        }

    // ------------------------------------------------------------ Werbung

    fun werbungLaden() = laden(
        holen = { _stand.value.werbung },
        setzen = { b -> _stand.update { it.copy(werbung = b) } },
        tun = { wege.werbung(it) },
    )

    fun werbecodeErzeugen() = tat(WERBUNG) { k ->
        val w = wege.werbecodeErzeugen(k)
        _stand.update { it.copy(werbung = Bereich(w, geladen = true)) }
        null
    }

    /** Einen fremden Code einlösen — der Gutschein liegt danach in der Garage. */
    fun werbungEinloesen(code: String, danach: () -> Unit) = tat(WERBUNG, danach) { k ->
        val w = wege.werbungEinloesen(k, code.trim())
        _stand.update { it.copy(werbung = Bereich(w, geladen = true)) }
        garageNeu()
        null
    }

    // -------------------------------------------------------------- Codes

    /**
     * Einen Aktionscode einlösen. Das Ergebnis bleibt als Rückmeldung stehen, statt
     * zu verschwinden — was ein Code gebracht hat, liest man zweimal.
     */
    fun codeEinloesen(code: String, danach: () -> Unit = {}) = tat(CODE, danach) { k ->
        val ertrag = wege.codeEinloesen(k, code.trim())
        shopSetzen(ertrag.shop)
        if (ertrag.wahlen > 0) garageNeu()
        "Eingelöst: ${ertrag.text}"
    }

    /**
     * Die Vorschau eines Creator-Codes. Ein 404 ist hier kein Fehler: Die meisten
     * Codes haben keinen Creator, und dann gibt es eben keine Vorschau.
     */
    fun codevorschau(code: String) = bereich.launch {
        val sauber = code.trim()
        if (sauber.length < 3) {
            _stand.update { it.copy(codevorschau = null) }
            return@launch
        }
        val vorschau = runCatching { wege.codevorschau(sauber) }.getOrNull()
        _stand.update { it.copy(codevorschau = vorschau) }
    }

    fun codevorschauWeg() = _stand.update { it.copy(codevorschau = null) }

    // ---------------------------------------------------------- Geschenke

    fun schenkfreundeLaden() = laden(
        holen = { _stand.value.schenkfreunde },
        setzen = { b -> _stand.update { it.copy(schenkfreunde = b) } },
        tun = { wege.schenkfreunde(it) },
    )

    fun schenken(an: Schenkfreund, artikelId: String, artikelName: String) =
        tat(SCHENKEN, laufschluessel = "$SCHENKEN:${an.kennung}") { k ->
            shopSetzen(wege.schenken(k, an.kennung, artikelId))
            "$artikelName ist auf dem Weg zu ${an.anzeigename}."
        }

    /** Ein Geschenk öffnen — erst hier erfährt man, was drin ist. */
    fun geschenkOeffnen(nr: Long, danach: () -> Unit = {}) = tat(GESCHENK, danach) { k ->
        val inhalt = wege.geschenkOeffnen(k, nr)
        _stand.update { it.copy(geschenk = inhalt) }
        null
    }

    fun geschenkWeg() = _stand.update { it.copy(geschenk = null) }

    // -------------------------------------------------------- Datenschutz

    fun sitzungenLaden() = laden(
        holen = { _stand.value.sitzungen },
        setzen = { b -> _stand.update { it.copy(sitzungen = b) } },
        tun = { wege.sitzungen(it) },
    )

    fun andereSitzungenBeenden() = tat(SITZUNGEN) { k ->
        wege.andereSitzungenBeenden(k)
        _stand.update { it.copy(sitzungen = Bereich(wege.sitzungen(k), geladen = true)) }
        "Alle anderen Geräte sind abgemeldet."
    }

    /** Den Datenauszug holen und dem Aufrufer als Text geben — er entscheidet, wohin. */
    fun datenauszug(beiText: (String) -> Unit) = tat(AUSZUG) { k ->
        beiText(wege.datenauszug(k))
        null
    }

    fun einwilligungenLaden() = laden(
        holen = { _stand.value.einwilligungen },
        setzen = { b -> _stand.update { it.copy(einwilligungen = b) } },
        tun = { wege.einwilligungen(it) },
    )

    fun einwilligungWiderrufen(id: String) =
        tat(EINWILLIGUNG, laufschluessel = "$EINWILLIGUNG:$id") { k ->
            wege.einwilligungWiderrufen(k, id)
            _stand.update { it.copy(einwilligungen = Bereich(wege.einwilligungen(k), geladen = true)) }
            null
        }

    fun blockadeAufheben(wen: String) = tat(BLOCKADE, laufschluessel = "$BLOCKADE:$wen") { k ->
        wege.freundLoesen(k, wen)
        freundeNeu()
        null
    }

    // ------------------------------------------------------------ Discord

    fun discordLaden() = laden(
        holen = { _stand.value.discord },
        setzen = { b -> _stand.update { it.copy(discord = b) } },
        tun = { wege.discordStatus(it) },
    )

    /** Die Zustimmungsseite von Discord holen; der Aufrufer öffnet sie im Browser. */
    fun discordStarten(beiAdresse: (String) -> Unit) = tat(DISCORD) { k ->
        beiAdresse(wege.discordStart(k).url)
        null
    }

    fun discordLoesen() = tat(DISCORD) { k ->
        wege.discordLoesen(k)
        _stand.update { it.copy(discord = Bereich(wege.discordStatus(k), geladen = true)) }
        "Die Verknüpfung ist gelöst."
    }

    // ------------------------------------------------- Passwort vergessen

    /**
     * Einen Code anfordern — ohne Anmeldung. Die Antwort ist immer derselbe Satz
     * (siehe `Kontowege.passwortVergessen`), und der nächste Schritt kommt immer.
     */
    fun passwortVergessen(benutzer: String, danach: () -> Unit) {
        if (benutzer.isBlank()) {
            melden(VERGESSEN, "Bitte deinen Benutzernamen oder deine E-Mail-Adresse angeben.", true)
            return
        }
        tat(VERGESSEN, danach, ohneKonto = true) { wege.passwortVergessen(benutzer.trim()).meldung }
    }

    fun passwortNeu(benutzer: String, code: String, passwort: String, danach: () -> Unit) =
        tat(VERGESSEN, danach, ohneKonto = true) {
            wege.passwortNeu(benutzer.trim(), code.trim(), passwort)
            "Dein neues Passwort steht. Melde dich jetzt damit an."
        }

    // ------------------------------------------------------------- Mantel

    private fun melden(schluessel: String, text: String, fehler: Boolean) = _stand.update {
        it.copy(rueckmeldungen = it.rueckmeldungen + (schluessel to Rueckmeldung(text, fehler)))
    }

    /**
     * Der gemeinsame Mantel um jede Handlung — dasselbe wie `arbeiten` in der
     * Sitzung, nur mit der Rückmeldung an ihrer Karte.
     *
     * @param laufschluessel Was gerade läuft, wenn es feiner ist als die Karte —
     *   „diese eine Einwilligung", nicht „irgendeine".
     * @param tun Gibt den Satz zurück, der bei Erfolg stehen soll, oder `null`.
     */
    private fun tat(
        schluessel: String,
        danach: () -> Unit = {},
        laufschluessel: String = schluessel,
        ohneKonto: Boolean = false,
        beiFehler: () -> Unit = {},
        tun: suspend (String) -> String?,
    ) = bereich.launch {
        if (_stand.value.laeuft != null) return@launch
        val k = kennung() ?: if (ohneKonto) "" else return@launch

        _stand.update { it.copy(laeuft = laufschluessel, rueckmeldungen = it.rueckmeldungen - schluessel) }
        runCatching { tun(k) }
            .onSuccess { satz ->
                if (satz != null) melden(schluessel, satz, fehler = false)
                danach()
            }
            .onFailure { f ->
                beiFehler()
                melden(schluessel, f.message ?: "Das hat nicht geklappt.", fehler = true)
            }
        _stand.update { it.copy(laeuft = null) }
    }

    /** Einen Bereich holen — immer neu; der alte Inhalt bleibt stehen, bis der neue da ist. */
    private fun <T> laden(
        holen: () -> Bereich<T>,
        setzen: (Bereich<T>) -> Unit,
        tun: suspend (String) -> T,
    ) = bereich.launch {
        val k = kennung() ?: return@launch
        val vorher = holen()
        if (vorher.laedt) return@launch

        setzen(vorher.copy(laedt = true, fehler = null))
        runCatching { tun(k) }
            .onSuccess { setzen(Bereich(inhalt = it, laedt = false, geladen = true)) }
            .onFailure { f ->
                setzen(holen().copy(laedt = false, geladen = true, fehler = f.message ?: "Konnte nicht geladen werden."))
            }
    }

    companion object {
        const val POSTFACH = "postfach"
        const val PASSWORT = "passwort"
        const val BENUTZERNAME = "benutzername"
        const val SIMULATION = "simulation"
        const val ANALYSE = "analyse"
        const val WELT = "welt"
        const val BUG = "bug"
        const val WERBUNG = "werbung"
        const val CODE = "code"
        const val SCHENKEN = "schenken"
        const val GESCHENK = "geschenk"
        const val SITZUNGEN = "sitzungen"
        const val AUSZUG = "auszug"
        const val EINWILLIGUNG = "einwilligung"
        const val BLOCKADE = "blockade"
        const val DISCORD = "discord"
        const val VERGESSEN = "vergessen"
    }
}

/** Ein Satz unter einer Karte — grün, wenn es geklappt hat, rot, wenn nicht. */
data class Rueckmeldung(val text: String, val fehler: Boolean)

/**
 * Was der Kontodienst weiß.
 *
 * @param laeuft Welche Handlung gerade läuft (ihr Schlüssel) — alle Knöpfe der
 *   Seite warten dann, statt eine zweite Anfrage loszuschicken.
 * @param emailBestaetigungOffen Hält den Pflichtdialog nach dem Eintragen für den
 *   Code offen — mit dem Eintragen fiele sonst die Bedingung weg, unter der er steht.
 */
data class Kontostand(
    val premium: Bereich<Premiumstand?> = Bereich(),
    val postfach: Bereich<Postfach?> = Bereich(),
    val werbung: Bereich<Werbung?> = Bereich(),
    val patientensimulation: Boolean = false,
    val sitzungen: Bereich<List<Sitzungsuebersicht>> = Bereich(),
    val einwilligungen: Bereich<List<Einwilligung>> = Bereich(),
    val discord: Bereich<Discordstatus?> = Bereich(),
    val schenkfreunde: Bereich<List<Schenkfreund>> = Bereich(),
    val codevorschau: Codevorschau? = null,
    val weltVorhanden: Boolean = false,
    val laeuft: String? = null,
    val rueckmeldungen: Map<String, Rueckmeldung> = emptyMap(),
    val geschenk: Geschenkinhalt? = null,
    val emailBestaetigungOffen: Boolean = false,
) {
    fun meldung(schluessel: String): Rueckmeldung? = rueckmeldungen[schluessel]

    /**
     * Ob die Pflicht-Adresse fehlt. Erst nach dem Laden eine Auskunft — vorher ist
     * „keine Adresse" nur „noch nicht gefragt", und der Dialog blitzte bei jedem
     * Start auf.
     */
    val emailFehlt: Boolean get() = postfach.geladen && postfach.fehler == null && postfach.inhalt?.email == null
}
