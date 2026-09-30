package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Buchdaten
import de.pagerspass.pagerspass.netz.Abzeichen
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Schicht
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Markenzahl
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.schmuck.Profilzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Die fünf übrigen Wege der Tableiste, angebunden.
 *
 * <b>Jede Seite lädt beim Öffnen und nur, was fehlt.</b> Der Ladeaufruf steht in
 * einem `LaunchedEffect` und die Sitzung bricht ab, wenn schon etwas dasteht —
 * sonst holt jeder Wechsel zwischen zwei Reitern der Leiste alles neu.
 *
 * <b>Die Seiten kennen die Sitzung nicht.</b> Sie bekommen ihren Bereich und
 * zwei Rückrufe. Das ist dieselbe Trennung wie beim Startbildschirm: Was der
 * Server liefert und was die Seite zeigt, sind zwei Dinge, und die Naht dazwischen
 * gehört an eine Stelle (`PagerSpassApp`).
 */

// ---------------------------------------------------------------- Dienstbuch

/**
 * Das Dienstbuch — die Schichten, die man gefahren ist.
 *
 * <b>Der Stufenbalken oben ist der des Kontos</b>, nicht ein zweiter. Er stand
 * hier bis eben mit festen Zahlen („noch 215 bis Stufe 13") und zeigte damit
 * jedem Konto denselben Fortschritt — die auffälligste Art, eine Seite fertig
 * aussehen zu lassen, die es nicht ist.
 */
@Composable
fun DienstbuchSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    buch: Bereichsstand<Buchdaten> = Bereichsstand(),
    beiLaden: () -> Unit = {},
    beiGarage: () -> Unit = {},
    beiBestenliste: () -> Unit = {},
    beiDienst: () -> Unit = {},
) {
    var reiter by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(Unit) { beiLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(titel = "Dienstbuch", unterzeile = "Was du gefahren bist")

        if (konto != null) {
            Kasten {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Stufe ${konto.level}", style = Schrift.Gross, color = Farben.Text)
                    Text(
                        text = "· ${konto.rang}",
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Marke("${zahl(konto.erfahrung)} XP", farbe = Farben.AmberHell)
                }
                Fortschritt(anteil = konto.stufenanteil, text = konto.stufentext)
            }
        }

        Reiterreihe {
            listOf("Übersicht", "Schichten", "Abzeichen").forEachIndexed { nr, wort ->
                Reiter(wort, offen = reiter == nr, beiDruck = { reiter = nr })
            }
        }

        Bereich(
            laedt = buch.laedt,
            fehler = buch.fehler,
            inhalt = buch.inhalt,
            beiErneut = beiLaden,
        ) { daten ->
            when (reiter) {
                0 -> Uebersicht(daten, beiDienst)
                1 -> Schichtenliste(daten.schichten, beiDienst)
                else -> Abzeichenliste(daten.abzeichen)
            }
        }

        Abschnitt("Weiter") {
            Wegzeile(
                titel = "Garage",
                unterzeile = "Deine Fahrzeuge und wo sie stehen",
                zeichen = Zeichen.Fahrzeug,
                beiDruck = beiGarage,
            )
            Wegzeile(
                titel = "Bestenliste",
                unterzeile = "Wo du zwischen den anderen stehst",
                zeichen = Zeichen.Uebung,
                beiDruck = beiBestenliste,
            )
        }
    }
}

/**
 * Die Übersicht — was aus den Schichten zu rechnen ist.
 *
 * <b>Vier Zahlen und die letzte Schicht.</b> Sie stehen hier und nicht auf dem
 * Reiter „Schichten", weil sie eine andere Frage beantworten: Dort steht, *was*
 * man gefahren ist; hier, *wie viel*.
 */
@Composable
private fun Uebersicht(daten: Buchdaten, beiDienst: () -> Unit) {
    if (daten.schichten.isEmpty()) {
        Leerhinweis(
            "Noch nichts im Buch. Nach deinem ersten Dienstende steht die erste Schicht hier.",
        ) {
            Knopf("Dienst aufnehmen", beiDienst, art = Knopfart.Haupt)
        }
        return
    }

    val einsaetze = daten.schichten.sumOf { it.einsaetze }
    val punkte = daten.schichten.sumOf { it.punkte }
    val fristen = daten.schichten.mapNotNull { it.hilfsfristSekunden }
    val schnitt = if (fristen.isEmpty()) null else fristen.average().toInt()

    Kasten(abstandInnen = Abstand.Klein) {
        Kennzahl("Schichten", daten.schichten.size.toString())
        Kennzahl("Einsätze", zahl(einsaetze))
        Kennzahl("Punkte", zahl(punkte))
        Kennzahl("Hilfsfrist im Schnitt", hilfsfrist(schnitt) ?: "—")
        Kennzahl(
            "Abzeichen",
            "${daten.abzeichen.count { it.erreicht }} von ${daten.abzeichen.size}",
        )
    }

    Abschnitt("Zuletzt") {
        daten.schichten.take(3).forEach { Schichtzeile(it) }
    }
}

/** Eine Zeile aus Etikett und Wert. */
@Composable
private fun Kennzahl(was: String, wert: String) {
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
        Text(text = wert, style = Schrift.MonoNormal, color = Farben.Text)
    }
}

@Composable
private fun Schichtenliste(schichten: List<Schicht>, beiDienst: () -> Unit) {
    if (schichten.isEmpty()) {
        Leerhinweis("Noch keine Schicht gefahren.") {
            Knopf("Dienst aufnehmen", beiDienst, art = Knopfart.Haupt)
        }
        return
    }
    schichten.forEach { Schichtzeile(it) }
}

/**
 * Eine Schicht im Buch.
 *
 * Ort und Kreis oben, darunter in Monospace, was Technik dazu schreibt: Datum,
 * Rufname, Einsätze, Hilfsfrist. Rechts die Punkte — sie sind der Grund, warum
 * jemand die Zeile überhaupt sucht.
 */
@Composable
private fun Schichtzeile(schicht: Schicht) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 9.dp)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = listOfNotNull(schicht.ort.ifBlank { null }, schicht.landkreis)
                    .joinToString(" · ")
                    .ifBlank { schicht.roomCode },
                style = Schrift.Normal,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SehrLeise(
                text = listOfNotNull(
                    zeitpunkt(schicht.beendetUm),
                    schicht.funkrufname,
                    "${schicht.einsaetze} Einsätze",
                    hilfsfrist(schicht.hilfsfristSekunden)?.let { "HF $it" },
                ).joinToString(" · "),
                mono = true,
            )
        }
        Marke(
            text = "${if (schicht.punkte >= 0) "+" else ""}${schicht.punkte}",
            farbe = if (schicht.punkte >= 0) Farben.GruenHell else Farben.SignalHell,
        )
    }
}

/**
 * Die Meilensteine.
 *
 * <b>Erreichte zuerst.</b> Wer die Seite öffnet, will sehen, was er hat — nicht,
 * was ihm fehlt. Die offenen stehen darunter und bleiben lesbar: Ein
 * ausgegrauter Meilenstein, den man nicht mehr entziffern kann, ist kein Ziel.
 */
@Composable
private fun Abzeichenliste(abzeichen: List<Abzeichen>) {
    if (abzeichen.isEmpty()) {
        Leerhinweis("Noch keine Meilensteine erreicht.")
        return
    }

    val (erreicht, offen) = abzeichen.partition { it.erreicht }

    Kasten(abstandInnen = Abstand.Klein) {
        Kennzahl("Erreicht", "${erreicht.size} von ${abzeichen.size}")
    }

    if (erreicht.isNotEmpty()) {
        Abschnitt("Erreicht") { erreicht.forEach { Abzeichenzeile(it) } }
    }

    if (offen.isNotEmpty()) {
        Abschnitt("Als Nächstes") {
            // <b>Gedeckelt, und der Deckel steht dabei.</b> Der Server liefert
            // 991 Meilensteine — jede Stufe jedes Grundes einzeln
            // („Einsätze abgeschlossen ×1", „×2", „×5", …). Sie alle zu setzen
            // heißt: 991 Zeilen in einer rollenden Spalte, die Compose
            // vollständig aufbaut, bevor die erste sichtbar wird. Gemessen ist
            // das eine Seite, die Sekunden braucht und danach ruckelt.
            //
            // Gezeigt werden die ersten dreißig — und darunter steht, wie viele
            // es sonst noch sind. Ein stiller Schnitt wäre die schlechtere
            // Lösung: Er sähe aus, als gäbe es nicht mehr.
            offen.take(OFFENE_ABZEICHEN).forEach { Abzeichenzeile(it) }

            if (offen.size > OFFENE_ABZEICHEN) {
                SehrLeise("… und ${zahl(offen.size - OFFENE_ABZEICHEN)} weitere Meilensteine")
            }
        }
    }
}

/** Wie viele offene Meilensteine die Liste zeigt — siehe die Begründung oben. */
private const val OFFENE_ABZEICHEN = 30

@Composable
private fun Abzeichenzeile(abzeichen: Abzeichen) {
    val farbe = if (abzeichen.erreicht) Farben.Text else Farben.TextLeise

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                ecke = 9.dp,
                randfarbe = if (abzeichen.erreicht) Farben.AmberTief else Farben.Rand,
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(text = abzeichen.titel, style = Schrift.Normal, color = farbe)
            SehrLeise(abzeichen.beschreibung)
            if (abzeichen.erreichtAm != null) SehrLeise(tag(abzeichen.erreichtAm), mono = true)
        }
        if (abzeichen.erreicht) Marke("Erreicht", farbe = Farben.GruenHell)
    }
}

// -------------------------------------------------------------------- Wache

/**
 * Die Wache.
 *
 * <b>Entweder die eigene oder die Suche</b> — nie beides. Wer in einer
 * Gemeinschaft ist, will hinein; wer in keiner ist, zur Liste. Dieselbe Weiche
 * wie in der Tableiste des Webs: Derselbe Knopf soll auf beiden Zweigen
 * denselben Weg nehmen.
 */
// ------------------------------------------------------------------ Freunde

/**
 * Freunde — Liste, Anfragen, Vorschläge.
 *
 * <b>Eine Liste trägt alle drei Reiter.</b> Der Server liefert sie in einem Zug;
 * `stand` sagt, wohin jemand gehört, und bei einer offenen Anfrage entscheidet
 * `vonMir`, ob man wartet oder am Zug ist. Genau dieser Unterschied macht den
 * Reiter „Anfragen" nützlich: Nur die eine Hälfte davon kann man beantworten.
 */
@Composable
fun FreundeSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    freunde: Bereichsstand<List<Freund>> = Bereichsstand(),
    server: String = "",
    beiLaden: () -> Unit = {},
    beiAntwort: (String, Boolean) -> Unit = { _, _ -> },
    /** Ein Tipp auf einen bestätigten Freund öffnet das Gespräch. */
    beiGespraech: (Freund) -> Unit = {},
    brett: @Composable ColumnScope.() -> Unit = {},
) {
    var reiter by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(Unit) { beiLaden() }

    val alle = freunde.inhalt.orEmpty()
    val liste = alle.filter { it.bestaetigt }
    val anfragen = alle.filter { it.angefragt }
    val zuBeantworten = anfragen.count { !it.vonMir }
    val ungelesen = liste.sumOf { it.ungelesen }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(titel = "Freunde", unterzeile = "Brett, Gespräche und wer mit dir fährt")

        Reiterreihe {
            Reiter("Brett", offen = reiter == 0, beiDruck = { reiter = 0 })
            Reiter(
                "Freunde",
                offen = reiter == 1,
                beiDruck = { reiter = 1 },
                marke = ungelesen,
            )
            Reiter(
                "Anfragen",
                offen = reiter == 2,
                beiDruck = { reiter = 2 },
                marke = zuBeantworten,
            )
        }

        if (reiter == 0) {
            brett()
            return@Seite
        }

        Bereich(
            laedt = freunde.laedt,
            fehler = freunde.fehler,
            inhalt = freunde.inhalt,
            beiErneut = beiLaden,
        ) {
            if (reiter == 1) {
                if (liste.isEmpty()) {
                    Leerhinweis(
                        "Noch niemand auf der Liste. Im Web kannst du jemanden über seinen " +
                            "Benutzernamen suchen.",
                    )
                } else {
                    liste.forEach { freund ->
                        Freundzeile(freund, server, beiDruck = { beiGespraech(freund) })
                    }
                }
            } else {
                if (anfragen.isEmpty()) {
                    Leerhinweis("Keine offenen Anfragen.")
                } else {
                    anfragen.forEach { Freundzeile(it, server, beiAntwort) }
                }
            }
        }
    }
}

@Composable
private fun Freundzeile(
    freund: Freund,
    server: String,
    beiAntwort: ((String, Boolean) -> Unit)? = null,
    beiDruck: (() -> Unit)? = null,
) {
    val offenVonAnderen = freund.angefragt && !freund.vonMir

    Profilzeile(
        beiDruck = beiDruck,
        kennung = freund.kennung,
        anzeigename = freund.anzeigename.ifBlank { freund.benutzername },
        // Wer gerade fährt, ist die interessantere Auskunft als „zuletzt online"
        // — sie verdrängt diese, statt danebenzustehen.
        unterzeile = listOfNotNull(
            "Stufe ${freund.level}",
            freund.rang.ifBlank { null },
            freund.anwesenheit?.ansage
                ?: seither(freund.zuletztGesehen)?.let { "zuletzt $it" },
        ).joinToString(" · "),
        wappen = freund.wappen,
        wappenfarbe = freund.wappenfarbe,
        kopfmuster = freund.kopfmuster,
        profilrahmen = freund.profilrahmen,
        bildAdresse = bildweg(server, freund.profilbild),
        premium = freund.premium,
        teammitglied = freund.teammitglied,
        imDienst = freund.anwesenheit != null,
        hinten = {
            when {
                freund.ungelesen > 0 -> Markenzahl(freund.ungelesen)
                freund.angefragt && freund.vonMir -> Marke("Gesendet")
                freund.anwesenheit != null -> Marke("Im Dienst", farbe = Farben.GruenHell)
            }
        },
        unten = {
            if (offenVonAnderen && beiAntwort != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        "Annehmen",
                        { beiAntwort(freund.kennung, true) },
                        art = Knopfart.Haupt,
                        kompakt = true,
                    )
                    Knopf(
                        "Ablehnen",
                        { beiAntwort(freund.kennung, false) },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }
        },
    )
}

// Shop und Konto stehen in `ShopSeiten.kt` und `KontoSeiten.kt` — sie sind
// mit Premium, Postfach und den Nebenwegen des Kontos zu groß für diese Sammlung
// geworden.
