package de.pagerspass.pagerspass.ansichten.melder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ansichten.welt.Regler
import de.pagerspass.pagerspass.melder.Bauplaene
import de.pagerspass.pagerspass.melder.Bauteil
import de.pagerspass.pagerspass.melder.Melderbauplan
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/** Die Farben der Werkstatt — Gehäusetöne, Displaygründe, Tinten und Signalfarben. */
private val WERKSTATTFARBEN = listOf(
    "#1b1f25", "#2a2f36", "#3a4149", "#6d7680", "#8d9399", "#d7dade", "#f4f1e8",
    "#3d1512", "#c62828", "#ff5a4d", "#ff8a1f", "#f2c300", "#83683a", "#b8a45c",
    "#16223a", "#1c7ed6", "#7fb7e8", "#0d4c52", "#2f9e44", "#8fd14f", "#a5b34c",
    "#1a1f0a", "#0b0d10", "#e7edf5", "#43185c", "#d8c4ee",
)

/**
 * Die Gehäusewerkstatt — `components/melder/Gehaeusewerkstatt.vue`, fürs Handy.
 *
 * <b>Im Web gibt es sie nur am Rechner</b> („am Handy wären die Teile zu klein,
 * um sie zu treffen"). Hier ist sie trotzdem, aber anders bedient: Man tippt ein
 * Teil an, um es zu wählen, und schiebt es mit dem Finger oder feldweise mit den
 * Pfeilen; Größe, Farbe, Aufgabe und Aufschrift stehen darunter als Knöpfe statt
 * als Griffe am Teil. Dasselbe Raster, dieselben achtzehn Bauteile, derselbe
 * `PSM2.`-Code — ein Gerät, das man am Rechner gebaut hat, kommt per Code herüber.
 */
@Composable
fun Gehaeusewerkstatt(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    melderId: String?,
    gesicht: String,
    beiZurueck: () -> Unit,
) {
    val zusammenhang = LocalContext.current
    val geraet = remember { Meldergeraet.bereit(zusammenhang) }
    val vorhanden = remember(melderId) { geraet.eigeneMelder.firstOrNull { it.id == melderId } }

    var id by remember(melderId) { mutableStateOf(vorhanden?.id) }
    var name by remember(melderId) { mutableStateOf(vorhanden?.name ?: "") }
    var plan by remember(melderId) { mutableStateOf(vorhanden?.plan ?: Bauplaene.neuerBauplan()) }
    var gewaehlt by remember { mutableStateOf<String?>(null) }
    var gruppe by remember { mutableStateOf("anzeige") }
    var code by remember { mutableStateOf("") }
    var meldung by remember { mutableStateOf<String?>(null) }
    var codeZeigen by remember { mutableStateOf(false) }

    fun teilAendern(tid: String, aendern: (Bauteil) -> Bauteil) {
        plan = plan.copy(teile = plan.teile.map { if (it.id == tid) aendern(it) else it })
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Gehäusewerkstatt",
            unterzeile = "Dein eigener Melder — der Bauplan bleibt auf diesem Gerät",
            knoepfe = { Knopf("Fertig", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        if (vorhanden == null && id == null) {
            Abschnitt("Vorlage") {
                SehrLeise("Womit du anfängst — alles lässt sich danach ändern.")
                Pillenreihe {
                    Bauplaene.VORLAGEN.forEach { (wort, _, bau) ->
                        Pille(wort, an = false, beiDruck = { plan = bau(); gewaehlt = null })
                    }
                }
            }
        }

        // ------------------------------------------------------ Werkbank
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Werkbank(
                plan = plan,
                gewaehlt = gewaehlt,
                beiWahl = { gewaehlt = it },
                beiSchieben = { tid, dx, dy -> teilAendern(tid) { it.copy(x = it.x + dx, y = it.y + dy) } },
                modifier = Modifier
                    .widthIn(max = if (plan.breite > plan.hoehe) 380.dp else 260.dp)
                    .heightIn(max = 460.dp),
            )
        }
        SehrLeise("Ein Teil antippen wählt es, ziehen schiebt es. Leerer Platz hebt die Wahl auf.")

        val teil = plan.teile.firstOrNull { it.id == gewaehlt }
        if (teil != null) {
            Teilbearbeitung(
                teil = teil,
                beiAendern = { neu -> teilAendern(teil.id) { neu } },
                beiLoeschen = { plan = plan.copy(teile = plan.teile.filterNot { it.id == teil.id }); gewaehlt = null },
                beiDoppeln = {
                    if (plan.teile.size < Bauplaene.TEILE_HOECHSTZAHL) {
                        val kopie = teil.copy(id = Bauplaene.kennung(), x = teil.x + 2, y = teil.y + 2)
                        plan = plan.copy(teile = plan.teile + kopie)
                        gewaehlt = kopie.id
                    }
                },
                beiNachVorn = {
                    plan = plan.copy(teile = plan.teile.filterNot { it.id == teil.id } + teil)
                },
            )
        }

        Abschnitt("Bauteil hinzufügen (${plan.teile.size}/${Bauplaene.TEILE_HOECHSTZAHL})") {
            Segment(
                seiten = Bauplaene.GRUPPEN.map { it.first },
                gewaehlt = gruppe,
                beiWahl = { gruppe = it },
                aufschrift = { g -> Bauplaene.GRUPPEN.first { it.first == g }.second },
                modifier = Modifier.fillMaxWidth(),
            )
            Pillenreihe {
                Bauplaene.ARTEN.filter { it.gruppe == gruppe }.forEach { m ->
                    Pille(m.name, an = false, aktiv = plan.teile.size < Bauplaene.TEILE_HOECHSTZAHL, beiDruck = {
                        val neu = Bauplaene.bauteil(
                            m.art,
                            x = ((plan.breite - m.breite) / 2).coerceAtLeast(0),
                            y = ((plan.hoehe - m.hoehe) / 2).coerceAtLeast(0),
                        )
                        plan = plan.copy(teile = plan.teile + neu)
                        gewaehlt = neu.id
                    })
                }
            }
            Bauplaene.ARTEN.filter { it.gruppe == gruppe }.forEach {
                Text("${it.name} — ${it.was}", style = Schrift.Winzig, color = Farben.TextSehrLeise)
            }
        }

        Abschnitt("Gehäuse") {
            Text("Breite ${plan.breite} · Höhe ${plan.hoehe} · Rundung ${plan.rundung}", style = Schrift.MonoKlein, color = Farben.TextLeise)
            Regler(plan.breite, Bauplaene.GRENZEN_BREITE, 1, { plan = plan.copy(breite = it) })
            Regler(plan.hoehe, Bauplaene.GRENZEN_HOEHE, 1, { plan = plan.copy(hoehe = it) })
            Regler(plan.rundung, 0..12, 1, { plan = plan.copy(rundung = it) })
            Segment(
                seiten = Bauplaene.OBERFLAECHEN.map { it.first },
                gewaehlt = plan.oberflaeche,
                beiWahl = { plan = plan.copy(oberflaeche = it) },
                aufschrift = { o -> Bauplaene.OBERFLAECHEN.first { it.first == o }.second },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Gehäusefarbe", style = Schrift.Winzig, color = Farben.TextLeise)
            Farbwahl(plan.gehaeuse) { plan = plan.copy(gehaeuse = it) }
            Text("Randfarbe", style = Schrift.Winzig, color = Farben.TextLeise)
            Farbwahl(plan.rand) { plan = plan.copy(rand = it) }
        }

        Abschnitt("Abnahme") {
            val befunde = Bauplaene.abnahme(plan)
            if (befunde.isEmpty()) Text("Alles da — das Gerät kann in den Dienst.", style = Schrift.Klein, color = Farben.GruenHell)
            befunde.forEach { b ->
                Text(
                    (if (b.mangel) "✗ " else "• ") + b.wort + " — " + b.satz,
                    style = Schrift.Klein,
                    color = if (b.mangel) Farben.SignalHell else Farben.TextLeise,
                    modifier = if (b.teil != null) Modifier.clickable { gewaehlt = b.teil } else Modifier,
                )
            }
        }

        Abschnitt("Sichern") {
            Feld(wert = name, beiAenderung = { name = it.take(Bauplaene.NAME_LAENGE) }, etikett = "Name", platzhalter = "Zum Beispiel „FF Musterdorf“")
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Sichern", {
                    val satz = geraet.melderSichern(id, name, plan)
                    meldung = if (satz == null) {
                        "Mehr als ${Bauplaene.MELDER_HOECHSTZAHL} eigene Melder gehen nicht."
                    } else {
                        id = satz.id
                        name = satz.name
                        "Gesichert."
                    }
                }, kompakt = true)
                val sid = id
                if (sid != null) {
                    Knopf("In die Tasche", {
                        geraet.melderSichern(sid, name, plan)
                        geraet.bauformSetzen("eigen:$sid")
                        meldung = "Dieser Melder steckt jetzt in deiner Tasche."
                    }, kompakt = true, art = Knopfart.Leise)
                    Knopf("Löschen", {
                        geraet.melderLoeschen(sid)
                        beiZurueck()
                    }, kompakt = true, art = Knopfart.Gefahr)
                }
            }
            meldung?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }
        }

        Abschnitt("Weitergeben") {
            SehrLeise("Ein Melder reist als Code — derselbe `PSM2.`-Code wie in der Werkstatt am Rechner.")
            if (codeZeigen) {
                SelectionContainer {
                    Text(Bauplaene.codeVon(plan), style = Schrift.MonoKlein, color = Farben.Amber)
                }
            } else {
                Knopf("Code zeigen", { codeZeigen = true }, kompakt = true, art = Knopfart.Leise)
            }
            Feld(wert = code, beiAenderung = { code = it }, etikett = "Code einlesen", platzhalter = "PSM2.…")
            Knopf("Auf die Werkbank", {
                val gelesen = Bauplaene.codeLesen(code)
                if (gelesen == null) {
                    meldung = "Dieser Code passt nicht."
                } else {
                    plan = gelesen
                    gewaehlt = null
                    code = ""
                    meldung = "Der Bauplan liegt auf der Werkbank — sichern nicht vergessen."
                }
            }, kompakt = true, aktiv = code.isNotBlank())
        }
    }
}

/**
 * Die Werkbank: der Plan, gezeichnet — antippen wählt, ziehen schiebt.
 *
 * Geschoben wird in ganzen Feldern: Der Finger sammelt Pixel, und sobald sie ein
 * Feld ausmachen, rückt das Teil um eines weiter. So bleibt der Plan im Raster,
 * wie im Web, und der Code trägt ganze Zahlen.
 */
@Composable
private fun Werkbank(
    plan: Melderbauplan,
    gewaehlt: String?,
    beiWahl: (String?) -> Unit,
    beiSchieben: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messer = rememberTextMeasurer()
    val rahmen = planRahmen(plan)
    val aktuellerPlan by rememberUpdatedState(plan)
    val aktuellWahl by rememberUpdatedState(gewaehlt)
    var restX by remember { mutableFloatStateOf(0f) }
    var restY by remember { mutableFloatStateOf(0f) }
    val anzeige = Melderanzeige.aus(PROBEALARM, false)

    Canvas(
        modifier = modifier
            .aspectRatio(rahmen.width / rahmen.height)
            .background(Farben.BgTief)
            .pointerInput(Unit) {
                detectTapGestures { p ->
                    val r = planRahmen(aktuellerPlan)
                    val u = size.width / r.width
                    val fx = p.x / u + r.left
                    val fy = p.y / u + r.top
                    val treffer = aktuellerPlan.teile.lastOrNull { t ->
                        fx >= t.x - 0.5f && fx <= t.x + t.breite + 0.5f && fy >= t.y - 0.5f && fy <= t.y + t.hoehe + 0.5f
                    }
                    beiWahl(treffer?.id)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { p: Offset ->
                        restX = 0f
                        restY = 0f
                        val r = planRahmen(aktuellerPlan)
                        val u = size.width / r.width
                        val fx = p.x / u + r.left
                        val fy = p.y / u + r.top
                        val treffer = aktuellerPlan.teile.lastOrNull { t ->
                            fx >= t.x - 0.5f && fx <= t.x + t.breite + 0.5f && fy >= t.y - 0.5f && fy <= t.y + t.hoehe + 0.5f
                        }
                        if (treffer != null) beiWahl(treffer.id)
                    },
                ) { aenderung, schub ->
                    val tid = aktuellWahl ?: return@detectDragGestures
                    aenderung.consume()
                    val u = size.width / planRahmen(aktuellerPlan).width
                    restX += schub.x / u
                    restY += schub.y / u
                    val dx = restX.toInt()
                    val dy = restY.toInt()
                    if (dx != 0 || dy != 0) {
                        restX -= dx
                        restY -= dy
                        beiSchieben(tid, dx, dy)
                    }
                }
            },
    ) {
        eigenerMelder(plan, anzeige, messer, 0f, gewaehlt)
    }
}

@Composable
private fun Teilbearbeitung(
    teil: Bauteil,
    beiAendern: (Bauteil) -> Unit,
    beiLoeschen: () -> Unit,
    beiDoppeln: () -> Unit,
    beiNachVorn: () -> Unit,
) {
    val m = Bauplaene.muster(teil.art)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche(randfarbe = Farben.Amber).padding(Abstand.Normal),
    ) {
        Text(m.name, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        Text("Lage ${teil.x}/${teil.y} · Größe ${teil.breite}×${teil.hoehe}", style = Schrift.MonoKlein, color = Farben.TextLeise)
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
            Text("Schieben", style = Schrift.Winzig, color = Farben.TextSehrLeise, modifier = Modifier.weight(1f))
            Knopf("◀", { beiAendern(teil.copy(x = teil.x - 1)) }, kompakt = true, art = Knopfart.Leise)
            Knopf("▲", { beiAendern(teil.copy(y = teil.y - 1)) }, kompakt = true, art = Knopfart.Leise)
            Knopf("▼", { beiAendern(teil.copy(y = teil.y + 1)) }, kompakt = true, art = Knopfart.Leise)
            Knopf("▶", { beiAendern(teil.copy(x = teil.x + 1)) }, kompakt = true, art = Knopfart.Leise)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
            Text("Größe", style = Schrift.Winzig, color = Farben.TextSehrLeise, modifier = Modifier.weight(1f))
            Knopf("B−", { beiAendern(teil.copy(breite = (teil.breite - 1).coerceAtLeast(1))) }, kompakt = true, art = Knopfart.Leise)
            Knopf("B+", { beiAendern(teil.copy(breite = (teil.breite + 1).coerceAtMost(52))) }, kompakt = true, art = Knopfart.Leise)
            Knopf("H−", { beiAendern(teil.copy(hoehe = (teil.hoehe - 1).coerceAtLeast(1))) }, kompakt = true, art = Knopfart.Leise)
            Knopf("H+", { beiAendern(teil.copy(hoehe = (teil.hoehe + 1).coerceAtMost(96))) }, kompakt = true, art = Knopfart.Leise)
        }
        Text("Rundung ${teil.rundung}", style = Schrift.Winzig, color = Farben.TextSehrLeise)
        Regler(teil.rundung, 0..24, 1, { beiAendern(teil.copy(rundung = it)) })
        if (m.varianten.isNotEmpty()) {
            Pillenreihe {
                m.varianten.forEachIndexed { i, v -> Pille(v, an = teil.variante == i, beiDruck = { beiAendern(teil.copy(variante = i)) }) }
            }
        }
        if (m.hatAufgabe) {
            Text("Aufgabe", style = Schrift.Winzig, color = Farben.TextSehrLeise)
            Pillenreihe {
                Bauplaene.AUFGABEN.forEach { (aid, wort) ->
                    Pille(wort, an = teil.aufgabe == aid, beiDruck = { beiAendern(teil.copy(aufgabe = aid)) })
                }
            }
        }
        if (m.hatText) {
            Feld(wert = teil.text, beiAenderung = { beiAendern(teil.copy(text = it.take(16))) }, etikett = "Aufschrift")
        }
        Text("Farbe", style = Schrift.Winzig, color = Farben.TextSehrLeise)
        Farbwahl(teil.farbe) { beiAendern(teil.copy(farbe = it)) }
        Text("Zweitfarbe", style = Schrift.Winzig, color = Farben.TextSehrLeise)
        Farbwahl(teil.zweitfarbe) { beiAendern(teil.copy(zweitfarbe = it)) }
        Pillenreihe {
            Pille("Nach vorn", an = false, beiDruck = beiNachVorn)
            Pille("Doppeln", an = false, beiDruck = beiDoppeln)
            Pille("Wegwerfen", an = false, farbe = Farben.Signal, beiDruck = beiLoeschen)
        }
    }
}

@Composable
private fun Farbwahl(gewaehlt: String, beiWahl: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WERKSTATTFARBEN.forEach { f ->
            val an = f.equals(gewaehlt.take(7), ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(hexfarbe(f), CircleShape)
                    .border(if (an) 3.dp else 1.dp, if (an) Farben.Amber else Farben.Rand, CircleShape)
                    .clickable(role = Role.RadioButton) { beiWahl(f) },
            )
        }
    }
}
