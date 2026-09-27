package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import de.pagerspass.pagerspass.mobil.Wachenstand
import de.pagerspass.pagerspass.netz.Gemeinschaftsantrag
import de.pagerspass.pagerspass.netz.Gemeinschaftsfilter
import de.pagerspass.pagerspass.netz.Landkreis
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
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/** Ein Code aus Adresse oder Feld: Großbuchstaben und Ziffern, höchstens sechs. */
internal fun codeBereinigen(roh: String?): String =
    roh.orEmpty().uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(6)

/**
 * Der Weg in eine Wachengemeinschaft — für alle, die in keiner sind.
 * Übertragen aus `GemeinschaftenView.vue`.
 *
 * <b>Drei Wege untereinander statt in einer Rangfolge</b>, weil sie
 * verschiedene Leute bedienen: Die Einladung erreicht den, den jemand kennt;
 * der Beitrittscode den, der neben jemandem sitzt; die öffentliche Liste den,
 * der niemanden kennt.
 */
@Composable
internal fun ColumnScope.WachenSuche(
    antraege: List<Gemeinschaftsantrag>,
    wachen: Wachenstand,
    landkreise: List<Landkreis>,
    code: String?,
    griffe: WachenGriffe,
) {
    val speicher = griffe.speicher
    val filter = wachen.filter
    val ausLink = codeBereinigen(code).takeIf { it.isNotEmpty() }
    var feldcode by rememberSaveable(code) { mutableStateOf(ausLink.orEmpty()) }
    var gruendenOffen by remember { mutableStateOf(false) }
    val bewerbung = remember { mutableStateMapOf<String, String>() }

    val einladungen = antraege.filter { it.richtung == "Einladung" && it.stand == "Offen" }
    val bewerbungen = antraege.filter { it.richtung == "Bewerbung" && it.stand == "Offen" }

    LaunchedEffect(Unit) { speicher.oeffentlicheLaden(filter) }

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        Etikett("Wachengemeinschaften")
        Seitenkopf(titel = "Eine Wache finden")
    }

    WachenNavigation(
        hier = "wache",
        hatWache = false,
        beiWache = {},
        beiRangliste = griffe.zurRangliste,
        beiShop = {},
    )

    Leise("Eine feste Mannschaft mit eigenem Chat — jedes Konto gehört höchstens einer an.")

    wachen.meldung?.let { Meldungszeile(it) }

    // Einladungen zuerst — auf die muss man antworten.
    if (einladungen.isNotEmpty()) {
        Ueberschrift("Du bist eingeladen")
        einladungen.forEach { a ->
            Kasten(marke = true, abstandInnen = Abstand.Klein) {
                Text(a.gemeinschaftName, style = Schrift.Normal, color = Farben.Text)
                SehrLeise("von ${a.vonName ?: "—"}")
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        "Beitreten",
                        { speicher.antragEntscheiden(a.nr, true) },
                        art = Knopfart.Haupt,
                        aktiv = !wachen.laeuft,
                        kompakt = true,
                    )
                    Knopf(
                        "Ablehnen",
                        { speicher.antragEntscheiden(a.nr, false) },
                        art = Knopfart.Leise,
                        aktiv = !wachen.laeuft,
                        kompakt = true,
                    )
                }
            }
        }
    }

    Ueberschrift("Mit Beitrittscode")
    Kasten(abstandInnen = Abstand.Klein) {
        // Wer über einen Link kommt, sieht den Code hier stehen und tritt erst mit
        // dem Knopf bei — ein Link allein bringt niemanden in eine Gemeinschaft.
        if (ausLink != null && feldcode == ausLink) {
            Text(
                text = "Du kommst über einen Link mit dem Code $ausLink. Beigetreten wird erst, wenn du " +
                    "auf „Beitreten“ tippst.",
                style = Schrift.Klein,
                color = Farben.Text,
            )
        } else {
            SehrLeise("Sechs Zeichen, wie ein Raumcode — vom Zugführer oder von der Leitung.")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Feld(
                wert = feldcode,
                beiAenderung = { feldcode = codeBereinigen(it) },
                platzhalter = "ABC123",
                stil = Schrift.MonoNormal,
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "Beitreten",
                { speicher.beitretenMitCode(feldcode) },
                art = Knopfart.Haupt,
                aktiv = !wachen.laeuft && feldcode.length == 6,
                kompakt = true,
            )
        }
    }

    Ueberschrift("Selbst gründen")
    Kasten(abstandInnen = Abstand.Klein) {
        SehrLeise("Ab Level 3. Du wirst die Leitung und bestimmst, wer dazukommt und wie.")
        Knopf(
            "Gemeinschaft gründen",
            {
                speicher.landkreisvorschlagLaden()
                gruendenOffen = true
            },
            kompakt = true,
        )
    }

    // ------------------------------------------------------ Öffentliche Liste
    Ueberschrift("Öffentliche Gemeinschaften")

    // Die Filter stehen offen da: Die Vorauswahl „Nur, wo ich aufgenommen werde"
    // verändert die Liste — wer das nicht sieht, hält die kurze Liste für alles.
    Kasten(abstandInnen = Abstand.Klein) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Feld(
                wert = filter.suche,
                beiAenderung = { speicher.filterSetzen(filter.copy(suche = it.take(60))) },
                platzhalter = "Name oder Stichwort",
                modifier = Modifier.weight(1f),
            )
            Knopf("Suchen", { speicher.oeffentlicheLaden(filter) }, aktiv = !wachen.laeuft, kompakt = true)
        }

        Landkreiswahl(
            etikett = "Landkreis",
            gewaehlt = landkreise.firstOrNull { it.id == filter.landkreisId },
            landkreise = landkreise,
            leer = "Alle",
            beiWahl = { speicher.oeffentlicheLaden(filter.copy(landkreisId = it?.id)) },
        )

        Etikett("Aufnahme")
        Pillenreihe {
            listOf(null, "Offen", "Antrag", "Einladung").forEach { modus ->
                Pille(
                    aufschrift = modus?.let { modustext(it) } ?: "Egal",
                    an = filter.modus == modus,
                    beiDruck = { speicher.oeffentlicheLaden(filter.copy(modus = modus)) },
                )
            }
        }

        Hakenzeile(
            text = "Nur, wo ich aufgenommen werde",
            an = filter.nurPassende,
            beiWechsel = { speicher.oeffentlicheLaden(filter.copy(nurPassende = it)) },
        )
        Hakenzeile(
            text = "Nur mit freien Plätzen",
            an = filter.nurFreie,
            beiWechsel = { speicher.oeffentlicheLaden(filter.copy(nurFreie = it)) },
        )

        if (filter.eingegrenzt) {
            Knopf(
                "Filter zurücksetzen",
                { speicher.oeffentlicheLaden(Gemeinschaftsfilter()) },
                art = Knopfart.Leise,
                aktiv = !wachen.laeuft,
                kompakt = true,
            )
        }
    }

    when {
        wachen.oeffentliche.isEmpty() && wachen.oeffentlicheLaeuft -> Ladezeile()

        wachen.oeffentliche.isEmpty() -> Leerhinweis(
            if (filter.eingegrenzt) {
                "Zu dieser Auswahl passt gerade keine Wache. Nimm einen Filter heraus — oder gründe selbst eine."
            } else {
                "Gerade ist keine öffentlich gelistet. Der Beitrittscode führt trotzdem überall hinein."
            },
        )

        else -> wachen.oeffentliche.forEach { g ->
            Kasten(abstandInnen = Abstand.Klein) {
                Text(
                    text = g.name,
                    style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold),
                    color = Farben.Text,
                )
                SehrLeise(
                    buildString {
                        append("${g.mitglieder} von ${g.maxMitglieder}")
                        g.landkreis?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                        append(" · ${modustext(g.beitrittModus)}")
                        if (g.mindestLevel > 1) append(" · ab Level ${g.mindestLevel}")
                    },
                )
                g.beschreibung?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = Schrift.Klein, color = Farben.TextLeise)
                }

                // Bei „Nur auf Einladung" gibt es bewusst keinen Knopf.
                if (g.beitrittModus != "Einladung") {
                    if (g.beitrittModus == "Antrag") {
                        Feld(
                            wert = bewerbung[g.id].orEmpty(),
                            beiAenderung = { bewerbung[g.id] = it.take(300) },
                            platzhalter = "Kurz zu dir (freiwillig)",
                        )
                    }
                    Knopf(
                        if (g.beitrittModus == "Offen") "Beitreten" else "Bewerben",
                        { speicher.bewerben(g.id, bewerbung[g.id]?.trim()?.ifEmpty { null }) },
                        aktiv = !wachen.laeuft,
                        kompakt = true,
                    )
                }
            }
        }
    }

    // Eigene Bewerbungen — Auskunft, kein Handlungsbedarf.
    if (bewerbungen.isNotEmpty()) {
        Ueberschrift("Deine offenen Bewerbungen")
        bewerbungen.forEach { a ->
            Kasten(abstandInnen = Abstand.Klein) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(a.gemeinschaftName, style = Schrift.Normal, color = Farben.Text)
                        SehrLeise("Wartet auf eine Antwort.")
                    }
                    Knopf(
                        "Zurückziehen",
                        { speicher.antragEntscheiden(a.nr, false) },
                        art = Knopfart.Leise,
                        aktiv = !wachen.laeuft,
                        kompakt = true,
                    )
                }
            }
        }
    }

    if (gruendenOffen) {
        GruendenBlende(
            landkreise = landkreise,
            vorschlagId = wachen.landkreisvorschlag?.landkreisId,
            laeuft = wachen.laeuft,
            meldung = wachen.meldung,
            beiGruenden = { name, beschreibung, kreis ->
                speicher.gruenden(name, beschreibung, kreis?.id, kreis?.name) { gruendenOffen = false }
            },
            beiSchliessen = { gruendenOffen = false },
        )
    }
}
