package de.pagerspass.pagerspass.ansichten

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import de.pagerspass.pagerspass.mobil.Geraeteeinstellungen
import de.pagerspass.pagerspass.mobil.eingabewegState
import de.pagerspass.pagerspass.mobil.kennungsformState
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Karte
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.zeichen.Zeichen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ------------------------------------------------------------ Deine Bedienung

/**
 * Die Geräteeinstellungen aus „Spiel & Bedienung“ in `KontoView.vue` — Eingabeweg, Fahrzeugkennung, Fahrzeugkatalog.
 *
 * Der Eingabeweg stand im Web einmal unten in der Dienstbuch-Übersicht, wo ihn
 * niemand als Einstellung suchte; hier steht er bei den übrigen. Beide gelten
 * nur für dieses Gerät (siehe [Geraeteeinstellungen]).
 */
@Composable
fun Bedienungskarte() {
    val zusammenhang = LocalContext.current
    val weg by eingabewegState()
    val form by kennungsformState()
    val staat by de.pagerspass.pagerspass.mobil.katalogstaatState()

    // Drei Gerätekarten wie in „Spiel & Bedienung" im Web (v6) — je eine Frage,
    // je eine Wahl. Vorher stand alles in einer Karte „Deine Bedienung".
    Karte(titel = "Notrufabfrage", zeichen = Zeichen.Notruf, text = "Wie du Rückfragen am Telefon stellst.") {
        Etikett("Eingabeweg")
        Pillenreihe {
            listOf("fragen" to "Fragen anklicken", "tippen" to "Selbst tippen", "sprechen" to "Sprechen").forEach { (id, wort) ->
                Pille(wort, weg == id, { Geraeteeinstellungen.eingabewegSetzen(zusammenhang, id) })
            }
        }
        SehrLeise(WEG_ERKLAERUNG[weg].orEmpty())
    }

    Karte(titel = "Fahrzeugkennung", zeichen = Zeichen.Fahrzeug, text = "Wie Fahrzeuge in deinen Listen heißen.") {
        Etikett("Darstellung")
        Pillenreihe {
            listOf(
                "kennzahl" to "Kennzahl — 1/44/1",
                "typ" to "Fahrzeugtyp — 1/HLF 20-1",
                "orga" to "Träger und Kreis — RK-Celle-3/91/1",
            ).forEach { (id, wort) ->
                Pille(wort, form == id, { Geraeteeinstellungen.kennungsformSetzen(zusammenhang, id) })
            }
        }
        SehrLeise(KENNUNG_ERKLAERUNG[form].orEmpty())
    }

    Karte(
        titel = "Fahrzeugkatalog",
        zeichen = Zeichen.Karte,
        text = "Aus welchem Land Autohaus und Garage ihre Fahrzeuge zeigen. Stellt sich von selbst auf " +
            "das Land der Runde, in der du spielst.",
    ) {
        de.pagerspass.pagerspass.ui.bausteine.Segment(
            seiten = de.pagerspass.pagerspass.netz.Staaten.ALLE,
            gewaehlt = staat,
            beiWahl = { Geraeteeinstellungen.katalogstaatSetzen(zusammenhang, it) },
            aufschrift = { de.pagerspass.pagerspass.netz.Staaten.kurz(it) },
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        )
        SehrLeise(
            "Jeder Fahrzeuggutschein gilt in jedem der drei Länder einmal: Bekommst du einen, darfst du " +
                "dir ein deutsches, ein österreichisches und ein schweizerisches Fahrzeug aussuchen — aber " +
                "nicht drei aus demselben Land.",
        )
    }
}

private val WEG_ERKLAERUNG = mapOf(
    "fragen" to "Sechs Knöpfe nach dem klassischen Abfrageschema. Schnell und ohne Tastatur.",
    "tippen" to "Frage frei eintippen. Gedeutet wird sie auf dem Server.",
    "sprechen" to "Frage ins Mikrofon sprechen, solange die Taste gedrückt ist. Verstanden wird sie auf dem Server.",
)

private val KENNUNG_ERKLAERUNG = mapOf(
    "kennzahl" to "Die Kurzform des Funkrufnamens, wie sie am Mikrofon gesagt wird: Wache, Kennzahl, laufende Nummer.",
    "typ" to "An der Stelle der Kennzahl steht der Fahrzeugtyp — Wache und laufende Nummer bleiben, du siehst " +
        "also weiter, welches Fahrzeug welcher Wache gemeint ist. Gerufen wird im Funk trotzdem der volle Funkrufname.",
    "orga" to "Vor der Kurzform stehen die Organisation und der Landkreis: „RD-Celle-1/83/1“ für den " +
        "Rettungsdienst. Nützlich, wo mehrere nebeneinander fahren. In einer Runde ohne echten Landkreis fällt " +
        "der Kreisteil weg.",
)

// ------------------------------------------------------------- Dieses Gerät

/**
 * „Dieses Gerät“ in der Privatsphäre — `PrivatsphaereView.vue`: die
 * Karten-Einwilligung und das Löschen dessen, was die App hier aufbewahrt.
 *
 * <b>Die Kartenfreigabe ist eine Einwilligung, kein Serverwert</b> (Art. 49
 * DSGVO, Datenschutzerklärung Ziffer 11). Sie gilt nur für dieses Gerät und
 * steht deshalb hier und nicht bei den Schaltern darüber.
 *
 * <b>Löschen mit Rückfrage.</b> Es nimmt keine Einwilligung zurück, sondern
 * meldet ab und vergisst jede Einstellung dieses Geräts — ein Tipp aus Versehen
 * kostet mehr als einer zu viel. Abgemeldet wird zuerst, solange das Merkmal
 * noch da ist: So endet auch die Sitzung am Server.
 *
 * @param beiAbmelden meldet am Server ab und kehrt erst danach zurück.
 */
@Composable
fun ColumnScope.DiesesGeraet(beiAbmelden: suspend () -> Unit) {
    val zusammenhang = LocalContext.current
    val ablage = remember { Ablage(zusammenhang) }
    val bereich = rememberCoroutineScope()
    var freigabe by remember { mutableStateOf<String?>("laedt") }
    var frage by remember { mutableStateOf(false) }
    var laeuft by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { freigabe = ablage.karteFreigabe() }

    Abschnitt("Dieses Gerät") {
        Schalterzeile(
            titel = "Kartenhintergrund laden",
            unterzeile = "Die Kacheln der Lagekarte, der Navigation und der Weltkarte kommen von unserem " +
                "eigenen Kartenserver, das Luftbild der Satelliten- und Hybridansicht von Esri in den USA; " +
                "dabei erhält Esri deine IP-Adresse. Aus heißt: Sie werden nicht mehr geladen, Marker und " +
                "Routen bleiben. Anders als die Schalter darüber gilt das nur für dieses Gerät — es ist eine " +
                "Einwilligung, kein Serverwert.",
            an = freigabe == "ja",
            aktiv = freigabe != "laedt",
            beiWechsel = { an ->
                val wert = if (an) "ja" else "nein"
                freigabe = wert
                bereich.launch { ablage.karteFreigabeSetzen(wert) }
            },
        )

        Text("Gespeicherte Daten dieses Geräts", style = Schrift.Normal, color = Farben.Text)
        SehrLeise(
            "Deine Anmeldung, der Server, die Karten- und Melderwahl, eigene Klänge, die Kartenebenen der " +
                "Welt und deine Bedienung. Sie gehen nicht an uns, aber auf einem geteilten Gerät liest sie " +
                "der Nächste. Löschen meldet dich hier ab und setzt dieses Gerät zurück — dein Konto bleibt.",
        )
        if (!frage) {
            Row { Knopf("Löschen", { frage = true }, art = Knopfart.Leise, kompakt = true) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                Knopf(
                    if (laeuft) "Wird gelöscht …" else "Ja, löschen",
                    {
                        laeuft = true
                        bereich.launch {
                            // Auch ohne Server soll das Gerät leer werden — darum geht es hier.
                            runCatching { beiAbmelden() }
                            lokaleDatenLoeschen(zusammenhang)
                            neuStarten(zusammenhang)
                        }
                    },
                    art = Knopfart.Gefahr,
                    aktiv = !laeuft,
                    kompakt = true,
                )
                Knopf("Abbrechen", { frage = false }, art = Knopfart.Leise, aktiv = !laeuft, kompakt = true)
            }
        }
    }
}

/**
 * Leert, was die App auf diesem Gerät aufbewahrt: die Ablage (Merkmal, Server,
 * Kartenwahl …), jede Gerätedatei (Melder, Welt, Bedienung), eigene Klänge und
 * den Zwischenspeicher.
 */
private suspend fun lokaleDatenLoeschen(zusammenhang: Context) = withContext(Dispatchers.IO) {
    runCatching { Ablage(zusammenhang).allesVergessen() }
    val anwendung = zusammenhang.applicationContext
    runCatching {
        File(anwendung.applicationInfo.dataDir, "shared_prefs").listFiles()?.forEach { datei ->
            anwendung.deleteSharedPreferences(datei.name.removeSuffix(".xml"))
        }
    }
    // Die Ablage selbst (`datastore/`) ist oben schon geleert; ihre Datei unter
    // der laufenden Instanz wegzuziehen, hieße einen Schreibfehler zu riskieren.
    runCatching { anwendung.filesDir.listFiles()?.filter { it.name != "datastore" }?.forEach { it.deleteRecursively() } }
    runCatching { anwendung.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
    Geraeteeinstellungen.vergessen()
}

/**
 * Neu starten statt weiterzumachen — `location.assign('/')` im Web. Was noch im
 * Speicher des Prozesses steht (Melderwahl, Klänge, Kacheln), soll mit weg.
 */
private fun neuStarten(zusammenhang: Context) {
    val start = zusammenhang.packageManager.getLaunchIntentForPackage(zusammenhang.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ?: return
    zusammenhang.startActivity(start)
    Runtime.getRuntime().exit(0)
}

// ------------------------------------------------------- Datenverarbeitung

/**
 * Die Datenverarbeitung in einem Fenster — `DatenverarbeitungDialog.vue`.
 *
 * <b>Der Text gehört zur App</b>, wie im Web zum Bau (`recht/datenverarbeitung.md`
 * wird mit `?raw` hineingenommen): Er ist die Fassung, der jemand zustimmt, und
 * darf sich nicht hinter dem Rücken der Anwendung ändern. Deshalb auch kein Weg
 * in den Browser — die Frage wird hier gestellt, also steht die Antwort hier.
 */
@Composable
fun Datenverarbeitungsblende(beiSchliessen: () -> Unit) {
    Blende(
        titel = "Datenverarbeitung",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Breit,
        kopfknoepfe = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
    ) {
        Etikett("Datenschutz")
        DATENVERARBEITUNG.forEach { block ->
            when (block) {
                is Textblock.Ueberschrift -> Etikett(block.text)
                is Textblock.Absatz -> Text(fett(block.text), style = Schrift.Normal, color = Farben.Text)
                is Textblock.Liste -> block.punkte.forEach { punkt ->
                    Text(fett("·  $punkt"), style = Schrift.Normal, color = Farben.Text)
                }
            }
        }
    }
}

private sealed interface Textblock {
    data class Ueberschrift(val text: String) : Textblock
    data class Absatz(val text: String) : Textblock
    data class Liste(val punkte: List<String>) : Textblock
}

/** `**fett**` wie in `recht/markdown.ts` — das einzige Auszeichnungszeichen im Text. */
private fun fett(text: String) = buildAnnotatedString {
    text.split("**").forEachIndexed { i, stueck ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stueck) } else append(stueck)
    }
}

/** Zeichengleich `web/src/recht/datenverarbeitung.md`. */
private val DATENVERARBEITUNG: List<Textblock> = listOf(
    Textblock.Ueberschrift("Worum es geht"),
    Textblock.Absatz(
        "Der Funkverkehr mit den Bots und die Notrufgespräche mit der Leitstelle sind im Spiel keine echte " +
            "Sprache, sondern eine Sammlung von Regeln und Textbausteinen. Sie werden besser, wenn wir sehen, " +
            "was Spielerinnen und Spieler tatsächlich sagen — und woran ein Bot vorbeiredet.",
    ),
    Textblock.Absatz(
        "Genau dafür bitten wir um die Erlaubnis, von diesen Gesprächen eine gesonderte Kopie zur Auswertung " +
            "anzulegen. Sie ist freiwillig, und ohne sie funktioniert das Spiel vollständig weiter.",
    ),
    Textblock.Ueberschrift("Was diese Frage betrifft — und was nicht"),
    Textblock.Absatz(
        "Das Funkprotokoll und das Anrufjournal einer Runde entstehen unabhängig von deiner Antwort; ohne sie " +
            "bliebe dein Dienstbuch leer. Sie hängen an der Runde und werden mit ihr nach 30 Tagen gelöscht.",
    ),
    Textblock.Absatz(
        "Hier geht es allein um die **zusätzliche Kopie**, die von der Runde losgelöst ist, deiner Kontokennung " +
            "zugeordnet wird und nicht mit der Runde verschwindet. Sagst du nein, bleibt es beim Protokoll der Runde.",
    ),
    Textblock.Ueberschrift("Was in die Kopie wandert"),
    Textblock.Liste(
        listOf(
            "Deine Funksprüche und die Antworten der Bots",
            "Deine Lagemeldungen und Nachforderungen",
            "Deine Fragen im Notrufgespräch und die Antworten des Anrufers",
            "Dazu der Rahmen, ohne den eine Zeile nichts erklärt: die Kennung, unter der gefunkt wurde (zum " +
                "Beispiel „Florian Heidefeld 1/44/1\"), die Rolle (Leitstelle oder Fahrzeugbesatzung), der " +
                "Fahrzeugtyp, ob die Gegenseite ein Bot war, der Zeitpunkt und der Vorgang, zu dem die Zeile " +
                "gehört — der Einsatz beziehungsweise das Gespräch",
        ),
    ),
    Textblock.Absatz(
        "Statt des Raumcodes steht dort ein Rundenschlüssel, der für jedes Konto anders ausfällt: deine eigenen " +
            "Zeilen bleiben gruppierbar, die Dateien mehrerer Konten lassen sich aber nicht zu einer gemeinsamen " +
            "Runde zusammensetzen.",
    ),
    Textblock.Ueberschrift("Was nicht hineinkommt"),
    Textblock.Liste(
        listOf(
            "Kein Ton. Was du in die Sprechtaste sagst, wird auf deinem Gerät in Text umgewandelt; die Aufnahme " +
                "selbst verlässt es nicht und wird nirgends gespeichert.",
            "Kein Anzeigename, kein Benutzername, keine IP-Adresse, keine Gerätekennung",
            "Nichts aus dem Chat mit anderen Menschen, aus Freundeslisten oder Gemeinschaften",
            "Nichts von Mitspielern: Funk zwischen zwei Menschen kommt in die Kopie nicht hinein, nur Gespräche " +
                "mit einem Bot oder einem Anrufer",
        ),
    ),
    Textblock.Ueberschrift("Wie gespeichert wird"),
    Textblock.Absatz(
        "Die Zeilen liegen als Textdateien auf unserem Server, eine je Konto und Tag, zugeordnet über deine " +
            "Kontokennung. Sie sind ausschließlich über die passwortgeschützte Verwaltung erreichbar und werden " +
            "an niemanden weitergegeben, verkauft oder für Werbung genutzt.",
    ),
    Textblock.Absatz("Verwendet werden sie allein dazu, den Bot-Funk und die Leitstellenanrufe zu verbessern."),
    Textblock.Ueberschrift("Widerruf"),
    Textblock.Absatz(
        "Du kannst deine Entscheidung jederzeit ändern: Konto → Datenschutzeinstellungen → „Daten für " +
            "Verbesserung der Produkte verwenden\".",
    ),
    Textblock.Absatz(
        "Wenn du dort auf „Aus\" stellst, entsteht keine weitere Kopie — und alles, was bis dahin auf diesem Weg " +
            "von dir gespeichert wurde, wird gelöscht. Der Widerruf wirkt also nicht nur nach vorn. Das " +
            "Protokoll laufender und vergangener Runden bleibt davon unberührt; es verschwindet nach 30 Tagen " +
            "von selbst.",
    ),
)
