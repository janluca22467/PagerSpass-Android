package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.Begleitergeraete
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.wert
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Die Hub-Befehle der Runde — jeder mit der **vollen** Argumentliste aus
 * `GameHub.cs`.
 *
 * <b>Warum „voll" hier fett steht.</b> SignalR bindet rein nach Stelle; ein
 * C#-Standardwert hilft über die Leitung nicht. Fehlt ein Argument, verwirft
 * der Hub den ganzen Aufruf still („Invocation provides 3 argument(s) but
 * target expects 5"), und die Oberfläche sieht davon nichts als einen Knopf,
 * der nichts tut. Deshalb steht jedes `null` hier ausgeschrieben.
 *
 * <b>Warum als Erweiterungen und nicht in `Runde`.</b> Die Runde hält Zustand
 * und Verbindung; diese Befehle sind reine Zurufe — was daraus wird, kommt als
 * `RoomState` zurück. Getrennt bleibt `Runde.kt` lesbar, und wer einen Befehl
 * sucht, sucht ihn hier.
 */

// ----------------------------------------------------------------- Hilfen

private fun alsText(wert: String?): JsonElement = wert?.let { JsonPrimitive(it) } ?: JsonNull

private fun alsZahl(wert: Int?): JsonElement = wert?.let { JsonPrimitive(it) } ?: JsonNull

private fun alsKomma(wert: Double?): JsonElement = wert?.let { JsonPrimitive(it) } ?: JsonNull

private fun alsSchalter(wert: Boolean?): JsonElement = wert?.let { JsonPrimitive(it) } ?: JsonNull

private fun alsTexte(werte: List<String>?): JsonElement =
    werte?.let { liste -> buildJsonArray { liste.forEach { add(JsonPrimitive(it)) } } } ?: JsonNull

// ------------------------------------------------------ Sitzung und Platz

/** Ob am eigenen Platz gerade ein Handy als Funkbegleiter hängt. */
suspend fun Runde.begleiterGekoppelt(): Boolean {
    val antwort = fragen("BegleiterGekoppelt")
    val ja = (antwort as? JsonPrimitive)?.content == "true"
    standAendern { it.copy(begleiterGekoppelt = ja) }
    return ja
}

/** Den eigenen Gerätestand an die gekoppelten Handys nachmelden — still im Fehlerfall. */
fun Runde.geraeteMelden(geraete: Begleitergeraete) =
    senden("GeraeteMelden", Netz.abgabe.encodeToJsonElement(Begleitergeraete.serializer(), geraete))

/**
 * Die Einwilligung am Platz nachtragen lassen — der Aufruf sagt nur „sieh noch
 * mal nach"; die Antwort kommt aus der Datenbank des Servers.
 */
suspend fun Runde.streamerfreigabeNachtragen(): Boolean =
    (fragen("StreamerfreigabeNachtragen") as? JsonPrimitive)?.content == "true"

/** Einen Bot-Platz im laufenden Dienst übernehmen — Fahrzeug, Status und Einsatz bleiben. */
fun Runde.botUebernehmen(botPlayerId: String) = senden("BotUebernehmen", wert(botPlayerId))

/** Einem Bot ein anderes Fahrzeug geben (Besatzungen einteilen). */
fun Runde.botZuweisen(botPlayerId: String, vehicleTemplateId: String) =
    senden("AssignBot", wert(botPlayerId), wert(vehicleTemplateId))

/** Einen Mitspieler aus dem Raum werfen — nur die Leitstelle. */
fun Runde.spielerKicken(zielPlayerId: String) = senden("KickPlayer", wert(zielPlayerId))

/** Um einen Leitstellenplatz bitten — der Host entscheidet. */
fun Runde.leitstellenplatzAnfragen() = senden("LeitstellenplatzAnfragen")

fun Runde.platzanfrageEntscheiden(zielPlayerId: String, annehmen: Boolean) =
    senden("PlatzanfrageEntscheiden", wert(zielPlayerId), wert(annehmen))

fun Runde.platzanfrageZuruecknehmen() = senden("PlatzanfrageZuruecknehmen")

/** Als Zuschauer um einen zusätzlichen Platz in der vollen Runde bitten (Premium). */
fun Runde.beitrittAnfragen() = senden("BeitrittAnfragen")

fun Runde.beitrittEntscheiden(zielPlayerId: String, annehmen: Boolean) =
    senden("BeitrittEntscheiden", wert(zielPlayerId), wert(annehmen))

fun Runde.beitrittsanfrageZuruecknehmen() = senden("BeitrittsanfrageZuruecknehmen")

/**
 * Einen Funkrufnamen von Hand vergeben.
 *
 * @param kurzname Die Kurzform fürs Tableau; `null` heißt „das Kennzeichen am
 *   Ende des Rufnamens".
 */
fun Runde.funkrufnameAendern(vehicleId: String, funkrufname: String, kurzname: String?) =
    senden("FunkrufnameAendern", wert(vehicleId), wert(funkrufname), alsText(kurzname))

fun Runde.funkrufnameZuruecksetzen(vehicleId: String) =
    senden("FunkrufnameZuruecksetzen", wert(vehicleId))

/** Ein Fahrzeug auf eine andere Wache stellen — nur in der Lobby. */
fun Runde.wacheZuweisen(vehicleId: String, kennung: String) =
    senden("WacheZuweisen", wert(vehicleId), wert(kennung))

/** Die eigene Live-Meldung an- oder abschalten. */
fun Runde.liveSetzen(an: Boolean) = senden("Live", wert(an))

/** Ob die eigenen getippten Funksprüche vorgelesen werden. */
fun Runde.funkVorlesen(an: Boolean) = senden("FunkVorlesen", wert(an))

/** Ob andere Plätze einen per Einzelruf anrufen dürfen. */
fun Runde.einzelrufZulassen(zulassen: Boolean) = senden("EinzelrufZulassenSetzen", wert(zulassen))

// ------------------------------------------------------ Rundeneinstellungen

/**
 * Eine Änderung an den Rundeneinstellungen — was nicht gesetzt ist, bleibt.
 *
 * Die Felder stehen in der Reihenfolge von `GameHub.UpdateSettings`; `null`
 * heißt über die Leitung „nicht mitgeschickt". Listen und Wörterbücher ersetzen
 * das Ganze: Eine leere Funkgruppenliste ist die Ansage „wieder ein Kanal für
 * alle", eine leere Wachenliste „alle, die der Kreis hergibt".
 */
data class Einstellungsaenderung(
    val mode: String? = null,
    val ort: String? = null,
    val leitstelle: String? = null,
    val einsatzIntervallSekunden: Int? = null,
    val organisationen: List<String>? = null,
    val botTempo: String? = null,
    val hiOrgs: List<String>? = null,
    val freischaltungenIgnorieren: Boolean? = null,
    val oeffentlich: Boolean? = null,
    val botGespraechigkeit: String? = null,
    val botFunkAktiv: Boolean? = null,
    val zeitmodus: String? = null,
    val telefonischeLeitstelle: Boolean? = null,
    val stoerungshaeufigkeit: String? = null,
    val tagesalarmstaerke: Boolean? = null,
    val loeschwasser: Boolean? = null,
    val sonderobjekte: Boolean? = null,
    val wiederherstellung: Boolean? = null,
    val gefahrgutlagen: Boolean? = null,
    val jahreszeit: String? = null,
    val silvester: Boolean? = null,
    val verlegungsfahrten: Boolean? = null,
    val suchlagen: Boolean? = null,
    val einsatzarbeit: Boolean? = null,
    val vegetationsbraende: Boolean? = null,
    val arbeitsfunk: String? = null,
    val einsatzende: String? = null,
    val einsatzleitung: Boolean? = null,
    val maxSpieler: Int? = null,
    val wachen: List<de.pagerspass.pagerspass.netz.Wachenwahl>? = null,
    val rufnamenpraefixe: Map<String, String>? = null,
    // `kennzahlen` setzt der Leitstellenbau, nicht die Lobby — die Stelle geht
    // deshalb immer als `null` hinaus (siehe `einstellungen`).
    val wachennummerStellen: Int? = null,
    val laufnummerStellen: Int? = null,
    val funkgruppen: List<Funkgruppeneingabe>? = null,
    val wachnummern: Map<String, Int>? = null,
    val funkverstossSchwelle: Int? = null,
    val einsatzdichte: String? = null,
    val streamermodus: Boolean? = null,
    val streamerplattform: String? = null,
    val streamerkanal: String? = null,
    val streameraufzeichnung: Boolean? = null,
    val kiFunkAktiv: Boolean? = null,
    /** Der leere Text setzt auf den Grundkatalog zurück — `null` hieße „nicht mitgeschickt". */
    val stichwortsetId: String? = null,
)

/**
 * Eine Zeile der Funkgruppen-Maske — Spiegel von `FunkgruppeEingabe`. Eine leere
 * `id` heißt „neue Zeile, vergib eine".
 */
data class Funkgruppeneingabe(
    val id: String = "",
    val nummer: String = "",
    val name: String,
    val organisationen: List<String> = emptyList(),
    val hiOrgs: List<String> = emptyList(),
    val fuehrung: Boolean = false,
)

/**
 * Die Rundeneinstellungen ändern — `UpdateSettings` mit **allen 44** Stellen.
 *
 * Die Reihenfolge ist die von `GameHub.UpdateSettings` und nachgezählt:
 * mode, ort, leitstelle, einsatzIntervallSekunden, organisationen, botTempo,
 * hiOrgs, freischaltungenIgnorieren, oeffentlich, botGespraechigkeit,
 * botFunkAktiv, zeitmodus, telefonischeLeitstelle, stoerungshaeufigkeit,
 * tagesalarmstaerke, loeschwasser, sonderobjekte, wiederherstellung,
 * gefahrgutlagen, jahreszeit, silvester, verlegungsfahrten, suchlagen,
 * einsatzarbeit, vegetationsbraende, arbeitsfunk, einsatzende, einsatzleitung,
 * maxSpieler, wachen, rufnamenpraefixe, kennzahlen (immer `null`),
 * wachennummerStellen, laufnummerStellen, funkgruppen, wachnummern,
 * funkverstossSchwelle, einsatzdichte, streamermodus, streamerplattform,
 * streamerkanal, streameraufzeichnung, kiFunkAktiv, stichwortsetId.
 *
 * Fehlt eine Stelle, lässt sich in der Lobby **keine einzige** Einstellung
 * mehr verstellen — der Hub verwirft den Aufruf, und die Oberfläche sieht nur
 * ein stillschweigend unverändertes Kästchen.
 */
fun Runde.einstellungen(a: Einstellungsaenderung) = senden(
    "UpdateSettings",
    alsText(a.mode),
    alsText(a.ort),
    alsText(a.leitstelle),
    alsZahl(a.einsatzIntervallSekunden),
    alsTexte(a.organisationen),
    alsText(a.botTempo),
    alsTexte(a.hiOrgs),
    alsSchalter(a.freischaltungenIgnorieren),
    alsSchalter(a.oeffentlich),
    alsText(a.botGespraechigkeit),
    alsSchalter(a.botFunkAktiv),
    alsText(a.zeitmodus),
    alsSchalter(a.telefonischeLeitstelle),
    alsText(a.stoerungshaeufigkeit),
    alsSchalter(a.tagesalarmstaerke),
    alsSchalter(a.loeschwasser),
    alsSchalter(a.sonderobjekte),
    alsSchalter(a.wiederherstellung),
    alsSchalter(a.gefahrgutlagen),
    alsText(a.jahreszeit),
    alsSchalter(a.silvester),
    alsSchalter(a.verlegungsfahrten),
    alsSchalter(a.suchlagen),
    alsSchalter(a.einsatzarbeit),
    alsSchalter(a.vegetationsbraende),
    alsText(a.arbeitsfunk),
    alsText(a.einsatzende),
    alsSchalter(a.einsatzleitung),
    alsZahl(a.maxSpieler),
    a.wachen?.let { liste ->
        buildJsonArray {
            liste.forEach { w ->
                add(
                    buildJsonObject {
                        put("kennung", JsonPrimitive(w.kennung))
                        put("name", alsText(w.name))
                        put("zugnummer", JsonPrimitive(w.zugnummer))
                        put("lat", alsKomma(w.lat))
                        put("lon", alsKomma(w.lon))
                        put("organisation", alsText(w.organisation))
                        put("traeger", alsText(w.traeger))
                    },
                )
            }
        }
    } ?: JsonNull,
    a.rufnamenpraefixe?.let { woerter ->
        buildJsonObject { woerter.forEach { (schluessel, wort) -> put(schluessel, JsonPrimitive(wort)) } }
    } ?: JsonNull,
    JsonNull, // kennzahlen — siehe oben
    alsZahl(a.wachennummerStellen),
    alsZahl(a.laufnummerStellen),
    a.funkgruppen?.let { liste ->
        buildJsonArray {
            liste.forEach { g ->
                add(
                    buildJsonObject {
                        put("id", JsonPrimitive(g.id))
                        put("nummer", JsonPrimitive(g.nummer))
                        put("name", JsonPrimitive(g.name))
                        put("organisationen", alsTexte(g.organisationen))
                        put("hiOrgs", alsTexte(g.hiOrgs))
                        put("fuehrung", JsonPrimitive(g.fuehrung))
                    },
                )
            }
        }
    } ?: JsonNull,
    a.wachnummern?.let { nummern ->
        buildJsonObject { nummern.forEach { (kennung, nr) -> put(kennung, JsonPrimitive(nr)) } }
    } ?: JsonNull,
    alsZahl(a.funkverstossSchwelle),
    alsText(a.einsatzdichte),
    alsSchalter(a.streamermodus),
    alsText(a.streamerplattform),
    alsText(a.streamerkanal),
    alsSchalter(a.streameraufzeichnung),
    alsSchalter(a.kiFunkAktiv),
    alsText(a.stichwortsetId),
)

// ------------------------------------------------------------- Leitstelle

/** In der freien Vergabe: eine Lage auswürfeln lassen. */
fun Runde.einsatzWuerfeln() = senden("GenerateIncident")

/**
 * Einen Einsatz umstufen — alle sieben Stellen von `UpdateIncident`. `null`
 * heißt jeweils „nicht anfassen".
 */
fun Runde.einsatzAktualisieren(
    incidentId: String,
    stichwort: String?,
    stichwortText: String?,
    meldebild: String?,
    prioritaet: Int?,
    empfohleneFahrzeuge: Int?,
    empfohleneFaehigkeiten: List<String>?,
) = senden(
    "UpdateIncident",
    wert(incidentId),
    alsText(stichwort),
    alsText(stichwortText),
    alsText(meldebild),
    alsZahl(prioritaet),
    alsZahl(empfohleneFahrzeuge),
    alsTexte(empfohleneFaehigkeiten),
)

/**
 * Wie lange jedes Fahrzeug zu dieser Lage bräuchte — Fahrzeug-Id → Sekunden.
 * Für die Sortierung im Alarmdialog; leer bei toter Leitung.
 */
suspend fun Runde.eintreffzeiten(incidentId: String): Map<String, Int> {
    val antwort = fragen("Eintreffzeiten", wert(incidentId)) as? JsonObject ?: return emptyMap()
    return antwort.mapNotNull { (id, sek) ->
        (sek as? JsonPrimitive)?.content?.toDoubleOrNull()?.let { id to it.toInt() }
    }.toMap()
}

/**
 * Den Abrollbehälter am WLF wechseln. Leer heißt „absetzen" — der Hub nimmt
 * dafür einen leeren Text, weil sich `null` über die Leitung nicht von „nicht
 * mitgeschickt" unterscheidet.
 */
fun Runde.abrollbehaelterWechseln(vehicleId: String, abTemplateId: String?) =
    senden("AbrollbehaelterWechseln", wert(vehicleId), wert(abTemplateId.orEmpty()))

/** Fahrzeuge von einem Einsatz zurückrufen — mit Grund, wenn es einen gibt. */
fun Runde.fahrzeugeZurueckrufen(incidentId: String, vehicleIds: List<String>, grund: String?) =
    senden("FahrzeugeZurueckrufen", wert(incidentId), alsTexte(vehicleIds), alsText(grund))

/** Eine Streife losschicken oder zurückholen. */
fun Runde.streifeSchicken(vehicleId: String, an: Boolean) =
    senden("StreifeSchicken", wert(vehicleId), wert(an))

fun Runde.zielklinikZuweisen(vehicleId: String, klinikId: String) =
    senden("ZielklinikZuweisen", wert(vehicleId), wert(klinikId))

/** Einem Fahrzeug einen Suchabschnitt zuteilen (0…7). */
fun Runde.suchabschnittZuteilen(vehicleId: String, abschnitt: Int) =
    senden("SuchabschnittZuteilen", wert(vehicleId), wert(abschnitt))

fun Runde.aufgabeZuteilen(vehicleId: String, nummer: Int) =
    senden("AufgabeZuteilen", wert(vehicleId), wert(nummer))

/** Die Eigenfeststellung einer Streife zum Einsatz machen. */
fun Runde.feststellungUebernehmen(feststellungId: String) =
    senden("FeststellungUebernehmen", wert(feststellungId))

fun Runde.feststellungVerwerfen(feststellungId: String) =
    senden("FeststellungVerwerfen", wert(feststellungId))

/** Die Bevölkerung warnen — an alle im Raum, folgenlos fürs Spiel. */
fun Runde.bevoelkerungWarnen(text: String) {
    if (text.isBlank()) return
    senden("BevoelkerungWarnen", wert(text.trim()))
}

/** Den Leitstellentisch einem Mitspieler anbieten. */
fun Runde.leitstelleUebergeben(zielPlayerId: String) =
    senden("LeitstelleUebergeben", wert(zielPlayerId))

/** Das Übergabeangebot annehmen oder ablehnen. */
fun Runde.uebergabeAntworten(annehmen: Boolean) = senden("UebergabeAntworten", wert(annehmen))

/** Die verwaiste Leitstelle übernehmen. */
fun Runde.leitstelleUebernehmen() = senden("LeitstelleUebernehmen")

/** Eine eigene Alarm- und Ausrückeordnung für die freie Vergabe speichern. */
fun Runde.aaoVorlageSpeichern(name: String, empfohleneFahrzeuge: Int, faehigkeiten: List<String>) =
    senden("SaveAaoVorlage", wert(name), wert(empfohleneFahrzeuge), alsTexte(faehigkeiten))

fun Runde.aaoVorlageLoeschen(name: String) = senden("DeleteAaoVorlage", wert(name))

// --------------------------------------------------------------- Funkgruppen

/**
 * Ein Fahrzeug auf eine andere Funkgruppe schalten. `null` heißt zurück auf die
 * Stammgruppe — über die Leitung als leerer Text.
 */
fun Runde.funkgruppeZuweisen(vehicleId: String, gruppeId: String?) =
    senden("FunkgruppeZuweisen", wert(vehicleId), wert(gruppeId.orEmpty()))

/**
 * Welche Gruppen dieser Platz mithört und auf welcher er sendet. `sendegruppe`
 * `null` heißt: der eine Kanal — über die Leitung als leerer Text.
 */
fun Runde.funkgruppenPlatz(gruppen: List<String>, sendegruppe: String?) =
    senden("FunkgruppenPlatz", alsTexte(gruppen), wert(sendegruppe.orEmpty()))

/** Eine dynamische DMO-Gruppe an einer Einsatzstelle öffnen. */
fun Runde.einsatzfunkgruppeOeffnen(incidentId: String, name: String) =
    senden("EinsatzfunkgruppeOeffnen", wert(incidentId), wert(name))

fun Runde.einsatzfunkgruppeSchliessen(gruppeId: String) =
    senden("EinsatzfunkgruppeSchliessen", wert(gruppeId))

// ------------------------------------------------------------ Notruftelefon

/** Den Anrufer orten lassen — das Ergebnis kommt nach der Wartezeit am Anruf. */
fun Runde.anrufOrten(anrufId: String) = senden("AnrufOrten", wert(anrufId))

/** Einen abgeschlossenen Anruf nachträglich orten — aus dem Anrufjournal. */
fun Runde.journalOrten(anrufId: String) = senden("JournalOrten", wert(anrufId))

/** Einen Anruf einem laufenden Einsatz zuordnen — kein zweiter Alarm zur selben Lage. */
fun Runde.anrufZuordnen(anrufId: String, incidentId: String) =
    senden("AnrufZuordnen", wert(anrufId), wert(incidentId))

/**
 * Eine freie Frage an den Anrufer — getippt oder gesprochen.
 *
 * @param kandidaten Straßen, die zur Frage passen könnten (die Ortsabfrage des Webs).
 */
fun Runde.anrufFrageFrei(anrufId: String, text: String, kandidaten: List<String>? = null) {
    if (text.isBlank()) return
    senden("AnrufFrage", wert(anrufId), JsonNull, wert(text.trim()), alsTexte(kandidaten))
}

/**
 * Das Gespräch beenden. `abbrechen = true` legt auf, bevor die Adresse
 * feststeht — der Anruf landet im Journal, wo er sich noch orten lässt.
 */
fun Runde.anrufBeenden(anrufId: String, abbrechen: Boolean) =
    senden("AnrufBeenden", wert(anrufId), wert(abbrechen))

// --------------------------------------------------------------- Einzelruf

/** Einen Einzelruf beginnen. `null` ruft die Leitstelle. */
fun Runde.einzelrufStarten(zielVehicleId: String?) = senden("EinzelrufStarten", alsText(zielVehicleId))

fun Runde.einzelrufAnnehmen(rufId: String) = senden("EinzelrufAnnehmen", wert(rufId))

fun Runde.einzelrufAbweisen(rufId: String) = senden("EinzelrufAbweisen", wert(rufId))

fun Runde.einzelrufBeenden(rufId: String) = senden("EinzelrufBeenden", wert(rufId))

/** Eine Zeile im Einzelruf mit einer Bot-Besatzung. */
fun Runde.einzelrufSagen(rufId: String, text: String) {
    if (text.isBlank()) return
    senden("EinzelrufSagen", wert(rufId), wert(text.trim()))
}

// ---------------------------------------------------------------- Fahrzeug

/** Die Notruftaste am Handfunkgerät — Vorrang auf der Gruppe, ohne Rückfrage. */
fun Runde.notruf() = senden("Notruf")

/** Nachfordern — die Leitstelle liest den Text. */
fun Runde.nachfordern(text: String) {
    if (text.isBlank()) return
    senden("RequestSupport", wert(text.trim()))
}

/**
 * Aus einer Eigenfeststellung einen Streifeneinsatz anlegen — alle zehn Stellen
 * von `StreifeneinsatzAnlegen`.
 */
fun Runde.streifeneinsatzAnlegen(
    stichwort: String,
    stichwortText: String,
    meldebild: String,
    adresse: String,
    ortsteil: String?,
    prioritaet: Int,
    lat: Double?,
    lon: Double?,
    empfohleneFahrzeuge: Int? = null,
    empfohleneFaehigkeiten: List<String>? = null,
) = senden(
    "StreifeneinsatzAnlegen",
    wert(stichwort),
    wert(stichwortText),
    wert(meldebild),
    wert(adresse),
    alsText(ortsteil),
    wert(prioritaet),
    alsKomma(lat),
    alsKomma(lon),
    alsZahl(empfohleneFahrzeuge),
    alsTexte(empfohleneFaehigkeiten),
)

/** An der Entnahmestelle Wasser aufnehmen. */
fun Runde.wasserAufnehmen() = senden("WasserAufnehmen")

/** Eine Aufgabe an der Einsatzstelle übernehmen — `null` gibt sie wieder ab. */
fun Runde.aufgabeUebernehmen(nummer: Int?) = senden("AufgabeUebernehmen", alsZahl(nummer))

fun Runde.bereitstellungsraumFestlegen(incidentId: String) =
    senden("BereitstellungsraumFestlegen", wert(incidentId))

fun Runde.bereitstellungSetzen(incidentId: String, vehicleId: String, halten: Boolean) =
    senden("BereitstellungSetzen", wert(incidentId), wert(vehicleId), wert(halten))

fun Runde.landeplatzFestlegen(incidentId: String) = senden("LandeplatzFestlegen", wert(incidentId))

fun Runde.landeplatzAusleuchten(incidentId: String) = senden("LandeplatzAusleuchten", wert(incidentId))

// -------------------------------------------------------- Patientensimulation

/** Ein Vitalzeichen messen — `was` ist der Name aus `Vitalzeichen` am Server. */
fun Runde.patientMessen(incidentId: String, patientId: String, was: String) =
    senden("PatientMessen", wert(incidentId), wert(patientId), wert(was))

/** Ein Befundschema erheben (xABCDE, SAMPLER, …). */
fun Runde.patientSchema(incidentId: String, patientId: String, schema: String) =
    senden("PatientSchema", wert(incidentId), wert(patientId), wert(schema))

fun Runde.patientMassnahme(incidentId: String, patientId: String, massnahmeId: String) =
    senden("PatientMassnahme", wert(incidentId), wert(patientId), wert(massnahmeId))

fun Runde.patientVerdacht(incidentId: String, patientId: String, text: String) =
    senden("PatientVerdacht", wert(incidentId), wert(patientId), wert(text))

/** Die Diagnose des Notarztes — `null` nimmt sie zurück. */
fun Runde.patientDiagnose(incidentId: String, patientId: String, text: String?) =
    senden("PatientDiagnose", wert(incidentId), wert(patientId), alsText(text))
