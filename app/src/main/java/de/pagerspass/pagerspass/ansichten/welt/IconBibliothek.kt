package de.pagerspass.pagerspass.ansichten.welt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.pagerspass.pagerspass.mobil.Iconpack
import de.pagerspass.pagerspass.netz.Blaulichtpunkt
import de.pagerspass.pagerspass.netz.IconGrenzen
import de.pagerspass.pagerspass.netz.Iconpack as Pack
import de.pagerspass.pagerspass.netz.Icontyp
import de.pagerspass.pagerspass.netz.Packicon
import de.pagerspass.pagerspass.netz.Packimport
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.bildVon
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Wie groß ein Icon höchstens wird, bevor es hochgeht — `FELD` in `utils/iconbild.ts`. */
private const val FELD = 512

/**
 * Die Wahl des Icon-Packs in den Welt-Einstellungen — `EinstellungBlende.vue`.
 *
 * <b>„Standard" steht immer da und ist keine Zeile mit Kennung.</b> Er ist, was
 * das Spiel ohnehin zeichnet — nicht bearbeitbar, nicht löschbar, und der
 * Zustand, in den man jederzeit zurückkann. Bearbeitet wird in der Bibliothek,
 * die sich über die Welt legt.
 */
@Composable
fun IconpackWahl() {
    val zusammenhang = LocalContext.current
    val wege = remember { Iconpack.wege(zusammenhang) }
    val bereich = rememberCoroutineScope()
    var packs by remember { mutableStateOf<List<Pack>>(emptyList()) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var bibliothek by remember { mutableStateOf(false) }

    suspend fun laden() {
        packs = runCatching { wege.packs() }.getOrElse { emptyList() }
    }
    LaunchedEffect(bibliothek) { if (!bibliothek) laden() }

    fun waehlen(id: String?) {
        sendet = true
        fehler = null
        bereich.launch {
            runCatching { wege.waehlen(id) }
                .onSuccess {
                    laden()
                    Iconpack.laden(zusammenhang)
                    if (id == null) Iconpack.vergessen()
                }
                .onFailure { fehler = it.message ?: "Das Icon-Pack ließ sich nicht setzen." }
            sendet = false
        }
    }

    Ueberschrift("Fahrzeug-Icons")
    Leisesatz("Nur du siehst sie — auf deiner Karte gelten sie für alle Fahrzeuge.", winzig = true)
    val aktiv = packs.firstOrNull { it.aktiv }?.id
    Pillenreihe {
        Pille("Standard", an = aktiv == null, beiDruck = { waehlen(null) }, aktiv = !sendet)
        packs.forEach { p ->
            Pille(
                p.name,
                an = aktiv == p.id,
                beiDruck = { waehlen(p.id) },
                aktiv = !sendet && !p.gesperrt && p.belegt > 0,
            )
        }
    }
    Leisesatz(
        if (packs.isEmpty()) {
            "Du hast noch kein Pack. Du musst nicht alle Fahrzeuge bebildern — ein einziges reicht."
        } else {
            "Ein Pack ersetzt nur, was es hinterlegt — der Rest bleibt gezeichnet."
        },
        winzig = true,
    )
    Warnsatz(fehler)
    Knopf("Icon-Packs verwalten", { bibliothek = true }, kompakt = true, art = Knopfart.Leise)

    if (bibliothek) {
        Dialog(
            onDismissRequest = { bibliothek = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        ) {
            Box(Modifier.fillMaxSize().background(Farben.Bg)) {
                IconBibliothek(beiSchliessen = {
                    bibliothek = false
                    bereich.launch { Iconpack.laden(zusammenhang) }
                })
            }
        }
    }
}

/**
 * Die Bibliothek — `IconBibliothekView.vue` und, für ein gewähltes Pack,
 * `IconEditorView.vue`.
 *
 * <b>Hochgeladen wird, nicht gemalt.</b> Das Web hatte einmal eine Zeichenfläche
 * und hat sie wieder abgeschafft: Wer ein Fahrzeug malen will, hat dafür bessere
 * Werkzeuge. Was bleibt, ist der Weg, der zählt: Bild aussuchen, ansehen,
 * übernehmen — und die Blaulichter dorthin setzen, wo sie auf dem Bild sitzen.
 */
@Composable
fun IconBibliothek(beiSchliessen: () -> Unit) {
    val zusammenhang = LocalContext.current
    val wege = remember { Iconpack.wege(zusammenhang) }
    val bereich = rememberCoroutineScope()

    var packs by remember { mutableStateOf<List<Pack>?>(null) }
    var typen by remember { mutableStateOf<List<Icontyp>>(emptyList()) }
    var grenzen by remember { mutableStateOf(IconGrenzen()) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var hinweis by remember { mutableStateOf<String?>(null) }
    var sendet by remember { mutableStateOf(false) }
    var offen by remember { mutableStateOf<Pack?>(null) }
    var neuerName by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeVon by remember { mutableStateOf<Pair<String, String>?>(null) }
    var umbenennen by remember { mutableStateOf<String?>(null) }
    var umName by remember { mutableStateOf("") }
    var loeschfrage by remember { mutableStateOf<String?>(null) }
    var importiert by remember { mutableStateOf<Packimport?>(null) }

    suspend fun laden() {
        runCatching {
            grenzen = wege.grenzen()
            typen = wege.typen()
            packs = wege.packs()
            fehler = null
        }.onFailure {
            fehler = it.message ?: "Die Icon-Packs ließen sich nicht laden."
            if (packs == null) packs = emptyList()
        }
    }
    LaunchedEffect(Unit) { laden() }

    fun handlung(ersatz: String, tun: suspend () -> Unit) {
        sendet = true
        fehler = null
        hinweis = null
        bereich.launch {
            runCatching { tun() }.onFailure { fehler = it.message ?: ersatz }
            laden()
            sendet = false
        }
    }

    val zip = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        handlung("Der Import ging nicht.") {
            val (name, bytes) = withContext(Dispatchers.IO) { dateiLesen(zusammenhang, uri) }
            if (bytes == null) error("Die Datei ließ sich nicht lesen.")
            importiert = wege.importieren(name, bytes)
        }
    }

    val pack = offen
    if (pack != null) {
        BackHandler { offen = null; bereich.launch { laden() } }
        IconEditor(pack, typen, grenzen, beiZurueck = { offen = null; bereich.launch { laden() } })
        return
    }
    BackHandler(onBack = beiSchliessen)

    Seite {
        Seitenkopf(
            titel = "Fahrzeug-Icons",
            unterzeile = "PagerSpass - World",
            knoepfe = { Knopf("Zurück zur Welt", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
        )
        Leisesatz(
            "Deine Fahrzeuge auf der Weltkarte zeichnet das Spiel selbst. Hier legst du eigene " +
                "Grafiken darüber — für jeden Typ eine, oder für ein paar. Was du nicht ersetzt, bleibt gezeichnet.",
        )
        Warnsatz(fehler)
        hinweis?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }

        val liste = packs
        if (liste == null) {
            Ladezeile()
            return@Seite
        }

        liste.forEach { p ->
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(p.name, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                    if (p.aktiv) Text("gilt auf deiner Karte", style = Schrift.Winzig, color = Farben.Amber)
                    if (p.gesperrt) Text("gesperrt", style = Schrift.Winzig, color = Farben.SignalHell)
                }
                Text(
                    "${p.belegt} von ${typen.size} Fahrzeugtypen · ${kb(p.bytes)} von ${kb(grenzen.maxBytesJePack)} KB",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
                if (umbenennen == p.id) {
                    Feld(wert = umName, beiAenderung = { umName = it.take(grenzen.maxNameLaenge) }, etikett = "Neuer Name")
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf("Übernehmen", {
                            val name = umName.trim()
                            umbenennen = null
                            handlung("Das Umbenennen ging nicht.") { wege.umbenennen(p.id, name) }
                        }, kompakt = true, aktiv = umName.isNotBlank() && !sendet)
                        Knopf("Abbrechen", { umbenennen = null }, kompakt = true, art = Knopfart.Leise)
                    }
                }
                if (loeschfrage == p.id) {
                    Text("„${p.name}“ mit allen Icons löschen? Das lässt sich nicht rückgängig machen.", style = Schrift.Klein, color = Farben.SignalHell)
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf("Endgültig löschen", {
                            loeschfrage = null
                            handlung("Das Löschen ging nicht.") { wege.loeschen(p.id) }
                        }, kompakt = true, art = Knopfart.Gefahr, aktiv = !sendet)
                        Knopf("Behalten", { loeschfrage = null }, kompakt = true, art = Knopfart.Leise)
                    }
                }
                Pillenreihe {
                    Pille("Bearbeiten", an = false, beiDruck = { offen = p }, aktiv = !sendet)
                    Pille("Umbenennen", an = false, beiDruck = { umbenennen = p.id; umName = p.name }, aktiv = !sendet)
                    Pille("Code holen", an = false, aktiv = !sendet, beiDruck = {
                        handlung("Der Code ließ sich nicht holen.") { codeVon = p.id to wege.code(p.id) }
                    })
                    Pille("Löschen", an = false, farbe = Farben.Signal, beiDruck = { loeschfrage = p.id }, aktiv = !sendet)
                }
                // Der Code erscheint erst, wenn jemand ihn geholt hat — auch auf dem
                // Server entsteht er erst dann.
                codeVon?.takeIf { it.first == p.id }?.let { (_, c) ->
                    SelectionContainer {
                        Text("Zum Weitergeben: $c", style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
                    }
                    SehrLeise("Wer ihn eingibt, bekommt eine eigene Kopie.")
                }
            }
        }
        if (liste.isEmpty()) {
            SehrLeise("Du hast noch kein Icon-Pack. Leg eins an — du musst nicht alle Fahrzeuge bebildern, ein einziges reicht.")
        }

        val voll = liste.size >= grenzen.maxPacks
        Abschnitt("Neues Pack") {
            Feld(
                wert = neuerName,
                beiAenderung = { neuerName = it.take(grenzen.maxNameLaenge) },
                platzhalter = "Name, zum Beispiel „Feuerrot“",
                aktiv = !voll,
            )
            Knopf("Anlegen", {
                val name = neuerName.trim()
                handlung("Das Anlegen ging nicht.") {
                    val neu = wege.anlegen(name)
                    neuerName = ""
                    offen = neu
                }
            }, kompakt = true, aktiv = !voll && !sendet && neuerName.isNotBlank())
            if (voll) SehrLeise("Mehr als ${grenzen.maxPacks} Packs gehen nicht. Benenne eins um oder lösche es.")
        }

        Abschnitt("Pack per Code übernehmen") {
            SehrLeise("Was dabei entsteht, ist eine eigene Kopie. Wer dir den Code gegeben hat, kann seins danach löschen.")
            Feld(wert = code, beiAenderung = { code = it.uppercase().take(6) }, platzhalter = "ABC234", aktiv = !voll)
            Knopf("Übernehmen", {
                val c = code
                handlung("Der Code passt nicht.") {
                    val neu = wege.uebernehmen(c)
                    code = ""
                    hinweis = "„${neu.name}“ ist übernommen."
                }
            }, kompakt = true, aktiv = !voll && !sendet && code.isNotBlank())
        }

        Abschnitt("Pack aus einer Zip importieren") {
            SehrLeise(
                "Die Datei heißt Pack_NAME.zip — NAME wird der Packname. Darin liegen eine details.json, " +
                    "die je Fahrzeugtyp die Grafik nennt, und ein Ordner assets mit den Grafiken. " +
                    "Wie das genau aussieht, steht im Wiki (World → Eigene Fahrzeug-Icons).",
            )
            Knopf("Zip wählen", { zip.launch("application/zip") }, kompakt = true, aktiv = !voll && !sendet)
            importiert?.let { i ->
                Text(
                    "„${i.pack.name}“ ist da — ${i.uebernommen} ${if (i.uebernommen == 1) "Icon" else "Icons"} übernommen" +
                        if (i.uebersprungen.isNotEmpty()) ", ${i.uebersprungen.size} übersprungen." else ".",
                    style = Schrift.Klein,
                    color = Farben.AmberHell,
                )
                i.uebersprungen.forEach { u ->
                    Text("${u.vorlageId} — ${u.grund}", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                }
            }
        }
    }
}

/**
 * Die Icons eines Packs — links die Typen, darüber der gewählte als eigene
 * Seite. Am Handy ist das im Web genauso: eine Liste, aus der sich der gewählte
 * Typ als Seite darüberlegt.
 */
@Composable
private fun IconEditor(pack: Pack, typen: List<Icontyp>, grenzen: IconGrenzen, beiZurueck: () -> Unit) {
    val zusammenhang = LocalContext.current
    val wege = remember { Iconpack.wege(zusammenhang) }

    var icons by remember { mutableStateOf<Map<String, Packicon>>(emptyMap()) }
    var server by remember { mutableStateOf("") }
    var laedt by remember { mutableStateOf(true) }
    var suche by remember { mutableStateOf("") }
    var nurBelegte by remember { mutableStateOf(false) }
    var gewaehlt by remember { mutableStateOf<String?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pack.id) {
        server = Iconpack.serverKennen(zusammenhang)
        runCatching { wege.icons(pack.id) }
            .onSuccess { liste -> icons = liste.associateBy { it.vorlageId } }
            .onFailure { fehler = it.message ?: "Die Icons ließen sich nicht laden." }
        laedt = false
    }

    val typ = typen.firstOrNull { it.vorlageId == gewaehlt }
    if (typ != null) {
        BackHandler { gewaehlt = null }
        IconTypSeite(
            pack = pack,
            typ = typ,
            icon = icons[typ.vorlageId],
            server = server,
            grenzen = grenzen,
            beiGeaendert = { neu ->
                icons = if (neu == null) icons - typ.vorlageId else icons + (typ.vorlageId to neu)
            },
            beiZurueck = { gewaehlt = null },
        )
        return
    }

    Seite {
        Seitenkopf(
            titel = pack.name,
            unterzeile = "${icons.size} von ${typen.size} Fahrzeugtypen belegt",
            knoepfe = { Knopf("Zur Bibliothek", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )
        Warnsatz(fehler)
        Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Fahrzeug suchen")
        Schalterzeile("Nur belegte Typen", nurBelegte, { nurBelegte = it })
        if (laedt) Ladezeile()

        val gefiltert = typen.filter { t ->
            (!nurBelegte || icons.containsKey(t.vorlageId)) &&
                (suche.isBlank() || listOf(t.typ, t.beschreibung, t.kategorie, t.organisation, t.vorlageId)
                    .any { it.contains(suche.trim(), ignoreCase = true) })
        }
        // Nach Organisation und Kategorie — dieselben Gruppen wie in der Kaufliste der Welt.
        gefiltert.groupBy { "${it.organisation} · ${it.kategorie}" }.forEach { (titel, liste) ->
            Ueberschrift(titel)
            liste.forEach { t ->
                val icon = icons[t.vorlageId]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .flaeche(randfarbe = if (icon != null) Farben.AmberTief else Farben.Rand)
                        .clickable(role = Role.Button) { gewaehlt = t.vorlageId }
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        if (icon != null) {
                            val bild by bildVon(Iconpack.adresse(icon.bild).takeIf { server.isNotBlank() })
                            bild?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                        } else {
                            Text("—", style = Schrift.Klein, color = Farben.TextSehrLeise)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(t.typ, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                        Text(t.beschreibung, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 2)
                    }
                }
            }
        }
        if (!laedt && gefiltert.isEmpty()) SehrLeise("Kein Fahrzeug passt dazu.")
    }
}

/** Ein Fahrzeugtyp: die Grafik, die Drehung und die Blaulichter. */
@Composable
private fun IconTypSeite(
    pack: Pack,
    typ: Icontyp,
    icon: Packicon?,
    server: String,
    grenzen: IconGrenzen,
    beiGeaendert: (Packicon?) -> Unit,
    beiZurueck: () -> Unit,
) {
    val zusammenhang = LocalContext.current
    val wege = remember { Iconpack.wege(zusammenhang) }
    val bereich = rememberCoroutineScope()
    var laeuft by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var hinweis by remember { mutableStateOf<String?>(null) }
    var lichtwahl by remember { mutableStateOf<Int?>(null) }
    var loeschfrage by remember { mutableStateOf(false) }
    val aktuell by rememberUpdatedState(icon)

    fun tun(ersatz: String, schritt: suspend () -> Unit) {
        laeuft = true
        fehler = null
        bereich.launch {
            runCatching { schritt() }.onFailure { fehler = it.message ?: ersatz }
            laeuft = false
        }
    }

    val bildwahl = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        tun("Das Hochladen ging nicht.") {
            val eingepasst = withContext(Dispatchers.Default) { bildEinpassen(zusammenhang, uri, grenzen) }
            val neu = wege.iconSetzen(pack.id, typ.vorlageId, eingepasst.daten, null)
            beiGeaendert(neu)
            hinweis = if (eingepasst.verkleinert) {
                "Übernommen — auf ${eingepasst.breite} × ${eingepasst.hoehe} Punkte verkleinert."
            } else {
                "Übernommen."
            }
        }
    }

    fun lichterSetzen(neu: List<Blaulichtpunkt>, wahl: Int?, meldung: String? = null) {
        val vorher = aktuell ?: return
        val anzahl = vorher.blaulichter.size
        tun("Das ging nicht.") {
            wege.blaulicht(pack.id, typ.vorlageId, neu)
            beiGeaendert(vorher.copy(blaulichter = neu))
            lichtwahl = wahl?.takeIf { it < neu.size }
            hinweis = meldung ?: when {
                neu.isEmpty() -> "Lichter entfernt — es blitzt wieder das ganze Bild."
                neu.size < anzahl -> "Licht entfernt."
                neu.size == 1 -> "Das erste Blaulicht ist gesetzt und ausgewählt."
                else -> "Das Icon trägt jetzt ${neu.size} Blaulichter."
            }
        }
    }

    Seite {
        Seitenkopf(
            titel = typ.typ,
            unterzeile = "${typ.organisation} · ${typ.kategorie}",
            knoepfe = { Knopf("Zur Liste", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )
        SehrLeise(typ.beschreibung)
        Warnsatz(fehler)
        hinweis?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }

        if (icon == null) {
            Leisesatz("Für diesen Typ zeichnet das Spiel selbst. Leg eine Grafik darüber — PNG, JPG oder WebP; sie wird als WebP abgelegt.")
        } else {
            Blaulichtbuehne(
                icon = icon,
                server = server,
                gewaehlt = lichtwahl,
                beiTipp = { x, y, treffer ->
                    if (treffer != null) {
                        lichtwahl = treffer
                    } else if (icon.blaulichter.size < grenzen.maxBlaulichter) {
                        val neu = icon.blaulichter + Blaulichtpunkt(x = runden(x), y = runden(y))
                        lichterSetzen(neu, neu.size - 1)
                    } else {
                        hinweis = "Mehr als ${grenzen.maxBlaulichter} Blaulichter trägt ein Icon nicht."
                    }
                },
                beiSchieben = { i, x, y ->
                    val neu = icon.blaulichter.mapIndexed { n, p -> if (n == i) p.copy(x = runden(x), y = runden(y)) else p }
                    lichterSetzen(neu, i, "Licht ${i + 1} verschoben.")
                },
            )
            SehrLeise("Tippen setzt ein Blaulicht dorthin, wo es auf dem Bild sitzt; ein gesetztes antippen wählt es, ziehen verschiebt es.")
            Text(
                "${icon.breite ?: "?"} × ${icon.hoehe ?: "?"} Punkte · ${icon.blaulichter.size}/${grenzen.maxBlaulichter} Blaulichter",
                style = Schrift.MonoKlein,
                color = Farben.TextLeise,
            )
            Schalterzeile(
                titel = "Dreht mit dem Kurs",
                an = icon.dreht,
                beiWechsel = { neu ->
                    tun("Das ging nicht.") {
                        wege.drehung(pack.id, typ.vorlageId, neu)
                        beiGeaendert(icon.copy(dreht = neu))
                        hinweis = if (neu) "Dreht jetzt mit dem Kurs." else "Steht jetzt still, egal wohin das Fahrzeug fährt."
                    }
                },
                unterzeile = "Aus: Das Bild steht aufrecht, wie es gemalt ist. An: Es zeigt in Fahrtrichtung — dann muss die Front oben liegen.",
                aktiv = !laeuft,
            )

            val licht = lichtwahl?.let { icon.blaulichter.getOrNull(it) }
            if (licht != null) {
                val index = lichtwahl ?: 0
                fun aendern(neu: Blaulichtpunkt) =
                    lichterSetzen(icon.blaulichter.mapIndexed { n, p -> if (n == index) neu else p }, index, "Licht ${index + 1} gespeichert.")
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.fillMaxWidth().flaeche(randfarbe = Farben.Amber).padding(Abstand.Normal),
                ) {
                    Text("Licht ${index + 1}", style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    Lichtwahl("Form", licht.form, grenzen.blaulichtFormen.ifEmpty { listOf("rund", "eckig") }, ::lichtwort) { aendern(licht.copy(form = it)) }
                    Lichtwahl("Blitzmuster", licht.art, grenzen.blaulichtArten.ifEmpty { listOf("doppel", "vierfach", "rundum") }, ::lichtwort) { aendern(licht.copy(art = it)) }
                    Lichtwahl("Takt", licht.takt, grenzen.blaulichtTakte.ifEmpty { listOf("a", "b") }, ::lichtwort) { aendern(licht.copy(takt = it)) }
                    Lichtwahl("Farbe", licht.farbe, grenzen.blaulichtFarben.ifEmpty { listOf("blau", "gelb") }, ::lichtwort) { aendern(licht.copy(farbe = it)) }
                    Text("Breite ${((licht.b ?: 0.16) * 100).roundToInt()} %", style = Schrift.Winzig, color = Farben.TextLeise)
                    Regler(((licht.b ?: 0.16) * 100).roundToInt(), 4..60, 1, { aendern(licht.copy(b = it / 100.0)) })
                    Text("Höhe ${((licht.h ?: 0.16) * 100).roundToInt()} %", style = Schrift.Winzig, color = Farben.TextLeise)
                    Regler(((licht.h ?: 0.16) * 100).roundToInt(), 4..60, 1, { aendern(licht.copy(h = it / 100.0)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf("Licht entfernen", {
                            val neu = icon.blaulichter.filterIndexed { n, _ -> n != index }
                            lichterSetzen(neu, if (neu.isEmpty()) null else minOf(index, neu.size - 1))
                        }, kompakt = true, art = Knopfart.Gefahr, aktiv = !laeuft)
                        Knopf("Fertig", { lichtwahl = null }, kompakt = true, art = Knopfart.Leise)
                    }
                }
            }
            if (icon.blaulichter.isNotEmpty()) {
                Knopf("Alle Lichter entfernen", { lichterSetzen(emptyList(), null) }, kompakt = true, art = Knopfart.Leise, aktiv = !laeuft)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(if (icon == null) "Bild hochladen" else "Bild ersetzen", { bildwahl.launch("image/*") }, kompakt = true, aktiv = !laeuft)
            if (icon != null) {
                Knopf("Icon entfernen", { loeschfrage = true }, kompakt = true, art = Knopfart.Gefahr, aktiv = !laeuft)
            }
        }
        SehrLeise(
            "Größer als $FELD Punkte wird verkleinert; die kürzeste Kante braucht mindestens ${grenzen.minKante} Punkte, " +
                "höchstens ${kb(grenzen.maxBytesJeIcon)} KB je Icon.",
        )
        if (loeschfrage && icon != null) {
            Text("Das Icon für ${typ.typ} entfernen?", style = Schrift.Klein, color = Farben.SignalHell)
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Entfernen", {
                    loeschfrage = false
                    tun("Das Entfernen ging nicht.") {
                        wege.iconLoeschen(pack.id, typ.vorlageId)
                        beiGeaendert(null)
                        lichtwahl = null
                        hinweis = "Entfernt — für diesen Typ zeichnet das Spiel wieder selbst."
                    }
                }, kompakt = true, art = Knopfart.Gefahr)
                Knopf("Behalten", { loeschfrage = false }, kompakt = true, art = Knopfart.Leise)
            }
        }
        if (laeuft) Ladezeile("Wird gespeichert …")
    }
}

/**
 * Die Bühne: das Bild, und darauf die Blaulichter. Die Lage wird in 0–1 auf dem
 * Bild gemessen, nicht auf der Bühne — ersetzt man das Bild, liegen die Punkte
 * weiter „auf dem Dach".
 */
@Composable
private fun Blaulichtbuehne(
    icon: Packicon,
    server: String,
    gewaehlt: Int?,
    beiTipp: (Double, Double, Int?) -> Unit,
    beiSchieben: (Int, Double, Double) -> Unit,
) {
    val bild by bildVon(Iconpack.adresse(icon.bild).takeIf { server.isNotBlank() })
    val verhaeltnis = if ((icon.hoehe ?: 0) > 0 && icon.breite != null) icon.breite.toFloat() / icon.hoehe!! else 1f
    val punkte by rememberUpdatedState(icon.blaulichter)
    var zug by remember { mutableStateOf<Pair<Int, Offset>?>(null) }

    fun treffer(p: Offset, w: Float, h: Float): Int? =
        punkte.indices.minByOrNull { i -> hypot(punkte[i].x * w - p.x, punkte[i].y * h - p.y) }
            ?.takeIf { i -> hypot(punkte[i].x * w - p.x, punkte[i].y * h - p.y) < 28f + w * 0.03f }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .aspectRatio(verhaeltnis.coerceIn(0.3f, 3f))
                .background(Farben.BgTief, Rundung.Klein)
                .border(1.dp, Farben.Rand, Rundung.Klein),
        ) {
            bild?.let { Image(it, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize()) }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { p ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            beiTipp((p.x / w).toDouble().coerceIn(0.0, 1.0), (p.y / h).toDouble().coerceIn(0.0, 1.0), treffer(p, w, h))
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { p ->
                                val i = treffer(p, size.width.toFloat(), size.height.toFloat())
                                zug = i?.let { it to p }
                            },
                            onDragEnd = {
                                val z = zug
                                zug = null
                                if (z != null) {
                                    beiSchieben(
                                        z.first,
                                        (z.second.x / size.width).toDouble().coerceIn(0.0, 1.0),
                                        (z.second.y / size.height).toDouble().coerceIn(0.0, 1.0),
                                    )
                                }
                            },
                            onDragCancel = { zug = null },
                        ) { aenderung, schub ->
                            val z = zug ?: return@detectDragGestures
                            aenderung.consume()
                            zug = z.first to (z.second + schub)
                        }
                    },
            ) {
                punkte.forEachIndexed { i, l ->
                    val mitte = zug?.takeIf { it.first == i }?.second
                        ?: Offset((l.x * size.width).toFloat(), (l.y * size.height).toFloat())
                    val bb = ((l.b ?: 0.16) * size.width).toFloat()
                    val hh = ((l.h ?: 0.16) * size.height).toFloat()
                    val farbe = if (l.farbe == "gelb") Color(0xFFFFC247) else Color(0xFF4D8DFF)
                    if (l.form == "eckig") {
                        drawRect(farbe.copy(alpha = 0.75f), mitte - Offset(bb / 2, hh / 2), Size(bb, hh))
                    } else {
                        drawOval(farbe.copy(alpha = 0.75f), mitte - Offset(bb / 2, hh / 2), Size(bb, hh))
                    }
                    if (i == gewaehlt) drawCircle(Farben.Amber, maxOf(bb, hh) / 2 + 6f, mitte, style = Stroke(3f))
                }
            }
        }
    }
}

@Composable
private fun Lichtwahl(titel: String, wert: String?, moeglich: List<String>, wort: (String) -> String, beiWahl: (String?) -> Unit) {
    Text(titel, style = Schrift.Winzig, color = Farben.TextSehrLeise)
    Pillenreihe {
        Pille("Vorgabe", an = wert == null, beiDruck = { beiWahl(null) })
        moeglich.forEach { m -> Pille(wort(m), an = wert == m, beiDruck = { beiWahl(m) }) }
    }
}

private fun lichtwort(wert: String): String = when (wert) {
    "rund" -> "Rund / Punkt"
    "eckig" -> "Eckig / Balken"
    "doppel" -> "Doppelblitz"
    "vierfach" -> "Vierfachblitz"
    "rundum" -> "Rundumkennleuchte"
    "a" -> "Takt A"
    "b" -> "Takt B"
    "blau" -> "Blau"
    "gelb" -> "Gelb"
    else -> wert
}

private fun runden(wert: Double): Double = Math.round(wert * 1000) / 1000.0

private fun kb(bytes: Long): Long = Math.round(bytes / 1024.0)

private class Eingepasst(val daten: ByteArray, val breite: Int, val hoehe: Int, val verkleinert: Boolean)

/**
 * Ein Bild einpassen — `bildEinpassen` in `utils/iconbild.ts`.
 *
 * Auf höchstens [FELD] Punkte verkleinert und als WebP abgelegt, damit der
 * Server immer dasselbe Format bekommt. Passt es nicht in den Deckel je Icon,
 * geht die Güte schrittweise herunter, bevor aufgegeben wird — eine Absage des
 * Servers nach dem Hochladen wäre der teurere Weg zur selben Auskunft.
 */
private fun bildEinpassen(zusammenhang: Context, uri: Uri, grenzen: IconGrenzen): Eingepasst {
    val bytes = zusammenhang.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("Die Datei ließ sich nicht lesen.")
    val masse = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, masse)
    if (masse.outWidth <= 0 || masse.outHeight <= 0) error("Das ist kein Bild, das dieses Gerät lesen kann.")

    var stichprobe = 1
    while (masse.outWidth / (stichprobe * 2) >= FELD && masse.outHeight / (stichprobe * 2) >= FELD) stichprobe *= 2
    val roh = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = stichprobe })
        ?: error("Das ist kein Bild, das dieses Gerät lesen kann.")

    val faktor = minOf(1f, FELD.toFloat() / roh.width, FELD.toFloat() / roh.height)
    val breite = maxOf(1, (roh.width * faktor).roundToInt())
    val hoehe = maxOf(1, (roh.height * faktor).roundToInt())
    if (minOf(masse.outWidth, masse.outHeight) < grenzen.minKante) {
        error("Das Bild ist zu klein — die kürzeste Kante braucht mindestens ${grenzen.minKante} Punkte.")
    }
    val passend = if (breite != roh.width || hoehe != roh.height) Bitmap.createScaledBitmap(roh, breite, hoehe, true) else roh

    @Suppress("DEPRECATION")
    val form = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
    var guete = 92
    var ergebnis: ByteArray
    do {
        val aus = ByteArrayOutputStream()
        passend.compress(form, guete, aus)
        ergebnis = aus.toByteArray()
        guete -= 12
    } while (ergebnis.size > grenzen.maxBytesJeIcon && guete > 20)
    if (ergebnis.size > grenzen.maxBytesJeIcon) error("Das Bild ist auch verkleinert größer als ${kb(grenzen.maxBytesJeIcon)} KB.")

    return Eingepasst(ergebnis, breite, hoehe, faktor < 1f || stichprobe > 1)
}

/** Name und Bytes einer gewählten Datei — der Name trägt beim Zip-Import den Packnamen. */
private fun dateiLesen(zusammenhang: Context, uri: Uri): Pair<String, ByteArray?> {
    var name = "Pack.zip"
    runCatching {
        zusammenhang.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) name = c.getString(0) ?: name
        }
    }
    val bytes = runCatching { zusammenhang.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
    return name to bytes
}
