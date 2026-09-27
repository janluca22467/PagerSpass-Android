package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.mobil.Einstellungsaenderung
import de.pagerspass.pagerspass.mobil.Lobbystand
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Garage
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Platzanfrage
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.netz.Spieler
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Teil
import de.pagerspass.pagerspass.ui.bausteine.Teilleiste
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Profilzeile
import de.pagerspass.pagerspass.ui.schmuck.profilbildAdresse
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.raster
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Die Lobby — übertragen aus `web/src/views/LobbyView.vue` in ihrer Handyform.
 *
 * <b>Am Handy ist sie keine Rolle, sondern fünf Teile:</b> fester Kopf oben,
 * **ein** Teil in der Mitte, Reiterleiste unten am Daumen — Rolle · Runde ·
 * Mannschaft · Chat · Mehr. Dieselbe Bauform wie die Welt am Handy.
 *
 * <b>Der Reiterwechsel wirft nichts weg.</b> Im Web ist die Auswahl ein
 * Attributselektor und kein `v-if`, damit jeder Teil beim Umschalten seinen
 * Zustand behält — die Suche der Fahrzeugliste, der halb getippte Chatsatz.
 * Hier steht dafür der Zustand der Teile *außerhalb* des `when`, in `remember`
 * am Rahmen; ein `remember` im ungezeigten Zweig wäre weg.
 *
 * <b>Sie ist auch der Weg in den laufenden Dienst.</b> Wer noch keine Rolle hat,
 * landet hier, auch wenn die Runde längst läuft — öffentliche Runden lassen sich
 * mitten im Betrieb betreten. Dann bietet der Teil „Rolle" zwei Wege: eine
 * Bot-Besatzung übernehmen oder ein eigenes Fahrzeug neu besetzen.
 *
 * <b>Und sie trägt nicht die Tableiste der App.</b> Im Raum hat jede Ansicht
 * ihre eigenen Reiter und braucht jede Zeile Höhe; zwei Leisten übereinander
 * wären eine zu viel.
 */
@Composable
fun LobbySeite(
    modifier: Modifier = Modifier,
    stand: Rundenstand = Rundenstand(),
    eigeneKennung: String = "",
    konto: Konto? = null,
    server: String = "",
    garage: Garage? = null,
    katalog: Katalog? = null,
    daten: Lobbystand = Lobbystand(),
    freunde: List<Freund> = emptyList(),
    einladeMeldung: String? = null,
    griffe: LobbyGriffe = LobbyGriffe(),
) {
    var reiter by remember { mutableStateOf(Lobbyteil.Rolle) }

    // Der Chatsatz lebt hier und nicht im Chat-Teil: Wer mitten im Tippen den
    // Reiter wechselt, soll seinen Satz wiederfinden.
    var chatsatz by remember { mutableStateOf("") }
    // Suche und Filter der Fahrzeugwahl ebenso.
    var suche by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        griffe.laden()
        griffe.freundeLaden()
    }

    val raum = stand.raum
    val chat = stand.lobbychatGesamt

    // Ungelesene Zeilen — die Marke am Reiter „Chat". <b>Sie zählt ab dem
    // Betreten</b>, nicht ab null, und nur fremde Zeilen: Wer die Lobby betritt,
    // bekommt keine Marke für Gespräche, die vor ihm liefen.
    var gelesen by remember { mutableIntStateOf(chat.size) }
    if (reiter == Lobbyteil.Chat && gelesen != chat.size) gelesen = chat.size
    val ungelesen = chat.drop(gelesen.coerceAtMost(chat.size)).count { it.vonId != eigeneKennung }

    val ich = raum?.players?.firstOrNull { it.id == eigeneKennung }
    val aussparung = WindowInsets.statusBars.asPaddingValues()
    val hostAnfragen = if (raum?.istHost == true) {
        raum.platzanfragen.size + raum.beitrittsanfragen.size
    } else {
        0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Farben.Bg, Farben.BgTief)))
            }
            .raster(),
    ) {
        Dienstleiste(
            stand = stand,
            katalog = katalog,
            oben = aussparung.calculateTopPadding(),
            beiVerlassen = griffe.verlassen,
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (raum == null) {
                Ladezeile(
                    "Die Leitstelle wird gerufen …",
                    Modifier.padding(horizontal = Abstand.Gross),
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Gross),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Abstand.Gross, vertical = Abstand.Gross),
                ) {
                    when (reiter) {
                        Lobbyteil.Rolle -> TeilRolle(
                            raum = raum,
                            ich = ich,
                            stand = stand,
                            konto = konto,
                            garage = garage,
                            katalog = katalog,
                            server = server,
                            suche = suche,
                            beiSuche = { suche = it },
                            filter = filter,
                            beiFilter = { filter = it },
                            griffe = griffe,
                        )

                        Lobbyteil.Runde -> TeilRunde(
                            raum = raum,
                            ich = ich,
                            konto = konto,
                            katalog = katalog,
                            daten = daten,
                            griffe = griffe,
                        )

                        Lobbyteil.Mannschaft -> TeilMannschaft(
                            raum = raum,
                            ich = ich,
                            eigeneKennung = eigeneKennung,
                            server = server,
                            freunde = freunde,
                            daten = daten,
                            griffe = griffe,
                        )

                        Lobbyteil.Chat -> TeilChat(
                            verlauf = chat,
                            raum = raum,
                            eigeneKennung = eigeneKennung,
                            teamBefehle = konto?.teammitglied == true,
                            satz = chatsatz,
                            beiSatz = { chatsatz = it },
                            beiSenden = {
                                griffe.chat(chatsatz)
                                chatsatz = ""
                            },
                        )

                        Lobbyteil.Mehr -> TeilMehr(
                            raum = raum,
                            ich = ich,
                            server = server,
                            freunde = freunde,
                            einladeMeldung = einladeMeldung,
                            griffe = griffe,
                        )
                    }
                }
            }
        }

        // <b>Der Startblock klebt unten.</b> Bei kurzen Teilen (Mehr, Chat) sitzt
        // er sonst mitten im Bild; bei langen wäre er unerreichbar. Im laufenden
        // Dienst gibt es nichts mehr zu beginnen — dann fällt er weg.
        if (raum != null && !raum.laeuft) {
            Startblock(raum = raum, ich = ich, laeuft = stand.laeuft, griffe = griffe)
        }

        Teilleiste(
            teile = Lobbyteil.entries.map { Teil(it.name, it.titel, it.zeichen) },
            offen = reiter.name,
            marken = mapOf(Lobbyteil.Chat.name to ungelesen, Lobbyteil.Rolle.name to hostAnfragen),
            // Wer keinen Platz hat, hält die ganze Runde auf — und sucht den
            // Reiter, auf dem er das ändert.
            ruft = if (ich?.role == "Unbestimmt" && reiter != Lobbyteil.Rolle) {
                setOf(Lobbyteil.Rolle.name)
            } else {
                emptySet()
            },
            beiWahl = { id -> reiter = Lobbyteil.valueOf(id) },
        )
    }
}

/**
 * Die Griffe der Lobby — alles, was sie am Hub oder an den Nachbarn auslöst.
 *
 * Eine Klasse statt dreißig Parameter: Die Lobby reicht sie an ihre Teile
 * durch, und wer einen Griff sucht, findet ihn an einer Stelle.
 */
class LobbyGriffe(
    val rolle: (String, String?) -> Unit = { _, _ -> },
    val bereit: (Boolean) -> Unit = {},
    val bot: (String, Int) -> Unit = { _, _ -> },
    val botEntfernen: (String) -> Unit = {},
    val botUebernehmen: (String) -> Unit = {},
    val chat: (String) -> Unit = {},
    val start: () -> Unit = {},
    val verlassen: () -> Unit = {},
    val einstellungen: (Einstellungsaenderung) -> Unit = {},
    val platzanfrageZuruecknehmen: () -> Unit = {},
    val platzanfrageEntscheiden: (String, Boolean) -> Unit = { _, _ -> },
    val beitrittEntscheiden: (String, Boolean) -> Unit = { _, _ -> },
    val kicken: (String) -> Unit = {},
    val freundAnfragen: (String) -> Unit = {},
    val rufname: (String, String, String?) -> Unit = { _, _, _ -> },
    val rufnameZuruecksetzen: (String) -> Unit = {},
    val wacheZuweisen: (String, String) -> Unit = { _, _ -> },
    val raumwachenLaden: () -> Unit = {},
    val kreiswachenLaden: (String) -> Unit = {},
    val vorlagenLaden: () -> Unit = {},
    val vorlageSpeichern: (String?, String) -> Unit = { _, _ -> },
    val live: (Boolean) -> Unit = {},
    val einladen: (String, Boolean) -> Unit = { _, _ -> },
    val freundeLaden: () -> Unit = {},
    val melderTesten: () -> Unit = {},
    val hilfe: () -> Unit = {},
    val laden: () -> Unit = {},
)

/** Die fünf Teile — dieselben Namen wie `Lobbyteil` im Web. */
private enum class Lobbyteil(val titel: String, val zeichen: ImageVector) {
    Rolle("Rolle", Zeichen.LobbyRolle),
    Runde("Runde", Zeichen.LobbyRunde),
    Mannschaft("Mannschaft", Zeichen.LobbyMannschaft),
    Chat("Chat", Zeichen.LobbyChat),
    Mehr("Mehr", Zeichen.LobbyMehr),
}

/**
 * Der Kopf — er rollt nicht mit und trägt eine Kante nach unten.
 *
 * Oben das <b>Raumschild</b> (unter welchem Code man dazukommt und für wen
 * disponiert wird), darunter die <b>Auskünfte</b> (Sandkasten, Spielerzahl,
 * Zuschauer). Die Taten — Einladen, Melder, Streamen — liegen am Handy hinter
 * dem Reiter „Mehr".
 */
@Composable
private fun Dienstleiste(
    stand: Rundenstand,
    katalog: Katalog?,
    oben: Dp,
    beiVerlassen: () -> Unit,
) {
    val zwischenablage = LocalClipboardManager.current
    val raum = stand.raum
    var kopiert by remember { mutableStateOf(false) }
    LaunchedEffect(kopiert) {
        if (kopiert) {
            kotlinx.coroutines.delay(2_400)
            kopiert = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Farben.FlaecheHoch, Farben.Flaeche)))
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(
                    color = Farben.Rand,
                    start = Offset(0f, size.height - strich / 2f),
                    end = Offset(size.width, size.height - strich / 2f),
                    strokeWidth = strich,
                )
            }
            .padding(top = oben)
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Der Code ist ein Knopf: Antippen kopiert ihn. Er wird
            // weitergegeben, und abtippen ist der Weg, den man nimmt, wenn es
            // keinen anderen gibt.
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        onClick = {
                            zwischenablage.setText(AnnotatedString(stand.code))
                            kopiert = true
                        },
                        role = Role.Button,
                        indication = null,
                        interactionSource = null,
                    ),
            ) {
                Text(
                    text = stand.code.ifBlank { "……" },
                    style = Schrift.MonoNormal.copy(
                        fontSize = Schrift.SCHLAGZEILE,
                        letterSpacing = 0.14.em,
                    ),
                    color = Farben.Amber,
                )
                SehrLeise(
                    when (stand.lage) {
                        Funkverbindung.Lage.Verbunden -> if (kopiert) "kopiert" else "tippen zum Kopieren"
                        Funkverbindung.Lage.Verbindet -> "Verbindung wird aufgebaut …"
                        Funkverbindung.Lage.Wiederverbinden -> "Verbindung kommt zurück …"
                        Funkverbindung.Lage.Getrennt -> "Getrennt"
                    },
                    mono = true,
                )
            }

            Knopf("Verlassen", beiVerlassen, art = Knopfart.Gefahr, kompakt = true)
        }

        // Für wen hier disponiert wird — vor Dienstbeginn und nicht erst am
        // Arbeitsplatz. Der Sitz steht dabei, weil „ILS Bodensee-Oberschwaben"
        // ihn nicht verrät, und die Nachbarkreise, weil sie die Schicht größer
        // machen als den Kreisnamen daneben.
        if (raum != null) {
            raum.settings.leitstelle?.takeIf { it.isNotBlank() }?.let { name ->
                Text(
                    text = name,
                    style = Schrift.Gross,
                    color = Farben.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val mitkreise = raum.settings.mitkreise.mapNotNull { id ->
                katalog?.landkreise?.firstOrNull { it.id == id }?.name
            }
            val unter = listOfNotNull(
                raum.settings.leitstellensitz?.takeIf { it.isNotBlank() }?.let { "Sitz $it" },
                raum.settings.landkreis ?: raum.settings.ort,
                mitkreise.takeIf { it.isNotEmpty() }?.let { "mit ${it.joinToString(", ")}" },
            ).joinToString(" · ")
            if (unter.isNotBlank()) SehrLeise(unter, mono = true)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            ) {
                // Vor der Spielerzahl und nicht darunter: Wer beitritt, muss
                // vorher wissen, dass es hier keine Punkte gibt — hinterher ist
                // die Schicht gefahren.
                if (raum.settings.sandkasten) {
                    Marke("Sandkasten — nicht gewertet", farbe = Farben.AmberHell)
                }
                // Menschen und nicht alle Besatzungen: „Spieler" heißt Spieler.
                Marke(
                    text = "${raum.menschen.size} / ${raum.maxSpieler} Spieler",
                    farbe = if (raum.menschen.size < 2) Farben.AmberHell else Farben.TextLeise,
                )
                Zuschauerzaehler(raum)
                if (raum.laeuft) Marke("Im Dienst", farbe = Farben.Amber)
            }
        }
    }
}

/**
 * Auge plus Zahl: wie viele gerade zusehen, ohne einen Platz zu belegen.
 *
 * <b>Nur wer wirklich dranhängt.</b> Ein Zuschauerplatz bleibt nach einem
 * Verbindungsabriss bestehen — mitgezählt würde er die Anzeige zu hoch ausweisen.
 * Bei null bleibt die Leiste aufgeräumt.
 */
@Composable
internal fun Zuschauerzaehler(raum: Raumzustand) {
    val sehen = raum.zuschauer.filter { it.verbunden }
    if (sehen.isEmpty()) return
    Marke(text = "👁 ${sehen.size}", farbe = Farben.TextLeise)
}

// ------------------------------------------------------------------- Rolle

/**
 * Teil 1 — welchen Platz man einnimmt.
 *
 * Die Leitstellenkarte sagt auf sich selbst, was ein Druck bewirkt: Rolle
 * nehmen, fragen, Anfrage zurückziehen. Darunter — nur beim Host — die Bitten
 * um den Tisch und um zusätzliche Plätze, dann die Fahrzeugwahl.
 */
@Composable
private fun ColumnScope.TeilRolle(
    raum: Raumzustand,
    ich: Spieler?,
    stand: Rundenstand,
    konto: Konto?,
    garage: Garage?,
    katalog: Katalog?,
    server: String,
    suche: String,
    beiSuche: (String) -> Unit,
    filter: String?,
    beiFilter: (String?) -> Unit,
    griffe: LobbyGriffe,
) {
    val istLeitstelle = ich?.istLeitstelle == true
    val besetzung = raum.leitstellen
    val leitstelleVoll = besetzung.size >= raum.maxLeitstellen

    // Die Rangschranke gilt nur in Runden für Fremde.
    val freischaltung = konto?.freischaltungen?.firstOrNull { it.was == "Leitstelle" }
    val freigeschaltet = raum.settings.freischaltungenIgnorieren || freischaltung?.offen == true
    val gesperrt = raum.settings.oeffentlich && !freigeschaltet
    val abRang = freischaltung?.abRang.orEmpty()
    val erstFragen = !istLeitstelle && besetzung.isNotEmpty()
    val anfrageLaeuft = stand.eigenePlatzanfrage

    Ueberschrift("Rolle wählen")

    Leitstellenkarte(
        istLeitstelle = istLeitstelle,
        anfrageLaeuft = anfrageLaeuft,
        gesperrt = gesperrt,
        abRang = abRang,
        erstFragen = erstFragen,
        besetzung = besetzung,
        maxLeitstellen = raum.maxLeitstellen,
        leitstelleVoll = leitstelleVoll,
        beiDruck = {
            when {
                istLeitstelle -> Unit
                anfrageLaeuft -> griffe.platzanfrageZuruecknehmen()
                else -> griffe.rolle("Leitstelle", null)
            }
        },
    )

    // Die Bitten um einen Platz — nur beim Host, und nur solange welche offen
    // sind. Sie stehen unter der Karte, weil sie zu ihr gehören.
    if (raum.istHost && raum.platzanfragen.isNotEmpty()) {
        val satz = if (leitstelleVoll) {
            "Es gibt nur einen Leitstellenplatz: Wer angenommen wird, übernimmt deinen Tisch — " +
                "du bleibst in der Runde und wählst danach ein Fahrzeug."
        } else {
            "Wer angenommen wird, gibt sein Fahrzeug ab und disponiert mit."
        }
        Anfragenkasten(
            titel = "Anfragen für den Tisch",
            anfragen = raum.platzanfragen,
            raum = raum,
            server = server,
            beiEntscheid = griffe.platzanfrageEntscheiden,
            fuss = "$satz Unbeantwortete Anfragen verfallen nach fünf Minuten.",
        )
    }

    // Und die andere Bitte: nicht um den Tisch, sondern um einen Platz überhaupt
    // — von jemandem, der zusieht, weil die Runde voll war.
    if (raum.istHost && raum.beitrittsanfragen.isNotEmpty()) {
        val vergeben = if (raum.ueberzaehlig > 0) " — ${raum.ueberzaehlig} davon sind schon vergeben" else ""
        Anfragenkasten(
            titel = "Bitten um einen zusätzlichen Platz",
            anfragen = raum.beitrittsanfragen,
            raum = raum,
            server = server,
            beiEntscheid = griffe.beitrittEntscheiden,
            fuss = "Die Runde ist voll (${raum.maxSpieler} Plätze). Wer angenommen wird, bekommt " +
                "einen Platz darüber hinaus$vergeben. Unbeantwortete Bitten verfallen nach fünf Minuten.",
        )
    }

    Fahrzeugwahl(
        raum = raum,
        ich = ich,
        istLeitstelle = istLeitstelle,
        garage = garage,
        katalog = katalog,
        suche = suche,
        beiSuche = beiSuche,
        filter = filter,
        beiFilter = beiFilter,
        griffe = griffe,
    )
}

/** Die Karte des Leitstellentischs — sie sagt vor dem Druck, was er bewirkt. */
@Composable
private fun Leitstellenkarte(
    istLeitstelle: Boolean,
    anfrageLaeuft: Boolean,
    gesperrt: Boolean,
    abRang: String,
    erstFragen: Boolean,
    besetzung: List<Spieler>,
    maxLeitstellen: Int,
    leitstelleVoll: Boolean,
    beiDruck: () -> Unit,
) {
    val aktiv = !(gesperrt && !istLeitstelle)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (istLeitstelle) Farben.HauchAmber else Farben.Flaeche,
                randfarbe = if (istLeitstelle) Farben.Amber else Farben.Rand,
            )
            .clickable(
                enabled = aktiv,
                onClick = beiDruck,
                role = Role.Button,
                indication = null,
                interactionSource = null,
            )
            .padding(Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Leitstelle",
                style = Schrift.Gross,
                color = if (aktiv) Farben.Text else Farben.TextSehrLeise,
                modifier = Modifier.weight(1f),
            )
            when {
                istLeitstelle -> Marke("Deine Rolle", farbe = Farben.Amber)
                anfrageLaeuft -> Marke("Anfrage läuft", farbe = Farben.Amber)
                gesperrt -> Marke("ab $abRang")
                erstFragen -> Marke("auf Anfrage")
            }
        }
        Text(
            text = "Nimmt Notrufe an, disponiert Fahrzeuge und führt den Funkverkehr.",
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
        // Der Grund steht auf der Karte, nicht in einer Meldung nach dem Druck:
        // Ein gesperrter Knopf ohne Grund sieht aus wie ein Fehler der Anwendung.
        if (gesperrt && !istLeitstelle) {
            SehrLeise(
                "In Runden für Fremde erst ab $abRang. In einer eigenen Runde mit Freunden geht " +
                    "sie sofort.",
            )
        }
        if (besetzung.isNotEmpty()) {
            val namen = besetzung.joinToString(", ") { it.name + if (it.live) " (live)" else "" }
            val zahl = if (maxLeitstellen > 1) " (${besetzung.size}/$maxLeitstellen)" else ""
            SehrLeise("Besetzt: $namen$zahl", mono = true)
        }
        when {
            anfrageLaeuft -> SehrLeise(
                "${besetzung.firstOrNull()?.name ?: "Der Host"} ist gefragt. Noch einmal drücken " +
                    "zieht die Anfrage zurück.",
            )

            erstFragen && !gesperrt -> {
                val wie = if (leitstelleVoll) {
                    ", ob er dir seinen Platz überlässt."
                } else {
                    ", ob du dich dazusetzen darfst."
                }
                SehrLeise("Am Tisch sitzt schon jemand — ein Druck fragt ${besetzung.first().name}$wie")
            }
        }
    }
}

/**
 * Die offenen Bitten mit Gesicht — hier wird über einen Menschen entschieden,
 * und derselbe Mensch soll aussehen wie zwei Zeilen später in der Mannschaft.
 */
@Composable
private fun Anfragenkasten(
    titel: String,
    anfragen: List<Platzanfrage>,
    raum: Raumzustand,
    server: String,
    beiEntscheid: (String, Boolean) -> Unit,
    fuss: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = Farben.AmberTief)
            .padding(Abstand.Normal),
    ) {
        Etikett("$titel (${anfragen.size})")
        anfragen.forEach { a ->
            val spieler = raum.players.firstOrNull { it.id == a.playerId }
            val zuschauer = raum.zuschauer.firstOrNull { it.id == a.playerId }
            Profilzeile(
                kennung = a.playerId,
                anzeigename = a.name,
                wappen = spieler?.wappen ?: zuschauer?.wappen ?: "Keines",
                wappenfarbe = spieler?.wappenfarbe ?: zuschauer?.wappenfarbe ?: 0,
                kopfmuster = spieler?.kopfmuster ?: zuschauer?.kopfmuster ?: "keines",
                profilrahmen = spieler?.profilrahmen ?: zuschauer?.profilrahmen ?: "keiner",
                bildAdresse = profilbildAdresse(server, spieler?.profilbild ?: zuschauer?.profilbild),
                teammitglied = spieler?.teammitglied ?: zuschauer?.teammitglied ?: false,
                unten = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            "Ablehnen",
                            { beiEntscheid(a.playerId, false) },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                        Knopf(
                            "Annehmen",
                            { beiEntscheid(a.playerId, true) },
                            art = Knopfart.Haupt,
                            kompakt = true,
                        )
                    }
                },
            )
        }
        SehrLeise(fuss)
    }
}

/**
 * Die Fahrzeugwahl — und im laufenden Dienst der zweite Weg daneben: eine
 * Bot-Besatzung übernehmen.
 *
 * <b>Als Reiter, nicht untereinander.</b> Beide Wege sind lang; untereinander
 * verlor man den jeweils anderen aus dem Blick. Die Zahl an jedem Reiter sagt,
 * was einen dort erwartet.
 */
@Composable
private fun Fahrzeugwahl(
    raum: Raumzustand,
    ich: Spieler?,
    istLeitstelle: Boolean,
    garage: Garage?,
    katalog: Katalog?,
    suche: String,
    beiSuche: (String) -> Unit,
    filter: String?,
    beiFilter: (String?) -> Unit,
    griffe: LobbyGriffe,
) {
    val aktiveOrgs = raum.settings.organisationen
    val hiOrgs = raum.settings.hiOrgs
    val verfuegbar = katalog?.fahrzeuge.orEmpty()
        .filter { it.organisation in aktiveOrgs }
        .filter { it.hiOrg == "Keine" || it.hiOrg.isBlank() || hiOrgs.isEmpty() || it.hiOrg in hiOrgs }

    // Ohne geladene Garage gilt alles als vorhanden — sonst stünde jemand vor
    // einer leeren Auswahl.
    val eigene = if (istLeitstelle) {
        verfuegbar
    } else {
        verfuegbar.filter {
            raum.settings.freischaltungenIgnorieren || garage == null || it.id in garage.fahrzeuge
        }
    }
    val wirksamerFilter = filter?.takeIf { it in aktiveOrgs }
    val gruppen = gruppiereFahrzeuge(
        eigene
            .filter { wirksamerFilter == null || it.organisation == wirksamerFilter }
            .filter { vorlagePasst(it, suche) },
    )
    val belegt = raum.vehicles.groupingBy { it.templateId }.eachCount()
    val meinTemplate = ich?.let { mich -> raum.vehicles.firstOrNull { it.playerId == mich.id }?.templateId }
    val botPlatz = raum.vehicles.size < MAX_FAHRZEUGE

    val imDienst = raum.laeuft && ich?.role == "Unbestimmt"
    val angebote = raum.players
        .filter { it.istBot && it.vehicleId != null }
        .mapNotNull { bot -> raum.vehicles.firstOrNull { it.id == bot.vehicleId }?.let { bot to it } }
    var weg by remember { mutableStateOf<String?>(null) }
    val wegAktiv = weg ?: if (angebote.isNotEmpty()) "bot" else "eigen"

    if (imDienst) {
        Pillenreihe {
            Pille(
                aufschrift = "Bot übernehmen",
                an = wegAktiv == "bot",
                zahl = angebote.size,
                beiDruck = { weg = "bot" },
            )
            Pille(
                aufschrift = "Neues Fahrzeug besetzen",
                an = wegAktiv == "eigen",
                zahl = eigene.size,
                beiDruck = { weg = "eigen" },
            )
        }
    }

    if (imDienst && wegAktiv == "bot") {
        SehrLeise(
            "Übernimm eine vorhandene Bot-Besatzung: Fahrzeug, Status und laufender Einsatz " +
                "bleiben erhalten — du sitzt sofort im Geschehen.",
        )
        if (angebote.isEmpty()) {
            Leerhinweis(
                "Im Moment ist kein Bot-Fahrzeug frei. Du kannst ein eigenes Fahrzeug in den " +
                    "Dienst stellen oder einen freien Leitstellenplatz übernehmen.",
                ausweg = {
                    Knopf("Neues Fahrzeug besetzen", { weg = "eigen" }, art = Knopfart.Haupt, kompakt = true)
                },
            )
        } else {
            angebote.forEach { (bot, fahrzeug) ->
                Botangebot(raum, fahrzeug) { griffe.botUebernehmen(bot.id) }
            }
        }
        return
    }

    if (imDienst) {
        SehrLeise(
            "Zur Wahl stehen die Fahrzeuge aus deiner Garage, die in dieser Runde aktiv sind. Sie " +
                "kommen einsatzbereit neu in den Dienst.",
        )
    }

    // Suche und Filter — in einer gewachsenen Garage sind es achtzig Karten in
    // einem Dutzend Gruppen.
    Feld(
        wert = suche,
        beiAenderung = { beiSuche(it.take(40)) },
        platzhalter = "Fahrzeug, Fähigkeit oder Träger suchen …",
    )
    Pillenreihe {
        Pille(aufschrift = "Alle", an = wirksamerFilter == null, beiDruck = { beiFilter(null) })
        Rundentexte.ORGANISATIONEN.filter { it in aktiveOrgs }.forEach { org ->
            Pille(
                aufschrift = Rundentexte.organisation(org),
                an = wirksamerFilter == org,
                farbe = Rundentexte.organisationFarbe(org),
                beiDruck = { beiFilter(org) },
            )
        }
    }

    gruppen.forEach { gruppe ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Etikett(gruppe.schluessel)
            SehrLeise(gruppe.traeger.sumOf { it.fahrzeuge.size }.toString(), mono = true)
        }
        gruppe.traeger.forEach { cluster ->
            if (gruppe.traeger.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(Rundentexte.traegerFarbe(cluster.hiOrg), CircleShape),
                    )
                    SehrLeise(cluster.aufschrift, mono = true)
                }
            }
            cluster.fahrzeuge.forEach { f ->
                Fahrzeugkarte(
                    vorlage = f,
                    gewaehlt = meinTemplate == f.id,
                    anzahl = belegt[f.id] ?: 0,
                    mitBot = istLeitstelle,
                    botPlatz = botPlatz,
                    beiWahl = { griffe.rolle("Fahrzeugbesatzung", f.id) },
                    beiBot = { griffe.bot(f.id, 1) },
                )
            }
        }
    }

    if (gruppen.isEmpty() && suche.isNotBlank()) {
        SehrLeise(
            "Nichts gefunden. Andere Schreibweise probieren — gesucht wird über Typ, " +
                "Beschreibung, Fähigkeit und Träger.",
            mono = true,
        )
    } else if (gruppen.isEmpty()) {
        SehrLeise(
            "In dieser Runde steht aus deiner Garage nichts zur Wahl. Die Leitstelle kann " +
                "weitere Organisationen oder Träger zuschalten.",
            mono = true,
        )
    }

    val offeneWahlen = garage?.offeneWahlen ?: 0
    if (offeneWahlen > 0) {
        val mehrzahl = if (offeneWahlen == 1) "" else "e"
        Text(
            text = "Du hast $offeneWahlen Fahrzeuggutschein$mehrzahl offen. Einlösen im Shop.",
            style = Schrift.MonoKlein,
            color = Farben.AmberHell,
        )
    }
}

/** Ein Bot-Fahrzeug im laufenden Dienst — mit seinem Einsatz, wenn es einen hat. */
@Composable
private fun Botangebot(raum: Raumzustand, fahrzeug: Rundenfahrzeug, beiUebernehmen: () -> Unit) {
    val einsatz = fahrzeug.einsatzId?.let { id -> raum.incidents.firstOrNull { it.id == id } }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        Text(text = fahrzeug.funkrufname, style = Schrift.MonoNormal, color = Farben.Text)
        SehrLeise("${fahrzeug.typ} · Status ${fahrzeug.status} ${fahrzeug.statusText}")
        SehrLeise(
            einsatz?.let { "${it.einsatznummer} · ${it.stichwort} · ${it.adresse}" }
                ?: "Derzeit ohne Einsatzbindung",
            mono = true,
        )
        Knopf("Bot übernehmen", beiUebernehmen, art = Knopfart.Haupt, kompakt = true)
    }
}

/** Eine Karte der Fahrzeugwahl — `FahrzeugKarte.vue` in der Handyform. */
@Composable
private fun Fahrzeugkarte(
    vorlage: Fahrzeugvorlage,
    gewaehlt: Boolean,
    anzahl: Int,
    mitBot: Boolean,
    botPlatz: Boolean,
    beiWahl: () -> Unit,
    beiBot: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (gewaehlt) Farben.HauchAmber else Farben.Flaeche,
                randfarbe = if (gewaehlt) Farben.Amber else Farben.Rand,
                ecke = 9.dp,
            )
            .clickable(
                onClick = beiWahl,
                role = Role.Button,
                indication = null,
                interactionSource = null,
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 32.dp)
                .background(Rundentexte.organisationFarbe(vorlage.organisation), Rundung.Rund),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = vorlage.typ,
                style = Schrift.Normal,
                color = if (gewaehlt) Farben.Amber else Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val unter = listOfNotNull(
                vorlage.beschreibung.ifBlank { null },
                vorlage.besatzung.ifBlank { null }?.let { "Besatzung $it" },
            ).joinToString(" · ")
            if (unter.isNotBlank()) SehrLeise(unter)
            if (anzahl > 0) SehrLeise("$anzahl× besetzt", mono = true)
        }
        // Nur die Leitstelle darf Besatzungen einteilen.
        if (mitBot) {
            Knopf(
                aufschrift = "+ Bot",
                beiDruck = beiBot,
                art = Knopfart.Leise,
                kompakt = true,
                aktiv = botPlatz,
            )
        }
    }
}

// --------------------------------------------------------------- Startblock

/**
 * Der Dienstbeginn — am Handy die klebende Leiste über der Reiterreihe.
 *
 * <b>Drei Bedingungen, drei Haken</b>, und der Satz sagt, was als Nächstes dran
 * ist. Die Leitstelle beginnt den Dienst; alle anderen melden sich bereit.
 */
@Composable
private fun Startblock(
    raum: Raumzustand,
    ich: Spieler?,
    laeuft: Boolean,
    griffe: LobbyGriffe,
) {
    val ohneLeitstelle = raum.players.filter { !it.istLeitstelle }
    val ohneWahl = ohneLeitstelle.count { it.vehicleId == null }
    val nichtBereit = ohneLeitstelle.count { !it.bereit }
    val startbereit = raum.players.size >= 2 && raum.vehicles.isNotEmpty() &&
        ohneLeitstelle.all { it.bereit && it.vehicleId != null }
    val hinweis = when {
        raum.players.size < 2 -> "Es fehlt noch mindestens ein Mitspieler."
        raum.vehicles.isEmpty() -> "Mindestens ein Spieler muss ein Fahrzeug besetzen."
        ohneWahl > 0 -> "Es warten noch Mannschaftsmitglieder auf ihre Fahrzeugwahl."
        nichtBereit > 0 -> "Noch nicht alle Mannschaftsmitglieder sind bereit."
        else -> null
    }
    val bots = raum.bots.size
    val plaetze = if (bots > 0) {
        "${raum.menschen.size} + $bots Bot${if (bots == 1) "" else "s"}"
    } else {
        "${raum.menschen.size}"
    }
    val besatzungen = when {
        ohneWahl > 0 -> "$ohneWahl ${if (ohneWahl == 1) "wählt" else "wählen"} noch"
        nichtBereit > 0 -> "$nichtBereit noch nicht bereit"
        else -> "vollzählig"
    }
    val schritte = listOf(
        Triple("Plätze besetzt", plaetze, raum.players.size >= 2),
        Triple("Fahrzeuge im Dienst", "${raum.vehicles.size}", raum.vehicles.isNotEmpty()),
        Triple("Besatzungen bereit", besatzungen, ohneWahl == 0 && nichtBereit == 0),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.Flaeche)
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(Farben.Rand, Offset(0f, strich / 2f), Offset(size.width, strich / 2f), strich)
            }
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dienstbeginn", style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f))
            Marke(
                if (startbereit) "startklar" else "noch nicht startklar",
                farbe = if (startbereit) Farben.GruenHell else Farben.AmberHell,
            )
        }
        schritte.forEach { (wort, wert, erfuellt) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (erfuellt) "✓" else "·",
                    style = Schrift.MonoKlein,
                    color = if (erfuellt) Farben.GruenHell else Farben.TextSehrLeise,
                )
                Text(
                    wort,
                    style = Schrift.Klein,
                    color = if (erfuellt) Farben.Text else Farben.TextLeise,
                    modifier = Modifier.weight(1f),
                )
                SehrLeise(wert, mono = true)
            }
        }

        if (ich?.istLeitstelle == true) {
            Knopf(
                aufschrift = "Dienst beginnen",
                beiDruck = griffe.start,
                art = Knopfart.Haupt,
                aktiv = startbereit && !laeuft,
                breit = true,
            )
            if (hinweis != null) SehrLeise(hinweis)
        } else {
            Knopf(
                aufschrift = if (ich?.bereit == true) "Bereit" else "Bereit melden",
                beiDruck = { griffe.bereit(ich?.bereit != true) },
                art = if (ich?.bereit == true) Knopfart.Haupt else Knopfart.Normal,
                aktiv = ich != null,
                breit = true,
            )
            val leitung = raum.leitstellen
            val wer = if (leitung.size == 1) leitung.first().name else "Die Leitstelle"
            SehrLeise("$wer startet die Runde.")
        }
    }
}

/**
 * Wie viele Fahrzeuge eine Runde trägt — `MAX_FAHRZEUGE` im Web. Darüber
 * nimmt der Server keine Bot-Besatzung mehr an.
 */
internal const val MAX_FAHRZEUGE = 400
