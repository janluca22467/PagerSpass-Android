package de.pagerspass.pagerspass.mobil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import de.pagerspass.pagerspass.ansichten.BegleiterKopplung
import de.pagerspass.pagerspass.ansichten.IconBibliothekSeite
import de.pagerspass.pagerspass.ansichten.IconEditorSeite
import de.pagerspass.pagerspass.ansichten.LehrgangSeite
import de.pagerspass.pagerspass.ansichten.LeitstellenbauSeite
import de.pagerspass.pagerspass.ansichten.UebungenSeite
import de.pagerspass.pagerspass.ansichten.UebungseditorSeite
import de.pagerspass.pagerspass.netz.Konto

/**
 * Die Wege jenseits der sechs Reiter — Übungen, Lehrgang, Leitstellenbau, die
 * Fahrzeug-Icons von World und der Begleiter-Link.
 *
 * <b>Ein eigener Baustein am Rand der Navigation</b>, damit `PagerSpassApp` nur
 * eine Zeile dafür trägt. Alle Wege hängen im Menü „Dienst" (die Leiste bleibt
 * dort markiert, siehe `weg`) — sie beginnen auf dem Startbildschirm.
 *
 * <b>Premium und Einweisung werden hier noch einmal geprüft</b>, wie im Router
 * des Webs: Ein Menüeintrag allein wäre keine Zugangssperre. Ohne Premium führt
 * der Weg zu den Icons in den Shop, mit offener Einweisung zurück zum Start.
 */
object Extraswege {
    const val UEBUNGEN = "uebungen"
    const val UEBUNG = "uebungen/{id}"
    const val LEHRGANG = "lehrgang"
    const val LEITSTELLENBAU = "leitstellenbau"
    const val ICONS = "welt/icons"
    const val ICONPACK = "welt/icons/{packId}"
    const val FUNK = "funk/{token}"

    /** Die Kennung einer ungespeicherten Übung im Weg — der Editor legt sie neu an. */
    const val NEUE_UEBUNG = "neu"

    fun uebung(id: String) = "uebungen/$id"
    fun iconpack(packId: String) = "welt/icons/$packId"
    fun funk(token: String) = "funk/$token"

    /** Zu welchem Weg der Leiste diese Seiten gehören — alle zum Dienst. */
    fun weg(route: String?): Weg? = when (route) {
        UEBUNGEN, UEBUNG, LEHRGANG, LEITSTELLENBAU, ICONS, ICONPACK, FUNK -> Weg.Dienst
        else -> null
    }
}

/**
 * Die Ziele der Extras in den `NavHost` hängen.
 *
 * @param zumShop führt in den Shop — der Ausweg ohne Premium.
 * @param zumStart führt zurück zum Startbildschirm — der Ausweg mit offener Einweisung.
 * @param zurWelt der Weg „Zurück zur Welt" aus der Icon-Bibliothek.
 * @param zumDienstbuch die Prüfungsschicht im Dienstbuch ansehen.
 */
fun NavGraphBuilder.extrasWege(
    steuerung: NavHostController,
    unterrand: Dp,
    sitzung: Sitzung,
    runde: Runde,
    begleiter: Begleiter,
    zumShop: () -> Unit,
    zumStart: () -> Unit,
    zurWelt: () -> Unit,
    zumDienstbuch: () -> Unit,
) {
    // ------------------------------------------------------------- Übungen

    composable(Extraswege.UEBUNGEN) {
        val werk: Uebungen = viewModel()
        val stand by werk.stand.collectAsStateWithLifecycle()
        val sitzungsstand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val name = sitzungsstand.konto?.anzeigename.orEmpty()

        UebungenSeite(
            unterrand = unterrand,
            stand = stand,
            katalogBereit = daten.katalog.inhalt != null,
            katalogFehler = daten.katalog.fehler?.let {
                "Der Fahrzeugkatalog ist gerade nicht erreichbar. Lade die Seite neu."
            },
            beiLaden = {
                sitzung.katalogSicherstellen()
                werk.meldungenWegnehmen()
                werk.listeLaden()
            },
            beiNeu = { steuerung.navigate(Extraswege.uebung(Extraswege.NEUE_UEBUNG)) },
            beiUebernehmen = { code, danach -> werk.uebernehmen(code, danach) },
            // Leiten: Die Runde entsteht genauso, der Ausbilder betritt sie aber
            // als Zuschauer — und bekommt dort den Regieplatz, weil die Übung ihm
            // gehört.
            beiLeiten = { id ->
                werk.rundeAnlegen(id) { code -> runde.zuschauen(code, name.ifBlank { "Übungsleitung" }) }
            },
            beiFahren = { id -> werk.rundeAnlegen(id) { code -> runde.beitreten(code, name) } },
            beiBearbeiten = { id -> steuerung.navigate(Extraswege.uebung(id)) },
            beiLoeschen = { id -> werk.loeschen(id) },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable(Extraswege.UEBUNG) { eintrag ->
        val id = eintrag.arguments?.getString("id").orEmpty()
        val werk: Uebungen = viewModel()
        val stand by werk.stand.collectAsStateWithLifecycle()
        val sitzungsstand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val name = sitzungsstand.konto?.anzeigename.orEmpty()
        val katalog = daten.katalog.inhalt

        LaunchedEffect(Unit) { sitzung.katalogSicherstellen() }
        // Eine neue Übung braucht den Katalog (Aufstellung und Kreis kommen von
        // dort) — sie entsteht, sobald er da ist.
        LaunchedEffect(id, katalog != null) {
            if (id == Extraswege.NEUE_UEBUNG) {
                if (katalog != null) werk.neu(katalog)
            } else {
                werk.oeffnen(id)
            }
        }

        UebungseditorSeite(
            unterrand = unterrand,
            stand = stand,
            katalog = katalog,
            beiAendern = { werk.aendern(it) },
            beiSichern = { werk.sichern() },
            beiLeiten = { sid ->
                werk.rundeAnlegen(sid) { code -> runde.zuschauen(code, name.ifBlank { "Übungsleitung" }) }
            },
            beiFahren = { sid -> werk.rundeAnlegen(sid) { code -> runde.beitreten(code, name) } },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    // ------------------------------------------------------------ Lehrgang

    composable(Extraswege.LEHRGANG) {
        val werk: Lehrgaenge = viewModel()
        val stand by werk.stand.collectAsStateWithLifecycle()
        val sitzungsstand by sitzung.stand.collectAsStateWithLifecycle()
        val browser = LocalUriHandler.current
        val name = sitzungsstand.konto?.anzeigename.orEmpty()

        LehrgangSeite(
            unterrand = unterrand,
            stand = stand,
            laeuft = sitzungsstand.laeuft,
            beiLaden = { werk.laden() },
            beiLesen = { lehrgang, modul ->
                val seite = modul.wikiseite ?: return@LehrgangSeite
                // Erst öffnen, dann abhaken: Wer den Weg nach draußen nimmt, kommt
                // vielleicht nicht zurück — der Haken soll trotzdem sitzen.
                runCatching { browser.openUri("https://wiki.pagerspass.de/$seite") }
                werk.gelesen(lehrgang, modul)
            },
            // Die Lektion ist die Ausbildungsschicht — derselbe Weg wie vom Start.
            beiLektion = { sitzung.ausbildungEroeffnen() },
            beiPruefung = { lehrgang, modul ->
                werk.pruefung(lehrgang, modul) { code -> runde.beitreten(code, name.ifBlank { "Prüfling" }) }
            },
            beiSchicht = { zumDienstbuch() },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    // ------------------------------------------------------ Leitstellenbau

    composable(Extraswege.LEITSTELLENBAU) {
        LeitstellenbauSeite(unterrand = unterrand, beiZurueck = { steuerung.popBackStack() })
    }

    // -------------------------------------------------------- World-Icons

    composable(Extraswege.ICONS) {
        Weltzugang(sitzung, zumShop, zumStart) {
            val werk: Iconwerk = viewModel()
            val stand by werk.stand.collectAsStateWithLifecycle()

            IconBibliothekSeite(
                unterrand = unterrand,
                stand = stand,
                beiLaden = { werk.bibliothekLaden() },
                beiBearbeiten = { steuerung.navigate(Extraswege.iconpack(it)) },
                beiAnlegen = { name ->
                    werk.anlegen(name) { neu -> steuerung.navigate(Extraswege.iconpack(neu)) }
                },
                beiUmbenennen = { pack, name -> werk.umbenennen(pack, name) },
                beiLoeschen = { werk.loeschen(it) },
                beiTeilen = { werk.teilen(it) },
                beiUebernehmen = { code, danach -> werk.uebernehmen(code, danach) },
                beiImport = { werk.importieren(it) },
                beiZurWelt = zurWelt,
            )
        }
    }

    composable(Extraswege.ICONPACK) { eintrag ->
        val packId = eintrag.arguments?.getString("packId").orEmpty()
        Weltzugang(sitzung, zumShop, zumStart) {
            val werk: Iconwerk = viewModel()
            val stand by werk.stand.collectAsStateWithLifecycle()

            IconEditorSeite(
                unterrand = unterrand,
                stand = stand,
                beiLaden = { werk.editorLaden(packId) },
                beiWaehlen = { werk.waehlen(it) },
                beiHochladen = { werk.hochladen(it) },
                beiDrehung = { werk.drehungUmlegen() },
                beiBlaulicht = { punkte, wahl, meldung -> werk.blaulichtSetzen(punkte, wahl, meldung) },
                beiLichtwahl = { werk.lichtWaehlen(it) },
                beiEntfernen = { werk.entfernen() },
                beiFehler = { werk.fehlerSetzen(it) },
                beiZurueck = { steuerung.popBackStack() },
            )
        }
    }

    // ------------------------------------------------------ Begleiter-Link

    /*
     * `…/mobile/funk/<token>` als eigener Weg — der Link, den der Rechner unter
     * dem QR-Code anbietet. Er koppelt sofort; ist die Kopplung da, übernimmt der
     * Begleiterrahmen den ganzen Schirm. Die Verknüpfung mit dem System (Deep
     * Link im Manifest) hängt nur noch `steuerung.navigate(Extraswege.funk(token))`
     * oder `begleiter.linkOeffnen(adresse)` an.
     */
    composable(Extraswege.FUNK) { eintrag ->
        val token = eintrag.arguments?.getString("token").orEmpty()
        val begleiterstand by begleiter.stand.collectAsStateWithLifecycle()

        LaunchedEffect(token) { if (token.isNotBlank()) begleiter.linkOeffnen(token) }

        BegleiterKopplung(
            unterrand = unterrand,
            laeuft = begleiterstand.laeuft,
            fehler = begleiterstand.fehler,
            beiKoppeln = { begleiter.koppeln(it) },
            beiZurueck = { steuerung.popBackStack() },
            // Der Link selbst braucht kein Premium — im Web nicht einmal ein Konto:
            // Der Token gehört einem Platz, den ein Premium-Konto freigegeben hat.
            // Die Schranke steht nur vor dem Scanner (Konto → Begleiter).
            premium = true,
        )
    }
}

/**
 * Die Schranke vor World — Premium und Einweisung, wie `meta.premium` und
 * `meta.nachEinweisung` im Router des Webs. Solange das Konto noch nicht da ist,
 * steht nichts; danach entweder die Seite oder der Ausweg.
 */
@Composable
private fun Weltzugang(
    sitzung: Sitzung,
    zumShop: () -> Unit,
    zumStart: () -> Unit,
    inhalt: @Composable () -> Unit,
) {
    val stand by sitzung.stand.collectAsStateWithLifecycle()
    val konto: Konto? = stand.konto
    val frei = konto != null && konto.premiumAktiv && !konto.einweisungOffen

    LaunchedEffect(konto?.premiumAktiv, konto?.einweisungOffen) {
        when {
            konto == null -> Unit
            !konto.premiumAktiv -> zumShop()
            konto.einweisungOffen -> zumStart()
        }
    }

    if (frei) inhalt()
}
