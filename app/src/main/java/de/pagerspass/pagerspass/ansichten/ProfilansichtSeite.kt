package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Profilansicht
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Kartenausschnitt
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.karte.Kartenstil
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.bildVon
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/**
 * Das Profil eines Kontos — das eigene wie ein fremdes (`views/freunde/ProfilView.vue`).
 *
 * Was hier fehlt, fehlt absichtlich: Der Server lässt weg, was die Privatsphäre
 * des Gezeigten ausschließt. Die Ansicht blendet nichts aus — sie zeigt, was
 * ankommt.
 *
 * Die Reihenfolge ist die der Fragen, mit denen man ein Profil öffnet: Wer ist
 * das (Kopf), was mache ich damit (Aktionen), wer ist das genauer (Vorstellung,
 * Vitrine), was hat er zuletzt getan (Brett).
 */
@Composable
fun ColumnScope.ProfilansichtInhalt(
    benutzername: String,
    ansicht: Profilansicht,
    server: String,
    meldung: String?,
    /** Der Code der eigenen, noch laufenden Runde — nur dann gibt es „Zu mir in die Runde". */
    eigeneRunde: String?,
    /** Ob man in die eigene Wache einladen darf — Leitung und Zugführer. */
    darfInWacheEinladen: Boolean,
    beiMeldungWeg: () -> Unit,
    beiLaden: () -> Unit,
    beiZurueck: () -> Unit,
    beiGespraech: (String) -> Unit,
    beiAnfragen: (String) -> Unit,
    beiAntworten: (String, Boolean) -> Unit,
    beiDazuschalten: (String) -> Unit,
    beiZuMirEinladen: (String, String) -> Unit,
    beiInWacheEinladen: (String) -> Unit,
    beiBeenden: (String) -> Unit,
    beiBlockieren: (String) -> Unit,
    beiMelden: (String, String) -> Unit,
    beiProfilGestalten: () -> Unit,
    beiWache: () -> Unit,
    beiQuittieren: (Long) -> Unit,
    beiEntfernen: (Long) -> Unit,
    beiEintragMelden: (Long) -> Unit,
    beiKommentare: (Long) -> Unit,
    beiProfil: (String) -> Unit,
    beiBezug: (art: String, id: String) -> Unit,
) {
    // Von einem Profil aufs nächste wechselt nur der Name — ohne diesen Schlüssel
    // bliebe das alte stehen.
    LaunchedEffect(benutzername) { beiLaden() }

    Seitenkopf(
        titel = "Profil",
        knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
    )

    val p = ansicht.profil
    if (p == null) {
        when {
            ansicht.fehler != null -> Fehlerzeile(ansicht.fehler, beiLaden)
            else -> FreundeLaden()
        }
        return
    }

    Profilkopf(p, server, beiProfilGestalten = beiProfilGestalten, beiWache = beiWache)

    if (!p.ich) {
        val befreundet = p.stand == "Bestaetigt"

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (befreundet) {
                Knopf("Nachricht schreiben", { beiGespraech(p.kennung) }, art = Knopfart.Haupt, kompakt = true)
            }
            if (p.stand == null) {
                Knopf("Freundschaft anfragen", { beiAnfragen(p.kennung) }, art = Knopfart.Haupt, kompakt = true)
            }
            if (p.stand == "Angefragt" && !p.vonMir) {
                Knopf("Anfrage annehmen", { beiAntworten(p.kennung, true) }, art = Knopfart.Haupt, kompakt = true)
                Knopf("Ablehnen", { beiAntworten(p.kennung, false) }, art = Knopfart.Leise, kompakt = true)
            } else if (p.stand == "Angefragt") {
                Marke("Anfrage läuft")
            }
            p.anwesenheit?.takeIf { it.platzFrei }?.let { wo ->
                Knopf("Dazuschalten", { beiDazuschalten(wo.roomCode) }, kompakt = true)
            }
            // „Zu mir in die Runde" — die Einladung aus der anderen Richtung,
            // sichtbar nur, solange es wirklich etwas einzuladen gibt.
            if (befreundet && eigeneRunde != null) {
                Knopf(
                    aufschrift = if (ansicht.rundeEingeladen) "Eingeladen" else "Zu mir in die Runde",
                    beiDruck = { beiZuMirEinladen(p.kennung, eigeneRunde) },
                    aktiv = !ansicht.rundeEingeladen,
                    kompakt = true,
                )
            }
            if (darfInWacheEinladen) {
                Knopf(
                    aufschrift = if (ansicht.wacheEingeladen) "Eingeladen" else "In die Wache einladen",
                    beiDruck = { beiInWacheEinladen(p.kennung) },
                    aktiv = !ansicht.wacheEingeladen,
                    kompakt = true,
                )
            }
        }

        Verwalten(
            profil = p,
            befreundet = befreundet,
            gemeldet = ansicht.gemeldet,
            beiBeenden = { beiBeenden(p.kennung) },
            beiBlockieren = { beiBlockieren(p.kennung) },
            beiMelden = { grund -> beiMelden(p.kennung, grund) },
        )
    }

    Meldungsstreifen(meldung, beiMeldungWeg)

    p.vorstellung?.takeIf { it.isNotBlank() }?.let { text ->
        Kasten { Text(text = text, style = Schrift.Lesetext, color = Farben.Text) }
    }

    if (p.vitrine.isNotEmpty()) {
        Abschnitt("Vitrine") {
            p.vitrine.forEach { a ->
                Kasten(abstandInnen = Abstand.Klein) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🏅", style = Schrift.Titel)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = a.titel, style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text)
                            SehrLeise(a.beschreibung)
                        }
                    }
                }
            }
        }
    }

    Abschnitt("Am Brett") {
        if (ansicht.eintraege.isEmpty()) {
            Leerhinweis(if (p.ich) "Du hast noch nichts angeschlagen." else "Hier steht noch nichts für dich.")
        }
        ansicht.eintraege.forEach { e ->
            EintragKarte(
                eintrag = e,
                server = server,
                beiQuittieren = { beiQuittieren(e.nr) },
                beiKommentare = { beiKommentare(e.nr) },
                beiEntfernen = { beiEntfernen(e.nr) },
                beiMelden = { beiEintragMelden(e.nr) },
                beiProfil = beiProfil,
                beiBezug = beiBezug,
            )
        }
    }
}

// ------------------------------------------------------------------- Kopf

/**
 * Der Kopf: Band in der Farbe des Wappens, Name mit Stern und Haken, der Titel
 * aus dem Shop, Rang, Wache und Dienst als Marken, die Lage — und beim Abo-Muster
 * „Eigene Lage" echte Kartenkacheln dahinter.
 */
@Composable
private fun Profilkopf(
    p: Profil,
    server: String,
    beiProfilGestalten: () -> Unit,
    beiWache: () -> Unit,
) {
    val ton = Wappen.ton(p.kennung, p.wappenfarbe)
    val ausschnitt = p.kartenausschnitt
    val zeigtKarte = p.kopfmuster == KARTENMUSTER && ausschnitt != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Rundung.Normal)
            .background(Farben.Flaeche)
            .then(if (zeigtKarte) Modifier else Modifier.kopfband(p.kopfmuster, ton))
            .border(1.dp, Farben.Rand, Rundung.Normal),
    ) {
        if (zeigtKarte && ausschnitt != null) {
            Kartenband(ausschnitt, Modifier.matchParentSize())
            // Der Verlauf bleibt darüber liegen — er hält den Namen auf einer Karte lesbar.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Farben.Flaeche.copy(alpha = 0.92f), Farben.Flaeche.copy(alpha = 0.35f)),
                        ),
                    ),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier.fillMaxWidth().padding(Abstand.Gross),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Gross),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Kontobild(
                    kennung = p.kennung,
                    anzeigename = p.anzeigename,
                    wappen = p.wappen,
                    wappenfarbe = p.wappenfarbe,
                    bildAdresse = bildweg(server, p.profilbild),
                    rahmen = p.profilrahmen,
                    groesse = 84.dp,
                    imDienst = p.anwesenheit != null,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier.weight(1f),
                ) {
                    Personenname(
                        name = p.anzeigename,
                        premium = p.premium,
                        teammitglied = p.teammitglied,
                        wachentag = p.wachentag,
                        stil = Schrift.Titel,
                        maxZeilen = 2,
                    )
                    titelName(p.titel)?.let {
                        Text(text = it, style = Schrift.Klein, color = Farben.AmberHell)
                    }
                    SehrLeise(p.benutzername, mono = true)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                p.rang?.takeIf { it.isNotBlank() }?.let { Marke("$it · Stufe ${p.level ?: 0}") }
                p.gemeinschaft?.takeIf { it.isNotBlank() }?.let { name ->
                    Marke(
                        "🏠 $name",
                        farbe = Farben.OrangeHell,
                        modifier = Modifier.clickable(
                            onClick = beiWache,
                            role = Role.Button,
                            indication = null,
                            interactionSource = null,
                        ),
                    )
                }
                if (p.anwesenheit != null) Marke("Im Dienst", farbe = Farben.GruenHell)
            }

            Text(
                text = lagezeile(p.anwesenheit, p.zuletztGesehen)
                    .ifBlank { "Dabei seit ${langdatum(p.dabeiSeit)}" },
                style = Schrift.Klein,
                color = if (p.anwesenheit != null) Farben.GruenHell else Farben.TextLeise,
            )

            if (p.schichten != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.SehrGross)) {
                    Profilwert("Schichten", p.schichten.toString())
                    Profilwert("Einsätze", (p.einsaetze ?: 0).toString())
                    Profilwert("Dabei seit", langdatum(p.dabeiSeit))
                }
            }

            // Bearbeitet wird an genau einer Stelle: im Konto.
            if (p.ich) {
                Knopf("Profil gestalten", beiProfilGestalten, art = Knopfart.Haupt, kompakt = true)
            }
        }
    }
}

@Composable
private fun Profilwert(was: String, wert: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        Etikett(was)
        Text(text = wert, style = Schrift.MonoNormal, color = Farben.Text)
    }
}

/**
 * Beenden, Blockieren und Melden hinter einer Klappe — nicht, weil sie
 * unwichtig wären, sondern weil sie in einer Reihe mit „Nachricht schreiben"
 * genauso groß aussähen wie das, weswegen man ein Profil öffnet.
 */
@Composable
private fun Verwalten(
    profil: Profil,
    befreundet: Boolean,
    gemeldet: Boolean,
    beiBeenden: () -> Unit,
    beiBlockieren: () -> Unit,
    beiMelden: (String) -> Unit,
) {
    var offen by rememberSaveable(profil.kennung) { mutableStateOf(false) }
    var beendenGefragt by remember(profil.kennung) { mutableStateOf(false) }
    var blockierenGefragt by remember(profil.kennung) { mutableStateOf(false) }
    var meldenOffen by remember(profil.kennung) { mutableStateOf(false) }
    var grund by rememberSaveable(profil.kennung) { mutableStateOf("") }

    Text(
        text = (if (offen) "▾ " else "▸ ") + "Diesen Kontakt verwalten",
        style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold),
        color = Farben.TextLeise,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = { offen = !offen },
                role = Role.Button,
                indication = null,
                interactionSource = null,
            )
            .padding(vertical = Abstand.Klein),
    )
    if (!offen) return

    Kasten(abstandInnen = Abstand.Klein) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        ) {
            if (befreundet) {
                Knopf(
                    aufschrift = if (beendenGefragt) "Wirklich beenden?" else "Freundschaft beenden",
                    beiDruck = {
                        if (!beendenGefragt) {
                            beendenGefragt = true
                        } else {
                            beendenGefragt = false
                            beiBeenden()
                        }
                    },
                    art = if (beendenGefragt) Knopfart.Gefahr else Knopfart.Leise,
                    kompakt = true,
                )
            }
            Knopf(
                aufschrift = if (blockierenGefragt) "Wirklich blockieren?" else "Blockieren",
                beiDruck = {
                    if (!blockierenGefragt) {
                        blockierenGefragt = true
                    } else {
                        blockierenGefragt = false
                        beiBlockieren()
                    }
                },
                art = if (blockierenGefragt) Knopfart.Gefahr else Knopfart.Leise,
                kompakt = true,
            )
            Knopf("Melden", { meldenOffen = !meldenOffen }, art = Knopfart.Leise, kompakt = true)
        }

        SehrLeise(
            "Blockieren beendet Freundschaft, Einladungen und Nachrichten in beide Richtungen. " +
                "Ihr findet und kontaktiert euch danach nicht mehr; aufheben kannst du die Blockade " +
                "später in deiner Kontaktliste. Melden ist davon unabhängig und sendet den angegebenen " +
                "Grund an die Moderation.",
        )

        if (meldenOffen) {
            Feld(
                wert = grund,
                beiAenderung = { grund = it.take(500) },
                etikett = "Worum geht es?",
                platzhalter = "Kurz beschreiben, was passiert ist …",
                einzeilig = false,
            )
            Knopf(
                aufschrift = "Meldung absenden",
                beiDruck = {
                    if (grund.isNotBlank()) {
                        beiMelden(grund.trim())
                        grund = ""
                        meldenOffen = false
                    }
                },
                art = Knopfart.Haupt,
                aktiv = grund.isNotBlank(),
                kompakt = true,
            )
        }

        if (gemeldet) SehrLeise("Meldung gesendet — danke für den Hinweis.")
    }
}

// ------------------------------------------------------------ Kartenband

/** Das Abo-Muster „Eigene Lage" — Spiegel von `KARTENMUSTER` im Web. */
private const val KARTENMUSTER = "premium-karte"

/**
 * Der Kartenausschnitt hinter dem Profilkopf — ein Bild, keine Karte.
 *
 * Nichts davon soll ein Profilkopf können, was eine Karte kann: ziehen, zoomen,
 * Marker. Er zeigt die Kacheln um den Mittelpunkt, sonst nichts.
 *
 * <b>Die Einwilligung gilt hier genauso.</b> Ohne Freigabe wird keine Kachel
 * geholt — dann steht der Kopf mit seinem Farbverlauf da wie bei jedem anderen
 * Konto. Eine Frage über ein fremdes Profil zu legen lohnt nicht; beantwortet
 * wird sie an einer Karte.
 */
@Composable
private fun Kartenband(ausschnitt: Kartenausschnitt, modifier: Modifier) {
    val zusammenhang = LocalContext.current
    val ablage = remember(zusammenhang) { Ablage(zusammenhang) }
    var frei by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { frei = runCatching { ablage.karteFreigabe() == "ja" }.getOrDefault(false) }
    if (!frei) return

    val stil = Kartenstil.Dunkel
    val z = ausschnitt.zoom.coerceIn(4, 14)
    val welt = 256.0 * 2.0.pow(z)
    val breiteRad = ausschnitt.lat.coerceIn(-85.0, 85.0) * PI / 180.0
    val mitteX = (ausschnitt.lon + 180.0) / 360.0 * welt
    val mitteY = (1.0 - ln(tan(breiteRad) + 1.0 / cos(breiteRad)) / PI) / 2.0 * welt
    val kacheln = 1 shl z

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val breite = maxWidth.value.toDouble()
        val hoehe = maxHeight.value.toDouble()
        val links = mitteX - breite / 2.0
        val oben = mitteY - hoehe / 2.0

        val x0 = floor(links / 256.0).toInt()
        val x1 = floor((links + breite) / 256.0).toInt()
        val y0 = floor(oben / 256.0).toInt()
        val y1 = floor((oben + hoehe) / 256.0).toInt()

        for (y in y0..y1) {
            if (y < 0 || y >= kacheln) continue
            for (x in x0..x1) {
                val xNorm = ((x % kacheln) + kacheln) % kacheln
                val adresse = stil.url(stil.quelle, z, xNorm, y)
                key(adresse, x) {
                    val bild by bildVon(adresse)
                    bild?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .offset((x * 256.0 - links).toFloat().dp, (y * 256.0 - oben).toFloat().dp)
                                .size(256.dp),
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Titel

/** Der Anzeigename eines Titels aus dem Shop oder der Vergabe; leer heißt keiner. */
private fun titelName(id: String?): String? = id?.takeIf { it.isNotBlank() }?.let { TITELNAMEN[it] }

private val TITELNAMEN = mapOf(
    "ehrenamt" to "Ehrenamt",
    "nachtschicht" to "Nachtschicht",
    "funkfuchs" to "Funkfuchs",
    "blaulichtfan" to "Blaulichtfan",
    "kaffeekasse" to "Kaffeekasse",
    "urgestein" to "Urgestein",
    "leitstellenlegende" to "Leitstellenlegende",
    "fruehschicht" to "Frühschicht",
    "helferherz" to "Helferherz",
    "lageprofi" to "Lageprofi",
    "team" to "Team",
    "helfer" to "Helfer",
    "betatester" to "Betatester",
    "jugendverband" to "Jugendverband",
    "premium" to "Premium",
    "premium-einsatzbereit" to "Premium · Einsatzbereit",
    "premium-leitstelle" to "Premium · Leitstelle",
    "premium-nachtdienst" to "Premium · Nachtdienst",
    "premium-stammbesatzung" to "Premium · Stammbesatzung",
    "premium-goenner" to "Premium · Gönner",
)
