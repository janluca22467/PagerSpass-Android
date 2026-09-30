package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Übertragung einer Schicht („Streamer-Modus") — übertragen aus
 * `web/src/types.ts` (Abschnitt „Übertragung einer Schicht").
 *
 * <b>Der Gegenstand, in den eingewilligt werden soll</b>, kommt vom Server, sobald
 * ein Beitritt allein an der Einwilligung scheitert (`JoinResult.einwilligung`).
 * Der Client setzt ihn beim Beitritt nicht selbst zusammen: Er kennt den Raum
 * noch nicht — Kanal, Plattform und Mitschnitt stehen in Einstellungen, die er erst
 * mit dem Raumzustand bekäme, und den bekommt er ja gerade nicht.
 */
@Serializable
data class Einwilligungsbedarf(
    val raumCode: String = "",
    /** Wer überträgt — der Anzeigename der Leitstelle dieser Runde. */
    val streamerName: String = "",
    val plattform: String = "Andere",
    val kanal: String = "",
    val aufzeichnung: Boolean = false,
    /** Die angezeigte Fassung — sie geht beim Erteilen zurück. */
    val fassung: String = UEBERTRAGUNG_FASSUNG,
    /** Ab welchem Alter man allein einwilligt; darunter kommen die Eltern dazu. */
    val mindestalterAllein: Int = MINDESTALTER_EINWILLIGUNG,
    /** Ob für diesen Kanal schon ein Vorgang bei den Erziehungsberechtigten liegt. */
    val wartetAufEltern: Boolean = false,
    /** Die Adresse des Bogens für die Eltern; `null`, wenn keiner läuft. */
    val elternbogen: String? = null,
)

/** Ab diesem Alter willigt man allein ein — `MINDESTALTER_EINWILLIGUNG` im Web. */
const val MINDESTALTER_EINWILLIGUNG = 18

/**
 * Höchstlänge des Kanalnamens — Spiegel von `Streamerangabe.MaxKanal`. Gleich und
 * nicht größer: Der Server kürzt nicht, er weist ab.
 */
const val MAX_STREAMERKANAL = 120

/**
 * Die Plattformen ausgeschrieben. „Andere" heißt hier „Anderswo": Die Aufschrift
 * steht im Einwilligungsbogen neben dem Kanal, und dort ist es eine Ortsangabe.
 */
val STREAMERPLATTFORMEN: List<Pair<String, String>> = listOf(
    "Twitch" to "Twitch",
    "YouTube" to "YouTube",
    "TikTok" to "TikTok",
    "Kick" to "Kick",
    "Discord" to "Discord",
    "Andere" to "Anderswo",
)

fun plattformName(id: String): String = STREAMERPLATTFORMEN.firstOrNull { it.first == id }?.second ?: id
