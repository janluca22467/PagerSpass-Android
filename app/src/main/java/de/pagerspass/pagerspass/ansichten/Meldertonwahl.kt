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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Eigentoene
import de.pagerspass.pagerspass.mobil.Einsprung
import de.pagerspass.pagerspass.mobil.rememberAblage
import de.pagerspass.pagerspass.mobil.rememberMelderTon
import de.pagerspass.pagerspass.mobil.rememberTonprobe
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.schmuck.Melderkatalog
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import kotlinx.coroutines.launch

/**
 * Die Wahl des Alarmtons im Melder-Reiter — übertragen aus
 * `web/src/components/ui/Meldertonwahl.vue`.
 *
 * <b>Drei Sorten Ton in einem Regal.</b> Vorn die gezeichneten aus der Tonwerkstatt,
 * dann die eingespielten aus der Klangwerkstatt, dann der Katalog. Die Reihenfolge ist
 * die Antwort auf die Frage, mit der man diese Leiste aufmacht: „Wo ist meiner?" Und es
 * ist <em>ein</em> Regal und nicht zwei Listen — dieselbe Entscheidung („womit geht
 * mein Melder los?"), an einem Ort.
 *
 * <b>Gewählt heißt gehört:</b> Wer einen Ton antippt, bekommt ihn sofort vorgespielt.
 * Er gilt sofort und nur auf diesem Gerät ([de.pagerspass.pagerspass.netz.Ablage]).
 *
 * <b>Zeigen, sperren, erklären.</b> Ohne Abo öffnen die beiden Werkstattknöpfe nicht
 * die Werkstatt, sondern den Sperrdialog — wer nie sieht, dass es sie gibt, vermisst
 * sie auch nicht.
 *
 * @param katalogFrei Ob ein Katalogton gewählt werden darf — Stufe, Kaufliste, Abo.
 * @param katalogSperre Woran er hängt, für die Aufschrift der gesperrten Pille.
 */
@Composable
fun Meldertonwahl(
    premium: Boolean,
    katalogFrei: (Melderkatalog.Ton) -> Boolean,
    katalogSperre: (Melderkatalog.Ton) -> String,
) {
    val zusammenhang = LocalContext.current
    remember(zusammenhang) { Eigentoene.sicherstellen(zusammenhang) }
    val ablage = rememberAblage()
    val bereich = rememberCoroutineScope()
    val ton by rememberMelderTon()
    val probe = rememberTonprobe()

    var tonwerkstatt by remember { mutableStateOf(false) }
    var werbung by remember { mutableStateOf(false) }
    var klangwerkstatt by remember { mutableStateOf(false) }
    var klangwerbung by remember { mutableStateOf(false) }

    val eigeneToene = Eigentoene.toene
    val eigeneKlaenge = Eigentoene.klaenge

    fun waehlen(art: String) {
        bereich.launch { ablage.melderTonSetzen(art) }
        probe.spielen(art)
    }

    /** Der Weg in die Tonwerkstatt — oder, ohne Abo, in den Sperrdialog. */
    fun tonwerkstattOeffnen() {
        probe.beenden()
        if (premium) tonwerkstatt = true else werbung = true
    }

    /**
     * Und in die Klangwerkstatt. Zwei Werkbänke und nicht eine mit zwei Reitern: In der
     * einen <em>zeichnet</em> man ein Muster, in der anderen <em>bringt</em> man einen Klang mit.
     */
    fun klangwerkstattOeffnen() {
        probe.beenden()
        if (premium) klangwerkstatt = true else klangwerbung = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
        Klangkarte(
            satz = if (eigeneKlaenge.isNotEmpty()) {
                "${eigeneKlaenge.size} von ${Eigentoene.KLANG_HOECHSTZAHL} Klängen eingespielt — neue aufnehmen " +
                    "oder bearbeiten."
            } else {
                "Tondatei hochladen oder selbst einsprechen — die Sirene vom Gerätehaus, dein echter Melder, " +
                    "deine Stimme."
            },
            beiDruck = ::klangwerkstattOeffnen,
        )

        Etikett("Alarmton")
        SehrLeise(
            "Womit dein Melder losgeht. Angetippt wird er sofort vorgespielt. Gilt für dieses Gerät und bleibt " +
                "stehen, bis du ihn änderst — im Dienst lässt er sich nicht mehr umstellen.",
        )
        Pillenreihe {
            // Die eigenen zuerst — das Einzige in diesem Regal, was man selbst hergestellt hat.
            eigeneToene.forEach { t ->
                val art = "eigen:${t.id}"
                EigenPille(
                    name = t.name,
                    an = ton == art,
                    frei = premium,
                    beiWahl = { waehlen(art) },
                    beiWeg = {
                        // Ohne Rückfrage: Der Verlust ist ein Raster, das man in einer Minute neu klickt.
                        probe.beenden()
                        Eigentoene.tonLoeschen(t.id)
                    },
                )
            }
            // Dann die eingespielten — ohne Wegwerf-Knopf: Eine Aufnahme wird in der
            // Klangwerkstatt weggeworfen, wo die Rückfrage steht.
            eigeneKlaenge.forEach { k ->
                val art = "klang:${k.id}"
                Pille(
                    aufschrift = "★ ${k.name}",
                    an = ton == art,
                    beiDruck = { waehlen(art) },
                    aktiv = premium,
                )
            }
            Melderkatalog.TOENE.forEach { t ->
                val frei = katalogFrei(t)
                Pille(
                    aufschrift = (if (t.premium) "★ " else "") + t.name +
                        if (!frei && !t.premium) " · ${katalogSperre(t)}" else "",
                    an = ton == t.id,
                    beiDruck = { waehlen(t.id) },
                    aktiv = frei,
                )
            }
        }

        Pillenreihe {
            val laeuft = probe.laeuft != null
            Knopf(
                if (laeuft) "Probe beenden" else "Ton anhören",
                beiDruck = { if (laeuft) probe.beenden() else probe.spielen(ton) },
                art = if (laeuft) Knopfart.Haupt else Knopfart.Normal,
                kompakt = true,
            )
            // Der Weg in den Baukasten — ohne Abo in Gold, mit Stern.
            Knopf(
                (if (premium) "" else "★ ") + if (eigeneToene.isNotEmpty()) "Tonwerkstatt" else "Ton zeichnen",
                beiDruck = ::tonwerkstattOeffnen,
                kompakt = true,
            )
        }
    }

    if (tonwerkstatt) Tonwerkstatt(premium = premium, beiSchliessen = { tonwerkstatt = false })

    if (klangwerkstatt) Klangwerkstatt(premium = premium, beiSchliessen = { klangwerkstatt = false })

    if (werbung) {
        Abosperre(
            titel = "Ein Ton, den sonst niemand hat",
            satz = "In der Tonwerkstatt klickst du dir deinen Melderton selbst ins Raster, statt aus " +
                "siebenundvierzig fertigen zu wählen — sechzehn Schritte, acht Höhen, Pausen dazwischen.",
            punkte = listOf(
                "Du siehst die Figur, bevor du sie hörst — und beim Hören läuft ein Zeiger mit.",
                "Bis zu zwölf eigene Töne mit Namen, jederzeit umschaltbar und wegwerfbar.",
                "Jeder trägt einen Code aus sechzehn Zeichen — gib ihn weiter, und ein anderer hat denselben Ton.",
            ),
            beiSchliessen = { werbung = false },
            beiPremium = {
                werbung = false
                Einsprung.oeffnen(Ladenbereich.Premium.weg)
            },
        )
    }

    if (klangwerbung) {
        Abosperre(
            titel = "Dein Melder, deine Stimme",
            satz = "In der Klangwerkstatt lädst du deinen Alarmton als Datei hoch oder sprichst ihn selbst ein — " +
                "die Sirene vom eigenen Gerätehaus, der Melder, den du im Dienst wirklich trägst, oder ein Satz, " +
                "den nur du hörst.",
            punkte = listOf(
                "Datei oder Mikrofon: MP3, WAV, OGG, M4A — oder direkt eingesprochen.",
                "Schneiden, Ruhe dazwischen einstellen, anhören — bis zu acht Klänge nebeneinander.",
                "Alles bleibt auf deinem Gerät. Nichts davon wird hochgeladen, von niemandem gehört.",
            ),
            beiSchliessen = { klangwerbung = false },
            beiPremium = {
                klangwerbung = false
                Einsprung.oeffnen(Ladenbereich.Premium.weg)
            },
        )
    }
}

/**
 * Die Karte zur Klangwerkstatt — über der Leiste und nicht als dritter kleiner Knopf im
 * Fuß: Es ist das Einzige in diesem Block, was kein Katalog bieten kann. Gold, weil Gold
 * „gehört zum Abo" heißt; der Stern steht immer da, auch mit Abo.
 */
@Composable
private fun Klangkarte(satz: String, beiDruck: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Farben.HauchAmber, Color.Transparent)),
                Rundung.Normal,
            )
            .border(1.dp, Farben.Amber.copy(alpha = 0.55f), Rundung.Normal)
            .clickable(onClick = beiDruck, role = Role.Button)
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(Farben.Amber.copy(alpha = 0.18f), CircleShape),
        ) {
            Text("🎙", style = Schrift.Titel)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Eigenen Alarmton aufnehmen",
                    style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                    color = Farben.AmberHell,
                )
                Text(
                    "★ Premium",
                    style = Schrift.Winzig.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Amber,
                )
            }
            Text(satz, style = Schrift.Klein, color = Farben.TextLeise)
        }
        Text("›", style = Schrift.Schlagzeile, color = Farben.Amber)
    }
}

/**
 * Ein eigener Ton — dieselbe Pille, aber als Hülle um zwei Knöpfe: wählen und
 * wegwerfen. Das Kreuz sitzt in der Pille und nicht daneben; eines zwischen zwei
 * Pillen gehörte optisch zu beiden.
 */
@Composable
private fun EigenPille(
    name: String,
    an: Boolean,
    frei: Boolean,
    beiWahl: () -> Unit,
    beiWeg: () -> Unit,
) {
    val farbe = when {
        !frei -> Farben.TextSehrLeise
        an -> Farben.Amber
        else -> Farben.TextLeise
    }
    val rand = when {
        !frei -> Farben.Rand.copy(alpha = 0.6f)
        an -> Farben.Amber
        else -> Farben.Rand
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .defaultMinSize(minHeight = Ziel.Kompakt)
            .background(if (an) Farben.Amber.copy(alpha = 0.10f) else Color.Transparent, Rundung.Rund)
            .border(1.dp, rand, Rundung.Rund),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(minHeight = Ziel.Kompakt)
                .clickable(enabled = frei, onClick = beiWahl, role = Role.Tab)
                .padding(start = Abstand.Normal, end = Abstand.Klein),
        ) {
            Text(
                "★ $name",
                style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                color = farbe,
                maxLines = 1,
            )
        }
        Box(Modifier.width(1.dp).height(Ziel.Kompakt / 2).background(Farben.Rand))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = 36.dp, minHeight = Ziel.Kompakt)
                .clickable(onClick = beiWeg, role = Role.Button)
                .semantics { contentDescription = "$name wegwerfen" },
        ) {
            Text("✕", style = Schrift.Winzig, color = Farben.TextSehrLeise)
        }
    }
}
