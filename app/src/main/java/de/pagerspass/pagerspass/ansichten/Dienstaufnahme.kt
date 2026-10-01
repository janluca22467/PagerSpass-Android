package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.pagerspass.pagerspass.mobil.Geraeteeinstellungen
import de.pagerspass.pagerspass.mobil.katalogstaatState
import de.pagerspass.pagerspass.mobil.nachBundesland
import de.pagerspass.pagerspass.netz.Katalogleitstelle
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Staaten
import de.pagerspass.pagerspass.netz.Staatsprofil
import de.pagerspass.pagerspass.netz.bundeslandname
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Karte
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * „Dienst aufnehmen" — die erste Startkarte, in der Fassung von `StartView.vue` (v6).
 *
 * <b>Der Staat zuerst.</b> Mit ihm schaltet die ganze Schicht um — Notrufnummern,
 * Fahrzeuge, Rufnamen, Stichworte —, deshalb steht er als eigener Schalter oben und
 * nicht als Gruppe in der Länderliste. Gemerkt wird er auf dem Gerät
 * (`Geraeteeinstellungen.katalogstaat`), dieselbe Stelle, die dem Autohaus sagt,
 * welche Fahrzeuge es zeigt. Liefert der Server nur deutsche Kreise (älterer Stand),
 * fällt der Schalter weg.
 *
 * <b>Vorgewählt wie im Web:</b> Niedersachsen und Celle, in Österreich
 * Innsbruck-Land, in der Schweiz Zürich. Der Knopf ist damit vom ersten Moment an
 * bedienbar — vorher stand er gesperrt, bis man zwei Listen durchgegangen war.
 *
 * <b>Felder nur, wo es eine Wahl gibt.</b> In der Schweiz ist der Kanton selbst der
 * Ausrückebereich; ein Kreisfeld mit genau einem Eintrag wäre nur im Weg. Die
 * Leitstelle erscheint nur, wo Feuerwehr und Rettung den Notruf getrennt annehmen
 * (Österreich, Schweiz). Der Haken „Ganzen Leitstellenbereich bespielen" nur, wo die
 * Leitstelle mehr als den gewählten Kreis disponiert.
 *
 * @param beiBesetzen Kreis, gewählte Leitstelle (oder `null` für die erste) und ob
 *   der ganze Bereich bespielt wird.
 */
@Composable
fun DienstaufnahmeKarte(
    landkreise: List<Landkreis>,
    leitstellen: List<Katalogleitstelle>,
    staaten: List<Staatsprofil>,
    laeuft: Boolean,
    beiBesetzen: (Landkreis?, String?, Boolean) -> Unit,
    vorlagen: (@Composable () -> Unit)?,
) {
    val zusammenhang = LocalContext.current
    val staat by katalogstaatState()

    // Gibt es überhaupt Kreise außerhalb Deutschlands? Sonst kein Schalter.
    val mehrereStaaten = remember(landkreise) { landkreise.any { Staaten.von(it.bundesland) != Staaten.DEUTSCHLAND } }
    val gilt = if (mehrereStaaten) staat else Staaten.DEUTSCHLAND
    val profil = staaten.firstOrNull { it.staat == gilt }
    val landwort = profil?.landwort ?: "Bundesland"
    val kreiswort = profil?.kreiswort ?: "Landkreis"

    val laender = remember(landkreise, gilt) {
        landkreise.map { it.bundesland }
            .filter { it.isNotBlank() && Staaten.von(it) == gilt }
            .distinct()
            .sortedBy { bundeslandname(it) }
    }

    var bundesland by rememberSaveable { mutableStateOf<String?>(null) }
    var landkreisId by rememberSaveable { mutableStateOf<String?>(null) }
    var leitstelleId by rememberSaveable { mutableStateOf<String?>(null) }
    var ganzerBereich by rememberSaveable { mutableStateOf(false) }
    var offeneWahl by remember { mutableStateOf<Dienstwahl?>(null) }

    // Die Vorgabe des Staats — beim ersten Laden und bei jedem Wechsel. Liegt sie
    // nicht vor, nimmt die Auswahl das erste Land und darin den ersten Kreis mit
    // echten Wachen.
    LaunchedEffect(gilt, laender) {
        if (laender.isEmpty()) return@LaunchedEffect
        if (bundesland != null && bundesland in laender && landkreise.any { it.id == landkreisId }) return@LaunchedEffect
        val (vorLand, vorKreis) = Staaten.vorgabe(gilt)
        val land = if (vorLand in laender) vorLand else laender.first()
        bundesland = land
        val (mitDaten, ohneDaten) = landkreise.nachBundesland(land)
        landkreisId = (mitDaten + ohneDaten).firstOrNull { it.id == vorKreis }?.id
            ?: (mitDaten + ohneDaten).firstOrNull()?.id
        leitstelleId = null
    }

    val (kreiseMitDaten, kreiseOhneDaten) = remember(landkreise, bundesland) {
        landkreise.nachBundesland(bundesland.orEmpty())
    }
    val kreiseImLand = kreiseMitDaten.size + kreiseOhneDaten.size
    val kreis = landkreise.firstOrNull { it.id == landkreisId }

    // Die Leitstellen des Kreises — die des eigenen Landes zuerst, wie im Server.
    val leitstellenImKreis = remember(leitstellen, landkreisId) {
        leitstellen.filter { landkreisId != null && landkreisId in it.kreise }
            .sortedBy { if (it.bundesland == kreis?.bundesland) 0 else 1 }
    }
    val leitstelle = leitstellenImKreis.firstOrNull { it.id == leitstelleId } ?: leitstellenImKreis.firstOrNull()
    val weitereKreise = leitstelle?.kreise.orEmpty()
        .filter { it != landkreisId }
        .mapNotNull { id -> landkreise.firstOrNull { it.id == id }?.name }
    val bereichWachen = when {
        kreis?.hatDaten != true -> 0
        !ganzerBereich -> kreis.wachen
        else -> leitstelle?.kreise.orEmpty()
            .mapNotNull { id -> landkreise.firstOrNull { it.id == id } }
            .filter { it.hatDaten }
            .sumOf { it.wachen }
    }

    Karte(
        titel = "Dienst aufnehmen",
        zeichen = Zeichen.Leitstelle,
        text = "Du übernimmst die Leitstelle und bekommst einen Raumcode zum Weitergeben.",
        haupt = true,
        knoepfe = {
            Knopf(
                aufschrift = "Leitstelle besetzen",
                beiDruck = {
                    // Nur dort mitgeschickt, wo es eine Wahl gab — sonst nimmt der Server die erste.
                    val gewaehlt = leitstelleId?.takeIf { leitstellenImKreis.size > 1 }
                    beiBesetzen(kreis, gewaehlt, ganzerBereich && weitereKreise.isNotEmpty())
                },
                art = Knopfart.Haupt,
                aktiv = !laeuft && kreis != null,
            )
            vorlagen?.invoke()
        },
    ) {
        if (landkreise.isEmpty()) {
            SehrLeise("Landkreise werden geladen …")
            return@Karte
        }

        if (mehrereStaaten) {
            Etikett("Land")
            Segment(
                seiten = Staaten.ALLE,
                gewaehlt = gilt,
                beiWahl = { Geraeteeinstellungen.katalogstaatSetzen(zusammenhang, it) },
                aufschrift = { Staaten.kurz(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Wahlfeld(
            etikett = landwort,
            wert = bundesland?.let { bundeslandname(it) },
            beiDruck = { offeneWahl = Dienstwahl.Land },
        )
        if (kreiseImLand > 1) {
            Wahlfeld(
                etikett = kreiswort,
                wert = kreis?.aufschrift,
                beiDruck = { offeneWahl = Dienstwahl.Kreis },
                aktiv = bundesland != null,
            )
        }
        if (leitstellenImKreis.size > 1) {
            Wahlfeld(
                etikett = "Leitstelle",
                wert = leitstelle?.name,
                beiDruck = { offeneWahl = Dienstwahl.Leitstelle },
            )
        }
        if (weitereKreise.isNotEmpty()) {
            Hakenzeile(
                text = "Ganzen Leitstellenbereich bespielen — auch ${weitereKreise.joinToString(", ")}. " +
                    "Mehr Wachen, längere Wege.",
                an = ganzerBereich,
                beiWechsel = { ganzerBereich = it },
            )
        }

        // Die Fußnoten: wer disponiert, was mitgespielt wird, wie groß es ist.
        leitstelle?.let { l ->
            SehrLeise(if (l.sitz.isNotBlank()) "${l.name} · Sitz ${l.sitz}" else l.name)
        }
        if (weitereKreise.isNotEmpty() && !ganzerBereich) {
            SehrLeise("Disponiert auch ${weitereKreise.joinToString(", ")} — hier nicht mitgespielt.")
        }
        if (kreis?.hatDaten == true) {
            SehrLeise("$bereichWachen echte Wachen · bis ${kreis.maxSpieler} Teilnehmer")
        } else if (kreis != null) {
            // Kein Fehler, sondern ein Stand — der Satz sagt, was das für die Schicht heißt.
            SehrLeise(
                "Für diesen Kreis liegen noch keine echten Wachen vor. Gespielt wird mit " +
                    "erfundenen Standorten: Die Karte zeigt den richtigen Kreis, Wachen, Straßen " +
                    "und Sonderobjekte darin sind aber gesetzt statt abgezogen.",
            )
        }
    }

    when (offeneWahl) {
        Dienstwahl.Land -> Wahlblende(
            titel = landwort,
            gruppen = listOf(null to laender),
            aufschrift = { bundeslandname(it) },
            gewaehlt = bundesland,
            beiWahl = { gewaehlt ->
                bundesland = gewaehlt
                // Ein Landeswechsel führt in einen anderen Kreis — der bisherige liegt
                // nicht mehr in der Liste. Der erste mit echten Wachen ist die Vorgabe.
                val (mit, ohne) = landkreise.nachBundesland(gewaehlt)
                landkreisId = (mit + ohne).firstOrNull()?.id
                leitstelleId = null
                offeneWahl = null
            },
            beiSchliessen = { offeneWahl = null },
            suchbar = laender.size > 8,
        )

        Dienstwahl.Kreis -> Wahlblende(
            titel = bundesland?.let { bundeslandname(it) } ?: kreiswort,
            gruppen = listOf("Mit echten Wachen" to kreiseMitDaten, "Noch mit erfundenen Standorten" to kreiseOhneDaten),
            aufschrift = { it.aufschrift },
            unterschrift = { k ->
                if (k.hatDaten) "${k.wachen} Wachen · bis ${k.maxSpieler} Spieler" else "erfundener Standardbereich"
            },
            gewaehlt = kreis,
            beiWahl = {
                landkreisId = it.id
                leitstelleId = null
                offeneWahl = null
            },
            beiSchliessen = { offeneWahl = null },
            suchbar = true,
        )

        Dienstwahl.Leitstelle -> Wahlblende(
            titel = "Leitstelle",
            gruppen = listOf(null to leitstellenImKreis),
            aufschrift = { it.name },
            unterschrift = { l -> l.sitz.takeIf { it.isNotBlank() }?.let { "Sitz $it" } },
            gewaehlt = leitstelle,
            beiWahl = {
                leitstelleId = it.id
                offeneWahl = null
            },
            beiSchliessen = { offeneWahl = null },
        )

        null -> Unit
    }
}

private enum class Dienstwahl { Land, Kreis, Leitstelle }
