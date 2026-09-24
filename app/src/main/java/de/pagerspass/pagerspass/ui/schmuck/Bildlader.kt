package de.pagerspass.pagerspass.ui.schmuck

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Der Bildlader — für Profilbilder, und bisher nur dafür.
 *
 * <b>Warum keine Bibliothek.</b> Coil oder Glide brächten einen
 * Abhängigkeitsbaum mit, dessen Gewinn hier gesparte Zeilen wären: Die App lädt
 * genau eine Sorte Bild, quadratisch, klein, von genau einem Server. Was an
 * einer Bibliothek wirklich hängt — Zwischenspeicher, Abbruch beim Verlassen der
 * Seite, kein zweiter Abruf für dasselbe Bild — steht unten und ist überschaubar.
 * Dieselbe Überlegung wie bei `Netz`: eine Bibliothek für eine Anforderung, die
 * man in fünfzig Zeilen erfüllt, ist keine Ersparnis.
 *
 * <b>Der Zwischenspeicher ist der eigentliche Punkt.</b> Eine Freundesliste mit
 * dreißig Zeilen zeigt beim Rollen dieselben Bilder immer wieder; ohne Speicher
 * wäre jedes Wiedersehen ein neuer Abruf. Er ist nach Größe begrenzt, nicht nach
 * Anzahl — ein Bild kostet so viel Speicher, wie es Pixel hat, und nicht so viel,
 * wie es Einträge sind.
 */
private object Bildspeicher {
    /**
     * Ein Achtel des Speichers, den die App bekommen darf.
     *
     * `LruCache` rechnet in Kilobyte, deshalb die Division. Ein Achtel ist die
     * Zahl, die Android selbst in seinen Beispielen nimmt — genug für ein paar
     * hundert Profilbilder, klein genug, dass daneben noch eine Karte passt.
     */
    private val achtel = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()

    private val speicher = object : LruCache<String, ImageBitmap>(achtel) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            value.width * value.height * 4 / 1024
    }

    operator fun get(adresse: String): ImageBitmap? = speicher[adresse]

    operator fun set(adresse: String, bild: ImageBitmap) {
        speicher.put(adresse, bild)
    }
}

/**
 * Ein Bild von einer Adresse — oder `null`, solange es keins gibt.
 *
 * <b>`null` ist kein Fehler, sondern der Normalfall.</b> Die meisten Konten
 * haben kein Bild, und wer eines hat, sieht es eine Zehntelsekunde später. Die
 * aufrufende Stelle zeichnet in dieser Zeit das Wappen — nicht einen leeren
 * Kreis, der dann springt.
 *
 * <b>Ein kaputtes Bild führt nicht zu einer Meldung.</b> Ein abgenommenes,
 * gelöschtes oder nicht ladbares Bild fällt still auf das Wappen zurück; das ist
 * dasselbe Verhalten wie im Web (`@error` an `<img>`).
 */
@Composable
fun bildVon(adresse: String?): State<ImageBitmap?> {
    val stand = remember(adresse) { mutableStateOf(adresse?.let { Bildspeicher[it] }) }

    LaunchedEffect(adresse) {
        if (adresse == null || stand.value != null) return@LaunchedEffect

        stand.value = withContext(Dispatchers.IO) {
            runCatching {
                val draht = (URL(adresse).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 20_000
                }
                try {
                    draht.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
                } finally {
                    draht.disconnect()
                }
            }.getOrNull()?.also { Bildspeicher[adresse] = it }
        }
    }

    return stand
}

/**
 * Die Adresse eines freigegebenen Profilbildes.
 *
 * <b>Gespeichert und ausgeliefert wird nur der Dateiname</b> — der Pfad davor
 * steht an dieser einen Stelle. Stünde er in den Ansichten, hinge er an so vielen
 * Stellen, wie es Listen mit Kontobildern gibt, und ein Umzug träfe sie alle.
 */
fun profilbildAdresse(server: String, dateiname: String?): String? =
    dateiname?.takeIf { it.isNotBlank() }?.let { "$server/api/profilbild/$it" }
