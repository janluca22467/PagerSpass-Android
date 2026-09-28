package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import de.pagerspass.pagerspass.mobil.Startstand
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Footerknopf
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Leitstelle
import de.pagerspass.pagerspass.netz.Rundenvorlagenzeile
import de.pagerspass.pagerspass.netz.Startkachel
import de.pagerspass.pagerspass.netz.Vorlagenbesatzung
import de.pagerspass.pagerspass.netz.Vorlageninhalt
import de.pagerspass.pagerspass.netz.Wache
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Karte
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.zeichen.Zeichen
import kotlinx.coroutines.launch

/**
 * Was der Startbildschirm für die Runde über das Bisherige hinaus zeigt —
 * übertragen aus `StartView.vue`, `start/RundenvorlagenDialog.vue`,
 * `start/VorlagenEditor.vue`, `start/UmfrageKarte.vue`, `start/Startkacheln.vue`.
 *
 * <b>In einer eigenen Datei</b>, damit die Startseite selbst nur an wenigen,
 * markierten Stellen wächst — sie ist eine Datei, an der mehrere Bereiche
 * arbeiten.
 */
class Startzusatz(
    val start: Startstand = Startstand(),
    /** `Katalog.leitstellen` — wer welchen Kreis disponiert. */
    val leitstellen: List<Leitstelle> = emptyList(),
    /** Die Fahrzeugtypen des Katalogs — für den Vorlageneditor. */
    val fahrzeuge: List<Fahrzeugvorlage> = emptyList(),
    /** Die eigene Wache — für die Clanrunde. */
    val clan: Gemeinschaft? = null,
    /** Der Kreis der heutigen Tagesschicht, lesbar — `null`, solange unbekannt. */
    val tagesschichtKreis: String? = null,
    val griffe: Startgriffe = Startgriffe(),
)

/** Die Griffe dazu — alles, was nach draußen geht. */
class Startgriffe(
    /** Leitstelle besetzen mit Kreis, Leitstelle und „ganzer Bereich" (Lücke A8). */
    val besetzen: ((Landkreis, String?, Boolean) -> Unit)? = null,
    val vorlagenLaden: () -> Unit = {},
    val vorlageStarten: (String) -> Unit = {},
    val vorlageLoeschen: (String) -> Unit = {},
    val vorlageEinloesen: (String) -> Unit = {},
    val vorlagenMeldungWeg: () -> Unit = {},
    val vorlageninhalt: suspend (String) -> Vorlageninhalt = { Vorlageninhalt() },
    val kreiswachen: suspend (String) -> List<Wache> = { emptyList() },
    val vorlageninhaltSichern: suspend (String, String, Int?, List<Vorlagenbesatzung>) -> Unit =
        { _, _, _, _ -> },
    val umfrageAntworten: (Int) -> Unit = {},
    val clanrundeBeitreten: (String) -> Unit = {},
    val clanrundeSchliessen: suspend (String) -> Unit = {},
    val ausbildungHinweisWeg: () -> Unit = {},
    val kachel: (Startkachel) -> Unit = {},
    val footer: (Footerknopf) -> Unit = {},
    val vertragKuendigen: () -> Unit = {},
    val vertragWiderrufen: () -> Unit = {},
)

// ------------------------------------------------------------- Clanrunde

/** „Auf der Wache läuft eine Runde" — mit dem Weg hinein. */
@Composable
internal fun Clanrundenkarte(zusatz: Startzusatz, laeuft: Boolean) {
    val clan = zusatz.clan ?: return
    val code = clan.laufendeRundeCode ?: return
    val lauf = rememberCoroutineScope()
    var weg by remember(code) { mutableStateOf(false) }
    if (weg) return
    Karte(
        titel = "Clanrunde",
        zeichen = Zeichen.Gemeinschaft,
        text = "Auf ${clan.name} läuft eine Runde.",
        haupt = true,
        knoepfe = {
            Knopf(
                "Dazustoßen",
                { zusatz.griffe.clanrundeBeitreten(code) },
                art = Knopfart.Haupt,
                aktiv = !laeuft,
                kompakt = true,
            )
            if (clan.darfRundeSchliessen) {
                Knopf(
                    "Nicht mehr anzeigen",
                    {
                        weg = true
                        lauf.launch { runCatching { zusatz.griffe.clanrundeSchliessen(clan.id) } }
                    },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        },
    )
}

// --------------------------------------------------------------- Umfrage

@Composable
internal fun Umfragekarte(start: Startstand, beiAntwort: (Int) -> Unit) {
    val umfrage = start.umfrage ?: return
    Karte(titel = "Kurze Frage", zeichen = Zeichen.Forum, text = umfrage.frage) {
        if (start.umfrageDank) {
            Text("Danke für deine Antwort!", style = Schrift.Klein, color = Farben.GruenHell)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                umfrage.optionen.forEachIndexed { i, option ->
                    Pille(aufschrift = option, an = false, aktiv = !start.umfrageLaeuft, beiDruck = { beiAntwort(i) })
                }
            }
        }
    }
}

// ------------------------------------------------------ Ausbildungshinweis

/** Für den, der noch nie gefahren ist — und wegtippbar, einmal für immer. */
@Composable
internal fun Ausbildungshinweis(beiAusbildung: () -> Unit, beiWeg: () -> Unit, laeuft: Boolean) {
    Karte(
        titel = "Zum ersten Mal in einer Leitstelle?",
        zeichen = Zeichen.Lehrgang,
        text = "Die Ausbildungsschicht zeigt Melder, FMS, Notruf und Funk in Ruhe — allein, " +
            "ohne Wertung, in zehn Minuten.",
        knoepfe = {
            Knopf("Ausbildung starten", beiAusbildung, art = Knopfart.Haupt, aktiv = !laeuft, kompakt = true)
            Knopf("Nicht mehr anzeigen", beiWeg, art = Knopfart.Leise, kompakt = true)
        },
    )
}

// ------------------------------------------------------ Ganzer Bereich

/** Die Leitstelle, die diesen Kreis disponiert — die erste, die ihn führt. */
internal fun leitstelleFuer(leitstellen: List<Leitstelle>, landkreisId: String?): Leitstelle? =
    landkreisId?.let { id -> leitstellen.firstOrNull { id in it.kreise } }

/**
 * Der Haken „ganzen Leitstellenbereich bespielen" und die Zeilen dazu —
 * nur, wenn die Leitstelle mehr als diesen einen Kreis führt.
 */
@Composable
internal fun ColumnScope.Bereichswahl(
    landkreis: Landkreis,
    leitstelle: Leitstelle?,
    landkreise: List<Landkreis>,
    ganzerBereich: Boolean,
    beiWechsel: (Boolean) -> Unit,
) {
    if (leitstelle == null) return
    val andere = leitstelle.kreise.filter { it != landkreis.id }
        .mapNotNull { id -> landkreise.firstOrNull { it.id == id } }
    SehrLeise(
        listOfNotNull(leitstelle.name, leitstelle.sitz.takeIf { it.isNotBlank() }?.let { "Sitz $it" })
            .joinToString(" · "),
        mono = true,
    )
    if (andere.isEmpty()) return
    val namen = andere.joinToString(", ") { it.name }
    Hakenzeile(
        text = "Ganzen Leitstellenbereich bespielen — auch $namen. Mehr Wachen, längere Wege.",
        an = ganzerBereich,
        beiWechsel = beiWechsel,
    )
    if (ganzerBereich) {
        val bereich = listOf(landkreis) + andere
        SehrLeise(
            "${bereich.sumOf { it.wachen }} echte Wachen · bis ${bereich.sumOf { it.maxSpieler }} Teilnehmer",
            mono = true,
        )
    } else {
        SehrLeise("Disponiert auch $namen — hier nicht mitgespielt.")
    }
}

// ------------------------------------------------------ Kacheln und Fuß

/** Die Startkacheln des Betriebs — was dort steht, bestimmt die Verwaltung. */
@Composable
internal fun Startkacheln(kacheln: List<Startkachel>, premium: Boolean, beiKachel: (Startkachel) -> Unit) {
    kacheln.forEach { k ->
        Wegzeile(
            titel = k.titel,
            unterzeile = k.beschreibung,
            zeichen = Zeichen.Weiter,
            schild = k.marke ?: if (k.premiumNoetig && !premium) "Premium" else null,
            beiDruck = { beiKachel(k) },
        )
    }
}

/** Die Fußknöpfe des Betriebs und die zwei Vertragswege. */
@Composable
internal fun Startfussknoepfe(
    footer: List<Footerknopf>,
    beiKnopf: (Footerknopf) -> Unit,
    beiKuendigen: () -> Unit,
    beiWiderrufen: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth(),
    ) {
        footer.filter { it.geraete == "beide" || it.geraete == "mobil" }.forEach { f ->
            Textweg(f.beschriftung, { beiKnopf(f) })
        }
        Textweg("Verträge hier kündigen", beiKuendigen)
        Textweg("Vertrag widerrufen", beiWiderrufen)
    }
}

// ------------------------------------------------------ Rundenvorlagen

/**
 * Die eigenen Rundenvorlagen — starten, bearbeiten, löschen, per Code
 * übernehmen. `RundenvorlagenDialog.vue`.
 */
@Composable
internal fun Rundenvorlagenblende(
    zusatz: Startzusatz,
    landkreise: List<Landkreis>,
    laeuft: Boolean,
    beiSchliessen: () -> Unit,
) {
    val start = zusatz.start
    val g = zusatz.griffe
    var code by remember { mutableStateOf("") }
    var loeschen by remember { mutableStateOf<Rundenvorlagenzeile?>(null) }
    var bearbeiten by remember { mutableStateOf<Rundenvorlagenzeile?>(null) }
    LaunchedEffect(Unit) { g.vorlagenLaden() }

    Blende(
        titel = "Rundenvorlagen",
        beiSchliessen = {
            g.vorlagenMeldungWeg()
            beiSchliessen()
        },
        fuss = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise) },
    ) {
        SehrLeise(
            "Eine Vorlage hält Kreis, Einstellungen und Aufstellung einer Lobby fest. Gespeichert " +
                "wird in der Lobby unter „Runde“.",
        )
        if (start.vorlagen.isEmpty()) {
            Leerhinweis("Noch keine Vorlagen gespeichert.")
        }
        start.vorlagen.forEach { v ->
            val kreis = landkreise.firstOrNull { it.id == v.landkreisId }?.name
            Karte(
                titel = v.name,
                text = listOfNotNull(
                    kreis,
                    "ganzer Bereich".takeIf { v.ganzerBereich },
                    "Code ${v.code}".takeIf { v.code.isNotBlank() },
                ).joinToString(" · "),
                knoepfe = {
                    Knopf("Starten", { g.vorlageStarten(v.id) }, art = Knopfart.Haupt, aktiv = !laeuft, kompakt = true)
                    Knopf("Bearbeiten", { bearbeiten = v }, kompakt = true)
                    Knopf(
                        "Löschen",
                        { loeschen = v },
                        art = Knopfart.Gefahr,
                        aktiv = start.vorlageLoescht != v.id,
                        kompakt = true,
                    )
                },
            )
        }
        Etikett("Vorlage per Code übernehmen")
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
        ) {
            Feld(
                wert = code,
                beiAenderung = { code = it.uppercase().take(12) },
                platzhalter = "Code",
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "Übernehmen",
                {
                    g.vorlageEinloesen(code.trim())
                    code = ""
                },
                aktiv = code.isNotBlank(),
                kompakt = true,
            )
        }
        start.vorlagenMeldung?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }
    }

    loeschen?.let { v ->
        Blende(
            titel = "Vorlage löschen?",
            beiSchliessen = { loeschen = null },
            breite = Dialogbreite.Schmal,
            fuss = {
                Knopf("Behalten", { loeschen = null }, art = Knopfart.Leise)
                Knopf(
                    "Löschen",
                    {
                        g.vorlageLoeschen(v.id)
                        loeschen = null
                    },
                    art = Knopfart.Gefahr,
                )
            },
        ) {
            Text("„${v.name}“ wird gelöscht. Wer den Code schon hat, behält seine Kopie.", style = Schrift.Normal, color = Farben.TextLeise)
        }
    }

    bearbeiten?.let { v ->
        Vorlageneditor(
            zeile = v,
            zusatz = zusatz,
            beiSchliessen = { bearbeiten = null },
        )
    }
}

/**
 * Den Inhalt einer Vorlage bearbeiten — Name, Plätze und je Fahrzeug Rufname,
 * Kurzform und Wache. `VorlagenEditor.vue`.
 */
@Composable
private fun Vorlageneditor(
    zeile: Rundenvorlagenzeile,
    zusatz: Startzusatz,
    beiSchliessen: () -> Unit,
) {
    val g = zusatz.griffe
    val lauf = rememberCoroutineScope()
    var inhalt by remember(zeile.id) { mutableStateOf<Vorlageninhalt?>(null) }
    var wachen by remember(zeile.id) { mutableStateOf<List<Wache>>(emptyList()) }
    var fehler by remember(zeile.id) { mutableStateOf<String?>(null) }
    var name by remember(zeile.id) { mutableStateOf(zeile.name) }
    var plaetze by remember(zeile.id) { mutableStateOf("") }
    var bestand by remember(zeile.id) { mutableStateOf<List<Vorlagenbesatzung>>(emptyList()) }
    var speichert by remember { mutableStateOf(false) }
    var wacheFuer by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(zeile.id) {
        runCatching { g.vorlageninhalt(zeile.id) }
            .onSuccess { i ->
                inhalt = i
                name = i.name.ifBlank { zeile.name }
                plaetze = i.maxSpieler?.toString().orEmpty()
                bestand = i.bestand
                i.landkreisId?.let { kreis -> wachen = runCatching { g.kreiswachen(kreis) }.getOrDefault(emptyList()) }
            }
            .onFailure { fehler = it.message ?: "Die Vorlage ließ sich nicht laden." }
    }

    Blende(
        titel = "Vorlage bearbeiten",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Breit,
        fuss = {
            Knopf("Abbrechen", beiSchliessen, art = Knopfart.Leise)
            Knopf(
                if (speichert) "Wird gespeichert…" else "Speichern",
                {
                    speichert = true
                    fehler = null
                    lauf.launch {
                        runCatching {
                            g.vorlageninhaltSichern(zeile.id, name.trim(), plaetze.toIntOrNull(), bestand)
                        }
                            .onSuccess { beiSchliessen() }
                            .onFailure { fehler = it.message ?: "Speichern fehlgeschlagen." }
                        speichert = false
                    }
                },
                art = Knopfart.Haupt,
                aktiv = inhalt != null && name.isNotBlank() && !speichert,
            )
        },
    ) {
        if (inhalt == null && fehler == null) {
            Ladezeile("Die Vorlage wird geladen …")
        }
        if (inhalt != null) {
            Feld(wert = name, beiAenderung = { name = it.take(60) }, etikett = "Name")
            Feld(
                wert = plaetze,
                beiAenderung = { plaetze = it.filter(Char::isDigit).take(3) },
                etikett = "Plätze",
                platzhalter = "wie der Kreis",
                tastatur = KeyboardType.Number,
            )
            Etikett("Aufstellung (${bestand.size})")
            if (bestand.isEmpty()) SehrLeise("Keine Fahrzeuge in dieser Vorlage.")
            bestand.forEachIndexed { i, b ->
                val typ = zusatz.fahrzeuge.firstOrNull { it.id == b.vorlageId }
                val wache = wachen.firstOrNull { it.kennung == b.wacheKennung }
                Karte(titel = typ?.typ ?: b.vorlageId) {
                    Feld(
                        wert = b.funkrufname.orEmpty(),
                        beiAenderung = { neu ->
                            bestand = bestand.mapIndexed { j, x -> if (j == i) x.copy(funkrufname = neu.take(48).ifBlank { null }) else x }
                        },
                        etikett = "Rufname",
                        platzhalter = "wie im Buch",
                    )
                    Feld(
                        wert = b.kurzname.orEmpty(),
                        beiAenderung = { neu ->
                            bestand = bestand.mapIndexed { j, x -> if (j == i) x.copy(kurzname = neu.take(16).ifBlank { null }) else x }
                        },
                        etikett = "Kurzform",
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            wache?.name ?: "Wache: automatisch",
                            style = Schrift.Klein,
                            color = Farben.TextLeise,
                            modifier = Modifier.weight(1f),
                        )
                        if (wachen.isNotEmpty()) Knopf("Wache", { wacheFuer = i }, kompakt = true)
                    }
                }
            }
        }
        fehler?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
    }

    wacheFuer?.let { i ->
        val b = bestand.getOrNull(i)
        val org = zusatz.fahrzeuge.firstOrNull { it.id == b?.vorlageId }?.organisation
        val passend = wachen.filter { org == null || it.organisation == org }
        Wahlblende(
            titel = "Wache",
            gruppen = listOf<Pair<String?, List<Wache?>>>(null to (listOf<Wache?>(null) + passend)),
            aufschrift = { it?.name ?: "automatisch" },
            gewaehlt = wachen.firstOrNull { it.kennung == b?.wacheKennung },
            beiWahl = { w ->
                bestand = bestand.mapIndexed { j, x -> if (j == i) x.copy(wacheKennung = w?.kennung) else x }
                wacheFuer = null
            },
            beiSchliessen = { wacheFuer = null },
            suchbar = passend.size > 8,
        )
    }
}

/** Die Marke der Vorlagenzahl am Knopf. */
@Composable
internal fun Vorlagenmarke(anzahl: Int) {
    if (anzahl > 0) Marke("$anzahl", farbe = Farben.AmberHell)
}
