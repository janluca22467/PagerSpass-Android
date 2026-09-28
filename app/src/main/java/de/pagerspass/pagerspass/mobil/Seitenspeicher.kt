package de.pagerspass.pagerspass.mobil

import de.pagerspass.pagerspass.netz.Abzeichen
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Garage
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.Profilbildstand
import de.pagerspass.pagerspass.netz.Betreibermitteilung
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.netz.Landkreis
import de.pagerspass.pagerspass.netz.Schicht
import de.pagerspass.pagerspass.netz.Shop

/**
 * Was die sechs Seiten anzeigen.
 *
 * <b>Getrennt vom `Sitzungsstand`</b>, und das hat einen Grund, der erst unter
 * Last sichtbar wird: Der Sitzungsstand ändert sich selten (Anmeldung, Konto,
 * Fehler); diese Listen ändern sich bei jedem Öffnen einer Seite. Lägen beide in
 * einem Objekt, zeichnete jede geladene Freundesliste den ganzen Rahmen samt
 * Tableiste neu.
 *
 * <b>Jeder Bereich hat seinen eigenen Ladezustand und seinen eigenen Fehler.</b>
 * Ein Fehler beim Shop darf das Dienstbuch nicht als kaputt erscheinen lassen,
 * und ein ladender Shop keine Meldung über die ganze App legen. Im Web ist das
 * dieselbe Aufteilung — dort ein Speicher je Bereich.
 *
 * <b>`geladen` ist nicht `liste.isEmpty()`.</b> Eine leere Liste kann heißen
 * „noch nicht geholt" oder „es gibt nichts" — und nur im zweiten Fall darf der
 * gestrichelte Leerhinweis stehen. Ohne diese Unterscheidung blitzt auf jeder
 * Seite kurz „Noch nichts im Buch" auf, bevor die Einträge kommen.
 */
data class Seitenstand(
    val buch: Bereich<Buchdaten> = Bereich(),
    val freunde: Bereich<List<Freund>> = Bereich(),
    val shop: Bereich<Shop?> = Bereich(),
    val wache: Bereich<Wachendaten> = Bereich(),
    val garage: Bereich<Garagendaten> = Bereich(),
    /**
     * Der Katalog — einmal geholt und behalten.
     *
     * Er ist für jede Runde gleich und mit 401 Kreisen und allen Fahrzeugen das
     * größte, was die App lädt. Ihn bei jedem Öffnen des Startbildschirms neu zu
     * holen wäre die teuerste Gewohnheit, die man sich hier angewöhnen kann.
     */
    val katalog: Bereich<Katalog?> = Bereich(),
    /** Das eigene Profil — Schmuck, Vorstellung, Vitrine. */
    val profil: Bereich<Profil?> = Bereich(),
    val mitteilungen: Bereich<List<Betreibermitteilung>> = Bereich(),
    /** Welche Betreibermitteilungen weggeklickt sind — nur auf diesem Gerät. */
    val mitteilungenGelesen: Set<String> = emptySet(),
    /** Offene Rundeneinladungen — die Karten auf dem Start. */
    val einladungen: Bereich<List<de.pagerspass.pagerspass.netz.Einladung>> = Bereich(),
    /** Unbestätigte Verwarnungen — Blende bis zur Kenntnisnahme. */
    val verwarnungen: List<de.pagerspass.pagerspass.netz.Verwarnung> = emptyList(),
    /** Unbestätigte Nachrichten der Verwaltung — ebenso. */
    val adminNachrichten: List<de.pagerspass.pagerspass.netz.AdminNachricht> = emptyList(),
    /** Runden, die gerade Verstärkung suchen. */
    val oeffentlicheRunden: Bereich<List<de.pagerspass.pagerspass.netz.OeffentlicheRunde>> =
        Bereich(),
    /** Der Stand der Schicht des Tages. */
    val tagesschicht: Bereich<de.pagerspass.pagerspass.netz.Tagesschicht?> = Bereich(),
    /** Der volle Blick auf die eigene Wache. */
    val wacheDetail: Bereich<de.pagerspass.pagerspass.netz.GemeinschaftDetail?> = Bereich(),
    /** Die eigenen offenen Bewerbungen und Einladungen. */
    val wachenantraege: Bereich<List<de.pagerspass.pagerspass.netz.Gemeinschaftsantrag>> =
        Bereich(),
    val profilbild: Bereich<Profilbildstand?> = Bereich(),
)

/**
 * Ein Bereich: der Inhalt, ob er geholt wird, ob er da ist, und was schiefging.
 *
 * @param inhalt Was gezeigt wird. Vor dem ersten Laden die leere Vorgabe.
 * @param laedt Ob gerade geholt wird. Beim **ersten** Mal zeigt die Seite dann
 *   nichts; beim Nachladen bleibt der alte Inhalt stehen, damit die Seite unter
 *   dem Finger nicht wegspringt.
 * @param geladen Ob überhaupt schon einmal eine Antwort kam — siehe oben.
 * @param fehler Der Satz des Servers, wenn es einen gab.
 */
data class Bereich<T>(
    val inhalt: T? = null,
    val laedt: Boolean = false,
    val geladen: Boolean = false,
    val fehler: String? = null,
) {
    /** Ob die Seite jetzt einen Ladezustand zeigen soll statt eines leeren Kastens. */
    val ersteLadung: Boolean get() = laedt && !geladen
}

/** Was das Dienstbuch braucht: die Schichten und die Meilensteine. */
data class Buchdaten(
    val schichten: List<Schicht> = emptyList(),
    val abzeichen: List<Abzeichen> = emptyList(),
)

/**
 * Was die Wachenseite braucht.
 *
 * <b>Entweder die eigene Gemeinschaft oder die Suche</b> — nie beides. Wer in
 * einer ist, will hinein; wer in keiner ist, zur Liste. Dieselbe Weiche wie in
 * der Tableiste des Webs.
 */
data class Wachendaten(
    val eigene: Gemeinschaft? = null,
    val offene: List<Gemeinschaft> = emptyList(),
)

/**
 * Was die Garage braucht.
 *
 * Der Fuhrpark nennt nur Ids (`fahrzeuge: ["hlf20", …]`); wie ein Fahrzeug
 * heißt, steht im Katalog. Deshalb werden hier beide zusammengeführt — die
 * Ansicht soll nicht zwei Quellen zusammenrechnen müssen.
 */
data class Garagendaten(
    val stand: Garage = Garage(),
    val fahrzeuge: List<Fahrzeugzeile> = emptyList(),
    /**
     * Was man noch nicht hat — das Autohaus.
     *
     * <b>Nur, was einen Preis hat.</b> Der Server rechnet die Preisliste je
     * Konto (`Garage.preise`); ein Bauplan, der dort nicht vorkommt, ist für
     * dieses Konto nicht zu haben, und ihn mit „—" anzubieten wäre eine
     * Einladung ins Leere.
     */
    val angebot: List<Fahrzeugzeile> = emptyList(),
)

/** Ein Fahrzeug im eigenen Fuhrpark, mit dem, was der Katalog dazu weiß. */
data class Fahrzeugzeile(
    val id: String,
    val typ: String,
    val beschreibung: String,
    val besatzung: String,
    val organisation: String,
    val preis: Int? = null,
    /** Das heutige Angebot — sein Preis ist bereits der ermäßigte. */
    val tagesangebot: Boolean = false,
    val regulaer: Int? = null,
)

/**
 * Die Landkreise, wie die Auswahl sie zeigt.
 *
 * <b>Kreise mit echten Daten zuerst, und ausdrücklich beschriftet.</b> Die Liste
 * führt alle 401, echte Wachen- und Straßendaten liegen aber nur für einen Teil
 * vor. Wer blind einen aussucht, landet im erfundenen Standardbereich und liest
 * den Hinweis darauf wie eine Fehlermeldung. Eine stillschweigend umsortierte
 * Liste sähe aus wie eine alphabetische, in der ein paar Namen an der falschen
 * Stelle stehen — deshalb die zwei Gruppen.
 */
fun List<Landkreis>.nachBundesland(bundesland: String): Pair<List<Landkreis>, List<Landkreis>> {
    val imLand = filter { it.bundesland == bundesland }.sortedBy { it.name }
    return imLand.filter { it.hatDaten } to imLand.filterNot { it.hatDaten }
}

/** Alle Bundesländer, die im Katalog vorkommen — in der Reihenfolge des Alphabets. */
fun List<Landkreis>.bundeslaender(): List<String> =
    map { it.bundesland }.filter { it.isNotBlank() }.distinct().sorted()
