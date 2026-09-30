package de.pagerspass.pagerspass.melder

import android.util.Base64
import kotlinx.serialization.Serializable
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Der Bauplan eines selbst gebauten Melders — Spiegel von `web/src/melder/bauplan.ts`.
 *
 * <b>Gemessen wird in Feldern, nicht in Punkten.</b> Das Gehäuse ist ein Raster
 * aus gleich großen Feldern (im Web sechs Pixel je Feld); jedes Bauteil liegt auf
 * ganzen Feldern. Damit sieht ein Plan auf jeder Bildschirmgröße gleich aus, und
 * der Weitergabe-Code (`PSM2.…`) trägt ganze Zahlen statt Pixeln.
 *
 * <b>Er gehört dem Gerät</b> — dieselbe Linie wie Bauform, Ton und Lautstärke.
 * Weitergegeben wird er als Code, nicht als Datensatz am Konto.
 */
@Serializable
data class Melderbauplan(
    val breite: Int = 36,
    val hoehe: Int = 62,
    val rundung: Int = 3,
    val gehaeuse: String = "#1b1f25",
    val rand: String = "#3a4149",
    val oberflaeche: String = "matt",
    val teile: List<Bauteil> = emptyList(),
)

@Serializable
data class Bauteil(
    val id: String = Bauplaene.kennung(),
    val art: String = "display",
    val x: Int = 0,
    val y: Int = 0,
    val breite: Int = 10,
    val hoehe: Int = 6,
    val farbe: String = "#2f363f",
    val zweitfarbe: String = "#e7edf5",
    val text: String = "",
    val aufgabe: String = "keine",
    val rundung: Int = 1,
    val variante: Int = 0,
)

@Serializable
data class EigenerMelder(val id: String, val name: String, val plan: Melderbauplan)

/** Eine Bauteilart mit ihren Vorgaben — `BAUTEIL_ARTEN` im Web. */
data class Bauteilmuster(
    val art: String,
    val gruppe: String,
    val name: String,
    val was: String,
    val breite: Int,
    val hoehe: Int,
    val farbe: String,
    val zweitfarbe: String,
    val text: String = "",
    val rundung: Int,
    val hatAufgabe: Boolean = false,
    val hatText: Boolean = false,
    val varianten: List<String> = emptyList(),
)

/** Ein Befund der Abnahme — `mangel` hält das Gerät an, `hinweis` nicht. */
data class Befund(val mangel: Boolean, val wort: String, val satz: String, val teil: String? = null)

object Bauplaene {

    val GRENZEN_BREITE = 24..52
    val GRENZEN_HOEHE = 28..96
    const val TEILE_HOECHSTZAHL = 28
    const val MELDER_HOECHSTZAHL = 6
    const val NAME_LAENGE = 24
    const val HAUSMARKE = "PAGERSPASS"
    const val HAUSMODELL = "PS-M1"

    /** Wie viele Zeilen ein Anzeigefeld dieser Höhe trägt — `displayZeilen`. */
    fun displayZeilen(hoeheInFeldern: Int): Int = maxOf(1, (hoeheInFeldern * 6 - 18) / 14)

    val GRUPPEN = listOf(
        "anzeige" to "Anzeige",
        "bedienung" to "Bedienung",
        "gehaeuse" to "Gehäuse",
        "schrift" to "Beschriftung",
    )

    val OBERFLAECHEN = listOf(
        "matt" to "Matt",
        "glanz" to "Hochglanz",
        "gebuerstet" to "Gebürstet",
        "gummi" to "Gummiert",
    )

    val ARTEN: List<Bauteilmuster> = listOf(
        Bauteilmuster("display", "anzeige", "Anzeigefeld", "Zeigt die Meldung, das Menü und die Ruheanzeige", 26, 20, "#a5b34c", "#1a1f0a", "", 1, varianten = listOf("Glatt", "Punktraster", "Hinter Glas")),
        Bauteilmuster("statusleiste", "anzeige", "Statusleiste", "Empfang, Akku und Schleife — die schmale Zeile über dem Feld", 18, 3, "#0f1216", "#8fd14f", "", 0, varianten = listOf("Empfangsbalken", "Punktreihe")),
        Bauteilmuster("leuchte", "anzeige", "Signalleuchte", "Blinkt, solange der Alarm läuft", 3, 3, "#3a1113", "#ff5a4d", "", 2, varianten = listOf("Punkt", "Streifen", "Ring")),
        Bauteilmuster("taste", "bedienung", "Bedientaste", "Mit Aufschrift und Aufgabe — sie tut wirklich etwas", 10, 6, "#2f363f", "#e7edf5", "OK", 1, hatAufgabe = true, hatText = true, varianten = listOf("Flach", "Gewölbt", "Wippe")),
        Bauteilmuster("steuerkreuz", "bedienung", "Steuerkreuz", "Oben und unten blättern, die Mitte trägt deine Aufgabe", 14, 14, "#2f363f", "#e7edf5", "", 7, hatAufgabe = true, varianten = listOf("Rund", "Eckig")),
        Bauteilmuster("schalter", "bedienung", "Schalter", "Schiebeschalter mit zwei Stellungen — mit Aufgabe belegbar", 8, 4, "#1a1f25", "#c8d0da", "", 2, hatAufgabe = true, varianten = listOf("Liegend", "Stehend")),
        Bauteilmuster("rad", "bedienung", "Drehregler", "Das Rad, das am echten Gerät die Lautstärke stellt", 5, 5, "#3a424c", "#0d1014", "", 3, varianten = listOf("Geriffelt", "Glatt mit Marke")),
        Bauteilmuster("gitter", "gehaeuse", "Schallgitter", "Der Schallaustritt — wo der Melder laut wird", 16, 4, "#0c0f13", "#05070a", "", 1, varianten = listOf("Lochraster", "Schlitze", "Wabe")),
        Bauteilmuster("antenne", "gehaeuse", "Antenne", "Der Stummel, der über die Gehäusekante hinausragt", 3, 10, "#1a1f25", "#3a4149", "", 2, varianten = listOf("Stummel", "Lang", "Gewendelt")),
        Bauteilmuster("blende", "gehaeuse", "Frontblende", "Eine Fläche zum Absetzen — Vertiefung, Streifen, Aufsatz", 30, 8, "#151a20", "#00000000", "", 1, varianten = listOf("Flach", "Vertieft", "Erhaben")),
        Bauteilmuster("wulst", "gehaeuse", "Griffwulst", "Die gummierte Kante, an der das Gerät in der Hand liegt", 3, 30, "#14181d", "#2a2f36", "", 1, varianten = listOf("Glatt", "Geriffelt")),
        Bauteilmuster("fuge", "gehaeuse", "Schalenfuge", "Die Naht der beiden Gehäusehälften", 30, 1, "#05070a", "#485260", "", 0),
        Bauteilmuster("clip", "gehaeuse", "Gürtelclip", "Der Bügel, mit dem das Gerät am Gürtel hängt", 4, 16, "#2a2f36", "#454d58", "", 2, varianten = listOf("Bügel", "Drehclip")),
        Bauteilmuster("kontakte", "gehaeuse", "Ladekontakte", "Zwei Blechzungen an der Unterkante", 10, 2, "#00000000", "#b8a45c", "", 0, varianten = listOf("Zwei Zungen", "Vier Zungen")),
        Bauteilmuster("schraube", "gehaeuse", "Schraube", "Die Schraube in der Ecke — sie macht aus einer Fläche ein Gehäuse", 2, 2, "#3a424c", "#0d1014", "", 1, varianten = listOf("Kreuzschlitz", "Schlitz", "Innensechskant")),
        Bauteilmuster("oese", "gehaeuse", "Trageöse", "Für Schlaufe oder Karabiner", 4, 4, "#2a2f36", "#0d1014", "", 2),
        Bauteilmuster("schrift", "schrift", "Beschriftung", "Ortsname, Rufnummer, Wehr — was du willst", 14, 4, "#00000000", "#8797ad", "FF Musterdorf", 0, hatText = true, varianten = listOf("Links", "Mittig", "Gesperrt")),
        Bauteilmuster("typenschild", "schrift", "Typenschild", "Gerahmt, mit Marke und Modell — jedes echte Gerät hat eins", 16, 6, "#0f1216", "#9aa5b3", HAUSMODELL, 1, hatText = true, varianten = listOf("Geprägt", "Aufgeklebt")),
    )

    val AUFGABEN = listOf(
        "quittieren" to "Quittieren",
        "hoch" to "Zurück ▲",
        "runter" to "Weiter ▼",
        "menue" to "Menü",
        "speicher" to "Speicher",
        "ausruecken" to "Ausrücken",
        "keine" to "Zierde",
    )

    fun muster(art: String): Bauteilmuster = ARTEN.firstOrNull { it.art == art } ?: ARTEN.first()

    fun kennung(): String =
        "b${System.currentTimeMillis().toString(36)}${(Math.random() * 46656).toInt().toString(36)}"

    fun bauteil(art: String, x: Int = 0, y: Int = 0, breite: Int? = null, hoehe: Int? = null, text: String? = null, aufgabe: String? = null, variante: Int = 0, farbe: String? = null, zweitfarbe: String? = null): Bauteil {
        val m = muster(art)
        return Bauteil(
            id = kennung(), art = art, x = x, y = y,
            breite = breite ?: m.breite, hoehe = hoehe ?: m.hoehe,
            farbe = farbe ?: m.farbe, zweitfarbe = zweitfarbe ?: m.zweitfarbe,
            text = text ?: m.text,
            aufgabe = aufgabe ?: if (m.hatAufgabe) "offen" else "keine",
            rundung = m.rundung, variante = variante,
        )
    }

    fun neuerBauplan(): Melderbauplan = Melderbauplan(
        teile = listOf(
            bauteil("gitter", 4, 3, 20, 4),
            bauteil("leuchte", 29, 3),
            bauteil("fuge", 0, 9, 36, 1),
            bauteil("statusleiste", 5, 12, 26, 3),
            bauteil("display", 5, 16, 26, 20, variante = 1),
            bauteil("taste", 4, 40, 8, 6, "▲", "hoch"),
            bauteil("taste", 13, 40, 10, 6, "OK", "quittieren", 1),
            bauteil("taste", 24, 40, 8, 6, "▼", "runter"),
            bauteil("taste", 4, 48, 28, 6, "SPEICHER", "speicher"),
            bauteil("typenschild", 10, 55, 16, 5),
            bauteil("schraube", 2, 58, 2, 2),
            bauteil("schraube", 32, 58, 2, 2),
        ),
    )

    /** Die Vorlagen der Werkstatt — `MELDER_VORLAGEN`. */
    val VORLAGEN: List<Triple<String, String, () -> Melderbauplan>> = listOf(
        Triple("Leeres Gehäuse", "Nur die Schale. Jedes Bauteil setzt du selbst.", { neuerBauplan().copy(teile = emptyList()) }),
        Triple("Einfacher Melder", "Anzeigefeld, drei Tasten, Typenschild.", { neuerBauplan() }),
        Triple("Schmaler Piepser", "Klein, hochkant, eine breite Quittiertaste.", {
            Melderbauplan(
                26, 46, 5, "#23262c", "#454c56", "matt",
                listOf(
                    bauteil("gitter", 3, 3, 20, 3, variante = 1),
                    bauteil("leuchte", 11, 7, 4, 2, variante = 1),
                    bauteil("display", 3, 11, 20, 16),
                    bauteil("taste", 3, 30, 8, 5, "▲", "hoch"),
                    bauteil("taste", 15, 30, 8, 5, "▼", "runter"),
                    bauteil("taste", 3, 37, 20, 6, "OK", "quittieren", 1),
                    bauteil("clip", 23, 14, 4, 14),
                ),
            )
        }),
        Triple("Mit Steuerkreuz", "Großes Feld hinter Glas, Steuerkreuz statt Tastenreihe.", {
            Melderbauplan(
                40, 74, 4, "#16191e", "#39404a", "glanz",
                listOf(
                    bauteil("statusleiste", 5, 4, 30, 3),
                    bauteil("display", 4, 8, 32, 26, variante = 2),
                    bauteil("fuge", 0, 36, 40, 1),
                    bauteil("leuchte", 17, 38, 6, 2, variante = 1),
                    bauteil("steuerkreuz", 13, 42, 14, 14, "OK", "quittieren"),
                    bauteil("taste", 3, 44, 9, 6, "MENÜ", "menue"),
                    bauteil("taste", 28, 44, 9, 6, "SPEI", "speicher"),
                    bauteil("gitter", 12, 58, 16, 4, variante = 2),
                    bauteil("typenschild", 12, 64, 16, 6),
                    bauteil("schraube", 2, 69, 2, 2, variante = 1),
                    bauteil("schraube", 36, 69, 2, 2, variante = 1),
                ),
            )
        }),
        Triple("Baustellengerät", "Gummiert, Griffwulst an beiden Seiten, Tasten für Handschuhe.", {
            Melderbauplan(
                40, 68, 6, "#23272d", "#8a5a10", "gummi",
                listOf(
                    bauteil("wulst", 0, 12, 3, 42, variante = 1),
                    bauteil("wulst", 37, 12, 3, 42, variante = 1),
                    bauteil("gitter", 10, 4, 20, 4),
                    bauteil("leuchte", 33, 4, 4, 4, variante = 2),
                    bauteil("statusleiste", 6, 10, 28, 3),
                    bauteil("display", 6, 14, 28, 20),
                    bauteil("taste", 6, 38, 13, 8, "▲", "hoch", 1),
                    bauteil("taste", 21, 38, 13, 8, "▼", "runter", 1),
                    bauteil("taste", 6, 48, 28, 8, "QUITTIEREN", "quittieren", 1),
                    bauteil("oese", 4, 60, 4, 4),
                    bauteil("kontakte", 15, 64, 10, 2),
                ),
            )
        }),
        Triple("Metallgehäuse", "Gebürstet, vier Schrauben, dunkles Display.", {
            Melderbauplan(
                34, 58, 2, "#8d9399", "#5b6167", "gebuerstet",
                listOf(
                    bauteil("schraube", 2, 2, 2, 2, variante = 2, farbe = "#6f767e"),
                    bauteil("leuchte", 15, 2, 4, 2, variante = 1),
                    bauteil("schraube", 30, 2, 2, 2, variante = 2, farbe = "#6f767e"),
                    bauteil("display", 4, 6, 26, 18, variante = 2, farbe = "#1d2a22", zweitfarbe = "#9df0b8"),
                    bauteil("gitter", 11, 25, 12, 3, variante = 1),
                    bauteil("fuge", 0, 29, 34, 1, farbe = "#6b7178", zweitfarbe = "#c3cad2"),
                    bauteil("taste", 4, 32, 12, 6, "MENÜ", "menue"),
                    bauteil("taste", 18, 32, 12, 6, "OK", "quittieren", 1),
                    bauteil("taste", 4, 40, 12, 6, "▲", "hoch"),
                    bauteil("taste", 18, 40, 12, 6, "▼", "runter"),
                    bauteil("typenschild", 9, 49, 16, 6, variante = 1),
                    bauteil("schraube", 2, 54, 2, 2, variante = 2, farbe = "#6f767e"),
                    bauteil("schraube", 30, 54, 2, 2, variante = 2, farbe = "#6f767e"),
                ),
            )
        }),
    )

    /** Die Abnahme — was das Gerät noch braucht, bevor es in den Dienst geht. */
    fun abnahme(plan: Melderbauplan): List<Befund> {
        if (plan.teile.isEmpty()) {
            return listOf(Befund(true, "Noch kein Bauteil", "Auf dem Gehäuse liegt nichts. Fang mit dem Anzeigefeld an."))
        }
        val befunde = mutableListOf<Befund>()
        fun hat(art: String) = plan.teile.any { it.art == art }
        if (!hat("display")) befunde += Befund(true, "Kein Anzeigefeld", "Ohne Anzeigefeld steht die Meldung nirgends.")
        if (plan.teile.none { it.aufgabe == "quittieren" }) {
            befunde += Befund(true, "Nichts quittiert", "Kein Bedienteil trägt die Aufgabe „Quittieren“.")
        }
        plan.teile.filter { muster(it.art).hatAufgabe && it.aufgabe == "offen" }.forEach {
            befunde += Befund(true, "${muster(it.art).name} ohne Aufgabe", "Sag noch, was dieses Bedienteil tun soll. „Zierde“ ist dabei eine Antwort.", it.id)
        }
        plan.teile.filter { it.art == "display" }.forEach {
            val zeilen = displayZeilen(it.hoehe)
            if (zeilen < 4) befunde += Befund(false, if (zeilen == 1) "Anzeigefeld: eine Zeile" else "Anzeigefeld: $zeilen Zeilen", "So flach blätterst du die Meldung einzeln durch.", it.id)
        }
        if (!hat("leuchte")) befunde += Befund(false, "Keine Signalleuchte", "Im Alarm blinkt an diesem Gehäuse nichts.")
        if (!hat("gitter")) befunde += Befund(false, "Kein Schallgitter", "Laut wird der Melder trotzdem — man sieht nur nicht, woher.")
        plan.teile.filter { !(it.x + it.breite > 0 && it.y + it.hoehe > 0 && it.x < plan.breite && it.y < plan.hoehe) }.forEach {
            befunde += Befund(false, "${muster(it.art).name} liegt daneben", "Dieses Teil sitzt außerhalb des Gehäuses.", it.id)
        }
        return befunde
    }

    // ------------------------------------------------------------- Prüfen

    private val FARBE = Regex("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$")

    private fun farbe(wert: String?, vorgabe: String) = wert?.takeIf { FARBE.matches(it) }?.lowercase() ?: vorgabe

    fun pruefen(plan: Melderbauplan): Melderbauplan {
        val aufgaben = AUFGABEN.map { it.first }.toSet() + "offen"
        return Melderbauplan(
            breite = plan.breite.coerceIn(GRENZEN_BREITE),
            hoehe = plan.hoehe.coerceIn(GRENZEN_HOEHE),
            rundung = plan.rundung.coerceIn(0, 12),
            gehaeuse = farbe(plan.gehaeuse, "#1b1f25"),
            rand = farbe(plan.rand, "#3a4149"),
            oberflaeche = plan.oberflaeche.takeIf { o -> OBERFLAECHEN.any { it.first == o } } ?: "matt",
            teile = plan.teile.filter { t -> ARTEN.any { it.art == t.art } }.take(TEILE_HOECHSTZAHL).map { t ->
                val m = muster(t.art)
                t.copy(
                    x = t.x.coerceIn(-52, 104),
                    y = t.y.coerceIn(-96, 192),
                    breite = t.breite.coerceIn(1, 52),
                    hoehe = t.hoehe.coerceIn(1, 96),
                    farbe = farbe(t.farbe, m.farbe),
                    zweitfarbe = farbe(t.zweitfarbe, m.zweitfarbe),
                    text = t.text.replace(Regex("\\s+"), " ").trim().take(16),
                    aufgabe = if (m.hatAufgabe) t.aufgabe.takeIf { it in aufgaben } ?: "offen" else "keine",
                    rundung = t.rundung.coerceIn(0, 24),
                    variante = t.variante.coerceIn(0, maxOf(0, m.varianten.size - 1)),
                )
            },
        )
    }

    // -------------------------------------------------------------- Codes

    private val FASSUNGEN = listOf("PSM2.", "PSM1.")
    private val ART_FOLGE_1 = listOf("display", "taste", "leuchte", "gitter", "rad", "antenne", "schrift", "fuge", "clip", "kontakte", "blende")
    private val AUFGABE_FOLGE get() = AUFGABEN.map { it.first } + "offen"

    /** Nur ein angehängtes Deckkraft-`ff` fällt weg — nicht das Ende einer Farbe wie `#ffffff`. */
    private fun farbeKurz(wert: String): String {
        val ohne = wert.removePrefix("#")
        return if (ohne.length == 8 && ohne.endsWith("ff")) ohne.dropLast(2) else ohne
    }

    private fun kodieren(text: String): String =
        URLEncoder.encode(text, "UTF-8").replace("+", "%20")

    /** Der Weitergabe-Code — derselbe `PSM2.`-Code wie in der Gehäusewerkstatt des Webs. */
    fun codeVon(plan: Melderbauplan): String {
        val arten = ARTEN.map { it.art }
        val kopf = listOf(
            plan.breite, plan.hoehe, plan.rundung,
            OBERFLAECHEN.indexOfFirst { it.first == plan.oberflaeche },
            farbeKurz(plan.gehaeuse), farbeKurz(plan.rand),
        ).joinToString(",")
        val teile = plan.teile.joinToString(";") { t ->
            listOf(
                arten.indexOf(t.art), t.x, t.y, t.breite, t.hoehe, t.rundung,
                farbeKurz(t.farbe), farbeKurz(t.zweitfarbe),
                AUFGABE_FOLGE.indexOf(t.aufgabe), t.variante, kodieren(t.text),
            ).joinToString(",")
        }
        val roh = "$kopf|$teile".toByteArray(Charsets.UTF_8)
        return FASSUNGEN[0] + Base64.encodeToString(roh, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun codeLesen(code: String): Melderbauplan? {
        val sauber = code.trim()
        val fassung = FASSUNGEN.indexOfFirst { sauber.startsWith(it) }
        if (fassung < 0) return null
        val alt = FASSUNGEN[fassung] == "PSM1."
        val folge = if (alt) ART_FOLGE_1 else ARTEN.map { it.art }
        val felder = if (alt) 9 else 10

        val roh = runCatching {
            String(Base64.decode(sauber.removePrefix(FASSUNGEN[fassung]), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrNull() ?: return null

        val kopf = roh.substringBefore("|").split(",")
        val rumpf = roh.substringAfter("|", "")
        if (kopf.size < 6) return null

        val teile = rumpf.split(";").filter { it.isNotBlank() }.mapNotNull { zeile ->
            val f = zeile.split(",")
            if (f.size < felder + 1) return@mapNotNull null
            val art = folge.getOrNull(f[0].toIntOrNull() ?: -1) ?: return@mapNotNull null
            Bauteil(
                id = kennung(), art = art,
                x = f[1].toIntOrNull() ?: 0, y = f[2].toIntOrNull() ?: 0,
                breite = f[3].toIntOrNull() ?: 1, hoehe = f[4].toIntOrNull() ?: 1,
                rundung = f[5].toIntOrNull() ?: 0,
                farbe = "#${f[6]}", zweitfarbe = "#${f[7]}",
                aufgabe = AUFGABE_FOLGE.getOrNull(f[8].toIntOrNull() ?: -1) ?: "offen",
                variante = if (alt) 0 else f[9].toIntOrNull() ?: 0,
                text = runCatching { URLDecoder.decode(f.drop(felder).joinToString(","), "UTF-8") }.getOrDefault(""),
            )
        }

        return pruefen(
            Melderbauplan(
                breite = kopf[0].toDoubleOrNull()?.roundToInt() ?: 36,
                hoehe = kopf[1].toDoubleOrNull()?.roundToInt() ?: 62,
                rundung = kopf[2].toDoubleOrNull()?.roundToInt() ?: 3,
                oberflaeche = OBERFLAECHEN.getOrNull(kopf[3].toIntOrNull() ?: -1)?.first ?: "matt",
                gehaeuse = "#${kopf[4]}",
                rand = "#${kopf[5]}",
                teile = teile,
            ),
        )
    }
}
