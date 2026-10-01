package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Tonstand
import de.pagerspass.pagerspass.netz.Begleitergeraete
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.QrCode
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Zwei Knöpfe des Fahrzeugkopfs aus dem Web v6 (`FahrzeugView.vue`): der Tonregler
 * (`ui/Tonregler.vue`) und der Knopf, der ein zweites Gerät als Funkgerät und Melder
 * dazuholt (`funk/BegleiterKnopf.vue`). Am Handy stehen sie in der zweiten Kopfzeile
 * neben der Melderwahl — „Melder und Ton teilen sich die zweite Zeile".
 */

// ------------------------------------------------------------- Tonregler

/**
 * Der Tonregler: ein Stumm-Knopf, der zeigt, was man wirklich hört, und daneben der
 * Öffner für die Schieber. Was hier bleibt, ist, was man <em>während</em> einer
 * Schicht anfasst — lauter, leiser, still. Die Wahl des Alarmtons steht im Konto.
 */
@Composable
fun DienstTonregler(raum: Raumzustand, eigeneKennung: String, befehle: Raumbefehle) {
    val zusammenhang = LocalContext.current
    remember { Tonstand.laden(zusammenhang) }
    val stumm by Tonstand.stumm.collectAsState()
    var offen by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Kopfzeichen(
            zeichen = if (stumm) ZEICHEN_STUMM else ZEICHEN_TON,
            beschreibung = if (stumm) "Stumm aufheben — außer dem Melder ist alles stumm" else "Alles außer dem Melder stummschalten",
            an = stumm,
            beiDruck = { Tonstand.stummSetzen(zusammenhang, !stumm) },
        )
        Kopfzeichen(ZEICHEN_REGLER, "Toneinstellungen", an = false, hervor = offen, beiDruck = { offen = !offen })
    }

    if (offen) {
        val melder = remember { Meldergeraet.bereit(zusammenhang) }
        val funk by Tonstand.funkPegel.collectAsState()
        val umgebung by Tonstand.umgebungPegel.collectAsState()
        val dmo by Tonstand.dmoMithoeren.collectAsState()
        val einzelrufZulassen = raum.players.firstOrNull { it.id == eigeneKennung }?.einzelrufZulassen ?: true

        Blende(titel = "Toneinstellungen", beiSchliessen = { offen = false }, breite = Dialogbreite.Schmal) {
            Etikett("Lautstärke")
            Reglerzeile("Melder", melder.lautstaerke) { melder.lautstaerkeSetzen(it) }
            // Am Funkregler hängen auch die Zeichen des Geräts — wer das Klingeln
            // leiser will, sucht sonst einen vierten Schieber, den es nicht gibt.
            Reglerzeile("Funk", funk, "Stimmen und Gerätetöne des Funkgeräts.") { Tonstand.funkSetzen(zusammenhang, it) }
            Reglerzeile("Umgebung", umgebung) { Tonstand.umgebungSetzen(zusammenhang, it) }
            Schalterzeile(
                titel = "Einzelrufe von Fahrzeugen annehmen",
                unterzeile = "Der Ruhe-Schalter des eigenen Platzes — die Leitstelle kommt immer durch.",
                an = einzelrufZulassen,
                beiWechsel = { befehle.einzelrufZulassen(it) },
            )
            Schalterzeile(
                titel = "Einsatzstellenfunk (DMO) mithören",
                an = dmo,
                beiWechsel = { Tonstand.dmoSetzen(zusammenhang, it) },
            )
            SehrLeise("Alarmton: ${melder.tonName()}\nZu ändern im Konto unter „Profil → Bearbeiten → Dein Melder“.")
        }
    }
}

@Composable
private fun Reglerzeile(name: String, wert: Float, zusatz: String? = null, beiAenderung: (Float) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(96.dp)) {
            Text(name, style = Schrift.Klein, color = Farben.Text)
            zusatz?.let { Text(it, style = Schrift.Winzig, color = Farben.TextSehrLeise) }
        }
        Slider(
            value = wert,
            onValueChange = beiAenderung,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Farben.Amber,
                activeTrackColor = Farben.Amber,
                inactiveTrackColor = Farben.FlaecheAktiv,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

// ------------------------------------------------------- Funkbegleiter

/**
 * Der Knopf, der ein zweites Handy als Funkgerät und Melder dazuholt.
 *
 * <b>Er steht für alle da</b>, nur in der laufenden Runde: ohne Abo in Gold mit einem
 * Stern, und dann erklärt er, was dahinterliegt, und führt ins Web — gekauft wird
 * nicht in der App.
 */
@Composable
fun BegleiterKopfknopf(raum: Raumzustand, premium: Boolean, befehle: Raumbefehle, server: String) {
    if (!raum.laeuft) return
    var offen by remember { mutableStateOf(false) }

    Box {
        Kopfzeichen(
            zeichen = ZEICHEN_BEGLEITER,
            beschreibung = if (premium) "Handy als Funkgerät und Melder koppeln" else "Handy als Funkgerät und Melder koppeln — gehört zu Premium",
            an = false,
            gold = !premium,
            beiDruck = { offen = true },
        )
        if (!premium) {
            Text(
                "★",
                style = Schrift.Winzig,
                color = Farben.Amber,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-4).dp),
            )
        }
    }

    if (offen) {
        if (premium) BegleiterDialog(raum, befehle, server) { offen = false }
        else BegleiterWerbung(server) { offen = false }
    }
}

@Composable
private fun BegleiterDialog(raum: Raumzustand, befehle: Raumbefehle, server: String, beiSchliessen: () -> Unit) {
    val zusammenhang = LocalContext.current
    val ablage = LocalClipboardManager.current
    val gekoppelt by befehle.begleiterGekoppelt.collectAsState()
    var link by remember { mutableStateOf<String?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var kopiert by remember { mutableStateOf(false) }

    LaunchedEffect(raum.code) {
        befehle.begleiterStandFragen()
        val melder = Meldergeraet.bereit(zusammenhang)
        runCatching {
            befehle.funkbegleiterErzeugen(
                raum.code,
                Begleitergeraete(
                    bauform = melder.bauform,
                    bauart = melder.bauart,
                    melderton = melder.ton,
                    gesicht = melder.gesicht,
                ),
            )
        }.onSuccess { link = "$server/play/mobile/funk/$it" }
            .onFailure { fehler = it.message ?: "Der QR-Code konnte nicht erzeugt werden." }
    }

    Blende(
        titel = "Handy-Funkbegleiter",
        augenbraue = "Premium",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise) },
    ) {
        Text(
            "Mit dem Handy scannen: Funkgerät, Funkchat und Melder öffnen sich direkt — ohne erneute Anmeldung. " +
                "Der Zugang gilt nur für deinen Platz in dieser Runde.",
            style = Schrift.Normal,
            color = Farben.Text,
            textAlign = TextAlign.Center,
        )
        val l = link
        when {
            fehler != null -> Text(fehler.orEmpty(), style = Schrift.MonoKlein, color = Farben.SignalHell)
            l == null -> SehrLeise("QR-Code wird erzeugt …")
            else -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QrCode(l, groesse = 220.dp, beschreibung = "QR-Code für den Funkbegleiter")
                }
                Text(
                    if (gekoppelt) "Handy gekoppelt ✓" else "Warte auf die Kopplung …",
                    style = Schrift.MonoKlein,
                    color = if (gekoppelt) Farben.GruenHell else Farben.TextSehrLeise,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Knopf(if (kopiert) "Link kopiert ✓" else "Direkten Link kopieren", {
                    ablage.setText(AnnotatedString(l))
                    kopiert = true
                }, art = Knopfart.Haupt, modifier = Modifier.fillMaxWidth())
                SehrLeise("Der Link verfällt nach acht Stunden. Teile ihn nicht weiter.")
            }
        }
    }
}

/** Die gesperrte Fassung: erklären, was dahinterliegt, und ins Web führen. */
@Composable
private fun BegleiterWerbung(server: String, beiSchliessen: () -> Unit) {
    val uri = LocalUriHandler.current
    Blende(
        titel = "Dein Handy wird zum Melder",
        augenbraue = "Premium",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Schließen", beiSchliessen, art = Knopfart.Leise)
            Knopf("Premium im Web", { runCatching { uri.openUri("$server/play/mobile/shop?bereich=premium") } }, art = Knopfart.Haupt)
        },
    ) {
        Text(
            "Scanne einen QR-Code, und das Handy neben dir ist Funkgerät und Piepser deines Platzes — " +
                "ohne zweite Anmeldung, ohne zweites Konto.",
            style = Schrift.Normal,
            color = Farben.Text,
        )
        listOf(
            "Der Melder piepst am Handy, auch bei dunklem Bildschirm.",
            "Sprechen und Hören über ein echtes Handfunkgerät — fünf Ausführungen zur Wahl.",
            "Der Funkbereich hier verschwindet dafür und macht der Karte Platz.",
        ).forEach { Text("✓ $it", style = Schrift.Klein, color = Farben.TextLeise) }
    }
}

// --------------------------------------------------------------- Bauteile

/** Ein Zeichenknopf des Kopfs — dieselbe Machart wie Dienstende und Aussteigen. */
@Composable
private fun Kopfzeichen(
    zeichen: ImageVector,
    beschreibung: String,
    an: Boolean,
    hervor: Boolean = false,
    gold: Boolean = false,
    beiDruck: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = Ziel.Normal, minHeight = Ziel.Normal)
            .background(
                when {
                    an -> Farben.HauchSignal
                    gold -> Farben.HauchAmber
                    else -> Farben.FlaecheHoch
                },
                Rundung.Klein,
            )
            .border(
                1.dp,
                when {
                    an -> Farben.SignalHell
                    hervor -> Farben.Amber
                    gold -> Farben.AmberTief
                    else -> Farben.Rand
                },
                Rundung.Klein,
            )
            .clickable(role = Role.Button, onClickLabel = beschreibung, onClick = beiDruck)
            .padding(horizontal = Abstand.Klein),
    ) {
        Icon(
            zeichen,
            contentDescription = beschreibung,
            tint = when {
                gold -> Farben.AmberHell
                an -> Farben.SignalHell
                else -> Farben.Text
            },
            modifier = Modifier.size(18.dp),
        )
    }
}

private val ZEICHEN_TON = fahrzeugzeichen(
    "Ton",
    "M11.5 5 7 9H3.5v6H7l4.5 4Z",
    "M15.2 9.2a4 4 0 0 1 0 5.6",
    "M17.9 6.6a7.8 7.8 0 0 1 0 10.8",
)
private val ZEICHEN_STUMM = fahrzeugzeichen(
    "Stumm",
    "M11.5 5 7 9H3.5v6H7l4.5 4Z",
    "m16.5 9.5 5 5m0-5-5 5",
)
private val ZEICHEN_REGLER = fahrzeugzeichen(
    "Regler",
    "M5 5v5m0 4v5M12 5v9m0 4v1M19 5v2m0 4v8",
    kreis(5f, 12f, 1.9f),
    kreis(12f, 16f, 1.9f),
    kreis(19f, 9f, 1.9f),
)
private val ZEICHEN_BEGLEITER = fahrzeugzeichen(
    "Begleiter",
    rechteck(8f, 3f, 8f, 18f, 2f),
    "M10.5 6h3M11 18.5h2",
    "M6 9.5a4.5 4.5 0 0 0 0 5M3.6 7.6a8 8 0 0 0 0 8.8",
    "M18 9.5a4.5 4.5 0 0 1 0 5M20.4 7.6a8 8 0 0 1 0 8.8",
)
