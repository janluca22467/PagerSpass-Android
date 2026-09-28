package de.pagerspass.pagerspass.mobil

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.ui.bausteine.Markenzahl
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Dauer
import de.pagerspass.pagerspass.ui.theme.Erhebung
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Mass
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Die Leiste am unteren Rand — der feste Boden des Handy-Zweigs.
 *
 * Übertragen aus `web/src/mobil/MobilTableiste.vue`.
 *
 * <b>Unten und nicht oben, weil der Daumen dort hinkommt:</b> sechs Wege, die
 * man im Dienst wirklich braucht — Dienst, Dienstbuch, Wache, Freunde, Shop,
 * Konto —, immer an derselben Stelle. Am Rechner übernimmt das eine Leiste am
 * Kopf; die ist zum Zielen mit der Maus gebaut und am Handy zu klein und zu
 * weit weg. Mit Premium kommt als siebter der Begleiter („Scan") dazu.
 *
 * <b>Wo die Leiste nicht hingehört, entscheidet nicht sie selbst</b>, sondern der
 * Rahmen (siehe `PagerSpassApp`): im Raum nicht (dort hat jede Ansicht ihre
 * eigenen Reiter und braucht jede Zeile Höhe), auf der Anmeldeseite nicht
 * (dorthin führt kein Weg zurück, solange kein Konto steht), in der Welt nicht
 * (die trägt ihre eigene Bar) und über einem offenen Dialog nicht.
 */
@Composable
fun Tableiste(
    hier: Weg?,
    beiWahl: (Weg) -> Unit,
    modifier: Modifier = Modifier,
    marken: Map<Weg, Int> = emptyMap(),
    begleiter: Boolean = false,
) {
    // Der Scan-Reiter hängt an Premium wie der Begleiter selbst: Ohne Abo erzeugt
    // der Rechner keinen QR-Code, und ein Reiter, der in eine Ladenwand führt,
    // wäre hier unten der teuerste Platz dafür.
    val wege = if (begleiter) Weg.entries else Weg.entries.filter { it != Weg.Begleiter }
    val sieben = wege.size > 6
    val breite = LocalConfiguration.current.screenWidthDp.dp

    // Der Streifen unter der Leiste gehört bei randlosen Geräten dem Gerät,
    // nicht der Wache — bedienbar ist er nicht, farbig gefüllt gehört er
    // trotzdem.
    val geraeterand = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Auf einer Handbreit verdrängen sechs volle Namen einander. Die kurze
    // Fassung ist im Spiel eindeutig; der volle Name bleibt für
    // Vorleseprogramme erhalten.
    val eng = breite < 560.dp

    // Zu siebt wird es eng: Unter 365 Punkten gibt die Leiste Fugen und Ränder
    // her, damit jedes Ziel seine 44 behält, und unter 341 trägt das Zeichen
    // allein — der Name bleibt für Vorleseprogramme erhalten.
    val randlos = sieben && breite < 365.dp
    val ohneText = sieben && breite <= 340.dp
    val sperrung = when {
        !sieben -> null
        randlos -> (-0.05).em
        else -> (-0.03).em
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(if (randlos) 0.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(Erhebung.Leiste, Rundung.LeisteOben)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFA263344), Color(0xFA101823)),
                ),
                shape = Rundung.LeisteOben,
            )
            // Die Kante nach oben — und die Lichtkante darauf. Beide sind hier
            // nicht Zierde: Ohne sie steht die Leiste kantenlos vor dem Grund
            // und der Inhalt, der darunter durchrollt, hört einfach auf.
            .drawBehind {
                val strich = 1.dp.toPx()
                drawLine(
                    color = Farben.RandHell.copy(alpha = 0.76f),
                    start = Offset(0f, strich / 2f),
                    end = Offset(size.width, strich / 2f),
                    strokeWidth = strich,
                )
            }
            .padding(horizontal = if (randlos) 0.dp else 4.dp)
            .padding(bottom = geraeterand),
    ) {
        wege.forEach { weg ->
            Wegknopf(
                weg = weg,
                hier = weg == hier,
                eng = eng,
                ohneText = ohneText,
                sperrung = sperrung,
                marke = marken[weg] ?: 0,
                beiDruck = { beiWahl(weg) },
            )
        }
    }
}

/**
 * Die sechs Wege der Leiste — sieben mit Premium, dann kommt der Begleiter dazu.
 *
 * Sie stehen als Aufzählung und nicht als sechs Aufrufe, weil mehr als die
 * Leiste sie braucht: Der Rahmen rechnet aus ihnen, wo man gerade ist, und die
 * Navigation nimmt ihre Adressen. Sechs Wege an drei Stellen von Hand gepflegt
 * wären ab dem ersten Umbau fünf und sieben.
 *
 * @param adresse Die Route in der Navigation.
 * @param titel Der volle Name — für Vorleseprogramme und für breite Geräte.
 * @param kurz Was auf einer Handbreit sichtbar wird. Gleich dem vollen Namen,
 *   wo er ohnehin passt.
 */
enum class Weg(
    val adresse: String,
    val titel: String,
    val kurz: String,
    val zeichen: ImageVector,
    val vorlesen: String = titel,
) {
    Dienst("dienst", "Dienst", "Dienst", Zeichen.WegDienst),
    Dienstbuch("dienstbuch", "Dienstbuch", "Buch", Zeichen.WegBuch),
    Wache("wache", "Wache", "Wache", Zeichen.WegWache),
    Freunde("freunde", "Freunde", "Freunde", Zeichen.WegFreunde),
    Shop("shop", "Shop", "Shop", Zeichen.WegShop),
    Konto("konto", "Konto", "Konto", Zeichen.WegKonto),

    /** Nur mit Premium sichtbar — `/scan` im Web; die Route ist die Begleiter-Kopplung. */
    Begleiter("begleiter", "Begleiter", "Scan", Zeichen.WegScan, "Begleiter koppeln — QR-Code scannen"),
}

/**
 * Ein Weg in der Leiste.
 *
 * <b>Die Aufschrift steht auf der Untergrenze der Staffel, nicht darunter.</b> Im
 * Web standen hier einmal 11,5 Punkte mit dem Argument, das Zeichen darüber trage
 * die halbe Bedeutung. Das stimmt — und ändert nichts daran, dass dies die sechs
 * Wege sind, die auf jeder Seite an derselben Stelle stehen.
 *
 * <b>Und nicht in der leisesten Farbe.</b> Die ist für Nebenangaben da, nicht für
 * die Hauptwegweisung. Fünf der sechs Wege stehen immer im Ruhezustand — in
 * `TextSehrLeise` sah die Leiste aus, als wäre sie insgesamt gesperrt.
 */
@Composable
private fun RowScope.Wegknopf(
    weg: Weg,
    hier: Boolean,
    eng: Boolean,
    ohneText: Boolean,
    sperrung: TextUnit?,
    marke: Int,
    beiDruck: () -> Unit,
) {
    val beruehrung = remember { MutableInteractionSource() }
    val gedrueckt by beruehrung.collectIsPressedAsState()
    val stauchung by animateFloatAsState(
        targetValue = if (gedrueckt) 0.96f else 1f,
        animationSpec = tween(Dauer.WECHSEL),
        label = "weg-stauchung",
    )

    val farbe = if (hier) Farben.Amber else Farben.TextLeise

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .weight(1f)
            .scale(stauchung)
            .padding(vertical = 6.dp)
            .defaultMinSize(minHeight = Mass.LeisteHoehe - 12.dp)
            .then(
                if (hier) {
                    Modifier.background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Farben.Amber.copy(alpha = 0.17f),
                                Farben.Amber.copy(alpha = 0.08f),
                            ),
                        ),
                        shape = Rundung.Normal,
                    )
                } else if (gedrueckt) {
                    Modifier.background(Farben.FlaecheAktiv, Rundung.Normal)
                } else {
                    Modifier
                }
            )
            // Der Balken sitzt unten auf der Kante und zeigt, wo man steht —
            // dieselbe Bildsprache wie die Reiter im Fahrzeug.
            .drawBehind {
                if (!hier) return@drawBehind
                val hoehe = 3.dp.toPx()
                val breite = size.width * 0.22f
                drawRoundRect(
                    color = Farben.Amber,
                    topLeft = Offset((size.width - breite) / 2f, size.height - hoehe - 3.dp.toPx()),
                    size = Size(breite, hoehe),
                    cornerRadius = CornerRadius(hoehe / 2f, hoehe / 2f),
                )
            }
            .clickable(
                onClick = beiDruck,
                role = Role.Tab,
                indication = null,
                interactionSource = beruehrung,
            )
            .semantics {
                contentDescription = if (marke > 0) "${weg.titel} — $marke offen" else weg.vorlesen
            }
            .padding(horizontal = Abstand.Haar, vertical = Abstand.Winzig),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(top = Abstand.Klein),
        ) {
            Icon(
                imageVector = weg.zeichen,
                contentDescription = null,
                tint = farbe,
                // Ohne Beschriftung darf das Zeichen die Höhe haben, die es
                // vorher mit ihr teilte.
                modifier = Modifier.size(if (ohneText) 26.dp else 22.dp),
            )
            if (!ohneText) {
                Text(
                    text = if (eng) weg.kurz else weg.titel,
                    style = if (sperrung != null) Schrift.Weg.copy(letterSpacing = sperrung) else Schrift.Weg,
                    color = farbe,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }

        // Die Marke sitzt oben rechts über dem Zeichen, nicht in der Ecke der
        // Spalte: In der Ecke stünde sie zwischen zwei Wegen und man läse sie
        // dem falschen zu.
        if (marke > 0) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(start = 26.dp, top = 2.dp)) {
                Markenzahl(marke)
            }
        }
    }
}
