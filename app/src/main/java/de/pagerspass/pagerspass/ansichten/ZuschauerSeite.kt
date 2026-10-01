package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Uebungsstand
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.karte.Lagekarte
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.seitengrund

/**
 * Der Zuschauerplatz — dieselben Bausteine wie die Leitstelle, nur ohne
 * Alarmierungsdialog, Sprechtaste und FMS-Tasten.
 *
 * Übertragen aus `ZuschauerView.vue`: Kopf mit Marke, Reiter Einsätze /
 * Fahrzeuge / Funk, die Karte nur auf dem Einsätze-Reiter, und die Regieleiste
 * nur für den Regieplatz — den Zuschauer, der die Übung angelegt hat.
 */
@Composable
fun ZuschauerSeite(
    modifier: Modifier = Modifier,
    stand: Rundenstand = Rundenstand(),
    eigeneKennung: String = "",
    beiVerlassen: () -> Unit = {},
    regie: RegieGriffe = RegieGriffe(),
    /** Ob das eigene Konto ein laufendes Abo hat — nur dann darf es um einen Platz bitten. */
    premium: Boolean = false,
    befehle: de.pagerspass.pagerspass.mobil.Raumbefehle = de.pagerspass.pagerspass.mobil.Raumbefehle.Leer,
) {
    var reiter by remember { mutableStateOf(0) }
    val raum = stand.raum
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val istRegie = raum?.zuschauer?.any { it.id == eigeneKennung && it.regie } == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .seitengrund(),
    ) {
        // Kopf — Verlassen, Marke, Leitstelle, Zahlen.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = oben + Abstand.Normal)
                .padding(horizontal = Abstand.Gross),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Marke(if (istRegie) "Regieplatz" else "Zuschauer", farbe = Farben.Amber)
                    Text(
                        text = raum?.settings?.leitstelle ?: stand.code,
                        style = Schrift.Klein,
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SehrLeise(
                    listOfNotNull(
                        "Raum ${stand.code}",
                        raum?.let { "${it.players.size} in der Runde" },
                        raum?.zuschauer?.size?.takeIf { it > 0 }?.let { "$it sehen zu" },
                    ).joinToString(" · "),
                )
            }
            Knopf("Verlassen", beiVerlassen, art = Knopfart.Leise, kompakt = true)
        }

        if (raum == null) {
            Ladezeile("Die Runde wird geladen …", Modifier.padding(Abstand.Gross))
            return@Column
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Gross),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Abstand.Gross),
        ) {
            // Der Platz in einer vollen Runde: Premium darf darum bitten, entscheiden
            // tut es die Leitstelle. Ein Knopf, der jedem angeboten wird und nur bei
            // manchen etwas tut, wäre ein Versprechen, das die Hälfte enttäuscht.
            val bitteLaeuft = raum.beitrittsanfragen.any { it.playerId == eigeneKennung }
            if (!istRegie && (bitteLaeuft || (raum.voll && premium))) {
                de.pagerspass.pagerspass.ui.bausteine.Kasten(abstandInnen = Abstand.Klein) {
                    Text("Die Runde ist voll.", style = Schrift.Normal, color = Farben.Text)
                    SehrLeise(
                        if (bitteLaeuft) "Die Leitstelle entscheidet, ob ein zusätzlicher Platz aufgemacht wird."
                        else "Als Premium-Mitglied kannst du um einen zusätzlichen Platz bitten.",
                    )
                    Row {
                        Knopf(
                            if (bitteLaeuft) "Bitte zurückziehen" else "Um einen Platz bitten",
                            {
                                if (bitteLaeuft) befehle.beitrittsanfrageZuruecknehmen()
                                else befehle.beitrittAnfragen()
                            },
                            art = if (bitteLaeuft) Knopfart.Leise else Knopfart.Haupt,
                            kompakt = true,
                        )
                    }
                }
            }

            Reiterreihe {
                listOf("Einsätze", "Fahrzeuge", "Funk").forEachIndexed { i, name ->
                    Reiter(
                        aufschrift = name,
                        offen = reiter == i,
                        beiDruck = { reiter = i },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Die Karte steht nur auf dem Einsätze-Reiter — am Handy ist für
            // Karte und Liste zugleich kein Platz (dieselbe Wahl wie im Web).
            if (reiter == 0) {
                Lagekarte(
                    raum = raum,
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                )
            }

            if (istRegie) {
                Regieleiste(raum = raum, uebung = raum.uebung, griffe = regie)
            }

            when (reiter) {
                0 -> {
                    val offene = raum.incidents.filter { !it.abgeschlossen }
                    if (offene.isEmpty()) {
                        Leerhinweis("Noch keine Einsätze.")
                    } else {
                        offene.forEach { Einsatzzeile(it) }
                    }
                }

                1 -> raum.vehicles.forEach { Dienstfahrzeugzeile(it) }

                2 -> Funkprotokoll(
                    zeilen = stand.funk,
                    eigenerRufname = null,
                    laeuft = stand.laeuft,
                    beiSenden = {},
                    nurLesen = true,
                )
            }
        }
    }
}

/** Die Griffe des Regieplatzes. */
class RegieGriffe(
    val start: () -> Unit = {},
    val jetzt: () -> Unit = {},
    val ueberspringen: () -> Unit = {},
    val achse: (Boolean) -> Unit = {},
    val lage: (String, String, String, String, String, Int) -> Unit = { _, _, _, _, _, _ -> },
    val stoerung: (String) -> Unit = {},
    val wetter: (String) -> Unit = {},
    val durchsage: (String) -> Unit = {},
    val beenden: () -> Unit = {},
)

private val STOERUNGEN = listOf(
    "BlinderAlarm" to "Blinder Alarm",
    "BoeswilligerNotruf" to "Böswilliger Notruf",
    "FehlalarmRueckruf" to "Fehlalarm-Rückruf",
    "Fahrzeugdefekt" to "Fahrzeugdefekt",
    "Funkloch" to "Funkloch",
    "Schaulustige" to "Schaulustige",
    "Drohne" to "Drohne",
    "Presseanruf" to "Presseanruf",
)

private val WETTER = listOf(
    "Klar" to "Klar",
    "Regen" to "Regen",
    "Glaette" to "Glätte",
    "Sturm" to "Sturm",
)

/**
 * Die Regieleiste — eine Spalte mit ganzen Zeilen, gedacht für ein zweites
 * Gerät neben der Gruppe. Kein Dialog: Ein Dialog, der die Lagekarte verdeckt,
 * nimmt dem Ausbilder genau das Bild, wegen dem er dasitzt.
 */
@Composable
private fun ColumnScope.Regieleiste(
    raum: Raumzustand,
    uebung: Uebungsstand?,
    griffe: RegieGriffe,
) {
    if (uebung == null) return
    var offenerBlock by remember { mutableStateOf<String?>(null) }

    Kasten {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Ueberschrift(uebung.name.ifBlank { "Übung" })
            SehrLeise("${uebung.stand}/${uebung.eintraege}")
            if (uebung.angehalten) Marke("angehalten", farbe = Farben.OrangeHell)
        }
        uebung.naechsterText?.let { naechster ->
            SehrLeise(
                uebung.naechsterNachSekunden?.let { s -> "$naechster · in ${s}s" } ?: naechster,
            )
        }

        if (raum.inLobby) {
            // In der Lobby genau ein Knopf — die Übungsleitung darf starten.
            Knopf("Dienst beginnen", griffe.start, art = Knopfart.Haupt)
            return@Kasten
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf("▶ Jetzt", griffe.jetzt, kompakt = true)
            Knopf("⏭ Überspringen", griffe.ueberspringen, kompakt = true)
            Knopf(
                if (uebung.angehalten) "▶ Achse weiter" else "⏸ Achse anhalten",
                { griffe.achse(!uebung.angehalten) },
                kompakt = true,
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            listOf("lage" to "+ Lage", "stoerung" to "+ Störung", "wetter" to "Wetter", "durchsage" to "Durchsage")
                .forEach { (id, name) ->
                    Pille(
                        aufschrift = name,
                        an = offenerBlock == id,
                        beiDruck = { offenerBlock = if (offenerBlock == id) null else id },
                    )
                }
        }

        when (offenerBlock) {
            "lage" -> RegieLageBlock { st, stText, meldebild, adresse, org, prio ->
                griffe.lage(st, stText, meldebild, adresse, org, prio)
                offenerBlock = null
            }

            "stoerung" -> FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                STOERUNGEN.forEach { (wert, name) ->
                    Pille(aufschrift = name, an = false, beiDruck = {
                        griffe.stoerung(wert)
                        offenerBlock = null
                    })
                }
            }

            "wetter" -> FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                WETTER.forEach { (wert, name) ->
                    Pille(aufschrift = name, an = false, beiDruck = {
                        griffe.wetter(wert)
                        offenerBlock = null
                    })
                }
            }

            "durchsage" -> {
                var text by remember { mutableStateOf("") }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Feld(
                        wert = text,
                        beiAenderung = { text = it.take(200) },
                        platzhalter = "Durchsage der Übungsleitung …",
                        modifier = Modifier.weight(1f),
                    )
                    Knopf("Senden", {
                        griffe.durchsage(text)
                        text = ""
                        offenerBlock = null
                    }, aktiv = text.isNotBlank(), kompakt = true)
                }
            }
        }

        Knopf("Übung beenden", griffe.beenden, art = Knopfart.Gefahr, kompakt = true)
    }
}

@Composable
private fun RegieLageBlock(
    beiEinwerfen: (String, String, String, String, String, Int) -> Unit,
) {
    var stichwort by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var meldebild by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var organisation by remember { mutableStateOf("Feuerwehr") }
    var prioritaet by remember { mutableStateOf(2) }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Feld(stichwort, { stichwort = it.take(12).uppercase() }, platzhalter = "Stichwort (B2 …)")
        Feld(text, { text = it.take(60) }, platzhalter = "Klartext")
        Feld(meldebild, { meldebild = it.take(200) }, platzhalter = "Meldebild")
        Feld(adresse, { adresse = it.take(120) }, platzhalter = "Adresse")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            listOf("Feuerwehr", "Rettungsdienst", "Thw", "Polizei").forEach { org ->
                Pille(aufschrift = org, an = organisation == org, beiDruck = { organisation = org })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            (1..3).forEach { p ->
                Pille(
                    aufschrift = "Dringlichkeit $p",
                    an = prioritaet == p,
                    beiDruck = { prioritaet = p },
                )
            }
        }
        Knopf(
            "Lage einwerfen",
            { beiEinwerfen(stichwort.trim(), text.trim(), meldebild.trim(), adresse.trim(), organisation, prioritaet) },
            aktiv = stichwort.isNotBlank() && adresse.isNotBlank(),
            kompakt = true,
        )
    }
}
