package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.netz.Anwesenheit
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.lichtkante
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Die gemeinsamen Bausteine des Freundebereichs — Rahmen, Namen, Lagezeile,
 * Meldung und die Zeitangaben, wie sie im Web in `utils/zeit.ts` und
 * `utils/anwesenheit.ts` stehen.
 *
 * <b>Ein Rahmen für vier Seiten.</b> Brett, Freunde, Nachrichten und Kontakte
 * teilen sich Kopf und Reiterreihe (`components/freunde/FreundeSeite.vue`); ohne
 * ihn stünde dasselbe Layout viermal da und liefe beim ersten Nachbessern an
 * einer Stelle auseinander.
 */

/** Die vier Wege des Freundebereichs — dieselben drei Gründe plus die Liste wie im Web. */
enum class Freundereiter(val titel: String) {
    Brett("Brett"),
    Liste("Freunde"),
    Nachrichten("Nachrichten"),
    Kontakte("Kontakte"),
}

/**
 * Der Rahmen der Freunde-Seiten: Ausweis, Reiterreihe, Inhalt.
 *
 * <b>Der Ausweis führt aufs eigene Profil.</b> Er trägt Wappen, Namen und die
 * drei Zahlen, die es nirgends sonst gibt — Freunde, im Dienst, ungelesen. Am
 * Handy war das eigene Profil vorher vom Freundebereich aus gar nicht zu
 * erreichen.
 *
 * <b>Die Marken an den Reitern heißen „hier wartet etwas":</b> Nachrichten
 * zeigt das Ungelesene, Kontakte die Anfragen und Einladungen. „Du hast 14
 * Freunde" wartet nicht — die Zahl steht im Ausweis.
 */
@Composable
fun FreundeRahmen(
    reiter: Freundereiter,
    konto: Konto?,
    meinProfil: Profil?,
    server: String,
    freundeZahl: Int,
    imDienstZahl: Int,
    ungelesen: Int,
    kontaktmarke: Int,
    beiReiter: (Freundereiter) -> Unit,
    beiMeinProfil: () -> Unit,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    /**
     * Ob der Ausweis oben steht. Auf einer Profilseite nicht: Dort trägt der
     * Inhalt selbst einen Kopf mit Wappen und Namen, und auf dem eigenen Profil
     * stünde dieselbe Person sonst zweimal übereinander.
     */
    ausweis: Boolean = true,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Seite(modifier = modifier, unterrand = unterrand) {
        if (ausweis) Ausweis(
            konto = konto,
            meinProfil = meinProfil,
            server = server,
            freundeZahl = freundeZahl,
            imDienstZahl = imDienstZahl,
            ungelesen = ungelesen,
            beiMeinProfil = beiMeinProfil,
        )

        Reiterreihe {
            Freundereiter.entries.forEach { r ->
                Reiter(
                    aufschrift = r.titel,
                    offen = r == reiter,
                    beiDruck = { beiReiter(r) },
                    marke = when (r) {
                        Freundereiter.Nachrichten -> ungelesen
                        Freundereiter.Kontakte -> kontaktmarke
                        else -> 0
                    },
                )
            }
        }

        inhalt()
    }
}

/** Der Streifen mit dem eigenen Wappen — der Weg aufs eigene Profil. */
@Composable
private fun Ausweis(
    konto: Konto?,
    meinProfil: Profil?,
    server: String,
    freundeZahl: Int,
    imDienstZahl: Int,
    ungelesen: Int,
    beiMeinProfil: () -> Unit,
) {
    val kennung = konto?.kennung.orEmpty()
    // Derselbe Ton wie im Wappen daneben — zwei Töne wären zwei Personen.
    val ton = Wappen.ton(kennung, meinProfil?.wappenfarbe ?: 0)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Rundung.Normal)
            .background(Farben.Flaeche)
            .kopfband(meinProfil?.kopfmuster ?: "keines", ton, zeile = true)
            .border(1.dp, Farben.Rand, Rundung.Normal)
            .lichtkante()
            .padding(Abstand.Normal),
    ) {
        if (konto == null) {
            Etikett("Freunde")
            return@Column
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = beiMeinProfil,
                    role = Role.Button,
                    indication = null,
                    interactionSource = null,
                ),
        ) {
            Kontobild(
                kennung = kennung,
                anzeigename = konto.anzeigename,
                wappen = meinProfil?.wappen ?: "Keines",
                wappenfarbe = meinProfil?.wappenfarbe ?: 0,
                bildAdresse = bildweg(server, meinProfil?.profilbild),
                rahmen = meinProfil?.profilrahmen ?: "keiner",
                groesse = 48.dp,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier.weight(1f),
            ) {
                Etikett("Freunde")
                Personenname(
                    name = konto.anzeigename,
                    premium = konto.premiumAktiv,
                    teammitglied = konto.teammitglied,
                    wachentag = meinProfil?.wachentag,
                    stil = Schrift.Titel,
                )
                Text(
                    text = konto.benutzername,
                    style = Schrift.MonoKlein,
                    color = Farben.TextLeise,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(text = "Mein Profil ›", style = Schrift.Klein, color = Farben.TextLeise)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Ausweiswert(freundeZahl, "Freunde", Modifier.weight(1f))
            Ausweiswert(imDienstZahl, "im Dienst", Modifier.weight(1f))
            Ausweiswert(ungelesen, "ungelesen", Modifier.weight(1f), offen = ungelesen > 0)
        }
    }
}

@Composable
private fun Ausweiswert(zahl: Int, was: String, modifier: Modifier, offen: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        modifier = modifier
            .background(Farben.Flaeche.copy(alpha = 0.46f), Rundung.Klein)
            .border(1.dp, Farben.Rand, Rundung.Klein)
            .padding(vertical = Abstand.Klein, horizontal = Abstand.Winzig),
    ) {
        Text(
            text = zahl.toString(),
            style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
            color = if (offen) Farben.SignalHell else Farben.Text,
        )
        Text(text = was, style = Schrift.Winzig, color = Farben.TextLeise, maxLines = 1)
    }
}

/**
 * Ein Name mit Wachentag, Haken und Stern — `components/Kontoname.vue`.
 *
 * Der Tag der Wache steht wie der Haken *vor* dem Namen: Er sagt, woher jemand
 * kommt. `Kontoname` kennt ihn nicht; deshalb steht er hier davor.
 */
@Composable
fun Personenname(
    name: String,
    modifier: Modifier = Modifier,
    premium: Boolean = false,
    teammitglied: Boolean = false,
    wachentag: String? = null,
    stil: TextStyle = Schrift.Normal,
    maxZeilen: Int = 1,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        if (!wachentag.isNullOrBlank()) {
            Text(
                text = wachentag,
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
                color = Farben.OrangeHell,
                maxLines = 1,
                modifier = Modifier
                    .border(1.dp, Farben.OrangeHell.copy(alpha = 0.6f), Rundung.Winzig)
                    .padding(horizontal = Abstand.Winzig),
            )
        }
        Kontoname(
            name = name.ifBlank { "—" },
            premium = premium,
            teammitglied = teammitglied,
            stil = stil,
            maxZeilen = maxZeilen,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/**
 * Was der Server abgelehnt hat — als Streifen über dem Inhalt.
 *
 * Zum Wegräumen ein eigener Knopf und nicht der ganze Streifen: Wer den Text
 * gerade erst liest, tippt beim Weiterlesen leicht darauf — und die Meldung ist
 * weg, bevor er weiß, was sie sagte.
 *
 * @param hinweis Grün für eine Bestätigung statt Signalrot für eine Ablehnung.
 */
@Composable
fun Meldungsstreifen(
    text: String?,
    beiSchliessen: () -> Unit,
    modifier: Modifier = Modifier,
    hinweis: Boolean = false,
) {
    if (text.isNullOrBlank()) return
    val farbe = if (hinweis) Farben.GruenHell else Farben.SignalHell
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (hinweis) Farben.Gruen.copy(alpha = 0.12f) else Farben.SignalTief.copy(alpha = 0.35f),
                Rundung.Klein,
            )
            .border(1.dp, farbe.copy(alpha = 0.7f), Rundung.Klein)
            .padding(start = Abstand.Normal),
    ) {
        Text(
            text = text,
            style = Schrift.MonoKlein,
            color = farbe,
            modifier = Modifier.weight(1f).padding(vertical = Abstand.Klein),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Ziel.Kompakt)
                .clickable(
                    onClick = beiSchliessen,
                    role = Role.Button,
                    indication = null,
                    interactionSource = null,
                ),
        ) {
            Text(text = "×", style = Schrift.Gross, color = farbe, textAlign = TextAlign.Center)
        }
    }
}

/** „Wird geholt …" — solange eine Seite des Bereichs noch nichts zeigen kann. */
@Composable
fun FreundeLaden() = Ladezeile("Wird geholt …")

/** Der grüne Punkt vor „Jetzt im Dienst". */
@Composable
fun Dienstpunkt() {
    Box(Modifier.size(8.dp).background(Farben.Gruen, CircleShape))
}

// ------------------------------------------------------------------- Lage

/**
 * Wo jemand gerade steckt — im Dienst mit Rolle und Ort, sonst „zuletzt online".
 *
 * Wer Anwesenheit und „zuletzt gesehen" abgeschaltet hat, kommt ohne beides an;
 * dann steht nichts da statt einer Vermutung.
 */
fun lagezeile(anwesenheit: Anwesenheit?, zuletztGesehen: String?): String {
    val wo = anwesenheit ?: return zuletztGesehen?.let { zuletztOnline(it) }.orEmpty()
    val rolle = if (wo.rolle == "Leitstelle") "Leitstelle" else (wo.funkrufname ?: "Fahrzeug")
    val zustand = if (wo.zustand == "Lobby") "in der Lobby" else "im Einsatz"
    return "$rolle · ${wo.ort} · $zustand"
}

/** Grobe Relativzeit — genug, um „kürzlich" von „eine Weile her" zu trennen. */
private fun zuletztOnline(roh: String): String {
    val dann = zeitLesen(roh) ?: return ""
    val minuten = maxOf(0L, Math.round(Duration.between(dann, Instant.now()).toMillis() / 60_000.0))
    if (minuten < 2) return "zuletzt online gerade eben"
    if (minuten < 60) return "zuletzt online vor $minuten Min."
    val stunden = Math.round(minuten / 60.0)
    if (stunden < 24) return "zuletzt online vor $stunden Std."
    val tage = Math.round(stunden / 24.0)
    if (tage < 14) return "zuletzt online vor $tage Tag${if (tage == 1L) "" else "en"}"
    return "zuletzt online am ${KURZDATUM.format(dann.atZone(ZoneId.systemDefault()))}"
}

// ------------------------------------------------------------------- Zeit

private val KURZDATUM = DateTimeFormatter.ofPattern("dd.MM.yy")
private val UHR = DateTimeFormatter.ofPattern("HH:mm")
private val WOCHENTAG_KURZ = DateTimeFormatter.ofPattern("EEE", Locale.GERMAN)
private val TAGTRENNER = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.GERMAN)
private val LANGDATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy")

internal fun zeitLesen(roh: String?): Instant? = roh?.takeIf { it.isNotBlank() }?.let {
    runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(it) }.getOrNull()
}

/**
 * Wie lange etwas her ist — für Einträge am Brett und Kommentare
 * (`seitdem` im Web). Ab einer Woche kippt die Angabe aufs Datum.
 */
fun wieLangeHer(roh: String?): String {
    val wann = zeitLesen(roh) ?: return ""
    val her = Duration.between(wann, Instant.now())
    if (her.toMinutes() < 1) return "gerade eben"
    if (her.toMinutes() < 60) return "vor ${her.toMinutes()} Min."
    if (her.toHours() < 24) return "vor ${her.toHours()} Std."

    val zone = ZoneId.systemDefault()
    val tage = ChronoUnit.DAYS.between(wann.atZone(zone).toLocalDate(), LocalDate.now(zone))
    if (tage <= 1) return "gestern"
    if (tage < 7) return "vor $tage Tagen"
    return KURZDATUM.format(wann.atZone(zone))
}

/**
 * Die Zeitmarke einer Gesprächszeile (`kurzzeit`): heute die Uhrzeit, gestern
 * das Wort, in dieser Woche der Wochentag, davor das Datum.
 */
fun postfachzeit(roh: String?): String {
    val wann = zeitLesen(roh) ?: return ""
    val zone = ZoneId.systemDefault()
    val tag = wann.atZone(zone).toLocalDate()
    val heute = LocalDate.now(zone)
    return when {
        tag == heute -> UHR.format(wann.atZone(zone))
        tag == heute.minusDays(1) -> "Gestern"
        Duration.between(wann, Instant.now()).toDays() < 7 -> WOCHENTAG_KURZ.format(wann.atZone(zone))
        else -> KURZDATUM.format(wann.atZone(zone))
    }
}

/** „14:32" — die Uhrzeit an einer Sprechblase. */
fun uhrzeitKurz(roh: String?): String =
    zeitLesen(roh)?.let { UHR.format(it.atZone(ZoneId.systemDefault())) }.orEmpty()

/** Der Trenner im Gespräch: „Heute", „Gestern" oder „Montag, 14.09.2026". */
fun tagesname(roh: String?): String {
    val wann = zeitLesen(roh) ?: return ""
    val zone = ZoneId.systemDefault()
    val tag = wann.atZone(zone).toLocalDate()
    val heute = LocalDate.now(zone)
    return when (tag) {
        heute -> "Heute"
        heute.minusDays(1) -> "Gestern"
        else -> TAGTRENNER.format(wann.atZone(zone))
    }
}

/** Der Kalendertag eines Zeitstempels in der Zone des Geräts. */
fun kalendertag(roh: String?): LocalDate? =
    zeitLesen(roh)?.atZone(ZoneId.systemDefault())?.toLocalDate()

/** „18.08.26" — kurz, für Vorschläge und Profile. */
fun kurzdatum(roh: String?): String =
    zeitLesen(roh)?.let { KURZDATUM.format(it.atZone(ZoneId.systemDefault())) }.orEmpty()

/** „18.08.2026" — für „Dabei seit". */
fun langdatum(roh: String?): String =
    zeitLesen(roh)?.let { LANGDATUM.format(it.atZone(ZoneId.systemDefault())) } ?: "—"
