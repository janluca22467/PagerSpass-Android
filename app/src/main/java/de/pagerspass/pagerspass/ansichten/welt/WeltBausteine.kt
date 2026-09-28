package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import de.pagerspass.pagerspass.netz.Ablage
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/*
 * Die kleinen Bausteine, die alle Seiten der Welt teilen — Zeichen, Währung,
 * Bereichsreiter, „Alle wählen", Organisationsfarben.
 */

/**
 * Die Bereiche des Arbeitsplatzes — `Werkzeug` in `components/welt/werkzeuge.ts`.
 *
 * @param kurz Die Kurzform für die Reiterleiste: Fünf Reiter teilen sich 390 Punkte, und
 *   die Aufschrift bricht nicht um.
 */
enum class Werkzeug(val id: String, val titel: String, val kurz: String, val zeichen: ImageVector) {
    Lagen("lagen", "Lagen", "Lagen", Weltzeichen.Lagen),
    Fahrzeuge("fahrzeuge", "Fahrzeuge", "Fahrzeuge", Weltzeichen.Fahrzeuge),
    Wachen("wachen", "Wachen", "Wachen", Weltzeichen.Wachen),
    Grosslage("grosslage", "Großeinsatz", "Großeinsatz", Weltzeichen.Grosslage),
    Kasse("kasse", "Kasse", "Kasse", Weltzeichen.Kasse),
    Rangliste("rangliste", "Rangliste", "Rangliste", Weltzeichen.Rangliste),
    Bauen("bauen", "Wache bauen", "Bauen", Weltzeichen.Bauen),
    Laufbahn("laufbahn", "Laufbahn", "Laufbahn", Weltzeichen.Laufbahn),
    Fahrzeugkauf("fahrzeugkauf", "Fahrzeug kaufen", "Kaufen", Weltzeichen.Fahrzeugkauf),
    Einstellungen("einstellungen", "Einstellungen", "Einstellungen", Weltzeichen.Einstellungen),
    Leihe("leihe", "Verleih", "Verleih", Weltzeichen.Leihe),
    Chat("chat", "Chat", "Chat", Weltzeichen.Chat),
    Wachenseite("wachenseite", "Wache", "Wache", Weltzeichen.Wachenseite),
    Mehr("mehr", "Mehr", "Mehr", Weltzeichen.Mehr),
    ;

    companion object {
        /** Die fünf Reiter am Handy. */
        val HANDY_REITER = listOf(Lagen, Fahrzeuge, Wachen, Bauen, Mehr)

        /** Was hinter „Mehr" steht — in dieser Reihenfolge. */
        val HINTER_MEHR = listOf(Grosslage, Chat, Kasse, Rangliste, Laufbahn, Leihe, Einstellungen)

        fun von(id: String?): Werkzeug? = entries.firstOrNull { it.id == id }
    }
}

/** Die Zeichen der Welt — Pfaddaten zeichengleich aus `werkzeuge.ts`. */
object Weltzeichen {
    /** Das Warndreieck — dieselbe Aussage wie die Lagenmarke auf der Karte. */
    val Lagen = strich("lagen", "M12 4 21.5 20h-19Z", "M12 10v4.5", "M12 17.4h.01")

    /** Kofferaufbau, Führerhaus, zwei Räder. */
    val Fahrzeuge = strich(
        "fahrzeuge",
        "M3 5h11v10H3Z",
        "M14 8.5h3.4l3.6 3.5V15h-3.5",
        "M6.8 15a1.9 1.9 0 1 1-.01 0Z",
        "M17.5 15a1.9 1.9 0 1 1-.01 0Z",
    )

    /** Das Haus mit Tor — die Wache, wie sie auch auf der Karte steht. */
    val Wachen = strich("wachen", "m3 11 9-7 9 7", "M5 10v10h14V10", "M10 20v-5h4v5")

    /** Der Kreis mit Ausrufezeichen: das eine Ereignis, das alle angeht. */
    val Grosslage = strich("grosslage", "M12 3a9 9 0 1 1-.01 0Z", "M12 8v5", "M12 16.6h.01")

    /** Die Raute der Welt-Credits, im aufgeschlagenen Blatt. */
    val Kasse = strich("kasse", "M4 4h16v16H4Z", "m12 8 3.5 4-3.5 4-3.5-4Z")

    /** Das Siegertreppchen. */
    val Rangliste = strich("rangliste", "M9 19V5h6v14", "M3 19v-9h6v9", "M15 19v-6h6v6", "M2.5 19h19")

    /** Zwei Pfeile gegeneinander — hin und zurück. */
    val Leihe = strich("leihe", "M3 9h14", "m14 6 3 3-3 3", "M21 15H7", "m10 18-3-3 3-3")

    val Einstellungen = strich(
        "einstellungen",
        "M12 3.5a8.5 8.5 0 1 1-.01 0Z",
        "M12 8.5a3.5 3.5 0 1 1-.01 0Z",
        "M12 2.5v2",
        "M12 19.5v2",
        "M2.5 12h2",
        "M19.5 12h2",
    )

    val Chat = strich("chat", "M3.5 4.5h17v11H10l-4.5 4v-4H3.5Z", "M7 8.5h10", "M7 11.5h6")

    val Bauen = strich("bauen", "M12 5v14", "M5 12h14")

    /** Aufbau, zwei Räder und das Plus oben rechts: „eines dazu". */
    val Fahrzeugkauf = strich(
        "fahrzeugkauf",
        "M2.5 17.5h11v-5h2.5l2 2v3h-1.5",
        "M5 17.5a1.5 1.5 0 1 0 3 0 1.5 1.5 0 1 0-3 0",
        "M13 17.5a1.5 1.5 0 1 0 3 0 1.5 1.5 0 1 0-3 0",
        "M18.5 5v6",
        "M15.5 8h6",
    )

    /** Die Treppe — Stufe um Stufe. */
    val Laufbahn = strich("laufbahn", "M3 19.5h5.5v-5H14v-5h5.5v-5", "M3 19.5h18")

    /** Drei Punkte in einer Reihe — das übliche Zeichen für „und noch etwas". */
    val Mehr = strich("mehr", "M5 12h.01", "M12 12h.01", "M19 12h.01", staerke = 2.6f)

    val Wachenseite = strich("wachenseite", "m3 11 9-7 9 7", "M5 10v10h14V10", "M12 13.5a2.5 2.5 0 1 1-.01 0Z")

    val Zurueck = strich("zurueck", "M19 12H5", "m11 6-6 6 6 6")

    val Weiter = strich("weiter", "m9 6 6 6-6 6")

    val Schliessen = strich("schliessen", "m6 6 12 12", "M18 6 6 18")

    val Haken = strich("haken", "m5 12.5 5 5L19.5 7")

    /** Die Raute der Welt-Credits — gefüllt. */
    val Waehrung: ImageVector = ImageVector.Builder(
        name = "waehrung",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(pathData = addPathNodes("M12 3.5 20.5 12 12 20.5 3.5 12Z"), fill = SolidColor(Color.White))
    }.build()

    /** Der Winkel nach oben — Erfahrung. */
    val Erfahrung = strich("erfahrung", "M4.5 16.5 12 8l7.5 8.5", staerke = 2.2f)
}

private fun strich(name: String, vararg pfade: String, staerke: Float = 1.8f): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        pfade.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = staerke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

// ------------------------------------------------------------------- Währung

/**
 * Ein Betrag in Welt-Credits: die Raute und die Zahl mit Tausenderpunkt.
 *
 * Die Raute und nicht „Cr" oder „€": Welt-Credits sind kein Geld und keine
 * Spiel-Credits des Kontos — sie haben ihr eigenes Zeichen, damit man beides nicht
 * verwechselt.
 */
@Composable
fun Credits(
    betrag: Int,
    modifier: Modifier = Modifier,
    stil: TextStyle = Schrift.MonoKlein,
    farbe: Color = Farben.Text,
    zeichengroesse: Dp = 12.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Weltzeichen.Waehrung,
            contentDescription = null,
            tint = Farben.Amber,
            modifier = Modifier.size(zeichengroesse),
        )
        Text(text = zahl(betrag), style = stil, color = farbe)
    }
}

// ------------------------------------------------------------ Bereichsreiter

/**
 * Die Reiter der Ausrückebereiche — „Alle" und je Bereich einer.
 *
 * <b>Ein Zustand für alle drei Blenden.</b> Lagen, Fahrzeuge und die Fahrzeugauswahl
 * spiegeln dieselbe Wahl (`gewaehlterBereich`). Solange es nur einen Bereich gibt, steht
 * hier nichts: Eine Reiterreihe mit einem Reiter ist keine.
 */
@Composable
fun Bereichsreiter(stand: Weltzustand, beiWahl: (String?) -> Unit, modifier: Modifier = Modifier) {
    if (!stand.mehrereBereiche) return

    val reiter = listOf<Pair<String?, String>>(null to "Alle") + stand.bereiche.map { it.name to it.name }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        reiter.forEach { (wert, text) ->
            val an = stand.gewaehlterBereich == wert
            Text(
                text = text,
                style = Schrift.Klein.copy(fontWeight = if (an) FontWeight.SemiBold else FontWeight.Normal),
                color = if (an) Farben.Amber else Farben.TextLeise,
                modifier = Modifier
                    .defaultMinSize(minHeight = 36.dp)
                    .background(if (an) Farben.HauchAmber else Color.Transparent, Rundung.Rund)
                    .border(1.dp, if (an) Farben.Amber else Farben.Rand, Rundung.Rund)
                    .clickable { beiWahl(wert) }
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            )
        }
    }
}

/**
 * „Alle wählen" — erst ab zwei Fahrzeugen: Bei einem wäre er ein zweiter Haken direkt
 * daneben.
 */
@Composable
fun AlleWaehlen(ids: List<String>, angehakt: Set<String>, beiSetzen: (Set<String>) -> Unit) {
    if (ids.size <= 1) return
    val alle = ids.all { it in angehakt }
    Knopf(
        aufschrift = if (alle) "Auswahl aufheben" else "Alle wählen (${ids.size})",
        beiDruck = {
            beiSetzen(if (alle) angehakt - ids.toSet() else angehakt + ids)
        },
        art = Knopfart.Leise,
        kompakt = true,
    )
}

// ---------------------------------------------------------------- Farben

/** Die Organisationsfarben — dieselben Werte wie `--org-*` in base.css. */
fun organisationsfarbe(organisation: String?): Color = when (organisation) {
    "Feuerwehr" -> Farben.OrgFeuerwehr
    "Rettungsdienst" -> Farben.OrgRettungsdienst
    "Thw" -> Farben.OrgThw
    "Polizei" -> Farben.OrgPolizei
    else -> Farben.Amber
}

/** Wie eine Organisation heißt, wenn ein Mensch sie liest. */
fun organisationsname(organisation: String): String = when (organisation) {
    "Thw" -> "THW"
    else -> organisation
}

/** Ein Satz des Servers an einem Knopf — rot, klein, direkt darunter. */
@Composable
fun Absage(text: String?, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    Text(text = text, style = Schrift.MonoKlein, color = Farben.SignalHell, modifier = modifier)
}

/** Eine gute Nachricht an einem Knopf — grün, klein. */
@Composable
fun Zusage(text: String?, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    Text(text = text, style = Schrift.Klein, color = Farben.GruenHell, modifier = modifier)
}

/** Die Farbe der FMS-Plakette — dieselbe wie im Fahrzeuge-Reiter, damit „2" hier und dort dasselbe heißt. */
fun fmsFarbe(status: Int): Color = when (status) {
    1 -> Farben.FmsSprechwunsch
    2 -> Farben.FmsFrei
    3 -> Farben.FmsAnfahrt
    4 -> Farben.FmsVorOrt
    6 -> Farben.FmsGebunden
    else -> Farben.RandHell
}

/** Die FMS-Plakette: eine Zahl auf ihrer Farbe. */
@Composable
fun Fmsplakette(status: Int, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .background(fmsFarbe(status), Rundung.Winzig),
    ) {
        Text(
            text = status.toString(),
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
            color = Farben.AufFarbe,
        )
    }
}

/**
 * Eine kleine Marke mit Rand — Fähigkeit, Bereich, Hinweis. Dieselbe Bauform wie `.marke`
 * im Web.
 */
@Composable
fun Weltmarke(
    text: String,
    farbe: Color = Farben.TextLeise,
    modifier: Modifier = Modifier,
    fuellung: Color? = null,
) {
    Text(
        text = text,
        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
        color = farbe,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .then(if (fuellung != null) Modifier.background(fuellung, Rundung.Rund) else Modifier)
            .border(1.dp, farbe, Rundung.Rund)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
    )
}

/** Die Adresse des Servers — für Bilder, die er unter einem Pfad ausliefert. */
@Composable
fun serveradresse(): String? {
    val zusammenhang = LocalContext.current
    val adresse by produceState<String?>(null) { value = Ablage(zusammenhang).server() }
    return adresse
}

/**
 * Eine Zeile zum Anhaken — ein Fahrzeug in einer Auswahl. Der Haken links, dahinter was
 * dazu zu sagen ist; die ganze Zeile nimmt den Tipp.
 */
@Composable
fun Hakenkarte(
    an: Boolean,
    beiWechsel: () -> Unit,
    modifier: Modifier = Modifier,
    vorn: @Composable (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(if (an) Farben.HauchAmber else Farben.FlaecheHoch, Rundung.Klein)
            .border(1.dp, if (an) Farben.AmberTief else Farben.Rand, Rundung.Klein)
            .clickable(onClick = beiWechsel)
            .padding(Abstand.Klein),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .background(if (an) Farben.Amber else Color.Transparent, Rundung.Winzig)
                .border(1.dp, if (an) Farben.Amber else Farben.RandHell, Rundung.Winzig),
        ) {
            if (an) {
                Icon(
                    imageVector = Weltzeichen.Haken,
                    contentDescription = null,
                    tint = Farben.AufAmber,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        vorn?.invoke()
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
            content = inhalt,
        )
    }
}

/** Eine Überschrift über einer Gruppe — Titel links, Zahl rechts. */
@Composable
fun Gruppenkopf(titel: String, zahl: Int? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = Abstand.Klein),
    ) {
        Text(
            text = titel.uppercase(),
            style = Schrift.Klein.copy(fontWeight = FontWeight.Bold),
            color = Farben.TextLeise,
            modifier = Modifier.weight(1f),
        )
        if (zahl != null && zahl > 0) Weltmarke(zahl.toString())
    }
}

/** Eine Möglichkeit in einem Auswahlfeld — mit Unterzeile und der Auskunft, ob sie geht. */
data class Wahl<T>(
    val wert: T,
    val text: String,
    val unter: String? = null,
    val aktiv: Boolean = true,
)

/**
 * Das Auswahlfeld — `<select>` im Web: ein Feld mit dem Gewählten, ein Tipp öffnet die
 * Liste. Eine Zeile, die nicht geht, steht blass da und nimmt keinen Tipp — dieselbe
 * Auskunft wie ein `disabled`-Eintrag.
 */
@Composable
fun <T> Auswahlfeld(
    platzhalter: String,
    wahlen: List<Wahl<T>>,
    gewaehlt: T?,
    beiWahl: (T) -> Unit,
    modifier: Modifier = Modifier,
    titel: String = platzhalter,
    aktiv: Boolean = true,
) {
    var offen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val jetzt = wahlen.firstOrNull { it.wert == gewaehlt }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .background(Farben.BgTief, Rundung.Klein)
            .border(1.dp, if (aktiv) Farben.RandHell else Farben.Rand, Rundung.Klein)
            .clickable(enabled = aktiv) { offen = true }
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Text(
            text = jetzt?.text ?: platzhalter,
            style = Schrift.Klein,
            color = if (jetzt == null || !aktiv) Farben.TextSehrLeise else Farben.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Weltzeichen.Weiter,
            contentDescription = null,
            tint = Farben.TextSehrLeise,
            modifier = Modifier.size(16.dp),
        )
    }

    if (offen) {
        de.pagerspass.pagerspass.ui.bausteine.Blende(titel = titel, beiSchliessen = { offen = false }) {
            if (wahlen.isEmpty()) {
                Text(text = "Nichts zur Wahl.", style = Schrift.Klein, color = Farben.TextSehrLeise)
            }
            wahlen.forEach { w ->
                val an = w.wert == gewaehlt
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp)
                        .background(if (an) Farben.HauchAmber else Farben.FlaecheHoch, Rundung.Klein)
                        .border(1.dp, if (an) Farben.AmberTief else Farben.Rand, Rundung.Klein)
                        .clickable(enabled = w.aktiv) {
                            beiWahl(w.wert)
                            offen = false
                        }
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Text(
                        text = w.text,
                        style = Schrift.Klein,
                        color = if (w.aktiv) Farben.Text else Farben.TextSehrLeise,
                    )
                    w.unter?.let { Text(text = it, style = Schrift.Winzig, color = Farben.TextSehrLeise) }
                }
            }
        }
    }
}

/** Ein Ton aus `#rrggbb` — `null`, wenn er sich nicht lesen lässt. */
fun farbeAus(text: String?): Color? = text?.let {
    runCatching { Color(android.graphics.Color.parseColor(it.trim())) }.getOrNull()
}
