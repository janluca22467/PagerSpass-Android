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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Buchdaten
import de.pagerspass.pagerspass.mobil.Wachendaten
import de.pagerspass.pagerspass.netz.Abzeichen
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.netz.Schicht
import de.pagerspass.pagerspass.netz.Shop
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
import de.pagerspass.pagerspass.ui.schmuck.Profilbanner
import de.pagerspass.pagerspass.ui.schmuck.Profilzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
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

// Das Dienstbuch steht in `DienstbuchSeiten.kt` — mit fünf Reitern ist es
// eine eigene Seite und kein Abschnitt dieser Datei mehr.

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

// --------------------------------------------------------------------- Shop

/**
 * Der Shop.
 *
 * <b>Der Kontostand steht im Kopf des Shops, nicht in der Tableiste.</b> Die
 * Leiste trägt Marken für Offenes, keine Kontostände.
 *
 * <b>Gekauft wird hier noch nicht.</b> Der Kaufweg (`/shop/kauf`) hängt an den
 * Kosmetik-Katalogen, die die App noch nicht kennt — ein Knopf, der einen Kauf
 * auslöst, dessen Ergebnis man nirgends sieht, wäre schlimmer als keiner.
 * Sortiment, Preise und Besitzstand stehen echt da.
 */
@Composable
fun ShopSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    shop: Bereichsstand<Shop?> = Bereichsstand(),
    laeuft: Boolean = false,
    beiLaden: () -> Unit = {},
    beiKauf: (String) -> Unit = {},
    beiTagesbonus: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Shop",
            knoepfe = {
                val stand = shop.inhalt
                if (stand != null) Marke("${zahl(stand.credits)} Credits", farbe = Farben.AmberHell)
            },
        )

        Bereich(
            laedt = shop.laedt,
            fehler = shop.fehler,
            inhalt = shop.inhalt,
            beiErneut = beiLaden,
        ) { stand ->
            Kasten(marke = stand.tagesbonus.verfuegbar, wartet = true, abstandInnen = Abstand.Klein) {
                Text("Tagesbonus", style = Schrift.Gross, color = Farben.Text)
                SehrLeise(
                    if (stand.tagesbonus.verfuegbar) {
                        "Heute noch nicht abgeholt · Serie: ${stand.tagesbonus.serie} Tage"
                    } else {
                        "Heute schon abgeholt · Serie: ${stand.tagesbonus.serie} Tage"
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        aufschrift = "Abholen",
                        beiDruck = beiTagesbonus,
                        art = Knopfart.Haupt,
                        aktiv = !laeuft && stand.tagesbonus.verfuegbar,
                        kompakt = true,
                    )
                }
            }

            Abschnitt("Sortiment dieser Woche") {
                if (stand.sortiment.isEmpty()) {
                    Leerhinweis("Diese Woche steht nichts im Sortiment.")
                } else {
                    stand.sortiment.forEach { artikel ->
                        Artikelzeile(
                            artikel = artikel,
                            credits = stand.credits,
                            laeuft = laeuft,
                            beiKauf = beiKauf,
                        )
                    }
                }
            }

            if (stand.imBesitz.isNotEmpty()) {
                Abschnitt("In deinem Besitz") {
                    Kasten(abstandInnen = Abstand.Winzig) {
                        stand.imBesitz.forEach { SehrLeise("${it.name} · ${it.art}") }
                    }
                }
            }
        }
    }
}

/**
 * Ein Artikel mit seinem Kaufknopf.
 *
 * <b>Der Knopf sagt, warum er nicht geht.</b> „Kaufen" ausgegraut heißt für den,
 * der davorsteht, gar nichts — „150 C fehlen" beantwortet die Frage, die er sich
 * gerade stellt. Ein gesperrter Knopf ohne Grund ist der häufigste Weg, jemanden
 * den Fehler bei sich selbst suchen zu lassen.
 */
@Composable
private fun Artikelzeile(
    artikel: de.pagerspass.pagerspass.netz.Shopartikel,
    credits: Int,
    laeuft: Boolean,
    beiKauf: (String) -> Unit,
) {
    val fehlt = artikel.preis - credits

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
                text = artikel.name,
                style = Schrift.Normal,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SehrLeise(artikel.art)
        }

        when {
            artikel.gekauft -> Marke("Gekauft", farbe = Farben.GruenHell)

            fehlt > 0 -> Knopf(
                aufschrift = "$fehlt C fehlen",
                beiDruck = {},
                aktiv = false,
                kompakt = true,
            )

            else -> Knopf(
                aufschrift = "${artikel.preis} C",
                beiDruck = { beiKauf(artikel.id) },
                art = Knopfart.Haupt,
                aktiv = !laeuft,
                kompakt = true,
            )
        }
    }
}

// -------------------------------------------------------------------- Konto

/**
 * Das Konto — die eigenen Angaben, die Einstellungen und der Weg hinaus.
 *
 * <b>„Konto löschen" gehört hierher und nirgendwo sonst.</b> Google Play verlangt
 * für jede App, in der man ein Konto anlegen kann, zwei Wege zur Löschung: einen
 * in der App und einen über eine öffentlich erreichbare Adresse.
 *
 * <b>Was die App noch nicht selbst kann, führt in den Browser</b> — und sagt das
 * auch. Ein Weg, der eine Seite öffnet, die es in der App nicht gibt, ist besser
 * als eine Zeile, die nichts tut; aber er muss vorher sagen, wohin er geht.
 */
@Composable
fun KontoSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    profil: Bereichsstand<Profil?> = Bereichsstand(),
    server: String = "",
    postfachFrei: Boolean = false,
    beiProfilLaden: () -> Unit = {},
    beiAbmelden: () -> Unit = {},
    beiLoeschen: () -> Unit = {},
    beiRechtstext: (String) -> Unit = {},
    beiProfil: () -> Unit = {},
    beiPrivatsphaere: () -> Unit = {},
    beiPostfach: () -> Unit = {},
    beiMitteilungen: () -> Unit = {},
    beiBegleiter: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiProfilLaden() }
    val p = profil.inhalt

    Seite(modifier = modifier, unterrand = unterrand) {
        // Das Banner statt einer Kopfzeile: Es ist dasselbe, das andere im
        // Profil sehen. Wer die Kontoseite öffnet, soll sein Konto sehen und
        // nicht die Vorlage, auf der jedes Konto gleich aussieht.
        Profilbanner(
            kennung = konto?.kennung.orEmpty(),
            anzeigename = konto?.anzeigename.orEmpty(),
            benutzername = konto?.benutzername,
            augenbraue = "Dein Konto",
            wappen = p?.wappen ?: "Keines",
            wappenfarbe = p?.wappenfarbe ?: 0,
            kopfmuster = p?.kopfmuster ?: "keines",
            profilrahmen = p?.profilrahmen ?: "keiner",
            bildAdresse = bildweg(server, p?.profilbild),
            premium = konto?.premiumAktiv == true,
            teammitglied = konto?.teammitglied == true,
            marken = listOfNotNull(
                konto?.let { "${it.rang} · Stufe ${it.level}" },
                if (konto?.premiumAktiv == true) "Premium" else "Free",
                p?.gemeinschaft,
            ),
            werte = listOfNotNull(
                konto?.let { "Credits" to zahl(it.credits) },
                konto?.let { "Erfahrung" to "${zahl(it.erfahrung)} XP" },
                konto?.erstelltUm?.let { "Dabei seit" to tag(it) },
            ),
            knoepfe = { Knopf("Profil einrichten", beiProfil, kompakt = true) },
        )

        /*
         * <b>Der Begleiter steht oben und nicht bei den Einstellungen.</b> Er
         * ist keine Einstellung, sondern ein Dienstweg: Wer ihn öffnet, hat
         * gerade einen QR-Code vor sich und will in zwei Griffen funken. Alles
         * darunter kann warten.
         */
        Abschnitt("Dienst auf diesem Gerät") {
            Wegzeile(
                "Mobiler Begleiter",
                beiBegleiter,
                unterzeile = "QR-Code scannen: Funkgerät und Melder aufs Handy",
                // Das Funkzeichen und nicht das Handy: Das Handy trägt schon die
                // Privatsphäre-Zeile darunter, und zweimal dasselbe Zeichen auf
                // einem Bildschirm heißt, dass keines mehr etwas sagt.
                zeichen = Zeichen.Funk,
            )
        }

        Abschnitt("Einstellungen") {
            Wegzeile(
                "Profil",
                beiProfil,
                unterzeile = "Wappen, Farbe, Rahmen, Kopfmuster",
                zeichen = Zeichen.Gemeinschaft,
            )
            Wegzeile(
                "Privatsphäre",
                beiPrivatsphaere,
                unterzeile = "Wer was von dir sieht",
                zeichen = Zeichen.Handy,
            )
            Wegzeile(
                "Mitteilungen",
                beiMitteilungen,
                unterzeile = "Was aufs Gerät darf",
                zeichen = Zeichen.Melder,
            )
            Wegzeile(
                "Postfach",
                beiPostfach,
                unterzeile = if (postfachFrei) {
                    "Nachrichten der Verwaltung"
                } else {
                    "Vom Betrieb — dein Postfach ist noch nicht freigeschaltet"
                },
                zeichen = Zeichen.Forum,
            )
        }

        Abschnitt("Rechtliches") {
            Wegzeile("Impressum", { beiRechtstext("impressum") }, zeichen = Zeichen.Wiki)
            Wegzeile("Datenschutz", { beiRechtstext("datenschutz") }, zeichen = Zeichen.Wiki)
            Wegzeile(
                "Nutzungsbedingungen",
                { beiRechtstext("nutzungsbedingungen") },
                zeichen = Zeichen.Wiki,
            )
            Wegzeile("AGB und Widerruf", { beiRechtstext("agb") }, zeichen = Zeichen.Wiki)
        }

        Abschnitt("Konto") {
            Text(
                text = "Abmelden trennt nur dieses Gerät. Löschen entfernt dein Konto " +
                    "dauerhaft — das lässt sich nicht rückgängig machen.",
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Abmelden", beiAbmelden, art = Knopfart.Leise)
                Knopf("Konto löschen", beiLoeschen, art = Knopfart.Gefahr)
            }
        }
    }
}
