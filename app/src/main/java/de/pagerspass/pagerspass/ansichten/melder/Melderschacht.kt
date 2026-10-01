package de.pagerspass.pagerspass.ansichten.melder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.melder.Geraetegeraeusche
import de.pagerspass.pagerspass.melder.Melderbauplan
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.melder.Melderkatalog
import de.pagerspass.pagerspass.melder.Meldermenue
import de.pagerspass.pagerspass.melder.Melderspeicher
import de.pagerspass.pagerspass.melder.Melderspieler
import de.pagerspass.pagerspass.melder.funkzeit
import de.pagerspass.pagerspass.netz.Alarmmeldung
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.karte.Kartenpunkt
import de.pagerspass.pagerspass.ui.karte.Punktkarte
import de.pagerspass.pagerspass.ui.karte.Punktkartenstand
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay

/** Der Takt, in dem eine Meldung Zeile für Zeile einläuft — 170 ms wie im Web. */
private const val EINLAUF_MS = 170L

/**
 * Der Melderschacht — das gewählte Gerät, bedienbar, mit seinem Speicher darunter.
 *
 * Übertragen aus `MelderGeraet.vue` und den zehn Gehäusen darunter, mit dem,
 * was `composables/melderanzeige.ts` und `composables/meldermenue.ts` für alle
 * zusammen führen: <b>eine Meldung, ein Speicher, ein Menü — zehn Gehäuse.</b>
 * Was einem Gerät gehört, ist allein, wo seine Tasten sitzen (siehe
 * [tastenflaechen]) und wie sie heißen.
 *
 * Die Rangfolge an der mittleren Taste ist an jedem Gerät dieselbe, damit
 * niemand beim Wechsel der Bauform umlernen muss: <b>Der Alarm gewinnt, danach
 * das Menü, danach die Ruhe.</b> Ein neuer Alarm schließt das Menü — der Schirm
 * gehört immer genau einer Sache.
 *
 * <b>Das Alarmfax hat keinen Speicher, sondern einen Papierkorb</b>
 * (`AlarmFax.vue`): Es druckt die Meldung Zeile für Zeile, mit Motor und
 * Nadeln, und quittiert wird mit der Hand — man reißt das fertige Blatt ab.
 * Danach liegt es im Korb und ist dort die einzige Stelle, an der man es noch
 * lesen kann.
 *
 * @param alarm Der Alarm, der gerade im Gerät steht — `null` in Ruhe.
 * @param kennung Die Fahrzeugkennung für das Ruhebild.
 * @param ziel Der Einsatzort — der Alarmmonitor zeigt ihn auf der Karte.
 * @param standort Wo das eigene Fahrzeug steht — die Karte des Monitors in Ruhe.
 * @param beiQuittieren `null`, wo das Gerät nur zeigt (die Vorschau ohne Probe).
 * @param beiAusruecken Annehmen und Status 3 in einem — ohne Angabe quittiert es nur.
 */
@Composable
fun Melderschacht(
    alarm: Alarmmeldung?,
    modifier: Modifier = Modifier,
    bauform: String? = null,
    gesicht: String? = null,
    plan: Melderbauplan? = null,
    kennung: String = "",
    ziel: Pair<Double, Double>? = null,
    standort: Pair<Double, Double>? = null,
    beiQuittieren: (() -> Unit)? = null,
    beiAusruecken: (() -> Unit)? = null,
    mitSpeicher: Boolean = true,
    hoechstbreite: Dp? = null,
) {
    val zusammenhang = LocalContext.current
    val geraet = remember { Meldergeraet.bereit(zusammenhang) }
    val speicher = remember { Melderspeicher.bereit(zusammenhang) }

    val form = bauform ?: geraet.wirksameBauform()
    val farbe = gesicht ?: geraet.gesicht
    val eigenplan = plan ?: geraet.eigenerPlan().takeIf { form.startsWith("eigen:") }
    val istFax = form == "fax" && eigenplan == null

    // Die Uhr im Kopf des Displays — ein Gerät, dessen Uhr steht, sieht kaputt aus.
    var uhr by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            uhr = System.currentTimeMillis()
        }
    }

    // --------------------------------------------------- Die Meldung läuft ein
    val volle = remember(alarm, uhr) { Melderanzeige.aus(alarm, laeuft = alarm != null) }
    var sichtbar by remember { mutableIntStateOf(Int.MAX_VALUE) }
    var versatz by remember { mutableIntStateOf(0) }
    var speicherOffen by remember { mutableStateOf(false) }
    var geholt by remember { mutableStateOf<Alarmmeldung?>(null) }

    LaunchedEffect(alarm?.incidentId) {
        versatz = 0
        if (alarm == null) {
            sichtbar = Int.MAX_VALUE
            Geraetegeraeusche.faxDruckenStoppen()
            return@LaunchedEffect
        }
        // Ein neuer Alarm schließt das Menü.
        Meldermenue.schliessen()
        speicherOffen = false
        sichtbar = 0
        if (istFax) Geraetegeraeusche.faxDruckenStarten()
        val gesamt = volle.zeilen.size
        while (sichtbar < gesamt) {
            delay(EINLAUF_MS)
            sichtbar++
        }
        // Der Motor folgt dem Druckzustand, nicht dem Alarm.
        if (istFax) Geraetegeraeusche.faxDruckenStoppen()
    }
    DisposableEffect(Unit) { onDispose { Geraetegeraeusche.faxDruckenStoppen() } }

    val laeuftEin = alarm != null && sichtbar < volle.zeilen.size
    val vergangene = speicher.vergangene(alarm?.incidentId)
    val menue = Meldermenue.bild()
    val darfMenue = Meldermenue.darfMenue

    // ------------------------------------------------------ Das Displaybild
    val anzeige = when {
        menue != null -> volle.copy(
            kopf = "MENÜ · ${Meldermenue.kopfwort}",
            stichwort = menue.titel,
            alarm = false,
            menue = menue,
        )
        alarm != null -> volle.copy(zeilen = volle.zeilen.take(sichtbar).drop(versatz))
        Meldermenue.geraetAus -> volle.copy(stichwort = "AUS", zeilen = listOf("GERÄT AUS", "Kein Ton", ""), ort = "Gerät aus")
        else -> volle.copy(
            zeilen = listOfNotNull(
                kennung.ifBlank { "----" },
                if (speicher.meldungen.isEmpty()) "Keine Meldungen" else "${speicher.meldungen.size} Meldungen",
                if (darfMenue) Meldermenue.kopfwort else "BETRIEBSBEREIT",
            ),
        )
    }.copy(tasten = tastenwoerter(form, alarm != null, menue != null, darfMenue))

    // ------------------------------------------------------------ Tasten
    fun quittieren() {
        if (alarm == null) return
        beiQuittieren?.invoke()
    }

    fun blaettern(schritt: Int) {
        Geraetegeraeusche.taste()
        if (alarm == null) return
        val gesamt = minOf(sichtbar, volle.zeilen.size)
        versatz = (versatz + schritt).coerceIn(0, (gesamt - 1).coerceAtLeast(0))
    }

    fun mitte() {
        when {
            alarm != null -> quittieren()
            Meldermenue.offen -> Meldermenue.waehlen(zusammenhang)
            darfMenue -> Meldermenue.oeffnen()
            else -> Geraetegeraeusche.taste()
        }
    }

    val hatMenuetaste = eigenplan?.teile?.any { it.aufgabe == "menue" } == true

    val beiTaste: (String) -> Unit = { aufgabe ->
        when (aufgabe) {
            "mitte" -> mitte()
            "quittieren" -> when {
                alarm != null -> quittieren()
                Meldermenue.offen -> Meldermenue.waehlen(zusammenhang)
                // Am selbst gebauten Gerät ist die Quittiertaste vielleicht die
                // einzige — dann muss sie auch das Menü öffnen.
                eigenplan != null && darfMenue && !hatMenuetaste -> Meldermenue.oeffnen()
                else -> Geraetegeraeusche.taste()
            }
            "hoch" -> if (Meldermenue.offen) Meldermenue.bewegen(zusammenhang, -1) else blaettern(-1)
            "runter" -> if (Meldermenue.offen) Meldermenue.bewegen(zusammenhang, 1) else blaettern(1)
            "zurueck" -> when {
                Meldermenue.offen -> Meldermenue.zurueck()
                // Am Bogenmelder heißt die untere Taste in Ruhe „Menü".
                form == "bogen" && darfMenue && alarm == null -> Meldermenue.oeffnen()
                else -> Geraetegeraeusche.taste()
            }
            "vor" -> if (Meldermenue.offen) Meldermenue.waehlen(zusammenhang) else if (darfMenue) Meldermenue.oeffnen() else Geraetegeraeusche.taste()
            "menue" -> if (Meldermenue.offen) { Geraetegeraeusche.taste(); Meldermenue.schliessen() } else if (darfMenue) Meldermenue.oeffnen()
            "speicher" -> {
                Geraetegeraeusche.taste()
                speicherOffen = !speicherOffen
            }
            "ausruecken" -> if (alarm != null) (beiAusruecken ?: beiQuittieren)?.invoke()
            // Der Farbmelder: zwei Softkeys, deren Bedeutung mit der Lage wechselt.
            "links" -> when {
                Meldermenue.offen -> Meldermenue.zurueck()
                alarm != null -> (beiAusruecken ?: beiQuittieren)?.invoke()
                darfMenue -> Meldermenue.oeffnen()
            }
            "rechts" -> when {
                Meldermenue.offen -> Meldermenue.waehlen(zusammenhang)
                alarm != null -> quittieren()
            }
            "lauter" -> Meldermenue.lauter(0.2f)
            "leiser" -> Meldermenue.lauter(-0.2f)
            // Das Fax: Erst ein vollständig ausgegebenes Blatt lässt sich abreißen.
            "abriss" -> if (alarm != null && !laeuftEin) {
                Geraetegeraeusche.faxAbreissen()
                quittieren()
            }
            // „Ton aus" — der stille Weg für den, der erst abstellen und dann
            // entscheiden will. Die Meldung bleibt stehen, bis quittiert ist.
            "stop" -> {
                Geraetegeraeusche.taste()
                Melderspieler.stoppen("alarm")
                Melderspieler.vibrationAus()
            }
            else -> Geraetegeraeusche.taste()
        }
    }

    // ------------------------------------------------------------- Aufbau
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        val quer = Melderkatalog.bauform(form)?.quer == true || (eigenplan != null && eigenplan.breite > eigenplan.hoehe)
        val breite = hoechstbreite ?: if (quer) 420.dp else 260.dp
        val verhaeltnis = seitenverhaeltnis(form, eigenplan)
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = breite)
                .fillMaxWidth()
                .aspectRatio(verhaeltnis),
        ) {
            Melderbild(
                bauform = if (eigenplan != null) "eigen" else form,
                gesicht = farbe,
                alarm = alarm,
                laeuft = alarm != null,
                plan = eigenplan,
                beiQuittieren = beiQuittieren,
                anzeigeVorgabe = anzeige,
                beiTaste = beiTaste,
                modifier = Modifier.fillMaxSize(),
            )
            // Der Alarmmonitor: die echte Karte statt der angedeuteten, genau über
            // dem Feld, das die Zeichnung dafür freihält (47/15, 46 × 36 Hundertstel).
            if (form == "monitor" && eigenplan == null && (ziel != null || standort != null)) {
                val u = maxWidth / 100f
                Monitorkarte(
                    ziel = ziel,
                    standort = standort,
                    modifier = Modifier
                        .offset(x = u * 47f, y = u * 15f)
                        .size(width = u * 46f, height = u * 36f),
                )
            }
        }

        if (!mitSpeicher) return@Column

        // ---------------------------------------------- Speicher / Papierkorb
        val liste = if (istFax) speicher.meldungen.filter { it.incidentId != alarm?.incidentId } else vergangene
        val wort = when {
            istFax -> "Papierkorb"
            form == "monitor" -> "Einsätze der Schicht"
            else -> "Speicher"
        }
        if (istFax && alarm != null) {
            SehrLeise(if (laeuftEin) "Druck läuft …" else "Blatt oben abreißen — das quittiert.", mono = true)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Knopf(
                aufschrift = (if (istFax) "🗑 " else "") + "$wort · ${liste.size}",
                beiDruck = { speicherOffen = !speicherOffen; if (!speicherOffen) geholt = null },
                art = if (speicherOffen) Knopfart.Normal else Knopfart.Leise,
                kompakt = true,
                aktiv = liste.isNotEmpty(),
            )
            if (speicherOffen && liste.isNotEmpty()) {
                Knopf(if (istFax) "Korb leeren" else "Leeren", {
                    speicher.leeren()
                    geholt = null
                    speicherOffen = false
                }, art = Knopfart.Leise, kompakt = true)
            }
        }
        if (speicherOffen && liste.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            ) {
                liste.forEach { a ->
                    val offen = geholt?.incidentId == a.incidentId
                    Speicherzeile(a, offen, istFax) { geholt = if (offen) null else a }
                }
            }
        }
    }
}

/** Die Aufschriften, die mit der Lage wechseln — die Rangfolge der mittleren Taste. */
private fun tastenwoerter(form: String, alarm: Boolean, menue: Boolean, darfMenue: Boolean): Map<String, String> {
    val mitte = when {
        alarm -> "QUITT"
        menue -> "WÄHLEN"
        darfMenue -> "MENÜ"
        else -> "RESET"
    }
    return when (form) {
        "klassik" -> mapOf("mitte" to if (alarm) "QUITTIEREN" else if (menue) "WÄHLEN" else if (darfMenue) "MENÜ" else "")
        "farbe" -> mapOf(
            "links" to when { menue -> "ZURÜCK"; alarm -> "ANNEHMEN"; darfMenue -> "MENÜ"; else -> "" },
            "rechts" to when { menue -> "WÄHLEN"; alarm -> "ABLEHNEN"; else -> "" },
        )
        "bogen" -> mapOf("mitte" to mitte, "zurueck" to if (menue) "ZURÜCK" else if (darfMenue) "MENÜ" else "")
        "lamellen" -> mapOf("mitte" to if (alarm || menue || darfMenue) mitte else "")
        else -> mapOf("mitte" to mitte)
    }
}

/**
 * Die Karte im Alarmmonitor — `NavigationKarte.vue` im Gehäuse.
 *
 * Sie steht still: Ein Wischen über ein Gerät meint die Seite, nicht die
 * Karte. Und sie fragt nicht selbst nach der Einwilligung — dafür ist sie zu
 * klein; ohne Freigabe stehen die Punkte auf dunklem Grund.
 */
@Composable
private fun Monitorkarte(ziel: Pair<Double, Double>?, standort: Pair<Double, Double>?, modifier: Modifier) {
    val mitte = ziel ?: standort ?: return
    val stand = remember(ziel, standort) { Punktkartenstand(mitte.first, mitte.second, 14.0) }
    val punkte = listOfNotNull(
        standort?.let { Kartenpunkt("fahrzeug", it.first, it.second, Farben.Eigenposition) },
        ziel?.let { Kartenpunkt("ziel", it.first, it.second, Farben.Signal, ziel = true) },
    )
    LaunchedEffect(stand.groesse, ziel, standort) {
        if (ziel != null && standort != null) stand.zuschneiden(listOf(ziel, standort), deckel = 15.0)
    }
    Punktkarte(
        punkte = punkte,
        stand = stand,
        modifier = modifier,
        gesten = false,
        frage = false,
        knoepfe = false,
    )
}

/** Eine Meldung im Speicher — antippen klappt sie auf (am Fax: holt das Blatt heraus). */
@Composable
private fun Speicherzeile(a: Alarmmeldung, offen: Boolean, fax: Boolean, beiDruck: () -> Unit) {
    val prio = when {
        a.prioritaet >= 3 -> Farben.SignalHell
        a.prioritaet == 2 -> Farben.Amber
        else -> Farben.GruenHell
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (offen && fax) Color(0xFFF2EEE2) else Farben.Flaeche,
                randfarbe = if (offen) Farben.Amber else Farben.Rand,
                ecke = 9.dp,
            )
            .clickable(role = Role.Button, onClick = beiDruck)
            .heightIn(min = 44.dp)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
    ) {
        val tinte = if (offen && fax) Color(0xFF221F18) else Farben.Text
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text(funkzeit(a.zeit), style = Schrift.MonoKlein, color = if (offen && fax) tinte else Farben.TextSehrLeise)
            Text(a.stichwort, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = if (offen && fax) tinte else prio)
            Text(a.adresse, style = Schrift.Klein, color = tinte, maxLines = 1, modifier = Modifier.weight(1f))
        }
        if (offen) {
            // Dieselben Felder wie auf dem Display — nur die Beschriftung gehört
            // dem Gerät (`bogen` in AlarmFax.vue).
            listOfNotNull(
                "Schleife" to a.schleife,
                "Stichwort" to "${a.stichwort} ${a.stichwortText}".trim(),
                "Adresse" to a.adresse,
                a.ortsteil?.let { "Ortsteil" to it },
                "Meldebild" to a.meldebild,
                "Einheiten" to a.einheiten.joinToString(", "),
                a.zusatztext?.let { "Leitstelle" to it },
            ).filter { it.second.isNotBlank() }.forEach { (etikett, wert) ->
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Text(etikett, style = Schrift.MonoKlein, color = if (fax) tinte.copy(alpha = 0.6f) else Farben.TextSehrLeise, modifier = Modifier.widthIn(min = 84.dp))
                    Text(wert, style = Schrift.Klein, color = tinte, modifier = Modifier.weight(1f))
                }
            }
            if (fax) SehrLeise("Antippen legt das Blatt zurück in den Korb.")
        }
    }
}

/** Der Einsatzort einer Meldung aus dem Raumzustand — die Meldung selbst trägt keine Koordinaten. */
fun einsatzort(raum: de.pagerspass.pagerspass.netz.Raumzustand?, incidentId: String?): Pair<Double, Double>? {
    val e = raum?.incidents?.firstOrNull { it.id == incidentId } ?: return null
    val lat = e.lat ?: return null
    val lon = e.lon ?: return null
    return lat to lon
}

/** Wo das eigene Fahrzeug steht — für die Karte des Monitors in Ruhe. */
fun fahrzeugort(raum: de.pagerspass.pagerspass.netz.Raumzustand?, spielerKennung: String): Pair<Double, Double>? {
    val f = raum?.vehicles?.firstOrNull { it.playerId == spielerKennung } ?: return null
    val lat = f.lat ?: f.wacheLat ?: return null
    val lon = f.lon ?: f.wacheLon ?: return null
    return lat to lon
}
