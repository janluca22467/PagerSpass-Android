package de.pagerspass.pagerspass.ansichten.melder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.pagerspass.pagerspass.ansichten.welt.Regler
import de.pagerspass.pagerspass.melder.EigenerKlang
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.melder.Melderspieler
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Die Klangwerkstatt — `components/ui/Klangwerkstatt.vue` mit `audio/melderklaenge.ts`.
 *
 * <b>Was mit Oszillatoren nicht geht:</b> die Sirene vom eigenen Gerätehaus, der
 * Melder, den man wirklich am Gürtel trägt, die Stimme, die „Alarm für die
 * Drehleiter" sagt. Eine Datei vom Gerät oder eine Aufnahme vom Mikrofon.
 *
 * <b>Die Datei verlässt das Gerät nie.</b> Sie liegt im privaten Speicher der
 * App (`filesDir`), nicht am Konto — dieselbe Zusage wie im Browser. Es gibt
 * keine Ablage auf dem Server, keine Frist, kein Löschbegehren.
 */
@Composable
fun Klangwerkstatt(modifier: Modifier = Modifier, unterrand: Dp = 0.dp, beiZurueck: () -> Unit) {
    val zusammenhang = LocalContext.current
    val geraet = remember { Meldergeraet.bereit(zusammenhang) }
    val bereich = rememberCoroutineScope()
    var meldung by remember { mutableStateOf<String?>(null) }
    var aufnahme by remember { mutableStateOf<Aufnahme?>(null) }
    var sekunden by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            Melderspieler.stoppen("probe")
            aufnahme?.abbrechen()
        }
    }

    val datei = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        bereich.launch {
            meldung = withContext(Dispatchers.IO) { dateiEinlesen(zusammenhang, uri) }
        }
    }

    fun aufnahmeStarten() {
        val neu = Aufnahme(zusammenhang)
        if (neu.starten()) {
            aufnahme = neu
            sekunden = 0
            meldung = null
        } else {
            meldung = "Das Mikrofon lässt sich gerade nicht öffnen."
        }
    }

    fun aufnahmeBeenden() {
        val a = aufnahme ?: return
        aufnahme = null
        bereich.launch {
            meldung = withContext(Dispatchers.IO) {
                val f = a.beenden() ?: return@withContext "Die Aufnahme ist leer geblieben."
                val bytes = f.readBytes()
                val dauer = laenge(f.absolutePath, zusammenhang, null)
                f.delete()
                val satz = geraet.klangAnlegen("Eingesprochen", bytes, "audio/mp4", "aufnahme", dauer)
                if (satz == null) "Kein Platz mehr — höchstens ${Meldergeraet.KLANG_HOECHSTZAHL} Klänge." else "„${satz.name}“ ist da."
            }
        }
    }

    val mikrofon = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { erlaubt: Boolean ->
        if (erlaubt) aufnahmeStarten() else meldung = "Ohne Mikrofon keine Aufnahme — eine Datei geht trotzdem."
    }

    // Die Aufnahme endet von selbst am Deckel — ein Melder spielt kein Lied ab.
    LaunchedEffect(aufnahme) {
        while (aufnahme != null) {
            delay(1_000)
            sekunden++
            if (sekunden >= Meldergeraet.KLANG_HOECHSTDAUER.toInt()) aufnahmeBeenden()
        }
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Eigene Klänge",
            unterzeile = "Datei oder Aufnahme — bleibt auf diesem Gerät",
            knoepfe = { Knopf("Fertig", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Abschnitt("Neuer Klang") {
            SehrLeise(
                "Bis ${Meldergeraet.KLANG_HOECHSTBYTES / (1024 * 1024)} MB je Datei, höchstens " +
                    "${Meldergeraet.KLANG_HOECHSTZAHL} Klänge. Hörbar sind bis zu " +
                    "${Meldergeraet.KLANG_HOECHSTDAUER.toInt()} Sekunden — den Ausschnitt wählst du danach.",
            )
            val voll = geraet.eigeneKlaenge.size >= Meldergeraet.KLANG_HOECHSTZAHL
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Datei wählen", { datei.launch("audio/*") }, kompakt = true, aktiv = !voll && aufnahme == null)
                if (aufnahme == null) {
                    Knopf("● Aufnehmen", {
                        val darf = ContextCompat.checkSelfPermission(zusammenhang, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        if (darf) aufnahmeStarten() else mikrofon.launch(Manifest.permission.RECORD_AUDIO)
                    }, kompakt = true, aktiv = !voll)
                } else {
                    Knopf("■ Stopp ($sekunden s)", ::aufnahmeBeenden, kompakt = true, art = Knopfart.Alarm)
                }
            }
            meldung?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }
        }

        Abschnitt("Deine Klänge (${geraet.eigeneKlaenge.size}/${Meldergeraet.KLANG_HOECHSTZAHL})") {
            if (geraet.eigeneKlaenge.isEmpty()) SehrLeise("Noch keiner da.")
            geraet.eigeneKlaenge.forEach { k -> Klangzeile(k, zusammenhang, geraet) }
        }
    }
}

@Composable
private fun Klangzeile(k: EigenerKlang, zusammenhang: Context, geraet: Meldergeraet) {
    var name by remember(k.id) { mutableStateOf(k.name) }
    val laenge = if (k.laenge > 0) k.laenge else Meldergeraet.KLANG_HOECHSTDAUER
    val zehntel = (laenge * 10).toInt().coerceAtLeast(1)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (k.herkunft == "aufnahme") "🎙 " else "♪ ") + k.name,
                style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                color = Farben.Text,
                modifier = Modifier.weight(1f),
            )
            if (geraet.ton == "klang:${k.id}") Text("GEWÄHLT", style = Schrift.Winzig, color = Farben.Amber)
        }
        Text(
            "${sek(laenge)} lang · ${k.groesse / 1024} KB · hörbar ${sek(k.von)}–${sek(k.bis)}, Ruhe ${sek(k.pause)}",
            style = Schrift.Winzig,
            color = Farben.TextSehrLeise,
        )
        Feld(wert = name, beiAenderung = { name = it.take(24) }, etikett = "Name")
        if (name != k.name && name.isNotBlank()) {
            Knopf("Namen übernehmen", { geraet.klangAendern(k.copy(name = name)) }, kompakt = true, art = Knopfart.Leise)
        }
        Text("Anfang", style = Schrift.Winzig, color = Farben.TextLeise)
        Regler((k.von * 10).toInt(), 0..zehntel, 1, { geraet.klangAendern(k.copy(von = it / 10.0, bis = maxOf(k.bis, it / 10.0 + 0.1))) })
        Text("Ende", style = Schrift.Winzig, color = Farben.TextLeise)
        Regler((k.bis * 10).toInt(), 0..zehntel, 1, { geraet.klangAendern(k.copy(bis = it / 10.0)) })
        Text("Ruhe bis zur Wiederholung", style = Schrift.Winzig, color = Farben.TextLeise)
        Regler((k.pause * 10).toInt(), 0..(Meldergeraet.KLANG_HOECHSTPAUSE * 10).toInt(), 1, { geraet.klangAendern(k.copy(pause = it / 10.0)) })
        Pillenreihe {
            Pille("▶ Probe", an = false, beiDruck = { Melderspieler.stoppen("probe"); Melderspieler.probe(zusammenhang, "klang:${k.id}") })
            Pille("Wählen", an = geraet.ton == "klang:${k.id}", beiDruck = { geraet.tonSetzen("klang:${k.id}") })
            Pille("Löschen", an = false, farbe = Farben.Signal, beiDruck = { geraet.klangLoeschen(k.id) })
        }
    }
}

private fun sek(wert: Double) = String.format(Locale.GERMANY, "%.1f s", wert)

/** Die Länge einer Tondatei in Sekunden — 0, wenn das Gerät sie nicht lesen kann. */
private fun laenge(pfad: String?, zusammenhang: Context, uri: Uri?): Double = runCatching {
    val leser = MediaMetadataRetriever()
    try {
        if (uri != null) leser.setDataSource(zusammenhang, uri) else leser.setDataSource(pfad)
        (leser.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000.0
    } finally {
        runCatching { leser.release() }
    }
}.getOrDefault(0.0)

/** Eine gewählte Datei einlesen — die Antwort ist der Satz für den Nutzer. */
private fun dateiEinlesen(zusammenhang: Context, uri: Uri): String {
    val aufloeser = zusammenhang.contentResolver
    val typ = aufloeser.getType(uri) ?: "audio/*"
    if (!typ.startsWith("audio/")) return "Das ist keine Tondatei."
    var name = "Eigene Datei"
    runCatching {
        aufloeser.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) name = c.getString(0)?.substringBeforeLast('.') ?: name
        }
    }
    val bytes = runCatching {
        aufloeser.openInputStream(uri)?.use { strom ->
            val puffer = ByteArray((Meldergeraet.KLANG_HOECHSTBYTES + 1).toInt())
            var gelesen = 0
            while (gelesen < puffer.size) {
                val n = strom.read(puffer, gelesen, puffer.size - gelesen)
                if (n < 0) break
                gelesen += n
            }
            puffer.copyOf(gelesen)
        }
    }.getOrNull() ?: return "Die Datei ließ sich nicht lesen."
    if (bytes.size > Meldergeraet.KLANG_HOECHSTBYTES) {
        return "Die Datei ist größer als ${Meldergeraet.KLANG_HOECHSTBYTES / (1024 * 1024)} MB."
    }
    val dauer = laenge(null, zusammenhang, uri)
    if (dauer <= 0.0) return "Diese Datei kann das Gerät nicht abspielen."
    val satz = Meldergeraet.klangAnlegen(name, bytes, typ, "datei", dauer)
        ?: return "Kein Platz mehr — höchstens ${Meldergeraet.KLANG_HOECHSTZAHL} Klänge."
    return "„${satz.name}“ ist da."
}

/**
 * Eine laufende Mikrofonaufnahme — AAC in MP4, in den Zwischenspeicher der App.
 * Erst beim Beenden wird daraus ein Klang; abgebrochen bleibt nichts liegen.
 */
private class Aufnahme(private val zusammenhang: Context) {
    private val ziel = File(zusammenhang.cacheDir, "aufnahme-${System.currentTimeMillis()}.m4a")
    private var rekorder: MediaRecorder? = null

    fun starten(): Boolean = runCatching {
        val r = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(zusammenhang)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(44_100)
        r.setAudioEncodingBitRate(96_000)
        r.setOutputFile(ziel.absolutePath)
        r.prepare()
        r.start()
        rekorder = r
    }.isSuccess

    fun beenden(): File? {
        val r = rekorder ?: return null
        rekorder = null
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        return ziel.takeIf { ok && it.exists() && it.length() > 0 }
    }

    fun abbrechen() {
        beenden()
        ziel.delete()
    }
}
