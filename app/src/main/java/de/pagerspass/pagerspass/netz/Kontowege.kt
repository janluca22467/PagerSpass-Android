package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege des Kontobereichs — Premium, Postfach, Werbung, Datenschutz,
 * Übertragungen, Discord, Codes, Newsletter und Verträge. Übertragen aus den
 * gleichnamigen Abschnitten in `web/src/api/rest.ts`.
 *
 * <b>Eine eigene Klasse neben `Konten` und `Spielwege`</b>, weil diese Wege eine
 * Sache gemeinsam haben: Sie gehören den Konto-Einstellungen und den Seiten, die
 * man ohne Anmeldung erreicht (Kündigen, Widerrufen, Newsletter, Code). Wer hier
 * etwas sucht, sucht es nicht zwischen Garage und Wachenchat.
 *
 * <b>Ohne Anmeldung gehen:</b> `passwortVergessen`, `passwortNeuSetzen`,
 * `codevorschau`, `newsletterAbmelden`, `vertragKuendigen`, `vertragWiderrufen`.
 * Ist jemand angemeldet, geht sein Merkmal trotzdem mit — wie bei jeder Anfrage.
 */
class Kontowege(private val netz: Netz) {

    // ------------------------------------------------------------- Premium

    suspend fun premium(kennung: String): Abostand =
        netz.hole("/api/konto/${teil(kennung)}/premium")

    /**
     * Die Kasse öffnen. `sofortAusfuehren` ist das ausdrückliche Verlangen nach
     * sofortigem Beginn — ohne ihn weist der Server ab.
     */
    suspend fun premiumKasse(kennung: String, plan: String, sofortAusfuehren: Boolean): Weiterleitung =
        netz.hole(
            "/api/konto/${teil(kennung)}/premium/checkout",
            "POST",
            buildJsonObject {
                put("plan", plan)
                put("sofortAusfuehren", sofortAusfuehren)
            }.toString(),
        )

    /** Die Rückkehr von der Bezahlseite: Sitzung abschließen, Premium steht. */
    suspend fun premiumAbschliessen(kennung: String, sessionId: String): Abostand =
        netz.hole(
            "/api/konto/${teil(kennung)}/premium/checkout/abschliessen",
            "POST",
            buildJsonObject { put("sessionId", sessionId) }.toString(),
        )

    /** Die Abo-Verwaltung bei Stripe. */
    suspend fun premiumPortal(kennung: String): Weiterleitung =
        netz.hole("/api/konto/${teil(kennung)}/premium/portal", "POST")

    // ------------------------------------------------------------ Spielweise

    suspend fun patientensimulation(kennung: String): Schalterstand =
        netz.hole("/api/konto/${teil(kennung)}/patientensimulation")

    suspend fun patientensimulationSetzen(kennung: String, an: Boolean): Schalterstand =
        netz.hole(
            "/api/konto/${teil(kennung)}/patientensimulation",
            "PUT",
            buildJsonObject { put("an", an) }.toString(),
        )

    // ------------------------------------------------------------ Benutzername

    /** Ändert den eindeutigen Benutzernamen — den Namen, unter dem einen andere finden. */
    suspend fun benutzernameAendern(kennung: String, benutzername: String): Konto =
        netz.hole(
            "/api/konto/${teil(kennung)}/benutzername",
            "PUT",
            buildJsonObject { put("benutzername", benutzername) }.toString(),
        )

    /**
     * Speichert den Entwurf des Profileditors.
     *
     * <b>Mit `VOLL` statt `Netz.abgabe`:</b> Der Kartenausschnitt hat Vorgabewerte
     * (Deutschland, Stufe 6), und ohne `encodeDefaults` ginge genau dieser
     * Ausschnitt als `{}` hinaus. Was `null` ist, bleibt trotzdem weg
     * (`explicitNulls = false`) — der Server unterscheidet „nicht geändert" von
     * „geleert".
     */
    suspend fun profilSpeichern(kennung: String, aenderung: Profilaenderung): Profil =
        netz.hole(
            "/api/konto/${teil(kennung)}/profil",
            "PUT",
            VOLL.encodeToString(Profilaenderung.serializer(), aenderung),
        )

    // ---------------------------------------------------------------- Postfach
    //
    // Jeder Schritt, der die Adresse anfasst, schickt das Kontopasswort mit — eine
    // offen stehende Sitzung soll nicht genügen, um ein Konto über eine
    // untergeschobene Adresse zu übernehmen.

    suspend fun postfach(kennung: String): Postfach =
        netz.hole("/api/konto/${teil(kennung)}/postfach")

    /** Trägt eine Adresse ein oder ersetzt sie; der Bestätigungscode geht sofort raus. */
    suspend fun emailSetzen(kennung: String, email: String, passwort: String): Postfachantwort =
        netz.hole(
            "/api/konto/${teil(kennung)}/postfach/email",
            "PUT",
            buildJsonObject {
                put("email", email)
                put("passwort", passwort)
            }.toString(),
        )

    /** „Nichts angekommen" — ein neuer Code an dieselbe Adresse. */
    suspend fun emailCodeNeu(kennung: String) =
        netz.ohneAntwort("/api/konto/${teil(kennung)}/postfach/email/code", "POST")

    suspend fun emailBestaetigen(kennung: String, code: String): Postfach =
        netz.hole(
            "/api/konto/${teil(kennung)}/postfach/email/bestaetigen",
            "POST",
            buildJsonObject { put("code", code) }.toString(),
        )

    suspend fun newsletterSetzen(kennung: String, an: Boolean): Postfach =
        netz.hole(
            "/api/konto/${teil(kennung)}/postfach/newsletter",
            "PUT",
            buildJsonObject { put("an", an) }.toString(),
        )

    /** Das Passwort braucht nur das Ausschalten — einschalten lässt sich der Schutz nicht mehr. */
    suspend fun zweiFaktorSetzen(kennung: String, an: Boolean, passwort: String): Postfach =
        netz.hole(
            "/api/konto/${teil(kennung)}/postfach/zweifaktor",
            "PUT",
            buildJsonObject {
                put("an", an)
                put("passwort", passwort)
            }.toString(),
        )

    /**
     * „Passwort vergessen": Benutzername oder Adresse hinein, ein Satz zurück.
     *
     * Der Satz ist immer derselbe — auch wenn es das Konto nicht gibt. Jede
     * genauere Auskunft wäre ein Verzeichnis der vergebenen Namen.
     */
    suspend fun passwortVergessen(benutzer: String): Serversatz =
        netz.hole(
            "/api/konto/passwort-vergessen",
            "POST",
            buildJsonObject { put("benutzer", benutzer) }.toString(),
        )

    suspend fun passwortNeuSetzen(benutzer: String, code: String, passwort: String): Serversatz =
        netz.hole(
            "/api/konto/passwort-neu",
            "POST",
            buildJsonObject {
                put("benutzer", benutzer)
                put("code", code)
                put("passwort", passwort)
            }.toString(),
        )

    /** Der Abmeldelink aus einer Newsletter-Nachricht — ohne Anmeldung, ohne Rückfrage. */
    suspend fun newsletterAbmelden(schluessel: String): Newsletterabmeldung =
        netz.hole("/api/newsletter/abmelden?schluessel=${teil(schluessel)}", "POST")

    // ----------------------------------------------------------------- Werbung

    suspend fun werbung(kennung: String): Werbung =
        netz.hole("/api/konto/${teil(kennung)}/werbung")

    /** Erzeugt den eigenen Werbecode beim ersten Mal; danach gibt es den vorhandenen zurück. */
    suspend fun werbecodeErzeugen(kennung: String): Werbung =
        netz.hole("/api/konto/${teil(kennung)}/werbung/code", "POST")

    /** Löst einen fremden Werbecode ein — einmal je Konto, in den ersten Tagen. */
    suspend fun werbecodeEinloesen(kennung: String, code: String): Werbung =
        netz.hole(
            "/api/konto/${teil(kennung)}/werbung/einloesen",
            "POST",
            buildJsonObject { put("code", code) }.toString(),
        )

    // --------------------------------------------------------------------- Bug

    /**
     * Meldet einen Fehler im Spiel. Beschreibung sagt, was passiert ist; Hergang,
     * wie es dazu kam — beides ist Pflicht.
     */
    suspend fun bugMelden(
        kennung: String,
        titel: String,
        beschreibung: String,
        hergang: String,
        seite: String?,
    ) = netz.ohneAntwort(
        "/api/bugs",
        "POST",
        buildJsonObject {
            put("kennung", kennung)
            put("titel", titel)
            put("beschreibung", beschreibung)
            put("hergang", hergang)
            put("seite", seite?.takeIf { it.isNotBlank() })
        }.toString(),
    )

    // ------------------------------------------------------------------- Welt

    /** Gibt 200, wenn es eine Leitstelle in World gibt; 404 heißt „noch keine gebaut". */
    suspend fun weltHatLeitstelle(kennung: String): Boolean = runCatching {
        netz.roh("/api/welt/stand?kennung=${teil(kennung)}")
        true
    }.getOrDefault(false)

    /**
     * Setzt World zurück. Das Passwort steht im Rumpf und nicht im Abfrageteil:
     * Abfrageteile stehen in Serverprotokollen.
     */
    suspend fun weltZuruecksetzen(kennung: String, passwort: String) = netz.ohneAntwort(
        "/api/welt/leitstelle?kennung=${teil(kennung)}",
        "DELETE",
        buildJsonObject { put("passwort", passwort) }.toString(),
    )

    // ------------------------------------------------------------ Datenschutz

    suspend fun analyse(kennung: String): Analysestand =
        netz.hole("/api/konto/${teil(kennung)}/analyse")

    /** `false` löscht serverseitig zugleich alles, was bisher aufgezeichnet wurde. */
    suspend fun analyseSpeichern(kennung: String, zugestimmt: Boolean): Analysestand =
        netz.hole(
            "/api/konto/${teil(kennung)}/analyse",
            "PUT",
            buildJsonObject { put("zugestimmt", zugestimmt) }.toString(),
        )

    /**
     * Alles, was zu diesem Konto gespeichert ist (Art. 15 und 20 DSGVO) — als
     * eingerücktes JSON, bereit für die Datei.
     */
    suspend fun datenauszug(kennung: String): String {
        val roh = netz.roh("/api/konto/${teil(kennung)}/datenauszug") ?: "{}"
        return runCatching {
            SCHOEN.encodeToString(JsonElement.serializer(), SCHOEN.parseToJsonElement(roh))
        }.getOrDefault(roh)
    }

    suspend fun sitzungen(kennung: String): List<Sitzungsuebersicht> =
        netz.hole("/api/konto/${teil(kennung)}/sitzungen")

    /** Meldet alle Sitzungen außer dieser ab. */
    suspend fun andereSitzungenBeenden(kennung: String) =
        netz.ohneAntwort("/api/konto/${teil(kennung)}/sitzungen/andere-beenden", "POST")

    /**
     * Die Privatsphäre. Die Seite führt ihren Stand selbst (wie im Web): jüngste
     * Anfrage gewinnt, bei einem Fehler nur die eigene Änderung zurück, dann den
     * echten Stand holen.
     */
    suspend fun privatsphaere(kennung: String): Privatsphaere =
        netz.hole("/api/konto/${teil(kennung)}/privatsphaere")

    /** Immer der ganze Satz, nie ein einzelnes Feld. */
    suspend fun privatsphaereSpeichern(kennung: String, einstellungen: Privatsphaere): Privatsphaere =
        netz.hole(
            "/api/konto/${teil(kennung)}/privatsphaere",
            "PUT",
            VOLL.encodeToString(Privatsphaere.serializer(), einstellungen),
        )

    /** Die Mitteilungs-Schalter — aus demselben Grund wie oben hier. */
    suspend fun mitteilungseinstellungen(kennung: String): Mitteilungseinstellungen =
        netz.hole("/api/konto/${teil(kennung)}/mitteilungen")

    suspend fun mitteilungseinstellungenSpeichern(
        kennung: String,
        einstellungen: Mitteilungseinstellungen,
    ): Mitteilungseinstellungen = netz.hole(
        "/api/konto/${teil(kennung)}/mitteilungen",
        "PUT",
        VOLL.encodeToString(Mitteilungseinstellungen.serializer(), einstellungen),
    )

    /** Die Freundesliste — hier nur für die eigenen Blockaden auf der Datenschutzseite. */
    suspend fun freunde(kennung: String): List<Freund> =
        netz.hole("/api/freunde/${teil(kennung)}")

    /** Löst eine Beziehung — hier: hebt eine eigene Blockade auf. */
    suspend fun blockadeAufheben(kennung: String, wen: String) =
        netz.ohneAntwort("/api/freunde/${teil(kennung)}/${teil(wen)}", "DELETE")

    // ------------------------------------------------------------ Übertragung

    /** Alle Einwilligungen dieses Kontos — samt widerrufener und abgelaufener. */
    suspend fun einwilligungen(kennung: String): List<Einwilligung> =
        netz.hole("/api/streaming/${teil(kennung)}/einwilligungen")

    /** Nimmt eine Einwilligung zurück — „jederzeit" im Sinne von Art. 7 Abs. 3 DSGVO. */
    suspend fun einwilligungWiderrufen(kennung: String, id: String) =
        netz.ohneAntwort("/api/streaming/${teil(kennung)}/einwilligung/${teil(id)}", "DELETE")

    // ---------------------------------------------------------------- Discord

    suspend fun discordStatus(kennung: String): DiscordStatus =
        netz.hole("/api/discord/status?kennung=${teil(kennung)}")

    /** Die Adresse, an der Discord um Zustimmung fragt — gebaut vom Server. */
    suspend fun discordStart(kennung: String): Weiterleitung =
        netz.hole("/api/discord/start?kennung=${teil(kennung)}", "POST")

    suspend fun discordLoesen(kennung: String) =
        netz.ohneAntwort("/api/discord/verknuepfung?kennung=${teil(kennung)}", "DELETE")

    // ------------------------------------------------------------------ Codes

    /** Die Seite hinter einem Creator-Code — ohne Anmeldung. */
    suspend fun codevorschau(code: String): Codevorschau =
        netz.hole("/api/code/${teil(code)}")

    /** Löst einen Aktionscode ein. Die Antwort sagt, was gekommen ist. */
    suspend fun codeEinloesen(kennung: String, code: String): Codeertrag =
        netz.hole(
            "/api/konto/${teil(kennung)}/code",
            "POST",
            buildJsonObject { put("code", code) }.toString(),
        )

    // --------------------------------------------------------------- Verträge

    /** „Verträge hier kündigen" (§ 312k BGB) — ohne Anmeldung erreichbar. */
    suspend fun vertragKuendigen(
        kuendigungsart: String,
        zeitpunkt: String,
        grund: String?,
        name: String,
        benutzername: String,
        email: String,
    ): Erklaerungsquittung = netz.hole(
        "/api/vertrag/kuendigen",
        "POST",
        buildJsonObject {
            put("kuendigungsart", kuendigungsart)
            put("zeitpunkt", zeitpunkt)
            put("grund", grund)
            put("name", name)
            put("benutzername", benutzername)
            put("email", email)
        }.toString(),
    )

    /** „Vertrag widerrufen" (§ 356a BGB) — dieselbe Bauart wie die Kündigung. */
    suspend fun vertragWiderrufen(
        name: String,
        benutzername: String,
        email: String,
        abgeschlossenAm: String?,
    ): Erklaerungsquittung = netz.hole(
        "/api/vertrag/widerrufen",
        "POST",
        buildJsonObject {
            put("name", name)
            put("benutzername", benutzername)
            put("email", email)
            put("abgeschlossenAm", abgeschlossenAm)
        }.toString(),
    )

    private companion object {
        /** Eingerückt, für die Datei des Datenauszugs — lesbar in jedem Texteditor. */
        val SCHOEN = Json { prettyPrint = true }

        /**
         * Der ganze Satz, auch was der Vorgabe entspricht. `Netz.abgabe` lässt
         * Vorgabewerte weg; der Server setzt dann zwar dieselben Vorgaben ein, aber
         * „immer der ganze Satz" soll hier wörtlich gelten.
         */
        val VOLL = Json(Netz.abgabe) { encodeDefaults = true }
    }
}

/** Ein Stück Adresse — kodiert, wie in `Konten.kt` (dort ebenfalls dateiprivat). */
private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
