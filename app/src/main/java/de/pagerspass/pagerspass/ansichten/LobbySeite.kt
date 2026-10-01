package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Spieler
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Markenzahl
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Teil
import de.pagerspass.pagerspass.ui.bausteine.Teilleiste
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Profilzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Erhebung
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.kopfverlauf
import de.pagerspass.pagerspass.ui.theme.seitengrund
import de.pagerspass.pagerspass.ui.zeichen.Zeichen
import androidx.compose.runtime.LaunchedEffect
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.mobil.Einstellungsaenderung
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Kasten

/**
 * Die Lobby — übertragen aus `web/src/views/LobbyView.vue` in ihrer Handyform.
 *
 * <b>Am Handy ist sie keine Rolle, sondern drei Teile:</b> fester Kopf oben,
 * **ein** Teil in der Mitte, Reiterleiste unten am Daumen — Rolle · Runde ·
 * Mannschaft · Chat · Mehr. Dieselbe Bauform wie die Welt am Handy.
 *
 * <b>Der Reiterwechsel wirft nichts weg.</b> Im Web ist die Auswahl ein
 * Attributselektor und kein `v-if`, damit jeder Teil beim Umschalten seinen
 * Zustand behält — die Rollposition der Fahrzeugliste, der halb getippte
 * Chatsatz. Hier steht dafür der Zustand der Teile *außerhalb* des `when`, in
 * `remember` am Rahmen; ein `remember` im ungezeigten Zweig wäre weg.
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
    garage: List<String> = emptyList(),
    fahrzeuge: List<Fahrzeugvorlage> = emptyList(),
    beiRolle: (String, String?) -> Unit = { _, _ -> },
    beiBereit: (Boolean) -> Unit = {},
    beiBot: (String) -> Unit = {},
    beiChat: (String) -> Unit = {},
    beiStart: () -> Unit = {},
    beiVerlassen: () -> Unit = {},
    befehle: Raumbefehle = Raumbefehle.Leer,
    neben: Raumneben = Raumneben(),
    katalog: Katalog? = null,
    /** Die eigene Freundesliste — für „Freunde einladen" unter Mehr. */
    freunde: List<de.pagerspass.pagerspass.netz.Freund> = emptyList(),
    beiFreundeLaden: () -> Unit = {},
    /** Einladen (Kennung, als Zuschauer); der Rückruf bekommt den Fehlersatz oder `null`. */
    beiEinladen: ((String, Boolean, (String?) -> Unit) -> Unit)? = null,
) {
    var reiter by remember { mutableStateOf(Lobbyteil.Rolle) }

    // Die offene Seite des Rundendialogs — `null` heißt: zu. Eine Seite und kein
    // Ja/Nein, weil der Dialog von verschiedenen Stellen aus aufgeht: vom Knopf unter
    // dem Steckbrief auf die erste Seite, von einer Zeile des Steckbriefs auf die Seite
    // dieser Einstellung, von der Botliste auf „Bots".
    var einstellungen by remember { mutableStateOf<Rundenseite?>(null) }

    // Der Chatsatz lebt hier und nicht im Chat-Teil: Wer mitten im Tippen den
    // Reiter wechselt, soll seinen Satz wiederfinden.
    var chatsatz by remember { mutableStateOf("") }

    // Ungelesene Zeilen — die Marke am Reiter „Chat". <b>Sie zählt ab dem
    // Betreten</b>, nicht ab null: Wer die Lobby betritt, bekommt keine Marke
    // für Gespräche, die vor ihm liefen.
    val raum = stand.raum
    var gelesen by remember { mutableIntStateOf(raum?.lobbychat?.size ?: 0) }
    if (reiter == Lobbyteil.Chat) gelesen = raum?.lobbychat?.size ?: 0
    val ungelesen = ((raum?.lobbychat?.size ?: 0) - gelesen).coerceAtLeast(0)

    val ich = raum?.players?.firstOrNull { it.id == eigeneKennung }
    val aussparung = WindowInsets.statusBars.asPaddingValues()
    val fuehrung = raum?.let { Lobbyfuehrung.von(it, ich) } ?: Lobbyfuehrung()

    // Einmal beim Betreten: Bietet der Server die KI-Schalter an? Ohne Antwort
    // bleiben sie verborgen, und alles andere geht wie vorher.
    LaunchedEffect(Unit) { befehle.serverangebotLaden() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .seitengrund(),
    ) {
        Dienstleiste(
            stand = stand,
            oben = aussparung.calculateTopPadding(),
            beiVerlassen = beiVerlassen,
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
                        Lobbyteil.Rolle -> {
                            TeilRolle(raum, ich, garage, fahrzeuge, beiRolle, befehle, fuehrung)
                            Anfragenkasten(raum, befehle)
                            // Der späte Beitritt: Wer mitten im Dienst ohne Platz
                            // dasteht, steigt am schnellsten in ein Bot-Fahrzeug.
                            if (raum.laeuft && ich?.role == "Unbestimmt") BotUebernahme(raum, befehle)
                        }
                        Lobbyteil.Runde -> TeilRunde(raum, fuehrung, fahrzeuge, befehle) { einstellungen = it }
                        Lobbyteil.Mannschaft -> TeilMannschaft(raum, ich, fahrzeuge, neben, befehle)
                        Lobbyteil.Chat -> TeilChat(raum, chatsatz, { chatsatz = it }) {
                            beiChat(chatsatz)
                            chatsatz = ""
                        }
                        Lobbyteil.Mehr -> TeilMehr(stand, ich, neben, befehle, freunde, beiFreundeLaden, beiEinladen)
                    }
                }
            }
        }

        // <b>Der Startblock klebt unten.</b> Bei kurzen Teilen (Mehr, Chat) sitzt
        // er sonst mitten im Bild; bei langen wäre er unerreichbar. Im Web löst
        // das `margin: auto 0 0` plus `sticky bottom: 0` — hier steht er
        // schlicht außerhalb der Rollfläche.
        if (raum != null && !raum.laeuft) {
            Startblock(
                raum = raum,
                ich = ich,
                fuehrung = fuehrung,
                laeuft = stand.laeuft,
                beiBereit = beiBereit,
                beiStart = beiStart,
            )
        }

        Teilleiste(
            teile = Lobbyteil.entries.map { Teil(it.name, it.titel, it.zeichen) },
            offen = reiter.name,
            marken = mapOf(
                Lobbyteil.Chat.name to ungelesen,
                // Nur, was auf eine Antwort wartet: die Bitten um einen Platz.
                Lobbyteil.Rolle.name to if (raum?.istHost == true) {
                    raum.platzanfragen.size + raum.beitrittsanfragen.size
                } else {
                    0
                },
            ),
            // Wer keinen Platz hat, hält die ganze Runde auf — und sucht den
            // Reiter, auf dem er das ändert.
            ruft = if (ich?.role == "Unbestimmt") setOf(Lobbyteil.Rolle.name) else emptySet(),
            beiWahl = { id -> reiter = Lobbyteil.valueOf(id) },
        )
    }

    val offen = einstellungen
    if (raum != null && offen != null) {
        Rundendialog(
            raum = raum,
            seite = offen,
            beiSeite = { einstellungen = it },
            einstellbar = fuehrung.einstellbar,
            fuehrtLobby = fuehrung.fuehrtLobby,
            kiLeitstelleSelbst = fuehrung.kiLeitstelleSelbst,
            istLeitstelle = ich?.istLeitstelle == true,
            premiumAktiv = ich?.premium == true,
            katalog = katalog,
            neben = neben,
            befehle = befehle,
            beiSchliessen = { einstellungen = null },
        )
    }
}

/**
 * Wer die Lobby führt — und was die KI-Leitstelle beim Dienstbeginn tun wird.
 *
 * <b>Die Lobby führt, wer am Tisch sitzt</b> — oder wer die KI-Leitstelle
 * eingeschaltet hat und selbst fährt (`KiLeitstelle.FuehrtLobby` am Server). Ohne
 * das hätte die Lobby nach seinem Wechsel ins Fahrzeug niemanden mehr, der den
 * Dienst beginnen darf. Setzt sich ein Mensch an den Tisch, führt wieder er.
 *
 * <b>Der Zustand der KI in Stufen</b> statt eines Hakens — dieselbe Rechnung wie
 * `KiLeitstelle.Wirksam`, nur so, dass die Karte sagen kann, *warum* sie nicht
 * übernimmt.
 */
internal data class Lobbyfuehrung(
    val fuehrtLobby: Boolean = false,
    val kiLeitstelleSelbst: Boolean = false,
    val einstellbar: Boolean = false,
    val menschAmTisch: Boolean = false,
    /** `aus`, `nurZufall`, `ohnePremium`, `pausiert` oder `bereit`. */
    val kiZustand: String = "aus",
) {
    companion object {
        fun von(raum: Raumzustand, ich: Spieler?): Lobbyfuehrung {
            val s = raum.settings
            val menschAmTisch = raum.players.any { !it.istBot && it.istLeitstelle }
            val fuehrt = ich?.istLeitstelle == true ||
                (s.kiLeitstelleAktiv && ich != null && raum.kiLeitstelleInhaberId == ich.id && !menschAmTisch)
            val premiumImRaum = raum.players.any { !it.istBot && it.premium }
            val ki = when {
                !s.kiLeitstelleAktiv -> if (s.mode == "Zufall") "aus" else "nurZufall"
                !premiumImRaum -> "ohnePremium"
                menschAmTisch -> "pausiert"
                else -> "bereit"
            }
            return Lobbyfuehrung(
                fuehrtLobby = fuehrt,
                kiLeitstelleSelbst = fuehrt && ich?.istLeitstelle != true,
                einstellbar = fuehrt && s.mode != "Tagesschicht",
                menschAmTisch = menschAmTisch,
                kiZustand = ki,
            )
        }
    }
}

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
 * <b>Er ist zusammengezogen.</b> Am Rechner stehen über Raumcode und
 * Ausrückebereich zwei Etiketten; am Handy fallen sie weg — sie kosteten
 * gemessene 82 Punkte für zwei Wörter, die aus dem Zusammenhang ohnehin
 * hervorgehen.
 */
@Composable
private fun Dienstleiste(
    stand: Rundenstand,
    oben: androidx.compose.ui.unit.Dp,
    beiVerlassen: () -> Unit,
) {
    val zwischenablage = LocalClipboardManager.current
    val raum = stand.raum

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            // Der schräge Verlauf des Tablet-Kopfs (Tabletlook, 30.09.2026).
            .kopfverlauf()
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
                        onClick = { zwischenablage.setText(AnnotatedString(stand.code)) },
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
                        Funkverbindung.Lage.Verbunden -> "tippen zum Kopieren"
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
        // Arbeitsplatz. Wer beitritt, sieht sonst nur einen Raumcode und weiß bis
        // zum ersten Einsatz nicht, in welcher Ecke Deutschlands er sitzt.
        if (raum != null) {
            val bereich = listOfNotNull(
                raum.settings.leitstelle?.ifBlank { null },
                raum.settings.landkreis?.ifBlank { null } ?: raum.settings.ort?.ifBlank { null },
            ).joinToString(" · ")

            if (bereich.isNotBlank()) {
                Text(
                    text = bereich,
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            ) {
                // Vor der Spielerzahl und nicht darunter: Wer beitritt, muss
                // vorher wissen, dass es hier keine Punkte gibt — hinterher ist
                // die Schicht gefahren.
                if (raum.settings.sandkasten) {
                    Marke("Nicht gewertet", farbe = Farben.AmberHell)
                }
                Marke(
                    text = "${raum.players.size} / ${raum.maxSpieler} Spieler",
                    farbe = if (raum.players.size < 2) Farben.AmberHell else Farben.TextLeise,
                )
                Zuschauerzaehler(raum)
                if (raum.settings.streamermodus) Marke("Wird übertragen", farbe = Farben.SignalHell)
            }
        }
    }
}

/** Teil 1 — welchen Platz man einnimmt. */
@Composable
private fun ColumnScope.TeilRolle(
    raum: Raumzustand,
    ich: Spieler?,
    garage: List<String>,
    fahrzeuge: List<Fahrzeugvorlage>,
    beiRolle: (String, String?) -> Unit,
    befehle: Raumbefehle,
    fuehrung: Lobbyfuehrung = Lobbyfuehrung(),
) {
    var wahlOffen by remember { mutableStateOf(false) }
    // Das Fahrzeug ist eine Laufzeit-Id, die Garage führt Baupläne — gesucht wird
    // deshalb über das Rundenfahrzeug, nicht über die Id am Spieler.
    val meines = raum.vehicles.firstOrNull { it.id == ich?.vehicleId }?.templateId ?: ich?.vehicleId
    val meinTyp = fahrzeuge.firstOrNull { it.id == meines }?.typ

    // Der Tisch: Sitzt schon jemand dort, wird aus dem Hinsetzen eine Bitte — und
    // ein zweiter Druck zieht sie zurück. Dieselbe Regel wie am Server.
    val amTisch = raum.players.filter { it.istLeitstelle }
    val eigeneAnfrage = raum.platzanfragen.any { it.playerId == ich?.id }
    val erstFragen = ich?.istLeitstelle != true && amTisch.isNotEmpty()

    Ueberschrift("Dein Platz")

    Pillenreihe {
        Pille(
            aufschrift = when {
                eigeneAnfrage -> "Leitstelle · Anfrage läuft"
                erstFragen -> "Leitstelle · auf Anfrage"
                else -> "Leitstelle"
            },
            an = ich?.istLeitstelle == true || eigeneAnfrage,
            beiDruck = {
                when {
                    ich?.istLeitstelle == true -> Unit
                    eigeneAnfrage -> befehle.platzanfrageZuruecknehmen()
                    else -> beiRolle("Leitstelle", null)
                }
            },
        )
        Pille(
            aufschrift = "Fahrzeug",
            an = ich?.istBesatzung == true,
            beiDruck = { wahlOffen = true },
            aktiv = garage.isNotEmpty(),
        )
    }

    if (garage.isEmpty()) {
        SehrLeise("Für einen Platz auf einem Fahrzeug brauchst du eines in deiner Garage.")
    } else {
        // <b>Eine Auswahl, keine Pillenreihe.</b> Hier standen alle Fahrzeuge der
        // Garage nebeneinander — bei 92 Stück sechs Bildschirmhöhen Pillen, durch
        // die man rollen musste, bevor irgendetwas anderes kam.
        Wahlfeld(
            etikett = "Dein Fahrzeug",
            wert = if (ich?.istBesatzung == true) meinTyp else null,
            beiDruck = { wahlOffen = true },
            platzhalter = "Fahrzeug aus deiner Garage wählen",
        )
    }

    if (ich?.role == "Unbestimmt" && !raum.laeuft) {
        SehrLeise("Ohne Platz kann der Dienst nicht beginnen.")
    }

    // Was der Druck auf „Leitstelle" bewirkt, steht daneben — nicht in einer
    // Meldung danach. An diesem Platz hängt die ganze Runde.
    when {
        eigeneAnfrage -> SehrLeise(
            "${amTisch.firstOrNull()?.name ?: "Der Host"} ist gefragt. Noch einmal drücken " +
                "zieht die Anfrage zurück.",
        )
        erstFragen -> SehrLeise(
            "Am Tisch sitzt schon ${amTisch.first().name} — ein Druck fragt, " +
                if (amTisch.size >= raum.maxLeitstellen) {
                    "ob du den Platz übernehmen darfst."
                } else {
                    "ob du dich dazusetzen darfst."
                },
        )
    }
    if (fuehrung.kiZustand == "bereit" && ich?.istLeitstelle != true) {
        SehrLeise("Die KI-Leitstelle hält den Tisch frei. Setzt du dich hin, disponierst du selbst, und sie pausiert.")
    }
    if (amTisch.isNotEmpty()) {
        SehrLeise(
            "Leitstelle besetzt: ${amTisch.joinToString(", ") { it.name }}" +
                if (raum.maxLeitstellen > 1) " (${amTisch.size}/${raum.maxLeitstellen})" else "",
            mono = true,
        )
    }

    KiLeitstellenkarte(raum, ich, fuehrung, befehle)

    if (wahlOffen) {
        val meine = fahrzeuge.filter { it.id in garage }.sortedBy { it.typ }
        Wahlblende(
            titel = "Dein Fahrzeug",
            gruppen = listOf(null to meine),
            aufschrift = { it.typ },
            unterschrift = { plan ->
                listOfNotNull(
                    plan.beschreibung.ifBlank { null },
                    plan.besatzung.ifBlank { null }?.let { "Besatzung $it" },
                ).joinToString(" · ").ifBlank { null }
            },
            gewaehlt = meine.firstOrNull { it.id == meines },
            beiWahl = { plan ->
                beiRolle("Fahrzeugbesatzung", plan.id)
                wahlOffen = false
            },
            beiSchliessen = { wahlOffen = false },
            suchbar = meine.size > 8,
        )
    }
}

/**
 * Teil 2 — der Steckbrief der Schicht und die Bot-Besatzungen.
 *
 * <b>Wo die Regler geblieben sind.</b> Bis hierher stand in diesem Reiter die
 * längste Fläche der Lobby: rund dreißig Regler untereinander. Sie stehen jetzt im
 * Rundendialog, geordnet nach Seiten — hier bleibt, was man *liest*: was für eine
 * Schicht das ist. Jede Zeile des Steckbriefs öffnet den Dialog auf der Seite, auf
 * der sie sich ändern lässt.
 */
@Composable
private fun ColumnScope.TeilRunde(
    raum: Raumzustand,
    fuehrung: Lobbyfuehrung,
    fahrzeuge: List<Fahrzeugvorlage>,
    befehle: Raumbefehle,
    beiEinstellungen: (Rundenseite) -> Unit,
) {
    val s = raum.settings
    val menschen = raum.players.count { !it.istBot }

    Ueberschrift("Rundeneinstellungen")

    // Wer disponiert, prägt die Schicht mehr als jede Regel darunter — und ist mit
    // der KI-Leitstelle keine Selbstverständlichkeit mehr.
    val amTisch = raum.players.filter { !it.istBot && it.istLeitstelle }
    val disponent = when {
        amTisch.isNotEmpty() -> amTisch.joinToString(", ") { it.name }
        fuehrung.kiZustand == "bereit" -> "KI-Leitstelle"
        else -> "niemand — der Tisch ist frei"
    }
    val (regelnAn, regelnVon) = regelstand(s)
    val zeilen = buildList<Triple<String, String, Rundenseite?>> {
        add(Triple("Disponiert", disponent, null))
        add(Triple("Spielmodus", RAUM_MODUS[s.mode] ?: s.mode, Rundenseite.Grund))
        add(Triple("Zeittempo", if (s.zeitmodus == "Simulation") "Simulation (schneller)" else "Echtzeit", Rundenseite.Grund))
        if (s.mode == "Zufall") {
            add(Triple("Einsatzdichte", EINSATZDICHTE_LABEL[s.einsatzdichte] ?: s.einsatzdichte, Rundenseite.Lage))
        }
        add(Triple("Störungen", STOERUNG_LABEL[s.stoerungshaeufigkeit] ?: s.stoerungshaeufigkeit, Rundenseite.Lage))
        add(
            Triple(
                "Jahreszeit",
                (JAHRESZEIT_LABEL[s.jahreszeit] ?: s.jahreszeit) + if (s.silvester) " · Silvester" else "",
                Rundenseite.Lage,
            ),
        )
        add(Triple("Einsatzregeln", "$regelnAn von $regelnVon an", Rundenseite.Regeln))
        add(
            Triple(
                "Organisationen",
                s.organisationen
                    .joinToString(", ") { o -> RAUM_ORGANISATIONEN.firstOrNull { it.first == o }?.second ?: o }
                    .ifBlank { "—" },
                Rundenseite.Orgs,
            ),
        )
        add(
            Triple(
                "Träger",
                if (s.hiOrgs.isEmpty()) "alle erlaubt" else s.hiOrgs.joinToString(", ") { RAUM_TRAEGER[it] ?: it },
                Rundenseite.Orgs,
            ),
        )
        add(Triple("Plätze", "$menschen von ${raum.maxSpieler}" + if (s.oeffentlich) " · öffentlich" else "", Rundenseite.Grund))
    }

    Kasten(innenraum = Abstand.Normal, abstandInnen = Abstand.Haar) {
        zeilen.forEach { (wort, wert, seite) ->
            Steckbriefzeile(wort, wert, seite?.let { ziel -> { beiEinstellungen(ziel) } })
        }

        // Ein Knopf für alle, mit zwei Aufschriften: Die Leitstelle stellt ein, alle
        // anderen lesen nach. Derselbe Dialog in beiden Fällen — gesperrt statt versteckt.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = Abstand.Klein),
        ) {
            SehrLeise(
                when {
                    !fuehrung.fuehrtLobby -> "Diese Einstellungen setzt die Leitstelle."
                    !fuehrung.einstellbar -> "Die Schicht des Tages ist für alle dieselbe."
                    else -> "Tippe auf eine Zeile, um sie zu ändern."
                },
                modifier = Modifier.weight(1f),
            )
            Knopf(
                if (fuehrung.einstellbar) "Runde einstellen" else "Alle Einstellungen",
                { beiEinstellungen(Rundenseite.Grund) },
                art = if (fuehrung.einstellbar) Knopfart.Haupt else Knopfart.Normal,
                kompakt = true,
            )
        }
    }

    if (s.sandkasten) {
        SehrLeise(
            "Im Sandkasten gibt es keine Erfahrung, keine Rangliste und keine " +
                "Saisonwertung — gefahren wird trotzdem echt.",
        )
    }

    // Bots setzt nur, wer die Lobby führt. Wer es nicht tut, liest die Liste — und
    // sieht über den Weg unten trotzdem, wie sie sich verhalten.
    if (fuehrung.fuehrtLobby) {
        Botverwaltung(raum, fahrzeuge, befehle) { beiEinstellungen(Rundenseite.Bots) }
    } else {
        Ueberschrift("Bot-Besatzungen")
        val bots = raum.players.filter { it.istBot }
        SehrLeise(
            "Der Server besetzt diese Fahrzeuge selbst: quittieren, ausrücken, eintreffen, Lage " +
                "melden, nachfordern. So läuft eine Runde auch zu zweit.",
        )
        if (bots.isEmpty()) SehrLeise("Noch keine Bot-Besatzungen eingeteilt.", mono = true)
        bots.forEach { b ->
            val f = raum.vehicles.firstOrNull { it.id == b.vehicleId }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Marke("Bot", farbe = Farben.ViolettHell)
                Text(
                    f?.funkrufname ?: b.name,
                    style = Schrift.MonoKlein,
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                f?.typ?.let { SehrLeise(it) }
            }
        }
        Row {
            Knopf(
                "Verhalten der Bots · ${BOT_TEMPO_LABEL[s.botTempo] ?: s.botTempo}, Funk ${if (s.botFunkAktiv) "an" else "aus"}",
                { beiEinstellungen(Rundenseite.Bots) },
                art = Knopfart.Leise,
                kompakt = true,
            )
        }
    }
}

/**
 * Eine Zeile des Steckbriefs — Wort links, Wert rechts. Mit Seite ist sie ein Weg:
 * Ein Druck öffnet den Dialog dort, wo sie sich ändern lässt.
 */
@Composable
private fun Steckbriefzeile(wort: String, wert: String, beiDruck: (() -> Unit)?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 36.dp)
            .then(
                if (beiDruck != null) {
                    Modifier.clickable(onClick = beiDruck, role = Role.Button, indication = null, interactionSource = null)
                } else {
                    Modifier
                },
            )
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(
                    Farben.Rand,
                    Offset(0f, size.height - strich / 2f),
                    Offset(size.width, size.height - strich / 2f),
                    strich,
                )
            }
            .padding(vertical = Abstand.Winzig),
    ) {
        Text(text = wort, style = Schrift.Klein, color = Farben.TextLeise, modifier = Modifier.weight(1f))
        Text(
            text = wert,
            style = Schrift.MonoKlein,
            color = Farben.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
        if (beiDruck != null) {
            Icon(Zeichen.Weiter, contentDescription = null, tint = Farben.TextSehrLeise, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * Die KI-Leitstelle (Premium) — eine Karte neben der Leitstellenkarte und kein Haken
 * darunter: Sie beantwortet dieselbe Frage („wer disponiert?"), nur mit der anderen
 * Antwort.
 *
 * <b>Warum sie ihren Zustand zeigt.</b> Ein Haken sagte nur „an". Ob sie wirklich
 * übernehmen würde, stand nirgends: Wer eingeschaltet hatte und noch am Tisch saß,
 * startete eine Schicht, in der er selbst disponierte. Jetzt steht an der Karte, was
 * beim Dienstbeginn tatsächlich geschieht.
 *
 * Sichtbar für den, der die Lobby führt, und für alle, sobald sie an ist — in den
 * beiden Modi, die man in der Lobby wählt. Die Sonderschichten bringen ihre
 * Leitstelle selbst mit.
 */
@Composable
private fun ColumnScope.KiLeitstellenkarte(
    raum: Raumzustand,
    ich: Spieler?,
    fuehrung: Lobbyfuehrung,
    befehle: Raumbefehle,
) {
    val s = raum.settings
    if (s.mode != "Zufall" && s.mode != "Frei") return
    if (!fuehrung.fuehrtLobby && !s.kiLeitstelleAktiv) return

    val premiumAktiv = ich?.premium == true
    val inhaber = raum.players.firstOrNull { it.id == raum.kiLeitstelleInhaberId }
    val amTisch = raum.players.filter { !it.istBot && it.istLeitstelle }
    val marke = when (fuehrung.kiZustand) {
        "nurZufall" -> "nur Zufall"
        "ohnePremium" -> "ohne Premium"
        "pausiert" -> "pausiert"
        "bereit" -> "übernimmt"
        else -> null
    }
    val lagesatz = when (fuehrung.kiZustand) {
        "nurZufall" -> "Nur bei Zufallseinsätzen: In der freien Vergabe denkt sich ein Mensch am Tisch die Lagen aus."
        "aus" -> if (premiumAktiv) {
            "Aus — am Tisch disponiert ein Mensch."
        } else {
            "Aus. Einschalten kann ein Premium-Mitglied; mit Mitspieler am Tisch geht es wie immer auch ohne."
        }
        "ohnePremium" -> "Eingeschaltet, aber ohne Premium-Konto in der Runde bleibt der Tisch leer. Es genügt eines."
        "pausiert" -> if (ich?.istLeitstelle == true && raum.kiLeitstelleInhaberId == ich.id) {
            "Du sitzt noch am Tisch — solange disponierst du selbst. Wähle ein Fahrzeug, dann übernimmt " +
                "die KI und du führst die Lobby weiter."
        } else {
            "${amTisch.joinToString(", ") { it.name }} disponiert selbst. Wird der Tisch frei, springt die KI ein."
        }
        else -> if (inhaber != null && inhaber.id != ich?.id) {
            "Mit dem Dienstbeginn disponiert die KI. Die Lobby führt ${inhaber.name}."
        } else {
            "Mit dem Dienstbeginn disponiert die KI. Du führst die Lobby aus dem Fahrzeug."
        }
    }

    Kasten(marke = fuehrung.kiZustand == "bereit", abstandInnen = Abstand.Klein) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "KI-Leitstelle",
                style = Schrift.Gross,
                color = if (fuehrung.kiZustand == "nurZufall") Farben.TextSehrLeise else Farben.Text,
                modifier = Modifier.weight(1f),
            )
            Marke("★ Premium", farbe = Farben.AmberHell)
            if (marke != null) Marke(marke, farbe = if (fuehrung.kiZustand == "bereit") Farben.Amber else Farben.TextLeise)
        }
        SehrLeise(
            "Hält den Tisch frei für alle, die fahren wollen: nimmt Notrufe an, fragt sie ab, alarmiert " +
                "nach dem Alarmvorschlag, erteilt das Wort, weist Zielkliniken zu und schließt " +
                "liegengebliebene Lagen. Am Funk ist sie ansprechbar — Nachforderungen, Klinik, wer noch " +
                "kommt, Einsatzdaten; mit KI-Funk versteht sie jeden Satz. Setzt sich ein Mensch an den " +
                "Tisch, pausiert sie.",
        )
        Text(lagesatz, style = Schrift.Klein, color = Farben.Text)

        if (fuehrung.fuehrtLobby && fuehrung.einstellbar) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf(
                    if (s.kiLeitstelleAktiv) "Abschalten" else "Einschalten",
                    { befehle.einstellungen(Einstellungsaenderung(kiLeitstelleAktiv = !s.kiLeitstelleAktiv)) },
                    art = if (s.kiLeitstelleAktiv) Knopfart.Normal else Knopfart.Haupt,
                    aktiv = s.kiLeitstelleAktiv || (premiumAktiv && s.mode == "Zufall"),
                    kompakt = true,
                )
                if (s.kiLeitstelleAktiv && fuehrung.kiLeitstelleSelbst) {
                    SehrLeise("Abgeschaltet führt die Lobby, wer sich an den Tisch setzt.", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Teil 3 — wer mitfährt. */
@Composable
private fun ColumnScope.TeilMannschaft(
    raum: Raumzustand,
    ich: Spieler?,
    fahrzeuge: List<Fahrzeugvorlage>,
    neben: Raumneben,
    befehle: Raumbefehle,
) {
    val istLeitstelle = ich?.istLeitstelle == true
    // Menschen und nicht alle Besatzungen: Bots sind Ausstattung, und eine Runde mit
    // einem Menschen und zwanzig Bots ist keine Runde mit 21 Spielern.
    val menschen = raum.players.filter { !it.istBot }

    Ueberschrift("Mannschaft (${menschen.size})")

    if (menschen.isEmpty()) {
        Leerhinweis("Noch niemand da.")
    } else {
        menschen.forEach { spieler ->
            Spielerzeile(
                spieler = spieler,
                funkrufname = raum.vehicles.firstOrNull { it.id == spieler.vehicleId }?.funkrufname,
                // Werfen darf nur die Leitstelle, und nie sich selbst.
                beiKick = if (istLeitstelle && spieler.id != ich?.id) {
                    { befehle.spielerKicken(spieler.id) }
                } else {
                    null
                },
            )
        }
    }

    // Die Bots selbst stehen unter „Runde" — hier nur die Zahl, als Ausstattung.
    if (raum.players.any { it.istBot }) {
        val bots = raum.players.count { it.istBot }
        SehrLeise("dazu $bots Bot-Besatzung${if (bots == 1) "" else "en"}")
    }

    Aufstellung(raum, istLeitstelle && raum.inLobby, fahrzeuge, neben, befehle)
}

/** Teil 4 — der Lobby-Chat. */
@Composable
private fun ColumnScope.TeilChat(
    raum: Raumzustand,
    satz: String,
    beiSatz: (String) -> Unit,
    beiSenden: () -> Unit,
) {
    Ueberschrift("Chat")

    if (raum.lobbychat.isEmpty()) {
        SehrLeise("Noch nichts gesagt.")
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
        ) {
            raum.lobbychat.takeLast(40).forEach { zeile ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (zeile.istSystem) "»" else zeile.absender,
                        style = Schrift.MonoKlein,
                        color = if (zeile.istSystem) Farben.TextSehrLeise else Farben.AmberHell,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = zeile.text,
                        style = Schrift.Klein,
                        color = if (zeile.istSystem) Farben.TextSehrLeise else Farben.Text,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Feld(
            wert = satz,
            beiAenderung = { beiSatz(it.take(200)) },
            platzhalter = "Etwas sagen …",
            weiterTaste = ImeAction.Send,
            modifier = Modifier.weight(1f),
        )
        Knopf("Senden", beiSenden, aktiv = satz.isNotBlank(), kompakt = true)
    }
}

/** Teil 5 — alles, was man einmal braucht. */
@Composable
private fun ColumnScope.TeilMehr(
    stand: Rundenstand,
    ich: Spieler?,
    neben: Raumneben,
    befehle: Raumbefehle,
    freunde: List<de.pagerspass.pagerspass.netz.Freund> = emptyList(),
    beiFreundeLaden: () -> Unit = {},
    beiEinladen: ((String, Boolean, (String?) -> Unit) -> Unit)? = null,
) {
    val zwischenablage = LocalClipboardManager.current

    Ueberschrift("Einladen")
    SehrLeise("Gib den Raumcode weiter — damit kommt jeder herein.")

    Text(
        text = stand.code,
        style = Schrift.MonoNormal.copy(fontSize = Schrift.ANZEIGE, letterSpacing = 0.18.em),
        color = Farben.Amber,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.BgTief, Rundung.Klein)
            .border(1.dp, Farben.AmberTief, Rundung.Klein)
            .padding(Abstand.Normal),
    )

    // Der QR-Code wie im Web (`LobbyView.vue`): dieselbe Adresse `/?raum=…`,
    // die die App auch selbst als Link annimmt. Der Server kommt aus der
    // Ablage — die Runde kennt ihn nicht, und am Vorabstand gebaut muss der
    // Code auch dorthin führen.
    val zusammenhang = androidx.compose.ui.platform.LocalContext.current
    val server by androidx.compose.runtime.produceState<String?>(null) {
        value = de.pagerspass.pagerspass.netz.Ablage(zusammenhang).server()
    }
    val adresse = server
    if (stand.code.isNotBlank() && adresse != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.fillMaxWidth(),
        ) {
            de.pagerspass.pagerspass.ui.bausteine.QrCode("$adresse/?raum=${stand.code}", groesse = 132.dp)
            SehrLeise("Scannen und beitreten")
        }
    }

    // Code und Link: Wer kein Konto hat, bekommt einen Link zum Weitergeben — wer
    // danebensteht, den Code zum Vorlesen.
    var kopiert by remember { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Knopf(
            aufschrift = if (kopiert == "code") "Code kopiert" else "Code kopieren",
            beiDruck = {
                zwischenablage.setText(AnnotatedString(stand.code))
                kopiert = "code"
            },
            kompakt = true,
        )
        if (adresse != null && stand.code.isNotBlank()) {
            Knopf(
                aufschrift = if (kopiert == "link") "Link kopiert" else "Beitrittslink kopieren",
                beiDruck = {
                    zwischenablage.setText(AnnotatedString("$adresse/?raum=${stand.code}"))
                    kopiert = "link"
                },
                art = Knopfart.Leise,
                kompakt = true,
            )
        }
    }

    // Die Rundenvorlage steht seit 5.0.0.26 im Rundendialog („Als Vorlage merken").
    val raum = stand.raum

    // Die Freundesliste — im selben Abschnitt wie Code, QR und Link: alle Wege, wie
    // jemand hereinkommt, an einer Stelle (`einladen` in `LobbyView.vue`).
    if (raum != null && beiEinladen != null) {
        LaunchedEffect(Unit) { beiFreundeLaden() }
        Freundeeinladen(raum, freunde, beiEinladen)
    }

    // Doppelter Zweck: Jeder hört einmal, wonach er gleich suchen muss — und ob der
    // Melder überhaupt laut ist.
    Ueberschrift("Melder")
    Meldertest()

    // Die eine Streamer-Stelle der Lobby: Übertragung der Runde und eigene
    // Live-Meldung in einem Dialog (`LiveKnopf.vue`). Hier und nicht am
    // Arbeitsplatz — es ist keine Einstellung des Platzes, sondern eine
    // Abmachung mit den anderen.
    if (raum != null) {
        var streamenOffen by remember { mutableStateOf(false) }
        Ueberschrift("Streamen")
        SehrLeise(
            if (ich?.live == true) "Du stehst gerade als live." else "Übertragung der Runde und die eigene Live-Meldung.",
        )
        Knopf(
            aufschrift = if (ich?.live == true) "LIVE — Streamen" else "Streamen",
            beiDruck = { streamenOffen = true },
            kompakt = true,
            art = if (ich?.live == true) Knopfart.Haupt else Knopfart.Normal,
        )
        if (streamenOffen) {
            Streamenblende(raum, ich?.id.orEmpty(), befehle) { streamenOffen = false }
        }
    }
}

/** Eine Zeile aus Etikett und Wert — die Bauform des Runden-Teils. */
@Composable
private fun Zeile(was: String, wert: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = was,
            style = Schrift.Klein,
            color = Farben.TextLeise,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = wert,
            style = Schrift.MonoKlein,
            color = Farben.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.4f),
            textAlign = TextAlign.End,
        )
    }
}

/** Eine Zeile je Spieler — mit Wappen, Platz und Bereitschaft. */
@Composable
private fun Spielerzeile(spieler: Spieler, funkrufname: String?, beiKick: (() -> Unit)? = null) {
    Profilzeile(
        kennung = spieler.id,
        anzeigename = spieler.name,
        unterzeile = listOfNotNull(
            when {
                spieler.istLeitstelle -> "Leitstelle"
                spieler.istBesatzung -> funkrufname ?: "Fahrzeug"
                else -> "wählt noch"
            },
            "Stufe ${spieler.level}",
        ).joinToString(" · "),
        premium = spieler.premium,
        teammitglied = spieler.teammitglied,
        hinten = {
            // Wer streamt, trägt es am Namen — dort, wo man erfährt, mit wem man fährt.
            if (spieler.live) Livemarke()
            when {
                spieler.istBot -> Marke("Bot", farbe = Farben.ViolettHell)
                !spieler.verbunden -> Marke("Weg", farbe = Farben.TextSehrLeise)
                spieler.istLeitstelle -> Marke("Am Tisch", farbe = Farben.AmberHell)
                spieler.bereit -> Marke("Bereit", farbe = Farben.GruenHell)
                else -> Marke("Wartet", farbe = Farben.AmberHell)
            }
            // Aus dem Raum werfen — ein Wort und nicht nur ein „×": Am Finger gibt
            // es kein „darüber", das erklärt, welches der beiden es ist.
            if (beiKick != null) Knopf("Werfen", beiKick, art = Knopfart.Gefahr, kompakt = true)
        },
    )
}

/**
 * Der Startblock — der Dienstbeginn, klebend über der Reiterleiste.
 *
 * <b>Was fehlt, steht als Liste daneben.</b> Ein einzelner Satz nannte immer nur die
 * *erste* Hürde: Wer sie behoben hatte, bekam den nächsten Satz, und so weiter —
 * drei Anläufe, um zu erfahren, was von Anfang an feststand. Jetzt stehen alle
 * Bedingungen gleichzeitig da, jede mit ihrem Haken; der Satz darunter sagt, was als
 * Nächstes dran ist. Durchsetzen tut es weiter der Server.
 *
 * <b>Wer die Lobby führt, beginnt; alle anderen melden sich bereit.</b> Die
 * Leitstelle meldet sich nicht bereit — sie ist es, sobald sie am Tisch sitzt. Wer
 * mit der KI-Leitstelle selbst fährt, meldet sich mit dem Start bereit
 * (`GameEngine.Handle(StartRound)`); ein Fahrzeug braucht er trotzdem.
 */
@Composable
private fun Startblock(
    raum: Raumzustand,
    ich: Spieler?,
    fuehrung: Lobbyfuehrung,
    laeuft: Boolean,
    beiBereit: (Boolean) -> Unit,
    beiStart: () -> Unit,
) {
    val mannschaft = raum.players.filter { !it.istLeitstelle }
    val bots = raum.players.count { it.istBot }
    val menschen = raum.players.size - bots
    val ohneWahl = mannschaft.count { it.vehicleId == null }
    val nichtBereit = mannschaft.count { !it.bereit && !(fuehrung.kiLeitstelleSelbst && it.id == ich?.id) }
    val kiFehlt = fuehrung.kiLeitstelleSelbst && fuehrung.kiZustand != "bereit"

    // Der Tisch als eigener Schritt — nur mit KI-Leitstelle. Ohne sie ist die Frage
    // beantwortet, bevor jemand den Startknopf überhaupt sieht.
    val schritte = buildList {
        if (raum.settings.kiLeitstelleAktiv) {
            add(
                Triple(
                    "Leitstelle",
                    when {
                        fuehrung.menschAmTisch -> "Mensch am Tisch"
                        fuehrung.kiZustand == "bereit" -> "KI"
                        else -> "frei"
                    },
                    fuehrung.menschAmTisch || fuehrung.kiZustand == "bereit",
                ),
            )
        }
        // Die Bedingung zählt Plätze, nicht Menschen — eine Bot-Besatzung ist einer davon.
        add(
            Triple(
                "Plätze besetzt",
                if (bots > 0) "$menschen + $bots Bot${if (bots == 1) "" else "s"}" else "$menschen",
                raum.players.size >= 2,
            ),
        )
        add(Triple("Fahrzeuge im Dienst", "${raum.vehicles.size}", raum.vehicles.isNotEmpty()))
        add(
            Triple(
                "Besatzungen bereit",
                when {
                    ohneWahl > 0 -> "$ohneWahl ${if (ohneWahl == 1) "wählt" else "wählen"} noch"
                    nichtBereit > 0 -> "$nichtBereit noch nicht bereit"
                    else -> "vollzählig"
                },
                ohneWahl == 0 && nichtBereit == 0,
            ),
        )
    }

    // Dieselbe Reihenfolge wie `startHinweis` im Web.
    val hindernis = when {
        !raum.hatLeitstelle && !fuehrung.kiLeitstelleSelbst -> "Es fehlt eine Leitstelle."
        raum.players.size < 2 -> "Es fehlt noch mindestens ein Mitspieler."
        raum.vehicles.isEmpty() -> "Mindestens ein Spieler muss ein Fahrzeug besetzen."
        kiFehlt -> "Die KI-Leitstelle übernimmt nicht — ohne Premium-Konto in der Runde bleibt der Tisch leer."
        ohneWahl > 0 -> "Es warten noch Mannschaftsmitglieder auf ihre Fahrzeugwahl."
        nichtBereit > 0 -> "Noch nicht alle Mannschaftsmitglieder sind bereit."
        // Im Streamer-Modus startet die Runde erst, wenn alle geantwortet haben.
        raum.settings.streamermodus && raum.players.any { !it.istBot && !it.streamerfreigabe } ->
            "Noch nicht alle haben der Übertragung zugestimmt — wer, steht unter Mehr → Streamen."
        else -> null
    }
    val startklar = hindernis == null

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
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Dienstbeginn", style = Schrift.Gross, color = Farben.Text, modifier = Modifier.weight(1f))
            Marke(
                if (startklar) "startklar" else "noch nicht startklar",
                farbe = if (startklar) Farben.GruenHell else Farben.AmberHell,
            )
        }

        // Die Prüfliste — zwei Spalten je Zeile, damit sie am Daumen kurz bleibt.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            maxItemsInEachRow = 2,
            modifier = Modifier.fillMaxWidth(),
        ) {
            schritte.forEach { (wort, wert, erfuellt) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (erfuellt) "✓" else "·",
                        style = Schrift.MonoKlein,
                        color = if (erfuellt) Farben.GruenHell else Farben.TextSehrLeise,
                    )
                    Text(
                        wort,
                        style = Schrift.Winzig,
                        color = if (erfuellt) Farben.Text else Farben.TextLeise,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        wert,
                        style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG),
                        color = Farben.TextSehrLeise,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (fuehrung.fuehrtLobby) {
            Knopf(
                aufschrift = "Dienst beginnen",
                beiDruck = beiStart,
                art = Knopfart.Haupt,
                aktiv = startklar && !laeuft,
                breit = true,
            )
            if (hindernis != null) {
                SehrLeise(hindernis, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Knopf(
                aufschrift = if (ich?.bereit == true) "Bereit" else "Bereit melden",
                beiDruck = { beiBereit(ich?.bereit != true) },
                art = if (ich?.bereit == true) Knopfart.Haupt else Knopfart.Normal,
                aktiv = ich != null,
                breit = true,
            )
            val amTisch = raum.players.filter { it.istLeitstelle }
            Leise(
                (if (amTisch.size == 1) amTisch.first().name else "Die Leitstelle") + " startet die Runde.",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
