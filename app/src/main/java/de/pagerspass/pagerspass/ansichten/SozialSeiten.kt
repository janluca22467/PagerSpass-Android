package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Sozialstand
import de.pagerspass.pagerspass.netz.BrettSeite
import de.pagerspass.pagerspass.netz.Bretteintrag
import de.pagerspass.pagerspass.netz.Brettkommentar
import de.pagerspass.pagerspass.netz.Nachricht
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Brett und Gespräch — übertragen aus dem Freundebereich des Web.
 *
 * <b>Das Brett ist die Bühne, die Quittung die einzige Reaktion.</b> Getippte
 * Beiträge und automatische Erfolge stehen in einem Strom; geblättert wird
 * über die Nummer der letzten Zeile, nicht über einen Versatz.
 */

/** Der Brett-Strom — als Teil der Freundeseite. */
@Composable
fun ColumnScope.BrettTeil(
    brett: Bereichsstand<BrettSeite>,
    reiter: String,
    hatWache: Boolean,
    darfOeffentlich: Boolean,
    beiReiter: (String) -> Unit,
    beiLaden: () -> Unit,
    beiMehr: () -> Unit,
    beiSchreiben: (String, String?) -> Unit,
    beiQuittieren: (Long) -> Unit,
    beiOeffnen: (Long) -> Unit,
) {
    LaunchedEffect(Unit) { beiLaden() }
    var text by remember { mutableStateOf("") }

    // Verfassen — oben, wie im Web. Die Sichtbarkeit folgt dem Kreis-Sieb.
    Kasten(abstandInnen = Abstand.Klein) {
        Feld(
            wert = text,
            beiAenderung = { text = it.take(500) },
            platzhalter = "Was gibt's von dir?",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SehrLeise("${text.length}/500", modifier = Modifier.weight(1f))
            Knopf(
                "Anschlagen",
                {
                    beiSchreiben(
                        text.trim(),
                        when (reiter) {
                            "Wache" -> "Wache"
                            "Alle" -> if (darfOeffentlich) "Oeffentlich" else null
                            else -> "Freunde"
                        },
                    )
                    text = ""
                },
                aktiv = text.isNotBlank(),
                kompakt = true,
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Pille("Freunde", an = reiter == "Freunde", beiDruck = { beiReiter("Freunde") })
        if (hatWache) {
            Pille("Wache", an = reiter == "Wache", beiDruck = { beiReiter("Wache") })
        }
        Pille("Alle", an = reiter == "Alle", beiDruck = { beiReiter("Alle") })
    }

    Bereich(
        laedt = brett.laedt,
        fehler = brett.fehler,
        inhalt = brett.inhalt,
        beiErneut = beiLaden,
    ) { seite ->
        if (seite.eintraege.isEmpty()) {
            Leerhinweis("Noch nichts am Brett. Der erste Anschlag gehört dir.")
        } else {
            seite.eintraege.forEach { eintrag ->
                EintragKarte(
                    eintrag = eintrag,
                    beiQuittieren = { beiQuittieren(eintrag.nr) },
                    beiOeffnen = { beiOeffnen(eintrag.nr) },
                )
            }
            if (seite.weiter != null) {
                Knopf("Mehr laden", beiMehr, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
}

/**
 * Eine Karte am Brett — Beitrag oder Erfolg.
 *
 * Erfolge tragen einen Titel und keinen Text; die Quittung ist ein Umschalter
 * und springt sofort, geprüft wird hinterher.
 */
@Composable
private fun EintragKarte(
    eintrag: Bretteintrag,
    beiQuittieren: () -> Unit,
    beiOeffnen: () -> Unit,
) {
    Kasten(abstandInnen = Abstand.Klein) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = eintrag.verfasser.anzeigename
                    .ifBlank { eintrag.verfasser.benutzername },
                style = Schrift.Klein,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SehrLeise(seither(eintrag.erstelltUm) ?: "", mono = true)
        }

        eintrag.titel?.let {
            Text(text = it, style = Schrift.Normal, color = Farben.AmberHell)
        }
        eintrag.text?.let {
            Text(text = it, style = Schrift.Klein, color = Farben.Text)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Gross),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Textweg(
                (if (eintrag.vonMirQuittiert) "✓ " else "○ ") +
                    "${eintrag.quittungen} Quittungen",
                beiQuittieren,
            )
            Textweg("${eintrag.kommentare} Kommentare", beiOeffnen)
        }
    }
}

/** Ein Eintrag mit seinen Kommentaren — die eigene Adresse des Push-Ziels. */
@Composable
fun EintragSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    nr: Long = 0,
    eintrag: Bereichsstand<Bretteintrag?> = Bereichsstand(),
    kommentare: List<Brettkommentar> = emptyList(),
    beiLaden: (Long) -> Unit = {},
    beiQuittieren: (Long) -> Unit = {},
    beiKommentieren: (Long, String) -> Unit = { _, _ -> },
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(nr) { beiLaden(nr) }
    var text by remember { mutableStateOf("") }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Eintrag",
            unterzeile = "Vom Brett",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = eintrag.laedt,
            fehler = eintrag.fehler,
            inhalt = eintrag.inhalt,
            beiErneut = { beiLaden(nr) },
        ) { e ->
            EintragKarte(
                eintrag = e,
                beiQuittieren = { beiQuittieren(e.nr) },
                beiOeffnen = {},
            )

            kommentare.forEach { kommentar ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .flaeche(ecke = 9.dp)
                        .padding(Abstand.Normal),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = kommentar.verfasser.anzeigename
                                    .ifBlank { kommentar.verfasser.benutzername },
                                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                                color = if (kommentar.vonMir) Farben.Amber
                                else Farben.TextSehrLeise,
                                modifier = Modifier.weight(1f),
                            )
                            SehrLeise(seither(kommentar.erstelltUm) ?: "", mono = true)
                        }
                        Text(text = kommentar.text, style = Schrift.Klein, color = Farben.Text)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Feld(
                    wert = text,
                    beiAenderung = { text = it.take(300) },
                    platzhalter = "Kommentieren …",
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Senden",
                    {
                        beiKommentieren(nr, text.trim())
                        text = ""
                    },
                    aktiv = text.isNotBlank(),
                    kompakt = true,
                )
            }
        }
    }
}

/**
 * Das Gespräch — der Chatverlauf mit einem Freund, als eigene Seite.
 *
 * <b>Ankommen gilt als Lesen</b>, solange die Seite offen ist — deshalb muss
 * sie sich beim Verlassen abmelden, sonst quittiert sie weiter.
 */
@Composable
fun GespraechSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    partnerKennung: String = "",
    partnerName: String = "",
    sozial: Sozialstand = Sozialstand(),
    meineKennung: String = "",
    beiOeffnen: (String) -> Unit = {},
    beiSchliessen: () -> Unit = {},
    beiSenden: (String, String) -> Unit = { _, _ -> },
    beiTermin: (Long, Boolean) -> Unit = { _, _ -> },
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(partnerKennung) { beiOeffnen(partnerKennung) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { beiSchliessen() }
    }
    var text by remember { mutableStateOf("") }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = partnerName.ifBlank { "Gespräch" },
            unterzeile = "Direktnachrichten",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        if (sozial.laedt) {
            SehrLeise("Der Verlauf wird geladen …")
        } else if (sozial.verlauf.isEmpty()) {
            Leerhinweis("Noch keine Nachricht — mach den Anfang.")
        }

        sozial.verlauf.forEach { nachricht ->
            Sprechblase(
                nachricht = nachricht,
                eigene = nachricht.von == meineKennung,
                beiTermin = beiTermin,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Feld(
                wert = text,
                beiAenderung = { text = it.take(500) },
                platzhalter = "Schreib was …",
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "Senden",
                {
                    beiSenden(partnerKennung, text)
                    text = ""
                },
                aktiv = text.isNotBlank(),
                kompakt = true,
            )
        }
    }
}

/**
 * Eine Sprechblase — eigene rechts in Amber, fremde links auf Fläche.
 *
 * Der Doppelhaken ist die Lesebestätigung; ob sie kommt, entscheidet die
 * Privatsphäre des Partners, nicht die App.
 */
@Composable
private fun Sprechblase(
    nachricht: Nachricht,
    eigene: Boolean,
    beiTermin: (Long, Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (eigene) androidx.compose.foundation.layout.Spacer(Modifier.weight(0.2f))
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier
                .weight(0.8f)
                .background(
                    if (eigene) Farben.Amber.copy(alpha = 0.14f) else Farben.Flaeche,
                    RoundedCornerShape(10.dp),
                )
                .padding(Abstand.Normal),
        ) {
            // Ein Terminvorschlag in der Zeile — mit Antwortknöpfen beim
            // Empfänger, solange er offen ist.
            if (nachricht.terminText != null || nachricht.terminZeitpunkt != null) {
                Text(
                    text = "📅 ${nachricht.terminText ?: "Termin"}" +
                        (nachricht.terminZeitpunkt?.let { " · ${zeitpunkt(it)}" } ?: ""),
                    style = Schrift.Klein,
                    color = Farben.AmberHell,
                )
                when (nachricht.terminStatus) {
                    "Zugesagt" -> SehrLeise("Zugesagt ✓")
                    "Abgesagt" -> SehrLeise("Abgesagt")
                    else -> if (!eigene) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf("Zusagen", { beiTermin(nachricht.nr, true) }, kompakt = true)
                            Knopf(
                                "Absagen",
                                { beiTermin(nachricht.nr, false) },
                                art = Knopfart.Leise,
                                kompakt = true,
                            )
                        }
                    }
                }
            }

            if (nachricht.text.isNotBlank()) {
                Text(text = nachricht.text, style = Schrift.Klein, color = Farben.Text)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                SehrLeise(uhrzeit(nachricht.gesendetUm), mono = true)
                if (eigene && nachricht.gelesenUm != null) {
                    Text(
                        text = "✓✓",
                        style = Schrift.Winzig,
                        color = Farben.Amber,
                    )
                }
            }
        }
        if (!eigene) androidx.compose.foundation.layout.Spacer(Modifier.weight(0.2f))
    }
}
