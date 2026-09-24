package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable

/**
 * Die Rümpfe des mobilen Begleiters — Spiegel von `Services/BegleiterVerbindungen.cs`
 * und `BegleiterJoinResult` in `Hubs/GameHub.cs`.
 */

/**
 * Der Gerätestand des Rechners, den das Handy spiegelt.
 *
 * <b>Es ist eine Spiegelung, keine Wahl.</b> Bauart, Bauform, Gehäuse, Ton und
 * Gesicht stellt man im Konto ein; der Begleiter zeigt, was dort steht — und
 * zwar auch dann, wenn mitten in der Schicht umgestellt wird (Ereignis
 * `BegleiterGeraete`). Ein Dienstbildschirm ist kein Einstellungsmenü.
 *
 * <b>Alles ist `null`-fähig, und das ist Absicht.</b> Ein älterer Client am
 * Rechner schickt beim Erzeugen des Tokens gar keinen Rumpf; dann gilt überall
 * das Dienstgerät. Reine Zeichenketten ohne Prüfung — was die App nicht kennt,
 * fällt auf die Vorgabe zurück, statt die Kopplung scheitern zu lassen.
 */
@Serializable
data class Begleitergeraete(
    /** Welches der sieben Gehäuse: `dienst`, `klassik`, `farbe`, `fax`, `uhr`, `wand`, `monitor`. */
    val bauform: String? = null,
    /** Das Funkgerät im Fahrzeug am Rechner — hier nur der Vollständigkeit halber. */
    val funkgeraet: String? = null,
    /** Die Ausführung des Handfunkgeräts auf dem Begleiter. */
    val begleiterFunkgeraet: String? = null,
    val melderton: String? = null,
    /** Das Meldergesicht — die Anzeige im Gehäuse. */
    val gesicht: String? = null,
    /**
     * Piepser oder Alarm-App: `dme` oder `app`.
     *
     * <b>Sie entscheidet die halbe Ansicht.</b> Wer am Rechner auf „App" steht,
     * soll am Handy nicht plötzlich einen DME in der Hand halten — das war die
     * eine Stelle, an der Rechner und Handy zwei sichtbar verschiedene Melder
     * zeigten.
     */
    val bauart: String? = null,
) {
    /** Ob der Melder als Alarm-App auftritt statt als Piepser. */
    val alsApp: Boolean get() = bauart == "app"

    /** Ob der Alarm auf dem Funkgerät steht — die dritte Bauart, ohne eigenen Melder. */
    val imFunk: Boolean get() = bauart == "funk"

    /**
     * Ob überhaupt ein Gehäuse hinzustellen ist.
     *
     * Bei „App" und bei „Im Funk" gibt es keines. Die Blende über allem zeigt den Alarm
     * in beiden Fällen (siehe `PagerSpassApp`), nur die ruhende Fläche darunter darf
     * dann keinen Piepser zeigen, den der Rechner gar nicht hat.
     */
    val ohneGehaeuse: Boolean get() = alsApp || imFunk

    /**
     * Ob das Gehäuse quer steht.
     *
     * Wachalarm und Alarmmonitor sind keine Handgeräte — der eine hängt an der
     * Wand, der andere im Flur. Sie dürfen die ganze Breite haben.
     */
    val querformat: Boolean get() = bauform == "wand" || bauform == "monitor"

    /** Wie das Gehäuse heißt, wenn ein Mensch es liest. */
    val bauformName: String
        get() = when (bauform) {
            "klassik" -> "Klassik-Melder"
            "farbe" -> "Farbmelder"
            "fax" -> "Alarmfax"
            "uhr" -> "Einsatzuhr"
            "wand" -> "Wachalarm"
            "monitor" -> "Alarmmonitor"
            else -> "Dienstmelder"
        }
}

/**
 * Die Antwort auf `JoinBegleiter` — Spiegel von `BegleiterJoinResult`.
 *
 * `playerId` ist der **Platz**, an dem dieses Gerät hängt, nicht das Konto auf
 * dem Handy: Der Begleiter handelt unter der Kennung des Rechnerplatzes, und
 * die Engine prüft an jedem Kommando, ob dieser Platz das Fahrzeug besitzt.
 */
@Serializable
data class Begleiterbeitritt(
    val ok: Boolean = false,
    val state: Raumzustand? = null,
    val playerId: String? = null,
    val name: String? = null,
    val fehler: String? = null,
    /**
     * Der Ersteintrag des Gerätestands, mehr nicht — ab hier hält
     * `BegleiterGeraete` ihn nach.
     */
    val geraete: Begleitergeraete? = null,
)
