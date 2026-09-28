package de.pagerspass.pagerspass.mobil

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Die Alarmtöne des Melders — übertragen aus `melderzyklus()` in
 * `web/src/audio/sounds.ts`.
 *
 * <b>Hören, bevor man zahlt.</b> Im Laden stand „Feuerglocke" und ein Preis;
 * wonach das klingt, erfuhr man erst nach dem Kauf. Ein Rahmen zeigt sich auf der
 * Bühne der Kachel, ein Ton hat kein Bild — also spielt die Kachel ihn vor.
 *
 * <b>Dieselben Zahlen wie im Web.</b> Jeder Ton ist eine Folge von Ereignissen
 * (Frequenz, Versatz, Dauer, Pegel, Wellenform, wahlweise ein Gleiten) in einem
 * Zyklus fester Länge. Das Web spielt sie über Web Audio; hier werden sie einmal
 * in einen Puffer gerechnet und über `AudioTrack` abgespielt — dieselbe Hüllkurve
 * (8 ms an, 12 ms aus), damit ein Ton am Handy nicht anders klickt als am Rechner.
 *
 * <b>Die Probe spielt den Ton in Priorität 1</b> und so lange wie im Web
 * (`composables/tonprobe.ts`): einen vollen Zyklus plus eine Viertelsekunde,
 * mindestens aber zwei Sekunden.
 */
object Meldertonprobe {

    private enum class Form { Sinus, Rechteck, Saege, Dreieck }

    private class Ereignis(
        val frequenz: Double,
        val versatz: Double,
        val dauer: Double,
        val pegel: Double,
        val form: Form = Form.Rechteck,
        val bis: Double? = null,
    )

    /**
     * Ein Durchlauf. `klang` trägt den eingespielten Klang, wenn dieser Ton einer ist
     * — statt der Ereignisse, nicht neben ihnen (`puffer` im Web).
     */
    private class Zyklus(
        val dauer: Double,
        val ereignisse: List<Ereignis>,
        val klang: FloatArray? = null,
        val klangPegel: Double = 0.0,
    )

    // Die drei Register aus sounds.ts — Piezo, Membran, Gerät.
    private const val PIEZO_TIEF = 2300.0
    private const val PIEZO_MITTEL = 2730.0
    private const val PIEZO_HOCH = 3150.0
    private const val PIEZO_SPITZ = 3600.0
    private const val MEMBRAN_HORN = 147.0
    private const val MEMBRAN_TIEF = 196.0
    private const val MEMBRAN_WARM = 392.0
    private const val MEMBRAN_MITTEL = 523.0
    private const val MEMBRAN_HELL = 784.0
    private const val MEMBRAN_KLAR = 1046.0
    private const val GERAET_MONITOR = 220.0
    private const val GERAET_WECKER = 2000.0
    private const val GERAET_BLINKER = 2400.0

    /** Die Fünftonfolge nach ZVEI — gemessen, nicht gewählt. */
    private val ZVEI = listOf(2400.0, 1060.0, 1160.0, 1270.0, 1400.0, 1530.0, 1670.0, 1830.0, 2000.0, 2200.0)
    private const val ZVEI_DAUER = 0.07

    private const val ABTASTRATE = 44_100
    private const val PROBE_MINDEST = 2.0
    private const val PROBE_PUFFER = 0.25

    /** Eine Folge gleich langer Töne im festen Abstand. */
    private fun folge(
        frequenzen: List<Double>,
        abstand: Double,
        dauer: Double,
        pegel: Double,
        form: Form = Form.Rechteck,
    ): List<Ereignis> = frequenzen.mapIndexed { i, f -> Ereignis(f, i * abstand, dauer, pegel, form) }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun zyklus(art: String, prioritaet: Int = 1): Zyklus {
        val stufe = (prioritaet.coerceIn(1, 3)) - 1
        val p = 0.27 + stufe * 0.05
        val t = 1.0 - stufe * 0.16
        val R = Form.Rechteck
        val S = Form.Sinus
        val W = Form.Saege
        val D = Form.Dreieck

        // Tonwerkstatt: die eigenen Töne — gezeichnet (`eigen:`) oder eingespielt (`klang:`).
        /*
         * Der eingespielte Klang zuerst. Liegt er nicht bereit (frisch gestartet, Datei
         * noch nicht decodiert, nicht mehr lesbar), fällt die Rechnung durch bis zum
         * Zweiklang am Ende — lieber der Standardton als Stille. Die Dringlichkeit
         * verkürzt die Ruhe und nicht den Klang: Eine Aufnahme schneller abzuspielen
         * hieße, sie höher zu machen.
         */
        if (art.startsWith("klang:")) {
            Eigentoene.klangstueck(art.removePrefix("klang:"))?.let { stueck ->
                return Zyklus(
                    dauer = max(0.1, stueck.proben.size.toDouble() / ABTASTRATE + stueck.pause * t),
                    ereignisse = emptyList(),
                    klang = stueck.proben,
                    klangPegel = Eigentoene.klangPegel(stueck, p),
                )
            }
        }

        // Der selbst gebaute Ton: dieselben beiden Größen wie jeder Zweig darunter —
        // `t` beschleunigt ihn mit der Dringlichkeit, `p` bleibt der aus der Rechnung.
        if (art == "eigen" || art.startsWith("eigen:")) {
            val plan = Eigentoene.planZuArt(art)
            val schritt = plan.abstand * t
            val form = when (plan.form) {
                "sine" -> S
                "triangle" -> D
                "sawtooth" -> W
                else -> R
            }
            return Zyklus(
                // Der Boden von einer Zehntelsekunde bleibt — ein kürzerer Zyklus
                // füllte den Puffer in einer Schleife, die nie fertig wird.
                dauer = max(0.1, Eigentoene.laenge(plan.schritte) * schritt),
                // Leere Felder erzeugen keinen Ton — sie verschieben nur die folgenden.
                ereignisse = plan.schritte.mapIndexedNotNull { i, frequenz ->
                    frequenz?.let { Ereignis(it.toDouble(), i * schritt, plan.dauer * t, p, form) }
                },
            )
        }
        // Ende Tonwerkstatt

        return when (art) {
            "dreiklang" -> Zyklus(
                1.1 * t,
                listOf(
                    Ereignis(PIEZO_MITTEL, 0.0, 0.06, p, R),
                    Ereignis(PIEZO_HOCH, 0.1 * t, 0.06, p, R),
                    Ereignis(PIEZO_SPITZ, 0.2 * t, 0.09, p + 0.03, R),
                ),
            )
            "warnton" -> Zyklus(
                0.62 * t,
                listOf(
                    Ereignis(PIEZO_HOCH, 0.0, 0.26 * t, p, R),
                    Ereignis(PIEZO_HOCH, 0.32 * t, 0.26 * t, p, R),
                ),
            )
            "klassik" -> Zyklus(
                1.25 * t,
                listOf(
                    Ereignis(PIEZO_TIEF, 0.0, 0.34 * t, p, R),
                    Ereignis(PIEZO_MITTEL, 0.38 * t, 0.34 * t, p, R),
                ),
            )
            "digital" -> Zyklus(
                1.0 * t,
                folge(List(4) { PIEZO_SPITZ }, 0.075 * t, 0.035, p),
            )
            "sirene" -> Zyklus(
                4.2 * t,
                listOf(
                    Ereignis(150.0, 0.0, 0.55 * t, p - 0.17, W, 305.0),
                    Ereignis(305.0, 0.55 * t, 0.5 * t, p - 0.06, W, 420.0),
                    Ereignis(420.0, 1.05 * t, 1.0 * t, p, W),
                    Ereignis(420.0, 2.05 * t, 0.7 * t, p - 0.07, W, 330.0),
                    Ereignis(330.0, 2.75 * t, 0.6 * t, p - 0.15, W, 255.0),
                    Ereignis(255.0, 3.35 * t, 0.55 * t, p - 0.22, W, 195.0),
                ),
            )
            "gong" -> Zyklus(
                1.4 * t,
                listOf(
                    Ereignis(MEMBRAN_WARM, 0.0, 0.85 * t, p, S),
                    Ereignis(MEMBRAN_HELL, 0.02, 0.8 * t, p - 0.1, S),
                ),
            )
            "puls" -> Zyklus(0.72 * t, folge(List(4) { PIEZO_HOCH }, 0.085 * t, 0.045, p))
            "morse" -> Zyklus(
                1.05 * t,
                listOf(
                    Ereignis(PIEZO_MITTEL, 0.0, 0.06, p, R),
                    Ereignis(PIEZO_MITTEL, 0.12 * t, 0.06, p, R),
                    Ereignis(PIEZO_MITTEL, 0.24 * t, 0.24 * t, p, R),
                ),
            )
            "aufwecker" -> Zyklus(
                0.95 * t,
                listOf(
                    Ereignis(PIEZO_TIEF, 0.0, 0.07 * t, p - 0.06, R),
                    Ereignis(PIEZO_MITTEL, 0.1 * t, 0.07 * t, p - 0.03, R),
                    Ereignis(PIEZO_HOCH, 0.2 * t, 0.07 * t, p, R),
                    Ereignis(PIEZO_SPITZ, 0.3 * t, 0.14 * t, p + 0.03, R),
                ),
            )
            "doppelton" -> Zyklus(0.7 * t, folge(List(2) { PIEZO_HOCH }, 0.13 * t, 0.08 * t, p))
            "wechselton" -> Zyklus(
                1.0 * t,
                folge(listOf(PIEZO_MITTEL, PIEZO_SPITZ, PIEZO_MITTEL, PIEZO_SPITZ), 0.16 * t, 0.12 * t, p),
            )
            "tacker" -> Zyklus(0.8 * t, folge(List(6) { PIEZO_SPITZ }, 0.07 * t, 0.022, p - 0.05, W))
            "steigton" -> Zyklus(0.85 * t, listOf(Ereignis(PIEZO_TIEF, 0.0, 0.45 * t, p, R, PIEZO_SPITZ)))
            "fallton" -> Zyklus(0.85 * t, listOf(Ereignis(PIEZO_SPITZ, 0.0, 0.45 * t, p, R, PIEZO_TIEF)))
            "triller" -> Zyklus(
                0.85 * t,
                folge(
                    listOf(PIEZO_HOCH, PIEZO_SPITZ, PIEZO_HOCH, PIEZO_SPITZ, PIEZO_HOCH, PIEZO_SPITZ),
                    0.045 * t, 0.035, p,
                ),
            )
            "kaskade" -> Zyklus(
                0.95 * t,
                folge(listOf(PIEZO_SPITZ, PIEZO_HOCH, PIEZO_MITTEL, PIEZO_TIEF), 0.11 * t, 0.09 * t, p, D),
            )
            "nachtwache" -> Zyklus(
                1.5 * t,
                listOf(
                    Ereignis(MEMBRAN_WARM, 0.0, 0.5 * t, p - 0.08, S),
                    Ereignis(330.0, 0.55 * t, 0.55 * t, p - 0.1, S),
                ),
            )
            "zirpen" -> Zyklus(
                0.7 * t,
                listOf(
                    Ereignis(PIEZO_HOCH, 0.0, 0.04, p, R, PIEZO_SPITZ),
                    Ereignis(PIEZO_HOCH, 0.09 * t, 0.04, p, R, PIEZO_SPITZ),
                ),
            )
            "hornruf" -> Zyklus(
                1.1 * t,
                listOf(
                    Ereignis(330.0, 0.0, 0.35 * t, p, W),
                    Ereignis(MEMBRAN_WARM, 0.4 * t, 0.5 * t, p, W),
                ),
            )
            "taktschlag" -> Zyklus(
                0.8 * t,
                listOf(
                    Ereignis(PIEZO_SPITZ, 0.0, 0.1 * t, p + 0.04, R),
                    Ereignis(PIEZO_MITTEL, 0.16 * t, 0.06 * t, p - 0.08, R),
                    Ereignis(PIEZO_MITTEL, 0.26 * t, 0.06 * t, p - 0.08, R),
                ),
            )
            "doppelschlag" -> Zyklus(
                0.95 * t,
                listOf(
                    Ereignis(PIEZO_HOCH, 0.0, 0.05, p, R),
                    Ereignis(PIEZO_HOCH, 0.09 * t, 0.05, p, R),
                    Ereignis(PIEZO_HOCH, 0.3 * t, 0.05, p, R),
                    Ereignis(PIEZO_HOCH, 0.39 * t, 0.05, p, R),
                ),
            )
            "wellenton" -> Zyklus(
                1.1 * t,
                listOf(
                    Ereignis(PIEZO_MITTEL, 0.0, 0.5 * t, p, D, PIEZO_SPITZ),
                    Ereignis(PIEZO_SPITZ, 0.52 * t, 0.5 * t, p, D, PIEZO_MITTEL),
                ),
            )
            "pfiff" -> Zyklus(0.6 * t, listOf(Ereignis(PIEZO_HOCH, 0.0, 0.1 * t, p, D, PIEZO_SPITZ)))
            "glocke" -> Zyklus(
                1.25 * t,
                listOf(
                    Ereignis(MEMBRAN_KLAR, 0.0, 0.3 * t, p, D),
                    Ereignis(MEMBRAN_KLAR, 0.35 * t, 0.3 * t, p - 0.06, D),
                    Ereignis(MEMBRAN_KLAR, 0.7 * t, 0.35 * t, p - 0.12, D),
                ),
            )
            "sprungton" -> Zyklus(
                0.9 * t,
                folge(listOf(PIEZO_TIEF, PIEZO_SPITZ, PIEZO_TIEF, PIEZO_SPITZ), 0.12 * t, 0.09 * t, p),
            )
            "schnellfolge" -> Zyklus(
                0.72 * t,
                folge(listOf(2400.0, 2650.0, 2900.0, 3150.0, 3400.0, 3650.0), 0.055 * t, 0.04, p),
            )
            "leitton" -> Zyklus(
                1.05 * t,
                listOf(
                    Ereignis(PIEZO_MITTEL, 0.0, 0.35 * t, p, R),
                    Ereignis(PIEZO_SPITZ, 0.4 * t, 0.1 * t, p + 0.03, R),
                ),
            )
            "stakkato" -> Zyklus(0.85 * t, folge(List(8) { PIEZO_SPITZ }, 0.048 * t, 0.022, p))
            "fanfare" -> Zyklus(
                1.05 * t,
                listOf(
                    Ereignis(MEMBRAN_MITTEL, 0.0, 0.12 * t, p, W),
                    Ereignis(659.0, 0.14 * t, 0.12 * t, p, W),
                    Ereignis(MEMBRAN_HELL, 0.28 * t, 0.12 * t, p, W),
                    Ereignis(MEMBRAN_KLAR, 0.42 * t, 0.32 * t, p + 0.03, W),
                ),
            )
            "grosslage" -> Zyklus(
                1.0 * t,
                listOf(
                    Ereignis(PIEZO_TIEF, 0.0, 0.18 * t, p, R, PIEZO_HOCH),
                    Ereignis(PIEZO_TIEF, 0.2 * t, 0.18 * t, p, R, PIEZO_HOCH),
                    Ereignis(PIEZO_SPITZ, 0.44 * t, 0.06, p + 0.04, R),
                    Ereignis(PIEZO_SPITZ, 0.54 * t, 0.06, p + 0.04, R),
                ),
            )
            "kommando" -> Zyklus(
                1.15 * t,
                listOf(
                    Ereignis(PIEZO_TIEF, 0.0, 0.4 * t, p, R),
                    Ereignis(PIEZO_SPITZ, 0.46 * t, 0.07, p, R),
                    Ereignis(PIEZO_SPITZ, 0.57 * t, 0.07, p, R),
                ),
            )
            "nebelhorn" -> Zyklus(1.5 * t, listOf(Ereignis(190.0, 0.0, 0.75 * t, p, W, 150.0)))
            "weckuhr" -> Zyklus(
                0.9 * t,
                folge(
                    List(8) { if (it % 2 == 0) GERAET_WECKER else 1700.0 },
                    0.045 * t, 0.03, p - 0.03, D,
                ),
            )
            "funkruf" -> Zyklus(
                1.2 * t,
                listOf(2, 5, 8, 1, 4).mapIndexed { i, ziffer ->
                    Ereignis(ZVEI[ziffer], i * ZVEI_DAUER, ZVEI_DAUER, p - 0.04, S)
                } + Ereignis(1750.0, 5 * ZVEI_DAUER + 0.12, 0.42 * t, p, S),
            )
            "herzschlag" -> Zyklus(
                1.0 * t,
                listOf(
                    Ereignis(GERAET_MONITOR, 0.0, 0.12 * t, p, S),
                    Ereignis(180.0, 0.17 * t, 0.16 * t, p - 0.05, S),
                ),
            )
            "pendel" -> Zyklus(1.1 * t, folge(listOf(MEMBRAN_KLAR, 880.0), 0.4 * t, 0.16 * t, p, D))
            "feuerglocke" -> Zyklus(
                1.0 * t,
                listOf(
                    Ereignis(1320.0, 0.0, 0.22 * t, p, D, 1180.0),
                    Ereignis(1320.0, 0.28 * t, 0.22 * t, p - 0.04, D, 1180.0),
                    Ereignis(1320.0, 0.56 * t, 0.26 * t, p - 0.08, D, 1180.0),
                ),
            )
            "tiefton" -> Zyklus(0.7 * t, listOf(Ereignis(330.0, 0.0, 0.35 * t, p, R)))
            "blinker" -> Zyklus(
                0.55 * t,
                listOf(
                    Ereignis(GERAET_BLINKER, 0.0, 0.035, p - 0.05, W),
                    Ereignis(GERAET_WECKER, 0.07 * t, 0.035, p - 0.05, W),
                ),
            )
            "edelklang" -> Zyklus(
                1.2 * t,
                listOf(
                    Ereignis(MEMBRAN_MITTEL, 0.0, 0.22 * t, p, S),
                    Ereignis(MEMBRAN_KLAR, 0.0, 0.22 * t, p - 0.12, S),
                    Ereignis(659.0, 0.24 * t, 0.22 * t, p, S),
                    Ereignis(1318.0, 0.24 * t, 0.22 * t, p - 0.12, S),
                    Ereignis(MEMBRAN_HELL, 0.48 * t, 0.38 * t, p, S),
                    Ereignis(1568.0, 0.48 * t, 0.38 * t, p - 0.12, S),
                ),
            )
            "nachtsignal" -> Zyklus(
                1.1 * t,
                listOf(
                    Ereignis(MEMBRAN_TIEF, 0.0, 0.6 * t, p - 0.06, S),
                    Ereignis(988.0, 0.1 * t, 0.08 * t, p - 0.1, D),
                    Ereignis(988.0, 0.3 * t, 0.08 * t, p - 0.1, D),
                ),
            )
            "platinruf" -> Zyklus(
                0.9 * t,
                listOf(
                    Ereignis(1174.0, 0.0, 0.1 * t, p, D),
                    Ereignis(1568.0, 0.12 * t, 0.34 * t, p, D, 1760.0),
                ),
            )
            "sturmglocke" -> Zyklus(
                1.5 * t,
                (0..4).map { i ->
                    Ereignis(MEMBRAN_KLAR, i * 0.22 * t, 0.2 * t, p - i * 0.035, D, 940.0)
                },
            )
            "taktfeuer" -> Zyklus(
                1.0 * t,
                folge(List(6) { 880.0 }, 0.13 * t, 0.06 * t, p, R) +
                    folge(List(6) { 440.0 }, 0.13 * t, 0.06 * t, p - 0.1, S),
            )
            "hallruf" -> Zyklus(
                1.35 * t,
                listOf(
                    Ereignis(MEMBRAN_HELL, 0.0, 0.14 * t, p, S),
                    Ereignis(MEMBRAN_KLAR, 0.16 * t, 0.2 * t, p, S),
                    Ereignis(698.0, 0.52 * t, 0.14 * t, p - 0.09, S),
                    Ereignis(932.0, 0.68 * t, 0.26 * t, p - 0.09, S),
                ),
            )
            "wachengong" -> Zyklus(
                1.6 * t,
                listOf(
                    Ereignis(659.0, 0.0, 0.4 * t, p, S),
                    Ereignis(1318.0, 0.02, 0.34 * t, p - 0.12, S),
                    Ereignis(MEMBRAN_MITTEL, 0.42 * t, 0.4 * t, p, S),
                    Ereignis(MEMBRAN_KLAR, 0.44 * t, 0.34 * t, p - 0.12, S),
                    Ereignis(MEMBRAN_WARM, 0.84 * t, 0.55 * t, p, S),
                    Ereignis(MEMBRAN_HELL, 0.86 * t, 0.48 * t, p - 0.12, S),
                ),
            )
            "glutwelle" -> Zyklus(
                1.6 * t,
                listOf(
                    Ereignis(MEMBRAN_WARM, 0.0, 0.6 * t, p, W, 622.0),
                    Ereignis(622.0, 0.62 * t, 0.6 * t, p, W, MEMBRAN_WARM),
                    Ereignis(MEMBRAN_TIEF, 0.0, 1.22 * t, p - 0.14, S),
                ),
            )
            "silberton" -> Zyklus(
                0.95 * t,
                listOf(
                    Ereignis(1568.0, 0.0, 0.16 * t, p, D),
                    Ereignis(2349.0, 0.0, 0.16 * t, p - 0.1, D),
                    Ereignis(1976.0, 0.22 * t, 0.26 * t, p, D),
                    Ereignis(2960.0, 0.22 * t, 0.26 * t, p - 0.1, D),
                ),
            )
            "doppelhorn" -> Zyklus(
                1.5 * t,
                listOf(
                    Ereignis(MEMBRAN_HORN, 0.0, 0.34 * t, p, W),
                    Ereignis(GERAET_MONITOR, 0.0, 0.34 * t, p - 0.08, W),
                    Ereignis(MEMBRAN_HORN, 0.46 * t, 0.62 * t, p, W),
                    Ereignis(GERAET_MONITOR, 0.46 * t, 0.62 * t, p - 0.08, W),
                ),
            )
            // Der Zweiklang — und alles, was hier (noch) nicht steht. Lieber der
            // Standardton als Stille: Ein stummer Melder ist der eine Fehler, den
            // man erst bemerkt, wenn der Einsatz vorbei ist.
            else -> Zyklus(
                0.62 * t,
                listOf(
                    Ereignis(PIEZO_MITTEL, 0.0, 0.1 * t, p, R),
                    Ereignis(PIEZO_SPITZ, 0.12 * t, 0.1 * t, p, R),
                ),
            )
        }
    }

    /** Wie lange eine Probe dieses Tons dauert — in Sekunden. */
    fun probedauer(art: String): Double = max(PROBE_MINDEST, zyklus(art).dauer + PROBE_PUFFER)

    /** Wie lang ein Durchlauf dieses Tons ist, in Sekunden — `tonZyklusMs` im Web. */
    fun zyklusdauer(art: String, prioritaet: Int = 1): Double = zyklus(art, prioritaet).dauer

    /**
     * Den Ton in einen Puffer rechnen: so viele Zyklen hintereinander, wie in die
     * Probe passen, am Ende abgeschnitten.
     */
    fun rechnen(art: String, sekunden: Double = probedauer(art), prioritaet: Int = 1): ShortArray {
        val zyklus = zyklus(art, prioritaet)
        val laenge = (sekunden * ABTASTRATE).roundToInt().coerceAtLeast(1)
        val summe = DoubleArray(laenge)
        val runden = ceil(sekunden / max(0.1, zyklus.dauer)).toInt()

        for (runde in 0 until runden) {
            val beginn = runde * zyklus.dauer
            zyklus.klang?.let { proben ->
                val von = (beginn * ABTASTRATE).roundToInt()
                for (i in proben.indices) {
                    val stelle = von + i
                    if (stelle >= laenge) break
                    summe[stelle] += proben[i] * zyklus.klangPegel
                }
            }
            for (e in zyklus.ereignisse) {
                if (e.dauer <= 0.0 || e.pegel <= 0.0) continue
                val von = ((beginn + e.versatz) * ABTASTRATE).roundToInt()
                val anzahl = (e.dauer * ABTASTRATE).roundToInt()
                var phase = 0.0

                for (i in 0 until anzahl) {
                    val stelle = von + i
                    if (stelle < 0) continue
                    if (stelle >= laenge) break
                    val zeit = i.toDouble() / ABTASTRATE
                    val anteil = zeit / e.dauer
                    val frequenz = e.bis?.let { e.frequenz + (it - e.frequenz) * anteil } ?: e.frequenz
                    phase += frequenz / ABTASTRATE
                    val bruch = phase - floor(phase)

                    val welle = when (e.form) {
                        Form.Sinus -> sin(2.0 * PI * bruch)
                        Form.Rechteck -> if (bruch < 0.5) 1.0 else -1.0
                        Form.Saege -> 2.0 * bruch - 1.0
                        Form.Dreieck -> 1.0 - 4.0 * abs(bruch - 0.5)
                    }

                    // Die Hüllkurve des Webs: 8 ms hinauf, 12 ms hinunter.
                    val huelle = when {
                        zeit < 0.008 -> zeit / 0.008
                        zeit > e.dauer - 0.012 -> max(0.0, (e.dauer - zeit) / 0.012)
                        else -> 1.0
                    }

                    summe[stelle] += welle * e.pegel * huelle
                }
            }
        }

        return ShortArray(laenge) { i ->
            (min(1.0, max(-1.0, summe[i])) * Short.MAX_VALUE).roundToInt().toShort()
        }
    }

    // Fahrzeug: der Alarm im Dienst — endlos, in der Dringlichkeit der Meldung.
    /**
     * Ganze Zyklen des Tons, mindestens drei Sekunden lang — die Spur läuft
     * darauf in der Schleife (`Melderwerk`). Ganze Zyklen, damit die Naht nicht
     * mitten in einem Ton liegt.
     */
    fun alarmschleife(art: String, prioritaet: Int): ShortArray {
        val dauer = max(0.1, zyklus(art, prioritaet).dauer)
        val runden = ceil(3.0 / dauer).toInt().coerceAtLeast(1)
        return rechnen(art, runden * dauer, prioritaet)
    }

    /** Die Abtastrate der Puffer — für Spuren, die sie selbst anlegen. */
    const val RATE = ABTASTRATE

    /** Einen gerechneten Puffer einmal abspielen — die Spur gibt der Aufrufer frei. */
    fun abspielen(puffer: ShortArray): AudioTrack {
        val spur = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(ABTASTRATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(puffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        spur.write(puffer, 0, puffer.size)
        spur.play()
        return spur
    }
}

/**
 * Die Probe an einer Kachel — welcher Ton gerade läuft, und der eine Knopf hin
 * und zurück.
 *
 * <b>Welcher, nicht nur ob.</b> Im Laden stehen mehrere Töne nebeneinander, und
 * die Kachel, die gerade tönt, soll das auch zeigen; mit einem Wahrheitswert
 * leuchteten alle.
 */
class Tonprobe internal constructor(private val bereich: CoroutineScope) {

    /** Die Id des Tons, der gerade läuft — oder `null`. */
    var laeuft by mutableStateOf<String?>(null)
        private set

    private var spur: AudioTrack? = null
    private var auftrag: Job? = null

    /** Derselbe Knopf hin und zurück: Wer den laufenden Ton noch einmal drückt, macht Schluss. */
    fun umschalten(art: String) {
        if (laeuft == art) beenden() else spielen(art)
    }

    /**
     * @param dauer Wie lange die Probe läuft; ohne Angabe die Probedauer der Tonwahl.
     *   Die Tonwerkstatt spielt genau einen Durchlauf und eine Viertelsekunde.
     */
    fun spielen(art: String, dauer: Double? = null) {
        beenden()
        laeuft = art
        auftrag = bereich.launch {
            val sekunden = dauer ?: Meldertonprobe.probedauer(art)
            val puffer = withContext(Dispatchers.Default) { Meldertonprobe.rechnen(art, sekunden) }
            val neu = runCatching { Meldertonprobe.abspielen(puffer) }.getOrNull()
            if (neu == null) {
                laeuft = null
                return@launch
            }
            spur = neu
            delay((sekunden * 1000).toLong())
            beenden()
        }
    }

    /**
     * Abstellen und freigeben. Der Auftrag wird zuerst gelöscht — sonst schaltete
     * der Zeitgeber der vorigen Probe später mitten in eine neue hinein ab.
     */
    fun beenden() {
        val alt = auftrag
        auftrag = null
        spur?.let { s ->
            runCatching { s.stop() }
            runCatching { s.release() }
        }
        spur = null
        laeuft = null
        alt?.cancel()
    }
}

/** Eine Probe, die mit der Seite geht: Wer sie verlässt, nimmt keinen Ton mit. */
@Composable
fun rememberTonprobe(): Tonprobe {
    // Die eigenen Töne müssen dastehen, bevor eine Probe nach ihnen fragt.
    val zusammenhang = androidx.compose.ui.platform.LocalContext.current
    remember(zusammenhang) { Eigentoene.sicherstellen(zusammenhang) }
    val bereich = rememberCoroutineScope()
    val probe = remember { Tonprobe(bereich) }
    DisposableEffect(probe) { onDispose { probe.beenden() } }
    return probe
}
