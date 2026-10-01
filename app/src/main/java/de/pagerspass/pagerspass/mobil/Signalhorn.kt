package de.pagerspass.pagerspass.mobil

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Eine Hornart des Bedienteils — dieselben neun wie `HORN_ARTEN` in `audio/sounds.ts`
 * (Web v6), mit denselben Zahlen.
 *
 * <b>Warum als Daten und nicht als neun Funktionen.</b> Die neun unterscheiden sich in
 * fünf Zahlen und sonst in nichts — das Bedienteil liest dieselbe Liste, um seine
 * Knöpfe zu beschriften. Eine zweite Liste dort liefe eines Tages auseinander.
 *
 * Die Aufnahmen, die das Web vom Server holt (`/api/hoerner`), lädt die App nicht: Sie
 * spielt den Nachbau, den das Web als Rückfall spielt. Ein Tastendruck ergibt sofort
 * ein Horn, ohne Netz.
 */
data class Hornbauart(
    val id: String,
    val name: String,
    /** Die Zeile darunter — was man hört, in einem Satz. */
    val was: String,
    /** Der tiefe der beiden Töne in Hertz. Der hohe ist eine Quarte darüber. */
    val grundton: Double,
    /** Wie lange der Übergang zwischen den Tönen dauert. */
    val gleit: Double,
    val saege: Boolean,
    val oberton: Double = 0.0,
    val obertonPegel: Double = 0.0,
    val rumpler: Double = 0.0,
    val rumplerPegel: Double = 0.3,
    /** Verstimmung des Schallbecher-Paars in Hertz — die Schwebung des Martin-Horns. */
    val paarabstand: Double = 0.0,
    /** Mechanisches Aufgleiten beim Einschalten, in Sekunden. */
    val anlauf: Double = 0.008,
    val glanz: Double = 0.0,
    val glanzPegel: Double = 0.0,
    /** Die laufende Pumpe des Kompressorhorns: Brummhöhe, Pegel, Stöße je Sekunde, Tiefe. */
    val pumpe: DoubleArray? = null,
    /** Der Jaulton des Anhaltesignals: bis wohin, und wie lang ein Durchlauf ist. */
    val jaulenBis: Double? = null,
    val jaulenPeriode: Double = 0.3,
)

object Signalhorn {

    /** Wie lange ein Ton eines Folgetonhorns steht: 750 ms, bei jeder Bauart (DIN 14610). */
    private const val HALT = 0.75

    /** Das Quartverhältnis — nimmt man es weg, klingt es wie eine Hupe. */
    private const val QUARTE = 4.0 / 3.0

    private const val RATE = 44_100

    val ARTEN: List<Hornbauart> = listOf(
        Hornbauart(
            "pressluft", "Pressluft", "Martin-Horn aus dem Kessel — a ′ und d ″, voll, rau, mit Tremolo",
            grundton = 435.0, gleit = 0.045, saege = true, oberton = 2.0, obertonPegel = 0.28,
            paarabstand = 5.0, anlauf = 0.09,
        ),
        Hornbauart(
            "stadt", "Stadt", "Die Stadtkennung nach DIN: 410 und 547 Hertz, breit zur Seite",
            grundton = 410.0, gleit = 0.008, saege = false,
        ),
        Hornbauart(
            "stadt2", "Stadt 2", "Dieselbe Kennung, eine Spur höher gestimmt — das zweite Werk",
            grundton = 440.0, gleit = 0.008, saege = false, paarabstand = 2.0,
        ),
        Hornbauart(
            "land", "Land", "Die Landkennung nach DIN: 362 und 483 Hertz, nach vorn geworfen",
            grundton = 362.0, gleit = 0.008, saege = false, glanz = 5.0, glanzPegel = 0.13,
        ),
        Hornbauart(
            "land2", "Land 2", "Landkennung, etwas höher gestimmt — der ältere Satz",
            grundton = 380.0, gleit = 0.01, saege = false, glanz = 5.0, glanzPegel = 0.1,
        ),
        Hornbauart(
            "kompressor", "Kompressor", "Luft vom laufenden Motor, höher gestimmter Satz — es brummt und pulsiert mit",
            grundton = 455.0, gleit = 0.03, saege = true, oberton = 2.0, obertonPegel = 0.14,
            paarabstand = 3.0, anlauf = 0.04, pumpe = doubleArrayOf(118.0, 0.13, 7.5, 0.26),
        ),
        Hornbauart(
            "ehorn", "E-Horn", "Das nüchterne DIN-Signal — sauber, hart geschaltet, ohne Schwebung",
            grundton = 420.0, gleit = 0.006, saege = false,
        ),
        Hornbauart(
            "starkton", "Starkton", "Elektronischer Starktongeber — tiefer Zusatz, harter Schnitt, kein Tremolo",
            grundton = 400.0, gleit = 0.008, saege = true, oberton = 2.0, obertonPegel = 0.16,
            rumpler = 0.5, rumplerPegel = 0.34, glanz = 3.0, glanzPegel = 0.14,
        ),
        Hornbauart(
            "anhalt", "Anhaltesignal", "Der Jaulton zum Anhalten — schnelles Auf und Ab, kein Wegerecht",
            grundton = 800.0, gleit = 0.006, saege = false, jaulenBis = 1700.0, jaulenPeriode = 0.3,
        ),
    )

    /** Die Bauart zu einer Kennung; unbekanntes fällt auf die Stadtfolge zurück. */
    fun bauart(id: String?): Hornbauart = ARTEN.firstOrNull { it.id == id } ?: ARTEN[1]

    @Volatile private var laeuft: String? = null
    @Volatile private var pegel = 0.7f
    private var spur: AudioTrack? = null
    private var faden: Thread? = null

    /** Welches Horn gerade läuft — `null` heißt: keines. */
    fun laeuft(): String? = laeuft

    /** 0 … 1, vom Regler des Bedienteils. Gilt sofort, auch im laufenden Ton. */
    fun lautstaerke(wert: Float) {
        pegel = wert.coerceIn(0f, 1f)
        pegelNeu()
    }

    /**
     * Den wirksamen Pegel neu setzen: der eigene Regler mal dem Umgebungsregler — das
     * Horn hängt im Web an der Gruppe „Umgebung", und der Stumm-Knopf trifft es mit.
     */
    fun pegelNeu() {
        runCatching { spur?.setVolume(pegel * Tonstand.umgebung()) }
    }

    /**
     * Das Horn einschalten. Läuft, bis [stoppen] es abstellt.
     *
     * <b>Gerechnet wird laufend, nicht als Schleife.</b> Eine fertige Schleife hätte an
     * der Naht einen Phasensprung — und ein Knacken alle drei Sekunden hört man bei
     * einem Signal, das minutenlang läuft. Ein eigener Faden schreibt deshalb Block für
     * Block nach, mit durchlaufenden Phasen.
     */
    @Synchronized
    fun starten(id: String) {
        // Dasselbe Horn ein zweites Mal: Der laufende Ton ist schon der richtige.
        if (laeuft == id) return
        stoppen()
        val bau = bauart(id)
        val neu = runCatching {
            AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
                maxOf(
                    AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
                    RATE / 5 * 2,
                ),
                AudioTrack.MODE_STREAM,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
            )
        }.onFailure { Log.w("Signalhorn", "Kein Ton", it) }.getOrNull() ?: return

        runCatching {
            neu.setVolume(pegel * Tonstand.umgebung())
            neu.play()
        }
        spur = neu
        laeuft = id
        faden = Thread({ schreiben(neu, bau, id) }, "Signalhorn").also { it.start() }
    }

    @Synchronized
    fun stoppen() {
        laeuft = null
        faden?.interrupt()
        faden = null
        spur?.let { s ->
            runCatching { s.pause(); s.flush(); s.stop() }
            s.release()
        }
        spur = null
    }

    private fun schreiben(spur: AudioTrack, bau: Hornbauart, id: String) {
        val block = ShortArray(1024)
        val tief = bau.grundton
        val hoch = bau.grundton * QUARTE
        // Die Phasen der Stimmen — sie laufen über die Blöcke durch.
        var grund = 0.0
        var paar = 0.0
        var ober = 0.0
        var rumpel = 0.0
        var glanz = 0.0
        var motor = 0.0
        var n = 0L
        val summe = 1.0 + (if (bau.paarabstand > 0) 1.0 else 0.0) + bau.obertonPegel +
            (if (bau.rumpler > 0) bau.rumplerPegel else 0.0) + bau.glanzPegel + (bau.pumpe?.get(1) ?: 0.0)

        while (laeuft == id && !Thread.currentThread().isInterrupted) {
            for (i in block.indices) {
                val t = n.toDouble() / RATE
                n++
                val f = frequenz(bau, t, tief, hoch)

                grund += 2 * PI * f / RATE
                var wert = welle(grund, bau.saege)
                if (bau.paarabstand > 0) {
                    paar += 2 * PI * (f + bau.paarabstand) / RATE
                    wert += welle(paar, bau.saege)
                }
                if (bau.oberton > 0) {
                    ober += 2 * PI * f * bau.oberton / RATE
                    wert += sin(ober) * bau.obertonPegel
                }
                if (bau.rumpler > 0) {
                    rumpel += 2 * PI * f * bau.rumpler / RATE
                    wert += welle(rumpel, true) * bau.rumplerPegel
                }
                if (bau.glanz > 0) {
                    glanz += 2 * PI * f * bau.glanz / RATE
                    wert += sin(glanz) * bau.glanzPegel
                }
                bau.pumpe?.let { p ->
                    motor += 2 * PI * p[0] / RATE
                    wert += welle(motor, true) * p[1]
                    // Das Takten der Pumpe liegt als Pulsieren auf dem Zweiklang.
                    wert *= 1 - p[3] * (0.5 + 0.5 * sin(2 * PI * p[2] * t))
                }
                // Weich einsetzen — ohne Knack beim Einschalten.
                val einsatz = minOf(1.0, t / 0.02)
                block[i] = (wert / summe * 0.55 * einsatz * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
            }
            if (grund > 1e6) grund %= 2 * PI
            val geschrieben = runCatching { spur.write(block, 0, block.size) }.getOrDefault(-1)
            if (geschrieben < 0) break
        }
    }

    /** Die Tonhöhe zur Zeit `t` — Folgeton mit Gleiten, oder der Jaulton. */
    private fun frequenz(bau: Hornbauart, t: Double, tief: Double, hoch: Double): Double {
        bau.jaulenBis?.let { bis ->
            // Auf und ab, ohne stillzustehen: ein Dreieck über den Durchlauf.
            val lage = (t / bau.jaulenPeriode) % 1.0
            val dreieck = if (lage < 0.5) lage * 2 else 2 - lage * 2
            return bau.grundton + (bis - bau.grundton) * dreieck
        }
        val lage = t % (2 * HALT)
        val (von, nach, seit) = if (lage < HALT) Triple(hoch, tief, lage) else Triple(tief, hoch, lage - HALT)
        // Am Anfang gibt es kein „von" — der erste Ton steht gleich.
        val f = if (t < HALT || seit >= bau.gleit) nach else von + (nach - von) * (seit / bau.gleit)
        // Das mechanische Aufgleiten: von viereinhalb Prozent darunter herauf.
        val anlauf = if (t < bau.anlauf) 0.955 + 0.045 * (t / bau.anlauf) else 1.0
        return f * anlauf
    }

    /** Rechteck weich gerundet (weniger Klirren auf kleinen Lautsprechern) oder Säge. */
    private fun welle(phase: Double, saege: Boolean): Double =
        if (saege) {
            // Eine Säge aus drei Teiltönen — die nackte Rampe klirrt am Handy.
            (sin(phase) + sin(2 * phase) / 2 + sin(3 * phase) / 3) * 0.6
        } else {
            tanh(sin(phase) * 3.5) * 0.8
        }
}
