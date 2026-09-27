package de.pagerspass.pagerspass.mobil

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.pagerspass.pagerspass.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder

/**
 * Der Einsprung — wohin die App beim Öffnen gehen soll.
 *
 * <b>Drei Quellen, ein Weg.</b> Ein Link auf `https://pagerspass.de/…` (App-Link
 * aus E-Mail, Discord, Stripe oder einem geteilten Raum), ein Tipp auf eine
 * Systemmeldung (`Intent`-Zusatz [ZIEL] mit einer App-Route) und die Rückkehr aus
 * dem Browser nach Kasse, Abo-Verwaltung oder Discord. Alle drei landen hier als
 * **App-Route** — dieselben Zeichenketten, unter denen `PagerSpassApp` die Seiten
 * registriert (`"konto"`, `"freunde/eintrag/42"`, `"code/LEITSTELLE200"`).
 *
 * <b>Die Übersetzung steht an genau einer Stelle</b> ([routeAus]): Webpfad hinein,
 * App-Route heraus. Die Webpfade sind die aus `web/src/router.ts` — mit oder ohne
 * Zweig davor (`/play/mobile/…`, `/play/desktop/…`, `/play/…` oder die alten
 * Pfade ohne `/play`, die das Web über `geraet.ts` → `altlink` weiterreicht).
 *
 * <b>Er wartet, bis jemand ihn abholt.</b> Kommt der Link, bevor die Anmeldung
 * durch ist, bleibt er stehen: Die Anmeldeseite holt nur, was ohne Konto geht
 * (Rechtstexte, Kündigen, Widerrufen, Newsletter, Code), alles andere geht nach
 * der Anmeldung los. Wer ihn abgeholt hat, ruft [erledigt].
 *
 * <b>Unbekanntes wird still übergangen.</b> Eine Route, die es im Graphen nicht
 * gibt, ist kein Absturz, sondern ein Link, der die App eben nur öffnet.
 */
object Einsprung {

    /** Der `Intent`-Zusatz für eine Systemmeldung: eine App-Route, z. B. `"freunde/nachrichten"`. */
    const val ZIEL = "ziel"

    private val _ziel = MutableStateFlow<String?>(null)

    /** Die Route, die als Nächstes geöffnet werden soll — oder nichts. */
    val ziel: StateFlow<String?> = _ziel.asStateFlow()

    /**
     * Nimmt einen Intent entgegen — aus `onCreate` und `onNewIntent`.
     *
     * Der Zusatz [ZIEL] geht vor den Daten: Eine Systemmeldung weiß genau, wohin
     * sie will, und trägt dafür keine Webadresse.
     */
    fun aufnehmen(absicht: Intent?) {
        absicht ?: return
        val route = absicht.getStringExtra(ZIEL)?.trim()?.trimStart('/')?.takeIf { it.isNotBlank() }
            ?: absicht.data?.let(::routeAus)
            ?: return
        _ziel.value = route
    }

    /** Eine Route von Hand vormerken — für Stellen in der App, die dorthin wollen. */
    fun oeffnen(route: String) {
        _ziel.value = route.trimStart('/')
    }

    /** Abgeholt — nur die, die gerade dasteht, damit ein neuerer Einsprung nicht verloren geht. */
    fun erledigt(route: String) {
        _ziel.compareAndSet(route, null)
    }

    private val _nachAnmeldung = MutableStateFlow<String?>(null)

    /**
     * Eine Route, die erst nach der Anmeldung gilt — „Konto anlegen und einlösen"
     * auf der Code-Seite: anmelden, dann zurück dorthin. Getrennt von [ziel], weil
     * die Anmeldeseite [ziel] sonst sofort wieder abholte.
     */
    val nachAnmeldung: StateFlow<String?> = _nachAnmeldung.asStateFlow()

    fun nachDerAnmeldung(route: String) {
        _nachAnmeldung.value = route.trimStart('/')
    }

    fun nachAnmeldungErledigt(route: String) {
        _nachAnmeldung.compareAndSet(route, null)
    }

    private val _premiumRueckkehr = MutableStateFlow<Pair<String, String?>?>(null)

    /**
     * Die Rückkehr von Kasse oder Abo-Verwaltung — `premium` (`erfolg`, `abbruch`,
     * `portal`) und `session_id`. Die Kontozentrale liest sie und räumt sie weg.
     */
    val premiumRueckkehr: StateFlow<Pair<String, String?>?> = _premiumRueckkehr.asStateFlow()

    fun premiumRueckkehrSetzen(weg: String, sitzung: String?) {
        _premiumRueckkehr.value = weg to sitzung
    }

    fun premiumRueckkehrErledigt() {
        _premiumRueckkehr.value = null
    }

    /**
     * Der Intent für eine Systemmeldung, die beim Antippen an eine Stelle der App
     * führt: `PendingIntent.getActivity(…, Einsprung.absicht(context, "freunde/nachrichten"), …)`.
     */
    fun absicht(zusammenhang: Context, route: String): Intent =
        Intent(zusammenhang, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ZIEL, route)

    /** Die Hosts, deren Links die App annimmt — dieselben wie im Manifest. */
    private val HOSTS = setOf("pagerspass.de", "www.pagerspass.de", "beta.pagerspass.de")

    /**
     * Webadresse → App-Route. `null` heißt: nichts Bestimmtes zu öffnen.
     *
     * Öffentlich und ohne Zustand, damit andere Stellen (etwa ein gescannter
     * QR-Code) dieselbe Übersetzung benutzen können.
     */
    fun routeAus(adresse: Uri): String? {
        val schema = adresse.scheme?.lowercase()
        if (schema != "https" && schema != "http") return null
        if (adresse.host?.lowercase() !in HOSTS) return null
        return routeAusPfad(adresse.path.orEmpty(), abfrage(adresse))
    }

    /** Die Abfrage als Karte — nur der erste Wert je Schlüssel, wie im Web-Router. */
    private fun abfrage(adresse: Uri): Map<String, String> = runCatching {
        adresse.queryParameterNames.associateWith { adresse.getQueryParameter(it).orEmpty() }
    }.getOrDefault(emptyMap())

    /**
     * Der eigentliche Übersetzer. Getrennt von [routeAus], damit er ohne `Uri`
     * auskommt — `Uri` ist unter Unit-Tests eine leere Attrappe.
     */
    fun routeAusPfad(roherPfad: String, abfrage: Map<String, String>): String? {
        val pfad = zweigWeg(roherPfad.trimEnd('/'))
        val teile = pfad.split('/').filter { it.isNotBlank() }
        val erstes = teile.getOrNull(0) ?: return "dienst"
        val zweites = teile.getOrNull(1)
        val drittes = teile.getOrNull(2)

        return when (erstes) {
            // Die Anmeldung selbst ist kein Ziel — wer angemeldet ist, landet im Dienst.
            "login", "app" -> "dienst"

            "dienstbuch" -> when (zweites) {
                null -> "dienstbuch"
                "schichten" -> abfrage["schicht"]?.takeIf { it.isNotBlank() }
                    ?.let { "dienstbuch/schicht/${stueck(it)}" } ?: "dienstbuch/schichten"
                "schicht" -> drittes?.let { "dienstbuch/schicht/${stueck(it)}" } ?: "dienstbuch/schichten"
                "laufbahn", "garage", "abzeichen", "auswertung" -> "dienstbuch/$zweites"
                else -> "dienstbuch"
            }

            // Die alten Pfade, die das Web weiterleitet (router.ts, „Chronik und Archiv …").
            "chronik", "archiv" -> "dienstbuch/schichten"
            "bestenliste" -> "dienstbuch/laufbahn"
            "garage" -> "dienstbuch/garage"

            "uebungen" -> zweites?.let { "uebungen/${stueck(it)}" } ?: "uebungen"
            "lehrgang" -> "lehrgang"
            "leitstellenbau" -> "leitstellenbau"

            "welt", "world" -> when (zweites) {
                null -> "welt"
                "leitstelle" -> "welt/leitstelle"
                "icons" -> drittes?.let { "welt/icons/${stueck(it)}" } ?: "welt/icons"
                else -> "welt"
            }

            "konto" -> when (zweites) {
                "privatsphaere" -> "privatsphaere"
                "mitteilungen" -> "mitteilungen"
                else -> kontoRoute(abfrage)
            }

            "shop" -> "shop"

            "discord" -> buildString {
                append("discord")
                val fertig = abfrage["fertig"]
                val fehler = abfrage["fehler"]
                val teil = listOfNotNull(
                    fertig?.let { "fertig=${stueck(it)}" },
                    fehler?.let { "fehler=${stueck(it)}" },
                )
                if (teil.isNotEmpty()) append("?" + teil.joinToString("&"))
            }

            "code" -> zweites?.let { "code/${stueck(it.uppercase())}" }
            "recht" -> zweites?.let { "recht/${stueck(it)}" }
            "newsletter" -> if (zweites == "abmelden") {
                "newsletter/abmelden?schluessel=${stueck(abfrage["schluessel"].orEmpty())}"
            } else {
                null
            }
            "vertrag-kuendigen" -> "vertrag/kuendigen"
            "vertrag-widerrufen" -> "vertrag/widerrufen"

            "freunde" -> when (zweites) {
                null -> "freunde"
                "liste", "nachrichten", "kontakte" -> "freunde/$zweites"
                "eintrag", "profil", "gespraech" ->
                    drittes?.let { "freunde/$zweites/${stueck(it)}" } ?: "freunde"
                else -> "freunde"
            }

            "gemeinschaft" -> if (zweites == "shop") "gemeinschaft/shop" else "gemeinschaft"
            "gemeinschaften" -> when {
                zweites == "rangliste" -> "gemeinschaften/rangliste"
                !abfrage["code"].isNullOrBlank() -> "gemeinschaften?code=${stueck(abfrage.getValue("code"))}"
                else -> "gemeinschaften"
            }

            // Die bestehende Unterseite der App heißt anders als der Webpfad.
            "oeffentliche-runden" -> "oeffentlicheRunden"
            "raum" -> zweites?.let { "raum/${stueck(it.uppercase())}" }
            "funk" -> zweites?.let { "funk/${stueck(it)}" }
            "scan" -> "scan"

            else -> null
        }
    }

    /**
     * Die Rückkehr von Kasse und Abo-Verwaltung: `/konto?premium=erfolg&session_id=…`.
     * Die Kontoseite liest beides aus ihrer Route (siehe `mobil/Kontobereich.kt`).
     */
    private fun kontoRoute(abfrage: Map<String, String>): String {
        val premium = abfrage["premium"]?.takeIf { it.isNotBlank() } ?: return "konto"
        val sitzung = abfrage["session_id"]?.takeIf { it.isNotBlank() }
        return buildString {
            append("konto?premium=${stueck(premium)}")
            if (sitzung != null) append("&session_id=${stueck(sitzung)}")
        }
    }

    /**
     * Den Zweig abschneiden: `/play/mobile/x` → `/x`, `/play/desktop/x` → `/x`,
     * `/play/x` → `/x`. Was ohne `/play` kommt, ist schon ein Pfad der Anwendung
     * (die alten Links — `/code/X`, `/vertrag-kuendigen`, `/recht/agb`).
     */
    private fun zweigWeg(pfad: String): String {
        val zweige = listOf("/play/mobile", "/play/desktop", "/play")
        for (zweig in zweige) {
            if (pfad == zweig) return "/"
            if (pfad.startsWith("$zweig/")) return pfad.removePrefix(zweig)
        }
        return pfad
    }

    /**
     * Ein Stück Route, sicher gemacht: Ein Schrägstrich oder ein Fragezeichen im
     * Raumcode darf nicht zu einer anderen Route werden.
     */
    private fun stueck(wert: String): String =
        URLEncoder.encode(wert, "UTF-8").replace("+", "%20")
}
