package de.pagerspass.pagerspass.ansichten

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Zeitangaben, wie sie im Spiel stehen.
 *
 * <b>Der Server schickt ISO-8601 mit Zeitzone</b> (`2026-09-09T18:03:27+00:00`).
 * Angezeigt wird in der Zone des Geräts — eine Schicht, die man um 20 Uhr
 * gefahren hat, soll auch um 20 Uhr im Buch stehen.
 *
 * <b>Alles hier verträgt Unsinn.</b> Ein Zeitstempel, den die App nicht lesen
 * kann, gibt einen Strich und keine Ausnahme: Eine kaputte Zeitangabe darf ein
 * Dienstbuch nicht leer erscheinen lassen.
 */
private val TAG_UND_ZEIT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")
private val NUR_TAG = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** „09.09.2026, 20:03" — für eine Schicht im Buch. */
fun zeitpunkt(roh: String?): String = lesen(roh)?.let {
    TAG_UND_ZEIT.format(it.atZone(ZoneId.systemDefault()))
} ?: "—"

/** „09.09.2026" — wo die Uhrzeit nichts beiträgt (Abzeichen, Kontoalter). */
fun tag(roh: String?): String = lesen(roh)?.let {
    NUR_TAG.format(it.atZone(ZoneId.systemDefault()))
} ?: "—"

/**
 * „vor 3 Minuten", „vor 2 Tagen".
 *
 * <b>Grob und ohne Sekunden.</b> „zuletzt online vor 47 Sekunden" liest niemand
 * als Auskunft, sondern als Überwachung; und bis man es gelesen hat, stimmt es
 * ohnehin nicht mehr.
 */
fun seither(roh: String?): String? {
    val dann = lesen(roh) ?: return null
    val spanne = Duration.between(dann, Instant.now())

    return when {
        spanne.isNegative -> "gerade eben"
        spanne.toMinutes() < 1 -> "gerade eben"
        spanne.toMinutes() < 60 -> "vor " + mehrzahl(spanne.toMinutes(), "Minute", "Minuten")
        spanne.toHours() < 24 -> "vor " + mehrzahl(spanne.toHours(), "Stunde", "Stunden")
        spanne.toDays() < 30 -> "vor " + mehrzahl(spanne.toDays(), "Tag", "Tagen")
        else -> "vor " + mehrzahl(spanne.toDays() / 30, "Monat", "Monaten")
    }
}

/**
 * Ein Wort mit seiner Zahl — in der richtigen Zahlform.
 *
 * „vor 1 Tagen" stand so auf der Freundesliste, und zwar bei jedem, der gestern
 * zuletzt da war. Eine Zeitangabe, die falsch gebeugt ist, liest sich wie eine,
 * die auch inhaltlich nicht stimmt.
 */
private fun mehrzahl(wert: Long, eins: String, viele: String): String =
    "$wert ${if (wert == 1L) eins else viele}"

/** Eine Hilfsfrist in Sekunden als „7:12". */
fun hilfsfrist(sekunden: Int?): String? {
    if (sekunden == null || sekunden < 0) return null
    return "%d:%02d".format(sekunden / 60, sekunden % 60)
}

/**
 * Eine Punktzahl mit Tausendertrennung — „2 785".
 *
 * Schmales Leerzeichen und kein Punkt: Ein Punkt in einer Zahl ist im deutschen
 * Zahlensatz üblich, steht hier aber neben Zeiten und Kennungen, in denen der
 * Punkt schon etwas anderes bedeutet.
 */
fun zahl(wert: Int): String =
    wert.toString().reversed().chunked(3).joinToString(" ").reversed()

private fun lesen(roh: String?): Instant? =
    roh?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?: roh?.let {
            runCatching {
                java.time.OffsetDateTime.parse(it).toInstant()
            }.getOrNull()
        }
