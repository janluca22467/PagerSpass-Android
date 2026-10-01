package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * Was die Lehrgänge seit 5.0.0.26 mehr können: die **Übung** mit Lernkarten und
 * die **Wissensprüfung** mit Fragen — Gegenstück zu `Lernkarte`, `Theoriefrage`
 * und `Theorieurteil` in `web/src/types.ts`.
 *
 * <b>Ein älterer Server schickt diese Module nicht</b>; dann bleiben `karten` und
 * `fragen` leer und die Seite zeigt, was sie vorher zeigte. Eigene Datei, damit
 * `Werkmodelle.kt` nur die neuen Felder am Modul trägt.
 */

/**
 * Eine Karte einer Übung. `art` ist `Auswahl`, `Reihenfolge` oder `Zuordnung`.
 * Bei der Reihenfolge stehen die Schritte in der richtigen Folge; gemischt wird
 * erst in der Ansicht. Die Lösung kommt mit — eine Übung prüft nichts.
 */
@Serializable
data class Lernkarte(
    val id: String = "",
    val art: String = "Auswahl",
    val frage: String = "",
    val antworten: List<String> = emptyList(),
    val richtig: Int = 0,
    val schritte: List<String> = emptyList(),
    val paare: List<Lernpaar> = emptyList(),
    val erklaerung: String = "",
)

@Serializable
data class Lernpaar(val links: String = "", val rechts: String = "")

/** Eine Frage der Wissensprüfung — ohne Lösung, die kennt nur der Server. */
@Serializable
data class Theoriefrage(
    val id: String = "",
    val frage: String = "",
    val antworten: List<String> = emptyList(),
)

/** Das Urteil einer Wissensprüfung: bestanden oder nicht, und was falsch war. */
@Serializable
data class Theorieurteil(
    val bestanden: Boolean = false,
    val fehler: List<Theoriefehler> = emptyList(),
    val erlaubteFehler: Int = 0,
)

@Serializable
data class Theoriefehler(
    val frageId: String = "",
    val frage: String = "",
    val erklaerung: String = "",
)

/** Der Rumpf der Abgabe: Frage-Id → Index der gewählten Antwort. */
@Serializable
internal data class Theorieabgabe(val antworten: Map<String, Int>)

/**
 * Aus dem Einrichtungsbogen nur, was die Lehrgänge brauchen: welche Rolle man am
 * liebsten spielt (`Leitstelle`, `Fahrzeug`, `Beides`) — sie entscheidet, welcher
 * Lehrgang „Dein Einstieg" heißt.
 */
@Serializable
data class Einrichtungsauszug(val spielrolle: String? = null)

/**
 * Die Wege der neuen Module — als Erweiterung an `Netz`, damit `Werkwege` nicht
 * wächst. Pfade wie `theorieAbgeben` und `einrichtungLaden` in `api/rest.ts`.
 */
internal suspend fun Netz.theorieAbgeben(
    lehrgangId: String,
    modulId: String,
    kennung: String,
    antworten: Map<String, Int>,
): Theorieurteil = hole(
    "/api/lehrgaenge/${teil(lehrgangId)}/module/${teil(modulId)}/antworten?kennung=${teil(kennung)}",
    "POST",
    Json.encodeToString(Theorieabgabe.serializer(), Theorieabgabe(antworten)),
)

internal suspend fun Netz.einrichtungsauszug(kennung: String): Einrichtungsauszug =
    hole("/api/konto/${teil(kennung)}/einrichtung")

private fun teil(wert: String): String = URLEncoder.encode(wert, "UTF-8")
