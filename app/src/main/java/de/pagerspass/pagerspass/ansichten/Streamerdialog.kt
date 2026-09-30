package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import de.pagerspass.pagerspass.mobil.Einstellungsaenderung
import de.pagerspass.pagerspass.mobil.Raumbefehle
import de.pagerspass.pagerspass.netz.Einwilligungsbedarf
import de.pagerspass.pagerspass.netz.MAX_STREAMERKANAL
import de.pagerspass.pagerspass.netz.MINDESTALTER_EINWILLIGUNG
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.STREAMERPLATTFORMEN
import de.pagerspass.pagerspass.netz.UEBERTRAGUNG_FASSUNG
import de.pagerspass.pagerspass.netz.plattformName
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Übertragung einer Schicht — übertragen aus `StreamerDialog.vue`,
 * `LiveDialog.vue`, `LiveKnopf.vue` und `ZuschauerZaehler.vue`.
 *
 * Zwei Seiten derselben Sache: Wer überträgt, stellt Kanal, Plattform und
 * Mitschnitt ein und meldet sich live (`Streamenblende`); wer mitfährt, wird
 * vorher gefragt (`Einwilligungsblende`). Beides verlangt der Server — hier steht
 * nur, was er zur Frage macht.
 */

/**
 * Die Frage, die dem eigenen Platz gestellt werden muss, weil die Leitstelle den
 * Streamer-Modus eingeschaltet oder den Kanal gewechselt hat, während man schon
 * saß — gerechnet aus dem Raumzustand wie `einwilligungAmPlatz` im Web.
 *
 * <b>Der Streamer selbst wird nie gefragt:</b> Er erklärt seine Übertragung, er
 * willigt nicht in sie ein — und der Server wiese seine Einwilligung ab.
 */
fun einwilligungAmPlatz(raum: Raumzustand?, eigeneKennung: String, elternbogen: String?): Einwilligungsbedarf? {
    val r = raum ?: return null
    val s = r.settings
    val kanal = s.streamerkanal?.takeIf { it.isNotBlank() } ?: return null
    if (!s.streamermodus) return null
    val ich = r.players.firstOrNull { it.id == eigeneKennung } ?: return null
    if (ich.streamerfreigabe || ich.id == s.streamerKontoId) return null

    return Einwilligungsbedarf(
        raumCode = r.code,
        streamerName = r.players.firstOrNull { it.id == s.streamerKontoId }?.name ?: "Die Leitstelle",
        plattform = s.streamerplattform,
        kanal = kanal,
        aufzeichnung = s.streameraufzeichnung,
        fassung = UEBERTRAGUNG_FASSUNG,
        mindestalterAllein = MINDESTALTER_EINWILLIGUNG,
        wartetAufEltern = elternbogen != null,
        elternbogen = elternbogen,
    )
}

/**
 * Die Frage vor dem Betreten einer übertragenen Schicht — und der Bogen für die
 * Erziehungsberechtigten, wenn sie nötig wird.
 *
 * <b>Eine Einwilligung und keine Selbstverpflichtung.</b> Der Gegenstand steht
 * oben als Steckbrief, der Wortlaut vollständig darunter, und beide Knöpfe sind
 * gleich groß: Eine Einwilligung, bei der das „Ja" leuchtet und das „Nein" ein
 * grauer Link ist, ist keine freie Entscheidung.
 *
 * <b>Sie lässt sich nicht wegwischen.</b> Wer nicht einwilligen will, drückt
 * „Nicht einwilligen" — derselbe Weg hinaus, nur einer, der auch das Richtige tut
 * (am Platz verlässt er die Runde).
 */
@Composable
fun Einwilligungsblende(
    frage: Einwilligungsbedarf,
    beiErteilen: (volljaehrig: Boolean, beiEnde: (Boolean, String?) -> Unit) -> Unit,
    beiAblehnen: () -> Unit,
) {
    val zwischenablage = LocalClipboardManager.current
    // Frisch je Frage — die Blende lebt kurz.
    var volljaehrig by remember(frage.raumCode, frage.kanal) { mutableStateOf<Boolean?>(null) }
    var gelesen by remember(frage.raumCode, frage.kanal) { mutableStateOf(false) }
    var laeuft by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var kopiert by remember { mutableStateOf(false) }
    LaunchedEffect(kopiert) {
        if (kopiert) {
            kotlinx.coroutines.delay(2_000)
            kopiert = false
        }
    }

    val wohin = "${plattformName(frage.plattform)} · ${frage.kanal}"
    val bogen = frage.elternbogen
    val bereit = volljaehrig != null && gelesen && !laeuft

    Blende(
        titel = "Diese Schicht wird übertragen",
        beiSchliessen = {},
        schliessenMoeglich = false,
        breite = Dialogbreite.Breit,
        fuss = {
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein), modifier = Modifier.fillMaxWidth()) {
                Knopf(
                    "Nicht einwilligen",
                    {
                        laeuft = true
                        beiAblehnen()
                        laeuft = false
                    },
                    aktiv = !laeuft,
                    modifier = Modifier.weight(1f),
                )
                if (bogen == null) {
                    Knopf(
                        if (laeuft) "Wird gespeichert…" else "Einwilligen und beitreten",
                        {
                            if (!bereit) return@Knopf
                            laeuft = true
                            fehler = null
                            beiErteilen(volljaehrig == true) { _, f ->
                                fehler = f
                                laeuft = false
                            }
                        },
                        art = Knopfart.Haupt,
                        aktiv = bereit,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    ) {
        Etikett("Einwilligung")
        // Der Steckbrief zuerst: Er ist der Gegenstand der Einwilligung.
        Steckbriefzeile("Wer überträgt", frage.streamerName)
        Steckbriefzeile("Wohin", wohin, mono = true)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Bleibt es stehen?", style = Schrift.Klein, color = Farben.TextSehrLeise, modifier = Modifier.weight(1f))
            Marke(
                if (frage.aufzeichnung) "Ja — wird aufgezeichnet" else "Nein — nur live",
                farbe = if (frage.aufzeichnung) Farben.SignalHell else Farben.GruenHell,
            )
        }

        if (bogen != null) {
            // Der Bogen hat den Wortlaut abgelöst: Antworten ist hier nichts mehr,
            // es fehlen die Eltern.
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.FlaecheHoch, Rundung.Klein)
                    .padding(Abstand.Normal),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Jetzt sind deine Eltern dran.") }
                        append(
                            " Gib diesen Link an einen Erziehungsberechtigten weiter. Er füllt dort einen " +
                                "kurzen Bogen aus und unterschreibt. Sobald er eingegangen ist, kommst du in die Runde.",
                        )
                    },
                    style = Schrift.Klein,
                    color = Farben.Text,
                )
                Text(bogen, style = Schrift.MonoKlein, color = Farben.AmberHell)
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(if (kopiert) "Kopiert" else "Link kopieren", {
                        zwischenablage.setText(AnnotatedString(bogen))
                        kopiert = true
                    }, kompakt = true)
                    Knopf("Ist er schon da?", {
                        laeuft = true
                        fehler = null
                        beiErteilen(false) { gilt, f ->
                            fehler = f ?: if (!gilt) "Der Bogen ist noch nicht eingegangen." else null
                            laeuft = false
                        }
                    }, kompakt = true, aktiv = !laeuft)
                }
                SehrLeise(
                    "Der Link gilt für diesen einen Vorgang. Du findest ihn außerdem jederzeit " +
                        "unter „Konto → Privatsphäre → Übertragungen“.",
                )
            }
        } else {
            Rechtstextbloecke(UEBERTRAGUNG_TEXT)

            // Die Altersfrage steht unter dem Text: Sie ist erst zu beantworten,
            // wenn man weiß, worum es geht. Drei Zustände — ohne Antwort ist
            // nichts behauptet.
            Ueberschrift("Wie alt bist du?")
            Hakenzeile(
                "Ich bin ${frage.mindestalterAllein} oder älter",
                an = volljaehrig == true,
                beiWechsel = { volljaehrig = if (it) true else null },
            )
            Hakenzeile(
                "Ich bin jünger — dann brauchen wir noch die Unterschrift eines " +
                    "Erziehungsberechtigten; du bekommst danach einen Link dafür.",
                an = volljaehrig == false,
                beiWechsel = { volljaehrig = if (it) false else null },
            )
            Hakenzeile(
                buildAnnotatedString {
                    append("Ich habe den Text gelesen und willige ein, dass mein Name, meine Stimme und was ich in dieser Runde schreibe über ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(wohin) }
                    append(" öffentlich übertragen ${if (frage.aufzeichnung) "und aufgezeichnet " else ""}werden.")
                },
                an = gelesen,
                beiWechsel = { gelesen = it },
            )
        }

        fehler?.let { Text(it, style = Schrift.Klein, color = Farben.SignalHell) }
        SehrLeise("Fassung ${frage.fassung} · nachzulesen unter „Übertragung einer Schicht“", mono = true)
    }
}

@Composable
private fun Steckbriefzeile(was: String, wert: String, mono: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(was, style = Schrift.Klein, color = Farben.TextSehrLeise, modifier = Modifier.weight(1f))
        Text(
            wert,
            style = if (mono) Schrift.MonoKlein else Schrift.Klein.copy(fontWeight = FontWeight.Bold),
            color = Farben.Text,
            modifier = Modifier.weight(1.6f),
        )
    }
}

/**
 * Ein Rechtstext in der Schreibweise von `recht/markdown.ts` — Absätze,
 * Zwischentitel (`## `), Listen (`- `) und Fettgesetztes (`**…**`). Mehr kennt der
 * Einwilligungstext nicht, und mehr braucht es hier nicht.
 */
@Composable
private fun ColumnScope.Rechtstextbloecke(text: String) {
    text.trim().split(Regex("\n\\s*\n")).forEach { block ->
        val zeilen = block.lines()
        when {
            block.startsWith("## ") -> Text(
                block.removePrefix("## ").trim(),
                style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                color = Farben.Text,
            )
            zeilen.all { it.trimStart().startsWith("- ") } -> Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
                zeilen.forEach { z ->
                    Text(fett("• " + z.trimStart().removePrefix("- ")), style = Schrift.Klein, color = Farben.TextLeise)
                }
            }
            else -> Text(fett(zeilen.joinToString("\n")), style = Schrift.Klein, color = Farben.TextLeise)
        }
    }
}

private fun fett(satz: String): AnnotatedString = buildAnnotatedString {
    var rest = satz
    val muster = Regex("\\*\\*([^*]+)\\*\\*")
    while (true) {
        val treffer = muster.find(rest) ?: break
        append(rest.substring(0, treffer.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Farben.Text)) { append(treffer.groupValues[1]) }
        rest = rest.substring(treffer.range.last + 1)
    }
    append(rest)
}

/**
 * „Streamen" — die eine Stelle für alles, was mit Übertragungen zu tun hat: oben
 * die Übertragung der Runde (die Leitstelle stellt sie, alle anderen lesen sie),
 * darunter die eigene Live-Meldung (`LiveDialog.vue`).
 *
 * <b>Die Bedingungen sind einzeln zu bestätigen.</b> Drei Sätze über einem Haken
 * liest niemand, drei Haken schon — und der erste ist der, der die Runde für alle
 * anderen rettet: sich im Stream nicht selbst zu hören.
 */
@Composable
fun Streamenblende(
    raum: Raumzustand,
    eigeneKennung: String,
    befehle: Raumbefehle,
    beiSchliessen: () -> Unit,
) {
    val s = raum.settings
    val ich = raum.players.firstOrNull { it.id == eigeneKennung }
    val istLeitstelle = ich?.istLeitstelle == true
    val live = ich?.live == true
    val menschen = raum.players.filter { !it.istBot }
    val offen = menschen.filter { !it.streamerfreigabe }
    val bestaetigt = remember { mutableStateOf(List(LIVE_BEDINGUNGEN.size) { false }) }
    var kanal by remember(s.streamerkanal) { mutableStateOf(s.streamerkanal.orEmpty()) }

    // Ohne Streamer-Modus hat niemand eingewilligt — und allein gegen Bots braucht
    // es keine Erlaubnis. Dieselbe Bedingung wie am Server (`LiveSetzen`).
    val fehltDerModus = !(s.streamermodus && !s.streamerkanal.isNullOrBlank()) &&
        raum.players.any { !it.istBot && it.id != eigeneKennung }
    val alleBestaetigt = bestaetigt.value.all { it } && !fehltDerModus

    fun kanalSenden() {
        if (kanal.trim() != s.streamerkanal.orEmpty()) {
            befehle.einstellungen(Einstellungsaenderung(streamerkanal = kanal.trim()))
        }
    }

    Blende(
        titel = "Streamen",
        beiSchliessen = {
            if (istLeitstelle) kanalSenden()
            beiSchliessen()
        },
        fuss = {
            if (live) {
                Knopf("Stream abmelden", {
                    befehle.live(false)
                    beiSchliessen()
                }, art = Knopfart.Haupt, breit = true)
            } else {
                Knopf("Live gehen", {
                    if (!alleBestaetigt) return@Knopf
                    befehle.live(true)
                    beiSchliessen()
                }, art = Knopfart.Haupt, aktiv = alleBestaetigt, breit = true)
            }
        },
    ) {
        Ueberschrift("Übertragung der Runde")

        if (istLeitstelle) {
            Schalterzeile(
                titel = "Streamer-Modus",
                unterzeile = "Diese Schicht geht nach außen. Jeder, der beitritt oder zusieht, wird vorher " +
                    "gefragt — und die Runde startet erst, wenn alle geantwortet haben.",
                an = s.streamermodus,
                beiWechsel = { an ->
                    // Alle vier in einem Griff: Der Server weist „an" ohne Kanal ab,
                    // übernimmt den Kanal aber vor dem Schalter.
                    befehle.einstellungen(
                        Einstellungsaenderung(
                            streamermodus = an,
                            streamerkanal = kanal.trim(),
                            streamerplattform = s.streamerplattform,
                            streameraufzeichnung = s.streameraufzeichnung,
                        ),
                    )
                },
                aktiv = kanal.isNotBlank(),
            )
            if (kanal.isBlank()) {
                Text("Trag zuerst den Kanal ein — ohne ihn kann niemand einwilligen.", style = Schrift.Klein, color = Farben.AmberHell)
            }

            // Die Angaben stehen auch bei ausgeschaltetem Schalter — man tippt den
            // Kanal, *dann* schaltet man ein.
            Feld(
                wert = kanal,
                beiAenderung = { kanal = it.take(MAX_STREAMERKANAL) },
                etikett = "Kanal",
                platzhalter = "twitch.tv/deine-leitstelle",
                weiterTaste = androidx.compose.ui.text.input.ImeAction.Done,
            )
            if (kanal.trim() != s.streamerkanal.orEmpty()) {
                Knopf("Kanal übernehmen", ::kanalSenden, kompakt = true)
            }
            SehrLeise(
                "Steht im Bogen, den deine Mitspieler zu sehen bekommen — und bei einem Minderjährigen " +
                    "in dem, den seine Eltern unterschreiben. Schreib ihn so, dass man dich damit findet.",
            )

            Etikett("Wohin")
            Pillenreihe {
                STREAMERPLATTFORMEN.forEach { (id, name) ->
                    Pille(name, an = s.streamerplattform == id, beiDruck = {
                        befehle.einstellungen(Einstellungsaenderung(streamerplattform = id))
                    })
                }
            }

            Schalterzeile(
                titel = "Wird aufgezeichnet",
                unterzeile = "Ein Mitschnitt bleibt stehen und ist Jahre später noch auffindbar — deshalb ist " +
                    "er eine eigene Frage. Wer nur dem Livestream zugestimmt hat, wird erneut gefragt.",
                an = s.streameraufzeichnung,
                beiWechsel = { befehle.einstellungen(Einstellungsaenderung(streameraufzeichnung = it)) },
            )

            if (offen.isNotEmpty()) {
                Text(
                    buildAnnotatedString {
                        append("Es fehlt noch die Zustimmung von ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(offen.joinToString(", ") { it.name }) }
                        append(".")
                    },
                    style = Schrift.Klein,
                    color = Farben.AmberHell,
                )
            } else if (s.streamermodus) {
                Text("Alle Mitspieler haben zugestimmt.", style = Schrift.Klein, color = Farben.GruenHell)
            }
        } else if (s.streamermodus) {
            Text(
                buildAnnotatedString {
                    append("Diese Runde wird übertragen: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.streamerkanal.orEmpty()) }
                    append(" (${plattformName(s.streamerplattform)})")
                    if (s.streameraufzeichnung) append(", mit Aufzeichnung")
                    append(". Einstellen kann das nur die Leitstelle.")
                },
                style = Schrift.Klein,
                color = Farben.Text,
            )
        } else {
            SehrLeise("Diese Runde wird nicht übertragen. Den Streamer-Modus schaltet die Leitstelle — hier, in diesem Dialog.")
        }

        Ueberschrift("Selbst live melden")
        if (live) {
            Text(
                "Du stehst gerade als live — die Marke steht an deinem Namen in der Lobby und in der " +
                    "Mannschaftsliste. Wenn du aufhörst zu streamen, melde dich ab.",
                style = Schrift.Klein,
                color = Farben.Text,
            )
        } else {
            SehrLeise(
                "Deine Mitspieler sehen dann eine Marke an deinem Namen — in der Lobby und in der " +
                    "Mannschaftsliste am Arbeitsplatz. Bestätige vorher drei Dinge:",
            )
            LIVE_BEDINGUNGEN.forEachIndexed { i, satz ->
                Hakenzeile(
                    fett(satz),
                    an = bestaetigt.value[i],
                    beiWechsel = { an -> bestaetigt.value = bestaetigt.value.toMutableList().also { it[i] = an } },
                )
            }
            if (fehltDerModus) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Erst der Streamer-Modus.") }
                        append(
                            " In dieser Runde spielen andere mit, und gefragt wurde niemand. Trag oben deinen Kanal " +
                                "ein und schalte den Streamer-Modus ein — dann bekommt jeder Mitspieler die Frage " +
                                "gestellt, und du kannst dich hier anmelden. Bist du nicht die Leitstelle, bitte sie darum.",
                        )
                    },
                    style = Schrift.Klein,
                    color = Farben.AmberHell,
                )
            }
            SehrLeise("Abmelden kannst du dich jederzeit an derselben Stelle — ohne diese Rückfrage.")
        }
    }
}

private val LIVE_BEDINGUNGEN = listOf(
    "Ich höre mich im Stream **nicht** selbst — meine Wiedergabe läuft nicht über die Lautsprecher, " +
        "mit denen ich den Funk höre. (Sonst geht jeder Funkspruch verzögert wieder in mein Mikrofon, " +
        "und für alle anderen klingt die Runde nach Echo.)",
    "Mir ist bewusst, dass ich damit die Stimmen und Namen meiner Mitspieler öffentlich übertrage.",
    "Ich blende keine Anmeldedaten, keine Adresszeile mit Rundencode und nichts aus meinem Konto ein, " +
        "was nicht dorthin gehört.",
)

/**
 * Auge plus Zahl: wie viele gerade zusehen, ohne einen Platz zu belegen. Nur, wer
 * wirklich dranhängt — ein Zuschauerplatz bleibt nach einem Abriss für den
 * Wiedereinstieg stehen, und mitgezählt wiese er die Anzeige zu hoch aus.
 */
@Composable
fun Zuschauerzaehler(raum: Raumzustand) {
    val da = raum.zuschauer.count { it.verbunden }
    if (da == 0) return
    Marke("👁 $da", farbe = Farben.TextLeise)
}

/** Die Marke am Namen eines Platzes, der sich als live gemeldet hat. */
@Composable
fun Livemarke() = Marke("LIVE", farbe = Farben.SignalHell)

/**
 * Der Wortlaut der Übertragungs-Einwilligung — zeichengleich `UEBERTRAGUNG` in
 * `web/src/recht/rechtstexte.ts`, Fassung [UEBERTRAGUNG_FASSUNG].
 *
 * <b>Er steht in der App und nicht hinter einem Link.</b> Eine Einwilligung
 * verlangt, dass der Text vorliegt, wenn gefragt wird. Wer hier ein Wort ändert,
 * ändert es dort mit und hebt die Fassung — der Server prüft sie.
 */
private val UEBERTRAGUNG_TEXT = """
Diese Schicht wird von der Leitstelle nach außen übertragen — an ein Publikum, das wir nicht kennen und nicht auswählen. Damit dabei etwas von dir zu sehen und zu hören sein darf, brauchen wir deine Einwilligung. Sie ist freiwillig; ohne sie kannst du in dieser Runde nicht mitfahren, und das Spiel bleibt im Übrigen vollständig nutzbar.

## Wer überträgt

Der Kanal, die Plattform und die Angabe, ob mitgeschnitten wird, stehen in der Frage, die dir gestellt wird, sowie in der Lobby der Runde. Sie sind der Gegenstand deiner Einwilligung: Wechselt die Leitstelle den Kanal oder die Plattform, oder schaltet sie den Mitschnitt hinzu, wirst du erneut gefragt. Eine Einwilligung gilt nur für das, was in ihr steht.

Übertragen wird von der Leitstelle der Runde, nicht von uns. PagerSpass betreibt keinen Kanal, schneidet nichts mit und leitet nichts weiter.

## Was von dir dabei sein kann

- dein Anzeigename, dein Wappen, dein Rang und dein Profilbild, wie sie in Lobby und Mannschaftsliste stehen,
- deine **Stimme**, wenn du den Sprechfunk benutzt — und mit ihr alles, was sie über dich verrät,
- alles, was du in dieser Runde schreibst: Funksprüche, Lagemeldungen, Nachforderungen, Lobby-Chat, Einsatzstellenfunk und Leitstellendraht,
- deine Rolle, dein Fahrzeug, sein Funkrufname und was du damit tust.

Was **nicht** dabei sein kann, weil die Leitstelle es selbst nicht sieht: dein Benutzername, deine E-Mail-Adresse, dein Passwort, deine Direktnachrichten und alles Übrige aus deinem Konto.

## Was wir dazu speichern

Damit wir belegen können, dass du gefragt wurdest und was du geantwortet hast (Art. 7 Abs. 1 DSGVO): deine Kontokennung, die Kennung und den Anzeigenamen der übertragenden Leitstelle, Plattform, Kanal und Mitschnitt-Angabe, den Zeitpunkt, die Fassung dieses Textes samt Prüfwert, den Code der Runde, bei deren Gelegenheit du gefragt wurdest, deine Angabe zur Volljährigkeit und — als gesalzenen Hash, nicht im Klartext — IP-Adresse und Gerätekennung des Geräts, von dem aus du geantwortet hast. Näheres in Ziffer 6 b der Datenschutzerklärung.

## Wenn du noch nicht 18 bist

Dann kannst du diese Einwilligung nicht allein erteilen: Es geht um die Verbreitung deiner Stimme und deines Namens an ein öffentliches Publikum, und dafür müssen deine Erziehungsberechtigten zustimmen. Sag es in der Frage ehrlich — du bekommst dann einen Link zu einem Bogen, den ein Erziehungsberechtigter ausfüllt und unterschreibt. Bis der eingegangen ist, kommst du in eine übertragene Runde nicht hinein; in jede andere Runde weiterhin.

Dass du 18 bist oder älter, ist deine eigene Angabe. Wir prüfen sie nicht nach — dazu müssten wir Ausweisdaten erheben, und PagerSpass erhebt bewusst kein Geburtsdatum (Ziffer 2 der Datenschutzerklärung). Eine falsche Angabe hilft dir nicht: Sie macht die Einwilligung unwirksam und dich gegenüber deinen Erziehungsberechtigten erklärungsbedürftig.

## Wie lange, und wie du sie zurücknimmst

Die Einwilligung gilt für ein Jahr; danach fragen wir erneut. Du kannst sie **jederzeit** widerrufen, ohne Angabe von Gründen, unter „Konto → Privatsphäre → Übertragungen". Der Widerruf wirkt für die Zukunft: Ab ihm darf von dir nichts mehr übertragen werden. Die Rechtmäßigkeit dessen, was bis dahin lief, bleibt unberührt.

## Was wir nicht können

Wir können nicht rückholen, was schon gesendet wurde, und wir können nichts aus einem fremden Kanal löschen. Über den Inhalt eines Streams oder einer Aufzeichnung entscheidet allein die Person, die ihn betreibt — sie ist dafür auch verantwortlich. Wenn du willst, dass ein Mitschnitt verschwindet, ist sie die Adresse; wir können dabei nur vermitteln. Genau deshalb fragen wir **vorher**.

Verantwortlich für die Verarbeitung auf unserer Seite sind wir (Impressum). Für die Übertragung selbst ist die Leitstelle verantwortlich, die sie vornimmt. Fragen zu deiner Einwilligung: support@pagerspass.de
"""
