package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Was in einer Runde steht — übertragen aus `RoomState` in `web/src/types.ts`.
 *
 * <b>Der `RoomState` ist groß und kommt bei jeder Änderung vollständig.</b> Der
 * Server schickt keinen Unterschied, sondern den ganzen Stand; das ist die
 * einfachste Art, zwei Seiten synchron zu halten, und der Grund, warum die
 * Ansicht ihn nur anzeigen und nie fortschreiben muss.
 *
 * <b>Hier steht nur, was die App zeigt.</b> Der echte Zustand trägt Einsätze,
 * Kliniken, Entnahmestellen, Sonderobjekte, vier Funkprotokolle und die
 * Anrufliste — das kommt mit den Ansichten, die sie brauchen.
 * `ignoreUnknownKeys` lässt den Rest still fallen.
 */
@Serializable
data class Raumzustand(
    val code: String = "",
    /** `Lobby`, `Laeuft` oder `Beendet`. */
    val state: String = "Lobby",
    val settings: Rundeneinstellungen = Rundeneinstellungen(),
    val players: List<Spieler> = emptyList(),
    val incidents: List<Einsatz> = emptyList(),
    val funkprotokoll: List<Funkzeile> = emptyList(),
    val anrufe: List<Anruf> = emptyList(),
    /**
     * Wie viele Plätze der Raum hat.
     *
     * <b>Sie steht am Raum, nicht an den Einstellungen.</b> Dort hatte ich sie
     * erst gesucht — mit dem Ergebnis „5 / 0 Spieler" im Kopf der Lobby, weil
     * das Feld schlicht auf seiner Vorgabe stand. Der Kreis bestimmt sie
     * (`maxSpieler` am Landkreis), der Raum trägt sie.
     */
    val maxSpieler: Int = 0,
    val vehicles: List<Rundenfahrzeug> = emptyList(),
    val lobbychat: List<Lobbynachricht> = emptyList(),
    val gestartetUm: String? = null,
    /** Der Lektionsstand der Ausbildungsschicht — `null` in jeder anderen Runde. */
    val ausbildung: Ausbildung? = null,
    /** Die ortsfesten Ziele der Karte. */
    val kliniken: List<Klinik> = emptyList(),
    val entnahmestellen: List<Entnahmestelle> = emptyList(),
    val sonderobjekte: List<Sonderobjekt> = emptyList(),
    /** Wer zusieht — zählt nicht gegen `maxSpieler`. */
    val zuschauer: List<Zuschauer> = emptyList(),
    /**
     * Der Leitstellendraht — nur gefüllt, wenn man selbst Leitstelle ist und
     * mindestens zwei Leitstellenplätze besetzt sind (`drahtOffen`).
     */
    val leitstellendraht: List<Drahtnachricht> = emptyList(),
    val drahtOffen: Boolean = false,
    /**
     * Der Einsatzstellenfunk — nur gefüllt, wenn das eigene Fahrzeug mit
     * Status 4 an einer offenen Lage steht. Der Server filtert; die App
     * zeigt an, was kommt.
     */
    val einsatzstellenchat: List<Einsatzstellennachricht> = emptyList(),
    /**
     * Welche Funkgruppen **ich** mithöre — ausgeschrieben vom Server, nicht als
     * „leer heißt alle". Leer heißt hier wirklich leer: eine Runde ohne
     * getrennte Funkverkehrskreise.
     */
    val gehoerteFunkgruppen: List<String> = emptyList(),
    /** Auf welcher Gruppe ich sende; `null` = der eine Kanal. */
    val sendegruppe: String? = null,
    /** Der Übungsstand — nur im Szenario-Modus gefüllt. */
    val uebung: Uebungsstand? = null,
    // Begleiter (Einzelruf): klingelnde und laufende Einzelrufe — siehe Einzelrufmodelle.kt.
    val einzelrufe: List<Einzelruf> = emptyList(),
) {
    val inLobby: Boolean get() = state == "Lobby"
    val laeuft: Boolean get() = state == "Laeuft"
    val beendet: Boolean get() = state == "Beendet"

    /**
     * Ob der Dienst beginnen kann.
     *
     * <b>Drei Bedingungen, und alle drei kommen vom Server.</b> Die App rechnet
     * sie hier nur nach, um den Knopf zu sperren und daneben zu sagen, was fehlt
     * — durchsetzen tut es der Hub. Wer sie hier weglässt, bietet einen Knopf an,
     * der mit einer Fehlermeldung antwortet.
     */
    val alleBereit: Boolean get() = players.isNotEmpty() && players.all { it.bereit }

    val hatLeitstelle: Boolean get() = players.any { it.role == "Leitstelle" }

    /** Wer ohne Platz dasteht — Fahrzeugbesatzung ohne Fahrzeug. */
    val ohnePlatz: List<Spieler>
        get() = players.filter { it.role == "Fahrzeugbesatzung" && it.vehicleId == null }
}

@Serializable
data class Rundeneinstellungen(
    /** Der Name der Leitstelle — „ILS Bodensee-Oberschwaben". */
    val leitstelle: String? = null,
    val leitstellensitz: String? = null,
    val landkreis: String? = null,
    val landkreisId: String? = null,
    val ort: String? = null,
    val oeffentlich: Boolean = false,
    /**
     * Ob die Runde ungewertet läuft.
     *
     * <b>Das steht im Kopf und nicht im Kleingedruckten.</b> Wer beitritt, muss
     * vorher wissen, dass es hier keine Punkte gibt — hinterher ist die Schicht
     * gefahren.
     */
    val sandkasten: Boolean = false,
    val modus: String? = null,
    /** Die Mitte des Kreises — der Startausschnitt der Karte. */
    val lat: Double? = null,
    val lon: Double? = null,
    /** Woher der Wind kommt (Grad) — für die Gefahrgutfahne. */
    val windrichtung: Int? = null,
    /**
     * Die Funkverkehrskreise der Runde.
     *
     * <b>Leer heißt ein Kanal für alle</b> — und das ist die Vorgabe: Wer allein
     * spielt, sieht von der ganzen Sache nichts.
     */
    val funkgruppen: List<Funkgruppe> = emptyList(),
)

/**
 * Ein Funkverkehrskreis — Spiegel von `Funkgruppe` in `types.ts`.
 *
 * <b>Die Farbe steht bewusst nicht am Server.</b> Sie ist keine Eigenschaft des
 * Kreises, sondern eine Anzeigefrage; der Client hat die Palette.
 */
@Serializable
data class Funkgruppe(
    val id: String = "",
    /** Die Rufgruppennummer wie im Gerät — „3301". Darf leer sein. */
    val nummer: String = "",
    val name: String = "",
    /** Nummer und Name in einem: „3301 Feuerwehr LK Zwickau". */
    val bezeichnung: String = "",
    /** Die Kurzmarke am Rand einer Protokollzeile — die Nummer, wo es eine gibt. */
    val marke: String = "",
    /** Wer hier zu Hause ist. Leer ist gültig: der Einsatzstellenkanal. */
    val organisationen: List<String> = emptyList(),
    /** Einzelne Träger, für die diese Gruppe gilt — feiner als die Organisation. */
    val hiOrgs: List<String> = emptyList(),
    /** Die Führungsgruppe: ELW, KdoW und OrgL, quer über alle Organisationen. */
    val fuehrung: Boolean = false,
    /**
     * Einsatz einer dynamischen DMO-Gruppe; `null` bei einer Kreisgruppe.
     *
     * <b>Die Leitstelle sieht sie nicht.</b> DMO ist Direktbetrieb, Gerät zu
     * Gerät, an der Einsatzstelle und ohne Netz — der Disponententisch ist dort
     * nicht dabei und könnte darauf weder hören noch senden.
     */
    val einsatzId: String? = null,
)

/**
 * Ein Spieler in der Runde.
 *
 * `role` ist `Unbestimmt`, `Leitstelle` oder `Fahrzeugbesatzung` — die drei
 * Werte von `PlayerRole`. Sie stehen als Zeichenkette und nicht als Aufzählung,
 * damit ein vierter Wert des Servers die Lobby nicht zum Absturz bringt.
 */
@Serializable
data class Spieler(
    val id: String = "",
    val name: String = "",
    val role: String = "Unbestimmt",
    val vehicleId: String? = null,
    val bereit: Boolean = false,
    val verbunden: Boolean = true,
    val istBot: Boolean = false,
    val level: Int = 0,
    val rang: String = "",
    val premium: Boolean = false,
    val teammitglied: Boolean = false,
    // Begleiter (Einzelruf): der Ruhe-Schalter des Platzes — die Leitstelle kommt immer durch.
    val einzelrufZulassen: Boolean = true,
) {
    val istLeitstelle: Boolean get() = role == "Leitstelle"
    val istBesatzung: Boolean get() = role == "Fahrzeugbesatzung"
}

/**
 * Ein Fahrzeug in der laufenden Runde.
 *
 * <b>`statusText` kommt vom Server und wird nicht nachgebaut.</b> Drei Status
 * heißen nicht überall gleich (2 bei der Polizei, 7 und 8 beim Rettungsdienst);
 * welche das sind, entscheidet `FmsRules` am Server — die App zeigt an, was er
 * schickt, statt eine zweite Tabelle zu führen.
 */
@Serializable
data class Rundenfahrzeug(
    val id: String = "",
    val templateId: String = "",
    val funkrufname: String = "",
    val kurzname: String = "",
    val typ: String = "",
    val organisation: String = "",
    val status: Int = 0,
    val statusText: String = "",
    val statusSeit: String = "",
    val besatzung: String = "",
    val faehigkeiten: List<String> = emptyList(),
    val einsatzId: String? = null,
    val playerId: String? = null,
    /** Ob ein Alarm noch unbestätigt am Melder hängt. */
    val alarmOffen: Boolean = false,
    val sondersignalAus: Boolean = false,
    val sprechwunschSeit: String? = null,
    val sprechwunschVorrang: Boolean = false,
    val sprechwunschAnliegen: String? = null,
    val besatzungVerfuegbar: Boolean = true,
    val ausserDienstGrund: String? = null,
    /**
     * Auf welcher Funkgruppe dieses Fahrzeug gerade ist — ausgerechnet, also
     * einschließlich Stammgruppe. `null` = der eine Kanal.
     */
    val funkgruppe: String? = null,
    /**
     * Ob es dorthin geschaltet wurde, statt auf der Gruppe seiner Organisation
     * zu stehen. Nur dafür da, „zurück auf Stamm" anzubieten, wo es etwas
     * zurückzunehmen gibt.
     */
    val funkgruppeAufgeschaltet: Boolean = false,
    /** Wo das Fahrzeug steht — der Server schiebt, die App zeichnet. */
    val lat: Double? = null,
    val lon: Double? = null,
    val wacheLat: Double? = null,
    val wacheLon: Double? = null,
    /** Die Fahrstrecke aus dem Routingdienst — leer heißt: Luftlinie zeichnen. */
    val route: List<Ort> = emptyList(),
    val routeIndex: Int = 0,
    val einsatzstelleErreicht: Boolean = false,
)

/** Ein Punkt auf der Karte. */
@Serializable
data class Ort(val lat: Double = 0.0, val lon: Double = 0.0)

/**
 * Ein Einsatz — der Kern der Runde.
 *
 * Das Web-Modell trägt weit über vierzig Felder (MANV, Wasserlage, Suchgebiet,
 * Zugplätze …); hier stehen die, die Einsatzliste, Alarmdialog und
 * Fahrzeugansicht zeigen. Der Rest kommt mit den Ansichten, die ihn brauchen.
 */
@Serializable
data class Einsatz(
    val id: String = "",
    val einsatznummer: String = "",
    val stichwort: String = "",
    val stichwortText: String = "",
    val meldebild: String = "",
    val adresse: String = "",
    val ortsteil: String? = null,
    val meldender: String? = null,
    val organisation: String = "Feuerwehr",
    val prioritaet: Int = 2,
    /** `Offen`, `Alarmiert`, `Anfahrt`, `VorOrt`, `InArbeit`, `Abgeschlossen`. */
    val state: String = "Offen",
    val alarmierteFahrzeuge: List<String> = emptyList(),
    val empfohleneFahrzeuge: Int = 0,
    val empfohleneFaehigkeiten: List<String> = emptyList(),
    val sichtung: Sichtung? = null,
    val nachforderungen: Int = 0,
    /** Wo die Lage liegt — `null`, solange der Ort nicht bekannt ist. */
    val lat: Double? = null,
    val lon: Double? = null,
    // ----------------------------------------------------------------- MANV
    /** Ob es ein Massenanfall ist — steht schon beim Notruf fest. */
    val manv: Boolean = false,
    val manvStufe: Int? = null,
    /**
     * Die Zwischenstufe der Erkundung — gerundet. <b>Solange sie steht, sind
     * `sichtung` und `manvPatienten` bewusst leer:</b> Wer die Lagegröße aus
     * der Patientenliste liest, liest null, obwohl dreißig liegen.
     */
    val betroffeneUngefaehr: Int? = null,
    val manvPatienten: List<ManvPatient> = emptyList(),
    val versorgungsstellen: List<Versorgungsstelle> = emptyList(),
    /** Auftrag → Funkrufname des Beauftragten (Vorsichtung, Sichtung, …). */
    val manvauftraege: Map<String, String> = emptyMap(),
    val einsatzleitung: String? = null,
    val einsatzleitungRd: String? = null,
    val abschnitte: List<Einsatzabschnitt> = emptyList(),
    val abschnitteRd: List<Einsatzabschnitt> = emptyList(),
    val aufgaben: List<Aufgabe> = emptyList(),
    // ------------------------------------------------------ Lagen mit Fläche
    val absperrradiusMeter: Double? = null,
    val brandradiusMeter: Double? = null,
    val suchradiusMeter: Double? = null,
    val suchabschnitte: List<Suchabschnitt> = emptyList(),
) {
    val offen: Boolean get() = state == "Offen"
    val abgeschlossen: Boolean get() = state == "Abgeschlossen"
}

/** Ein Patient am MANV — die Kategorie vergibt die Sichtung, nie der Spieler. */
@Serializable
data class ManvPatient(
    val id: String = "",
    /** `Rot`, `Gelb`, `Gruen`, `Schwarz` — oder `null` vor der Sichtung. */
    val kategorie: String? = null,
    /** `WartetAufSichtung`, `WartetAufTransport`, `ImTransport`, `Uebergeben`. */
    val status: String = "WartetAufSichtung",
    val transportVehicleId: String? = null,
    val zielklinikId: String? = null,
    /** Die nötige Versorgungsstufe — `null` bei Schwarz, mit Absicht. */
    val bedarf: String? = null,
    /** Der Name der Versorgungsstelle, an der er liegt — `null` = im Gelände. */
    val stelle: String? = null,
)

/** Verletztenablage oder Behandlungsplatz an einer Lage. */
@Serializable
data class Versorgungsstelle(
    val art: String = "Verletztenablage",
    val name: String = "",
    val kapazitaet: Int = 0,
    val einsatzbereit: Boolean = false,
    val aufbaufortschritt: Double = 0.0,
    val patienten: List<String> = emptyList(),
    val imAbbau: Boolean = false,
)

/** Ein Einsatzabschnitt — Name plus zugeteilte Funkrufnamen. */
@Serializable
data class Einsatzabschnitt(
    val name: String = "",
    val funkrufnamen: List<String> = emptyList(),
)

/** Eine Aufgabe des Einsatzarbeitsplans — hier zählt nur, ob sie fertig ist. */
@Serializable
data class Aufgabe(
    val name: String = "",
    val fertig: Boolean = false,
)

/** Ein Sektor des Suchgebiets — acht à 45 Grad. */
@Serializable
data class Suchabschnitt(
    val nummer: Int = 0,
    val fortschritt: Double = 0.0,
)

/** Ein Krankenhaus auf der Karte. */
@Serializable
data class Klinik(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val abteilungen: List<String> = emptyList(),
    val abgemeldet: List<String> = emptyList(),
    val hatLandeplatz: Boolean = false,
    val istUeberregional: Boolean = false,
)

/** Eine Löschwasser-Entnahmestelle. */
@Serializable
data class Entnahmestelle(
    val id: String = "",
    val name: String = "",
    val art: String = "",
    val artText: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

/** Ein Sonderobjekt — Schule, Heim, Industrie. */
@Serializable
data class Sonderobjekt(
    val id: String = "",
    val name: String = "",
    val art: String = "",
    val artText: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

/**
 * Ein Zuschauer — bewusst kein `Spieler`: Er hat weder Rolle noch Fahrzeug und
 * zählt nicht gegen `maxSpieler`. `regie = true` ist der Regieplatz.
 */
@Serializable
data class Zuschauer(
    val id: String = "",
    val name: String = "",
    val verbunden: Boolean = true,
    val regie: Boolean = false,
    val teammitglied: Boolean = false,
)

/** Der Stand einer Übung (Szenario-Modus) — die Zeitachse der Regie. */
@Serializable
data class Uebungsstand(
    val name: String = "",
    val stand: Int = 0,
    val eintraege: Int = 0,
    val angehalten: Boolean = false,
    val naechsterText: String? = null,
    val naechsterNachSekunden: Int? = null,
    val vergangeneSekunden: Int = 0,
    /** Nur für die Übungsleitung gefüllt. */
    val marken: List<Uebungsmarke> = emptyList(),
)

@Serializable
data class Uebungsmarke(
    val nachSekunden: Int = 0,
    /** `Lage`, `Stoerung` oder `Wetterwechsel`. */
    val art: String = "Lage",
)

/**
 * Eine Zeile auf dem Leitstellendraht.
 *
 * Absender ist der <b>Spielername</b>, nicht der Leitstellenname — alle
 * Plätze teilen sich denselben. `nurGesprochen` ist der Sprechvermerk ohne
 * Wortlaut.
 */
@Serializable
data class Drahtnachricht(
    val id: String = "",
    val zeit: String = "",
    val vonId: String = "",
    val vonName: String = "",
    val text: String = "",
    val nurGesprochen: Boolean = false,
)

/** Eine Zeile im Einsatzstellenfunk — Absender ist der Funkrufname. */
@Serializable
data class Einsatzstellennachricht(
    val id: String = "",
    val zeit: String = "",
    val einsatzId: String = "",
    val vonPlayerId: String = "",
    val vonFunkrufname: String = "",
    val text: String = "",
    val nurGesprochen: Boolean = false,
)

/** Ein schlanker Positions-Tick — kommt mehrmals je Sekunde, ohne Vollstand. */
@Serializable
data class FahrzeugPosition(
    val vehicleId: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val einsatzstelleErreicht: Boolean = false,
    val routeIndex: Int = 0,
)

/** Die Fahrstrecke eines Fahrzeugs — kommt nach, sobald das Routing liefert. */
@Serializable
data class FahrzeugRoute(
    val vehicleId: String = "",
    val route: List<Ort> = emptyList(),
    val routeIndex: Int = 0,
)

/**
 * Der Lektionsstand der Ausbildungsschicht.
 *
 * `titel` und `text` sind die Hinweisleiste — der Server führt durch die
 * Schicht, die App zeigt nur an. `hinweis` ist der deutlichere zweite Satz,
 * wenn ein Schritt länger offen bleibt.
 */
@Serializable
data class Ausbildung(
    val schritt: Int = 0,
    val schritte: Int = 0,
    val titel: String? = null,
    val text: String? = null,
    val hinweis: String? = null,
    val abgeschlossen: Boolean = false,
)

/** Die Sichtung an einer Lage — Rot, Gelb, Grün und die Verstorbenen. */
@Serializable
data class Sichtung(
    val rot: Int = 0,
    val gelb: Int = 0,
    val gruen: Int = 0,
    val schwarz: Int = 0,
)

/**
 * Eine Zeile im Funkprotokoll.
 *
 * `kind` unterscheidet Funk, System, Lagemeldung, Nachforderung, Alarm und
 * Sprache — die Farbe der Zeile hängt daran.
 */
@Serializable
data class Funkzeile(
    val id: String = "",
    val zeit: String = "",
    val kind: String = "Funk",
    val von: String = "",
    val an: String? = null,
    val text: String = "",
    val incidentId: String? = null,
    val funkgruppe: String? = null,
)

/**
 * Der Alarm, wie er am Melder ankommt.
 *
 * Er ist ein eigenes Ereignis und kein Teil des Raumzustands: Der Zustand sagt,
 * *dass* ein Alarm offen ist (`alarmOffen`), das Ereignis sagt, *was* gerade
 * gekommen ist — und nur das Ereignis darf den Melder auslösen. Wer den Melder
 * am Zustand aufhängt, weckt ihn bei jedem Neuladen wieder auf.
 */
@Serializable
data class Alarmmeldung(
    val incidentId: String = "",
    val einsatznummer: String = "",
    val schleife: String = "",
    val stichwort: String = "",
    val stichwortText: String = "",
    val meldebild: String = "",
    val adresse: String = "",
    val ortsteil: String? = null,
    val prioritaet: Int = 2,
    val zeit: String = "",
    val einheiten: List<String> = emptyList(),
    /**
     * Die eigene Meldung der Leitstelle — null, wenn sie nichts gesagt hat.
     *
     * <b>Der Vorgabewert ist hier eine Falle, kein Komfort.</b> `kotlinx.serialization`
     * lässt ein unbekanntes Feld stillschweigend leer; wäre diese Zeile beim Umbau
     * vergessen worden, sähe der Android-Spieler seinen Alarm **ohne** die Worte der
     * Leitstelle, während jeder Browser sie hat — und das sähe aus wie ein Fehler des
     * Servers. Wer am Melderinhalt etwas ändert, ändert diese Datei mit.
     */
    val zusatztext: String? = null,
)

/**
 * Ein Notruf an der Leitstelle.
 *
 * <b>Das Gespräch ist das Rätsel.</b> Der Anrufer weiß etwas, die Leitstelle
 * fragt es ab (`erfragt` hält fest, was schon geklärt ist), und aus dem, was
 * erfragt wurde, baut der Server den `vorschlag` — nur daraus: Was nicht
 * gefragt wurde, steht auch nicht drin.
 */
@Serializable
data class Anruf(
    val id: String = "",
    val nummer: Int = 0,
    /** `Klingelt`, `ImGespraech`, `Beendet`, `Verpasst`, `Abgewiesen`. */
    val zustand: String = "Klingelt",
    val bearbeiterPlayerId: String? = null,
    val eingangUm: String = "",
    val verlauf: List<Gespraechszeile> = emptyList(),
    val erfragt: List<String> = emptyList(),
    val vorschlag: Notrufvorschlag? = null,
) {
    val klingelt: Boolean get() = zustand == "Klingelt"
    val imGespraech: Boolean get() = zustand == "ImGespraech"
}

@Serializable
data class Gespraechszeile(
    val zeit: String = "",
    val vonLeitstelle: Boolean = false,
    val text: String = "",
    val frage: String? = null,
    val verstanden: Boolean = true,
)

@Serializable
data class Notrufvorschlag(
    val stichwort: String = "",
    val stichwortText: String = "",
    val organisation: String = "Feuerwehr",
    val prioritaet: Int = 2,
    val meldebild: String = "",
    val adresse: String = "",
    val ortsteil: String? = null,
    val meldender: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val empfohleneFahrzeuge: Int = 0,
    val empfohleneFaehigkeiten: List<String> = emptyList(),
    /** 0 bis 1 — wie vollständig abgefragt wurde. */
    val guete: Double = 0.0,
)

/**
 * Die sechs Dinge, die man einen Anrufer fragen kann.
 *
 * Die Reihenfolge ist die des Webs: Ort zuerst, denn ohne Ort fährt niemand.
 */
val FAKTENARTEN: List<Pair<String, String>> = listOf(
    "Ort" to "Wo genau ist das?",
    "Was" to "Was ist passiert?",
    "Betroffene" to "Sind Menschen betroffen?",
    "Ausmass" to "Wie groß ist das Ausmaß?",
    "Gefahren" to "Gibt es besondere Gefahren?",
    "Anrufer" to "Wie heißen Sie?",
)

/** Was der Server nach Dienstende gutgeschrieben hat. */
@Serializable
data class Gutschrift(
    val kennung: String = "",
    val punkte: Int = 0,
    val gesamt: Int = 0,
    val level: Int = 0,
    val rang: String = "",
)

/** Eine Zeile im Lobby-Chat. */
@Serializable
data class Lobbynachricht(
    val id: String = "",
    val absender: String = "",
    val text: String = "",
    val um: String = "",
    val istSystem: Boolean = false,
)

/**
 * Was beim Beitritt herauskommt.
 *
 * <b>`ok = false` ist kein Netzfehler, sondern eine Absage</b> — „Diesen Raum
 * gibt es nicht (mehr)", „Der Raum ist voll", „Bitte melde dich zuerst an". Der
 * Grund steht in `fehler` und ist für Menschen geschrieben.
 */
@Serializable
data class Beitrittsergebnis(
    val ok: Boolean = false,
    val state: Raumzustand? = null,
    val fehler: String? = null,
)
