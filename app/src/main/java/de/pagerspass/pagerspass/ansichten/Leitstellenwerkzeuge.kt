package de.pagerspass.pagerspass.ansichten

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ansichten.welt.Regler
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.melder.Melderkatalog
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.mobil.Tonpegel
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.QrCode
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.launch

/**
 * Die zwei Werkzeuge im Fach des Leitstellenkopfs, die keinen eigenen Dialog der
 * Leitstelle hatten: der Ton (`Tonregler.vue`) und der Handy-Funkbegleiter
 * (`BegleiterKnopf.vue`, `BegleiterDialog.vue`).
 */

/**
 * Was man **während** einer Schicht am Ton anfasst: lauter, leiser, still. Die Wahl des
 * Alarmtons steht nicht hier, sondern beim Melder — sie steht unten als Auskunft, damit
 * niemand sie hier vergeblich sucht.
 *
 * Bewusst weggelassen gegenüber dem Web: Umgebung, Leitstellenhintergrund, Durchsage
 * und DMO-Mithören — die App spielt keine dieser Spuren.
 */
@Composable
fun Tonblende(raum: Raumzustand, eigeneKennung: String, befehle: Raumbefehle, beiZu: () -> Unit) {
    val zusammenhang = LocalContext.current
    val geraet = remember { Meldergeraet.bereit(zusammenhang) }
    remember { Tonpegel.laden(zusammenhang) }
    val ich = raum.players.firstOrNull { it.id == eigeneKennung }

    Blende(titel = "Toneinstellungen", beiSchliessen = beiZu, augenbraue = "Ton") {
        Schalterzeile(
            titel = "Alles außer dem Melder stumm",
            unterzeile = "Der Melder klingelt trotzdem.",
            an = Tonpegel.stummAusserMelder,
            beiWechsel = { Tonpegel.stummSetzen(zusammenhang, it) },
        )

        Etikett("Lautstärke")
        Pegelzeile("Melder", geraet.lautstaerke) { geraet.lautstaerkeSetzen(it) }
        Pegelzeile("Funk", Tonpegel.funk, "Stimmen und Gerätetöne des Funkgeräts.") {
            Tonpegel.funkSetzen(zusammenhang, it)
        }

        Schalterzeile(
            titel = "Einzelrufe von Fahrzeugen annehmen",
            unterzeile = "Der Ruhe-Schalter des eigenen Platzes — die Leitstelle kommt immer durch.",
            an = ich?.einzelrufZulassen != false,
            beiWechsel = { befehle.einzelrufZulassen(it) },
        )

        val tonname = Melderkatalog.TOENE.firstOrNull { it.id == geraet.ton }?.name ?: "Eigener Ton"
        SehrLeise("Alarmton: $tonname\nZu ändern beim Melder.")
    }
}

@Composable
private fun Pegelzeile(name: String, wert: Float, zusatz: String? = null, beiAenderung: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(name, style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
        Text("${(wert * 100).toInt()} %", style = Schrift.MonoKlein, color = Farben.Amber)
    }
    zusatz?.let { SehrLeise(it) }
    Regler((wert * 100).toInt(), 0..100, 5, { beiAenderung(it / 100f) })
}

/**
 * Der Handy-Funkbegleiter: ein zweites Handy als Funkgerät und Melder dieses Platzes.
 *
 * <b>Gezeigt immer, gesperrt ohne Abo</b> — wie `BegleiterKnopf.vue`. Ohne Premium
 * sagt die Blende, was dahinterliegt, und führt auf die Webseite; gekauft wird dort.
 */
@Composable
fun Begleiterblende(
    raum: Raumzustand,
    server: String,
    premium: Boolean,
    befehle: Raumbefehle,
    neben: Raumneben,
    beiZu: () -> Unit,
) {
    val zusammenhang = LocalContext.current
    val uri = LocalUriHandler.current
    val ablage = LocalClipboardManager.current
    val bereich = rememberCoroutineScope()
    var link by remember { mutableStateOf<String?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var kopiert by remember { mutableStateOf(false) }

    if (premium) {
        LaunchedEffect(raum.code) {
            befehle.begleiterNachfragen()
            befehle.funkbegleiterLink(raum.code, server)
                .onSuccess { link = it }
                .onFailure { fehler = it.message ?: "Der QR-Code konnte nicht erzeugt werden." }
        }
    }

    Blende(titel = "Handy-Funkbegleiter", beiSchliessen = beiZu, augenbraue = "Premium") {
        if (!premium) {
            Text(
                "Ein zweites Handy als Funkgerät und Melder deines Platzes — es piepst, wenn " +
                    "alarmiert wird, und du funkst darüber. Der Funkbegleiter gehört zu Premium.",
                style = Schrift.Normal,
                color = Farben.TextLeise,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Premium im Web", {
                    bereich.launch { runCatching { uri.openUri("${server.trimEnd('/')}/play/mobile/shop?bereich=premium") } }
                }, art = Knopfart.Haupt, kompakt = true)
            }
            return@Blende
        }

        Text(
            "Mit dem Handy scannen: Funkgerät, Funkchat und Melder öffnen sich direkt — ohne " +
                "erneute Anmeldung. Der Zugang gilt nur für deinen Platz in dieser Runde.",
            style = Schrift.Normal,
            color = Farben.TextLeise,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val l = link
        when {
            fehler != null -> Text(fehler.orEmpty(), style = Schrift.MonoKlein, color = Farben.SignalHell)
            l == null -> SehrLeise("QR-Code wird erzeugt …")
            else -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    QrCode(l, groesse = 220.dp, beschreibung = "QR-Code für den Funkbegleiter")
                }
                Text(
                    if (neben.begleiterGekoppelt) "Handy gekoppelt ✓" else "Warte auf die Kopplung …",
                    style = Schrift.MonoKlein,
                    color = if (neben.begleiterGekoppelt) Farben.Gruen else Farben.TextSehrLeise,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        if (kopiert) "Link kopiert ✓" else "Direkten Link kopieren",
                        {
                            ablage.setText(AnnotatedString(l))
                            kopiert = true
                        },
                        art = Knopfart.Haupt,
                        modifier = Modifier.weight(1f),
                    )
                    Knopf(
                        "Teilen",
                        {
                            val senden = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, l)
                            }
                            runCatching {
                                zusammenhang.startActivity(
                                    Intent.createChooser(senden, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
