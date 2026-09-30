package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Modelle rund ums Konto — Premium, Postfach, Werbung, Codes, Geschenke,
 * Sitzungen, Einwilligungen und Discord. Übertragen aus `web/src/types.ts`.
 *
 * <b>Eigene Datei, weil es ein eigener Bereich ist.</b> `Spielmodelle.kt` trägt,
 * was die sechs Seiten der Tableiste lesen; hier steht, was die Konto-Zentrale und
 * der Shop darüber hinaus brauchen. Dieselben Regeln: alles mit Vorgabe, nur was
 * auch gezeigt wird.
 */

// -------------------------------------------------------------------- Premium

/**
 * Der Stand des Abos — `GET /api/konto/{k}/premium` (`PremiumDto` am Server).
 *
 * <b>Gekauft wird er hier nie.</b> Die App liest den Stand und zeigt, was dazu
 * gehört; Abschließen, Verwalten und Kündigen führen auf die Webseite. Deshalb
 * fehlt `publishableKey` absichtlich — die App hat keinen Grund, ihn zu kennen.
 *
 * @param status `Free`, `Active`, `PastDue` oder `Canceled`.
 * @param plan `None`, `Monthly` oder `Yearly`.
 * @param bis Das Ende des bezahlten Zeitraums. Es bleibt auch nach dem Ende des
 *   Abos stehen — ein Datum sagt nur etwas, solange `aktiv` gilt.
 */
@Serializable
data class Premiumstand(
    val status: String = "Free",
    val plan: String = "None",
    val aktiv: Boolean = false,
    val seit: String? = null,
    val bis: String? = null,
    val kaufVerfuegbar: Boolean = false,
    val monatspreisCent: Int = 0,
    val jahrespreisCent: Int = 0,
    val grenzen: Premiumgrenzen = Premiumgrenzen(),
    val vorteile: List<String> = emptyList(),
) {
    /** Eine Zahlung ist offen — die Vorteile ruhen, gekündigt ist nichts. */
    val zahlungOffen: Boolean get() = status == "PastDue"
}

/** Wie viel Platz ein Premium-Konto hat — die Zahlen stehen im Schaufenster. */
@Serializable
data class Premiumgrenzen(
    val aaoVorlagen: Int = 0,
    val szenarien: Int = 0,
    val leitstellen: Int = 0,
    val rundenvorlagen: Int = 0,
)

// ------------------------------------------------------------------- Postfach

/**
 * Die E-Mail-Adresse des Kontos — `GET /api/konto/{k}/postfach`.
 *
 * <b>Nicht zu verwechseln mit der Postfach-Seite der App</b>, die die
 * Nachrichten des Betriebs zeigt. Der Name kommt vom Server; gemeint ist hier die
 * Adresse, ihre Bestätigung, der Newsletter und der alte Anmeldeschutz.
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
    /** Ob der Server überhaupt Post verschicken kann — sonst kommt kein Code an. */
    val versandbereit: Boolean = true,
)

/** Die Antwort auf das Eintragen einer Adresse: der neue Stand und ein Satz dazu. */
@Serializable
data class Postfachantwort(
    val postfach: Postfach = Postfach(),
    val hinweis: String? = null,
)

// ------------------------------------------------------------------- Werbung

/** Der Werbe-Abschnitt: eigener Code, Geworbene, und ob man selbst noch einlösen darf. */
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
    /** Was eine Werbung jetzt einbringt — fertig gerechnet vom Server. */
    val creditsJetzt: Int = 0,
)

// --------------------------------------------------------------- Codes, Shop

/** Was ein eingelöster Aktionscode gebracht hat — samt frischem Shop-Stand. */
@Serializable
data class Codeertrag(
    /** Fertig formuliert: „200 Credits", „Melder-Gesicht Kupferglanz". */
    val text: String = "",
    val credits: Int = 0,
    val wahlen: Int = 0,
    val artikelId: String? = null,
    val shop: Shop = Shop(),
)

/** Die öffentliche Seite eines Creator-Codes — `GET /api/code/{code}`. */
@Serializable
data class Codevorschau(
    val code: String = "",
    val creator: String = "",
    val kanal: String? = null,
    val belohnung: String = "",
    val einloesbar: Boolean = false,
    val grund: String? = null,
    val gueltigBis: String? = null,
)

/** Das Fahrzeug des Tages im Shop. */
@Serializable
data class Tagesangebot(
    val templateId: String = "",
    val typ: String = "",
    val preis: Int = 0,
    val regulaer: Int = 0,
)

/** Ein Dienstauftrag der Woche. `erfuellt` kommt vom Server, statt es zu erraten. */
@Serializable
data class Auftrag(
    val id: String = "",
    /** `Leicht`, `Mittel` oder `Schwer`. */
    val stufe: String = "",
    val text: String = "",
    val stand: Int = 0,
    val ziel: Int = 1,
    val betrag: Int = 0,
    val erfuellt: Boolean = false,
)

/** Der Wochendeckel der Zulagen. */
@Serializable
data class Zulagen(
    val verbraucht: Int = 0,
    val deckel: Int = 0,
    val naechsterWechsel: String? = null,
)

/** Eine Zeile des Credit-Auszugs. */
@Serializable
data class Creditposten(
    val grund: String = "",
    val text: String = "",
    val betrag: Int = 0,
    val um: String = "",
)

/** Ein Freund in der Schenkliste — mit der Auskunft, ob die Wartezeit um ist. */
@Serializable
data class Schenkfreund(
    val kennung: String = "",
    val anzeigename: String = "",
    val benutzername: String = "",
    val darfEmpfangen: Boolean = false,
    val wartetage: Int = 0,
)

// `Geschenkinhalt` (was beim Öffnen eines Geschenks herauskam) steht in Freundemodelle.kt.

// ------------------------------------------------------ Datenschutz, Sitzungen

/** Eine offene Sitzung dieses Kontos. `diese` ist die, aus der gefragt wird. */
@Serializable
data class Sitzungsuebersicht(
    val erstelltUm: String = "",
    val zuletztGenutztUm: String = "",
    val diese: Boolean = false,
)

/** Eine erteilte Übertragungseinwilligung, wie die Privatsphäre sie zeigt. */
@Serializable
data class Einwilligung(
    val id: String = "",
    val streamerName: String = "",
    val plattform: String = "",
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

/** Die Antwort auf die Frage nach der Aufzeichnung. `null` heißt: noch nicht gefragt. */
@Serializable
data class Analysestand(
    val zugestimmt: Boolean? = null,
    val entschiedenUm: String? = null,
)

/** Ein Schalter mit einem einzigen Wert — die Patientensimulation. */
@Serializable
data class Schalterstand(val an: Boolean = false)

/** Eine Auskunft in einem Satz — „Passwort vergessen" antwortet so. */
@Serializable
data class Satz(val meldung: String = "")

/** Eine Adresse, an die der Browser soll — der Start der Discord-Verknüpfung. */
@Serializable
data class Adresse(val url: String = "")

/** Der Stand der Discord-Verknüpfung. */
@Serializable
data class Discordstatus(
    val verknuepft: Boolean = false,
    val discordName: String? = null,
    val verknuepftAm: String? = null,
)

/** Wie die Plattformen einer Übertragung heißen — `STREAMERPLATTFORM_LABEL` im Web. */
fun plattformname(plattform: String): String = when (plattform) {
    "Twitch" -> "Twitch"
    "YouTube" -> "YouTube"
    "TikTok" -> "TikTok"
    "Kick" -> "Kick"
    "Discord" -> "Discord"
    "Andere" -> "Anderswo"
    else -> plattform.ifBlank { "Übertragung" }
}
