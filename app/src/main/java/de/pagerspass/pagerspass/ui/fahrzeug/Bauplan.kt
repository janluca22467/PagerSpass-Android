package de.pagerspass.pagerspass.ui.fahrzeug

import androidx.compose.ui.graphics.Color

/*
 * Ein Bauplan je eindeutigem Fahrzeugtyp aus `Data/fahrzeuge.json` — übertragen aus
 * `utils/fahrzeugBauplan.ts`.
 *
 * Maße in Dezimetern, damit die Größenverhältnisse echt sind: ein LF 20 ist 7,6 m lang
 * und 2,5 m breit, ein Krad 2,2 m. Gezeichnet wird daraus in `Riss.kt`.
 *
 * Lackiert wird wie im Echten, Stand 2026:
 *   Feuerwehr       RAL 3000 Feuerrot, weiße Konturmarkierung
 *   Rettungsdienst  RAL 9010 Reinweiß mit tagesleuchtroter Blockmarkierung
 *   THW             RAL 5002 Ultramarinblau
 *   Polizei         Silber mit RAL 5017 Verkehrsblau — seit 2005 nicht mehr grün
 *
 * `lack` ist die Karosseriefarbe, `zier` die Farbe der Markierung. `hell` sagt der
 * Zeichnung, dass Kanten, Fugen und Anbauteile dunkel werden müssen — auf einem weißen
 * Wagen ist eine weiße Kante unsichtbar.
 */

/** Grundform. Alles außer `Strasse` hat eine eigene Zeichenroutine. */
enum class Form { Strasse, Heli, Boot, Moto, Quad, Gespann }

/** Fahrgestellklasse — bestimmt Räder, Kabinenlänge und Umriss. */
enum class Klasse { Lkw, Transporter, Pkw }

/** Was hinter dem Fahrerhaus steht. */
enum class Aufbauart { Kasten, Koffer, Tank, Pritsche, Ab, Mulde, Bus, Gepanzert, Keiner }

/** Aufbau eines gezogenen Feuerwehranhängers. */
enum class Anhaengerart { Gefahrgut, Strom, Boot, Kueche, Wassertank }

/** Beladung und Anbauten, die eine Bauart von der anderen unterscheiden. */
enum class Dachmodul {
    Steckleiter, Drehleiter, Mast, Kran, Grosskran, Lichtmast, Antennen, Fernmeldemast,
    Werfer, Wasserwerfer, Boot, Dachstreifen, Haspel, Flaschen, Aggregat, Bagger,
    Hundeboxen, Zelt, Seilwinde, Dachkoffer, Ladebordwand, Messtechnik, Tauchpumpe, Kessel,
}

/**
 * `Streifen` ist die Konturmarkierung nach DIN 30710, `Blockstreifen` der breite
 * Warnstreifen des Rettungsdienstes, `Band` der durchgehende Streifen der Polizei.
 */
enum class Markierungsart { Streifen, Blockstreifen, Band, NurHeck, Keine }

/**
 * Wie die Sondersignalanlage blitzt: `Doppel` ist der moderne LED-Balken, `Vierfach`
 * fährt die Polizei und wer eine Vierfachblitzanlage hat, `Rundum` ist die klassische
 * Rundumkennleuchte, die auf- und abblendet statt zu blitzen.
 *
 * `wort` ist die Schreibweise des Webs — so stehen die Muster auch in den Blaulicht-
 * punkten eines Icon-Packs.
 */
enum class Blitzmuster(val wort: String) {
    Doppel("doppel"),
    Vierfach("vierfach"),
    Rundum("rundum"),
    ;

    companion object {
        /** Das Muster zu seinem Wort; Unbekanntes blitzt doppelt wie ein LED-Balken. */
        fun von(wort: String?): Blitzmuster = entries.firstOrNull { it.wort == wort } ?: Doppel
    }
}

/**
 * Die Lackierungen — dieselben Werte wie `--lack-*` in `styles/fahrzeuge.css`. Echte
 * RAL-Töne und deshalb feste Werte: Ein Feuerwehrrot wird auch in einem hellen Thema
 * nicht heller.
 */
object Lackfarbe {
    val Feuerwehr = Color(0xFFB32821) // RAL 3000 Feuerrot
    val Weiss = Color(0xFFEFF2F7) // RAL 9010 Reinweiß
    val Leuchtrot = Color(0xFFE2231A) // RAL 3024 — die Blockmarkierung am Rettungswagen
    val Thw = Color(0xFF24316F) // RAL 5002 Ultramarinblau
    val Silber = Color(0xFFC2C8D2) // Silber der Streifenwagen
    val Polizeiblau = Color(0xFF0D3B8C) // RAL 5017 Verkehrsblau
    val Grau = Color(0xFF7C8794) // private Träger
    val Zivil = Color(0xFF464C56) // Zivilstreife
    val ZivilHell = Color(0xFF8B95A3) // Silbergrau — das zweite Zivilfahrzeug (KDD)
    val Anthrazit = Color(0xFF343A43) // SEK
    val Dlrg = Color(0xFFD8352C)
    val DlrgGelb = Color(0xFFF5C518)
    val Wasserwacht = Color(0xFFCF2F28)
    val Dgzrs = Color(0xFFD81F26)
    val Rth = Color(0xFFF2C200) // das Gelb der Rettungshubschrauber
    val ZierHell = Color(0xFFF4F7FB)
    val ZierDunkel = Color(0xFF1A222E)
    val Warnorange = Color(0xFFF0A500) // Bagger, Rettungsbrett, Tochterboot
}

/**
 * Der Bauplan eines Fahrzeugtyps. Was im Web ausgelassen werden darf (`?:`), ist hier
 * `null` oder trägt denselben Vorgabewert, mit dem die Zeichnung dort rechnet.
 */
data class Bauplan(
    /** Genau der `typ` aus dem Fahrzeugkatalog des Servers. */
    val typ: String = "",
    /** Karosseriefarbe. */
    val lack: Color,
    /** Farbe der Markierung — Streifen, Karos, Dachband. */
    val zier: Color,
    /** Heller Lack: Kanten und Anbauteile müssen dunkel werden. */
    val hell: Boolean = false,
    val form: Form = Form.Strasse,
    val klasse: Klasse? = null,
    /** Länge in Dezimetern. */
    val laenge: Float,
    /** Breite in Dezimetern. */
    val breite: Float,
    /** Eckenrundung der Karosserie. */
    val radius: Float? = null,
    /** Länge des Fahrerhauses; sonst aus der Klasse abgeleitet. */
    val kabinenlaenge: Float? = null,
    val aufbau: Aufbauart? = null,
    val dach: List<Dachmodul> = emptyList(),
    val markierung: Markierungsart? = null,
    /** Rot-weiße Warnschraffur am Heck — am Streifenwagen gibt es die nicht. */
    val heckschraffur: Boolean = true,
    /** Achspositionen als Anteil der Länge. */
    val achsen: List<Float>? = null,
    val reifen: Float? = null,
    val reifenLang: Float? = null,
    /** Zwillingsbereifung an den Hinterachsen (Lkw). */
    val zwilling: Boolean = true,
    /** Blaulicht auf der Karte zeichnen? Das fachliche Sondersignal bleibt davon unberührt. */
    val blaulicht: Boolean = true,
    /** Blitzmuster der Sondersignalanlage; ohne Angabe je nach Form (siehe `Riss.kt`). */
    val blitz: Blitzmuster? = null,
    /** Freiraum hinter dem Aufbau. */
    val heckfrei: Float? = null,
    /** Boot mit Steuerhaus statt offener Konsole. */
    val kajuete: Boolean = false,
    /** Seenotkreuzer mit Tochterboot im Heck. */
    val tochterboot: Boolean = false,
    /** Nur bei `Gespann`: was auf dem gezogenen Anhänger steht. */
    val anhaenger: Anhaengerart? = null,
    /**
     * Nur bei `Gespann`: gezogen vom Schlepper statt vom Mehrzweckfahrzeug.
     *
     * <b>Der Schlepper braucht eine eigene Silhouette</b>, sonst ist er auf der Karte ein
     * MZF mit Fass dahinter. Von oben erkennt man ihn an einem: die Hinterräder sind
     * doppelt so breit wie die vorderen, und die Kabine steht schmal dazwischen.
     */
    val traktor: Boolean = false,
)

/**
 * Was eine Lackierung ausmacht — wird über eine Vorlage gelegt wie im Web das
 * Hineinspreizen: Nur was hier gesetzt ist (nicht `null`), überschreibt.
 */
private data class Lackierung(
    val lack: Color,
    val zier: Color,
    val hell: Boolean? = null,
    val markierung: Markierungsart? = null,
    val heckschraffur: Boolean? = null,
    val blitz: Blitzmuster? = null,
)

private fun Bauplan.mit(l: Lackierung): Bauplan = copy(
    lack = l.lack,
    zier = l.zier,
    hell = l.hell ?: hell,
    markierung = l.markierung ?: markierung,
    heckschraffur = l.heckschraffur ?: heckschraffur,
    blitz = l.blitz ?: blitz,
)

/** Feuerrot, wie jedes Löschfahrzeug in Deutschland. */
private val FEUERWEHR = Lackierung(Lackfarbe.Feuerwehr, Lackfarbe.ZierHell)

/** Weißer Wagen, tagesleuchtrote Blockmarkierung. */
private val RETTUNG = Lackierung(Lackfarbe.Weiss, Lackfarbe.Leuchtrot, hell = true)

/** Ultramarinblau des THW. */
private val THW = Lackierung(Lackfarbe.Thw, Lackfarbe.ZierHell, blitz = Blitzmuster.Rundum)

/** Silber-blau: die Lackierung der deutschen Polizei seit 2005. */
private val POLIZEI = Lackierung(
    Lackfarbe.Silber,
    Lackfarbe.Polizeiblau,
    hell = true,
    markierung = Markierungsart.Band,
    heckschraffur = false,
    blitz = Blitzmuster.Vierfach,
)

/** Rettungswagen privater Träger: ebenfalls weiß, aber ohne Signalrot. */
private val PRIVAT = Lackierung(Lackfarbe.Weiss, Lackfarbe.Grau, hell = true)

/** DLRG — rot mit gelbem Streifen. */
private val DLRG = Lackierung(Lackfarbe.Dlrg, Lackfarbe.DlrgGelb)

/** Wasserwacht — rot-weiß. */
private val WASSERWACHT = Lackierung(Lackfarbe.Wasserwacht, Lackfarbe.ZierHell)

/** DGzRS — der rote Rumpf der Seenotretter. */
private val DGZRS = Lackierung(Lackfarbe.Dgzrs, Lackfarbe.ZierHell)

/** Zivilstreife: ein unauffälliger Wagen, deshalb dunkelgrau und ohne Markierung. */
private val ZIVIL = Lackierung(
    Lackfarbe.Zivil,
    Lackfarbe.Zivil,
    markierung = Markierungsart.Keine,
    heckschraffur = false,
)

/**
 * Das zweite Zivilfahrzeug: silbergrau.
 *
 * <b>Eine zweite Lackierung und nicht dieselbe.</b> Zwei dunkelgraue Pkw nebeneinander
 * sind auf der Kartenmarke derselbe Fleck — wer Zivilstreife und Kriminaldauerdienst hat,
 * könnte sie nicht auseinanderhalten.
 */
private val ZIVIL_HELL = Lackierung(
    Lackfarbe.ZivilHell,
    Lackfarbe.ZivilHell,
    hell = true,
    markierung = Markierungsart.Keine,
    heckschraffur = false,
)

/** SEK: anthrazit, keine Beschriftung. */
private val SEK_FARBE = Lackierung(Lackfarbe.Anthrazit, Lackfarbe.Anthrazit, heckschraffur = false)

/** Vorlage für Löschfahrzeuge — LKW-Fahrgestell, Kastenaufbau, Steckleiter. */
private fun lf(
    laenge: Float,
    breite: Float = 25f,
    dach: List<Dachmodul> = listOf(Dachmodul.Steckleiter, Dachmodul.Lichtmast),
): Bauplan = Bauplan(
    lack = FEUERWEHR.lack,
    zier = FEUERWEHR.zier,
    klasse = Klasse.Lkw,
    laenge = laenge,
    breite = breite,
    aufbau = Aufbauart.Kasten,
    dach = dach,
    markierung = Markierungsart.Streifen,
)

private fun gw(laenge: Float, dach: List<Dachmodul> = listOf(Dachmodul.Lichtmast), breite: Float = 25f): Bauplan =
    lf(laenge, breite, dach)

private fun thwLkw(
    laenge: Float,
    dach: List<Dachmodul>,
    breite: Float = 25f,
    aufbau: Aufbauart = Aufbauart.Kasten,
): Bauplan = Bauplan(
    lack = THW.lack,
    zier = THW.zier,
    blitz = THW.blitz,
    klasse = Klasse.Lkw,
    laenge = laenge,
    breite = breite,
    aufbau = aufbau,
    dach = dach,
    markierung = Markierungsart.Streifen,
)

private fun kombi(farben: Lackierung, laenge: Float = 48f, dach: List<Dachmodul> = emptyList()): Bauplan =
    Bauplan(
        lack = farben.lack,
        zier = farben.zier,
        klasse = Klasse.Pkw,
        laenge = laenge,
        breite = 19f,
        radius = 5f,
        aufbau = Aufbauart.Keiner,
        dach = dach,
        markierung = Markierungsart.Streifen,
    ).mit(farben)

private fun transporter(
    farben: Lackierung,
    laenge: Float,
    dach: List<Dachmodul> = emptyList(),
    aufbau: Aufbauart = Aufbauart.Koffer,
    breite: Float = 21f,
): Bauplan = Bauplan(
    lack = farben.lack,
    zier = farben.zier,
    klasse = Klasse.Transporter,
    laenge = laenge,
    breite = breite,
    radius = 4f,
    aufbau = aufbau,
    dach = dach,
    markierung = Markierungsart.Streifen,
).mit(farben)

/** Ein Gespann aus Zugfahrzeug und Anhänger — im Web `form: 'gespann'` mit `klasse: 'transporter'`. */
private fun gespann(farben: Lackierung, laenge: Float, breite: Float, anhaenger: Anhaengerart): Bauplan =
    Bauplan(
        lack = farben.lack,
        zier = farben.zier,
        form = Form.Gespann,
        klasse = Klasse.Transporter,
        laenge = laenge,
        breite = breite,
        anhaenger = anhaenger,
    ).mit(farben)

/** Sonderform ohne Fahrgestell (Hubschrauber, Boot, Krad, Quad). */
private fun sonderform(farben: Lackierung, form: Form, laenge: Float, breite: Float): Bauplan =
    Bauplan(lack = farben.lack, zier = farben.zier, form = form, laenge = laenge, breite = breite).mit(farben)

/** Der Katalogeintrag: Typname auf eine Vorlage. */
private infix fun String.wie(vorlage: Bauplan): Bauplan = vorlage.copy(typ = this)

private val ST = Dachmodul.Steckleiter
private val LM = Dachmodul.Lichtmast

val BAUPLAENE: List<Bauplan> = listOf(
    // ------------------------------------------------------------ Feuerwehr
    "TSF-W" wie lf(60f, 22f, listOf(ST)),
    "LF 8/6" wie lf(64f, 23f),
    "MLF" wie lf(66f, 23f),
    "LF 10" wie lf(70f),
    "LF 20" wie lf(76f),
    // Das Katastrophenschutz-Fahrzeug: Gruppenkabine wie beim LF 10, im Aufbau aber
    // Tragkraftspritze und B-Schlauch statt Tank. Kein Lichtmast — daran ist es auf der
    // Lagekarte vom LF 10 daneben zu unterscheiden.
    "LF 16-TS" wie lf(69f, 24f, listOf(ST)),
    // Kürzer als das HLF 20 und ohne dessen Schlauchhaspel.
    "HLF 10" wie lf(72f, 24f, listOf(ST, LM)),
    "HLF 20" wie lf(80f, 25f, listOf(ST, LM, Dachmodul.Haspel)),
    "HLF (Werk)" wie lf(80f, 25f, listOf(ST, LM, Dachmodul.Haspel)),
    // Der Vorgänger der Reihe: kürzer und schmaler als das TLF 3000, oben die Steckleiter.
    "TLF 16/25" wie lf(73f, 24f, listOf(ST)).copy(aufbau = Aufbauart.Tank),
    "TLF 3000" wie lf(76f, 25f, listOf(Dachmodul.Werfer, LM)).copy(aufbau = Aufbauart.Tank, reifen = 3.8f),
    "TLF 4000" wie lf(82f, 26f, listOf(Dachmodul.Werfer, LM))
        .copy(aufbau = Aufbauart.Tank, reifen = 4.2f, reifenLang = 9.4f),
    // Der Unimog ist kurz, hoch und schmal — die großen Reifen sind das Merkmal.
    "TLF 2000 Unimog" wie lf(62f, 26f, listOf(LM))
        .copy(aufbau = Aufbauart.Tank, reifen = 5.2f, reifenLang = 7.6f),
    // Der Forsttraktor: ein Schlepper mit Wasserfass am Haken, ohne Balken über die Breite.
    "Forsttraktor mit Löschanhänger" wie gespann(FEUERWEHR, 88f, 20f, Anhaengerart.Wassertank)
        .copy(traktor = true, blitz = Blitzmuster.Rundum),
    "SLF" wie lf(84f, 26f, listOf(Dachmodul.Werfer, LM)).copy(aufbau = Aufbauart.Tank),
    "FLF" wie lf(98f, 30f, listOf(Dachmodul.Werfer)).copy(
        aufbau = Aufbauart.Tank,
        achsen = listOf(0.14f, 0.28f, 0.72f, 0.86f),
        reifen = 4.4f,
        reifenLang = 10f,
        zwilling = false,
    ),
    "DLK 23/12" wie lf(100f, 25f, listOf(Dachmodul.Drehleiter)),
    "DLA(K) 18/12" wie lf(84f, 23f, listOf(Dachmodul.Drehleiter)),
    "TMF 32" wie lf(100f, 25f, listOf(Dachmodul.Mast)),
    "GW-Höhenrettung" wie gw(70f, listOf(LM, Dachmodul.Haspel), 24f),
    "RW" wie gw(76f, listOf(LM, Dachmodul.Seilwinde)),
    "RW-Kran" wie gw(82f, listOf(Dachmodul.Kran)),
    "FwK 30" wie gw(108f, listOf(Dachmodul.Grosskran), 26f)
        .copy(aufbau = Aufbauart.Pritsche, achsen = listOf(0.12f, 0.24f, 0.74f, 0.87f)),
    "WLF mit AB" wie gw(92f, emptyList(), 25f).copy(aufbau = Aufbauart.Ab),
    "GW-Gefahrgut" wie gw(82f, listOf(LM, Dachmodul.Flaschen)),
    "GW-Gefahrgut (Werk)" wie gw(82f, listOf(LM, Dachmodul.Flaschen)),
    "GW-Atemschutz" wie gw(72f, listOf(Dachmodul.Flaschen), 24f),
    "GW-Mess" wie transporter(FEUERWEHR, 62f, listOf(Dachmodul.Messtechnik)),
    "Dekon-P" wie gw(78f, listOf(Dachmodul.Zelt)),
    "GW-Wasserrettung" wie gw(78f, listOf(Dachmodul.Boot)),
    "ELW 1" wie transporter(FEUERWEHR, 58f, listOf(Dachmodul.Antennen)).copy(blitz = Blitzmuster.Vierfach),
    // Der A-Dienst bleibt bei der Großlage stehen und trägt dafür den Fernmeldemast —
    // vier Punkte länger als der ELW 1 daneben.
    "ELW 1 A-Dienst" wie transporter(FEUERWEHR, 62f, listOf(Dachmodul.Antennen, Dachmodul.Fernmeldemast))
        .copy(blitz = Blitzmuster.Vierfach),
    "ELW 2" wie gw(84f, listOf(Dachmodul.Fernmeldemast), 25f)
        .copy(aufbau = Aufbauart.Koffer, blitz = Blitzmuster.Vierfach),
    "MTF" wie transporter(FEUERWEHR, 58f, emptyList(), Aufbauart.Bus),
    "GW-Logistik" wie gw(78f, listOf(Dachmodul.Ladebordwand)).copy(aufbau = Aufbauart.Koffer),
    "WLF + AB Rüst" wie gw(92f, listOf(Dachmodul.Seilwinde, LM)).copy(aufbau = Aufbauart.Ab),
    "WLF + AB Tank" wie gw(92f, listOf(Dachmodul.Werfer)).copy(aufbau = Aufbauart.Ab),
    "WLF + AB Atem-/Strahlenschutz" wie gw(92f, listOf(Dachmodul.Flaschen, Dachmodul.Messtechnik))
        .copy(aufbau = Aufbauart.Ab),
    "WLF + AB Atemschutz" wie gw(92f, listOf(Dachmodul.Flaschen)).copy(aufbau = Aufbauart.Ab),
    "WLF + AB Gefahrgut" wie gw(92f, listOf(Dachmodul.Flaschen, Dachmodul.Tauchpumpe)).copy(aufbau = Aufbauart.Ab),
    "WLF + AB TEL" wie gw(92f, listOf(Dachmodul.Fernmeldemast))
        .copy(aufbau = Aufbauart.Ab, blitz = Blitzmuster.Vierfach),
    "WLF + AB Schlauch" wie gw(92f, listOf(Dachmodul.Haspel)).copy(aufbau = Aufbauart.Ab),
    "WLF + AB Mulde" wie gw(92f, emptyList()).copy(aufbau = Aufbauart.Mulde),
    "WLF + AB Notstrom" wie gw(92f, listOf(Dachmodul.Aggregat, LM)).copy(aufbau = Aufbauart.Ab),
    "WLF + AB Dekon P" wie gw(92f, listOf(Dachmodul.Zelt)).copy(aufbau = Aufbauart.Ab),
    "WLF + AB Öl" wie gw(92f, listOf(Dachmodul.Tauchpumpe)).copy(aufbau = Aufbauart.Ab),
    // Sandsackfüllanlage: offene Mulde, dazu der Bagger, mit dem der Sand bewegt wird.
    "WLF + AB Sandsack" wie gw(92f, listOf(Dachmodul.Bagger)).copy(aufbau = Aufbauart.Mulde),
    // Feldküche und Zelte — derselbe Behälter wie Dekon P, aber mit Ladebordwand.
    "WLF + AB Betreuung" wie gw(92f, listOf(Dachmodul.Zelt, Dachmodul.Ladebordwand)).copy(aufbau = Aufbauart.Ab),
    // Waldbrandmodul: Werfer wie das TLF, Haspel für die D-Schlauchstrecke.
    "WLF + AB Rüst Vegetation" wie gw(92f, listOf(Dachmodul.Werfer, Dachmodul.Haspel)).copy(aufbau = Aufbauart.Ab),
    // Der Küchencontainer: Kochkessel und Abzug statt Zeltrollen.
    "WLF + AB Küche" wie gw(92f, listOf(Dachmodul.Kessel, Dachmodul.Ladebordwand)).copy(aufbau = Aufbauart.Ab),
    // Das Trägerfahrzeug allein: kein Aufbau — ein Lkw mit einem Haken darauf.
    "WLF" wie gw(86f, emptyList()).copy(aufbau = Aufbauart.Keiner),
    "GW-Öl" wie gw(74f, listOf(Dachmodul.Tauchpumpe, LM)).copy(aufbau = Aufbauart.Koffer),
    // Kofferaufbau für die Bodenstation, Mast für die Antenne, Lichtmast für die Nachtsuche.
    "Drohnenstaffel" wie transporter(FEUERWEHR, 58f, listOf(Dachmodul.Mast, LM)),
    "SW 1000" wie gw(70f, listOf(Dachmodul.Haspel), 23f),
    "SW 2000" wie gw(78f, listOf(Dachmodul.Haspel)),
    "SW KatS" wie gw(82f, listOf(Dachmodul.Haspel), 25f).copy(reifen = 4.1f, reifenLang = 9.2f),
    "KdoW" wie kombi(FEUERWEHR, 49f, listOf(Dachmodul.Antennen)).copy(blitz = Blitzmuster.Vierfach),
    // Die drei Führungsdienste unterscheiden sich über Länge und Dach, nicht über den Lack:
    // Der C-Dienst ist der kürzeste und trägt nichts auf dem Dach.
    "KdoW C-Dienst" wie kombi(FEUERWEHR, 44f, emptyList()).copy(blitz = Blitzmuster.Vierfach),
    // Der B-Dienst arbeitet aus dem Wagen heraus: Antennen und Dachkoffer.
    "KdoW B-Dienst" wie kombi(FEUERWEHR, 52f, listOf(Dachmodul.Antennen, Dachmodul.Dachkoffer))
        .copy(blitz = Blitzmuster.Vierfach),
    "MZF mit Ladefläche" wie transporter(FEUERWEHR, 62f, listOf(Dachmodul.Ladebordwand), Aufbauart.Pritsche, 22f),
    "MZF + Anhänger Gefahrgut" wie gespann(FEUERWEHR, 96f, 22f, Anhaengerart.Gefahrgut),
    "MZF + Anhänger Strom/Licht" wie gespann(FEUERWEHR, 96f, 22f, Anhaengerart.Strom),
    "MZF + Anhänger MZB" wie gespann(FEUERWEHR, 102f, 22f, Anhaengerart.Boot).copy(blitz = Blitzmuster.Rundum),
    "Quad (Feuerwehr)" wie sonderform(FEUERWEHR, Form.Quad, 28f, 16f),

    // -------------------------------------------------------- Rettungsdienst
    "RTW" wie transporter(RETTUNG, 68f, emptyList(), Aufbauart.Koffer, 22f)
        .copy(dach = listOf(Dachmodul.Dachstreifen), markierung = Markierungsart.Blockstreifen),
    "RTW (privat)" wie transporter(PRIVAT, 68f, emptyList(), Aufbauart.Koffer, 22f)
        .copy(dach = listOf(Dachmodul.Dachstreifen), markierung = Markierungsart.Blockstreifen),
    "KTW" wie transporter(RETTUNG, 58f, emptyList(), Aufbauart.Koffer, 20f)
        .copy(dach = listOf(Dachmodul.Dachstreifen), markierung = Markierungsart.Blockstreifen),
    "KTW (privat)" wie transporter(PRIVAT, 58f, emptyList(), Aufbauart.Koffer, 20f)
        .copy(dach = listOf(Dachmodul.Dachstreifen), markierung = Markierungsart.Blockstreifen),
    "NKTW" wie transporter(RETTUNG, 60f, emptyList(), Aufbauart.Koffer, 20f)
        .copy(dach = listOf(Dachmodul.Dachstreifen), markierung = Markierungsart.Blockstreifen),
    "ITW" wie transporter(RETTUNG, 76f, emptyList(), Aufbauart.Koffer, 23f)
        .copy(dach = listOf(Dachmodul.Dachstreifen), markierung = Markierungsart.Blockstreifen),
    "NEF" wie kombi(RETTUNG, 50f, listOf(Dachmodul.Dachkoffer))
        .copy(blitz = Blitzmuster.Vierfach, markierung = Markierungsart.Blockstreifen),
    "OrgL" wie kombi(RETTUNG, 48f, listOf(Dachmodul.Antennen)).copy(blitz = Blitzmuster.Vierfach),
    "ELRD" wie kombi(RETTUNG, 48f, listOf(Dachmodul.Antennen)).copy(blitz = Blitzmuster.Vierfach),
    // Der Leitende Notarzt kommt allein — etwas kürzer als OrgL und ELRD: Er sichtet.
    "LNA" wie kombi(RETTUNG, 46f, listOf(Dachmodul.Antennen)).copy(blitz = Blitzmuster.Vierfach),
    // Der ELW 1 der Hilfsorganisationen: ein Transporter mit Besprechungsraum, weiß mit
    // Blockmarkierung — ein roter ELW 1 an einer MANV-Stelle wäre eine falsche Fährte.
    "ELW 1 (RD)" wie transporter(RETTUNG, 58f, listOf(Dachmodul.Antennen, Dachmodul.Mast), Aufbauart.Koffer, 21f).copy(
        dach = listOf(Dachmodul.Antennen, Dachmodul.Mast, Dachmodul.Dachstreifen),
        markierung = Markierungsart.Blockstreifen,
        blitz = Blitzmuster.Vierfach,
    ),
    "GW-San" wie gw(78f, listOf(Dachmodul.Ladebordwand)).mit(RETTUNG).copy(
        aufbau = Aufbauart.Koffer,
        dach = listOf(Dachmodul.Ladebordwand, Dachmodul.Dachstreifen),
        markierung = Markierungsart.Blockstreifen,
    ),
    "BtKW" wie transporter(RETTUNG, 60f, emptyList(), Aufbauart.Bus)
        .copy(dach = listOf(Dachmodul.Dachstreifen), markierung = Markierungsart.Blockstreifen),
    // Der Feldkochherd der Verpflegungseinheit: gezogen, nicht selbstfahrend.
    "Feldküche" wie gespann(RETTUNG, 98f, 21f, Anhaengerart.Kueche).copy(markierung = Markierungsart.Blockstreifen),
    // Der Mannschaftswagen des Verpflegungstrupps — länger als der BtKW, er fährt 1/8.
    "MTW Verpflegung" wie transporter(RETTUNG, 64f, listOf(Dachmodul.Dachkoffer), Aufbauart.Bus)
        .copy(markierung = Markierungsart.Blockstreifen),
    "MTW Hundestaffel" wie transporter(RETTUNG, 58f, listOf(Dachmodul.Hundeboxen))
        .copy(markierung = Markierungsart.Blockstreifen),
    "Rettungshundestaffel" wie transporter(RETTUNG, 56f, listOf(Dachmodul.Hundeboxen))
        .copy(markierung = Markierungsart.Blockstreifen),
    // Der Flug läuft weiterhin mit Sondersignal; nur ein blau blinkender Hubschrauber auf
    // der Karte wäre die falsche Darstellung.
    Bauplan(
        typ = "RTH",
        form = Form.Heli,
        lack = Lackfarbe.Rth,
        zier = Lackfarbe.ZierDunkel,
        hell = true,
        laenge = 88f,
        breite = 42f,
        blaulicht = false,
    ),
    "MRB" wie sonderform(DLRG, Form.Boot, 62f, 20f),
    "RTB" wie sonderform(WASSERWACHT, Form.Boot, 52f, 18f),
    "Seenotrettungsboot" wie sonderform(DGZRS, Form.Boot, 82f, 26f).copy(kajuete = true),
    "Seenotrettungskreuzer" wie sonderform(DGZRS, Form.Boot, 108f, 34f).copy(kajuete = true, tochterboot = true),
    "Tauchergruppe" wie transporter(DLRG, 60f, listOf(Dachmodul.Flaschen)),
    "Quad" wie sonderform(WASSERWACHT, Form.Quad, 28f, 16f),

    // ------------------------------------------------------------------- THW
    "GKW" wie thwLkw(78f, listOf(LM, Dachmodul.Seilwinde)),
    "MzKW" wie thwLkw(76f, listOf(Dachmodul.Kran), 25f, Aufbauart.Pritsche),
    "FGr SB" wie thwLkw(82f, listOf(Dachmodul.Kran), 25f, Aufbauart.Pritsche),
    "MTW" wie transporter(THW, 58f, emptyList(), Aufbauart.Bus),
    "FGr FK" wie transporter(THW, 62f, listOf(Dachmodul.Antennen)),
    "FGr BrB" wie thwLkw(88f, emptyList(), 25f, Aufbauart.Pritsche),
    "FGr I" wie thwLkw(72f, listOf(LM), 24f),
    "FGr N" wie thwLkw(74f, listOf(Dachmodul.Aggregat), 24f, Aufbauart.Pritsche),
    "FGr Ortung" wie transporter(THW, 64f, listOf(Dachmodul.Hundeboxen)),
    "FGr R" wie thwLkw(96f, listOf(Dachmodul.Bagger), 27f, Aufbauart.Pritsche),
    "FGr TW" wie thwLkw(78f, listOf(LM), 25f, Aufbauart.Tank),
    "FGr W" wie thwLkw(80f, listOf(Dachmodul.Boot)),
    "FGr Ö" wie thwLkw(78f, listOf(Dachmodul.Tauchpumpe)),

    // -------------------------------------------------------------- Polizei
    "FuStW" wie kombi(POLIZEI, 48f),
    "Unfallaufnahme" wie kombi(POLIZEI, 50f, listOf(Dachmodul.Dachkoffer)),
    "Diensthundeführer" wie kombi(POLIZEI, 50f, listOf(Dachmodul.Hundeboxen)),
    "Zivilstreife" wie kombi(ZIVIL, 48f).copy(markierung = Markierungsart.Keine),
    /*
     * Die Kriminalpolizei — drei Fahrzeuge, von denen nur eines aussieht wie Polizei. Auf
     * der Karte trägt jedes ein Merkmal, das auf 42 Punkten noch trägt: den helleren Lack
     * am KDD, den Lichtmast an der Kriminaltechnik, Länge und Fensterband am MEK.
     */
    // Ein ziviler Kombi, silbergrau, mit den beiden Antennenfüßen auf dem Dach.
    "KDD" wie kombi(ZIVIL_HELL, 52f, listOf(Dachmodul.Antennen)),
    // Der Erkennungsdienst: Kastenwagen in Polizeilackierung, Lichtmast für den Tatort.
    "Kriminaltechnik" wie transporter(POLIZEI, 66f, listOf(LM), Aufbauart.Kasten, 21f),
    "Krad" wie sonderform(POLIZEI, Form.Moto, 22f, 8f).copy(lack = Lackfarbe.Weiss),
    "GruKw" wie transporter(POLIZEI, 60f, emptyList(), Aufbauart.Bus),
    "SEK" wie transporter(SEK_FARBE, 64f, emptyList(), Aufbauart.Gepanzert, 22f)
        .copy(markierung = Markierungsart.NurHeck),
    // Das MEK fährt, was auf der Straße nicht auffällt: ziviler Transporter mit Fensterband.
    "MEK" wie transporter(ZIVIL, 58f, emptyList(), Aufbauart.Bus, 20f),
    "WaWe 10" wie Bauplan(
        lack = POLIZEI.lack,
        zier = POLIZEI.zier,
        klasse = Klasse.Lkw,
        laenge = 96f,
        breite = 28f,
    ).mit(POLIZEI).copy(
        aufbau = Aufbauart.Gepanzert,
        dach = listOf(Dachmodul.Wasserwerfer),
        markierung = Markierungsart.Streifen,
        achsen = listOf(0.15f, 0.68f, 0.84f),
    ),
    "Polizeihubschrauber" wie sonderform(POLIZEI, Form.Heli, 88f, 42f),
)

/** Ein Rückfall je Organisation, falls der Server einen Typ liefert, den der Katalog nicht kennt. */
private val RUECKFALL = mapOf(
    "Feuerwehr" to "LF 10",
    "Rettungsdienst" to "RTW",
    "Thw" to "GKW",
    "Polizei" to "FuStW",
)

private val NACH_TYP: Map<String, Bauplan> = BAUPLAENE.associateBy { it.typ.lowercase() }

/**
 * Der Bauplan zu einem Fahrzeugtyp. Der Katalog des Servers und dieser hier werden von
 * Hand zusammengehalten; kommt trotzdem ein unbekannter Typ an, fährt er als
 * Standardfahrzeug seiner Organisation, statt zu verschwinden.
 */
fun bauplanFuer(typ: String, organisation: String): Bauplan =
    NACH_TYP[typ.trim().lowercase()]
        ?: NACH_TYP.getValue((RUECKFALL[organisation] ?: "LF 10").lowercase())
