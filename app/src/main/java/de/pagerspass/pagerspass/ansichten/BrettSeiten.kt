package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Bretteintrag
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.netz.Schicht
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
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
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Das Brett — die Startseite des Freundebereichs (`views/freunde/BrettView.vue`).
 *
 * Was hier steht, ist nicht getippt, sondern gefahren: beendete Schichten,
 * Beförderungen, Abzeichen, Bestmarken, die Stufe der eigenen Wache. Dazwischen
 * die Beiträge, die jemand selbst geschrieben hat.
 *
 * <b>Streng nach Zeit, kein sortierender Algorithmus.</b> Was es stattdessen
 * gibt, ist ein Sieb, das man selbst in der Hand hält: der Kreis (Freunde,
 * Wache, Öffentlich) kommt vom Server, die Art (Beiträge, Erfolge, Schichten)
 * wird hier gefiltert. Beides ist erklärbar — man sieht, was man eingestellt hat.
 */
@Composable
fun ColumnScope.BrettInhalt(
    konto: Konto?,
    meinProfil: Profil?,
    server: String,
    imDienst: List<Freund>,
    eintraege: List<Bretteintrag>,
    reiter: String,
    kannMehr: Boolean,
    laeuft: Boolean,
    istGeladen: Boolean,
    hatWache: Boolean,
    darfOeffentlich: Boolean,
    schichten: List<Schicht>,
    meldung: String?,
    beiMeldungWeg: () -> Unit,
    beiReiter: (String) -> Unit,
    beiMehr: () -> Unit,
    beiSchreiben: (text: String, sichtbarkeit: String, roomCode: String?) -> Unit,
    beiQuittieren: (Long) -> Unit,
    beiEntfernen: (Long) -> Unit,
    beiMelden: (Long) -> Unit,
    beiKommentare: (Long) -> Unit,
    beiProfil: (String) -> Unit,
    beiBezug: (art: String, id: String) -> Unit,
    beiDazuschalten: (Freund) -> Unit,
    beiHausordnung: () -> Unit,
) {
    // Ohne Konto gibt es kein Brett.
    if (konto == null) {
        Leerhinweis("Das Brett gehört zu einem Konto. Es entsteht beim ersten Dienst von selbst.")
        return
    }

    ImDienstLeiste(freunde = imDienst, server = server, beiProfil = beiProfil, beiDazuschalten = beiDazuschalten)

    Verfassen(
        konto = konto,
        meinProfil = meinProfil,
        server = server,
        vorgabe = meinProfil?.standardsichtbarkeit ?: "Freunde",
        hatWache = hatWache,
        darfOeffentlich = darfOeffentlich,
        schichten = schichten,
        laeuft = laeuft,
        beiSenden = beiSchreiben,
        beiHausordnung = beiHausordnung,
    )

    // Zwei Schalter, zwei Fragen: „von wem" und „was". Zusammen ergeben sie
    // einen Satz — Freunde × Erfolge ist der Ausschnitt, den man am häufigsten sucht.
    val kreise = buildList {
        add("Freunde")
        if (hatWache) add("Wache")
        add("Alle")
    }
    Segment(
        seiten = kreise,
        gewaehlt = reiter,
        beiWahl = beiReiter,
        // „Öffentlich" und nicht „Alle": daneben steht das Sieb, dessen erste
        // Stellung „Alles" heißt — zwei Schalter mit fast demselben Wort.
        aufschrift = { if (it == "Alle") "Öffentlich" else it },
        modifier = Modifier.fillMaxWidth(),
    )

    var sieb by rememberSaveable { mutableStateOf("Alles") }
    Segment(
        seiten = SIEBE,
        gewaehlt = sieb,
        beiWahl = { sieb = it },
        aufschrift = { if (it == "Beitraege") "Beiträge" else it },
        modifier = Modifier.fillMaxWidth(),
    )

    Meldungsstreifen(meldung, beiMeldungWeg)

    // Gesiebt wird, was schon da ist, und nicht neu geholt: Der Server kennt
    // den Ausschnitt nicht, und ein eigener Abruf je Art wäre eine zweite
    // Wahrheit über dieselbe Liste.
    val gesiebt = when (sieb) {
        "Beitraege" -> eintraege.filter { it.art == "Beitrag" }
        "Erfolge" -> eintraege.filter { it.art in ERFOLGE }
        "Schichten" -> eintraege.filter { it.art == "Schicht" }
        else -> eintraege
    }

    if (gesiebt.isEmpty()) {
        if (laeuft && !istGeladen) {
            FreundeLaden()
        } else if (!laeuft) {
            Leerhinweis(
                when {
                    eintraege.isNotEmpty() ->
                        "In diesem Ausschnitt steht nichts. Stell das Sieb auf „Alles“."
                    reiter == "Wache" && hatWache -> "Auf eurer Wache war noch nichts los."
                    reiter == "Wache" -> "Du bist in keiner Wachengemeinschaft."
                    reiter == "Alle" -> "Noch hat niemand öffentlich etwas angeschlagen."
                    else -> "Hier steht, was deine Freunde im Dienst erlebt haben. Fahr eine " +
                        "Schicht oder schreib die erste Zeile."
                },
            )
        }
    }

    gesiebt.forEach { e ->
        EintragKarte(
            eintrag = e,
            server = server,
            beiQuittieren = { beiQuittieren(e.nr) },
            beiKommentare = { beiKommentare(e.nr) },
            beiEntfernen = { beiEntfernen(e.nr) },
            beiMelden = { beiMelden(e.nr) },
            beiProfil = beiProfil,
            beiBezug = beiBezug,
        )
    }

    // „Mehr laden" bleibt sichtbar, auch wenn das Sieb gerade alles zurückhält:
    // Nachschub kann Zeilen bringen, die durchkommen.
    if (kannMehr) {
        Knopf("Mehr laden", beiMehr, art = Knopfart.Leise, aktiv = !laeuft, breit = true)
    }
}

private val SIEBE = listOf("Alles", "Beitraege", "Erfolge", "Schichten")

private val ERFOLGE = setOf(
    "Befoerderung",
    "Abzeichen",
    "Rekord",
    "Wachenstufe",
    "Gemeinschaftsbeitritt",
)

// ----------------------------------------------------------- Jetzt im Dienst

/**
 * Wer von den Freunden gerade Dienst hat — die Leiste ganz oben am Brett.
 *
 * Seitwärts rollend statt umbrechend: Bei acht Freunden im Dienst wäre der halbe
 * erste Bildschirm eine Kachelwand, bevor die erste Zeile am Brett steht.
 */
@Composable
private fun ImDienstLeiste(
    freunde: List<Freund>,
    server: String,
    beiProfil: (String) -> Unit,
    beiDazuschalten: (Freund) -> Unit,
) {
    if (freunde.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dienstpunkt()
            Ueberschrift("Jetzt im Dienst")
            SehrLeise(freunde.size.toString(), mono = true)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            freunde.forEach { f ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier
                        .width(190.dp)
                        .clip(Rundung.Normal)
                        .background(Farben.Flaeche)
                        .kopfband(f.kopfmuster, Wappen.ton(f.kennung, f.wappenfarbe), zeile = true)
                        .border(1.dp, Farben.Rand, Rundung.Normal)
                        .padding(Abstand.Normal),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = { beiProfil(f.benutzername) },
                                role = Role.Button,
                                indication = null,
                                interactionSource = null,
                            ),
                    ) {
                        Kontobild(
                            kennung = f.kennung,
                            anzeigename = f.anzeigename,
                            wappen = f.wappen,
                            wappenfarbe = f.wappenfarbe,
                            bildAdresse = bildweg(server, f.profilbild),
                            imDienst = true,
                        )
                        Personenname(
                            name = f.anzeigename,
                            premium = f.premium,
                            teammitglied = f.teammitglied,
                            wachentag = f.wachentag,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = lagezeile(f.anwesenheit, f.zuletztGesehen),
                        style = Schrift.MonoKlein,
                        color = Farben.TextLeise,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val frei = f.anwesenheit?.platzFrei == true
                    Knopf(
                        aufschrift = if (frei) "Dazuschalten" else "Voll",
                        beiDruck = { beiDazuschalten(f) },
                        aktiv = frei,
                        kompakt = true,
                        breit = true,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Verfassen

/**
 * Das Feld, in das man einen Beitrag schreibt — oben am Brett, fest in der Seite.
 *
 * Es klappt erst auf, wenn jemand hineintippt. Vorher nahm es mit Feld,
 * Sichtbarkeit, Schicht, Zähler, Knopf und Hausordnung die halbe Höhe eines
 * Handybildschirms ein — bei jedem Besuch, während geschrieben wird an den
 * wenigsten.
 *
 * <b>Die Sichtbarkeit wird vor dem Schreiben gewählt</b> und steht sichtbar
 * daneben; sie startet mit der Standardsichtbarkeit aus dem eigenen Profil.
 * „Alle" gibt es erst nach der ersten gefahrenen Schicht — ein Eintrag, den der
 * Server danach still zurückstuft, wäre eine Lüge auf dem Bildschirm.
 */
@Composable
private fun Verfassen(
    konto: Konto,
    meinProfil: Profil?,
    server: String,
    vorgabe: String,
    hatWache: Boolean,
    darfOeffentlich: Boolean,
    schichten: List<Schicht>,
    laeuft: Boolean,
    beiSenden: (String, String, String?) -> Unit,
    beiHausordnung: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var offen by rememberSaveable { mutableStateOf(false) }
    var sichtbarkeit by rememberSaveable(vorgabe) { mutableStateOf(vorgabe) }
    var angehaengt by rememberSaveable { mutableStateOf<String?>(null) }

    val kreise = buildList {
        add("Freunde")
        if (hatWache) add("Wache")
        if (darfOeffentlich) add("Oeffentlich")
    }
    val gewaehlt = if (sichtbarkeit in kreise) sichtbarkeit else "Freunde"
    val ausgeklappt = offen || text.isNotEmpty()
    val uebrig = HOECHSTENS - text.trim().length
    val bereit = text.trim().isNotEmpty() && uebrig >= 0

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = if (ausgeklappt) Farben.RandHell else Farben.Rand)
            .padding(horizontal = if (ausgeklappt) Abstand.Gross else Abstand.Normal, vertical = Abstand.Normal),
    ) {
        if (ausgeklappt) Etikett("Was war los?")

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Kontobild(
                kennung = konto.kennung,
                anzeigename = konto.anzeigename,
                wappen = meinProfil?.wappen ?: "Keines",
                wappenfarbe = meinProfil?.wappenfarbe ?: 0,
                bildAdresse = bildweg(server, meinProfil?.profilbild),
            )
            Feld(
                wert = text,
                beiAenderung = { text = it.take(HOECHSTENS + 200) },
                platzhalter = "Eine Zeile aus deiner Schicht …",
                einzeilig = !ausgeklappt,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { if (it.hasFocus) offen = true },
            )
        }

        if (!ausgeklappt) return@Column

        Segment(
            seiten = kreise,
            gewaehlt = gewaehlt,
            beiWahl = { sichtbarkeit = it },
            aufschrift = {
                when (it) {
                    "Wache" -> "+ Wache"
                    "Oeffentlich" -> "Alle"
                    else -> it
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (schichten.isNotEmpty()) {
            SehrLeise("Schicht")
            Pillenreihe {
                Pille("nicht anhängen", an = angehaengt == null, beiDruck = { angehaengt = null })
                schichten.take(10).forEach { s ->
                    val datum = kurzdatum(s.beendetUm).take(5)
                    Pille(
                        aufschrift = s.ort + if (datum.isNotBlank()) " · $datum" else "",
                        an = angehaengt == s.roomCode,
                        beiDruck = { angehaengt = s.roomCode },
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Der Zähler erscheint erst, wenn es eng wird.
            if (uebrig < 100) {
                Text(
                    text = uebrig.toString(),
                    style = Schrift.MonoKlein,
                    color = if (uebrig < 0) Farben.SignalHell else Farben.TextSehrLeise,
                )
            }
            Spacer(Modifier.weight(1f))
            Knopf(
                aufschrift = "Anschlagen",
                beiDruck = {
                    if (bereit) {
                        beiSenden(text.trim(), gewaehlt, angehaengt)
                        text = ""
                        angehaengt = null
                        offen = false
                    }
                },
                art = Knopfart.Haupt,
                aktiv = bereit && !laeuft,
                kompakt = true,
            )
        }

        SehrLeise("Am Brett gilt die Hausordnung:")
        Textweg("keine Links, keine Werbung, keine fremden Daten.", beiHausordnung)
    }
}

private const val HOECHSTENS = 500

// ---------------------------------------------------------------- Die Karte

/** Woher eine Zeile kommt: Zeichen, Wort und Farbe. Ein Beitrag bekommt keine. */
private data class Eintragsart(val zeichen: String, val wort: String, val farbe: Color)

private fun eintragsart(art: String): Eintragsart? = when (art) {
    "Schicht" -> Eintragsart("🚒", "Schicht", Farben.Blau)
    "Befoerderung" -> Eintragsart("🎖", "Beförderung", Farben.Violett)
    "Abzeichen" -> Eintragsart("🏅", "Abzeichen", Farben.Amber)
    "Rekord" -> Eintragsart("⏱", "Bestmarke", Farben.Gruen)
    "Wachenstufe" -> Eintragsart("🏠", "Wache", Farben.FmsAnfahrt)
    "Gemeinschaftsbeitritt" -> Eintragsart("🤝", "Beigetreten", Farben.FmsAnfahrt)
    else -> null
}

/**
 * Eine Zeile am Brett: wer, was, wann — und was man damit tun kann
 * (`components/freunde/EintragKarte.vue`).
 *
 * Woher die Zeile kommt, sagt die Karte in drei Stufen: der Farbstreifen links,
 * das Zeichen und das Wort daneben. An eigenen Zeilen steht, wer sie lesen darf,
 * und der Weg ins eigene Dienstbuch; an fremden „Melden" statt „Zurücknehmen".
 * Beides fragt im selben Knopf nach — kein Dialog über der Seite.
 *
 * @param einzeln Auf der Einzelseite gibt es keinen Weg „zu den Kommentaren" —
 *   man ist schon da.
 */
@Composable
fun EintragKarte(
    eintrag: Bretteintrag,
    server: String,
    beiQuittieren: () -> Unit,
    beiKommentare: () -> Unit,
    beiEntfernen: () -> Unit,
    beiMelden: () -> Unit,
    beiProfil: (String) -> Unit,
    beiBezug: (art: String, id: String) -> Unit,
    modifier: Modifier = Modifier,
    einzeln: Boolean = false,
) {
    val art = eintragsart(eintrag.art)
    var gefragt by remember(eintrag.nr) { mutableStateOf<String?>(null) }
    val form = RoundedCornerShape(14.dp)
    val v = eintrag.verfasser

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .fillMaxWidth()
            .clip(form)
            .flaeche(randfarbe = if (eintrag.vonMir) Farben.AmberTief else Farben.Rand)
            .drawWithContent {
                drawContent()
                if (art != null) drawRect(art.farbe, size = size.copy(width = 3.dp.toPx()))
            }
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        // Ein gelöschtes Konto hat kein Profil mehr, aber seine Zeile steht noch.
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
            ) {
                Kontobild(
                    kennung = v.kennung,
                    anzeigename = v.anzeigename,
                    wappen = v.wappen ?: "Keines",
                    wappenfarbe = v.wappenfarbe,
                    bildAdresse = bildweg(server, v.profilbild),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Personenname(
                        name = v.anzeigename,
                        premium = v.premium,
                        teammitglied = v.teammitglied,
                        wachentag = v.wachentag,
                    )
                    if (v.benutzername.isNotBlank()) {
                        Text(
                            text = v.benutzername,
                            style = Schrift.MonoKlein,
                            color = Farben.TextSehrLeise,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                // Wer darf das lesen — nur an den eigenen Zeilen.
                if (eintrag.vonMir) {
                    Marke(
                        when (eintrag.sichtbarkeit) {
                            "Oeffentlich" -> "Öffentlich"
                            "Wache" -> "Wache"
                            else -> "Freunde"
                        },
                    )
                }
                SehrLeise(wieLangeHer(eintrag.erstelltUm), mono = true)
            }
        }

        if (art != null) {
            Text(
                text = "${art.zeichen} ${art.wort}",
                style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                color = art.farbe,
            )
        }

        eintrag.titel?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = Schrift.Gross, color = Farben.Text)
        }
        eintrag.text?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = Schrift.Lesetext, color = Farben.Text)
        }

        // Der Weg vom Brett ins eigene Dienstbuch — nur an den eigenen Zeilen.
        val bezugId = eintrag.bezugId
        val bezugArt = eintrag.bezugArt
        if (eintrag.vonMir && bezugId != null && bezugArt != null) {
            val text = when (bezugArt) {
                "Schicht" -> "Zur Nachbesprechung →"
                "Abzeichen" -> "Zu den Abzeichen →"
                "Gemeinschaft" -> "Zur Wache →"
                else -> null
            }
            if (text != null) Textweg(text, { beiBezug(bezugArt, bezugId) })
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Eine Reaktion, und sie heißt, wie sie im Funk heißt.
            Knopf(
                aufschrift = "✓✓ Quittiert" + if (eintrag.quittungen > 0) " ${eintrag.quittungen}" else "",
                beiDruck = beiQuittieren,
                art = if (eintrag.vonMirQuittiert) Knopfart.Normal else Knopfart.Leise,
                kompakt = true,
            )
            if (!einzeln) {
                Knopf(
                    aufschrift = "💬 Kommentare" + if (eintrag.kommentare > 0) " ${eintrag.kommentare}" else "",
                    beiDruck = beiKommentare,
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
            // Das Aufräumen steht zuletzt und für sich: der seltenste Griff der
            // Karte und der einzige, der etwas wegnimmt.
            if (eintrag.vonMir) {
                Knopf(
                    aufschrift = if (gefragt == "entfernen") "Wirklich zurücknehmen?" else "Zurücknehmen",
                    beiDruck = {
                        if (gefragt != "entfernen") {
                            gefragt = "entfernen"
                        } else {
                            gefragt = null
                            beiEntfernen()
                        }
                    },
                    art = if (gefragt == "entfernen") Knopfart.Gefahr else Knopfart.Leise,
                    kompakt = true,
                )
            } else {
                Knopf(
                    aufschrift = if (gefragt == "melden") "Wirklich melden?" else "Melden",
                    beiDruck = {
                        if (gefragt != "melden") {
                            gefragt = "melden"
                        } else {
                            gefragt = null
                            beiMelden()
                        }
                    },
                    art = if (gefragt == "melden") Knopfart.Gefahr else Knopfart.Leise,
                    kompakt = true,
                )
            }
        }
    }
}
