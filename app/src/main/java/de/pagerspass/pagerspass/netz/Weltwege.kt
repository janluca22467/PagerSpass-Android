package de.pagerspass.pagerspass.netz

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege von PagerSpass - World — übertragen aus `web/src/api/rest.ts`.
 *
 * Alle Wege liegen unter `/api/welt` und sind serverseitig durch einen Gruppenfilter
 * geschützt (siehe `Endpunkte/WeltEndpunkte.cs`) — ohne aktives Premium antworten sie
 * mit 403. Der Startbildschirm hält davor schon den Eintrag zu; beides zusammen ist
 * Absicht: Ein Eintrag ist keine Zugangssperre.
 *
 * <b>Alle `/api/welt/*` tragen `?kennung=`.</b> Die Icon-Wege darunter nicht — die
 * lesen die geprüfte Kennung aus dem Sitzungskontext (siehe `IconEndpunkte.Wer`).
 *
 * <b>Fehler</b> kommen als `Netzfehler` — mit `auswahlNeu` und `retryNach`, wo der
 * Server sie beilegt (siehe `Netz.ausFehler`). Den einen Wiederholungsversuch bei
 * `auswahlNeu` macht der Zustand (`mobil/Welt.kt`, `mitToken`), nicht dieser Weg.
 */
class Weltwege(private val netz: Netz) {

    // ------------------------------------------------------------ Karte, Stand

    /** Alle Leitstellen, die zuletzt gespielt haben — die gemeinsame Karte. */
    suspend fun karte(kennung: String): List<Weltleitstelle> =
        netz.hole("/api/welt/karte?kennung=${teil(kennung)}")

    /** Gründet die eine Leitstelle dieses Kontos. */
    suspend fun leitstelleGruenden(kennung: String, name: String, lat: Double, lon: Double) =
        netz.ohneAntwort(
            "/api/welt/leitstelle?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject {
                put("name", name)
                put("lat", lat)
                put("lon", lon)
            }.toString(),
        )

    /** Der Besitzstand. 404 heißt „noch keine Leitstelle gebaut". */
    suspend fun stand(kennung: String): Weltstand =
        netz.hole("/api/welt/stand?kennung=${teil(kennung)}")

    /** Gründet die Zweigstelle — den zweiten (dritten …) Ausrückebereich. */
    suspend fun zweigstelleGruenden(kennung: String, name: String, lat: Double, lon: Double) =
        netz.ohneAntwort(
            "/api/welt/zweigstelle?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject {
                put("name", name)
                put("lat", lat)
                put("lon", lon)
            }.toString(),
        )

    /**
     * Der laufende Betrieb. <b>Der Abruf ist zugleich das Lebenszeichen</b> — er setzt
     * auf dem Server `ZuletztGesehen`.
     */
    suspend fun betrieb(kennung: String): Weltbetrieb =
        netz.hole("/api/welt/betrieb?kennung=${teil(kennung)}")

    /** Die Laufbahn: dreißig und mehr Stufen, einmal je Sitzung. */
    suspend fun laufbahn(kennung: String): List<WeltLaufbahnstufe> =
        netz.hole("/api/welt/laufbahn?kennung=${teil(kennung)}")

    // --------------------------------------------------------------- Leihmarkt

    suspend fun leihen(kennung: String): WeltLeihstand =
        netz.hole("/api/welt/leihen?kennung=${teil(kennung)}")

    /** Stellt ein eigenes Fahrzeug in den Markt — es fährt noch nicht los. */
    suspend fun leiheAnbieten(kennung: String, fahrzeugId: String, tage: Int, preis: Int) =
        netz.ohneAntwort(
            "/api/welt/leihen?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject {
                put("fahrzeugId", fahrzeugId)
                put("tage", tage)
                put("preis", preis)
            }.toString(),
        )

    /** Mietet ein Angebot: Geld wechselt, das Fahrzeug fährt zur gewählten eigenen Wache. */
    suspend fun leiheMieten(kennung: String, id: String, zielWacheId: String) =
        netz.ohneAntwort(
            "/api/welt/leihen/${teil(id)}/mieten?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject { put("zielWacheId", zielWacheId) }.toString(),
        )

    /** Zieht das eigene Angebot aus dem Markt zurück. */
    suspend fun leiheZuruecknehmen(kennung: String, id: String) =
        netz.ohneAntwort("/api/welt/leihen/${teil(id)}?kennung=${teil(kennung)}", "DELETE")

    /** Gibt ein geliehenes Fahrzeug vorzeitig zurück — es fährt im nächsten Takt heim. */
    suspend fun leiheZurueckgeben(kennung: String, fahrzeugId: String) =
        netz.ohneAntwort(
            "/api/welt/leihen/${teil(fahrzeugId)}/zurueckgeben?kennung=${teil(kennung)}",
            "POST",
        )

    /** Hängt ein Gesuch in den Markt: diese Fahrzeugart, so viele Tage, so viel geboten. */
    suspend fun gesuchAufgeben(
        kennung: String,
        kategorie: String,
        zielWacheId: String,
        tage: Int,
        preis: Int,
    ) = netz.ohneAntwort(
        "/api/welt/gesuche?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("kategorie", kategorie)
            put("zielWacheId", zielWacheId)
            put("tage", tage)
            put("preis", preis)
        }.toString(),
    )

    suspend fun gesuchZurueckziehen(kennung: String, id: String) =
        netz.ohneAntwort("/api/welt/gesuche/${teil(id)}?kennung=${teil(kennung)}", "DELETE")

    /** Bedient ein fremdes Gesuch: das gewählte eigene Fahrzeug fährt los, das Geld kommt. */
    suspend fun gesuchBedienen(kennung: String, id: String, fahrzeugId: String) =
        netz.ohneAntwort(
            "/api/welt/gesuche/${teil(id)}/bedienen?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject { put("fahrzeugId", fahrzeugId) }.toString(),
        )

    // ------------------------------------------------------------ Einstellungen

    suspend fun einstellung(kennung: String): WeltEinstellung =
        netz.hole("/api/welt/einstellungen?kennung=${teil(kennung)}")

    /**
     * Setzt die Gangart. Bei allem außer `Eigen` entscheidet der Server aus der groben
     * Wahl — die drei Zahlen gehen trotzdem mit, damit der Aufruf eine Form hat.
     */
    suspend fun einstellungSetzen(
        kennung: String,
        gangart: String,
        dichte: Int,
        nachschub: Int,
        arbeitszeit: Int,
    ) = netz.ohneAntwort(
        "/api/welt/einstellungen?kennung=${teil(kennung)}",
        "PUT",
        buildJsonObject {
            put("gangart", gangart)
            put("dichte", dichte)
            put("nachschub", nachschub)
            put("arbeitszeit", arbeitszeit)
        }.toString(),
    )

    // ------------------------------------------------------------- Icon-Packs

    /** Die eigenen Packs — für die Pillenreihe in den Einstellungen. Ohne `kennung`. */
    suspend fun iconpacks(): List<WeltIconpack> = netz.hole("/api/icons/packs")

    /** Die Icons des Packs, das gerade gilt — leer heißt Standard. */
    suspend fun iconpackAktiv(): List<WeltPackicon> = netz.hole("/api/icons/aktiv")

    /** Stellt ein Pack auf die Karte — `null` heißt zurück auf den gezeichneten Standard. */
    suspend fun iconpackWaehlen(packId: String?) = netz.ohneAntwort(
        "/api/icons/aktiv",
        "PUT",
        buildJsonObject {
            put("packId", packId?.let { JsonPrimitive(it) } ?: JsonNull)
        }.toString(),
    )

    // ------------------------------------------------------------ Lagen, Alarm

    /**
     * Alarmiert Fahrzeuge auf eine Lage. Eine Liste und nicht ein Fahrzeug: Ein Zug
     * wird gemeinsam alarmiert.
     */
    suspend fun alarmieren(
        kennung: String,
        lageId: String,
        fahrzeugIds: List<String>,
        token: String,
    ): WeltAlarmantwort = netz.hole(
        "/api/welt/lagen/${teil(lageId)}/alarm?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("fahrzeugIds", texte(fahrzeugIds))
            put("token", token)
        }.toString(),
    )

    /** Die eigenen Fahrzeuge wieder aus einer Lage entlassen — ohne Token. */
    suspend fun lageEinruecken(kennung: String, lageId: String): WeltEinrueckantwort =
        netz.hole(
            "/api/welt/lagen/${teil(lageId)}/einruecken?kennung=${teil(kennung)}",
            "POST",
        )

    /**
     * Die Fahrzeugauswahl einer Lage öffnen — die Voraussetzung des Alarms: Der Server
     * antwortet mit einem kurzlebigen Token, das der Alarm zurückverlangt.
     */
    suspend fun auswahl(kennung: String, lageId: String): WeltAuswahl =
        netz.hole("/api/welt/lagen/${teil(lageId)}/auswahl?kennung=${teil(kennung)}")

    /** Einen ganzen Zug alarmieren — geschickt wird die Zug-Id, nicht die Fahrzeugliste. */
    suspend fun zugAlarmieren(
        kennung: String,
        lageId: String,
        zugId: String,
        token: String,
    ): WeltAlarmantwort = netz.hole(
        "/api/welt/lagen/${teil(lageId)}/alarm?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("fahrzeugIds", JsonArray(emptyList()))
            put("zugId", zugId)
            put("token", token)
        }.toString(),
    )

    /** Gibt eine eigene Lage für alle frei — endgültig. */
    suspend fun lageFreigeben(kennung: String, lageId: String) = netz.ohneAntwort(
        "/api/welt/lagen/${teil(lageId)}/freigeben?kennung=${teil(kennung)}",
        "POST",
    )

    /** Bricht eine laufende Anfahrt ab: Das Fahrzeug dreht um und rückt ein. */
    suspend fun anfahrtAbbrechen(kennung: String, fahrzeugId: String) = netz.ohneAntwort(
        "/api/welt/fahrzeuge/${teil(fahrzeugId)}/abbrechen?kennung=${teil(kennung)}",
        "POST",
    )

    // ------------------------------------------------------------------ Streife

    /** Die Orte, die sich als Streifenstation anbieten — nach Nähe zur Leitstelle. */
    suspend fun streifenorte(kennung: String): List<WeltStreifenstation> =
        netz.hole("/api/welt/streifenorte?kennung=${teil(kennung)}")

    /** Wie ein frei auf der Karte gesetzter Punkt heißt — benannt wird auf dem Server. */
    suspend fun ortsname(kennung: String, lat: Double, lon: Double): WeltOrtsname =
        netz.hole("/api/welt/ortsname?kennung=${teil(kennung)}&lat=$lat&lon=$lon")

    /** Setzt den Streifenpfad eines Fahrzeugs — eine leere Liste nimmt ihn zurück. */
    suspend fun streifeSetzen(
        kennung: String,
        fahrzeugId: String,
        stationen: List<WeltStreifenstation>,
    ) = netz.ohneAntwort(
        "/api/welt/fahrzeuge/${teil(fahrzeugId)}/streife?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject { put("stationen", stationenJson(stationen)) }.toString(),
    )

    suspend fun streifenrouten(kennung: String): List<WeltStreifenroute> =
        netz.hole("/api/welt/streifenrouten?kennung=${teil(kennung)}")

    /** Legt eine Route an — gleicher Name überschreibt die vorhandene. */
    suspend fun streifenrouteSichern(
        kennung: String,
        name: String,
        stationen: List<WeltStreifenstation>,
    ) = netz.ohneAntwort(
        "/api/welt/streifenrouten?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("name", name)
            put("stationen", stationenJson(stationen))
        }.toString(),
    )

    suspend fun streifenrouteEntfernen(kennung: String, id: String) = netz.ohneAntwort(
        "/api/welt/streifenrouten/${teil(id)}?kennung=${teil(kennung)}",
        "DELETE",
    )

    /** Legt eine gespeicherte Route auf ein Fahrzeug — geprüft wird auf dem Server. */
    suspend fun streifenrouteAuflegen(kennung: String, fahrzeugId: String, routeId: String) =
        netz.ohneAntwort(
            "/api/welt/fahrzeuge/${teil(fahrzeugId)}/streife/route/${teil(routeId)}" +
                "?kennung=${teil(kennung)}",
            "POST",
        )

    // ------------------------------------------------------------ Eigene Punkte

    /** Die selbst gesetzten Punkte — alle, auch die ausgeblendeten. */
    suspend fun pois(kennung: String): List<Weltpoi> =
        netz.hole("/api/welt/pois?kennung=${teil(kennung)}")

    /**
     * Setzt einen neuen Punkt. Mit `gelaende` werden `lat`/`lon` und `flaeche`
     * überhört — die Ecken sagen beides schon.
     */
    suspend fun poiSetzen(
        kennung: String,
        name: String,
        art: String,
        lat: Double,
        lon: Double,
        flaeche: Double,
        gelaende: List<WeltEcke>? = null,
        gelaendeart: String? = null,
        strecke: Boolean = false,
    ): Weltpoi = netz.hole(
        "/api/welt/pois?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("name", name)
            put("art", art)
            put("lat", lat)
            put("lon", lon)
            put("flaeche", flaeche)
            if (gelaende != null) put("gelaende", eckenJson(gelaende))
            put("gelaendeart", gelaendeart?.let { JsonPrimitive(it) } ?: JsonNull)
            put("strecke", strecke)
        }.toString(),
    )

    /**
     * Ändert einen Punkt — geschickt wird nur, was sich ändert.
     *
     * `betroffene = -1` nimmt die eigene Zahl zurück; `hinweise` ist eine Zeile je
     * Hinweis, leer löscht sie. Eine leere `gelaende`-Liste nimmt das Gelände zurück.
     * `gelaendeart` wirkt nur mit `gelaendeartSetzen = true` — null heißt dann
     * „Gebäudegrundstück".
     */
    suspend fun poiAendern(kennung: String, id: String, aenderung: WeltPoiAenderung) {
        val rumpf = buildJsonObject {
            aenderung.name?.let { put("name", it) }
            aenderung.art?.let { put("art", it) }
            aenderung.flaeche?.let { put("flaeche", it) }
            aenderung.sichtbar?.let { put("sichtbar", it) }
            aenderung.aktiv?.let { put("aktiv", it) }
            aenderung.gewicht?.let { put("gewicht", it) }
            aenderung.nachtsBelegt?.let { put("nachtsBelegt", it) }
            aenderung.organisationen?.let { put("organisationen", texte(it)) }
            aenderung.farbe?.let { put("farbe", it) }
            aenderung.betroffene?.let { put("betroffene", it) }
            aenderung.hinweise?.let { put("hinweise", it) }
            aenderung.gelaende?.let { put("gelaende", eckenJson(it)) }
            if (aenderung.gelaendeartSetzen) {
                put("gelaendeart", aenderung.gelaendeart?.let { JsonPrimitive(it) } ?: JsonNull)
                put("gelaendeartSetzen", true)
            }
            aenderung.strecke?.let { put("strecke", it) }
        }
        netz.patch("/api/welt/pois/${teil(id)}?kennung=${teil(kennung)}", rumpf.toString())
    }

    /** Nimmt einen Punkt endgültig weg — das ist der Unterschied zum Ausblenden. */
    suspend fun poiEntfernen(kennung: String, id: String) =
        netz.ohneAntwort("/api/welt/pois/${teil(id)}?kennung=${teil(kennung)}", "DELETE")

    // -------------------------------------------------------------------- Züge

    /** Alle eigenen Züge — für die Ein-Klick-Alarmierung im Lagendialog. */
    suspend fun zuege(kennung: String): List<WeltZug> =
        netz.hole("/api/welt/zuege?kennung=${teil(kennung)}")

    suspend fun zugAufstellen(
        kennung: String,
        wacheId: String,
        vorgabeId: String,
        name: String?,
    ): WeltNeueId = netz.hole(
        "/api/welt/zuege?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("wacheId", wacheId)
            put("vorgabeId", vorgabeId)
            put("name", name?.let { JsonPrimitive(it) } ?: JsonNull)
        }.toString(),
    )

    suspend fun zugAufloesen(kennung: String, zugId: String) =
        netz.ohneAntwort("/api/welt/zuege/${teil(zugId)}?kennung=${teil(kennung)}", "DELETE")

    suspend fun zugFahrzeugSetzen(kennung: String, zugId: String, fahrzeugId: String, dazu: Boolean) =
        netz.ohneAntwort(
            "/api/welt/zuege/${teil(zugId)}/fahrzeug?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject {
                put("fahrzeugId", fahrzeugId)
                put("dazu", dazu)
            }.toString(),
        )

    // ------------------------------------------------------- Wachen, Fahrzeuge

    suspend fun wacheBauen(kennung: String, name: String, art: String, lat: Double, lon: Double) =
        netz.ohneAntwort(
            "/api/welt/wachen?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject {
                put("name", name)
                put("art", art)
                put("lat", lat)
                put("lon", lon)
            }.toString(),
        )

    suspend fun wacheAbreissen(kennung: String, wacheId: String) =
        netz.ohneAntwort("/api/welt/wachen/${teil(wacheId)}?kennung=${teil(kennung)}", "DELETE")

    suspend fun fahrzeugKaufen(kennung: String, wacheId: String, vorlageId: String) =
        netz.ohneAntwort(
            "/api/welt/fahrzeuge?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject {
                put("wacheId", wacheId)
                put("vorlageId", vorlageId)
            }.toString(),
        )

    suspend fun fahrzeugVerkaufen(kennung: String, fahrzeugId: String) = netz.ohneAntwort(
        "/api/welt/fahrzeuge/${teil(fahrzeugId)}?kennung=${teil(kennung)}",
        "DELETE",
    )

    /** Baut eine Wache aus: zwei Stellplätze mehr, gegen Credits und echte Bauzeit. */
    suspend fun wacheAusbauen(kennung: String, wacheId: String) = netz.ohneAntwort(
        "/api/welt/wachen/${teil(wacheId)}/ausbau?kennung=${teil(kennung)}",
        "POST",
    )

    /** Benennt eine Wache um. Die vergebenen Funkrufnamen bleiben, wie sie sind. */
    suspend fun wacheUmbenennen(kennung: String, wacheId: String, name: String) =
        netz.ohneAntwort(
            "/api/welt/wachen/${teil(wacheId)}/name?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject { put("name", name) }.toString(),
        )

    /** Setzt ein Fahrzeug auf eine andere eigene Wache um — eine Verlegefahrt. */
    suspend fun fahrzeugUmsetzen(kennung: String, fahrzeugId: String, zielWacheId: String) =
        netz.ohneAntwort(
            "/api/welt/fahrzeuge/${teil(fahrzeugId)}/umsetzen?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject { put("zielWacheId", zielWacheId) }.toString(),
        )

    /** Die Seite einer Wache: Fahrzeuge, Ausbaustand, Einsatzzahl und ihre Chronik. */
    suspend fun wachenseite(kennung: String, wacheId: String): WeltWachenseite =
        netz.hole("/api/welt/wachen/${teil(wacheId)}?kennung=${teil(kennung)}")

    /** Setzt Wappen und Foto einer Wache — alle drei Felder in einer Anfrage. */
    suspend fun wappenSetzen(
        kennung: String,
        wacheId: String,
        zeichen: String?,
        farbe: Int?,
        fotoZeigen: Boolean,
    ) = netz.ohneAntwort(
        "/api/welt/wachen/${teil(wacheId)}/wappen?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("zeichen", zeichen?.let { JsonPrimitive(it) } ?: JsonNull)
            put("farbe", farbe?.let { JsonPrimitive(it) } ?: JsonNull)
            put("fotoZeigen", fotoZeigen)
        }.toString(),
    )

    /** Welche Werkstätten für dieses Fahrzeug infrage kommen — samt Preis und Dauer. */
    suspend fun werkstattwahl(kennung: String, fahrzeugId: String): WeltWerkstattwahl =
        netz.hole("/api/welt/fahrzeuge/${teil(fahrzeugId)}/werkstatt?kennung=${teil(kennung)}")

    /** Schickt das Fahrzeug zur Instandsetzung. */
    suspend fun zurWerkstatt(kennung: String, fahrzeugId: String, werkstattId: String) =
        netz.ohneAntwort(
            "/api/welt/fahrzeuge/${teil(fahrzeugId)}/werkstatt?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject { put("werkstattId", werkstattId) }.toString(),
        )

    /** Welche Lehrgänge diese Besatzung besuchen kann — mit Begründung, wo nicht. */
    suspend fun lehrgaenge(kennung: String, fahrzeugId: String): List<WeltLehrgang> =
        netz.hole("/api/welt/fahrzeuge/${teil(fahrzeugId)}/lehrgaenge?kennung=${teil(kennung)}")

    /** Schickt die Besatzung zum Lehrgang: Das Fahrzeug fährt zur Einrichtung. */
    suspend fun zumLehrgang(
        kennung: String,
        fahrzeugId: String,
        einrichtungId: String,
        lehrgangId: String,
    ) = netz.ohneAntwort(
        "/api/welt/fahrzeuge/${teil(fahrzeugId)}/lehrgang?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("einrichtungId", einrichtungId)
            put("lehrgangId", lehrgangId)
        }.toString(),
    )

    /**
     * Gibt einem Fahrzeug einen eigenen Funkrufnamen. Ein leerer Name ist die
     * Rücknahme: Dann bildet die Systematik ihn wieder.
     */
    suspend fun fahrzeugUmbenennen(kennung: String, fahrzeugId: String, name: String) =
        netz.ohneAntwort(
            "/api/welt/fahrzeuge/${teil(fahrzeugId)}/name?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject { put("name", name) }.toString(),
        )

    // --------------------------------------------------------- Großlage, Events

    /** Schickt Fahrzeuge in den Bereitstellungsraum der Großlage. */
    suspend fun bereitstellen(
        kennung: String,
        woche: Int,
        fahrzeugIds: List<String>,
    ): WeltBereitstellantwort = netz.hole(
        "/api/welt/grosslage/bereitstellen?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("woche", woche)
            put("fahrzeugIds", texte(fahrzeugIds))
        }.toString(),
    )

    /** Dasselbe für den Bereitstellungsraum eines Event-Einsatzes. */
    suspend fun eventBereitstellen(
        kennung: String,
        eventId: String,
        fahrzeugIds: List<String>,
    ): WeltBereitstellantwort = netz.hole(
        "/api/welt/events/${teil(eventId)}/bereitstellen?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject { put("fahrzeugIds", texte(fahrzeugIds)) }.toString(),
    )

    /** Die Großlage der Woche; `null`, solange keine angekündigt ist. */
    suspend fun grosslage(kennung: String): Weltgrosslage? =
        netz.hole("/api/welt/grosslage?kennung=${teil(kennung)}")

    /** Die Event-Einsätze der World: was läuft und was in den nächsten Tagen ansteht. */
    suspend fun events(kennung: String): List<WeltEvent> =
        netz.hole("/api/welt/events?kennung=${teil(kennung)}")

    /** Die drei Wochenziele samt eigenem Fortschritt. */
    suspend fun ziele(kennung: String): WeltZiele =
        netz.hole("/api/welt/ziele?kennung=${teil(kennung)}")

    // --------------------------------------------------------------------- Chat

    /** Der Chatverlauf beim Öffnen — gesendet wird über den Hub. */
    suspend fun chat(kennung: String): WeltChatstand =
        netz.hole("/api/welt/chat?kennung=${teil(kennung)}")

    /** Hakt die eigene Post als gelesen ab. */
    suspend fun chatGelesen(kennung: String): WeltAbgehakt =
        netz.hole("/api/welt/chat/gelesen?kennung=${teil(kennung)}", "POST")

    // ------------------------------------------------------- Kasse, Rangliste

    suspend fun kassenblatt(kennung: String): WeltKassenblatt =
        netz.hole("/api/welt/buchungen?kennung=${teil(kennung)}")

    /** Nimmt einen der Kredite auf — es geht immer nur einer zur Zeit. */
    suspend fun kreditAufnehmen(kennung: String, betrag: Int) = netz.ohneAntwort(
        "/api/welt/kredit?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject { put("betrag", betrag) }.toString(),
    )

    /** Zahlt außer der Reihe zurück — mehr als die Restschuld wird gedeckelt. */
    suspend fun kreditTilgen(kennung: String, betrag: Int) = netz.ohneAntwort(
        "/api/welt/kredit/tilgen?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject { put("betrag", betrag) }.toString(),
    )

    suspend fun rangliste(kennung: String): WeltRangliste =
        netz.hole("/api/welt/rangliste?kennung=${teil(kennung)}")

    // ---------------------------------------------------------------- Helfer

    private fun texte(werte: List<String>): JsonElement = JsonArray(werte.map { JsonPrimitive(it) })

    private fun stationenJson(stationen: List<WeltStreifenstation>): JsonElement =
        Netz.abgabe.encodeToJsonElement(ListSerializer(WeltStreifenstation.serializer()), stationen)

    private fun eckenJson(ecken: List<WeltEcke>): JsonElement =
        Netz.abgabe.encodeToJsonElement(ListSerializer(WeltEcke.serializer()), ecken)
}

/**
 * Was an einem Punkt geändert werden soll — jedes Feld, das `null` ist, bleibt, wie es
 * ist (siehe `Weltwege.poiAendern`).
 */
data class WeltPoiAenderung(
    val name: String? = null,
    val art: String? = null,
    val flaeche: Double? = null,
    val sichtbar: Boolean? = null,
    val aktiv: Boolean? = null,
    val gewicht: Int? = null,
    val nachtsBelegt: Boolean? = null,
    val organisationen: List<String>? = null,
    val farbe: Int? = null,
    /** -1 nimmt die eigene Zahl zurück und lässt wieder aus der Fläche rechnen. */
    val betroffene: Int? = null,
    /** Die Einsatzplan-Zeilen, eine je Zeile. Leerer Text löscht sie. */
    val hinweise: String? = null,
    val gelaende: List<WeltEcke>? = null,
    val gelaendeart: String? = null,
    val gelaendeartSetzen: Boolean = false,
    val strecke: Boolean? = null,
)

/** Wie in `Spielwege`: einmal kodieren statt an sechzig Stellen. */
private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
