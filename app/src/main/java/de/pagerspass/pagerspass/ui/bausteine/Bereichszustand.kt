package de.pagerspass.pagerspass.ui.bausteine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Die drei Zustände, in denen ein Bereich sein kann, bevor er Inhalt hat.
 *
 * <b>Sie stehen als ein Baustein und nicht je Seite.</b> Sechs Seiten laden
 * Listen; wer den Ladezustand je Seite schreibt, hat beim vierten Mal einen
 * anderen Abstand, und beim fünften vergisst er den Fehlerfall. Genau das ist im
 * Web die Ursache für die Streuung gewesen, die `base.css` seitenweise
 * dokumentiert.
 *
 * <b>„Lädt" heißt nur beim ersten Mal „nichts zeigen".</b> Beim Nachladen bleibt
 * der alte Inhalt stehen — sonst springt die Seite unter dem Finger weg, sobald
 * jemand zurückkommt.
 */
@Composable
fun <T> Bereich(
    laedt: Boolean,
    fehler: String?,
    inhalt: T?,
    modifier: Modifier = Modifier,
    beiErneut: (() -> Unit)? = null,
    leer: (@Composable () -> Unit)? = null,
    istLeer: (T) -> Boolean = { false },
    zeigen: @Composable ColumnScope.(T) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier.fillMaxWidth(),
    ) {
        when {
            laedt && inhalt == null -> Ladezeile()

            fehler != null && inhalt == null -> Fehlerzeile(fehler, beiErneut)

            inhalt == null -> Unit

            istLeer(inhalt) && leer != null -> leer()

            else -> zeigen(inhalt)
        }
    }
}

/**
 * „Wird geladen …"
 *
 * Ein Ring und ein Wort. <b>Kein Skelett aus grauen Balken:</b> Das ist die
 * Bauform für Listen, deren Länge man vorher kennt — hier weiß niemand, ob drei
 * oder dreißig Zeilen kommen, und ein Skelett, das die falsche Zahl zeigt, ist
 * eine Auskunft, die gleich wieder zurückgenommen wird.
 */
@Composable
fun Ladezeile(text: String = "Wird geladen …", modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(vertical = Abstand.Gross),
    ) {
        CircularProgressIndicator(
            color = Farben.Amber,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp),
        )
        Text(text = text, style = Schrift.Klein, color = Farben.TextSehrLeise)
    }
}

/**
 * Was schiefging — und ein Weg zurück.
 *
 * <b>Der Satz kommt vom Server</b>, nicht von der App: Er ist für Menschen
 * geschrieben. Der Knopf daneben ist der Unterschied zwischen einer Meldung und
 * einer Sackgasse — die häufigste Ursache ist ein Netz, das eine Sekunde später
 * wieder da ist.
 */
@Composable
fun Fehlerzeile(text: String, beiErneut: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Abstand.Normal),
        modifier = modifier
            .fillMaxWidth()
            .flaeche(randfarbe = Farben.SignalTief)
            .padding(Abstand.Gross),
    ) {
        Text(text = text, style = Schrift.Klein, color = Farben.SignalHell)
        if (beiErneut != null) {
            Box { Knopf("Erneut versuchen", beiErneut, art = Knopfart.Leise, kompakt = true) }
        }
    }
}
