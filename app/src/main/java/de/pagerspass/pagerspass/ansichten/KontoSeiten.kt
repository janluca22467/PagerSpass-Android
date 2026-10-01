package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Kontodienst
import de.pagerspass.pagerspass.mobil.Kontostand
import de.pagerspass.pagerspass.mobil.Rueckmeldung
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Postfach
import de.pagerspass.pagerspass.netz.Premiumstand
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.netz.Server
import de.pagerspass.pagerspass.netz.Werbung
import de.pagerspass.pagerspass.netz.plattformname
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Codefeld
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Karte
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.schmuck.Profilbanner
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.zeichen.Zeichen
import java.util.Locale

/**
 * Die Konto-Zentrale — das Gegenstück zu `KontoView.vue` in der Handy-Fassung.
 *
 * <b>Dieselbe Reihenfolge wie im Web:</b> der Kopf mit Person und Stand, die kurzen
 * Wege, dann die Karten Spielweise, Premium, Sicherheit und E-Mail-Adresse, die
 * Wachengemeinschaft, „Freunde werben", „Fehler melden", das Rechtliche und ganz
 * unten die beiden Gefahrenbereiche. Wer beide Fassungen kennt, soll nicht suchen.
 *
 * <b>Zwei Zeilen hat nur die App:</b> den mobilen Begleiter und das Postfach der
 * Verwaltung. Beide stehen oben, weil sie Wege auf diesem Gerät sind.
 *
 * <b>Geld nur auf der Webseite.</b> Die Premium-Karte zeigt den Stand und führt
 * zum Abschließen, Verwalten und Kündigen auf pagerspass.de — und sagt das auch.
 * Kommt die App danach aus dem Hintergrund zurück, holt sie Konto und Abo neu:
 * Genau dann kann sich dort etwas geändert haben.
 *
 * <b>„Konto löschen" gehört hierher und nirgendwo sonst.</b> Google Play verlangt
 * für jede App, in der man ein Konto anlegen kann, einen Löschweg in der App.
 */
@Composable
fun KontoSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    profil: Bereichsstand<Profil?> = Bereichsstand(),
    server: String = "",
    postfachFrei: Boolean = false,
    stand: Kontostand = Kontostand(),
    dienst: Kontodienst? = null,
    beiProfilLaden: () -> Unit = {},
    /** Konto neu holen — nach der Rückkehr von der Webseite. */
    beiKontoLaden: () -> Unit = {},
    beiAbmelden: () -> Unit = {},
    beiLoeschen: () -> Unit = {},
    beiProfil: () -> Unit = {},
    beiPrivatsphaere: () -> Unit = {},
    beiPostfach: () -> Unit = {},
    beiMitteilungen: () -> Unit = {},
    beiBegleiter: () -> Unit = {},
    beiDiscord: () -> Unit = {},
    beiGemeinschaft: () -> Unit = {},
    beiShop: () -> Unit = {},
    beiPremium: () -> Unit = {},
    /** „Dein Melder" — Bauform, Gesicht, Alarmton und die Werkstätten. */
    beiMelder: () -> Unit = {},
) {
    val browser = LocalUriHandler.current
    val p = profil.inhalt

    BeiRueckkehr {
        beiProfilLaden()
        beiKontoLaden()
        dienst?.premiumLaden()
        dienst?.postfachLaden()
        dienst?.werbungLaden()
        dienst?.simulationLaden()
    }

    // Erst mit dem Premiumstand: Ohne Abo antwortet die Welt mit 403, und die Zeile
    // zum Zurücksetzen bliebe ohnehin aus.
    val premiumAktiv = stand.premium.inhalt?.aktiv ?: (konto?.premiumAktiv == true)
    androidx.compose.runtime.LaunchedEffect(premiumAktiv) { dienst?.weltPruefen(premiumAktiv) }

    Seite(modifier = modifier, unterrand = unterrand) {
        Profilbanner(
            kennung = konto?.kennung.orEmpty(),
            anzeigename = konto?.anzeigename.orEmpty(),
            benutzername = konto?.benutzername,
            augenbraue = "Dein Konto",
            wappen = p?.wappen ?: "Keines",
            wappenfarbe = p?.wappenfarbe ?: 0,
            kopfmuster = p?.kopfmuster ?: "keines",
            profilrahmen = p?.profilrahmen ?: "keiner",
            bildAdresse = bildweg(server, p?.profilbild),
            premium = premiumAktiv,
            teammitglied = konto?.teammitglied == true,
            marken = listOfNotNull(
                konto?.rang,
                konto?.let { "Stufe ${it.level}" },
                if (premiumAktiv) "Premium" else "Free",
            ),
            werte = listOfNotNull(
                konto?.let { "Credits" to zahl(it.credits) },
                "Dabei seit" to tag(konto?.erstelltUm),
                "Profil" to if (p != null) "Bereit" else "Wird geladen",
            ),
            knoepfe = {
                Knopf(
                    if (p != null) "Profil gestalten" else "Profil wird geladen …",
                    beiProfil,
                    art = Knopfart.Haupt,
                    aktiv = p != null,
                    kompakt = true,
                )
            },
        )

        // Die Wege des Handys zuerst: Wer die Kontoseite mit einem QR-Code vor sich
        // öffnet, will in zwei Griffen funken — alles darunter kann warten.
        Abschnitt("Dieses Gerät") {
            Wegzeile(
                "Mobiler Begleiter",
                beiBegleiter,
                unterzeile = "QR-Code scannen: Funkgerät und Melder aufs Handy",
                zeichen = Zeichen.Funk,
            )
            Wegzeile(
                "Dein Melder",
                beiMelder,
                unterzeile = "Bauform, Gesicht und Alarmton für dieses Gerät",
                zeichen = Zeichen.Melder,
            )
            Wegzeile(
                "Postfach",
                beiPostfach,
                unterzeile = if (postfachFrei) {
                    "Nachrichten der Verwaltung"
                } else {
                    "Vom Betrieb — dein Postfach ist noch nicht freigeschaltet"
                },
                zeichen = Zeichen.Forum,
            )
        }

        Abschnitt("Schnellzugriff") {
            Wegzeile("Privatsphäre", beiPrivatsphaere, unterzeile = "Sichtbarkeit und Einladungen", zeichen = Zeichen.Handy)
            Wegzeile("Mitteilungen", beiMitteilungen, unterzeile = "Alarm und Nachrichten", zeichen = Zeichen.Melder)
            Wegzeile("Discord", beiDiscord, unterzeile = "Rolle und Verknüpfung", zeichen = Zeichen.Kanal)
            Wegzeile("Gemeinschaft", beiGemeinschaft, unterzeile = "Mannschaft und Wachenchat", zeichen = Zeichen.Gemeinschaft)
            Wegzeile("Shop", beiShop, unterzeile = "Credits und Freischaltungen", zeichen = Zeichen.WegShop)
        }

        if (dienst == null) return@Seite

        Spielweise(stand, dienst)
        Premiumkarte(stand, server, beiPremium = beiPremium)
        Sicherheit(konto, stand, dienst, beiAbmelden)
        Emailkarte(stand, dienst)
        Bedienungskarte()

        Karte(
            titel = "Wachengemeinschaft",
            zeichen = Zeichen.Gemeinschaft,
            text = "Deine feste Mannschaft mit eigenem Chat und gemeinsamen Runden.",
            knoepfe = { Knopf("Gemeinschaft öffnen", beiGemeinschaft, kompakt = true) },
        )

        Werbeabschnitt(stand, dienst)
        Fehlermeldung(stand, dienst)

        Karte(titel = "Rechtliches", zeichen = Zeichen.Wiki, text = "Alle verbindlichen Texte und Kontaktangaben.") {
            val recht = { seite: String -> browser.openUri(Rechtsstand.adresse(Server.BETRIEB, seite)) }
            Wegzeile("Nutzungsbedingungen", { recht(Rechtsstand.NUTZUNGSBEDINGUNGEN) })
            Wegzeile("AGB", { recht(Rechtsstand.AGB) })
            Wegzeile("Datenschutzerklärung", { recht(Rechtsstand.DATENSCHUTZ) })
            Wegzeile("Impressum", { recht(Rechtsstand.IMPRESSUM) })
            Wegzeile("Verträge hier kündigen", { browser.openUri(imWeb(server, "vertrag-kuendigen")) })
            Wegzeile("Vertrag widerrufen", { browser.openUri(imWeb(server, "vertrag-widerrufen")) })
        }

        if (stand.weltVorhanden) Weltreset(stand, dienst)

        Gefahrenkarte(
            titel = "Konto dauerhaft löschen",
            text = "Garage, Erfahrung, Dienstbuch und Freundschaften werden gelöscht. " +
                "Gemeinsame Schichten bleiben als Historie der Mannschaft bestehen. Ein " +
                "laufendes Premium-Abo wird dabei sofort beendet.",
        ) {
            Knopf("Konto löschen", beiLoeschen, art = Knopfart.Alarm, kompakt = true)
        }
    }
}

// ---------------------------------------------------------------- Die Karten

/**
 * Die Spielweise — ein Schalter, und er legt eine ganze Art um, Einsätze zu fahren.
 * Am Konto und nicht in der Lobby: eine Frage des Geschmacks, keine der Schicht.
 */
@Composable
private fun Spielweise(stand: Kontostand, dienst: Kontodienst) {
    Karte(titel = "Spielweise", zeichen = Zeichen.Fahrzeug, text = "Wie ausführlich du am Patienten arbeitest.") {
        Schalterzeile(
            titel = "Patientensimulation",
            unterzeile = "Vitalwerte messen (der Blutdruck dauert 20 Sekunden), xABCDE, SAMPLER " +
                "und OPQRST abarbeiten, Verdachtsdiagnose festhalten, behandeln. Aus heißt: " +
                "genau wie bisher.",
            an = stand.patientensimulation,
            beiWechsel = { dienst.simulationSetzen(it) },
            aktiv = stand.laeuft == null,
        )
        SehrLeise(
            "Sie bringt keinen Vorteil — weder mehr Punkte noch schnelleren Aufstieg. Sie " +
                "ist eine Art zu spielen und kein Weg nach oben.",
        )
        Rueckmeldungszeile(stand.meldung(Kontodienst.SIMULATION))
    }
}

/**
 * Premium — Stand und Wege, aber keine Kasse.
 *
 * <b>Der Satz unter dem Zustand sagt ein Datum nur, solange das Abo läuft.</b>
 * `bis` bleibt nach dem Ende stehen; „Free · läuft bis 01.07.2026" mit einem Datum
 * in der Vergangenheit war im Web der Fehler, der diese Regel erzwungen hat.
 */
@Composable
internal fun Premiumkarte(
    stand: Kontostand,
    server: String,
    beiPremium: () -> Unit,
) {
    val browser = LocalUriHandler.current
    val premium = stand.premium.inhalt

    Karte(titel = "Premium", zeichen = KontoZeichen.Stern, text = "Dein Plan und die Abo-Verwaltung.") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Statuspunkt(
                when {
                    premium?.aktiv == true -> Farben.GruenHell
                    premium?.zahlungOffen == true -> Farben.Amber
                    else -> Farben.RandHell
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                Text(premiumHinweis(premium), style = Schrift.Gross, color = Farben.Text)
                Text(premiumUnter(premium), style = Schrift.Klein, color = Farben.TextLeise)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            if (premium != null && premium.kaufVerfuegbar && !premium.aktiv && !premium.zahlungOffen) {
                Knopf("Premium ansehen", beiPremium, kompakt = true)
            }
            if (premium?.aktiv == true || premium?.zahlungOffen == true) {
                Knopf("Abo verwalten", { browser.openUri(imWeb(server, "konto")) }, kompakt = true)
            }
        }

        Echtgeldhinweis()

        if (stand.premium.fehler != null) Rueckmeldungszeile(Rueckmeldung(stand.premium.fehler, true))

        // Die beiden gesetzlichen Wege (§ 312k, § 356a BGB) — sie stehen immer da,
        // nicht nur bei laufendem Abo: Wer gerade gekündigt hat und widerrufen will,
        // muss sie genauso finden.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Abstand.Normal)) {
            Textweg("Verträge hier kündigen", { browser.openUri(imWeb(server, "vertrag-kuendigen")) })
            Textweg("Vertrag widerrufen", { browser.openUri(imWeb(server, "vertrag-widerrufen")) })
        }
    }
}

/**
 * Der eine Satz, der an jeder Stelle mit Geld steht.
 *
 * Er ist keine Entschuldigung, sondern eine Wegbeschreibung: Wer auf „Abo
 * verwalten" drückt und im Browser landet, soll vorher gewusst haben, warum.
 */
@Composable
internal fun Echtgeldhinweis() {
    Text(
        text = "Käufe mit echtem Geld nur auf pagerspass.de — die Knöpfe öffnen die Webseite. " +
            "Credits gibst du dagegen direkt hier in der App aus.",
        style = Schrift.Klein,
        color = Farben.AmberHell,
    )
}

internal fun premiumHinweis(premium: Premiumstand?): String = when {
    premium == null -> "Status wird geladen"
    premium.aktiv -> if (premium.plan == "Yearly") "Premium aktiv · Jahr" else "Premium aktiv · Monat"
    premium.zahlungOffen -> "Zahlung offen"
    else -> "Free"
}

private fun premiumUnter(premium: Premiumstand?): String = when {
    premium == null -> "Der Zustand deines Abos wird geladen."
    premium.aktiv -> premium.bis?.let { "Der bezahlte Zeitraum läuft bis zum ${tag(it)}." }
        ?: "Läuft, bis du in der Abo-Verwaltung kündigst."
    premium.zahlungOffen -> "Bring die Zahlung in Ordnung, sonst endet Premium."
    premium.status == "Canceled" -> if (premium.kaufVerfuegbar) {
        "Dein Abo ist beendet. Im Shop kannst du es neu abschließen."
    } else {
        "Dein Abo ist beendet. Neue Abos sind derzeit nicht verfügbar."
    }
    premium.kaufVerfuegbar -> "Premium kaufst du im Shop; hier verwaltest du dein Abo."
    else -> "Neue Premium-Abos sind derzeit nicht verfügbar."
}

/**
 * Sicherheit — Passwort, Benutzername und die Sitzung auf diesem Gerät.
 *
 * Jedes Formular klappt einzeln auf, wie im Web: Sie gleichzeitig offen stehen zu
 * lassen wäre ein Bogen, den niemand ausfüllt.
 */
@Composable
private fun Sicherheit(konto: Konto?, stand: Kontostand, dienst: Kontodienst, beiAbmelden: () -> Unit) {
    var offen by rememberSaveable { mutableStateOf<String?>(null) }
    var aktuelles by remember { mutableStateOf("") }
    var neues by remember { mutableStateOf("") }
    var wiederholt by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val laeuft = stand.laeuft != null

    Karte(titel = "Sicherheit", zeichen = KontoZeichen.Schloss, text = "Passwort und Sitzung auf diesem Gerät.") {
        Tatzeile("Passwort", "••••••••", mono = true) {
            Knopf("Ändern", { offen = if (offen == "passwort") null else "passwort" }, kompakt = true)
        }
        if (offen == "passwort") {
            Feld(aktuelles, { aktuelles = it }, etikett = "Aktuelles Passwort", geheim = true)
            Feld(neues, { neues = it }, etikett = "Neues Passwort", geheim = true)
            Feld(wiederholt, { wiederholt = it }, etikett = "Neues Passwort wiederholen", geheim = true, weiterTaste = ImeAction.Done)
            SehrLeise("Mindestens 8 Zeichen.")
            Knopf(
                "Passwort speichern",
                {
                    dienst.passwortAendern(aktuelles, neues, wiederholt) {
                        aktuelles = ""
                        neues = ""
                        wiederholt = ""
                        offen = null
                    }
                },
                art = Knopfart.Haupt,
                aktiv = !laeuft && aktuelles.isNotEmpty() && neues.length >= 8 && wiederholt.isNotEmpty(),
            )
        }
        Rueckmeldungszeile(stand.meldung(Kontodienst.PASSWORT))

        Tatzeile("Benutzername", "@${konto?.benutzername.orEmpty()}", mono = true) {
            Knopf(
                "Ändern",
                {
                    offen = if (offen == "name") null else "name"
                    name = konto?.benutzername.orEmpty()
                },
                kompakt = true,
            )
        }
        if (offen == "name") {
            SehrLeise(
                "3 bis 20 Zeichen: Buchstaben, Ziffern, Bindestrich, Unterstrich. Unter diesem " +
                    "Namen finden dich andere.",
            )
            Feld(name, { name = it.take(20) }, etikett = "Benutzername", weiterTaste = ImeAction.Done)
            Knopf(
                "Benutzernamen speichern",
                { dienst.benutzernameAendern(name) { offen = null } },
                art = Knopfart.Haupt,
                aktiv = !laeuft && name.trim().length >= 3 && name.trim() != konto?.benutzername,
            )
        }
        Rueckmeldungszeile(stand.meldung(Kontodienst.BENUTZERNAME))

        Tatzeile("Dieses Gerät", "Nur diese Sitzung wird beendet.") {
            Knopf("Abmelden", beiAbmelden, art = Knopfart.Leise, kompakt = true)
        }
    }
}

/**
 * Die E-Mail-Adresse — seit 5.0.0.58 Pflicht, und die Karte ist auf das
 * geschrumpft, was man dafür braucht: die Adresse, ihre Bestätigung und den
 * Newsletter. Wer den alten Anmeldeschutz noch an hat, sieht den Weg hinaus.
 *
 * Ohne bestätigte Adresse lässt der Server weder in eine Runde noch in die Welt —
 * deshalb steht hier deutlich, was noch fehlt.
 */
@Composable
private fun Emailkarte(stand: Kontostand, dienst: Kontodienst) {
    var offen by rememberSaveable { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var passwort by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var schutzPasswort by remember { mutableStateOf("") }
    val postfach: Postfach? = stand.postfach.inhalt
    val laeuft = stand.laeuft != null

    Karte(
        titel = "E-Mail-Adresse",
        zeichen = KontoZeichen.Brief,
        text = "Für ein vergessenes Passwort und — wenn du ihn bestellst — den Newsletter.",
    ) {
        if (postfach != null && !postfach.versandbereit) {
            Text(
                "Auf diesem Server ist gerade kein Postausgang eingerichtet — Codes werden nicht verschickt.",
                style = Schrift.MonoKlein,
                color = Farben.AmberHell,
            )
        }

        Tatzeile(
            titel = "Adresse",
            unterzeile = postfach?.email ?: "Keine hinterlegt",
            mono = true,
            zusatz = postfach?.email?.let {
                if (postfach.bestaetigt) "Bestätigt" to Farben.GruenHell else "Noch nicht bestätigt" to Farben.AmberHell
            },
        ) {
            Knopf(
                if (postfach?.email != null) "Ändern" else "Hinzufügen",
                {
                    offen = if (offen == "email") null else "email"
                    // Mit der bisherigen Adresse vorbelegt: Wer sie ändert, ändert
                    // meistens eine Kleinigkeit und tippt nicht alles neu.
                    email = postfach?.email.orEmpty()
                    dienst.rueckmeldungWeg(Kontodienst.POSTFACH)
                },
                kompakt = true,
            )
        }

        if (offen == "email") {
            Feld(email, { email = it.take(254) }, etikett = "E-Mail-Adresse", tastatur = KeyboardType.Email)
            Feld(passwort, { passwort = it }, etikett = "Passwort zur Bestätigung", geheim = true, weiterTaste = ImeAction.Done)
            SehrLeise(
                "Wir schicken dir einen sechsstelligen Code. Bis er eingegeben ist, wird die " +
                    "Adresse für nichts verwendet.",
            )
            Knopf(
                "Adresse speichern",
                {
                    dienst.emailSetzen(email, passwort) {
                        passwort = ""
                        // Direkt weiter zum Code: Er ist gerade rausgegangen.
                        offen = "bestaetigen"
                    }
                },
                art = Knopfart.Haupt,
                aktiv = !laeuft && email.contains('@') && passwort.isNotEmpty(),
            )
        }

        if (postfach?.email != null && !postfach.bestaetigt) {
            Tatzeile(
                "Bestätigen",
                "Der Code aus der E-Mail, sechs Ziffern. Erst bestätigt hilft die Adresse beim " +
                    "vergessenen Passwort.",
            ) {
                Knopf("Code eingeben", { offen = if (offen == "bestaetigen") null else "bestaetigen" }, kompakt = true)
            }
            if (offen == "bestaetigen") {
                Codefeld(code, { code = it.filter(Char::isDigit) }, etikett = "Code")
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf(
                        "Bestätigen",
                        { dienst.emailBestaetigen(code) { code = ""; offen = null } },
                        art = Knopfart.Haupt,
                        aktiv = !laeuft && code.length == 6,
                        kompakt = true,
                    )
                    Knopf("Nichts angekommen", { dienst.emailCodeAnfordern() }, art = Knopfart.Leise, aktiv = !laeuft, kompakt = true)
                }
            }
        }

        // Der Newsletter hängt an einer bestätigten Adresse (Double Opt-in).
        val newsletter = postfach?.newsletter == true
        Tatzeile(
            "Newsletter",
            "Gelegentlich Neues zum Spiel. Abmelden jederzeit, auch über den Link in jeder " +
                "Ausgabe." + if (postfach?.bestaetigt != true && !newsletter) " Erst nach der Bestätigung deiner Adresse." else "",
        ) {
            Knopf(
                if (newsletter) "Abbestellen" else "Bestellen",
                { dienst.newsletter(!newsletter) },
                aktiv = !laeuft && (postfach?.bestaetigt == true || newsletter),
                kompakt = true,
            )
        }

        if (postfach?.zweiFaktorAktiv == true) {
            Tatzeile(
                "Anmeldeschutz",
                "Bei dir noch eingeschaltet: Die Anmeldung verlangt einen Code aus deinem Postfach.",
            ) {
                Knopf("Ausschalten", { offen = if (offen == "schutz") null else "schutz" }, kompakt = true)
            }
            if (offen == "schutz") {
                Feld(schutzPasswort, { schutzPasswort = it }, etikett = "Passwort zur Bestätigung", geheim = true, weiterTaste = ImeAction.Done)
                Knopf(
                    "Anmeldeschutz ausschalten",
                    { dienst.anmeldeschutzAus(schutzPasswort) { schutzPasswort = ""; offen = null } },
                    art = Knopfart.Gefahr,
                    aktiv = !laeuft && schutzPasswort.isNotEmpty(),
                )
            }
        }

        stand.postfach.fehler?.let { Rueckmeldungszeile(Rueckmeldung(it, true)) }
        Rueckmeldungszeile(stand.meldung(Kontodienst.POSTFACH))
    }
}

/**
 * Freunde werben — der eigene Code zum Weitergeben und, solange das Konto jung
 * genug ist, das Feld für einen fremden.
 *
 * <b>Ein Feld, das ohne ein Wort verschwindet, ist eine Panne.</b> Wer kein
 * Einlösefeld sieht, liest hier, warum: selbst geworben, eigener Code erstellt,
 * oder das Fenster ist zu.
 */
@Composable
private fun Werbeabschnitt(stand: Kontostand, dienst: Kontodienst) {
    val zwischenablage = LocalClipboardManager.current
    var eingabe by remember { mutableStateOf("") }
    var kopiert by remember { mutableStateOf(false) }
    val laeuft = stand.laeuft != null

    Abschnitt("Freunde werben") {
        SehrLeise("Code weitergeben oder als neues Konto einen fremden Code einlösen.")

        val werbung: Werbung? = stand.werbung.inhalt
        if (werbung == null) {
            SehrLeise(stand.werbung.fehler ?: "Der Werbestand wird geholt …")
            return@Abschnitt
        }

        if (werbung.aktionTitel != null) {
            Kasten(marke = true, abstandInnen = Abstand.Winzig) {
                Text(
                    "${werbung.aktionTitel} — jede Werbung zählt gerade ${werbung.aktionFaktor}-fach: " +
                        "${werbung.creditsJetzt} statt ${werbung.creditsJeWerbung} Credits.",
                    style = Schrift.Klein,
                    color = Farben.AmberHell,
                )
                werbung.aktionBis?.let { SehrLeise("Noch bis ${zeitpunkt(it)}.") }
            }
        }

        Kasten(abstandInnen = Abstand.Klein) {
            Text("01 · Dein Code zum Weitergeben", style = Schrift.Gross, color = Farben.Text)
            Leise(
                "Dein Freund erhält einen Fahrzeug-Gutschein; du bekommst ${werbung.creditsJetzt} " +
                    "Credits nach seinem ersten Aufstieg.",
            )
            if (werbung.code != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(werbung.code, style = Schrift.MonoNormal.copy(fontSize = Schrift.TITEL), color = Farben.Amber, modifier = Modifier.weight(1f))
                    Knopf(
                        if (kopiert) "Kopiert" else "Kopieren",
                        {
                            zwischenablage.setText(AnnotatedString(werbung.code))
                            kopiert = true
                        },
                        kompakt = true,
                    )
                }
                SehrLeise(
                    "${werbung.geworben} geworben · ${werbung.ausgezahlt} mit Aufstieg · " +
                        "${werbung.verdienteCredits} Credits verdient" +
                        if (werbung.ausgezahlt >= werbung.deckel) {
                            " — Deckel erreicht."
                        } else {
                            " · höchstens ${werbung.deckel} Werbungen"
                        },
                )
            } else {
                if (werbung.darfEinloesen) {
                    SehrLeise("Mit deinem eigenen Code wirst du zum Werber; danach kannst du keinen fremden mehr einlösen.")
                }
                Knopf("Werbecode erstellen", { dienst.werbecodeErzeugen() }, aktiv = !laeuft, kompakt = true)
            }
        }

        Kasten(abstandInnen = Abstand.Klein) {
            Text("02 · Code eines Freundes", style = Schrift.Gross, color = Farben.Text)
            when {
                werbung.geworbenVon != null -> Leise(
                    "Du wurdest von ${werbung.geworbenVon} geworben. Dein Gutschein wartet in der Garage.",
                )
                werbung.darfEinloesen -> {
                    Feld(
                        eingabe,
                        { eingabe = it.uppercase().take(16) },
                        etikett = "Werbecode",
                        platzhalter = "z. B. H7KM-3QX4",
                        weiterTaste = ImeAction.Done,
                    )
                    SehrLeise("In den ersten ${werbung.fenstertage} Tagen nach der Anmeldung einlösbar.")
                    Knopf(
                        "Einlösen",
                        { dienst.werbungEinloesen(eingabe) { eingabe = "" } },
                        aktiv = !laeuft && eingabe.isNotBlank(),
                        kompakt = true,
                    )
                }
                werbung.code != null -> Leise(
                    "Du hast deinen eigenen Werbecode erstellt — damit bist du der Werber. Einen " +
                        "fremden Code einlösen kann nur, wer selbst noch keinen ausgegeben hat.",
                )
                else -> Leise(
                    "Werbecodes gelten in den ersten ${werbung.fenstertage} Tagen nach der Anmeldung — " +
                        "dein Konto ist schon länger dabei.",
                )
            }
        }
        Rueckmeldungszeile(stand.meldung(Kontodienst.WERBUNG))
    }
}

/** „Fehler melden" — Beschreibung und Hergang sind Pflicht: Ohne Weg lässt sich nichts nachstellen. */
@Composable
private fun Fehlermeldung(stand: Kontostand, dienst: Kontodienst) {
    var offen by rememberSaveable { mutableStateOf(false) }
    var titel by remember { mutableStateOf("") }
    var beschreibung by remember { mutableStateOf("") }
    var hergang by remember { mutableStateOf("") }
    var seite by remember { mutableStateOf("") }

    Karte(titel = "Fehler melden", zeichen = KontoZeichen.Hilfe, text = "Beschreibe den Weg zum Problem direkt aus dem Spiel.") {
        Knopf(
            "Bug melden",
            {
                offen = !offen
                // Wer das Formular erneut öffnet, will die nächste Meldung schreiben —
                // der Dank für die letzte hat dann seinen Dienst getan.
                dienst.rueckmeldungWeg(Kontodienst.BUG)
            },
            kompakt = true,
        )
        if (offen) {
            Feld(titel, { titel = it.take(120) }, etikett = "Titel", platzhalter = "Worum geht es, in einem Satz?")
            Feld(
                beschreibung,
                { beschreibung = it.take(4000) },
                etikett = "Was ist passiert?",
                platzhalter = "Was war zu sehen, was hat gefehlt?",
                einzeilig = false,
            )
            Feld(
                hergang,
                { hergang = it.take(4000) },
                etikett = "Wie kam es dazu?",
                platzhalter = "Schritt für Schritt bis zum Fehler.",
                einzeilig = false,
            )
            Feld(seite, { seite = it.take(200) }, etikett = "Wo im Spiel? (freiwillig)", platzhalter = "z. B. Leitstelle oder Garage", weiterTaste = ImeAction.Done)
            Knopf(
                "Meldung abschicken",
                {
                    dienst.bugMelden(titel, beschreibung, hergang, seite) {
                        titel = ""; beschreibung = ""; hergang = ""; seite = ""; offen = false
                    }
                },
                art = Knopfart.Haupt,
                aktiv = stand.laeuft == null && titel.isNotBlank() && beschreibung.isNotBlank() && hergang.isNotBlank(),
            )
        }
        Rueckmeldungszeile(stand.meldung(Kontodienst.BUG))
    }
}

/**
 * PagerSpass - World zurücksetzen.
 *
 * <b>Dieselbe Bestätigung wie beim Löschen des Kontos:</b> eine Rückfrage mit dem
 * Passwort. Das Konto bleibt; zurückgesetzt wird nur der Weltbestand — danach darf
 * man einen neuen Standort wählen, und genau dafür ist der Knopf da.
 */
@Composable
private fun Weltreset(stand: Kontostand, dienst: Kontodienst) {
    var frage by remember { mutableStateOf(false) }
    var passwort by remember { mutableStateOf("") }

    Gefahrenkarte(
        titel = "PagerSpass - World zurücksetzen",
        text = "Leitstelle, Wachen, Fahrzeuge, Guthaben und Buchungsblatt werden gelöscht. Dein " +
            "Konto und alles andere bleiben. Danach wählst du einen neuen Standort.",
    ) {
        Knopf("Welt zurücksetzen", { frage = true }, art = Knopfart.Alarm, kompakt = true)
    }
    Rueckmeldungszeile(stand.meldung(Kontodienst.WELT))

    if (frage) {
        Blende(
            titel = "Welt dauerhaft zurücksetzen",
            beiSchliessen = { frage = false },
            breite = Dialogbreite.Schmal,
            fussAlsSpalte = true,
            fuss = {
                Knopf(
                    "Welt dauerhaft zurücksetzen",
                    { dienst.weltZuruecksetzen(passwort) { passwort = ""; frage = false } },
                    art = Knopfart.Alarm,
                    breit = true,
                    aktiv = stand.laeuft == null && passwort.isNotBlank(),
                )
                Knopf("Abbrechen", { frage = false }, art = Knopfart.Leise, breit = true)
            },
        ) {
            Text(
                "Leitstelle, Wachen, Fahrzeuge, Guthaben und Buchungsblatt werden gelöscht. Das " +
                    "lässt sich nicht rückgängig machen.",
                style = Schrift.Normal,
                color = Farben.TextLeise,
            )
            Feld(passwort, { passwort = it }, etikett = "Passwort zur Bestätigung", geheim = true, weiterTaste = ImeAction.Done)
            Rueckmeldungszeile(stand.meldung(Kontodienst.WELT)?.takeIf { it.fehler })
        }
    }
}

// ------------------------------------------------------------------- Discord

/**
 * Discord verknüpfen — das Gegenstück zu `DiscordVerknuepfenView.vue`.
 *
 * <b>Die Zustimmung geschieht im Browser.</b> Der Server liefert die Adresse samt
 * `state`; die App öffnet sie und holt den Stand neu, sobald sie wieder im
 * Vordergrund ist — der Rückweg von Discord landet auf der Webseite, nicht hier.
 */
@Composable
fun DiscordSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    stand: Kontostand = Kontostand(),
    dienst: Kontodienst? = null,
    beiZurueck: () -> Unit = {},
) {
    val browser = LocalUriHandler.current
    var loesenFrage by remember { mutableStateOf(false) }
    BeiRueckkehr { dienst?.discordLaden() }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Discord",
            unterzeile = "Rolle und Verknüpfung",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Abschnitt("Discord verknüpfen") {
            Bereich(
                laedt = stand.discord.ersteLadung,
                fehler = stand.discord.fehler,
                inhalt = stand.discord.inhalt,
                beiErneut = { dienst?.discordLaden() },
            ) { status ->
                Kasten(abstandInnen = Abstand.Klein) {
                    if (status.verknuepft) {
                        Text("Verknüpft mit ${status.discordName ?: "Discord"}", style = Schrift.Gross, color = Farben.Text)
                        SehrLeise(
                            "Seit ${tag(status.verknuepftAm)}. Dein Dienstgrad wird bei jedem Aufstieg " +
                                "nachgezogen; einmal täglich gleicht der Server zusätzlich ab.",
                        )
                        if (!loesenFrage) {
                            Knopf("Verknüpfung lösen", { loesenFrage = true }, art = Knopfart.Leise, kompakt = true)
                        } else {
                            SehrLeise(
                                "Discord entzieht dir dann alle PagerSpass-Rollen. Dein Spielkonto bleibt " +
                                    "unberührt; neu verknüpfen geht jederzeit.",
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                                Knopf(
                                    "Ja, lösen",
                                    { dienst?.discordLoesen(); loesenFrage = false },
                                    aktiv = stand.laeuft == null,
                                    kompakt = true,
                                )
                                Knopf("Behalten", { loesenFrage = false }, art = Knopfart.Leise, kompakt = true)
                            }
                        }
                    } else {
                        Text("Dein Dienstgrad als Discord-Rolle", style = Schrift.Gross, color = Farben.Text)
                        SehrLeise(
                            "Du meldest dich einmal bei Discord an und stimmst zu — danach trägt dein " +
                                "Discord-Profil „PagerSpass · ${konto?.benutzername.orEmpty()}“, und der " +
                                "Server gibt dir die Rolle zu deinem Rang (${konto?.rang.orEmpty()}) von " +
                                "selbst. Steigst du auf, zieht die Rolle nach.",
                        )
                        Knopf(
                            if (stand.laeuft == Kontodienst.DISCORD) "Einen Moment …" else "Mit Discord verbinden",
                            { dienst?.discordStarten { browser.openUri(it) } },
                            art = Knopfart.Haupt,
                            aktiv = stand.laeuft == null,
                        )
                    }
                }
            }
            Rueckmeldungszeile(stand.meldung(Kontodienst.DISCORD))
        }

        Abschnitt("Was Discord dabei erfährt") {
            Leise(
                "Deinen Benutzernamen, deine Laufbahnstufe, deinen Dienstgrad und die Zahl deiner " +
                    "gefahrenen Dienste — das ist alles, und nur solange die Verknüpfung besteht. Keine " +
                    "Schichten, keine Freunde, keine Nachrichten. PagerSpass erfährt umgekehrt nur " +
                    "deinen Discord-Namen.",
            )
        }
    }
}

// -------------------------------------------------------------- Privatsphäre

/**
 * Was die Privatsphäre über die Schalter hinaus zeigt: Blockaden,
 * Produktverbesserung, Übertragungen, angemeldete Geräte und die Rechte aus der
 * DSGVO — in der Reihenfolge von `PrivatsphaereView.vue`.
 *
 * <b>Der Datenauszug wird eine Datei, die man selbst ablegt.</b> Android fragt
 * dafür nach dem Ort (`CreateDocument`); ein Teilen-Blatt mit dem ganzen JSON
 * als Text stieße an die Größengrenze eines Intents, und eine stille Ablage im
 * Download-Ordner wäre eine Datei, von der man nicht weiß, dass es sie gibt.
 */
@Composable
fun ColumnScope.PrivatsphaereZusatz(
    konto: Konto?,
    stand: Kontostand,
    dienst: Kontodienst,
    freunde: List<Freund>,
    beiFreundeLaden: () -> Unit,
    beiKonto: () -> Unit,
    beiMitteilungen: () -> Unit,
    /**
     * Am Server abmelden und erst danach zurückkehren — für „Gespeicherte Daten
     * dieses Geräts löschen“. Ohne ihn fehlt der Abschnitt „Dieses Gerät“.
     */
    beiAbmelden: (suspend () -> Unit)? = null,
) {
    val browser = LocalUriHandler.current
    val zusammenhang = androidx.compose.ui.platform.LocalContext.current
    var auszug by remember { mutableStateOf<String?>(null) }
    var datenverarbeitung by rememberSaveable { mutableStateOf(false) }
    if (datenverarbeitung) Datenverarbeitungsblende { datenverarbeitung = false }
    val ablegen = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { ziel ->
        val text = auszug
        if (ziel != null && text != null) {
            runCatching {
                zusammenhang.contentResolver.openOutputStream(ziel)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            }
        }
        auszug = null
    }

    BeiRueckkehr {
        beiFreundeLaden()
        dienst.einwilligungenLaden()
        dienst.sitzungenLaden()
    }

    val blockiert = freunde.filter { it.stand == "Blockiert" && it.vonMir }

    Abschnitt("Blockierte Konten") {
        Leise(
            "Wer hier steht, kann dir weder Anfragen noch Nachrichten schicken und findet dich " +
                "nicht in der Suche. Davon erfährt er nichts. Blockieren kannst du in den Kontakten " +
                "oder im Profil eines Kontos.",
        )
        if (blockiert.isEmpty()) {
            Leerhinweis("Du hast niemanden blockiert.")
        } else {
            blockiert.forEach { b ->
                Tatzeile(b.anzeigename.ifBlank { b.benutzername }, "@${b.benutzername}", mono = true) {
                    val hier = stand.laeuft == "${Kontodienst.BLOCKADE}:${b.kennung}"
                    Knopf(
                        if (hier) "Wird aufgehoben …" else "Blockade aufheben",
                        { dienst.blockadeAufheben(b.kennung) },
                        art = Knopfart.Leise,
                        aktiv = stand.laeuft == null,
                        kompakt = true,
                    )
                }
            }
        }
        Rueckmeldungszeile(stand.meldung(Kontodienst.BLOCKADE))
    }

    Abschnitt("Produktverbesserung") {
        Schalterzeile(
            titel = "Daten für Verbesserung der Produkte verwenden",
            unterzeile = "Zeichnet den Funkverkehr mit den Bots und die Notrufgespräche mit der " +
                "Leitstelle auf — was du funkst und was der Bot antwortet, samt Kennung, Rolle und " +
                "Fahrzeug. Kein Ton, kein Name. Aus heißt: Es wird nichts mehr aufgezeichnet, und " +
                "alles bisher Gespeicherte wird gelöscht.",
            an = konto?.analyseZustimmung == true,
            beiWechsel = { dienst.analyseSetzen(it) },
            aktiv = stand.laeuft == null,
        )
        Textweg("Datenverarbeitung anzeigen", { datenverarbeitung = true })
        Rueckmeldungszeile(stand.meldung(Kontodienst.ANALYSE))
    }

    Abschnitt("Übertragungen") {
        Leise(
            "Wem du erlaubt hast, eine Schicht mit dir öffentlich zu übertragen. Ein Widerruf wirkt " +
                "ab sofort: In eine Runde dieser Leitstelle kommst du danach nur noch, wenn du erneut " +
                "einwilligst. Was bis dahin gesendet wurde, können wir nicht zurückholen — dafür ist " +
                "verantwortlich, wer überträgt.",
        )
        Bereich(
            laedt = stand.einwilligungen.ersteLadung,
            fehler = stand.einwilligungen.fehler,
            inhalt = stand.einwilligungen.inhalt,
            beiErneut = { dienst.einwilligungenLaden() },
        ) { liste ->
            if (liste.isEmpty()) {
                Leerhinweis("Du hast noch keiner Übertragung zugestimmt.")
            }
            liste.forEach { e ->
                Kasten(abstandInnen = Abstand.Winzig) {
                    Text(e.streamerName, style = Schrift.Normal, color = Farben.Text)
                    SehrLeise("${plattformname(e.plattform)} · ${e.kanal}", mono = true)
                    SehrLeise(
                        when {
                            e.widerrufenUm != null -> "Widerrufen am ${tag(e.widerrufenUm)}"
                            e.wartetAufEltern -> "Wartet auf die Unterschrift eines Erziehungsberechtigten"
                            !e.gilt -> "Abgelaufen am ${tag(e.laeuftAbUm)} — beim nächsten Beitritt wird neu gefragt"
                            else -> "Erteilt am ${tag(e.erteiltUm)}, gilt bis ${tag(e.laeuftAbUm)}"
                        } + if (e.aufzeichnung) " · auch als Aufzeichnung" else "",
                    )
                    e.elternbogen?.let { bogen ->
                        Textweg("Bogen für die Erziehungsberechtigten öffnen", { browser.openUri(bogen) })
                    }
                    if (e.widerrufenUm == null) {
                        val hier = stand.laeuft == "${Kontodienst.EINWILLIGUNG}:${e.id}"
                        Knopf(
                            if (hier) "Wird widerrufen …" else "Widerrufen",
                            { dienst.einwilligungWiderrufen(e.id) },
                            art = Knopfart.Leise,
                            aktiv = stand.laeuft == null,
                            kompakt = true,
                        )
                    }
                }
            }
        }
        Rueckmeldungszeile(stand.meldung(Kontodienst.EINWILLIGUNG))
        Textweg("Einwilligungstext lesen", { browser.openUri(Rechtsstand.adresse(Server.BETRIEB, "uebertragung")) })
    }

    beiAbmelden?.let { DiesesGeraet(it) }

    Abschnitt("Angemeldete Geräte") {
        Leise(
            "Wo dein Konto gerade angemeldet ist. Gerät und Ort speichern wir zu einer Anmeldung " +
                "nicht — erkennen kannst du sie am Zeitpunkt. Eine Anmeldung verfällt nach 90 Tagen " +
                "ohne Nutzung von selbst.",
        )
        val sitzungen = stand.sitzungen.inhalt.orEmpty()
        Bereich(
            laedt = stand.sitzungen.ersteLadung,
            fehler = stand.sitzungen.fehler,
            inhalt = stand.sitzungen.inhalt,
            beiErneut = { dienst.sitzungenLaden() },
        ) { liste ->
            liste.forEach { s ->
                Tatzeile(
                    if (s.diese) "Dieses Gerät" else "Anderes Gerät",
                    "Angemeldet am ${zeitpunkt(s.erstelltUm)} · zuletzt genutzt ${zeitpunkt(s.zuletztGenutztUm)}",
                )
            }
        }
        Knopf(
            if (stand.laeuft == Kontodienst.SITZUNGEN) "Wird abgemeldet …" else "Auf allen anderen Geräten abmelden",
            { dienst.andereSitzungenBeenden() },
            aktiv = stand.laeuft == null && sitzungen.any { !it.diese },
        )
        Rueckmeldungszeile(stand.meldung(Kontodienst.SITZUNGEN))
    }

    Abschnitt("Deine Rechte") {
        Tatzeile(
            "Auskunft und Datenübertragbarkeit",
            "Alles, was zu deinem Konto gespeichert ist, als eine JSON-Datei — Konto, " +
                "Einstellungen, Dienstbuch, Garage, Nachrichten, Einträge, Gemeinschaften, " +
                "Einwilligungen, Käufe und die Aufzeichnung zur Produktverbesserung (Art. 15 und 20 " +
                "DSGVO). Passwort- und Sitzungsabdrücke fehlen aus Sicherheitsgründen, Meldungen " +
                "anderer über dich zum Schutz der Meldenden.",
        ) {
            Knopf(
                if (stand.laeuft == Kontodienst.AUSZUG) "Wird erstellt …" else "Herunterladen",
                {
                    dienst.datenauszug { text ->
                        auszug = text
                        ablegen.launch("pagerspass-datenauszug-${java.time.LocalDate.now()}.json")
                    }
                },
                aktiv = stand.laeuft == null,
                kompakt = true,
            )
        }
        Rueckmeldungszeile(stand.meldung(Kontodienst.AUSZUG))
        Tatzeile(
            "Berichtigung",
            "Anzeigename, Benutzername, E-Mail-Adresse und Profil änderst du selbst im Konto " +
                "(Art. 16 DSGVO).",
        ) { Knopf("Zum Konto", beiKonto, art = Knopfart.Leise, kompakt = true) }
        Tatzeile(
            "Löschung",
            "Dein Konto samt Garage, Dienstbuch, Freundschaften, Profilbild und Aufzeichnungen " +
                "löschst du jederzeit selbst, ganz unten im Konto (Art. 17 DSGVO). Ein laufendes " +
                "Premium-Abo endet dabei mit.",
        ) { Knopf("Zum Konto", beiKonto, art = Knopfart.Leise, kompakt = true) }
        Tatzeile(
            "Widerruf von Einwilligungen",
            "Produktverbesserung, Übertragungen und Kartenhintergrund nimmst du oben mit einem " +
                "Klick zurück, den Newsletter im Konto, Mitteilungen in deren Einstellungen, die " +
                "Discord-Verknüpfung dort (Art. 7 Abs. 3 DSGVO). Der Widerruf wirkt für die Zukunft.",
        ) { Knopf("Mitteilungen", beiMitteilungen, art = Knopfart.Leise, kompakt = true) }
        Tatzeile(
            "Einschränkung und Widerspruch",
            "Du kannst verlangen, dass wir Daten nur noch aufbewahren, statt sie zu nutzen (Art. 18 " +
                "DSGVO), und jeder Verarbeitung aus berechtigtem Interesse widersprechen (Art. 21 " +
                "DSGVO). Eine formlose E-Mail an $SUPPORT_EMAIL genügt; wir antworten innerhalb " +
                "eines Monats.",
        ) {
            Knopf(
                "E-Mail schreiben",
                { browser.openUri("mailto:$SUPPORT_EMAIL?subject=Datenschutz") },
                art = Knopfart.Leise,
                kompakt = true,
            )
        }
        Tatzeile(
            "Beschwerde bei einer Aufsichtsbehörde",
            "Du kannst dich jederzeit bei einer Datenschutz-Aufsichtsbehörde beschweren (Art. 77 " +
                "DSGVO). Für uns zuständig ist der Landesbeauftragte für den Datenschutz Niedersachsen.",
        ) {
            Knopf("Öffnen", { browser.openUri("https://www.lfd.niedersachsen.de") }, art = Knopfart.Leise, kompakt = true)
        }
    }
}

/** Die Adresse für Anfragen zum Datenschutz — `SUPPORT_EMAIL` in `rechtstexte.ts`. */
private const val SUPPORT_EMAIL = "support@pagerspass.de"

// -------------------------------------------------------- Passwort vergessen

/**
 * „Passwort vergessen" — zwei Schritte, wie in `LoginView.vue`.
 *
 * <b>Der zweite Schritt kommt immer</b>, auch wenn es das Konto nicht gibt: Der
 * Server antwortet in jedem Fall gleich, und ein Formular, das bei einem
 * unbekannten Namen stehen bliebe, machte diese Zusage zunichte.
 *
 * <b>Danach wird nicht automatisch angemeldet.</b> Der Server hat gerade alle
 * Sitzungen des Kontos beendet, und das soll er auch; die Anmeldung steht mit dem
 * Namen schon ausgefüllt da (`beiFertig`).
 */
@Composable
fun PasswortVergessen(
    stand: Kontostand,
    dienst: Kontodienst,
    vorbelegt: String,
    beiFertig: (benutzer: String, satz: String) -> Unit,
    beiZurueck: () -> Unit,
) {
    var schritt by rememberSaveable { mutableStateOf(1) }
    var benutzer by rememberSaveable { mutableStateOf(vorbelegt) }
    var code by remember { mutableStateOf("") }
    var passwort by remember { mutableStateOf("") }
    val laeuft = stand.laeuft != null
    val meldung = stand.meldung(Kontodienst.VERGESSEN)

    Kasten(innenraum = Abstand.Gross) {
        if (schritt == 1) {
            Text("Passwort vergessen", style = Schrift.Titel, color = Farben.Text)
            SehrLeise(
                "Gib deinen Benutzernamen oder deine hinterlegte E-Mail-Adresse an. Wenn dazu ein " +
                    "Konto mit bestätigter Adresse gehört, schicken wir einen Code.",
            )
            Feld(benutzer, { benutzer = it.take(254) }, etikett = "Benutzername oder E-Mail", weiterTaste = ImeAction.Done)
            Knopf("Code anfordern", { dienst.passwortVergessen(benutzer) { schritt = 2 } }, art = Knopfart.Haupt, breit = true, aktiv = !laeuft)
            Knopf("Zurück", { dienst.rueckmeldungWeg(Kontodienst.VERGESSEN); beiZurueck() }, breit = true)
            if (meldung?.fehler == true) Rueckmeldungszeile(meldung)
        } else {
            Text("Neues Passwort", style = Schrift.Titel, color = Farben.Text)
            if (meldung != null) Rueckmeldungszeile(meldung)
            Codefeld(code, { code = it.filter(Char::isDigit) }, etikett = "Code aus der E-Mail")
            Feld(passwort, { passwort = it }, etikett = "Neues Passwort", geheim = true, weiterTaste = ImeAction.Done)
            SehrLeise("Mindestens 8 Zeichen.")
            Knopf(
                "Passwort setzen",
                {
                    dienst.passwortNeu(benutzer, code, passwort) {
                        beiFertig(benutzer.trim(), "Dein neues Passwort steht. Melde dich jetzt damit an.")
                        dienst.rueckmeldungWeg(Kontodienst.VERGESSEN)
                    }
                },
                art = Knopfart.Haupt,
                breit = true,
                aktiv = !laeuft && code.length == 6 && passwort.length >= 8,
            )
            Knopf("Abbrechen", { dienst.rueckmeldungWeg(Kontodienst.VERGESSEN); beiZurueck() }, breit = true)
        }
    }
}

// ---------------------------------------------------------------- Blenden

/**
 * Die Frage nach der Aufzeichnung — `AnalyseDialog.vue`. Sie steht, solange das
 * Konto noch nicht geantwortet hat (`analyseZustimmung == null`), und nach dem
 * Rechtsstand, vor der Pflicht-Adresse.
 */
@Composable
fun Analyseblende(stand: Kontostand, dienst: Kontodienst) {
    // Der Text steht in der App, nicht auf der Webseite — gefragt wird hier.
    var datenverarbeitung by rememberSaveable { mutableStateOf(false) }
    Blende(
        titel = "Dürfen wir deine Daten für die Verbesserung unseres Produktes verwenden?",
        beiSchliessen = {},
        schliessenMoeglich = false,
        fussAlsSpalte = true,
        fuss = {
            Knopf("Ja, gerne", { dienst.analyseSetzen(true) }, art = Knopfart.Haupt, breit = true, aktiv = stand.laeuft == null)
            Knopf("Nein, danke", { dienst.analyseSetzen(false) }, breit = true, aktiv = stand.laeuft == null)
            Knopf(
                "Datenverarbeitung anzeigen",
                { datenverarbeitung = true },
                art = Knopfart.Leise,
                breit = true,
            )
        },
    ) {
        Text(
            "Damit ist der Funkverkehr mit den Bots und die Notrufgespräche mit der Leitstelle " +
                "gemeint — was du funkst und was der Bot antwortet. Daran verbessern wir beides.",
            style = Schrift.Normal,
            color = Farben.TextLeise,
        )
        SehrLeise(
            "Freiwillig, jederzeit widerrufbar unter Konto → Privatsphäre. Ein Widerruf löscht auch " +
                "das bereits Aufgezeichnete. Ohne Zustimmung funktioniert das Spiel vollständig weiter.",
        )
        Rueckmeldungszeile(stand.meldung(Kontodienst.ANALYSE))
    }
    // Nach der Frage gesetzt, damit das Fenster über ihr aufgeht und nicht darunter.
    if (datenverarbeitung) Datenverarbeitungsblende { datenverarbeitung = false }
}

/**
 * Die Pflicht, eine E-Mail-Adresse zu hinterlegen — `EmailPflichtDialog.vue`.
 *
 * <b>Er sperrt aus, wie der Rechtsstand.</b> Ohne Adresse lässt der Server weder in
 * eine Runde noch in die Welt; der einzige Weg daran vorbei ist „Abmelden". Der
 * zweite Schritt (der Code) lässt sich auf „Später" vertagen — die Pflicht ist mit
 * dem Eintragen erfüllt.
 */
@Composable
fun EmailPflichtblende(stand: Kontostand, dienst: Kontodienst, beiAbmelden: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var passwort by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val laeuft = stand.laeuft != null
    val bestaetigen = stand.emailBestaetigungOffen

    Blende(
        titel = if (bestaetigen) "Adresse bestätigen" else "Hinterlege deine E-Mail-Adresse",
        beiSchliessen = {},
        schliessenMoeglich = false,
        breite = Dialogbreite.Schmal,
        fussAlsSpalte = true,
        fuss = {
            if (bestaetigen) {
                Knopf("Bestätigen", { dienst.emailBestaetigen(code) }, art = Knopfart.Haupt, breit = true, aktiv = !laeuft && code.length == 6)
                Knopf("Neuen Code schicken", { dienst.emailCodeAnfordern() }, breit = true, aktiv = !laeuft)
                Knopf("Später", { dienst.emailBestaetigungSpaeter() }, art = Knopfart.Leise, breit = true)
            } else {
                Knopf(
                    "Adresse speichern",
                    { dienst.emailSetzen(email, passwort, pflicht = true) { passwort = "" } },
                    art = Knopfart.Haupt,
                    breit = true,
                    aktiv = !laeuft && email.contains('@') && passwort.isNotEmpty(),
                )
                Knopf("Abmelden", beiAbmelden, art = Knopfart.Leise, breit = true, aktiv = !laeuft)
            }
        },
    ) {
        if (!bestaetigen) {
            Text(
                "Zu jedem Konto gehört jetzt eine E-Mail-Adresse — ohne sie kannst du nicht " +
                    "weiterspielen. Wir nutzen sie nur für zwei Dinge: damit du wieder hineinkommst, " +
                    "wenn du dein Passwort vergisst, und für den Newsletter, aber nur, wenn du ihn " +
                    "ausdrücklich bestellst.",
                style = Schrift.Normal,
                color = Farben.TextLeise,
            )
            Feld(email, { email = it.take(254) }, etikett = "E-Mail-Adresse", platzhalter = "kim@beispiel.de", tastatur = KeyboardType.Email)
            Feld(passwort, { passwort = it }, etikett = "Dein Passwort zur Bestätigung", geheim = true, weiterTaste = ImeAction.Done)
        } else {
            SehrLeise(
                "Erst eine bestätigte Adresse hilft beim vergessenen Passwort. Du kannst das auch " +
                    "später in den Kontoeinstellungen erledigen.",
            )
            Codefeld(code, { code = it.filter(Char::isDigit) }, etikett = "Code aus der E-Mail")
        }
        Rueckmeldungszeile(stand.meldung(Kontodienst.POSTFACH))
    }
}

/** Was im Geschenk war — erst beim Öffnen erfährt man es. */
@Composable
fun Geschenkblende(stand: Kontostand, dienst: Kontodienst) {
    val inhalt = stand.geschenk ?: return
    Blende(
        titel = "Ein Geschenk",
        beiSchliessen = { dienst.geschenkWeg() },
        breite = Dialogbreite.Schmal,
        fuss = { Knopf("Danke", { dienst.geschenkWeg() }, art = Knopfart.Haupt, breit = true) },
    ) {
        Text(inhalt.artikelName, style = Schrift.MonoNormal.copy(fontSize = Schrift.TITEL), color = Farben.Amber)
        SehrLeise("Von ${inhalt.vonName}. Es liegt jetzt in deinem Besitz — anlegen kannst du es im Profil.")
    }
}

// ---------------------------------------------------------------- Bausteine

/**
 * Wenn die App wieder in den Vordergrund kommt — und beim ersten Zeigen.
 *
 * Android meldet einem neu angehängten Beobachter die verpassten Ereignisse nach;
 * wer auf einer sichtbaren Seite `ON_RESUME` abonniert, bekommt es sofort. Damit
 * ist dieser eine Haken zugleich das Laden beim Öffnen und das Nachladen nach der
 * Rückkehr aus dem Browser — wo gekauft, gekündigt oder verknüpft wurde.
 */
@Composable
internal fun BeiRueckkehr(tun: () -> Unit) {
    val besitzer = LocalLifecycleOwner.current
    val aktuell by rememberUpdatedState(tun)
    DisposableEffect(besitzer) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            if (ereignis == Lifecycle.Event.ON_RESUME) aktuell()
        }
        besitzer.lifecycle.addObserver(beobachter)
        onDispose { besitzer.lifecycle.removeObserver(beobachter) }
    }
}

/** Eine Seite des mobilen Webs — der Weg für alles, was Geld kostet. */
internal fun imWeb(server: String, seite: String) = "${server.trimEnd('/')}/play/mobile/$seite"

/** Die Zeile unter einer Karte — grün für „erledigt", rot und in Monospace für Fehler. */
@Composable
internal fun Rueckmeldungszeile(meldung: Rueckmeldung?) {
    if (meldung == null) return
    Text(
        text = meldung.text,
        style = if (meldung.fehler) Schrift.MonoKlein else Schrift.Klein,
        color = if (meldung.fehler) Farben.SignalHell else Farben.GruenHell,
    )
}

/**
 * Eine Zeile aus Titel, Unterzeile und Handlung — `.tatzeile` im Web.
 *
 * @param zusatz Eine dritte, farbige Zeile („Bestätigt", „Noch nicht bestätigt").
 */
@Composable
internal fun Tatzeile(
    titel: String,
    unterzeile: String? = null,
    mono: Boolean = false,
    zusatz: Pair<String, Color>? = null,
    knopf: (@Composable () -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar), modifier = Modifier.weight(1f)) {
            Text(titel, style = Schrift.Normal, color = Farben.Text)
            if (unterzeile != null) SehrLeise(unterzeile, mono = mono)
            if (zusatz != null) Text(zusatz.first, style = Schrift.Klein, color = zusatz.second)
        }
        knopf?.invoke()
    }
}

/** Ein Gefahrenbereich — rot umrandet, mit Kennung darüber. */
@Composable
private fun Gefahrenkarte(titel: String, text: String, knopf: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .padding(top = Abstand.SehrGross)
            .fillMaxWidth()
            .flaeche(randfarbe = Farben.SignalTief)
            .padding(Abstand.Gross),
    ) {
        Text("GEFAHRENBEREICH", style = Schrift.Etikett, color = Farben.SignalHell)
        Text(titel, style = Schrift.Gross, color = Farben.Text)
        Leise(text)
        knopf()
    }
}

/** Der Punkt vor dem Zustand — Farbe an einer Stelle, an der Farbe etwas heißt. */
@Composable
private fun Statuspunkt(farbe: Color) {
    Box(Modifier.size(10.dp).background(farbe, CircleShape))
}

// ------------------------------------------------------------------- Zeichen

/**
 * Die paar Zeichen, die nur die Konto-Zentrale braucht — auf demselben 24er-Raster
 * und in derselben Strichstärke wie `Zeichen`, aber hier und nicht dort, weil sie
 * zu dieser Seite gehören und nirgends sonst stehen.
 */
internal object KontoZeichen {
    val Stern = strich("stern", "m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z")
    val Schloss = strich("schloss", "M6 11h12v10H6z", "M8.5 11V8a3.5 3.5 0 0 1 7 0v3")
    val Brief = strich("brief", "M3.5 6h17v12h-17z", "m3.5 6 8.5 7 8.5-7")
    val Hilfe = strich("hilfe", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z", "M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6v.6", "M12 17h.01")
    val Geschenk = strich("geschenk", "M4 10h16v4H4z", "M5.5 14h13v7h-13z", "M12 10v11", "M12 10c-2.5 0-4.5-1-4.5-3a2 2 0 0 1 4.5 0c0-2 2-2 2-2a2 2 0 0 1 2.5 2c0 2-2 3-4.5 3")
}

private fun strich(name: String, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        pfade.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** Geldbeträge wie auf der Webseite: „4,99 €". */
internal fun euro(cent: Int): String = String.format(Locale.GERMANY, "%.2f €", cent / 100.0)
