package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Wachendaten
import de.pagerspass.pagerspass.mobil.Wachenstand
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.GemeinschaftDetail
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Wachenartikel
import de.pagerspass.pagerspass.netz.Wachenplatz
import de.pagerspass.pagerspass.netz.Wachenschatz
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die beiden Unterseiten des Wachenbereichs: die Rangliste der Wachen und der
 * Wachen-Shop. Übertragen aus `WachenranglisteView.vue`, `WachenShopView.vue`
 * und `components/gemeinschaft/WachenShop.vue`.
 */

// ----------------------------------------------------------------- Rangliste

/**
 * Die Rangliste der Wachengemeinschaften.
 *
 * Sortiert nach gesammelter Erfahrung, nicht nach Aktivität: Die Stufe ist das,
 * worauf eine Wache hinarbeitet. Die Aktivität der letzten 30 Tage steht
 * daneben — sie beantwortet die andere Frage: wer fährt gerade. Die eigene
 * Wache steht immer da, auch ohne Platz, sonst suchte man sich dumm nach ihr.
 */
@Composable
fun WachenranglisteSeite(
    griffe: WachenGriffe,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    wachen: Wachenstand = Wachenstand(),
    hatWache: Boolean = false,
) {
    LaunchedEffect(Unit) { griffe.speicher.ranglisteLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
            Etikett("Wachengemeinschaften")
            Seitenkopf(titel = "Rangliste")
        }

        WachenNavigation(
            hier = "rangliste",
            hatWache = hatWache,
            beiWache = griffe.zurWache,
            beiRangliste = {},
            beiShop = griffe.zumShop,
        )

        Leise(
            "Eine Wache sammelt, was ihre Mitglieder verdienen. Sortiert nach der Gesamterfahrung; " +
                "„30 Tage“ zeigt, wer gerade fährt.",
        )

        wachen.meldung?.let { Meldungszeile(it) }

        val stand = wachen.rangliste
        Bereich(
            laedt = stand.laedt,
            fehler = stand.fehler,
            inhalt = stand.inhalt,
            beiErneut = { griffe.speicher.ranglisteLaden() },
        ) { liste ->
            if (liste.isEmpty()) {
                Leerhinweis("Noch keine Wache hat Punkte gesammelt.")
            } else {
                Column(modifier = Modifier.fillMaxWidth().flaeche()) {
                    liste.forEach { Ranglistenzeile(it) }
                }
            }
        }
    }
}

@Composable
private fun Ranglistenzeile(w: Wachenplatz) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Platzziffer(if (w.platz > 0) w.platz.toString() else "—", hervor = w.istEigene)
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = w.name,
                    style = Schrift.Normal,
                    color = if (w.istEigene) Farben.Amber else Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (w.istEigene) Marke("Eure", farbe = Farben.Amber)
            }
            SehrLeise(
                "Stufe ${w.stufe} · ${w.bezeichnung} · ${anzahl(w.mitglieder, "Mitglied", "Mitglieder")}" +
                    (w.landkreis?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                mono = true,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(zahl(w.erfahrung), style = Schrift.MonoNormal, color = Farben.Text)
            SehrLeise("${zahl(w.aktivitaetPunkte)} / 30 T.", mono = true)
        }
    }
}

// --------------------------------------------------------------- Wachen-Shop

/**
 * Der Wachen-Shop als eigene Seite — Kasse und Ausbauten der eigenen Wache.
 *
 * Wer keine Wache hat, hat auch keinen Shop und landet bei der Wache-Seite,
 * also in der Suche.
 */
@Composable
fun WachenShopSeite(
    griffe: WachenGriffe,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    wache: Bereichsstand<Wachendaten> = Bereichsstand(),
    detail: Bereichsstand<GemeinschaftDetail?> = Bereichsstand(),
    wachen: Wachenstand = Wachenstand(),
    konto: Konto? = null,
) {
    val speicher = griffe.speicher
    val eigene = wache.inhalt?.eigene
    val hier = detail.inhalt?.takeIf { eigene != null && it.gemeinschaft.id == eigene.id }
    val kopf = hier?.gemeinschaft ?: eigene
    val schatz = hier?.schatz
    var anpassenOffen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { speicher.anmelden() }
    LaunchedEffect(eigene) { eigene?.let { speicher.oeffnen(it.id) } }
    DisposableEffect(Unit) { onDispose { speicher.schliessen() } }

    // Ohne Wache kein Laden — zurück dorthin, wo man eine findet.
    LaunchedEffect(wache.geladen, eigene == null) {
        if (wache.geladen && wache.inhalt != null && eigene == null) griffe.zurWache()
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        // Der Eingang wie im Konto-Shop — der Stand steht da, bevor die Ware kommt.
        Kasten(abstandInnen = Abstand.Klein) {
            Etikett("Wachengemeinschaft")
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Wachen-Shop",
                    style = Schrift.Schlagzeile,
                    color = Farben.Text,
                    modifier = Modifier.weight(1f),
                )
                Marke("${zahl(schatz?.coins ?: 0)} Coins", farbe = Farben.AmberHell)
            }
            SehrLeise(
                "Coins verdient die Wache mit Stufenaufstiegen, Clanrunden und Einzahlungen aus der " +
                    "Mannschaft — zehn Credits werden ein Coin. Die Auslage wechselt jede Woche.",
            )
        }

        WachenNavigation(
            hier = "shop",
            hatWache = true,
            beiWache = griffe.zurWache,
            beiRangliste = griffe.zurRangliste,
            beiShop = {},
        )

        // Der Weg zum Anpassen steht über der Ware: Wer den Laden öffnet, hat oft
        // schon etwas und will es anziehen, nicht kaufen.
        if (schatz != null) {
            Knopf("Wache anpassen", { anpassenOffen = true }, kompakt = true)
        }

        wachen.meldung?.let { Meldungszeile(it) }

        if (schatz != null && kopf != null) {
            WachenShopInhalt(
                kopf = kopf,
                schatz = schatz,
                meineCredits = konto?.credits ?: 0,
                laeuft = wachen.laeuft,
                beiKaufen = { speicher.ausbauKaufen(kopf.id, it) },
                beiWunsch = { artikel, an -> speicher.wunschSetzen(kopf.id, artikel, an) },
                beiEinzahlen = { speicher.einzahlen(kopf.id, it) },
                beiAnpassen = { anpassenOffen = true },
            )
        } else {
            Leise("Der Wachen-Shop öffnet gerade …")
        }
    }

    if (anpassenOffen && kopf != null && schatz != null) {
        AussehenBlende(
            gemeinschaft = kopf,
            schatz = schatz,
            laeuft = wachen.laeuft,
            meldung = wachen.meldung,
            beiSchmuck = { art, stueck -> speicher.schmuckSetzen(kopf.id, art, stueck) },
            beiFarbe = { speicher.farbeSetzen(kopf.id, it) },
            beiSchliessen = { anpassenOffen = false },
        )
    }
}

/** Was ein Ausbau oder Stück bringt — der Name allein sagt es nicht jedem. */
private fun wirkung(art: String): String = when (art) {
    "Mitgliederplaetze" -> "Zwei Plätze mehr in der Mannschaft — mehrfach kaufbar, bis 50."
    "Terminplaetze" -> "Ein geplanter Dienst mehr gleichzeitig im Dienstplan."
    "Emblemrahmen" -> "Ein Ring um das Wachen-Emblem."
    "Emblemzeichen" -> "Ein Zeichen im Emblem, an Stelle der Initialen."
    "Wachenfarbe" -> "Ein Farbton über die freie Palette hinaus."
    "Beiname" -> "Eine Zeile über dem Wachennamen."
    else -> "Ein Muster im Kopf der Wachenseite."
}

/**
 * Die Aufschrift des Kaufknopfs — dieselbe Form wie überall (`utils/kauftexte.ts`):
 * der Preis, scharf gestellt die Rückfrage, und wenn es nicht reicht, der
 * Fehlbetrag statt des Preises. Das Guthaben ist hier die Kasse der Wache.
 */
private fun kaufknopf(preis: Int, guthaben: Int, scharf: Boolean): String {
    val fehlt = maxOf(0, preis - guthaben)
    return when {
        fehlt > 0 -> "Es fehlen $fehlt Coins"
        scharf -> "Wirklich für $preis Coins kaufen?"
        else -> "$preis Coins"
    }
}

/**
 * Die Auslage: Ausbau (immer da, rotiert nicht — eine Wache, die auf die Woche
 * mit Mitgliederplätzen warten muss, kann nicht wachsen), die zehn Zierstücke
 * dieser Woche mit dem Angebot, Einzahlen und der Auszug.
 *
 * Kaufen dürfen Leitung und Zugführer; alle anderen setzen ihren Wunsch — der
 * Shop selbst ist die Wunschliste.
 */
@Composable
private fun WachenShopInhalt(
    kopf: Gemeinschaft,
    schatz: Wachenschatz,
    meineCredits: Int,
    laeuft: Boolean,
    beiKaufen: (String) -> Unit,
    beiWunsch: (String, Boolean) -> Unit,
    beiEinzahlen: (Int) -> Unit,
    beiAnpassen: () -> Unit,
) {
    // Der Artikel, dessen Kauf auf die Bestätigung wartet — gegen den Fehlgriff.
    var bestaetige by remember { mutableStateOf<String?>(null) }
    var einzahlung by remember { mutableStateOf("") }
    val ton = wachenton(kopf)
    val ausbau = schatz.artikel.filter { !it.istZierde }
    val zierde = schatz.artikel.filter { it.istZierde }

    // Der erste Druck stellt scharf, der zweite kauft — ein Kauf ist endgültig.
    fun kaufdruck(id: String) {
        if (laeuft) return
        if (bestaetige != id) {
            bestaetige = id
        } else {
            bestaetige = null
            beiKaufen(id)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Gross)) {
        Leise(
            if (schatz.darfKaufen) {
                "Du darfst für die Wache kaufen."
            } else {
                "Kaufen dürfen Leitung und Zugführer — wünschen darfst du dir alles."
            },
        )

        // Ausbau: was die Wache größer macht, nicht schöner. Steht immer da.
        if (ausbau.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Ueberschrift("Ausbau")
                ausbau.forEach { a ->
                    Ware(
                        a = a,
                        gattung = "Ausbau",
                        besitz = if (a.gekauft > 0) "${a.gekauft} ×" else null,
                        istAusbau = true,
                        schatz = schatz,
                        ton = ton,
                        wachenname = kopf.name,
                        laeuft = laeuft,
                        scharf = bestaetige == a.id,
                        beiKaufdruck = { kaufdruck(a.id) },
                        beiWunsch = beiWunsch,
                    )
                }
            }
        }

        // Zierde: die Auslage dieser Woche.
        if (zierde.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Ueberschrift("Diese Woche im Laden")
                auslagenrest(schatz.wechseltUm)?.let { rest ->
                    val am = wachenzeit(schatz.wechseltUm)
                        ?.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd.MM.", java.util.Locale.GERMAN))
                    Text(
                        text = buildAnnotatedString {
                            append("Wechselt $rest")
                            if (am != null) {
                                withStyle(SpanStyle(color = Farben.TextSehrLeise)) { append(" · $am") }
                            }
                        },
                        style = Schrift.Klein,
                        color = Farben.Text,
                    )
                }
                Leise(
                    "Zehn Stücke je Woche, eines davon im Angebot. Gekauft ist gekauft — was ihr habt, " +
                        "bleibt euch, auch wenn es nächste Woche nicht mehr hier steht.",
                )
                Textweg("Den ganzen Katalog ansehen", beiAnpassen, farbe = Farben.Amber)
                zierde.forEach { a ->
                    Ware(
                        a = a,
                        gattung = platzname(a.art),
                        besitz = if (a.gekauft > 0) "✓ Im Besitz" else null,
                        istAusbau = false,
                        schatz = schatz,
                        ton = ton,
                        wachenname = kopf.name,
                        laeuft = laeuft,
                        scharf = bestaetige == a.id,
                        beiKaufdruck = { kaufdruck(a.id) },
                        beiWunsch = beiWunsch,
                    )
                }
            }
        }

        // Einzahlen darf jedes Mitglied — der Weg, beizutragen, ohne zu kaufen.
        val credits = einzahlung.toIntOrNull() ?: 0
        val coins = maxOf(0, credits) / 10
        val abgebucht = coins * 10
        val rest = credits - abgebucht
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Ueberschrift("Einzahlen")
            Kasten(abstandInnen = Abstand.Klein) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Feld(
                        wert = einzahlung,
                        beiAenderung = { einzahlung = it.filter(Char::isDigit).take(7) },
                        etikett = "Aus deinen Credits in die Wachenkasse",
                        platzhalter = "Credits",
                        tastatur = KeyboardType.Number,
                        stil = Schrift.MonoNormal,
                        modifier = Modifier.weight(1f),
                    )
                    Knopf(
                        "Einzahlen",
                        {
                            if (coins >= 1) {
                                beiEinzahlen(credits)
                                einzahlung = ""
                            }
                        },
                        aktiv = !laeuft && coins >= 1 && abgebucht <= meineCredits,
                        kompakt = true,
                    )
                }
                SehrLeise(
                    if (coins > 0) {
                        "$abgebucht deiner Credits werden $coins ${if (coins == 1) "Coin" else "Coins"}" +
                            (if (rest > 0) ", $rest bleiben bei dir" else "") + "."
                    } else {
                        "Ab 10 Credits — du hast $meineCredits."
                    },
                )
            }
        }

        // Der Auszug: warum eigentlich 80 Coins? Hier steht es.
        if (schatz.auszug.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Ueberschrift("Letzte Bewegungen")
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier
                        .fillMaxWidth()
                        .flaeche()
                        .padding(Abstand.Normal),
                ) {
                    schatz.auszug.forEach { p ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            SehrLeise(tagMonat(p.um), mono = true)
                            Text(
                                text = p.text,
                                style = Schrift.Klein,
                                color = Farben.Text,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = (if (p.betrag > 0) "+" else "") + p.betrag,
                                style = Schrift.MonoKlein,
                                color = if (p.betrag < 0) Farben.TextLeise else Farben.GruenHell,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Eine Kachel der Auslage — Probe, Name, Wirkung, Wünsche und der Knopf. */
@Composable
private fun Ware(
    a: Wachenartikel,
    gattung: String,
    besitz: String?,
    istAusbau: Boolean,
    schatz: Wachenschatz,
    ton: androidx.compose.ui.graphics.Color,
    wachenname: String,
    laeuft: Boolean,
    scharf: Boolean,
    beiKaufdruck: () -> Unit,
    beiWunsch: (String, Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = if (a.angebot) Farben.AmberTief else Farben.Rand)
            .padding(Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(92.dp)
                    .height(56.dp)
                    .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false),
            ) {
                WachenProbe(a.art, a.stueckId, a.name, ton, wachenname)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gattung.uppercase(),
                    style = Schrift.Etikett,
                    color = gattungston(a.art),
                )
                Text(a.name, style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold), color = Farben.Text)
            }
            besitz?.let { Marke(it, farbe = Farben.GruenHell) }
        }

        // Das Stück der Woche trägt seinen alten Preis durchgestrichen daneben.
        if (a.angebot) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Farben.Amber, fontWeight = FontWeight.Bold)) {
                        append("−${schatz.angebotRabattProzent} %")
                    }
                    append("  ")
                    withStyle(
                        SpanStyle(
                            color = Farben.TextSehrLeise,
                            textDecoration = TextDecoration.LineThrough,
                        ),
                    ) {
                        append(a.listenpreis.toString())
                    }
                },
                style = Schrift.MonoKlein,
                color = Farben.Text,
            )
        }

        SehrLeise(wirkung(a.art))
        if (a.wuensche > 0 && (istAusbau || a.gekauft == 0)) {
            Text(
                text = "${a.wuensche} ${if (a.wuensche == 1) "Wunsch" else "Wünsche"} aus der Mannschaft",
                style = Schrift.Klein,
                color = Farben.AmberHell,
            )
        }

        when {
            istAusbau && a.ausverkauft -> SehrLeise("Ausgebaut")
            !istAusbau && a.gekauft > 0 -> SehrLeise("Im Besitz")
            schatz.darfKaufen -> Knopf(
                kaufknopf(a.preis, schatz.coins, scharf),
                beiKaufdruck,
                art = if (scharf) Knopfart.Haupt else Knopfart.Normal,
                aktiv = !laeuft && schatz.coins >= a.preis,
                kompakt = true,
            )
            else -> Knopf(
                if (a.vonMirGewuenscht) "Gewünscht ✓" else "Wünschen",
                { beiWunsch(a.id, !a.vonMirGewuenscht) },
                art = Knopfart.Leise,
                aktiv = !laeuft,
                kompakt = true,
            )
        }
    }
}
