package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Rümpfe der vorbereiteten Übungen und der Lehrgänge — übertragen aus
 * `web/src/types.ts` (Szenario, Szenarioeintrag, Lehrgang …) und gegengelesen an
 * `Endpunkte/SzenarioEndpunkte.cs`.
 *
 * <b>Die Aufzählungen stehen als Zeichenkette</b> („Lage", „Stoerung",
 * „Wetterwechsel"), wie überall in der App: Ein vierter Wert des Servers darf den
 * Editor nicht zum Absturz bringen, er zeigt ihn dann eben roh.
 *
 * <b>`null` heißt beim Szenario oft „die Vorgabe"</b> — eine Jahreszeit `null` ist
 * „wie gerade draußen", eine Windrichtung `null` ist „keine Angabe". Deshalb sind
 * diese Felder hier ausdrücklich `null`-fähig und nicht mit einem Ersatzwert
 * belegt: Ein Ersatzwert ginge beim Speichern als Entscheidung an den Server.
 */

// ------------------------------------------------------------------- Übungen

/**
 * Ein Eintrag der Zeitachse.
 *
 * Die Felder einer Lage sind genau die des Einsatzbogens — ein Szenario ist eine
 * freie Vergabe, die schon gestern aufgeschrieben wurde. Was zu einer anderen Art
 * gehört, bleibt `null`.
 */
@Serializable
data class Szenarioeintrag(
    /** `Lage`, `Stoerung` oder `Wetterwechsel`. */
    val art: String = "Lage",
    /** Abstand zum Dienstbeginn in Sekunden. */
    val nachSekunden: Int = 0,
    val stichwort: String? = null,
    val stichwortText: String? = null,
    val meldebild: String? = null,
    val adresse: String? = null,
    val organisation: String? = null,
    val prioritaet: Int? = null,
    val ortsteil: String? = null,
    val meldender: String? = null,
    val empfohleneFahrzeuge: Int? = null,
    val empfohleneFaehigkeiten: List<String>? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val stoerung: String? = null,
    val wetter: String? = null,
    val windrichtung: Int? = null,
)

/**
 * Die Regeln, unter denen eine Übung läuft.
 *
 * <b>Die letzten drei zeigt der Editor nicht</b> (so wenig wie das Web), sie
 * fahren aber mit: Der Server schickt sie mit, und wer sie beim Speichern
 * wegließe, setzte sie still auf die Vorgabe zurück.
 */
@Serializable
data class Szenarioeinstellungen(
    val tagesalarmstaerke: Boolean? = null,
    val loeschwasser: Boolean? = null,
    val sonderobjekte: Boolean? = null,
    val wiederherstellung: Boolean? = null,
    val einsatzarbeit: Boolean? = null,
    val suchlagen: Boolean? = null,
    val vegetationsbraende: Boolean? = null,
    val gefahrgutlagen: Boolean? = null,
    val telefonischeLeitstelle: Boolean? = null,
    val wetter: String? = null,
    val windrichtung: Int? = null,
    val jahreszeit: String? = null,
    val arbeitsfunk: String? = null,
    val einsatzende: String? = null,
    val einsatzleitung: Boolean? = null,
)

/** Eine vorbereitete Übung mit allem, was drinsteht. */
@Serializable
data class Szenario(
    /** Leer, solange sie noch nie gespeichert wurde. */
    val id: String = "",
    val name: String = "",
    val beschreibung: String = "",
    val landkreisId: String? = null,
    /** Der sechsstellige Code zum Weitergeben. */
    val code: String = "",
    val aufstellung: List<String> = emptyList(),
    val zeitachse: List<Szenarioeintrag> = emptyList(),
    val einstellungen: Szenarioeinstellungen = Szenarioeinstellungen(),
    val geaendertUm: String = "",
)

/**
 * Was beim Speichern hinausgeht — `SzenarioRequest` am Server.
 *
 * Ohne `code` und `geaendertUm`: Beides vergibt der Server, und ohne `id` entsteht
 * eine neue Übung.
 */
@Serializable
data class Szenarioanfrage(
    val id: String? = null,
    val name: String = "",
    val beschreibung: String? = null,
    val landkreisId: String? = null,
    val aufstellung: List<String> = emptyList(),
    val zeitachse: List<Szenarioeintrag> = emptyList(),
    val einstellungen: Szenarioeinstellungen = Szenarioeinstellungen(),
)

/** Eine Zeile der Übungsliste — ohne den Inhalt, den für eine Liste niemand braucht. */
@Serializable
data class Szenariozeile(
    val id: String = "",
    val name: String = "",
    val beschreibung: String = "",
    val landkreisId: String? = null,
    val code: String = "",
    val eintraege: Int = 0,
    val geaendertUm: String = "",
)

/**
 * Eine gefahrene Übung im Verlauf.
 *
 * Der Name steht in der Zeile selbst: Wer seine Übung später umbaut oder löscht,
 * verliert seinen Verlauf dadurch nicht.
 */
@Serializable
data class Uebungsfahrt(
    val name: String = "",
    val roomCode: String = "",
    val beendetUm: String = "",
    /** `Leitstelle`, `Fahrzeugbesatzung` oder `Unbestimmt`. */
    val rolle: String = "",
    val funkrufname: String? = null,
    val einsaetze: Int = 0,
)

// ---------------------------------------------------------------- Lehrgänge

/** Ein Modul eines Lehrgangs samt dem Stand des eigenen Kontos. */
@Serializable
data class Lehrgangsmodul(
    val id: String = "",
    /** `Lesestoff`, `Lektion` oder `Pruefung`. */
    val art: String = "",
    val titel: String = "",
    val text: String = "",
    val wikiseite: String? = null,
    val erledigt: Boolean = false,
    /** Nur bei einer Prüfung aussagekräftig. */
    val bestanden: Boolean = false,
    /** Die Prüfungsschicht im Archiv — der Nachweis hinter dem Zeugnis. */
    val schichtCode: String? = null,
    val erledigtUm: String? = null,
)

/** Ein Lehrgang: Lesestoff, Lektion, Prüfung — in dieser Reihenfolge. */
@Serializable
data class Lehrgang(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String = "",
    val module: List<Lehrgangsmodul> = emptyList(),
    val bestanden: Boolean = false,
    val bestandenUm: String? = null,
    val zeugnisSchicht: String? = null,
    /** Ob ein vorheriger Lehrgang noch fehlt. Der Server weist ihn dann auch ab. */
    val gesperrt: Boolean = false,
    /** Die Titel der fehlenden Lehrgänge — leer, sobald sie stehen. */
    val voraussetzungen: List<String> = emptyList(),
)

// -------------------------------------------------------------- Aufschriften

/**
 * Die Aufschriften der Aufzählungen — dieselben Sätze wie in `types.ts`.
 *
 * <b>Als Tabellen und nicht an der Anzeigestelle</b>, aus dem Grund, den das Web
 * an `MODUS_LABEL` aufgeschrieben hat: Eine Weiche an der Anzeigestelle vergisst
 * den dritten Fall, eine Tabelle zeigt ihn als Lücke.
 */
object Uebungsaufschrift {
    val SZENARIOART: Map<String, String> = linkedMapOf(
        "Lage" to "Lage",
        "Stoerung" to "Störung",
        "Wetterwechsel" to "Wetter",
    )

    val STOERUNGSART: Map<String, String> = linkedMapOf(
        "BlinderAlarm" to "Blinder Alarm",
        "BoeswilligerNotruf" to "Böswilliger Notruf",
        "FehlalarmRueckruf" to "Fehlalarm-Rückruf",
        "Fahrzeugdefekt" to "Fahrzeug bleibt liegen",
        "Funkloch" to "Funkloch",
        "Schaulustige" to "Schaulustige",
        "Drohne" to "Drohne über der Einsatzstelle",
        "Presseanruf" to "Presse ruft an",
    )

    val WETTER: Map<String, String> = linkedMapOf(
        "Klar" to "Klar",
        "Regen" to "Regen",
        "Glaette" to "Glätte",
        "Sturm" to "Sturm",
    )

    val JAHRESZEIT: Map<String, String> = linkedMapOf(
        "Fruehling" to "Frühling",
        "Sommer" to "Sommer",
        "Herbst" to "Herbst",
        "Winter" to "Winter",
    )

    val ORGANISATION: Map<String, String> = linkedMapOf(
        "Feuerwehr" to "Feuerwehr",
        "Rettungsdienst" to "Rettungsdienst",
        "Thw" to "THW",
        "Polizei" to "Polizei",
    )

    /** Die Dringlichkeit, wie der Einsatzbogen sie nennt. */
    val PRIORITAET: Map<Int, String> = linkedMapOf(
        1 to "Normal",
        2 to "Dringend",
        3 to "Sonderrechte",
    )

    /** Kurzname des Trägers — `HIORG_LABEL` im Web. „Keine" ist kein Träger. */
    val TRAEGER: Map<String, String> = linkedMapOf(
        "Keine" to "",
        "Drk" to "DRK",
        "Juh" to "Johanniter",
        "Mhd" to "Malteser",
        "Asb" to "ASB",
        "Dlrg" to "DLRG",
        "Bergwacht" to "Bergwacht",
        "Wasserwacht" to "Wasserwacht",
        "Dgzrs" to "Seenotretter",
        "Brh" to "Rettungshunde",
        "Werkfeuerwehr" to "Werkfeuerwehr",
        "Privat" to "Privater RD",
    )

    fun traeger(hiOrg: String): String = TRAEGER[hiOrg] ?: hiOrg

    fun art(art: String): String = SZENARIOART[art] ?: art
    fun stoerung(art: String?): String = STOERUNGSART[art ?: "Funkloch"] ?: art.orEmpty()
    fun wetter(lage: String?): String = WETTER[lage ?: "Klar"] ?: lage.orEmpty()
    fun organisation(org: String?): String = ORGANISATION[org.orEmpty()] ?: org.orEmpty()
}
