package de.pagerspass.pagerspass.netz

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

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
    private val serverVonHandSchluessel = stringPreferencesKey("serverVonHand")
    private val rundenSchluessel = stringPreferencesKey("offeneRunde")
    private val begleiterSchluessel = stringPreferencesKey("begleiterToken")
    private val karteFreigabeSchluessel = stringPreferencesKey("karteFreigabe")
    private val karteStilSchluessel = stringPreferencesKey("karteStil")
    private val mitteilungenGelesenSchluessel = stringPreferencesKey("mitteilungenGelesen")
    private val gemeldeteHinweiseSchluessel = stringPreferencesKey("gemeldeteHinweise")
    private val weggelegteVorschlaegeSchluessel = stringPreferencesKey("vorschlaegeWeg")

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

    /**
     * Den Server ablegen — und dazu, ob ihn jemand auf der Anmeldeseite von Hand
     * gewählt hat oder ob er aus der zentralen Vorgabe der Verwaltung stammt
     * (siehe `Zentralserver`). Nur ein von Hand gewählter bleibt stehen, wenn die
     * Vorgabe sich ändert.
     */
    suspend fun serverSetzen(adresse: String, vonHand: Boolean = true) {
        zusammenhang.ablage.edit {
            it[serverSchluessel] = adresse.trimEnd('/')
            it[serverVonHandSchluessel] = if (vonHand) "ja" else "nein"
        }
    }

    /**
     * Ob der Server von Hand gewählt ist.
     *
     * <b>Ohne Vermerk zählt ein abgelegter Server als von Hand.</b> Vor der
     * zentralen Vorgabe legte nur die Anmeldeseite einen Server ab — wer dort
     * die Beta gewählt hat, soll nach dem Update nicht still auf pagerspass.de
     * landen und abgemeldet sein. Er sieht stattdessen den Hinweis mit
     * „Zentrale Vorgabe verwenden".
     */
    suspend fun serverVonHand(): Boolean = when (lesen(serverVonHandSchluessel)) {
        "ja" -> true
        "nein" -> false
        else -> lesen(serverSchluessel) != null
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
     * Weggelegte Freundschaftsvorschläge — Kennungen, nur dieses Gerät.
     *
     * Wie `pagerspass.vorschlaege.weg` im `localStorage` des Web: Der Server
     * soll kein Gedächtnis dafür haben, wen man nicht vorgeschlagen bekommen
     * will. „Wieder zeigen" leert den Eintrag.
     */
    suspend fun weggelegteVorschlaege(): Set<String> =
        lesen(weggelegteVorschlaegeSchluessel)?.split(",")?.filter { it.isNotBlank() }?.toSet()
            ?: emptySet()

    suspend fun weggelegteVorschlaegeMerken(kennungen: Set<String>) {
        zusammenhang.ablage.edit { stand ->
            if (kennungen.isEmpty()) stand.remove(weggelegteVorschlaegeSchluessel)
            else stand[weggelegteVorschlaegeSchluessel] = kennungen.joinToString(",")
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

    /**
     * Alles vergessen — „Gespeicherte Daten dieses Geräts löschen“ in der
     * Privatsphäre. Danach steht die App da wie frisch installiert.
     */
    suspend fun allesVergessen() {
        zusammenhang.ablage.edit { it.clear() }
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
     * Der Zweigstand V6 (`deploy.sh --ziel v6`) — erreicht den Release nie, eigene
     * Daten wie die Beta.
     */
    const val V6 = "https://v6.pagerspass.de"

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

    /**
     * Die Auswahl, die die Anmeldeseite anbietet. Die Entwicklungsmaschine nur im
     * Debug-Bau: Auf einem Telefon im Feld ist `localhost` nie erreichbar, und ein
     * Knopf, der immer ins Leere führt, ist schlechter als keiner.
     */
    val AUSWAHL: List<String>
        get() = listOfNotNull(
            BETRIEB,
            BETA,
            V6,
            ENTWICKLUNG.takeIf { de.pagerspass.pagerspass.BuildConfig.DEBUG },
        )

    /** Wie ein Server in der Auswahl heißt. */
    fun name(adresse: String): String = when (adresse.trimEnd('/')) {
        BETRIEB -> "Normal"
        BETA -> "Beta"
        V6 -> "V6"
        ENTWICKLUNG -> "Entwicklung"
        else -> adresse.substringAfter("://").substringBefore(":")
    }
}
