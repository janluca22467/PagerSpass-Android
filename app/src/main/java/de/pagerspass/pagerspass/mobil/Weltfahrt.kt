package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltFremdfahrzeug
import java.time.OffsetDateTime
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Wie ein Fahrzeug der Welt zwischen zwei Abrufen fährt — übertragen aus `stores/welt.ts`
 * (`strecke` bis `restMinuten`), Zeile für Zeile.
 *
 * <b>Der Server fährt, die App fährt nur nach.</b> Seit 4.0.0.50 fährt die `Weltuhr` jede
 * Fahrt selbst ab, und `lat`/`lon` ist der Ort dieser Sekunde. Hier wird daraus nur noch
 * die halbe Sekunde bis zum nächsten Bild: dieselbe Rechnung (`Geo.BewegenEntlang`), von
 * der gemeldeten Position aus, mit dem gemeldeten Tempo. Liefen beide auseinander,
 * spränge die Marke bei jedem Abruf.
 *
 * <b>Zwei Uhren.</b> `versatzMs` ist, wie weit die Uhr dieses Geräts von der des Servers
 * abweicht — gebraucht überall dort, wo ein Serverzeitpunkt mit dem Jetzt verglichen
 * wird. `standSeitMs` ist, wann der letzte Betrieb galt, nach der Uhr <em>dieses</em>
 * Geräts — für „wie viele Meter seit der Auskunft" zählt eine Spanne, und die misst die
 * lokale Uhr richtig, auch wenn sie zwei Minuten falsch geht.
 */
class Weltfahrt {

    @Volatile var versatzMs: Long = 0
    @Volatile var standSeitMs: Long = 0

    /** Die Form, die jede Fahrt hat — eigene wie fremde. */
    class Fahrt(
        val lat: Double,
        val lon: Double,
        val zielLat: Double,
        val zielLon: Double,
        val losUm: String?,
        val ankunftUm: String?,
        val route: String?,
        val routeIndex: Int,
        val tempoMs: Double,
        val lage: String = "",
    )

    /** Ein Stand auf der Strecke: Ort und der Stützpunkt, auf den danach zugefahren wird. */
    class Stand(val lat: Double, val lon: Double, val index: Int)

    /** Ort und Kurs in einer Rechnung — siehe `fahrtLage`. */
    class Lage(val lat: Double, val lon: Double, val kurs: Double)

    // ----------------------------------------------------------------- Strecke

    /**
     * Die Stützpunkte einer Route — einmal zerlegt und gemerkt.
     *
     * `fahrzeugOrt` läuft fünfmal je Sekunde für jede Marke; ein `split` über zweihundert
     * Punkte je Aufruf wäre für eine Zeichenkette, die sich während der ganzen Fahrt nicht
     * ändert, eine Viertelmillion Zerlegungen in der Sekunde.
     */
    private val routen = HashMap<String, List<DoubleArray>>()

    fun strecke(roh: String?): List<DoubleArray> {
        if (roh.isNullOrEmpty()) return emptyList()

        routen[roh]?.let { return it }

        val punkte = ArrayList<DoubleArray>()
        for (stueck in roh.split(';')) {
            val teile = stueck.split(',')
            val a = teile.getOrNull(0)?.trim()?.toDoubleOrNull()
            val b = teile.getOrNull(1)?.trim()?.toDoubleOrNull()
            if (a != null && b != null && a.isFinite() && b.isFinite()) punkte += doubleArrayOf(a, b)
        }

        // Bei tausend Einträgen wird geleert — eine Route, die danach noch gebraucht wird,
        // ist in einer Millisekunde wieder da.
        if (routen.size > 1000) routen.clear()
        routen[roh] = punkte

        return punkte
    }

    /**
     * Kleinwinkelnäherung: Auf der Länge eines Streckenstücks weicht sie von der
     * Haversine-Formel um Promille ab, und sie kostet einen Bruchteil.
     */
    fun meter(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val dLat = (bLat - aLat) * 111_320.0
        val dLon = (bLon - aLon) * 111_320.0 * cos(((aLat + bLat) / 2.0) * (Math.PI / 180.0))
        return hypot(dLat, dLon)
    }

    /**
     * Wie viele Meter dieses Fahrzeug seit der letzten Auskunft gefahren ist — ab dem
     * späteren von Abruf und Abfahrt. Steht es noch am Tor, liegt seine Abfahrt in der
     * Zukunft, und gezählt wird erst ab ihr.
     */
    fun gefahreneMeter(f: Fahrt): Double {
        if (f.tempoMs <= 0.0) return 0.0

        val los = weltzeit(f.losUm)
        val losLokal = if (los != null) los - versatzMs else standSeitMs
        val ab = max(standSeitMs, losLokal)

        return max(0.0, ((System.currentTimeMillis() - ab) / 1000.0) * f.tempoMs)
    }

    /**
     * Fährt eine Strecke von einem Punkt aus weiter — zeichengetreu dieselbe Schleife wie
     * `Geo.BewegenEntlang` auf dem Server.
     */
    fun entlang(
        punkte: List<DoubleArray>,
        ab: Int,
        startLat: Double,
        startLon: Double,
        strecke: Double,
    ): Stand {
        var uebrig = max(0.0, strecke)
        var index = max(0, min(ab, punkte.size))
        var lat = startLat
        var lon = startLon

        while (index < punkte.size) {
            val ziel = punkte[index]
            val abstand = meter(lat, lon, ziel[0], ziel[1])

            // Stützpunkt erreicht: weiter zum nächsten, mit dem Rest des Wegs.
            if (abstand <= uebrig) {
                lat = ziel[0]
                lon = ziel[1]
                uebrig -= abstand
                index++
                continue
            }

            val teil = uebrig / abstand
            return Stand(lat + (ziel[0] - lat) * teil, lon + (ziel[1] - lon) * teil, index)
        }

        return Stand(lat, lon, index)
    }

    /**
     * Dieselbe Fahrt rückwärts — für die Kolonne. Der Rückstand muss wirklich
     * zurückgefahren werden: an derselben Straße entlang, nur in die andere Richtung.
     */
    fun zurueck(
        punkte: List<DoubleArray>,
        ab: Int,
        startLat: Double,
        startLon: Double,
        strecke: Double,
    ): Stand {
        var uebrig = max(0.0, strecke)
        var index = max(0, min(ab, punkte.size))
        var lat = startLat
        var lon = startLon

        while (index > 0) {
            val ziel = punkte[index - 1]
            val abstand = meter(lat, lon, ziel[0], ziel[1])

            if (abstand <= uebrig) {
                lat = ziel[0]
                lon = ziel[1]
                uebrig -= abstand
                index--
                continue
            }

            val teil = uebrig / abstand
            return Stand(lat + (ziel[0] - lat) * teil, lon + (ziel[1] - lon) * teil, index)
        }

        // Am Anfang der Route ist Schluss — auf dem Hof, wo die Kolonne in Wirklichkeit
        // auch steht.
        return Stand(lat, lon, index)
    }

    /**
     * Ob diese Fahrt noch auf ihre Straßenroute wartet — dann steht die Marke.
     * Zeichengetreu dieselbe Bedingung wie `Weltlauf.WartetAufRoute`.
     */
    fun wartetAufRoute(f: Fahrt): Boolean {
        val los = weltzeit(f.losUm) ?: return false
        val an = weltzeit(f.ankunftUm) ?: return false

        val jetzt = System.currentTimeMillis() + versatzMs
        val anteilig = (an - los) * ROUTENFRIST_ANTEIL
        val frist = if (anteilig < ROUTENFRIST_MS) {
            ROUTENFRIST_MS.toDouble()
        } else {
            min(anteilig, ROUTENFRIST_HOECHSTENS_MS.toDouble())
        }

        return jetzt - los < frist && an > jetzt + NACHREICHEN_LOHNT_AB_MS
    }

    /** Dasselbe ohne Route: geradeaus aufs Ziel zu, höchstens bis dorthin. */
    fun geradeaus(f: Fahrt, strecke: Double): Stand {
        val rest = meter(f.lat, f.lon, f.zielLat, f.zielLon)
        if (rest <= 0.0) return Stand(f.zielLat, f.zielLon, 0)

        // Ein negativer Wert ist der Rückstand in der Kolonne und zieht die Marke ein
        // Stück hinter die Spitze; weiter als eine ganze Fahrtlänge zurück geht es nicht.
        val teil = min(1.0, max(-1.0, strecke / rest))

        return Stand(
            f.lat + (f.zielLat - f.lat) * teil,
            f.lon + (f.zielLon - f.lon) * teil,
            0,
        )
    }

    /** Wo ein Fahrzeug in diesem Augenblick steht, samt dem Stützpunkt vor ihm. */
    fun fahrtstand(f: Fahrt, rueckstandMeter: Double = 0.0): Stand {
        val weg = gefahreneMeter(f) - rueckstandMeter
        val punkte = strecke(f.route)

        if (punkte.size <= 1) {
            // Die Straße wird noch geholt: Die Marke steht, wo der Server sie gemeldet hat.
            if (wartetAufRoute(f)) return Stand(f.lat, f.lon, 0)
            return geradeaus(f, weg)
        }

        return if (weg >= 0) {
            entlang(punkte, f.routeIndex, f.lat, f.lon, weg)
        } else {
            zurueck(punkte, f.routeIndex, f.lat, f.lon, -weg)
        }
    }

    /** Wie weit eine laufende Fahrt ist, 0 bis 1 — nach der Zeit, für den Balken. */
    fun anteil(losUm: String?, ankunftUm: String?): Double {
        val los = weltzeit(losUm) ?: return 0.0
        val an = weltzeit(ankunftUm) ?: return 0.0
        if (an <= los) return 1.0

        val jetzt = System.currentTimeMillis() + versatzMs
        return min(1.0, max(0.0, (jetzt - los).toDouble() / (an - los)))
    }

    /** Wo ein Fahrzeug in diesem Augenblick steht — mit Rückstand in der Kolonne. */
    fun fahrzeugOrt(f: Fahrt, rueckstandMeter: Double = 0.0): Pair<Double, Double> {
        val s = fahrtstand(f, rueckstandMeter)
        return s.lat to s.lon
    }

    /** Der Kompasskurs, in den die Marke zeigt. 0 bis 360, Norden ist 0. */
    fun kurs(f: Fahrt): Double = kursAus(f, strecke(f.route), fahrtstand(f))

    /** Ort <b>und</b> Kurs in einer Rechnung — die Karte braucht je Bild beides. */
    fun fahrtLage(f: Fahrt, rueckstandMeter: Double = 0.0): Lage {
        val punkte = strecke(f.route)
        val stand = fahrtstand(f, rueckstandMeter)
        return Lage(stand.lat, stand.lon, kursAus(f, punkte, stand))
    }

    /** Die Kursrechnung selbst — auf einem Stand, der schon vorliegt. */
    private fun kursAus(f: Fahrt, punkte: List<DoubleArray>, stand: Stand): Double {
        // Es steht: die Richtung des letzten Streckenstücks.
        if (f.losUm == null || f.ankunftUm == null) {
            if (punkte.size > 1) return peilungAus(punkte[punkte.size - 2], punkte[punkte.size - 1])
            return richtungOderNull(f.lat, f.lon, f.zielLat, f.zielLon) ?: 0.0
        }

        // Der Stützpunkt vor ihm ist die Richtung. Ist keiner mehr übrig, behält die Marke
        // die Richtung des letzten Stücks, statt sich nach Norden zu drehen.
        if (punkte.size > 1) {
            val vorn = punkte.getOrNull(stand.index)
            return (vorn?.let { richtungOderNull(stand.lat, stand.lon, it[0], it[1]) })
                ?: peilungAus(punkte[punkte.size - 2], punkte[punkte.size - 1])
        }

        return richtungOderNull(stand.lat, stand.lon, f.zielLat, f.zielLon) ?: 0.0
    }

    /** Die Peilung zwischen zwei Punkten — null, wenn es derselbe Punkt ist. */
    private fun richtungOderNull(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double? {
        if (kotlin.math.abs(bLat - aLat) < 1e-9 && kotlin.math.abs(bLon - aLon) < 1e-9) return null
        return weltpeilung(aLat, aLon, bLat, bLon)
    }

    private fun peilungAus(a: DoubleArray, b: DoubleArray): Double =
        weltpeilung(a[0], a[1], b[0], b[1])

    /** Wie weit eine laufende Fahrt ist, 0 bis 1 — für den Balken in der Fahrzeugliste. */
    fun fortschritt(losUm: String?, ankunftUm: String?): Double = anteil(losUm, ankunftUm)

    /**
     * Ob das Fahrzeug noch am Tor steht — ein eigener Zustand (`Ausrueckt`) und als
     * zweiter Weg die Zeitrechnung, falls Uhr und Lage einen Takt auseinanderliegen.
     */
    fun ruecktAus(lage: String?, losUm: String?): Boolean {
        if (lage == "Ausrueckt") return true
        val los = weltzeit(losUm) ?: return false
        return System.currentTimeMillis() + versatzMs < los
    }

    /**
     * Wie viele Minuten es noch bis zur Ankunft sind. Aufgerundet: „noch 0 min" wäre für
     * die letzten sechzig Sekunden eine Lüge.
     */
    fun restMinuten(ankunftUm: String?): Int? {
        val an = weltzeit(ankunftUm) ?: return null
        val ms = an - (System.currentTimeMillis() + versatzMs)
        return if (ms <= 0) 0 else ceil(ms / 60_000.0).toInt()
    }

    /** Die Serverzeit dieses Augenblicks. */
    fun jetztServer(): Long = System.currentTimeMillis() + versatzMs

    companion object {
        /** So lange steht eine Fahrt ohne Route mindestens still — `Weltlauf.Routenfrist`. */
        const val ROUTENFRIST_MS = 60_000L

        /** Der Deckel der Wartezeit — `Weltlauf.RoutenfristHoechstens`. */
        const val ROUTENFRIST_HOECHSTENS_MS = 300_000L

        /** Welcher Anteil der Fahrt höchstens im Warten vergeht — `Weltlauf.RoutenfristAnteil`. */
        const val ROUTENFRIST_ANTEIL = 0.05

        /** `Weltuhr.LohntSichAb`: ab dieser Restzeit wird keine Route mehr nachgereicht. */
        const val NACHREICHEN_LOHNT_AB_MS = 45_000L

        /** Wie viele Meter ein Fahrzeug hinter dem vorigen derselben Kolonne fährt. */
        const val ABSTAND_IN_KOLONNE = 120.0
    }
}

/** Die Peilung zwischen zwei Punkten, 0 bis 360 — Norden ist 0. */
fun weltpeilung(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
    val rad = Math.PI / 180.0
    val dLon = (bLon - aLon) * rad
    val y = sin(dLon) * cos(bLat * rad)
    val x = cos(aLat * rad) * sin(bLat * rad) - sin(aLat * rad) * cos(bLat * rad) * cos(dLon)
    return (atan2(y, x) / rad + 360.0) % 360.0
}

/** Eine eigene Fahrt in der gemeinsamen Form. */
fun WeltFahrzeug.fahrt(): Weltfahrt.Fahrt = Weltfahrt.Fahrt(
    lat, lon, zielLat, zielLon, losUm, ankunftUm, route, routeIndex, tempoMs, lage,
)

/** Eine fremde Fahrt in der gemeinsamen Form. */
fun WeltFremdfahrzeug.fahrt(): Weltfahrt.Fahrt = Weltfahrt.Fahrt(
    lat, lon, zielLat, zielLon, losUm, ankunftUm, route, routeIndex, tempoMs, lage,
)

/**
 * Ein Zeitpunkt des Servers in Millisekunden — `null`, wenn keiner da ist oder er sich
 * nicht lesen lässt.
 *
 * <b>Gemerkt</b>, weil dieselben Zeitpunkte fünfmal je Sekunde für jede Marke gebraucht
 * werden; `OffsetDateTime.parse` ist dafür zu teuer.
 */
fun weltzeit(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    zeitpunkte[iso]?.let { return it }
    val wert = runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
        .recoverCatching { java.time.Instant.parse(iso).toEpochMilli() }
        .getOrNull()
        ?: return null
    synchronized(zeitpunkte) {
        if (zeitpunkte.size > 4000) zeitpunkte.clear()
        zeitpunkte[iso] = wert
    }
    return wert
}

private val zeitpunkte = java.util.concurrent.ConcurrentHashMap<String, Long>()
