package de.pagerspass.pagerspass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import de.pagerspass.pagerspass.mobil.Meldermeldung
import de.pagerspass.pagerspass.mobil.PagerSpassApp
import de.pagerspass.pagerspass.mobil.Runde
import de.pagerspass.pagerspass.mobil.Sitzung
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.PagerSpassTheme

/**
 * Der Einstieg.
 *
 * <b>Von Kante zu Kante, und beide Systemleisten durchsichtig.</b> Der Grund der
 * Seite (Raster, Schein, Verlauf) läuft im Web unter Uhr und Wischstreifen
 * durch; hier tut er dasselbe. Die Aussparungen hält jede Seite selbst frei —
 * `Seite` rechnet sie in ihr Polster ein, die Tableiste füllt den Streifen unter
 * sich mit ihrer eigenen Farbe.
 *
 * <b>Beide Leisten ausdrücklich dunkel.</b> Ohne das setzt Android die Symbole
 * der Statusleiste nach der Systemeinstellung — auf einem Gerät im hellen Modus
 * stünde eine schwarze Uhr auf unserem schwarzen Grund.
 *
 * <b>Die Sitzung hängt an der Activity, nicht an der Oberfläche.</b> Sie hält
 * Konto, Merkmal und Serveradresse und überlebt damit das Drehen des Geräts.
 * Ohne das meldete sich die App bei jeder Drehung neu an.
 */
class MainActivity : ComponentActivity() {

    private val sitzung: Sitzung by viewModels()

    /**
     * Die Runde hängt ebenfalls an der Activity und nicht an der Oberfläche.
     *
     * <b>Sie muss ein Drehen des Geräts überleben.</b> Andernfalls würde die
     * Live-Verbindung bei jeder Drehung abgebaut und neu aufgebaut — mit einem
     * `Leave` und einem `Join` dazwischen, und wer im Dienst dreht, stünde
     * plötzlich ohne Platz da.
     */
    private val runde: Runde by viewModels()
    private val sozial: de.pagerspass.pagerspass.mobil.Sozial by viewModels()

    /**
     * Der mobile Begleiter — aus demselben Grund an der Activity wie die Runde.
     *
     * <b>Und aus einem zweiten:</b> Er ist ein Melder. Ein Melder, dessen
     * Verbindung beim Drehen des Geräts oder beim Wechsel in eine andere App
     * abrisse, wäre einer, der genau dann schweigt, wenn man ihn hinlegt.
     */
    private val begleiter: de.pagerspass.pagerspass.mobil.Begleiter by viewModels()

    /**
     * PagerSpass - World — an der Activity aus demselben Grund wie die Runde:
     * Takt und Funkverbindung sollen ein Drehen des Geräts überleben.
     */
    private val welt: de.pagerspass.pagerspass.mobil.Welt by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Der Melder braucht seinen Kanal, bevor der erste Alarm kommt — und er
        // darf mehrfach angelegt werden: Android behält die Einstellungen.
        Meldermeldung.kanalAnlegen(this)
        // Der Mitteilungsabruf — Einladungen und Verwaltungspost, alle
        // Viertelstunde, plus die Tageserinnerung um zwölf.
        de.pagerspass.pagerspass.mobil.Mitteilungsabruf.einrichten(this)

        val dunkel = SystemBarStyle.dark(Farben.Bg.toArgb())
        enableEdgeToEdge(statusBarStyle = dunkel, navigationBarStyle = dunkel)
        super.onCreate(savedInstanceState)

        setContent {
            PagerSpassTheme {
                PagerSpassApp(
                    sitzung = sitzung,
                    runde = runde,
                    sozial = sozial,
                    begleiter = begleiter,
                    welt = welt,
                )
            }
        }
    }
}
