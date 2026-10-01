package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Kreiswache
import de.pagerspass.pagerspass.netz.ORG_NAME
import de.pagerspass.pagerspass.netz.Wachenwahl
import de.pagerspass.pagerspass.netz.istEigeneWache
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.karte.Kartenpunkt
import de.pagerspass.pagerspass.ui.karte.Punktkarte
import de.pagerspass.pagerspass.ui.karte.Punktkartenstand
import de.pagerspass.pagerspass.ui.theme.Abstand

/**
 * Die Karte im Leitstellenbau — die rechte Spalte von `LeitstellenbauView.vue`.
 *
 * <b>„Sind meine Wachen gut verteilt?" ist eine Frage an eine Karte</b>, nicht an
 * eine Liste: Die Einsätze entstehen im Umkreis der bespielten Wachen, und ob
 * zwischen zweien ein Loch klafft, sieht man nur dort. Die Liste darunter bleibt
 * als Rückfall — für die Suche nach Namen und ohne Kartenfreigabe.
 *
 * Drei Handgriffe, wie im Web, nur ohne Ziehen (auf dem Handy rollt ein
 * gezogener Finger die Seite):
 * - <b>Wählen</b>: Ein Tipp auf eine Wache des Kreises nimmt sie in die Runde
 *   oder heraus. Eine selbst gebaute lässt sich so nicht abwählen, nur in den
 *   Blick nehmen — abwählen hieße bei ihr „weg für immer", und ein Tipp auf die
 *   Karte ist kein Weg, auf dem man etwas für immer verliert.
 * - <b>Verschieben</b>: Die Wache im Blick zieht an den Ort des nächsten Tipps
 *   (`dragend` im Web). Nur, was mitspielt — eine ungewählte zu verschieben
 *   wäre eine Einstellung ohne Wirkung.
 * - <b>Bauen</b>: Der Tipp stellt eine eigene Wache an diese Stelle.
 */
@Composable
fun Leitstellenbaukarte(
    abzug: List<Kreiswache>,
    wachen: List<Wachenwahl>,
    voll: Boolean,
    beiUmschalten: (Kreiswache) -> Unit,
    beiVerschieben: (kennung: String, lat: Double, lon: Double) -> Unit,
    beiBauen: (lat: Double, lon: Double) -> Unit,
) {
    var modus by remember { mutableStateOf("waehlen") }
    var imBlick by remember { mutableStateOf<String?>(null) }
    val gewaehlt = wachen.associateBy { it.kennung }
    val eigene = wachen.filter { istEigeneWache(it.kennung) }

    val stand = remember { Punktkartenstand(52.6, 10.1, 10.0) }

    fun ort(w: Wachenwahl, a: Kreiswache?): Pair<Double, Double>? {
        val lat = w.lat ?: a?.lat ?: return null
        val lon = w.lon ?: a?.lon ?: return null
        return lat to lon
    }

    val punkte = abzug.map { a ->
        val wahl = gewaehlt[a.kennung]
        val (lat, lon) = wahl?.let { ort(it, a) } ?: (a.lat to a.lon)
        Kartenpunkt(
            id = a.kennung,
            lat = lat,
            lon = lon,
            farbe = orgFarbe(wahl?.organisation ?: a.organisation),
            beschriftung = a.name,
            gewaehlt = wahl != null,
            hervorgehoben = imBlick == a.kennung,
        )
    } + eigene.mapNotNull { w ->
        val (lat, lon) = ort(w, null) ?: return@mapNotNull null
        Kartenpunkt(
            id = w.kennung,
            lat = lat,
            lon = lon,
            farbe = orgFarbe(w.organisation ?: "Feuerwehr"),
            beschriftung = "★ " + (w.name?.ifBlank { null } ?: "Eigene Wache"),
            gewaehlt = true,
            hervorgehoben = imBlick == w.kennung,
        )
    }

    // Einmal den Kreis ins Bild, sobald Größe und Wachen da sind (`karteAusrichten`).
    var ausgerichtet by remember { mutableStateOf(false) }
    LaunchedEffect(stand.groesse, punkte.isNotEmpty()) {
        if (ausgerichtet || stand.groesse == IntSize.Zero || punkte.isEmpty()) return@LaunchedEffect
        stand.zuschneiden(punkte.map { it.lat to it.lon }, deckel = 13.0)
        ausgerichtet = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Segment(
            seiten = listOf("waehlen", "verschieben", "bauen"),
            gewaehlt = modus,
            beiWahl = { modus = it },
            aufschrift = { when (it) { "waehlen" -> "Wählen"; "verschieben" -> "Verschieben"; else -> "Bauen" } },
            modifier = Modifier.fillMaxWidth(),
        )
        SehrLeise(
            when (modus) {
                "waehlen" -> "Tipp auf eine Wache nimmt sie in die Runde oder heraus. Gefüllt heißt: spielt mit."
                "verschieben" -> imBlick?.let { k ->
                    val name = abzug.firstOrNull { it.kennung == k }?.name ?: eigene.firstOrNull { it.kennung == k }?.name
                    "„${name?.ifBlank { null } ?: "Eigene Wache"}\" zieht an den Ort des nächsten Tipps."
                } ?: "Erst im Modus „Wählen\" eine bespielte Wache antippen — dann hier den neuen Ort."
                else -> if (voll) "Mehr Wachen gehen nicht." else "Tipp auf die Karte stellt eine eigene Wache an diese Stelle."
            },
        )
        Punktkarte(
            punkte = punkte,
            stand = stand,
            modifier = Modifier.fillMaxWidth().height(320.dp),
            beiPunkt = { kennung ->
                when (modus) {
                    "waehlen" -> {
                        imBlick = kennung
                        abzug.firstOrNull { it.kennung == kennung }?.let(beiUmschalten)
                    }
                    else -> imBlick = kennung
                }
            },
            beiOrt = { lat, lon ->
                val r5 = { x: Double -> Math.round(x * 100_000.0) / 100_000.0 }
                when (modus) {
                    "verschieben" -> imBlick?.takeIf { it in gewaehlt }?.let { beiVerschieben(it, r5(lat), r5(lon)) }
                    "bauen" -> if (!voll) beiBauen(r5(lat), r5(lon))
                    else -> imBlick = null
                }
            },
        )
        imBlick?.let { k ->
            val a = abzug.firstOrNull { it.kennung == k }
            val w = gewaehlt[k]
            val name = a?.name ?: w?.name?.ifBlank { null } ?: "Eigene Wache"
            SehrLeise(
                "Im Blick: $name · " + (ORG_NAME[w?.organisation ?: a?.organisation] ?: (w?.organisation ?: a?.organisation ?: "")) +
                    if (w == null) " · spielt nicht mit" else " · Zug ${w.zugnummer}",
                mono = true,
            )
            if (a != null && w != null && (w.lat != null || w.lon != null)) {
                Knopf("An den Platz aus dem Abzug", {
                    beiVerschieben(k, Double.NaN, Double.NaN)
                }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
}
