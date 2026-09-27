package de.pagerspass.pagerspass.mobil

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import de.pagerspass.pagerspass.ansichten.ArchivDebriefingSeite
import de.pagerspass.pagerspass.ansichten.Buchreiter
import de.pagerspass.pagerspass.ansichten.DienstbuchAbzeichen
import de.pagerspass.pagerspass.ansichten.DienstbuchAuswertung
import de.pagerspass.pagerspass.ansichten.DienstbuchGarage
import de.pagerspass.pagerspass.ansichten.DienstbuchLaufbahn
import de.pagerspass.pagerspass.ansichten.DienstbuchSchichten
import de.pagerspass.pagerspass.ansichten.DienstbuchUebersicht
import de.pagerspass.pagerspass.ansichten.Ladenbereich
import de.pagerspass.pagerspass.ansichten.Ladengriffe
import de.pagerspass.pagerspass.ansichten.LadenSeite
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.netz.Server

/**
 * Die Wege von Dienstbuch und Shop — sechs Seiten Buch, fünf Bereiche Laden und
 * die Nachbesprechung einer archivierten Schicht.
 *
 * <b>Hier und nicht im Rahmen</b>, damit `PagerSpassApp` eine Zeile trägt statt
 * vierzehn Ziele. Jede Seite holt sich ihren Stand selbst aus der Sitzung; das
 * ist dieselbe Naht wie bei den übrigen Wegen, nur eine Datei weiter.
 *
 * <b>Die Wege heißen wie im Web</b> (`/dienstbuch/schichten`,
 * `/shop?bereich=premium` → `shop/premium`). Die Tableiste markiert auf allen den
 * Weg, aus dem sie kommen — `unterseitenweg` im Rahmen kennt die Vorsilben.
 */
fun NavGraphBuilder.dienstbuchUndShopWege(
    sitzung: Sitzung,
    steuerung: NavHostController,
    unterrand: Dp,
) {
    /** Ein Reiter des Buchs — ersetzt die Seite, statt einen Verlauf zu stapeln. */
    fun reiter(r: Buchreiter) {
        steuerung.navigate(r.weg) {
            popUpTo(Buchreiter.Uebersicht.weg)
            launchSingleTop = true
        }
    }

    fun schicht(code: String) = steuerung.navigate("$SCHICHT_WEG/$code")

    fun ersteSchicht() = wegWaehlen(steuerung, Weg.Dienst)

    // ------------------------------------------------------------ Dienstbuch

    composable(Buchreiter.Uebersicht.weg) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val buch by sitzung.dienstbuch.stand.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            sitzung.katalogSicherstellen()
            sitzung.garageLaden()
            sitzung.dienstbuch.wachenkurvenLaden()
        }
        DienstbuchUebersicht(
            unterrand = unterrand,
            konto = stand.konto,
            stand = buch,
            garage = daten.garage.inhalt?.stand,
            fahrzeuge = daten.katalog.inhalt?.fahrzeuge.orEmpty(),
            beiReiter = ::reiter,
            beiLaden = { sitzung.dienstbuch.laden() },
            beiSchicht = ::schicht,
            beiErsteSchicht = ::ersteSchicht,
        )
    }

    composable(Buchreiter.Schichten.weg) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val buch by sitzung.dienstbuch.stand.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { sitzung.katalogSicherstellen() }
        DienstbuchSchichten(
            unterrand = unterrand,
            konto = stand.konto,
            stand = buch,
            fahrzeuge = daten.katalog.inhalt?.fahrzeuge.orEmpty(),
            beiReiter = ::reiter,
            beiLaden = { sitzung.dienstbuch.laden() },
            beiSchicht = ::schicht,
            beiBuchungen = { sitzung.dienstbuch.buchungen(it) },
            beiErsteSchicht = ::ersteSchicht,
        )
    }

    composable(Buchreiter.Laufbahn.weg) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val buch by sitzung.dienstbuch.stand.collectAsStateWithLifecycle()
        // Ohne Freundesliste stünde „+ Freund" auch an denen, die es längst sind.
        LaunchedEffect(Unit) { sitzung.freundeLaden() }
        DienstbuchLaufbahn(
            unterrand = unterrand,
            konto = stand.konto,
            stand = buch,
            freunde = daten.freunde.inhalt.orEmpty(),
            beiReiter = ::reiter,
            beiLaden = { sitzung.dienstbuch.laden() },
            beiAnfragen = { wen ->
                sitzung.dienstbuch.freundAnfragen(wen).also { if (it.isSuccess) sitzung.freundeLaden(neu = true) }
            },
        )
    }

    composable(Buchreiter.Garage.weg) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        DienstbuchGarage(
            unterrand = unterrand,
            konto = stand.konto,
            garage = daten.garage,
            katalog = daten.katalog.inhalt?.fahrzeuge.orEmpty(),
            beiReiter = ::reiter,
            beiLaden = {
                sitzung.katalogSicherstellen()
                sitzung.garageLaden(neu = true)
            },
            beiShop = { steuerung.navigate(Ladenbereich.Fahrzeuge.weg) },
        )
    }

    composable(Buchreiter.Abzeichen.weg) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val buch by sitzung.dienstbuch.stand.collectAsStateWithLifecycle()
        DienstbuchAbzeichen(
            unterrand = unterrand,
            konto = stand.konto,
            stand = buch,
            beiReiter = ::reiter,
            beiLaden = { sitzung.dienstbuch.laden() },
            beiVitrineLaden = { sitzung.dienstbuch.vitrineLaden() },
            beiVitrine = { sitzung.dienstbuch.vitrineUmschalten(it) },
            beiProfil = { steuerung.navigate(PROFIL_WEG) },
        )
    }

    composable(Buchreiter.Auswertung.weg) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val buch by sitzung.dienstbuch.stand.collectAsStateWithLifecycle()
        DienstbuchAuswertung(
            unterrand = unterrand,
            konto = stand.konto,
            stand = buch,
            beiReiter = ::reiter,
            beiLaden = { sitzung.dienstbuch.auswertungLaden() },
            beiPremium = { steuerung.navigate(Ladenbereich.Premium.weg) },
        )
    }

    composable("$SCHICHT_WEG/{code}") { eintrag ->
        val code = eintrag.arguments?.getString("code").orEmpty()
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val buch by sitzung.dienstbuch.stand.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { sitzung.freundeLaden() }
        ArchivDebriefingSeite(
            unterrand = unterrand,
            code = code,
            konto = stand.konto,
            stand = buch.runde,
            freunde = daten.freunde.inhalt.orEmpty(),
            beiLaden = { sitzung.dienstbuch.rundeLaden(code) },
            beiZurueck = {
                if (!steuerung.popBackStack()) reiter(Buchreiter.Schichten)
            },
            beiAnfragen = { wen ->
                sitzung.dienstbuch.freundAnfragen(wen).also { if (it.isSuccess) sitzung.freundeLaden(neu = true) }
            },
            beiPremium = { steuerung.navigate(Ladenbereich.Premium.weg) },
        )
    }

    // ------------------------------------------------------------------ Shop

    Ladenbereich.entries.forEach { bereich ->
        composable(bereich.weg) {
            val stand by sitzung.stand.collectAsStateWithLifecycle()
            val daten by sitzung.daten.collectAsStateWithLifecycle()
            val buch by sitzung.dienstbuch.stand.collectAsStateWithLifecycle()
            val browser = LocalUriHandler.current
            val stelle = sitzung.dienstbuch

            LadenSeite(
                unterrand = unterrand,
                anfang = bereich,
                konto = stand.konto,
                shop = daten.shop,
                garage = daten.garage,
                katalog = daten.katalog.inhalt?.fahrzeuge.orEmpty(),
                abo = buch.abo,
                griffe = Ladengriffe(
                    kaufen = stelle::artikelKaufen,
                    drehen = stelle::radDrehen,
                    codeEinloesen = stelle::codeEinloesen,
                    beschenkbare = stelle::beschenkbare,
                    schenken = stelle::schenken,
                    fahrzeugWaehlen = stelle::fahrzeugWaehlen,
                    fahrzeugKaufen = stelle::fahrzeugKaufen,
                    kasse = stelle::premiumKasse,
                ),
                beiLaden = {
                    sitzung.shopLaden(neu = true)
                    sitzung.katalogSicherstellen()
                    sitzung.garageLaden(neu = true)
                },
                beiAboLaden = { neu -> stelle.aboLaden(neu) },
                beiBrowser = { url -> runCatching { browser.openUri(url) } },
                beiAnlegen = { steuerung.navigate(PROFIL_WEG) },
                beiAboVerwalten = { wegWaehlen(steuerung, Weg.Konto) },
                beiRechtstext = { seite -> runCatching { browser.openUri(Rechtsstand.adresse(Server.BETRIEB, seite)) } },
                beiVertrag = { seite -> runCatching { browser.openUri("${Server.BETRIEB}/$seite") } },
                beiAusbildung = { wegWaehlen(steuerung, Weg.Dienst) },
            )
        }
    }
}

/** Die Nachbesprechung einer archivierten Schicht — `dienstbuch/schicht/{code}`. */
const val SCHICHT_WEG = "dienstbuch/schicht"

/**
 * Der Profileditor — der Weg, den der Rahmen als `UNTERSEITE_PROFIL` führt. Er
 * steht hier noch einmal, weil die Konstante dort privat ist.
 */
private const val PROFIL_WEG = "profil"

/** Ob eine Route zum Dienstbuch oder zum Shop gehört — für die Markierung der Leiste. */
fun dienstbuchOderShopWeg(route: String?): Weg? = when {
    route == null -> null
    route.startsWith(Buchreiter.Uebersicht.weg) -> Weg.Dienstbuch
    route.startsWith(Ladenbereich.Uebersicht.weg) -> Weg.Shop
    else -> null
}

/**
 * Der Wechsel auf einen Weg der Leiste — dasselbe wie `zurWahl` im Rahmen: kein
 * Verlauf, gemerkter Stand je Weg.
 */
private fun wegWaehlen(steuerung: NavHostController, weg: Weg) {
    steuerung.navigate(weg.adresse) {
        popUpTo(steuerung.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
