package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.mobil.Raumneben
import de.pagerspass.pagerspass.netz.Anruf
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Klinik
import de.pagerspass.pagerspass.netz.Ortung
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Werkzeuge der Leitstelle über Einsatzliste und Alarmdialog hinaus — übertragen
 * aus `EinsatzDetail.vue`, `BesatzungDialog.vue`, `WarnungDialog.vue`,
 * `Anrufjournal.vue`, dem Feststellungsband der `LeitstelleView.vue` und den
 * Griffen an den Kacheln des `FahrzeugTableau.vue`.
 */

/**
 * Die Eigenfeststellungen der Streifen — ein Band, kein Dialog: Eine Streife hat
 * etwas gesehen und fährt weiter. Wer nichts tut, dem verfällt die Meldung ohne
 * Punktabzug.
 */
@Composable
fun ColumnScope.Feststellungsband(raum: Raumzustand, befehle: Raumbefehle) {
    raum.feststellungen.forEach { f ->
        Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
            Text("⇢ Feststellung", style = Schrift.Klein, color = Farben.BlauHell)
            Text(
                "${f.funkrufname} · ${f.stichwort} ${f.stichwortText} — ${f.meldebild} (${f.ort})",
                style = Schrift.Klein,
                color = Farben.Text,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf("Streifeneinsatz anlegen", { befehle.feststellungUebernehmen(f.id) }, art = Knopfart.Haupt, kompakt = true)
                Knopf("Kein Einsatz", { befehle.feststellungVerwerfen(f.id) }, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
}

/** Fährt dieses Fahrzeug Patienten in eine Klinik? Dieselbe Frage wie `machtPatiententransport`. */
private fun machtPatiententransport(f: Rundenfahrzeug): Boolean =
    f.organisation == "Rettungsdienst" &&
        "Transport" in f.faehigkeiten &&
        f.faehigkeiten.none { it == "Betreuung" || it == "Ortung" }

/**
 * Der Einsatzbogen — alles, was die Leitstelle an einer Lage tun kann, die schon
 * läuft: nachalarmieren, zurückrufen, Transportziele, Suchabschnitte, Aufgaben.
 *
 * <b>Zurückrufen ist ein eigener Modus.</b> Erst der Modus, dann die Auswahl, dann
 * der Rückruf — ein Druck, der still ein Fahrzeug von der Lage abzieht, wäre genau
 * der Fehlgriff, gegen den dieser Weg gebaut ist.
 */
@Composable
fun Einsatzblende(
    einsatz: Einsatz,
    raum: Raumzustand,
    befehle: Raumbefehle,
    beiAlarmieren: () -> Unit,
    beiAbraeumen: () -> Unit,
    beiZu: () -> Unit,
) {
    var rueckruf by remember(einsatz.id) { mutableStateOf(false) }
    var zurueck by remember(einsatz.id) { mutableStateOf<Set<String>>(emptySet()) }
    var klinikFuer by remember(einsatz.id) { mutableStateOf<Rundenfahrzeug?>(null) }
    var abschnittFuer by remember(einsatz.id) { mutableStateOf<Rundenfahrzeug?>(null) }
    var aufgabeFuer by remember(einsatz.id) { mutableStateOf<Rundenfahrzeug?>(null) }

    val fahrzeuge = einsatz.alarmierteFahrzeuge.mapNotNull { id -> raum.vehicles.firstOrNull { it.id == id } }
    val abziehbar: (Rundenfahrzeug) -> Boolean = { f ->
        f.einsatzId == einsatz.id || (f.einsatzId == null && f.status in listOf(1, 2, 6))
    }
    val transporte = fahrzeuge.filter { it.einsatzId == einsatz.id && it.status == 7 && machtPatiententransport(it) }
    val vorOrt = fahrzeuge.filter { it.einsatzId == einsatz.id && it.status == 4 }

    Blende(
        titel = "${einsatz.einsatznummer} · ${einsatz.stichwort}",
        beiSchliessen = beiZu,
        fuss = {
            if (einsatz.abgeschlossen) {
                Knopf("Abräumen", beiAbraeumen, art = Knopfart.Leise)
            } else if (rueckruf) {
                Knopf("Abbrechen", {
                    rueckruf = false
                    zurueck = emptySet()
                }, art = Knopfart.Leise)
                Knopf(
                    "Zurückrufen (${zurueck.size})",
                    {
                        befehle.fahrzeugeZurueckrufen(einsatz.id, zurueck.toList())
                        rueckruf = false
                        zurueck = emptySet()
                    },
                    art = Knopfart.Gefahr,
                    aktiv = zurueck.isNotEmpty(),
                )
            } else {
                Knopf(if (fahrzeuge.isEmpty()) "Alarmieren" else "Nachalarmieren", beiAlarmieren, art = Knopfart.Alarm)
            }
        },
    ) {
        Text(einsatz.stichwortText, style = Schrift.Normal, color = Farben.Text)
        Text(
            listOfNotNull(einsatz.adresse.ifBlank { null }, einsatz.ortsteil).joinToString(" · "),
            style = Schrift.MonoKlein,
            color = Farben.AmberHell,
        )
        if (einsatz.meldebild.isNotBlank()) SehrLeise(einsatz.meldebild)
        einsatz.meldender?.let { SehrLeise("gemeldet von $it") }
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Marke(einsatzZustand(einsatz.state), farbe = einsatzFarbe(einsatz.state))
            Marke("Priorität ${einsatz.prioritaet}")
            einsatz.einsatzleitung?.let { Marke("EL $it", farbe = Farben.BlauHell) }
        }
        einsatz.wasserlageText?.let { SehrLeise("Wasser: $it") }

        // ------------------------------------------------------- Die Kräfte
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ueberschrift("Kräfte (${fahrzeuge.size}/${einsatz.empfohleneFahrzeuge})", Modifier.weight(1f))
            if (!einsatz.abgeschlossen && fahrzeuge.any(abziehbar) && !rueckruf) {
                Knopf("Zurückrufen", { rueckruf = true }, art = Knopfart.Leise, kompakt = true)
            }
        }
        if (fahrzeuge.isEmpty()) SehrLeise("Noch niemand alarmiert.")
        fahrzeuge.forEach { f ->
            if (rueckruf && abziehbar(f)) {
                Hakenzeile(
                    text = "${f.funkrufname} · ${f.statusText}",
                    an = f.id in zurueck,
                    beiWechsel = { an -> zurueck = if (an) zurueck + f.id else zurueck - f.id },
                )
            } else {
                Dienstfahrzeugzeile(f)
            }
        }

        // ------------------------------------------------- Transportziele
        if (transporte.isNotEmpty()) {
            Ueberschrift("Transportziele")
            transporte.forEach { f ->
                val ziel = raum.kliniken.firstOrNull { it.id == f.zielklinikId }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                        SehrLeise(ziel?.name ?: "ohne Ziel — wartet")
                    }
                    Knopf(if (ziel == null) "Ziel wählen" else "Umleiten", { klinikFuer = f }, kompakt = true)
                }
            }
        }

        // ------------------------------------------------------- Suchlage
        if ((einsatz.suchradiusMeter ?: 0.0) > 0.0 && einsatz.suchabschnitte.isNotEmpty()) {
            Ueberschrift("Suche · ${(einsatz.suchfortschritt * 100).toInt()} %")
            if (einsatz.personGefundenUm != null) SehrLeise("Die Person ist gefunden.")
            vorOrt.forEach { f ->
                val abschnitt = f.suchabschnitt?.let { einsatz.suchabschnitte.getOrNull(it) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                        SehrLeise(abschnitt?.let { "${it.name} · ${(it.fortschritt * 100).toInt()} %" } ?: "ohne Abschnitt")
                    }
                    Knopf("Abschnitt", { abschnittFuer = f }, kompakt = true)
                }
            }
        }

        // ------------------------------------------------------ Aufgaben
        val zaehlende = einsatz.aufgaben.filter { !it.entfallen }
        if (zaehlende.isNotEmpty()) {
            Ueberschrift("Arbeiten ${zaehlende.count { it.fertig }} von ${zaehlende.size}")
            zaehlende.forEach { a ->
                SehrLeise(
                    "${a.name} — " + when {
                        a.fertig -> "fertig"
                        !a.begonnen -> "wartet"
                        else -> "${(a.fortschritt * 100).toInt()} %"
                    },
                )
            }
            vorOrt.forEach { f ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(f.funkrufname, style = Schrift.MonoKlein, color = Farben.Text)
                        SehrLeise(einsatz.aufgaben.firstOrNull { it.nummer == f.aufgabe }?.name ?: "ohne Aufgabe")
                    }
                    Knopf("Aufgabe", { aufgabeFuer = f }, kompakt = true)
                }
            }
        }
    }

    klinikFuer?.let { f ->
        Wahlblende(
            titel = "Transportziel für ${f.funkrufname}",
            gruppen = listOf(null to raum.kliniken.sortedBy { it.name }),
            aufschrift = { it.name },
            unterschrift = { k: Klinik ->
                listOfNotNull(
                    "Landeplatz".takeIf { k.hatLandeplatz },
                    "überregional".takeIf { k.istUeberregional },
                    k.abgemeldet.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "abgemeldet: "),
                ).joinToString(" · ").ifBlank { null }
            },
            gewaehlt = raum.kliniken.firstOrNull { it.id == f.zielklinikId },
            beiWahl = {
                befehle.zielklinikZuweisen(f.id, it.id)
                klinikFuer = null
            },
            beiSchliessen = { klinikFuer = null },
            suchbar = raum.kliniken.size > 8,
        )
    }

    abschnittFuer?.let { f ->
        Wahlblende(
            titel = "Suchabschnitt für ${f.funkrufname}",
            gruppen = listOf(null to einsatz.suchabschnitte.indices.toList()),
            aufschrift = { i -> einsatz.suchabschnitte[i].name.ifBlank { "Abschnitt ${i + 1}" } },
            unterschrift = { i -> "${(einsatz.suchabschnitte[i].fortschritt * 100).toInt()} % abgesucht" },
            gewaehlt = f.suchabschnitt,
            beiWahl = {
                befehle.suchabschnittZuteilen(f.id, it)
                abschnittFuer = null
            },
            beiSchliessen = { abschnittFuer = null },
        )
    }

    aufgabeFuer?.let { f ->
        // Zwingende ohne die Fähigkeit stehen gar nicht erst zur Wahl — der Server
        // wiese sie ab, und eine Auswahl, die beim Loslassen scheitert, ist keine.
        val waehlbar = einsatz.aufgaben.filter { a ->
            !a.fertig && a.begonnen && (!a.zwingend || a.faehigkeit == null || a.faehigkeit in f.faehigkeiten)
        }
        Wahlblende(
            titel = "Aufgabe für ${f.funkrufname}",
            gruppen = listOf(null to waehlbar),
            aufschrift = { "${it.name} · ${(it.fortschritt * 100).toInt()} %" },
            gewaehlt = waehlbar.firstOrNull { it.nummer == f.aufgabe },
            beiWahl = {
                befehle.aufgabeZuteilen(f.id, it.nummer)
                aufgabeFuer = null
            },
            beiSchliessen = { aufgabeFuer = null },
        )
    }
}

/**
 * Die Griffe an einem Fahrzeug des Tableaus: anrufen, Streife, Funkgruppe.
 *
 * <b>Angerufen wird nur, wo ein Mensch sitzt.</b> Ein Bot nimmt nicht ab, und ein
 * Knopf, der verlässlich „meldet sich nicht" erzeugt, ist ein kaputter Knopf.
 */
@Composable
fun Fahrzeuggriffe(
    f: Rundenfahrzeug,
    raum: Raumzustand,
    eigeneKennung: String,
    befehle: Raumbefehle,
    beiSprechwunsch: (String) -> Unit,
    beiZu: () -> Unit,
) {
    val mensch = raum.players.firstOrNull { it.id == f.playerId && !it.istBot }
    val amTelefon = raum.einzelrufe.any {
        it.vonPlayerId == eigeneKennung || it.angenommenVonPlayerId == eigeneKennung
    }
    val streifeGeht = f.streifenfaehig && (f.aufStreife || (f.einsatzId == null && f.status in 1..2))
    // DMO-Gruppen einer Einsatzstelle sieht die Leitstelle nicht — sie ist dort
    // nicht dabei und hat über den Kanal nichts zu sagen.
    val kreisgruppen = raum.settings.funkgruppen.filter { it.einsatzId == null }

    Blende(titel = f.funkrufname, beiSchliessen = beiZu) {
        Dienstfahrzeugzeile(f)
        mensch?.let { SehrLeise("Besatzung: ${it.name}") }
        f.ausserDienstGrund?.let { SehrLeise("Außer Dienst: $it") }

        Pillenreihe {
            if (f.sprechwunschSeit != null) {
                Pille("Sprechwunsch: Kommen", an = true, beiDruck = {
                    beiSprechwunsch(f.id)
                    beiZu()
                })
            }
            if (mensch != null) {
                Pille("Einzelruf", an = false, aktiv = !amTelefon, beiDruck = {
                    befehle.einzelrufStarten(f.id)
                    beiZu()
                })
            }
            if (streifeGeht) {
                Pille(if (f.aufStreife) "Streife beenden" else "Auf Streife schicken", an = f.aufStreife, beiDruck = {
                    befehle.streifeSchicken(f.id, !f.aufStreife)
                })
            }
        }
        f.streifenziel?.takeIf { f.aufStreife }?.let { SehrLeise("Streife: $it") }

        if (kreisgruppen.isNotEmpty()) {
            Ueberschrift("Funkgruppe")
            Pillenreihe {
                Pille("Stammgruppe", an = !f.funkgruppeAufgeschaltet, beiDruck = { befehle.funkgruppeZuweisen(f.id, null) })
                kreisgruppen.forEach { g ->
                    Pille(
                        g.marke.ifBlank { g.name },
                        an = f.funkgruppeAufgeschaltet && f.funkgruppe == g.id,
                        beiDruck = { befehle.funkgruppeZuweisen(f.id, g.id) },
                    )
                }
            }
        }
    }
}

/**
 * Die Mannschaft während der laufenden Runde: übergeben, werfen, Bots einteilen.
 * Dasselbe wie in der Lobby, nur jederzeit erreichbar.
 */
@Composable
fun Besatzungsblende(
    raum: Raumzustand,
    eigeneKennung: String,
    katalog: List<Fahrzeugvorlage>,
    befehle: Raumbefehle,
    beiZu: () -> Unit,
) {
    val menschen = raum.players.filter { !it.istBot && it.id != eigeneKennung }

    Blende(titel = "Mannschaft verwalten", beiSchliessen = beiZu) {
        Ueberschrift("Mitspieler")
        if (menschen.isEmpty()) SehrLeise("Keine weiteren Mitspieler im Raum.")
        menschen.forEach { p ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = Schrift.Normal, color = if (p.verbunden) Farben.Text else Farben.TextSehrLeise, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    SehrLeise(
                        when {
                            p.istLeitstelle -> "Leitstelle"
                            p.vehicleId != null -> raum.vehicles.firstOrNull { it.id == p.vehicleId }?.funkrufname ?: "Fahrzeug"
                            else -> "wählt noch"
                        },
                    )
                }
                // Die Übergabe wird angeboten, nicht zugeschoben — der Mitspieler sieht
                // das Protokoll und sagt ja oder nein.
                if (p.verbunden && raum.laeuft && !p.istLeitstelle) {
                    Knopf("Übergeben", { befehle.leitstelleUebergeben(p.id) }, kompakt = true)
                }
                Knopf("Werfen", { befehle.spielerKicken(p.id) }, art = Knopfart.Gefahr, kompakt = true)
            }
        }
        if (raum.uebergabe != null) SehrLeise("Übergabe angeboten — wartet auf Antwort.")

        Botverwaltung(raum, katalog, befehle)
    }
}

/**
 * Die Bevölkerungswarnung — der MoWaS-Knopf. Ausdrücklich folgenlos: Die Warnung
 * erscheint allen im Raum als Band und steht im Funkprotokoll, sonst nichts.
 */
@Composable
fun Warnungsblende(befehle: Raumbefehle, beiZu: () -> Unit) {
    var eigener by remember { mutableStateOf("") }
    var gesendet by remember { mutableStateOf(false) }
    val vorlagen = listOf(
        "Probewarnung — es besteht keine Gefahr. Dies ist nur ein Test.",
        "Sirenenprobe: Heute heulen die Sirenen. Bitte nicht wundern.",
        "Rauchentwicklung im Stadtgebiet — Fenster und Türen geschlossen halten.",
        "Entwarnung: Die Gefahr besteht nicht mehr. Danke für Ihre Aufmerksamkeit.",
    )
    val warnen: (String) -> Unit = { text ->
        befehle.bevoelkerungWarnen(text)
        eigener = ""
        gesendet = true
    }

    Blende(titel = "Bevölkerung warnen", beiSchliessen = beiZu) {
        SehrLeise("Nur zum Spaß — folgenlos fürs Spiel. Die Warnung geht an alle im Raum.")
        vorlagen.forEach { v ->
            Knopf(v, { warnen(v) }, art = Knopfart.Leise, breit = true)
        }
        Feld(wert = eigener, beiAenderung = { eigener = it.take(300) }, etikett = "Eigener Text", einzeilig = false)
        Row {
            Knopf("Warnen", { warnen(eigener) }, art = Knopfart.Alarm, aktiv = eigener.isNotBlank(), kompakt = true)
        }
        if (gesendet) SehrLeise("Gesendet — das Band zieht gleich bei allen auf.")
    }
}

/** Der Satz zur Ortung — dieselben drei Fassungen wie im Telefonfenster. */
fun ortungstext(o: Ortung?): String? = when {
    o == null -> null
    o.laeuft -> "Ortung läuft — das dauert einen Moment …"
    o.erfolgreich != true -> "Ortung fehlgeschlagen — kein verwertbares Signal."
    else -> "Anrufer geortet: ±${o.radiusMeter?.toInt() ?: "?"} m" +
        (o.ortstext?.let { " · $it" } ?: "") + " — der Kreis liegt auf der Lagekarte."
}

/** Wie ein abgeschlossener Anruf ausging — in Worten. */
private fun ausgangText(ausgang: String): String = when (ausgang) {
    "EinsatzAngelegt" -> "Einsatz angelegt"
    "Verworfen" -> "verworfen"
    "Verpasst" -> "verpasst"
    "Abgewiesen" -> "abgewiesen"
    "Presse" -> "Presseanfrage"
    "Zugeordnet" -> "einem Einsatz zugeordnet"
    else -> ausgang
}

/**
 * Das Anrufjournal — jedes abgeschlossene Gespräch der Schicht, nachlesbar. Wer
 * abgebrochen hat, bevor die Adresse feststand, kann den Anschluss nachträglich
 * orten lassen; das dauert länger und geht öfter schief als am Apparat.
 */
@Composable
fun ColumnScope.Anrufjournal(raum: Raumzustand, befehle: Raumbefehle) {
    var offen by remember { mutableStateOf<String?>(null) }

    Ueberschrift("Anrufjournal (${raum.anrufjournal.size})")
    if (raum.anrufjournal.isEmpty()) {
        SehrLeise("Noch kein abgeschlossenes Gespräch.")
        return
    }
    raum.anrufjournal.asReversed().take(30).forEach { e ->
        Kasten(abstandInnen = Abstand.Klein) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Notruf ${e.nummer}", style = Schrift.Normal, color = Farben.Text)
                    SehrLeise("${uhrzeit(e.eingangUm)} · ${ausgangText(e.ausgang)}", mono = true)
                }
                Knopf(if (offen == e.anrufId) "Zu" else "Verlauf", {
                    offen = if (offen == e.anrufId) null else e.anrufId
                }, art = Knopfart.Leise, kompakt = true)
                if (e.ortbar && e.ortung == null) {
                    Knopf("Orten", { befehle.journalOrten(e.anrufId) }, kompakt = true)
                }
            }
            ortungstext(e.ortung)?.let { SehrLeise(it) }
            if (offen == e.anrufId) {
                e.verlauf.forEach { z ->
                    Text(
                        (if (z.vonLeitstelle) "Du: " else "Anrufer: ") + z.text,
                        style = Schrift.Klein,
                        color = if (z.vonLeitstelle) Farben.AmberHell else Farben.Text,
                    )
                }
            }
        }
    }
}

/**
 * Die Werkzeuge am Apparat, die der Fragenkatalog nicht hat: orten, frei fragen
 * (getippt oder gesprochen) und — nach dem Auflegen — einer laufenden Lage
 * zuordnen statt einen zweiten Einsatz anzulegen.
 */
@Composable
fun ColumnScope.Telefonwerkzeug(
    anruf: Anruf,
    raum: Raumzustand,
    neben: Raumneben,
    befehle: Raumbefehle,
    beiZugeordnet: () -> Unit,
) {
    var frage by remember(anruf.id) { mutableStateOf("") }
    var zuordnen by remember(anruf.id) { mutableStateOf(false) }
    val beendet = anruf.zustand == "Beendet"

    if (!beendet) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
        ) {
            Feld(
                wert = frage,
                beiAenderung = { frage = it.take(200) },
                platzhalter = "Eigene Frage …",
                modifier = Modifier.weight(1f),
            )
            Knopf("Fragen", {
                befehle.anrufFrageText(anruf.id, frage)
                frage = ""
            }, aktiv = frage.isNotBlank(), kompakt = true)
        }

        // Die Rückfrage gesprochen: Der Server versteht sie und stellt sie.
        Sprechtaste(
            sendet = neben.sendetAuf == "notruf",
            wirdVerstanden = neben.notrufVersteht,
            belegtVon = null,
            gesperrtBis = null,
            beiDruck = { befehle.notrufSprechenStarten() },
            beiLoslassen = { befehle.notrufSprechenBeenden(anruf.id) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), verticalAlignment = Alignment.CenterVertically) {
            Knopf("Orten", { befehle.anrufOrten(anruf.id) }, aktiv = anruf.ortung == null, kompakt = true)
            Box(Modifier.weight(1f)) { ortungstext(anruf.ortung)?.let { SehrLeise(it) } }
        }
    } else {
        ortungstext(anruf.ortung)?.let { SehrLeise(it) }
    }

    val offene = raum.incidents.filter { !it.abgeschlossen }
    if (beendet && anruf.vorschlag != null && offene.isNotEmpty()) {
        Row {
            Knopf("Laufender Lage zuordnen", { zuordnen = true }, art = Knopfart.Leise, kompakt = true)
        }
    }

    if (zuordnen) {
        Wahlblende(
            titel = "Welche Lage meldet der Anrufer?",
            gruppen = listOf(null to offene),
            aufschrift = { "${it.einsatznummer} · ${it.stichwort} — ${it.adresse}" },
            unterschrift = { it.stichwortText },
            beiWahl = {
                befehle.anrufZuordnen(anruf.id, it.id)
                zuordnen = false
                beiZugeordnet()
            },
            beiSchliessen = { zuordnen = false },
        )
    }
}
