package de.pagerspass.pagerspass.ansichten

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.pagerspass.pagerspass.mobil.Tonwahl
import de.pagerspass.pagerspass.mobil.rememberAblage
import de.pagerspass.pagerspass.mobil.rememberAlarmierungsart
import de.pagerspass.pagerspass.mobil.rememberMelderTon
import de.pagerspass.pagerspass.mobil.rememberMelderprofil
import de.pagerspass.pagerspass.mobil.rememberTonprobe
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.ui.schmuck.Melderkatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Was auf dem Melder liegt — `alarme` und `alarmQuittiert` aus `web/src/stores/spiel.ts`.
 *
 * <b>Quittieren und Löschen sind zwei Handgriffe</b>, so wie am echten Gerät: Der
 * erste Druck stellt Ton und Vibration ab, die Meldung aber bleibt stehen — man will
 * auf der Fahrt noch ablesen können, wohin es geht. Weg ist sie erst mit Status 3
 * oder 4, oder wenn der Einsatz diesem Fahrzeug nicht mehr gehört.
 *
 * <b>Mehrere Alarme stapeln sich.</b> Der jüngste liegt oben; ein zweiter Alarm zum
 * selben Einsatz ersetzt den ersten, statt ihn zu verdoppeln.
 *
 * Der Stand gehört dem Gerät und nicht dem Server — deshalb hier und nicht in der
 * `Runde`: Der Server kennt nur `alarmOffen`; ob die Meldung noch auf dem Display
 * steht, ist eine Frage dieses Bildschirms.
 */
class Melderstand {
    var alarme by mutableStateOf<List<Alarmmeldung>>(emptyList())
        private set

    var quittiert by mutableStateOf(false)
        private set

    /** Die Meldung oben auf dem Stapel. */
    val aktuell: Alarmmeldung? get() = alarme.firstOrNull()

    /** Auf dem Display steht eine Meldung — vom Alarm bis Status 3. */
    val meldungSteht: Boolean get() = aktuell != null

    /** Das Gerät alarmiert gerade: Ton, Vibration, blinkende Leuchte. */
    val hatAlarm: Boolean get() = meldungSteht && !quittiert

    fun neu(alarm: Alarmmeldung) {
        alarme = listOf(alarm) + alarme.filter { it.incidentId != alarm.incidentId }
        // Eine neue Meldung ist unquittiert — auch wenn die vorige schon still war.
        quittiert = false
    }

    fun quittieren() {
        quittiert = true
    }

    fun raeumen() {
        alarme = emptyList()
        quittiert = false
    }
}

/** Die Taktung, in der sich eine quittierte, aber stehende Meldung wieder meldet. */
const val ERINNERUNG_TAKT_MS = 60_000L

/** Die Uhr eines Geräts — sekündlich, damit Zeiten und Restzeiten nicht stehen bleiben. */
@Composable
fun rememberSekundenuhr(): Long {
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            jetzt = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return jetzt
}

private val UHR_KURZ = DateTimeFormatter.ofPattern("HH:mm")
private val UHR_LANG = DateTimeFormatter.ofPattern("HH:mm:ss")
private val DATUM_KURZ = DateTimeFormatter.ofPattern("dd. MMM", Locale.GERMAN)

/**
 * Was jeder Melder anzeigt — `composables/melderanzeige.ts`.
 *
 * Es gibt zehn Gehäuse, aber nur **eine** Meldung: dieselbe Uhr, dieselben Zeilen,
 * derselbe Speicher. Was den Geräten gehört, ist allein das Gehäuse — wie viele
 * Zeilen das Display fasst, wo die Tasten sitzen.
 */
class Melderanzeige internal constructor(
    val melder: Melderstand,
    val zeilen: List<String>,
    private val zeilenJeSeite: Int?,
    val vergangene: List<Alarmmeldung>,
    val zeitKurz: String,
    val zeitLang: String,
    val datumKurz: String,
    private val sichtbarStand: androidx.compose.runtime.MutableIntState,
    private val anfangStand: androidx.compose.runtime.MutableIntState,
    private val speicherStand: androidx.compose.runtime.MutableState<Boolean>,
) {
    val hatAlarm: Boolean get() = melder.hatAlarm
    val meldungSteht: Boolean get() = melder.meldungSteht
    val aktuell: Alarmmeldung? get() = melder.aktuell

    /** Die eigene Meldung der Leitstelle — dieselbe Wahrheit wie in `zeilen`. */
    val zusatz: String? get() = aktuell?.zusatztext?.trim()?.takeIf { it.isNotEmpty() }

    val sichtbar: Int get() = sichtbarStand.intValue

    /** Was im Sichtfenster steht — ein DME baut die Meldung Zeile für Zeile auf. */
    val angezeigt: List<String>
        get() {
            val da = zeilen.take(sichtbarStand.intValue)
            val platz = zeilenJeSeite ?: return da
            return da.drop(anfangStand.intValue).take(platz)
        }

    val laeuftEin: Boolean get() = meldungSteht && sichtbarStand.intValue < zeilen.size

    val mehrOben: Boolean get() = zeilenJeSeite != null && anfangStand.intValue > 0

    val mehrUnten: Boolean
        get() = zeilenJeSeite != null && anfangStand.intValue + zeilenJeSeite < zeilen.size

    var speicherOffen: Boolean
        get() = speicherStand.value
        set(wert) {
            speicherStand.value = wert
        }

    /** Blättern an den Tasten — mit Sichtfenster verschiebt es das Fenster. */
    fun blaettern(richtung: Int) {
        val platz = zeilenJeSeite
        if (platz == null) {
            sichtbarStand.intValue = (sichtbarStand.intValue + richtung).coerceIn(1, zeilen.size.coerceAtLeast(1))
            return
        }
        val letzteSeite = (zeilen.size - platz).coerceAtLeast(0)
        anfangStand.intValue = (anfangStand.intValue + richtung).coerceIn(0, letzteSeite)
    }
}

/** Die Zeilen einer Meldung, wie ein Piepser sie schreibt — `zeilen` im Web. */
fun meldungszeilen(alarm: Alarmmeldung): List<String> = listOf(
    "*ALARM*        ${uhrzeit(alarm.zeit)}",
    alarm.schleife,
    alarm.funkgruppe?.let { "GRUPPE $it" }.orEmpty(),
    "${alarm.stichwort} ${alarm.stichwortText}".trim(),
    alarm.adresse,
    alarm.ortsteil?.let { "OT $it" }.orEmpty(),
    alarm.meldebild,
    "EINH: ${alarm.einheiten.joinToString(", ")}",
    alarm.zusatztext?.trim()?.takeIf { it.isNotEmpty() }?.let { "LST: $it" }.orEmpty(),
).filter { it.isNotBlank() }

/**
 * Die Anzeige eines Gehäuses.
 *
 * @param zeilenJeSeite Wie viele Zeilen das Display fasst; `null` zeigt alles (Fax, Monitor).
 * @param kennung Die Kurzkennung des eigenen Fahrzeugs, schon in der gewählten Form.
 */
@Composable
fun rememberMelderanzeige(
    melder: Melderstand,
    verlauf: List<Alarmmeldung>,
    kennung: String,
    schleife: String,
    zeilenJeSeite: Int?,
): Melderanzeige {
    val jetzt = rememberSekundenuhr()
    val zeit = java.time.Instant.ofEpochMilli(jetzt).atZone(java.time.ZoneId.systemDefault())
    val alarm = melder.aktuell
    val zeitLang = UHR_LANG.format(zeit)

    val zeilen = if (alarm == null) {
        listOf(zeitLang, kennung.ifBlank { "----" }, "BETRIEBSBEREIT", schleife).filter { it.isNotBlank() }
    } else {
        meldungszeilen(alarm)
    }

    val sichtbar = remember { mutableIntStateOf(0) }
    val anfang = remember { mutableIntStateOf(0) }
    val speicher = remember { mutableStateOf(false) }

    // Die Meldung läuft ein — Zeile für Zeile, alle 170 ms, und das Fenster wandert mit.
    LaunchedEffect(alarm) {
        if (alarm == null) {
            sichtbar.intValue = zeilen.size
            anfang.intValue = 0
            return@LaunchedEffect
        }
        val gesamt = meldungszeilen(alarm).size
        sichtbar.intValue = 0
        anfang.intValue = 0
        while (sichtbar.intValue < gesamt) {
            delay(170)
            sichtbar.intValue += 1
            if (zeilenJeSeite != null) anfang.intValue = (sichtbar.intValue - zeilenJeSeite).coerceAtLeast(0)
        }
    }

    return Melderanzeige(
        melder = melder,
        zeilen = zeilen,
        zeilenJeSeite = zeilenJeSeite,
        vergangene = verlauf.filter { it.incidentId != alarm?.incidentId },
        zeitKurz = UHR_KURZ.format(zeit),
        zeitLang = zeitLang,
        datumKurz = DATUM_KURZ.format(zeit).replace(" ", ""),
        sichtbarStand = sichtbar,
        anfangStand = anfang,
        speicherStand = speicher,
    )
}

// ---------------------------------------------------------------- Gerätemenü

/** Eine Zeile im Gerätemenü. */
data class Menueeintrag(val id: String, val name: String, val wert: String = "")

/**
 * Das Gerätemenü (Abo) — `composables/meldermenue.ts`.
 *
 * Bedient mit den Tasten des Gehäuses: ▲ und ▼ bewegen den Balken (die Auswahl
 * läuft um), die Mitte wählt. Zurück geht es über die letzte Zeile jeder Seite —
 * an einem Gerät mit unbeschrifteten Tasten die ehrlichere Stelle dafür.
 */
class Meldermenue internal constructor(
    val darfMenue: Boolean,
    private val seiteStand: androidx.compose.runtime.MutableState<String>,
    private val auswahlStand: androidx.compose.runtime.MutableIntState,
    private val spurStand: androidx.compose.runtime.MutableState<List<String>>,
    val eintraege: List<Menueeintrag>,
    val kopfwort: String,
    private val beiWahl: (seite: String, eintrag: Menueeintrag) -> Unit,
    private val beiBalken: (seite: String, eintrag: Menueeintrag?) -> Unit,
    private val beiLauter: (Float) -> Unit,
    private val beiTaste: () -> Unit,
) {
    val seite: String get() = seiteStand.value
    val offen: Boolean get() = seiteStand.value != "zu"
    val auswahl: Int get() = auswahlStand.intValue

    fun menueOeffnen() {
        beiTaste()
        seiteStand.value = "wurzel"
        auswahlStand.intValue = 0
        spurStand.value = emptyList()
    }

    fun schliessen() {
        seiteStand.value = "zu"
        auswahlStand.intValue = 0
        spurStand.value = emptyList()
    }

    /** Eine Ebene zurück — auf der Wurzel heißt das hinaus. */
    fun zurueck() {
        beiTaste()
        zurueckGehen()
    }

    internal fun zurueckGehen() {
        val spur = spurStand.value
        if (spur.isEmpty()) {
            schliessen()
            return
        }
        seiteStand.value = spur.last()
        spurStand.value = spur.dropLast(1)
        auswahlStand.intValue = 0
    }

    internal fun hinein(ziel: String) {
        spurStand.value = spurStand.value + seiteStand.value
        seiteStand.value = ziel
        auswahlStand.intValue = 0
    }

    fun bewegen(schritt: Int) {
        beiTaste()
        if (seite == "lautstaerke") {
            beiLauter(if (schritt < 0) 0.1f else -0.1f)
            return
        }
        val anzahl = eintraege.size
        if (anzahl == 0) return
        auswahlStand.intValue = (auswahlStand.intValue + schritt + anzahl) % anzahl
        beiBalken(seite, eintraege.getOrNull(auswahlStand.intValue))
    }

    fun waehlen() {
        beiTaste()
        if (!offen) {
            menueOeffnen()
            return
        }
        val eintrag = eintraege.getOrNull(auswahlStand.intValue) ?: return
        if (eintrag.id == ZURUECK) {
            zurueckGehen()
            return
        }
        beiWahl(seite, eintrag)
    }

    /** Lauter oder leiser an den Wippen — mit dem Klick des Geräts. */
    fun lauter(schritt: Float) {
        beiTaste()
        beiLauter(schritt)
    }

    companion object {
        const val ZURUECK = "zurueck"
    }
}

/**
 * Das Gerätemenü mit allem, was es anfasst: Alarmierungsart, Profil und Ton aus der
 * Ablage, Lautstärke und „Gerät aus" aus dem Tonregler, der Speicher aus dem Verlauf.
 */
@Composable
fun rememberMeldermenue(
    premium: Boolean,
    tonwahl: Tonwahl,
    verlauf: List<Alarmmeldung>,
    beiTaste: () -> Unit,
): Meldermenue {
    val ablage = rememberAblage()
    val bereich = rememberCoroutineScope()
    val probe = rememberTonprobe()
    val art by rememberAlarmierungsart()
    val ton by rememberMelderTon()
    val profilId by rememberMelderprofil()

    val seite = remember { mutableStateOf("zu") }
    val auswahl = remember { mutableIntStateOf(0) }
    val spur = remember { mutableStateOf<List<String>>(emptyList()) }

    val profil = Melderkatalog.PROFILE.firstOrNull { it.id == profilId } ?: Melderkatalog.PROFILE.first()
    val artName = Melderkatalog.ALARMIERUNGSARTEN.firstOrNull { it.id == art }?.name.orEmpty()
    val tonName = Melderkatalog.TOENE.firstOrNull { it.id == ton }?.name ?: "Eigener Ton"
    val lautWort = "${(tonwahl.melder * 100).toInt()} %"
    val zurueck = Menueeintrag(Meldermenue.ZURUECK, "Zurück")

    val eintraege = when (seite.value) {
        "wurzel" -> listOf(
            Menueeintrag("alarmierung", "Alarmierung", artName),
            Menueeintrag("ausschalten", if (tonwahl.geraetAus) "Einschalten" else "Ausschalten"),
            Menueeintrag("einstellungen", "Einstellungen"),
            Menueeintrag("meldungen", "Meldungen"),
            Menueeintrag("profile", "Profile", profil.name),
            Menueeintrag("probealarm", "Probealarm", if (probe.laeuft != null) "♪" else ""),
            Menueeintrag(Meldermenue.ZURUECK, "Menü schließen"),
        )
        "alarmierung" -> Melderkatalog.ALARMIERUNGSARTEN.map {
            Menueeintrag(it.id, it.name, if (art == it.id) "●" else "")
        } + zurueck
        "einstellungen" -> listOf(
            Menueeintrag("lautstaerke", "Lautstärke", lautWort),
            Menueeintrag("ton", "Alarmton", tonName),
            zurueck,
        )
        "ton" -> Melderkatalog.TOENE.filter { !it.premium || premium }.map {
            Menueeintrag(it.id, it.name, if (ton == it.id) "●" else "")
        } + zurueck
        "profile" -> Melderkatalog.PROFILE.map {
            Menueeintrag(it.id, it.name, if (profilId == it.id) "●" else "")
        } + zurueck
        "lautstaerke" -> {
            val stufen = (tonwahl.melder * 10).toInt().coerceIn(0, 10)
            listOf(Menueeintrag("wert", "▮".repeat(stufen) + "▯".repeat(10 - stufen), lautWort))
        }
        "meldungen" -> (
            if (verlauf.isEmpty()) {
                listOf(Menueeintrag("leer", "Keine Meldungen"))
            } else {
                verlauf.mapIndexed { i, a -> Menueeintrag("m$i", "${uhrzeit(a.zeit)} ${a.stichwort}") }
            }
            ) + zurueck
        else -> emptyList()
    }

    val menue = remember { arrayOfNulls<Meldermenue>(1) }

    fun lauterStellen(schritt: Float) {
        val neu = (tonwahl.melder + schritt).coerceIn(0f, 1f)
        tonwahl.melderSetzen(neu)
        // Wer am Regler dreht, will hören — eine stumme Art hebt sich damit auf.
        if (neu > 0f && (art == "stumm" || art == "vibration")) {
            bereich.launch { ablage.alarmierungsartSetzen("voll") }
        }
    }

    val neu = Meldermenue(
        darfMenue = premium,
        seiteStand = seite,
        auswahlStand = auswahl,
        spurStand = spur,
        eintraege = eintraege,
        kopfwort = if (tonwahl.geraetAus) "Aus" else profil.name,
        beiWahl = { s, eintrag ->
            val m = menue[0]
            when (s) {
                "wurzel" -> when (eintrag.id) {
                    "ausschalten" -> {
                        tonwahl.geraetAus = !tonwahl.geraetAus
                        m?.schliessen()
                    }
                    "probealarm" -> probe.spielen(ton)
                    else -> m?.hinein(eintrag.id)
                }
                "alarmierung" -> {
                    bereich.launch { ablage.alarmierungsartSetzen(eintrag.id) }
                    m?.zurueckGehen()
                }
                "einstellungen" -> m?.hinein(eintrag.id)
                "ton" -> {
                    bereich.launch { ablage.melderTonSetzen(eintrag.id) }
                    probe.beenden()
                    m?.zurueckGehen()
                }
                "profile" -> {
                    val p = Melderkatalog.PROFILE.firstOrNull { it.id == eintrag.id }
                    if (p != null) {
                        bereich.launch { ablage.melderprofilSetzen(p.id, p.art) }
                        tonwahl.profilNachfuehren(p.id)
                    }
                    m?.zurueckGehen()
                }
                else -> m?.zurueckGehen()
            }
        },
        beiBalken = { s, eintrag ->
            // Auf der Ton-Seite ist das Blättern zugleich das Probehören.
            if (s == "ton" && eintrag != null && eintrag.id != Meldermenue.ZURUECK) probe.spielen(eintrag.id)
        },
        beiLauter = { lauterStellen(it) },
        beiTaste = beiTaste,
    )
    menue[0] = neu
    return neu
}
