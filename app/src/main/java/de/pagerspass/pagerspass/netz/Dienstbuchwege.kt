package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege des Dienstbuchs und des Shops — übertragen aus `web/src/api/rest.ts`.
 *
 * <b>Eine eigene Klasse neben `Spielwege`</b>, weil das Dienstbuch mit sechs
 * Seiten und der Shop mit fünf Bereichen zusammen gut zwanzig Wege brauchen. In
 * `Spielwege` stünden sie zwischen Brett und Wache und machten aus einer Datei mit
 * Ordnung eine mit vierzig Wegen und keiner.
 *
 * Dieselbe Trennung wie dort: <b>Die Kennung steht im Pfad oder in der Abfrage,
 * das Merkmal im Kopf.</b>
 */
class Dienstbuchwege(private val netz: Netz) {

    // ------------------------------------------------------------------ Archiv

    /** Die eigenen beendeten Runden in Kurzform — jüngste zuerst. */
    suspend fun archiv(kennung: String, anzahl: Int = 50): List<ArchivEintrag> =
        netz.hole("/api/archiv?kennung=${teil(kennung)}&anzahl=$anzahl")

    /**
     * Eine abgeschlossene Schicht vollständig — nur für die Mannschaft, die sie
     * gefahren hat. Wer nicht dabei war, bekommt dieselbe Antwort wie für einen
     * unbekannten Code.
     */
    suspend fun archivRunde(code: String, kennung: String): ArchivRunde =
        netz.hole("/api/archiv/${teil(code)}?kennung=${teil(kennung)}")

    /** Bilanzen je Mitspieler und der Verlauf über die letzten Schichten. */
    suspend fun statistik(kennung: String, runden: Int = 10): ArchivStatistik =
        netz.hole("/api/archiv/statistik?kennung=${teil(kennung)}&runden=$runden")

    /**
     * Die Auswertung über alle Schichten — eine Abo-Leistung.
     *
     * <b>Ohne Abo antwortet der Server mit 403</b>; der Aufrufer macht daraus den
     * Weg in den Laden und keine Fehlermeldung (`Netzfehler.stand`).
     */
    suspend fun auswertung(kennung: String): Dienstauswertung =
        netz.hole("/api/archiv/auswertung?kennung=${teil(kennung)}")

    // ----------------------------------------------------------------- Chronik

    /** Die gebuchten Schichten, jüngste zuerst — mit Rolle, Fahrzeug und Punkten. */
    suspend fun chronik(kennung: String, anzahl: Int = 100): List<Dienstschicht> =
        netz.hole("/api/konto/${teil(kennung)}/chronik?anzahl=$anzahl")

    /** Wofür es in einer Schicht Punkte gab — beim ersten Aufklappen geholt. */
    suspend fun buchungen(kennung: String, code: String): List<Buchungsposten> =
        netz.hole("/api/konto/${teil(kennung)}/chronik/${teil(code)}")

    suspend fun abzeichen(kennung: String): List<Abzeichen> =
        netz.hole("/api/konto/${teil(kennung)}/abzeichen")

    // ---------------------------------------------------------------- Laufbahn

    /** Alle Stufen der Laufbahn — für jeden gleich, ohne Anmeldung. */
    suspend fun laufbahn(): List<Laufbahnstufe> = netz.hole("/api/laufbahn")

    suspend fun bestenliste(kennung: String?, anzahl: Int = 25): List<Bestenlistenplatz> =
        netz.hole("/api/bestenliste?anzahl=$anzahl" + (kennung?.let { "&kennung=${teil(it)}" } ?: ""))

    /** Die Zwischenwertung des laufenden Monats. */
    suspend fun saisonbestenliste(kennung: String?, anzahl: Int = 25): List<Saisonbestenlistenplatz> =
        netz.hole(
            "/api/bestenliste/saison?anzahl=$anzahl" + (kennung?.let { "&kennung=${teil(it)}" } ?: ""),
        )

    suspend fun tagesschicht(kennung: String?): Tagesschicht =
        netz.hole("/api/tagesschicht" + (kennung?.let { "?kennung=${teil(it)}" } ?: ""))

    /**
     * Das Konto, roh — für zwei Leser zugleich.
     *
     * Die Sitzung liest daraus ihr `Konto`, das Dienstbuch die Freischaltungen, die
     * das Konto-Modell nicht trägt. Eine Anfrage, zweimal eingelesen, statt zwei
     * Anfragen für dieselbe Antwort.
     */
    suspend fun kontoRoh(kennung: String): String =
        netz.roh("/api/konto/${teil(kennung)}") ?: throw Netzfehler("Das Konto kam leer zurück.")

    // ------------------------------------------------------------------- Wache

    /** Die eigene Wache — oder `null`, wenn man in keiner ist. */
    suspend fun gemeinschaft(kennung: String): Gemeinschaft? =
        netz.hole("/api/gemeinschaften/${teil(kennung)}")

    /** Die Hilfsfristkurven der anderen Mitglieder — Vergleich in der Übersicht. */
    suspend fun wachenkurven(kennung: String, gemeinschaftId: String): List<Wachenkurve> =
        netz.hole("/api/gemeinschaften/${teil(kennung)}/${teil(gemeinschaftId)}/hilfsfristen")

    // ----------------------------------------------------------------- Vitrine

    /** Die Vitrine des eigenen Profils — nur die Liste daraus. */
    suspend fun vitrine(kennung: String, benutzername: String): Vitrinenprofil =
        netz.hole("/api/profil/${teil(kennung)}/${teil(benutzername)}")

    /**
     * Die Vitrine setzen — höchstens sechs Ids.
     *
     * Derselbe Weg wie jede Profiländerung; nur dieses eine Feld geht mit, die
     * übrigen bleiben, wie sie sind.
     */
    suspend fun vitrineSpeichern(kennung: String, ids: List<String>): Vitrinenprofil = netz.hole(
        "/api/konto/${teil(kennung)}/profil",
        "PUT",
        buildJsonObject {
            put("vitrine", buildJsonArray { ids.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
        }.toString(),
    )

    // -------------------------------------------------------------------- Shop

    suspend fun shop(kennung: String): Shop = netz.hole("/api/konto/${teil(kennung)}/shop")

    suspend fun artikelKaufen(kennung: String, artikelId: String): Shop = netz.hole(
        "/api/konto/${teil(kennung)}/shop/kauf",
        "POST",
        buildJsonObject { put("artikelId", artikelId) }.toString(),
    )

    suspend fun tagesbonus(kennung: String): Gluecksradergebnis =
        netz.hole("/api/konto/${teil(kennung)}/shop/tagesbonus", "POST")

    /** Einen Aktionscode einlösen — Groß-/Kleinschreibung und Bindestriche egal. */
    suspend fun codeEinloesen(kennung: String, code: String): Codegutschrift = netz.hole(
        "/api/konto/${teil(kennung)}/code",
        "POST",
        buildJsonObject { put("code", code) }.toString(),
    )

    /** Wer beschenkt werden darf — und wer noch ein paar Tage warten muss. */
    suspend fun beschenkbare(kennung: String): List<Beschenkbarer> =
        netz.hole("/api/freunde/${teil(kennung)}/schenken")

    /** Ein Stück aus dem heutigen Sortiment verschenken; die Antwort ist der frische Shop. */
    suspend fun schenken(kennung: String, an: String, artikelId: String): Shop = netz.hole(
        "/api/freunde/${teil(kennung)}/schenken",
        "POST",
        buildJsonObject {
            put("an", an)
            put("artikelId", artikelId)
        }.toString(),
    )

    // ------------------------------------------------------------------ Garage

    suspend fun garage(kennung: String): Garage = netz.hole("/api/konto/${teil(kennung)}/garage")

    /** Einen Fahrzeuggutschein einlösen. */
    suspend fun fahrzeugWaehlen(kennung: String, vorlage: String): Garage = netz.hole(
        "/api/konto/${teil(kennung)}/garage",
        "POST",
        buildJsonObject { put("templateId", vorlage) }.toString(),
    )

    /** Ein Fahrzeug gegen Credits kaufen — der Gutschein bleibt dabei stehen. */
    suspend fun fahrzeugKaufen(kennung: String, vorlage: String): Garage = netz.hole(
        "/api/konto/${teil(kennung)}/garage/kauf",
        "POST",
        buildJsonObject { put("templateId", vorlage) }.toString(),
    )
}

private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
