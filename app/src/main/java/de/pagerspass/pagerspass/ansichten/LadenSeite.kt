package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Garagendaten
import de.pagerspass.pagerspass.mobil.rememberTonprobe
import de.pagerspass.pagerspass.netz.Abostand
import de.pagerspass.pagerspass.netz.Beschenkbarer
import de.pagerspass.pagerspass.netz.Codegutschrift
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Gluecksradergebnis
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Shop
import de.pagerspass.pagerspass.netz.Shopartikel
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Markenzahl
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Kataloge
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Der Shop — das tägliche Schaufenster. Übertragen aus `views/ShopView.vue`.
 *
 * <b>Aufgebaut wie ein Ladenlokal, nicht wie eine Liste:</b> oben das Portal mit
 * dem Credit-Stand und der Uhr des Sortiments, darunter fünf feste Bereiche. Die
 * Übersicht sammelt Abo und Verdienst, Premium ist das Schaufenster des Abos, das
 * Sortiment zeigt die Tagesstücke, Fahrzeuge führt ins Autohaus und Guthaben
 * erklärt Ein- und Ausgänge.
 *
 * <b>Kaufbar ist nur Zierde.</b> Was die Laufbahn verleiht, bleibt unverkäuflich;
 * ein Kauf ist endgültig und will deshalb zweimal gedrückt sein.
 */

/** Die fünf Bereiche des Ladens. */
enum class Ladenbereich(val weg: String, val titel: String, val kennung: String, val kopf: String, val satz: String) {
    Uebersicht(
        "shop", "Übersicht", "DEIN SHOP", "Heute für dich",
        "Premium, Tagesbonus und Wochenaufträge auf einen Blick.",
    ),
    Premium(
        "shop/premium", "Premium", "DAS ABO", "Dein Auftritt im Dienst",
        "Alles, was zum Abo gehört – und was es ausdrücklich nicht tut.",
    ),
    Sortiment(
        "shop/sortiment", "Sortiment", "TAGESAUSWAHL", "Zierstücke des Tages",
        "Acht wechselnde Stücke – kaufen, verschenken oder direkt anlegen.",
    ),
    Fahrzeuge(
        "shop/fahrzeuge", "Fahrzeuge", "AUTOHAUS", "Deine nächsten Fahrzeuge",
        "Tagesangebot, Gutscheine und der vollständige Fahrzeugkatalog.",
    ),
    Guthaben(
        "shop/guthaben", "Guthaben", "KONTOBEWEGUNGEN", "Credits im Blick",
        "Nachvollziehen, was hereinkam, und Aktionscodes einlösen.",
    ),
}

/** Was der Laden tun kann — die Handlungen des Speichers, als Griffe gereicht. */
class Ladengriffe(
    val kaufen: suspend (String) -> Result<Unit>,
    val drehen: suspend () -> Result<Gluecksradergebnis>,
    val codeEinloesen: suspend (String) -> Result<Codegutschrift>,
    val beschenkbare: suspend () -> Result<List<Beschenkbarer>>,
    val schenken: suspend (String, String) -> Result<Unit>,
    val fahrzeugWaehlen: suspend (Fahrzeugvorlage) -> Result<Unit>,
    val fahrzeugKaufen: suspend (Fahrzeugvorlage) -> Result<Unit>,
    val kasse: suspend (String, Boolean) -> Result<String>,
)

@Composable
fun LadenSeite(
    unterrand: Dp,
    anfang: Ladenbereich,
    konto: Konto?,
    shop: Bereichsstand<Shop?>,
    garage: Bereichsstand<Garagendaten>,
    katalog: List<Fahrzeugvorlage>,
    abo: Bereichsstand<Abostand>,
    griffe: Ladengriffe,
    beiLaden: () -> Unit,
    beiAboLaden: (Boolean) -> Unit,
    beiBrowser: (String) -> Unit,
    beiAnlegen: () -> Unit,
    beiAboVerwalten: () -> Unit,
    beiRechtstext: (String) -> Unit,
    beiVertrag: (String) -> Unit,
    beiAusbildung: () -> Unit,
) {
    var bereichWahl by rememberSaveable { mutableStateOf(anfang.name) }
    val hier = Ladenbereich.entries.firstOrNull { it.name == bereichWahl } ?: Ladenbereich.Uebersicht
    val bereich = rememberCoroutineScope()

    var bestaetige by remember { mutableStateOf<String?>(null) }
    var laeuft by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var kasseLaeuft by remember { mutableStateOf(false) }
    var kassenfehler by remember { mutableStateOf<String?>(null) }
    var autoMeldung by remember { mutableStateOf<String?>(null) }
    var autoFehler by remember { mutableStateOf<String?>(null) }
    var vorwahl by remember { mutableStateOf<String?>(null) }
    var schenkStueck by remember { mutableStateOf<Shopartikel?>(null) }
    var codeOffen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        beiLaden()
        beiAboLaden(false)
    }

    // Zurück aus der Kasse im Browser: Der Stand kann sich geändert haben.
    val lebenslauf = LocalLifecycleOwner.current
    DisposableEffect(lebenslauf) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            if (ereignis == Lifecycle.Event.ON_RESUME && kasseLaeuft) {
                kasseLaeuft = false
                beiAboLaden(true)
            }
        }
        lebenslauf.lifecycle.addObserver(beobachter)
        onDispose { lebenslauf.lifecycle.removeObserver(beobachter) }
    }

    // Die Uhr rechnet minütlich — ein Sekundenticker wäre Unruhe ohne Auskunft.
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            jetzt = System.currentTimeMillis()
        }
    }

    val stand = shop.inhalt
    val credits = stand?.credits ?: konto?.credits ?: 0
    val gutscheine = garage.inhalt?.stand?.offeneWahlen ?: 0
    val premiumAktiv = abo.inhalt?.aktiv == true

    fun waehlen(neu: Ladenbereich) {
        bereichWahl = neu.name
        bestaetige = null
    }

    Seite(unterrand = unterrand) {
        Portal(credits)

        if (stand != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(Farben.Amber, CircleShape))
                Text(
                    "Neues Sortiment ${wechseltext(stand.naechsterWechsel, jetzt)}",
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                )
            }
        }

        fehler?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }

        Ladenreiter(
            hier = hier,
            premiumAktiv = premiumAktiv,
            sortiment = stand?.sortiment?.size ?: 0,
            gutscheine = gutscheine,
            credits = credits,
            beiWahl = ::waehlen,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
            Text(hier.kennung, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.AmberHell)
            Text(hier.kopf, style = Schrift.Titel, color = Farben.Text)
            Text(hier.satz, style = Schrift.Klein, color = Farben.TextLeise)
        }

        if (hier == Ladenbereich.Uebersicht) Anreisser(premiumAktiv) { waehlen(Ladenbereich.Premium) }

        if (hier == Ladenbereich.Premium) {
            PremiumSchaufenster(
                abo = abo,
                laeuft = kasseLaeuft,
                fehler = kassenfehler ?: abo.fehler,
                beiKaufen = { plan, sofort ->
                    if (!kasseLaeuft) {
                        kasseLaeuft = true
                        kassenfehler = null
                        bereich.launch {
                            griffe.kasse(plan, sofort)
                                .onSuccess { url -> beiBrowser(url) }
                                .onFailure {
                                    kassenfehler = it.message ?: "Die Kasse ließ sich nicht öffnen."
                                    kasseLaeuft = false
                                }
                        }
                    }
                },
                beiVerwalten = beiAboVerwalten,
                beiRechtstext = beiRechtstext,
                beiVertrag = beiVertrag,
            )
            return@Seite
        }

        val s = stand
        if (s == null) {
            val ladefehler = shop.fehler
            if (ladefehler != null) Fehlerzeile(ladefehler, beiLaden) else Ladezeile("Der Shop öffnet gerade …")
            return@Seite
        }

        when (hier) {
            Ladenbereich.Uebersicht -> {
                Gluecksrad(tagesbonus = s.tagesbonus, beiDrehen = griffe.drehen)
                Auftraege(s, konto?.einweisungOffen == true, jetzt, beiAusbildung)
            }

            Ladenbereich.Sortiment -> Sortiment(
                s = s,
                bestaetige = bestaetige,
                laeuft = laeuft,
                beiKaufen = { a ->
                    // Erst scharf stellen, dann kaufen — ein Fehlgriff soll nichts kosten.
                    val scharf = bestaetige == a.id
                    if (!a.gekauft && !laeuft && !scharf) bestaetige = a.id
                    if (!a.gekauft && !laeuft && scharf) {
                        laeuft = true
                        fehler = null
                        bereich.launch {
                            griffe.kaufen(a.id).onFailure { fehler = it.message ?: "Der Kauf hat nicht geklappt." }
                            bestaetige = null
                            laeuft = false
                        }
                    }
                },
                beiSchenken = { schenkStueck = it },
                beiAnlegen = beiAnlegen,
            )

            Ladenbereich.Fahrzeuge -> {
                val eigene = garage.inhalt?.stand?.fahrzeuge.orEmpty().toSet()
                val zurWahl = katalog.filter { it.id !in eigene }
                val angebot = s.tagesangebot
                if (katalog.isEmpty() || garage.inhalt == null) {
                    Ladezeile()
                } else {
                    if (angebot != null && zurWahl.any { it.id == angebot.templateId }) {
                        Ueberschrift("Fahrzeug des Tages")
                        Tagesangebotskarte(
                            typ = angebot.typ,
                            preis = angebot.preis,
                            regulaer = angebot.regulaer,
                            organisation = katalog.firstOrNull { it.id == angebot.templateId }?.organisation,
                            beiAnsehen = { vorwahl = angebot.templateId },
                        )
                    }

                    if (zurWahl.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Ueberschrift("Autohaus", Modifier.weight(1f))
                            if (gutscheine > 0) {
                                Text(
                                    "$gutscheine Gutschein${if (gutscheine == 1) "" else "e"}",
                                    style = Schrift.MonoKlein,
                                    color = Farben.AmberHell,
                                )
                            }
                        }
                        autoMeldung?.let {
                            Text(
                                it,
                                style = Schrift.MonoKlein,
                                color = Farben.GruenHell,
                                modifier = Modifier.clickable { autoMeldung = null },
                            )
                        }
                        autoFehler?.let {
                            Text(
                                it,
                                style = Schrift.MonoKlein,
                                color = Farben.SignalHell,
                                modifier = Modifier.clickable { autoFehler = null },
                            )
                        }
                        val garagenstand = garage.inhalt?.stand
                        Autohaus(
                            auswahl = zurWahl,
                            offeneWahlen = gutscheine,
                            laeuft = laeuft,
                            credits = credits,
                            preise = garagenstand?.preise.orEmpty(),
                            tagesangebot = garagenstand?.tagesangebot,
                            tagesangebotRegulaer = garagenstand?.tagesangebotRegulaer,
                            vorwahl = vorwahl,
                            beiUebernehmen = { f ->
                                if (!laeuft) {
                                    laeuft = true
                                    autoMeldung = null
                                    autoFehler = null
                                    bereich.launch {
                                        griffe.fahrzeugWaehlen(f)
                                            .onSuccess { autoMeldung = "${f.typ} steht jetzt in deiner Garage." }
                                            .onFailure {
                                                autoFehler = it.message ?: "Das Fahrzeug ließ sich nicht übernehmen."
                                            }
                                        laeuft = false
                                    }
                                }
                            },
                            beiKaufen = { f ->
                                if (!laeuft) {
                                    laeuft = true
                                    autoMeldung = null
                                    autoFehler = null
                                    bereich.launch {
                                        griffe.fahrzeugKaufen(f)
                                            .onSuccess {
                                                autoMeldung = "${f.typ} gekauft — es steht jetzt in deiner Garage."
                                            }
                                            .onFailure { autoFehler = it.message ?: "Der Kauf hat nicht geklappt." }
                                        laeuft = false
                                    }
                                }
                            },
                        )
                    } else {
                        Leerhinweis("Du besitzt bereits alle Fahrzeuge aus dem aktuellen Katalog.")
                    }
                }
            }

            Ladenbereich.Guthaben -> {
                if (s.auszug.isNotEmpty()) {
                    Ueberschrift("Letzte Bewegungen")
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
                    ) {
                        s.auszug.forEach { p ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(buchTagMonat(p.um), style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                                Text(p.text, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                                Text(
                                    (if (p.betrag > 0) "+" else "") + p.betrag,
                                    style = Schrift.MonoKlein,
                                    color = if (p.betrag < 0) Farben.TextLeise else Farben.GruenHell,
                                )
                            }
                        }
                    }
                }

                // Ganz unten: Eine Ansicht, die mit einem Eingabefeld beginnt, sieht
                // nach Arbeit aus statt nach Schaufenster.
                Ueberschrift("Code einlösen")
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Gross),
                ) {
                    SehrLeise("Aktionscodes gibt es auf unseren Kanälen — jeder gilt einmal je Konto.")
                    Knopf("Code eingeben", { codeOffen = true }, kompakt = true)
                }
            }

            Ladenbereich.Premium -> Unit
        }
    }

    schenkStueck?.let { artikel ->
        Schenkblende(
            artikel = artikel,
            griffe = griffe,
            beiSchliessen = { schenkStueck = null },
        )
    }

    if (codeOffen) {
        Codeblende(griffe = griffe, beiSchliessen = { codeOffen = false })
    }
}

/** Der Eingang: Ladenname, ein Satz dazu und der Kassenstand als eigene Kachel. */
@Composable
private fun Portal(credits: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Etikett("Ausrüstung")
            Text("Shop", style = Schrift.Schlagzeile, color = Farben.Text)
            Text(
                "Alles Zierde, kein Vorteil im Einsatz — und was die Laufbahn verleiht, bleibt unverkäuflich.",
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .flaeche(ecke = 12.dp, randfarbe = Farben.AmberTief)
                .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
        ) {
            Text(
                zahl(credits),
                style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold, fontSize = Schrift.TITEL),
                color = Farben.AmberHell,
            )
            Text("Credits", style = Schrift.Winzig, color = Farben.TextLeise)
        }
    }
}

/** „in 18 Std. 56 Min." — wie lange das Ausgestellte noch gilt. */
private fun wechseltext(naechsterWechsel: String, jetzt: Long): String {
    val ziel = zeitVon(naechsterWechsel)?.toEpochMilli() ?: return ""
    val minuten = max(0L, ceil((ziel - jetzt) / 60_000.0).toLong())
    val stunden = minuten / 60
    return if (stunden > 0) "in $stunden Std. ${minuten % 60} Min." else "in $minuten Min."
}

/** „noch 2 Tage" — bis die Woche der Aufträge wechselt. */
private fun wochentext(naechsterWechsel: String, jetzt: Long): String {
    val ziel = zeitVon(naechsterWechsel)?.toEpochMilli() ?: return ""
    val zeit = ziel - jetzt
    val tage = floor(zeit / 86_400_000.0).toLong()
    if (tage >= 1) return "noch $tage Tag${if (tage == 1L) "" else "e"}"
    val stunden = max(0L, ceil(zeit / 3_600_000.0).toLong())
    return "noch $stunden Std."
}

/**
 * Die Reiterreihe des Ladens — fünf Wege, einer davon golden. Sie rollt seitwärts,
 * statt fünf Wörter auf sechzig Punkte zu quetschen; die Zahl steht am Weg.
 */
@Composable
private fun Ladenreiter(
    hier: Ladenbereich,
    premiumAktiv: Boolean,
    sortiment: Int,
    gutscheine: Int,
    credits: Int,
    beiWahl: (Ladenbereich) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(Farben.Rand, Offset(0f, size.height - strich / 2f), Offset(size.width, size.height - strich / 2f), strich)
            },
    ) {
        Ladenbereich.entries.forEach { b ->
            val offen = b == hier
            val gold = b == Ladenbereich.Premium
            val farbe = when {
                offen -> Farben.Amber
                gold -> Farben.AmberHell
                else -> Farben.TextLeise
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .defaultMinSize(minHeight = Ziel.Normal)
                    .clickable(onClick = { beiWahl(b) }, role = Role.Tab, indication = null, interactionSource = null)
                    .drawBehind {
                        if (!offen) return@drawBehind
                        val balken = 2.dp.toPx()
                        drawLine(Farben.Amber, Offset(0f, size.height - balken / 2f), Offset(size.width, size.height - balken / 2f), balken)
                    }
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                if (gold) Text("★", style = Schrift.Klein, color = farbe)
                Text(
                    b.titel,
                    style = Schrift.Normal.copy(fontWeight = if (offen) FontWeight.SemiBold else FontWeight.Normal),
                    color = farbe,
                    maxLines = 1,
                )
                val zahl = when (b) {
                    Ladenbereich.Premium -> if (premiumAktiv) "✓" else null
                    Ladenbereich.Sortiment -> if (sortiment > 0) sortiment.toString() else null
                    Ladenbereich.Guthaben -> credits.toString()
                    Ladenbereich.Fahrzeuge, Ladenbereich.Uebersicht -> null
                }
                if (zahl != null) Kleinzahl(zahl)
                if (b == Ladenbereich.Fahrzeuge && gutscheine > 0) Markenzahl(gutscheine)
            }
        }
    }
}

@Composable
private fun Kleinzahl(text: String) {
    Text(
        text,
        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
        color = Farben.TextLeise,
        modifier = Modifier
            .border(1.dp, Farben.Rand, Rundung.Rund)
            .padding(horizontal = Abstand.Winzig),
    )
}

/**
 * Premium in der Übersicht: nur der Anreißer. Das Angebot selbst steht im
 * eigenen Bereich daneben.
 */
@Composable
private fun Anreisser(premiumAktiv: Boolean, beiAnsehen: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = if (premiumAktiv) Farben.Amber else Farben.AmberTief)
            .padding(Abstand.Gross),
    ) {
        Text("★", style = Schrift.Schlagzeile, color = Farben.Amber)
        Column(modifier = Modifier.weight(1f)) {
            Etikett("Abo")
            Text("PagerSpass Premium", style = Schrift.Gross, color = Farben.AmberHell)
            Text(
                if (premiumAktiv) {
                    "Läuft auf diesem Konto — hier steht, was alles dazugehört."
                } else {
                    "Mehr Platz für eigene Alarm- und Ausrückeordnungen, dazu Rahmen, Melder, Muster und " +
                        "Titel, die es sonst nirgends gibt."
                },
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
        }
        Knopf(if (premiumAktiv) "Ansehen" else "Premium ansehen", beiAnsehen, kompakt = true)
    }
}

/**
 * Die Dienstaufträge der Woche — der Teil des Ladens, in dem man nichts ausgibt,
 * sondern verdient. Sie erfüllen sich von selbst; es gibt nichts abzuholen.
 *
 * <b>Mit offener Einweisung stehen sie nicht da</b>, sondern der Weg zu ihnen: Sie
 * zählen Dienste, und Dienst darf dieses Konto noch nicht — eine Liste mit „0/5"
 * wäre eine Aufgabe ohne Tür.
 */
@Composable
private fun Auftraege(s: Shop, einweisungOffen: Boolean, jetzt: Long, beiAusbildung: () -> Unit) {
    if (einweisungOffen) {
        Ueberschrift("Dienstaufträge der Woche")
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Gross),
        ) {
            Text(
                "Die Aufträge zählen gefahrene Dienste — und dein erster steht noch aus. Fahr die " +
                    "Ausbildungsschicht; danach stehen hier drei Aufträge, und alle Runden sind offen.",
                style = Schrift.Klein,
                color = Farben.TextLeise,
            )
            Textweg("Ausbildungsschicht", beiAusbildung)
        }
        return
    }
    if (s.auftraege.isEmpty()) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        Ueberschrift("Dienstaufträge der Woche", Modifier.weight(1f))
        Text(wochentext(s.zulagen.naechsterWechsel, jetzt), style = Schrift.MonoKlein, color = Farben.TextLeise)
    }
    s.auftraege.forEach { a ->
        val ton = stufenton(a.stufe)
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .flaeche(randfarbe = if (a.erfuellt) Farben.Gruen else ton.copy(alpha = 0.5f))
                .padding(Abstand.Normal),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (a.stufe) {
                        "Leicht" -> "LEICHT"
                        "Mittel" -> "MITTEL"
                        "Schwer" -> "SCHWER"
                        else -> a.stufe.uppercase()
                    },
                    style = Schrift.Etikett,
                    color = ton,
                    modifier = Modifier.weight(1f),
                )
                Text("+${a.betrag}", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
            }
            Text(a.text, style = Schrift.Normal, color = Farben.Text)
            if (a.erfuellt) {
                Text("✓ Erfüllt", style = Schrift.MonoKlein, color = Farben.GruenHell)
            } else {
                Fortschritt(
                    anteil = if (a.ziel > 0) min(1f, a.stand.toFloat() / a.ziel) else 0f,
                    text = "${a.stand}/${a.ziel}",
                )
            }
        }
    }
    // Der Wochendeckel, still, aber sichtbar — sonst stünde irgendwann eine
    // gekürzte Gutschrift im Auszug, die sich niemand erklären kann.
    if (s.zulagen.deckel > 0) {
        SehrLeise(
            "Neue Aufträge montags um 12:00. Zulagen diese Woche: ${s.zulagen.verbraucht} von " +
                "${s.zulagen.deckel} Credits — mehr bringt der Dienst in einer Woche nicht ein.",
        )
    }
}

/** Die Gattung je Artikelart — die Zeile oben links auf der Kachel. */
private fun gattung(art: String) = when (art) {
    "Meldergesicht" -> "Melder"
    "Profilrahmen" -> "Rahmen"
    "Kopfmuster" -> "Muster"
    "Wappenfarbe" -> "Wappenfarbe"
    "Titel" -> "Titel"
    "Melderton" -> "Ton"
    else -> art
}

/**
 * Der Ton je Gattung. Er tönt die Bühne der Kachel, damit man schon aus dem
 * Augenwinkel sieht, ob da ein Melder oder ein Titel steht.
 */
private fun shopGattungston(art: String): Color = when (art) {
    "Meldergesicht" -> Farben.Amber
    "Profilrahmen" -> Color(0xFF5F8CFF)
    "Kopfmuster" -> Color(0xFF46C8A0)
    "Wappenfarbe" -> Color(0xFFAA78FF)
    "Titel" -> Color(0xFFFF7890)
    "Melderton" -> Color(0xFFFFA04D)
    else -> Farben.TextLeise
}

/** Das Schaufenster des Tages — zwei Kacheln je Zeile. */
@Composable
private fun Sortiment(
    s: Shop,
    bestaetige: String?,
    laeuft: Boolean,
    beiKaufen: (Shopartikel) -> Unit,
    beiSchenken: (Shopartikel) -> Unit,
    beiAnlegen: () -> Unit,
) {
    val probe = rememberTonprobe()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Ueberschrift("Heute im Sortiment", Modifier.weight(1f))
        Text("Nur heute", style = Schrift.MonoKlein, color = Farben.AmberHell)
    }

    if (s.sortiment.isEmpty()) {
        Leerhinweis("Für heute ist noch kein Sortiment eingetroffen.")
        return
    }

    s.sortiment.chunked(2).forEach { paar ->
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            paar.forEach { a ->
                val fehlt = a.preis - s.credits
                val zuWenig = !a.gekauft && fehlt > 0
                Warenkachel(
                    artikel = a,
                    modifier = Modifier.weight(1f),
                    buehne = {
                        when (a.art) {
                            "Meldergesicht" -> Kataloge.gesicht(a.stueckId)?.let { g ->
                                Minimelder(g.gehaeuse, g.lcd, breite = 30.dp)
                            } ?: Unit

                            "Profilrahmen" -> Kontobild(
                                kennung = "schaufenster",
                                anzeigename = "P S",
                                rahmen = a.stueckId,
                                wappenfarbe = 3,
                                groesse = 44.dp,
                            )

                            "Kopfmuster" -> Box(
                                Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(30.dp)
                                    .clip(Rundung.Winzig)
                                    .background(Farben.BgTief)
                                    .kopfband(a.stueckId, Wappen.ton("schaufenster", 3)),
                            )

                            "Wappenfarbe" -> Box(
                                Modifier
                                    .size(40.dp)
                                    .background(
                                        Wappen.ton("", a.stueckId.toIntOrNull() ?: 0),
                                        CircleShape,
                                    ),
                            )

                            // Ein Ton hat kein Bild. Statt eines Sinnbilds steht hier der
                            // Knopf, der ihn vorspielt.
                            "Melderton" -> if (Kataloge.ton(a.stueckId) != null) {
                                val spielt = probe.laeuft == a.stueckId
                                Knopf(
                                    aufschrift = if (spielt) "■ Läuft …" else "▶ Anhören",
                                    beiDruck = { probe.umschalten(a.stueckId) },
                                    art = Knopfart.Leise,
                                    kompakt = true,
                                )
                            } else {
                                Unit
                            }

                            // Der Titel ausdrücklich, nicht als „sonst": Eine neue Gattung
                            // soll hier lieber nichts zeigen als das Falsche.
                            "Titel" -> Text(
                                "„${Kataloge.titel(a.stueckId) ?: a.name}“",
                                style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold),
                                color = Farben.Text,
                                textAlign = TextAlign.Center,
                            )

                            else -> Unit
                        }
                    },
                    fuss = {
                        if (a.gekauft) {
                            Knopf("Im Konto anlegen", beiAnlegen, art = Knopfart.Leise, kompakt = true, breit = true)
                        } else {
                            Knopf(
                                aufschrift = kaufknopf(a.preis, s.credits, besitz = false, scharf = bestaetige == a.id),
                                beiDruck = { beiKaufen(a) },
                                art = if (bestaetige == a.id) Knopfart.Haupt else Knopfart.Normal,
                                aktiv = !zuWenig && !laeuft,
                                kompakt = true,
                                breit = true,
                            )
                        }
                        // Verschenken als Zeichen neben dem Kauf, nicht als zweiter
                        // gleich großer Knopf — der Kauf ist der Regelfall.
                        Knopf(
                            aufschrift = "🎁 Verschenken",
                            beiDruck = { beiSchenken(a) },
                            art = Knopfart.Leise,
                            aktiv = !zuWenig && !laeuft,
                            kompakt = true,
                            breit = true,
                        )
                    },
                )
            }
            if (paar.size == 1) Box(Modifier.weight(1f))
        }
    }
}

/**
 * Eine Ware im Schaufenster: Gattung oben links, Besitz in der Ecke, das Stück auf
 * einer getönten Bühne, der Name, die Knöpfe am Fuß.
 */
@Composable
private fun Warenkachel(
    artikel: Shopartikel,
    modifier: Modifier,
    buehne: @Composable () -> Unit,
    fuss: @Composable () -> Unit,
) {
    val ton = shopGattungston(artikel.art)
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .flaeche(ecke = 12.dp)
            .padding(Abstand.Normal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                gattung(artikel.art).uppercase(),
                style = Schrift.Etikett,
                color = ton,
                modifier = Modifier.weight(1f),
            )
            if (artikel.gekauft) Text("✓ Im Besitz", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.GruenHell)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .background(ton.copy(alpha = 0.10f), Rundung.Klein),
        ) {
            buehne()
        }
        Text(
            artikel.name,
            style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold),
            color = Farben.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        fuss()
    }
}

/** Das Fahrzeug des Tages — ein Banner, das den Wagen ins Schaufenster stellt. */
@Composable
private fun Tagesangebotskarte(
    typ: String,
    preis: Int,
    regulaer: Int,
    organisation: String?,
    beiAnsehen: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(randfarbe = orgTon(organisation))
            .flaechenmarke(wartet = true)
            .padding(Abstand.Gross),
    ) {
        Text("−25 % · nur heute", style = Schrift.MonoKlein, color = Farben.AmberHell)
        Text(typ, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold, fontSize = Schrift.TITEL), color = Farben.Text)
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$regulaer Credits",
                style = Schrift.Klein.copy(textDecoration = TextDecoration.LineThrough),
                color = Farben.TextSehrLeise,
            )
            Text("$preis Credits", style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
        }
        Knopf("Im Autohaus ansehen", beiAnsehen, art = Knopfart.Haupt, kompakt = true)
    }
}

/**
 * Verschenken. Bezahlt wird aus dem eigenen Guthaben, bekommen tut es der andere
 * — Credits wandern nie, nur Zierde. <b>Zu junge Freundschaften stehen mit
 * dabei</b>, nicht ausgeblendet: Ein Name, der einfach fehlt, sieht aus wie ein
 * Fehler.
 */
@Composable
private fun Schenkblende(artikel: Shopartikel, griffe: Ladengriffe, beiSchliessen: () -> Unit) {
    val bereich = rememberCoroutineScope()
    var freunde by remember { mutableStateOf<List<Beschenkbarer>?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var fertig by remember { mutableStateOf<String?>(null) }
    var gerade by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(artikel.id) {
        griffe.beschenkbare()
            .onSuccess { freunde = it }
            .onFailure { fehler = it.message ?: "Die Freundesliste kam nicht." }
    }

    Blende(
        titel = "${artikel.name} verschenken",
        beiSchliessen = beiSchliessen,
        fuss = { Knopf(if (fertig != null) "Fertig" else "Abbrechen", beiSchliessen) },
    ) {
        val erledigt = fertig
        if (erledigt != null) {
            Text(erledigt, style = Schrift.Normal, color = Farben.GruenHell)
        } else {
            SehrLeise(
                "Du bezahlst ${artikel.preis} Credits, bekommen tut es der andere. Verschenken geht an " +
                    "Freunde, mit denen du seit mindestens zwei Tagen verbunden bist.",
            )
            val liste = freunde
            when {
                liste == null && fehler == null -> SehrLeise("Die Freundesliste wird geholt …")
                liste != null && liste.isEmpty() -> Leerhinweis("Du hast noch keine bestätigten Freunde.")
                liste != null -> liste.forEach { f ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(f.anzeigename, style = Schrift.Normal, color = Farben.Text)
                            SehrLeise("@${f.benutzername}")
                        }
                        if (f.darfEmpfangen) {
                            Knopf(
                                aufschrift = if (gerade == f.kennung) "Wird verpackt …" else "Schenken",
                                beiDruck = {
                                    if (gerade == null) {
                                        gerade = f.kennung
                                        fehler = null
                                        bereich.launch {
                                            griffe.schenken(f.kennung, artikel.id)
                                                .onSuccess { fertig = "${artikel.name} ist auf dem Weg zu ${f.anzeigename}." }
                                                .onFailure { fehler = it.message ?: "Das Geschenk ging nicht raus." }
                                            gerade = null
                                        }
                                    }
                                },
                                aktiv = gerade == null,
                                kompakt = true,
                            )
                        } else {
                            SehrLeise("noch ${f.wartetage} ${if (f.wartetage == 1) "Tag" else "Tage"}")
                        }
                    }
                }
            }
        }
        fehler?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
    }
}

/**
 * Einen Aktionscode einlösen. Das Ergebnis bleibt im Fenster stehen, statt als
 * Meldung zu verschwinden: Was ein Code gebracht hat, liest man zweimal.
 */
@Composable
private fun Codeblende(griffe: Ladengriffe, beiSchliessen: () -> Unit) {
    val bereich = rememberCoroutineScope()
    var eingabe by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    var ertrag by remember { mutableStateOf<String?>(null) }
    var prueft by remember { mutableStateOf(false) }

    fun einloesen() {
        if (prueft || eingabe.isBlank()) return
        prueft = true
        fehler = null
        bereich.launch {
            griffe.codeEinloesen(eingabe)
                .onSuccess {
                    ertrag = it.text
                    eingabe = ""
                }
                .onFailure { fehler = it.message ?: "Der Code ging nicht durch." }
            prueft = false
        }
    }

    Blende(
        titel = "Code einlösen",
        beiSchliessen = beiSchliessen,
        fuss = {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Schließen", beiSchliessen, art = Knopfart.Leise)
                Knopf(
                    if (prueft) "Wird geprüft …" else "Einlösen",
                    { einloesen() },
                    art = Knopfart.Haupt,
                    aktiv = !prueft && eingabe.isNotBlank(),
                )
            }
        },
    ) {
        Feld(
            wert = eingabe,
            // Am Handy schreibt ein Feld klein und korrigiert — der Server nimmt es
            // ohnehin ohne Rücksicht auf Groß und Klein, gezeigt wird es groß.
            beiAenderung = { eingabe = it.uppercase().take(32) },
            etikett = "Dein Code",
            platzhalter = "z. B. LEITSTELLE200",
            tastatur = KeyboardType.Ascii,
            stil = Schrift.MonoNormal,
        )
        val gebracht = ertrag
        when {
            gebracht != null -> Text("Eingelöst: $gebracht", style = Schrift.Normal, color = Farben.GruenHell)
            fehler != null -> Text(fehler.orEmpty(), style = Schrift.MonoKlein, color = Farben.SignalHell)
            else -> SehrLeise("Groß- und Kleinschreibung ist egal, Leerzeichen und Bindestriche auch.")
        }
    }
}
