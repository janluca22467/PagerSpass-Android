package de.pagerspass.pagerspass.mobil

import android.content.Context
import de.pagerspass.pagerspass.ansichten.BegleiterGriffe
import de.pagerspass.pagerspass.ansichten.FahrzeugGriffe
import de.pagerspass.pagerspass.ansichten.Funkgriffe
import de.pagerspass.pagerspass.ansichten.Ladenbereich
import de.pagerspass.pagerspass.ansichten.ManvGriffe
import de.pagerspass.pagerspass.ansichten.Patientengriffe
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Begleitergeraete
import de.pagerspass.pagerspass.netz.Fahrzeugwege
import de.pagerspass.pagerspass.netz.Netz
import kotlinx.coroutines.flow.first

/**
 * Die Griffe des Fahrzeugs — aus der `Runde` gebaut, an einer Stelle.
 *
 * Steht hier und nicht im Rundenrahmen, damit der Rahmen eine Zeile hat und nicht
 * sechzig — und damit die Arbeit an der Leitstelle dort nicht mit dieser hier
 * zusammenstößt.
 */
internal fun fahrzeuggriffe(
    runde: Runde,
    zusammenhang: Context,
    kennung: String,
    hilfe: () -> Unit,
): FahrzeugGriffe = FahrzeugGriffe(
    fms = { status, grund, dauer -> runde.fmsSetzen(status, grund, dauer) },
    sondersignal = { runde.sondersignal(it) },
    lagemeldung = { runde.lagemeldung(it) },
    nachfordern = { runde.nachfordern(it) },
    funk = Funkgriffe(
        funk = { runde.funken(it) },
        einsatzstelle = { runde.einsatzstelleSchreiben(it) },
        sprechstart = { runde.sprechenStarten() },
        sprechende = { runde.sprechenBeenden() },
        einsatzstelleSprechstart = { runde.einsatzstelleSprechenStarten() },
        einsatzstelleSprechende = { runde.einsatzstelleSprechenBeenden() },
        notruf = { runde.notruf() },
        funkgruppe = { fahrzeug, gruppe -> runde.funkgruppeZuweisen(fahrzeug, gruppe) },
        einzelrufStarten = { runde.einzelrufStarten(it) },
        einzelrufAnnehmen = { runde.einzelrufAnnehmen(it) },
        einzelrufAbweisen = { runde.einzelrufAbweisen(it) },
        einzelrufBeenden = { runde.einzelrufBeenden(it) },
    ),
    ueberspringen = { runde.ausbildungUeberspringen() },
    dienstende = { runde.dienstBeenden() },
    verlassen = { runde.verlassen() },
    hilfe = hilfe,
    alarmQuittieren = { runde.alarmQuittieren() },
    alarmWeg = { runde.alarmWegtippen() },
    leitstelleUebernehmen = { runde.leitstelleUebernehmen() },
    streife = { fahrzeug, an -> runde.streifeSchicken(fahrzeug, an) },
    streifeneinsatz = { m ->
        runde.streifeneinsatzAnlegen(
            stichwort = m.stichwort,
            stichwortText = m.stichwortText,
            meldebild = m.meldebild,
            adresse = m.adresse,
            ortsteil = null,
            prioritaet = m.prioritaet,
            lat = null,
            lon = null,
            empfohleneFahrzeuge = m.empfohleneFahrzeuge,
            empfohleneFaehigkeiten = m.empfohleneFaehigkeiten,
        )
    },
    wasserAufnehmen = { runde.wasserAufnehmen() },
    aufgabeUebernehmen = { runde.aufgabeUebernehmen(it) },
    patient = Patientengriffe(
        messen = { e, p, was -> runde.patientMessen(e, p, was) },
        schema = { e, p, s -> runde.patientSchema(e, p, s) },
        massnahme = { e, p, m -> runde.patientMassnahme(e, p, m) },
        verdacht = { e, p, t -> runde.patientVerdacht(e, p, t) },
        diagnose = { e, p, t -> runde.patientDiagnose(e, p, t) },
    ),
    einzelruf = BegleiterGriffe(
        einzelrufStarten = { runde.einzelrufStarten(it) },
        einzelrufAnnehmen = { runde.einzelrufAnnehmen(it) },
        einzelrufAbweisen = { runde.einzelrufAbweisen(it) },
        einzelrufBeenden = { runde.einzelrufBeenden(it) },
        einzelrufSagen = { ruf, text -> runde.einzelrufSagen(ruf, text) },
        einzelrufSprechenStarten = { runde.einzelrufSprechenStarten() },
        einzelrufSprechenBeenden = { runde.einzelrufSprechenBeenden(it) },
        einzelrufZulassen = { runde.einzelrufZulassen(it) },
    ),
    einzelrufZulassen = { runde.einzelrufZulassen(it) },
    pegel = { funk, einsatzstelle, durchsage -> runde.pegelSetzen(funk, einsatzstelle, durchsage) },
    begleiterZugang = {
        runCatching {
            val code = runde.stand.value.code
            val ablage = Ablage(zusammenhang)
            val tonwahl = Tonwahl.von(zusammenhang)
            val zugang = Fahrzeugwege(Netz(ablage)).funkbegleiter(
                kennung,
                code,
                Begleitergeraete(
                    bauform = ablage.melderBauformFluss().first(),
                    melderton = ablage.melderTonFluss().first(),
                    bauart = tonwahl.bauart,
                    funkAusgelagert = true,
                ),
            )
            // Ausdrücklich der Handy-Zweig: Der Link geht per QR-Code an ein Telefon.
            "${ablage.server().trimEnd('/')}/play/mobile/funk/${zugang.token}"
        }
    },
    premium = {
        // Der Laden liegt außerhalb der Runde — erst hinaus, dann dorthin.
        Einsprung.oeffnen(Ladenbereich.Premium.weg)
    },
)

/** Die Griffe der Führung und des Massenanfalls — samt Tablet (Lücke A7). */
internal fun manvgriffe(runde: Runde): ManvGriffe = ManvGriffe(
    uebernehmen = { e, z -> runde.einsatzleitungUebernehmen(e, z) },
    abgeben = { e, z -> runde.einsatzleitungAbgeben(e, z) },
    abschnittBilden = { e, n, z -> runde.abschnittBilden(e, n, z) },
    abschnittZuteilen = { e, f, a, z -> runde.abschnittZuteilen(e, f, a, z) },
    auftrag = { e, a, f -> runde.manvauftragUebertragen(e, a, f) },
    anordnen = { e, art, g -> runde.versorgungsstelleAnordnen(e, art, g) },
    abbauen = { e, s, ab -> runde.versorgungsstelleAbbauen(e, s, ab) },
    verlegen = { e, p, s -> runde.patientVerlegen(e, p, s) },
    transportmittel = { e, p, f -> runde.patientTransportmittel(e, p, f) },
    zielklinik = { e, p, k -> runde.patientZielklinik(e, p, k) },
    transport = { e, p -> runde.transportEinleiten(e, p) },
    verstorbene = { runde.verstorbeneUebergeben(it) },
    triage = { runde.triageKoordinieren(it) },
    abschnittUmbenennen = { e, bisher, neu, z -> runde.abschnittUmbenennen(e, bisher, neu, z) },
    nachforderungenWeiterreichen = { e, z -> runde.nachforderungenWeiterreichen(e, z) },
    bereitstellungsraumFestlegen = { runde.bereitstellungsraumFestlegen(it) },
    bereitstellungSetzen = { e, f, halten -> runde.bereitstellungSetzen(e, f, halten) },
    landeplatzFestlegen = { runde.landeplatzFestlegen(it) },
    landeplatzAusleuchten = { runde.landeplatzAusleuchten(it) },
    einsatzfunkgruppeOeffnen = { e, name -> runde.einsatzfunkgruppeOeffnen(e, name) },
    einsatzfunkgruppeSchliessen = { runde.einsatzfunkgruppeSchliessen(it) },
    funkgruppeZuweisen = { f, g -> runde.funkgruppeZuweisen(f, g) },
)
