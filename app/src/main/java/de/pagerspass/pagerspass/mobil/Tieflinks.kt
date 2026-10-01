package de.pagerspass.pagerspass.mobil

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Links von außen — `https://pagerspass.de/code/…`, `/funk/…`, `/raum/…` —, die
 * Android an die App gibt statt an den Browser.
 *
 * <b>Ein Briefkasten, kein Weg.</b> Die Activity legt die Adresse hier ab, so
 * wie sie kam; der Rahmen holt sie, sobald er handeln kann. Das ist der ganze
 * Grund für diesen Umweg: Ein Link kommt oft vor der Anmeldung an — aus einer
 * Mail, aus einem Video —, und dann gibt es noch kein Konto, keinen Shop und
 * keine Wache, an die er sich wenden könnte. Er wartet, bis es sie gibt.
 *
 * <b>Ein Objekt und kein ViewModel</b>, weil `onNewIntent` an der Activity
 * ankommt und nicht in der Oberfläche; eine Prozessgrenze überlebt der Eintrag
 * nicht, muss er aber auch nicht — Android liefert den Intent beim Neustart
 * erneut.
 */
object Tieflinks {

    private val _offen = MutableStateFlow<String?>(null)

    /** Die Adresse, die noch nicht erledigt ist — oder nichts. */
    val offen: StateFlow<String?> = _offen.asStateFlow()

    /** Eine Adresse annehmen. Was die App nicht versteht, wird gar nicht erst abgelegt. */
    fun annehmen(adresse: String?) {
        if (adresse != null && tiefzielAus(adresse) != null) _offen.value = adresse
    }

    /** Erledigt — nur, wenn inzwischen nicht schon die nächste angekommen ist. */
    fun erledigt(adresse: String) {
        _offen.compareAndSet(adresse, null)
    }
}

/** Was ein Link von der App will. */
sealed interface Tiefziel {
    /** Ein Creator- oder Aktionscode — `CodeView.vue`. */
    data class Code(val code: String) : Tiefziel

    /** Den Funkbegleiter koppeln — `BegleiterView.vue`. Die volle Adresse, damit der Server geprüft wird. */
    data class Funk(val adresse: String) : Tiefziel

    /** Einer Runde beitreten — `/raum/ABC123` oder der QR-Code der Lobby (`/?raum=ABC123`). */
    data class Raum(val code: String) : Tiefziel

    /** Einer Wache mit Beitrittscode beitreten — der QR-Code der Wache (`/gemeinschaften?code=…`). */
    data class Wache(val code: String) : Tiefziel

    /** Eine Seite, die die App selbst kennt — als Pfad des Web, siehe `appziel`. */
    data class Seite(val pfad: String) : Tiefziel
}

/** Die Wirte, deren Links die App annimmt — dieselben wie im Manifest. */
private val WIRTE = setOf("pagerspass.de", "www.pagerspass.de", "beta.pagerspass.de", "v6.pagerspass.de")

/**
 * Was eine Adresse meint — `null`, wenn sie nicht von uns ist oder die App mit
 * ihr nichts anfangen kann (dann bleibt sie dem Browser).
 *
 * Die Vorsätze `/play/mobile` und `/play/desktop` fallen weg: Dieselbe Seite
 * hat im Web drei Adressen, und jede davon kann geteilt worden sein.
 */
fun tiefzielAus(adresse: String): Tiefziel? {
    val roh = adresse.trim()
    if (!roh.startsWith("https://") && !roh.startsWith("http://")) return null
    val ohneSchema = roh.substringAfter("://")
    val wirt = ohneSchema.substringBefore('/').substringBefore('?').substringBefore('#').lowercase()
    if (wirt !in WIRTE) return null

    val rest = ohneSchema.removePrefix(ohneSchema.substringBefore('/').substringBefore('?'))
    val pfad = rest.substringBefore('?').substringBefore('#').trimEnd('/')
        .removePrefix("/play/mobile").removePrefix("/play/desktop")
    val anfrage = rest.substringAfter('?', "").substringBefore('#')
    fun parameter(name: String): String? = anfrage.split('&')
        .firstOrNull { it.startsWith("$name=") }
        ?.substringAfter('=')
        ?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    val teile = pfad.trim('/').split('/').filter { it.isNotEmpty() }
    return when {
        teile.size == 2 && teile[0] == "code" -> Tiefziel.Code(teile[1].uppercase())
        teile.size == 2 && teile[0] == "funk" -> tokenAus(roh)?.let { Tiefziel.Funk(roh) }
        teile.size == 2 && teile[0] == "raum" ->
            teile[1].takeIf { c -> c.all { it.isLetterOrDigit() } }?.let { Tiefziel.Raum(it.uppercase()) }
        teile.isEmpty() && parameter("raum") != null -> Tiefziel.Raum(parameter("raum")!!.uppercase())
        teile == listOf("gemeinschaften") && parameter("code") != null ->
            Tiefziel.Wache(parameter("code")!!.uppercase())
        // Alles mit Geld bleibt im Browser — dieselbe Regel wie bei den Startkacheln.
        appziel(pfad + (if (anfrage.isNotEmpty()) "?$anfrage" else "")) != null ->
            Tiefziel.Seite(pfad + (if (anfrage.isNotEmpty()) "?$anfrage" else ""))
        else -> null
    }
}
