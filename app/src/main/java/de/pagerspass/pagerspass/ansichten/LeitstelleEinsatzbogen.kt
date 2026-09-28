package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Leitstellenstand
import de.pagerspass.pagerspass.netz.AaoVorlagenzeile
import de.pagerspass.pagerspass.netz.Einsatz
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Notrufvorschlag
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Stichwort
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlin.random.Random

/**
 * Der Einsatzbogen — `NeuerEinsatzDialog.vue` in allen fünf Fassungen: leer
 * (Notrufannahme der Freien Vergabe, mit Würfel), nach dem Gespräch (vorbelegt
 * mit dem Vorschlag, dazu „zu laufendem Einsatz nehmen"), nachträglich geortet,
 * als Folgeeinsatz und als Umstufung eines laufenden Einsatzes.
 *
 * <b>Die Koordinate geht nur mit, solange der Ort unverändert ist.</b> Wer die
 * Adresse aus dem Gespräch umschreibt, meint einen anderen Ort — dann fahren die
 * Fahrzeuge auf die Adresse, nicht auf den alten Punkt.
 *
 * @param beiSchliessen `true`, wenn der Bogen abgeschickt wurde — nach einem
 *   Gespräch heißt `false`: Der Vorschlag wird verworfen.
 */
@Composable
internal fun Einsatzbogenblende(
    raum: Raumzustand,
    katalog: Katalog?,
    konto: Konto?,
    daten: Leitstellenstand,
    griffe: LeitstellenGriffe,
    beiSchliessen: (abgeschickt: Boolean) -> Unit,
    umstufen: Einsatz? = null,
    vorschlag: Notrufvorschlag? = null,
    anrufId: String? = null,
    geortet: Geortet? = null,
    ursprung: Einsatz? = null,
) {
    val aktiveOrgs = raum.settings.organisationen.ifEmpty { Rundentexte.ORGANISATIONEN }
    val aaoFrei = freigeschaltet(raum, konto, "EigeneAao")
    val landkreisId = raum.settings.landkreisId

    var organisation by remember {
        mutableStateOf(umstufen?.organisation ?: vorschlag?.organisation ?: aktiveOrgs.first())
    }
    var suche by remember { mutableStateOf("") }
    var stichwortId by remember { mutableStateOf<String?>(null) }
    var stichwort by remember { mutableStateOf(umstufen?.stichwort ?: vorschlag?.stichwort ?: "") }
    var stichwortText by remember { mutableStateOf(umstufen?.stichwortText ?: vorschlag?.stichwortText ?: "") }
    var meldebild by remember { mutableStateOf(umstufen?.meldebild ?: vorschlag?.meldebild ?: "") }
    var adresse by remember {
        mutableStateOf(umstufen?.adresse ?: geortet?.text ?: ursprung?.adresse ?: vorschlag?.adresse ?: "")
    }
    var ortsteil by remember {
        mutableStateOf(umstufen?.ortsteil ?: ursprung?.ortsteil ?: vorschlag?.ortsteil ?: "")
    }
    var meldender by remember { mutableStateOf(vorschlag?.meldender ?: "") }
    var prioritaet by remember { mutableIntStateOf(umstufen?.prioritaet ?: vorschlag?.prioritaet ?: 2) }
    var anzahl by remember {
        mutableIntStateOf(umstufen?.empfohleneFahrzeuge ?: vorschlag?.empfohleneFahrzeuge ?: 1)
    }
    var faehigkeiten by remember {
        mutableStateOf(umstufen?.empfohleneFaehigkeiten ?: vorschlag?.empfohleneFaehigkeiten ?: emptyList())
    }
    var zeigeFeinheiten by remember { mutableStateOf(umstufen != null) }
    var zeigeZuordnung by remember { mutableStateOf(false) }
    var ortwahl by remember { mutableStateOf(false) }

    LaunchedEffect(landkreisId) { landkreisId?.let { griffe.strassenLaden(it) } }
    LaunchedEffect(Unit) { griffe.ordnungenLaden() }

    val stichworte = katalog?.stichworte.orEmpty().filter { it.organisation in aktiveOrgs }
    val text = suche.trim().lowercase()
    val treffer = if (text.isEmpty()) {
        stichworte.filter { it.organisation == organisation }
    } else {
        stichworte.filter { s ->
            s.stichwort.lowercase().contains(text) ||
                s.stichwortText.lowercase().contains(text) ||
                s.meldebilder.any { it.lowercase().contains(text) }
        }
    }
    val gewaehltesStichwort = katalog?.stichworte?.firstOrNull { it.id == stichwortId }

    val eigene = daten.eigeneStichworte.filter { v ->
        text.isEmpty() ||
            v.stichwort.orEmpty().lowercase().contains(text) ||
            v.stichwortText.orEmpty().lowercase().contains(text) ||
            v.name.lowercase().contains(text)
    }

    val echteStrassen = landkreisId?.let { daten.strassen[it] }.orEmpty()
    val nutztEchteStrassen = echteStrassen.isNotEmpty()
    val orte = katalog?.orte.orEmpty()
    val ortAuswahl: List<Pair<String, String>> = if (nutztEchteStrassen) {
        echteStrassen.map { it to it }
    } else {
        orte.map { o ->
            "${o.strasse}|${o.ortsteil}" to "${o.strasse}${o.objekt?.let { " · $it" } ?: ""} (${o.ortsteil})"
        }
    }

    fun stichwortUebernehmen(s: Stichwort) {
        stichwortId = s.id
        organisation = s.organisation
        stichwort = s.stichwort
        stichwortText = s.stichwortText
        prioritaet = s.prioritaet
        anzahl = s.empfohleneFahrzeuge
        faehigkeiten = s.empfohleneFaehigkeiten
        if (meldebild.isBlank() && s.meldebilder.isNotEmpty()) meldebild = s.meldebilder.first()
    }

    fun eigenesUebernehmen(v: AaoVorlagenzeile) {
        stichwortId = null
        stichwort = v.stichwort.orEmpty()
        stichwortText = v.stichwortText.orEmpty()
        anzahl = v.empfohleneFahrzeuge
        faehigkeiten = v.empfohleneFaehigkeiten
        v.meldebild?.let { if (meldebild.isBlank()) meldebild = it }
        v.adresse?.let { if (adresse.isBlank()) adresse = it }
    }

    fun hausnummer() = 1 + Random.nextInt(119)

    fun ortUebernehmen(wert: String) {
        if (nutztEchteStrassen) {
            adresse = "$wert ${hausnummer()}"
            ortsteil = ""
            return
        }
        val ort = orte.firstOrNull { "${it.strasse}|${it.ortsteil}" == wert } ?: return
        val nr = hausnummer()
        adresse = ort.objekt?.let { "$it, ${ort.strasse} $nr" } ?: "${ort.strasse} $nr"
        ortsteil = ort.ortsteil
    }

    fun wuerfeln() {
        val auswahl = stichworte.randomOrNull() ?: return
        stichwortUebernehmen(auswahl)
        meldebild = auswahl.meldebilder.randomOrNull() ?: auswahl.stichwortText
        val nr = hausnummer()
        if (nutztEchteStrassen) {
            echteStrassen.randomOrNull()?.let {
                adresse = "$it $nr"
                ortsteil = ""
            }
        } else {
            orte.randomOrNull()?.let { ort ->
                adresse = ort.objekt?.let { "$it, ${ort.strasse} $nr" } ?: "${ort.strasse} $nr"
                ortsteil = ort.ortsteil
            }
        }
        meldender = katalog?.meldende?.randomOrNull().orEmpty()
    }

    // Die Koordinate aus dem Gespräch oder der Ortung — nur, solange der Ort
    // unverändert dasteht.
    val adresseAusDemGespraech = vorschlag != null && vorschlag.lat != null && vorschlag.lon != null &&
        adresse.trim() == vorschlag.adresse.trim() &&
        ortsteil.trim() == vorschlag.ortsteil.orEmpty().trim()
    val ortAusDerOrtung = geortet != null && adresse.trim() == geortet.text.trim()
    val koordinate: Pair<Double, Double>? = when {
        ortAusDerOrtung && geortet != null -> geortet.lat to geortet.lon
        adresseAusDemGespraech && vorschlag?.lat != null && vorschlag.lon != null -> vorschlag.lat to vorschlag.lon
        else -> null
    }

    val gueltig = stichwort.isNotBlank() && (umstufen != null || adresse.isNotBlank())
    val zuordenbar = umstufen == null && vorschlag != null && anrufId != null

    fun absenden() {
        if (!gueltig) return
        if (umstufen != null) {
            griffe.einsatzAendern(
                umstufen.id,
                stichwort.trim(),
                stichwortText.trim().ifEmpty { stichwort.trim() },
                meldebild.trim(),
                prioritaet,
                anzahl,
                faehigkeiten,
            )
        } else {
            griffe.einsatzAnlegen(
                Einsatzbogen(
                    stichwort = stichwort.trim(),
                    stichwortText = stichwortText.trim().ifEmpty { stichwort.trim() },
                    meldebild = meldebild.trim(),
                    adresse = adresse.trim(),
                    organisation = organisation,
                    prioritaet = prioritaet,
                    ortsteil = ortsteil.trim().ifEmpty { null },
                    meldender = meldender.trim().ifEmpty { null },
                    empfohleneFahrzeuge = anzahl,
                    empfohleneFaehigkeiten = faehigkeiten,
                    lat = koordinate?.first,
                    lon = koordinate?.second,
                    anrufId = anrufId,
                    ursprungEinsatzId = ursprung?.id,
                ),
            )
        }
        beiSchliessen(true)
    }

    Blende(
        titel = if (umstufen != null) "${umstufen.einsatznummer} umstufen" else "Einsatz anlegen",
        beiSchliessen = { beiSchliessen(false) },
        breite = Dialogbreite.Breit,
        kopfknoepfe = {
            // Würfeln nur beim leeren Bogen — nach einem Gespräch wirft es die
            // Arbeit weg, für die man abgefragt hat.
            if (umstufen == null && vorschlag == null && geortet == null) {
                Knopf("Würfeln", { wuerfeln() }, art = Knopfart.Leise, kompakt = true)
            }
            Knopf("Abbrechen", { beiSchliessen(false) }, art = Knopfart.Leise, kompakt = true)
        },
        fuss = {
            Knopf(
                if (umstufen != null) "Umstufen" else "Einsatz eröffnen",
                { absenden() },
                art = Knopfart.Haupt,
                aktiv = gueltig,
                breit = true,
            )
        },
    ) {
        Etikett(
            when {
                umstufen != null -> "Lagemeldung ausgewertet"
                geortet != null -> "Nachträgliche Ortung"
                else -> "Notrufannahme"
            },
        )

        // Meldet der Anrufer eine Lage, die schon läuft, gehört seine Meldung an
        // den bestehenden Einsatz — ein zweiter Alarm bindet dieselben Fahrzeuge.
        if (zuordenbar && anrufId != null) {
            val laufende = raum.incidents.filter { !it.abgeschlossen }
                .map { e ->
                    val m = if (vorschlag?.lat != null && vorschlag.lon != null && e.lat != null && e.lon != null) {
                        distanzMeter(vorschlag.lat, vorschlag.lon, e.lat, e.lon)
                    } else {
                        null
                    }
                    e to m
                }
                .sortedWith(compareBy<Pair<Einsatz, Double?>> { it.second ?: Double.MAX_VALUE }.thenBy { it.first.einsatznummer })
            Text(
                "${if (zeigeZuordnung) "▾" else "▸"} Zu laufendem Einsatz nehmen (${laufende.size} offen)",
                style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                color = Farben.Text,
                modifier = Modifier.fillMaxWidth().clickable { zeigeZuordnung = !zeigeZuordnung }.padding(vertical = Abstand.Winzig),
            )
            if (zeigeZuordnung) {
                SehrLeise(
                    "Meldet der Anrufer eine Lage, die schon läuft, geht seine Meldung an den bestehenden " +
                        "Einsatz — es entsteht kein zweiter.",
                )
                if (laufende.isEmpty()) {
                    SehrLeise("Es läuft kein Einsatz, zu dem dieser Anruf gehören könnte.", mono = true)
                }
                laufende.forEach { (e, m) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .flaeche(farbe = Farben.FlaecheHoch, randfarbe = Rundentexte.organisationFarbe(e.organisation), ecke = 9.dp)
                            .clickable {
                                griffe.anrufZuordnen(anrufId, e.id)
                                beiSchliessen(true)
                            }
                            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                    ) {
                        Text(e.einsatznummer, style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                        Text(
                            "${e.stichwort} ${e.stichwortText} · ${e.adresse}",
                            style = Schrift.Klein,
                            color = Farben.Text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        m?.let {
                            SehrLeise(if (it < 1000) "${it.toInt()} m" else "%.1f km".format(it / 1000), mono = true)
                        }
                    }
                }
            }
        }

        // Die Stichwortsuche über alle aktiven Organisationen.
        Feld(
            wert = suche,
            beiAenderung = { suche = it },
            etikett = "Stichwort suchen",
            platzhalter = "z. B. Brand, eingeklemmt, Atemnot …",
        )
        if (suche.isBlank()) {
            Pillenreihe {
                aktiveOrgs.forEach { org ->
                    Pille(
                        aufschrift = Rundentexte.organisation(org),
                        an = organisation == org,
                        farbe = Rundentexte.organisationFarbe(org),
                        beiDruck = { organisation = org },
                    )
                }
            }
        }

        // Die eigenen Stichwörter zuerst, in einer eigenen Liste.
        if (eigene.isNotEmpty()) {
            Etikett("Eigene Stichwörter")
            eigene.forEach { v ->
                Stichwortzeile(
                    kurz = v.stichwort.orEmpty(),
                    lang = v.stichwortText.orEmpty(),
                    rechts = if (v.landkreisId != null) "nur hier" else "überall",
                    an = false,
                    farbe = Farben.Violett,
                    prio = 2,
                    beiDruck = { eigenesUebernehmen(v) },
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
        ) {
            treffer.forEach { s ->
                Stichwortzeile(
                    kurz = s.stichwort,
                    lang = s.stichwortText,
                    rechts = Rundentexte.organisation(s.organisation),
                    an = s.id == stichwortId,
                    farbe = Rundentexte.organisationFarbe(s.organisation),
                    prio = s.prioritaet,
                    beiDruck = { stichwortUebernehmen(s) },
                )
            }
            if (treffer.isEmpty()) {
                SehrLeise("Nichts gefunden — die Felder unten lassen sich frei ausfüllen.", mono = true)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Feld(
                wert = stichwort,
                beiAenderung = { stichwort = it.take(12) },
                etikett = "Stichwort",
                platzhalter = "B2",
                modifier = Modifier.weight(1f),
            )
            Feld(
                wert = stichwortText,
                beiAenderung = { stichwortText = it.take(60) },
                etikett = "Klartext",
                platzhalter = "Zimmerbrand",
                modifier = Modifier.weight(2f),
            )
        }

        if (umstufen == null) {
            if (ortAuswahl.isNotEmpty()) {
                Wahlfeld(
                    etikett = if (nutztEchteStrassen) {
                        "Einsatzort aus dem Ortsverzeichnis (echte Straßen des Kreises)"
                    } else {
                        "Einsatzort aus dem Ortsverzeichnis"
                    },
                    wert = null,
                    platzhalter = "Straße suchen — z. B. Bahnhof",
                    beiDruck = { ortwahl = true },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Feld(
                    wert = adresse,
                    beiAenderung = { adresse = it.take(120) },
                    etikett = "Einsatzort",
                    platzhalter = "Bahnhofstraße 12",
                    modifier = Modifier.weight(2f),
                )
                Feld(
                    wert = ortsteil,
                    beiAenderung = { ortsteil = it.take(40) },
                    etikett = "Ortsteil",
                    platzhalter = "Mitte",
                    modifier = Modifier.weight(1f),
                )
            }
            if (adresseAusDemGespraech) {
                SehrLeise("Koordinate aus dem Gespräch wird übernommen — die Fahrzeuge fahren auf den Punkt.", mono = true)
            }
            if (ortAusDerOrtung) {
                SehrLeise("Koordinate aus der Ortung wird übernommen.", mono = true)
            }
        }

        Feld(
            wert = meldebild,
            beiAenderung = { meldebild = it.take(400) },
            etikett = "Meldebild",
            platzhalter = "Was hat der Anrufer gemeldet?",
            einzeilig = false,
        )
        gewaehltesStichwort?.meldebilder?.takeIf { it.isNotEmpty() }?.let { bilder ->
            Pillenreihe {
                bilder.forEach { m -> Pille(m, an = meldebild == m, beiDruck = { meldebild = m }) }
            }
        }

        Etikett("Dringlichkeit")
        Pillenreihe {
            listOf(1 to "Normal", 2 to "Dringend", 3 to "Sonderrechte").forEach { (p, wort) ->
                Pille(wort, an = prioritaet == p, farbe = prioFarbe(p), beiDruck = { prioritaet = p })
            }
        }
        if (umstufen != null && prioritaet > umstufen.prioritaet) {
            SehrLeise("Höherstufen ordnet an, wie gefahren wird — für die Wertung zählt die Lage.")
        }

        // Die Feinheiten steuern den Alarmvorschlag und sind selten nötig.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { zeigeFeinheiten = !zeigeFeinheiten }.padding(vertical = Abstand.Winzig),
        ) {
            Text(
                "${if (zeigeFeinheiten) "▾" else "▸"} Alarm- und Ausrückeordnung",
                style = Schrift.Klein.copy(fontWeight = FontWeight.SemiBold),
                color = Farben.Text,
            )
            SehrLeise("($anzahl Fzg${if (faehigkeiten.isNotEmpty()) ", ${faehigkeiten.size} Funktion(en)" else ""})")
            if (!aaoFrei) Text("🔒", style = Schrift.Klein)
        }
        if (zeigeFeinheiten) {
            AaoOrdnung(
                anzahl = anzahl,
                faehigkeiten = faehigkeiten,
                beiAnzahl = { anzahl = it },
                beiFaehigkeiten = { faehigkeiten = it },
                beiGeaendert = {},
                raum = raum,
                katalog = katalog,
                konto = konto,
                daten = daten,
                griffe = griffe,
                umstufen = umstufen != null,
                bogen = Bogenangaben(stichwort, stichwortText, meldebild, adresse),
                beiUebernommen = { v ->
                    if (!v.stichwort.isNullOrBlank() && stichwort.isBlank()) {
                        stichwortId = null
                        stichwort = v.stichwort
                        stichwortText = v.stichwortText ?: v.stichwort
                    }
                    v.meldebild?.let { if (meldebild.isBlank()) meldebild = it }
                    v.adresse?.let { if (adresse.isBlank()) adresse = it }
                },
            )
            if (umstufen == null) {
                Feld(
                    wert = meldender,
                    beiAenderung = { meldender = it.take(80) },
                    etikett = "Meldender",
                    platzhalter = "Anruferin, Nachbarin aus dem Erdgeschoss",
                )
            }
        }
    }

    if (ortwahl) {
        Wahlblende(
            titel = "Einsatzort",
            gruppen = listOf(null to ortAuswahl),
            aufschrift = { it.second },
            beiWahl = {
                ortwahl = false
                ortUebernehmen(it.first)
            },
            beiSchliessen = { ortwahl = false },
            suchbar = true,
        )
    }
}

/** Eine Zeile der Stichwortliste — Kürzel, Klartext, Organisation. */
@Composable
private fun Stichwortzeile(
    kurz: String,
    lang: String,
    rechts: String,
    an: Boolean,
    farbe: androidx.compose.ui.graphics.Color,
    prio: Int,
    beiDruck: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(
                farbe = if (an) Farben.FlaecheAktiv else Farben.FlaecheHoch,
                randfarbe = if (an) Farben.Amber else Farben.Rand,
                ecke = 9.dp,
            )
            .clickable(onClick = beiDruck)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Box(Modifier.width(3.dp).height(18.dp).background(farbe))
        Text(kurz, style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = prioFarbe(prio))
        Text(
            lang,
            style = Schrift.Klein,
            color = Farben.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        SehrLeise(rechts)
    }
}
