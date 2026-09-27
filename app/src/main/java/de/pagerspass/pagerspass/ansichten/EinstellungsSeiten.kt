package de.pagerspass.pagerspass.ansichten

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.rememberAblage
import de.pagerspass.pagerspass.netz.Betreibermitteilung
import de.pagerspass.pagerspass.netz.Einwilligung
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kontowege
import de.pagerspass.pagerspass.netz.Mitteilungseinstellungen
import de.pagerspass.pagerspass.netz.Privatsphaere
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.netz.SICHTBARKEITEN
import de.pagerspass.pagerspass.netz.Sitzungsuebersicht
import de.pagerspass.pagerspass.netz.plattformname
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Ladezeile
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Segment
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * Datenschutz und Privatsphäre — `PrivatsphaereView.vue`.
 *
 * <b>Eine eigene Seite neben dem Konto</b>: sechzehn Entscheidungen, die zusammen
 * gelesen gehören — und daneben alle Wege, die die DSGVO einem gibt und die sich
 * selbst gehen lassen: Datenauszug, Einwilligungen samt Widerruf, Blockaden,
 * offene Sitzungen und was dieses Gerät selbst speichert.
 *
 * <b>Gespeichert wird sofort bei jeder Änderung.</b> Angezeigt wird danach, was
 * der Server gespeichert hat — nicht das Erhoffte. Nur die jüngste Anfrage darf
 * den Stand setzen; eine ältere kennt spätere Schalter noch nicht.
 */
@Composable
fun PrivatsphaereSeite(
    wege: Kontowege,
    konto: Konto?,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    beiAnalyse: suspend (Boolean) -> Unit = {},
    beiGeraetLeeren: () -> Unit = {},
    beiWeg: (String) -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    val zusammenhang = LocalContext.current
    val ablage = rememberAblage()
    val bereich = rememberCoroutineScope()
    val speichern = rememberDateiSpeichern("application/json")
    val zaehler = remember { AtomicInteger(0) }

    var einstellungen by remember { mutableStateOf(Privatsphaere()) }
    var laedt by remember { mutableStateOf(true) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var gespeichert by remember { mutableStateOf(false) }
    var datenverarbeitungOffen by remember { mutableStateOf(false) }

    var einwilligungen by remember { mutableStateOf<List<Einwilligung>?>(null) }
    var widerrufLaeuft by remember { mutableStateOf<String?>(null) }
    var blockaden by remember { mutableStateOf<List<Freund>?>(null) }
    var aufhebenLaeuft by remember { mutableStateOf<String?>(null) }
    var sitzungen by remember { mutableStateOf<List<Sitzungsuebersicht>?>(null) }
    var beendenLaeuft by remember { mutableStateOf(false) }
    var auszugLaeuft by remember { mutableStateOf(false) }
    var karten by remember { mutableStateOf(false) }
    var eintraege by remember { mutableStateOf(0) }
    var leerenFrage by remember { mutableStateOf(false) }

    val kennung = konto?.kennung

    suspend fun einwilligungenHolen(k: String) {
        runCatching { wege.einwilligungen(k) }
            .onSuccess { einwilligungen = it }
            .onFailure {
                einwilligungen = emptyList()
                fehler = it.message ?: "Die Übertragungen ließen sich nicht laden."
            }
    }

    suspend fun sitzungenHolen(k: String) {
        runCatching { wege.sitzungen(k) }
            .onSuccess { sitzungen = it }
            .onFailure {
                sitzungen = emptyList()
                fehler = it.message ?: "Die Sitzungen ließen sich nicht laden."
            }
    }

    LaunchedEffect(kennung) {
        karten = ablage.karteFreigabe() == "ja"
        eintraege = runCatching { ablage.eintragszahl() }.getOrDefault(0)
        val k = kennung ?: run {
            laedt = false
            return@LaunchedEffect
        }
        runCatching { wege.privatsphaere(k) }
            .onSuccess { einstellungen = it }
            .onFailure { fehler = it.message ?: "Die Einstellungen ließen sich nicht laden." }
        laedt = false

        // Unabhängig voneinander: Ein Fehlschlag bei den Sitzungen darf die
        // Blockaden nicht mitnehmen, und keiner von beiden die Schalter darüber.
        launch { einwilligungenHolen(k) }
        launch {
            runCatching { wege.freunde(k) }
                .onSuccess { liste -> blockaden = liste.filter { it.stand == "Blockiert" && it.vonMir } }
                .onFailure {
                    blockaden = emptyList()
                    fehler = it.message ?: "Die Blockaden ließen sich nicht laden."
                }
        }
        launch { sitzungenHolen(k) }
    }

    /** Übernimmt eine Änderung — und zeigt danach, was der Server gespeichert hat. */
    fun aendern(neu: Privatsphaere) {
        val k = kennung ?: return
        val nr = zaehler.incrementAndGet()
        val vorher = einstellungen
        einstellungen = neu
        fehler = null

        bereich.launch {
            try {
                val stand = wege.privatsphaereSpeichern(k, neu)
                if (nr != zaehler.get()) return@launch
                einstellungen = stand
                gespeichert = true
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (e: Exception) {
                if (nr != zaehler.get()) return@launch
                // Ein Schalter, der umspringt, obwohl nichts gespeichert wurde, ist
                // schlimmer als eine Fehlermeldung. Danach den echten Stand holen —
                // frühere Anfragen können durchgegangen sein.
                einstellungen = vorher
                gespeichert = false
                fehler = e.message ?: "Das ließ sich gerade nicht speichern."
                runCatching { wege.privatsphaere(k) }.onSuccess { if (nr == zaehler.get()) einstellungen = it }
            }
        }
    }

    val p = einstellungen

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Privatsphäre",
            unterzeile = "Wer was von dir sieht",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        if (konto == null) {
            Leerhinweis("Für dieses Gerät ist kein Konto angemeldet.")
            return@Seite
        }
        if (laedt) {
            Ladezeile()
            return@Seite
        }

        Leise(
            "Alles hier gilt serverseitig: Was abgeschaltet ist, wird nicht ausgeblendet, sondern " +
                "gar nicht erst herausgegeben. Bestehende Freundschaften bleiben von jeder dieser " +
                "Einstellungen unberührt.",
        )
        val meldung = fehler
        if (meldung != null) Warnzeile(meldung) else if (gespeichert) Erfolgszeile("Gespeichert.")

        // Der große Schalter steht allein und über allem: Er nimmt die Einzelnen
        // darunter zurück, und das soll man sehen, bevor man sie stellt.
        Abschnitt("Privates Konto") {
            Kasten(marke = true, abstandInnen = Abstand.Klein) {
                Schalterzeile(
                    titel = "Konto auf privat stellen",
                    unterzeile = "Du erscheinst weder in der Bestenliste noch in den Mitspieler-" +
                        "Vorschlägen anderer, und dein Spielstand (Level und Rang) ist außerhalb deiner " +
                        "Freundesliste nicht zu sehen. Die Einstellungen darunter gelten zusätzlich.",
                    an = p.privatesKonto,
                    beiWechsel = { aendern(p.copy(privatesKonto = it)) },
                )
            }
        }

        Abschnitt("Wer dich erreichen darf") {
            Kasten(abstandInnen = Abstand.Klein) {
                Stufenwahl(
                    titel = "Über den Benutzernamen findbar",
                    erklaerung = "Wer dich in der Suche finden darf. „Mitspieler\" heißt: nur, wer schon " +
                        "eine Schicht mit dir gefahren ist. Freunde finden dich immer.",
                    gewaehlt = p.auffindbarkeit,
                    beiWahl = { aendern(p.copy(auffindbarkeit = it)) },
                )
                Stufenwahl(
                    titel = "Freundschaftsanfragen von",
                    erklaerung = "Wer dir eine Anfrage schicken darf. Wer es nicht darf, bekommt dieselbe " +
                        "Auskunft wie bei einem Namen, den es nicht gibt — deine Einstellung verrät sich " +
                        "nicht.",
                    gewaehlt = p.anfragenVon,
                    beiWahl = { aendern(p.copy(anfragenVon = it)) },
                )
                Schalterzeile(
                    "Einladungen in Runden",
                    unterzeile = "Ob Freunde dich in ihre laufende Runde einladen dürfen. Aus heißt: sie " +
                        "sehen einen Hinweis, dass du gerade keine Einladungen möchtest.",
                    an = p.einladungenErlauben,
                    beiWechsel = { aendern(p.copy(einladungenErlauben = it)) },
                )
                Schalterzeile(
                    "Lesebestätigungen senden",
                    unterzeile = "Ob Absender sehen, dass du ihre Direktnachrichten gelesen hast. Aus " +
                        "heißt: bei ihnen steht kein „gelesen\" mehr — dein eigener Ungelesen-Zähler " +
                        "arbeitet unverändert weiter.",
                    an = p.lesebestaetigungenSenden,
                    beiWechsel = { aendern(p.copy(lesebestaetigungenSenden = it)) },
                )
                Schalterzeile(
                    "Kommentare unter deinen Einträgen",
                    unterzeile = "Ob unter deinen Brett-Einträgen kommentiert werden darf. Aus gilt für " +
                        "alle außer dir; bereits geschriebene Kommentare bleiben stehen.",
                    an = p.kommentareErlauben,
                    beiWechsel = { aendern(p.copy(kommentareErlauben = it)) },
                )
            }
        }

        // Die Blockaden gehören zu „wer dich erreichen darf" — die schärfste Antwort
        // darauf, für genau ein Konto. Nur die eigenen: Wer einen selbst blockiert
        // hat, erfährt davon nichts.
        Abschnitt("Blockierte Konten") {
            Kasten(abstandInnen = Abstand.Klein) {
                Leise(
                    "Wer hier steht, kann dir weder Anfragen noch Nachrichten schicken und findet dich " +
                        "nicht in der Suche. Davon erfährt er nichts. Blockieren kannst du in den " +
                        "Kontakten oder im Profil eines Kontos.",
                )
                val liste = blockaden
                when {
                    liste == null -> SehrLeise("Wird geladen …")
                    liste.isEmpty() -> Leerhinweis("Du hast niemanden blockiert.")
                    else -> liste.forEach { b ->
                        Listenzeile(titel = b.anzeigename, unter = "@${b.benutzername}", mono = true) {
                            Knopf(
                                if (aufhebenLaeuft == b.kennung) "Wird aufgehoben …" else "Blockade aufheben",
                                {
                                    aufhebenLaeuft = b.kennung
                                    fehler = null
                                    bereich.launch {
                                        runCatching { wege.blockadeAufheben(konto.kennung, b.kennung) }
                                            .onSuccess { blockaden = blockaden?.filter { it.kennung != b.kennung } }
                                            .onFailure {
                                                fehler = it.message ?: "Die Blockade ließ sich gerade nicht aufheben."
                                            }
                                        aufhebenLaeuft = null
                                    }
                                },
                                art = Knopfart.Leise,
                                kompakt = true,
                                aktiv = aufhebenLaeuft != b.kennung,
                            )
                        }
                    }
                }
                Knopf("Zu den Kontakten", { beiWeg("freunde/kontakte") }, art = Knopfart.Leise, kompakt = true)
            }
        }

        Abschnitt("Was andere von dir sehen") {
            Kasten(abstandInnen = Abstand.Klein) {
                Schalterzeile(
                    "Dienst anzeigen",
                    unterzeile = "Ob Freunde sehen, dass und in welcher Runde du gerade Dienst hast. Aus " +
                        "heißt: deine Zeile in ihrer Freundesliste bleibt still, auch während du spielst.",
                    an = p.anwesenheitZeigen,
                    beiWechsel = { aendern(p.copy(anwesenheitZeigen = it)) },
                )
                Schalterzeile(
                    "Zuletzt online anzeigen",
                    unterzeile = "Der Ersatzhinweis in der Freundesliste, solange du nicht im Dienst bist.",
                    an = p.zuletztGesehenZeigen,
                    beiWechsel = { aendern(p.copy(zuletztGesehenZeigen = it)) },
                )
                Schalterzeile(
                    "In der Bestenliste stehen",
                    unterzeile = "Ob dein Anzeigename in Bestenliste und Monatswertung auftaucht. Deine " +
                        "Punkte zählst du dir weiterhin selbst — im Dienstbuch stehen sie unabhängig davon.",
                    an = p.inBestenliste,
                    beiWechsel = { aendern(p.copy(inBestenliste = it)) },
                )
                Schalterzeile(
                    "Als Mitspieler vorgeschlagen werden",
                    unterzeile = "Ob du anderen nach einer gemeinsamen Schicht als möglicher Freund " +
                        "angeboten wirst. Deine eigenen Vorschläge bleiben davon unberührt.",
                    an = p.alsVorschlagErscheinen,
                    beiWechsel = { aendern(p.copy(alsVorschlagErscheinen = it)) },
                )
                Schalterzeile(
                    "In der Welt sichtbar",
                    unterzeile = "Ob dein Anzeigename und deine Stufe in PagerSpass - World zu sehen sind. " +
                        "Aus heißt: Deine Leitstelle steht weiter auf der Karte — mit dem Namen, den du ihr " +
                        "gegeben hast —, aber ohne dich und ohne Stufe, und in der Rangliste steht sie gar " +
                        "nicht.",
                    an = p.inWeltSichtbar,
                    beiWechsel = { aendern(p.copy(inWeltSichtbar = it)) },
                )
            }
        }

        Abschnitt("Wachengemeinschaft") {
            Kasten(abstandInnen = Abstand.Klein) {
                Schalterzeile(
                    "In Wachen-Gemeinschaften einladen",
                    unterzeile = "Ob dich Zugführer und Leitungen in ihre Wachengemeinschaft einladen " +
                        "dürfen. Getrennt von den Runden-Einladungen: die kommen von Freunden, diese hier " +
                        "auch von Fremden — und sie begründen eine dauerhafte Mitgliedschaft. Über den " +
                        "Beitrittscode kommst du weiterhin selbst hinein.",
                    an = p.gemeinschaftseinladungenErlauben,
                    beiWechsel = { aendern(p.copy(gemeinschaftseinladungenErlauben = it)) },
                )
                Schalterzeile(
                    "Gemeinschaft im Profil zeigen",
                    unterzeile = "Ob andere sehen, in welcher Wachengemeinschaft du bist — im Suchtreffer, " +
                        "im Profil, in der Freundesliste. Deine eigenen Mitglieder sehen es immer, sonst " +
                        "gäbe es keine Mitgliederliste.",
                    an = p.gemeinschaftZeigen,
                    beiWechsel = { aendern(p.copy(gemeinschaftZeigen = it)) },
                )
                Schalterzeile(
                    "In der öffentlichen Mitgliederliste stehen",
                    unterzeile = "Ist deine Gemeinschaft öffentlich gelistet, kann jeder ihre Mitglieder " +
                        "sehen. Aus heißt: dein Anzeigename fehlt dort — dabei bist du trotzdem.",
                    an = p.inGemeinschaftsliste,
                    beiWechsel = { aendern(p.copy(inGemeinschaftsliste = it)) },
                )
                Schalterzeile(
                    "Statistiken tracken",
                    unterzeile = "Ob deine gefahrenen Schichten der Wache namentlich zugeschrieben werden. " +
                        "Aus heißt: deine Punkte zählen weiter für die Stufe der Wache — ein Schalter von " +
                        "dir soll die Mannschaft nicht bestrafen —, werden aber ohne deine Kennung gebucht. " +
                        "Du tauchst dann in keiner Wachen-Rangliste auf.",
                    an = p.wachenstatistikTeilen,
                    beiWechsel = { aendern(p.copy(wachenstatistikTeilen = it)) },
                )
                Schalterzeile(
                    "Hilfsfristen in der Wachengemeinschaft",
                    unterzeile = "Ob die anderen deiner Gemeinschaft deine Hilfsfrist-Kurve im Dienstbuch " +
                        "sehen. Aus heißt: du zählst dort nicht mit.",
                    an = p.chronikTeilen,
                    beiWechsel = { aendern(p.copy(chronikTeilen = it)) },
                )
            }
        }

        // Die Aufzeichnung geht an `aendern` vorbei: Sie ist eine eigene Einwilligung
        // mit eigenem Weg — und mit einem dritten Zustand, „noch nicht gefragt".
        Abschnitt("Produktverbesserung") {
            Kasten(abstandInnen = Abstand.Klein) {
                Schalterzeile(
                    "Daten für Verbesserung der Produkte verwenden",
                    unterzeile = "Zeichnet den Funkverkehr mit den Bots und die Notrufgespräche mit der " +
                        "Leitstelle auf — was du funkst und was der Bot antwortet, samt Kennung, Rolle und " +
                        "Fahrzeug. Kein Ton, kein Name. Aus heißt: Es wird nichts mehr aufgezeichnet, und " +
                        "alles bisher Gespeicherte wird gelöscht.",
                    an = konto.analyseZustimmung == true,
                    beiWechsel = { an ->
                        fehler = null
                        bereich.launch {
                            runCatching { beiAnalyse(an) }
                                .onSuccess { gespeichert = true }
                                .onFailure {
                                    gespeichert = false
                                    fehler = it.message ?: "Das ließ sich gerade nicht speichern."
                                }
                        }
                    },
                )
                Knopf(
                    "Datenverarbeitung anzeigen",
                    { datenverarbeitungOffen = true },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        }

        // „Jederzeit widerruflich" (Art. 7 Abs. 3 DSGVO) heißt, dass es diese Stelle
        // geben muss. Ohne Rückfrage: Der Widerruf soll so leicht sein wie das Erteilen.
        Abschnitt("Übertragungen") {
            Kasten(abstandInnen = Abstand.Klein) {
                Leise(
                    "Wem du erlaubt hast, eine Schicht mit dir öffentlich zu übertragen. Ein Widerruf " +
                        "wirkt ab sofort: In eine Runde dieser Leitstelle kommst du danach nur noch, wenn " +
                        "du erneut einwilligst. Was bis dahin gesendet wurde, können wir nicht " +
                        "zurückholen — dafür ist verantwortlich, wer überträgt.",
                )
                val liste = einwilligungen
                when {
                    liste == null -> SehrLeise("Wird geladen …")
                    liste.isEmpty() -> Leerhinweis("Du hast noch keiner Übertragung zugestimmt.")
                    else -> liste.forEach { e -> Einwilligungszeile(e, widerrufLaeuft == e.id) {
                        widerrufLaeuft = e.id
                        bereich.launch {
                            runCatching { wege.einwilligungWiderrufen(konto.kennung, e.id) }
                                .onSuccess { einwilligungenHolen(konto.kennung) }
                                .onFailure { fehler = it.message ?: "Der Widerruf ging gerade nicht." }
                            widerrufLaeuft = null
                        }
                    } }
                }
                Knopf(
                    "Einwilligungstext lesen",
                    { beiWeg("recht/${Rechtsstand.UEBERTRAGUNG}") },
                    art = Knopfart.Leise,
                    kompakt = true,
                )
            }
        }

        Abschnitt("Dieses Gerät") {
            Kasten(abstandInnen = Abstand.Klein) {
                Schalterzeile(
                    "Kartenhintergrund laden",
                    unterzeile = "Die Kacheln der Lagekarte und der Navigation kommen von unserem eigenen " +
                        "Kartenserver, das Luftbild der Satelliten- und Hybridansicht von Esri in den USA; " +
                        "dabei erhält Esri deine IP-Adresse. Aus heißt: Sie werden nicht mehr geladen, " +
                        "Marker und Routen bleiben. Anders als die Schalter darüber gilt das nur für dieses " +
                        "Gerät — es ist eine Einwilligung, kein Serverwert.",
                    an = karten,
                    beiWechsel = { an ->
                        karten = an
                        bereich.launch { ablage.karteFreigabeSetzen(if (an) "ja" else "nein") }
                    },
                )
                Listenzeile(
                    titel = "Gespeicherte Daten dieses Geräts",
                    unter = "$eintraege Einträge: deine Anmeldung, die Kartenfreigabe, deine Bedienung und " +
                        "die Melder-Einstellungen. Sie gehen nicht an uns, aber auf einem geteilten Gerät " +
                        "liest sie der Nächste. Löschen meldet dich hier ab und setzt dieses Gerät zurück — " +
                        "dein Konto bleibt.",
                ) {
                    if (!leerenFrage) {
                        Knopf("Löschen", { leerenFrage = true }, art = Knopfart.Leise, kompakt = true)
                    }
                }
                if (leerenFrage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf("Ja, löschen", beiGeraetLeeren, art = Knopfart.Gefahr, kompakt = true)
                        Knopf("Abbrechen", { leerenFrage = false }, art = Knopfart.Leise, kompakt = true)
                    }
                }
            }
        }

        // Die offenen Sitzungen — hier und nicht nur unter „Sicherheit", weil die
        // Frage dieselbe ist wie bei jedem Schalter: Wer sieht gerade, was ich tue?
        Abschnitt("Angemeldete Geräte") {
            Kasten(abstandInnen = Abstand.Klein) {
                Leise(
                    "Wo dein Konto gerade angemeldet ist. Gerät und Ort speichern wir zu einer " +
                        "Anmeldung nicht — erkennen kannst du sie am Zeitpunkt. Eine Anmeldung verfällt " +
                        "nach 90 Tagen ohne Nutzung von selbst.",
                )
                val liste = sitzungen
                if (liste == null) {
                    SehrLeise("Wird geladen …")
                } else {
                    liste.forEach { s ->
                        Listenzeile(
                            titel = if (s.diese) "Dieses Gerät" else "Anderes Gerät",
                            unter = "Angemeldet am ${zeitpunkt(s.erstelltUm)} · zuletzt genutzt " +
                                zeitpunkt(s.zuletztGenutztUm),
                        ) {}
                    }
                }
                Knopf(
                    if (beendenLaeuft) "Wird abgemeldet …" else "Auf allen anderen Geräten abmelden",
                    {
                        beendenLaeuft = true
                        fehler = null
                        bereich.launch {
                            runCatching { wege.andereSitzungenBeenden(konto.kennung) }
                                .onSuccess { sitzungenHolen(konto.kennung) }
                                .onFailure { fehler = it.message ?: "Das Abmelden ging gerade nicht." }
                            beendenLaeuft = false
                        }
                    },
                    kompakt = true,
                    aktiv = !beendenLaeuft && (liste?.count { !it.diese } ?: 0) > 0,
                )
            }
        }

        // Jedes Recht aus Kapitel III der DSGVO mit seinem Weg.
        Abschnitt("Deine Rechte") {
            Kasten(abstandInnen = Abstand.Klein) {
                Listenzeile(
                    titel = "Auskunft und Datenübertragbarkeit",
                    unter = "Alles, was zu deinem Konto gespeichert ist, als eine JSON-Datei — Konto, " +
                        "Einstellungen, Dienstbuch, Garage, Nachrichten, Einträge, Gemeinschaften, " +
                        "Einwilligungen, Käufe und die Aufzeichnung zur Produktverbesserung (Art. 15 und " +
                        "20 DSGVO). Passwort- und Sitzungsabdrücke fehlen aus Sicherheitsgründen, " +
                        "Meldungen anderer über dich zum Schutz der Meldenden.",
                ) {
                    Knopf(
                        if (auszugLaeuft) "Wird erstellt …" else "Herunterladen",
                        {
                            auszugLaeuft = true
                            fehler = null
                            bereich.launch {
                                runCatching { wege.datenauszug(konto.kennung) }
                                    .onSuccess { json ->
                                        speichern("pagerspass-datenauszug-${LocalDate.now()}.json", json)
                                    }
                                    .onFailure {
                                        fehler = it.message ?: "Der Datenauszug ließ sich gerade nicht erstellen."
                                    }
                                auszugLaeuft = false
                            }
                        },
                        kompakt = true,
                        aktiv = !auszugLaeuft,
                    )
                }
                Listenzeile(
                    titel = "Berichtigung",
                    unter = "Anzeigename, Benutzername, E-Mail-Adresse und Profil änderst du selbst im " +
                        "Konto (Art. 16 DSGVO).",
                ) {
                    Knopf("Zum Konto", { beiWeg("konto") }, art = Knopfart.Leise, kompakt = true)
                }
                Listenzeile(
                    titel = "Löschung",
                    unter = "Dein Konto samt Garage, Dienstbuch, Freundschaften, Profilbild und " +
                        "Aufzeichnungen löschst du jederzeit selbst, ganz unten im Konto (Art. 17 DSGVO). " +
                        "Ein laufendes Premium-Abo endet dabei mit.",
                ) {
                    Knopf("Zum Konto", { beiWeg("konto") }, art = Knopfart.Leise, kompakt = true)
                }
                Listenzeile(
                    titel = "Widerruf von Einwilligungen",
                    unter = "Produktverbesserung, Übertragungen und Kartenhintergrund nimmst du oben mit " +
                        "einem Druck zurück, den Newsletter im Konto, Mitteilungen in deren Einstellungen, " +
                        "die Discord-Verknüpfung dort (Art. 7 Abs. 3 DSGVO). Der Widerruf wirkt für die " +
                        "Zukunft.",
                ) {
                    Knopf("Mitteilungen", { beiWeg("mitteilungen") }, art = Knopfart.Leise, kompakt = true)
                }
                Listenzeile(
                    titel = "Einschränkung und Widerspruch",
                    unter = "Du kannst verlangen, dass wir Daten nur noch aufbewahren, statt sie zu nutzen " +
                        "(Art. 18 DSGVO), und jeder Verarbeitung aus berechtigtem Interesse widersprechen " +
                        "(Art. 21 DSGVO). Eine formlose E-Mail an ${Rechtsstand.SUPPORT_EMAIL} genügt; " +
                        "wir antworten innerhalb eines Monats.",
                ) {
                    Knopf(
                        "E-Mail schreiben",
                        { mailSchreiben(zusammenhang, Rechtsstand.SUPPORT_EMAIL, "Datenschutz") },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
                Listenzeile(
                    titel = "Beschwerde bei einer Aufsichtsbehörde",
                    unter = "Du kannst dich jederzeit bei einer Datenschutz-Aufsichtsbehörde beschweren " +
                        "(Art. 77 DSGVO). Für uns zuständig ist der Landesbeauftragte für den Datenschutz " +
                        "Niedersachsen.",
                ) {
                    Knopf(
                        "Zur Behörde",
                        { imBrowser(zusammenhang, "https://www.lfd.niedersachsen.de") },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Knopf("Datenschutzerklärung", { beiWeg("recht/${Rechtsstand.DATENSCHUTZ}") }, kompakt = true)
                    // Für den, der nicht sechzehn Entscheidungen treffen will, sondern eine.
                    // Einwilligungen bleiben, wie sie sind: Sie haben eigene Wege.
                    Knopf(
                        "Alles auf maximalen Schutz",
                        {
                            aendern(
                                Privatsphaere(
                                    privatesKonto = true,
                                    auffindbarkeit = "Niemand",
                                    anfragenVon = "Niemand",
                                    einladungenErlauben = false,
                                    anwesenheitZeigen = false,
                                    zuletztGesehenZeigen = false,
                                    inBestenliste = false,
                                    alsVorschlagErscheinen = false,
                                    chronikTeilen = false,
                                    gemeinschaftseinladungenErlauben = false,
                                    gemeinschaftZeigen = false,
                                    inGemeinschaftsliste = false,
                                    wachenstatistikTeilen = false,
                                    kommentareErlauben = false,
                                    lesebestaetigungenSenden = false,
                                    inWeltSichtbar = false,
                                ),
                            )
                        },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                    Knopf(
                        "Auf Voreinstellung zurücksetzen",
                        { aendern(Privatsphaere()) },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
            }
        }
    }

    if (datenverarbeitungOffen) Datenverarbeitungblende { datenverarbeitungOffen = false }
}

/** Eine Einstellung mit drei Stufen — Alle, Mitspieler, Niemand. */
@Composable
private fun Stufenwahl(titel: String, erklaerung: String, gewaehlt: String, beiWahl: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Text(titel, style = Schrift.Normal, color = Farben.Text)
        SehrLeise(erklaerung)
        Segment(
            seiten = SICHTBARKEITEN,
            gewaehlt = gewaehlt,
            beiWahl = beiWahl,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Titel, Unterzeile, rechts ein Knopf — `.zeile` im Web. */
@Composable
private fun Listenzeile(
    titel: String,
    unter: String?,
    mono: Boolean = false,
    knopf: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier.weight(1f),
        ) {
            Text(titel, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
            if (unter != null) SehrLeise(unter, mono = mono)
        }
        knopf()
    }
}

/** Eine erteilte, widerrufene oder abgelaufene Einwilligung — samt Bogen, wo er noch fehlt. */
@Composable
private fun Einwilligungszeile(e: Einwilligung, laeuft: Boolean, beiWiderruf: () -> Unit) {
    val browser = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Winzig)) {
        Listenzeile(titel = e.streamerName, unter = "${plattformname(e.plattform)} · ${e.kanal}", mono = true) {
            // Widerrufen lässt sich nur, was noch steht.
            if (e.widerrufenUm == null) {
                Knopf(
                    if (laeuft) "Wird widerrufen …" else "Widerrufen",
                    beiWiderruf,
                    art = Knopfart.Leise,
                    kompakt = true,
                    aktiv = !laeuft,
                )
            }
        }
        SehrLeise(
            when {
                e.widerrufenUm != null -> "Widerrufen am ${tag(e.widerrufenUm)}"
                e.wartetAufEltern -> "Wartet auf die Unterschrift eines Erziehungsberechtigten"
                !e.gilt -> "Abgelaufen am ${tag(e.laeuftAbUm)} — beim nächsten Beitritt wird neu gefragt"
                else -> "Erteilt am ${tag(e.erteiltUm)}, gilt bis ${tag(e.laeuftAbUm)}"
            } + if (e.aufzeichnung) " · auch als Aufzeichnung" else "",
        )
        // Der Bogen bleibt auch hier erreichbar — der Vorgang läuft weiter, und die
        // Eltern sollen nicht auf den einen Augenblick in der Runde angewiesen sein.
        e.elternbogen?.let { bogen ->
            Textweg("Bogen für die Erziehungsberechtigten öffnen", { browser.openUri(bogen) })
        }
    }
}

// ------------------------------------------------------------ Mitteilungen

/**
 * Welche Mitteilungen ankommen sollen — und ob überhaupt welche ankommen können.
 * Das Gegenstück zu `MitteilungenView.vue`.
 *
 * <b>Über der Liste steht die Erlaubnis des Geräts.</b> Schalter, die man stellt,
 * während der Weg dahinter zu ist, sind eine Lüge auf dem Bildschirm. Ab Android
 * 13 ist das die Laufzeitberechtigung `POST_NOTIFICATIONS`; darunter (und danach)
 * zählt, ob die Mitteilungen der App in den Systemeinstellungen an sind — das
 * wird bei jeder Rückkehr auf die Seite neu gelesen.
 */
@Composable
fun MitteilungenSeite(
    wege: Kontowege,
    konto: Konto?,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    beiZurueck: () -> Unit = {},
) {
    val zusammenhang = LocalContext.current
    val lebenslauf = LocalLifecycleOwner.current
    val bereich = rememberCoroutineScope()
    val zaehler = remember { AtomicInteger(0) }

    var einstellungen by remember { mutableStateOf(Mitteilungseinstellungen()) }
    var laedt by remember { mutableStateOf(true) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var gespeichert by remember { mutableStateOf(false) }

    fun erlaubtLesen(): Boolean = NotificationManagerCompat.from(zusammenhang).areNotificationsEnabled()

    var erlaubt by remember { mutableStateOf(erlaubtLesen()) }
    // Ob die Frage schon einmal gestellt und abgelehnt wurde — dann fragt Android
    // nicht noch einmal, und der Weg führt über die Einstellungen.
    var abgelehnt by remember { mutableStateOf(false) }

    val frage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ja ->
        erlaubt = erlaubtLesen()
        abgelehnt = !ja
    }

    // Die Erlaubnis kann sich ändern, während die Seite offen steht — meist, weil
    // jemand in den Geräteeinstellungen war.
    DisposableEffect(lebenslauf) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            if (ereignis == Lifecycle.Event.ON_RESUME) erlaubt = erlaubtLesen()
        }
        lebenslauf.lifecycle.addObserver(beobachter)
        onDispose { lebenslauf.lifecycle.removeObserver(beobachter) }
    }

    LaunchedEffect(konto?.kennung) {
        val k = konto?.kennung ?: run {
            laedt = false
            return@LaunchedEffect
        }
        runCatching { wege.mitteilungseinstellungen(k) }
            .onSuccess { einstellungen = it }
            .onFailure { fehler = it.message ?: "Die Einstellungen ließen sich nicht laden." }
        laedt = false
    }

    fun aendern(neu: Mitteilungseinstellungen) {
        val k = konto?.kennung ?: return
        val nr = zaehler.incrementAndGet()
        val vorher = einstellungen
        einstellungen = neu
        fehler = null

        bereich.launch {
            try {
                val stand = wege.mitteilungseinstellungenSpeichern(k, neu)
                if (nr != zaehler.get()) return@launch
                einstellungen = stand
                gespeichert = true
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (e: Exception) {
                if (nr != zaehler.get()) return@launch
                einstellungen = vorher
                gespeichert = false
                fehler = e.message ?: "Das ließ sich gerade nicht speichern."
                runCatching { wege.mitteilungseinstellungen(k) }.onSuccess { if (nr == zaehler.get()) einstellungen = it }
            }
        }
    }

    fun einstellungenOeffnen() {
        val absicht = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, zusammenhang.packageName)
        runCatching { zusammenhang.startActivity(absicht) }
    }

    val e = einstellungen

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Mitteilungen",
            unterzeile = "Alarm und Nachrichten",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        if (konto == null) {
            Leerhinweis("Für dieses Gerät ist kein Konto angemeldet.")
            return@Seite
        }

        Leise(
            "Was hier aus ist, wird nicht stummgeschaltet, sondern gar nicht erst verschickt. Auf dem " +
                "Bildschirm bleibt trotzdem alles zu sehen — es geht nur um die Mitteilungen des Geräts.",
        )
        val meldung = fehler
        if (meldung != null) Warnzeile(meldung) else if (gespeichert) Erfolgszeile("Gespeichert.")

        Abschnitt("Dieses Gerät") {
            Kasten(abstandInnen = Abstand.Klein) {
                when {
                    erlaubt -> {
                        Text("Mitteilungen sind erlaubt.", style = Schrift.MonoKlein, color = Farben.GruenHell)
                        SehrLeise(
                            "Dieses Gerät ist angemeldet: Der Melder meldet sich im Dienst sofort, " +
                                "Einladungen und Nachrichten der Verwaltung holt die App alle Viertelstunde ab.",
                        )
                    }

                    abgelehnt || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {
                        Warnzeile(
                            "Mitteilungen sind für dieses Gerät abgeschaltet — von den Schaltern unten kommt " +
                                "gerade nichts an.",
                        )
                        Leise(
                            "Android fragt kein zweites Mal — erlaube die Mitteilungen von PagerSpass in den " +
                                "Einstellungen des Geräts.",
                        )
                        Knopf("Einstellungen öffnen", { einstellungenOeffnen() }, kompakt = true)
                    }

                    else -> {
                        Leise(
                            "Dieses Gerät hat noch keine Erlaubnis, Mitteilungen zu zeigen — von den " +
                                "Schaltern unten kommt bis dahin nichts an.",
                        )
                        Knopf(
                            "Mitteilungen erlauben",
                            { frage.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            art = Knopfart.Haupt,
                            kompakt = true,
                        )
                        Textweg("Oder in den Einstellungen des Geräts", { einstellungenOeffnen() })
                    }
                }
            }
        }

        if (laedt) {
            Ladezeile()
            return@Seite
        }

        Abschnitt("Was ankommen soll") {
            Kasten(abstandInnen = Abstand.Klein) {
                // Der Alarm steht mit auf der Liste: Eine Seite, die verspricht zu
                // zeigen, was ankommt, und die lauteste verschweigt, ist falsch.
                Schalterzeile(
                    "Alarm im Dienst",
                    unterzeile = "Der Melder geht, während die App im Hintergrund liegt oder das Handy " +
                        "gesperrt ist.",
                    an = e.alarm,
                    beiWechsel = { aendern(e.copy(alarm = it)) },
                )
                Schalterzeile(
                    "Tägliche Erinnerung",
                    unterzeile = "Einmal täglich um 12:00 Uhr, wenn du gerade keinen Dienst hast.",
                    an = e.tageserinnerung,
                    beiWechsel = { aendern(e.copy(tageserinnerung = it)) },
                )
                Schalterzeile(
                    "Spieleinladungen",
                    unterzeile = "Jemand lädt dich in eine Runde ein.",
                    an = e.einladungen,
                    beiWechsel = { aendern(e.copy(einladungen = it)) },
                )
                Schalterzeile(
                    "Nachrichten im Wachenchat",
                    unterzeile = "Neue Zeile im Chat deiner Gemeinschaft — und was die Wache sonst " +
                        "betrifft, etwa ein geplanter Dienst.",
                    an = e.chat,
                    beiWechsel = { aendern(e.copy(chat = it)) },
                )
                Schalterzeile(
                    "Private Nachrichten",
                    unterzeile = "Direktnachricht von einem Freund.",
                    an = e.privatnachrichten,
                    beiWechsel = { aendern(e.copy(privatnachrichten = it)) },
                )
                Schalterzeile(
                    "Kommentare am Brett",
                    unterzeile = "Jemand kommentiert einen deiner Einträge. Quittungen lösen bewusst nichts " +
                        "aus — sie sind der häufigste Vorgang am Brett.",
                    an = e.brett,
                    beiWechsel = { aendern(e.copy(brett = it)) },
                )
            }
        }
    }
}

// ------------------------------------------------------ Vom Betrieb

/**
 * Mitteilungen vom Betrieb — Wartungsfenster, Neuigkeiten, Hinweise.
 *
 * <b>Hieß bis hierher „Postfach"</b>, und das war doppelt belegt: Im Web ist das
 * Postfach die E-Mail-Adresse des Kontos (sie steht jetzt in der Kontozentrale).
 * Was hier steht, sind die Betreibermitteilungen — dieselben wie auf dem Start,
 * mit allen, die dort weggeklickt wurden.
 */
@Composable
fun BetriebsmitteilungenSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    mitteilungen: Bereichsstand<List<Betreibermitteilung>> = Bereichsstand(),
    beiLaden: () -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }
    val browser = LocalUriHandler.current

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Mitteilungen vom Betrieb",
            unterzeile = "Wartung, Neuigkeiten und Hinweise",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = mitteilungen.laedt,
            fehler = mitteilungen.fehler,
            inhalt = mitteilungen.inhalt,
            beiErneut = beiLaden,
        ) { liste ->
            if (liste.isEmpty()) {
                Leerhinweis("Zurzeit gibt es nichts zu vermelden.")
            } else {
                liste.forEach { m ->
                    Kasten(abstandInnen = Abstand.Klein) {
                        Text(m.titel, style = Schrift.Gross, color = Farben.Text)
                        Text(m.text, style = Schrift.Normal, color = Farben.TextLeise)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            SehrLeise(tag(m.ab), mono = true)
                            if (m.link != null) {
                                Textweg(m.linkText ?: "Öffnen", { browser.openUri(m.link) })
                            }
                        }
                    }
                }
            }
        }
    }
}
