package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltfahrt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.mobil.fahrt
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.WeltLage
import de.pagerspass.pagerspass.netz.Weltpoi
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.karte.Kartenebene
import de.pagerspass.pagerspass.ui.karte.Kartenflaeche
import de.pagerspass.pagerspass.ui.karte.Kartenform
import de.pagerspass.pagerspass.ui.karte.Kartenmarke
import de.pagerspass.pagerspass.ui.karte.Kartenpinsel
import de.pagerspass.pagerspass.ui.karte.Kartenrand
import de.pagerspass.pagerspass.ui.karte.Kartenzustand
import de.pagerspass.pagerspass.ui.karte.kartenpille
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Die Ebenen der Weltkarte — `EBENEN_NAMEN` in `components/welt/ebenen.ts`, in dieser
 * Reihenfolge im Menü. Alle stehen anfangs an: Die gemeinsame Karte ist der Kern des
 * Modus, und wer sie zum ersten Mal öffnet, soll sehen, dass da noch andere sind.
 */
val WELTEBENEN: List<Pair<String, String>> = listOf(
    "lagen" to "Lagen",
    "eigene" to "Eigene Fahrzeuge",
    "wachen" to "Eigene Wachen",
    "pois" to "Eigene Punkte",
    "wege" to "Fahrweg beim Anklicken",
    "fremde" to "Fremde Fahrzeuge",
    "fremdeWachen" to "Fremde Wachen",
    "grosslage" to "Großeinsatz und Events",
)

/** Was auf der Karte gerade gezeichnet oder gesetzt wird — die Modi des Arbeitsplatzes. */
data class Kartenmodi(
    val bauModus: Boolean = false,
    /** `wache`, `zweig` oder `poi` — was am gesetzten Punkt entsteht. */
    val bauZweck: String = "wache",
    val bauort: Pair<Double, Double>? = null,
    val pfadModus: Boolean = false,
    val pfad: List<de.pagerspass.pagerspass.netz.WeltStreifenstation> = emptyList(),
    val pfadStart: Pair<Double, Double>? = null,
    val gelaendeModus: Boolean = false,
    val gelaende: List<Pair<Double, Double>> = emptyList(),
    val gelaendeStrecke: Boolean = false,
    /** Ob am Handy gerade eine Seite über der Karte liegt. */
    val seiteOffen: Boolean = false,
)

/**
 * Die Karte der einen Welt — übertragen aus `components/welt/Weltkarte.vue`.
 *
 * <b>Was gezeichnet wird: alles.</b> Eigene Wachen und Fahrzeuge, <em>alle</em> Lagen
 * (sie gehören niemandem), die Wachen und fahrenden Fahrzeuge der anderen, die
 * Leitstellen, die eigenen Punkte und Gelände, die Großlage der Woche und die
 * Bereitstellungsräume der Events. Fremde Fahrzeuge, die auf ihrer Wache stehen,
 * stecken in der Zahl an ihrem Haus.
 *
 * <b>Fünf Bilder je Sekunde.</b> Ein Takt von 200 ms lässt die Fahrzeugmarken zwischen
 * zwei Abrufen auf ihrer Straße gleiten (`Weltfahrt`) und die Blaulichter blitzen.
 *
 * <b>Ein Tipp auf die freie Karte</b> setzt je nach Modus eine Geländeecke, einen
 * Streifenpunkt oder den Bauplatz; ohne Modus nimmt er den gezeigten Fahrweg weg.
 */
@Composable
fun Weltkarte(
    welt: Welt,
    stand: Weltzustand,
    karte: Kartenzustand,
    katalog: Katalog?,
    ebenen: Map<String, Boolean>,
    beiEbene: (String, Boolean) -> Unit,
    modi: Kartenmodi,
    rand: Kartenrand,
    randOben: Dp,
    randUnten: Dp,
    beiLage: (String) -> Unit,
    beiBauort: (Double, Double) -> Unit,
    beiPfadpunkt: (Double, Double) -> Unit,
    beiGelaendeecke: (Double, Double) -> Unit,
    beiGelaendeZurueck: () -> Unit,
    beiGelaendeFertig: () -> Unit,
    beiWache: (String) -> Unit,
    beiFremdwache: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val packbilder by welt.packbilder.collectAsState()
    val jetzt by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(200)
            value = System.currentTimeMillis()
        }
    }

    var gewaehlt by remember { mutableStateOf<String?>(null) }
    var hinweis by remember { mutableStateOf<String?>(null) }

    // Erst wenn der Stand da ist, weiß die Karte, wohin sie schauen soll.
    val eigenerName = stand.stand?.name
    LaunchedEffect(eigenerName) {
        val s = stand.stand ?: return@LaunchedEffect
        karte.hinschauen(s.lat, s.lon, 11.0)
    }

    fun an(ebene: String) = ebenen[ebene] ?: true

    val vorlagen = remember(katalog) { katalog?.fahrzeuge.orEmpty().associateBy { it.id } }

    // ------------------------------------------------------------- Marken
    val marken = ArrayList<Kartenmarke>()
    val formen = ArrayList<Kartenform>()

    // Die sichtbare Fläche mit einem Viertel Rand — was dahinter fährt, wird nicht
    // gerechnet (`imBild` im Web). Im Zweifel sichtbar.
    val sicht = sichtfeld(karte)

    // Die Bauradien — einer je Ausrückebereich, nur beim Setzen und Zeichnen.
    val s = stand.stand
    if ((modi.bauModus || modi.gelaendeModus) && s != null) {
        val bereiche = s.bereiche.ifEmpty {
            listOf(de.pagerspass.pagerspass.netz.WeltBereich(lat = s.lat, lon = s.lon, bauradiusMeter = 35_000.0))
        }
        bereiche.forEach { b ->
            formen += Kartenform.Kreis(
                lat = b.lat,
                lon = b.lon,
                meter = if (b.bauradiusMeter > 0) b.bauradiusMeter else 35_000.0,
                fuellung = Color.Transparent,
                rand = Farben.Amber.copy(alpha = 0.7f),
                randbreite = 2.dp,
                strich = listOf(6f, 6f),
            )
        }
    }

    // Die eigenen Punkte und ihre Gelände.
    if (an("pois")) {
        stand.pois.filter { it.sichtbar }.forEach { p ->
            val wort = poiWort(p)
            if (p.gelaende.size >= (if (p.strecke) 2 else 3)) {
                formen += gelaendeform(p) { hinweis = wort }
            }
            if (!nurFlaeche(p)) {
                marken += Kartenmarke(
                    id = "poi-${p.id}",
                    lat = p.lat,
                    lon = p.lon,
                    ebene = -900,
                    trefferDp = 14f,
                    beiTipp = { hinweis = wort },
                ) { ort, _ -> objektsymbol(ort, Wappen.ton(p.id, p.farbe), p.aktiv) }
            }
        }
    }

    // Die fremden Leitstellen — dasselbe Zeichen wie die eigene, in Blau, unter allem.
    val eigeneKennung = stand.kennung
    stand.leitstellen.filter { it.besitzerId != eigeneKennung }.forEach { l ->
        val wort = listOfNotNull(l.name, l.stufe?.let { "Stufe $it" }, "${l.wachen} Wachen").joinToString(" · ")
        marken += Kartenmarke(
            id = "ls-${l.besitzerId}",
            lat = l.lat,
            lon = l.lon,
            ebene = -100,
            trefferDp = 14f,
            beiTipp = { hinweis = wort },
        ) { ort, _ -> leitstellensymbol(ort, Farben.Blau, deckkraft = 0.8f, groesse = 21f) }
    }

    // Die fremden Wachen — mit der Zahl der Fahrzeuge, die dort stehen.
    if (an("fremdeWachen")) {
        stand.betrieb?.fremdeWachen.orEmpty().forEach { w ->
            marken += Kartenmarke(
                id = "fw-${w.id}",
                lat = w.lat,
                lon = w.lon,
                ebene = 0,
                trefferDp = 16f,
                beiTipp = { beiFremdwache(w.id) },
            ) { ort, pinsel ->
                wachensymbol(ort, Farben.TextSehrLeise, wachenzeichen(w.art), deckkraft = 0.75f)
                if (w.fahrzeuge > 0) wachenzahl(ort, w.fahrzeuge, fremd = true, pinsel = pinsel)
            }
        }
    }

    // Die eigenen Wachen — und ein pulsender Ring, wo gerade ausgerückt wird.
    if (an("wachen")) {
        val ausrueckende = stand.betrieb?.fahrzeuge.orEmpty()
            .filter { it.lage == "Ausrueckt" }
            .groupingBy { it.wacheId }
            .eachCount()
        s?.wachen.orEmpty().forEach { w ->
            val raus = ausrueckende[w.id] ?: 0
            marken += Kartenmarke(
                id = "w-${w.id}",
                lat = w.lat,
                lon = w.lon,
                ebene = 100,
                trefferDp = 18f,
                beiTipp = { beiWache(w.id) },
            ) { ort, pinsel ->
                if (raus > 0) puls(ort, 15.dp.toPx(), Farben.Amber, 1_600L, jetzt)
                val farbe = wachenorganisation(w.art)?.let { organisationsfarbe(it) } ?: Farben.Amber
                wachensymbol(ort, farbe, wachenzeichen(w.art))
                if (w.belegt > 0) wachenzahl(ort, w.belegt, fremd = false, pinsel = pinsel)
            }
        }
    }

    // Die eigene Leitstelle und die Zweigstellen — mit Namensschild.
    if (s != null) {
        marken += Kartenmarke(id = "leitstelle", lat = s.lat, lon = s.lon, ebene = 400, trefferDp = 16f, beiTipp = {
            hinweis = "${s.name} · Deine Leitstelle"
        }) { ort, pinsel ->
            leitstellensymbol(ort, Farben.Amber)
            leitstellenschild(ort, s.name, pinsel)
        }
        s.bereiche.filter { it.istZweigstelle }.forEach { z ->
            marken += Kartenmarke(id = "zweig-${z.name}", lat = z.lat, lon = z.lon, ebene = 400, trefferDp = 16f, beiTipp = {
                hinweis = "${z.name} · Deine Zweigstelle — eigener Ausrückebereich"
            }) { ort, pinsel ->
                leitstellensymbol(ort, Farben.Amber)
                leitstellenschild(ort, z.name, pinsel)
            }
        }
    }

    // Die Lagen — sie gehören niemandem; die eigene atmet, bis sie freigegeben ist.
    if (an("lagen")) {
        stand.betrieb?.lagen.orEmpty().forEach { l ->
            marken += Kartenmarke(
                id = "l-${l.id}",
                lat = l.lat,
                lon = l.lon,
                ebene = 200,
                trefferDp = 20f,
                beiTipp = { beiLage(l.id) },
            ) { ort, pinsel -> lagensymbol(ort, l, pinsel, jetzt) }
        }
    }

    // Die Bereitstellungsräume der Events — vor dem Termin, damit man weiß, wohin.
    if (an("grosslage")) {
        stand.events.filter { it.zustand != "Vorbei" && it.brLat != null && it.brLon != null }.forEach { e ->
            val name = e.brName ?: "Bereitstellungsraum"
            val wort = "$name · ${e.titel}\n" + when {
                e.raumOffen -> "${e.bereitgestellt} Fahrzeuge dort"
                e.brOffenAb != null -> "Öffnet am ${datumzeit(e.brOffenAb)}"
                else -> "Noch geschlossen"
            }
            marken += Kartenmarke(
                id = "br-${e.id}",
                lat = e.brLat ?: 0.0,
                lon = e.brLon ?: 0.0,
                ebene = 600,
                trefferDp = 18f,
                beiTipp = { hinweis = wort },
            ) { ort, pinsel -> raumsymbol(ort, name, !e.raumOffen, pinsel) }
        }

        val g = stand.grosslage
        if (g != null && g.zustand != "Vorbei") {
            marken += Kartenmarke(id = "gross", lat = g.lat, lon = g.lon, ebene = 700, trefferDp = 20f, beiTipp = {
                hinweis = "${g.name}\n${g.bereitgestellt} Fahrzeuge bereitgestellt"
            }) { ort, pinsel -> grosssymbol(ort, g.name, g.zustand == "Laeuft", pinsel, jetzt) }
        }
    }

    // Die fahrenden Fahrzeuge — eigene und fremde, gestaffelt in der Kolonne.
    val kolonne = HashMap<String, Int>()
    fun rueckstand(schluessel: String): Double {
        val n = kolonne[schluessel] ?: 0
        kolonne[schluessel] = n + 1
        return n * Weltfahrt.ABSTAND_IN_KOLONNE
    }

    if (an("fremde")) {
        stand.betrieb?.fremde.orEmpty().filter { faehrt(it.lage) }.sortedBy { it.id }.forEach { f ->
            val zurueck = rueckstand(f.route ?: f.id)
            if (!imBild(sicht, f.lat, f.lon, f.zielLat, f.zielLon)) return@forEach
            val lage = welt.fahrt.fahrtLage(f.fahrt(), zurueck)
            val vorlage = vorlagen[f.vorlageId]
            val plan = rissplan(vorlage, f.typ)
            val icon = stand.pack[f.vorlageId]
            val bild = packbilder[f.vorlageId]
            val wort = (if (f.besitzer != null) "${f.funkrufname} · ${f.besitzer}" else f.funkrufname) + "\n${f.typ}"
            marken += Kartenmarke(
                id = "ff-${f.id}",
                lat = lage.lat,
                lon = lage.lon,
                ebene = 300,
                trefferDp = 18f,
                klumpen = true,
                beiTipp = { hinweis = wort },
            ) { ort, _ ->
                fahrzeugmarkeZeichnen(ort, lage.kurs, plan, f.blaulicht, jetzt, bild, icon, deckkraft = 0.55f)
            }
        }
    }

    if (an("eigene")) {
        stand.betrieb?.fahrzeuge.orEmpty().filter { faehrt(it.lage) }.sortedBy { it.id }.forEach { f ->
            val zurueck = rueckstand(f.route ?: f.id)
            if (!imBild(sicht, f.lat, f.lon, f.zielLat, f.zielLon) && f.id != gewaehlt) return@forEach
            val lage = welt.fahrt.fahrtLage(f.fahrt(), zurueck)
            val vorlage = vorlagen[f.vorlageId]
            val plan = rissplan(vorlage, f.typ)
            val icon = stand.pack[f.vorlageId]
            val bild = packbilder[f.vorlageId]
            // Die Kennzahl aus dem Funkrufnamen — „Uelzen 44/1" wird zu „44/1".
            val kurz = f.funkrufname.split(" ").lastOrNull() ?: f.funkrufname
            marken += Kartenmarke(
                id = "f-${f.id}",
                lat = lage.lat,
                lon = lage.lon,
                ebene = 500,
                trefferDp = 20f,
                klumpen = true,
                klumpenEigen = true,
                beiTipp = {
                    // Der Klick zeigt den Fahrweg — noch einmal, und er ist wieder weg.
                    gewaehlt = if (gewaehlt == f.id) null else f.id
                    hinweis = "${f.funkrufname}\n${f.typ}"
                },
            ) { ort, pinsel ->
                fahrzeugmarkeZeichnen(ort, lage.kurs, plan, f.blaulicht, jetzt, bild, icon)
                fahrzeugschild(ort, kurz, pinsel)
            }

            // Der Weg gehört genau einem Fahrzeug: dem angeklickten.
            if (f.id == gewaehlt && an("wege")) {
                val punkte = welt.fahrt.strecke(f.route).map { it[0] to it[1] }
                if (punkte.size >= 2) {
                    formen += Kartenform.Linie(
                        punkte = punkte,
                        farbe = if (f.lage == "Streife") Farben.Eigenposition else Farben.Amber,
                        breite = 4.dp,
                        strich = listOf(1f, 7f),
                        deckkraft = 0.65f,
                    )
                }
            }
        }
    }

    // Ein Weg, dessen Fahrzeug eingerückt ist, hat keinen Anker mehr.
    val gewaehltFaehrt = stand.betrieb?.fahrzeuge?.any { it.id == gewaehlt && faehrt(it.lage) } == true
    if (gewaehlt != null && !gewaehltFaehrt) gewaehlt = null

    // Der Streifenpfad, der gerade entsteht: Anfahrt einmal, Runde immer wieder.
    if (modi.pfadModus && modi.pfad.isNotEmpty()) {
        val punkte = modi.pfad.map { it.lat to it.lon }
        modi.pfadStart?.let { start ->
            formen += Kartenform.Linie(
                listOf(start, punkte.first()),
                Farben.Eigenposition,
                breite = 2.dp,
                strich = listOf(2f, 5f),
                deckkraft = 0.6f,
            )
        }
        if (punkte.size > 1) {
            formen += Kartenform.Linie(punkte + punkte.first(), Farben.Eigenposition, breite = 3.dp, strich = listOf(6f, 6f))
        }
        modi.pfad.forEachIndexed { i, p ->
            marken += Kartenmarke(id = "pfad-$i", lat = p.lat, lon = p.lon, ebene = 400, trefferDp = 16f, beiTipp = {
                hinweis = "${i + 1}. ${p.name}"
            }) { ort, pinsel -> nummer(ort, i + 1, pinsel) }
        }
    }

    // Das Gelände, das gerade entsteht — ab der dritten Ecke eine Fläche, davor eine Linie.
    if (modi.gelaendeModus && modi.gelaende.isNotEmpty()) {
        val punkte = modi.gelaende
        if (modi.gelaendeStrecke || punkte.size == 2) {
            if (punkte.size >= 2) {
                formen += Kartenform.Linie(
                    punkte,
                    Farben.AmberHell.copy(alpha = 0.9f),
                    breite = if (modi.gelaendeStrecke) 4.dp else 2.dp,
                    strich = listOf(5f, 5f),
                )
            }
        } else if (punkte.size >= 3) {
            formen += Kartenform.Flaeche(
                punkte,
                Farben.Amber.copy(alpha = 0.12f),
                Farben.AmberHell.copy(alpha = 0.9f),
                randbreite = 2.dp,
                strich = listOf(5f, 5f),
            )
        }
        punkte.forEachIndexed { i, (lat, lon) ->
            marken += Kartenmarke(id = "ecke-$i", lat = lat, lon = lon, ebene = 400) { ort, pinsel ->
                nummer(ort, i + 1, pinsel)
            }
        }
    }

    // Die Vorschau am gesetzten Bauplatz — das Zeichen dessen, was dort entsteht.
    val bauort = modi.bauort
    if (modi.bauModus && bauort != null) {
        marken += Kartenmarke(id = "bauvorschau", lat = bauort.first, lon = bauort.second, ebene = 300, beiTipp = {
            hinweis = when (modi.bauZweck) {
                "zweig" -> "Hier entsteht die zweite Leitstelle"
                "poi" -> "Hier entsteht dein Punkt"
                else -> "Hier entsteht die Wache"
            }
        }) { ort, _ ->
            val atem = 0.6f + 0.15f * sin(jetzt / 1000.0 * PI).toFloat()
            drawCircle(
                Farben.Amber.copy(alpha = atem),
                radius = 17.dp.toPx(),
                center = ort,
                style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
            )
            when (modi.bauZweck) {
                "zweig" -> leitstellensymbol(ort, Farben.Amber, deckkraft = atem)
                "poi" -> objektsymbol(ort, Farben.Amber)
                else -> wachensymbol(ort, Farben.Amber, "tor", deckkraft = atem)
            }
        }
    }

    Kartenflaeche(
        zustand = karte,
        modifier = modifier.fillMaxSize(),
        marken = marken,
        formen = formen,
        klumpenBisZoom = 9.0,
        beiKartentipp = { lat, lon ->
            hinweis = null
            // Der gezeigte Fahrweg geht mit — die natürliche Geste für „weg damit".
            gewaehlt = null
            when {
                modi.gelaendeModus -> beiGelaendeecke(lat, lon)
                modi.pfadModus -> beiPfadpunkt(lat, lon)
                modi.bauModus -> beiBauort(lat, lon)
            }
        },
        beiEinpassen = {
            // Passt den Ausschnitt auf den eigenen Bestand ein — Wachen und alles, was fährt.
            val punkte = ArrayList<Pair<Double, Double>>()
            s?.wachen.orEmpty().forEach { punkte += it.lat to it.lon }
            stand.betrieb?.fahrzeuge.orEmpty().filter { it.lage != "Wache" }.forEach {
                punkte += welt.fahrt.fahrzeugOrt(it.fahrt())
            }
            if (punkte.isEmpty() && s != null) punkte += s.lat to s.lon
            karte.zuschneiden(punkte, hoechstens = 13.0, rand = rand)
        },
        ebenen = WELTEBENEN.map { (id, name) ->
            Kartenebene(
                name = name,
                an = an(id),
                vorhanden = when (id) {
                    "lagen" -> stand.betrieb?.lagen.orEmpty().isNotEmpty()
                    "wachen" -> s?.wachen.orEmpty().isNotEmpty()
                    "pois" -> stand.pois.isNotEmpty()
                    "fremde" -> stand.betrieb?.fremde.orEmpty().isNotEmpty()
                    "fremdeWachen" -> stand.betrieb?.fremdeWachen.orEmpty().isNotEmpty()
                    "grosslage" -> stand.grosslage != null || stand.events.any { it.brLat != null }
                    else -> true
                },
                setzen = { beiEbene(id, it) },
            )
        },
        randOben = randOben,
        randUnten = randUnten,
    ) {
        Kartenhinweise(
            hinweis = hinweis,
            beiHinweisWeg = { hinweis = null },
            modi = modi,
            randOben = randOben,
            randUnten = randUnten,
            beiGelaendeZurueck = beiGelaendeZurueck,
            beiGelaendeFertig = beiGelaendeFertig,
        )
    }
}

/** Die Zeilen über der Karte: der Steckbrief der angetippten Marke, der Modushinweis, die Zeichenleiste. */
@Composable
private fun BoxScope.Kartenhinweise(
    hinweis: String?,
    beiHinweisWeg: () -> Unit,
    modi: Kartenmodi,
    randOben: Dp,
    randUnten: Dp,
    beiGelaendeZurueck: () -> Unit,
    beiGelaendeFertig: () -> Unit,
) {
    if (hinweis != null) {
        Text(
            text = hinweis,
            style = Schrift.Klein,
            color = Farben.Text,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = randOben + Abstand.Klein, start = Abstand.Klein, end = 150.dp)
                .widthIn(max = 280.dp)
                .background(Farben.Flaeche.copy(alpha = 0.95f), RoundedCornerShape(9.dp))
                .border(1.dp, Farben.Rand, RoundedCornerShape(9.dp))
                .clickable(onClick = beiHinweisWeg)
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        )
    }

    val zeigen = (modi.bauModus || modi.gelaendeModus || modi.pfadModus) && !modi.seiteOffen
    if (!zeigen) return

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(bottom = randUnten + Abstand.Klein, start = Abstand.Gross, end = Abstand.Gross),
    ) {
        Text(
            text = modustext(modi),
            style = Schrift.MonoKlein,
            color = Farben.Text,
            modifier = Modifier
                .background(Farben.Flaeche.copy(alpha = 0.95f), RoundedCornerShape(9.dp))
                .border(1.dp, Farben.AmberTief, RoundedCornerShape(9.dp))
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        )

        // Die Zeichenleiste — die beiden Griffe, die beim Tippen gebraucht werden: einer
        // zurück, einer hinüber ins Formular.
        if (modi.gelaendeModus) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf(
                    "Punkt zurück",
                    beiGelaendeZurueck,
                    kompakt = true,
                    aktiv = modi.gelaende.isNotEmpty(),
                )
                Knopf("Fertig", beiGelaendeFertig, art = Knopfart.Haupt, kompakt = true)
            }
        }
    }
}

/** Was der Hinweis sagt — drei Modi, und beim Bauen, was entsteht. */
private fun modustext(modi: Kartenmodi): String {
    if (modi.pfadModus) {
        val gesetzt = modi.pfad.size
        if (gesetzt == 0) return "Tipp die Stationen der Streife ab."
        return "$gesetzt ${if (gesetzt == 1) "Station" else "Stationen"} — höchstens acht."
    }

    if (modi.gelaendeModus) {
        val gesetzt = modi.gelaende.size
        val strecke = modi.gelaendeStrecke
        val noetig = if (strecke) 2 else 3
        if (gesetzt == 0) return "Tipp die ${if (strecke) "Strecke" else "Ecken des Geländes"} ab."
        if (gesetzt < noetig) return "$gesetzt von mindestens $noetig Punkten — weitertippen."
        val mass = if (strecke) {
            laengenwort(zuglaengeMeter(modi.gelaende))
        } else {
            flaechenwort(vieleckQuadratmeter(modi.gelaende))
        }
        return "$gesetzt Punkte · $mass"
    }

    return when (modi.bauZweck) {
        "zweig" -> "Tipp auf die Karte, wo die zweite Leitstelle stehen soll."
        "poi" -> "Tipp auf die Karte, wo dein Punkt liegen soll."
        else -> "Tipp auf die Karte, wo die Wache stehen soll."
    }
}

// --------------------------------------------------------------- Hilfen

/**
 * Ob dieses Fahrzeug eine eigene Marke bekommt: gezeichnet wird, was unterwegs ist. Wer
 * steht, steht unter einer Marke, die es schon gibt — auf der Wache unter dem Haus, an der
 * Lage unter deren Zahl, im Bereitstellungsraum unter dem Raum.
 */
private fun faehrt(lage: String): Boolean =
    lage != "Wache" && lage != "Ausrueckt" && lage != "VorOrt" && lage != "Bereitstellung"

/** Das Sichtfeld mit einem Viertel Rand — `null`, solange die Karte nicht gemessen ist. */
private fun sichtfeld(karte: Kartenzustand): DoubleArray? {
    if (karte.groesse.width == 0) return null
    val (nLat, wLon) = karte.ortVon(Offset(0f, 0f))
    val (sLat, oLon) = karte.ortVon(Offset(karte.groesse.width.toFloat(), karte.groesse.height.toFloat()))
    val dLat = (nLat - sLat) * 0.25
    val dLon = (oLon - wLon) * 0.25
    return doubleArrayOf(sLat - dLat, nLat + dLat, wLon - dLon, oLon + dLon)
}

/** Geprüft wird das Rechteck aus Ort und Ziel, nicht der Ort allein — die Marke steht dazwischen. */
private fun imBild(sicht: DoubleArray?, lat: Double, lon: Double, zielLat: Double, zielLon: Double): Boolean {
    if (sicht == null) return true
    return max(lat, zielLat) >= sicht[0] && minOf(lat, zielLat) <= sicht[1] &&
        max(lon, zielLon) >= sicht[2] && minOf(lon, zielLon) <= sicht[3]
}

/** Ob dieser Punkt nur als Fläche gezeichnet wird, ohne Haus — Wald, Feld, Strecke. */
private fun nurFlaeche(p: Weltpoi): Boolean =
    p.gelaendeart != null && p.gelaende.size >= (if (p.strecke) 2 else 3)

private fun poiWort(p: Weltpoi): String = buildString {
    append(p.name)
    append("\n").append(p.artText)
    if (p.betroffene > 0) append("\netwa ${p.betroffene} Personen")
    if (!p.nachtsBelegt) append("\nnachts leer")
    if (p.gewicht > 1) append("\nHäufigkeit ${p.gewicht}")
    if (p.organisationen.isNotEmpty()) append("\n").append(p.organisationen.joinToString(", "))
    if (!p.aktiv) append("\nstillgelegt — hier entstehen keine Lagen")
}

/** Der Ton eines Geländes — vier Sorten, vier Töne aus der Palette. */
private fun gelaendeton(art: String?): Color = when (art) {
    "Wald" -> Farben.GruenHell
    "Feld" -> Farben.AmberHell
    "Autobahn" -> Farben.BlauHell
    "Schiene" -> Farben.TextSehrLeise
    else -> Farben.Amber
}

private fun gelaendeform(p: Weltpoi, beiTipp: () -> Unit): Kartenform {
    val ton = gelaendeton(p.gelaendeart)
    val ecken = p.gelaende.map { it.lat to it.lon }
    val tippbar = if (nurFlaeche(p)) beiTipp else null
    return if (p.strecke) {
        Kartenform.Linie(
            ecken,
            ton,
            breite = 4.dp,
            strich = if (p.aktiv) null else listOf(4f, 4f),
            deckkraft = if (p.aktiv) 0.85f else 0.3f,
        )
    } else {
        Kartenform.Flaeche(
            ecken,
            ton.copy(alpha = if (p.aktiv) 0.08f else 0.03f),
            ton.copy(alpha = if (p.aktiv) 0.65f else 0.3f),
            randbreite = 2.dp,
            strich = if (p.aktiv) null else listOf(4f, 4f),
            beiTipp = tippbar,
        )
    }
}

/** Ein pulsender Ring — die ausrückende Wache, die eigene Lage. */
private fun DrawScope.puls(ort: Offset, radius: Float, farbe: Color, periode: Long, jetzt: Long) {
    val phase = (jetzt % periode).toFloat() / periode
    val welle = 0.5f - 0.5f * kotlin.math.cos(phase * 2f * PI.toFloat())
    val skala = 0.82f + 0.36f * welle
    drawCircle(
        farbe.copy(alpha = 0.9f - 0.55f * welle),
        radius = radius * skala,
        center = ort,
        style = Stroke(2.dp.toPx()),
    )
}

/** Die Zahl an einer Wache — rechts oben am Haus. */
private fun DrawScope.wachenzahl(ort: Offset, zahl: Int, fremd: Boolean, pinsel: Kartenpinsel) {
    val text = zahl.toString()
    val breite = max(20.dp.toPx(), text.length * 7.dp.toPx() + 8.dp.toPx())
    val mitte = Offset(ort.x + 4.dp.toPx() + breite / 2f, ort.y - 18.dp.toPx() + 10.dp.toPx())
    kartenpille(
        messer = pinsel.messer,
        text = text,
        mitte = mitte,
        flaeche = if (fremd) Farben.FlaecheAktiv else Farben.Amber,
        schrift = if (fremd) Farben.TextLeise else Farben.AufFarbe,
        hoehe = 20.dp,
        groesse = 12.sp,
    )
}

/** Das Namensschild unter einer Leitstelle. */
private fun DrawScope.leitstellenschild(ort: Offset, name: String, pinsel: Kartenpinsel) {
    val kurz = if (name.length > 26) name.take(25) + "…" else name
    val mitte = Offset(ort.x, ort.y + 18.dp.toPx() + 9.dp.toPx())
    kartenpille(
        messer = pinsel.messer,
        text = kurz,
        mitte = mitte,
        flaeche = Farben.BgTief.copy(alpha = 0.78f),
        schrift = Farben.AmberHell,
        hoehe = 18.dp,
        groesse = 12.sp,
        fett = false,
        saum = Farben.AmberTief.copy(alpha = 0.7f),
    )
}

/** Das Schild unter einem eigenen Fahrzeug — die Kennzahl. */
private fun DrawScope.fahrzeugschild(ort: Offset, kurz: String, pinsel: Kartenpinsel) {
    val mitte = Offset(ort.x, ort.y + 26.dp.toPx() + 9.dp.toPx())
    kartenpille(
        messer = pinsel.messer,
        text = kurz,
        mitte = mitte,
        flaeche = Farben.BgTief.copy(alpha = 0.88f),
        schrift = Farben.Text,
        hoehe = 18.dp,
        groesse = 12.sp,
        fett = false,
        saum = Farben.RandHell.copy(alpha = 0.55f),
    )
}

/** Eine nummerierte Station — Streifenpfad und Geländeecken, dieselbe Geste, dieselbe Anzeige. */
private fun DrawScope.nummer(ort: Offset, n: Int, pinsel: Kartenpinsel) {
    val r = 14.dp.toPx()
    drawCircle(Farben.Flaeche, radius = r, center = ort)
    drawCircle(Farben.Eigenposition, radius = r, center = ort, style = Stroke(2.dp.toPx()))
    val text = pinsel.messer.measure(
        n.toString(),
        TextStyle(color = Farben.Eigenposition, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
    )
    drawText(text, topLeft = Offset(ort.x - text.size.width / 2f, ort.y - text.size.height / 2f))
}

/**
 * Das Symbol einer Lage: Priorität färbt, eigener Erstzugriff bekommt einen Ring, und die
 * Lage, die noch bei einem selbst liegt, atmet.
 */
private fun DrawScope.lagensymbol(ort: Offset, l: WeltLage, pinsel: Kartenpinsel, jetzt: Long) {
    val erledigt = l.zustand == "Erledigt"
    val gedeckt = (eingetroffen(l) ?: -1) >= l.gefordertFahrzeuge
    val meine = l.zustaendig && !l.freigegeben

    val grund = when {
        erledigt -> Farben.TextLeise
        l.prioritaet >= 3 -> Farben.Signal
        l.prioritaet == 2 -> Farben.FmsAnfahrt
        else -> Farben.Blau
    }
    val deckkraft = when {
        erledigt -> 0.75f
        gedeckt -> 0.45f
        else -> 1f
    }

    val text = pinsel.messer.measure(
        deckungstext(l),
        TextStyle(color = Farben.AufFarbe, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
    )
    val h = 22.dp.toPx()
    val b = max(32.dp.toPx(), text.size.width + 8.dp.toPx())
    val links = Offset(ort.x - b / 2f, ort.y - h / 2f)
    val ecke = CornerRadius(h / 2f, h / 2f)

    // Die eigene, nicht freigegebene Lage atmet — sie ist die einzige, für die niemand
    // sonst einspringen kann.
    if (meine) {
        val phase = (jetzt % 2_400L).toFloat() / 2_400f
        val welle = 0.5f - 0.5f * kotlin.math.cos(phase * 2f * PI.toFloat())
        val skala = 1f + 0.28f * welle
        val bb = (b + 8.dp.toPx()) * skala
        val hh = (h + 8.dp.toPx()) * skala
        drawRoundRect(
            Farben.Amber.copy(alpha = 0.55f * (1f - welle)),
            topLeft = Offset(ort.x - bb / 2f, ort.y - hh / 2f),
            size = Size(bb, hh),
            cornerRadius = CornerRadius(hh / 2f, hh / 2f),
        )
    }

    // Der Saum: dunkel ringsum, dazu Amber für den eigenen Erstzugriff und die Tageslage,
    // Signal für die Lagen der Großlage.
    val saum = when {
        erledigt -> Farben.BgTief
        l.ausGrosslage -> Farben.Signal
        l.istTageslage -> Farben.Amber
        else -> Farben.BgTief
    }
    if (l.eigene && !erledigt && !l.ausGrosslage && !l.istTageslage) {
        val r = 4.dp.toPx()
        drawRoundRect(
            Farben.Amber.copy(alpha = deckkraft),
            topLeft = links - Offset(r, r),
            size = Size(b + 2 * r, h + 2 * r),
            cornerRadius = CornerRadius(h / 2f + r, h / 2f + r),
        )
    }
    val s = 2.dp.toPx()
    drawRoundRect(
        saum.copy(alpha = deckkraft),
        topLeft = links - Offset(s, s),
        size = Size(b + 2 * s, h + 2 * s),
        cornerRadius = CornerRadius(h / 2f + s, h / 2f + s),
    )
    drawRoundRect(grund.copy(alpha = deckkraft), topLeft = links, size = Size(b, h), cornerRadius = ecke)
    if (erledigt) {
        val a = 4.dp.toPx()
        drawRoundRect(
            Farben.RandHell,
            topLeft = links - Offset(a, a),
            size = Size(b + 2 * a, h + 2 * a),
            cornerRadius = CornerRadius(h / 2f + a, h / 2f + a),
            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
        )
    }
    drawText(
        text,
        topLeft = Offset(ort.x - text.size.width / 2f, ort.y - text.size.height / 2f),
        alpha = deckkraft,
    )
}

/** Der Bereitstellungsraum eines Events: gestrichelter Kreis, Sammelzeichen, Schild. */
private fun DrawScope.raumsymbol(ort: Offset, name: String, zu: Boolean, pinsel: Kartenpinsel) {
    val r = 16.dp.toPx()
    val deckkraft = if (zu) 0.75f else 1f
    drawCircle(Farben.FlaecheAktiv.copy(alpha = deckkraft), radius = r, center = ort)
    drawCircle(
        (if (zu) Farben.RandHell else Farben.Amber).copy(alpha = deckkraft),
        radius = r,
        center = ort,
        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))),
    )
    raumzeichen(ort, (if (zu) Farben.TextLeise else Farben.AmberHell).copy(alpha = deckkraft))
    kartenpille(
        messer = pinsel.messer,
        text = name,
        mitte = Offset(ort.x, ort.y + 22.dp.toPx() + 9.dp.toPx()),
        flaeche = if (zu) Farben.RandHell else Farben.Amber,
        schrift = if (zu) Farben.Text else Farben.AufFarbe,
        hoehe = 18.dp,
        groesse = 12.sp,
        fett = false,
    )
}

/** Die Großlage der Woche: Signalring, Ausrufezeichen, Schild — und sie pulsiert, solange sie läuft. */
private fun DrawScope.grosssymbol(ort: Offset, name: String, laeuft: Boolean, pinsel: Kartenpinsel, jetzt: Long) {
    val r = 19.dp.toPx()
    if (laeuft) {
        val phase = (jetzt % 2_000L).toFloat() / 2_000f
        drawCircle(
            Farben.HauchSignal.copy(alpha = 0.5f * (1f - phase)),
            radius = r + 12.dp.toPx() * phase,
            center = ort,
        )
    }
    drawCircle(Farben.FlaecheAktiv, radius = r, center = ort)
    drawCircle(Farben.Signal, radius = r, center = ort, style = Stroke(3.dp.toPx()))
    val zeichen = pinsel.messer.measure(
        "!",
        TextStyle(color = Farben.SignalHell, fontSize = 21.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    )
    drawText(zeichen, topLeft = Offset(ort.x - zeichen.size.width / 2f, ort.y - zeichen.size.height / 2f))
    kartenpille(
        messer = pinsel.messer,
        text = name,
        mitte = Offset(ort.x, ort.y + 26.dp.toPx() + 9.dp.toPx()),
        flaeche = Farben.Signal,
        schrift = Farben.AufFarbe,
        hoehe = 18.dp,
        groesse = 12.sp,
        fett = false,
    )
}

/** Datum und Uhrzeit kurz — „Sa., 04.10., 14:00". */
fun datumzeit(iso: String?): String {
    val ms = de.pagerspass.pagerspass.mobil.weltzeit(iso) ?: return iso.orEmpty()
    val format = java.text.SimpleDateFormat("EE, dd.MM., HH:mm", java.util.Locale.GERMANY)
    return format.format(java.util.Date(ms))
}
