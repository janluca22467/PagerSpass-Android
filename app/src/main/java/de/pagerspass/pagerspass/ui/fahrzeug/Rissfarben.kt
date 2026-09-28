package de.pagerspass.pagerspass.ui.fahrzeug

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/*
 * Die Farben der Bauteile — die Regeln `.fz-*` aus `styles/fahrzeuge.css`.
 *
 * Im Web hängt jedes Teil der Zeichnung an einer Klasse, und das Stilblatt färbt es; hier
 * ist die Klasse ein `Teil`, und das Stilblatt steht in seinem Eintrag. Hell lackierte
 * Fahrzeuge (weißer Rettungsdienst, silberne Polizei, gelber Hubschrauber) tragen im Web
 * ihre Zeichnung in einer Gruppe `.fz-hell`, die alles umdreht, was am dunklen Lack hell
 * war — das ist hier der zweite Stil eines Teils.
 */

/** Woher die Füllung kommt: fest, oder aus dem Bauplan (`--lack`, `--zier`). */
internal enum class Quelle { Fest, Lack, Zier }

/** Füllung, Strich und Strichbreite eines Teils — in Rastereinheiten, wie im SVG. */
internal class Stil(
    val fuellung: Color? = null,
    val strich: Color? = null,
    val breite: Float = 0f,
    val quelle: Quelle = Quelle.Fest,
)

/** `rgb(r g b / a%)` des Stilblatts. */
private fun rgb(r: Int, g: Int, b: Int, anteil: Float = 1f): Color =
    Color(r, g, b, (anteil * 255f).roundToInt())

private fun weiss(anteil: Float) = rgb(255, 255, 255, anteil)
private fun schwarz(anteil: Float) = rgb(0, 0, 0, anteil)
private fun hellblau(anteil: Float) = rgb(235, 242, 252, anteil)
private fun tinte(anteil: Float) = rgb(20, 30, 44, anteil)

private fun f(fuellung: Color) = Stil(fuellung = fuellung)
private fun fs(fuellung: Color, strich: Color, breite: Float) = Stil(fuellung, strich, breite)
private fun s(strich: Color, breite: Float) = Stil(strich = strich, breite = breite)

/** Die Warnfarbe an Bagger, Rettungsbrett und Tochterboot. */
private val WARNORANGE = Lackfarbe.Warnorange

/**
 * Ein Bauteil mit seinem Stil — und, wo das Stilblatt eine Regel `.fz-hell …` hat, dem
 * Stil auf hellem Lack.
 */
internal enum class Teil(val stil: Stil, val hell: Stil? = null) {
    Lack(Stil(quelle = Quelle.Lack)),
    Kante(s(weiss(0.22f), 0.45f), s(tinte(0.38f), 0.5f)),
    Sicke(s(schwarz(0.22f), 0.4f), s(tinte(0.2f), 0.4f)),
    Grill(f(rgb(20, 26, 34, 0.7f))),
    Gegengewicht(fs(rgb(60, 70, 84, 0.75f), schwarz(0.4f), 0.4f)),
    Rad(f(Color(0xFF10161F))),
    Stossstange(f(rgb(210, 220, 235, 0.82f)), f(Color(0xFFAEB7C4))),
    Licht(f(Color(0xFFFFF6D8))),
    Glanz(f(weiss(0.1f))),
    Dach(fs(weiss(0.09f), schwarz(0.22f), 0.35f), fs(tinte(0.06f), tinte(0.2f), 0.35f)),
    Dachglanz(fs(weiss(0.12f), schwarz(0.28f), 0.35f), fs(tinte(0.06f), tinte(0.2f), 0.35f)),
    Spiegel(fs(Color(0xFF0F151D), weiss(0.3f), 0.3f)),
    GlasKlein(f(rgb(12, 22, 34, 0.78f))),
    Fuge(s(schwarz(0.4f), 0.5f), s(tinte(0.28f), 0.5f)),
    Aufbau(fs(schwarz(0.12f), schwarz(0.28f), 0.4f), fs(tinte(0.06f), tinte(0.24f), 0.4f)),
    Podest(fs(weiss(0.07f), schwarz(0.2f), 0.3f)),
    Koffer(fs(weiss(0.08f), schwarz(0.26f), 0.4f), fs(tinte(0.05f), tinte(0.22f), 0.4f)),
    Klima(fs(rgb(226, 232, 240, 0.55f), schwarz(0.3f), 0.3f), fs(Color(0xFFDFE5EE), tinte(0.3f), 0.3f)),
    Tank(fs(schwarz(0.18f), weiss(0.16f), 0.4f)),
    Dom(fs(rgb(215, 225, 240, 0.55f), schwarz(0.35f), 0.3f)),
    Pritsche(f(schwarz(0.42f))),
    Ladung(fs(rgb(150, 160, 175, 0.42f), schwarz(0.3f), 0.3f)),
    Container(fs(Color(0xFF8D97A6), schwarz(0.45f), 0.5f)),
    Riffel(s(schwarz(0.3f), 0.4f)),
    Hakenarm(s(rgb(220, 228, 240, 0.5f), 1.2f)),
    Mulde(fs(Color(0xFF798594), schwarz(0.48f), 0.7f)),
    MuldeInnen(fs(rgb(18, 24, 32, 0.58f), rgb(230, 236, 246, 0.22f), 0.4f)),
    Deichsel(s(Color(0xFF7D8998), 1.2f), s(Color(0xFF8B94A3), 1.2f)),
    Anhaenger(fs(rgb(82, 92, 106, 0.92f), schwarz(0.45f), 0.5f), fs(Color(0xFFE4E9F0), tinte(0.38f), 0.5f)),
    Panzer(fs(schwarz(0.22f), schwarz(0.35f), 0.5f)),
    Gitter(s(rgb(225, 232, 244, 0.6f), 0.6f), s(tinte(0.55f), 0.6f)),
    Holm(f(Color(0xFFC9D3E1))),
    HolmFein(s(rgb(232, 238, 248, 0.55f), 0.4f)),
    Sprosse(s(rgb(60, 72, 88, 0.55f), 0.5f)),
    Drehkranz(fs(schwarz(0.32f), rgb(235, 240, 250, 0.45f), 0.5f)),
    DrehkranzInnen(f(rgb(235, 240, 250, 0.6f))),
    Leiterteil(fs(Color(0xFFB9C4D4), rgb(30, 40, 54, 0.45f), 0.4f)),
    Korb(fs(rgb(20, 28, 38, 0.55f), Color(0xFFD5DEEA), 0.8f)),
    Stuetze(fs(Color(0xFF9AA6B7), rgb(20, 28, 38, 0.4f), 0.3f)),
    Ausleger(fs(Color(0xFF99A5B6), rgb(30, 40, 54, 0.45f), 0.4f)),
    Ausleger2(fs(Color(0xFFC2CCD9), rgb(30, 40, 54, 0.35f), 0.3f)),
    Haken(s(hellblau(0.7f), 0.7f)),
    Lichtmast(fs(rgb(14, 20, 28, 0.78f), rgb(240, 246, 255, 0.4f), 0.4f)),
    Lichtkopf(f(Color(0xFFFFF3CF))),
    Antenne(s(hellblau(0.7f), 0.55f)),
    Antennenfuss(f(hellblau(0.7f))),
    Sat(fs(schwarz(0.3f), hellblau(0.65f), 0.6f)),
    SatInnen(f(hellblau(0.7f))),
    Werfer(fs(schwarz(0.38f), hellblau(0.55f), 0.5f)),
    Rohr(f(rgb(212, 222, 238, 0.7f))),
    Bootrumpf(fs(Color(0xFFD9E0EA), rgb(20, 28, 38, 0.45f), 0.4f)),
    Bootdeck(f(schwarz(0.3f))),
    Haspel(fs(schwarz(0.3f), hellblau(0.6f), 0.55f)),
    Flasche(fs(rgb(212, 222, 238, 0.6f), schwarz(0.35f), 0.3f)),
    Aggregat(fs(rgb(140, 152, 168, 0.45f), schwarz(0.35f), 0.4f)),
    Kette(fs(Color(0xFF10161F), hellblau(0.25f), 0.3f)),
    Baggerhaus(fs(WARNORANGE, schwarz(0.35f), 0.4f)),
    Schaufel(fs(rgb(120, 132, 148, 0.7f), schwarz(0.4f), 0.3f)),
    Box(fs(schwarz(0.3f), hellblau(0.4f), 0.4f), fs(tinte(0.16f), tinte(0.35f), 0.4f)),
    Zelt(fs(rgb(225, 232, 244, 0.45f), schwarz(0.3f), 0.3f)),
    Winde(fs(rgb(200, 210, 226, 0.6f), schwarz(0.35f), 0.3f)),
    Dachkoffer(fs(schwarz(0.26f), weiss(0.22f), 0.4f)),
    Bordwand(f(rgb(190, 202, 220, 0.55f)), f(Color(0xFFB6BFCC))),
    Messkopf(fs(schwarz(0.32f), hellblau(0.55f), 0.4f)),
    Pumpe(fs(schwarz(0.32f), hellblau(0.55f), 0.5f)),
    Kessel(fs(rgb(200, 210, 226, 0.55f), schwarz(0.38f), 0.4f), fs(Color(0xFFCDD5E1), tinte(0.4f), 0.4f)),
    Schornstein(fs(schwarz(0.34f), hellblau(0.45f), 0.3f), fs(tinte(0.3f), tinte(0.35f), 0.3f)),

    /** Das liegende Wasserfass des Forsttraktors — heller als der Gerätekasten daneben. */
    Tankfass(fs(rgb(205, 216, 232, 0.55f), schwarz(0.38f), 0.4f), fs(Color(0xFFCCD4E0), tinte(0.4f), 0.4f)),
    Domdeckel(fs(schwarz(0.3f), hellblau(0.5f), 0.4f), fs(tinte(0.28f), tinte(0.38f), 0.4f)),

    /** Das Auspuffrohr an der Kabinensäule des Schleppers. */
    Auspuff(fs(schwarz(0.4f), hellblau(0.35f), 0.3f), fs(tinte(0.42f), tinte(0.3f), 0.3f)),
    Schlauch(f(rgb(212, 222, 238, 0.45f))),
    Kontur(Stil(quelle = Quelle.Zier)),
    Dachstreifen(Stil(quelle = Quelle.Zier)),
    Block(Stil(quelle = Quelle.Zier)),

    /** Dachkennzeichnung nach DIN 14035. */
    Dachkennung(f(rgb(20, 26, 34, 0.72f))),
    SchraffurGrund(f(rgb(248, 250, 255, 0.88f))),
    Schraffur(f(Lackfarbe.Leuchtrot)),
    Rueckleuchte(f(Color(0xFFFF5A4A))),
    Lichtbalken(fs(Color(0xFF0D1219), weiss(0.28f), 0.4f)),
    Seitenkoffer(fs(schwarz(0.3f), weiss(0.25f), 0.4f)),
    Lenker(f(Color(0xFF10161F))),
    SitzBank(f(Color(0xFF151D27))),
    Rettungsbrett(fs(WARNORANGE, schwarz(0.3f), 0.3f)),

    // Hubschrauber
    Kufe(f(rgb(190, 202, 220, 0.6f)), f(Color(0xFF8B94A3))),
    Leitwerk(Stil(strich = weiss(0.3f), breite = 0.4f, quelle = Quelle.Lack), Stil(strich = tinte(0.35f), breite = 0.4f, quelle = Quelle.Lack)),
    Fenestron(fs(schwarz(0.45f), hellblau(0.45f), 0.6f)),
    HeckrotorNabe(f(hellblau(0.7f))),
    Heckblatt(s(hellblau(0.55f), 0.7f)),
    Rotorscheibe(fs(rgb(200, 220, 255, 0.05f), rgb(200, 220, 255, 0.12f), 0.4f)),
    Blatt(f(rgb(226, 234, 248, 0.42f)), f(rgb(40, 52, 70, 0.45f))),
    Rotorkopf(fs(schwarz(0.55f), hellblau(0.6f), 0.5f)),

    // Boot
    Scheuerleiste(s(rgb(20, 26, 34, 0.55f), 1.2f)),
    Deck(f(schwarz(0.26f))),

    /** Seenotkreuzer tragen einen weißen Aufbau auf rotem Rumpf. */
    Kajuete(fs(Color(0xFFEEF1F6), tinte(0.35f), 0.4f)),
    Konsole(f(rgb(20, 26, 34, 0.7f))),
    Sitz(f(rgb(20, 26, 34, 0.55f))),
    Tochterboot(fs(WARNORANGE, schwarz(0.35f), 0.4f)),
    Motor(f(Color(0xFF10161F))),
    Welle(s(rgb(190, 220, 255, 0.45f), 1.1f)),

    // Blaulicht — die Deckkraft steuert der Takt (siehe `Blitztakt.kt`).
    Blau(f(Color(0xFFCFE2FF))),
    Halo(f(Color(0xFF2F6BFF))),
    Weissblitz(f(Color.White)),
}
