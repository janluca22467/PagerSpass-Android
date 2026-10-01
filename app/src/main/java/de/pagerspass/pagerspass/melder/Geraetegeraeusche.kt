package de.pagerspass.pagerspass.melder

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Die kleinen Geräusche am Gerät — Tastenklick, Einschaltpiep, der Drucker des
 * Alarmfaxes und das Abreißen des Blatts.
 *
 * Übertragen aus `audio/sounds.ts` (`tastePiepen`, `faxDruckenStarten`,
 * `faxDruckenStoppen`, `faxAbreissenTon`). Das Web baut sie aus Oszillatoren,
 * Rauschpuffern und Bandpässen; hier wird dasselbe einmal in einen Puffer
 * gerechnet und als statische `AudioTrack` abgespielt — dieselbe Art wie der
 * Alarmton im [Melderspieler].
 *
 * <b>Nicht auf dem Alarmkanal.</b> Ein Tastenklick ist Rückmeldung, kein Alarm:
 * Er folgt der Lautstärke für Systemtöne und darf im Nachtdienst leise sein,
 * während der Alarm laut bleibt.
 *
 * <b>Sie schweigen mit dem Gerät.</b> Ein ausgeschalteter Melder und einer auf
 * „Stumm" oder „Nur Vibration" klickt nicht — ein Piepser, der stumm alarmiert,
 * aber bei jedem Tastendruck piept, verriete den Träger im Einsatz.
 */
object Geraetegeraeusche {

    private const val RATE = 22_050
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var druck: AudioTrack? = null
    private var druckLauf = 0

    private val attribute: AudioAttributes
        get() = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    /** Ob das Gerät gerade Töne von sich geben darf (`bedientoeneHoerbar`). */
    private fun hoerbar(): Boolean {
        if (Meldermenue.geraetAus) return false
        return Melderkatalog.alarmierungsart(Meldergeraet.alarmierungsart).ton && Meldergeraet.lautstaerke > 0f
    }

    private fun pegel(): Float = Meldergeraet.lautstaerke.coerceIn(0.15f, 1f)

    // --------------------------------------------------------------- Pieps

    /** Der Tastenklick der Piezoscheibe — kurz, hoch, trocken. */
    fun taste() {
        if (!hoerbar()) return
        einmal { ton(3100.0, 0.028, 0.35, rechteck = true) }
    }

    /**
     * Das Ende des Selbsttests nach dem Einschalten (`einschaltPiepen`): zwei
     * Töne, aufsteigend. Er kommt nach der Schaltzeile — vorher wäre er der
     * letzte Piep des ausgeschalteten Geräts und damit gar keiner.
     */
    fun einschalten() {
        if (!hoerbar()) return
        einmal { ton(2200.0, 0.07, 0.3, rechteck = true) + FloatArray((RATE * 0.05).toInt()) + ton(3300.0, 0.09, 0.3, rechteck = true) }
    }

    // ----------------------------------------------------------- Das Fax

    /**
     * Der Drucker läuft an: Motor, Nadeln und im Takt der Zeilen (170 ms) der
     * Papiervorschub — `faxDruckenStarten`.
     *
     * <b>Er folgt dem Druckzustand, nicht dem Alarm.</b> Wird die Meldung
     * während der Ausgabe anderswo quittiert, endet er mit der letzten Zeile;
     * wer ihn stoppt, ist die Anzeige, die die Zeilen zählt.
     */
    fun faxDruckenStarten() {
        if (druck != null || !hoerbar()) return
        val dieser = ++druckLauf
        val lautstaerke = pegel()
        bereich.launch {
            val puffer = withContext(Dispatchers.Default) { druckpuffer() }
            if (dieser != druckLauf) return@launch
            druck = spur(puffer, schleife = true, lautstaerke = lautstaerke)
        }
    }

    fun faxDruckenStoppen() {
        druckLauf++
        druck?.let { s -> runCatching { s.stop() }; runCatching { s.release() } }
        druck = null
    }

    /** Papier an der Zahnleiste: erst ein raues Reißen, dann der Schlag des gelösten Bogens. */
    fun faxAbreissen() {
        if (!hoerbar()) return
        einmal { abrisspuffer() }
    }

    // --------------------------------------------------------- Rechnen

    private fun einmal(bau: () -> FloatArray) {
        val lautstaerke = pegel()
        bereich.launch {
            val puffer = withContext(Dispatchers.Default) { bau() }
            val s = spur(puffer, schleife = false, lautstaerke = lautstaerke) ?: return@launch
            // Nach dem Durchlauf freigeben — eine statische Spur hält sonst ihren Speicher.
            kotlinx.coroutines.delay((puffer.size * 1000L / RATE) + 200)
            runCatching { s.release() }
        }
    }

    private fun spur(werte: FloatArray, schleife: Boolean, lautstaerke: Float): AudioTrack? {
        val pcm = ShortArray(werte.size) { (werte[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort() }
        if (pcm.isEmpty()) return null
        return runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(attribute)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
                .apply {
                    write(pcm, 0, pcm.size)
                    if (schleife) setLoopPoints(0, pcm.size, -1)
                    setVolume(lautstaerke)
                    play()
                }
        }.getOrNull()
    }

    /** Ein Ton mit kurzer Hüllkurve — Sinus oder Rechteck. */
    private fun ton(frequenz: Double, dauer: Double, staerke: Double, rechteck: Boolean = false): FloatArray {
        val n = (RATE * dauer).toInt()
        val flanke = (RATE * 0.004).toInt().coerceAtLeast(1)
        return FloatArray(n) { i ->
            val phase = sin(2 * PI * frequenz * i / RATE)
            val welle = if (rechteck) (if (phase >= 0) 0.6 else -0.6) else phase
            val huelle = minOf(1.0, i / flanke.toDouble(), (n - i) / flanke.toDouble())
            (welle * huelle * staerke).toFloat()
        }
    }

    /**
     * Zwei Zeilentakte Drucker: Motor (9 Hz Brummen), Nadeln (Rauschen in
     * Stößen), durch einen Bandpass bei 1450 Hz — und am Anfang jedes Takts
     * der Vorschub, ein Klick und ein tiefer Nachschlag.
     */
    private fun druckpuffer(): FloatArray {
        val takt = (RATE * 0.17).toInt()
        val n = takt * 2
        val zufall = Random(7)
        val roh = FloatArray(n) { i ->
            val position = (i % takt).toDouble() / takt
            val motor = sin(position * PI * 2 * 9) * 0.18
            val nadeln = (zufall.nextDouble() * 2 - 1) * (if (i % 41 < 7) 0.7 else 0.16)
            ((motor + nadeln) * 0.34).toFloat()
        }
        val gefiltert = bandpass(roh, 1450.0, 0.7)
        for (i in gefiltert.indices) gefiltert[i] *= 0.9f
        for (anfang in listOf(0, takt)) {
            val klick = ton(1180.0, 0.025, 0.16, rechteck = true)
            val nach = ton(430.0, 0.035, 0.14)
            klick.forEachIndexed { j, w -> if (anfang + j < n) gefiltert[anfang + j] += w }
            val versatz = anfang + (RATE * 0.026).toInt()
            nach.forEachIndexed { j, w -> if (versatz + j < n) gefiltert[versatz + j] += w }
        }
        return gefiltert
    }

    /** Das Reißen: Rauschen in Zacken, der Bandpass wandert von 2400 auf 950 Hz. */
    private fun abrisspuffer(): FloatArray {
        val dauer = 0.38
        val n = (RATE * dauer).toInt()
        val zufall = Random(11)
        val roh = FloatArray(n) { i ->
            val position = i.toDouble() / n
            val zacken = if (i % 67 < 19) 1.0 else 0.32
            val huelle = sin(PI * minOf(1.0, position * 1.15)) * (1 - position * 0.55)
            ((zufall.nextDouble() * 2 - 1) * zacken * huelle).toFloat()
        }
        // Der wandernde Filter in vier Stücken — genauer hört es niemand.
        val stueck = n / 4
        val aus = FloatArray(n)
        for (k in 0 until 4) {
            val von = k * stueck
            val bis = if (k == 3) n else von + stueck
            val f = 2400.0 - (2400.0 - 950.0) * k / 3.0
            val teil = bandpass(roh.copyOfRange(von, bis), f, 0.45)
            teil.copyInto(aus, von)
        }
        for (i in aus.indices) aus[i] *= 0.7f
        // Der Bogen löst sich hörbar von der Kante.
        val schlag = ton(190.0, 0.05, 0.3)
        val ab = n - (RATE * 0.055).toInt()
        schlag.forEachIndexed { j, w -> if (ab + j < n) aus[ab + j] += w }
        return aus
    }

    /** Ein Bandpass nach dem Kochbuch (RBJ) — dasselbe wie der `BiquadFilterNode` des Webs. */
    private fun bandpass(ein: FloatArray, frequenz: Double, q: Double): FloatArray {
        val w0 = 2 * PI * frequenz / RATE
        val alpha = sin(w0) / (2 * q)
        val a0 = 1 + alpha
        val b0 = alpha / a0
        val b2 = -alpha / a0
        val a1 = -2 * cos(w0) / a0
        val a2 = (1 - alpha) / a0
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
        return FloatArray(ein.size) { i ->
            val x = ein[i].toDouble()
            val y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            y.toFloat()
        }
    }
}
