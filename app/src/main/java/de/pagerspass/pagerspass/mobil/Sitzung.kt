package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Anmeldeergebnis
import de.pagerspass.pagerspass.netz.Konten
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kontowege
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.netz.Server
import de.pagerspass.pagerspass.netz.Spielwege
import de.pagerspass.pagerspass.netz.Wartungsstand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Die Sitzung — das Gegenstück zum Konto-Teil von `web/src/stores/spiel.ts`.
 *
 * <b>Sie ist die einzige Stelle, die weiß, ob jemand angemeldet ist.</b> Der
 * Rahmen fragt sie, die Anmeldeseite spricht mit ihr, und der Startbildschirm
 * bekommt sein Konto von ihr. Im Web ist das ein Pinia-Speicher; hier ein
 * ViewModel, weil es dieselbe Aufgabe hat: den Zustand über den Neuaufbau der
 * Oberfläche hinweg zu halten — auch über das Drehen des Geräts.
 *
 * <b>Was hier ausdrücklich nicht steht:</b> die Runde. Lobby, Leitstelle und
 * Fahrzeug hängen an einer Live-Verbindung (SignalR) und bekommen ihre eigene
 * Sitzung, wenn sie an der Reihe sind. Beides in einem Objekt wäre ein Objekt,
 * das bei jedem Funkspruch die Anmeldung neu zeichnet.
 */
class Sitzung(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val netz = Netz(ablage)
    val konten = Konten(netz, ablage)
    private val wege = Spielwege(netz)

    private val _stand = MutableStateFlow(Sitzungsstand())
    val stand: StateFlow<Sitzungsstand> = _stand.asStateFlow()

    private val _daten = MutableStateFlow(Seitenstand())
    val daten: StateFlow<Seitenstand> = _daten.asStateFlow()

    // ------------------------------------------- Bereich Wachengemeinschaft
    /**
     * Der Speicher der Wachenseiten (`mobil/Wachen.kt`). Er schreibt in denselben
     * `Seitenstand` — die Tableiste liest ihre Marke daraus.
     */
    val wachen = Wachenspeicher(
        wege = de.pagerspass.pagerspass.netz.Wachenwege(netz),
        umfang = viewModelScope,
        kennung = { _stand.value.konto?.kennung },
        daten = _daten,
        kontoAuffrischen = {
            viewModelScope.launch {
                val kennung = _stand.value.konto?.kennung ?: return@launch
                runCatching { konten.laden(kennung) }
                    .onSuccess { konto -> _stand.update { it.copy(konto = konto) } }
            }
        },
    )

    // ------------------------------------------------ Dienstbuch und Shop
    /**
     * Dienstbuch (sechs Seiten, Nachbesprechung aus dem Archiv) und Shop (fünf
     * Bereiche) — ein eigener Speicher, siehe `Dienstbuchstelle`. Er meldet
     * Kontostand, Shop und Garage hierher zurück, damit Leiste, Ausweis und Laden
     * dieselbe Zahl zeigen.
     */
    val dienstbuch = Dienstbuchstelle(
        netz = netz,
        bereich = viewModelScope,
        konto = { _stand.value.konto },
        kontoGesetzt = { frisch -> _stand.update { it.copy(konto = frisch) } },
        creditsGesetzt = { c -> _stand.update { it.copy(konto = it.konto?.copy(credits = c)) } },
        shopGesetzt = { shop -> _daten.update { d -> d.copy(shop = Bereich(shop, geladen = true)) } },
        garageNeu = { garageLaden(neu = true) },
    )
    // ------------------------------------------------ Ende Dienstbuch und Shop

    init {
        // Die beiden Ereignisse, die nicht Antwort auf eine Anfrage sind. Sie
        // gehören dem Rahmen, nicht der Stelle, die zufällig gerade lud.
        netz.beiAbmeldung = { _stand.update { it.copy(konto = null) } }
        netz.beiWartung = { wartung -> _stand.update { it.copy(wartung = wartung) } }

        kontoSicherstellen()
    }

    /**
     * Nachsehen, ob dieses Gerät schon ein Konto hat.
     *
     * <b>Sie läuft beim Start und darf scheitern.</b> Wer kein Merkmal hat, ist
     * nicht angemeldet — das ist der Normalfall beim ersten Start und keine
     * Meldung wert. Wer eines hat, dessen Sitzung aber abgelaufen ist, bekommt
     * einen 401, und den räumt die Netzschicht selbst weg.
     *
     * <b>Was sie nicht tut: melden, dass der Server nicht erreichbar ist.</b> Ein
     * Fehler auf dem Weg in die Anmeldemaske wäre eine Meldung über einem
     * Bildschirm, auf dem man ohnehin nichts anderes tun kann als sich anzumelden
     * — und dabei erführe man ihn ein zweites Mal.
     */
    fun kontoSicherstellen() = viewModelScope.launch {
        _stand.update { it.copy(server = ablage.server(), laedt = true) }

        val kennung = ablage.kennung()
        val merkmal = ablage.merkmal()

        if (kennung == null || merkmal == null) {
            _stand.update { it.copy(laedt = false, geprueft = true) }
            return@launch
        }

        runCatching { konten.laden(kennung) }
            .onSuccess { konto -> _stand.update { it.copy(konto = konto, laedt = false, geprueft = true) } }
            .onFailure { _stand.update { it.copy(laedt = false, geprueft = true) } }

        versionHolen()
    }

    /** Anmelden — mit dem möglichen Zwischenschritt Anmeldeschutz. */
    fun anmelden(benutzername: String, passwort: String, beiZweiFaktor: (String, String) -> Unit) =
        arbeiten {
            when (val ergebnis = konten.anmelden(benutzername.trim(), passwort)) {
                is Anmeldeergebnis.Angemeldet -> angekommen(ergebnis.konto)
                is Anmeldeergebnis.ZweiFaktor -> beiZweiFaktor(ergebnis.anfrage, ergebnis.ziel)
            }
        }

    fun zweiFaktorBestaetigen(anfrage: String, code: String) =
        arbeiten { angekommen(konten.zweiFaktorBestaetigen(anfrage, code.trim())) }

    /**
     * Konto anlegen.
     *
     * Der Wiederherstellungscode aus der Antwort wird festgehalten, weil er in
     * genau dieser einen Antwort steht und in keiner weiteren. Die Anmeldeseite
     * zeigt ihn danach; wer ihn hier fallen lässt, lässt ihn endgültig fallen.
     */
    fun kontoAnlegen(benutzername: String, anzeigename: String, passwort: String, email: String) = arbeiten {
        val konto = konten.anlegen(benutzername.trim(), anzeigename.trim(), passwort, email.trim())

        // Auf der Beta ist das Konto jetzt da, aber noch zu: Das Team schaltet es
        // frei, beantragt wird das im Discord. Nichts übernehmen — die Netzschicht hat
        // kein Merkmal gemerkt —, sondern den Hinweis zeigen und auf der Anmeldeseite
        // bleiben. Angemeldet wird nach der Freischaltung über den gewohnten Weg.
        if (konto.freischaltungOffen) {
            _stand.update {
                it.copy(
                    hinweis = "Dein Konto „${konto.benutzername}“ ist angelegt. Auf der Beta " +
                        "wird es erst vom Team freigeschaltet: Stell im Discord-Server unter " +
                        "#beta-antrag einen Antrag mit deinem Benutzernamen „${konto.benutzername}“ " +
                        "— sobald das Team ihn freigegeben hat, kannst du dich hier anmelden.",
                )
            }
            return@arbeiten
        }

        _stand.update { it.copy(wiederherstellungscode = konto.wiederherstellungscode) }
        angekommen(konto)
    }

    fun abmelden() = viewModelScope.launch {
        konten.abmelden()
        _stand.update { Sitzungsstand(server = ablage.server(), geprueft = true) }
        // Alles vergessen, was zum alten Konto gehörte. Ohne diese Zeile stünde
        // beim nächsten Anmelden die Freundesliste des Vorgängers auf dem Schirm,
        // bis die neue geladen ist.
        _daten.value = Seitenstand()
    }

    fun rechtsstandZustimmen() = arbeiten {
        val kennung = _stand.value.konto?.kennung ?: return@arbeiten
        angekommen(konten.rechtsstandZustimmen(kennung))
    }

    fun umbenennen(anzeigename: String) = arbeiten {
        val kennung = _stand.value.konto?.kennung ?: return@arbeiten
        angekommen(konten.umbenennen(kennung, anzeigename.trim()))
    }

    fun kontoLoeschen(passwort: String) = arbeiten {
        val kennung = _stand.value.konto?.kennung ?: return@arbeiten
        konten.loeschen(kennung, passwort)
        _stand.value = Sitzungsstand(server = ablage.server(), geprueft = true)
        _daten.value = Seitenstand()
    }

    // ------------------------------------------------------------ Die Seiten
    //
    // Jede Seite ruft beim Öffnen ihre Ladefunktion. Sie holt nur, was noch
    // fehlt: `nurWennNoetig` bricht ab, wenn schon etwas dasteht — sonst lädt
    // jeder Wechsel zwischen zwei Reitern der Tableiste alles neu, und die
    // Seite springt bei jedem Zurückkommen unter dem Finger weg.

    fun katalogSicherstellen() = laden(
        holen = { _daten.value.katalog },
        setzen = { _daten.update { d -> d.copy(katalog = it) } },
        tun = { wege.katalog() },
        nurWennNoetig = true,
    )

    fun buchLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.buch },
        setzen = { _daten.update { d -> d.copy(buch = it) } },
        tun = {
            val kennung = kennung()
            Buchdaten(
                schichten = runCatching { wege.chronik(kennung) }.getOrDefault(emptyList()),
                abzeichen = runCatching { wege.abzeichen(kennung) }.getOrDefault(emptyList()),
            )
        },
        nurWennNoetig = !neu,
    )

    fun freundeLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.freunde },
        setzen = { _daten.update { d -> d.copy(freunde = it) } },
        tun = { wege.freunde(kennung()) },
        nurWennNoetig = !neu,
    )

    fun shopLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.shop },
        setzen = { _daten.update { d -> d.copy(shop = it) } },
        tun = { wege.shop(kennung()) },
        nurWennNoetig = !neu,
    )

    fun wacheLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.wache },
        setzen = { _daten.update { d -> d.copy(wache = it) } },
        tun = {
            val eigene = wege.gemeinschaft(kennung())
            // Die Suchliste nur holen, wenn es nichts Eigenes gibt — wer in einer
            // Wache ist, sieht sie nie, und dreißig fremde Gemeinschaften sind
            // eine Anfrage, die niemand liest.
            Wachendaten(
                eigene = eigene,
                offene = if (eigene == null) wege.offeneGemeinschaften() else emptyList(),
            )
        },
        nurWennNoetig = !neu,
    )

    fun garageLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.garage },
        setzen = { _daten.update { d -> d.copy(garage = it) } },
        tun = {
            val stand = wege.garage(kennung())
            val katalog = _daten.value.katalog.inhalt ?: runCatching { wege.katalog() }.getOrNull()
            val baupläne = katalog?.fahrzeuge.orEmpty()
            val nachId = baupläne.associateBy { it.id }

            fun zeile(id: String, preis: Int? = null) = nachId[id].let { plan ->
                Fahrzeugzeile(
                    id = id,
                    typ = plan?.typ ?: id,
                    beschreibung = plan?.beschreibung.orEmpty(),
                    besatzung = plan?.besatzung.orEmpty(),
                    organisation = plan?.organisation.orEmpty(),
                    preis = preis,
                    tagesangebot = id == stand.tagesangebot,
                    regulaer = if (id == stand.tagesangebot) stand.tagesangebotRegulaer else null,
                )
            }

            val eigene = stand.fahrzeuge.toSet()

            Garagendaten(
                stand = stand,
                fahrzeuge = stand.fahrzeuge.map { zeile(it) }.sortedBy { it.typ },
                angebot = baupläne
                    .filterNot { it.id in eigene }
                    .mapNotNull { plan -> stand.preise[plan.id]?.let { zeile(plan.id, it) } }
                    // Das Tagesangebot zuerst, dann nach Preis: Wer ein Fahrzeug
                    // sucht, sucht zuerst eines, das er sich leisten kann.
                    .sortedWith(compareByDescending<Fahrzeugzeile> { it.tagesangebot }
                        .thenBy { it.preis }),
            )
        },
        nurWennNoetig = !neu,
    )

    fun profilLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.profil },
        setzen = { _daten.update { d -> d.copy(profil = it) } },
        tun = {
            val konto = _stand.value.konto ?: error("Kein Konto angemeldet.")
            wege.profil(konto.kennung, konto.benutzername)
        },
        nurWennNoetig = !neu,
    )

    fun profilbildLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.profilbild },
        setzen = { _daten.update { d -> d.copy(profilbild = it) } },
        tun = { wege.profilbildstand(kennung()) },
        nurWennNoetig = !neu,
    )

    /**
     * Ein Bild einreichen.
     *
     * <b>Die Prüfung vor dem Hochladen ist keine Schikane.</b> Der Server nennt
     * in `Profilbildstand` seine Grenzen (Größe, kürzeste Kante, erlaubte
     * Typen); sie hier zu prüfen erspart einen Upload über Mobilfunk, der am
     * Ende abgewiesen wird. Der Server prüft trotzdem noch einmal — er muss.
     */
    fun profilbildEinreichen(dateiname: String, inhaltstyp: String, daten: ByteArray) = arbeiten {
        val grenzen = _daten.value.profilbild.inhalt
        val hoechstens = grenzen?.maxBytes ?: 0

        if (hoechstens > 0 && daten.size > hoechstens) {
            throw IllegalArgumentException(
                "Das Bild ist ${daten.size / 1024} KB groß; erlaubt sind ${hoechstens / 1024} KB.",
            )
        }

        val stand = wege.profilbildEinreichen(kennung(), dateiname, inhaltstyp, daten)
        _daten.update { d -> d.copy(profilbild = Bereich(stand, geladen = true)) }
        profilLaden(neu = true)
    }

    fun profilbildEntfernen() = arbeiten {
        val stand = wege.profilbildEntfernen(kennung())
        _daten.update { d -> d.copy(profilbild = Bereich(stand, geladen = true)) }
        profilLaden(neu = true)
    }

    fun mitteilungenLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.mitteilungen },
        setzen = { _daten.update { d -> d.copy(mitteilungen = it) } },
        tun = { wege.mitteilungen() },
        nurWennNoetig = !neu,
    ).also {
        viewModelScope.launch {
            val gelesen = ablage.mitteilungenGelesen()
            _daten.update { d -> d.copy(mitteilungenGelesen = gelesen) }
        }
    }

    /** Eine Betreibermitteilung wegklicken — nur auf diesem Gerät, wie im Web. */
    fun mitteilungGelesen(id: String) = viewModelScope.launch {
        ablage.mitteilungGelesenMerken(id)
        _daten.update { d -> d.copy(mitteilungenGelesen = d.mitteilungenGelesen + id) }
    }

    // ------------------------------------------------------------- Die Wache

    fun clanrundeStarten(id: String) = viewModelScope.launch {
        runCatching { wege.clanrundeStarten(kennung(), id) }
            .onSuccess { antwort ->
                val code = (antwort["code"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                if (code != null) _stand.update { it.copy(raumcode = code) }
            }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message) } }
    }

    fun einladungenLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.einladungen },
        setzen = { _daten.update { d -> d.copy(einladungen = it) } },
        tun = { wege.einladungen(kennung()) },
        nurWennNoetig = !neu,
    )

    /**
     * Verwarnungen und Verwaltungsnachrichten — beim Anmelden geholt, als
     * Blende gezeigt, bis sie bestätigt sind. Der Server liefert nur die
     * unbestätigten; Fehler hier sind still, denn eine kaputte Abfrage darf
     * den Start nicht blockieren.
     */
    fun hinweiseLaden() = viewModelScope.launch {
        runCatching { wege.verwarnungen(kennung()) }
            .onSuccess { liste -> _daten.update { d -> d.copy(verwarnungen = liste) } }
        runCatching { wege.adminNachrichten(kennung()) }
            .onSuccess { liste -> _daten.update { d -> d.copy(adminNachrichten = liste) } }
    }

    fun verwarnungBestaetigen(nr: Int) = viewModelScope.launch {
        runCatching { wege.verwarnungBestaetigen(kennung(), nr) }
        _daten.update { d -> d.copy(verwarnungen = d.verwarnungen.filter { it.nr != nr }) }
    }

    fun adminNachrichtBestaetigen(nr: Int) = viewModelScope.launch {
        runCatching { wege.adminNachrichtBestaetigen(kennung(), nr) }
        _daten.update { d -> d.copy(adminNachrichten = d.adminNachrichten.filter { it.nr != nr }) }
    }

    /**
     * Eine Runde eröffnen — und den Raumcode melden.
     *
     * <b>Die App kann die Runde noch nicht fahren.</b> Lobby und Dienst hängen an
     * der Live-Verbindung, die noch aussteht. Der Code ist trotzdem echt: Er lässt
     * sich weitergeben und im Web besetzen. Deshalb steht hier ein Ergebnis und
     * kein `TODO` — ein Knopf, der nichts tut, ist schlechter als einer, der
     * ehrlich sagt, wie weit er kommt.
     */
    fun raumEroeffnen(
        landkreisId: String?,
        leitstelleId: String? = null,
        ganzerBereich: Boolean = false,
    ) = arbeiten {
        // Runde, Teil 1 (A8): Leitstelle und „ganzer Bereich" wie im Web mitschicken.
        val raum = wege.raumAnlegen(landkreisId, ganzerBereich = ganzerBereich, leitstelleId = leitstelleId)
        _stand.update { it.copy(raumcode = raum.code) }
    }

    fun ausbildungEroeffnen() = arbeiten {
        val raum = wege.ausbildungAnlegen()
        _stand.update { it.copy(raumcode = raum.code) }
    }

    /** Die Schicht des Tages anlegen — festes Skript, Bots als Besatzung. */
    fun tagesschichtEroeffnen() = arbeiten {
        val code = wege.tagesschichtAnlegen()
        _stand.update { it.copy(raumcode = code) }
    }

    fun oeffentlicheRundenLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.oeffentlicheRunden },
        setzen = { _daten.update { d -> d.copy(oeffentlicheRunden = it) } },
        tun = { wege.oeffentlicheRunden() },
        nurWennNoetig = !neu,
    )

    fun tagesschichtLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.tagesschicht },
        setzen = { _daten.update { d -> d.copy(tagesschicht = it) } },
        tun = { wege.tagesschicht(_stand.value.konto?.kennung) },
        nurWennNoetig = !neu,
    )

    fun raumcodeWegnehmen() = _stand.update { it.copy(raumcode = null) }

    // ------------------------------------- Bereich Konto: Konto, Recht, E-Mail-Pflicht
    //
    // Die Wege des Kontobereichs stehen in `netz/Kontowege.kt`; die Seiten rufen sie
    // selbst (siehe `mobil/Kontobereich.kt`). Hier steht nur, was am Konto selbst
    // hängt und deshalb durch die Sitzung muss.

    val kontowege = Kontowege(netz)

    /** Die Ablage dieses Geräts — für „Deine Bedienung" und „Dieses Gerät". */
    val geraeteablage: Ablage get() = ablage

    private val _emailBestaetigungOffen = MutableStateFlow(false)

    /**
     * Ob die Pflichtblende nach dem Eintragen der Adresse für den Code stehen
     * bleibt. Mit dem Eintragen fällt `emailFehlt` — der Code ist aber gerade erst
     * unterwegs, und das Feld dafür soll nicht verschwinden.
     */
    val emailBestaetigungOffen: StateFlow<Boolean> = _emailBestaetigungOffen.asStateFlow()

    /** Ein neuer Stand des Kontos, wie ihn ein Weg des Kontobereichs zurückgab. */
    fun kontoUebernehmen(konto: Konto) = _stand.update { it.copy(konto = konto) }

    /** Ein Feld am Konto nachziehen — Premium, Credits, Analysezustimmung. */
    fun kontoAendern(aenderung: (Konto) -> Konto) =
        _stand.update { s -> s.copy(konto = s.konto?.let(aenderung)) }

    /** Das Konto frisch holen — nach Benutzername, Premium, E-Mail. Scheitert still. */
    fun kontoNachladen() = viewModelScope.launch {
        val kennung = _stand.value.konto?.kennung ?: return@launch
        runCatching { konten.laden(kennung) }
            .onSuccess { neu -> _stand.update { it.copy(konto = neu) } }
    }

    /**
     * Die Antwort auf die Frage nach der Aufzeichnung. Ein „nein" löscht
     * serverseitig auch das bereits Aufgezeichnete.
     */
    suspend fun analyseBeantworten(zugestimmt: Boolean) {
        val kennung = kennung()
        val stand = kontowege.analyseSpeichern(kennung, zugestimmt)
        kontoAendern { it.copy(analyseZustimmung = stand.zugestimmt ?: zugestimmt) }
    }

    /** Trägt die Pflicht-Adresse ein; der Server schickt dabei den Bestätigungscode. */
    suspend fun emailHinterlegen(email: String, passwort: String) {
        val kennung = kennung()
        kontowege.emailSetzen(kennung, email, passwort)
        _emailBestaetigungOffen.value = true
        runCatching { konten.laden(kennung) }
            .onSuccess { neu -> _stand.update { it.copy(konto = neu) } }
            .onFailure { kontoAendern { it.copy(emailFehlt = false) } }
    }

    suspend fun emailCodeBestaetigen(code: String) {
        kontowege.emailBestaetigen(kennung(), code)
        _emailBestaetigungOffen.value = false
    }

    suspend fun emailCodeNeu() = kontowege.emailCodeNeu(kennung())

    /** „Später" — die Pflicht ist erfüllt, bestätigt wird in den Kontoeinstellungen. */
    fun emailBestaetigungSpaeter() {
        _emailBestaetigungOffen.value = false
    }

    /**
     * Meldet ab und leert alles, was dieses Gerät für PagerSpass aufbewahrt —
     * „Dieses Gerät → Löschen" in der Privatsphäre. Abgemeldet wird zuerst, solange
     * das Merkmal noch da ist: So endet auch die Sitzung am Server.
     */
    fun geraetLeeren() = viewModelScope.launch {
        konten.abmelden()
        runCatching { ablage.allesVergessen() }
        _emailBestaetigungOffen.value = false
        _stand.update { Sitzungsstand(server = ablage.server(), geprueft = true) }
        _daten.value = Seitenstand()
    }

    private fun kennung(): String =
        _stand.value.konto?.kennung ?: throw IllegalStateException("Kein Konto angemeldet.")

    /**
     * Der gemeinsame Mantel um das Laden eines Bereichs.
     *
     * Dasselbe wie `arbeiten` eine Etage tiefer: Ladezustand setzen, Fehler in
     * einen Satz verwandeln, `geladen` merken. Wer das je Bereich schreibt,
     * vergisst beim vierten `geladen = true` — und dann steht der Leerhinweis
     * für immer da.
     */
    private fun <T> laden(
        holen: () -> Bereich<T>,
        setzen: (Bereich<T>) -> Unit,
        tun: suspend () -> T,
        nurWennNoetig: Boolean,
    ) = viewModelScope.launch {
        val vorher = holen()
        if (vorher.laedt) return@launch
        if (nurWennNoetig && vorher.geladen) return@launch
        if (_stand.value.konto == null && vorher.geladen) return@launch

        setzen(vorher.copy(laedt = true, fehler = null))

        runCatching { tun() }
            .onSuccess { setzen(Bereich(inhalt = it, laedt = false, geladen = true)) }
            .onFailure { f ->
                setzen(
                    holen().copy(
                        laedt = false,
                        geladen = true,
                        fehler = f.message ?: "Konnte nicht geladen werden.",
                    )
                )
            }
    }

    /** Den Server wechseln — und alles vergessen, was zum alten gehörte. */
    fun serverWechseln(adresse: String) = viewModelScope.launch {
        ablage.serverSetzen(adresse)
        ablage.anmeldungMerken(null, null)
        _stand.value = Sitzungsstand(server = adresse.trimEnd('/'), geprueft = true)
        versionHolen()
    }

    fun fehlerWegnehmen() = _stand.update { it.copy(fehler = null) }

    fun wiederherstellungscodeWegnehmen() = _stand.update { it.copy(wiederherstellungscode = null) }

    private fun versionHolen() = viewModelScope.launch {
        runCatching { konten.version() }
            .onSuccess { v ->
                _stand.update {
                    it.copy(
                        version = v.version,
                        kanal = v.kanal,
                        kontofreischaltung = v.kontofreischaltung,
                    )
                }
            }
    }

    /**
     * Ein Konto ist angekommen.
     *
     * Hier und nur hier wird entschieden, ob die neue Fassung der Rechtstexte
     * vorzulegen ist. Nicht in der Ansicht: Die Frage stellt sich nach jeder
     * Anmeldung, nach jeder Kontoanlage und nach jedem Nachladen gleich.
     */
    private fun angekommen(konto: Konto) {
        _stand.update {
            it.copy(
                konto = konto,
                fehler = null,
                hinweis = null,
                rechtsstandOffen = Rechtsstand.vorzulegen(konto.rechtsstandVersion),
            )
        }
        versionHolen()
    }

    /**
     * Der gemeinsame Mantel um alles, was eine Weile dauert und schiefgehen kann.
     *
     * <b>Er nimmt dem Aufrufer drei Dinge ab</b>, die im Web an jeder Stelle
     * einzeln standen: den Laufzustand setzen und zurücknehmen, den Fehler in
     * eine Meldung verwandeln, und den alten Fehler wegräumen, bevor der neue
     * Versuch beginnt. Wer das je Aufruf schreibt, vergisst beim vierten Mal das
     * Zurücknehmen — und dann dreht sich der Knopf für immer.
     */
    private fun arbeiten(tun: suspend () -> Unit) = viewModelScope.launch {
        _stand.update { it.copy(laeuft = true, fehler = null, hinweis = null) }
        runCatching { tun() }
            .onFailure { fehler ->
                _stand.update { it.copy(fehler = fehler.message ?: "Etwas ist schiefgegangen.") }
            }
        _stand.update { it.copy(laeuft = false) }
    }
}

/**
 * Was die Sitzung über den Stand der Dinge weiß.
 *
 * <b>`geprueft` ist nicht dasselbe wie `konto == null`.</b> Beim Start weiß die
 * App eine knappe Sekunde lang noch gar nichts — und in dieser Sekunde darf sie
 * weder die Anmeldemaske noch den Startbildschirm zeigen, sondern gar nichts.
 * Ohne diese Unterscheidung blitzt bei jedem Start die Anmeldung auf, bevor das
 * gespeicherte Konto geladen ist.
 */
data class Sitzungsstand(
    val konto: Konto? = null,
    /** Ob die erste Prüfung („liegt hier ein Konto?") durch ist. */
    val geprueft: Boolean = false,
    /** Ob gerade das gespeicherte Konto geladen wird. */
    val laedt: Boolean = false,
    /** Ob gerade eine Anmeldung, Anlage oder Änderung läuft. */
    val laeuft: Boolean = false,
    val fehler: String? = null,
    /**
     * Eine freundliche Auskunft auf der Anmeldeseite — das Gegenstück zu `fehler`:
     * „Dein Konto ist angelegt und wartet auf die Freischaltung." Steht nur, bis
     * das nächste Konto ankommt oder die nächste Aktion beginnt.
     */
    val hinweis: String? = null,
    /** Steht nach der Kontoanlage genau einmal zur Verfügung. */
    val wiederherstellungscode: String? = null,
    /** Ob dem Konto die neue Fassung der Rechtstexte vorzulegen ist. */
    val rechtsstandOffen: Boolean = false,
    val wartung: Wartungsstand? = null,
    /** Der Code einer gerade eröffneten Runde — siehe `raumEroeffnen`. */
    val raumcode: String? = null,
    val server: String = Server.VORGABE,
    val version: String? = null,
    val kanal: String? = null,
    /** Ob das Postfach freigeschaltet ist — es hängt an einem Betriebsschalter. */
    val postfachFrei: Boolean = false,
    /** Ob ein neues Konto erst vom Team freigeschaltet werden muss — die Regel der Beta. */
    val kontofreischaltung: Boolean = false,
) {
    val angemeldet: Boolean get() = konto != null

    /** Ob dies ein Vorabstand ist — das „Beta" im Fuß. */
    val beta: Boolean get() = kanal.equals("beta", ignoreCase = true) || "beta" in server
}
