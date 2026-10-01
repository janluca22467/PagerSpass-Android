package de.pagerspass.pagerspass.mobil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Anmeldeergebnis
import de.pagerspass.pagerspass.netz.Konten
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Privatsphaere
import de.pagerspass.pagerspass.netz.Profilaenderung
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

    /**
     * Konto-Zentrale, Premium-Stand, Postfach und Shop-Nebenwege — siehe
     * `Kontodienst`. Er hängt hier, damit er die Anmeldung teilt und mit ihr geht.
     */
    val kontodienst = Kontodienst(
        wege = de.pagerspass.pagerspass.netz.Kontowege(netz),
        bereich = viewModelScope,
        kennung = { _stand.value.konto?.kennung },
        kontoAendern = { aendern -> _stand.update { it.copy(konto = it.konto?.let(aendern)) } },
        kontoSetzen = { konto -> _stand.update { it.copy(konto = konto) } },
        shopSetzen = { shop ->
            _daten.update { d -> d.copy(shop = Bereich(shop, geladen = true)) }
            _stand.update { it.copy(konto = it.konto?.copy(credits = shop.credits)) }
        },
        garageNeu = { garageLaden(neu = true) },
        freundeNeu = { freundeLaden(neu = true) },
    )

    /**
     * Altersfrage, Einrichtungsbogen, Maßnahmenübersicht — siehe `Einrichtungsdienst`.
     * Neben dem Kontodienst und nicht in ihm: Hier lässt jeder Fehlschlag die Frage zu.
     */
    val einrichtungsdienst = Einrichtungsdienst(
        wege = de.pagerspass.pagerspass.netz.Einrichtungswege(netz),
        bereich = viewModelScope,
        kennung = { _stand.value.konto?.kennung },
        kontoNeu = {
            val k = _stand.value.konto?.kennung
            if (k != null) {
                val neu = de.pagerspass.pagerspass.netz.Kontowege(netz).konto(k)
                _stand.update { it.copy(konto = neu) }
            }
        },
    )

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
    fun kontoAnlegen(benutzername: String, anzeigename: String, passwort: String) = arbeiten {
        val konto = konten.anlegen(benutzername.trim(), anzeigename.trim(), passwort)

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
        kontodienst.vergessen()
        einrichtungsdienst.vergessen()
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
        kontodienst.vergessen()
        einrichtungsdienst.vergessen()
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

    fun bestenlisteLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.bestenliste },
        setzen = { _daten.update { d -> d.copy(bestenliste = it) } },
        tun = { wege.bestenliste(kennung = _stand.value.konto?.kennung) },
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

    /**
     * Den Schmuck ändern — Wappen, Farbe, Rahmen, Muster, Vorstellung, Name.
     *
     * <b>Die Antwort ist das neue Profil</b>, und sie wird sofort gesetzt: Wer
     * ein Muster wählt, sieht es im selben Augenblick am Banner darüber. Ein
     * Nachladen wäre eine zweite Anfrage für etwas, das schon da ist.
     *
     * Ändert sich dabei der Anzeigename, geht auch das Konto mit — es trägt ihn
     * ebenfalls, und ein Konto, das noch den alten Namen führt, ließe die
     * Tableiste und den Startbildschirm zwei verschiedene Personen zeigen.
     */
    fun profilAendern(aenderung: Profilaenderung) = arbeiten {
        val kennung = _stand.value.konto?.kennung ?: return@arbeiten
        val profil = wege.profilSpeichern(kennung, aenderung)
        _daten.update { d -> d.copy(profil = Bereich(profil, geladen = true)) }

        if (aenderung.anzeigename != null) {
            _stand.update { it.copy(konto = it.konto?.copy(anzeigename = profil.anzeigename)) }
        }
    }

    /**
     * Einen Artikel kaufen.
     *
     * <b>Der ganze Shop kommt zurück</b> — Credit-Stand, Besitzstand und die
     * Kaufbarkeit aller anderen Artikel haben sich mitgeändert. Und weil der
     * Besitzstand auch die Profilseite steuert, ist damit dort sofort offen, was
     * eben gekauft wurde.
     */
    fun artikelKaufen(artikelId: String) = arbeiten {
        val kennung = kennung()
        val shop = wege.artikelKaufen(kennung, artikelId)
        _daten.update { d -> d.copy(shop = Bereich(shop, geladen = true)) }
        // Der Credit-Stand steht auch am Konto — sonst zeigt der Kopf des Shops
        // 32 und der Dienstausweis weiter 182.
        _stand.update { it.copy(konto = it.konto?.copy(credits = shop.credits)) }
    }

    fun tagesbonusHolen() = arbeiten {
        val ergebnis = wege.tagesbonus(kennung())
        _daten.update { d ->
            d.copy(shop = Bereich(ergebnis.shop, geladen = true), bonusgewinn = ergebnis.gewinn)
        }
        _stand.update { it.copy(konto = it.konto?.copy(credits = ergebnis.shop.credits)) }
    }

    fun bonusgewinnWegnehmen() = _daten.update { it.copy(bonusgewinn = null) }

    /** Ein Fahrzeug aussuchen (Gutschein) oder kaufen (Credits). */
    fun fahrzeugHolen(vorlage: String, kaufen: Boolean) = arbeiten {
        val kennung = kennung()
        val garage = if (kaufen) {
            wege.fahrzeugKaufen(kennung, vorlage)
        } else {
            wege.fahrzeugWaehlen(kennung, vorlage)
        }
        _stand.update { it.copy(konto = it.konto?.copy(credits = garage.credits)) }
        garageLaden(neu = true)
    }

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

    fun privatsphaereLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.privatsphaere },
        setzen = { _daten.update { d -> d.copy(privatsphaere = it) } },
        tun = { wege.privatsphaere(kennung()) },
        nurWennNoetig = !neu,
    )

    /**
     * Eine Einstellung umlegen.
     *
     * <b>Sie steht sofort auf dem Schirm und wird dann geschickt.</b> Ein
     * Schalter, der erst nach der Antwort umspringt, fühlt sich kaputt an —
     * gerade auf einer Seite mit sechzehn davon. Scheitert der Aufruf, holt die
     * Antwort den alten Stand zurück, und die Meldung sagt, warum.
     */
    fun privatsphaereSetzen(neu: Privatsphaere) = viewModelScope.launch {
        val vorher = _daten.value.privatsphaere
        _daten.update { d -> d.copy(privatsphaere = vorher.copy(inhalt = neu)) }

        runCatching { wege.privatsphaereSpeichern(kennung(), neu) }
            .onSuccess { gespeichert ->
                _daten.update { d -> d.copy(privatsphaere = Bereich(gespeichert, geladen = true)) }
            }
            .onFailure { f ->
                _daten.update { d -> d.copy(privatsphaere = vorher) }
                _stand.update { it.copy(fehler = f.message) }
            }
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

    fun mitteilungsschalterLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.mitteilungsschalter },
        setzen = { _daten.update { d -> d.copy(mitteilungsschalter = it) } },
        tun = { wege.mitteilungseinstellungen(kennung()) },
        nurWennNoetig = !neu,
    )

    /** Wie bei der Privatsphäre: sofort umlegen, dann speichern, sonst zurück. */
    fun mitteilungsschalterSetzen(neu: de.pagerspass.pagerspass.netz.Mitteilungseinstellungen) =
        viewModelScope.launch {
            val vorher = _daten.value.mitteilungsschalter
            _daten.update { d -> d.copy(mitteilungsschalter = vorher.copy(inhalt = neu)) }

            runCatching { wege.mitteilungseinstellungenSpeichern(kennung(), neu) }
                .onSuccess { gespeichert ->
                    _daten.update { d ->
                        d.copy(mitteilungsschalter = Bereich(gespeichert, geladen = true))
                    }
                }
                .onFailure { f ->
                    _daten.update { d -> d.copy(mitteilungsschalter = vorher) }
                    _stand.update { it.copy(fehler = f.message) }
                }
        }

    // ------------------------------------------------------------------ Brett

    fun brettLaden(reiter: String? = null, neu: Boolean = false) {
        val kreis = reiter ?: _daten.value.brettReiter
        if (reiter != null) _daten.update { d -> d.copy(brettReiter = reiter) }
        laden(
            holen = { _daten.value.brett },
            setzen = { _daten.update { d -> d.copy(brett = it) } },
            tun = { wege.brett(kennung(), kreis) },
            nurWennNoetig = !neu && reiter == null,
        )
    }

    /** Die nächste Seite anhängen — geblättert über `weiter`, nicht Offset. */
    fun brettMehr() = viewModelScope.launch {
        val stand = _daten.value.brett.inhalt ?: return@launch
        val weiter = stand.weiter ?: return@launch
        runCatching { wege.brett(kennung(), _daten.value.brettReiter, vorNr = weiter) }
            .onSuccess { seite ->
                _daten.update { d ->
                    d.copy(
                        brett = d.brett.copy(
                            inhalt = seite.copy(eintraege = stand.eintraege + seite.eintraege),
                        ),
                    )
                }
            }
    }

    fun brettSchreiben(text: String, sichtbarkeit: String?) = viewModelScope.launch {
        runCatching { wege.brettSchreiben(kennung(), text, sichtbarkeit) }
            .onSuccess { brettLaden(neu = true) }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message) } }
    }

    /**
     * Die Quittung umlegen — <b>sofort auf dem Schirm, dann zum Server.</b>
     * Ein Herz, das erst nach der Antwort umspringt, fühlt sich kaputt an.
     */
    fun brettQuittieren(nr: Long) = viewModelScope.launch {
        fun de.pagerspass.pagerspass.netz.Bretteintrag.umgelegt() = copy(
            vonMirQuittiert = !vonMirQuittiert,
            quittungen = if (vonMirQuittiert) quittungen - 1 else quittungen + 1,
        )
        _daten.update { d ->
            d.copy(
                brett = d.brett.copy(
                    inhalt = d.brett.inhalt?.let { s ->
                        s.copy(eintraege = s.eintraege.map { if (it.nr == nr) it.umgelegt() else it })
                    },
                ),
                brettEintrag = d.brettEintrag.copy(
                    inhalt = d.brettEintrag.inhalt?.let { if (it.nr == nr) it.umgelegt() else it },
                ),
            )
        }
        runCatching { wege.brettQuittieren(kennung(), nr) }
    }

    fun brettEintragLaden(nr: Long) = viewModelScope.launch {
        _daten.update { d -> d.copy(brettEintrag = Bereich(laedt = true)) }
        runCatching {
            val eintrag = wege.bretteintrag(kennung(), nr)
            val kommentare = wege.brettKommentare(kennung(), nr)
            _daten.update { d ->
                d.copy(
                    brettEintrag = Bereich(eintrag, geladen = true),
                    brettKommentare = kommentare,
                )
            }
        }.onFailure { f ->
            _daten.update { d ->
                d.copy(brettEintrag = Bereich(fehler = f.message, geladen = true))
            }
        }
    }

    fun brettKommentieren(nr: Long, text: String) = viewModelScope.launch {
        runCatching { wege.brettKommentieren(kennung(), nr, text) }
            .onSuccess { neu ->
                _daten.update { d -> d.copy(brettKommentare = d.brettKommentare + neu) }
            }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message) } }
    }

    // ------------------------------------------------------------- Die Wache

    fun wacheDetailLaden(id: String) = viewModelScope.launch {
        runCatching { wege.gemeinschaftDetail(kennung(), id) }
            .onSuccess { detail ->
                _daten.update { d -> d.copy(wacheDetail = Bereich(detail, geladen = true)) }
            }
            .onFailure { f ->
                _daten.update { d ->
                    d.copy(wacheDetail = Bereich(fehler = f.message, geladen = true))
                }
            }
    }

    /**
     * Der gemeinsame Mantel für Wachen-Aktionen: tun, dann Detail und Liste
     * frisch holen. Der Server prüft die Rollen — die App zeigt seine Antwort.
     */
    private fun wachenAktion(id: String?, tun: suspend () -> Unit) = viewModelScope.launch {
        runCatching { tun() }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message) } }
        wacheLaden(neu = true)
        id?.let { wacheDetailLaden(it) }
    }

    fun antragEntscheiden(id: String, nr: Long, annehmen: Boolean) =
        wachenAktion(id) { wege.gemeinschaftsantragEntscheiden(kennung(), nr, annehmen) }

    /** Die eigene Bewerbung zurückziehen — dieselbe Leitung, `annehmen = false`. */
    fun bewerbungZurueckziehen(nr: Long) = viewModelScope.launch {
        runCatching { wege.gemeinschaftsantragEntscheiden(kennung(), nr, false) }
        wachenantraegeLaden(neu = true)
    }

    fun einladungAnnehmen(nr: Long) = wachenAktion(null) {
        wege.gemeinschaftsantragEntscheiden(kennung(), nr, true)
    }

    fun rolleSetzen(id: String, wen: String, rolle: String) =
        wachenAktion(id) { wege.gemeinschaftRolle(kennung(), id, wen, rolle) }

    fun leitungUebergeben(id: String, an: String) =
        wachenAktion(id) { wege.gemeinschaftLeitung(kennung(), id, an) }

    fun mitgliedEntfernen(id: String, wen: String) =
        wachenAktion(id) { wege.gemeinschaftEntfernen(kennung(), id, wen) }

    fun austreten(id: String) = wachenAktion(null) {
        wege.gemeinschaftEntfernen(kennung(), id, kennung())
    }

    fun wacheEinladen(id: String, wen: String) =
        wachenAktion(id) { wege.gemeinschaftEinladen(kennung(), id, wen) }

    fun pinnwandSetzen(id: String, text: String?) =
        wachenAktion(id) { wege.gemeinschaftPinnwand(kennung(), id, text) }

    fun terminAnlegen(id: String, titel: String, wann: String) =
        wachenAktion(id) { wege.terminAnlegen(kennung(), id, titel, wann) }

    fun terminAntworten(id: String, nr: Long, antwort: String) =
        wachenAktion(id) { wege.terminAntworten(kennung(), nr, antwort) }

    fun terminAbsagen(id: String, nr: Long) =
        wachenAktion(id) { wege.terminAbsagen(kennung(), nr) }

    fun zeileMelden(id: String, nr: Long, grund: String?) =
        wachenAktion(id) { wege.chatzeileMelden(kennung(), nr, grund) }

    fun zeileEntfernen(id: String, nr: Long) =
        wachenAktion(id) { wege.chatzeileEntfernen(kennung(), nr) }

    fun meldungErledigt(id: String, nr: Long) =
        wachenAktion(id) { wege.meldungErledigt(kennung(), nr) }

    fun clanrundeStarten(id: String) = viewModelScope.launch {
        runCatching { wege.clanrundeStarten(kennung(), id) }
            .onSuccess { antwort ->
                val code = (antwort["code"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                if (code != null) _stand.update { it.copy(raumcode = code) }
            }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message) } }
    }

    fun gruenden(name: String, beschreibung: String?) = wachenAktion(null) {
        wege.gemeinschaftGruenden(kennung(), name, beschreibung)
    }

    fun beitrittMitCode(code: String) = wachenAktion(null) {
        wege.gemeinschaftBeitrittMitCode(kennung(), code)
    }

    fun bewerben(id: String, nachricht: String?) = viewModelScope.launch {
        runCatching { wege.gemeinschaftBewerben(kennung(), id, nachricht) }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message) } }
        wacheLaden(neu = true)
        wachenantraegeLaden(neu = true)
    }

    fun wachenantraegeLaden(neu: Boolean = false) = laden(
        holen = { _daten.value.wachenantraege },
        setzen = { _daten.update { d -> d.copy(wachenantraege = it) } },
        tun = { wege.gemeinschaftsantraege(kennung()) },
        nurWennNoetig = !neu,
    )

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

    /** Eine Freundschaftsanfrage annehmen oder ablehnen — danach neu laden. */
    fun freundAntworten(wen: String, annehmen: Boolean) = viewModelScope.launch {
        runCatching { wege.freundAntworten(kennung(), wen, annehmen) }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message) } }
        freundeLaden(neu = true)
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
    fun raumEroeffnen(landkreisId: String?, leitstelleId: String? = null, ganzerBereich: Boolean = true) = arbeiten {
        val raum = wege.raumAnlegen(landkreisId, ganzerBereich, leitstelleId)
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
        kontodienst.vergessen()
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
     * Das Konto still neu holen — nach einer Einzahlung in die Wachenkasse oder
     * einem Kauf für die Wache stimmen die Credits am Konto sonst nicht mehr.
     *
     * <b>Still heißt: ohne `laedt`.</b> Das setzte das Band „Verbindung wird
     * hergestellt" — für einen Kontostand, der sich um zwei Zahlen ändert.
     */
    fun kontoAuffrischen() = viewModelScope.launch {
        val kennung = _stand.value.konto?.kennung ?: return@launch
        runCatching { konten.laden(kennung) }
            .onSuccess { konto -> _stand.update { it.copy(konto = konto) } }
    }

    /**
     * Die Detailansicht der Wache übernehmen, die eine Antwort gleich
     * mitgebracht hat (Shop, Aussehen, Tag, Einstellungen) — statt sie ein
     * zweites Mal zu holen.
     */
    fun wacheDetailSetzen(detail: de.pagerspass.pagerspass.netz.GemeinschaftDetail) =
        _daten.update { d -> d.copy(wacheDetail = Bereich(detail, geladen = true)) }

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
