package de.pagerspass.pagerspass.ansichten

import androidx.compose.ui.graphics.Color
import de.pagerspass.pagerspass.ui.theme.Farben
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Was die Seiten von Dienstbuch und Shop gemeinsam brauchen: Zeiten, Dauern,
 * Vorzeichen, die Farben und Namen der Organisationen und Träger.
 *
 * <b>Neben `Zeit.kt`, nicht darin.</b> Dort stehen die Angaben, die überall in der
 * App gleich aussehen; hier die, die nur das Buch so schreibt — „7:12 min", „vor 3
 * Tagen" nicht, dafür „23.09." und „−5".
 */

private val DEUTSCH = Locale.GERMAN

/** Ein Zeitstempel des Servers — oder `null`, wenn er nicht zu lesen ist. */
internal fun zeitVon(roh: String?): Instant? {
    if (roh.isNullOrBlank()) return null
    return runCatching { Instant.parse(roh) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(roh).toInstant() }.getOrNull()
}

private fun ortszeit(roh: String?) = zeitVon(roh)?.atZone(ZoneId.systemDefault())

/** „23.09." — wo der Monat als Anker reicht. */
internal fun tagMonat(roh: String?): String =
    ortszeit(roh)?.format(DateTimeFormatter.ofPattern("dd.MM.")) ?: "—"

/** „23.09.26, 20:14" — die Zeile unter einem Diagrammpunkt. */
internal fun kurzzeitpunkt(roh: String?): String =
    ortszeit(roh)?.format(DateTimeFormatter.ofPattern("dd.MM.yy, HH:mm")) ?: "—"

/** „23.09.2026, 20:14:07" — für die Textdatei des Funkprotokolls. */
internal fun zeitGenau(roh: String?): String =
    ortszeit(roh)?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss")) ?: "—"

/** „September 2026" — der Kopf einer Monatsgruppe. */
internal fun monatJahr(roh: String?): String {
    val z = ortszeit(roh) ?: return "Ohne Datum"
    val monat = z.month.getDisplayName(TextStyle.FULL_STANDALONE, DEUTSCH)
    return "$monat ${z.year}"
}

/** „Sep" — das Kalenderblatt einer Schicht. */
internal fun monatKurz(roh: String?): String =
    ortszeit(roh)?.month?.getDisplayName(TextStyle.SHORT, DEUTSCH)?.removeSuffix(".") ?: ""

/** „Di" — der Wochentag am Kalenderblatt. */
internal fun wochentagKurz(roh: String?): String =
    ortszeit(roh)?.dayOfWeek?.getDisplayName(TextStyle.SHORT, DEUTSCH)?.removeSuffix(".") ?: ""

/** „07" — der Tag am Kalenderblatt. */
internal fun tagImMonat(roh: String?): String =
    ortszeit(roh)?.format(DateTimeFormatter.ofPattern("dd")) ?: "—"

/** „20:14". */
internal fun uhrzeitKurz(roh: String?): String =
    ortszeit(roh)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""

/**
 * Eine Dauer, wie das Buch sie schreibt: „7:12 min" ab einer Minute, sonst „42 s".
 * `null` wird ein Strich.
 */
internal fun dauerText(sekunden: Double?): String {
    if (sekunden == null) return "—"
    val s = sekunden.roundToInt()
    return if (s >= 60) "${s / 60}:${(s % 60).toString().padStart(2, '0')} min" else "$s s"
}

/** „7:12" — ohne Einheit, für Achsen und Kennzahlen mit eigener Einheit daneben. */
internal fun minutenSekunden(sekunden: Double?): String {
    if (sekunden == null) return "—"
    val s = sekunden.roundToInt()
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/**
 * Punkte mit Vorzeichen. Fast alles ist Gewinn, aber ein verpasster Notruf steht
 * mit Minus da — ein festes „+" davor machte aus einem Abzug „+-5".
 */
internal fun mitVorzeichen(punkte: Int): String =
    if (punkte < 0) "−${zahl(-punkte)}" else "+${zahl(punkte)}"

/** Eine Zahl mit Minuszeichen statt Bindestrich, wenn sie negativ ist. */
internal fun zahlMitMinus(wert: Int): String = if (wert < 0) "−${zahl(abs(wert))}" else zahl(wert)

/** Eine Kommazahl mit höchstens einer Stelle — „3,5". */
internal fun eineStelle(wert: Double): String {
    val gerundet = (wert * 10).roundToInt() / 10.0
    return if (gerundet == gerundet.toInt().toDouble()) {
        gerundet.toInt().toString()
    } else {
        gerundet.toString().replace('.', ',')
    }
}

/** „1 Schicht" / „3 Schichten" — ein Wort mit seiner Zahl, richtig gebeugt. */
internal fun mitWort(wert: Int, eins: String, viele: String): String =
    "${zahl(wert)} ${if (wert == 1) eins else viele}"

// ----------------------------------------------------------- Organisationen

/** Die Farbe einer Organisation — dieselben vier wie am Fahrzeug und auf der Karte. */
internal fun orgTon(organisation: String?): Color = when (organisation) {
    "Feuerwehr" -> Farben.OrgFeuerwehr
    "Rettungsdienst" -> Farben.OrgRettungsdienst
    "Thw" -> Farben.OrgThw
    "Polizei" -> Farben.OrgPolizei
    "Leitstelle" -> Farben.ViolettHell
    else -> Farben.RandHell
}

/** Wie eine Organisation heißt, wenn ein Mensch sie liest — „THW" statt „Thw". */
internal fun orgName(organisation: String): String = when (organisation) {
    "Thw" -> "THW"
    else -> organisation
}

/** Die vier Organisationen in ihrer festen Reihenfolge. */
internal val BUCH_ORGANISATIONEN = listOf("Feuerwehr", "Rettungsdienst", "Thw", "Polizei")

/** Der Kurzname eines Trägers — leer für „Keine". */
internal fun traegerName(hiOrg: String): String = when (hiOrg) {
    "Drk" -> "DRK"
    "Juh" -> "Johanniter"
    "Mhd" -> "Malteser"
    "Asb" -> "ASB"
    "Dlrg" -> "DLRG"
    "Bergwacht" -> "Bergwacht"
    "Wasserwacht" -> "Wasserwacht"
    "Dgzrs" -> "Seenotretter"
    "Brh" -> "Rettungshunde"
    "Werkfeuerwehr" -> "Werkfeuerwehr"
    "Privat" -> "Privater RD"
    else -> ""
}

/** Der volle Name eines Trägers — für das Schaufenster des Autohauses. */
internal fun traegerLang(hiOrg: String): String = when (hiOrg) {
    "Drk" -> "Deutsches Rotes Kreuz"
    "Juh" -> "Johanniter-Unfall-Hilfe"
    "Mhd" -> "Malteser Hilfsdienst"
    "Asb" -> "Arbeiter-Samariter-Bund"
    "Dlrg" -> "Deutsche Lebens-Rettungs-Gesellschaft"
    "Bergwacht" -> "Bergwacht"
    "Wasserwacht" -> "Wasserwacht"
    "Dgzrs" -> "Deutsche Gesellschaft zur Rettung Schiffbrüchiger"
    "Brh" -> "Bundesverband Rettungshunde"
    "Werkfeuerwehr" -> "Werkfeuerwehr"
    "Privat" -> "Privater Rettungsdienst"
    else -> ""
}

/** Die eigene Farbe eines Trägers — DRK und Johanniter auf einen Blick getrennt. */
internal fun traegerTon(hiOrg: String): Color = when (hiOrg) {
    "Drk" -> Farben.HiorgDrk
    "Juh" -> Farben.HiorgJuh
    "Mhd" -> Farben.HiorgMhd
    "Asb" -> Farben.HiorgAsb
    "Dlrg" -> Farben.HiorgDlrg
    "Bergwacht" -> Farben.HiorgBergwacht
    "Wasserwacht" -> Farben.HiorgWasserwacht
    "Dgzrs" -> Farben.HiorgDgzrs
    "Brh" -> Farben.HiorgBrh
    "Werkfeuerwehr" -> Farben.HiorgWerkfeuerwehr
    "Privat" -> Farben.HiorgPrivat
    else -> Color.Transparent
}
