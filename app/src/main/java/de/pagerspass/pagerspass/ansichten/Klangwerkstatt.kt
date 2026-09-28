package de.pagerspass.pagerspass.ansichten

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.EigenerKlang
import de.pagerspass.pagerspass.mobil.Eigentoene
import de.pagerspass.pagerspass.mobil.Klangaufnahme
import de.pagerspass.pagerspass.mobil.rememberAblage
import de.pagerspass.pagerspass.mobil.rememberMelderTon
import de.pagerspass.pagerspass.mobil.rememberTonprobe
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Regler
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Die Klangwerkstatt — der Melderton, der nicht gerechnet wird.
 *
 * Übertragen aus `web/src/components/ui/Klangwerkstatt.vue`. Die Tonwerkstatt zeichnet
 * Muster aus Schwingungen; die Sirene vom eigenen Gerätehaus, der Melder, den man im
 * Dienst wirklich trägt, oder eine Stimme sind damit nicht zu treffen. Hier kommt ein
 * Klang herein — als Datei aus der Dokumentauswahl des Systems oder über das Mikrofon.
 *
 * <b>Zwei Wege hinein, und beide enden an derselben Stelle:</b> ein Stück Ton mit
 * Namen, Schnitt und Ruhe. Was danach kommt — schneiden, anhören, einstellen,
 * wegwerfen — ist für beide gleich.
 *
 * <b>Was sie ausdrücklich nicht kann:</b> keine Lautstärke (der Klang wird beim
 * Einlesen auf die gehörte Lautstärke der Katalogtöne gebracht) und kein Code zum
 * Weitergeben — <b>die Datei verlässt dieses Gerät nicht.</b>
 */
@Composable
fun Klangwerkstatt(premium: Boolean, beiSchliessen: () -> Unit) {
    val zusammenhang = LocalContext.current
    remember(zusammenhang) { Eigentoene.sicherstellen(zusammenhang) }
    val probe = rememberTonprobe()
    val ablage = rememberAblage()
    val bereich = rememberCoroutineScope()
    val eingestellt by rememberMelderTon()
    val mitMikrofon = rememberMikrofonfrage()

    val klaenge = Eigentoene.klaenge
    val voll = klaenge.size >= Eigentoene.KLANG_HOECHSTZAHL
    var laedt by remember { mutableStateOf(false) }

    // Die Statuszeile: Fast alles hier kann aus Gründen scheitern, die man dem Bild
    // nicht ansieht — ohne einen Satz dazu liest sich „nichts passiert" wie ein kaputter Knopf.
    var meldung by remember { mutableStateOf("") }
    var meldungLauf by remember { mutableIntStateOf(0) }
    LaunchedEffect(meldungLauf) {
        if (meldungLauf == 0) return@LaunchedEffect
        delay(6000)
        meldung = ""
    }
    fun sagen(text: String) {
        meldung = text
        meldungLauf += 1
    }

    // Welcher Klang auf der Werkbank liegt — `null` heißt: nur das Regal.
    var offen by remember { mutableStateOf<String?>(null) }
    var umriss by remember { mutableStateOf<List<Float>>(emptyList()) }
    var wegwerffrage by remember { mutableStateOf<String?>(null) }

    /*
     * Die Kurve wird geholt, wenn ein anderer Klang aufgeschlagen wird — und nur dann.
     * Das Verschieben der Marken ändert an ihr nichts.
     */
    LaunchedEffect(offen) {
        umriss = emptyList()
        val id = offen ?: return@LaunchedEffect
        val neu = Eigentoene.umrissVon(id)
        if (neu != null && offen == id) umriss = neu
    }

    // ------------------------------------------------------ Der gemeinsame Weg

    /**
     * Was beide Wege am Ende tun: vermessen, anlegen, aufschlagen. Das Vermessen ist
     * zugleich die ehrliche Formatprüfung — ob eine Datei lesbar ist, weiß nur das Gerät.
     */
    fun aufnehmen(name: String, bytes: ByteArray, typ: String, herkunft: String) {
        if (Eigentoene.klaenge.size >= Eigentoene.KLANG_HOECHSTZAHL) {
            sagen("Das Regal ist voll — ${Eigentoene.KLANG_HOECHSTZAHL} Klänge. Wirf einen weg, dann geht der nächste.")
            return
        }
        laedt = true
        bereich.launch {
            try {
                val gemessen = Eigentoene.vermessen(bytes)
                if (gemessen == null) {
                    sagen("Diese Datei kann das Gerät nicht lesen. MP3, WAV, OGG oder M4A gehen zuverlässig.")
                    return@launch
                }
                val satz = Eigentoene.klangAnlegen(name, bytes, typ, herkunft, gemessen.laenge)
                if (satz == null) {
                    sagen("Konnte nicht gesichert werden — vermutlich ist der Speicher dieses Geräts voll.")
                    return@launch
                }
                offen = satz.id
                umriss = gemessen.umriss
                sagen(
                    if (gemessen.laenge > Eigentoene.KLANG_HOECHSTDAUER) {
                        "Übernommen — und auf ${Eigentoene.KLANG_HOECHSTDAUER.toInt()} Sekunden geschnitten. " +
                            "Schieb den Ausschnitt dahin, wo er hingehört."
                    } else {
                        "Übernommen. Hör ihn dir an und stell ihn ein, wenn er sitzt."
                    },
                )
            } finally {
                laedt = false
            }
        }
    }

    // ------------------------------------------------------------ Datei laden

    val dateiwahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { quelle ->
        if (quelle == null) return@rememberLauncherForActivityResult
        bereich.launch {
            val datei = withContext(Dispatchers.IO) { runCatching { dateiLesen(zusammenhang, quelle) }.getOrNull() }
            when {
                datei == null -> sagen("Diese Datei kann das Gerät nicht lesen. MP3, WAV, OGG oder M4A gehen zuverlässig.")
                datei.bytes == null -> sagen(
                    "${masse(datei.groesse)} sind zu viel — höchstens ${masse(Eigentoene.KLANG_HOECHSTBYTES.toLong())}. " +
                        "Ein Melderton ist ein paar Sekunden lang, kein Lied.",
                )
                else -> aufnehmen(datei.name, datei.bytes, datei.typ, "datei")
            }
        }
    }

    // ------------------------------------------------------------ Einsprechen

    val aufnahme = remember { Klangaufnahme() }
    var nimmtAuf by remember { mutableStateOf(false) }
    var aufnahmeSekunden by remember { mutableIntStateOf(0) }

    fun namensvorschlag(): String =
        "Eingesprochen ${Eigentoene.klaenge.count { it.herkunft == "aufnahme" } + 1}"

    fun aufnahmeBeenden() {
        if (!nimmtAuf) return
        nimmtAuf = false
        bereich.launch {
            val bytes = withContext(Dispatchers.IO) { aufnahme.beenden() }
            if (bytes == null) {
                sagen("Die Aufnahme ist leer geblieben. Versuch es noch einmal.")
                return@launch
            }
            aufnehmen(namensvorschlag(), bytes, "audio/wav", "aufnahme")
        }
    }

    /** Abbrechen heißt wegwerfen — wer die Werkstatt schließt, sichert nichts versehentlich. */
    fun aufnahmeAbbrechen() {
        if (!nimmtAuf) return
        nimmtAuf = false
        bereich.launch(Dispatchers.IO) { aufnahme.verwerfen() }
    }

    fun aufnahmeStarten() {
        probe.beenden()
        if (nimmtAuf || Eigentoene.klaenge.size >= Eigentoene.KLANG_HOECHSTZAHL) return
        // Die Erlaubnis wird im Augenblick erfragt, in dem sie gebraucht wird.
        mitMikrofon {
            if (aufnahme.starten()) {
                aufnahmeSekunden = 0
                nimmtAuf = true
            } else {
                sagen(
                    "Kein Zugriff aufs Mikrofon. Das Gerät fragt beim ersten Mal nach — wer abgelehnt hat, " +
                        "gibt es in den App-Einstellungen frei.",
                )
            }
        }
    }

    // Von selbst Schluss machen: Ein vergessener Aufnahmeknopf liefe sonst weiter.
    LaunchedEffect(nimmtAuf) {
        while (nimmtAuf) {
            delay(1000)
            aufnahmeSekunden += 1
            if (aufnahmeSekunden >= Eigentoene.AUFNAHME_HOECHSTDAUER) aufnahmeBeenden()
        }
    }

    // Das Mikrofon wieder loslassen, wenn die Werkstatt geht — sonst hörte das Spiel scheinbar weiter mit.
    DisposableEffect(aufnahme) { onDispose { aufnahme.verwerfen() } }

    fun schliessen() {
        probe.beenden()
        aufnahmeAbbrechen()
        beiSchliessen()
    }

    // --------------------------------------------------------------- Das Regal

    fun wegwerfen(id: String) {
        probe.beenden()
        wegwerffrage = null
        if (offen == id) offen = null
        Eigentoene.klangLoeschen(id)
        sagen("Weggeworfen.")
    }

    Blende(
        titel = "Klangwerkstatt",
        beiSchliessen = ::schliessen,
        breite = Dialogbreite.Breit,
        kopfknoepfe = { Knopf("Schließen", ::schliessen, art = Knopfart.Leise, kompakt = true) },
        fuss = {
            SehrLeise(
                "Deine Klänge liegen an diesem Gerät und verlassen es nicht — am Rechner andere als am Handy, " +
                    "wenn du magst.",
            )
            Knopf("Fertig", ::schliessen, art = Knopfart.Haupt)
        },
    ) {
        Text(
            "PAGERSPASS · WERKSTATT",
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
            color = Farben.Amber,
        )
        SehrLeise(
            "Lade eine Tondatei hoch oder sprich deinen Alarm selbst ein. Beides bleibt auf diesem Gerät — es " +
                "wird nirgendwohin geschickt. Dein Klang ist nicht lauter als die anderen Töne, nur deiner.",
        )

        // Ohne Abo steht die ganze Werkbank trotzdem da, nur gesperrt.
        val wegeFrei = premium && !laedt
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier.alpha(if (wegeFrei) 1f else 0.45f),
        ) {
            Weg("Datei hochladen") {
                SehrLeise(
                    "MP3, WAV, OGG oder M4A, höchstens ${masse(Eigentoene.KLANG_HOECHSTBYTES.toLong())}. Was länger " +
                        "ist als ${Eigentoene.KLANG_HOECHSTDAUER.toInt()} Sekunden, schneidest du gleich zurecht.",
                )
                Knopf(
                    "Datei wählen",
                    beiDruck = {
                        probe.beenden()
                        dateiwahl.launch(arrayOf("audio/*"))
                    },
                    aktiv = wegeFrei && !voll,
                    kompakt = true,
                )
            }

            Weg("Selbst einsprechen") {
                SehrLeise(
                    "Bis zu ${Eigentoene.AUFNAHME_HOECHSTDAUER} Sekunden über das Mikrofon. Das Gerät fragt beim " +
                        "ersten Mal nach.",
                )
                if (nimmtAuf) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).background(Farben.Signal, CircleShape))
                        Text(
                            "$aufnahmeSekunden / ${Eigentoene.AUFNAHME_HOECHSTDAUER} s",
                            style = Schrift.MonoKlein,
                            color = Farben.Text,
                        )
                    }
                    Pillenreihe {
                        Knopf("Fertig", ::aufnahmeBeenden, art = Knopfart.Haupt, kompakt = true)
                        Knopf("Verwerfen", ::aufnahmeAbbrechen, art = Knopfart.Leise, kompakt = true)
                    }
                } else {
                    Knopf("Aufnehmen", ::aufnahmeStarten, aktiv = wegeFrei && !voll, kompakt = true)
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.alpha(if (premium) 1f else 0.45f),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Etikett("Deine Klänge")
                SehrLeise("${klaenge.size} von ${Eigentoene.KLANG_HOECHSTZAHL}")
            }

            when {
                !Eigentoene.klaengeGelesen -> SehrLeise("Wird gelesen …")
                klaenge.isEmpty() -> SehrLeise(
                    "Noch keiner. Was hier liegt, steht danach in der Tonwahl zwischen den anderen Tönen — und " +
                        "lässt sich dort genauso anklicken.",
                )
                else -> klaenge.forEach { k ->
                    val art = "klang:${k.id}"
                    val laeuft = probe.laeuft == art
                    Regalzeile(
                        name = k.name,
                        offen = offen == k.id,
                        eingestellt = eingestellt == art,
                        aktiv = premium,
                        beiOeffnen = {
                            probe.beenden()
                            offen = if (offen == k.id) null else k.id
                        },
                        beiWeg = { wegwerffrage = if (wegwerffrage == k.id) null else k.id },
                        zusatz = (if (k.herkunft == "aufnahme") "eingesprochen" else "Datei") + " · " +
                            sekunden(min(Eigentoene.KLANG_HOECHSTDAUER, k.bis - k.von)),
                        knoepfe = {
                            Knopf(
                                if (laeuft) "Läuft …" else "Anhören",
                                beiDruck = { probe.umschalten(art) },
                                art = if (laeuft) Knopfart.Haupt else Knopfart.Normal,
                                aktiv = premium,
                                kompakt = true,
                            )
                        },
                    )

                    // Die Rückfrage steht in der Zeile und nicht als zweiter Dialog über dem ersten.
                    if (wegwerffrage == k.id) {
                        SehrLeise("Wirklich wegwerfen? Die Aufnahme liegt nur hier — wir haben keine Kopie.")
                        Pillenreihe {
                            Knopf("Wegwerfen", { wegwerfen(k.id) }, art = Knopfart.Gefahr, kompakt = true)
                            Knopf("Behalten", { wegwerffrage = null }, art = Knopfart.Leise, kompakt = true)
                        }
                    }

                    if (offen == k.id) {
                        Klangbank(
                            k = k,
                            umriss = umriss,
                            eingestellt = eingestellt == art,
                            aktiv = premium,
                            beiEinstellen = {
                                bereich.launch { ablage.melderTonSetzen(art) }
                                probe.spielen(art)
                            },
                        )
                    }
                }
            }
        }

        if (meldung.isNotEmpty()) Text(meldung, style = Schrift.Klein, color = Farben.AmberHell)
    }
}

/** Einer der beiden Wege hinein — eine Karte mit Namen, Satz und Knopf. */
@Composable
private fun Weg(name: String, inhalt: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.BgTief, Rundung.Klein)
            .border(1.dp, Farben.Rand, Rundung.Klein)
            .padding(Abstand.Normal),
    ) {
        Text(name, style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text)
        inhalt()
    }
}

/**
 * Die Werkbank des offenen Klangs: Name, Wellenform, Anfang, Ende, Ruhe.
 *
 * <b>Zwei Schieber und kein Ziehen im Bild.</b> Die Marken liegen bei einer kurzen
 * Aufnahme wenige Punkte auseinander — ein Griff an der Wellenform wäre am Finger
 * unbrauchbar. Die Kurve zeigt den Ausschnitt, gestellt wird er darunter.
 */
@Composable
private fun Klangbank(
    k: EigenerKlang,
    umriss: List<Float>,
    eingestellt: Boolean,
    aktiv: Boolean,
    beiEinstellen: () -> Unit,
) {
    // Der Name im Feld — getrennt vom Satz, gesichert, wenn das Tippen innehält.
    var namensfeld by remember(k.id) { mutableStateOf(k.name) }
    LaunchedEffect(k.id, namensfeld) {
        delay(700)
        if (namensfeld.isNotBlank() && namensfeld != k.name) Eigentoene.klangAendern(k.id, name = namensfeld)
    }

    val dauer = min(Eigentoene.KLANG_HOECHSTDAUER, k.bis - k.von)
    val links = if (k.laenge > 0) (k.von / k.laenge).coerceIn(0.0, 1.0).toFloat() else 0f
    val rechts = if (k.laenge > 0) (k.bis / k.laenge).coerceIn(0.0, 1.0).toFloat() else 1f

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Farben.Rand, Rundung.Klein)
            .padding(Abstand.Normal),
    ) {
        Feld(
            wert = namensfeld,
            beiAenderung = { namensfeld = it.take(Eigentoene.NAME_LAENGE) },
            etikett = "Name",
            platzhalter = "z. B. Sirene Gerätehaus",
            aktiv = aktiv,
        )

        // Die Wellenform — Auskunft, kein Bedienelement.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Farben.BgTief, Rundung.Winzig)
                .drawBehind {
                    if (umriss.isNotEmpty()) {
                        val breite = size.width / umriss.size
                        umriss.forEachIndexed { i, spitze ->
                            val hoehe = max(2f, spitze * size.height)
                            val x = i * breite
                            val drin = (i + 0.5f) / umriss.size in links..rechts
                            drawRect(
                                color = if (drin) Farben.Amber else Farben.RandHell,
                                topLeft = Offset(x + breite * 0.15f, (size.height - hoehe) / 2f),
                                size = Size(breite * 0.7f, hoehe),
                            )
                        }
                    }
                    // Was außerhalb des Ausschnitts liegt, liegt im Schatten.
                    drawRect(Color.Black.copy(alpha = 0.45f), Offset.Zero, Size(size.width * links, size.height))
                    drawRect(
                        Color.Black.copy(alpha = 0.45f),
                        Offset(size.width * rechts, 0f),
                        Size(size.width * (1f - rechts), size.height),
                    )
                },
        ) {
            if (umriss.isEmpty()) SehrLeise("wird gezeichnet …")
        }

        val ende = (k.laenge * 100).roundToInt().coerceAtLeast(5)
        Schnittzeile("Anfang", k.von) {
            Regler(
                wert = (k.von * 100).roundToInt(),
                beiAenderung = { neu ->
                    val von = min(neu / 100.0, k.bis - 0.1)
                    Eigentoene.klangAendern(k.id, von = von, bis = min(k.bis, von + Eigentoene.KLANG_HOECHSTDAUER))
                },
                von = 0,
                bis = ende,
                schritt = 5,
                aktiv = aktiv,
            )
        }
        Schnittzeile("Ende", k.bis) {
            Regler(
                wert = (k.bis * 100).roundToInt(),
                beiAenderung = { neu ->
                    val bis = max(neu / 100.0, k.von + 0.1)
                    Eigentoene.klangAendern(k.id, bis = bis, von = max(k.von, bis - Eigentoene.KLANG_HOECHSTDAUER))
                },
                von = 0,
                bis = ende,
                schritt = 5,
                aktiv = aktiv,
            )
        }
        Schnittzeile("Ruhe danach", k.pause) {
            Regler(
                wert = (k.pause * 10).roundToInt(),
                beiAenderung = { neu -> Eigentoene.klangAendern(k.id, pause = neu / 10.0) },
                von = 0,
                bis = (Eigentoene.KLANG_HOECHSTPAUSE * 10).roundToInt(),
                aktiv = aktiv,
            )
        }

        SehrLeise(
            "Ausschnitt ${sekunden(dauer)} · ein Durchlauf ${sekunden(dauer + k.pause)} · Datei ${masse(k.groesse)}. " +
                "Ein Melder schweigt zwischen zwei Durchläufen — daran hört man, dass er kein Wecker ist.",
        )

        Knopf(
            if (eingestellt) "Eingestellt" else "Als Alarmton einstellen",
            beiDruck = beiEinstellen,
            art = Knopfart.Haupt,
            aktiv = aktiv && !eingestellt,
        )
    }
}

@Composable
private fun Schnittzeile(wort: String, wert: Double, regler: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text(wort, style = Schrift.Klein, color = Farben.TextLeise, modifier = Modifier.weight(1f))
            Text(sekunden(wert), style = Schrift.MonoKlein, color = Farben.Text)
        }
        regler()
    }
}

/** Was die Dokumentauswahl hergab — `bytes` fehlt, wenn die Datei zu groß ist. */
private class Gewaehlt(val name: String, val typ: String, val groesse: Long, val bytes: ByteArray?)

/**
 * Eine gewählte Datei lesen — Name ohne Endung, Inhaltstyp, Bytes. Zu große Dateien
 * werden gar nicht erst ganz gelesen: Drei Megabyte sind die Grenze, und alles
 * darüber ist ein Lied und kein Melderton.
 */
private fun dateiLesen(zusammenhang: Context, quelle: Uri): Gewaehlt? {
    val aufloeser = zusammenhang.contentResolver
    var name = "Eigene Datei"
    var groesse = -1L
    aufloeser.query(quelle, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { zeiger ->
        if (zeiger.moveToFirst()) {
            val n = zeiger.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val g = zeiger.getColumnIndex(OpenableColumns.SIZE)
            if (n >= 0 && !zeiger.isNull(n)) name = zeiger.getString(n)
            if (g >= 0 && !zeiger.isNull(g)) groesse = zeiger.getLong(g)
        }
    }
    val typ = aufloeser.getType(quelle) ?: "audio/*"
    val ohneEndung = name.replace(Regex("\\.[^.]+$"), "")
    val grenze = Eigentoene.KLANG_HOECHSTBYTES

    if (groesse > grenze) return Gewaehlt(ohneEndung, typ, groesse, null)

    val bytes = aufloeser.openInputStream(quelle)?.use { strom ->
        val puffer = java.io.ByteArrayOutputStream()
        val stueck = ByteArray(64 * 1024)
        var gesamt = 0L
        while (true) {
            val n = strom.read(stueck)
            if (n < 0) break
            gesamt += n
            if (gesamt > grenze) return Gewaehlt(ohneEndung, typ, gesamt, null)
            puffer.write(stueck, 0, n)
        }
        puffer.toByteArray()
    } ?: return null

    return Gewaehlt(ohneEndung, typ, bytes.size.toLong(), bytes)
}

/** Megabyte mit einer Nachkommastelle, darunter Kilobyte — mit Komma, wie im Web. */
private fun masse(bytes: Long): String =
    if (bytes >= 1024 * 1024) {
        String.format(Locale.ROOT, "%.1f", bytes / 1024.0 / 1024.0).replace('.', ',') + " MB"
    } else {
        "${max(1L, (bytes / 1024.0).roundToInt().toLong())} kB"
    }

private fun sekunden(wert: Double): String =
    String.format(Locale.ROOT, "%.1f", wert).replace('.', ',') + " s"
