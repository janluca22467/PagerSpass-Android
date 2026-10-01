package de.pagerspass.pagerspass.ansichten

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Kartenablage
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Die Schichtkarte: die Nachbesprechung als Bild — übertragen aus
 * `utils/schichtkarte.ts` und dem Kartenbereich der `DebriefingView.vue` (5.0.0.26).
 * Leitstelle, Datum, die vier Kennzahlen des Abends und die Gutschrift, 1200 × 630
 * (das Maß einer Link-Vorschau), auf dem Gerät gezeichnet.
 *
 * <b>Dieselbe Zeichnung wie im Web</b>, Strich für Strich — Maße, Schriftgrößen und
 * die Farbtafeln sind abgeschrieben. Wortmarke und Melder liegen als Assets bei
 * (`assets/marke/`); fehlen sie, steht der Name als Text da, wie im Web offline.
 */

data class Schichtkartendaten(
    val leitstelle: String,
    val code: String,
    val datum: String,
    val dienstdauer: String,
    val einsaetze: Int,
    val abgeschlossen: Int,
    val disposition: String,
    val hilfsfrist: String,
    val funksprueche: Int,
    /** Nur direkt nach dem eigenen Dienst. */
    val punkte: Int?,
    val rang: String?,
    val motto: String? = null,
)

/** Wie lang das Motto höchstens sein darf — Feld und Zeichnung halten sich daran. */
const val MOTTO_MAX = 48

private class Kartenfarben(
    val grundOben: Int,
    val grundUnten: Int,
    val flaeche: Int,
    val rand: Int,
    val text: Int,
    val leise: Int,
    val sehrLeise: Int,
    val akzent: Int,
    val hell: Boolean = false,
)

private fun f(hex: Long): Int = (0xFF000000 or hex).toInt()

/** Die Gestaltungen — `SCHICHTKARTEN_DESIGNS` (Name, Stufe, Abo) und ihre Farbtafeln. */
private class Kartendesign(val id: String, val name: String, val abLevel: Int, val premium: Boolean, val farben: Kartenfarben)

private val DESIGNS = listOf(
    Kartendesign("standard", "Dunkel", 1, false, Kartenfarben(f(0x101825), f(0x0a0f16), f(0x141c27), f(0x2a3648), f(0xe9f0f9), f(0xb0c0d4), f(0x8797ad), f(0xffb020))),
    Kartendesign("bernstein", "Bernstein-LCD", 25, false, Kartenfarben(f(0x241703), f(0x150d02), f(0x2c1e06), f(0x5c451a), f(0xffd98a), f(0xd9b465), f(0xa2833f), f(0xffb020))),
    Kartendesign("hell", "Heller Aushang", 50, false, Kartenfarben(f(0xf4f1ea), f(0xe7e1d3), f(0xfdfcf8), f(0xcfc6b2), f(0x20242b), f(0x4d5460), f(0x7d8494), f(0xb3760a), hell = true)),
    Kartendesign("premium-leitstelle", "Leitstellennacht", 1, true, Kartenfarben(f(0x0b1220), f(0x060a12), f(0x111b2e), f(0x27405f), f(0xe6f0ff), f(0xa9c0dd), f(0x7d93b0), f(0x4f9dff))),
    Kartendesign("premium-signalrot", "Signalrot", 1, true, Kartenfarben(f(0x1c1214), f(0x100a0b), f(0x26171a), f(0x5e2b31), f(0xffe9ea), f(0xe0b3b7), f(0xac858a), f(0xff5a5f))),
    Kartendesign("premium-blaulicht", "Blaue Stunde", 1, true, Kartenfarben(f(0x101a3a), f(0x080d1f), f(0x182550), f(0x31468a), f(0xe8ecff), f(0xb3bcea), f(0x8790bd), f(0x7f8cff))),
    Kartendesign("premium-wachengold", "Wachengold", 1, true, Kartenfarben(f(0x191613), f(0x0d0b09), f(0x211d17), f(0x57482a), f(0xf6e9c8), f(0xd3c194), f(0xa5966d), f(0xffcc4d))),
    Kartendesign("premium-waldwacht", "Waldwacht", 1, true, Kartenfarben(f(0x0d1a13), f(0x06100b), f(0x14261b), f(0x2c5138), f(0xe6f6ea), f(0xaed3ba), f(0x84a690), f(0x54d08a))),
    Kartendesign("premium-morgenlicht", "Morgenlicht", 1, true, Kartenfarben(f(0xfdf3e6), f(0xf4e2cc), f(0xfffaf3), f(0xdcc4a6), f(0x2a1f16), f(0x5c4736), f(0x8a7460), f(0xc2530f), hell = true)),
)

private const val BREITE = 1200
private const val HOEHE = 630

private fun asset(zusammenhang: Context, pfad: String): Bitmap? =
    runCatching { zusammenhang.assets.open(pfad).use { BitmapFactory.decodeStream(it) } }.getOrNull()

private fun schrift(gewicht: Int, mono: Boolean = false, kursiv: Boolean = false): Typeface =
    Typeface.create(if (mono) Typeface.MONOSPACE else Typeface.SANS_SERIF, gewicht, kursiv)

/** `fillText` mit Höchstbreite: Was nicht passt, wird gestaucht statt abgeschnitten. */
private fun Canvas.text(t: String, x: Float, y: Float, p: Paint, max: Float? = null) {
    val breite = p.measureText(t)
    val alt = p.textScaleX
    if (max != null && breite > max) p.textScaleX = max / breite
    drawText(t, x, y, p)
    p.textScaleX = alt
}

/** Zeichnet die Karte — dieselben Schritte in derselben Reihenfolge wie `schichtkarteAlsBlob`. */
fun schichtkarteZeichnen(zusammenhang: Context, d: Schichtkartendaten, designId: String = "standard"): Bitmap {
    val design = DESIGNS.firstOrNull { it.id == designId } ?: DESIGNS.first()
    val fa = design.farben
    val bild = Bitmap.createBitmap(BREITE, HOEHE, Bitmap.Config.ARGB_8888)
    val z = Canvas(bild)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    // Grund mit leichtem Verlauf.
    p.shader = LinearGradient(0f, 0f, 0f, HOEHE.toFloat(), fa.grundOben, fa.grundUnten, Shader.TileMode.CLAMP)
    z.drawRect(0f, 0f, BREITE.toFloat(), HOEHE.toFloat(), p)
    p.shader = null

    // Der Melder rechts, leicht angeschnitten wie ein Gerät am Bildrand.
    asset(zusammenhang, "marke/melder-512.png")?.let { m ->
        val h = 430f
        val w = m.width.toFloat() / m.height * h
        z.save()
        p.alpha = if (design.id == "hell") 217 else 235
        z.translate(BREITE - w + 70f, 150f)
        z.rotate(-7f)
        z.drawBitmap(m, null, RectF(0f, 0f, w, h), p)
        z.restore()
        p.alpha = 255
    }

    val links = 64f

    // Kopf: Wortmarke links, Datum rechts.
    val wortmarke = asset(zusammenhang, if (fa.hell) "marke/wortmarke-hell.png" else "marke/wortmarke.png")
    if (wortmarke != null) {
        val h = 44f
        val w = wortmarke.width.toFloat() / wortmarke.height * h
        z.drawBitmap(wortmarke, null, RectF(links, 52f, links + w, 52f + h), p)
    } else {
        p.color = fa.text
        p.typeface = schrift(800)
        p.textSize = 40f
        z.text("PagerSpass", links, 88f, p)
    }

    p.color = fa.sehrLeise
    p.typeface = schrift(500, mono = true)
    p.textSize = 24f
    p.textAlign = Paint.Align.RIGHT
    z.text(d.datum, BREITE - 64f, 84f, p)
    p.textAlign = Paint.Align.LEFT

    // Die Schicht.
    p.color = fa.akzent
    p.typeface = schrift(700)
    p.textSize = 22f
    p.letterSpacing = 3f / 22f
    z.text("NACHBESPRECHUNG", links, 168f, p)
    p.letterSpacing = 0f

    p.color = fa.text
    p.typeface = schrift(800)
    p.textSize = 58f
    z.text(d.leitstelle, links, 232f, p, 780f)

    p.color = fa.leise
    p.typeface = schrift(500)
    p.textSize = 26f
    z.text("Raum ${d.code} · Dienstdauer ${d.dienstdauer}", links, 276f, p)

    // Das Motto aus dem Abo — in Anführungszeichen und in der leisesten Farbe.
    d.motto?.trim()?.take(MOTTO_MAX)?.takeIf { it.isNotEmpty() }?.let { motto ->
        p.color = fa.sehrLeise
        p.typeface = schrift(500, kursiv = true)
        p.textSize = 24f
        z.text("„$motto“", links, 312f, p, 640f)
    }

    // Die vier Kennzahlen.
    val kennzahlen = listOf(
        Triple("${d.einsaetze}", "Einsätze", "${d.abgeschlossen} abgeschlossen"),
        Triple(d.disposition, "Ø Disposition", "Notruf bis Alarm"),
        Triple(d.hilfsfrist, "Ø Hilfsfrist", "Alarm bis Eintreffen"),
        Triple("${d.funksprueche}", "Funksprüche", "im Protokoll"),
    )
    val kachelBreite = 232f
    val kachelHoehe = 150f
    val abstand = 20f
    val obenY = 330f
    kennzahlen.forEachIndexed { i, (wert, name, unter) ->
        val x = links + i * (kachelBreite + abstand)
        val r = RectF(x, obenY, x + kachelBreite, obenY + kachelHoehe)
        p.style = Paint.Style.FILL
        p.color = fa.flaeche
        z.drawRoundRect(r, 12f, 12f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        p.color = fa.rand
        z.drawRoundRect(r, 12f, 12f, p)
        p.style = Paint.Style.FILL

        p.color = fa.akzent
        p.typeface = schrift(700)
        p.textSize = 17f
        z.text(name.uppercase(), x + 22f, obenY + 40f, p)

        p.color = fa.text
        p.typeface = schrift(800, mono = true)
        p.textSize = 46f
        z.text(wert, x + 22f, obenY + 96f, p, kachelBreite - 44f)

        p.color = fa.sehrLeise
        p.typeface = schrift(500)
        p.textSize = 17f
        z.text(unter, x + 22f, obenY + 126f, p, kachelBreite - 44f)
    }

    // Die Gutschrift, wenn es eine gibt.
    if (d.punkte != null) {
        val y = obenY + kachelHoehe + 62f
        p.color = fa.akzent
        p.typeface = schrift(800, mono = true)
        p.textSize = 30f
        val punkteText = "+${d.punkte} Punkte"
        val punkteBreite = p.measureText(punkteText)
        z.text(punkteText, links, y, p)
        d.rang?.let {
            p.color = fa.leise
            p.typeface = schrift(500)
            p.textSize = 26f
            z.text("· $it", links + punkteBreite + 14f, y, p)
        }
    }

    // Fuß: Haarlinie, Adresse und der Satz, der überall steht.
    p.color = fa.rand
    p.strokeWidth = 1f
    z.drawLine(links, HOEHE - 74f, BREITE - 64f, HOEHE - 74f, p)

    p.color = fa.sehrLeise
    p.typeface = schrift(600)
    p.textSize = 22f
    z.text("pagerspass.de", links, HOEHE - 36f, p)

    p.textAlign = Paint.Align.RIGHT
    p.typeface = schrift(500)
    p.textSize = 20f
    z.text("Ein Spiel, kein Einsatzmittel — bei echten Notfällen: 112", BREITE - 64f, HOEHE - 36f, p)
    p.textAlign = Paint.Align.LEFT

    return bild
}

/** Gestaltung und Motto sind eine Gerätewahl wie im Web (`localStorage`). */
private const val KARTENDATEI = "pagerspass_schichtkarte"

/**
 * Der Kartenbereich der Nachbesprechung: links das Bild, darunter, was daran zu stellen
 * ist — Gestaltung, Motto (Abo), Teilen. Er steht auf der Seite und nicht in einem
 * Fenster: Man vergleicht die Gestaltungen und sieht das Motto einrücken.
 */
@Composable
fun ColumnScope.Kartenbereich(daten: Schichtkartendaten, level: Int, premium: Boolean, server: String?) {
    val zusammenhang = LocalContext.current
    val uri = LocalUriHandler.current
    val bereich = rememberCoroutineScope()
    val ablage = remember { zusammenhang.getSharedPreferences(KARTENDATEI, Context.MODE_PRIVATE) }

    // Die Laufbahn-Gestaltungen erst, wenn die Stufe sie hergibt; die Abo-Gestaltungen
    // immer — ohne Abo als gesperrte Probe mit Stern.
    val angeboten = DESIGNS.filter { it.premium || level >= it.abLevel }
    fun waehlbar(d: Kartendesign) = !d.premium || premium
    var design by remember { mutableStateOf(ablage.getString("design", "standard") ?: "standard") }
    var motto by remember { mutableStateOf((ablage.getString("motto", "") ?: "").take(MOTTO_MAX)) }
    // Was einem nicht (mehr) zusteht, fällt auf „Dunkel" zurück — für Vorschau und Datei gleich.
    val gezeichnet = angeboten.firstOrNull { it.id == design && waehlbar(it) }?.id ?: "standard"
    val mitMotto = daten.copy(motto = if (premium) motto.trim() else null)

    var vorschau by remember { mutableStateOf<Bitmap?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var laeuft by remember { mutableStateOf(false) }

    LaunchedEffect(mitMotto, gezeichnet) {
        vorschau = withContext(Dispatchers.Default) {
            runCatching { schichtkarteZeichnen(zusammenhang, mitMotto, gezeichnet) }.getOrNull()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.Flaeche, Rundung.Normal)
            .border(1.dp, Farben.Rand, Rundung.Normal)
            .padding(Abstand.Normal),
    ) {
        val farben = (DESIGNS.firstOrNull { it.id == gezeichnet } ?: DESIGNS.first()).farben
        val grund = Modifier
            .fillMaxWidth()
            .aspectRatio(BREITE / HOEHE.toFloat())
            .clip(Rundung.Klein)
            .background(Brush.linearGradient(listOf(Color(farben.grundOben), Color(farben.grundUnten))))
            .border(1.dp, Color(farben.rand), Rundung.Klein)
        val b = vorschau
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription = "Vorschau der Schichtkarte", modifier = grund)
        } else {
            Column(grund) {}
        }

        Text("Schichtkarte", style = Schrift.Gross, color = Farben.Text)
        Leise("Die Kennzahlen des Abends als Bild — 1200 × 630, fertig für Discord.")

        if (angeboten.size > 1) {
            Pillenreihe {
                angeboten.forEach { d ->
                    Pille(
                        aufschrift = (if (d.premium) "★ " else "") + d.name,
                        an = gezeichnet == d.id,
                        aktiv = waehlbar(d),
                        farbe = if (d.premium) Farben.AmberHell else Farben.Amber,
                        beiDruck = {
                            design = d.id
                            ablage.edit().putString("design", d.id).apply()
                        },
                    )
                }
            }
        }

        if (!premium) {
            Knopf(
                "★ Eigenes Motto auf die Karte",
                {
                    bereich.launch {
                        val s = server ?: de.pagerspass.pagerspass.netz.Ablage(zusammenhang).server()
                        runCatching { uri.openUri("${s.trimEnd('/')}/play/mobile/shop?bereich=premium") }
                    }
                },
                kompakt = true,
            )
        } else {
            Etikett("Dein Motto")
            Feld(
                motto,
                {
                    motto = it.take(MOTTO_MAX)
                    ablage.edit().putString("motto", motto).apply()
                },
                platzhalter = "Steht unter dem Namen der Leitstelle",
            )
            SehrLeise("${motto.length}/$MOTTO_MAX", mono = true)
        }

        Knopf(
            if (laeuft) "Wird gezeichnet …" else "Teilen",
            {
                laeuft = true
                fehler = null
                bereich.launch {
                    runCatching {
                        val bild = withContext(Dispatchers.Default) { schichtkarteZeichnen(zusammenhang, mitMotto, gezeichnet) }
                        val adresse = withContext(Dispatchers.IO) {
                            Kartenablage.ablegen(zusammenhang, "schichtkarte-${daten.code}", bild)
                        }
                        val senden = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, adresse)
                            clipData = android.content.ClipData.newRawUri("Schichtkarte", adresse)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        zusammenhang.startActivity(
                            Intent.createChooser(senden, "Schichtkarte teilen")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
                        )
                    }.onFailure { fehler = it.message ?: "Die Schichtkarte ließ sich nicht erzeugen." }
                    laeuft = false
                }
            },
            art = Knopfart.Haupt,
            aktiv = !laeuft,
        )
        fehler?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
    }
}
