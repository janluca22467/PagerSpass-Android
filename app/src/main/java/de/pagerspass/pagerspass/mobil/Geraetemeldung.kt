package de.pagerspass.pagerspass.mobil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.pagerspass.pagerspass.netz.Begleitergeraete

/**
 * Meldet den Gerätestand dieses Platzes an das gekoppelte Handy nach — der
 * Beobachter aus `stores/spiel.ts` (`GeraeteMelden`).
 *
 * <b>Der Zugang trägt nur den Stand von damals.</b> Zwischen dem Erzeugen des
 * QR-Codes und dem Scannen kann eine Ewigkeit liegen, und wer danach Bauform,
 * Bauart oder Ton umstellt, sähe am Handy noch das alte Gerät. Deshalb steht die
 * Kopplung mit im Schlüssel: Sobald das Handy dran ist, geht der aktuelle Stand
 * hinaus, und danach bei jeder Änderung wieder.
 *
 * Ohne gekoppeltes Handy wird nichts geschickt — der Server verwürfe es ohnehin.
 * Still im Fehlerfall: Es ist eine Nachricht über das Aussehen eines Piepsers,
 * kein Kommando.
 *
 * @param gekoppelt `Rundenstand.begleiterGekoppelt`.
 * @param gesicht Das Meldergesicht aus dem Profil — `null`, solange es nicht geladen ist.
 * @param funkAusgelagert Ob der Funk dieses Platzes am Handy liegt.
 * @param zeigtAlles Die Leitstelle schickt ausdrücklich „alles zeigen" mit, wie ihr Zugang.
 */
@Composable
internal fun GeraeteNachmelden(
    runde: Runde,
    gekoppelt: Boolean,
    gesicht: String?,
    funkAusgelagert: Boolean,
    zeigtAlles: Boolean = false,
) {
    val zusammenhang = LocalContext.current
    val tonwahl = remember(zusammenhang) { Tonwahl.von(zusammenhang) }
    val bauform by rememberMelderBauform()
    val melderton by rememberMelderTon()

    val geraete = Begleitergeraete(
        bauform = bauform,
        bauart = tonwahl.bauart,
        melderton = melderton,
        gesicht = gesicht?.takeIf { it.isNotBlank() },
        zeigtMelder = if (zeigtAlles) true else null,
        zeigtFunkgeraet = if (zeigtAlles) true else null,
        zeigtFunkchat = if (zeigtAlles) true else null,
        funkAusgelagert = funkAusgelagert,
    )

    LaunchedEffect(gekoppelt, geraete) {
        if (gekoppelt) runCatching { runde.geraeteMelden(geraete) }
    }
}
