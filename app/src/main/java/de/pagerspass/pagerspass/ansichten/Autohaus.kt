package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Das Autohaus — wo ein Fahrzeuggutschein eingelöst oder ein Fahrzeug gekauft
 * wird. Übertragen aus `components/garage/Autohaus.vue`.
 *
 * <b>Aufgebaut wie ein Verkaufsraum:</b> oben die Reihen, darunter das
 * Schaufenster mit dem Wagen, den man gerade ansieht. Das ist mehr Aufhebens als
 * eine Liste mit Häkchen — es passiert ein-, zweimal je Abend und gilt dauerhaft,
 * also darf man das Fahrzeug einmal ganz ansehen, bevor man es einstellt.
 *
 * <b>Zwei Wege, und der Gutschein zuerst.</b> Er kostet nichts; kaufen geht immer,
 * und der Gutschein bleibt dabei stehen. Ein Kauf will zweimal gedrückt sein.
 */
@Composable
fun Autohaus(
    auswahl: List<Fahrzeugvorlage>,
    offeneWahlen: Int,
    laeuft: Boolean,
    credits: Int,
    preise: Map<String, Int>,
    tagesangebot: String?,
    tagesangebotRegulaer: Int?,
    vorwahl: String?,
    beiUebernehmen: (Fahrzeugvorlage) -> Unit,
    beiKaufen: (Fahrzeugvorlage) -> Unit,
) {
    var gewaehlt by remember { mutableStateOf<String?>(null) }
    var bestaetige by remember { mutableStateOf<String?>(null) }
    var suche by remember { mutableStateOf("") }

    // Das Tagesangebot stellt seinen Wagen von außen ins Schaufenster.
    LaunchedEffect(vorwahl) {
        if (vorwahl != null && auswahl.any { it.id == vorwahl }) gewaehlt = vorwahl
    }

    // Fällt das gewählte Fahrzeug aus der Auslage (gerade übernommen), darf die
    // Markierung nicht auf einer Id stehen bleiben — die Bestätigung erst recht nicht.
    LaunchedEffect(auswahl) {
        if (gewaehlt != null && auswahl.none { it.id == gewaehlt }) gewaehlt = null
        if (bestaetige != null && auswahl.none { it.id == bestaetige }) bestaetige = null
    }

    val reihen = garageGruppieren(auswahl.filter { vorlagePasst(it, suche) })
    val erstes = reihen.firstOrNull()?.traeger?.firstOrNull()?.fahrzeuge?.firstOrNull()
    // Das Schaufenster liest aus der ganzen Auswahl: Ein Suchbegriff, der den Wagen
    // aus den Reihen nimmt, räumt ihn nicht aus dem Fenster.
    val imFenster = auswahl.firstOrNull { it.id == gewaehlt } ?: erstes

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Gross),
    ) {
        Etikett(if (offeneWahlen > 0) "Fahrzeuggutschein" else "Credits")
        Text(
            if (offeneWahlen > 0) {
                "$offeneWahlen Fahrzeuggutschein${if (offeneWahlen == 1) "" else "e"}"
            } else {
                "Fahrzeuge kaufen"
            },
            style = Schrift.Titel,
            color = Farben.Text,
        )
        SehrLeise(
            if (offeneWahlen > 0) {
                "Jeder Aufstieg bringt einen Gutschein — lös ihn gegen ein Fahrzeug deiner Wahl ein, " +
                    "aus welcher Organisation auch immer. Die Entscheidung gilt dauerhaft. Daneben trägt " +
                    "jedes Fahrzeug seinen Preis: Kaufen geht immer, der Gutschein bleibt dabei stehen."
            } else {
                "Den nächsten Fahrzeuggutschein bringt dein nächster Aufstieg — wer nicht warten will, " +
                    "kauft mit Credits. Du hast ${zahl(credits)} davon."
            },
        )

        if (auswahl.size > 8) {
            Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Typ, Fähigkeit, Organisation …")
        }

        // Die Reihen — eine eigene Rollstrecke, damit das Schaufenster darunter in
        // Reichweite bleibt.
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .background(Farben.BgTief, Rundung.Klein)
                .verticalScroll(rememberScrollState())
                .padding(Abstand.Klein),
        ) {
            if (reihen.isEmpty()) SehrLeise("Kein Fahrzeug passt zu „$suche“.")
            reihen.forEach { gruppe ->
                Text(
                    gruppe.schluessel,
                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                    color = Farben.TextLeise,
                    modifier = Modifier.padding(top = Abstand.Klein),
                )
                gruppe.traeger.forEach { cluster ->
                    if (gruppe.traeger.size > 1) Traegertitel(cluster.hiOrg, cluster.name)
                    cluster.fahrzeuge.forEach { f ->
                        val aktiv = imFenster?.id == f.id
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Rundung.Winzig)
                                .background(if (aktiv) Farben.FlaecheHoch else Color.Transparent)
                                .drawBehind {
                                    drawRect(orgTon(f.organisation), size = Size(3.dp.toPx(), size.height))
                                }
                                .clickable(
                                    onClick = {
                                        if (gewaehlt != f.id) bestaetige = null
                                        gewaehlt = f.id
                                    },
                                    role = Role.Button,
                                    indication = null,
                                    interactionSource = null,
                                )
                                .padding(start = Abstand.Normal, top = Abstand.Winzig, bottom = Abstand.Winzig),
                        ) {
                            Text(
                                f.typ,
                                style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                                color = if (aktiv) Farben.Amber else Farben.Text,
                            )
                            Text(f.kategorie, style = Schrift.Winzig, color = Farben.TextSehrLeise)
                        }
                    }
                }
            }
        }

        when {
            imFenster != null -> Schaufenster(
                f = imFenster,
                offeneWahlen = offeneWahlen,
                laeuft = laeuft,
                credits = credits,
                preis = preise[imFenster.id],
                istTagesangebot = imFenster.id == tagesangebot,
                regulaer = tagesangebotRegulaer,
                scharf = bestaetige == imFenster.id,
                beiUebernehmen = { beiUebernehmen(imFenster) },
                beiKaufen = {
                    val scharf = bestaetige == imFenster.id
                    bestaetige = if (scharf) null else imFenster.id
                    if (scharf) beiKaufen(imFenster)
                },
            )

            // Zwei verschiedene Leeren: alles gekauft — oder nur die Suche lässt nichts übrig.
            auswahl.isEmpty() -> SehrLeise("Im ganzen Katalog steht nichts mehr, was dir noch fehlt.", mono = true)

            else -> SehrLeise("Kein Treffer — der Katalog hat noch ${auswahl.size} Fahrzeuge für dich.", mono = true)
        }
    }
}

/** Das Schaufenster: ein Fahrzeug groß, mit allem, was es kann. */
@Composable
private fun Schaufenster(
    f: Fahrzeugvorlage,
    offeneWahlen: Int,
    laeuft: Boolean,
    credits: Int,
    preis: Int?,
    istTagesangebot: Boolean,
    regulaer: Int?,
    scharf: Boolean,
    beiUebernehmen: () -> Unit,
    beiKaufen: () -> Unit,
) {
    val farbe = orgTon(f.organisation)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 12.dp, randfarbe = farbe.copy(alpha = 0.6f))
            .padding(Abstand.Gross),
    ) {
        Etikett("${orgName(f.organisation)} · ${f.kategorie}")
        if (istTagesangebot) {
            Text("Tagesangebot · 25 % Rabatt", style = Schrift.MonoKlein, color = Farben.AmberHell)
        }
        Text(f.typ, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold, fontSize = Schrift.TITEL), color = Farben.Text)
        if (f.hiOrg.isNotBlank() && f.hiOrg != "Keine") Traegertitel(f.hiOrg, traegerLang(f.hiOrg))
        Text(f.beschreibung, style = Schrift.Klein, color = Farben.TextLeise)

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Gross)) {
            Column {
                Etikett("Besatzung")
                Text(f.besatzung.ifBlank { "—" }, style = Schrift.MonoKlein, color = Farben.Text)
            }
            Column(modifier = Modifier.weight(1f)) {
                Etikett("Schleifen")
                Text(
                    f.schleifen.joinToString(", ").ifBlank { "—" },
                    style = Schrift.MonoKlein,
                    color = Farben.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Etikett("Kann")
        if (f.faehigkeiten.isEmpty()) {
            SehrLeise("Keine besonderen Fähigkeiten.")
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            ) {
                f.faehigkeiten.forEach { Minimarke(it) }
            }
        }

        if (offeneWahlen > 0) {
            Knopf(
                aufschrift = if (laeuft) "Wird eingestellt …" else "Gutschein einlösen: ${f.typ}",
                beiDruck = beiUebernehmen,
                art = Knopfart.Haupt,
                aktiv = !laeuft,
                breit = true,
            )
        }

        // Der zweite Weg: kaufen statt wählen. Bei offener Wahl bewusst der leisere
        // Knopf — die freie Wahl zuerst, sie kostet nichts.
        if (preis != null) {
            val fehlt = preis - credits
            Knopf(
                aufschrift = kaufknopf(preis, credits, besitz = false, scharf = scharf),
                beiDruck = beiKaufen,
                art = if (offeneWahlen == 0 || scharf) Knopfart.Haupt else Knopfart.Normal,
                aktiv = !laeuft && fehlt <= 0,
                breit = true,
            )
        }
        if (istTagesangebot && regulaer != null) {
            Row {
                Text("Regulär ", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                Text(
                    regulaer.toString(),
                    style = Schrift.MonoKlein.copy(textDecoration = TextDecoration.LineThrough),
                    color = Farben.TextSehrLeise,
                )
                Text(" Credits", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
            }
        }
    }
}

/**
 * Die Aufschrift eines Kaufknopfs — dieselbe Form an jeder Kaufstelle
 * (`utils/kauftexte.ts`): Der Knopf sagt, warum er nicht geht, und fragt vor dem
 * endgültigen Kauf einmal nach.
 */
internal fun kaufknopf(preis: Int, guthaben: Int, besitz: Boolean, scharf: Boolean, waehrung: String = "Credits"): String {
    if (besitz) return "✓ Im Besitz"
    val fehlt = preis - guthaben
    if (fehlt > 0) return "Es fehlen $fehlt $waehrung"
    if (scharf) return "Wirklich für $preis $waehrung kaufen?"
    return "$preis $waehrung"
}
