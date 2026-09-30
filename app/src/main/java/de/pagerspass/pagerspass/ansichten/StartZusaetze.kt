package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Footerknopf
import de.pagerspass.pagerspass.netz.OffeneUmfrage
import de.pagerspass.pagerspass.netz.Startkachel
import de.pagerspass.pagerspass.ui.bausteine.Karte
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Was der Startbildschirm außer den festen Wegen trägt — aus `StartView.vue`:
 * die offene Umfrage, die frei konfigurierbaren Kacheln der Verwaltung und die
 * Knöpfe der Fußzeile.
 *
 * <b>Alles davon ist Beiwerk.</b> Scheitert ein Abruf, fehlt die Karte — der
 * Startbildschirm ist dann, was er ohne sie war. Eine Fehlermeldung hätte dort
 * keinen Adressaten.
 */

/**
 * Die offene Umfrage. Eine Frage, ein paar Antworten, ein Knopf — und nach
 * der Antwort ein Dank statt der Karte. Die Wahl geht erst mit dem Knopf ab:
 * Ein Tippen auf die falsche Option soll folgenlos sein.
 */
@Composable
fun Umfragekarte(umfrage: OffeneUmfrage?, dank: Boolean, beiAntworten: (Int) -> Unit) {
    if (umfrage == null) return
    var wahl by remember(umfrage.id) { mutableStateOf<Int?>(null) }
    var unterwegs by remember(umfrage.id) { mutableStateOf(false) }

    Karte(
        titel = if (dank) "Danke!" else "Umfrage",
        zeichen = Zeichen.Forum,
        text = if (dank) "Deine Antwort ist angekommen." else umfrage.frage,
        haupt = !dank,
        knoepfe = if (dank) null else {
            {
                Knopf(
                    "Antwort senden",
                    {
                        val w = wahl ?: return@Knopf
                        unterwegs = true
                        beiAntworten(w)
                    },
                    art = Knopfart.Haupt,
                    aktiv = wahl != null && !unterwegs,
                    kompakt = true,
                )
            }
        },
    ) {
        if (!dank) {
            umfrage.optionen.forEachIndexed { i, option ->
                val an = wahl == i
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .flaeche(
                            farbe = if (an) Farben.HauchAmber else Farben.FlaecheHoch,
                            randfarbe = if (an) Farben.Amber else Farben.Rand,
                            ecke = 9.dp,
                        )
                        .clickable(role = Role.RadioButton) { wahl = i }
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Text(if (an) "◉" else "○", style = Schrift.Normal, color = if (an) Farben.Amber else Farben.TextSehrLeise)
                    Text(option, style = Schrift.Normal, color = Farben.Text)
                }
            }
            SehrLeise("Anonym — gespeichert wird nur, welche Antwort gewählt wurde, nicht von wem.")
        }
    }
}

/**
 * Die Kacheln der Verwaltung — als Letztes im Menü, weil die festen Wege das
 * Spiel sind und das hier ist, was gerade gilt.
 *
 * Rechts steht genau eine Sache: Fehlt Premium, „Premium" — das ist dann die
 * Auskunft, auf die es ankommt. Sonst die eingetragene Marke, sonst nichts.
 */
@Composable
fun Startkacheln(
    kacheln: List<Startkachel>,
    premium: Boolean,
    laeuft: Boolean,
    beiKachel: (Startkachel) -> Unit,
) {
    kacheln.forEach { k ->
        Wegzeile(
            titel = (if (k.premiumNoetig) "★ " else "") + k.titel,
            unterzeile = k.beschreibung,
            zeichen = pfadzeichen(k.pfad, gefuellt = false) ?: Zeichen.Weiter,
            schild = when {
                k.premiumNoetig && !premium -> "Premium"
                else -> k.marke
            },
            aktiv = !(laeuft && k.aktion == "raum"),
            beiDruck = { beiKachel(k) },
        )
    }
}

/**
 * Die Knöpfe der Fußzeile — Discord, Instagram, was die Verwaltung einträgt.
 *
 * Gefiltert wird hier, nicht am Server: Der Server kennt das Gerät nicht, und
 * ein Knopf kann auf „mobil" oder „desktop" beschränkt sein.
 */
@Composable
fun Fussknoepfe(knoepfe: List<Footerknopf>, beiLink: (String) -> Unit) {
    val sichtbar = knoepfe.filter { it.geraete == "beide" || it.geraete == "mobil" }
    if (sichtbar.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        sichtbar.forEach { k ->
            val zeichen = pfadzeichen(k.pfad, gefuellt = true) ?: return@forEach
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Ziel.Normal)
                    .flaeche(ecke = 999.dp, farbe = Farben.Flaeche)
                    .clickable(role = Role.Button) { beiLink(k.ziel) }
                    .semantics { contentDescription = k.beschriftung },
            ) {
                Icon(zeichen, contentDescription = null, tint = Farben.TextLeise, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** „Vorlagen (3)" — der Knopf neben „Leitstelle besetzen". */
@Composable
fun Vorlagenknopf(anzahl: Int, beiDruck: () -> Unit) {
    Knopf(
        if (anzahl > 0) "Vorlagen ($anzahl)" else "Vorlagen",
        beiDruck,
        art = Knopfart.Leise,
    )
}

