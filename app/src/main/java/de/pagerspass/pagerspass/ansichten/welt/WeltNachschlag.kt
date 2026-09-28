package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.mobil.weltzeit
import de.pagerspass.pagerspass.netz.WeltKassenblatt
import de.pagerspass.pagerspass.netz.WeltRangliste
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Die Nachschlagewerke hinter „Mehr": Kasse, Rangliste, Laufbahn. Man geht einmal hin,
 * liest und geht zurück.
 */

// ------------------------------------------------------------------ Kasse

/** Was eine Buchung war — `GRUNDTEXT` in `KasseBlende.vue`. */
private val GRUNDTEXT = mapOf(
    "Gruendung" to "Startguthaben",
    "Lage" to "Lagen",
    "Erstzugriff" to "Erstzugriffe",
    "Hilfsfrist" to "Hilfsfristen",
    "Grosslage" to "Großeinsätze",
    "WacheGebaut" to "Wachen gebaut",
    "WacheAbgerissen" to "Abrisse (Erstattung)",
    "FahrzeugGekauft" to "Fahrzeuge gekauft",
    "FahrzeugVerkauft" to "Fahrzeuge verkauft",
    "Verfall" to "Verfallene Lagen",
    "Ausbau" to "Ausbauten",
    "Wochenziel" to "Wochenziele",
    "Kredit" to "Kredit aufgenommen",
    "Tilgung" to "Tilgungen",
    "Lehrgang" to "Lehrgangsgebühren",
    "LeiheEinnahme" to "Verliehen (Einnahme)",
    "LeiheAusgabe" to "Geliehen (Miete)",
    "Werkstatt" to "Instandsetzungen",
    "Zweigstelle" to "Zweigstelle gegründet",
    "Wochensieg" to "Wochensiege (Preisgeld)",
)

private fun zeitpunkt(iso: String): String {
    val ms = weltzeit(iso) ?: return iso
    return SimpleDateFormat("dd.MM., HH:mm", Locale.GERMANY).format(Date(ms))
}

private fun mitVorzeichen(n: Int): String = (if (n >= 0) "+" else "") + zahl(n)

/**
 * Die Kasse — übertragen aus `components/welt/KasseBlende.vue`.
 *
 * <b>Der Kredit steht oben:</b> Eine offene Schuld erklärt, warum jede Gutschrift kleiner
 * ausfällt als die Vergütung der Lage. Darunter die Summen je Grund und die letzten
 * Buchungen mit Vermerk.
 */
@Composable
fun WeltKasseBlende(welt: Welt, stand: Weltzustand) {
    val bereich = rememberCoroutineScope()

    var blatt by remember { mutableStateOf<WeltKassenblatt?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var siebenTage by remember { mutableStateOf(true) }
    var arbeitet by remember { mutableStateOf(false) }
    var kreditfehler by remember { mutableStateOf<String?>(null) }
    var tilgung by remember { mutableStateOf("") }

    suspend fun laden() {
        welt.holen("Das Kassenblatt ließ sich nicht laden.") { welt.wege.kassenblatt(it) }
            .onSuccess {
                blatt = it
                fehler = null
            }
            .onFailure { fehler = it.message }
    }

    // Jede Gutschrift ändert das Blatt — dann wird neu geholt.
    LaunchedEffect(stand.gutschrift) { laden() }

    fun kreditTun(standard: String, was: suspend (String) -> Unit, danach: () -> Unit = {}) {
        if (arbeitet) return
        arbeitet = true
        kreditfehler = null
        bereich.launch {
            kreditfehler = welt.aktion(standard, Welt.Nachladen.Betrieb, was)
            if (kreditfehler == null) {
                danach()
                laden()
            }
            arbeitet = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        val b = blatt
        if (fehler != null && b == null) {
            Absage(fehler)
            return@Column
        }
        if (b == null) {
            Ladezeile("Das Kassenblatt wird geladen …")
            return@Column
        }

        // ------------------------------------------------------- Kredit
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
        ) {
            val k = b.kredit
            if (k != null) {
                val anteil = if (k.rueckzahlung <= 0) 0f else (k.getilgt.toFloat() / k.rueckzahlung).coerceIn(0f, 1f)
                val roh = tilgung.trim()
                val betrag = if (roh.isEmpty()) {
                    k.restschuld
                } else {
                    val eingabe = roh.replace(" ", "").replace(".", "").replace(",", ".").toDoubleOrNull()?.toInt() ?: 0
                    if (eingabe <= 0) 0 else minOf(eingabe, k.restschuld)
                }
                val gedeckt = b.guthaben >= betrag

                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Restschuld", style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                    Credits(k.restschuld, stil = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), farbe = Farben.SignalHell)
                }
                Box(Modifier.fillMaxWidth().height(6.dp).background(Farben.BgTief, Rundung.Rund)) {
                    Box(Modifier.fillMaxWidth(anteil).fillMaxHeight().background(Farben.Gruen, Rundung.Rund))
                }
                Text(
                    text = "${zahl(k.getilgt)} von ${zahl(k.rueckzahlung)} getilgt · ${k.zinsprozent} % Aufschlag · " +
                        "${k.tilgungProzent} % jeder Gutschrift tilgen von selbst.",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Feld(
                        wert = tilgung,
                        beiAenderung = { tilgung = it.take(12) },
                        platzhalter = zahl(k.restschuld),
                        tastatur = KeyboardType.Number,
                        stil = Schrift.MonoKlein,
                        modifier = Modifier.weight(1f),
                    )
                    Knopf(
                        "${zahl(betrag)} tilgen",
                        { kreditTun("Die Tilgung ging nicht.", { welt.wege.kreditTilgen(it, betrag) }) { tilgung = "" } },
                        art = Knopfart.Haupt,
                        kompakt = true,
                        aktiv = !arbeitet && gedeckt && betrag > 0,
                    )
                }
                if (!gedeckt) {
                    Text(text = "Dafür fehlen Welt-Credits — es geht auch weniger.", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                }
            } else {
                Text(text = "Kredit aufnehmen", style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                b.kreditangebote.forEach { a ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                            Credits(a.betrag, stil = Schrift.MonoNormal)
                            Text(
                                text = "${a.zinsprozent} % Aufschlag · zurück ${zahl(a.rueckzahlung)}",
                                style = Schrift.Winzig,
                                color = Farben.TextSehrLeise,
                            )
                        }
                        Knopf(
                            if (a.frei) "Aufnehmen" else "ab Stufe ${a.abStufe}",
                            { kreditTun("Der Kredit ging nicht.", { welt.wege.kreditAufnehmen(it, a.betrag) }) },
                            kompakt = true,
                            aktiv = !arbeitet && a.frei,
                        )
                    }
                }
                Text(
                    text = "Einer zur Zeit — getilgt wird von selbst, ein Viertel jeder Gutschrift.",
                    style = Schrift.Winzig,
                    color = Farben.TextSehrLeise,
                )
            }
            Absage(kreditfehler)
        }

        // ------------------------------------------------------- Summen
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Pille("7 Tage", siebenTage, { siebenTage = true })
            Pille("Seit je", !siebenTage, { siebenTage = false })
        }
        val summen = if (siebenTage) b.summenSiebenTage else b.summenGesamt
        if (summen.isEmpty()) {
            Leerhinweis("In diesem Zeitraum hat sich nichts bewegt.")
        }
        summen.forEach { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(text = GRUNDTEXT[s.grund] ?: s.grund, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                Text(
                    text = mitVorzeichen(s.summe),
                    style = Schrift.MonoKlein,
                    color = if (s.summe >= 0) Farben.GruenHell else Farben.SignalHell,
                )
            }
        }

        // ------------------------------------------------------- Buchungen
        Gruppenkopf("Letzte Buchungen")
        b.buchungen.forEach { z ->
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                    Text(text = GRUNDTEXT[z.grund] ?: z.grund, style = Schrift.Klein, color = Farben.Text)
                    z.vermerk?.let { Text(text = it, style = Schrift.Winzig, color = Farben.TextSehrLeise) }
                    Text(text = zeitpunkt(z.um), style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextSehrLeise)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = mitVorzeichen(z.betrag),
                        style = Schrift.MonoKlein,
                        color = if (z.betrag >= 0) Farben.GruenHell else Farben.SignalHell,
                    )
                    Text(text = "→ ${zahl(z.standDanach)}", style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextSehrLeise)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Rangliste

/**
 * Die Rangliste — übertragen aus `components/welt/RanglisteBlende.vue`: diese Woche und
 * seit je, der Sieger der Vorwoche obenan, jede Minute neu.
 */
@Composable
fun WeltRanglisteBlende(welt: Welt, stand: Weltzustand, griffe: Weltgriffe) {
    var liste by remember { mutableStateOf<WeltRangliste?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var woche by remember { mutableStateOf(true) }

    LaunchedEffect(stand.wochensiege) {
        while (true) {
            welt.holen("Die Rangliste ließ sich nicht laden.") { welt.wege.rangliste(it) }
                .onSuccess {
                    liste = it
                    fehler = null
                }
                .onFailure { fehler = it.message }
            delay(60_000)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        val l = liste
        if (fehler != null && l == null) {
            Absage(fehler)
            return@Column
        }
        if (l == null) {
            Ladezeile()
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Pille("Diese Woche", woche, { woche = true })
            Pille("Seit je", !woche, { woche = false })
        }

        val vorwoche = l.letzteWoche
        if (woche && vorwoche != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().flaeche(randfarbe = GOLD, ecke = 9.dp).padding(Abstand.Normal),
            ) {
                Text(text = "♛", style = Schrift.Titel, color = GOLD)
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Letzte Woche gewann", style = Schrift.Klein, color = Farben.TextLeise)
                        Kontoname(vorwoche.name, vorwoche.benutzername, griffe)
                    }
                    Text(
                        text = "${vorwoche.leitstelle} · ${zahl(vorwoche.credits)} verdient · ${zahl(vorwoche.preisgeld)} Preisgeld",
                        style = Schrift.Winzig,
                        color = Farben.TextSehrLeise,
                    )
                }
            }
        }

        val zeilen = if (woche) l.dieseWoche else l.gesamt
        if (zeilen.isEmpty()) {
            Leerhinweis("Noch hat niemand etwas verdient. Der erste Platz ist zu haben.")
        }
        zeilen.forEach { r ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = Abstand.Winzig),
            ) {
                Text(
                    text = r.platz.toString(),
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = when (r.platz) {
                        1 -> GOLD
                        2 -> Farben.TextLeise
                        3 -> Farben.OrangeHell
                        else -> Farben.TextSehrLeise
                    },
                    modifier = Modifier.width(32.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                        Kontoname(r.name, r.benutzername, griffe)
                        // Die Krone nur, wo es etwas zu zeigen gibt.
                        if (r.wochensiege > 0) {
                            Text(text = "♛ ${r.wochensiege}", style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold), color = GOLD)
                        }
                    }
                    Text(
                        text = "${r.leitstelle} · Stufe ${r.stufe} · ${r.lagenGedeckt} Lagen",
                        style = Schrift.Winzig,
                        color = Farben.TextSehrLeise,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Credits(r.credits)
            }
        }

        val meinPlatz = if (woche) l.meinPlatzWoche else l.meinPlatzGesamt
        Text(
            text = if (meinPlatz != null) {
                "Dein Platz: $meinPlatz" + if (l.meineWochensiege > 0) " · gewonnene Wochen: ${l.meineWochensiege}" else ""
            } else {
                "Sobald du etwas verdienst, stehst du mit drauf."
            },
            style = Schrift.Klein,
            color = Farben.TextLeise,
        )
    }
}

/**
 * Ein Name, der zum Profil führt, wenn es einen Benutzernamen gibt — `Kontoname.vue`.
 * Ohne Benutzernamen ist er nur Text: Ein Weg ins Leere wäre schlimmer als keiner.
 */
@Composable
private fun Kontoname(name: String, benutzername: String?, griffe: Weltgriffe) {
    Text(
        text = name,
        style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
        color = if (benutzername != null) Farben.BlauHell else Farben.Text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = if (benutzername != null) Modifier.clickable { griffe.profil(benutzername) } else Modifier,
    )
}

// ------------------------------------------------------------------ Laufbahn

/** Was eine Stufe freischaltet, in Worten — `NAMEN` in `LaufbahnBlende.vue`. */
private val FREISCHALTUNG = mapOf(
    "Loeschgruppe" to "Löschgruppenhaus",
    "Rettungsdienst" to "Rettungsdienst",
    "Aussenstelle" to "Rettungswachen-Außenstelle",
    "Hilfsorganisation" to "Hilfsorganisationen als Träger",
    "Polizei" to "Polizei",
    "Thw" to "THW",
    "Luftrettung" to "Luftrettung",
    "Ausbildung" to "Lehrgangseinrichtung",
    "Berufsfeuerwehr" to "Feuerwache (Berufsfeuerwehr)",
    "Grossanbau" to "Großanbau — vierte Ausbaustufe",
    "Leihmarkt" to "Leihmarkt-Ausbau — zwei Plätze mehr",
    "Erweiterungsbau" to "Erweiterungsbau — fünfte Ausbaustufe",
    "Grosskredit" to "Großkredit — 150 000 auf einmal",
    "Werkfeuerwehr" to "Werkfeuerwehr — zehn Stellplätze",
    "Wasserrettung" to "Wasserrettungsstation — Boote und Taucher",
    "Werkstatt" to "Fahrzeugwerkstatt — instand setzen statt ersetzen",
    "Logistikzentrum" to "Logistikzentrum — Abrollbehälter und Logistik",
    "Bereitschaftspolizei" to "Bereitschaftspolizei — geschlossene Einheiten",
    "Zweigstelle" to "Zweigstelle — ein zweiter Ausrückebereich",
    "Grossraumhalle" to "Großraumhalle — sechste Ausbaustufe",
    "Werkstattausbau" to "Werkstattausbau — doppelte Bühnen, halbe Standzeit",
    "Rettungshunde" to "Rettungshundestaffel — Flächensuche mit Hund",
    "Aufbaulehrgaenge" to "Aufbaulehrgänge — Höhenrettung, Tauchen, Strahlenschutz",
    "Bergwacht" to "Bergwacht-Station — Gelände- und Höhenrettung",
    "LeihmarktII" to "Leihmarkt-Ausbau II — acht Plätze je Seite",
    "Waldbrandstuetzpunkt" to "Waldbrand-Stützpunkt — vier Plätze für die Fläche",
    "Zugvorlagen" to "Rüstzug und zwei Löschzüge als Vorlage",
    "ZweigstelleII" to "Zweite Zweigstelle — ein dritter Ausrückebereich",
    "Kriminaldienst" to "Kriminaldirektion — Ermittlung und Kriminaltechnik",
    "WerkstattausbauII" to "Werkstattausbau II — vierfache Bühnen, viertel Standzeit",
    "Landesfeuerwehrschule" to "Landesfeuerwehrschule — doppelte Lehrsäle, halbe Dauer",
    "GrosskreditII" to "Großkredit II — 500 000 auf einmal",
    "ZweigstelleIII" to "Dritte Zweigstelle — ein vierter Ausrückebereich",
    "Polizeiflieger" to "Polizeifliegerstaffel — ein Haus für den Hubschrauber",
    "Reservestellplatz" to "Reservestellplatz — ein Fahrzeug über dem Deckel",
    "Seenotrettung" to "Seenotrettungsstation — Kreuzer und Tochterboot",
    "Streifenausbau" to "Streifenausbau — zwölf Stationen je Pfad",
    "ZweigstelleIV" to "Vierte Zweigstelle — ein fünfter Ausrückebereich",
    "Fuehrungslehrgaenge" to "Führungslehrgänge — Führung A und B, Verpflegung, Behandlungsplatz",
    "Verwertung" to "Verwertung — sechzig statt fünfzig Prozent zurück",
    "GrosskreditIII" to "Großkredit III — eine Million auf einmal",
    "Massenanfall" to "Massenanfall — MANV-Lagen entstehen auch bei dir",
    "Landesleitstelle" to "Landesleitstelle — die siebte Ausbaustufe",
)

private fun freiwort(was: String): String = FREISCHALTUNG[was] ?: was

/**
 * Die Laufbahn — übertragen aus `components/welt/LaufbahnBlende.vue`.
 *
 * <b>Oben die Gegenüberstellung</b>, weil sie die Frage beantwortet, mit der man kommt:
 * Erfahrung wächst nur und bestimmt die Stufe; Welt-Credits gibt man aus.
 */
@Composable
fun WeltLaufbahnBlende(welt: Welt, stand: Weltzustand) {
    LaunchedEffect(Unit) { welt.laufbahnLaden() }

    val meine = stand.stand?.stufe ?: 1
    val erfahrung = stand.stand?.erfahrung ?: 0
    val naechste = stand.laufbahn.firstOrNull { it.nummer > meine && it.schaltetFrei.isNotEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            Groesse(
                titel = "Erfahrung",
                unter = "wächst nur · bestimmt die Stufe",
                modifier = Modifier.weight(1f),
            ) {
                Icon(imageVector = Weltzeichen.Erfahrung, contentDescription = null, tint = Farben.BlauHell, modifier = Modifier.size(14.dp))
                Text(text = zahl(erfahrung), style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.BlauHell)
            }
            Groesse(
                titel = "Welt-Credits",
                unter = "gibst du aus · ändert die Stufe nicht",
                modifier = Modifier.weight(1f),
            ) {
                Credits(stand.guthaben, stil = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold))
            }
        }
        Text(text = "Hundert verdiente Credits sind zehn Erfahrung — und die schrumpft nie.", style = Schrift.Klein, color = Farben.TextLeise)

        if (naechste != null) {
            Text(
                text = "Als Nächstes: ${naechste.schaltetFrei.joinToString(" und ") { freiwort(it) }} ab Stufe " +
                    "${naechste.nummer} — noch ${zahl(maxOf(0, naechste.abErfahrung - erfahrung))} Erfahrung.",
                style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                color = Farben.Amber,
            )
        }

        if (stand.laufbahn.isEmpty()) {
            Ladezeile("Die Laufbahn wird geladen …")
            return@Column
        }

        stand.laufbahn.forEach { z ->
            val erreicht = z.nummer <= meine
            val aktuell = z.nummer == meine
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(
                        farbe = if (aktuell) Farben.FlaecheAktiv else Farben.Flaeche,
                        randfarbe = if (aktuell) Farben.Amber else Farben.Rand,
                        ecke = 9.dp,
                        mitLichtkante = false,
                    )
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text(
                    text = z.nummer.toString(),
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = if (erreicht) Farben.Amber else Farben.TextSehrLeise,
                    modifier = Modifier.width(32.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${z.wachendeckel} Wachen",
                            style = Schrift.Klein,
                            color = if (erreicht) Farben.Text else Farben.TextLeise,
                            modifier = Modifier.weight(1f),
                        )
                        Text(text = "ab ${zahl(z.abErfahrung)} EP", style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextSehrLeise)
                    }
                    if (z.schaltetFrei.isNotEmpty()) {
                        Text(
                            text = z.schaltetFrei.joinToString(" · ") { freiwort(it) },
                            style = Schrift.Winzig,
                            color = if (erreicht) Farben.GruenHell else Farben.TextLeise,
                        )
                    } else if (aktuell) {
                        Text(text = "Hier stehst du.", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                    }
                }
            }
        }
    }
}

@Composable
private fun Groesse(titel: String, unter: String, modifier: Modifier = Modifier, wert: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = modifier.flaeche(ecke = 9.dp).padding(Abstand.Normal),
    ) {
        Text(text = titel, style = Schrift.Klein, color = Farben.TextLeise)
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) { wert() }
        Text(text = unter, style = Schrift.Winzig, color = Farben.TextSehrLeise)
    }
}
