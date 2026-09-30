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
import androidx.compose.ui.graphics.Brush
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
import de.pagerspass.pagerspass.ui.theme.raster
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

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
) {
    var reiter by remember { mutableStateOf(Lobbyteil.Rolle) }

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
                            TeilRolle(raum, ich, garage, fahrzeuge, beiRolle, befehle)
                            Anfragenkasten(raum, befehle)
                            // Der späte Beitritt: Wer mitten im Dienst ohne Platz
                            // dasteht, steigt am schnellsten in ein Bot-Fahrzeug.
                            if (raum.laeuft && ich?.role == "Unbestimmt") BotUebernahme(raum, befehle)
                        }
                        Lobbyteil.Runde -> TeilRunde(raum, ich, neben, befehle)
                        Lobbyteil.Mannschaft -> TeilMannschaft(raum, ich, fahrzeuge, neben, befehle)
                        Lobbyteil.Chat -> TeilChat(raum, chatsatz, { chatsatz = it }) {
                            beiChat(chatsatz)
                            chatsatz = ""
                        }
                        Lobbyteil.Mehr -> TeilMehr(stand, ich, neben, befehle)
                    }
                }
            }
        }

        // <b>Der Startblock klebt unten.</b> Bei kurzen Teilen (Mehr, Chat) sitzt
        // er sonst mitten im Bild; bei langen wäre er unerreichbar. Im Web löst
        // das `margin: auto 0 0` plus `sticky bottom: 0` — hier steht er
        // schlicht außerhalb der Rollfläche.
        if (raum != null && !raum.laeuft) {
            Startblock(raum = raum, ich = ich, laeuft = stand.laeuft, beiBereit = beiBereit, beiStart = beiStart)
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
    if (amTisch.isNotEmpty()) {
        SehrLeise(
            "Leitstelle besetzt: ${amTisch.joinToString(", ") { it.name }}" +
                if (raum.maxLeitstellen > 1) " (${amTisch.size}/${raum.maxLeitstellen})" else "",
            mono = true,
        )
    }

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

/** Teil 2 — worauf man sich einlässt, und die Regler der Leitstelle. */
@Composable
private fun ColumnScope.TeilRunde(
    raum: Raumzustand,
    ich: Spieler?,
    neben: Raumneben,
    befehle: Raumbefehle,
) {
    Ueberschrift("Die Runde")

    Zeile("Ausrückebereich", raum.settings.leitstelle ?: "—")
    Zeile("Landkreis", raum.settings.landkreis ?: raum.settings.ort ?: "—")
    Zeile("Höchstens", "${raum.maxSpieler} Spieler")
    Zeile("Öffentlich", if (raum.settings.oeffentlich) "ja" else "nein")
    Zeile("Gewertet", if (raum.settings.sandkasten) "nein" else "ja")
    Zeile("Modus", RAUM_MODUS[raum.settings.mode] ?: raum.settings.mode)

    if (raum.settings.sandkasten) {
        SehrLeise(
            "Im Sandkasten gibt es keine Erfahrung, keine Rangliste und keine " +
                "Saisonwertung — gefahren wird trotzdem echt.",
        )
    }

    Rundenregler(raum, ich?.istLeitstelle == true, neben, befehle)
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

    // Bots setzt nur die Leitstelle. Wer keine ist, sieht nur die Zahl: Ein Knopf,
    // der bei jedem Druck „nicht erlaubt" antwortet, ist schlechter als keiner.
    if (istLeitstelle) {
        Botverwaltung(raum, fahrzeuge, befehle)
    } else if (raum.players.any { it.istBot }) {
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

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Knopf(
            aufschrift = "Code kopieren",
            beiDruck = { zwischenablage.setText(AnnotatedString(stand.code)) },
            kompakt = true,
        )
    }

    val raum = stand.raum
    if (raum != null && ich?.istLeitstelle == true && raum.inLobby) {
        Rundenvorlage(raum, neben, befehle)
    }

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
 * Der Startblock — bereit melden und den Dienst beginnen.
 *
 * <b>Was fehlt, steht als Satz daneben.</b> Drei Bedingungen prüft der Hub: eine
 * Leitstelle, niemand ohne Platz, alle bereit. Die App rechnet sie nach, um zu
 * sagen, welche gerade offen ist — durchsetzen tut es weiter der Server.
 */
@Composable
private fun Startblock(
    raum: Raumzustand,
    ich: Spieler?,
    laeuft: Boolean,
    beiBereit: (Boolean) -> Unit,
    beiStart: () -> Unit,
) {
    // Dieselbe Reihenfolge wie `startHinweis` im Web. Die Leitstelle meldet sich
    // nicht bereit — sie ist es, sobald sie am Tisch sitzt.
    val mannschaft = raum.players.filter { !it.istLeitstelle }
    val hindernis = when {
        !raum.hatLeitstelle -> "Es fehlt eine Leitstelle."
        raum.players.size < 2 -> "Es fehlt noch mindestens ein Mitspieler."
        raum.vehicles.isEmpty() -> "Mindestens ein Spieler muss ein Fahrzeug besetzen."
        mannschaft.any { it.vehicleId == null } ->
            "Es warten noch Mannschaftsmitglieder auf ihre Fahrzeugwahl."
        mannschaft.any { !it.bereit } -> "Noch nicht alle Mannschaftsmitglieder sind bereit."
        // Im Streamer-Modus startet die Runde erst, wenn alle geantwortet haben.
        raum.settings.streamermodus && raum.players.any { !it.istBot && !it.streamerfreigabe } ->
            "Noch nicht alle haben der Übertragung zugestimmt — wer, steht unter Mehr → Streamen."
        else -> null
    }

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
        if (hindernis != null) SehrLeise(hindernis)

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Knopf(
                aufschrift = if (ich?.bereit == true) "Doch nicht" else "Bereit",
                beiDruck = { beiBereit(ich?.bereit != true) },
                art = if (ich?.bereit == true) Knopfart.Leise else Knopfart.Normal,
                aktiv = ich != null,
                modifier = Modifier.weight(1f),
            )

            if (ich?.istLeitstelle == true) {
                Knopf(
                    aufschrift = "Dienst beginnen",
                    beiDruck = beiStart,
                    art = Knopfart.Haupt,
                    aktiv = hindernis == null && !laeuft,
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}
