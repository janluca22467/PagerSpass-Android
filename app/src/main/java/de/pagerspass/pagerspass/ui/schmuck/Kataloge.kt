package de.pagerspass.pagerspass.ui.schmuck

import androidx.compose.ui.graphics.Color

/**
 * Die übrigen Kataloge aus `web/src/types.ts` — alles, was Laufbahn, Shop und
 * Premium-Schaufenster beschriften, ohne dass der Server es mitschickt.
 *
 * <b>Neben `Schmuck`, nicht darin.</b> `Schmuck` trägt, was die Profilseite zum
 * Anlegen braucht (Muster, Rahmen, Wappenzeichen mit Zeichnung). Hier steht, was
 * nur *benannt* wird: Melder-Gesichter und -Bauformen, Alarmtöne, Schichtkarten,
 * Titel, die Kauffarben und die Stufen der Wappenzeichen. Der Laufbahnpass liest
 * aus beiden — so wie im Web aus einer einzigen Datei.
 *
 * <b>Von Hand gleichzuhalten</b>, genau wie `Schmuck`: Ein Stück, das im Web
 * dazukommt, fehlt hier, bis es jemand einträgt — es fällt dann schlicht aus der
 * Liste, ohne Fehler.
 */
object Kataloge {

    /**
     * Ein Melder-Gesicht: Gehäuse- und Anzeigefarbe, dazu der Weg zu ihm.
     *
     * Die beiden Farben sind die der kleinen Vorschau im Web (`gehaeuse`, `lcd`) —
     * der volle Farbsatz eines Geräts steht dort in `styles/melder.css` und wird
     * hier nicht gebraucht: Pass und Laden zeigen nur die Miniatur.
     */
    data class Gesicht(
        val id: String,
        val name: String,
        val abLevel: Int,
        val gehaeuse: Color,
        val lcd: Color,
        val preisCredits: Int? = null,
        val premium: Boolean = false,
    )

    val MELDER_GESICHTER = listOf(
        Gesicht("standard", "Dienstgerät", 1, Color(0xFF1B1F25), Color(0xFFA5B34C)),
        Gesicht("bernstein", "Bernstein", 5, Color(0xFF1B1F25), Color(0xFFD19C3F)),
        Gesicht("signalrot", "Signalrot", 10, Color(0xFF3D1512), Color(0xFFA5B34C)),
        Gesicht("tiefblau", "Tiefblau", 18, Color(0xFF16223A), Color(0xFFA5B34C)),
        Gesicht("eis", "Eisblau", 28, Color(0xFF1B1F25), Color(0xFFA3C6CF)),
        Gesicht("nachtdienst", "Nachtdienst", 40, Color(0xFF0F1013), Color(0xFF17090B)),
        Gesicht("chrom", "Chrom", 55, Color(0xFF6D7680), Color(0xFFA5B34C)),
        Gesicht("gold", "Gold", 70, Color(0xFF83683A), Color(0xFFA5B34C)),
        Gesicht("kupferfunk", "Kupferfunk", 80, Color(0xFF7D4930), Color(0xFFE1B768)),
        Gesicht("jubilaeum", "Jubiläum", 100, Color(0xFF5F2030), Color(0xFFF0D27A)),
        Gesicht("petrollicht", "Petrollicht", 120, Color(0xFF0D4C52), Color(0xFF91E3D0)),
        Gesicht("polar", "Polar", 140, Color(0xFFD7E4EA), Color(0xFF31537F)),
        Gesicht("leitstellenblau", "Leitstellenblau", 160, Color(0xFF182F52), Color(0xFF7FB7E8)),
        Gesicht("morgenrot", "Morgenrot", 190, Color(0xFF762E35), Color(0xFFFFB86B)),
        Gesicht("legende", "Legende", 200, Color(0xFF171B24), Color(0xFFF6DF82)),
        // Die Shop-Gesichter — gekauft, nicht erspielt (Schatzkammer.cs).
        Gesicht("neonorange", "Neonorange", 1, Color(0xFFB3491A), Color(0xFFFFD23F), 120),
        Gesicht("olivgruen", "Olivgrün", 1, Color(0xFF2E3524), Color(0xFFA5B34C), 150),
        Gesicht("schneeweiss", "Schneeweiß", 1, Color(0xFFD7DADE), Color(0xFF4C6EB3), 230),
        Gesicht("petrol", "Petrol", 1, Color(0xFF0F5259), Color(0xFF9FD6C9), 140),
        Gesicht("purpur", "Purpur", 1, Color(0xFF43185C), Color(0xFFE3B25C), 170),
        Gesicht("sand", "Sand", 1, Color(0xFFA4936C), Color(0xFF3C3A26), 120),
        Gesicht("mint", "Mint", 1, Color(0xFF1B1F25), Color(0xFF8FE3B0), 140),
        Gesicht("graphit", "Graphit", 1, Color(0xFF33373D), Color(0xFFC2CAD2), 110),
        Gesicht("bordeaux", "Bordeaux", 1, Color(0xFF5C1F2E), Color(0xFFE3B25C), 150),
        Gesicht("arktis", "Arktis", 1, Color(0xFFB9D4E0), Color(0xFF2E4A85), 200),
        Gesicht("lavendel", "Lavendel", 1, Color(0xFF605080), Color(0xFFD8C4EE), 170),
        Gesicht("kupferglanz", "Kupferglanz", 1, Color(0xFF9B5B35), Color(0xFFF0C071), 180),
        Gesicht("tannengrün", "Tannengrün", 1, Color(0xFF193D31), Color(0xFF8FC99E), 150),
        Gesicht("sonnengelb", "Sonnengelb", 1, Color(0xFFD39A22), Color(0xFF3D3010), 140),
        // Premium: Abo-Stück, kein Credit-Artikel.
        Gesicht("premium", "Premium", 1, Color(0xFF10151C), Color(0xFFF4D35E), premium = true),
        Gesicht("premium-onyx", "Onyx", 1, Color(0xFF0B0C10), Color(0xFFC8CCD4), premium = true),
        Gesicht("premium-titanglanz", "Titanglanz", 1, Color(0xFF8B939E), Color(0xFF1B2028), premium = true),
        Gesicht("premium-aurora", "Aurora", 1, Color(0xFF152A3D), Color(0xFF7CF0C4), premium = true),
        Gesicht("premium-messingdienst", "Messingdienst", 1, Color(0xFF7A5D22), Color(0xFFF6E2A8), premium = true),
        Gesicht("premium-karbon", "Karbon", 1, Color(0xFF191C22), Color(0xFFDFE6F0), premium = true),
        Gesicht("premium-glutkern", "Glutkern", 1, Color(0xFF2B1108), Color(0xFFFF9A4D), premium = true),
        Gesicht("premium-eisspiegel", "Eisspiegel", 1, Color(0xFFDCE5EE), Color(0xFF1E4A78), premium = true),
        Gesicht("premium-purpurnacht", "Purpurnacht", 1, Color(0xFF251035), Color(0xFFF5A8D8), premium = true),
        Gesicht("premium-bernsteinglas", "Bernsteinglas", 1, Color(0xFF1B150F), Color(0xFFF0AB3C), premium = true),
    )

    /**
     * Die Alarmtöne des Melders. Die Id ist zugleich der Schlüssel in
     * `Meldertonprobe` — dort steht, wie jeder klingt.
     */
    val MELDER_TOENE = listOf(
        Schmuck.Stueck("zweiklang", "Zweiklang", 1),
        Schmuck.Stueck("dreiklang", "Dreiklang", 1),
        Schmuck.Stueck("warnton", "Warnton", 1),
        Schmuck.Stueck("doppelton", "Doppelton", 3),
        Schmuck.Stueck("wechselton", "Wechselton", 6),
        Schmuck.Stueck("tacker", "Tacker", 9),
        Schmuck.Stueck("steigton", "Steigton", 12),
        Schmuck.Stueck("klassik", "Klassik 80er", 16),
        Schmuck.Stueck("fallton", "Fallton", 18),
        Schmuck.Stueck("triller", "Triller", 21),
        Schmuck.Stueck("kaskade", "Kaskade", 24),
        Schmuck.Stueck("nachtwache", "Nachtwache", 27),
        Schmuck.Stueck("zirpen", "Zirpen", 30),
        Schmuck.Stueck("digital", "Digital", 32),
        Schmuck.Stueck("hornruf", "Hornruf", 34),
        Schmuck.Stueck("taktschlag", "Taktschlag", 37),
        Schmuck.Stueck("doppelschlag", "Doppelschlag", 40),
        Schmuck.Stueck("wellenton", "Wellenton", 43),
        Schmuck.Stueck("pfiff", "Pfiff", 46),
        Schmuck.Stueck("glocke", "Glocke", 49),
        Schmuck.Stueck("sprungton", "Sprungton", 52),
        Schmuck.Stueck("schnellfolge", "Schnellfolge", 55),
        Schmuck.Stueck("leitton", "Leitton", 58),
        Schmuck.Stueck("stakkato", "Stakkato", 62),
        Schmuck.Stueck("fanfare", "Fanfare", 66),
        Schmuck.Stueck("grosslage", "Großlage", 68),
        Schmuck.Stueck("kommando", "Kommando", 72),
        Schmuck.Stueck("blinker", "Blinker", 1, 100),
        Schmuck.Stueck("puls", "Puls", 1, 120),
        Schmuck.Stueck("tiefton", "Tiefton", 1, 120),
        Schmuck.Stueck("morse", "Morsegruß", 1, 150),
        Schmuck.Stueck("feuerglocke", "Feuerglocke", 1, 150),
        Schmuck.Stueck("pendel", "Pendel", 1, 165),
        Schmuck.Stueck("sirene", "Sirene", 1, 180),
        Schmuck.Stueck("herzschlag", "Herzschlag", 1, 180),
        Schmuck.Stueck("funkruf", "Funkruf", 1, 195),
        Schmuck.Stueck("aufwecker", "Aufwecker", 1, 210),
        Schmuck.Stueck("weckuhr", "Weckuhr", 1, 210),
        Schmuck.Stueck("nebelhorn", "Nebelhorn", 1, 230),
        Schmuck.Stueck("gong", "Gong", 1, 240),
        Schmuck.Stueck("edelklang", "Edelklang", 1, premium = true),
        Schmuck.Stueck("nachtsignal", "Nachtsignal", 1, premium = true),
        Schmuck.Stueck("platinruf", "Platinruf", 1, premium = true),
        Schmuck.Stueck("sturmglocke", "Sturmglocke", 1, premium = true),
        Schmuck.Stueck("taktfeuer", "Taktfeuer", 1, premium = true),
        Schmuck.Stueck("hallruf", "Hallruf", 1, premium = true),
        Schmuck.Stueck("wachengong", "Wachen-Gong", 1, premium = true),
        Schmuck.Stueck("glutwelle", "Glutwelle", 1, premium = true),
        Schmuck.Stueck("silberton", "Silberton", 1, premium = true),
        Schmuck.Stueck("doppelhorn", "Doppelhorn", 1, premium = true),
    )

    /** Eine Melder-Bauform — eine andere Art Gerät, nicht bloß eine Farbe. */
    data class Bauform(
        val id: String,
        val name: String,
        val abLevel: Int,
        val erklaerung: String,
        val premium: Boolean = false,
    )

    val MELDER_BAUFORMEN = listOf(
        Bauform("dienst", "Dienst", 1, "Dienstgerät mit LC-Display"),
        Bauform("klassik", "Klassik", 10, "Kleiner Piepser mit gelber Quittungstaste"),
        Bauform("farbe", "Farbe", 15, "Farbdisplay mit Annehmen und Ablehnen"),
        Bauform("fax", "Alarmfax", 75, "Druckt die Meldung — abreißen quittiert"),
        Bauform(
            "quad", "Quermelder", 40,
            "Querformat, Tasten oben auf dem Rücken — die Meldung in ganzen Zeilen",
            premium = true,
        ),
        Bauform(
            "lamellen", "Lamellenmelder", 120,
            "Vier gewölbte Lamellen, Textdisplay und ein roter Quittungsblock",
        ),
        Bauform(
            "leucht", "Leuchtmelder", 90,
            "Liegendes Kissen mit grün hinterleuchteter Anzeige und Pfeiltasten",
        ),
        Bauform(
            "bogen", "Bogenmelder", 150,
            "Farbschirm mit Kopfleiste, zwei Bogentasten und einer Wippe",
        ),
        Bauform("uhr", "Einsatzuhr", 1, "Am Handgelenk: Stichwort, Ort, zwei Flächen", premium = true),
        Bauform(
            "monitor", "Alarmmonitor", 1,
            "Bildschirm im Flur: Stichwort, Adresse und die Karte",
            premium = true,
        ),
    )

    /** Die Gehäuse des Funkgeräts — nur Farbe, am Funk ändert sich nichts. */
    val FUNKGERAETE = listOf(
        Schmuck.Stueck("standard", "Dienstgerät", 1),
        Schmuck.Stueck("premium-nachtwache", "Nachtwache", 1, premium = true),
        Schmuck.Stueck("premium-signalgelb", "Signalgelb", 1, premium = true),
        Schmuck.Stueck("premium-stahlblau", "Stahlblau", 1, premium = true),
        Schmuck.Stueck("premium-messing", "Messing", 1, premium = true),
        Schmuck.Stueck("premium-nachtglut", "Nachtglut", 1, premium = true),
        Schmuck.Stueck("premium-polar", "Polarweiß", 1, premium = true),
        Schmuck.Stueck("premium-oliv", "Oliv", 1, premium = true),
        Schmuck.Stueck("premium-karbon", "Karbon", 1, premium = true),
    )

    /** Die Gestaltungen der Schichtkarte — Laufbahn ab 25 und 50, der Rest am Abo. */
    val SCHICHTKARTEN_DESIGNS = listOf(
        Schmuck.Stueck("standard", "Dunkel", 1),
        Schmuck.Stueck("bernstein", "Bernstein-LCD", 25),
        Schmuck.Stueck("hell", "Heller Aushang", 50),
        Schmuck.Stueck("premium-leitstelle", "Leitstellennacht", 1, premium = true),
        Schmuck.Stueck("premium-signalrot", "Signalrot", 1, premium = true),
        Schmuck.Stueck("premium-blaulicht", "Blaue Stunde", 1, premium = true),
        Schmuck.Stueck("premium-wachengold", "Wachengold", 1, premium = true),
        Schmuck.Stueck("premium-waldwacht", "Waldwacht", 1, premium = true),
        Schmuck.Stueck("premium-morgenlicht", "Morgenlicht", 1, premium = true),
    )

    /** Die kaufbaren Titel — sie stehen unter dem Namen. */
    val PROFILTITEL = listOf(
        Schmuck.Stueck("ehrenamt", "Ehrenamt", 1, 150),
        Schmuck.Stueck("nachtschicht", "Nachtschicht", 1, 180),
        Schmuck.Stueck("funkfuchs", "Funkfuchs", 1, 210),
        Schmuck.Stueck("blaulichtfan", "Blaulichtfan", 1, 150),
        Schmuck.Stueck("kaffeekasse", "Kaffeekasse", 1, 120),
        Schmuck.Stueck("urgestein", "Urgestein", 1, 240),
        Schmuck.Stueck("leitstellenlegende", "Leitstellenlegende", 1, 270),
        Schmuck.Stueck("fruehschicht", "Frühschicht", 1, 140),
        Schmuck.Stueck("helferherz", "Helferherz", 1, 200),
        Schmuck.Stueck("lageprofi", "Lageprofi", 1, 230),
    )

    /** Die vergebenen Titel — von der Verwaltung oder aus dem Abo, nie gekauft. */
    val VERGABETITEL = listOf(
        "team" to "Team",
        "helfer" to "Helfer",
        "betatester" to "Betatester",
        "jugendverband" to "Jugendverband",
        "premium" to "Premium",
        "premium-einsatzbereit" to "Premium · Einsatzbereit",
        "premium-leitstelle" to "Premium · Leitstelle",
        "premium-nachtdienst" to "Premium · Nachtdienst",
        "premium-stammbesatzung" to "Premium · Stammbesatzung",
        "premium-goenner" to "Premium · Gönner",
    )

    /** Welche der vergebenen Titel am Abo hängen. */
    val PREMIUM_TITEL_IDS = listOf(
        "premium",
        "premium-einsatzbereit",
        "premium-leitstelle",
        "premium-nachtdienst",
        "premium-stammbesatzung",
        "premium-goenner",
    )

    /**
     * Die Wappenfarben aus dem Laden — ihr Wert ist der Platz in
     * `Wappen.PALETTE`, und genau so steht er als `stueckId` am Shopartikel.
     */
    val WAPPEN_KAUFFARBEN = listOf(
        Triple(9, "Tiefrot", 90),
        Triple(10, "Waldgrün", 90),
        Triple(11, "Ozean", 90),
        Triple(12, "Anthrazit", 90),
    )

    /**
     * Ab welcher Stufe ein Wappenzeichen offen ist. Die Abo-Zeichen stehen mit 1
     * darin, damit die Tafel vollständig bleibt — sie haben keine Stufe.
     */
    val WAPPEN_AB_LEVEL = linkedMapOf(
        "Keines" to 1,
        "Feuerwehr" to 1,
        "Rettungsdienst" to 1,
        "Thw" to 1,
        "Polizei" to 1,
        "Drehleiter" to 15,
        "Rth" to 30,
        "Boot" to 45,
        "Funkmast" to 58,
        "Leitstelle" to 1,
        "Bergwacht" to 1,
        "Wasserrettung" to 1,
    )

    val WAPPEN_LABEL = mapOf(
        "Keines" to "ohne",
        "Feuerwehr" to "Feuerwehr",
        "Rettungsdienst" to "Rettungsdienst",
        "Thw" to "THW",
        "Polizei" to "Polizei",
        "Drehleiter" to "Drehleiter",
        "Rth" to "Rettungshubschrauber",
        "Boot" to "Rettungsboot",
        "Funkmast" to "Funkmast",
        "Leitstelle" to "Leitstelle",
        "Bergwacht" to "Bergwacht",
        "Wasserrettung" to "Wasserrettung",
    )

    /** Ab welcher Stufe die Wappenfarbe Gold (Platz 8) offen ist. */
    const val GOLDFARBE_AB_LEVEL = 70

    fun gesicht(id: String): Gesicht? = MELDER_GESICHTER.firstOrNull { it.id == id }

    fun ton(id: String): Schmuck.Stueck? = MELDER_TOENE.firstOrNull { it.id == id }

    fun titel(id: String): String? =
        PROFILTITEL.firstOrNull { it.id == id }?.name
            ?: VERGABETITEL.firstOrNull { it.first == id }?.second
}
