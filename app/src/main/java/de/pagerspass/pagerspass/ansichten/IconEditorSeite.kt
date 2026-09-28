package de.pagerspass.pagerspass.ansichten

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Iconbild
import de.pagerspass.pagerspass.mobil.Iconstand
import de.pagerspass.pagerspass.netz.Blaulicht
import de.pagerspass.pagerspass.netz.Icontyp
import de.pagerspass.pagerspass.netz.Packicon
import de.pagerspass.pagerspass.netz.iconAdresse
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Regler
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.bildVon
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Die Icons eines Packs — übertragen aus `web/src/views/IconEditorView.vue`, in
 * seiner Handyform: eine Liste, aus der sich der gewählte Typ als Seite
 * darüberlegt. Zwei Spalten von je zehn Zentimetern gibt es am Handy nicht, und
 * eine Grafik, die sich unter einer Liste versteckt, findet niemand.
 *
 * <b>Hochgeladen wird, nicht gezeichnet.</b> Wer ein Fahrzeug malen will, hat
 * dafür Werkzeuge, die tausendmal besser sind; was hier bleibt, ist der Weg, der
 * zählt: Bild aussuchen, ansehen, übernehmen — und die Blaulichter setzen.
 */
@Composable
fun IconEditorSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Iconstand = Iconstand(),
    beiLaden: () -> Unit = {},
    beiWaehlen: (String?) -> Unit = {},
    beiHochladen: (Uri) -> Unit = {},
    beiDrehung: () -> Unit = {},
    beiBlaulicht: (punkte: List<Blaulicht>, wahl: Int?, meldung: String?) -> Unit = { _, _, _ -> },
    beiLichtwahl: (Int?) -> Unit = {},
    beiEntfernen: () -> Unit = {},
    beiFehler: (String?) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    // Das Zurück des Geräts führt vom Typ zurück in die Liste — wie „← Liste".
    BackHandler(enabled = stand.gewaehlt != null) { beiWaehlen(null) }

    val typ = stand.typen.firstOrNull { it.vorlageId == stand.gewaehlt }

    Seite(modifier = modifier, unterrand = unterrand) {
        Etikett("Fahrzeug-Icons")
        Seitenkopf(
            titel = stand.packname.ifBlank { "Icon-Pack" },
            knoepfe = { Knopf("Alle Packs", beiZurueck, kompakt = true) },
        )

        if (!stand.laedt) {
            SehrLeise(
                "${stand.icons.size} von ${stand.typen.size} Fahrzeugtypen belegt · " +
                    "${iconKilobyte(stand.packBytes)} von ${iconKilobyte(stand.grenzen?.maxBytesJePack ?: 0L)} KB",
            )
        }

        stand.fehler?.let { Fehlerzeile(it) }

        if (stand.laedt) {
            Ladezeile()
            return@Seite
        }

        if (typ == null) {
            Typenliste(stand, beiWaehlen)
        } else {
            Typenblatt(
                stand = stand,
                typ = typ,
                icon = stand.icons[typ.vorlageId],
                beiWaehlen = beiWaehlen,
                beiHochladen = beiHochladen,
                beiDrehung = beiDrehung,
                beiBlaulicht = beiBlaulicht,
                beiLichtwahl = beiLichtwahl,
                beiEntfernen = beiEntfernen,
                beiFehler = beiFehler,
            )
        }
    }
}

// ------------------------------------------------------------------ Die Liste

/**
 * Die Fahrzeugtypen, nach Organisation und Kategorie gruppiert — dieselben
 * Gruppen wie in der Kaufliste der Welt. Bei hundert Zeilen sind Suche und
 * „Nur belegte" keine Zier.
 */
@Composable
private fun Typenliste(stand: Iconstand, beiWaehlen: (String?) -> Unit) {
    var suche by rememberSaveable { mutableStateOf("") }
    var nurBelegte by rememberSaveable { mutableStateOf(false) }

    val wort = suche.trim().lowercase()
    val gefiltert = stand.typen.filter { t ->
        if (nurBelegte && !stand.icons.containsKey(t.vorlageId)) return@filter false
        // Die Beschreibung zählt mit: Wer „Drehleiter" tippt, findet sonst nichts,
        // weil das Fahrzeug im Katalog „DLK 23/12" heißt.
        wort.isEmpty() ||
            t.typ.lowercase().contains(wort) ||
            t.kategorie.lowercase().contains(wort) ||
            t.organisation.lowercase().contains(wort) ||
            t.beschreibung.lowercase().contains(wort)
    }
    val gruppen = gefiltert.groupBy { "${it.organisation} · ${it.kategorie}" }

    Kasten(abstandInnen = Abstand.Klein) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Feld(
                wert = suche,
                beiAenderung = { suche = it },
                platzhalter = "Fahrzeugtyp suchen …",
                modifier = Modifier.weight(1f),
            )
            Pille("Nur belegte", an = nurBelegte, beiDruck = { nurBelegte = !nurBelegte })
        }

        if (gefiltert.isEmpty()) SehrLeise("Dazu gibt es keinen Fahrzeugtyp.")

        gruppen.forEach { (titel, liste) ->
            Ueberschrift(titel, Modifier.padding(top = Abstand.Klein))
            liste.forEach { t ->
                Typzeile(t, stand.icons[t.vorlageId], stand.server) { beiWaehlen(t.vorlageId) }
            }
        }
    }
}

@Composable
private fun Typzeile(typ: Icontyp, icon: Packicon?, server: String, beiDruck: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = beiDruck, role = Role.Button)
            .padding(vertical = Abstand.Winzig),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(Farben.BgTief, Rundung.Winzig),
        ) {
            if (icon != null) {
                val bild by bildVon(iconAdresse(server, icon.bild))
                bild?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(36.dp),
                    )
                }
            } else {
                Fahrzeugsinnbild(typ.organisation, groesse = 36.dp, typ = typ.typ, quer = true)
            }
        }
        Text(
            text = typ.typ,
            style = Schrift.Normal,
            color = Farben.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        when {
            icon?.dreht == true -> Text("⟳", style = Schrift.Normal, color = Farben.Amber)
            icon != null -> Text("●", style = Schrift.Klein, color = Farben.Amber)
        }
    }
}

// ------------------------------------------------------------ Der gewählte Typ

@Composable
private fun Typenblatt(
    stand: Iconstand,
    typ: Icontyp,
    icon: Packicon?,
    beiWaehlen: (String?) -> Unit,
    beiHochladen: (Uri) -> Unit,
    beiDrehung: () -> Unit,
    beiBlaulicht: (List<Blaulicht>, Int?, String?) -> Unit,
    beiLichtwahl: (Int?) -> Unit,
    beiEntfernen: () -> Unit,
    beiFehler: (String?) -> Unit,
) {
    var entfernenFragen by remember { mutableStateOf(false) }
    val bildwahl = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { quelle ->
        if (quelle != null) beiHochladen(quelle)
    }

    val punkte = icon?.blaulichter.orEmpty()
    val maxPunkte = stand.grenzen?.maxBlaulichter ?: 8
    val vorgabeArt = blitzVorgabe(typ.typ, typ.organisation)
    val lichtwahl = stand.lichtwahl?.takeIf { it < punkte.size }
    val aktivesLicht = lichtwahl?.let { punkte.getOrNull(it) }

    /** Ein Licht ändern — beim Vergrößern rückt die Mitte ein, damit es auf dem Bild bleibt. */
    fun lichtAendern(umbau: (Blaulicht) -> Blaulicht) {
        val index = lichtwahl ?: return
        val licht = aktivesLicht ?: return
        val geaendert = umbau(licht).let { g -> innerhalbMitGroesse(g, g.x, g.y) }
        beiBlaulicht(
            punkte.mapIndexed { i, p -> if (i == index) geaendert else p },
            index,
            "Licht ${index + 1} gespeichert.",
        )
    }

    Kasten(abstandInnen = Abstand.Normal) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Knopf("← Liste", { beiWaehlen(null) }, kompakt = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typ.typ,
                    style = Schrift.Gross,
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SehrLeise(typ.organisation)
            }
        }
        if (typ.beschreibung.isNotBlank()) SehrLeise(typ.beschreibung)

        // Zwei Bühnen: was gilt, und was gälte ohne Icon. Der Vergleich ist der Punkt.
        Etikett("Dein Icon")
        if (icon != null) {
            Lichtbuehne(
                stand = stand,
                icon = icon,
                punkte = punkte,
                lichtwahl = lichtwahl,
                maxPunkte = maxPunkte,
                beiBlaulicht = beiBlaulicht,
                beiLichtwahl = beiLichtwahl,
                beiFehler = beiFehler,
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .background(Farben.BgTief, Rundung.Klein)
                    .border(1.dp, Farben.Rand, Rundung.Klein),
            ) {
                SehrLeise("noch keins")
            }
        }

        Etikett("Ohne Icon")
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .background(Farben.BgTief, Rundung.Klein)
                .border(1.dp, Farben.Rand, Rundung.Klein),
        ) {
            // Der Riss des Spiels — das, was ohne eigenes Icon zu sehen wäre.
            Fahrzeugsinnbild(typ.organisation, groesse = 72.dp, typ = typ.typ)
        }

        stand.hinweis?.let { Text(it, style = Schrift.Klein, color = Farben.GruenHell) }

        Pillenreihe {
            Knopf(
                if (icon != null) "Bild ersetzen" else "Bild hochladen",
                {
                    bildwahl.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                art = Knopfart.Haupt,
                aktiv = !stand.laeuft,
            )
            if (icon != null) {
                Knopf("Entfernen", { entfernenFragen = true }, art = Knopfart.Gefahr, aktiv = !stand.laeuft)
            }
        }

        // Der Rotationsschalter — er steht aus, und das ist die wichtigere Hälfte:
        // Ein hochgeladenes Bild hat meist eine Leserichtung und keine Fahrtrichtung.
        if (icon != null) {
            Schalterzeile(
                titel = "Mit dem Kurs drehen",
                an = icon.dreht,
                beiWechsel = { beiDrehung() },
                aktiv = !stand.laeuft,
                unterzeile = if (icon.dreht) {
                    "Das Icon zeigt in Fahrtrichtung — richtig für eine Zeichnung von oben."
                } else {
                    "Das Icon steht immer gerade, egal wohin das Fahrzeug fährt."
                },
            )

            Lichtverwaltung(
                stand = stand,
                punkte = punkte,
                maxPunkte = maxPunkte,
                lichtwahl = lichtwahl,
                aktivesLicht = aktivesLicht,
                vorgabeArt = vorgabeArt,
                beiLichtwahl = beiLichtwahl,
                beiBlaulicht = beiBlaulicht,
                beiLichtAendern = { lichtAendern(it) },
            )
        }

        SehrLeise(
            "PNG, JPEG, WebP oder GIF, höchstens ${iconKilobyte(stand.grenzen?.maxBytesJeIcon ?: 0L)} KB. " +
                "Größere Bilder werden vor dem Hochladen auf ${Iconbild.FELD} Punkte verkleinert; " +
                "das Seitenverhältnis bleibt, wie es ist.",
        )
    }

    if (entfernenFragen) {
        Blende(
            titel = "Icon entfernen",
            beiSchliessen = { entfernenFragen = false },
            breite = Dialogbreite.Schmal,
            fuss = {
                Knopf("Abbrechen", { entfernenFragen = false }, art = Knopfart.Leise)
                Knopf(
                    "Entfernen",
                    {
                        beiEntfernen()
                        entfernenFragen = false
                    },
                    art = Knopfart.Gefahr,
                )
            },
        ) {
            Text(
                text = "Das Icon für ${typ.typ} entfernen?",
                style = Schrift.Normal,
                color = Farben.Text,
            )
        }
    }
}

// ------------------------------------------------------------- Die Lichtbühne

/** Eine laufende Zeigergeste: ein neues Licht, oder das Verschieben eines vorhandenen. */
private data class Zeigergeste(
    val index: Int?,
    val startX: Float,
    val startY: Float,
    val x: Float,
    val y: Float,
    val bewegt: Boolean,
)

/**
 * Das eigene Bild mit seinen Blaulichtern — und die Gesten darauf.
 *
 * <b>Tippen</b> auf eine freie Stelle setzt einen Standardpunkt, <b>Ziehen</b>
 * spannt einen Balken auf. Tippen auf ein Licht wählt es, Ziehen verschiebt es.
 * Gespeichert wird erst beim Loslassen: Beim Ziehen entstünden sonst Dutzende
 * Aufrufe für dieselbe Handbewegung.
 *
 * <b>Die Vergrößerung wird nie mit dem Icon gespeichert</b> — sie ist ein
 * Arbeitsmittel für genaues Setzen, bis 400 %, mit verschiebbarem Ausschnitt.
 */
@Composable
private fun Lichtbuehne(
    stand: Iconstand,
    icon: Packicon,
    punkte: List<Blaulicht>,
    lichtwahl: Int?,
    maxPunkte: Int,
    beiBlaulicht: (List<Blaulicht>, Int?, String?) -> Unit,
    beiLichtwahl: (Int?) -> Unit,
    beiFehler: (String?) -> Unit,
) {
    val dichte = LocalDensity.current
    val bild by bildVon(iconAdresse(stand.server, icon.bild))

    var zoom by remember(icon.vorlageId) { mutableFloatStateOf(1f) }
    var panX by remember(icon.vorlageId) { mutableFloatStateOf(0f) }
    var panY by remember(icon.vorlageId) { mutableFloatStateOf(0f) }
    var verschieben by remember(icon.vorlageId) { mutableStateOf(false) }
    var geste by remember(icon.vorlageId) { mutableStateOf<Zeigergeste?>(null) }

    // Das Seitenverhältnis: aus der Antwort, sonst aus dem geladenen Bild.
    val breiteRoh = (icon.breite ?: bild?.width ?: 1).coerceAtLeast(1).toFloat()
    val hoeheRoh = (icon.hoehe ?: bild?.height ?: 1).coerceAtLeast(1).toFloat()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(Rundung.Klein)
            .background(Farben.BgTief)
            .border(1.dp, Farben.Rand, Rundung.Klein),
    ) {
        val seite = with(dichte) { maxWidth.toPx() }
        val basis = seite * 0.86f * zoom
        val bildB = if (breiteRoh >= hoeheRoh) basis else basis * breiteRoh / hoeheRoh
        val bildH = if (breiteRoh >= hoeheRoh) basis * hoeheRoh / breiteRoh else basis
        val grenzeX = max(0f, (bildB - seite) / 2f + seite * 0.07f)
        val grenzeY = max(0f, (bildH - seite) / 2f + seite * 0.07f)
        val links = (seite - bildB) / 2f + panX.coerceIn(-grenzeX, grenzeX)
        val oben = (seite - bildH) / 2f + panY.coerceIn(-grenzeY, grenzeY)

        // Was die Geste liest, liest sie frisch — sie läuft über viele Zeichnungen.
        val aktuell by rememberUpdatedState(
            Buehnenlage(links, oben, bildB, bildH, grenzeX, grenzeY, punkte, verschieben, stand.laeuft),
        )
        val setzen by rememberUpdatedState(beiBlaulicht)
        val waehlen by rememberUpdatedState(beiLichtwahl)
        val melden by rememberUpdatedState(beiFehler)
        val hoechstens by rememberUpdatedState(maxPunkte)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(icon.vorlageId) {
                    val schwelle = 5.dp.toPx()
                    val trefferPunkt = 12.dp.toPx()
                    val mindest = 6.dp.toPx()

                    awaitEachGesture {
                        val runter = awaitFirstDown(requireUnconsumed = false)
                        val lage = aktuell
                        if (lage.laeuft) return@awaitEachGesture

                        if (lage.verschieben) {
                            // Der Ausschnitt folgt dem Finger.
                            runter.consume()
                            var letzte = runter.position
                            while (true) {
                                val ereignis = awaitPointerEvent()
                                val zug = ereignis.changes.firstOrNull { it.id == runter.id } ?: break
                                if (!zug.pressed) break
                                val d = zug.position - letzte
                                letzte = zug.position
                                panX = (panX + d.x).coerceIn(-lage.grenzeX, lage.grenzeX)
                                panY = (panY + d.y).coerceIn(-lage.grenzeY, lage.grenzeY)
                                zug.consume()
                            }
                            return@awaitEachGesture
                        }

                        val start = lage.bruch(runter.position) ?: return@awaitEachGesture
                        runter.consume()

                        val getroffen = lage.treffer(start.x, start.y, trefferPunkt)
                        if (getroffen < 0 && lage.punkte.size >= hoechstens) {
                            melden(
                                "Höchstens $hoechstens Blaulichter je Icon — wähl eines aus und " +
                                    "entferne es zuerst.",
                            )
                            return@awaitEachGesture
                        }

                        waehlen(if (getroffen >= 0) getroffen else null)
                        var laufend = Zeigergeste(
                            index = if (getroffen >= 0) getroffen else null,
                            startX = start.x,
                            startY = start.y,
                            x = start.x,
                            y = start.y,
                            bewegt = false,
                        )
                        geste = laufend

                        var abgebrochen = false
                        while (true) {
                            val ereignis = awaitPointerEvent()
                            val zug = ereignis.changes.firstOrNull { it.id == runter.id }
                            if (zug == null) {
                                abgebrochen = true
                                break
                            }
                            val ort = lage.bruchGeklemmt(zug.position)
                            val weit = hypot(
                                (ort.x - laufend.startX) * lage.bildB,
                                (ort.y - laufend.startY) * lage.bildH,
                            ) > schwelle
                            laufend = laufend.copy(x = ort.x, y = ort.y, bewegt = laufend.bewegt || weit)
                            geste = laufend
                            zug.consume()
                            if (!zug.pressed) break
                        }
                        geste = null
                        if (abgebrochen) return@awaitEachGesture

                        val alle = lage.punkte
                        val index = laufend.index

                        // Ein vorhandenes Licht: Antippen wählt es, Ziehen verschiebt es.
                        if (index != null) {
                            if (!laufend.bewegt) return@awaitEachGesture
                            val licht = alle.getOrNull(index) ?: return@awaitEachGesture
                            val ort = innerhalbMitGroesse(licht, laufend.x.toDouble(), laufend.y.toDouble())
                            setzen(
                                alle.mapIndexed { i, p -> if (i == index) ort else p },
                                index,
                                "Licht ${index + 1} verschoben.",
                            )
                            return@awaitEachGesture
                        }

                        // Kurzer Tipp: ein fertiger Standardpunkt. Ziehen: die aufgespannte Fläche.
                        if (!laufend.bewegt) {
                            setzen(
                                alle + Blaulicht(x = runden(laufend.x.toDouble()), y = runden(laufend.y.toDouble())),
                                alle.size,
                                null,
                            )
                            return@awaitEachGesture
                        }

                        val l = min(laufend.startX, laufend.x)
                        val r = max(laufend.startX, laufend.x)
                        val o = min(laufend.startY, laufend.y)
                        val u = max(laufend.startY, laufend.y)
                        val b = runden(max((r - l).toDouble(), (mindest / lage.bildB).toDouble()))
                        val h = runden(max((u - o).toDouble(), (mindest / lage.bildH).toDouble()))
                        val neu = innerhalbMitGroesse(
                            Blaulicht(x = 0.5, y = 0.5, b = b, h = h, form = "eckig"),
                            ((l + r) / 2f).toDouble(),
                            ((o + u) / 2f).toDouble(),
                        )
                        setzen(alle + neu, alle.size, null)
                    }
                },
        ) {
            bild?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { IntOffset(links.roundToInt(), oben.roundToInt()) }
                        .size(with(dichte) { bildB.toDp() }, with(dichte) { bildH.toDp() }),
                )
            }

            punkte.forEachIndexed { i, p ->
                Lichtpunkt(
                    licht = p,
                    nummer = i + 1,
                    gewaehlt = lichtwahl == i,
                    links = links,
                    oben = oben,
                    bildB = bildB,
                    bildH = bildH,
                )
            }

            // Die Vorschau der aufgezogenen Fläche, bevor sie beim Loslassen gespeichert wird.
            geste?.takeIf { it.index == null && it.bewegt }?.let { g ->
                Lichtpunkt(
                    licht = Blaulicht(
                        x = ((g.startX + g.x) / 2f).toDouble(),
                        y = ((g.startY + g.y) / 2f).toDouble(),
                        b = abs(g.x - g.startX).toDouble(),
                        h = abs(g.y - g.startY).toDouble(),
                        form = "eckig",
                    ),
                    nummer = null,
                    gewaehlt = false,
                    links = links,
                    oben = oben,
                    bildB = bildB,
                    bildH = bildH,
                    entwurf = true,
                )
            }
        }
    }

    // Die Vergrößerung.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        fun zoomSetzen(wert: Float) {
            val neu = wert.coerceIn(1f, 4f)
            // Denselben Ausschnitt unter der Mitte halten.
            val faktor = neu / zoom
            panX *= faktor
            panY *= faktor
            zoom = neu
            if (neu == 1f) {
                verschieben = false
                panX = 0f
                panY = 0f
            }
        }

        Knopf("−", { zoomSetzen(zoom - 0.25f) }, aktiv = zoom > 1f, kompakt = true)
        Regler(
            wert = (zoom * 100).roundToInt(),
            beiAenderung = { zoomSetzen(it / 100f) },
            von = 100,
            bis = 400,
            schritt = 25,
            etikett = "Zoom ${(zoom * 100).roundToInt()} %",
            modifier = Modifier.weight(1f),
        )
        Knopf("+", { zoomSetzen(zoom + 0.25f) }, aktiv = zoom < 4f, kompakt = true)
    }
    if (zoom > 1f) {
        Pillenreihe {
            Pille(
                aufschrift = if (verschieben) "Lichter setzen" else "Ausschnitt verschieben",
                an = verschieben,
                beiDruck = { verschieben = !verschieben },
            )
            Pille(
                aufschrift = "Anpassen",
                an = false,
                beiDruck = {
                    zoom = 1f
                    panX = 0f
                    panY = 0f
                    verschieben = false
                },
            )
        }
    }
    SehrLeise(
        if (verschieben) {
            "Ziehen verschiebt den Bildausschnitt."
        } else {
            "Tippen setzt ein Licht, Ziehen spannt eine Fläche auf."
        },
    )
}

/** Was die Geste über die Bühne wissen muss — in Bildpunkten der Bühne. */
private data class Buehnenlage(
    val links: Float,
    val oben: Float,
    val bildB: Float,
    val bildH: Float,
    val grenzeX: Float,
    val grenzeY: Float,
    val punkte: List<Blaulicht>,
    val verschieben: Boolean,
    val laeuft: Boolean,
) {
    /** Die Stelle auf dem Bild, 0–1 — oder `null`, wenn der Finger daneben liegt. */
    fun bruch(ort: Offset): Offset? {
        if (bildB <= 0f || bildH <= 0f) return null
        val x = (ort.x - links) / bildB
        val y = (ort.y - oben) / bildH
        return if (x in 0f..1f && y in 0f..1f) Offset(x, y) else null
    }

    /** Dasselbe, aber an den Rand geklemmt — beim Ziehen über die Kante hinaus. */
    fun bruchGeklemmt(ort: Offset): Offset = Offset(
        ((ort.x - links) / bildB).coerceIn(0f, 1f),
        ((ort.y - oben) / bildH).coerceIn(0f, 1f),
    )

    /** Trifft auch einen aufgezogenen Balken an seiner Fläche, nicht nur an der Mitte. */
    fun treffer(x: Float, y: Float, punkt: Float): Int {
        for (i in punkte.indices.reversed()) {
            val p = punkte[i]
            val halbB = p.b?.let { it / 2 } ?: (punkt / bildB).toDouble()
            val halbH = p.h?.let { it / 2 } ?: (punkt / bildH).toDouble()
            if (abs(p.x - x) <= halbB && abs(p.y - y) <= halbH) return i
        }
        return -1
    }
}

/** Ein Licht auf der Bühne — rund oder eckig, blau oder gelb, mit seiner Nummer. */
@Composable
private fun Lichtpunkt(
    licht: Blaulicht,
    nummer: Int?,
    gewaehlt: Boolean,
    links: Float,
    oben: Float,
    bildB: Float,
    bildH: Float,
    entwurf: Boolean = false,
) {
    val dichte = LocalDensity.current
    val standard = with(dichte) { 14.dp.toPx() }
    val mindest = with(dichte) { 6.dp.toPx() }
    val eckig = licht.form == "eckig"

    val b = licht.b?.let { (it * bildB).toFloat() }?.let { if (eckig) max(it, mindest) else it } ?: standard
    val h = licht.h?.let { (it * bildH).toFloat() }?.let { if (eckig) max(it, mindest) else it } ?: standard
    val mitteX = links + (licht.x * bildB).toFloat()
    val mitteY = oben + (licht.y * bildH).toFloat()

    val farbe = if (licht.farbe == "gelb") Farben.Amber else Farben.Eigenposition
    val form: Shape = if (eckig) Rundung.Winzig else CircleShape

    Box(
        modifier = Modifier
            .offset { IntOffset((mitteX - b / 2f).roundToInt(), (mitteY - h / 2f).roundToInt()) }
            .size(with(dichte) { b.toDp() }, with(dichte) { h.toDp() })
            .then(
                if (entwurf) {
                    Modifier
                        .background(Farben.Eigenposition.copy(alpha = 0.24f), form)
                        .border(1.dp, Farben.Amber, form)
                } else {
                    Modifier
                        .background(
                            Brush.radialGradient(
                                listOf(farbe, farbe.copy(alpha = 0.45f), Color.Transparent),
                            ),
                            form,
                        )
                        .border(
                            if (gewaehlt) 2.dp else 1.dp,
                            if (gewaehlt) Farben.Amber else Color.White.copy(alpha = 0.85f),
                            form,
                        )
                },
            ),
    )

    if (nummer != null) {
        Text(
            text = nummer.toString(),
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
            color = Farben.Text,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (mitteX - with(dichte) { 8.dp.toPx() }).roundToInt(),
                        (mitteY - h / 2f - with(dichte) { 18.dp.toPx() }).roundToInt(),
                    )
                }
                .background(Farben.BgTief, Rundung.Rund)
                .padding(horizontal = Abstand.Winzig),
        )
    }
}

// ------------------------------------------------------------ Die Lichterliste

@Composable
private fun Lichtverwaltung(
    stand: Iconstand,
    punkte: List<Blaulicht>,
    maxPunkte: Int,
    lichtwahl: Int?,
    aktivesLicht: Blaulicht?,
    vorgabeArt: String,
    beiLichtwahl: (Int?) -> Unit,
    beiBlaulicht: (List<Blaulicht>, Int?, String?) -> Unit,
    beiLichtAendern: ((Blaulicht) -> Blaulicht) -> Unit,
) {
    val grenzen = stand.grenzen
    val laeuft = stand.laeuft

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().padding(top = Abstand.Klein),
    ) {
        Text("Blaulicht", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        SehrLeise(
            "Für genaues Setzen kannst du das Bild oben bis auf 400 % vergrößern und den " +
                "Ausschnitt verschieben.\n" +
                if (punkte.isNotEmpty()) {
                    "${punkte.size} von höchstens $maxPunkte Lichtern. Tipp auf eine Nummer, um " +
                        "das Licht einzustellen; zieh es, um es zu verschieben. Auf einer freien " +
                        "Stelle setzt ein Tipp einen Punkt und Ziehen spannt einen Balken oder " +
                        "Blitzer auf."
                } else {
                    "Bei Sondersignal blitzt das ganze Bild. Tipp auf das Bild, um einen " +
                        "Lichtpunkt zu setzen, oder zieh genau über die Fläche, die leuchten soll."
                },
        )

        punkte.forEachIndexed { i, p ->
            val an = lichtwahl == i
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(
                        farbe = if (an) Farben.HauchAmber else Farben.FlaecheHoch,
                        randfarbe = if (an) Farben.Amber else Farben.Rand,
                        ecke = 9.dp,
                        mitLichtkante = false,
                    )
                    .clickable(onClick = { beiLichtwahl(i) }, role = Role.Button)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text(
                    text = "${i + 1}",
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = if (an) Farben.Amber else Farben.TextLeise,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Licht ${i + 1}", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    SehrLeise(
                        "${formName(p.form)} · ${musterName(p.art ?: vorgabeArt)} · " +
                            "Takt ${(p.takt ?: "a").uppercase()} · ${farbeName(p.farbe)}",
                    )
                }
            }
        }

        if (aktivesLicht != null && lichtwahl != null) {
            Lichtregler(
                licht = aktivesLicht,
                nummer = lichtwahl + 1,
                formen = grenzen?.blaulichtFormen.orEmpty().ifEmpty { listOf("rund", "eckig") },
                arten = grenzen?.blaulichtArten.orEmpty().ifEmpty { listOf("doppel", "vierfach", "rundum") },
                takte = grenzen?.blaulichtTakte.orEmpty().ifEmpty { listOf("a", "b") },
                farben = grenzen?.blaulichtFarben.orEmpty().ifEmpty { listOf("blau", "gelb") },
                vorgabeArt = vorgabeArt,
                laeuft = laeuft,
                beiAendern = beiLichtAendern,
                beiEntfernen = {
                    val neu = punkte.filterIndexed { i, _ -> i != lichtwahl }
                    val naechste = if (neu.isEmpty()) null else min(lichtwahl, neu.size - 1)
                    beiBlaulicht(neu, naechste, null)
                },
            )
        }

        Pillenreihe {
            if (punkte.isNotEmpty()) {
                Knopf("Alle Lichter entfernen", { beiBlaulicht(emptyList(), null, null) }, aktiv = !laeuft, kompakt = true)
            } else {
                Knopf(
                    "Licht in die Mitte setzen",
                    { beiBlaulicht(listOf(Blaulicht(x = 0.5, y = 0.5)), 0, null) },
                    aktiv = !laeuft,
                    kompakt = true,
                )
            }
        }
    }
}

/** Die Regler des gewählten Lichts: Form, Muster, Takt, Farbe, Größe. */
@Composable
private fun Lichtregler(
    licht: Blaulicht,
    nummer: Int,
    formen: List<String>,
    arten: List<String>,
    takte: List<String>,
    farben: List<String>,
    vorgabeArt: String,
    laeuft: Boolean,
    beiAendern: ((Blaulicht) -> Blaulicht) -> Unit,
    beiEntfernen: () -> Unit,
) {
    // Die Größe lebt beim Ziehen hier und geht erst beim Loslassen hinaus — wie
    // das `change` des Webs. Sonst wäre jede Stufe ein eigener Aufruf.
    var breite by remember(nummer, licht.b) { mutableIntStateOf(((licht.b ?: 0.12) * 100).roundToInt()) }
    var hoehe by remember(nummer, licht.h) { mutableIntStateOf(((licht.h ?: 0.12) * 100).roundToInt()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(farbe = Farben.Flaeche, randfarbe = Farben.AmberTief, ecke = 9.dp, mitLichtkante = false)
            .padding(Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Licht $nummer einstellen",
                    style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                )
                SehrLeise(
                    "Takt B läuft eine halbe Blitzfolge hinter Takt A — damit wechseln linke " +
                        "und rechte Leuchten wirklich ab.",
                )
            }
            Knopf("Licht entfernen", beiEntfernen, art = Knopfart.Gefahr, aktiv = !laeuft, kompakt = true)
        }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Etikett("Form")
            Segment(
                seiten = formen,
                gewaehlt = licht.form ?: "rund",
                beiWahl = { f -> if (!laeuft) beiAendern { it.copy(form = f) } },
                aufschrift = { formName(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Etikett("Blitzmuster")
            Pillenreihe {
                Pille(
                    aufschrift = "Wie das Fahrzeug (${musterName(vorgabeArt)})",
                    an = licht.art == null,
                    aktiv = !laeuft,
                    beiDruck = { beiAendern { it.copy(art = null) } },
                )
                arten.forEach { art ->
                    Pille(
                        aufschrift = musterName(art),
                        an = licht.art == art,
                        aktiv = !laeuft,
                        beiDruck = { beiAendern { it.copy(art = art) } },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Etikett("Blitzfolge")
            Segment(
                seiten = takte,
                gewaehlt = licht.takt ?: "a",
                beiWahl = { t -> if (!laeuft) beiAendern { it.copy(takt = t) } },
                aufschrift = { "Takt ${it.uppercase()}" },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Etikett("Farbe")
            Segment(
                seiten = farben,
                gewaehlt = licht.farbe ?: "blau",
                beiWahl = { f -> if (!laeuft) beiAendern { it.copy(farbe = f) } },
                aufschrift = { farbeName(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Regler(
            wert = breite,
            beiAenderung = { breite = it },
            von = 2,
            bis = 100,
            etikett = "Breite " + if (licht.b == null) "Standard" else "$breite %",
            aktiv = !laeuft,
            beiLoslassen = { beiAendern { it.copy(b = runden(breite / 100.0)) } },
        )
        Regler(
            wert = hoehe,
            beiAenderung = { hoehe = it },
            von = 2,
            bis = 100,
            etikett = "Höhe " + if (licht.h == null) "Standard" else "$hoehe %",
            aktiv = !laeuft,
            beiLoslassen = { beiAendern { it.copy(h = runden(hoehe / 100.0)) } },
        )

        if (licht.b != null || licht.h != null) {
            Knopf(
                "Standardgröße",
                { beiAendern { it.copy(b = null, h = null) } },
                aktiv = !laeuft,
                kompakt = true,
            )
        }
    }
}

// --------------------------------------------------------------- Kleinteile

private fun runden(wert: Double): Double = Math.round(wert * 1000) / 1000.0

/**
 * Hält ein Licht samt seiner Fläche auf dem Bild — nach dem Runden noch einmal
 * geklemmt, sonst schöbe die Rundung allein einen Tausendstel über den Rand.
 */
private fun innerhalbMitGroesse(p: Blaulicht, x: Double, y: Double): Blaulicht {
    val halbB = (p.b ?: 0.0) / 2
    val halbH = (p.h ?: 0.0) / 2
    return p.copy(
        x = runden(x).coerceIn(halbB, 1 - halbB),
        y = runden(y).coerceIn(halbH, 1 - halbH),
    )
}

private fun musterName(art: String): String = when (art) {
    "vierfach" -> "Vierfachblitz"
    "rundum" -> "Rundumkennleuchte"
    else -> "Doppelblitz"
}

private fun formName(form: String?): String = if (form == "eckig") "Eckig / Balken" else "Rund / Punkt"

private fun farbeName(farbe: String?): String = if (farbe == "gelb") "Gelb" else "Blau"

/**
 * Das Blitzmuster, das ein Licht ohne eigene Angabe trägt — das der Anlage aus
 * dem Bauplan des Fahrzeugs (`fahrzeugBauplan.ts`).
 *
 * <b>Nur die Ausnahmen stehen hier.</b> Die allermeisten Baupläne blitzen doppelt;
 * die Liste nennt, wer es anders tut, gezogen aus den Bauplänen des Webs. Ein
 * unbekannter Typ fährt wie das Standardfahrzeug seiner Organisation — beim THW
 * die Rundumkennleuchte, bei der Polizei der Vierfachblitz.
 */
private fun blitzVorgabe(typ: String, organisation: String): String =
    BLITZ_AUSNAHMEN[typ.trim().lowercase()]
        ?: when (organisation) {
            "Thw" -> "rundum"
            "Polizei" -> "vierfach"
            else -> "doppel"
        }

private val BLITZ_AUSNAHMEN: Map<String, String> = mapOf(
    "forsttraktor mit löschanhänger" to "rundum",
    "elw 1" to "vierfach",
    "elw 1 a-dienst" to "vierfach",
    "elw 2" to "vierfach",
    "wlf + ab tel" to "vierfach",
    "kdow" to "vierfach",
    "kdow c-dienst" to "vierfach",
    "kdow b-dienst" to "vierfach",
    "mzf + anhänger mzb" to "rundum",
    "nef" to "vierfach",
    "orgl" to "vierfach",
    "elrd" to "vierfach",
    "lna" to "vierfach",
    "elw 1 (rd)" to "vierfach",
    "gkw" to "rundum",
    "mzkw" to "rundum",
    "fgr sb" to "rundum",
    "mtw" to "rundum",
    "fgr fk" to "rundum",
    "fgr brb" to "rundum",
    "fgr i" to "rundum",
    "fgr n" to "rundum",
    "fgr ortung" to "rundum",
    "fgr r" to "rundum",
    "fgr tw" to "rundum",
    "fgr w" to "rundum",
    "fgr ö" to "rundum",
    "fustw" to "vierfach",
    "unfallaufnahme" to "vierfach",
    "diensthundeführer" to "vierfach",
    "kriminaltechnik" to "vierfach",
    "krad" to "vierfach",
    "grukw" to "vierfach",
    "wawe 10" to "vierfach",
    "polizeihubschrauber" to "vierfach",
)
