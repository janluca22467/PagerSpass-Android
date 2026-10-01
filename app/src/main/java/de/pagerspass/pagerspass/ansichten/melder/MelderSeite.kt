package de.pagerspass.pagerspass.ansichten.melder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ansichten.welt.Regler
import de.pagerspass.pagerspass.melder.Melderkatalog
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.melder.Melderspieler
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay

/** Die Probemeldung — dieselbe Art Meldung, die das Gerätemenü im Web als Probealarm schickt. */
internal val PROBEALARM = Alarmmeldung(
    incidentId = "probe",
    schleife = "PROBE",
    stichwort = "B2",
    stichwortText = "Zimmerbrand",
    meldebild = "Rauch aus Fenster im 1. OG",
    adresse = "Hauptstraße 12",
    ortsteil = "Mitte",
    einheiten = listOf("HLF 1", "DLK 1", "ELW 1"),
)

/**
 * „Dein Melder" — die Melder-Gruppe aus `ProfilBearbeiten.vue`.
 *
 * <b>Bauform, Gehäuse und Alarmton für dieses Gerät.</b> Alles hier gilt sofort
 * und nur auf diesem Handy — mit der einen Ausnahme des Gesichts, das am Konto
 * hängt und deshalb an den Server geht. Die Seite sagt das auch so, damit
 * niemand am Rechner seinen Handyton sucht.
 *
 * <b>Die Werkstätten stehen hier und nicht in eigenen Wegen der Leiste.</b> Man
 * öffnet sie von der Wahl aus und kehrt dorthin zurück; sie liegen über der
 * Seite und gehen mit Zurück wieder zu.
 */
@Composable
fun MelderSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    profil: Bereichsstand<Profil?> = Bereichsstand(),
    server: String = "",
    /** Die Artikel-Kennungen aus dem Besitzstand (`ton-…`, `melder-…`) — `null`: noch nicht geladen. */
    gekauft: Set<String>? = null,
    laeuft: Boolean = false,
    beiLaden: () -> Unit = {},
    beiGesicht: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    val zusammenhang = LocalContext.current
    val browser = LocalUriHandler.current
    val geraet = remember { Meldergeraet.bereit(zusammenhang) }

    LaunchedEffect(Unit) { beiLaden() }

    val stufe = konto?.level ?: 1
    val premium = konto?.premiumAktiv == true

    // Was das Konto zulässt — sobald es bekannt ist, wird die Wahl dagegen geprüft.
    LaunchedEffect(konto?.level, konto?.premiumAktiv, gekauft) {
        if (konto != null) geraet.kontoMerken(stufe, premium, gekauft)
    }
    LaunchedEffect(profil.inhalt?.melderGesicht) {
        profil.inhalt?.melderGesicht?.let { geraet.gesichtMerken(it) }
    }

    // Wer die Seite verlässt, will keinen Probeton im Ohr behalten.
    DisposableEffect(Unit) { onDispose { Melderspieler.stoppen("probe") } }

    var werkstatt by remember { mutableStateOf<String?>(null) }
    var bearbeiteterMelder by remember { mutableStateOf<String?>(null) }

    if (werkstatt != null) {
        BackHandler { werkstatt = null }
        when (werkstatt) {
            "ton" -> Tonwerkstatt(modifier, unterrand, beiZurueck = { werkstatt = null })
            "klang" -> Klangwerkstatt(modifier, unterrand, beiZurueck = { werkstatt = null })
            "gehaeuse" -> Gehaeusewerkstatt(
                modifier, unterrand,
                melderId = bearbeiteterMelder,
                gesicht = geraet.gesicht,
                beiZurueck = { werkstatt = null; bearbeiteterMelder = null },
            )
        }
        return
    }

    var probe by remember { mutableStateOf(false) }
    LaunchedEffect(probe) {
        if (probe) {
            delay(4_000)
            probe = false
        }
    }

    val premiumweg = { browser.openUri("${server.trimEnd('/')}/play/mobile/shop?bereich=premium") }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Dein Melder",
            unterzeile = "Bauform, Gehäuse und Alarmton für dieses Gerät",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )
        Marke("Gilt sofort · nur dieses Gerät", farbe = Farben.AmberHell)

        // ------------------------------------------------------ Vorschau
        val bauform = geraet.wirksameBauform()
        val plan = geraet.eigenerPlan().takeIf { bauform.startsWith("eigen:") }
        // Das Gerät, bedienbar wie im Dienst: Tasten, Gerätemenü (Premium),
        // Speicher — hier lernt man es kennen, bevor der erste Alarm kommt.
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            val quer = Melderkatalog.bauform(bauform)?.quer == true || (plan != null && plan.breite > plan.hoehe)
            Melderschacht(
                alarm = if (probe) PROBEALARM else null,
                bauform = bauform,
                gesicht = geraet.gesicht,
                plan = plan,
                kennung = "PROBE",
                beiQuittieren = { probe = false; Melderspieler.stoppen("probe"); Melderspieler.vibrationAus() },
                hoechstbreite = if (quer) 380.dp else 230.dp,
            )
        }
        if (premium) {
            SehrLeise("Die Tasten am Gerät sind echt: Die mittlere öffnet das Gerätemenü — Alarmierung, Profile, Lautstärke, Ton, Meldungen.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                if (probe) "Probealarm läuft …" else "Probealarm",
                {
                    probe = true
                    val art = Melderkatalog.alarmierungsart(geraet.alarmierungsart)
                    if (art.ton) Melderspieler.probe(zusammenhang, geraet.ton, 2)
                },
                kompakt = true,
                aktiv = !probe,
            )
        }

        // ------------------------------------------------------ Bauart
        Abschnitt("Bauart") {
            SehrLeise("Piepser in der Tasche, Alarm-App über dem ganzen Schirm — oder gar kein eigener Melder.")
            Segment(
                seiten = listOf("dme", "app", "funk"),
                gewaehlt = geraet.bauart,
                beiWahl = { geraet.bauartSetzen(it) },
                aufschrift = { when (it) { "dme" -> "DME"; "app" -> "App"; else -> "Im Funk" } },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ------------------------------------------------------ Bauform
        Abschnitt("Melder-Bauform") {
            SehrLeise("Welches Gerät in deiner Tasche steckt — es bleibt stehen, bis du es änderst.")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                // Die selbst gebauten zuerst: Wer sich einen Melder gebaut hat,
                // sucht seinen und nicht den siebten aus dem Katalog.
                geraet.eigeneMelder.forEach { m ->
                    Bauformkachel(
                        name = "★ ${m.name}",
                        text = "Selbst gebaut",
                        gewaehlt = geraet.bauform == "eigen:${m.id}",
                        frei = premium,
                        sperre = "Premium",
                        beiWahl = { geraet.bauformSetzen("eigen:${m.id}") },
                    ) {
                        Melderbild("eigen", geraet.gesicht, null, plan = m.plan, modifier = Modifier.heightIn(max = 96.dp))
                    }
                }
                Melderkatalog.BAUFORMEN.forEach { b ->
                    val frei = b.frei(stufe, premium)
                    Bauformkachel(
                        name = (if (b.premium) "★ " else "") + b.name,
                        text = b.erklaerung,
                        gewaehlt = geraet.bauform == b.id,
                        frei = frei,
                        sperre = if (b.premium) "Premium" else "ab St. ${b.abLevel}",
                        beiWahl = { geraet.bauformSetzen(b.id) },
                    ) {
                        Melderbild(b.id, geraet.gesicht, null, modifier = Modifier.heightIn(max = 96.dp))
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Knopf(
                    if (geraet.eigeneMelder.isEmpty()) "★ Eigenen Melder bauen" else "★ Gehäusewerkstatt",
                    { if (premium) { bearbeiteterMelder = null; werkstatt = "gehaeuse" } else premiumweg() },
                    kompakt = true,
                )
            }
            SehrLeise(
                if (premium) {
                    "Gehäuse, Anzeigefeld, Tasten und Leuchten selbst zusammensetzen — der Bauplan bleibt auf diesem Gerät."
                } else {
                    "Die Gehäusewerkstatt gehört zu Premium. Premium schließt du auf der Webseite ab."
                },
            )
            if (premium && geraet.eigeneMelder.isNotEmpty()) {
                Pillenreihe {
                    geraet.eigeneMelder.forEach { m ->
                        Pille("✎ ${m.name}", an = false, beiDruck = { bearbeiteterMelder = m.id; werkstatt = "gehaeuse" })
                    }
                }
            }
        }

        // ------------------------------------------------------ Gesicht
        Abschnitt("Melder-Gesicht") {
            SehrLeise(
                "Gehäuse und Display deines Melders. Die eine Zeile dieser Gruppe, die zum Konto " +
                    "gehört: Sie gilt überall. Reine Zierde, kein Spielvorteil.",
            )
            val gewaehlt = profil.inhalt?.melderGesicht?.ifBlank { null } ?: geraet.gesicht
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                Melderkatalog.GESICHTER.forEach { g ->
                    val frei = g.id == gewaehlt || g.frei(stufe, premium, gekauft.orEmpty())
                    Gesichtkachel(g, gewaehlt == g.id, frei, aktiv = !laeuft) {
                        geraet.gesichtMerken(g.id)
                        beiGesicht(g.id)
                    }
                }
            }
        }

        // ------------------------------------------------------ Alarmton
        Abschnitt("Alarmton") {
            SehrLeise("Antippen wählt den Ton und spielt ihn einmal vor.")
            val waehlen = { id: String ->
                geraet.tonSetzen(id)
                Melderspieler.stoppen("probe")
                Melderspieler.probe(zusammenhang, id, 1)
            }
            Pillenreihe {
                geraet.eigeneToene.forEach { t ->
                    Pille("★ ${t.name}", an = geraet.ton == "eigen:${t.id}", beiDruck = { waehlen("eigen:${t.id}") }, aktiv = premium)
                }
                geraet.eigeneKlaenge.forEach { k ->
                    Pille("♪ ${k.name}", an = geraet.ton == "klang:${k.id}", beiDruck = { waehlen("klang:${k.id}") }, aktiv = premium)
                }
                Melderkatalog.TOENE.forEach { t ->
                    val frei = t.id == geraet.ton || t.frei(stufe, premium, gekauft.orEmpty())
                    Pille(
                        aufschrift = if (frei) t.name else "${t.name} · ${t.sperrgrund()}",
                        an = geraet.ton == t.id,
                        beiDruck = { waehlen(t.id) },
                        aktiv = frei,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("★ Tonwerkstatt", { if (premium) werkstatt = "ton" else premiumweg() }, kompakt = true)
                Knopf("★ Eigene Klänge", { if (premium) werkstatt = "klang" else premiumweg() }, kompakt = true)
            }
            if (!premium) {
                SehrLeise("Eigene Töne und Klänge gehören zu Premium. Premium schließt du auf der Webseite ab.")
            }
        }

        // ------------------------------------------------------ Alarmierung
        Abschnitt("Alarmierung") {
            SehrLeise(
                "Wie dein Melder einen Alarm ankündigt. Die einzige Einstellung hier mit Folgen " +
                    "im Dienst: Wer auf Vibration oder stumm steht, hört den Alarm nicht.",
            )
            Pillenreihe {
                Melderkatalog.ALARMIERUNGSARTEN.forEach { a ->
                    Pille(a.name, an = geraet.alarmierungsart == a.id, beiDruck = { geraet.alarmierungsartSetzen(a.id) })
                }
            }
            SehrLeise(Melderkatalog.alarmierungsart(geraet.alarmierungsart).was)
        }

        Abschnitt("Profil") {
            SehrLeise("Alarmierungsart und Lautstärke unter einem Namen.")
            Pillenreihe {
                Melderkatalog.PROFILE.forEach { p ->
                    Pille(p.name, an = geraet.profil == p.id, beiDruck = { geraet.profilWaehlen(p) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lautstärke", style = Schrift.Klein, color = Farben.Text, modifier = Modifier.weight(1f))
                Text("${(geraet.lautstaerke * 100).toInt()} %", style = Schrift.MonoKlein, color = Farben.Amber)
            }
            Regler(
                (geraet.lautstaerke * 100).toInt(), 0..100, 5,
                { geraet.lautstaerkeSetzen(it / 100f) },
            )
            SehrLeise("Der Melder spielt auf dem Alarmkanal — die Alarm-Lautstärke des Handys gilt obendrauf.")
        }
    }
}

/** Eine Bauform als Kachel: das Gerät klein, Name, Satz, und warum es gesperrt ist. */
@Composable
private fun Bauformkachel(
    name: String,
    text: String,
    gewaehlt: Boolean,
    frei: Boolean,
    sperre: String,
    beiWahl: () -> Unit,
    bild: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(150.dp)
            .flaeche(
                farbe = if (gewaehlt) Farben.HauchAmber else Farben.Flaeche,
                randfarbe = if (gewaehlt) Farben.Amber else Farben.Rand,
            )
            .clickable(enabled = frei, role = Role.RadioButton, onClick = beiWahl)
            .padding(Abstand.Klein),
    ) {
        Box(Modifier.height(100.dp), contentAlignment = Alignment.Center) { bild() }
        Text(
            name,
            style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
            color = if (frei) Farben.Text else Farben.TextSehrLeise,
            maxLines = 1,
        )
        Text(text, style = Schrift.Winzig, color = Farben.TextSehrLeise, maxLines = 2)
        if (!frei) Text(sperre, style = Schrift.MonoKlein, color = Farben.Amber)
    }
}

/**
 * Ein Gesicht als kleines Gerät — `.melderprobe` aus `styles/melder.css`: Kopf
 * mit Gitter und Leuchte, Glas, drei Tasten. Ein farbiges Rechteck zeigte, welche
 * Farbe man wählt; das hier zeigt, wie das Stück damit aussieht.
 */
@Composable
private fun Gesichtkachel(g: Melderkatalog.Gesicht, gewaehlt: Boolean, frei: Boolean, aktiv: Boolean, beiWahl: () -> Unit) {
    val p = Melderkatalog.palette(g.id)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .width(76.dp)
            .border(if (gewaehlt) 2.dp else 1.dp, if (gewaehlt) Farben.Amber else Farben.Rand, Rundung.Klein)
            .clickable(enabled = frei && aktiv, role = Role.RadioButton, onClick = beiWahl)
            .padding(Abstand.Winzig),
    ) {
        Canvas(Modifier.size(width = 40.dp, height = 56.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(p.gehMittel, cornerRadius = CornerRadius(w * 0.18f))
            drawRoundRect(p.gehRand, cornerRadius = CornerRadius(w * 0.18f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            for (i in 0 until 5) drawCircle(Color.Black.copy(alpha = 0.5f), w * 0.03f, Offset(w * (0.2f + i * 0.1f), h * 0.1f))
            drawCircle(Color(0xFFFF4A3D), w * 0.05f, Offset(w * 0.8f, h * 0.1f))
            drawRoundRect(p.lcdGrund, Offset(w * 0.12f, h * 0.2f), Size(w * 0.76f, h * 0.45f), CornerRadius(w * 0.04f))
            for (i in 0 until 3) drawRect(p.lcdTinte.copy(alpha = 0.7f), Offset(w * 0.18f, h * (0.27f + i * 0.11f)), Size(w * (0.6f - i * 0.12f), h * 0.04f))
            for (i in 0 until 3) drawRoundRect(p.gehTief, Offset(w * (0.12f + i * 0.27f), h * 0.74f), Size(w * 0.22f, h * 0.12f), CornerRadius(w * 0.06f))
        }
        Text(g.name, style = Schrift.Winzig, color = if (frei) Farben.Text else Farben.TextSehrLeise, maxLines = 1)
        if (!frei) Text(g.sperrgrund(), style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.Amber, maxLines = 1)
    }
}
