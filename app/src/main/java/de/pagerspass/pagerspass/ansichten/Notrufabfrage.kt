package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Anruf
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.NotrufabfrageKiAntwort
import de.pagerspass.pagerspass.netz.Stichwort
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Die strukturierte Notrufabfrage — das Menü neben dem Telefon. Übertragen aus
 * `components/leitstelle/NotrufAbfrage.vue`, `composables/notrufabfrage.ts` und
 * `utils/notrufabfrage.ts` (5.0.0.26).
 *
 * <b>Warum es sie gibt.</b> Am Apparat standen bisher sechs Fragenknöpfe und danach
 * der Vorschlag. Was dazwischen liegt — aus dem, was der Anrufer sagt, ein Stichwort
 * machen —, blieb Kopfarbeit, und wer neu am Platz sitzt, weiß nicht, wonach er als
 * Nächstes fragen soll. Die Abfrage führt in wenigen Schritten zu einem Stichwort
 * dieses Spiels.
 *
 * <b>Eingebunden ins Gespräch, nicht daneben gestellt.</b> Jeder Schritt hängt an
 * einer Telefonfrage: „Anrufer fragen" stellt sie wirklich, und Antworten, deren
 * Wörter der Anrufer schon gesagt hat, tragen die Marke „im Gespräch". Entschieden
 * wird trotzdem von Hand — eine Abfrage, die sich selbst ausfüllt, lehrt niemanden
 * das Abfragen.
 *
 * <b>Premium: die KI-Hilfe.</b> Ein Sprachmodell liest das Gespräch und schlägt für
 * den offenen Schritt eine Antwort vor (`GameHub.NotrufabfrageKi`). „Auto" lässt es
 * die Schritte nacheinander wählen, solange es sich sicher ist.
 *
 * <b>Das Ergebnis</b> geht beim Übernehmen des Vorschlags in den Einsatz: Stichwort,
 * Dringlichkeit, Meldebild (siehe `abfrageUebernehmen`).
 */

// ============================================================ Die Daten

@Serializable
data class Abfrageziel(
    /** Kandidaten aus dem Katalog — der erste vorhandene gewinnt. */
    val stichworte: List<String> = emptyList(),
    /** Die Bezeichnung, falls der Katalog keinen der Kandidaten kennt. */
    val text: String = "",
    val organisation: String = "Feuerwehr",
    val prioritaet: Int = 2,
    /** Wen die Lage außerdem braucht — ein Hinweis, alarmiert wird von Hand. */
    val mitalarm: List<String> = emptyList(),
    /** Was dem Anrufer bis zum Eintreffen gesagt wird. */
    val anweisungen: List<String> = emptyList(),
)

@Serializable
data class Abfrageantwort(
    val label: String = "",
    /** Die Kurzform fürs Meldebild. Fehlt sie, zählt die Antwort nicht als Befund. */
    val merkmal: String? = null,
    val weiter: String? = null,
    val ziel: Abfrageziel? = null,
    /** Hebt die Dringlichkeit an, egal wohin der Weg danach führt. */
    val prioritaet: Int? = null,
    val mitalarm: List<String> = emptyList(),
    val anweisungen: List<String> = emptyList(),
    /** Woran die Antwort im Gespräch zu erkennen ist — markiert nur, entscheidet nicht. */
    val woerter: List<String> = emptyList(),
)

@Serializable
data class Abfrageknoten(
    val frage: String = "",
    /** Die Frage am Telefon, die diesen Schritt klärt. */
    val fakt: String? = null,
    val hilfe: String? = null,
    val antworten: List<Abfrageantwort> = emptyList(),
)

@Serializable
data class Abfragezweig(
    val kuerzel: String = "",
    val titel: String = "",
    val untertitel: String = "",
    val start: String = "",
    val woerter: List<String> = emptyList(),
)

@Serializable
data class Notruffrage(val art: String = "", val text: String = "", val kurz: String = "")

@Serializable
data class Zusatzoption(
    val label: String = "",
    val merkmal: String? = null,
    val prioritaet: Int? = null,
    val mitalarm: List<String> = emptyList(),
    val anweisungen: List<String> = emptyList(),
)

/** Eine ergänzende Angabe — ändert das Stichwort nicht mehr, die Lage aber schon. */
@Serializable
data class Zusatzfrage(
    val id: String = "",
    val frage: String = "",
    val fakt: String? = null,
    val optionen: List<Zusatzoption> = emptyList(),
)

@Serializable
private data class Abfragebaumdaten(
    val zweige: List<Abfragezweig> = emptyList(),
    val fragen: List<Notruffrage> = emptyList(),
    val zusatz: Map<String, List<Zusatzfrage>> = emptyMap(),
    val knoten: Map<String, Abfrageknoten> = emptyMap(),
)

/** Der Baum — einmal gelesen, beim ersten Zugriff. */
internal object Abfragebaum {
    private val daten: Abfragebaumdaten by lazy {
        Netz.abgabe.decodeFromString(Abfragebaumdaten.serializer(), ABFRAGE_DATEN)
    }
    val zweige: List<Abfragezweig> get() = daten.zweige
    val knoten: Map<String, Abfrageknoten> get() = daten.knoten
    val fragen: List<Notruffrage> get() = daten.fragen

    fun zweig(bereich: String): Abfragezweig? = zweige.firstOrNull { it.kuerzel == bereich }

    /** Die ergänzenden Angaben für einen Bereich: erst die eigenen, dann die für alle. */
    fun zusatz(bereich: String): List<Zusatzfrage> = daten.zusatz[bereich].orEmpty()

    fun frage(art: String): Notruffrage? = fragen.firstOrNull { it.art == art }
}

/** Die Zielzeit vom Abheben bis zum Stichwort — die Marke, an der die Uhr umfärbt. */
private const val ZIELZEIT_SEKUNDEN = 60

/** Ab hier wählt „Auto" selbst; darunter bleibt es stehen und zeigt nur. */
private const val KI_SICHER = 0.7

// ============================================================ Der Stand

data class Abfrageschritt(val knoten: String, val antwort: Int)

/** Der Stand einer Abfrage — je Anruf, damit ein Reiterwechsel nichts verliert. */
data class Abfragestand(
    val bereich: String? = null,
    val weg: List<Abfrageschritt> = emptyList(),
    /** Ein im Ergebnis umgewähltes Stichwort — verfällt, sobald sich der Weg ändert. */
    val wahl: String? = null,
    /** Die ergänzenden Angaben: Frage-Id → gewählte Option. */
    val zusatz: Map<String, Int> = emptyMap(),
)

/**
 * Die Stände aller laufenden Abfragen. Außerhalb jeder Ansicht, weil die Abfrage
 * das Schließen der Blende überleben muss: Das Ergebnis wird erst beim Übernehmen
 * des Vorschlags gelesen, und bis dahin ist die Blende womöglich zweimal zu gewesen.
 */
internal val abfragestaende = mutableStateMapOf<String, Abfragestand>()

/** Was aus dem gegangenen Weg folgt — genau das nimmt der Einsatz mit. */
data class Abfrageauswertung(
    val bereich: String,
    val ziel: Abfrageziel,
    val prioritaet: Int,
    val mitalarm: List<String>,
    val merkmale: List<String>,
    val anweisungen: List<String>,
    val weg: List<Pair<String, String>>,
    val wahl: String? = null,
)

private fun antwortZu(s: Abfrageschritt): Abfrageantwort? = Abfragebaum.knoten[s.knoten]?.antworten?.getOrNull(s.antwort)

/** Der Knoten, an dem die Abfrage gerade steht — `null` am Anfang und am Ende. */
private fun knotenId(stand: Abfragestand): String? {
    val bereich = stand.bereich ?: return null
    val letzter = stand.weg.lastOrNull() ?: return Abfragebaum.zweig(bereich)?.start
    return antwortZu(letzter)?.weiter
}

/**
 * Fasst einen Weg zusammen, der an einem Ziel endet; `null`, solange er es nicht tut.
 * Die Dringlichkeit ist die höchste auf dem ganzen Weg: Wer „Personen eingeschlossen"
 * gewählt hat, hat eine Menschenrettung — auch wenn am Ende „ein Zimmer" steht.
 */
internal fun auswerten(stand: Abfragestand): Abfrageauswertung? {
    val bereich = stand.bereich ?: return null
    val ziel = stand.weg.lastOrNull()?.let(::antwortZu)?.ziel ?: return null
    var prioritaet = ziel.prioritaet
    val mitalarm = LinkedHashSet(ziel.mitalarm)
    val merkmale = mutableListOf<String>()
    val anweisungen = LinkedHashSet<String>()
    val protokoll = mutableListOf<Pair<String, String>>()

    stand.weg.forEach { s ->
        val k = Abfragebaum.knoten[s.knoten] ?: return@forEach
        val a = k.antworten.getOrNull(s.antwort) ?: return@forEach
        protokoll += k.frage to a.label
        a.merkmal?.let { if (it !in merkmale) merkmale += it }
        a.prioritaet?.let { if (it > prioritaet) prioritaet = it }
        mitalarm += a.mitalarm
        anweisungen += a.anweisungen
    }
    anweisungen += ziel.anweisungen

    // Die ergänzenden Angaben zählen wie Antworten — sie ändern nicht das Stichwort,
    // wohl aber die Lage.
    Abfragebaum.zusatz(bereich).forEach { z ->
        val o = stand.zusatz[z.id]?.let { z.optionen.getOrNull(it) } ?: return@forEach
        protokoll += z.frage to o.label
        o.merkmal?.let { if (it !in merkmale) merkmale += it }
        o.prioritaet?.let { if (it > prioritaet) prioritaet = it }
        mitalarm += o.mitalarm
        anweisungen += o.anweisungen
    }
    // Wer die Lage führt, steht nicht noch einmal unter „außerdem".
    mitalarm -= ziel.organisation

    return Abfrageauswertung(bereich, ziel, prioritaet, mitalarm.toList(), merkmale, anweisungen.toList(), protokoll, stand.wahl)
}

/** Die Auswertung zu einem Anruf — für den Einsatz beim Übernehmen. */
internal fun abfrageauswertung(anrufId: String?): Abfrageauswertung? =
    anrufId?.let { abfragestaende[it] }?.let(::auswerten)

/** Das Meldebild aus der Abfrage: Bezeichnung und Befunde in einer Zeile. */
internal fun meldebildAus(a: Abfrageauswertung): String {
    val befunde = a.merkmale.filter { it != a.ziel.text }
    return if (befunde.isEmpty()) a.ziel.text else "${a.ziel.text} — ${befunde.joinToString(", ")}"
}

/** Der erste Kandidat des Ziels, den der Katalog der Runde kennt. Eine Wahl geht vor. */
internal fun stichwortAusKatalog(ziel: Abfrageziel, katalog: List<Stichwort>, wahl: String? = null): Stichwort? {
    val kandidaten = if (wahl != null) listOf(wahl) + ziel.stichworte else ziel.stichworte
    kandidaten.forEach { k -> katalog.firstOrNull { it.stichwort == k }?.let { return it } }
    return null
}

/**
 * Stichworte, zwischen denen im Ergebnis umgewählt werden kann: die übrigen Kandidaten
 * und die Verwandten des Treffers — gleiche Organisation, gleiche Gruppe vor dem
 * Leerzeichen („B2 KELLER" → alles mit „B2").
 */
private fun aehnlicheStichworte(ziel: Abfrageziel, katalog: List<Stichwort>, aktuell: String?, hoechstens: Int = 8): List<Stichwort> {
    val gesehen = mutableSetOf<String>().apply { aktuell?.let { add(it) } }
    val liste = mutableListOf<Stichwort>()
    fun nehmen(s: Stichwort?) {
        if (s == null || s.stichwort in gesehen || liste.size >= hoechstens) return
        gesehen += s.stichwort
        liste += s
    }
    ziel.stichworte.forEach { k -> nehmen(katalog.firstOrNull { it.stichwort == k }) }
    val gruppe = (aktuell ?: ziel.stichworte.firstOrNull() ?: "").substringBefore(' ')
    if (gruppe.isNotBlank()) {
        katalog.filter { it.organisation == ziel.organisation && it.stichwort.substringBefore(' ') == gruppe }.forEach(::nehmen)
    }
    return liste
}

/** Welche Antworten der Anrufer schon angesprochen hat — nur seine eigenen Sätze. */
private fun imGespraech(woerter: List<String>, anruferText: String): Boolean =
    woerter.isNotEmpty() && anruferText.isNotBlank() && woerter.any { anruferText.contains(it) }

/** Der Index der *einen* Antwort, deren Wörter der Anrufer benutzt hat — sonst keiner. */
private fun gespraechsvorschlag(woerterJeAntwort: List<List<String>>, anruferText: String): Int? {
    val treffer = woerterJeAntwort.indices.filter { imGespraech(woerterJeAntwort[it], anruferText) }
    return treffer.singleOrNull()
}

/** Alle Ziele unterhalb eines Knotens — die Prognose im Lagebild. */
private fun erreichbareZiele(knotenId: String, gesehen: MutableSet<String> = mutableSetOf()): List<Abfrageziel> {
    if (!gesehen.add(knotenId)) return emptyList()
    val k = Abfragebaum.knoten[knotenId] ?: return emptyList()
    return k.antworten.flatMap { a -> a.ziel?.let { listOf(it) } ?: a.weiter?.let { erreichbareZiele(it, gesehen) }.orEmpty() }
}

// ============================================================ Die Ansicht

/**
 * Die Abfrage selbst — ein Arbeitsblatt: oben das Lagebild, das mit jeder Antwort
 * wächst, darunter die Pflichtfragen, der Ablauf und die eine offene Frage. Farbe
 * trägt nur, was Bedeutung hat: die Priorität, die Organisation, „im Gespräch" und
 * die KI.
 *
 * @param beiFrage Eine Telefonfrage stellen — dieselbe wie die Fragenknöpfe.
 * @param beiKi Die KI-Hilfe fragen; `null` heißt, sie ist für dieses Konto gesperrt.
 * @param beiAuflegen Am Ende: auflegen, damit der Vorschlag entsteht.
 */
@Composable
fun ColumnScope.Notrufabfrage(
    anruf: Anruf,
    katalog: List<Stichwort>,
    beiFrage: (String) -> Unit,
    beiOrten: () -> Unit,
    beiKi: (suspend (frage: String, antworten: List<String>) -> NotrufabfrageKiAntwort)?,
    beiAuflegen: () -> Unit,
) {
    val stand = abfragestaende[anruf.id] ?: Abfragestand()
    fun setzen(neu: Abfragestand) {
        abfragestaende[anruf.id] = neu
    }

    val bereich = stand.bereich
    val zweig = bereich?.let { Abfragebaum.zweig(it) }
    val kid = knotenId(stand)
    val knoten = kid?.let { Abfragebaum.knoten[it] }
    val auswertung = auswerten(stand)
    val anruferText = anruf.verlauf.filter { !it.vonLeitstelle }.joinToString(" ") { it.text }.lowercase()
    val treffer = auswertung?.let { stichwortAusKatalog(it.ziel, katalog, it.wahl) }
    val kuerzel = treffer?.stichwort ?: auswertung?.ziel?.stichworte?.firstOrNull().orEmpty()
    val stichwortText = treffer?.stichwortText ?: auswertung?.ziel?.text.orEmpty()

    fun antworten(index: Int) {
        val id = kid ?: return
        if (Abfragebaum.knoten[id]?.antworten?.getOrNull(index) == null) return
        setzen(stand.copy(weg = stand.weg + Abfrageschritt(id, index), wahl = null))
    }

    fun bereichWaehlen(neu: String) {
        setzen(stand.copy(bereich = neu, weg = emptyList(), wahl = null, zusatz = if (neu != stand.bereich) emptyMap() else stand.zusatz))
    }

    fun waehlen(index: Int) {
        if (bereich == null) Abfragebaum.zweige.getOrNull(index)?.let { bereichWaehlen(it.kuerzel) } else antworten(index)
    }

    // ------------------------------------------------------- Gesprächsuhr
    // Ab der ersten Zeile, also ab dem Abheben. Ist das Ergebnis da, bleibt sie stehen.
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            jetzt = System.currentTimeMillis()
            delay(1000)
        }
    }
    var fertigUm by remember(anruf.id) { mutableStateOf<Long?>(null) }
    if (auswertung != null && fertigUm == null) fertigUm = jetzt
    if (auswertung == null && fertigUm != null) fertigUm = null
    val beginn = zeitInMillis(anruf.verlauf.firstOrNull()?.zeit) ?: zeitInMillis(anruf.eingangUm) ?: jetzt
    val sekunden = (((fertigUm ?: jetzt) - beginn) / 1000).coerceAtLeast(0)
    val uhrText = "%d:%02d".format(sekunden / 60, sekunden % 60)
    val uhrUeber = sekunden > ZIELZEIT_SEKUNDEN

    // -------------------------------------------------------------- KI
    val bereichLauf = rememberCoroutineScope()
    var kiVorschlag by remember(anruf.id) { mutableStateOf<Pair<String, NotrufabfrageKiAntwort>?>(null) }
    var kiLaeuft by remember(anruf.id) { mutableStateOf(false) }
    var kiFehler by remember(anruf.id) { mutableStateOf<String?>(null) }
    var kiAuto by remember(anruf.id) { mutableStateOf(false) }
    var sperreOffen by remember { mutableStateOf(false) }
    val schluessel = if (bereich == null) "bereich" else kid ?: "ende"
    val aktuellerVorschlag = kiVorschlag?.takeIf { it.first == schluessel }?.second

    fun offenerSchritt(st: Abfragestand): Pair<String, List<String>>? {
        if (st.bereich == null) {
            return "Welcher Bereich trifft auf diesen Notruf zu?" to Abfragebaum.zweige.map { "${it.titel} (${it.untertitel})" }
        }
        val k = knotenId(st)?.let { Abfragebaum.knoten[it] } ?: return null
        return k.frage to k.antworten.map { it.label }
    }

    suspend fun kiFragen(): NotrufabfrageKiAntwort? {
        val ki = beiKi ?: run {
            sperreOffen = true
            return null
        }
        val st = abfragestaende[anruf.id] ?: Abfragestand()
        val (frage, liste) = offenerSchritt(st) ?: return null
        val fuer = if (st.bereich == null) "bereich" else knotenId(st) ?: "ende"
        kiLaeuft = true
        kiFehler = null
        try {
            val a = ki(frage, liste)
            val nun = abfragestaende[anruf.id] ?: Abfragestand()
            if ((if (nun.bereich == null) "bereich" else knotenId(nun) ?: "ende") != fuer) return null
            if (!a.ok || a.index == null) {
                kiFehler = a.fehler ?: "Keine Verbindung zum Server."
                return null
            }
            kiVorschlag = fuer to a
            return a
        } finally {
            kiLaeuft = false
        }
    }

    fun kiAutoStarten() {
        if (beiKi == null) {
            sperreOffen = true
            return
        }
        if (kiAuto) {
            kiAuto = false
            return
        }
        kiAuto = true
        bereichLauf.launch {
            try {
                // Höchstens acht Schritte — kein Baum ist tiefer.
                repeat(8) {
                    if (!kiAuto || auswerten(abfragestaende[anruf.id] ?: Abfragestand()) != null) return@launch
                    val v = kiFragen() ?: return@launch
                    if (!kiAuto || (v.sicherheit ?: 0.0) < KI_SICHER) return@launch
                    val st = abfragestaende[anruf.id] ?: Abfragestand()
                    val idx = v.index ?: return@launch
                    if (st.bereich == null) {
                        Abfragebaum.zweige.getOrNull(idx)?.let { z ->
                            abfragestaende[anruf.id] = st.copy(bereich = z.kuerzel, weg = emptyList(), wahl = null, zusatz = emptyMap())
                        }
                    } else {
                        val id = knotenId(st) ?: return@launch
                        abfragestaende[anruf.id] = st.copy(weg = st.weg + Abfrageschritt(id, idx), wahl = null)
                    }
                }
            } finally {
                kiAuto = false
            }
        }
    }

    // ------------------------------------------------------------ Kopf
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Etikett("Notrufabfrage")
        zweig?.let { Marke(it.kuerzel, farbe = bereichsfarbe(it.kuerzel)) }
        Box(Modifier.weight(1f))
        Text(
            uhrText,
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
            color = when {
                auswertung != null -> Farben.GruenHell
                uhrUeber -> Farben.AmberHell
                else -> Farben.TextLeise
            },
        )
        Knopf("←", {
            kiAuto = false
            setzen(if (stand.weg.isNotEmpty()) stand.copy(weg = stand.weg.dropLast(1), wahl = null) else stand.copy(bereich = null, wahl = null))
        }, art = Knopfart.Leise, aktiv = bereich != null, kompakt = true)
        Knopf("Neu", {
            kiAuto = false
            kiVorschlag = null
            setzen(Abfragestand())
        }, art = Knopfart.Leise, aktiv = bereich != null, kompakt = true)
    }

    // -------------------------------------------------------- Lagebild
    // Was der Weg bis hierher schon sagt: die Prognose („B2 · B3"), danach das
    // Stichwort. Priorität, Befunde und Mitalarm wachsen mit jeder Antwort.
    val lagePrio: Int
    val lageMerkmale: List<String>
    val lageMit: List<String>
    val lageTitel: String
    val lageUnter: String
    if (auswertung != null) {
        lagePrio = auswertung.prioritaet
        lageMerkmale = auswertung.merkmale
        lageMit = auswertung.mitalarm
        lageTitel = kuerzel
        lageUnter = stichwortText
    } else {
        var p = 1
        val m = mutableListOf<String>()
        val mit = LinkedHashSet<String>()
        stand.weg.forEach { s ->
            val a = antwortZu(s) ?: return@forEach
            a.merkmal?.let { if (it !in m) m += it }
            a.prioritaet?.let { if (it > p) p = it }
            mit += a.mitalarm
        }
        val ziele = (kid ?: zweig?.start)?.let { erreichbareZiele(it) }.orEmpty()
        val prognose = ziele.mapNotNull { it.stichworte.firstOrNull()?.substringBefore(' ')?.ifBlank { null } }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(4).map { it.key }
        lagePrio = p
        lageMerkmale = m
        lageMit = mit.toList()
        lageTitel = if (bereich != null) prognose.joinToString(" · ").ifBlank { "—" } else "Noch offen"
        lageUnter = if (bereich != null) {
            "${ziele.size} mögliche ${if (ziele.size == 1) "Lage" else "Lagen"} — weiter abfragen"
        } else {
            "Erst den Bereich wählen"
        }
    }
    val prioFarbe = when (lagePrio) {
        3 -> Farben.SignalHell
        2 -> Farben.AmberHell
        else -> Farben.GruenHell
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(prioFarbe.copy(alpha = 0.08f), Rundung.Klein)
            .border(1.dp, prioFarbe.copy(alpha = 0.4f), Rundung.Klein)
            .padding(Abstand.Normal),
    ) {
        Text("P$lagePrio", style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = prioFarbe)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text((if (auswertung != null) "STICHWORT" else "PROGNOSE"), style = Schrift.Etikett, color = Farben.TextSehrLeise)
            Text(lageTitel, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            SehrLeise(lageUnter)
            if (lageMerkmale.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    lageMerkmale.asReversed().take(5).forEach { Marke(it) }
                    if (lageMerkmale.size > 5) Marke("+${lageMerkmale.size - 5}")
                }
            }
            if (lageMit.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    Text("Mit", style = Schrift.Winzig, color = Farben.TextSehrLeise)
                    lageMit.forEach { o -> Marke(orgName(o), farbe = orgFarbe(o)) }
                }
            }
        }
    }

    // ---------------------------------------------------- Pflichtfragen
    // Auch hier, nicht nur unter dem Verlauf: Wer mitten in der Abfrage nach dem Ort
    // fragen will, muss nicht erst hinüber und wieder zurück.
    val geortet = anruf.ortung?.erfolgreich == true
    val ortBekannt = "Ort" in anruf.erfragt || geortet
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Abfragebaum.fragen.filter { it.art != "Ausmass" }.forEach { f ->
            val erledigt = f.art in anruf.erfragt || (f.art == "Ort" && geortet)
            Kachelknopf(
                text = f.kurz.removeSuffix("?"),
                punkt = when {
                    erledigt -> Farben.GruenHell
                    f.art == "Ort" -> Farben.AmberHell
                    else -> Farben.TextSehrLeise
                },
                an = erledigt,
            ) { beiFrage(f.art) }
        }
        Kachelknopf("✦ ${if (kiLaeuft && !kiAuto) "…" else "KI"}", punkt = null, an = false, farbe = Farben.ViolettHell, aktiv = !kiLaeuft && auswertung == null) {
            bereichLauf.launch { kiFragen() }
        }
        Kachelknopf(if (kiAuto) "■ Stopp" else "▶ Auto", punkt = null, an = kiAuto, farbe = Farben.ViolettHell, aktiv = auswertung == null || kiAuto) {
            kiAutoStarten()
        }
    }
    kiFehler?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
    if (sperreOffen) {
        PremiumHinweis { sperreOffen = false }
    }

    // ---------------------------------------------------------- Ablauf
    // Die Stationen: Bereich, jede Antwort, die offene Frage — ein Druck auf eine
    // Station geht dorthin zurück.
    if (bereich != null) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxWidth()) {
            Station("Bereich", zweig?.titel, fertig = true) { setzen(Abfragestand()) }
            stand.weg.forEachIndexed { i, s ->
                Station(Abfragebaum.knoten[s.knoten]?.frage.orEmpty(), antwortZu(s)?.label, fertig = true) {
                    setzen(stand.copy(weg = stand.weg.take(i), wahl = null))
                }
            }
            when {
                knoten != null -> Station(knoten.frage, null, fertig = false) {}
                auswertung != null -> Station("Stichwort", kuerzel, fertig = false, ziel = true) {}
            }
        }
    }

    if (auswertung == null && stand.weg.any { antwortZu(it)?.prioritaet == 3 }) {
        Text(
            "Dringend — Menschenleben in Gefahr. Kurz halten, Ort sichern, alarmieren.",
            style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
            color = Farben.SignalHell,
        )
    }

    // ----------------------------------------------------------- Bühne
    when {
        bereich == null -> {
            SehrLeise("Schritt 1", mono = true)
            Text("Worum geht es?", style = Schrift.Titel, color = Farben.Text)
            SehrLeise("Nach dem ersten Satz des Anrufers.")
            gespraechsvorschlag(Abfragebaum.zweige.map { it.woerter }, anruferText)?.let { i ->
                Vorschlagzeile(Abfragebaum.zweige[i].titel) { waehlen(i) }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                maxItemsInEachRow = 2,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Abfragebaum.zweige.forEachIndexed { i, z ->
                    Bereichskachel(
                        z = z,
                        nummer = i + 1,
                        genannt = imGespraech(z.woerter, anruferText),
                        ki = aktuellerVorschlag?.index == i,
                        modifier = Modifier.weight(1f),
                    ) {
                        kiAuto = false
                        bereichWaehlen(z.kuerzel)
                    }
                }
            }
        }

        knoten != null -> {
            SehrLeise("Schritt ${stand.weg.size + 1}", mono = true)
            Text(knoten.frage, style = Schrift.Titel, color = Farben.Text)
            knoten.hilfe?.let { Leise(it) }
            knoten.fakt?.let { fakt ->
                val gefragt = fakt in anruf.erfragt
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Ziel.Normal)
                        .background(Farben.BgTief, Rundung.Klein)
                        .border(1.dp, if (gefragt) Farben.GruenHell.copy(alpha = 0.5f) else Farben.RandHell, Rundung.Klein)
                        .clickable(onClick = { beiFrage(fakt) }, role = Role.Button, indication = null, interactionSource = null)
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Text("☎", style = Schrift.Normal, color = Farben.Amber)
                    Text("„${Abfragebaum.frage(fakt)?.text ?: fakt}“", style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                    Text(if (gefragt) "gefragt ✓" else "fragen", style = Schrift.Winzig, color = if (gefragt) Farben.GruenHell else Farben.AmberHell)
                }
            }
            gespraechsvorschlag(knoten.antworten.map { it.woerter }, anruferText)?.let { i ->
                Vorschlagzeile(knoten.antworten[i].label) { waehlen(i) }
            }
            knoten.antworten.forEachIndexed { i, a ->
                Antwortkachel(
                    a = a,
                    nummer = i + 1,
                    genannt = imGespraech(a.woerter, anruferText),
                    kiProzent = aktuellerVorschlag?.takeIf { it.index == i }?.sicherheit?.let { Math.round(it * 100).toInt() },
                ) {
                    kiAuto = false
                    antworten(i)
                }
            }
        }

        auswertung != null -> {
            if (!ortBekannt) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Farben.Amber.copy(alpha = 0.08f), Rundung.Klein)
                        .border(1.dp, Farben.Amber.copy(alpha = 0.5f), Rundung.Klein)
                        .padding(Abstand.Normal),
                ) {
                    Text("Der Einsatzort fehlt noch. Ohne ihn kommt kein Fahrzeug an.", style = Schrift.Klein, color = Farben.AmberHell)
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf("☎ Nach dem Ort fragen", { beiFrage("Ort") }, kompakt = true)
                        Knopf(
                            if (anruf.ortung?.laeuft == true) "Ortung läuft …" else "Orten",
                            beiOrten,
                            art = Knopfart.Leise,
                            aktiv = anruf.ortung == null,
                            kompakt = true,
                        )
                    }
                }
            }

            val org = auswertung.ziel.organisation
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(orgFarbe(org).copy(alpha = 0.08f), Rundung.Klein)
                    .border(1.dp, orgFarbe(org).copy(alpha = 0.5f), Rundung.Klein)
                    .padding(Abstand.Normal),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Text(kuerzel, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                    Marke(orgName(org), farbe = orgFarbe(org))
                }
                Text(stichwortText, style = Schrift.Normal, color = Farben.Text)
                SehrLeise(meldebildAus(auswertung))
                treffer?.let { t ->
                    SehrLeise(
                        "${t.empfohleneFahrzeuge} ${if (t.empfohleneFahrzeuge == 1) "Fahrzeug" else "Fahrzeuge"}" +
                            if (t.empfohleneFaehigkeiten.isNotEmpty()) " · ${t.empfohleneFaehigkeiten.joinToString(", ")}" else "",
                        mono = true,
                    )
                } ?: SehrLeise("Nicht im Katalog dieser Runde — das Stichwort wählst du beim Anlegen.")
            }

            // Ergänzende Angaben — sie ändern das Stichwort nicht, die Lage aber schon.
            val zusatz = Abfragebaum.zusatz(auswertung.bereich)
            Etikett("Ergänzende Angaben · ${zusatz.count { it.id in stand.zusatz }}/${zusatz.size}")
            zusatz.forEach { z ->
                Text(z.frage, style = Schrift.Klein, color = Farben.TextLeise)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    z.optionen.forEachIndexed { oi, o ->
                        val an = stand.zusatz[z.id] == oi
                        val ernst = an && (o.prioritaet == 3 || o.mitalarm.isNotEmpty())
                        de.pagerspass.pagerspass.ui.bausteine.Pille(
                            aufschrift = o.label,
                            an = an,
                            farbe = if (ernst) Farben.SignalHell else Farben.Amber,
                            beiDruck = {
                                val neu = stand.zusatz.toMutableMap()
                                if (neu[z.id] == oi) neu.remove(z.id) else neu[z.id] = oi
                                setzen(stand.copy(zusatz = neu))
                            },
                        )
                    }
                }
            }

            if (auswertung.anweisungen.isNotEmpty()) {
                Etikett("Dem Anrufer sagen")
                auswertung.anweisungen.forEachIndexed { i, a ->
                    Text("${i + 1}. $a", style = Schrift.Klein, color = Farben.Text)
                }
            }

            val alternativen = aehnlicheStichworte(auswertung.ziel, katalog, treffer?.stichwort)
            if (alternativen.isNotEmpty()) {
                Etikett("Passt ein anderes Stichwort besser?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    alternativen.forEach { a ->
                        de.pagerspass.pagerspass.ui.bausteine.Pille("${a.stichwort} · ${a.stichwortText}", an = false, beiDruck = { setzen(stand.copy(wahl = a.stichwort)) })
                    }
                    if (auswertung.wahl != null) {
                        de.pagerspass.pagerspass.ui.bausteine.Pille("↺ Vorschlag der Abfrage", an = false, beiDruck = { setzen(stand.copy(wahl = null)) })
                    }
                }
            }

            // Wie gut die Abfrage war — fürs Ergebnis, nicht für die Wertung der Schicht.
            // Der Ort zählt doppelt: Ohne ihn kommt kein Fahrzeug irgendwo an.
            val pflicht = listOf("Ort", "Was", "Betroffene", "Gefahren", "Anrufer")
            val fehlend = pflicht.filter { it !in anruf.erfragt && !(it == "Ort" && geortet) }
            val von = pflicht.size + 1
            val punkte = von - fehlend.size - if ("Ort" in fehlend) 1 else 0
            Etikett("Auswertung")
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
                Guete("$punkte/$von", "Pflichtfragen", punkte == von, Modifier.weight(1f))
                Guete(uhrText, "bis zum Stichwort", !uhrUeber, Modifier.weight(1f))
                Guete("${zusatz.count { it.id in stand.zusatz }}/${zusatz.size}", "Angaben", zusatz.all { it.id in stand.zusatz }, Modifier.weight(1f))
            }
            if (fehlend.isNotEmpty()) {
                SehrLeise("Noch nicht gefragt: ${fehlend.joinToString(", ") { Abfragebaum.frage(it)?.kurz?.removeSuffix("?") ?: it }}")
            }
            var protokollOffen by remember(anruf.id) { mutableStateOf(false) }
            Text(
                "${if (protokollOffen) "▾" else "▸"} Protokoll (${auswertung.weg.size} Einträge)",
                style = Schrift.Klein,
                color = Farben.TextLeise,
                modifier = Modifier
                    .defaultMinSize(minHeight = Ziel.Kompakt)
                    .clickable(onClick = { protokollOffen = !protokollOffen }, role = Role.Button, indication = null, interactionSource = null),
            )
            if (protokollOffen) {
                auswertung.weg.forEach { (frage, antwort) ->
                    Text("$frage — $antwort", style = Schrift.Winzig, color = Farben.TextLeise)
                }
            }

            Leise("Geht mit allen Angaben in den Einsatz — beim Anlegen änderbar.")
            Knopf("Auflegen und Einsatz anlegen", beiAuflegen, art = Knopfart.Haupt, breit = true)
        }
    }

    // Der KI-Vorschlag zum offenen Schritt.
    if (aktuellerVorschlag != null && auswertung == null) {
        val idx = aktuellerVorschlag.index ?: 0
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier
                .fillMaxWidth()
                .background(Farben.Violett.copy(alpha = 0.10f), Rundung.Klein)
                .border(1.dp, Farben.ViolettHell.copy(alpha = 0.5f), Rundung.Klein)
                .padding(Abstand.Normal),
        ) {
            Text("KI-Vorschlag: Antwort ${idx + 1}", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.ViolettHell)
            aktuellerVorschlag.begruendung?.let { Text(it, style = Schrift.Klein, color = Farben.Text) }
            aktuellerVorschlag.nachfragen?.let { SehrLeise("Nachfragen: „$it“") }
            Row { Knopf("Übernehmen", { waehlen(idx) }, kompakt = true) }
        }
    }
}

/**
 * Übernimmt die Auswertung in den Vorschlag des Servers: Stichwort aus dem Katalog,
 * die höhere Dringlichkeit, und die Befunde hinter dem Meldebild des Gesprächs — es
 * sind die Worte des Anrufers, die Befunde kommen dahinter, nicht an seine Stelle.
 */
internal fun abfrageUebernehmen(
    vorschlag: de.pagerspass.pagerspass.netz.Notrufvorschlag,
    a: Abfrageauswertung?,
    katalog: List<Stichwort>,
): de.pagerspass.pagerspass.netz.Notrufvorschlag {
    if (a == null) return vorschlag
    val vorlage = stichwortAusKatalog(a.ziel, katalog, a.wahl)
    val befund = meldebildAus(a)
    val bisher = vorschlag.meldebild.trim()
    return vorschlag.copy(
        stichwort = vorlage?.stichwort ?: a.ziel.stichworte.firstOrNull() ?: a.ziel.text,
        stichwortText = vorlage?.stichwortText ?: a.ziel.text,
        organisation = vorlage?.organisation ?: a.ziel.organisation,
        prioritaet = maxOf(vorlage?.prioritaet ?: 1, a.prioritaet),
        empfohleneFahrzeuge = vorlage?.empfohleneFahrzeuge ?: vorschlag.empfohleneFahrzeuge,
        empfohleneFaehigkeiten = vorlage?.empfohleneFaehigkeiten ?: vorschlag.empfohleneFaehigkeiten,
        meldebild = if (bisher.isNotEmpty() && !bisher.contains(befund)) "$bisher — Abfrage: $befund" else bisher.ifEmpty { befund },
    )
}

// ============================================================ Bausteine

private fun bereichsfarbe(kuerzel: String): Color = when (kuerzel) {
    "FEU" -> Farben.OrgFeuerwehr
    "TH" -> Farben.Blau
    "RD" -> Farben.OrgRettungsdienst
    "POL" -> Farben.OrgPolizei
    else -> Farben.Amber
}

private fun orgName(o: String): String = RAUM_ORGANISATIONEN.firstOrNull { it.first == o }?.second ?: o

@Composable
private fun Kachelknopf(
    text: String,
    punkt: Color?,
    an: Boolean,
    farbe: Color = Farben.Text,
    aktiv: Boolean = true,
    beiDruck: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .defaultMinSize(minHeight = Ziel.Kompakt)
            .background(if (an) farbe.copy(alpha = 0.12f) else Farben.BgTief, Rundung.Rund)
            .border(1.dp, if (an) farbe.copy(alpha = 0.6f) else Farben.Rand, Rundung.Rund)
            .clickable(enabled = aktiv, onClick = beiDruck, role = Role.Button, indication = null, interactionSource = null)
            .padding(horizontal = Abstand.Normal),
    ) {
        if (punkt != null) Box(Modifier.size(7.dp).background(punkt, CircleShape))
        Text(text, style = Schrift.Klein, color = if (aktiv) farbe else Farben.TextSehrLeise)
    }
}

@Composable
private fun Station(frage: String, antwort: String?, fertig: Boolean, ziel: Boolean = false, beiDruck: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = fertig, onClick = beiDruck, role = Role.Button, indication = null, interactionSource = null)
            .padding(vertical = 2.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    when {
                        ziel -> Farben.GruenHell
                        fertig -> Farben.TextSehrLeise
                        else -> Farben.Amber
                    },
                    CircleShape,
                ),
        )
        Text(frage, style = Schrift.Winzig, color = if (fertig) Farben.TextSehrLeise else Farben.Text, modifier = Modifier.weight(1f))
        if (antwort != null) Text(antwort, style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold), color = Farben.TextLeise)
    }
}

@Composable
private fun Vorschlagzeile(text: String, beiDruck: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ziel.Normal)
            .background(Farben.Amber.copy(alpha = 0.10f), Rundung.Klein)
            .border(1.dp, Farben.Amber.copy(alpha = 0.5f), Rundung.Klein)
            .clickable(onClick = beiDruck, role = Role.Button, indication = null, interactionSource = null)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Text("AUS DEM GESPRÄCH", style = Schrift.Etikett, color = Farben.AmberHell)
        Text(text, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
        Text("übernehmen", style = Schrift.Winzig, color = Farben.AmberHell)
    }
}

@Composable
private fun Bereichskachel(
    z: Abfragezweig,
    nummer: Int,
    genannt: Boolean,
    ki: Boolean,
    modifier: Modifier,
    beiDruck: () -> Unit,
) {
    val farbe = bereichsfarbe(z.kuerzel)
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 72.dp)
            .background(farbe.copy(alpha = if (genannt || ki) 0.16f else 0.07f), Rundung.Klein)
            .border(
                if (genannt || ki) 2.dp else 1.dp,
                when {
                    ki -> Farben.ViolettHell
                    genannt -> Farben.Amber
                    else -> farbe.copy(alpha = 0.5f)
                },
                Rundung.Klein,
            )
            .clickable(onClick = beiDruck, role = Role.Button, indication = null, interactionSource = null)
            .padding(Abstand.Normal),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text("$nummer", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
            Text(z.kuerzel, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = farbe)
        }
        Text(z.titel, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        Text(z.untertitel, style = Schrift.Winzig, color = Farben.TextSehrLeise)
    }
}

@Composable
private fun Antwortkachel(a: Abfrageantwort, nummer: Int, genannt: Boolean, kiProzent: Int?, beiDruck: () -> Unit) {
    val dringend = a.prioritaet == 3 || a.ziel?.prioritaet == 3
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ziel.Normal)
            .background(if (genannt) Farben.Amber.copy(alpha = 0.08f) else Farben.BgTief, Rundung.Klein)
            .border(
                1.dp,
                when {
                    kiProzent != null -> Farben.ViolettHell
                    genannt -> Farben.Amber
                    dringend -> Farben.SignalHell.copy(alpha = 0.6f)
                    else -> Farben.Rand
                },
                Rundung.Klein,
            )
            .clickable(onClick = beiDruck, role = Role.Button, indication = null, interactionSource = null)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Text("$nummer", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise, modifier = Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(a.label, style = Schrift.Normal, color = if (dringend) Farben.SignalHell else Farben.Text)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                a.ziel?.let { Text("→ ${it.stichworte.firstOrNull().orEmpty()}", style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise) }
                    ?: a.merkmal?.let { Text(it, style = Schrift.Winzig, color = Farben.TextSehrLeise) }
                if (genannt) Text("im Gespräch", style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
                if (kiProzent != null) Text("KI $kiProzent %", style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold), color = Farben.ViolettHell)
            }
        }
        Text(if (a.ziel != null) "✓" else "›", style = Schrift.Gross, color = Farben.TextSehrLeise)
    }
}

@Composable
private fun Guete(wert: String, wort: String, gut: Boolean, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Farben.BgTief, Rundung.Klein)
            .border(1.dp, if (gut) Farben.GruenHell.copy(alpha = 0.5f) else Farben.Rand, Rundung.Klein)
            .padding(Abstand.Klein),
    ) {
        Text(wert, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = if (gut) Farben.GruenHell else Farben.Text)
        Text(wort, style = Schrift.Winzig, color = Farben.TextSehrLeise)
    }
}

/**
 * Die KI-Hilfe gehört zu Premium. Kein Kauf in der App — der Weg dorthin ist die
 * Webseite (`/play/mobile/shop?bereich=premium`).
 */
@Composable
private fun PremiumHinweis(beiZu: () -> Unit) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val zusammenhang = androidx.compose.ui.platform.LocalContext.current
    val bereich = rememberCoroutineScope()
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.Amber.copy(alpha = 0.08f), Rundung.Klein)
            .border(1.dp, Farben.AmberHell.copy(alpha = 0.5f), Rundung.Klein)
            .padding(Abstand.Normal),
    ) {
        Text("★ KI-Hilfe für die Notrufabfrage", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.AmberHell)
        Leise(
            "Ein Sprachmodell liest das Gespräch mit und schlägt für jeden Schritt der Abfrage die " +
                "passende Antwort vor — mit Begründung und der nächsten Rückfrage. Du entscheidest — " +
                "nichts wird ohne dich alarmiert.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf("Premium im Web", {
                bereich.launch {
                    val server = de.pagerspass.pagerspass.netz.Ablage(zusammenhang).server()
                    runCatching { uri.openUri("$server/play/mobile/shop?bereich=premium") }
                }
            }, kompakt = true)
            Knopf("Schließen", beiZu, art = Knopfart.Leise, kompakt = true)
        }
    }
}
