package de.pagerspass.pagerspass.netz

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Was zwischen zwei Starts liegen bleibt: das Sitzungsmerkmal, die Kennung und
 * der Server, mit dem gesprochen wird.
 *
 * <b>Das Merkmal weist die Anfrage aus; die Kennung sagt nur, um wessen Daten es
 * geht.</b> Das ist die Trennung, die im Web hinter `SPEICHER_MERKMAL` steht und
 * sie hat einen Grund: Vorher war die Kennung beides — und weil sie durch
 * Einladungen wandert, war sie als Ausweis untauglich.
 *
 * <b>Warum DataStore und nicht SharedPreferences.</b> Nicht wegen der Sperre auf
 * dem Hauptfaden, sondern weil der Zugriff hier ohnehin aus Nebenläufen kommt:
 * Die Netzschicht liest das Merkmal vor jeder Anfrage, und ein blockierender
 * Zugriff aus einem Coroutine-Kontext ist genau die Art Fehler, die erst unter
 * Last auffällt.
 *
 * <b>Was hier ausdrücklich nicht steht:</b> das Passwort. Es geht einmal über die
 * Leitung und wird nie abgelegt — wer sich neu anmelden muss, tippt es neu.
 */
private val Context.ablage by preferencesDataStore(name = "pagerspass")

class Ablage(private val zusammenhang: Context) {

    private val merkmalSchluessel = stringPreferencesKey("merkmal")
    private val kennungSchluessel = stringPreferencesKey("kennung")
    private val serverSchluessel = stringPreferencesKey("server")
    private val rundenSchluessel = stringPreferencesKey("offeneRunde")
    private val begleiterSchluessel = stringPreferencesKey("begleiterToken")
    private val karteFreigabeSchluessel = stringPreferencesKey("karteFreigabe")
    private val karteStilSchluessel = stringPreferencesKey("karteStil")
    private val mitteilungenGelesenSchluessel = stringPreferencesKey("mitteilungenGelesen")
    private val gemeldeteHinweiseSchluessel = stringPreferencesKey("gemeldeteHinweise")

    suspend fun merkmal(): String? = lesen(merkmalSchluessel)

    suspend fun kennung(): String? = lesen(kennungSchluessel)

    /**
     * Der Server, mit dem gesprochen wird.
     *
     * Ohne Eintrag gilt die Vorgabe aus `Server.VORGABE`. Die Anmeldeseite zeigt
     * ihn unten an und lässt ihn ändern — dieselbe Zeile wie im Web. Sie ist
     * beim Entwickeln der Unterschied zwischen „die App ist kaputt" und „sie
     * redet mit dem falschen Rechner".
     */
    suspend fun server(): String = lesen(serverSchluessel) ?: Server.VORGABE

    suspend fun serverSetzen(adresse: String) {
        zusammenhang.ablage.edit { it[serverSchluessel] = adresse.trimEnd('/') }
    }

    /**
     * Der Raumcode der laufenden Runde — oder nichts.
     *
     * <b>Er überlebt den Prozess, damit die Runde es tut.</b> Android beendet
     * die App im Hintergrund, wann es will; wer nach zwei Minuten in einer
     * anderen App zurückkommt, steht sonst vor dem Startbildschirm, während
     * seine Schicht ohne ihn weiterläuft. Der Server hält den Platz — die App
     * muss nur wissen, dass sie zurückwill.
     */
    suspend fun offeneRunde(): String? = lesen(rundenSchluessel)

    /**
     * Die Karten-Einwilligung — `"ja"`, `"nein"` oder nichts (noch nicht
     * gefragt).
     *
     * <b>Kacheln erst nach ausdrücklicher Einwilligung</b> (Art. 49 DSGVO,
     * Datenschutzerklärung Ziffer 11) — dieselbe Regel wie im Web: Ohne „ja"
     * wird keine einzige Kachel geladen; Marker und Strecken stehen trotzdem.
     */
    suspend fun karteFreigabe(): String? = lesen(karteFreigabeSchluessel)

    suspend fun karteFreigabeSetzen(wert: String) {
        zusammenhang.ablage.edit { it[karteFreigabeSchluessel] = wert }
    }

    /** Der gewählte Kartenstil — gilt geräteweit für alle Karten gemeinsam. */
    suspend fun karteStil(): String? = lesen(karteStilSchluessel)

    suspend fun karteStilSetzen(wert: String) {
        zusammenhang.ablage.edit { it[karteStilSchluessel] = wert }
    }

    /**
     * Weggeklickte Betreibermitteilungen — nur dieses Gerät, wie der
     * `localStorage`-Eintrag des Web. Es gibt keinen Server-Zustand dafür.
     */
    suspend fun mitteilungenGelesen(): Set<String> =
        lesen(mitteilungenGelesenSchluessel)?.split(",")?.filter { it.isNotBlank() }?.toSet()
            ?: emptySet()

    suspend fun mitteilungGelesenMerken(id: String) {
        val neu = mitteilungenGelesen() + id
        zusammenhang.ablage.edit { it[mitteilungenGelesenSchluessel] = neu.joinToString(",") }
    }

    /**
     * Was der Mitteilungsabruf schon gemeldet hat — damit dieselbe Einladung
     * nicht alle Viertelstunde neu aufs Gerät klopft.
     */
    suspend fun gemeldeteHinweise(): Set<String> =
        lesen(gemeldeteHinweiseSchluessel)?.split(",")?.filter { it.isNotBlank() }?.toSet()
            ?: emptySet()

    suspend fun hinweiseGemeldetMerken(neue: Set<String>) {
        // Der Satz wird bei jedem Lauf frisch geschrieben — was der Server
        // nicht mehr liefert, fällt hier auch wieder heraus.
        zusammenhang.ablage.edit {
            it[gemeldeteHinweiseSchluessel] = neue.joinToString(",")
        }
    }

    /**
     * Der QR-Token des mobilen Begleiters — oder nichts.
     *
     * <b>Ein Melder, der das Beenden der App nicht überlebt, ist keiner.</b>
     * Genau dann, wenn das Handy als Melder auf dem Tisch liegt, räumt Android
     * die App im Hintergrund weg. Der Token gilt acht Stunden; läuft er ab,
     * wird er beim nächsten Versuch still weggeworfen — „ungültig oder
     * abgelaufen" ist am nächsten Morgen keine Begrüßung wert.
     *
     * <b>Er ist kein Ausweis.</b> Der Token trägt keine Kontokennung und öffnet
     * nichts als Funk und Melder eines Platzes in einer Runde; er liegt hier
     * neben dem Raumcode und nicht neben dem Merkmal.
     */
    suspend fun offenerBegleiter(): String? = lesen(begleiterSchluessel)

    suspend fun begleiterMerken(token: String?) {
        zusammenhang.ablage.edit { stand ->
            if (token != null) stand[begleiterSchluessel] = token
            else stand.remove(begleiterSchluessel)
        }
    }

    suspend fun rundeMerken(code: String?) {
        zusammenhang.ablage.edit { stand ->
            if (code != null) stand[rundenSchluessel] = code else stand.remove(rundenSchluessel)
        }
    }

    /**
     * Merkmal und Kennung gemeinsam setzen oder gemeinsam wegwerfen.
     *
     * Gemeinsam, weil das eine ohne das andere nichts nützt: Ein Merkmal ohne
     * Kennung weist eine Anfrage aus, die kein Ziel hat; eine Kennung ohne
     * Merkmal nennt ein Ziel, für das man nicht zuständig ist.
     */
    suspend fun anmeldungMerken(merkmal: String?, kennung: String?) {
        zusammenhang.ablage.edit { stand ->
            if (merkmal != null) stand[merkmalSchluessel] = merkmal else stand.remove(merkmalSchluessel)
            if (kennung != null) stand[kennungSchluessel] = kennung else stand.remove(kennungSchluessel)
        }
    }

    // ------------------------------------------ Gerät und Bedienung (Bereich Konto)
    //
    // Was nur für dieses Gerät gilt und nie zum Server geht: die Gerätekennung für
    // die Kopfzeile `X-PagerSpass-Geraet` und die Einstellungen aus „Deine
    // Bedienung" und dem Melder-Reiter des Profileditors. Im Web sind es
    // `localStorage`-Einträge; die Werte sind zeichengleich dieselben.

    private val geraetSchluessel = stringPreferencesKey("geraet")
    private val eingabewegSchluessel = stringPreferencesKey("notrufEingabeweg")
    private val kennungsformSchluessel = stringPreferencesKey("kennungsform")
    private val bauformSchluessel = stringPreferencesKey("melderBauform")
    private val meldertonSchluessel = stringPreferencesKey("melderTon")
    private val alarmierungSchluessel = stringPreferencesKey("melderAlarmierung")
    private val melderprofilSchluessel = stringPreferencesKey("melderprofil")

    /**
     * Die Gerätekennung dieses Geräts — eine Zufallszahl, sonst nichts.
     *
     * <b>Wozu.</b> Eine dauerhafte Sperre hängt an der Herkunft der Verbindung, und
     * die wechselt beim Schritt vom WLAN ins Mobilfunknetz. Diese Kennung ist das
     * zweite Merkmal: Sie überlebt den Netzwechsel. <b>Kein Fingerabdruck</b> —
     * hier wird nichts vermessen; wer die App-Daten löscht, hat eine neue.
     *
     * Sie entsteht in einer Schreibtransaktion: Zwei Anfragen, die gleichzeitig
     * die erste Kennung wollen, bekommen dieselbe.
     */
    suspend fun geraetekennung(): String {
        geraetZwischenstand?.let { return it }
        var ergebnis = ""
        zusammenhang.ablage.edit { stand ->
            val vorhanden = stand[geraetSchluessel]
            ergebnis = if (vorhanden != null && vorhanden.length >= 16) {
                vorhanden
            } else {
                java.util.UUID.randomUUID().toString().replace("-", "").also {
                    stand[geraetSchluessel] = it
                }
            }
        }
        geraetZwischenstand = ergebnis
        return ergebnis
    }

    /**
     * Der Eingabeweg am Notruftelefon: `fragen`, `tippen` oder `sprechen`.
     * Vorgabe `fragen` — wie `useEingabeweg` im Web.
     */
    fun eingabewegFluss(): Flow<String> = zusammenhang.ablage.data.map {
        auswahl(it[eingabewegSchluessel], EINGABEWEGE)
    }

    suspend fun eingabewegSetzen(wert: String) = setzen(eingabewegSchluessel, wert, EINGABEWEGE)

    /**
     * Wie Fahrzeuge in den Listen heißen: `kennzahl`, `typ` oder `orga`.
     * Vorgabe `kennzahl` — wie `useFahrzeugkennung` im Web.
     */
    fun kennungsformFluss(): Flow<String> = zusammenhang.ablage.data.map {
        auswahl(it[kennungsformSchluessel], KENNUNGSFORMEN)
    }

    suspend fun kennungsformSetzen(wert: String) = setzen(kennungsformSchluessel, wert, KENNUNGSFORMEN)

    /** Die Melder-Bauform dieses Geräts (Id aus `MELDER_BAUFORMEN`), Vorgabe `dienst`. */
    fun melderBauformFluss(): Flow<String> = zusammenhang.ablage.data.map {
        it[bauformSchluessel] ?: "dienst"
    }

    suspend fun melderBauformSetzen(wert: String) {
        zusammenhang.ablage.edit { it[bauformSchluessel] = wert }
    }

    /** Der Alarmton dieses Geräts (Id aus `MELDER_TOENE`), Vorgabe `zweiklang`. */
    fun melderTonFluss(): Flow<String> = zusammenhang.ablage.data.map {
        it[meldertonSchluessel] ?: "zweiklang"
    }

    suspend fun melderTonSetzen(wert: String) {
        zusammenhang.ablage.edit { it[meldertonSchluessel] = wert }
    }

    /** Wie der Melder ankündigt: `voll`, `ton`, `vibration` oder `stumm`. */
    fun alarmierungsartFluss(): Flow<String> = zusammenhang.ablage.data.map {
        auswahl(it[alarmierungSchluessel], ALARMIERUNGSARTEN)
    }

    /** Das Melderprofil (Id aus `MELDERPROFILE`), Vorgabe `vollalarm`. */
    fun melderprofilFluss(): Flow<String> = zusammenhang.ablage.data.map {
        it[melderprofilSchluessel] ?: "vollalarm"
    }

    /**
     * Setzt die Alarmierungsart von Hand. Das Profil bleibt dabei stehen — im Web
     * genauso: Das Profil ist ein Voreinstellungsknopf, kein Zustand, der jede
     * spätere Änderung sperrt.
     */
    suspend fun alarmierungsartSetzen(wert: String) =
        setzen(alarmierungSchluessel, wert, ALARMIERUNGSARTEN)

    /** Ein Profil setzt Alarmierungsart und Profil gemeinsam — `melderprofilSetzen` im Web. */
    suspend fun melderprofilSetzen(id: String, art: String) {
        zusammenhang.ablage.edit {
            it[melderprofilSchluessel] = id
            it[alarmierungSchluessel] = auswahl(art, ALARMIERUNGSARTEN)
        }
    }

    /** Wie viele Einträge dieses Gerät für PagerSpass aufbewahrt. */
    suspend fun eintragszahl(): Int = zusammenhang.ablage.data.first().asMap().size

    /**
     * Vergisst alles, was dieses Gerät für PagerSpass aufbewahrt — Anmeldung,
     * Kartenfreigabe, Bedienung, Melder. Der Server bleibt stehen; wer ihn geändert
     * hatte, soll nicht versehentlich mit dem Betrieb reden.
     */
    suspend fun allesVergessen() {
        val server = lesen(serverSchluessel)
        zusammenhang.ablage.edit { stand ->
            stand.clear()
            if (server != null) stand[serverSchluessel] = server
        }
        geraetZwischenstand = null
    }

    private suspend fun setzen(
        schluessel: androidx.datastore.preferences.core.Preferences.Key<String>,
        wert: String,
        erlaubt: List<String>,
    ) {
        zusammenhang.ablage.edit { it[schluessel] = auswahl(wert, erlaubt) }
    }

    private fun auswahl(wert: String?, erlaubt: List<String>): String =
        wert?.takeIf { it in erlaubt } ?: erlaubt.first()

    companion object {
        /** Die drei Eingabewege — der erste ist die Vorgabe. */
        val EINGABEWEGE = listOf("fragen", "tippen", "sprechen")

        /** Die drei Kennungsformen — die erste ist die Vorgabe. */
        val KENNUNGSFORMEN = listOf("kennzahl", "typ", "orga")

        /** Die vier Alarmierungsarten — die erste ist die Vorgabe. */
        val ALARMIERUNGSARTEN = listOf("voll", "ton", "vibration", "stumm")

        /** Die Gerätekennung, einmal gelesen — sie steht an jeder Anfrage. */
        @Volatile
        private var geraetZwischenstand: String? = null
    }

    private suspend fun lesen(schluessel: androidx.datastore.preferences.core.Preferences.Key<String>) =
        zusammenhang.ablage.data.first()[schluessel]
}

/** Die Server, mit denen die App sprechen kann. */
object Server {
    /** Der Betrieb. */
    const val BETRIEB = "https://pagerspass.de"

    /** Der Vorabstand — dieselbe App, anderer Datenbestand. */
    const val BETA = "https://beta.pagerspass.de"

    /**
     * Die API auf der Entwicklungsmaschine.
     *
     * `localhost` und nicht `10.0.2.2`: Auf dieser Maschine bleibt der SYN vom
     * Emulator zum Wirt unbeantwortet, und die App wirkt zehn Sekunden lang tot,
     * bis der Zeitablauf zuschlägt. Der Weg, der geht, ist
     * `adb reverse tcp:5473 tcp:5473` — nach jedem Neustart von adb oder
     * Emulator neu zu setzen.
     */
    const val ENTWICKLUNG = "http://localhost:5473"

    /** Womit die App startet, wenn niemand etwas anderes eingestellt hat. */
    val VORGABE: String
        get() = if (de.pagerspass.pagerspass.BuildConfig.DEBUG) ENTWICKLUNG else BETRIEB

    /** Die Auswahl, die die Anmeldeseite anbietet. */
    val AUSWAHL = listOf(BETRIEB, BETA, ENTWICKLUNG)
}
