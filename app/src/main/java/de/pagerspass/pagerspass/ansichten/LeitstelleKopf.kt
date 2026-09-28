package de.pagerspass.pagerspass.ansichten

import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import de.pagerspass.pagerspass.mobil.Leitstellenstand
import de.pagerspass.pagerspass.mobil.Rundenstand
import de.pagerspass.pagerspass.mobil.Tonstand
import de.pagerspass.pagerspass.mobil.rememberTonstand
import de.pagerspass.pagerspass.netz.Anruf
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Raumzustand
import de.pagerspass.pagerspass.netz.Rundenfahrzeug
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Hakenzeile
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Regler
import de.pagerspass.pagerspass.ui.bausteine.Schalterzeile
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Ueberschrift
import de.pagerspass.pagerspass.ui.bausteine.Zeichenknopf
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * Der Kopf der Leitstelle und die Bänder darunter — `.dienstleiste` und die
 * Streifen aus `LeitstelleView.vue`.
 *
 * <b>Der Kopf trägt, was für die ganze Schicht gilt:</b> Name und Sitz der
 * Leitstelle, das Wetter (es bremst jede Anfahrt im Kreis), die drei Zähler und
 * die Rundenknöpfe. <b>Die Bänder tragen, was jetzt eilt</b> — und verschwinden
 * wieder, wenn es erledigt ist.
 */
@Composable
internal fun Leitstellenkopf(
    stand: Rundenstand,
    raum: Raumzustand,
    katalog: Katalog?,
    konto: Konto?,
    daten: Leitstellenstand,
    griffe: LeitstellenGriffe,
    beiNeuerEinsatz: () -> Unit,
) {
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val ton by rememberTonstand()

    var tonOffen by remember { mutableStateOf(false) }
    var begleiterOffen by remember { mutableStateOf(false) }
    var premiumHinweis by remember { mutableStateOf(false) }
    var warnungOffen by remember { mutableStateOf(false) }
    var besatzungOffen by remember { mutableStateOf(false) }
    var verlassenGefragt by remember { mutableStateOf(false) }
    var dienstendeGefragt by remember { mutableStateOf(false) }

    val offene = raum.incidents.filter { !it.abgeschlossen }
    val undisponiert = offene.count { it.state == "Offen" }
    val unquittiert = raum.vehicles.count { it.alarmOffen }
    val frei = raum.vehicles.count { it.status == 1 || it.status == 2 }
    val gefahrgutOffen = offene.any { it.gefahrgutlage }
    val freieVergabe = raum.settings.mode == "Frei"
    val menschen = raum.players.count { !it.istBot }
    val kannVerlassen = menschen > 1

    val mitkreise = raum.settings.mitkreise.mapNotNull { id ->
        katalog?.landkreise?.firstOrNull { it.id == id }?.name
    }

    val schwelle = raum.dienstendeSchwelle.coerceAtLeast(1)
    val stimmen = raum.dienstendeStimmen
    val eigeneStimme = raum.dienstendeEigeneStimme
    val zeigeStimmstand = schwelle > 1 && stimmen > 0

    val bremse = raum.wetter.bremsfaktor.let { f ->
        if (f >= 1.0) null else "−${((1 - f) * 100).roundToInt()} %"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Farben.FlaecheHoch, Farben.Flaeche)))
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(
                    color = Farben.Rand,
                    start = Offset(0f, size.height - strich / 2f),
                    end = Offset(size.width, size.height - strich / 2f),
                    strokeWidth = strich,
                )
            }
            .padding(top = oben)
            .padding(horizontal = Abstand.Gross, vertical = Abstand.Klein),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.size(8.dp).background(Farben.GruenHell, CircleShape))
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier.weight(1f),
            ) {
                // Der Sitz gehört an den Namen und nicht in eine Nebenzeile: „ILS
                // Bodensee-Oberschwaben" verrät nicht, dass sie in Weingarten sitzt.
                Text(
                    text = listOfNotNull(
                        raum.settings.leitstelle ?: "Leitstelle",
                        raum.settings.leitstellensitz?.let { "Sitz $it" },
                    ).joinToString(" · "),
                    style = Schrift.MonoNormal.copy(fontWeight = FontWeight.Bold),
                    color = Farben.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SehrLeise(
                    listOfNotNull(
                        raum.code,
                        Rundentexte.modus(raum.settings.mode),
                        mitkreise.takeIf { it.isNotEmpty() }?.let { "mit ${it.joinToString(", ")}" },
                        // Der Wind zählt nur für Gefahrgutlagen — sonst ist er Rauschen.
                        raum.settings.windText.takeIf { raum.settings.gefahrgutlagen && gefahrgutOffen }
                            ?.let { "Wind aus $it" },
                    ).joinToString(" · "),
                    mono = true,
                )
            }
            Knopf("?", griffe.hilfe, art = Knopfart.Leise, kompakt = true)
        }

        // Das Wetter steht im Kopf und nicht in einer Nebenzeile: Es bremst jede
        // Anfahrt, und wer nicht weiß, warum das HLF sechs statt vier Minuten
        // braucht, hält es für einen Fehler. Die Bremse steht deshalb als Zahl da.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
        ) {
            Text(
                text = "${wetterZeichen(raum.wetter.aktuell)} ${raum.wetter.aktuellText.ifBlank { Rundentexte.wetter(raum.wetter.aktuell) }}",
                style = Schrift.Klein,
                color = if (raum.wetter.welle) Farben.OrangeHell else Farben.TextLeise,
            )
            bremse?.let { Text("Anfahrt $it", style = Schrift.MonoKlein, color = Farben.AmberHell) }
            if (raum.wetter.welle) Text("Unwetterlage", style = Schrift.MonoKlein, color = Farben.OrangeHell)
            raum.wetter.angekuendigtText?.takeIf { it.isNotBlank() }?.let {
                Text("⚠ $it in Kürze", style = Schrift.MonoKlein, color = Farben.SignalHell)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Zaehler(undisponiert, "offen", warn = undisponiert > 0)
            Zaehler(unquittiert, "unquittiert", warn = unquittiert > 0)
            Zaehler(frei, "frei", warn = false)
            Zuschauerzaehler(raum)
        }

        // Zeichen statt Wörter: fünf gleich große Felder stehen still, fünf
        // verschieden lange Wörter brächen je nach Modus anders um.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            Zeichenknopf(
                beiDruck = { griffe.tonSetzen(ton.copy(stumm = !ton.stumm)) },
                beschreibung = if (ton.stumm) {
                    "Stumm aufheben — außer dem Melder ist alles stumm"
                } else {
                    "Alles außer dem Melder stummschalten"
                },
                art = if (ton.stumm) Knopfart.Gefahr else Knopfart.Normal,
                kompakt = true,
            ) {
                Icon(
                    if (ton.stumm) Leitstellenzeichen.Stumm else Leitstellenzeichen.Laut,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Zeichenknopf({ tonOffen = true }, "Toneinstellungen", kompakt = true) {
                Icon(Leitstellenzeichen.Regler, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            if (raum.laeuft) {
                val premium = stand.ich?.premium == true
                Zeichenknopf(
                    beiDruck = {
                        if (premium) {
                            begleiterOffen = true
                            griffe.begleiterErzeugen()
                        } else {
                            premiumHinweis = true
                        }
                    },
                    beschreibung = if (premium) {
                        "Handy als Funkgerät und Melder koppeln"
                    } else {
                        "Handy als Funkgerät und Melder koppeln — gehört zu Premium"
                    },
                    art = if (stand.begleiterGekoppelt) Knopfart.Haupt else Knopfart.Leise,
                    kompakt = true,
                ) {
                    Icon(Leitstellenzeichen.Begleiter, contentDescription = null, modifier = Modifier.size(18.dp))
                    if (!premium) Text("★", style = Schrift.Winzig, color = Farben.AmberHell)
                }
            }

            // Notruf und Würfeln gehören der Freien Vergabe — im Zufallsmodus
            // erzeugt der Server die Lagen selbst.
            if (freieVergabe) {
                Zeichenknopf(beiNeuerEinsatz, "Neuen Einsatz aufnehmen", art = Knopfart.Haupt, kompakt = true) {
                    Icon(Leitstellenzeichen.NeuerEinsatz, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Zeichenknopf(griffe.wuerfeln, "Einsatz auswürfeln", kompakt = true) {
                    Icon(Leitstellenzeichen.Wuerfel, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Zeichenknopf({ warnungOffen = true }, "Bevölkerung warnen", art = Knopfart.Leise, kompakt = true) {
                Icon(Leitstellenzeichen.Warnung, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Zeichenknopf({ besatzungOffen = true }, "Besatzungen einteilen", art = Knopfart.Leise, kompakt = true) {
                Icon(Leitstellenzeichen.Besatzung, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            // In einer besetzten Runde geht nur der eigene Platz frei. Allein
            // beendet derselbe Weg den Dienst über den Knopf daneben.
            if (kannVerlassen) {
                Zeichenknopf({ verlassenGefragt = true }, "Leitstelle verlassen", art = Knopfart.Leise, kompakt = true) {
                    Icon(Leitstellenzeichen.Verlassen, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            // Das Dienstende ist eine Abstimmung. Entscheidet dieser Druck die
            // Schicht, wird vorher gefragt; eine Stimme, die nur zählt, und das
            // Zurücknehmen gehen ohne Umweg durch.
            Zeichenknopf(
                beiDruck = {
                    val entscheidet = !eigeneStimme && stimmen + 1 >= schwelle
                    if (entscheidet) dienstendeGefragt = true else griffe.dienstende()
                },
                beschreibung = dienstendeTitel(raum.istPruefung, schwelle, stimmen, eigeneStimme),
                art = if (eigeneStimme) Knopfart.Haupt else Knopfart.Leise,
                kompakt = true,
            ) {
                Icon(Leitstellenzeichen.Dienstende, contentDescription = null, modifier = Modifier.size(18.dp))
                if (raum.istPruefung) Text("Prüfung abschließen", style = Schrift.Klein)
                if (zeigeStimmstand) Text("$stimmen/$schwelle", style = Schrift.MonoKlein)
            }
        }
    }

    if (tonOffen) {
        LeitstellenTonblende(
            ton = ton,
            einzelrufZulassen = stand.ich?.einzelrufZulassen ?: true,
            griffe = griffe,
            beiSchliessen = { tonOffen = false },
        )
    }

    if (begleiterOffen) {
        Begleiterblende(
            stand = stand,
            daten = daten,
            griffe = griffe,
            beiSchliessen = { begleiterOffen = false },
        )
    }

    if (premiumHinweis) {
        Blende(
            titel = "Dein Handy wird zum Melder",
            beiSchliessen = { premiumHinweis = false },
            breite = Dialogbreite.Schmal,
            fuss = { Knopf("Verstanden", { premiumHinweis = false }, art = Knopfart.Haupt) },
        ) {
            Text(
                "Scanne einen QR-Code, und das Handy neben dir ist Funkgerät und Piepser " +
                    "deines Platzes — ohne zweite Anmeldung, ohne zweites Konto.",
                style = Schrift.Normal,
                color = Farben.TextLeise,
            )
            SehrLeise("• Der Melder piepst am Handy, auch bei dunklem Bildschirm.")
            SehrLeise("• Sprechen und Hören über ein echtes Handfunkgerät — fünf Ausführungen zur Wahl.")
            SehrLeise("• Der Funkbereich hier verschwindet dafür und macht der Arbeit Platz.")
            Leise("Der Funkbegleiter gehört zu Premium.")
        }
    }

    if (warnungOffen) {
        Warnungblende(griffe = griffe, beiSchliessen = { warnungOffen = false })
    }

    if (besatzungOffen) {
        Besatzungblende(
            raum = raum,
            katalog = katalog,
            eigeneKennung = stand.eigeneKennung,
            griffe = griffe,
            beiSchliessen = { besatzungOffen = false },
        )
    }

    if (verlassenGefragt) {
        Blende(
            titel = "Leitstelle verlassen?",
            beiSchliessen = { verlassenGefragt = false },
            breite = Dialogbreite.Schmal,
            fuss = {
                Knopf("In der Leitstelle bleiben", { verlassenGefragt = false }, art = Knopfart.Leise)
                Knopf(
                    "Leitstelle verlassen",
                    {
                        verlassenGefragt = false
                        griffe.verlassen()
                    },
                    art = Knopfart.Alarm,
                )
            },
        ) {
            Etikett("Einsatznetz")
            Text(
                "Dein Leitstellenplatz wird frei. Die laufende Schicht und alle Einsätze bleiben " +
                    "für die übrigen Besatzungen bestehen.",
                style = Schrift.Normal,
                color = Farben.Text,
            )
            SehrLeise(
                "Was du bis hierher gefahren hast, wird beim Aussteigen gutgeschrieben. " +
                    "Was danach passiert, zählt für dich nicht mehr.",
            )
        }
    }

    if (dienstendeGefragt) {
        Dienstendeblende(
            raum = raum,
            schwelle = schwelle,
            beiWeiter = { dienstendeGefragt = false },
            beiBeenden = {
                dienstendeGefragt = false
                griffe.dienstende()
            },
        )
    }
}

/** Ein Zähler im Kopf — die Zahl fett, amber, sobald etwas wartet. */
@Composable
private fun Zaehler(zahl: Int, wort: String, warn: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig), verticalAlignment = Alignment.CenterVertically) {
        Text(
            zahl.toString(),
            style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
            color = if (warn) Farben.Amber else Farben.Text,
        )
        Text(wort, style = Schrift.Klein, color = if (warn) Farben.AmberHell else Farben.TextSehrLeise)
    }
}

/** Der Name des Dienstende-Knopfs — `dienstendeTitel` im Store. */
private fun dienstendeTitel(pruefung: Boolean, schwelle: Int, stimmen: Int, eigene: Boolean): String {
    if (pruefung) return "Prüfung abschließen"
    if (schwelle <= 1) return "Dienst beenden"
    val stand = "$stimmen von $schwelle"
    return if (eigene) {
        "Du hast fürs Dienstende gestimmt ($stand) — noch einmal drücken nimmt die Stimme zurück"
    } else {
        "Fürs Dienstende stimmen ($stand nötig)"
    }
}

/**
 * „Dienst beenden?" — `DienstendeDialog.vue`, gefragt nur, wenn dieser Druck die
 * Schicht entscheidet. Der Preis steht rot darunter, wenn es einen gibt.
 */
@Composable
private fun Dienstendeblende(
    raum: Raumzustand,
    schwelle: Int,
    beiWeiter: () -> Unit,
    beiBeenden: () -> Unit,
) {
    val pruefung = raum.istPruefung
    val zuKurz = zeitMillis(raum.gestartetUm)?.let {
        raum.laeuft && System.currentTimeMillis() - it < 5 * 60 * 1000L
    } == true

    Blende(
        titel = if (pruefung) "Prüfung abschließen?" else "Dienst beenden?",
        beiSchliessen = beiWeiter,
        breite = Dialogbreite.Schmal,
        fuss = {
            Knopf("Weiter im Dienst", beiWeiter, art = Knopfart.Leise)
            Knopf(if (pruefung) "Prüfung abschließen" else "Dienst beenden", beiBeenden, art = Knopfart.Alarm)
        },
    ) {
        Etikett("Dienstende")
        Text(
            text = when {
                pruefung -> "Die Prüfung wird abgeschlossen und bewertet. Weiterfahren geht danach nicht mehr."
                schwelle > 1 -> "Deine Stimme ist die letzte, die fehlt — die Schicht endet damit für alle im Raum."
                else -> "Die Schicht endet, und alle Einsätze werden abgerechnet."
            },
            style = Schrift.Normal,
            color = Farben.Text,
        )
        if (pruefung && !raum.pruefungAbschlussbereit) {
            Text(
                "Noch nicht alle Prüfungslagen sind vollständig abgearbeitet.",
                style = Schrift.MonoKlein,
                color = Farben.SignalHell,
            )
        } else if (zuKurz) {
            Text(
                "Die Schicht läuft noch keine fünf Minuten und bringt bis dahin keine Punkte.",
                style = Schrift.MonoKlein,
                color = Farben.SignalHell,
            )
        }
    }
}

/**
 * Die Toneinstellungen im Dienst — `Tonregler.vue` mit `leitstelle`.
 *
 * Was man während einer Schicht wirklich anfasst: lauter, leiser, still. Der
 * Ruhe-Schalter des Einzelrufs liegt am Server, weil die Vermittlung ihn prüft.
 */
@Composable
private fun LeitstellenTonblende(
    ton: Tonstand,
    einzelrufZulassen: Boolean,
    griffe: LeitstellenGriffe,
    beiSchliessen: () -> Unit,
) {
    Blende(
        titel = "Toneinstellungen",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = { Knopf("Fertig", beiSchliessen, art = Knopfart.Haupt) },
    ) {
        Schalterzeile(
            titel = "Alles außer dem Melder stumm",
            an = ton.stumm,
            beiWechsel = { griffe.tonSetzen(ton.copy(stumm = it)) },
        )
        Etikett("Lautstärke")
        Regler(
            wert = (ton.melder * 100).roundToInt(),
            beiAenderung = { griffe.tonSetzen(ton.copy(melder = it / 100f)) },
            von = 0,
            bis = 100,
            etikett = "Melder ${(ton.melder * 100).roundToInt()} %",
        )
        Regler(
            wert = (ton.funk * 100).roundToInt(),
            beiAenderung = { griffe.tonSetzen(ton.copy(funk = it / 100f)) },
            von = 0,
            bis = 100,
            etikett = "Funk ${(ton.funk * 100).roundToInt()} %",
            aktiv = !ton.stumm,
        )
        SehrLeise("Stimmen und Gerätetöne des Funkgeräts — auch das Klingeln von Telefon und Einzelruf.")

        Hakenzeile(
            "Einzelrufe von Fahrzeugen annehmen",
            einzelrufZulassen,
            { griffe.einzelrufZulassen(it) },
        )
        SehrLeise("Der Ruhe-Schalter des eigenen Platzes — die Leitstelle kommt immer durch.")

        Hakenzeile(
            "Durchsage der Leitstelle beim Alarm",
            ton.durchsage,
            { griffe.tonSetzen(ton.copy(durchsage = it)) },
        )
        Hakenzeile(
            "Einsatzstellenfunk (DMO) mithören",
            !ton.dmoStumm,
            { griffe.tonSetzen(ton.copy(dmoStumm = !it)) },
        )
        SehrLeise("Der Alarmton wird im Konto unter „Profil → Bearbeiten → Dein Melder“ gewählt.")
    }
}

/**
 * Der Handy-Funkbegleiter — `BegleiterDialog.vue`.
 *
 * Auch am Handy sinnvoll: Wer die Leitstelle am Tablet fährt, legt Funk und
 * Melder auf ein zweites Gerät. Der QR-Code öffnet dort den Begleiter, ohne
 * zweite Anmeldung; der Haken darunter blendet Funk und Einzelruf hier aus,
 * sobald das Handy gekoppelt ist.
 */
@Composable
private fun Begleiterblende(
    stand: Rundenstand,
    daten: Leitstellenstand,
    griffe: LeitstellenGriffe,
    beiSchliessen: () -> Unit,
) {
    val zwischenablage = LocalClipboardManager.current
    var kopiert by remember { mutableStateOf(false) }
    val code = stand.raum?.code
    val ausgelagert = code != null && daten.ausgelagertFuer == code

    // Die Kopplung hält der Server — solange die Blende steht, wird nachgefragt.
    LaunchedEffect(Unit) {
        while (isActive) {
            griffe.begleiterPruefen()
            delay(3_000)
        }
    }

    Blende(
        titel = "Handy-Funkbegleiter",
        beiSchliessen = beiSchliessen,
        breite = Dialogbreite.Schmal,
        fuss = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise) },
    ) {
        Etikett("Premium")
        Text(
            "Mit dem Handy scannen: Funkgerät, Funkchat und Melder öffnen sich direkt — ohne " +
                "erneute Anmeldung. Der Zugang gilt nur für deinen Platz in dieser Runde.",
            style = Schrift.Normal,
            color = Farben.TextLeise,
        )
        val b = daten.begleiter
        when {
            b.laedt -> SehrLeise("QR-Code wird erzeugt …")
            b.fehler != null -> Text(b.fehler, style = Schrift.MonoKlein, color = Farben.SignalHell)
            b.link != null -> {
                val bild = remember(b.link) { qrBildVon(b.link) }
                if (bild != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Image(
                            bitmap = bild,
                            contentDescription = "QR-Code des Funkbegleiters",
                            modifier = Modifier.size(220.dp).background(Color.White, Rundung.Klein).padding(8.dp),
                        )
                    }
                }
                Text(
                    if (stand.begleiterGekoppelt) "Handy gekoppelt ✓" else "Warte auf die Kopplung …",
                    style = Schrift.MonoKlein,
                    color = if (stand.begleiterGekoppelt) Farben.GruenHell else Farben.TextSehrLeise,
                )
                Knopf(
                    if (kopiert) "Link kopiert ✓" else "Direkten Link kopieren",
                    {
                        zwischenablage.setText(AnnotatedString(b.link))
                        kopiert = true
                    },
                    art = Knopfart.Haupt,
                    breit = true,
                )
                Hakenzeile(
                    "Funk und Einzelruf hier ausblenden, sobald das Handy gekoppelt ist — beides " +
                        "liegt dann auf dem Handy, der Platz gehört dem Arbeitsplatz",
                    ausgelagert,
                    { griffe.begleiterAuslagern(it) },
                )
                SehrLeise("Der Link verfällt nach acht Stunden. Teile ihn nicht weiter.")
            }
        }
    }
}

/** Ein QR-Code als Bild — `QrCode.vue`. */
private fun qrBildVon(inhalt: String): ImageBitmap? = runCatching {
    val matrix = QRCodeWriter().encode(
        inhalt,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(EncodeHintType.MARGIN to 1),
    )
    val breite = matrix.width
    val hoehe = matrix.height
    val punkte = IntArray(breite * hoehe) { i ->
        if (matrix.get(i % breite, i / breite)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }
    Bitmap.createBitmap(punkte, breite, hoehe, Bitmap.Config.ARGB_8888).asImageBitmap()
}.getOrNull()

/**
 * Die Bevölkerungswarnung — der MoWaS-Knopf, ausdrücklich folgenlos fürs Spiel.
 *
 * Vier fertige Texte für den schnellen Griff, ein Feld für den eigenen. Die
 * Vorlagen senden sofort; die Abklingzeit des Servers fängt das Dauerfeuer ab.
 */
@Composable
private fun Warnungblende(griffe: LeitstellenGriffe, beiSchliessen: () -> Unit) {
    var eigener by remember { mutableStateOf("") }
    var gesendet by remember { mutableIntStateOf(0) }

    LaunchedEffect(gesendet) {
        if (gesendet > 0) {
            delay(3_000)
            gesendet = 0
        }
    }

    fun warnen(text: String) {
        val getippt = text.trim()
        if (getippt.isEmpty()) return
        griffe.warnen(getippt)
        eigener = ""
        gesendet += 1
    }

    Blende(
        titel = "Bevölkerung warnen",
        beiSchliessen = beiSchliessen,
        kopfknoepfe = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
    ) {
        Etikett("Nur zum Spaß — folgenlos fürs Spiel")
        Leise(
            "Die Warnung erscheint allen in dieser Runde als Warn-App-Meldung und steht im " +
                "Funkprotokoll. Sonst passiert nichts — versprochen.",
        )
        WARNVORLAGEN.forEach { v ->
            Text(
                text = v,
                style = Schrift.Klein,
                color = Farben.Text,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp)
                    .clickable { warnen(v) }
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            )
        }
        Etikett("Eigener Text")
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
        ) {
            Feld(
                wert = eigener,
                beiAenderung = { eigener = it.take(120) },
                platzhalter = "Die Bevölkerung wird gebeten, …",
                modifier = Modifier.weight(1f),
            )
            Knopf("Warnen", { warnen(eigener) }, aktiv = eigener.isNotBlank(), kompakt = true)
        }
        if (gesendet > 0) {
            Text("Warnung ist raus — sieh nach oben.", style = Schrift.MonoKlein, color = Farben.AmberHell)
        }
    }
}

/** Die Klassiker des Warntags — bewusst harmlos gehalten. */
private val WARNVORLAGEN = listOf(
    "Probewarnung — es besteht keine Gefahr. Dies ist nur ein Test.",
    "Sirenenprobe: Heute heulen die Sirenen. Bitte nicht wundern.",
    "Rauchentwicklung im Stadtgebiet — Fenster und Türen geschlossen halten.",
    "Entwarnung: Die Gefahr besteht nicht mehr. Danke für Ihre Aufmerksamkeit.",
)

/**
 * „Mannschaft verwalten" — `BesatzungDialog.vue`.
 *
 * Oben die Mitspieler (übergeben, hinauswerfen), darunter die Bot-Besatzungen
 * (versetzen, entfernen) und die Zeile, mit der eine neue dazukommt.
 */
@Composable
private fun Besatzungblende(
    raum: Raumzustand,
    katalog: Katalog?,
    eigeneKennung: String,
    griffe: LeitstellenGriffe,
    beiSchliessen: () -> Unit,
) {
    val kennung = rememberKennung(raum)
    val kennzahl = kennungIstKennzahl()
    val menschen = raum.players.filter { !it.istBot && it.id != eigeneKennung }
    val bots = raum.players.filter { it.istBot }
    val orgs = raum.settings.organisationen
    val hiOrgs = raum.settings.hiOrgs
    val vorlagen = katalog?.fahrzeuge.orEmpty().filter { it.organisation in orgs }
    fun gehoertZurSchicht(f: Fahrzeugvorlage) = f.hiOrg == "Keine" || hiOrgs.isEmpty() || f.hiOrg in hiOrgs
    val schicht = vorlagen.filter { gehoertZurSchicht(it) }
    val ueberoertlich = vorlagen.filter { !gehoertZurSchicht(it) }
    val gruppen: List<Pair<String?, List<Fahrzeugvorlage>>> =
        if (ueberoertlich.isNotEmpty()) {
            listOf("Träger der Schicht" to schicht, "Überörtlich anfordern" to ueberoertlich)
        } else {
            listOf(null to schicht)
        }

    var versetzenFuer by remember { mutableStateOf<String?>(null) }
    var neueVorlage by remember { mutableStateOf<Fahrzeugvorlage?>(null) }
    var vorlagenwahl by remember { mutableStateOf(false) }
    var anzahl by remember { mutableIntStateOf(1) }
    var gesperrtBis by remember { mutableStateOf(0L) }

    fun vorlagentext(f: Fahrzeugvorlage) =
        f.typ + if (f.hiOrg != "Keine") " · ${Rundentexte.traeger(f.hiOrg)}" else ""

    fun fahrzeugVon(id: String?): Rundenfahrzeug? = id?.let { v -> raum.vehicles.firstOrNull { it.id == v } }

    Blende(
        titel = "Mannschaft verwalten",
        beiSchliessen = beiSchliessen,
        kopfknoepfe = { Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, kompakt = true) },
    ) {
        Etikett("Besatzung")
        Ueberschrift("Mitspieler")
        if (menschen.isEmpty()) {
            SehrLeise("Keine weiteren Mitspieler im Raum.", mono = true)
        }
        menschen.forEach { p ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Box(
                    Modifier.size(8.dp).background(
                        if (p.verbunden) Farben.GruenHell else Farben.TextSehrLeise,
                        CircleShape,
                    ),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        p.name,
                        style = Schrift.Normal,
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SehrLeise(
                        when {
                            p.istLeitstelle -> "Leitstelle"
                            p.vehicleId != null -> fahrzeugVon(p.vehicleId)?.let(kennung) ?: "Fahrzeug"
                            else -> "wählt noch"
                        },
                        mono = true,
                    )
                }
                // Die Dienstübergabe: angeboten, nicht zugeschoben.
                if (p.verbunden && raum.laeuft) {
                    Knopf("Übergeben", { griffe.leitstelleUebergeben(p.id) }, kompakt = true)
                }
                Knopf("×", { griffe.kicken(p.id) }, art = Knopfart.Gefahr, kompakt = true)
            }
        }
        if (raum.uebergabe != null) {
            SehrLeise("Übergabe angeboten — wartet auf Antwort.")
        }

        Ueberschrift("Bot-Besatzungen")
        if (bots.isEmpty()) {
            SehrLeise("Noch keine Bot-Besatzungen eingeteilt.", mono = true)
        }
        bots.forEach { b ->
            val f = fahrzeugVon(b.vehicleId)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(farbe = Farben.FlaecheHoch, ecke = 9.dp)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            ) {
                Text("BOT", style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
                Column(Modifier.weight(1f)) {
                    Text(
                        f?.let(kennung) ?: "—",
                        style = Schrift.MonoNormal,
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (kennzahl && f != null) SehrLeise(f.typ)
                }
                Knopf("Versetzen …", { versetzenFuer = b.id }, art = Knopfart.Leise, kompakt = true)
                Knopf("×", { griffe.botEntfernen(b.id) }, art = Knopfart.Gefahr, kompakt = true)
            }
        }

        Wahlfeld(
            etikett = "Neue Bot-Besatzung",
            wert = neueVorlage?.let { vorlagentext(it) },
            platzhalter = "Fahrzeug wählen …",
            beiDruck = { vorlagenwahl = true },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Knopf("−", { anzahl = (anzahl - 1).coerceAtLeast(1) }, art = Knopfart.Leise, kompakt = true)
            Text("$anzahl", style = Schrift.MonoNormal, color = Farben.Text)
            Knopf("+", { anzahl = (anzahl + 1).coerceAtMost(10) }, art = Knopfart.Leise, kompakt = true)
            Box(Modifier.weight(1f))
            Knopf(
                "+ Bot",
                {
                    // Eine kurze Sperre gegen den Doppeltipp — sonst kämen zwei Besatzungen.
                    val v = neueVorlage
                    val jetzt = System.currentTimeMillis()
                    if (v != null && jetzt >= gesperrtBis) {
                        gesperrtBis = jetzt + 600
                        griffe.botHinzufuegen(v.id, anzahl.coerceIn(1, 10))
                        anzahl = 1
                    }
                },
                aktiv = neueVorlage != null,
                kompakt = true,
            )
        }
        if (gruppen.size > 1) {
            SehrLeise(
                "Ein Träger aus „Überörtlich anfordern“ rückt mit der ersten Besatzung in die " +
                    "Schicht ein und steht danach auch den Mitspielern offen.",
            )
        }
    }

    if (vorlagenwahl) {
        Wahlblende(
            titel = "Fahrzeug wählen",
            gruppen = gruppen,
            aufschrift = { vorlagentext(it) },
            gewaehlt = neueVorlage,
            beiWahl = {
                neueVorlage = it
                vorlagenwahl = false
            },
            beiSchliessen = { vorlagenwahl = false },
            suchbar = true,
        )
    }

    versetzenFuer?.let { botId ->
        Wahlblende(
            titel = "Auf ein anderes Fahrzeug versetzen",
            gruppen = gruppen,
            aufschrift = { vorlagentext(it) },
            beiWahl = {
                griffe.botVersetzen(botId, it.id)
                versetzenFuer = null
            },
            beiSchliessen = { versetzenFuer = null },
            suchbar = true,
        )
    }
}

// ================================================================ Bänder

/**
 * Die Bänder unter dem Kopf — Lektion, Sprechwünsche, Notrufe, Feststellungen.
 *
 * Alles hier ist zeitkritisch und verschwindet, sobald es erledigt ist.
 */
@Composable
internal fun Leitstellenbaender(
    stand: Rundenstand,
    raum: Raumzustand,
    klingelnde: List<Anruf>,
    imGespraech: Boolean,
    griffe: LeitstellenGriffe,
) {
    val jetzt = rememberJetzt()
    val kennung = rememberKennung(raum)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Lektionsleiste(raum, griffe)

        stand.funkhinweis?.let { hinweis ->
            Text(
                text = hinweis,
                style = Schrift.MonoKlein,
                color = Farben.AmberHell,
                modifier = Modifier
                    .fillMaxWidth()
                    .flaeche(farbe = Farben.FlaecheHoch, randfarbe = Farben.AmberTief, ecke = 9.dp)
                    .clickable(onClick = griffe.funkhinweisWeg)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
            )
        }

        // Sprechwünsche stehen ganz oben: dringend zuerst, sonst wer am längsten
        // wartet. Jede Besatzung bekommt ihren eigenen Knopf — nie „der Nächste".
        val wuensche = raum.vehicles
            .filter { it.sprechwunschSeit != null || it.status == 5 || it.status == 0 }
            .sortedWith(
                compareBy<Rundenfahrzeug> { dringlichkeit(it) }
                    .thenBy { it.sprechwunschSeit ?: it.statusSeit },
            )
        if (wuensche.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.FmsSprechwunsch.copy(alpha = 0.16f), Rundung.Klein)
                    .border(1.dp, Farben.FmsSprechwunsch, Rundung.Klein)
                    .padding(Abstand.Klein),
            ) {
                Text(
                    "◉ " + if (wuensche.size == 1) "Sprechwunsch" else "${wuensche.size} Sprechwünsche",
                    style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                    color = Farben.BlauHell,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    wuensche.forEach { f ->
                        Column(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .background(
                                    if (f.sprechwunschVorrang) Farben.SignalTief else Farben.FlaecheHoch,
                                    Rundung.Klein,
                                )
                                .border(
                                    1.dp,
                                    if (f.sprechwunschVorrang) Farben.SignalHell else Farben.RandHell,
                                    Rundung.Klein,
                                )
                                .clickable { griffe.sprechwunsch(f.id) }
                                .padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig),
                        ) {
                            Text(kennung(f), style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                                f.sprechwunschAnliegen?.let {
                                    Text(it, style = Schrift.Winzig, color = Farben.AmberHell)
                                }
                                Text(
                                    wartezeit(f.sprechwunschSeit ?: f.statusSeit, jetzt),
                                    style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                                    color = Farben.TextLeise,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Es klingelt. Neben dem Sprechwunsch, weil beides zeitkritisch ist.
        if (klingelnde.isNotEmpty()) {
            var gesperrtBis by remember { mutableStateOf(0L) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.SignalTief.copy(alpha = 0.5f), Rundung.Klein)
                    .border(1.dp, Farben.SignalHell, Rundung.Klein)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "☎ " + if (klingelnde.size == 1) "Notruf" else "${klingelnde.size} Notrufe",
                        style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold),
                        color = Farben.Text,
                    )
                    if (klingelnde.size > 1) {
                        SehrLeise("${klingelnde.size - 1} in der Leitung", mono = true)
                    }
                }
                Knopf(
                    "Annehmen",
                    { griffe.anrufAnnehmen(klingelnde.first().id) },
                    art = Knopfart.Haupt,
                    aktiv = !imGespraech,
                    kompakt = true,
                )
                Knopf(
                    "Abweisen",
                    {
                        // Ein Doppeltipp wies bisher gleich den nächsten mit ab — nach
                        // jedem Abweisen gilt eine kurze Sperre.
                        val jetztMs = System.currentTimeMillis()
                        if (jetztMs >= gesperrtBis) {
                            gesperrtBis = jetztMs + 700
                            griffe.anrufAbweisen(klingelnde.first().id)
                        }
                    },
                    art = Knopfart.Gefahr,
                    kompakt = true,
                )
            }
        }

        // Die Eigenfeststellungen der Streifen — kein Vorgang, der die Arbeit
        // unterbricht. Wer nichts tut, dem verfällt sie ohne Punktabzug.
        raum.feststellungen.forEach { f ->
            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Farben.OrgPolizei.copy(alpha = 0.14f), Rundung.Klein)
                    .border(1.dp, Farben.OrgPolizei, Rundung.Klein)
                    .padding(horizontal = Abstand.Normal, vertical = Abstand.Winzig),
            ) {
                Text("⇢ Feststellung", style = Schrift.MonoKlein.copy(fontWeight = FontWeight.Bold), color = Farben.GruenHell)
                Text(
                    "${f.funkrufname} · ${f.stichwort} ${f.stichwortText} — ${f.meldebild} (${f.ort})",
                    style = Schrift.Klein,
                    color = Farben.Text,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                    Knopf("Streifeneinsatz anlegen", { griffe.feststellungUebernehmen(f.id) }, kompakt = true)
                    Knopf("Kein Einsatz", { griffe.feststellungVerwerfen(f.id) }, art = Knopfart.Leise, kompakt = true)
                }
            }
        }
    }
}

/**
 * Wie dringend ein Sprechwunsch ist — kleiner heißt weiter vorn. Geordnet wird
 * danach, was die Meldung tut: Vorrang, dann „die Leitstelle muss entscheiden",
 * dann unangekündigt, dann „die Meldung hakt ab".
 */
private fun dringlichkeit(f: Rundenfahrzeug): Int {
    if (f.sprechwunschVorrang) return 0
    return when (f.sprechwunschAnliegen) {
        "Nachforderung", "Transportziel" -> 1
        null -> 2
        else -> 3
    }
}

/**
 * Die Lektion der Ausbildungsschicht — eine schmale Leiste, kein Overlay. Der
 * deutlichere zweite Satz kommt erst nach 45 Sekunden auf demselben Schritt.
 */
@Composable
private fun Lektionsleiste(raum: Raumzustand, griffe: LeitstellenGriffe) {
    val ausbildung = raum.ausbildung ?: return
    var seit by remember(ausbildung.schritt) { mutableIntStateOf(0) }
    LaunchedEffect(ausbildung.schritt) {
        while (isActive) {
            delay(1_000)
            seit += 1
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        modifier = Modifier
            .fillMaxWidth()
            .flaeche(farbe = Farben.FlaecheHoch, randfarbe = Farben.AmberTief, ecke = 9.dp)
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        if (ausbildung.abgeschlossen) {
            Etikett("Einweisung")
            Text(
                "Geschafft! Beende die Schicht über „Dienstende“ — danach steht die Einweisung in " +
                    "deinem Dienstbuch, und die erste echte Runde wartet.",
                style = Schrift.Klein,
                color = Farben.Text,
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Lektion ${ausbildung.schritt + 1}/${ausbildung.schritte}",
                style = Schrift.MonoKlein,
                color = Farben.AmberHell,
                modifier = Modifier.weight(1f),
            )
            Knopf("Überspringen", griffe.ausbildungUeberspringen, art = Knopfart.Leise, kompakt = true)
        }
        ausbildung.titel?.let { Text(it, style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text) }
        ausbildung.text?.let { Text(it, style = Schrift.Klein, color = Farben.TextLeise) }
        val hinweis = ausbildung.hinweis
        if (hinweis != null && seit >= 45) {
            Text("💡 $hinweis", style = Schrift.Klein, color = Farben.AmberHell)
        }
    }
}

/**
 * Das Rückholband: „Alarm B2 (3 Fzg) geht in 4 s raus" — mit der eigenen
 * Meldung darin, denn eine vertippte Meldung ist derselbe Fall wie die falsche
 * Schleife.
 */
@Composable
internal fun Rueckholband(
    wartend: WartenderAlarm,
    rest: Int,
    beiRueckholen: () -> Unit,
    beiSofort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .fillMaxWidth()
            .background(Farben.FlaecheHoch, Rundung.Normal)
            .border(1.dp, Farben.SignalHell, Rundung.Normal)
            .padding(Abstand.Normal),
    ) {
        Text(
            buildString {
                append("⏱ Alarm")
                if (wartend.stichwort.isNotBlank()) append(" ${wartend.stichwort}")
                append(" (${wartend.auftrag.fahrzeugIds.size} Fzg) geht in $rest s raus")
                wartend.auftrag.zusatztext?.let { append(" · „$it“") }
            },
            style = Schrift.MonoKlein,
            color = Farben.Text,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
            Knopf("Rückholen", beiRueckholen, art = Knopfart.Gefahr, kompakt = true, modifier = Modifier.weight(1f))
            Knopf("Sofort raus", beiSofort, art = Knopfart.Alarm, kompakt = true, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Die Töne des Tischs — das Telefon klingelt, ein Sprechwunsch gongt.
 *
 * <b>Einmal je Zustandspaket, nicht je Wagen:</b> Drei neue Wünsche auf einmal
 * wären drei ineinandergeschobene Signale. Es gibt eines, und es trägt die
 * höchste Dringlichkeit darin. Was beim Hereinkommen schon wartete, hat sich
 * nicht gerade gemeldet — es gongt nicht.
 */
@Composable
internal fun Leitstellentoene(raum: Raumzustand?, klingelt: Boolean) {
    val ton by rememberTonstand()
    val pegel = ton.tongeberpegel

    LaunchedEffect(klingelt, pegel) {
        if (!klingelt || pegel == 0) return@LaunchedEffect
        val geber = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, pegel) }.getOrNull()
            ?: return@LaunchedEffect
        try {
            while (isActive) {
                geber.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1_000)
                delay(3_000)
            }
        } finally {
            geber.release()
        }
    }

    val gemeldet = remember { mutableSetOf<String>() }
    var gelesen by remember { mutableStateOf(false) }
    val wartende = raum?.vehicles?.filter {
        it.status == 5 || it.status == 0 || it.sprechwunschSeit != null
    }
    val schluessel = wartende?.joinToString(",") { it.id }

    LaunchedEffect(schluessel) {
        if (wartende == null) return@LaunchedEffect
        var melden = false
        var vorrang = false
        wartende.forEach { f ->
            if (!gelesen || f.id in gemeldet) return@forEach
            melden = true
            if (f.status == 0 || f.sprechwunschVorrang) vorrang = true
        }
        gemeldet.clear()
        gemeldet.addAll(wartende.map { it.id })
        gelesen = true

        if (!melden || pegel == 0) return@LaunchedEffect
        val geber = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, pegel) }.getOrNull()
            ?: return@LaunchedEffect
        try {
            if (vorrang) {
                // Das harte Wechselsignal: Der Platz muss sofort besetzt werden.
                repeat(3) {
                    geber.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 220)
                    delay(260)
                    geber.startTone(ToneGenerator.TONE_CDMA_LOW_L, 220)
                    delay(260)
                }
            } else {
                geber.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                delay(400)
            }
        } finally {
            geber.release()
        }
    }
}
