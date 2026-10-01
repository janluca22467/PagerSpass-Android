package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Werkstand
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.HIORG_NAME
import de.pagerspass.pagerspass.netz.JAHRESZEIT_NAME
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.ORGANISATIONEN
import de.pagerspass.pagerspass.netz.ORG_NAME
import de.pagerspass.pagerspass.netz.STOERUNG_NAME
import de.pagerspass.pagerspass.netz.SZENARIOART_NAME
import de.pagerspass.pagerspass.netz.Stichwort
import de.pagerspass.pagerspass.netz.Szenario
import de.pagerspass.pagerspass.netz.Szenarioeinstellungen
import de.pagerspass.pagerspass.netz.Szenarioeintrag
import de.pagerspass.pagerspass.netz.WETTER_NAME
import de.pagerspass.pagerspass.netz.bundeslandname
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
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
 * Vorbereitete Übungen — das Gegenstück zu `web/src/views/UebungenView.vue`.
 *
 * <b>Liste und Editor sind zwei Seiten</b>, im Web eine Ansicht mit `?id=`.
 * Zurück aus dem Editor führt zur Liste; der Entwurf liegt in der Werkstatt und
 * überlebt das Drehen des Geräts.
 *
 * <b>„Leiten" und „Selbst fahren"</b> eröffnen beide eine Runde mit dieser
 * Übung. Wer leitet, tritt als Zuschauer bei und sitzt damit am Regieplatz;
 * wer fährt, nimmt einen Platz wie in jeder Runde.
 */
@Composable
fun UebungenSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Werkstand = Werkstand(),
    katalogBereit: Boolean = false,
    beiLaden: (Boolean) -> Unit = {},
    beiNeu: () -> Unit = {},
    beiOeffnen: (String) -> Unit = {},
    beiLoeschen: (String) -> Unit = {},
    beiUebernehmen: (String) -> Unit = {},
    beiFahren: (String) -> Unit = {},
    beiLeiten: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden(false) }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Vorbereitete Übungen",
            unterzeile = "Eine Lage vorher bauen und mehrmals fahren",
            knoepfe = { Knopf("← Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Knopf("Neue Übung", beiNeu, art = Knopfart.Haupt, aktiv = katalogBereit, breit = true)
        if (!katalogBereit) SehrLeise("Der Fahrzeugkatalog wird noch geladen.")

        Rueckmeldung(stand.meldung, stand.fehler)

        Codeuebernahme(
            etikett = "Übung eines anderen übernehmen",
            erklaerung = "Du bekommst eine eigene Kopie. Ändert der andere seine Übung, bleibt deine, wie sie ist.",
            aktiv = !stand.laeuft,
            beiUebernehmen = beiUebernehmen,
        )

        Bereich(
            laedt = stand.szenarien.ersteLadung,
            fehler = stand.szenarien.fehler,
            inhalt = stand.szenarien.inhalt,
            beiErneut = { beiLaden(true) },
        ) { liste ->
            if (liste.isEmpty()) Leerhinweis("Noch keine Übung. „Neue Übung\" legt die erste an.")
            liste.forEach { s ->
                Kasten(abstandInnen = Abstand.Klein) {
                    Text(s.name, style = Schrift.Gross, color = Farben.Text)
                    if (s.beschreibung.isNotBlank()) Leise(s.beschreibung)
                    SehrLeise(
                        "${s.eintraege} ${if (s.eintraege == 1) "Eintrag" else "Einträge"} · Code ${s.code}",
                        mono = true,
                    )
                    Pillenreihe {
                        Knopf("Leiten", { beiLeiten(s.id) }, art = Knopfart.Haupt, aktiv = !stand.laeuft, kompakt = true)
                        Knopf("Selbst fahren", { beiFahren(s.id) }, aktiv = !stand.laeuft, kompakt = true)
                        Knopf("Bearbeiten", { beiOeffnen(s.id) }, kompakt = true)
                        Loeschknopf({ beiLoeschen(s.id) }, aktiv = !stand.laeuft)
                    }
                }
            }
        }

        // Der Übungsverlauf steht hier und nicht im Dienstbuch: Eine Übung
        // bringt weder Erfahrung noch Credits und zählt in keiner Wertung.
        if (stand.uebungsverlauf.isNotEmpty()) {
            Abschnitt("Übungsverlauf") {
                SehrLeise(
                    "Was du gefahren hast. Übungen bringen bewusst keine Erfahrung und keine " +
                        "Credits — sonst schriebe sich jeder seine Punkte selbst.",
                )
                Kasten(abstandInnen = Abstand.Winzig) {
                    stand.uebungsverlauf.forEach { f ->
                        Column {
                            Text(f.name, style = Schrift.Klein, color = Farben.Text)
                            SehrLeise(
                                "${tag(f.beendetUm)} · " +
                                    (if (f.rolle == "Leitstelle") "Leitstelle" else f.funkrufname ?: "Besatzung") +
                                    " · ${f.einsaetze} ${if (f.einsaetze == 1) "Einsatz" else "Einsätze"}",
                                mono = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Ein frischer Entwurf — mit den vier Fahrzeugen, mit denen man fast jede Lage beginnt. */
fun neueUebung(katalog: Katalog?): Szenario = Szenario(
    name = "Neue Übung",
    landkreisId = katalog?.landkreise?.firstOrNull()?.id,
    aufstellung = listOf("hlf20", "lf10", "rtw", "nef").filter { id ->
        katalog?.fahrzeuge?.any { it.id == id } == true
    },
    einstellungen = Szenarioeinstellungen(),
)

/**
 * Der Übungseditor — Stammdaten, Aufstellung, Zeitachse, Regeln.
 *
 * Er folgt der Handyansicht des Webs: alles untereinander, die Zeitachse als
 * Liste zum Aufklappen, darüber die Schiene als Übersicht (kein Bedienelement —
 * sie zeigt, wie dicht es wird). Die Zeit eines Eintrags stellt man mit Minus
 * und Plus in Halbminuten, nicht mit einem Schieber.
 */
@Composable
fun UebungEditorSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Werkstand = Werkstand(),
    katalog: Katalog? = null,
    beiAendern: (Szenario) -> Unit = {},
    beiSichern: () -> Unit = {},
    beiFahren: (String) -> Unit = {},
    beiLeiten: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    var wahl by remember { mutableStateOf<String?>(null) }
    var offen by rememberSaveable { mutableStateOf(-1) }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = stand.uebung.inhalt?.name?.ifBlank { null } ?: "Übung",
            unterzeile = "Übung bearbeiten",
            knoepfe = { Knopf("← Zur Liste", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = stand.uebung.laedt,
            fehler = stand.uebung.fehler,
            inhalt = stand.uebung.inhalt,
        ) { s ->
            val kreise = katalog?.landkreise.orEmpty()
            val kreis = kreise.firstOrNull { it.id == s.landkreisId }
            fun imKreis(hiOrg: String?) =
                hiOrg == null || hiOrg == "Keine" || hiOrg.isBlank() || kreis == null ||
                    kreis.hiOrgs.isEmpty() || hiOrg in kreis.hiOrgs
            fun vorlage(id: String) = katalog?.fahrzeuge?.firstOrNull { it.id == id }

            // ------------------------------------------------------ Stammdaten
            Kasten(abstandInnen = Abstand.Klein) {
                Feld(s.name, { beiAendern(s.copy(name = it.take(60))) }, etikett = "Name")
                Feld(
                    s.beschreibung,
                    { beiAendern(s.copy(beschreibung = it.take(400))) },
                    etikett = "Beschreibung",
                    platzhalter = "Worum es in dieser Übung geht.",
                    einzeilig = false,
                )
                Wahlfeld(
                    "Landkreis",
                    kreis?.aufschrift ?: "Erfundener Standardbereich",
                    { wahl = "kreis" },
                )
                if (s.code.isNotBlank()) SehrLeise("Zum Weitergeben: ${s.code}", mono = true)
            }

            // ------------------------------------------------------ Aufstellung
            Abschnitt("Aufstellung (${s.aufstellung.size} Fahrzeuge)") {
                SehrLeise("Was in dieser Übung im Dienst steht. Unbesetzte Fahrzeuge bekommen Bot-Besatzungen.")
                if (s.aufstellung.isEmpty()) {
                    Rueckmeldung(null, "Ohne Fahrzeuge kann die Übung nicht beginnen — es fehlt die Gegenstelle.")
                }
                // Wer im Kreis keine Wache hat, rückt dort auch nicht aus — das
                // entscheidet der Server beim Eröffnen, und hier steht es vorher.
                val fremd = s.aufstellung.mapNotNull { vorlage(it) }.filter { !imKreis(it.hiOrg) }
                if (fremd.isNotEmpty()) {
                    Rueckmeldung(
                        null,
                        (if (fremd.size == 1) "Ein Fahrzeug steht" else "${fremd.size} Fahrzeuge stehen") +
                            " in diesem Landkreis nicht im Dienst und " +
                            (if (fremd.size == 1) "fehlt" else "fehlen") + " in der Übung: " +
                            fremd.joinToString(", ") { "${it.typ} (${HIORG_NAME[it.hiOrg] ?: it.hiOrg})" } + ".",
                    )
                }
                s.aufstellung.forEachIndexed { nr, id ->
                    val v = vorlage(id)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .flaeche(
                                ecke = 9.dp,
                                randfarbe = if (imKreis(v?.hiOrg)) orgFarbe(v?.organisation).copy(alpha = 0.6f) else Farben.SignalTief,
                            )
                            .padding(start = Abstand.Normal),
                    ) {
                        Text(v?.typ ?: id, style = Schrift.MonoNormal, color = Farben.Text)
                        val traeger = v?.hiOrg?.let { HIORG_NAME[it] }.orEmpty()
                        SehrLeise(listOfNotNull(traeger.ifBlank { null }, v?.beschreibung).joinToString(" · "), Modifier.weight(1f))
                        Knopf("×", {
                            beiAendern(s.copy(aufstellung = s.aufstellung.filterIndexed { i, _ -> i != nr }))
                        }, art = Knopfart.Leise, kompakt = true)
                    }
                }
                Knopf("Fahrzeug hinzufügen", { wahl = "fahrzeug" }, kompakt = true)
            }

            // ------------------------------------------------------- Zeitachse
            val achse = s.zeitachse
            Abschnitt("Zeitachse") {
                SehrLeise(
                    "Alles ab Dienstbeginn gerechnet — eine Übung, die um 19:20 statt 19:00 " +
                        "anfängt, läuft genauso.",
                )
                val ende = maxOf(600, (achse.maxOfOrNull { it.nachSekunden } ?: 0) + 60)
                Kasten(abstandInnen = Abstand.Winzig, farbe = Farben.BgTief) {
                    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                        drawLine(
                            Farben.Rand,
                            Offset(0f, size.height / 2),
                            Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                        )
                        achse.forEach { e ->
                            val x = e.nachSekunden.toFloat() / ende * size.width
                            drawLine(
                                when (e.art) {
                                    "Lage" -> Farben.Signal
                                    "Stoerung" -> Farben.Amber
                                    else -> Farben.Blau
                                },
                                Offset(x, 4f),
                                Offset(x, size.height - 4f),
                                strokeWidth = 3.dp.toPx(),
                            )
                        }
                    }
                    SehrLeise("0:00 – ${zeitText(ende)}", mono = true)
                }

                if (achse.isEmpty()) Leerhinweis("Noch nichts auf der Zeitachse.")

                achse.forEachIndexed { nr, e ->
                    Eintrag(
                        e = e,
                        offen = offen == nr,
                        katalog = katalog,
                        beiKopf = { offen = if (offen == nr) -1 else nr },
                        beiAendern = { neu, sortieren ->
                            val liste = achse.toMutableList().also { it[nr] = neu }
                            if (sortieren) {
                                val sortiert = liste.sortedBy { it.nachSekunden }
                                offen = sortiert.indexOfFirst { it === neu }
                                beiAendern(s.copy(zeitachse = sortiert))
                            } else {
                                beiAendern(s.copy(zeitachse = liste))
                            }
                        },
                        beiWahl = { wahl = "$it:$nr" },
                        beiLoeschen = {
                            offen = -1
                            beiAendern(s.copy(zeitachse = achse.filterIndexed { i, _ -> i != nr }))
                        },
                    )
                }

                Pillenreihe {
                    listOf("Lage" to "+ Lage", "Stoerung" to "+ Störung", "Wetterwechsel" to "+ Wetter").forEach { (art, wort) ->
                        Knopf(wort, {
                            val nach = achse.maxOfOrNull { it.nachSekunden }?.plus(120) ?: 30
                            val neu = neuerEintrag(art, nach)
                            val sortiert = (achse + neu).sortedBy { it.nachSekunden }
                            offen = sortiert.indexOfFirst { it === neu }
                            beiAendern(s.copy(zeitachse = sortiert))
                        }, kompakt = true)
                    }
                }
            }

            // ----------------------------------------------------------- Regeln
            val r = s.einstellungen
            Abschnitt("Regeln dieser Übung") {
                Kasten(abstandInnen = Abstand.Winzig) {
                    fun haken(text: String, an: Boolean?, setzen: (Boolean) -> Szenarioeinstellungen) =
                        Triple(text, an == true, setzen)
                    listOf(
                        haken("Tagesalarmstärke — Ausrückzeiten gelten, ehrenamtliche brauchen länger", r.tagesalarmstaerke) { r.copy(tagesalarmstaerke = it) },
                        haken("Löschwasser — Tanks laufen leer, Versorgung muss stehen", r.loeschwasser) { r.copy(loeschwasser = it) },
                        haken("Tätigkeiten an der Einsatzstelle", r.einsatzarbeit) { r.copy(einsatzarbeit = it) },
                        haken("Wiederherstellung der Einsatzbereitschaft", r.wiederherstellung) { r.copy(wiederherstellung = it) },
                        haken("Sonderobjekte mit Einsatzplan", r.sonderobjekte) { r.copy(sonderobjekte = it) },
                        haken("Notrufe kommen als Telefonanruf herein", r.telefonischeLeitstelle) { r.copy(telefonischeLeitstelle = it) },
                    ).forEach { (text, an, setzen) ->
                        Hakenzeile(text, an, { beiAendern(s.copy(einstellungen = setzen(it))) })
                    }
                }
                Etikett("Wetter bei Dienstbeginn")
                Pillenreihe {
                    WETTER_NAME.forEach { (w, name) ->
                        Pille(name, r.wetter == w, { beiAendern(s.copy(einstellungen = r.copy(wetter = w))) })
                    }
                }
                Etikett("Jahreszeit")
                Pillenreihe {
                    Pille("Wie gerade draußen", r.jahreszeit == null, {
                        beiAendern(s.copy(einstellungen = r.copy(jahreszeit = null)))
                    })
                    JAHRESZEIT_NAME.forEach { (j, name) ->
                        Pille(name, r.jahreszeit == j, { beiAendern(s.copy(einstellungen = r.copy(jahreszeit = j))) })
                    }
                }
            }

            // -------------------------------------------------------------- Fuß
            Kasten(abstandInnen = Abstand.Klein) {
                Pillenreihe {
                    Knopf("Speichern", beiSichern, art = Knopfart.Haupt, aktiv = !stand.laeuft)
                    if (s.id.isNotBlank()) {
                        Knopf("Übung leiten", { beiLeiten(s.id) }, aktiv = !stand.laeuft)
                        Knopf("Selbst fahren", { beiFahren(s.id) }, aktiv = !stand.laeuft)
                    }
                    Knopf("Zurück", beiZurueck, art = Knopfart.Leise)
                }
                Rueckmeldung(stand.meldung, stand.fehler)
            }

            // ----------------------------------------------------------- Wahlen
            val w = wahl
            when {
                w == "kreis" -> {
                    val ohne = Landkreis(id = "", name = "Erfundener Standardbereich")
                    Wahlblende(
                        titel = "Landkreis",
                        gruppen = listOf<Pair<String?, List<Landkreis>>>(null to listOf(ohne)) +
                            kreise.groupBy { it.bundesland }.toList().sortedBy { bundeslandname(it.first) }
                                .map { (land, liste) -> bundeslandname(land) to liste.sortedBy { it.name } },
                        aufschrift = { it.aufschrift },
                        unterschrift = { if (it.id.isBlank() || it.hatDaten) null else "ohne Wachendaten" },
                        gewaehlt = kreis ?: ohne,
                        beiWahl = {
                            beiAendern(s.copy(landkreisId = it.id.ifBlank { null }))
                            wahl = null
                        },
                        beiSchliessen = { wahl = null },
                        suchbar = true,
                    )
                }

                w == "fahrzeug" -> Wahlblende(
                    titel = "Fahrzeug hinzufügen",
                    gruppen = katalog?.fahrzeuge.orEmpty()
                        .filter { imKreis(it.hiOrg) }
                        .groupBy { it.kategorie.ifBlank { ORG_NAME[it.organisation] ?: it.organisation } }
                        .toList()
                        .sortedBy { it.first }
                        .map { (k, liste) -> k to liste.sortedBy { it.typ } },
                    aufschrift = { v: Fahrzeugvorlage ->
                        v.typ + (HIORG_NAME[v.hiOrg]?.ifBlank { null }?.let { " · $it" } ?: "")
                    },
                    unterschrift = { v ->
                        listOfNotNull(
                            v.beschreibung.ifBlank { null },
                            if (v.id in s.aufstellung) "schon ${s.aufstellung.count { it == v.id }}× dabei" else null,
                        ).joinToString(" · ").ifBlank { null }
                    },
                    beiWahl = {
                        beiAendern(s.copy(aufstellung = s.aufstellung + it.id))
                        wahl = null
                    },
                    beiSchliessen = { wahl = null },
                    suchbar = true,
                )

                w != null && w.startsWith("stichwort:") -> {
                    val nr = w.substringAfter(":").toIntOrNull() ?: -1
                    val e = achse.getOrNull(nr)
                    val eigene = Stichwort(id = "", stichwort = "— eigene Angaben —")
                    if (e != null) {
                        Wahlblende(
                            titel = "Stichwort aus dem Katalog",
                            gruppen = listOf<Pair<String?, List<Stichwort>>>(null to listOf(eigene)) +
                                katalog?.stichworte.orEmpty().groupBy { it.organisation }.toList()
                                    .map { (o, l) -> (ORG_NAME[o] ?: o) to l },
                            aufschrift = { if (it.id.isBlank()) it.stichwort else "${it.stichwort} · ${it.stichwortText}" },
                            gewaehlt = katalog?.stichworte?.firstOrNull {
                                it.stichwort == e.stichwort && it.stichwortText == e.stichwortText
                            } ?: eigene,
                            beiWahl = { v ->
                                if (v.id.isNotBlank()) {
                                    val neu = e.copy(
                                        stichwort = v.stichwort,
                                        stichwortText = v.stichwortText,
                                        organisation = v.organisation,
                                        prioritaet = v.prioritaet,
                                        empfohleneFahrzeuge = v.empfohleneFahrzeuge,
                                        empfohleneFaehigkeiten = v.empfohleneFaehigkeiten,
                                        meldebild = e.meldebild?.ifBlank { null } ?: v.meldebilder.firstOrNull().orEmpty(),
                                    )
                                    beiAendern(s.copy(zeitachse = achse.toMutableList().also { it[nr] = neu }))
                                }
                                wahl = null
                            },
                            beiSchliessen = { wahl = null },
                            suchbar = true,
                        )
                    }
                }

                w != null && w.startsWith("stoerung:") -> {
                    val nr = w.substringAfter(":").toIntOrNull() ?: -1
                    val e = achse.getOrNull(nr)
                    if (e != null) {
                        Wahlblende(
                            titel = "Was dazwischenkommt",
                            gruppen = listOf(null to STOERUNG_NAME.keys.toList()),
                            aufschrift = { STOERUNG_NAME[it] ?: it },
                            gewaehlt = e.stoerung,
                            beiWahl = { a ->
                                beiAendern(s.copy(zeitachse = achse.toMutableList().also { it[nr] = e.copy(stoerung = a) }))
                                wahl = null
                            },
                            beiSchliessen = { wahl = null },
                        )
                    }
                }
            }
        }
    }
}

/** Ein Eintrag der Zeitachse — zugeklappt eine Zeile, aufgeklappt sein Bogen. */
@Composable
private fun Eintrag(
    e: Szenarioeintrag,
    offen: Boolean,
    katalog: Katalog?,
    beiKopf: () -> Unit,
    beiAendern: (Szenarioeintrag, Boolean) -> Unit,
    beiWahl: (String) -> Unit,
    beiLoeschen: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(ecke = 9.dp, randfarbe = if (offen) Farben.Amber else Farben.Rand),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = beiKopf)
                .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
        ) {
            Text("+${zeitText(e.nachSekunden)}", style = Schrift.MonoNormal, color = Farben.Amber)
            SehrLeise(SZENARIOART_NAME[e.art] ?: e.art)
            Text(
                eintragTitel(e),
                style = Schrift.Normal,
                color = Farben.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(if (offen) "▾" else "▸", style = Schrift.Normal, color = Farben.TextSehrLeise)
        }

        if (!offen) return@Column

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier.padding(start = Abstand.Normal, end = Abstand.Normal, bottom = Abstand.Normal),
        ) {
            // Ein Schieber wie im Web (`type="range"`, 30-Sekunden-Schritte). Sortiert
            // wird erst beim Loslassen — sonst spränge der Eintrag unter dem Finger weg.
            Schieber(
                etikett = "Nach Dienstbeginn",
                wert = e.nachSekunden,
                von = 0,
                bis = 3600,
                schritt = 30,
                anzeige = { "+${zeitText(it)}" },
                beiAenderung = { beiAendern(e.copy(nachSekunden = it), false) },
                beiLoslassen = { beiAendern(e, true) },
            )

            when (e.art) {
                "Lage" -> {
                    Wahlfeld(
                        "Stichwort aus dem Katalog",
                        katalog?.stichworte?.firstOrNull {
                            it.stichwort == e.stichwort && it.stichwortText == e.stichwortText
                        }?.let { "${it.stichwort} · ${it.stichwortText}" } ?: "— eigene Angaben —",
                        { beiWahl("stichwort") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Feld(
                            e.stichwort.orEmpty(),
                            { beiAendern(e.copy(stichwort = it.take(12)), false) },
                            etikett = "Stichwort",
                            modifier = Modifier.weight(1f),
                        )
                        Feld(
                            e.stichwortText.orEmpty(),
                            { beiAendern(e.copy(stichwortText = it.take(60)), false) },
                            etikett = "Klartext",
                            modifier = Modifier.weight(2f),
                        )
                    }
                    Feld(
                        e.adresse.orEmpty(),
                        { beiAendern(e.copy(adresse = it.take(80)), false) },
                        etikett = "Adresse",
                        platzhalter = "Straße und Hausnummer im gewählten Kreis",
                    )
                    Feld(
                        e.meldebild.orEmpty(),
                        { beiAendern(e.copy(meldebild = it.take(240)), false) },
                        etikett = "Meldebild",
                        platzhalter = "Was gemeldet wurde.",
                        einzeilig = false,
                    )
                    Etikett("Organisation")
                    Pillenreihe {
                        ORGANISATIONEN.forEach { o ->
                            Pille(ORG_NAME[o] ?: o, e.organisation == o, { beiAendern(e.copy(organisation = o), false) }, farbe = orgFarbe(o))
                        }
                    }
                    Etikett("Dringlichkeit")
                    Pillenreihe {
                        listOf(1 to "Normal", 2 to "Dringend", 3 to "Sonderrechte").forEach { (p, name) ->
                            Pille(name, e.prioritaet == p, { beiAendern(e.copy(prioritaet = p), false) })
                        }
                    }
                    Etikett(
                        "Alarm- und Ausrückeordnung (${e.empfohleneFahrzeuge ?: "—"} Fzg, " +
                            "${e.empfohleneFaehigkeiten?.size ?: 0} " +
                            (if (e.empfohleneFaehigkeiten?.size == 1) "Funktion)" else "Funktionen)"),
                    )
                    Schieber(
                        etikett = "Empfohlene Fahrzeuge",
                        wert = e.empfohleneFahrzeuge ?: 1,
                        von = 1,
                        bis = 12,
                        schritt = 1,
                        beiAenderung = { beiAendern(e.copy(empfohleneFahrzeuge = it), false) },
                    )
                    if (katalog?.faehigkeiten?.isNotEmpty() == true) {
                        Etikett("Geforderte Funktionen")
                        Pillenreihe {
                            katalog.faehigkeiten.forEach { f ->
                                val drin = e.empfohleneFaehigkeiten?.contains(f) == true
                                Pille(f, drin, {
                                    val liste = e.empfohleneFaehigkeiten.orEmpty()
                                    beiAendern(e.copy(empfohleneFaehigkeiten = if (drin) liste - f else liste + f), false)
                                })
                            }
                        }
                    }
                }

                "Stoerung" -> {
                    Wahlfeld("Was dazwischenkommt", STOERUNG_NAME[e.stoerung] ?: e.stoerung, { beiWahl("stoerung") })
                    SehrLeise(
                        "Woran es geschieht, sucht der Server sich zur Laufzeit — welches Fahrzeug " +
                            "in Minute sieben auf der Anfahrt ist, weiß beim Schreiben niemand. Gibt es " +
                            "nichts zu stören, passiert nichts.",
                    )
                }

                else -> {
                    Etikett("Wetterlage")
                    Pillenreihe {
                        WETTER_NAME.forEach { (w, name) ->
                            Pille(name, e.wetter == w, { beiAendern(e.copy(wetter = w), false) })
                        }
                    }
                    Stufenwahl(
                        etikett = "Windrichtung (Grad)",
                        wert = e.windrichtung ?: 0,
                        beiAenderung = { beiAendern(e.copy(windrichtung = it), false) },
                        schritt = 45,
                        von = 0,
                        bis = 315,
                        anzeige = { "$it°" },
                    )
                }
            }

            Knopf("Eintrag löschen", beiLoeschen, art = Knopfart.Leise, kompakt = true)
        }
    }
}

private fun neuerEintrag(art: String, nach: Int) = Szenarioeintrag(
    art = art,
    nachSekunden = nach,
    stichwort = if (art == "Lage") "B2" else null,
    stichwortText = if (art == "Lage") "Zimmerbrand" else null,
    meldebild = if (art == "Lage") "" else null,
    adresse = if (art == "Lage") "" else null,
    organisation = if (art == "Lage") "Feuerwehr" else null,
    prioritaet = if (art == "Lage") 2 else null,
    stoerung = if (art == "Stoerung") "Fahrzeugdefekt" else null,
    wetter = if (art == "Wetterwechsel") "Sturm" else null,
)

private fun eintragTitel(e: Szenarioeintrag): String = when (e.art) {
    "Lage" -> "${e.stichwort ?: "—"} ${e.stichwortText.orEmpty()}".trim()
    "Stoerung" -> STOERUNG_NAME[e.stoerung ?: "Funkloch"] ?: e.stoerung.orEmpty()
    else -> "Wetter: ${WETTER_NAME[e.wetter ?: "Klar"] ?: e.wetter}"
}

private fun zeitText(sekunden: Int): String = "%d:%02d".format(sekunden / 60, sekunden % 60)

/**
 * Ein Schieber mit Etikett und Wert — wo das Web einen `type="range"` setzt.
 *
 * Plus und Minus in 30-Sekunden-Schritten hießen für „Minute 40“ achtzig Tipps;
 * der Schieber ist ein Zug. Die Windrichtung bleibt beim Stufenwähler — dort hat
 * das Web ein Zahlenfeld, und acht Stufen sind mit Tipps schneller als gezogen.
 */
@Composable
private fun Schieber(
    etikett: String,
    wert: Int,
    von: Int,
    bis: Int,
    schritt: Int,
    beiAenderung: (Int) -> Unit,
    anzeige: (Int) -> String = { it.toString() },
    beiLoslassen: () -> Unit = {},
) {
    val loslassen by rememberUpdatedState(beiLoslassen)
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Etikett(etikett, Modifier.weight(1f))
            Text(anzeige(wert.coerceIn(von, bis)), style = Schrift.MonoNormal, color = Farben.Amber)
        }
        Slider(
            value = wert.coerceIn(von, bis).toFloat(),
            onValueChange = { v ->
                val gerundet = (von + Math.round((v - von) / schritt) * schritt).coerceIn(von, bis)
                if (gerundet != wert) beiAenderung(gerundet)
            },
            onValueChangeFinished = { loslassen() },
            valueRange = von.toFloat()..bis.toFloat(),
            steps = ((bis - von) / schritt - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = Farben.Amber,
                activeTrackColor = Farben.Amber,
                inactiveTrackColor = Farben.FlaecheAktiv,
                activeTickColor = Farben.AmberTief,
                inactiveTickColor = Farben.Rand,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
