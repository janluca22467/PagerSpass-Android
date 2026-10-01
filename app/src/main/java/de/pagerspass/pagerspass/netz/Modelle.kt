package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Modelle der API — übertragen aus `web/src/types.ts`.
 *
 * <b>Fast alles ist optional.</b> Das ist keine Nachlässigkeit, sondern die
 * Lehre aus der Vorlage: Dort steht an `teammitglied` ausdrücklich, dass ein
 * älterer Server es noch nicht mitschickt und dass es nur an *einem* der Wege
 * hängt — Anmeldung und Umbenennung liefern es beim nächsten Laden nach. Ein
 * Modell, das darauf besteht, bricht die Anmeldung gegen einen Server, der einen
 * Tag älter ist als die App.
 *
 * Hier stehen nur die Modelle, die Anmeldung und Startbildschirm brauchen. Die
 * übrigen kommen mit den Ansichten, die sie benutzen — ein vollständiges
 * `types.ts` (2.400 Zeilen) auf Vorrat zu übersetzen hieße, 90 Prozent davon nie
 * gegen einen echten Rumpf zu prüfen.
 */

/**
 * Ein Konto.
 *
 * <b>`merkmal` kommt nur bei Anmeldung und Kontoanlage mit</b> — es ist der
 * Ausweis für diese Sitzung und steht danach in der Ablage, nicht im Modell.
 */
@Serializable
data class Konto(
    val kennung: String,
    /** Eindeutig — der Schlüssel zum Wiederherstellen und späteren Einladen. */
    val benutzername: String = "",
    val anzeigename: String = "",
    val erfahrung: Int = 0,
    val level: Int = 1,
    val rang: String = "",
    /** Punkte bis zum nächsten Aufstieg; `null` auf der letzten Stufe. */
    val bisZumNaechsten: Int? = null,
    /**
     * Was die aktuelle Stufe gekostet hat — der Punktestand, an dem sie begann.
     * Ohne ihn ließe sich der Anteil nur ab null zeigen, und der Balken spränge
     * bei jedem Aufstieg zurück auf fast voll.
     */
    val schwelle: Int = 0,
    /** Verdient über Aufstiege, ausgegeben im Shop. Kein Echtgeld. */
    val credits: Int = 0,
    val premiumAktiv: Boolean = false,
    val premiumBis: String? = null,
    /** Der blaue Team-Haken. Hängt nur an `GET /api/konto/{kennung}`. */
    val teammitglied: Boolean = false,
    val erstelltUm: String? = null,
    /**
     * Ob die Gesprächsverläufe zur Produktverbesserung aufgezeichnet werden
     * dürfen. `null` heißt: noch nicht gefragt — dann stellt die App die Frage.
     */
    val analyseZustimmung: Boolean? = null,
    /**
     * Ob die verpflichtende Ausbildungsschicht noch aussteht. Solange sie es
     * tut, zeigt der Startbildschirm nur diesen Weg — und der Server ließe
     * ohnehin keine andere Runde zu.
     */
    val einweisungOffen: Boolean = false,
    /**
     * Ob das Konto noch auf seine Freischaltung durch das Team wartet — nur auf
     * der Beta je wahr. Kommt in der Antwort auf das Anlegen zusammen mit
     * <em>keinem</em> Merkmal: Dann übernimmt die App das Konto nicht, sondern
     * zeigt den Hinweis; angemeldet wird nach der Freischaltung.
     */
    val freischaltungOffen: Boolean = false,
    /**
     * Fassung der Rechtstexte, der dieses Konto zugestimmt hat. Weicht sie von
     * `Rechtsstand.AKTUELL` ab, ist die neue Fassung vorzulegen.
     */
    val rechtsstandVersion: String? = null,
    /**
     * Wo das Konto bei der Altersfrage steht (v6): `Offen`, `WartetAufEltern`
     * oder `Freigegeben`. Ein älterer Server schickt das Feld nicht — dann gibt
     * es nichts zu fragen.
     */
    val altersstand: String? = null,
    /** Nur bei Anmeldung und Kontoanlage. Danach steht es in der Ablage. */
    val merkmal: String? = null,
    /** Nur bei der Kontoanlage — und nur dieses eine Mal. */
    val wiederherstellungscode: String? = null,
) {
    /**
     * Wie weit die aktuelle Stufe gefüllt ist, zwischen 0 und 1.
     *
     * Gerechnet aus Schwelle und Rest, nicht ab null: Sonst stünde der Balken
     * nach jedem Aufstieg fast am Ende, weil die Erfahrung ja weiterläuft.
     * Auf der letzten Stufe (`bisZumNaechsten == null`) ist er voll.
     */
    val stufenanteil: Float
        get() {
            val rest = bisZumNaechsten ?: return 1f
            val spanne = (erfahrung - schwelle) + rest
            return if (spanne <= 0) 1f else ((erfahrung - schwelle).toFloat() / spanne).coerceIn(0f, 1f)
        }

    /** „noch 215 bis Stufe 13" — oder die Ansage, dass es keine höhere gibt. */
    val stufentext: String
        get() = bisZumNaechsten?.let { "noch $it bis Stufe ${level + 1}" } ?: "höchste Stufe erreicht"
}

/**
 * Was bei der Anmeldung herauskommt.
 *
 * <b>Zwei Ausgänge, nicht einer.</b> Hat das Konto den Anmeldeschutz
 * eingeschaltet, antwortet der Server statt mit dem Konto mit einem Zettel:
 * `{ zweiFaktor: true, anfrage, ziel }`. Dann fehlt noch der Zahlencode aus dem
 * Postfach. Ohne eingeschalteten Schutz — der Normalfall — kommt das Konto.
 */
sealed interface Anmeldeergebnis {
    data class Angemeldet(val konto: Konto) : Anmeldeergebnis

    data class ZweiFaktor(val anfrage: String, val ziel: String) : Anmeldeergebnis
}

/** Die Rohantwort der Anmeldung — Konto und Zweifaktor-Zettel in einem Rumpf. */
@Serializable
internal data class Anmeldeantwort(
    val zweiFaktor: Boolean = false,
    val anfrage: String? = null,
    val ziel: String? = null,
    val kennung: String? = null,
    val merkmal: String? = null,
)

/**
 * Der Stand einer Wartung.
 *
 * Er kommt dem 503 beigelegt — die Sperrseite steht damit, bevor überhaupt
 * jemand nachgefragt hat.
 */
@Serializable
data class Wartungsstand(
    val aktiv: Boolean = false,
    val titel: String? = null,
    val text: String? = null,
    val bis: String? = null,
)

/** Die laufende Fassung des Servers — für den Fuß des Startbildschirms. */
@Serializable
data class Versionsstand(
    val version: String? = null,
    val kanal: String? = null,
    /**
     * Ob das Postfach freigeschaltet ist.
     *
     * <b>Es hängt an einem Betriebsschalter</b> und steht in der Vorgabe auf
     * „aus". Die App fragt ihn hier ab, statt den Weg blind anzubieten — ein
     * Postfach, das mit „nicht gefunden" antwortet, sieht aus wie ein Fehler
     * und ist eine Entscheidung.
     */
    val postfach: Boolean = false,
    /**
     * Ob ein neues Konto auf diesem Stand erst vom Team freigeschaltet werden
     * muss — die Regel der Beta. Die Anmeldeseite sagt es vor dem Anlegen.
     */
    val kontofreischaltung: Boolean = false,
)
