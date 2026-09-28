package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.mobil.weltzeit
import de.pagerspass.pagerspass.netz.WeltZiele
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Der Großeinsatz der Woche — übertragen aus `components/welt/GrosslageBlende.vue`:
 * Stand, Wellen, Bedarfsblatt, Bereitstellungsraum mit Vorschicken, die Vorwoche und
 * darunter die drei Wochenziele.
 */
@Composable
fun WeltGrosslageBlende(welt: Welt, stand: Weltzustand, griffe: Weltgriffe) {
    val bereich = rememberCoroutineScope()

    var ziele by remember { mutableStateOf<WeltZiele?>(null) }
    var angehakt by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sendet by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var wahlOffen by remember { mutableStateOf(false) }

    LaunchedEffect(stand.gutschrift) {
        ziele = welt.holen("") { welt.wege.ziele(it) }.getOrNull()
    }

    val jetzt by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    val serverJetzt = jetzt + welt.fahrt.versatzMs

    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Normal), modifier = Modifier.fillMaxWidth()) {
        val g = stand.grosslage
        if (g == null) {
            Leerhinweis("Für diese Woche ist noch nichts angekündigt. Der Termin wird montags gezogen.")
        } else {
            val anteil = if (g.bedarf == 0) 0 else minOf(100, ((g.gestellt.toDouble() / g.bedarf) * 100).roundToInt())

            val countdown = run {
                val ziel = weltzeit(if (g.zustand == "Angekuendigt") g.beginntUm else g.endetUm) ?: return@run ""
                val ms = ziel - serverJetzt
                if (ms <= 0) return@run if (g.zustand == "Angekuendigt") "jetzt" else "vorbei"
                val stunden = ms / 3_600_000
                val minuten = (ms % 3_600_000) / 60_000
                when {
                    stunden >= 24 -> "in ${stunden / 24} Tagen, ${stunden % 24} h"
                    stunden > 0 -> "in $stunden h $minuten min"
                    else -> "in $minuten min"
                }
            }
            val naechsteWelle = run {
                val um = weltzeit(g.naechsteWelleUm) ?: return@run ""
                val ms = um - serverJetzt
                if (ms <= 0) return@run "gleich"
                val stunden = ms / 3_600_000
                val minuten = ceil((ms % 3_600_000) / 60_000.0).toInt()
                if (stunden > 0) "in $stunden h $minuten min" else "in $minuten min"
            }
            val termin = weltzeit(g.beginntUm)?.let {
                SimpleDateFormat("EEEE, dd.MM., HH:mm", Locale.GERMANY).format(Date(it))
            } ?: g.beginntUm

            // ------------------------------------------------------- Kopf
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                Text(text = g.name, style = Schrift.Titel, color = Farben.Text, modifier = Modifier.weight(1f))
                Weltmarke(
                    text = when (g.zustand) {
                        "Laeuft" -> "läuft"
                        "Vorbei" -> "vorbei"
                        else -> "angekündigt"
                    },
                    farbe = when (g.zustand) {
                        "Laeuft" -> Farben.SignalHell
                        "Vorbei" -> Farben.TextSehrLeise
                        else -> Farben.Amber
                    },
                )
            }
            Text(text = countdown, style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold), color = Farben.Amber)
            Text(text = "$termin · zwölf Stunden · ${g.ort}", style = Schrift.Klein, color = Farben.TextLeise)

            // ------------------------------------------------------- Bedarf
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
            ) {
                Zeile("Einsatzkräfte an den Abschnitten", zahl(g.gestellteKraefte), stark = true)
                Zeile("Fahrzeuge im Einsatz — von allen Leitstellen", "${g.gestellt} / ${g.bedarf}")
                Box(Modifier.fillMaxWidth().height(6.dp).background(Farben.BgTief, Rundung.Rund)) {
                    Box(Modifier.fillMaxWidth(anteil / 100f).fillMaxHeight().background(Farben.Amber, Rundung.Rund))
                }
                Hinweis(
                    "${g.bedarf} Fahrzeuge fordert dieser Großeinsatz insgesamt — mehr, als eine Leitstelle allein " +
                        "stellen kann. Bei $anteil % ist der Bonus zu $anteil % verdient; voll gibt es ihn bei gedecktem Bedarf.",
                )
                if (g.zustand == "Laeuft") {
                    Zeile("Welle", "${g.wellenStand} / ${g.wellen}")
                    Hinweis(
                        if (naechsteWelle.isNotEmpty()) {
                            "Der Bedarf kommt in Wellen über die zwölf Stunden verteilt — die nächste Welle bringt $naechsteWelle neue Abschnitte."
                        } else {
                            "Alle Wellen sind abgeworfen — was jetzt offen ist, ist alles, was noch kommt."
                        },
                    )
                    Zeile("Abschnitte, die noch Fahrzeuge brauchen", g.offeneLagen.toString())
                    if (g.offenePlaetze > 0) Zeile("Freie Fahrzeugplätze dort", g.offenePlaetze.toString())
                    g.bedarfe.forEach { b ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                            Weltmarke(b.faehigkeit, if (b.imRaum < b.fehlt) Farben.SignalHell else Farben.TextLeise)
                            Text(
                                text = "fehlt an ${b.fehlt} ${if (b.fehlt == 1) "Abschnitt" else "Abschnitten"} · ${b.imRaum} im Raum",
                                style = Schrift.Winzig,
                                color = Farben.TextSehrLeise,
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------------- Bereitstellungsraum
            if (g.anfahrtOffen) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.fillMaxWidth().flaeche(ecke = 9.dp).padding(Abstand.Normal),
                ) {
                    Zeile(
                        "Im Bereitstellungsraum",
                        "${zahl(g.bereitgestellteKraefte)} Kräfte" +
                            if (g.meineBereitgestelltenKraefte > 0) " · davon ${zahl(g.meineBereitgestelltenKraefte)} von dir" else "",
                        stark = true,
                    )
                    if (g.bereitgestellt > 0) {
                        Zeile(
                            "Fahrzeuge dort",
                            "${g.bereitgestellt}" + if (g.meineBereitgestellt > 0) " · davon ${g.meineBereitgestellt} von dir" else "",
                        )
                    }
                    if (g.bedarfe.isEmpty() && g.angefordert.isNotEmpty()) {
                        Hinweis("Wird angefordert — die Zahl sagt, wie viele Fahrzeuge im Raum es mitbringen:")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                            g.angefordert.forEach { a ->
                                Weltmarke("${a.faehigkeit} · ${a.imRaum}", if (a.imRaum == 0) Farben.SignalHell else Farben.TextLeise)
                            }
                        }
                    }

                    val freie = stand.freieFahrzeuge
                    if (!wahlOffen) {
                        Knopf("Fahrzeuge vorschicken", { wahlOffen = true }, art = Knopfart.Haupt, aktiv = freie.isNotEmpty())
                    } else {
                        if (freie.isEmpty()) {
                            Absage("Kein Fahrzeug auf der Wache.")
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (freie.size == 1) "Ein Fahrzeug auf der Wache" else "${freie.size} Fahrzeuge auf der Wache",
                                    style = Schrift.Winzig,
                                    color = Farben.TextSehrLeise,
                                    modifier = Modifier.weight(1f),
                                )
                                AlleWaehlen(freie.map { it.id }, angehakt) { angehakt = it }
                            }
                            freie.forEach { f ->
                                Hakenkarte(
                                    an = f.id in angehakt,
                                    beiWechsel = { angehakt = if (f.id in angehakt) angehakt - f.id else angehakt + f.id },
                                ) {
                                    Text(text = f.funkrufname, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                                    Text(text = f.typ, style = Schrift.Winzig, color = Farben.TextLeise)
                                }
                            }
                        }
                        Absage(fehler)
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf(
                                if (sendet) "Fährt los …" else "Losschicken (${angehakt.size})",
                                {
                                    if (angehakt.isEmpty()) {
                                        fehler = "Wähle mindestens ein Fahrzeug."
                                    } else if (!sendet) {
                                        sendet = true
                                        val ids = angehakt.toList()
                                        bereich.launch {
                                            fehler = welt.bereitstellen(ids)
                                            sendet = false
                                            if (fehler == null) {
                                                angehakt = emptySet()
                                                wahlOffen = false
                                            }
                                        }
                                    }
                                },
                                art = Knopfart.Haupt,
                                aktiv = !sendet && freie.isNotEmpty(),
                            )
                            Knopf("Abbrechen", { wahlOffen = false }, art = Knopfart.Leise)
                        }
                    }
                }
            }

            // ------------------------------------------------------- Hinweise
            if (g.beteiligt) {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Du bist dabei — Bonus bis zu", style = Schrift.Klein, color = Farben.Text)
                    Credits(g.abschlussbonus)
                    Text(text = ", voll bei gedecktem Bedarf.", style = Schrift.Klein, color = Farben.Text)
                }
            } else {
                Text(
                    text = if (g.zustand == "Laeuft") "Die Lagen stehen auf der Karte und in der Lagenliste."
                    else "Allein nicht zu schaffen — schick Fahrzeuge vor, sie warten dort bis zum Beginn.",
                    style = Schrift.Klein,
                    color = Farben.Text,
                )
            }
            if (g.anfahrtOffen) {
                Text(
                    text = "Fahrzeuge im Bereitstellungsraum arbeiten die Abschnitte von selbst ab — und kehren danach dorthin zurück.",
                    style = Schrift.Klein,
                    color = Farben.TextLeise,
                )
            }
            Knopf("Auf der Karte zeigen", { griffe.hinschauen(g.lat, g.lon); griffe.karteFreigeben() }, art = Knopfart.Leise, kompakt = true)

            g.vorwoche?.let { v ->
                Text(
                    text = "Letzte Woche: ${v.name} — ${if (v.geschafft) "geschafft" else "verfehlt"} " +
                        "(${v.gestellt}/${v.bedarf} Kräfte, ${zahl(v.bonus)} Welt-Credits je Beteiligtem).",
                    style = Schrift.Klein,
                    color = if (v.geschafft) Farben.GruenHell else Farben.TextLeise,
                )
            }
        }

        // ------------------------------------------------------- Wochenziele
        ziele?.let { z ->
            Gruppenkopf("Diese Woche")
            z.ziele.forEach { ziel ->
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (ziel.erfuellt) Weltzeichen.Haken else Weltzeichen.Grosslage,
                        contentDescription = if (ziel.erfuellt) "erfüllt" else null,
                        tint = if (ziel.erfuellt) Farben.GruenHell else Farben.TextSehrLeise,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
                        Text(text = ziel.titel, style = Schrift.Klein, color = if (ziel.erfuellt) Farben.TextLeise else Farben.Text)
                        Text(text = "${ziel.stand}/${ziel.zielwert}", style = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), color = Farben.TextSehrLeise)
                    }
                    Credits(ziel.belohnung, stil = Schrift.MonoKlein.copy(fontSize = Schrift.WINZIG), zeichengroesse = 10.dp)
                }
            }
        }
    }
}

@Composable
private fun Zeile(wort: String, wert: String, stark: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
        Text(text = wort, style = Schrift.Klein, color = if (stark) Farben.Text else Farben.TextLeise, modifier = Modifier.weight(1f))
        Text(text = wert, style = Schrift.MonoKlein, color = Farben.Text)
    }
}

@Composable
private fun Hinweis(text: String) {
    Text(text = text, style = Schrift.Winzig, color = Farben.TextSehrLeise)
}
