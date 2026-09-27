package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.netz.Abostand
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.schmuck.Kataloge
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.schmuck.Schmuck
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

/**
 * Das Premium-Schaufenster — der eigene Bereich des Abos im Shop. Übertragen aus
 * `components/shop/PremiumSchaufenster.vue`.
 *
 * <b>Die Ware steht im Fenster, nicht in einer Aufzählung:</b> dieselben Rahmen,
 * Gesichter, Muster, Karten und Titel, die man nach dem Abschluss trägt,
 * gezeichnet von denselben Bausteinen wie im Profil.
 *
 * <b>Drei Regeln.</b> Keine Zahl von Hand — gezählt wird aus den Katalogen. Kein
 * Knopf, der nichts tut — solange der Verkauf zu ist, steht hier der Grund. Und die
 * ehrliche Zeile bleibt: Was Premium nicht tut, steht genauso groß da wie das, was
 * es tut.
 *
 * <b>Bezahlt wird im Browser.</b> Die Kasse ist eine Stripe-Seite; die App öffnet
 * sie und holt den Stand frisch, sobald man zurückkommt.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumSchaufenster(
    abo: Bereichsstand<Abostand>,
    laeuft: Boolean,
    fehler: String?,
    beiKaufen: (plan: String, sofortAusfuehren: Boolean) -> Unit,
    beiVerwalten: () -> Unit,
    beiRechtstext: (String) -> Unit,
    beiVertrag: (String) -> Unit,
) {
    val stand = abo.inhalt
    val aktiv = stand?.aktiv == true
    // „Zahlung offen" ist ein Verwaltungszustand — ein zweiter Kauf legte bei
    // Stripe ein zweites Abo auf denselben Kunden an.
    val offen = stand?.status == "PastDue"
    val kaufbar = stand?.kaufVerfuegbar == true && !aktiv && !offen
    val monat = stand?.monatspreisCent ?: 0
    val jahr = stand?.jahrespreisCent ?: 0
    val kasse = remember { BringIntoViewRequester() }
    val bereich = rememberCoroutineScope()
    var sofortAusfuehren by remember { mutableStateOf(false) }

    val rahmen = Schmuck.RAHMEN.filter { it.premium }
    val gesichter = Kataloge.MELDER_GESICHTER.filter { it.premium }
    val muster = Schmuck.KOPFMUSTER.filter { it.premium }
    val bauformen = Kataloge.MELDER_BAUFORMEN.filter { it.premium }
    val funkgeraete = Kataloge.FUNKGERAETE.filter { it.premium }
    val karten = Kataloge.SCHICHTKARTEN_DESIGNS.filter { it.premium }
    val titel = Kataloge.VERGABETITEL.filter { it.first in Kataloge.PREMIUM_TITEL_IDS }
    val toene = Kataloge.MELDER_TOENE.filter { it.premium }

    // Die Bühne.
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = if (aktiv) Farben.Amber else Farben.AmberTief)
            .padding(Abstand.Gross),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("★", style = Schrift.Schlagzeile, color = Farben.Amber, modifier = Modifier.weight(1f))
            Marke(
                text = when {
                    aktiv -> "Aktiv"
                    offen -> "Zahlung offen"
                    stand?.kaufVerfuegbar == true -> "Optional"
                    else -> "Nicht verfügbar"
                },
                farbe = when {
                    aktiv -> Farben.GruenHell
                    offen -> Farben.SignalHell
                    else -> Farben.TextLeise
                },
            )
        }
        Etikett("Abo · monatlich kündbar")
        Text("PagerSpass Premium", style = Schrift.Schlagzeile, color = Farben.AmberHell)
        Text(
            "Dein Dienst bleibt derselbe — dein Auftritt nicht. Mehr Platz für eigene Alarm- und " +
                "Ausrückeordnungen, dazu Rahmen, Melder, Muster, Karten und Titel, die es sonst nirgends gibt.",
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
        if (kaufbar && monat > 0) {
            Text(
                "ab ${euro(Math.round(jahr / 12.0).toInt())} im Monat · im Jahresplan",
                style = Schrift.Normal,
                color = Farben.Text,
            )
        }
        if (kaufbar) {
            Knopf("Pläne ansehen", { bereich.launch { kasse.bringIntoView() } }, art = Knopfart.Haupt, kompakt = true)
        }
    }

    // Was das Abo umfasst, in vier Zahlen — gezählt, nicht geschrieben.
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        listOf(
            gesichter.size to "Melder-Gesichter",
            toene.size to "Alarmtöne",
            (rahmen.size + muster.size) to "Rahmen & Muster",
            (bauformen.size + funkgeraete.size + karten.size + titel.size) to "Geräte, Karten, Titel",
        ).forEach { (wert, was) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).flaeche(ecke = 9.dp).padding(Abstand.Klein),
            ) {
                Text(wert.toString(), style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
                Text(was, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 2)
            }
        }
    }

    if (fehler != null && stand == null) {
        Text(fehler, style = Schrift.MonoKlein, color = Farben.SignalHell)
    } else if (stand == null) {
        SehrLeise("Premium wird geladen …")
    }

    Stueck("Dein Kartenkopf", "neu") {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.BgTief, Rundung.Klein)
                .kopfband("premium-lagekarte", Wappen.ton("schaufenster", 3))
                .padding(Abstand.Normal),
        ) {
            Text("Wo du Dienst tust", style = Schrift.Gross, color = Farben.Text)
            Text("Dein Ausschnitt, hinter deinem Namen.", style = Schrift.Klein, color = Farben.TextLeise)
        }
        Satz(
            "Statt eines gezeichneten Musters liegt hinter deinem Profilkopf eine echte Karte — den " +
                "Ausschnitt schiebst du dir selbst zurecht. Näher als eine Ortsansicht geht es nicht: Dort " +
                "soll eine Gegend stehen, keine Anschrift.",
        )
    }

    Stueck("Profilrahmen", rahmen.size.toString()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            rahmen.forEach { r ->
                Kontobild(
                    kennung = "schaufenster",
                    anzeigename = "P S",
                    rahmen = r.id,
                    wappenfarbe = 3,
                    groesse = 44.dp,
                )
            }
        }
        Satz("Zwei davon leuchten: Beim Lauflicht wandert der Schein um dein Wappen, beim Nordlicht wechselt er die Farbe.")
    }

    Stueck("Melder-Gesichter", gesichter.size.toString()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            gesichter.forEach { g -> Minimelder(g.gehaeuse, g.lcd, breite = 26.dp) }
        }
        Satz("Die Farbe gilt auf jedem Gerät — vom Piepser über die Einsatzuhr bis zum Wandtableau.")
    }

    Stueck("Kopfmuster", muster.size.toString()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            muster.filter { it.id != "premium-karte" }.forEach { m ->
                Box(
                    Modifier
                        .size(width = 96.dp, height = 28.dp)
                        .clip(Rundung.Winzig)
                        .background(Farben.BgTief)
                        .kopfband(m.id, Wappen.ton("schaufenster", 3)),
                )
            }
        }
        Satz("Das Farbband über deinem Profil. Sieben gezeichnete — und als achtes der Kartenkopf oben.")
    }

    Stueck("Titel & goldener Name", titel.size.toString()) {
        titel.forEach { (_, name) ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Kontoname(name = "Alex Bremer", premium = true)
                Text(name, style = Schrift.Winzig, color = Farben.AmberHell)
            }
        }
        Satz("Dein Name wird golden und trägt einen Stern — am Brett, in der Lobby und in jeder Mannschaftsliste.")
    }

    Stueck("Geräte", (bauformen.size + funkgeraete.size).toString()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            bauformen.forEach { Marke(it.name, farbe = Farben.AmberHell) }
            funkgeraete.forEach { Marke(it.name) }
        }
        Satz(
            "Drei andere Arten Melder — quer in der Hand, am Handgelenk, im Flur — und acht Gehäuse für " +
                "dein Funkgerät, am Rechner wie am Handy. Am Funk ändert sich nichts. Leucht-, Lamellen- und " +
                "Bogenmelder gibt es nicht im Abo: Die erspielst du in der Laufbahn ab Stufe 90.",
        )
    }

    Stueck("Werkstatt · Gehäuse", "18") {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Gross)) {
            Minimelder(androidx.compose.ui.graphics.Color(0xFF16223A), androidx.compose.ui.graphics.Color(0xFF7FB7E8), breite = 30.dp)
            Minimelder(androidx.compose.ui.graphics.Color(0xFF7D4930), androidx.compose.ui.graphics.Color(0xFFE1B768), breite = 30.dp)
            Minimelder(androidx.compose.ui.graphics.Color(0xFFD7DADE), androidx.compose.ui.graphics.Color(0xFF1B1F25), breite = 30.dp)
        }
        Satz(
            "In der Gehäusewerkstatt ziehst du dir deinen Melder selbst zusammen — achtzehn Bauteile von der " +
                "Signalleuchte bis zum Typenschild, jedes in zwei bis drei Bauarten, dazu Maße, Ecken, vier " +
                "Oberflächen und jede Farbe. Und es ist ein echtes Gerät: Die Quittiertaste quittiert, das " +
                "Anzeigefeld zeigt die Meldung, die Signalleuchte blinkt im Alarm. Nur für dich — und mit einem " +
                "Code zum Weitergeben. Anfangen kannst du beim leeren Gehäuse oder bei einer von fünf Vorlagen; " +
                "die Abnahme sagt dir, was dem Gerät noch fehlt. Gebaut wird am Rechner, getragen überall.",
        )
    }

    Stueck("Werkstatt · Klang", "2") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            listOf("Raster zeichnen", "Datei hochladen", "Selbst einsprechen").forEach { Marke(it, farbe = Farben.AmberHell) }
            listOf("Schneiden", "Ruhe einstellen", "Code weitergeben").forEach { Marke(it) }
        }
        Satz(
            "Zwei Werkbänke für denselben Zweck. In der Tonwerkstatt klickst du deinen Melderton ins Raster — " +
                "sechzehn Schritte, acht Höhen, Pausen dazwischen. In der Klangwerkstatt bringst du ihn mit: als " +
                "Datei oder über dein Mikrofon, zugeschnitten und mit der Ruhe dazwischen, die du willst. Alles " +
                "bleibt auf deinem Gerät, und lauter als die anderen Töne ist er nicht.",
        )
    }

    Stueck("Handy-Funkbegleiter", funkgeraete.size.toString()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            funkgeraete.forEach { f ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(44.dp)
                        .background(Farben.FlaecheAktiv, Rundung.Winzig)
                        .border(1.dp, Farben.RandHell, Rundung.Winzig)
                        .padding(Abstand.Winzig),
                ) {
                    Text("FUNK", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.GruenHell)
                    Box(Modifier.height(10.dp))
                    Text(f.name, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 1)
                }
            }
        }
        Satz(
            "Dein Handy wird per QR-Code zum Funkgerät und Melder deines Platzes — ohne zweite Anmeldung, mit " +
                "Mitteilung bei Alarm auch bei dunklem Bildschirm. Es zeigt dasselbe Gerät, das du am Rechner " +
                "gewählt hast, nur in voller Größe.",
        )
    }

    Stueck("Schichtkarte", karten.size.toString()) {
        Kartenproben(karten.map { it.id })
        Satz("Dazu deine eigene Zeile auf dem Bild, das du nach der Schicht teilst — dein Motto, unter dem Namen der Leitstelle.")
    }

    if (stand != null) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Gross),
        ) {
            Text("Alles, was dazugehört", style = Schrift.Gross, color = Farben.Text)
            stand.vorteile.forEach { Text("• $it", style = Schrift.Klein, color = Farben.TextLeise) }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(kasse)
                .flaeche(randfarbe = Farben.AmberTief)
                .padding(Abstand.Gross),
        ) {
            when {
                aktiv -> {
                    Text("Danke — Premium läuft auf diesem Konto.", style = Schrift.Gross, color = Farben.AmberHell)
                    SehrLeise(
                        "Verwaltet wird das Abo im Konto: Plan wechseln, Zahlungsweg ändern, kündigen. Was du " +
                            "hier siehst, gehört alles dir.",
                    )
                    Knopf("Abo verwalten", beiVerwalten, art = Knopfart.Haupt)
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Gross)) {
                        Textweg("Verträge hier kündigen", { beiVertrag("vertrag-kuendigen") })
                        Textweg("Vertrag widerrufen", { beiVertrag("vertrag-widerrufen") })
                    }
                }

                offen -> {
                    Text(
                        "Für dein Abo ist eine Zahlung offen. Solange sie offen ist, ruhen die Premium-Vorteile — " +
                            "gekündigt ist deshalb nichts.",
                        style = Schrift.Klein,
                        color = Farben.SignalHell,
                    )
                    Knopf("Zahlung in Ordnung bringen", beiVerwalten, art = Knopfart.Haupt)
                }

                kaufbar -> {
                    // Die Erklärung zum sofortigen Beginn — nicht vorausgewählt: Ein
                    // voreingestellter Haken ist keine Erklärung, sondern eine Vermutung.
                    Hakenzeile(
                        text = "Ich verlange ausdrücklich, dass ihr sofort — also vor Ablauf der vierzehntägigen " +
                            "Widerrufsfrist — mit der Leistung beginnt und Premium freischaltet. Mir ist bekannt, " +
                            "dass ich bei einem Widerruf einen anteiligen Betrag für die Zeit bis zum Widerruf zahle.",
                        an = sofortAusfuehren,
                        beiWechsel = { sofortAusfuehren = it },
                    )
                    Textweg("Widerrufsbelehrung", { beiRechtstext("agb") })

                    Plan(
                        name = "Monatlich",
                        preis = euro(monat),
                        unter = "je Monat",
                        bedingung = "Endpreis · verlängert sich automatisch um einen Monat · jederzeit zum Ende " +
                            "des Zeitraums kündbar",
                        knopf = if (laeuft) "Einen Moment …" else "Monatlich abonnieren",
                        haupt = false,
                        aktiv = !laeuft && sofortAusfuehren,
                        beiDruck = { beiKaufen("Monthly", sofortAusfuehren) },
                    )
                    Plan(
                        name = "Jährlich",
                        preis = euro(jahr),
                        unter = "je Jahr · das sind ${euro(Math.round(jahr / 12.0).toInt())} im Monat",
                        bedingung = "Endpreis · verlängert sich automatisch um ein Jahr · jederzeit zum Ende " +
                            "des Zeitraums kündbar",
                        knopf = if (laeuft) "Einen Moment …" else "Jährlich abonnieren",
                        haupt = true,
                        aktiv = !laeuft && sofortAusfuehren,
                        beiDruck = { beiKaufen("Yearly", sofortAusfuehren) },
                    )
                    SehrLeise("Verbindlich bestellst du erst auf der folgenden Bezahlseite von Stripe. Es gelten unsere AGB.")
                    Textweg("AGB", { beiRechtstext("agb") })
                    if (fehler != null) Text(fehler, style = Schrift.MonoKlein, color = Farben.SignalHell)
                    SehrLeise(
                        "Bezahlt wird über Stripe; PagerSpass sieht deine Kartendaten nie. Die Zierstücke " +
                            "bleiben, solange das Abo läuft — was du dir mit Credits gekauft oder erspielt hast, " +
                            "bleibt für immer.",
                    )
                }

                else -> {
                    Text("Premium ist derzeit nicht zu haben.", style = Schrift.Gross, color = Farben.Text)
                    SehrLeise(
                        "Neue Abos können momentan nicht abgeschlossen werden. Laufende Abos und bereits " +
                            "gutgeschriebene Premium-Zeit bleiben davon unberührt — alles oben steht dir dann " +
                            "weiterhin offen.",
                    )
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Gross),
    ) {
        Text("Was Premium nicht tut", style = Schrift.Gross, color = Farben.Text)
        Ehrlich(
            "Keine schnelleren Fahrzeuge, keine bessere Hilfsfrist.",
            "Was in der Schicht zählt, zählt für alle gleich.",
        )
        Ehrlich(
            "Keine Punkte, keine Abkürzung in der Laufbahn.",
            "Erspielte Rahmen, Wappen und Melder bleiben erspielt — sie sind hier nicht zu kaufen, auch nicht " +
                "die späten Laufbahngeräte ab Stufe 90.",
        )
        Ehrlich(
            "Kein lauterer Melder, kein Vordrängen im Funk.",
            "Die Abo-Töne klingen voller, nicht lauter.",
        )
    }
}

/** „4,99 €". */
private fun euro(cent: Int): String =
    NumberFormat.getCurrencyInstance(Locale.GERMANY).format(cent / 100.0)

/** Eine Kachel im Fenster: Kopf mit Zahl, Bühne, ein Satz dazu. */
@Composable
private fun Stueck(titel: String, zahl: String, inhalt: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Gross),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titel, style = Schrift.Gross, color = Farben.Text, modifier = Modifier.weight(1f))
            Text(zahl, style = Schrift.MonoKlein, color = Farben.AmberHell)
        }
        inhalt()
    }
}

@Composable
private fun Satz(text: String) {
    Text(text, style = Schrift.Klein, color = Farben.TextLeise)
}

@Composable
private fun Ehrlich(fett: String, rest: String) {
    Column {
        Text(fett, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        Text(rest, style = Schrift.Klein, color = Farben.TextLeise)
    }
}

@Composable
private fun Plan(
    name: String,
    preis: String,
    unter: String,
    bedingung: String,
    knopf: String,
    haupt: Boolean,
    aktiv: Boolean,
    beiDruck: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 12.dp, randfarbe = if (haupt) Farben.Amber else Farben.Rand)
            .padding(Abstand.Normal),
    ) {
        Etikett(name)
        Text(preis, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold, fontSize = Schrift.TITEL), color = Farben.Text)
        Text(unter, style = Schrift.Klein, color = Farben.TextLeise)
        SehrLeise(bedingung)
        Knopf(knopf, beiDruck, art = if (haupt) Knopfart.Haupt else Knopfart.Normal, aktiv = aktiv, breit = true)
    }
}

/**
 * Die Abo-Schichtkarten als echte Karten, nicht als Farbkärtchen: gezeichnet von
 * derselben Stelle, die nach der Schicht das Bild macht — mit erkennbar erfundenen
 * Werten („Leitstelle Musterkreis").
 */
@Composable
private fun Kartenproben(designs: List<String>) {
    var bilder by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    LaunchedEffect(designs) {
        val beispiel = Schichtkartendaten(
            leitstelle = "Leitstelle Musterkreis",
            code = "MK-1",
            datum = tag(java.time.Instant.now().toString()),
            dienstdauer = "2 Std. 00 Min.",
            einsaetze = 12,
            abgeschlossen = 12,
            disposition = "0:38",
            hilfsfrist = "7:24",
            funksprueche = 84,
            punkte = 180,
            rang = "Disponent/in",
        )
        bilder = withContext(Dispatchers.Default) {
            designs.mapNotNull { id ->
                runCatching { id to schichtkarteZeichnen(beispiel, id).asImageBitmap() }.getOrNull()
            }.toMap()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        designs.chunked(2).forEach { paar ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
                paar.forEach { id ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1200f / 630f)
                            .clip(Rundung.Winzig)
                            .background(Farben.BgTief),
                    ) {
                        bilder[id]?.let { b ->
                            Image(
                                bitmap = b,
                                contentDescription = Kataloge.SCHICHTKARTEN_DESIGNS.firstOrNull { it.id == id }?.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (paar.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}
