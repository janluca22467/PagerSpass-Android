package de.pagerspass.pagerspass.ansichten

import de.pagerspass.pagerspass.mobil.Tonstand
import de.pagerspass.pagerspass.netz.AaoVorlagenzeile
import de.pagerspass.pagerspass.netz.Aaosicherung

/**
 * Was der Leitstellentisch tun kann — gebündelt wie die `LobbyGriffe`, damit der
 * Rahmen eine Zeile hat und nicht sechzig.
 *
 * <b>Jeder Griff ist ein Zuruf.</b> Was daraus wird, kommt als Raumzustand
 * zurück; die Ansicht schreibt nie selbst fort. Ausnahmen sind die drei Fragen,
 * die eine Antwort brauchen (Alarmvorschlag, Eintreffzeiten, die gesprochene
 * Alarmmeldung) — sie sind `suspend`.
 *
 * Die Vorgaben tun nichts, damit die Seite sich auch ohne Rahmen zeichnen lässt.
 */
data class LeitstellenGriffe(
    // ------------------------------------------------------------ Kopf
    val wuerfeln: () -> Unit = {},
    val warnen: (text: String) -> Unit = {},
    val dienstende: () -> Unit = {},
    /** Den Raum verlassen — der Leitstellenplatz wird frei. */
    val verlassen: () -> Unit = {},
    val hilfe: () -> Unit = {},
    val tonSetzen: (Tonstand) -> Unit = {},
    val einzelrufZulassen: (Boolean) -> Unit = {},
    val ausbildungUeberspringen: () -> Unit = {},
    /** Den QR-Zugang für ein zweites Handy erzeugen (Premium). */
    val begleiterErzeugen: () -> Unit = {},
    /** Funk und Melder hier ausblenden, solange das Handy gekoppelt ist. */
    val begleiterAuslagern: (Boolean) -> Unit = {},
    /** Beim Server nachfragen, ob ein Begleiter hängt. */
    val begleiterPruefen: () -> Unit = {},
    // ---------------------------------------------------------- Besatzung
    val leitstelleUebergeben: (playerId: String) -> Unit = {},
    val kicken: (playerId: String) -> Unit = {},
    val botVersetzen: (botId: String, vorlageId: String) -> Unit = { _, _ -> },
    val botEntfernen: (botId: String) -> Unit = {},
    val botHinzufuegen: (vorlageId: String, anzahl: Int) -> Unit = { _, _ -> },
    // ------------------------------------------------------------ Bänder
    val sprechwunsch: (vehicleId: String) -> Unit = {},
    val anrufAnnehmen: (anrufId: String) -> Unit = {},
    val anrufAbweisen: (anrufId: String) -> Unit = {},
    val feststellungUebernehmen: (id: String) -> Unit = {},
    val feststellungVerwerfen: (id: String) -> Unit = {},
    // ------------------------------------------------------------ Einsatz
    val einsatzAnlegen: (Einsatzbogen) -> Unit = {},
    /** Umstufen — alle sieben Stellen von `UpdateIncident`, `null` heißt „nicht anfassen". */
    val einsatzAendern: (
        incidentId: String,
        stichwort: String?,
        stichwortText: String?,
        meldebild: String?,
        prioritaet: Int?,
        empfohleneFahrzeuge: Int?,
        empfohleneFaehigkeiten: List<String>?,
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    val einsatzSchliessen: (incidentId: String) -> Unit = {},
    val zurueckrufen: (incidentId: String, vehicleIds: List<String>) -> Unit = { _, _ -> },
    val zielklinik: (vehicleId: String, klinikId: String) -> Unit = { _, _ -> },
    val suchabschnitt: (vehicleId: String, abschnitt: Int) -> Unit = { _, _ -> },
    val aufgabe: (vehicleId: String, nummer: Int) -> Unit = { _, _ -> },
    val anrufZuordnen: (anrufId: String, incidentId: String) -> Unit = { _, _ -> },
    val vorschlagVerwerfen: (anrufId: String) -> Unit = {},
    // ------------------------------------------------------------- Alarm
    val alarmvorschlag: suspend (incidentId: String) -> List<String> = { emptyList() },
    val eintreffzeiten: suspend (incidentId: String) -> Map<String, Int> = { emptyMap() },
    val alarmieren: (Alarmauftrag) -> Unit = {},
    /** Die gesprochene Meldung beginnen — `false`, wenn das Mikrofon nicht aufging. */
    val alarmMeldungStarten: (meldungId: String) -> Boolean = { false },
    val alarmMeldungBeenden: suspend (meldungId: String) -> Boolean = { false },
    val alarmMeldungVerwerfen: (meldungId: String) -> Unit = {},
    /**
     * Die Aufnahme abschließen, ohne auf die Antwort zu warten — für den Augenblick,
     * in dem der Dialog schon geht: Sein eigener Ablauf wäre dann mit ihm beendet.
     */
    val alarmMeldungAbschliessen: (meldungId: String) -> Unit = {},
    // ------------------------------------------ Alarm- und Ausrückeordnung
    /** Eine Ordnung für diese Schicht — `SaveAaoVorlage` am Hub. */
    val schichtordnung: (name: String, anzahl: Int, faehigkeiten: List<String>) -> Unit = { _, _, _ -> },
    val ordnungenLaden: () -> Unit = {},
    val ordnungSichern: (Aaosicherung) -> Unit = {},
    val ordnungLoeschen: (AaoVorlagenzeile) -> Unit = {},
    val strassenLaden: (landkreisId: String) -> Unit = {},
    val eventsLaden: () -> Unit = {},
    // ----------------------------------------------------------- Telefon
    val anrufFrage: (anrufId: String, art: String) -> Unit = { _, _ -> },
    val anrufFrageFrei: (anrufId: String, text: String) -> Unit = { _, _ -> },
    val anrufOrten: (anrufId: String) -> Unit = {},
    val anrufBeenden: (anrufId: String, abbrechen: Boolean) -> Unit = { _, _ -> },
    val notrufSprechenStarten: () -> Boolean = { false },
    val notrufSprechenBeenden: (anrufId: String) -> Unit = {},
    val journalOrten: (anrufId: String) -> Unit = {},
    // ----------------------------------------------------------- Tableau
    val einzelrufStarten: (vehicleId: String?) -> Unit = {},
    val streife: (vehicleId: String, an: Boolean) -> Unit = { _, _ -> },
    val funkgruppeZuweisen: (vehicleId: String, gruppeId: String?) -> Unit = { _, _ -> },
    val abrollbehaelter: (vehicleId: String, vorlageId: String?) -> Unit = { _, _ -> },
    val rufname: (vehicleId: String, name: String, kurz: String?) -> Unit = { _, _, _ -> },
    val rufnameZuruecksetzen: (vehicleId: String) -> Unit = {},
    // -------------------------------------------------------------- Funk
    val funken: (text: String, an: String?) -> Unit = { _, _ -> },
    val sprechStart: () -> Unit = {},
    val sprechEnde: () -> Unit = {},
    val funkVorlesen: (Boolean) -> Unit = {},
    val funkgruppenPlatz: (gruppen: List<String>, sendegruppe: String?) -> Unit = { _, _ -> },
    val draht: (text: String) -> Unit = {},
    val drahtSprechStart: () -> Unit = {},
    val drahtSprechEnde: () -> Unit = {},
    val funkhinweisWeg: () -> Unit = {},
    // --------------------------------------------------------- Einzelruf
    val einzelrufAnnehmen: (rufId: String) -> Unit = {},
    val einzelrufAbweisen: (rufId: String) -> Unit = {},
    val einzelrufBeenden: (rufId: String) -> Unit = {},
    val einzelrufSagen: (rufId: String, text: String) -> Unit = { _, _ -> },
    val einzelrufSprechenStarten: () -> Boolean = { false },
    val einzelrufSprechenBeenden: (rufId: String) -> Unit = {},
    /** Das Mikrofon im Gespräch zwischen zwei Menschen — an mit der Annahme, aus mit dem Ende. */
    val einzelrufMikrofon: (Boolean) -> Unit = {},
)

/**
 * Der ausgefüllte Einsatzbogen — was `CreateIncident` braucht.
 *
 * `lat`/`lon` stehen nur, wenn der Ort aus dem Gespräch oder einer Ortung kommt
 * und niemand ihn danach geändert hat.
 */
data class Einsatzbogen(
    val stichwort: String,
    val stichwortText: String,
    val meldebild: String,
    val adresse: String,
    val organisation: String,
    val prioritaet: Int,
    val ortsteil: String?,
    val meldender: String?,
    val empfohleneFahrzeuge: Int,
    val empfohleneFaehigkeiten: List<String>,
    val lat: Double? = null,
    val lon: Double? = null,
    val anrufId: String? = null,
    val ursprungEinsatzId: String? = null,
)

/**
 * Was der Alarmdialog nach oben meldet — ein Objekt statt fünf Argumenten
 * (`Alarmauftrag` im Web). Ab drei gleichartigen Werten liest niemand mehr,
 * welcher an welcher Stelle steht.
 */
data class Alarmauftrag(
    val einsatzId: String,
    val fahrzeugIds: List<String>,
    val abrollbehaelter: Map<String, String>,
    /** Die eigene Meldung — `null`, wenn nichts geschrieben wurde. */
    val zusatztext: String?,
    /** Die Kennung der gesprochenen Fassung — `null`, wenn nur getippt wurde. */
    val meldungId: String?,
)
