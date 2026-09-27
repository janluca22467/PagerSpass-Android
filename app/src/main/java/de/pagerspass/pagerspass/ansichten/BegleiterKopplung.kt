package de.pagerspass.pagerspass.ansichten

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Karte
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.kamera.Qrleser
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Die Kopplungsseite unter „Konto" — hier wird aus dem Handy ein Melder.
 *
 * <b>Der Weg ist der des Webs, nur andersherum gelesen.</b> Am Rechner erzeugt
 * man in der laufenden Runde über das Handy-Symbol einen QR-Code; er trägt
 * einen Token, der acht Stunden gilt, keine Kontokennung enthält und
 * ausschließlich Funk und Melder **dieses einen Platzes in dieser einen Runde**
 * freigibt. Diese Seite liest ihn.
 *
 * <b>Zwei Wege hinein.</b> Der Sucher ist der Regelfall; daneben steht die
 * Eingabezeile, weil der Rechner auch „Direkten Link kopieren" anbietet — und
 * weil die Kamera abgelehnt sein kann. Ein Bildschirm, der ohne Kameraerlaubnis
 * nur eine Sackgasse zeigt, wäre eine Sackgasse.
 *
 * <b>Die Kameraerlaubnis wird hier erfragt und nirgends sonst.</b> Wer nie
 * koppelt, wird nie gefragt — dieselbe Regel wie beim Mikrofon an der
 * Sprechtaste und beim Melder in der Runde.
 */
@Composable
fun BegleiterKopplung(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    laeuft: Boolean = false,
    fehler: String? = null,
    beiKoppeln: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
    /**
     * Ob das Konto Premium hat. Ohne Abo führt der Weg in den Shop — dieselbe
     * Regel wie am Weg `/scan` im Web (`meta.premium`) und derselbe Grund: Den
     * QR-Code erzeugen kann ohnehin nur ein Konto mit Premium.
     */
    premium: Boolean = true,
    beiShop: () -> Unit = {},
) {
    val zusammenhang = LocalContext.current

    LaunchedEffect(premium) { if (!premium) beiShop() }
    if (!premium) {
        Seite(modifier = modifier, unterrand = unterrand) {
            Seitenkopf(
                titel = "Mobiler Begleiter",
                unterzeile = "Funkgerät und Melder auf diesem Gerät",
                knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
            )
            Karte(
                titel = "Ein Weg mit Premium",
                zeichen = Zeichen.Handy,
                text = "Den Begleiter koppelt ein Konto mit Premium — die Übersicht im Shop " +
                    "erklärt das Abo.",
                knoepfe = { Knopf("Zum Shop", beiShop, art = Knopfart.Haupt) },
            )
        }
        return
    }

    var erlaubt by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                zusammenhang,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var abgelehnt by remember { mutableStateOf(false) }
    var eingabe by remember { mutableStateOf("") }

    val frage = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { gegeben ->
        erlaubt = gegeben
        abgelehnt = !gegeben
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Mobiler Begleiter",
            unterzeile = "Funkgerät und Melder auf diesem Gerät",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        // Der Kopplungsversuch läuft — und er kann ein paar Sekunden dauern
        // (Verbindung aufbauen, Platz übernehmen). Ohne diese Zeile sähe der
        // Sucher aus, als hätte er den Code gar nicht gelesen.
        if (laeuft) Ladezeile("Funkplatz wird gekoppelt …")

        Karte(
            titel = "So kommt der Dienst aufs Handy",
            zeichen = Zeichen.Handy,
            text = "Am Rechner in der laufenden Runde auf das Handy-Symbol tippen — " +
                "dort steht der QR-Code. Sobald er gescannt ist, liegen Funkgerät, " +
                "Funkchat und Melder hier; der Rechner blendet sie aus und behält " +
                "Karte, Tableau und Einsätze.",
        ) {
            SehrLeise(
                "Es wird kein zweites Konto angelegt und keine zweite Runde betreten: " +
                    "Der Code gilt für deinen Platz in dieser einen Runde und läuft nach " +
                    "acht Stunden ab. Erzeugen kann ihn nur ein Konto mit Premium.",
            )
        }

        Abschnitt("QR-Code scannen") {
            when {
                erlaubt -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(Rundung.Normal)
                            .flaeche(),
                    ) {
                        Qrleser(
                            beiFund = beiKoppeln,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Sucherrahmen(Modifier.fillMaxSize())
                    }
                    SehrLeise("Den Code am Bildschirm des Rechners in den Rahmen halten.")
                }

                else -> {
                    Text(
                        text = if (abgelehnt) {
                            "Ohne Kamera geht der Sucher nicht. Du kannst die Erlaubnis in " +
                                "den Einstellungen des Geräts nachreichen — oder unten den " +
                                "kopierten Link einsetzen."
                        } else {
                            "Zum Scannen braucht die App die Kamera. Sie wird für nichts " +
                                "anderes benutzt, und es wird kein Bild gespeichert oder " +
                                "gesendet."
                        },
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                    )
                    Knopf(
                        aufschrift = if (abgelehnt) "Nochmal fragen" else "Kamera erlauben",
                        beiDruck = { frage.launch(Manifest.permission.CAMERA) },
                        art = Knopfart.Haupt,
                    )
                }
            }
        }

        Abschnitt("Oder den Link einsetzen") {
            Leise(
                "Am Rechner steht unter dem QR-Code „Direkten Link kopieren\". Der Link " +
                    "gehört nur dir — teile ihn nicht weiter.",
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Feld(
                    wert = eingabe,
                    beiAenderung = { eingabe = it.trim() },
                    platzhalter = "https://pagerspass.de/play/mobile/funk/…",
                    weiterTaste = ImeAction.Go,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    aufschrift = "Koppeln",
                    beiDruck = { beiKoppeln(eingabe) },
                    aktiv = !laeuft && eingabe.isNotBlank(),
                    kompakt = true,
                )
            }
            if (fehler != null) {
                Text(
                    text = fehler,
                    style = Schrift.MonoKlein,
                    color = Farben.SignalHell,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Der Rahmen im Sucher.
 *
 * Er tut nichts — er sagt nur, wohin. Ein Kamerabild ohne Zielmarke lässt den
 * Code suchen, obwohl er längst im Bild ist; vier Ecken genügen dafür, und ein
 * geschlossener Kasten würde das Bild eher verstellen als führen.
 */
@Composable
private fun Sucherrahmen(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(Abstand.SehrGross)) {
        val laenge = size.minDimension * 0.18f
        val dicke = 3.dp.toPx()

        // Ein Winkel: von der Ecke aus je ein Stück nach innen — waagerecht und
        // senkrecht. `dx`/`dy` sagen, in welche Richtung „innen" liegt.
        fun winkel(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(
                color = Farben.Amber,
                start = Offset(x, y),
                end = Offset(x + dx * laenge, y),
                strokeWidth = dicke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Farben.Amber,
                start = Offset(x, y),
                end = Offset(x, y + dy * laenge),
                strokeWidth = dicke,
                cap = StrokeCap.Round,
            )
        }

        winkel(0f, 0f, 1f, 1f)
        winkel(size.width, 0f, -1f, 1f)
        winkel(0f, size.height, 1f, -1f)
        winkel(size.width, size.height, -1f, -1f)
    }
}
