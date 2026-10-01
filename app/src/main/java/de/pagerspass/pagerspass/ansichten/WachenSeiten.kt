package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Kreisstand
import de.pagerspass.pagerspass.mobil.Sozialstand
import de.pagerspass.pagerspass.mobil.Wachendaten
import de.pagerspass.pagerspass.mobil.Wachenfilter
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.GemeinschaftDetail
import de.pagerspass.pagerspass.netz.Gemeinschaftsantrag
import de.pagerspass.pagerspass.netz.Gemeinschaftsmitglied
import de.pagerspass.pagerspass.netz.Gemeinschaftsnachricht
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Schicht
import de.pagerspass.pagerspass.netz.Wacheneinstellungen
import de.pagerspass.pagerspass.netz.Wachentermin
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.schmuck.Profilzeile
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Das Wachensystem — übertragen aus `GemeinschaftView.vue` und
 * `GemeinschaftenView.vue` in die Handyform.
 *
 * <b>Drei Reiter, wie im Web: Chat, Mannschaft, Wache.</b> Der Chat trägt Aushang
 * und Verlauf; die Mannschaft Anträge, Meldungen, Mitglieder und wer am meisten
 * fährt; die Wache den Tag, den Dienstplan, die Hilfsfristen, das Logbuch, die
 * Mitgliedschaft und die Laufbahn. Was welche Rolle darf, entscheidet der Server —
 * die App blendet nur aus, was ohnehin abgewiesen würde: Führen ab Zugführer,
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
    /** Name, Beschreibung, Landkreis-Kennung, Landkreis-Name. */
    val gruenden: (String, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    val bewerben: (String, String?) -> Unit = { _, _ -> },
    val bewerbungZurueckziehen: (Long) -> Unit = {},
    val einladungAnnehmen: (Long) -> Unit = {},
    // Die Extras — Freundeskreis statt Sitzung.
    /** Den Hinweis auf die laufende Clanrunde abnehmen. */
    val clanrundeSchliessen: (String) -> Unit = {},
    /** In eine laufende Runde — über ihren Raumcode. */
    val dazustossen: (String) -> Unit = {},
    /** Ein Profil öffnen — über den Benutzernamen. */
    val profil: (String) -> Unit = {},
    val tagSetzen: (String, String) -> Unit = { _, _ -> },
    val einstellungen: (String, Wacheneinstellungen) -> Unit = { _, _ -> },
    val codeErneuern: (String) -> Unit = {},
    val aufloesen: (String) -> Unit = {},
    val schmuck: (String, String, String?) -> Unit = { _, _, _ -> },
    val farbe: (String, Int) -> Unit = { _, _ -> },
    val rangliste: () -> Unit = {},
    val shop: () -> Unit = {},
    val hilfsfristen: (String) -> Unit = {},
    val laufbahn: () -> Unit = {},
    val suchen: (Wachenfilter) -> Unit = {},
    val landkreisvorschlag: () -> Unit = {},
    val meldungWeg: () -> Unit = {},
)

@Composable
fun WachenSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    wache: Bereichsstand<Wachendaten> = Bereichsstand(),
    detail: Bereichsstand<GemeinschaftDetail?> = Bereichsstand(),
    antraege: Bereichsstand<List<Gemeinschaftsantrag>> = Bereichsstand(),
    sozial: Sozialstand = Sozialstand(),
    meineKennung: String = "",
    konto: Konto? = null,
    kreis: Kreisstand = Kreisstand(),
    landkreise: List<Landkreis> = emptyList(),
    /** Die eigenen Schichten — die eigene Kurve neben denen der Mannschaft. */
    schichten: List<Schicht> = emptyList(),
    server: String = "",
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
                    detail = detail.inhalt?.takeIf { it.gemeinschaft.id == daten.eigene.id },
                    sozial = sozial,
                    meineKennung = meineKennung,
                    konto = konto,
                    kreis = kreis,
                    landkreise = landkreise,
                    schichten = schichten,
                    server = server,
                    griffe = griffe,
                )
            } else {
                WachenSuche(
                    offene = daten.offene,
                    antraege = antraege.inhalt.orEmpty(),
                    kreis = kreis,
                    landkreise = landkreise,
                    beiAntraege = beiAntraege,
                    griffe = griffe,
                    level = konto?.level ?: 1,
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
    konto: Konto?,
    kreis: Kreisstand,
    landkreise: List<Landkreis>,
    schichten: List<Schicht>,
    server: String,
    griffe: WachenGriffe,
) {
    var reiter by rememberSaveable { mutableStateOf(0) }
    var einstellungenOffen by remember { mutableStateOf(false) }
    var aussehenOffen by remember { mutableStateOf(false) }
    var planenOffen by remember { mutableStateOf(false) }
    val id = gemeinschaft.id
    val termine = detail?.termine?.filter { !it.abgesagt }.orEmpty()
    // Ältere Server schicken keine Obergrenze — dann gilt die der ersten Stufe.
    val maxTermine = detail?.statistik?.maxTermine?.takeIf { it > 0 } ?: 1
    val offen = (detail?.antraege?.size ?: 0) + (detail?.meldungen?.size ?: 0)
    // Was gerade gekauft oder angelegt wurde, steht zuerst im Detail — der Kopf
    // der Liste folgt einen Atemzug später. Für das Aussehen gilt das frischere.
    val kopf = detail?.gemeinschaft?.let {
        gemeinschaft.copy(
            name = it.name,
            beschreibung = it.beschreibung,
            wappenfarbe = it.wappenfarbe,
            kopfmuster = it.kopfmuster,
            emblemrahmen = it.emblemrahmen,
            emblemzeichen = it.emblemzeichen,
            beiname = it.beiname,
            tag = it.tag ?: gemeinschaft.tag,
        )
    } ?: gemeinschaft
    val schatz = detail?.schatz
    val beiname = schatz?.schmuck
        ?.firstOrNull { it.art == "Beiname" && it.stueckId == kopf.beiname }?.name
        ?: kopf.beiname.ifBlank { null }

    // Kopf — Musterband, Emblem, Name, Zeile, Wege; darunter die Stufe. Dieselbe
    // Bauform wie der Wachenkopf des Web: Das Band trägt das Kopfmuster der Wache.
    val ton = de.pagerspass.pagerspass.ui.schmuck.Wappen.ton(kopf.id, kopf.wappenfarbe)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .flaeche(ecke = 12.dp)
            .kopfband(kopf.kopfmuster, ton)
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wachenemblem(kopf)
            Column(modifier = Modifier.weight(1f)) {
                // Der Beiname steht an der Stelle des Etiketts und ersetzt es.
                if (beiname != null) {
                    Text(
                        text = beiname,
                        style = Schrift.Winzig,
                        color = Farben.AmberHell,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Etikett("Wachengemeinschaft")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    kopf.tag?.takeIf { it.isNotBlank() }?.let { Wachentagplakette(it) }
                    Text(
                        text = kopf.name,
                        style = Schrift.Titel,
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SehrLeise(
                    listOfNotNull(
                        "${gemeinschaft.mitglieder} von ${gemeinschaft.maxMitglieder} Mitgliedern",
                        gemeinschaft.landkreis?.ifBlank { null },
                        rollenname(gemeinschaft.eigeneRolle)?.let { "du bist $it" },
                    ).joinToString(" · "),
                )
            }
        }
        if (!kopf.beschreibung.isNullOrBlank()) {
            Text(text = kopf.beschreibung, style = Schrift.Klein, color = Farben.TextLeise)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        ) {
            // „Clanrunde" steht nicht mehr hier, sondern im Band „Gemeinsam fahren"
            // darunter, neben „Dienst planen" — siehe `Wachenplan.kt`.
            // Anpassen darf, wer auch kauft. Der Knopf steht neben den
            // Einstellungen und nicht darin: Wie die Wache aussieht, ändert man
            // oft und gern, wie sie aufnimmt, selten und mit Bedacht.
            if (gemeinschaft.darfFuehren && schatz != null) {
                Knopf("Aussehen", { aussehenOffen = true }, art = Knopfart.Leise, kompakt = true)
            }
            if (gemeinschaft.istLeitung) {
                Knopf("Einstellungen", { einstellungenOffen = true }, art = Knopfart.Leise, kompakt = true)
            }
            Knopf("Rangliste", griffe.rangliste, art = Knopfart.Leise, kompakt = true)
            Knopf("Shop", griffe.shop, art = Knopfart.Leise, kompakt = true)
        }
        detail?.statistik?.let { WachenStufe(it) }
    }

    // Die Kennzahlen der Wache als Tafel — dieselbe wie oben auf der Übersicht des
    // Dienstbuchs. Über den Reitern und nicht in einem: Sie gehören zum Namen der
    // Wache wie der Kopf darüber.
    detail?.statistik?.let { Wachentafel(it) }

    Meldungszeile(kreis.meldung, kreis.hinweis, griffe.meldungWeg)

    // Jetzt fahren oder später: laufende Clanrunde, nächster Dienst, „Clanrunde
    // starten" und „Dienst planen" in einem Wink über den Reitern.
    GemeinsamFahren(
        laufendeRunde = gemeinschaft.laufendeRundeCode,
        termine = termine,
        ich = meineKennung,
        darfFuehren = gemeinschaft.darfFuehren,
        darfRundeSchliessen = gemeinschaft.darfRundeSchliessen,
        landkreis = gemeinschaft.landkreis,
        maxTermine = maxTermine,
        laeuft = kreis.laeuft,
        beiStarten = { griffe.clanrunde(id) },
        beiPlanen = { planenOffen = true },
        beiBeitreten = griffe.dazustossen,
        beiSchliessen = { griffe.clanrundeSchliessen(id) },
        beiAntworten = { nr, antwort -> griffe.terminAntworten(id, nr, antwort) },
    )

    Reiterreihe {
        Reiter("Chat", offen = reiter == 0, beiDruck = { reiter = 0 })
        // Die Marke nur für das, was eine Antwort verlangt: Anträge und Meldungen.
        Reiter("Mannschaft", offen = reiter == 1, beiDruck = { reiter = 1 }, marke = offen)
        Reiter("Wache", offen = reiter == 2, beiDruck = { reiter = 2 })
    }

    when (reiter) {
        0 -> WachenChat(
            gemeinschaft = gemeinschaft,
            zeilen = sozial.wachenchat,
            meineKennung = meineKennung,
            griffe = griffe,
        )
        1 -> Mannschaft(
            gemeinschaft = gemeinschaft,
            detail = detail,
            meineKennung = meineKennung,
            server = server,
            griffe = griffe,
        )
        else -> WacheTeil(
            gemeinschaft = kopf,
            detail = detail,
            meineKennung = meineKennung,
            konto = konto,
            kreis = kreis,
            schichten = schichten,
            griffe = griffe,
        )
    }

    if (einstellungenOffen) {
        EinstellungenBlende(
            gemeinschaft = kopf,
            landkreise = landkreise,
            server = server,
            laeuft = kreis.laeuft,
            beiSpeichern = { griffe.einstellungen(id, it) },
            beiCodeErneuern = { griffe.codeErneuern(id) },
            beiAufloesen = {
                griffe.aufloesen(id)
                einstellungenOffen = false
            },
            beiSchliessen = { einstellungenOffen = false },
        )
    }

    // Der Dialog schließt erst, wenn der Server den Dienst angenommen hat — so
    // steht ein abgewiesener Termin nicht still verschwunden da.
    if (planenOffen) {
        var gesendet by remember { mutableStateOf(false) }
        LaunchedEffect(kreis.laeuft, termine.size) {
            if (gesendet && !kreis.laeuft) planenOffen = false
        }
        DienstPlanenBlende(
            belegt = termine.size,
            maxTermine = maxTermine,
            laeuft = kreis.laeuft,
            beiPlanen = { titel, wann ->
                griffe.terminAnlegen(id, titel, wann)
                gesendet = true
            },
            beiSchliessen = { planenOffen = false },
        )
    }

    if (aussehenOffen && schatz != null) {
        AussehenBlende(
            gemeinschaft = kopf,
            schatz = schatz,
            laeuft = kreis.laeuft,
            beiSchmuck = { art, stueck -> griffe.schmuck(id, art, stueck) },
            beiFarbe = { griffe.farbe(id, it) },
            beiSchliessen = { aussehenOffen = false },
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

    // Der Aushang — eine Karte mit amberner Kante über dem Verlauf: das, was die
    // Leitung gelesen haben will, bevor jemand in den Chat rollt. Bearbeitet wird
    // er an Ort und Stelle, wie die Pinnwand des Web.
    if (!gemeinschaft.ankuendigung.isNullOrBlank() || (gemeinschaft.darfFuehren && aushangOffen)) {
        Buchkarte(
            titel = if (aushangOffen) "Aushang an der Pinnwand" else "Aushang",
            akzent = true,
            kopfweg = if (gemeinschaft.darfFuehren && !aushangOffen) {
                { Knopf("Ändern", { aushangOffen = true }, art = Knopfart.Leise, kompakt = true) }
            } else {
                null
            },
        ) {
            if (aushangOffen) {
                var text by remember { mutableStateOf(gemeinschaft.ankuendigung.orEmpty()) }
                Feld(
                    wert = text,
                    beiAenderung = { text = it.take(500) },
                    platzhalter = "Was alle wissen sollen — leer lassen nimmt den Aushang ab.",
                    einzeilig = false,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Anheften", {
                        // Leer nimmt den Aushang ab — wie im Web.
                        griffe.pinnwand(gemeinschaft.id, text.trim().ifBlank { null })
                        aushangOffen = false
                    }, art = Knopfart.Haupt, kompakt = true)
                    Knopf("Abbrechen", { aushangOffen = false }, art = Knopfart.Leise, kompakt = true)
                }
            } else {
                Text(text = gemeinschaft.ankuendigung.orEmpty(), style = Schrift.Normal, color = Farben.Text)
            }
        }
    }

    Buchkarte(
        titel = "Wachenchat",
        kopfweg = if (gemeinschaft.darfFuehren && gemeinschaft.ankuendigung.isNullOrBlank() && !aushangOffen) {
            { Knopf("Aushang anheften", { aushangOffen = true }, art = Knopfart.Leise, kompakt = true) }
        } else {
            null
        },
    ) {
        if (zeilen.isEmpty()) {
            SehrLeise("Noch nichts geschrieben — sag Hallo.")
        } else {
            zeilen.takeLast(60).forEach { zeile ->
                Chatzeile(
                    zeile = zeile,
                    eigene = zeile.von == meineKennung,
                    beiDruck = { zeileOffen = zeile },
                    beiDazustossen = griffe.dazustossen,
                )
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
    }

    zeileOffen?.let { zeile ->
        ZeileBlende(
            zeile = zeile,
            eigene = zeile.von == meineKennung,
            darfFuehren = gemeinschaft.darfFuehren,
            beiMelden = { grund ->
                griffe.zeileMelden(gemeinschaft.id, zeile.nr, grund)
                zeileOffen = null
            },
            beiEntfernen = {
                griffe.zeileEntfernen(gemeinschaft.id, zeile.nr)
                zeileOffen = null
            },
            beiSchliessen = { zeileOffen = null },
        )
    }
}

/**
 * Eine Zeile im Wachenchat, angetippt — melden oder herausnehmen.
 *
 * Melden darf jeder fremde Zeilen, mit einem freiwilligen Grund; herausnehmen die
 * eigene jeder, fremde ab Zugführer — und das im zweiten Tipp, denn der Wortlaut
 * verschwindet danach auch bei denen, die ihn schon gelesen haben.
 */
@Composable
private fun ZeileBlende(
    zeile: Gemeinschaftsnachricht,
    eigene: Boolean,
    darfFuehren: Boolean,
    beiMelden: (String?) -> Unit,
    beiEntfernen: () -> Unit,
    beiSchliessen: () -> Unit,
) {
    var grund by remember { mutableStateOf("") }
    var gefragt by remember { mutableStateOf(false) }

    Blende(titel = zeile.vonName, beiSchliessen = beiSchliessen) {
        Etikett("Zeile im Wachenchat · ${zeitpunkt(zeile.gesendetUm)}")
        Text(text = zeile.text, style = Schrift.Normal, color = Farben.Text)
        if (!eigene) {
            Feld(
                wert = grund,
                beiAenderung = { grund = it.take(200) },
                etikett = "Warum? (freiwillig)",
                platzhalter = "Ein Stichwort genügt",
            )
            Knopf("An die Wachenführung melden", { beiMelden(grund.trim().ifBlank { null }) }, kompakt = true)
            SehrLeise(
                "Die Führung sieht die Zeile und entscheidet. Wer gemeldet hat, erfährt der Verfasser nicht.",
            )
        }
        if (eigene || darfFuehren) {
            if (!gefragt) {
                Knopf(
                    if (eigene) "Zeile zurücknehmen" else "Zeile herausnehmen",
                    { gefragt = true },
                    art = Knopfart.Gefahr,
                    kompakt = true,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Ja, herausnehmen", beiEntfernen, art = Knopfart.Gefahr, kompakt = true)
                    Knopf("Abbrechen", { gefragt = false }, art = Knopfart.Leise, kompakt = true)
                }
            }
            SehrLeise(
                "Der Wortlaut verschwindet danach aus dem Verlauf — auch bei denen, die ihn schon " +
                    "gelesen haben. An seiner Stelle steht ein Vermerk.",
            )
        }
    }
}

@Composable
private fun Chatzeile(
    zeile: Gemeinschaftsnachricht,
    eigene: Boolean,
    beiDruck: () -> Unit,
    beiDazustossen: (String) -> Unit,
) {
    when {
        zeile.entfernt -> SehrLeise("Zeile von ${zeile.vonName} herausgenommen.")
        zeile.art == "System" -> SehrLeise(zeile.text)
        zeile.art == "Clanrunde" -> Kasten(abstandInnen = Abstand.Klein) {
            SehrLeise("${zeile.vonName} hat eine Clanrunde eröffnet — Raum ${zeile.roomCode}.")
            zeile.roomCode?.let { code ->
                Textweg("Dazustoßen", { beiDazustossen(code) })
            }
        }
        // Die eigene Blase rechts, fremde links — dieselbe Sprache wie das
        // Gespräch, damit „von mir" ohne Lesen erkennbar ist.
        else -> Row(modifier = Modifier.fillMaxWidth()) {
            if (eigene) Spacer(Modifier.weight(0.18f))
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .weight(0.82f)
                    .background(
                        if (eigene) Farben.Amber.copy(alpha = 0.13f) else Farben.Flaeche,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable(onClick = beiDruck)
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
            if (!eigene) Spacer(Modifier.weight(0.18f))
        }
    }
}

// --------------------------------------------------------------- Mannschaft

@Composable
private fun ColumnScope.Mannschaft(
    gemeinschaft: Gemeinschaft,
    detail: GemeinschaftDetail?,
    meineKennung: String,
    server: String,
    griffe: WachenGriffe,
) {
    val id = gemeinschaft.id
    var mitgliedOffen by remember { mutableStateOf<Gemeinschaftsmitglied?>(null) }
    var einladenOffen by remember { mutableStateOf(false) }

    // Beitrittsanträge — nur Entscheider bekommen sie überhaupt.
    // Die amberne Kante: Hier liegt etwas, das auf eine Antwort wartet — man sieht
    // es, bevor man die Überschrift liest.
    detail?.antraege?.takeIf { it.isNotEmpty() }?.let { antraege ->
        Buchkarte("Beitrittsanträge", zahl = antraege.size.toString(), akzent = true, dicht = true, abstandInnen = 0.dp) {
        antraege.forEachIndexed { i, antrag ->
            Kartenzeile(letzte = i == antraege.lastIndex) {
                // Der Name führt ins Profil — man will sehen, wen man aufnimmt.
                val weg = antrag.benutzername
                Text(
                    text = antrag.anzeigename,
                    style = Schrift.Normal,
                    color = if (weg != null) Farben.BlauHell else Farben.Text,
                    modifier = if (weg != null) {
                        Modifier.clickable(role = Role.Button) { griffe.profil(weg) }
                    } else {
                        Modifier
                    },
                )
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
    }

    // Gemeldete Zeilen — über der Mannschaft: Aufgaben, die liegen bleiben,
    // wenn man sie erst beim Scrollen findet.
    detail?.meldungen?.takeIf { it.isNotEmpty() }?.let { meldungen ->
        Buchkarte("Gemeldete Zeilen", zahl = meldungen.size.toString(), akzent = true, dicht = true, abstandInnen = 0.dp) {
        meldungen.forEachIndexed { i, meldung ->
            Kartenzeile(letzte = i == meldungen.lastIndex) {
                Text(meldung.verfasserName, style = Schrift.Normal, color = Farben.Text)
                Text("„${meldung.text}“", style = Schrift.Klein, color = Farben.TextLeise)
                SehrLeise(
                    "Gemeldet von ${meldung.melderName}" + (meldung.grund?.let { " · „$it“" } ?: ""),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        if (meldung.entfernt) "Schon heraus" else "Herausnehmen",
                        { griffe.zeileEntfernen(id, meldung.nachrichtNr) },
                        art = Knopfart.Gefahr,
                        aktiv = !meldung.entfernt,
                        kompakt = true,
                    )
                    Knopf("In Ordnung", {
                        griffe.meldungErledigt(id, meldung.nr)
                    }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
        }
    }

    // Mannschaft — sortiert kommt sie vom Server, hier wird nicht umsortiert. Der
    // Zähler gehört in den Kopf: „Wie voll sind wir" ist die erste Frage an die Liste.
    Buchkarte(
        titel = "Mannschaft",
        zahl = "${gemeinschaft.mitglieder} / ${gemeinschaft.maxMitglieder}",
        kopfweg = if (gemeinschaft.darfFuehren) {
            { Knopf("Einladen", { einladenOffen = true }, art = Knopfart.Leise, kompakt = true) }
        } else {
            null
        },
        dicht = true,
        abstandInnen = 0.dp,
    ) {
    val mitglieder = detail?.mitglieder.orEmpty()
    mitglieder.forEachIndexed { i, mitglied ->
        Profilzeile(
            randlos = true,
            modifier = Modifier.zeilenstrich(i == mitglieder.lastIndex),
            kennung = mitglied.kennung,
            anzeigename = mitglied.anzeigename,
            unterzeile = listOfNotNull(
                mitglied.level?.let { "Stufe $it" },
                lage(mitglied.anwesenheit, null).ifBlank { null },
            ).joinToString(" · ").ifBlank { null },
            wappen = mitglied.wappen ?: "Keines",
            wappenfarbe = mitglied.wappenfarbe,
            bildAdresse = bildweg(server, mitglied.profilbild),
            premium = mitglied.premium == true,
            teammitglied = mitglied.teammitglied == true,
            imDienst = mitglied.anwesenheit != null,
            beiDruck = { mitgliedOffen = mitglied },
            hinten = {
                Marke(
                    rollenname(mitglied.rolle) ?: mitglied.rolle,
                    farbe = if (mitglied.rolle == "Leitung") Farben.Amber else Farben.TextLeise,
                )
            },
        )
    }
    }

    // Wer fährt am meisten — eine Rangliste wie die des Dienstbuchs: Gold, Silber,
    // Bronze für die ersten drei, die eigene Zeile in Amber. Wer seine Statistik
    // nicht teilt, fehlt hier.
    detail?.beitraege?.takeIf { it.isNotEmpty() }?.let { beitraege ->
        Buchkarte("Wer fährt am meisten", dicht = true, abstandInnen = 0.dp) {
            beitraege.forEachIndexed { i, b ->
                Rangzeile(
                    platz = i + 1,
                    wert = zahl(b.punkte),
                    letzte = i == beitraege.lastIndex,
                    eigen = b.kennung == meineKennung,
                    beiDruck = b.benutzername?.let { weg -> { griffe.profil(weg) } },
                ) {
                    Kontoname(name = b.anzeigename, premium = b.premium, teammitglied = b.teammitglied)
                    SehrLeise(
                        "${b.schichten} ${if (b.schichten == 1) "Schicht" else "Schichten"} · " +
                            "${b.einsaetze} Einsätze",
                        mono = true,
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------- Blenden
    mitgliedOffen?.let { mitglied ->
        MitgliedBlende(
            mitglied = mitglied,
            gemeinschaft = gemeinschaft,
            selbst = mitglied.kennung == meineKennung,
            griffe = griffe,
            beiSchliessen = { mitgliedOffen = null },
        )
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
}

/**
 * Ein Mitglied, angetippt — wer es ist, seit wann, und was man mit ihm darf.
 *
 * Sich selbst sieht man hier mit dem Austritt; die Leitung sieht Befördern,
 * Übergeben und Entfernen; ein Zugführer führt nur die Mitglieder. Der Weg ins
 * Profil steht bei allen.
 */
@Composable
private fun MitgliedBlende(
    mitglied: Gemeinschaftsmitglied,
    gemeinschaft: Gemeinschaft,
    selbst: Boolean,
    griffe: WachenGriffe,
    beiSchliessen: () -> Unit,
) {
    val id = gemeinschaft.id
    var gefragt by remember { mutableStateOf(false) }
    // Ein Zugführer führt nur Mitglieder; andere Zugführer und die Leitung
    // ändert allein die Leitung.
    val darfVerwalten = !selbst && (
        gemeinschaft.istLeitung ||
            (gemeinschaft.eigeneRolle == "Zugfuehrer" && mitglied.rolle == "Mitglied")
        )

    Blende(titel = mitglied.anzeigename, beiSchliessen = beiSchliessen) {
        Etikett(rollenname(mitglied.rolle) ?: mitglied.rolle)
        if (mitglied.benutzername.isNotBlank()) SehrLeise(mitglied.benutzername, mono = true)
        mitglied.rang?.let { SehrLeise("$it · Stufe ${mitglied.level ?: 0}") }
        SehrLeise("Auf der Wache seit ${tag(mitglied.beigetretenUm)}")
        Text(
            text = lage(mitglied.anwesenheit, null).ifBlank { "Keine Angabe" },
            style = Schrift.Klein,
            color = if (mitglied.anwesenheit != null) Farben.GruenHell else Farben.TextLeise,
        )
        if (mitglied.benutzername.isNotBlank()) {
            Knopf("Profil ansehen", {
                beiSchliessen()
                griffe.profil(mitglied.benutzername)
            }, kompakt = true)
        }

        if (selbst) {
            SehrLeise(
                "Ein Konto gehört höchstens einer Wachengemeinschaft an. Zum Wechseln trittst du hier " +
                    "aus. Bist du die Leitung, rückt der Dienstälteste nach; bist du die letzte Person, " +
                    "löst sich die Gemeinschaft auf.",
            )
            if (!gefragt) {
                Knopf("Gemeinschaft verlassen", { gefragt = true }, art = Knopfart.Leise, kompakt = true)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Ja, austreten", {
                        griffe.austreten(id)
                        beiSchliessen()
                    }, art = Knopfart.Gefahr, kompakt = true)
                    Knopf("Abbrechen", { gefragt = false }, art = Knopfart.Leise, kompakt = true)
                }
            }
        } else if (darfVerwalten) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                when (mitglied.rolle) {
                    "Mitglied" -> Knopf("Zum Zugführer befördern", {
                        griffe.rolleSetzen(id, mitglied.kennung, "Zugfuehrer")
                        beiSchliessen()
                    }, kompakt = true)
                    "Zugfuehrer" -> Knopf("Zum Mitglied zurückstufen", {
                        griffe.rolleSetzen(id, mitglied.kennung, "Mitglied")
                        beiSchliessen()
                    }, art = Knopfart.Leise, kompakt = true)
                }
                if (gemeinschaft.istLeitung) {
                    Knopf("Leitung übergeben", {
                        griffe.leitungUebergeben(id, mitglied.kennung)
                        beiSchliessen()
                    }, art = Knopfart.Leise, kompakt = true)
                }
            }
            SehrLeise(
                if (gemeinschaft.istLeitung) {
                    "Wer die Leitung übernimmt, kann alles: Rollen vergeben, Mitglieder entfernen und " +
                        "die Gemeinschaft auflösen. Du selbst wirst dabei Zugführer."
                } else {
                    "Als Zugführer führst du die Mitglieder deiner Wache. Andere Zugführer und die " +
                        "Leitung ändert nur die Leitung selbst."
                },
            )
            if (!gefragt) {
                Knopf("Aus der Gemeinschaft entfernen", { gefragt = true }, art = Knopfart.Gefahr, kompakt = true)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Ja, entfernen", {
                        griffe.mitgliedEntfernen(id, mitglied.kennung)
                        beiSchliessen()
                    }, art = Knopfart.Gefahr, kompakt = true)
                    Knopf("Abbrechen", { gefragt = false }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
    }
}

// -------------------------------------------------------------------- Wache

@Composable
private fun ColumnScope.WacheTeil(
    gemeinschaft: Gemeinschaft,
    detail: GemeinschaftDetail?,
    meineKennung: String,
    konto: Konto?,
    kreis: Kreisstand,
    schichten: List<Schicht>,
    griffe: WachenGriffe,
) {
    val id = gemeinschaft.id
    var austrittGefragt by remember { mutableStateOf(false) }

    LaunchedEffect(id) { griffe.laufbahn() }

    // Der Wachentag — das Ziel, auf das die Laufbahn zuläuft.
    detail?.statistik?.let { s ->
        WachenTag(
            gemeinschaft = gemeinschaft,
            stufe = s.stufe,
            name = konto?.anzeigename ?: "Du",
            premium = konto?.premiumAktiv == true,
            laeuft = kreis.laeuft,
            beiSpeichern = { griffe.tagSetzen(id, it) },
        )
    }

    // Der Dienstplan — mit Deckel: „2 / 3" sagt auch, ob noch einer hineinpasst.
    // Eingetragen wird oben im Band „Gemeinsam fahren".
    val termine = detail?.termine?.filter { !it.abgesagt }.orEmpty()
    Buchkarte(
        titel = "Dienstplan",
        zahl = if (termine.isNotEmpty()) {
            "${termine.size} / ${detail?.statistik?.maxTermine?.takeIf { it > 0 } ?: 1}"
        } else {
            null
        },
        dicht = true,
        abstandInnen = 0.dp,
    ) {
        Dienstplan(
            termine = termine,
            ich = meineKennung,
            darfPlanen = gemeinschaft.darfFuehren,
            beiAntworten = { nr, antwort -> griffe.terminAntworten(id, nr, antwort) },
            beiAbsagen = { griffe.terminAbsagen(id, it) },
            beiBeitreten = griffe.dazustossen,
        )
    }

    // Logbuch — die gefahrenen Clanrunden; eine laufende kann man noch erreichen.
    detail?.logbuch?.takeIf { it.isNotEmpty() }?.let { logbuch ->
        Buchkarte("Logbuch", zahl = logbuch.size.toString(), dicht = true, abstandInnen = 0.dp) {
            logbuch.forEachIndexed { i, r ->
                Kartenzeile(letzte = i == logbuch.lastIndex) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    tag(r.gestartetUm) + (r.landkreis?.let { " · $it" } ?: ""),
                                    style = Schrift.MonoKlein,
                                    color = Farben.Text,
                                )
                                // Grün und vorn dabei: die eine Zeile, an der man noch
                                // teilnehmen kann.
                                if (r.beendetUm == null) Marke("läuft", farbe = Farben.GruenHell)
                            }
                            SehrLeise(
                                if (r.beendetUm != null) {
                                    "${r.teilnehmer} dabei · ${r.einsaetze} Einsätze · ${zahl(r.punkte)} Punkte"
                                } else {
                                    "läuft gerade · eröffnet von ${r.vonName ?: "—"}"
                                },
                            )
                        }
                        if (r.beendetUm == null) {
                            Knopf("Dazustoßen", { griffe.dazustossen(r.roomCode) }, art = Knopfart.Leise, kompakt = true)
                        } else {
                            SehrLeise(r.roomCode, mono = true)
                        }
                    }
                }
            }
        }
    }

    // Mitgliedschaft — der Austritt schließt die Wache-Spalte ab, mit Signalkante
    // statt eines roten Kastens und der Rückfrage im zweiten Druck.
    Buchkarte("Mitgliedschaft", kante = Farben.SignalHell) {
        if (!austrittGefragt) {
            SehrLeise("Du bist in dieser Wachengemeinschaft. Ein Konto gehört immer nur einer an.")
            Knopf("Gemeinschaft verlassen", { austrittGefragt = true }, art = Knopfart.Leise, aktiv = !kreis.laeuft)
        } else {
            Text(
                "Wirklich austreten? Bist du die Leitung, rückt der Dienstälteste nach; bist du die " +
                    "letzte Person, löst sich die Gemeinschaft auf.",
                style = Schrift.Klein,
                color = Farben.Text,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Ja, austreten", { griffe.austreten(id) }, art = Knopfart.Alarm, aktiv = !kreis.laeuft)
                Knopf("Abbrechen", { austrittGefragt = false }, art = Knopfart.Leise)
            }
        }
    }

    // Die Laufbahn als Band — Verwaltung und Fernziel, kein Tagesgeschäft.
    detail?.statistik?.let { WachenLaufbahn(it, kreis.laufbahn) }
}

// ------------------------------------------------------------ Ohne Wache

/** Wie ein Aufnahmemodus heißt, wenn ein Mensch ihn liest. */
private fun modustext(modus: String): String = when (modus) {
    "Einladung" -> "Nur auf Einladung"
    "Antrag" -> "Auf Antrag"
    else -> "Offen für alle"
}

@Composable
private fun ColumnScope.WachenSuche(
    offene: List<Gemeinschaft>,
    antraege: List<Gemeinschaftsantrag>,
    kreis: Kreisstand,
    landkreise: List<Landkreis>,
    beiAntraege: () -> Unit,
    griffe: WachenGriffe,
    level: Int = 1,
) {
    LaunchedEffect(Unit) {
        beiAntraege()
        if (!kreis.oeffentliche.geladen) griffe.suchen(kreis.filter)
    }
    var code by remember { mutableStateOf("") }
    var gruendenOffen by remember { mutableStateOf(false) }
    var bewerbungFuer by remember { mutableStateOf<String?>(null) }
    var kreiswahl by remember { mutableStateOf(false) }
    var suche by rememberSaveable { mutableStateOf(kreis.filter.suche) }
    val filter = kreis.filter

    Seitenkopf(titel = "Eine Wache finden", unterzeile = "Wachengemeinschaften")
    // Ein Satz, nicht drei: Was vorab gebraucht wird, ist „eine feste Mannschaft,
    // und man ist in höchstens einer". Der Rest erklärt sich, sobald man drin ist.
    SehrLeise("Eine feste Mannschaft mit eigenem Chat — jedes Konto gehört höchstens einer an.")
    Textweg("Rangliste der Wachen", griffe.rangliste)

    Meldungszeile(kreis.meldung, kreis.hinweis, griffe.meldungWeg)

    // Offene Einladungen zuerst — die kürzeste Tür, mit amberner Kante.
    val einladungen = antraege.filter { it.richtung == "Einladung" && it.stand == "Offen" }
    if (einladungen.isNotEmpty()) {
        Buchkarte("Du bist eingeladen", zahl = einladungen.size.toString(), akzent = true, dicht = true, abstandInnen = 0.dp) {
            einladungen.forEachIndexed { i, einladung ->
                Antragszeile(
                    gemeinschaftId = einladung.gemeinschaftId,
                    name = einladung.gemeinschaftName,
                    unter = einladung.vonName?.let { "von $it" },
                    letzte = i == einladungen.lastIndex,
                ) {
                    Knopf("Beitreten", {
                        griffe.einladungAnnehmen(einladung.nr)
                    }, art = Knopfart.Haupt, aktiv = !kreis.laeuft, kompakt = true)
                    Knopf("Ablehnen", {
                        griffe.bewerbungZurueckziehen(einladung.nr)
                    }, art = Knopfart.Leise, aktiv = !kreis.laeuft, kompakt = true)
                }
            }
        }
    }

    // Zwei Wege hinein — mit Code oder selbst gegründet.
    Buchkarte("Mit Beitrittscode") {
        SehrLeise("Sechs Zeichen, wie ein Raumcode — vom Zugführer oder von der Leitung.")
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
        ) {
            Feld(
                wert = code,
                beiAenderung = { code = it.take(6).uppercase() },
                platzhalter = "ABC123",
                stil = Schrift.MonoNormal,
                modifier = Modifier.width(132.dp),
            )
            // Neben dem Codefeld nimmt der Knopf den Rest der Zeile (`mobil.css`, 2).
            Knopf(
                "Beitreten",
                { griffe.beitreten(code) },
                art = Knopfart.Haupt,
                aktiv = !kreis.laeuft && code.length == 6,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // Wie weit es noch ist, steht an der Karte und nicht erst in der Fehlermeldung
    // nach dem Absenden.
    val darfGruenden = level >= GRUENDEN_AB_LEVEL
    Buchkarte("Selbst gründen") {
        SehrLeise("Du wirst die Leitung und bestimmst, wer dazukommt und wie.")
        Text(
            if (darfGruenden) {
                "✓ Ab Level $GRUENDEN_AB_LEVEL — du bist Level $level"
            } else {
                "Ab Level $GRUENDEN_AB_LEVEL — dir fehlen noch ${GRUENDEN_AB_LEVEL - level}"
            },
            style = Schrift.Klein,
            color = if (darfGruenden) Farben.GruenHell else Farben.TextLeise,
        )
        Knopf("Gemeinschaft gründen", {
            griffe.landkreisvorschlag()
            gruendenOffen = true
        }, aktiv = darfGruenden, breit = true)
    }

    // Die Filter stehen offen da: Die Vorauswahl „Nur, wo ich aufgenommen werde"
    // verändert die Liste — wer das nicht sieht, hält die kurze Liste für alles.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Bottom,
    ) {
        Feld(
            wert = suche,
            beiAenderung = { suche = it.take(40) },
            platzhalter = "⌕  Name oder Stichwort",
            modifier = Modifier.weight(1f),
        )
        Knopf("Suchen", { griffe.suchen(filter.copy(suche = suche)) }, aktiv = !kreis.oeffentliche.laedt, kompakt = true)
    }
    Wahlfeld(
        etikett = "Landkreis",
        wert = landkreise.firstOrNull { it.id == filter.landkreisId }?.name,
        platzhalter = "Alle",
        beiDruck = { kreiswahl = true },
    )
    Etikett("Aufnahme")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
    ) {
        listOf(null to "Egal", "Offen" to "Offen für alle", "Antrag" to "Auf Antrag", "Einladung" to "Nur auf Einladung")
            .forEach { (wert, text) ->
                Pille(text, an = filter.modus == wert, beiDruck = { griffe.suchen(filter.copy(modus = wert, suche = suche)) })
            }
    }
    Hakenzeile(
        "Nur, wo ich aufgenommen werde",
        an = filter.nurPassende,
        beiWechsel = { griffe.suchen(filter.copy(nurPassende = it, suche = suche)) },
    )
    Hakenzeile(
        "Nur mit freien Plätzen",
        an = filter.nurFreie,
        beiWechsel = { griffe.suchen(filter.copy(nurFreie = it, suche = suche)) },
    )
    if (filter.eingegrenzt) {
        Knopf("Filter zurücksetzen", {
            suche = ""
            griffe.suchen(Wachenfilter())
        }, art = Knopfart.Leise, aktiv = !kreis.laeuft, kompakt = true)
    }

    val liste = kreis.oeffentliche.inhalt ?: offene
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Ueberschrift("Öffentliche Gemeinschaften")
        if (!kreis.oeffentliche.laedt) SehrLeise(liste.size.toString(), mono = true)
    }
    if (liste.isEmpty() && !kreis.oeffentliche.laedt) {
        if (filter.eingegrenzt) {
            Leerhinweis(
                "Zu dieser Auswahl passt gerade keine Wache. Nimm einen Filter heraus — oder gründe selbst eine.",
            ) {
                Knopf("Filter zurücksetzen", {
                    suche = ""
                    griffe.suchen(Wachenfilter())
                }, aktiv = !kreis.laeuft)
            }
        } else {
            Leerhinweis("Gerade ist keine öffentlich gelistet. Der Beitrittscode führt trotzdem überall hinein.")
        }
    } else {
        liste.forEach { g ->
            Wachenkarte(
                g = g,
                level = level,
                laeuft = kreis.laeuft,
                bewerbungOffen = bewerbungFuer == g.id,
                beiBewerbungOeffnen = { bewerbungFuer = if (it) g.id else null },
                griffe = griffe,
            )
        }
    }

    // Eigene offene Bewerbungen — mit dem Rückzieher.
    val bewerbungen = antraege.filter { it.richtung == "Bewerbung" && it.stand == "Offen" }
    if (bewerbungen.isNotEmpty()) {
        Buchkarte("Deine offenen Bewerbungen", zahl = bewerbungen.size.toString(), dicht = true, abstandInnen = 0.dp) {
            bewerbungen.forEachIndexed { i, b ->
                Antragszeile(
                    gemeinschaftId = b.gemeinschaftId,
                    name = b.gemeinschaftName,
                    unter = "Wartet auf eine Antwort.",
                    letzte = i == bewerbungen.lastIndex,
                ) {
                    Knopf("Zurückziehen", {
                        griffe.bewerbungZurueckziehen(b.nr)
                    }, art = Knopfart.Leise, aktiv = !kreis.laeuft, kompakt = true)
                }
            }
        }
    }

    if (kreiswahl) {
        Landkreiswahl(
            landkreise = landkreise,
            gewaehlt = filter.landkreisId,
            mitKeinem = "Alle",
            beiWahl = {
                kreiswahl = false
                griffe.suchen(filter.copy(landkreisId = it, suche = suche))
            },
            beiSchliessen = { kreiswahl = false },
        )
    }

    if (gruendenOffen) {
        GruendenBlende(
            landkreise = landkreise,
            vorschlag = kreis.landkreisvorschlag?.landkreisId,
            laeuft = kreis.laeuft,
            beiGruenden = { name, text, kreisId ->
                val k = landkreise.firstOrNull { it.id == kreisId }
                griffe.gruenden(name, text, k?.id, k?.name)
                gruendenOffen = false
            },
            beiSchliessen = { gruendenOffen = false },
        )
    }
}

/**
 * Eine Gemeinschaft gründen — mit dem Landkreis, in dem das Konto ohnehin am
 * häufigsten fährt, schon vorgewählt. Ein leeres Formular wäre eine Frage, deren
 * Antwort der Server längst kennt.
 */
@Composable
private fun GruendenBlende(
    landkreise: List<Landkreis>,
    vorschlag: String?,
    laeuft: Boolean,
    beiGruenden: (String, String?, String?) -> Unit,
    beiSchliessen: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var beschreibung by remember { mutableStateOf("") }
    var landkreisId by remember { mutableStateOf<String?>(null) }
    var angefasst by remember { mutableStateOf(false) }
    var kreiswahl by remember { mutableStateOf(false) }
    // Der Vorschlag kommt womöglich erst nach dem Öffnen an — dann gilt er, solange
    // niemand selbst gewählt hat.
    val gewaehlt = if (angefasst) landkreisId else landkreisId ?: vorschlag

    Blende(
        titel = "Gemeinschaft gründen",
        beiSchliessen = beiSchliessen,
        fuss = {
            Knopf("Gemeinschaft gründen", {
                beiGruenden(name.trim(), beschreibung.trim().ifBlank { null }, gewaehlt)
            }, art = Knopfart.Haupt, aktiv = !laeuft && name.isNotBlank(), kompakt = true)
        },
    ) {
        Feld(
            wert = name,
            beiAenderung = { name = it.take(40) },
            etikett = "Name",
            platzhalter = "z. B. Wache Nord",
        )
        Feld(
            wert = beschreibung,
            beiAenderung = { beschreibung = it.take(300) },
            etikett = "Beschreibung (freiwillig)",
            platzhalter = "Wann fahrt ihr, wen sucht ihr?",
            einzeilig = false,
        )
        Wahlfeld(
            etikett = "Landkreis",
            wert = landkreise.firstOrNull { it.id == gewaehlt }?.name,
            platzhalter = "Noch keiner",
            beiDruck = { kreiswahl = true },
        )
        SehrLeise("Hier laufen später eure Clanrunden. Änderbar bleibt er jederzeit.")
    }

    if (kreiswahl) {
        Landkreiswahl(
            landkreise = landkreise,
            gewaehlt = gewaehlt,
            mitKeinem = "Noch keiner",
            beiWahl = {
                landkreisId = it
                angefasst = true
                kreiswahl = false
            },
            beiSchliessen = { kreiswahl = false },
        )
    }
}

/** Wie eine Rolle heißt, wenn ein Mensch sie liest. */
private fun rollenname(rolle: String?): String? = when (rolle) {
    "Mitglied" -> "Mitglied"
    "Zugfuehrer" -> "Zugführer"
    "Leitung" -> "Leitung"
    else -> null
}
