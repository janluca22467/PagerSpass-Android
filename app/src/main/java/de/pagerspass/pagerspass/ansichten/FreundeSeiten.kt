package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Kreisstand
import de.pagerspass.pagerspass.netz.Anwesenheit
import de.pagerspass.pagerspass.netz.Einladung
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Markenzahl
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.schmuck.Profilzeile
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Der Freundebereich — übertragen aus `views/freunde/` und
 * `components/freunde/FreundeSeite.vue` in die Handyform.
 *
 * <b>Vier Wege statt einer langen Seite</b>, wie im Web: Brett, Freunde,
 * Nachrichten, Kontakte. Das sind genau die vier Gründe, aus denen man
 * hierherkommt — lesen, nachsehen wer fährt, schreiben, jemanden finden. Vorher
 * lag die Verwaltung (Anfragen) auf demselben Reiter wie die Liste, und wer
 * schreiben wollte, musste an ihr vorbei.
 *
 * <b>Darüber der Ausweis</b>: das eigene Wappen mit Namen und drei Zahlen, und er
 * führt aufs eigene Profil — die Stelle, an der man sieht, wie einen die anderen
 * sehen.
 */
class FreundeGriffe(
    val laden: () -> Unit = {},
    /** Ein Profil öffnen — über den eindeutigen Benutzernamen. */
    val profil: (String) -> Unit = {},
    val gespraech: (Freund) -> Unit = {},
    /** Zu jemandem in die Runde — über ihren Raumcode. */
    val dazuschalten: (String) -> Unit = {},
    val antworten: (String, Boolean) -> Unit = { _, _ -> },
    val anfragen: (String) -> Unit = {},
    val loesen: (String) -> Unit = {},
    val suchen: (String) -> Unit = {},
    val vorschlaegeLaden: () -> Unit = {},
    val weglegen: (String) -> Unit = {},
    val zurueckholen: () -> Unit = {},
    val einladungAnnehmen: (Einladung) -> Unit = {},
    val einladungAblehnen: (Einladung) -> Unit = {},
    val meldungWeg: () -> Unit = {},
    /** Zum Dienst — der Ausweg aus leeren Vorschlägen („Schicht fahren"). */
    val dienst: () -> Unit = {},
)

@Composable
fun FreundeSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    /** Das eigene Profil — für Wappen, Farbe und Muster des Ausweises. */
    meinProfil: Profil? = null,
    freunde: Bereichsstand<List<Freund>> = Bereichsstand(),
    einladungen: List<Einladung> = emptyList(),
    kreis: Kreisstand = Kreisstand(),
    server: String = "",
    griffe: FreundeGriffe = FreundeGriffe(),
    brett: @Composable ColumnScope.() -> Unit = {},
) {
    var reiter by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(Unit) { griffe.laden() }

    val alle = freunde.inhalt.orEmpty()
    val bestaetigte = alle.filter { it.bestaetigt }.gespraechsordnung()
    val offeneAnfragen = alle.filter { it.angefragt && !it.vonMir }
    val imDienst = bestaetigte.count { it.anwesenheit != null }
    val ungelesen = bestaetigte.sumOf { it.ungelesen }

    Seite(modifier = modifier, unterrand = unterrand) {
        if (konto != null) {
            Ausweis(
                konto = konto,
                profil = meinProfil,
                server = server,
                freunde = bestaetigte.size,
                imDienst = imDienst,
                ungelesen = ungelesen,
                beiDruck = { griffe.profil(konto.benutzername) },
            )
        }

        // Mit den Zeichen des Webs, am Handy über dem Wort (`FreundeSeite.vue`):
        // „Nachrichten" passt neben Zeichen und Marke nicht in eine Viertelbreite.
        Reiterreihe {
            Reiter(
                "Brett",
                offen = reiter == 0,
                beiDruck = { reiter = 0 },
                zeichen = pfadzeichen(REITER_BRETT, gefuellt = false),
                senkrecht = true,
            )
            // Ohne Marke: „Du hast 14 Freunde" wartet nicht auf dich — die Zahl
            // steht im Ausweis. Eine Marke am Reiter heißt hier „hier wartet etwas".
            Reiter(
                "Freunde",
                offen = reiter == 1,
                beiDruck = { reiter = 1 },
                zeichen = pfadzeichen(REITER_FREUNDE, gefuellt = false),
                senkrecht = true,
            )
            Reiter(
                "Nachrichten",
                offen = reiter == 2,
                beiDruck = { reiter = 2 },
                marke = ungelesen,
                zeichen = pfadzeichen(REITER_NACHRICHTEN, gefuellt = false),
                senkrecht = true,
            )
            Reiter(
                "Kontakte",
                offen = reiter == 3,
                beiDruck = { reiter = 3 },
                marke = offeneAnfragen.size + einladungen.size,
                zeichen = pfadzeichen(REITER_KONTAKTE, gefuellt = false),
                senkrecht = true,
            )
        }

        Meldungszeile(kreis.meldung, kreis.hinweis, griffe.meldungWeg)

        when (reiter) {
            0 -> {
                // Wer gerade fährt, steht über dem Brett — das Einzige hier, das in
                // Minuten veraltet. Fährt niemand, fehlt die Karte ganz.
                ImDienstLeiste(
                    freunde = bestaetigte.filter { it.anwesenheit != null },
                    server = server,
                    beiProfil = griffe.profil,
                    beiDazuschalten = griffe.dazuschalten,
                    beiAlle = { reiter = 1 },
                )
                brett()
            }
            else -> Bereich(
                laedt = freunde.laedt,
                fehler = freunde.fehler,
                inhalt = freunde.inhalt,
                beiErneut = griffe.laden,
            ) {
                when (reiter) {
                    1 -> FreundeListe(bestaetigte, server, griffe, beiKontakte = { reiter = 3 })
                    2 -> Gespraechsliste(bestaetigte, server, griffe, beiKontakte = { reiter = 3 })
                    else -> Kontakte(alle, einladungen, kreis, server, griffe)
                }
            }
        }
    }
}

/**
 * Der Ausweis über den Reitern — Wappen, Name, drei Zahlen.
 *
 * Wappen, Farbe und Muster kommen aus dem eigenen Profil, nicht aus dem Konto:
 * Das Konto kennt sie nicht, und aus der Kennung gewürfelt trüge derselbe Mensch
 * hier eine andere Farbe als am Brett.
 */
@Composable
private fun Ausweis(
    konto: Konto,
    profil: Profil?,
    server: String,
    freunde: Int,
    imDienst: Int,
    ungelesen: Int,
    beiDruck: () -> Unit,
) {
    val ton = Wappen.ton(konto.kennung, profil?.wappenfarbe ?: 0)

    // Am Handy sagt der Ausweis, wo man ist, nicht wer man ist (`mobil.css`,
    // `.freunde-kopf .ich`): Wappen klein, „Freunde" als Titel, der Weg ins
    // eigene Profil — Name und Kennung stehen einen Tipp weiter im Profil. Die
    // drei Zahlen sind eine leise Zeile statt dreier Kästchen. So endet der
    // Kopf bei 177 statt 260 Punkten, und das Brett beginnt früher.
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .flaeche(randfarbe = Farben.Amber.copy(alpha = 0.28f).compositeOver(Farben.Rand))
            .kopfband(profil?.kopfmuster ?: "keines", ton, zeile = true)
            .clickable(onClick = beiDruck, role = Role.Button)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Kontobild(
                kennung = konto.kennung,
                anzeigename = konto.anzeigename,
                wappen = profil?.wappen ?: "Keines",
                wappenfarbe = profil?.wappenfarbe ?: 0,
                bildAdresse = bildweg(server, profil?.profilbild),
                rahmen = profil?.profilrahmen ?: "keiner",
                groesse = 34.dp,
            )
            Text(
                "Freunde",
                style = Schrift.Gross.copy(fontWeight = FontWeight.Bold),
                color = Farben.Text,
                modifier = Modifier.weight(1f),
            )
            Text("Mein Profil ›", style = Schrift.Klein, color = Farben.AmberHell)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            Ausweiswert(freunde, "Freunde")
            // Grün wie der Ring am Wappen: „sitzt in einer Runde". Rot nur, was auf
            // Antwort wartet. Eine Null bleibt weiß — sie meldet nichts.
            Ausweiswert(imDienst, "im Dienst", farbe = if (imDienst > 0) Farben.GruenHell else Farben.Text)
            Ausweiswert(ungelesen, "ungelesen", farbe = if (ungelesen > 0) Farben.SignalHell else Farben.Text)
        }
    }
}

@Composable
private fun Ausweiswert(zahl: Int, wort: String, farbe: androidx.compose.ui.graphics.Color = Farben.Text) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = zahl.toString(),
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.ExtraBold),
            color = farbe,
        )
        Text(
            text = wort.uppercase(),
            style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
            color = Farben.TextLeise,
        )
    }
}

// ------------------------------------------------------------------ Freunde

/**
 * Deine Freunde — wer sie sind und wer gerade fährt.
 *
 * <b>Geordnet nach Lage, nicht nach Nachricht</b>: oben, wer im Dienst ist — das
 * Einzige an dieser Liste, was sich in Minuten ändert und wonach man handelt.
 * Die Zeile führt ins Profil; „Schreiben" daneben ins Gespräch.
 */
@Composable
private fun ColumnScope.FreundeListe(
    bestaetigte: List<Freund>,
    server: String,
    griffe: FreundeGriffe,
    beiKontakte: () -> Unit = {},
) {
    var suche by rememberSaveable { mutableStateOf("") }

    if (bestaetigte.size > 4) {
        Feld(wert = suche, beiAenderung = { suche = it.take(48) }, platzhalter = "Name suchen …")
    }

    val gezeigt = bestaetigte.filter { it.passt(suche) }.sortedBy { it.anzeigename.lowercase() }
    val dienst = gezeigt.filter { it.anwesenheit != null }
    val ruhend = gezeigt.filter { it.anwesenheit == null }

    // Wer noch niemanden hat, bekommt den Weg zu den Leuten, mit denen er gefahren ist.
    if (bestaetigte.isEmpty()) {
        Buchkarte("Deine Freunde", zahl = "0") {
            Leerhinweis("Noch niemand. Unter „Kontakte“ findest du Leute, mit denen du schon gefahren bist.") {
                Knopf("Zu den Kontakten", beiKontakte, art = Knopfart.Haupt)
            }
        }
        return
    }
    if (suche.isNotBlank() && gezeigt.isEmpty()) {
        Leerhinweis("Niemand mit diesem Namen.") {
            Knopf("Suche leeren", { suche = "" })
        }
    }

    // „Im Dienst" steht auch leer da (außer beim Suchen) — mit dem Satz, dass
    // gerade niemand fährt, statt einer Lücke.
    listOf(
        Triple("Im Dienst", dienst, dienst.isNotEmpty() || suche.isBlank()),
        Triple("Gerade nicht im Dienst", ruhend, ruhend.isNotEmpty()),
    ).forEach { (titel, leute, zeigen) ->
        if (!zeigen) return@forEach
        Buchkarte(titel, zahl = leute.size.toString(), dicht = leute.isNotEmpty(), abstandInnen = 0.dp) {
        if (leute.isEmpty()) {
            Leerhinweis(
                "Gerade fährt niemand aus deiner Liste. Wer in den Dienst geht, rückt hier von allein nach oben.",
            )
        }
        leute.forEachIndexed { i, f ->
            Freundzeile(
                freund = f,
                server = server,
                unterzeile = lage(f.anwesenheit, f.zuletztGesehen).ifBlank { f.benutzername },
                beiDruck = { griffe.profil(f.benutzername) },
                letzte = i == leute.lastIndex,
                unten = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        f.anwesenheit?.let { wo ->
                            Knopf(
                                if (wo.platzFrei) "Dazuschalten" else "Voll",
                                { griffe.dazuschalten(wo.roomCode) },
                                aktiv = wo.platzFrei,
                                kompakt = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Knopf(
                            if (f.ungelesen > 0) "Schreiben · ${f.ungelesen}" else "Schreiben",
                            { griffe.gespraech(f) },
                            art = Knopfart.Leise,
                            kompakt = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                },
            )
        }
        }
    }
}

// -------------------------------------------------------------- Nachrichten

/**
 * Die Gesprächsliste — je Freund eine Zeile mit der jüngsten Nachricht.
 *
 * Die ganze Zeile ist der Weg ins Gespräch: Am Daumen ist die Zeile das Ziel.
 * Suche und Sieb erst ab einer Liste, die man nicht mehr überblickt — bei drei
 * Freunden wären sie zwei Zeilen Verwaltung über drei Zeilen Inhalt.
 */
@Composable
private fun ColumnScope.Gespraechsliste(
    bestaetigte: List<Freund>,
    server: String,
    griffe: FreundeGriffe,
    beiKontakte: () -> Unit = {},
) {
    var suche by rememberSaveable { mutableStateOf("") }
    var sieb by rememberSaveable { mutableStateOf("Alle") }
    val ungelesen = bestaetigte.sumOf { it.ungelesen }
    val imDienst = bestaetigte.count { it.anwesenheit != null }

    if (bestaetigte.size > 4) {
        Feld(wert = suche, beiAenderung = { suche = it.take(48) }, platzhalter = "Name suchen …")
        // Der Wahlschalter des Web — eine Wahl aus dreien, die Zahl steht in der Aufschrift.
        Segment(
            seiten = listOf("Alle", "Ungelesen", "ImDienst"),
            gewaehlt = sieb,
            beiWahl = { sieb = it },
            aufschrift = {
                when (it) {
                    "Ungelesen" -> if (ungelesen > 0) "Ungelesen $ungelesen" else "Ungelesen"
                    "ImDienst" -> if (imDienst > 0) "Im Dienst $imDienst" else "Im Dienst"
                    else -> "Alle"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val gezeigt = bestaetigte.filter { f ->
        when {
            sieb == "Ungelesen" && f.ungelesen == 0 -> false
            sieb == "ImDienst" && f.anwesenheit == null -> false
            else -> f.passt(suche)
        }
    }

    val leer = when {
        bestaetigte.isEmpty() ->
            "Noch niemand. Unter „Kontakte“ findest du Leute, mit denen du schon gefahren bist."
        suche.isNotBlank() && gezeigt.isEmpty() -> "Niemand mit diesem Namen."
        sieb == "Ungelesen" && gezeigt.isEmpty() -> "Nichts Ungelesenes — alles beantwortet."
        sieb == "ImDienst" && gezeigt.isEmpty() -> "Gerade fährt niemand von deinen Freunden."
        else -> null
    }
    Buchkarte("Gespräche", zahl = gezeigt.size.toString(), dicht = leer == null, abstandInnen = 0.dp) {
    // Der leere Zustand steht in der Karte und trägt den Weg hinaus: zu den
    // Kontakten, wenn es niemanden gibt, sonst zurück zur ganzen Liste.
    leer?.let {
        Leerhinweis(it) {
            if (bestaetigte.isEmpty()) {
                Knopf("Zu den Kontakten", beiKontakte, art = Knopfart.Haupt)
            } else {
                Knopf("Ganze Liste zeigen", {
                    suche = ""
                    sieb = "Alle"
                })
            }
        }
    }

    gezeigt.forEachIndexed { i, f ->
        val vorschau = when {
            f.anwesenheit != null -> lage(f.anwesenheit, f.zuletztGesehen)
            f.letzteNachricht != null ->
                (if (f.letzteNachrichtVonMir) "Du: " else "") + f.letzteNachricht
            else -> listOf(f.benutzername, lage(null, f.zuletztGesehen))
                .filter { it.isNotBlank() }
                .joinToString(" · ")
        }
        Freundzeile(
            freund = f,
            server = server,
            unterzeile = vorschau,
            // Die Rahmen bleiben in dieser Liste weg: Der Ringplatz gehört dem
            // grünen Dienstring, der Frage, wegen der man sie öffnet.
            mitSchmuck = false,
            beiDruck = { griffe.gespraech(f) },
            hinten = {
                Column(horizontalAlignment = Alignment.End) {
                    f.letzteNachrichtUm?.let { SehrLeise(kurzzeit(it), mono = true) }
                    if (f.ungelesen > 0) Markenzahl(f.ungelesen)
                }
            },
            unten = dazuschaltknopf(f.anwesenheit, griffe),
            letzte = i == gezeigt.lastIndex,
        )
    }
    }
}

// ----------------------------------------------------------------- Kontakte

/**
 * Kontakte: alles, was eine Antwort verlangt oder eine Verbindung herstellt.
 *
 * <b>Die Reihenfolge ist die der Dringlichkeit</b>: Was auf mich wartet, steht
 * oben (Einladungen, Anfragen); was ich suchen will, darunter; was ich selbst
 * angefragt oder blockiert habe, ganz unten hinter einer Klappe.
 */
@Composable
private fun ColumnScope.Kontakte(
    alle: List<Freund>,
    einladungen: List<Einladung>,
    kreis: Kreisstand,
    server: String,
    griffe: FreundeGriffe,
) {
    LaunchedEffect(Unit) { griffe.vorschlaegeLaden() }
    var suchbegriff by rememberSaveable { mutableStateOf("") }
    var klappeOffen by rememberSaveable { mutableStateOf(false) }

    val offeneAnfragen = alle.filter { it.angefragt && !it.vonMir }
    val gestellte = alle.filter { it.angefragt && it.vonMir }
    val blockierte = alle.filter { it.stand == "Blockiert" }
    val vorschlaege = kreis.vorschlaege.inhalt.orEmpty()
    val offeneVorschlaege = vorschlaege.filter { it.kennung !in kreis.weggelegt }

    // Die Reihenfolge des Web am Handy: was auf mich wartet (Einladungen,
    // Anfragen), was sich von selbst ergibt (Vorschläge), dann das Werkzeug
    // (Suche) und das Abgeräumte hinter einer Klappe. Jede Gruppe ist eine Karte
    // mit Kopf und Zähler — dieselben Bausteine wie im Dienstbuch.
    if (einladungen.isNotEmpty()) {
        Buchkarte(
            "Einladungen in eine Runde",
            zahl = einladungen.size.toString(),
            dicht = true,
            abstandInnen = 0.dp,
        ) {
            einladungen.forEachIndexed { i, e ->
                Kartenzeile(letzte = i == einladungen.lastIndex, hinterlegt = Farben.HauchAmber) {
                    Text(
                        text = "${e.vonName} lädt dich " +
                            (if (e.alsZuschauer) "zum Zuschauen in eine Runde" else "in eine Runde"),
                        style = Schrift.Normal,
                        color = Farben.Text,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    ) {
                        Marke(e.roomCode)
                        (e.landkreis ?: e.ort.ifBlank { null })?.let { SehrLeise(it) }
                        if (e.zustand != null) SehrLeise("${e.spieler}/${e.maxSpieler} Spieler", mono = true)
                    }
                    if (e.mitspieler.isNotEmpty()) SehrLeise("Dabei: ${e.mitspieler.joinToString(", ")}")
                    e.hinweis?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            if (e.alsZuschauer) "Zuschauen" else "Annehmen",
                            { griffe.einladungAnnehmen(e) },
                            art = Knopfart.Haupt,
                            aktiv = e.annehmbar,
                            kompakt = true,
                        )
                        Knopf(
                            if (e.annehmbar) "Ablehnen" else "Wegräumen",
                            { griffe.einladungAblehnen(e) },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                }
            }
        }
    }

    if (offeneAnfragen.isNotEmpty()) {
        Buchkarte(
            "Warten auf deine Antwort",
            zahl = offeneAnfragen.size.toString(),
            dicht = true,
            abstandInnen = 0.dp,
        ) {
            offeneAnfragen.forEachIndexed { i, f ->
                Freundzeile(
                    freund = f,
                    server = server,
                    unterzeile = listOf(f.benutzername, f.rang).filter { it.isNotBlank() }.joinToString(" · "),
                    beiDruck = { griffe.profil(f.benutzername) },
                    letzte = i == offeneAnfragen.lastIndex,
                    unten = {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf(
                                "Annehmen",
                                { griffe.antworten(f.kennung, true) },
                                art = Knopfart.Haupt,
                                kompakt = true,
                            )
                            Knopf(
                                "Ablehnen",
                                { griffe.antworten(f.kennung, false) },
                                art = Knopfart.Leise,
                                kompakt = true,
                            )
                        }
                    },
                )
            }
        }
    }

    // Vorschläge — die Karte steht immer da: Wer frisch anfängt, sähe sonst nur ein
    // Suchfeld und wüsste nicht, dass sich diese Seite von selbst füllt.
    Buchkarte(
        "Leute, mit denen du gefahren bist",
        zahl = offeneVorschlaege.size.takeIf { it > 0 }?.toString(),
        dicht = offeneVorschlaege.isNotEmpty(),
        abstandInnen = 0.dp,
        // Ein Satzknopf im Kopf statt eines Absatzes unter der Liste: Er gehört zur
        // ganzen Karte, nicht zur letzten Zeile.
        kopfweg = if (kreis.weggelegt.isNotEmpty() && offeneVorschlaege.isNotEmpty()) {
            { Textweg("${kreis.weggelegt.size} weggelegt · zeigen", griffe.zurueckholen) }
        } else {
            null
        },
    ) {
        when {
            offeneVorschlaege.isNotEmpty() -> offeneVorschlaege.forEachIndexed { i, v ->
                Profilzeile(
                    kennung = v.kennung,
                    anzeigename = v.anzeigename,
                    // Der Grund, warum dieser Mensch hier steht. Ohne ihn ist ein
                    // Vorschlag eine Behauptung; mit ihm eine Erinnerung.
                    unterzeile = "${v.rang} · ${v.gemeinsameSchichten} " +
                        (if (v.gemeinsameSchichten == 1) "gemeinsame Schicht" else "gemeinsame Schichten") +
                        ", zuletzt ${tag(v.zuletztZusammen)}",
                    bildAdresse = bildweg(server, v.profilbild),
                    premium = v.premium,
                    teammitglied = v.teammitglied,
                    beiDruck = { griffe.profil(v.benutzername) },
                    randlos = true,
                    modifier = Modifier.zeilenstrich(i == offeneVorschlaege.lastIndex),
                    unten = {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf(
                                "Anfragen",
                                { griffe.anfragen(v.kennung) },
                                art = Knopfart.Haupt,
                                aktiv = !kreis.laeuft,
                                kompakt = true,
                            )
                            Knopf("Weglegen", { griffe.weglegen(v.kennung) }, art = Knopfart.Leise, kompakt = true)
                        }
                    },
                )
            }
            kreis.weggelegt.isNotEmpty() && vorschlaege.isNotEmpty() -> Leerhinweis("Alle Vorschläge weggelegt.") {
                Knopf("Wieder zeigen", griffe.zurueckholen)
            }
            else -> Leerhinweis(
                "Nach einer gemeinsamen Schicht stehen hier Leute, mit denen du gefahren bist. " +
                    "Einladungen in eine Runde und offene Anfragen landen ebenfalls auf dieser Seite.",
            ) {
                Knopf("Schicht fahren", griffe.dienst)
            }
        }
    }

    // Suche — über den eindeutigen Benutzernamen, nicht über den Anzeigenamen.
    Buchkarte("Jemanden hinzufügen", geraeumig = true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
        ) {
            Feld(
                wert = suchbegriff,
                beiAenderung = { suchbegriff = it.take(20).trim() },
                etikett = "Benutzername",
                platzhalter = "z. B. kim-42",
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "Suchen",
                { griffe.suchen(suchbegriff) },
                art = Knopfart.Haupt,
                aktiv = !kreis.sucht && suchbegriff.isNotBlank(),
                kompakt = true,
            )
        }
        SehrLeise(
            "Gesucht wird über den eindeutigen Benutzernamen, nicht über den Anzeigenamen — " +
                "deinen findest du in deinem Profil.",
        )

        kreis.treffer?.let { t ->
            Profilzeile(
                kennung = t.kennung,
                anzeigename = t.anzeigename,
                unterzeile = listOfNotNull(t.benutzername, t.rang).joinToString(" · "),
                bildAdresse = bildweg(server, t.profilbild),
                premium = t.premium,
                teammitglied = t.teammitglied,
                beiDruck = { griffe.profil(t.benutzername) },
                hinten = {
                    when (t.stand) {
                        null -> Knopf(
                            "Anfragen",
                            { griffe.anfragen(t.kennung) },
                            art = Knopfart.Haupt,
                            aktiv = !kreis.laeuft,
                            kompakt = true,
                        )
                        "Bestaetigt" -> Marke("Schon befreundet")
                        "Angefragt" -> Marke("Anfrage läuft")
                        else -> Marke("Nicht möglich")
                    }
                },
            )
        }
    }

    // Was sonst noch offen oder abgeräumt ist — eine Karte, die aufklappt.
    if (gestellte.isNotEmpty() || blockierte.isNotEmpty()) {
        Buchkarte(
            "Eigene Anfragen und Blockiertes",
            zahl = (gestellte.size + blockierte.size).toString(),
            dicht = true,
            abstandInnen = 0.dp,
            modifier = Modifier.clickable(role = Role.Button) { klappeOffen = !klappeOffen },
            kopfweg = { Text(if (klappeOffen) "⌄" else "›", style = Schrift.Gross, color = Farben.TextLeise) },
        ) {
            if (klappeOffen) {
                val zahl = gestellte.size + blockierte.size
                gestellte.forEachIndexed { i, f ->
                    Freundzeile(
                        freund = f,
                        server = server,
                        unterzeile = "Deine Anfrage läuft.",
                        letzte = i == zahl - 1,
                        hinten = {
                            Knopf("Zurückziehen", { griffe.loesen(f.kennung) }, art = Knopfart.Leise, kompakt = true)
                        },
                    )
                }
                blockierte.forEachIndexed { i, f ->
                    // Ohne Bild, Band und Wappen, mit Absicht: Wer jemanden
                    // blockiert hat, soll dessen Gesicht nicht wiedersehen, nur weil
                    // er nachschlägt, wen er blockiert hat. Der Name genügt.
                    Profilzeile(
                        kennung = f.kennung,
                        anzeigename = f.anzeigename,
                        unterzeile = "Blockiert — weder Anfragen noch Nachrichten.",
                        premium = f.premium,
                        teammitglied = f.teammitglied,
                        randlos = true,
                        modifier = Modifier.zeilenstrich(gestellte.size + i == zahl - 1),
                        hinten = {
                            Knopf("Aufheben", { griffe.loesen(f.kennung) }, art = Knopfart.Leise, kompakt = true)
                        },
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------- Bausteine

/** „Dazuschalten" unter einer Zeile — nur, wer gerade fährt, hat eine Runde dafür. */
private fun dazuschaltknopf(
    wo: Anwesenheit?,
    griffe: FreundeGriffe,
): (@Composable ColumnScope.() -> Unit)? = if (wo == null) null else {
    {
        Knopf(
            if (wo.platzFrei) "Dazuschalten" else "Voll",
            { griffe.dazuschalten(wo.roomCode) },
            aktiv = wo.platzFrei,
            kompakt = true,
        )
    }
}

/** Eine Personenzeile mit dem Schmuck des Freundes — dieselbe Form in allen drei Reitern. */
@Composable
private fun Freundzeile(
    freund: Freund,
    server: String,
    unterzeile: String,
    mitSchmuck: Boolean = true,
    beiDruck: (() -> Unit)? = null,
    hinten: (@Composable () -> Unit)? = null,
    unten: (@Composable ColumnScope.() -> Unit)? = null,
    /** Gesetzt, wenn die Zeile in einer Karte steht: randlos, mit Trennstrich bis auf die letzte. */
    letzte: Boolean? = null,
) {
    Profilzeile(
        modifier = if (letzte != null) Modifier.zeilenstrich(letzte) else Modifier,
        randlos = letzte != null,
        kennung = freund.kennung,
        anzeigename = freund.anzeigename.ifBlank { freund.benutzername },
        unterzeile = unterzeile.ifBlank { null },
        wappen = freund.wappen,
        wappenfarbe = freund.wappenfarbe,
        kopfmuster = if (mitSchmuck) freund.kopfmuster else "keines",
        profilrahmen = if (mitSchmuck && freund.anwesenheit == null) freund.profilrahmen else "keiner",
        bildAdresse = bildweg(server, freund.profilbild),
        premium = freund.premium,
        teammitglied = freund.teammitglied,
        imDienst = freund.anwesenheit != null,
        beiDruck = beiDruck,
        hinten = hinten,
        unten = unten,
    )
}

/**
 * Eine Meldungszeile — was schiefging (rot) oder was gut ging (grün), zum
 * Wegtippen. Das Gegenstück zu `components/freunde/Meldung.vue`.
 */
@Composable
fun Meldungszeile(fehler: String?, hinweis: String? = null, beiSchliessen: () -> Unit) {
    val text = fehler ?: hinweis ?: return
    val farbe = if (fehler != null) Farben.SignalHell else Farben.GruenHell

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = farbe.copy(alpha = 0.6f), ecke = 9.dp)
            .padding(start = Abstand.Normal),
    ) {
        Text(
            text = text,
            style = Schrift.MonoKlein,
            color = farbe,
            modifier = Modifier.weight(1f),
        )
        Textweg("Schließen", beiSchliessen, farbe = Farben.TextLeise)
    }
}

/** Die Gesprächsordnung: Ungelesenes zuerst, dann wer im Dienst ist, dann das jüngste Gespräch. */
private fun List<Freund>.gespraechsordnung(): List<Freund> = sortedWith(
    compareByDescending<Freund> { it.ungelesen > 0 }
        .thenByDescending { it.anwesenheit != null }
        .thenByDescending { it.letzteNachrichtUm?.let(::zeitwert) ?: 0L }
        .thenBy { it.anzeigename.lowercase() },
)

private fun Freund.passt(begriff: String): Boolean {
    val b = begriff.trim().lowercase()
    return b.isEmpty() || anzeigename.lowercase().contains(b) || benutzername.lowercase().contains(b)
}

/**
 * Wo jemand gerade steckt — im Dienst mit Rolle und Ort, sonst „zuletzt online".
 *
 * Wer beides abgeschaltet hat, kommt ohne beides an; dann steht hier nichts,
 * statt einer Vermutung. Wortgleich mit `utils/anwesenheit.ts`.
 */
fun lage(wo: Anwesenheit?, zuletztGesehen: String?): String {
    if (wo == null) return zuletztGesehen?.let(::zuletztOnline).orEmpty()

    val rolle = if (wo.rolle == "Leitstelle") "Leitstelle" else wo.funkrufname ?: "Fahrzeug"
    val zustand = if (wo.zustand == "Lobby") "in der Lobby" else "im Einsatz"
    return "$rolle · ${wo.ort} · $zustand"
}

private fun zuletztOnline(iso: String): String {
    val dann = zeitwert(iso) ?: return ""
    val minuten = maxOf(0L, Duration.between(Instant.ofEpochMilli(dann), Instant.now()).toMinutes())

    if (minuten < 2) return "zuletzt online gerade eben"
    if (minuten < 60) return "zuletzt online vor $minuten Min."
    val stunden = Math.round(minuten / 60.0)
    if (stunden < 24) return "zuletzt online vor $stunden Std."
    val tage = Math.round(stunden / 24.0)
    if (tage < 14) return "zuletzt online vor $tage Tag${if (tage == 1L) "" else "en"}"
    return "zuletzt online am " + DateTimeFormatter.ofPattern("dd.MM.yy")
        .format(Instant.ofEpochMilli(dann).atZone(ZoneId.systemDefault()))
}

/**
 * Eine Zeitangabe für die Gesprächsliste: heute die Uhrzeit, gestern „Gestern",
 * in der Woche der Wochentag, sonst das Datum — wie `kurzzeit` im Web.
 */
fun kurzzeit(iso: String): String {
    val dann = zeitwert(iso)?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
        ?: return ""
    val heute = LocalDate.now()
    return when {
        dann.toLocalDate() == heute -> DateTimeFormatter.ofPattern("HH:mm").format(dann)
        dann.toLocalDate() == heute.minusDays(1) -> "Gestern"
        Duration.between(dann.toInstant(), Instant.now()).toDays() < 7 ->
            dann.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN)
        else -> DateTimeFormatter.ofPattern("dd.MM.yy").format(dann)
    }
}

/** Die Zeichen der vier Reiter — dieselben Pfade wie in `FreundeSeite.vue`. */
private const val REITER_BRETT =
    "M5.5 6h13a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Z M12 3v3 M7.5 11h9 M7.5 15h5.5"
private const val REITER_FREUNDE =
    "M6 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0 M3.5 19.5a6 6 0 0 1 12 0 M16 5.2a3.5 3.5 0 0 1 0 5.6 M17.5 14.4a6 6 0 0 1 3 5.1"
private const val REITER_NACHRICHTEN =
    "M20.5 12.2c0 4-3.8 7.2-8.5 7.2-1 0-2-.15-2.9-.42L4 20.5l1.6-3.6A6.9 6.9 0 0 1 3.5 12.2C3.5 8.2 7.3 5 12 5s8.5 3.2 8.5 7.2Z"
private const val REITER_KONTAKTE =
    "M6.5 8.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0 M4 19.5a6 6 0 0 1 12 0 M18.5 8.5v5 M16 11h5"

/** Ein ISO-Zeitstempel als Millisekunden — `null`, wenn er nicht zu lesen ist. */
internal fun zeitwert(iso: String): Long? =
    runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
