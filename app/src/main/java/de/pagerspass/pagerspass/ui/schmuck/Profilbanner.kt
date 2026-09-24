package de.pagerspass.pagerspass.ui.schmuck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.Ziel
import de.pagerspass.pagerspass.ui.theme.lichtkante

/**
 * Der Name eines Kontos — mit Stern und Haken, wenn sie ihm zustehen.
 *
 * Übertragen aus `web/src/components/Kontoname.vue`.
 *
 * <b>Der blaue Haken steht davor, der goldene Stern dahinter.</b> Das ist keine
 * Willkür: Der Haken sagt, *wer* da schreibt (die Verwaltung vergibt ihn), der
 * Stern sagt, *was* jemand hat. Das eine gehört vor den Namen, das andere
 * daneben.
 *
 * <b>Ein Abo färbt den Namen golden.</b> Auch ohne Stern — der Stern ist die
 * Wiederholung für den Fall, dass die Farbe allein nicht auffällt.
 */
@Composable
fun Kontoname(
    name: String,
    modifier: Modifier = Modifier,
    premium: Boolean = false,
    teammitglied: Boolean = false,
    stil: TextStyle = Schrift.Normal,
    maxZeilen: Int = 1,
) {
    // <b>Beide Zeichen stehen im Text, nicht daneben.</b> Als Geschwister in
    // einer Zeile sind sie an den Anfang und das Ende der ganzen Breite
    // gebunden — bei einem Namen, der auf zwei Zeilen umbricht, stand der Stern
    // damit rechts außen auf Höhe der ersten Zeile statt hinter dem letzten
    // Buchstaben. Als eingebetteter Inhalt wandert er mit dem Text.
    val hakenMarke = "haken"
    val sternMarke = "stern"

    val text = buildAnnotatedString {
        if (teammitglied) {
            appendInlineContent(hakenMarke, "✔")
            append(" ")
        }
        append(name)
        if (premium) {
            append(" ")
            appendInlineContent(sternMarke, "★")
        }
    }

    val hoehe = stil.fontSize * 0.95f
    val eingebettet = buildMap<String, InlineTextContent> {
        if (teammitglied) {
            put(
                hakenMarke,
                InlineTextContent(
                    Placeholder(hoehe, hoehe, PlaceholderVerticalAlign.TextCenter),
                ) {
                    Icon(HAKEN, contentDescription = "Team", tint = Farben.BlauHell)
                },
            )
        }
        if (premium) {
            put(
                sternMarke,
                InlineTextContent(
                    Placeholder(hoehe, hoehe, PlaceholderVerticalAlign.TextCenter),
                ) {
                    Icon(STERN, contentDescription = "Premium", tint = GOLD)
                },
            )
        }
    }

    Text(
        text = text,
        inlineContent = eingebettet,
        style = stil,
        // Ein Abo färbt den Namen golden — auch ohne Stern. Der Stern ist die
        // Wiederholung für den Fall, dass die Farbe allein nicht auffällt.
        color = if (premium) GOLD else Farben.Text,
        maxLines = maxZeilen,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * Das Profilbanner — der Kopf, der überall gleich aussieht.
 *
 * <b>Warum die eigene Kontoseite es überhaupt trägt.</b> Ein Muster, das man im
 * Konto aussucht und danach nur im fremden Profil zu sehen bekommt, ist ein
 * Kauf, den man selbst nie benutzt. Wer die Kontoseite öffnet, soll sein Konto
 * sehen und nicht die Vorlage, auf der jedes Konto gleich aussieht.
 *
 * <b>Das Band füllt die Karte und läuft nicht als Streifen darüber.</b> Ein Band
 * von 76 Punkten endete im Web auf halber Höhe des Wappens, und die Kante lief
 * quer durch das Gesicht der Karte.
 *
 * @param augenbraue Die Kleinschrift über dem Namen — „DEIN KONTO", „PROFIL".
 * @param werte Was unter dem Namen steht: Credits, Erfahrung, Schichten. Als
 *   Paare, weil die Reihenfolge bedeutungstragend ist und ein Kartenausschnitt
 *   sie nicht umsortieren soll.
 */
@Composable
fun Profilbanner(
    kennung: String,
    anzeigename: String,
    modifier: Modifier = Modifier,
    benutzername: String? = null,
    augenbraue: String? = null,
    wappen: String = "Keines",
    wappenfarbe: Int = 0,
    kopfmuster: String = "keines",
    profilrahmen: String = "keiner",
    bildAdresse: String? = null,
    premium: Boolean = false,
    teammitglied: Boolean = false,
    imDienst: Boolean = false,
    marken: List<String> = emptyList(),
    werte: List<Pair<String, String>> = emptyList(),
    knoepfe: (@Composable () -> Unit)? = null,
    inhalt: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val ton = Wappen.ton(kennung, wappenfarbe)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier
            .fillMaxWidth()
            .clip(Rundung.Normal)
            .background(Farben.Flaeche)
            .kopfband(kopfmuster, ton)
            .border(1.dp, Farben.Rand, Rundung.Normal)
            .lichtkante()
            .padding(Abstand.Gross),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Gross),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Kontobild(
                kennung = kennung,
                anzeigename = anzeigename,
                wappen = wappen,
                wappenfarbe = wappenfarbe,
                bildAdresse = bildAdresse,
                rahmen = profilrahmen,
                groesse = 84.dp,
                imDienst = imDienst,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                modifier = Modifier.weight(1f),
            ) {
                if (augenbraue != null) Etikett(augenbraue)

                Kontoname(
                    name = anzeigename.ifBlank { "—" },
                    premium = premium,
                    teammitglied = teammitglied,
                    stil = Schrift.Titel,
                    maxZeilen = 2,
                )

                if (benutzername != null) {
                    Text(
                        text = "@$benutzername",
                        style = Schrift.MonoKlein,
                        color = Farben.TextLeise,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (marken.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                marken.forEach { Marke(it) }
            }
        }

        if (werte.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.SehrGross),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                werte.forEach { (was, wert) ->
                    Column(verticalArrangement = Arrangement.spacedBy(Abstand.Haar)) {
                        Etikett(was)
                        Text(text = wert, style = Schrift.Gross, color = Farben.Text)
                    }
                }
            }
        }

        inhalt?.invoke(this)

        if (knoepfe != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            ) {
                knoepfe()
            }
        }
    }
}

/**
 * Eine Listenzeile mit Kontobild und Band — Freunde, Bestenliste, Mitglieder.
 *
 * <b>Dasselbe Band wie im Kopf, nur leiser</b> (45 %, siehe `kopfband`). Damit
 * sagt jede Zeile weiterhin, wem sie gehört, ohne dass zehn Zeilen untereinander
 * zur Tapete werden.
 *
 * <b>Und die leiseste Schrift steigt darauf eine Stufe.</b> Das ist der zweite
 * Teil derselben Messung: Auf dem Band hält `TextSehrLeise` die Grenze nicht
 * mehr, `TextLeise` schon. Deshalb nimmt die Unterzeile hier `TextLeise` — und
 * nicht, weil sie wichtiger wäre als anderswo.
 */
@Composable
fun Profilzeile(
    kennung: String,
    anzeigename: String,
    modifier: Modifier = Modifier,
    unterzeile: String? = null,
    wappen: String = "Keines",
    wappenfarbe: Int = 0,
    kopfmuster: String = "keines",
    profilrahmen: String = "keiner",
    bildAdresse: String? = null,
    premium: Boolean = false,
    teammitglied: Boolean = false,
    imDienst: Boolean = false,
    vorn: (@Composable () -> Unit)? = null,
    hinten: (@Composable () -> Unit)? = null,
    beiDruck: (() -> Unit)? = null,
    unten: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val ton = Wappen.ton(kennung, wappenfarbe)

    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = modifier
            .fillMaxWidth()
            .clip(Rundung.Klein)
            .background(Farben.Flaeche)
            .kopfband(kopfmuster, ton, zeile = true)
            .border(1.dp, Farben.Rand, Rundung.Klein)
            .then(
                if (beiDruck != null) {
                    Modifier.clickable(
                        onClick = beiDruck,
                        role = Role.Button,
                        indication = null,
                        interactionSource = null,
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Ziel.Normal),
        ) {
            vorn?.invoke()

            Kontobild(
                kennung = kennung,
                anzeigename = anzeigename,
                wappen = wappen,
                wappenfarbe = wappenfarbe,
                bildAdresse = bildAdresse,
                rahmen = profilrahmen,
                imDienst = imDienst,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                modifier = Modifier.weight(1f),
            ) {
                Kontoname(name = anzeigename, premium = premium, teammitglied = teammitglied)
                if (unterzeile != null) {
                    Text(
                        text = unterzeile,
                        style = Schrift.Klein,
                        // Eine Stufe lauter als sonst — siehe oben.
                        color = Farben.TextLeise,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            hinten?.invoke()
        }

        unten?.invoke(this)
    }
}

/** Das Gold, in dem ein Abo-Name gesetzt wird. */
private val GOLD = Color(0xFFE0B84F)

private val STERN: ImageVector = zeichen(
    "stern",
    "M12 2.5l2.9 5.9 6.5.9-4.7 4.6 1.1 6.5L12 17.3l-5.8 3.1 1.1-6.5L2.6 9.3l6.5-.9z",
)

private val HAKEN: ImageVector = zeichen(
    "team",
    "M12 2.2l2.3 1.7 2.8-.3 1 2.7 2.5 1.3-.6 2.8 1.7 2.3-1.7 2.3.6 2.8-2.5 1.3-1 2.7-2.8-.3" +
        "L12 21.8 9.7 20l-2.8.3-1-2.7-2.5-1.3.6-2.8L2.3 11.2 4 8.9l-.6-2.8 2.5-1.3 1-2.7 2.8.3z",
    "M8.4 12.1l2.4 2.4 4.8-4.8",
)

private fun zeichen(name: String, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        pfade.forEachIndexed { nr, d ->
            // Der erste Pfad ist die Fläche, jeder weitere die Zeichnung darin —
            // beim Haken das Häkchen im Siegel. Es wird in der Grundfarbe der
            // Seite gestochen, damit es auf jedem Untergrund gleich aussieht.
            addPath(
                pathData = addPathNodes(d),
                fill = if (nr == 0) SolidColor(Color.White) else null,
                stroke = if (nr == 0) null else SolidColor(Farben.Bg),
                strokeLineWidth = 2.2f,
            )
        }
    }.build()
