package de.pagerspass.pagerspass.ansichten

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import de.pagerspass.pagerspass.mobil.Einstellungsaenderung
import de.pagerspass.pagerspass.mobil.Lobbystand
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Lobbynachricht
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.netz.STREAMERPLATTFORMEN
import de.pagerspass.pagerspass.netz.Spieler
import de.pagerspass.pagerspass.netz.plattformname
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Profilzeile
import de.pagerspass.pagerspass.ui.schmuck.profilbildAdresse
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Die drei übrigen Teile der Lobby — Mannschaft, Chat, Mehr — samt ihren
 * Dialogen. Übertragen aus `LobbyView.vue`, `lobby/LobbyChat.vue`,
 * `lobby/EinladenPanel.vue`, `lobby/RufnameDialog.vue`, `lobby/WacheDialog.vue`
 * und `lobby/LiveDialog.vue`.
 */

// -------------------------------------------------------------- Mannschaft

/**
 * Teil 3 — wer dabei ist, und die Aufstellung der Fahrzeuge.
 *
 * <b>Menschen mit Gesicht, Bots als Zahl.</b> Eine Bot-Besatzung hat kein
 * Profil; sie steht in der Aufstellung, nicht in der Mannschaft.
 */
@Composable
internal fun ColumnScope.TeilMannschaft(
    raum: Raumzustand,
    ich: Spieler?,
    eigeneKennung: String,
    server: String,
    freunde: List<Freund>,
    daten: Lobbystand,
    griffe: LobbyGriffe,
) {
    val istLeitstelle = ich?.istLeitstelle == true
    val befreundet = freunde.filter { it.bestaetigt || it.angefragt }.map { it.kennung }.toSet()
    var rufnameFuer by remember { mutableStateOf<String?>(null) }
    var wacheFuer by remember { mutableStateOf<String?>(null) }

    Ueberschrift("Mannschaft")

    raum.menschen.forEach { s ->
        val fahrzeug = s.vehicleId?.let { id -> raum.vehicles.firstOrNull { it.id == id } }
        val istIch = s.id == eigeneKennung
        val platz = when {
            s.istLeitstelle -> "Leitstelle"
            fahrzeug != null -> fahrzeug.kurzname.ifBlank { fahrzeug.funkrufname }
            else -> "wählt noch"
        }
        val unter = listOfNotNull(
            s.rang.ifBlank { null },
            "Stufe ${s.level}",
            platz,
        ).joinToString(" · ")
        Profilzeile(
            kennung = s.id,
            anzeigename = s.name + if (istIch) " (du)" else "",
            unterzeile = unter,
            wappen = s.wappen,
            wappenfarbe = s.wappenfarbe,
            kopfmuster = s.kopfmuster,
            profilrahmen = s.profilrahmen,
            bildAdresse = profilbildAdresse(server, s.profilbild),
            premium = s.premium,
            teammitglied = s.teammitglied,
            hinten = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!s.istLeitstelle && !raum.laeuft) {
                        Marke(
                            if (s.bereit) "Bereit ✓" else "Nicht bereit",
                            farbe = if (s.bereit) Farben.GruenHell else Farben.TextSehrLeise,
                        )
                    }
                    if (s.live) Marke("live", farbe = Farben.SignalHell)
                    if (!istIch && s.id !in befreundet && eigeneKennung.isNotBlank()) {
                        if (s.id in daten.angefragt) {
                            SehrLeise("angefragt", mono = true)
                        } else {
                            Knopf("+ Freund", { griffe.freundAnfragen(s.id) }, art = Knopfart.Leise, kompakt = true)
                        }
                    }
                    if (raum.istHost && !istIch) {
                        Knopf("×", { griffe.kicken(s.id) }, art = Knopfart.Gefahr, kompakt = true)
                    }
                }
            },
        )
    }

    val bots = raum.bots.size
    if (bots > 0) {
        SehrLeise("dazu $bots Bot-Besatzung${if (bots == 1) "" else "en"}", mono = true)
    }
    daten.freundMeldung?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }

    if (raum.vehicles.isNotEmpty()) {
        Etikett("Aufstellung (${raum.vehicles.size})")
        raum.vehicles.forEach { f ->
            val besatzung = raum.players.firstOrNull { it.id == f.playerId }
            Aufstellungszeile(
                fahrzeug = f,
                besatzung = besatzung,
                darfAendern = istLeitstelle && !raum.laeuft,
                beiRufname = { rufnameFuer = f.id },
                beiWache = {
                    griffe.raumwachenLaden()
                    wacheFuer = f.id
                },
            )
        }
        SehrLeise("Rufnamen und Wachen lassen sich nur hier vergeben — im laufenden Dienst stehen sie fest.")
    }

    // Ein Fahrzeug, das inzwischen weg ist, schließt seinen Dialog von selbst.
    rufnameFuer?.let { id -> raum.vehicles.firstOrNull { it.id == id } }?.let { f ->
        run {
            RufnameBlende(
                fahrzeug = f,
                raum = raum,
                beiSchliessen = { rufnameFuer = null },
                beiUebernehmen = { name, kurz ->
                    griffe.rufname(f.id, name, kurz)
                    rufnameFuer = null
                },
                beiZuruecksetzen = {
                    griffe.rufnameZuruecksetzen(f.id)
                    rufnameFuer = null
                },
            )
        }
    }

    wacheFuer?.let { id -> raum.vehicles.firstOrNull { it.id == id } }?.let { f ->
        run {
            WacheBlende(
                fahrzeug = f,
                daten = daten,
                beiSchliessen = { wacheFuer = null },
                beiWahl = { kennung ->
                    griffe.wacheZuweisen(f.id, kennung)
                    wacheFuer = null
                },
            )
        }
    }
}

/** Eine Zeile der Aufstellung — Rufname, Besatzung, und bei der Leitstelle ✎ und ⌂. */
@Composable
private fun Aufstellungszeile(
    fahrzeug: Rundenfahrzeug,
    besatzung: Spieler?,
    darfAendern: Boolean,
    beiRufname: () -> Unit,
    beiWache: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 28.dp)
                .background(Rundentexte.organisationFarbe(fahrzeug.organisation)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
            Text(
                text = fahrzeug.funkrufname,
                style = Schrift.MonoNormal,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val wer = when {
                besatzung == null -> "unbesetzt"
                besatzung.istBot -> "Bot"
                else -> besatzung.name
            }
            SehrLeise("${fahrzeug.typ} · $wer" + if (fahrzeug.rufnameVonHand) " · Rufname von Hand" else "")
        }
        if (darfAendern) {
            Knopf("⌂", beiWache, art = Knopfart.Leise, kompakt = true)
            Knopf("✎", beiRufname, art = Knopfart.Leise, kompakt = true)
        }
    }
}

/**
 * Den Funkrufnamen eines Fahrzeugs von Hand setzen — `RufnameDialog.vue`.
 * Ein belegter Name ist erlaubt, wird aber angesagt: zwei gleiche Rufnamen im
 * Funk sind die Übung, die jemand vielleicht will.
 */
@Composable
private fun RufnameBlende(
    fahrzeug: Rundenfahrzeug,
    raum: Raumzustand,
    beiSchliessen: () -> Unit,
    beiUebernehmen: (String, String?) -> Unit,
    beiZuruecksetzen: () -> Unit,
) {
    var name by remember(fahrzeug.id) { mutableStateOf(fahrzeug.funkrufname) }
    var kurz by remember(fahrzeug.id) { mutableStateOf(fahrzeug.kurzname) }
    val sauber = name.trim()
    val belegt = raum.vehicles.any {
        it.id != fahrzeug.id && it.funkrufname.equals(sauber, ignoreCase = true)
    }
    val letztesWort = sauber.split(' ').lastOrNull().orEmpty()

    Blende(
        titel = "Rufname",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = {
            if (fahrzeug.rufnameVonHand) {
                Knopf("Wieder wie im Buch", beiZuruecksetzen, art = Knopfart.Leise)
            }
            Knopf(
                "Übernehmen",
                { beiUebernehmen(sauber, kurz.trim().ifBlank { null }) },
                art = Knopfart.Haupt,
                aktiv = sauber.isNotBlank(),
            )
        },
    ) {
        SehrLeise("${fahrzeug.typ} · ${Rundentexte.organisation(fahrzeug.organisation)}")
        Feld(
            wert = name,
            beiAenderung = { name = it.take(48) },
            etikett = "Rufname im Funk",
        )
        if (belegt) {
            Text(
                "Diesen Rufnamen trägt schon ein anderes Fahrzeug der Runde.",
                style = Schrift.Klein,
                color = Farben.AmberHell,
            )
        }
        Feld(
            wert = kurz,
            beiAenderung = { kurz = it.take(16) },
            etikett = "Kurzform fürs Tableau",
            platzhalter = letztesWort,
        )
    }
}

/**
 * Das Fahrzeug einem anderen Hof zuordnen — `WacheDialog.vue`. Nur Wachen der
 * eigenen Organisation stehen zur Wahl; ein RTW auf der Feuerwache wäre ein
 * Fehler, den niemand gewollt hat.
 */
@Composable
private fun WacheBlende(
    fahrzeug: Rundenfahrzeug,
    daten: Lobbystand,
    beiSchliessen: () -> Unit,
    beiWahl: (String) -> Unit,
) {
    Blende(
        titel = "Wache für ${fahrzeug.funkrufname}",
        beiSchliessen = beiSchliessen,
        fuss = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise) },
    ) {
        val wachen = daten.raumwachen
        when {
            wachen == null -> Ladezeile("Die Wachen der Runde werden geholt …")
            else -> {
                val passend = wachen.filter {
                    it.organisation.isBlank() || fahrzeug.organisation.isBlank() ||
                        it.organisation == fahrzeug.organisation
                }
                daten.raumwachenFehler?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
                if (passend.isEmpty() && daten.raumwachenFehler == null) {
                    Leerhinweis("Für diese Organisation gibt es in der Runde keine Wache.")
                }
                passend.forEach { w ->
                    val hier = fahrzeug.id in w.fahrzeuge
                    val zahl = w.fahrzeuge.size
                    val belegung = when {
                        hier -> "steht hier"
                        zahl == 0 -> "noch niemand"
                        zahl == 1 -> "ein Fahrzeug"
                        else -> "$zahl Fahrzeuge"
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .flaeche(
                                farbe = if (hier) Farben.HauchAmber else Farben.Flaeche,
                                randfarbe = if (hier) Farben.Amber else Farben.Rand,
                                ecke = 9.dp,
                            )
                            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                            Text(w.name, style = Schrift.Normal, color = Farben.Text)
                            SehrLeise(
                                listOfNotNull(
                                    w.zugnummer.takeIf { it > 0 }?.let { "Zug $it" },
                                    belegung,
                                ).joinToString(" · "),
                                mono = true,
                            )
                        }
                        if (!hier) Knopf("Hierhin", { beiWahl(w.kennung) }, art = Knopfart.Normal, kompakt = true)
                    }
                }
                SehrLeise(
                    "Die Wache bestimmt, von wo das Fahrzeug ausrückt, und die Zahl vorn im " +
                        "Rufnamen — ein von Hand gesetzter Rufname bleibt stehen.",
                )
            }
        }
    }
}

// ---------------------------------------------------------------------- Chat

/**
 * Teil 4 — der Lobbychat, `lobby/LobbyChat.vue`.
 *
 * <b>@ spricht jemanden an.</b> Wer erwähnt wird, bekommt einen Ton (das macht
 * `Runde`); hier wird die Zeile hervorgehoben, und beim Tippen eines `@`
 * schlägt der Chat die ersten sechs Mitspieler vor.
 */
@Composable
internal fun ColumnScope.TeilChat(
    verlauf: List<Lobbynachricht>,
    raum: Raumzustand,
    eigeneKennung: String,
    teamBefehle: Boolean,
    satz: String,
    beiSatz: (String) -> Unit,
    beiSenden: () -> Unit,
) {
    Ueberschrift("Chat")

    if (verlauf.isEmpty()) {
        Leerhinweis("Noch nichts geschrieben. Mit @ sprichst du jemanden direkt an.")
    } else {
        verlauf.takeLast(200).forEach { n -> Chatzeile(n, eigeneKennung) }
    }

    // Die Vorschläge zum angefangenen @Namen — nur Menschen, nicht man selbst.
    val anfang = Regex("@([^@\\s]*)$").find(satz)
    if (anfang != null) {
        val teil = anfang.groupValues[1]
        val vorschlaege = raum.menschen
            .filter { it.id != eigeneKennung }
            .filter { teil.isEmpty() || it.name.startsWith(teil, ignoreCase = true) }
            .take(6)
        if (vorschlaege.isNotEmpty()) {
            Pillenreihe {
                vorschlaege.forEach { s ->
                    Pille(
                        aufschrift = "@${s.name}",
                        an = false,
                        beiDruck = { beiSatz(satz.substring(0, anfang.range.first) + "@${s.name} ") },
                    )
                }
            }
        }
    }

    // Die Befehle des Teams — nur als Hinweis, ausgeführt werden sie am Server.
    if (teamBefehle && satz.startsWith("/")) {
        val befehle = listOf(
            "/leeren" to "leert den Chat für alle",
            "/hilfe" to "zeigt die Befehle",
            "/vanish" to "blendet dich aus der Mannschaft aus",
        ).filter { it.first.startsWith(satz.trim().substringBefore(' ')) }
        befehle.forEach { (b, was) -> SehrLeise("$b — $was", mono = true) }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Feld(
            wert = satz,
            beiAenderung = { beiSatz(it.take(500)) },
            platzhalter = "Schreiben — @ erwähnt jemanden …",
            modifier = Modifier.weight(1f),
        )
        Knopf("Senden", { if (satz.isNotBlank()) beiSenden() }, art = Knopfart.Haupt, aktiv = satz.isNotBlank())
    }
}

/** Eine Zeile — mit Zeit, Namen und hervorgehobenen Erwähnungen. */
@Composable
private fun Chatzeile(n: Lobbynachricht, eigeneKennung: String) {
    val mich = eigeneKennung.isNotBlank() && eigeneKennung in n.erwaehnte
    val system = n.vonId.isBlank()
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (mich) {
                    Modifier.flaeche(farbe = Farben.HauchAmber, randfarbe = Farben.AmberTief, ecke = 9.dp)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = if (mich) Abstand.Klein else 0.dp, vertical = Abstand.Haar),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Text(
                text = if (system) "Leitstelle" else n.vonName,
                style = Schrift.Klein,
                color = if (n.vonId == eigeneKennung) Farben.Amber else Farben.TextLeise,
            )
            SehrLeise(uhrzeit(n.zeit), mono = true)
        }
        Text(
            text = mitErwaehnungen(n.text),
            style = Schrift.Normal,
            color = if (system) Farben.TextLeise else Farben.Text,
        )
    }
}

/** `@Name` in Bernstein — der Rest bleibt, wie er ist. */
private fun mitErwaehnungen(text: String): AnnotatedString = buildAnnotatedString {
    var ab = 0
    Regex("@[^@\\s]+").findAll(text).forEach { m ->
        append(text.substring(ab, m.range.first))
        withStyle(SpanStyle(color = Farben.Amber)) { append(m.value) }
        ab = m.range.last + 1
    }
    append(text.substring(ab))
}

private val UHR = DateTimeFormatter.ofPattern("HH:mm")

private fun uhrzeit(roh: String): String = runCatching {
    UHR.format(OffsetDateTime.parse(roh).atZoneSameInstant(ZoneId.systemDefault()))
}.getOrDefault("")

// ---------------------------------------------------------------------- Mehr

/**
 * Teil 5 — Einladen, Melder testen, Streamen, Spielhilfe.
 */
@Composable
internal fun ColumnScope.TeilMehr(
    raum: Raumzustand,
    ich: Spieler?,
    server: String,
    freunde: List<Freund>,
    einladeMeldung: String?,
    griffe: LobbyGriffe,
) {
    var alsZuschauer by remember { mutableStateOf(false) }
    var eingeladen by remember { mutableStateOf(emptySet<String>()) }
    var liveOffen by remember { mutableStateOf(false) }
    var kopiert by remember { mutableStateOf(false) }
    val zwischenablage = LocalClipboardManager.current
    LaunchedEffect(kopiert) {
        if (kopiert) {
            kotlinx.coroutines.delay(2_400)
            kopiert = false
        }
    }

    val link = "${server.trimEnd('/')}/?raum=${raum.code}"
    val frei = (raum.maxSpieler - raum.menschen.size).coerceAtLeast(0)

    Ueberschrift("Einladen")
    Pillenreihe {
        Pille(aufschrift = "Mitspieler · $frei frei", an = !alsZuschauer, beiDruck = { alsZuschauer = false })
        Pille(aufschrift = "Zuschauer · ohne Platz", an = alsZuschauer, beiDruck = { alsZuschauer = true })
    }
    SehrLeise(
        if (alsZuschauer) {
            "Zuschauer sehen die Runde mit, belegen keinen Platz und funken nicht."
        } else if (frei == 0) {
            "Die Runde ist voll. Wer jetzt kommt, sieht zu und kann um einen Platz bitten."
        } else {
            "Code scannen oder Link öffnen — wer eine Rolle wählt, spielt mit."
        },
    )

    val bild = remember(link) { qrBild(link) }
    if (bild != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                bitmap = bild,
                contentDescription = "QR-Code zum Beitreten",
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .size(200.dp)
                    .background(androidx.compose.ui.graphics.Color.White)
                    .padding(Abstand.Klein),
            )
        }
    }
    Knopf(
        if (kopiert) "Kopiert ✓" else "Beitrittslink kopieren",
        {
            zwischenablage.setText(AnnotatedString(link))
            kopiert = true
        },
        breit = true,
    )

    val bestaetigt = freunde.filter { it.bestaetigt }
    Etikett("Freunde")
    if (bestaetigt.isEmpty()) {
        SehrLeise("Noch keine Freunde — über „+ Freund“ in der Mannschaft lässt sich das ändern.")
    }
    bestaetigt.forEach { f ->
        val dabei = f.anwesenheit?.roomCode == raum.code ||
            raum.players.any { it.id == f.kennung } || raum.zuschauer.any { it.id == f.kennung }
        val unter = when {
            dabei -> null
            f.anwesenheit != null -> "schon in einer Runde"
            else -> "nicht im Dienst"
        }
        Profilzeile(
            kennung = f.kennung,
            anzeigename = f.anzeigename.ifBlank { f.benutzername },
            unterzeile = unter,
            wappen = f.wappen,
            wappenfarbe = f.wappenfarbe,
            kopfmuster = f.kopfmuster,
            profilrahmen = f.profilrahmen,
            bildAdresse = profilbildAdresse(server, f.profilbild),
            premium = f.premium,
            teammitglied = f.teammitglied,
            hinten = {
                when {
                    dabei -> Marke("Ist dabei", farbe = Farben.GruenHell)
                    f.kennung in eingeladen -> Marke("Eingeladen", farbe = Farben.Amber)
                    else -> Knopf(
                        "Einladen",
                        {
                            griffe.einladen(f.kennung, alsZuschauer)
                            eingeladen = eingeladen + f.kennung
                        },
                        kompakt = true,
                    )
                }
            },
        )
    }
    einladeMeldung?.let { SehrLeise(it) }

    Ueberschrift("Mehr")
    Knopf("Melder testen", griffe.melderTesten, breit = true)
    Knopf(
        if (ich?.live == true) "Streamen · live" else "Streamen",
        { liveOffen = true },
        breit = true,
    )
    Knopf("Spielhilfe", griffe.hilfe, art = Knopfart.Leise, breit = true)

    if (liveOffen) {
        LiveBlende(raum = raum, ich = ich, griffe = griffe, beiSchliessen = { liveOffen = false })
    }
}

/**
 * Streamen — `lobby/LiveDialog.vue`. Die Leitstelle stellt den Streamer-Modus
 * der Runde; jeder andere kann sich selbst als „live“ melden, nach drei
 * Bestätigungen.
 */
@Composable
private fun LiveBlende(
    raum: Raumzustand,
    ich: Spieler?,
    griffe: LobbyGriffe,
    beiSchliessen: () -> Unit,
) {
    val s = raum.settings
    val istLeitstelle = ich?.istLeitstelle == true
    var kanal by remember { mutableStateOf(s.streamerkanal.orEmpty()) }
    var haken1 by remember { mutableStateOf(false) }
    var haken2 by remember { mutableStateOf(false) }
    var haken3 by remember { mutableStateOf(false) }
    val ohneFreigabe = raum.menschen.filter { !it.streamerfreigabe }

    Blende(
        titel = "Streamen",
        beiSchliessen = beiSchliessen,
        fuss = {
            Knopf("Schließen", beiSchliessen, art = Knopfart.Leise)
            if (ich?.live == true) {
                Knopf("Stream abmelden", { griffe.live(false) }, art = Knopfart.Gefahr)
            } else if (ich != null) {
                Knopf(
                    "Live gehen",
                    { griffe.live(true) },
                    art = Knopfart.Haupt,
                    aktiv = haken1 && haken2 && haken3,
                )
            }
        },
    ) {
        Etikett("Streamer-Modus der Runde")
        if (istLeitstelle) {
            val kanalSauber = kanal.trim()
            Schalterzeile(
                titel = "Streamer-Modus",
                an = s.streamermodus,
                beiWechsel = { an ->
                    griffe.einstellungen(
                        Einstellungsaenderung(
                            streamermodus = an,
                            streamerkanal = kanalSauber.ifBlank { null },
                        ),
                    )
                },
                unterzeile = "Wer beitritt, wird um Einwilligung gefragt — die Runde wird übertragen.",
                aktiv = s.streamermodus || kanalSauber.isNotBlank(),
            )
            Feld(
                wert = kanal,
                beiAenderung = { kanal = it.take(120) },
                etikett = "Kanal",
                platzhalter = "twitch.tv/…",
            )
            if (kanalSauber != s.streamerkanal.orEmpty()) {
                Knopf(
                    "Kanal übernehmen",
                    { griffe.einstellungen(Einstellungsaenderung(streamerkanal = kanalSauber)) },
                    kompakt = true,
                )
            }
            Pillenreihe {
                STREAMERPLATTFORMEN.forEach { (roh, name) ->
                    Pille(
                        aufschrift = name,
                        an = s.streamerplattform == roh,
                        beiDruck = { griffe.einstellungen(Einstellungsaenderung(streamerplattform = roh)) },
                    )
                }
            }
            Schalterzeile(
                titel = "Wird aufgezeichnet",
                an = s.streameraufzeichnung,
                beiWechsel = { griffe.einstellungen(Einstellungsaenderung(streameraufzeichnung = it)) },
                unterzeile = "Die Übertragung bleibt danach abrufbar.",
            )
            if (s.streamermodus && ohneFreigabe.isNotEmpty()) {
                Text(
                    "Noch ohne Einwilligung: ${ohneFreigabe.joinToString(", ") { it.name }}",
                    style = Schrift.Klein,
                    color = Farben.AmberHell,
                )
            }
        } else {
            SehrLeise(
                if (s.streamermodus) {
                    "Diese Runde wird übertragen — auf ${plattformname(s.streamerplattform)}" +
                        (s.streamerkanal?.let { " ($it)" } ?: "") +
                        if (s.streameraufzeichnung) ", mit Aufzeichnung." else "."
                } else {
                    "Die Leitstelle hat den Streamer-Modus nicht eingeschaltet."
                },
            )
        }

        if (ich != null && !ich.live) {
            Etikett("Selbst live gehen")
            Hakenzeile("Ich übertrage meinen eigenen Bildschirm.", haken1, { haken1 = it })
            Hakenzeile("Mitspieler erscheinen nur mit ihrem Rufnamen, nicht mit Klarnamen.", haken2, { haken2 = it })
            Hakenzeile("Ich halte mich an die Regeln der Plattform.", haken3, { haken3 = it })
        } else if (ich?.live == true) {
            Marke("Du bist als live gemeldet", farbe = Farben.SignalHell)
        }
    }
}

/**
 * Der QR-Code als Bild — `com.google.zxing:core` schreibt eine Bitmatrix, hier
 * wird sie Punkt für Punkt in ein Bitmap übertragen. `null` bei Unsinn.
 */
private fun qrBild(inhalt: String): ImageBitmap? = runCatching {
    val matrix = QRCodeWriter().encode(
        inhalt,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(EncodeHintType.MARGIN to 1),
    )
    val breite = matrix.width
    val hoehe = matrix.height
    val punkte = IntArray(breite * hoehe) { i ->
        if (matrix.get(i % breite, i / breite)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
    Bitmap.createBitmap(punkte, breite, hoehe, Bitmap.Config.ARGB_8888).asImageBitmap()
}.getOrNull()
