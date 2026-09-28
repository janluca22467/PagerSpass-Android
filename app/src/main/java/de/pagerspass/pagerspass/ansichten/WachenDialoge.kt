package de.pagerspass.pagerspass.ansichten

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.Gemeinschaftseinstellungen
import de.pagerspass.pagerspass.netz.Gemeinschaftsmitglied
import de.pagerspass.pagerspass.netz.Gemeinschaftsnachricht
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Wachenschatz
import de.pagerspass.pagerspass.netz.Wachenstueck
import de.pagerspass.pagerspass.netz.Wachensuchtreffer
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Blenden der Wachenseiten — Mitglied, Chatzeile, Einladen, Einstellungen,
 * Aussehen, Gründen. Übertragen aus den `…Dialog.vue`-Dateien in `web/src/components/gemeinschaft/`.
 *
 * <b>Alle Personen- und Zeilen-Aktionen liegen hier und nicht als Knöpfe in der
 * Liste.</b> Dort drängten sie Name, Rang und Dienstalter auf zwei Wörter
 * Breite — und das bei jeder Zeile, obwohl man fast immer nur liest.
 *
 * <b>Was endgültig ist, fragt ein zweites Mal</b> — Entfernen, Austreten,
 * Auflösen. Der erste Druck stellt die Frage, der zweite beantwortet sie.
 */

// ------------------------------------------------------------- Chatzeile

/**
 * Was man mit einer Chatzeile tun kann: melden oder herausnehmen. Melden gibt
 * die Zeile an die Führung ab, Herausnehmen entscheidet selbst — wer nur eines
 * darf, sieht nur eines.
 */
@Composable
internal fun NachrichtBlende(
    nachricht: Gemeinschaftsnachricht,
    ich: String,
    eigeneRolle: String?,
    laeuft: Boolean,
    beiMelden: (Long, String?) -> Unit,
    beiEntfernen: (Long) -> Unit,
    beiSchliessen: () -> Unit,
) {
    val istEigene = nachricht.von == ich
    val darfFuehren = eigeneRolle == "Leitung" || eigeneRolle == "Zugfuehrer"
    val darfEntfernen = istEigene || darfFuehren
    val darfMelden = !istEigene
    var grund by remember { mutableStateOf("") }
    var gefragt by remember { mutableStateOf(false) }

    Blende(
        titel = nachricht.vonName,
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        kopfknoepfe = {
            Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true)
        },
    ) {
        Etikett("Zeile im Wachenchat")
        SehrLeise(tagUndZeit(nachricht.gesendetUm), mono = true)
        Text(
            text = nachricht.text,
            style = Schrift.Normal,
            color = Farben.Text,
            modifier = Modifier
                .fillMaxWidth()
                .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false)
                .padding(Abstand.Normal),
        )

        if (darfMelden) {
            Feld(
                wert = grund,
                beiAenderung = { grund = it.take(200) },
                etikett = "Warum? (freiwillig)",
                platzhalter = "Ein Stichwort genügt",
            )
            Knopf(
                "An die Wachenführung melden",
                { beiMelden(nachricht.nr, grund.trim().ifEmpty { null }) },
                aktiv = !laeuft,
                kompakt = true,
            )
            SehrLeise(
                "Die Führung sieht die Zeile und entscheidet. Wer gemeldet hat, erfährt der Verfasser nicht.",
            )
        }

        if (darfEntfernen) {
            if (!gefragt) {
                Knopf(
                    if (istEigene) "Zeile zurücknehmen" else "Zeile herausnehmen",
                    { gefragt = true },
                    art = Knopfart.Gefahr,
                    kompakt = true,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        "Ja, herausnehmen",
                        { beiEntfernen(nachricht.nr) },
                        art = Knopfart.Alarm,
                        aktiv = !laeuft,
                        kompakt = true,
                    )
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

// ---------------------------------------------------------------- Mitglied

/**
 * Ein Mitglied der Wache: was man über es weiß, und was man mit ihm tun kann.
 *
 * <b>Verwalten dürfen ab 5.0.0.23 auch Zugführer</b> — gegen einen echt
 * niedrigeren Rang. Wer einladen darf, muss auch wieder hinausbegleiten dürfen;
 * hier stand früher „nur die Leitung", und ein Zugführer sah keinen Knopf, den
 * der Server längst angenommen hätte.
 */
@Composable
internal fun MitgliedBlende(
    mitglied: Gemeinschaftsmitglied,
    ich: String,
    eigeneRolle: String?,
    laeuft: Boolean,
    beiRolle: (String, String) -> Unit,
    beiLeitung: (String) -> Unit,
    beiEntfernen: (String) -> Unit,
    beiAustreten: () -> Unit,
    beiProfil: ((String) -> Unit)?,
    beiSchliessen: () -> Unit,
) {
    val istSelbst = mitglied.kennung == ich
    val istLeitung = eigeneRolle == "Leitung"
    val eigenerRang = wachenrang(eigeneRolle)
    val darfVerwalten = !istSelbst &&
        eigenerRang >= wachenrang("Zugfuehrer") &&
        wachenrang(mitglied.rolle) < eigenerRang
    var gefragt by remember { mutableStateOf(false) }
    val lage = lagezeile(mitglied.anwesenheit)

    Blende(
        titel = mitglied.anzeigename,
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        kopfknoepfe = {
            Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true)
        },
    ) {
        Etikett(wachenrolle(mitglied.rolle))
        Kontoname(
            name = mitglied.anzeigename,
            premium = mitglied.premium == true,
            teammitglied = mitglied.teammitglied == true,
            stil = Schrift.Gross,
        )
        SehrLeise(mitglied.benutzername, mono = true)

        mitglied.rang?.let { rang ->
            Angabe("Rang", "$rang · Stufe ${mitglied.level ?: "—"}")
        }
        Angabe("Auf der Wache seit", tagLang(mitglied.beigetretenUm))
        Angabe(
            "Status",
            lage.ifEmpty { "Keine Angabe" },
            farbe = if (mitglied.anwesenheit != null) Farben.GruenHell else Farben.Text,
        )

        if (beiProfil != null && mitglied.benutzername.isNotBlank()) {
            Knopf("Profil ansehen", { beiProfil(mitglied.benutzername) }, art = Knopfart.Leise, kompakt = true)
        }

        when {
            // Der eigene Eintrag: hier geht es nur um die eigene Mitgliedschaft.
            istSelbst -> {
                SehrLeise(
                    "Ein Konto gehört höchstens einer Wachengemeinschaft an. Zum Wechseln trittst du " +
                        "hier aus. Bist du die Leitung, rückt der Dienstälteste nach; bist du die letzte " +
                        "Person, löst sich die Gemeinschaft auf.",
                )
                if (!gefragt) {
                    Knopf("Gemeinschaft verlassen", { gefragt = true }, art = Knopfart.Leise, kompakt = true)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf("Ja, austreten", beiAustreten, art = Knopfart.Alarm, aktiv = !laeuft, kompakt = true)
                        Knopf("Abbrechen", { gefragt = false }, art = Knopfart.Leise, kompakt = true)
                    }
                }
            }

            darfVerwalten -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                ) {
                    when (mitglied.rolle) {
                        "Mitglied" -> Knopf(
                            "Zum Zugführer befördern",
                            { beiRolle(mitglied.kennung, "Zugfuehrer") },
                            aktiv = !laeuft,
                            kompakt = true,
                        )
                        "Zugfuehrer" -> Knopf(
                            "Zum Mitglied zurückstufen",
                            { beiRolle(mitglied.kennung, "Mitglied") },
                            art = Knopfart.Leise,
                            aktiv = !laeuft,
                            kompakt = true,
                        )
                    }
                    // Die Leitung gibt es genau einmal — der Knopf gehört allein ihr.
                    if (istLeitung) {
                        Knopf(
                            "Leitung übergeben",
                            { beiLeitung(mitglied.kennung) },
                            art = Knopfart.Leise,
                            aktiv = !laeuft,
                            kompakt = true,
                        )
                    }
                }

                SehrLeise(
                    if (istLeitung) {
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
                        Knopf(
                            "Ja, entfernen",
                            { beiEntfernen(mitglied.kennung) },
                            art = Knopfart.Alarm,
                            aktiv = !laeuft,
                            kompakt = true,
                        )
                        Knopf("Abbrechen", { gefragt = false }, art = Knopfart.Leise, kompakt = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun Angabe(was: String, wert: String, farbe: Color = Farben.Text) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        Etikett(was)
        Text(text = wert, style = Schrift.Normal, color = farbe)
    }
}

// ----------------------------------------------------------------- Einladen

/**
 * Jemanden in die Wache einladen — zwei Wege nebeneinander.
 *
 * Man weiß, wen man will: Dann steht er in der Freundesliste, und ein Druck
 * genügt. Oder man kennt nur den Benutzernamen: Dann sucht man ihn, und
 * eingeladen wird mit der <b>Kennung</b> des Treffers. Hier ging früher der
 * getippte Benutzername als Kennung zum Server — und jede Einladung scheiterte.
 */
@Composable
internal fun EinladenBlende(
    dabei: Set<String>,
    freunde: List<Freund>,
    treffer: Wachensuchtreffer?,
    suchmeldung: String?,
    eingeladen: Set<String>,
    meldung: String?,
    laeuft: Boolean,
    beiSuchen: (String) -> Unit,
    beiEinladen: (String) -> Unit,
    beiSchliessen: () -> Unit,
) {
    var begriff by remember { mutableStateOf("") }
    val offeneFreunde = freunde.filter { it.bestaetigt && it.kennung !in dabei }

    Blende(
        titel = "Zur Gemeinschaft einladen",
        beiSchliessen = beiSchliessen,
        kopfknoepfe = {
            Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true)
        },
    ) {
        meldung?.let { Meldungszeile(it) }

        Ueberschrift("Über den Benutzernamen")
        SehrLeise("Erreicht auch, wer nicht mit dir befreundet ist.")
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Feld(
                wert = begriff,
                beiAenderung = { begriff = it.trim().take(40) },
                platzhalter = "benutzername",
                modifier = Modifier.weight(1f),
            )
            Knopf("Suchen", { beiSuchen(begriff) }, aktiv = begriff.isNotBlank(), kompakt = true)
        }
        suchmeldung?.let { SehrLeise(it) }

        treffer?.let { t ->
            Einladezeile(
                name = t.anzeigename,
                premium = t.premium,
                teammitglied = t.teammitglied,
                tag = t.wachentag,
                unterzeile = t.benutzername + (t.rang?.let { " · $it" } ?: ""),
                aufschrift = when {
                    t.kennung in dabei -> "Schon dabei"
                    t.kennung in eingeladen -> "Eingeladen"
                    else -> "Einladen"
                },
                aktiv = !laeuft && t.kennung !in dabei && t.kennung !in eingeladen,
                beiDruck = { beiEinladen(t.kennung) },
            )
        }

        Ueberschrift("Aus deiner Freundesliste", Modifier.padding(top = Abstand.Klein))
        if (offeneFreunde.isEmpty()) {
            SehrLeise("Hier steht niemand, der noch nicht auf der Wache ist.")
        } else {
            offeneFreunde.forEach { f ->
                Einladezeile(
                    name = f.anzeigename.ifBlank { f.benutzername },
                    premium = f.premium,
                    teammitglied = f.teammitglied,
                    tag = null,
                    unterzeile = f.benutzername + (f.rang.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    aufschrift = if (f.kennung in eingeladen) "Eingeladen" else "Einladen",
                    aktiv = !laeuft && f.kennung !in eingeladen,
                    beiDruck = { beiEinladen(f.kennung) },
                )
            }
        }
    }
}

@Composable
private fun Einladezeile(
    name: String,
    premium: Boolean,
    teammitglied: Boolean,
    tag: String?,
    unterzeile: String,
    aufschrift: String,
    aktiv: Boolean,
    beiDruck: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!tag.isNullOrBlank()) Wachentagplakette(tag)
                Kontoname(name = name, premium = premium, teammitglied = teammitglied)
            }
            SehrLeise(unterzeile, mono = true)
        }
        Knopf(aufschrift, beiDruck, aktiv = aktiv, kompakt = true)
    }
}

/** Die Zeile mit dem Satz des Servers — mono, wie `.meldung` im Web. */
@Composable
internal fun Meldungszeile(text: String) {
    Text(
        text = text,
        style = Schrift.MonoKlein,
        color = Farben.AmberHell,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(farbe = Farben.HauchAmber, randfarbe = Farben.AmberTief, ecke = 9.dp, mitLichtkante = false)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    )
}

// ------------------------------------------------------------ Landkreiswahl

/** Ein Feld für den Landkreis — mit „Keiner" als eigener Wahl. */
@Composable
internal fun Landkreiswahl(
    etikett: String,
    gewaehlt: Landkreis?,
    landkreise: List<Landkreis>,
    leer: String,
    beiWahl: (Landkreis?) -> Unit,
) {
    var offen by remember { mutableStateOf(false) }

    Wahlfeld(
        etikett = etikett,
        wert = gewaehlt?.aufschrift,
        beiDruck = { offen = true },
        platzhalter = leer,
    )

    if (offen) {
        val alle: List<Landkreis?> = listOf<Landkreis?>(null) + landkreise.sortedBy { it.name }
        Wahlblende(
            titel = etikett,
            gruppen = listOf<Pair<String?, List<Landkreis?>>>(null to alle),
            aufschrift = { it?.aufschrift ?: leer },
            beiWahl = {
                beiWahl(it)
                offen = false
            },
            beiSchliessen = { offen = false },
            gewaehlt = gewaehlt,
            suchbar = true,
        )
    }
}

// ------------------------------------------------------------------ Gründen

/**
 * Eine Wache gründen — bewusst kurz: Name, ein Satz, ein Landkreis. Der
 * Landkreis ist mit dem vorbelegt, in dem man ohnehin fährt; er ist die
 * Grundlage jeder Clanrunde.
 */
@Composable
internal fun GruendenBlende(
    landkreise: List<Landkreis>,
    vorschlagId: String?,
    laeuft: Boolean,
    meldung: String?,
    beiGruenden: (String, String?, Landkreis?) -> Unit,
    beiSchliessen: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var beschreibung by remember { mutableStateOf("") }
    var kreis by remember { mutableStateOf<Landkreis?>(null) }
    var angefasst by remember { mutableStateOf(false) }

    // Der Vorschlag kommt nach dem Öffnen — er füllt nur, solange niemand gewählt hat.
    LaunchedEffect(vorschlagId, landkreise.size) {
        if (!angefasst && kreis == null && vorschlagId != null) {
            kreis = landkreise.firstOrNull { it.id == vorschlagId }
        }
    }

    Blende(
        titel = "Gemeinschaft gründen",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf(
                "Gemeinschaft gründen",
                { beiGruenden(name.trim(), beschreibung.trim().ifEmpty { null }, kreis) },
                art = Knopfart.Haupt,
                aktiv = !laeuft && name.isNotBlank(),
            )
        },
    ) {
        Etikett("Wachengemeinschaft")
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
        Landkreiswahl(
            etikett = "Landkreis",
            gewaehlt = kreis,
            landkreise = landkreise,
            leer = "Noch keiner",
            beiWahl = {
                angefasst = true
                kreis = it
            },
        )
        SehrLeise("Hier laufen später eure Clanrunden. Änderbar bleibt er jederzeit.")
        meldung?.let { Meldungszeile(it) }
    }
}

// ------------------------------------------------------------ Einstellungen

/**
 * Die Stellschrauben der Wache — nur für die Leitung, alle auf einmal
 * gespeichert: Die Beitrittsregeln hängen zusammen, und wer sie stellt, will
 * sie als Ganzes stellen.
 *
 * Der Beitrittscode steht mit QR daneben — der Weg, der auf einem Bildschirm
 * gezeigt und abfotografiert wird. Auflösen steht ganz unten und braucht einen
 * zweiten Druck: Es ist der einzige Knopf hier, der nichts zurücklässt.
 */
@Composable
internal fun EinstellungenBlende(
    gemeinschaft: Gemeinschaft,
    landkreise: List<Landkreis>,
    server: String,
    laeuft: Boolean,
    meldung: String?,
    beiSpeichern: (Gemeinschaftseinstellungen) -> Unit,
    beiCodeErneuern: () -> Unit,
    beiAufloesen: () -> Unit,
    beiSchliessen: () -> Unit,
) {
    val zusammenhang = LocalContext.current
    val zwischenablage = LocalClipboardManager.current

    var name by remember { mutableStateOf(gemeinschaft.name) }
    var beschreibung by remember { mutableStateOf(gemeinschaft.beschreibung.orEmpty()) }
    var kreis by remember {
        mutableStateOf(landkreise.firstOrNull { it.id == gemeinschaft.landkreisId })
    }
    var oeffentlich by remember { mutableStateOf(gemeinschaft.oeffentlich) }
    var inRangliste by remember { mutableStateOf(gemeinschaft.inRangliste) }
    var modus by remember { mutableStateOf(gemeinschaft.beitrittModus) }
    var mindestLevel by remember { mutableStateOf(gemeinschaft.mindestLevel.toString()) }
    var mindestErfahrung by remember { mutableStateOf(gemeinschaft.mindestErfahrung.toString()) }
    var maxMitglieder by remember { mutableStateOf(gemeinschaft.maxMitglieder.toString()) }
    var aufloesenGefragt by remember { mutableStateOf(false) }
    var kopiert by remember { mutableStateOf(false) }

    // Kommt der Katalog erst nach dem Öffnen, wird der gespeicherte Kreis nachgetragen.
    LaunchedEffect(landkreise.size) {
        if (kreis == null) kreis = landkreise.firstOrNull { it.id == gemeinschaft.landkreisId }
    }

    Blende(
        titel = "Einstellungen",
        beiSchliessen = beiSchliessen,
        kopfknoepfe = {
            Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true)
        },
    ) {
        Etikett("Wachengemeinschaft")
        Feld(wert = name, beiAenderung = { name = it.take(40) }, etikett = "Name")
        Feld(
            wert = beschreibung,
            beiAenderung = { beschreibung = it.take(300) },
            etikett = "Beschreibung",
            einzeilig = false,
        )
        Landkreiswahl(
            etikett = "Landkreis",
            gewaehlt = kreis,
            landkreise = landkreise,
            leer = "Keiner",
            beiWahl = { kreis = it },
        )
        SehrLeise("Hier laufen eure Clanrunden.")

        Schalterzeile(
            titel = "Öffentlich gelistet",
            an = oeffentlich,
            beiWechsel = { oeffentlich = it },
            unterzeile = "Name, Beschreibung, Landkreis und Mitgliederzahl sind dann für alle angemeldeten " +
                "Konten sichtbar. Wer in der Mitgliederliste steht, entscheidet jeder selbst in seinen " +
                "Privatsphäre-Einstellungen.",
        )
        Schalterzeile(
            titel = "In der Rangliste stehen",
            an = inRangliste,
            beiWechsel = { inRangliste = it },
            unterzeile = "Ob eure Wache in der Gemeinschafts-Rangliste auftaucht. Getrennt vom öffentlichen " +
                "Listen: das eine ist eine Einladung an Neue, das andere ein Vergleich mit anderen Wachen — " +
                "man kann das eine wollen und das andere nicht.",
        )

        Etikett("Aufnahme")
        Segment(
            seiten = listOf("Einladung", "Antrag", "Offen"),
            gewaehlt = modus,
            beiWahl = { modus = it },
            aufschrift = {
                when (it) {
                    "Einladung" -> "Nur Einladung"
                    "Antrag" -> "Auf Antrag"
                    else -> "Offen"
                }
            },
        )
        SehrLeise(
            "„Offen“ nimmt jeden auf, der die Schwellen darunter erfüllt. Über den Beitrittscode kommt " +
                "man in jedem Fall herein.",
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Feld(
                wert = mindestLevel,
                beiAenderung = { mindestLevel = it.filter(Char::isDigit).take(3) },
                etikett = "Mindestlevel",
                tastatur = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            Feld(
                wert = mindestErfahrung,
                beiAenderung = { mindestErfahrung = it.filter(Char::isDigit).take(9) },
                etikett = "Mindesterfahrung",
                tastatur = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        Feld(
            wert = maxMitglieder,
            beiAenderung = { maxMitglieder = it.filter(Char::isDigit).take(2) },
            etikett = "Höchstens Mitglieder",
            tastatur = KeyboardType.Number,
        )
        SehrLeise("Mindestens ${gemeinschaft.mitglieder} (so viele seid ihr), höchstens 50.")

        meldung?.let { Meldungszeile(it) }

        Knopf(
            "Übernehmen",
            {
                beiSpeichern(
                    Gemeinschaftseinstellungen(
                        name = name.trim(),
                        beschreibung = beschreibung.trim().ifEmpty { null },
                        landkreisId = kreis?.id,
                        landkreis = kreis?.name,
                        // Unverändert durchgereicht: Der Farbton wird im Aussehen gewählt.
                        wappenfarbe = gemeinschaft.wappenfarbe,
                        oeffentlich = oeffentlich,
                        inRangliste = inRangliste,
                        beitrittModus = modus,
                        mindestLevel = mindestLevel.toIntOrNull() ?: gemeinschaft.mindestLevel,
                        mindestErfahrung = mindestErfahrung.toIntOrNull() ?: gemeinschaft.mindestErfahrung,
                        maxMitglieder = maxMitglieder.toIntOrNull() ?: gemeinschaft.maxMitglieder,
                    ),
                )
            },
            art = Knopfart.Haupt,
            aktiv = !laeuft && name.isNotBlank(),
            breit = true,
        )

        gemeinschaft.beitrittscode?.let { code ->
            // Ein vollständiger Link, kein nackter Code: Abfotografiert wird er meist
            // von jemandem, der die Seite noch gar nicht offen hat.
            val link = "${server.trimEnd('/')}/gemeinschaften?code=$code"

            Ueberschrift("Beitrittscode", Modifier.padding(top = Abstand.Gross))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Qrbild(inhalt = link, groesse = 136.dp)
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = code,
                        style = Schrift.Titel.copy(fontFamily = Schrift.Mono),
                        color = Farben.Amber,
                    )
                    SehrLeise("Wer diesen Code hat, tritt ohne Rückfrage bei. Abfotografieren genügt.")
                }
            }
            SehrLeise(link, mono = true)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                Knopf(
                    "Teilen",
                    {
                        val absicht = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "${gemeinschaft.name} auf PagerSpass")
                            putExtra(Intent.EXTRA_TEXT, link)
                        }
                        runCatching { zusammenhang.startActivity(Intent.createChooser(absicht, null)) }
                    },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
                Knopf(
                    if (kopiert) "Link kopiert" else "Link kopieren",
                    {
                        zwischenablage.setText(AnnotatedString(link))
                        kopiert = true
                    },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
                Knopf(
                    "Code neu würfeln",
                    {
                        kopiert = false
                        beiCodeErneuern()
                    },
                    art = Knopfart.Leise,
                    aktiv = !laeuft,
                    kompakt = true,
                )
            }
        }

        Ueberschrift("Gemeinschaft auflösen", Modifier.padding(top = Abstand.Gross))
        SehrLeise(
            "Mitgliedschaften, Chatverlauf und alle offenen Anträge werden gelöscht. Das lässt sich nicht " +
                "rückgängig machen.",
        )
        if (!aufloesenGefragt) {
            Knopf("Auflösen", { aufloesenGefragt = true }, art = Knopfart.Gefahr, kompakt = true)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Ja, endgültig auflösen", beiAufloesen, art = Knopfart.Alarm, aktiv = !laeuft, kompakt = true)
                Knopf("Abbrechen", { aufloesenGefragt = false }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
}

/**
 * Ein QR-Code, gezeichnet aus der Matrix von `zxing` — dieselbe Bibliothek,
 * die den Begleiter-Code liest, schreibt ihn hier auch. Schwarz auf Weiß und
 * mit Ruhezone, sonst liest ihn nicht jede Kamera.
 */
@Composable
internal fun Qrbild(inhalt: String, groesse: Dp = 152.dp) {
    val matrix = remember(inhalt) {
        runCatching {
            QRCodeWriter().encode(inhalt, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 2))
        }.getOrNull()
    } ?: return

    Canvas(
        modifier = Modifier
            .size(groesse)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White),
    ) {
        val zelle = size.width / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * zelle, y * zelle),
                        // Ein Hauch Überstand gegen die Haarfugen zwischen den Zellen.
                        size = Size(zelle + 0.5f, zelle + 0.5f),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Aussehen

/**
 * Die Probe eines Stücks — ein Baustein für Auslage und Anpassen-Dialog, damit
 * ein Muster im Laden genauso aussieht wie danach am Kopf.
 */
@Composable
internal fun WachenProbe(
    art: String,
    stueckId: String?,
    name: String,
    ton: Color,
    wachenname: String,
) {
    when (art) {
        "Kopfmuster" -> Box(
            modifier = Modifier
                .width(84.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Farben.Flaeche)
                .kopfband(stueckId ?: "keines", ton),
        )

        "Emblemrahmen" -> Wachenemblem(wachenname, ton, null, stueckId, groesse = 40.dp)

        "Emblemzeichen" -> Wachenemblem(wachenname, ton, stueckId, null, groesse = 40.dp)

        "Wachenfarbe" -> {
            val nummer = stueckId?.toIntOrNull()
            val farbe = nummer?.let { Wappen.PALETTE[it % Wappen.PALETTE.size] } ?: ton
            Wachenemblem(wachenname, farbe, null, null, groesse = 40.dp)
        }

        "Beiname" -> Text(
            text = name.uppercase(),
            style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
            color = Farben.Amber,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Der Ausbau hat kein Stück, das man ansehen könnte — er hat eine Zahl.
        else -> Text(
            text = if (art == "Mitgliederplaetze") "+2" else "+1",
            style = Schrift.Titel.copy(fontFamily = Schrift.Mono),
            color = Farben.Amber,
        )
    }
}

/** Was unter einem Stück steht, das die Wache nicht tragen darf: Preis oder Stufe. */
private fun huerde(s: Wachenstueck): String = when {
    s.frei -> ""
    s.preis == null -> "Ab Stufe ${s.abStufe}"
    s.imSortiment -> "${s.preis} Coins · im Laden"
    else -> "${s.preis} Coins"
}

/**
 * „Wache anpassen" — der ganze Katalog, nicht nur die Auslage der Woche: alles,
 * was die Wache hat, was sie noch erspielen kann und was im Laden wartet.
 *
 * Oben die Vorschau — derselbe Kopf wie auf der Wachenseite, nur klein, und er
 * ändert sich beim Anlegen mit. `darfKaufen` steuert nur, welche Knöpfe
 * dastehen; entschieden wird am Server.
 */
@Composable
internal fun AussehenBlende(
    gemeinschaft: Gemeinschaft,
    schatz: Wachenschatz,
    laeuft: Boolean,
    meldung: String?,
    beiSchmuck: (String, String?) -> Unit,
    beiFarbe: (Int) -> Unit,
    beiSchliessen: () -> Unit,
) {
    val darfWaehlen = schatz.darfKaufen
    val ton = wachenton(gemeinschaft)
    val beiname = beinameVon(schatz, gemeinschaft.beiname)

    fun getragen(art: String): String = when (art) {
        "Kopfmuster" -> gemeinschaft.kopfmuster
        "Emblemrahmen" -> gemeinschaft.emblemrahmen
        "Emblemzeichen" -> gemeinschaft.emblemzeichen
        else -> gemeinschaft.beiname
    }

    Blende(
        titel = "Wache anpassen",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Breit,
        kopfknoepfe = {
            Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true)
        },
    ) {
        // Die Vorschau — dieselbe Bauform wie der Wachenkopf.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .flaeche(ecke = 9.dp)
                .kopfband(gemeinschaft.kopfmuster, ton)
                .padding(Abstand.Normal),
        ) {
            Wachenemblem(
                name = gemeinschaft.name,
                ton = ton,
                zeichen = gemeinschaft.emblemzeichen,
                rahmen = gemeinschaft.emblemrahmen,
                groesse = 48.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                beiname?.let {
                    Text(
                        text = it.uppercase(),
                        style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Amber,
                    )
                }
                Text(gemeinschaft.name, style = Schrift.Gross, color = Farben.Text)
                SehrLeise("So sieht eure Wache aus.")
            }
        }

        meldung?.let { Meldungszeile(it) }
        if (!darfWaehlen) Leise("Das Aussehen der Wache wählen Leitung und Zugführer. Ansehen darf es jeder.")

        // Erst der Farbton: Er färbt alles andere, also gehört er nach vorn.
        Ueberschrift("Farbton")
        SehrLeise("Die ersten acht gehören jeder Wache. Was danach kommt, steht im Laden.")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        ) {
            for (n in 0 until 8) {
                val farbe = Wappen.ton(gemeinschaft.id, n)
                Farbfeld(
                    farbe = farbe,
                    gewaehlt = gemeinschaft.wappenfarbe == n,
                    zu = false,
                    aktiv = darfWaehlen && !laeuft,
                    aufschrift = if (n == 0) "A" else "",
                    beiDruck = { if (gemeinschaft.wappenfarbe != n) beiFarbe(n) },
                )
            }
            schatz.schmuck.filter { it.art == "Wachenfarbe" }.forEach { f ->
                val nummer = f.stueckId.toIntOrNull() ?: 0
                Farbfeld(
                    farbe = Wappen.PALETTE[nummer % Wappen.PALETTE.size],
                    gewaehlt = gemeinschaft.wappenfarbe == nummer,
                    zu = !f.frei,
                    aktiv = darfWaehlen && !laeuft && f.frei,
                    aufschrift = "",
                    beiDruck = { if (gemeinschaft.wappenfarbe != nummer) beiFarbe(nummer) },
                )
            }
        }

        // Dann die vier Plätze, immer in derselben Reihenfolge.
        listOf("Kopfmuster", "Emblemrahmen", "Emblemzeichen", "Beiname").forEach { art ->
            val alle = schatz.schmuck.filter { it.art == art }
            Ueberschrift(platzname(art), Modifier.padding(top = Abstand.Klein))
            SehrLeise("${alle.count { it.frei }} von ${alle.size} offen")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                alle.forEach { s ->
                    val an = getragen(art) == s.stueckId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                        modifier = Modifier
                            .width(104.dp)
                            .heightIn(min = 112.dp)
                            .flaeche(
                                farbe = if (an) Farben.FlaecheHoch else Farben.Flaeche,
                                randfarbe = if (an) gattungston(art) else Farben.Rand,
                                ecke = 9.dp,
                                mitLichtkante = false,
                            )
                            .clickable(enabled = darfWaehlen && !laeuft && s.frei) {
                                // Ein Druck auf das Getragene legt es ab — `null` heißt leer,
                                // und wie das auf diesem Platz heißt, weiß der Server.
                                beiSchmuck(art, if (an) null else s.stueckId)
                            }
                            .padding(Abstand.Klein),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.height(48.dp).fillMaxWidth(),
                        ) {
                            WachenProbe(art, s.stueckId, s.name, ton, gemeinschaft.name)
                        }
                        Text(
                            text = s.name,
                            style = Schrift.Winzig,
                            color = if (s.frei) Farben.Text else Farben.TextSehrLeise,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        when {
                            !s.frei -> Text(
                                text = huerde(s),
                                style = Schrift.Winzig,
                                color = Farben.TextSehrLeise,
                                textAlign = TextAlign.Center,
                            )
                            s.preis == null && s.abStufe > 1 -> Text(
                                text = "Erspielt",
                                style = Schrift.Winzig,
                                color = Farben.AmberHell,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Farbfeld(
    farbe: Color,
    gewaehlt: Boolean,
    zu: Boolean,
    aktiv: Boolean,
    aufschrift: String,
    beiDruck: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .border(2.dp, if (gewaehlt) Farben.Amber else Color.Transparent, RoundedCornerShape(10.dp))
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (zu) farbe.copy(alpha = 0.45f) else farbe)
            .clickable(enabled = aktiv, onClick = beiDruck),
    ) {
        when {
            zu -> Icon(
                imageVector = SCHLOSS,
                contentDescription = "Noch nicht gekauft",
                tint = Wappen.schrift(farbe),
                modifier = Modifier.size(16.dp),
            )
            aufschrift.isNotEmpty() -> Text(
                text = aufschrift,
                style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
                color = Wappen.schrift(farbe),
            )
        }
    }
}
