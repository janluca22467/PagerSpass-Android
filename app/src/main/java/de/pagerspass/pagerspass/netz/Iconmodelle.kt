package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Rümpfe der eigenen Fahrzeug-Icons für World — übertragen aus
 * `web/src/types.ts` (Iconpack, Packicon, Blaulicht, Icontyp, Packimport,
 * IconGrenzen) und gegengelesen an `Endpunkte/IconEndpunkte.cs`.
 *
 * <b>Was ein Pack ist.</b> Fahrzeuge werden im Spiel nicht als Bild geladen,
 * sondern gezeichnet. Ein Pack ist eine **Ersetzungstabelle**: Es tritt an diese
 * Stelle, aber nur dort, wo es etwas hinterlegt hat. `belegt` ist deshalb keine
 * Vollständigkeitsangabe, sondern eine Auskunft.
 */

/** Ein Icon-Pack in der Bibliothek. */
@Serializable
data class Iconpack(
    val id: String = "",
    val name: String = "",
    val code: String? = null,
    val belegt: Int = 0,
    val bytes: Long = 0,
    val gesperrt: Boolean = false,
    /** Ob dieses Pack gerade auf der Karte gilt — die Antwort kommt vom Server. */
    val aktiv: Boolean = false,
    val geaendertUm: String = "",
)

/**
 * Ein einzelnes Icon.
 *
 * `bild` ist ein Pfad unter `/api/icon/…` — ohne Anmeldung abrufbar, weil ein
 * `<img>` im Web keine Kopfzeile mitschickt. Hier wird er vor den eingestellten
 * Server gesetzt (siehe `iconAdresse`).
 */
@Serializable
data class Packicon(
    val vorlageId: String = "",
    val bild: String = "",
    val breite: Int? = null,
    val hoehe: Int? = null,
    /**
     * Ob das Icon sich mit dem Kurs des Fahrzeugs dreht. Vorgabe ist **nein**: Ein
     * hochgeladenes Bild hat eine Leserichtung und keine Fahrtrichtung.
     */
    val dreht: Boolean = false,
    /** Leer heißt: Bei Sondersignal blitzt das ganze Bild. */
    val blaulichter: List<Blaulicht> = emptyList(),
)

/**
 * Ein Blaulicht auf einem eigenen Icon — Mittelpunkt in 0–1 auf dem Bild, von
 * links bzw. von oben. Alles außer der Position ist optional, und `null` heißt
 * immer „die Vorgabe": rund, Standardgröße, Muster der Anlage, Takt a, blau.
 */
@Serializable
data class Blaulicht(
    val x: Double = 0.5,
    val y: Double = 0.5,
    /** Breite als Anteil der Bildbreite — `null`: der Standardpunkt. */
    val b: Double? = null,
    /** Höhe als Anteil der Bildhöhe — `null`: der Standardpunkt. */
    val h: Double? = null,
    /** `rund` oder `eckig`. */
    val form: String? = null,
    /** `doppel`, `vierfach` oder `rundum` — `null`: wie das Fahrzeug. */
    val art: String? = null,
    /** `a` oder `b` — b blitzt eine halbe Periode nach a. */
    val takt: String? = null,
    /** `blau` oder `gelb`. */
    val farbe: String? = null,
)

/** Ein Fahrzeugtyp, für den ein Icon hinterlegt werden kann — je Typ genau einer. */
@Serializable
data class Icontyp(
    val vorlageId: String = "",
    val typ: String = "",
    val organisation: String = "",
    val kategorie: String = "",
    val beschreibung: String = "",
)

/** Was beim Zip-Import übersprungen wurde — und warum. */
@Serializable
data class Packimporthinweis(
    val vorlageId: String = "",
    val grund: String = "",
)

/** Das Ergebnis eines Zip-Imports: das neue Pack und der Bericht dazu. */
@Serializable
data class Packimport(
    val pack: Iconpack = Iconpack(),
    val uebernommen: Int = 0,
    val uebersprungen: List<Packimporthinweis> = emptyList(),
)

/** Die Grenzen der Icon-Packs — vom Server, damit sie nicht driften. */
@Serializable
data class IconGrenzen(
    val maxPacks: Int = 0,
    val maxBytesJeIcon: Long = 0,
    val maxBytesJePack: Long = 0,
    val minKante: Int = 0,
    val maxKante: Int = 0,
    val maxNameLaenge: Int = 40,
    /** Wie viele Blaulichter ein Icon höchstens trägt. */
    val maxBlaulichter: Int = 8,
    /** Die wählbaren Blitzmuster — die Vorgabe „wie das Fahrzeug" steht nicht darin. */
    val blaulichtArten: List<String> = emptyList(),
    val blaulichtFormen: List<String> = emptyList(),
    val blaulichtTakte: List<String> = emptyList(),
    val blaulichtFarben: List<String> = emptyList(),
    val erlaubteTypen: List<String> = emptyList(),
)

/** Die volle Adresse eines Icons — der Pfad kommt ohne Server. */
fun iconAdresse(server: String, bild: String): String =
    if (bild.startsWith("http://") || bild.startsWith("https://")) bild
    else server.trimEnd('/') + "/" + bild.trimStart('/')
