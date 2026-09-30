package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Einstellungsaenderung
import de.pagerspass.pagerspass.mobil.Funkgruppenentwurf
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.netz.Kreiswache
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Wachenwahl
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Wie im Kreis gefunkt wird — übertragen aus `LobbyView.vue` (Rufname-Wörter),
 * `FunkgruppenMaske.vue` und `WachenMaske.vue`.
 *
 * <b>Getippt wird lokal, gesendet beim Verlassen des Felds.</b> Jeder Buchstabe
 * einzeln über den Hub wäre ein Kommando je Tastendruck, und jedes davon benennte
 * alle Fahrzeuge um. Und der Entwurf folgt dem Server nur, wenn sich dort
 * wirklich etwas geändert hat — nie in das Feld, in dem gerade jemand schreibt.
 */

/** Das Rufname-Wort der BOS-Systematik — `PRAEFIX_ORG` und `PRAEFIX_HIORG` im Web. */
private val VORGABEWORT = mapOf(
    "Feuerwehr" to "Florian", "Rettungsdienst" to "Rotkreuz", "Thw" to "Heros", "Polizei" to "Peter",
    "Drk" to "Rotkreuz", "Juh" to "Akkon", "Mhd" to "Johannes", "Asb" to "Sama", "Dlrg" to "Pelikan",
    "Bergwacht" to "Bergwacht", "Wasserwacht" to "Wasserwacht", "Dgzrs" to "Seenot",
    "Brh" to "Rettungshund", "Werkfeuerwehr" to "Florian", "Privat" to "Rettung",
)

private val ORG_KURZ = mapOf("Feuerwehr" to "FW", "Rettungsdienst" to "RD", "Thw" to "THW", "Polizei" to "POL")

private fun schluesselName(s: String): String =
    RAUM_ORGANISATIONEN.firstOrNull { it.first == s }?.second ?: RAUM_TRAEGER[s] ?: s

/**
 * Ruft `beiVerlassen`, sobald der Fokus aus dem Feld darunter geht — das
 * `@change` des Web. Beim ersten Aufbau (noch nie im Fokus) geschieht nichts.
 */
@Composable
private fun Modifier.beimVerlassen(beiFokus: (Boolean) -> Unit, beiVerlassen: () -> Unit): Modifier {
    // Gemerkt über das Neuzeichnen hinweg: Jeder neue Raumstand baut die Kette
    // neu, und ein frisches „hatte keinen Fokus" verschluckte das Verlassen.
    val merker = remember { BooleanArray(1) }
    return onFocusChanged { f ->
        val hatte = merker[0]
        merker[0] = f.hasFocus
        if (hatte && !f.hasFocus) beiVerlassen()
        if (hatte != f.hasFocus) beiFokus(f.hasFocus)
    }
}

// ============================================================ Rufname-Wörter

/**
 * Die Rufname-Wörter — ein Feld je aktiver Organisation und je gewähltem Träger;
 * der Platzhalter zeigt das Wort der BOS-Systematik, das ohne Eintrag gilt. Eine
 * Beschriftung wie das Rufnamenformat, kein Umbau — deshalb in jeder Runde.
 */
@Composable
fun ColumnScope.Rufnamewoerter(raum: Raumzustand, einstellbar: Boolean, befehle: Raumbefehle) {
    val s = raum.settings
    val schluessel = s.organisationen + s.hiOrgs
    if (schluessel.isEmpty()) return

    var entwurf by remember { mutableStateOf(s.rufnamenpraefixe) }
    var inArbeit by remember { mutableStateOf<String?>(null) }
    var vomServer by remember { mutableStateOf(s.rufnamenpraefixe) }
    if (vomServer != s.rufnamenpraefixe) {
        vomServer = s.rufnamenpraefixe
        val neu = s.rufnamenpraefixe.toMutableMap()
        inArbeit?.let { k -> entwurf[k]?.let { neu[k] = it } }
        entwurf = neu
    }

    fun senden() {
        if (!einstellbar) return
        val sauber = entwurf.mapValues { it.value.trim() }
        if (sauber != s.rufnamenpraefixe) befehle.einstellungen(Einstellungsaenderung(rufnamenpraefixe = sauber))
    }

    Etikett("Rufname-Wörter")
    schluessel.forEach { k ->
        Feld(
            wert = entwurf[k].orEmpty(),
            beiAenderung = { entwurf = entwurf + (k to it.take(24)) },
            etikett = schluesselName(k),
            platzhalter = VORGABEWORT[k] ?: "Einheit",
            aktiv = einstellbar,
            weiterTaste = ImeAction.Done,
            modifier = Modifier.beimVerlassen({ if (it) inArbeit = k else if (inArbeit == k) inArbeit = null }, ::senden),
        )
    }
    SehrLeise("Kein Muss: Leer gilt die BOS-Systematik. Ein eigenes Wort benennt die Fahrzeuge sofort um.")
}

// ============================================================ Funkgruppen

private const val MAX_GRUPPEN = 12

/**
 * Die Funkverkehrskreise der Runde — Nummer, Name, wer darauf zu Hause ist.
 *
 * <b>Frei gepflegt und nicht aus den Organisationen abgeleitet</b>: Der große
 * Kreis fährt je Organisation eine eigene Gruppe, der kleine legt Polizei und THW
 * zusammen. Für den Normalfall gibt es trotzdem „Nach Organisation trennen".
 *
 * <b>Eine Organisation ist auf der ersten Gruppe zu Hause, auf der sie steht</b> —
 * dieselbe Regel, nach der der Server die Stammgruppe sucht. Steht sie weiter
 * unten noch einmal, ist die Pille dort gedämpft.
 */
@Composable
fun ColumnScope.Funkgruppenmaske(raum: Raumzustand, istLeitstelle: Boolean, befehle: Raumbefehle) {
    val server = raum.settings.funkgruppen.filter { it.einsatzId == null }.map {
        Funkgruppenentwurf(it.id, it.nummer, it.name, it.organisationen, it.hiOrgs, it.fuehrung)
    }
    var entwurf by remember { mutableStateOf(server) }
    var vomServer by remember { mutableStateOf(server) }
    var fokus by remember { mutableStateOf<Int?>(null) }
    if (vomServer != server) {
        vomServer = server
        // Die Zeile, in der getippt wird, und noch nicht gesendete leere Zeilen
        // bleiben stehen — ein fremder Raumstand räumte sie sonst weg.
        val f = fokus
        val offen = entwurf.filter { it.id.isBlank() && it.name.isBlank() }
        entwurf = server.mapIndexed { i, g -> if (i == f) entwurf.getOrNull(i)?.let { e -> g.copy(nummer = e.nummer, name = e.name) } ?: g else g } + offen
    }

    val organisationen = raum.settings.organisationen
    val traeger = raum.settings.hiOrgs

    fun senden(zeilen: List<Funkgruppenentwurf> = entwurf) {
        if (!istLeitstelle) return
        // Eine Zeile ohne Namen weist der Server ab; sie wegzulassen ist freundlicher.
        befehle.einstellungen(Einstellungsaenderung(funkgruppen = zeilen.filter { it.name.isNotBlank() }))
    }

    fun aendern(i: Int, neu: Funkgruppenentwurf, gleichSenden: Boolean) {
        entwurf = entwurf.toMutableList().also { it[i] = neu }
        if (gleichSenden) senden()
    }

    Etikett("Funkgruppen — ${if (entwurf.isEmpty()) "ein Kanal für alle" else "${entwurf.size} Kanäle"}")
    SehrLeise(
        "Getrennte Funkverkehrskreise, damit sich mehrere Disponenten die Arbeit teilen können: einer " +
            "Rettungsdienst, einer Feuerwehr. Ohne Eintrag läuft alles über einen Kanal.",
    )

    entwurf.forEachIndexed { i, zeile ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.Flaeche, Rundung.Klein)
                .padding(Abstand.Klein),
        ) {
            Box(Modifier.width(4.dp).height(44.dp).background(Farben.kanal(i), Rundung.Rund))
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Feld(
                        wert = zeile.nummer,
                        beiAenderung = { aendern(i, zeile.copy(nummer = it.take(12)), false) },
                        platzhalter = "3301",
                        aktiv = istLeitstelle,
                        stil = Schrift.MonoNormal,
                        weiterTaste = ImeAction.Next,
                        modifier = Modifier.weight(0.35f).beimVerlassen({ if (it) fokus = i else if (fokus == i) fokus = null }) { senden() },
                    )
                    Feld(
                        wert = zeile.name,
                        beiAenderung = { aendern(i, zeile.copy(name = it.take(40)), false) },
                        platzhalter = "Feuerwehr Landkreis …",
                        aktiv = istLeitstelle,
                        weiterTaste = ImeAction.Done,
                        modifier = Modifier.weight(0.65f).beimVerlassen({ if (it) fokus = i else if (fokus == i) fokus = null }) { senden() },
                    )
                }
                Pillenreihe {
                    organisationen.forEach { o ->
                        val an = o in zeile.organisationen
                        val doppelt = an && entwurf.indexOfFirst { o in it.organisationen } != i
                        Pille(
                            ORG_KURZ[o] ?: o,
                            an = an,
                            farbe = if (doppelt) Farben.TextSehrLeise else Farben.Amber,
                            beiDruck = {
                                aendern(i, zeile.copy(organisationen = if (an) zeile.organisationen - o else zeile.organisationen + o), true)
                            },
                            aktiv = istLeitstelle,
                        )
                    }
                    // Die Führung quer über alle — höchstens eine Gruppe trägt sie;
                    // der Klick nimmt sie der anderen weg.
                    Pille("Führung", an = zeile.fuehrung, beiDruck = {
                        val an = !zeile.fuehrung
                        entwurf = entwurf.mapIndexed { j, g -> g.copy(fuehrung = j == i && an) }
                        senden()
                    }, aktiv = istLeitstelle)
                }
                // Die Träger nur, wo der Kreis mehrere führt und die Zeile den
                // Rettungsdienst trägt.
                if (traeger.size > 1 && "Rettungsdienst" in zeile.organisationen) {
                    Pillenreihe {
                        traeger.forEach { h ->
                            val an = h in zeile.hiOrgs
                            Pille(RAUM_TRAEGER[h] ?: h, an = an, beiDruck = {
                                aendern(i, zeile.copy(hiOrgs = if (an) zeile.hiOrgs - h else zeile.hiOrgs + h), true)
                            }, aktiv = istLeitstelle)
                        }
                    }
                    if (zeile.hiOrgs.isEmpty()) SehrLeise("alle Träger")
                }
                if (organisationen.any { o -> o in zeile.organisationen && entwurf.indexOfFirst { o in it.organisationen } != i }) {
                    SehrLeise("Gedämpft: steht schon weiter oben — dort ist sie zu Hause.")
                }
            }
            Knopf("✕", {
                val naechste = entwurf.filterIndexed { j, _ -> j != i }
                entwurf = naechste
                senden(naechste)
            }, art = Knopfart.Leise, kompakt = true, aktiv = istLeitstelle)
        }
    }

    Pillenreihe {
        Knopf("+ Gruppe", {
            if (entwurf.size < MAX_GRUPPEN) entwurf = entwurf + Funkgruppenentwurf()
        }, art = Knopfart.Leise, kompakt = true, aktiv = istLeitstelle && entwurf.size < MAX_GRUPPEN)
        Knopf("Nach Organisation trennen", {
            // Die Nummern bleiben leer — sie sind Landessache, und eine erfundene
            // 3301 in Niedersachsen wäre falscher als gar keine.
            val kreis = raum.settings.landkreis ?: raum.settings.ort ?: ""
            val neu = organisationen.map { o ->
                val name = if (o == "Thw") "THW" else o
                Funkgruppenentwurf(name = if (kreis.isNotBlank()) "$name $kreis" else name, organisationen = listOf(o))
            }
            entwurf = neu
            senden(neu)
        }, art = Knopfart.Leise, kompakt = true, aktiv = istLeitstelle && organisationen.isNotEmpty())
        if (entwurf.isNotEmpty()) {
            Knopf("Ein Kanal für alle", {
                entwurf = emptyList()
                senden(emptyList())
            }, art = Knopfart.Leise, kompakt = true, aktiv = istLeitstelle)
        }
    }
}

// ============================================================ Wachen

/**
 * Welche Wachen des Kreises bespielt werden — und unter welcher Nummer sie funken.
 *
 * <b>Hier wird nur weggelassen und beschriftet.</b> Wachen verschieben, umwidmen
 * oder erfinden ist der Umbau des Ausrückebereichs; der bleibt dem Leitstellenbau.
 *
 * <b>Geschickt wird immer die ganze Liste</b> — und was dieser Kasten nicht zeigt
 * (Selbstgebautes, angeklickte Ortswehren im Sandkasten), geht unverändert mit.
 * Sonst risse der erste Haken die halbe Leitstelle ab (siehe `umbau` im Web).
 */
@Composable
fun ColumnScope.Wachenmaske(raum: Raumzustand, istLeitstelle: Boolean, neben: Raumneben, befehle: Raumbefehle) {
    val s = raum.settings
    var offen by remember { mutableStateOf(false) }
    val kreisId = s.landkreisId

    // Erst, wenn der Kasten aufgeht — und je Kreis nur einmal.
    LaunchedEffect(offen, kreisId) { if (offen && kreisId != null) befehle.kreiswachenLaden(kreisId) }
    val geladen = kreisId != null && neben.kreiswachenFuer == kreisId
    val bespielbar: List<Kreiswache> = if (geladen) neben.kreiswachen.filter { it.bespielt } else emptyList()

    val gewaehlt = s.wachen
    val dabei = gewaehlt.map { it.kennung }.toSet()
    val abgewaehlt: Set<String> =
        if (gewaehlt.isEmpty()) emptySet() else bespielbar.map { it.kennung }.filter { it !in dabei }.toSet()
    val bestand = gewaehlt.associateBy { it.kennung }
    val zurWahl = bespielbar.map { it.kennung }.toSet()
    val umbau = gewaehlt.filter { it.kennung !in zurWahl }

    fun eintrag(w: Kreiswache): Wachenwahl = bestand[w.kennung] ?: Wachenwahl(kennung = w.kennung)

    fun umschalten(w: Kreiswache) {
        if (!istLeitstelle) return
        val weg = abgewaehlt.toMutableSet()
        if (w.kennung in weg) {
            weg.remove(w.kennung)
        } else {
            // Die letzte Wache bleibt: Ein Kreis ohne Wache ist unspielbar.
            if (bespielbar.size - weg.size <= 1) return
            weg.add(w.kennung)
        }
        if (weg.isEmpty() && bestand.isEmpty()) {
            befehle.einstellungen(Einstellungsaenderung(wachen = emptyList()))
            return
        }
        val bleiben = bespielbar.filter { it.kennung !in weg }
        befehle.einstellungen(Einstellungsaenderung(wachen = umbau + bleiben.map(::eintrag)))
    }

    fun nummerSetzen(w: Kreiswache, wert: String) {
        val zahl = wert.trim().toIntOrNull()
        val neu = s.wachnummern.toMutableMap()
        if (zahl == null || zahl <= 0 || zahl == w.wachnummer) neu.remove(w.kennung) else neu[w.kennung] = zahl.coerceAtMost(999)
        if (neu != s.wachnummern) befehle.einstellungen(Einstellungsaenderung(wachnummern = neu))
    }

    Etikett(
        "Wachen — " + if (geladen && bespielbar.isNotEmpty()) {
            "${bespielbar.size - abgewaehlt.size} von ${bespielbar.size} besetzt"
        } else {
            "Kreiswachen"
        },
    )
    SehrLeise(
        "Welche Wachen des Kreises bespielt werden und unter welcher Nummer sie funken. Ohne Änderung " +
            "stehen auf allen Fahrzeuge, und die Nummer ist ihr Platz in der Reihe.",
    )
    Knopf(if (offen) "Wachen zuklappen" else "Wachen einstellen", { offen = !offen }, art = Knopfart.Leise, kompakt = true)
    if (!offen) return

    when {
        kreisId == null -> SehrLeise("Diese Runde spielt im erfundenen Standardbereich — dort gibt es keine Kreiswachen zu wählen.")
        !geladen -> SehrLeise("Wachen werden geladen …")
        bespielbar.isEmpty() -> SehrLeise("Für diesen Kreis liegen noch keine echten Wachen vor.")
        else -> {
            // Nach Organisation gruppiert — man sucht „die Feuerwachen", nicht
            // „Wache Nummer 12".
            bespielbar.groupBy { it.organisation }.forEach { (org, liste) ->
                Etikett(schluesselName(org))
                liste.forEach { w ->
                    val aus = w.kennung in abgewaehlt
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Hakenzeile(w.name, an = !aus, beiWechsel = { umschalten(w) }, aktiv = istLeitstelle, modifier = Modifier.weight(1f))
                        Wachnummerfeld(
                            nummer = s.wachnummern[w.kennung] ?: w.wachnummer,
                            aktiv = istLeitstelle && !aus,
                            beiSetzen = { nummerSetzen(w, it) },
                        )
                    }
                }
            }
            if (istLeitstelle && abgewaehlt.isNotEmpty()) {
                Knopf("Wieder alle besetzen", {
                    befehle.einstellungen(
                        Einstellungsaenderung(wachen = if (umbau.isNotEmpty()) umbau + bespielbar.map(::eintrag) else emptyList()),
                    )
                }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
}

/** Die Wachnummer als Zahlenfeld — gesendet beim Verlassen, wie im Web `@change`. */
@Composable
private fun Wachnummerfeld(nummer: Int, aktiv: Boolean, beiSetzen: (String) -> Unit) {
    var text by remember(nummer) { mutableStateOf(nummer.toString()) }
    Column(modifier = Modifier.width(76.dp)) {
        Feld(
            wert = text,
            beiAenderung = { text = it.filter(Char::isDigit).take(3) },
            aktiv = aktiv,
            tastatur = KeyboardType.Number,
            weiterTaste = ImeAction.Done,
            stil = Schrift.MonoNormal,
            modifier = Modifier.beimVerlassen({}) { if (text != nummer.toString()) beiSetzen(text) },
        )
    }
}
