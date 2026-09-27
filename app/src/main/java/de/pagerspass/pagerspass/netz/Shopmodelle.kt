package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Was der Shop über Sortiment und Tagesbonus hinaus zeigt — übertragen aus
 * `web/src/types.ts`.
 *
 * `Shop` selbst steht in `Spielmodelle.kt` und trägt diese Teile als Felder mit
 * Vorgabe; hier stehen nur ihre Formen, damit die Datei der Seitenmodelle nicht
 * mit jeder Ladenecke wächst.
 */

/** Das Fahrzeug des Tages — sein Preis ist bereits der ermäßigte. */
@Serializable
data class Tagesangebot(
    val templateId: String = "",
    val typ: String = "",
    val preis: Int = 0,
    val regulaer: Int = 0,
)

/**
 * Ein Dienstauftrag der Woche.
 *
 * <b>`erfuellt` kommt vom Server</b>, statt es aus Stand und Ziel zu erraten: Die
 * Karte zeigt bei Erfüllung etwas anderes als einen vollen Balken, und diese
 * Entscheidung gehört nicht in die Ansicht.
 */
@Serializable
data class Auftrag(
    val id: String = "",
    /** `Leicht`, `Mittel`, `Schwer`. */
    val stufe: String = "Leicht",
    val text: String = "",
    val stand: Int = 0,
    val ziel: Int = 0,
    val betrag: Int = 0,
    val erfuellt: Boolean = false,
)

/**
 * Der Wochendeckel der Zulagen: Alle Zulagen zusammen bringen je Woche höchstens
 * `deckel` Credits — die Regel steht am Server in `Praemien.cs`.
 */
@Serializable
data class Zulagen(
    val verbraucht: Int = 0,
    val deckel: Int = 0,
    val naechsterWechsel: String = "",
)

/** Eine Zeile des Credit-Auszugs — Gutschrift positiv, Kauf negativ. */
@Serializable
data class Creditposten(
    val grund: String = "",
    val text: String = "",
    val betrag: Int = 0,
    val um: String = "",
)

/** Was ein eingelöster Aktionscode gebracht hat — samt frischem Shop-Stand. */
@Serializable
data class Codegutschrift(
    /** Fertig formuliert: „200 Credits", „Melder-Gesicht Kupferglanz". */
    val text: String = "",
    val credits: Int = 0,
    val wahlen: Int = 0,
    val artikelIds: List<String> = emptyList(),
    val shop: Shop = Shop(),
)

/** Ein Freund in der Schenkliste — mit der Auskunft, ob die Wartezeit um ist. */
@Serializable
data class Beschenkbarer(
    val kennung: String = "",
    val anzeigename: String = "",
    val benutzername: String = "",
    val darfEmpfangen: Boolean = false,
    /** Wie viele Tage die Freundschaft noch braucht; 0, wenn es losgeht. */
    val wartetage: Int = 0,
)

/** Die Grenzen, die das Abo verschiebt — die Zahl eigener Vorlagen und Leitstellen. */
@Serializable
data class Abogrenzen(
    val aaoVorlagen: Int = 0,
    val szenarien: Int = 0,
    val leitstellen: Int = 0,
    val rundenvorlagen: Int = 0,
)

/**
 * Der Stand des Abos.
 *
 * `status` ist `Free`, `Active`, `PastDue` oder `Canceled`; `plan` ist `None`,
 * `Monthly` oder `Yearly`. <b>`kaufVerfuegbar` ist ein Schalter der Verwaltung</b>
 * und keine Konstante — solange er aus ist, steht im Schaufenster kein Kaufknopf,
 * sondern der Grund.
 */
@Serializable
data class Abostand(
    val status: String = "Free",
    val plan: String = "None",
    val aktiv: Boolean = false,
    val seit: String? = null,
    val bis: String? = null,
    val kaufVerfuegbar: Boolean = false,
    val publishableKey: String = "",
    val monatspreisCent: Int = 0,
    val jahrespreisCent: Int = 0,
    val grenzen: Abogrenzen = Abogrenzen(),
    val vorteile: List<String> = emptyList(),
)

/** Eine Adresse, zu der der Server schickt — Stripe-Kasse oder Kundenportal. */
@Serializable
data class Kassenadresse(
    val url: String = "",
)
