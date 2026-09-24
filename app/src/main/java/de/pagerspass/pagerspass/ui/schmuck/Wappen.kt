package de.pagerspass.pagerspass.ui.schmuck

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Die Farben der Kontowappen — übertragen aus `web/src/utils/wappen.ts`.
 *
 * <b>Eine Palette für beides.</b> Sie färbt das Wappen *und* den Verlauf des
 * Profilbanners. Zwei Paletten wären ab dem ersten Nachbessern zwei verschiedene
 * Personen: oben ein blauer Kopf, darin ein grünes Wappen.
 */
object Wappen {

    val PALETTE: List<Color> = listOf(
        Color(0xFF1C7ED6),
        Color(0xFF2F9E44),
        Color(0xFFE5352B),
        Color(0xFFF08C00),
        Color(0xFF7048E8),
        Color(0xFF0B7285),
        Color(0xFFC2255C),
        Color(0xFF5C7CFA),
        // Nummer 8: Gold — die einzige erspielte Farbe (ab Stufe 70).
        Color(0xFFA8861D),
        // Shop-Farben: eine eigene, kaufbare Reihe nach dem erspielten Gold.
        Color(0xFF8F2525),
        Color(0xFF246B45),
        Color(0xFF1F5F78),
        Color(0xFF34383D),
    )

    /**
     * Der Ton eines Kontos.
     *
     * <b>Ohne eigene Wahl entscheidet die Kennung</b> — und zwar stabil, damit
     * dieselbe Person überall gleich aussieht und niemand seine Farbe wechselt,
     * nur weil er in einer anderen Liste steht. Die Rechnung ist zeichengleich
     * die des Webs; eine andere ergäbe dieselbe Person in zwei Farben, je
     * nachdem, ob sie im Browser oder in der App steht.
     */
    fun ton(kennung: String, farbe: Int = 0): Color {
        if (farbe > 0) return PALETTE[farbe % PALETTE.size]

        var summe = 0
        for (zeichen in kennung) summe = (summe + zeichen.code) % 997
        return PALETTE[summe % PALETTE.size]
    }

    /**
     * Die Schriftfarbe auf einem Wappenton: was von beiden besser zu lesen ist.
     *
     * <b>Warum das nicht immer Weiß sein kann.</b> Nachgemessen hält Weiß nur
     * auf sieben von dreizehn Tönen die Grenze: auf Orange (#f08c00) kommt es
     * auf 2,48:1, auf Gold und Grün auf 3,45:1. Die Initialen stehen in einer
     * Listenzeile bei 14,5 Punkt — das ist keine große Schrift, für die 3:1
     * genügten.
     *
     * <b>Dunkler machen ginge nicht:</b> Gold ist ab Stufe 70 erspielt, die vier
     * Töne darunter im Shop gekauft. Wer sie abdunkelt, ändert einen Gegenstand,
     * den jemand besitzt. Die Tinte darf wechseln, die Farbe nicht.
     */
    fun schrift(ton: Color): Color {
        // Der Kontrast gegen Weiß fällt mit der Helligkeit, der gegen die dunkle
        // Tinte steigt mit ihr. Sie kreuzen sich genau einmal — mehr als ein
        // Vergleich ist hier nicht nötig.
        val l = helligkeit(ton)
        val gegenHell = 1.05f / (l + 0.05f)
        val gegenDunkel = (l + 0.05f) / (helligkeit(TINTE_DUNKEL) + 0.05f)

        return if (gegenDunkel > gegenHell) TINTE_DUNKEL else TINTE_HELL
    }

    /**
     * Die ein bis zwei Buchstaben, die im Wappen stehen, wenn kein Bild und kein
     * Zeichen gewählt ist — „Wache Nord" wird „WN".
     *
     * Das Ersatzzeichen ist ein Parameter: Im Web stand diese Kette an vier
     * Stellen und war schon auseinandergelaufen — zwei Fassungen fielen auf `?`
     * zurück, zwei auf `W`. Das war der einzige echte Unterschied zwischen ihnen.
     */
    fun initialen(name: String?, ersatz: String = "?"): String {
        val buchstaben = name.orEmpty()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.first().uppercaseChar() }

        return when {
            buchstaben.isEmpty() -> ersatz
            buchstaben.size == 1 -> buchstaben.first().toString()
            else -> "${buchstaben.first()}${buchstaben[1]}"
        }
    }

    /** Die dunkle Tinte auf hellen Tönen — derselbe Grund wie `BgTief`. */
    private val TINTE_DUNKEL = Color(0xFF05070A)
    private val TINTE_HELL = Color.White

    /** Relative Helligkeit nach WCAG. */
    private fun helligkeit(farbe: Color): Float {
        fun kanal(s: Float) = if (s <= 0.03928f) s / 12.92f else ((s + 0.055f) / 1.055f).pow(2.4f)
        return 0.2126f * kanal(farbe.red) + 0.7152f * kanal(farbe.green) + 0.0722f * kanal(farbe.blue)
    }
}
