package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Modelle rund um den Weg in eine Runde — Rundenvorlagen, Wachen,
 * Startkacheln, Fußzeile, Umfrage, Wartung. Übertragen aus `web/src/types.ts`;
 * die Namen der Felder sind die des Servers.
 *
 * <b>Alles mit Vorgabe</b>, aus demselben Grund wie überall: Ein Feld, das der
 * Server neu mitschickt oder weglässt, darf den Startbildschirm nicht umwerfen.
 */

// ------------------------------------------------------------ Rundenvorlagen

/**
 * Eine gespeicherte Rundenvorlage — der Reglerstand einer Lobby, per Code
 * weiterzugeben. Nicht zu verwechseln mit dem Leitstellenbau: Der baut den Ort
 * um, diese merkt sich die Regeln.
 */
@Serializable
data class Rundenvorlagenzeile(
    val id: String = "",
    val name: String = "",
    /** Der Landkreis; `null` = erfundener Standardbereich. */
    val landkreisId: String? = null,
    /** Die disponierende Leitstelle; `null` = die erste des Kreises. */
    val leitstelleId: String? = null,
    /** Ob der ganze Leitstellenbereich bespielt wird. */
    val ganzerBereich: Boolean = false,
    /** Der sechsstellige Code zum Weitergeben. */
    val code: String = "",
    val geaendertUm: String = "",
)

/**
 * Ein Fahrzeug der gespeicherten Aufstellung. Rufname und Wache stehen daneben,
 * weil beide in der Lobby von Hand gesetzt werden; `null` heißt „nichts
 * eingestellt" — dann vergibt die eröffnete Runde es selbst.
 */
@Serializable
data class Vorlagenbesatzung(
    val vorlageId: String = "",
    val funkrufname: String? = null,
    val kurzname: String? = null,
    val wacheKennung: String? = null,
)

/** Was der Vorlagen-Editor bearbeitet: Name, Plätze, Aufstellung. */
@Serializable
data class Vorlageninhalt(
    val name: String = "",
    val landkreisId: String? = null,
    val maxSpieler: Int? = null,
    val bestand: List<Vorlagenbesatzung> = emptyList(),
)

// -------------------------------------------------------------------- Wachen

/** Eine echte Wache eines Landkreises — aus `/api/landkreise/{id}/wachen`. */
@Serializable
data class Wache(
    /** Der Schlüssel, unter dem eine gespeicherte Auswahl sie wiederfindet. */
    val kennung: String = "",
    val name: String = "",
    val organisation: String = "",
    /** Wer sie betreibt — `Keine` bei kommunalen Wachen. */
    val traeger: String = "Keine",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    /** Ob hier in einer Runde ohne eigene Wachenwahl Fahrzeuge stehen — vom Server gerechnet. */
    val bespielt: Boolean = false,
    /** Die Nummer im Funkrufnamen — ihr Platz in der Reihe. 0, wenn nicht bespielt. */
    val wachnummer: Int = 0,
)

/**
 * Eine Wache, wie sie in *dieser* Runde dasteht — aus `/api/rooms/{code}/wachen`.
 * Sie kennt den Sandkasten; gerechnet wird am Server.
 */
@Serializable
data class Raumwache(
    val kennung: String = "",
    val name: String = "",
    val organisation: String = "",
    /** Die Zahl vor dem ersten Schrägstrich, die ein Fahrzeug hier tragen würde. */
    val zugnummer: Int = 0,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    /** Die Fahrzeuge dieser Runde, die auf diesem Hof stehen — Ids. */
    val fahrzeuge: List<String> = emptyList(),
)

/** Ein Stichwort-Set für die Lobby — nur der Kopf. */
@Serializable
data class Stichwortset(
    val id: String = "",
    val name: String = "",
    /** Das Bundesland, für das es gilt; `null` bei einem freien Set. */
    val bundesland: String? = null,
    /** Wie viele Lagen es führt. */
    val lagen: Int = 0,
)

/**
 * Eine echte Leitstelle mit dem Gebiet, das sie disponiert — aus dem Katalog.
 * `kreise` sind Landkreis-Kennungen: meist mehrere.
 */
@Serializable
data class Leitstelle(
    val id: String = "",
    val name: String = "",
    val bundesland: String = "",
    val sitz: String = "",
    val kreise: List<String> = emptyList(),
)

// --------------------------------------------------------- Startbildschirm

/**
 * Eine frei konfigurierbare Kachel im Menü — neben den festen Wegen. Die trägt
 * die Verwaltung ein: eine Runde zum gemeinsamen Beitreten, ein Termin, eine Seite.
 */
@Serializable
data class Startkachel(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String? = null,
    /** Der fertige SVG-Pfad in einem 24×24-Feld. */
    val pfad: String = "",
    /** `route`, `raum` oder `link`. */
    val aktion: String = "link",
    /** Ein Weg im Spiel, ein sechsstelliger Raumcode oder eine Adresse. */
    val ziel: String = "",
    /** Ob Premium nötig ist — dann führt sie für freie Konten in den Shop. */
    val premiumNoetig: Boolean = false,
    /** Die kleine Marke rechts („Neu", „Heute"). */
    val marke: String? = null,
)

/** Ein frei konfigurierbarer Knopf in der Fußzeile — nur ein Zeichen. */
@Serializable
data class Footerknopf(
    val beschriftung: String = "",
    val ziel: String = "",
    /** Die `d`-Angabe eines SVG-Pfades in einem 24×24-Feld. */
    val pfad: String = "",
    /** `beide`, `mobil` oder `desktop`. */
    val geraete: String = "beide",
)

/** Eine offene Umfrage der Betreiber; die Antwort ist der Index in die Optionen. */
@Serializable
data class OffeneUmfrage(
    val id: String = "",
    val frage: String = "",
    val optionen: List<String> = emptyList(),
)

@Serializable
internal data class Umfrageumschlag(val umfrage: OffeneUmfrage? = null)

// ------------------------------------------------------------------- Wartung

/**
 * Eine angekündigte Wartung — sie sperrt noch nichts, sie kommt nur.
 * `beginntInSekunden` rechnet der Server, nicht die Uhr des Handys.
 */
@Serializable
data class Wartungsankuendigung(
    val titel: String = "",
    val begruendung: String = "",
    val beginnUm: String = "",
    val endeUm: String = "",
    val beginntInSekunden: Int = 0,
    val dauerMinuten: Int = 0,
)

/** Die Antwort von `/api/wartung` — beide `null` heißt: offen, nichts in Sicht. */
@Serializable
data class Wartungsumschlag(
    val wartung: Wartungsstand? = null,
    val geplant: Wartungsankuendigung? = null,
)
