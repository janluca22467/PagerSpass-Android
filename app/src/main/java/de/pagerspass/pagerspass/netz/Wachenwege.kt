package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege der Wachengemeinschaft — übertragen aus dem Abschnitt
 * „Wachengemeinschaften" in `web/src/api/rest.ts`.
 *
 * <b>Getrennt von `Spielwege`</b>, weil der Wachenbereich mit Laufbahn,
 * Rangliste, Kasse und Einstellungen inzwischen dreißig eigene Wege hat. Wege,
 * die es dort schon gab, stehen hier ein zweites Mal, wo ihre Antwort
 * gebraucht wird: Beförderung, Aushang und Leitungswechsel antworten mit dem
 * vollen `GemeinschaftDetail`, und wer die Antwort wegwirft, fragt gleich darauf
 * dasselbe noch einmal.
 *
 * <b>Die Kennung steht im Pfad</b>, wie überall: Das Merkmal weist die Anfrage
 * aus, die Kennung sagt, um wessen Daten es geht.
 */
class Wachenwege(private val netz: Netz) {

    // ------------------------------------------------------------ Kurzform

    /** Die eigene Wache — oder `null`. Ein leerer Rumpf ist hier der Normalfall. */
    suspend fun gemeinschaft(kennung: String): Gemeinschaft? =
        netz.hole("/api/gemeinschaften/${stelle(kennung)}")

    /**
     * Die öffentliche Liste, eingegrenzt am Server.
     *
     * <b>Gefiltert wird dort und nicht hier:</b> Der Server schneidet nach
     * `anzahl` Zeilen ab, und wer erst dreißig holt und dann siebenundzwanzig
     * wegwirft, behauptet, mehr gebe es nicht.
     */
    suspend fun oeffentliche(filter: Gemeinschaftsfilter, anzahl: Int = 30): List<Gemeinschaft> {
        val teile = buildList {
            add("anzahl=$anzahl")
            filter.suche.trim().takeIf { it.isNotEmpty() }?.let { add("suche=${stelle(it)}") }
            filter.landkreisId?.let { add("landkreisId=${stelle(it)}") }
            filter.modus?.let { add("modus=${stelle(it)}") }
            if (filter.nurFreie) add("nurFreie=true")
            if (filter.nurPassende) add("nurPassende=true")
        }
        return netz.hole("/api/gemeinschaften/oeffentlich?${teile.joinToString("&")}")
    }

    /** Der Landkreis, in dem dieses Konto ohnehin fährt — füllt das Gründen vor. */
    suspend fun landkreisvorschlag(kennung: String): Landkreisvorschlag =
        netz.hole("/api/gemeinschaften/vorschlag-landkreis/${stelle(kennung)}")

    suspend fun detail(kennung: String, id: String): GemeinschaftDetail =
        netz.hole("/api/gemeinschaften/${stelle(kennung)}/detail/${stelle(id)}")

    // ---------------------------------------------------- Gründen, Beitreten

    suspend fun gruenden(
        kennung: String,
        name: String,
        beschreibung: String?,
        landkreisId: String?,
        landkreis: String?,
    ): Gemeinschaft = netz.hole(
        "/api/gemeinschaften/${stelle(kennung)}",
        "POST",
        buildJsonObject {
            put("name", name)
            // Wie im Web: Was fehlt, fehlt — `undefined` statt `null`.
            beschreibung?.let { put("beschreibung", it) }
            landkreisId?.let { put("landkreisId", it) }
            landkreis?.let { put("landkreis", it) }
        }.toString(),
    )

    /** Beitritt über den sechsstelligen Code — derselbe Weg, den ein QR-Code öffnet. */
    suspend fun beitretenMitCode(kennung: String, code: String): Gemeinschaft = netz.hole(
        "/api/gemeinschaften/${stelle(kennung)}/beitrittscode",
        "POST",
        buildJsonObject { put("code", code.trim().uppercase()) }.toString(),
    )

    /** Bewerbung. Bei einer offenen Wache tritt man sofort bei (`beigetreten`). */
    suspend fun bewerben(kennung: String, id: String, nachricht: String?): Boolean {
        val antwort = netz.hole<JsonObject>(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/bewerben",
            "POST",
            buildJsonObject {
                put("nachricht", nachricht?.let { JsonPrimitive(it) } ?: JsonNull)
            }.toString(),
        )
        return (antwort["beigetreten"] as? JsonPrimitive)?.content == "true"
    }

    /** Die offenen Vorgänge des Kontos: Einladungen an einen und eigene Bewerbungen. */
    suspend fun antraege(kennung: String): List<Gemeinschaftsantrag> =
        netz.hole("/api/gemeinschaften/${stelle(kennung)}/antraege")

    /**
     * Entscheiden — über einen Antrag an die Wache, eine Einladung an einen selbst
     * oder, mit `annehmen = false`, die eigene Bewerbung zurückziehen.
     *
     * @return Wer dadurch beigetreten ist — `null`, wenn niemand.
     */
    suspend fun antragEntscheiden(kennung: String, nr: Long, annehmen: Boolean): String? {
        val antwort = netz.hole<JsonObject?>(
            "/api/gemeinschaften/${stelle(kennung)}/antraege/$nr",
            "POST",
            buildJsonObject { put("annehmen", annehmen) }.toString(),
        )
        val wer = antwort?.get("beigetreten")
        return (wer as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
    }

    /** Einladen — `wen` ist die <b>Kennung</b>, nie der Benutzername. */
    suspend fun einladen(kennung: String, id: String, wen: String) = netz.ohneAntwort(
        "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/einladen",
        "POST",
        buildJsonObject { put("kennung", wen) }.toString(),
    )

    /**
     * Die Suche über den eindeutigen Benutzernamen.
     *
     * Findet sie niemanden, antwortet der Server mit 404 und dem Satz „Diesen
     * Benutzernamen gibt es nicht." — der kommt als `Netzfehler` an und steht
     * dann unter dem Suchfeld.
     */
    suspend fun benutzerSuchen(kennung: String, benutzername: String): Wachensuchtreffer? =
        netz.hole(
            "/api/freunde/${stelle(kennung)}/suche?benutzername=${stelle(benutzername.trim())}",
        )

    // ------------------------------------------------------------ Mannschaft

    suspend fun rolleSetzen(kennung: String, id: String, wen: String, rolle: String): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/rolle",
            "POST",
            buildJsonObject {
                put("kennung", wen)
                put("rolle", rolle)
            }.toString(),
        )

    suspend fun leitungUebertragen(kennung: String, id: String, an: String): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/leitung",
            "POST",
            buildJsonObject { put("kennung", an) }.toString(),
        )

    /** Entfernt ein Mitglied — oder, mit der eigenen Kennung als `wen`, tritt aus. */
    suspend fun mitgliedEntfernen(kennung: String, id: String, wen: String) = netz.ohneAntwort(
        "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/mitglieder/${stelle(wen)}",
        "DELETE",
    )

    // ------------------------------------------------------------------ Chat

    /** Meldet eine Chatzeile bei der Wachenführung — der Grund ist freiwillig. */
    suspend fun nachrichtMelden(kennung: String, nr: Long, grund: String?) = netz.ohneAntwort(
        "/api/gemeinschaften/${stelle(kennung)}/nachrichten/$nr/melden",
        "POST",
        buildJsonObject {
            // `undefined` im Web: ohne Grund fehlt das Feld ganz.
            grund?.let { put("grund", it) }
        }.toString(),
    )

    /** Nimmt eine Zeile heraus — die eigene jeder, fremde ab Zugführer. */
    suspend fun nachrichtEntfernen(kennung: String, nr: Long) = netz.ohneAntwort(
        "/api/gemeinschaften/${stelle(kennung)}/nachrichten/$nr",
        "DELETE",
    )

    /** „Angesehen, in Ordnung" — die Zeile bleibt, die Meldung ist abgehakt. */
    suspend fun meldungErledigen(kennung: String, nr: Long) = netz.ohneAntwort(
        "/api/gemeinschaften/${stelle(kennung)}/meldungen/$nr/erledigt",
        "POST",
    )

    /** Heftet einen Aushang an — `null` nimmt ihn ab. */
    suspend fun pinnwandSpeichern(kennung: String, id: String, text: String?): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/pinnwand",
            "PUT",
            buildJsonObject {
                put("text", text?.let { JsonPrimitive(it) } ?: JsonNull)
            }.toString(),
        )

    // ------------------------------------------------------------- Clanrunde

    /**
     * Nimmt den Hinweis auf die laufende Clanrunde ab. Die Runde läuft weiter —
     * sie verschwindet nur aus Startbildschirm und Wachenseite.
     */
    suspend fun clanrundeSchliessen(kennung: String, id: String) = netz.ohneAntwort(
        "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/clanrunde",
        "DELETE",
    )

    // ----------------------------------------------------------- Dienstplan

    /**
     * Einen Dienst planen.
     *
     * @param wann Ein Zeitpunkt mit Zone (ISO-8601, UTC) — kein Ortszeit-Text.
     *   Hier stand früher ein fest angehängtes `+02:00`, und im Winter lag jeder
     *   geplante Dienst eine Stunde daneben.
     */
    suspend fun terminPlanen(kennung: String, id: String, titel: String, wann: String): Wachentermin =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/termine",
            "POST",
            buildJsonObject {
                put("titel", titel)
                put("wann", wann)
            }.toString(),
        )

    /** `Zugesagt`, `Abgesagt` oder `Offen`. */
    suspend fun terminBeantworten(kennung: String, nr: Long, antwort: String) = netz.ohneAntwort(
        "/api/gemeinschaften/${stelle(kennung)}/termine/$nr",
        "POST",
        buildJsonObject { put("antwort", antwort) }.toString(),
    )

    suspend fun terminAbsagen(kennung: String, nr: Long) = netz.ohneAntwort(
        "/api/gemeinschaften/${stelle(kennung)}/termine/$nr",
        "DELETE",
    )

    // --------------------------------------------------------- Einstellungen

    /** Alle Stellschrauben auf einmal — `null` wird ausdrücklich mitgeschickt. */
    suspend fun einstellungenSpeichern(
        kennung: String,
        id: String,
        e: Gemeinschaftseinstellungen,
    ): GemeinschaftDetail = netz.hole(
        "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}",
        "PUT",
        buildJsonObject {
            put("name", e.name)
            put("beschreibung", e.beschreibung?.let { JsonPrimitive(it) } ?: JsonNull)
            put("landkreisId", e.landkreisId?.let { JsonPrimitive(it) } ?: JsonNull)
            put("landkreis", e.landkreis?.let { JsonPrimitive(it) } ?: JsonNull)
            put("wappenfarbe", e.wappenfarbe)
            put("oeffentlich", e.oeffentlich)
            put("inRangliste", e.inRangliste)
            put("beitrittModus", e.beitrittModus)
            put("mindestLevel", e.mindestLevel)
            put("mindestErfahrung", e.mindestErfahrung)
            put("maxMitglieder", e.maxMitglieder)
        }.toString(),
    )

    /** Würfelt den Beitrittscode neu — der alte führt danach ins Leere. */
    suspend fun beitrittscodeErneuern(kennung: String, id: String): String? {
        val antwort = netz.hole<JsonObject?>(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/beitrittscode",
            "POST",
        )
        return (antwort?.get("code") as? JsonPrimitive)?.content
    }

    suspend fun aufloesen(kennung: String, id: String) = netz.ohneAntwort(
        "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}",
        "DELETE",
    )

    /** Setzt den Wachentag — ein leerer Text nimmt ihn ab. */
    suspend fun tagSetzen(kennung: String, id: String, tag: String): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/tag",
            "PUT",
            buildJsonObject { put("tag", tag) }.toString(),
        )

    // ------------------------------------------------------ Rangliste, Stufen

    /** Die Rangliste der Wachen. Ohne Kennung fehlt nur die Markierung der eigenen. */
    suspend fun rangliste(kennung: String?, anzahl: Int = 25): List<Wachenplatz> {
        val eigen = kennung?.let { "&kennung=${stelle(it)}" }.orEmpty()
        return netz.hole("/api/gemeinschaften/rangliste?anzahl=$anzahl$eigen")
    }

    /** Die Laufbahn der Wachengemeinschaften — ohne Kennung, sie ist für alle gleich. */
    suspend fun laufbahn(): List<Wachenrang> = netz.hole("/api/wachenlaufbahn")

    // ------------------------------------------------------------ Wachen-Shop

    /** Kauft einen Ausbau oder ein Zierstück — Leitung und Zugführer. */
    suspend fun ausbauKaufen(kennung: String, id: String, artikelId: String): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/shop/kauf",
            "POST",
            buildJsonObject { put("artikelId", artikelId) }.toString(),
        )

    /** Setzt oder nimmt den eigenen Wunsch nach einem Artikel. */
    suspend fun wunschSetzen(kennung: String, id: String, artikelId: String, an: Boolean): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/shop/wunsch",
            "POST",
            buildJsonObject {
                put("artikelId", artikelId)
                put("an", an)
            }.toString(),
        )

    /** Zahlt Credits in die Wachenkasse ein — Umtausch 10:1, nur ganze Coins. */
    suspend fun einzahlen(kennung: String, id: String, credits: Int): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/shop/einzahlen",
            "POST",
            buildJsonObject { put("credits", credits) }.toString(),
        )

    /**
     * Legt ein Zierstück an oder ab (`null` legt ab). `art` ist der Platz:
     * `Kopfmuster`, `Emblemrahmen`, `Emblemzeichen` oder `Beiname`.
     */
    suspend fun schmuckSetzen(kennung: String, id: String, art: String, stueckId: String?): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/shop/aussehen",
            "POST",
            buildJsonObject {
                put("art", art)
                put("stueckId", stueckId?.let { JsonPrimitive(it) } ?: JsonNull)
            }.toString(),
        )

    /** Setzt den Farbton der Wache — 0 ist „automatisch aus der Kennung". */
    suspend fun farbeSetzen(kennung: String, id: String, farbe: Int): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${stelle(kennung)}/${stelle(id)}/shop/farbe",
            "POST",
            buildJsonObject { put("farbe", farbe) }.toString(),
        )
}

/** Wie in `Spielwege`: einmal kodieren statt an dreißig Stellen. */
private fun stelle(wert: String): String = URLEncoder.encode(wert, "UTF-8")
