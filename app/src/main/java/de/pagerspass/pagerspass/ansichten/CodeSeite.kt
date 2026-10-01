package de.pagerspass.pagerspass.ansichten

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import de.pagerspass.pagerspass.mobil.Kontodienst
import de.pagerspass.pagerspass.mobil.Kontostand
import de.pagerspass.pagerspass.ui.bausteine.Blende
import de.pagerspass.pagerspass.ui.bausteine.Dialogbreite
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Die Seite hinter einem geteilten Code — `CodeView.vue`, als Blende.
 *
 * Wer `https://pagerspass.de/code/LEITSTELLE200` antippt, kommt hierher: erst
 * was es gibt, dann von wem, dann der Knopf. Im Web ist das eine eigene Seite,
 * weil sie auch ohne Konto erreichbar sein muss; in der App wartet der Link
 * bis nach der Anmeldung (`Tieflinks`) und braucht deshalb keinen eigenen Weg —
 * eine Blende über der Seite, auf der man gerade ist, genügt.
 *
 * <b>Ein Code ohne Creator ist kein Fehler.</b> Die meisten Aktionscodes haben
 * keine Vorschau; dann steht nur der Code da, und Einlösen sagt, was er bringt.
 */
@Composable
fun CodeBlende(
    code: String,
    stand: Kontostand,
    dienst: Kontodienst,
    beiShop: () -> Unit,
    beiSchliessen: () -> Unit,
) {
    var laedt by remember(code) { mutableStateOf(true) }
    var eingeloest by remember(code) { mutableStateOf(false) }
    val browser = LocalUriHandler.current

    LaunchedEffect(code) {
        dienst.rueckmeldungWeg(Kontodienst.CODE)
        dienst.codevorschau(code).join()
        laedt = false
    }

    // Erst nach dem Laden gelesen: Davor könnte noch die Vorschau aus dem Shop stehen.
    val vorschau = if (laedt) null else stand.codevorschau
    val meldung = stand.meldung(Kontodienst.CODE)
    val arbeitet = stand.laeuft == Kontodienst.CODE

    Blende(
        titel = "Code",
        beiSchliessen = {
            dienst.codevorschauWeg()
            beiSchliessen()
        },
        breite = Dialogbreite.Schmal,
        fuss = {
            when {
                eingeloest && meldung?.fehler == false ->
                    Knopf("In den Shop", beiShop, art = Knopfart.Haupt, breit = true)
                vorschau != null && !vorschau.einloesbar ->
                    Knopf("Schließen", beiSchliessen, art = Knopfart.Leise, breit = true)
                else -> Knopf(
                    if (arbeitet) "Wird eingelöst …" else "Jetzt einlösen",
                    {
                        eingeloest = true
                        dienst.codeEinloesen(code)
                    },
                    art = Knopfart.Haupt,
                    breit = true,
                    aktiv = !laedt && !arbeitet && !(eingeloest && meldung?.fehler == false),
                )
            }
        },
    ) {
        if (laedt) {
            SehrLeise("Der Code wird geprüft …")
            return@Blende
        }
        Text(
            code,
            style = Schrift.Titel.copy(fontFamily = Schrift.Mono, fontWeight = FontWeight.Bold),
            color = Farben.Amber,
        )
        if (vorschau != null) {
            SehrLeise("CODE VON", mono = true)
            Text(vorschau.creator, style = Schrift.Gross, color = Farben.Text)
            vorschau.kanal?.takeIf { it.isNotBlank() }?.let { kanal ->
                Textweg("Zum Kanal ›", { runCatching { browser.openUri(kanal) } })
            }
            Text(vorschau.belohnung, style = Schrift.Normal, color = Farben.Text)
            if (!vorschau.einloesbar) {
                Text(
                    vorschau.grund ?: "Dieser Code gilt gerade nicht.",
                    style = Schrift.Klein,
                    color = Farben.SignalHell,
                )
            }
        } else {
            SehrLeise("Ein Aktionscode. Was er bringt, steht nach dem Einlösen hier.")
        }
        if (eingeloest) Rueckmeldungszeile(meldung)
    }
}
