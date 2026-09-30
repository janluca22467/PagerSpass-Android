package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Kreisstand
import de.pagerspass.pagerspass.netz.Fremdprofil
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.schmuck.Profilbanner
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Was man auf einem fremden Profil tun kann — und auf dem eigenen.
 *
 * Die Griffe sind dieselben wie im Web (`views/freunde/ProfilView.vue`); was
 * davon erscheint, entscheidet der Stand der Beziehung, den der Server gleich
 * mitschickt.
 */
class Profilgriffe(
    val laden: (String) -> Unit = {},
    val anfragen: (String) -> Unit = {},
    val antworten: (String, Boolean) -> Unit = { _, _ -> },
    val beenden: (String) -> Unit = {},
    val blockieren: (String) -> Unit = {},
    val melden: (String, String) -> Unit = { _, _ -> },
    val gespraech: (Fremdprofil) -> Unit = {},
    val dazuschalten: (String) -> Unit = {},
    /** In die eigene Wache einladen — nur, wer dort führen darf, bekommt den Knopf. */
    val inWacheEinladen: ((String) -> Unit)? = null,
    val eigenesProfil: () -> Unit = {},
    val quittieren: (Long) -> Unit = {},
    val eintragOeffnen: (Long) -> Unit = {},
    val meldungWeg: () -> Unit = {},
    val zurueck: () -> Unit = {},
)

/**
 * Das Profil eines anderen — oder das eigene, wie andere es sehen.
 *
 * <b>Dasselbe Banner wie auf der eigenen Profilseite</b> (`Profilbanner`): Wappen,
 * Rahmen, Kopfmuster, Bild. Wer sich ein Muster kauft, soll es hier so sehen,
 * wie jeder andere es sieht.
 *
 * <b>Beenden, Blockieren und Melden stehen hinter einer Klappe.</b> Nicht, weil
 * sie unwichtig wären — in einer Reihe mit „Nachricht schreiben" sahen sie genauso
 * groß aus wie das, weswegen man ein Profil öffnet.
 */
@Composable
fun FremdprofilSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    benutzername: String = "",
    kreis: Kreisstand = Kreisstand(),
    server: String = "",
    griffe: Profilgriffe = Profilgriffe(),
    brettgriffe: Brettgriffe = Brettgriffe(),
) {
    LaunchedEffect(benutzername) { griffe.laden(benutzername) }

    var verwaltenOffen by remember(benutzername) { mutableStateOf(false) }
    var beendenGefragt by remember(benutzername) { mutableStateOf(false) }
    var blockierenGefragt by remember(benutzername) { mutableStateOf(false) }
    var meldenOffen by remember(benutzername) { mutableStateOf(false) }
    var meldenGrund by remember(benutzername) { mutableStateOf("") }
    var eingeladen by remember(benutzername) { mutableStateOf(false) }

    // Nur das Profil zeigen, das auch gemeint ist — sonst stünde beim Wechsel
    // von einem Profil zum nächsten einen Augenblick lang das alte da.
    val profil = kreis.profil.inhalt?.takeIf { it.benutzername.equals(benutzername, true) }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Profil",
            unterzeile = profil?.let { if (it.ich) "So sehen dich die anderen" else "@${it.benutzername}" },
            knoepfe = { Knopf("Zurück", griffe.zurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = kreis.profil.laedt,
            fehler = kreis.profil.fehler,
            inhalt = profil,
            beiErneut = { griffe.laden(benutzername) },
        ) { p ->
            Profilbanner(
                kennung = p.kennung,
                anzeigename = p.anzeigename,
                benutzername = p.benutzername,
                augenbraue = p.titel?.takeIf { it.isNotBlank() },
                wappen = p.wappen,
                wappenfarbe = p.wappenfarbe,
                kopfmuster = p.kopfmuster,
                profilrahmen = p.profilrahmen,
                bildAdresse = bildweg(server, p.profilbild),
                premium = p.premium,
                teammitglied = p.teammitglied,
                imDienst = p.anwesenheit != null,
                marken = listOfNotNull(
                    p.rang?.let { "$it · Stufe ${p.level ?: 0}" },
                    p.gemeinschaft?.let { "🏠 $it" },
                    if (p.anwesenheit != null) "Im Dienst" else null,
                ),
                werte = if (p.schichten != null) {
                    listOf(
                        "Schichten" to zahl(p.schichten),
                        "Einsätze" to zahl(p.einsaetze ?: 0),
                        "Dabei seit" to tag(p.dabeiSeit),
                    )
                } else {
                    emptyList()
                },
                knoepfe = if (p.ich) {
                    { Knopf("Profil gestalten", griffe.eigenesProfil, art = Knopfart.Haupt, kompakt = true) }
                } else {
                    null
                },
                inhalt = {
                    Text(
                        text = lage(p.anwesenheit, p.zuletztGesehen)
                            .ifBlank { "Dabei seit ${tag(p.dabeiSeit)}" },
                        style = Schrift.Klein,
                        color = if (p.anwesenheit != null) Farben.GruenHell else Farben.TextLeise,
                    )
                },
            )

            if (!p.ich) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (p.befreundet) {
                        Knopf("Nachricht schreiben", { griffe.gespraech(p) }, art = Knopfart.Haupt)
                    }
                    if (p.stand == null) {
                        Knopf(
                            "Freundschaft anfragen",
                            { griffe.anfragen(p.kennung) },
                            art = Knopfart.Haupt,
                            aktiv = !kreis.laeuft,
                        )
                    }
                    if (p.stand == "Angefragt" && !p.vonMir) {
                        Knopf("Anfrage annehmen", { griffe.antworten(p.kennung, true) }, art = Knopfart.Haupt)
                        Knopf("Ablehnen", { griffe.antworten(p.kennung, false) }, art = Knopfart.Leise)
                    } else if (p.stand == "Angefragt") {
                        Marke("Anfrage läuft")
                    }
                    if (p.anwesenheit?.platzFrei == true) {
                        Knopf("Dazuschalten", { griffe.dazuschalten(p.anwesenheit.roomCode) })
                    }
                    griffe.inWacheEinladen?.let { einladen ->
                        Knopf(
                            if (eingeladen) "Eingeladen" else "In die Wache einladen",
                            {
                                einladen(p.kennung)
                                eingeladen = true
                            },
                            aktiv = !eingeladen,
                        )
                    }
                }

                Textweg(
                    (if (verwaltenOffen) "▾ " else "▸ ") + "Diesen Kontakt verwalten",
                    { verwaltenOffen = !verwaltenOffen },
                    farbe = Farben.TextLeise,
                )
                if (verwaltenOffen) {
                    Kasten(abstandInnen = Abstand.Klein) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        ) {
                            if (p.befreundet) {
                                Knopf(
                                    if (beendenGefragt) "Wirklich beenden?" else "Freundschaft beenden",
                                    {
                                        if (!beendenGefragt) {
                                            beendenGefragt = true
                                        } else {
                                            beendenGefragt = false
                                            griffe.beenden(p.kennung)
                                        }
                                    },
                                    art = if (beendenGefragt) Knopfart.Gefahr else Knopfart.Leise,
                                    kompakt = true,
                                )
                            }
                            Knopf(
                                if (blockierenGefragt) "Wirklich blockieren?" else "Blockieren",
                                {
                                    if (!blockierenGefragt) {
                                        blockierenGefragt = true
                                    } else {
                                        blockierenGefragt = false
                                        griffe.blockieren(p.kennung)
                                    }
                                },
                                art = if (blockierenGefragt) Knopfart.Gefahr else Knopfart.Leise,
                                kompakt = true,
                            )
                            Knopf("Melden", { meldenOffen = !meldenOffen }, art = Knopfart.Leise, kompakt = true)
                        }
                        SehrLeise(
                            "Blockieren beendet Freundschaft, Einladungen und Nachrichten in beide " +
                                "Richtungen. Ihr findet und kontaktiert euch danach nicht mehr; aufheben " +
                                "kannst du die Blockade später in deiner Kontaktliste. Melden ist davon " +
                                "unabhängig und sendet den angegebenen Grund an die Moderation.",
                        )
                        if (meldenOffen) {
                            Feld(
                                wert = meldenGrund,
                                beiAenderung = { meldenGrund = it.take(500) },
                                etikett = "Worum geht es?",
                                platzhalter = "Kurz beschreiben, was passiert ist …",
                                einzeilig = false,
                            )
                            Knopf(
                                "Meldung absenden",
                                {
                                    griffe.melden(p.kennung, meldenGrund.trim())
                                    meldenGrund = ""
                                    meldenOffen = false
                                },
                                art = Knopfart.Haupt,
                                aktiv = meldenGrund.isNotBlank(),
                                kompakt = true,
                            )
                        }
                    }
                }
            }

            Meldungszeile(kreis.meldung, kreis.hinweis, griffe.meldungWeg)

            p.vorstellung?.takeIf { it.isNotBlank() }?.let {
                Kasten { Text(it, style = Schrift.Normal, color = Farben.Text) }
            }

            if (p.vitrine.isNotEmpty()) {
                Abschnitt("Vitrine") {
                    p.vitrine.forEach { a ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .flaeche(ecke = 9.dp)
                                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                        ) {
                            Text("🏅", style = Schrift.Gross)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.titel, style = Schrift.Normal, color = Farben.Text)
                                SehrLeise(a.beschreibung)
                            }
                        }
                    }
                }
            }

            Abschnitt("Am Brett") {
                if (kreis.profilBrett.isEmpty()) {
                    Leerhinweis(
                        if (p.ich) "Du hast noch nichts angeschlagen." else "Hier steht noch nichts für dich.",
                    )
                }
                kreis.profilBrett.forEach { e ->
                    EintragKarte(
                        eintrag = e,
                        server = server,
                        beiQuittieren = { griffe.quittieren(e.nr) },
                        beiOeffnen = { griffe.eintragOeffnen(e.nr) },
                        griffe = brettgriffe,
                    )
                }
            }
        }
    }
}
