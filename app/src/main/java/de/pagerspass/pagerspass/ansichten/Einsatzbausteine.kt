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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.ManvPatient
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.netz.Stichwort
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.delay

/**
 * Die Kästen der Fahrzeugseite über Status und Lagemeldung hinaus — übertragen aus
 * `EinsatzKarte.vue` (Wasser, Aufgaben, Suche), `LagemeldungForm.vue`
 * (Nachfordern), `FmsTastatur.vue` (Streife), `FeststellungForm.vue`,
 * `EinsatzleiterTablet.vue` (Flächen und Einsatzfunkgruppen), `PatientBogen.vue`
 * und dem Notweg „Leitstelle verwaist" der `FahrzeugView.vue`.
 *
 * <b>Jeder Kasten erscheint nur, wo er etwas tun kann.</b> Ein Knopf, der
 * verlässlich eine Absage erzeugt, ist ein kaputter Knopf — die Bedingungen des
 * Servers stehen deshalb hier ein zweites Mal, entschieden wird trotzdem dort.
 */

/** Ein ISO-Zeitstempel in Epoch-Millis — `null`, wenn er nicht zu lesen ist. */
internal fun zeitpunktMs(roh: String?): Long? {
    if (roh.isNullOrBlank()) return null
    return runCatching { java.time.Instant.parse(roh).toEpochMilli() }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(roh).toInstant().toEpochMilli() }.getOrNull()
}

/** Eine Uhr, die jede Sekunde tickt — für Restzeiten, die der Server nicht nachschickt. */
@Composable
internal fun sekundentakt(): Long {
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            jetzt = System.currentTimeMillis()
        }
    }
    return jetzt
}

/**
 * Die Hinweise über dem Einsatz: die verwaiste Leitstelle, die laufende
 * Wiederherstellung, der Notarzt, der woanders mitfährt.
 *
 * <b>Die verwaiste Leitstelle ist eine Zeile, kein schwebender Knopf</b> — ein
 * Fehlgriff hieße hier „Leitstelle übernommen". Die zwei Minuten sind der Spiegel
 * der Schonfrist am Server; durchgesetzt wird dort.
 */
@Composable
fun ColumnScope.Fahrzeughinweise(raum: Raumzustand, meins: Rundenfahrzeug, befehle: Raumbefehle) {
    val jetzt = sekundentakt()

    val verwaistSeit = zeitpunktMs(raum.leitstelleVerwaistSeit)
    if (verwaistSeit != null && jetzt - verwaistSeit >= 2 * 60 * 1000) {
        Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
            Text("Die Leitstelle ist verwaist.", style = Schrift.Normal, color = Farben.AmberHell)
            Row { Knopf("Leitstelle übernehmen", befehle::leitstelleUebernehmen, kompakt = true) }
        }
    }

    zeitpunktMs(meins.wiederherstellungBis)?.let { bis ->
        val rest = ((bis - jetzt) / 1000).toInt()
        if (rest > 0) {
            SehrLeise(
                "${meins.ausserDienstGrund ?: "Wiederherstellung"} — noch " +
                    (if (rest < 60) "$rest s" else "${(rest + 59) / 60} min") + ", dann wieder einsatzbereit",
                mono = true,
            )
        }
    }

    meins.notarztBei?.let {
        SehrLeise("Notarzt begleitet $it — bis zur Übergabe ohne Notarzt unterwegs", mono = true)
    }
}

/** Nachfordern — ohne eigenen Text der Satz „Weitere Kräfte erforderlich." */
@Composable
fun ColumnScope.Nachforderung(befehle: Raumbefehle) {
    var offen by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    Row { Knopf(if (offen) "Nachfordern — abbrechen" else "Nachfordern", { offen = !offen }, kompakt = true) }
    if (offen) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
        ) {
            Feld(
                wert = text,
                beiAenderung = { text = it.take(200) },
                platzhalter = "Was wird benötigt? (z. B. zweiter RTW)",
                weiterTaste = ImeAction.Send,
                modifier = Modifier.weight(1f),
            )
            Knopf("Anfordern", {
                befehle.nachfordern(text)
                text = ""
                offen = false
            }, art = Knopfart.Alarm, kompakt = true)
        }
    }
}

/**
 * Der Tank — nur, wenn die Runde Löschwasser spielt und das Fahrzeug einen hat.
 *
 * Holen kann nur, wer an der Einsatzstelle steht und noch nicht unterwegs ist. Steht
 * die Wasserversorgung, wird der Knopf nicht angeboten: Der Tank füllt sich dann
 * über die Leitung, und die Fahrt wäre eine Fahrt für nichts.
 */
@Composable
fun ColumnScope.Wasserkasten(raum: Raumzustand, einsatz: Einsatz?, meins: Rundenfahrzeug, befehle: Raumbefehle) {
    if (!raum.settings.loeschwasser || meins.tankLiter <= 0.0) return
    val anteil = (meins.wasserLiter / meins.tankLiter).toFloat().coerceIn(0f, 1f)
    val unterwegs = meins.entnahmestelleId != null && !meins.entnahmestelleErreicht
    val kannHolen = meins.status == 4 &&
        meins.entnahmestelleId == null &&
        anteil < 0.99f &&
        einsatz?.wasserversorgungSteht != true &&
        raum.entnahmestellen.isNotEmpty()

    Ueberschrift("Wasser")
    Fortschritt(anteil, text = "${meins.wasserLiter.toInt()} / ${meins.tankLiter.toInt()} l")
    when {
        einsatz?.wasserversorgungSteht == true -> SehrLeise("Die Wasserversorgung steht — der Tank füllt sich über die Leitung.")
        meins.entnahmestelleErreicht -> SehrLeise("Füllt an der Entnahmestelle auf …")
        unterwegs -> SehrLeise("Unterwegs zur Entnahmestelle.")
    }
    if (kannHolen) {
        Row { Knopf("Wasser holen", befehle::wasserAufnehmen, kompakt = true) }
    }
}

/**
 * Die Aufgaben der Einsatzstelle, aus der Sicht dieses Fahrzeugs. Übernehmen darf
 * man erst an der Einsatzstelle — wer auf der Anfahrt sitzt, verteilt keine Trupps.
 */
@Composable
fun ColumnScope.Arbeitskasten(einsatz: Einsatz, meins: Rundenfahrzeug, befehle: Raumbefehle) {
    val zaehlende = einsatz.aufgaben.filter { !it.entfallen }
    if (zaehlende.isEmpty()) return
    val vorOrt = meins.status == 4 && meins.einsatzstelleErreicht

    Ueberschrift("Arbeiten ${zaehlende.count { it.fertig }} von ${zaehlende.size}")
    if (einsatz.arbeitFertigUm != null) SehrLeise("Alle Arbeiten abgeschlossen.")
    zaehlende.forEach { a ->
        val meine = meins.aufgabe == a.nummer
        val waehlbar = vorOrt && !a.fertig && a.begonnen && !meine &&
            (!a.zwingend || a.faehigkeit == null || a.faehigkeit in meins.faehigkeiten)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    a.name,
                    style = Schrift.Klein,
                    color = if (!a.begonnen && !a.fertig) Farben.TextSehrLeise else Farben.Text,
                )
                SehrLeise(
                    when {
                        a.fertig -> "fertig"
                        !a.begonnen -> "wartet" + (a.wartetAuf?.let { " auf $it" } ?: "")
                        else -> "${(a.fortschritt * 100).toInt()} %"
                    } + if (meine) " · deine Aufgabe" else "",
                )
            }
            if (meine) {
                Knopf("Abgeben", { befehle.aufgabeUebernehmen(null) }, art = Knopfart.Leise, kompakt = true)
            } else if (waehlbar) {
                Knopf("Übernehmen", { befehle.aufgabeUebernehmen(a.nummer) }, kompakt = true)
            }
        }
    }
}

/** Der eigene Suchabschnitt — nur an einer Suchlage, solange die Person vermisst ist. */
@Composable
fun ColumnScope.Suchkasten(einsatz: Einsatz, meins: Rundenfahrzeug) {
    if ((einsatz.suchradiusMeter ?: 0.0) <= 0.0) return
    Ueberschrift("Suche · ${(einsatz.suchfortschritt * 100).toInt()} %")
    if (einsatz.personGefundenUm != null) {
        SehrLeise("Die Person ist gefunden.")
        return
    }
    val abschnitt = meins.suchabschnitt?.let { einsatz.suchabschnitte.getOrNull(it) }
    if (abschnitt == null) {
        SehrLeise("Noch kein Abschnitt zugeteilt — an der Einsatzstelle nimmt der Trupp den am wenigsten abgesuchten.")
    } else {
        Fortschritt(abschnitt.fortschritt.toFloat(), text = abschnitt.name)
    }
}

/**
 * Die Streife — losfahren, zurückholen, und die eigene Maske für das, was man
 * dabei sieht. Nur für Fahrzeuge, die der Server als streifenfähig meldet.
 */
@Composable
fun ColumnScope.Streifenkasten(meins: Rundenfahrzeug, katalog: Katalog?, befehle: Raumbefehle) {
    if (!meins.streifenfaehig) return
    val moeglich = meins.aufStreife || (meins.einsatzId == null && meins.status in 1..2)

    Ueberschrift("Streife")
    Schalterzeile(
        titel = if (meins.aufStreife) "Auf Streife" else "Streife fahren",
        unterzeile = meins.streifenziel?.takeIf { meins.aufStreife }?.let { "Unterwegs: $it" }
            ?: "Nur ohne Einsatz und einsatzbereit.",
        an = meins.aufStreife,
        beiWechsel = { befehle.streifeSchicken(meins.id, it) },
        aktiv = moeglich,
    )
    if (meins.aufStreife) Feststellungsmaske(meins, katalog, befehle)
}

/**
 * Die eigene Feststellung: Was die Streife sieht, wird ein Einsatz. Ein Feld, ein
 * paar Vorschläge, ein Knopf — über die Kräfte entscheidet die Leitstelle.
 */
@Composable
private fun ColumnScope.Feststellungsmaske(meins: Rundenfahrzeug, katalog: Katalog?, befehle: Raumbefehle) {
    var stichwort by remember { mutableStateOf<Stichwort?>(null) }
    var wahl by remember { mutableStateOf(false) }
    var meldebild by remember { mutableStateOf("") }
    var ort by remember { mutableStateOf("") }
    var gesendet by remember { mutableStateOf(false) }
    // Nur Stichworte der eigenen Organisation — was sie sonst sieht, meldet sie über Funk.
    val stichworte = katalog?.stichworte.orEmpty()
        .filter { it.organisation == meins.organisation }
        .sortedBy { it.stichwort }
    val ortVorschlag = meins.streifenziel.orEmpty()

    Kasten(abstandInnen = Abstand.Klein) {
        Text("Eigene Feststellung", style = Schrift.Normal, color = Farben.Text)
        SehrLeise("Was ihr hier seht, wird ein Einsatz. Einer je Streifenabschnitt.")
        Wahlfeld(
            etikett = "Stichwort",
            wert = stichwort?.let { "${it.stichwort} — ${it.stichwortText}" },
            beiDruck = { wahl = true },
        )
        stichwort?.meldebilder?.takeIf { it.isNotEmpty() }?.let { bilder ->
            Pillenreihe {
                bilder.forEach { b -> Pille(b, an = meldebild == b, beiDruck = { meldebild = b }) }
            }
        }
        Feld(
            wert = ort,
            beiAenderung = { ort = it.take(120) },
            etikett = "Wo",
            platzhalter = ortVorschlag.ifBlank { "Straße, Objekt, Kreuzung …" },
        )
        Row {
            Knopf(
                "Streifeneinsatz anlegen",
                {
                    val s = stichwort ?: return@Knopf
                    befehle.streifeneinsatzAnlegen(
                        stichwort = s.stichwort,
                        stichwortText = s.stichwortText,
                        meldebild = meldebild.ifBlank { s.meldebilder.firstOrNull() ?: s.stichwortText },
                        adresse = ort.trim().ifBlank { ortVorschlag },
                        prioritaet = s.prioritaet,
                        empfohleneFahrzeuge = s.empfohleneFahrzeuge,
                        empfohleneFaehigkeiten = s.empfohleneFaehigkeiten,
                    )
                    stichwort = null
                    meldebild = ""
                    ort = ""
                    gesendet = true
                },
                art = Knopfart.Haupt,
                aktiv = stichwort != null && (ort.isNotBlank() || ortVorschlag.isNotBlank()),
                kompakt = true,
            )
        }
        if (gesendet) SehrLeise("Angelegt — er steht gleich im Tableau.")
    }

    if (wahl) {
        Wahlblende(
            titel = "Stichwort",
            gruppen = listOf(null to stichworte),
            aufschrift = { "${it.stichwort} — ${it.stichwortText}" },
            gewaehlt = stichwort,
            beiWahl = {
                stichwort = it
                meldebild = it.meldebilder.firstOrNull().orEmpty()
                wahl = false
            },
            beiSchliessen = { wahl = false },
            suchbar = stichworte.size > 8,
        )
    }
}

/**
 * Die Flächen der Einsatzstelle und ihre Funkgruppen — der Arbeitsplatz dessen, der
 * die Lage führt (aus dem Tablet der Einsatzleitung).
 */
@Composable
fun ColumnScope.Flaechenkasten(
    einsatz: Einsatz,
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    neben: Raumneben,
    befehle: Raumbefehle,
) {
    val fuehrt = einsatz.einsatzleitung == meins.funkrufname || einsatz.einsatzleitungRd == meins.funkrufname
    if (!fuehrt) return

    val kraefte = einsatz.alarmierteFahrzeuge.mapNotNull { id -> raum.vehicles.firstOrNull { it.id == id } }
        .filter { it.einsatzId == einsatz.id }
    val amOrt = kraefte.filter { it.status == 4 }
    var neueGruppe by remember(einsatz.id) { mutableStateOf("") }
    var aufschaltenFuer by remember(einsatz.id) { mutableStateOf<String?>(null) }

    Kasten(abstandInnen = Abstand.Klein) {
        // ------------------------------------------------- Bereitstellungsraum
        Ueberschrift("Bereitstellungsraum")
        if (!einsatz.bereitstellungsraum) {
            Row {
                Knopf("Bereitstellungsraum einrichten", { befehle.bereitstellungsraumFestlegen(einsatz.id) }, kompakt = true)
            }
        } else {
            SehrLeise("Eingerichtet · ${einsatz.inBereitstellung.size} in Bereitstellung")
            if (amOrt.isEmpty()) SehrLeise("Noch keine Kraft an der Einsatzstelle.")
            // Zwei Pillen statt eines Umschalters: Was gilt, ist zu sehen, ohne es
            // aus einer Aufschrift zu erschließen.
            amOrt.forEach { f ->
                val haelt = f.funkrufname in einsatz.inBereitstellung
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Pille("Im BR", an = haelt, beiDruck = { befehle.bereitstellungSetzen(einsatz.id, f.id, true) })
                    Pille("An E-Stelle", an = !haelt, beiDruck = { befehle.bereitstellungSetzen(einsatz.id, f.id, false) })
                }
            }
        }

        // ------------------------------------------------------- Landeplatz
        Ueberschrift("Hubschrauberlandeplatz")
        val lp = einsatz.landeplatz
        if (lp == null) {
            Row { Knopf("Landeplatz festlegen", { befehle.landeplatzFestlegen(einsatz.id) }, kompakt = true) }
        } else {
            SehrLeise(
                "Fläche: " + if (lp.hergerichtet) "hergerichtet" else "im Aufbau · ${(lp.fortschritt * 100).toInt()} %",
            )
            SehrLeise(
                "Licht: " + when {
                    lp.ausgeleuchtet -> "ausgeleuchtet — Nachtlandung möglich"
                    lp.lichtfortschritt > 0 -> "im Aufbau · ${(lp.lichtfortschritt * 100).toInt()} %"
                    else -> "nicht angeordnet"
                },
            )
            if (!lp.ausgeleuchtet && lp.lichtfortschritt == 0.0) {
                Row { Knopf("Ausleuchten anordnen", { befehle.landeplatzAusleuchten(einsatz.id) }, kompakt = true) }
            }
        }

        // ------------------------------------------------ Einsatzfunkgruppen
        val gruppen = raum.settings.funkgruppen.filter { it.einsatzId == einsatz.id }
        Ueberschrift("Einsatzfunkgruppen (${gruppen.size}/3)")
        if (gruppen.isEmpty()) SehrLeise("Noch keine DMO-Gruppe geöffnet. Bis zu drei laufen parallel.")
        gruppen.forEach { g ->
            val aufgeschaltet = kraefte.filter { it.funkgruppe == g.id }.joinToString(", ") { it.funkrufname }
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                Text("${g.nummer} ${g.name}".trim(), style = Schrift.Normal, color = Farben.Text)
                SehrLeise("Aufgeschaltet: ${aufgeschaltet.ifBlank { "noch niemand" }}")
                Pillenreihe {
                    Pille(
                        if (neben.sendetAuf == null && meins.funkgruppe == g.id) "Sendekanal" else "Hier senden",
                        an = meins.funkgruppe == g.id,
                        beiDruck = { befehle.funkgruppeZuweisen(meins.id, g.id) },
                    )
                    Pille("Kraft aufschalten", an = false, beiDruck = { aufschaltenFuer = g.id })
                    Pille("Schließen", an = false, beiDruck = { befehle.einsatzfunkgruppeSchliessen(g.id) })
                }
            }
        }
        if (gruppen.size < 3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.Bottom,
            ) {
                Feld(
                    wert = neueGruppe,
                    beiAenderung = { neueGruppe = it.take(40) },
                    platzhalter = "Einsatzstelle ${gruppen.size + 1}",
                    modifier = Modifier.weight(1f),
                )
                Knopf("DMO öffnen", {
                    befehle.einsatzfunkgruppeOeffnen(einsatz.id, neueGruppe)
                    neueGruppe = ""
                }, art = Knopfart.Alarm, kompakt = true)
            }
        }
    }

    aufschaltenFuer?.let { gruppeId ->
        Wahlblende(
            titel = "Kraft aufschalten",
            gruppen = listOf(null to amOrt.filter { it.funkgruppe != gruppeId }),
            aufschrift = { it.funkrufname },
            unterschrift = { it.typ },
            beiWahl = {
                befehle.funkgruppeZuweisen(it.id, gruppeId)
                aufschaltenFuer = null
            },
            beiSchliessen = { aufschaltenFuer = null },
        )
    }
}

/** Die Messwerte in der Reihenfolge der Erstuntersuchung — mit ihrer Dauer. */
private val MESSBAR = listOf(
    Triple("Bewusstsein", "Bewusstsein ansprechen", "Bewusstsein"),
    Triple("Atemfrequenz", "Atemfrequenz", "Atemfrequenz"),
    Triple("Puls", "Puls", "Puls"),
    Triple("Sauerstoffsaettigung", "Sättigung", "Sättigung"),
    Triple("Blutdruck", "Blutdruck", "Blutdruck"),
    Triple("Blutzucker", "Blutzucker", "Blutzucker"),
    Triple("Temperatur", "Temperatur", "Temperatur"),
)

private val SCHEMATA = listOf(
    Triple("XAbcde", "xABCDE", "Erstuntersuchung — was sofort tötet"),
    Triple("Sampler", "SAMPLER", "Anamnese — was vorher war"),
    Triple("Opqrst", "OPQRST", "Schmerz beschreiben"),
)

/**
 * Die Patienten dieser Lage, an denen gearbeitet werden kann — nur, wenn die
 * Simulation läuft und die eigene Besatzung am Patienten ist (Status 4) oder ihn an
 * Bord hat (Status 7). Der eigene zuerst: An einer Lage mit zwanzig Bögen ist das die
 * erste Frage der Besatzung.
 */
@Composable
fun ColumnScope.Patientenkasten(einsatz: Einsatz, meins: Rundenfahrzeug, befehle: Raumbefehle) {
    if (meins.status != 4 && meins.status != 7) return
    val patienten = einsatz.manvPatienten
        .filter { it.simuliert }
        .sortedWith(compareBy({ it.transportVehicleId != meins.id }, { it.id }))
    if (patienten.isEmpty()) return

    Ueberschrift("Patienten (${patienten.size})")
    val jetzt = sekundentakt()
    patienten.forEach { p ->
        Patientenbogen(einsatz.id, p, meins, zugewiesen = p.transportVehicleId == meins.id, jetzt = jetzt, befehle = befehle)
    }
}

/**
 * Der Bogen eines Patienten: messen, untersuchen, behandeln, einordnen.
 *
 * <b>Die Uhr am Messwert ist kein Zierrat.</b> Eine Blutdruckmessung dauert zwanzig
 * Sekunden — ohne sichtbaren Ablauf sieht das aus wie ein Knopf, der nichts tut.
 */
@Composable
private fun Patientenbogen(
    einsatzId: String,
    p: ManvPatient,
    meins: Rundenfahrzeug,
    zugewiesen: Boolean,
    jetzt: Long,
    befehle: Raumbefehle,
) {
    var verdacht by remember(p.id) { mutableStateOf("") }
    var diagnose by remember(p.id) { mutableStateOf("") }
    val werte = p.werte.orEmpty()
    val auffaellig = p.auffaelligeWerte.orEmpty().toSet()
    val rest = zeitpunktMs(p.messungFertigUm)?.let { ((it - jetzt) / 1000).coerceAtLeast(0) }
    val genugUntersucht = werte.size >= 3 && p.befunde.orEmpty().isNotEmpty()
    val hatNotarzt = meins.faehigkeiten.any { it.equals("Notarzt", ignoreCase = true) }
    val wertname: (String) -> String = { id -> MESSBAR.firstOrNull { it.first == id }?.third ?: id }

    Kasten(marke = zugewiesen, abstandInnen = Abstand.Klein) {
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Text("Patient ${p.id}", style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f))
            p.kategorie?.let { Marke(it) }
            if (zugewiesen) Marke("dir zugewiesen", farbe = Farben.AmberHell)
        }

        // Erst, was nicht stimmt; darin wie darunter die Ordnung der Erstuntersuchung.
        val reihenfolge = MESSBAR.map { it.first }.filter { it in werte } + werte.keys.filter { k -> MESSBAR.none { it.first == k } }
        val geordnet = reihenfolge.filter { it in auffaellig } + reihenfolge.filter { it !in auffaellig }
        if (geordnet.isEmpty()) SehrLeise("Noch nichts gemessen.")
        geordnet.forEach { id ->
            Text(
                "${wertname(id)}: ${werte[id]}",
                style = Schrift.MonoKlein,
                color = if (id in auffaellig) Farben.SignalHell else Farben.Text,
            )
        }
        if (rest != null) {
            SehrLeise(
                if (rest > 0) "${wertname(p.misstGerade.orEmpty())} läuft — noch $rest s"
                else "${wertname(p.misstGerade.orEmpty())} — wird abgelesen …",
                mono = true,
            )
        }

        Pillenreihe {
            MESSBAR.forEach { (id, name, _) ->
                Pille(name, an = id in werte, aktiv = rest == null && id !in werte, beiDruck = {
                    befehle.patientMessen(einsatzId, p.id, id)
                })
            }
        }
        Pillenreihe {
            SCHEMATA.forEach { (id, name, _) ->
                val erhoben = p.befunde.orEmpty().any { it.schema == id }
                Pille(name, an = erhoben, aktiv = !erhoben, beiDruck = { befehle.patientSchema(einsatzId, p.id, id) })
            }
        }
        p.befunde.orEmpty().forEach { b ->
            SehrLeise(b.name)
            b.punkte.forEach { pt ->
                Text("${pt.schluessel} ${pt.frage}: ${pt.befund.ifBlank { "unauffällig" }}", style = Schrift.Klein, color = Farben.Text)
            }
        }

        // Maßnahmen — was nicht geht, steht gesperrt da statt als toter Knopf.
        if (p.massnahmen.orEmpty().isNotEmpty()) {
            Pillenreihe {
                p.massnahmen.orEmpty().forEach { m ->
                    Pille(
                        m.name + if (m.brauchtArzt && !m.moeglich) " (Notarzt)" else "",
                        an = m.laeuft,
                        aktiv = m.moeglich && !m.laeuft,
                        beiDruck = { befehle.patientMassnahme(einsatzId, p.id, m.id) },
                    )
                }
            }
        }

        // Einordnen: erst der Verdacht der Besatzung, dann die Diagnose des Arztes.
        if (p.verdachtsdiagnose != null) {
            SehrLeise("Verdacht: ${p.verdachtsdiagnose}")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = verdacht,
                    beiAenderung = { verdacht = it.take(120) },
                    platzhalter = if (genugUntersucht) "Verdachtsdiagnose …" else "Erst drei Werte und ein Schema",
                    aktiv = genugUntersucht,
                    modifier = Modifier.weight(1f),
                )
                Knopf("Festhalten", {
                    befehle.patientVerdacht(einsatzId, p.id, verdacht)
                    verdacht = ""
                }, aktiv = genugUntersucht && verdacht.isNotBlank(), kompakt = true)
            }
        }
        if (p.diagnose != null) {
            SehrLeise("Diagnose: ${p.diagnose}")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.Bottom) {
                Feld(
                    wert = diagnose,
                    beiAenderung = { diagnose = it.take(120) },
                    platzhalter = if (hatNotarzt) "Diagnose (leer: Klartext des Bildes) …" else "Die Diagnose stellt der Notarzt",
                    aktiv = hatNotarzt,
                    modifier = Modifier.weight(1f),
                )
                Knopf("Diagnose stellen", {
                    befehle.patientDiagnose(einsatzId, p.id, diagnose)
                    diagnose = ""
                }, aktiv = hatNotarzt, kompakt = true)
            }
        }
    }
}

/**
 * Einzelruf und Notruftaste am Handfunkgerät.
 *
 * <b>Die Notruftaste fragt nach.</b> Am Vorbild sitzt sie unter einer Klappe — sie
 * darf nicht dem Daumen passieren, der eigentlich etwas anderes wollte.
 */
@Composable
fun ColumnScope.Geraetegriffe(
    raum: Raumzustand,
    meins: Rundenfahrzeug,
    eigeneKennung: String,
    befehle: Raumbefehle,
) {
    var rufWahl by remember { mutableStateOf(false) }
    var notrufFrage by remember { mutableStateOf(false) }
    val ich = raum.players.firstOrNull { it.id == eigeneKennung }
    val amTelefon = raum.einzelrufe.any { it.vonPlayerId == eigeneKennung || it.angenommenVonPlayerId == eigeneKennung }
    // Anrufbar ist, wo ein Mensch sitzt — ein Bot nimmt nicht ab.
    val ziele = raum.vehicles.filter { f ->
        f.id != meins.id && raum.players.any { it.id == f.playerId && !it.istBot }
    }

    Ueberschrift("Handfunkgerät")
    Pillenreihe {
        Pille("Einzelruf", an = false, aktiv = !amTelefon, beiDruck = { rufWahl = true })
        Pille("Notruf", an = false, farbe = Farben.SignalHell, beiDruck = { notrufFrage = true })
    }
    Schalterzeile(
        titel = "Einzelrufe annehmen",
        unterzeile = "Aus heißt: Besatzungen kommen nicht durch. Die Leitstelle immer.",
        an = ich?.einzelrufZulassen != false,
        beiWechsel = { befehle.einzelrufZulassen(it) },
    )
    Schalterzeile(
        titel = "Getippten Funk vorlesen",
        unterzeile = "Wer auf der Gruppe mithört, hört deine getippten Sprüche als Stimme.",
        an = ich?.funkVorlesen == true,
        beiWechsel = { befehle.funkVorlesen(it) },
    )

    // Die Rufgruppe — das Gerät versucht es, der Server entscheidet („Auf DMO kann
    // erst an der Einsatzstelle geschaltet werden").
    val gruppen = raum.settings.funkgruppen.filter { it.einsatzId == null || it.einsatzId == meins.einsatzId }
    if (gruppen.isNotEmpty()) {
        Ueberschrift("Rufgruppe")
        Pillenreihe {
            Pille("Stammgruppe", an = !meins.funkgruppeAufgeschaltet, beiDruck = { befehle.funkgruppeZuweisen(meins.id, null) })
            gruppen.forEach { g ->
                Pille(
                    g.marke.ifBlank { g.name },
                    an = meins.funkgruppeAufgeschaltet && meins.funkgruppe == g.id,
                    beiDruck = { befehle.funkgruppeZuweisen(meins.id, g.id) },
                )
            }
        }
    }

    if (rufWahl) {
        Wahlblende(
            titel = "Einzelruf an",
            gruppen = listOf(null to (listOf<Rundenfahrzeug?>(null) + ziele)),
            aufschrift = { it?.funkrufname ?: "Leitstelle" },
            unterschrift = { it?.typ },
            beiWahl = {
                befehle.einzelrufStarten(it?.id)
                rufWahl = false
            },
            beiSchliessen = { rufWahl = false },
        )
    }

    if (notrufFrage) {
        Blende(
            titel = "Notruf auslösen?",
            beiSchliessen = { notrufFrage = false },
            fuss = {
                Knopf("Abbrechen", { notrufFrage = false }, art = Knopfart.Leise)
                Knopf("Notruf", {
                    befehle.notruf()
                    notrufFrage = false
                }, art = Knopfart.Alarm)
            },
        ) {
            SehrLeise("Der Notruf geht mit Vorrang an die Leitstelle und alle auf der Gruppe.")
        }
    }
}
