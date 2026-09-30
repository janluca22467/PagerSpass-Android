package de.pagerspass.pagerspass.melder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Der Lautsprecher des Melders — `alarmStarten`/`alarmStoppen` aus `audio/sounds.ts`.
 *
 * <b>Ein Durchlauf, in Schleife.</b> Das Web plant alle halbe Sekunde die
 * nächsten zwei Sekunden Oszillatoren. Hier wird der Durchlauf einmal gerechnet
 * (`Meldertoene.abtasten`) und als statischer Puffer mit Endlosschleife in eine
 * `AudioTrack` gelegt — das Gerät spielt ihn dann ohne jede weitere Arbeit der
 * App, auch wenn der Hauptfaden gerade beschäftigt ist.
 *
 * <b>Auf dem Alarmkanal</b> (`USAGE_ALARM`): Er folgt der Alarm-Lautstärke des
 * Geräts, nicht der Medienlautstärke, die beim Spielen oft auf null steht.
 *
 * <b>Eine Probe unterbricht keinen Alarm</b> — dieselbe Regel wie im Web
 * (`Tonanlass`): Wer in der Tonwahl probehört, während der Melder geht, soll den
 * Alarm nicht abwürgen.
 */
object Melderspieler {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var spur: AudioTrack? = null
    private var spieler: MediaPlayer? = null
    private var klangschleife: Job? = null
    private var probeende: Job? = null
    private var vibrator: Vibrator? = null

    /** `alarm` oder `probe` — was gerade läuft, oder `null`. */
    var anlass: String? = null
        private set

    /** Zählt jedes Starten und Stoppen — ein Puffer, der zu spät fertig wird, spielt dann nicht mehr. */
    private var lauf = 0

    private var laufendeArt: String? = null
    private var laufendePrio = 0

    private val attribute: AudioAttributes
        get() = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    /**
     * Den Melder losgehen lassen.
     *
     * @param art Die Tonart; ohne Angabe der eingestellte Ton.
     * @param anlass `alarm` hält, bis gestoppt wird; `probe` endet von selbst.
     */
    fun starten(
        zusammenhang: Context,
        art: String = Meldergeraet.ton,
        prioritaet: Int = 1,
        anlass: String = "alarm",
    ): Boolean {
        if (anlass == "probe" && this.anlass == "alarm") return false
        if (this.anlass == anlass && laufendeArt == art && laufendePrio == prioritaet) return true

        stoppen()
        Meldergeraet.bereit(zusammenhang)
        val dieser = ++lauf
        this.anlass = anlass
        laufendeArt = art
        laufendePrio = prioritaet

        val lautstaerke = Meldergeraet.lautstaerke.let { if (anlass == "probe") it.coerceAtLeast(0.3f) else it }

        val klang = Meldergeraet.klang(art)
        if (klang != null) {
            klangSpielen(klang, lautstaerke)
            return true
        }

        bereich.launch {
            val puffer = withContext(Dispatchers.Default) {
                Meldertoene.abtasten(Meldertoene.zyklus(art, prioritaet, Meldergeraet.planVon(art)))
            }
            if (lauf != dieser) return@launch
            spur = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(attribute)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(Meldertoene.ABTASTRATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(puffer.size * 2)
                    .build()
                    .apply {
                        write(puffer, 0, puffer.size)
                        setLoopPoints(0, puffer.size, -1)
                        setVolume(lautstaerke)
                        play()
                    }
            }.getOrNull()
        }
        return true
    }

    /**
     * Ein eigener Klang: der Ausschnitt `von`–`bis`, dann die Ruhe, von vorn.
     *
     * Über `MediaPlayer` und nicht über die Audiospur: Die Datei kann MP3, AAC
     * oder OGG sein, und das Gerät decodiert sie selbst — die App müsste sonst
     * jede dieser Formen kennen.
     */
    private fun klangSpielen(klang: EigenerKlang, lautstaerke: Float) {
        val datei = Meldergeraet.klangdatei(klang.id) ?: return
        val p = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attribute)
                setDataSource(datei.absolutePath)
                prepare()
                setVolume(lautstaerke, lautstaerke)
            }
        }.getOrNull() ?: return
        spieler = p
        klangschleife = bereich.launch {
            val laenge = ((klang.bis - klang.von) * 1000).toLong().coerceAtLeast(100)
            val ruhe = (klang.pause * 1000).toLong()
            while (isActive) {
                runCatching {
                    p.seekTo((klang.von * 1000).toInt())
                    p.start()
                }
                delay(laenge)
                runCatching { p.pause() }
                delay(ruhe)
            }
        }
    }

    /** Eine Probe: zwei Durchläufe, mindestens gut eine Sekunde, höchstens sechs. */
    fun probe(zusammenhang: Context, art: String, prioritaet: Int = 1) {
        if (!starten(zusammenhang, art, prioritaet, "probe")) return
        val klang = Meldergeraet.klang(art)
        val dauer = if (klang != null) {
            ((klang.bis - klang.von) * 1000).toLong()
        } else {
            (Meldertoene.zyklus(art, prioritaet, Meldergeraet.planVon(art)).dauer * 2000).toLong()
        }.coerceIn(1_200, 6_000)
        probeende?.cancel()
        probeende = bereich.launch {
            delay(dauer)
            stoppen("probe")
        }
    }

    /** Anhalten — mit `nur` bloß dann, wenn gerade genau dieser Anlass läuft. */
    fun stoppen(nur: String? = null) {
        if (nur != null && anlass != nur) return
        lauf++
        probeende?.cancel()
        probeende = null
        klangschleife?.cancel()
        klangschleife = null
        spur?.let { s -> runCatching { s.stop() }; runCatching { s.release() } }
        spur = null
        spieler?.let { s -> runCatching { s.stop() }; runCatching { s.release() } }
        spieler = null
        anlass = null
        laufendeArt = null
        laufendePrio = 0
    }

    // ----------------------------------------------------------- Vibration

    /** Das Muster des Web (`vibrationAlarm`): lang, kurz, lang — und von vorn, bis quittiert ist. */
    fun vibrieren(zusammenhang: Context) {
        val v = vibrator ?: runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                zusammenhang.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                zusammenhang.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
        vibrator = v
        runCatching {
            v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 800, 900), 0), attribute)
        }
    }

    fun vibrationAus() {
        runCatching { vibrator?.cancel() }
    }
}
