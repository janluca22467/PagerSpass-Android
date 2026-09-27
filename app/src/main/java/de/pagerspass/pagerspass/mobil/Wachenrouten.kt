package de.pagerspass.pagerspass.mobil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.pagerspass.pagerspass.ansichten.WachenGriffe
import de.pagerspass.pagerspass.ansichten.WachenSeite
import de.pagerspass.pagerspass.ansichten.WachenShopSeite
import de.pagerspass.pagerspass.ansichten.WachenranglisteSeite

/**
 * Die Wege des Wachenbereichs — Wache, Rangliste, Shop und der Link mit
 * Beitrittscode. Übertragen aus den Routen `gemeinschaft`, `gemeinschaften`,
 * `wachenrangliste` und `wachenshop` des Web.
 *
 * <b>Eine Erweiterung des NavHost</b> statt vier `composable`-Blöcken im Rahmen:
 * Der Rahmen ruft genau `wachenWege(…)`, und was zum Wachenbereich gehört, steht
 * hier beieinander.
 */

/** Die Rangliste der Wachen — `/gemeinschaften/rangliste` im Web. */
const val WACHE_RANGLISTE = "wache/rangliste"

/** Der Wachen-Shop — `/gemeinschaft/shop` im Web. */
const val WACHE_SHOP = "wache/shop"

/**
 * Der Weg eines Beitrittslinks: `gemeinschaften?code=ABC123`, dieselbe Form wie
 * `/gemeinschaften?code=…` im Web. Der Code ist freiwillig; ohne ihn ist es die
 * Wache-Seite. Beigetreten wird erst auf den Knopf.
 */
const val WACHE_FINDEN = "gemeinschaften?code={code}"

/** Die Adresse, mit der man einen Beitrittslink öffnet — für die Tiefe-Links-Leitung. */
fun wachenfindenweg(code: String?): String =
    if (code.isNullOrBlank()) "gemeinschaften" else "gemeinschaften?code=$code"

/** Ob eine Route zum Wachenbereich gehört — die Leiste markiert dann „Wache". */
fun istWachenweg(route: String?): Boolean =
    route != null && (route.startsWith("wache/") || route.startsWith("gemeinschaften"))

/**
 * Trägt die Wege des Wachenbereichs in den NavHost ein.
 *
 * @param beiRunde In eine laufende Runde — derselbe Weg wie vom Startbildschirm
 *   aus (`Runde.beitreten`).
 * @param beiProfil Ins Profil eines Mitglieds über den Benutzernamen — `null`,
 *   solange die App diesen Weg nicht kennt.
 */
fun NavGraphBuilder.wachenWege(
    sitzung: Sitzung,
    sozial: Sozial,
    steuerung: NavHostController,
    unterrand: Dp,
    beiRunde: (String) -> Unit,
    beiWache: () -> Unit,
    beiProfil: ((String) -> Unit)? = null,
) {
    composable(Weg.Wache.adresse) {
        Wachenbereich(sitzung, sozial, steuerung, unterrand, beiRunde, beiWache, beiProfil, code = null)
    }

    composable(
        WACHE_FINDEN,
        arguments = listOf(
            navArgument("code") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { eintrag ->
        Wachenbereich(
            sitzung,
            sozial,
            steuerung,
            unterrand,
            beiRunde,
            beiWache,
            beiProfil,
            code = eintrag.arguments?.getString("code"),
        )
    }

    composable(WACHE_RANGLISTE) {
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val wachen by sitzung.wachen.stand.collectAsStateWithLifecycle()
        WachenranglisteSeite(
            griffe = griffe(sitzung, sozial, steuerung, beiRunde, beiWache, beiProfil),
            unterrand = unterrand,
            wachen = wachen,
            hatWache = daten.wache.inhalt?.eigene != null,
        )
    }

    composable(WACHE_SHOP) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val wachen by sitzung.wachen.stand.collectAsStateWithLifecycle()
        WachenShopSeite(
            griffe = griffe(sitzung, sozial, steuerung, beiRunde, beiWache, beiProfil),
            unterrand = unterrand,
            wache = daten.wache,
            detail = daten.wacheDetail,
            wachen = wachen,
            konto = stand.konto,
        )
    }
}

@Composable
private fun Wachenbereich(
    sitzung: Sitzung,
    sozial: Sozial,
    steuerung: NavHostController,
    unterrand: Dp,
    beiRunde: (String) -> Unit,
    beiWache: () -> Unit,
    beiProfil: ((String) -> Unit)?,
    code: String?,
) {
    val stand by sitzung.stand.collectAsStateWithLifecycle()
    val daten by sitzung.daten.collectAsStateWithLifecycle()
    val wachen by sitzung.wachen.stand.collectAsStateWithLifecycle()
    val sozialstand by sozial.stand.collectAsStateWithLifecycle()

    WachenSeite(
        griffe = griffe(sitzung, sozial, steuerung, beiRunde, beiWache, beiProfil),
        unterrand = unterrand,
        wache = daten.wache,
        detail = daten.wacheDetail,
        antraege = daten.wachenantraege,
        wachen = wachen,
        sozial = sozialstand,
        konto = stand.konto,
        freunde = daten.freunde.inhalt.orEmpty(),
        landkreise = daten.katalog.inhalt?.landkreise.orEmpty(),
        server = stand.server,
        code = code,
    )
}

private fun griffe(
    sitzung: Sitzung,
    sozial: Sozial,
    steuerung: NavHostController,
    beiRunde: (String) -> Unit,
    beiWache: () -> Unit,
    beiProfil: ((String) -> Unit)?,
) = WachenGriffe(
    speicher = sitzung.wachen,
    chatOeffnen = { sozial.wachenchatOeffnen(it) },
    chatSenden = { id, text -> sozial.wachenchatSenden(id, text) },
    seiteSichtbar = { sozial.wachenseiteOffen = it },
    runde = beiRunde,
    // Eröffnen setzt den Raumcode — der Rahmen tritt daraufhin sofort bei.
    clanrunde = { sitzung.clanrundeStarten(it) },
    katalog = { sitzung.katalogSicherstellen() },
    freundeLaden = { sitzung.freundeLaden() },
    zurRangliste = {
        steuerung.navigate(WACHE_RANGLISTE) { launchSingleTop = true }
    },
    zumShop = {
        steuerung.navigate(WACHE_SHOP) { launchSingleTop = true }
    },
    zurWache = beiWache,
    profil = beiProfil,
)
