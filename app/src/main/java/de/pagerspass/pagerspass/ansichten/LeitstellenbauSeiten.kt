package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Werkstand
import de.pagerspass.pagerspass.netz.Baufunkgruppe
import de.pagerspass.pagerspass.netz.EIGEN_PRAEFIX
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.HIORG_NAME
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Kreiswache
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Leitstellenvorlage
import de.pagerspass.pagerspass.netz.ORGANISATIONEN
import de.pagerspass.pagerspass.netz.ORG_KURZ
import de.pagerspass.pagerspass.netz.ORG_NAME
import de.pagerspass.pagerspass.netz.PRAEFIX_HIORG
import de.pagerspass.pagerspass.netz.PRAEFIX_ORG
import de.pagerspass.pagerspass.netz.Vorlageneinstellungen
import de.pagerspass.pagerspass.netz.Wachenwahl
import de.pagerspass.pagerspass.netz.istEigeneWache
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Der Leitstellenbau — das Gegenstück zu `web/src/views/LeitstellenbauView.vue`.
 *
 * <b>Im Web steht am Handy nur ein Satz</b> („am Schreibtisch gebaut"), weil
 * der Editor dort eine Karte zum Ziehen und zwei Spalten braucht. Die App baut
 * ihn trotzdem — ohne Karte: Die Wachen stehen als Liste zum Anhaken, eine
 * selbst gebaute Wache bekommt ihre Lage über zwei Zahlenfelder. Das ist
 * genau der Weg, den das Web ohne Kartenfreigabe auch anbietet.
 *
 * Eine Sandkastenrunde wird <b>nicht gewertet</b>: keine Erfahrung, keine
 * Rangliste, keine Saisonwertung. Ins Dienstbuch kommt sie trotzdem.
 */
@Composable
fun LeitstellenbauSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Werkstand = Werkstand(),
    katalogBereit: Boolean = false,
    beiLaden: (Boolean) -> Unit = {},
    beiNeu: () -> Unit = {},
    beiOeffnen: (String) -> Unit = {},
    beiLoeschen: (String) -> Unit = {},
    beiUebernehmen: (String) -> Unit = {},
    beiEroeffnen: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden(false) }
    val frei = stand.frei("Sandkasten")

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Deine eigene Leitstelle",
            unterzeile = "Eigene Wachen, Plätze und Rufnamen",
            knoepfe = { Knopf("← Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        if (!frei) {
            Rueckmeldung(
                null,
                "Eine eigene Leitstelle gibt es ab Stufe 20 (${stand.abRang("Sandkasten")}). " +
                    "Bis dahin kannst du hier zusehen, aber nichts bauen.",
            )
        }

        Knopf("Neue Leitstelle", beiNeu, art = Knopfart.Haupt, aktiv = katalogBereit && frei, breit = true)

        Rueckmeldung(stand.meldung, stand.fehler)

        Codeuebernahme(
            etikett = "Leitstelle eines anderen übernehmen",
            erklaerung = "Du bekommst eine eigene Kopie. Baut der andere seinen Kreis um, bleibt deiner, wie er ist.",
            aktiv = frei && !stand.laeuft,
            beiUebernehmen = beiUebernehmen,
        )

        Bereich(
            laedt = stand.vorlagen.ersteLadung,
            fehler = stand.vorlagen.fehler,
            inhalt = stand.vorlagen.inhalt,
            beiErneut = { beiLaden(true) },
        ) { liste ->
            if (liste.isEmpty()) Leerhinweis("Noch keine Leitstelle. „Neue Leitstelle\" legt die erste an.")
            liste.forEach { v ->
                Kasten(abstandInnen = Abstand.Klein) {
                    Text(v.name.ifBlank { "Ohne Namen" }, style = Schrift.Gross, color = Farben.Text)
                    if (v.beschreibung.isNotBlank()) Leise(v.beschreibung)
                    SehrLeise(
                        "${v.wachen} ${if (v.wachen == 1) "Wache" else "Wachen"} · " +
                            (if (v.maxSpieler > 0) "${v.maxSpieler} Plätze" else "Plätze wie im Kreis") +
                            " · Code ${v.code}",
                        mono = true,
                    )
                    Pillenreihe {
                        Knopf("Runde eröffnen", { beiEroeffnen(v.id) }, art = Knopfart.Haupt, aktiv = frei && !stand.laeuft, kompakt = true)
                        Knopf("Bearbeiten", { beiOeffnen(v.id) }, kompakt = true)
                        Loeschknopf({ beiLoeschen(v.id) }, aktiv = !stand.laeuft)
                    }
                }
            }
        }

        SehrLeise(
            "Eine Sandkastenrunde wird nicht gewertet: keine Erfahrung, keine Rangliste, " +
                "keine Saisonwertung. Ins Dienstbuch kommt sie trotzdem.",
        )
    }
}

/** Ein frischer Entwurf im ersten Kreis mit echten Wachen. */
fun neueLeitstelle(katalog: Katalog?): Leitstellenvorlage {
    val kreise = katalog?.landkreise.orEmpty()
    val erster = kreise.firstOrNull { it.hatDaten } ?: kreise.firstOrNull()
    return Leitstellenvorlage(
        landkreisId = erster?.id,
        ort = erster?.kreisstadt ?: "Heidefeld",
        leitstelle = erster?.let { leitstellenname(katalog, it) } ?: "Leitstelle Heidefeld",
        einstellungen = Vorlageneinstellungen(),
    )
}

private fun leitstellenname(katalog: Katalog?, kreis: Landkreis): String =
    katalog?.leitstellen?.firstOrNull { kreis.id in it.kreise }?.name ?: "Leitstelle ${kreis.name}"

private const val MAX_WACHEN = 60
private const val LISTE_HOECHSTENS = 60

/**
 * Der Editor einer eigenen Leitstelle.
 *
 * Abschnitte wie im Web: die Leitstelle, Plätze, bespielte Wachen,
 * Funkgruppen, feste Fahrzeugrufnamen, Rufnamen, Kennzahlen — und die Regeln
 * der Runde, die im Web in der Lobby nachgestellt werden und hier gleich mit.
 */
@Composable
fun LeitstelleEditorSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Werkstand = Werkstand(),
    katalog: Katalog? = null,
    beiAendern: (Leitstellenvorlage) -> Unit = {},
    beiKreis: (String?) -> Unit = {},
    beiSichern: () -> Unit = {},
    beiEroeffnen: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    var wahl by remember { mutableStateOf<String?>(null) }
    var suche by rememberSaveable { mutableStateOf("") }
    val frei = stand.frei("Sandkasten")

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = stand.leitstelle.inhalt?.name?.ifBlank { null } ?: "Neue Leitstelle",
            unterzeile = "Leitstellenbau",
            knoepfe = { Knopf("← Zur Liste", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = stand.leitstelle.laedt,
            fehler = stand.leitstelle.fehler,
            inhalt = stand.leitstelle.inhalt,
        ) { v ->
            val kreise = katalog?.landkreise.orEmpty()
            val kreis = kreise.firstOrNull { it.id == v.landkreisId }
            val abzug = stand.kreiswachen.inhalt.orEmpty()
            val gewaehlt = v.wachen.associateBy { it.kennung }
            // Der Staat des Kreises — ohne Kreis Deutschland. Er entscheidet, welche
            // Fahrzeuge zur Wahl stehen (in Tirol kein HLF 20) und wie Rufnamen klingen.
            val staat = werkstaatVon(kreis?.bundesland)
            val vorlagenImStaat = katalog?.fahrzeuge.orEmpty().filter { it.imStaat(staat) }

            fun wahlAendern(kennung: String, neu: (Wachenwahl) -> Wachenwahl) =
                beiAendern(v.copy(wachen = v.wachen.map { if (it.kennung == kennung) neu(it) else it }))

            fun art(w: Wachenwahl): String? =
                w.organisation ?: abzug.firstOrNull { it.kennung == w.kennung }?.organisation

            fun naechsteZug(): Int {
                val belegt = v.wachen.map { it.zugnummer }.toSet()
                var n = 1
                while (n in belegt && n < 99) n++
                return n
            }

            // --------------------------------------------------- Die Leitstelle
            Abschnitt("Die Leitstelle") {
                Kasten(abstandInnen = Abstand.Klein) {
                    Feld(v.name, { beiAendern(v.copy(name = it.take(60))) }, etikett = "Name", platzhalter = "Meine Wache")
                    Feld(
                        v.beschreibung,
                        { beiAendern(v.copy(beschreibung = it.take(400))) },
                        etikett = "Beschreibung",
                        platzhalter = "Wofür ist sie gedacht?",
                    )
                    Wahlfeld("Landkreis", kreis?.let { it.name + if (it.hatDaten) "" else " (ohne Wachendaten)" }, { wahl = "kreis" })
                    Feld(v.ort, { beiAendern(v.copy(ort = it.take(40))) }, etikett = "Ortsname im Funk")
                    Feld(v.leitstelle, { beiAendern(v.copy(leitstelle = it.take(60))) }, etikett = "Name der Leitstelle")
                    Etikett("Woher die Einsätze kommen")
                    Pillenreihe {
                        listOf("Zufall" to "Der Server würfelt sie", "Frei" to "Die Leitstelle denkt sie sich aus").forEach { (m, text) ->
                            Pille(text, (v.einstellungen.mode ?: "Zufall") == m, {
                                beiAendern(v.copy(einstellungen = v.einstellungen.copy(mode = m)))
                            })
                        }
                    }
                }
            }

            // ------------------------------------------------------------ Plätze
            val vorgabe = kreis?.maxSpieler?.takeIf { it > 0 } ?: 32
            Abschnitt("Plätze") {
                SehrLeise("Menschen und Bots zusammen. Null heißt: so viele, wie der Kreis von sich aus trägt — hier $vorgabe.")
                Stufenwahl(
                    etikett = "Plätze",
                    wert = v.maxSpieler,
                    beiAenderung = { beiAendern(v.copy(maxSpieler = it)) },
                    schritt = 2,
                    von = 0,
                    bis = 400,
                    anzeige = { if (it > 0) "$it" else "$vorgabe (Vorgabe)" },
                )
                if (v.maxSpieler in 1..23) {
                    Rueckmeldung(null, "Weniger als 24 Plätze gibt es nicht — der Server hebt kleinere Zahlen auf 24 an.")
                }
            }

            // ------------------------------------------------- Bespielte Wachen
            Abschnitt("Bespielte Wachen") {
                SehrLeise(
                    "Ohne Auswahl verteilt das Spiel die Fahrzeuge wie sonst auch über die größten " +
                        "Standorte des Kreises. Wählst du welche, gelten nur die — und die Einsätze " +
                        "entstehen um sie herum.",
                )
                Pillenreihe {
                    Knopf("Wache bauen", {
                        if (v.wachen.size >= MAX_WACHEN) return@Knopf
                        val eigene = v.wachen.filter { istEigeneWache(it.kennung) }
                        val mitte = (abzug.map { it.lat to it.lon } + eigene.mapNotNull { w -> w.lat?.let { it to (w.lon ?: 0.0) } })
                            .takeIf { it.isNotEmpty() }
                            ?.let { l -> l.map { it.first }.average() to l.map { it.second }.average() }
                            ?: (52.6 to 10.1)
                        val versatz = (eigene.size % 6) * 0.004
                        val neu = Wachenwahl(
                            kennung = EIGEN_PRAEFIX + java.util.UUID.randomUUID().toString().replace("-", "").take(13),
                            name = "",
                            zugnummer = naechsteZug(),
                            lat = runde5(mitte.first + versatz),
                            lon = runde5(mitte.second + versatz),
                            organisation = "Feuerwehr",
                        )
                        beiAendern(v.copy(wachen = v.wachen + neu))
                        suche = ""
                    }, aktiv = v.wachen.size < MAX_WACHEN, kompakt = true)
                    if (v.wachen.isNotEmpty()) {
                        Knopf("Leeren", {
                            beiAendern(v.copy(wachen = emptyList(), festeFunkrufnamen = emptyMap()))
                        }, art = Knopfart.Leise, kompakt = true)
                    }
                }
                SehrLeise(
                    "„Wache bauen\" stellt eine eigene in die Mitte des Kreises — Name, Art, Träger " +
                        "und Standort stehen dann in ihrer Zeile. Der Name entscheidet mit, wer dort " +
                        "Dienst tut: Was „Hauptwache\", „Berufsfeuerwehr\" oder „Feuer- und " +
                        "Rettungswache\" heißt, ist rund um die Uhr besetzt, ein Gerätehaus nicht.",
                )
                if (v.wachen.size >= MAX_WACHEN) {
                    Rueckmeldung(null, "$MAX_WACHEN Wachen sind das Höchste je Leitstelle. Für eine weitere muss erst eine weichen.")
                }
                when {
                    stand.kreiswachen.laedt -> Ladezeile("Die Wachen des Kreises werden geholt …")
                    stand.kreiswachen.geladen && abzug.isEmpty() -> Rueckmeldung(
                        "Für diesen Landkreis liegen noch keine Wachendaten vor. Bauen kannst du hier " +
                            "trotzdem — was du selbst hinstellst, braucht keinen Abzug. Ohne eigene Wachen " +
                            "läuft die Runde mit dem erfundenen Standardbereich.",
                        null,
                    )
                }

                val eigene = v.wachen.filter { istEigeneWache(it.kennung) }
                SehrLeise(
                    "${v.wachen.size} bespielt" +
                        (if (eigene.isNotEmpty()) " · ${eigene.size} selbst gebaut" else "") +
                        " · ${abzug.size} im Abzug",
                    mono = true,
                )
                val ohne = if (v.wachen.isEmpty()) emptyList() else
                    listOf("Feuerwehr", "Rettungsdienst").filter { o -> v.wachen.none { art(it) == o } }
                if (ohne.isNotEmpty()) {
                    Rueckmeldung(
                        "Für ${ohne.joinToString(" und ") { ORG_NAME[it] ?: it }} hast du keine Wache " +
                            "gewählt — diese Fahrzeuge stehen dann auf den Standardwachen des Kreises.",
                        null,
                    )
                }

                // Die Karte: Wachen des Kreises antippen, eigene hinstellen und
                // verschieben. Die Liste darunter bleibt als Rückfall.
                Leitstellenbaukarte(
                    abzug = abzug,
                    wachen = v.wachen,
                    voll = v.wachen.size >= MAX_WACHEN,
                    beiUmschalten = { a ->
                        if (a.kennung in gewaehlt) {
                            beiAendern(
                                v.copy(
                                    wachen = v.wachen.filter { it.kennung != a.kennung },
                                    festeFunkrufnamen = v.festeFunkrufnamen.filterKeys { !it.startsWith("${a.kennung}:") },
                                ),
                            )
                        } else if (v.wachen.size < MAX_WACHEN) {
                            beiAendern(v.copy(wachen = v.wachen + Wachenwahl(kennung = a.kennung, name = a.name, zugnummer = naechsteZug())))
                        }
                    },
                    beiVerschieben = { kennung, lat, lon ->
                        // NaN heißt: zurück an den Platz aus dem Abzug.
                        wahlAendern(kennung) {
                            if (lat.isNaN()) it.copy(lat = null, lon = null) else it.copy(lat = lat, lon = lon)
                        }
                    },
                    beiBauen = { lat, lon ->
                        beiAendern(
                            v.copy(
                                wachen = v.wachen + Wachenwahl(
                                    kennung = EIGEN_PRAEFIX + java.util.UUID.randomUUID().toString().replace("-", "").take(13),
                                    name = "",
                                    zugnummer = naechsteZug(),
                                    lat = lat,
                                    lon = lon,
                                    organisation = "Feuerwehr",
                                ),
                            ),
                        )
                    },
                )

                eigene.forEach { w ->
                    EigeneWache(
                        w = w,
                        beiAendern = { neu -> wahlAendern(w.kennung) { neu } },
                        beiTraeger = { wahl = "traeger:${w.kennung}" },
                        beiAbreissen = {
                            beiAendern(
                                v.copy(
                                    wachen = v.wachen.filter { it.kennung != w.kennung },
                                    festeFunkrufnamen = v.festeFunkrufnamen.filterKeys { !it.startsWith("${w.kennung}:") },
                                ),
                            )
                        },
                    )
                }

                if (abzug.isNotEmpty()) {
                    Feld(suche, { suche = it }, platzhalter = "Wache suchen …")
                    val wort = suche.trim().lowercase()
                    val passend = abzug.filter { a ->
                        wort.isEmpty() || listOf(a.name, ORG_NAME[a.organisation].orEmpty(), HIORG_NAME[a.traeger].orEmpty())
                            .joinToString(" ").lowercase().contains(wort)
                    }.sortedByDescending { it.kennung in gewaehlt }
                    if (passend.isEmpty()) SehrLeise("Keine Wache passt zu „$suche\".")
                    passend.take(LISTE_HOECHSTENS).forEach { a ->
                        Abzugswache(
                            a = a,
                            wahl = gewaehlt[a.kennung],
                            beiUmschalten = {
                                if (a.kennung in gewaehlt) {
                                    beiAendern(
                                        v.copy(
                                            wachen = v.wachen.filter { it.kennung != a.kennung },
                                            festeFunkrufnamen = v.festeFunkrufnamen.filterKeys { !it.startsWith("${a.kennung}:") },
                                        ),
                                    )
                                } else {
                                    beiAendern(
                                        v.copy(wachen = v.wachen + Wachenwahl(kennung = a.kennung, name = a.name, zugnummer = naechsteZug())),
                                    )
                                }
                            },
                            beiAendern = { neu -> wahlAendern(a.kennung) { neu } },
                        )
                    }
                    if (passend.size > LISTE_HOECHSTENS) {
                        SehrLeise("… und ${passend.size - LISTE_HOECHSTENS} weitere — die Suche findet sie.")
                    }
                }
            }

            // ---------------------------------------------------- Funkgruppen
            Abschnitt("Funkgruppen") {
                SehrLeise("Diese Kreisgruppen stehen in jeder Runde dieser Leitstelle bereit. Ohne Eintrag funken alle auf einem gemeinsamen Kanal.")
                v.funkgruppen.forEachIndexed { nr, g ->
                    fun setzen(neu: Baufunkgruppe) =
                        beiAendern(v.copy(funkgruppen = v.funkgruppen.toMutableList().also { it[nr] = neu }))
                    Kasten(abstandInnen = Abstand.Winzig, farbe = Farben.FlaecheHoch) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Feld(g.nummer, { setzen(g.copy(nummer = it.take(12))) }, etikett = "Nummer", platzhalter = "3301", modifier = Modifier.weight(1f))
                            Feld(g.name, { setzen(g.copy(name = it.take(40))) }, etikett = "Name", platzhalter = "Feuerwehr Landkreis …", modifier = Modifier.weight(2f))
                        }
                        Pillenreihe {
                            ORGANISATIONEN.forEach { o ->
                                val an = o in g.organisationen
                                Pille(ORG_KURZ[o] ?: o, an, {
                                    setzen(g.copy(organisationen = if (an) g.organisationen - o else g.organisationen + o))
                                }, farbe = orgFarbe(o))
                            }
                            Pille("Führung", g.fuehrung, {
                                val an = !g.fuehrung
                                beiAendern(
                                    v.copy(
                                        funkgruppen = v.funkgruppen.mapIndexed { i, x -> x.copy(fuehrung = i == nr && an) },
                                    ),
                                )
                            })
                            Knopf("✕", {
                                beiAendern(v.copy(funkgruppen = v.funkgruppen.filterIndexed { i, _ -> i != nr }))
                            }, art = Knopfart.Leise, kompakt = true)
                        }
                    }
                }
                Knopf("+ Funkgruppe", {
                    val id = "vorlage-${java.lang.Long.toString(System.currentTimeMillis(), 36)}-" +
                        java.util.UUID.randomUUID().toString().take(4)
                    beiAendern(v.copy(funkgruppen = v.funkgruppen + Baufunkgruppe(id = id)))
                }, art = Knopfart.Leise, aktiv = v.funkgruppen.size < 12, kompakt = true)
            }

            // ------------------------------------------ Feste Fahrzeugrufnamen
            FesteRufnamen(v, abzug, vorlagenImStaat, beiAendern, ::art)

            // ------------------------------------------------------- Rufnamen
            val beispielzug = v.wachen.firstOrNull()?.zugnummer ?: 1
            fun schluessel(f: Fahrzeugvorlage) = if (f.hiOrg.isNotBlank() && f.hiOrg != "Keine") f.hiOrg else f.organisation
            fun vorgabewort(s: String) = PRAEFIX_ORG[s] ?: PRAEFIX_HIORG[s] ?: "Einheit"
            fun kennzahl(f: Fahrzeugvorlage) = v.kennzahlen[f.id]?.trim()?.ifBlank { null } ?: f.kennzahl
            fun landeswort(f: Fahrzeugvorlage, s: String): String {
                // Jenseits der Grenze ruft das Fahrzeug selbst — „Tank", „Pumpe", „Christophorus".
                if (staat != "Deutschland") return f.rufwort ?: f.typ
                if (kreis?.bundesland != "NordrheinWestfalen" || f.organisation != "Rettungsdienst") return vorgabewort(s)
                return when (kennzahl(f)) {
                    "82" -> "Notarzt"
                    "85" -> "Krankentransport"
                    else -> "Rettung"
                }
            }
            fun rufname(f: Fahrzeugvorlage?, s: String): String {
                if (f == null) return "—"
                val wort = v.rufnamenpraefixe[s]?.trim()?.ifBlank { null } ?: landeswort(f, s)
                val ort = v.ort.trim().ifBlank { "Heidefeld" }
                // Österreich und die Schweiz: Wort und Wehr, ohne Kennzahl — „Tank Hall".
                // Wie ein Land die Nummern anhängt, entscheidet der Server.
                if (staat != "Deutschland") return if (f.organisation == "Polizei") "$ort 1" else "$wort $ort"
                return if (f.organisation == "Polizei") "$wort ${kennzahl(f)}/1" else "$wort $ort $beispielzug/${kennzahl(f)}/1"
            }

            Abschnitt("Rufnamen") {
                SehrLeise("Leer lassen heißt: die BOS-Systematik gilt. Die Vorschau zeigt, was daraus im Funk wird.")
                val traeger = (abzug.map { it.traeger } + v.wachen.mapNotNull { it.traeger })
                    .filter { it != "Keine" && it.isNotBlank() }.distinct()
                (ORGANISATIONEN + traeger).forEach { s ->
                    val beispiel = vorlagenImStaat.firstOrNull { schluessel(it) == s }
                    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                        Feld(
                            v.rufnamenpraefixe[s].orEmpty(),
                            { t -> beiAendern(v.copy(rufnamenpraefixe = v.rufnamenpraefixe + (s to t.take(24)))) },
                            etikett = ORG_NAME[s] ?: HIORG_NAME[s] ?: s,
                            platzhalter = vorgabewort(s),
                        )
                        SehrLeise(rufname(beispiel, s), mono = true)
                    }
                }
            }

            // ----------------------------------------------------- Kennzahlen
            Abschnitt("Kennzahlen der Fahrzeuge") {
                SehrLeise(
                    "Die Zahl in der Mitte des Rufnamens — bei „12/44/1\" die 44. Sie ist eine " +
                        "Beschriftung: Die Alarm- und Ausrückeordnung sucht Fahrzeuge über ihre " +
                        "Fähigkeiten, nie über diese Zahl. Leer lassen heißt „wie im Katalog\".",
                )
                var kategorie by rememberSaveable { mutableStateOf<String?>(null) }
                val gruppen = vorlagenImStaat.sortedBy { it.typ }.groupBy { it.kategorie }.toList().sortedBy { it.first }
                Pillenreihe {
                    gruppen.forEach { (k, l) ->
                        Pille(k.ifBlank { "Sonstige" }, kategorie == k, { kategorie = if (kategorie == k) null else k }, zahl = l.size)
                    }
                }
                gruppen.firstOrNull { it.first == kategorie }?.second?.forEach { f ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(f.typ, style = Schrift.Klein, color = Farben.Text)
                            SehrLeise(rufname(f, schluessel(f)), mono = true)
                        }
                        Feld(
                            v.kennzahlen[f.id].orEmpty(),
                            { t -> beiAendern(v.copy(kennzahlen = v.kennzahlen + (f.id to t.take(4)))) },
                            platzhalter = f.kennzahl,
                            modifier = Modifier.width(90.dp),
                        )
                    }
                }
            }

            // --------------------------------------------------------- Regeln
            val e = v.einstellungen
            Abschnitt("Regeln der Runde") {
                Kasten(abstandInnen = Abstand.Winzig) {
                    listOf(
                        Triple("Tagesalarmstärke", e.tagesalarmstaerke) { b: Boolean -> e.copy(tagesalarmstaerke = b) },
                        Triple("Löschwasser", e.loeschwasser) { b: Boolean -> e.copy(loeschwasser = b) },
                        Triple("Tätigkeiten an der Einsatzstelle", e.einsatzarbeit) { b: Boolean -> e.copy(einsatzarbeit = b) },
                        Triple("Wiederherstellung der Einsatzbereitschaft", e.wiederherstellung) { b: Boolean -> e.copy(wiederherstellung = b) },
                        Triple("Sonderobjekte mit Einsatzplan", e.sonderobjekte) { b: Boolean -> e.copy(sonderobjekte = b) },
                        Triple("Suchlagen", e.suchlagen) { b: Boolean -> e.copy(suchlagen = b) },
                        Triple("Vegetationsbrände", e.vegetationsbraende) { b: Boolean -> e.copy(vegetationsbraende = b) },
                        Triple("Gefahrgutlagen", e.gefahrgutlagen) { b: Boolean -> e.copy(gefahrgutlagen = b) },
                        Triple("Notrufe kommen als Telefonanruf herein", e.telefonischeLeitstelle) { b: Boolean -> e.copy(telefonischeLeitstelle = b) },
                        Triple("Öffentlich — andere können beitreten", e.oeffentlich) { b: Boolean -> e.copy(oeffentlich = b) },
                    ).forEach { (text, an, setzen) ->
                        Hakenzeile(text, an == true, { beiAendern(v.copy(einstellungen = setzen(it))) })
                    }
                }
            }

            // ------------------------------------------------------------ Fuß
            Kasten(abstandInnen = Abstand.Klein) {
                Pillenreihe {
                    Knopf("Speichern", beiSichern, art = Knopfart.Haupt, aktiv = frei && !stand.laeuft)
                    if (v.id.isNotBlank()) {
                        Knopf("Runde eröffnen", { beiEroeffnen(v.id) }, aktiv = frei && !stand.laeuft)
                    }
                    Knopf("Zur Liste", beiZurueck, art = Knopfart.Leise)
                }
                Rueckmeldung(stand.meldung, stand.fehler)
                if (v.code.isNotBlank()) SehrLeise("Zum Weitergeben: ${v.code}", mono = true)
            }

            // ---------------------------------------------------------- Wahlen
            val w = wahl
            when {
                w == "kreis" -> Wahlblende(
                    titel = "Landkreis",
                    gruppen = werkKreisgruppen(kreise),
                    aufschrift = { it.name },
                    unterschrift = { if (it.hatDaten) "${it.wachen} Wachen" else "ohne Wachendaten" },
                    gewaehlt = kreis,
                    beiWahl = { k ->
                        beiAendern(
                            v.copy(
                                landkreisId = k.id,
                                wachen = emptyList(),
                                festeFunkrufnamen = emptyMap(),
                                ort = k.kreisstadt,
                                leitstelle = leitstellenname(katalog, k),
                            ),
                        )
                        beiKreis(k.id)
                        wahl = null
                    },
                    beiSchliessen = { wahl = null },
                    suchbar = true,
                )

                w != null && w.startsWith("traeger:") -> {
                    val kennung = w.substringAfter(":")
                    val optionen = listOf("", "Keine") + HIORG_NAME.keys.filter { it != "Keine" }
                    Wahlblende(
                        titel = "Träger",
                        gruppen = listOf(null to optionen),
                        aufschrift = {
                            when (it) {
                                "" -> "Aus dem Namen lesen"
                                "Keine" -> "Kommunal — kein Träger"
                                else -> HIORG_NAME[it] ?: it
                            }
                        },
                        gewaehlt = gewaehlt[kennung]?.traeger ?: "",
                        beiWahl = { t ->
                            wahlAendern(kennung) { it.copy(traeger = t.ifBlank { null }) }
                            wahl = null
                        },
                        beiSchliessen = { wahl = null },
                    )
                }
            }
        }
    }
}

/** Eine Wache aus dem Kartenabzug — anhaken, dann Zug und Art. */
@Composable
private fun Abzugswache(
    a: Kreiswache,
    wahl: Wachenwahl?,
    beiUmschalten: () -> Unit,
    beiAendern: (Wachenwahl) -> Unit,
) {
    val veraendert = wahl != null &&
        (wahl.lat != null || wahl.lon != null || (wahl.organisation != null && wahl.organisation != a.organisation))
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 9.dp, randfarbe = if (wahl != null) orgFarbe(wahl.organisation ?: a.organisation) else Farben.Rand)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Winzig),
    ) {
        Hakenzeile(
            text = a.name + "  ·  " + (ORG_NAME[wahl?.organisation ?: a.organisation] ?: a.organisation) +
                (if (a.traeger != "Keine") " · ${HIORG_NAME[a.traeger] ?: a.traeger}" else "") +
                (if (veraendert) " · geändert" else ""),
            an = wahl != null,
            beiWechsel = { beiUmschalten() },
        )
        if (wahl != null) {
            // Die Zugnummer steht an der Wache und nicht am Fahrzeug: Sie meint
            // den Standort. „Florian Uelzen 12/44/1" ist die Ortswehr Nr. 12.
            Stufenwahl("Zug", wahl.zugnummer, { beiAendern(wahl.copy(zugnummer = it)) }, 1, 1, 99)
            Pillenreihe {
                ORGANISATIONEN.forEach { o ->
                    Pille(ORG_NAME[o] ?: o, (wahl.organisation ?: a.organisation) == o, {
                        beiAendern(wahl.copy(organisation = if (o == a.organisation) null else o))
                    }, farbe = orgFarbe(o))
                }
                if (veraendert) {
                    Knopf("Zurücksetzen", {
                        beiAendern(wahl.copy(lat = null, lon = null, organisation = null))
                    }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
    }
}

/** Eine selbst gebaute Wache — Name, Zug, Art, Träger, Standort. */
@Composable
private fun EigeneWache(
    w: Wachenwahl,
    beiAendern: (Wachenwahl) -> Unit,
    beiTraeger: () -> Unit,
    beiAbreissen: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 9.dp, randfarbe = Farben.Amber)
            .padding(Abstand.Klein),
    ) {
        Feld(w.name.orEmpty(), { beiAendern(w.copy(name = it.take(80))) }, etikett = "Selbst gebaute Wache", platzhalter = "Name der Wache")
        Stufenwahl("Zug", w.zugnummer, { beiAendern(w.copy(zugnummer = it)) }, 1, 1, 99)
        Pillenreihe {
            ORGANISATIONEN.forEach { o ->
                Pille(ORG_NAME[o] ?: o, (w.organisation ?: "Feuerwehr") == o, { beiAendern(w.copy(organisation = o)) }, farbe = orgFarbe(o))
            }
        }
        Wahlfeld(
            "Träger",
            when (w.traeger) {
                null -> "Aus dem Namen lesen"
                "Keine" -> "Kommunal — kein Träger"
                else -> HIORG_NAME[w.traeger] ?: w.traeger
            },
            beiTraeger,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Zahlfeld("Breite", w.lat, { t ->
                t.replace(',', '.').toDoubleOrNull()?.let { beiAendern(w.copy(lat = runde5(it))) }
            }, Modifier.weight(1f))
            Zahlfeld("Länge", w.lon, { t ->
                t.replace(',', '.').toDoubleOrNull()?.let { beiAendern(w.copy(lon = runde5(it))) }
            }, Modifier.weight(1f))
        }
        Knopf("Abreißen", beiAbreissen, art = Knopfart.Gefahr, kompakt = true)
    }
}

/**
 * Feste Fahrzeugrufnamen — eine Kennung gilt für diesen Typ an genau dieser
 * Wache. Ein zweiter Eintrag für dieselbe Wache und denselben Typ benennt das
 * zweite Fahrzeug (`wache:fahrzeug:2`), in dieser Reihenfolge.
 */
@Composable
private fun FesteRufnamen(
    v: Leitstellenvorlage,
    abzug: List<Kreiswache>,
    fahrzeuge: List<Fahrzeugvorlage>,
    beiAendern: (Leitstellenvorlage) -> Unit,
    art: (Wachenwahl) -> String?,
) {
    var wache by rememberSaveable { mutableStateOf<String?>(null) }
    var fahrzeug by rememberSaveable { mutableStateOf<String?>(null) }
    var rufname by rememberSaveable { mutableStateOf("") }
    var wahl by remember { mutableStateOf<String?>(null) }

    val wachen = v.wachen.map { w ->
        Triple(w.kennung, abzug.firstOrNull { it.kennung == w.kennung }?.name ?: w.name?.ifBlank { null } ?: w.kennung, art(w))
    }
    val org = wachen.firstOrNull { it.first == wache }?.third
    val passende = fahrzeuge.filter { org == null || it.organisation == org }.sortedBy { it.typ }

    fun schluessel(w: String, f: String, p: Int) = if (p <= 1) "$w:$f" else "$w:$f:$p"

    Abschnitt("Feste Fahrzeugrufnamen") {
        SehrLeise(
            "Eine Kennung gilt für diesen Fahrzeugtyp an genau dieser Wache. So bleibt etwa ein " +
                "RTH in jeder Runde „Christoph 19\". Noch ein Eintrag für dieselbe Wache und denselben " +
                "Typ benennt das zweite Fahrzeug, der nächste das dritte.",
        )
        if (v.wachen.isEmpty()) {
            SehrLeise("Wähle erst Wachen aus — ein fester Rufname hängt an einer Wache.")
        } else {
            Wahlfeld("Wache", wachen.firstOrNull { it.first == wache }?.second, { wahl = "wache" }, platzhalter = "Wache …")
            Wahlfeld(
                "Fahrzeug",
                fahrzeuge.firstOrNull { it.id == fahrzeug }?.typ,
                { wahl = "fahrzeug" },
                platzhalter = "Fahrzeug …",
                aktiv = wache != null,
            )
            Feld(rufname, { rufname = it.take(48) }, platzhalter = "Christoph 19")
            Knopf("Übernehmen", {
                val w = wache ?: return@Knopf
                val f = fahrzeug ?: return@Knopf
                var p = 1
                while (v.festeFunkrufnamen.containsKey(schluessel(w, f, p))) p++
                beiAendern(v.copy(festeFunkrufnamen = v.festeFunkrufnamen + (schluessel(w, f, p) to rufname.trim())))
                rufname = ""
            }, aktiv = wache != null && fahrzeug != null && rufname.isNotBlank(), kompakt = true)
        }

        v.festeFunkrufnamen.entries
            .map { (k, r) ->
                val teile = k.split(":")
                val p = teile.getOrNull(2)?.toIntOrNull() ?: 1
                val wn = wachen.firstOrNull { it.first == teile[0] }?.second ?: teile[0]
                val fn = fahrzeuge.firstOrNull { it.id == teile.getOrNull(1) }?.typ ?: teile.getOrNull(1).orEmpty()
                listOf(k, r, "$fn${if (p > 1) " ($p.)" else ""} · $wn")
            }
            .sortedBy { it[2] }
            .forEach { (k, r, wo) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r, style = Schrift.MonoNormal, color = Farben.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        SehrLeise(wo)
                    }
                    Knopf("✕", { beiAendern(v.copy(festeFunkrufnamen = v.festeFunkrufnamen - k)) }, art = Knopfart.Leise, kompakt = true)
                }
            }
    }

    when (wahl) {
        "wache" -> Wahlblende(
            titel = "Wache",
            gruppen = listOf(null to wachen),
            aufschrift = { it.second },
            unterschrift = { it.third?.let { o -> ORG_NAME[o] } },
            gewaehlt = wachen.firstOrNull { it.first == wache },
            beiWahl = {
                wache = it.first
                if (passende.none { f -> f.id == fahrzeug }) fahrzeug = null
                wahl = null
            },
            beiSchliessen = { wahl = null },
            suchbar = wachen.size > 8,
        )
        "fahrzeug" -> Wahlblende(
            titel = "Fahrzeug",
            gruppen = listOf(null to passende),
            aufschrift = { it.typ },
            unterschrift = { it.beschreibung.ifBlank { null } },
            gewaehlt = passende.firstOrNull { it.id == fahrzeug },
            beiWahl = {
                fahrzeug = it.id
                wahl = null
            },
            beiSchliessen = { wahl = null },
            suchbar = true,
        )
    }
}

private fun runde5(x: Double): Double = Math.round(x * 100_000.0) / 100_000.0
