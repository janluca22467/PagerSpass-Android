package de.pagerspass.pagerspass.ansichten

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Bretteintrag
import de.pagerspass.pagerspass.netz.Brettkommentar
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Nachricht
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Ein Eintrag und das Gespräch — die beiden Seiten des Freundebereichs, die eine
 * Mitteilung öffnen kann (`views/freunde/EintragView.vue`, `views/GespraechView.vue`).
 */

// ------------------------------------------------------------------ Eintrag

/**
 * Ein einzelner Eintrag samt seinen Kommentaren.
 *
 * Eine eigene Adresse, weil eine Mitteilung sie öffnen muss: „Kim hat
 * kommentiert" führt hierher und nicht auf ein Brett, in dem man die Zeile erst
 * suchen müsste. Der offene Eintrag gehört der Seite — beim Verlassen wird er
 * geschlossen, sonst schriebe der Kommentar-Handler in eine Liste, die niemand
 * mehr ansieht.
 */
@Composable
fun ColumnScope.EintragInhalt(
    nr: Long,
    eintrag: Bretteintrag?,
    laedt: Boolean,
    fehler: String?,
    kommentare: List<Brettkommentar>,
    server: String,
    meldung: String?,
    beiMeldungWeg: () -> Unit,
    beiLaden: (Long) -> Unit,
    beiSchliessen: (Long) -> Unit,
    beiQuittieren: (Long) -> Unit,
    beiEntfernen: (Long) -> Unit,
    beiMelden: (Long) -> Unit,
    beiKommentarMelden: (Long) -> Unit,
    beiKommentieren: (nr: Long, text: String, danach: (Boolean) -> Unit) -> Unit,
    beiProfil: (String) -> Unit,
    beiBezug: (art: String, id: String) -> Unit,
    beiZurueck: () -> Unit,
) {
    val schliessen by rememberUpdatedState(beiSchliessen)
    LaunchedEffect(nr) { beiLaden(nr) }
    DisposableEffect(nr) { onDispose { schliessen(nr) } }

    var text by rememberSaveable(nr) { mutableStateOf("") }
    var gemeldet by remember(nr) { mutableStateOf<Long?>(null) }

    Seitenkopf(
        titel = "Eintrag",
        knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
    )

    when {
        laedt && eintrag == null -> FreundeLaden()
        fehler != null && eintrag == null -> Fehlerzeile(fehler, { beiLaden(nr) })
        eintrag != null -> {
            EintragKarte(
                eintrag = eintrag,
                server = server,
                einzeln = true,
                beiQuittieren = { beiQuittieren(eintrag.nr) },
                beiKommentare = {},
                beiEntfernen = { beiEntfernen(eintrag.nr) },
                beiMelden = { beiMelden(eintrag.nr) },
                beiProfil = beiProfil,
                beiBezug = beiBezug,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Abstand.Normal),
            ) {
                Ueberschrift("Kommentare")
                if (kommentare.isNotEmpty()) SehrLeise(kommentare.size.toString(), mono = true)
            }

            if (kommentare.isEmpty()) {
                Leerhinweis("Noch nichts gesagt. Der erste Satz gehört dir.")
            }

            kommentare.forEach { k ->
                Kommentarzeile(
                    kommentar = k,
                    server = server,
                    gefragt = gemeldet == k.nr,
                    beiProfil = beiProfil,
                    beiMelden = {
                        if (gemeldet != k.nr) {
                            gemeldet = k.nr
                        } else {
                            gemeldet = null
                            beiKommentarMelden(k.nr)
                        }
                    },
                )
            }

            // Unter einem öffentlichen Eintrag ist auch der Kommentar öffentlich —
            // das steht am Feld, bevor jemand tippt.
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche()
                    .padding(Abstand.Klein),
            ) {
                Feld(
                    wert = text,
                    beiAenderung = { text = it.take(300) },
                    platzhalter = if (eintrag.sichtbarkeit == "Oeffentlich") {
                        "Öffentlich antworten …"
                    } else {
                        "Antworten …"
                    },
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    aufschrift = "Senden",
                    beiDruck = {
                        val schicken = text.trim()
                        if (schicken.isNotEmpty()) {
                            beiKommentieren(eintrag.nr, schicken) { ok -> if (ok) text = "" }
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = text.isNotBlank(),
                    kompakt = true,
                )
            }

            Meldungsstreifen(meldung, beiMeldungWeg)
        }
    }
}

@Composable
private fun Kommentarzeile(
    kommentar: Brettkommentar,
    server: String,
    gefragt: Boolean,
    beiProfil: (String) -> Unit,
    beiMelden: () -> Unit,
) {
    val v = kommentar.verfasser
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                ecke = 9.dp,
                randfarbe = if (kommentar.vonMir) Farben.AmberTief else Farben.Rand,
            )
            .padding(Abstand.Normal),
    ) {
        Kontobild(
            kennung = v.kennung,
            anzeigename = v.anzeigename,
            wappen = v.wappen ?: "Keines",
            wappenfarbe = v.wappenfarbe,
            bildAdresse = bildweg(server, v.profilbild),
            modifier = if (v.benutzername.isNotBlank()) {
                Modifier.clickable(
                    onClick = { beiProfil(v.benutzername) },
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Personenname(
                    name = v.anzeigename,
                    premium = v.premium,
                    teammitglied = v.teammitglied,
                    wachentag = v.wachentag,
                    stil = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (v.benutzername.isNotBlank()) {
                                Modifier.clickable(
                                    onClick = { beiProfil(v.benutzername) },
                                    role = Role.Button,
                                    indication = null,
                                    interactionSource = null,
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
                SehrLeise(wieLangeHer(kommentar.erstelltUm), mono = true)
            }
            Text(text = kommentar.text, style = Schrift.Klein, color = Farben.Text)
            if (!kommentar.vonMir) {
                Knopf(
                    aufschrift = if (gefragt) "Wirklich melden?" else "Melden",
                    beiDruck = beiMelden,
                    art = if (gefragt) Knopfart.Gefahr else Knopfart.Leise,
                    kompakt = true,
                )
            }
        }
    }
}

// ----------------------------------------------------------------- Gespräch

/** Eine Zeile des Verlaufs, wie sie gezeichnet wird: Trenner oder Nachricht. */
private sealed interface Verlaufszeile {
    val schluessel: String

    data class Trenner(val text: String, override val schluessel: String) : Verlaufszeile
    data class Botschaft(val nachricht: Nachricht, val anschluss: Boolean) : Verlaufszeile {
        override val schluessel: String get() = "n${nachricht.nr}"
    }
}

/**
 * Wie der Verlauf gezeichnet wird: vor jedem neuen Tag ein Trenner, und
 * Nachrichten desselben Absenders innerhalb von fünf Minuten rücken zusammen.
 * Drei Sätze hintereinander sind ein Gedanke und keine drei Meldungen.
 */
private fun verlaufszeilen(verlauf: List<Nachricht>): List<Verlaufszeile> = buildList {
    verlauf.forEachIndexed { index, n ->
        val davor = verlauf.getOrNull(index - 1)
        val neuerTag = davor == null || kalendertag(davor.gesendetUm) != kalendertag(n.gesendetUm)
        if (neuerTag) add(Verlaufszeile.Trenner(tagesname(n.gesendetUm), "t${n.nr}"))

        val anschluss = !neuerTag && davor != null &&
            davor.von == n.von &&
            n.terminStatus == null && davor.terminStatus == null &&
            abstandMinuten(davor.gesendetUm, n.gesendetUm) < 5
        add(Verlaufszeile.Botschaft(n, anschluss))
    }
}

private fun abstandMinuten(vorher: String, nachher: String): Long {
    val a = zeitLesen(vorher) ?: return Long.MAX_VALUE
    val b = zeitLesen(nachher) ?: return Long.MAX_VALUE
    return Duration.between(a, b).toMinutes()
}

private val ERINNERUNG = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")

/**
 * Das Gespräch mit einem Freund — eine eigene Seite, der Verlauf bekommt die
 * ganze Höhe: Kopf oben, Eingabe unten, nur der Verlauf rollt.
 *
 * <b>Mitgerollt wird nur, solange man unten steht.</b> Wer nachliest, wird
 * nicht nach unten gerissen; stattdessen erscheint „Neue Nachrichten ↓".
 *
 * <b>Das Feld wird beim Senden sofort frei</b> — die Nachricht kommt über den
 * Hub zurück. Lehnt der Server ab, steht der Entwurf wieder im Feld.
 */
@Composable
fun GespraechSeite(
    partnerKennung: String,
    partner: Freund?,
    geladen: Boolean,
    verlauf: List<Nachricht>,
    verlaufLaedt: Boolean,
    meineKennung: String,
    server: String,
    meldung: String?,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    beiOeffnen: (String) -> Unit,
    beiSchliessen: (String) -> Unit,
    beiSenden: (text: String, terminText: String?, terminZeitpunkt: String?, danach: (Boolean) -> Unit) -> Unit,
    beiTermin: (Long, Boolean) -> Unit,
    beiGeschenk: (nr: Long, danach: (String?) -> Unit) -> Unit,
    beiDazuschalten: (Freund) -> Unit,
    beiProfil: (String) -> Unit,
    beiMeldungWeg: () -> Unit,
    beiZurueck: () -> Unit,
    beiFreundesliste: () -> Unit,
) {
    val schliessen by rememberUpdatedState(beiSchliessen)
    LaunchedEffect(partnerKennung) { beiOeffnen(partnerKennung) }
    DisposableEffect(partnerKennung) { onDispose { schliessen(partnerKennung) } }

    var entwurf by rememberSaveable(partnerKennung) { mutableStateOf("") }
    var terminOffen by rememberSaveable { mutableStateOf(false) }
    var terminText by rememberSaveable { mutableStateOf("") }
    var terminZeit by rememberSaveable { mutableStateOf<String?>(null) }
    var oeffnetGerade by remember { mutableStateOf<Long?>(null) }
    val geschenkFehler = remember { mutableStateMapOf<Long, String>() }

    val zeilen = remember(verlauf) { verlaufszeilen(verlauf) }
    val liste = rememberLazyListState()
    val unten by remember {
        derivedStateOf {
            val info = liste.layoutInfo
            val letzte = info.visibleItemsInfo.lastOrNull()
            letzte == null || letzte.index >= info.totalItemsCount - 2
        }
    }
    var neueUnten by remember { mutableStateOf(false) }
    var ersterStand by remember(partnerKennung) { mutableStateOf(true) }

    // Automatisch ans Ende nur, solange man unten steht — oder wenn man selbst
    // geschrieben hat. Beim ersten Stand immer.
    LaunchedEffect(zeilen.size, verlauf.lastOrNull()?.nr) {
        if (zeilen.isEmpty()) return@LaunchedEffect
        val letzte = verlauf.lastOrNull()
        if (ersterStand || unten || letzte?.von == meineKennung) {
            liste.scrollToItem(zeilen.size - 1)
            neueUnten = false
            ersterStand = false
        } else {
            neueUnten = true
        }
    }
    LaunchedEffect(unten) { if (unten) neueUnten = false }

    val dichte = LocalDensity.current
    val tastatur = with(dichte) { WindowInsets.ime.getBottom(dichte).toDp() }
    val leiste = with(dichte) { WindowInsets.navigationBars.getBottom(dichte).toDp() }
    val oben = with(dichte) { WindowInsets.statusBars.getTop(dichte).toDp() }
    val zusammenhang = LocalContext.current

    fun senden() {
        val text = entwurf
        if (text.isBlank()) return
        entwurf = ""
        beiSenden(text, null, null) { ok -> if (!ok) entwurf = text }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize().background(Farben.Bg),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Mass.Listenbreite)
                .padding(
                    start = Abstand.Gross,
                    end = Abstand.Gross,
                    top = oben + Abstand.Normal,
                    bottom = maxOf(tastatur, leiste + unterrand) + Abstand.Normal,
                ),
        ) {
            Gespraechskopf(
                partner = partner,
                server = server,
                beiZurueck = beiZurueck,
                beiProfil = beiProfil,
                beiDazuschalten = beiDazuschalten,
            )

            // Wer hier landet, ohne (noch) befreundet zu sein, bekommt das gesagt
            // statt einer leeren Eingabe, deren Senden der Server ohnehin abwiese.
            if (geladen && partner == null) {
                Kasten {
                    Text(
                        text = "Mit diesem Konto besteht keine Freundschaft — vielleicht wurde sie gelöst.",
                        style = Schrift.Normal,
                        color = Farben.Text,
                    )
                    Knopf("Zur Freundesliste", beiFreundesliste, kompakt = true)
                }
                return@Column
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = liste,
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (zeilen.isEmpty()) {
                        item(key = "leer") {
                            SehrLeise(
                                if (verlaufLaedt) "Der Verlauf wird geladen …"
                                else "Noch nichts geschrieben. Der erste Satz gehört dir.",
                                modifier = Modifier.fillMaxWidth().padding(vertical = Abstand.Gross),
                            )
                        }
                    }
                    items(zeilen, key = { it.schluessel }) { zeile ->
                        when (zeile) {
                            is Verlaufszeile.Trenner -> Text(
                                text = zeile.text,
                                style = Schrift.Winzig,
                                color = Farben.TextSehrLeise,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Abstand.Normal),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )

                            is Verlaufszeile.Botschaft -> Box(
                                modifier = Modifier.padding(
                                    top = if (zeile.anschluss) 0.dp else Abstand.Klein,
                                ),
                            ) {
                                val n = zeile.nachricht
                                val eigene = n.von == meineKennung
                                val geschenk = n.geschenk
                                when {
                                    n.terminStatus != null -> Terminkarte(n, eigene, beiTermin)
                                    geschenk != null -> Geschenkkarte(
                                        nachricht = n,
                                        eigene = eigene,
                                        oeffnet = oeffnetGerade == geschenk.nr,
                                        fehler = geschenkFehler[geschenk.nr],
                                        beiOeffnen = {
                                            if (oeffnetGerade == null) {
                                                oeffnetGerade = geschenk.nr
                                                geschenkFehler.remove(geschenk.nr)
                                                beiGeschenk(geschenk.nr) { fehler ->
                                                    if (fehler != null) geschenkFehler[geschenk.nr] = fehler
                                                    oeffnetGerade = null
                                                }
                                            }
                                        },
                                    )
                                    else -> Sprechblase(n, eigene)
                                }
                            }
                        }
                    }
                }

                if (neueUnten) {
                    Text(
                        text = "Neue Nachrichten ↓",
                        style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                        color = Farben.AufAmber,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Abstand.Normal)
                            .background(Farben.Amber, Rundung.Rund)
                            .clickable(
                                onClick = { neueUnten = false },
                                role = Role.Button,
                                indication = null,
                                interactionSource = null,
                            )
                            .padding(horizontal = Abstand.Gross, vertical = Abstand.Klein),
                    )
                }
            }

            // Ein Tipp auf „Neue Nachrichten" rollt ans Ende — außerhalb der
            // Liste, weil das Rollen eine Aussetzfunktion braucht.
            LaunchedEffect(neueUnten) {
                if (!neueUnten && zeilen.isNotEmpty() && !unten) liste.animateScrollToItem(zeilen.size - 1)
            }

            Meldungsstreifen(meldung, beiMeldungWeg)

            // ---------------------------------------------------- Terminvorschlag
            if (terminOffen) {
                Kasten(abstandInnen = Abstand.Klein) {
                    Feld(
                        wert = terminText,
                        beiAenderung = { terminText = it.take(120) },
                        etikett = "Terminvorschlag",
                        platzhalter = "z. B. Sa 20:00",
                    )
                    Etikett("Erinnerung (optional)")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Knopf(
                            aufschrift = terminZeit?.let { anzeigeZeit(it) } ?: "Zeitpunkt wählen",
                            beiDruck = {
                                zeitpunktWaehlen(zusammenhang) { iso -> terminZeit = iso }
                            },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                        if (terminZeit != null) {
                            Knopf("×", { terminZeit = null }, art = Knopfart.Leise, kompakt = true)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            aufschrift = "Vorschlagen",
                            beiDruck = {
                                val t = terminText.trim()
                                if (t.isNotEmpty()) {
                                    beiSenden(
                                        "Vorschlag für die nächste gemeinsame Schicht: $t",
                                        t,
                                        terminZeit,
                                    ) { ok -> if (ok) terminOffen = false }
                                }
                            },
                            art = Knopfart.Haupt,
                            aktiv = terminText.isNotBlank(),
                            kompakt = true,
                        )
                        Knopf("Abbrechen", { terminOffen = false }, art = Knopfart.Leise, kompakt = true)
                    }
                }
            }

            // ----------------------------------------------------------- Eingabe
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf(
                    aufschrift = "📅",
                    beiDruck = {
                        terminOffen = !terminOffen
                        terminText = ""
                        terminZeit = null
                    },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
                Feld(
                    wert = entwurf,
                    beiAenderung = { entwurf = it.take(500) },
                    platzhalter = "Nachricht …",
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    aufschrift = "Senden",
                    beiDruck = { senden() },
                    art = Knopfart.Haupt,
                    aktiv = entwurf.isNotBlank(),
                    kompakt = true,
                )
            }
        }
    }
}

/** Der Kopf des Gesprächs: zurück, wer, wo — und dazuschalten, wenn er fährt. */
@Composable
private fun Gespraechskopf(
    partner: Freund?,
    server: String,
    beiZurueck: () -> Unit,
    beiProfil: (String) -> Unit,
    beiDazuschalten: (Freund) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Knopf("‹", beiZurueck, art = Knopfart.Leise, kompakt = true)

        if (partner == null) {
            Text("Gespräch", style = Schrift.Titel, color = Farben.Text, modifier = Modifier.weight(1f))
            return@Row
        }

        // Das Wappen führt aufs Profil und trägt den grünen Ring, wenn der andere fährt.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = { beiProfil(partner.benutzername) },
                    role = Role.Button,
                    indication = null,
                    interactionSource = null,
                ),
        ) {
            Kontobild(
                kennung = partner.kennung,
                anzeigename = partner.anzeigename,
                wappen = partner.wappen,
                wappenfarbe = partner.wappenfarbe,
                bildAdresse = bildweg(server, partner.profilbild),
                imDienst = partner.anwesenheit != null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Personenname(
                    name = partner.anzeigename,
                    premium = partner.premium,
                    teammitglied = partner.teammitglied,
                    wachentag = partner.wachentag,
                    stil = Schrift.Gross,
                )
                Text(
                    text = lagezeile(partner.anwesenheit, partner.zuletztGesehen)
                        .ifBlank { partner.benutzername },
                    style = Schrift.MonoKlein,
                    color = if (partner.anwesenheit != null) Farben.GruenHell else Farben.TextSehrLeise,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        partner.anwesenheit?.let { wo ->
            Knopf(
                aufschrift = if (wo.platzFrei) "Dazuschalten" else "Runde voll",
                beiDruck = { beiDazuschalten(partner) },
                aktiv = wo.platzFrei,
                kompakt = true,
            )
        }
    }
}

/**
 * Eine Sprechblase — eigene rechts in Amber, fremde links auf Fläche.
 *
 * Ein Haken heißt zugestellt, zwei heißen gelesen — der zweite kommt live, sobald
 * der andere das Gespräch öffnet. Ob er kommt, entscheidet dessen Privatsphäre.
 */
@Composable
private fun Sprechblase(nachricht: Nachricht, eigene: Boolean) {
    Row(
        horizontalArrangement = if (eigene) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            horizontalAlignment = if (eigene) Alignment.End else Alignment.Start,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    if (eigene) Farben.Amber.copy(alpha = 0.14f) else Farben.Flaeche,
                    RoundedCornerShape(10.dp),
                )
                .border(
                    1.dp,
                    if (eigene) Farben.AmberTief else Farben.Rand,
                    RoundedCornerShape(10.dp),
                )
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            Text(text = nachricht.text, style = Schrift.Normal, color = Farben.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                SehrLeise(uhrzeitKurz(nachricht.gesendetUm), mono = true)
                if (eigene) {
                    Text(
                        text = if (nachricht.gelesenUm != null) "✓✓" else "✓",
                        style = Schrift.MonoKlein,
                        color = if (nachricht.gelesenUm != null) Farben.Amber else Farben.TextSehrLeise,
                    )
                }
            }
        }
    }
}

/** Ein Terminvorschlag bekommt seine eigene Karte statt einer Textblase. */
@Composable
private fun Terminkarte(nachricht: Nachricht, eigene: Boolean, beiTermin: (Long, Boolean) -> Unit) {
    val status = nachricht.terminStatus ?: "Offen"
    val farbe = when (status) {
        "Zugesagt" -> Farben.GruenHell
        "Abgesagt" -> Farben.SignalHell
        else -> Farben.AmberHell
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        if (eigene) Spacer(Modifier.weight(0.18f))
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .weight(0.82f)
                .flaeche(randfarbe = farbe.copy(alpha = 0.6f), ecke = 10.dp)
                .padding(Abstand.Normal),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "📅 TERMINVORSCHLAG",
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = farbe,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = when (status) {
                        "Zugesagt" -> "Zugesagt"
                        "Abgesagt" -> "Abgesagt"
                        else -> "Noch offen"
                    },
                    style = Schrift.MonoKlein,
                    color = farbe,
                )
            }
            Text(text = nachricht.terminText.orEmpty(), style = Schrift.Gross, color = Farben.Text)
            nachricht.terminZeitpunkt?.let { SehrLeise("Erinnerung: ${anzeigeZeit(it)}") }
            SehrLeise(uhrzeitKurz(nachricht.gesendetUm), mono = true)

            if (status == "Offen" && !eigene) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Zusagen", { beiTermin(nachricht.nr, true) }, art = Knopfart.Haupt, kompakt = true)
                    Knopf("Absagen", { beiTermin(nachricht.nr, false) }, art = Knopfart.Leise, kompakt = true)
                }
            } else if (status == "Offen") {
                SehrLeise("Wartet auf Antwort.")
            }
        }
        if (!eigene) Spacer(Modifier.weight(0.18f))
    }
}

/**
 * Ein Geschenk — solange es zu ist, steht nicht drin, was drin ist. Erst der
 * Knopf verrät es.
 */
@Composable
private fun Geschenkkarte(
    nachricht: Nachricht,
    eigene: Boolean,
    oeffnet: Boolean,
    fehler: String?,
    beiOeffnen: () -> Unit,
) {
    val geschenk = nachricht.geschenk ?: return
    Row(modifier = Modifier.fillMaxWidth()) {
        if (eigene) Spacer(Modifier.weight(0.18f))
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .weight(0.82f)
                .flaeche(
                    randfarbe = if (geschenk.geoeffnet) Farben.Rand else Farben.AmberTief,
                    ecke = 10.dp,
                )
                .padding(Abstand.Normal),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🎁 GESCHENK",
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.AmberHell,
                    modifier = Modifier.weight(1f),
                )
                if (geschenk.geoeffnet) SehrLeise("geöffnet", mono = true)
            }
            Text(
                text = when {
                    geschenk.geoeffnet -> geschenk.artikelName.orEmpty()
                    geschenk.fuerMich -> "Du hast ein Geschenk erhalten."
                    else -> "Noch nicht geöffnet."
                },
                style = Schrift.Normal,
                color = Farben.Text,
            )
            SehrLeise(uhrzeitKurz(nachricht.gesendetUm), mono = true)
            if (geschenk.fuerMich && !geschenk.geoeffnet) {
                Knopf(
                    aufschrift = if (oeffnet) "Wird geöffnet …" else "Öffnen",
                    beiDruck = beiOeffnen,
                    art = Knopfart.Haupt,
                    aktiv = !oeffnet,
                    kompakt = true,
                )
            }
            if (fehler != null) {
                Text(text = fehler, style = Schrift.MonoKlein, color = Farben.SignalHell)
            }
        }
        if (!eigene) Spacer(Modifier.weight(0.18f))
    }
}

/** Ein ISO-Zeitpunkt als „14.09.2026, 20:00". */
private fun anzeigeZeit(iso: String): String =
    zeitLesen(iso)?.let { ERINNERUNG.format(it.atZone(ZoneId.systemDefault())) } ?: iso

/**
 * Datum, dann Uhrzeit — die beiden Wähler des Systems nacheinander.
 *
 * Wie `datetime-local` im Web: ein Zeitpunkt in der Zone des Geräts, geschickt
 * als ISO mit Versatz, damit der Server weiß, welche Stunde gemeint war.
 */
private fun zeitpunktWaehlen(zusammenhang: android.content.Context, beiWahl: (String) -> Unit) {
    val jetzt = LocalDateTime.now().plusHours(1).withMinute(0)
    DatePickerDialog(
        zusammenhang,
        { _, jahr, monat, tag ->
            TimePickerDialog(
                zusammenhang,
                { _, stunde, minute ->
                    val gewaehlt = LocalDateTime.of(jahr, monat + 1, tag, stunde, minute)
                    beiWahl(gewaehlt.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString())
                },
                jetzt.hour,
                jetzt.minute,
                true,
            ).show()
        },
        jetzt.year,
        jetzt.monthValue - 1,
        jetzt.dayOfMonth,
    ).show()
}
