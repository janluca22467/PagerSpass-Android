package de.pagerspass.pagerspass.ui.schmuck

import androidx.compose.ui.graphics.Color

/**
 * Die Schmuckkataloge — übertragen aus `KOPFMUSTER` und `PROFILRAHMEN` in
 * `web/src/types.ts`.
 *
 * <b>Drei Wege zu einem Stück, und sie sind nicht dasselbe.</b> Das ist der
 * Grund, warum hier drei Felder stehen statt einer Stufe:
 *
 *   * **`abLevel`** — erspielt. Es öffnet sich von selbst, wenn man weit genug
 *     ist, und steht vorher als Ziel da.
 *   * **`preisCredits`** — gekauft. Es öffnet sich nie von selbst; wer es haben
 *     will, geht in den Shop. Die Stufe ist dabei 1 und sagt gar nichts.
 *   * **`premium`** — am Abo. Es ist da, solange das Abo läuft, und danach nicht
 *     mehr (siehe „Premium-Ablauf: was bleibt").
 *
 * <b>Warum das eine eigene Datei ist.</b> Diese Listen sind Daten und ändern
 * sich mit jeder Freigabe; die Zeichnungen daneben ändern sich fast nie. Wer
 * beides mischt, sucht beim Nachtragen eines Musters in 800 Zeilen Kurvenmathematik.
 *
 * <b>Sie sind von Hand gleichzuhalten.</b> Der Server liefert die Kataloge nicht
 * mit — im Web stehen sie ebenfalls fest in `types.ts`. Ein Stück, das dort
 * dazukommt, fehlt hier, bis es jemand einträgt; es fällt dann in der App
 * schlicht aus der Auswahl, ohne Fehler.
 */
object Schmuck {

    /**
     * Ein Stück Schmuck.
     *
     * @param preisCredits `null` heißt: nicht käuflich. Ein Preis heißt: **nur**
     *   käuflich — die Stufe daneben ist dann 1 und bedeutet nichts.
     */
    data class Stueck(
        val id: String,
        val name: String,
        val abLevel: Int = 1,
        val preisCredits: Int? = null,
        val premium: Boolean = false,
        val farbe: Color? = null,
    ) {
        /**
         * Ob dieses Stück getragen werden darf.
         *
         * @param besitzt Die `stueckId`s aus dem Shop-Besitzstand. Ohne sie ist
         *   jedes gekaufte Stück gesperrt — was richtig ist: Nicht geladen heißt
         *   nicht besessen, und ein Stück fälschlich offen anzubieten endet in
         *   einer Fehlermeldung des Servers.
         */
        fun offen(stufe: Int, premiumAktiv: Boolean, besitzt: Set<String>): Boolean = when {
            premium -> premiumAktiv
            preisCredits != null -> id in besitzt
            else -> stufe >= abLevel
        }

        /** Was an einem gesperrten Stück steht — und zwar warum, nicht nur dass. */
        fun sperrgrund(): String = when {
            premium -> "Premium"
            preisCredits != null -> "$preisCredits C"
            else -> "ab $abLevel"
        }
    }

    /** Die Kopfmuster — das Band hinter Wappen und Name. */
    val KOPFMUSTER = listOf(
        Stueck("keines", "Glatt", 1, null, false),
        Stueck("streifen", "Diagonalstreifen", 8, null, false),
        Stueck("battenburg", "Battenburg", 22, null, false),
        Stueck("karbon", "Karbon", 34, null, false),
        Stueck("goldverlauf", "Goldverlauf", 66, null, false),
        Stueck("warnschraffur", "Warnschraffur", 1, 150, false),
        Stueck("punktraster", "Punktraster", 1, 120, false),
        Stueck("wellen", "Wellen", 1, 140, false),
        Stueck("gitter", "Gitter", 1, 120, false),
        Stueck("sparren", "Sparren", 1, 150, false),
        Stueck("funkwellen", "Funkwellen", 1, 180, false),
        Stueck("nadelstreifen", "Nadelstreifen", 1, 110, false),
        Stueck("flecktarn", "Flecktarn", 1, 170, false),
        Stueck("blitz", "Blitz", 1, 150, false),
        Stueck("topografie", "Topografie", 1, 180, false),
        Stueck("signalband", "Signalband", 1, 140, false),
        Stueck("sternbild", "Sternbild", 1, 170, false),
        Stueck("premium", "Premium", 1, null, true),
        Stueck("premium-lichtband", "Lichtband", 1, null, true),
        Stueck("premium-wabe", "Wabe", 1, null, true),
        Stueck("premium-schraffur", "Goldschraffur", 1, null, true),
        Stueck("premium-sternenflug", "Sternenflug", 1, null, true),
        Stueck("premium-lagekarte", "Lagekarte", 1, null, true),
        Stueck("premium-morgenschicht", "Morgenschicht", 1, null, true),
        Stueck("premium-polarband", "Polarband", 1, null, true),
        Stueck("premium-leuchtsuch", "Leuchtsuche", 1, null, true),
        Stueck("premium-atemzug", "Atemzug", 1, null, true),
        Stueck("premium-karte", "Eigene Lage", 1, null, true),
    )

    /** Die Rahmen ums Wappen. */
    val RAHMEN = listOf(
        Stueck("keiner", "Ohne Rahmen", 1, null, false, null),
        Stueck("stahl", "Stahl", 12, null, false, Color(0xFF98A2AE)),
        Stueck("kupfer", "Kupfer", 24, null, false, Color(0xFFC07A4A)),
        Stueck("silber", "Silber", 36, null, false, Color(0xFFD8DFE7)),
        Stueck("gold", "Gold", 48, null, false, Color(0xFFE0B84F)),
        Stueck("blaulicht", "Blaulicht", 60, null, false, Color(0xFF2F6BFF)),
        Stueck("platin", "Platin", 90, null, false, Color(0xFFD7DDE5)),
        Stueck("signalblau", "Signalblau", 125, null, false, Color(0xFF3478F6)),
        Stueck("kommandogold", "Kommandogold", 150, null, false, Color(0xFFC89932)),
        Stueck("ehrenstahl", "Ehrenstahl", 175, null, false, Color(0xFF66758A)),
        Stueck("ehrenkranz", "Ehrenkranz", 200, null, false, Color(0xFFF0C85A)),
        Stueck("messing", "Messing", 1, 180, false, Color(0xFFB9963F)),
        Stueck("nachtblau", "Nachtblau", 1, 240, false, Color(0xFF27406E)),
        Stueck("smaragd", "Smaragd", 1, 210, false, Color(0xFF2E9E6B)),
        Stueck("rubin", "Rubin", 1, 210, false, Color(0xFFC23B4B)),
        Stueck("titan", "Titan", 1, 150, false, Color(0xFF7C8AA0)),
        Stueck("bronze", "Bronze", 1, 140, false, Color(0xFF9C6F3F)),
        Stueck("violett", "Violett", 1, 180, false, Color(0xFF8A5FD6)),
        Stueck("elfenbein", "Elfenbein", 1, 150, false, Color(0xFFE8E2D0)),
        Stueck("koralle", "Koralle", 1, 170, false, Color(0xFFDF7664)),
        Stueck("eisblau", "Eisblau", 1, 200, false, Color(0xFF8BC8DC)),
        Stueck("obsidian", "Obsidian", 1, 230, false, Color(0xFF32343A)),
        Stueck("roségold", "Roségold", 1, 210, false, Color(0xFFCE8E82)),
        Stueck("premium", "Premium", 1, null, true, Color(0xFFF4D35E)),
        Stueck("premium-platin", "Platin", 1, null, true, Color(0xFFDFE4EA)),
        Stueck("premium-signal", "Signalrot", 1, null, true, Color(0xFFE5352B)),
        Stueck("premium-nachtgold", "Nachtgold", 1, null, true, Color(0xFF8A6A1F)),
        Stueck("premium-glut", "Glut", 1, null, true, Color(0xFFFF7A1A)),
        Stueck("premium-lauflicht", "Lauflicht", 1, null, true, Color(0xFFFFC247)),
        Stueck("premium-nordlicht", "Nordlicht", 1, null, true, Color(0xFF7FE3C6)),
        Stueck("premium-tiefsee", "Tiefsee", 1, null, true, Color(0xFF1F8A8F)),
        Stueck("premium-wechselblitz", "Wechselblitz", 1, null, true, Color(0xFF4F9BFF)),
    )

    /**
     * Die Wappenzeichen samt Stufe — aus `WAPPEN_AB_LEVEL`.
     *
     * Nur die, für die die App auch eine Zeichnung hat (siehe `Kontobild`). Ein
     * Zeichen ohne Zeichnung anzubieten hieße, eine Wahl zu erlauben, deren
     * Ergebnis man nicht sieht.
     */
    val WAPPENZEICHEN = listOf(
        Stueck("Keines", "Initialen", 1),
        Stueck("Feuerwehr", "Feuerwehr", 1),
        Stueck("Rettungsdienst", "Rettungsdienst", 1),
        Stueck("Thw", "THW", 1),
        Stueck("Polizei", "Polizei", 1),
        Stueck("Drehleiter", "Drehleiter", 15),
    )

    fun rahmenfarbe(id: String): Color? = RAHMEN.firstOrNull { it.id == id }?.farbe
}
