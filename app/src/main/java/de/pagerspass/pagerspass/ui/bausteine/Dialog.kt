package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Erhebung
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.lichtkante

/**
 * Eine Haarlinie an der Ober- oder Unterkante.
 *
 * Kopf und Fuß eines Dialogs trennen sich vom Inhalt durch genau diese Linie.
 * `Modifier.border` kann sie nicht — der zieht immer alle vier Seiten.
 */
private fun Modifier.kante(unten: Boolean): Modifier = drawBehind {
    val strich = 1.dp.toPx()
    val y = if (unten) size.height - strich / 2f else strich / 2f
    drawLine(Farben.Rand, Offset(0f, y), Offset(size.width, y), strich)
}

/** Wie breit ein Dialog werden darf — drei Stufen, nach dem, was drinsteht. */
enum class Dialogbreite(val breite: Dp) {
    /** Eine Frage, eine Antwort: Bestätigen, Mitglied, Nachricht. */
    Schmal(440.dp),

    /** Der Regelfall: ein Formular, eine Liste. */
    Normal(560.dp),

    /** Was zum Lesen da ist: Wiki, Rechtstext, Einsatzaufnahme. */
    Breit(720.dp),
}

/**
 * Der Dialog — eine Bauform für alle.
 *
 * Im Web trug vorher jeder seine eigene: drei Hintergründe, zwei Weichzeichner,
 * drei Ebenen, neun Breiten, sechs Höhen. Zwei davon hießen nicht einmal Dialog.
 *
 * <b>Am Handy nimmt er die Breite der Seite — ihre Höhe nur, wenn er sie
 * braucht.</b> Das ist die wichtigste Abweichung vom Rechner und sie ist
 * nachgemessen: Bei der Aufzeichnungsfrage auf 390 × 844 stand der Kasten 828
 * Punkte hoch, weil zwei Angaben einander widersprachen. Bei der Verwarnung —
 * drei Absätze und ein Knopf — blieben dadurch rund 400 Punkte schwarze Fläche
 * zwischen dem letzten Satz und der Fußleiste, und „Gelesen" saß eine halbe
 * Bildschirmhöhe unter dem Text, auf den es sich bezieht.
 *
 * Deshalb sitzt der Kasten hier **unten** und ist so hoch, wie sein Inhalt ihn
 * braucht — dort, wo der Daumen ohnehin ist. Wer mehr Inhalt hat, als der
 * Bildschirm trägt, läuft gegen den Deckel und füllt die Seite wie bisher.
 *
 * <b>Der Kasten selbst rollt nie</b> — das tut sein Inhalt. Ohne das rollten
 * beide, und der Kopf wanderte mit.
 */
@Composable
fun Blende(
    titel: String,
    beiSchliessen: () -> Unit,
    modifier: Modifier = Modifier,
    breite: Dialogbreite = Dialogbreite.Normal,
    schliessenMoeglich: Boolean = true,
    kopfknoepfe: @Composable (() -> Unit)? = null,
    fuss: @Composable (() -> Unit)? = null,
    fussAlsSpalte: Boolean = false,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val aussparung = WindowInsets.safeDrawing.asPaddingValues()
    val schirmhoehe = LocalConfiguration.current.screenHeightDp.dp

    Dialog(
        onDismissRequest = { if (schliessenMoeglich) beiSchliessen() },
        properties = DialogProperties(
            dismissOnBackPress = schliessenMoeglich,
            dismissOnClickOutside = schliessenMoeglich,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            // Die Aussparungen oben und unten gehören zum Rand, nicht zum
            // Dialog: Ohne das säße der Schließen-Knopf unter der Uhr und der
            // Fuß über dem Wischstreifen.
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .background(Farben.Ueberlagerung)
                .clickable(
                    enabled = schliessenMoeglich,
                    onClick = beiSchliessen,
                    indication = null,
                    interactionSource = null,
                )
                .padding(
                    start = Abstand.Gross,
                    end = Abstand.Gross,
                    top = maxOf(Abstand.Gross, aussparung.calculateTopPadding()),
                    bottom = maxOf(Abstand.Gross, aussparung.calculateBottomPadding()),
                ),
        ) {
            Column(
                modifier = modifier
                    .widthIn(max = breite.breite)
                    .fillMaxWidth()
                    // 88 % der Bildschirmhöhe, höchstens 760 Punkte — der
                    // Deckel aus base.css. Darunter wächst der Kasten mit
                    // seinem Inhalt.
                    .heightIn(max = minOf(schirmhoehe * 0.88f, 760.dp))
                    .shadow(Erhebung.Alarm, Rundung.Normal)
                    .background(Farben.Flaeche, Rundung.Normal)
                    .border(1.dp, Farben.RandHell, Rundung.Normal)
                    .clip(Rundung.Normal)
                    .lichtkante()
                    // Ein Druck im Dialog darf ihn nicht schließen — der Kasten
                    // fängt den Druck ab, der sonst zur Überlagerung durchfiele.
                    .clickable(
                        enabled = false,
                        onClick = {},
                        indication = null,
                        interactionSource = null,
                    ),
            ) {
                Dialogkopf(titel, kopfknoepfe)

                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(Abstand.Gross),
                    content = inhalt,
                )

                if (fuss != null) Dialogfuss(fussAlsSpalte, fuss)
            }
        }
    }
}

/**
 * Kopf und Fuß tragen ihre Größe fest.
 *
 * In einem Flex-Kasten darf ein Kind unter seine Inhaltsgröße schrumpfen — im
 * Web wurde daraus bei einem vollen Dialog ein Kopf von zwölf Punkten Höhe mit
 * abgeschnittenem Titel. In Compose ist das Gegenstück, dass allein der Inhalt
 * ein `weight` bekommt und Kopf und Fuß keines.
 */
@Composable
private fun Dialogkopf(titel: String, knoepfe: @Composable (() -> Unit)?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            // Der Kopf am Handy trägt einen Verlauf — er hebt ihn vom Inhalt ab,
            // ohne dass dafür eine zweite Linie nötig wäre.
            .background(Brush.verticalGradient(listOf(Farben.FlaecheHoch, Farben.Flaeche)))
            .kante(unten = true)
            .padding(Abstand.Gross),
    ) {
        Text(text = titel, style = Schrift.Titel, color = Farben.Text, modifier = Modifier.weight(1f))
        // Schrumpfen erlaubt, umbrechen statt hinauslaufen: Das Notruftelefon
        // trägt drei Knöpfe, und auf einer Handbreit lief „Auflegen" rechts aus
        // dem Bild — ausgerechnet der Knopf, der das Gespräch beendet.
        if (knoepfe != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                knoepfe()
            }
        }
    }
}

/**
 * Der Fuß — die Antworten.
 *
 * <b>Am Handy teilen sie sich die Zeile, statt sich rechts zu drängen.</b> Am
 * Rechner ist der Fuß so breit wie der Dialog, also 560 Punkte; am Handy so
 * breit wie der Bildschirm, und „Gelesen" stand als schmaler Knopf in der
 * rechten unteren Ecke einer 390 Punkte breiten Leiste.
 *
 * @param alsSpalte Für eine Entscheidung, bei der die Antworten untereinander
 *   stehen — Rechtsstand, Datenverarbeitung. Nebeneinander stünden dort zwei
 *   gleich aussehende Knöpfe mit gegenteiliger Bedeutung.
 */
@Composable
private fun Dialogfuss(alsSpalte: Boolean, inhalt: @Composable () -> Unit) {
    val rahmen = Modifier
        .fillMaxWidth()
        .background(Farben.Flaeche)
        .kante(unten = false)
        .padding(horizontal = Abstand.Gross, vertical = Abstand.Normal)

    if (alsSpalte) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = rahmen,
        ) {
            inhalt()
        }
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = rahmen,
        ) {
            inhalt()
        }
    }
}

/**
 * Ein Dialog mit Seiten — Kopf, eine Wegeleiste, die nicht mitrollt, und genau
 * eine Seite darunter. Übertragen aus `.dialog--arbeit` mit `.rd__wege` im Web
 * (`RundenDialog.vue`).
 *
 * <b>Er hat eine feste Höhe und nicht die seines Inhalts.</b> Mit der Höhe des
 * Inhalts spränge er bei jedem Seitenwechsel: Eine Seite ist eine Zeile mit einem
 * Knopf, die nächste vier Bildschirme lang. Ein Kasten, dessen Kanten beim
 * Umschalten wandern, verliert den Blick — die Wegeleiste spränge mit.
 *
 * @param etikett Die kleine Zeile über dem Titel — wovon der Dialog handelt.
 * @param leiste Die Wege zu den Seiten. Sie steht zwischen Kopf und Inhalt und
 *   rollt nicht mit; am Handy ist sie eine Zeile, die seitwärts rollt.
 * @param rollstand Der Rollstand der Seite — von außen, damit ein Seitenwechsel
 *   oben anfangen kann.
 */
@Composable
fun Seitenblende(
    titel: String,
    beiSchliessen: () -> Unit,
    leiste: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    etikett: String? = null,
    rollstand: androidx.compose.foundation.ScrollState = rememberScrollState(),
    fuss: @Composable (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val aussparung = WindowInsets.safeDrawing.asPaddingValues()
    val schirmhoehe = LocalConfiguration.current.screenHeightDp.dp

    Dialog(
        onDismissRequest = beiSchliessen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .background(Farben.Ueberlagerung)
                .clickable(onClick = beiSchliessen, indication = null, interactionSource = null)
                .padding(
                    start = Abstand.Gross,
                    end = Abstand.Gross,
                    top = maxOf(Abstand.Gross, aussparung.calculateTopPadding()),
                    bottom = maxOf(Abstand.Gross, aussparung.calculateBottomPadding()),
                ),
        ) {
            Column(
                modifier = modifier
                    .widthIn(max = Dialogbreite.Breit.breite)
                    .fillMaxWidth()
                    // Fest: dieselbe Höhe wie der Deckel jeder Blende.
                    .height(minOf(schirmhoehe * 0.88f, 760.dp))
                    .shadow(Erhebung.Alarm, Rundung.Normal)
                    .background(Farben.Flaeche, Rundung.Normal)
                    .border(1.dp, Farben.RandHell, Rundung.Normal)
                    .clip(Rundung.Normal)
                    .lichtkante()
                    .clickable(enabled = false, onClick = {}, indication = null, interactionSource = null),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Farben.FlaecheHoch, Farben.Flaeche)))
                        .padding(start = Abstand.Gross, end = Abstand.Gross, top = Abstand.Gross, bottom = Abstand.Klein),
                ) {
                    if (etikett != null) {
                        Text(
                            text = etikett.uppercase(),
                            style = Schrift.Etikett,
                            color = Farben.TextSehrLeise,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                    Text(text = titel, style = Schrift.Titel, color = Farben.Text)
                }

                Box(Modifier.fillMaxWidth().kante(unten = true)) { leiste() }

                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rollstand)
                        .padding(Abstand.Gross),
                    content = inhalt,
                )

                if (fuss != null) Dialogfuss(false, fuss)
            }
        }
    }
}
