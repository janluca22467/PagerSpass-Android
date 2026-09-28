package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Die Wege, die der Leitstellentisch neben dem Hub braucht — übertragen aus
 * `web/src/api/rest.ts`.
 *
 * <b>Drei Dinge, und keines davon ist Spielzustand.</b> Die eigenen Alarm- und
 * Ausrückeordnungen hängen am Konto und überdauern die Schicht; der Zugang des
 * Funkbegleiters ist ein kurzlebiger Schlüssel; die Event-Einsätze sind die
 * Kulisse, aus der eine Lage stammen kann. Alles andere kommt mit dem
 * Raumzustand.
 */
class Leitstellenwege(private val netz: Netz) {

    // ---------------------------------------------- Alarm- und Ausrückeordnung

    /**
     * Die Ordnungen, die in dieser Runde gelten — die allgemeinen und die des
     * gefahrenen Kreises (`GET /api/aao?kennung[&landkreisId]`).
     */
    suspend fun aaoVorlagen(kennung: String, landkreisId: String?): List<AaoVorlagenzeile> {
        val kreis = landkreisId?.let { "&landkreisId=${teil(it)}" }.orEmpty()
        return netz.hole("/api/aao?kennung=${teil(kennung)}$kreis")
    }

    /** Der ganze Bestand samt Zähler und Grenze — für die Verwaltung. */
    suspend fun aaoBestand(kennung: String): AaoBestand =
        netz.hole("/api/aao/alle?kennung=${teil(kennung)}")

    /**
     * Eine Ordnung dauerhaft sichern (`POST /api/aao`). Die Antwort interessiert
     * nur im Fehlerfall — dann wirft `Netz` mit dem Satz des Servers („Höchstens
     * 80 gespeicherte Ordnungen").
     */
    suspend fun aaoVorlageSichern(kennung: String, vorlage: Aaosicherung) {
        netz.ohneAntwort(
            "/api/aao?kennung=${teil(kennung)}",
            "POST",
            buildJsonObject {
                put("name", vorlage.name)
                put("landkreisId", vorlage.landkreisId?.let { JsonPrimitive(it) } ?: JsonNull)
                put("empfohleneFahrzeuge", vorlage.empfohleneFahrzeuge)
                put(
                    "empfohleneFaehigkeiten",
                    buildJsonArray { vorlage.empfohleneFaehigkeiten.forEach { add(JsonPrimitive(it)) } },
                )
                put("stichwort", vorlage.stichwort?.let { JsonPrimitive(it) } ?: JsonNull)
                put("stichwortText", vorlage.stichwortText?.let { JsonPrimitive(it) } ?: JsonNull)
                put("meldebild", vorlage.meldebild?.let { JsonPrimitive(it) } ?: JsonNull)
                put("adresse", vorlage.adresse?.let { JsonPrimitive(it) } ?: JsonNull)
                put("stichwortAendern", vorlage.stichwortAendern)
                put("lageAendern", vorlage.lageAendern)
            }.toString(),
        )
    }

    suspend fun aaoVorlageLoeschen(kennung: String, id: String) =
        netz.ohneAntwort("/api/aao/${teil(id)}?kennung=${teil(kennung)}", "DELETE")

    // ------------------------------------------------------- Funkbegleiter

    /**
     * Den kurzlebigen QR-Zugang zum Funkbegleiter dieses Platzes erzeugen. Der
     * Gerätestand geht mit — das Handy zeigt dann denselben Melder.
     */
    suspend fun funkbegleiterErzeugen(
        kennung: String,
        code: String,
        geraete: Begleitergeraete,
    ): Begleiterzugang = netz.hole(
        "/api/konto/${teil(kennung)}/raum/${teil(code)}/funkbegleiter",
        "POST",
        Netz.abgabe.encodeToString(Begleitergeraete.serializer(), geraete),
    )

    // ------------------------------------------------------------ Events

    /** Die Event-Einsätze des Spiels — einmal je Sitzung reicht. */
    suspend fun spielevents(): List<Spielevent> = netz.hole("/api/events")
}

private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")

/**
 * Eine dauerhaft gespeicherte Alarm- und Ausrückeordnung — Spiegel von
 * `AaoVorlagenzeile` in `rest.ts`.
 *
 * `landkreisId` `null` heißt „für alle Runden". Ein `stichwort` macht aus ihr
 * zugleich ein eigenes Stichwort im Einsatzbogen.
 */
@Serializable
data class AaoVorlagenzeile(
    val id: String = "",
    val name: String = "",
    val landkreisId: String? = null,
    val empfohleneFahrzeuge: Int = 0,
    val empfohleneFaehigkeiten: List<String> = emptyList(),
    val geaendertUm: String = "",
    val stichwort: String? = null,
    val stichwortText: String? = null,
    val meldebild: String? = null,
    val adresse: String? = null,
)

/** Der ganze Bestand samt Zähler und Grenze — die Grundlage der Verwaltung. */
@Serializable
data class AaoBestand(
    val vorlagen: List<AaoVorlagenzeile> = emptyList(),
    val anzahl: Int = 0,
    val grenze: Int = 0,
    val premiumGrenze: Int = 0,
    val premium: Boolean = false,
)

/** Was beim Sichern einer Ordnung mitgeht — die Felder von `aaoVorlageSichern`. */
data class Aaosicherung(
    val name: String,
    val landkreisId: String?,
    val empfohleneFahrzeuge: Int,
    val empfohleneFaehigkeiten: List<String>,
    val stichwort: String? = null,
    val stichwortText: String? = null,
    val meldebild: String? = null,
    val adresse: String? = null,
    /** Ob dieser Aufruf über Stichwort und Lage überhaupt entschieden hat — der Bogen ja, das Alarmfenster nein. */
    val stichwortAendern: Boolean = false,
    val lageAendern: Boolean = false,
)

/** Der Zugang zum Funkbegleiter — acht Stunden gültig. */
@Serializable
data class Begleiterzugang(
    val token: String = "",
    val gueltigBis: String = "",
)

/**
 * Ein Event-Einsatz des Spiels — Spiegel von `SpielEvent` in `types.ts`.
 *
 * Er ist die Kulisse, in der eine Lage steht: Titel, Zeitfenster, Bild.
 */
@Serializable
data class Spielevent(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String? = null,
    val bannerUrl: String? = null,
    val hintergrundUrl: String? = null,
    val farbe: String? = null,
    val beginntUm: String = "",
    val endetUm: String = "",
    /** `Geplant`, `Laeuft`, `Beendet`. */
    val zustand: String = "",
    val stichwort: String = "",
    val stichwortText: String = "",
)

/**
 * Ein Eintrag im Ortsverzeichnis des Katalogs — `Ort` in `types.ts`. Hier
 * `Katalogort`, weil `Ort` schon der Kartenpunkt der Runde ist.
 */
@Serializable
data class Katalogort(
    val strasse: String = "",
    val ortsteil: String = "",
    val objekt: String? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)
