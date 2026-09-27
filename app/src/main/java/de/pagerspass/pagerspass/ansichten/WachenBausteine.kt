package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.pagerspass.pagerspass.netz.Anwesenheit
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.Wachenrang
import de.pagerspass.pagerspass.netz.Wachenregeln
import de.pagerspass.pagerspass.netz.Wachenrunde
import de.pagerspass.pagerspass.netz.Wachenschatz
import de.pagerspass.pagerspass.netz.Wachenstatistik
import de.pagerspass.pagerspass.netz.Wachentermin
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.schmuck.Schmuck
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Die Bausteine der Wachenseiten — Emblem, Stufe, Tag, Dienstplan, Logbuch,
 * Laufbahn. Übertragen aus `web/src/components/gemeinschaft/*` und
 * `web/src/utils/wachenschmuck.ts`.
 *
 * <b>Hier steht, was mehr als eine Wachenseite braucht</b> — der Kopf der Wache
 * genauso wie der Anpassen-Dialog und die Probe im Laden. Zweimal gepflegt
 * sahen sie im Web schon beim ersten Nachbessern verschieden aus.
 */

// ------------------------------------------------------------------- Wörter

/** Wie eine Rolle heißt, wenn ein Mensch sie liest. */
internal fun wachenrolle(rolle: String?): String = when (rolle) {
    "Zugfuehrer" -> "Zugführer"
    "Leitung" -> "Leitung"
    "Mitglied" -> "Mitglied"
    else -> rolle.orEmpty()
}

/** Die Rollen als Zahl — dieselbe Leiter wie `Gemeinschaftsregeln.Rang` am Server. */
internal fun wachenrang(rolle: String?): Int = when (rolle) {
    "Leitung" -> 2
    "Zugfuehrer" -> 1
    "Mitglied" -> 0
    else -> -1
}

/** Wie man hineinkommt — dieselben Wörter wie `MODUSTEXT` im Web. */
internal fun modustext(modus: String): String = when (modus) {
    "Einladung" -> "Nur auf Einladung"
    "Antrag" -> "Auf Antrag"
    "Offen" -> "Offen für alle"
    else -> modus
}

/**
 * Wo ein Mitglied gerade steckt — Rolle, Ort, Zustand. Wer seine Anwesenheit
 * abgeschaltet hat, kommt ohne sie an; dann steht hier nichts statt einer
 * Vermutung.
 */
internal fun lagezeile(wo: Anwesenheit?): String {
    if (wo == null) return ""
    val rolle = if (wo.rolle == "Leitstelle") "Leitstelle" else (wo.funkrufname ?: "Fahrzeug")
    val zustand = if (wo.zustand == "Lobby") "in der Lobby" else "im Einsatz"
    return "$rolle · ${wo.ort} · $zustand"
}

/** „1 Schicht", „3 Schichten" — ein Wort mit seiner Zahl. */
internal fun anzahl(n: Int, eins: String, viele: String): String = "$n ${if (n == 1) eins else viele}"

// -------------------------------------------------------------------- Zeiten

private val DEUTSCH = Locale.GERMAN

/** Ein Zeitstempel des Servers in der Zone des Geräts — `null`, wenn unlesbar. */
internal fun wachenzeit(roh: String?): ZonedDateTime? {
    if (roh.isNullOrBlank()) return null
    val zone = ZoneId.systemDefault()
    return runCatching { Instant.parse(roh).atZone(zone) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(roh).atZoneSameInstant(zone) }.getOrNull()
}

private fun formatiert(roh: String?, muster: String): String =
    wachenzeit(roh)?.format(DateTimeFormatter.ofPattern(muster, DEUTSCH)) ?: "—"

/** „27.09., 19:30" — im Logbuch zählt der Tag, nicht die Sekunde. */
internal fun tagUndZeit(roh: String?): String = formatiert(roh, "dd.MM., HH:mm")

/** „27.09.26" — das Dienstalter in der Mannschaftsliste. */
internal fun tagKurz(roh: String?): String = formatiert(roh, "dd.MM.yy")

/** „27. September 2026" — „Auf der Wache seit". */
internal fun tagLang(roh: String?): String = formatiert(roh, "d. MMMM yyyy")

/** „27.09." — im Coin-Auszug. */
internal fun tagMonat(roh: String?): String = formatiert(roh, "dd.MM.")

/** „19:30". */
internal fun wachenuhr(roh: String?): String = formatiert(roh, "HH:mm")

/**
 * Der Tagestrenner im Chat: „Heute", „Gestern" oder „Montag, 21.09." — das
 * Jahr nur, wenn es nicht das laufende ist.
 */
internal fun tagestext(wann: ZonedDateTime): String {
    val heute = LocalDate.now(wann.zone)
    val tag = wann.toLocalDate()
    return when (tag) {
        heute -> "Heute"
        heute.minusDays(1) -> "Gestern"
        else -> {
            val muster = if (tag.year == heute.year) "EEEE, dd.MM." else "EEEE, dd.MM.yy"
            wann.format(DateTimeFormatter.ofPattern(muster, DEUTSCH))
        }
    }
}

/**
 * Wie lange es noch dauert, in Worten — „noch 3 Tage". Nicht auf die Minute:
 * Ein Sortiment, das eine Woche steht, braucht keine laufende Uhr.
 */
internal fun auslagenrest(bis: String?): String? {
    val ende = wachenzeit(bis)?.toInstant() ?: return null
    val ms = ende.toEpochMilli() - System.currentTimeMillis()
    if (ms <= 0) return null
    val stunden = ms / 3_600_000
    return when {
        stunden >= 48 -> "noch ${stunden / 24} Tage"
        stunden >= 24 -> "noch 1 Tag"
        stunden >= 2 -> "noch $stunden Stunden"
        stunden >= 1 -> "noch 1 Stunde"
        else -> "weniger als eine Stunde"
    }
}

// ------------------------------------------------------------------ Schmuck

/**
 * Wie „nichts" auf einem Platz heißt — drei verschiedene Wörter, und das ist
 * kein Versehen: So stehen sie in der Datenbank, seit es die Spalten gibt.
 */
private fun leerwort(art: String): String = when (art) {
    "Kopfmuster" -> "keines"
    "Emblemrahmen" -> "keiner"
    "Emblemzeichen" -> "keines"
    else -> ""
}

/** Ob auf diesem Platz gerade etwas getragen wird. */
internal fun traegt(art: String, wert: String?): Boolean = !wert.isNullOrEmpty() && wert != leerwort(art)

/**
 * Der Ton eines Emblem-Rahmens. Den Stahlkranz gibt es nur an der Wache; alle
 * anderen Ringe haben dieselben Töne wie die Profilrahmen.
 */
internal fun rahmenton(id: String?): Color? = when {
    id.isNullOrEmpty() || id == "keiner" -> null
    id == "stahlkranz" -> Color(0xFF8794A6)
    else -> Schmuck.rahmenfarbe(id)
}

/**
 * Der Klarname eines Beinamens. Gespeichert ist die Stück-Id; wie sie heißt,
 * weiß allein der Katalog im Schatz — ohne ihn steht lieber nichts da als eine
 * rohe Id.
 */
internal fun beinameVon(schatz: Wachenschatz?, id: String?): String? {
    if (id.isNullOrEmpty()) return null
    return schatz?.schmuck?.firstOrNull { it.art == "Beiname" && it.stueckId == id }?.name
}

/** Wie der Platz in einer Überschrift heißt. */
internal fun platzname(art: String): String = when (art) {
    "Kopfmuster" -> "Kopfmuster"
    "Emblemrahmen" -> "Emblem-Rahmen"
    "Emblemzeichen" -> "Emblem-Zeichen"
    "Beiname" -> "Beiname"
    "Wachenfarbe" -> "Farbton"
    else -> "Ausbau"
}

/** Der Akzentton einer Gattung — dieselben wie im Konto-Shop. */
internal fun gattungston(art: String): Color = when (art) {
    "Kopfmuster" -> Color(0xFF46C8A0)
    "Emblemrahmen" -> Color(0xFF5F8CFF)
    "Emblemzeichen" -> Color(0xFFD98CF0)
    "Beiname" -> Color(0xFFF0A35F)
    "Wachenfarbe" -> Color(0xFF7FD0E8)
    else -> Farben.Amber
}

/** Der Ton der Wache — dieselbe Herleitung wie beim Kontowappen. */
internal fun wachenton(g: Gemeinschaft): Color = Wappen.ton(g.id, g.wappenfarbe)

/**
 * Das Emblem der Wache — Initialen oder Zeichen im Ton der Wache, mit dem
 * gekauften Ring darum.
 *
 * <b>Eckig und nicht rund.</b> Ein Kreis hieße „eine Person" wie beim
 * Kontowappen; ein Schild mit runden Ecken liest sich als Ort.
 */
@Composable
internal fun Wachenemblem(
    name: String,
    ton: Color,
    zeichen: String?,
    rahmen: String?,
    groesse: Dp = 64.dp,
) {
    val form = RoundedCornerShape(groesse * 0.22f)
    val ring = rahmenton(rahmen)
    val tinte = Wappen.schrift(ton)
    val bild = if (traegt("Emblemzeichen", zeichen)) wachenzeichen(zeichen.orEmpty()) else null

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .border(3.dp, ring ?: Farben.Flaeche, form)
            .padding(3.dp)
            .size(groesse)
            .clip(form)
            .background(ton),
    ) {
        if (bild != null) {
            Icon(
                imageVector = bild,
                contentDescription = null,
                tint = tinte,
                modifier = Modifier.size(groesse * 0.6f),
            )
        } else {
            Text(
                text = Wappen.initialen(name, "W"),
                style = (if (groesse >= 48.dp) Schrift.Titel else Schrift.Klein)
                    .copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.03.em),
                color = tinte,
            )
        }
    }
}

/** Der Wachentag als Plakette — dieselbe Form vor dem Namen der Wache und ihrer Leute. */
@Composable
internal fun Wachentagplakette(tag: String, gross: Boolean = false) {
    Text(
        text = tag,
        style = (if (gross) Schrift.MonoNormal else Schrift.Winzig.copy(fontFamily = Schrift.Mono))
            .copy(fontWeight = FontWeight.Bold),
        color = Farben.AufAmber,
        modifier = Modifier
            .background(Farben.AmberHell, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

// --------------------------------------------------------------- Zeichen

/**
 * Die Zeichen im Wachen-Emblem — gezeichnete Linien, keine Emoji. Die Pfade
 * sind zeichengleich die aus `WachenZeichen.vue`; Kreise und Rechtecke sind als
 * Pfade geschrieben, weil Pfaddaten diese Grundformen nicht kennen.
 */
internal fun wachenzeichen(name: String): ImageVector? = ZEICHEN[name]

private fun kreis(cx: Double, cy: Double, r: Double): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

private fun rechteck(x: Double, y: Double, b: Double, h: Double, r: Double): String =
    "M${x + r} ${y}H${x + b - r}A$r $r 0 0 1 ${x + b} ${y + r}" +
        "V${y + h - r}A$r $r 0 0 1 ${x + b - r} ${y + h}" +
        "H${x + r}A$r $r 0 0 1 $x ${y + h - r}" +
        "V${y + r}A$r $r 0 0 1 ${x + r} ${y}Z"

private fun linien(name: String, vararg pfade: String): ImageVector =
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
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private val ZEICHEN: Map<String, ImageVector> by lazy {
    mapOf(
        "helm" to linien(
            "helm",
            "M5 15a7 7 0 0 1 14 0",
            "M3.5 15h17",
            "M12 8V5",
            "M9 15c0-3 1.3-5 3-5s3 2 3 5",
        ),
        "florianskreuz" to linien(
            "florianskreuz",
            "M12 2.5 14.4 8 20 5.6 17.6 11.2 23 12l-5.4 1.6L20 19l-5.6-2.4L12 22l-2.4-5.4L4 19l2.4-5.8L1 12l5.4-.8L4 5.6 9.6 8Z",
        ),
        "hydrant" to linien(
            "hydrant",
            "M9 9a3 3 0 0 1 6 0v9H9Z",
            "M9.5 6.5h5",
            "M7 11h2M15 11h2",
            "M7.5 20.5h9",
        ),
        "drehleiter" to linien(
            "drehleiter",
            "M4 19h16",
            "m5 16 12-9",
            "m7.5 14 1.8 2.4M10.5 11.8l1.8 2.4M13.5 9.6l1.8 2.4",
            "m3.8 16.4 3-2.2",
            kreis(6.0, 19.0, 1.6),
            kreis(17.0, 19.0, 1.6),
        ),
        "funkmast" to linien(
            "funkmast",
            "M9 21 12 6l3 15",
            "M10 15h4M9.4 18h5.2",
            "M7.5 8a6 6 0 0 1 0-4M16.5 8a6 6 0 0 0 0-4",
            "M5 10a9 9 0 0 1 0-8M19 10a9 9 0 0 0 0-8",
        ),
        "anker" to linien(
            "anker",
            kreis(12.0, 5.0, 2.0),
            "M12 7v13",
            "M8 10h8",
            "M4.5 14a7.5 7.5 0 0 0 15 0",
            "M4.5 14v-2.5M19.5 14v-2.5",
        ),
        "kreuz" to linien(
            "kreuz",
            kreis(12.0, 12.0, 8.5),
            "M12 7.5v9M7.5 12h9",
        ),
        "zahnrad" to linien(
            "zahnrad",
            kreis(12.0, 12.0, 3.2),
            "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3",
            "m5.3 5.3 2.1 2.1M16.6 16.6l2.1 2.1M18.7 5.3l-2.1 2.1M7.4 16.6l-2.1 2.1",
        ),
        "rth" to linien(
            "rth",
            "M4 5.5h16",
            "M12 5.5v2.5",
            "M7 12.5a4.5 4.5 0 0 1 4.5-4.5h1.5c2.5 0 4 1.7 4.5 4l3.5 1.5H7Z",
            "M15 14.2 20.5 18",
            "M19 18h3",
            "M8.5 14v3M14 14v3",
            "M7 17.5h8",
        ),
        "flamme" to linien(
            "flamme",
            "M12 2.5c3.5 4 5.5 6.4 5.5 10a5.5 5.5 0 0 1-11 0c0-2.2 1-3.9 2.5-5.5.4 1.2 1.1 2 2 2.3-.4-2.4.3-4.6 1-6.8Z",
        ),
        "stern" to linien(
            "stern",
            "m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9Z",
        ),
        "leitstelle" to linien(
            "leitstelle",
            rechteck(3.5, 8.5, 17.0, 10.0, 1.5),
            "M8 22h8M12 18.5V22",
            "M7 15h4M7 12h7",
            "M8.5 5.5a6 6 0 0 1 7 0",
            kreis(12.0, 2.8, 1.0),
        ),
        "ehrenzeichen" to linien(
            "ehrenzeichen",
            "M8 2.5 10.5 9M16 2.5 13.5 9",
            kreis(12.0, 15.0, 6.5),
            "m12 11.3 1.4 2.9 3.1.4-2.3 2.2.6 3.1L12 18.4l-2.8 1.5.6-3.1-2.3-2.2 3.1-.4Z",
        ),
    )
}

/** Das Schloss an einer Stufe, die noch kommt. */
internal val SCHLOSS: ImageVector by lazy {
    linien("schloss", "M8 11V8a4 4 0 0 1 8 0v3", rechteck(5.0, 11.0, 14.0, 10.0, 2.0))
}

// ------------------------------------------------------------ Wachenstufe

/**
 * Wie weit die laufende Stufe gefüllt ist — zwischen der Schwelle dieser Stufe
 * und der nächsten, nicht von null aus: Sonst stünde der Balken ab Stufe 10
 * dauerhaft fast am Anschlag.
 */
internal fun stufenanteil(erfahrung: Int, schwelle: Int, bis: Int?): Float {
    if (bis == null) return 1f
    val spanne = erfahrung + bis - schwelle
    if (spanne <= 0) return 1f
    return ((erfahrung - schwelle).toFloat() / spanne).coerceIn(0f, 1f)
}

/** „noch 1 200 bis Stufe 13" — die kurze Form fürs Band. */
internal fun stufenziel(bis: Int?, naechste: Int): String =
    if (bis == null) "höchste Stufe erreicht" else "noch ${zahl(bis)} bis Stufe $naechste"

/**
 * Die Stufe der Wache mit ihrem Fortschritt — übertragen aus `WachenStufe.vue`.
 *
 * Die Zahl allein sagt nichts; daneben steht, was diese Stufe hergibt und was
 * die nächste bringt. Ein Balken ohne Ziel ist Dekoration.
 */
@Composable
internal fun WachenStufe(s: Wachenstatistik) {
    val anteil = maxOf(0.02f, stufenanteil(s.erfahrung, s.schwelle ?: 0, s.bisZurNaechsten))

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Etikett("Stufe ${s.stufe}")
                Text(text = s.bezeichnung, style = Schrift.Gross, color = Farben.Text)
            }
            Text(
                text = buildAnnotatedString {
                    append(zahl(s.erfahrung))
                    withStyle(SpanStyle(color = Farben.TextSehrLeise, fontSize = 13.sp)) {
                        append(" Punkte")
                    }
                },
                style = Schrift.MonoNormal,
                color = Farben.Text,
            )
        }

        Fortschritt(anteil = anteil)

        val bis = s.bisZurNaechsten
        SehrLeise(
            if (bis != null) {
                "Noch ${zahl(bis)} Punkte bis Stufe ${s.stufe + 1}." +
                    (s.naechsteFreischaltung?.let { " $it." } ?: "")
            } else {
                "Höchste Stufe erreicht."
            },
        )

        Text(
            text = buildAnnotatedString {
                append("Auf dieser Stufe: bis zu ${s.maxMitglieder} Mitglieder · ")
                append("Clanrunde alle ${s.clanrundenSperreMinuten} Minuten · ")
                // „geplanter Dienst", nicht „geplante Dienst": das Adjektiv beugt sich mit.
                append("${s.maxTermine} ${if (s.maxTermine == 1) "geplanter Dienst" else "geplante Dienste"} · ")
                append("bis zu ${s.clanrundenCoins} Coins je Clanrunde")
                if (s.stufenstuecke > 0) {
                    append(" · ")
                    // In Amber: das Einzige in dieser Zeile, das man nicht kaufen kann.
                    withStyle(SpanStyle(color = Farben.AmberHell)) {
                        append(anzahl(s.stufenstuecke, "Zierstück", "Zierstücke") + " erspielt")
                    }
                }
            },
            style = Schrift.Klein,
            color = Farben.TextSehrLeise,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Kennwert("Schichten", zahl(s.schichten), Modifier.weight(1f))
            Kennwert("Einsätze", zahl(s.einsaetze), Modifier.weight(1f))
            Kennwert(
                "Ø Hilfsfrist",
                s.hilfsfristSekunden?.let { hilfsfrist(it.roundToInt()) } ?: "—",
                Modifier.weight(1f),
            )
            Kennwert("30 Tage", zahl(s.aktivitaetPunkte), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Kennwert(was: String, wert: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = modifier
            .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Klein),
    ) {
        Text(
            text = was.uppercase(),
            style = Schrift.Etikett,
            color = Farben.TextLeise,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = wert, style = Schrift.MonoNormal, color = Farben.Text, maxLines = 1)
    }
}

// --------------------------------------------------------------- Wachentag

/** Dieselbe Form wie am Server: Großbuchstaben, nur Buchstaben und Ziffern. */
private fun tagSaeubern(roh: String): String =
    roh.uppercase()
        .filter { it in 'A'..'Z' || it in '0'..'9' || it == 'Ä' || it == 'Ö' || it == 'Ü' }
        .take(Wachenregeln.TAG_HOECHSTENS)

/**
 * Der Wachentag — das Kürzel vor jedem Namen der Mannschaft. Übertragen aus
 * `WachenTag.vue`.
 *
 * <b>Drei Zustände, eine Karte.</b> Unter der Stufe ist er ein Ziel mit Balken;
 * ab der Stufe für Leitung und Zugführer ein Feld, für alle anderen eine
 * Auskunft.
 */
@Composable
internal fun WachenTag(
    gemeinschaft: Gemeinschaft,
    stufe: Int,
    darfFuehren: Boolean,
    laeuft: Boolean,
    eigenerName: String,
    premium: Boolean,
    beiSpeichern: (String) -> Unit,
) {
    val aktuell = gemeinschaft.tag
    val erreicht = stufe >= Wachenregeln.TAG_AB_STUFE
    val fehlen = maxOf(0, Wachenregeln.TAG_AB_STUFE - stufe)
    val anteil = (stufe.toFloat() / Wachenregeln.TAG_AB_STUFE).coerceIn(0.04f, 1f)

    var bearbeitet by remember { mutableStateOf(false) }
    var eingabe by remember(aktuell) { mutableStateOf(aktuell) }

    val gueltig = eingabe.isEmpty() || eingabe.length >= Wachenregeln.TAG_MINDESTENS
    val geaendert = eingabe != aktuell
    val vorschau = (if (bearbeitet) eingabe else aktuell).ifEmpty { "TAG" }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche()
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (erreicht) Farben.HauchAmber else Farben.FlaecheHoch,
                        RoundedCornerShape(12.dp),
                    ),
            ) {
                Wachentagplakette(aktuell.ifEmpty { vorschau }, gross = true)
            }
            Column(modifier = Modifier.weight(1f)) {
                Etikett("Wachentag")
                Text(
                    text = when {
                        erreicht && aktuell.isNotEmpty() -> "Eure Wache trägt $aktuell"
                        erreicht -> "Noch kein Tag gesetzt"
                        else -> "Ab Stufe ${Wachenregeln.TAG_AB_STUFE}"
                    },
                    style = Schrift.Gross,
                    color = Farben.Text,
                )
                SehrLeise(
                    if (erreicht) {
                        "Er steht vor dem Namen jedes Mitglieds — im Dienst, in der Lobby, am Brett " +
                            "und in den Kontakten."
                    } else {
                        "Noch ${anzahl(fehlen, "Stufe", "Stufen")}. Dann trägt jedes Mitglied eures " +
                            "Teams das Kürzel eurer Wache vor seinem Namen — überall im Spiel."
                    },
                )
            }
        }

        if (!erreicht) Fortschritt(anteil = anteil)

        // So sieht es aus — am eigenen Namen, denn der ist es, den man zuerst sucht.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SehrLeise("So steht es an dir:")
            Wachentagplakette(vorschau)
            Kontoname(name = eigenerName, premium = premium, stil = Schrift.Klein)
        }

        if (erreicht && darfFuehren) {
            if (bearbeitet) {
                Feld(
                    wert = eingabe,
                    beiAenderung = { eingabe = tagSaeubern(it) },
                    etikett = "${Wachenregeln.TAG_MINDESTENS} bis ${Wachenregeln.TAG_HOECHSTENS} " +
                        "Buchstaben oder Ziffern",
                    platzhalter = "z. B. NORD",
                    fehler = if (!gueltig) "Mindestens ${Wachenregeln.TAG_MINDESTENS} Zeichen." else null,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        "Speichern",
                        {
                            if (gueltig && geaendert) {
                                beiSpeichern(eingabe)
                                bearbeitet = false
                            }
                        },
                        art = Knopfart.Haupt,
                        aktiv = !laeuft && gueltig && geaendert,
                        kompakt = true,
                    )
                    Knopf(
                        "Abbrechen",
                        {
                            eingabe = aktuell
                            bearbeitet = false
                        },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        if (aktuell.isNotEmpty()) "Tag ändern" else "Tag festlegen",
                        {
                            eingabe = aktuell
                            bearbeitet = true
                        },
                        aktiv = !laeuft,
                        kompakt = true,
                    )
                    if (aktuell.isNotEmpty()) {
                        Knopf(
                            "Abnehmen",
                            {
                                beiSpeichern("")
                                bearbeitet = false
                            },
                            art = Knopfart.Leise,
                            aktiv = !laeuft,
                            kompakt = true,
                        )
                    }
                }
            }
        } else if (erreicht) {
            SehrLeise("Festlegen können ihn Leitung und Zugführer.")
        }
    }
}

// -------------------------------------------------------------- Dienstplan

/** Vorbelegung: heute in einer Stunde, auf die volle Viertelstunde aufgerundet. */
private fun frueheste(): ZonedDateTime {
    val bald = ZonedDateTime.now(ZoneId.systemDefault()).plusHours(1).withSecond(0).withNano(0)
    val viertel = ((bald.minute + 14) / 15) * 15
    return bald.withMinute(0).plusMinutes(viertel.toLong())
}

/**
 * Der Dienstplan der Wache — übertragen aus `Dienstplan.vue`.
 *
 * Datum und Uhrzeit kommen aus den Wählern des Systems und werden in der Zone
 * des Geräts gelesen. Hier stand früher ein Freitextfeld mit fest angehängtem
 * `+02:00` — im Winter lag damit jeder Dienst eine Stunde daneben.
 */
@Composable
internal fun Dienstplan(
    termine: List<Wachentermin>,
    ich: String,
    darfPlanen: Boolean,
    maxTermine: Int,
    beiPlanen: (String, Instant) -> Unit,
    beiAntwort: (Long, String) -> Unit,
    beiAbsagen: (Long) -> Unit,
    beiBeitreten: (String) -> Unit,
) {
    val zusammenhang = LocalContext.current
    var formularOffen by remember { mutableStateOf(false) }
    var titel by remember { mutableStateOf("") }
    var datum by remember { mutableStateOf(LocalDate.now()) }
    var zeit by remember { mutableStateOf(LocalTime.of(20, 0)) }

    val planVoll = termine.size >= maxTermine
    val gewaehlt = ZonedDateTime.of(datum, zeit, ZoneId.systemDefault())
    val vorbei = gewaehlt.toInstant().isBefore(Instant.now())

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche()
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        if (termine.isEmpty() && !formularOffen) SehrLeise("Noch nichts geplant.")

        termine.forEach { t ->
            val meine = t.zusagen.firstOrNull { it.kennung == ich }?.antwort
            val dabei = t.zusagen.filter { it.antwort == "Zugesagt" }
            val wann = wachenzeit(t.wann)

            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Die Datumskachel trägt Tag und Monat, die Zeile Wochentag und Uhrzeit.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(min = 48.dp)
                            .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp, mitLichtkante = false)
                            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
                    ) {
                        Text(
                            text = wann?.dayOfMonth?.toString() ?: "—",
                            style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                            color = Farben.Text,
                        )
                        Text(
                            text = wann?.format(DateTimeFormatter.ofPattern("MMM", DEUTSCH)) ?: "",
                            style = Schrift.Winzig,
                            color = Farben.TextLeise,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.titel, style = Schrift.Normal, color = Farben.Text)
                        SehrLeise(
                            (wann?.format(DateTimeFormatter.ofPattern("EEE, HH:mm", DEUTSCH)) ?: "—") +
                                " · von ${t.vonName ?: "—"}",
                            mono = true,
                        )
                    }
                    t.roomCode?.let { code ->
                        Knopf("Beitreten", { beiBeitreten(code) }, art = Knopfart.Haupt, kompakt = true)
                    }
                }

                SehrLeise(
                    if (dabei.isNotEmpty()) {
                        "Dabei: ${dabei.joinToString(", ") { it.anzeigename }}"
                    } else {
                        "Noch niemand zugesagt."
                    },
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                ) {
                    Knopf(
                        "Ich komme",
                        { beiAntwort(t.nr, "Zugesagt") },
                        art = if (meine == "Zugesagt") Knopfart.Haupt else Knopfart.Normal,
                        kompakt = true,
                    )
                    Knopf(
                        "Kann nicht",
                        { beiAntwort(t.nr, "Abgesagt") },
                        art = if (meine == "Abgesagt") Knopfart.Gefahr else Knopfart.Leise,
                        kompakt = true,
                    )
                    if (darfPlanen || t.von == ich) {
                        Knopf("Dienst absagen", { beiAbsagen(t.nr) }, art = Knopfart.Leise, kompakt = true)
                    }
                }
            }
        }

        if (darfPlanen) {
            when {
                formularOffen -> {
                    Feld(
                        wert = titel,
                        beiAenderung = { titel = it.take(120) },
                        etikett = "Wofür?",
                        platzhalter = "Wofür? (z. B. Großschadenslage)",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    ) {
                        Knopf(
                            gewaehlt.format(DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy", DEUTSCH)),
                            {
                                val fenster = android.app.DatePickerDialog(
                                    zusammenhang,
                                    { _, jahr, monat, tag -> datum = LocalDate.of(jahr, monat + 1, tag) },
                                    datum.year,
                                    datum.monthValue - 1,
                                    datum.dayOfMonth,
                                )
                                fenster.datePicker.minDate = System.currentTimeMillis() - 1000
                                fenster.show()
                            },
                            kompakt = true,
                        )
                        Knopf(
                            gewaehlt.format(DateTimeFormatter.ofPattern("HH:mm", DEUTSCH)) + " Uhr",
                            {
                                android.app.TimePickerDialog(
                                    zusammenhang,
                                    { _, stunde, minute -> zeit = LocalTime.of(stunde, minute) },
                                    zeit.hour,
                                    zeit.minute,
                                    true,
                                ).show()
                            },
                            kompakt = true,
                        )
                    }
                    if (vorbei) SehrLeise("Dieser Zeitpunkt liegt in der Vergangenheit.")
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            "Eintragen",
                            {
                                beiPlanen(titel.trim().ifEmpty { "Gemeinsamer Dienst" }, gewaehlt.toInstant())
                                titel = ""
                                formularOffen = false
                            },
                            art = Knopfart.Haupt,
                            aktiv = !vorbei,
                            kompakt = true,
                        )
                        Knopf("Abbrechen", { formularOffen = false }, art = Knopfart.Leise, kompakt = true)
                    }
                }

                planVoll -> SehrLeise(
                    "Auf dieser Stufe stehen höchstens $maxTermine " +
                        "${if (maxTermine == 1) "Dienst" else "Dienste"} gleichzeitig im Plan.",
                )

                else -> Knopf(
                    "Dienst planen",
                    {
                        val start = frueheste()
                        datum = start.toLocalDate()
                        zeit = start.toLocalTime()
                        formularOffen = true
                    },
                    kompakt = true,
                )
            }
        }
    }
}

// ----------------------------------------------------------------- Logbuch

/** Das Logbuch der Clanrunden — die laufende steht grün und lädt zum Dazustoßen. */
@Composable
internal fun Logbuch(runden: List<Wachenrunde>, beiBeitreten: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche()
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        runden.forEach { r ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tagUndZeit(r.gestartetUm) + (r.landkreis?.let { " · $it" } ?: ""),
                            style = Schrift.MonoKlein,
                            color = Farben.Text,
                        )
                        if (r.beendetUm == null) Marke("läuft", farbe = Farben.GruenHell)
                    }
                    SehrLeise(
                        if (r.beendetUm != null) {
                            "${r.teilnehmer} dabei · ${r.einsaetze} Einsätze · ${zahl(r.punkte)} Punkte"
                        } else {
                            "läuft gerade · eröffnet von ${r.vonName ?: "—"}"
                        },
                    )
                }
                if (r.beendetUm == null) {
                    Knopf("Dazustoßen", { beiBeitreten(r.roomCode) }, art = Knopfart.Leise, kompakt = true)
                } else {
                    SehrLeise(r.roomCode, mono = true)
                }
            }
        }
    }
}

// ----------------------------------------------------------------- Laufbahn

/** Was eine Stufe gegenüber der davor freischaltet — `titel` und `stueck` in Amber. */
private data class Gabe(val hervor: Boolean, val text: String)

private fun gaben(r: Wachenrang, davor: Wachenrang?): List<Gabe> = buildList {
    if (davor == null) {
        // Die erste Karte trägt die Ausgangswerte — der Maßstab für alle Unterschiede.
        add(Gabe(false, "Bis zu ${r.maxMitglieder} Mitglieder"))
        add(Gabe(false, "Clanrunde alle ${r.clanrundenSperreMinuten} Minuten"))
        add(Gabe(false, "1 geplanter Dienst"))
        return@buildList
    }
    if (r.bezeichnung != davor.bezeichnung) add(Gabe(true, "Titel „${r.bezeichnung}“"))
    r.belohnungen.forEach { add(Gabe(true, "Zierde „$it“")) }
    if (r.clanrundenSperreMinuten < davor.clanrundenSperreMinuten) {
        add(Gabe(false, "Clanrunde alle ${r.clanrundenSperreMinuten} Minuten"))
    }
    if (r.maxTermine > davor.maxTermine) add(Gabe(false, "${r.maxTermine} geplante Dienste gleichzeitig"))
    if (r.clanrundenCoins > davor.clanrundenCoins) {
        add(Gabe(false, "Bis zu ${r.clanrundenCoins} Coins je Clanrunde"))
    }
    if (r.maxMitglieder > davor.maxMitglieder) add(Gabe(false, "Bis zu ${r.maxMitglieder} Mitglieder"))
}

/**
 * Die Laufbahn der Wache als Band — übertragen aus `WachenLaufbahn.vue`.
 *
 * Beim Öffnen steht die eigene Stufe vorn: Wer auf 12 steht, will sehen, was
 * als Nächstes kommt, nicht elf erledigte Karten abrollen. Beschriftet wird nur
 * der Unterschied zur Stufe davor.
 */
@Composable
internal fun WachenLaufbahn(s: Wachenstatistik, stufen: List<Wachenrang>) {
    if (stufen.isEmpty()) return

    val eigene = stufen.indexOfFirst { it.stufe == s.stufe }.coerceAtLeast(0)
    val band = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, eigene - 1))
    val fortschritt = stufenanteil(s.erfahrung, s.schwelle ?: 0, s.bisZurNaechsten)

    // Kommt die Liste erst nach dem Aufbau, springt das Band einmal zur eigenen Stufe.
    LaunchedEffect(stufen.size) { band.scrollToItem(maxOf(0, eigene - 1)) }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Ueberschrift("Laufbahn der Wache")
        SehrLeise(
            "Stufe ${s.stufe} · ${zahl(s.erfahrung)} Punkte · ${stufenziel(s.bisZurNaechsten, s.stufe + 1)}",
            mono = true,
        )

        LazyRow(
            state = band,
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(stufen.size, key = { stufen[it].stufe }) { i ->
                val r = stufen[i]
                val davor = stufen.getOrNull(i - 1)
                val name = if (davor == null || r.bezeichnung != davor.bezeichnung) r.bezeichnung else ""
                val aktuell = r.stufe == s.stufe
                val erreicht = r.stufe < s.stufe

                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier
                        .width(168.dp)
                        .heightIn(min = 150.dp)
                        .flaeche(
                            farbe = if (aktuell) Farben.FlaecheHoch else Farben.Flaeche,
                            randfarbe = if (aktuell) Farben.Amber else Farben.Rand,
                            ecke = 12.dp,
                        )
                        .padding(Abstand.Normal),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = r.stufe.toString(),
                            style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                            color = if (aktuell) Farben.Amber else Farben.Text,
                            modifier = Modifier.weight(1f),
                        )
                        when {
                            erreicht -> Text("✓", style = Schrift.Klein, color = Farben.GruenHell)
                            r.stufe > s.stufe -> Icon(
                                imageVector = SCHLOSS,
                                contentDescription = "Ab ${zahl(r.ab)} Punkten",
                                tint = Farben.TextSehrLeise,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Text(
                        text = name,
                        style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildAnnotatedString {
                            append("ab ${zahl(r.ab)} P. · ")
                            withStyle(SpanStyle(color = Farben.AmberHell)) { append("+${r.coins} Coins") }
                        },
                        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                        color = Farben.TextSehrLeise,
                    )
                    gaben(r, davor).forEach { g ->
                        Text(
                            text = g.text,
                            style = Schrift.Winzig,
                            color = if (g.hervor) Farben.AmberHell else Farben.TextLeise,
                        )
                    }
                    if (aktuell) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Farben.BgTief, RoundedCornerShape(2.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fortschritt)
                                    .fillMaxHeight()
                                    .background(Farben.Amber, RoundedCornerShape(2.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Kleinteile

/** Eine Zahl, rechtsbündig in fester Breite — Platzziffern fluchten dann. */
@Composable
internal fun Platzziffer(text: String, hervor: Boolean = false) {
    Text(
        text = text,
        style = Schrift.MonoNormal,
        color = if (hervor) Farben.Amber else Farben.TextSehrLeise,
        textAlign = TextAlign.End,
        modifier = Modifier.widthIn(min = 26.dp),
    )
}
