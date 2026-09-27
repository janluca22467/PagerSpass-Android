package de.pagerspass.pagerspass.ansichten.welt

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import de.pagerspass.pagerspass.mobil.Sitzung
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/** Die Gründungsseite — `/welt` im Web. */
const val WEG_WELT = "welt"

/** Der Arbeitsplatz — `/welt/leitstelle` im Web. */
const val WEG_WELT_LEITSTELLE = "welt/leitstelle"

/**
 * Die Icon-Bibliothek — `/welt/icons` im Web. Sie gehört nicht zu diesem Bereich; die
 * Einstellungen verlinken nur dorthin, wenn es die Seite in der App gibt.
 */
const val WEG_WELT_ICONS = "welt/icons"

/**
 * Ob auf dieser Seite die Tableiste der App fehlt.
 *
 * Die Welt trägt ihre eigene Leiste unten. Eine zweite darüber nähme der Karte eine Zeile
 * und stünde für einen Weg, den man von dort ohnehin über „Zurück" geht
 * (`leisteSichtbar` in `mobil/MobilApp.vue`).
 */
fun istWeltweg(route: String?): Boolean = route == WEG_WELT || route == WEG_WELT_LEITSTELLE

/**
 * Die Wege der Welt im Navigationsgraphen — Gründung und Arbeitsplatz.
 *
 * <b>Die Weiche des Routers steht hier</b> (`router.ts`, `meta.premium` und
 * `meta.nachEinweisung`): Ohne Premium geht es in den Laden, mit offener Einweisung auf
 * den Start. Der Eintrag auf dem Startbildschirm prüft dasselbe schon vorher — ein
 * Eintrag ist aber keine Zugangssperre.
 *
 * @param beiShop Ohne Premium: in den Laden (Hinweis `world-premium` im Web).
 * @param beiStart Mit offener Einweisung: auf den Start (Hinweis `einweisung`).
 * @param beiProfil Ein Benutzername wurde angetippt — das Profil dazu.
 */
fun NavGraphBuilder.weltSeiten(
    sitzung: Sitzung,
    steuerung: NavHostController,
    beiShop: () -> Unit,
    beiStart: () -> Unit,
    beiProfil: (String) -> Unit,
    beiImWeb: (String) -> Unit,
) {
    composable(WEG_WELT) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val welt = weltModell()
        WeltTor(konto = stand.konto, beiShop = beiShop, beiStart = beiStart) {
            WeltGruendungSeite(
                welt = welt,
                kennung = stand.konto?.kennung,
                beiArbeitsplatz = {
                    steuerung.navigate(WEG_WELT_LEITSTELLE) {
                        popUpTo(WEG_WELT) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                beiZurueck = { steuerung.popBackStack() },
            )
        }
    }

    composable(WEG_WELT_LEITSTELLE) {
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val daten by sitzung.daten.collectAsStateWithLifecycle()
        val welt = weltModell()

        LaunchedEffect(Unit) { sitzung.katalogSicherstellen() }

        WeltTor(konto = stand.konto, beiShop = beiShop, beiStart = beiStart) {
            WeltArbeitsplatz(
                welt = welt,
                kennung = stand.konto?.kennung,
                katalog = daten.katalog.inhalt,
                beiGruendung = {
                    steuerung.navigate(WEG_WELT) {
                        popUpTo(WEG_WELT_LEITSTELLE) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                beiHinaus = { beiStart() },
                beiProfil = beiProfil,
                beiIcons = {
                    // Die Bibliothek baut ein anderer Bereich; fehlt die Seite noch im
                    // Graphen, führt der Weg ins Web statt in einen Absturz.
                    if (steuerung.graph.findNode(WEG_WELT_ICONS) != null) {
                        steuerung.navigate(WEG_WELT_ICONS)
                    } else {
                        beiImWeb("welt/icons")
                    }
                },
            )
        }
    }
}

/**
 * Die Weiche vor jeder Weltseite: Premium und Einweisung.
 *
 * Solange das Konto noch nicht da ist, steht nichts — die Sitzung prüft es gerade.
 */
@Composable
private fun WeltTor(
    konto: Konto?,
    beiShop: () -> Unit,
    beiStart: () -> Unit,
    inhalt: @Composable () -> Unit,
) {
    val premium = konto?.premiumAktiv == true
    val einweisung = konto?.einweisungOffen == true

    LaunchedEffect(konto?.kennung, premium, einweisung) {
        if (konto == null) return@LaunchedEffect
        if (!premium) beiShop() else if (einweisung) beiStart()
    }

    if (konto != null && premium && !einweisung) inhalt()
}

/**
 * Der Zustand der Welt — an der Activity und nicht an der Seite.
 *
 * Gründung und Arbeitsplatz sind zwei Ziele im Graphen und teilen sich denselben Zustand:
 * Wer gegründet hat, soll im Arbeitsplatz nicht noch einmal den Stand holen müssen, und
 * wer das Gerät dreht, soll seinen Chatverlauf behalten.
 */
@Composable
fun weltModell(): Welt {
    val aktivitaet = LocalContext.current.aktivitaet()
    val besitzer = aktivitaet ?: checkNotNull(LocalViewModelStoreOwner.current) {
        "Kein ViewModelStoreOwner für die Welt."
    }
    return viewModel(viewModelStoreOwner = besitzer)
}

private tailrec fun Context.aktivitaet(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.aktivitaet()
    else -> null
}

// -------------------------------------------------------------- Erklärung

/**
 * Was PagerSpass - World ist — in drei Absätzen. Übertragen aus
 * `components/welt/WorldErklaerungDialog.vue`.
 *
 * <b>Warum es diesen Dialog gibt.</b> „PagerSpass - World" stand als Name da: als Eintrag
 * im Menü und als Zeile unter den Premium-Vorteilen. Beide sagen, wo es hingeht und was es
 * kostet — keiner sagt, was es ist. Für ein freies Konto führt der Eintrag in den Laden,
 * und hineingehen, um es herauszufinden, geht dann nicht.
 *
 * <b>Keine Zahlen</b>: Startguthaben, Stufen und Preise stehen an den Stellen, an denen man
 * sie einlöst — eine Zahl in einem Werbetext ist die erste, die nicht mehr stimmt.
 */
@Composable
fun WorldErklaerungDialog(beiSchliessen: () -> Unit) {
    Blende(
        titel = "Eine Karte, auf der alle spielen",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        kopfknoepfe = {
            Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true)
        },
    ) {
        Text(
            text = "PAGERSPASS · WORLD",
            style = Schrift.Winzig.copy(
                fontFamily = Schrift.Mono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.em,
            ),
            color = GOLD,
        )
        Text(
            text = "Eine Runde ist eine Schicht: Sie fängt an, sie hört auf, danach ist der " +
                "Kreis wieder leer. World ist das Gegenteil davon. Du gründest einmal eine " +
                "eigene Leitstelle — irgendwo auf der Deutschlandkarte, auf der auch alle " +
                "anderen stehen — und die bleibt.",
            style = Schrift.Lesetext,
            color = Farben.Text,
        )
        Text(
            text = "Von da an baust du auf: Wachen setzen, Fahrzeuge kaufen, an jedem Einsatz " +
                "verdienen. Lagen entstehen in deinem Bereich — rund um deine Wachen, und je " +
                "mehr davon stehen, desto mehr Arbeit gibt es. Sie liegen zuerst allein bei " +
                "dir; was du nicht selbst schaffst, gibst du für alle frei, und dann bekommt " +
                "den Zuschlag, wer zuerst da ist. Gefahren wird die echte Straße, deshalb " +
                "entscheidet dein Standort über alles.",
            style = Schrift.Lesetext,
            color = Farben.Text,
        )
        Text(
            text = "Einmal in der Woche kommt ein Großeinsatz dazu, den keine Leitstelle " +
                "allein deckt: dieselbe Lage für alle, an einem Ort, zu dem die einen zwanzig " +
                "Minuten und die anderen neun Stunden brauchen.",
            style = Schrift.Lesetext,
            color = Farben.Text,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.FlaecheHoch, Rundung.Klein)
                .border(1.dp, Farben.Rand, Rundung.Klein)
                .padding(Abstand.Normal),
        ) {
            listOf(
                "Eine Welt für alle — deine Leitstelle steht neben ihren.",
                "Läuft rund um die Uhr weiter, auch wenn du nicht da bist.",
                "Eigenes Guthaben, eigene Stufen — deine Runden bleiben davon unberührt.",
            ).forEach { satz ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Top,
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .background(Farben.Amber, CircleShape),
                    )
                    Text(text = satz, style = Schrift.Klein, color = Farben.TextLeise)
                }
            }
        }
        Text(
            text = "Der Zugang gehört zu PagerSpass Premium.",
            style = Schrift.Klein,
            color = Farben.TextSehrLeise,
        )
    }
}

/** Der goldene Anflug der Welt — `#f4d35e` an Kennzeile und Premium-Stern. */
val GOLD = androidx.compose.ui.graphics.Color(0xFFF4D35E)
