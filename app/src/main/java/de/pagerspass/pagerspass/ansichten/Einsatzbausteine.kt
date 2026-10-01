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
 * Kästen der Fahrzeugseite — übertragen aus `FeststellungForm.vue` und den
 * Griffen am Handfunkgerät (`FunkgeraetVoll.vue`). Einsatzkarte, FMS, Lagemeldung,
 * PatSim, FwSim und das Einsatz-Tablet stehen in eigenen Dateien
 * (`Fahrzeugkasten.kt`, `Patsim.kt`, `Fwsim.kt`, `Einsatztablet.kt`).
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
 * Die eigene Feststellung: Was die Streife sieht, wird ein Einsatz. Ein Feld, ein
 * paar Vorschläge, ein Knopf — über die Kräfte entscheidet die Leitstelle. Nur auf
 * Streife: Ohne Streifenfahrt weist der Server sie ohnehin ab.
 */
@Composable
fun ColumnScope.Feststellungskasten(meins: Rundenfahrzeug, katalog: Katalog?, befehle: Raumbefehle) {
    if (!meins.aufStreife) return
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
    val vorlesezusammenhang = androidx.compose.ui.platform.LocalContext.current
    Schalterzeile(
        titel = "Getippten Funk vorlesen",
        unterzeile = "Wer auf der Gruppe mithört, hört deine getippten Sprüche als Stimme.",
        an = ich?.funkVorlesen == true,
        // Wie `funkVorlesenSetzen` im Web: Das Gerät merkt es sich für die nächste
        // Schicht, der Beitritt meldet es dann gleich wieder an.
        beiWechsel = {
            de.pagerspass.pagerspass.mobil.Geraeteeinstellungen.funkVorlesenSetzen(vorlesezusammenhang, it)
            befehle.funkVorlesen(it)
        },
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
