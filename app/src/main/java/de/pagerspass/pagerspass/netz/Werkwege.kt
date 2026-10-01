package de.pagerspass.pagerspass.netz

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * Die Wege der Werkstatt — das Gegenstück zu den Abschnitten „Lehrgänge",
 * „Übungen", „Selbst gebaute Leitstellen", „Rundenvorlagen", Archiv und
 * Dienstbuch in `web/src/api/rest.ts`.
 *
 * <b>Dieselben Pfade, dieselben Rümpfe.</b> Die Kennung steht fast überall im
 * Abfrageteil (`?kennung=`) und nicht im Pfad — der Server prüft sie in der
 * Kontowache, bevor der Endpunkt überhaupt läuft. Wo sie im Pfad steht
 * (`/api/konto/{k}/…`), steht sie dort auch im Web.
 *
 * <b>Beim Senden werden Vorgaben mitgeschickt</b> (`encodeDefaults`). Die
 * gemeinsame Abgabe lässt sie weg, und bei einem Editor ist das eine Falle: Ein
 * Haken, der auf seiner Vorgabe `true` steht, käme als „nicht gesetzt" an, und
 * der Server setzte seine eigene Vorgabe ein. Was `null` ist, bleibt draußen —
 * das heißt beim Server ausdrücklich „wie im Kreis".
 */
class Werkwege(private val netz: Netz) {

    // ------------------------------------------------------------ Dienstbuch

    suspend fun archiv(kennung: String, anzahl: Int = 50): List<Archiveintrag> =
        netz.hole("/api/archiv?kennung=${stueck(kennung)}&anzahl=$anzahl")

    suspend fun archivrunde(code: String, kennung: String): Archivrunde =
        netz.hole("/api/archiv/${stueck(code)}?kennung=${stueck(kennung)}")

    suspend fun archivstatistik(kennung: String, runden: Int = 10): Archivstatistik =
        netz.hole("/api/archiv/statistik?kennung=${stueck(kennung)}&runden=$runden")

    /** Die Auswertung über alle Schichten — ohne Abo antwortet der Server mit 403. */
    suspend fun dienstauswertung(kennung: String): Dienstauswertung =
        netz.hole("/api/archiv/auswertung?kennung=${stueck(kennung)}")

    suspend fun chronik(kennung: String, anzahl: Int = 100): List<Schicht> =
        netz.hole("/api/konto/${stueck(kennung)}/chronik?anzahl=$anzahl")

    /** Wofür es in einer bestimmten Schicht Punkte gab. */
    suspend fun buchungen(kennung: String, code: String): List<Erfahrungsposten> =
        netz.hole("/api/konto/${stueck(kennung)}/chronik/${stueck(code)}")

    suspend fun abzeichen(kennung: String): List<Abzeichen> =
        netz.hole("/api/konto/${stueck(kennung)}/abzeichen")

    /** Gutscheine gelten je Staat (v6) — `staat` sagt, wessen. Ältere Server überlesen es. */
    suspend fun garagenauszug(kennung: String, staat: String = Staaten.DEUTSCHLAND): Garagenauszug =
        netz.hole("/api/konto/${stueck(kennung)}/garage?staat=${stueck(staat)}")

    suspend fun laufbahn(): List<Rang> = netz.hole("/api/laufbahn")

    suspend fun bestenliste(kennung: String, anzahl: Int = 25): List<Bestenlistenplatz> =
        netz.hole("/api/bestenliste?anzahl=$anzahl&kennung=${stueck(kennung)}")

    suspend fun saisonbestenliste(kennung: String, anzahl: Int = 25): List<Saisonplatz> =
        netz.hole("/api/bestenliste/saison?anzahl=$anzahl&kennung=${stueck(kennung)}")

    suspend fun tagesschicht(kennung: String): Tagesschicht =
        netz.hole("/api/tagesschicht?kennung=${stueck(kennung)}")

    /** Die Zusatzfunktionen des Kontos — aus dem Konto gelesen, nur diese eine Liste. */
    suspend fun freischaltungen(kennung: String): List<Freischaltung> =
        netz.hole<Kontofreischaltungen>("/api/konto/${stueck(kennung)}").freischaltungen

    /** Die eigene Gemeinschaft — `null`, wenn man in keiner ist (leerer Rumpf). */
    suspend fun eigeneGemeinschaft(kennung: String): Gemeinschaft? =
        netz.hole("/api/gemeinschaften/${stueck(kennung)}")

    suspend fun hilfsfristen(kennung: String, id: String): List<Mitgliedshilfsfrist> =
        netz.hole("/api/gemeinschaften/${stueck(kennung)}/${stueck(id)}/hilfsfristen")

    /** Die Vitrine steht am Profil; gelesen wird sie über den eigenen Benutzernamen. */
    suspend fun vitrine(kennung: String, benutzername: String): List<String> =
        netz.hole<Vitrinenprofil>("/api/profil/${stueck(kennung)}/${stueck(benutzername)}")
            .vitrine.map { it.id }

    suspend fun vitrineSetzen(kennung: String, ids: List<String>) = netz.ohneAntwort(
        "/api/konto/${stueck(kennung)}/profil",
        "PUT",
        senden.encodeToString(Vitrinenaenderung.serializer(), Vitrinenaenderung(ids)),
    )

    // --------------------------------------------------------------- Lehrgang

    suspend fun lehrgaenge(kennung: String): List<Lehrgang> =
        netz.hole("/api/lehrgaenge?kennung=${stueck(kennung)}")

    /** Hakt ein Lesestoff-Modul ab. Lektion und Prüfung erledigen sich durch Fahren. */
    suspend fun modulErledigen(lehrgangId: String, modulId: String, kennung: String) =
        netz.ohneAntwort(
            "/api/lehrgaenge/${stueck(lehrgangId)}/module/${stueck(modulId)}?kennung=${stueck(kennung)}",
            "POST",
        )

    suspend fun pruefungAnlegen(lehrgangId: String, modulId: String, kennung: String): String =
        netz.hole<Raumcode>(
            "/api/rooms/pruefung?lehrgang=${stueck(lehrgangId)}&modul=${stueck(modulId)}" +
                "&kennung=${stueck(kennung)}",
            "POST",
        ).code

    // ---------------------------------------------------------------- Übungen

    suspend fun szenarien(kennung: String): List<Szenariozeile> =
        netz.hole("/api/szenarien?kennung=${stueck(kennung)}")

    suspend fun uebungsverlauf(kennung: String): List<Uebungsfahrt> =
        netz.hole("/api/szenarien/verlauf?kennung=${stueck(kennung)}")

    suspend fun szenario(id: String, kennung: String): Szenario =
        netz.hole("/api/szenarien/${stueck(id)}?kennung=${stueck(kennung)}")

    /** Legt an oder schreibt fort — ohne `id` entsteht eine neue. */
    suspend fun szenarioSichern(kennung: String, szenario: Szenario): Szenario {
        val rumpf = senden.encodeToJsonElement(Szenario.serializer(), szenario).let { json ->
            // Id leer heißt neu; `geaendertUm` und `code` vergibt der Server.
            kotlinx.serialization.json.JsonObject(
                json.jsonObjectOhne("code", "geaendertUm")
                    .let { if (szenario.id.isBlank()) it - "id" else it },
            )
        }
        return netz.hole("/api/szenarien?kennung=${stueck(kennung)}", "POST", rumpf.toString())
    }

    suspend fun szenarioLoeschen(id: String, kennung: String) =
        netz.ohneAntwort("/api/szenarien/${stueck(id)}?kennung=${stueck(kennung)}", "DELETE")

    suspend fun szenarioUebernehmen(code: String, kennung: String): Szenario =
        netz.hole("/api/szenarien/code/${stueck(code)}?kennung=${stueck(kennung)}", "POST")

    suspend fun szenarioRunde(id: String, kennung: String): String =
        netz.hole<Raumcode>(
            "/api/rooms/szenario?id=${stueck(id)}&kennung=${stueck(kennung)}",
            "POST",
        ).code

    // --------------------------------------------------------- Leitstellenbau

    suspend fun kreiswachen(landkreisId: String): List<Kreiswache> =
        netz.hole("/api/landkreise/${stueck(landkreisId)}/wachen")

    suspend fun vorlagen(kennung: String): List<Vorlagenzeile> =
        netz.hole("/api/vorlagen?kennung=${stueck(kennung)}")

    suspend fun vorlage(id: String, kennung: String): Leitstellenvorlage =
        netz.hole("/api/vorlagen/${stueck(id)}?kennung=${stueck(kennung)}")

    suspend fun vorlageSichern(kennung: String, vorlage: Leitstellenvorlage): Leitstellenvorlage {
        val json = senden.encodeToJsonElement(Leitstellenvorlage.serializer(), vorlage)
        val rumpf = kotlinx.serialization.json.JsonObject(
            json.jsonObjectOhne("code", "geaendertUm")
                .let { if (vorlage.id.isBlank()) it - "id" else it },
        )
        return netz.hole("/api/vorlagen?kennung=${stueck(kennung)}", "POST", rumpf.toString())
    }

    suspend fun vorlageLoeschen(id: String, kennung: String) =
        netz.ohneAntwort("/api/vorlagen/${stueck(id)}?kennung=${stueck(kennung)}", "DELETE")

    suspend fun vorlageUebernehmen(code: String, kennung: String): Leitstellenvorlage =
        netz.hole("/api/vorlagen/code/${stueck(code)}?kennung=${stueck(kennung)}", "POST")

    suspend fun sandkasten(id: String, kennung: String): String =
        netz.hole<Raumcode>(
            "/api/rooms/sandkasten?id=${stueck(id)}&kennung=${stueck(kennung)}",
            "POST",
        ).code

    // --------------------------------------------------------- Rundenvorlagen

    suspend fun rundenvorlagen(kennung: String): List<Rundenvorlagenzeile> =
        netz.hole("/api/rundenvorlagen?kennung=${stueck(kennung)}")

    suspend fun rundenvorlageLoeschen(id: String, kennung: String) =
        netz.ohneAntwort("/api/rundenvorlagen/${stueck(id)}?kennung=${stueck(kennung)}", "DELETE")

    suspend fun rundenvorlageUebernehmen(code: String, kennung: String): Rundenvorlagenzeile =
        netz.hole("/api/rundenvorlagen/code/${stueck(code)}?kennung=${stueck(kennung)}", "POST")

    suspend fun rundenvorlageStarten(id: String, kennung: String): String =
        netz.hole<Raumcode>(
            "/api/rooms/rundenvorlage?id=${stueck(id)}&kennung=${stueck(kennung)}",
            "POST",
        ).code

    suspend fun vorlageninhalt(id: String, kennung: String): Vorlageninhalt =
        netz.hole("/api/rundenvorlagen/${stueck(id)}/inhalt?kennung=${stueck(kennung)}")

    /**
     * Name, Plätze und Aufstellung ersetzen; die Regler bleiben unberührt.
     *
     * `maxSpieler = null` geht bewusst **nicht** mit — das heißt beim Server „so
     * viele, wie der Kreis hergibt", und genau das meint ein leeres Feld.
     */
    suspend fun vorlageninhaltSichern(
        id: String,
        kennung: String,
        inhalt: Vorlageninhalt,
    ): Rundenvorlagenzeile {
        val rumpf = buildString {
            append("{\"name\":")
            append(senden.encodeToString(kotlinx.serialization.serializer<String>(), inhalt.name))
            if (inhalt.maxSpieler != null) append(",\"maxSpieler\":${inhalt.maxSpieler}")
            append(",\"bestand\":")
            append(
                senden.encodeToString(
                    ListSerializer(Vorlagenbesatzung.serializer()),
                    inhalt.bestand,
                ),
            )
            append("}")
        }
        return netz.hole(
            "/api/rundenvorlagen/${stueck(id)}/inhalt?kennung=${stueck(kennung)}",
            "PUT",
            rumpf,
        )
    }

    // -------------------------------------------------------- Startbildschirm

    suspend fun startkacheln(): List<Startkachel> = netz.hole("/api/startkacheln")

    suspend fun footer(): List<Footerknopf> = netz.hole("/api/footer")

    suspend fun umfrage(kennung: String): OffeneUmfrage? =
        netz.hole<Umfragestand>("/api/konto/${stueck(kennung)}/umfrage").umfrage

    suspend fun umfrageAntworten(kennung: String, umfrageId: String, option: Int) =
        netz.ohneAntwort(
            "/api/konto/${stueck(kennung)}/umfrage/${stueck(umfrageId)}",
            "POST",
            senden.encodeToString(Umfrageantwort.serializer(), Umfrageantwort(option)),
        )

    private companion object {
        /** Siehe oben: Vorgaben mit, `null` draußen. */
        val senden = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }
    }
}

private fun kotlinx.serialization.json.JsonElement.jsonObjectOhne(
    vararg schluessel: String,
): Map<String, kotlinx.serialization.json.JsonElement> =
    (this as kotlinx.serialization.json.JsonObject).filterKeys { it !in schluessel }

private fun stueck(wert: String): String = URLEncoder.encode(wert, "UTF-8")
