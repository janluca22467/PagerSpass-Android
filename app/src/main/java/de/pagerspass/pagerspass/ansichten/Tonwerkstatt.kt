package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Eigentoene
import de.pagerspass.pagerspass.mobil.Meldertonprobe
import de.pagerspass.pagerspass.mobil.Tonbauplan
import de.pagerspass.pagerspass.mobil.rememberAblage
import de.pagerspass.pagerspass.mobil.rememberMelderTon
import de.pagerspass.pagerspass.mobil.rememberTonprobe
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Zeichenknopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Die Tonwerkstatt — der eigene Melderton, gezeichnet statt ausgefüllt.
 *
 * Übertragen aus `web/src/components/ui/Tonwerkstatt.vue`. Ein Raster aus sechzehn
 * Schritten und fünfzehn Höhen: Ein Feld ist ein Ton, ein leeres eine Pause — so ist
 * auch „di--di" baubar, die Hälfte aller echten Meldertöne. Jede Änderung wird sofort
 * vorgespielt, und beim Hören läuft ein Zeiger mit.
 *
 * <b>Werkbank und Regal.</b> Das Raster ist der Entwurf ([Eigentoene.entwurf]); was
 * man behalten will, bekommt einen Namen und steht danach in der Tonwahl. Ein Ton aus
 * dem Regal wird auf die Werkbank <em>kopiert</em> — erst „Sichern" schreibt zurück.
 * Weitergegeben wird er als Code aus sechzehn Zeichen.
 *
 * <b>Was er ausdrücklich nicht kann:</b> keine Lautstärke, keine Zahl der
 * Wiederholungen, keine Zweitstimme. Ohne Abo steht der Baukasten trotzdem da, nur
 * gesperrt — wer nicht sieht, dass es ihn gibt, vermisst ihn auch nicht.
 */
@Composable
fun Tonwerkstatt(premium: Boolean, beiSchliessen: () -> Unit) {
    val zusammenhang = LocalContext.current
    remember(zusammenhang) { Eigentoene.sicherstellen(zusammenhang) }
    val probe = rememberTonprobe()
    val zwischenablage = LocalClipboardManager.current
    val ablage = rememberAblage()
    val bereich = rememberCoroutineScope()
    val eingestellt by rememberMelderTon()

    val plan = Eigentoene.entwurf
    val toene = Eigentoene.toene
    val gesetzt = plan.schritte.count { it != null }
    val laenge = Eigentoene.laenge(plan.schritte)
    val sekunden = String.format(Locale.ROOT, "%.2f", laenge * plan.abstand)
    val takt = Eigentoene.TAKTE.minByOrNull { abs(it.abstand - plan.abstand) } ?: Eigentoene.TAKTE[1]
    val voll = toene.size >= Eigentoene.HOECHSTZAHL
    val code = Eigentoene.codeVon(plan)

    // Welchen gesicherten Ton man bearbeitet — `null` heißt „ein neuer".
    var bearbeitet by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var codeEingabe by remember { mutableStateOf("") }
    var einfuegen by remember { mutableStateOf(false) }

    // Eine Rückmeldung, die nach vier Sekunden von selbst geht — jede neue startet die Uhr neu.
    var meldung by remember { mutableStateOf<String?>(null) }
    var meldungLauf by remember { mutableIntStateOf(0) }
    LaunchedEffect(meldungLauf) {
        if (meldungLauf == 0) return@LaunchedEffect
        delay(4000)
        meldung = null
    }
    fun sagen(text: String) {
        meldung = text
        meldungLauf += 1
    }

    // ------------------------------------------------------------ Probehören

    val probeLaeuft = probe.laeuft == "eigen"
    var probeLauf by remember { mutableIntStateOf(0) }
    var zeiger by remember { mutableIntStateOf(-1) }

    /**
     * Genau ein ganzer Durchlauf und eine Viertelsekunde — nie weniger. Eine feste Zahl
     * schnitt bei „Langsam" das letzte Viertel ab.
     */
    fun probeSpielen() {
        probe.spielen("eigen", Meldertonprobe.zyklusdauer("eigen") + 0.25)
        probeLauf += 1
    }

    /*
     * Der Zeiger ist nachgestellt, nicht gemessen: derselbe Takt, mit dem der Ton
     * gerechnet wurde, im selben Augenblick begonnen. Modulo der wirklichen Länge und
     * nicht der Rasterbreite — sonst liefe er durch ein leeres Viertel.
     */
    LaunchedEffect(probeLauf, probeLaeuft) {
        if (!probeLaeuft) {
            zeiger = -1
            return@LaunchedEffect
        }
        val entwurf = Eigentoene.entwurf
        val taktMs = entwurf.abstand * 1000
        val runde = Eigentoene.laenge(entwurf.schritte).coerceAtLeast(1)
        val beginn = System.nanoTime()
        while (true) {
            val vergangen = (System.nanoTime() - beginn) / 1_000_000.0
            zeiger = (vergangen / taktMs).toInt() % runde
            delay(max(30.0, taktMs / 4).toLong())
        }
    }

    /** Jede Änderung spielt vor — ohne Abo bleibt es still, dort ist das Raster gesperrt. */
    fun planSetzen(neu: Tonbauplan) {
        Eigentoene.entwurfSetzen(neu)
        if (premium && neu.schritte.any { it != null }) probeSpielen()
    }

    /** Derselbe Druck setzt und nimmt weg; eine Spalte trägt höchstens einen Ton. */
    fun feldSchalten(schritt: Int, hertz: Int) {
        val aktuell = Eigentoene.entwurf
        val schritte = aktuell.schritte.toMutableList()
        schritte[schritt] = if (schritte[schritt] == hertz) null else hertz
        planSetzen(aktuell.copy(schritte = schritte))
    }

    fun leeren() {
        probe.beenden()
        Eigentoene.entwurfSetzen(Eigentoene.entwurf.copy(schritte = List(Eigentoene.SCHRITTE) { null }))
    }

    // ------------------------------------------------------------ Das Regal

    /** Einen Ton aus dem Regal auf die Werkbank holen — kopiert, nicht verbunden. */
    fun oeffnen(id: String) {
        val ton = Eigentoene.toene.firstOrNull { it.id == id } ?: return
        bearbeitet = id
        name = ton.name
        planSetzen(ton.plan)
    }

    fun sichern(alsNeu: Boolean) {
        val id = Eigentoene.tonSichern(name, Eigentoene.entwurf, if (alsNeu) null else bearbeitet)
        if (id == null) {
            sagen("Das Regal ist voll — ${Eigentoene.HOECHSTZAHL} Töne sind das Höchste. Wirf einen weg.")
            return
        }
        bearbeitet = id
        name = Eigentoene.toene.firstOrNull { it.id == id }?.name ?: name
        // Gesichert heißt eingestellt: Ein zweiter Druck in der Tonwahl wäre ein
        // Schritt, den man vergisst — und beim nächsten Alarm hörte man den alten Ton.
        bereich.launch { ablage.melderTonSetzen("eigen:$id") }
        sagen("Gesichert und eingestellt.")
    }

    fun wegwerfen(id: String) {
        Eigentoene.tonLoeschen(id)
        if (bearbeitet == id) bearbeitet = null
    }

    /**
     * Einen fremden Code auf die Werkbank legen — nicht sofort sichern: Man soll ihn
     * erst hören, und ein Code, der ungefragt ein Fach belegt, ist bei zwölf eine Zumutung.
     */
    fun codeUebernehmen() {
        val gelesen = Eigentoene.codeLesen(codeEingabe)
        if (gelesen == null) {
            sagen("Das ist kein gültiger Ton-Code. Prüf ihn auf Tippfehler.")
            return
        }
        bearbeitet = null
        name = ""
        planSetzen(gelesen)
        codeEingabe = ""
        einfuegen = false
        sagen("Übernommen — hör ihn dir an und sichere ihn, wenn er dir gefällt.")
    }

    fun schliessen() {
        probe.beenden()
        beiSchliessen()
    }

    Blende(
        titel = "Tonwerkstatt",
        beiSchliessen = ::schliessen,
        breite = Dialogbreite.Breit,
        kopfknoepfe = { Knopf("Schließen", ::schliessen, art = Knopfart.Leise, kompakt = true) },
        fuss = {
            SehrLeise("Deine Töne liegen an diesem Gerät — am Rechner andere als am Handy, wenn du magst.")
            Knopf("Fertig", ::schliessen, art = Knopfart.Haupt)
        },
    ) {
        Text(
            "PAGERSPASS · WERKSTATT",
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
            color = Farben.Amber,
        )
        SehrLeise(
            "Nach rechts die sechzehn Schritte, nach oben die Höhe. Ein leeres Feld ist eine Pause. Jede " +
                "Änderung wird sofort vorgespielt — dein Ton ist nicht lauter als die anderen, nur deiner.",
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier.alpha(if (premium) 1f else 0.45f),
        ) {
            Raster(plan, laenge, zeiger, premium, ::feldSchalten)

            Etikett("Takt")
            Pillenreihe {
                Eigentoene.TAKTE.forEach { t ->
                    Pille(
                        t.name,
                        an = takt.name == t.name,
                        beiDruck = { planSetzen(Eigentoene.entwurf.copy(abstand = t.abstand, dauer = t.dauer)) },
                        aktiv = premium,
                    )
                }
            }

            Etikett("Klangfarbe")
            Pillenreihe {
                Eigentoene.FARBEN.forEach { f ->
                    Pille(
                        f.name,
                        an = plan.form == f.form,
                        beiDruck = { planSetzen(Eigentoene.entwurf.copy(form = f.form)) },
                        aktiv = premium,
                    )
                }
            }
            Eigentoene.FARBEN.firstOrNull { it.form == plan.form }?.let { SehrLeise(it.was) }

            Etikett("Werkbank")
            Pillenreihe {
                Knopf(
                    if (probeLaeuft) "Läuft …" else "Anhören",
                    beiDruck = { if (probeLaeuft) probe.beenden() else probeSpielen() },
                    art = if (probeLaeuft) Knopfart.Haupt else Knopfart.Normal,
                    aktiv = premium && gesetzt > 0,
                    kompakt = true,
                )
                Knopf(
                    "Raster leeren",
                    beiDruck = ::leeren,
                    art = Knopfart.Leise,
                    aktiv = premium && gesetzt > 0,
                    kompakt = true,
                )
            }
            SehrLeise(if (gesetzt == 0) "Noch kein Ton gesetzt" else "$gesetzt Töne · ein Durchlauf $sekunden s")

            // Sichern, Regal und Code — der Teil, der aus einem Versuch einen Ton macht.
            Feld(
                wert = name,
                beiAenderung = { name = it.take(Eigentoene.NAME_LAENGE) },
                etikett = "Name",
                platzhalter = "z. B. Nachtwache",
                aktiv = premium,
            )
            Pillenreihe {
                Knopf(
                    if (bearbeitet != null) "Änderung sichern" else "Ton sichern",
                    beiDruck = { sichern(alsNeu = false) },
                    art = Knopfart.Haupt,
                    aktiv = premium && gesetzt > 0 && !(voll && bearbeitet == null),
                )
                if (bearbeitet != null) {
                    Knopf(
                        "Als neuen sichern",
                        beiDruck = { sichern(alsNeu = true) },
                        art = Knopfart.Leise,
                        aktiv = premium && gesetzt > 0 && !voll,
                        kompakt = true,
                    )
                }
            }

            // Das Regal steht auch leer da, mit seinem Satz: Dass hier nichts liegt, ist eine Auskunft.
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Etikett("Deine Töne")
                SehrLeise("${toene.size} von ${Eigentoene.HOECHSTZAHL}")
            }
            if (toene.isEmpty()) {
                SehrLeise(
                    "Noch keiner gesichert. Was du sicherst, steht danach in der Tonwahl zwischen den anderen — " +
                        "und lässt sich dort auch wieder wegwerfen.",
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    toene.forEach { t ->
                        Regalzeile(
                            name = t.name,
                            offen = bearbeitet == t.id,
                            eingestellt = eingestellt == "eigen:${t.id}",
                            aktiv = premium,
                            beiOeffnen = { oeffnen(t.id) },
                            beiWeg = { wegwerfen(t.id) },
                        )
                    }
                }
            }

            // Der Code — die Antwort auf die Frage, die man erst hat, wenn etwas im Regal liegt.
            Etikett("Code zum Weitergeben")
            SehrLeise(
                "Sechzehn Zeichen, die dieses Raster beschreiben — schick sie jemandem, und er hat denselben " +
                    "Ton. Groß- und Kleinschreibung ist egal.",
            )
            Text(code, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
            Pillenreihe {
                Knopf(
                    "Kopieren",
                    beiDruck = {
                        zwischenablage.setText(AnnotatedString(code))
                        sagen("Code kopiert — gib ihn weiter.")
                    },
                    aktiv = premium,
                    kompakt = true,
                )
                Knopf(
                    "Code einlesen",
                    beiDruck = { einfuegen = !einfuegen },
                    art = Knopfart.Leise,
                    aktiv = premium,
                    kompakt = true,
                )
            }
            if (einfuegen) {
                Feld(
                    wert = codeEingabe,
                    beiAenderung = { codeEingabe = it },
                    platzhalter = "Code hier einfügen",
                    stil = Schrift.MonoNormal,
                    aktiv = premium,
                )
                Knopf("Übernehmen", ::codeUebernehmen, aktiv = premium, kompakt = true)
            }
        }

        meldung?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }
    }
}

/**
 * Das Raster: oben hoch, unten tief — der Katalog zählt aufsteigend, ein Bild nicht.
 * Nach jedem Viertel ein Strich, und was hinter dem letzten benutzten Viertel liegt,
 * steht schattiert da: Es klingt nicht mehr mit.
 */
@Composable
private fun Raster(
    plan: Tonbauplan,
    laenge: Int,
    zeiger: Int,
    aktiv: Boolean,
    beiFeld: (Int, Int) -> Unit,
) {
    val feld = RoundedCornerShape(3.dp)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        Eigentoene.LEITER.reversed().forEach { reihe ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    reihe.name,
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                    maxLines = 1,
                    modifier = Modifier.width(64.dp),
                )
                for (schritt in 1..Eigentoene.SCHRITTE) {
                    val an = plan.schritte.getOrNull(schritt - 1) == reihe.hertz
                    val stumm = schritt > laenge
                    val grund = when {
                        an -> Farben.Amber
                        zeiger == schritt - 1 -> Farben.FlaecheAktiv
                        else -> Farben.FlaecheHoch
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .padding(end = if (schritt % Eigentoene.GRUPPE == 0 && schritt < Eigentoene.SCHRITTE) 4.dp else 1.dp)
                            .background(if (stumm && !an) grund.copy(alpha = 0.4f) else grund, feld)
                            .border(1.dp, if (zeiger == schritt - 1) Farben.AmberHell else Farben.Rand, feld)
                            .clickable(
                                enabled = aktiv,
                                onClick = { beiFeld(schritt - 1, reihe.hertz) },
                                role = Role.Checkbox,
                            )
                            .semantics { contentDescription = "Schritt $schritt, Höhe ${reihe.name}" },
                    )
                }
            }
        }
        // Die Schrittnummern — mit derselben leeren Zelle links, damit die Spalten übereinanderstehen.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.width(64.dp))
            for (schritt in 1..Eigentoene.SCHRITTE) {
                Text(
                    "$schritt",
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontSize = Schrift.WINZIG * 0.8f),
                    color = when {
                        zeiger == schritt - 1 -> Farben.Amber
                        schritt > laenge -> Farben.TextSehrLeise.copy(alpha = 0.5f)
                        else -> Farben.TextSehrLeise
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (schritt % Eigentoene.GRUPPE == 0 && schritt < Eigentoene.SCHRITTE) 4.dp else 1.dp),
                )
            }
        }
    }
}

/** Eine Zeile im Regal — der Name öffnet, das Kreuz wirft weg. */
@Composable
internal fun Regalzeile(
    name: String,
    offen: Boolean,
    eingestellt: Boolean,
    aktiv: Boolean,
    beiOeffnen: () -> Unit,
    beiWeg: () -> Unit,
    zusatz: String? = null,
    knoepfe: @Composable () -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (offen) Farben.HauchAmber else Farben.FlaecheHoch, Rundung.Klein)
            .border(1.dp, if (offen) Farben.Amber else Farben.Rand, Rundung.Klein)
            .padding(start = Abstand.Normal),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = aktiv, onClick = beiOeffnen, role = Role.Button)
                .padding(vertical = Abstand.Klein),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text, maxLines = 1)
                if (eingestellt) {
                    Text(
                        "eingestellt",
                        style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Amber,
                    )
                }
            }
            if (zusatz != null) SehrLeise(zusatz)
        }
        knoepfe()
        Zeichenknopf(beiDruck = beiWeg, beschreibung = "$name wegwerfen", aktiv = aktiv, kompakt = true) {
            Text("✕", style = Schrift.Klein, color = Farben.TextSehrLeise)
        }
    }
}
