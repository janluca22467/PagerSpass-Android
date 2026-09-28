package de.pagerspass.pagerspass.ui.fahrzeug

/*
 * Die Takte der Sondersignalanlage — die Keyframes aus `styles/fahrzeuge.css` als
 * Rechnung über die Uhrzeit.
 *
 * Kein Links-Rechts-Links-Rechts: Eine echte Anlage blitzt in Salven. Der Doppelblitz
 * ist der Normalfall moderner LED-Balken — zwei harte Schläge, dann eine lange Pause.
 * Die Polizei fährt Vierfachblitz, das THW und die Boote tragen die klassische
 * Rundumkennleuchte, die auf- und abblendet, statt zu blitzen. Die Seite `b` läuft eine
 * halbe Periode hinter der Seite `a` (die `animation-delay` des Webs).
 */

/** Periode eines Musters in Millisekunden — `fz-doppel` 0,95 s, `fz-vierfach` 1,1 s, `fz-rundum` 1,4 s. */
internal fun periodeVon(muster: Blitzmuster): Long = when (muster) {
    Blitzmuster.Doppel -> 950L
    Blitzmuster.Vierfach -> 1_100L
    Blitzmuster.Rundum -> 1_400L
}

/** Wo im Umlauf eine Seite gerade steht, 0 bis unter 1. */
private fun phase(periode: Long, versatz: Long, jetzt: Long): Float =
    Math.floorMod(jetzt - versatz, periode).toFloat() / periode

/** Weiches Ein- und Ausblenden wie `ease-in-out`. */
private fun weich(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** Die Rundumkennleuchte: in 8 % des Umlaufs hoch, bis 20 % wieder herunter. */
private fun rundum(p: Float): Float = when {
    p < 0.08f -> weich(p / 0.08f)
    p < 0.2f -> 1f - weich((p - 0.08f) / 0.12f)
    else -> 0f
}

/**
 * Wie hell eine Lampe gerade ist: 1 heißt an (`--an`), 0 heißt aus (`--aus`) — was
 * das in Deckkraft bedeutet, weiß die Lampe selbst (der Blitzer wird voll hell, sein
 * Lichthof nur ein Drittel, der Frontblitzer geht ganz aus).
 *
 * @param takt `'a'` oder `'b'` — die beiden Seiten der Anlage.
 */
fun blitzphase(muster: Blitzmuster, takt: Char, jetzt: Long): Float {
    val periode = periodeVon(muster)
    val p = phase(periode, if (takt == 'b') periode / 2 else 0L, jetzt)
    return when (muster) {
        // Zwei Schläge in 130 ms, dann Ruhe.
        Blitzmuster.Doppel -> if (p < 0.046f || (p >= 0.091f && p < 0.136f)) 1f else 0f
        // Vier kurze Schläge hintereinander — die unruhigere Anlage.
        Blitzmuster.Vierfach ->
            if (p < 0.027f || (p >= 0.053f && p < 0.08f) || (p >= 0.106f && p < 0.133f) || (p >= 0.159f && p < 0.186f)) 1f else 0f
        Blitzmuster.Rundum -> rundum(p)
    }
}

/**
 * Der Lichthof um die ganze Marke (`.fzg-icon--sonder`): Er schlägt zweimal je Umlauf,
 * nicht viermal — die beiden Seiten sieht man auf Kartengröße nicht getrennt, und ein
 * Hof, der viermal so schnell flackerte, wäre auf vierzig Marken nicht auszuhalten.
 */
fun hofphase(muster: Blitzmuster, jetzt: Long): Float {
    val p = phase(periodeVon(muster), 0L, jetzt)
    return when (muster) {
        Blitzmuster.Doppel -> {
            val q = if (p >= 0.5f) p - 0.5f else p
            if (q < 0.046f || (q >= 0.091f && q < 0.136f)) 1f else 0f
        }
        Blitzmuster.Vierfach -> blitzphase(Blitzmuster.Vierfach, 'a', jetzt)
        Blitzmuster.Rundum -> rundum(if (p >= 0.5f) p - 0.5f else p)
    }
}
