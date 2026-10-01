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

// Die Regler der Runde stehen seit 5.0.0.26 im Rundendialog (`Rundendialog.kt`),
// ihre Schalter und Beschriftungen in `Rundenregeln.kt`.

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
    beiVerhalten: (() -> Unit)? = null,
) {
    val bots = raum.players.filter { it.istBot }
    var versetzen by remember { mutableStateOf<Spieler?>(null) }
    var neuWahl by remember { mutableStateOf(false) }
    var neu by remember { mutableStateOf<Fahrzeugvorlage?>(null) }
    var anzahl by remember { mutableIntStateOf(1) }
    val gruppen = remember(raum.settings, katalog) { botGruppen(raum, katalog) }

    // Die Lücke zum vollen Zug: so viele Fahrzeuge, wie die Runde groß ist —
    // gezählt werden alle Besatzungen, Menschen wie Bots.
    val luecke = (raum.maxSpieler - raum.players.size).coerceAtLeast(0)
    // Sperre gegen den Doppeldruck: Bis der Server die neuen Bots meldet, steht die
    // Lücke noch auf dem alten Wert. Frei wird sie, sobald sie sich bewegt.
    var fuelltBei by remember { mutableStateOf<Int?>(null) }
    if (fuelltBei != null && fuelltBei != luecke) fuelltBei = null

    Ueberschrift("Bot-Besatzungen")
    SehrLeise(
        "Der Server besetzt diese Fahrzeuge selbst: quittieren, ausrücken, eintreffen, Lage " +
            "melden, nachfordern. So läuft eine Runde auch zu zweit.",
    )

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Knopf(
            "Zug auffüllen ($luecke offen)",
            {
                fuelltBei = luecke
                zugAuffuellen(gruppen.firstOrNull()?.second.orEmpty(), luecke).forEach { (id, n) ->
                    befehle.botHinzufuegen(id, n)
                }
            },
            aktiv = luecke > 0 && fuelltBei == null && raum.vehicles.size < 400,
            kompakt = true,
        )
        Knopf(
            "Alle entfernen",
            { bots.forEach { befehle.botEntfernen(it.id) } },
            art = Knopfart.Leise,
            aktiv = bots.isNotEmpty(),
            kompakt = true,
        )
    }
    if (bots.isEmpty()) SehrLeise("Noch keine Bot-Besatzungen eingeteilt.", mono = true)

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
    // Das Verhalten der Bots steht im Rundendialog auf der Seite „Bots". Der Weg
    // dorthin steht hier, wo man an Bots denkt.
    if (beiVerhalten != null) {
        val s = raum.settings
        Row {
            Knopf(
                "Verhalten der Bots · ${BOT_TEMPO_LABEL[s.botTempo] ?: s.botTempo}, Funk ${if (s.botFunkAktiv) "an" else "aus"}",
                beiVerhalten,
                art = Knopfart.Leise,
                kompakt = true,
            )
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

/**
 * Füllt die Aufstellung mit einem sinnvollen Zug auf, statt vierzehnmal einzeln
 * drücken zu lassen — dieselbe Rechnung wie `zugAuffuellen` im Web.
 *
 * Die Wunschliste ist von Natur aus Feuerwehr-lastig. Stur der Reihe nach aufgefüllt
 * bekäme die Feuerwehr immer den Löwenanteil der Bots; deshalb erst nach Organisation
 * gruppieren und reihum verteilen, innerhalb einer Organisation über ihre eigenen
 * Typen — erst die gewünschten, dann alle übrigen. Je Typ ein Sammelbefehl.
 */
internal fun zugAuffuellen(verfuegbar: List<Fahrzeugvorlage>, luecke: Int): Map<String, Int> {
    if (verfuegbar.isEmpty() || luecke <= 0) return emptyMap()
    val wunsch = listOf(
        "hlf20", "dlk23", "rtw", "lf10", "elw1", "nef", "tlf3000", "rw", "fustw", "gkw",
        "gwmess", "ktw", "mtw", "grukw",
    )
    val nachOrg = linkedMapOf<String, MutableList<String>>()
    wunsch.forEach { id ->
        val f = verfuegbar.firstOrNull { it.id == id } ?: return@forEach
        nachOrg.getOrPut(f.organisation) { mutableListOf() }.add(f.id)
    }
    verfuegbar.forEach { f ->
        val liste = nachOrg.getOrPut(f.organisation) { mutableListOf() }
        if (f.id !in liste) liste.add(f.id)
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

    // Der Titel steht im Kopf des Rundendialogs („Als Vorlage merken").
    de.pagerspass.pagerspass.ui.bausteine.Leise(
        "Speichert alle Rundeneinstellungen dieser Lobby unter einem Namen. Die nächste " +
            "Runde startest du damit vom Startbildschirm aus — samt Kreis, ohne alles neu einzustellen.",
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
