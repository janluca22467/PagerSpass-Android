package de.pagerspass.pagerspass.ansichten

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Einrichtungsdienst
import de.pagerspass.pagerspass.mobil.Einrichtungsstand
import de.pagerspass.pagerspass.mobil.Geraeteeinstellungen
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

// ================================================================ Altersfrage

/**
 * Die Altersfrage — `AltersfreigabeDialog.vue` (v6).
 *
 * <b>Warum es sie gibt.</b> Einen Nutzungsvertrag schließt ein Minderjähriger nur
 * mit Zustimmung seiner Erziehungsberechtigten wirksam (§§ 107, 108 BGB). Gefragt
 * werden die Eltern über einen Bogen auf forms.pagerspass.de, der das Konto
 * freigibt, sobald er unterschrieben ist.
 *
 * <b>Zwei Zustände.</b> `Offen`: die Frage mit zwei gleich breiten Knöpfen.
 * `WartetAufEltern`: der Link zum Bogen — öffnen, kopieren, teilen — und ein Knopf,
 * der nachsieht. Nachgesehen wird auch von selbst, sobald die App wieder in den
 * Vordergrund kommt: Wer vom Bogen zurückkommt, soll nicht erst etwas drücken müssen.
 *
 * <b>Er sperrt aus wie die E-Mail-Pflicht.</b> Der einzige Weg daran vorbei ist
 * „Abmelden". Die Antwort gilt einmal; ein Versehen korrigiert der Support.
 */
@Composable
fun Altersfreigabeblende(
    wartet: Boolean,
    stand: Einrichtungsstand,
    dienst: Einrichtungsdienst,
    beiAbmelden: () -> Unit,
) {
    val browser = LocalUriHandler.current
    val ablage = LocalClipboardManager.current
    val zusammenhang = LocalContext.current
    var hinweis by remember { mutableStateOf<String?>(null) }
    val link = stand.elternlink

    // Den Link holen, sobald gewartet wird — und nach jeder Rückkehr nachsehen.
    LaunchedEffect(wartet) { if (wartet) dienst.elternlinkLaden() }
    BeiRueckkehr { if (wartet) dienst.nachsehen() }

    Blende(
        titel = if (wartet) "Deine Eltern müssen zustimmen" else "Wie alt bist du?",
        augenbraue = if (wartet) "Fast geschafft" else "Eine Frage",
        beiSchliessen = {},
        schliessenMoeglich = false,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Abmelden", beiAbmelden, art = Knopfart.Leise, aktiv = !stand.laeuft)
            if (wartet) {
                Knopf(
                    "Schon unterschrieben?",
                    {
                        dienst.nachsehen {
                            hinweis = "Noch nicht — der Bogen ist noch nicht unterschrieben."
                        }
                    },
                    aktiv = !stand.laeuft,
                )
            }
        },
    ) {
        if (!wartet) {
            Text(
                "Bevor es weitergeht, brauchen wir eine Antwort: Bist du 18 oder älter? Wer " +
                    "jünger ist, braucht für PagerSpass das Einverständnis seiner Eltern — dafür " +
                    "gibt es einen kurzen Bogen, den sie ausfüllen.",
                style = Schrift.Normal,
                color = Farben.Text,
            )
            SehrLeise(
                "Wir fragen nicht nach deinem Geburtstag, nur nach dieser einen Antwort. Du " +
                    "kannst sie nur einmal geben.",
            )
            // Untereinander und volle Breite — gleich gewichtet bis auf die Farbe, damit
            // keine von beiden wie „die richtige" aussieht, die man eben durchklickt.
            Knopf(
                "Ich bin 18 oder älter",
                { dienst.alterAngeben(true) },
                art = Knopfart.Haupt,
                breit = true,
                aktiv = !stand.laeuft,
            )
            Knopf(
                "Ich bin unter 18",
                { dienst.alterAngeben(false) { l -> l?.let { runCatching { browser.openUri(it) } } } },
                breit = true,
                aktiv = !stand.laeuft,
            )
        } else {
            Text(
                "Dein Konto ist freigegeben, sobald ein Elternteil den Bogen ausgefüllt und " +
                    "unterschrieben hat. Öffne ihn gemeinsam mit deinen Eltern, oder schick ihnen " +
                    "den Link.",
                style = Schrift.Normal,
                color = Farben.Text,
            )
            if (link != null) {
                Knopf("Bogen öffnen", { runCatching { browser.openUri(link) } }, art = Knopfart.Haupt, breit = true)
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        "Link kopieren",
                        {
                            ablage.setText(AnnotatedString(link))
                            hinweis = "Link kopiert. Schick ihn deinen Eltern."
                        },
                        modifier = Modifier.weight(1f),
                        aktiv = !stand.laeuft,
                    )
                    Knopf(
                        "Teilen",
                        {
                            val senden = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "PagerSpass — Einverständnis")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Kannst du mir bitte PagerSpass freigeben? Dafür brauche ich dein " +
                                        "Einverständnis: $link",
                                )
                            }
                            runCatching {
                                zusammenhang.startActivity(
                                    Intent.createChooser(senden, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        aktiv = !stand.laeuft,
                    )
                }
                SehrLeise(link, mono = true)
            } else {
                Text(
                    "Der Bogen für deine Eltern ist gerade nicht erreichbar. Versuch es später noch " +
                        "einmal oder schreib an support@pagerspass.de.",
                    style = Schrift.Klein,
                    color = Farben.SignalHell,
                )
            }
            hinweis?.let { Text(it, style = Schrift.Klein, color = Farben.GruenHell) }
        }
        stand.fehler?.let { Text(it, style = Schrift.MonoKlein, color = Farben.SignalHell) }
    }
}

// ========================================================== Einrichtungsbogen

/**
 * Was der Bogen am Ende weiß — `Antworten` in `fragebogen.ts`. Leer, solange die
 * Frage nicht dran war.
 */
data class Bogenantworten(
    val spielrolle: String? = null,
    val anrufeAnnehmen: Boolean? = null,
    val eingabeweg: String? = null,
    val kennungsform: String? = null,
    val massnahmenAlle: Boolean? = null,
    val funkVorlesen: Boolean? = null,
    val vorkenntnisse: String? = null,
)

/** Eine Antwort: Aufschrift, ein Satz darunter, was sie setzt und wohin es danach geht. */
class Bogenantwort(
    val text: String,
    val erklaerung: String,
    val setzt: (Bogenantworten) -> Bogenantworten,
    val weiter: (Bogenantworten) -> String,
)

class Bogenfrage(val id: String, val frage: String, val hinweis: String, val antworten: List<Bogenantwort>)

/**
 * Der Einrichtungsbogen — `fragebogen.ts` (v6), als Daten und nicht als Vorlage.
 *
 * Der Bogen verzweigt: Wer keine Anrufe annehmen will, wird nicht gefragt, wie er
 * sie beantwortet; wer nur Leitstelle spielt, nicht nach den Maßnahmen am
 * Patienten. Hier steht an jeder Antwort, wohin sie führt. Jede Frage setzt etwas,
 * das es schon gibt — der Bogen ist nur der Weg, alles einmal zu sehen.
 */
object Fragebogen {
    const val ENDE = "ende"
    const val ERSTE = "rolle"

    /** „Frage 2 von höchstens 7" — gezählt wird der längste Weg. */
    const val HOECHSTENS = 7

    /** Nach der Kennung: Wer auch Fahrzeug fährt, wird nach den Maßnahmen gefragt. */
    private val nachKennung: (Bogenantworten) -> String =
        { a -> if (a.spielrolle == "Beides") "katalog" else "vorlesen" }

    val FRAGEN: Map<String, Bogenfrage> = listOf(
        Bogenfrage(
            "rolle",
            "Was möchtest du am liebsten spielen?",
            "Danach richtet sich, welche Fragen noch kommen und welcher Lehrgang dein Einstieg ist. " +
                "Spielen kannst du trotzdem alles.",
            listOf(
                Bogenantwort("Leitstelle", "Notrufe annehmen, Fahrzeuge alarmieren, den Funk führen.", { it.copy(spielrolle = "Leitstelle") }, { "anrufe" }),
                Bogenantwort("Fahrzeug", "Melder quittieren, ausrücken, Status drücken, Lage melden.", { it.copy(spielrolle = "Fahrzeug") }, { "katalog" }),
                Bogenantwort("Beides", "Mal am Tableau, mal auf dem Fahrzeug — je nach Runde.", { it.copy(spielrolle = "Beides") }, { "anrufe" }),
            ),
        ),
        Bogenfrage(
            "anrufe",
            "Möchtest du Anrufe annehmen?",
            "Ja heißt: In deinen Runden kommen Notrufe als Anruf herein — es klingelt, jemand meldet " +
                "sich, und was du erfährst, hängt davon ab, was du fragst. Nein heißt: Einsätze kommen " +
                "fertig beschrieben. Umlegen kannst du es in jeder Lobby.",
            listOf(
                Bogenantwort("Ja", "Das Telefon klingelt, du fragst ab.", { it.copy(anrufeAnnehmen = true) }, { "eingabeweg" }),
                Bogenantwort("Nein", "Einsätze stehen gleich mit Stichwort und Adresse da.", { it.copy(anrufeAnnehmen = false) }, { "kennung" }),
            ),
        ),
        Bogenfrage(
            "eingabeweg",
            "Wie möchtest du Anrufe beantworten?",
            "Wie du dem Anrufer deine Rückfragen stellst. Gedeutet wird auf allen drei Wegen gleich.",
            listOf(
                Bogenantwort("Klicken", "Sechs Knöpfe nach dem Abfrageschema. Schnell und ohne Tastatur.", { it.copy(eingabeweg = "fragen") }, { "kennung" }),
                Bogenantwort("Sprechen", "Frage ins Mikrofon sprechen, solange die Taste gedrückt ist.", { it.copy(eingabeweg = "sprechen") }, { "kennung" }),
                Bogenantwort("Tippen", "Frage frei eintippen, so wie du sie stellen würdest.", { it.copy(eingabeweg = "tippen") }, { "kennung" }),
            ),
        ),
        Bogenfrage(
            "kennung",
            "Wie sollen Fahrzeuge in deinen Listen heißen?",
            "Nur die Anzeige am Tableau und in den Listen. Gerufen wird im Funk immer der volle Funkrufname.",
            listOf(
                Bogenantwort("Kennzahl — 1/44/1", "Wie am Mikrofon: Wache, Kennzahl, laufende Nummer.", { it.copy(kennungsform = "kennzahl") }, nachKennung),
                Bogenantwort("Fahrzeugtyp — 1/HLF 20-1", "Für alle, die die Kennzahlen noch nicht auswendig können.", { it.copy(kennungsform = "typ") }, nachKennung),
                Bogenantwort("Träger und Kreis — RK-Celle-3/91/1", "Nützlich, wo mehrere Träger nebeneinander fahren.", { it.copy(kennungsform = "orga") }, nachKennung),
            ),
        ),
        Bogenfrage(
            "katalog",
            "Wie viele Maßnahmen willst du am Patienten sehen?",
            "PatSim ist immer an. Du kannst die Übersicht jederzeit unter Konto → Spiel & Bedienung umstellen.",
            listOf(
                Bogenantwort("Einfach", "Die Maßnahmen, die zum Patienten vorgeschlagen werden — übersichtlich für den Anfang.", { it.copy(massnahmenAlle = false) }, { "vorlesen" }),
                Bogenantwort("Erweitert", "Alle rund 90 Maßnahmen, mit Suche und Gruppen — wie im Dienst.", { it.copy(massnahmenAlle = true) }, { "vorlesen" }),
            ),
        ),
        Bogenfrage(
            "vorlesen",
            "Sollen deine getippten Funksprüche vorgelesen werden?",
            "Für alle, die nicht sprechen können oder wollen: Dein getippter Spruch wird auf dem Kanal " +
                "mit einer Stimme vorgesprochen. Der Schalter 🗣 an der Funkeingabe legt es jederzeit wieder um.",
            listOf(
                Bogenantwort("Ja, vorlesen", "Wer aufs Funkgerät hört, bekommt deinen Spruch mit.", { it.copy(funkVorlesen = true) }, { "vorkenntnisse" }),
                Bogenantwort("Nein, nur ins Protokoll", "Getippter Funk steht wie bisher im Funkprotokoll.", { it.copy(funkVorlesen = false) }, { "vorkenntnisse" }),
            ),
        ),
        Bogenfrage(
            "vorkenntnisse",
            "Kennst du dich mit Leitstelle und BOS-Funk schon aus?",
            "Es ändert keine Regel — nur den Rat, womit du anfängst.",
            listOf(
                Bogenantwort("Neu hier", "Status, Rufnamen, Stichworte — alles noch fremd.", { it.copy(vorkenntnisse = "Neu") }, { ENDE }),
                Bogenantwort("Ein bisschen", "Schon mal gespielt oder reingelesen.", { it.copy(vorkenntnisse = "Etwas") }, { ENDE }),
                Bogenantwort("Ja, gut", "Aus dem Ehrenamt, dem Beruf oder vielen Schichten hier.", { it.copy(vorkenntnisse = "Erfahren") }, { ENDE }),
            ),
        ),
    ).associateBy { it.id }

    /** Der Rat am Ende: welcher Lehrgang der Einstieg ist, und ein Satz dazu. */
    fun empfehlung(a: Bogenantworten): String {
        val (titel, was) = if (a.spielrolle == "Fahrzeug") {
            "Fahrzeug — Grundlagen" to "Melder, Status, Anfahrt und Lagemeldung"
        } else {
            "Leitstelle — Grundlagen" to "Notruf, Alarmierung und Funk vom Tableau aus"
        }
        val tempo = when (a.vorkenntnisse) {
            "Erfahren" -> "Du kennst dich aus — lies quer und geh gleich an die Prüfung."
            "Neu" -> "Nimm dir die Anleitung in Ruhe vor; wiederholen darfst du jede Prüfung, so oft du willst."
            else -> "Die Anleitung ist kurz, die Prüfung auch."
        }
        return "Dein Einstieg ist „$titel“: $was. $tempo"
    }

    /** Die Zusammenfassung — nur, was gefragt wurde. */
    fun zusammenfassung(a: Bogenantworten): List<Pair<String, String>> = buildList {
        a.spielrolle?.let { add("Am liebsten" to it) }
        a.anrufeAnnehmen?.let { add("Anrufe annehmen" to if (it) "Ja" else "Nein") }
        a.eingabeweg?.let {
            add("Anrufe beantworten" to (mapOf("fragen" to "Klicken", "sprechen" to "Sprechen", "tippen" to "Tippen")[it] ?: it))
        }
        a.kennungsform?.let {
            add("Fahrzeuge heißen" to (mapOf("kennzahl" to "Kennzahl", "typ" to "Fahrzeugtyp", "orga" to "Träger und Kreis")[it] ?: it))
        }
        a.massnahmenAlle?.let { add("Maßnahmen in PatSim" to if (it) "Erweitert" else "Einfach") }
        a.funkVorlesen?.let { add("Getippter Funk" to if (it) "Vorlesen" else "Nur Protokoll") }
        a.vorkenntnisse?.let {
            add("Vorwissen" to (mapOf("Neu" to "Neu hier", "Etwas" to "Ein bisschen", "Erfahren" to "Gut")[it] ?: it))
        }
    }
}

/**
 * Der Einrichtungsbogen — `EinrichtungsDialog.vue` (v6): eine Frage nach der
 * anderen, am Ende eine Zusammenfassung mit dem Rat, welcher Lehrgang der
 * Einstieg ist.
 *
 * <b>Ohne Zurück-Taste und ohne Tipp daneben</b>, wie die Analysefrage:
 * „weggeklickt" ist keine Antwort, und der Bogen käme beim nächsten Start wieder.
 * Wer nicht antworten will, hat „Mit den Vorgaben weiter" — das speichert genau,
 * was ohne Bogen gälte.
 *
 * <b>Was wohin geschrieben wird.</b> Anrufe, Rolle und Vorkenntnisse gehen ans
 * Konto, die Maßnahmenübersicht über ihren eigenen Weg ebenfalls; Eingabeweg,
 * Fahrzeugkennung und Vorlesen sind Einstellungen dieses Geräts. Erst das Gerät,
 * dann das Konto: Geht der Aufruf schief, steht wenigstens die Bedienung schon.
 */
@Composable
fun Einrichtungsblende(
    stand: Einrichtungsstand,
    dienst: Einrichtungsdienst,
    beiLehrgang: () -> Unit,
) {
    val zusammenhang = LocalContext.current

    // Der Weg als Stapel: „Zurück" nimmt die letzte Frage herunter und mit ihr, was
    // sie gesetzt hatte.
    val verlauf = remember { mutableStateListOf<Pair<String, Bogenantworten>>() }
    var aktuell by remember { mutableStateOf(Fragebogen.ERSTE) }
    var antworten by remember { mutableStateOf(Bogenantworten()) }
    val letzteWahl = remember { mutableStateOf(mapOf<String, String>()) }
    val frage = Fragebogen.FRAGEN[aktuell]

    fun speichern(a: Bogenantworten, danach: () -> Unit) {
        a.eingabeweg?.let { Geraeteeinstellungen.eingabewegSetzen(zusammenhang, it) }
        a.kennungsform?.let { Geraeteeinstellungen.kennungsformSetzen(zusammenhang, it) }
        a.funkVorlesen?.let { Geraeteeinstellungen.funkVorlesenSetzen(zusammenhang, it) }
        dienst.abschliessen(
            // Ohne Antwort gilt, was ohne Bogen galt: Notrufe kommen fertig beschrieben.
            anrufeAnnehmen = a.anrufeAnnehmen ?: false,
            spielrolle = a.spielrolle,
            vorkenntnisse = a.vorkenntnisse,
            massnahmenAlle = a.massnahmenAlle,
            danach = danach,
        )
    }

    Blende(
        titel = frage?.frage ?: "So richten wir es ein",
        augenbraue = if (frage != null) {
            "Einrichtung · Frage ${verlauf.size + 1} von höchstens ${Fragebogen.HOECHSTENS}"
        } else {
            "Einrichtung · Fertig"
        },
        beiSchliessen = {},
        schliessenMoeglich = false,
        fussAlsSpalte = frage == null,
        fuss = {
            if (frage == null) {
                Knopf("Speichern und zum Lehrgang", { speichern(antworten, beiLehrgang) }, art = Knopfart.Haupt, breit = true, aktiv = !stand.laeuft)
                Knopf("Speichern", { speichern(antworten) {} }, breit = true, aktiv = !stand.laeuft)
            }
            if (verlauf.isNotEmpty()) {
                Knopf(
                    "Zurück",
                    {
                        val (vorher, alt) = verlauf.removeAt(verlauf.lastIndex)
                        aktuell = vorher
                        antworten = alt
                    },
                    art = Knopfart.Leise,
                    breit = frage == null,
                    aktiv = !stand.laeuft,
                )
            } else {
                Knopf(
                    "Mit den Vorgaben weiter",
                    {
                        antworten = Bogenantworten()
                        speichern(Bogenantworten()) {}
                    },
                    art = Knopfart.Leise,
                    aktiv = !stand.laeuft,
                )
            }
        },
    ) {
        if (frage != null) {
            Leise(frage.hinweis)
            frage.antworten.forEachIndexed { nr, antwort ->
                Antwortzeile(
                    nummer = nr + 1,
                    antwort = antwort,
                    gewaehlt = letzteWahl.value[frage.id] == antwort.text,
                    beiDruck = {
                        letzteWahl.value = letzteWahl.value + (frage.id to antwort.text)
                        verlauf.add(frage.id to antworten)
                        antworten = antwort.setzt(antworten)
                        aktuell = antwort.weiter(antworten)
                    },
                )
            }
        } else {
            Fragebogen.zusammenfassung(antworten).forEach { (was, wert) ->
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                    Text(was, style = Schrift.Klein, color = Farben.TextLeise, modifier = Modifier.weight(1f))
                    Text(wert, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Farben.Gruen, Rundung.Klein)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Etikett("Dein erster Lehrgang")
                Text(Fragebogen.empfehlung(antworten), style = Schrift.Normal, color = Farben.Text)
                Leise(
                    "Mit anderen Spielern funkst du nach dem Lehrgang „Sprechfunk“. Er setzt " +
                        "„Leitstelle — Grundlagen“ oder „Fahrzeug — Grundlagen“ voraus. In Runden nur " +
                        "mit Bots darfst du jederzeit funken und üben.",
                )
            }
            SehrLeise(
                "Ändern kannst du alles später unter Konto — dort lässt sich der Bogen auch noch " +
                    "einmal durchgehen.",
            )
        }
        stand.fehler?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
    }
}

/**
 * Eine Antwort ist eine ganze Zeile zum Antippen, mit Nummer davor — eine Zeile
 * statt Pillen, weil jede Antwort einen Satz Erklärung mitbringt.
 */
@Composable
private fun Antwortzeile(nummer: Int, antwort: Bogenantwort, gewaehlt: Boolean, beiDruck: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Ziel.Normal)
            .border(1.dp, if (gewaehlt) Farben.Amber else Farben.Rand, Rundung.Klein)
            .clickable(onClick = beiDruck, role = Role.Button)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Text(
            "$nummer",
            style = Schrift.MonoNormal.copy(fontWeight = FontWeight.ExtraBold),
            color = Farben.TextSehrLeise,
            modifier = Modifier.width(22.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
            Text(antwort.text, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            Leise(antwort.erklaerung)
        }
    }
}
