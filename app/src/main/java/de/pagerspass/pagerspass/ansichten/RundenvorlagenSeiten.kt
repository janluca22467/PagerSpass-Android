package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import de.pagerspass.pagerspass.mobil.Werkstand
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Kreiswache
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Rundenvorlagenzeile
import de.pagerspass.pagerspass.netz.Vorlagenbesatzung
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Gemerkte Rundeneinstellungen — das Gegenstück zu `RundenvorlagenDialog.vue`.
 *
 * <b>Eine Seite statt eines Dialogs.</b> Im Web steht die Liste in einer
 * Blende über dem Startbildschirm; am Handy ist eine Blende mit Liste,
 * Codefeld und zwei Knöpfen je Zeile ein Bildschirm, der keiner sein will.
 *
 * Angelegt wird eine Vorlage nicht hier, sondern in der Lobby („Vorlage"):
 * Der Server liest den Reglerstand selbst aus dem Raum.
 */
@Composable
fun RundenvorlagenSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Werkstand = Werkstand(),
    landkreise: List<Landkreis> = emptyList(),
    beiLaden: (Boolean) -> Unit = {},
    beiStarten: (Rundenvorlagenzeile) -> Unit = {},
    beiBearbeiten: (Rundenvorlagenzeile) -> Unit = {},
    beiLoeschen: (Rundenvorlagenzeile) -> Unit = {},
    beiUebernehmen: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden(true) }
    fun kreisname(id: String?) =
        if (id == null) "Erfundener Standardbereich" else landkreise.firstOrNull { it.id == id }?.name ?: id

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Vorlagen",
            unterzeile = "Gemerkte Rundeneinstellungen",
            knoepfe = { Knopf("← Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Rueckmeldung(stand.meldung, stand.fehler)

        Bereich(
            laedt = stand.rundenvorlagen.ersteLadung,
            fehler = stand.rundenvorlagen.fehler,
            inhalt = stand.rundenvorlagen.inhalt,
            beiErneut = { beiLaden(true) },
        ) { liste ->
            if (liste.isEmpty()) {
                Leerhinweis(
                    "Noch keine Vorlage. Stell eine Runde so ein, wie du sie magst, und speichere " +
                        "den Stand in der Lobby unter „Vorlage\" — dann steht er hier.",
                )
            }
            liste.forEach { v ->
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                    Wegzeile(
                        titel = v.name,
                        unterzeile = kreisname(v.landkreisId) +
                            (if (v.ganzerBereich) " · ganzer Bereich" else "") + " · Code ${v.code}",
                        zeichen = Zeichen.Leitstelle,
                        aktiv = !stand.laeuft,
                        beiDruck = { beiStarten(v) },
                    )
                    // Bearbeiten steht vor Löschen: Es ist der Griff, den man tut,
                    // und Löschen der, den man einmal tut.
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf("Bearbeiten", { beiBearbeiten(v) }, art = Knopfart.Leise, kompakt = true)
                        Loeschknopf({ beiLoeschen(v) }, aktiv = !stand.laeuft)
                    }
                }
            }
        }

        Codeuebernahme(
            etikett = "Vorlage übernehmen",
            erklaerung = "Der sechsstellige Code einer fremden Rundenvorlage — du bekommst eine eigene " +
                "Kopie. Ändert der andere seine später, bleibt deine, wie sie ist.",
            aktiv = !stand.laeuft,
            beiUebernehmen = beiUebernehmen,
        )
    }
}

/**
 * Den Inhalt einer Vorlage bearbeiten — Name, Plätze, Aufstellung mit
 * Funkrufnamen und Wache. Die Regler bleiben, wie sie in der Lobby waren.
 *
 * Das Gegenstück zu `VorlagenEditorDialog.vue`, am Handy als eigene Seite.
 */
@Composable
fun RundenvorlageEditorSeite(
    id: String,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Werkstand = Werkstand(),
    fahrzeuge: List<Fahrzeugvorlage> = emptyList(),
    beiLaden: () -> Unit = {},
    beiErneut: () -> Unit = beiLaden,
    beiAendern: (de.pagerspass.pagerspass.netz.Vorlageninhalt) -> Unit = {},
    beiSichern: () -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(id) { beiLaden() }
    var wahl by remember { mutableStateOf<String?>(null) }
    fun fahrzeugname(vid: String) = fahrzeuge.firstOrNull { it.id == vid }?.typ ?: vid
    val wachen = stand.inhaltswachen.sortedBy { it.name }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = stand.vorlageninhalt.inhalt?.name?.ifBlank { null } ?: "Vorlage",
            unterzeile = "Vorlage bearbeiten",
            knoepfe = { Knopf("Abbrechen", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = stand.vorlageninhalt.laedt,
            fehler = stand.vorlageninhalt.fehler,
            inhalt = stand.vorlageninhalt.inhalt,
            beiErneut = beiErneut,
        ) { inhalt ->
            Kasten(abstandInnen = Abstand.Klein) {
                Feld(inhalt.name, { beiAendern(inhalt.copy(name = it.take(60))) }, etikett = "Name")
                Zahlfeld(
                    "Plätze für Mitspieler",
                    inhalt.maxSpieler,
                    { t -> beiAendern(inhalt.copy(maxSpieler = t.trim().toIntOrNull())) },
                    platzhalter = "Vorgabe des Kreises",
                )
                SehrLeise("Leer lassen heißt: so viele, wie der Kreis hergibt. Bot-Besatzungen zählen nicht dagegen.")
            }

            Abschnitt("Aufstellung — ${inhalt.bestand.size} Besatzungen") {
                if (inhalt.bestand.isEmpty()) Leerhinweis("Noch keine Bot-Besatzung gespeichert.")
                inhalt.bestand.forEachIndexed { nr, b ->
                    fun setzen(neu: Vorlagenbesatzung) =
                        beiAendern(inhalt.copy(bestand = inhalt.bestand.toMutableList().also { it[nr] = neu }))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                        modifier = Modifier
                            .fillMaxWidth()
                            .flaeche(ecke = 9.dp)
                            .padding(Abstand.Klein),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fahrzeugname(b.vorlageId), style = Schrift.MonoNormal, color = Farben.Text, modifier = Modifier.weight(1f))
                            Knopf("Entfernen", {
                                beiAendern(inhalt.copy(bestand = inhalt.bestand.filterIndexed { i, _ -> i != nr }))
                            }, art = Knopfart.Leise, kompakt = true)
                        }
                        Feld(
                            b.funkrufname.orEmpty(),
                            { t ->
                                // Der Kurzname ist das letzte Wort des Rufnamens — wie
                                // `rufnameSetzen` im Web.
                                val sauber = t.take(60)
                                setzen(
                                    b.copy(
                                        funkrufname = sauber.ifBlank { null },
                                        kurzname = sauber.trim().split(Regex("\\s+")).lastOrNull()?.ifBlank { null },
                                    ),
                                )
                            },
                            platzhalter = "Rufname aus der Systematik",
                        )
                        Wahlfeld(
                            "Wache",
                            wachen.firstOrNull { it.kennung == b.wacheKennung }?.name ?: "Wache der Runde",
                            { wahl = "wache:$nr" },
                        )
                    }
                }
                Knopf("Fahrzeug hinzufügen", { wahl = "fahrzeug" }, art = Knopfart.Leise, kompakt = true)
            }

            Kasten(abstandInnen = Abstand.Klein) {
                Pillenreihe {
                    Knopf(
                        "Speichern",
                        beiSichern,
                        art = Knopfart.Haupt,
                        aktiv = !stand.laeuft && inhalt.name.isNotBlank(),
                    )
                    Knopf("Abbrechen", beiZurueck, art = Knopfart.Leise)
                }
                Rueckmeldung(null, stand.fehler)
            }

            val w = wahl
            when {
                w == "fahrzeug" -> Wahlblende(
                    titel = "Fahrzeug hinzufügen",
                    gruppen = fahrzeuge.sortedBy { it.typ }.groupBy { it.kategorie }.toList().sortedBy { it.first },
                    aufschrift = { it.typ },
                    unterschrift = { it.beschreibung.ifBlank { null } },
                    beiWahl = { f ->
                        beiAendern(inhalt.copy(bestand = inhalt.bestand + Vorlagenbesatzung(vorlageId = f.id)))
                        wahl = null
                    },
                    beiSchliessen = { wahl = null },
                    suchbar = true,
                )

                w != null && w.startsWith("wache:") -> {
                    val nr = w.substringAfter(":").toIntOrNull() ?: -1
                    val b = inhalt.bestand.getOrNull(nr)
                    val runde = Kreiswache(kennung = "", name = "Wache der Runde")
                    if (b != null) {
                        Wahlblende(
                            titel = "Wache",
                            gruppen = listOf(null to listOf(runde) + wachen),
                            aufschrift = { it.name },
                            gewaehlt = wachen.firstOrNull { it.kennung == b.wacheKennung } ?: runde,
                            beiWahl = { k ->
                                beiAendern(
                                    inhalt.copy(
                                        bestand = inhalt.bestand.toMutableList().also {
                                            it[nr] = b.copy(wacheKennung = k.kennung.ifBlank { null })
                                        },
                                    ),
                                )
                                wahl = null
                            },
                            beiSchliessen = { wahl = null },
                            suchbar = wachen.size > 8,
                        )
                    }
                }
            }
        }
    }
}
