package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Einstellungsaenderung
import de.pagerspass.pagerspass.mobil.Funkgruppeneingabe
import de.pagerspass.pagerspass.mobil.Lobbystand
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Spieler
import de.pagerspass.pagerspass.netz.Wache
import de.pagerspass.pagerspass.netz.Wachenwahl
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Der Teil „Runde" der Lobby — Steckbrief, Rundeneinstellungen, Bot-Besatzungen
 * und Ränge. Übertragen aus `LobbyView.vue` (Zeilen ab „Rundeneinstellungen")
 * samt `lobby/FunkgruppenMaske.vue` und `lobby/WachenMaske.vue`.
 *
 * <b>Für alle lesbar, für die Leitstelle stellbar.</b> Wer nicht disponiert,
 * sieht dieselben Gruppen gesperrt — ein Formular in Grau hinter fünf
 * Klappflächen wäre schlechter; der Steckbrief oben beantwortet die Frage „was
 * für eine Schicht ist das" ohne eine Gruppe zu öffnen.
 *
 * <b>Jeder Griff ist ein `UpdateSettings`</b> mit genau dem einen Feld, das
 * sich ändert — alles andere geht als `null` hinaus und bleibt.
 */
@Composable
internal fun ColumnScope.TeilRunde(
    raum: Raumzustand,
    ich: Spieler?,
    konto: Konto?,
    katalog: Katalog?,
    daten: Lobbystand,
    griffe: LobbyGriffe,
) {
    val s = raum.settings
    val istLeitstelle = ich?.istLeitstelle == true
    // Die Schicht des Tages ist für alle dieselbe — hier stellt sie niemand.
    val einstellbar = istLeitstelle && s.mode != "Tagesschicht"
    val menschen = raum.menschen
    val setzen = griffe.einstellungen

    Ueberschrift("Rundeneinstellungen")

    Steckbrief(raum)
    if (!istLeitstelle) {
        SehrLeise("Diese Einstellungen setzt die Leitstelle. Aufklappen und nachlesen geht trotzdem.")
    } else if (!einstellbar) {
        SehrLeise(
            "Die Schicht des Tages ist für alle dieselbe — an ihren Regeln lässt sich nichts " +
                "verstellen. Nachlesen geht.",
        )
    }

    // ----------------------------------------------------------- Grundregeln
    Einstellgruppe(
        titel = "Grundregeln",
        stand = "${Rundentexte.modus(s.mode)} · ${raum.maxSpieler} Plätze",
        offenAnfangs = true,
    ) {
        Etikett("Spielmodus")
        Wahlkarten(
            wahl = listOf(
                Triple("Zufall", "Zufallseinsätze", "Der Notruf klingelt von selbst."),
                Triple("Frei", "Freie Vergabe", "Die Leitstelle denkt sich Lagen aus."),
            ),
            gewaehlt = s.mode,
            aktiv = einstellbar,
            beiWahl = { setzen(Einstellungsaenderung(mode = it)) },
        )
        Etikett("Zeittempo")
        Wahlkarten(
            wahl = listOf(
                Triple("Echtzeit", "Echtzeit", "Fahrten und Funk laufen wie im echten Dienst."),
                Triple("Simulation", "Simulation", "Deutlich schneller — für die ganze Runde."),
            ),
            gewaehlt = s.zeitmodus,
            aktiv = einstellbar,
            beiWahl = { setzen(Einstellungsaenderung(zeitmodus = it)) },
        )

        // Die Plätze — in jeder Runde. Die untere Marke ist die Zahl der
        // Menschen, die schon da sind: Weiter herunter lässt der Server nicht.
        val untergrenze = maxOf(24, menschen.size).coerceAtMost(400)
        var regler by remember(raum.maxSpieler) {
            mutableFloatStateOf(raum.maxSpieler.coerceIn(untergrenze, 400).toFloat())
        }
        Etikett("Plätze — ${regler.toInt()}")
        Slider(
            value = regler,
            onValueChange = { regler = (it / 2f).toInt() * 2f },
            onValueChangeFinished = {
                if (regler.toInt() != raum.maxSpieler) setzen(Einstellungsaenderung(maxSpieler = regler.toInt()))
            },
            valueRange = untergrenze.toFloat()..400f,
            enabled = einstellbar && untergrenze < 400,
            colors = SliderDefaults.colors(
                thumbColor = Farben.Amber,
                activeTrackColor = Farben.Amber,
                inactiveTrackColor = Farben.Rand,
            ),
        )
        SehrLeise(
            "Plätze für Mitspieler. Bot-Besatzungen zählen nicht mit — du kannst so viele " +
                "einteilen, wie die Runde Fahrzeuge trägt. Die Zahl der Leitstellenplätze folgt " +
                "den Plätzen: je 45 einer, höchstens sechs.",
        )
    }

    // ---------------------------------------------------- Funk und Rufnamen
    val beispiel = "${mitStellen(1, s.wachennummerStellen)}/83/${mitStellen(1, s.laufnummerStellen)}"
    Einstellgruppe(titel = "Funk und Rufnamen", stand = beispiel) {
        Etikett("Rufnamenformat · Beispiel $beispiel")
        SehrLeise("Wachnummer")
        Pillenreihe {
            (1..3).forEach { n ->
                Pille(
                    aufschrift = mitStellen(1, n),
                    an = s.wachennummerStellen == n,
                    aktiv = einstellbar,
                    beiDruck = { setzen(Einstellungsaenderung(wachennummerStellen = n)) },
                )
            }
        }
        SehrLeise("Laufende Nummer")
        Pillenreihe {
            (1..3).forEach { n ->
                Pille(
                    aufschrift = mitStellen(1, n),
                    an = s.laufnummerStellen == n,
                    aktiv = einstellbar,
                    beiDruck = { setzen(Einstellungsaenderung(laufnummerStellen = n)) },
                )
            }
        }
        SehrLeise(
            "Eine Mindestbreite, keine Obergrenze: Wache 55 bleibt „55\". Fahrzeuge, denen die " +
                "Leitstelle einen eigenen Rufnamen gegeben hat, bleiben unberührt.",
        )

        Rufnamewoerter(raum, einstellbar, setzen)

        FunkgruppenMaske(raum, einstellbar, setzen)

        Etikett("Dauerfunk-Verstöße bis zur Entfernung")
        Pillenreihe {
            listOf(3, 4, 5).forEach { n ->
                Pille(
                    aufschrift = "$n",
                    an = s.funkverstossSchwelle == n,
                    aktiv = einstellbar,
                    beiDruck = { setzen(Einstellungsaenderung(funkverstossSchwelle = n)) },
                )
            }
        }
        SehrLeise(
            "Nach 20 Sekunden endet die Sendung automatisch. Es folgen 1 Minute Sendepause und " +
                "ein Verstoß. Beim ${s.funkverstossSchwelle}. Verstoß wird der Platz aus dieser " +
                "Runde entfernt; neue Runden bleiben davon unberührt.",
        )

        WachenMaske(raum, daten, einstellbar, setzen, griffe.kreiswachenLaden)
    }

    // ---------------------------------------------------------- Einsatzregeln
    val regeln = listOf(
        s.telefonischeLeitstelle, s.tagesalarmstaerke, s.loeschwasser, s.sonderobjekte,
        s.wiederherstellung, s.silvester, s.gefahrgutlagen, s.suchlagen, s.vegetationsbraende,
        s.einsatzarbeit, s.einsatzleitung, s.verlegungsfahrten,
    )
    Einstellgruppe(titel = "Einsatzregeln", stand = "${regeln.count { it }} von ${regeln.size} an") {
        // Die Einsatzdichte gibt es nur bei Zufallseinsätzen: Nachschub legt
        // allein der Generator nach.
        if (s.mode == "Zufall") {
            Etikett("Einsatzdichte")
            Stufenwahl(Rundentexte.EINSATZDICHTE, s.einsatzdichte, einstellbar) {
                setzen(Einstellungsaenderung(einsatzdichte = it))
            }
            val vorschlag = when {
                menschen.size <= 1 -> "Ruhig"
                menschen.size == 2 -> "Normal"
                else -> "Dicht"
            }
            val zusatz = buildString {
                append(Rundentexte.einsatzdichteHinweis(s.einsatzdichte))
                if (s.einsatzdichte != vorschlag) {
                    val wer = if (menschen.size == 1) "Disponent" else "Disponenten"
                    append(" Zu ${menschen.size} $wer wäre „${Rundentexte.einsatzdichte(vorschlag)}“ das übliche Maß.")
                }
                if (s.telefonischeLeitstelle) {
                    val einsaetze = if (s.offeneEinsatzGrenze == 1) "Einsatz" else "Einsätzen"
                    append(
                        " Mit telefonischer Leitstelle bleibt es bei ${s.offeneEinsatzGrenze} offenen " +
                            "$einsaetze: Ein laufendes Gespräch bindet dich ganz.",
                    )
                }
            }
            SehrLeise(zusatz)
        }
        // Störungen und Jahreszeit fehlen in der Ausbildungsschicht: Dort wird
        // der Regelfall geübt.
        if (s.mode != "Ausbildung") {
            Etikett("Störungen im Dienst")
            Stufenwahl(Rundentexte.STOERUNG, s.stoerungshaeufigkeit, einstellbar) {
                setzen(Einstellungsaenderung(stoerungshaeufigkeit = it))
            }
            SehrLeise(
                "Blinde Alarme, böswillige Notrufe, Rückrufe des Meldenden, liegengebliebene " +
                    "Fahrzeuge, Funklöcher. " + Rundentexte.stoerungHinweis(s.stoerungshaeufigkeit),
            )
            Etikett("Jahreszeit")
            Stufenwahl(Rundentexte.JAHRESZEITEN, s.jahreszeit, einstellbar) {
                setzen(Einstellungsaenderung(jahreszeit = it))
            }
            SehrLeise(
                "Sie entscheidet, welches Wetter überhaupt vorkommt — Glätte gibt es im Sommer " +
                    "nicht — und was gehäuft gemeldet wird: Flächenbrände und Badeunfälle im " +
                    "Sommer, Kaminbrände und Glätteunfälle im Winter.",
            )
        }

        Schalterzeile(
            titel = "Telefonische Leitstelle",
            an = s.telefonischeLeitstelle,
            aktiv = einstellbar,
            unterzeile = "An heißt: Notrufe kommen als Anruf herein. Es klingelt, jemand meldet " +
                "sich — und was du erfährst, hängt davon ab, was du fragst. Aus bleibt alles wie gewohnt.",
            beiWechsel = { setzen(Einstellungsaenderung(telefonischeLeitstelle = it)) },
        )
        if (s.mode != "Ausbildung") {
            REGELSCHALTER.forEach { regel ->
                Schalterzeile(
                    titel = regel.titel,
                    an = regel.wert(s),
                    aktiv = einstellbar,
                    unterzeile = regel.hinweis,
                    beiWechsel = { setzen(regel.aenderung(it)) },
                )
            }
        }
    }

    // ---------------------------------------------------------- Organisationen
    val aktiveOrgs = s.organisationen
    val aktiveHiOrgs = s.hiOrgs
    val imKreis = katalog?.landkreise?.firstOrNull { it.name == s.landkreis }?.hiOrgs.orEmpty()
    Einstellgruppe(
        titel = "Organisationen",
        stand = "${aktiveOrgs.size} von ${Rundentexte.ORGANISATIONEN.size} · " +
            if (aktiveHiOrgs.isNotEmpty()) "${aktiveHiOrgs.size} Träger" else "alle Träger",
        offenAnfangs = true,
    ) {
        Etikett("Beteiligte Organisationen")
        Pillenreihe {
            Rundentexte.ORGANISATIONEN.forEach { org ->
                Pille(
                    aufschrift = Rundentexte.organisation(org),
                    an = org in aktiveOrgs,
                    farbe = Rundentexte.organisationFarbe(org),
                    aktiv = einstellbar,
                    beiDruck = {
                        // Mindestens eine Organisation muss bleiben.
                        val neu = if (org in aktiveOrgs) aktiveOrgs - org else aktiveOrgs + org
                        if (neu.isNotEmpty()) setzen(Einstellungsaenderung(organisationen = neu))
                    },
                )
            }
        }
        Etikett(
            "Hilfsorganisationen · " +
                if (aktiveHiOrgs.isNotEmpty()) "${aktiveHiOrgs.size} gewählt" else "alle erlaubt",
        )
        Pillenreihe {
            Rundentexte.HIORGS.forEach { h ->
                Pille(
                    aufschrift = Rundentexte.traeger(h),
                    an = h in aktiveHiOrgs,
                    farbe = Rundentexte.traegerFarbe(h),
                    aktiv = einstellbar,
                    // Hervorgehoben sind die, die es im gewählten Kreis wirklich gibt.
                    zeichenVorn = if (h in imKreis) {
                        { Box(Modifier.size(6.dp).background(Farben.Amber, CircleShape)) }
                    } else {
                        null
                    },
                    beiDruck = {
                        val neu = if (h in aktiveHiOrgs) aktiveHiOrgs - h else aktiveHiOrgs + h
                        setzen(Einstellungsaenderung(hiOrgs = neu))
                    },
                )
            }
        }
        SehrLeise(
            "Ohne Auswahl fahren alle Träger mit. Hervorgehoben sind die, die es im gewählten " +
                "Landkreis wirklich gibt.",
        )
    }

    // ------------------------------------------------------------------ Vorlage
    // Nur für die Leitstelle, nur mit Konto, und nur in einer gewöhnlichen Runde:
    // Der Sandkasten hat den Leitstellenbau, die Sonderschichten bringen ihre
    // Regeln selbst mit.
    val vorlagenMoeglich = !s.sandkasten && (s.mode == "Zufall" || s.mode == "Frei")
    if (istLeitstelle && konto != null && vorlagenMoeglich) {
        LaunchedEffect(Unit) { griffe.vorlagenLaden() }
        Einstellgruppe(
            titel = "Vorlage",
            stand = if (daten.vorlagen.isNotEmpty()) "${daten.vorlagen.size} gemerkt" else "noch keine",
        ) {
            Vorlagenblock(raum, daten, griffe)
        }
    }

    // ---------------------------------------------------------- Bot-Besatzungen
    Ueberschrift("Bot-Besatzungen")
    Botbesatzungen(raum, istLeitstelle, konto, katalog, daten, griffe)

    // -------------------------------------------------------------------- Ränge
    if (istLeitstelle) {
        Ueberschrift("Ränge")
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
        ) {
            Schalterzeile(
                titel = "Freischaltungen ignorieren",
                an = s.freischaltungenIgnorieren,
                unterzeile = "Alle Sonderfahrzeuge und Zusatzfunktionen stehen jedem offen. Damit " +
                    "scheitert kein Spielabend daran, dass jemand neu ist.",
                beiWechsel = { setzen(Einstellungsaenderung(freischaltungenIgnorieren = it)) },
            )
            Schalterzeile(
                titel = "Öffentliche Runde",
                an = s.oeffentlich,
                unterzeile = "Für Fremde. Nur dann ist die Leitstelle an einen Rang gebunden — die " +
                    "eigene Runde führt man ab dem ersten Dienst selbst.",
                beiWechsel = { setzen(Einstellungsaenderung(oeffentlich = it)) },
            )
        }
    }
}

/** Der Steckbrief — was für eine Schicht das hier ist, ohne eine Gruppe zu öffnen. */
@Composable
private fun Steckbrief(raum: Raumzustand) {
    val s = raum.settings
    val zeilen = buildList {
        add("Spielmodus" to Rundentexte.modus(s.mode))
        add("Zeittempo" to if (s.zeitmodus == "Simulation") "Simulation (schneller)" else "Echtzeit")
        if (s.mode == "Zufall") add("Einsatzdichte" to Rundentexte.einsatzdichte(s.einsatzdichte))
        add("Störungen" to Rundentexte.stoerung(s.stoerungshaeufigkeit))
        add("Jahreszeit" to Rundentexte.jahreszeit(s.jahreszeit) + if (s.silvester) " · Silvester" else "")
        add(
            "Organisationen" to s.organisationen.joinToString(", ") { Rundentexte.organisation(it) }
                .ifBlank { "—" },
        )
        add(
            "Träger" to if (s.hiOrgs.isNotEmpty()) {
                s.hiOrgs.joinToString(", ") { Rundentexte.traeger(it) }
            } else {
                "alle erlaubt"
            },
        )
        add("Plätze" to "${raum.menschen.size} von ${raum.maxSpieler}")
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        zeilen.forEach { (wort, wert) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.Top,
            ) {
                Text(wort, style = Schrift.Klein, color = Farben.TextLeise, modifier = Modifier.weight(1f))
                Text(
                    wert,
                    style = Schrift.MonoKlein,
                    color = Farben.Text,
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}

/**
 * Eine einklappbare Gruppe — `details.einstellgruppe` im Web.
 *
 * <b>Die Marke im Kopf trägt den Stand</b> und nicht eine Aufzählung dessen,
 * was darin steht: Was man wissen will, ist, was eingestellt *ist*.
 */
@Composable
private fun Einstellgruppe(
    titel: String,
    stand: String,
    offenAnfangs: Boolean = false,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    var offen by remember { mutableStateOf(offenAnfangs) }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { offen = !offen },
                    role = Role.Button,
                    indication = null,
                    interactionSource = null,
                ),
        ) {
            Text(
                text = if (offen) "▾" else "▸",
                style = Schrift.MonoNormal,
                color = Farben.TextLeise,
            )
            Text(text = titel, style = Schrift.Gross, color = Farben.Text, modifier = Modifier.weight(1f))
            Text(
                text = stand,
                style = Schrift.MonoKlein,
                color = Farben.TextSehrLeise,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (offen) inhalt()
    }
}

/** Zwei Karten nebeneinander — Titel und ein Satz, was die Wahl bedeutet. */
@Composable
private fun Wahlkarten(
    wahl: List<Triple<String, String, String>>,
    gewaehlt: String,
    aktiv: Boolean,
    beiWahl: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        wahl.forEach { (wert, titel, satz) ->
            val an = gewaehlt == wert
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier
                    .weight(1f)
                    .flaeche(
                        farbe = if (an) Farben.HauchAmber else Farben.FlaecheHoch,
                        randfarbe = if (an) Farben.Amber else Farben.Rand,
                        ecke = 9.dp,
                    )
                    .clickable(
                        enabled = aktiv && !an,
                        onClick = { beiWahl(wert) },
                        role = Role.RadioButton,
                        indication = null,
                        interactionSource = null,
                    )
                    .padding(Abstand.Klein),
            ) {
                Text(
                    titel,
                    style = Schrift.Normal,
                    color = if (an) Farben.Amber else if (aktiv) Farben.Text else Farben.TextSehrLeise,
                )
                SehrLeise(satz)
            }
        }
    }
}

/** Eine Stufenwahl als Pillenreihe — Dichte, Störungen, Jahreszeit, Tempo. */
@Composable
private fun Stufenwahl(
    stufen: List<Pair<String, String>>,
    gewaehlt: String,
    aktiv: Boolean,
    beiWahl: (String) -> Unit,
) {
    Pillenreihe {
        stufen.forEach { (wert, name) ->
            Pille(
                aufschrift = name,
                an = gewaehlt == wert,
                aktiv = aktiv,
                beiDruck = { if (gewaehlt != wert) beiWahl(wert) },
            )
        }
    }
}

/** Ein Regelschalter der Einsatzregeln — Aufschrift, Erklärung und das Feld dazu. */
private class Regel(
    val titel: String,
    val hinweis: String,
    val wert: (de.pagerspass.pagerspass.netz.Rundeneinstellungen) -> Boolean,
    val aenderung: (Boolean) -> Einstellungsaenderung,
)

/**
 * Die Schalter der Einsatzregeln — Reihenfolge wie im Web: erst der Dienstalltag,
 * dann die Lagen mit eigener Fläche, zuletzt das, was nach dem Eintreffen geschieht.
 */
private val REGELSCHALTER = listOf(
    Regel(
        "Tagesalarmstärke berücksichtigen",
        "Freiwillige Feuerwehr und THW brauchen fünf bis sieben Minuten zum Ausrücken — werktags " +
            "tagsüber am längsten, weil die Besatzung auf Arbeit ist, am Feierabend und am " +
            "Wochenende am kürzesten. Berufsfeuerwehr, Rettungsdienst und Polizei sitzen auf der " +
            "Wache und sind in ein bis drei Minuten draußen; im Rettungsdienst gilt das für jeden " +
            "Träger. Aus heißt: jedes Fahrzeug ist zu jeder Stunde sofort besetzbar und fährt ohne " +
            "Ausrückzeit los.",
        { it.tagesalarmstaerke },
        { Einstellungsaenderung(tagesalarmstaerke = it) },
    ),
    Regel(
        "Löschwasser berücksichtigen",
        "Tanks laufen leer, die Wasserversorgung muss erst aufgebaut werden, und im Außenbereich " +
            "ohne Hydrantennetz bleibt nur der Pendelverkehr zur nächsten Entnahmestelle. Aus " +
            "heißt: jeder Tank reicht ewig.",
        { it.loeschwasser },
        { Einstellungsaenderung(loeschwasser = it) },
    ),
    Regel(
        "Sonderobjekte bespielen",
        "Ein Teil der Einsätze liegt in Schulen, Pflegeheimen, Betrieben oder Bahnhöfen. Dann " +
            "steht am Einsatz ein Einsatzplan mit der Zahl der Menschen im Objekt, und die Alarm- " +
            "und Ausrückeordnung fällt größer aus — aber nur, wenn das Objekt selbst betroffen " +
            "ist. Aus heißt: jede Adresse ist dieselbe Adresse.",
        { it.sonderobjekte },
        { Einstellungsaenderung(sonderobjekte = it) },
    ),
    Regel(
        "Einsatzbereitschaft wiederherstellen",
        "Nach dem Einsatz steht ein Fahrzeug für ein paar Minuten auf Status 6: der Rettungswagen " +
            "wird desinfiziert, das Löschfahrzeug füllt den Tank und tauscht Schläuche, nach einer " +
            "Hilfeleistung wird Gerät verlastet. Wer nur hingefahren und wieder abgedreht ist, " +
            "bleibt sofort verfügbar. Aus heißt: Status 2 gilt in dem Moment, in dem die Besatzung " +
            "ihn drückt.",
        { it.wiederherstellung },
        { Einstellungsaenderung(wiederherstellung = it) },
    ),
    Regel(
        "Silvesterlage",
        "Kleinbrände, brennende Container und Handverletzungen durch Feuerwerkskörper in Serie — " +
            "dazu etwas mehr Platz auf der Einsatzliste, weil in dieser Nacht mehrere " +
            "Kleinigkeiten gleichzeitig laufen. Aus heißt: ein Dienst wie jeder andere.",
        { it.silvester },
        { Einstellungsaenderung(silvester = it) },
    ),
    Regel(
        "Gefahrgutlagen mit Ausbreitung",
        "Eine ABC-Lage bekommt eine Fläche: einen Absperrbereich, der wächst, solange kein " +
            "Messtrupp vor Ort ist, und eine Ausbreitung in Windrichtung. Wer keinen Atemschutz " +
            "führt, hält gegen den Wind im Bereitstellungsraum. Liegt ein Sonderobjekt in der " +
            "Fahne, muss es geräumt werden. Aus heißt: eine Gefahrgutlage ist ein Einsatz wie " +
            "jeder andere.",
        { it.gefahrgutlagen },
        { Einstellungsaenderung(gefahrgutlagen = it) },
    ),
    Regel(
        "Vermisstensuche als Flächenlage",
        "Eine Suchlage bekommt ein Gebiet in acht Abschnitten, das abgesucht werden muss, und " +
            "eine Person, die irgendwo darin liegt. Wer sucht, entscheidet über die Zeit: eine " +
            "Rettungshundestaffel ist dreimal so schnell wie ein Löschfahrzeug, und nachts findet " +
            "niemand etwas, solange kein Licht da ist. Aus heißt: eine Vermisstensuche ist ein " +
            "Einsatz wie jeder andere.",
        { it.suchlagen },
        { Einstellungsaenderung(suchlagen = it) },
    ),
    Regel(
        "Vegetationsbrand als wachsende Fläche",
        "Ein Wald- oder Flächenbrand bekommt Hektar statt eines Punktes. Die Fläche wächst mit " +
            "Wind und Trockenheit, solange zu wenig dagegen steht — und ohne Wasser hilft auch das " +
            "beste Fahrzeug nicht. Die einzige Lage im Spiel, bei der die Zeit gegen dich " +
            "arbeitet. Aus heißt: ein Waldbrand ist ein Einsatz wie jeder andere.",
        { it.vegetationsbraende },
        { Einstellungsaenderung(vegetationsbraende = it) },
    ),
    Regel(
        "Tätigkeiten an der Einsatzstelle",
        "Jede Einsatzstelle bekommt Aufgaben aus ihrer Alarm- und Ausrückeordnung — erkunden, " +
            "arbeiten, aufräumen. Wer vor Ort ist, arbeitet sie ab; wie schnell, hängt an der Zahl " +
            "der Kräfte und daran, ob das Fahrzeug kann, was die Aufgabe verlangt. Vor den " +
            "Innenangriff kommt der Atemschutz. Aus heißt: Status 4 ist wie früher das Ende, und " +
            "die Besatzungen rücken nach ihrer eigenen Zeit ein.",
        { it.einsatzarbeit },
        { Einstellungsaenderung(einsatzarbeit = it) },
    ),
    Regel(
        "Einsatzleitung vor Ort",
        "Ab vier alarmierten Fahrzeugen übernimmt ein Führungsfahrzeug die Lage: Abschnitte " +
            "bilden, Kräfte zuteilen, Nachforderungen bündeln — und am Ende die Abschlussmeldung " +
            "geben. Aus heißt: Die Alarm- und Ausrückeordnung fordert keine Führung mehr an, es " +
            "rückt kein ELW aus, und jede Besatzung meldet sich selbst bei der Leitstelle.",
        { it.einsatzleitung },
        { Einstellungsaenderung(einsatzleitung = it) },
    ),
    Regel(
        "Terminfahrten",
        "Krankentransport und Verlegung werden bestellt statt gemeldet: mit Abholort, einem Haus, " +
            "das den Patienten erwartet, und einem Termin einige Minuten in der Zukunft. Wer zu " +
            "spät alarmiert, reißt ihn; wer gar nichts schickt, verliert den Auftrag. Aus heißt: " +
            "Sie kommen als Notruf wie jede andere Lage.",
        { it.verlegungsfahrten },
        { Einstellungsaenderung(verlegungsfahrten = it) },
    ),
)

// ----------------------------------------------------------- Rufname-Wörter

/**
 * Eigene Rufname-Wörter statt „Florian", „Rotkreuz", „Heros" — ein Feld je
 * Organisation und je gewähltem Träger. Leer gilt die BOS-Systematik.
 *
 * <b>Der Entwurf hält, was getippt ist</b>, und geht mit „Übernehmen" hinaus —
 * ein neuer Stand vom Server ersetzt ihn nur, solange nichts getippt ist.
 */
@Composable
private fun Rufnamewoerter(
    raum: Raumzustand,
    einstellbar: Boolean,
    setzen: (Einstellungsaenderung) -> Unit,
) {
    val s = raum.settings
    val schluessel = s.organisationen + s.hiOrgs
    var entwurf by remember { mutableStateOf(s.rufnamenpraefixe) }
    var bearbeitet by remember { mutableStateOf(false) }
    LaunchedEffect(s.rufnamenpraefixe) { if (!bearbeitet) entwurf = s.rufnamenpraefixe }

    Etikett("Rufname-Wörter")
    schluessel.forEach { sch ->
        val name = if (Rundentexte.istOrganisation(sch)) Rundentexte.organisation(sch) else Rundentexte.traeger(sch)
        Feld(
            wert = entwurf[sch].orEmpty(),
            beiAenderung = { neu ->
                entwurf = entwurf + (sch to neu.take(24))
                bearbeitet = true
            },
            etikett = name,
            platzhalter = Rundentexte.rufwort(sch),
            aktiv = einstellbar,
        )
    }
    if (bearbeitet && einstellbar) {
        Knopf(
            "Rufname-Wörter übernehmen",
            {
                setzen(Einstellungsaenderung(rufnamenpraefixe = entwurf.filterValues { it.isNotBlank() }))
                bearbeitet = false
            },
            kompakt = true,
        )
    }
    SehrLeise(
        "Kein Muss: Leer gilt die BOS-Systematik. Ein eigenes Wort benennt die Fahrzeuge sofort um.",
    )
}

// ---------------------------------------------------------- Funkgruppen-Maske

/** Eine Zeile der Maske — der Entwurf, nicht der Stand des Servers. */
private data class Gruppenzeile(
    val schluessel: Int,
    val id: String,
    val nummer: String,
    val name: String,
    val organisationen: List<String>,
    val hiOrgs: List<String>,
    val fuehrung: Boolean,
)

/**
 * Die Funkverkehrskreise der Runde — `lobby/FunkgruppenMaske.vue`.
 *
 * Getrennte Kanäle, damit sich mehrere Disponenten die Arbeit teilen können.
 * Ohne Eintrag läuft alles über einen Kanal — so wie bisher, und so, wie man
 * allein spielt. Pillen schicken sofort; Nummer und Name gehen mit
 * „Übernehmen", damit ein halb getippter Name nicht als Gruppe hinausgeht.
 */
@Composable
private fun FunkgruppenMaske(
    raum: Raumzustand,
    einstellbar: Boolean,
    setzen: (Einstellungsaenderung) -> Unit,
) {
    val s = raum.settings
    // Die dynamischen DMO-Gruppen einer Einsatzstelle stehen nicht in der Maske.
    val vomServer = s.funkgruppen.filter { it.einsatzId == null }
    var naechster by remember { mutableStateOf(1) }
    fun ausServer(): List<Gruppenzeile> = vomServer.map { g ->
        Gruppenzeile(naechster++, g.id, g.nummer, g.name, g.organisationen, g.hiOrgs, g.fuehrung)
    }
    var entwurf by remember { mutableStateOf(ausServer()) }
    var bearbeitet by remember { mutableStateOf(false) }
    LaunchedEffect(vomServer) { if (!bearbeitet) entwurf = ausServer() }

    fun senden(zeilen: List<Gruppenzeile>) {
        setzen(
            Einstellungsaenderung(
                funkgruppen = zeilen.filter { it.name.isNotBlank() }.map {
                    Funkgruppeneingabe(
                        id = it.id,
                        nummer = it.nummer.trim(),
                        name = it.name.trim(),
                        organisationen = it.organisationen,
                        hiOrgs = it.hiOrgs,
                        fuehrung = it.fuehrung,
                    )
                },
            ),
        )
        bearbeitet = false
    }

    fun aendern(zeile: Gruppenzeile, sofort: Boolean, neu: (Gruppenzeile) -> Gruppenzeile) {
        val liste = entwurf.map { if (it.schluessel == zeile.schluessel) neu(it) else it }
        entwurf = liste
        if (sofort) senden(liste) else bearbeitet = true
    }

    val organisationen = s.organisationen
    val traeger = s.hiOrgs

    Etikett(
        "Funkgruppen · " + if (entwurf.isEmpty()) "ein Kanal für alle" else "${entwurf.size} Kanäle",
    )
    SehrLeise(
        "Getrennte Funkverkehrskreise, damit sich mehrere Disponenten die Arbeit teilen können: " +
            "einer Rettungsdienst, einer Feuerwehr. Ohne Eintrag läuft alles über einen Kanal — so " +
            "wie bisher, und so, wie man allein spielt.",
    )

    entwurf.forEachIndexed { i, zeile ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp)
                .padding(Abstand.Klein),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(Farben.Kanal[i % Farben.Kanal.size], CircleShape),
                )
                Feld(
                    wert = zeile.nummer,
                    beiAenderung = { neu -> aendern(zeile, false) { it.copy(nummer = neu.take(12)) } },
                    platzhalter = "3301",
                    aktiv = einstellbar,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "✕",
                    {
                        val liste = entwurf.filter { it.schluessel != zeile.schluessel }
                        entwurf = liste
                        senden(liste)
                    },
                    art = Knopfart.Leise,
                    kompakt = true,
                    aktiv = einstellbar,
                )
            }
            Feld(
                wert = zeile.name,
                beiAenderung = { neu -> aendern(zeile, false) { it.copy(name = neu.take(40)) } },
                platzhalter = "Feuerwehr Landkreis …",
                aktiv = einstellbar,
            )
            Pillenreihe {
                organisationen.forEach { o ->
                    val an = o in zeile.organisationen
                    // Eine Organisation, die schon weiter oben zu Hause ist, steht rot.
                    val doppelt = an && entwurf.firstOrNull { o in it.organisationen }?.schluessel != zeile.schluessel
                    Pille(
                        aufschrift = Rundentexte.organisationKurz(o),
                        an = an,
                        farbe = if (doppelt) Farben.SignalHell else Farben.Amber,
                        aktiv = einstellbar,
                        beiDruck = {
                            aendern(zeile, true) {
                                it.copy(organisationen = if (an) it.organisationen - o else it.organisationen + o)
                            }
                        },
                    )
                }
                // Die Führung quer über alle vier: ELW, KdoW und OrgL sitzen auf
                // derselben Leitung. Es gibt höchstens eine Führungsgruppe.
                Pille(
                    aufschrift = "Führung",
                    an = zeile.fuehrung,
                    aktiv = einstellbar,
                    beiDruck = {
                        val an = !zeile.fuehrung
                        val liste = entwurf.map {
                            it.copy(fuehrung = if (it.schluessel == zeile.schluessel) an else false)
                        }
                        entwurf = liste
                        senden(liste)
                    },
                )
            }
            // Die Träger — nur, wo der Kreis mehrere führt, und nur an Zeilen mit
            // Rettungsdienst.
            if (traeger.size > 1 && "Rettungsdienst" in zeile.organisationen) {
                Pillenreihe {
                    traeger.forEach { h ->
                        val an = h in zeile.hiOrgs
                        Pille(
                            aufschrift = Rundentexte.traeger(h),
                            an = an,
                            aktiv = einstellbar,
                            beiDruck = {
                                aendern(zeile, true) { it.copy(hiOrgs = if (an) it.hiOrgs - h else it.hiOrgs + h) }
                            },
                        )
                    }
                }
                if (zeile.hiOrgs.isEmpty()) SehrLeise("alle Träger")
            }
        }
    }

    if (bearbeitet && einstellbar) {
        Knopf("Funkgruppen übernehmen", { senden(entwurf) }, kompakt = true)
    }

    Pillenreihe {
        Knopf(
            "+ Gruppe",
            {
                entwurf = entwurf + Gruppenzeile(naechster++, "", "", "", emptyList(), emptyList(), false)
                bearbeitet = true
            },
            art = Knopfart.Leise,
            kompakt = true,
            aktiv = einstellbar && entwurf.size < MAX_FUNKGRUPPEN,
        )
        Knopf(
            "Nach Organisation trennen",
            {
                val kreis = s.landkreis ?: s.ort ?: ""
                val liste = organisationen.map { o ->
                    val name = if (o == "Thw") "THW" else o
                    Gruppenzeile(
                        naechster++, "", "",
                        if (kreis.isNotBlank()) "$name $kreis" else name,
                        listOf(o), emptyList(), false,
                    )
                }
                entwurf = liste
                senden(liste)
            },
            art = Knopfart.Leise,
            kompakt = true,
            aktiv = einstellbar && organisationen.isNotEmpty(),
        )
        if (entwurf.isNotEmpty()) {
            Knopf(
                "Ein Kanal für alle",
                {
                    entwurf = emptyList()
                    senden(emptyList())
                },
                art = Knopfart.Leise,
                kompakt = true,
                aktiv = einstellbar,
            )
        }
    }
}

private const val MAX_FUNKGRUPPEN = 12

// --------------------------------------------------------------- Wachenmaske

/**
 * Welche Wachen des Kreises bespielt werden und unter welcher Nummer sie funken
 * — `lobby/WachenMaske.vue`. Ohne Änderung stehen auf allen Fahrzeuge, und die
 * Nummer ist ihr Platz in der Reihe.
 *
 * <b>In der Lobby ist damit nur das Weglassen gemeint.</b> Verschobene oder
 * selbst gebaute Wachen einer Sandkastenrunde gehen unverändert mit zurück,
 * statt beim ersten Haken abgeräumt zu werden.
 */
@Composable
private fun WachenMaske(
    raum: Raumzustand,
    daten: Lobbystand,
    einstellbar: Boolean,
    setzen: (Einstellungsaenderung) -> Unit,
    beiLaden: (String) -> Unit,
) {
    val s = raum.settings
    var offen by remember { mutableStateOf(false) }
    var nummerFuer by remember { mutableStateOf<Wache?>(null) }
    val kreis = s.landkreisId
    LaunchedEffect(offen, kreis) { if (offen && kreis != null) beiLaden(kreis) }

    val wachen = if (daten.kreiswachenFuer == kreis) daten.kreiswachen else null
    val bespielbar = wachen.orEmpty().filter { it.bespielt }
    val gewaehlt = s.wachen
    val abgewaehlt: Set<String> = if (gewaehlt.isEmpty()) {
        emptySet()
    } else {
        val dabei = gewaehlt.map { it.kennung }.toSet()
        bespielbar.map { it.kennung }.filter { it !in dabei }.toSet()
    }
    val bestand = gewaehlt.associateBy { it.kennung }
    val zurWahl = bespielbar.map { it.kennung }.toSet()
    val umbau = gewaehlt.filter { it.kennung !in zurWahl }
    fun eintrag(w: Wache): Wachenwahl = bestand[w.kennung] ?: Wachenwahl(kennung = w.kennung)
    fun nummer(w: Wache): Int = s.wachnummern[w.kennung] ?: w.wachnummer

    Etikett(
        "Wachen · " + if (wachen != null) {
            "${bespielbar.size - abgewaehlt.size} von ${bespielbar.size} besetzt"
        } else {
            "Kreiswachen"
        },
    )
    SehrLeise(
        "Welche Wachen des Kreises bespielt werden und unter welcher Nummer sie funken. Ohne " +
            "Änderung stehen auf allen Fahrzeuge, und die Nummer ist ihr Platz in der Reihe.",
    )
    Knopf(if (offen) "Wachen zuklappen" else "Wachen einstellen", { offen = !offen }, art = Knopfart.Leise, kompakt = true)

    if (!offen) return
    when {
        kreis == null -> SehrLeise(
            "Diese Runde spielt im erfundenen Standardbereich — dort gibt es keine Kreiswachen zu wählen.",
        )

        wachen == null -> SehrLeise("Wachen werden geladen …")
        bespielbar.isEmpty() -> SehrLeise("Für diesen Kreis liegen noch keine echten Wachen vor.")
        else -> {
            bespielbar.groupBy { it.organisation }.forEach { (org, liste) ->
                Etikett(Rundentexte.organisation(org))
                liste.forEach { w ->
                    val aus = w.kennung in abgewaehlt
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Hakenzeile(
                            text = w.name,
                            an = !aus,
                            aktiv = einstellbar,
                            beiWechsel = {
                                val weg = abgewaehlt.toMutableSet()
                                if (w.kennung in weg) {
                                    weg.remove(w.kennung)
                                } else {
                                    // Eine muss bleiben.
                                    if (bespielbar.size - weg.size <= 1) return@Hakenzeile
                                    weg.add(w.kennung)
                                }
                                if (weg.isEmpty() && bestand.isEmpty()) {
                                    setzen(Einstellungsaenderung(wachen = emptyList()))
                                } else {
                                    val bleiben = bespielbar.filter { it.kennung !in weg }
                                    setzen(Einstellungsaenderung(wachen = umbau + bleiben.map { eintrag(it) }))
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Knopf(
                            "${nummer(w)}",
                            { nummerFuer = w },
                            art = Knopfart.Leise,
                            kompakt = true,
                            aktiv = einstellbar && !aus,
                        )
                    }
                }
            }
            if (einstellbar && abgewaehlt.isNotEmpty()) {
                Knopf(
                    "Wieder alle besetzen",
                    {
                        setzen(
                            Einstellungsaenderung(
                                wachen = if (umbau.isNotEmpty()) umbau + bespielbar.map { eintrag(it) } else emptyList(),
                            ),
                        )
                    },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        }
    }

    nummerFuer?.let { w ->
        var text by remember(w.kennung) { mutableStateOf(nummer(w).toString()) }
        Blende(
            titel = w.name,
            beiSchliessen = { nummerFuer = null },
            breite = Dialogbreite.Schmal,
            fuss = {
                Knopf("Abbrechen", { nummerFuer = null }, art = Knopfart.Leise)
                Knopf(
                    "Übernehmen",
                    {
                        val zahl = text.trim().toIntOrNull()
                        val neu = s.wachnummern.toMutableMap()
                        if (zahl == null || zahl <= 0 || zahl == w.wachnummer) neu.remove(w.kennung)
                        else neu[w.kennung] = zahl.coerceAtMost(999)
                        setzen(Einstellungsaenderung(wachnummern = neu))
                        nummerFuer = null
                    },
                    art = Knopfart.Haupt,
                )
            },
        ) {
            Feld(
                wert = text,
                beiAenderung = { neu -> text = neu.filter { it.isDigit() }.take(3) },
                etikett = "Wachnummer",
                tastatur = KeyboardType.Number,
            )
            SehrLeise("Die Zahl vor dem ersten Schrägstrich — ${text.ifBlank { "1" }}/44/1.")
        }
    }
}

// ------------------------------------------------------------------- Vorlage

/**
 * Den Reglerstand als Vorlage speichern. Die nächste Runde startest du damit
 * vom Startbildschirm aus — samt Kreis, ohne alles neu einzustellen.
 */
@Composable
private fun Vorlagenblock(raum: Raumzustand, daten: Lobbystand, griffe: LobbyGriffe) {
    var wahl by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }

    SehrLeise(
        "Speichert alle Rundeneinstellungen dieser Lobby unter einem Namen. Die nächste Runde " +
            "startest du damit vom Startbildschirm aus — samt Kreis, ohne alles neu einzustellen.",
    )
    if (daten.vorlagen.isNotEmpty()) {
        Pillenreihe {
            Pille("Als neue Vorlage", an = wahl == null, beiDruck = { wahl = null })
            daten.vorlagen.forEach { v ->
                Pille(
                    aufschrift = "„${v.name}“ überschreiben",
                    an = wahl == v.id,
                    beiDruck = {
                        wahl = v.id
                        name = v.name
                    },
                )
            }
        }
    }
    Feld(
        wert = name,
        beiAenderung = { name = it.take(60) },
        platzhalter = "Name der Vorlage, z. B. Feierabendrunde",
    )
    Knopf(
        if (wahl != null) "Vorlage überschreiben" else "Als Vorlage speichern",
        { griffe.vorlageSpeichern(wahl, name) },
        aktiv = !daten.vorlageLaeuft && name.isNotBlank(),
        kompakt = true,
    )
    daten.vorlageMeldung?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
        ?: daten.vorlageGespeichert?.let { SehrLeise("Gespeichert. Code zum Weitergeben: ${it.code}") }
}

// ---------------------------------------------------------- Bot-Besatzungen

/**
 * Die Bot-Besatzungen — der Server besetzt diese Fahrzeuge selbst: quittieren,
 * ausrücken, eintreffen, Lage melden, nachfordern. So läuft eine Runde auch zu
 * zweit.
 */
@Composable
private fun Botbesatzungen(
    raum: Raumzustand,
    istLeitstelle: Boolean,
    konto: Konto?,
    katalog: Katalog?,
    daten: Lobbystand,
    griffe: LobbyGriffe,
) {
    val s = raum.settings
    val bots = raum.bots
    val setzen = griffe.einstellungen
    val botPlatz = raum.vehicles.size < MAX_FAHRZEUGE
    val zugLuecke = (raum.maxSpieler - raum.players.size).coerceAtLeast(0)
    var zugFuellt by remember { mutableStateOf(false) }
    var setWahlOffen by remember { mutableStateOf(false) }
    LaunchedEffect(zugFuellt) {
        if (zugFuellt) {
            kotlinx.coroutines.delay(3_000)
            zugFuellt = false
        }
    }
    // Sobald sich an der Lücke etwas tut, ist der Zug aufgefüllt.
    LaunchedEffect(zugLuecke) { zugFuellt = false }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
    ) {
        SehrLeise(
            "Der Server besetzt diese Fahrzeuge selbst: quittieren, ausrücken, eintreffen, Lage " +
                "melden, nachfordern. So läuft eine Runde auch zu zweit.",
        )
        if (istLeitstelle) {
            Pillenreihe {
                Knopf(
                    "Zug auffüllen ($zugLuecke offen)",
                    {
                        val verfuegbar = katalog?.fahrzeuge.orEmpty()
                            .filter { it.organisation in s.organisationen }
                            .filter {
                                it.hiOrg == "Keine" || it.hiOrg.isBlank() || s.hiOrgs.isEmpty() || it.hiOrg in s.hiOrgs
                            }
                        zugAuffuellen(verfuegbar.map { it.id to it.organisation }, zugLuecke).forEach { (id, anzahl) ->
                            griffe.bot(id, anzahl)
                        }
                        zugFuellt = true
                    },
                    kompakt = true,
                    aktiv = zugLuecke > 0 && botPlatz && !zugFuellt,
                )
                Knopf(
                    "Alle entfernen",
                    { bots.forEach { griffe.botEntfernen(it.id) } },
                    art = Knopfart.Leise,
                    kompakt = true,
                    aktiv = bots.isNotEmpty(),
                )
            }
        }
        if (bots.isEmpty()) {
            SehrLeise("Noch keine Bot-Besatzungen eingeteilt.", mono = true)
        } else {
            bots.forEach { b ->
                val fahrzeug = raum.vehicles.firstOrNull { it.id == b.vehicleId }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Marke("Bot", farbe = Farben.ViolettHell)
                    Text(
                        fahrzeug?.kurzname.orEmpty().ifBlank { fahrzeug?.funkrufname.orEmpty() },
                        style = Schrift.MonoKlein,
                        color = Farben.Text,
                    )
                    Text(
                        fahrzeug?.typ.orEmpty(),
                        style = Schrift.Klein,
                        color = Farben.TextSehrLeise,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (istLeitstelle) {
                        Knopf("×", { griffe.botEntfernen(b.id) }, art = Knopfart.Leise, kompakt = true)
                    }
                }
            }
        }

        if (bots.isNotEmpty() && istLeitstelle) {
            Etikett("Arbeitstempo der Bots")
            Stufenwahl(Rundentexte.BOT_TEMPO, s.botTempo, true) { setzen(Einstellungsaenderung(botTempo = it)) }

            // Welche Lagen gespielt werden — nur, wenn es etwas zu wählen gibt.
            if (daten.stichwortsets.isNotEmpty()) {
                val gewaehlt = daten.stichwortsets.firstOrNull { it.id == s.stichwortsetId }
                Wahlfeld(
                    etikett = "Stichwörter",
                    wert = gewaehlt?.let { "${it.name} · ${it.lagen} ${if (it.lagen == 1) "Lage" else "Lagen"}" }
                        ?: "Grundkatalog (bundesweit gemischt)",
                    beiDruck = { setWahlOffen = true },
                )
            }

            Schalterzeile(
                titel = "Bot-Funk",
                an = s.botFunkAktiv,
                unterzeile = "Aus heißt: Bots antworten nur mit einer knappen Quittung und melden " +
                    "sich nie von sich aus — der stille Grundzustand.",
                beiWechsel = { setzen(Einstellungsaenderung(botFunkAktiv = it)) },
            )
            if (s.botFunkAktiv) {
                // Nur wo der Server ihn anbietet — ein Haken, der still nichts
                // tut, wäre schlimmer als keiner.
                if (daten.kiFunkVerfuegbar) {
                    val premiumAktiv = konto?.premiumAktiv == true
                    val premiumImRaum = raum.players.any { !it.istBot && it.premium }
                    Schalterzeile(
                        titel = "KI-Funk (Beta) ★ Premium",
                        an = s.kiFunkAktiv && premiumImRaum,
                        aktiv = premiumAktiv,
                        unterzeile = buildString {
                            append(
                                "Besatzungen formulieren ihre Funkantworten frei und lagebezogen — " +
                                    "Befehle wirken weiterhin wie gewohnt. Die Antworten entstehen bei " +
                                    "einem externen Dienst (Mistral).",
                            )
                            if (!premiumAktiv) {
                                append(
                                    " Einschalten kann ihn ein Premium-Mitglied; einer in der Runde " +
                                        "genügt für die ganze Leitstelle. Der Bot-Funk bleibt für alle " +
                                        "da — er antwortet weiter nach Regeln, nur eben aus dem Katalog.",
                                )
                            } else if (!premiumImRaum) {
                                append(" Sobald du der Runde beitrittst, gilt dein Abo für die ganze Leitstelle.")
                            }
                        },
                        beiWechsel = { setzen(Einstellungsaenderung(kiFunkAktiv = it)) },
                    )
                }
                Etikett("Gesprächigkeit der Bots")
                Stufenwahl(Rundentexte.GESPRAECHIGKEIT, s.botGespraechigkeit, true) {
                    setzen(Einstellungsaenderung(botGespraechigkeit = it))
                }
                SehrLeise(Rundentexte.gespraechigkeitHinweis(s.botGespraechigkeit))

                Etikett("Wer sich von sich aus meldet")
                Stufenwahl(Rundentexte.ARBEITSFUNK, s.arbeitsfunk, true) {
                    setzen(Einstellungsaenderung(arbeitsfunk = it))
                }
                SehrLeise(Rundentexte.arbeitsfunkHinweis(s.arbeitsfunk))

                if (s.einsatzarbeit) {
                    Etikett("Einsatzende")
                    Stufenwahl(Rundentexte.EINSATZENDE, s.einsatzende, true) {
                        setzen(Einstellungsaenderung(einsatzende = it))
                    }
                    SehrLeise(Rundentexte.einsatzendeHinweis(s.einsatzende))
                }
            }
        }
    }

    if (setWahlOffen) {
        val grund = de.pagerspass.pagerspass.netz.Stichwortset(id = "", name = "Grundkatalog (bundesweit gemischt)")
        Wahlblende(
            titel = "Stichwörter",
            gruppen = listOf(null to listOf(grund) + daten.stichwortsets),
            aufschrift = { it.name },
            unterschrift = { satz ->
                if (satz.id.isEmpty()) null else "${satz.lagen} ${if (satz.lagen == 1) "Lage" else "Lagen"}"
            },
            gewaehlt = daten.stichwortsets.firstOrNull { it.id == s.stichwortsetId } ?: grund,
            beiWahl = { satz ->
                // Der leere Text setzt auf den Grundkatalog zurück — `null` hieße
                // „nicht mitgeschickt".
                setzen(Einstellungsaenderung(stichwortsetId = satz.id))
                setWahlOffen = false
            },
            beiSchliessen = { setWahlOffen = false },
        )
    }
}

/**
 * Den Zug auffüllen — so viele Bot-Besatzungen, wie Plätze frei sind, reihum
 * über die Organisationen, erst nach der Wunschfolge, dann der Rest.
 * Übertragen aus `zugAuffuellen` in `LobbyView.vue`.
 *
 * @return Vorlage → Anzahl; mehrere gleiche Fahrzeuge gehen in einem Befehl.
 */
internal fun zugAuffuellen(verfuegbar: List<Pair<String, String>>, luecke: Int): Map<String, Int> {
    if (verfuegbar.isEmpty() || luecke <= 0) return emptyMap()
    val wunschfolge = listOf(
        "hlf20", "dlk23", "rtw", "lf10", "elw1", "nef", "tlf3000", "rw",
        "fustw", "gkw", "gwmess", "ktw", "mtw", "grukw",
    )
    val orgVon = verfuegbar.toMap()
    val nachOrg = linkedMapOf<String, MutableList<String>>()
    wunschfolge.forEach { id ->
        val org = orgVon[id] ?: return@forEach
        nachOrg.getOrPut(org) { mutableListOf() }.add(id)
    }
    verfuegbar.forEach { (id, org) ->
        val liste = nachOrg.getOrPut(org) { mutableListOf() }
        if (id !in liste) liste.add(id)
    }
    val orgs = nachOrg.keys.toList()
    val zeiger = orgs.associateWith { 0 }.toMutableMap()
    val bedarf = linkedMapOf<String, Int>()
    repeat(luecke) { i ->
        val org = orgs[i % orgs.size]
        val liste = nachOrg.getValue(org)
        val stand = zeiger.getValue(org)
        val id = liste[stand % liste.size]
        zeiger[org] = stand + 1
        bedarf[id] = (bedarf[id] ?: 0) + 1
    }
    return bedarf
}
