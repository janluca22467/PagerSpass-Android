package de.pagerspass.pagerspass.melder

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Die Kataloge des Melders — übertragen aus `MELDER_BAUFORMEN`,
 * `MELDER_GESICHTER`, `MELDER_TOENE`, `ALARMIERUNGSARTEN` und `MELDERPROFILE`
 * in `web/src/types.ts`.
 *
 * <b>Von Hand gleichzuhalten, wie die Schmuckkataloge daneben</b>
 * (`ui/schmuck/Schmuck.kt`): Der Server liefert sie nicht mit, im Web stehen sie
 * ebenfalls fest im Code. Ein Stück, das dort dazukommt, fehlt hier, bis es
 * jemand einträgt — es fällt dann still aus der Auswahl, ohne Fehler.
 *
 * <b>Drei Wege zu einem Stück</b> — erspielt (`abLevel`), gekauft
 * (`preisCredits`, Artikel `melder-<id>` bzw. `ton-<id>`) oder am Abo
 * (`premium`). Dieselbe Dreiteilung wie beim Profilschmuck, aus denselben
 * Gründen.
 */
object Melderkatalog {

    /** Eine Bauform — welches Gerät in der Tasche steckt. */
    data class Bauform(
        val id: String,
        val name: String,
        val abLevel: Int,
        val erklaerung: String,
        val premium: Boolean = false,
    ) {
        /**
         * Gefragt wird das Merkmal, nicht die Stufe allein: Die Abo-Geräte
         * hängen an laufendem Premium und an keiner Laufbahnstufe.
         */
        fun frei(stufe: Int, premiumAktiv: Boolean): Boolean =
            if (premium) premiumAktiv else stufe >= abLevel

        /** Liegt das Gerät quer? Dann bekommt es in Vorschau und Blende mehr Breite. */
        val quer: Boolean get() = id in QUER
    }

    private val QUER = setOf("quad", "leucht", "monitor", "fax")

    val BAUFORMEN = listOf(
        Bauform("dienst", "Dienst", 1, "Dienstgerät mit LC-Display"),
        Bauform("klassik", "Klassik", 10, "Kleiner Piepser mit gelber Quittungstaste"),
        Bauform("farbe", "Farbe", 15, "Farbdisplay mit Annehmen und Ablehnen"),
        Bauform("fax", "Alarmfax", 75, "Druckt die Meldung — abreißen quittiert"),
        Bauform(
            "quad", "Quermelder", 40,
            "Querformat, Tasten oben auf dem Rücken — die Meldung in ganzen Zeilen",
            premium = true,
        ),
        Bauform("lamellen", "Lamellenmelder", 120, "Vier gewölbte Lamellen, Textdisplay und ein roter Quittungsblock"),
        Bauform("leucht", "Leuchtmelder", 90, "Liegendes Kissen mit grün hinterleuchteter Anzeige und Pfeiltasten"),
        Bauform("bogen", "Bogenmelder", 150, "Farbschirm mit Kopfleiste, zwei Bogentasten und einer Wippe"),
        Bauform("uhr", "Einsatzuhr", 1, "Am Handgelenk: Stichwort, Ort, zwei Flächen", premium = true),
        Bauform("monitor", "Alarmmonitor", 1, "Bildschirm im Flur: Stichwort, Adresse und die Karte", premium = true),
    )

    fun bauform(id: String): Bauform? = BAUFORMEN.firstOrNull { it.id == id }

    // ------------------------------------------------------------- Gesichter

    /**
     * Ein Gesicht: Gehäuse und Display als benannte Ausführung.
     *
     * Es ist das eine Stück des Melders, das **am Konto** hängt (Profilfeld
     * `melderGesicht`) — alles andere hier gehört dem Gerät.
     */
    data class Gesicht(
        val id: String,
        val name: String,
        val abLevel: Int,
        val gehaeuse: Color,
        val lcd: Color,
        val preisCredits: Int? = null,
        val premium: Boolean = false,
    ) {
        fun frei(stufe: Int, premiumAktiv: Boolean, gekauft: Set<String>): Boolean = when {
            premium -> premiumAktiv
            preisCredits != null -> "melder-$id" in gekauft
            else -> stufe >= abLevel
        }

        fun sperrgrund(): String = when {
            premium -> "Premium"
            preisCredits != null -> "$preisCredits C"
            else -> "ab St. $abLevel"
        }
    }

    private fun g(id: String, name: String, ab: Int, geh: Long, lcd: Long, preis: Int? = null, premium: Boolean = false) =
        Gesicht(id, name, ab, Color(0xFF000000 or geh), Color(0xFF000000 or lcd), preis, premium)

    val GESICHTER = listOf(
        g("standard", "Dienstgerät", 1, 0x1b1f25, 0xa5b34c),
        g("bernstein", "Bernstein", 5, 0x1b1f25, 0xd19c3f),
        g("signalrot", "Signalrot", 10, 0x3d1512, 0xa5b34c),
        g("tiefblau", "Tiefblau", 18, 0x16223a, 0xa5b34c),
        g("eis", "Eisblau", 28, 0x1b1f25, 0xa3c6cf),
        g("nachtdienst", "Nachtdienst", 40, 0x0f1013, 0x17090b),
        g("chrom", "Chrom", 55, 0x6d7680, 0xa5b34c),
        g("gold", "Gold", 70, 0x83683a, 0xa5b34c),
        g("kupferfunk", "Kupferfunk", 80, 0x7d4930, 0xe1b768),
        g("jubilaeum", "Jubiläum", 100, 0x5f2030, 0xf0d27a),
        g("petrollicht", "Petrollicht", 120, 0x0d4c52, 0x91e3d0),
        g("polar", "Polar", 140, 0xd7e4ea, 0x31537f),
        g("leitstellenblau", "Leitstellenblau", 160, 0x182f52, 0x7fb7e8),
        g("morgenrot", "Morgenrot", 190, 0x762e35, 0xffb86b),
        g("legende", "Legende", 200, 0x171b24, 0xf6df82),

        g("neonorange", "Neonorange", 1, 0xb3491a, 0xffd23f, preis = 120),
        g("olivgruen", "Olivgrün", 1, 0x2e3524, 0xa5b34c, preis = 150),
        g("schneeweiss", "Schneeweiß", 1, 0xd7dade, 0x4c6eb3, preis = 230),
        g("petrol", "Petrol", 1, 0x0f5259, 0x9fd6c9, preis = 140),
        g("purpur", "Purpur", 1, 0x43185c, 0xe3b25c, preis = 170),
        g("sand", "Sand", 1, 0xa4936c, 0x3c3a26, preis = 120),
        g("mint", "Mint", 1, 0x1b1f25, 0x8fe3b0, preis = 140),
        g("graphit", "Graphit", 1, 0x33373d, 0xc2cad2, preis = 110),
        g("bordeaux", "Bordeaux", 1, 0x5c1f2e, 0xe3b25c, preis = 150),
        g("arktis", "Arktis", 1, 0xb9d4e0, 0x2e4a85, preis = 200),
        g("lavendel", "Lavendel", 1, 0x605080, 0xd8c4ee, preis = 170),
        g("kupferglanz", "Kupferglanz", 1, 0x9b5b35, 0xf0c071, preis = 180),
        g("tannengrün", "Tannengrün", 1, 0x193d31, 0x8fc99e, preis = 150),
        g("sonnengelb", "Sonnengelb", 1, 0xd39a22, 0x3d3010, preis = 140),

        g("premium", "Premium", 1, 0x10151c, 0xf4d35e, premium = true),
        g("premium-onyx", "Onyx", 1, 0x0b0c10, 0xc8ccd4, premium = true),
        g("premium-titanglanz", "Titanglanz", 1, 0x8b939e, 0x1b2028, premium = true),
        g("premium-aurora", "Aurora", 1, 0x152a3d, 0x7cf0c4, premium = true),
        g("premium-messingdienst", "Messingdienst", 1, 0x7a5d22, 0xf6e2a8, premium = true),
        g("premium-karbon", "Karbon", 1, 0x191c22, 0xdfe6f0, premium = true),
        g("premium-glutkern", "Glutkern", 1, 0x2b1108, 0xff9a4d, premium = true),
        g("premium-eisspiegel", "Eisspiegel", 1, 0xdce5ee, 0x1e4a78, premium = true),
        g("premium-purpurnacht", "Purpurnacht", 1, 0x251035, 0xf5a8d8, premium = true),
        g("premium-bernsteinglas", "Bernsteinglas", 1, 0x1b150f, 0xf0ab3c, premium = true),
    )

    fun gesicht(id: String?): Gesicht =
        GESICHTER.firstOrNull { it.id == id } ?: GESICHTER.first()

    /**
     * Der volle Farbsatz eines Gesichts — Gehäuse in drei Tönen und Rand,
     * Display in drei Tönen und Tinte.
     *
     * <b>Abgeleitet statt abgeschrieben.</b> Im Web stehen die vollen Sätze in
     * `styles/melder.css`, und dort sind sie fast überall dieselbe Rechnung:
     * etwas heller, etwas dunkler, ein Rand eine Stufe über dem Grund. Die
     * Tinte richtet sich nach der Helligkeit des Glases — dunkle Schrift auf
     * hellem Display, helle auf dunklem. Das eine Gesicht, das davon abweicht
     * (Nachtdienst: rote Tinte auf schwarzem Glas, damit nachts nichts blendet),
     * steht ausdrücklich da.
     */
    fun palette(id: String?): Melderpalette {
        val g = gesicht(id)
        val lcdHell = g.lcd.luminance() > 0.35f
        val tinte = when {
            g.id == "nachtdienst" -> Color(0xFFFF5546)
            lcdHell -> lerp(g.lcd, Color.Black, 0.86f)
            else -> lerp(g.lcd, Color.White, 0.88f)
        }
        return Melderpalette(
            gehHell = lerp(g.gehaeuse, Color.White, 0.12f),
            gehMittel = g.gehaeuse,
            gehTief = lerp(g.gehaeuse, Color.Black, 0.28f),
            gehRand = lerp(g.gehaeuse, Color.White, 0.22f),
            lcdHell = lerp(g.lcd, Color.White, 0.14f),
            lcdGrund = g.lcd,
            lcdRand = lerp(g.lcd, Color.Black, 0.38f),
            lcdTinte = tinte,
            gehHellFlaeche = g.gehaeuse.luminance() > 0.45f,
        )
    }

    // ----------------------------------------------------------------- Töne

    /** Ein Alarmton aus dem Katalog. */
    data class Ton(
        val id: String,
        val name: String,
        val abLevel: Int = 1,
        val preisCredits: Int? = null,
        val premium: Boolean = false,
    ) {
        /** Gekaufte Töne stehen im Besitzstand als Artikel `ton-<id>` — wie im Web. */
        fun frei(stufe: Int, premiumAktiv: Boolean, gekauft: Set<String>): Boolean = when {
            premium -> premiumAktiv
            preisCredits != null -> "ton-$id" in gekauft
            else -> stufe >= abLevel
        }

        fun sperrgrund(): String = when {
            premium -> "Premium"
            preisCredits != null -> "im Shop"
            else -> "ab St. $abLevel"
        }
    }

    val TOENE = listOf(
        Ton("zweiklang", "Zweiklang"),
        Ton("dreiklang", "Dreiklang"),
        Ton("warnton", "Warnton"),
        Ton("doppelton", "Doppelton", 3),
        Ton("wechselton", "Wechselton", 6),
        Ton("tacker", "Tacker", 9),
        Ton("steigton", "Steigton", 12),
        Ton("klassik", "Klassik 80er", 16),
        Ton("fallton", "Fallton", 18),
        Ton("triller", "Triller", 21),
        Ton("kaskade", "Kaskade", 24),
        Ton("nachtwache", "Nachtwache", 27),
        Ton("zirpen", "Zirpen", 30),
        Ton("digital", "Digital", 32),
        Ton("hornruf", "Hornruf", 34),
        Ton("taktschlag", "Taktschlag", 37),
        Ton("doppelschlag", "Doppelschlag", 40),
        Ton("wellenton", "Wellenton", 43),
        Ton("pfiff", "Pfiff", 46),
        Ton("glocke", "Glocke", 49),
        Ton("sprungton", "Sprungton", 52),
        Ton("schnellfolge", "Schnellfolge", 55),
        Ton("leitton", "Leitton", 58),
        Ton("stakkato", "Stakkato", 62),
        Ton("fanfare", "Fanfare", 66),
        Ton("grosslage", "Großlage", 68),
        Ton("kommando", "Kommando", 72),

        Ton("blinker", "Blinker", preisCredits = 100),
        Ton("puls", "Puls", preisCredits = 120),
        Ton("tiefton", "Tiefton", preisCredits = 120),
        Ton("morse", "Morsegruß", preisCredits = 150),
        Ton("feuerglocke", "Feuerglocke", preisCredits = 150),
        Ton("pendel", "Pendel", preisCredits = 165),
        Ton("sirene", "Sirene", preisCredits = 180),
        Ton("herzschlag", "Herzschlag", preisCredits = 180),
        Ton("funkruf", "Funkruf", preisCredits = 195),
        Ton("aufwecker", "Aufwecker", preisCredits = 210),
        Ton("weckuhr", "Weckuhr", preisCredits = 210),
        Ton("nebelhorn", "Nebelhorn", preisCredits = 230),
        Ton("gong", "Gong", preisCredits = 240),

        Ton("edelklang", "Edelklang", premium = true),
        Ton("nachtsignal", "Nachtsignal", premium = true),
        Ton("platinruf", "Platinruf", premium = true),
        Ton("sturmglocke", "Sturmglocke", premium = true),
        Ton("taktfeuer", "Taktfeuer", premium = true),
        Ton("hallruf", "Hallruf", premium = true),
        Ton("wachengong", "Wachen-Gong", premium = true),
        Ton("glutwelle", "Glutwelle", premium = true),
        Ton("silberton", "Silberton", premium = true),
        Ton("doppelhorn", "Doppelhorn", premium = true),
    )

    /** Der Ton, auf den alles zurückfällt — der, den jedes Konto kennt. */
    const val TON_STANDARD = "zweiklang"

    // -------------------------------------------------- Alarmierung, Profile

    data class Alarmierungsart(val id: String, val name: String, val was: String) {
        val ton: Boolean get() = id == "voll" || id == "ton"
        val vibration: Boolean get() = id == "voll" || id == "vibration"
    }

    val ALARMIERUNGSARTEN = listOf(
        Alarmierungsart("voll", "Ton und Vibration", "Der Regelfall im Dienst"),
        Alarmierungsart("ton", "Nur Ton", "Ohne Vibration — am Gürtel unauffälliger"),
        Alarmierungsart("vibration", "Nur Vibration", "Stumm. Den Alarm musst du sehen."),
        Alarmierungsart("stumm", "Stumm", "Weder Ton noch Vibration — nur die Anzeige"),
    )

    fun alarmierungsart(id: String): Alarmierungsart =
        ALARMIERUNGSARTEN.firstOrNull { it.id == id } ?: ALARMIERUNGSARTEN.first()

    data class Profil(val id: String, val name: String, val was: String, val art: String, val lautstaerke: Float)

    /** Alarmierungsart und Lautstärke unter einem Namen — dasselbe, was am Gerät im Displaykopf steht. */
    val PROFILE = listOf(
        Profil("vollalarm", "Vollalarm", "Laut, mit Vibration — der Dienstzustand", "voll", 1f),
        Profil("bereitschaft", "Bereitschaft", "Gedämpft, aber hörbar", "voll", 0.6f),
        Profil("nachtdienst", "Nachtdienst", "Leise und mit Vibration — weckt niemanden mit", "voll", 0.3f),
        Profil("still", "Stille Alarmierung", "Nur Vibration. Den Alarm musst du sehen.", "vibration", 0f),
    )
}

/** Der volle Farbsatz eines Gesichts — siehe [Melderkatalog.palette]. */
data class Melderpalette(
    val gehHell: Color,
    val gehMittel: Color,
    val gehTief: Color,
    val gehRand: Color,
    val lcdHell: Color,
    val lcdGrund: Color,
    val lcdRand: Color,
    val lcdTinte: Color,
    /** Ob das Gehäuse hell ist — dann steht die Beschriftung darauf dunkel. */
    val gehHellFlaeche: Boolean,
) {
    val aufGehaeuse: Color get() = if (gehHellFlaeche) Color(0xFF1B2028) else Color(0xFFC9D2DC)
}
