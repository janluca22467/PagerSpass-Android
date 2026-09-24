package de.pagerspass.pagerspass.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Die Farben von PagerSpass — übertragen aus `web/src/styles/base.css`.
 *
 * Vorbild ist ein abgedunkelter Leitstellenarbeitsplatz: dunkle Flächen, damit
 * Statusfarben und Alarme sofort ins Auge springen, Monospace überall dort, wo
 * im echten Leben auch Technik schreibt (Funk, Status, Melder).
 *
 * <b>Warum die Werte hier stehen und nicht in `colors.xml`.</b> Die Oberfläche
 * ist vollständig Compose; eine zweite Fassung in XML wäre ab dem ersten Tag
 * eine Fassung, die ausschert. Was das System braucht (Fensterfarbe beim Start),
 * steht in `themes.xml` und ist dort ausdrücklich als abgeschriebener Wert
 * gekennzeichnet.
 *
 * <b>Die Ordnung ist die des Vorbilds</b> — Flächen, Schrift, Akzente, die
 * hellen Fassungen der Akzente, Kanäle, Status, Organisationen. Wer eine Farbe
 * sucht, findet sie an derselben Stelle wie im Web.
 */
object Farben {

    // ------------------------------------------------------------- Flächen
    //
    // Die Abstufung ist bewusst groß: Vorher lagen Seitengrund und Kastenfläche
    // so dicht beieinander, dass ein Kasten erst an seinem Rahmen zu erkennen
    // war. Jetzt trägt jede Ebene ihren eigenen Wert.

    val Bg = Color(0xFF141518)
    val BgTief = Color(0xFF09090b)
    val Flaeche = Color(0xFF222429)
    val FlaecheHoch = Color(0xFF32343c)
    val FlaecheAktiv = Color(0xFF434650)
    val Rand = Color(0xFF333741)
    val RandHell = Color(0xFF515763)

    // -------------------------------------------------------------- Schrift
    //
    // Alle drei Stufen halten 4,5:1 auch auf `FlaecheAktiv` — der hellsten
    // Fläche, auf der in dieser Anwendung Text steht. Das war nicht immer so;
    // die Begründung steht ausführlich in base.css.

    val Text = Color(0xFFF4F8FC)
    val TextLeise = Color(0xFFCBD6E3)
    val TextSehrLeise = Color(0xFFADBBCB)

    // -------------------------------------------------------------- Akzente

    val Amber = Color(0xFFFFB020)
    val AmberTief = Color(0xFFA86C00)
    val Signal = Color(0xFFE5352B)
    val SignalTief = Color(0xFF7D1A15)
    val Gruen = Color(0xFF2F9E44)
    val Blau = Color(0xFF1C7ED6)
    val Violett = Color(0xFF8462EB)

    // ----------------------------------------- Dieselben Akzente als Schrift
    //
    // Die Akzente oben sind Füllfarben: Sie tragen einen Knopf, einen Balken,
    // einen Punkt. Als *Schrift* auf dunklem Grund fallen sie durch — Blau
    // steht gegen die Kastenfläche bei rund 2,6:1. Diese hier sind die
    // Fassungen, in denen man sie lesen kann.

    val AmberHell = Color(0xFFF0C86E)
    val BlauHell = Color(0xFF7BC0F5)
    val OrangeHell = Color(0xFFFFA448)
    val SignalHell = Color(0xFFFF9F98)
    val ViolettHell = Color(0xFFC1ADFD)
    val GruenHell = Color(0xFF69DB7C)

    // -------------------------------------------------------- Die Funkgruppen
    //
    // Sechs Kanalfarben, vergeben nach dem Platz einer Gruppe in der Liste.
    // Nicht nach Organisation: Eine Gruppe *hat* keine Organisation, sie kann
    // mehrere tragen oder gar keine (der Einsatzstellenkanal). Ab dem siebten
    // Kanal läuft die Reihe von vorn — dann trägt die Nummer daneben die
    // Unterscheidung allein.

    val Kanal = listOf(
        Color(0xFF78BEF5),
        Color(0xFFFF9D3B),
        Color(0xFF6EE7A8),
        Color(0xFFB197FC),
        Color(0xFFF0C86E),
        Color(0xFF6EDCD6),
    )

    /** Die Kanalfarbe zum Rang einer Gruppe — die Reihe läuft um. */
    fun kanal(rang: Int): Color = Kanal[((rang % Kanal.size) + Kanal.size) % Kanal.size]

    // ------------------------------- Schrift auf einer hellen Fläche
    //
    // Amber, Grün, Signal tragen dunkle Aufschrift. Ein Wert, nicht acht.

    val AufFarbe = Color(0xFF0A0D12)

    /**
     * Die Aufschrift des Hauptknopfs.
     *
     * Nicht `AufFarbe`: Der Hauptknopf ist der einzige Ort, an dem die dunkle
     * Schrift einen Stich ins Braune trägt — sie sitzt auf vollem Amber und
     * wirkt dort neutral-schwarz zu hart. Stand in base.css als `#1a1200`.
     */
    val AufAmber = Color(0xFF1A1200)

    // ---------------------------------------------------------- Auf der Karte

    val Eigenposition = Color(0xFF2F6BFF)
    val SuchFarbe = Color(0xFF4FC3D9)

    // ------------------------------------------------------------ Die Status
    //
    // FMS — die Zahlen, die im Funk gesprochen werden. Sie sind Bedeutung,
    // nicht Schmuck: Wer hier eine Farbe ändert, ändert eine Ansage.

    val FmsFrei = Color(0xFF2F9E44)
    val FmsAnfahrt = Color(0xFFF08C00)
    val FmsVorOrt = Color(0xFFE8590C)
    val FmsSprechwunsch = Color(0xFF1C7ED6)
    val FmsDefekt = Color(0xFFD24C4C)
    val FmsGebunden = Color(0xFF8462EB)

    // ---------------------------------------------------- Die Organisationen

    val OrgFeuerwehr = Color(0xFFE5352B)
    val OrgRettungsdienst = Color(0xFFFF8A3D)
    val OrgThw = Color(0xFF1552D1)
    val OrgPolizei = Color(0xFF2F9E44)

    // ----------------------------------------------- Die Hilfsorganisationen

    val HiorgDrk = Color(0xFFD64550)
    val HiorgJuh = Color(0xFFC2255C)
    val HiorgMhd = Color(0xFFD4A017)
    val HiorgAsb = Color(0xFF4C6EF5)
    val HiorgDlrg = Color(0xFFF4C430)
    val HiorgBergwacht = Color(0xFF38A169)
    val HiorgWasserwacht = Color(0xFF22B8CF)
    val HiorgDgzrs = Color(0xFF1864AB)
    val HiorgBrh = Color(0xFFA1662F)
    val HiorgWerkfeuerwehr = Color(0xFF7048E8)
    val HiorgPrivat = Color(0xFF868E96)

    // ----------------------------------------------------------- Die Tönungen
    //
    // Drei Tönungen für das, was im Web als `--hauch-*` steht. Sie standen dort
    // in 18 verschiedenen Deckkräften, bevor sie einen Namen bekamen; die Zahlen
    // hier sind die häufigsten, nicht die gemittelten.

    val HauchAmber = Amber.copy(alpha = 0.10f)
    val HauchSignal = Signal.copy(alpha = 0.12f)
    val HauchHell = Color.White.copy(alpha = 0.08f)

    /**
     * Die Überlagerung hinter einem Dialog.
     *
     * `rgb(3 5 8 / 72%)` — dunkler als der Seitengrund, damit auch ein Dialog
     * auf einer dunklen Seite eine Kante bekommt.
     */
    val Ueberlagerung = Color(0xFF030508).copy(alpha = 0.72f)
}
