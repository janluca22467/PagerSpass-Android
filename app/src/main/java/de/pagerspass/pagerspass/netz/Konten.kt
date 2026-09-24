package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Konto-Wege der API — übertragen aus dem Abschnitt „Konto" in
 * `web/src/api/rest.ts`.
 *
 * <b>Wer das Merkmal setzt, setzt auch die Kennung.</b> Im Web tut das
 * `merkmalSetzen` an drei Stellen (Anlegen, Anmelden, Zweifaktor) und
 * `kontoAbmelden` räumt es weg — hier steht dieselbe Regel in
 * `anmeldungMerken`. Sie gehört in diese Datei und nicht in die Ansicht: Eine
 * Anmeldung, die nur die Ansicht kennt, überlebt keinen Neustart.
 */
class Konten(private val netz: Netz, private val ablage: Ablage) {

    /**
     * Legt ein Konto an.
     *
     * <b>Der Rechtsstand geht mit.</b> Nur der Client weiß, welche Fassung der
     * Texte beim Setzen der Zustimmungshaken tatsächlich angezeigt wurde — der
     * Server kann das nicht erraten, und ohne die Angabe wäre die Zustimmung
     * nicht nachweisbar.
     *
     * <b>Nur die Antwort auf diesen einen Aufruf enthält den
     * Wiederherstellungscode.</b> Wer ihn hier nicht zeigt, zeigt ihn nie.
     *
     * <b>Auf der Beta kommt das Konto ohne Merkmal zurück</b> (`freischaltungOffen`):
     * Es ist angelegt, aber erst das Team schaltet es frei. Dann wird hier nichts
     * gemerkt — die Ablage bleibt leer, und die Sitzung zeigt den Hinweis.
     */
    suspend fun anlegen(benutzername: String, anzeigename: String, passwort: String): Konto {
        val konto: Konto = netz.hole(
            "/api/konto",
            "POST",
            buildJsonObject {
                put("benutzername", benutzername)
                put("anzeigename", anzeigename)
                put("passwort", passwort)
                put("rechtsstand", Rechtsstand.AKTUELL)
            }.toString(),
        )

        if (!konto.freischaltungOffen) ablage.anmeldungMerken(konto.merkmal, konto.kennung)
        return konto
    }

    /**
     * Meldet an — und kommt unter Umständen nur bis zur Hälfte.
     *
     * Der Rumpf trägt entweder das Konto oder den Zweifaktor-Zettel. Deshalb
     * wird er hier zweimal gelesen: einmal als Zettel, um die Weiche zu stellen,
     * und nur im Normalfall als Konto.
     */
    suspend fun anmelden(benutzername: String, passwort: String): Anmeldeergebnis {
        val roh = netz.roh(
            "/api/konto/anmelden",
            "POST",
            buildJsonObject {
                put("benutzername", benutzername)
                put("passwort", passwort)
            }.toString(),
        ) ?: throw Netzfehler("Die Leitstelle hat nichts zurückgemeldet.")

        val zettel = Netz.abgabe.decodeFromString<Anmeldeantwort>(roh)
        if (zettel.zweiFaktor) {
            return Anmeldeergebnis.ZweiFaktor(zettel.anfrage.orEmpty(), zettel.ziel.orEmpty())
        }

        val konto = Netz.abgabe.decodeFromString<Konto>(roh)
        ablage.anmeldungMerken(konto.merkmal, konto.kennung)
        return Anmeldeergebnis.Angemeldet(konto)
    }

    /** Der zweite Schritt: die sechs Ziffern aus dem Postfach. */
    suspend fun zweiFaktorBestaetigen(anfrage: String, code: String): Konto {
        val konto: Konto = netz.hole(
            "/api/konto/anmelden/zweifaktor",
            "POST",
            buildJsonObject {
                put("anfrage", anfrage)
                put("code", code)
            }.toString(),
        )

        ablage.anmeldungMerken(konto.merkmal, konto.kennung)
        return konto
    }

    /**
     * Meldet diese Sitzung am Server ab und wirft das Merkmal weg.
     *
     * <b>Auch wenn der Server nicht erreichbar war.</b> Wer im Flugmodus auf
     * „Abmelden" tippt, ist abgemeldet — sonst bliebe das Merkmal liegen und die
     * App stünde beim nächsten Start wieder im fremden Konto.
     */
    suspend fun abmelden() {
        try {
            netz.ohneAntwort("/api/konto/abmelden", "POST")
        } catch (_: Exception) {
            // Absicht — siehe oben.
        } finally {
            ablage.anmeldungMerken(null, null)
        }
    }

    /** Das eigene Konto nachladen — der Weg, an dem der Team-Haken hängt. */
    suspend fun laden(kennung: String): Konto = netz.hole("/api/konto/${teil(kennung)}")

    suspend fun umbenennen(kennung: String, anzeigename: String): Konto = netz.hole(
        "/api/konto/${teil(kennung)}",
        "PUT",
        buildJsonObject { put("anzeigename", anzeigename) }.toString(),
    )

    /**
     * Stimmt einer neuen Fassung der Rechtstexte zu.
     *
     * Die Fassung kommt von hier und nicht vom Server, aus demselben Grund wie
     * beim Anlegen: Nur der Client weiß, welcher Text beim Zustimmen tatsächlich
     * angezeigt wurde.
     */
    suspend fun rechtsstandZustimmen(kennung: String): Konto = netz.hole(
        "/api/konto/${teil(kennung)}/rechtsstand",
        "PUT",
        buildJsonObject { put("rechtsstand", Rechtsstand.AKTUELL) }.toString(),
    )

    suspend fun passwortAendern(kennung: String, aktuelles: String, neues: String) =
        netz.ohneAntwort(
            "/api/konto/${teil(kennung)}/passwort",
            "PUT",
            buildJsonObject {
                put("aktuellesPasswort", aktuelles)
                put("neuesPasswort", neues)
            }.toString(),
        )

    /**
     * Löscht das Konto dauerhaft.
     *
     * <b>Dieser Weg ist Pflicht, nicht Ausstattung.</b> Google Play verlangt für
     * jede App, in der man ein Konto anlegen kann, zwei Wege zur Löschung: einen
     * in der App und einen über eine öffentlich erreichbare Adresse. Dies ist
     * der erste; der zweite ist eine Angabe in der Play Console.
     */
    suspend fun loeschen(kennung: String, passwort: String) {
        netz.ohneAntwort(
            "/api/konto/${teil(kennung)}",
            "DELETE",
            buildJsonObject { put("passwort", passwort) }.toString(),
        )
        ablage.anmeldungMerken(null, null)
    }

    /** Die Fassung, die auf dem Server läuft — für den Fuß des Startbildschirms. */
    suspend fun version(): Versionsstand = netz.hole("/api/version")
}

/**
 * Ein Stück Adresse, das aus einer Kennung kommt.
 *
 * Kennungen sind vom Server vergeben und enthalten nichts, was kodiert werden
 * müsste — aber sie stehen im Pfad, und die Vorlage kodiert sie an jeder der
 * über sechzig Fundstellen. Was an sechzig Stellen richtig gemacht wird, wird an
 * der einundsechzigsten vergessen; hier ist es eine Stelle.
 */
private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
