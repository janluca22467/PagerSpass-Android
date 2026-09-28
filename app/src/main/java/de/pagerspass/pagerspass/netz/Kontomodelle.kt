package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Modelle des Kontobereichs — Premium, Postfach, Werbung, Einwilligungen,
 * Sitzungen, Discord, Codes und Verträge. Übertragen aus `web/src/types.ts` und
 * `web/src/api/rest.ts`.
 *
 * <b>Alles mit Vorgabe</b>, wie in `Spielmodelle.kt`: Ein Feld, das der Server
 * (noch) nicht schickt, darf die Seite nicht zum Absturz bringen — es zeigt dann
 * eben weniger.
 *
 * <b>Aufzählungen stehen als Zeichenkette.</b> Der Server schreibt sie als Namen
 * (`JsonStringEnumConverter`): `"Active"`, `"Twitch"`, `"Kuendigung"`. Als Kotlin-
 * `enum` brächte ein neuer Wert des Servers die ganze Antwort zu Fall.
 */

// -------------------------------------------------------------------- Premium

// Der Stand des Abos steht als `Abostand` in `Shopmodelle.kt` — Laden und
// Kontozentrale lesen dieselbe Antwort.

/** Eine Adresse, an die der Browser weitergeht — Kasse, Abo-Verwaltung, Discord. */
@Serializable
data class Weiterleitung(val url: String = "")

// ------------------------------------------------------------------- Postfach

/**
 * Die E-Mail-Adresse des Kontos.
 *
 * <b>Seit 5.0.0.58 Pflicht</b> — sie ist der Weg zurück, wenn das Passwort
 * vergessen ist. `versandbereit` sagt, ob der Server überhaupt Post verschicken
 * kann; ohne Postausgang (im Entwicklungsbetrieb der Normalfall) verspricht die
 * Seite keinen Code, der nie ankommt.
 */
@Serializable
data class Postfach(
    val email: String? = null,
    val bestaetigt: Boolean = false,
    val bestaetigtUm: String? = null,
    val newsletter: Boolean = false,
    val newsletterSeit: String? = null,
    val zweiFaktorAktiv: Boolean = false,
    val freigeschaltet: Boolean = true,
    val versandbereit: Boolean = true,
)

/** Die Antwort auf das Eintragen einer Adresse: der neue Stand und ein Satz dazu. */
@Serializable
data class Postfachantwort(
    val postfach: Postfach = Postfach(),
    val hinweis: String? = null,
)

/** Ein Schalter, wie der Server ihn zurückgibt — `{ an }`. */
@Serializable
data class Schalterstand(val an: Boolean = false)

/** Ein Satz vom Server — „Passwort vergessen", „Passwort neu". */
@Serializable
data class Serversatz(val meldung: String = "")

/** Die Antwort auf den Abmeldelink des Newsletters. */
@Serializable
data class Newsletterabmeldung(
    val abgemeldet: Boolean = false,
    val meldung: String = "",
)

// ------------------------------------------------------------------- Werbung

/**
 * Der Werbestand: eigener Code, Geworbene, und ob man selbst noch einlösen darf.
 *
 * `creditsJetzt` kommt fertig gerechnet (Grundbetrag mal Aktionsfaktor) — die
 * Multiplikation steht nicht an zwei Stellen.
 */
@Serializable
data class Werbung(
    /** Schon in Anzeigeform (`H7KM-3QX4`); `null`, solange keiner erzeugt wurde. */
    val code: String? = null,
    val geworben: Int = 0,
    val ausgezahlt: Int = 0,
    val verdienteCredits: Int = 0,
    val creditsJeWerbung: Int = 0,
    val deckel: Int = 0,
    val fenstertage: Int = 0,
    val darfEinloesen: Boolean = false,
    val einloesbarBis: String? = null,
    val geworbenVon: String? = null,
    val aktionTitel: String? = null,
    val aktionFaktor: Int = 1,
    val aktionBis: String? = null,
    val creditsJetzt: Int = 0,
)

// ------------------------------------------------------------ Datenschutz

/** `zugestimmt: null` heißt: noch nicht gefragt. */
@Serializable
data class Analysestand(
    val zugestimmt: Boolean? = null,
    val entschiedenUm: String? = null,
)

/** Eine offene Sitzung dieses Kontos — ohne Gerät und ohne Ort. */
@Serializable
data class Sitzungsuebersicht(
    val erstelltUm: String = "",
    val zuletztGenutztUm: String = "",
    val diese: Boolean = false,
)

// ---------------------------------------------------- Übertragung (Streamer)

/**
 * Eine erteilte (oder widerrufene, abgelaufene) Einwilligung in die Übertragung.
 *
 * <b>`gilt` ist nicht `widerrufenUm == null`.</b> Bei einer Selbstauskunft
 * „unter 18" steht die Zeile, trägt aber nichts, solange der Bogen der
 * Erziehungsberechtigten fehlt.
 */
@Serializable
data class Einwilligung(
    val id: String = "",
    val streamerName: String = "",
    val plattform: String = "Andere",
    val kanal: String = "",
    val aufzeichnung: Boolean = false,
    val erteiltUm: String = "",
    val laeuftAbUm: String = "",
    val volljaehrig: Boolean = false,
    val gilt: Boolean = false,
    val wartetAufEltern: Boolean = false,
    val elternzustimmungUm: String? = null,
    val widerrufenUm: String? = null,
    val fassung: String = "",
    val elternbogen: String? = null,
)

/** Wie eine Plattform heißt — `STREAMERPLATTFORM_LABEL` aus `types.ts`. */
fun plattformname(roh: String): String = when (roh) {
    "Twitch" -> "Twitch"
    "YouTube" -> "YouTube"
    "TikTok" -> "TikTok"
    "Kick" -> "Kick"
    "Discord" -> "Discord"
    "Andere" -> "Anderswo"
    else -> roh
}

// -------------------------------------------------------------------- Discord

/** Ob dieses Konto mit Discord verknüpft ist, und falls ja, mit wem. */
@Serializable
data class DiscordStatus(
    val verknuepft: Boolean = false,
    val discordName: String? = null,
    val verknuepftAm: String? = null,
)

// ---------------------------------------------------------------------- Codes

/** Die öffentliche Auskunft zu einem Creator-Code — ohne Anmeldung. */
@Serializable
data class Codevorschau(
    val code: String = "",
    val creator: String = "",
    val kanal: String? = null,
    /** Fertig formuliert: „200 Credits und 1 Fahrzeug-Gutschein". */
    val belohnung: String = "",
    val einloesbar: Boolean = false,
    val grund: String? = null,
    val gueltigBis: String? = null,
)

/**
 * Was ein eingelöster Code gebracht hat.
 *
 * Der Shop-Stand kommt als rohes JSON mit: Das Shop-Modell gehört einem anderen
 * Bereich der App, und hier wird nur der Credit-Stand daraus gelesen.
 */
@Serializable
data class Codeertrag(
    val text: String = "",
    val credits: Int = 0,
    val wahlen: Int = 0,
    val artikelId: String? = null,
    val shop: kotlinx.serialization.json.JsonObject? = null,
) {
    /** Der Credit-Stand nach dem Einlösen — oder `null`, wenn keiner mitkam. */
    val neueCredits: Int?
        get() = (shop?.get("credits") as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toIntOrNull()
}

// ------------------------------------------------------------------- Verträge

/** Eine Zeile der Quittung — Feld und Wert, wie eingegeben. */
@Serializable
data class Quittungszeile(val feld: String = "", val wert: String = "")

/**
 * Was nach einer Kündigung oder einem Widerruf zurückkommt.
 *
 * <b>Für jeden dieselbe Form</b> — sie sagt nicht, ob es das Konto gibt. `art`
 * ist `Kuendigung` oder `Widerruf`; `eingang` ist Datum und Uhrzeit in deutscher
 * Zeit, fertig gesetzt.
 */
@Serializable
data class Erklaerungsquittung(
    val id: String = "",
    val art: String = "",
    val eingangUm: String = "",
    val eingang: String = "",
    val inhalt: List<Quittungszeile> = emptyList(),
    val meldung: String = "",
) {
    val istKuendigung: Boolean get() = art.equals("Kuendigung", ignoreCase = true)
}

// -------------------------------------------------------------------- Profil

/**
 * Der Kartenausschnitt hinter dem Profilkopf — das Abo-Muster „Eigene Lage".
 *
 * Die Vorgabe ist Deutschland und ausdrücklich nicht der eigene Standort: danach
 * fragt hier nichts.
 */
@Serializable
data class Kartenausschnitt(
    val lat: Double = 51.2,
    val lon: Double = 10.4,
    val zoom: Int = 6,
) {
    companion object {
        /** `KARTENKOPF_STANDARD` aus `types.ts`. */
        val STANDARD = Kartenausschnitt()

        /** Näher als eine Ortsansicht geht es nicht — dort soll eine Gegend stehen, keine Anschrift. */
        const val MAX_ZOOM = 14
        const val MIN_ZOOM = 4

        /** Das Muster, zu dem der Ausschnitt gehört. */
        const val MUSTER = "premium-karte"
    }
}
