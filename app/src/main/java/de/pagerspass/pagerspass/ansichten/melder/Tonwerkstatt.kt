package de.pagerspass.pagerspass.ansichten.melder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.melder.Meldergeraet
import de.pagerspass.pagerspass.melder.Meldertoene
import de.pagerspass.pagerspass.melder.Melderspieler
import de.pagerspass.pagerspass.melder.TONBAU_GRUPPE
import de.pagerspass.pagerspass.melder.TONBAU_HOECHSTZAHL
import de.pagerspass.pagerspass.melder.TONBAU_LEITER
import de.pagerspass.pagerspass.melder.TONBAU_SCHRITTE
import de.pagerspass.pagerspass.melder.TONBAU_TAKTE
import de.pagerspass.pagerspass.melder.Wellenform
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlin.math.abs

/**
 * Die Tonwerkstatt — `components/ui/Tonwerkstatt.vue`.
 *
 * <b>Ein Raster, keine Schieberegler.</b> Sechzehn Felder, jedes eine Höhe aus
 * der Leiter oder eine Pause; dazu Takt und Klangfarbe. Man wählt oben den Stift
 * (eine Höhe oder „Pause") und tippt die Felder an — dasselbe Bild wie am
 * Rechner, nur in zwei Reihen zu acht, damit jedes Feld groß genug für einen
 * Daumen bleibt.
 *
 * <b>Der Entwurf ist nicht der gesicherte Ton.</b> Wer am Raster tippt, ändert
 * die Werkbank; was er sichert, bekommt eine eigene Kennung. Ohne die Trennung
 * überschriebe jeder Tipp den eingestellten Alarmton — und man merkte es erst
 * beim nächsten Einsatz.
 */
@Composable
fun Tonwerkstatt(modifier: Modifier = Modifier, unterrand: Dp = 0.dp, beiZurueck: () -> Unit) {
    val zusammenhang = LocalContext.current
    val geraet = remember { Meldergeraet.bereit(zusammenhang) }
    val plan = geraet.werkbank

    var stift by remember { mutableStateOf<Int?>(TONBAU_LEITER[3].second) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var meldung by remember { mutableStateOf<String?>(null) }
    var bearbeitet by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { Melderspieler.stoppen("probe") } }

    fun probe() {
        Melderspieler.stoppen("probe")
        Melderspieler.probe(zusammenhang, "eigen", 1)
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Tonwerkstatt",
            unterzeile = "Dein eigener Alarmton — bleibt auf diesem Gerät",
            knoepfe = { Knopf("Fertig", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Abschnitt("Stift") {
            SehrLeise("Wähle eine Höhe — dann tippst du die Felder an. Ein zweiter Tipp mit demselben Stift leert das Feld.")
            Pillenreihe {
                Pille("Pause", an = stift == null, beiDruck = { stift = null })
                TONBAU_LEITER.forEach { (wort, hz) ->
                    Pille(wort, an = stift == hz, beiDruck = { stift = hz })
                }
            }
        }

        Abschnitt("Raster") {
            for (reihe in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                    for (s in 0 until 8) {
                        val i = reihe * 8 + s
                        val wert = plan.schritte.getOrNull(i)
                        if (s > 0 && s % TONBAU_GRUPPE == 0) Spacer(Modifier.width(6.dp))
                        val stufe = wert?.let { hz -> TONBAU_LEITER.indexOfFirst { it.second == hz } } ?: -1
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .background(
                                    if (wert != null) Farben.Amber.copy(alpha = 0.25f + 0.05f * (stufe % 8)) else Farben.BgTief,
                                    Rundung.Winzig,
                                )
                                .border(1.dp, if (wert != null) Farben.Amber else Farben.Rand, Rundung.Winzig)
                                .clickable(role = Role.Button) {
                                    val neu = plan.schritte.toMutableList()
                                    while (neu.size < TONBAU_SCHRITTE) neu += null
                                    neu[i] = if (neu[i] == stift) null else stift
                                    // Ein leeres Raster ist kein Ton — dann bleibt das Feld stehen.
                                    if (neu.any { it != null }) geraet.werkbankSetzen(plan.copy(schritte = neu))
                                },
                        ) {
                            Text(
                                text = if (wert == null) "·" else TONBAU_LEITER.getOrNull(stufe)?.first?.take(4).orEmpty(),
                                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                                color = if (wert != null) Farben.Text else Farben.TextSehrLeise,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            SehrLeise(
                "Ein Durchlauf endet am Ende des Viertels, in dem der letzte Ton steht — " +
                    "wer eine lange Pause will, setzt einen Ton in ein späteres Viertel.",
            )
        }

        Abschnitt("Takt und Klangfarbe") {
            val takt = TONBAU_TAKTE.minByOrNull { abs(it.second - plan.abstand) } ?: TONBAU_TAKTE[1]
            Segment(
                seiten = TONBAU_TAKTE,
                gewaehlt = takt,
                beiWahl = { geraet.werkbankSetzen(plan.copy(abstand = it.second, dauer = it.third)) },
                aufschrift = { it.first },
                modifier = Modifier.fillMaxWidth(),
            )
            Segment(
                seiten = Wellenform.entries.toList(),
                gewaehlt = Wellenform.aus(plan.form),
                beiWahl = { geraet.werkbankSetzen(plan.copy(form = it.kennung)) },
                aufschrift = { it.name_ },
                modifier = Modifier.fillMaxWidth(),
            )
            SehrLeise(Wellenform.aus(plan.form).was)
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("▶ Probe", ::probe, kompakt = true)
                Knopf("Als Alarmton nehmen", {
                    geraet.tonSetzen("eigen")
                    meldung = "Der Entwurf ist jetzt dein Alarmton — sichere ihn, damit er bleibt."
                }, kompakt = true, art = Knopfart.Leise)
            }
        }

        Abschnitt(if (bearbeitet != null) "Ton aktualisieren" else "Sichern") {
            Feld(wert = name, beiAenderung = { name = it.take(24) }, etikett = "Name", platzhalter = "Zum Beispiel „Wache 3“")
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                val id = bearbeitet
                if (id != null) {
                    Knopf("Überschreiben", {
                        geraet.tonAendern(id, name, plan)
                        meldung = "Gesichert."
                        bearbeitet = null
                    }, kompakt = true)
                    Knopf("Als neuer Ton", { bearbeitet = null }, kompakt = true, art = Knopfart.Leise)
                } else {
                    Knopf("Sichern", {
                        val satz = geraet.tonSichern(name, plan)
                        meldung = if (satz == null) {
                            "Mehr als $TONBAU_HOECHSTZAHL eigene Töne gehen nicht. Lösche einen, um Platz zu machen."
                        } else {
                            geraet.tonSetzen("eigen:${satz.id}")
                            name = ""
                            "„${satz.name}“ ist gesichert und dein Alarmton."
                        }
                    }, kompakt = true, aktiv = geraet.eigeneToene.size < TONBAU_HOECHSTZAHL)
                }
            }
            meldung?.let { Text(it, style = Schrift.Klein, color = Farben.AmberHell) }
        }

        Abschnitt("Weitergeben") {
            SehrLeise("Ein Ton reist als Code — über Chat, Zettel oder Telefon. Derselbe Code klingt im Browser genauso.")
            SelectionContainer {
                Text(
                    Meldertoene.tonCodeVon(plan).chunked(4).joinToString(" "),
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Amber,
                )
            }
            Feld(wert = code, beiAenderung = { code = it.take(24) }, etikett = "Code einlesen", platzhalter = "XXXX XXXX XXXX XXXX")
            Knopf("Auf die Werkbank", {
                val gelesen = Meldertoene.tonCodeLesen(code)
                if (gelesen == null) {
                    meldung = "Dieser Code passt nicht — sechzehn Zeichen, vertippt ist schnell."
                } else {
                    geraet.werkbankSetzen(gelesen)
                    code = ""
                    meldung = "Der Ton liegt auf der Werkbank."
                    probe()
                }
            }, kompakt = true, aktiv = code.isNotBlank())
        }

        Abschnitt("Deine Töne (${geraet.eigeneToene.size}/$TONBAU_HOECHSTZAHL)") {
            if (geraet.eigeneToene.isEmpty()) SehrLeise("Noch keiner gesichert.")
            geraet.eigeneToene.forEach { t ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier.fillMaxWidth().flaeche().padding(Abstand.Normal),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.name, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text, modifier = Modifier.weight(1f))
                        if (geraet.ton == "eigen:${t.id}") Text("GEWÄHLT", style = Schrift.Winzig, color = Farben.Amber)
                    }
                    Pillenreihe {
                        Pille("▶", an = false, beiDruck = { Melderspieler.stoppen("probe"); Melderspieler.probe(zusammenhang, "eigen:${t.id}") })
                        Pille("Wählen", an = geraet.ton == "eigen:${t.id}", beiDruck = { geraet.tonSetzen("eigen:${t.id}") })
                        Pille("Bearbeiten", an = bearbeitet == t.id, beiDruck = {
                            geraet.werkbankSetzen(t.plan)
                            name = t.name
                            bearbeitet = t.id
                        })
                        Pille("Löschen", an = false, farbe = Farben.Signal, beiDruck = {
                            geraet.tonLoeschen(t.id)
                            if (bearbeitet == t.id) bearbeitet = null
                        })
                    }
                }
            }
        }
    }
}
