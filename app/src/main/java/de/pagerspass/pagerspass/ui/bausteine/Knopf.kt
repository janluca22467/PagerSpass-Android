package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Dauer
import de.pagerspass.pagerspass.ui.theme.Erhebung
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Der Knopf — übertragen aus `.knopf` in `base.css`.
 *
 * <b>Die Art ist ein Wert, keine eigene Bauform.</b> Im Web sind das
 * Modifikatoren an einer Klasse (`.knopf--haupt`, `--alarm`, `--leise`,
 * `--gefahr`); hier ein Aufzählungstyp. Der Grund ist derselbe: Ein zweiter
 * Knopf-Baustein wäre ab dem ersten Tag ein Knopf, der beim nächsten Umbau ein
 * anderes Polster bekommt.
 */
enum class Knopfart {
    /** Der Regelfall: gehobene Fläche, heller Rand. */
    Normal,

    /**
     * Der Hauptweg der Seite — voll Amber mit dunkler Aufschrift.
     *
     * Höchstens einer je Bildschirm. Wenn zwei Knöpfe Amber tragen, trägt keiner
     * mehr die Aussage „das ist es, was du hier tust".
     */
    Haupt,

    /**
     * Rot, *bevor* man ihn drückt — „Alarmieren", „Notruf".
     *
     * Nicht zu verwechseln mit `Gefahr`: Der hier sagt, was die Sache ist; der
     * andere fragt nach.
     */
    Alarm,

    /** Der Nebenweg: ohne Fläche, nur Rand — „Abbrechen", „Zurück". */
    Leise,

    /**
     * Die Rückfrage — der zweite Druck auf denselben Knopf.
     *
     * Keine rote Fläche, sondern rote Schrift auf der gewohnten: Der Knopf
     * bleibt derselbe und sagt nur, dass er es jetzt ernst meint. Er stand im
     * Web wortgleich in drei Ansichten, bevor er ein Wert wurde.
     */
    Gefahr,
}

/**
 * Ein Knopf mit Aufschrift.
 *
 * @param kompakt Engeres Polster und kleinere Schrift für dichte Leisten.
 *   <b>Nicht niedriger:</b> Am Rechner schrumpft ein kompakter Knopf auf 36
 *   Punkte, am Daumen gilt die 44er-Marke ohne Ausnahme (siehe `Ziel.Kompakt`).
 * @param breit Ob der Knopf die volle Breite nimmt. Am Handy ist das der
 *   Regelfall für den Hauptweg am Fuß eines Formulars.
 */
@Composable
fun Knopf(
    aufschrift: String,
    beiDruck: () -> Unit,
    modifier: Modifier = Modifier,
    art: Knopfart = Knopfart.Normal,
    aktiv: Boolean = true,
    kompakt: Boolean = false,
    breit: Boolean = false,
    zeichenVorn: @Composable (() -> Unit)? = null,
) {
    Knopfrahmen(
        beiDruck = beiDruck,
        modifier = modifier,
        art = art,
        aktiv = aktiv,
        kompakt = kompakt,
        breit = breit,
    ) {
        zeichenVorn?.invoke()
        Text(
            text = aufschrift,
            style = if (kompakt) Schrift.Knopf.copy(fontSize = Schrift.KLEIN) else Schrift.Knopf,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Der Knopf ohne Aufschrift — nur ein Zeichen darin.
 *
 * <b>Warum er eine eigene Zeile bekommt.</b> Im Web standen dreizehn davon in
 * zehn Größen (24, 26, 32, 34, 38, 40, 42, 44, 46, 48) und **neun verfehlten die
 * 44er-Marke**. Das ist keine Geschmacksfrage: Ein Zeichen ohne Wort ist ohnehin
 * schon schwerer zu treffen, weil man erst hinsieht, was es bedeutet.
 *
 * `defaultMinSize` und keine feste Breite: Die Marke sagt „mindestens so groß",
 * und ein Zeichenknopf mit einer Zahl darin darf breiter werden.
 */
@Composable
fun Zeichenknopf(
    beiDruck: () -> Unit,
    beschreibung: String,
    modifier: Modifier = Modifier,
    art: Knopfart = Knopfart.Normal,
    aktiv: Boolean = true,
    kompakt: Boolean = false,
    inhalt: @Composable () -> Unit,
) {
    Knopfrahmen(
        beiDruck = beiDruck,
        modifier = modifier.defaultMinSize(minWidth = if (kompakt) Ziel.Kompakt else Ziel.Normal),
        art = art,
        aktiv = aktiv,
        kompakt = kompakt,
        ohnePolster = true,
        beschreibung = beschreibung,
        inhalt = inhalt,
    )
}

/**
 * Der gemeinsame Rahmen — Fläche, Rand, Höhe, Rückmeldung auf den Druck.
 *
 * Er ist nicht öffentlich: Wer einen Knopf braucht, nimmt `Knopf` oder
 * `Zeichenknopf`. Ein dritter Aufrufer wäre ein Knopf, der sich seine Höhe
 * selbst ausdenkt — und genau daran ist die Streuung im Web entstanden.
 */
@Composable
private fun Knopfrahmen(
    beiDruck: () -> Unit,
    modifier: Modifier,
    art: Knopfart,
    aktiv: Boolean,
    kompakt: Boolean,
    breit: Boolean = false,
    ohnePolster: Boolean = false,
    beschreibung: String? = null,
    inhalt: @Composable () -> Unit,
) {
    val beruehrung = remember { MutableInteractionSource() }
    val gedrueckt by beruehrung.collectIsPressedAsState()

    // Am Handy staucht der Knopf, am Rechner sinkt er einen Punkt. Gestaucht,
    // weil der Finger die Fläche verdeckt: Eine Bewegung von einem Punkt sieht
    // man unter dem eigenen Daumen nicht.
    val stauchung by animateFloatAsState(
        targetValue = if (gedrueckt && aktiv) 0.98f else 1f,
        animationSpec = tween(Dauer.WECHSEL),
        label = "knopf-stauchung",
    )

    val farben = knopffarben(art, aktiv, gedrueckt)
    val hoehe: Dp = if (kompakt) Ziel.Kompakt else Ziel.Normal
    val polster: Dp = when {
        ohnePolster -> 0.dp
        kompakt -> Abstand.Normal
        else -> Abstand.Gross
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .scale(stauchung)
            .then(if (breit) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = hoehe)
            // Der Schein des Hauptknopfs ist Licht in der Akzentfarbe, kein
            // Schatten — und er geht mit, wenn der Knopf gesperrt ist. Im Web
            // blieb er stehen, und „Bauen für 4.000" leuchtete gesperrt genauso
            // wie offen; man tippte darauf.
            .then(
                if (art == Knopfart.Haupt && aktiv) {
                    Modifier.shadow(
                        elevation = Erhebung.Hebt,
                        shape = Rundung.Klein,
                        ambientColor = Farben.Amber,
                        spotColor = Farben.Amber,
                    )
                } else {
                    Modifier
                }
            )
            .background(farben.flaeche, Rundung.Klein)
            .border(1.dp, farben.rand, Rundung.Klein)
            .clickable(
                enabled = aktiv,
                onClick = beiDruck,
                role = Role.Button,
                interactionSource = beruehrung,
                indication = null,
            )
            .padding(horizontal = polster)
            // Ein Knopf ohne Aufschrift sagt einem Vorleseprogramm sonst gar
            // nichts — im Web trägt er dafür `aria-label`.
            .then(
                if (beschreibung != null) {
                    Modifier.semantics { contentDescription = beschreibung }
                } else {
                    Modifier
                }
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides farben.schrift, content = inhalt)
    }
}

/** Fläche, Rand und Schrift eines Knopfes in einem Zustand. */
private data class Knopffarben(val flaeche: Color, val rand: Color, val schrift: Color)

/**
 * Welche Farben ein Knopf gerade trägt.
 *
 * <b>Gesperrt heißt „jetzt nicht", nicht „unlesbar".</b> Mit 40 % Deckkraft stand
 * die dunkle Aufschrift eines Hauptknopfs auf abgedunkeltem Amber — „Dienst
 * beginnen" und „Beitreten" waren damit genau in dem Moment kaum zu entziffern,
 * in dem man wissen will, was da eigentlich nicht geht. Ein gesperrter Haupt-
 * oder Alarmknopf gibt deshalb die *Farbe* auf, nicht den Text: gedämpfte
 * Fläche, aber eine Aufschrift, die über der Grenze bleibt.
 *
 * Die Dämpfung gilt weiterhin für die drei übrigen Arten: Die tragen ihren Text
 * ohnehin auf dunklem Grund und bleiben auch abgedunkelt lesbar.
 */
@Composable
private fun knopffarben(art: Knopfart, aktiv: Boolean, gedrueckt: Boolean): Knopffarben {
    if (!aktiv && (art == Knopfart.Haupt || art == Knopfart.Alarm)) {
        return Knopffarben(Farben.FlaecheHoch, Farben.Rand, Farben.TextSehrLeise)
    }

    val grund = when (art) {
        Knopfart.Normal -> Knopffarben(Farben.FlaecheHoch, Farben.RandHell, Farben.Text)
        Knopfart.Haupt -> Knopffarben(Farben.Amber, Farben.Amber, Farben.AufAmber)
        Knopfart.Alarm -> Knopffarben(Farben.Signal, Farben.SignalHell, Color.White)
        Knopfart.Leise -> Knopffarben(Color.Transparent, Farben.Rand, Farben.TextLeise)
        Knopfart.Gefahr -> Knopffarben(Farben.FlaecheHoch, Farben.SignalTief, Farben.SignalHell)
    }

    // Der gedrückte Zustand am Handy: hellere Fläche, amberfarbener Rand. Bei
    // Haupt und Alarm bleibt die Fläche, wie sie ist — sie *ist* schon die
    // Rückmeldung, und ein Amber, das beim Drücken grau wird, sieht kaputt aus.
    val gehalten = gedrueckt && aktiv && art != Knopfart.Haupt && art != Knopfart.Alarm
    val farben = if (gehalten) {
        grund.copy(flaeche = Farben.FlaecheAktiv, rand = Farben.AmberTief)
    } else {
        grund
    }

    return if (aktiv) farben else farben.copy(
        flaeche = farben.flaeche.copy(alpha = farben.flaeche.alpha * 0.6f),
        rand = farben.rand.copy(alpha = farben.rand.alpha * 0.6f),
        schrift = farben.schrift.copy(alpha = 0.6f),
    )
}
