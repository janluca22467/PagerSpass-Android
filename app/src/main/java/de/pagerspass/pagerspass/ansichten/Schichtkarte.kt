package de.pagerspass.pagerspass.ansichten

import android.content.Context
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.schmuck.Kataloge
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Die Schichtkarte — die Kennzahlen des Abends als Bild, 1200 × 630, fertig für
 * Discord. Übertragen aus `utils/schichtkarte.ts` und dem Kartenbereich der
 * Nachbesprechung.
 *
 * <b>Eine Stelle für Vorschau und Datei.</b> Eine Vorschau, die ihre Zahlen anders
 * zusammensucht als das Bild, das man danach speichert, ist keine Vorschau, sondern
 * eine zweite Karte, die zufällig ähnlich aussieht. Beide Wege zeichnen deshalb mit
 * `schichtkarteZeichnen`.
 *
 * <b>Die Wortmarke fehlt am Handy</b> — das Web lädt sie als Bild vom Server; hier
 * steht, wie im Web, wenn das Bild nicht kommt, der Schriftzug.
 */

/** Was auf der Karte steht. */
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
    val punkte: Int? = null,
    val rang: String? = null,
    val motto: String? = null,
)

/** Die Farben einer Gestaltung. */
private data class Kartenfarben(
    val grundOben: Color,
    val grundUnten: Color,
    val flaeche: Color,
    val rand: Color,
    val text: Color,
    val leise: Color,
    val sehrLeise: Color,
    val akzent: Color,
)

private val DESIGNS = mapOf(
    "standard" to Kartenfarben(
        Color(0xFF101825), Color(0xFF0A0F16), Color(0xFF141C27), Color(0xFF2A3648),
        Color(0xFFE9F0F9), Color(0xFFB0C0D4), Color(0xFF8797AD), Color(0xFFFFB020),
    ),
    "bernstein" to Kartenfarben(
        Color(0xFF241703), Color(0xFF150D02), Color(0xFF2C1E06), Color(0xFF5C451A),
        Color(0xFFFFD98A), Color(0xFFD9B465), Color(0xFFA2833F), Color(0xFFFFB020),
    ),
    "hell" to Kartenfarben(
        Color(0xFFF4F1EA), Color(0xFFE7E1D3), Color(0xFFFDFCF8), Color(0xFFCFC6B2),
        Color(0xFF20242B), Color(0xFF4D5460), Color(0xFF7D8494), Color(0xFFB3760A),
    ),
    "premium-leitstelle" to Kartenfarben(
        Color(0xFF0B1220), Color(0xFF060A12), Color(0xFF111B2E), Color(0xFF27405F),
        Color(0xFFE6F0FF), Color(0xFFA9C0DD), Color(0xFF7D93B0), Color(0xFF4F9DFF),
    ),
    "premium-signalrot" to Kartenfarben(
        Color(0xFF1C1214), Color(0xFF100A0B), Color(0xFF26171A), Color(0xFF5E2B31),
        Color(0xFFFFE9EA), Color(0xFFE0B3B7), Color(0xFFAC858A), Color(0xFFFF5A5F),
    ),
    "premium-blaulicht" to Kartenfarben(
        Color(0xFF101A3A), Color(0xFF080D1F), Color(0xFF182550), Color(0xFF31468A),
        Color(0xFFE8ECFF), Color(0xFFB3BCEA), Color(0xFF8790BD), Color(0xFF7F8CFF),
    ),
    "premium-wachengold" to Kartenfarben(
        Color(0xFF191613), Color(0xFF0D0B09), Color(0xFF211D17), Color(0xFF57482A),
        Color(0xFFF6E9C8), Color(0xFFD3C194), Color(0xFFA5966D), Color(0xFFFFCC4D),
    ),
    "premium-waldwacht" to Kartenfarben(
        Color(0xFF0D1A13), Color(0xFF06100B), Color(0xFF14261B), Color(0xFF2C5138),
        Color(0xFFE6F6EA), Color(0xFFAED3BA), Color(0xFF84A690), Color(0xFF54D08A),
    ),
    "premium-morgenlicht" to Kartenfarben(
        Color(0xFFFDF3E6), Color(0xFFF4E2CC), Color(0xFFFFFAF3), Color(0xFFDCC4A6),
        Color(0xFF2A1F16), Color(0xFF5C4736), Color(0xFF8A7460), Color(0xFFC2530F),
    ),
)

private fun farbenVon(design: String): Kartenfarben = DESIGNS[design] ?: DESIGNS.getValue("standard")

/** Wie lang das Motto sein darf — die Karte hat eine Zeile dafür. */
internal const val SCHICHTKARTE_MOTTO_MAX = 48

private const val BREITE = 1200
private const val HOEHE = 630

/**
 * Die Karte zeichnen — dieselben Maße, Schriftgrößen und Abstände wie im Web.
 */
fun schichtkarteZeichnen(d: Schichtkartendaten, design: String): Bitmap {
    val f = farbenVon(design)
    val bild = Bitmap.createBitmap(BREITE, HOEHE, Bitmap.Config.ARGB_8888)
    val z = android.graphics.Canvas(bild)
    val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    val sansFett = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    val mono = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    val monoFett = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    val grund = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, HOEHE.toFloat(),
            f.grundOben.toArgb(), f.grundUnten.toArgb(), Shader.TileMode.CLAMP,
        )
    }
    z.drawRect(0f, 0f, BREITE.toFloat(), HOEHE.toFloat(), grund)

    fun schrift(farbe: Color, groesse: Float, art: Typeface, rechts: Boolean = false) = Paint().apply {
        isAntiAlias = true
        color = farbe.toArgb()
        textSize = groesse
        typeface = art
        textAlign = if (rechts) Paint.Align.RIGHT else Paint.Align.LEFT
    }

    /** Text, der in eine Breite passen muss — gestaucht statt übergelaufen, wie `fillText(…, maxWidth)`. */
    fun passend(text: String, x: Float, y: Float, paint: Paint, hoechstens: Float) {
        val breite = paint.measureText(text)
        if (breite > hoechstens && breite > 0f) {
            z.save()
            z.scale(hoechstens / breite, 1f, x, y)
            z.drawText(text, x, y, paint)
            z.restore()
        } else {
            z.drawText(text, x, y, paint)
        }
    }

    val links = 64f
    z.drawText("PagerSpass", links, 88f, schrift(f.text, 40f, sansFett))
    z.drawText(d.datum, BREITE - 64f, 84f, schrift(f.sehrLeise, 24f, mono, rechts = true))

    val etikett = schrift(f.akzent, 22f, sansFett).apply { letterSpacing = 0.14f }
    z.drawText("NACHBESPRECHUNG", links, 168f, etikett)
    passend(d.leitstelle, links, 232f, schrift(f.text, 58f, sansFett), 780f)
    z.drawText("Raum ${d.code} · Dienstdauer ${d.dienstdauer}", links, 276f, schrift(f.leise, 26f, sans))

    val motto = d.motto?.trim()?.take(SCHICHTKARTE_MOTTO_MAX)
    if (!motto.isNullOrEmpty()) {
        val kursiv = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        passend("„$motto“", links, 312f, schrift(f.sehrLeise, 24f, kursiv), 640f)
    }

    val kennzahlen = listOf(
        Triple(d.einsaetze.toString(), "Einsätze", "${d.abgeschlossen} abgeschlossen"),
        Triple(d.disposition, "Ø Disposition", "Notruf bis Alarm"),
        Triple(d.hilfsfrist, "Ø Hilfsfrist", "Alarm bis Eintreffen"),
        Triple(d.funksprueche.toString(), "Funksprüche", "im Protokoll"),
    )
    val kachelBreite = 232f
    val kachelHoehe = 150f
    val obenY = 330f
    val kachelGrund = Paint().apply { isAntiAlias = true; color = f.flaeche.toArgb() }
    val kachelRand = Paint().apply {
        isAntiAlias = true
        color = f.rand.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    kennzahlen.forEachIndexed { i, (wert, name, unter) ->
        val x = links + i * (kachelBreite + 20f)
        val rahmen = RectF(x, obenY, x + kachelBreite, obenY + kachelHoehe)
        z.drawRoundRect(rahmen, 12f, 12f, kachelGrund)
        z.drawRoundRect(rahmen, 12f, 12f, kachelRand)
        z.drawText(name.uppercase(), x + 22f, obenY + 40f, schrift(f.akzent, 17f, sansFett))
        passend(wert, x + 22f, obenY + 96f, schrift(f.text, 46f, monoFett), kachelBreite - 44f)
        passend(unter, x + 22f, obenY + 126f, schrift(f.sehrLeise, 17f, sans), kachelBreite - 44f)
    }

    if (d.punkte != null) {
        val y = obenY + kachelHoehe + 62f
        val punkte = schrift(f.akzent, 30f, monoFett)
        val text = "+${d.punkte} Punkte"
        z.drawText(text, links, y, punkte)
        if (d.rang != null) {
            z.drawText("· ${d.rang}", links + punkte.measureText(text) + 14f, y, schrift(f.leise, 26f, sans))
        }
    }

    val linie = Paint().apply { color = f.rand.toArgb(); strokeWidth = 1f }
    z.drawLine(links, HOEHE - 74f, BREITE - 64f, HOEHE - 74f, linie)
    z.drawText("pagerspass.de", links, HOEHE - 36f, schrift(f.sehrLeise, 22f, sansFett))
    z.drawText(
        "Ein Spiel, kein Einsatzmittel — bei echten Notfällen: 112",
        BREITE - 64f,
        HOEHE - 36f,
        schrift(f.sehrLeise, 20f, sans, rechts = true),
    )

    return bild
}

private const val ABLAGE = "pagerspass.schichtkarte"

private fun gemerkt(zusammenhang: Context, schluessel: String): String? = runCatching {
    zusammenhang.getSharedPreferences(ABLAGE, Context.MODE_PRIVATE).getString(schluessel, null)
}.getOrNull()

private fun merken(zusammenhang: Context, schluessel: String, wert: String) {
    runCatching {
        zusammenhang.getSharedPreferences(ABLAGE, Context.MODE_PRIVATE).edit().putString(schluessel, wert).apply()
    }
}

/**
 * Der Kartenbereich: links das Bild, darunter, was daran zu stellen ist.
 *
 * <b>Die Gestaltung ist eine Gerätewahl</b> wie im Web — gemerkt am Gerät,
 * angeboten nur, was einem zusteht: Die Laufbahn-Gestaltungen erst ab ihrer Stufe
 * (eine Belohnung kündigt man nicht an), die Abo-Gestaltungen immer, ohne Abo aber
 * gesperrt mit Stern. <b>Ein ausgelaufenes Abo behält seine Gestaltung nicht</b> —
 * gezeichnet wird dann wieder „Dunkel", und das Motto bleibt im Feld stehen, nur
 * nicht auf der Karte.
 */
@Composable
fun Schichtkartenbereich(
    daten: Schichtkartendaten,
    stufe: Int,
    premiumAktiv: Boolean,
    beiMottowerbung: () -> Unit,
) {
    val zusammenhang = LocalContext.current
    val designs = Kataloge.SCHICHTKARTEN_DESIGNS.filter { it.premium || stufe >= it.abLevel }
    var design by remember { mutableStateOf(gemerkt(zusammenhang, "design") ?: "standard") }
    var motto by remember { mutableStateOf((gemerkt(zusammenhang, "motto") ?: "").take(SCHICHTKARTE_MOTTO_MAX)) }
    var vorschau by remember { mutableStateOf<Bitmap?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }

    val gezeichnet = designs.firstOrNull { it.id == design && (!it.premium || premiumAktiv) }?.id ?: "standard"
    val mitMotto = daten.copy(motto = if (premiumAktiv) motto.trim() else "")

    // Nach einer kurzen Ruhe — beim Tippen im Mottofeld entstünde sonst je Zeichen
    // ein Bild von 1200 × 630.
    LaunchedEffect(mitMotto, gezeichnet) {
        delay(250)
        vorschau = withContext(Dispatchers.Default) {
            runCatching { schichtkarteZeichnen(mitMotto, gezeichnet) }.getOrNull()
        }
    }

    val speichern = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { ziel ->
        if (ziel == null) return@rememberLauncherForActivityResult
        fehler = runCatching {
            val bild = vorschau ?: schichtkarteZeichnen(mitMotto, gezeichnet)
            val puffer = ByteArrayOutputStream()
            bild.compress(Bitmap.CompressFormat.PNG, 100, puffer)
            zusammenhang.contentResolver.openOutputStream(ziel)?.use { it.write(puffer.toByteArray()) }
                ?: error("Die Datei ließ sich nicht schreiben.")
        }.exceptionOrNull()?.let { it.message ?: "Die Schichtkarte ließ sich nicht erzeugen." }
    }

    val f = farbenVon(gezeichnet)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Gross),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BREITE.toFloat() / HOEHE)
                .background(Brush.linearGradient(listOf(f.grundOben, f.grundUnten)), Rundung.Klein)
                .border(1.dp, f.rand, Rundung.Klein),
        ) {
            vorschau?.let { b ->
                Image(
                    bitmap = b.asImageBitmap(),
                    contentDescription = "Vorschau der Schichtkarte",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().aspectRatio(BREITE.toFloat() / HOEHE),
                )
            }
        }

        Text("Schichtkarte", style = Schrift.Gross, color = Farben.Text)
        SehrLeise("Die Kennzahlen des Abends als Bild — 1200 × 630, fertig für Discord.")

        if (designs.size > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                designs.forEach { d ->
                    val waehlbar = !d.premium || premiumAktiv
                    val probe = farbenVon(d.id)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                        modifier = Modifier.clickable(
                            enabled = waehlbar,
                            onClick = {
                                design = d.id
                                merken(zusammenhang, "design", d.id)
                            },
                            role = Role.RadioButton,
                            indication = null,
                            interactionSource = null,
                        ),
                    ) {
                        Box(
                            Modifier
                                .size(width = 64.dp, height = 34.dp)
                                .background(Brush.linearGradient(listOf(probe.grundOben, probe.grundUnten)), Rundung.Winzig)
                                .border(
                                    if (gezeichnet == d.id) 2.dp else 1.dp,
                                    if (gezeichnet == d.id) Farben.Amber else probe.rand,
                                    Rundung.Winzig,
                                ),
                        ) {
                            Box(
                                Modifier
                                    .padding(6.dp)
                                    .size(width = 22.dp, height = 4.dp)
                                    .background(probe.akzent),
                            )
                        }
                        Text(
                            text = (if (d.premium) "★ " else "") + d.name,
                            style = Schrift.Winzig,
                            color = when {
                                !waehlbar -> Farben.TextSehrLeise
                                d.premium -> Farben.AmberHell
                                else -> Farben.TextLeise
                            },
                        )
                    }
                }
            }
        }

        if (!premiumAktiv) {
            Knopf("★ Eigenes Motto auf die Karte", beiMottowerbung, kompakt = true)
        } else {
            Feld(
                wert = motto,
                beiAenderung = {
                    motto = it.take(SCHICHTKARTE_MOTTO_MAX)
                    merken(zusammenhang, "motto", motto)
                },
                etikett = "Dein Motto",
                platzhalter = "Steht unter dem Namen der Leitstelle",
            )
            SehrLeise("${motto.length}/$SCHICHTKARTE_MOTTO_MAX", mono = true)
        }

        Row {
            Knopf(
                aufschrift = "Bild speichern",
                beiDruck = {
                    fehler = null
                    speichern.launch("schichtkarte-${daten.code}.png")
                },
                art = Knopfart.Haupt,
            )
        }
        fehler?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
    }
}
