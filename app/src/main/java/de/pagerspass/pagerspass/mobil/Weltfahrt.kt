package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.WeltFahrzeug
import de.pagerspass.pagerspass.netz.WeltFremdfahrzeug
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Wo ein Fahrzeug der Welt *gerade jetzt* steht — das Gegenstück zu
 * `fahrzeugOrt` und Umgebung in `web/src/stores/welt.ts`.
 *
 * <b>Die Weltuhr fährt jede Fahrt selbst ab</b>, der Betrieb kommt aber nur alle
 * fünf Sekunden. Dazwischen rechnet die Karte weiter: vom gemeldeten Ort aus,
 * entlang der Route, mit dem Tempo, das der Server mitschickt. Ohne das
 * sprängen die Marken im Fünf-Sekunden-Takt.
 *
 * <b>Die Zahlen hier sind die des Webs, zeichengleich.</b> Wer sie hier anders
 * wählt, sieht dieselbe Welt in App und Browser verschieden fahren.
 *
 * @param versatzMs Serverzeit minus Gerätezeit, aus dem letzten Betrieb.
 * @param standSeitMs Wann (Gerätezeit) der letzte Betrieb ankam.
 */
class Weltfahrt(val versatzMs: Long, val standSeitMs: Long) {

    /** Die gemeinsame Form von eigenem und fremdem Fahrzeug. */
    data class Fahrt(
        val lat: Double,
        val lon: Double,
        val zielLat: Double,
        val zielLon: Double,
        val losUm: String?,
        val ankunftUm: String?,
        val route: String?,
        val routeIndex: Int,
        val tempoMs: Double,
    )

    data class Stand(val lat: Double, val lon: Double, val index: Int)

    /** Ort und Fahrtrichtung in Grad (0 = Norden). */
    data class Lage(val lat: Double, val lon: Double, val kurs: Double)

    private fun jetzt() = System.currentTimeMillis()

    private fun gefahreneMeter(f: Fahrt): Double {
        if (f.tempoMs <= 0) return 0.0
        val losLokal = f.losUm?.let { zeit(it) - versatzMs } ?: standSeitMs
        val ab = max(standSeitMs, losLokal)
        return max(0.0, (jetzt() - ab) / 1000.0 * f.tempoMs)
    }

    /**
     * Ob die Straße noch geholt wird. So lange steht die Marke, wo der Server
     * sie gemeldet hat — vorher zog sie quer über Felder und sprang beim
     * Nachreichen auf die Straße zurück.
     */
    private fun wartetAufRoute(f: Fahrt): Boolean {
        val los = f.losUm?.let { zeit(it) } ?: return false
        val an = f.ankunftUm?.let { zeit(it) } ?: return false
        val jetzt = jetzt() + versatzMs
        val anteilig = (an - los) * ROUTENFRIST_ANTEIL
        val frist = if (anteilig < ROUTENFRIST_MS) ROUTENFRIST_MS.toDouble()
        else min(anteilig, ROUTENFRIST_HOECHSTENS_MS.toDouble())
        return jetzt - los < frist && an > jetzt + NACHREICHEN_LOHNT_AB_MS
    }

    private fun geradeaus(f: Fahrt, strecke: Double): Stand {
        val rest = meter(f.lat, f.lon, f.zielLat, f.zielLon)
        if (rest <= 0) return Stand(f.zielLat, f.zielLon, 0)
        val teil = min(1.0, max(-1.0, strecke / rest))
        return Stand(f.lat + (f.zielLat - f.lat) * teil, f.lon + (f.zielLon - f.lon) * teil, 0)
    }

    fun stand(f: Fahrt, rueckstandMeter: Double = 0.0): Stand {
        val weg = gefahreneMeter(f) - rueckstandMeter
        val punkte = strecke(f.route)
        if (punkte.size <= 1) {
            if (wartetAufRoute(f)) return Stand(f.lat, f.lon, 0)
            return geradeaus(f, weg)
        }
        return if (weg >= 0) entlang(punkte, f.routeIndex, f.lat, f.lon, weg)
        else zurueck(punkte, f.routeIndex, f.lat, f.lon, -weg)
    }

    fun lage(f: Fahrt, rueckstandMeter: Double = 0.0): Lage {
        val punkte = strecke(f.route)
        val s = stand(f, rueckstandMeter)
        return Lage(s.lat, s.lon, kursAus(f, punkte, s))
    }

    /** Wie weit die Fahrt ist, zwischen 0 und 1 — für den Balken. */
    fun anteil(losUm: String?, ankunftUm: String?): Double {
        val los = losUm?.let { zeit(it) } ?: return 0.0
        val an = ankunftUm?.let { zeit(it) } ?: return 0.0
        if (an <= los) return 1.0
        return min(1.0, max(0.0, (jetzt() + versatzMs - los).toDouble() / (an - los)))
    }

    /** Ob das Fahrzeug noch auf dem Hof steht: alarmiert, aber nicht losgefahren. */
    fun ruecktAus(lage: String?, losUm: String?): Boolean {
        if (lage == "Ausrueckt") return true
        val los = losUm?.let { zeit(it) } ?: return false
        return jetzt() + versatzMs < los
    }

    /** Minuten bis zu einem Serverzeitpunkt; `null` ohne Zeitpunkt, 0 wenn vorbei. */
    fun restMinuten(um: String?): Int? {
        val ziel = um?.let { zeit(it) } ?: return null
        val ms = ziel - (jetzt() + versatzMs)
        return if (ms <= 0) 0 else ceil(ms / 60_000.0).toInt()
    }

    /** Millisekunden bis zu einem Serverzeitpunkt, in Serverzeit gemessen. */
    fun restMs(um: String?): Long? = um?.let { zeit(it) - (jetzt() + versatzMs) }

    private fun kursAus(f: Fahrt, punkte: List<DoubleArray>, s: Stand): Double {
        if (f.losUm == null || f.ankunftUm == null) {
            if (punkte.size > 1) return peilung(punkte[punkte.size - 2], punkte[punkte.size - 1])
            return richtung(f.lat, f.lon, f.zielLat, f.zielLon) ?: 0.0
        }
        if (punkte.size > 1) {
            val vorn = punkte.getOrNull(s.index)
            return (vorn?.let { richtung(s.lat, s.lon, it[0], it[1]) })
                ?: peilung(punkte[punkte.size - 2], punkte[punkte.size - 1])
        }
        return richtung(s.lat, s.lon, f.zielLat, f.zielLon) ?: 0.0
    }

    companion object {
        private const val ROUTENFRIST_MS = 60_000L
        private const val ROUTENFRIST_HOECHSTENS_MS = 300_000L
        private const val ROUTENFRIST_ANTEIL = 0.05
        private const val NACHREICHEN_LOHNT_AB_MS = 45_000L

        /**
         * Die Stützpunkte einer Route — einmal zerlegt und gemerkt. Die Karte
         * fragt mehrmals je Sekunde je Marke; die Zeichenkette ändert sich
         * während der ganzen Fahrt nicht.
         */
        private val routen = ConcurrentHashMap<String, List<DoubleArray>>()
        private val zeiten = ConcurrentHashMap<String, Long>()

        fun strecke(roh: String?): List<DoubleArray> {
            if (roh.isNullOrEmpty()) return emptyList()
            routen[roh]?.let { return it }
            val punkte = roh.split(';').mapNotNull { stueck ->
                val teile = stueck.split(',')
                val a = teile.getOrNull(0)?.toDoubleOrNull()
                val b = teile.getOrNull(1)?.toDoubleOrNull()
                if (a != null && b != null) doubleArrayOf(a, b) else null
            }
            if (routen.size > 1000) routen.clear()
            routen[roh] = punkte
            return punkte
        }

        /** Ein ISO-Zeitpunkt als Millisekunden; unlesbar heißt 0. */
        fun zeit(roh: String): Long {
            zeiten[roh]?.let { return it }
            val wert = runCatching { Instant.parse(roh).toEpochMilli() }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(roh).toInstant().toEpochMilli() }.getOrNull()
                ?: 0L
            if (zeiten.size > 4000) zeiten.clear()
            zeiten[roh] = wert
            return wert
        }

        /** Kleinwinkelnäherung — auf einem Streckenstück weicht sie um Promille ab. */
        fun meter(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
            val dLat = (bLat - aLat) * 111_320
            val dLon = (bLon - aLon) * 111_320 * cos((aLat + bLat) / 2 * (PI / 180))
            return hypot(dLat, dLon)
        }

        private fun entlang(punkte: List<DoubleArray>, ab: Int, sLat: Double, sLon: Double, strecke: Double): Stand {
            var uebrig = max(0.0, strecke)
            var index = ab.coerceIn(0, punkte.size)
            var lat = sLat
            var lon = sLon
            while (index < punkte.size) {
                val ziel = punkte[index]
                val abstand = meter(lat, lon, ziel[0], ziel[1])
                if (abstand <= uebrig) {
                    lat = ziel[0]; lon = ziel[1]; uebrig -= abstand; index++
                    continue
                }
                val teil = uebrig / abstand
                return Stand(lat + (ziel[0] - lat) * teil, lon + (ziel[1] - lon) * teil, index)
            }
            return Stand(lat, lon, index)
        }

        private fun zurueck(punkte: List<DoubleArray>, ab: Int, sLat: Double, sLon: Double, strecke: Double): Stand {
            var uebrig = max(0.0, strecke)
            var index = ab.coerceIn(0, punkte.size)
            var lat = sLat
            var lon = sLon
            while (index > 0) {
                val ziel = punkte[index - 1]
                val abstand = meter(lat, lon, ziel[0], ziel[1])
                if (abstand <= uebrig) {
                    lat = ziel[0]; lon = ziel[1]; uebrig -= abstand; index--
                    continue
                }
                val teil = uebrig / abstand
                return Stand(lat + (ziel[0] - lat) * teil, lon + (ziel[1] - lon) * teil, index)
            }
            return Stand(lat, lon, index)
        }

        private fun richtung(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double? {
            if (abs(bLat - aLat) < 1e-9 && abs(bLon - aLon) < 1e-9) return null
            return peilung(doubleArrayOf(aLat, aLon), doubleArrayOf(bLat, bLon))
        }

        private fun peilung(a: DoubleArray, b: DoubleArray): Double {
            val rad = PI / 180
            val dLon = (b[1] - a[1]) * rad
            val y = sin(dLon) * cos(b[0] * rad)
            val x = cos(a[0] * rad) * sin(b[0] * rad) - sin(a[0] * rad) * cos(b[0] * rad) * cos(dLon)
            return (atan2(y, x) / rad + 360) % 360
        }

        /** Luftlinie in Metern (Haversine) — für Entfernungen in Listen. */
        fun luftlinie(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
            val r = 6_371_000.0
            val dLat = (bLat - aLat) * PI / 180
            val dLon = (bLon - aLon) * PI / 180
            val m = sin(dLat / 2).let { it * it } +
                cos(aLat * PI / 180) * cos(bLat * PI / 180) * sin(dLon / 2).let { it * it }
            return 2 * r * kotlin.math.asin(min(1.0, kotlin.math.sqrt(m)))
        }
    }
}

fun WeltFahrzeug.fahrt() = Weltfahrt.Fahrt(lat, lon, zielLat, zielLon, losUm, ankunftUm, route, routeIndex, tempoMs)

fun WeltFremdfahrzeug.fahrt() = Weltfahrt.Fahrt(lat, lon, zielLat, zielLon, losUm, ankunftUm, route, routeIndex, tempoMs)
