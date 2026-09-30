package de.pagerspass.pagerspass.ansichten.welt

import java.text.Normalizer

/**
 * Die Städte der Gründungskarte — übertragen aus `components/welt/ortsnamen.ts`.
 *
 * <b>Eine feste Liste und keine Ortssuche beim Kartendienst.</b> Die Suche soll
 * ohne Einwilligung in fremde Server auskommen (dieselbe Regel wie bei den
 * Kacheln), und für „wo gründe ich“ reichen die Städte, die man kennt:
 * Hineinzoomen und Tippen setzt jeden anderen Ort.
 *
 * <b>Nach Größe sortiert, in vier Rängen.</b> Der Rang sagt, ab welcher
 * Zoomstufe der Name auf der Karte steht; die Reihenfolge entscheidet bei einem
 * Suchtreffer mit mehreren Kandidaten — die größere Stadt gewinnt.
 */
data class Stadt(val name: String, val lat: Double, val lon: Double, val rang: Int)

/** Ab welcher Zoomstufe ein Rang beschriftet wird — darunter wäre es nur Gedränge. */
fun stadtAbZoom(rang: Int): Int = when (rang) {
    1 -> 5
    2 -> 6
    3 -> 7
    else -> 8
}

val STAEDTE: List<Stadt> = listOf(
    Stadt("Berlin", 52.52, 13.405, 1),
    Stadt("Hamburg", 53.551, 9.994, 1),
    Stadt("München", 48.137, 11.575, 1),
    Stadt("Köln", 50.938, 6.96, 1),
    Stadt("Frankfurt am Main", 50.11, 8.682, 1),
    Stadt("Stuttgart", 48.776, 9.183, 1),
    Stadt("Düsseldorf", 51.227, 6.774, 1),
    Stadt("Leipzig", 51.34, 12.375, 1),
    Stadt("Dortmund", 51.514, 7.466, 1),
    Stadt("Essen", 51.456, 7.012, 1),
    Stadt("Bremen", 53.079, 8.802, 1),
    Stadt("Dresden", 51.051, 13.738, 1),
    Stadt("Hannover", 52.376, 9.732, 1),
    Stadt("Nürnberg", 49.452, 11.077, 1),
    Stadt("Duisburg", 51.435, 6.763, 1),
    Stadt("Bochum", 51.482, 7.216, 2),
    Stadt("Wuppertal", 51.256, 7.151, 2),
    Stadt("Bielefeld", 52.03, 8.532, 2),
    Stadt("Bonn", 50.737, 7.098, 2),
    Stadt("Münster", 51.961, 7.626, 2),
    Stadt("Mannheim", 49.488, 8.466, 2),
    Stadt("Karlsruhe", 49.007, 8.404, 2),
    Stadt("Augsburg", 48.371, 10.898, 2),
    Stadt("Wiesbaden", 50.078, 8.24, 2),
    Stadt("Mönchengladbach", 51.185, 6.442, 2),
    Stadt("Gelsenkirchen", 51.518, 7.086, 2),
    Stadt("Aachen", 50.776, 6.084, 2),
    Stadt("Braunschweig", 52.269, 10.521, 2),
    Stadt("Kiel", 54.323, 10.123, 2),
    Stadt("Chemnitz", 50.833, 12.925, 2),
    Stadt("Halle (Saale)", 51.482, 11.97, 2),
    Stadt("Magdeburg", 52.121, 11.628, 2),
    Stadt("Freiburg im Breisgau", 47.999, 7.842, 2),
    Stadt("Krefeld", 51.339, 6.586, 2),
    Stadt("Mainz", 50.0, 8.271, 2),
    Stadt("Lübeck", 53.866, 10.687, 2),
    Stadt("Erfurt", 50.978, 11.029, 2),
    Stadt("Oberhausen", 51.47, 6.852, 2),
    Stadt("Rostock", 54.092, 12.099, 2),
    Stadt("Kassel", 51.312, 9.48, 2),
    Stadt("Hagen", 51.367, 7.463, 3),
    Stadt("Potsdam", 52.391, 13.065, 3),
    Stadt("Saarbrücken", 49.24, 6.997, 3),
    Stadt("Hamm", 51.681, 7.815, 3),
    Stadt("Ludwigshafen am Rhein", 49.477, 8.445, 3),
    Stadt("Mülheim an der Ruhr", 51.427, 6.883, 3),
    Stadt("Oldenburg", 53.144, 8.214, 3),
    Stadt("Osnabrück", 52.279, 8.047, 3),
    Stadt("Leverkusen", 51.046, 6.984, 3),
    Stadt("Darmstadt", 49.873, 8.651, 3),
    Stadt("Heidelberg", 49.399, 8.672, 3),
    Stadt("Solingen", 51.171, 7.083, 3),
    Stadt("Herne", 51.538, 7.22, 3),
    Stadt("Neuss", 51.198, 6.691, 3),
    Stadt("Regensburg", 49.013, 12.101, 3),
    Stadt("Paderborn", 51.719, 8.754, 3),
    Stadt("Ingolstadt", 48.766, 11.426, 3),
    Stadt("Offenbach am Main", 50.1, 8.766, 3),
    Stadt("Fürth", 49.477, 10.989, 3),
    Stadt("Würzburg", 49.792, 9.953, 3),
    Stadt("Ulm", 48.401, 9.988, 3),
    Stadt("Heilbronn", 49.142, 9.219, 3),
    Stadt("Pforzheim", 48.892, 8.694, 3),
    Stadt("Wolfsburg", 52.423, 10.787, 3),
    Stadt("Göttingen", 51.541, 9.916, 3),
    Stadt("Bottrop", 51.524, 6.929, 3),
    Stadt("Reutlingen", 48.491, 9.211, 3),
    Stadt("Erlangen", 49.598, 11.004, 3),
    Stadt("Bremerhaven", 53.54, 8.581, 3),
    Stadt("Koblenz", 50.356, 7.598, 3),
    Stadt("Bergisch Gladbach", 50.992, 7.136, 3),
    Stadt("Remscheid", 51.179, 7.189, 3),
    Stadt("Trier", 49.75, 6.637, 3),
    Stadt("Recklinghausen", 51.614, 7.198, 3),
    Stadt("Jena", 50.927, 11.589, 3),
    Stadt("Moers", 51.451, 6.627, 3),
    Stadt("Salzgitter", 52.155, 10.332, 3),
    Stadt("Siegen", 50.875, 8.024, 3),
    Stadt("Gütersloh", 51.906, 8.378, 3),
    Stadt("Hildesheim", 52.152, 9.951, 3),
    Stadt("Hanau", 50.133, 8.917, 3),
    Stadt("Kaiserslautern", 49.444, 7.769, 3),
    Stadt("Cottbus", 51.757, 14.329, 3),
    Stadt("Schwerin", 53.629, 11.414, 3),
    Stadt("Flensburg", 54.784, 9.437, 4),
    Stadt("Zwickau", 50.718, 12.496, 4),
    Stadt("Gera", 50.881, 12.083, 4),
    Stadt("Rosenheim", 47.857, 12.118, 4),
    Stadt("Passau", 48.574, 13.461, 4),
    Stadt("Bamberg", 49.891, 10.887, 4),
    Stadt("Bayreuth", 49.945, 11.576, 4),
    Stadt("Kempten (Allgäu)", 47.727, 10.315, 4),
    Stadt("Konstanz", 47.66, 9.175, 4),
    Stadt("Stralsund", 54.309, 13.082, 4),
    Stadt("Greifswald", 54.093, 13.388, 4),
    Stadt("Neubrandenburg", 53.557, 13.261, 4),
    Stadt("Frankfurt (Oder)", 52.347, 14.55, 4),
    Stadt("Görlitz", 51.153, 14.987, 4),
    Stadt("Dessau-Roßlau", 51.834, 12.243, 4),
    Stadt("Wismar", 53.893, 11.465, 4),
    Stadt("Lüneburg", 53.248, 10.408, 4),
    Stadt("Celle", 52.622, 10.08, 4),
    Stadt("Emden", 53.367, 7.207, 4),
    Stadt("Wilhelmshaven", 53.53, 8.106, 4),
    Stadt("Fulda", 50.551, 9.676, 4),
    Stadt("Gießen", 50.584, 8.678, 4),
    Stadt("Marburg", 50.81, 8.771, 4),
    Stadt("Landshut", 48.537, 12.152, 4),
    Stadt("Plauen", 50.496, 12.137, 4),
    Stadt("Stendal", 52.606, 11.858, 4),
    Stadt("Neuruppin", 52.924, 12.803, 4),
    Stadt("Eberswalde", 52.833, 13.82, 4),
    Stadt("Brandenburg an der Havel", 52.412, 12.532, 4),
    Stadt("Hof", 50.313, 11.912, 4),
    Stadt("Aschaffenburg", 49.974, 9.149, 4),
    Stadt("Schweinfurt", 50.049, 10.221, 4),
    Stadt("Coburg", 50.258, 10.965, 4),
    Stadt("Weimar", 50.979, 11.329, 4),
    Stadt("Eisenach", 50.975, 10.32, 4),
    Stadt("Nordhausen", 51.505, 10.791, 4),
    Stadt("Suhl", 50.609, 10.693, 4),
    Stadt("Halberstadt", 51.896, 11.047, 4),
    Stadt("Lutherstadt Wittenberg", 51.866, 12.649, 4),
    Stadt("Salzwedel", 52.851, 11.153, 4),
    Stadt("Bautzen", 51.181, 14.424, 4),
    Stadt("Freiberg", 50.912, 13.342, 4),
    Stadt("Pirna", 50.963, 13.941, 4),
    Stadt("Zittau", 50.896, 14.807, 4),
    Stadt("Offenburg", 48.473, 7.944, 4),
    Stadt("Villingen-Schwenningen", 48.06, 8.458, 4),
    Stadt("Friedrichshafen", 47.654, 9.479, 4),
    Stadt("Ravensburg", 47.782, 9.611, 4),
    Stadt("Aalen", 48.837, 10.093, 4),
    Stadt("Crailsheim", 49.134, 10.071, 4),
    Stadt("Memmingen", 47.984, 10.181, 4),
    Stadt("Garmisch-Partenkirchen", 47.492, 11.095, 4),
    Stadt("Traunstein", 47.869, 12.643, 4),
    Stadt("Straubing", 48.881, 12.573, 4),
    Stadt("Deggendorf", 48.834, 12.964, 4),
    Stadt("Weiden in der Oberpfalz", 49.676, 12.156, 4),
    Stadt("Cham", 49.221, 12.665, 4),
    Stadt("Landau in der Pfalz", 49.199, 8.118, 4),
    Stadt("Pirmasens", 49.201, 7.605, 4),
    Stadt("Idar-Oberstein", 49.713, 7.31, 4),
    Stadt("Bitburg", 49.968, 6.527, 4),
    Stadt("Arnsberg", 51.396, 8.064, 4),
    Stadt("Minden", 52.29, 8.915, 4),
    Stadt("Detmold", 51.938, 8.879, 4),
    Stadt("Lingen (Ems)", 52.522, 7.318, 4),
    Stadt("Meppen", 52.693, 7.291, 4),
    Stadt("Stade", 53.6, 9.476, 4),
    Stadt("Cuxhaven", 53.861, 8.694, 4),
    Stadt("Uelzen", 52.965, 10.559, 4),
    Stadt("Husum", 54.477, 9.051, 4),
    Stadt("Heide", 54.196, 9.093, 4),
    Stadt("Prenzlau", 53.317, 13.863, 4),
    Stadt("Waren (Müritz)", 53.518, 12.68, 4),
    Stadt("Güstrow", 53.794, 12.176, 4),
)

/**
 * Sucht eine Stadt nach ihrem Namen — ohne Rücksicht auf Groß- und
 * Kleinschreibung und auf Umlaute: „Muenchen“ und „munchen“ führen beide nach
 * München. Erst ein genauer Treffer, dann einer, der so anfängt.
 */
fun stadtFinden(eingabe: String): Stadt? {
    val gesucht = vereinfachen(eingabe)
    if (gesucht.length < 2) return null
    return STAEDTE.firstOrNull { vereinfachen(it.name) == gesucht }
        ?: STAEDTE.firstOrNull { vereinfachen(it.name).startsWith(gesucht) }
}

/** Die Vorschläge unter dem Suchfeld — höchstens fünf, größte zuerst. */
fun staedteVorschlagen(eingabe: String): List<Stadt> {
    val gesucht = vereinfachen(eingabe)
    if (gesucht.length < 2) return emptyList()
    return STAEDTE.filter { vereinfachen(it.name).contains(gesucht) }.take(5)
}

private fun vereinfachen(text: String): String {
    val klein = text.trim().lowercase()
        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
    return Normalizer.normalize(klein, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
}
