package de.pagerspass.pagerspass.ui.schmuck

import androidx.compose.ui.graphics.Color

/**
 * Die Kataloge des Profileditors, die `Schmuck.kt` nicht führt — Wappen mit
 * Stufen, Farben, Titel. Übertragen aus `web/src/types.ts`.
 *
 * <b>Von Hand gleichzuhalten</b>, wie die Kataloge in `Schmuck.kt`: Der Server
 * liefert sie nicht mit. Was im Web dazukommt, fehlt hier, bis es jemand einträgt.
 */
object Profilkatalog {

    /** Ein Wappenzeichen — frei, über die Stufe oder am Abo (`WAPPEN_*`). */
    data class Wappenwahl(
        val wert: String,
        val name: String,
        val abLevel: Int = 1,
        val premium: Boolean = false,
    )

    val WAPPEN = listOf(
        Wappenwahl("Keines", "ohne"),
        Wappenwahl("Feuerwehr", "Feuerwehr"),
        Wappenwahl("Rettungsdienst", "Rettungsdienst"),
        Wappenwahl("Thw", "THW"),
        Wappenwahl("Polizei", "Polizei"),
        Wappenwahl("Drehleiter", "Drehleiter", abLevel = 15),
        Wappenwahl("Rth", "Rettungshubschrauber", abLevel = 30),
        Wappenwahl("Boot", "Rettungsboot", abLevel = 45),
        Wappenwahl("Funkmast", "Funkmast", abLevel = 58),
        Wappenwahl("Leitstelle", "Leitstelle", premium = true),
        Wappenwahl("Bergwacht", "Bergwacht", premium = true),
        Wappenwahl("Wasserrettung", "Wasserrettung", premium = true),
    )

    /** Die neunte Farbe ist Gold — die einzige erspielte (`GOLDFARBE`). */
    const val GOLDFARBE = 8
    const val GOLDFARBE_AB_LEVEL = 70

    /** Die vier Kauffarben dahinter — im Shop als `farbe-9` … `farbe-12`. */
    val KAUFFARBEN: Map<Int, String> = mapOf(
        9 to "Tiefrot",
        10 to "Waldgrün",
        11 to "Ozean",
        12 to "Anthrazit",
    )

    /** Wie eine Farbe heißt. Die acht freien tragen keinen Namen — nur eine Nummer. */
    fun farbname(farbe: Int): String = when {
        farbe == GOLDFARBE -> "Gold"
        else -> KAUFFARBEN[farbe] ?: "Farbe ${farbe + 1}"
    }

    /** Ein Titel unter dem Namen. */
    data class Titel(val id: String, val name: String, val preisCredits: Int? = null)

    /** Die Shop-Titel (`PROFILTITEL`) — im Shop als `titel-<id>`. */
    val TITEL = listOf(
        Titel("ehrenamt", "Ehrenamt", 150),
        Titel("nachtschicht", "Nachtschicht", 180),
        Titel("funkfuchs", "Funkfuchs", 210),
        Titel("blaulichtfan", "Blaulichtfan", 150),
        Titel("kaffeekasse", "Kaffeekasse", 120),
        Titel("urgestein", "Urgestein", 240),
        Titel("leitstellenlegende", "Leitstellenlegende", 270),
        Titel("fruehschicht", "Frühschicht", 140),
        Titel("helferherz", "Helferherz", 200),
        Titel("lageprofi", "Lageprofi", 230),
    )

    /** Die von Hand verliehenen Titel (`VERGABETITEL`) samt der Abo-Titel. */
    val VERGABETITEL = listOf(
        Titel("team", "Team"),
        Titel("helfer", "Helfer"),
        Titel("betatester", "Betatester"),
        Titel("jugendverband", "Jugendverband"),
        Titel("premium", "Premium"),
        Titel("premium-einsatzbereit", "Premium · Einsatzbereit"),
        Titel("premium-leitstelle", "Premium · Leitstelle"),
        Titel("premium-nachtdienst", "Premium · Nachtdienst"),
        Titel("premium-stammbesatzung", "Premium · Stammbesatzung"),
        Titel("premium-goenner", "Premium · Gönner"),
    )

    /** `PREMIUM_TITEL_IDS` — der Spiegel von `Premium.TitelIds` am Server. */
    val PREMIUM_TITEL = setOf(
        "premium",
        "premium-einsatzbereit",
        "premium-leitstelle",
        "premium-nachtdienst",
        "premium-stammbesatzung",
        "premium-goenner",
    )

    /** Wie ein Titel heißt — oder nichts, wenn keiner getragen wird. */
    fun titelname(id: String?): String? {
        if (id.isNullOrBlank()) return null
        return (TITEL + VERGABETITEL).firstOrNull { it.id == id }?.name
    }

    /** Die drei Sichtbarkeiten eines neuen Brett-Beitrags — Wert und Aufschrift. */
    val SICHTBARKEITEN = listOf(
        "Freunde" to "Freunde",
        "Wache" to "Freunde und Wache",
        "Oeffentlich" to "Alle",
    )
}

/**
 * Die Melder-Kataloge — Bauform, Gesicht, Ton, Alarmierung, Profil.
 * Übertragen aus `MELDER_BAUFORMEN`, `MELDER_GESICHTER`, `MELDER_TOENE`,
 * `ALARMIERUNGSARTEN` und `MELDERPROFILE` in `web/src/types.ts`.
 *
 * <b>Bauform, Ton, Alarmierung und Profil gelten nur für dieses Gerät</b> (siehe
 * `Ablage`); das Gesicht gehört zum Konto und geht mit „Speichern" an den Server.
 */
object Melderkatalog {

    data class Bauform(
        val id: String,
        val name: String,
        val erklaerung: String,
        val abLevel: Int = 1,
        val premium: Boolean = false,
    )

    val BAUFORMEN = listOf(
        Bauform("dienst", "Dienst", "Dienstgerät mit LC-Display"),
        Bauform("klassik", "Klassik", "Kleiner Piepser mit gelber Quittungstaste", 10),
        Bauform("farbe", "Farbe", "Farbdisplay mit Annehmen und Ablehnen", 15),
        Bauform("fax", "Alarmfax", "Druckt die Meldung — abreißen quittiert", 75),
        Bauform(
            "quad",
            "Quermelder",
            "Querformat, Tasten oben auf dem Rücken — die Meldung in ganzen Zeilen",
            40,
            premium = true,
        ),
        Bauform(
            "lamellen",
            "Lamellenmelder",
            "Vier gewölbte Lamellen, Textdisplay und ein roter Quittungsblock",
            120,
        ),
        Bauform(
            "leucht",
            "Leuchtmelder",
            "Liegendes Kissen mit grün hinterleuchteter Anzeige und Pfeiltasten",
            90,
        ),
        Bauform("bogen", "Bogenmelder", "Farbschirm mit Kopfleiste, zwei Bogentasten und einer Wippe", 150),
        Bauform("uhr", "Einsatzuhr", "Am Handgelenk: Stichwort, Ort, zwei Flächen", 1, premium = true),
    )

    /** Ein Melder-Gesicht: Gehäuse und Display. Im Shop als `melder-<id>`. */
    data class Gesicht(
        val id: String,
        val name: String,
        val gehaeuse: Color,
        val lcd: Color,
        val abLevel: Int = 1,
        val preisCredits: Int? = null,
        val premium: Boolean = false,
    )

    val GESICHTER = listOf(
        Gesicht("standard", "Dienstgerät", Color(0xFF1B1F25), Color(0xFFA5B34C)),
        Gesicht("bernstein", "Bernstein", Color(0xFF1B1F25), Color(0xFFD19C3F), 5),
        Gesicht("signalrot", "Signalrot", Color(0xFF3D1512), Color(0xFFA5B34C), 10),
        Gesicht("tiefblau", "Tiefblau", Color(0xFF16223A), Color(0xFFA5B34C), 18),
        Gesicht("eis", "Eisblau", Color(0xFF1B1F25), Color(0xFFA3C6CF), 28),
        Gesicht("nachtdienst", "Nachtdienst", Color(0xFF0F1013), Color(0xFF17090B), 40),
        Gesicht("chrom", "Chrom", Color(0xFF6D7680), Color(0xFFA5B34C), 55),
        Gesicht("gold", "Gold", Color(0xFF83683A), Color(0xFFA5B34C), 70),
        Gesicht("kupferfunk", "Kupferfunk", Color(0xFF7D4930), Color(0xFFE1B768), 80),
        Gesicht("jubilaeum", "Jubiläum", Color(0xFF5F2030), Color(0xFFF0D27A), 100),
        Gesicht("petrollicht", "Petrollicht", Color(0xFF0D4C52), Color(0xFF91E3D0), 120),
        Gesicht("polar", "Polar", Color(0xFFD7E4EA), Color(0xFF31537F), 140),
        Gesicht("leitstellenblau", "Leitstellenblau", Color(0xFF182F52), Color(0xFF7FB7E8), 160),
        Gesicht("morgenrot", "Morgenrot", Color(0xFF762E35), Color(0xFFFFB86B), 190),
        Gesicht("legende", "Legende", Color(0xFF171B24), Color(0xFFF6DF82), 200),
        Gesicht("neonorange", "Neonorange", Color(0xFFB3491A), Color(0xFFFFD23F), preisCredits = 120),
        Gesicht("olivgruen", "Olivgrün", Color(0xFF2E3524), Color(0xFFA5B34C), preisCredits = 150),
        Gesicht("schneeweiss", "Schneeweiß", Color(0xFFD7DADE), Color(0xFF4C6EB3), preisCredits = 230),
        Gesicht("petrol", "Petrol", Color(0xFF0F5259), Color(0xFF9FD6C9), preisCredits = 140),
        Gesicht("purpur", "Purpur", Color(0xFF43185C), Color(0xFFE3B25C), preisCredits = 170),
        Gesicht("sand", "Sand", Color(0xFFA4936C), Color(0xFF3C3A26), preisCredits = 120),
        Gesicht("mint", "Mint", Color(0xFF1B1F25), Color(0xFF8FE3B0), preisCredits = 140),
        Gesicht("graphit", "Graphit", Color(0xFF33373D), Color(0xFFC2CAD2), preisCredits = 110),
        Gesicht("bordeaux", "Bordeaux", Color(0xFF5C1F2E), Color(0xFFE3B25C), preisCredits = 150),
        Gesicht("arktis", "Arktis", Color(0xFFB9D4E0), Color(0xFF2E4A85), preisCredits = 200),
        Gesicht("lavendel", "Lavendel", Color(0xFF605080), Color(0xFFD8C4EE), preisCredits = 170),
        Gesicht("kupferglanz", "Kupferglanz", Color(0xFF9B5B35), Color(0xFFF0C071), preisCredits = 180),
        Gesicht("tannengrün", "Tannengrün", Color(0xFF193D31), Color(0xFF8FC99E), preisCredits = 150),
        Gesicht("sonnengelb", "Sonnengelb", Color(0xFFD39A22), Color(0xFF3D3010), preisCredits = 140),
        Gesicht("premium", "Premium", Color(0xFF10151C), Color(0xFFF4D35E), premium = true),
        Gesicht("premium-onyx", "Onyx", Color(0xFF0B0C10), Color(0xFFC8CCD4), premium = true),
        Gesicht("premium-titanglanz", "Titanglanz", Color(0xFF8B939E), Color(0xFF1B2028), premium = true),
        Gesicht("premium-aurora", "Aurora", Color(0xFF152A3D), Color(0xFF7CF0C4), premium = true),
        Gesicht("premium-messingdienst", "Messingdienst", Color(0xFF7A5D22), Color(0xFFF6E2A8), premium = true),
        Gesicht("premium-karbon", "Karbon", Color(0xFF191C22), Color(0xFFDFE6F0), premium = true),
        Gesicht("premium-glutkern", "Glutkern", Color(0xFF2B1108), Color(0xFFFF9A4D), premium = true),
        Gesicht("premium-eisspiegel", "Eisspiegel", Color(0xFFDCE5EE), Color(0xFF1E4A78), premium = true),
        Gesicht("premium-purpurnacht", "Purpurnacht", Color(0xFF251035), Color(0xFFF5A8D8), premium = true),
        Gesicht("premium-bernsteinglas", "Bernsteinglas", Color(0xFF1B150F), Color(0xFFF0AB3C), premium = true),
    )

    /** Ein Alarmton. Im Shop als `ton-<id>`. */
    data class Ton(
        val id: String,
        val name: String,
        val abLevel: Int = 1,
        val preisCredits: Int? = null,
        val premium: Boolean = false,
    )

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

    /** Wie ein Melder einen Alarm ankündigt — Id, Name, Erklärung. */
    data class Alarmierung(val id: String, val name: String, val was: String)

    val ALARMIERUNGSARTEN = listOf(
        Alarmierung("voll", "Ton und Vibration", "Der Regelfall im Dienst"),
        Alarmierung("ton", "Nur Ton", "Ohne Vibration — am Gürtel unauffälliger"),
        Alarmierung("vibration", "Nur Vibration", "Stumm. Den Alarm musst du sehen."),
        Alarmierung("stumm", "Stumm", "Weder Ton noch Vibration — nur die Anzeige"),
    )

    /** Alarmierungsart und Lautstärke unter einem Namen. */
    data class Profil(
        val id: String,
        val name: String,
        val was: String,
        val art: String,
        val lautstaerke: Float,
    )

    val PROFILE = listOf(
        Profil("vollalarm", "Vollalarm", "Laut, mit Vibration — der Dienstzustand", "voll", 1f),
        Profil("bereitschaft", "Bereitschaft", "Gedämpft, aber hörbar", "voll", 0.6f),
        Profil("nachtdienst", "Nachtdienst", "Leise und mit Vibration — weckt niemanden mit", "voll", 0.3f),
        Profil("still", "Stille Alarmierung", "Nur Vibration. Den Alarm musst du sehen.", "vibration", 0f),
    )
}
