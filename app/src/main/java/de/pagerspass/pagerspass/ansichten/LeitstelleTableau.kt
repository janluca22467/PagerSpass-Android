package de.pagerspass.pagerspass.ansichten

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlin.math.roundToInt

/**
 * Das Fahrzeugtableau — `FahrzeugTableau.vue`.
 *
 * Reiter je Abteilung, eine Suche über Rufname, Typ, Fähigkeit und Schleife,
 * und die dichte Ansicht, wenn nur Status und Kennung zählen. Jede Kachel sagt,
 * was mit dem Fahrzeug gerade ist: wohin es fährt und wie lange noch, ob die
 * Besatzung erst anrückt, was an Stellen fehlt, ob es auf Streife ist.
 *
 * <b>Anrufen geht von hier</b> (✆, der Einzelruf) — an jedes besetzte
 * Fahrzeug, solange die Leitstelle nicht schon telefoniert. <b>Umbenennen,
 * Kanal umschalten und Umrüsten</b> gehören der Lobby, wie im Web.
 */
@Composable
internal fun Fahrzeugtableau(
    raum: Raumzustand,
    stand: Rundenstand,
    katalog: Katalog?,
    hervorheben: List<String>,
    griffe: LeitstellenGriffe,
    modifier: Modifier = Modifier,
) {
    val zusammenhang = LocalContext.current
    val ablage = remember { zusammenhang.getSharedPreferences("pagerspass-tableau", Context.MODE_PRIVATE) }
    val jetzt = rememberJetzt()
    val kennung = rememberKennung(raum)
    val kennzahl = kennungIstKennzahl()
    val mitMikrofon = rememberMikrofonfrage()

    var suche by remember { mutableStateOf("") }
    var reiter by remember { mutableStateOf("alle") }
    var kompakt by remember { mutableStateOf(ablage.getBoolean("kompakt", false)) }
    var umbenennen by remember { mutableStateOf<String?>(null) }
    var kanalwahl by remember { mutableStateOf<String?>(null) }
    var abwahl by remember { mutableStateOf<String?>(null) }
    var anrufGesperrtBis by remember { mutableStateOf(0L) }

    val ich = stand.eigeneKennung
    val istLeitstelle = stand.ich?.istLeitstelle == true
    val darfUmbenennen = istLeitstelle && raum.inLobby
    val rufe = raum.einzelrufe
    val amTelefon = rufe.any { r ->
        (r.laeuft && (r.vonPlayerId == ich || r.angenommenVonPlayerId == ich)) ||
            (r.klingelt && r.vonPlayerId == ich) ||
            (r.klingelt && (r.zielPlayerId == ich || (r.zielPlayerId == null && istLeitstelle)))
    }

    val gefunden = raum.vehicles.filter { fahrzeugPasst(it, suche) }
    val gruppen = gruppiereTableau(gefunden)
    val alleGruppen = gruppiereTableau(raum.vehicles)
    // Verschwindet die gewählte Abteilung, steht man sonst vor einem leeren Feld.
    val aktiverReiter = if (reiter != "alle" && alleGruppen.none { it.schluessel == reiter }) "alle" else reiter
    val sichtbar = if (aktiverReiter == "alle") gruppen else gruppen.filter { it.schluessel == aktiverReiter }

    val kreisgruppen = kreisfunkgruppen(raum)
    val getrennt = raum.settings.funkgruppen.isNotEmpty()
    val abVorlagen = katalog?.fahrzeuge.orEmpty().filter { it.kategorie == "Abrollbehälter" }
    val belegteAb = raum.vehicles.mapNotNull { it.abrollbehaelterTemplateId }.toSet()
    fun abName(id: String?): String =
        if (id == null) "ohne AB" else abVorlagen.firstOrNull { it.id == id }?.typ?.replace("WLF + ", "") ?: id

    Column(modifier = modifier) {
        if (raum.vehicles.isEmpty()) {
            Leerteil("Keine Fahrzeuge im Dienst.")
            return@Column
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            Pille("Alle", an = aktiverReiter == "alle", zahl = gefunden.size, beiDruck = { reiter = "alle" })
            gruppen.forEach { g ->
                Pille(
                    aufschrift = g.label,
                    an = aktiverReiter == g.schluessel,
                    farbe = g.farbe,
                    zahl = g.fahrzeuge.size,
                    beiDruck = { reiter = g.schluessel },
                    zeichenVorn = { Box(Modifier.size(8.dp).background(g.farbe, CircleShape)) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Abstand.Normal),
        ) {
            Feld(
                wert = suche,
                beiAenderung = { suche = it },
                platzhalter = "Rufname, Typ …",
                modifier = Modifier.weight(1f),
            )
            Pille(
                aufschrift = "Dicht",
                an = kompakt,
                beiDruck = {
                    kompakt = !kompakt
                    ablage.edit().putBoolean("kompakt", kompakt).apply()
                },
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Abstand.Normal),
        ) {
            if (gefunden.isEmpty() || sichtbar.isEmpty()) {
                SehrLeise("Kein Fahrzeug passt zu „$suche“.", mono = true)
            }
            sichtbar.forEach { g ->
                if (aktiverReiter == "alle") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = Abstand.Klein),
                    ) {
                        Box(Modifier.width(3.dp).height(14.dp).background(g.farbe))
                        Etikett(g.label)
                        SehrLeise("${g.fahrzeuge.size}", mono = true)
                    }
                }
                g.fahrzeuge.forEach { f ->
                    Kachel(
                        f = f,
                        raum = raum,
                        jetzt = jetzt,
                        kennung = kennung(f),
                        typZeigen = kennzahl,
                        kompakt = kompakt,
                        hervor = f.id in hervorheben,
                        darfAnrufen = istLeitstelle && raum.laeuft && besatzungVon(raum.players, f.playerId) != null,
                        amTelefon = amTelefon,
                        darfStreife = istLeitstelle && raum.laeuft && f.streifenfaehig &&
                            (f.aufStreife || (f.einsatzId == null && (f.status == 1 || f.status == 2))),
                        darfUmbenennen = darfUmbenennen,
                        getrennt = getrennt,
                        abName = if (f.templateId == "wlfab") abName(f.abrollbehaelterTemplateId) else null,
                        darfUmruesten = darfUmbenennen && f.templateId == "wlfab" && f.status == 2 && f.einsatzId == null,
                        beiAnrufen = {
                            val t = System.currentTimeMillis()
                            if (!amTelefon && t >= anrufGesperrtBis) {
                                anrufGesperrtBis = t + 1_500
                                // Erst das Mikrofon, dann der Ruf — mit einem Menschen
                                // ist es ein Telefonat.
                                mitMikrofon { griffe.einzelrufStarten(f.id) }
                            }
                        },
                        beiStreife = { griffe.streife(f.id, !f.aufStreife) },
                        beiUmbenennen = { umbenennen = f.id },
                        beiKanal = { kanalwahl = if (kanalwahl == f.id) null else f.id },
                        beiAb = { abwahl = if (abwahl == f.id) null else f.id },
                    )
                    if (kanalwahl == f.id) {
                        Pillenreihe {
                            Pille("Stammgruppe", an = !f.funkgruppeAufgeschaltet, beiDruck = {
                                kanalwahl = null
                                griffe.funkgruppeZuweisen(f.id, null)
                            })
                            kreisgruppen.forEach { gr ->
                                Pille(
                                    gr.marke.ifBlank { gr.name },
                                    an = f.funkgruppeAufgeschaltet && f.funkgruppe == gr.id,
                                    farbe = Farben.kanal(funkgruppeRang(raum, gr.id)),
                                    beiDruck = {
                                        kanalwahl = null
                                        griffe.funkgruppeZuweisen(f.id, gr.id)
                                    },
                                )
                            }
                        }
                    }
                    if (abwahl == f.id) {
                        Pillenreihe {
                            Pille("Absetzen", an = f.abrollbehaelterTemplateId == null, beiDruck = {
                                abwahl = null
                                griffe.abrollbehaelter(f.id, null)
                            })
                            abVorlagen
                                .filter { it.id !in belegteAb || it.id == f.abrollbehaelterTemplateId }
                                .forEach { ab ->
                                    Pille(
                                        ab.typ.replace("WLF + ", ""),
                                        an = f.abrollbehaelterTemplateId == ab.id,
                                        beiDruck = {
                                            abwahl = null
                                            griffe.abrollbehaelter(f.id, ab.id)
                                        },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }

    raum.vehicles.firstOrNull { it.id == umbenennen }?.let { f ->
        Rufnameblende(
            fahrzeug = f,
            raum = raum,
            griffe = griffe,
            beiSchliessen = { umbenennen = null },
        )
    }
}

/** Eine Kachel des Tableaus — Status links, darunter was gerade mit dem Fahrzeug ist. */
@Composable
private fun Kachel(
    f: Rundenfahrzeug,
    raum: Raumzustand,
    jetzt: Long,
    kennung: String,
    typZeigen: Boolean,
    kompakt: Boolean,
    hervor: Boolean,
    darfAnrufen: Boolean,
    amTelefon: Boolean,
    darfStreife: Boolean,
    darfUmbenennen: Boolean,
    getrennt: Boolean,
    abName: String?,
    darfUmruesten: Boolean,
    beiAnrufen: () -> Unit,
    beiStreife: () -> Unit,
    beiUmbenennen: () -> Unit,
    beiKanal: () -> Unit,
    beiAb: () -> Unit,
) {
    val besatzung = besatzungVon(raum.players, f.playerId)
    val funkloch = (zeitMillis(f.funklochBis) ?: 0L) > jetzt
    val einsatz = f.einsatzId?.let { id -> raum.incidents.firstOrNull { it.id == id } }
    val blinkt = f.status == 5 || f.status == 0 || f.sprechwunschSeit != null

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (hervor) Farben.FlaecheAktiv else Farben.Flaeche,
                randfarbe = when {
                    f.alarmOffen -> Farben.SignalHell
                    hervor -> Farben.Amber
                    else -> kachelFarbe(f).copy(alpha = 0.55f)
                },
                ecke = 9.dp,
            )
            .padding(horizontal = Abstand.Normal, vertical = if (kompakt) Abstand.Winzig else Abstand.Klein),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (kompakt) 28.dp else 34.dp)
                .background(fmsFarbe(f.status), CircleShape)
                .then(if (blinkt) Modifier.border(2.dp, Farben.BlauHell, CircleShape) else Modifier),
        ) {
            Text(f.status.toString(), style = Schrift.MarkeZahl, color = Farben.AufFarbe)
        }

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    kennung,
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = if (funkloch) Farben.TextSehrLeise else Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (besatzung?.istBot == true) {
                    Text("BOT", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
                } else if (besatzung != null) {
                    Text(
                        besatzung.name,
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = Farben.GruenHell,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (darfAnrufen) {
                    Text(
                        "✆",
                        style = Schrift.Normal,
                        color = if (amTelefon) Farben.TextSehrLeise else Farben.GruenHell,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 36.dp, minHeight = 32.dp)
                            .clickable(enabled = !amTelefon, onClick = beiAnrufen)
                            .padding(horizontal = Abstand.Klein),
                    )
                }
            }
            if (typZeigen) SehrLeise(f.typ)

            if (kompakt) return@Column

            // Die Kanalmarke — nur, wo es getrennte Gruppen gibt.
            funkgruppeVon(raum, f.funkgruppe)?.takeIf { getrennt }?.let { gr ->
                Text(
                    gr.marke.ifBlank { gr.name } + if (f.funkgruppeAufgeschaltet) " ↷" else "",
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                    color = Farben.kanal(funkgruppeRang(raum, gr.id)),
                    modifier = if (darfUmbenennen) Modifier.clickable(onClick = beiKanal) else Modifier,
                )
            }
            abName?.let {
                Text(
                    it,
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                    color = Farben.TextLeise,
                    modifier = if (darfUmruesten) Modifier.clickable(onClick = beiAb) else Modifier,
                )
            }

            val dauer = zeitMillis(f.statusSeit)?.let { ((jetzt - it) / 60_000).toInt() }?.takeIf { it >= 1 }
            val wiederher = zeitMillis(f.wiederherstellungBis)?.let { ((it - jetzt) / 1000).toInt() }?.takeIf { it > 0 }
            Text(
                buildString {
                    append(f.statusText)
                    dauer?.let { append(" · seit $it min") }
                    f.ausserDienstGrund?.let { g ->
                        append(" — $g")
                        wiederher?.let { append(", noch ${minuten(it)}") }
                    }
                },
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
            einsatz?.let { e ->
                val ort = e.ortsteil?.let { "${e.adresse} ($it)" } ?: e.adresse
                Zeile("${e.stichwort} · $ort", Farben.Text)
            }
            if (f.sprechwunschSeit != null) {
                Zeile("◉ Sprechwunsch" + (f.sprechwunschAnliegen?.let { " — $it" } ?: ""), Farben.BlauHell)
            }
            val begleitetVon = raum.vehicles.firstOrNull { it.notarztBei == f.funkrufname }?.funkrufname
            if (f.notarztBei != null) {
                Zeile("ohne NA — begleitet ${f.notarztBei}", Farben.TextLeise)
            } else if (begleitetVon != null) {
                Zeile("✚ Notarzt an Bord — von $begleitetVon", Farben.SignalHell)
            }
            when {
                funkloch -> Zeile("⚡ kein Funkkontakt", Farben.TextSehrLeise)
                anfahrtText(f, raum) != null -> Zeile(anfahrtText(f, raum).orEmpty(), Farben.AmberHell)
                transportText(f, raum) != null -> Zeile(transportText(f, raum).orEmpty(), Farben.AmberHell)
                ausrueckText(f, jetzt) != null -> Zeile(
                    ausrueckText(f, jetzt).orEmpty(),
                    if (!f.besatzungVerfuegbar) Farben.SignalHell else Farben.TextLeise,
                )
            }
            if (f.besatzungsluecken.isNotEmpty()) {
                Zeile(
                    "⚑ " + f.besatzungsluecken.joinToString(" · ") { l ->
                        if (l.soll > 1) "${l.name} ${l.ist}/${l.soll}" else "ohne ${l.name}"
                    },
                    Farben.OrangeHell,
                )
            }
            if (f.aufStreife) {
                Zeile("⇢ Streife" + (f.streifenziel?.let { " — $it" } ?: ""), Farben.GruenHell)
            }
            wasserText(f)?.let {
                Zeile(it, if (f.wasserLiter <= 0 && f.entnahmestelleId == null) Farben.SignalHell else Farben.BlauHell)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            ) {
                SehrLeise(seitKurz(f.statusSeit, jetzt), mono = true)
                traegerLabel(f).takeIf { it.isNotEmpty() }?.let { SehrLeise("· $it", mono = true) }
                einsatz?.let { SehrLeise("· ${it.einsatznummer}", mono = true) }
                if (f.alarmOffen) Text("· nicht quittiert", style = Schrift.MonoKlein, color = Farben.SignalHell)
            }
            if (darfStreife || darfUmbenennen) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    if (darfStreife) {
                        Knopf(
                            if (f.aufStreife) "einrücken" else "Streife",
                            beiStreife,
                            art = if (f.aufStreife) Knopfart.Haupt else Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                    if (darfUmbenennen) Knopf("✎ Rufname", beiUmbenennen, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
    }
}

@Composable
private fun Zeile(text: String, farbe: androidx.compose.ui.graphics.Color) {
    Text(text, style = Schrift.MonoKlein, color = farbe)
}

private fun minuten(sekunden: Int): String =
    if (sekunden < 60) "${sekunden}s" else "${(sekunden / 60.0).roundToInt()} min"

private fun seitKurz(iso: String, jetzt: Long): String {
    val dann = zeitMillis(iso) ?: return ""
    val s = ((jetzt - dann) / 1000).coerceAtLeast(0)
    return if (s < 60) "${s}s" else "${s / 60}m"
}

private fun anfahrtText(f: Rundenfahrzeug, raum: Raumzustand): String? {
    if (f.status != 3 || f.einsatzId == null) return null
    val e = raum.incidents.firstOrNull { it.id == f.einsatzId } ?: return null
    val eLat = e.lat ?: return null
    val eLon = e.lon ?: return null
    val fLat = f.lat ?: return null
    val fLon = f.lon ?: return null
    val meter = restEntfernungMeter(fLat, fLon, f.route, f.routeIndex, eLat, eLon)
    val sekunden = meter / tempoMs(raum, f.organisation, if (f.sondersignalAus) 1 else e.prioritaet)
    return "${formatEntfernung(meter)} · ca. ${formatAnfahrtszeit(sekunden)}"
}

private fun transportText(f: Rundenfahrzeug, raum: Raumzustand): String? {
    if (f.status != 7 || f.zielklinikId == null || f.zielklinikErreicht) return null
    val k = raum.kliniken.firstOrNull { it.id == f.zielklinikId } ?: return null
    val fLat = f.lat ?: return null
    val fLon = f.lon ?: return null
    val meter = restEntfernungMeter(fLat, fLon, f.route, f.routeIndex, k.lat, k.lon)
    val e = raum.incidents.firstOrNull { it.id == f.einsatzId }
    val prio = if (f.sondersignalAus) 1 else (e?.prioritaet ?: 2)
    val sekunden = meter / tempoMs(raum, f.organisation, prio)
    return "→ ${k.name} · ${formatEntfernung(meter)} · ca. ${formatAnfahrtszeit(sekunden)}"
}

private fun ausrueckText(f: Rundenfahrzeug, jetzt: Long): String? {
    zeitMillis(f.besatzungAb)?.let { ab ->
        val rest = ((ab - jetzt) / 1000).toInt()
        if (rest > 0) return "Besatzung rückt an · noch ${minuten(rest)}"
    }
    if (!f.besatzungVerfuegbar) return "keine Besatzung verfügbar"
    if (f.ausrueckzeitSekunden <= 0) return null
    return "Ausrückzeit ca. ${minuten(f.ausrueckzeitSekunden)}"
}

private fun wasserText(f: Rundenfahrzeug): String? {
    if (f.tankLiter <= 0) return null
    if (f.entnahmestelleErreicht) return "nimmt Wasser auf"
    if (f.entnahmestelleId != null) return "holt Wasser"
    if (f.wasserLiter.toDouble() / f.tankLiter > 0.5) return null
    return "Wasser ${f.wasserLiter} l"
}

/**
 * Den Funkrufnamen von Hand vergeben — `RufnameDialog.vue`.
 *
 * Der Name ist eine Beschriftung, kein Schlüssel: Alarmiert wird weiter über
 * Fähigkeiten und Schleifen. Zwei gleiche Kennungen auf dem Kanal wären nicht
 * auseinanderzuhalten — deshalb die Sperre bei einem schon vergebenen Namen.
 */
@Composable
private fun Rufnameblende(
    fahrzeug: Rundenfahrzeug,
    raum: Raumzustand,
    griffe: LeitstellenGriffe,
    beiSchliessen: () -> Unit,
) {
    var lang by remember(fahrzeug.id) { mutableStateOf(fahrzeug.funkrufname) }
    var kurz by remember(fahrzeug.id) { mutableStateOf(fahrzeug.kurzname) }
    val abgeleitet = lang.trim().split(" ").filter { it.isNotEmpty() }.lastOrNull().orEmpty()
    val gueltig = lang.isNotBlank()
    val belegt = lang.isNotBlank() && raum.vehicles.any {
        it.id != fahrzeug.id && it.funkrufname.equals(lang.trim(), ignoreCase = true)
    }

    Blende(
        titel = fahrzeug.typ,
        beiSchliessen = beiSchliessen,
        kopfknoepfe = { Knopf("Abbrechen", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
        fuss = {
            if (fahrzeug.rufnameVonHand) {
                Knopf(
                    "Wieder wie im Buch",
                    {
                        griffe.rufnameZuruecksetzen(fahrzeug.id)
                        beiSchliessen()
                    },
                    art = Knopfart.Leise,
                )
            }
            Knopf(
                "Übernehmen",
                {
                    griffe.rufname(fahrzeug.id, lang.trim(), kurz.trim().ifEmpty { null })
                    beiSchliessen()
                },
                art = Knopfart.Haupt,
                aktiv = gueltig && !belegt,
            )
        },
    ) {
        Etikett("Funkrufname")
        SehrLeise("bisher ${fahrzeug.funkrufname}", mono = true)
        Feld(
            wert = lang,
            beiAenderung = { lang = it.take(48) },
            etikett = "Rufname im Funk",
            platzhalter = "Florian Uelzen 10/83/01",
        )
        Feld(
            wert = kurz,
            beiAenderung = { kurz = it.take(16) },
            etikett = "Kurzform fürs Tableau",
            platzhalter = abgeleitet,
        )
        SehrLeise(
            "Leer lassen heißt „wie im Funk, nur das Kennzeichen“" +
                if (abgeleitet.isNotEmpty()) " — also $abgeleitet." else ".",
        )
        if (belegt) {
            Text(
                "${lang.trim()} fährt in dieser Runde schon. Zwei Fahrzeuge mit derselben Kennung wären " +
                    "auf dem Kanal nicht auseinanderzuhalten.",
                style = Schrift.Klein,
                color = Farben.AmberHell,
            )
        }
        SehrLeise(
            "Der Name ist eine Beschriftung, kein Schlüssel: Alarmiert wird weiter über Fähigkeiten und " +
                "Alarmschleifen. Was hier steht, bleibt auch dann stehen, wenn das Rufnamenformat der Runde " +
                "sich ändert.",
        )
    }
}
