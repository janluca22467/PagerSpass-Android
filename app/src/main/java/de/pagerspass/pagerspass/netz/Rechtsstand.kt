package de.pagerspass.pagerspass.netz

/**
 * Die Fassung der Rechtstexte, der ein Konto zugestimmt hat.
 *
 * <b>Diese Zeichenkette ist eine Zusage und keine Versionsnummer.</b> Sie geht
 * bei der Kontoanlage und bei jeder Zustimmung an den Server und wird dort
 * gespeichert — sie ist der Nachweis dafür, welcher Text jemandem tatsächlich
 * vorgelegen hat. Der Server kann das nicht erraten; nur der Client weiß es.
 *
 * <b>Verglichen wird „älter", nicht „anders".</b> Im Web stand hier einmal ein
 * `!==`, und das hat ein Konto in eine Schleife gebracht, sobald zwei
 * verschiedene Stände des Frontends auf dasselbe Konto trafen: Der ältere Stand
 * legte seine Fassung vor, der neuere hielt sie für falsch und legte seine vor,
 * und so fort. Ein Zeichenkettenvergleich auf „kleiner" schneidet das ab — die
 * Fassungen sind deshalb ausdrücklich so benannt, dass sie in dieser Ordnung
 * aufsteigen (`2026-09-08b` folgt auf `2026-09-08`).
 *
 * <b>Sie muss mit `web/src/recht/rechtstexte.ts` übereinstimmen.</b> Das ist die
 * eine Stelle, an der App und Web denselben Wert von Hand führen. Wer dort eine
 * neue Fassung setzt, setzt sie hier mit — sonst legt die App einem Konto eine
 * Fassung vor, die der Server nicht kennt, oder sie fragt gar nicht erst.
 *
 * Hier stand „und es gibt dafür keinen Wächter". Es gibt einen:
 * `web/scripts/kontrast-pruefen.mjs` vergleicht beide Werte und bricht `npm run
 * build` ab, wenn sie auseinanderlaufen. Am 15.09.2026 hat er genau das getan —
 * die Web-Fassung stand schon auf `2026-09-15b`, diese hier noch auf
 * `2026-09-15`.
 *
 * <b>Was noch aussteht.</b> Die App zeigt die Texte bisher nicht selbst; die
 * Wege im Konto und im Fuß der Anmeldung führen an den Browser. Für die
 * Veröffentlichung reicht das nicht: Die Texte beschreiben derzeit nur die
 * Web-Anwendung und erfassen die App formal nicht (siehe `doku/app/android-app.md`,
 * Blocker 4).
 */
object Rechtsstand {
    /** Muss zeichengleich `RECHTSSTAND` in `web/src/recht/rechtstexte.ts` sein. */
    const val AKTUELL = "2026-09-24"

    /** Wie der Stand in einer Fußzeile heißt. */
    const val ANSAGE = "Stand: 24. September 2026 (Fassung $AKTUELL)"

    /**
     * Ist diesem Konto die aktuelle Fassung noch vorzulegen?
     *
     * `null` — ein Konto ohne hinterlegte Fassung — heißt ja: Es hat noch keiner
     * zugestimmt.
     */
    fun vorzulegen(stand: String?): Boolean = stand == null || stand < AKTUELL

    /** Wo die Texte im Web stehen. Von dort holt sie auch die App. */
    fun adresse(server: String, seite: String) = "$server/recht/$seite"

    /** Die vier Texte, die an der Anmeldung hängen. */
    const val AGB = "agb"
    const val NUTZUNGSBEDINGUNGEN = "nutzungsbedingungen"
    const val DATENSCHUTZ = "datenschutz"
    const val IMPRESSUM = "impressum"
}
