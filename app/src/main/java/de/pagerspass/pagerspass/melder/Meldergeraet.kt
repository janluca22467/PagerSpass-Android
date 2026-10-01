package de.pagerspass.pagerspass.melder

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Ein eigener Klang — eine Datei vom Gerät oder eine Aufnahme vom Mikrofon.
 *
 * Alles außer den Bytes: Die liegen als Datei unter `filesDir/melderklaenge/<id>`
 * und werden nie hochgeladen — dieselbe Zusage wie im Web („bleibt auf deinem
 * Gerät"). Eine Mikrofonaufnahme ist das Persönlichste, was dieses Spiel
 * anfassen könnte.
 */
@Serializable
data class EigenerKlang(
    val id: String,
    val name: String,
    /** `datei` oder `aufnahme` — eine Aufnahme darf man neu einsprechen, eine Datei nicht. */
    val herkunft: String = "datei",
    val typ: String = "audio/*",
    val groesse: Long = 0,
    /** Die volle Länge der Quelle, in Sekunden. */
    val laenge: Double = 0.0,
    val von: Double = 0.0,
    val bis: Double = 0.0,
    val pause: Double = 0.6,
    val angelegt: Long = 0,
)

/**
 * Der Melder dieses Geräts — Bauform, Ton, Alarmierung und alles Selbstgebaute.
 *
 * <b>Warum am Gerät und nicht am Konto.</b> Dieselbe Linie wie im Web (siehe den
 * Kopf von `stores/melder.ts` und `Meldertoene.cs` auf dem Server): Wer nachts
 * am Handy eine ruhige Ansage will und am Rechner die Sirene, darf das. Das eine
 * Stück, das am Konto hängt, ist das Gesicht (`melderGesicht` im Profil); es
 * wird hier nur gespiegelt, damit die Alarmblende es kennt, ohne das Profil zu
 * laden.
 *
 * <b>Compose-Zustand statt Fluss.</b> Jede Ansicht, die den Melder zeigt, liest
 * diese Felder direkt und zeichnet sich bei einer Änderung neu — ohne dass ein
 * Modell sie durch fünf Parameterlisten reichen muss. Geschrieben wird sofort in
 * die `SharedPreferences`: Eine Einstellung, die man trifft und die beim
 * nächsten Start fehlt, ist schlimmer als keine.
 */
object Meldergeraet {

    private const val DATEI = "melder"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private var ablage: SharedPreferences? = null
    private var ordner: File? = null

    /** Welches Gerät in der Tasche steckt — eine Katalog-Kennung oder `eigen:<id>`. */
    var bauform by mutableStateOf("dienst")
        private set

    /**
     * Piepser, Alarm-App oder gar kein eigener Melder: `dme`, `app`, `funk` —
     * `MelderToggle.vue`. Die Bauform darunter gilt nur für `dme`.
     */
    var bauart by mutableStateOf("dme")
        private set

    /** Der Alarmton — eine Katalog-Kennung, `eigen:<id>` oder `klang:<id>`. */
    var ton by mutableStateOf(Melderkatalog.TON_STANDARD)
        private set

    /** Das Gesicht vom Konto — gespiegelt, siehe oben. */
    var gesicht by mutableStateOf("standard")
        private set

    var alarmierungsart by mutableStateOf("voll")
        private set

    var profil by mutableStateOf("vollalarm")
        private set

    var lautstaerke by mutableStateOf(1f)
        private set

    var eigeneToene by mutableStateOf(listOf<EigenerTon>())
        private set

    var eigeneMelder by mutableStateOf(listOf<EigenerMelder>())
        private set

    var eigeneKlaenge by mutableStateOf(listOf<EigenerKlang>())
        private set

    /** Der Entwurf auf der Werkbank der Tonwerkstatt — nicht der gespeicherte Ton. */
    var werkbank by mutableStateOf(Tonbauplan())
        private set

    // Was zuletzt über das Konto bekannt war — für die Rückfälle unten.
    private var stufe = 0
    private var premium = false
    private var gekauft = emptySet<String>()
    private var kontoBekannt = false

    /**
     * Einmal beim ersten Gebrauch laden — jede Ansicht, die den Melder braucht,
     * ruft das; nur der erste Ruf tut etwas.
     */
    fun bereit(zusammenhang: Context): Meldergeraet {
        if (ablage != null) return this
        val a = zusammenhang.applicationContext.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
        ablage = a
        ordner = File(zusammenhang.applicationContext.filesDir, "melderklaenge").apply { mkdirs() }

        bauform = a.getString("bauform", null) ?: "dienst"
        bauart = a.getString("bauart", null) ?: "dme"
        ton = a.getString("ton", null) ?: Melderkatalog.TON_STANDARD
        gesicht = a.getString("gesicht", null) ?: "standard"
        alarmierungsart = a.getString("alarmierungsart", null) ?: "voll"
        profil = a.getString("profil", null) ?: "vollalarm"
        lautstaerke = a.getFloat("lautstaerke", 1f)
        eigeneToene = lesen("eigenetoene", ListSerializer(EigenerTon.serializer())).orEmpty()
            .map { it.copy(plan = Meldertoene.tonbauplanPruefen(it.plan)) }
            .take(TONBAU_HOECHSTZAHL)
        eigeneMelder = lesen("eigenemelder", ListSerializer(EigenerMelder.serializer())).orEmpty()
            .map { it.copy(plan = Bauplaene.pruefen(it.plan)) }
            .take(Bauplaene.MELDER_HOECHSTZAHL)
        eigeneKlaenge = lesen("eigeneklaenge", ListSerializer(EigenerKlang.serializer())).orEmpty()
            .filter { klangdatei(it.id)?.exists() == true }
            .take(KLANG_HOECHSTZAHL)
        werkbank = lesen("werkbank", Tonbauplan.serializer())?.let(Meldertoene::tonbauplanPruefen) ?: Tonbauplan()
        return this
    }

    private fun <T> lesen(schluessel: String, form: kotlinx.serialization.KSerializer<T>): T? =
        ablage?.getString(schluessel, null)?.let { runCatching { json.decodeFromString(form, it) }.getOrNull() }

    private fun <T> schreiben(schluessel: String, form: kotlinx.serialization.KSerializer<T>, wert: T) {
        ablage?.edit()?.putString(schluessel, json.encodeToString(form, wert))?.apply()
    }

    private fun merken(schluessel: String, wert: String) {
        ablage?.edit()?.putString(schluessel, wert)?.apply()
    }

    // ------------------------------------------------------------ Setzen

    fun bauformSetzen(id: String) { bauform = id; merken("bauform", id) }

    fun bauartSetzen(id: String) { bauart = id; merken("bauart", id) }

    fun tonSetzen(id: String) { ton = id; merken("ton", id) }

    fun gesichtMerken(id: String?) {
        val neu = id?.takeIf { it.isNotBlank() } ?: "standard"
        if (neu == gesicht) return
        gesicht = neu
        merken("gesicht", neu)
    }

    fun alarmierungsartSetzen(id: String) {
        alarmierungsart = id
        merken("alarmierungsart", id)
        // Eine Art von Hand gewählt heißt: Das Profil trifft nicht mehr zu.
        val passt = Melderkatalog.PROFILE.firstOrNull { it.art == id && it.lautstaerke == lautstaerke }
        profil = passt?.id ?: ""
        merken("profil", profil)
    }

    fun lautstaerkeSetzen(wert: Float) {
        lautstaerke = wert.coerceIn(0f, 1f)
        ablage?.edit()?.putFloat("lautstaerke", lautstaerke)?.apply()
        val passt = Melderkatalog.PROFILE.firstOrNull { it.art == alarmierungsart && it.lautstaerke == lautstaerke }
        profil = passt?.id ?: ""
        merken("profil", profil)
    }

    fun profilWaehlen(p: Melderkatalog.Profil) {
        alarmierungsart = p.art
        lautstaerke = p.lautstaerke
        profil = p.id
        ablage?.edit()
            ?.putString("alarmierungsart", p.art)
            ?.putFloat("lautstaerke", p.lautstaerke)
            ?.putString("profil", p.id)
            ?.apply()
    }

    fun werkbankSetzen(plan: Tonbauplan) {
        werkbank = Meldertoene.tonbauplanPruefen(plan)
        schreiben("werkbank", Tonbauplan.serializer(), werkbank)
    }

    // ---------------------------------------------------------- Eigene Töne

    /** Einen Ton sichern — `null`, wenn das Regal voll ist. Voll heißt voll, auch hier. */
    fun tonSichern(name: String, plan: Tonbauplan): EigenerTon? {
        if (eigeneToene.size >= TONBAU_HOECHSTZAHL) return null
        val satz = EigenerTon(
            id = "t${System.currentTimeMillis().toString(36)}",
            name = name.replace(Regex("\\s+"), " ").trim().ifBlank { "Eigener Ton" }.take(24),
            plan = Meldertoene.tonbauplanPruefen(plan),
        )
        eigeneToene = eigeneToene + satz
        schreiben("eigenetoene", ListSerializer(EigenerTon.serializer()), eigeneToene)
        return satz
    }

    fun tonAendern(id: String, name: String? = null, plan: Tonbauplan? = null) {
        eigeneToene = eigeneToene.map { t ->
            if (t.id != id) t else t.copy(
                name = name?.trim()?.ifBlank { t.name }?.take(24) ?: t.name,
                plan = plan?.let(Meldertoene::tonbauplanPruefen) ?: t.plan,
            )
        }
        schreiben("eigenetoene", ListSerializer(EigenerTon.serializer()), eigeneToene)
    }

    /** Wer den eingestellten Ton löscht, hört beim nächsten Alarm den Standard — nicht Stille. */
    fun tonLoeschen(id: String) {
        eigeneToene = eigeneToene.filterNot { it.id == id }
        schreiben("eigenetoene", ListSerializer(EigenerTon.serializer()), eigeneToene)
        if (ton == "eigen:$id") tonSetzen(Melderkatalog.TON_STANDARD)
    }

    fun planVon(art: String): Tonbauplan? = when {
        art == "eigen" -> werkbank
        art.startsWith("eigen:") -> eigeneToene.firstOrNull { it.id == art.removePrefix("eigen:") }?.plan ?: werkbank
        else -> null
    }

    // -------------------------------------------------------- Eigene Melder

    fun melderSichern(id: String?, name: String, plan: Melderbauplan): EigenerMelder? {
        val sauber = Bauplaene.pruefen(plan)
        val kurz = name.replace(Regex("\\s+"), " ").trim().ifBlank { "Eigener Melder" }.take(Bauplaene.NAME_LAENGE)
        val vorhanden = id?.let { i -> eigeneMelder.firstOrNull { it.id == i } }
        val satz: EigenerMelder
        if (vorhanden != null) {
            satz = vorhanden.copy(name = kurz, plan = sauber)
            eigeneMelder = eigeneMelder.map { if (it.id == satz.id) satz else it }
        } else {
            if (eigeneMelder.size >= Bauplaene.MELDER_HOECHSTZAHL) return null
            satz = EigenerMelder("m${System.currentTimeMillis().toString(36)}", kurz, sauber)
            eigeneMelder = eigeneMelder + satz
        }
        schreiben("eigenemelder", ListSerializer(EigenerMelder.serializer()), eigeneMelder)
        return satz
    }

    fun melderLoeschen(id: String) {
        eigeneMelder = eigeneMelder.filterNot { it.id == id }
        schreiben("eigenemelder", ListSerializer(EigenerMelder.serializer()), eigeneMelder)
        if (bauform == "eigen:$id") bauformSetzen("dienst")
    }

    fun eigenerPlan(): Melderbauplan? =
        bauform.takeIf { it.startsWith("eigen:") }
            ?.let { b -> eigeneMelder.firstOrNull { it.id == b.removePrefix("eigen:") }?.plan }

    // ------------------------------------------------------- Eigene Klänge

    const val KLANG_HOECHSTZAHL = 8
    const val KLANG_HOECHSTBYTES = 3L * 1024 * 1024
    const val KLANG_HOECHSTDAUER = 12.0
    const val KLANG_HOECHSTPAUSE = 4.0

    fun klangdatei(id: String): File? = ordner?.let { File(it, id.replace(Regex("[^a-z0-9]"), "")) }

    /**
     * Einen Klang anlegen — die Bytes als Datei, der Satz in die Liste.
     *
     * Der Deckel wird hier geprüft und nicht in der Oberfläche: Voll heißt voll,
     * auch für einen Klang, der über einen zweiten Weg hereinkommt.
     */
    fun klangAnlegen(name: String, bytes: ByteArray, typ: String, herkunft: String, laenge: Double): EigenerKlang? {
        if (bytes.size > KLANG_HOECHSTBYTES) return null
        if (eigeneKlaenge.size >= KLANG_HOECHSTZAHL) return null
        val id = "k${System.currentTimeMillis().toString(36)}${(Math.random() * 1296).toInt().toString(36)}"
        val datei = klangdatei(id) ?: return null
        if (runCatching { datei.writeBytes(bytes) }.isFailure) return null
        val satz = EigenerKlang(
            id = id,
            name = name.replace(Regex("\\s+"), " ").trim()
                .ifBlank { if (herkunft == "aufnahme") "Eingesprochen" else "Eigene Datei" }.take(24),
            herkunft = herkunft,
            typ = typ,
            groesse = bytes.size.toLong(),
            laenge = laenge,
            von = 0.0,
            // Was länger ist als der Deckel, steht beim Anlegen schon geschnitten da.
            bis = minOf(laenge, KLANG_HOECHSTDAUER),
            pause = 0.6,
            angelegt = System.currentTimeMillis(),
        )
        eigeneKlaenge = eigeneKlaenge + satz
        schreiben("eigeneklaenge", ListSerializer(EigenerKlang.serializer()), eigeneKlaenge)
        return satz
    }

    fun klangAendern(satz: EigenerKlang) {
        val laenge = if (satz.laenge > 0) satz.laenge else KLANG_HOECHSTDAUER
        val von = satz.von.coerceIn(0.0, laenge)
        val bis = satz.bis.coerceIn(von + 0.05, laenge).coerceAtMost(von + KLANG_HOECHSTDAUER)
        val sauber = satz.copy(
            name = satz.name.trim().ifBlank { "Eigener Klang" }.take(24),
            von = von,
            bis = bis,
            pause = satz.pause.coerceIn(0.0, KLANG_HOECHSTPAUSE),
        )
        eigeneKlaenge = eigeneKlaenge.map { if (it.id == sauber.id) sauber else it }
        schreiben("eigeneklaenge", ListSerializer(EigenerKlang.serializer()), eigeneKlaenge)
    }

    fun klangLoeschen(id: String) {
        klangdatei(id)?.delete()
        eigeneKlaenge = eigeneKlaenge.filterNot { it.id == id }
        schreiben("eigeneklaenge", ListSerializer(EigenerKlang.serializer()), eigeneKlaenge)
        if (ton == "klang:$id") tonSetzen(Melderkatalog.TON_STANDARD)
    }

    fun klang(art: String): EigenerKlang? =
        art.takeIf { it.startsWith("klang:") }?.let { a -> eigeneKlaenge.firstOrNull { it.id == a.removePrefix("klang:") } }

    // ---------------------------------------------------------- Rückfälle

    /**
     * Was über das Konto bekannt ist — und die Wahl dagegen prüfen.
     *
     * Dasselbe wie der `watchEffect` in `MelderGeraet.vue` und
     * `abogeraeteNachpruefen` im Web: Eine Bauform, die (nicht mehr) offensteht,
     * fällt aufs Dienstgerät zurück, ein Ton auf den Zweiklang. <b>Aber nur, wenn
     * es etwas zu prüfen gibt</b> — ohne Konto fielen Stufe und Abo auf ihre
     * Vorgaben, und jede Wahl wäre „nicht frei". Das ist kein Befund, sondern
     * eine offene Frage.
     *
     * @param gekauft Die Artikel-Kennungen aus dem Besitzstand (`ton-…`,
     *   `melder-…`); `null` heißt: nicht geladen — dann bleiben gekaufte Töne
     *   unangetastet, statt fälschlich zurückzufallen.
     */
    fun kontoMerken(stufe: Int, premium: Boolean, gekauft: Set<String>?) {
        this.stufe = stufe
        this.premium = premium
        if (gekauft != null) this.gekauft = gekauft
        kontoBekannt = true

        val b = Melderkatalog.bauform(bauform)
        if (bauform.startsWith("eigen:") && !premium) bauformSetzen("dienst")
        else if (b == null && !bauform.startsWith("eigen:")) bauformSetzen("dienst")
        else if (b != null && !b.frei(stufe, premium)) bauformSetzen("dienst")

        val katalogton = Melderkatalog.TOENE.firstOrNull { it.id == ton }
        when {
            (ton.startsWith("eigen") || ton.startsWith("klang:")) && !premium -> tonSetzen(Melderkatalog.TON_STANDARD)
            katalogton != null && katalogton.preisCredits != null && gekauft == null -> Unit
            katalogton != null && !katalogton.frei(stufe, premium, this.gekauft) -> tonSetzen(Melderkatalog.TON_STANDARD)
        }
    }

    /**
     * Ob das Konto ein laufendes Abo hat — daran hängt das Gerätemenü
     * (`darfMenue` in `composables/meldermenue.ts`). Ohne bekanntes Konto:
     * nein; ein Menü, das eine Zehntelsekunde nach dem Laden wieder
     * verschwindet, wäre schlimmer als eines, das eine Zehntelsekunde später
     * erscheint.
     */
    val abo: Boolean get() = kontoBekannt && premium

    /**
     * Die Töne, die dieses Konto hören darf — in der Reihenfolge der Tonwahl:
     * erst die selbst gebauten, dann die Klänge, dann der Katalog.
     *
     * Für die Ton-Seite des Gerätemenüs. Ein gesperrter Ton steht dort gar
     * nicht erst; die Kontoseite zeigt ihn mit Sperrgrund, das Gerät nicht —
     * dasselbe wie im Web.
     */
    fun hoerbareToene(): List<Pair<String, String>> {
        val eigene = if (premium) {
            eigeneToene.map { "eigen:${it.id}" to "★ ${it.name}" } +
                eigeneKlaenge.map { "klang:${it.id}" to "♪ ${it.name}" }
        } else {
            emptyList()
        }
        return eigene + Melderkatalog.TOENE
            .filter { it.id == ton || it.frei(stufe, premium, gekauft) }
            .map { it.id to it.name }
    }

    /** Der Name des eingestellten Tons — für die Wertspalte im Menü. */
    fun tonName(): String = hoerbareToene().firstOrNull { it.first == ton }?.second
        ?: Melderkatalog.TOENE.firstOrNull { it.id == ton }?.name
        ?: "Eigener Ton"

    /** Die Bauform, die gerade gilt — die Wahl, wenn sie offensteht. */
    fun wirksameBauform(): String {
        if (!kontoBekannt) return bauform
        if (bauform.startsWith("eigen:")) return if (premium && eigenerPlan() != null) bauform else "dienst"
        val b = Melderkatalog.bauform(bauform) ?: return "dienst"
        return if (b.frei(stufe, premium)) bauform else "dienst"
    }
}
