package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Einladung
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Suchtreffer
import de.pagerspass.pagerspass.netz.Vorschlag
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Markenzahl
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Freunde, Nachrichten, Kontakte — drei der vier Wege des Freundebereichs
 * (`views/freunde/FreundeListeView.vue`, `NachrichtenView.vue`, `KontakteView.vue`).
 *
 * <b>Drei Listen, drei Fragen.</b> Die Freunde beantworten „wer ist da und wer
 * fährt gerade", die Nachrichten „wer wartet auf Antwort", die Kontakte „was
 * verlangt eine Antwort oder stellt eine Verbindung her". Deshalb führt eine
 * Zeile unter „Freunde" ins Profil, unter „Nachrichten" ins Gespräch.
 */

// ------------------------------------------------------------ Die Zeile

/**
 * Eine Personenzeile — Kontobild, Name, eine Unterzeile, Knöpfe am Rand.
 *
 * Wie `Profilzeile`, aber mit Wachentag vor dem Namen, einem Platz neben dem
 * Namen (Rang, Zeit) und einem eigenen Weg fürs Bild: In der Gesprächsliste
 * führt die Zeile ins Gespräch und das Bild aufs Profil.
 */
@Composable
private fun Personenzeile(
    kennung: String,
    anzeigename: String,
    bildAdresse: String?,
    modifier: Modifier = Modifier,
    wappen: String = "Keines",
    wappenfarbe: Int = 0,
    kopfmuster: String = "keines",
    profilrahmen: String = "keiner",
    premium: Boolean = false,
    teammitglied: Boolean = false,
    wachentag: String? = null,
    imDienst: Boolean = false,
    hervorgehoben: Boolean = false,
    beiDruck: (() -> Unit)? = null,
    beiBild: (() -> Unit)? = null,
    nebenName: (@Composable () -> Unit)? = null,
    unterzeile: (@Composable () -> Unit)? = null,
    hinten: (@Composable RowScope.() -> Unit)? = null,
    unten: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .fillMaxWidth()
            .clip(Rundung.Klein)
            .background(Farben.Flaeche)
            .kopfband(kopfmuster, Wappen.ton(kennung, wappenfarbe), zeile = true)
            .border(1.dp, if (hervorgehoben) Farben.AmberTief else Farben.Rand, Rundung.Klein)
            .then(
                if (beiDruck != null) {
                    Modifier.clickable(
                        onClick = beiDruck,
                        role = Role.Button,
                        indication = null,
                        interactionSource = null,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Ziel.Normal),
        ) {
            Kontobild(
                kennung = kennung,
                anzeigename = anzeigename,
                wappen = wappen,
                wappenfarbe = wappenfarbe,
                bildAdresse = bildAdresse,
                rahmen = profilrahmen,
                imDienst = imDienst,
                modifier = if (beiBild != null) {
                    Modifier.clickable(
                        onClick = beiBild,
                        role = Role.Button,
                        indication = null,
                        interactionSource = null,
                    )
                } else {
                    Modifier
                },
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Personenname(
                        name = anzeigename,
                        premium = premium,
                        teammitglied = teammitglied,
                        wachentag = wachentag,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    nebenName?.invoke()
                }
                unterzeile?.invoke()
            }
            hinten?.invoke(this)
        }
        unten?.invoke(this)
    }
}

@Composable
private fun Unterzeile(text: String, farbe: androidx.compose.ui.graphics.Color = Farben.TextLeise, mono: Boolean = false) {
    Text(
        text = text,
        style = if (mono) Schrift.MonoKlein else Schrift.Klein,
        color = farbe,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Das Suchfeld der beiden Listen — erst ab einer Liste, die man nicht mehr überblickt. */
private fun passt(f: Freund, suche: String): Boolean {
    val begriff = suche.trim().lowercase()
    if (begriff.isEmpty()) return true
    return f.anzeigename.lowercase().contains(begriff) || f.benutzername.lowercase().contains(begriff)
}

// --------------------------------------------------------------- Freunde

/**
 * Deine Freunde — wer sie sind und wer gerade fährt.
 *
 * <b>Nach Lage, nicht nach Nachricht:</b> oben, wer im Dienst ist — das ist das
 * Einzige an dieser Liste, was sich in Minuten ändert und wonach man handelt.
 * Darunter die übrigen, nach Namen. Verwaltet wird hier nichts; die Zeile führt
 * ins Profil, der Knopf daneben ins Gespräch.
 */
@Composable
fun ColumnScope.FreundeListeInhalt(
    konto: Konto?,
    bestaetigte: List<Freund>,
    laedt: Boolean,
    geladen: Boolean,
    fehler: String?,
    server: String,
    meldung: String?,
    beiMeldungWeg: () -> Unit,
    beiLaden: () -> Unit,
    beiProfil: (String) -> Unit,
    beiGespraech: (String) -> Unit,
    beiDazuschalten: (Freund) -> Unit,
) {
    if (konto == null) {
        Leerhinweis("Freunde gehören zu einem Konto.")
        return
    }
    if (!geladen && laedt) {
        FreundeLaden()
        return
    }
    if (fehler != null && bestaetigte.isEmpty()) {
        Fehlerzeile(fehler, beiLaden)
        return
    }

    var suche by rememberSaveable { mutableStateOf("") }
    if (bestaetigte.size > 4) {
        Feld(
            wert = suche,
            beiAenderung = { suche = it.take(48) },
            platzhalter = "Name suchen …",
            weiterTaste = ImeAction.Search,
        )
    }

    val gezeigt = bestaetigte.filter { passt(it, suche) }
    val nachNamen = compareBy<Freund> { it.anzeigename.lowercase() }
    val imDienst = gezeigt.filter { it.anwesenheit != null }.sortedWith(nachNamen)
    val ruhend = gezeigt.filter { it.anwesenheit == null }.sortedWith(nachNamen)

    when {
        bestaetigte.isEmpty() -> Leerhinweis(
            "Noch niemand. Unter „Kontakte“ findest du Leute, mit denen du schon gefahren bist.",
        )
        suche.isNotBlank() && gezeigt.isEmpty() -> Leerhinweis("Niemand mit diesem Namen.")
    }

    listOf("Im Dienst" to imDienst, "Gerade nicht im Dienst" to ruhend).forEach { (titel, leute) ->
        if (leute.isEmpty()) return@forEach
        Abschnitt {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Ueberschrift(titel)
                SehrLeise(leute.size.toString(), mono = true)
            }
            leute.forEach { f ->
                val lage = lagezeile(f.anwesenheit, f.zuletztGesehen)
                Personenzeile(
                    kennung = f.kennung,
                    anzeigename = f.anzeigename,
                    bildAdresse = bildweg(server, f.profilbild),
                    wappen = f.wappen,
                    wappenfarbe = f.wappenfarbe,
                    kopfmuster = f.kopfmuster,
                    premium = f.premium,
                    teammitglied = f.teammitglied,
                    wachentag = f.wachentag,
                    imDienst = f.anwesenheit != null,
                    beiDruck = { beiProfil(f.benutzername) },
                    nebenName = {
                        if (f.rang.isNotBlank()) Marke("${f.rang} · Stufe ${f.level}")
                    },
                    unterzeile = {
                        // Eine Zeile, eine Auskunft: die Lage in Grün, sonst „zuletzt
                        // online"; der Benutzername nur, wenn beides fehlt.
                        if (lage.isNotBlank()) {
                            Unterzeile(lage, if (f.anwesenheit != null) Farben.GruenHell else Farben.TextLeise)
                        } else {
                            Unterzeile(f.benutzername, Farben.TextSehrLeise, mono = true)
                        }
                    },
                    unten = {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        ) {
                            f.anwesenheit?.let { wo ->
                                Knopf(
                                    aufschrift = if (wo.platzFrei) "Dazuschalten" else "Voll",
                                    beiDruck = { beiDazuschalten(f) },
                                    aktiv = wo.platzFrei,
                                    kompakt = true,
                                )
                            }
                            Knopf(
                                aufschrift = "Schreiben",
                                beiDruck = { beiGespraech(f.kennung) },
                                art = Knopfart.Leise,
                                kompakt = true,
                                // Die Marke zeichnet sich bei null gar nicht erst.
                                zeichenVorn = { Markenzahl(f.ungelesen) },
                            )
                        }
                    },
                )
            }
        }
    }

    // Nur, wenn wirklich niemand fährt — nicht schon, wenn die Suche niemanden übrig lässt.
    if (bestaetigte.isNotEmpty() && bestaetigte.none { it.anwesenheit != null } && suche.isBlank()) {
        Leise(
            "Gerade fährt niemand aus deiner Liste. Wer in den Dienst geht, rückt hier von " +
                "allein nach oben.",
        )
    }

    Meldungsstreifen(meldung, beiMeldungWeg)
}

// ----------------------------------------------------------- Nachrichten

/**
 * Die Gesprächsliste: je Freund eine Zeile mit der jüngsten Nachricht.
 *
 * Die ganze Zeile ist der Weg ins Gespräch — am Daumen ist die Zeile das Ziel.
 * Das Bild führt aufs Profil. Suche und Sieb erst ab einer Liste, die man nicht
 * mehr überblickt: bei drei Freunden wären sie zwei Zeilen Verwaltung über drei
 * Zeilen Inhalt.
 */
@Composable
fun ColumnScope.NachrichtenInhalt(
    konto: Konto?,
    bestaetigte: List<Freund>,
    ungelesen: Int,
    laedt: Boolean,
    geladen: Boolean,
    fehler: String?,
    server: String,
    meldung: String?,
    beiMeldungWeg: () -> Unit,
    beiLaden: () -> Unit,
    beiProfil: (String) -> Unit,
    beiGespraech: (String) -> Unit,
    beiDazuschalten: (Freund) -> Unit,
) {
    if (konto == null) {
        Leerhinweis("Nachrichten gehören zu einem Konto.")
        return
    }
    if (!geladen && laedt) {
        FreundeLaden()
        return
    }
    if (fehler != null && bestaetigte.isEmpty()) {
        Fehlerzeile(fehler, beiLaden)
        return
    }

    var sieb by rememberSaveable { mutableStateOf("Alle") }
    var suche by rememberSaveable { mutableStateOf("") }
    val imDienstZahl = bestaetigte.count { it.anwesenheit != null }

    if (bestaetigte.size > 4) {
        Feld(
            wert = suche,
            beiAenderung = { suche = it.take(48) },
            platzhalter = "Name suchen …",
            weiterTaste = ImeAction.Search,
        )
        // Die Zahl steht am Sieb, nicht daneben: Sie ist der Grund, es umzustellen.
        Segment(
            seiten = listOf("Alle", "Ungelesen", "ImDienst"),
            gewaehlt = sieb,
            beiWahl = { sieb = it },
            aufschrift = {
                when (it) {
                    "Ungelesen" -> if (ungelesen > 0) "Ungelesen $ungelesen" else "Ungelesen"
                    "ImDienst" -> if (imDienstZahl > 0) "Im Dienst $imDienstZahl" else "Im Dienst"
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
            else -> passt(f, suche)
        }
    }

    val leer = when {
        bestaetigte.isEmpty() ->
            "Noch niemand. Unter „Kontakte“ findest du Leute, mit denen du schon gefahren bist."
        gezeigt.isNotEmpty() -> null
        suche.isNotBlank() -> "Niemand mit diesem Namen."
        sieb == "Ungelesen" -> "Nichts Ungelesenes — alles beantwortet."
        sieb == "ImDienst" -> "Gerade fährt niemand von deinen Freunden."
        else -> null
    }
    if (leer != null) Leerhinweis(leer)

    gezeigt.forEach { f ->
        val lage = lagezeile(f.anwesenheit, f.zuletztGesehen)
        Personenzeile(
            kennung = f.kennung,
            anzeigename = f.anzeigename,
            bildAdresse = bildweg(server, f.profilbild),
            wappen = f.wappen,
            wappenfarbe = f.wappenfarbe,
            premium = f.premium,
            teammitglied = f.teammitglied,
            wachentag = f.wachentag,
            imDienst = f.anwesenheit != null,
            hervorgehoben = f.ungelesen > 0,
            beiDruck = { beiGespraech(f.kennung) },
            beiBild = { beiProfil(f.benutzername) },
            nebenName = {
                f.letzteNachrichtUm?.let { SehrLeise(postfachzeit(it), mono = true) }
            },
            unterzeile = {
                when {
                    f.anwesenheit != null -> Unterzeile(lage, Farben.GruenHell, mono = true)
                    f.letzteNachricht != null -> Unterzeile(
                        (if (f.letzteNachrichtVonMir) "Du: " else "") + f.letzteNachricht,
                        Farben.TextSehrLeise,
                    )
                    else -> Unterzeile(
                        f.benutzername + if (lage.isNotBlank()) " · $lage" else "",
                        Farben.TextSehrLeise,
                        mono = true,
                    )
                }
            },
            hinten = {
                Markenzahl(f.ungelesen)
                f.anwesenheit?.let { wo ->
                    Knopf(
                        aufschrift = if (wo.platzFrei) "Dazuschalten" else "Voll",
                        beiDruck = { beiDazuschalten(f) },
                        aktiv = wo.platzFrei,
                        kompakt = true,
                    )
                }
            },
        )
    }

    Meldungsstreifen(meldung, beiMeldungWeg)
}

// -------------------------------------------------------------- Kontakte

/** Was der Stand eines Treffers für einen selbst bedeutet. */
private fun trefferstand(stand: String): String = when (stand) {
    "Bestaetigt" -> "Schon befreundet"
    "Angefragt" -> "Anfrage läuft"
    else -> "Nicht möglich"
}

/**
 * Kontakte: alles, was eine Antwort verlangt oder eine Verbindung herstellt —
 * Einladungen, offene Anfragen, Suche, Vorschläge, und ganz unten das
 * Abgeräumte.
 *
 * Die Reihenfolge ist die der Dringlichkeit: was auf mich wartet, steht oben;
 * was ich suchen will, darunter; was ich abgelehnt oder blockiert habe, ganz
 * unten hinter einer Klappe.
 */
@Composable
fun ColumnScope.KontakteInhalt(
    konto: Konto?,
    einladungen: List<Einladung>,
    offeneAnfragen: List<Freund>,
    gestellteAnfragen: List<Freund>,
    blockierte: List<Freund>,
    vorschlaege: List<Vorschlag>,
    offeneVorschlaege: List<Vorschlag>,
    weggelegt: Int,
    treffer: Suchtreffer?,
    sucht: Boolean,
    server: String,
    meldung: String?,
    beiMeldungWeg: () -> Unit,
    beiEinladungAnnehmen: (Einladung) -> Unit,
    beiEinladungAblehnen: (Einladung) -> Unit,
    beiAntworten: (String, Boolean) -> Unit,
    beiSuchen: (String) -> Unit,
    beiAnfragen: (String) -> Unit,
    beiWeglegen: (String) -> Unit,
    beiZurueckholen: () -> Unit,
    beiLoesen: (String) -> Unit,
    beiProfil: (String) -> Unit,
) {
    if (konto == null) {
        Leerhinweis("Kontakte gehören zu einem Konto.")
        return
    }

    // ------------------------------------------------------------ Einladungen
    if (einladungen.isNotEmpty()) {
        Ueberschrift("Einladungen in eine Runde")
        einladungen.forEach { e ->
            Kasten(marke = true, wartet = e.annehmbar, abstandInnen = Abstand.Klein) {
                Text(
                    text = "${e.vonName} lädt dich " +
                        if (e.alsZuschauer) "zum Zuschauen in eine Runde" else "in eine Runde",
                    style = Schrift.Normal,
                    color = Farben.Text,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                ) {
                    Marke(e.roomCode)
                    val wo = e.landkreis?.takeIf { it.isNotBlank() } ?: e.ort.takeIf { it.isNotBlank() }
                    if (wo != null) Leise(wo)
                    if (e.zustand != null) {
                        Text(
                            text = "${e.spieler}/${e.maxSpieler} Spieler",
                            style = Schrift.MonoKlein,
                            color = Farben.TextLeise,
                        )
                    }
                }
                if (e.mitspieler.isNotEmpty()) SehrLeise("Dabei: ${e.mitspieler.joinToString(", ")}")
                e.hinweis?.let { Text(text = it, style = Schrift.Klein, color = Farben.AmberHell) }
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        aufschrift = if (e.alsZuschauer) "Zuschauen" else "Annehmen",
                        beiDruck = { beiEinladungAnnehmen(e) },
                        art = Knopfart.Haupt,
                        aktiv = e.annehmbar,
                        kompakt = true,
                    )
                    Knopf(
                        aufschrift = if (e.annehmbar) "Ablehnen" else "Wegräumen",
                        beiDruck = { beiEinladungAblehnen(e) },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }
        }
    }

    // --------------------------------------------------------- Offene Anfragen
    if (offeneAnfragen.isNotEmpty()) {
        Abschnitt("Warten auf deine Antwort") {
            offeneAnfragen.forEach { f ->
                Personenzeile(
                    kennung = f.kennung,
                    anzeigename = f.anzeigename,
                    bildAdresse = bildweg(server, f.profilbild),
                    wappen = f.wappen,
                    wappenfarbe = f.wappenfarbe,
                    kopfmuster = f.kopfmuster,
                    profilrahmen = f.profilrahmen,
                    premium = f.premium,
                    teammitglied = f.teammitglied,
                    wachentag = f.wachentag,
                    hervorgehoben = true,
                    beiDruck = { beiProfil(f.benutzername) },
                    unterzeile = {
                        Unterzeile(
                            listOf(f.benutzername, f.rang).filter { it.isNotBlank() }.joinToString(" · "),
                            mono = true,
                        )
                    },
                    unten = {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf(
                                "Annehmen",
                                { beiAntworten(f.kennung, true) },
                                art = Knopfart.Haupt,
                                kompakt = true,
                            )
                            Knopf(
                                "Ablehnen",
                                { beiAntworten(f.kennung, false) },
                                art = Knopfart.Leise,
                                kompakt = true,
                            )
                        }
                    },
                )
            }
        }
    }

    // ------------------------------------------------------------------ Suche
    var suchbegriff by rememberSaveable { mutableStateOf("") }
    Abschnitt("Jemanden hinzufügen") {
        Kasten(abstandInnen = Abstand.Klein) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Feld(
                    wert = suchbegriff,
                    beiAenderung = { suchbegriff = it.take(20) },
                    etikett = "Benutzername",
                    platzhalter = "z. B. kim-42",
                    weiterTaste = ImeAction.Search,
                    stil = Schrift.MonoNormal,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    aufschrift = "Suchen",
                    beiDruck = { if (suchbegriff.isNotBlank()) beiSuchen(suchbegriff.trim()) },
                    art = Knopfart.Haupt,
                    aktiv = !sucht && suchbegriff.isNotBlank(),
                    kompakt = true,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            SehrLeise(
                "Gesucht wird über den eindeutigen Benutzernamen, nicht über den Anzeigenamen — " +
                    "deinen findest du in deinem Profil.",
            )
        }

        treffer?.let { t ->
            Personenzeile(
                kennung = t.kennung,
                anzeigename = t.anzeigename,
                bildAdresse = bildweg(server, t.profilbild),
                premium = t.premium,
                teammitglied = t.teammitglied,
                wachentag = t.wachentag,
                beiDruck = { beiProfil(t.benutzername) },
                unterzeile = {
                    Unterzeile(
                        t.benutzername + (t.rang?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                        mono = true,
                    )
                },
                hinten = {
                    val stand = t.stand
                    if (stand == null) {
                        Knopf("Anfragen", { beiAnfragen(t.kennung) }, art = Knopfart.Haupt, kompakt = true)
                    } else {
                        Marke(trefferstand(stand))
                    }
                },
            )
        }
    }

    // ------------------------------------------------------------- Vorschläge
    if (offeneVorschlaege.isNotEmpty()) {
        Abschnitt("Leute, mit denen du gefahren bist") {
            offeneVorschlaege.forEach { v ->
                Kasten(abstandInnen = Abstand.Klein) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = { beiProfil(v.benutzername) },
                                role = Role.Button,
                                indication = null,
                                interactionSource = null,
                            ),
                    ) {
                        Kontobild(
                            kennung = v.kennung,
                            anzeigename = v.anzeigename,
                            bildAdresse = bildweg(server, v.profilbild),
                            groesse = 48.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Personenname(
                                name = v.anzeigename,
                                premium = v.premium,
                                teammitglied = v.teammitglied,
                                wachentag = v.wachentag,
                            )
                            if (v.rang.isNotBlank()) SehrLeise(v.rang, mono = true)
                        }
                    }
                    // Der Grund, warum dieser Mensch hier steht — ohne ihn ist ein
                    // Vorschlag eine Behauptung, mit ihm eine Erinnerung.
                    Leise(
                        "${v.gemeinsameSchichten} " +
                            (if (v.gemeinsameSchichten == 1) "gemeinsame Schicht" else "gemeinsame Schichten") +
                            ", zuletzt ${kurzdatum(v.zuletztZusammen)}",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            "Anfragen",
                            { beiAnfragen(v.kennung) },
                            art = Knopfart.Haupt,
                            kompakt = true,
                        )
                        Knopf("Weglegen", { beiWeglegen(v.kennung) }, art = Knopfart.Leise, kompakt = true)
                    }
                }
            }
            if (weggelegt > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SehrLeise("$weggelegt weggelegt.")
                    Textweg("Wieder zeigen", beiZurueckholen)
                }
            }
        }
    } else if (weggelegt > 0 && vorschlaege.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SehrLeise("Alle Vorschläge weggelegt.")
            Textweg("Wieder zeigen", beiZurueckholen)
        }
    }

    // Wer frisch anfängt, sieht nur ein Suchfeld — und weiß nicht, dass die Seite
    // sich von selbst füllt.
    if (einladungen.isEmpty() && offeneAnfragen.isEmpty() && vorschlaege.isEmpty()) {
        Leerhinweis(
            "Nach einer gemeinsamen Schicht stehen hier Vorschläge: Leute, mit denen du " +
                "gefahren bist. Einladungen in eine Runde und offene Anfragen landen ebenfalls hier.",
        )
    }

    Meldungsstreifen(meldung, beiMeldungWeg)

    // ------------------------------------------ Eigene Anfragen und Blockiertes
    if (gestellteAnfragen.isNotEmpty() || blockierte.isNotEmpty()) {
        var offen by rememberSaveable { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Ziel.Normal)
                .clickable(
                    onClick = { offen = !offen },
                    role = Role.Button,
                    indication = null,
                    interactionSource = null,
                ),
        ) {
            Text(
                text = (if (offen) "▾ " else "▸ ") + "Eigene Anfragen und Blockiertes",
                style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold),
                color = Farben.TextLeise,
            )
            SehrLeise((gestellteAnfragen.size + blockierte.size).toString(), mono = true)
        }

        if (offen) {
            gestellteAnfragen.forEach { f ->
                Personenzeile(
                    kennung = f.kennung,
                    anzeigename = f.anzeigename,
                    bildAdresse = bildweg(server, f.profilbild),
                    wappen = f.wappen,
                    wappenfarbe = f.wappenfarbe,
                    kopfmuster = f.kopfmuster,
                    profilrahmen = f.profilrahmen,
                    premium = f.premium,
                    teammitglied = f.teammitglied,
                    wachentag = f.wachentag,
                    beiDruck = { beiProfil(f.benutzername) },
                    unterzeile = { Unterzeile("Deine Anfrage läuft.", Farben.TextSehrLeise) },
                    hinten = {
                        Knopf("Zurückziehen", { beiLoesen(f.kennung) }, art = Knopfart.Leise, kompakt = true)
                    },
                )
            }
            blockierte.forEach { f ->
                // Ohne Bild, ohne Band, ohne Wappen: Wer jemanden blockiert hat, soll
                // dessen Gesicht nicht wiedersehen, nur weil er nachschlägt.
                Personenzeile(
                    kennung = f.kennung,
                    anzeigename = f.anzeigename,
                    bildAdresse = null,
                    premium = f.premium,
                    teammitglied = f.teammitglied,
                    wachentag = f.wachentag,
                    unterzeile = {
                        Unterzeile("Blockiert — weder Anfragen noch Nachrichten.", Farben.TextSehrLeise)
                    },
                    hinten = {
                        Knopf("Aufheben", { beiLoesen(f.kennung) }, art = Knopfart.Leise, kompakt = true)
                    },
                )
            }
        }
    }
}
