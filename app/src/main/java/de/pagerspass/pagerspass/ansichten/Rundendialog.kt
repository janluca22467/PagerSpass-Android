package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Einstellungsaenderung
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Stichwortset
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seitenblende
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Die Rundeneinstellungen — ein Dialog mit Seiten statt einer Spalte mit Reglern.
 * Übertragen aus `components/lobby/RundenDialog.vue`.
 *
 * <b>Was vorher war.</b> Rund dreißig Einstellungen standen im Reiter „Runde" der
 * Lobby untereinander — Grundregeln, Einsatzregeln, Organisationen, Funk, Wachen,
 * dazu die Regler der Bots, die erst erschienen, wenn schon ein Bot eingeteilt war.
 * Wer wissen wollte, was für eine Schicht das ist, rollte an ihnen vorbei.
 *
 * <b>Jetzt.</b> Die Lobby trägt nur den Steckbrief, und dieser Dialog alles, was man
 * daran stellen kann: oben die Wege mit ihrem Stand, darunter genau eine Seite.
 * Jede Einstellung hat einen Ort, und der Ort steht in der Wegeleiste, bevor man
 * ihn öffnet.
 *
 * <b>Für wen.</b> Für alle. Wer die Lobby nicht führt, liest nach — die Regler
 * stehen gesperrt statt versteckt: Was eingestellt *ist*, soll jeder lesen können.
 *
 * Jede Änderung geht sofort an den Server. Einen „Speichern"-Knopf gibt es deshalb
 * nicht — der Fuß sagt das, damit niemand einen sucht.
 */
@Composable
fun Rundendialog(
    raum: Raumzustand,
    seite: Rundenseite,
    beiSeite: (Rundenseite) -> Unit,
    einstellbar: Boolean,
    fuehrtLobby: Boolean,
    kiLeitstelleSelbst: Boolean,
    istLeitstelle: Boolean,
    premiumAktiv: Boolean,
    katalog: Katalog?,
    neben: Raumneben,
    befehle: Raumbefehle,
    beiSchliessen: () -> Unit,
) {
    val s = raum.settings
    val setzen: (Einstellungsaenderung) -> Unit = { if (einstellbar) befehle.einstellungen(it) }

    // Einmal beim Öffnen: Sets ändern sich in der Verwaltung, nicht in der Lobby.
    // Die Vorlagen erst hier — für alle außer der Leitstelle gibt es die Seite nicht.
    val vorlageSichtbar = istLeitstelle && !s.sandkasten && s.mode in listOf("Zufall", "Frei")
    LaunchedEffect(Unit) {
        befehle.stichwortsetsLaden()
        befehle.serverangebotLaden()
        if (vorlageSichtbar) befehle.rundenvorlagenLaden()
    }

    // Die Wegeleiste: Titel und Stand jeder Seite — „Zufallseinsätze · Echtzeit",
    // „7 von 12 an". Man sieht, was eingestellt ist, bevor man hineingeht.
    val (regelnAn, regelnVon) = regelstand(s)
    val seiten = buildList {
        add(Triple(Rundenseite.Grund, "Grundregeln", "${RAUM_MODUS[s.mode] ?: s.mode} · ${if (s.zeitmodus == "Simulation") "Simulation" else "Echtzeit"}"))
        add(
            Triple(
                Rundenseite.Lage, "Einsatzlage",
                when (s.mode) {
                    "Ausbildung" -> "Regelfall"
                    "Zufall" -> "${EINSATZDICHTE_LABEL[s.einsatzdichte] ?: s.einsatzdichte} · ${JAHRESZEIT_LABEL[s.jahreszeit] ?: s.jahreszeit}"
                    else -> "${STOERUNG_LABEL[s.stoerungshaeufigkeit] ?: s.stoerungshaeufigkeit} · ${JAHRESZEIT_LABEL[s.jahreszeit] ?: s.jahreszeit}"
                },
            ),
        )
        add(Triple(Rundenseite.Regeln, "Einsatzregeln", "$regelnAn von $regelnVon an"))
        add(
            Triple(
                Rundenseite.Orgs, "Organisationen",
                "${s.organisationen.size} von ${RAUM_ORGANISATIONEN.size}" +
                    if (s.hiOrgs.isNotEmpty()) " · ${s.hiOrgs.size} Träger" else "",
            ),
        )
        add(Triple(Rundenseite.Funk, "Funk und Rufnamen", rufnamenBeispiel(s)))
        add(Triple(Rundenseite.Wachen, "Wachen", if (s.wachen.isNotEmpty()) "angepasst" else "alle besetzt"))
        add(Triple(Rundenseite.Bots, "Bots", "${BOT_TEMPO_LABEL[s.botTempo] ?: s.botTempo} · Funk ${if (s.botFunkAktiv) "an" else "aus"}"))
        if (vorlageSichtbar) {
            add(
                Triple(
                    Rundenseite.Vorlage, "Als Vorlage merken",
                    if (neben.rundenvorlagen.isNotEmpty()) "${neben.rundenvorlagen.size} gemerkt" else "noch keine",
                ),
            )
        }
    }

    // Verschwindet die offene Seite (Vorlage nach einem Rollenwechsel), fällt der
    // Dialog auf die erste zurück, statt eine leere Fläche zu zeigen.
    val offen = if (seiten.any { it.first == seite }) seite else Rundenseite.Grund
    val titel = seiten.first { it.first == offen }.second

    // Beim Seitenwechsel oben anfangen — und den offenen Weg ins Bild holen. Öffnet
    // der Dialog vom Steckbrief aus auf „Bots", stünde er sonst hinter dem Rand.
    val rollstand = rememberScrollState()
    val wegstand = rememberLazyListState()
    LaunchedEffect(offen) {
        rollstand.scrollTo(0)
        val i = seiten.indexOfFirst { it.first == offen }
        if (i >= 0) wegstand.animateScrollToItem((i - 1).coerceAtLeast(0))
    }

    val fusssatz = when {
        !fuehrtLobby -> "Diese Einstellungen setzt die Leitstelle. Nachlesen geht."
        !einstellbar -> "Die Schicht des Tages ist für alle dieselbe — an ihren Regeln lässt sich nichts verstellen."
        else -> "Jede Änderung gilt sofort für alle in der Lobby."
    }

    Seitenblende(
        titel = titel,
        etikett = "Rundeneinstellungen · ${s.leitstelle.orEmpty()}",
        beiSchliessen = beiSchliessen,
        rollstand = rollstand,
        leiste = {
            LazyRow(
                state = wegstand,
                horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                contentPadding = PaddingValues(horizontal = Abstand.Gross, vertical = Abstand.Klein),
            ) {
                itemsIndexed(seiten) { _, (id, wegtitel, stand) ->
                    Wegknopf(wegtitel, stand, an = id == offen) { beiSeite(id) }
                }
            }
        },
        fuss = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SehrLeise(fusssatz, modifier = Modifier.weight(1f))
                Knopf("Fertig", beiSchliessen, art = Knopfart.Haupt)
            }
        },
    ) {
        val premiumImRaum = raum.players.any { !it.istBot && it.premium }
        val menschen = raum.players.count { !it.istBot }

        when (offen) {
            Rundenseite.Grund -> SeiteGrund(raum, einstellbar, kiLeitstelleSelbst, menschen, setzen)
            Rundenseite.Lage -> SeiteLage(raum, einstellbar, menschen, premiumAktiv, premiumImRaum, neben, setzen)
            Rundenseite.Regeln -> SeiteRegeln(raum, einstellbar, premiumAktiv, premiumImRaum, neben, setzen)
            Rundenseite.Orgs -> SeiteOrgs(raum, einstellbar, katalog, setzen)
            Rundenseite.Funk -> SeiteFunk(raum, einstellbar, befehle, setzen)
            Rundenseite.Wachen -> Wachenmaske(raum, einstellbar, neben, befehle, aufgeklappt = true)
            Rundenseite.Bots -> SeiteBots(raum, einstellbar, premiumAktiv, premiumImRaum, neben, setzen)
            Rundenseite.Vorlage -> Rundenvorlage(raum, neben, befehle)
        }
    }
}

/** Ein Weg der Leiste: Titel oben, Stand darunter — wie `.rd__weg` am Handy. */
@Composable
private fun Wegknopf(titel: String, stand: String, an: Boolean, beiDruck: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .defaultMinSize(minHeight = Ziel.Normal)
            .widthIn(max = 220.dp)
            .background(if (an) Farben.FlaecheAktiv else Farben.BgTief, Rundung.Klein)
            .border(1.dp, if (an) Farben.Amber else Farben.Rand, Rundung.Klein)
            .clickable(onClick = beiDruck, role = Role.Tab, indication = null, interactionSource = null)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Text(
            text = titel,
            style = Schrift.Klein.copy(fontWeight = if (an) FontWeight.Bold else FontWeight.SemiBold),
            color = if (an) Farben.Text else Farben.TextLeise,
            maxLines = 1,
        )
        Text(
            text = stand,
            style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG),
            color = if (an) Farben.AmberHell else Farben.TextSehrLeise,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Zwei Karten nebeneinander — Titel und ein Satz, wie `.modus__wahl` im Web. */
@Composable
private fun RowScope.Moduskarte(titel: String, satz: String, an: Boolean, aktiv: Boolean, beiDruck: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = Ziel.Normal)
            .background(if (an) Farben.Amber.copy(alpha = 0.10f) else Farben.BgTief, Rundung.Klein)
            .border(1.dp, if (an) Farben.Amber else Farben.Rand, Rundung.Klein)
            .clickable(enabled = aktiv, onClick = beiDruck, role = Role.RadioButton, indication = null, interactionSource = null)
            .padding(Abstand.Normal),
    ) {
        Text(
            text = titel,
            style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
            color = when {
                an -> Farben.Amber
                !aktiv -> Farben.TextSehrLeise
                else -> Farben.Text
            },
        )
        Leise(satz)
    }
}

/** Eine Wahl aus wenigen gleich breiten Pillen — `.tempo` im Web. */
@Composable
private fun <T> Tempowahl(
    werte: List<T>,
    gewaehlt: T,
    aktiv: Boolean,
    aufschrift: (T) -> String,
    beiWahl: (T) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth(),
    ) {
        werte.forEach { w ->
            Pille(
                aufschrift = aufschrift(w),
                an = w == gewaehlt,
                beiDruck = { beiWahl(w) },
                aktiv = aktiv,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Ein Block der Seite: Etikett, Inhalt, Hinweis — `.rd__block`. */
@Composable
private fun ColumnScope.Block(etikett: String?, hinweis: String? = null, inhalt: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
        if (etikett != null) Etikett(etikett)
        inhalt()
        if (!hinweis.isNullOrBlank()) SehrLeise(hinweis)
    }
}

/** Die Premium-Marke an einem KI-Schalter. */
@Composable
private fun Premiummarke() = Marke("★ Premium", farbe = Farben.AmberHell)

// ================================================================ Die Seiten

@Composable
private fun ColumnScope.SeiteGrund(
    raum: Raumzustand,
    einstellbar: Boolean,
    kiLeitstelleSelbst: Boolean,
    menschen: Int,
    setzen: (Einstellungsaenderung) -> Unit,
) {
    val s = raum.settings
    // Die KI-Leitstelle disponiert nur Zufallseinsätze — das steht hier, bevor
    // jemand drückt.
    Block(
        "Spielmodus",
        hinweis = when {
            !(s.kiLeitstelleAktiv && s.mode == "Zufall") -> null
            kiLeitstelleSelbst -> "Solange die KI-Leitstelle disponiert, bleibt es bei Zufallseinsätzen."
            else -> "Freie Vergabe schaltet die KI-Leitstelle ab."
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            Moduskarte("Zufallseinsätze", "Der Notruf klingelt von selbst.", s.mode == "Zufall", einstellbar) {
                setzen(Einstellungsaenderung(mode = "Zufall"))
            }
            Moduskarte("Freie Vergabe", "Die Leitstelle denkt sich Lagen aus.", s.mode == "Frei", einstellbar && !kiLeitstelleSelbst) {
                setzen(Einstellungsaenderung(mode = "Frei"))
            }
        }
    }

    Block("Zeittempo") {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
            Moduskarte("Echtzeit", "Fahrten und Funk laufen wie im echten Dienst.", s.zeitmodus == "Echtzeit", einstellbar) {
                setzen(Einstellungsaenderung(zeitmodus = "Echtzeit"))
            }
            Moduskarte("Simulation", "Deutlich schneller — für die ganze Runde.", s.zeitmodus == "Simulation", einstellbar) {
                setzen(Einstellungsaenderung(zeitmodus = "Simulation"))
            }
        }
    }

    // Die Plätze. Die untere Marke ist die Zahl der Menschen, die schon da sind:
    // Weiter herunter lässt der Server nicht, und der Regler soll das zeigen, statt
    // es durch eine Fehlermeldung beizubringen.
    val unten = maxOf(24, menschen).toFloat()
    var zug by remember(raum.maxSpieler) { mutableFloatStateOf(raum.maxSpieler.toFloat().coerceIn(unten, 400f)) }
    Block(
        "Plätze für Mitspieler — ${zug.toInt()}",
        hinweis = "Bot-Besatzungen zählen nicht mit — du kannst so viele einteilen, wie die Runde " +
            "Fahrzeuge trägt. Die Zahl der Leitstellenplätze folgt den Plätzen: je 45 einer, höchstens sechs.",
    ) {
        Slider(
            value = zug,
            onValueChange = { zug = (Math.round(it / 2f) * 2f).coerceIn(unten, 400f) },
            onValueChangeFinished = { setzen(Einstellungsaenderung(maxSpieler = zug.toInt())) },
            valueRange = unten..400f,
            enabled = einstellbar,
            colors = SliderDefaults.colors(
                thumbColor = Farben.Amber,
                activeTrackColor = Farben.Amber,
                inactiveTrackColor = Farben.FlaecheAktiv,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // Der Zugang: Beide Schalter beantworten dieselbe Frage — wer hier mitspielen
    // darf und womit.
    Block("Zugang") {
        Schalterzeile(
            titel = "Öffentliche Runde",
            unterzeile = "Für Fremde. Nur dann ist die Leitstelle an einen Rang gebunden — die eigene " +
                "Runde führt man ab dem ersten Dienst selbst.",
            an = s.oeffentlich,
            beiWechsel = { setzen(Einstellungsaenderung(oeffentlich = it)) },
            aktiv = einstellbar,
        )
        Schalterzeile(
            titel = "Freischaltungen ignorieren",
            unterzeile = "Alle Sonderfahrzeuge und Zusatzfunktionen stehen jedem offen. Damit scheitert " +
                "kein Spielabend daran, dass jemand neu ist.",
            an = s.freischaltungenIgnorieren,
            beiWechsel = { setzen(Einstellungsaenderung(freischaltungenIgnorieren = it)) },
            aktiv = einstellbar,
        )
    }
}

@Composable
private fun ColumnScope.SeiteLage(
    raum: Raumzustand,
    einstellbar: Boolean,
    menschen: Int,
    premiumAktiv: Boolean,
    premiumImRaum: Boolean,
    neben: Raumneben,
    setzen: (Einstellungsaenderung) -> Unit,
) {
    val s = raum.settings

    // Die Einsatzdichte gibt es nur bei Zufallseinsätzen: Nachschub legt allein der
    // Generator nach. Der Vorschlag rechnet wie `Einsatzdichten.Vorschlag` am
    // Server — Menschen, keine Bots: Sie disponieren nicht.
    if (s.mode == "Zufall") {
        val vorschlag = when {
            menschen <= 1 -> "Ruhig"
            menschen == 2 -> "Normal"
            else -> "Dicht"
        }
        Block(
            "Einsatzdichte",
            hinweis = listOfNotNull(
                EINSATZDICHTE_HINWEIS[s.einsatzdichte],
                if (s.einsatzdichte != vorschlag && !s.kiLeitstelleAktiv) {
                    "Zu $menschen ${if (menschen == 1) "Disponent" else "Disponenten"} wäre " +
                        "„${EINSATZDICHTE_LABEL[vorschlag]}“ das übliche Maß."
                } else {
                    null
                },
                if (s.kiLeitstelleAktiv) {
                    "Die KI-Leitstelle schafft jede Stufe — die Frage ist, wie viel ihr im Fahrzeug fahren wollt."
                } else {
                    null
                },
                if (s.telefonischeLeitstelle) {
                    "Mit telefonischer Leitstelle bleibt es bei ${s.offeneEinsatzGrenze} offenen " +
                        "${if (s.offeneEinsatzGrenze == 1) "Einsatz" else "Einsätzen"}: Ein laufendes " +
                        "Gespräch bindet den Tisch ganz."
                } else {
                    null
                },
            ).joinToString(" "),
        ) {
            Tempowahl(listOf("Ruhig", "Normal", "Dicht"), s.einsatzdichte, einstellbar, { EINSATZDICHTE_LABEL[it] ?: it }) {
                setzen(Einstellungsaenderung(einsatzdichte = it))
            }
        }

        // KI-Lagen: nur wo der Server es anbietet, gesperrt ohne eigenes Abo.
        if (neben.kiFunkVerfuegbar) {
            Row(verticalAlignment = Alignment.CenterVertically) { Premiummarke() }
            Schalterzeile(
                titel = "KI-Lagen (Beta)",
                unterzeile = "Die Einsätze erfindet eine KI — Stichwort, Meldebild und Ausrückeordnung, " +
                    "passend zu den Fahrzeugen dieser Runde. Fällt sie aus, kommen die Lagen wie gewohnt " +
                    "aus dem Katalog. Die Lagen entstehen bei einem externen Dienst (Mistral)." +
                    if (!premiumAktiv) " Einschalten kann sie ein Premium-Mitglied." else "",
                an = s.kiLagenAktiv && premiumImRaum,
                beiWechsel = { setzen(Einstellungsaenderung(kiLagenAktiv = it)) },
                aktiv = einstellbar && premiumAktiv,
            )
        }
    }

    if (s.mode != "Ausbildung") {
        Block(
            "Störungen im Dienst",
            hinweis = "Blinde Alarme, böswillige Notrufe, Rückrufe des Meldenden, liegengebliebene " +
                "Fahrzeuge, Funklöcher. ${STOERUNG_HINWEIS[s.stoerungshaeufigkeit].orEmpty()}",
        ) {
            Tempowahl(listOf("Aus", "Selten", "Gelegentlich"), s.stoerungshaeufigkeit, einstellbar, { STOERUNG_LABEL[it] ?: it }) {
                setzen(Einstellungsaenderung(stoerungshaeufigkeit = it))
            }
        }
        Block(
            "Jahreszeit",
            hinweis = "Sie entscheidet, welches Wetter überhaupt vorkommt — Glätte gibt es im Sommer " +
                "nicht — und was gehäuft gemeldet wird: Flächenbrände und Badeunfälle im Sommer, " +
                "Kaminbrände und Glätteunfälle im Winter.",
        ) {
            Tempowahl(listOf("Fruehling", "Sommer", "Herbst", "Winter"), s.jahreszeit, einstellbar, { JAHRESZEIT_LABEL[it] ?: it }) {
                setzen(Einstellungsaenderung(jahreszeit = it))
            }
        }
    }

    // Ein Auswahlfeld mit genau einem Eintrag ist keine Wahl — ohne gepflegtes
    // Set steht es gar nicht da.
    if (neben.stichwortsets.isNotEmpty()) {
        var setWahl by remember { mutableStateOf(false) }
        val grund = if (s.staat == "Deutschland") {
            "Grundkatalog (bundesweit gemischt)"
        } else {
            "Grundkatalog (Stichworte ${s.bundesland ?: s.staat})"
        }
        val gewaehlt = neben.stichwortsets.firstOrNull { it.id == s.stichwortsetId }
        Block(
            null,
            hinweis = "Welche Lagen gemeldet werden. Ein Set eines Bundeslands oder Kantons bringt dessen " +
                "Stichwörter mit; in Österreich und der Schweiz spricht schon der Grundkatalog die " +
                "Stichworte des Landes.",
        ) {
            Wahlfeld(
                etikett = "Stichwörter",
                wert = gewaehlt?.let { "${it.name} · ${it.lagen} ${if (it.lagen == 1) "Lage" else "Lagen"}" } ?: grund,
                beiDruck = { setWahl = true },
                aktiv = einstellbar,
            )
        }
        if (setWahl) {
            Wahlblende(
                titel = "Stichwörter",
                gruppen = listOf(null to (listOf<Stichwortset?>(null) + neben.stichwortsets)),
                aufschrift = { it?.name ?: grund },
                unterschrift = { it?.let { satz -> "${satz.lagen} ${if (satz.lagen == 1) "Lage" else "Lagen"}" } },
                gewaehlt = gewaehlt,
                beiWahl = {
                    setzen(Einstellungsaenderung(stichwortsetId = it?.id ?: ""))
                    setWahl = false
                },
                beiSchliessen = { setWahl = false },
            )
        }
    } else if (s.mode == "Ausbildung") {
        Leerhinweis("In der Ausbildungsschicht wird der Regelfall geübt — Dichte, Störungen und Jahreszeit bringt sie selbst mit.")
    }
}

@Composable
private fun ColumnScope.SeiteRegeln(
    raum: Raumzustand,
    einstellbar: Boolean,
    premiumAktiv: Boolean,
    premiumImRaum: Boolean,
    neben: Raumneben,
    setzen: (Einstellungsaenderung) -> Unit,
) {
    val s = raum.settings
    val regeln = regelnImModus(s)

    Regelgruppe.entries.forEach { gruppe ->
        val inGruppe = regeln.filter { it.gruppe == gruppe }
        if (inGruppe.isEmpty()) return@forEach
        Block("${gruppe.titel} · ${gruppe.unter}") {
            inGruppe.forEach { r ->
                val zusatz = if (r.schluessel == "telefonischeLeitstelle" && s.kiLeitstelleAktiv) {
                    " Die KI-Leitstelle fragt dann selbst ab: Lagen werden etwas später alarmiert, und " +
                        "nur mit dem, was der Anrufer gesagt hat."
                } else {
                    ""
                }
                Schalterzeile(
                    titel = r.titel,
                    unterzeile = r.text + zusatz,
                    an = r.an(s),
                    beiWechsel = { setzen(r.setzen(it)) },
                    aktiv = einstellbar,
                )

                // KI-Anrufe hängen an der telefonischen Leitstelle und stehen deshalb
                // direkt darunter — nur wo der Server den Zugang anbietet.
                if (r.schluessel == "telefonischeLeitstelle" && neben.kiFunkVerfuegbar && s.telefonischeLeitstelle) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                        modifier = Modifier.padding(start = Abstand.Gross),
                    ) {
                        Premiummarke()
                        Schalterzeile(
                            titel = "KI-Anrufe (Beta)",
                            unterzeile = "Anrufer sprechen frei statt aus dem Katalog — aufgeregt, beruhigt, " +
                                "menschlich. Was sie verraten, hängt weiter allein an deinen Fragen. " +
                                "Eingeschaltet geht, was du am Telefon sagst, an einen externen Dienst " +
                                "(Mistral, siehe Datenschutzerklärung Ziffer 8 d)." +
                                (if (!premiumAktiv) " Einschalten kann sie ein Premium-Mitglied; einer in der Runde genügt." else "") +
                                if (s.kiLeitstelleAktiv) {
                                    " Fragt die KI-Leitstelle ab, hört niemand zu — dann sprechen die Anrufer " +
                                        "aus dem Katalog, und es geht nichts hinaus."
                                } else {
                                    ""
                                },
                            an = s.kiAnrufeAktiv && premiumImRaum,
                            beiWechsel = { setzen(Einstellungsaenderung(kiAnrufeAktiv = it)) },
                            aktiv = einstellbar && premiumAktiv,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SeiteOrgs(
    raum: Raumzustand,
    einstellbar: Boolean,
    katalog: Katalog?,
    setzen: (Einstellungsaenderung) -> Unit,
) {
    val s = raum.settings
    // Träger, die im Kreis tatsächlich Wachen betreiben — hervorgehoben, kein Zwang.
    val imKreis = katalog?.landkreise?.firstOrNull { it.id == s.landkreisId || it.name == s.landkreis }?.hiOrgs.orEmpty()

    Block(
        "Beteiligte Organisationen",
        hinweis = "Mindestens eine bleibt dabei. Wer abgewählt ist, rückt nicht aus und steht auch in " +
            "der Fahrzeugwahl nicht zur Wahl.",
    ) {
        Pillenreihe {
            RAUM_ORGANISATIONEN.forEach { (org, name) ->
                val an = org in s.organisationen
                Pille(
                    aufschrift = name,
                    an = an,
                    farbe = orgFarbe(org),
                    beiDruck = {
                        val neu = if (an) s.organisationen - org else s.organisationen + org
                        if (neu.isNotEmpty()) setzen(Einstellungsaenderung(organisationen = neu))
                    },
                    aktiv = einstellbar,
                )
            }
        }
    }

    Block(
        "Hilfsorganisationen · ${if (s.hiOrgs.isEmpty()) "alle erlaubt" else "${s.hiOrgs.size} gewählt"}",
        hinweis = "Ohne Auswahl fahren alle Träger mit. Mit Punkt sind die, die es im gewählten " +
            "Landkreis wirklich gibt.",
    ) {
        Pillenreihe {
            RAUM_TRAEGER.forEach { (h, name) ->
                val an = h in s.hiOrgs
                Pille(
                    aufschrift = if (h in imKreis) "$name ·" else name,
                    an = an,
                    beiDruck = { setzen(Einstellungsaenderung(hiOrgs = if (an) s.hiOrgs - h else s.hiOrgs + h)) },
                    aktiv = einstellbar,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SeiteFunk(
    raum: Raumzustand,
    einstellbar: Boolean,
    befehle: Raumbefehle,
    setzen: (Einstellungsaenderung) -> Unit,
) {
    val s = raum.settings
    Block(
        "Rufnamenformat · Beispiel ${rufnamenBeispiel(s)}",
        hinweis = "Eine Mindestbreite, keine Obergrenze: Wache 55 bleibt „55“. Fahrzeuge, denen die " +
            "Leitstelle einen eigenen Rufnamen gegeben hat, bleiben unberührt.",
    ) {
        SehrLeise("Wachnummer")
        Tempowahl(listOf(1, 2, 3), s.wachennummerStellen, einstellbar, { "1".padStart(it, '0') }) {
            setzen(Einstellungsaenderung(wachennummerStellen = it))
        }
        SehrLeise("Laufende Nummer")
        Tempowahl(listOf(1, 2, 3), s.laufnummerStellen, einstellbar, { "1".padStart(it, '0') }) {
            setzen(Einstellungsaenderung(laufnummerStellen = it))
        }
    }

    Rufnamewoerter(raum, einstellbar, befehle)
    Funkgruppenmaske(raum, einstellbar, befehle)

    Block(
        "Dauerfunk-Verstöße bis zur Entfernung",
        hinweis = "Nach 20 Sekunden endet die Sendung automatisch. Es folgen 1 Minute Sendepause und " +
            "ein Verstoß. Beim ${s.funkverstossSchwelle}. Verstoß wird der Platz aus dieser Runde " +
            "entfernt; neue Runden bleiben davon unberührt.",
    ) {
        Tempowahl(listOf(3, 4, 5), s.funkverstossSchwelle, einstellbar, { it.toString() }) {
            setzen(Einstellungsaenderung(funkverstossSchwelle = it))
        }
    }
}

@Composable
private fun ColumnScope.SeiteBots(
    raum: Raumzustand,
    einstellbar: Boolean,
    premiumAktiv: Boolean,
    premiumImRaum: Boolean,
    neben: Raumneben,
    setzen: (Einstellungsaenderung) -> Unit,
) {
    val s = raum.settings
    val bots = raum.players.count { it.istBot }

    // Es steht immer da — wer vorher wissen will, wie gesprächig sie sein werden,
    // findet es, bevor der erste Bot eingeteilt ist.
    Leise(
        "Der Server besetzt Bot-Fahrzeuge selbst: quittieren, ausrücken, eintreffen, Lage melden, " +
            "nachfordern. " + when (bots) {
                0 -> "Noch ist keine eingeteilt — das geht in der Lobby unter „Bot-Besatzungen“."
                1 -> "In dieser Runde fährt eine Besatzung."
                else -> "In dieser Runde fahren $bots Besatzungen."
            },
    )

    Block("Arbeitstempo") {
        Tempowahl(listOf("Gemuetlich", "Normal", "Zuegig"), s.botTempo, einstellbar, { BOT_TEMPO_LABEL[it] ?: it }) {
            setzen(Einstellungsaenderung(botTempo = it))
        }
    }

    Block("Funk") {
        Schalterzeile(
            titel = "Bot-Funk",
            unterzeile = "Aus heißt: Bots antworten nur mit einer knappen Quittung und melden sich nie " +
                "von sich aus — der stille Grundzustand.",
            an = s.botFunkAktiv,
            beiWechsel = { setzen(Einstellungsaenderung(botFunkAktiv = it)) },
            aktiv = einstellbar,
        )
        // Ohne eigenes Abo gesperrt statt abweisbar: Eine Absage löst keinen neuen
        // Raumstand aus, und der Schalter stünde sichtbar auf „an".
        if (s.botFunkAktiv && neben.kiFunkVerfuegbar) {
            Premiummarke()
            Schalterzeile(
                titel = "KI-Funk (Beta)",
                unterzeile = "Besatzungen formulieren ihre Funkantworten frei und lagebezogen — Befehle " +
                    "wirken weiterhin wie gewohnt. Mit KI-Leitstelle versteht und beantwortet auch sie " +
                    "jeden Funkspruch frei. Die Antworten entstehen bei einem externen Dienst (Mistral)." +
                    when {
                        !premiumAktiv -> " Einschalten kann ihn ein Premium-Mitglied; einer in der Runde " +
                            "genügt für die ganze Leitstelle. Der Bot-Funk bleibt für alle da — er antwortet " +
                            "weiter nach Regeln, nur eben aus dem Katalog."
                        !premiumImRaum -> " Sobald du der Runde beitrittst, gilt dein Abo für die ganze Leitstelle."
                        else -> ""
                    },
                an = s.kiFunkAktiv && premiumImRaum,
                beiWechsel = { setzen(Einstellungsaenderung(kiFunkAktiv = it)) },
                aktiv = einstellbar && premiumAktiv,
            )
        }
    }

    if (s.botFunkAktiv) {
        Block("Gesprächigkeit", hinweis = BOT_GESPRAECHIGKEIT_HINWEIS[s.botGespraechigkeit]) {
            Tempowahl(listOf("Knapp", "Normal", "Gespraechig"), s.botGespraechigkeit, einstellbar, { BOT_GESPRAECHIGKEIT_LABEL[it] ?: it }) {
                setzen(Einstellungsaenderung(botGespraechigkeit = it))
            }
        }
        // Außerhalb der Einsatzarbeit-Bedingung: Der Schalter regelt auch die
        // Eigenmeldung auf der Anfahrt („Lage auf Sicht").
        Block("Wer sich von sich aus meldet", hinweis = ARBEITSFUNK_HINWEIS[s.arbeitsfunk]) {
            Tempowahl(listOf("NurEinsatzleitung", "AlleBesatzungen"), s.arbeitsfunk, einstellbar, { ARBEITSFUNK_LABEL[it] ?: it }) {
                setzen(Einstellungsaenderung(arbeitsfunk = it))
            }
        }
        if (s.einsatzarbeit) {
            Block("Einsatzende", hinweis = EINSATZENDE_HINWEIS[s.einsatzende]) {
                Tempowahl(listOf("Selbsttaetig", "NachFreigabe"), s.einsatzende, einstellbar, { EINSATZENDE_LABEL[it] ?: it }) {
                    setzen(Einstellungsaenderung(einsatzende = it))
                }
            }
        }
    }
}
