package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Kontodienst
import de.pagerspass.pagerspass.mobil.Kontostand
import de.pagerspass.pagerspass.netz.Auftrag
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Premiumstand
import de.pagerspass.pagerspass.netz.Shop
import de.pagerspass.pagerspass.netz.Shopartikel
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import java.time.Duration
import java.time.Instant

/**
 * Der Shop — das Gegenstück zu `ShopView.vue`.
 *
 * <b>Fünf Bereiche wie im Web:</b> Übersicht (Abo-Anreißer, Glücksrad,
 * Dienstaufträge), Premium (das Schaufenster), Sortiment (die Stücke des Tages),
 * Fahrzeuge (Tagesangebot und der Weg ins Autohaus) und Guthaben (Auszug und
 * Aktionscodes). Die Bereiche stehen als Pillen, die umbrechen dürfen — fünf Reiter
 * mit Zahl passen nicht auf eine Handbreit, und ein abgeschnittenes „Fahrzeu…" ist
 * kein Weg.
 *
 * <b>Credits werden hier ausgegeben, echtes Geld nie.</b> Sortiment, Glücksrad,
 * Geschenke und Codes laufen in der App; der Premium-Bereich zeigt Preise und
 * Vorteile und schickt zum Abschließen auf die Webseite. Nach der Rückkehr holt die
 * Seite den Abo-Stand neu — dort kann sich gerade etwas geändert haben.
 *
 * <b>Ein Kauf will zweimal gedrückt sein</b> (`kaufknopf` im Web): erst scharf
 * stellen, dann kaufen. Ein Kauf ist endgültig; ein Fehlgriff soll keinen kosten.
 */
@Composable
fun ShopSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    shop: Bereichsstand<Shop?> = Bereichsstand(),
    gutscheine: Int = 0,
    server: String = "",
    laeuft: Boolean = false,
    stand: Kontostand = Kontostand(),
    dienst: Kontodienst? = null,
    /**
     * Der Bereich, in den jemand geschickt wurde — `premium` aus „Premium ansehen".
     * Er wird einmal übernommen und dann zurückgegeben (`beiWunschErfuellt`), sonst
     * spränge der Shop bei jedem Besuch wieder dorthin.
     */
    wunsch: String? = null,
    beiWunschErfuellt: () -> Unit = {},
    beiLaden: () -> Unit = {},
    beiKauf: (String) -> Unit = {},
    beiTagesbonus: () -> Unit = {},
    beiAutohaus: () -> Unit = {},
    beiProfil: () -> Unit = {},
    beiAusbildung: () -> Unit = {},
    /**
     * Der Stand der Garage — mit ihm steht das Autohaus im Bereich „Fahrzeuge“
     * selbst (wie im Web). Ohne ihn bleibt es beim Weg in die Garage.
     */
    garage: de.pagerspass.pagerspass.mobil.Garagendaten? = null,
    katalog: List<de.pagerspass.pagerspass.netz.Fahrzeugvorlage> = emptyList(),
    beiFahrzeugHolen: ((String, Boolean) -> Unit)? = null,
) {
    var bereich by rememberSaveable { mutableStateOf(wunsch ?: UEBERSICHT) }
    LaunchedEffect(wunsch) {
        if (wunsch != null) {
            bereich = wunsch
            beiWunschErfuellt()
        }
    }
    var scharf by remember { mutableStateOf<String?>(null) }
    var schenkstueck by remember { mutableStateOf<Shopartikel?>(null) }
    var codeOffen by remember { mutableStateOf(false) }

    BeiRueckkehr {
        beiLaden()
        dienst?.premiumLaden()
    }

    val credits = shop.inhalt?.credits ?: konto?.credits ?: 0
    val premium = stand.premium.inhalt

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Shop",
            unterzeile = "Alles Zierde, kein Vorteil im Einsatz — und was die Laufbahn verleiht, " +
                "bleibt unverkäuflich.",
            knoepfe = { Marke("${zahl(credits)} Credits", farbe = Farben.AmberHell) },
        )

        shop.inhalt?.naechsterWechsel?.let { wechsel ->
            SehrLeise("Neues Sortiment ${restzeit(wechsel)}", mono = true)
        }

        Pillenreihe {
            BEREICHE.forEach { (id, wort) ->
                Pille(
                    aufschrift = if (id == PREMIUM && premium?.aktiv == true) "$wort ✓" else wort,
                    an = bereich == id,
                    beiDruck = {
                        bereich = id
                        scharf = null
                    },
                    zahl = when (id) {
                        SORTIMENT -> shop.inhalt?.sortiment?.size?.takeIf { it > 0 }
                        FAHRZEUGE -> gutscheine.takeIf { it > 0 }
                        else -> null
                    },
                )
            }
        }

        Bereichskopf(bereich)

        if (bereich == PREMIUM) {
            Schaufenster(stand, server)
            return@Seite
        }

        Bereich(
            laedt = shop.ersteLadung,
            fehler = shop.fehler,
            inhalt = shop.inhalt,
            beiErneut = beiLaden,
        ) { s ->
            when (bereich) {
                UEBERSICHT -> Uebersicht(s, premium, konto, laeuft, beiTagesbonus, beiAusbildung) {
                    bereich = PREMIUM
                }
                SORTIMENT -> Sortiment(
                    s = s,
                    scharf = scharf,
                    laeuft = laeuft,
                    beiKauf = { a ->
                        if (scharf != a.id) {
                            scharf = a.id
                        } else {
                            beiKauf(a.id)
                            scharf = null
                        }
                    },
                    beiSchenken = { a ->
                        schenkstueck = a
                        dienst?.rueckmeldungWeg(Kontodienst.SCHENKEN)
                        dienst?.schenkfreundeLaden()
                    },
                    beiProfil = beiProfil,
                )
                FAHRZEUGE -> if (garage != null && beiFahrzeugHolen != null) {
                    Fahrzeugbereich(s, gutscheine, null)
                    Autohaus(
                        garage = garage,
                        katalog = katalog,
                        credits = credits,
                        laeuft = laeuft,
                        vorwahl = s.tagesangebot?.templateId?.ifBlank { null },
                        beiHolen = beiFahrzeugHolen,
                    )
                } else {
                    Fahrzeugbereich(s, gutscheine, beiAutohaus)
                }
                else -> Guthaben(s) {
                    dienst?.rueckmeldungWeg(Kontodienst.CODE)
                    dienst?.codevorschauWeg()
                    codeOffen = true
                }
            }
        }
    }

    val stueck = schenkstueck
    if (stueck != null && dienst != null) {
        Schenkblende(stueck, stand, dienst) { schenkstueck = null }
    }
    if (codeOffen && dienst != null) {
        Codeblende(stand, dienst) { codeOffen = false }
    }
}

const val UEBERSICHT = "uebersicht"
const val PREMIUM = "premium"
private const val SORTIMENT = "sortiment"
private const val FAHRZEUGE = "fahrzeuge"
private const val GUTHABEN = "guthaben"

private val BEREICHE = listOf(
    UEBERSICHT to "Übersicht",
    PREMIUM to "Premium",
    SORTIMENT to "Sortiment",
    FAHRZEUGE to "Fahrzeuge",
    GUTHABEN to "Guthaben",
)

/** Kennung, Überschrift und Satz über jedem Bereich — dieselben Wörter wie im Web. */
@Composable
private fun Bereichskopf(bereich: String) {
    val (kennung, titel, satz) = when (bereich) {
        PREMIUM -> Triple("Das Abo", "Dein Auftritt im Dienst", "Alles, was zum Abo gehört – und was es ausdrücklich nicht tut.")
        SORTIMENT -> Triple("Tagesauswahl", "Zierstücke des Tages", "Acht wechselnde Stücke – kaufen, verschenken oder direkt anlegen.")
        FAHRZEUGE -> Triple("Autohaus", "Deine nächsten Fahrzeuge", "Tagesangebot, Gutscheine und der vollständige Fahrzeugkatalog.")
        GUTHABEN -> Triple("Kontobewegungen", "Credits im Blick", "Nachvollziehen, was hereinkam, und Aktionscodes einlösen.")
        else -> Triple("Dein Shop", "Heute für dich", "Premium, Tagesbonus und Wochenaufträge auf einen Blick.")
    }
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        Etikett(kennung)
        Text(titel, style = Schrift.Titel, color = Farben.Text)
        Leise(satz)
    }
}

// ----------------------------------------------------------------- Übersicht

@Composable
private fun Uebersicht(
    s: Shop,
    premium: Premiumstand?,
    konto: Konto?,
    laeuft: Boolean,
    beiTagesbonus: () -> Unit,
    beiAusbildung: () -> Unit,
    beiPremium: () -> Unit,
) {
    val aktiv = premium?.aktiv == true

    // Premium in der Übersicht: nur der Anreißer. Das Angebot steht im eigenen Bereich.
    Kasten(marke = aktiv, abstandInnen = Abstand.Klein) {
        Etikett("Abo")
        Text("PagerSpass Premium", style = Schrift.Gross, color = Farben.Text)
        Leise(
            if (aktiv) {
                "Läuft auf diesem Konto — hier steht, was alles dazugehört."
            } else {
                "Mehr Platz für eigene Alarm- und Ausrückeordnungen, dazu Rahmen, Melder, Muster und " +
                    "Titel, die es sonst nirgends gibt."
            },
        )
        Knopf(if (aktiv) "Ansehen" else "Premium ansehen", beiPremium, kompakt = true)
    }

    // Das tägliche Glücksrad. Gedreht wird am Server; hier steht, was es geben kann.
    val bonus = s.tagesbonus
    Kasten(marke = bonus.verfuegbar, wartet = true, abstandInnen = Abstand.Klein) {
        Etikett("Tägliches Glücksrad")
        Text(
            if (bonus.verfuegbar) "Dein Dreh ist bereit" else "Für heute gedreht",
            style = Schrift.Gross,
            color = Farben.Text,
        )
        SehrLeise("Serie: ${bonus.serie} ${if (bonus.serie == 1) "Tag" else "Tage"}")
        SehrLeise("Credits, Fahrzeuggutscheine und als Wachmitglied auch Gemeinschafts-Coins.")
        bonus.felder.forEach { feld ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
                Text(feld.text, style = Schrift.Klein, color = Farben.TextLeise, modifier = Modifier.weight(1f))
                SehrLeise("${feld.gewicht} %", mono = true)
            }
        }
        Knopf(
            when {
                laeuft -> "Dreht …"
                bonus.verfuegbar -> "Jetzt drehen"
                else -> "Gedreht ✓"
            },
            beiTagesbonus,
            art = Knopfart.Haupt,
            aktiv = bonus.verfuegbar && !laeuft,
        )
    }

    // Mit offener Einweisung stehen die Aufträge nicht da, sondern der Weg zu ihnen:
    // Sie zählen Dienste, und Dienst darf dieses Konto noch nicht.
    if (konto?.einweisungOffen == true) {
        Abschnitt("Dienstaufträge der Woche") {
            Kasten(abstandInnen = Abstand.Klein) {
                Leise(
                    "Die Aufträge zählen gefahrene Dienste — und dein erster steht noch aus. Fahr die " +
                        "Ausbildungsschicht; danach stehen hier drei Aufträge, und alle Runden sind offen.",
                )
                Textweg("Zur Ausbildungsschicht", beiAusbildung)
            }
        }
    } else if (s.auftraege.isNotEmpty()) {
        Abschnitt("Dienstaufträge der Woche", weiterweg = {
            s.zulagen.naechsterWechsel?.let { SehrLeise(wochenrest(it), mono = true) }
        }) {
            s.auftraege.forEach { Auftragszeile(it) }
            if (s.zulagen.deckel > 0) {
                SehrLeise(
                    "Neue Aufträge montags um 12:00. Zulagen diese Woche: ${s.zulagen.verbraucht} von " +
                        "${s.zulagen.deckel} Credits — mehr bringt der Dienst in einer Woche nicht ein.",
                )
            }
        }
    }
}

/** Ein Dienstauftrag — Stufe, Lohn, Text und der Balken. Erfüllt heißt: kein Balken, ein Haken. */
@Composable
private fun Auftragszeile(a: Auftrag) {
    val ton = when (a.stufe) {
        "Leicht" -> Farben.GruenHell
        "Schwer" -> Farben.SignalHell
        else -> Farben.Amber
    }
    Kasten(abstandInnen = Abstand.Winzig) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
            Marke(a.stufe.lowercase(), farbe = ton)
            Spacer1()
            Text("+${a.betrag}", style = Schrift.MonoKlein, color = Farben.AmberHell)
        }
        Text(a.text, style = Schrift.Normal, color = Farben.Text)
        if (a.erfuellt) {
            Text("✓ Erfüllt", style = Schrift.MonoKlein, color = Farben.GruenHell)
        } else {
            Fortschritt(anteil = a.stand.toFloat() / a.ziel.coerceAtLeast(1), text = "${a.stand}/${a.ziel}")
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Spacer1() =
    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

// ------------------------------------------------------------------- Premium

/**
 * Das Schaufenster — `PremiumSchaufenster.vue`, ohne Kasse.
 *
 * <b>Die Bühne, die Vitrine, die Vorteilsliste und die Pläne stehen da wie im Web</b>
 * — was fehlt, ist der Haken zum sofortigen Beginn und der Knopf zu Stripe. Beides
 * gehört an die Stelle, an der bezahlt wird, und die ist die Webseite. Die Knöpfe
 * der Pläne öffnen sie, genau im Premium-Bereich des Shops.
 */
@Composable
private fun Schaufenster(stand: Kontostand, server: String) {
    val browser = LocalUriHandler.current
    val premium = stand.premium.inhalt
    val aktiv = premium?.aktiv == true
    val offen = premium?.zahlungOffen == true
    val kaufbar = premium?.kaufVerfuegbar == true && !aktiv && !offen
    val zurKasse = { browser.openUri(imWeb(server, "shop?bereich=premium")) }

    Kasten(marke = true, wartet = aktiv, abstandInnen = Abstand.Klein) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            Etikett("Abo · monatlich kündbar", Modifier.weight(1f))
            Marke(
                when {
                    aktiv -> "Aktiv"
                    offen -> "Zahlung offen"
                    premium?.kaufVerfuegbar == true -> "Optional"
                    else -> "Nicht verfügbar"
                },
                farbe = when {
                    aktiv -> Farben.GruenHell
                    offen -> Farben.Amber
                    else -> Farben.TextLeise
                },
            )
        }
        Text("PagerSpass Premium", style = Schrift.Titel, color = Farben.Text)
        Leise(
            "Dein Dienst bleibt derselbe — dein Auftritt nicht. Mehr Platz für eigene Alarm- und " +
                "Ausrückeordnungen, dazu Rahmen, Melder, Muster, Karten und Titel, die es sonst " +
                "nirgends gibt.",
        )
        // Der Preis steht nur da, wo er einzulösen ist.
        if (kaufbar && premium != null && premium.jahrespreisCent > 0) {
            Text(
                "ab ${euro(Math.round(premium.jahrespreisCent / 12.0).toInt())} im Monat · im Jahresplan",
                style = Schrift.Normal,
                color = Farben.AmberHell,
            )
        }
        Echtgeldhinweis()
    }

    if (premium == null) {
        SehrLeise(stand.premium.fehler ?: "Premium wird geladen …")
        return
    }

    Kasten(abstandInnen = Abstand.Winzig) {
        Etikett("Platz im Abo")
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
            listOf(
                premium.grenzen.aaoVorlagen to "AAO-Vorlagen",
                premium.grenzen.szenarien to "Szenarien",
                premium.grenzen.rundenvorlagen to "Runden­vorlagen",
                premium.grenzen.leitstellen to "Leitstellen",
            ).forEach { (wert, was) ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(zahl(wert), style = Schrift.MonoNormal.copy(fontSize = Schrift.TITEL), color = Farben.Amber)
                    SehrLeise(was)
                }
            }
        }
    }

    Vitrine()

    if (premium.vorteile.isNotEmpty()) {
        Abschnitt("Alles, was dazugehört") {
            Kasten(abstandInnen = Abstand.Winzig) {
                premium.vorteile.forEach { v -> Text("· $v", style = Schrift.Klein, color = Farben.TextLeise) }
            }
        }
    }

    Abschnitt("Die Pläne") {
        when {
            aktiv -> Kasten(abstandInnen = Abstand.Klein) {
                Text("Danke — Premium läuft auf diesem Konto.", style = Schrift.Gross, color = Farben.Text)
                Leise(
                    "Verwaltet wird das Abo auf der Webseite: Plan wechseln, Zahlungsweg ändern, " +
                        "kündigen. Was du hier siehst, gehört alles dir.",
                )
                Knopf("Abo verwalten", { browser.openUri(imWeb(server, "konto")) }, art = Knopfart.Haupt)
                Vertragswege(server)
            }

            offen -> Kasten(abstandInnen = Abstand.Klein) {
                Text(
                    "Für dein Abo ist eine Zahlung offen. Solange sie offen ist, ruhen die " +
                        "Premium-Vorteile — gekündigt ist deshalb nichts.",
                    style = Schrift.Klein,
                    color = Farben.AmberHell,
                )
                Knopf("Zahlung in Ordnung bringen", { browser.openUri(imWeb(server, "konto")) }, art = Knopfart.Haupt)
                Vertragswege(server)
            }

            kaufbar -> {
                Plan(
                    etikett = "Monatlich",
                    preis = euro(premium.monatspreisCent),
                    unter = "je Monat",
                    bedingung = "Endpreis · verlängert sich automatisch um einen Monat · jederzeit zum " +
                        "Ende des Zeitraums kündbar",
                    knopf = "Monatlich abonnieren",
                    haupt = false,
                    beiDruck = zurKasse,
                )
                Plan(
                    etikett = "Jährlich",
                    preis = euro(premium.jahrespreisCent),
                    unter = "je Jahr · das sind ${euro(Math.round(premium.jahrespreisCent / 12.0).toInt())} im Monat",
                    bedingung = "Endpreis · verlängert sich automatisch um ein Jahr · jederzeit zum Ende " +
                        "des Zeitraums kündbar",
                    knopf = "Jährlich abonnieren",
                    haupt = true,
                    beiDruck = zurKasse,
                )
                SehrLeise(
                    "Abgeschlossen wird auf pagerspass.de: Dort bestätigst du den sofortigen Beginn und " +
                        "bestellst verbindlich auf der Bezahlseite von Stripe. Es gelten unsere AGB. " +
                        "Bezahlt wird über Stripe; PagerSpass sieht deine Kartendaten nie. Die Zierstücke " +
                        "bleiben, solange das Abo läuft — was du dir mit Credits gekauft oder erspielt " +
                        "hast, bleibt für immer.",
                )
                Vertragswege(server)
            }

            else -> Kasten(abstandInnen = Abstand.Klein) {
                Text("Premium ist derzeit nicht zu haben.", style = Schrift.Gross, color = Farben.Text)
                Leise(
                    "Neue Abos können momentan nicht abgeschlossen werden. Laufende Abos und bereits " +
                        "gutgeschriebene Premium-Zeit bleiben davon unberührt — alles oben steht dir dann " +
                        "weiterhin offen.",
                )
            }
        }
    }
}

/** Ein Plan mit Preis, Bedingung und dem Weg zur Webseite — § 312j BGB am Preis, nicht im Kleingedruckten. */
@Composable
private fun Plan(
    etikett: String,
    preis: String,
    unter: String,
    bedingung: String,
    knopf: String,
    haupt: Boolean,
    beiDruck: () -> Unit,
) {
    Kasten(marke = haupt, abstandInnen = Abstand.Winzig) {
        Etikett(etikett)
        Text(preis, style = Schrift.MonoNormal.copy(fontSize = Schrift.SCHLAGZEILE), color = Farben.Text)
        SehrLeise(unter)
        SehrLeise(bedingung)
        Knopf(
            "$knopf auf pagerspass.de",
            beiDruck,
            art = if (haupt) Knopfart.Haupt else Knopfart.Normal,
            breit = true,
        )
    }
}

/** Die beiden gesetzlichen Wege (§ 312k, § 356a BGB) — neben der Verwaltung, nicht statt ihr. */
@Composable
private fun Vertragswege(server: String) {
    val browser = LocalUriHandler.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
        Textweg("Verträge hier kündigen", { browser.openUri(imWeb(server, "vertrag-kuendigen")) })
        Textweg("Vertrag widerrufen", { browser.openUri(imWeb(server, "vertrag-widerrufen")) })
    }
}

// ----------------------------------------------------------------- Sortiment

@Composable
private fun Sortiment(
    s: Shop,
    scharf: String?,
    laeuft: Boolean,
    beiKauf: (Shopartikel) -> Unit,
    beiSchenken: (Shopartikel) -> Unit,
    beiProfil: () -> Unit,
) {
    Abschnitt("Heute im Sortiment", weiterweg = { SehrLeise("Nur heute", mono = true) }) {
        if (s.sortiment.isEmpty()) {
            Leerhinweis("Für heute ist noch kein Sortiment eingetroffen.")
            return@Abschnitt
        }
        s.sortiment.forEach { a ->
            val fehlt = (a.preis - s.credits).coerceAtLeast(0)
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(ecke = 9.dp, randfarbe = if (scharf == a.id) Farben.AmberTief else Farben.Rand)
                    .padding(Abstand.Normal),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                        Etikett(gattung(a.art))
                        Text(a.name, style = Schrift.Normal, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (a.gekauft) Marke("✓ Im Besitz", farbe = Farben.GruenHell)
                }
                // Das Stück selbst auf seiner Bühne — nicht nur sein Name (`WareKachel.vue`).
                Warenbuehne(a)
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    when {
                        a.gekauft -> Knopf("Im Konto anlegen", beiProfil, art = Knopfart.Leise, kompakt = true)
                        fehlt > 0 -> Knopf("Es fehlen $fehlt Credits", {}, aktiv = false, kompakt = true)
                        else -> Knopf(
                            if (scharf == a.id) "Wirklich für ${a.preis} Credits kaufen?" else "${a.preis} Credits",
                            { beiKauf(a) },
                            art = if (scharf == a.id) Knopfart.Haupt else Knopfart.Normal,
                            aktiv = !laeuft,
                            kompakt = true,
                        )
                    }
                    // Verschenken steht auch an Stücken, die man selbst schon hat —
                    // genau dort ist es am naheliegendsten.
                    Knopf("Verschenken", { beiSchenken(a) }, art = Knopfart.Leise, aktiv = fehlt == 0 && !laeuft, kompakt = true)
                }
            }
        }
    }
}

/** Die Gattung eines Stücks — `GATTUNG` in `ShopView.vue`. */
private fun gattung(art: String): String = when (art) {
    "Meldergesicht" -> "Melder"
    "Profilrahmen" -> "Rahmen"
    "Kopfmuster" -> "Muster"
    "Wappenfarbe" -> "Wappenfarbe"
    "Titel" -> "Titel"
    "Melderton" -> "Ton"
    else -> art
}

// ----------------------------------------------------------------- Fahrzeuge

/**
 * Fahrzeuge — das Tagesangebot und der Weg ins Autohaus.
 *
 * Gekauft wird im Autohaus der Garage, nicht hier: Zwei Kaufstellen für dieselbe
 * Sache wären zwei Wegweiser zum selben Ort (so steht es auch über `ShopView.vue`).
 */
@Composable
private fun Fahrzeugbereich(s: Shop, gutscheine: Int, beiAutohaus: (() -> Unit)?) {
    val angebot = s.tagesangebot
    if (angebot != null) {
        Abschnitt("Fahrzeug des Tages") {
            Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
                Text("−25 % · nur heute", style = Schrift.MonoKlein, color = Farben.Amber)
                Text(angebot.typ, style = Schrift.MonoNormal.copy(fontSize = Schrift.TITEL), color = Farben.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${angebot.regulaer} Credits",
                        style = Schrift.Klein.copy(textDecoration = TextDecoration.LineThrough),
                        color = Farben.TextSehrLeise,
                    )
                    Text("${angebot.preis} Credits", style = Schrift.MonoNormal, color = Farben.AmberHell)
                }
                if (beiAutohaus != null) Knopf("Im Autohaus ansehen", beiAutohaus, art = Knopfart.Haupt)
                else SehrLeise("Steht unten im Autohaus im Schaufenster.")
            }
        }
    }
    // Mit dem Autohaus im Shop braucht es den Weg dorthin nicht.
    if (beiAutohaus == null) return

    Abschnitt("Autohaus", weiterweg = {
        if (gutscheine > 0) SehrLeise("$gutscheine Gutschein${if (gutscheine == 1) "" else "e"}", mono = true)
    }) {
        Kasten(abstandInnen = Abstand.Klein) {
            Leise(
                if (gutscheine > 0) {
                    "Du hast $gutscheine Gutschein${if (gutscheine == 1) "" else "e"} offen — such dir ein " +
                        "Fahrzeug aus, es kostet nichts. Alles andere kaufst du mit Credits."
                } else {
                    "Der vollständige Fahrzeugkatalog mit den Preisen für dein Konto."
                },
            )
            Knopf("Zum Autohaus", beiAutohaus)
        }
    }
}

// ------------------------------------------------------------------ Guthaben

@Composable
private fun Guthaben(s: Shop, beiCode: () -> Unit) {
    if (s.auszug.isNotEmpty()) {
        Abschnitt("Letzte Bewegungen") {
            Kasten(abstandInnen = Abstand.Winzig) {
                s.auszug.forEach { p ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
                        SehrLeise(tagKurz(p.um), mono = true)
                        Text(p.text, style = Schrift.Klein, color = Farben.TextLeise, modifier = Modifier.weight(1f))
                        Text(
                            "${if (p.betrag > 0) "+" else ""}${p.betrag}",
                            style = Schrift.MonoKlein,
                            color = if (p.betrag < 0) Farben.TextLeise else Farben.GruenHell,
                        )
                    }
                }
            }
        }
    }

    // Ganz unten: Eine Ansicht, die mit einem Eingabefeld beginnt, sieht nach Arbeit
    // aus — und die Buchung steht danach gleich darüber im Auszug.
    Abschnitt("Code einlösen") {
        Kasten(abstandInnen = Abstand.Klein) {
            Leise("Aktionscodes gibt es auf unseren Kanälen — jeder gilt einmal je Konto.")
            Knopf("Code eingeben", beiCode, kompakt = true)
        }
    }
}

// ------------------------------------------------------------------- Blenden

/**
 * Verschenken — bezahlt wird aus dem eigenen Guthaben, bekommen tut es der andere.
 * Zu junge Freundschaften stehen mit ihrer Wartezeit dabei, nicht ausgeblendet:
 * Ein Name, der einfach fehlt, sieht aus wie ein Fehler.
 */
@Composable
private fun Schenkblende(stueck: Shopartikel, stand: Kontostand, dienst: Kontodienst, beiSchliessen: () -> Unit) {
    val fertig = stand.meldung(Kontodienst.SCHENKEN)?.takeIf { !it.fehler }

    Blende(
        titel = "${stueck.name} verschenken",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = { Knopf(if (fertig != null) "Fertig" else "Abbrechen", beiSchliessen, breit = true) },
    ) {
        if (fertig != null) {
            Text(fertig.text, style = Schrift.Normal, color = Farben.GruenHell)
            return@Blende
        }
        Leise(
            "Du bezahlst ${stueck.preis} Credits, bekommen tut es der andere. Verschenken geht an " +
                "Freunde, mit denen du seit mindestens zwei Tagen verbunden bist.",
        )
        val freunde = stand.schenkfreunde
        when {
            freunde.ersteLadung || !freunde.geladen -> SehrLeise("Die Freundesliste wird geholt …")
            freunde.inhalt.isNullOrEmpty() && freunde.fehler == null -> Leerhinweis("Du hast noch keine bestätigten Freunde.")
            else -> freunde.inhalt.orEmpty().forEach { f ->
                Tatzeile(f.anzeigename, "@${f.benutzername}", mono = true) {
                    if (f.darfEmpfangen) {
                        Knopf(
                            if (stand.laeuft == "${Kontodienst.SCHENKEN}:${f.kennung}") "Wird verpackt …" else "Schenken",
                            { dienst.schenken(f, stueck.id, stueck.name) },
                            aktiv = stand.laeuft == null,
                            kompakt = true,
                        )
                    } else {
                        SehrLeise("noch ${f.wartetage} ${if (f.wartetage == 1) "Tag" else "Tage"}")
                    }
                }
            }
        }
        freunde.fehler?.let { Rueckmeldungszeile(de.pagerspass.pagerspass.mobil.Rueckmeldung(it, true)) }
        Rueckmeldungszeile(stand.meldung(Kontodienst.SCHENKEN))
    }
}

/**
 * Einen Aktionscode einlösen. Am Handy schreibt ein Feld ohne Vorgaben klein und
 * korrigiert — deshalb wird hier großgeschrieben, bevor der Code rausgeht.
 * Gehört er einem Creator, steht darunter, von wem er ist und was er bringt.
 */
@Composable
private fun Codeblende(stand: Kontostand, dienst: Kontodienst, beiSchliessen: () -> Unit) {
    var code by remember { mutableStateOf("") }
    val meldung = stand.meldung(Kontodienst.CODE)
    LaunchedEffect(code) {
        kotlinx.coroutines.delay(400)
        dienst.codevorschau(code)
    }

    Blende(
        titel = "Code einlösen",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, breit = true)
            Knopf(
                if (stand.laeuft == Kontodienst.CODE) "Wird geprüft …" else "Einlösen",
                { dienst.codeEinloesen(code) { code = "" } },
                art = Knopfart.Haupt,
                breit = true,
                aktiv = stand.laeuft == null && code.isNotBlank(),
            )
        },
    ) {
        Feld(
            code,
            { code = it.uppercase().take(32) },
            etikett = "Dein Code",
            platzhalter = "z. B. LEITSTELLE200",
            weiterTaste = ImeAction.Done,
        )
        stand.codevorschau?.let { v ->
            Kasten(abstandInnen = Abstand.Haar) {
                Text("Code von ${v.creator}", style = Schrift.Normal, color = Farben.Text)
                SehrLeise(v.belohnung)
                if (!v.einloesbar) SehrLeise(v.grund ?: "Dieser Code gilt gerade nicht.")
            }
        }
        if (meldung != null) {
            Rueckmeldungszeile(meldung)
        } else {
            SehrLeise("Groß- und Kleinschreibung ist egal, Leerzeichen und Bindestriche auch.")
        }
    }
}

// ---------------------------------------------------------------- Zeitrechnung

/** „in 18 Std. 56 Min." — minutengenau, wie im Web. */
private fun restzeit(iso: String): String {
    val minuten = runCatching { Duration.between(Instant.now(), zeitpunktAus(iso)).toMinutes() }
        .getOrDefault(0L)
        .coerceAtLeast(0L)
    val stunden = minuten / 60
    return if (stunden > 0) "in $stunden Std. ${minuten % 60} Min." else "in $minuten Min."
}

/** „noch 2 Tage" oder „noch 5 Std." bis zum Wochenwechsel. */
private fun wochenrest(iso: String): String {
    val zeit = runCatching { Duration.between(Instant.now(), zeitpunktAus(iso)) }.getOrDefault(Duration.ZERO)
    val tage = zeit.toDays()
    if (tage >= 1) return "noch $tage Tag${if (tage == 1L) "" else "e"}"
    return "noch ${((zeit.toMinutes() + 59) / 60).coerceAtLeast(0)} Std."
}

/** Ein Zeitpunkt des Servers — mit Versatz (`+00:00`) oder mit `Z`. */
private fun zeitpunktAus(iso: String): Instant =
    runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrElse { Instant.parse(iso) }

/** Tag und Monat — „24.09." für den Auszug. */
private fun tagKurz(iso: String): String = tag(iso).take(6)
