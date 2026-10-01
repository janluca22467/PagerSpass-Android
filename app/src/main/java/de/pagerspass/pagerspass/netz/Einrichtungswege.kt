package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege und Formen für das, was beim Start eines Kontos gefragt wird — die
 * Altersfrage, der Einrichtungsbogen und die Übersicht der Maßnahmen in PatSim.
 * Übertragen aus `web/src/api/rest.ts` (v6).
 *
 * <b>Ein älterer Server kennt sie nicht.</b> `pagerspass.de` läuft unter Umständen
 * noch auf einem Stand ohne diese Pfade; dann antworten sie mit 404. Der
 * `Einrichtungsdienst` fängt das ab und tut so, als gäbe es nichts zu fragen —
 * ein Dialog, der wegen eines fehlenden Wegs den Start blockiert, wäre schlimmer
 * als ein Bogen, der erst mit dem nächsten Server kommt.
 */
class Einrichtungswege(private val netz: Netz) {

    // ---------------------------------------------------------- Altersfrage

    /** Wo das Konto bei der Altersfrage steht, samt Link zum Bogen der Eltern. */
    suspend fun alter(kennung: String): Altersfreigabe =
        netz.hole("/api/konto/${teil(kennung)}/alter")

    /** „18 oder älter" oder „unter 18" — gilt einmal; eine zweite Antwort ändert nichts. */
    suspend fun alterAngeben(kennung: String, volljaehrig: Boolean): Altersfreigabe = netz.hole(
        "/api/konto/${teil(kennung)}/alter",
        "POST",
        buildJsonObject { put("volljaehrig", volljaehrig) }.toString(),
    )

    // ---------------------------------------------------- Einrichtungsbogen

    suspend fun einrichtung(kennung: String): Einrichtung =
        netz.hole("/api/konto/${teil(kennung)}/einrichtung")

    /** Hält die Antworten fest und hakt den Bogen ab. */
    suspend fun einrichtungSpeichern(
        kennung: String,
        anrufeAnnehmen: Boolean,
        spielrolle: String?,
        vorkenntnisse: String?,
        patientensimulation: Boolean?,
    ): Einrichtung = netz.hole(
        "/api/konto/${teil(kennung)}/einrichtung",
        "PUT",
        buildJsonObject {
            put("anrufeAnnehmen", anrufeAnnehmen)
            put("spielrolle", spielrolle)
            put("vorkenntnisse", vorkenntnisse)
            put("patientensimulation", patientensimulation)
        }.toString(),
    )

    // -------------------------------------------------- Maßnahmen in PatSim

    /** Einfach (nur die vorgeschlagenen) oder erweitert (alle). Am Konto, auf jedem Gerät gleich. */
    suspend fun massnahmenkatalog(kennung: String): Massnahmenkatalog =
        netz.hole("/api/konto/${teil(kennung)}/massnahmenkatalog")

    suspend fun massnahmenkatalogSetzen(kennung: String, alle: Boolean): Massnahmenkatalog = netz.hole(
        "/api/konto/${teil(kennung)}/massnahmenkatalog",
        "PUT",
        buildJsonObject { put("alle", alle) }.toString(),
    )
}

/**
 * Die Altersfrage — `Altersfreigabe` in `types.ts`.
 *
 * @param stand `Offen`, `WartetAufEltern` oder `Freigegeben` (der Server kennt
 *   vielleicht weitere; alles außer den beiden ersten heißt „nichts zu fragen").
 * @param elternlink Der Bogen auf forms.pagerspass.de — nur, solange das Konto wartet.
 */
@Serializable
data class Altersfreigabe(
    val stand: String = "Freigegeben",
    val volljaehrig: Boolean? = null,
    val elternlink: String? = null,
)

/** Der Einrichtungsbogen am Konto — `Einrichtung` in `types.ts`. */
@Serializable
data class Einrichtung(
    /** Ob der Bogen noch beantwortet werden will. */
    val offen: Boolean = false,
    val erledigtUm: String? = null,
    val anrufeAnnehmen: Boolean = false,
    /** `Leitstelle`, `Fahrzeug` oder `Beides`. */
    val spielrolle: String? = null,
    /** `Neu`, `Etwas` oder `Erfahren`. */
    val vorkenntnisse: String? = null,
    val patientensimulation: Boolean = true,
)

@Serializable
data class Massnahmenkatalog(val alle: Boolean = false)

private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
