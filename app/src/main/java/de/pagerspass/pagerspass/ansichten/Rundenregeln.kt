package de.pagerspass.pagerspass.ansichten

import de.pagerspass.pagerspass.mobil.Einstellungsaenderung
import de.pagerspass.pagerspass.netz.Rundeneinstellungen

/**
 * Die Schalter der Einsatzregeln und die Seiten des Rundendialogs — übertragen aus
 * `components/lobby/rundenregeln.ts`.
 *
 * <b>Warum als Daten.</b> Im Web standen sie bis 5.0.0.9 als zwölf fast gleiche
 * Blöcke hintereinander und zusätzlich noch einmal als Namensliste, damit die
 * Gruppe ihren Stand („6 von 12 an") zählen konnte. Zwei Listen derselben Sache
 * laufen auseinander, sobald jemand einen dreizehnten Schalter dazubaut. Jetzt gibt
 * es eine, und aus ihr zeichnen sich der Dialog und der Steckbrief der Lobby.
 *
 * <b>Die Gruppen.</b> Zwölf Schalter in einer Reihe liest niemand; vier Gruppen zu
 * je zwei bis vier schon. Sie folgen der Frage, die man sich beim Einstellen
 * stellt: Wie kommen die Einsätze herein? Wie realistisch ist der Alltag? Welche
 * besonderen Lagen gibt es? Was geschieht vor Ort?
 */

/**
 * Die Seiten des Rundendialogs — hier und nicht im Dialog, weil die Lobby sie
 * ebenfalls kennt: Ein Druck auf eine Zeile des Steckbriefs öffnet den Dialog genau
 * auf der Seite, auf der diese Einstellung steht.
 */
enum class Rundenseite { Grund, Lage, Regeln, Orgs, Funk, Wachen, Bots, Vorlage }

enum class Regelgruppe(val titel: String, val unter: String) {
    Notruf("Notruf und Aufträge", "Wie die Einsätze hereinkommen."),
    Alltag("Dienstalltag", "Idealfall oder das, was wirklich bremst."),
    Lagen("Besondere Lagen", "Einsätze, die eine eigene Fläche bekommen."),
    Vorort("An der Einsatzstelle", "Was nach dem Eintreffen geschieht."),
}

/**
 * Eine Einsatzregel.
 *
 * @param text Was „an" heißt — und am Ende, was „aus" heißt.
 * @param auchInAusbildung Die Ausbildungsschicht übt den Regelfall: Dort gibt es nur
 *   die telefonische Leitstelle, alles Übrige steht nicht zur Wahl.
 */
class Regel(
    val schluessel: String,
    val gruppe: Regelgruppe,
    val titel: String,
    val text: String,
    val an: (Rundeneinstellungen) -> Boolean,
    val setzen: (Boolean) -> Einstellungsaenderung,
    val auchInAusbildung: Boolean = false,
)

val REGELN: List<Regel> = listOf(
    Regel(
        "telefonischeLeitstelle", Regelgruppe.Notruf, "Telefonische Leitstelle",
        "Notrufe kommen als Anruf herein. Es klingelt, jemand meldet sich — und was du " +
            "erfährst, hängt davon ab, was du fragst. Aus bleibt alles wie gewohnt.",
        { it.telefonischeLeitstelle }, { Einstellungsaenderung(telefonischeLeitstelle = it) },
        auchInAusbildung = true,
    ),
    // Die einzige Regel, die einen Einsatz ankündigt, statt ihn zu melden — für
    // eine Dialysefahrt ruft niemand die 112.
    Regel(
        "verlegungsfahrten", Regelgruppe.Notruf, "Terminfahrten",
        "Krankentransport und Verlegung werden bestellt statt gemeldet: mit Abholort, " +
            "einem Haus, das den Patienten erwartet, und einem Termin einige Minuten in der " +
            "Zukunft. Wer zu spät alarmiert, reißt ihn; wer gar nichts schickt, verliert den " +
            "Auftrag. Aus heißt: Sie kommen als Notruf wie jede andere Lage.",
        { it.verlegungsfahrten }, { Einstellungsaenderung(verlegungsfahrten = it) },
    ),
    // Das zentrale Problem der Freiwilligen Feuerwehr.
    Regel(
        "tagesalarmstaerke", Regelgruppe.Alltag, "Tagesalarmstärke berücksichtigen",
        "Freiwillige Feuerwehr und THW brauchen fünf bis sieben Minuten zum Ausrücken — " +
            "werktags tagsüber am längsten, weil die Besatzung auf Arbeit ist, am Feierabend " +
            "und am Wochenende am kürzesten. Berufsfeuerwehr, Rettungsdienst und Polizei " +
            "sitzen auf der Wache und sind in ein bis drei Minuten draußen. Aus heißt: jedes " +
            "Fahrzeug ist zu jeder Stunde sofort besetzbar.",
        { it.tagesalarmstaerke }, { Einstellungsaenderung(tagesalarmstaerke = it) },
    ),
    Regel(
        "loeschwasser", Regelgruppe.Alltag, "Löschwasser berücksichtigen",
        "Tanks laufen leer, die Wasserversorgung muss erst aufgebaut werden, und im " +
            "Außenbereich ohne Hydrantennetz bleibt nur der Pendelverkehr zur nächsten " +
            "Entnahmestelle. Aus heißt: jeder Tank reicht ewig.",
        { it.loeschwasser }, { Einstellungsaenderung(loeschwasser = it) },
    ),
    Regel(
        "sonderobjekte", Regelgruppe.Alltag, "Sonderobjekte bespielen",
        "Ein Teil der Einsätze liegt in Schulen, Pflegeheimen, Betrieben oder Bahnhöfen. " +
            "Dann steht am Einsatz ein Einsatzplan mit der Zahl der Menschen im Objekt, und " +
            "die Alarm- und Ausrückeordnung fällt größer aus — aber nur, wenn das Objekt " +
            "selbst betroffen ist. Aus heißt: jede Adresse ist dieselbe Adresse.",
        { it.sonderobjekte }, { Einstellungsaenderung(sonderobjekte = it) },
    ),
    Regel(
        "wiederherstellung", Regelgruppe.Alltag, "Einsatzbereitschaft wiederherstellen",
        "Nach dem Einsatz steht ein Fahrzeug für ein paar Minuten auf Status 6: der " +
            "Rettungswagen wird desinfiziert, das Löschfahrzeug füllt den Tank und tauscht " +
            "Schläuche. Wer nur hingefahren und wieder abgedreht ist, bleibt sofort " +
            "verfügbar. Aus heißt: Status 2 gilt in dem Moment, in dem die Besatzung ihn drückt.",
        { it.wiederherstellung }, { Einstellungsaenderung(wiederherstellung = it) },
    ),
    // Ausdrücklich nicht ans Datum gebunden: Der beliebteste Dienst im Jahr ist
    // nichts, worauf man bis zum 31. Dezember warten möchte.
    Regel(
        "silvester", Regelgruppe.Lagen, "Silvesterlage",
        "Kleinbrände, brennende Container und Handverletzungen durch Feuerwerkskörper in " +
            "Serie — dazu etwas mehr Platz auf der Einsatzliste, weil in dieser Nacht mehrere " +
            "Kleinigkeiten gleichzeitig laufen. Aus heißt: ein Dienst wie jeder andere.",
        { it.silvester }, { Einstellungsaenderung(silvester = it) },
    ),
    Regel(
        "gefahrgutlagen", Regelgruppe.Lagen, "Gefahrgutlagen mit Ausbreitung",
        "Eine ABC-Lage bekommt einen Absperrbereich, der wächst, solange kein Messtrupp " +
            "vor Ort ist, und eine Ausbreitung in Windrichtung. Wer keinen Atemschutz führt, " +
            "hält gegen den Wind im Bereitstellungsraum. Liegt ein Sonderobjekt in der Fahne, " +
            "muss es geräumt werden. Aus heißt: ein Einsatz wie jeder andere.",
        { it.gefahrgutlagen }, { Einstellungsaenderung(gefahrgutlagen = it) },
    ),
    Regel(
        "suchlagen", Regelgruppe.Lagen, "Vermisstensuche als Flächenlage",
        "Eine Suchlage bekommt ein Gebiet in acht Abschnitten und eine Person, die " +
            "irgendwo darin liegt. Eine Rettungshundestaffel ist dreimal so schnell wie ein " +
            "Löschfahrzeug, und nachts findet niemand etwas, solange kein Licht da ist. Aus " +
            "heißt: ein Einsatz wie jeder andere.",
        { it.suchlagen }, { Einstellungsaenderung(suchlagen = it) },
    ),
    Regel(
        "vegetationsbraende", Regelgruppe.Lagen, "Vegetationsbrand als wachsende Fläche",
        "Ein Wald- oder Flächenbrand bekommt Hektar statt eines Punktes. Die Fläche wächst " +
            "mit Wind und Trockenheit, solange zu wenig dagegen steht — und ohne Wasser hilft " +
            "auch das beste Fahrzeug nicht. Aus heißt: ein Einsatz wie jeder andere.",
        { it.vegetationsbraende }, { Einstellungsaenderung(vegetationsbraende = it) },
    ),
    Regel(
        "einsatzarbeit", Regelgruppe.Vorort, "Tätigkeiten an der Einsatzstelle",
        "Jede Einsatzstelle bekommt Aufgaben aus ihrer Alarm- und Ausrückeordnung — " +
            "erkunden, arbeiten, aufräumen. Wie schnell, hängt an der Zahl der Kräfte und " +
            "daran, ob das Fahrzeug kann, was die Aufgabe verlangt. Vor den Innenangriff kommt " +
            "der Atemschutz. Aus heißt: Status 4 ist wie früher das Ende.",
        { it.einsatzarbeit }, { Einstellungsaenderung(einsatzarbeit = it) },
    ),
    Regel(
        "einsatzleitung", Regelgruppe.Vorort, "Einsatzleitung vor Ort",
        "Ab vier alarmierten Fahrzeugen übernimmt ein Führungsfahrzeug die Lage: " +
            "Abschnitte bilden, Kräfte zuteilen, Nachforderungen bündeln — und am Ende die " +
            "Abschlussmeldung geben. Aus heißt: Es rückt kein ELW aus, und jede Besatzung " +
            "meldet sich selbst bei der Leitstelle.",
        { it.einsatzleitung }, { Einstellungsaenderung(einsatzleitung = it) },
    ),
)

/** Die Schalter, die in dieser Runde überhaupt zur Wahl stehen. */
fun regelnImModus(s: Rundeneinstellungen): List<Regel> =
    if (s.mode == "Ausbildung") REGELN.filter { it.auchInAusbildung } else REGELN

/** „6 von 12 an" — gezählt über die Schalter, die es in diesem Modus gibt. */
fun regelstand(s: Rundeneinstellungen): Pair<Int, Int> {
    val regeln = regelnImModus(s)
    return regeln.count { it.an(s) } to regeln.size
}

// ------------------------------------------------------------- Beschriftungen

internal val EINSATZDICHTE_LABEL = mapOf("Ruhig" to "Ruhig", "Normal" to "Normal", "Dicht" to "Dicht")
internal val STOERUNG_LABEL = mapOf("Aus" to "Aus", "Selten" to "Selten", "Gelegentlich" to "Gelegentlich")
internal val JAHRESZEIT_LABEL =
    mapOf("Fruehling" to "Frühling", "Sommer" to "Sommer", "Herbst" to "Herbst", "Winter" to "Winter")
internal val BOT_TEMPO_LABEL = mapOf("Gemuetlich" to "Gemütlich", "Normal" to "Normal", "Zuegig" to "Zügig")
internal val BOT_GESPRAECHIGKEIT_LABEL = mapOf("Knapp" to "Knapp", "Normal" to "Normal", "Gespraechig" to "Gesprächig")
internal val BOT_GESPRAECHIGKEIT_HINWEIS = mapOf(
    "Knapp" to "Nur Quittungen und Pflichtmeldungen.",
    "Normal" to "Gelegentliche Eigenmeldungen, Rückfragen wie immer.",
    "Gespraechig" to "Bots melden sich auch von sich aus und fragen nach.",
)
internal val ARBEITSFUNK_LABEL = mapOf("NurEinsatzleitung" to "Nur die Einsatzleitung", "AlleBesatzungen" to "Alle Besatzungen")
internal val ARBEITSFUNK_HINWEIS = mapOf(
    "NurEinsatzleitung" to "Nur die führende Einheit meldet sich von sich aus — Lage, Nachforderung, Abschluss. " +
        "Alle anderen arbeiten still; ihre Aufgaben stehen in der Chronologie. Angesprochen " +
        "antwortet natürlich jede Besatzung.",
    "AlleBesatzungen" to "Jede Besatzung meldet sich von sich aus — Lage auf Sicht und jede fertige Aufgabe. " +
        "Immer angemeldet über den Sprechwunsch.",
)
internal val EINSATZENDE_LABEL = mapOf("Selbsttaetig" to "Selbsttätig", "NachFreigabe" to "Nach Freigabe")
internal val EINSATZDICHTE_HINWEIS = mapOf(
    "Ruhig" to "Alle drei Minuten eine Lage, höchstens zwei offen. Für die erste Schicht allein.",
    "Normal" to "Alle zwei Minuten eine Lage, höchstens drei offen. Allein und geübt.",
    "Dicht" to "Alle anderthalb Minuten eine Lage, höchstens vier offen. Für die Leitstelle zu zweit.",
)
internal val STOERUNG_HINWEIS = mapOf(
    "Aus" to "Jede Alarmierung ist echt, jedes Fahrzeug fährt durch.",
    "Selten" to "Etwa einmal je Stunde kommt etwas dazwischen.",
    "Gelegentlich" to "Mehrmals je Stunde — für Schichten, die fordern sollen.",
)
internal val EINSATZENDE_HINWEIS = mapOf(
    "Selbsttaetig" to "Auf die Abschlussmeldung hin rücken alle Kräfte ein, der Einsatz schließt sich.",
    "NachFreigabe" to "Die Kräfte bleiben, bis du die Abschlussmeldung quittierst oder abschließt.",
)

/** „1/83/1" mit den eingestellten Stellen — „01/83/1" sagt mehr als „Stellen: 2". */
internal fun rufnamenBeispiel(s: Rundeneinstellungen): String {
    fun stellen(n: Int) = "1".padStart(n.coerceIn(1, 3), '0')
    return "${stellen(s.wachennummerStellen)}/83/${stellen(s.laufnummerStellen)}"
}
