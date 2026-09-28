package de.pagerspass.pagerspass.mobil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pagerspass.pagerspass.ansichten.LeitstelleSeite
import de.pagerspass.pagerspass.ansichten.LeitstellenGriffe
import de.pagerspass.pagerspass.netz.Begleitergeraete
import kotlinx.coroutines.launch

/**
 * Der Leitstellenplatz im Rundenrahmen — die Verdrahtung zwischen
 * [LeitstelleSeite] und der [Runde].
 *
 * <b>Eine eigene Datei, damit der Rahmen eine Zeile hat.</b> Der Tisch hat an die
 * sechzig Griffe; stünden sie in `Rundenrahmen.kt`, liefe jede Änderung an der
 * Leitstelle durch die heißeste Datei der Runde.
 *
 * Hier liegt auch, was der Tisch über den Hub hinaus braucht: die nachgeladenen
 * Daten (`Leitstellendaten`) und der Tonregler, der auf die Lautsprecher der
 * Runde gelegt wird.
 */
@Composable
internal fun Leitstellenplatz(
    stand: Rundenstand,
    sitzung: Sitzung,
    runde: Runde,
    hilfe: () -> Unit,
) {
    val sitzungsstand by sitzung.stand.collectAsStateWithLifecycle()
    val seiten by sitzung.daten.collectAsStateWithLifecycle()
    val daten: Leitstellendaten = viewModel()
    val datenstand by daten.stand.collectAsStateWithLifecycle()
    val zusammenhang = LocalContext.current
    val bereich = rememberCoroutineScope()
    val ton by rememberTonstand()
    val bauform by rememberMelderBauform()
    val melderton by rememberMelderTon()

    val konto = sitzungsstand.konto
    val kennung = konto?.kennung.orEmpty()
    val aktuellerStand by rememberUpdatedState(stand)

    // Der Tonregler liegt am Gerät — hier kommt er auf die Lautsprecher.
    LaunchedEffect(ton) { runde.tonAnwenden(ton) }

    // Ob schon ein Handy am Platz hängt — einmal beim Hereinkommen.
    LaunchedEffect(stand.raum?.code) {
        if (stand.raum?.laeuft == true) runCatching { runde.begleiterGekoppelt() }
    }

    val griffe = LeitstellenGriffe(
        // ---------------------------------------------------------- Kopf
        wuerfeln = { runde.einsatzWuerfeln() },
        warnen = { runde.bevoelkerungWarnen(it) },
        dienstende = { runde.dienstBeenden() },
        verlassen = { runde.verlassen() },
        hilfe = hilfe,
        tonSetzen = { Tonregler.setzen(zusammenhang, it) },
        einzelrufZulassen = { runde.einzelrufZulassen(it) },
        ausbildungUeberspringen = { runde.ausbildungUeberspringen() },
        begleiterErzeugen = {
            val code = aktuellerStand.raum?.code.orEmpty()
            daten.begleiterErzeugen(
                kennung,
                code,
                Begleitergeraete(
                    bauform = bauform,
                    melderton = melderton,
                    zeigtMelder = true,
                    zeigtFunkgeraet = true,
                    zeigtFunkchat = true,
                    funkAusgelagert = true,
                ),
            )
            // Wie im Web: Wer den Code erzeugt, legt den Funk damit aufs Handy.
            daten.auslagern(code)
        },
        begleiterAuslagern = { an -> daten.auslagern(if (an) aktuellerStand.raum?.code else null) },
        begleiterPruefen = { bereich.launch { runCatching { runde.begleiterGekoppelt() } } },
        // ----------------------------------------------------- Besatzung
        leitstelleUebergeben = { runde.leitstelleUebergeben(it) },
        kicken = { runde.spielerKicken(it) },
        botVersetzen = { bot, vorlage -> runde.botZuweisen(bot, vorlage) },
        botEntfernen = { runde.botEntfernen(it) },
        botHinzufuegen = { vorlage, anzahl -> runde.botHinzufuegen(vorlage, anzahl) },
        // -------------------------------------------------------- Bänder
        sprechwunsch = { runde.sprechwunschBeantworten(it) },
        anrufAnnehmen = { runde.anrufAnnehmen(it) },
        anrufAbweisen = { runde.anrufAbweisen(it) },
        feststellungUebernehmen = { runde.feststellungUebernehmen(it) },
        feststellungVerwerfen = { runde.feststellungVerwerfen(it) },
        // -------------------------------------------------------- Einsatz
        einsatzAnlegen = { b ->
            runde.einsatzAnlegen(
                stichwort = b.stichwort,
                stichwortText = b.stichwortText,
                meldebild = b.meldebild,
                adresse = b.adresse,
                organisation = b.organisation,
                prioritaet = b.prioritaet,
                meldender = b.meldender,
                empfohleneFahrzeuge = b.empfohleneFahrzeuge,
                empfohleneFaehigkeiten = b.empfohleneFaehigkeiten,
                anrufId = b.anrufId,
                ortsteil = b.ortsteil,
                lat = b.lat,
                lon = b.lon,
                ursprungEinsatzId = b.ursprungEinsatzId,
            )
        },
        einsatzAendern = { id, sw, text, bild, prio, anzahl, faehigkeiten ->
            runde.einsatzAktualisieren(id, sw, text, bild, prio, anzahl, faehigkeiten)
        },
        einsatzSchliessen = { runde.einsatzSchliessen(it) },
        zurueckrufen = { id, fahrzeuge -> runde.fahrzeugeZurueckrufen(id, fahrzeuge, null) },
        zielklinik = { fahrzeug, klinik -> runde.zielklinikZuweisen(fahrzeug, klinik) },
        suchabschnitt = { fahrzeug, abschnitt -> runde.suchabschnittZuteilen(fahrzeug, abschnitt) },
        aufgabe = { fahrzeug, nummer -> runde.aufgabeZuteilen(fahrzeug, nummer) },
        anrufZuordnen = { anruf, einsatz -> runde.anrufZuordnen(anruf, einsatz) },
        vorschlagVerwerfen = { runde.vorschlagVerwerfen(it) },
        // ---------------------------------------------------------- Alarm
        alarmvorschlag = { runde.alarmvorschlag(it) },
        eintreffzeiten = { runde.eintreffzeiten(it) },
        alarmieren = { a ->
            runde.alarmieren(a.einsatzId, a.fahrzeugIds, a.abrollbehaelter, a.zusatztext, a.meldungId)
        },
        alarmMeldungStarten = { runde.alarmMeldungSprechenStarten(it) },
        alarmMeldungBeenden = { runde.alarmMeldungSprechenBeenden(it) },
        alarmMeldungVerwerfen = { runde.alarmMeldungVerwerfen(it) },
        alarmMeldungAbschliessen = { id -> bereich.launch { runde.alarmMeldungSprechenBeenden(id) } },
        schichtordnung = { name, anzahl, faehigkeiten -> runde.aaoVorlageSpeichern(name, anzahl, faehigkeiten) },
        ordnungenLaden = { daten.ordnungenLaden(kennung, aktuellerStand.raum?.settings?.landkreisId) },
        ordnungSichern = { daten.ordnungSichern(kennung, aktuellerStand.raum?.settings?.landkreisId, it) },
        ordnungLoeschen = { daten.ordnungLoeschen(kennung, it) },
        strassenLaden = { daten.strassenSicherstellen(it) },
        eventsLaden = { daten.eventsSicherstellen() },
        // -------------------------------------------------------- Telefon
        anrufFrage = { anruf, art -> runde.anrufFragen(anruf, art) },
        anrufFrageFrei = { anruf, text -> runde.anrufFrageFrei(anruf, text) },
        anrufOrten = { runde.anrufOrten(it) },
        anrufBeenden = { anruf, abbrechen -> runde.anrufBeenden(anruf, abbrechen) },
        notrufSprechenStarten = { runde.notrufSprechenStarten() },
        notrufSprechenBeenden = { runde.notrufSprechenBeenden(it) },
        journalOrten = { runde.journalOrten(it) },
        // -------------------------------------------------------- Tableau
        einzelrufStarten = { runde.einzelrufStarten(it) },
        streife = { fahrzeug, an -> runde.streifeSchicken(fahrzeug, an) },
        funkgruppeZuweisen = { fahrzeug, gruppe -> runde.funkgruppeZuweisen(fahrzeug, gruppe) },
        abrollbehaelter = { fahrzeug, vorlage -> runde.abrollbehaelterWechseln(fahrzeug, vorlage) },
        rufname = { fahrzeug, name, kurz -> runde.funkrufnameAendern(fahrzeug, name, kurz) },
        rufnameZuruecksetzen = { runde.funkrufnameZuruecksetzen(it) },
        // ----------------------------------------------------------- Funk
        funken = { text, an -> runde.funken(text, an) },
        sprechStart = { runde.sprechenStarten() },
        sprechEnde = { runde.sprechenBeenden() },
        funkVorlesen = { runde.funkVorlesen(it) },
        funkgruppenPlatz = { gruppen, sende -> runde.funkgruppenPlatz(gruppen, sende) },
        draht = { runde.drahtSenden(it) },
        drahtSprechStart = { runde.drahtSprechenStarten() },
        drahtSprechEnde = { runde.drahtSprechenBeenden() },
        funkhinweisWeg = { runde.funkhinweisWegnehmen() },
        // ------------------------------------------------------ Einzelruf
        einzelrufAnnehmen = { runde.einzelrufAnnehmen(it) },
        einzelrufAbweisen = { runde.einzelrufAbweisen(it) },
        einzelrufBeenden = { runde.einzelrufBeenden(it) },
        einzelrufSagen = { ruf, text -> runde.einzelrufSagen(ruf, text) },
        einzelrufSprechenStarten = { runde.einzelrufSprechenStarten() },
        einzelrufSprechenBeenden = { runde.einzelrufSprechenBeenden(it) },
        einzelrufMikrofon = { runde.einzelrufMikrofon(it) },
    )

    LeitstelleSeite(
        stand = stand,
        katalog = seiten.katalog.inhalt,
        konto = konto,
        daten = datenstand,
        griffe = griffe,
    )
}
