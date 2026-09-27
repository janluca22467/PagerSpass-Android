package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Dienstbuchstand
import de.pagerspass.pagerspass.mobil.Dienstbuchstelle
import de.pagerspass.pagerspass.netz.Abzeichen
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlin.math.max

/**
 * Die Abzeichen — übertragen aus `views/dienstbuch/AbzeichenView.vue`.
 *
 * Knapp tausend Sammelkarten aus ein paar Familien — zu viele für eine Liste,
 * deshalb wie die Garage aufgebaut: Tore je Kategorie, eine Suche quer durch alle,
 * und obenan, was zuletzt dazukam.
 *
 * <b>Die Vitrine wird hier gewählt</b> und nicht im Profil — hier liegen die
 * Abzeichen samt Toren und Suche. Der Stern an einem erreichten Abzeichen ist
 * zugleich der Schalter; bei einem offenen bleibt er reine Anzeige.
 */

/**
 * Feste Reihenfolge der Kategorien: von den Meilensteinen über die Organisationen
 * bis zu den großen Sammelkategorien. Unbekannte landen hinten, statt zu
 * verschwinden.
 */
private val KATEGORIEN = listOf(
    "Meilensteine", "Feuerwehr", "Rettungsdienst", "THW", "Polizei",
    "Zeiten", "Einsatzarten", "Fuhrpark", "Landkreise",
)

private fun kategorieRang(kategorie: String): Int =
    KATEGORIEN.indexOf(kategorie).let { if (it < 0) KATEGORIEN.size else it }

/**
 * Wie viele Abzeichen eine Kategorie unter „Alle" zeigt, bevor „weitere" kommt.
 * Wer eine Kategorie wählt oder sucht, bekommt alles; nur der Überblick ist
 * gekürzt — tausend Kacheln auf einmal liest niemand am Stück.
 */
private const val JE_KATEGORIE = 24

@Composable
fun DienstbuchAbzeichen(
    unterrand: Dp,
    konto: Konto?,
    stand: Dienstbuchstand,
    beiReiter: (Buchreiter) -> Unit,
    beiLaden: () -> Unit,
    beiVitrineLaden: () -> Unit,
    beiVitrine: (Abzeichen) -> Unit,
    beiProfil: () -> Unit,
) {
    LaunchedEffect(Unit) {
        beiLaden()
        beiVitrineLaden()
    }
    val buch = stand.buch

    var tor by rememberSaveable { mutableStateOf("alle") }
    var suche by rememberSaveable { mutableStateOf("") }
    var aufgeklappt by rememberSaveable { mutableStateOf(emptyList<String>()) }

    Dienstbuchrahmen(
        unterrand = unterrand,
        konto = konto,
        hier = Buchreiter.Abzeichen,
        beiReiter = beiReiter,
        laedt = buch.ersteLadung,
        fehler = if (buch.inhalt == null) buch.fehler else null,
        beiErneut = beiLaden,
    ) {
        val abzeichen = buch.inhalt?.abzeichen.orEmpty()
        val erreicht = abzeichen.count { it.erreicht }
        val anteil = erreicht.toFloat() / max(1, abzeichen.size)

        // Der Gesamtstand und die Vitrine — beides beantwortet „was habe ich".
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Gross),
        ) {
            Etikett("Gesammelt")
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    zahl(erreicht),
                    style = Schrift.Anzeige,
                    color = Farben.AmberHell,
                )
                Text(" / ${zahl(abzeichen.size)}", style = Schrift.MonoNormal, color = Farben.TextLeise)
            }
            Fortschritt(anteil = anteil)

            Etikett("Vitrine ${stand.vitrine.size} / ${Dienstbuchstelle.VITRINE_HOECHSTENS}")
            SehrLeise(
                "Bis zu ${Dienstbuchstelle.VITRINE_HOECHSTENS} Abzeichen stehen in deiner Vitrine im " +
                    "Profil — tippe auf den Stern eines erreichten Abzeichens.",
            )
            Textweg("Vitrine im Profil", beiProfil)
            stand.vitrinenmeldung?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
        }

        val zuletzt = abzeichen
            .filter { it.erreicht && it.erreichtAm != null }
            .sortedByDescending { zeitVon(it.erreichtAm)?.toEpochMilli() ?: 0L }
            .take(8)
        if (zuletzt.isNotEmpty()) {
            Buchkarte(titel = "Zuletzt erreicht") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                ) {
                    zuletzt.forEach { a ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(1.dp, Farben.AmberTief, Rundung.Rund)
                                .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
                        ) {
                            Text("★", style = Schrift.Klein, color = Farben.Amber)
                            Text(a.titel, style = Schrift.Klein, color = Farben.Text, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Die Tore: eine Karte je Kategorie mit ihrem Stand als Balken.
        val kategorien = abzeichen.groupBy { it.kategorie }
            .map { (kategorie, liste) -> Triple(kategorie, liste.count { it.erreicht }, liste.size) }
            .sortedBy { kategorieRang(it.first) }

        if (kategorien.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                (listOf(Triple("alle", erreicht, abzeichen.size)) + kategorien).chunked(2).forEach { paar ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        paar.forEach { (kategorie, geschafft, gesamt) ->
                            Kategorietor(
                                name = if (kategorie == "alle") "Alle" else kategorie,
                                geschafft = geschafft,
                                gesamt = gesamt,
                                an = tor == kategorie,
                                beiDruck = { tor = kategorie },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (paar.size == 1) androidx.compose.foundation.layout.Box(Modifier.weight(1f))
                    }
                }
            }
        }

        Feld(wert = suche, beiAenderung = { suche = it }, platzhalter = "Abzeichen durchsuchen …")
        val sucht = suche.isNotBlank()
        if (sucht) SehrLeise("Die Suche geht durch alle Kategorien.")

        val begriff = suche.trim().lowercase()
        val sichtbar = abzeichen
            .filter { sucht || tor == "alle" || it.kategorie == tor }
            .filter {
                begriff.isEmpty() || it.titel.lowercase().contains(begriff) ||
                    it.beschreibung.lowercase().contains(begriff)
            }
        val gruppen = sichtbar.groupBy { it.kategorie }.entries.sortedBy { kategorieRang(it.key) }

        if (gruppen.isEmpty()) {
            Leerhinweis(if (sucht) "Nichts gefunden zu „$suche“." else "Hier steht noch nichts.")
        }

        gruppen.forEach { (kategorie, eintraege) ->
            val gekuerzt = tor == "alle" && !sucht && kategorie !in aufgeklappt
            // Erreichte zuerst: Wer die Sammlung überfliegt, will sehen, was er hat.
            val gezeigt = if (gekuerzt) {
                eintraege.sortedByDescending { it.erreicht }.take(JE_KATEGORIE)
            } else {
                eintraege
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth().padding(top = Abstand.Klein),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(kategorie, style = Schrift.Gross, color = Farben.Text, modifier = Modifier.weight(1f))
                    SehrLeise("${eintraege.count { it.erreicht }} / ${eintraege.size}", mono = true)
                }
                gezeigt.forEach { a ->
                    Abzeichenkachel(
                        abzeichen = a,
                        inVitrine = a.id in stand.vitrine,
                        beiStern = { beiVitrine(a) },
                    )
                }
                if (gezeigt.size < eintraege.size) {
                    Knopf(
                        aufschrift = "${eintraege.size - gezeigt.size} weitere zeigen",
                        beiDruck = { aufgeklappt = aufgeklappt + kategorie },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }
        }
    }
}

/** Ein Tor: Name, Stand als Zahl und Balken. */
@Composable
private fun Kategorietor(
    name: String,
    geschafft: Int,
    gesamt: Int,
    an: Boolean,
    beiDruck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = modifier
            .flaeche(
                ecke = 9.dp,
                farbe = if (an) Farben.FlaecheHoch else Farben.Flaeche,
                randfarbe = if (an) Farben.Amber else Farben.Rand,
            )
            .clickable(onClick = beiDruck, role = Role.Tab, indication = null, interactionSource = null)
            .padding(Abstand.Klein),
    ) {
        Text(name, style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text, maxLines = 1)
        Text("$geschafft / $gesamt", style = Schrift.MonoKlein, color = Farben.TextLeise)
        Fortschritt(anteil = geschafft.toFloat() / max(1, gesamt))
    }
}

/**
 * Eine Abzeichenkachel. Der Stern bei einem erreichten Abzeichen ist der
 * Schalter für die Vitrine — golden, wenn es darin steht.
 */
@Composable
private fun Abzeichenkachel(abzeichen: Abzeichen, inVitrine: Boolean, beiStern: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                ecke = 9.dp,
                randfarbe = when {
                    inVitrine -> Farben.Amber
                    abzeichen.erreicht -> Farben.AmberTief
                    else -> Farben.Rand
                },
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        if (abzeichen.erreicht) {
            Text(
                text = "★",
                style = Schrift.Titel,
                color = if (inVitrine) Farben.Amber else Farben.TextSehrLeise,
                modifier = Modifier
                    .widthIn(min = 32.dp)
                    .clickable(onClick = beiStern, role = Role.Switch, indication = null, interactionSource = null)
                    .semantics {
                        contentDescription = if (inVitrine) {
                            "${abzeichen.titel} aus der Vitrine nehmen"
                        } else {
                            "${abzeichen.titel} in die Vitrine stellen"
                        }
                    }
                    .padding(Abstand.Winzig),
            )
        } else {
            Text("☆", style = Schrift.Titel, color = Farben.Rand, modifier = Modifier.widthIn(min = 32.dp).padding(Abstand.Winzig))
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                abzeichen.titel,
                style = Schrift.Normal,
                color = if (abzeichen.erreicht) Farben.Text else Farben.TextLeise,
            )
            SehrLeise(abzeichen.beschreibung)
            if (abzeichen.erreicht && abzeichen.erreichtAm != null) {
                SehrLeise("erreicht am ${tag(abzeichen.erreichtAm)}", mono = true)
            }
        }
    }
}
