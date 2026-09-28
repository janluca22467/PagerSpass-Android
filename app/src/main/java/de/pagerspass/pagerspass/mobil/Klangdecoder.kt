package de.pagerspass.pagerspass.mobil

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

/**
 * Eine Tondatei hörbar machen — das Gegenstück zu `ctx.decodeAudioData` im Web.
 *
 * <b>Zwei Wege, ein Ergebnis.</b> Eine WAV-Datei mit rohen Proben liest diese Datei
 * selbst: So liegen die eigenen Aufnahmen der Klangwerkstatt (die als WAV gesichert
 * werden) nicht an der Laune eines Gerätedecoders. Alles andere — MP3, OGG, M4A —
 * geht durch `MediaExtractor` und `MediaCodec`, also durch das, was dieses Gerät
 * lesen kann. Das ist dieselbe ehrliche Formatprüfung wie im Web: Ob eine Datei
 * lesbar ist, weiß nur das Gerät, und eine Liste erlaubter Endungen hätte je nach
 * Hersteller zu viel oder zu wenig durchgelassen.
 *
 * <b>Heraus kommt immer eine Spur.</b> Der Melder hat einen Lautsprecher; eine
 * Stereoaufnahme wird zusammengelegt statt eine Seite genommen, damit nichts
 * verschwindet, was nur rechts steht (`melderklangEinlesen` in sounds.ts).
 */
object Klangdecoder {

    /** Eine decodierte Spur: Proben zwischen −1 und 1, in ihrer eigenen Abtastrate. */
    class Spur(val proben: FloatArray, val rate: Int) {
        val laenge: Double get() = if (rate > 0) proben.size.toDouble() / rate else 0.0
    }

    /**
     * Mehr decodiert niemand — fünf Minuten sind weit jenseits dessen, was in drei
     * Megabyte passt, und eine Grenze, hinter der kein Gerät am Einstellen eines
     * Alarmtons in die Knie geht.
     */
    private const val HOECHSTSEKUNDEN = 300

    /** Die Datei lesen — oder `null`, wenn dieses Gerät sie nicht versteht. */
    fun lesen(bytes: ByteArray): Spur? =
        runCatching { wavLesen(bytes) }.getOrNull() ?: runCatching { mitCodec(bytes) }.getOrNull()

    /**
     * Auf eine andere Abtastrate bringen — gerade gerechnet, zwischen zwei Proben
     * verbunden. Für einen Alarmton reicht das; ein Tonstudio ist das nicht.
     */
    fun umrechnen(spur: Spur, ziel: Int): FloatArray {
        if (spur.rate == ziel || spur.rate <= 0) return spur.proben
        val quelle = spur.proben
        if (quelle.isEmpty()) return quelle
        val anzahl = max(1, (quelle.size.toLong() * ziel / spur.rate).toInt())
        val schritt = spur.rate.toDouble() / ziel
        return FloatArray(anzahl) { i ->
            val stelle = i * schritt
            val links = stelle.toInt().coerceAtMost(quelle.size - 1)
            val rechts = min(links + 1, quelle.size - 1)
            val anteil = (stelle - links).toFloat()
            quelle[links] + (quelle[rechts] - quelle[links]) * anteil
        }
    }

    // ------------------------------------------------------------------ WAV

    /**
     * Eine RIFF-WAV-Datei mit rohen Proben (8, 16, 24 oder 32 Bit, auch Fließkomma).
     * Alles, was keine ist, gibt `null` — dann versucht es der Gerätedecoder.
     */
    private fun wavLesen(bytes: ByteArray): Spur? {
        if (bytes.size < 44) return null
        if (String(bytes, 0, 4, Charsets.US_ASCII) != "RIFF") return null
        if (String(bytes, 8, 4, Charsets.US_ASCII) != "WAVE") return null

        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var stelle = 12
        var format = 0
        var kanaele = 0
        var rate = 0
        var bits = 0
        var datenAb = -1
        var datenLaenge = 0

        while (stelle + 8 <= bytes.size) {
            val name = String(bytes, stelle, 4, Charsets.US_ASCII)
            val groesse = b.getInt(stelle + 4)
            val inhalt = stelle + 8
            if (groesse < 0) return null
            when (name) {
                "fmt " -> {
                    if (inhalt + 16 > bytes.size) return null
                    format = b.getShort(inhalt).toInt() and 0xFFFF
                    kanaele = b.getShort(inhalt + 2).toInt() and 0xFFFF
                    rate = b.getInt(inhalt + 4)
                    bits = b.getShort(inhalt + 14).toInt() and 0xFFFF
                    // WAVE_FORMAT_EXTENSIBLE: Das eigentliche Format steht im Untertyp.
                    if (format == 0xFFFE && groesse >= 26 && inhalt + 26 <= bytes.size) {
                        format = b.getShort(inhalt + 24).toInt() and 0xFFFF
                    }
                }
                "data" -> {
                    datenAb = inhalt
                    datenLaenge = min(groesse, bytes.size - inhalt)
                }
            }
            if (datenAb >= 0 && format != 0) break
            stelle = inhalt + groesse + (groesse and 1)
        }

        if (datenAb < 0 || kanaele <= 0 || rate <= 0) return null
        val breite = bits / 8
        if (breite !in 1..4) return null
        val fliess = format == 3
        if (!fliess && format != 1) return null
        if (fliess && breite != 4) return null

        val rahmen = breite * kanaele
        val zahl = min(datenLaenge / rahmen, HOECHSTSEKUNDEN * rate)
        val proben = FloatArray(zahl)
        for (i in 0 until zahl) {
            var summe = 0f
            for (k in 0 until kanaele) {
                val p = datenAb + i * rahmen + k * breite
                summe += when {
                    fliess -> b.getFloat(p)
                    breite == 1 -> ((bytes[p].toInt() and 0xFF) - 128) / 128f
                    breite == 2 -> b.getShort(p) / 32768f
                    breite == 3 -> {
                        val wert = (bytes[p].toInt() and 0xFF) or
                            ((bytes[p + 1].toInt() and 0xFF) shl 8) or
                            (bytes[p + 2].toInt() shl 16)
                        wert / 8_388_608f
                    }
                    else -> b.getInt(p) / 2_147_483_648f
                }
            }
            proben[i] = summe / kanaele
        }
        return Spur(proben, rate)
    }

    /** Proben als WAV mit 16 Bit, eine Spur — so sichert die Klangwerkstatt ihre Aufnahmen. */
    fun wavSchreiben(proben: ShortArray, anzahl: Int, rate: Int): ByteArray {
        val daten = anzahl * 2
        val b = ByteBuffer.allocate(44 + daten).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray(Charsets.US_ASCII))
        b.putInt(36 + daten)
        b.put("WAVE".toByteArray(Charsets.US_ASCII))
        b.put("fmt ".toByteArray(Charsets.US_ASCII))
        b.putInt(16)
        b.putShort(1)
        b.putShort(1)
        b.putInt(rate)
        b.putInt(rate * 2)
        b.putShort(2)
        b.putShort(16)
        b.put("data".toByteArray(Charsets.US_ASCII))
        b.putInt(daten)
        for (i in 0 until anzahl) b.putShort(proben[i])
        return b.array()
    }

    // ------------------------------------------------------ Gerätedecoder

    /** Die Bytes als Quelle für den `MediaExtractor` — ohne Umweg über eine Datei. */
    private class Bytequelle(private val bytes: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= bytes.size) return -1
            val n = min(size.toLong(), bytes.size - position).toInt()
            System.arraycopy(bytes, position.toInt(), buffer, offset, n)
            return n
        }

        override fun getSize(): Long = bytes.size.toLong()

        override fun close() {}
    }

    /** Eine wachsende Reihe von Proben — ohne eine Liste aus Millionen Kästchen. */
    private class Sammler {
        var werte = FloatArray(1 shl 16)
        var anzahl = 0

        fun dazu(wert: Float) {
            if (anzahl == werte.size) werte = werte.copyOf(werte.size * 2)
            werte[anzahl++] = wert
        }

        fun fertig(): FloatArray = werte.copyOf(anzahl)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun mitCodec(bytes: ByteArray): Spur? {
        val auszug = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            auszug.setDataSource(Bytequelle(bytes))
            val spur = (0 until auszug.trackCount).firstOrNull {
                auszug.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            auszug.selectTrack(spur)
            val format = auszug.getTrackFormat(spur)
            val art = format.getString(MediaFormat.KEY_MIME) ?: return null
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var kanaele = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var fliess = false

            val werk = MediaCodec.createDecoderByType(art)
            codec = werk
            werk.configure(format, null, null, 0)
            werk.start()

            val info = MediaCodec.BufferInfo()
            val sammler = Sammler()
            var eingabeZu = false
            var fertig = false
            // Ein Decoder, der nichts mehr herausgibt, darf die Werkstatt nicht aufhängen.
            var leerlauf = 0

            while (!fertig && leerlauf < 500) {
                if (!eingabeZu) {
                    val i = werk.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val puffer = werk.getInputBuffer(i)
                        val n = if (puffer != null) auszug.readSampleData(puffer, 0) else -1
                        if (n < 0) {
                            werk.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eingabeZu = true
                        } else {
                            werk.queueInputBuffer(i, 0, n, auszug.sampleTime, 0)
                            auszug.advance()
                        }
                    }
                }

                val o = werk.dequeueOutputBuffer(info, 10_000)
                when {
                    o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val neu = werk.outputFormat
                        rate = neu.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        kanaele = neu.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        fliess = neu.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            neu.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                        leerlauf = 0
                    }
                    o >= 0 -> {
                        leerlauf = 0
                        val puffer = werk.getOutputBuffer(o)
                        if (puffer != null && info.size > 0 && kanaele > 0) {
                            puffer.position(info.offset)
                            puffer.limit(info.offset + info.size)
                            val roh = puffer.slice().order(ByteOrder.nativeOrder())
                            if (fliess) {
                                val f = roh.asFloatBuffer()
                                val rahmen = f.remaining() / kanaele
                                for (r in 0 until rahmen) {
                                    var summe = 0f
                                    for (k in 0 until kanaele) summe += f.get(r * kanaele + k)
                                    sammler.dazu(summe / kanaele)
                                }
                            } else {
                                val s = roh.asShortBuffer()
                                val rahmen = s.remaining() / kanaele
                                for (r in 0 until rahmen) {
                                    var summe = 0f
                                    for (k in 0 until kanaele) summe += s.get(r * kanaele + k) / 32768f
                                    sammler.dazu(summe / kanaele)
                                }
                            }
                        }
                        werk.releaseOutputBuffer(o, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) fertig = true
                        if (rate > 0 && sammler.anzahl > HOECHSTSEKUNDEN * rate) fertig = true
                    }
                    else -> leerlauf += 1
                }
            }

            if (sammler.anzahl == 0 || rate <= 0) return null
            return Spur(sammler.fertig(), rate)
        } finally {
            codec?.let { c ->
                runCatching { c.stop() }
                runCatching { c.release() }
            }
            runCatching { auszug.release() }
        }
    }
}

/**
 * Das Mikrofon der Klangwerkstatt — `MediaRecorder` im Web, hier `AudioRecord`.
 *
 * <b>Roh aufgenommen, als WAV gesichert.</b> Das Web nimmt, was der Browser gibt
 * (meist WebM); hier gibt es keinen Grund für einen Umweg über einen Codierer:
 * Dreißig Sekunden in 16 Bit sind 2,6 Megabyte und passen unter die drei, die eine
 * Datei höchstens haben darf.
 *
 * <b>Ein eigener Faden</b> wie beim Sprechfunk — `AudioRecord.read` blockiert.
 */
class Klangaufnahme {

    private var geraet: AudioRecord? = null
    private var faden: Thread? = null
    private val laeuft = AtomicBoolean(false)

    private val proben = ShortArray(Eigentoene.AUFNAHME_HOECHSTDAUER * RATE)

    @Volatile
    private var anzahl = 0

    /** Das Mikrofon öffnen — `false`, wenn es nicht zu bekommen war. */
    @SuppressLint("MissingPermission") // Die Werkstatt fragt vor dem Druck.
    fun starten(): Boolean {
        if (laeuft.get()) return true
        anzahl = 0

        val mindest = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (mindest <= 0) return false

        val aufnahme = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(mindest, RATE / 5 * 2),
            )
        }.getOrNull() ?: return false

        if (aufnahme.state != AudioRecord.STATE_INITIALIZED) {
            aufnahme.release()
            return false
        }

        geraet = aufnahme
        laeuft.set(true)
        runCatching { aufnahme.startRecording() }.onFailure {
            laeuft.set(false)
            aufnahme.release()
            geraet = null
            return false
        }

        faden = Thread {
            val stueck = ShortArray(RATE / 10)
            while (laeuft.get()) {
                val n = aufnahme.read(stueck, 0, stueck.size)
                if (n <= 0) continue
                val platz = min(n, proben.size - anzahl)
                if (platz > 0) {
                    System.arraycopy(stueck, 0, proben, anzahl, platz)
                    anzahl += platz
                }
            }
        }.apply {
            name = "Klangwerkstatt-Mikrofon"
            start()
        }
        return true
    }

    /** Aufhören und die Aufnahme als WAV zurückgeben — `null`, wenn nichts darin ist. */
    fun beenden(): ByteArray? {
        schliessen()
        val n = anzahl
        if (n < RATE / 10) return null
        return Klangdecoder.wavSchreiben(proben, n, RATE)
    }

    /** Aufhören und nichts behalten. */
    fun verwerfen() {
        schliessen()
        anzahl = 0
    }

    /** Wie viele Sekunden schon aufgenommen sind. */
    val sekunden: Double get() = anzahl.toDouble() / RATE

    private fun schliessen() {
        laeuft.set(false)
        faden?.join(500)
        faden = null
        geraet?.let { g ->
            runCatching { g.stop() }
            runCatching { g.release() }
        }
        geraet = null
    }

    private companion object {
        const val RATE = 44_100
    }
}
