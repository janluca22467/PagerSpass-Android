package de.pagerspass.pagerspass.ansichten

import de.pagerspass.pagerspass.mobil.Geraeteeinstellungen
import de.pagerspass.pagerspass.netz.Rundenfahrzeug

/**
 * Funktexte in der eingestellten Fahrzeugkennung — `funkname` und `funkanzeige` aus
 * `composables/fahrzeugkennung.ts` (Web v6).
 *
 * <b>Nur für die Anzeige.</b> Gesagt und gesendet wird weiter der volle Funkrufname;
 * wer aber „Typ" als Kennung gewählt hat, liest im Protokoll „Florian Celle
 * 1/HLF 20-1" statt „Florian Celle 1/44/1" — dieselbe Form wie auf dem Tableau.
 */

/** Der Funkrufname, wie er in der eingestellten Form heißt. */
internal fun funkname(f: Rundenfahrzeug, form: String = Geraeteeinstellungen.kennungsform.value): String {
    if (form == "kennzahl") return f.funkrufname
    val eigen = Geraeteeinstellungen.kennung(f.kurzname.ifBlank { f.funkrufname }, f.typ, f.organisation, null, form)
    // Bei „Typ" bleibt der Rufnamenvorsatz stehen; die Kennzahl darin ist das Einzige,
    // was die Form ersetzt. Bei „Organisation" steht die Orga-Kennung allein.
    if (form == "typ" && f.kurzname.isNotBlank() && f.funkrufname.contains(f.kurzname)) {
        return f.funkrufname.replace(f.kurzname, eigen)
    }
    return eigen
}

private class Funkersatz(val fahrzeuge: List<Rundenfahrzeug>, val form: String, val muster: Regex?, val ersatz: Map<String, String>)

@Volatile private var funkcache: Funkersatz? = null

/**
 * Ein Funktext mit den Fahrzeugnamen in der eingestellten Kennung. Ersetzt werden der
 * volle Funkrufname und die nackte Kurzform („1/44/1"), der längste Treffer zuerst;
 * die Grenzen verhindern, dass „1/44/1" mitten in „11/44/1" gefunden wird.
 */
internal fun funkanzeige(text: String?, fahrzeuge: List<Rundenfahrzeug>): String {
    if (text.isNullOrEmpty()) return text.orEmpty()
    val form = Geraeteeinstellungen.kennungsform.value
    if (form == "kennzahl" || fahrzeuge.isEmpty()) return text

    val cache = funkcache?.takeIf { it.fahrzeuge === fahrzeuge && it.form == form } ?: run {
        val ersatz = LinkedHashMap<String, String>()
        fahrzeuge.forEach { f ->
            val neu = funkname(f, form)
            if (f.funkrufname.isNotBlank() && neu != f.funkrufname) ersatz.putIfAbsent(f.funkrufname.lowercase(), neu)
            val kurz = Geraeteeinstellungen.kennung(f.kurzname, f.typ, f.organisation, null, form)
            if (f.kurzname.isNotBlank() && kurz != f.kurzname) ersatz.putIfAbsent(f.kurzname.lowercase(), kurz)
        }
        val namen = ersatz.keys.sortedByDescending { it.length }.map(Regex::escape)
        val muster = if (namen.isEmpty()) null else Regex(
            "(?<![\\p{L}\\p{N}/])(?:${namen.joinToString("|")})(?![\\p{N}/])",
            setOf(RegexOption.IGNORE_CASE),
        )
        Funkersatz(fahrzeuge, form, muster, ersatz).also { funkcache = it }
    }
    val muster = cache.muster ?: return text
    return muster.replace(text) { t -> cache.ersatz[t.value.lowercase()] ?: t.value }
}

/**
 * Ob die freie Rede für diesen Platz gerade zu ist — ohne Lehrgang „Sprechfunk" in
 * einer Runde mit einem zweiten Menschen (`funkGesperrt` im Web). Entscheiden tut der
 * Server; hier steht nur der Hinweis, bevor jemand tippt.
 */
internal fun funkGesperrt(raum: de.pagerspass.pagerspass.netz.Raumzustand, eigeneKennung: String): Boolean {
    val selbst = raum.players.firstOrNull { it.id == eigeneKennung } ?: return false
    if (selbst.funkschein != false) return false
    return raum.players.any { !it.istBot && it.id != selbst.id }
}
