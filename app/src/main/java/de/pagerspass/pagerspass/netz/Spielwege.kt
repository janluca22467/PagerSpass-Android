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
     * `ganzerBereich` heißt: der ganze Kreis statt eines einzelnen Ortes. Das ist
     * die Vorgabe, wenn jemand nur einen Kreis wählt und sonst nichts.
     */
    suspend fun raumAnlegen(landkreisId: String?, ganzerBereich: Boolean = true, leitstelleId: String? = null): Raum =
        netz.hole(
            "/api/rooms",
            "POST",
            buildJsonObject {
                if (landkreisId != null) put("landkreisId", landkreisId)
                // Nur, wo es eine Wahl gab (Österreich, Schweiz); sonst nimmt der Server die erste.
                if (leitstelleId != null) put("leitstelleId", leitstelleId)
                put("ganzerBereich", ganzerBereich)
            }.toString(),
        )

    /** Was über einen Raum bekannt ist, bevor man beitritt. */
    suspend fun rauminfo(code: String): Rauminfo =
        netz.hole("/api/rooms/${teil(code.uppercase())}")

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

    suspend fun bestenliste(anzahl: Int = 25, kennung: String? = null): List<Bestenlistenplatz> {
        val eigen = kennung?.let { "&kennung=${teil(it)}" }.orEmpty()
        return netz.hole("/api/bestenliste?anzahl=$anzahl$eigen")
    }

    // ------------------------------------------------------------------ Garage

    /**
     * Die Garage eines Staats (v6): Deutschland, Österreich und die Schweiz führen
     * je eigene Gutscheine (`garageLaden(kennung, staat)` im Web). Ein Server ohne
     * Staaten überliest `?staat=` und antwortet wie bisher.
     */
    suspend fun garage(kennung: String, staat: String = Staaten.DEUTSCHLAND): Garage =
        netz.hole("/api/konto/${teil(kennung)}/garage?staat=${teil(staat)}")

    /**
     * Ein Fahrzeug aussuchen — der Gutschein aus einem Aufstieg.
     *
     * <b>Nicht dasselbe wie kaufen.</b> Eine Wahl kostet nichts und ist an
     * `offeneWahlen` gebunden; ein Kauf kostet Credits und geht immer. Zwei Wege
     * mit zwei Bedeutungen, deshalb zwei Aufrufe.
     */
    suspend fun fahrzeugWaehlen(
        kennung: String,
        vorlage: String,
        staat: String = Staaten.DEUTSCHLAND,
    ): Garage = netz.hole(
        "/api/konto/${teil(kennung)}/garage?staat=${teil(staat)}",
        "POST",
        buildJsonObject { put("templateId", vorlage) }.toString(),
    )

    suspend fun fahrzeugKaufen(
        kennung: String,
        vorlage: String,
        staat: String = Staaten.DEUTSCHLAND,
    ): Garage = netz.hole(
        "/api/konto/${teil(kennung)}/garage/kauf?staat=${teil(staat)}",
        "POST",
        buildJsonObject { put("templateId", vorlage) }.toString(),
    )

    // -------------------------------------------------------------------- Shop

    suspend fun shop(kennung: String): Shop = netz.hole("/api/konto/${teil(kennung)}/shop")

    /**
     * Kauft einen Artikel — Meldergesicht, Rahmen, Muster, Farbe, Titel, Ton.
     *
     * <b>Die Antwort ist der ganze Shop</b>, nicht nur eine Bestätigung: Nach
     * einem Kauf haben sich der Credit-Stand, der Besitzstand und die Kaufbarkeit
     * jedes anderen Artikels geändert. Wer nur „gekauft: ja" zurückgäbe, müsste
     * gleich darauf alles neu holen.
     */
    suspend fun artikelKaufen(kennung: String, artikelId: String): Shop = netz.hole(
        "/api/konto/${teil(kennung)}/shop/kauf",
        "POST",
        buildJsonObject { put("artikelId", artikelId) }.toString(),
    )

    /** Holt den Tagesbonus ab — Gewinn und frischer Shop-Stand in einem. */
    suspend fun tagesbonus(kennung: String): Gluecksradergebnis =
        netz.hole("/api/konto/${teil(kennung)}/shop/tagesbonus", "POST")

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

    /** Wer einem vorgeschlagen wird — aus gemeinsamen Schichten. */
    suspend fun freundesvorschlaege(kennung: String): List<Freund> =
        netz.hole("/api/freunde/${teil(kennung)}/vorschlaege")

    suspend fun freundAntworten(kennung: String, wen: String, annehmen: Boolean) =
        netz.ohneAntwort(
            "/api/freunde/${teil(kennung)}/antwort",
            "POST",
            buildJsonObject {
                put("kennung", wen)
                put("annehmen", annehmen)
            }.toString(),
        )

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

    suspend fun profilSpeichern(kennung: String, aenderung: Profilaenderung): Profil =
        netz.hole(
            "/api/konto/${teil(kennung)}/profil",
            "PUT",
            Netz.abgabe.encodeToString(Profilaenderung.serializer(), aenderung),
        )

    // ------------------------------------------------------------ Mitteilungen

    /** Was der Betrieb gerade zu sagen hat. Ohne Konto und ohne Anmeldung. */
    suspend fun mitteilungen(): List<Betreibermitteilung> = netz.hole("/api/mitteilungen")

    // ----------------------------------------------------------- Privatsphäre

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

    /**
     * Einen Beitrag anschlagen — mit `roomCode` hängt eine eigene Schicht daran
     * (`VerfassenFeld.vue`). Ein Server, der das Feld nicht kennt, überliest es.
     */
    suspend fun brettSchreiben(
        kennung: String,
        text: String,
        sichtbarkeit: String?,
        roomCode: String? = null,
    ): Bretteintrag =
        netz.hole(
            "/api/brett/${teil(kennung)}",
            "POST",
            Netz.abgabe.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                kotlinx.serialization.json.buildJsonObject {
                    put("text", kotlinx.serialization.json.JsonPrimitive(text))
                    sichtbarkeit?.let {
                        put("sichtbarkeit", kotlinx.serialization.json.JsonPrimitive(it))
                    }
                    roomCode?.takeIf { it.isNotBlank() }?.let {
                        put("roomCode", kotlinx.serialization.json.JsonPrimitive(it))
                    }
                },
            ),
        )

    suspend fun brettLoeschen(kennung: String, nr: Long) =
        netz.ohneAntwort("/api/brett/${teil(kennung)}/eintrag/$nr", "DELETE")

    /** Die Quittung umlegen — an oder aus, der Server sagt, was daraus wurde. */
    suspend fun brettQuittieren(kennung: String, nr: Long): kotlinx.serialization.json.JsonObject =
        netz.hole("/api/brett/${teil(kennung)}/eintrag/$nr/quittung", "POST")

    suspend fun brettKommentare(kennung: String, nr: Long): List<Brettkommentar> =
        netz.hole("/api/brett/${teil(kennung)}/eintrag/$nr/kommentare")

    suspend fun brettKommentieren(kennung: String, nr: Long, text: String): Brettkommentar =
        netz.hole(
            "/api/brett/${teil(kennung)}/eintrag/$nr/kommentare",
            "POST",
            """{"text":${Netz.abgabe.encodeToString(kotlinx.serialization.serializer<String>(), text)}}""",
        )

    // ---------------------------------------------------------- Gemeinschaft

    suspend fun gemeinschaftDetail(kennung: String, id: String): GemeinschaftDetail =
        netz.hole("/api/gemeinschaften/${teil(kennung)}/detail/${teil(id)}")

    suspend fun gemeinschaftGruenden(
        kennung: String,
        name: String,
        beschreibung: String?,
    ): Gemeinschaft = netz.hole(
        "/api/gemeinschaften/${teil(kennung)}",
        "POST",
        Netz.abgabe.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject {
                put("name", kotlinx.serialization.json.JsonPrimitive(name))
                beschreibung?.let {
                    put("beschreibung", kotlinx.serialization.json.JsonPrimitive(it))
                }
            },
        ),
    )

    suspend fun gemeinschaftChat(kennung: String, id: String, anzahl: Int = 100): List<Gemeinschaftsnachricht> =
        netz.hole("/api/gemeinschaften/${teil(kennung)}/${teil(id)}/nachrichten?anzahl=$anzahl")

    suspend fun gemeinschaftBeitrittMitCode(kennung: String, code: String): Gemeinschaft =
        netz.hole(
            "/api/gemeinschaften/${teil(kennung)}/beitrittscode",
            "POST",
            """{"code":"${code.trim().uppercase()}"}""",
        )

    /** Bewerben — bei Modus `Offen` tritt man sofort bei. */
    suspend fun gemeinschaftBewerben(
        kennung: String,
        id: String,
        nachricht: String?,
    ): kotlinx.serialization.json.JsonObject = netz.hole(
        "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/bewerben",
        "POST",
        Netz.abgabe.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject {
                put(
                    "nachricht",
                    nachricht?.let { kotlinx.serialization.json.JsonPrimitive(it) }
                        ?: kotlinx.serialization.json.JsonNull,
                )
            },
        ),
    )

    suspend fun gemeinschaftsantraege(kennung: String): List<Gemeinschaftsantrag> =
        netz.hole("/api/gemeinschaften/${teil(kennung)}/antraege")

    /** Entscheiden oder — für die eigene Bewerbung — zurückziehen. */
    suspend fun gemeinschaftsantragEntscheiden(kennung: String, nr: Long, annehmen: Boolean) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/antraege/$nr",
            "POST",
            """{"annehmen":$annehmen}""",
        )

    suspend fun gemeinschaftEinladen(kennung: String, id: String, wen: String) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/einladen",
            "POST",
            """{"kennung":"$wen"}""",
        )

    suspend fun gemeinschaftRolle(kennung: String, id: String, wen: String, rolle: String) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/rolle",
            "POST",
            """{"kennung":"$wen","rolle":"$rolle"}""",
        )

    suspend fun gemeinschaftLeitung(kennung: String, id: String, an: String) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/leitung",
            "POST",
            """{"kennung":"$an"}""",
        )

    /** Fremdes Mitglied entfernen — oder mit der eigenen Kennung: austreten. */
    suspend fun gemeinschaftEntfernen(kennung: String, id: String, wen: String) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/mitglieder/${teil(wen)}",
            "DELETE",
        )

    suspend fun gemeinschaftAufloesen(kennung: String, id: String) =
        netz.ohneAntwort("/api/gemeinschaften/${teil(kennung)}/${teil(id)}", "DELETE")

    suspend fun gemeinschaftPinnwand(kennung: String, id: String, text: String?) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/pinnwand",
            "PUT",
            Netz.abgabe.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                kotlinx.serialization.json.buildJsonObject {
                    put(
                        "text",
                        text?.let { kotlinx.serialization.json.JsonPrimitive(it) }
                            ?: kotlinx.serialization.json.JsonNull,
                    )
                },
            ),
        )

    suspend fun terminAnlegen(kennung: String, id: String, titel: String, wann: String): Wachentermin =
        netz.hole(
            "/api/gemeinschaften/${teil(kennung)}/${teil(id)}/termine",
            "POST",
            Netz.abgabe.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                kotlinx.serialization.json.buildJsonObject {
                    put("titel", kotlinx.serialization.json.JsonPrimitive(titel))
                    put("wann", kotlinx.serialization.json.JsonPrimitive(wann))
                },
            ),
        )

    /** `Zugesagt`, `Abgesagt` oder `Offen`. */
    suspend fun terminAntworten(kennung: String, nr: Long, antwort: String) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/termine/$nr",
            "POST",
            """{"antwort":"$antwort"}""",
        )

    suspend fun terminAbsagen(kennung: String, nr: Long) =
        netz.ohneAntwort("/api/gemeinschaften/${teil(kennung)}/termine/$nr", "DELETE")

    suspend fun chatzeileMelden(kennung: String, nr: Long, grund: String?) =
        netz.ohneAntwort(
            "/api/gemeinschaften/${teil(kennung)}/nachrichten/$nr/melden",
            "POST",
            Netz.abgabe.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                kotlinx.serialization.json.buildJsonObject {
                    put(
                        "grund",
                        grund?.let { kotlinx.serialization.json.JsonPrimitive(it) }
                            ?: kotlinx.serialization.json.JsonNull,
                    )
                },
            ),
        )

    suspend fun chatzeileEntfernen(kennung: String, nr: Long) =
        netz.ohneAntwort("/api/gemeinschaften/${teil(kennung)}/nachrichten/$nr", "DELETE")

    suspend fun meldungErledigt(kennung: String, nr: Long) =
        netz.ohneAntwort("/api/gemeinschaften/${teil(kennung)}/meldungen/$nr/erledigt", "POST")

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

    suspend fun mitteilungseinstellungenSpeichern(
        kennung: String,
        einstellungen: Mitteilungseinstellungen,
    ): Mitteilungseinstellungen = netz.hole(
        "/api/konto/${teil(kennung)}/mitteilungen",
        "PUT",
        Netz.abgabe.encodeToString(Mitteilungseinstellungen.serializer(), einstellungen),
    )

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

    suspend fun privatsphaere(kennung: String): Privatsphaere =
        netz.hole("/api/konto/${teil(kennung)}/privatsphaere")

    suspend fun privatsphaereSpeichern(kennung: String, einstellungen: Privatsphaere): Privatsphaere =
        netz.hole(
            "/api/konto/${teil(kennung)}/privatsphaere",
            "PUT",
            Netz.abgabe.encodeToString(Privatsphaere.serializer(), einstellungen),
        )
}

/** Wie in `Konten`: einmal kodieren statt an sechzig Stellen. */
private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
