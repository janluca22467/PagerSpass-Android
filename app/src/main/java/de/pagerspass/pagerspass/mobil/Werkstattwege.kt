package de.pagerspass.pagerspass.mobil

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import de.pagerspass.pagerspass.ansichten.LehrgangSeite
import de.pagerspass.pagerspass.ansichten.LeitstelleEditorSeite
import de.pagerspass.pagerspass.ansichten.LeitstellenbauSeite
import de.pagerspass.pagerspass.ansichten.RundenvorlageEditorSeite
import de.pagerspass.pagerspass.ansichten.RundenvorlagenSeite
import de.pagerspass.pagerspass.ansichten.SchichtSeite
import de.pagerspass.pagerspass.ansichten.UebungEditorSeite
import de.pagerspass.pagerspass.ansichten.UebungenSeite
import de.pagerspass.pagerspass.ansichten.neueLeitstelle
import de.pagerspass.pagerspass.ansichten.neueUebung

/**
 * Die Unterseiten der Werkstatt — Lehrgang, Übungen, Leitstellenbau,
 * Rundenvorlagen und eine Schicht aus dem Archiv.
 *
 * <b>Eigene Datei und nicht im Rahmen</b>, damit `PagerSpassApp` nur eine
 * Zeile dafür trägt. Jede Seite holt sich ihren Stand selbst aus der Werkstatt
 * (und den Katalog aus der Sitzung) — über `collectAsStateWithLifecycle`,
 * damit sie neu zeichnet, wenn er sich ändert.
 *
 * <b>Eine Runde betritt man über den Rahmen.</b> Die Werkstatt holt den
 * Raumcode; `beitreten` bzw. `zuschauen` kommen von dort, wo die Runde lebt.
 */
fun NavGraphBuilder.werkstattwege(
    platz: Dp,
    steuerung: NavHostController,
    werkstatt: Werkstatt,
    sitzung: Sitzung,
    beitreten: (String) -> Unit,
    zuschauen: (String) -> Unit,
    imBrowser: (String) -> Unit,
) {
    composable(UNTERSEITE_LEHRGANG) {
        val werk by werkstatt.stand.collectAsStateWithLifecycle()
        val sitzungsstand by sitzung.stand.collectAsStateWithLifecycle()
        val einweisungOffen = sitzungsstand.konto?.einweisungOffen == true
        // Der Server streicht die Einweisungspflicht, sobald ein Grundlagenlehrgang
        // bestanden ist. Die Sitzung hält das Konto von vorher; ohne Nachfragen
        // blieben Leiste und Startseite bis zum nächsten Start verschlossen.
        val ersetzt = werk.lehrgaenge.inhalt.orEmpty().any { it.ersetztEinweisung && it.bestanden }
        LaunchedEffect(einweisungOffen, ersetzt) {
            if (einweisungOffen && ersetzt) sitzung.kontoAuffrischen()
        }
        LehrgangSeite(
            einweisungOffen = einweisungOffen,
            beiUebungGeschafft = { l, m -> werkstatt.uebungGeschafft(l, m) },
            beiTheorie = { l, m, antworten, beiUrteil -> werkstatt.theorieAbgeben(l, m, antworten, beiUrteil) },
            unterrand = platz,
            stand = werk,
            beiLaden = { werkstatt.lehrgaengeLaden(neu = it) },
            beiLesen = { l, m ->
                // Erst die Seite, dann der Haken — wie im Web. Wer „Lesen"
                // tippt und zurückkommt, findet das Modul abgehakt.
                m.wikiseite?.let { imBrowser("https://wiki.pagerspass.de/$it") }
                werkstatt.modulGelesen(l, m)
            },
            beiLektion = { sitzung.ausbildungEroeffnen() },
            beiPruefung = { l, m -> werkstatt.pruefungFahren(l, m, beitreten) },
            beiSchicht = { steuerung.navigate("$UNTERSEITE_SCHICHT/$it") },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable(UNTERSEITE_UEBUNGEN) {
        val werk by werkstatt.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { sitzung.katalogSicherstellen() }
        UebungenSeite(
            unterrand = platz,
            stand = werk,
            katalogBereit = daten.katalog.inhalt != null,
            beiLaden = { werkstatt.szenarienLaden(neu = it) },
            beiNeu = { steuerung.navigate("$UNTERSEITE_UEBUNG/$NEU") },
            beiOeffnen = { steuerung.navigate("$UNTERSEITE_UEBUNG/$it") },
            beiLoeschen = { werkstatt.uebungLoeschen(it) },
            beiUebernehmen = { werkstatt.uebungUebernehmen(it) },
            beiFahren = { werkstatt.uebungRunde(it, beitreten) },
            beiLeiten = { werkstatt.uebungRunde(it, zuschauen) },
            beiZurueck = { steuerung.popBackStack() },
            landkreise = daten.katalog.inhalt?.landkreise.orEmpty(),
        )
    }

    composable("$UNTERSEITE_UEBUNG/{id}") { eintrag ->
        val werk by werkstatt.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val id = eintrag.arguments?.getString("id").orEmpty()
        // Einmal je Besuch und nicht bei jedem Neuaufbau: Nach dem Drehen des
        // Geräts stünde sonst ein frischer Entwurf da, wo eben noch einer war.
        var geoeffnet by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(id, daten.katalog.inhalt != null) {
            sitzung.katalogSicherstellen()
            if (geoeffnet || (id == NEU && daten.katalog.inhalt == null)) return@LaunchedEffect
            geoeffnet = true
            werkstatt.uebungOeffnen(id.takeIf { it != NEU }) { neueUebung(daten.katalog.inhalt) }
        }
        UebungEditorSeite(
            unterrand = platz,
            stand = werk,
            katalog = daten.katalog.inhalt,
            beiAendern = { werkstatt.uebungAendern(it) },
            beiSichern = { werkstatt.uebungSichern() },
            beiFahren = { werkstatt.uebungRunde(it, beitreten) },
            beiLeiten = { werkstatt.uebungRunde(it, zuschauen) },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable(UNTERSEITE_LEITSTELLENBAU) {
        val werk by werkstatt.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { sitzung.katalogSicherstellen() }
        LeitstellenbauSeite(
            unterrand = platz,
            stand = werk,
            katalogBereit = daten.katalog.inhalt != null,
            beiLaden = { werkstatt.vorlagenLaden(neu = it) },
            beiNeu = { steuerung.navigate("$UNTERSEITE_LEITSTELLE/$NEU") },
            beiOeffnen = { steuerung.navigate("$UNTERSEITE_LEITSTELLE/$it") },
            beiLoeschen = { werkstatt.leitstelleLoeschen(it) },
            beiUebernehmen = { werkstatt.leitstelleUebernehmen(it) },
            beiEroeffnen = { werkstatt.sandkastenEroeffnen(it, beitreten) },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable("$UNTERSEITE_LEITSTELLE/{id}") { eintrag ->
        val werk by werkstatt.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val id = eintrag.arguments?.getString("id").orEmpty()
        var geoeffnet by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(id, daten.katalog.inhalt != null) {
            sitzung.katalogSicherstellen()
            if (geoeffnet || (id == NEU && daten.katalog.inhalt == null)) return@LaunchedEffect
            geoeffnet = true
            werkstatt.leitstelleOeffnen(id.takeIf { it != NEU }) { neueLeitstelle(daten.katalog.inhalt) }
        }
        LeitstelleEditorSeite(
            unterrand = platz,
            stand = werk,
            katalog = daten.katalog.inhalt,
            beiAendern = { werkstatt.leitstelleAendern(it) },
            beiKreis = { werkstatt.kreiswachenLaden(it) },
            beiSichern = { werkstatt.leitstelleSichern() },
            beiEroeffnen = { werkstatt.sandkastenEroeffnen(it, beitreten) },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable(UNTERSEITE_RUNDENVORLAGEN) {
        val werk by werkstatt.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        RundenvorlagenSeite(
            unterrand = platz,
            stand = werk,
            landkreise = daten.katalog.inhalt?.landkreise.orEmpty(),
            beiLaden = { werkstatt.rundenvorlagenLaden(neu = it) },
            beiStarten = { werkstatt.rundenvorlageStarten(it, beitreten) },
            beiBearbeiten = { steuerung.navigate("$UNTERSEITE_RUNDENVORLAGE/${it.id}") },
            beiLoeschen = { werkstatt.rundenvorlageLoeschen(it) },
            beiUebernehmen = { werkstatt.rundenvorlageUebernehmen(it) },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable("$UNTERSEITE_RUNDENVORLAGE/{id}") { eintrag ->
        val werk by werkstatt.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val id = eintrag.arguments?.getString("id").orEmpty()
        var geoeffnet by rememberSaveable { mutableStateOf(false) }
        RundenvorlageEditorSeite(
            id = id,
            unterrand = platz,
            stand = werk,
            fahrzeuge = daten.katalog.inhalt?.fahrzeuge.orEmpty(),
            beiLaden = {
                if (!geoeffnet) {
                    geoeffnet = true
                    werkstatt.vorlageninhaltLaden(id)
                }
            },
            beiErneut = { werkstatt.vorlageninhaltLaden(id) },
            beiAendern = { werkstatt.vorlageninhaltAendern(it) },
            beiSichern = { werkstatt.vorlageninhaltSichern(id) { steuerung.popBackStack() } },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable("$UNTERSEITE_SCHICHT/{code}") { eintrag ->
        val werk by werkstatt.stand.collectAsStateWithLifecycle()
        SchichtSeite(
            code = eintrag.arguments?.getString("code").orEmpty(),
            unterrand = platz,
            stand = werk,
            werkstatt = werkstatt,
            beiZurueck = { steuerung.popBackStack() },
        )
    }
}

internal const val UNTERSEITE_LEHRGANG = "lehrgang"
internal const val UNTERSEITE_UEBUNGEN = "uebungen"
internal const val UNTERSEITE_UEBUNG = "uebung"
internal const val UNTERSEITE_LEITSTELLENBAU = "leitstellenbau"
internal const val UNTERSEITE_LEITSTELLE = "leitstelle"
internal const val UNTERSEITE_RUNDENVORLAGEN = "rundenvorlagen"
internal const val UNTERSEITE_RUNDENVORLAGE = "rundenvorlage"
internal const val UNTERSEITE_SCHICHT = "schicht"

/** Die Kennung, unter der der Editor einen frischen Entwurf öffnet. */
private const val NEU = "neu"

/** Zu welchem Weg der Leiste eine Werkstattseite gehört — die Schicht zum Buch, der Rest zum Dienst. */
internal fun werkstattweg(route: String?): Weg? = when {
    route == null -> null
    route.startsWith(UNTERSEITE_SCHICHT) -> Weg.Dienstbuch
    route == UNTERSEITE_LEHRGANG || route == UNTERSEITE_UEBUNGEN ||
        route.startsWith("$UNTERSEITE_UEBUNG/") || route == UNTERSEITE_LEITSTELLENBAU ||
        route.startsWith(UNTERSEITE_LEITSTELLE) || route.startsWith(UNTERSEITE_RUNDENVORLAGE) -> Weg.Dienst
    else -> null
}

/**
 * Wohin eine Kachel der Verwaltung mit der Aktion `route` führt — als Weg der
 * App, wo es ihn gibt. `null` heißt: Den gibt es nur im Web.
 */
internal fun werkstattziel(ziel: String): String? {
    val pfad = ziel.trim().substringBefore('?').trimEnd('/')
    return when {
        pfad == "/lehrgang" -> UNTERSEITE_LEHRGANG
        pfad == "/uebungen" -> UNTERSEITE_UEBUNGEN
        pfad == "/leitstellenbau" -> UNTERSEITE_LEITSTELLENBAU
        pfad.startsWith("/dienstbuch") -> Weg.Dienstbuch.adresse
        else -> null
    }
}
