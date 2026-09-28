package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Uebungsseite
import de.pagerspass.pagerspass.mobil.bundeslaender
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Stichwort
import de.pagerspass.pagerspass.netz.Szenario
import de.pagerspass.pagerspass.netz.Szenarioeinstellungen
import de.pagerspass.pagerspass.netz.Szenarioeintrag
import de.pagerspass.pagerspass.netz.Uebungsaufschrift
import de.pagerspass.pagerspass.netz.bundeslandname
import de.pagerspass.pagerspass.ui.bausteine.Codefeld
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Regler
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die vorbereiteten Übungen — Liste und Editor, übertragen aus
 * `web/src/views/UebungenView.vue`.
 *
 * <b>Zwei Seiten statt einer Seite mit zwei Zuständen.</b> Im Web entscheidet
 * `?id` in der Adresse zwischen Liste und Editor, und der Weg zurück ist der
 * Zurück-Knopf des Browsers. Hier sind es zwei Ziele der Navigation — derselbe
 * Gedanke: Zurück ist Zurück, nicht ein eigener Zustand, den man im falschen
 * Moment verliert.
 *
 * <b>Der Eintragseditor ist keine Überlagerung.</b> Ein Dialog, der mit seinem
 * Inhalt wächst, schiebt am Handy seine Knopfleiste unter die Bildkante; ein
 * aufgeklappter Abschnitt in der Seite rollt mit ihr.
 */

// -------------------------------------------------------------------- Liste

@Composable
fun UebungenSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Uebungsseite = Uebungsseite(),
    /** Ob der Katalog da ist — ohne ihn lässt sich keine Übung anlegen. */
    katalogBereit: Boolean = false,
    katalogFehler: String? = null,
    beiLaden: () -> Unit = {},
    beiNeu: () -> Unit = {},
    beiUebernehmen: (code: String, danach: () -> Unit) -> Unit = { _, _ -> },
    beiLeiten: (String) -> Unit = {},
    beiFahren: (String) -> Unit = {},
    beiBearbeiten: (String) -> Unit = {},
    beiLoeschen: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    var fremderCode by rememberSaveable { mutableStateOf("") }

    Seite(modifier = modifier, unterrand = unterrand) {
        Etikett("Übungen")
        Seitenkopf(
            titel = "Vorbereitete Übungen",
            knoepfe = {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true)
                    // Ohne Katalog keine Aufstellung und kein Landkreis — eine
                    // Übung ohne Fahrzeuge ist eine Runde, die nie beginnen kann.
                    Knopf(
                        "Neue Übung",
                        beiNeu,
                        art = Knopfart.Haupt,
                        kompakt = true,
                        aktiv = katalogBereit,
                    )
                }
            },
        )

        if (!katalogBereit && katalogFehler == null) {
            SehrLeise("Der Fahrzeugkatalog wird noch geladen.")
        }
        (stand.fehler ?: katalogFehler)?.let { Hinweiszeile(it, fehler = true) }
            ?: stand.meldung?.let { Hinweiszeile(it) }

        // Übernehmen per Code — eine eigene Kopie, keine Verknüpfung.
        Kasten(abstandInnen = Abstand.Klein) {
            Etikett("Übung eines anderen übernehmen")
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Codefeld(
                    wert = fremderCode,
                    beiAenderung = { fremderCode = it },
                    laenge = 6,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Übernehmen",
                    { beiUebernehmen(fremderCode) { fremderCode = "" } },
                    aktiv = !stand.unterwegs && fremderCode.trim().length >= 6,
                    kompakt = true,
                )
            }
            SehrLeise(
                "Du bekommst eine eigene Kopie. Ändert der andere seine Übung, bleibt " +
                    "deine, wie sie ist.",
            )
        }

        val liste = stand.liste.inhalt.orEmpty()

        if (stand.liste.ersteLadung) Ladezeile()

        // Nur wenn die Liste wirklich beim Server war — sonst stünde „noch keine"
        // da, während in Wahrheit nur die Antwort ausblieb.
        if (stand.liste.geladen && liste.isEmpty()) {
            Leerhinweis("Noch keine Übung. „Neue Übung\" legt die erste an.")
        }

        liste.forEach { s ->
            Kasten(abstandInnen = Abstand.Klein) {
                Text(
                    text = s.name,
                    style = Schrift.Gross,
                    color = Farben.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (s.beschreibung.isNotBlank()) Leise(s.beschreibung)
                SehrLeise(
                    "${s.eintraege} ${if (s.eintraege == 1) "Eintrag" else "Einträge"} · Code ${s.code}",
                    mono = true,
                )
                Pillenreihe {
                    Knopf(
                        "Leiten",
                        { beiLeiten(s.id) },
                        art = Knopfart.Haupt,
                        aktiv = !stand.unterwegs,
                        kompakt = true,
                    )
                    Knopf("Selbst fahren", { beiFahren(s.id) }, aktiv = !stand.unterwegs, kompakt = true)
                    Knopf("Bearbeiten", { beiBearbeiten(s.id) }, kompakt = true)
                    val gefragt = stand.loeschGefragt == s.id
                    Knopf(
                        if (gefragt) "Wirklich löschen?" else "Löschen",
                        { beiLoeschen(s.id) },
                        art = if (gefragt) Knopfart.Gefahr else Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }
        }

        // Der Übungsverlauf. Er steht hier und nicht im Dienstbuch: Eine Übung
        // bringt weder Erfahrung noch Credits und zählt in keiner Wertung.
        if (stand.verlauf.isNotEmpty()) {
            Kasten(abstandInnen = Abstand.Klein) {
                Etikett("Übungsverlauf")
                SehrLeise(
                    "Was du gefahren hast. Übungen bringen bewusst keine Erfahrung und " +
                        "keine Credits — sonst schriebe sich jeder seine Punkte selbst.",
                )
                stand.verlauf.forEach { f ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                        modifier = Modifier.fillMaxWidth().padding(top = Abstand.Winzig),
                    ) {
                        Text(text = f.name, style = Schrift.Normal, color = Farben.Text)
                        SehrLeise(
                            "${tag(f.beendetUm)} · " +
                                (if (f.rolle == "Leitstelle") "Leitstelle" else f.funkrufname ?: "Besatzung") +
                                " · ${f.einsaetze} ${if (f.einsaetze == 1) "Einsatz" else "Einsätze"}",
                            mono = true,
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------- Editor

/**
 * Der Editor einer Übung.
 *
 * <b>Er ändert den Entwurf, nicht den Server.</b> Jede Eingabe geht als
 * Änderung an den Entwurf (`beiAendern`); gespeichert wird erst mit
 * „Speichern" — wie im Web, wo der Entwurf an der Seite hängt.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UebungseditorSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Uebungsseite = Uebungsseite(),
    katalog: Katalog? = null,
    beiAendern: ((Szenario) -> Szenario) -> Unit = {},
    beiSichern: () -> Unit = {},
    beiLeiten: (String) -> Unit = {},
    beiFahren: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    val offen = stand.offen

    Seite(modifier = modifier, unterrand = unterrand) {
        Etikett("Übungen")
        Seitenkopf(
            titel = "Vorbereitete Übungen",
            knoepfe = {
                Knopf("← Zur Liste", beiZurueck, art = Knopfart.Leise, kompakt = true)
            },
        )

        if (offen == null) {
            if (stand.oeffnet) Ladezeile("Die Übung wird geöffnet …")
            stand.fehler?.let { Hinweiszeile(it, fehler = true) }
            return@Seite
        }

        Stammdaten(offen, katalog, beiAendern)
        Aufstellung(offen, katalog, beiAendern)
        Zeitachse(offen, katalog, beiAendern)
        Regeln(offen.einstellungen) { umbau -> beiAendern { it.copy(einstellungen = umbau(it.einstellungen)) } }

        // Der Fuß: Gespeichert wird hier, und die Rückmeldung steht hier — eine
        // Bestätigung, die man erst durch Hochrollen findet, ist keine.
        Pillenreihe(modifier = Modifier.fillMaxWidth()) {
            Knopf("Speichern", beiSichern, art = Knopfart.Haupt, aktiv = !stand.speichert)
            if (offen.id.isNotBlank()) {
                Knopf("Übung leiten", { beiLeiten(offen.id) }, aktiv = !stand.unterwegs)
                Knopf("Selbst fahren", { beiFahren(offen.id) }, aktiv = !stand.unterwegs)
            }
            Knopf("Zurück", beiZurueck, art = Knopfart.Leise)
        }
        (stand.fehler)?.let { Hinweiszeile(it, fehler = true) }
            ?: stand.meldung?.let { Hinweiszeile(it) }
    }
}

// --------------------------------------------------------------- Stammdaten

@Composable
private fun Stammdaten(
    offen: Szenario,
    katalog: Katalog?,
    beiAendern: ((Szenario) -> Szenario) -> Unit,
) {
    var kreiswahl by remember { mutableStateOf(false) }
    val landkreise = katalog?.landkreise.orEmpty()
    val kreis = landkreise.firstOrNull { it.id == offen.landkreisId }

    Kasten(abstandInnen = Abstand.Normal) {
        Feld(
            wert = offen.name,
            beiAenderung = { neu -> beiAendern { it.copy(name = neu.take(60)) } },
            etikett = "Name",
        )
        Feld(
            wert = offen.beschreibung,
            beiAenderung = { neu -> beiAendern { it.copy(beschreibung = neu.take(400)) } },
            etikett = "Beschreibung",
            platzhalter = "Worum es in dieser Übung geht.",
            einzeilig = false,
            weiterTaste = ImeAction.Default,
        )
        Wahlfeld(
            etikett = "Landkreis",
            wert = kreis?.name ?: if (offen.landkreisId == null) "Erfundener Standardbereich" else offen.landkreisId,
            beiDruck = { kreiswahl = true },
            aktiv = landkreise.isNotEmpty(),
        )
        if (offen.code.isNotBlank()) {
            SehrLeise("Zum Weitergeben: ${offen.code}", mono = true)
        }
    }

    if (kreiswahl) {
        val gruppen: List<Pair<String?, List<Landkreis?>>> =
            listOf<Pair<String?, List<Landkreis?>>>(null to listOf(null)) +
                landkreise.bundeslaender().map { land ->
                    bundeslandname(land) to landkreise.filter { it.bundesland == land }.sortedBy { it.name }
                }
        Wahlblende(
            titel = "Landkreis",
            gruppen = gruppen,
            aufschrift = { it?.name ?: "Erfundener Standardbereich" },
            gewaehlt = kreis,
            beiWahl = { gewaehlt ->
                beiAendern { it.copy(landkreisId = gewaehlt?.id) }
                kreiswahl = false
            },
            beiSchliessen = { kreiswahl = false },
            suchbar = true,
        )
    }
}

// -------------------------------------------------------------- Aufstellung

/**
 * Die Aufstellung — was in dieser Übung im Dienst steht.
 *
 * <b>Der Träger steht an jedem Fahrzeug.</b> Ohne ihn stand im Web fünfmal „RTW"
 * untereinander, und welches davon das Malteser-Fahrzeug war, ließ sich nicht
 * sagen. Und er entscheidet: Wer im gewählten Kreis keine Wache hat, rückt dort
 * nicht aus — die Engine weist ihn beim Eröffnen der Runde wortlos ab.
 */
@Composable
private fun Aufstellung(
    offen: Szenario,
    katalog: Katalog?,
    beiAendern: ((Szenario) -> Szenario) -> Unit,
) {
    var auswahlOffen by rememberSaveable { mutableStateOf(false) }

    val vorlagen = katalog?.fahrzeuge.orEmpty().associateBy { it.id }
    val kreisTraeger = katalog?.landkreise.orEmpty()
        .firstOrNull { it.id == offen.landkreisId }?.hiOrgs.orEmpty()

    /** Ob dieser Träger im gewählten Kreis überhaupt ausrücken kann. */
    fun traegerImKreis(hiOrg: String): Boolean =
        hiOrg == "Keine" || hiOrg.isBlank() || kreisTraeger.isEmpty() || hiOrg in kreisTraeger

    fun imKreis(vorlage: Fahrzeugvorlage?): Boolean = vorlage != null && traegerImKreis(vorlage.hiOrg)

    val fehlend = offen.aufstellung.filter { !imKreis(vorlagen[it]) }

    Kasten(abstandInnen = Abstand.Klein) {
        Etikett("Aufstellung (${offen.aufstellung.size} Fahrzeuge)")
        SehrLeise("Was in dieser Übung im Dienst steht. Unbesetzte Fahrzeuge bekommen Bot-Besatzungen.")

        if (offen.aufstellung.isEmpty()) {
            Hinweiszeile(
                "Ohne Fahrzeuge kann die Übung nicht beginnen — es fehlt die Gegenstelle.",
                fehler = true,
            )
        }

        if (fehlend.isNotEmpty()) {
            val namen = fehlend.joinToString(", ") { id ->
                val v = vorlagen[id]
                "${v?.typ ?: id} (${v?.let { Uebungsaufschrift.traeger(it.hiOrg) }.orEmpty()})"
            }
            Hinweiszeile(
                (if (fehlend.size == 1) "Ein Fahrzeug steht" else "${fehlend.size} Fahrzeuge stehen") +
                    " in diesem Landkreis nicht im Dienst und " +
                    (if (fehlend.size == 1) "fehlt" else "fehlen") +
                    " in der Übung: $namen.",
                fehler = true,
            )
        }

        // Die gewählten Fahrzeuge: Sinnbild, Typ und — wo es einen gibt — der Träger.
        offen.aufstellung.forEachIndexed { nr, id ->
            val vorlage = vorlagen[id]
            val fremd = !imKreis(vorlage)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(
                        farbe = if (fremd) Farben.HauchSignal else Farben.FlaecheHoch,
                        randfarbe = if (fremd) Farben.SignalTief else Farben.Rand,
                        ecke = 9.dp,
                        mitLichtkante = false,
                    )
                    .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
            ) {
                if (vorlage != null) Fahrzeugsinnbild(vorlage.organisation, groesse = 24.dp, typ = vorlage.typ)
                Text(
                    text = vorlage?.typ ?: id,
                    style = Schrift.MonoKlein,
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (vorlage != null && vorlage.hiOrg != "Keine" && vorlage.hiOrg.isNotBlank()) {
                    Traegermarke(vorlage.hiOrg)
                }
                Box(Modifier.weight(1f))
                Knopf(
                    "×",
                    { beiAendern { s -> s.copy(aufstellung = s.aufstellung.filterIndexed { i, _ -> i != nr }) } },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
            if (fremd) SehrLeise("Dieser Träger hat im gewählten Landkreis keine Wache.")
        }

        Ausklappzeile(
            titel = "Fahrzeug hinzufügen",
            offen = auswahlOffen,
            beiDruck = { auswahlOffen = !auswahlOffen },
        )

        if (auswahlOffen) {
            // Die Auswahl, auf den gewählten Kreis beschränkt: Ein Knopf, der ein
            // Fahrzeug in eine Übung stellt, in der es dann nicht erscheint, ist
            // kein Angebot, sondern eine Falle.
            fahrzeuggruppen(katalog?.fahrzeuge.orEmpty())
                .map { g -> g.copy(traeger = g.traeger.filter { traegerImKreis(it.hiOrg) }) }
                .filter { it.traeger.isNotEmpty() }
                .forEach { gruppe ->
                    Ueberschrift(gruppe.schluessel, Modifier.padding(top = Abstand.Klein))
                    gruppe.traeger.forEach { cluster ->
                        // Der Träger nur, wo es mehr als einen gibt.
                        if (gruppe.traeger.size > 1) Traegermarke(cluster.hiOrg, cluster.aufschrift)
                        cluster.fahrzeuge.forEach { f ->
                            Fahrzeugkarte(
                                vorlage = f,
                                aktiv = f.id in offen.aufstellung,
                                // Ein Fahrzeug darf mehrfach im Dienst stehen — zwei RTW sind zwei RTW.
                                beiDruck = { beiAendern { s -> s.copy(aufstellung = s.aufstellung + f.id) } },
                            )
                        }
                    }
                }
        }
    }
}

/** Eine Fahrzeugkarte der Auswahl — Sinnbild, Organisationsstreifen, Träger, Beschreibung. */
@Composable
private fun Fahrzeugkarte(vorlage: Fahrzeugvorlage, aktiv: Boolean, beiDruck: () -> Unit) {
    val orgfarbe = organisationsfarbeVon(vorlage.organisation)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (aktiv) Farben.HauchAmber else Farben.Flaeche,
                randfarbe = if (aktiv) Farben.Amber else Farben.Rand,
                ecke = 9.dp,
                mitLichtkante = false,
            )
            .clickable(onClick = beiDruck, role = Role.Button)
            .padding(end = Abstand.Normal),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(56.dp)
                .background(orgfarbe, Rundung.Winzig),
        )
        Fahrzeugsinnbild(vorlage.organisation, groesse = 38.dp, typ = vorlage.typ)
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f).padding(vertical = Abstand.Klein),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = vorlage.typ,
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = if (aktiv) Farben.Amber else Farben.Text,
                )
                if (vorlage.hiOrg != "Keine" && vorlage.hiOrg.isNotBlank()) Traegermarke(vorlage.hiOrg)
            }
            if (vorlage.beschreibung.isNotBlank()) {
                Text(
                    text = vorlage.beschreibung,
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SehrLeise(
                (listOf(vorlage.besatzung) + vorlage.faehigkeiten.take(3))
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                mono = true,
            )
        }
    }
}

/** Der Träger als Punkt und Name — „● Johanniter". */
@Composable
private fun Traegermarke(hiOrg: String, aufschrift: String = Uebungsaufschrift.traeger(hiOrg)) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(traegerfarbeVon(hiOrg).takeIf { hiOrg != "Keine" } ?: Farben.TextSehrLeise, CircleShape),
        )
        Text(
            text = aufschrift,
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
            color = Farben.TextLeise,
            maxLines = 1,
        )
    }
}

/** Eine Gruppe der Auswahl: Organisation und Kategorie, darunter die Träger. */
private data class Uebungsfahrzeuggruppe(
    val schluessel: String,
    val organisation: String,
    val traeger: List<Uebungstraegergruppe>,
)

private data class Uebungstraegergruppe(
    val hiOrg: String,
    val aufschrift: String,
    val fahrzeuge: List<Fahrzeugvorlage>,
)

/**
 * Die Auswahl, geordnet wie in Garage und Lobby — `gruppiereFahrzeuge` im Web:
 * Organisation und Kategorie in der Reihenfolge der Wache, darunter der Träger.
 */
private fun fahrzeuggruppen(liste: List<Fahrzeugvorlage>): List<Uebungsfahrzeuggruppe> {
    val organisationen = listOf("Feuerwehr", "Rettungsdienst", "Thw", "Polizei")

    fun rang(org: String, kategorie: String): Pair<Int, Int> {
        val o = organisationen.indexOf(org).let { if (it < 0) organisationen.size else it }
        val k = KATEGORIEFOLGE[org]?.indexOf(kategorie) ?: -1
        return o to (if (k < 0) Int.MAX_VALUE else k)
    }

    return liste
        .groupBy { "${Uebungsaufschrift.organisation(it.organisation)} · ${it.kategorie}" }
        .map { (schluessel, fahrzeuge) ->
            val erstes = fahrzeuge.first()
            Triple(schluessel, rang(erstes.organisation, erstes.kategorie), fahrzeuge)
        }
        .sortedWith(
            compareBy<Triple<String, Pair<Int, Int>, List<Fahrzeugvorlage>>>(
                { it.second.first },
                { it.second.second },
                { it.first },
            ),
        )
        .map { (schluessel, _, fahrzeuge) ->
            Uebungsfahrzeuggruppe(schluessel, fahrzeuge.first().organisation, traegergruppen(fahrzeuge))
        }
}

private fun traegergruppen(fahrzeuge: List<Fahrzeugvorlage>): List<Uebungstraegergruppe> {
    val nach = fahrzeuge.groupBy { it.hiOrg.ifBlank { "Keine" } }
    val alphabetisch: (List<Fahrzeugvorlage>) -> List<Fahrzeugvorlage> = { l -> l.sortedBy { it.typ.lowercase() } }

    if (nach.size <= 1) {
        return nach.map { (hiOrg, l) -> Uebungstraegergruppe(hiOrg, Uebungsaufschrift.traeger(hiOrg), alphabetisch(l)) }
    }

    return nach.entries
        .sortedBy { Uebungsaufschrift.traeger(it.key).ifBlank { "Sonstige" }.lowercase() }
        .map { (hiOrg, l) ->
            Uebungstraegergruppe(
                hiOrg,
                if (hiOrg == "Keine") "Öffentlicher Träger" else Uebungsaufschrift.traeger(hiOrg),
                alphabetisch(l),
            )
        }
}

/** Die Kategorien in der Reihenfolge, in der man sie auf der Wache sucht. */
private val KATEGORIEFOLGE: Map<String, List<String>> = mapOf(
    "Feuerwehr" to listOf(
        "Löschfahrzeuge", "Tanklöschfahrzeuge", "Hubrettung", "Rüst- und Technikzug", "Führung",
        "Logistik und Mannschaft", "Gefahrgut und Messtechnik", "Wasserversorgung",
        "Vegetationsbrand", "Wasserrettung", "Anhänger und Boote", "Abrollbehälter",
    ),
    "Rettungsdienst" to listOf(
        "Rettungsmittel", "Notarztzubringer", "Krankentransport", "Führung",
        "Massenanfall und Betreuung", "Wasserrettung", "Seenotrettung", "Bergrettung",
        "Rettungshunde",
    ),
    "Thw" to listOf("Bergung", "Fachgruppen", "Führung und Mannschaft"),
    "Polizei" to listOf(
        "Streifendienst", "Bundespolizei", "Einsatzeinheiten", "Kriminaldienst",
        "Spezialkräfte", "Luftunterstützung",
    ),
)

// ---------------------------------------------------------------- Zeitachse

/**
 * Die Zeitachse — alles ab Dienstbeginn gerechnet.
 *
 * <b>Die Schiene oben ist eine Übersicht, kein Bedienelement:</b> Sie zeigt, wie
 * dicht es wird. Geändert wird unten in der Liste, wo auch am Handy Platz ist.
 *
 * <b>Die Reihenfolge ist die Zeit.</b> Nach jeder Zeitänderung wird sortiert —
 * erst beim Loslassen des Reglers, sonst spränge der Eintrag unter dem Finger
 * weg. Der aufgeklappte Eintrag wandert dabei mit.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Zeitachse(
    offen: Szenario,
    katalog: Katalog?,
    beiAendern: ((Szenario) -> Szenario) -> Unit,
) {
    var bearbeitet by rememberSaveable { mutableIntStateOf(-1) }
    /** Der eben angelegte Eintrag — er wird ins Bild geholt (siehe unten). */
    var frisch by remember { mutableStateOf<Szenarioeintrag?>(null) }

    val zeitachse = offen.zeitachse
    val ende = maxOf(600, (zeitachse.maxOfOrNull { it.nachSekunden + 60 } ?: 0))

    /*
     * Alle Änderungen gehen als Umbau des *jüngsten* Stands hinaus, nicht als
     * fertiger Eintrag: Zwischen zwei Zeichnungen kommen beim Ziehen am Regler
     * mehrere Werte an, und ein Eintrag, der aus der letzten Zeichnung stammt,
     * schriebe den vorletzten Wert zurück.
     */
    fun ersetzen(nr: Int, umbau: (Szenarioeintrag) -> Szenarioeintrag) =
        beiAendern { s -> s.copy(zeitachse = s.zeitachse.mapIndexed { i, e -> if (i == nr) umbau(e) else e }) }

    /** Sortieren — und den aufgeklappten Eintrag mitnehmen. */
    fun sortieren() {
        val nr = bearbeitet
        beiAendern { s ->
            val auf = s.zeitachse.getOrNull(nr)
            val sortiert = s.zeitachse.sortedBy { it.nachSekunden }
            if (auf != null) bearbeitet = sortiert.indexOfFirst { it === auf }
            s.copy(zeitachse = sortiert)
        }
    }

    /**
     * Einen Eintrag anlegen — 120 Sekunden nach dem letzten, aufgeklappt und ins
     * Bild geholt. Am Handy sah es sonst aus, als täte der Knopf nichts: Der neue
     * Eintrag ging unterhalb der Bildkante auf.
     */
    fun anlegen(art: String) {
        beiAendern { s ->
            val nach = if (s.zeitachse.isEmpty()) 30 else s.zeitachse.maxOf { it.nachSekunden } + 120
            val lage = art == "Lage"
            val neu = Szenarioeintrag(
                art = art,
                nachSekunden = nach,
                stichwort = if (lage) "B2" else null,
                stichwortText = if (lage) "Zimmerbrand" else null,
                meldebild = if (lage) "" else null,
                adresse = if (lage) "" else null,
                organisation = if (lage) "Feuerwehr" else null,
                prioritaet = if (lage) 2 else null,
                stoerung = if (art == "Stoerung") "Fahrzeugdefekt" else null,
                wetter = if (art == "Wetterwechsel") "Sturm" else null,
            )
            val sortiert = (s.zeitachse + neu).sortedBy { it.nachSekunden }
            bearbeitet = sortiert.indexOfFirst { it === neu }
            frisch = neu
            s.copy(zeitachse = sortiert)
        }
    }

    Kasten(abstandInnen = Abstand.Klein) {
        Etikett("Zeitachse")
        SehrLeise(
            "Alles ab Dienstbeginn gerechnet — eine Übung, die um 19:20 statt 19:00 " +
                "anfängt, läuft genauso.",
        )

        // Die Schiene.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .flaeche(farbe = Farben.HauchHell, ecke = 6.dp, mitLichtkante = false),
        ) {
            val breite = maxWidth - 12.dp
            zeitachse.forEach { e ->
                val anteil = (e.nachSekunden.toFloat() / ende).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .offset(x = 6.dp + breite * anteil, y = 6.dp)
                        .width(3.dp)
                        .height(22.dp)
                        .background(schienenfarbe(e.art), Rundung.Winzig),
                )
            }
            Text(
                text = zeitText(ende),
                style = Schrift.MonoKlein,
                color = Farben.TextSehrLeise,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = Abstand.Klein),
            )
        }

        if (zeitachse.isEmpty()) {
            SehrLeise("Noch nichts auf der Zeitachse.")
        }

        zeitachse.forEachIndexed { nr, e ->
            val holer = remember(e) { BringIntoViewRequester() }
            LaunchedEffect(frisch) {
                if (frisch != null && frisch === e) {
                    holer.bringIntoView()
                    frisch = null
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(holer),
            ) {
                Eintragskopf(
                    eintrag = e,
                    offen = bearbeitet == nr,
                    beiDruck = { bearbeitet = if (bearbeitet == nr) -1 else nr },
                )

                if (bearbeitet == nr) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Abstand.Klein, bottom = Abstand.Klein),
                    ) {
                        Regler(
                            wert = e.nachSekunden,
                            beiAenderung = { neu -> ersetzen(nr) { it.copy(nachSekunden = neu) } },
                            von = 0,
                            bis = 3600,
                            schritt = 30,
                            etikett = "Minute nach Dienstbeginn: ${zeitText(e.nachSekunden)}",
                            beiLoslassen = { sortieren() },
                        )

                        when (e.art) {
                            "Lage" -> Lageeintrag(e, katalog) { umbau -> ersetzen(nr, umbau) }
                            "Stoerung" -> Stoerungseintrag(e) { umbau -> ersetzen(nr, umbau) }
                            else -> Wettereintrag(e) { umbau -> ersetzen(nr, umbau) }
                        }

                        Knopf(
                            "Eintrag löschen",
                            {
                                beiAendern { s ->
                                    s.copy(zeitachse = s.zeitachse.filterIndexed { i, _ -> i != nr })
                                }
                                bearbeitet = -1
                            },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                }
            }
        }

        Pillenreihe(modifier = Modifier.padding(top = Abstand.Klein)) {
            Knopf("+ Lage", { anlegen("Lage") }, kompakt = true)
            Knopf("+ Störung", { anlegen("Stoerung") }, kompakt = true)
            Knopf("+ Wetter", { anlegen("Wetterwechsel") }, kompakt = true)
        }
    }
}

@Composable
private fun Eintragskopf(eintrag: Szenarioeintrag, offen: Boolean, beiDruck: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .background(if (offen) Farben.FlaecheAktiv else Farben.FlaecheHoch, Rundung.Klein)
            .border(1.dp, if (offen) Farben.AmberTief else Farben.Rand, Rundung.Klein)
            .clickable(onClick = beiDruck, role = Role.Button)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Text(
            text = "+${zeitText(eintrag.nachSekunden)}",
            style = Schrift.MonoKlein,
            color = Farben.Amber,
        )
        Text(
            text = Uebungsaufschrift.art(eintrag.art),
            style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
            color = schienenfarbe(eintrag.art),
        )
        Text(
            text = eintragTitel(eintrag),
            style = Schrift.Klein,
            color = Farben.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(text = if (offen) "▾" else "▸", style = Schrift.Klein, color = Farben.TextSehrLeise)
    }
}

/** Die Felder einer Lage — genau die des Einsatzbogens. */
@Composable
private fun Lageeintrag(
    e: Szenarioeintrag,
    katalog: Katalog?,
    beiAendern: ((Szenarioeintrag) -> Szenarioeintrag) -> Unit,
) {
    var stichwortwahl by remember { mutableStateOf(false) }
    var ordnungOffen by remember { mutableStateOf(false) }

    val stichworte = katalog?.stichworte.orEmpty()
    val vorlage = stichworte.firstOrNull { it.stichwort == e.stichwort && it.stichwortText == e.stichwortText }

    Wahlfeld(
        etikett = "Stichwort aus dem Katalog",
        wert = vorlage?.let { "${it.stichwort} · ${it.stichwortText}" } ?: "— eigene Angaben —",
        beiDruck = { stichwortwahl = true },
        aktiv = stichworte.isNotEmpty(),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        Feld(
            wert = e.stichwort.orEmpty(),
            beiAenderung = { neu -> beiAendern { it.copy(stichwort = neu.take(12)) } },
            etikett = "Stichwort",
            stil = Schrift.MonoNormal,
            modifier = Modifier.weight(1f),
        )
        Feld(
            wert = e.stichwortText.orEmpty(),
            beiAenderung = { neu -> beiAendern { it.copy(stichwortText = neu.take(60)) } },
            etikett = "Klartext",
            modifier = Modifier.weight(1.6f),
        )
    }

    Feld(
        wert = e.adresse.orEmpty(),
        beiAenderung = { neu -> beiAendern { it.copy(adresse = neu.take(80)) } },
        etikett = "Adresse",
        platzhalter = "Straße und Hausnummer im gewählten Kreis",
    )
    Feld(
        wert = e.meldebild.orEmpty(),
        beiAenderung = { neu -> beiAendern { it.copy(meldebild = neu.take(240)) } },
        etikett = "Meldebild",
        platzhalter = "Was gemeldet wurde.",
        einzeilig = false,
        weiterTaste = ImeAction.Default,
    )

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Etikett("Organisation")
        Segment(
            seiten = Uebungsaufschrift.ORGANISATION.keys.toList(),
            gewaehlt = e.organisation ?: "Feuerwehr",
            beiWahl = { org -> beiAendern { it.copy(organisation = org) } },
            aufschrift = { Uebungsaufschrift.organisation(it) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Etikett("Dringlichkeit")
        Segment(
            seiten = Uebungsaufschrift.PRIORITAET.keys.toList(),
            gewaehlt = e.prioritaet ?: 2,
            beiWahl = { prio -> beiAendern { it.copy(prioritaet = prio) } },
            aufschrift = { Uebungsaufschrift.PRIORITAET[it].orEmpty() },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val funktionen = e.empfohleneFaehigkeiten.orEmpty()
    Ausklappzeile(
        titel = "Alarm- und Ausrückeordnung",
        zusatz = "(${e.empfohleneFahrzeuge ?: "—"} Fzg, ${funktionen.size} " +
            "${if (funktionen.size == 1) "Funktion" else "Funktionen"})",
        offen = ordnungOffen,
        beiDruck = { ordnungOffen = !ordnungOffen },
    )
    if (ordnungOffen) {
        Regler(
            wert = e.empfohleneFahrzeuge ?: 1,
            beiAenderung = { zahl -> beiAendern { it.copy(empfohleneFahrzeuge = zahl) } },
            von = 1,
            bis = 12,
            etikett = "Empfohlene Fahrzeuge: ${e.empfohleneFahrzeuge ?: 1}",
        )
        Etikett("Geforderte Funktionen")
        Pillenreihe {
            katalog?.faehigkeiten.orEmpty().forEach { f ->
                val an = f in funktionen
                Pille(
                    aufschrift = f,
                    an = an,
                    beiDruck = {
                        beiAendern { alt ->
                            val jetzt = alt.empfohleneFaehigkeiten.orEmpty()
                            alt.copy(empfohleneFaehigkeiten = if (f in jetzt) jetzt - f else jetzt + f)
                        }
                    },
                )
            }
        }
    }

    if (stichwortwahl) {
        Wahlblende(
            titel = "Stichwort aus dem Katalog",
            gruppen = listOf<Pair<String?, List<Stichwort?>>>(null to (listOf<Stichwort?>(null) + stichworte)),
            aufschrift = { s -> s?.let { "${it.stichwort} · ${it.stichwortText}" } ?: "— eigene Angaben —" },
            gewaehlt = vorlage,
            beiWahl = { s ->
                // Übernimmt Klartext, Ordnung und ein Meldebild — die eigenen
                // Angaben bleiben, wo „eigene Angaben" gewählt ist.
                if (s != null) {
                    beiAendern { alt ->
                        alt.copy(
                            stichwort = s.stichwort,
                            stichwortText = s.stichwortText,
                            organisation = s.organisation,
                            prioritaet = s.prioritaet,
                            empfohleneFahrzeuge = s.empfohleneFahrzeuge,
                            empfohleneFaehigkeiten = s.empfohleneFaehigkeiten,
                            meldebild = alt.meldebild?.takeIf { it.isNotBlank() }
                                ?: s.meldebilder.firstOrNull().orEmpty(),
                        )
                    }
                }
                stichwortwahl = false
            },
            beiSchliessen = { stichwortwahl = false },
            suchbar = true,
        )
    }
}

@Composable
private fun Stoerungseintrag(
    e: Szenarioeintrag,
    beiAendern: ((Szenarioeintrag) -> Szenarioeintrag) -> Unit,
) {
    Etikett("Was dazwischenkommt")
    Pillenreihe {
        Uebungsaufschrift.STOERUNGSART.forEach { (art, name) ->
            Pille(
                aufschrift = name,
                an = (e.stoerung ?: "Fahrzeugdefekt") == art,
                beiDruck = { beiAendern { it.copy(stoerung = art) } },
            )
        }
    }
    SehrLeise(
        "Woran es geschieht, sucht der Server sich zur Laufzeit — welches Fahrzeug in " +
            "Minute sieben auf der Anfahrt ist, weiß beim Schreiben niemand. Gibt es nichts " +
            "zu stören, passiert nichts.",
    )
}

@Composable
private fun Wettereintrag(
    e: Szenarioeintrag,
    beiAendern: ((Szenarioeintrag) -> Szenarioeintrag) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Etikett("Wetterlage")
        Segment(
            seiten = Uebungsaufschrift.WETTER.keys.toList(),
            gewaehlt = e.wetter ?: "Klar",
            beiWahl = { lage -> beiAendern { it.copy(wetter = lage) } },
            aufschrift = { Uebungsaufschrift.wetter(it) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Windfeld(e.windrichtung) { grad -> beiAendern { it.copy(windrichtung = grad) } }
}

/**
 * Die Windrichtung in Grad, 0–359. Ein Zahlenfeld wie im Web; was keine Zahl ist,
 * steht als „keine Angabe" da, statt eine Null zu behaupten.
 */
@Composable
private fun Windfeld(wert: Int?, beiAenderung: (Int?) -> Unit) {
    Feld(
        wert = wert?.toString().orEmpty(),
        beiAenderung = { roh ->
            val ziffern = roh.filter { it.isDigit() }.take(3)
            beiAenderung(ziffern.toIntOrNull()?.coerceIn(0, 359))
        },
        etikett = "Windrichtung (Grad)",
        tastatur = KeyboardType.Number,
        stil = Schrift.MonoNormal,
    )
}

// ------------------------------------------------------------------- Regeln

@Composable
private fun Regeln(
    e: Szenarioeinstellungen,
    beiAendern: ((Szenarioeinstellungen) -> Szenarioeinstellungen) -> Unit,
) {
    Kasten(abstandInnen = Abstand.Klein) {
        Etikett("Regeln dieser Übung")

        Hakenzeile(
            "Tagesalarmstärke — Ausrückzeiten gelten, ehrenamtliche brauchen länger",
            e.tagesalarmstaerke == true,
            { an -> beiAendern { it.copy(tagesalarmstaerke = an) } },
        )
        Hakenzeile(
            "Löschwasser — Tanks laufen leer, Versorgung muss stehen",
            e.loeschwasser == true,
            { an -> beiAendern { it.copy(loeschwasser = an) } },
        )
        Hakenzeile(
            "Tätigkeiten an der Einsatzstelle",
            e.einsatzarbeit == true,
            { an -> beiAendern { it.copy(einsatzarbeit = an) } },
        )
        Hakenzeile(
            "Wiederherstellung der Einsatzbereitschaft",
            e.wiederherstellung == true,
            { an -> beiAendern { it.copy(wiederherstellung = an) } },
        )
        Hakenzeile(
            "Sonderobjekte mit Einsatzplan",
            e.sonderobjekte == true,
            { an -> beiAendern { it.copy(sonderobjekte = an) } },
        )
        Hakenzeile(
            "Notrufe kommen als Telefonanruf herein",
            e.telefonischeLeitstelle == true,
            { an -> beiAendern { it.copy(telefonischeLeitstelle = an) } },
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.padding(top = Abstand.Klein),
        ) {
            Etikett("Wetter bei Dienstbeginn")
            Segment(
                seiten = Uebungsaufschrift.WETTER.keys.toList(),
                gewaehlt = e.wetter ?: "Klar",
                beiWahl = { lage -> beiAendern { it.copy(wetter = lage) } },
                aufschrift = { Uebungsaufschrift.wetter(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Etikett("Jahreszeit")
            Pillenreihe {
                Pille(
                    aufschrift = "Wie gerade draußen",
                    an = e.jahreszeit == null,
                    beiDruck = { beiAendern { it.copy(jahreszeit = null) } },
                )
                Uebungsaufschrift.JAHRESZEIT.forEach { (zeit, name) ->
                    Pille(
                        aufschrift = name,
                        an = e.jahreszeit == zeit,
                        beiDruck = { beiAendern { it.copy(jahreszeit = zeit) } },
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------- Kleinteile

/** Eine aufklappbare Zeile — das `<details>` des Webs. */
@Composable
private fun Ausklappzeile(
    titel: String,
    offen: Boolean,
    beiDruck: () -> Unit,
    zusatz: String? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clickable(onClick = beiDruck, role = Role.Button),
    ) {
        Text(text = if (offen) "▾" else "▸", style = Schrift.Klein, color = Farben.Amber)
        Text(text = titel, style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text)
        if (zusatz != null) {
            Text(
                text = zusatz,
                style = Schrift.Winzig,
                color = Farben.TextSehrLeise,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

/** Ein Satz über der Seite — Rückmeldung oder Fehler, der Fehler mit rotem Rand links. */
@Composable
private fun Hinweiszeile(text: String, fehler: Boolean = false) {
    Text(
        text = text,
        style = Schrift.Klein,
        color = if (fehler) Farben.SignalHell else Farben.Text,
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.HauchHell, Rundung.Winzig)
            .drawBehind {
                if (fehler) drawRect(Farben.Signal, size = size.copy(width = 3.dp.toPx()))
            }
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    )
}

private fun schienenfarbe(art: String) = when (art) {
    "Stoerung" -> Farben.Signal
    "Wetterwechsel" -> androidx.compose.ui.graphics.Color(0xFF7FA8FF)
    else -> Farben.Amber
}

/** Minuten und Sekunden — „12:30". */
private fun zeitText(sekunden: Int): String = "${sekunden / 60}:${(sekunden % 60).toString().padStart(2, '0')}"

private fun eintragTitel(e: Szenarioeintrag): String = when (e.art) {
    "Lage" -> "${e.stichwort ?: "—"} ${e.stichwortText.orEmpty()}".trim()
    "Stoerung" -> Uebungsaufschrift.stoerung(e.stoerung)
    else -> "Wetter: ${Uebungsaufschrift.wetter(e.wetter)}"
}
