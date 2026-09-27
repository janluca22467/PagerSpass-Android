package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * Die Wege der vorbereiteten Übungen und der Lehrgänge — übertragen aus
 * `web/src/api/rest.ts` (`szenarien…`, `lehrgaenge…`, `pruefungAnlegen`).
 *
 * <b>Eine eigene Klasse neben `Spielwege`</b>, weil das eine eigene Sorte Weg ist:
 * Ein Szenario ist der einzige Gegenstand des Spiels, den ein Spieler *baut* statt
 * spielt — anlegen, ändern, löschen, weitergeben. Dieselbe Trennung zieht der
 * Server mit `SzenarioEndpunkte.cs`.
 *
 * <b>Die Kennung steht im Abfrageteil</b>, nicht im Pfad — anders als bei den
 * Kontowegen. Sie ist der Handelnde; geprüft hat sie die Kontowache am Server.
 */
class Uebungswege(private val netz: Netz) {

    // ------------------------------------------------------------ Übungen

    /** Die eigenen Übungen, die zuletzt geänderte zuerst. */
    suspend fun szenarien(kennung: String): List<Szenariozeile> =
        netz.hole("/api/szenarien?kennung=${teil(kennung)}")

    /** Der eigene Übungsverlauf: was wann gefahren wurde, das Neueste zuerst. */
    suspend fun verlauf(kennung: String): List<Uebungsfahrt> =
        netz.hole("/api/szenarien/verlauf?kennung=${teil(kennung)}")

    /** Eine Übung mit allem, was drinsteht — zum Öffnen im Editor. */
    suspend fun szenario(id: String, kennung: String): Szenario =
        netz.hole("/api/szenarien/${teil(id)}?kennung=${teil(kennung)}")

    /**
     * Legt eine Übung an oder schreibt sie fort — ohne `id` entsteht eine neue.
     * Zurück kommt sie so, wie der Server sie abgelegt hat, samt Code.
     */
    suspend fun sichern(kennung: String, anfrage: Szenarioanfrage): Szenario =
        netz.hole(
            "/api/szenarien?kennung=${teil(kennung)}",
            "POST",
            MIT_VORGABEN.encodeToString(Szenarioanfrage.serializer(), anfrage),
        )

    suspend fun loeschen(id: String, kennung: String) {
        netz.ohneAntwort("/api/szenarien/${teil(id)}?kennung=${teil(kennung)}", "DELETE")
    }

    /** Übernimmt eine fremde Übung per Code — als eigene Kopie mit eigenem Code. */
    suspend fun uebernehmen(code: String, kennung: String): Szenario =
        netz.hole("/api/szenarien/code/${teil(code)}?kennung=${teil(kennung)}", "POST")

    /** Eröffnet eine Runde, die diese Übung fährt, und gibt ihren Raumcode zurück. */
    suspend fun rundeAnlegen(id: String, kennung: String): String =
        netz.hole<Raum>(
            "/api/rooms/szenario?id=${teil(id)}&kennung=${teil(kennung)}",
            "POST",
        ).code.ifBlank { throw Netzfehler("Die Runde ließ sich nicht eröffnen.") }

    // ---------------------------------------------------------- Lehrgänge

    suspend fun lehrgaenge(kennung: String): List<Lehrgang> =
        netz.hole("/api/lehrgaenge?kennung=${teil(kennung)}")

    /**
     * Hakt ein Lesestoff-Modul ab. Lektion und Prüfung erledigen sich durch
     * Fahren — dafür gibt es hier bewusst keinen Weg.
     */
    suspend fun modulErledigen(lehrgangId: String, modulId: String, kennung: String) {
        // Die Antwort (`{ erledigt }`) liest niemand — die Seite lädt danach ohnehin
        // den ganzen Lehrgang neu.
        netz.ohneAntwort(
            "/api/lehrgaenge/${teil(lehrgangId)}/module/${teil(modulId)}?kennung=${teil(kennung)}",
            "POST",
        )
    }

    /** Eröffnet die Prüfungsschicht eines Moduls und gibt ihren Raumcode zurück. */
    suspend fun pruefungAnlegen(lehrgangId: String, modulId: String, kennung: String): String =
        netz.hole<Raum>(
            "/api/rooms/pruefung?lehrgang=${teil(lehrgangId)}" +
                "&modul=${teil(modulId)}&kennung=${teil(kennung)}",
            "POST",
        ).code.ifBlank { throw Netzfehler("Die Prüfungsschicht ließ sich nicht eröffnen.") }

    private companion object {
        /**
         * Derselbe Umgang mit JSON wie in `Netz`, nur **mit** den Vorgaben.
         *
         * <b>Ohne das fiele genau das Falsche weg.</b> `Netz.abgabe` lässt beim
         * Senden alles aus, was seiner Vorgabe gleicht — und ein Eintrag der Art
         * „Lage" nach null Sekunden hat genau die Vorgaben. Er käme ohne Art und
         * ohne Zeit am Server an. `null` bleibt trotzdem draußen
         * (`explicitNulls = false`): Das heißt dort „keine Angabe".
         */
        val MIT_VORGABEN = Json(from = Netz.abgabe) { encodeDefaults = true }

        fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
    }
}
