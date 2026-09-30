package de.pagerspass.pagerspass.netz

import kotlinx.serialization.encodeToString
import java.net.URLEncoder

/**
 * Die Wege der Welt — übertragen aus dem World-Abschnitt von `web/src/api/rest.ts`.
 *
 * <b>Alle liegen unter `/api/welt` und tragen die Kennung als Abfrage</b>
 * (`?kennung=`), nicht im Pfad wie die Kontowege. Der Server schützt die ganze
 * Gruppe mit zwei Filtern: Premium (403 mit „nur mit aktivem Premium“) und die
 * bestätigte E-Mail-Adresse (403 mit dem Satz aus `EmailPflicht`). Beide Sätze
 * kommen als `fehler` und werden so angezeigt, wie der Server sie schreibt.
 *
 * <b>Die Rümpfe sind kleine Datenklassen</b> (siehe `Weltmodelle.kt`) und keine
 * von Hand gebauten Objekte: So steht jeder Rumpf einmal mit Feldnamen da, und
 * ein Tippfehler im Namen ist ein Kompilierfehler statt ein stilles `400`.
 */
class Weltwege(private val netz: Netz) {

    private fun k(kennung: String) = "kennung=${URLEncoder.encode(kennung, "UTF-8")}"
    private fun p(teil: String) = URLEncoder.encode(teil, "UTF-8").replace("+", "%20")
    private inline fun <reified T> json(wert: T): String = Netz.abgabe.encodeToString(wert)

    // ----------------------------------------------------------- Gründung

    suspend fun karte(kennung: String): List<Weltleitstelle> =
        netz.hole("/api/welt/karte?${k(kennung)}")

    suspend fun gruenden(kennung: String, name: String, lat: Double, lon: Double) =
        netz.ohneAntwort("/api/welt/leitstelle?${k(kennung)}", "POST", json(WeltGruendung(name, lat, lon)))

    suspend fun zweigstelleGruenden(kennung: String, name: String, lat: Double, lon: Double) =
        netz.ohneAntwort("/api/welt/zweigstelle?${k(kennung)}", "POST", json(WeltGruendung(name, lat, lon)))

    // --------------------------------------------------- Stand und Betrieb

    suspend fun stand(kennung: String): Weltstand = netz.hole("/api/welt/stand?${k(kennung)}")

    suspend fun betrieb(kennung: String): Weltbetrieb = netz.hole("/api/welt/betrieb?${k(kennung)}")

    suspend fun laufbahn(kennung: String): List<WeltLaufbahnstufe> =
        netz.hole("/api/welt/laufbahn?${k(kennung)}")

    suspend fun grosslage(kennung: String): Weltgrosslage? = netz.hole("/api/welt/grosslage?${k(kennung)}")

    suspend fun events(kennung: String): List<WeltEvent> = netz.hole("/api/welt/events?${k(kennung)}")

    suspend fun ziele(kennung: String): WeltZiele = netz.hole("/api/welt/ziele?${k(kennung)}")

    /** Derselbe Katalog wie im Rundenspiel — hier mit den Fähigkeiten. */
    suspend fun katalog(): WeltKatalog = netz.hole("/api/catalog")

    // ------------------------------------------------------------- Lagen

    suspend fun auswahl(kennung: String, lageId: String): WeltAuswahl =
        netz.hole("/api/welt/lagen/${p(lageId)}/auswahl?${k(kennung)}")

    suspend fun alarmieren(kennung: String, lageId: String, fahrzeugIds: List<String>, token: String) =
        netz.ohneAntwort(
            "/api/welt/lagen/${p(lageId)}/alarm?${k(kennung)}",
            "POST",
            json(WeltAlarm(fahrzeugIds, token)),
        )

    suspend fun zugAlarmieren(kennung: String, lageId: String, zugId: String, token: String) =
        netz.ohneAntwort(
            "/api/welt/lagen/${p(lageId)}/alarm?${k(kennung)}",
            "POST",
            json(WeltAlarm(emptyList(), token, zugId)),
        )

    suspend fun einruecken(kennung: String, lageId: String) =
        netz.ohneAntwort("/api/welt/lagen/${p(lageId)}/einruecken?${k(kennung)}", "POST")

    suspend fun freigeben(kennung: String, lageId: String) =
        netz.ohneAntwort("/api/welt/lagen/${p(lageId)}/freigeben?${k(kennung)}", "POST")

    suspend fun ortsname(kennung: String, lat: Double, lon: Double): WeltOrtsname =
        netz.hole("/api/welt/ortsname?${k(kennung)}&lat=$lat&lon=$lon")

    // ----------------------------------------------------- Großlage, Events

    suspend fun bereitstellen(kennung: String, woche: Int, fahrzeugIds: List<String>) =
        netz.ohneAntwort(
            "/api/welt/grosslage/bereitstellen?${k(kennung)}",
            "POST",
            json(WeltBereitstellung(woche, fahrzeugIds)),
        )

    suspend fun eventBereitstellen(kennung: String, eventId: String, fahrzeugIds: List<String>) =
        netz.ohneAntwort(
            "/api/welt/events/${p(eventId)}/bereitstellen?${k(kennung)}",
            "POST",
            json(WeltFahrzeugliste(fahrzeugIds)),
        )

    // ------------------------------------------------------------ Bauen

    suspend fun wacheBauen(kennung: String, name: String, art: String, lat: Double, lon: Double) =
        netz.ohneAntwort("/api/welt/wachen?${k(kennung)}", "POST", json(WeltWachenbau(name, art, lat, lon)))

    suspend fun fahrzeugKaufen(kennung: String, wacheId: String, vorlageId: String) =
        netz.ohneAntwort("/api/welt/fahrzeuge?${k(kennung)}", "POST", json(WeltFahrzeugkauf(wacheId, vorlageId)))

    // ------------------------------------------------------------ Wachen

    suspend fun wacheAbreissen(kennung: String, wacheId: String) =
        netz.ohneAntwort("/api/welt/wachen/${p(wacheId)}?${k(kennung)}", "DELETE")

    suspend fun wacheAusbauen(kennung: String, wacheId: String) =
        netz.ohneAntwort("/api/welt/wachen/${p(wacheId)}/ausbau?${k(kennung)}", "POST")

    suspend fun wacheUmbenennen(kennung: String, wacheId: String, name: String) =
        netz.ohneAntwort("/api/welt/wachen/${p(wacheId)}/name?${k(kennung)}", "POST", json(WeltName(name)))

    suspend fun wachenseite(kennung: String, wacheId: String): WeltWachenseite =
        netz.hole("/api/welt/wachen/${p(wacheId)}?${k(kennung)}")

    suspend fun wappenSetzen(kennung: String, wacheId: String, zeichen: String?, farbe: Int?, fotoZeigen: Boolean) =
        netz.ohneAntwort(
            "/api/welt/wachen/${p(wacheId)}/wappen?${k(kennung)}",
            "POST",
            json(WeltWappen(zeichen, farbe, fotoZeigen)),
        )

    suspend fun zuege(kennung: String): List<WeltZug> = netz.hole("/api/welt/zuege?${k(kennung)}")

    suspend fun zugAufstellen(kennung: String, wacheId: String, vorgabeId: String, name: String?) =
        netz.ohneAntwort("/api/welt/zuege?${k(kennung)}", "POST", json(WeltZugaufstellung(wacheId, vorgabeId, name)))

    suspend fun zugAufloesen(kennung: String, zugId: String) =
        netz.ohneAntwort("/api/welt/zuege/${p(zugId)}?${k(kennung)}", "DELETE")

    suspend fun zugFahrzeug(kennung: String, zugId: String, fahrzeugId: String, dazu: Boolean) =
        netz.ohneAntwort(
            "/api/welt/zuege/${p(zugId)}/fahrzeug?${k(kennung)}",
            "POST",
            json(WeltZugfahrzeug(fahrzeugId, dazu)),
        )

    // --------------------------------------------------------- Fahrzeuge

    suspend fun fahrzeugVerkaufen(kennung: String, id: String) =
        netz.ohneAntwort("/api/welt/fahrzeuge/${p(id)}?${k(kennung)}", "DELETE")

    suspend fun fahrzeugUmsetzen(kennung: String, id: String, zielWacheId: String) =
        netz.ohneAntwort("/api/welt/fahrzeuge/${p(id)}/umsetzen?${k(kennung)}", "POST", json(WeltUmsetzen(zielWacheId)))

    suspend fun fahrzeugUmbenennen(kennung: String, id: String, name: String) =
        netz.ohneAntwort("/api/welt/fahrzeuge/${p(id)}/name?${k(kennung)}", "POST", json(WeltName(name)))

    suspend fun anfahrtAbbrechen(kennung: String, id: String) =
        netz.ohneAntwort("/api/welt/fahrzeuge/${p(id)}/abbrechen?${k(kennung)}", "POST")

    suspend fun werkstattwahl(kennung: String, id: String): WeltWerkstattwahl =
        netz.hole("/api/welt/fahrzeuge/${p(id)}/werkstatt?${k(kennung)}")

    suspend fun zurWerkstatt(kennung: String, id: String, werkstattId: String) =
        netz.ohneAntwort(
            "/api/welt/fahrzeuge/${p(id)}/werkstatt?${k(kennung)}",
            "POST",
            json(WeltWerkstattziel(werkstattId)),
        )

    suspend fun lehrgaenge(kennung: String, id: String): List<WeltLehrgang> =
        netz.hole("/api/welt/fahrzeuge/${p(id)}/lehrgaenge?${k(kennung)}")

    suspend fun zumLehrgang(kennung: String, id: String, einrichtungId: String, lehrgangId: String) =
        netz.ohneAntwort(
            "/api/welt/fahrzeuge/${p(id)}/lehrgang?${k(kennung)}",
            "POST",
            json(WeltLehrgangsziel(einrichtungId, lehrgangId)),
        )

    suspend fun streifenorte(kennung: String): List<WeltStreifenstation> =
        netz.hole("/api/welt/streifenorte?${k(kennung)}")

    suspend fun streifeSetzen(kennung: String, id: String, stationen: List<WeltStreifenstation>) =
        netz.ohneAntwort("/api/welt/fahrzeuge/${p(id)}/streife?${k(kennung)}", "POST", json(WeltStreife(stationen)))

    suspend fun streifenrouten(kennung: String): List<WeltStreifenroute> =
        netz.hole("/api/welt/streifenrouten?${k(kennung)}")

    suspend fun streifenrouteSichern(kennung: String, name: String, stationen: List<WeltStreifenstation>) =
        netz.ohneAntwort("/api/welt/streifenrouten?${k(kennung)}", "POST", json(WeltRoutensicherung(name, stationen)))

    suspend fun streifenrouteEntfernen(kennung: String, id: String) =
        netz.ohneAntwort("/api/welt/streifenrouten/${p(id)}?${k(kennung)}", "DELETE")

    suspend fun streifenrouteAuflegen(kennung: String, fahrzeugId: String, routeId: String) =
        netz.ohneAntwort("/api/welt/fahrzeuge/${p(fahrzeugId)}/streife/route/${p(routeId)}?${k(kennung)}", "POST")

    // ------------------------------------------------------ Eigene Punkte

    suspend fun pois(kennung: String): List<Weltpoi> = netz.hole("/api/welt/pois?${k(kennung)}")

    internal suspend fun poiSetzen(kennung: String, punkt: WeltPoiNeu) =
        netz.ohneAntwort("/api/welt/pois?${k(kennung)}", "POST", json(punkt))

    suspend fun poiAendern(kennung: String, id: String, aenderung: WeltPoiAenderung) =
        netz.ohneAntwort("/api/welt/pois/${p(id)}?${k(kennung)}", "PATCH", json(aenderung))

    suspend fun poiEntfernen(kennung: String, id: String) =
        netz.ohneAntwort("/api/welt/pois/${p(id)}?${k(kennung)}", "DELETE")

    // --------------------------------------------------------- Leihmarkt

    suspend fun leihen(kennung: String): WeltLeihstand = netz.hole("/api/welt/leihen?${k(kennung)}")

    suspend fun leiheAnbieten(kennung: String, fahrzeugId: String, tage: Int, preis: Int) =
        netz.ohneAntwort("/api/welt/leihen?${k(kennung)}", "POST", json(WeltLeiheAnbieten(fahrzeugId, tage, preis)))

    suspend fun leiheMieten(kennung: String, id: String, zielWacheId: String) =
        netz.ohneAntwort("/api/welt/leihen/${p(id)}/mieten?${k(kennung)}", "POST", json(WeltLeiheMieten(zielWacheId)))

    suspend fun leiheZuruecknehmen(kennung: String, id: String) =
        netz.ohneAntwort("/api/welt/leihen/${p(id)}?${k(kennung)}", "DELETE")

    suspend fun leiheZurueckgeben(kennung: String, fahrzeugId: String) =
        netz.ohneAntwort("/api/welt/leihen/${p(fahrzeugId)}/zurueckgeben?${k(kennung)}", "POST")

    suspend fun gesuchAufgeben(kennung: String, kategorie: String, zielWacheId: String, tage: Int, preis: Int) =
        netz.ohneAntwort(
            "/api/welt/gesuche?${k(kennung)}",
            "POST",
            json(WeltGesuchAufgeben(kategorie, zielWacheId, tage, preis)),
        )

    suspend fun gesuchZurueckziehen(kennung: String, id: String) =
        netz.ohneAntwort("/api/welt/gesuche/${p(id)}?${k(kennung)}", "DELETE")

    suspend fun gesuchBedienen(kennung: String, id: String, fahrzeugId: String) =
        netz.ohneAntwort("/api/welt/gesuche/${p(id)}/bedienen?${k(kennung)}", "POST", json(WeltGesuchBedienen(fahrzeugId)))

    // ------------------------------------------------------------- Kasse

    suspend fun kassenblatt(kennung: String): WeltKassenblatt = netz.hole("/api/welt/buchungen?${k(kennung)}")

    suspend fun kreditAufnehmen(kennung: String, betrag: Long) =
        netz.ohneAntwort("/api/welt/kredit?${k(kennung)}", "POST", json(WeltBetrag(betrag)))

    suspend fun kreditTilgen(kennung: String, betrag: Long) =
        netz.ohneAntwort("/api/welt/kredit/tilgen?${k(kennung)}", "POST", json(WeltBetrag(betrag)))

    suspend fun rangliste(kennung: String): WeltRangliste = netz.hole("/api/welt/rangliste?${k(kennung)}")

    // ------------------------------------------------------ Einstellungen

    suspend fun einstellung(kennung: String): WeltEinstellung = netz.hole("/api/welt/einstellungen?${k(kennung)}")

    suspend fun einstellungSetzen(kennung: String, gangart: String, dichte: Int, nachschub: Int, arbeitszeit: Int) =
        netz.ohneAntwort(
            "/api/welt/einstellungen?${k(kennung)}",
            "PUT",
            json(WeltEinstellungSetzen(gangart, dichte, nachschub, arbeitszeit)),
        )

    // -------------------------------------------------------------- Chat

    suspend fun chat(kennung: String): WeltChatstand = netz.hole("/api/welt/chat?${k(kennung)}")

    suspend fun chatGelesen(kennung: String) =
        netz.ohneAntwort("/api/welt/chat/gelesen?${k(kennung)}", "POST")
}
