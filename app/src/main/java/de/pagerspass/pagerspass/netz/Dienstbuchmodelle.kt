package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Modelle der sechs Dienstbuch-Seiten — übertragen aus `web/src/types.ts`.
 *
 * <b>Sekunden sind hier Kommazahlen.</b> Der Server rechnet Hilfsfristen,
 * Ausrückzeiten und Status-6-Zeiten als `double` (Mittelwerte über Einsätze) und
 * schickt sie auch so: `423.5`. Ein `Int` an dieser Stelle wirft beim Einlesen —
 * und weil das Dienstbuch seine Nebenlisten still auffängt, stand die Seite dann
 * leer da, ohne dass irgendwo ein Fehler erschien. Genau das ist mit
 * `Schicht.hilfsfristSekunden` passiert; deshalb liest das Dienstbuch die
 * Chronik jetzt über `Dienstschicht`.
 *
 * Wie überall: jedes Feld mit Vorgabe, damit ein älterer oder neuerer Server die
 * Seite nicht zum Absturz bringt.
 */

// ------------------------------------------------------------------ Archiv

/** Eine Zeile der Archivübersicht — Grundlage der Schichtenliste. */
@Serializable
data class ArchivEintrag(
    val code: String = "",
    val ort: String = "",
    val leitstelle: String = "",
    val landkreis: String? = null,
    val mode: String = "",
    val gestartetUm: String? = null,
    val beendetUm: String? = null,
    val einsaetze: Int = 0,
    val abgeschlosseneEinsaetze: Int = 0,
    val spieler: Int = 0,
    val fahrzeuge: Int = 0,
    val funksprueche: Int = 0,
)

/**
 * Eine gebuchte Schicht aus der Chronik — dasselbe wie `Schicht`, nur mit der
 * Hilfsfrist als Kommazahl (siehe oben).
 */
@Serializable
data class Dienstschicht(
    val roomCode: String = "",
    val beendetUm: String = "",
    val ort: String = "",
    val landkreis: String? = null,
    val rolle: String = "",
    val funkrufname: String? = null,
    val fahrzeugtyp: String? = null,
    val einsaetze: Int = 0,
    val hilfsfristSekunden: Double? = null,
    val punkte: Int = 0,
)

/** Die besten je erreichten Werte — neben den Durchschnitten der Bilanz. */
@Serializable
data class Schichtrekorde(
    val schnellsteAusrueckzeitSekunden: Double? = null,
    val kuerzesteHilfsfristSekunden: Double? = null,
    /** Das Stichwort, zu dem die kürzeste Hilfsfrist gehört. */
    val kuerzesteHilfsfristStichwort: String? = null,
    val meisteLagemeldungenSchicht: Int = 0,
)

/** Was ein Mitspieler über eine oder mehrere Schichten zusammengetragen hat. */
@Serializable
data class Schichtbilanz(
    val playerId: String = "",
    val name: String = "",
    val rolle: String = "",
    val runden: Int = 0,
    val einsaetze: Int = 0,
    val lagemeldungen: Int = 0,
    val funksprueche: Int = 0,
    /** Ø Zeit vom Alarm bis Status 3; `null`, solange nie ausgerückt wurde. */
    val ausrueckzeitSekunden: Double? = null,
    /** Gesamtzeit im Status 6. */
    val ausserDienstSekunden: Double = 0.0,
    val rekorde: Schichtrekorde = Schichtrekorde(),
)

/** Eine Runde als Punkt im Verlauf. */
@Serializable
data class RundenKennzahl(
    val code: String = "",
    val ort: String = "",
    val beendetUm: String? = null,
    val einsaetze: Int = 0,
    val hilfsfristSekunden: Double? = null,
    val dispositionszeitSekunden: Double? = null,
)

/** Zahlen über die letzten Schichten — Bilanzen je Mitspieler und der Verlauf. */
@Serializable
data class ArchivStatistik(
    val runden: Int = 0,
    val spieler: List<Schichtbilanz> = emptyList(),
    /** Älteste zuerst — so, wie das Diagramm sie zeichnet. */
    val verlauf: List<RundenKennzahl> = emptyList(),
)

// ---------------------------------------------------------------- Laufbahn

/** Eine Stufe der Laufbahn. */
@Serializable
data class Laufbahnstufe(
    val level: Int = 0,
    val bezeichnung: String = "",
    val ab: Int = 0,
    /** Wie viele Fahrzeugwahlen dieser Aufstieg bringt — meist eine. */
    val fahrzeuge: Int = 0,
)

/** Ein Platz in der Zwischenwertung des laufenden Kalendermonats. */
@Serializable
data class Saisonbestenlistenplatz(
    val platz: Int = 0,
    val anzeigename: String = "",
    val punkte: Int = 0,
    val istEigenerEintrag: Boolean = false,
)

/** Wofür es in einer Schicht Punkte gab — eine Zeile unter der aufgeklappten Schicht. */
@Serializable
data class Buchungsposten(
    val grund: String = "",
    val text: String = "",
    val punkte: Int = 0,
)

/**
 * Eine Freischaltung am Konto — die eigene Ausrückeordnung, der Sandkasten.
 *
 * Sie stehen im Laufbahnpass an der Stufe, die sie öffnet. Das Konto-Modell der
 * Sitzung trägt sie nicht mit; das Dienstbuch liest sie aus derselben Antwort
 * (`GET /api/konto/{kennung}`) über `Kontofreischaltungen`.
 */
@Serializable
data class Kontofreischaltung(
    val was: String = "",
    val bezeichnung: String = "",
    val abLevel: Int = 0,
    val abRang: String = "",
    val offen: Boolean = false,
)

/** Nur die Freischaltungen aus der Kontoantwort. */
@Serializable
data class Kontofreischaltungen(
    val freischaltungen: List<Kontofreischaltung> = emptyList(),
)

/** Ein Punkt der Hilfsfristkurve eines Wachenmitglieds. */
@Serializable
data class Wachenkurvenpunkt(
    val beendetUm: String = "",
    val hilfsfristSekunden: Double? = null,
)

/** Die Kurve eines Mitglieds der eigenen Wache — für den Vergleich in der Übersicht. */
@Serializable
data class Wachenkurve(
    val anzeigename: String = "",
    val verlauf: List<Wachenkurvenpunkt> = emptyList(),
)

/**
 * Was ein Konto in einer Organisation verdient hat — reine Statistik. Es steht
 * an der Garage (`proOrganisation`) und trägt die Balken der Übersicht.
 */
@Serializable
data class Organisationsfortschritt(
    val organisation: String = "",
    val erfahrung: Int = 0,
)

// ----------------------------------------------------------------- Vitrine

/** Ein Abzeichen in der Vitrine eines Profils. */
@Serializable
data class Vitrinenstueck(
    val id: String = "",
    val titel: String = "",
    val beschreibung: String = "",
    val kategorie: String = "",
)

/**
 * Nur die Vitrine aus der Profilantwort.
 *
 * Ein eigenes Modell statt eines Feldes mehr an `Profil`: Die Abzeichenseite
 * braucht aus dem ganzen Profil genau diese Liste, und so bleibt sie unabhängig
 * davon, was die Profilseite an ihrem Modell ändert.
 */
@Serializable
data class Vitrinenprofil(
    val vitrine: List<Vitrinenstueck> = emptyList(),
)

// -------------------------------------------------------- Dienstauswertung

/** Eine Woche im Verlauf. `beginn` ist der Montag als `YYYY-MM-DD`. */
@Serializable
data class Dienstwoche(
    val beginn: String = "",
    val schichten: Int = 0,
    val einsaetze: Int = 0,
    val punkte: Int = 0,
    /** `null` heißt „keine Schicht mit Hilfsfrist" — nicht dasselbe wie null Sekunden. */
    val hilfsfristSekunden: Double? = null,
)

/** Ein Fahrzeugtyp, den dieses Konto gefahren hat, und wie oft. */
@Serializable
data class Dienstfahrzeug(
    val typ: String = "",
    val schichten: Int = 0,
    val einsaetze: Int = 0,
)

/** Die Auswertung des eigenen Dienstbuchs über alle Schichten (Premium). */
@Serializable
data class Dienstauswertung(
    val schichten: Int = 0,
    val einsaetze: Int = 0,
    val punkte: Int = 0,
    val leitstellenschichten: Int = 0,
    val fahrzeugschichten: Int = 0,
    val ersteSchicht: String? = null,
    val letzteSchicht: String? = null,
    val hilfsfristSekunden: Double? = null,
    val besteHilfsfristSekunden: Double? = null,
    /** Älteste Woche zuerst; leere Wochen stehen mit drin. */
    val verlauf: List<Dienstwoche> = emptyList(),
    val fahrzeuge: List<Dienstfahrzeug> = emptyList(),
)

// ---------------------------------------------------------------- Freunde

/** Die Antwort auf eine Freundschaftsanfrage. */
@Serializable
data class Anfrageergebnis(
    val stand: String = "",
)
