package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Sozialstand
import de.pagerspass.pagerspass.mobil.Wachendaten
import de.pagerspass.pagerspass.mobil.Wachenspeicher
import de.pagerspass.pagerspass.mobil.Wachenstand
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.GemeinschaftDetail
import de.pagerspass.pagerspass.netz.Gemeinschaftsantrag
import de.pagerspass.pagerspass.netz.Gemeinschaftsmeldung
import de.pagerspass.pagerspass.netz.Gemeinschaftsmitglied
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Wachenbeitrag
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.schmuck.profilbildAdresse
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Wachengemeinschaft — übertragen aus `GemeinschaftView.vue` und
 * `GemeinschaftenView.vue` in die Handyform.
 *
 * <b>Drei Bereiche, wie im Web seit 5.0.0.58:</b> Chat (was wird geschrieben),
 * Mannschaft (wer ist dabei) und Wache (wohin geht sie). Vorher trug der zweite
 * Reiter alles, was nicht Chat war — vier Bildschirme Rollweg, in denen die
 * Mannschaft irgendwo in der Mitte lag.
 *
 * <b>Was welche Rolle darf, entscheidet der Server</b> — die App blendet nur
 * aus, was ohnehin abgewiesen würde: Führen ab Zugführer, Verwalten gegen
 * einen echt niedrigeren Rang, Einstellungen nur als Leitung.
 */
class WachenGriffe(
    /** Der Speicher mit allen Wachen-Aktionen — `mobil/Wachen.kt`. */
    val speicher: Wachenspeicher,
    /** Den Chat der Wache über den Sozial-Hub öffnen. */
    val chatOeffnen: (String) -> Unit = {},
    val chatSenden: (String, String) -> Unit = { _, _ -> },
    /** Ob die Wachenseite zu sehen ist — nur dann gilt eine neue Zeile als gelesen. */
    val seiteSichtbar: (Boolean) -> Unit = {},
    /** In eine laufende Runde — derselbe Weg wie vom Startbildschirm. */
    val runde: (String) -> Unit = {},
    /** Eine Clanrunde eröffnen und hinein. */
    val clanrunde: (String) -> Unit = {},
    val katalog: () -> Unit = {},
    val freundeLaden: () -> Unit = {},
    val zurRangliste: () -> Unit = {},
    val zumShop: () -> Unit = {},
    val zurWache: () -> Unit = {},
    /** Ins Profil eines Mitglieds (Benutzername) — `null`, solange es den Weg nicht gibt. */
    val profil: ((String) -> Unit)? = null,
)

/**
 * Die Wache-Seite der Tableiste: die eigene Wache — oder, wer in keiner ist,
 * die Suche. Dieselbe Weiche wie im Web.
 *
 * @param code Ein Beitrittscode aus einem Link (`/gemeinschaften?code=…`). Er
 *   landet im Feld und nicht in der Gemeinschaft: Beigetreten wird erst auf den
 *   Knopf — ein fremder Link bringt niemanden ungefragt irgendwohin.
 */
@Composable
fun WachenSeite(
    griffe: WachenGriffe,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    wache: Bereichsstand<Wachendaten> = Bereichsstand(),
    detail: Bereichsstand<GemeinschaftDetail?> = Bereichsstand(),
    antraege: Bereichsstand<List<Gemeinschaftsantrag>> = Bereichsstand(),
    wachen: Wachenstand = Wachenstand(),
    sozial: Sozialstand = Sozialstand(),
    konto: Konto? = null,
    freunde: List<Freund> = emptyList(),
    landkreise: List<Landkreis> = emptyList(),
    server: String = "",
    code: String? = null,
) {
    LaunchedEffect(Unit) {
        griffe.speicher.anmelden()
        griffe.katalog()
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        Bereich(
            laedt = wache.laedt,
            fehler = wache.fehler,
            inhalt = wache.inhalt,
            beiErneut = { griffe.speicher.anmelden() },
        ) { daten ->
            val eigene = daten.eigene
            if (eigene != null) {
                EigeneWache(
                    eigene = eigene,
                    detail = detail.inhalt?.takeIf { it.gemeinschaft.id == eigene.id },
                    wachen = wachen,
                    sozial = sozial,
                    konto = konto,
                    freunde = freunde,
                    landkreise = landkreise,
                    server = server,
                    griffe = griffe,
                )
            } else {
                WachenSuche(
                    antraege = antraege.inhalt.orEmpty(),
                    wachen = wachen,
                    landkreise = landkreise,
                    code = code,
                    griffe = griffe,
                )
            }
        }
    }
}

// ------------------------------------------------------------ Eigene Wache

@Composable
private fun ColumnScope.EigeneWache(
    eigene: Gemeinschaft,
    detail: GemeinschaftDetail?,
    wachen: Wachenstand,
    sozial: Sozialstand,
    konto: Konto?,
    freunde: List<Freund>,
    landkreise: List<Landkreis>,
    server: String,
    griffe: WachenGriffe,
) {
    val speicher = griffe.speicher
    val id = eigene.id
    val ich = konto?.kennung.orEmpty()
    // Der Kopf aus dem Detail, solange es da ist — es ist der jüngere Stand nach
    // einer Aktion; sonst die Kurzform.
    val kopf = detail?.gemeinschaft ?: eigene
    val statistik = detail?.statistik
    val schatz = detail?.schatz
    val darfFuehren = kopf.darfFuehren
    val ton = wachenton(kopf)
    val beiname = beinameVon(schatz, kopf.beiname)
    val mitglieder = detail?.mitglieder.orEmpty()
    val bilder = remember(mitglieder) { mitglieder.associateBy { it.kennung } }
    val eingang = detail?.antraege.orEmpty()
    val meldungen = detail?.meldungen.orEmpty()
    val aufgaben = eingang.size + meldungen.size
    val verlauf = if (sozial.wachenchatId == id) sozial.wachenchat else emptyList()

    var bereich by rememberSaveable { mutableStateOf("chat") }
    var einstellungenOffen by remember { mutableStateOf(false) }
    var einladenOffen by remember { mutableStateOf(false) }
    var anpassenOffen by remember { mutableStateOf(false) }
    var aushangOffen by remember { mutableStateOf(false) }
    var aushang by remember { mutableStateOf("") }
    var offenesMitglied by remember { mutableStateOf<String?>(null) }
    var offeneZeile by remember { mutableStateOf<Long?>(null) }
    var austrittGefragt by remember { mutableStateOf(false) }

    // Der Chat hängt an der Id — ein Wechsel der Kurzform öffnet ihn nicht neu.
    LaunchedEffect(id) {
        griffe.chatOeffnen(id)
        speicher.laufbahnLaden()
    }
    // Das Detail folgt jeder Änderung der Kurzform: Tritt jemand bei, wird befördert
    // oder ein Aushang geändert, kommt über den Hub die neue Kurzform — und erst
    // dieser Effekt holt die Mannschaft nach.
    LaunchedEffect(eigene) { speicher.oeffnen(id) }
    DisposableEffect(id) {
        griffe.seiteSichtbar(true)
        onDispose {
            griffe.seiteSichtbar(false)
            speicher.schliessen()
        }
    }

    // ------------------------------------------------------------------ Kopf
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .flaeche()
            .kopfband(kopf.kopfmuster, ton)
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Gross),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wachenemblem(
                name = kopf.name,
                ton = ton,
                zeichen = kopf.emblemzeichen,
                rahmen = kopf.emblemrahmen,
                groesse = 64.dp,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier.weight(1f),
            ) {
                // Der Beiname steht an der Stelle des Etiketts und ersetzt es.
                if (beiname != null) {
                    Text(
                        text = beiname.uppercase(),
                        style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Amber,
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
                    if (kopf.tag.isNotEmpty()) Wachentagplakette(kopf.tag, gross = true)
                    Text(
                        text = kopf.name,
                        style = Schrift.Titel,
                        color = Farben.Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SehrLeise(
                    listOfNotNull(
                        "${kopf.mitglieder} von ${kopf.maxMitglieder} Mitgliedern",
                        kopf.landkreis?.ifBlank { null },
                        kopf.eigeneRolle?.let { "du bist ${wachenrolle(it)}" },
                    ).joinToString(" · "),
                )
                if (!kopf.beschreibung.isNullOrBlank()) Leise(kopf.beschreibung)
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        ) {
            if (darfFuehren) {
                Knopf("Clanrunde", { griffe.clanrunde(id) }, art = Knopfart.Haupt, aktiv = !wachen.laeuft, kompakt = true)
            }
            // Anpassen darf, wer auch kauft — Leitung und Zugführer.
            if (darfFuehren && schatz != null) {
                Knopf("Aussehen", { anpassenOffen = true }, art = Knopfart.Leise, kompakt = true)
            }
            if (kopf.istLeitung) {
                Knopf("Einstellungen", { einstellungenOffen = true }, art = Knopfart.Leise, kompakt = true)
            }
            Knopf("Rangliste", griffe.zurRangliste, art = Knopfart.Leise, kompakt = true)
            Knopf("Shop", griffe.zumShop, art = Knopfart.Leise, kompakt = true)
        }

        statistik?.let { WachenStufe(it) }
    }

    wachen.meldung?.let { Meldungszeile(it) }

    // Die Clanrunde, zu der gerade eingeladen wird — ob es sie noch gibt,
    // entscheidet der Server.
    kopf.laufendeRundeCode?.let { code ->
        Kasten(marke = true, abstandInnen = Abstand.Klein) {
            Text("Eine Clanrunde läuft unter $code.", style = Schrift.Normal, color = Farben.Text)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                Knopf("Dazustoßen", { griffe.runde(code) }, art = Knopfart.Haupt, kompakt = true)
                // Die Runde läuft dann weiter — sie lädt nur niemanden mehr dazu ein.
                if (kopf.darfRundeSchliessen) {
                    Knopf(
                        "Nicht mehr anzeigen",
                        { speicher.rundeSchliessen(id) },
                        art = Knopfart.Leise,
                        aktiv = !wachen.laeuft,
                        kompakt = true,
                    )
                }
            }
        }
    }

    Reiterreihe {
        Reiter("Chat", offen = bereich == "chat", beiDruck = { bereich = "chat" })
        // Die Marke nur für das, was eine Antwort verlangt: Anträge und Meldungen.
        Reiter("Mannschaft", offen = bereich == "mannschaft", beiDruck = { bereich = "mannschaft" }, marke = aufgaben)
        Reiter("Wache", offen = bereich == "wache", beiDruck = { bereich = "wache" })
    }

    when (bereich) {
        "mannschaft" -> Mannschaftsbereich(
            kopf = kopf,
            eingang = eingang,
            meldungen = meldungen,
            mitglieder = mitglieder,
            beitraege = detail?.beitraege.orEmpty(),
            bilder = bilder,
            ich = ich,
            server = server,
            laeuft = wachen.laeuft,
            speicher = speicher,
            beiMitglied = { offenesMitglied = it },
            beiEinladen = {
                griffe.freundeLaden()
                einladenOffen = true
            },
        )

        "wache" -> {
            if (statistik != null) {
                WachenTag(
                    gemeinschaft = kopf,
                    stufe = statistik.stufe,
                    darfFuehren = darfFuehren,
                    laeuft = wachen.laeuft,
                    eigenerName = konto?.anzeigename ?: "Du",
                    premium = konto?.premiumAktiv == true,
                    beiSpeichern = { speicher.tagSetzen(id, it) },
                )
            }

            val termine = detail?.termine?.filter { !it.abgesagt }.orEmpty()
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Ueberschrift("Dienstplan")
                if (termine.isNotEmpty()) Marke(termine.size.toString())
            }
            Dienstplan(
                termine = termine,
                ich = ich,
                darfPlanen = darfFuehren,
                maxTermine = statistik?.maxTermine?.takeIf { it > 0 } ?: 1,
                beiPlanen = { titel, wann -> speicher.dienstPlanen(id, titel, wann) },
                beiAntwort = { nr, antwort -> speicher.dienstBeantworten(nr, antwort) },
                beiAbsagen = { speicher.dienstAbsagen(it) },
                beiBeitreten = griffe.runde,
            )

            val logbuch = detail?.logbuch.orEmpty()
            if (logbuch.isNotEmpty()) {
                Ueberschrift("Logbuch")
                Logbuch(logbuch, beiBeitreten = griffe.runde)
            }

            // Der Austritt schließt die Spalte ab — früher hing er allein am eigenen
            // Eintrag in der Mannschaftsliste, und darauf kam man nicht.
            Ueberschrift("Mitgliedschaft")
            Kasten(abstandInnen = Abstand.Klein) {
                if (!austrittGefragt) {
                    SehrLeise("Du bist in dieser Wachengemeinschaft. Ein Konto gehört immer nur einer an.")
                    Knopf(
                        "Gemeinschaft verlassen",
                        { austrittGefragt = true },
                        art = Knopfart.Leise,
                        aktiv = !wachen.laeuft,
                        kompakt = true,
                    )
                } else {
                    Text(
                        text = "Wirklich austreten? Bist du die Leitung, rückt der Dienstälteste nach; bist du " +
                            "die letzte Person, löst sich die Gemeinschaft auf.",
                        style = Schrift.Klein,
                        color = Farben.Text,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            "Ja, austreten",
                            { speicher.verlassen(id) { austrittGefragt = false } },
                            art = Knopfart.Alarm,
                            aktiv = !wachen.laeuft,
                            kompakt = true,
                        )
                        Knopf("Abbrechen", { austrittGefragt = false }, art = Knopfart.Leise, kompakt = true)
                    }
                }
            }

            // Die Laufbahn — Verwaltung und Fernziel, kein Tagesgeschäft.
            statistik?.let { WachenLaufbahn(it, wachen.laufbahn) }
        }

        else -> {
            // Der Aushang steht über dem Chat: Er ist das, was die Leitung gelesen
            // haben will, bevor jemand in den Verlauf rollt.
            if (!kopf.ankuendigung.isNullOrBlank() || (darfFuehren && aushangOffen)) {
                Kasten(abstandInnen = Abstand.Klein) {
                    if (aushangOffen) {
                        Feld(
                            wert = aushang,
                            beiAenderung = { aushang = it.take(500) },
                            etikett = "Aushang an der Pinnwand",
                            platzhalter = "Was alle wissen sollen — leer lassen nimmt den Aushang ab.",
                            einzeilig = false,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf(
                                "Anheften",
                                { speicher.anheften(id, aushang) { aushangOffen = false } },
                                art = Knopfart.Haupt,
                                aktiv = !wachen.laeuft,
                                kompakt = true,
                            )
                            Knopf("Abbrechen", { aushangOffen = false }, art = Knopfart.Leise, kompakt = true)
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Etikett("Aushang")
                                Text(kopf.ankuendigung.orEmpty(), style = Schrift.Normal, color = Farben.Text)
                            }
                            if (darfFuehren) {
                                Knopf(
                                    "Ändern",
                                    {
                                        aushang = kopf.ankuendigung.orEmpty()
                                        aushangOffen = true
                                    },
                                    art = Knopfart.Leise,
                                    kompakt = true,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Ueberschrift("Wachenchat", Modifier.weight(1f))
                if (darfFuehren && kopf.ankuendigung.isNullOrBlank() && !aushangOffen) {
                    Knopf(
                        "Aushang anheften",
                        {
                            aushang = ""
                            aushangOffen = true
                        },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }

            if (detail != null) {
                Wachenchat(
                    verlauf = verlauf,
                    ich = ich,
                    laufendeRunde = kopf.laufendeRundeCode,
                    mitglieder = bilder,
                    server = server,
                    beiSenden = { griffe.chatSenden(id, it) },
                    beiBeitreten = griffe.runde,
                    beiOeffnen = { offeneZeile = it.nr },
                )
            } else {
                Kasten { Leise("Wachenchat wird verbunden …") }
            }
        }
    }

    // ---------------------------------------------------------------- Blenden

    offenesMitglied?.let { kennung ->
        // Ist das Mitglied inzwischen weg, steht eben kein Dialog mehr da.
        val mitglied = mitglieder.firstOrNull { it.kennung == kennung }
        if (mitglied != null) {
            MitgliedBlende(
                mitglied = mitglied,
                ich = ich,
                eigeneRolle = kopf.eigeneRolle,
                laeuft = wachen.laeuft,
                beiRolle = { wen, rolle ->
                    speicher.rolleSetzen(id, wen, rolle)
                    offenesMitglied = null
                },
                beiLeitung = { wen ->
                    speicher.leitungUebertragen(id, wen)
                    offenesMitglied = null
                },
                beiEntfernen = { wen ->
                    speicher.entfernen(id, wen)
                    offenesMitglied = null
                },
                beiAustreten = {
                    speicher.verlassen(id)
                    offenesMitglied = null
                },
                beiProfil = griffe.profil,
                beiSchliessen = { offenesMitglied = null },
            )
        }
    }

    offeneZeile?.let { nr ->
        val zeile = verlauf.firstOrNull { it.nr == nr }
        if (zeile != null) {
            NachrichtBlende(
                nachricht = zeile,
                ich = ich,
                eigeneRolle = kopf.eigeneRolle,
                laeuft = wachen.laeuft,
                beiMelden = { n, grund ->
                    speicher.zeileMelden(n, grund)
                    offeneZeile = null
                },
                beiEntfernen = { n ->
                    speicher.zeileEntfernen(n)
                    offeneZeile = null
                },
                beiSchliessen = { offeneZeile = null },
            )
        }
    }

    if (einladenOffen) {
        EinladenBlende(
            dabei = mitglieder.map { it.kennung }.toSet(),
            freunde = freunde,
            treffer = wachen.suchtreffer,
            suchmeldung = wachen.suchmeldung,
            eingeladen = wachen.eingeladen,
            meldung = wachen.meldung,
            laeuft = wachen.laeuft,
            beiSuchen = { speicher.benutzerSuchen(it) },
            beiEinladen = { speicher.einladen(id, it) },
            beiSchliessen = {
                speicher.sucheLeeren()
                einladenOffen = false
            },
        )
    }

    if (einstellungenOffen && kopf.istLeitung) {
        EinstellungenBlende(
            gemeinschaft = kopf,
            landkreise = landkreise,
            server = server,
            laeuft = wachen.laeuft,
            meldung = wachen.meldung,
            beiSpeichern = { speicher.einstellungenSpeichern(id, it) },
            beiCodeErneuern = { speicher.codeErneuern(id) },
            beiAufloesen = { speicher.aufloesen(id) { einstellungenOffen = false } },
            beiSchliessen = { einstellungenOffen = false },
        )
    }

    if (anpassenOffen && schatz != null) {
        AussehenBlende(
            gemeinschaft = kopf,
            schatz = schatz,
            laeuft = wachen.laeuft,
            meldung = wachen.meldung,
            beiSchmuck = { art, stueck -> speicher.schmuckSetzen(id, art, stueck) },
            beiFarbe = { speicher.farbeSetzen(id, it) },
            beiSchliessen = { anpassenOffen = false },
        )
    }
}

// --------------------------------------------------------------- Mannschaft

@Composable
private fun Mannschaftsbereich(
    kopf: Gemeinschaft,
    eingang: List<Gemeinschaftsantrag>,
    meldungen: List<Gemeinschaftsmeldung>,
    mitglieder: List<Gemeinschaftsmitglied>,
    beitraege: List<Wachenbeitrag>,
    bilder: Map<String, Gemeinschaftsmitglied>,
    ich: String,
    server: String,
    laeuft: Boolean,
    speicher: Wachenspeicher,
    beiMitglied: (String) -> Unit,
    beiEinladen: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Gross)) {
        // Beitrittsanträge — nur Entscheider bekommen sie überhaupt.
        if (eingang.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Ueberschrift("Beitrittsanträge")
                eingang.forEach { a ->
                    Kasten(abstandInnen = Abstand.Klein) {
                        Kontoname(name = a.anzeigename, premium = a.premium, teammitglied = a.teammitglied)
                        a.nachricht?.let { SehrLeise("„$it“") }
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf("Aufnehmen", { speicher.antragEntscheiden(a.nr, true) }, aktiv = !laeuft, kompakt = true)
                            Knopf(
                                "Ablehnen",
                                { speicher.antragEntscheiden(a.nr, false) },
                                art = Knopfart.Leise,
                                aktiv = !laeuft,
                                kompakt = true,
                            )
                        }
                    }
                }
            }
        }

        // Gemeldete Zeilen — Aufgaben, die liegen bleiben, wenn man sie erst beim
        // Rollen findet.
        if (meldungen.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Ueberschrift("Gemeldete Zeilen")
                meldungen.forEach { m ->
                    Kasten(abstandInnen = Abstand.Klein) {
                        Text(m.verfasserName, style = Schrift.Normal, color = Farben.Text)
                        Text(
                            text = m.text,
                            style = Schrift.Klein,
                            color = Farben.Text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false)
                                .padding(Abstand.Klein),
                        )
                        SehrLeise("Gemeldet von ${m.melderName}" + (m.grund?.let { " · „$it“" } ?: ""))
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf(
                                if (m.entfernt) "Schon heraus" else "Herausnehmen",
                                { speicher.zeileEntfernen(m.nachrichtNr) },
                                art = Knopfart.Alarm,
                                aktiv = !laeuft && !m.entfernt,
                                kompakt = true,
                            )
                            Knopf(
                                "In Ordnung",
                                { speicher.meldungAbhaken(m.nr) },
                                art = Knopfart.Leise,
                                aktiv = !laeuft,
                                kompakt = true,
                            )
                        }
                    }
                }
            }
        }

        // Mannschaft — sortiert kommt sie vom Server (Rang, dann Dienstalter).
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Ueberschrift("Mannschaft")
                Marke("${kopf.mitglieder} / ${kopf.maxMitglieder}")
                Spacer(Modifier.weight(1f))
                if (kopf.darfFuehren) {
                    Knopf("Einladen", beiEinladen, art = Knopfart.Leise, kompakt = true)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().flaeche()) {
                mitglieder.forEach { m -> Mitgliedzeile(m, ich, server) { beiMitglied(m.kennung) } }
            }
        }

        // Die interne Rangliste — wer seine Statistik nicht teilt, fehlt hier.
        if (beitraege.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Ueberschrift("Wer fährt am meisten")
                Column(modifier = Modifier.fillMaxWidth().flaeche()) {
                    beitraege.forEachIndexed { i, b ->
                        val bild = bilder[b.kennung]
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                        ) {
                            Platzziffer((i + 1).toString(), hervor = i == 0)
                            Kontobild(
                                kennung = b.kennung,
                                anzeigename = b.anzeigename,
                                wappen = bild?.wappen ?: "Keines",
                                wappenfarbe = bild?.wappenfarbe ?: 0,
                                bildAdresse = profilbildAdresse(server, bild?.profilbild),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Kontoname(name = b.anzeigename, premium = b.premium, teammitglied = b.teammitglied)
                                SehrLeise(
                                    "${anzahl(b.schichten, "Schicht", "Schichten")} · ${b.einsaetze} Einsätze",
                                    mono = true,
                                )
                            }
                            Text(zahl(b.punkte), style = Schrift.MonoNormal, color = Farben.Text)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Eine Zeile der Mannschaft. Der grüne Ring sagt „sitzt gerade in einer Runde",
 * bevor man die Lagezeile gelesen hat; die Aktionen liegen im Dialog dahinter.
 */
@Composable
private fun Mitgliedzeile(m: Gemeinschaftsmitglied, ich: String, server: String, beiDruck: () -> Unit) {
    val lage = lagezeile(m.anwesenheit)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = beiDruck)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Kontobild(
            kennung = m.kennung,
            anzeigename = m.anzeigename,
            wappen = m.wappen ?: "Keines",
            wappenfarbe = m.wappenfarbe,
            bildAdresse = profilbildAdresse(server, m.profilbild),
            imDienst = m.anwesenheit != null,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Kontoname(
                    name = m.anzeigename,
                    premium = m.premium == true,
                    teammitglied = m.teammitglied == true,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Marke(wachenrolle(m.rolle), farbe = if (m.rolle == "Leitung") Farben.Amber else Farben.TextLeise)
                if (m.kennung == ich) SehrLeise("(du)")
            }
            SehrLeise(
                m.benutzername +
                    (m.rang?.let { " · $it · Stufe ${m.level ?: "—"}" } ?: "") +
                    " · dabei seit ${tagKurz(m.beigetretenUm)}",
                mono = true,
            )
            if (lage.isNotEmpty()) {
                Text(
                    text = lage,
                    style = Schrift.Klein,
                    color = if (m.anwesenheit != null) Farben.GruenHell else Farben.TextLeise,
                )
            }
        }
        Text("›", style = Schrift.Gross, color = Farben.TextSehrLeise)
    }
}

// ----------------------------------------------------------- Wachennavigation

/**
 * Der Wegweiser des Wachenbereichs — „Meine Wache"/„Wache finden",
 * „Rangliste" und, mit eigener Wache, „Wachen-Shop".
 */
@Composable
internal fun WachenNavigation(
    hier: String,
    hatWache: Boolean,
    beiWache: () -> Unit,
    beiRangliste: () -> Unit,
    beiShop: () -> Unit,
) {
    Reiterreihe {
        Reiter(
            if (hatWache) "Meine Wache" else "Wache finden",
            offen = hier == "wache",
            beiDruck = beiWache,
        )
        Reiter("Rangliste", offen = hier == "rangliste", beiDruck = beiRangliste)
        if (hatWache) Reiter("Wachen-Shop", offen = hier == "shop", beiDruck = beiShop)
    }
}
