package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel

/**
 * Der Umschalter — zwei oder drei Seiten derselben Sache.
 *
 * „Anmelden | Konto erstellen", „Alle | Mitspieler | Niemand", „An | Aus". Im Web
 * stand er fünfmal im Baum, und zwei davon waren bis aufs Zeichen dieselben zwölf
 * Zeilen unter verschiedenem Namen.
 *
 * <b>Zwei Zustände desselben Formulars, keine zwei Seiten.</b> Genau deshalb ist
 * es ein Umschalter und keine Reiterreihe: Die Reiter oben in einer Ansicht
 * wechseln, *was* man sieht; dieser hier wechselt, *wie* dasselbe gemeint ist.
 *
 * <b>Mindestens 44 Punkte breit, nicht nur hoch.</b> Bei einem Segment, das „An"
 * heißt, sind zwei Buchstaben plus zweimal zwölf Punkte Polster gerade vierzig —
 * vier zu wenig, und zwar an genau den Schaltern, mit denen man in der
 * Privatsphäre etwas ein- und ausschaltet.
 *
 * Die Aufschrift steht in Monospace und Versalien: Sie ist eine Stellung, kein
 * Satz.
 */
@Composable
fun <T> Segment(
    seiten: List<T>,
    gewaehlt: T,
    beiWahl: (T) -> Unit,
    modifier: Modifier = Modifier,
    aufschrift: (T) -> String = { it.toString() },
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(Farben.BgTief, Rundung.Rund)
            .border(1.dp, Farben.Rand, Rundung.Rund)
            .padding(Abstand.Haar),
    ) {
        seiten.forEach { seite ->
            val an = seite == gewaehlt
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = Ziel.Kompakt, minHeight = Ziel.Kompakt)
                    .background(if (an) Farben.FlaecheAktiv else Color.Transparent, Rundung.Rund)
                    .clickable(
                        onClick = { beiWahl(seite) },
                        role = Role.Tab,
                        indication = null,
                        interactionSource = null,
                    )
                    .padding(horizontal = Abstand.Normal),
            ) {
                Text(
                    text = aufschrift(seite).uppercase(),
                    style = Schrift.Winzig.copy(
                        fontFamily = Schrift.Mono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.em,
                    ),
                    color = if (an) Farben.Amber else Farben.TextSehrLeise,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Der Textweg — ein Weg, der wie Text aussieht.
 *
 * „Passwort vergessen?", „Alle zeigen →". <b>Keine amberne Fläche:</b> Amber
 * heißt in dieser Anwendung „das ist die Handlung", und auf einem Bildschirm
 * gibt es davon eine. Ein Nebenweg, der wie der Hauptweg aussieht, nimmt dem
 * Hauptweg seine Aussage.
 *
 * Er hält trotzdem die 44 Punkte Höhe — er wird mit dem Daumen getroffen wie
 * alles andere auch.
 */
@Composable
fun Textweg(
    aufschrift: String,
    beiDruck: () -> Unit,
    modifier: Modifier = Modifier,
    aktiv: Boolean = true,
    farbe: Color = Farben.BlauHell,
    zeichen: ImageVector? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Winzig),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = Ziel.Kompakt)
            .clickable(
                enabled = aktiv,
                onClick = beiDruck,
                role = Role.Button,
                indication = null,
                interactionSource = null,
            ),
    ) {
        Text(
            text = aufschrift,
            style = Schrift.Klein,
            color = if (aktiv) farbe else Farben.TextSehrLeise,
        )
        if (zeichen != null) {
            Icon(
                imageVector = zeichen,
                contentDescription = null,
                tint = if (aktiv) farbe else Farben.TextSehrLeise,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
