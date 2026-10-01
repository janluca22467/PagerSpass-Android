package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.mobil.bundeslaender
import de.pagerspass.pagerspass.mobil.nachBundesland
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.bundeslandname
import de.pagerspass.pagerspass.ui.bausteine.Codefeld
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Karte
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.PagerSpassTheme
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.theme.flaechenmarke
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Der Startbildschirm — das Gegenstück zu `web/src/views/StartView.vue`.
 *
 * Die Seite, auf der man zwischen zwei Schichten steht. Ihre Reihenfolge ist die
 * des Webs und sie ist keine Geschmacksfrage:
 *
 *  1. **Der Empfang** — dasselbe Zeichen und derselbe Schriftzug wie über der
 *     Anmeldung. Wer sich eben angemeldet hat, soll nicht auf einer fremden
 *     Seite ankommen.
 *  2. **Der Dienstausweis** — Stufe, Name, Rang, Balken bis zum Aufstieg.
 *  3. **Was jetzt ansteht** — die Einweisung, solange sie aussteht.
 *  4. **Schicht starten** — die Karte, die etwas wissen will.
 *  5. **Das Menü** — die Wege, die keine Frage stellen.
 *  6. **Der Fuß** — Rechtstexte und der Satz, der klarstellt, was das hier ist.
 *
 * <b>Am Handy steht der Streifen oben, nicht in der Mitte.</b> Am Rechner mittet
 * die Seite ihren Inhalt senkrecht, weil sie als einzige aufs Bild passen soll.
 * Hier ist der Inhalt höher als der Bildschirm — und was über die Oberkante
 * ragt, bekommt man nicht zurück.
 */
@Composable
fun StartSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    landkreise: List<Landkreis> = emptyList(),
    laeuft: Boolean = false,
    beta: Boolean = false,
    version: String? = null,
    beiKatalog: () -> Unit = {},
    beiUmbenennen: (String) -> Unit = {},
    beiBesetzen: (Landkreis?) -> Unit = {},
    /** Besetzen mit Leitstelle und Bereich (v6) — ohne Angabe gilt `beiBesetzen`. */
    beiBesetzenMit: ((Landkreis?, String?, Boolean) -> Unit)? = null,
    /** Welche Leitstelle für welche Kreise disponiert — für Wahl und Fußnote. */
    leitstellen: List<de.pagerspass.pagerspass.netz.Katalogleitstelle> = emptyList(),
    /** Was Deutschland, Österreich und die Schweiz anders nennen. */
    staaten: List<de.pagerspass.pagerspass.netz.Staatsprofil> = emptyList(),
    beiAusbildung: () -> Unit = {},
    beiBeitreten: (String) -> Unit = {},
    beiZuschauen: (String) -> Unit = {},
    beiRechtstext: (String) -> Unit = {},
    beiImWeb: () -> Unit = {},
    beiTagesschicht: () -> Unit = {},
    beiOeffentlicheRunden: () -> Unit = {},
    /** PagerSpass - World, nativ — ohne Angabe führt der Weg wie bisher ins Web. */
    beiWelt: (() -> Unit)? = null,
    mitteilungen: List<de.pagerspass.pagerspass.netz.Betreibermitteilung> = emptyList(),
    gelesen: Set<String> = emptySet(),
    einladungen: List<de.pagerspass.pagerspass.netz.Einladung> = emptyList(),
    beiStartdaten: () -> Unit = {},
    beiGelesen: (String) -> Unit = {},
    beiEinladung: (de.pagerspass.pagerspass.netz.Einladung) -> Unit = {},
    beiLink: (String) -> Unit = {},
    /** Lehrgang, Übungen und Leitstellenbau — native Seiten, siehe `Startweg`. */
    beiStartweg: (Startweg) -> Unit = {},
    /** Der Knopf „Vorlagen" neben „Leitstelle besetzen" — nur mit Konto. */
    vorlagen: (@Composable () -> Unit)? = null,
    /** Was nach den Einladungen steht — die offene Umfrage. */
    zusatz: @Composable ColumnScope.() -> Unit = {},
    /** Die Kacheln der Verwaltung, als Letztes im Menü. */
    kacheln: @Composable ColumnScope.() -> Unit = {},
    /** Die Knöpfe der Fußzeile. */
    fussknoepfe: @Composable () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        beiKatalog()
        beiStartdaten()
    }

    var umbenennen by remember { mutableStateOf(false) }
    var name by remember(konto?.anzeigename) { mutableStateOf(konto?.anzeigename.orEmpty()) }

    var raumcode by rememberSaveable { mutableStateOf("") }

    Seite(modifier = modifier, unterrand = unterrand) {
        Empfangszeile()

        Dienstausweis(
            konto = konto,
            umbenennen = umbenennen,
            name = name,
            laeuft = laeuft,
            beiName = { name = it },
            beiUmbenennen = { umbenennen = it },
            beiSpeichern = {
                beiUmbenennen(name)
                umbenennen = false
            },
        )

        // Die Reihenfolge des Web: Einweisung (Pflicht oder Hinweis), Einladungen,
        // Mitteilungen, Umfrage — was sofort in eine Schicht führt, zuerst.
        val einweisung = konto?.einweisungOffen == true
        val zusammenhang = androidx.compose.ui.platform.LocalContext.current
        var hinweisWeg by remember(konto?.kennung) {
            mutableStateOf(
                konto == null ||
                    de.pagerspass.pagerspass.mobil.Geraeteeinstellungen.ausbildungHinweisWeg(zusammenhang, konto.kennung),
            )
        }
        val ausbildungHinweis = !einweisung && konto != null && konto.erfahrung == 0 && !hinweisWeg
        if (einweisung || ausbildungHinweis) {
            Einweisung(
                beiStart = beiAusbildung,
                laeuft = laeuft,
                pflicht = einweisung,
                beiLehrgang = { beiStartweg(Startweg.Lehrgang) },
                beiWeg = {
                    konto?.let {
                        de.pagerspass.pagerspass.mobil.Geraeteeinstellungen.ausbildungHinweisWegnehmen(zusammenhang, it.kennung)
                    }
                    hinweisWeg = true
                },
            )
        }

        // Rundeneinladungen — dieselbe Bauform. Annehmen tritt sofort bei.
        einladungen.forEach { e ->
            Karte(
                titel = "Einladung — ${e.ort.ifBlank { e.roomCode }}",
                zeichen = Zeichen.WegFreunde,
                text = listOfNotNull(
                    "${e.vonName} lädt dich ein",
                    "${e.spieler}/${e.maxSpieler} Spieler".takeIf { e.maxSpieler > 0 },
                    e.hinweis,
                ).joinToString(" · "),
                knoepfe = {
                    Knopf(
                        aufschrift = if (e.alsZuschauer) "Zusehen" else "Beitreten",
                        beiDruck = { beiEinladung(e) },
                        aktiv = e.annehmbar && !laeuft,
                        kompakt = true,
                    )
                },
            ) {}
        }

        // Betreibermitteilungen als Karten — wie auf dem Startbildschirm des
        // Web. „Gelesen" ist rein lokal; einen Server-Zustand gibt es nicht.
        mitteilungen.filter { it.id !in gelesen }.forEach { m ->
            Karte(titel = m.titel, zeichen = Zeichen.Forum, text = m.text, knoepfe = {
                if (m.link != null) {
                    Knopf(m.linkText ?: "Öffnen", { beiLink(m.link) }, kompakt = true)
                }
                Knopf("Gelesen", { beiGelesen(m.id) }, art = Knopfart.Leise, kompakt = true)
            }) {}
        }

        zusatz()


        if (!einweisung) {
            Ueberschrift("Schicht starten")

            DienstaufnahmeKarte(
                landkreise = landkreise,
                leitstellen = leitstellen,
                staaten = staaten,
                laeuft = laeuft,
                beiBesetzen = { kreis, leitstelle, bereich ->
                    beiBesetzenMit?.invoke(kreis, leitstelle, bereich) ?: beiBesetzen(kreis)
                },
                vorlagen = vorlagen,
            )

            // Die zweite Karte: beitreten statt eröffnen. Sie stand im Web
            // gleichberechtigt neben der ersten — wer verabredet ist, kommt
            // über den Code, wer nicht, über den Kreis.
            Karte(
                titel = "Einer Runde beitreten",
                zeichen = Zeichen.Weiter,
                text = "Raumcode eingeben, Fahrzeug wählen, Melder scharf schalten.",
                knoepfe = {
                    Knopf(
                        aufschrift = "Beitreten",
                        beiDruck = { beiBeitreten(raumcode) },
                        art = Knopfart.Haupt,
                        aktiv = !laeuft && raumcode.length >= 4,
                    )
                    // Der stille Weg daneben: kein Platz, keine Rolle — nur
                    // die Sicht. Übungsleitungen kommen hierüber an den
                    // Regieplatz.
                    Knopf(
                        aufschrift = "Nur zuschauen",
                        beiDruck = { beiZuschauen(raumcode) },
                        art = Knopfart.Leise,
                        aktiv = !laeuft && raumcode.length >= 4,
                    )
                },
            ) {
                Codefeld(
                    wert = raumcode,
                    beiAenderung = { raumcode = it },
                    etikett = "Raumcode",
                )
            }

            Ueberschrift("Menü")
        }

        Startweg.menue(einweisung, konto?.premiumAktiv == true).forEach { eintrag ->
            Wegzeile(
                titel = eintrag.titel,
                unterzeile = eintrag.unterzeile,
                zeichen = eintrag.zeichen,
                schild = eintrag.schild,
                beiDruck = {
                    when (eintrag.weg) {
                        Startweg.Ausbildung -> beiAusbildung()
                        Startweg.Tagesschicht -> beiTagesschicht()
                        Startweg.OeffentlicheRunden -> beiOeffentlicheRunden()
                        Startweg.Lehrgang, Startweg.Uebungen, Startweg.Leitstellenbau ->
                            beiStartweg(eintrag.weg)
                        Startweg.Welt -> beiWelt?.invoke() ?: beiImWeb()
                        else -> beiImWeb()
                    }
                },
            )
        }

        if (!einweisung) kacheln()

        Fuss(beta = beta, version = version, beiRechtstext = beiRechtstext, knoepfe = fussknoepfe)
    }

}


/**
 * Die Empfangszeile des Startbildschirms.
 *
 * Zeichen und Schriftzug kommen aus `Empfang.kt` — dieselben wie über der
 * Anmeldung. Was hier dazukommt, ist die Zeile daneben: <b>am Handy neben dem
 * Schriftzug und nicht darunter.</b> Untereinander wäre der Empfang für
 * jemanden, der die App selbst geöffnet hat, das erste Drittel des Bildschirms.
 */
@Composable
private fun Empfangszeile(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(bottom = Abstand.Klein),
    ) {
        Empfang()

        Text(
            text = "Leitstelle · Funk · Melder",
            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
            color = Farben.TextSehrLeise,
            modifier = Modifier
                .padding(start = Abstand.Winzig)
                .drawBehind {
                    drawLine(
                        color = Farben.Rand,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(start = Abstand.Klein),
        )
    }
}

/**
 * Der Dienstausweis.
 *
 * <b>Er trägt die Amber-Kante</b> („das hier gehört dir") und ist damit einer der
 * drei Orte im ganzen Spiel, an denen sie steht.
 *
 * <b>Der Stufenbalken steht unter Stufe und Name, nicht neben ihnen.</b> Das ist
 * die Umbruchfassung des Webs, und sie ist keine Feinheit: Nebeneinander blieben
 * dem Balken auf einer Handbreit gemessene 40 Punkte — er sah aus wie eine
 * amberfarbene Pille und war als Strecke nicht mehr zu lesen.
 */
@Composable
private fun Dienstausweis(
    konto: Konto?,
    umbenennen: Boolean,
    name: String,
    laeuft: Boolean,
    beiName: (String) -> Unit,
    beiUmbenennen: (Boolean) -> Unit,
    beiSpeichern: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier
            .fillMaxWidth()
            .flaeche()
            .flaechenmarke()
            .padding(Abstand.Normal),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Gross),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(Farben.HauchAmber, CircleShape)
                    .border(2.dp, Farben.Amber, CircleShape),
            ) {
                Text(
                    text = konto?.level?.toString() ?: "–",
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Amber,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.weight(1f),
            ) {
                if (umbenennen) {
                    Feld(
                        wert = name,
                        beiAenderung = { beiName(it.take(24)) },
                        etikett = "Dein Name im Funk",
                        platzhalter = "z. B. Kim",
                        weiterTaste = ImeAction.Done,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            "Übernehmen",
                            beiSpeichern,
                            kompakt = true,
                            aktiv = !laeuft && name.isNotBlank(),
                        )
                        Knopf(
                            "Abbrechen",
                            { beiUmbenennen(false) },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                } else {
                    Etikett("Im Dienst")
                    Text(
                        text = konto?.anzeigename.orEmpty().ifBlank { "—" },
                        style = Schrift.Gross,
                        color = Farben.Text,
                    )
                    Text(
                        text = "${konto?.rang.orEmpty()} · ${zahl(konto?.erfahrung ?: 0)} Punkte",
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                    )
                }
            }
        }

        if (!umbenennen) {
            Fortschritt(anteil = konto?.stufenanteil ?: 0f, text = konto?.stufentext ?: "—")

            // Hier steht nur, was den Ausweis selbst betrifft. Der Weg ins
            // Dienstbuch stand einmal zusätzlich als Knopf an dieser Stelle —
            // den trägt am Handy die Tableiste.
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf(
                    "Umbenennen",
                    { beiUmbenennen(true) },
                    art = Knopfart.Leise,
                    kompakt = true,
                    aktiv = konto != null,
                )
            }
        }
    }
}

/**
 * Der Hinweis auf die Ausbildungsschicht.
 *
 * Er steht **zwischen Ausweis und Menü**, weil er die Antwort auf „was mache ich
 * hier zuerst" ist.
 *
 * <b>Bei der Pflicht gibt es kein „Nicht mehr anzeigen".</b> Was sich wegtippen
 * ließe, wäre keine Pflicht, und der Server wiese jede andere Runde ohnehin ab.
 */
@Composable
private fun Einweisung(
    beiStart: () -> Unit,
    laeuft: Boolean,
    modifier: Modifier = Modifier,
    pflicht: Boolean = true,
    beiLehrgang: (() -> Unit)? = null,
    beiWeg: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier
            .fillMaxWidth()
            .flaeche()
            .flaechenmarke(wartet = true)
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal),
    ) {
        Text(
            text = if (pflicht) "Zuerst die Ausbildungsschicht" else "Zum ersten Mal in einer Leitstelle?",
            style = Schrift.Gross,
            color = Farben.Text,
        )
        Text(
            text = "Die Ausbildungsschicht erklärt dir Melder, FMS, Notruf und Funk in rund " +
                "zwanzig Minuten — allein, mit Bot-Besatzungen, ohne Wertung." +
                if (pflicht) {
                    " Sie gehört für jedes neue Konto an den Anfang; danach stehen alle Runden offen. " +
                        "Wer lieber liest, besteht stattdessen einen Grundlagen-Lehrgang — Leitstelle " +
                        "oder Fahrzeug — und ist damit ebenso durch."
                } else {
                    ""
                },
            style = Schrift.Klein,
            color = Farben.TextSehrLeise,
        )
        if (pflicht && beiLehrgang != null) Textweg("Zu den Lehrgängen", beiLehrgang)
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            // Nicht „Ausbildungsschicht starten": Der Menüweg darunter heißt so,
            // und zwei Knöpfe mit demselben Wort auf einem Bildschirm sind für
            // jede Suche zwei Treffer, von denen einer der falsche ist.
            Knopf("Ausbildung starten", beiStart, art = Knopfart.Haupt, aktiv = !laeuft)
            if (!pflicht && beiWeg != null) {
                Knopf("Nicht mehr anzeigen", beiWeg, art = Knopfart.Leise)
            }
        }
    }
}

/**
 * Der Fuß.
 *
 * <b>Der erste Satz ist keine Höflichkeit.</b> „Ein Spiel, kein Einsatzmittel"
 * steht auf jedem Bildschirm, von dem aus jemand die Anwendung zum ersten Mal
 * sieht, und die 112 daneben ist der Grund dafür.
 *
 * <b>Impressum und Datenschutz müssen von überall her erreichbar sein</b> (§ 5
 * DDG). Der Startbildschirm ist der eine Ort, an dem jeder Weg vorbeiführt.
 */
@Composable
private fun Fuss(
    beta: Boolean,
    version: String?,
    beiRechtstext: (String) -> Unit,
    modifier: Modifier = Modifier,
    knoepfe: @Composable () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Abstand.SehrGross)
            .drawBehind {
                drawLine(
                    color = Farben.Rand,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(top = Abstand.Gross),
    ) {
        knoepfe()

        Row(
            horizontalArrangement = Arrangement.spacedBy(
                Abstand.Klein,
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ein Spiel, kein Einsatzmittel. Bei echten Notfällen: 112.",
                style = Schrift.Klein.copy(letterSpacing = 0.06.em),
                color = Farben.TextSehrLeise,
                textAlign = TextAlign.Center,
            )
            if (beta) Marke("Beta", farbe = Farben.AmberHell)
        }

        if (version != null) {
            Text(
                text = version,
                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                color = Farben.TextSehrLeise,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            Textweg("Impressum", { beiRechtstext("impressum") })
            Textweg("Datenschutz", { beiRechtstext("datenschutz") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            Textweg("Nutzungsbedingungen", { beiRechtstext("nutzungsbedingungen") })
            Textweg("AGB", { beiRechtstext("agb") })
        }
    }
}

/** Wohin der Startbildschirm führen kann. */
enum class Startweg {
    Ausbildung,
    Tagesschicht,
    OeffentlicheRunden,
    Lehrgang,
    Welt,
    Uebungen,
    Leitstellenbau,
    ;

    /** Ein Menüeintrag: Titel, Erklärzeile, Zeichen und was rechts steht. */
    data class Eintrag(
        val weg: Startweg,
        val titel: String,
        val unterzeile: String,
        val zeichen: ImageVector,
        val schild: String? = null,
    )

    companion object {
        /**
         * Das Menü — die Wege, die keine Frage stellen.
         *
         * <b>Solange die Einweisungspflicht aussteht, bleibt alles weg außer der
         * Ausbildung</b> — nicht ausgegraut: Ein Weg, der sichtbar dasteht und
         * nur nicht gedrückt werden kann, müsste erklären warum, und genau das
         * tut die Pflicht-Karte darüber schon.
         *
         * <b>Lehrgang, Übungen und Leitstellenbau sind native Seiten</b>
         * (`beiStartweg`); World führt bisher ins Web, und die Unterzeile sagt
         * das, statt es den Nutzer beim Antippen herausfinden zu lassen.
         */
        fun menue(einweisungPflicht: Boolean, premium: Boolean): List<Eintrag> {
            val ausbildung = Eintrag(
                weg = Ausbildung,
                titel = "Ausbildungsschicht",
                unterzeile = "Melder, FMS, Notruf und Funk — allein, ohne Wertung",
                zeichen = Zeichen.Lehrgang,
            )
            if (einweisungPflicht) return listOf(ausbildung)

            return listOf(
                ausbildung,
                Eintrag(
                    weg = Tagesschicht,
                    titel = "Schicht des Tages",
                    unterzeile = "Gewertet, für alle dieselbe",
                    zeichen = Zeichen.Uebung,
                ),
                Eintrag(
                    weg = OeffentlicheRunden,
                    titel = "Öffentliche Runden",
                    unterzeile = "Wo gerade jemand Verstärkung sucht",
                    zeichen = Zeichen.Gemeinschaft,
                ),
                Eintrag(
                    weg = Lehrgang,
                    titel = "Lehrgang",
                    unterzeile = "Lesen, üben, prüfen",
                    zeichen = Zeichen.Wiki,
                ),
                Eintrag(
                    weg = Welt,
                    titel = "World",
                    unterzeile = "Eine Karte, alle Leitstellen",
                    zeichen = Zeichen.Welt,
                    schild = if (premium) null else "Premium",
                ),
                Eintrag(
                    weg = Uebungen,
                    titel = "Übungen",
                    unterzeile = "Eine Lage vorher bauen und mehrmals fahren",
                    zeichen = Zeichen.Lage,
                ),
                Eintrag(
                    weg = Leitstellenbau,
                    titel = "Leitstellenbau",
                    unterzeile = "Eigene Wachen, Plätze und Rufnamen",
                    zeichen = Zeichen.Karte,
                ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080B10, heightDp = 1400)
@Composable
private fun StartVorschau() {
    PagerSpassTheme {
        StartSeite(
            konto = Konto(
                kennung = "x",
                anzeigename = "Kim",
                level = 12,
                rang = "Oberbrandmeister/in",
                erfahrung = 2785,
                bisZumNaechsten = 215,
                schwelle = 2400,
            ),
            landkreise = listOf(
                Landkreis(
                    id = "westerwaldkreis",
                    name = "Westerwaldkreis",
                    kreisstadt = "Montabaur",
                    hatDaten = true,
                    wachen = 42,
                    maxSpieler = 30,
                    bundesland = "Rheinland-Pfalz",
                ),
            ),
            version = "5.0.0.35",
        )
    }
}
