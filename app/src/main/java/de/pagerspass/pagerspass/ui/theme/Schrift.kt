package de.pagerspass.pagerspass.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Die Schriftstaffel — übertragen aus `base.css`, in der Fassung der Handy-Schicht.
 *
 * <b>Zwei Familien.</b> Serifenlos für alles, was man liest; Monospace überall
 * dort, wo im echten Leben auch Technik schreibt — Funk, Status, Melder,
 * Kennungen, Zeiten. Beide sind Systemschriften: Im Web stehen sie als
 * `system-ui` und `ui-monospace`, unter Android sind das Roboto und Roboto Mono.
 * Es liegt **keine Schriftdatei im Paket** — nichts zu lizenzieren, nichts, das
 * die App größer macht.
 *
 * <b>Die drei unteren Stufen sind am Handy größer als am Rechner.</b> Die
 * Rechnerstrecke prüft seit jeher, dass nichts unter 12 Punkt gesetzt ist; die
 * Handystrecke prüfte das nie. Nachgemessen auf einem 390 × 844 trug 12 Punkt
 * die halbe Oberfläche — alle Etiketten, die Reiter, die Marken und die
 * Beschriftung der ganzen Tableiste. Deshalb hier 13 / 14,5 / 16 statt
 * 12 / 13,5 / 15. Die Stufen bleiben unterscheidbar, der Abstand nach oben
 * (17 Punkt) bleibt bestehen.
 *
 * <b>Warum `sp` und nicht `dp`.</b> Weil die Systemeinstellung „Schriftgröße"
 * mitgehen muss. Das ist unter Android das Gegenstück zu dem, was im Web der
 * Zoom tut, und es ist eine Bedienungshilfe, keine Störung. Was dabei nicht
 * mitwachsen darf — Zielflächen, Zeichen, Marken —, steht in `Masse.kt` in `dp`.
 */
object Schrift {

    /** Was man liest. Unter Android: Roboto. */
    val Sans = FontFamily.Default

    /**
     * Was Technik schreibt: Funk, Status, Melder, Kennungen, Zeiten, Raumcodes.
     *
     * Im Web heißt die Klasse `.mono` und steht an über hundert Stellen. Sie ist
     * keine Verzierung — eine Fahrzeugkennung in Proportionalschrift springt
     * beim Zählen, und im Funkprotokoll fluchten die Zeiten nicht mehr.
     */
    val Mono = FontFamily.Monospace

    // -------------------------------------------------------- Die Zeilenhöhen
    //
    // Fünf Stufen. Die unterste ist keine Zeilenhöhe im eigentlichen Sinn,
    // sondern die Ansage „hier bricht nichts um": eine Zahl in einem Kreis, ein
    // Symbol in einem Knopf. Sie war im Web die meistbenutzte von allen.

    /** Bricht nicht um: Symbol, Zahl in einem Kreis, Statuspunkt. */
    const val ZEILE_BLOCK = 1.0f

    /** Große Schrift, die trotzdem zusammenhält: Schlagzeile, Kennzahl. */
    const val ZEILE_ENG = 1.2f

    /** Überschrift, Knopfaufschrift, Marke. */
    const val ZEILE_KNAPP = 1.35f

    /** Der Regelfall: Listen, Kästen, Nebenangaben. */
    const val ZEILE = 1.5f

    /** Was man wirklich liest: Recht, Wiki, lange Hinweise. */
    const val ZEILE_WEIT = 1.7f

    // ---------------------------------------------------------- Die Größen
    //
    // Sie stehen einzeln da, weil nicht jede Stelle einen fertigen Stil will:
    // Wer eine Zeile aus drei Teilen baut, braucht die Zahl, nicht den Stil.

    val WINZIG = 13.sp
    val KLEIN = 14.5.sp
    val NORMAL = 16.sp
    val GROSS = 17.sp
    val TITEL = 21.sp
    val SCHLAGZEILE = 26.sp
    val ANZEIGE = 34.sp

    /**
     * Die Größe in einem Eingabefeld.
     *
     * Sie ist im Web ausdrücklich von `NORMAL` getrennt, weil iOS beim Antippen
     * eines Feldes unter 16 Punkt in die Seite zoomt. Unter Android gibt es
     * diesen Zwang nicht — der Wert bleibt trotzdem eigenständig, damit die
     * beiden Zweige an dieser Stelle nicht auseinanderlaufen.
     */
    val EINGABE = 16.sp

    /** Der Schriftzug über dem Login — die eine große Zeile der Anmeldung. */
    val SCHRIFTZUG = 40.sp

    // ------------------------------------------------------------ Die Stile
    //
    // Was oben die Zahl ist, ist hier der fertige Stil. Farbe steht bewusst
    // nicht drin: Die trägt die Stelle, an der der Text sitzt.

    /** Marken, Kürzel, Zahlen in engen Kästen. */
    val Winzig = TextStyle(fontFamily = Sans, fontSize = WINZIG, lineHeight = WINZIG * ZEILE)

    /** Nebenangaben, Zeiten, Statuszeilen. */
    val Klein = TextStyle(fontFamily = Sans, fontSize = KLEIN, lineHeight = KLEIN * ZEILE)

    /** Fließtext in Listen und Kästen — der Regelfall. */
    val Normal = TextStyle(fontFamily = Sans, fontSize = NORMAL, lineHeight = NORMAL * ZEILE)

    /** Was man wirklich liest: Recht, Wiki, lange Hinweise. */
    val Lesetext = TextStyle(fontFamily = Sans, fontSize = NORMAL, lineHeight = NORMAL * ZEILE_WEIT)

    /** Überschriften innerhalb einer Seite. */
    val Gross = TextStyle(
        fontFamily = Sans,
        fontSize = GROSS,
        fontWeight = FontWeight.SemiBold,
        lineHeight = GROSS * ZEILE_KNAPP,
    )

    /** Name eines Kastens, einer Karte, eines Dialogs. */
    val Titel = TextStyle(
        fontFamily = Sans,
        fontSize = TITEL,
        fontWeight = FontWeight.Bold,
        lineHeight = TITEL * ZEILE_KNAPP,
    )

    /** Kopfzeile einer Ansicht, Stichwort, Kennzahl. */
    val Schlagzeile = TextStyle(
        fontFamily = Sans,
        fontSize = SCHLAGZEILE,
        fontWeight = FontWeight.Bold,
        lineHeight = SCHLAGZEILE * ZEILE_ENG,
    )

    /** Die eine große Zahl: Melder, Alarmbild, Raumcode. */
    val Anzeige = TextStyle(
        fontFamily = Mono,
        fontSize = ANZEIGE,
        fontWeight = FontWeight.Bold,
        lineHeight = ANZEIGE * ZEILE_BLOCK,
        letterSpacing = 0.04.em,
    )

    /** Die Aufschrift eines Knopfes. */
    val Knopf = TextStyle(
        fontFamily = Sans,
        fontSize = NORMAL,
        fontWeight = FontWeight.SemiBold,
        lineHeight = NORMAL * ZEILE_KNAPP,
    )

    /**
     * Das Etikett über einem Abschnitt — „SCHICHTEN", „AUSRÜSTUNG", „BESATZUNG".
     *
     * Versalien und Sperrung sind hier Bedeutung, nicht Schmuck: Das Etikett
     * ist keine Überschrift, die man liest, sondern eine Beschriftung, die man
     * im Vorbeisehen erfasst. Der Text wird an der Stelle großgeschrieben, an
     * der er gesetzt wird (`Etikett`-Baustein), nicht hier.
     */
    val Etikett = TextStyle(
        fontFamily = Sans,
        fontSize = WINZIG,
        fontWeight = FontWeight.Bold,
        lineHeight = WINZIG * ZEILE_KNAPP,
        letterSpacing = 0.05.em,
    )

    /** Was Technik schreibt — in klein. */
    val MonoKlein = TextStyle(fontFamily = Mono, fontSize = KLEIN, lineHeight = KLEIN * ZEILE)

    /** Was Technik schreibt — im Regelfall. */
    val MonoNormal = TextStyle(fontFamily = Mono, fontSize = NORMAL, lineHeight = NORMAL * ZEILE)

    /** Die Zahl in einer Marke: bricht nicht um, zentriert im Kreis. */
    val MarkeZahl = TextStyle(
        fontFamily = Mono,
        fontSize = WINZIG,
        fontWeight = FontWeight.Bold,
        lineHeight = WINZIG * ZEILE_BLOCK,
        // Ohne diese beiden sitzt eine einstellige Zahl in einem 20-Punkt-Kreis
        // sichtbar zu tief: Compose stellt sonst über der Zahl den vollen
        // Schriftraum frei, und der ist bei Mono höher als die Ziffer.
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )

    /** Die Aufschrift eines Weges in der Tableiste. */
    val Weg = TextStyle(
        fontFamily = Sans,
        fontSize = WINZIG,
        fontWeight = FontWeight.W600,
        lineHeight = WINZIG * ZEILE_BLOCK,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )
}
