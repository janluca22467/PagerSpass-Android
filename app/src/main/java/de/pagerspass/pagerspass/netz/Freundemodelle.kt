package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Modelle des Freundebereichs, die `Spielmodelle.kt` noch nicht kannte —
 * übertragen aus `web/src/types.ts`.
 *
 * <b>Eine eigene Datei mit Absicht.</b> Suche, Vorschläge, Geschenke und die
 * Vitrine gehören nur zu den Freunden; in `Spielmodelle.kt` stünden sie zwischen
 * Garage und Katalog, und wer dort sucht, sucht nicht nach ihnen.
 *
 * <b>Alles mit Vorgabe</b> — dieselbe Regel wie nebenan: `ignoreUnknownKeys`
 * fängt neue Felder ab, Vorgaben fehlende.
 */

/**
 * Ein Treffer der Suche über den eindeutigen Benutzernamen.
 *
 * `level` und `rang` fehlen bei einem privaten Konto — gefunden werden darf es,
 * sein Stand bleibt unter Freunden. `stand` ist `null`, solange man sich nicht
 * kennt; sonst `Bestaetigt`, `Angefragt` oder `Blockiert`.
 */
@Serializable
data class Suchtreffer(
    val kennung: String = "",
    val benutzername: String = "",
    val anzeigename: String = "",
    val level: Int? = null,
    val rang: String? = null,
    val stand: String? = null,
    val profilbild: String? = null,
    val premium: Boolean = false,
    val teammitglied: Boolean = false,
    val wachentag: String? = null,
)

/** Jemand, mit dem man schon gefahren ist, aber noch nicht befreundet. */
@Serializable
data class Vorschlag(
    val kennung: String = "",
    val benutzername: String = "",
    val anzeigename: String = "",
    val level: Int = 0,
    val rang: String = "",
    val zuletztZusammen: String = "",
    val gemeinsameSchichten: Int = 0,
    val profilbild: String? = null,
    val premium: Boolean = false,
    val teammitglied: Boolean = false,
    val wachentag: String? = null,
)

/** Was beim Öffnen eines Geschenks herauskam — erst hier steht, was drin war. */
@Serializable
data class Geschenkinhalt(
    val artikelId: String = "",
    val artikelName: String = "",
    val vonName: String = "",
)

/** Ein Abzeichen in der Vitrine eines Profils. */
@Serializable
data class VitrinenAbzeichen(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String = "",
    val kategorie: String = "",
)

/** Die Antwort auf eine Quittung: steht sie jetzt, und wie viele sind es. */
@Serializable
data class Quittungsstand(
    val quittiert: Boolean = false,
    val anzahl: Int = 0,
)

/**
 * Die Antwort auf eine Anfrage — `Angefragt`, oder gleich `Bestaetigt`, wenn der
 * andere schon gefragt hatte.
 */
@Serializable
data class Anfragestand(val stand: String = "")
