package de.pagerspass.pagerspass.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Die Staffeln — Abstand, Rundung, Zielfläche, Erhebung, Dauer.
 *
 * Übertragen aus `web/src/styles/base.css` und `web/src/styles/mobil.css`. Wo
 * die Handy-Schicht einen Wert des Rechners überschreibt, gilt hier der Wert
 * der Handy-Schicht und der Grund steht dabei — diese App *ist* der Handy-Zweig.
 *
 * <b>Warum Staffeln und keine freien Zahlen.</b> Im Web standen für dieselbe
 * Sache 113 Schattenangaben auf 80 Werten, 116 Zeilenhöhen auf 19 Werten und
 * sieben Dauern für einen Übergang. Das ist nie eine Gestaltungsentscheidung
 * gewesen, sondern die Zahl, die beim Abschreiben gerade danebenlag. Wer hier
 * eine Zahl direkt hinschreibt statt eine Stufe zu nehmen, fängt das von vorn an.
 */

/**
 * Die Abstände.
 *
 * Sechs Stufen, jede mit einer Aufgabe. Wer eine siebte braucht, meint meistens
 * eine der sechs.
 */
object Abstand {
    /** Rand an Rand: Marken, Punkte, Trennstriche. */
    val Haar = 2.dp

    /** Innerhalb einer Zeile: Symbol und Wort. */
    val Winzig = 4.dp

    /** Zwischen zwei Zeilen derselben Sache. */
    val Klein = 8.dp

    /** Der Regelfall: Innenraum eines Kastens, Gitterfuge. */
    val Normal = 12.dp

    /** Zwischen zwei Kästen. */
    val Gross = 16.dp

    /** Zwischen zwei Abschnitten einer Seite. */
    val SehrGross = 24.dp
}

/**
 * Die Rundungen.
 *
 * Vier Stufen nach dem, was gerundet wird — nicht nach Geschmack.
 */
object Rundung {
    /** Kästen, Karten, Dialoge. */
    val Normal = RoundedCornerShape(14.dp)

    /** Knöpfe, Felder. */
    val Klein = RoundedCornerShape(9.dp)

    /** Was in einer Zeile sitzt: Marken, Punkte, Balken. */
    val Winzig = RoundedCornerShape(6.dp)

    /** Pillen und Kreise. */
    val Rund = RoundedCornerShape(999.dp)

    /** Die Tableiste: oben gerundet, unten am Bildschirmrand bündig. */
    val LeisteOben = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)

    /** Ein Kopf, der klebt: unten gerundet, oben am Rand bündig. */
    val KopfUnten = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
}

/**
 * Die Zielflächen — was der Daumen treffen muss.
 *
 * <b>44 ohne Ausnahme.</b> Am Rechner gibt es eine kompakte Stufe von 36 Punkten
 * für dichte Leisten, in denen ein Mauszeiger zeigt. Am Daumen gilt die
 * 44er-Marke ohne Ausnahme, also fällt die Ausnahme hier weg: `Kompakt` *ist*
 * `Normal`. Ein Knopf, der im Web `.knopf--kompakt` trägt, ist hier einfach ein
 * Knopf — er behält nur die kleinere Schrift und das engere Polster.
 */
object Ziel {
    /** Die Marke: alles, was man antippt, ist mindestens so hoch. */
    val Normal = 44.dp

    /**
     * Eingabefelder — zwei Punkte mehr.
     *
     * Nicht aus Gestaltung: Ein Feld trägt seinen Text auf der Grundlinie, ein
     * Knopf zentriert ihn. Bei gleicher Höhe sieht das Feld flacher aus.
     */
    val Feld = 46.dp

    /** Am Handy keine eigene Stufe — siehe oben. */
    val Kompakt = Normal
}

/**
 * Die Marken — die kleinen Zahlen an einem Weg.
 */
object Marke {
    /**
     * Der Kreis mit einer Zahl darin.
     *
     * 20 und nicht 17: Die Zahl steht auf der kleinsten Stufe der Schriftstaffel,
     * und ein Kreis, der seiner Zahl gerade eben passt, sieht aus wie ein
     * Druckfehler.
     */
    val Zahl = 20.dp

    /** Der größere Klumpen — Wappen, Profilbild in einer Zeile. */
    val Klumpen = 28.dp
}

/**
 * Was die Seite außen zusammenhält.
 */
object Mass {
    /** Die Höhe einer Kopfzeile. */
    val Kopfhoehe = 56.dp

    /**
     * Die Höhe der Tableiste am unteren Rand.
     *
     * Sie steht hier und nicht in `Tableiste.kt`, weil sich mehr als die Leiste
     * danach richtet — Meldungen und schwebende Knöpfe müssen wissen, wo der
     * Boden aufhört. Im Web stand die Zahl aus genau demselben Grund in
     * `styles/mobil.css` und nicht in der Komponente; eine abgeschriebene 72 in
     * der Meldung blieb dort kleben, als die Leiste ihre Höhe änderte.
     */
    val LeisteHoehe = 70.dp

    /** Was man wirklich liest — Recht, Wiki: schmaler als der Bildschirm. */
    val Lesebreite = 860.dp

    /** Listen laufen breiter, aber nicht unbegrenzt. */
    val Listenbreite = 1120.dp
}

/**
 * Die Ebenen.
 *
 * Compose stapelt nach Reihenfolge im Baum, nicht nach Zahl — diese Werte sind
 * deshalb kein `z-index`, sondern die *Ordnung*, in der der Rahmen seine Teile
 * setzt (siehe `mobil/PagerSpassApp.kt`). Sie stehen trotzdem als Zahlen da,
 * damit die Reihenfolge nachlesbar ist und nicht aus der Schachtelung erraten
 * werden muss.
 */
object Ebene {
    /** Köpfe und Leisten, die beim Rollen stehen bleiben. */
    const val KLEBEND = 5f

    /** Bedienung auf einer Karte: Knöpfe über dem Kartenbild. */
    const val AUFSATZ = 500f

    /** Überlagerung und Kasten — jeder Dialog, ohne Ausnahme. */
    const val DIALOG = 800f

    /** Was die App über allem sagt: Verbindung, Hinweise. */
    const val MELDUNG = 900f

    /** Was den Dienst unterbricht: Melder, Alarmbild. */
    const val ALARM = 950f

    /** Muss beantwortet sein, bevor überhaupt etwas geht. */
    const val ZUSTIMMUNG = 1100f
}

/**
 * Wie lange etwas braucht.
 *
 * Zwei Dauern, nicht sieben. `Wechsel` ist die Rückmeldung auf eine Berührung —
 * sie muss unter der Schwelle bleiben, ab der man auf sie wartet. `WechselLang`
 * ist für das, was sich wirklich bewegt: ein Balken, der wächst, eine Karte, die
 * nachführt.
 */
object Dauer {
    const val WECHSEL = 120
    const val WECHSEL_LANG = 220
}

/**
 * Die Erhebung — wie hoch etwas über seiner Umgebung liegt.
 *
 * <b>Warum eine Erhebung auf dunklem Grund zwei Teile hat.</b> Auf hellem Grund
 * genügt der Schatten. Auf dunklem verschluckt der Grund ihn — ein schwarzer
 * Schatten auf #080b10 ist fast nichts. Was die Höhe hier trägt, ist die
 * **Lichtkante oben**: die Kante, die dem Licht zugewandt ist. Sie steht deshalb
 * als eigener Modifikator daneben (`Modifier.lichtkante()`) und nicht in jeder
 * Stufe mit drin — nicht jede erhobene Fläche will sie (ein Balken schon, ein
 * Menü nicht).
 *
 * Die Zahlen sind die Umrechnung der CSS-Schatten in die eine Zahl, die Compose
 * dafür kennt: `0 2px 6px rgb(0 0 0 / 34%)` wird zu 4 dp. Das ist keine exakte
 * Übertragung — Compose zeichnet einen Schatten nach Materialregeln, CSS nach
 * Streuung und Deckkraft. Erhalten bleibt, worauf es ankommt: die *Reihenfolge*
 * der fünf Stufen und ihr Abstand zueinander.
 */
object Erhebung {
    /** Liegt auf: Marke, Pille, Zeile einer Liste. */
    val Ruht = 1.dp

    /** Ist anfassbar: Knopf, Karte im Ruhezustand. */
    val Hebt = 4.dp

    /** Löst sich ab: Menü, Aufsatz auf der Karte. */
    val Schwebt = 12.dp

    /** Liegt über der Seite: jeder Dialog. */
    val Dialog = 20.dp

    /** Liegt über allem: Melder, Alarmbild. */
    val Alarm = 32.dp

    /**
     * Und die sechste, die nach oben wirft.
     *
     * Eine Leiste, die am unteren Rand klebt, steht nicht *über* der Seite,
     * sondern *vor* ihr — der Inhalt rollt darunter durch. Ihr Schatten fällt
     * deshalb gegen die Rollrichtung.
     */
    val Leiste = 16.dp
}
