package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.netz.Codevorschau
import de.pagerspass.pagerspass.netz.DiscordStatus
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kontowege
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Seiten, die man meist von außen erreicht — aus Discord, einem Video, einer
 * E-Mail: Discord-Verknüpfung, Creator-Code, Newsletter-Abmeldung. Übertragen aus
 * `DiscordVerknuepfenView.vue`, `CodeView.vue` und `NewsletterAbmeldenView.vue`.
 */

// ---------------------------------------------------------------- Discord

/**
 * Die Discord-Verknüpfung: ein Konto, ein Knopf, danach vergibt Discord die
 * Dienstgrad-Rollen von selbst.
 *
 * Der eigentliche Tausch läuft auf dem Server; die Seite holt nur die vom Server
 * gebaute OAuth-Adresse ab und öffnet sie im Browser. Zurück kommt der Browser
 * über den App-Link mit `?fertig=1` oder `?fehler=…` (siehe `mobil/Einsprung.kt`).
 */
@Composable
fun DiscordSeite(
    wege: Kontowege,
    konto: Konto?,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    fertig: Boolean = false,
    rueckkehrfehler: String? = null,
    beiZurueck: () -> Unit = {},
) {
    val zusammenhang = LocalContext.current
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()
    var laedt by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf<DiscordStatus?>(null) }
    var loesenFrage by remember { mutableStateOf(false) }

    LaunchedEffect(konto?.kennung, fertig) {
        val kennung = konto?.kennung ?: run {
            laedt = false
            return@LaunchedEffect
        }
        runCatching { wege.discordStatus(kennung) }
            .onSuccess { status = it }
            .onFailure { v.fehler = it.message ?: "Der Stand ließ sich nicht laden." }
        laedt = false
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Discord",
            unterzeile = "Rolle und Verknüpfung",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        if (konto == null) {
            Leerhinweis("Für dieses Gerät ist kein Konto angemeldet.")
            return@Seite
        }

        if (fertig) {
            Erfolgszeile(
                "Verknüpft. Discord vergibt deine Dienstgrad-Rolle jetzt von selbst — das kann " +
                    "eine Minute dauern.",
            )
        } else if (rueckkehrfehler != null) {
            Warnzeile("Die Verknüpfung hat nicht geklappt: ${rueckkehrfehlerLesen(rueckkehrfehler)}")
        }
        v.fehler?.let { Warnzeile(it) }

        if (laedt) {
            Ladezeile()
            return@Seite
        }

        Abschnitt("Discord verknüpfen") {
            Kasten {
                val s = status
                if (s?.verknuepft == true) {
                    Text(
                        text = "Verknüpft mit ${s.discordName ?: "Discord"}",
                        style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                    )
                    SehrLeise(
                        "Seit ${tag(s.verknuepftAm)}. Dein Dienstgrad wird bei jedem Aufstieg " +
                            "nachgezogen; einmal täglich gleicht der Server zusätzlich ab.",
                    )
                    if (!loesenFrage) {
                        Knopf("Verknüpfung lösen", { loesenFrage = true }, art = Knopfart.Leise)
                    } else {
                        SehrLeise(
                            "Discord entzieht dir dann alle PagerSpass-Rollen. Dein Spielkonto bleibt " +
                                "unberührt; neu verknüpfen geht jederzeit.",
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(de.pagerspass.pagerspass.ui.theme.Abstand.Klein)) {
                            Knopf(
                                "Ja, lösen",
                                {
                                    bereich.vorgang(v, "Das Lösen ließ sich gerade nicht speichern.") {
                                        wege.discordLoesen(konto.kennung)
                                        status = DiscordStatus()
                                        loesenFrage = false
                                    }
                                },
                                aktiv = !v.laeuft,
                            )
                            Knopf("Behalten", { loesenFrage = false }, art = Knopfart.Leise)
                        }
                    }
                } else {
                    Text(
                        text = "Dein Dienstgrad als Discord-Rolle",
                        style = Schrift.Normal.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                    )
                    SehrLeise(
                        "Du meldest dich einmal bei Discord an und stimmst zu — danach trägt dein " +
                            "Discord-Profil „PagerSpass · ${konto.benutzername}\", und der Server gibt " +
                            "dir die Rolle zu deinem Rang (${konto.rang}) von selbst. Steigst du auf, " +
                            "zieht die Rolle nach.",
                    )
                    Knopf(
                        if (v.laeuft) "Einen Moment …" else "Mit Discord verbinden",
                        {
                            bereich.vorgang(v, "Discord ließ sich nicht erreichen.") {
                                val ziel = wege.discordStart(konto.kennung)
                                imBrowser(zusammenhang, ziel.url)
                            }
                        },
                        art = Knopfart.Haupt,
                        aktiv = !v.laeuft,
                    )
                }
            }
        }

        Abschnitt("Was Discord dabei erfährt") {
            Kasten {
                Leise(
                    "Deinen Benutzernamen, deine Laufbahnstufe, deinen Dienstgrad und die Zahl " +
                        "deiner gefahrenen Dienste — das ist alles, und nur solange die Verknüpfung " +
                        "besteht. Keine Schichten, keine Freunde, keine Nachrichten. PagerSpass " +
                        "erfährt umgekehrt nur deinen Discord-Namen.",
                )
            }
        }
    }
}

/**
 * Liest `?fehler=` — aber nur, was der eigene Server dort hinschreibt. Die Adresse
 * kann jeder bauen; Unbekanntes wird zur allgemeinen Meldung.
 */
private fun rueckkehrfehlerLesen(roh: String): String =
    if (roh in BEKANNTE_RUECKKEHRFEHLER) roh else "Versuch es bitte noch einmal."

private val BEKANNTE_RUECKKEHRFEHLER = setOf(
    "Die Verknüpfung ist auf diesem Server nicht eingerichtet.",
    "Die Zustimmung wurde abgebrochen.",
    "Der Anlauf war abgelaufen — versuch es noch einmal.",
    "Discord hat die Anmeldung nicht bestätigt.",
    "Zu diesem Anlauf gibt es kein Konto mehr.",
    "Das hat gerade nicht geklappt — versuch es später noch einmal.",
)

// ------------------------------------------------------------------- Code

/**
 * Die Seite hinter einem Creator-Code: `/code/LEITSTELLE200`.
 *
 * <b>Die einzige Seite, die etwas hergibt, bevor jemand ein Konto hat.</b> Erst
 * was es gibt, dann von wem, dann die Frage nach einem Konto. Der Weg ohne Konto
 * ist zweistufig: anmelden, zurück hierher, einlösen.
 */
@Composable
fun CodeSeite(
    code: String,
    wege: Kontowege,
    konto: Konto?,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    beiEingeloest: (Int?) -> Unit = {},
    beiAnmelden: () -> Unit = {},
    beiZumSpiel: () -> Unit = {},
    beiShop: () -> Unit = {},
    beiZurueck: (() -> Unit)? = null,
) {
    val zusammenhang = LocalContext.current
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()
    val gross = code.uppercase()
    var laedt by remember(gross) { mutableStateOf(true) }
    var vorschau by remember(gross) { mutableStateOf<Codevorschau?>(null) }
    var fehler by remember(gross) { mutableStateOf<String?>(null) }
    var ertrag by rememberSaveable(gross) { mutableStateOf<String?>(null) }

    LaunchedEffect(gross) {
        runCatching { wege.codevorschau(gross) }
            .onSuccess { vorschau = it }
            .onFailure { fehler = it.message ?: "Diesen Code gibt es nicht." }
        laedt = false
    }

    Seite(modifier = modifier, unterrand = unterrand, breite = Mass.Lesebreite) {
        if (beiZurueck != null) {
            Seitenkopf(
                titel = "Code",
                knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
            )
        }

        val vs = vorschau
        when {
            laedt -> Leise("Der Code wird geprüft …")

            // Kein Warnzeichen: Am wahrscheinlichsten hat sich jemand vertippt oder
            // die Aktion ist vorbei.
            vs == null -> Kasten {
                Etikett("Code")
                Text("Diesen Code gibt es nicht", style = Schrift.Titel, color = Farben.Text)
                Leise(
                    (
                        "${fehler.orEmpty()} Prüf ihn noch einmal, Zeichen für Zeichen — oder sieh " +
                            "im Spiel nach, ob die Aktion schon vorbei ist."
                        ).trimStart(),
                )
                Knopf("Zu PagerSpass", beiZumSpiel, art = Knopfart.Haupt)
            }

            else -> Kasten(marke = true) {
                Etikett("Code von")
                Text(vs.creator, style = Schrift.Schlagzeile, color = Farben.Text)
                vs.kanal?.takeIf { it.isNotBlank() }?.let { kanal ->
                    Textweg("Zum Kanal", { imBrowser(zusammenhang, kanal) })
                }
                Text(
                    text = vs.code,
                    style = Schrift.MonoNormal.copy(fontSize = Schrift.TITEL, letterSpacing = 0.12.em),
                    color = Farben.Amber,
                    textAlign = TextAlign.Start,
                )
                Text(vs.belohnung, style = Schrift.Gross, color = Farben.Text)

                val erreicht = ertrag
                when {
                    erreicht != null -> {
                        Erfolgszeile("Eingelöst — $erreicht")
                        Knopf("In den Shop", beiShop, art = Knopfart.Haupt)
                    }

                    !vs.einloesbar -> {
                        Warnzeile(vs.grund ?: "Dieser Code gilt gerade nicht.")
                        Knopf("Trotzdem reinschauen", beiZumSpiel)
                    }

                    konto != null -> {
                        Knopf(
                            if (v.laeuft) "Wird eingelöst …" else "Jetzt einlösen",
                            {
                                bereich.vorgang(v) {
                                    val antwort = wege.codeEinloesen(konto.kennung, gross)
                                    ertrag = antwort.text
                                    beiEingeloest(antwort.neueCredits)
                                }
                            },
                            art = Knopfart.Haupt,
                            aktiv = !v.laeuft,
                        )
                        v.fehler?.let { Warnzeile(it) }
                    }

                    else -> {
                        Knopf("Konto anlegen und einlösen", beiAnmelden, art = Knopfart.Haupt)
                        SehrLeise(
                            "Kostenlos. Nach dem Anlegen landest du wieder hier und der Code wird " +
                                "gutgeschrieben.",
                        )
                    }
                }

                SehrLeise(
                    "PagerSpass ist ein Leitstellenspiel — du disponierst Feuerwehr und " +
                        "Rettungsdienst auf echten Karten. Ein Spiel, kein Einsatzmittel: Bei echten " +
                        "Notfällen wählst du die 112.",
                )
            }
        }
    }
}

// ------------------------------------------------------------- Newsletter

/**
 * Der Abmeldelink aus jeder Newsletter-Nachricht.
 *
 * <b>Die Seite handelt von selbst.</b> § 7 Abs. 3 Nr. 4 UWG verlangt den
 * Widerspruch ohne Hürde — ein Bestätigungsknopf wäre eine, eine Anmeldung erst
 * recht. Der Schlüssel im Link genügt.
 */
@Composable
fun NewsletterAbmeldenSeite(
    schluessel: String?,
    wege: Kontowege,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    beiZumSpiel: () -> Unit = {},
) {
    var stand by rememberSaveable(schluessel) { mutableStateOf("laeuft") }
    var meldung by rememberSaveable(schluessel) { mutableStateOf("") }

    LaunchedEffect(schluessel) {
        if (stand != "laeuft") return@LaunchedEffect
        if (schluessel.isNullOrBlank()) {
            stand = "fehler"
            meldung = "In diesem Link fehlt der Schlüssel. Bitte öffne ihn direkt aus der E-Mail."
            return@LaunchedEffect
        }
        runCatching { wege.newsletterAbmelden(schluessel) }
            .onSuccess {
                stand = "fertig"
                meldung = it.meldung
            }
            .onFailure {
                stand = "fehler"
                meldung = it.message ?: "Das hat gerade nicht geklappt."
            }
    }

    Seite(modifier = modifier, unterrand = unterrand, breite = Mass.Lesebreite) {
        Kasten {
            Text("Newsletter", style = Schrift.Titel, color = Farben.Text)
            when (stand) {
                "laeuft" -> SehrLeise("Einen Augenblick …")
                "fertig" -> {
                    Erfolgszeile(meldung)
                    SehrLeise(
                        "Du kannst ihn jederzeit wieder bestellen — in deinem Konto unter " +
                            "„E-Mail-Adresse\".",
                    )
                }
                else -> Warnzeile(meldung)
            }
            Knopf("Zum Spiel", beiZumSpiel, art = Knopfart.Haupt)
        }
    }
}
