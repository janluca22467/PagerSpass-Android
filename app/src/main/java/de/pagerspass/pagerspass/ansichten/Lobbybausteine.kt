package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import de.pagerspass.pagerspass.mobil.Einstellungsaenderung
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Platzanfrage
import de.pagerspass.pagerspass.netz.Raumwache
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundeneinstellungen
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.netz.Rundenvorlagenzeile
import de.pagerspass.pagerspass.netz.Spieler
import de.pagerspass.pagerspass.netz.Stichwortset
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Handgriffe der Lobby — übertragen aus `LobbyView.vue`, `WacheDialog.vue`,
 * `RufnameDialog.vue`, `BesatzungDialog.vue` und dem Abrollbehälter aus
 * `FahrzeugTableau.vue`.
 *
 * <b>Wer nicht die Leitstelle ist, sieht die Regler trotzdem</b> — gesperrt statt
 * versteckt. Was eingestellt *ist*, soll jeder lesen können; ändern kann es nur,
 * wer am Tisch sitzt.
 */

/** Die Beschriftungen der Aufzählungen des Servers — dieselben Wörter wie im Web. */
internal val RAUM_MODUS = mapOf(
    "Zufall" to "Zufallseinsätze",
    "Frei" to "Freie Vergabe",
    "Ausbildung" to "Ausbildungsschicht",
    "Tagesschicht" to "Schicht des Tages",
    "Szenario" to "Übung",
)

internal val RAUM_ORGANISATIONEN = listOf(
    "Feuerwehr" to "Feuerwehr",
    "Rettungsdienst" to "Rettungsdienst",
    "Thw" to "THW",
    "Polizei" to "Polizei",
)

internal val RAUM_TRAEGER = mapOf(
    "Drk" to "DRK", "Juh" to "Johanniter", "Mhd" to "Malteser", "Asb" to "ASB",
    "Dlrg" to "DLRG", "Wasserwacht" to "Wasserwacht", "Bergwacht" to "Bergwacht",
    "Dgzrs" to "Seenotretter", "Brh" to "Rettungshunde", "Werkfeuerwehr" to "Werkfeuerwehr",
    "Privat" to "Privater RD",
)

private val EINSATZDICHTE_HINWEIS = mapOf(
    "Ruhig" to "Alle drei Minuten eine Lage, höchstens zwei offen. Für die erste Schicht allein.",
    "Normal" to "Alle zwei Minuten eine Lage, höchstens drei offen. Allein und geübt.",
    "Dicht" to "Alle anderthalb Minuten eine Lage, höchstens vier offen. Für die Leitstelle zu zweit.",
)

private val STOERUNG_HINWEIS = mapOf(
    "Aus" to "Jede Alarmierung ist echt, jedes Fahrzeug fährt durch.",
    "Selten" to "Etwa einmal je Stunde kommt etwas dazwischen.",
    "Gelegentlich" to "Mehrmals je Stunde — für Schichten, die fordern sollen.",
)

private val EINSATZENDE_HINWEIS = mapOf(
    "Selbsttaetig" to "Auf die Abschlussmeldung hin rücken alle Kräfte ein, der Einsatz schließt sich.",
    "NachFreigabe" to "Die Kräfte bleiben, bis du die Abschlussmeldung quittierst oder abschließt.",
)

/**
 * Die Schalter der Einsatzregeln — Reihenfolge wie im Web: erst der Dienstalltag,
 * dann die Lagen mit eigener Fläche, zuletzt, was nach dem Eintreffen geschieht.
 */
private class Regelschalter(
    val titel: String,
    val hinweis: String,
    val an: (Rundeneinstellungen) -> Boolean,
    val setzen: (Boolean) -> Einstellungsaenderung,
)

private val REGELSCHALTER = listOf(
    Regelschalter(
        "Telefonische Leitstelle",
        "Notrufe kommen als Anruf herein — erst fragen, dann disponieren.",
        { it.telefonischeLeitstelle },
    ) { Einstellungsaenderung(telefonischeLeitstelle = it) },
    Regelschalter(
        "Tagesalarmstärke berücksichtigen",
        "Freiwillige brauchen werktags tagsüber am längsten zum Ausrücken.",
        { it.tagesalarmstaerke },
    ) { Einstellungsaenderung(tagesalarmstaerke = it) },
    Regelschalter(
        "Löschwasser berücksichtigen",
        "Tanks laufen leer, die Wasserversorgung muss erst aufgebaut werden.",
        { it.loeschwasser },
    ) { Einstellungsaenderung(loeschwasser = it) },
    Regelschalter(
        "Sonderobjekte bespielen",
        "Schulen, Heime, Bahnhöfe — mit Einsatzplan und größerer Ausrückeordnung.",
        { it.sonderobjekte },
    ) { Einstellungsaenderung(sonderobjekte = it) },
    Regelschalter(
        "Einsatzbereitschaft wiederherstellen",
        "Nach dem Einsatz erst desinfizieren, auffüllen, reinigen — dann Status 2.",
        { it.wiederherstellung },
    ) { Einstellungsaenderung(wiederherstellung = it) },
    Regelschalter(
        "Silvesterlage",
        "Kleinbrände, Container und Handverletzungen in Serie.",
        { it.silvester },
    ) { Einstellungsaenderung(silvester = it) },
    Regelschalter(
        "Gefahrgutlagen mit Ausbreitung",
        "Die Fahne zieht mit dem Wind; Sonderobjekte darin müssen geräumt werden.",
        { it.gefahrgutlagen },
    ) { Einstellungsaenderung(gefahrgutlagen = it) },
    Regelschalter(
        "Vermisstensuche als Flächenlage",
        "Suchabschnitte, die Trupps nacheinander absuchen.",
        { it.suchlagen },
    ) { Einstellungsaenderung(suchlagen = it) },
    Regelschalter(
        "Vegetationsbrand als wachsende Fläche",
        "Die Fläche wächst mit Wind und Trockenheit, solange zu wenig dagegen steht.",
        { it.vegetationsbraende },
    ) { Einstellungsaenderung(vegetationsbraende = it) },
    Regelschalter(
        "Tätigkeiten an der Einsatzstelle",
        "Jede Einsatzstelle bekommt Aufgaben aus ihrer Alarm- und Ausrückeordnung.",
        { it.einsatzarbeit },
    ) { Einstellungsaenderung(einsatzarbeit = it) },
    Regelschalter(
        "Einsatzleitung vor Ort",
        "Ab vier Fahrzeugen übernimmt ein Führungsfahrzeug die Lage.",
        { it.einsatzleitung },
    ) { Einstellungsaenderung(einsatzleitung = it) },
    Regelschalter(
        "Terminfahrten",
        "Krankentransport und Verlegung werden bestellt statt gemeldet — mit Termin.",
        { it.verlegungsfahrten },
    ) { Einstellungsaenderung(verlegungsfahrten = it) },
)

/**
 * Die Rundeneinstellungen — die Regler der Leitstelle.
 *
 * <b>Die Schicht des Tages ist für alle dieselbe:</b> Dort ist nichts einstellbar,
 * auch nicht für die Leitstelle. Ein abgewiesener Klick zeigte am Ende eine
 * Einstellung an, die die Runde nicht hat.
 */
@Composable
fun ColumnScope.Rundenregler(
    raum: Raumzustand,
    istLeitstelle: Boolean,
    neben: Raumneben,
    befehle: Raumbefehle,
) {
    val s = raum.settings
    val einstellbar = istLeitstelle && s.mode != "Tagesschicht"
    val menschen = raum.players.count { !it.istBot }
    val setzen: (Einstellungsaenderung) -> Unit = { if (einstellbar) befehle.einstellungen(it) }

    LaunchedEffect(einstellbar) { if (einstellbar) befehle.stichwortsetsLaden() }

    if (!istLeitstelle) {
        SehrLeise("Diese Einstellungen setzt die Leitstelle. Nachlesen geht trotzdem.")
    } else if (!einstellbar) {
        SehrLeise("Die Schicht des Tages ist für alle dieselbe — an ihren Regeln lässt sich nichts verstellen.")
    }

    // ------------------------------------------------------------ Grundregeln
    Ueberschrift("Grundregeln")
    Etikett("Spielmodus")
    Pillenreihe {
        listOf("Zufall", "Frei").forEach { m ->
            Pille(RAUM_MODUS[m] ?: m, an = s.mode == m, beiDruck = { setzen(Einstellungsaenderung(mode = m)) }, aktiv = einstellbar)
        }
        if (s.mode !in listOf("Zufall", "Frei")) Pille(RAUM_MODUS[s.mode] ?: s.mode, an = true, beiDruck = {}, aktiv = false)
    }
    SehrLeise(if (s.mode == "Frei") "Die Leitstelle denkt sich Lagen aus." else "Der Notruf klingelt von selbst.")

    Etikett("Zeittempo")
    Pillenreihe {
        Pille("Echtzeit", an = s.zeitmodus == "Echtzeit", beiDruck = { setzen(Einstellungsaenderung(zeitmodus = "Echtzeit")) }, aktiv = einstellbar)
        Pille("Simulation", an = s.zeitmodus == "Simulation", beiDruck = { setzen(Einstellungsaenderung(zeitmodus = "Simulation")) }, aktiv = einstellbar)
    }

    // Die Plätze in Zweierschritten — der Server klemmt auf 24 bis 400, und weiter
    // herunter als die Zahl der Menschen, die schon da sind, lässt er nicht.
    Etikett("Plätze — ${raum.maxSpieler}")
    val untergrenze = maxOf(24, menschen)
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        listOf(-20, -2, 2, 20).forEach { schritt ->
            Knopf(
                aufschrift = if (schritt > 0) "+$schritt" else "$schritt",
                beiDruck = {
                    setzen(Einstellungsaenderung(maxSpieler = (raum.maxSpieler + schritt).coerceIn(untergrenze, 400)))
                },
                art = Knopfart.Leise,
                kompakt = true,
                aktiv = einstellbar,
            )
        }
    }
    SehrLeise("Plätze für Mitspieler. Bot-Besatzungen zählen nicht mit.")

    Schalterzeile(
        titel = "Öffentlich",
        unterzeile = "Die Runde steht in der Liste offener Runden — Fremde können beitreten.",
        an = s.oeffentlich,
        beiWechsel = { setzen(Einstellungsaenderung(oeffentlich = it)) },
        aktiv = einstellbar,
    )

    // ---------------------------------------------------------- Einsatzregeln
    Ueberschrift("Einsatzregeln")
    if (s.mode == "Zufall") {
        Etikett("Einsatzdichte")
        Pillenreihe {
            listOf("Ruhig", "Normal", "Dicht").forEach { d ->
                Pille(d, an = s.einsatzdichte == d, beiDruck = { setzen(Einstellungsaenderung(einsatzdichte = d)) }, aktiv = einstellbar)
            }
        }
        EINSATZDICHTE_HINWEIS[s.einsatzdichte]?.let { SehrLeise(it) }
    }

    if (s.mode != "Ausbildung") {
        Etikett("Störungen im Dienst")
        Pillenreihe {
            listOf("Aus", "Selten", "Gelegentlich").forEach { h ->
                Pille(h, an = s.stoerungshaeufigkeit == h, beiDruck = { setzen(Einstellungsaenderung(stoerungshaeufigkeit = h)) }, aktiv = einstellbar)
            }
        }
        STOERUNG_HINWEIS[s.stoerungshaeufigkeit]?.let { SehrLeise(it) }

        Etikett("Jahreszeit")
        Pillenreihe {
            listOf("Fruehling" to "Frühling", "Sommer" to "Sommer", "Herbst" to "Herbst", "Winter" to "Winter").forEach { (id, name) ->
                Pille(name, an = s.jahreszeit == id, beiDruck = { setzen(Einstellungsaenderung(jahreszeit = id)) }, aktiv = einstellbar)
            }
        }
    }

    REGELSCHALTER.forEach { r ->
        Schalterzeile(
            titel = r.titel,
            unterzeile = r.hinweis,
            an = r.an(s),
            beiWechsel = { setzen(r.setzen(it)) },
            aktiv = einstellbar,
        )
    }

    // ---------------------------------------------------------- Organisationen
    Ueberschrift("Organisationen")
    Pillenreihe {
        RAUM_ORGANISATIONEN.forEach { (org, name) ->
            val an = org in s.organisationen
            Pille(
                aufschrift = name,
                an = an,
                beiDruck = {
                    val neu = if (an) s.organisationen - org else s.organisationen + org
                    // Mindestens eine Organisation muss bleiben.
                    if (neu.isNotEmpty()) setzen(Einstellungsaenderung(organisationen = neu))
                },
                aktiv = einstellbar,
            )
        }
    }
    Etikett(if (s.hiOrgs.isEmpty()) "Hilfsorganisationen — alle erlaubt" else "Hilfsorganisationen — ${s.hiOrgs.size} gewählt")
    Pillenreihe {
        RAUM_TRAEGER.forEach { (h, name) ->
            val an = h in s.hiOrgs
            Pille(
                aufschrift = name,
                an = an,
                beiDruck = { setzen(Einstellungsaenderung(hiOrgs = if (an) s.hiOrgs - h else s.hiOrgs + h)) },
                aktiv = einstellbar,
            )
        }
    }
    SehrLeise("Ohne Auswahl fahren alle Träger mit.")

    if (neben.stichwortsets.isNotEmpty()) {
        var setWahl by remember { mutableStateOf(false) }
        val gewaehlt = neben.stichwortsets.firstOrNull { it.id == s.stichwortsetId }
        Wahlfeld(
            etikett = "Stichwörter",
            wert = gewaehlt?.name ?: "Grundkatalog (bundesweit gemischt)",
            beiDruck = { setWahl = true },
            aktiv = einstellbar,
        )
        if (setWahl) {
            Wahlblende(
                titel = "Stichwörter",
                gruppen = listOf(null to (listOf<Stichwortset?>(null) + neben.stichwortsets)),
                aufschrift = { it?.name ?: "Grundkatalog (bundesweit gemischt)" },
                unterschrift = { it?.let { satz -> "${satz.lagen} ${if (satz.lagen == 1) "Lage" else "Lagen"}" } },
                gewaehlt = gewaehlt,
                beiWahl = {
                    setzen(Einstellungsaenderung(stichwortsetId = it?.id ?: ""))
                    setWahl = false
                },
                beiSchliessen = { setWahl = false },
            )
        }
    }

    // ------------------------------------------------------- Funk und Rufnamen
    Ueberschrift("Funk und Rufnamen")
    Etikett("Wachnummer — Stellen")
    Pillenreihe {
        (1..3).forEach { n ->
            Pille("1".padStart(n, '0'), an = s.wachennummerStellen == n, beiDruck = { setzen(Einstellungsaenderung(wachennummerStellen = n)) }, aktiv = einstellbar)
        }
    }
    Etikett("Laufende Nummer — Stellen")
    Pillenreihe {
        (1..3).forEach { n ->
            Pille("1".padStart(n, '0'), an = s.laufnummerStellen == n, beiDruck = { setzen(Einstellungsaenderung(laufnummerStellen = n)) }, aktiv = einstellbar)
        }
    }
    Etikett("Dauerfunk-Verstöße bis zur Entfernung")
    Pillenreihe {
        listOf(3, 4, 5).forEach { n ->
            Pille(n.toString(), an = s.funkverstossSchwelle == n, beiDruck = { setzen(Einstellungsaenderung(funkverstossSchwelle = n)) }, aktiv = einstellbar)
        }
    }
    SehrLeise(
        "Nach 20 Sekunden endet die Sendung automatisch, dann folgen Sendepause und ein " +
            "Verstoß. Beim ${s.funkverstossSchwelle}. Verstoß wird der Platz aus dieser Runde entfernt.",
    )
    // Wie die Kennungen heißen, über welchen Kanal sie laufen und auf welchen
    // Wachen sie stehen — wer das eine einstellt, stellt meistens das andere mit.
    Rufnamewoerter(raum, einstellbar, befehle)
    Funkgruppenmaske(raum, istLeitstelle, befehle)
    Wachenmaske(raum, istLeitstelle, neben, befehle)

    // ------------------------------------------------------------------- Bots
    if (raum.players.any { it.istBot }) {
        Ueberschrift("Bot-Besatzungen")
        Etikett("Arbeitstempo")
        Pillenreihe {
            listOf("Gemuetlich" to "Gemütlich", "Normal" to "Normal", "Zuegig" to "Zügig").forEach { (id, name) ->
                Pille(name, an = s.botTempo == id, beiDruck = { setzen(Einstellungsaenderung(botTempo = id)) }, aktiv = istLeitstelle)
            }
        }
        Schalterzeile(
            titel = "Bot-Funk",
            unterzeile = "Aus heißt: Bots antworten nur knapp und melden sich nie von sich aus.",
            an = s.botFunkAktiv,
            beiWechsel = { if (istLeitstelle) befehle.einstellungen(Einstellungsaenderung(botFunkAktiv = it)) },
            aktiv = istLeitstelle,
        )
        if (s.botFunkAktiv) {
            Etikett("Gesprächigkeit")
            Pillenreihe {
                listOf("Knapp" to "Knapp", "Normal" to "Normal", "Gespraechig" to "Gesprächig").forEach { (id, name) ->
                    Pille(name, an = s.botGespraechigkeit == id, beiDruck = { if (istLeitstelle) befehle.einstellungen(Einstellungsaenderung(botGespraechigkeit = id)) }, aktiv = istLeitstelle)
                }
            }
            Etikett("Wer sich von sich aus meldet")
            Pillenreihe {
                listOf("NurEinsatzleitung" to "Nur die Einsatzleitung", "AlleBesatzungen" to "Alle Besatzungen").forEach { (id, name) ->
                    Pille(name, an = s.arbeitsfunk == id, beiDruck = { if (istLeitstelle) befehle.einstellungen(Einstellungsaenderung(arbeitsfunk = id)) }, aktiv = istLeitstelle)
                }
            }
            if (s.einsatzarbeit) {
                Etikett("Einsatzende")
                Pillenreihe {
                    listOf("Selbsttaetig" to "Selbsttätig", "NachFreigabe" to "Nach Freigabe").forEach { (id, name) ->
                        Pille(name, an = s.einsatzende == id, beiDruck = { if (istLeitstelle) befehle.einstellungen(Einstellungsaenderung(einsatzende = id)) }, aktiv = istLeitstelle)
                    }
                }
                EINSATZENDE_HINWEIS[s.einsatzende]?.let { SehrLeise(it) }
            }
        }
    }
}

/**
 * Die Bitten, über die der Host zu entscheiden hat — um den Tisch und um einen
 * Platz in der vollen Runde. Für alle anderen leer.
 */
@Composable
fun ColumnScope.Anfragenkasten(raum: Raumzustand, befehle: Raumbefehle) {
    if (!raum.istHost) return
    val leitstelleVoll = raum.players.count { it.istLeitstelle } >= raum.maxLeitstellen

    if (raum.platzanfragen.isNotEmpty()) {
        Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
            Ueberschrift("Anfragen für den Tisch (${raum.platzanfragen.size})")
            raum.platzanfragen.forEach { a ->
                Anfragezeile(a, { befehle.platzanfrageEntscheiden(a.playerId, false) }) {
                    befehle.platzanfrageEntscheiden(a.playerId, true)
                }
            }
            SehrLeise(
                (if (leitstelleVoll) {
                    "Es gibt nur einen Leitstellenplatz: Wer angenommen wird, übernimmt deinen " +
                        "Tisch — du bleibst in der Runde und wählst danach ein Fahrzeug."
                } else {
                    "Wer angenommen wird, gibt sein Fahrzeug ab und disponiert mit."
                }) + " Unbeantwortete Anfragen verfallen nach fünf Minuten.",
            )
        }
    }

    if (raum.beitrittsanfragen.isNotEmpty()) {
        Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
            Ueberschrift("Bitten um einen zusätzlichen Platz (${raum.beitrittsanfragen.size})")
            raum.beitrittsanfragen.forEach { a ->
                Anfragezeile(a, { befehle.beitrittEntscheiden(a.playerId, false) }) {
                    befehle.beitrittEntscheiden(a.playerId, true)
                }
            }
            SehrLeise(
                "Die Runde ist voll (${raum.maxSpieler} Plätze). Wer angenommen wird, bekommt " +
                    "einen Platz darüber hinaus" +
                    (if (raum.ueberzaehlig > 0) " — ${raum.ueberzaehlig} davon sind schon vergeben" else "") +
                    ". Unbeantwortete Bitten verfallen nach fünf Minuten.",
            )
        }
    }
}

@Composable
private fun Anfragezeile(a: Platzanfrage, beiAblehnen: () -> Unit, beiAnnehmen: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(a.name, style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Knopf("Ablehnen", beiAblehnen, art = Knopfart.Leise, kompakt = true)
        Knopf("Annehmen", beiAnnehmen, art = Knopfart.Haupt, kompakt = true)
    }
}

/**
 * Der späte Beitritt: eine vorhandene Bot-Besatzung übernehmen. Fahrzeug, Status und
 * laufender Einsatz bleiben — man sitzt sofort im Geschehen.
 */
@Composable
fun ColumnScope.BotUebernahme(raum: Raumzustand, befehle: Raumbefehle) {
    val angebote = raum.players
        .filter { it.istBot && it.vehicleId != null }
        .mapNotNull { bot -> raum.vehicles.firstOrNull { it.id == bot.vehicleId }?.let { bot to it } }

    Ueberschrift("Bot übernehmen (${angebote.size})")
    SehrLeise(
        "Übernimm eine vorhandene Bot-Besatzung: Fahrzeug, Status und laufender Einsatz " +
            "bleiben erhalten — du sitzt sofort im Geschehen.",
    )
    if (angebote.isEmpty()) {
        Leerhinweis(
            "Im Moment ist kein Bot-Fahrzeug frei. Du kannst ein eigenes Fahrzeug in den " +
                "Dienst stellen oder einen freien Leitstellenplatz übernehmen.",
        )
    }
    angebote.forEach { (bot, f) ->
        val einsatz = raum.incidents.firstOrNull { it.id == f.einsatzId }
        Kasten(abstandInnen = Abstand.Klein) {
            Text(f.funkrufname, style = Schrift.MonoNormal, color = Farben.Text)
            SehrLeise("${f.typ} · Status ${f.status} ${f.statusText}")
            SehrLeise(
                einsatz?.let { "${it.einsatznummer} · ${it.stichwort} · ${it.adresse}" }
                    ?: "Derzeit ohne Einsatzbindung",
                mono = true,
            )
            Row {
                Knopf("Bot übernehmen", { befehle.botUebernehmen(bot.id) }, art = Knopfart.Haupt, kompakt = true)
            }
        }
    }
}

/**
 * Die Fahrzeugbaupläne, die in dieser Runde gehen — nach Trägern der Schicht und
 * „überörtlich" getrennt. Wer aus der zweiten Gruppe wählt, holt den Träger in die
 * Schicht.
 */
internal fun botGruppen(
    raum: Raumzustand,
    katalog: List<Fahrzeugvorlage>,
): List<Pair<String?, List<Fahrzeugvorlage>>> {
    val orgs = raum.settings.organisationen
    val hiOrgs = raum.settings.hiOrgs
    val passend = katalog
        .filter { orgs.isEmpty() || it.organisation in orgs }
        .filter { it.kategorie != "Abrollbehälter" }
        .sortedBy { it.typ }
    val (schicht, ueberoertlich) = passend.partition {
        it.hiOrg == "Keine" || it.hiOrg.isBlank() || hiOrgs.isEmpty() || it.hiOrg in hiOrgs
    }
    return if (ueberoertlich.isEmpty()) {
        listOf(null to schicht)
    } else {
        listOf("Träger der Schicht" to schicht, "Überörtlich anfordern" to ueberoertlich)
    }
}

/**
 * Die Bot-Besatzungen verwalten — einteilen, versetzen, entfernen. Jederzeit, nur
 * durch die Leitstelle.
 */
@Composable
fun ColumnScope.Botverwaltung(
    raum: Raumzustand,
    katalog: List<Fahrzeugvorlage>,
    befehle: Raumbefehle,
) {
    val bots = raum.players.filter { it.istBot }
    var versetzen by remember { mutableStateOf<Spieler?>(null) }
    var neuWahl by remember { mutableStateOf(false) }
    var neu by remember { mutableStateOf<Fahrzeugvorlage?>(null) }
    var anzahl by remember { mutableIntStateOf(1) }
    val gruppen = remember(raum.settings, katalog) { botGruppen(raum, katalog) }

    Ueberschrift("Bot-Besatzungen (${bots.size})")
    SehrLeise(
        "Der Server besetzt diese Fahrzeuge selbst: quittieren, ausrücken, eintreffen, Lage " +
            "melden, nachfordern. So läuft eine Runde auch zu zweit.",
    )

    bots.forEach { bot ->
        val f = raum.vehicles.firstOrNull { it.id == bot.vehicleId }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Marke("Bot", farbe = Farben.ViolettHell)
            Column(Modifier.weight(1f)) {
                Text(f?.funkrufname ?: bot.name, style = Schrift.MonoKlein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                f?.typ?.let { SehrLeise(it) }
            }
            Knopf("Versetzen", { versetzen = bot }, art = Knopfart.Leise, kompakt = true)
            Knopf("×", { befehle.botEntfernen(bot.id) }, art = Knopfart.Gefahr, kompakt = true)
        }
    }

    Wahlfeld(
        etikett = "Bot einteilen",
        wert = neu?.typ,
        beiDruck = { neuWahl = true },
        platzhalter = "Fahrzeug wählen …",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Knopf("−", { anzahl = (anzahl - 1).coerceAtLeast(1) }, art = Knopfart.Leise, kompakt = true)
        Text("$anzahl×", style = Schrift.MonoNormal, color = Farben.Text)
        Knopf("+", { anzahl = (anzahl + 1).coerceAtMost(10) }, art = Knopfart.Leise, kompakt = true)
        Knopf(
            "Einteilen",
            {
                neu?.let { befehle.botHinzufuegen(it.id, anzahl) }
                anzahl = 1
            },
            art = Knopfart.Haupt,
            aktiv = neu != null,
            kompakt = true,
        )
    }
    if (bots.isNotEmpty()) {
        Row {
            Knopf("Alle entfernen", { bots.forEach { befehle.botEntfernen(it.id) } }, art = Knopfart.Leise, kompakt = true)
        }
    }

    if (neuWahl) {
        Wahlblende(
            titel = "Bot einteilen",
            gruppen = gruppen,
            aufschrift = { vorlagenname(it) },
            unterschrift = { it.beschreibung.ifBlank { null } },
            gewaehlt = neu,
            beiWahl = {
                neu = it
                neuWahl = false
            },
            beiSchliessen = { neuWahl = false },
            suchbar = true,
        )
    }

    versetzen?.let { bot ->
        Wahlblende(
            titel = "Versetzen",
            gruppen = gruppen,
            aufschrift = { vorlagenname(it) },
            beiWahl = {
                befehle.botVersetzen(bot.id, it.id)
                versetzen = null
            },
            beiSchliessen = { versetzen = null },
            suchbar = true,
        )
    }
}

private fun vorlagenname(f: Fahrzeugvorlage): String =
    f.typ + (RAUM_TRAEGER[f.hiOrg]?.let { " · $it" } ?: "")

/**
 * Die Aufstellung mit ihren Kennungen — und der Ort, an dem Rufnamen, Wachen und
 * Abrollbehälter vergeben werden. Nur hier und nur vor Dienstbeginn: Ein Rufname,
 * den man mitten im Dienst ändert, lässt drei Mitspieler den alten rufen.
 */
@Composable
fun ColumnScope.Aufstellung(
    raum: Raumzustand,
    istLeitstelle: Boolean,
    katalog: List<Fahrzeugvorlage>,
    neben: Raumneben,
    befehle: Raumbefehle,
) {
    if (raum.vehicles.isEmpty()) return
    var umbenennen by remember { mutableStateOf<Rundenfahrzeug?>(null) }
    var umstellen by remember { mutableStateOf<Rundenfahrzeug?>(null) }
    var umruesten by remember { mutableStateOf<Rundenfahrzeug?>(null) }

    Ueberschrift("Aufstellung")
    raum.vehicles.forEach { f ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                SehrLeise(
                    f.typ + if (f.templateId == "wlfab") " · ${abBehaelterName(f.abrollbehaelterTemplateId, katalog)}" else "",
                )
            }
            if (istLeitstelle) {
                if (f.templateId == "wlfab") Knopf("AB", { umruesten = f }, art = Knopfart.Leise, kompakt = true)
                Knopf("Wache", {
                    umstellen = f
                    befehle.raumwachenLaden(raum.code)
                }, art = Knopfart.Leise, kompakt = true)
                Knopf("Name", { umbenennen = f }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
    if (istLeitstelle) {
        SehrLeise("Rufnamen und Wachen lassen sich nur hier vergeben — im laufenden Dienst stehen sie fest.")
    }

    umbenennen?.let { f -> Rufnameblende(f, befehle) { umbenennen = null } }

    umstellen?.let { f ->
        Wachenwahl(f, neben.raumwachen, befehle) { umstellen = null }
    }

    umruesten?.let { f ->
        val belegt = raum.vehicles.mapNotNull { it.abrollbehaelterTemplateId }.toSet()
        val behaelter = katalog.filter {
            it.kategorie == "Abrollbehälter" && (it.id !in belegt || it.id == f.abrollbehaelterTemplateId)
        }
        Wahlblende(
            titel = "${f.funkrufname} umrüsten",
            gruppen = listOf(null to (listOf<Fahrzeugvorlage?>(null) + behaelter)),
            aufschrift = { it?.typ?.removePrefix("WLF + ") ?: "Absetzen" },
            unterschrift = { it?.faehigkeiten?.joinToString(", ")?.ifBlank { null } },
            gewaehlt = behaelter.firstOrNull { it.id == f.abrollbehaelterTemplateId },
            beiWahl = {
                befehle.abrollbehaelterWechseln(f.id, it?.id)
                umruesten = null
            },
            beiSchliessen = { umruesten = null },
        )
    }
}

internal fun abBehaelterName(templateId: String?, katalog: List<Fahrzeugvorlage>): String {
    if (templateId == null) return "ohne AB"
    return katalog.firstOrNull { it.id == templateId }?.typ?.removePrefix("WLF + ") ?: templateId
}

/** Einen eigenen Funkrufnamen vergeben — ohne Kurzform leitet der Server sie ab. */
@Composable
private fun Rufnameblende(f: Rundenfahrzeug, befehle: Raumbefehle, beiZu: () -> Unit) {
    var name by remember(f.id) { mutableStateOf(f.funkrufname) }
    var kurz by remember(f.id) { mutableStateOf("") }

    Blende(
        titel = "Funkrufname",
        beiSchliessen = beiZu,
        fuss = {
            if (f.rufnameVonHand) {
                Knopf("Zurücksetzen", {
                    befehle.funkrufnameZuruecksetzen(f.id)
                    beiZu()
                }, art = Knopfart.Leise)
            }
            Knopf("Übernehmen", {
                befehle.funkrufnameAendern(f.id, name, kurz)
                beiZu()
            }, art = Knopfart.Haupt, aktiv = name.isNotBlank())
        },
    ) {
        SehrLeise("${f.typ} — so wird dieses Fahrzeug in der Runde gerufen.")
        Feld(wert = name, beiAenderung = { name = it.take(40) }, etikett = "Funkrufname")
        Feld(
            wert = kurz,
            beiAenderung = { kurz = it.take(12) },
            etikett = "Kurzform fürs Tableau (optional)",
            platzhalter = "leer: das letzte Wort",
        )
    }
}

/** Ein Fahrzeug auf eine andere Wache stellen — aus der Liste, aus der das Spiel verteilt. */
@Composable
private fun Wachenwahl(
    f: Rundenfahrzeug,
    wachen: List<Raumwache>,
    befehle: Raumbefehle,
    beiZu: () -> Unit,
) {
    if (wachen.isEmpty()) {
        Blende(titel = "Wache von ${f.funkrufname}", beiSchliessen = beiZu) {
            SehrLeise("Die Wachen dieser Runde werden geladen …")
        }
        return
    }
    val eigene = wachen.filter { it.organisation == f.organisation }.ifEmpty { wachen }
    Wahlblende(
        titel = "Wache von ${f.funkrufname}",
        gruppen = eigene.groupBy { it.organisation }.map { (org, liste) -> org to liste.sortedBy { it.zugnummer } },
        aufschrift = { "${it.zugnummer} · ${it.name}" },
        unterschrift = { w -> if (w.fahrzeuge.isEmpty()) null else "${w.fahrzeuge.size} Fahrzeuge stehen dort" },
        gewaehlt = wachen.firstOrNull { f.id in it.fahrzeuge },
        beiWahl = {
            befehle.wacheZuweisen(f.id, it.kennung)
            beiZu()
        },
        beiSchliessen = beiZu,
        suchbar = eigene.size > 8,
    )
}

/**
 * Den Reglerstand als Vorlage merken — nur die Leitstelle, nur in einer gewöhnlichen
 * Runde: Der Sandkasten hat den Leitstellenbau, die Sonderschichten bringen ihre
 * Regeln selbst mit.
 */
@Composable
fun ColumnScope.Rundenvorlage(raum: Raumzustand, neben: Raumneben, befehle: Raumbefehle) {
    if (raum.settings.sandkasten || raum.settings.mode !in listOf("Zufall", "Frei")) return
    var name by remember { mutableStateOf("") }
    var ueberschreiben by remember { mutableStateOf<Rundenvorlagenzeile?>(null) }
    var wahl by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { befehle.rundenvorlagenLaden() }

    Ueberschrift("Vorlage")
    SehrLeise(
        "Speichert alle Rundeneinstellungen dieser Lobby unter einem Namen. Die nächste " +
            "Runde startest du damit vom Startbildschirm aus.",
    )
    if (neben.rundenvorlagen.isNotEmpty()) {
        Wahlfeld(
            etikett = "Speichern als",
            wert = ueberschreiben?.let { "„${it.name}“ überschreiben" } ?: "Neue Vorlage",
            beiDruck = { wahl = true },
        )
    }
    Feld(
        wert = name,
        beiAenderung = { name = it.take(60) },
        platzhalter = "Name der Vorlage, z. B. Feierabendrunde",
    )
    Row {
        Knopf(
            if (ueberschreiben != null) "Vorlage überschreiben" else "Als Vorlage speichern",
            { befehle.rundenvorlageSichern(ueberschreiben?.id, name, raum.code) },
            aktiv = name.isNotBlank(),
            kompakt = true,
        )
    }
    neben.vorlageMeldung?.let { SehrLeise(it, mono = true) }

    if (wahl) {
        Wahlblende(
            titel = "Speichern als",
            gruppen = listOf(null to (listOf<Rundenvorlagenzeile?>(null) + neben.rundenvorlagen)),
            aufschrift = { it?.let { v -> "„${v.name}“ überschreiben" } ?: "Neue Vorlage" },
            gewaehlt = ueberschreiben,
            beiWahl = {
                ueberschreiben = it
                if (it != null) name = it.name
                wahl = false
            },
            beiSchliessen = { wahl = false },
        )
    }
}
