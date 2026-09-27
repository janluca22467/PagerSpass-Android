package de.pagerspass.pagerspass.ansichten

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.pagerspass.pagerspass.mobil.Begleiterstand
import de.pagerspass.pagerspass.netz.Einzelruf
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Regler
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Was der Begleiter über die Grundgriffe hinaus kann — Einzelruf und Tonregler.
 *
 * <b>Gebündelt, damit der Rahmen eine Zeile hat und nicht zehn.</b> Die Griffe
 * gehören alle demselben ViewModel (`Begleiter`); `PagerSpassApp` reicht sie mit
 * `begleiter.griffe()` durch.
 */
data class BegleiterGriffe(
    val einzelrufStarten: (zielVehicleId: String?) -> Unit = {},
    val einzelrufAnnehmen: (rufId: String) -> Unit = {},
    val einzelrufAbweisen: (rufId: String) -> Unit = {},
    val einzelrufBeenden: (rufId: String) -> Unit = {},
    val einzelrufSagen: (rufId: String, text: String) -> Unit = { _, _ -> },
    /** Die rastende Sprechtaste im Bot-Gespräch — gibt zurück, ob das Mikrofon aufging. */
    val einzelrufSprechenStarten: () -> Boolean = { false },
    val einzelrufSprechenBeenden: (rufId: String) -> Unit = {},
    val einzelrufZulassen: (Boolean) -> Unit = {},
    val tonSetzen: (funk: Float?, melder: Float?, stumm: Boolean?) -> Unit = { _, _, _ -> },
)

/**
 * Die Einzelruf-Leiste — übertragen aus `components/funk/EinzelrufLeiste.vue`.
 *
 * Sie kennt die drei Lagen eines Telefonats und zeigt immer genau eine: <b>es
 * klingelt bei mir</b> (grün nimmt an, rot weist ab), <b>ich rufe</b> (rot bricht
 * ab), <b>das Gespräch läuft</b> (Gegenstelle und Dauer, rot legt auf). Ein
 * laufendes Gespräch verdeckt jedes Klingeln — besetzt ist besetzt.
 *
 * <b>Sie schwebt über allem</b>, auch über einem anderen Reiter: Wer gerade im
 * Funkchat liest, muss das Klingeln trotzdem annehmen können.
 *
 * <b>Das Mikrofon braucht eine Erlaubnis</b> — sie wird beim Annehmen erfragt,
 * nicht vorher: Wer nie telefoniert, wird nie gefragt.
 */
@Composable
fun Einzelrufleiste(
    stand: Begleiterstand,
    griffe: BegleiterGriffe,
    modifier: Modifier = Modifier,
) {
    val laufend = stand.laufenderEinzelruf
    val eingehend = stand.eingehenderEinzelruf
    val ausgehend = stand.ausgehenderEinzelruf

    Einzelruftoene(stand)

    val mitMikrofon = rememberMikrofonfrage()

    val rahmen = modifier
        .widthIn(max = 560.dp)
        .fillMaxWidth()
        .background(Farben.FlaecheHoch, Rundung.Normal)
        .border(1.dp, Farben.RandHell, Rundung.Normal)
        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein)

    when {
        laufend != null -> Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = rahmen,
        ) {
            if (laufend.mitBot) {
                Botgespraech(stand, laufend, griffe, mitMikrofon = mitMikrofon)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Punkt(Farben.GruenHell)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (laufend.vonPlayerId == stand.spielerId) laufend.zielName else laufend.vonName,
                        style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Gespraechsdauer(laufend.angenommenUm)
                }
                Knopf("Auflegen", { griffe.einzelrufBeenden(laufend.id) }, art = Knopfart.Alarm, kompakt = true)
            }
        }

        eingehend != null -> Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = rahmen,
        ) {
            Punkt(Farben.Amber)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eingehend.vonName,
                    style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SehrLeise("Einzelruf")
            }
            Knopf(
                "Annehmen",
                { mitMikrofon { griffe.einzelrufAnnehmen(eingehend.id) } },
                art = Knopfart.Haupt,
                kompakt = true,
            )
            Knopf("Abweisen", { griffe.einzelrufAbweisen(eingehend.id) }, art = Knopfart.Alarm, kompakt = true)
        }

        ausgehend != null -> Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = rahmen,
        ) {
            Punkt(Farben.BlauHell)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ausgehend.zielName,
                    style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SehrLeise("wird gerufen …")
            }
            Knopf("Abbrechen", { griffe.einzelrufBeenden(ausgehend.id) }, art = Knopfart.Alarm, kompakt = true)
        }
    }
}

/**
 * Das Gespräch mit einer Bot-Besatzung — in Zeilen statt mit Stimme.
 *
 * Sie hört zu und antwortet; sie hat nur keine Stimme auf der Leitung. Was man
 * sagt, schreibt der Server mit (die rastende Taste), tippen geht genauso.
 */
@Composable
private fun Botgespraech(
    stand: Begleiterstand,
    ruf: Einzelruf,
    griffe: BegleiterGriffe,
    mitMikrofon: (() -> Unit) -> Unit,
) {
    var eingabe by remember { mutableStateOf("") }
    val zeilen = ruf.verlauf.orEmpty()
    val rollen = rememberScrollState()

    // Neue Zeilen kommen unten an — dorthin sieht man beim Telefonieren.
    LaunchedEffect(zeilen.size) { rollen.animateScrollTo(rollen.maxValue) }

    // Aufgelegt heißt: Das Mikrofon ist zu, egal wer aufgelegt hat — das räumt
    // das ViewModel beim Ende des Gesprächs selbst weg (`einzelrufAufraeumen`).
    val hoert = stand.einzelrufHoert

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 180.dp)
            .background(Farben.BgTief, Rundung.Klein)
            .verticalScroll(rollen)
            .padding(Abstand.Klein),
    ) {
        if (zeilen.isEmpty()) {
            SehrLeise("Verbunden. Sprich oder schreib — die Besatzung hört mit.", mono = true)
        }
        zeilen.forEach { z ->
            Text(
                text = "${z.vonName}: ${z.text}",
                style = Schrift.Klein,
                color = if (z.vonPlayerId == stand.spielerId) Farben.AmberHell else Farben.Text,
            )
        }
        if (stand.einzelrufErkennung) SehrLeise("Wird verstanden …")
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Die Sprechtaste rastet ein: Vollduplex heißt, dass man redet, bis man
        // aufhört — nicht, solange man einen Knopf hält.
        Knopf(
            if (hoert) "Hört zu …" else "Sprechen",
            {
                if (hoert) {
                    griffe.einzelrufSprechenBeenden(ruf.id)
                } else {
                    mitMikrofon { griffe.einzelrufSprechenStarten() }
                }
            },
            art = if (hoert) Knopfart.Haupt else Knopfart.Normal,
            kompakt = true,
        )
        Feld(
            wert = eingabe,
            beiAenderung = { eingabe = it.take(500) },
            platzhalter = "Sagen …",
            weiterTaste = ImeAction.Send,
            modifier = Modifier.weight(1f),
        )
        Knopf(
            "Senden",
            {
                griffe.einzelrufSagen(ruf.id, eingabe.trim())
                eingabe = ""
            },
            aktiv = eingabe.isNotBlank(),
            kompakt = true,
        )
    }
}

/**
 * Erst das Mikrofon, dann der Handgriff — die Erlaubnis wird im Augenblick
 * erfragt, in dem sie gebraucht wird: beim Annehmen, beim Rufen, beim Sprechen.
 *
 * <b>Auch ohne Erlaubnis geht es weiter.</b> Hören geht trotzdem, und die
 * Gegenstelle soll nicht ins Leere klingeln; das ViewModel sagt dann, dass das
 * Mikrofon nicht zu bekommen war.
 */
@Composable
fun rememberMikrofonfrage(): (() -> Unit) -> Unit {
    val zusammenhang = LocalContext.current
    var nachErlaubnis by remember { mutableStateOf<(() -> Unit)?>(null) }
    val frage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        nachErlaubnis?.invoke()
        nachErlaubnis = null
    }

    return { tun ->
        val da = ContextCompat.checkSelfPermission(zusammenhang, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (da) {
            tun()
        } else {
            nachErlaubnis = tun
            frage.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

/** Die Gesprächsdauer, sekündlich — mm:ss ab der Annahme. */
@Composable
private fun Gespraechsdauer(angenommenUm: String?) {
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(angenommenUm) {
        while (isActive) {
            jetzt = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val beginn = angenommenUm?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    val sekunden = if (beginn == null) 0L else ((jetzt - beginn) / 1000).coerceAtLeast(0)
    Text(
        text = "${sekunden / 60}:${(sekunden % 60).toString().padStart(2, '0')}",
        style = Schrift.MonoKlein,
        color = Farben.TextLeise,
    )
}

@Composable
private fun Punkt(farbe: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(10.dp).background(farbe, CircleShape))
}

/**
 * Die Zeichen des Einzelrufs — es klingelt anders als das Telefon.
 *
 * Solange es bei mir klingelt, klingelt es hörbar; solange ich rufe, läuft der
 * Freiton; endet ein Ruf, kommt das Ende-Zeichen. Am Zustand aufgehängt und nicht
 * an den Knöpfen: Dann hört es beim Annehmen wie beim Verfallen auf. Der
 * Funkregler (und „stumm") gilt auch hier.
 */
@Composable
private fun Einzelruftoene(stand: Begleiterstand) {
    val pegel = if (stand.stumm) 0 else (stand.tonFunk * 100).roundToInt().coerceIn(0, 100)
    val klingelt = stand.einzelrufHier && stand.eingehenderEinzelruf != null
    val ruft = stand.einzelrufHier && stand.ausgehenderEinzelruf != null
    val spricht = stand.eigenerEinzelruf?.id

    LaunchedEffect(klingelt, ruft, pegel) {
        if ((!klingelt && !ruft) || pegel == 0) return@LaunchedEffect
        val ton = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, pegel) }.getOrNull()
            ?: return@LaunchedEffect
        try {
            while (isActive) {
                if (klingelt) {
                    repeat(2) {
                        ton.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                        delay(260)
                    }
                    delay(1_400)
                } else {
                    ton.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1_000)
                    delay(4_000)
                }
            }
        } finally {
            ton.release()
        }
    }

    // Das Ende eines eigenen Gesprächs — dieselbe Auskunft für alle drei Fälle
    // (aufgelegt, von der Gegenstelle beendet, abgerissen): Niemand hört mehr zu.
    var vorher by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(spricht) {
        val war = vorher
        vorher = spricht
        if (war != null && spricht == null && pegel > 0) {
            val ton = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, pegel) }.getOrNull()
            ton?.startTone(ToneGenerator.TONE_PROP_NACK, 300)
            delay(400)
            ton?.release()
        }
    }
}

/**
 * Der Tonregler — übertragen aus `components/ui/Tonregler.vue`.
 *
 * Was man <b>während</b> einer Schicht wirklich anfasst: lauter, leiser, still.
 * Der Funkregler trägt auch den Einzelruf; der Ruhe-Schalter des Einzelrufs liegt
 * am Server, weil die Vermittlung ihn prüfen muss.
 */
@Composable
fun Tonreglerblende(
    stand: Begleiterstand,
    griffe: BegleiterGriffe,
    beiSchliessen: () -> Unit,
) {
    Blende(
        titel = "Toneinstellungen",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = { Knopf("Fertig", beiSchliessen, art = Knopfart.Haupt) },
    ) {
        Schalterzeile(
            titel = "Alles außer dem Melder stumm",
            an = stand.stumm,
            beiWechsel = { griffe.tonSetzen(null, null, it) },
        )

        Etikett("Lautstärke")
        Regler(
            wert = (stand.tonMelder * 100).roundToInt(),
            beiAenderung = { griffe.tonSetzen(null, it / 100f, null) },
            von = 0,
            bis = 100,
            etikett = "Melder",
        )
        Regler(
            wert = (stand.tonFunk * 100).roundToInt(),
            beiAenderung = { griffe.tonSetzen(it / 100f, null, null) },
            von = 0,
            bis = 100,
            etikett = "Funk",
            aktiv = !stand.stumm,
        )
        SehrLeise("Stimmen und Gerätetöne des Funkgeräts — auch das Klingeln des Einzelrufs.")

        Hakenzeile(
            "Einzelrufe von Fahrzeugen annehmen",
            stand.einzelrufZulassen,
            { griffe.einzelrufZulassen(it) },
        )
        SehrLeise("Der Ruhe-Schalter des eigenen Platzes — die Leitstelle kommt immer durch.")
    }
}
