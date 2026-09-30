package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege der Konto-Zentrale und des Shops — übertragen aus `web/src/api/rest.ts`.
 *
 * <b>Was hier ausdrücklich fehlt: `/premium/checkout` und `/premium/portal`.</b>
 * Beides führt zu Stripe und damit zu echtem Geld. Die App schließt kein Abo ab
 * und verwaltet keines — sie liest nur den Stand und schickt für alles andere auf
 * die Webseite. Wer hier einen der beiden Wege nachträgt, baut einen Kauf in die
 * App, den der Store ganz anders behandelt als einen Link.
 *
 * <b>Credits dagegen dürfen hier ausgegeben werden.</b> Sie sind Spielwährung,
 * verdient im Dienst; Sortiment, Tagesbonus, Codes und Geschenke bleiben deshalb
 * in der App.
 */
class Kontowege(private val netz: Netz) {

    // ------------------------------------------------------------- Premium

    /** Das eigene Konto — nach der Rückkehr von der Webseite, ohne die Sitzung neu zu prüfen. */
    suspend fun konto(kennung: String): Konto = netz.hole("/api/konto/${teil(kennung)}")

    suspend fun premium(kennung: String): Premiumstand =
        netz.hole("/api/konto/${teil(kennung)}/premium")

    // ---------------------------------------------------------- Sicherheit

    suspend fun passwortAendern(kennung: String, aktuelles: String, neues: String) =
        netz.ohneAntwort(
            "/api/konto/${teil(kennung)}/passwort",
            "PUT",
            buildJsonObject {
                put("aktuellesPasswort", aktuelles)
                put("neuesPasswort", neues)
            }.toString(),
        )

    /** Der eindeutige Name, unter dem einen andere finden. Die Antwort ist das neue Konto. */
    suspend fun benutzernameAendern(kennung: String, benutzername: String): Konto = netz.hole(
        "/api/konto/${teil(kennung)}/benutzername",
        "PUT",
        buildJsonObject { put("benutzername", benutzername) }.toString(),
    )

    /**
     * „Passwort vergessen" — der Satz zurück ist immer derselbe, auch wenn es das
     * Konto nicht gibt. Jede genauere Auskunft wäre ein Verzeichnis der Namen.
     */
    suspend fun passwortVergessen(benutzer: String): Satz = netz.hole(
        "/api/konto/passwort-vergessen",
        "POST",
        buildJsonObject { put("benutzer", benutzer) }.toString(),
    )

    suspend fun passwortNeu(benutzer: String, code: String, passwort: String): Satz = netz.hole(
        "/api/konto/passwort-neu",
        "POST",
        buildJsonObject {
            put("benutzer", benutzer)
            put("code", code)
            put("passwort", passwort)
        }.toString(),
    )

    // ------------------------------------------------------------ Postfach

    suspend fun postfach(kennung: String): Postfach =
        netz.hole("/api/konto/${teil(kennung)}/postfach")

    /** Trägt eine Adresse ein oder ersetzt sie; der Code geht sofort raus. */
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
    suspend fun emailCodeAnfordern(kennung: String) =
        netz.ohneAntwort("/api/konto/${teil(kennung)}/postfach/email/code", "POST")

    suspend fun emailBestaetigen(kennung: String, code: String): Postfach = netz.hole(
        "/api/konto/${teil(kennung)}/postfach/email/bestaetigen",
        "POST",
        buildJsonObject { put("code", code) }.toString(),
    )

    suspend fun newsletter(kennung: String, an: Boolean): Postfach = netz.hole(
        "/api/konto/${teil(kennung)}/postfach/newsletter",
        "PUT",
        buildJsonObject { put("an", an) }.toString(),
    )

    /** Das Passwort braucht nur das Ausschalten — Einschalten macht das Konto sicherer. */
    suspend fun zweiFaktor(kennung: String, an: Boolean, passwort: String): Postfach = netz.hole(
        "/api/konto/${teil(kennung)}/postfach/zweifaktor",
        "PUT",
        buildJsonObject {
            put("an", an)
            put("passwort", passwort)
        }.toString(),
    )

    // --------------------------------------------------------- Spielweise

    suspend fun patientensimulation(kennung: String): Schalterstand =
        netz.hole("/api/konto/${teil(kennung)}/patientensimulation")

    suspend fun patientensimulationSetzen(kennung: String, an: Boolean): Schalterstand =
        netz.hole(
            "/api/konto/${teil(kennung)}/patientensimulation",
            "PUT",
            buildJsonObject { put("an", an) }.toString(),
        )

    /** `false` löscht serverseitig zugleich alles bisher Aufgezeichnete. */
    suspend fun analyseSetzen(kennung: String, zugestimmt: Boolean): Analysestand = netz.hole(
        "/api/konto/${teil(kennung)}/analyse",
        "PUT",
        buildJsonObject { put("zugestimmt", zugestimmt) }.toString(),
    )

    // ------------------------------------------------------------- Welt

    /** Ob es überhaupt eine Leitstelle gibt — ein 404 heißt „noch keine gebaut". */
    suspend fun weltVorhanden(kennung: String): Boolean =
        runCatching { netz.roh("/api/welt/stand?kennung=${teil(kennung)}") }.isSuccess

    /** Das Passwort im Rumpf, nicht in der Adresse — die steht in Protokollen. */
    suspend fun weltZuruecksetzen(kennung: String, passwort: String) = netz.ohneAntwort(
        "/api/welt/leitstelle?kennung=${teil(kennung)}",
        "DELETE",
        buildJsonObject { put("passwort", passwort) }.toString(),
    )

    // ------------------------------------------------------------- Bugs

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
            put("seite", seite?.ifBlank { null })
        }.toString(),
    )

    // ------------------------------------------------------ Werbung, Codes

    suspend fun werbung(kennung: String): Werbung = netz.hole("/api/konto/${teil(kennung)}/werbung")

    suspend fun werbecodeErzeugen(kennung: String): Werbung =
        netz.hole("/api/konto/${teil(kennung)}/werbung/code", "POST")

    suspend fun werbungEinloesen(kennung: String, code: String): Werbung = netz.hole(
        "/api/konto/${teil(kennung)}/werbung/einloesen",
        "POST",
        buildJsonObject { put("code", code) }.toString(),
    )

    suspend fun codeEinloesen(kennung: String, code: String): Codeertrag = netz.hole(
        "/api/konto/${teil(kennung)}/code",
        "POST",
        buildJsonObject { put("code", code) }.toString(),
    )

    /** Ohne Anmeldung — und ein Code ohne Creator antwortet mit 404 wie ein unbekannter. */
    suspend fun codevorschau(code: String): Codevorschau = netz.hole("/api/code/${teil(code)}")

    // --------------------------------------------------------- Geschenke

    suspend fun schenkfreunde(kennung: String): List<Schenkfreund> =
        netz.hole("/api/freunde/${teil(kennung)}/schenken")

    suspend fun schenken(kennung: String, an: String, artikelId: String): Shop = netz.hole(
        "/api/freunde/${teil(kennung)}/schenken",
        "POST",
        buildJsonObject {
            put("an", an)
            put("artikelId", artikelId)
        }.toString(),
    )

    suspend fun geschenkOeffnen(kennung: String, nr: Long): Geschenkinhalt =
        netz.hole("/api/freunde/${teil(kennung)}/geschenke/$nr/oeffnen", "POST")

    /** Löst eine Beziehung — für eine Blockade heißt das: sie aufheben. */
    suspend fun freundLoesen(kennung: String, wen: String) =
        netz.ohneAntwort("/api/freunde/${teil(kennung)}/${teil(wen)}", "DELETE")

    // ------------------------------------------------------- Datenschutz

    suspend fun sitzungen(kennung: String): List<Sitzungsuebersicht> =
        netz.hole("/api/konto/${teil(kennung)}/sitzungen")

    suspend fun andereSitzungenBeenden(kennung: String) =
        netz.ohneAntwort("/api/konto/${teil(kennung)}/sitzungen/andere-beenden", "POST")

    /**
     * Alles, was zu diesem Konto gespeichert ist — als JSON, schön gesetzt.
     *
     * Gelesen als Baum und wieder geschrieben, damit die Datei lesbar ist: Der
     * Server schickt eine einzige Zeile, und eine Auskunft nach Art. 15 DSGVO, die
     * man nicht lesen kann, ist nur halb eine.
     */
    suspend fun datenauszug(kennung: String): String {
        val roh = netz.roh("/api/konto/${teil(kennung)}/datenauszug") ?: "{}"
        return runCatching {
            schoen.encodeToString(JsonElement.serializer(), Netz.abgabe.parseToJsonElement(roh))
        }.getOrDefault(roh)
    }

    suspend fun einwilligungen(kennung: String): List<Einwilligung> =
        netz.hole("/api/streaming/${teil(kennung)}/einwilligungen")

    /**
     * Erteilt die Einwilligung für die Runde mit diesem Code.
     *
     * <b>Kein Kanal geht mit</b> — den nimmt der Server aus der Runde. Die Fassung
     * muss die sein, die angezeigt wurde (`UEBERTRAGUNG_FASSUNG` im Web); weicht sie
     * von der des Servers ab, weist er ab. Gerufen wird das vom Beitritt einer Runde,
     * der an der Einwilligung scheitert — nicht von der Privatsphäre, die nur
     * widerruft.
     */
    suspend fun einwilligungErteilen(
        kennung: String,
        raumCode: String,
        volljaehrig: Boolean,
        fassung: String = UEBERTRAGUNG_FASSUNG,
    ): Einwilligung = netz.hole(
        "/api/streaming/${teil(kennung)}/einwilligung",
        "POST",
        buildJsonObject {
            put("raumCode", raumCode)
            put("volljaehrig", volljaehrig)
            put("fassung", fassung)
        }.toString(),
    )

    suspend fun einwilligungWiderrufen(kennung: String, id: String) =
        netz.ohneAntwort("/api/streaming/${teil(kennung)}/einwilligung/${teil(id)}", "DELETE")

    // ----------------------------------------------------------- Discord

    suspend fun discordStatus(kennung: String): Discordstatus =
        netz.hole("/api/discord/status?kennung=${teil(kennung)}")

    /** Die Adresse, an der Discord um Zustimmung fragt — nur der Server kennt ihr `state`. */
    suspend fun discordStart(kennung: String): Adresse =
        netz.hole("/api/discord/start?kennung=${teil(kennung)}", "POST")

    suspend fun discordLoesen(kennung: String) =
        netz.ohneAntwort("/api/discord/verknuepfung?kennung=${teil(kennung)}", "DELETE")

    private companion object {
        val schoen = kotlinx.serialization.json.Json { prettyPrint = true }
    }
}

/** Die Fassung des Übertragungs-Einwilligungstexts — `recht/rechtstexte.ts` im Web. */
const val UEBERTRAGUNG_FASSUNG = "2026-09-15"

private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
