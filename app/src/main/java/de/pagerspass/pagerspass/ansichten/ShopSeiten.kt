package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SenkrechterReiter
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
        Ladenportal(
            etikett = "Ausrüstung",
            titel = "Shop",
            satz = "Alles Zierde, kein Vorteil im Einsatz — und was die Laufbahn verleiht, " +
                "bleibt unverkäuflich.",
            stand = zahl(credits),
            waehrung = "Credits",
        )

        shop.inhalt?.naechsterWechsel?.let { wechsel ->
            SehrLeise("Neues Sortiment ${restzeit(wechsel)}", mono = true)
        }

        // Die Reiterreihe des Ladens — senkrecht, alle fünf in einer Zeile (mobil.css,
        // `.shop-reiter`). Der Kontostand steht nicht auch noch am Reiter: Er steht
        // eine Handbreit darüber groß im Kopf.
        Reiterreihe {
            BEREICHE.forEach { (id, wort) ->
                SenkrechterReiter(
                    aufschrift = wort,
                    offen = bereich == id,
                    beiDruck = {
                        bereich = id
                        scharf = null
                    },
                    zeichen = ShopZeichen.zu(id),
                    zeichenGold = id == PREMIUM,
                    ecke = when (id) {
                        PREMIUM -> "✓".takeIf { premium?.aktiv == true }
                        SORTIMENT -> shop.inhalt?.sortiment?.size?.takeIf { it > 0 }?.toString()
                        FAHRZEUGE -> gutscheine.takeIf { it > 0 }?.toString()
                        else -> null
                    },
                    eckeWarnt = id == FAHRZEUGE,
                )
            }
        }

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
                UEBERSICHT -> Uebersicht(s, premium, konto, gutscheine, laeuft, beiTagesbonus, beiAusbildung) {
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

// ----------------------------------------------------------------- Übersicht

@Composable
private fun Uebersicht(
    s: Shop,
    premium: Premiumstand?,
    konto: Konto?,
    gutscheine: Int,
    laeuft: Boolean,
    beiTagesbonus: () -> Unit,
    beiAusbildung: () -> Unit,
    beiPremium: () -> Unit,
) {
    val aktiv = premium?.aktiv == true

    // Die Tafel: was der Laden heute für dich bereithält, in Zahlen. Die Credits
    // stehen nicht darauf — sie stehen eine Handbreit darüber im Kopf.
    val bonusstand = s.tagesbonus
    val wechsel = s.naechsterWechsel?.let { "neu ${restzeit(it)}" } ?: "wechselt täglich"
    Kennzahltafel(
        buildList {
            add { m: Modifier ->
                Kennzahlkachel(
                    "Glücksrad-Serie",
                    "${bonusstand.serie} ${if (bonusstand.serie == 1) "Tag" else "Tage"}",
                    if (bonusstand.verfuegbar) "Dein Dreh ist bereit" else "Für heute gedreht",
                    m,
                )
            }
            add { m: Modifier ->
                Kennzahlkachel(
                    "Sortiment",
                    "${s.sortiment.size} ${if (s.sortiment.size == 1) "Stück" else "Stücke"}",
                    wechsel,
                    m,
                    farbe = Farben.Blau,
                )
            }
            add { m: Modifier ->
                Kennzahlkachel(
                    "Fahrzeuggutscheine",
                    "$gutscheine",
                    if (gutscheine > 0) "im Autohaus einlösen" else "der nächste mit dem Aufstieg",
                    m,
                    farbe = Farben.Gruen,
                )
            }
            if (s.zulagen.deckel > 0) {
                add { m: Modifier ->
                    Kennzahlkachel(
                        "Wochenzulagen",
                        "${s.zulagen.verbraucht} / ${s.zulagen.deckel}",
                        "Credits · " + (s.zulagen.naechsterWechsel?.let { wochenrest(it) } ?: "diese Woche"),
                        m,
                        farbe = Farben.Violett,
                    )
                }
            }
        },
    )

    // Premium in der Übersicht: nur der Anreißer, als Wink mit Amberkante — das
    // einzige Angebot der Seite, das echtes Geld kostet. Das Angebot steht im eigenen Bereich.
    Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                KontoZeichen.Stern,
                contentDescription = null,
                tint = Farben.AmberHell,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                Text("PagerSpass Premium", style = Schrift.Normal.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Farben.Text)
                Leise(
                    if (aktiv) {
                        "Läuft auf diesem Konto — hier steht, was alles dazugehört."
                    } else {
                        "Mehr Platz für eigene Alarm- und Ausrückeordnungen, dazu Rahmen, Melder, Muster und " +
                            "Titel, die es sonst nirgends gibt."
                    },
                )
            }
        }
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

    // Der Umfang in vier Zahlen — zwischen Bühne und Vitrine, als Tafel wie in der
    // Übersicht: oben, was Premium ist, unten liegt es ausgelegt.
    val katalog = de.pagerspass.pagerspass.melder.Melderkatalog
    val rahmenZahl = de.pagerspass.pagerspass.ui.schmuck.Schmuck.RAHMEN.count { it.premium }
    val musterZahl = de.pagerspass.pagerspass.ui.schmuck.Schmuck.KOPFMUSTER.count { it.premium }
    // Bauformen, dazu acht Funkgerätegehäuse, sechs Schichtkarten, sechs Titel — wie im Web.
    val weitere = katalog.BAUFORMEN.count { it.premium } + 8 + 6 + 6
    Kennzahltafel(
        listOf(
            { m -> Kennzahlkachel("Melder", "${katalog.GESICHTER.count { it.premium }}", "Gesichter fürs Gerät", m) },
            { m -> Kennzahlkachel("Alarmtöne", "${katalog.TOENE.count { it.premium }}", "voller, nicht lauter", m, farbe = Farben.Violett) },
            { m -> Kennzahlkachel("Schmuck", "${rahmenZahl + musterZahl}", "$rahmenZahl Rahmen · $musterZahl Muster", m, farbe = Farben.Blau) },
            { m -> Kennzahlkachel("Weiteres", "$weitere", "Geräte, Karten, Titel", m, farbe = Farben.Gruen) },
        ),
    )

    Vitrine()

    if (premium.vorteile.isNotEmpty()) {
        Abschnitt("Alles, was dazugehört") {
            Kasten(abstandInnen = Abstand.Winzig) {
                premium.vorteile.forEach { v -> Text("· $v", style = Schrift.Klein, color = Farben.TextLeise) }
            }
        }
    }

    Abschnitt(if (aktiv || offen) "Dein Abo" else "Pläne") {
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

/** Die Zeichen der Reiterreihe des Ladens — dieselben Pfade wie in `ShopView.vue`. */
private object ShopZeichen {
    val Uebersicht = strich(
        "shop-uebersicht",
        "M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z",
        "M15 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z",
        "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1Z",
        "M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1Z",
    )
    val Sortiment = strich("shop-sortiment", "M4 8h16l-1 13H5L4 8Z", "M8 8V6a4 4 0 0 1 8 0v2")
    val Fahrzeuge = strich(
        "shop-fahrzeuge",
        "M3 15V9l3-4h10l4 4v6",
        "M3 12h17M7 15v3M17 15v3",
        "M5 15a2 2 0 1 0 4 0 2 2 0 1 0-4 0",
        "M15 15a2 2 0 1 0 4 0 2 2 0 1 0-4 0",
    )
    val Guthaben = strich(
        "shop-guthaben",
        "M3 6h15a2 2 0 0 1 2 2v11H5a2 2 0 0 1-2-2V6Z",
        "M3 6l12-3v3M15 11h6v5h-6a2.5 2.5 0 0 1 0-5Z",
    )

    fun zu(bereich: String) = when (bereich) {
        PREMIUM -> KontoZeichen.Stern
        SORTIMENT -> Sortiment
        FAHRZEUGE -> Fahrzeuge
        GUTHABEN -> Guthaben
        else -> Uebersicht
    }
}

/**
 * Der Eingang des Ladens — `ShopPortal.vue` in der Handy-Fassung: Etikett und Name
 * links, die Kasse rechts als eigene Kachel, der Satz unter beiden. Am Handy der
 * Kopf des Tablets statt eines goldenen Bands (mobil.css, `.portal`); die Kasse
 * bleibt amber — sie ist die Zahl, um die es geht.
 */
@Composable
internal fun Ladenportal(etikett: String, titel: String, satz: String, stand: String, waehrung: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .clip(de.pagerspass.pagerspass.ui.theme.Rundung.Normal)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(listOf(Farben.FlaecheHoch, Farben.Flaeche)),
            )
            .border(1.dp, Farben.Rand, de.pagerspass.pagerspass.ui.theme.Rundung.Normal)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                Etikett(etikett)
                Text(
                    titel,
                    style = Schrift.Schlagzeile.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
                    color = Farben.Text,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .background(Farben.BgTief.copy(alpha = 0.7f), de.pagerspass.pagerspass.ui.theme.Rundung.Klein)
                    .border(1.dp, Farben.AmberTief, de.pagerspass.pagerspass.ui.theme.Rundung.Klein)
                    .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
            ) {
                Text(
                    stand,
                    style = Schrift.Schlagzeile.copy(
                        fontFamily = Schrift.Mono,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        lineHeight = Schrift.SCHLAGZEILE,
                    ),
                    color = Farben.Amber,
                    maxLines = 1,
                )
                Text(waehrung.uppercase(), style = Schrift.Etikett.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = Farben.TextLeise)
            }
        }
        Text(satz, style = Schrift.Klein, color = Farben.TextLeise)
    }
}
