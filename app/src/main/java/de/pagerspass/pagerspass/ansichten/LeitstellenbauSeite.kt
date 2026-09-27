package de.pagerspass.pagerspass.ansichten

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Der Leitstellenbau am Handy — nur ein Hinweis, wörtlich wie im mobilen Web
 * (`LeitstellenbauView.vue`, Zweig `amHandy`).
 *
 * Zwei Spalten, eine Karte zum Ziehen und eine Liste mit siebzig Zeilen sind auf
 * einem Handy keine Bedienung. Ein Satz, der sagt warum, und der Weg zurück —
 * kein halbierter Editor, in dem die Hälfte nicht geht. Der Startbildschirm führt
 * deshalb gar nicht erst hierher (im Web ist die Kachel am Handy ausgeblendet);
 * die Seite steht für den, der über eine Adresse kommt.
 */
@Composable
fun LeitstellenbauSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    beiZurueck: () -> Unit = {},
) {
    Seite(modifier = modifier, unterrand = unterrand) {
        Etikett("Leitstellenbau")
        Seitenkopf(
            titel = "Am Schreibtisch gebaut",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Kasten {
            Text(
                text = buildAnnotatedString {
                    append("Die eigene Leitstelle wird ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("am Rechner gebaut") }
                    append(
                        ": Wachen auf einer Karte anklicken und verschieben, Rufnamen nebenher " +
                            "prüfen — dafür braucht es Platz, den ein Handybildschirm nicht hat.",
                    )
                },
                style = Schrift.Normal,
                color = Farben.Text,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Fahren kannst du sie überall.")
                    }
                    append(" Eine Sandkastenrunde ist eine Runde wie jede andere: Raumcode eingeben und mitspielen.")
                },
                style = Schrift.Normal,
                color = Farben.TextLeise,
            )
        }
    }
}
