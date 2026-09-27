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
    // ------------------------------------------------------------------------
    // Der Rest von `RoomStateDto` — nachgetragen, damit Leitstelle, Fahrzeug und
    // Nachbesprechung nicht an einem Feld scheitern, das still wegfiel.
    // ------------------------------------------------------------------------
    /** Lehrgangsprüfung statt gewöhnlicher Schicht. */
    val istPruefung: Boolean = false,
    /** Zeitachse und alle Prüfungslagen sind abgearbeitet. */
    val pruefungAbschlussbereit: Boolean = false,
    /**
     * Wer den Sprechfunk der **eigenen** Einsatzstelle gerade hält — die dritte
     * Halbduplex-Leitung neben `funkkanal` und `drahtkanal`.
     */
    val einsatzstellenkanal: Funkkanal = Funkkanal(),
    /**
     * Die gerade belegten Kanäle samt Gruppe. <b>Nur die belegten</b> — ein
     * freier Kanal ist die Abwesenheit eines Eintrags.
     */
    val funkkanaele: List<Funkkanal> = emptyList(),
    /** Mein eigener Sendekanal: wer ihn gerade hält, oder niemand. */
    val funkkanal: Funkkanal = Funkkanal(),
    /** Wer den Draht gerade hält — nur an den Leitstellenplätzen gefüllt. */
    val drahtkanal: Funkkanal = Funkkanal(),
    /** Die klingelnden und laufenden Einzelrufe der Runde. */
    val einzelrufe: List<Einzelruf> = emptyList(),
    /** Offene Eigenfeststellungen der Streifen — Meldungen ohne Einsatz. */
    val feststellungen: List<Feststellung> = emptyList(),
    /** Abgeschlossene Anrufe der Schicht — jedes Gespräch bleibt nachlesbar. */
    val anrufjournal: List<AnrufjournalEintrag> = emptyList(),
    val beendetUm: String? = null,
    /** Die Uhr des Servers beim Verschicken — gegen die schiefe Uhr des Handys. */
    val serverZeit: String = "",
    /**
     * Laufende Nummer des Zustands. <b>Ältere Stände werden verworfen</b> — der
     * Server verschickt außerhalb seiner Raumsperre, zwei Stände können sich
     * überholen (siehe `Runde.setzen`).
     */
    val version: Long = 0,
    val minSpieler: Int = 0,
    /** Wie viele Spieler gleichzeitig die Leitstelle besetzen dürfen. */
    val maxLeitstellen: Int = 1,
    /** Gespeicherte Alarm- und Ausrückeordnungen für die Freie Vergabe. */
    val aaoVorlagen: List<AaoVorlage> = emptyList(),
    /** Offene Bitten um einen Leitstellenplatz — entscheiden darf nur der Host. */
    val platzanfragen: List<Platzanfrage> = emptyList(),
    /** Offene Bitten um einen Platz in dieser **vollen** Runde — von Zuschauern. */
    val beitrittsanfragen: List<Platzanfrage> = emptyList(),
    /** Ob die Runde gerade voll ist — gerechnet vom Server, nicht hier. */
    val voll: Boolean = false,
    /** Wie viele Plätze über der eingestellten Größe schon zugesagt sind. */
    val ueberzaehlig: Int = 0,
    /**
     * Ob man selbst der Host ist — die erste Leitstelle im Raum. Kommt gerechnet
     * vom Server, damit die Regel nicht an zwei Stellen steht.
     */
    val istHost: Boolean = false,
    /** Der Stand der Abstimmung übers Dienstende (`Logic.Dienstende`). */
    val dienstendeStimmen: Int = 0,
    /** Wie viele Stimmen es braucht. Bei 1 ist es kein Abstimmen, sondern ein Beenden. */
    val dienstendeSchwelle: Int = 0,
    val dienstendeEigeneStimme: Boolean = false,
    /** Die Nachbesprechung; steht erst nach Dienstende. */
    val auswertung: Auswertung? = null,
    /** Wo das Wetter gerade steht — die geltende Lage steht zusätzlich in `settings`. */
    val wetter: Wetterstand = Wetterstand(),
    /** Das laufende Übergabeangebot samt Protokoll; `null` im Regelfall. */
    val uebergabe: Uebergabe? = null,
    /** Seit wann keine Leitstelle mehr verbunden ist; `null` im Regelfall. */
    val leitstelleVerwaistSeit: String? = null,
) {
    /**
     * Die Menschen im Raum — Bot-Besatzungen sind Ausstattung, keine Teilnehmer.
     * „5 / 24 Spieler" zählt diese Liste, nicht `players`.
     */
    val menschen: List<Spieler> get() = players.filter { !it.istBot }

    val bots: List<Spieler> get() = players.filter { it.istBot }

    /** Wer am Leitstellentisch sitzt — in der Reihenfolge des Servers. */
    val leitstellen: List<Spieler> get() = players.filter { it.istLeitstelle }

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
    /**
     * Der Spielmodus: `Zufall`, `Frei`, `Ausbildung`, `Tagesschicht`, `Szenario`.
     *
     * <b>Das Feld heißt am Server `mode`, nicht `modus`.</b> Unter dem deutschen
     * Namen stand es hier einmal — und war damit immer `null`, weil
     * `ignoreUnknownKeys` das echte Feld still fallen ließ.
     */
    val mode: String = "Zufall",
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
    // ------------------------------------------------------------------------
    // Der Rest von `SettingsDto` — in der Reihenfolge des Servers.
    // ------------------------------------------------------------------------
    /** `Feuerwehr`, `Rettungsdienst`, `Thw`, `Polizei` — wer in dieser Runde fährt. */
    val organisationen: List<String> = emptyList(),
    val einsatzIntervallSekunden: Int = 0,
    /** `Gemuetlich`, `Normal`, `Zuegig`. */
    val botTempo: String = "Normal",
    /**
     * Kennungen der Nachbarkreise, die diese Runde mitdisponiert — leer heißt:
     * nur der eigene Kreis. Die Namen stehen im Kreiskatalog.
     */
    val mitkreise: List<String> = emptyList(),
    /** Hilfsorganisationen, die in diesem Kreis fahren. Leer = alle erlaubt. */
    val hiOrgs: List<String> = emptyList(),
    /** Rangschranken für diese Runde außer Kraft. */
    val freischaltungenIgnorieren: Boolean = false,
    /** `Knapp`, `Normal`, `Gespraechig`. */
    val botGespraechigkeit: String = "Normal",
    /** Hauptschalter: aus heißt, Bots antworten nur mit einer knappen Quittung. */
    val botFunkAktiv: Boolean = false,
    /** `Echtzeit` oder `Simulation`. */
    val zeitmodus: String = "Echtzeit",
    /** Startwert der Wetterlage: `Klar`, `Regen`, `Glaette`, `Sturm`. */
    val wetter: String = "Klar",
    /** Notrufe kommen als Telefonanruf herein statt als fertiger Einsatz. */
    val telefonischeLeitstelle: Boolean = false,
    /** `Aus`, `Selten`, `Gelegentlich`. */
    val stoerungshaeufigkeit: String = "Aus",
    val tagesalarmstaerke: Boolean = false,
    val loeschwasser: Boolean = false,
    val sonderobjekte: Boolean = false,
    val wiederherstellung: Boolean = false,
    val gefahrgutlagen: Boolean = false,
    /** Die Windrichtung als Wort — „Südwest". */
    val windText: String = "",
    /** `Fruehling`, `Sommer`, `Herbst`, `Winter`. */
    val jahreszeit: String = "Sommer",
    val silvester: Boolean = false,
    val verlegungsfahrten: Boolean = false,
    val suchlagen: Boolean = false,
    val einsatzarbeit: Boolean = false,
    val vegetationsbraende: Boolean = false,
    /** `NurEinsatzleitung` oder `AlleBesatzungen`. */
    val arbeitsfunk: String = "NurEinsatzleitung",
    /** `Selbsttaetig` oder `NachFreigabe`. */
    val einsatzende: String = "Selbsttaetig",
    val einsatzleitung: Boolean = false,
    /** Die bespielten Wachen; leer heißt „alle, die der Kreis hergibt". */
    val wachen: List<Wachenwahl> = emptyList(),
    /** Eigene Wachnummern je Wachenkennung — die Zahl vor dem ersten Schrägstrich. */
    val wachnummern: Map<String, Int> = emptyMap(),
    /** Eigene Rufname-Wörter je Träger bzw. Organisation („Florian" → „Christoph"). */
    val rufnamenpraefixe: Map<String, String> = emptyMap(),
    /** Eigene Kennzahlen je Fahrzeugvorlage — die Mitte des Rufnamens. */
    val kennzahlen: Map<String, String> = emptyMap(),
    /** Mindestbreite der Wachnummer im Rufnamen: 1 → „1/44/1", 2 → „01/44/1". */
    val wachennummerStellen: Int = 1,
    /** Dasselbe für die letzte Zahl. */
    val laufnummerStellen: Int = 1,
    /** Nach wie vielen 20-Sekunden-Dauersendungen der Spieler entfernt wird (3–5). */
    val funkverstossSchwelle: Int = 3,
    /** `Ruhig`, `Normal`, `Dicht`. */
    val einsatzdichte: String = "Normal",
    /** Die Zahl der offenen Einsätze, die im Dienst wirklich gilt — vom Server gerechnet. */
    val offeneEinsatzGrenze: Int = 0,
    /** Diese Schicht wird öffentlich übertragen — jeder, der hinzukommt, wird gefragt. */
    val streamermodus: Boolean = false,
    /** `Twitch`, `YouTube`, `TikTok`, `Kick`, `Discord`, `Andere`. */
    val streamerplattform: String = "Twitch",
    val streamerkanal: String? = null,
    val streameraufzeichnung: Boolean = false,
    /** Wer überträgt — die Spieler-Id dessen, der den Schalter umgelegt hat. */
    val streamerKontoId: String? = null,
    /** Bot-Besatzungen formulieren frei (KI) — nur, wo der Server es anbietet. */
    val kiFunkAktiv: Boolean = false,
    /** Das Stichwort-Set dieser Runde; `null` heißt der Grundkatalog. */
    val stichwortsetId: String? = null,
)

/**
 * Eine bespielte Wache — Spiegel von `Wachenwahl` am Server.
 *
 * Wiederzufinden über die Kennung, nicht über die Stelle in der Liste. `null`
 * heißt jeweils „wie im Abzug".
 */
@Serializable
data class Wachenwahl(
    val kennung: String = "",
    val name: String? = null,
    val zugnummer: Int = 1,
    val lat: Double? = null,
    val lon: Double? = null,
    val organisation: String? = null,
    val traeger: String? = null,
)

/**
 * Wo das Wetter der Runde gerade steht — was gilt, was kommt, ob eine Welle läuft.
 */
@Serializable
data class Wetterstand(
    val aktuell: String = "Klar",
    val aktuellText: String = "",
    val jahreszeit: String = "Sommer",
    val angekuendigt: String? = null,
    val angekuendigtText: String? = null,
    /** Ob gerade eine Unwetterwelle läuft — viele kleine Lagen auf einmal. */
    val welle: Boolean = false,
    /** Faktor auf die Fahrgeschwindigkeit; 1 bei klarer Lage. */
    val bremsfaktor: Double = 1.0,
)

/**
 * Wer einen Halbduplex-Kanal gerade hält. Beide Felder `null` heißt: frei.
 * `funkgruppe` `null` heißt: der eine Kanal einer Runde ohne Gruppen.
 */
@Serializable
data class Funkkanal(
    val senderPlayerId: String? = null,
    val senderName: String? = null,
    val funkgruppe: String? = null,
) {
    val frei: Boolean get() = senderPlayerId == null
}

/** Eine gespeicherte Alarm- und Ausrückeordnung für die Freie Vergabe. */
@Serializable
data class AaoVorlage(
    val name: String = "",
    val empfohleneFahrzeuge: Int = 0,
    val empfohleneFaehigkeiten: List<String> = emptyList(),
)

/**
 * Eine offene Bitte um einen Platz — am Leitstellentisch oder in einer vollen
 * Runde. Der Name steht mit dabei: Wer gerade die Verbindung verloren hat, steht
 * in der Spielerliste blass, und eine Anfrage ohne Namen wäre ein Rätsel.
 */
@Serializable
data class Platzanfrage(
    val playerId: String = "",
    val name: String = "",
    /** Bitten verfallen nach fünf Minuten. */
    val um: String = "",
)

/**
 * Ein Einzelruf — das Vollduplex-Gespräch zwischen zwei Plätzen.
 *
 * `zielPlayerId` `null` heißt: Die Leitstelle wird gerufen, und jeder ihrer
 * Plätze darf abnehmen; wer abnimmt, steht danach in `angenommenVonPlayerId`.
 */
@Serializable
data class Einzelruf(
    val id: String = "",
    /** `Klingelt` oder `Laeuft`. */
    val zustand: String = "Klingelt",
    val vonPlayerId: String = "",
    val vonName: String = "",
    val zielPlayerId: String? = null,
    val zielName: String = "",
    val angenommenVonPlayerId: String? = null,
    val eingangUm: String = "",
    val angenommenUm: String? = null,
    /** Am anderen Ende sitzt eine Bot-Besatzung — dann läuft es in Zeilen. */
    val mitBot: Boolean = false,
    /** Was gesagt wurde — nur im Gespräch mit einem Bot und nur für die Beteiligten. */
    val verlauf: List<Einzelrufzeile>? = null,
)

@Serializable
data class Einzelrufzeile(
    val id: String = "",
    val zeit: String = "",
    val vonPlayerId: String = "",
    val vonName: String = "",
    val text: String = "",
)

/**
 * Eine offene Eigenfeststellung einer Streife — etwas, das die Besatzung selbst
 * gesehen hat und das noch kein Einsatz ist. Die gewürfelte Lage bleibt am
 * Server; übertragen wird, was die Besatzung gesagt hat.
 */
@Serializable
data class Feststellung(
    val id: String = "",
    val vehicleId: String = "",
    val funkrufname: String = "",
    val stichwort: String = "",
    val stichwortText: String = "",
    val meldebild: String = "",
    val ort: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val prioritaet: Int = 2,
    val gemeldetUm: String = "",
    /** Bis hierher steht das Angebot; danach verfällt es still. */
    val verfaelltBis: String = "",
)

/**
 * Der Stand der Ortung eines Anrufs. Solange sie läuft, steht hier nur, dass sie
 * läuft — das Ergebnis kommt erst nach der Wartezeit.
 */
@Serializable
data class Ortung(
    val laeuft: Boolean = false,
    val fertigUm: String = "",
    val erfolgreich: Boolean? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val radiusMeter: Int? = null,
    val ortstext: String? = null,
)

/** Ein abgeschlossener Anruf im Journal der Schicht. */
@Serializable
data class AnrufjournalEintrag(
    val anrufId: String = "",
    val nummer: Int = 0,
    val eingangUm: String = "",
    val abgeschlossenUm: String = "",
    /** `EinsatzAngelegt`, `Verworfen`, `Verpasst`, `Abgewiesen`, `Presse`, `Zugeordnet`. */
    val ausgang: String = "",
    val incidentId: String? = null,
    val verlauf: List<Gespraechszeile> = emptyList(),
    val erfragt: List<String> = emptyList(),
    val ortung: Ortung? = null,
    /** Ob sich noch nachträglich orten lässt. */
    val ortbar: Boolean = false,
)

// ------------------------------------------------------------- Nachbesprechung

/** Die Nachbesprechung — steht erst nach Dienstende am Raum. */
@Serializable
data class Auswertung(
    val befunde: List<Befund> = emptyList(),
    val zeitachsen: List<Zeitachse> = emptyList(),
    val spieler: List<Spielerbilanz> = emptyList(),
    val doppelmeldungen: Doppelmeldungen = Doppelmeldungen(),
)

/** Ein Punkt der Nachbesprechung — Klartext statt Punktzahl. */
@Serializable
data class Befund(
    /** `Lob`, `Hinweis`, `Mangel`. */
    val grad: String = "Hinweis",
    val titel: String = "",
    val text: String = "",
    val incidentId: String? = null,
)

/** Die Marken eines Einsatzes auf der Zeitleiste. Nur der Notruf steht immer fest. */
@Serializable
data class Zeitachse(
    val incidentId: String = "",
    val notruf: String = "",
    val alarm: String? = null,
    val ausgerueckt: String? = null,
    val vorOrt: String? = null,
    val ersteLagemeldung: String? = null,
    val abschluss: String? = null,
)

@Serializable
data class Spielerrekorde(
    val schnellsteAusrueckzeitSekunden: Double? = null,
    val kuerzesteHilfsfristSekunden: Double? = null,
    val kuerzesteHilfsfristStichwort: String? = null,
    val meisteLagemeldungenSchicht: Int = 0,
)

@Serializable
data class Spielerbilanz(
    val playerId: String = "",
    val name: String = "",
    val rolle: String = "Unbestimmt",
    val runden: Int = 0,
    val einsaetze: Int = 0,
    val lagemeldungen: Int = 0,
    val funksprueche: Int = 0,
    val ausrueckzeitSekunden: Double? = null,
    val ausserDienstSekunden: Double = 0.0,
    val rekorde: Spielerrekorde = Spielerrekorde(),
)

/** Was aus den mehrfach gemeldeten Lagen wurde. */
@Serializable
data class Doppelmeldungen(
    val mehrfachGemeldeteLagen: Int = 0,
    val zusammengefuehrt: Int = 0,
    val falschzugeordnet: Int = 0,
    val doppelalarmierungen: Int = 0,
    val nennenswert: Boolean = false,
)

// ---------------------------------------------------------------- Übergabe

/** Eine Zeile des Übergabeprotokolls zu einem offenen Einsatz. */
@Serializable
data class UebergabeEinsatz(
    val einsatznummer: String = "",
    val stichwort: String = "",
    val alterMinuten: Int = 0,
    val hinweise: List<String> = emptyList(),
)

/** Ein Fahrzeug, das der Übernehmende auf dem Schirm haben muss. */
@Serializable
data class UebergabeFahrzeug(
    val funkrufname: String = "",
    val hinweis: String = "",
)

/** Das laufende Übergabeangebot der Leitstelle samt Protokoll. */
@Serializable
data class Uebergabe(
    val vonPlayerId: String = "",
    val vonName: String = "",
    val zielPlayerId: String = "",
    val offeneEinsaetze: List<UebergabeEinsatz> = emptyList(),
    val fahrzeuge: List<UebergabeFahrzeug> = emptyList(),
    val klingelndeAnrufe: Int = 0,
    val wetter: String = "",
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
    /** Ob andere Fahrzeuge diesen Platz per Einzelruf anrufen dürfen. */
    val einzelrufZulassen: Boolean = true,
    /** Bis wann nach einem Funklimit nicht erneut gesendet werden darf. */
    val funkGesperrtBis: String? = null,
    /** In dieser Runde erreichte Dauerlimits. */
    val funkverstoesse: Int = 0,
    /** Ob die getippten Funksprüche dieses Spielers vorgelesen werden. */
    val funkVorlesen: Boolean = false,
    /** Ob dieser Platz gerade einen Stream sendet — eine Ansage, keine Messung. */
    val live: Boolean = false,
    // Wie dieser Spieler aussieht — dieselben Angaben wie überall sonst, vom
    // Server schon gefiltert. Bots tragen die schlichte Fassung.
    val wappen: String = "Keines",
    val wappenfarbe: Int = 0,
    val profilrahmen: String = "keiner",
    val profilbild: String? = null,
    val kopfmuster: String = "keines",
    /** Der Tag der Wache — steht wie der Haken vor dem Namen. */
    val wachentag: String? = null,
    /**
     * Ob für diesen Platz eine Einwilligung in die Übertragung vorliegt. Ohne
     * Streamer-Modus immer `true` — dann gibt es nichts zu erlauben.
     */
    val streamerfreigabe: Boolean = true,
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
    // ------------------------------------------------------------------------
    // Der Rest von `VehicleDto`.
    // ------------------------------------------------------------------------
    /** Ob die Leitstelle diesen Rufnamen selbst vergeben hat — dann gibt es „wieder wie im Buch". */
    val rufnameVonHand: Boolean = false,
    /** Der Träger — `Keine` bei kommunalen Fahrzeugen. */
    val hiOrg: String = "Keine",
    val beschreibung: String = "",
    val schleifen: List<String> = emptyList(),
    /** Ehrenamtlich besetzt — nur dann hängt die Ausrückzeit an der Uhrzeit. */
    val ehrenamtlich: Boolean = false,
    /** Mit welcher Ausrückzeit gerade zu rechnen ist, in Sekunden; 0 heißt „fährt sofort". */
    val ausrueckzeitSekunden: Int = 0,
    val istLuftfahrzeug: Boolean = false,
    /** Warum gerade nicht geflogen wird („Nacht", „Sturm") — `null` heißt flugbereit. */
    val flugHindernis: String? = null,
    /** Funkrufname des Fahrzeugs, in dem der eigene Notarzt gerade mitfährt. */
    val notarztBei: String? = null,
    /** Läuft die Alarmierung: wann die Besatzung im Hof ist. */
    val besatzungAb: String? = null,
    /** Ob dieses Fahrzeug auf Streife ist — unterwegs, ohne Auftrag, jederzeit alarmierbar. */
    val aufStreife: Boolean = false,
    val streifenziel: String? = null,
    val streifenfaehig: Boolean = false,
    /** Die unbesetzt gebliebenen Stellen der Besatzung — leer im Regelfall. */
    val besatzungsluecken: List<Besatzungsluecke> = emptyList(),
    /** Katalog-Id des aufgenommenen Abrollbehälters am WLF. */
    val abrollbehaelterTemplateId: String? = null,
    /** Bis zu diesem Zeitpunkt arbeitet der Hakenarm; vorher ist Status 3 gesperrt. */
    val aufsattelnBis: String? = null,
    /** Seit wann die Leitstelle das Wort erteilt hat — das „J" am Bedienteil. */
    val sprechaufforderungSeit: String? = null,
    val ausserDienstBis: String? = null,
    /** Bis wann die Einsatzbereitschaft wiederhergestellt wird — nicht vorzeitig zu beenden. */
    val wiederherstellungBis: String? = null,
    /** Bis wann kein Funkkontakt zu diesem Fahrzeug besteht. */
    val funklochBis: String? = null,
    /** Zugewiesene Zielklinik; `null`, solange die Leitstelle keine genannt hat. */
    val zielklinikId: String? = null,
    /** Bei Status 7: die Klinik ist erreicht, die Übergabe kann gemeldet werden. */
    val zielklinikErreicht: Boolean = false,
    /** Der Suchabschnitt, den dieses Fahrzeug absucht (0…7). */
    val suchabschnitt: Int? = null,
    /** Die Nummer der Aufgabe, an der dieses Fahrzeug arbeitet. */
    val aufgabe: Int? = null,
    /** Löschwassertank in Litern; 0 heißt, dieses Fahrzeug führt kein Wasser mit. */
    val tankLiter: Int = 0,
    val wasserLiter: Int = 0,
    /** Entnahmestelle, zu der gerade Wasser geholt wird. */
    val entnahmestelleId: String? = null,
    val entnahmestelleErreicht: Boolean = false,
)

/** Eine unbesetzte Stelle auf dem Fahrzeug — `ist` ist die ganze Auskunft beim Atemschutz. */
@Serializable
data class Besatzungsluecke(
    /** `Fuehrung`, `Maschinist`, `Atemschutz`, `Notfallsanitaeter`. */
    val art: String = "",
    val name: String = "",
    val soll: Int = 0,
    val ist: Int = 0,
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
    // ------------------------------------------------------------------------
    // Der Rest von `IncidentDto`.
    // ------------------------------------------------------------------------
    /** Der Einsatz, aus dem diese Lage entstand; `null` bei eigenständigen Einsätzen. */
    val ursprungEinsatzId: String? = null,
    /** Der PagerSpaß-Event-Einsatz, aus dem diese Lage stammt. */
    val eventId: String? = null,
    /** Der Verband, mit dem dieses Stichwort ausrückt — „Löschzug", „Rüstzug". */
    val verband: String? = null,
    /** Die Plätze dieses Verbandes samt dem Fahrzeug, das sie gerade füllt. */
    val zugplaetze: List<Zugplatz> = emptyList(),
    /** Fachabteilung, die der Patient braucht; `null`, wenn jedes Haus ihn aufnimmt. */
    val benoetigteVersorgung: String? = null,
    /** Ein Auftrag von innen — Verpflegung, Transport, Absicherung. */
    val internerAuftrag: Boolean = false,
    /** `Ortsnetz`, `OffeneEntnahme`, `Keine` — erst gesetzt, wenn gemeldet. */
    val wasserlage: String? = null,
    val wasserlageText: String? = null,
    val wasserversorgungSteht: Boolean = false,
    val wasserbedarfProMinute: Int = 0,
    /** Das Sonderobjekt, an dem der Einsatz liegt — `null` bei einer Straßenadresse. */
    val objektName: String? = null,
    val objektart: String? = null,
    val objektgelaende: String? = null,
    val objektartText: String? = null,
    val objektBetroffene: Int = 0,
    /** Der Einsatzplan — leer ohne Objekt. */
    val objektHinweise: List<String> = emptyList(),
    /** Ob diese Lage einen Gefahrenbereich hat (ABC-Lage). */
    val gefahrgutlage: Boolean = false,
    val gefahrstoff: String? = null,
    /** Länge der Ausbreitungsfahne in Windrichtung, in Metern. */
    val ausbreitungMeter: Double = 0.0,
    val messtruppVorOrt: Boolean = false,
    val schaulustige: Boolean = false,
    val drohne: Boolean = false,
    /** Wann der Patient einer bestellten Fahrt abgeholt sein muss. */
    val terminUm: String? = null,
    /** Das angemeldete Zielhaus einer bestellten Fahrt. */
    val zielklinikVorgabe: String? = null,
    /** Wie weit das Suchgebiet insgesamt abgesucht ist, 0 bis 1. */
    val suchfortschritt: Double = 0.0,
    val personGefundenUm: String? = null,
    /** Wie weit die Stelle insgesamt ist, 0 bis 1 — nach Aufwand gewichtet. */
    val arbeitsfortschritt: Double = 0.0,
    val arbeitFertigUm: String? = null,
    /** Wie viel Fläche brennt, in Hektar; 0 bei jeder anderen Lage. */
    val brandflaecheHektar: Double = 0.0,
    val brandflaecheHoechstHektar: Double = 0.0,
    /** Gesammelte, noch nicht weitergereichte Nachforderungen der Kräfte. */
    val offeneNachforderungen: List<OffeneNachforderung> = emptyList(),
    val offeneNachforderungenRd: List<OffeneNachforderung> = emptyList(),
    /** Ob an dieser Lage ein Bereitstellungsraum festgelegt ist. */
    val bereitstellungsraum: Boolean = false,
    /** Die Funkrufnamen, die dort halten. */
    val inBereitstellung: List<String> = emptyList(),
    /** Der Hubschrauberlandeplatz; `null`, solange keiner festgelegt ist. */
    val landeplatz: Landeplatz? = null,
    val lagemeldungen: List<Lagemeldung> = emptyList(),
    val chronologie: List<Chronikeintrag> = emptyList(),
    val eingangUm: String = "",
    val erstAlarmUm: String? = null,
    val erstesFahrzeugVorOrtUm: String? = null,
    val abgeschlossenUm: String? = null,
    val dispositionszeitSekunden: Double? = null,
    val hilfsfristSekunden: Double? = null,
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
    // Die Patientensimulation — alles leer, solange keine läuft. Und alles schon
    // gefiltert: Was nicht gemessen wurde, kommt nicht über die Leitung.
    val simuliert: Boolean = false,
    /** Die gemessenen Werte — je Vitalzeichen sein fertiger Anzeigetext. */
    val werte: Map<String, String>? = null,
    /** Welche Werte vom Normbereich abweichen — vom Server gerechnet. */
    val auffaelligeWerte: List<String>? = null,
    val misstGerade: String? = null,
    val messungFertigUm: String? = null,
    val befunde: List<Befundschema>? = null,
    val massnahmen: List<Patientenmassnahme>? = null,
    val verdachtsdiagnose: String? = null,
    /** Die Diagnose des Notarztes; `null`, solange keiner sie gestellt hat. */
    val diagnose: String? = null,
)

/** Ein abgearbeitetes Schema mit seinen Punkten. */
@Serializable
data class Befundschema(
    val schema: String = "",
    val name: String = "",
    val punkte: List<Befundpunkt> = emptyList(),
)

/** Leerer Befund heißt unauffällig — und wird auch so angezeigt. */
@Serializable
data class Befundpunkt(
    val schluessel: String = "",
    val frage: String = "",
    val befund: String = "",
)

@Serializable
data class Patientenmassnahme(
    val id: String = "",
    val name: String = "",
    val dauer: Int = 0,
    val brauchtArzt: Boolean = false,
    /** Ob sie schon durchgeführt ist. */
    val laeuft: Boolean = false,
    /** Ob sie jetzt geht — falsch etwa, solange der Notarzt fehlt. */
    val moeglich: Boolean = false,
)

/**
 * Ein Platz im Verband — die leeren kommen mit: Die Leitstelle soll sehen,
 * dass die Drehleiter fehlt, und nicht bloß vier statt fünf Fahrzeuge zählen.
 */
@Serializable
data class Zugplatz(
    val bezeichnung: String = "",
    val faehigkeit: String = "",
    val pflicht: Boolean = false,
    val fahrzeugId: String? = null,
    val funkrufname: String? = null,
)

/** Eine gesammelte, noch nicht weitergereichte Nachforderung. */
@Serializable
data class OffeneNachforderung(
    val funkrufname: String = "",
    val text: String = "",
)

@Serializable
data class Lagemeldung(
    val zeit: String = "",
    val funkrufname: String = "",
    val text: String = "",
)

@Serializable
data class Chronikeintrag(
    val zeit: String = "",
    val text: String = "",
    val urheber: String? = null,
)

/**
 * Der Hubschrauberlandeplatz. Fläche und Licht stehen getrennt: Erst wenn beides
 * fertig ist, darf dort nachts gelandet werden.
 */
@Serializable
data class Landeplatz(
    val hergerichtet: Boolean = false,
    val ausgeleuchtet: Boolean = false,
    val fortschritt: Double = 0.0,
    val lichtfortschritt: Double = 0.0,
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

/**
 * Eine Aufgabe des Einsatzarbeitsplans.
 *
 * `begonnen` rechnet der Server: Alles mit kleinerem Rang ist fertig, hier darf
 * gearbeitet werden. `mittelFehlt` und `kraefteFehlen` sind zwei verschiedene
 * Auskünfte — einmal fehlt ein Mittel, einmal fehlen Hände.
 */
@Serializable
data class Aufgabe(
    val name: String = "",
    val fertig: Boolean = false,
    val nummer: Int = 0,
    /** Was sie verlangt; `null` bei Erkundung, Abschluss und Sichtung. */
    val faehigkeit: String? = null,
    /** 0 Erkundung, 1 die Arbeit, 2 Abschluss. */
    val rang: Int = 0,
    val zwingend: Boolean = false,
    val fortschritt: Double = 0.0,
    val begonnen: Boolean = false,
    val fremdgefuehrt: Boolean = false,
    /** Offene Vorgänger in Klartext; `null`, sobald sie begonnen werden darf. */
    val wartetAuf: String? = null,
    /** Zurückgenommen statt erledigt — `fertig` ist dann wahr, `fortschritt` 0. */
    val entfallen: Boolean = false,
    val mittelFehlt: Boolean = false,
    val mindestkraefte: Int = 0,
    val kraefteFehlen: Boolean = false,
)

/** Ein Sektor des Suchgebiets — acht à 45 Grad. */
@Serializable
data class Suchabschnitt(
    val nummer: Int = 0,
    val fortschritt: Double = 0.0,
    /** Die Himmelsrichtung — „Nordost". */
    val name: String = "",
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
    /** Frei verfügbare Behandlungsplätze, nach Fachabteilung. */
    val freieBetten: Map<String, Int> = emptyMap(),
    /** Ein Haus im Umland: außerhalb des Bereichs, aber im Flugradius. */
    val imUmland: Boolean = false,
    /** Luftlinie zum Ausrückebereich in Kilometern — nur bei `imUmland`. */
    val entfernungKm: Double? = null,
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
    // Das Aussehen — gebraucht, wo ein Zuschauer als Person auftritt: in seiner
    // Bitte um einen Platz.
    val wappen: String = "Keines",
    val wappenfarbe: Int = 0,
    val profilrahmen: String = "keiner",
    val profilbild: String? = null,
    val kopfmuster: String = "keines",
    val wachentag: String? = null,
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
    /** Seit wann der offene Schritt ansteht — Grundlage für den zweiten Hinweis. */
    val schrittSeit: String = "",
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
    /**
     * Die Rufgruppe der alarmierten Besatzung („3301 Kreis West") — `null` in
     * einer Runde ohne Gruppen. Auf dem Melder, weil der Alarm der Moment ist,
     * in dem man die Nummer braucht.
     */
    val funkgruppe: String? = null,
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
    /** Bis wann es klingelt — danach ist der Anruf verpasst. */
    val klingeltBis: String = "",
    /** Die Ortung dieses Anrufs; `null`, solange keine angefragt wurde. */
    val ortung: Ortung? = null,
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
    /** Welche Faktenarten nicht erfragt wurden. */
    val fehlend: List<String> = emptyList(),
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

/**
 * Eine Zeile im Lobby-Chat — Spiegel von `LobbynachrichtDto`.
 *
 * <b>Hier standen einmal `absender`, `um` und `istSystem`</b> — Felder, die der
 * Server nie geschickt hat. Der Chat zeigte deshalb jede Zeile ohne Namen.
 */
@Serializable
data class Lobbynachricht(
    val id: String = "",
    val zeit: String = "",
    val vonId: String = "",
    val vonName: String = "",
    val text: String = "",
    /**
     * Spieler-Ids, die im Text per @Name angesprochen sind — vom Server erkannt,
     * damit alle Clients dieselben Erwähnungen sehen (und hören).
     */
    val erwaehnte: List<String> = emptyList(),
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
    /**
     * Gesetzt, wenn der Beitritt **nur** an der Übertragungseinwilligung
     * scheitert. Dann ist nichts kaputt, sondern eine Frage offen — siehe
     * `Uebertragungsfrage` und `Runde.einwilligung`.
     */
    val einwilligung: Uebertragungsfrage? = null,
)
