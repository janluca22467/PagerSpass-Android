package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/*
 * Die Rümpfe der eigenen Fahrzeug-Icons — Spiegel von `Endpunkte/IconEndpunkte.cs`
 * und den Typen `Iconpack`, `Packicon`, `Blaulicht`, `Icontyp`, `IconGrenzen`,
 * `Packimport` in `web/src/types.ts`.
 */

/** Ein Pack — eine Ersetzungstabelle „Fahrzeugtyp → Grafik". */
@Serializable
data class Iconpack(
    val id: String = "",
    val name: String = "",
    val code: String? = null,
    val belegt: Int = 0,
    val bytes: Long = 0,
    val gesperrt: Boolean = false,
    /** Ob dieses Pack gerade auf der Karte gilt — die Antwort kommt vom Server, nicht aus Raten. */
    val aktiv: Boolean = false,
    val geaendertUm: String = "",
)

/**
 * Ein Blaulicht auf einem eigenen Icon — Mittelpunkt in 0–1 auf dem Bild, von
 * links bzw. von oben. Alles außer der Lage ist wahlfrei, und `null` heißt immer
 * „die Vorgabe".
 */
@Serializable
data class Blaulichtpunkt(
    val x: Double = 0.5,
    val y: Double = 0.5,
    val b: Double? = null,
    val h: Double? = null,
    val form: String? = null,
    val art: String? = null,
    val takt: String? = null,
    val farbe: String? = null,
)

/** Ein Icon eines Packs — `bild` ist die Adresse unter `/api/icon/…`. */
@Serializable
data class Packicon(
    val vorlageId: String = "",
    val bild: String = "",
    val breite: Int? = null,
    val hoehe: Int? = null,
    /**
     * Ob das Icon sich mit dem Kurs dreht. Vorgabe ist **nein**: Ein Bild hat eine
     * Leserichtung und keine Fahrtrichtung — mitgedreht stünde es auf dem Kopf,
     * sobald das Fahrzeug nach Süden fährt.
     */
    val dreht: Boolean = false,
    val blaulichter: List<Blaulichtpunkt> = emptyList(),
)

/** Ein Fahrzeugtyp, für den ein Icon hinterlegt werden kann — aus dem Weltkatalog. */
@Serializable
data class Icontyp(
    val vorlageId: String = "",
    val typ: String = "",
    val organisation: String = "",
    val kategorie: String = "",
    val beschreibung: String = "",
)

/** Die Grenzen — vom Server, damit sie nicht driften. */
@Serializable
data class IconGrenzen(
    val maxPacks: Int = 5,
    val maxBytesJeIcon: Long = 256 * 1024,
    val maxBytesJePack: Long = 8L * 1024 * 1024,
    val minKante: Int = 16,
    val maxKante: Int = 512,
    val maxNameLaenge: Int = 40,
    val maxBlaulichter: Int = 8,
    val blaulichtArten: List<String> = emptyList(),
    val blaulichtFormen: List<String> = emptyList(),
    val blaulichtTakte: List<String> = emptyList(),
    val blaulichtFarben: List<String> = emptyList(),
    val erlaubteTypen: List<String> = emptyList(),
)

@Serializable
data class PackimportHinweis(val vorlageId: String = "", val grund: String = "")

/** Das Ergebnis eines Zip-Imports: das neue Pack und was dabei übersprungen wurde. */
@Serializable
data class Packimport(
    val pack: Iconpack = Iconpack(),
    val uebernommen: Int = 0,
    val uebersprungen: List<PackimportHinweis> = emptyList(),
)

@Serializable
data class Packname(val name: String)

@Serializable
data class Packcode(val code: String = "")

/** `null` heißt: zurück auf die gezeichneten Fahrzeuge des Spiels. */
@Serializable
data class Packwahl(val packId: String?)

@Serializable
data class Icondrehung(val dreht: Boolean)

@Serializable
data class Iconblaulicht(val punkte: List<Blaulichtpunkt>)
