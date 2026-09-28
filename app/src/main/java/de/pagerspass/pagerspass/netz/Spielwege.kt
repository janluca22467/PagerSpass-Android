package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege der sechs Seiten — übertragen aus `web/src/api/rest.ts`.
 *
 * <b>Getrennt von `Konten`</b>, weil das zwei Dinge sind: Dort steht, wer man
 * ist; hier, was man sieht. Wer beides in eine Datei legt, hat beim nächsten
 * Umbau eine Datei mit vierzig Wegen und keiner Ordnung darin.
 *
 * <b>Die Kennung steht im Pfad, das Merkmal im Kopf.</b> Das ist die Trennung
 * aus der Vorlage: Das Merkmal weist die Anfrage aus, die Kennung sagt nur, um
 * wessen Daten es geht.
 */
class Spielwege(private val netz: Netz) {

    // ---------------------------------------------------------------- Katalog

    /**
     * Der Katalog.
     *
     * <b>Ohne Kennung und ohne Anmeldung.</b> Er ist für jede Runde gleich; die
     * Anmeldeseite könnte ihn schon laden. Weil er groß ist, holt ihn die Sitzung
     * genau einmal und behält ihn.
     */
    suspend fun katalog(): Katalog = netz.hole("/api/catalog")

    // ------------------------------------------------------------------- Runde

    /**
     * Eröffnet eine Runde und gibt den Raumcode zurück.
     *
     * <b>`ganzerBereich` heißt: auch die Nachbarkreise dieser Leitstelle
     * mitspielen</b> — nicht „der ganze Kreis". Die Vorgabe ist deshalb `false`,
     * wie im Web; der Startbildschirm fragt nur dort, wo es mehr als einen Kreis
     * gibt. `leitstelleId` bleibt fast immer leer: Der Server nimmt dann die eine
     * Leitstelle des Kreises (Rumpf wie `raumAnlegen` in rest.ts).
     */
    suspend fun raumAnlegen(
        landkreisId: String?,
        ganzerBereich: Boolean = false,
        leitstelleId: String? = null,
    ): Raum =
        netz.hole(
            "/api/rooms",
            "POST",
            buildJsonObject {
                if (landkreisId != null) put("landkreisId", landkreisId)
                if (leitstelleId != null) put("leitstelleId", leitstelleId)
                put("ganzerBereich", ganzerBereich)
            }.toString(),
        )

    /**
     * Legt eine Ausbildungsschicht an — feste Zusammensetzung, Bots als Gegenüber.
     *
     * Sie ist der einzige Weg, den ein Konto mit offener Einweisungspflicht gehen
     * darf; jede andere Runde weist der Hub ohnehin ab.
     */
    suspend fun ausbildungAnlegen(): Raum = netz.hole("/api/rooms/ausbildung", "POST")

    // -------------------------------------------------------------- Dienstbuch

    /** Die gefahrenen Schichten, jüngste zuerst. */
    suspend fun chronik(kennung: String, anzahl: Int = 50): List<Schicht> =
        netz.hole("/api/konto/${teil(kennung)}/chronik?anzahl=$anzahl")

    suspend fun abzeichen(kennung: String): List<Abzeichen> =
        netz.hole("/api/konto/${teil(kennung)}/abzeichen")

    // ------------------------------------------------------------------ Garage

    suspend fun garage(kennung: String): Garage = netz.hole("/api/konto/${teil(kennung)}/garage")

    // -------------------------------------------------------------------- Shop

    suspend fun shop(kennung: String): Shop = netz.hole("/api/konto/${teil(kennung)}/shop")

    // ----------------------------------------------------------------- Freunde

    /**
     * Die Freundesliste.
     *
     * <b>Sie hängt nicht unter `/api/konto`</b>, sondern hat einen eigenen Ast —
     * `/api/freunde/{kennung}`. Dieselbe Liste trägt alle drei Reiter: Wer
     * angenommen ist, steht unter „Liste"; wer offen ist, unter „Anfragen", und
     * dort entscheidet `vonMir`, ob man wartet oder am Zug ist.
     */
    suspend fun freunde(kennung: String): List<Freund> = netz.hole("/api/freunde/${teil(kennung)}")

    // ------------------------------------------------------------ Gemeinschaft

    /**
     * Die eigene Wache — oder nichts.
     *
     * <b>„Nichts" ist hier der Normalfall und kein Fehler.</b> Der Server
     * antwortet für ein Konto ohne Gemeinschaft mit einem leeren Rumpf bei 200;
     * genau dafür gibt `Netz.roh` `null` zurück, statt am JSON zu scheitern.
     */
    suspend fun gemeinschaft(kennung: String): Gemeinschaft? =
        netz.hole("/api/gemeinschaften/${teil(kennung)}")

    suspend fun offeneGemeinschaften(anzahl: Int = 30): List<Gemeinschaft> =
        netz.hole("/api/gemeinschaften/oeffentlich?anzahl=$anzahl")

    // ------------------------------------------------------------------ Profil

    /**
     * Ein Profil über den eindeutigen Benutzernamen — das eigene wie ein fremdes.
     *
     * <b>Der Weg hängt nicht unter `/api/konto`</b>, sondern unter `/api/profil`,
     * und er nennt beide: wer fragt (`kennung`) und wonach (`wen`). Das ist kein
     * Zierrat — was ein Profil hergibt, hängt an der Privatsphäre des anderen und
     * daran, ob man befreundet ist.
     */
    suspend fun profil(kennung: String, wen: String): Profil =
        netz.hole("/api/profil/${teil(kennung)}/${teil(wen)}")

    /** Der Stand des eigenen Bildes: was sichtbar ist, was wartet, was abgelehnt wurde. */
    suspend fun profilbildstand(kennung: String): Profilbildstand =
        netz.hole("/api/konto/${teil(kennung)}/profilbild")

    /** Reicht ein Bild zur Prüfung ein. */
    suspend fun profilbildEinreichen(
        kennung: String,
        dateiname: String,
        inhaltstyp: String,
        daten: ByteArray,
    ): Profilbildstand = netz.hochladen(
        pfad = "/api/konto/${teil(kennung)}/profilbild",
        feld = "datei",
        dateiname = dateiname,
        inhaltstyp = inhaltstyp,
        daten = daten,
    )

    /** Nimmt das eigene Bild zurück — das wartende, das sichtbare oder beides. */
    suspend fun profilbildEntfernen(kennung: String): Profilbildstand =
        netz.hole("/api/konto/${teil(kennung)}/profilbild", "DELETE")

    // ------------------------------------------------------------ Mitteilungen

    /** Was der Betrieb gerade zu sagen hat. Ohne Konto und ohne Anmeldung. */
    suspend fun mitteilungen(): List<Betreibermitteilung> = netz.hole("/api/mitteilungen")

    // -------------------------------------------------------------- Soziales

    /**
     * Der Gesprächsverlauf mit einem Freund — älteste zuerst.
     *
     * Senden und Gelesen-Melden laufen <b>nicht</b> über REST, sondern über
     * den Sozial-Hub — nur so erfahren beide Seiten sofort davon.
     */
    suspend fun nachrichtenVerlauf(kennung: String, mit: String, anzahl: Int = 100): List<Nachricht> =
        netz.hole("/api/freunde/${teil(kennung)}/nachrichten/${teil(mit)}?anzahl=$anzahl")

    /** Eine Seite des Bretts — geblättert über `vorNr`, nicht über Offset. */
    suspend fun brett(kennung: String, reiter: String, vorNr: Long? = null): BrettSeite =
        netz.hole(
            "/api/brett/${teil(kennung)}?reiter=$reiter" +
                (vorNr?.let { "&vorNr=$it" } ?: ""),
        )

    suspend fun bretteintrag(kennung: String, nr: Long): Bretteintrag =
        netz.hole("/api/brett/${teil(kennung)}/eintrag/$nr")

    suspend fun brettKommentare(kennung: String, nr: Long): List<Brettkommentar> =
        netz.hole("/api/brett/${teil(kennung)}/eintrag/$nr/kommentare")

    suspend fun brettKommentieren(kennung: String, nr: Long, text: String): Brettkommentar =
        netz.hole(
            "/api/brett/${teil(kennung)}/eintrag/$nr/kommentare",
            "POST",
            """{"text":${Netz.abgabe.encodeToString(kotlinx.serialization.serializer<String>(), text)}}""",
        )

    // ---------------------------------------------------------- Gemeinschaft

    suspend fun gemeinschaftChat(kennung: String, id: String, anzahl: Int = 100): List<Gemeinschaftsnachricht> =
        netz.hole("/api/gemeinschaften/${teil(kennung)}/${teil(id)}/nachrichten?anzahl=$anzahl")

    suspend fun gemeinschaftEinladen(kennung: String, id: String, wen: String) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/einladen",
            "POST",
            """{"kennung":"$wen"}""",
        )

    suspend fun clanrundeStarten(kennung: String, id: String): kotlinx.serialization.json.JsonObject =
        netz.hole("/api/gemeinschaften/${teil(kennung)}/${teil(id)}/clanrunde", "POST")

    // ------------------------------------------------ Runden zum Beitreten

    /** Die Liste der Runden, die gerade Verstärkung suchen. */
    suspend fun oeffentlicheRunden(): List<OeffentlicheRunde> =
        netz.hole("/api/rooms/oeffentlich")

    /** Der Stand der Schicht des Tages — Kreis, Tagesliste, eigener Platz. */
    suspend fun tagesschicht(kennung: String?): Tagesschicht = netz.hole(
        "/api/tagesschicht" + (kennung?.let { "?kennung=${teil(it)}" } ?: ""),
    )

    /** Legt die Schicht des Tages an — festes Skript, Bots als Besatzung. */
    suspend fun tagesschichtAnlegen(): String {
        val antwort = netz.hole<kotlinx.serialization.json.JsonObject>(
            "/api/rooms/tagesschicht",
            "POST",
        )
        return (antwort["code"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            ?: throw Netzfehler("Die Schicht des Tages ließ sich nicht anlegen.")
    }

    // ------------------------------------------------------- Mitteilungen

    suspend fun mitteilungseinstellungen(kennung: String): Mitteilungseinstellungen =
        netz.hole("/api/konto/${teil(kennung)}/mitteilungen")

    suspend fun einladungen(kennung: String): List<Einladung> =
        netz.hole("/api/freunde/${teil(kennung)}/einladungen")

    /** Nur die noch nicht zur Kenntnis genommenen — der Server filtert. */
    suspend fun verwarnungen(kennung: String): List<Verwarnung> =
        netz.hole("/api/konto/${teil(kennung)}/verwarnungen")

    suspend fun verwarnungBestaetigen(kennung: String, nr: Int) =
        netz.ohneAntwort("/api/konto/${teil(kennung)}/verwarnungen/$nr/gelesen", "POST")

    suspend fun adminNachrichten(kennung: String): List<AdminNachricht> =
        netz.hole("/api/konto/${teil(kennung)}/admin-nachrichten")

    suspend fun adminNachrichtBestaetigen(kennung: String, nr: Int) =
        netz.ohneAntwort("/api/konto/${teil(kennung)}/admin-nachrichten/$nr/gelesen", "POST")

}

/** Wie in `Konten`: einmal kodieren statt an sechzig Stellen. */
private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
