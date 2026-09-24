package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Sozialstand
import de.pagerspass.pagerspass.mobil.Wachendaten
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.GemeinschaftDetail
import de.pagerspass.pagerspass.netz.Gemeinschaftsmitglied
import de.pagerspass.pagerspass.netz.Gemeinschaftsnachricht
import de.pagerspass.pagerspass.netz.Wachentermin
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Das Wachensystem — übertragen aus `GemeinschaftView.vue` und
 * `GemeinschaftenView.vue` in die Handyform.
 *
 * <b>Zwei Reiter, wie im Web: Chat und Wache.</b> Der Chat trägt Aushang und
 * Verlauf; die Wache trägt Anträge, Meldungen, Mannschaft, Dienstplan und die
 * Mitgliedschaft. Was welche Rolle darf, entscheidet der Server — die App
 * blendet nur aus, was ohnehin abgewiesen würde: Führen ab Zugführer,
 * Verwalten nur als Leitung.
 */
class WachenGriffe(
    val chatSenden: (String, String) -> Unit = { _, _ -> },
    val antragEntscheiden: (String, Long, Boolean) -> Unit = { _, _, _ -> },
    val rolleSetzen: (String, String, String) -> Unit = { _, _, _ -> },
    val leitungUebergeben: (String, String) -> Unit = { _, _ -> },
    val mitgliedEntfernen: (String, String) -> Unit = { _, _ -> },
    val austreten: (String) -> Unit = {},
    val einladen: (String, String) -> Unit = { _, _ -> },
    val pinnwand: (String, String?) -> Unit = { _, _ -> },
    val terminAnlegen: (String, String, String) -> Unit = { _, _, _ -> },
    val terminAntworten: (String, Long, String) -> Unit = { _, _, _ -> },
    val terminAbsagen: (String, Long) -> Unit = { _, _ -> },
    val zeileMelden: (String, Long, String?) -> Unit = { _, _, _ -> },
    val zeileEntfernen: (String, Long) -> Unit = { _, _ -> },
    val meldungErledigt: (String, Long) -> Unit = { _, _ -> },
    val clanrunde: (String) -> Unit = {},
    val beitreten: (String) -> Unit = {},
    val gruenden: (String, String?) -> Unit = { _, _ -> },
    val bewerben: (String, String?) -> Unit = { _, _ -> },
    val bewerbungZurueckziehen: (Long) -> Unit = {},
    val einladungAnnehmen: (Long) -> Unit = {},
)

@Composable
fun WachenSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    wache: Bereichsstand<Wachendaten> = Bereichsstand(),
    detail: Bereichsstand<GemeinschaftDetail?> = Bereichsstand(),
    antraege: Bereichsstand<List<de.pagerspass.pagerspass.netz.Gemeinschaftsantrag>> =
        Bereichsstand(),
    sozial: Sozialstand = Sozialstand(),
    meineKennung: String = "",
    beiLaden: () -> Unit = {},
    beiDetail: (String) -> Unit = {},
    beiChatOeffnen: (String) -> Unit = {},
    beiAntraege: () -> Unit = {},
    griffe: WachenGriffe = WachenGriffe(),
) {
    LaunchedEffect(Unit) { beiLaden() }

    val eigene = wache.inhalt?.eigene
    // Auch die Mitgliederzahl ist ein Schlüssel: Tritt jemand bei, kommt das
    // Gemeinschafts-Ereignis, `wacheLaden` hebt die Zahl — und erst dieser
    // Effekt holt die Mannschaftsliste nach. Ohne ihn fehlte der Neue.
    LaunchedEffect(eigene?.id, eigene?.mitglieder) {
        eigene?.id?.let {
            beiDetail(it)
            beiChatOeffnen(it)
        }
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        Bereich(
            laedt = wache.laedt,
            fehler = wache.fehler,
            inhalt = wache.inhalt,
            beiErneut = beiLaden,
        ) { daten ->
            if (daten.eigene != null) {
                EigeneWacheSeite(
                    gemeinschaft = daten.eigene,
                    detail = detail.inhalt,
                    sozial = sozial,
                    meineKennung = meineKennung,
                    griffe = griffe,
                )
            } else {
                WachenSuche(
                    offene = daten.offene,
                    antraege = antraege.inhalt.orEmpty(),
                    beiAntraege = beiAntraege,
                    griffe = griffe,
                )
            }
        }
    }
}

// ------------------------------------------------------------ Eigene Wache

@Composable
private fun ColumnScope.EigeneWacheSeite(
    gemeinschaft: Gemeinschaft,
    detail: GemeinschaftDetail?,
    sozial: Sozialstand,
    meineKennung: String,
    griffe: WachenGriffe,
) {
    var reiter by rememberSaveable { mutableStateOf(0) }
    val id = gemeinschaft.id
    val offen = (detail?.antraege?.size ?: 0) + (detail?.meldungen?.size ?: 0)

    // Kopf — Musterband, Emblem mit Initialen, Name, Zeile. Dieselbe Bauform
    // wie der Wachenkopf des Web: Das Band trägt das Kopfmuster der Wache.
    val ton = de.pagerspass.pagerspass.ui.schmuck.Wappen.ton(
        gemeinschaft.id,
        gemeinschaft.wappenfarbe,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .flaeche(ecke = 12.dp)
            .kopfband(gemeinschaft.kopfmuster, ton)
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Das Emblem — zwei Initialen im Tonkreis, wie das Web ohne
            // gewähltes Zeichen.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(ton.copy(alpha = 0.35f), CircleShape)
                    .border(1.5.dp, ton.copy(alpha = 0.8f), CircleShape),
            ) {
                Text(
                    text = gemeinschaft.name.split(" ", "-")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercaseChar() }
                        .joinToString(""),
                    style = Schrift.Gross,
                    color = Farben.Text,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (gemeinschaft.beiname.isNotBlank()) {
                    Text(
                        text = gemeinschaft.beiname,
                        style = Schrift.Winzig,
                        color = Farben.AmberHell,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = gemeinschaft.name,
                    style = Schrift.Titel,
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SehrLeise(
                    listOfNotNull(
                        "${gemeinschaft.mitglieder} von ${gemeinschaft.maxMitglieder} Mitgliedern",
                        gemeinschaft.landkreis?.ifBlank { null },
                        rollenname(gemeinschaft.eigeneRolle)?.let { "du bist $it" },
                    ).joinToString(" · "),
                )
            }
        }
        if (!gemeinschaft.beschreibung.isNullOrBlank()) {
            Text(
                text = gemeinschaft.beschreibung,
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
        }
        detail?.statistik?.let { s ->
            // FlowRow, nicht Row: Drei Marken passen nicht immer nebeneinander,
            // und eine Marke, die senkrecht bricht, liest niemand mehr.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Marke("Stufe ${s.stufe} · ${s.bezeichnung}", farbe = Farben.AmberHell)
                Marke("${s.schichten} Schichten")
                Marke("${s.einsaetze} Einsätze")
            }
        }
        if (gemeinschaft.darfFuehren) {
            Knopf("Clanrunde eröffnen", { griffe.clanrunde(id) }, kompakt = true)
        }
    }

    // Der Hinweis auf die laufende Clanrunde — mit „Dazustoßen".
    gemeinschaft.laufendeRundeCode?.let { code ->
        Kasten(abstandInnen = Abstand.Klein) {
            SehrLeise("Eine Clanrunde läuft — Raum $code.")
        }
    }

    Reiterreihe {
        Reiter("Chat", offen = reiter == 0, beiDruck = { reiter = 0 })
        Reiter("Wache", offen = reiter == 1, beiDruck = { reiter = 1 }, marke = offen)
    }

    if (reiter == 0) {
        WachenChat(
            gemeinschaft = gemeinschaft,
            zeilen = sozial.wachenchat,
            meineKennung = meineKennung,
            griffe = griffe,
        )
    } else {
        WachenVerwaltung(
            gemeinschaft = gemeinschaft,
            detail = detail,
            meineKennung = meineKennung,
            griffe = griffe,
        )
    }
}

@Composable
private fun ColumnScope.WachenChat(
    gemeinschaft: Gemeinschaft,
    zeilen: List<Gemeinschaftsnachricht>,
    meineKennung: String,
    griffe: WachenGriffe,
) {
    var satz by remember { mutableStateOf("") }
    var aushangOffen by remember { mutableStateOf(false) }
    var zeileOffen by remember { mutableStateOf<Gemeinschaftsnachricht?>(null) }

    // Der Aushang — angeheftet über dem Verlauf, wie die Pinnwand des Web.
    if (!gemeinschaft.ankuendigung.isNullOrBlank() || gemeinschaft.darfFuehren) {
        Kasten(abstandInnen = Abstand.Klein) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Ueberschrift("Aushang")
                    Text(
                        text = gemeinschaft.ankuendigung ?: "Nichts angeheftet.",
                        style = Schrift.Klein,
                        color = if (gemeinschaft.ankuendigung != null) Farben.Text
                        else Farben.TextSehrLeise,
                    )
                }
                if (gemeinschaft.darfFuehren) {
                    Knopf("Ändern", { aushangOffen = true }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
    }

    if (zeilen.isEmpty()) {
        Leerhinweis("Noch nichts geschrieben — sag Hallo.")
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
        ) {
            zeilen.takeLast(60).forEach { zeile ->
                Chatzeile(
                    zeile = zeile,
                    eigene = zeile.von == meineKennung,
                    beiLang = { zeileOffen = zeile },
                )
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Feld(
            wert = satz,
            beiAenderung = { satz = it.take(500) },
            platzhalter = "An die Wache …",
            modifier = Modifier.weight(1f),
        )
        Knopf(
            "Senden",
            {
                griffe.chatSenden(gemeinschaft.id, satz)
                satz = ""
            },
            aktiv = satz.isNotBlank(),
            kompakt = true,
        )
    }

    if (aushangOffen) {
        var text by remember { mutableStateOf(gemeinschaft.ankuendigung.orEmpty()) }
        Blende(
            titel = "Aushang",
            beiSchliessen = { aushangOffen = false },
            fuss = {
                Knopf("Abnehmen", {
                    griffe.pinnwand(gemeinschaft.id, null)
                    aushangOffen = false
                }, art = Knopfart.Leise, kompakt = true)
                Knopf("Anheften", {
                    griffe.pinnwand(gemeinschaft.id, text.trim())
                    aushangOffen = false
                }, art = Knopfart.Haupt, kompakt = true, aktiv = text.isNotBlank())
            },
        ) {
            Feld(
                wert = text,
                beiAenderung = { text = it.take(500) },
                platzhalter = "Was alle sehen sollen …",
            )
        }
    }

    zeileOffen?.let { zeile ->
        val eigene = zeile.von == meineKennung
        Blende(titel = "Zeile von ${zeile.vonName}", beiSchliessen = { zeileOffen = null }) {
            Text(text = zeile.text, style = Schrift.Klein, color = Farben.TextLeise)
            if (eigene || gemeinschaft.darfFuehren) {
                Knopf("Zeile herausnehmen", {
                    griffe.zeileEntfernen(gemeinschaft.id, zeile.nr)
                    zeileOffen = null
                }, art = Knopfart.Gefahr, kompakt = true)
            }
            if (!eigene) {
                Knopf("Melden", {
                    griffe.zeileMelden(gemeinschaft.id, zeile.nr, null)
                    zeileOffen = null
                }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
}

@Composable
private fun Chatzeile(
    zeile: Gemeinschaftsnachricht,
    eigene: Boolean,
    beiLang: () -> Unit,
) {
    when {
        zeile.entfernt -> SehrLeise("Zeile von ${zeile.vonName} herausgenommen.")
        zeile.art == "System" -> SehrLeise(zeile.text)
        zeile.art == "Clanrunde" -> Kasten(abstandInnen = Abstand.Klein) {
            SehrLeise("${zeile.vonName} hat eine Clanrunde eröffnet — Raum ${zeile.roomCode}.")
        }
        // Die eigene Blase rechts, fremde links — dieselbe Sprache wie das
        // Gespräch, damit „von mir" ohne Lesen erkennbar ist.
        else -> Row(modifier = Modifier.fillMaxWidth()) {
            if (eigene) androidx.compose.foundation.layout.Spacer(Modifier.weight(0.18f))
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .weight(0.82f)
                    .background(
                        if (eigene) Farben.Amber.copy(alpha = 0.13f) else Farben.Flaeche,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable(onClick = beiLang)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = zeile.vonName,
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = if (eigene) Farben.Amber else Farben.TextSehrLeise,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = uhrzeit(zeile.gesendetUm),
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = Farben.TextSehrLeise,
                    )
                }
                Text(text = zeile.text, style = Schrift.Klein, color = Farben.Text)
            }
            if (!eigene) androidx.compose.foundation.layout.Spacer(Modifier.weight(0.18f))
        }
    }
}

@Composable
private fun ColumnScope.WachenVerwaltung(
    gemeinschaft: Gemeinschaft,
    detail: GemeinschaftDetail?,
    meineKennung: String,
    griffe: WachenGriffe,
) {
    val id = gemeinschaft.id
    var mitgliedOffen by remember { mutableStateOf<Gemeinschaftsmitglied?>(null) }
    var einladenOffen by remember { mutableStateOf(false) }
    var terminOffen by remember { mutableStateOf(false) }
    var austrittGefragt by remember { mutableStateOf(false) }

    // Beitrittsanträge — nur Entscheider bekommen sie überhaupt.
    detail?.antraege?.takeIf { it.isNotEmpty() }?.let { antraege ->
        Ueberschrift("Beitrittsanträge")
        antraege.forEach { antrag ->
            Kasten(abstandInnen = Abstand.Klein) {
                Text(antrag.anzeigename, style = Schrift.Normal, color = Farben.Text)
                antrag.nachricht?.let { SehrLeise("„$it“") }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Aufnehmen", {
                        griffe.antragEntscheiden(id, antrag.nr, true)
                    }, art = Knopfart.Haupt, kompakt = true)
                    Knopf("Ablehnen", {
                        griffe.antragEntscheiden(id, antrag.nr, false)
                    }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
    }

    // Gemeldete Zeilen — dieselbe Bühne.
    detail?.meldungen?.takeIf { it.isNotEmpty() }?.let { meldungen ->
        Ueberschrift("Gemeldete Zeilen")
        meldungen.forEach { meldung ->
            Kasten(abstandInnen = Abstand.Klein) {
                SehrLeise("${meldung.verfasserName} — gemeldet von ${meldung.melderName}")
                Text("„${meldung.text}“", style = Schrift.Klein, color = Farben.Text)
                meldung.grund?.let { SehrLeise("Grund: $it") }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Herausnehmen", {
                        griffe.zeileEntfernen(id, meldung.nachrichtNr)
                        griffe.meldungErledigt(id, meldung.nr)
                    }, art = Knopfart.Gefahr, kompakt = true)
                    Knopf("In Ordnung", {
                        griffe.meldungErledigt(id, meldung.nr)
                    }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
    }

    // Mannschaft — sortiert kommt sie vom Server, hier wird nicht umsortiert.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Ueberschrift("Mannschaft")
        Marke("${gemeinschaft.mitglieder} / ${gemeinschaft.maxMitglieder}")
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        if (gemeinschaft.darfFuehren) {
            Knopf("Einladen", { einladenOffen = true }, art = Knopfart.Leise, kompakt = true)
        }
    }
    detail?.mitglieder?.forEach { mitglied ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .flaeche(ecke = 9.dp)
                .clickable { mitgliedOffen = mitglied }
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mitglied.anzeigename,
                    style = Schrift.Normal,
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SehrLeise(
                    listOfNotNull(
                        mitglied.level?.let { "Stufe $it" },
                        mitglied.anwesenheit?.ansage,
                    ).joinToString(" · "),
                )
            }
            Marke(
                rollenname(mitglied.rolle) ?: mitglied.rolle,
                farbe = if (mitglied.rolle == "Leitung") Farben.Amber else Farben.TextLeise,
            )
        }
    }

    // Dienstplan.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Ueberschrift("Dienstplan")
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        if (gemeinschaft.darfFuehren) {
            Knopf("Dienst planen", { terminOffen = true }, art = Knopfart.Leise, kompakt = true)
        }
    }
    val termine = detail?.termine?.filter { !it.abgesagt }.orEmpty()
    if (termine.isEmpty()) {
        Leerhinweis("Kein Dienst geplant.")
    } else {
        termine.forEach { termin ->
            Terminzeile(termin, meineKennung, gemeinschaft, griffe)
        }
    }

    // Beitrittscode — ab Zugführer sichtbar, so kommt man neue Leute holen.
    gemeinschaft.beitrittscode?.let { code ->
        Kasten(abstandInnen = Abstand.Klein) {
            Ueberschrift("Beitrittscode")
            Text(text = code, style = Schrift.MonoNormal, color = Farben.Amber)
            SehrLeise("Weitergeben heißt einladen — wer ihn hat, kommt rein.")
        }
    }

    // Mitgliedschaft — der Austritt mit Rückfrage im zweiten Klick.
    Ueberschrift("Mitgliedschaft")
    Knopf(
        if (austrittGefragt) "Wirklich austreten?" else "Aus der Wache austreten",
        {
            if (austrittGefragt) griffe.austreten(id) else austrittGefragt = true
        },
        art = Knopfart.Gefahr,
        kompakt = true,
    )

    // ---------------------------------------------------------- Blenden
    mitgliedOffen?.let { mitglied ->
        val selbst = mitglied.kennung == meineKennung
        Blende(titel = mitglied.anzeigename, beiSchliessen = { mitgliedOffen = null }) {
            SehrLeise(rollenname(mitglied.rolle) ?: mitglied.rolle)
            if (gemeinschaft.istLeitung && !selbst) {
                when (mitglied.rolle) {
                    "Mitglied" -> Knopf("Zum Zugführer befördern", {
                        griffe.rolleSetzen(id, mitglied.kennung, "Zugfuehrer")
                        mitgliedOffen = null
                    }, kompakt = true)
                    "Zugfuehrer" -> Knopf("Zum Mitglied zurückstufen", {
                        griffe.rolleSetzen(id, mitglied.kennung, "Mitglied")
                        mitgliedOffen = null
                    }, kompakt = true)
                }
                Knopf("Leitung übergeben", {
                    griffe.leitungUebergeben(id, mitglied.kennung)
                    mitgliedOffen = null
                }, art = Knopfart.Leise, kompakt = true)
                Knopf("Aus der Gemeinschaft entfernen", {
                    griffe.mitgliedEntfernen(id, mitglied.kennung)
                    mitgliedOffen = null
                }, art = Knopfart.Gefahr, kompakt = true)
            }
        }
    }

    if (einladenOffen) {
        var wen by remember { mutableStateOf("") }
        Blende(
            titel = "In die Wache einladen",
            beiSchliessen = { einladenOffen = false },
            fuss = {
                Knopf("Einladen", {
                    griffe.einladen(id, wen.trim())
                    einladenOffen = false
                }, art = Knopfart.Haupt, aktiv = wen.isNotBlank(), kompakt = true)
            },
        ) {
            Feld(wert = wen, beiAenderung = { wen = it }, platzhalter = "Benutzername oder Kennung")
        }
    }

    if (terminOffen) {
        var titel by remember { mutableStateOf("") }
        var datum by remember { mutableStateOf("") }
        var zeit by remember { mutableStateOf("19:30") }
        Blende(
            titel = "Dienst planen",
            beiSchliessen = { terminOffen = false },
            fuss = {
                Knopf("Anlegen", {
                    griffe.terminAnlegen(id, titel.trim(), "${datum}T$zeit:00+02:00")
                    terminOffen = false
                }, art = Knopfart.Haupt, aktiv = titel.isNotBlank() && datum.length == 10, kompakt = true)
            },
        ) {
            Feld(wert = titel, beiAenderung = { titel = it.take(80) }, platzhalter = "Was steht an?")
            Feld(wert = datum, beiAenderung = { datum = it.take(10) }, platzhalter = "JJJJ-MM-TT")
            Feld(wert = zeit, beiAenderung = { zeit = it.take(5) }, platzhalter = "HH:MM")
        }
    }
}

@Composable
private fun Terminzeile(
    termin: Wachentermin,
    meineKennung: String,
    gemeinschaft: Gemeinschaft,
    griffe: WachenGriffe,
) {
    val meine = termin.zusagen.firstOrNull { it.kennung == meineKennung }?.antwort ?: "Offen"
    val zusagen = termin.zusagen.count { it.antwort == "Zugesagt" }

    Kasten(abstandInnen = Abstand.Klein) {
        Text(termin.titel, style = Schrift.Normal, color = Farben.Text)
        SehrLeise(
            listOfNotNull(
                zeitpunkt(termin.wann),
                termin.vonName?.let { "geplant von $it" },
                "$zusagen zugesagt",
            ).joinToString(" · "),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                if (meine == "Zugesagt") "Zugesagt ✓" else "Zusagen",
                { griffe.terminAntworten(gemeinschaft.id, termin.nr, "Zugesagt") },
                art = if (meine == "Zugesagt") Knopfart.Haupt else Knopfart.Normal,
                kompakt = true,
            )
            Knopf(
                if (meine == "Abgesagt") "Abgesagt" else "Absagen",
                { griffe.terminAntworten(gemeinschaft.id, termin.nr, "Abgesagt") },
                art = Knopfart.Leise,
                kompakt = true,
            )
            if (gemeinschaft.darfFuehren || termin.von == meineKennung) {
                Knopf(
                    "Streichen",
                    { griffe.terminAbsagen(gemeinschaft.id, termin.nr) },
                    art = Knopfart.Gefahr,
                    kompakt = true,
                )
            }
        }
    }
}

// ------------------------------------------------------------ Ohne Wache

@Composable
private fun ColumnScope.WachenSuche(
    offene: List<Gemeinschaft>,
    antraege: List<de.pagerspass.pagerspass.netz.Gemeinschaftsantrag>,
    beiAntraege: () -> Unit,
    griffe: WachenGriffe,
) {
    LaunchedEffect(Unit) { beiAntraege() }
    var code by remember { mutableStateOf("") }
    var gruendenOffen by remember { mutableStateOf(false) }
    var bewerbungFuer by remember { mutableStateOf<Gemeinschaft?>(null) }

    Seitenkopf(titel = "Wachen", unterzeile = "Such dir eine oder gründe eine eigene")

    // Offene Einladungen zuerst — die kürzeste Tür.
    val einladungen = antraege.filter { it.richtung == "Einladung" && it.stand == "Offen" }
    einladungen.forEach { einladung ->
        Kasten(marke = true, abstandInnen = Abstand.Klein) {
            Text(
                "Du bist eingeladen: ${einladung.gemeinschaftName}",
                style = Schrift.Normal,
                color = Farben.Text,
            )
            einladung.vonName?.let { SehrLeise("von $it") }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Beitreten", {
                    griffe.einladungAnnehmen(einladung.nr)
                }, art = Knopfart.Haupt, kompakt = true)
                Knopf("Ablehnen", {
                    griffe.bewerbungZurueckziehen(einladung.nr)
                }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }

    Kasten(abstandInnen = Abstand.Klein) {
        Ueberschrift("Mit Beitrittscode")
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
        ) {
            Feld(
                wert = code,
                beiAenderung = { code = it.take(6).uppercase() },
                platzhalter = "ABC123",
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "Beitreten",
                { griffe.beitreten(code) },
                aktiv = code.length == 6,
                kompakt = true,
            )
        }
    }

    Kasten(abstandInnen = Abstand.Klein) {
        Ueberschrift("Selbst gründen")
        SehrLeise("Ab Stufe 3 — du wirst die Leitung.")
        Knopf("Wache gründen", { gruendenOffen = true }, kompakt = true)
    }

    Ueberschrift("Öffentliche Wachen")
    if (offene.isEmpty()) {
        Leerhinweis("Keine offene Wache gefunden.")
    } else {
        offene.forEach { g ->
            Kasten(abstandInnen = Abstand.Klein) {
                Text(g.name, style = Schrift.Normal, color = Farben.Text)
                SehrLeise(
                    listOfNotNull(
                        g.landkreis?.ifBlank { null },
                        "${g.mitglieder}/${g.maxMitglieder} Mitglieder",
                        "ab Stufe ${g.mindestLevel}".takeIf { g.mindestLevel > 0 },
                    ).joinToString(" · "),
                )
                when (g.beitrittModus) {
                    "Offen" -> Knopf("Beitreten", { griffe.bewerben(g.id, null) }, kompakt = true)
                    "Antrag" -> Knopf("Bewerben", { bewerbungFuer = g }, kompakt = true)
                    // Bei `Einladung` gibt es bewusst keinen Knopf.
                }
            }
        }
    }

    // Eigene offene Bewerbungen — mit dem Rückzieher.
    val bewerbungen = antraege.filter { it.richtung == "Bewerbung" && it.stand == "Offen" }
    if (bewerbungen.isNotEmpty()) {
        Ueberschrift("Deine Bewerbungen")
        bewerbungen.forEach { b ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SehrLeise(b.gemeinschaftName, modifier = Modifier.weight(1f))
                Knopf("Zurückziehen", {
                    griffe.bewerbungZurueckziehen(b.nr)
                }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }

    if (gruendenOffen) {
        var name by remember { mutableStateOf("") }
        var beschreibung by remember { mutableStateOf("") }
        Blende(
            titel = "Wache gründen",
            beiSchliessen = { gruendenOffen = false },
            fuss = {
                Knopf("Gründen", {
                    griffe.gruenden(name.trim(), beschreibung.trim().ifBlank { null })
                    gruendenOffen = false
                }, art = Knopfart.Haupt, aktiv = name.trim().length >= 3, kompakt = true)
            },
        ) {
            Feld(wert = name, beiAenderung = { name = it.take(40) }, platzhalter = "Name der Wache")
            Feld(
                wert = beschreibung,
                beiAenderung = { beschreibung = it.take(300) },
                platzhalter = "Worum es euch geht …",
            )
        }
    }

    bewerbungFuer?.let { g ->
        var nachricht by remember { mutableStateOf("") }
        Blende(
            titel = "Bewerben bei ${g.name}",
            beiSchliessen = { bewerbungFuer = null },
            fuss = {
                Knopf("Bewerben", {
                    griffe.bewerben(g.id, nachricht.trim().ifBlank { null })
                    bewerbungFuer = null
                }, art = Knopfart.Haupt, kompakt = true)
            },
        ) {
            Feld(
                wert = nachricht,
                beiAenderung = { nachricht = it.take(300) },
                platzhalter = "Ein Satz zu dir (freiwillig)",
            )
        }
    }
}

/** Wie eine Rolle heißt, wenn ein Mensch sie liest. */
private fun rollenname(rolle: String?): String? = when (rolle) {
    "Mitglied" -> "Mitglied"
    "Zugfuehrer" -> "Zugführer"
    "Leitung" -> "Leitung"
    else -> null
}
