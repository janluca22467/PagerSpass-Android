package de.pagerspass.pagerspass.mobil

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.ui.schmuck.Melderkatalog
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Der Bauplan eines selbst gebauten Meldertons — `Tonbauplan` in `web/src/audio/sounds.ts`.
 *
 * <b>Was er bewusst nicht kann.</b> Keine Lautstärke, keine Zahl der Wiederholungen,
 * keine Zweitstimme. Der Pegel kommt aus derselben Rechnung wie bei jedem anderen Ton
 * ([Meldertonprobe]) — ein Baukasten, mit dem man sich einen lauteren Melder baut,
 * wäre der erste Kaufvorteil im Einsatz.
 *
 * @param schritte Das Raster: sechzehn gleich lange Schritte, jeder eine Tonhöhe aus
 *   [Eigentoene.LEITER] oder eine Pause (`null`).
 * @param abstand Wie lang ein Schritt dauert, in Sekunden.
 * @param dauer Wie lang ein Ton innerhalb seines Schritts klingt.
 * @param form Die Wellenform: `sine`, `triangle`, `square` oder `sawtooth`.
 */
@Serializable
data class Tonbauplan(
    val schritte: List<Int?> = emptyList(),
    val abstand: Double = 0.16,
    val dauer: Double = 0.12,
    val form: String = "square",
)

/** Ein gesicherter eigener Ton — Kennung, Name, Bauplan. Er gehört dem Gerät, nicht dem Konto. */
@Serializable
data class EigenerTon(
    val id: String = "",
    val name: String = "",
    val plan: Tonbauplan = Tonbauplan(),
)

/**
 * Ein eigener Klang, wie ihn die Oberfläche und die Tonwahl sehen — alles außer den
 * Bytes (`EigenerKlang` in `audio/melderklaenge.ts`). Die Bytes liegen daneben als
 * Datei; diese Sätze werden bei jeder Änderung neu gezeichnet, und drei Megabyte in
 * einem beobachteten Zustand wären ein Fehler, den man erst am ruckelnden Regal bemerkt.
 */
@Serializable
data class EigenerKlang(
    val id: String = "",
    val name: String = "",
    /** `datei` oder `aufnahme` — eine Aufnahme darf man neu einsprechen, eine Datei nicht. */
    val herkunft: String = "datei",
    /** Der Inhaltstyp, wie das Gerät ihn gemeldet hat — nur zur Anzeige. */
    val typ: String = "audio/*",
    val groesse: Long = 0,
    /** Die volle Länge der Quelle in Sekunden. */
    val laenge: Double = 0.0,
    val von: Double = 0.0,
    val bis: Double = 0.0,
    /** Die Ruhe zwischen zwei Durchläufen. */
    val pause: Double = 0.6,
    val angelegt: Long = 0,
)

/**
 * Die eigenen Alarmtöne dieses Geräts — die gezeichneten der Tonwerkstatt und die
 * eingespielten der Klangwerkstatt.
 *
 * Übertragen aus dem Teil von `web/src/stores/melder.ts`, der sie hält
 * (`tonbauplan`, `eigeneToene`, `eigeneKlaenge`), aus den Bauplan-Rechnungen in
 * `audio/sounds.ts` (Leiter, Takte, Code) und aus `audio/melderklaenge.ts`.
 *
 * <b>Warum sie am Gerät liegen und nicht am Konto.</b> Dieselbe Linie wie bei Bauform
 * und Alarmton: Wer nachts am Handy einen ruhigen Ton will und am Rechner den lauten,
 * darf das. Und eine Mikrofonaufnahme ist das Persönlichste, was dieses Spiel anfassen
 * könnte — <b>sie verlässt das Gerät nie.</b> Es gibt keine Ablage am Server, keinen
 * Aufruf, der sie hochlädt; weitergegeben wird nur der gezeichnete Ton, und der als
 * Code aus sechzehn Zeichen.
 *
 * <b>Ein Objekt je Prozess</b>, wie die Modulwerte im Web: Der Alarm wird an mehreren
 * Stellen ausgelöst ([Melderwerk], [Tonprobe]), und jede müsste die Liste sonst kennen
 * und durchreichen. [Meldertonprobe] schlägt hier nach, wenn eine Tonart mit `eigen:`
 * oder `klang:` beginnt.
 *
 * <b>Die Bauplan-Liste liegt in `SharedPreferences`</b> (wie der Tonregler), weil sie
 * beim ersten Alarm schon dastehen muss. Die Klänge liegen als Dateien im
 * App-Speicher — der Gegenpart zur IndexedDB des Webs.
 */
object Eigentoene {

    // ------------------------------------------------------------ Die Grenzen

    /** Wie breit das Raster ist — sechzehn Schritte, zwei Takte. */
    const val SCHRITTE = 16

    /** Nach wie vielen Schritten das Raster eine Trennlinie zieht. */
    const val GRUPPE = 4

    /** Wie viele eigene Töne ein Gerät behält. */
    const val HOECHSTZAHL = 12

    /** So lang darf ein Name sein — beim gebauten Ton wie beim Klang. */
    const val NAME_LAENGE = 24

    /** Wie lang ein eingespielter Klang höchstens klingt, in Sekunden. */
    const val KLANG_HOECHSTDAUER = 12.0

    /** Wie lange ein Klang höchstens ruht, bevor er von vorn anfängt. */
    const val KLANG_HOECHSTPAUSE = 4.0

    /** Wie viele eigene Klänge ein Gerät hält. */
    const val KLANG_HOECHSTZAHL = 8

    /** Wie groß eine Datei sein darf — die Grenze ist das Decodieren, nicht der Speicher. */
    const val KLANG_HOECHSTBYTES = 3 * 1024 * 1024

    /** Wie lange man am Stück einsprechen darf — mehr als am Ende klingt, zum Schneiden. */
    const val AUFNAHME_HOECHSTDAUER = 30

    /** Eine Höhe der Leiter. */
    class Stufe(val name: String, val hertz: Int)

    /**
     * Die Tonleiter des Baukastens — feste Höhen. <b>Angehängt wird, nie
     * eingeschoben:</b> Der Code trägt je Schritt die <em>Stelle</em> in dieser Liste.
     */
    val LEITER = listOf(
        Stufe("Tief", 392),
        Stufe("Warm", 523),
        Stufe("Mittel", 659),
        Stufe("Klar", 880),
        Stufe("Hell", 1046),
        Stufe("Spitz", 1318),
        Stufe("Schrill", 1568),
        Stufe("Sehr hoch", 1976),
        // Die analogen Höhen — die Fünftonfolge nach ZVEI, gemessen und nicht gewählt.
        Stufe("Analog 1", 1060),
        Stufe("Analog 3", 1270),
        Stufe("Analog 4", 1400),
        Stufe("Analog 5", 1530),
        Stufe("Analog 6", 1670),
        Stufe("Analog 7", 1830),
        Stufe("Analog 9", 2200),
    )

    class Takt(val name: String, val abstand: Double, val dauer: Double)

    /** Die drei Takte — der Code trägt die Stufe (0–2), nicht die Sekundenzahl. */
    val TAKTE = listOf(
        Takt("Langsam", 0.26, 0.2),
        Takt("Mittel", 0.16, 0.12),
        Takt("Schnell", 0.09, 0.06),
    )

    class Farbe(val name: String, val form: String, val was: String)

    /** Die vier Klangfarben — aus demselben Grund hier wie die Takte. */
    val FARBEN = listOf(
        Farbe("Weich", "sine", "Sinus — voll und rund, wie ein Gong"),
        Farbe("Klar", "triangle", "Dreieck — hell, ohne zu stechen"),
        Farbe("Hart", "square", "Rechteck — der Ton des Dienstmelders"),
        Farbe("Scharf", "sawtooth", "Sägezahn — durchdringend wie ein Warnton"),
    )

    /** Womit der Baukasten anfängt: der Zweiklang, im Raster gezeichnet. */
    val STANDARD = Tonbauplan(
        schritte = listOf<Int?>(880, 1318) + List(SCHRITTE - 2) { null },
        abstand = 0.16,
        dauer = 0.12,
        form = "square",
    )

    /** Der Ton, auf den zurückgesprungen wird — der, mit dem jedes Konto anfängt. */
    const val TON_STANDARD = "zweiklang"

    // --------------------------------------------------------------- Zustand

    /** Der Entwurf auf der Werkbank der Tonwerkstatt — Tonart `eigen`. */
    var entwurf by mutableStateOf(STANDARD)
        private set

    /** Die gesicherten Töne — Tonart `eigen:<Kennung>`. */
    var toene by mutableStateOf<List<EigenerTon>>(emptyList())
        private set

    /** Die eingespielten Klänge — Tonart `klang:<Kennung>`. */
    var klaenge by mutableStateOf<List<EigenerKlang>>(emptyList())
        private set

    /** Ob die Ablage schon geantwortet hat — die Oberfläche unterscheidet „leer" von „lädt". */
    var klaengeGelesen by mutableStateOf(false)
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var zusammenhang: Context? = null
    private var speicher: SharedPreferences? = null

    /**
     * Einmal je Prozess vom Gerät holen — gerufen von jeder Stelle, die einen Ton
     * spielen kann. Die Klänge werden danach im Hintergrund decodiert; bis dahin
     * fällt ein eingestellter Klang auf den Zweiklang zurück (lieber der Standardton
     * als Stille, wie im Web).
     */
    fun sicherstellen(kontext: Context) {
        if (zusammenhang != null) return
        synchronized(this) {
            if (zusammenhang != null) return
            val app = kontext.applicationContext
            val ablage = app.getSharedPreferences("pagerspass-eigentoene", Context.MODE_PRIVATE)
            speicher = ablage
            entwurf = runCatching {
                ablage.getString(S_ENTWURF, null)?.let { pruefen(json.decodeFromString(Tonbauplan.serializer(), it)) }
            }.getOrNull() ?: STANDARD
            toene = runCatching {
                ablage.getString(S_TOENE, null)?.let { json.decodeFromString(ListSerializer(EigenerTon.serializer()), it) }
            }.getOrNull().orEmpty()
                .filter { it.id.isNotEmpty() }
                .take(HOECHSTZAHL)
                .map { it.copy(name = namenKuerzen(it.name, "Eigener Ton"), plan = pruefen(it.plan)) }
            klaenge = runCatching {
                ablage.getString(S_KLAENGE, null)?.let { json.decodeFromString(ListSerializer(EigenerKlang.serializer()), it) }
            }.getOrNull().orEmpty()
                .mapNotNull(::satzPruefen)
                .sortedBy { it.angelegt }
                .take(KLANG_HOECHSTZAHL)
            klaengeGelesen = true
            zusammenhang = app
        }
        bereich.launch { klaenge.forEach { bereitlegen(it) } }
    }

    private fun ablageVon(): Ablage? = zusammenhang?.let { Ablage(it) }

    /** Den eingestellten Alarmton zurücksetzen, wenn er gerade `art` ist. */
    private fun abwaehlen(art: String) {
        val ablage = ablageVon() ?: return
        bereich.launch {
            if (ablage.melderTonFluss().first() == art) ablage.melderTonSetzen(TON_STANDARD)
        }
    }

    // ------------------------------------------------------- Der Bauplan

    /**
     * Einen Bauplan in die Form bringen, in der er klingen darf — die verbindliche
     * Prüfung (`tonbauplanPruefen`). Ein Plan ohne Ton wäre ein stummer Melder.
     */
    fun pruefen(plan: Tonbauplan): Tonbauplan {
        val hoehen = LEITER.map { it.hertz }
        val schritte = List(SCHRITTE) { i -> plan.schritte.getOrNull(i)?.takeIf { it in hoehen } }
        fun grenze(wert: Double, von: Double, bis: Double, ersatz: Double) =
            if (wert.isFinite()) wert.coerceIn(von, bis) else ersatz
        return Tonbauplan(
            schritte = if (schritte.any { it != null }) schritte else STANDARD.schritte,
            abstand = grenze(plan.abstand, 0.06, 0.6, STANDARD.abstand),
            dauer = grenze(plan.dauer, 0.03, 0.45, STANDARD.dauer),
            form = plan.form.takeIf { f -> FARBEN.any { it.form == f } } ?: STANDARD.form,
        )
    }

    /**
     * Wie viele Schritte ein Durchlauf wirklich dauert — bis zum Ende des Viertels, in
     * dem der letzte Ton steht (`tonbauLaenge`).
     */
    fun laenge(schritte: List<Int?>): Int {
        val letzter = schritte.indexOfLast { it != null }
        if (letzter < 0) return schritte.size
        return min(schritte.size, (letzter / GRUPPE + 1) * GRUPPE)
    }

    /**
     * Welchen Bauplan eine Tonart meint. Der Rückfall auf den Entwurf ist Absicht: Wer
     * einen Ton löscht, den er eingestellt hatte, soll beim nächsten Alarm etwas hören.
     */
    fun planZuArt(art: String): Tonbauplan {
        if (art == "eigen") return pruefen(entwurf)
        val id = art.removePrefix("eigen:")
        return toene.firstOrNull { it.id == id }?.plan ?: pruefen(entwurf)
    }

    /**
     * Den Entwurf auf der Werkbank setzen — er wird sofort gemerkt.
     *
     * Er steht hier so, wie er gezeichnet ist, auch leer: Wer das Raster leert, soll
     * ein leeres Raster sehen. Gespielt und gemerkt wird dagegen der geprüfte Plan —
     * ein leeres Raster klänge nach nichts, und ein stummer Melder ist der eine Zustand,
     * den der Baukasten nicht herstellen darf.
     */
    fun entwurfSetzen(plan: Tonbauplan) {
        val hoehen = LEITER.map { it.hertz }
        entwurf = plan.copy(schritte = List(SCHRITTE) { i -> plan.schritte.getOrNull(i)?.takeIf { it in hoehen } })
        speicher?.edit()?.putString(S_ENTWURF, json.encodeToString(Tonbauplan.serializer(), pruefen(plan)))?.apply()
    }

    /**
     * Einen Ton sichern — neu anlegen oder einen bestehenden überschreiben. Gibt die
     * Kennung zurück, `null` heißt: Das Regal ist voll.
     */
    fun tonSichern(name: String, plan: Tonbauplan, id: String? = null): String? {
        val geprueft = pruefen(plan)
        val stelle = if (id != null) toene.indexOfFirst { it.id == id } else -1

        if (id != null && stelle >= 0) {
            toene = toene.toMutableList().also { it[stelle] = EigenerTon(id, namenKuerzen(name, "Eigener Ton"), geprueft) }
            toeneMerken()
            return id
        }

        // Voll heißt voll — hier und nicht erst in der Oberfläche.
        if (toene.size >= HOECHSTZAHL) return null

        val neu = "t" + neueKennung()
        toene = toene + EigenerTon(neu, namenKuerzen(name, "Eigener Ton"), geprueft)
        toeneMerken()
        return neu
    }

    /**
     * Einen gesicherten Ton wegwerfen. Stand er als Alarmton eingestellt, springt die
     * Wahl auf den Standardton zurück.
     */
    fun tonLoeschen(id: String) {
        toene = toene.filter { it.id != id }
        toeneMerken()
        abwaehlen("eigen:$id")
    }

    private fun toeneMerken() {
        speicher?.edit()?.putString(S_TOENE, json.encodeToString(ListSerializer(EigenerTon.serializer()), toene))?.apply()
    }

    // --------------------------------------------------- Der Code zum Weitergeben

    /** Crockford-Base32 ohne I, L, O und U — der Code wird abgeschrieben und vorgelesen. */
    private const val CODE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val CODE_LAENGE = 16
    private const val CODE_FASSUNG = 1

    /**
     * Der Bauplan als Code: vier Bit Fassung, zwei Bit Klangfarbe, zwei Bit Takt,
     * sechzehn Schritte zu je vier Bit, acht Bit Prüfsumme — achtzig Bit, sechzehn Zeichen.
     */
    fun codeVon(plan: Tonbauplan): String {
        val geprueft = pruefen(plan)
        val hoehen = LEITER.map { it.hertz }
        val farbe = max(0, FARBEN.indexOfFirst { it.form == geprueft.form })
        var takt = 0
        TAKTE.forEachIndexed { i, t ->
            if (abs(t.abstand - geprueft.abstand) < abs(TAKTE[takt].abstand - geprueft.abstand)) takt = i
        }
        val stufen = geprueft.schritte.map { s -> if (s == null) 0 else hoehen.indexOf(s) + 1 }

        val bytes = mutableListOf((CODE_FASSUNG shl 4) or (farbe shl 2) or takt)
        for (i in 0 until SCHRITTE step 2) bytes.add((stufen[i] shl 4) or stufen[i + 1])
        bytes.add(pruefsumme(bytes))
        return zuBase32(bytes)
    }

    /** Einen Code zurück in einen Bauplan — oder `null`, wenn er keiner ist. */
    fun codeLesen(code: String): Tonbauplan? {
        val sauber = code.uppercase()
            .replace(Regex("[IL]"), "1")
            .replace("O", "0")
            .replace(Regex("[^0-9A-Z]"), "")
        if (sauber.length != CODE_LAENGE) return null

        val bytes = ausBase32(sauber) ?: return null
        if (bytes.size < 2) return null
        val nutz = bytes.dropLast(1)
        if (bytes.last() != pruefsumme(nutz)) return null

        val kopf = nutz[0]
        // Eine künftige Fassung wird abgewiesen statt halb gelesen.
        if (kopf shr 4 != CODE_FASSUNG) return null

        val stufen = nutz.drop(1).flatMap { listOf(it shr 4, it and 0xF) }
        val hoehen = LEITER.map { it.hertz }
        val takt = TAKTE.getOrNull(kopf and 0b11) ?: TAKTE[1]

        return pruefen(
            Tonbauplan(
                schritte = stufen.map { n -> if (n == 0) null else hoehen.getOrNull(n - 1) },
                abstand = takt.abstand,
                dauer = takt.dauer,
                form = FARBEN.getOrNull((kopf shr 2) and 0b11)?.form ?: STANDARD.form,
            ),
        )
    }

    /** Erst drehen, dann verrechnen — sonst ergäben zwei vertauschte Bytes dieselbe Summe. */
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
        }
        return aus.toString()
    }

    private fun ausBase32(text: String): List<Int>? {
        var bits = 0
        var wert = 0
        val aus = mutableListOf<Int>()
        for (zeichen in text) {
            val stelle = CODE_ALPHABET.indexOf(zeichen)
            if (stelle < 0) return null
            wert = (wert shl 5) or stelle
            bits += 5
            if (bits >= 8) {
                aus.add((wert shr (bits - 8)) and 0xff)
                bits -= 8
            }
        }
        return aus
    }

    // ------------------------------------------------ Die eingespielten Klänge

    /** Ein eingelesener Klang, wie der Mischer ihn braucht — auf die Rate der Töne gebracht. */
    class Klangstueck(
        val proben: FloatArray,
        /** Der höchste Ausschlag, 0 bis 1. */
        val spitze: Double,
        /** Der Effektivwert, also die gehörte Lautstärke. */
        val effektiv: Double,
        val pause: Double,
    )

    /** Was eine Datei hergibt, ohne sie zu behalten — Länge und Umrisslinie. */
    class Vermessung(val laenge: Double, val umriss: List<Float>)

    /** Nur was hier steht, ist hörbar — alles andere liegt als Datei und muss erst decodiert werden. */
    private val stuecke = ConcurrentHashMap<String, Klangstueck>()

    /** Klänge, die gerade (neu) eingelesen werden — ein neuer Schnitt bricht den alten ab. */
    private val inArbeit = ConcurrentHashMap<String, Job>()

    /**
     * Die zuletzt decodierte Datei — damit das Ziehen an den Schnittmarken nicht bei
     * jedem Schritt drei Megabyte durch den Decoder schickt.
     */
    @Volatile
    private var zuletzt: Pair<String, FloatArray>? = null

    fun klangstueck(id: String): Klangstueck? = stuecke[id]

    fun klangDa(id: String): Boolean = stuecke.containsKey(id)

    /**
     * Den Pegel eines Klangs gegen die gerechneten Töne abgleichen: der Effektivwert
     * auf den Pegel des Katalogtons, gedeckelt am Ausschlag. Ein eingespielter Ton ist
     * so laut wie ein gebauter — nicht lauter.
     */
    fun klangPegel(stueck: Klangstueck, pegel: Double): Double {
        if (stueck.spitze <= 0.0 || stueck.effektiv <= 0.0) return 0.0
        return min(0.95 / stueck.spitze, pegel / stueck.effektiv)
    }

    private fun ordner(): File? = zusammenhang?.let { File(it.filesDir, "melderklaenge").apply { mkdirs() } }

    /** Die Bytes eines Klangs — für das Einlesen und für die Kurve in der Klangwerkstatt. */
    fun klangBytes(id: String): ByteArray? = runCatching { ordner()?.let { File(it, id).readBytes() } }.getOrNull()

    /** Die ganze Datei als Proben in der Rate der Töne — aus dem Zwischenspeicher, wenn sie es gerade war. */
    private fun vollspur(id: String, bytes: ByteArray? = null): FloatArray? {
        zuletzt?.let { (kennung, proben) -> if (kennung == id) return proben }
        val roh = bytes ?: klangBytes(id) ?: return null
        val spur = Klangdecoder.lesen(roh) ?: return null
        val proben = Klangdecoder.umrechnen(spur, Meldertonprobe.RATE)
        zuletzt = id to proben
        return proben
    }

    /**
     * Eine Datei ansehen, ohne sie zu behalten — und zugleich die einzige ehrliche
     * Formatprüfung: `null` heißt, dieses Gerät kann sie nicht lesen.
     */
    suspend fun vermessen(bytes: ByteArray, balken: Int = 120): Vermessung? = withContext(Dispatchers.Default) {
        val spur = Klangdecoder.lesen(bytes) ?: return@withContext null
        Vermessung(spur.laenge, umriss(spur.proben, balken))
    }

    /** Die Umrisslinie eines gesicherten Klangs. */
    suspend fun umrissVon(id: String, balken: Int = 120): List<Float>? = withContext(Dispatchers.Default) {
        vollspur(id)?.let { umriss(it, balken) }
    }

    private fun umriss(daten: FloatArray, balken: Int): List<Float> {
        val jeBalken = max(1, daten.size / balken)
        return List(balken) { b ->
            var spitze = 0f
            val start = b * jeBalken
            for (i in start until min(daten.size, start + jeBalken)) {
                val wert = abs(daten[i])
                if (wert > spitze) spitze = wert
            }
            spitze
        }
    }

    /**
     * Den Schnitt eines Klangs anwenden: ausschneiden, Ränder weich machen (zwanzig
     * Millisekunden auf und ab — ein Sprung im Signal klingt als Knacken), vermessen.
     */
    private fun einlesen(satz: EigenerKlang): Boolean {
        val voll = vollspur(satz.id) ?: return false
        val rate = Meldertonprobe.RATE
        val gesamt = voll.size.toDouble() / rate
        val von = satz.von.coerceIn(0.0, gesamt)
        val bis = min(gesamt, max(von + 0.05, satz.bis))
        val laenge = min(KLANG_HOECHSTDAUER, bis - von)
        val ab = (von * rate).toInt()
        val zahl = max(1, (laenge * rate).toInt())

        val heraus = FloatArray(zahl) { i -> voll.getOrElse(ab + i) { 0f } }
        val flanke = min(zahl / 2, (0.02 * rate).toInt())
        for (i in 0 until flanke) {
            val f = i.toFloat() / flanke
            heraus[i] *= f
            heraus[zahl - 1 - i] *= f
        }

        var spitze = 0.0
        var summe = 0.0
        for (w in heraus) {
            val a = abs(w).toDouble()
            if (a > spitze) spitze = a
            summe += w.toDouble() * w
        }

        stuecke[satz.id] = Klangstueck(
            proben = heraus,
            spitze = spitze,
            effektiv = sqrt(summe / zahl),
            pause = satz.pause.coerceIn(0.0, KLANG_HOECHSTPAUSE),
        )
        return true
    }

    private suspend fun bereitlegen(satz: EigenerKlang): Boolean = withContext(Dispatchers.Default) {
        runCatching { einlesen(satz) }.getOrDefault(false)
    }

    /** Neu einlesen, ohne zu warten — ein neuerer Schnitt löst den älteren ab. */
    private fun nachlegen(satz: EigenerKlang) {
        inArbeit.remove(satz.id)?.cancel()
        inArbeit[satz.id] = bereich.launch { bereitlegen(satz) }
    }

    private fun namenKuerzen(name: String, ersatz: String): String {
        val sauber = name.replace(Regex("\\s+"), " ").trim()
        return sauber.ifEmpty { ersatz }.take(NAME_LAENGE)
    }

    /** Einen Satz gesund machen — was verbogen ist, darf höchstens sich selbst kaputt machen. */
    private fun satzPruefen(s: EigenerKlang): EigenerKlang? {
        if (s.id.isEmpty()) return null
        val laenge = if (s.laenge > 0) s.laenge else KLANG_HOECHSTDAUER
        val von = s.von.coerceIn(0.0, laenge)
        val bis = max(von + 0.05, min(laenge, if (s.bis > 0) s.bis else laenge))
        return s.copy(
            name = namenKuerzen(s.name, "Eigener Klang"),
            herkunft = if (s.herkunft == "aufnahme") "aufnahme" else "datei",
            laenge = laenge,
            von = von,
            bis = min(bis, von + KLANG_HOECHSTDAUER),
            pause = s.pause.coerceIn(0.0, KLANG_HOECHSTPAUSE),
        )
    }

    private fun klaengeMerken() {
        speicher?.edit()?.putString(S_KLAENGE, json.encodeToString(ListSerializer(EigenerKlang.serializer()), klaenge))?.apply()
    }

    /**
     * Einen Klang anlegen. Der Deckel wird hier geprüft und nicht in der Oberfläche.
     * Gibt den fertigen Satz zurück — oder `null`, wenn das Regal voll, die Datei zu
     * groß oder der Speicher nicht mitspielt.
     */
    suspend fun klangAnlegen(
        name: String,
        bytes: ByteArray,
        typ: String,
        herkunft: String,
        laenge: Double,
    ): EigenerKlang? {
        if (bytes.size > KLANG_HOECHSTBYTES) return null
        if (klaenge.size >= KLANG_HOECHSTZAHL) return null
        val ort = ordner() ?: return null

        val satz = EigenerKlang(
            id = "k" + neueKennung(),
            name = namenKuerzen(name, if (herkunft == "aufnahme") "Eingesprochen" else "Eigene Datei"),
            herkunft = if (herkunft == "aufnahme") "aufnahme" else "datei",
            typ = typ,
            groesse = bytes.size.toLong(),
            laenge = laenge,
            von = 0.0,
            // Was länger ist als der Deckel, steht beim Anlegen schon geschnitten da.
            bis = min(laenge, KLANG_HOECHSTDAUER),
            pause = 0.6,
            angelegt = System.currentTimeMillis(),
        )

        val geschrieben = withContext(Dispatchers.IO) {
            runCatching { File(ort, satz.id).writeBytes(bytes) }.isSuccess
        }
        if (!geschrieben) return null

        withContext(Dispatchers.Default) { runCatching { vollspur(satz.id, bytes) } }
        klaenge = klaenge + satz
        klaengeMerken()
        bereitlegen(satz)
        return satz
    }

    /** Namen, Schnitt oder Ruhe ändern — die Bytes bleiben, wo sie sind. */
    fun klangAendern(
        id: String,
        name: String? = null,
        von: Double? = null,
        bis: Double? = null,
        pause: Double? = null,
    ) {
        val alt = klaenge.firstOrNull { it.id == id } ?: return
        val neu = satzPruefen(
            alt.copy(
                name = name ?: alt.name,
                von = von ?: alt.von,
                bis = bis ?: alt.bis,
                pause = pause ?: alt.pause,
            ),
        ) ?: return
        klaenge = klaenge.map { if (it.id == id) neu else it }
        klaengeMerken()
        // Der Schnitt sitzt im eingelesenen Stück — wer ihn verschiebt, muss neu einlesen.
        if (neu.von != alt.von || neu.bis != alt.bis || neu.pause != alt.pause) nachlegen(neu)
    }

    /**
     * Einen Klang wegwerfen — die Rückfrage steht in der Oberfläche, hier ist der
     * Entschluss schon gefasst. Stand er als Alarmton, springt die Wahl zurück.
     */
    fun klangLoeschen(id: String) {
        klaenge = klaenge.filter { it.id != id }
        klaengeMerken()
        inArbeit.remove(id)?.cancel()
        stuecke.remove(id)
        if (zuletzt?.first == id) zuletzt = null
        ordner()?.let { runCatching { File(it, id).delete() } }
        abwaehlen("klang:$id")
    }

    // ------------------------------------------------------------ Das Abo

    /**
     * Ein abgelaufenes Abo nimmt den Alarmton weg, der am Abo hängt — die Zeilen zum
     * Ton aus `abogeraeteNachpruefen` im Web. Die Baupläne und Dateien bleiben
     * liegen; was endet, ist das Recht, sie zu hören, nicht der Besitz.
     */
    suspend fun aboNachpruefen(ablage: Ablage) {
        val ton = ablage.melderTonFluss().first()
        val amAbo = ton == "eigen" || ton.startsWith("eigen:") || ton.startsWith("klang:") ||
            Melderkatalog.TOENE.firstOrNull { it.id == ton }?.premium == true
        if (amAbo) ablage.melderTonSetzen(TON_STANDARD)
    }

    private fun neueKennung(): String =
        System.currentTimeMillis().toString(36) + Random.nextInt(1296).toString(36)

    private const val S_ENTWURF = "entwurf"
    private const val S_TOENE = "toene"
    private const val S_KLAENGE = "klaenge"
}
