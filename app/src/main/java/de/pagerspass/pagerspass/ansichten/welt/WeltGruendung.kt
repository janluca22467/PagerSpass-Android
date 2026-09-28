package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Weltleitstelle
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.karte.Kartenflaeche
import de.pagerspass.pagerspass.ui.karte.Kartenmarke
import de.pagerspass.pagerspass.ui.karte.Kartenpinsel
import de.pagerspass.pagerspass.ui.karte.Kartenrand
import de.pagerspass.pagerspass.ui.karte.Kartenstil
import de.pagerspass.pagerspass.ui.karte.rememberKartenzustand
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Die Gründung: einmal im Leben eine Deutschlandkarte, ein Tipp, ein Name — übertragen aus
 * `views/WeltGruendungView.vue`.
 *
 * <b>Warum eine eigene Seite und nicht ein Dialog im Arbeitsplatz.</b> Der Standort ist die
 * folgenreichste Entscheidung des ganzen Modus — an ihm hängt jede Anfahrtszeit, jeder
 * Nachbar und jede Lage, die je entsteht. Er verdient den ganzen Bildschirm, und zwar
 * genau einmal: Wer schon eine Leitstelle hat, wird von hier sofort weitergeschickt.
 *
 * <b>Fremde Leitstellen stehen mit auf der Karte.</b> Sie sind die Auskunft, die man
 * braucht: Sitzt hier schon jemand, mit dem ich mir die Straße teile?
 *
 * <b>Am Handy liegt die Maske als Blatt über den unteren 60 %</b> — die Karte darüber ist
 * die Fläche, auf die man tippt.
 */
@Composable
fun WeltGruendungSeite(
    welt: Welt,
    kennung: String?,
    beiArbeitsplatz: () -> Unit,
    beiZurueck: () -> Unit,
) {
    val zustand by welt.zustand.collectAsState()
    val bereich = rememberCoroutineScope()
    val zusammenhang = LocalContext.current
    val ablage = remember { Ablage(zusammenhang) }
    val dichte = LocalDensity.current
    val schirmhoehe = LocalConfiguration.current.screenHeightDp.dp
    val blatthoehe = schirmhoehe * 0.6f
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val unten = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Deutschland im Bild: Der Modus spielt hier, und ein Weltausschnitt zwänge jeden dazu,
    // erst einmal hineinzuzoomen.
    val karte = rememberKartenzustand(51.2, 10.4, 6.0)

    var laedt by remember { mutableStateOf(true) }
    var leitstellen by remember { mutableStateOf<List<Weltleitstelle>>(emptyList()) }
    var gewaehltLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var gewaehltLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var suche by rememberSaveable { mutableStateOf("") }
    var sucheLeer by remember { mutableStateOf(false) }
    var hinweis by remember { mutableStateOf<String?>(null) }
    var versuch by remember { mutableStateOf(0) }

    LaunchedEffect(kennung, versuch) {
        welt.kennungSetzen(kennung)
        if (kennung == null) return@LaunchedEffect

        // Die fremden Leitstellen gleich mit anfragen: Sie hängen nicht am eigenen Stand.
        val fremde = bereich.launch {
            runCatching { welt.wege.karte(kennung) }.getOrNull()?.let { leitstellen = it }
        }

        welt.standLaden()

        // Wer schon gegründet hat, hat hier nichts mehr zu tun.
        if (welt.zustand.value.hatLeitstelle) {
            beiArbeitsplatz()
            return@LaunchedEffect
        }

        // Ein Netzfehler ist kein „noch keine Leitstelle" — sonst gründete man womöglich
        // ein zweites Mal. Er steht mit „Erneut versuchen" da.
        if (welt.zustand.value.standFehler != null) return@LaunchedEffect

        laedt = false
        fremde.join()
    }

    val bereit = gewaehltLat != null && gewaehltLon != null && name.trim().length >= 3

    fun stadtSuchen() {
        val stadt = stadtFinden(suche)
        sucheLeer = stadt == null && suche.isNotBlank()
        if (stadt == null) return

        suche = stadt.name
        gewaehltLat = stadt.lat
        gewaehltLon = stadt.lon
        fehler = null

        // Die Maske liegt als Blatt über den unteren 60 % — die Stadt soll in der Mitte des
        // freien Streifens darüber stehen, nicht unter dem Formular, aus dem man sie sucht.
        val rand = with(dichte) { Kartenrand(unten = blatthoehe.toPx()) }
        karte.hinschauen(stadt.lat, stadt.lon, 11.0, rand)
    }

    fun gruenden() {
        val k = kennung ?: return
        val lat = gewaehltLat ?: return
        val lon = gewaehltLon ?: return
        sendet = true
        fehler = null
        bereich.launch {
            runCatching { welt.wege.leitstelleGruenden(k, name.trim(), lat, lon) }
                .onSuccess {
                    welt.standLaden()
                    beiArbeitsplatz()
                }
                .onFailure { fehler = it.message ?: "Die Gründung ging nicht." }
            sendet = false
        }
    }

    val marken = buildList {
        leitstellen.forEach { l ->
            add(
                Kartenmarke(
                    id = "ls-${l.besitzerId}",
                    lat = l.lat,
                    lon = l.lon,
                    ebene = 1,
                    trefferDp = 16f,
                    // Besitzer und Stufe nur, wenn sie mitgekommen sind — wer sich in der
                    // Welt nicht zeigt, schickt beides nicht.
                    beiTipp = {
                        hinweis = listOfNotNull(
                            l.name,
                            l.besitzer,
                            l.stufe?.let { "Stufe $it" },
                        ).joinToString(" · ")
                    },
                ) { ort, _ ->
                    drawCircle(Farben.TextSehrLeise.copy(alpha = 0.7f), radius = 6.dp.toPx(), center = ort)
                    drawCircle(Farben.RandHell, radius = 6.dp.toPx(), center = ort, style = Stroke(1.dp.toPx()))
                },
            )
        }
        val lat = gewaehltLat
        val lon = gewaehltLon
        if (lat != null && lon != null) {
            add(Kartenmarke(id = "eigen", lat = lat, lon = lon, ebene = 10) { ort, pinsel ->
                standortstern(ort, pinsel)
            })
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Farben.BgTief)) {
        Kartenflaeche(
            zustand = karte,
            modifier = Modifier.fillMaxSize(),
            marken = marken,
            beiKartentipp = { lat, lon ->
                hinweis = null
                if (!laedt) {
                    gewaehltLat = lat
                    gewaehltLon = lon
                    fehler = null
                }
            },
            randOben = oben,
            randUnten = blatthoehe,
            freigabeAbdunkeln = true,
            zeichnenUnten = { pinsel -> ortsnamenZeichnen(pinsel) },
        ) {
            hinweis?.let { text ->
                Text(
                    text = text,
                    style = Schrift.Klein,
                    color = Farben.Text,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = oben + Abstand.Klein, start = Abstand.Klein, end = 120.dp)
                        .background(Farben.Flaeche.copy(alpha = 0.94f), RoundedCornerShape(9.dp))
                        .border(1.dp, Farben.Rand, RoundedCornerShape(9.dp))
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                )
            }
        }

        // ------------------------------------------------------------- Maske
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = blatthoehe)
                .background(
                    Brush.verticalGradient(
                        listOf(Farben.FlaecheHoch.copy(alpha = 0.95f), Farben.Flaeche.copy(alpha = 0.97f)),
                    ),
                    RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                )
                .border(1.dp, Farben.RandHell, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .verticalScroll(rememberScrollState())
                .padding(start = Abstand.Gross, end = Abstand.Gross, top = Abstand.Gross, bottom = Abstand.Gross + unten),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PAGERSPASS · WORLD",
                    style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG, letterSpacing = 0.08.em),
                    color = Farben.Amber,
                    modifier = Modifier
                        .background(Farben.HauchAmber, RoundedCornerShape(999.dp))
                        .border(1.dp, Farben.AmberTief, RoundedCornerShape(999.dp))
                        .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
                )
                Box(Modifier.weight(1f))
                Textweg("Zurück", beiZurueck)
            }

            Text(text = "Deine Leitstelle in der Welt", style = Schrift.Schlagzeile, color = Farben.Text)

            Text(
                text = "Eine Karte, alle Spieler. Wähle einen Ort — er entscheidet über jede " +
                    "Anfahrt und jeden Nachbarn. Es gibt keinen Mindestabstand: Deine " +
                    "Heimatstadt geht auch dann, wenn dort schon jemand sitzt.",
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )

            val standFehler = zustand.standFehler
            if (laedt && standFehler != null) {
                Text(text = standFehler, style = Schrift.MonoKlein, color = Farben.SignalHell)
                Knopf("Erneut versuchen", { versuch++ }, kompakt = true)
            } else if (laedt) {
                Text(text = "Wird geladen …", style = Schrift.Klein, color = Farben.TextLeise)
            }

            if (!laedt && leitstellen.isNotEmpty()) {
                Text(
                    text = "${leitstellen.size} " +
                        (if (leitstellen.size == 1) "Leitstelle steht" else "Leitstellen stehen") +
                        " schon auf der Karte — die grauen Punkte.",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
            }

            if (!laedt) {
                Feld(
                    wert = name,
                    beiAenderung = { name = it.take(60) },
                    etikett = "Name der Leitstelle",
                    platzhalter = "Leitstelle Musterstadt",
                )

                // Die Ansicht steht vor der Suche: Erst sucht man sich den Untergrund aus,
                // auf dem man den Ort erkennt — im Luftbild sieht man die eigene Wache.
                Etikett("Kartenansicht")
                Segment(
                    seiten = Kartenstil.entries,
                    gewaehlt = karte.stil,
                    beiWahl = { s ->
                        karte.stil = s
                        if (karte.zoom > s.maxZoom) karte.zoom = s.maxZoom.toDouble()
                        bereich.launch { ablage.karteStilSetzen(s.name) }
                    },
                    aufschrift = { it.titel },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Die Suche gehört zum Standort: Sie steht direkt über der Zeile, die sagt,
                // ob einer gesetzt ist.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Feld(
                        wert = suche,
                        beiAenderung = {
                            suche = it
                            sucheLeer = false
                        },
                        etikett = "Stadt suchen",
                        platzhalter = "z. B. Kassel",
                        weiterTaste = ImeAction.Search,
                        modifier = Modifier.weight(1f),
                    )
                    Knopf("Hin", { stadtSuchen() })
                }

                val vorschlaege = staedteMit(suche).filter { it.name != suche }
                if (vorschlaege.isNotEmpty()) {
                    Pillenreihe {
                        vorschlaege.forEach { s ->
                            Pille(
                                aufschrift = s.name,
                                an = false,
                                beiDruck = {
                                    suche = s.name
                                    stadtSuchen()
                                },
                            )
                        }
                    }
                }

                if (sucheLeer) {
                    Text(
                        text = "Die Stadt kenne ich nicht — zoom hinein und tipp auf die Karte.",
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                    )
                }

                val lat = gewaehltLat
                val lon = gewaehltLon
                if (lat != null && lon != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Weltzeichen.Haken,
                            contentDescription = null,
                            tint = Farben.GruenHell,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(text = "Standort gesetzt —", style = Schrift.Klein, color = Farben.Text)
                        Text(
                            text = String.format(Locale.US, "%.4f, %.4f", lat, lon),
                            style = Schrift.MonoKlein,
                            color = Farben.Text,
                        )
                    }
                } else {
                    Text(
                        text = "Noch kein Standort — such eine Stadt oder tipp auf die Karte.",
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                    )
                }

                // Ohne Freigabe ist die Karte schwarz bis auf die Städtenamen. Das gehört
                // gesagt, sonst sieht die Seite kaputt aus, wo nur die Einwilligung fehlt.
                if (karte.freigabe == "nein") {
                    Text(
                        text = "Kartenhintergrund ist aus (Konto → Privatsphäre).",
                        style = Schrift.Winzig,
                        color = Farben.TextSehrLeise,
                    )
                }

                Absage(fehler)

                Knopf(
                    aufschrift = if (sendet) "Wird gegründet …" else "Leitstelle gründen",
                    beiDruck = { gruenden() },
                    art = Knopfart.Haupt,
                    aktiv = bereit && !sendet,
                    breit = true,
                )

                // Die Zahl ist `Weltpreise.Startguthaben` — über die Schnittstelle vorher
                // nicht zu haben, weil `/stand` erst mit einer Leitstelle antwortet.
                Text(
                    text = "Du startest mit 25 000 Welt-Credits. Davon kaufst du Wachen und " +
                        "Fahrzeuge; was du an Lagen verdienst, kommt dazu.",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
            }
        }
    }
}

/** Der Stern am gewählten Standort — gut sichtbar auch auf dem Luftbild. */
private fun DrawScope.standortstern(ort: Offset, pinsel: Kartenpinsel) {
    drawCircle(Farben.AufFarbe, radius = 16.dp.toPx(), center = ort)
    drawCircle(Farben.Amber, radius = 14.dp.toPx(), center = ort)
    val stern = pinsel.messer.measure(
        "★",
        TextStyle(color = Farben.AufFarbe, fontSize = 16.sp, fontWeight = FontWeight.Bold),
    )
    drawText(stern, topLeft = Offset(ort.x - stern.size.width / 2f, ort.y - stern.size.height / 2f))
}

/**
 * Die Städtenamen — bei jeder Zoomstufe neu verteilt: Die Liste ist nach Größe geordnet,
 * und ein Name, der einen schon gesetzten überdecken würde, bleibt weg (`ortsnamenAnhaengen`
 * im Web).
 */
private fun DrawScope.ortsnamenZeichnen(pinsel: Kartenpinsel) {
    val zustand = pinsel.zustand
    val gesetzt = mutableListOf<FloatArray>()

    for (s in STAEDTE) {
        if (zustand.zoom < abZoom(s.rang)) continue

        val p = zustand.bildschirm(s.lat, s.lon)
        if (p.x < -200 || p.y < -40 || p.x > size.width + 40 || p.y > size.height + 40) continue

        val breite = (s.name.length * (if (s.rang == 1) 9.5f else 8f) + 14f) * density
        val kasten = floatArrayOf(p.x - 5 * density, p.y - 9 * density, p.x - 5 * density + breite, p.y + 9 * density)
        if (gesetzt.any { ueberlappt(it, kasten) }) continue
        gesetzt += kasten

        val farbe = if (s.rang == 1) Farben.Text else Farben.TextLeise
        drawCircle(Farben.BgTief, radius = 4.5f * density, center = p)
        drawCircle(farbe, radius = 3f * density, center = p)

        val stil = TextStyle(
            color = farbe,
            fontSize = if (s.rang == 1) 14.5.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.04.em,
        )
        val text = pinsel.messer.measure(s.name.uppercase(), stil)
        val halo = pinsel.messer.measure(s.name.uppercase(), stil.copy(color = Farben.BgTief))
        val links = Offset(p.x + 7 * density, p.y - text.size.height / 2f)
        // Der Schatten ringsum — ohne ihn steht der Name auf einem Luftbild nicht ab.
        for (dx in listOf(-1f, 1f)) for (dy in listOf(-1f, 1f)) {
            drawText(halo, topLeft = links + Offset(dx * density, dy * density))
        }
        drawText(text, topLeft = links)
    }
}

private fun ueberlappt(a: FloatArray, b: FloatArray): Boolean =
    a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3]
