package de.pagerspass.pagerspass.mobil

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.pagerspass.pagerspass.ansichten.Analyseblende
import de.pagerspass.pagerspass.ansichten.BetriebsmitteilungenSeite
import de.pagerspass.pagerspass.ansichten.CodeSeite
import de.pagerspass.pagerspass.ansichten.DiscordSeite
import de.pagerspass.pagerspass.ansichten.EmailPflichtblende
import de.pagerspass.pagerspass.ansichten.KontoZentrale
import de.pagerspass.pagerspass.ansichten.MitteilungenSeite
import de.pagerspass.pagerspass.ansichten.NewsletterAbmeldenSeite
import de.pagerspass.pagerspass.ansichten.PrivatsphaereSeite
import de.pagerspass.pagerspass.ansichten.ProfilSeite
import de.pagerspass.pagerspass.ansichten.RechtSeite
import de.pagerspass.pagerspass.ansichten.Rechtsstandblende
import de.pagerspass.pagerspass.ansichten.VertragKuendigenSeite
import de.pagerspass.pagerspass.ansichten.VertragWiderrufenSeite

/**
 * Der Kontobereich im Rahmen — Routen, Einsprünge, Startfragen.
 *
 * <b>Eine Datei statt vieler Stellen in `PagerSpassApp`.</b> Der Rahmen ruft genau
 * drei Dinge von hier: [kontobereich] im `NavHost`, [EinsprungFolgen] neben dem
 * `NavHost` und [KontoBlenden] über allem; dazu [Draussen] um die Anmeldeseite.
 * Wer hier etwas ändert, fasst den Rahmen nicht an.
 */
object Kontorouten {
    const val KONTO = "konto"
    const val PROFIL = "profil"
    const val PRIVATSPHAERE = "privatsphaere"
    const val MITTEILUNGEN = "mitteilungen"

    /** Die Mitteilungen vom Betrieb — die Route heißt aus alter Zeit noch „postfach". */
    const val BETRIEB = "postfach"
    const val DISCORD = "discord"
    const val CODE = "code"
    const val RECHT = "recht"
    const val NEWSLETTER = "newsletter/abmelden"
    const val KUENDIGEN = "vertrag/kuendigen"
    const val WIDERRUFEN = "vertrag/widerrufen"
    const val BEGLEITER = "begleiter"
}

/** Die sechs Wege der Leiste — sie werden gewählt, nicht gestapelt (siehe `zurWahl`). */
private val LEISTENWEGE: Set<String> = Weg.entries.map { it.adresse }.toSet()

/**
 * Zu welchem Weg der Leiste eine Seite des Kontobereichs gehört — die Ergänzung zu
 * `unterseitenweg` im Rahmen. Die Seiten ohne Anmeldung (Recht, Verträge, Code,
 * Newsletter) stehen angemeldet ebenfalls unter „Konto".
 */
fun kontobereichWeg(route: String?): Weg? {
    val basis = route?.substringBefore('?') ?: return null
    return when {
        basis == Kontorouten.KONTO ||
            basis == Kontorouten.PROFIL ||
            basis == Kontorouten.PRIVATSPHAERE ||
            basis == Kontorouten.MITTEILUNGEN ||
            basis == Kontorouten.BETRIEB ||
            basis == Kontorouten.DISCORD ||
            basis == Kontorouten.NEWSLETTER ||
            basis.startsWith("${Kontorouten.CODE}/") ||
            basis.startsWith("${Kontorouten.RECHT}/") ||
            basis.startsWith("vertrag/") -> Weg.Konto
        else -> null
    }
}

/**
 * Die Seiten des Kontobereichs im `NavHost`.
 *
 * @param platz Die Höhe der Tableiste — so viel Unterrand braucht jede Seite.
 * @param beiRaum Einer Runde beitreten (für Codes, die aus einem Link kommen).
 */
fun NavGraphBuilder.kontobereich(sitzung: Sitzung, steuerung: NavHostController, platz: Dp) {
    composable(Kontorouten.KONTO) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val rueckkehr by Einsprung.premiumRueckkehr.collectAsStateWithLifecycle()

        KontoZentrale(
            wege = sitzung.kontowege,
            unterrand = platz,
            konto = stand.konto,
            profil = daten.profil,
            server = stand.server,
            laeuft = stand.laeuft,
            fehler = stand.fehler,
            premiumRueckkehr = rueckkehr?.first,
            sessionId = rueckkehr?.second,
            beiRueckkehrErledigt = { Einsprung.premiumRueckkehrErledigt() },
            beiProfilLaden = { sitzung.profilLaden() },
            beiKontoNeu = { sitzung.kontoUebernehmen(it) },
            beiKontoAendern = { aenderung -> sitzung.kontoAendern(aenderung) },
            beiPasswortAendern = { aktuell, neu ->
                val kennung = sitzung.stand.value.konto?.kennung
                    ?: throw IllegalStateException("Bitte melde dich zuerst an.")
                sitzung.konten.passwortAendern(kennung, aktuell, neu)
            },
            beiAbmelden = { sitzung.abmelden() },
            beiLoeschen = { passwort -> sitzung.kontoLoeschen(passwort) },
            beiProfil = { steuerung.navigate(Kontorouten.PROFIL) },
            beiWeg = { navigieren(steuerung, it) },
        )
    }

    composable(Kontorouten.PROFIL) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()

        // Der Shop sagt, was gekauft ist — und nur Gekauftes lässt sich tragen.
        LaunchedEffect(Unit) { sitzung.shopLaden() }

        ProfilSeite(
            wege = sitzung.kontowege,
            unterrand = platz,
            konto = stand.konto,
            profil = daten.profil,
            server = stand.server,
            besitzt = daten.shop.inhalt?.imBesitz.orEmpty()
                .flatMap { listOf(it.stueckId, it.id) }
                .filter { it.isNotBlank() }
                .toSet(),
            profilbild = daten.profilbild,
            landkreise = daten.katalog.inhalt?.landkreise.orEmpty(),
            laeuft = stand.laeuft,
            beiLaden = { sitzung.profilLaden() },
            beiKatalog = { sitzung.katalogSicherstellen() },
            beiGespeichert = {
                sitzung.profilLaden(neu = true)
                sitzung.kontoNachladen()
            },
            beiKontoNeu = { sitzung.kontoUebernehmen(it) },
            beiBildLaden = { sitzung.profilbildLaden() },
            beiBildEinreichen = { name, typ, bytes -> sitzung.profilbildEinreichen(name, typ, bytes) },
            beiBildEntfernen = { sitzung.profilbildEntfernen() },
            beiWeg = { navigieren(steuerung, it) },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable(Kontorouten.PRIVATSPHAERE) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        PrivatsphaereSeite(
            wege = sitzung.kontowege,
            konto = stand.konto,
            unterrand = platz,
            beiAnalyse = { sitzung.analyseBeantworten(it) },
            beiGeraetLeeren = { sitzung.geraetLeeren() },
            beiWeg = { navigieren(steuerung, it) },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable(Kontorouten.MITTEILUNGEN) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        MitteilungenSeite(
            wege = sitzung.kontowege,
            konto = stand.konto,
            unterrand = platz,
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable(Kontorouten.BETRIEB) {
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        BetriebsmitteilungenSeite(
            unterrand = platz,
            mitteilungen = daten.mitteilungen,
            beiLaden = { sitzung.mitteilungenLaden() },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable(
        "${Kontorouten.DISCORD}?fertig={fertig}&fehler={fehler}",
        arguments = listOf(
            navArgument("fertig") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("fehler") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { eintrag ->
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        DiscordSeite(
            wege = sitzung.kontowege,
            konto = stand.konto,
            unterrand = platz,
            fertig = eintrag.arguments?.getString("fertig") == "1",
            rueckkehrfehler = eintrag.arguments?.getString("fehler")?.takeIf { it.isNotBlank() },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable("${Kontorouten.CODE}/{code}") { eintrag ->
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        CodeSeite(
            code = eintrag.arguments?.getString("code").orEmpty(),
            wege = sitzung.kontowege,
            konto = stand.konto,
            unterrand = platz,
            beiEingeloest = { credits ->
                if (credits != null) sitzung.kontoAendern { it.copy(credits = credits) }
                sitzung.shopLaden(neu = true)
                sitzung.garageLaden(neu = true)
            },
            beiZumSpiel = { navigieren(steuerung, Weg.Dienst.adresse) },
            beiShop = { navigieren(steuerung, Weg.Shop.adresse) },
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable("${Kontorouten.RECHT}/{seite}") { eintrag ->
        RechtSeite(
            seite = eintrag.arguments?.getString("seite").orEmpty(),
            unterrand = platz,
            beiZurueck = { steuerung.popBackStack() },
        )
    }

    composable(
        "${Kontorouten.NEWSLETTER}?schluessel={schluessel}",
        arguments = listOf(
            navArgument("schluessel") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { eintrag ->
        NewsletterAbmeldenSeite(
            schluessel = eintrag.arguments?.getString("schluessel"),
            wege = sitzung.kontowege,
            unterrand = platz,
            beiZumSpiel = { navigieren(steuerung, Weg.Dienst.adresse) },
        )
    }

    composable(Kontorouten.KUENDIGEN) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        VertragKuendigenSeite(
            wege = sitzung.kontowege,
            unterrand = platz,
            konto = stand.konto,
            beiZurueck = { steuerung.popBackStack() },
            beiZumSpiel = { navigieren(steuerung, Weg.Dienst.adresse) },
            beiWiderrufen = { navigieren(steuerung, Kontorouten.WIDERRUFEN) },
        )
    }

    composable(Kontorouten.WIDERRUFEN) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        VertragWiderrufenSeite(
            wege = sitzung.kontowege,
            unterrand = platz,
            konto = stand.konto,
            beiZurueck = { steuerung.popBackStack() },
            beiZumSpiel = { navigieren(steuerung, Weg.Dienst.adresse) },
            beiKuendigen = { navigieren(steuerung, Kontorouten.KUENDIGEN) },
            beiRechtstext = { navigieren(steuerung, "${Kontorouten.RECHT}/$it") },
        )
    }
}

/**
 * Eine Route öffnen — wo immer sie herkommt (Einsprung, Knopf, Schnellweg).
 *
 * Die sechs Wege der Leiste werden gewählt und nicht gestapelt, wie in der Leiste
 * selbst. Alles andere wird gestapelt — <b>und was es im Graphen nicht gibt, wird
 * still übergangen</b>: `navigate` wirft dann, und ein Link auf eine Seite, die
 * diese Fassung der App noch nicht kennt, soll sie nur öffnen, nicht abstürzen
 * lassen.
 *
 * @return ob die Route gefunden wurde.
 */
fun navigieren(steuerung: NavHostController, route: String): Boolean {
    val basis = route.substringBefore('?')
    return try {
        if (basis in LEISTENWEGE && basis == route) {
            steuerung.navigate(route) {
                popUpTo(steuerung.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            steuerung.navigate(route)
        }
        true
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: IllegalStateException) {
        false
    }
}

/**
 * Holt den [Einsprung] ab, sobald die App angemeldet dasteht.
 *
 * <b>Neben den `NavHost` gehört er, nicht davor:</b> Seine Wirkung läuft nach der
 * Zusammensetzung, und bis dahin hat der `NavHost` seinen Graphen gesetzt — ein
 * `navigate` vor dem Graphen wirft.
 *
 * Zwei Ziele haben einen Rückfall, falls kein anderer Bereich ihre Route
 * registriert hat: `raum/{code}` tritt der Runde bei, `funk/{token}` koppelt den
 * Begleiter.
 */
@Composable
fun EinsprungFolgen(
    steuerung: NavHostController,
    beiRaum: (String) -> Unit,
    beiFunk: (String) -> Unit,
) {
    val ziel by Einsprung.ziel.collectAsStateWithLifecycle()
    val spaeter by Einsprung.nachAnmeldung.collectAsStateWithLifecycle()

    LaunchedEffect(ziel) {
        val route = ziel ?: return@LaunchedEffect
        Einsprung.erledigt(route)
        folgen(steuerung, route, beiRaum, beiFunk)
    }

    LaunchedEffect(spaeter) {
        val route = spaeter ?: return@LaunchedEffect
        Einsprung.nachAnmeldungErledigt(route)
        folgen(steuerung, route, beiRaum, beiFunk)
    }
}

private fun folgen(
    steuerung: NavHostController,
    route: String,
    beiRaum: (String) -> Unit,
    beiFunk: (String) -> Unit,
) {
    val adresse = Uri.parse("app://pagerspass/$route")
    val basis = route.substringBefore('?')

    // Die Rückkehr von Kasse und Abo-Verwaltung: die Kontozentrale liest sie.
    if (basis == Kontorouten.KONTO) {
        adresse.getQueryParameter("premium")?.takeIf { it.isNotBlank() }?.let { weg ->
            Einsprung.premiumRueckkehrSetzen(weg, adresse.getQueryParameter("session_id"))
        }
        navigieren(steuerung, Kontorouten.KONTO)
        return
    }

    if (navigieren(steuerung, route)) return

    val rest = adresse.pathSegments.drop(1).firstOrNull() ?: return
    when (adresse.pathSegments.firstOrNull()) {
        "raum" -> beiRaum(rest)
        "funk" -> beiFunk(rest)
    }
}

/**
 * Was ohne Anmeldung geht — und deshalb schon vor ihr geöffnet wird.
 */
private fun ohneAnmeldung(route: String): Boolean {
    val basis = route.substringBefore('?')
    return basis.startsWith("${Kontorouten.RECHT}/") ||
        basis == Kontorouten.KUENDIGEN ||
        basis == Kontorouten.WIDERRUFEN ||
        basis == Kontorouten.NEWSLETTER ||
        basis.startsWith("${Kontorouten.CODE}/")
}

/**
 * Der Rahmen um die Anmeldeseite — mit den Seiten, die ohne Konto erreichbar sein
 * müssen: Rechtstexte (§ 5 DDG, und die Zustimmung verlinkt dorthin), Kündigen
 * und Widerrufen (§ 312k, § 356a BGB), Newsletter abmelden (§ 7 UWG) und die
 * Seite eines Creator-Codes.
 *
 * <b>Eine Seite tief, mit Zurück.</b> Hier gibt es keine Leiste und keinen
 * Verlauf; jede dieser Seiten führt zur Anmeldung zurück.
 *
 * @param login Die Anmeldeseite; sie bekommt den Weg zu diesen Seiten mit.
 */
@Composable
fun Draussen(sitzung: Sitzung, login: @Composable (beiSeite: (String) -> Unit) -> Unit) {
    var seite by rememberSaveable { mutableStateOf<String?>(null) }
    val ziel by Einsprung.ziel.collectAsStateWithLifecycle()

    // Was ohne Anmeldung geht, wird gleich geöffnet; alles andere wartet.
    LaunchedEffect(ziel) {
        val route = ziel ?: return@LaunchedEffect
        if (ohneAnmeldung(route)) {
            Einsprung.erledigt(route)
            seite = route
        }
    }

    BackHandler(enabled = seite != null) { seite = null }

    val gerade = seite
    if (gerade == null) {
        login { seite = it }
        return
    }

    val adresse = Uri.parse("app://pagerspass/$gerade")
    val teile = adresse.pathSegments
    val zurueck = { seite = null }

    when {
        gerade.startsWith("${Kontorouten.RECHT}/") -> RechtSeite(
            seite = teile.getOrNull(1).orEmpty(),
            beiZurueck = zurueck,
        )

        gerade == Kontorouten.KUENDIGEN -> VertragKuendigenSeite(
            wege = sitzung.kontowege,
            beiZurueck = zurueck,
            beiZumSpiel = zurueck,
            beiWiderrufen = { seite = Kontorouten.WIDERRUFEN },
        )

        gerade == Kontorouten.WIDERRUFEN -> VertragWiderrufenSeite(
            wege = sitzung.kontowege,
            beiZurueck = zurueck,
            beiZumSpiel = zurueck,
            beiKuendigen = { seite = Kontorouten.KUENDIGEN },
            beiRechtstext = { seite = "${Kontorouten.RECHT}/$it" },
        )

        gerade.startsWith(Kontorouten.NEWSLETTER) -> NewsletterAbmeldenSeite(
            schluessel = adresse.getQueryParameter("schluessel"),
            wege = sitzung.kontowege,
            beiZumSpiel = zurueck,
        )

        gerade.startsWith("${Kontorouten.CODE}/") -> {
            val code = teile.getOrNull(1).orEmpty()
            CodeSeite(
                code = code,
                wege = sitzung.kontowege,
                konto = null,
                // Anmelden, dann zurück hierher — der Code wird erst danach eingelöst.
                beiAnmelden = {
                    Einsprung.nachDerAnmeldung("${Kontorouten.CODE}/$code")
                    seite = null
                },
                beiZumSpiel = zurueck,
                beiShop = zurueck,
                beiZurueck = zurueck,
            )
        }

        else -> login { seite = it }
    }
}

/**
 * Die Fragen, die beantwortet sein müssen, bevor es weitergeht — in der
 * Reihenfolge von `MobilApp.vue`: Analyse → Rechtsstand → E-Mail-Pflicht. Nie
 * zwei übereinander. Der Wiederherstellungscode geht allen vor, weil er nur ein
 * einziges Mal zu haben ist.
 */
@Composable
fun KontoBlenden(sitzung: Sitzung) {
    val stand by sitzung.stand.collectAsStateWithLifecycle()
    val emailOffen by sitzung.emailBestaetigungOffen.collectAsStateWithLifecycle()
    val konto = stand.konto ?: return
    if (stand.wiederherstellungscode != null) return

    val analyseOffen = konto.analyseZustimmung == null
    val rechtsstandOffen = !analyseOffen && stand.rechtsstandOffen
    val emailPflicht = !analyseOffen && !rechtsstandOffen && (konto.emailFehlt || emailOffen)

    when {
        analyseOffen -> Analyseblende { sitzung.analyseBeantworten(it) }

        rechtsstandOffen -> Rechtsstandblende(
            laeuft = stand.laeuft,
            fehler = stand.fehler,
            beiZustimmen = { sitzung.rechtsstandZustimmen() },
            beiAbmelden = { sitzung.abmelden() },
        )

        emailPflicht -> EmailPflichtblende(
            bestaetigungOffen = emailOffen,
            beiSpeichern = { email, passwort -> sitzung.emailHinterlegen(email, passwort) },
            beiBestaetigen = { sitzung.emailCodeBestaetigen(it) },
            beiNeuerCode = { sitzung.emailCodeNeu() },
            beiSpaeter = { sitzung.emailBestaetigungSpaeter() },
            beiAbmelden = { sitzung.abmelden() },
        )
    }
}
