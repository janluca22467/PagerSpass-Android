package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege des Freundebereichs und der Wache, die `Spielwege` noch nicht kennt —
 * übertragen aus `web/src/api/rest.ts`.
 *
 * <b>Getrennt von `Spielwege`</b>, weil dort schon sechzig Wege stehen und
 * gleichzeitig daran gearbeitet wird. Die Ordnung ist dieselbe: Die Kennung des
 * Handelnden steht im Pfad, das Merkmal im Kopf, und entschieden wird am Server —
 * jede Antwort ist das Ergebnis oder ein `fehler`, den `Netz` zur Meldung macht.
 */
class Freundewege(private val netz: Netz) {

    // ---------------------------------------------------------------- Freunde

    /** Sucht ein Konto über seinen eindeutigen Benutzernamen — nicht den Anzeigenamen. */
    suspend fun suchen(kennung: String, benutzername: String): Suchtreffer =
        netz.hole("/api/freunde/${teil(kennung)}/suche?benutzername=${teil(benutzername)}")

    /** Stellt eine Anfrage. Hatte der andere schon gefragt, sind beide sofort verbunden. */
    suspend fun anfragen(kennung: String, wen: String) = netz.ohneAntwort(
        "/api/freunde/${teil(kennung)}/anfrage",
        "POST",
        buildJsonObject { put("kennung", wen) }.toString(),
    )

    suspend fun antworten(kennung: String, wen: String, annehmen: Boolean) = netz.ohneAntwort(
        "/api/freunde/${teil(kennung)}/antwort",
        "POST",
        buildJsonObject {
            put("kennung", wen)
            put("annehmen", annehmen)
        }.toString(),
    )

    /** Blockieren beendet Freundschaft, Einladungen und Nachrichten in beide Richtungen. */
    suspend fun blockieren(kennung: String, wen: String) = netz.ohneAntwort(
        "/api/freunde/${teil(kennung)}/blockieren",
        "POST",
        buildJsonObject { put("kennung", wen) }.toString(),
    )

    /** Meldet ein Konto — unabhängig davon, ob man befreundet oder blockiert ist. */
    suspend fun melden(kennung: String, wen: String, grund: String) = netz.ohneAntwort(
        "/api/freunde/${teil(kennung)}/melden",
        "POST",
        buildJsonObject {
            put("kennung", wen)
            put("grund", grund)
        }.toString(),
    )

    /**
     * Freundschaft beenden, eigene Anfrage zurückziehen oder eigene Blockade
     * aufheben — alle drei räumen dieselbe Zeile weg.
     */
    suspend fun loesen(kennung: String, wen: String) =
        netz.ohneAntwort("/api/freunde/${teil(kennung)}/${teil(wen)}", "DELETE")

    /** Leute, mit denen dieses Konto schon gefahren ist, aber noch nicht befreundet ist. */
    suspend fun vorschlaege(kennung: String): List<Vorschlag> =
        netz.hole("/api/freunde/${teil(kennung)}/vorschlaege")

    suspend fun einladungen(kennung: String): List<Einladung> =
        netz.hole("/api/freunde/${teil(kennung)}/einladungen")

    /** Öffnet ein Geschenk — erst hier erfährt man, was drin ist. */
    suspend fun geschenkOeffnen(kennung: String, nr: Long): Geschenkinhalt =
        netz.hole("/api/freunde/${teil(kennung)}/geschenke/$nr/oeffnen", "POST")

    // ----------------------------------------------------------------- Profil

    /** Ein Profil über den eindeutigen Benutzernamen — das eigene wie ein fremdes. */
    suspend fun profil(kennung: String, benutzername: String): Fremdprofil =
        netz.hole("/api/profil/${teil(kennung)}/${teil(benutzername)}")

    /** Die Einträge eines einzelnen Kontos — die Zeitleiste seines Profils. */
    suspend fun brettVon(kennung: String, wen: String, vorNr: Long? = null): BrettSeite =
        netz.hole(
            "/api/brett/${teil(kennung)}/von/${teil(wen)}" + (vorNr?.let { "?vorNr=$it" } ?: ""),
        )

    // ------------------------------------------------------------------ Brett

    suspend fun eintragEntfernen(kennung: String, nr: Long) =
        netz.ohneAntwort("/api/brett/${teil(kennung)}/eintrag/$nr", "DELETE")

    suspend fun eintragMelden(kennung: String, nr: Long, grund: String?) = netz.ohneAntwort(
        "/api/brett/${teil(kennung)}/eintrag/$nr/melden",
        "POST",
        grundrumpf(grund),
    )

    suspend fun kommentarMelden(kennung: String, nr: Long, grund: String?) = netz.ohneAntwort(
        "/api/brett/${teil(kennung)}/kommentar/$nr/melden",
        "POST",
        grundrumpf(grund),
    )

    // ------------------------------------------------------------------ Wache

    /**
     * Die öffentlich gelisteten Wachen, eingegrenzt.
     *
     * `nurPassende` holt sich die Erfahrung am Server aus der Anmeldung — sie
     * steht bewusst nicht in der Adresse.
     */
    suspend fun oeffentliche(
        suche: String? = null,
        landkreisId: String? = null,
        modus: String? = null,
        nurFreie: Boolean = false,
        nurPassende: Boolean = false,
        anzahl: Int = 30,
    ): List<Gemeinschaft> {
        val teile = buildList {
            add("anzahl=$anzahl")
            suche?.takeIf { it.isNotBlank() }?.let { add("suche=${teil(it)}") }
            landkreisId?.let { add("landkreisId=${teil(it)}") }
            modus?.let { add("modus=${teil(it)}") }
            if (nurFreie) add("nurFreie=true")
            if (nurPassende) add("nurPassende=true")
        }
        return netz.hole("/api/gemeinschaften/oeffentlich?${teile.joinToString("&")}")
    }

    suspend fun landkreisvorschlag(kennung: String): Landkreisvorschlag =
        netz.hole("/api/gemeinschaften/vorschlag-landkreis/${teil(kennung)}")

    /** Gründen — mit dem Landkreis, in dem später die Clanrunden laufen. */
    suspend fun gruenden(
        kennung: String,
        name: String,
        beschreibung: String?,
        landkreisId: String?,
        landkreis: String?,
    ): Gemeinschaft = netz.hole(
        "/api/gemeinschaften/${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("name", name)
            beschreibung?.let { put("beschreibung", it) }
            landkreisId?.let { put("landkreisId", it) }
            landkreis?.let { put("landkreis", it) }
        }.toString(),
    )

    suspend fun einstellungenSpeichern(
        kennung: String,
        id: String,
        einstellungen: Wacheneinstellungen,
    ): GemeinschaftDetail = netz.hole(
        "/api/gemeinschaften/${teil(kennung)}/${teil(id)}",
        "PUT",
        Netz.abgabe.encodeToString(Wacheneinstellungen.serializer(), einstellungen),
    )

    /** Würfelt den Beitrittscode neu — der alte führt danach ins Leere. */
    suspend fun codeErneuern(kennung: String, id: String): JsonObject =
        netz.hole("/api/gemeinschaften/${teil(kennung)}/${teil(id)}/beitrittscode", "POST")

    suspend fun aufloesen(kennung: String, id: String) =
        netz.ohneAntwort("/api/gemeinschaften/${teil(kennung)}/${teil(id)}", "DELETE")

    /** Setzt den Wachentag — ein leerer Text nimmt ihn ab. */
    suspend fun tagSetzen(kennung: String, id: String, tag: String): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/tag",
            "PUT",
            buildJsonObject { put("tag", tag) }.toString(),
        )

    /** Nimmt den Hinweis auf die laufende Clanrunde ab — die Runde läuft weiter. */
    suspend fun clanrundeSchliessen(kennung: String, id: String) =
        netz.ohneAntwort("/api/gemeinschaften/${teil(kennung)}/${teil(id)}/clanrunde", "DELETE")

    /** Die Hilfsfrist-Kurven der Mitglieder — wer seine Chronik nicht teilt, fehlt. */
    suspend fun hilfsfristen(kennung: String, id: String): List<GemeinschaftsHilfsfrist> =
        netz.hole("/api/gemeinschaften/${teil(kennung)}/${teil(id)}/hilfsfristen")

    /** Die Rangliste der Wachen. Ohne Kennung fehlt nur die Markierung der eigenen. */
    suspend fun rangliste(kennung: String?, anzahl: Int = 25): List<Wachenplatz> =
        netz.hole(
            "/api/gemeinschaften/rangliste?anzahl=$anzahl" +
                (kennung?.let { "&kennung=${teil(it)}" } ?: ""),
        )

    /** Die Laufbahn der Wachen — für das Stufen-Band. Ohne Kennung, für alle gleich. */
    suspend fun wachenlaufbahn(): List<Wachenrang> = netz.hole("/api/wachenlaufbahn")

    // ------------------------------------------------------------- Wachen-Shop
    //
    // Jede Antwort ist die frische Detailansicht der Wache: Nach einem Kauf haben
    // sich Kasse, Auslage und womöglich der Kopf geändert.

    suspend fun ausbauKaufen(kennung: String, id: String, artikelId: String): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/shop/kauf",
            "POST",
            buildJsonObject { put("artikelId", artikelId) }.toString(),
        )

    suspend fun wunschSetzen(
        kennung: String,
        id: String,
        artikelId: String,
        an: Boolean,
    ): GemeinschaftDetail = netz.hole(
        "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/shop/wunsch",
        "POST",
        buildJsonObject {
            put("artikelId", artikelId)
            put("an", an)
        }.toString(),
    )

    /** Zahlt Credits in die Wachenkasse ein — zehn Credits werden ein Coin. */
    suspend fun einzahlen(kennung: String, id: String, credits: Int): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/shop/einzahlen",
            "POST",
            buildJsonObject { put("credits", credits) }.toString(),
        )

    /** Legt ein Zierstück an oder ab (`null` legt ab) — Muster, Rahmen, Zeichen, Beiname. */
    suspend fun schmuckSetzen(
        kennung: String,
        id: String,
        art: String,
        stueckId: String?,
    ): GemeinschaftDetail = netz.hole(
        "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/shop/aussehen",
        "POST",
        buildJsonObject {
            put("art", art)
            put("stueckId", stueckId?.let { JsonPrimitive(it) } ?: JsonNull)
        }.toString(),
    )

    /** Setzt den Farbton der Wache — 0 ist „automatisch aus der Kennung". */
    suspend fun farbeSetzen(kennung: String, id: String, farbe: Int): GemeinschaftDetail =
        netz.hole(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/shop/farbe",
            "POST",
            buildJsonObject { put("farbe", farbe) }.toString(),
        )

    /** `{ "grund": … }` — ohne Grund steht `null` darin, nicht nichts. */
    private fun grundrumpf(grund: String?): String =
        buildJsonObject { put("grund", grund?.let { JsonPrimitive(it) } ?: JsonNull) }.toString()
}

/** Wie in `Spielwege`: einmal kodieren statt an vierzig Stellen. */
private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
