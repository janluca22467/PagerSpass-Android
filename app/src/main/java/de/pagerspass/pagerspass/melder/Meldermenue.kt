package de.pagerspass.pagerspass.melder

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Eine Zeile im Gerätemenü — der Wert rechts ist der aktuelle, wie am echten Gerät. */
data class Menueeintrag(val id: String, val name: String, val wert: String = "")

/**
 * Was das Display zeigt, solange das Menü offen ist: der Kopf, die Zeilen und
 * welche davon der Balken trägt.
 */
data class Menuebild(val titel: String, val eintraege: List<Menueeintrag>, val auswahl: Int)

/**
 * Das Gerätemenü des Meldeempfängers — das, was hinter der mittleren Taste liegt.
 *
 * Übertragen aus `composables/meldermenue.ts`. <b>Keine zweite Wahrheit:</b>
 * Jeder Menüpunkt greift auf denselben Zustand zu, den auch „Dein Melder"
 * bedient ([Meldergeraet]) — dieselbe Lautstärke, derselbe Ton, dieselbe
 * Alarmierungsart. Ein Menü mit eigenen Werten wäre ein Gerät, das etwas
 * anderes tut als das, was in den Einstellungen steht.
 *
 * <b>Der Zustand liegt im Objekt und nicht in der Ansicht.</b> Das Gerät steht
 * an mehreren Stellen (Status-Reiter, Alarmblende, „Dein Melder"), und wer
 * mitten im Blättern einen Alarm bekommt, soll danach nicht von vorn anfangen —
 * dieselbe Begründung wie im Web, wo man die Bauform im Dienst wechselt.
 *
 * <b>Das Menü gehört zum Abo</b> (`darfMenue`): Wer keines hat, bedient seinen
 * Melder wie bisher — quittieren, blättern, Speicher — und stellt Ton und
 * Lautstärke auf der Seite „Dein Melder" ein.
 */
object Meldermenue {

    private const val ZURUECK = "zurueck"

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var probeJob: Job? = null

    /** `zu`, `wurzel`, `alarmierung`, `einstellungen`, `lautstaerke`, `ton`, `meldungen`, `profile`. */
    var seite by mutableStateOf("zu")
        private set

    var auswahl by mutableStateOf(0)
        private set

    private var spur = listOf<String>()

    /**
     * Ob das Gerät ausgeschaltet ist.
     *
     * <b>„Aus" heißt hier der Ton, nicht der Empfang</b> — die Begründung aus dem
     * Web: Ein Menüpunkt zwei Tastendrücke tief, hinter dem man eine ganze
     * Schicht verpasst, wäre eine Falle. Das Display wird dunkel, der Melder
     * schweigt, und die Meldung steht trotzdem da, wenn man hinsieht.
     *
     * Flüchtig, mit Absicht: Nach einem Neustart ist das Gerät an. Ein Melder,
     * der sich über Nacht „aus" merkt, wäre der eine, der morgens nicht piept.
     */
    var geraetAus by mutableStateOf(false)
        private set

    /** Ob gerade ein Probealarm aus dem Menü läuft. */
    var probeLaeuft by mutableStateOf(false)
        private set

    val offen: Boolean get() = seite != "zu"

    val darfMenue: Boolean get() = Meldergeraet.abo

    /** Was rechts im Kopf des Displays steht — wie am echten Gerät. */
    val kopfwort: String
        get() = if (geraetAus) "Aus" else
            Melderkatalog.PROFILE.firstOrNull { it.id == Meldergeraet.profil }?.name ?: "Eigenes Profil"

    private val lautWort: String get() = "${(Meldergeraet.lautstaerke * 100).toInt()} %"

    private val titel: String
        get() = when (seite) {
            "alarmierung" -> "Alarmierung"
            "einstellungen" -> "Einstellungen"
            "lautstaerke" -> "Lautstärke"
            "ton" -> "Alarmton"
            "meldungen" -> "Meldungen"
            "profile" -> "Profile"
            else -> "Menü"
        }

    /** Die Zeilen der gerade offenen Seite. */
    val eintraege: List<Menueeintrag>
        get() = when (seite) {
            "wurzel" -> listOf(
                Menueeintrag("alarmierung", "Alarmierung", Melderkatalog.alarmierungsart(Meldergeraet.alarmierungsart).name),
                Menueeintrag("ausschalten", if (geraetAus) "Einschalten" else "Ausschalten"),
                Menueeintrag("einstellungen", "Einstellungen"),
                Menueeintrag("meldungen", "Meldungen", Melderspeicher.meldungen.size.toString()),
                Menueeintrag("profile", "Profile", kopfwort.takeUnless { geraetAus }.orEmpty()),
                Menueeintrag("probealarm", "Probealarm", if (probeLaeuft) "♪" else ""),
                // Auf der Wurzel heißt „zurück" hinaus — das Wort sagt es.
                Menueeintrag(ZURUECK, "Menü schließen"),
            )
            "alarmierung" -> Melderkatalog.ALARMIERUNGSARTEN.map {
                Menueeintrag(it.id, it.name, if (Meldergeraet.alarmierungsart == it.id) "●" else "")
            } + Menueeintrag(ZURUECK, "Zurück")
            "einstellungen" -> listOf(
                Menueeintrag("lautstaerke", "Lautstärke", lautWort),
                Menueeintrag("ton", "Alarmton", Meldergeraet.tonName()),
                Menueeintrag(ZURUECK, "Zurück"),
            )
            "ton" -> Meldergeraet.hoerbareToene().map { (id, name) ->
                Menueeintrag(id, name, if (Meldergeraet.ton == id) "●" else "")
            } + Menueeintrag(ZURUECK, "Zurück")
            "profile" -> Melderkatalog.PROFILE.map {
                Menueeintrag(it.id, it.name, if (Meldergeraet.profil == it.id) "●" else "")
            } + Menueeintrag(ZURUECK, "Zurück")
            // Die Lautstärke als Balken aus Blockzeichen: auf jedem Display
            // dasselbe Bild, verstellt mit den Blättertasten (siehe `bewegen`).
            "lautstaerke" -> {
                val stufen = (Meldergeraet.lautstaerke * 10).toInt().coerceIn(0, 10)
                listOf(Menueeintrag("wert", "▮".repeat(stufen) + "▯".repeat(10 - stufen), lautWort))
            }
            // Der Meldungsspeicher: dieselbe Liste, die unter dem Gerät
            // aufklappt — nur eben im Gerät, wo das Vorbild sie führt.
            "meldungen" -> (
                if (Melderspeicher.meldungen.isEmpty()) listOf(Menueeintrag("leer", "Keine Meldungen"))
                else Melderspeicher.meldungen.mapIndexed { i, a ->
                    Menueeintrag("m$i", "${funkzeit(a.zeit)} ${a.stichwort}")
                }
                ) + Menueeintrag(ZURUECK, "Zurück")
            else -> emptyList()
        }

    /** Das Bild fürs Display — `null`, solange das Menü zu ist. */
    fun bild(): Menuebild? = if (!offen) null else Menuebild(titel, eintraege, auswahl)

    // -------------------------------------------------------- Handgriffe

    /** Das Menü aufschlagen (mit dem Klick der Taste, die es öffnet). */
    fun oeffnen() {
        if (!darfMenue) return
        Geraetegeraeusche.taste()
        seite = "wurzel"
        auswahl = 0
        spur = emptyList()
    }

    /**
     * Schließen — der eine Weg ohne Klick: Er kommt meistens vom nächsten
     * Alarm, und ein Tastenklick in dem Augenblick, in dem der Melder losgeht,
     * wäre ein Geräusch aus dem Nichts.
     */
    fun schliessen() {
        probeAbbrechen()
        seite = "zu"
        spur = emptyList()
        auswahl = 0
    }

    /** Die Zurück-Taste: eine Ebene hinauf, auf der Wurzel hinaus. */
    fun zurueck() {
        Geraetegeraeusche.taste()
        zurueckGehen()
    }

    private fun zurueckGehen() {
        Melderspieler.stoppen("probe")
        val vorher = spur.lastOrNull()
        if (vorher == null) {
            schliessen()
            return
        }
        spur = spur.dropLast(1)
        seite = vorher
        auswahl = 0
    }

    private fun hinein(zusammenhang: Context, ziel: String) {
        spur = spur + seite
        seite = ziel
        auswahl = 0
        tonUnterDemBalken(zusammenhang)
    }

    /**
     * Hoch und runter — die Auswahl läuft um, wie am Gerät. Auf der
     * Lautstärkeseite drehen dieselben Tasten den Regler: hoch heißt lauter.
     */
    fun bewegen(zusammenhang: Context, schritt: Int) {
        Geraetegeraeusche.taste()
        if (seite == "lautstaerke") {
            lauterStellen(if (schritt < 0) 0.1f else -0.1f)
            return
        }
        val anzahl = eintraege.size
        if (anzahl == 0) return
        auswahl = (auswahl + schritt + anzahl) % anzahl
        // Auf der Ton-Seite ist Blättern zugleich Probehören.
        tonUnterDemBalken(zusammenhang)
    }

    /**
     * „Wählen" — der eine Handgriff, der etwas auslöst. Das Kreuz bewegt nur
     * den Balken; wer sich verklickt, hat sich damit noch nichts eingestellt.
     */
    fun waehlen(zusammenhang: Context) {
        Geraetegeraeusche.taste()
        if (!offen) {
            if (darfMenue) { seite = "wurzel"; auswahl = 0; spur = emptyList() }
            return
        }
        val eintrag = eintraege.getOrNull(auswahl) ?: return
        if (eintrag.id == ZURUECK) {
            zurueckGehen()
            return
        }
        when (seite) {
            "wurzel" -> when (eintrag.id) {
                "ausschalten" -> {
                    geraetAus = !geraetAus
                    schliessen()
                    if (!geraetAus) Geraetegeraeusche.einschalten()
                }
                "probealarm" -> probealarm(zusammenhang)
                else -> hinein(zusammenhang, eintrag.id)
            }
            "alarmierung" -> {
                Meldergeraet.alarmierungsartSetzen(eintrag.id)
                zurueckGehen()
            }
            "einstellungen" -> hinein(zusammenhang, eintrag.id)
            "ton" -> {
                Meldergeraet.tonSetzen(eintrag.id)
                zurueckGehen()
            }
            "profile" -> {
                Melderkatalog.PROFILE.firstOrNull { it.id == eintrag.id }?.let { Meldergeraet.profilWaehlen(it) }
                zurueckGehen()
            }
            else -> zurueckGehen()
        }
    }

    /** Derselbe Regler von außen — die Lautstärkewippen am Farbmelder. */
    fun lauter(schritt: Float) {
        Geraetegeraeusche.taste()
        lauterStellen(schritt)
    }

    private fun lauterStellen(schritt: Float) {
        val neu = ((Meldergeraet.lautstaerke + schritt) * 100).toInt().coerceIn(0, 100) / 100f
        Meldergeraet.lautstaerkeSetzen(neu)
        // Wer am Regler dreht, will hören — eine stumme Art hebt sich damit auf.
        if (neu > 0f && !Melderkatalog.alarmierungsart(Meldergeraet.alarmierungsart).ton) {
            Meldergeraet.alarmierungsartSetzen("voll")
        }
    }

    /**
     * Der Probealarm — ein Durchlauf des eingestellten Tons, dann Ruhe. Er
     * läuft mit der eingestellten Art: Wer auf „Stille Alarmierung" steht,
     * hört auch die Probe nicht. Das ist der Sinn der Probe — sie zeigt, was
     * ein Alarm jetzt täte.
     */
    private fun probealarm(zusammenhang: Context) {
        if (probeLaeuft) return
        val art = Melderkatalog.alarmierungsart(Meldergeraet.alarmierungsart)
        probeLaeuft = true
        if (art.ton && !geraetAus) Melderspieler.probe(zusammenhang, Meldergeraet.ton, 1)
        if (art.vibration) Melderspieler.vibrieren(zusammenhang)
        probeJob?.cancel()
        probeJob = bereich.launch {
            delay(3_000)
            Melderspieler.vibrationAus()
            probeLaeuft = false
        }
    }

    /**
     * Den Ton unter dem Balken anspielen — nur auf der Ton-Seite. Die Probe ist
     * vom Profil ausgenommen (anders als der Probealarm): Man kommt auf diese
     * Seite, um zu hören.
     */
    private fun tonUnterDemBalken(zusammenhang: Context) {
        if (seite != "ton") return
        val eintrag = eintraege.getOrNull(auswahl) ?: return
        if (eintrag.id == ZURUECK) return
        Melderspieler.stoppen("probe")
        Melderspieler.probe(zusammenhang, eintrag.id, 1)
    }

    private fun probeAbbrechen() {
        probeJob?.cancel()
        probeJob = null
        if (probeLaeuft) {
            Melderspieler.vibrationAus()
            probeLaeuft = false
        }
        Melderspieler.stoppen("probe")
    }
}

/** „14:32" aus dem ISO-Zeitstempel einer Meldung — `funkzeit` im Web. */
fun funkzeit(iso: String): String =
    Regex("T(\\d{2}:\\d{2})").find(iso)?.groupValues?.get(1) ?: iso.take(5)
