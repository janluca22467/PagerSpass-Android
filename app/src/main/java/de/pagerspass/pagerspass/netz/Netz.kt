package de.pagerspass.pagerspass.netz

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Die Netzschicht — das Gegenstück zu `web/src/api/rest.ts`.
 *
 * <b>Ein Weg für alle Anfragen, und er kennt vier Sonderfälle</b>, genau wie die
 * Vorlage:
 *
 *  * **401** ist kein Fehler der Ansicht, sondern das Ende der Anmeldung. Das
 *    Merkmal wird weggeworfen und der Rahmen darüber unterrichtet — sonst
 *    stünde die App mit einem abgelaufenen Ausweis da und meldete auf jeder
 *    Seite einzeln „nicht berechtigt".
 *  * **503 mit `wartung`** ist eine Wartung, keine Störung. Der Server legt bei,
 *    worum es geht und wie lange es dauert.
 *  * **Leerer Rumpf bei 200.** ASP.NET Core schreibt für `Results.Ok(null)` gar
 *    nichts und bleibt trotzdem bei 200. Wer blind `Json.decode` aufruft,
 *    bekommt „Unexpected end of input" und der Aufrufer einen Fehler statt der
 *    Auskunft „nichts da" — bei der eigenen Wachengemeinschaft der Normalfall
 *    für jeden, der in keiner ist.
 *  * **Der Fehlerrumpf trägt einen Text.** `{ "fehler": "…" }` ist der Satz, den
 *    der Server für Menschen geschrieben hat. Ihn wegzuwerfen und „HTTP 400" zu
 *    zeigen, ist die häufigste Art, eine gute Fehlermeldung zu verlieren.
 *
 * <b>Warum `HttpURLConnection` und keine Bibliothek.</b> Die App stellt einfache
 * JSON-Anfragen gegen eine einzige API; Retrofit oder Ktor brächten einen
 * Abhängigkeitsbaum mit, dessen einziger Gewinn hier gesparte Zeilen wären. Was
 * an einer Bibliothek wirklich hängt — Verbindungswiederverwendung,
 * Zeitablauf, TLS — macht die Laufzeit von Android ohnehin selbst.
 */
class Netz(private val ablage: Ablage) {

    /**
     * Was die Netzschicht dem Rahmen zurückmeldet, ohne gefragt zu werden.
     *
     * Zwei Ereignisse, die keine Antwort auf eine Anfrage sind: Die Sitzung ist
     * abgelaufen, und der Server wird gewartet. Beide betreffen die ganze App
     * und nicht die Stelle, die gerade zufällig etwas geladen hat.
     */
    var beiAbmeldung: (() -> Unit)? = null
    var beiWartung: ((Wartungsstand) -> Unit)? = null

    /** Eine Anfrage, die eine Antwort hat. */
    suspend inline fun <reified T> hole(
        pfad: String,
        verfahren: String = "GET",
        rumpf: String? = null,
    ): T = abgabe.decodeFromString(roh(pfad, verfahren, rumpf) ?: "null")

    /** Eine Anfrage, deren Antwort niemanden interessiert. */
    suspend fun ohneAntwort(pfad: String, verfahren: String, rumpf: String? = null) {
        roh(pfad, verfahren, rumpf)
    }

    /**
     * Der eine Weg nach draußen.
     *
     * Gibt den Rumpf als Zeichenkette zurück oder `null`, wenn keiner kam —
     * siehe den Absatz über den leeren Rumpf oben.
     */
    suspend fun roh(pfad: String, verfahren: String = "GET", rumpf: String? = null): String? =
        withContext(Dispatchers.IO) {
            val adresse = URL("${ablage.server()}$pfad")
            val draht = (adresse.openConnection() as HttpURLConnection).apply {
                requestMethod = verfahren
                // Zehn Sekunden — dieselbe Größenordnung, nach der im Web der
                // Browser aufgibt. Länger heißt: Die App wirkt tot, und der
                // Nutzer tippt ein zweites Mal.
                connectTimeout = 10_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/json")
                if (rumpf != null) setRequestProperty("Content-Type", "application/json")

                // Das Merkmal weist die Anfrage aus. Es steht als `Bearer`, weil
                // die API es so erwartet (siehe `json()` in rest.ts).
                ablage.merkmal()?.let { setRequestProperty("Authorization", "Bearer $it") }

                if (rumpf != null) {
                    doOutput = true
                    outputStream.use { it.write(rumpf.toByteArray(Charsets.UTF_8)) }
                }
            }

            try {
                val stand = draht.responseCode
                val text = (if (stand in 200..299) draht.inputStream else draht.errorStream)
                    ?.bufferedReader()
                    ?.use(BufferedReader::readText)

                if (stand in 200..299) return@withContext text?.ifBlank { null }

                throw ausFehler(stand, text)
            } catch (fehler: Netzfehler) {
                throw fehler
            } catch (fehler: Exception) {
                Log.w("Netz", "$verfahren $pfad", fehler)
                // Die Meldung des Betriebssystems („failed to connect to
                // localhost/127.0.0.1:5473") sagt einem Spieler nichts. Der Satz
                // hier ist derselbe wie im Web.
                throw Netzfehler("Die Leitstelle antwortet nicht.", ursache = fehler)
            } finally {
                draht.disconnect()
            }
        }

    /**
     * Eine Datei hochladen — `multipart/form-data`, von Hand geschrieben.
     *
     * <b>Warum von Hand.</b> Java kennt kein `FormData`. Der Rumpf besteht aus
     * einer Trennmarke, einem Kopf je Teil und der Marke noch einmal am Ende mit
     * zwei Bindestrichen dahinter — das ist die ganze Form. Eine Bibliothek
     * dafür einzubinden hieße, für dreißig Zeilen einen Abhängigkeitsbaum zu
     * holen.
     *
     * <b>Die Trennmarke steht im Inhaltstyp und im Rumpf.</b> Genau daran
     * scheitert der übliche Versuch: Wer `Content-Type: multipart/form-data`
     * ohne `boundary` setzt, bekommt vom Server „es fehlt die Datei", obwohl sie
     * mitgeschickt wurde. Im Web ist es dasselbe Problem andersherum — dort darf
     * man den Inhaltstyp gerade *nicht* selbst setzen, weil `fetch` ihn samt
     * Marke schreibt.
     */
    suspend inline fun <reified T> hochladen(
        pfad: String,
        feld: String,
        dateiname: String,
        inhaltstyp: String,
        daten: ByteArray,
    ): T {
        val marke = "----pagerspass${System.nanoTime()}"
        val kopf = (
            "--$marke\r\n" +
                "Content-Disposition: form-data; name=\"$feld\"; filename=\"$dateiname\"\r\n" +
                "Content-Type: $inhaltstyp\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
        val fuss = "\r\n--$marke--\r\n".toByteArray(Charsets.UTF_8)

        val antwort = rohBinaer(pfad, "multipart/form-data; boundary=$marke", kopf + daten + fuss)
        return abgabe.decodeFromString(antwort ?: "null")
    }

    /**
     * Derselbe Weg wie `roh`, nur mit einem Rumpf aus Bytes statt aus Text.
     *
     * Er steht getrennt, weil `roh` seinen Inhaltstyp selbst setzt und den Rumpf
     * als Zeichenkette nimmt — bei einer Bilddatei wäre beides falsch.
     */
    suspend fun rohBinaer(pfad: String, inhaltstyp: String, daten: ByteArray): String? =
        withContext(Dispatchers.IO) {
            val draht = (URL("${ablage.server()}$pfad").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                // Ein Bild darf länger brauchen als eine Liste — es geht in die
                // andere Richtung, und dort ist die Leitung schmaler.
                readTimeout = 60_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", inhaltstyp)
                ablage.merkmal()?.let { setRequestProperty("Authorization", "Bearer $it") }
                doOutput = true
                setFixedLengthStreamingMode(daten.size)
                outputStream.use { it.write(daten) }
            }

            try {
                val stand = draht.responseCode
                val text = (if (stand in 200..299) draht.inputStream else draht.errorStream)
                    ?.bufferedReader()
                    ?.use(BufferedReader::readText)

                if (stand in 200..299) return@withContext text?.ifBlank { null }
                throw ausFehler(stand, text)
            } catch (fehler: Netzfehler) {
                throw fehler
            } catch (fehler: Exception) {
                Log.w("Netz", "POST $pfad", fehler)
                throw Netzfehler("Die Leitstelle antwortet nicht.", ursache = fehler)
            } finally {
                draht.disconnect()
            }
        }

    /** Aus einem Fehlerrumpf einen Fehler machen, den man anzeigen kann. */
    suspend fun ausFehler(stand: Int, text: String?): Netzfehler {
        val koerper = runCatching { text?.let { abgabe.parseToJsonElement(it).jsonObject } }.getOrNull()

        if (stand == 401) {
            ablage.anmeldungMerken(null, null)
            beiAbmeldung?.invoke()
        }

        if (stand == 503) {
            val wartung = runCatching {
                koerper?.get("wartung")?.let { abgabe.decodeFromJsonElement<Wartungsstand>(it) }
            }.getOrNull()
            if (wartung != null) beiWartung?.invoke(wartung)
        }

        val satz = runCatching { koerper?.get("fehler")?.jsonPrimitive?.content }.getOrNull()

        // World: die beiden Felder, an denen die Anzeige mehr tun muss als den Satz
        // zeigen — `auswahlNeu` (Fahrzeugauswahl neu holen) und `retryNach` (in wie
        // vielen Sekunden es wieder geht). Dasselbe wie `Antwortfehler` in rest.ts.
        val auswahlNeu = runCatching {
            koerper?.get("auswahlNeu")?.jsonPrimitive?.booleanOrNull
        }.getOrNull() == true
        val retryNach = runCatching {
            koerper?.get("retryNach")?.jsonPrimitive?.doubleOrNull?.toInt()
        }.getOrNull()

        return Netzfehler(
            satz ?: "Die Leitstelle antwortet nicht ($stand).",
            stand,
            auswahlNeu = auswahlNeu,
            retryNach = retryNach,
        )
    }

    // ------------------------------------------------------ World: PATCH
    //
    // `HttpURLConnection` kennt kein PATCH — `requestMethod = "PATCH"` wirft eine
    // `ProtocolException`. Der eine Weg der API, der es braucht (`PATCH
    // /api/welt/pois/{id}`), geht deshalb über OkHttp, das ohnehin für den Hub
    // mitkommt. Fehlerrumpf und Sonderfälle laufen durch dasselbe `ausFehler`.

    private val patchKlient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /** Eine PATCH-Anfrage mit JSON-Rumpf — gibt den Rumpf der Antwort zurück oder `null`. */
    suspend fun patch(pfad: String, rumpf: String): String? = withContext(Dispatchers.IO) {
        val merkmal = ablage.merkmal()
        val anfrage = okhttp3.Request.Builder()
            .url("${ablage.server()}$pfad")
            .header("Accept", "application/json")
            .patch(rumpf.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .apply { if (merkmal != null) header("Authorization", "Bearer $merkmal") }
            .build()

        try {
            patchKlient.newCall(anfrage).execute().use { antwort ->
                val text = antwort.body?.string()
                if (antwort.isSuccessful) return@withContext text?.ifBlank { null }
                throw ausFehler(antwort.code, text)
            }
        } catch (fehler: Netzfehler) {
            throw fehler
        } catch (fehler: Exception) {
            Log.w("Netz", "PATCH $pfad", fehler)
            throw Netzfehler("Die Leitstelle antwortet nicht.", ursache = fehler)
        }
    }

    companion object {
        /**
         * Der Umgang mit JSON.
         *
         * `ignoreUnknownKeys` ist hier keine Bequemlichkeit, sondern Pflicht: Die
         * API wächst schneller als die App, und ein Feld, das der Server neu
         * mitschickt, darf die Anmeldung nicht zum Absturz bringen.
         * `explicitNulls = false` lässt beim Senden weg, was `null` ist — der
         * Server unterscheidet an mehreren Stellen zwischen „nicht gesetzt" und
         * „ausdrücklich leer".
         */
        val abgabe = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
    }
}

/**
 * Ein Fehler, den man anzeigen darf.
 *
 * Seine Meldung ist der Satz des Servers, nicht der der Laufzeit. `stand` trägt
 * den HTTP-Code für die wenigen Stellen, die ihn unterscheiden müssen — 403 beim
 * Abo heißt „hier geht es in den Laden" und nicht „Fehler".
 */
class Netzfehler(
    meldung: String,
    val stand: Int = 0,
    ursache: Throwable? = null,
    /** World: Die Fahrzeugauswahl ist nicht mehr gültig und muss neu geholt werden. */
    val auswahlNeu: Boolean = false,
    /** World: In wie vielen Sekunden es wieder geht — `null`, wenn es keine Drosselung war. */
    val retryNach: Int? = null,
) : Exception(meldung, ursache)
