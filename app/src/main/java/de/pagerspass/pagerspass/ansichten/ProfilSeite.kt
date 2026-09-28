package de.pagerspass.pagerspass.ansichten

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.rememberAblage
import de.pagerspass.pagerspass.netz.Kartenausschnitt
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kontowege
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.netz.Profilaenderung
import de.pagerspass.pagerspass.netz.Profilbildstand
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Melderkatalog
import de.pagerspass.pagerspass.ui.schmuck.Profilbanner
import de.pagerspass.pagerspass.ui.schmuck.Profilkatalog
import de.pagerspass.pagerspass.ui.schmuck.Schmuck
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Der Profileditor — das Gegenstück zu `ProfilBearbeiten.vue`.
 *
 * <b>Fünf Reiter, und es steht immer nur einer da</b> — Person, Auftritt,
 * Schmuck, Melder, Funk. Untereinander waren es im Web elf gleich aussehende
 * Blöcke über acht Handybildschirme; wer die Wappenfarbe suchte, rollte an allem
 * vorbei.
 *
 * <b>Ein Entwurf und ein „Speichern".</b> Was zum Konto gehört (Name, Titel,
 * Wappen, Farbe, Rahmen, Muster, Gesicht, Ausschnitt), wird im Entwurf gesammelt
 * und mit einem Druck übernommen — die Vorschau oben zeigt ihn vorher. Was nur
 * diesem Gerät gehört (Bauform, Alarmton, Alarmierung, Profil), gilt sofort; das
 * Profilbild wird beim Auswählen eingereicht.
 *
 * <b>Der Benutzername zuerst.</b> Er hat einen eigenen Weg, ist eindeutig und
 * kann scheitern, während alles andere gelingt — sonst stünde am Ende ein
 * gespeichertes Profil unter einem Namen, den es nicht gibt.
 */
@Composable
fun ProfilSeite(
    wege: Kontowege,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    profil: Bereichsstand<Profil?> = Bereichsstand(),
    server: String = "",
    /**
     * Was im Shop gekauft wurde — Artikel-Ids (`titel-ehrenamt`, `farbe-9`,
     * `melder-mint`, `ton-gong`) und Stück-Ids. <b>Leer heißt gesperrt</b>: Nicht
     * geladen ist nicht dasselbe wie besessen.
     */
    besitzt: Set<String> = emptySet(),
    profilbild: Bereichsstand<Profilbildstand?> = Bereichsstand(),
    landkreise: List<Landkreis> = emptyList(),
    laeuft: Boolean = false,
    beiLaden: () -> Unit = {},
    beiKatalog: () -> Unit = {},
    beiGespeichert: () -> Unit = {},
    beiKontoNeu: (Konto) -> Unit = {},
    beiBildLaden: () -> Unit = {},
    beiBildEinreichen: (String, String, ByteArray) -> Unit = { _, _, _ -> },
    beiBildEntfernen: () -> Unit = {},
    beiWeg: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Profil gestalten",
            unterzeile = konto?.let { "@${it.benutzername}" },
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = profil.laedt,
            fehler = profil.fehler,
            inhalt = profil.inhalt,
            beiErneut = beiLaden,
        ) { p ->
            Editor(
                p = p,
                wege = wege,
                konto = konto,
                server = server,
                besitzt = besitzt,
                profilbild = profilbild,
                landkreise = landkreise,
                laeuft = laeuft,
                beiKatalog = beiKatalog,
                beiGespeichert = beiGespeichert,
                beiKontoNeu = beiKontoNeu,
                beiBildLaden = beiBildLaden,
                beiBildEinreichen = beiBildEinreichen,
                beiBildEntfernen = beiBildEntfernen,
                beiWeg = beiWeg,
                beiAbbrechen = beiZurueck,
            )
        }
    }
}

/** Der Entwurf — alles, was mit „Speichern" zum Konto geht. */
private data class Entwurf(
    val anzeigename: String,
    val benutzername: String,
    val vorstellung: String,
    val wappen: String,
    val wappenfarbe: Int,
    val standardsichtbarkeit: String,
    val melderGesicht: String,
    val profilrahmen: String,
    val kopfmuster: String,
    val kartenausschnitt: Kartenausschnitt,
    val titel: String,
) {
    companion object {
        fun aus(p: Profil) = Entwurf(
            anzeigename = p.anzeigename,
            benutzername = p.benutzername,
            vorstellung = p.vorstellung.orEmpty(),
            wappen = p.wappen,
            wappenfarbe = p.wappenfarbe,
            standardsichtbarkeit = p.standardsichtbarkeit ?: "Freunde",
            melderGesicht = p.melderGesicht.ifBlank { "standard" },
            profilrahmen = p.profilrahmen,
            kopfmuster = p.kopfmuster,
            // Nie leer: Wer die Karte anwählt, soll sofort etwas sehen. Die Vorgabe
            // ist Deutschland und ausdrücklich nicht der eigene Standort.
            kartenausschnitt = p.kartenausschnitt ?: Kartenausschnitt.STANDARD,
            titel = p.titel.orEmpty(),
        )
    }
}

@Composable
private fun Editor(
    p: Profil,
    wege: Kontowege,
    konto: Konto?,
    server: String,
    besitzt: Set<String>,
    profilbild: Bereichsstand<Profilbildstand?>,
    landkreise: List<Landkreis>,
    laeuft: Boolean,
    beiKatalog: () -> Unit,
    beiGespeichert: () -> Unit,
    beiKontoNeu: (Konto) -> Unit,
    beiBildLaden: () -> Unit,
    beiBildEinreichen: (String, String, ByteArray) -> Unit,
    beiBildEntfernen: () -> Unit,
    beiWeg: (String) -> Unit,
    beiAbbrechen: () -> Unit,
) {
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()
    var entwurf by remember(p.kennung) { mutableStateOf(Entwurf.aus(p)) }
    var reiter by rememberSaveable { mutableStateOf("person") }

    val stufe = konto?.level ?: 1
    val premium = konto?.premiumAktiv == true

    /** Laufbahn nach Stufe, Shop nach Kauf, Abo nach Premium — `tragbar` im Web. */
    fun tragbar(abLevel: Int, preis: Int?, nurPremium: Boolean, artikel: String, stueck: String): Boolean = when {
        nurPremium -> premium
        preis == null -> stufe >= abLevel
        else -> artikel in besitzt || stueck in besitzt
    }

    fun sperrtext(abLevel: Int, preis: Int?, nurPremium: Boolean): String = when {
        nurPremium -> "Premium"
        preis == null -> "ab St. $abLevel"
        else -> "im Shop"
    }

    // Die Vorschau zeigt, was auf den ersten drei Reitern eingestellt wird. Melder
    // und Funk ändern daran nichts — dort nähme sie nur Platz.
    if (reiter != "melder" && reiter != "funk") {
        Profilbanner(
            kennung = p.kennung,
            anzeigename = entwurf.anzeigename.ifBlank { "Dein Anzeigename" },
            benutzername = entwurf.benutzername.ifBlank { "benutzername" },
            augenbraue = "Live-Vorschau",
            wappen = entwurf.wappen,
            wappenfarbe = entwurf.wappenfarbe,
            kopfmuster = entwurf.kopfmuster,
            profilrahmen = entwurf.profilrahmen,
            bildAdresse = bildweg(server, p.profilbild),
            premium = p.premium,
            teammitglied = p.teammitglied,
            marken = listOfNotNull(Profilkatalog.titelname(entwurf.titel)),
        ) {
            Leise(entwurf.vorstellung.ifBlank { "Hier kann ein kurzer Satz über dich stehen." })
        }
        SehrLeise("Deine Änderungen werden erst mit „Speichern“ übernommen.")
    }

    Reiterreihe {
        listOf(
            "person" to "Person",
            "auftritt" to "Auftritt",
            "schmuck" to "Schmuck",
            "melder" to "Melder",
            "funk" to "Funk",
        ).forEach { (id, name) ->
            Reiter(name, offen = reiter == id, beiDruck = { reiter = id })
        }
    }

    when (reiter) {
        "person" -> {
            Kopfzeile("Wer du bist", "Die Angaben, unter denen andere dich finden und ansprechen.", konto = true)
            Feld(
                wert = entwurf.anzeigename,
                beiAenderung = { entwurf = entwurf.copy(anzeigename = it.take(48)) },
                etikett = "Anzeigename",
            )
            SehrLeise("Der Name, unter dem du im Funk und in der Mannschaftsliste stehst.")
            Feld(
                wert = entwurf.benutzername,
                beiAenderung = { entwurf = entwurf.copy(benutzername = it.take(20)) },
                etikett = "Benutzername",
                stil = Schrift.MonoNormal,
            )
            SehrLeise(
                "3 bis 20 Zeichen: Buchstaben, Ziffern, Bindestrich, Unterstrich. Unter diesem Namen " +
                    "finden dich andere.",
            )
            Feld(
                wert = entwurf.vorstellung,
                beiAenderung = { entwurf = entwurf.copy(vorstellung = it.take(200)) },
                etikett = "Vorstellung",
                platzhalter = "Ein Satz über dich — ohne Links.",
                einzeilig = false,
                weiterTaste = ImeAction.Default,
            )
            SehrLeise("Ein Satz über dich — er steht an deinem Profilkopf. ${entwurf.vorstellung.length}/200")
            Etikett("Neue Beiträge sind sichtbar für")
            SehrLeise(
                "Die Voreinstellung fürs Brett — beim Schreiben kannst du sie für den einzelnen Beitrag " +
                    "ändern.",
            )
            Pillenreihe {
                Profilkatalog.SICHTBARKEITEN.forEach { (wert, name) ->
                    Pille(
                        name,
                        an = entwurf.standardsichtbarkeit == wert,
                        beiDruck = { entwurf = entwurf.copy(standardsichtbarkeit = wert) },
                    )
                }
            }
        }

        "auftritt" -> {
            Kopfzeile("Dein Auftritt", "Bild, Zeichen, Farbe und Titel — das, was neben deinem Namen steht.", konto = true)
            Knopf(
                "Vitrine im Dienstbuch wählen",
                { beiWeg("dienstbuch/abzeichen") },
                art = Knopfart.Leise,
                kompakt = true,
            )

            Bildabschnitt(
                stand = profilbild,
                laeuft = laeuft,
                beiLaden = beiBildLaden,
                beiEinreichen = beiBildEinreichen,
                beiEntfernen = beiBildEntfernen,
            )

            Etikett("Wappen")
            SehrLeise("Das Zeichen in deinem Kontobild. Die Sonderzeichen schaltet die Laufbahn frei.")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                Profilkatalog.WAPPEN.forEach { w ->
                    val frei = if (w.premium) premium else stufe >= w.abLevel
                    Wahlkachel(
                        name = (if (w.premium) "★ " else "") + w.name,
                        gewaehlt = entwurf.wappen == w.wert,
                        frei = frei,
                        sperre = if (w.premium) null else "ab St. ${w.abLevel}",
                        beiWahl = { entwurf = entwurf.copy(wappen = w.wert) },
                    ) {
                        Kontobild(
                            kennung = p.kennung,
                            anzeigename = entwurf.anzeigename.ifBlank { p.anzeigename },
                            wappen = w.wert,
                            wappenfarbe = entwurf.wappenfarbe,
                            groesse = 44.dp,
                        )
                    }
                }
            }

            Etikett("Titel")
            SehrLeise(
                "Eine Zeile unter deinem Namen — alle Titel gibt es im Shop. Kein Rang: die Ränge vergibt " +
                    "die Laufbahn.",
            )
            Pillenreihe {
                Pille("Kein Titel", an = entwurf.titel.isBlank(), beiDruck = { entwurf = entwurf.copy(titel = "") })
                Profilkatalog.TITEL.forEach { t ->
                    val gekauft = "titel-${t.id}" in besitzt || t.id in besitzt || entwurf.titel == t.id
                    Pille(
                        if (gekauft) t.name else "${t.name} — im Shop",
                        an = entwurf.titel == t.id,
                        beiDruck = { entwurf = entwurf.copy(titel = t.id) },
                        aktiv = gekauft,
                    )
                }
                // Die Vergabetitel — nur die eigenen: Wer keinen trägt, soll hier auch
                // keinen sehen. Die Abo-Titel hängen am Abo, nicht an der Kaufliste.
                Profilkatalog.VERGABETITEL
                    .filter { t ->
                        if (t.id in Profilkatalog.PREMIUM_TITEL) {
                            premium
                        } else {
                            "titel-${t.id}" in besitzt || entwurf.titel == t.id
                        }
                    }
                    .forEach { t ->
                        Pille(
                            (if (t.id in Profilkatalog.PREMIUM_TITEL) "★ " else "") + t.name,
                            an = entwurf.titel == t.id,
                            beiDruck = { entwurf = entwurf.copy(titel = t.id) },
                        )
                    }
            }
            if (entwurf.titel in Profilkatalog.PREMIUM_TITEL) {
                Text("★ Dieser Titel gehört zu Premium.", style = Schrift.Klein, color = Farben.AmberHell)
            }

            Etikett("Wappenfarbe")
            SehrLeise("Der Grund deines Kontobilds. Gold kommt aus der Laufbahn, die vier daneben aus dem Shop.")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                for (farbe in 0..12) {
                    // 0–7 frei, Gold über die Stufe, die Kauffarben über die Kaufliste —
                    // und mit laufendem Abo die ganze Palette.
                    val frei = when {
                        farbe < Profilkatalog.GOLDFARBE -> true
                        premium -> true
                        farbe == Profilkatalog.GOLDFARBE -> stufe >= Profilkatalog.GOLDFARBE_AB_LEVEL
                        else -> "farbe-$farbe" in besitzt || farbe == entwurf.wappenfarbe
                    }
                    val grund = if (farbe == Profilkatalog.GOLDFARBE) {
                        "ab Stufe ${Profilkatalog.GOLDFARBE_AB_LEVEL}"
                    } else {
                        "im Shop"
                    }
                    Wahlkachel(
                        name = Profilkatalog.farbname(farbe),
                        gewaehlt = entwurf.wappenfarbe == farbe,
                        frei = frei,
                        sperre = grund,
                        beiWahl = { entwurf = entwurf.copy(wappenfarbe = farbe) },
                    ) {
                        Kontobild(
                            kennung = p.kennung,
                            anzeigename = entwurf.anzeigename.ifBlank { p.anzeigename },
                            wappen = entwurf.wappen,
                            wappenfarbe = farbe,
                            groesse = 36.dp,
                        )
                    }
                }
            }
        }

        "schmuck" -> {
            Kopfzeile(
                "Profilschmuck",
                "Ring und Farbband deines Profilkopfs — freigeschaltet über Laufbahn und Shop.",
                konto = true,
            )

            Etikett("Profilrahmen")
            SehrLeise("Der Ring um dein Wappen — am Profilkopf. Schaltet sich über die Laufbahn frei.")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                Schmuck.RAHMEN.forEach { r ->
                    val frei = r.id == entwurf.profilrahmen ||
                        tragbar(r.abLevel, r.preisCredits, r.premium, "rahmen-${r.id}", r.id)
                    Wahlkachel(
                        name = (if (r.premium) "★ " else "") + r.name,
                        gewaehlt = entwurf.profilrahmen == r.id,
                        frei = frei,
                        sperre = if (r.premium) null else sperrtext(r.abLevel, r.preisCredits, r.premium),
                        beiWahl = { entwurf = entwurf.copy(profilrahmen = r.id) },
                    ) {
                        Kontobild(
                            kennung = p.kennung,
                            anzeigename = entwurf.anzeigename.ifBlank { p.anzeigename },
                            wappen = entwurf.wappen,
                            wappenfarbe = entwurf.wappenfarbe,
                            bildAdresse = bildweg(server, p.profilbild),
                            rahmen = r.id,
                            groesse = 40.dp,
                        )
                    }
                }
            }

            Etikett("Kopfmuster")
            SehrLeise("Das Muster im Farbband deines Profils. Schaltet sich über die Laufbahn frei.")
            val ton = Wappen.ton(p.kennung, entwurf.wappenfarbe)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                Schmuck.KOPFMUSTER.forEach { m ->
                    val frei = m.id == entwurf.kopfmuster ||
                        tragbar(m.abLevel, m.preisCredits, m.premium, "muster-${m.id}", m.id)
                    Wahlkachel(
                        name = (if (m.premium) "★ " else "") + m.name,
                        gewaehlt = entwurf.kopfmuster == m.id,
                        frei = frei,
                        sperre = if (m.premium) null else sperrtext(m.abLevel, m.preisCredits, m.premium),
                        beiWahl = { entwurf = entwurf.copy(kopfmuster = m.id) },
                    ) {
                        Box(
                            Modifier
                                .size(width = 76.dp, height = 40.dp)
                                .background(Farben.Flaeche, Rundung.Klein)
                                .kopfband(m.id, ton),
                        )
                    }
                }
            }

            // „Eigene Lage" ist das einzige Muster mit einer zweiten Frage: welcher
            // Ausschnitt. Sie steht hier unter dem Raster — was man einstellt, gehört
            // neben das, wofür man es einstellt.
            if (entwurf.kopfmuster == Kartenausschnitt.MUSTER) {
                Kasten(abstandInnen = Abstand.Klein) {
                    Etikett("Dein Ausschnitt")
                    SehrLeise("Mit + und − den Maßstab wählen")
                    Kartenwahl(
                        ausschnitt = entwurf.kartenausschnitt,
                        beiAenderung = { entwurf = entwurf.copy(kartenausschnitt = it) },
                        landkreise = landkreise,
                        beiKatalog = beiKatalog,
                    )
                    Text(
                        text = hervorgehoben(
                            "**Das sieht jeder, der dein Profil öffnet.** Näher als eine Ortsansicht geht " +
                                "es deshalb nicht — der Maßstab hört bei Stufe ${Kartenausschnitt.MAX_ZOOM} " +
                                "auf, damit dort eine Gegend steht und keine Anschrift.",
                        ),
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                    )
                }
            }
        }

        "melder" -> Meldereiter(
            stufe = stufe,
            premium = premium,
            besitzt = besitzt,
            gesicht = entwurf.melderGesicht,
            beiGesicht = { entwurf = entwurf.copy(melderGesicht = it) },
            tragbar = ::tragbar,
            sperrtext = ::sperrtext,
        )

        else -> {
            Kopfzeile("Dein Funkgerät", "Die Sprechtaste auf diesem Gerät.", konto = false)
            Kasten(abstandInnen = Abstand.Klein) {
                Etikett("Sprechtaste")
                Leise(
                    "Am Handy funkst du mit der Sprechtaste auf dem Bildschirm — halten statt tippen, wie " +
                        "am echten Handfunkgerät. Sie gilt für Funk, Leitstellendraht und Einsatzstelle, und " +
                        "immer für die Leitung, die gerade offen ist. Eine Taste auf einer angeschlossenen " +
                        "Tastatur legst du im Web am Rechner fest.",
                )
            }
        }
    }

    v.fehler?.let { Warnzeile(it) }
    SehrLeise("Profilangaben gelten überall — Melder und Funkgerät nur hier, und schon jetzt.")
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Knopf("Abbrechen", beiAbbrechen, art = Knopfart.Leise, aktiv = !v.laeuft)
        Knopf(
            if (v.laeuft) "Wird gespeichert …" else "Speichern",
            {
                val kennung = konto?.kennung ?: p.kennung
                val stand = entwurf
                bereich.vorgang(v, "Das ließ sich nicht speichern.") {
                    if (stand.benutzername != p.benutzername) {
                        beiKontoNeu(wege.benutzernameAendern(kennung, stand.benutzername))
                    }
                    wege.profilSpeichern(
                        kennung,
                        Profilaenderung(
                            anzeigename = stand.anzeigename,
                            vorstellung = stand.vorstellung,
                            wappen = stand.wappen,
                            wappenfarbe = stand.wappenfarbe,
                            melderGesicht = stand.melderGesicht,
                            profilrahmen = stand.profilrahmen,
                            kopfmuster = stand.kopfmuster,
                            titel = stand.titel,
                            standardsichtbarkeit = stand.standardsichtbarkeit,
                            // Nur, wenn die Karte auch gewählt ist — sonst weist der Server
                            // ein Konto ohne Abo mit „Der Kartenkopf gehört zu Premium" ab.
                            kartenausschnitt = stand.kartenausschnitt.takeIf {
                                stand.kopfmuster == Kartenausschnitt.MUSTER
                            },
                        ),
                    )
                    beiGespeichert()
                    v.erfolg = "Gespeichert."
                }
            },
            art = Knopfart.Haupt,
            aktiv = !v.laeuft,
        )
    }
    v.erfolg?.let { Erfolgszeile(it) }
}

/** Überschrift eines Reiters samt der Marke, wofür er gilt. */
@Composable
private fun Kopfzeile(titel: String, unter: String, konto: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        Text(titel, style = Schrift.Gross.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
        SehrLeise(unter)
        Text(
            text = if (konto) "Gilt überall · mit „Speichern“" else "Gilt sofort · nur dieses Gerät",
            style = Schrift.MonoKlein,
            color = if (konto) Farben.Amber else Farben.BlauHell,
        )
    }
}

/**
 * Eine Kachel der Auswahl — Bild, Name, und warum sie gesperrt ist. Der Grund
 * steht an der Kachel und nicht nur im Vorleser: Am Finger gibt es kein
 * Überfahren, und ein grauer Knopf ohne Grund sieht aus wie ein Fehler.
 */
@Composable
private fun Wahlkachel(
    name: String,
    gewaehlt: Boolean,
    frei: Boolean,
    sperre: String?,
    beiWahl: () -> Unit,
    bild: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .width(92.dp)
            .background(if (gewaehlt) Farben.HauchAmber else Farben.Flaeche, Rundung.Klein)
            .border(if (gewaehlt) 2.dp else 1.dp, if (gewaehlt) Farben.Amber else Farben.Rand, Rundung.Klein)
            .clickable(
                enabled = frei,
                onClick = beiWahl,
                role = Role.RadioButton,
                indication = null,
                interactionSource = null,
            )
            .padding(Abstand.Klein),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) { bild() }
        Text(
            text = name,
            style = Schrift.Winzig,
            color = when {
                gewaehlt -> Farben.Amber
                frei -> Farben.TextLeise
                else -> Farben.TextSehrLeise
            },
            maxLines = 2,
        )
        if (!frei && sperre != null) {
            Text(sperre, style = Schrift.Winzig.copy(fontFamily = Schrift.Mono), color = Farben.TextSehrLeise)
        }
    }
}

// --------------------------------------------------------------- Melder

/**
 * Der Melder-Reiter: Bauform, Gesicht, Alarmton, Alarmierung und Profil.
 *
 * Nur das Gesicht gehört zum Konto („Speichern"); der Rest gilt sofort und nur
 * auf diesem Gerät (`Ablage`, gelesen im Dienst über `mobil/Bedienung.kt`). Die
 * Gehäusewerkstatt gibt es nur am Rechner.
 */
@Composable
private fun Meldereiter(
    stufe: Int,
    premium: Boolean,
    besitzt: Set<String>,
    gesicht: String,
    beiGesicht: (String) -> Unit,
    tragbar: (Int, Int?, Boolean, String, String) -> Boolean,
    sperrtext: (Int, Int?, Boolean) -> String,
) {
    val ablage = rememberAblage()
    val bereich = rememberCoroutineScope()
    val bauform by remember(ablage) { ablage.melderBauformFluss() }.collectAsState(initial = "dienst")
    val ton by remember(ablage) { ablage.melderTonFluss() }.collectAsState(initial = "zweiklang")
    val art by remember(ablage) { ablage.alarmierungsartFluss() }.collectAsState(initial = "voll")
    val profil by remember(ablage) { ablage.melderprofilFluss() }.collectAsState(initial = "vollalarm")

    Kopfzeile("Dein Melder", "Bauform, Gehäuse und Alarmton für dieses Gerät.", konto = false)

    Etikett("Melder-Bauform")
    SehrLeise("Welches Gerät in deiner Tasche steckt — es bleibt stehen, bis du es änderst.")
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Melderkatalog.BAUFORMEN.forEach { b ->
            val frei = if (b.premium) premium else stufe >= b.abLevel
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pille(
                    aufschrift = (if (b.premium) "★ " else "") + b.name +
                        if (!frei && !b.premium) " · ab St. ${b.abLevel}" else "",
                    an = bauform == b.id,
                    beiDruck = { bereich.launch { ablage.melderBauformSetzen(b.id) } },
                    aktiv = frei,
                )
                SehrLeise(b.erklaerung, modifier = Modifier.weight(1f))
            }
        }
    }
    SehrLeise(
        "★ Die Gehäusewerkstatt gibt es nur am Rechner. Dort ziehst du dir deinen Melder aus achtzehn " +
            "Bauteilen selbst zusammen — am Handy wären die Teile zu klein, um sie zu treffen.",
    )

    Etikett("Melder-Gesicht")
    SehrLeise(
        "Gehäuse und Display deines Melders. Die eine Zeile dieser Gruppe, die zum Konto gehört: Sie gilt " +
            "überall und wird erst mit „Speichern“ übernommen. Schaltet sich über die Laufbahn frei — reine " +
            "Zierde, kein Spielvorteil.",
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
    ) {
        Melderkatalog.GESICHTER.forEach { g ->
            val frei = g.id == gesicht || tragbar(g.abLevel, g.preisCredits, g.premium, "melder-${g.id}", g.id)
            Wahlkachel(
                name = (if (g.premium) "★ " else "") + g.name,
                gewaehlt = gesicht == g.id,
                frei = frei,
                sperre = if (g.premium) null else sperrtext(g.abLevel, g.preisCredits, g.premium),
                beiWahl = { beiGesicht(g.id) },
            ) {
                // Das Gerät im Kleinen: Gehäuse und Anzeigefeld in ihren Farben.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 34.dp, height = 44.dp)
                        .background(g.gehaeuse, Rundung.Winzig)
                        .border(1.dp, Farben.RandHell, Rundung.Winzig),
                ) {
                    Box(Modifier.size(width = 24.dp, height = 16.dp).background(g.lcd, Rundung.Winzig))
                }
            }
        }
    }

    // Tonwerkstatt: der Alarmton samt Ton- und Klangwerkstatt (Meldertonwahl.kt).
    Meldertonwahl(
        premium = premium,
        katalogFrei = { t -> t.id == ton || tragbar(t.abLevel, t.preisCredits, t.premium, "ton-${t.id}", t.id) },
        katalogSperre = { t -> sperrtext(t.abLevel, t.preisCredits, t.premium) },
    )

    Etikett("Alarmierung")
    SehrLeise(
        "Wie dein Melder einen Alarm ankündigt. Die einzige Einstellung hier mit Folgen im Dienst: Wer auf " +
            "Vibration oder stumm steht, hört den Alarm nicht.",
    )
    Pillenreihe {
        Melderkatalog.ALARMIERUNGSARTEN.forEach { a ->
            Pille(
                a.name,
                an = art == a.id,
                beiDruck = { bereich.launch { ablage.alarmierungsartSetzen(a.id) } },
            )
        }
    }
    Melderkatalog.ALARMIERUNGSARTEN.firstOrNull { it.id == art }?.let { SehrLeise(it.was) }

    Etikett("Profil")
    SehrLeise("Alarmierungsart und Lautstärke unter einem Namen — dasselbe, was am Gerät im Displaykopf steht.")
    Pillenreihe {
        Melderkatalog.PROFILE.forEach { pr ->
            Pille(
                pr.name,
                an = profil == pr.id,
                beiDruck = { bereich.launch { ablage.melderprofilSetzen(pr.id, pr.art) } },
            )
        }
    }
    Melderkatalog.PROFILE.firstOrNull { it.id == profil }?.let { SehrLeise(it.was) }
}

// ------------------------------------------------------------ Kartenwahl

/**
 * Den Kartenausschnitt für den Profilkopf wählen — `Kartenwahl.vue`.
 *
 * Geschoben wird über die Mercator-Ebene: Der Mittelpunkt wird in Weltpunkte
 * umgerechnet, wandert mit dem Finger mit und wird zurückgerechnet. <b>Die Karte
 * hört erst auf Wischen, wenn man es verlangt</b> („Karte verschieben") — sonst
 * schluckte das Feld jeden Wisch, der auf ihm beginnt, und die Seite rollte nicht.
 *
 * <b>Kacheln zeichnet die App im Kopf nicht</b> (siehe `Kopfmuster.kt`); das Feld
 * zeigt deshalb das Raster mit Fadenkreuz, Maßstab und Koordinaten.
 */
@Composable
private fun Kartenwahl(
    ausschnitt: Kartenausschnitt,
    beiAenderung: (Kartenausschnitt) -> Unit,
    landkreise: List<Landkreis>,
    beiKatalog: () -> Unit,
) {
    LaunchedEffect(Unit) { beiKatalog() }
    val dichte = LocalDensity.current.density
    val aktuell by rememberUpdatedState(ausschnitt)
    val melden by rememberUpdatedState(beiAenderung)
    var beweglich by remember { mutableStateOf(false) }
    var kreiswahl by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Farben.BgTief, Rundung.Klein)
            .border(1.dp, if (beweglich) Farben.Amber else Farben.Rand, Rundung.Klein)
            .drawBehind {
                val schritt = 24.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Farben.Rand, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += schritt
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Farben.Rand, Offset(0f, y), Offset(size.width, y), 1f)
                    y += schritt
                }
                val mitte = center
                val arm = 10.dp.toPx()
                drawLine(Farben.Amber, Offset(mitte.x - arm, mitte.y), Offset(mitte.x + arm, mitte.y), 2.dp.toPx())
                drawLine(Farben.Amber, Offset(mitte.x, mitte.y - arm), Offset(mitte.x, mitte.y + arm), 2.dp.toPx())
            }
            .then(
                if (beweglich) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { aenderung, weg ->
                            aenderung.consume()
                            val a = aktuell
                            val punkt = zuWeltpunkt(a)
                            // Die Karte folgt dem Finger: Wer nach rechts zieht, holt den
                            // Westen ins Bild — der Mittelpunkt wandert nach links.
                            melden(zuOrt(punkt.first - weg.x / dichte, punkt.second - weg.y / dichte, a.zoom))
                        }
                    }
                } else {
                    Modifier
                }
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.align(Alignment.TopEnd).padding(Abstand.Winzig),
        ) {
            Knopf(
                "+",
                { melden(aktuell.copy(zoom = min(Kartenausschnitt.MAX_ZOOM, aktuell.zoom + 1))) },
                kompakt = true,
                aktiv = ausschnitt.zoom < Kartenausschnitt.MAX_ZOOM,
            )
            Knopf(
                "−",
                { melden(aktuell.copy(zoom = max(Kartenausschnitt.MIN_ZOOM, aktuell.zoom - 1))) },
                kompakt = true,
                aktiv = ausschnitt.zoom > Kartenausschnitt.MIN_ZOOM,
            )
        }
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(Abstand.Winzig)) {
            Knopf(
                if (beweglich) "Fertig" else "Karte verschieben",
                { beweglich = !beweglich },
                art = if (beweglich) Knopfart.Normal else Knopfart.Haupt,
                kompakt = true,
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.weight(1f)) {
            Text(massstab(ausschnitt.zoom), style = Schrift.Normal, color = Farben.Text)
            SehrLeise("%.4f, %.4f".format(java.util.Locale.ROOT, ausschnitt.lat, ausschnitt.lon), mono = true)
        }
        Knopf(
            if (landkreise.isEmpty()) "Kreise werden geladen …" else "Zu einem Kreis",
            { kreiswahl = true },
            kompakt = true,
            aktiv = landkreise.isNotEmpty(),
        )
        Knopf("Zurücksetzen", { melden(Kartenausschnitt.STANDARD) }, art = Knopfart.Leise, kompakt = true)
    }

    if (kreiswahl) {
        Auswahlblende(
            titel = "Zu einem Kreis springen",
            eintraege = landkreise.sortedBy { it.name },
            aufschrift = { it.name },
            unterzeile = { it.kreisstadt.takeIf { s -> s.isNotBlank() && s != it.name } },
            beiWahl = { kreis ->
                // Stufe 11 zeigt den Kreisort mit seinem Umland — nah genug, um ihn zu
                // erkennen, und weit genug, dass die Wahl danach noch etwas zu schieben hat.
                melden(Kartenausschnitt(lat = kreis.lat, lon = kreis.lon, zoom = 11))
                kreiswahl = false
            },
            beiSchliessen = { kreiswahl = false },
        )
    }
}

/** Wie weit der Ausschnitt reicht — in Worten statt in Zoomstufen. */
private fun massstab(zoom: Int): String = when {
    zoom >= 13 -> "Ort"
    zoom >= 11 -> "Stadt und Umland"
    zoom >= 9 -> "Landkreis"
    zoom >= 7 -> "Region"
    else -> "Land"
}

private const val KACHEL = 256.0

private fun zuWeltpunkt(a: Kartenausschnitt): Pair<Float, Float> {
    val breite = a.lat * PI / 180.0
    val n = KACHEL * 2.0.pow(a.zoom)
    val x = (a.lon + 180.0) / 360.0 * n
    val y = (1.0 - ln(tan(breite) + 1.0 / cos(breite)) / PI) / 2.0 * n
    return x.toFloat() to y.toFloat()
}

private fun zuOrt(x: Float, y: Float, zoom: Int): Kartenausschnitt {
    val n = KACHEL * 2.0.pow(zoom)
    // In der Länge läuft die Welt rund; in der Breite endet Mercator bei 85°.
    val lon = ((((x / n) * 360.0 - 180.0) % 360.0) + 540.0) % 360.0 - 180.0
    val lat = (atan(sinh(PI * (1.0 - 2.0 * y / n))) * 180.0 / PI).coerceIn(-85.0, 85.0)
    return Kartenausschnitt(lat = lat, lon = lon, zoom = zoom)
}

// ------------------------------------------------------------- Profilbild

/**
 * Das Profilbild — einreichen, warten, zurücknehmen (`Profilbildwahl.vue`).
 *
 * <b>Ein Bild wird nicht gesetzt, sondern eingereicht.</b> Zwischen dem Hochladen
 * und dem Sichtbarwerden liegt eine Freigabe durch die Verwaltung.
 *
 * <b>Verkleinert wird vor dem Hochladen.</b> Ein Foto vom Telefon hat vier bis
 * zwölf Megabyte und 4000 Punkte Kante — der Server nimmt zwei Megabyte. Also
 * hier: auf 1024 Punkte längste Kante, als WebP (oder JPEG, wo WebP nicht erlaubt
 * ist). Unverändert bleibt, was schon passt, und jedes GIF — es könnte bewegt sein.
 */
@Composable
private fun Bildabschnitt(
    stand: Bereichsstand<Profilbildstand?>,
    laeuft: Boolean,
    beiLaden: () -> Unit,
    beiEinreichen: (String, String, ByteArray) -> Unit,
    beiEntfernen: () -> Unit,
) {
    LaunchedEffect(Unit) { beiLaden() }

    val zusammenhang = LocalContext.current
    val bereich = rememberCoroutineScope()
    var meldung by remember { mutableStateOf<String?>(null) }
    var bereitet by remember { mutableStateOf(false) }
    val b = stand.inhalt

    val waehler = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { ziel ->
        if (ziel == null) return@rememberLauncherForActivityResult
        meldung = null
        bereitet = true
        bereich.launch {
            val ergebnis = runCatching {
                withContext(Dispatchers.IO) { bildAufbereiten(zusammenhang, ziel, b) }
            }
            bereitet = false
            ergebnis
                .onSuccess { (name, typ, daten) ->
                    val grenze = b?.maxBytes ?: 0L
                    if (grenze > 0 && daten.size > grenze) {
                        meldung = "Das Bild ist zu groß — erlaubt sind höchstens ${grenze / (1024 * 1024)} MB."
                    } else {
                        beiEinreichen(name, typ, daten)
                    }
                }
                .onFailure { meldung = it.message ?: "Das Bild ließ sich nicht hochladen." }
        }
    }

    Abschnitt("Profilbild") {
        if (b?.wartet == true) {
            Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
                Text("Wird geprüft", style = Schrift.Gross, color = Farben.Text)
                SehrLeise(
                    "Dein Bild liegt bei der Verwaltung. Bis zur Freigabe bleibt das bisherige stehen — " +
                        "eingereicht ${tag(b.eingereichtUm)}.",
                )
            }
        }

        if (b?.abgelehnt == true) {
            Kasten(abstandInnen = Abstand.Klein) {
                Text("Nicht freigegeben", style = Schrift.Gross, color = Farben.SignalHell)
                SehrLeise(b.grund ?: "Ohne Angabe eines Grundes.")
            }
        }

        SehrLeise(
            buildString {
                append("Ein eigenes Bild wird von der Verwaltung freigegeben, bevor es sichtbar wird.")
                if (b != null && b.maxBytes > 0) {
                    append(" Höchstens ${b.maxBytes / (1024 * 1024)} MB")
                    if (b.minKante > 0) append(", mindestens ${b.minKante} Punkte Kante")
                    append(" — große Fotos verkleinert die App vorher selbst.")
                }
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                aufschrift = when {
                    bereitet -> "Wird vorbereitet …"
                    b?.bild != null -> "Anderes Bild wählen"
                    else -> "Bild wählen"
                },
                beiDruck = { waehler.launch("image/*") },
                aktiv = !laeuft && !bereitet,
                kompakt = true,
            )
            if (b?.bild != null || b?.eingereicht != null) {
                Knopf(
                    "Bild entfernen",
                    beiEntfernen,
                    art = Knopfart.Gefahr,
                    aktiv = !laeuft,
                    kompakt = true,
                )
            }
        }
        meldung?.let { Warnzeile(it) }
    }
}

/** Längste Kante nach dem Verkleinern — reichlich für einen Kreis von 84 Punkten. */
private const val ZIELKANTE = 1024

/**
 * Bereitet ein gewähltes Bild fürs Hochladen auf — `aufbereiten` in
 * `Profilbildwahl.vue`. `ImageDecoder` dreht dabei nach den EXIF-Angaben, wie
 * `createImageBitmap(…, { imageOrientation: 'from-image' })` im Web.
 *
 * Gibt Dateiname, Inhaltstyp und Bytes zurück. <b>Der echte Dateiname geht
 * niemanden etwas an</b> — er trägt regelmäßig mehr Auskunft, als das Hochladen
 * eines Bildes verlangt; der Server braucht nur die Endung.
 */
private fun bildAufbereiten(zusammenhang: Context, ziel: Uri, grenzen: Profilbildstand?): Triple<String, String, ByteArray> {
    val aufloeser = zusammenhang.contentResolver
    val typ = aufloeser.getType(ziel) ?: "image/jpeg"
    val roh = aufloeser.openInputStream(ziel)?.use { it.readBytes() }
        ?: throw IllegalArgumentException("Das Bild ließ sich nicht lesen.")
    val erlaubt = grenzen?.erlaubteTypen.orEmpty()
    val artErlaubt = erlaubt.isEmpty() || typ in erlaubt
    val hoechstens = grenzen?.maxBytes?.takeIf { it > 0 } ?: Long.MAX_VALUE

    if (typ == "image/gif") return Triple("profilbild.gif", typ, roh)

    var kante = 0
    val bild: Bitmap = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(aufloeser, ziel)) { decoder, info, _ ->
            val breite = info.size.width
            val hoehe = info.size.height
            kante = max(breite, hoehe)
            if (kante > ZIELKANTE) {
                val faktor = ZIELKANTE.toFloat() / kante
                decoder.setTargetSize(max(1, (breite * faktor).roundToInt()), max(1, (hoehe * faktor).roundToInt()))
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (_: Exception) {
        if (artErlaubt) return Triple("profilbild.${kurzform(typ)}", typ, roh)
        throw IllegalArgumentException(
            "Dieses Bildformat lässt sich hier nicht öffnen. Wähle bitte ein JPEG oder PNG — etwa ein " +
                "Bildschirmfoto.",
        )
    }

    if (artErlaubt && roh.size <= hoechstens && kante <= ZIELKANTE) {
        bild.recycle()
        return Triple("profilbild.${kurzform(typ)}", typ, roh)
    }

    val ausgabe = ByteArrayOutputStream()
    return try {
        if (erlaubt.isEmpty() || "image/webp" in erlaubt) {
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            bild.compress(format, 85, ausgabe)
            Triple("profilbild.webp", "image/webp", ausgabe.toByteArray())
        } else {
            // JPEG kennt keine Durchsicht — sie würde schwarz. Also auf weißem Grund.
            val weiss = Bitmap.createBitmap(bild.width, bild.height, Bitmap.Config.ARGB_8888)
            Canvas(weiss).apply {
                drawColor(android.graphics.Color.WHITE)
                drawBitmap(bild, 0f, 0f, null)
            }
            weiss.compress(Bitmap.CompressFormat.JPEG, 85, ausgabe)
            weiss.recycle()
            Triple("profilbild.jpg", "image/jpeg", ausgabe.toByteArray())
        }
    } finally {
        bild.recycle()
    }
}

/** Aus `image/jpeg` wird `jpg`. */
private fun kurzform(inhaltstyp: String): String = when (inhaltstyp.substringAfter("/")) {
    "jpeg" -> "jpg"
    "svg+xml" -> "svg"
    else -> inhaltstyp.substringAfter("/")
}

/** Wo das Profilbild liegt — der Pfad steht an genau einer Stelle. */
internal fun bildweg(server: String, dateiname: String?): String? =
    de.pagerspass.pagerspass.ui.schmuck.profilbildAdresse(server, dateiname)
