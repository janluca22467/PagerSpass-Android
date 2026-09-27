package de.pagerspass.pagerspass.ansichten

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Iconstand
import de.pagerspass.pagerspass.netz.Iconpack
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Icon-Bibliothek — alle eigenen Fahrzeug-Icon-Packs, übertragen aus
 * `web/src/views/IconBibliothekView.vue`.
 *
 * <b>Warum eine eigene Seite und keine Blende der Welt.</b> Ein Zeichenwerkzeug
 * mit Zoom in einem Fenster über der Karte wäre beides halb; als Seite hat es den
 * ganzen Schirm.
 *
 * <b>„Standard" steht hier nicht.</b> Er ist kein Pack, sondern der gezeichnete
 * Riss — nicht bearbeitbar. Gewählt wird er in den Einstellungen der Welt.
 */
@Composable
fun IconBibliothekSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Iconstand = Iconstand(),
    beiLaden: () -> Unit = {},
    beiBearbeiten: (String) -> Unit = {},
    beiAnlegen: (String) -> Unit = {},
    beiUmbenennen: (Iconpack, String) -> Unit = { _, _ -> },
    beiLoeschen: (Iconpack) -> Unit = {},
    beiTeilen: (Iconpack) -> Unit = {},
    beiUebernehmen: (code: String, danach: () -> Unit) -> Unit = { _, _ -> },
    beiImport: (Uri) -> Unit = {},
    beiZurWelt: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    val browser = LocalUriHandler.current
    var neuerName by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var umbenennen by remember { mutableStateOf<Iconpack?>(null) }
    var loeschen by remember { mutableStateOf<Iconpack?>(null) }

    val zipwahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { quelle ->
        if (quelle != null) beiImport(quelle)
    }

    val grenzen = stand.grenzen
    val gesperrt = stand.sendet || stand.voll

    Seite(modifier = modifier, unterrand = unterrand) {
        Etikett("PagerSpass - World")
        Seitenkopf(
            titel = "Fahrzeug-Icons",
            knoepfe = { Knopf("Zurück zur Welt", beiZurWelt, kompakt = true) },
        )

        Leise(
            "Deine Fahrzeuge auf der Weltkarte zeichnet das Spiel selbst. Hier legst du " +
                "eigene Grafiken darüber — für jeden Typ eine, oder für ein paar. Was du " +
                "nicht ersetzt, bleibt gezeichnet.",
        )

        stand.fehler?.let { Fehlerzeile(it) }

        if (stand.laedt && stand.packs.isEmpty() && grenzen == null) {
            Ladezeile()
            return@Seite
        }

        stand.packs.forEach { p ->
            Kasten(abstandInnen = Abstand.Klein) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = p.name,
                        style = Schrift.Gross,
                        color = Farben.Text,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (p.aktiv) {
                        Text("gilt auf deiner Karte", style = Schrift.Winzig, color = Farben.Amber)
                    }
                    if (p.gesperrt) {
                        Text("gesperrt", style = Schrift.Winzig, color = Farben.SignalHell)
                    }
                }
                SehrLeise(
                    "${p.belegt} von ${stand.typen.size} Fahrzeugtypen · ${iconKilobyte(p.bytes)} von " +
                        "${iconKilobyte(grenzen?.maxBytesJePack ?: 0L)} KB",
                )
                Pillenreihe {
                    Knopf("Bearbeiten", { beiBearbeiten(p.id) }, art = Knopfart.Haupt, kompakt = true)
                    Knopf("Umbenennen", { umbenennen = p }, aktiv = !stand.sendet, kompakt = true)
                    Knopf("Code holen", { beiTeilen(p) }, aktiv = !stand.sendet, kompakt = true)
                    Knopf("Löschen", { loeschen = p }, art = Knopfart.Gefahr, aktiv = !stand.sendet, kompakt = true)
                }

                // Der Code erscheint erst, wenn jemand ihn geholt hat — er entsteht
                // auch am Server erst dann.
                stand.codeVon?.takeIf { it.first == p.id }?.let { (_, c) ->
                    Text(
                        text = "Zum Weitergeben: $c",
                        style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                    )
                    SehrLeise("Wer ihn eingibt, bekommt eine eigene Kopie.")
                }
            }
        }

        if (stand.packs.isEmpty() && !stand.laedt) {
            Leerhinweis(
                "Du hast noch kein Icon-Pack. Leg eins an — du musst nicht alle Fahrzeuge " +
                    "malen, ein einziges reicht.",
            )
        }

        Kasten(abstandInnen = Abstand.Klein) {
            Ueberschrift("Neues Pack")
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Feld(
                    wert = neuerName,
                    beiAenderung = { neuerName = it.take(grenzen?.maxNameLaenge ?: 40) },
                    platzhalter = "Name, zum Beispiel „Feuerrot“",
                    aktiv = !gesperrt,
                    weiterTaste = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Anlegen",
                    {
                        beiAnlegen(neuerName)
                        neuerName = ""
                    },
                    art = Knopfart.Haupt,
                    aktiv = !gesperrt,
                    kompakt = true,
                )
            }
            if (stand.voll) {
                SehrLeise("Mehr als ${grenzen?.maxPacks} Packs gehen nicht. Benenne eins um oder lösche es.")
            }
        }

        Kasten(abstandInnen = Abstand.Klein) {
            Ueberschrift("Pack per Code übernehmen")
            SehrLeise(
                "Was dabei entsteht, ist eine eigene Kopie. Wer dir den Code gegeben hat, " +
                    "kann seins danach löschen, ohne dass deine Fahrzeuge ausfallen.",
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Feld(
                    wert = code,
                    beiAenderung = { code = it.uppercase().take(6) },
                    platzhalter = "ABC234",
                    stil = Schrift.MonoNormal,
                    aktiv = !gesperrt,
                    weiterTaste = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Übernehmen",
                    { beiUebernehmen(code) { code = "" } },
                    aktiv = !gesperrt && code.isNotBlank(),
                    kompakt = true,
                )
            }
        }

        Kasten(abstandInnen = Abstand.Klein) {
            Ueberschrift("Pack aus einer Zip importieren")
            SehrLeise(
                "Die Datei heißt Pack_NAME.zip — NAME wird der Packname. Darin liegen eine " +
                    "details.json, die je Fahrzeugtyp die Grafik nennt, und ein Ordner assets " +
                    "mit den Grafiken. Wie das genau aussieht, steht im Wiki.",
            )
            Textweg("Wiki: Pack als Zip importieren", {
                browser.openUri("https://wiki.pagerspass.de/world/#pack-als-zip-importieren")
            })
            Knopf(
                "Zip wählen",
                { zipwahl.launch(arrayOf("application/zip", "application/x-zip-compressed")) },
                aktiv = !gesperrt,
                kompakt = true,
            )
            if (stand.voll) {
                SehrLeise(
                    "Mehr als ${grenzen?.maxPacks} Packs gehen nicht — auch nicht per Import. " +
                        "Lösche eins, um Platz zu machen.",
                )
            }

            stand.importiert?.let { bericht ->
                Text(
                    text = "${bericht.pack.name} ist da — ${bericht.uebernommen} " +
                        (if (bericht.uebernommen == 1) "Icon" else "Icons") + " übernommen" +
                        (if (bericht.uebersprungen.isNotEmpty()) ", ${bericht.uebersprungen.size} übersprungen" else "") +
                        ".",
                    style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                    color = Farben.Text,
                )
                bericht.uebersprungen.forEach { u ->
                    SehrLeise("${u.vorlageId} — ${u.grund}")
                }
            }
        }
    }

    umbenennen?.let { pack ->
        var name by remember(pack.id) { mutableStateOf(pack.name) }
        Blende(
            titel = "Pack umbenennen",
            beiSchliessen = { umbenennen = null },
            breite = Dialogbreite.Schmal,
            fuss = {
                Knopf("Abbrechen", { umbenennen = null }, art = Knopfart.Leise)
                Knopf(
                    "Übernehmen",
                    {
                        beiUmbenennen(pack, name.trim())
                        umbenennen = null
                    },
                    art = Knopfart.Haupt,
                    aktiv = name.isNotBlank(),
                )
            },
        ) {
            Feld(
                wert = name,
                beiAenderung = { name = it.take(grenzen?.maxNameLaenge ?: 40) },
                etikett = "Neuer Name",
                weiterTaste = ImeAction.Done,
            )
        }
    }

    // Ein Pack ist Arbeit, die nicht zurückkommt: Die Rückfrage nennt deshalb den
    // Namen und die Zahl der Icons, nicht nur „Wirklich löschen?".
    loeschen?.let { pack ->
        Blende(
            titel = "Pack löschen",
            beiSchliessen = { loeschen = null },
            breite = Dialogbreite.Schmal,
            fuss = {
                Knopf("Abbrechen", { loeschen = null }, art = Knopfart.Leise)
                Knopf(
                    "Endgültig löschen",
                    {
                        beiLoeschen(pack)
                        loeschen = null
                    },
                    art = Knopfart.Gefahr,
                )
            },
        ) {
            Text(
                text = "„${pack.name}\" mit ${pack.belegt} Icons endgültig löschen?",
                style = Schrift.Normal,
                color = Farben.Text,
            )
        }
    }
}

/** Kilobyte statt Bytes — sechs Millionen sagt niemandem etwas. */
internal fun iconKilobyte(bytes: Long): Long = Math.round(bytes / 1024.0)
