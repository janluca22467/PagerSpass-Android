package de.pagerspass.pagerspass.melder

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Die Wellenform eines Tons — dieselben vier wie `OscillatorType` im Web.
 *
 * Die Kennungen sind die des Webs (`sine`, `triangle`, …), weil sie im
 * gespeicherten Bauplan stehen und ein Plan, den jemand als Code weitergibt,
 * auf beiden Seiten dasselbe heißen muss.
 */
enum class Wellenform(val kennung: String, val name_: String, val was: String) {
    Sinus("sine", "Weich", "Sinus — voll und rund, wie ein Gong"),
    Dreieck("triangle", "Klar", "Dreieck — hell, ohne zu stechen"),
    Rechteck("square", "Hart", "Rechteck — der Ton des Dienstmelders"),
    Saegezahn("sawtooth", "Scharf", "Sägezahn — durchdringend wie ein Warnton"),
    ;

    companion object {
        fun aus(kennung: String?): Wellenform = entries.firstOrNull { it.kennung == kennung } ?: Rechteck
    }
}

/** Ein Ton innerhalb eines Durchlaufs — `bis` lässt ihn gleichmäßig gleiten. */
data class Tonereignis(
    val frequenz: Double,
    val versatz: Double,
    val dauer: Double,
    val pegel: Double,
    val form: Wellenform = Wellenform.Rechteck,
    val bis: Double? = null,
)

/** Ein Durchlauf des Melders: so lang, mit diesen Tönen, dann von vorn. */
data class Melderzyklus(val dauer: Double, val ereignisse: List<Tonereignis>)

/**
 * Der Bauplan eines eigenen Tons — Spiegel von `Tonbauplan` in `audio/sounds.ts`.
 *
 * Sechzehn gleich lange Schritte, jeder eine Tonhöhe aus der Leiter oder eine
 * Pause (`null`). Pausen sind Felder wie Töne, weil „di--di" die Hälfte aller
 * echten Meldertöne ausmacht und sich mit einer „Pause am Ende" nicht bauen ließ.
 */
@Serializable
data class Tonbauplan(
    val schritte: List<Int?> = TONBAU_STANDARD_SCHRITTE,
    val abstand: Double = 0.16,
    val dauer: Double = 0.12,
    val form: String = "square",
)

/** Ein gesicherter eigener Ton — gehört dem Gerät, nicht dem Konto. */
@Serializable
data class EigenerTon(val id: String, val name: String, val plan: Tonbauplan)

const val TONBAU_SCHRITTE = 16
const val TONBAU_GRUPPE = 4
const val TONBAU_HOECHSTZAHL = 12

private val TONBAU_STANDARD_SCHRITTE: List<Int?> = listOf<Int?>(880, 1318) + List(14) { null }

/**
 * Die Leiter des Baukastens. <b>Angehängt wird, nie eingeschoben:</b> Der
 * Weitergabe-Code trägt je Schritt die Stelle in dieser Liste, nicht die
 * Frequenz — ein Eintrag in der Mitte verschöbe alle Codes dahinter.
 */
val TONBAU_LEITER: List<Pair<String, Int>> = listOf(
    "Tief" to 392, "Warm" to 523, "Mittel" to 659, "Klar" to 880,
    "Hell" to 1046, "Spitz" to 1318, "Schrill" to 1568, "Sehr hoch" to 1976,
    "Analog 1" to 1060, "Analog 3" to 1270, "Analog 4" to 1400, "Analog 5" to 1530,
    "Analog 6" to 1670, "Analog 7" to 1830, "Analog 9" to 2200,
)

/** Die drei Takte: Name, Schrittabstand, Tondauer — in Sekunden. */
val TONBAU_TAKTE: List<Triple<String, Double, Double>> = listOf(
    Triple("Langsam", 0.26, 0.2),
    Triple("Mittel", 0.16, 0.12),
    Triple("Schnell", 0.09, 0.06),
)

/**
 * Die Klangwerkstatt und die Töne — alles, was ein Melderton ist, ohne das
 * Abspielen (das steht in `Melderspieler`).
 *
 * <b>Die Muster sind Zahl für Zahl die des Webs</b> (`melderzyklus` in
 * `audio/sounds.ts`). Dort baut der Browser sie aus Oszillatoren zusammen; hier
 * wird derselbe Durchlauf einmal in Abtastwerte gerechnet und von der
 * Audiospur in Schleife gespielt. Das Ergebnis ist dasselbe Muster — und ein
 * Ton, den man im Shop gekauft hat, klingt am Handy wie am Rechner.
 */
object Meldertoene {

    const val ABTASTRATE = 44_100

    private object Piezo {
        const val TIEF = 2300.0
        const val MITTEL = 2730.0
        const val HOCH = 3150.0
        const val SPITZ = 3600.0
    }

    private object Membran {
        const val HORN = 147.0
        const val TIEF = 196.0
        const val WARM = 392.0
        const val MITTEL = 523.0
        const val HELL = 784.0
        const val KLAR = 1046.0
    }

    private const val MONITOR = 220.0
    private const val WECKER = 2000.0
    private const val BLINKER = 2400.0
    private val ZVEI = doubleArrayOf(2400.0, 1060.0, 1160.0, 1270.0, 1400.0, 1530.0, 1670.0, 1830.0, 2000.0, 2200.0)
    private const val ZVEI_DAUER = 0.07

    private val R = Wellenform.Rechteck
    private val S = Wellenform.Sinus
    private val D = Wellenform.Dreieck
    private val Z = Wellenform.Saegezahn

    private fun folge(
        frequenzen: List<Double>,
        abstand: Double,
        dauer: Double,
        pegel: Double,
        form: Wellenform = R,
    ) = frequenzen.mapIndexed { i, f -> Tonereignis(f, i * abstand, dauer, pegel, form) }

    /** Wie viele Schritte ein Durchlauf des Baukastens wirklich dauert — bis zum Ende des Viertels mit dem letzten Ton. */
    fun tonbauLaenge(schritte: List<Int?>): Int {
        val letzter = schritte.indexOfLast { it != null }
        if (letzter < 0) return schritte.size
        return min(schritte.size, (letzter / TONBAU_GRUPPE + 1) * TONBAU_GRUPPE)
    }

    /**
     * Der Durchlauf eines Tons.
     *
     * @param prioritaet 1–3: Je dringender der Alarm, desto lauter und dichter —
     *   wie im Web (`pegel` und `tempo`).
     * @param plan Der Bauplan, wenn `art` ein eigener Ton ist (`eigen`, `eigen:…`).
     */
    fun zyklus(art: String, prioritaet: Int = 1, plan: Tonbauplan? = null): Melderzyklus {
        val stufe = prioritaet.coerceIn(1, 3) - 1
        val pegel = 0.27 + stufe * 0.05
        val tempo = 1 - stufe * 0.16
        val t = tempo

        if (art == "eigen" || art.startsWith("eigen:")) {
            val p = tonbauplanPruefen(plan ?: Tonbauplan())
            val schritt = p.abstand * t
            return Melderzyklus(
                dauer = maxOf(0.1, tonbauLaenge(p.schritte) * schritt),
                ereignisse = p.schritte.mapIndexedNotNull { i, f ->
                    f?.let { Tonereignis(it.toDouble(), i * schritt, p.dauer * t, pegel, Wellenform.aus(p.form)) }
                },
            )
        }

        return when (art) {
            "dreiklang" -> Melderzyklus(1.1 * t, listOf(
                Tonereignis(Piezo.MITTEL, 0.0, 0.06, pegel),
                Tonereignis(Piezo.HOCH, 0.1 * t, 0.06, pegel),
                Tonereignis(Piezo.SPITZ, 0.2 * t, 0.09, pegel + 0.03),
            ))
            "warnton" -> Melderzyklus(0.62 * t, listOf(
                Tonereignis(Piezo.HOCH, 0.0, 0.26 * t, pegel),
                Tonereignis(Piezo.HOCH, 0.32 * t, 0.26 * t, pegel),
            ))
            "klassik" -> Melderzyklus(1.25 * t, listOf(
                Tonereignis(Piezo.TIEF, 0.0, 0.34 * t, pegel),
                Tonereignis(Piezo.MITTEL, 0.38 * t, 0.34 * t, pegel),
            ))
            "digital" -> Melderzyklus(1.0 * t, folge(List(4) { Piezo.SPITZ }, 0.075 * t, 0.035, pegel))
            "sirene" -> Melderzyklus(4.2 * t, listOf(
                Tonereignis(150.0, 0.0, 0.55 * t, pegel - 0.17, Z, 305.0),
                Tonereignis(305.0, 0.55 * t, 0.5 * t, pegel - 0.06, Z, 420.0),
                Tonereignis(420.0, 1.05 * t, 1.0 * t, pegel, Z),
                Tonereignis(420.0, 2.05 * t, 0.7 * t, pegel - 0.07, Z, 330.0),
                Tonereignis(330.0, 2.75 * t, 0.6 * t, pegel - 0.15, Z, 255.0),
                Tonereignis(255.0, 3.35 * t, 0.55 * t, pegel - 0.22, Z, 195.0),
            ))
            "gong" -> Melderzyklus(1.4 * t, listOf(
                Tonereignis(Membran.WARM, 0.0, 0.85 * t, pegel, S),
                Tonereignis(Membran.HELL, 0.02, 0.8 * t, pegel - 0.1, S),
            ))
            "puls" -> Melderzyklus(0.72 * t, folge(List(4) { Piezo.HOCH }, 0.085 * t, 0.045, pegel))
            "morse" -> Melderzyklus(1.05 * t, listOf(
                Tonereignis(Piezo.MITTEL, 0.0, 0.06, pegel),
                Tonereignis(Piezo.MITTEL, 0.12 * t, 0.06, pegel),
                Tonereignis(Piezo.MITTEL, 0.24 * t, 0.24 * t, pegel),
            ))
            "aufwecker" -> Melderzyklus(0.95 * t, listOf(
                Tonereignis(Piezo.TIEF, 0.0, 0.07 * t, pegel - 0.06),
                Tonereignis(Piezo.MITTEL, 0.1 * t, 0.07 * t, pegel - 0.03),
                Tonereignis(Piezo.HOCH, 0.2 * t, 0.07 * t, pegel),
                Tonereignis(Piezo.SPITZ, 0.3 * t, 0.14 * t, pegel + 0.03),
            ))
            "doppelton" -> Melderzyklus(0.7 * t, folge(listOf(Piezo.HOCH, Piezo.HOCH), 0.13 * t, 0.08 * t, pegel))
            "wechselton" -> Melderzyklus(1.0 * t, folge(
                listOf(Piezo.MITTEL, Piezo.SPITZ, Piezo.MITTEL, Piezo.SPITZ), 0.16 * t, 0.12 * t, pegel,
            ))
            "tacker" -> Melderzyklus(0.8 * t, folge(List(6) { Piezo.SPITZ }, 0.07 * t, 0.022, pegel - 0.05, Z))
            "steigton" -> Melderzyklus(0.85 * t, listOf(Tonereignis(Piezo.TIEF, 0.0, 0.45 * t, pegel, R, Piezo.SPITZ)))
            "fallton" -> Melderzyklus(0.85 * t, listOf(Tonereignis(Piezo.SPITZ, 0.0, 0.45 * t, pegel, R, Piezo.TIEF)))
            "triller" -> Melderzyklus(0.85 * t, folge(
                listOf(Piezo.HOCH, Piezo.SPITZ, Piezo.HOCH, Piezo.SPITZ, Piezo.HOCH, Piezo.SPITZ), 0.045 * t, 0.035, pegel,
            ))
            "kaskade" -> Melderzyklus(0.95 * t, folge(
                listOf(Piezo.SPITZ, Piezo.HOCH, Piezo.MITTEL, Piezo.TIEF), 0.11 * t, 0.09 * t, pegel, D,
            ))
            "nachtwache" -> Melderzyklus(1.5 * t, listOf(
                Tonereignis(Membran.WARM, 0.0, 0.5 * t, pegel - 0.08, S),
                Tonereignis(330.0, 0.55 * t, 0.55 * t, pegel - 0.1, S),
            ))
            "zirpen" -> Melderzyklus(0.7 * t, listOf(
                Tonereignis(Piezo.HOCH, 0.0, 0.04, pegel, R, Piezo.SPITZ),
                Tonereignis(Piezo.HOCH, 0.09 * t, 0.04, pegel, R, Piezo.SPITZ),
            ))
            "hornruf" -> Melderzyklus(1.1 * t, listOf(
                Tonereignis(330.0, 0.0, 0.35 * t, pegel, Z),
                Tonereignis(Membran.WARM, 0.4 * t, 0.5 * t, pegel, Z),
            ))
            "taktschlag" -> Melderzyklus(0.8 * t, listOf(
                Tonereignis(Piezo.SPITZ, 0.0, 0.1 * t, pegel + 0.04),
                Tonereignis(Piezo.MITTEL, 0.16 * t, 0.06 * t, pegel - 0.08),
                Tonereignis(Piezo.MITTEL, 0.26 * t, 0.06 * t, pegel - 0.08),
            ))
            "doppelschlag" -> Melderzyklus(0.95 * t, listOf(0.0, 0.09, 0.3, 0.39).map {
                Tonereignis(Piezo.HOCH, it * t, 0.05, pegel)
            })
            "wellenton" -> Melderzyklus(1.1 * t, listOf(
                Tonereignis(Piezo.MITTEL, 0.0, 0.5 * t, pegel, D, Piezo.SPITZ),
                Tonereignis(Piezo.SPITZ, 0.52 * t, 0.5 * t, pegel, D, Piezo.MITTEL),
            ))
            "pfiff" -> Melderzyklus(0.6 * t, listOf(Tonereignis(Piezo.HOCH, 0.0, 0.1 * t, pegel, D, Piezo.SPITZ)))
            "glocke" -> Melderzyklus(1.25 * t, listOf(
                Tonereignis(Membran.KLAR, 0.0, 0.3 * t, pegel, D),
                Tonereignis(Membran.KLAR, 0.35 * t, 0.3 * t, pegel - 0.06, D),
                Tonereignis(Membran.KLAR, 0.7 * t, 0.35 * t, pegel - 0.12, D),
            ))
            "sprungton" -> Melderzyklus(0.9 * t, folge(
                listOf(Piezo.TIEF, Piezo.SPITZ, Piezo.TIEF, Piezo.SPITZ), 0.12 * t, 0.09 * t, pegel,
            ))
            "schnellfolge" -> Melderzyklus(0.72 * t, folge(
                listOf(2400.0, 2650.0, 2900.0, 3150.0, 3400.0, 3650.0), 0.055 * t, 0.04, pegel,
            ))
            "leitton" -> Melderzyklus(1.05 * t, listOf(
                Tonereignis(Piezo.MITTEL, 0.0, 0.35 * t, pegel),
                Tonereignis(Piezo.SPITZ, 0.4 * t, 0.1 * t, pegel + 0.03),
            ))
            "stakkato" -> Melderzyklus(0.85 * t, folge(List(8) { Piezo.SPITZ }, 0.048 * t, 0.022, pegel))
            "fanfare" -> Melderzyklus(1.05 * t, listOf(
                Tonereignis(Membran.MITTEL, 0.0, 0.12 * t, pegel, Z),
                Tonereignis(659.0, 0.14 * t, 0.12 * t, pegel, Z),
                Tonereignis(Membran.HELL, 0.28 * t, 0.12 * t, pegel, Z),
                Tonereignis(Membran.KLAR, 0.42 * t, 0.32 * t, pegel + 0.03, Z),
            ))
            "grosslage" -> Melderzyklus(1.0 * t, listOf(
                Tonereignis(Piezo.TIEF, 0.0, 0.18 * t, pegel, R, Piezo.HOCH),
                Tonereignis(Piezo.TIEF, 0.2 * t, 0.18 * t, pegel, R, Piezo.HOCH),
                Tonereignis(Piezo.SPITZ, 0.44 * t, 0.06, pegel + 0.04),
                Tonereignis(Piezo.SPITZ, 0.54 * t, 0.06, pegel + 0.04),
            ))
            "kommando" -> Melderzyklus(1.15 * t, listOf(
                Tonereignis(Piezo.TIEF, 0.0, 0.4 * t, pegel),
                Tonereignis(Piezo.SPITZ, 0.46 * t, 0.07, pegel),
                Tonereignis(Piezo.SPITZ, 0.57 * t, 0.07, pegel),
            ))
            "nebelhorn" -> Melderzyklus(1.5 * t, listOf(Tonereignis(190.0, 0.0, 0.75 * t, pegel, Z, 150.0)))
            "weckuhr" -> Melderzyklus(0.9 * t, folge(
                List(8) { if (it % 2 == 0) WECKER else 1700.0 }, 0.045 * t, 0.03, pegel - 0.03, D,
            ))
            "funkruf" -> Melderzyklus(1.2 * t, listOf(2, 5, 8, 1, 4).mapIndexed { i, ziffer ->
                Tonereignis(ZVEI[ziffer], i * ZVEI_DAUER, ZVEI_DAUER, pegel - 0.04, S)
            } + Tonereignis(1750.0, 5 * ZVEI_DAUER + 0.12, 0.42 * t, pegel, S))
            "herzschlag" -> Melderzyklus(1.0 * t, listOf(
                Tonereignis(MONITOR, 0.0, 0.12 * t, pegel, S),
                Tonereignis(180.0, 0.17 * t, 0.16 * t, pegel - 0.05, S),
            ))
            "pendel" -> Melderzyklus(1.1 * t, folge(listOf(Membran.KLAR, 880.0), 0.4 * t, 0.16 * t, pegel, D))
            "feuerglocke" -> Melderzyklus(1.0 * t, listOf(
                Tonereignis(1320.0, 0.0, 0.22 * t, pegel, D, 1180.0),
                Tonereignis(1320.0, 0.28 * t, 0.22 * t, pegel - 0.04, D, 1180.0),
                Tonereignis(1320.0, 0.56 * t, 0.26 * t, pegel - 0.08, D, 1180.0),
            ))
            "tiefton" -> Melderzyklus(0.7 * t, listOf(Tonereignis(330.0, 0.0, 0.35 * t, pegel)))
            "blinker" -> Melderzyklus(0.55 * t, listOf(
                Tonereignis(BLINKER, 0.0, 0.035, pegel - 0.05, Z),
                Tonereignis(WECKER, 0.07 * t, 0.035, pegel - 0.05, Z),
            ))
            "edelklang" -> Melderzyklus(1.2 * t, listOf(
                Tonereignis(Membran.MITTEL, 0.0, 0.22 * t, pegel, S),
                Tonereignis(Membran.KLAR, 0.0, 0.22 * t, pegel - 0.12, S),
                Tonereignis(659.0, 0.24 * t, 0.22 * t, pegel, S),
                Tonereignis(1318.0, 0.24 * t, 0.22 * t, pegel - 0.12, S),
                Tonereignis(Membran.HELL, 0.48 * t, 0.38 * t, pegel, S),
                Tonereignis(1568.0, 0.48 * t, 0.38 * t, pegel - 0.12, S),
            ))
            "nachtsignal" -> Melderzyklus(1.1 * t, listOf(
                Tonereignis(Membran.TIEF, 0.0, 0.6 * t, pegel - 0.06, S),
                Tonereignis(988.0, 0.1 * t, 0.08 * t, pegel - 0.1, D),
                Tonereignis(988.0, 0.3 * t, 0.08 * t, pegel - 0.1, D),
            ))
            "platinruf" -> Melderzyklus(0.9 * t, listOf(
                Tonereignis(1174.0, 0.0, 0.1 * t, pegel, D),
                Tonereignis(1568.0, 0.12 * t, 0.34 * t, pegel, D, 1760.0),
            ))
            "sturmglocke" -> Melderzyklus(1.5 * t, (0..4).map { i ->
                Tonereignis(Membran.KLAR, i * 0.22 * t, 0.2 * t, pegel - i * 0.035, D, 940.0)
            })
            "taktfeuer" -> Melderzyklus(1.0 * t,
                folge(List(6) { 880.0 }, 0.13 * t, 0.06 * t, pegel, R) +
                    folge(List(6) { 440.0 }, 0.13 * t, 0.06 * t, pegel - 0.1, S),
            )
            "hallruf" -> Melderzyklus(1.35 * t, listOf(
                Tonereignis(Membran.HELL, 0.0, 0.14 * t, pegel, S),
                Tonereignis(Membran.KLAR, 0.16 * t, 0.2 * t, pegel, S),
                Tonereignis(698.0, 0.52 * t, 0.14 * t, pegel - 0.09, S),
                Tonereignis(932.0, 0.68 * t, 0.26 * t, pegel - 0.09, S),
            ))
            "wachengong" -> Melderzyklus(1.6 * t, listOf(
                Tonereignis(659.0, 0.0, 0.4 * t, pegel, S),
                Tonereignis(1318.0, 0.02, 0.34 * t, pegel - 0.12, S),
                Tonereignis(Membran.MITTEL, 0.42 * t, 0.4 * t, pegel, S),
                Tonereignis(Membran.KLAR, 0.44 * t, 0.34 * t, pegel - 0.12, S),
                Tonereignis(Membran.WARM, 0.84 * t, 0.55 * t, pegel, S),
                Tonereignis(Membran.HELL, 0.86 * t, 0.48 * t, pegel - 0.12, S),
            ))
            "glutwelle" -> Melderzyklus(1.6 * t, listOf(
                Tonereignis(Membran.WARM, 0.0, 0.6 * t, pegel, Z, 622.0),
                Tonereignis(622.0, 0.62 * t, 0.6 * t, pegel, Z, Membran.WARM),
                Tonereignis(Membran.TIEF, 0.0, 1.22 * t, pegel - 0.14, S),
            ))
            "silberton" -> Melderzyklus(0.95 * t, listOf(
                Tonereignis(1568.0, 0.0, 0.16 * t, pegel, D),
                Tonereignis(2349.0, 0.0, 0.16 * t, pegel - 0.1, D),
                Tonereignis(1976.0, 0.22 * t, 0.26 * t, pegel, D),
                Tonereignis(2960.0, 0.22 * t, 0.26 * t, pegel - 0.1, D),
            ))
            "doppelhorn" -> Melderzyklus(1.5 * t, listOf(
                Tonereignis(Membran.HORN, 0.0, 0.34 * t, pegel, Z),
                Tonereignis(MONITOR, 0.0, 0.34 * t, pegel - 0.08, Z),
                Tonereignis(Membran.HORN, 0.46 * t, 0.62 * t, pegel, Z),
                Tonereignis(MONITOR, 0.46 * t, 0.62 * t, pegel - 0.08, Z),
            ))
            // Der Zweiklang — und alles, was die App (noch) nicht kennt. Ein
            // stummer Melder ist der eine Fehler, den man erst nach dem Einsatz merkt.
            else -> Melderzyklus(0.62 * t, listOf(
                Tonereignis(Piezo.MITTEL, 0.0, 0.1 * t, pegel),
                Tonereignis(Piezo.SPITZ, 0.12 * t, 0.1 * t, pegel),
            ))
        }
    }

    /**
     * Einen Durchlauf in Abtastwerte rechnen — 16 Bit, einkanalig.
     *
     * <b>Rechteck und Sägezahn mit PolyBLEP.</b> Ein Piezo-Ton liegt bei drei
     * Kilohertz; ein nackt gerechnetes Rechteck hat dort Obertöne weit über der
     * halben Abtastrate, die als falsche Töne zurückspiegeln. Der Browser
     * rechnet seine Oszillatoren bandbegrenzt — die kleine Korrektur an den
     * Sprungstellen tut hier dasselbe, und der Ton klingt nicht verstimmt.
     *
     * Die Hüllkurve ist die des Webs: 8 ms hinein, 12 ms hinaus — ohne sie
     * knackt jeder Ton an beiden Enden.
     */
    fun abtasten(zyklus: Melderzyklus, lautstaerke: Float = 1f): ShortArray {
        val rate = ABTASTRATE.toDouble()
        val laenge = maxOf(1, (zyklus.dauer * rate).roundToInt())
        val summe = DoubleArray(laenge)

        for (e in zyklus.ereignisse) {
            val start = (e.versatz * rate).roundToInt()
            val n = (e.dauer * rate).roundToInt()
            var phase = 0.0
            for (i in 0 until n) {
                val stelle = start + i
                if (stelle >= laenge) break
                val zeit = i / rate
                val f = if (e.bis != null) e.frequenz + (e.bis - e.frequenz) * (i.toDouble() / n) else e.frequenz
                val schritt = f / rate
                val wert = when (e.form) {
                    Wellenform.Sinus -> sin(2 * PI * phase)
                    Wellenform.Dreieck -> 1 - 4 * abs(phase - 0.5)
                    Wellenform.Rechteck -> {
                        var w = if (phase < 0.5) 1.0 else -1.0
                        w += blep(phase, schritt)
                        w -= blep((phase + 0.5) % 1.0, schritt)
                        w
                    }
                    Wellenform.Saegezahn -> 2 * phase - 1 - blep(phase, schritt)
                }
                val huelle = min(1.0, min(zeit / 0.008, (e.dauer - zeit) / 0.012)).coerceAtLeast(0.0)
                summe[stelle] += wert * e.pegel * huelle
                phase += schritt
                phase -= floor(phase)
            }
        }

        val faktor = 32767.0 * lautstaerke.coerceIn(0f, 1f)
        return ShortArray(laenge) { i -> (summe[i].coerceIn(-1.0, 1.0) * faktor).toInt().toShort() }
    }

    private fun blep(t: Double, dt: Double): Double = when {
        dt <= 0 -> 0.0
        t < dt -> { val x = t / dt; x + x - x * x - 1 }
        t > 1 - dt -> { val x = (t - 1) / dt; x * x + x + x + 1 }
        else -> 0.0
    }

    // ------------------------------------------------------------ Baupläne

    /**
     * Einen Plan gesund machen — dieselbe Haltung wie `tonbauplanPruefen` im
     * Web: Was von Hand verbogen wurde, darf höchstens sich selbst kaputt machen.
     */
    fun tonbauplanPruefen(plan: Tonbauplan): Tonbauplan {
        val hoehen = TONBAU_LEITER.map { it.second }.toSet()
        val schritte = List(TONBAU_SCHRITTE) { i -> plan.schritte.getOrNull(i)?.takeIf { it in hoehen } }
        return Tonbauplan(
            schritte = if (schritte.any { it != null }) schritte else TONBAU_STANDARD_SCHRITTE,
            abstand = plan.abstand.takeIf { it.isFinite() }?.coerceIn(0.06, 0.6) ?: 0.16,
            dauer = plan.dauer.takeIf { it.isFinite() }?.coerceIn(0.03, 0.45) ?: 0.12,
            form = Wellenform.aus(plan.form).kennung,
        )
    }

    private const val CODE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val CODE_LAENGE = 16
    private const val CODE_FASSUNG = 1

    /**
     * Der Weitergabe-Code eines Plans — sechzehn Zeichen, dieselben wie im Web.
     *
     * Ein Kopfbyte (Fassung, Farbe, Takt), acht Bytes für die sechzehn Schritte
     * (vier Bit je Schritt: Stelle in der Leiter, 0 ist Pause) und eine
     * Prüfsumme, in Crockford-Base32 ohne die verwechselbaren Buchstaben.
     */
    fun tonCodeVon(plan: Tonbauplan): String {
        val p = tonbauplanPruefen(plan)
        val hoehen = TONBAU_LEITER.map { it.second }
        val farbe = Wellenform.entries.indexOf(Wellenform.aus(p.form)).coerceAtLeast(0)
        val takt = TONBAU_TAKTE.indices.minByOrNull { abs(TONBAU_TAKTE[it].second - p.abstand) } ?: 1
        val stufen = p.schritte.map { s -> if (s == null) 0 else hoehen.indexOf(s) + 1 }

        val bytes = mutableListOf((CODE_FASSUNG shl 4) or (farbe shl 2) or takt)
        for (i in 0 until TONBAU_SCHRITTE step 2) bytes += (stufen[i] shl 4) or stufen[i + 1]
        bytes += pruefsumme(bytes)
        return zuBase32(bytes)
    }

    fun tonCodeLesen(code: String): Tonbauplan? {
        val sauber = code.uppercase()
            .replace(Regex("[IL]"), "1")
            .replace("O", "0")
            .replace(Regex("[^0-9A-Z]"), "")
        if (sauber.length != CODE_LAENGE) return null

        val bytes = ausBase32(sauber) ?: return null
        val nutz = bytes.dropLast(1)
        if (bytes.last() != pruefsumme(nutz)) return null

        val kopf = nutz[0]
        if (kopf shr 4 != CODE_FASSUNG) return null

        val stufen = nutz.drop(1).flatMap { listOf(it shr 4, it and 0xf) }
        val hoehen = TONBAU_LEITER.map { it.second }
        val takt = TONBAU_TAKTE.getOrNull(kopf and 0b11) ?: TONBAU_TAKTE[1]

        return tonbauplanPruefen(
            Tonbauplan(
                schritte = stufen.map { n -> if (n == 0) null else hoehen.getOrNull(n - 1) },
                abstand = takt.second,
                dauer = takt.third,
                form = Wellenform.entries.getOrNull((kopf shr 2) and 0b11)?.kennung ?: "square",
            ),
        )
    }

    private fun pruefsumme(bytes: List<Int>): Int {
        var summe = 0x5a
        for (b in bytes) {
            summe = ((summe shl 1) or (summe ushr 7)) and 0xff
            summe = summe xor b
        }
        return summe
    }

    private fun zuBase32(bytes: List<Int>): String {
        var bits = 0
        var wert = 0
        val aus = StringBuilder()
        for (b in bytes) {
            wert = (wert shl 8) or b
            bits += 8
            while (bits >= 5) {
                aus.append(CODE_ALPHABET[(wert shr (bits - 5)) and 31])
                bits -= 5
            }
            wert = wert and ((1 shl bits) - 1)
        }
        return aus.toString()
    }

    private fun ausBase32(text: String): List<Int>? {
        var bits = 0
        var wert = 0
        val aus = mutableListOf<Int>()
        for (z in text) {
            val stelle = CODE_ALPHABET.indexOf(z)
            if (stelle < 0) return null
            wert = (wert shl 5) or stelle
            bits += 5
            if (bits >= 8) {
                aus += (wert shr (bits - 8)) and 0xff
                bits -= 8
            }
            wert = wert and ((1 shl bits) - 1)
        }
        return aus
    }
}
