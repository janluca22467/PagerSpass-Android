package de.pagerspass.pagerspass.mobil

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Brettkommentar
import de.pagerspass.pagerspass.netz.Bretteintrag
import de.pagerspass.pagerspass.netz.Einladung
import de.pagerspass.pagerspass.netz.Freundewege
import de.pagerspass.pagerspass.netz.Funkverbindung
import de.pagerspass.pagerspass.netz.Gemeinschaftsnachricht
import de.pagerspass.pagerspass.netz.Geschenkinhalt
import de.pagerspass.pagerspass.netz.Mitteilungseinstellungen
import de.pagerspass.pagerspass.netz.Nachricht
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Spielwege
import de.pagerspass.pagerspass.netz.wert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Die Sozialschicht — Direktnachrichten, Brett-Ereignisse, Wachenchat, und der
 * Stand des Freundebereichs. Das Gegenstück zu `web/src/api/sozial.ts` samt den
 * Speichern `freunde.ts` und `brett.ts`.
 *
 * <b>Ein eigener Hub, mit Absicht getrennt vom Spiel.</b> `/hub/sozial` trägt
 * alles, was nicht zur laufenden Runde gehört; reißt diese Leitung ab, fährt
 * die Schicht ungestört weiter — dieselbe Trennung wie im Web.
 *
 * <b>Der Handschlag ist zweistufig:</b> erst die WebSocket-Verbindung, dann
 * genau ein `Anmelden` ohne Argumente — die Kennung zieht der Server aus dem
 * Merkmal. Erst danach kommen Ereignisse. <b>Und nach jedem Wiederverbinden
 * noch einmal:</b> Der Kontext auf dem Server ist dann neu, und ohne erneutes
 * `Anmelden` käme kein einziges Ereignis mehr an. Was während der Funkstille
 * geschah, wird danach nachgeholt (`nachholen`) — der Hub kennt kein „was habe
 * ich verpasst".
 *
 * <b>Die Leitung hängt am Konto.</b> Beim Abmelden oder Kontowechsel wird sie
 * gekappt und der Stand weggeworfen — sonst hingen Freundesliste und Verlauf
 * des vorigen Kontos im Speicher eines anderen Menschen am selben Gerät.
 */
class Sozial(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val netz = Netz(ablage)
    private val wege = Spielwege(netz)
    private val freundewege = Freundewege(netz)

    private val _stand = MutableStateFlow(Sozialstand())
    val stand: StateFlow<Sozialstand> = _stand.asStateFlow()

    /** Die Haken für die Nachbarn: Wer neu laden muss, erfährt es hier. */
    var beiFreundesliste: (() -> Unit)? = null
    var beiEinladungen: (() -> Unit)? = null
    var beiGemeinschaft: (() -> Unit)? = null
    var beiBrett: (() -> Unit)? = null

    /** Eine Nachricht der Verwaltung kam live — die Sitzung holt ihre Blende. */
    var beiAdminNachricht: (() -> Unit)? = null

    /** Wie eine Wache heißt — für die Überschrift der Mitteilung aus dem Wachenchat. */
    var wachenname: (String) -> String? = { null }

    @Volatile
    private var kennung: String = ""

    /** Ob die nächste stehende Leitung die erste ist — danach heißt Verbunden: nachholen. */
    @Volatile
    private var ersteVerbindung = true

    /** Ob die App gerade zu sehen ist — nur dahinter gibt es Systemmeldungen. */
    @Volatile
    private var imVordergrund = true

    /** Die Mitteilungsschalter des Kontos, beim Wechsel in den Hintergrund frisch geholt. */
    @Volatile
    private var schalter: Mitteilungseinstellungen? = null

    /** Welche Nachrichten schon gemeldet wurden — eine beantwortete Terminzeile kommt ein zweites Mal. */
    private val gemeldet = java.util.Collections.synchronizedSet(mutableSetOf<Long>())

    private fun eigeneKennung(): String? = kennung.ifBlank { null }

    /** Freundesliste, Einladungen, Vorschläge, Suche, fremde Profile. */
    val kreis = Freundeskreis(
        bereich = viewModelScope,
        wege = freundewege,
        spielwege = wege,
        kennung = ::eigeneKennung,
        meldung = ::meldungSetzen,
        ablage = anwendung.getSharedPreferences("sozial", Context.MODE_PRIVATE),
    )

    /** Das Brett mit seinen drei Kreisen und dem offenen Eintrag. */
    val brett = Brettstapel(
        bereich = viewModelScope,
        wege = freundewege,
        spielwege = wege,
        kennung = ::eigeneKennung,
        meldung = ::meldungSetzen,
    )

    /**
     * Die Leitung — und zwar eine neue je Konto.
     *
     * <b>Nicht dieselbe wiederverwenden.</b> Eine geschlossene Verbindung meldet
     * ihr Ende erst später; hinge sie an demselben Objekt wie die neue, räumte ihr
     * Nachzügler die neue gleich wieder ab. Mit einem Objekt je Anlauf redet die
     * alte nur noch mit sich selbst — die Empfänger prüfen, ob sie noch gilt.
     */
    @Volatile
    private var draht: Funkverbindung = neuerDraht()

    private val netzbeobachter = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Netz zurück — wie `online` im Web.
            wiederaufnehmen()
        }
    }

    init {
        brett.beiZeile = { nr, abbildung -> kreis.profilzeileAendern(nr, abbildung) }
        brett.beiEntfernt = { nr -> kreis.profilzeileEntfernen(nr) }

        runCatching {
            anwendung.getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(netzbeobachter)
        }
    }

    override fun onCleared() {
        runCatching {
            getApplication<Application>().getSystemService(ConnectivityManager::class.java)
                ?.unregisterNetworkCallback(netzbeobachter)
        }
        draht.trennen()
        super.onCleared()
    }

    // ------------------------------------------------------------- Leitung

    private fun neuerDraht(): Funkverbindung {
        val d = Funkverbindung(ablage, pfad = "/hub/sozial")
        d.beiLage = { lage -> if (d === draht) lageGeaendert(lage) }
        handlerSetzen(d)
        return d
    }

    /**
     * Die Leitung steht (wieder) — anmelden, und nach einer Funkstille nachholen.
     *
     * Dasselbe für den ersten Aufbau und für das Wiederverbinden der
     * `Funkverbindung`: Beide enden in `Verbunden`, und beide brauchen das eine
     * `Anmelden`. Nur beim ersten gibt es nichts nachzuholen — den Stand holt
     * `verbinden` ohnehin.
     */
    private fun lageGeaendert(lage: Funkverbindung.Lage) {
        _stand.update { it.copy(lage = lage) }
        if (lage != Funkverbindung.Lage.Verbunden || kennung.isBlank()) return

        val d = draht
        viewModelScope.launch {
            val antwort = runCatching { d.frage("Anmelden") }.getOrNull()
            if (d !== draht) return@launch

            // Ein Nein heißt: kein gültiges Merkmal. Dann keine Leitung stehen
            // lassen, die verbunden aussieht, aber nichts zugestellt bekommt.
            if ((antwort as? JsonPrimitive)?.booleanOrNull == false) {
                d.trennen()
                return@launch
            }

            // `GemeinschaftBetreten` gehört nach dem Anmelden dazu, wenn ein
            // Wachenchat mitgelesen wird — `Anmelden` betritt nur die eigene.
            _stand.value.wachenchatId?.let { id -> d.rufen("GemeinschaftBetreten", wert(id)) }

            if (ersteVerbindung) {
                ersteVerbindung = false
            } else {
                nachholen()
            }
        }
    }

    /**
     * Verbinden und anmelden — idempotent, mehrfach rufen ist erlaubt.
     *
     * Mit einer anderen Kennung als bisher ist es ein Kontowechsel: erst alles
     * Alte weg, dann neu.
     */
    fun verbinden(eigeneKennung: String) = viewModelScope.launch {
        if (eigeneKennung.isBlank()) return@launch
        if (kennung.isNotBlank() && kennung != eigeneKennung) abbauen()
        kennung = eigeneKennung

        kreis.standHolen()
        leitungAufbauen()
    }

    /** Auflegen und vergessen — beim Abmelden. */
    fun trennen() = abbauen()

    /**
     * Die aufgegebene Leitung zurückholen — nach Bildschirm-an oder Netz-zurück.
     *
     * Das Wiederverbinden der `Funkverbindung` gibt nach gut einer Minute auf;
     * wer danach auf einer Seite blieb, dessen Chat, Einladungen und Marken
     * standen still. Steht die Leitung noch, tut das hier nichts.
     */
    fun wiederaufnehmen() {
        if (kennung.isBlank()) return
        if (draht.lage != Funkverbindung.Lage.Getrennt) return
        viewModelScope.launch { leitungAufbauen() }
    }

    /**
     * Die App ist zu sehen — oder nicht mehr.
     *
     * Beim Zurückkommen wird die Leitung geprüft; beim Gehen holen wir die
     * Mitteilungsschalter frisch, damit eine Systemmeldung sie beachtet, auch
     * wenn sie gerade eben umgelegt wurden.
     */
    fun vordergrund(sichtbar: Boolean) {
        imVordergrund = sichtbar
        if (sichtbar) {
            wiederaufnehmen()
            // Was im offenen Gespräch ankam, während die App weglag, ist jetzt gelesen.
            _stand.value.gespraechMit?.let { mit ->
                if (draht.lage == Funkverbindung.Lage.Verbunden) draht.rufen("Gelesen", wert(mit))
            }
        } else {
            val k = eigeneKennung() ?: return
            viewModelScope.launch {
                runCatching { wege.mitteilungseinstellungen(k) }.onSuccess { schalter = it }
            }
        }
    }

    private suspend fun leitungAufbauen() {
        val d = draht
        if (d.lage != Funkverbindung.Lage.Getrennt) return
        // Scheitert der Aufbau, versucht es die Funkverbindung von selbst weiter;
        // steht sie danach, meldet `lageGeaendert` an. Ohne Kanal bleibt der per
        // REST geladene Stand trotzdem lesbar.
        runCatching { d.verbinden() }
    }

    private fun abbauen() {
        val alt = draht
        kennung = ""
        ersteVerbindung = true
        schalter = null
        gemeldet.clear()
        draht = neuerDraht()
        alt.trennen()
        _stand.value = Sozialstand()
        kreis.leeren()
        brett.leeren()
    }

    /**
     * Nach einer Funkstille alles nachholen, was keine Ereignisse bekommen hat:
     * Listen, Brett, offenes Gespräch, mitgelesener Wachenchat.
     */
    private fun nachholen() {
        kreis.standHolen()
        beiFreundesliste?.invoke()
        beiEinladungen?.invoke()
        beiGemeinschaft?.invoke()
        brett.nachholen()
        // `gespraechOeffnen` mischt, statt zu leeren, und quittiert als gelesen.
        _stand.value.gespraechMit?.let { gespraechOeffnen(it) }
        _stand.value.wachenchatId?.let { id ->
            val k = eigeneKennung() ?: return@let
            viewModelScope.launch {
                runCatching { wege.gemeinschaftChat(k, id) }.onSuccess { zeilen ->
                    _stand.update {
                        if (it.wachenchatId != id) return@update it
                        val neu = zeilen.map { z -> z.nr }.toSet()
                        it.copy(
                            wachenchat = (it.wachenchat.filter { z -> z.nr !in neu } + zeilen)
                                .sortedBy { z -> z.nr }
                                .takeLast(200),
                        )
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------ Ereignisse

    private fun <T> lesen(serializer: KSerializer<T>, element: JsonElement?): T? =
        element?.let { runCatching { Netz.abgabe.decodeFromJsonElement(serializer, it) }.getOrNull() }

    private fun handlerSetzen(d: Funkverbindung) {
        // Nur die geltende Leitung spricht — der Nachzügler einer alten schweigt.
        fun auf(ereignis: String, empfang: (List<JsonElement>) -> Unit) =
            d.auf(ereignis) { argumente -> if (d === draht) empfang(argumente) }

        // Eine Direktnachricht — an Empfänger UND Absender (Mehrgerät). Eine
        // beantwortete Terminzeile kommt als Ersatz derselben `nr`.
        auf("Nachricht") { argumente ->
            val nachricht = lesen(Nachricht.serializer(), argumente.firstOrNull()) ?: return@auf
            val partner = if (nachricht.von == kennung) nachricht.an else nachricht.von

            var imOffenen = false
            _stand.update { alt ->
                if (alt.gespraechMit == null || alt.gespraechMit != partner) return@update alt
                imOffenen = true
                val ohne = alt.verlauf.filter { it.nr != nachricht.nr }
                alt.copy(verlauf = (ohne + nachricht).sortedBy { it.nr })
            }

            // Im offenen Gespräch gilt Ankommen als Lesen — wie im Web. Aber nur,
            // solange die App zu sehen ist: Wer sie weggelegt hat, liest nicht.
            if (imOffenen && nachricht.von != kennung && imVordergrund) {
                d.rufen("Gelesen", wert(partner))
            }

            kreis.laden()
            beiFreundesliste?.invoke()
            nachrichtMelden(nachricht)
        }

        // Der Partner hat gelesen — alle eigenen Zeilen an ihn bekommen den Haken.
        auf("NachrichtenGelesen") { argumente ->
            val von = (argumente.getOrNull(0) as? JsonPrimitive)?.contentOrNull ?: return@auf
            val um = (argumente.getOrNull(1) as? JsonPrimitive)?.contentOrNull ?: ""
            _stand.update { alt ->
                if (alt.gespraechMit != von) return@update alt
                alt.copy(
                    verlauf = alt.verlauf.map {
                        if (it.an == von && it.gelesenUm == null) it.copy(gelesenUm = um) else it
                    },
                )
            }
        }

        auf("Freundesliste") {
            kreis.laden()
            beiFreundesliste?.invoke()
        }

        auf("Einladung") { argumente ->
            lesen(Einladung.serializer(), argumente.firstOrNull())?.let {
                kreis.einladungEin(it)
                einladungMelden(it)
            }
            beiEinladungen?.invoke()
        }

        auf("Einladungen") {
            viewModelScope.launch { kreis.einladungenAktualisieren() }
            beiEinladungen?.invoke()
        }

        auf("BrettEintrag") { argumente ->
            lesen(Bretteintrag.serializer(), argumente.firstOrNull())?.let { brett.eintragEin(it) }
            beiBrett?.invoke()
        }

        auf("BrettQuittung") { argumente ->
            val nr = (argumente.getOrNull(0) as? JsonPrimitive)?.longOrNull ?: return@auf
            val anzahl = (argumente.getOrNull(1) as? JsonPrimitive)?.intOrNull ?: return@auf
            brett.quittungEin(nr, anzahl)
        }

        auf("BrettKommentar") { argumente ->
            val kommentar = lesen(Brettkommentar.serializer(), argumente.firstOrNull()) ?: return@auf
            brett.kommentarEin(kommentar)
            kommentarMelden(kommentar)
        }

        // Eine Zeile im Wachenchat — neu oder ersetzt (Entfernen sendet
        // dasselbe Ereignis noch einmal, mit leerem Text).
        auf("Gemeinschaftsnachricht") { argumente ->
            val zeile = lesen(Gemeinschaftsnachricht.serializer(), argumente.firstOrNull())
                ?: return@auf

            var imOffenen = false
            _stand.update { alt ->
                if (alt.wachenchatId != zeile.gemeinschaftId) return@update alt
                imOffenen = true
                val ohne = alt.wachenchat.filter { it.nr != zeile.nr }
                alt.copy(wachenchat = (ohne + zeile).sortedBy { it.nr }.takeLast(200))
            }

            if (imOffenen && imVordergrund) {
                d.rufen("GemeinschaftGelesen", wert(zeile.gemeinschaftId))
            } else {
                // Nicht offen (oder weggelegt): Die Marke an der Leiste muss
                // hochzählen, und die zählt die Gemeinschaft selbst.
                beiGemeinschaft?.invoke()
            }
            chatzeileMelden(zeile)
        }

        auf("Gemeinschaften") { beiGemeinschaft?.invoke() }
        auf("Gemeinschaft") { beiGemeinschaft?.invoke() }
        auf("Gemeinschaftsantrag") { beiGemeinschaft?.invoke() }
        auf("Gemeinschaftsaufloesung") { argumente ->
            val name = (argumente.getOrNull(1) as? JsonPrimitive)?.contentOrNull
            _stand.update {
                it.copy(
                    meldung = "„${name ?: "Deine Wache"}“ wurde aufgelöst.",
                    wachenchat = emptyList(),
                    wachenchatId = null,
                )
            }
            beiGemeinschaft?.invoke()
        }

        // Eine gezielte Nachricht der Verwaltung — die Blende holt die Sitzung.
        auf("AdminNachricht") { beiAdminNachricht?.invoke() }
    }

    // ---------------------------------------------------------- Mitteilungen

    private fun nachrichtMelden(nachricht: Nachricht) {
        if (imVordergrund || nachricht.von == kennung || nachricht.gelesenUm != null) return
        if (schalter?.privatnachrichten == false) return
        if (!gemeldet.add(nachricht.nr)) return

        val name = kreis.stand.value.freund(nachricht.von)?.anzeigename?.ifBlank { null }
            ?: "einem Freund"
        val text = when {
            nachricht.geschenk != null -> "🎁 Ein Geschenk für dich"
            nachricht.terminText != null -> "📅 ${nachricht.terminText}"
            else -> nachricht.text
        }
        Sozialmitteilung.zeigen(
            getApplication<Application>(),
            schluessel = "nachricht:${nachricht.von}",
            titel = "Nachricht von $name",
            text = gekuerzt(text),
            weg = "${FreundeWeg.GESPRAECH}/${nachricht.von}",
        )
    }

    /**
     * Ein Kommentar unter einem eigenen Eintrag — die Mitteilung, die das Web als
     * Push schickt. Wem der Eintrag gehört, weiß der Stapel oft schon; sonst
     * fragt ein Abruf nach.
     */
    private fun kommentarMelden(kommentar: Brettkommentar) {
        if (imVordergrund || kommentar.vonMir || kommentar.verfasser.kennung == kennung) return
        if (schalter?.brett == false) return
        val k = eigeneKennung() ?: return

        viewModelScope.launch {
            val eigener = brett.istEigener(kommentar.eintragNr)
                ?: runCatching { wege.bretteintrag(k, kommentar.eintragNr).vonMir }.getOrDefault(false)
            if (!eigener) return@launch

            val von = kommentar.verfasser.anzeigename.ifBlank { "Jemand" }
            Sozialmitteilung.zeigen(
                getApplication<Application>(),
                schluessel = "kommentar:${kommentar.eintragNr}",
                titel = "$von hat kommentiert",
                text = gekuerzt(kommentar.text),
                weg = "${FreundeWeg.EINTRAG}/${kommentar.eintragNr}",
            )
        }
    }

    /**
     * Eine Rundeneinladung — der Abruf fände sie erst in einer Viertelstunde, und
     * dann ist die Runde womöglich vorbei. Derselbe Schlüssel wie im Abruf: meldet
     * der sie später noch einmal, ersetzt er diese Meldung, statt eine zweite
     * danebenzulegen.
     */
    private fun einladungMelden(einladung: Einladung) {
        if (imVordergrund || !einladung.annehmbar) return
        if (schalter?.einladungen == false) return

        val ort = einladung.landkreis?.takeIf { it.isNotBlank() } ?: einladung.ort.ifBlank { einladung.roomCode }
        Sozialmitteilung.zeigen(
            getApplication<Application>(),
            schluessel = "einladung:${einladung.nr}",
            titel = "Einladung — $ort",
            text = "${einladung.vonName} lädt dich in eine Runde ein",
            weg = FreundeWeg.KONTAKTE,
        )
    }

    private fun chatzeileMelden(zeile: Gemeinschaftsnachricht) {
        if (imVordergrund || zeile.von == kennung || zeile.entfernt || zeile.text.isBlank()) return
        if (schalter?.chat == false) return

        val wache = wachenname(zeile.gemeinschaftId) ?: "Wachenchat"
        Sozialmitteilung.zeigen(
            getApplication<Application>(),
            schluessel = "wache:${zeile.gemeinschaftId}",
            titel = "Wachenchat — $wache",
            text = gekuerzt("${zeile.vonName}: ${zeile.text}"),
            weg = Weg.Wache.adresse,
        )
    }

    private fun gekuerzt(text: String): String =
        if (text.length <= 140) text else text.take(139) + "…"

    // -------------------------------------------------------------- Aufrufe

    /**
     * Eine Hub-Methode rufen, die den Grund einer Ablehnung zurückgibt — oder
     * nichts, wenn es klappte.
     *
     * <b>Ohne Leitung gar nicht erst.</b> Ein Aufruf über eine tote Verbindung
     * wartete sonst auf eine Antwort, die nie kommt, und der Knopf darüber drehte
     * sich bis zum Beenden der App.
     */
    private suspend fun rufenMitGrund(methode: String, vararg argumente: JsonElement): String? {
        val d = draht
        if (d.lage != Funkverbindung.Lage.Verbunden) return "Keine Verbindung zur Leitstelle."

        // Eingepackt in `Result`, damit „kam nichts" (Zeitablauf) und „kam `null`"
        // (Erfolg) zwei Dinge bleiben.
        val ergebnis = withTimeoutOrNull(15_000L) { runCatching { d.frage(methode, *argumente) } }
        if (ergebnis == null || ergebnis.isFailure) return "Das hat gerade nicht geklappt."

        val grund = (ergebnis.getOrNull() as? JsonPrimitive)?.contentOrNull
        return grund?.takeIf { it.isNotBlank() && it != "null" }
    }

    // -------------------------------------------------------------- Gespräch

    /**
     * Ein Gespräch öffnen, den Verlauf laden, das Ungelesene quittieren.
     *
     * Geleert wird nur beim Wechsel des Partners. Ist es dasselbe Gespräch (nach
     * einem Geschenk, nach dem Nachholen), wird der frische Stand hineingemischt:
     * Der Server gilt, und nur was über den Hub kam, während die Antwort
     * unterwegs war, wird hinten angehängt — es ist jünger als alles darin.
     */
    fun gespraechOeffnen(mit: String) = viewModelScope.launch {
        val k = eigeneKennung() ?: return@launch
        _stand.update {
            if (it.gespraechMit != mit) {
                it.copy(gespraechMit = mit, verlauf = emptyList(), laedt = true)
            } else {
                it.copy(laedt = it.verlauf.isEmpty())
            }
        }

        runCatching { wege.nachrichtenVerlauf(k, mit) }
            .onSuccess { geladen ->
                // Inzwischen ein anderes Gespräch geöffnet — dessen Verlauf nicht überschreiben.
                if (_stand.value.gespraechMit != mit) return@onSuccess
                val letzte = geladen.maxOfOrNull { it.nr } ?: 0L
                _stand.update {
                    it.copy(
                        verlauf = geladen + it.verlauf.filter { n -> n.nr > letzte },
                        laedt = false,
                    )
                }
                if (imVordergrund) draht.rufen("Gelesen", wert(mit))
                kreis.laden()
                beiFreundesliste?.invoke()
            }
            .onFailure { f ->
                _stand.update {
                    it.copy(laedt = false, meldung = f.message ?: "Der Verlauf ließ sich nicht laden.")
                }
            }
    }

    /**
     * Schließen gehört dazu — sonst quittiert der Handler weiter als „gelesen".
     *
     * Mit `mit` schließt nur, wer das Gespräch auch geöffnet hat: Beim Wechsel
     * von einem Gespräch ins nächste räumt die alte Seite sonst das neue ab.
     */
    fun gespraechSchliessen(mit: String? = null) = _stand.update {
        if (mit != null && it.gespraechMit != mit) it else it.copy(gespraechMit = null, verlauf = emptyList())
    }

    /**
     * Eine Nachricht schicken. Mit `terminText` wird sie zugleich ein
     * Terminvorschlag für eine gemeinsame Schicht; `terminZeitpunkt` (ISO) ist
     * der optionale Zeitpunkt für eine spätere Erinnerung.
     *
     * `danach` sagt, ob es geklappt hat — die Seite legt den Entwurf sonst
     * zurück ins Feld.
     */
    fun senden(
        an: String,
        text: String,
        terminText: String? = null,
        terminZeitpunkt: String? = null,
        danach: (Boolean) -> Unit = {},
    ) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        val grund = rufenMitGrund(
            "Senden",
            wert(an),
            wert(text.trim().take(500)),
            terminText?.let { wert(it) } ?: JsonNull,
            terminZeitpunkt?.let { wert(it) } ?: JsonNull,
        )
        _stand.update { it.copy(meldung = grund) }
        danach(grund == null)
    }

    /** Zu- oder Absage — die Antwort kommt als Ersatz derselben Zeile zurück. */
    fun terminBeantworten(nr: Long, zusagen: Boolean) = viewModelScope.launch {
        val grund = rufenMitGrund("TerminBeantworten", JsonPrimitive(nr), wert(zusagen))
        _stand.update { it.copy(meldung = grund) }
    }

    /**
     * Ein Geschenk öffnen und den Verlauf neu holen — beim Öffnen füllt der
     * Server erst jetzt den Namen des Stücks ein, den er vorher verschwiegen hat.
     */
    fun geschenkOeffnen(nr: Long, danach: (Geschenkinhalt?, String?) -> Unit = { _, _ -> }) =
        viewModelScope.launch {
            val k = eigeneKennung() ?: return@launch
            runCatching { freundewege.geschenkOeffnen(k, nr) }
                .onSuccess { inhalt ->
                    _stand.value.gespraechMit?.let { gespraechOeffnen(it) }
                    danach(inhalt, null)
                }
                .onFailure { f -> danach(null, f.message ?: "Das ging gerade nicht.") }
        }

    // ----------------------------------------------------------- Einladungen

    /**
     * In die eigene Runde einladen — der Rückweg des Eingeladenen ist der GameHub.
     * `alsZuschauer` geht auch in eine volle Runde. Ohne `danach` steht der
     * Erfolg als Meldung da, wie bisher.
     */
    fun einladen(
        an: String,
        roomCode: String,
        alsZuschauer: Boolean = false,
        danach: ((Boolean) -> Unit)? = null,
    ) = viewModelScope.launch {
        val grund = rufenMitGrund("Einladen", wert(an), wert(roomCode), wert(alsZuschauer))
        _stand.update {
            it.copy(meldung = grund ?: if (danach == null) "Einladung ist raus." else null)
        }
        danach?.invoke(grund == null)
    }

    /**
     * Eine Rundeneinladung abhaken — angenommen oder abgelehnt. Der Beitritt
     * selbst passiert danach über die Runde; hier wird nur der Zettel vom Tisch
     * genommen. Ohne diesen Aufruf blieb jede Einladung liegen.
     */
    fun einladungBeantworten(nr: Int, annehmen: Boolean, danach: (Boolean) -> Unit = {}) =
        viewModelScope.launch {
            val grund = rufenMitGrund("EinladungBeantworten", JsonPrimitive(nr), wert(annehmen))
            kreis.einladungWeg(nr)
            beiEinladungen?.invoke()
            _stand.update { it.copy(meldung = grund) }
            danach(grund == null)
        }

    // ------------------------------------------------------------ Wachenchat

    fun wachenchatOeffnen(id: String) = viewModelScope.launch {
        val k = eigeneKennung() ?: return@launch
        _stand.update { it.copy(wachenchatId = id, wachenchat = emptyList()) }
        runCatching { wege.gemeinschaftChat(k, id) }
            .onSuccess { zeilen -> _stand.update { it.copy(wachenchat = zeilen) } }
        if (draht.lage == Funkverbindung.Lage.Verbunden) {
            runCatching {
                draht.frage("GemeinschaftBetreten", wert(id))
                draht.rufen("GemeinschaftGelesen", wert(id))
            }
        }
        beiGemeinschaft?.invoke()
    }

    fun wachenchatSenden(id: String, text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        val grund = rufenMitGrund("GemeinschaftSenden", wert(id), wert(text.trim().take(500)))
        if (grund != null) _stand.update { it.copy(meldung = grund) }
    }

    /**
     * Die Leitung vom Chat einer Gemeinschaft lösen — nach dem Austreten oder
     * Auflösen, damit keine Zeilen mehr hereinkommen, die niemanden mehr angehen.
     */
    fun gemeinschaftVerlassen(id: String) {
        if (draht.lage == Funkverbindung.Lage.Verbunden) draht.rufen("GemeinschaftVerlassen", wert(id))
        _stand.update {
            if (it.wachenchatId == id) it.copy(wachenchatId = null, wachenchat = emptyList()) else it
        }
    }

    // ---------------------------------------------------------------- Freunde

    /**
     * Eine Freundschaftsanfrage stellen — für jede Stelle, die einen
     * „+ Freund"-Knopf trägt. Eine Ablehnung steht danach als Meldung da.
     */
    fun freundAnfragen(wen: String, danach: (() -> Unit)? = null) = kreis.anfragen(wen, danach)

    fun meldungWegnehmen() = _stand.update { it.copy(meldung = null) }

    private fun meldungSetzen(text: String?) = _stand.update { it.copy(meldung = text) }

}

/** Was die Sozialschicht gerade hält. */
data class Sozialstand(
    /** Mit wem das Gespräch offen ist — `null` heißt: keins. */
    val gespraechMit: String? = null,
    val verlauf: List<Nachricht> = emptyList(),
    val laedt: Boolean = false,
    /** Welcher Wachenchat mitgelesen wird. */
    val wachenchatId: String? = null,
    val wachenchat: List<Gemeinschaftsnachricht> = emptyList(),
    /** Was der Server abgelehnt hat — als Streifen über dem Inhalt, bis man ihn wegräumt. */
    val meldung: String? = null,
    /** Wo die Leitung steht. */
    val lage: Funkverbindung.Lage = Funkverbindung.Lage.Getrennt,
)
