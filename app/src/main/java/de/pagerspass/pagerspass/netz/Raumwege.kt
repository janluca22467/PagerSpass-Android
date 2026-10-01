package de.pagerspass.pagerspass.netz

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die REST-Wege, die im Raum gebraucht werden — übertragen aus `web/src/api/rest.ts`.
 *
 * <b>Warum neben dem Hub noch REST.</b> Was dem Konto gehört und die Schicht
 * überlebt (Rundenvorlagen, dauerhafte AAO), liegt nicht im Raum und kommt deshalb
 * auch nicht mit dem Raumzustand; was nur einmal nachgeschlagen wird (Wachen der
 * Runde, Stichwort-Sets), wäre im Raumzustand Ballast bei jedem Funkspruch.
 */
class Raumwege(private val netz: Netz) {

    /** Die Wachen, auf denen in dieser Runde Fahrzeuge stehen können. */
    suspend fun raumwachen(code: String): List<Raumwache> =
        netz.hole("/api/rooms/${teil(code)}/wachen")

    /**
     * Die echten Wachen des Kreises — für die Wachenmaske der Lobby. Nicht dasselbe
     * wie `raumwachen`: Das hier ist eine Auskunft über die Karte, jenes eine über
     * die Runde.
     */
    suspend fun kreiswachen(landkreisId: String): List<Kreiswache> =
        netz.hole("/api/landkreise/${teil(landkreisId)}/wachen")

    /**
     * Echte Straßennamen des Kreises — leer, solange für ihn keine OSM-Daten
     * vorliegen. Die Vorschläge im Adressfeld des neuen Einsatzes.
     */
    suspend fun strassen(landkreisId: String): List<String> =
        netz.hole("/api/landkreise/${teil(landkreisId)}/strassen")

    /** Die Stichwort-Sets, aus denen die Leitstelle wählen kann. */
    suspend fun stichwortsets(): List<Stichwortset> = netz.hole("/api/stichwortsets")

    // --------------------------------------------------------- Rundenvorlagen

    suspend fun rundenvorlagen(kennung: String): List<Rundenvorlagenzeile> =
        netz.hole("/api/rundenvorlagen?kennung=${teil(kennung)}")

    /**
     * Den Reglerstand dieser Lobby als Vorlage merken — mit `id` wird eine bestehende
     * überschrieben. Der Inhalt wird nicht mitgeschickt: Der Server liest ihn selbst
     * aus dem Raum.
     */
    suspend fun rundenvorlageSichern(
        kennung: String,
        id: String?,
        name: String,
        raumCode: String,
    ): Rundenvorlagenzeile = netz.hole(
        "/api/rundenvorlagen?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            if (id != null) put("id", id)
            put("name", name)
            put("raumCode", raumCode)
        }.toString(),
    )

    // ------------------------------------------------- Dauerhafte AAO am Konto

    /** Die Ordnungen, die in dieser Runde gelten — die allgemeinen und die des Kreises. */
    suspend fun aaoVorlagen(kennung: String, landkreisId: String?): List<AaoVorlagenzeile> {
        val kreis = landkreisId?.let { "&landkreisId=${teil(it)}" }.orEmpty()
        return netz.hole("/api/aao?kennung=${teil(kennung)}$kreis")
    }

    /**
     * Eine Ordnung dauerhaft sichern. `landkreisId = null` heißt: für alle Runden.
     *
     * <b>`stichwortAendern` und `lageAendern` stehen fest auf false.</b> Aus dem
     * Alarmfenster wird nur die Ordnung gesichert; ohne die beiden Schalter löschte
     * das Sichern den eigenen Verzeichniseintrag gleichen Namens (siehe `rest.ts`).
     */
    suspend fun aaoVorlageSichern(
        kennung: String,
        name: String,
        landkreisId: String?,
        empfohleneFahrzeuge: Int,
        empfohleneFaehigkeiten: List<String>,
    ): AaoSpeicherergebnis = netz.hole(
        "/api/aao?kennung=${teil(kennung)}",
        "POST",
        buildJsonObject {
            put("name", name)
            put("landkreisId", landkreisId?.let { JsonPrimitive(it) } ?: JsonNull)
            put("empfohleneFahrzeuge", empfohleneFahrzeuge)
            put(
                "empfohleneFaehigkeiten",
                buildJsonArray { empfohleneFaehigkeiten.forEach { add(JsonPrimitive(it)) } },
            )
            put("stichwortAendern", false)
            put("lageAendern", false)
        }.toString(),
    )

    suspend fun aaoVorlageLoeschen(kennung: String, id: String) =
        netz.ohneAntwort("/api/aao/${teil(id)}?kennung=${teil(kennung)}", "DELETE")

    /** PatSim: einfache oder erweiterte Übersicht der Maßnahmen (Konto → Spiel & Bedienung). */
    suspend fun massnahmenkatalog(kennung: String): Massnahmenkatalog =
        netz.hole("/api/konto/${teil(kennung)}/massnahmenkatalog")

    /**
     * Ob der Server die KI-Schalter (Funk, Anrufe, Lagen) anbietet — `kiFunk` in
     * `/api/version`. Ein älterer Server kennt das Feld nicht; dann heißt es „nein".
     */
    suspend fun kiFunkVerfuegbar(): Boolean =
        netz.hole<Serverangebot>("/api/version").kiFunk

    /**
     * Den Zugang für einen Handy-Funkbegleiter dieses Platzes erzeugen (Premium) —
     * `funkbegleiterErzeugen` im Web. Die App schickt keine Gerätewahl mit; der
     * Server lässt den Begleiter dann aufs Dienstgerät zurückfallen.
     */
    suspend fun funkbegleiterErzeugen(kennung: String, code: String): Funkbegleiterzugang =
        netz.hole(
            "/api/konto/${teil(kennung)}/raum/${teil(code)}/funkbegleiter",
            "POST",
            """{"funkAusgelagert":true}""",
        )

    /**
     * Einen Zugang für den Funkbegleiter erzeugen — `funkbegleiterErzeugen` in
     * `rest.ts`. Der Rumpf ist der Gerätestand, den das zweite Gerät spiegelt.
     */
    suspend fun funkbegleiter(kennung: String, code: String, geraete: Begleitergeraete): Begleiterzugang =
        netz.hole(
            "/api/konto/${teil(kennung)}/raum/${teil(code)}/funkbegleiter",
            "POST",
            buildJsonObject {
                put("bauform", geraete.bauform?.let { JsonPrimitive(it) } ?: JsonNull)
                put("bauart", geraete.bauart?.let { JsonPrimitive(it) } ?: JsonNull)
                put("funkgeraet", geraete.funkgeraet?.let { JsonPrimitive(it) } ?: JsonNull)
                put("begleiterFunkgeraet", geraete.begleiterFunkgeraet?.let { JsonPrimitive(it) } ?: JsonNull)
                put("melderton", geraete.melderton?.let { JsonPrimitive(it) } ?: JsonNull)
                put("gesicht", geraete.gesicht?.let { JsonPrimitive(it) } ?: JsonNull)
                put("zeigtMelder", true)
                put("zeigtFunkgeraet", true)
                put("zeigtFunkchat", true)
                put("funkAusgelagert", false)
            }.toString(),
        )
}

/** Der Zugang eines Funkbegleiters — der Token geht als Link an das Handy. */
@kotlinx.serialization.Serializable
data class Funkbegleiterzugang(val token: String = "", val gueltigBis: String? = null)

private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
