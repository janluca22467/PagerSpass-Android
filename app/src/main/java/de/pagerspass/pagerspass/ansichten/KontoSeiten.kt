package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.rememberAblage
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Kontowege
import de.pagerspass.pagerspass.netz.Postfach
import de.pagerspass.pagerspass.netz.PremiumStand
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.netz.Werbung
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Codefeld
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.schmuck.Profilbanner
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.zeichen.Zeichen
import kotlinx.coroutines.launch

/**
 * Die Kontozentrale — das Gegenstück zu `KontoView.vue`.
 *
 * <b>Jedes Formular klappt einzeln auf.</b> Sie gleichzeitig offen stehen zu
 * lassen wäre ein Bogen, den niemand ausfüllt; eines nach dem anderen ist eine
 * Entscheidung. Die E-Mail-Karte hat ihre eigenen vier Formulare und deshalb
 * einen eigenen Merker — ein geöffnetes Codefeld klappt die Passwortänderung
 * nicht zu.
 *
 * <b>Die Karten laden unabhängig voneinander.</b> Premium, Postfach, Werbung und
 * Spielweise haben je ihren eigenen Weg; ein Fehlschlag bei einem nimmt die
 * anderen nicht mit.
 *
 * <b>Die Rückkehr von der Bezahlseite</b> kommt über den App-Link
 * (`/konto?premium=erfolg&session_id=…`, siehe `mobil/Einsprung.kt`) als
 * [premiumRueckkehr] und [sessionId] herein.
 */
@Composable
fun KontoZentrale(
    wege: Kontowege,
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    profil: Bereichsstand<Profil?> = Bereichsstand(),
    server: String = "",
    laeuft: Boolean = false,
    fehler: String? = null,
    premiumRueckkehr: String? = null,
    sessionId: String? = null,
    beiRueckkehrErledigt: () -> Unit = {},
    beiProfilLaden: () -> Unit = {},
    beiKontoNeu: (Konto) -> Unit = {},
    beiKontoAendern: ((Konto) -> Konto) -> Unit = {},
    beiPasswortAendern: suspend (String, String) -> Unit = { _, _ -> },
    beiAbmelden: () -> Unit = {},
    beiLoeschen: (String) -> Unit = {},
    beiProfil: () -> Unit = {},
    beiWeg: (String) -> Unit = {},
) {
    LaunchedEffect(Unit) { beiProfilLaden() }
    val p = profil.inhalt

    // Welches Formular gerade offensteht; null heißt: nur die Knöpfe.
    var offen by rememberSaveable { mutableStateOf<String?>(null) }
    fun umschalten(welches: String) {
        offen = if (offen == welches) null else welches
    }

    Seite(modifier = modifier, unterrand = unterrand) {
        if (konto == null) {
            Leise("Für dieses Gerät ist kein Konto angemeldet.")
            return@Seite
        }

        // Der Kopf ist der eine Ort, an dem Person, Stand und wichtigste Handlung
        // zusammenkommen. Er trägt denselben Schmuck wie das öffentliche Profil.
        Profilbanner(
            kennung = konto.kennung,
            anzeigename = konto.anzeigename,
            benutzername = konto.benutzername,
            augenbraue = "Dein Konto",
            wappen = p?.wappen ?: "Keines",
            wappenfarbe = p?.wappenfarbe ?: 0,
            kopfmuster = p?.kopfmuster ?: "keines",
            profilrahmen = p?.profilrahmen ?: "keiner",
            bildAdresse = bildweg(server, p?.profilbild),
            premium = konto.premiumAktiv,
            teammitglied = konto.teammitglied,
            // Rang und Stufe sind zwei Marken und nicht eine: Zusammen brachen sie am
            // Handy auf zwei Zeilen um, und die Stufe ist eine Zahl, kein Titel.
            marken = listOfNotNull(
                konto.wachentag?.takeIf { it.isNotBlank() }?.let { "[$it]" },
                konto.rang.takeIf { it.isNotBlank() },
                "Stufe ${konto.level}",
                if (konto.premiumAktiv) "Premium" else "Free",
            ),
            werte = listOf(
                "Credits" to zahl(konto.credits),
                "Dabei seit" to tag(konto.erstelltUm),
                "Profil" to if (p != null) "Bereit" else "Wird geladen",
            ),
            knoepfe = {
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        if (p != null) "Profil gestalten" else "Profil wird geladen …",
                        beiProfil,
                        art = Knopfart.Haupt,
                        kompakt = true,
                        aktiv = p != null,
                    )
                    if (p != null) {
                        Knopf(
                            "Öffentliches Profil",
                            { beiWeg("freunde/profil/${p.benutzername}") },
                            art = Knopfart.Leise,
                            kompakt = true,
                        )
                    }
                }
            },
        )

        Schnellwege(beiWeg)

        Spielweisekarte(wege, konto)

        Premiumkarte(
            wege = wege,
            konto = konto,
            rueckkehr = premiumRueckkehr,
            sessionId = sessionId,
            beiRueckkehrErledigt = beiRueckkehrErledigt,
            beiKontoAendern = beiKontoAendern,
            beiWeg = beiWeg,
        )

        Sicherheitskarte(
            offen = offen == "passwort",
            beiUmschalten = { umschalten("passwort") },
            beiPasswortAendern = beiPasswortAendern,
            beiFertig = { offen = null },
            beiAbmelden = beiAbmelden,
        )

        Emailkarte(wege, konto)

        Bedienungskarte()

        Abschnitt("Wachengemeinschaft") {
            Kasten(abstandInnen = Abstand.Klein) {
                Leise("Deine feste Mannschaft mit eigenem Chat und gemeinsamen Runden.")
                Knopf("Gemeinschaft öffnen", { beiWeg("gemeinschaften") }, kompakt = true)
            }
        }

        Werbekarte(wege, konto)

        Fehlerkarte(
            wege = wege,
            konto = konto,
            offen = offen == "bug",
            beiUmschalten = { umschalten("bug") },
            beiFertig = { offen = null },
        )

        Abschnitt("Rechtliches") {
            SehrLeise("Alle verbindlichen Texte und Kontaktangaben.")
            Wegzeile("Nutzungsbedingungen", { beiWeg("recht/${Rechtsstand.NUTZUNGSBEDINGUNGEN}") }, zeichen = Zeichen.Wiki)
            Wegzeile("AGB", { beiWeg("recht/${Rechtsstand.AGB}") }, zeichen = Zeichen.Wiki)
            Wegzeile("Datenschutzerklärung", { beiWeg("recht/${Rechtsstand.DATENSCHUTZ}") }, zeichen = Zeichen.Wiki)
            Wegzeile("Impressum", { beiWeg("recht/${Rechtsstand.IMPRESSUM}") }, zeichen = Zeichen.Wiki)
            Wegzeile("Verträge hier kündigen", { beiWeg("vertrag/kuendigen") }, zeichen = Zeichen.Wiki)
            Wegzeile("Vertrag widerrufen", { beiWeg("vertrag/widerrufen") }, zeichen = Zeichen.Wiki)
        }

        Weltkarte(
            wege = wege,
            konto = konto,
            offen = offen == "weltreset",
            beiUmschalten = { umschalten("weltreset") },
            beiFertig = { offen = null },
        )

        Loeschkarte(
            offen = offen == "loeschen",
            laeuft = laeuft,
            fehler = if (offen == "loeschen") fehler else null,
            beiUmschalten = { umschalten("loeschen") },
            beiLoeschen = beiLoeschen,
        )
    }
}

// ------------------------------------------------------------- Schnellwege

/**
 * Die Schnellzugriffe unter dem Kopf. Dazu, was es nur in der App gibt: der
 * mobile Begleiter und die Mitteilungen vom Betrieb (die hießen bis hierher
 * „Postfach" — das Postfach im Web ist aber die E-Mail-Adresse weiter unten).
 */
@Composable
private fun Schnellwege(beiWeg: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Wegzeile(
            "Mobiler Begleiter",
            { beiWeg("begleiter") },
            unterzeile = "QR-Code scannen: Funkgerät und Melder aufs Handy",
            zeichen = Zeichen.Funk,
        )
        Wegzeile("Privatsphäre", { beiWeg("privatsphaere") }, unterzeile = "Sichtbarkeit und Einladungen", zeichen = Zeichen.Handy)
        Wegzeile("Mitteilungen", { beiWeg("mitteilungen") }, unterzeile = "Alarm und Nachrichten", zeichen = Zeichen.Melder)
        Wegzeile("Discord", { beiWeg("discord") }, unterzeile = "Rolle und Verknüpfung", zeichen = Zeichen.Kanal)
        Wegzeile(
            "Gemeinschaft",
            { beiWeg("gemeinschaften") },
            unterzeile = "Mannschaft und Wachenchat",
            zeichen = Zeichen.Gemeinschaft,
        )
        Wegzeile("Shop", { beiWeg("shop") }, unterzeile = "Credits und Freischaltungen", zeichen = Zeichen.WegShop)
        Wegzeile(
            "Mitteilungen vom Betrieb",
            { beiWeg("postfach") },
            unterzeile = "Wartung, Neuigkeiten und Hinweise",
            zeichen = Zeichen.Forum,
        )
    }
}

// --------------------------------------------------------------- Spielweise

/**
 * Die Spielweise: die Patientensimulation an oder aus.
 *
 * <b>Gelesen und geschrieben beim Server.</b> Ein Wert auf dem Gerät stünde auf
 * dem zweiten Gerät anders — und die Simulation entscheidet darüber, was in der
 * Runde passiert.
 */
@Composable
private fun Spielweisekarte(wege: Kontowege, konto: Konto) {
    val bereich = rememberCoroutineScope()
    var an by remember { mutableStateOf(false) }
    var laeuft by remember { mutableStateOf(false) }

    LaunchedEffect(konto.kennung) {
        // Lässt sie sich nicht lesen, bleibt der Schalter aus — der Zustand eines
        // Kontos ohne Eintrag.
        runCatching { wege.patientensimulation(konto.kennung) }.onSuccess { an = it.an }
    }

    Abschnitt("Spielweise") {
        Kasten(abstandInnen = Abstand.Klein) {
            SehrLeise("Wie ausführlich du am Patienten arbeitest.")
            Schalterzeile(
                titel = "Patientensimulation",
                unterzeile = "Vitalwerte messen (der Blutdruck dauert 20 Sekunden), xABCDE, SAMPLER " +
                    "und OPQRST abarbeiten, Verdachtsdiagnose festhalten, behandeln. Aus heißt: " +
                    "genau wie bisher.",
                an = an,
                aktiv = !laeuft,
                beiWechsel = { neu ->
                    an = neu
                    laeuft = true
                    bereich.launch {
                        // Zurückstellen, wenn es scheitert: Sonst behauptet der Schalter
                        // einen Zustand, den der Server nie bekommen hat.
                        an = runCatching { wege.patientensimulationSetzen(konto.kennung, neu).an }
                            .getOrDefault(!neu)
                        laeuft = false
                    }
                },
            )
            SehrLeise(
                "Sie bringt keinen Vorteil — weder mehr Punkte noch schnelleren Aufstieg. Sie ist " +
                    "eine Art zu spielen und kein Weg nach oben.",
            )
        }
    }
}

// ------------------------------------------------------------------ Premium

/**
 * Dein Plan und die Abo-Verwaltung.
 *
 * <b>Drei Rückwege von der Bezahlseite</b>, und jeder bekommt eine Antwort:
 * `erfolg` schließt die Sitzung ab, `abbruch` sagt, dass nichts abgebucht wurde,
 * `portal` holt nur den Stand neu.
 */
@Composable
private fun Premiumkarte(
    wege: Kontowege,
    konto: Konto,
    rueckkehr: String?,
    sessionId: String?,
    beiRueckkehrErledigt: () -> Unit,
    beiKontoAendern: ((Konto) -> Konto) -> Unit,
    beiWeg: (String) -> Unit,
) {
    val zusammenhang = LocalContext.current
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()
    var stand by remember { mutableStateOf<PremiumStand?>(null) }

    fun uebernehmen(neu: PremiumStand) {
        stand = neu
        beiKontoAendern {
            it.copy(
                premiumStatus = neu.status,
                premiumPlan = neu.plan,
                premiumAktiv = neu.aktiv,
                premiumBis = neu.bis,
            )
        }
    }

    LaunchedEffect(konto.kennung, rueckkehr, sessionId) {
        when (rueckkehr) {
            "abbruch" -> {
                v.fehler = "Der Kauf wurde abgebrochen — es wurde nichts abgebucht."
                beiRueckkehrErledigt()
            }
            "portal" -> beiRueckkehrErledigt()
            "erfolg" -> if (!sessionId.isNullOrBlank()) {
                v.laeuft = true
                runCatching { wege.premiumAbschliessen(konto.kennung, sessionId) }
                    .onSuccess {
                        uebernehmen(it)
                        v.erfolg = "Premium wurde aktiviert."
                    }
                    .onFailure { v.fehler = it.message ?: "Premium konnte nach dem Kauf nicht aktiviert werden." }
                v.laeuft = false
                beiRueckkehrErledigt()
            }
        }

        runCatching { wege.premium(konto.kennung) }
            .onSuccess { uebernehmen(it) }
            .onFailure { if (v.fehler == null) v.fehler = it.message ?: "Premium ließ sich nicht laden." }
    }

    val s = stand
    val hinweis = when {
        s == null -> "Status wird geladen"
        s.aktiv -> if (s.plan == "Yearly") "Premium aktiv · Jahr" else "Premium aktiv · Monat"
        s.status == "PastDue" -> "Zahlung offen"
        else -> "Free"
    }
    // Ein Datum sagt nur etwas, solange das Abo läuft: `bis` bleibt auch nach dem
    // Ende stehen, und „Free · läuft bis …" mit einem vergangenen Tag ist falsch.
    val unter = when {
        s == null -> "Der Zustand deines Abos wird geladen."
        s.aktiv -> s.bis?.let { "Der bezahlte Zeitraum läuft bis zum ${tag(it)}." }
            ?: "Läuft, bis du in der Abo-Verwaltung kündigst."
        s.status == "PastDue" -> "Bring die Zahlung in Ordnung, sonst endet Premium."
        s.status == "Canceled" -> if (s.kaufVerfuegbar) {
            "Dein Abo ist beendet. Im Shop kannst du es neu abschließen."
        } else {
            "Dein Abo ist beendet. Neue Abos sind derzeit nicht verfügbar."
        }
        s.kaufVerfuegbar -> "Premium kaufst du im Shop; hier verwaltest du dein Abo."
        else -> "Neue Premium-Abos sind derzeit nicht verfügbar."
    }

    Abschnitt("Premium") {
        Kasten(marke = s?.aktiv == true, abstandInnen = Abstand.Klein) {
            SehrLeise("Dein Plan und die Abo-Verwaltung.")
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(if (s?.aktiv == true) Farben.Gruen else Farben.RandHell, CircleShape),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                    Text(hinweis, style = Schrift.Normal.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    Leise(unter)
                }
            }

            if (s != null && s.kaufVerfuegbar && !s.aktiv && s.status != "PastDue") {
                Knopf("Premium ansehen", { beiWeg("shop") }, kompakt = true)
            }
            if (s != null && (s.aktiv || s.status == "PastDue")) {
                Knopf(
                    if (v.laeuft) "Weiter zu Stripe …" else "Abo verwalten",
                    {
                        bereich.vorgang(v, "Abo-Verwaltung ließ sich nicht öffnen.") {
                            val ziel = wege.premiumPortal(konto.kennung)
                            imBrowser(zusammenhang, ziel.url)
                        }
                    },
                    kompakt = true,
                    aktiv = !v.laeuft,
                )
            }
            Vorgangszeilen(v)

            // Die beiden gesetzlichen Wege (§ 312k, § 356a BGB) — immer, nicht nur bei
            // laufendem Abo: Wer gerade gekündigt hat und widerrufen will, muss sie
            // genauso finden.
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
                Textweg("Verträge hier kündigen", { beiWeg("vertrag/kuendigen") })
                Textweg("Vertrag widerrufen", { beiWeg("vertrag/widerrufen") })
            }
        }
    }
}

// --------------------------------------------------------------- Sicherheit

@Composable
private fun Sicherheitskarte(
    offen: Boolean,
    beiUmschalten: () -> Unit,
    beiPasswortAendern: suspend (String, String) -> Unit,
    beiFertig: () -> Unit,
    beiAbmelden: () -> Unit,
) {
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()
    var aktuell by remember { mutableStateOf("") }
    var neu by remember { mutableStateOf("") }
    var wiederholt by remember { mutableStateOf("") }

    Abschnitt("Sicherheit") {
        Kasten(abstandInnen = Abstand.Klein) {
            SehrLeise("Passwort und Sitzung auf diesem Gerät.")
            Tatzeile("Passwort", "••••••••", mono = true) {
                Knopf("Ändern", beiUmschalten, kompakt = true)
            }

            if (offen) {
                Feld(aktuell, { aktuell = it }, etikett = "Aktuelles Passwort", geheim = true)
                Feld(neu, { neu = it }, etikett = "Neues Passwort", geheim = true)
                Feld(
                    wiederholt,
                    { wiederholt = it },
                    etikett = "Neues Passwort wiederholen",
                    geheim = true,
                    weiterTaste = ImeAction.Done,
                )
                SehrLeise("Mindestens 8 Zeichen.")
                v.fehler?.let { Warnzeile(it) }
                Knopf(
                    "Passwort speichern",
                    {
                        when {
                            neu != wiederholt -> v.fehler = "Die neuen Passwörter stimmen nicht überein."
                            neu.length < 8 -> v.fehler = "Das neue Passwort braucht mindestens 8 Zeichen."
                            aktuell.isEmpty() -> v.fehler = "Bitte gib dein aktuelles Passwort an."
                            else -> bereich.vorgang(v, "Das Passwort ließ sich nicht ändern.") {
                                beiPasswortAendern(aktuell, neu)
                                aktuell = ""
                                neu = ""
                                wiederholt = ""
                                beiFertig()
                                v.erfolg = "Dein Passwort ist geändert."
                            }
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = !v.laeuft,
                )
            } else {
                v.erfolg?.let { Erfolgszeile(it) }
            }

            Tatzeile("Dieses Gerät", "Nur diese Sitzung wird beendet.") {
                Knopf("Abmelden", beiAbmelden, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
}

/** Eine Zeile „Was — Stand — Knopf": `.tatzeile` im Web. */
@Composable
private fun Tatzeile(
    titel: String,
    unter: String?,
    mono: Boolean = false,
    unterFarbe: androidx.compose.ui.graphics.Color? = null,
    zweiteZeile: String? = null,
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
            if (unter != null) {
                Text(
                    unter,
                    style = if (mono) Schrift.MonoKlein else Schrift.Klein,
                    color = Farben.TextSehrLeise,
                )
            }
            if (zweiteZeile != null) {
                Text(zweiteZeile, style = Schrift.Klein, color = unterFarbe ?: Farben.TextSehrLeise)
            }
        }
        knopf()
    }
}

// ------------------------------------------------------------------ E-Mail

/**
 * Die E-Mail-Adresse — seit 5.0.0.58 Pflicht, und die Karte ist auf das
 * geschrumpft, was man dafür braucht: die Adresse, ihre Bestätigung und den
 * Newsletter. Der Anmeldeschutz lässt sich nicht mehr einschalten; wer ihn noch
 * trägt, sieht den Weg hinaus.
 */
@Composable
private fun Emailkarte(wege: Kontowege, konto: Konto) {
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()
    var postfach by remember { mutableStateOf<Postfach?>(null) }
    var offen by rememberSaveable { mutableStateOf<String?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
    var passwort by remember { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var schutzPasswort by remember { mutableStateOf("") }

    LaunchedEffect(konto.kennung) {
        // Scheitert das, bleibt die Karte bei „keine Adresse" — ein Versuch, eine
        // einzutragen, sagt dann selbst, was schiefging.
        runCatching { wege.postfach(konto.kennung) }.onSuccess { postfach = it }
    }

    fun umschalten(welches: String) {
        offen = if (offen == welches) null else welches
        v.leeren()
        if (offen == "email") email = postfach?.email.orEmpty()
    }

    val pf = postfach

    Abschnitt("E-Mail-Adresse") {
        Kasten(abstandInnen = Abstand.Klein) {
            SehrLeise("Für ein vergessenes Passwort und — wenn du ihn bestellst — den Newsletter.")

            if (pf != null && !pf.versandbereit) {
                Warnzeile(
                    "Auf diesem Server ist gerade kein Postausgang eingerichtet — Codes werden nicht " +
                        "verschickt.",
                )
            }

            Tatzeile(
                titel = "Adresse",
                unter = pf?.email ?: "Keine hinterlegt",
                mono = true,
                zweiteZeile = pf?.email?.let { if (pf.bestaetigt) "Bestätigt" else "Noch nicht bestätigt" },
                unterFarbe = if (pf?.bestaetigt == true) Farben.GruenHell else Farben.SignalHell,
            ) {
                Knopf(if (pf?.email != null) "Ändern" else "Hinzufügen", { umschalten("email") }, kompakt = true)
            }

            if (offen == "email") {
                Feld(
                    email,
                    { email = it.take(254) },
                    etikett = "E-Mail-Adresse",
                    tastatur = KeyboardType.Email,
                )
                Feld(
                    passwort,
                    { passwort = it },
                    etikett = "Passwort zur Bestätigung",
                    geheim = true,
                    weiterTaste = ImeAction.Done,
                )
                SehrLeise(
                    "Wir schicken dir einen sechsstelligen Code. Bis er eingegeben ist, wird die " +
                        "Adresse für nichts verwendet.",
                )
                Knopf(
                    "Adresse speichern",
                    {
                        bereich.vorgang(v) {
                            val antwort = wege.emailSetzen(konto.kennung, email.trim(), passwort)
                            postfach = antwort.postfach
                            passwort = ""
                            // Direkt weiter zum Code — er ist gerade rausgegangen.
                            offen = "bestaetigen"
                            v.erfolg = antwort.hinweis ?: "Der Bestätigungscode ist unterwegs."
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = !v.laeuft,
                )
            }

            if (pf?.email != null && !pf.bestaetigt) {
                Tatzeile(
                    "Bestätigen",
                    "Der Code aus der E-Mail, sechs Ziffern. Erst bestätigt hilft die Adresse beim " +
                        "vergessenen Passwort.",
                ) {
                    Knopf("Code eingeben", { umschalten("bestaetigen") }, kompakt = true)
                }
                if (offen == "bestaetigen") {
                    Codefeld(code, { code = it.filter(Char::isDigit) }, etikett = "Code")
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Knopf(
                            "Bestätigen",
                            {
                                bereich.vorgang(v) {
                                    postfach = wege.emailBestaetigen(konto.kennung, code)
                                    code = ""
                                    offen = null
                                    v.erfolg = "Deine Adresse ist bestätigt."
                                }
                            },
                            art = Knopfart.Haupt,
                            kompakt = true,
                            aktiv = !v.laeuft,
                        )
                        Knopf(
                            "Nichts angekommen",
                            {
                                bereich.vorgang(v) {
                                    wege.emailCodeNeu(konto.kennung)
                                    v.erfolg = "Ein neuer Code ist unterwegs."
                                }
                            },
                            art = Knopfart.Leise,
                            kompakt = true,
                            aktiv = !v.laeuft,
                        )
                    }
                }
            }

            // Der Newsletter hängt an einer bestätigten Adresse (Double Opt-in).
            val bestellbar = pf?.bestaetigt == true || pf?.newsletter == true
            Tatzeile(
                "Newsletter",
                "Gelegentlich Neues zum Spiel. Abmelden jederzeit, auch über den Link in jeder " +
                    "Ausgabe." + if (!bestellbar) " Erst nach der Bestätigung deiner Adresse." else "",
            ) {
                Knopf(
                    if (pf?.newsletter == true) "Abbestellen" else "Bestellen",
                    {
                        val an = pf?.newsletter != true
                        bereich.vorgang(v) {
                            postfach = wege.newsletterSetzen(konto.kennung, an)
                            v.erfolg = if (an) {
                                "Du bekommst den Newsletter. Abmelden geht jederzeit — auch über den Link " +
                                    "in jeder Ausgabe."
                            } else {
                                "Du bekommst keinen Newsletter mehr."
                            }
                        }
                    },
                    kompakt = true,
                    aktiv = !v.laeuft && bestellbar,
                )
            }

            if (pf?.zweiFaktorAktiv == true) {
                Tatzeile(
                    "Anmeldeschutz",
                    "Bei dir noch eingeschaltet: Die Anmeldung verlangt einen Code aus deinem Postfach.",
                ) {
                    Knopf("Ausschalten", { umschalten("schutzAus") }, kompakt = true)
                }
                if (offen == "schutzAus") {
                    Feld(
                        schutzPasswort,
                        { schutzPasswort = it },
                        etikett = "Passwort zur Bestätigung",
                        geheim = true,
                        weiterTaste = ImeAction.Done,
                    )
                    Knopf(
                        "Anmeldeschutz ausschalten",
                        {
                            bereich.vorgang(v) {
                                postfach = wege.zweiFaktorSetzen(konto.kennung, false, schutzPasswort)
                                schutzPasswort = ""
                                offen = null
                                v.erfolg = "Der Anmeldeschutz ist aus."
                            }
                        },
                        art = Knopfart.Gefahr,
                        aktiv = !v.laeuft,
                    )
                }
            }

            Vorgangszeilen(v)
        }
    }
}

// ----------------------------------------------------------------- Bedienung

/**
 * Deine Bedienung — zwei Einstellungen, die nur auf diesem Gerät gelten.
 *
 * Gelesen werden sie im Dienst (siehe `mobil/Bedienung.kt`); hier stehen sie bei
 * den übrigen Einstellungen, wo man sie sucht.
 */
@Composable
private fun Bedienungskarte() {
    val ablage = rememberAblage()
    val bereich = rememberCoroutineScope()
    val weg by remember(ablage) { ablage.eingabewegFluss() }
        .collectAsState(initial = Ablage.EINGABEWEGE.first())
    val form by remember(ablage) { ablage.kennungsformFluss() }
        .collectAsState(initial = Ablage.KENNUNGSFORMEN.first())

    Abschnitt("Deine Bedienung") {
        SehrLeise("Diese beiden Einstellungen gelten nur auf diesem Gerät.")

        Kasten(abstandInnen = Abstand.Klein) {
            Text("Notrufabfrage", style = Schrift.Gross, color = Farben.Text)
            Leise("Wie du Rückfragen am Telefon stellst.")
            Etikett("Eingabeweg")
            Pillenreihe {
                EINGABEWEGE.forEach { (wert, name) ->
                    Pille(name, an = weg == wert, beiDruck = { bereich.launch { ablage.eingabewegSetzen(wert) } })
                }
            }
            SehrLeise(WEG_ERKLAERUNG[weg].orEmpty())
        }

        Kasten(abstandInnen = Abstand.Klein) {
            Text("Fahrzeugkennung", style = Schrift.Gross, color = Farben.Text)
            Leise("Wie Fahrzeuge in deinen Listen heißen.")
            Etikett("Darstellung")
            Pillenreihe {
                KENNUNGSFORMEN.forEach { (wert, name) ->
                    Pille(name, an = form == wert, beiDruck = { bereich.launch { ablage.kennungsformSetzen(wert) } })
                }
            }
            SehrLeise(KENNUNG_ERKLAERUNG[form].orEmpty())
        }
    }
}

private val EINGABEWEGE = listOf(
    "fragen" to "Fragen anklicken",
    "tippen" to "Selbst tippen",
    "sprechen" to "Sprechen",
)

private val WEG_ERKLAERUNG = mapOf(
    "fragen" to "Sechs Knöpfe nach dem klassischen Abfrageschema. Schnell und ohne Tastatur.",
    "tippen" to "Frage frei eintippen. Gedeutet wird sie auf dem Server.",
    "sprechen" to "Frage ins Mikrofon sprechen, solange die Taste gedrückt ist. Gedeutet wird sie " +
        "auf dem Server.",
)

private val KENNUNGSFORMEN = listOf(
    "kennzahl" to "Kennzahl — 1/44/1",
    "typ" to "Fahrzeugtyp — 1/HLF 20-1",
    "orga" to "Träger und Kreis — RK-Celle-3/91/1",
)

private val KENNUNG_ERKLAERUNG = mapOf(
    "kennzahl" to "Die Kurzform des Funkrufnamens, wie sie am Mikrofon gesagt wird: Wache, Kennzahl, " +
        "laufende Nummer.",
    "typ" to "An der Stelle der Kennzahl steht der Fahrzeugtyp — Wache und laufende Nummer bleiben, du " +
        "siehst also weiter, welches Fahrzeug welcher Wache gemeint ist. Gerufen wird im Funk trotzdem " +
        "der volle Funkrufname.",
    "orga" to "Vor der Kurzform stehen der Träger und der Landkreis: „RK-Celle-3/91/1\" für das Rote " +
        "Kreuz, „RD-Celle-1/83/1\" für den kommunalen Rettungsdienst. Nützlich, wo mehrere Träger " +
        "nebeneinander fahren. In einer Runde ohne echten Landkreis fällt der Kreisteil weg.",
)

// ------------------------------------------------------------------- Werbung

/**
 * Freunde werben — den eigenen Code weitergeben oder als neues Konto einen
 * fremden einlösen. Warum gerade kein Einlösefeld dasteht, sagt ein Satz: Ein
 * Feld, das ohne ein Wort verschwindet, ist keine Regel, sondern eine Panne.
 */
@Composable
private fun Werbekarte(wege: Kontowege, konto: Konto) {
    val zusammenhang = LocalContext.current
    val zwischenablage = LocalClipboardManager.current
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()
    var werbung by remember { mutableStateOf<Werbung?>(null) }
    var eingabe by rememberSaveable { mutableStateOf("") }
    var kopiert by remember { mutableStateOf(false) }

    LaunchedEffect(konto.kennung) {
        runCatching { wege.werbung(konto.kennung) }.onSuccess { werbung = it }
    }

    Abschnitt("Freunde werben") {
        SehrLeise("Code weitergeben oder als neues Konto einen fremden Code einlösen.")

        val w = werbung
        if (w != null && w.aktionTitel != null) {
            Kasten(marke = true, abstandInnen = Abstand.Klein) {
                Text(
                    text = hervorgehoben(
                        "**${w.aktionTitel}** — jede Werbung zählt gerade ${w.aktionFaktor}-fach: " +
                            "${w.creditsJetzt} statt ${w.creditsJeWerbung} Credits.",
                    ),
                    style = Schrift.Normal,
                    color = Farben.TextLeise,
                )
                w.aktionBis?.let { Leise("Noch bis ${zeitpunkt(it)}.") }
            }
        }

        Kasten(abstandInnen = Abstand.Klein) {
            if (w == null) {
                SehrLeise("Der Werbestand wird geholt …")
                return@Kasten
            }

            Text("01", style = Schrift.MonoKlein, color = Farben.Amber)
            Text("Dein Code zum Weitergeben", style = Schrift.Gross, color = Farben.Text)
            Leise(
                "Dein Freund erhält einen Fahrzeug-Gutschein; du bekommst ${w.creditsJetzt} Credits " +
                    "nach seinem ersten Aufstieg.",
            )
            val eigener = w.code
            if (eigener != null) {
                Text(
                    text = eigener,
                    style = Schrift.MonoNormal.copy(fontSize = Schrift.TITEL, letterSpacing = 0.12.em),
                    color = Farben.Amber,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Teilen", { textTeilen(zusammenhang, eigener, "Mein PagerSpass-Werbecode") }, kompakt = true)
                    Knopf(
                        if (kopiert) "Kopiert" else "Kopieren",
                        {
                            zwischenablage.setText(AnnotatedString(eigener))
                            kopiert = true
                        },
                        art = Knopfart.Leise,
                        kompakt = true,
                    )
                }
                SehrLeise(
                    "${w.geworben} geworben · ${w.ausgezahlt} mit Aufstieg · ${w.verdienteCredits} " +
                        "Credits verdient" +
                        if (w.ausgezahlt >= w.deckel) " — Deckel erreicht." else " · höchstens ${w.deckel} Werbungen",
                )
            } else {
                if (w.darfEinloesen) {
                    Leise(
                        "Mit deinem eigenen Code wirst du zum Werber; danach kannst du keinen fremden " +
                            "mehr einlösen.",
                    )
                }
                Knopf(
                    "Werbecode erstellen",
                    {
                        bereich.vorgang(v, "Der Code ließ sich nicht erstellen.") {
                            werbung = wege.werbecodeErzeugen(konto.kennung)
                        }
                    },
                    kompakt = true,
                    aktiv = !v.laeuft,
                )
            }
        }

        if (w != null) {
            Kasten(abstandInnen = Abstand.Klein) {
                Text("02", style = Schrift.MonoKlein, color = Farben.Amber)
                Text("Code eines Freundes", style = Schrift.Gross, color = Farben.Text)
                when {
                    w.geworbenVon != null -> Text(
                        text = hervorgehoben(
                            "Du wurdest von **${w.geworbenVon}** geworben. Dein Gutschein wartet in der Garage.",
                        ),
                        style = Schrift.Klein,
                        color = Farben.TextLeise,
                    )

                    w.darfEinloesen -> {
                        Feld(
                            eingabe,
                            { eingabe = it.uppercase().take(16) },
                            etikett = "Werbecode",
                            platzhalter = "z. B. H7KM-3QX4",
                            weiterTaste = ImeAction.Done,
                            stil = Schrift.MonoNormal,
                        )
                        SehrLeise("In den ersten ${w.fenstertage} Tagen nach der Anmeldung einlösbar.")
                        Knopf(
                            "Einlösen",
                            {
                                bereich.vorgang(v, "Der Code ging nicht durch.") {
                                    werbung = wege.werbecodeEinloesen(konto.kennung, eingabe.trim())
                                    eingabe = ""
                                }
                            },
                            kompakt = true,
                            aktiv = !v.laeuft && eingabe.isNotBlank(),
                        )
                    }

                    w.code != null -> Leise(
                        "Du hast deinen eigenen Werbecode erstellt — damit bist du der Werber. Einen " +
                            "fremden Code einlösen kann nur, wer selbst noch keinen ausgegeben hat.",
                    )

                    else -> Leise(
                        "Werbecodes gelten in den ersten ${w.fenstertage} Tagen nach der Anmeldung — dein " +
                            "Konto ist schon länger dabei.",
                    )
                }
                v.fehler?.let { Warnzeile(it) }
            }
        }
    }
}

// -------------------------------------------------------------- Fehler melden

@Composable
private fun Fehlerkarte(
    wege: Kontowege,
    konto: Konto,
    offen: Boolean,
    beiUmschalten: () -> Unit,
    beiFertig: () -> Unit,
) {
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()
    var titel by rememberSaveable { mutableStateOf("") }
    var beschreibung by rememberSaveable { mutableStateOf("") }
    var hergang by rememberSaveable { mutableStateOf("") }
    var seite by rememberSaveable { mutableStateOf("") }

    Abschnitt("Fehler melden") {
        Kasten(abstandInnen = Abstand.Klein) {
            SehrLeise("Beschreibe den Weg zum Problem direkt aus dem Spiel.")
            Knopf(
                "Bug melden",
                {
                    // Wer das Formular erneut öffnet, will die nächste Meldung schreiben —
                    // der Dank für die letzte hat seinen Dienst getan.
                    v.leeren()
                    beiUmschalten()
                },
                kompakt = true,
            )
            if (!offen) v.erfolg?.let { Erfolgszeile(it) }

            if (offen) {
                Feld(titel, { titel = it.take(120) }, etikett = "Titel", platzhalter = "Worum geht es, in einem Satz?")
                Feld(
                    beschreibung,
                    { beschreibung = it.take(4000) },
                    etikett = "Was ist passiert?",
                    platzhalter = "Was war zu sehen, was hat gefehlt?",
                    einzeilig = false,
                    weiterTaste = ImeAction.Default,
                )
                Feld(
                    hergang,
                    { hergang = it.take(4000) },
                    etikett = "Wie kam es dazu?",
                    platzhalter = "Schritt für Schritt bis zum Fehler.",
                    einzeilig = false,
                    weiterTaste = ImeAction.Default,
                )
                Feld(
                    seite,
                    { seite = it.take(200) },
                    etikett = "Wo im Spiel? (freiwillig)",
                    platzhalter = "z. B. Leitstelle oder Garage",
                    weiterTaste = ImeAction.Done,
                )
                v.fehler?.let { Warnzeile(it) }
                Knopf(
                    "Meldung abschicken",
                    {
                        if (titel.isBlank() || beschreibung.isBlank() || hergang.isBlank()) {
                            v.fehler = "Bitte Titel, Beschreibung und Hergang angeben."
                        } else {
                            bereich.vorgang(v, "Die Meldung ließ sich nicht abschicken.") {
                                wege.bugMelden(konto.kennung, titel, beschreibung, hergang, seite)
                                titel = ""
                                beschreibung = ""
                                hergang = ""
                                seite = ""
                                beiFertig()
                                v.erfolg = "Danke — deine Meldung ist angekommen."
                            }
                        }
                    },
                    art = Knopfart.Haupt,
                    aktiv = !v.laeuft,
                )
            }
        }
    }
}

// --------------------------------------------------------------------- Welt

/**
 * PagerSpass - World zurücksetzen. Nur, wenn es etwas zurückzusetzen gibt: mit
 * Premium und einer gebauten Leitstelle. Das Konto bleibt; danach steht man
 * wieder auf der Gründungsseite.
 */
@Composable
private fun Weltkarte(
    wege: Kontowege,
    konto: Konto,
    offen: Boolean,
    beiUmschalten: () -> Unit,
    beiFertig: () -> Unit,
) {
    val bereich = rememberCoroutineScope()
    val v = rememberVorgang()
    var hatLeitstelle by remember { mutableStateOf(false) }
    var passwort by remember { mutableStateOf("") }

    LaunchedEffect(konto.kennung, konto.premiumAktiv) {
        hatLeitstelle = konto.premiumAktiv && wege.weltHatLeitstelle(konto.kennung)
    }

    if (!hatLeitstelle) {
        v.erfolg?.let { Erfolgszeile(it) }
        return
    }

    Abschnitt("Gefahrenbereich") {
        Kasten(abstandInnen = Abstand.Klein) {
            Text("PagerSpass - World zurücksetzen", style = Schrift.Gross, color = Farben.Text)
            Leise(
                "Leitstelle, Wachen, Fahrzeuge, Guthaben und Buchungsblatt werden gelöscht. Dein Konto " +
                    "und alles andere bleiben. Danach wählst du einen neuen Standort.",
            )
            Knopf("Welt zurücksetzen", beiUmschalten, art = Knopfart.Alarm, kompakt = true)
            if (offen) {
                Feld(passwort, { passwort = it }, etikett = "Passwort zur Bestätigung", geheim = true, weiterTaste = ImeAction.Done)
                v.fehler?.let { Warnzeile(it) }
                Knopf(
                    "Welt dauerhaft zurücksetzen",
                    {
                        bereich.vorgang(v, "Das Zurücksetzen ging nicht.") {
                            wege.weltZuruecksetzen(konto.kennung, passwort)
                            passwort = ""
                            beiFertig()
                            v.erfolg = "Deine Welt ist zurückgesetzt. Du kannst neu gründen."
                            hatLeitstelle = false
                        }
                    },
                    art = Knopfart.Alarm,
                    aktiv = !v.laeuft && passwort.isNotEmpty(),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Löschen

@Composable
private fun Loeschkarte(
    offen: Boolean,
    laeuft: Boolean,
    fehler: String?,
    beiUmschalten: () -> Unit,
    beiLoeschen: (String) -> Unit,
) {
    var passwort by remember { mutableStateOf("") }

    Abschnitt("Gefahrenbereich") {
        Kasten(abstandInnen = Abstand.Klein) {
            Text("Konto dauerhaft löschen", style = Schrift.Gross, color = Farben.Text)
            Leise(
                "Garage, Erfahrung, Dienstbuch und Freundschaften werden gelöscht. Gemeinsame Schichten " +
                    "bleiben als Historie der Mannschaft bestehen. Ein laufendes Premium-Abo wird dabei " +
                    "sofort beendet.",
            )
            Knopf("Konto löschen", beiUmschalten, art = Knopfart.Alarm, kompakt = true)
            if (offen) {
                Feld(passwort, { passwort = it }, etikett = "Passwort zur Bestätigung", geheim = true, weiterTaste = ImeAction.Done)
                fehler?.let { Warnzeile(it) }
                Knopf(
                    "Konto dauerhaft löschen",
                    { beiLoeschen(passwort) },
                    art = Knopfart.Alarm,
                    aktiv = !laeuft && passwort.isNotEmpty(),
                )
            }
        }
    }
}
