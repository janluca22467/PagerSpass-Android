package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Lehrgangsseite
import de.pagerspass.pagerspass.netz.Lehrgang
import de.pagerspass.pagerspass.netz.Lehrgangsmodul
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Fehlerzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die Lehrgänge — übertragen aus `web/src/views/LehrgangView.vue`.
 *
 * Lesen, üben, prüfen — in dieser Reihenfolge, mit einem Zeugnis am Ende. Was
 * hier steht, kommt vom Server; abgehakt wird hier nur der Lesestoff (siehe
 * `Lehrgaenge`).
 *
 * <b>Der nächste offene Punkt trägt den Hauptknopf.</b> Die Reihenfolge ist der
 * Sinn eines Lehrgangs — wer die Prüfung zuerst fährt, prüft nichts. Wiederholen
 * darf man trotzdem alles.
 */
@Composable
fun LehrgangSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    stand: Lehrgangsseite = Lehrgangsseite(),
    beiLaden: () -> Unit = {},
    /** Den Lesestoff im Wiki öffnen — und danach abhaken. */
    beiLesen: (Lehrgang, Lehrgangsmodul) -> Unit = { _, _ -> },
    /** Die Lektion ist die Ausbildungsschicht. */
    beiLektion: () -> Unit = {},
    beiPruefung: (Lehrgang, Lehrgangsmodul) -> Unit = { _, _ -> },
    /** Die Prüfungsschicht im Dienstbuch ansehen — der Nachweis hinter dem Zeugnis. */
    beiSchicht: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
    /** Ob gerade eine Runde eröffnet wird (Lektion) — gegen den Doppeldruck. */
    laeuft: Boolean = false,
) {
    LaunchedEffect(Unit) { beiLaden() }

    val startet = stand.startet || laeuft

    Seite(modifier = modifier, unterrand = unterrand) {
        Etikett("Ausbildung")
        Seitenkopf(
            titel = "Lehrgänge",
            unterzeile = "Lesen, üben, prüfen — in dieser Reihenfolge. Wiederholen darfst du immer.",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        stand.fehler?.let { Fehlerzeile(it) }

        val liste = stand.liste
        when {
            liste.laedt && liste.inhalt == null -> Ladezeile()
            liste.fehler != null && liste.inhalt == null -> Fehlerzeile(liste.fehler, beiLaden)
            // Nur, wenn wirklich geholt wurde — sonst wäre „keine hinterlegt" eine
            // Auskunft über den Server, während nur die Antwort ausblieb.
            liste.geladen && liste.inhalt.orEmpty().isEmpty() ->
                Leerhinweis("Auf diesem Server sind keine Lehrgänge hinterlegt.")
        }

        liste.inhalt.orEmpty().forEach { l ->
            Lehrgangskarte(
                lehrgang = l,
                startet = startet,
                beiLesen = { m -> beiLesen(l, m) },
                beiLektion = beiLektion,
                beiPruefung = { m -> beiPruefung(l, m) },
                beiSchicht = beiSchicht,
            )
        }
    }
}

@Composable
private fun Lehrgangskarte(
    lehrgang: Lehrgang,
    startet: Boolean,
    beiLesen: (Lehrgangsmodul) -> Unit,
    beiLektion: () -> Unit,
    beiPruefung: (Lehrgangsmodul) -> Unit,
    beiSchicht: (String) -> Unit,
) {
    // Gesperrt ist kein Fehler, sondern eine Reihenfolge — deshalb der leise,
    // gestrichelte Rand und keine Signalfarbe. Der Lehrgang bleibt lesbar.
    val rahmen = if (lehrgang.gesperrt) {
        Modifier
            .background(Farben.Flaeche, Rundung.Normal)
            .drawBehind {
                drawRoundRect(
                    color = Farben.Rand,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
    } else {
        Modifier.flaeche()
    }

    val naechstes = lehrgang.module.firstOrNull { !it.erledigt }
    fun istDran(m: Lehrgangsmodul) = !lehrgang.gesperrt && naechstes?.id == m.id

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().then(rahmen).padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = lehrgang.titel,
                    style = Schrift.Gross,
                    color = if (lehrgang.gesperrt) Farben.TextLeise else Farben.Text,
                )
                if (lehrgang.beschreibung.isNotBlank()) Leise(lehrgang.beschreibung)
            }
            when {
                lehrgang.bestanden -> Schild("Bestanden", Farben.Gruen, Farben.AufFarbe)
                lehrgang.gesperrt -> Schild("Gesperrt", Farben.Rand, Farben.TextLeise)
            }
        }

        // Die Sperre nennt ihren Grund — ein ausgegrauter Knopf ohne Satz daneben
        // sieht aus wie ein Fehler.
        if (lehrgang.gesperrt) {
            Text(text = sperrsatz(lehrgang), style = Schrift.Klein, color = Farben.AmberHell)
        }

        // Das Zeugnis — mit der Schicht, in der geprüft wurde.
        if (lehrgang.bestanden) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.Gruen.copy(alpha = 0.12f), Rundung.Klein)
                    .padding(Abstand.Normal),
            ) {
                Text(
                    text = "${lehrgang.titel} — bestanden am ${tag(lehrgang.bestandenUm)}.",
                    style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                    color = Farben.Text,
                )
                lehrgang.zeugnisSchicht?.let { schicht ->
                    Textweg("Prüfungsschicht ansehen →", { beiSchicht(schicht) })
                }
            }
        }

        lehrgang.module.forEach { m ->
            Modulzeile(
                modul = m,
                dran = istDran(m),
                gesperrt = lehrgang.gesperrt,
                startet = startet,
                beiDruck = {
                    when (m.art) {
                        "Lesestoff" -> beiLesen(m)
                        "Lektion" -> beiLektion()
                        else -> beiPruefung(m)
                    }
                },
            )
        }
    }
}

@Composable
private fun Modulzeile(
    modul: Lehrgangsmodul,
    dran: Boolean,
    gesperrt: Boolean,
    startet: Boolean,
    beiDruck: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (dran) {
                    Modifier.background(Farben.HauchAmber, Rundung.Klein)
                } else {
                    Modifier
                },
            )
            .padding(Abstand.Klein),
    ) {
        Text(
            text = if (modul.erledigt) "✓" else "○",
            style = Schrift.Gross,
            color = if (modul.erledigt) Farben.GruenHell else Farben.TextSehrLeise,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "${ARTBEZEICHNUNG[modul.art] ?: modul.art}  ${modul.titel}",
                style = Schrift.Normal.copy(fontWeight = FontWeight.SemiBold),
                color = if (modul.erledigt) Farben.TextLeise else Farben.Text,
            )
            if (modul.text.isNotBlank()) Leise(modul.text)

            // Am Versuch aufgehängt, nicht an `erledigt`: Eine nicht bestandene
            // Prüfung gilt als nicht erledigt — genau dann muss der Satz stehen.
            if (modul.art == "Pruefung" && modul.erledigtUm != null) {
                Text(
                    text = (if (modul.bestanden) "Bestanden" else "Noch nicht bestanden — fahr sie noch einmal.") +
                        " · ${tag(modul.erledigtUm)}",
                    style = Schrift.Klein,
                    color = if (modul.bestanden) Farben.GruenHell else Farben.SignalHell,
                )
            }

            Box(Modifier.padding(top = Abstand.Winzig)) {
                val aufschrift = when (modul.art) {
                    "Lesestoff" -> if (modul.erledigt) "Nochmal lesen" else "Lesen"
                    "Lektion" -> if (modul.erledigt) "Nochmal fahren" else "Fahren"
                    else -> if (modul.bestanden) "Nochmal prüfen" else "Prüfung fahren"
                }
                Knopf(
                    aufschrift = aufschrift,
                    beiDruck = beiDruck,
                    art = if (dran) Knopfart.Haupt else Knopfart.Normal,
                    aktiv = !gesperrt && (modul.art == "Lesestoff" || !startet) &&
                        (modul.art != "Lesestoff" || modul.wikiseite != null),
                    kompakt = true,
                )
            }
        }
    }
}

/** Eine kleine gefüllte Marke — „Bestanden", „Gesperrt". */
@Composable
private fun Schild(text: String, grund: androidx.compose.ui.graphics.Color, schrift: androidx.compose.ui.graphics.Color) {
    Text(
        text = text.uppercase(),
        style = Schrift.Winzig.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.ExtraBold),
        color = schrift,
        modifier = Modifier
            .background(grund, Rundung.Winzig)
            .padding(horizontal = Abstand.Klein, vertical = Abstand.Haar),
    )
}

private val ARTBEZEICHNUNG = mapOf(
    "Lesestoff" to "LESEN",
    "Lektion" to "LEKTION",
    "Pruefung" to "PRÜFUNG",
)

/**
 * Was über einem gesperrten Lehrgang steht — aus den Titeln, die der Server
 * mitschickt, und nicht aus einer eigenen Liste: Welche Lehrgänge einander
 * voraussetzen, steht am Server.
 */
private fun sperrsatz(lehrgang: Lehrgang): String {
    val namen = lehrgang.voraussetzungen.map { "„$it“" }
    if (namen.isEmpty()) return "Dieser Lehrgang ist noch gesperrt."

    val liste = if (namen.size == 1) {
        namen.first()
    } else {
        namen.dropLast(1).joinToString(", ") + " und " + namen.last()
    }
    return "Erst nach $liste."
}
