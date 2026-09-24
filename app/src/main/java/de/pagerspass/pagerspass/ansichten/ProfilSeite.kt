package de.pagerspass.pagerspass.ansichten

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Profil
import de.pagerspass.pagerspass.netz.Profilaenderung
import de.pagerspass.pagerspass.netz.Profilbildstand
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.schmuck.Profilbanner
import de.pagerspass.pagerspass.ui.schmuck.Schmuck
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Das eigene Profil — ansehen und einrichten.
 *
 * <b>Die Vorschau steht oben und ist keine Vorschau, sondern das Ding selbst.</b>
 * Dasselbe Banner, das andere im Profil sehen. Wer ein Muster wählt, sieht es
 * eine Zeile höher — nicht in einem Kästchen daneben, das behauptet, so werde es
 * später aussehen.
 *
 * <b>Jede Wahl geht sofort an den Server.</b> Kein „Speichern" am Fuß: Es gibt
 * hier nichts, was man in Kombination entscheidet, und ein Formular mit sechs
 * Auswahlreihen und einem Knopf ganz unten ist am Handy ein Formular, dessen
 * Knopf man nicht findet. Der Name ist die Ausnahme — er wird getippt, und
 * getippter Text braucht ein Ende.
 */
@Composable
fun ProfilSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    konto: Konto? = null,
    profil: Bereichsstand<Profil?> = Bereichsstand(),
    server: String = "",
    /**
     * Was im Shop gekauft wurde — die `stueckId`s aus dem Besitzstand.
     *
     * <b>Leer heißt gesperrt, und das ist richtig so:</b> Nicht geladen ist nicht
     * dasselbe wie besessen, und ein Stück fälschlich offen anzubieten endet in
     * einer Fehlermeldung des Servers, nachdem man es angetippt hat.
     */
    besitzt: Set<String> = emptySet(),
    profilbild: Bereichsstand<Profilbildstand?> = Bereichsstand(),
    laeuft: Boolean = false,
    beiLaden: () -> Unit = {},
    beiAendern: (Profilaenderung) -> Unit = {},
    beiBildLaden: () -> Unit = {},
    beiBildEinreichen: (String, String, ByteArray) -> Unit = { _, _, _ -> },
    beiBildEntfernen: () -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(Unit) { beiLaden() }

    val stufe = konto?.level ?: 0
    val premium = konto?.premiumAktiv == true

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Profil",
            unterzeile = "Was andere von dir sehen",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Bereich(
            laedt = profil.laedt,
            fehler = profil.fehler,
            inhalt = profil.inhalt,
            beiErneut = beiLaden,
        ) { p ->
            Profilbanner(
                kennung = p.kennung,
                anzeigename = p.anzeigename,
                benutzername = p.benutzername,
                augenbraue = "Dein Profil",
                wappen = p.wappen,
                wappenfarbe = p.wappenfarbe,
                kopfmuster = p.kopfmuster,
                profilrahmen = p.profilrahmen,
                bildAdresse = bildweg(server, p.profilbild),
                premium = p.premium,
                teammitglied = p.teammitglied,
                marken = listOfNotNull(
                    p.rang?.let { "$it · Stufe ${p.level ?: 0}" },
                    p.gemeinschaft,
                    p.titel?.takeIf { it.isNotBlank() },
                ),
                werte = listOfNotNull(
                    p.schichten?.let { "Schichten" to it.toString() },
                    p.einsaetze?.let { "Einsätze" to zahl(it) },
                    "Dabei seit" to tag(p.dabeiSeit),
                ),
            )

            Namensfeld(p.anzeigename, laeuft) { beiAendern(Profilaenderung(anzeigename = it)) }

            Vorstellungsfeld(p.vorstellung.orEmpty(), laeuft) {
                beiAendern(Profilaenderung(vorstellung = it))
            }

            Abschnitt("Wappenzeichen") {
                SehrLeise("Ein Zeichen ersetzt deine Initialen im Wappen.")
                Stueckreihe(
                    stuecke = Schmuck.WAPPENZEICHEN,
                    stufe = stufe,
                    premium = premium,
                    besitzt = besitzt,
                    gewaehlt = p.wappen,
                    beiWahl = { beiAendern(Profilaenderung(wappen = it)) },
                )
            }

            Abschnitt("Wappenfarbe") {
                SehrLeise(
                    "Ohne eigene Wahl entscheidet deine Kennung — dann siehst du überall " +
                        "dieselbe Farbe.",
                )
                Farbreihe(
                    gewaehlt = p.wappenfarbe,
                    beiWahl = { beiAendern(Profilaenderung(wappenfarbe = it)) },
                )
            }

            Abschnitt("Rahmen") {
                SehrLeise("Der Ring ums Wappen — erspielt, gekauft oder am Abo.")
                Stueckreihe(
                    stuecke = Schmuck.RAHMEN,
                    stufe = stufe,
                    premium = premium,
                    besitzt = besitzt,
                    gewaehlt = p.profilrahmen,
                    beiWahl = { beiAendern(Profilaenderung(profilrahmen = it)) },
                )
            }

            Abschnitt("Kopfmuster") {
                SehrLeise("Das Band hinter Wappen und Name — hier oben und in jeder Liste.")
                Musterreihe(
                    stufe = stufe,
                    premium = premium,
                    besitzt = besitzt,
                    kennung = p.kennung,
                    wappenfarbe = p.wappenfarbe,
                    gewaehlt = p.kopfmuster,
                    beiWahl = { beiAendern(Profilaenderung(kopfmuster = it)) },
                )
            }

            Bildabschnitt(
                stand = profilbild,
                server = server,
                laeuft = laeuft,
                beiLaden = beiBildLaden,
                beiEinreichen = beiBildEinreichen,
                beiEntfernen = beiBildEntfernen,
            )
        }
    }
}

/**
 * Das Profilbild — einreichen, warten, zurücknehmen.
 *
 * <b>Ein Bild wird nicht gesetzt, sondern eingereicht.</b> Zwischen dem
 * Hochladen und dem Sichtbarwerden liegt eine Freigabe durch die Verwaltung.
 * Das steht hier auch so da: Wer sein Bild wählt und danach das alte sieht,
 * hält die App sonst für kaputt.
 *
 * <b>Die Grenzen kommen vom Server</b> (Größe, kürzeste Kante, erlaubte Typen)
 * und stehen dabei, bevor jemand wählt — nicht als Fehlermeldung danach.
 */
@Composable
private fun Bildabschnitt(
    stand: Bereichsstand<Profilbildstand?>,
    server: String,
    laeuft: Boolean,
    beiLaden: () -> Unit,
    beiEinreichen: (String, String, ByteArray) -> Unit,
    beiEntfernen: () -> Unit,
) {
    LaunchedEffect(Unit) { beiLaden() }

    val zusammenhang = LocalContext.current
    val waehler = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { ziel ->
        if (ziel == null) return@rememberLauncherForActivityResult

        // Der Inhalt kommt über den Anbieter, nicht über einen Dateipfad: Eine
        // Auswahl aus der Galerie ist eine `content://`-Adresse, hinter der
        // ebenso gut eine Cloud stecken kann. `openInputStream` ist der einzige
        // Weg, der in beiden Fällen trägt.
        val typ = zusammenhang.contentResolver.getType(ziel) ?: "image/jpeg"
        val daten = runCatching {
            zusammenhang.contentResolver.openInputStream(ziel)?.use { it.readBytes() }
        }.getOrNull()

        if (daten != null) beiEinreichen(dateiname(typ), typ, daten)
    }

    Abschnitt("Profilbild") {
        val b = stand.inhalt

        if (b?.wartet == true) {
            Kasten(marke = true, wartet = true, abstandInnen = Abstand.Klein) {
                Text("Wird geprüft", style = Schrift.Gross, color = Farben.Text)
                SehrLeise(
                    "Dein Bild liegt bei der Verwaltung. Bis zur Freigabe bleibt das " +
                        "bisherige stehen — eingereicht ${tag(b.eingereichtUm)}.",
                )
            }
        }

        if (b?.abgelehnt == true) {
            Kasten(abstandInnen = Abstand.Klein) {
                Text("Nicht freigegeben", style = Schrift.Gross, color = Farben.SignalHell)
                SehrLeise(b.grund ?: "Ohne Angabe eines Grundes.")
            }
        }

        SehrLeise(
            buildString {
                append("Ein eigenes Bild wird von der Verwaltung freigegeben, bevor es ")
                append("sichtbar wird.")
                if (b != null && b.maxBytes > 0) {
                    append(" Höchstens ${b.maxBytes / 1024} KB")
                    if (b.minKante > 0) append(", mindestens ${b.minKante} Punkte Kante")
                    append(".")
                }
                if (b != null && b.erlaubteTypen.isNotEmpty()) {
                    append(" Erlaubt: ${b.erlaubteTypen.joinToString(", ") { kurzform(it) }}.")
                }
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                aufschrift = if (b?.bild != null) "Anderes Bild wählen" else "Bild wählen",
                beiDruck = { waehler.launch("image/*") },
                aktiv = !laeuft,
                kompakt = true,
            )
            if (b?.bild != null || b?.eingereicht != null) {
                Knopf(
                    "Bild entfernen",
                    beiEntfernen,
                    art = Knopfart.Gefahr,
                    aktiv = !laeuft,
                    kompakt = true,
                )
            }
        }
    }
}

/**
 * Ein Dateiname für den Upload.
 *
 * <b>Der echte Name der Datei geht niemanden etwas an.</b> Er steht bei einer
 * Galerie-Auswahl ohnehin nicht zuverlässig zur Verfügung, und er trägt
 * regelmäßig mehr Auskunft über den Nutzer, als das Hochladen eines Bildes
 * verlangt („IMG_2024_Hochzeit_Oma.jpg"). Der Server braucht nur die Endung.
 */
private fun dateiname(inhaltstyp: String): String = "profilbild.${kurzform(inhaltstyp)}"

/** Aus `image/jpeg` wird `jpg`. */
private fun kurzform(inhaltstyp: String): String = when (inhaltstyp.substringAfter("/")) {
    "jpeg" -> "jpg"
    "svg+xml" -> "svg"
    else -> inhaltstyp.substringAfter("/")
}

/**
 * Der Anzeigename.
 *
 * <b>Er hat als einziges Feld einen Knopf.</b> Getippter Text hat kein
 * natürliches Ende — bei jeder Taste zu speichern hieße, bei „Ki" einen Namen zu
 * setzen, den niemand haben wollte.
 */
@Composable
private fun Namensfeld(name: String, laeuft: Boolean, beiSetzen: (String) -> Unit) {
    var wert by remember(name) { mutableStateOf(name) }

    Abschnitt("Name im Funk") {
        Feld(
            wert = wert,
            beiAenderung = { wert = it.take(24) },
            platzhalter = "z. B. Kim",
            weiterTaste = ImeAction.Done,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf(
                aufschrift = "Übernehmen",
                beiDruck = { beiSetzen(wert) },
                aktiv = !laeuft && wert.isNotBlank() && wert != name,
                kompakt = true,
            )
        }
    }
}

/** Die Vorstellung — der Satz, der im Profil unter dem Namen steht. */
@Composable
private fun Vorstellungsfeld(text: String, laeuft: Boolean, beiSetzen: (String) -> Unit) {
    var wert by remember(text) { mutableStateOf(text) }

    Abschnitt("Über dich") {
        Feld(
            wert = wert,
            beiAenderung = { wert = it.take(280) },
            platzhalter = "Zwei Sätze über dich — was du fährst, wo du bist.",
            einzeilig = false,
            weiterTaste = ImeAction.Default,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Knopf(
                aufschrift = "Übernehmen",
                beiDruck = { beiSetzen(wert) },
                aktiv = !laeuft && wert != text,
                kompakt = true,
            )
            SehrLeise("${wert.length}/280", mono = true)
        }
    }
}

/**
 * Eine Reihe aus Schmuckstücken.
 *
 * <b>Was noch nicht zu haben ist, steht trotzdem da — und sagt, warum.</b> Ein
 * Rahmen ab Stufe 48 ist ein Ziel; weggelassen wäre er ein Geheimnis, ausgegraut
 * ohne Erklärung ein Fehler. Und der Grund ist nicht immer die Stufe: Die
 * meisten Muster und Rahmen sind **gekauft**, nicht erspielt — an denen steht
 * ihr Preis, nicht eine Stufe, die nichts bedeutet.
 */
@Composable
private fun Stueckreihe(
    stuecke: List<Schmuck.Stueck>,
    stufe: Int,
    premium: Boolean,
    besitzt: Set<String>,
    gewaehlt: String,
    beiWahl: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
    ) {
        stuecke.forEach { stueck ->
            // Was man trägt, gilt als offen — ohne Rückfrage beim Shop.
            //
            // <b>Der Besitzstand ist nicht die ganze Wahrheit.</b> `imBesitz`
            // führt, was im Shop gekauft wurde; ein Stück kann aber auch aus
            // einem Gutschein, einer Aktion oder einer früheren Freischaltung
            // stammen. Nachgemessen am eigenen Konto: Das getragene Kopfmuster
            // stand als „180 C" gesperrt da, obwohl es eine Zeile höher im
            // Banner lief. Wer etwas trägt, besitzt es — alles andere ist eine
            // Auswahl, die dem Nutzer widerspricht.
            val offen = stueck.id == gewaehlt || stueck.offen(stufe, premium, besitzt)
            Pille(
                aufschrift = if (offen) stueck.name else "${stueck.name} · ${stueck.sperrgrund()}",
                an = stueck.id == gewaehlt,
                beiDruck = { beiWahl(stueck.id) },
                aktiv = offen,
            )
        }
    }
}

/** Die dreizehn Wappenfarben — als Kreise, denn sie sind selbst die Auskunft. */
@Composable
private fun Farbreihe(gewaehlt: Int, beiWahl: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
    ) {
        // Die Null ist „automatisch" und steht als erste. Sie ist keine Farbe,
        // sondern der Verzicht auf die Wahl — deshalb trägt sie ein Wort.
        Marke(
            text = "Automatisch",
            farbe = if (gewaehlt == 0) Farben.Amber else Farben.TextLeise,
            modifier = Modifier.clickable(
                onClick = { beiWahl(0) },
                role = Role.RadioButton,
                indication = null,
                interactionSource = null,
            ),
        )

        Wappen.PALETTE.forEachIndexed { nr, farbe ->
            val wert = nr + 1
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(farbe, CircleShape)
                    .border(
                        width = if (wert == gewaehlt) 3.dp else 1.dp,
                        color = if (wert == gewaehlt) Farben.Amber else Farben.Rand,
                        shape = CircleShape,
                    )
                    .clickable(
                        onClick = { beiWahl(wert) },
                        role = Role.RadioButton,
                        indication = null,
                        interactionSource = null,
                    ),
            )
        }
    }
}

/**
 * Die Kopfmuster — jedes als Kachel, die sich selbst zeigt.
 *
 * <b>Ein Muster als Wort in einer Liste ist keine Auswahl.</b> „Sparren",
 * „Topografie", „Flecktarn" sagen einem, der sie nicht kennt, gar nichts. Jede
 * Kachel zeichnet deshalb ihr eigenes Band — im eigenen Wappenton, damit die
 * Wahl aussieht wie das Ergebnis.
 */
@Composable
private fun Musterreihe(
    stufe: Int,
    premium: Boolean,
    besitzt: Set<String>,
    kennung: String,
    wappenfarbe: Int,
    gewaehlt: String,
    beiWahl: (String) -> Unit,
) {
    val ton = Wappen.ton(kennung, wappenfarbe)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
    ) {
        Schmuck.KOPFMUSTER.forEach { muster ->
            // Siehe `Stueckreihe`: Was man trägt, gilt als offen.
            val offen = muster.id == gewaehlt || muster.offen(stufe, premium, besitzt)

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 52.dp)
                        .background(Farben.Flaeche, Rundung.Klein)
                        .kopfband(muster.id, ton)
                        .border(
                            width = if (muster.id == gewaehlt) 2.dp else 1.dp,
                            color = if (muster.id == gewaehlt) Farben.Amber else Farben.Rand,
                            shape = Rundung.Klein,
                        )
                        .clickable(
                            enabled = offen,
                            onClick = { beiWahl(muster.id) },
                            role = Role.RadioButton,
                            indication = null,
                            interactionSource = null,
                        ),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    if (!offen) {
                        Text(
                            text = muster.sperrgrund(),
                            style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                            color = Farben.TextSehrLeise,
                            modifier = Modifier.padding(Abstand.Winzig),
                        )
                    }
                }
                Text(
                    text = muster.name,
                    style = Schrift.Winzig,
                    color = if (muster.id == gewaehlt) Farben.Amber else Farben.TextLeise,
                )
            }
        }
    }
}

/** Wo das Profilbild liegt — der Pfad steht an genau einer Stelle. */
internal fun bildweg(server: String, dateiname: String?): String? =
    de.pagerspass.pagerspass.ui.schmuck.profilbildAdresse(server, dateiname)
