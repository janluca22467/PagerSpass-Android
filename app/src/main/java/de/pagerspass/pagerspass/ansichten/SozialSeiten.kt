package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Sozialstand
import de.pagerspass.pagerspass.netz.BrettSeite
import de.pagerspass.pagerspass.netz.Bretteintrag
import de.pagerspass.pagerspass.netz.Brettkommentar
import de.pagerspass.pagerspass.netz.Freund
import de.pagerspass.pagerspass.netz.Nachricht
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Textweg
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

/**
 * Brett und Gespräch — übertragen aus dem Freundebereich des Web.
 *
 * <b>Das Brett ist die Bühne, die Quittung die einzige Reaktion.</b> Getippte
 * Beiträge und automatische Erfolge stehen in einem Strom; geblättert wird
 * über die Nummer der letzten Zeile, nicht über einen Versatz.
 */

/**
 * Was eine Karte am Brett außer Quittieren und Öffnen noch kann.
 *
 * Eine eigene Klasse, weil dieselben drei Griffe an drei Stellen gebraucht
 * werden — im Strom, im einzelnen Eintrag und auf der Zeitleiste eines Profils.
 */
class Brettgriffe(
    /** Das Profil des Verfassers öffnen — über den Benutzernamen. */
    val profil: (String) -> Unit = {},
    /** Den eigenen Eintrag zurücknehmen. */
    val entfernen: (Long) -> Unit = {},
    /** Einen fremden Eintrag melden. */
    val melden: (Long) -> Unit = {},
)

/** Der Brett-Strom — als Teil der Freundeseite. */
@Composable
fun ColumnScope.BrettTeil(
    brett: Bereichsstand<BrettSeite>,
    reiter: String,
    hatWache: Boolean,
    darfOeffentlich: Boolean,
    beiReiter: (String) -> Unit,
    beiLaden: () -> Unit,
    beiMehr: () -> Unit,
    beiSchreiben: (String, String?) -> Unit,
    beiQuittieren: (Long) -> Unit,
    beiOeffnen: (Long) -> Unit,
    server: String = "",
    griffe: Brettgriffe = Brettgriffe(),
) {
    LaunchedEffect(Unit) { beiLaden() }
    var text by remember { mutableStateOf("") }

    // Verfassen — oben, wie im Web. Die Sichtbarkeit folgt dem Kreis-Sieb.
    Kasten(abstandInnen = Abstand.Klein) {
        Feld(
            wert = text,
            beiAenderung = { text = it.take(500) },
            platzhalter = "Was gibt's von dir?",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SehrLeise("${text.length}/500", modifier = Modifier.weight(1f))
            Knopf(
                "Anschlagen",
                {
                    beiSchreiben(
                        text.trim(),
                        when (reiter) {
                            "Wache" -> "Wache"
                            "Alle" -> if (darfOeffentlich) "Oeffentlich" else null
                            else -> "Freunde"
                        },
                    )
                    text = ""
                },
                aktiv = text.isNotBlank(),
                kompakt = true,
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
        Pille("Freunde", an = reiter == "Freunde", beiDruck = { beiReiter("Freunde") })
        if (hatWache) {
            Pille("Wache", an = reiter == "Wache", beiDruck = { beiReiter("Wache") })
        }
        Pille("Alle", an = reiter == "Alle", beiDruck = { beiReiter("Alle") })
    }

    Bereich(
        laedt = brett.laedt,
        fehler = brett.fehler,
        inhalt = brett.inhalt,
        beiErneut = beiLaden,
    ) { seite ->
        if (seite.eintraege.isEmpty()) {
            Leerhinweis("Noch nichts am Brett. Der erste Anschlag gehört dir.")
        } else {
            seite.eintraege.forEach { eintrag ->
                EintragKarte(
                    eintrag = eintrag,
                    server = server,
                    beiQuittieren = { beiQuittieren(eintrag.nr) },
                    beiOeffnen = { beiOeffnen(eintrag.nr) },
                    griffe = griffe,
                )
            }
            if (seite.weiter != null) {
                Knopf("Mehr laden", beiMehr, art = Knopfart.Leise, kompakt = true)
            }
        }
    }
}

/** Wie eine automatische Zeile sich ansagt — Zeichen und Wort, wie im Web. */
private fun artzeile(art: String): String? = when (art) {
    "Schicht" -> "🚒 Schicht"
    "Befoerderung" -> "🎖 Beförderung"
    "Abzeichen" -> "🏅 Abzeichen"
    "Rekord" -> "⏱ Bestmarke"
    "Wachenstufe" -> "🏠 Wache"
    "Gemeinschaftsbeitritt" -> "🤝 Beigetreten"
    else -> null
}

/**
 * Eine Karte am Brett — Beitrag oder Erfolg.
 *
 * Erfolge tragen einen Titel und keinen Text; die Quittung ist ein Umschalter
 * und springt sofort, geprüft wird hinterher.
 *
 * <b>Rechts unten steht genau ein Griff, der etwas wegnimmt</b>: am eigenen
 * Eintrag „Zurücknehmen", am fremden „Melden". Beide fragen im zweiten Tipp
 * nach — ein Eintrag, der beim Vorbeiscrollen verschwindet, ist verloren.
 */
@Composable
fun EintragKarte(
    eintrag: Bretteintrag,
    beiQuittieren: () -> Unit,
    beiOeffnen: (() -> Unit)?,
    server: String = "",
    griffe: Brettgriffe = Brettgriffe(),
) {
    var gefragt by remember(eintrag.nr) { mutableStateOf(false) }
    val v = eintrag.verfasser

    Kasten(
        marke = eintrag.vonMir,
        abstandInnen = Abstand.Klein,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (v.benutzername.isNotBlank()) {
                        Modifier.clickable(role = Role.Button) { griffe.profil(v.benutzername) }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Kontobild(
                kennung = v.kennung,
                anzeigename = v.anzeigename.ifBlank { v.benutzername },
                wappen = v.wappen ?: "Keines",
                wappenfarbe = v.wappenfarbe,
                bildAdresse = bildweg(server, v.profilbild),
            )
            Column(modifier = Modifier.weight(1f)) {
                Kontoname(
                    name = v.anzeigename.ifBlank { v.benutzername },
                    premium = v.premium,
                    teammitglied = v.teammitglied,
                    stil = Schrift.Klein,
                )
                if (v.benutzername.isNotBlank()) SehrLeise(v.benutzername, mono = true)
            }
            // Der Kreis steht nur am eigenen Eintrag — an einem fremden wäre er
            // eine Angabe über jemand anderen, die niemand braucht.
            if (eintrag.vonMir) Marke(kreisname(eintrag.sichtbarkeit))
            SehrLeise(seither(eintrag.erstelltUm) ?: "", mono = true)
        }

        artzeile(eintrag.art)?.let { SehrLeise(it) }
        eintrag.titel?.let {
            Text(text = it, style = Schrift.Normal, color = Farben.AmberHell)
        }
        eintrag.text?.let {
            Text(text = it, style = Schrift.Klein, color = Farben.Text)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Textweg(
                "✓✓ Quittiert" + (if (eintrag.quittungen > 0) " ${eintrag.quittungen}" else ""),
                beiQuittieren,
                farbe = if (eintrag.vonMirQuittiert) Farben.Amber else Farben.TextLeise,
            )
            if (beiOeffnen != null) {
                Textweg(
                    "Kommentare" + (if (eintrag.kommentare > 0) " ${eintrag.kommentare}" else ""),
                    beiOeffnen,
                    farbe = Farben.TextLeise,
                )
            }
            Spacer(Modifier.weight(1f))
            Textweg(
                when {
                    eintrag.vonMir && gefragt -> "Wirklich zurücknehmen?"
                    eintrag.vonMir -> "Zurücknehmen"
                    gefragt -> "Wirklich melden?"
                    else -> "Melden"
                },
                {
                    if (!gefragt) {
                        gefragt = true
                    } else {
                        gefragt = false
                        if (eintrag.vonMir) griffe.entfernen(eintrag.nr) else griffe.melden(eintrag.nr)
                    }
                },
                farbe = if (gefragt) Farben.SignalHell else Farben.TextSehrLeise,
            )
        }
    }
}

private fun kreisname(sichtbarkeit: String): String = when (sichtbarkeit) {
    "Oeffentlich" -> "Öffentlich"
    "Wache" -> "Wache"
    else -> "Freunde"
}

/** Ein Eintrag mit seinen Kommentaren — die eigene Adresse des Push-Ziels. */
@Composable
fun EintragSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    nr: Long = 0,
    eintrag: Bereichsstand<Bretteintrag?> = Bereichsstand(),
    kommentare: List<Brettkommentar> = emptyList(),
    server: String = "",
    meldung: String? = null,
    hinweis: String? = null,
    beiLaden: (Long) -> Unit = {},
    beiQuittieren: (Long) -> Unit = {},
    beiKommentieren: (Long, String) -> Unit = { _, _ -> },
    beiKommentarMelden: (Long) -> Unit = {},
    beiMeldungWeg: () -> Unit = {},
    griffe: Brettgriffe = Brettgriffe(),
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(nr) { beiLaden(nr) }
    var text by remember { mutableStateOf("") }
    var gemeldet by remember { mutableStateOf<Long?>(null) }

    Seite(modifier = modifier, unterrand = unterrand) {
        Seitenkopf(
            titel = "Eintrag",
            unterzeile = "Vom Brett",
            knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
        )

        Meldungszeile(meldung, hinweis, beiMeldungWeg)

        Bereich(
            laedt = eintrag.laedt,
            fehler = eintrag.fehler,
            inhalt = eintrag.inhalt,
            beiErneut = { beiLaden(nr) },
        ) { e ->
            EintragKarte(
                eintrag = e,
                server = server,
                beiQuittieren = { beiQuittieren(e.nr) },
                beiOeffnen = null,
                griffe = griffe,
            )

            kommentare.forEach { kommentar ->
                val k = kommentar.verfasser
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .flaeche(ecke = 9.dp)
                        .padding(Abstand.Normal),
                ) {
                    Kontobild(
                        kennung = k.kennung,
                        anzeigename = k.anzeigename.ifBlank { k.benutzername },
                        wappen = k.wappen ?: "Keines",
                        wappenfarbe = k.wappenfarbe,
                        bildAdresse = bildweg(server, k.profilbild),
                        groesse = 28.dp,
                        modifier = Modifier.clickable(role = Role.Button) {
                            if (k.benutzername.isNotBlank()) griffe.profil(k.benutzername)
                        },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = k.anzeigename.ifBlank { k.benutzername },
                                style = Schrift.Winzig.copy(fontFamily = Schrift.Mono),
                                color = if (kommentar.vonMir) Farben.Amber
                                else Farben.TextSehrLeise,
                                modifier = Modifier.weight(1f),
                            )
                            SehrLeise(seither(kommentar.erstelltUm) ?: "", mono = true)
                        }
                        Text(text = kommentar.text, style = Schrift.Klein, color = Farben.Text)
                        if (!kommentar.vonMir) {
                            Textweg(
                                if (gemeldet == kommentar.nr) "Wirklich melden?" else "Melden",
                                {
                                    if (gemeldet != kommentar.nr) {
                                        gemeldet = kommentar.nr
                                    } else {
                                        gemeldet = null
                                        beiKommentarMelden(kommentar.nr)
                                    }
                                },
                                farbe = if (gemeldet == kommentar.nr) Farben.SignalHell
                                else Farben.TextSehrLeise,
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Feld(
                    wert = text,
                    beiAenderung = { text = it.take(300) },
                    platzhalter = "Kommentieren …",
                    modifier = Modifier.weight(1f),
                )
                Knopf(
                    "Senden",
                    {
                        beiKommentieren(nr, text.trim())
                        text = ""
                    },
                    aktiv = text.isNotBlank(),
                    kompakt = true,
                )
            }
        }
    }
}

/**
 * Das Gespräch — der Chatverlauf mit einem Freund, als eigene Seite.
 *
 * <b>Ankommen gilt als Lesen</b>, solange die Seite offen ist — deshalb muss
 * sie sich beim Verlassen abmelden, sonst quittiert sie weiter.
 *
 * <b>Oben steht, mit wem man spricht</b>, und die Zeile führt in sein Profil —
 * dieselbe Auskunft wie in der Gesprächsliste, an derselben Stelle. Fährt er
 * gerade, steht „Dazuschalten" daneben.
 */
@Composable
fun GespraechSeite(
    modifier: Modifier = Modifier,
    unterrand: Dp = 0.dp,
    partnerKennung: String = "",
    partnerName: String = "",
    /** Der Freund aus der Liste — `null`, solange sie nicht geladen ist. */
    partner: Freund? = null,
    server: String = "",
    sozial: Sozialstand = Sozialstand(),
    meineKennung: String = "",
    /** Das Geschenk, das gerade geöffnet wird. */
    oeffnetGeschenk: Long? = null,
    meldung: String? = null,
    hinweis: String? = null,
    beiOeffnen: (String) -> Unit = {},
    beiSchliessen: () -> Unit = {},
    beiSenden: (String, String) -> Unit = { _, _ -> },
    beiTermin: (Long, Boolean) -> Unit = { _, _ -> },
    beiGeschenk: (Long) -> Unit = {},
    beiProfil: (String) -> Unit = {},
    beiDazuschalten: (String) -> Unit = {},
    beiMeldungWeg: () -> Unit = {},
    beiZurueck: () -> Unit = {},
) {
    LaunchedEffect(partnerKennung) { beiOeffnen(partnerKennung) }
    DisposableEffect(Unit) {
        onDispose { beiSchliessen() }
    }
    var text by remember { mutableStateOf("") }

    Seite(modifier = modifier, unterrand = unterrand) {
        if (partner == null) {
            Seitenkopf(
                titel = partnerName.ifBlank { "Gespräch" },
                unterzeile = "Direktnachrichten",
                knoepfe = { Knopf("Zurück", beiZurueck, art = Knopfart.Leise, kompakt = true) },
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Knopf("‹", beiZurueck, art = Knopfart.Leise, kompakt = true)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Button) { beiProfil(partner.benutzername) },
                ) {
                    Kontobild(
                        kennung = partner.kennung,
                        anzeigename = partner.anzeigename,
                        wappen = partner.wappen,
                        wappenfarbe = partner.wappenfarbe,
                        bildAdresse = bildweg(server, partner.profilbild),
                        imDienst = partner.anwesenheit != null,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Kontoname(
                            name = partner.anzeigename,
                            premium = partner.premium,
                            teammitglied = partner.teammitglied,
                        )
                        Text(
                            text = lage(partner.anwesenheit, partner.zuletztGesehen)
                                .ifBlank { partner.benutzername },
                            style = Schrift.MonoKlein,
                            color = if (partner.anwesenheit != null) Farben.GruenHell
                            else Farben.TextSehrLeise,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                partner.anwesenheit?.let { wo ->
                    Knopf(
                        if (wo.platzFrei) "Dazuschalten" else "Runde voll",
                        { beiDazuschalten(wo.roomCode) },
                        aktiv = wo.platzFrei,
                        kompakt = true,
                    )
                }
            }
        }

        Meldungszeile(meldung, hinweis, beiMeldungWeg)

        if (sozial.laedt) {
            SehrLeise("Der Verlauf wird geladen …")
        } else if (sozial.verlauf.isEmpty()) {
            Leerhinweis("Noch nichts geschrieben. Der erste Satz gehört dir.")
        }

        sozial.verlauf.forEach { nachricht ->
            Sprechblase(
                nachricht = nachricht,
                eigene = nachricht.von == meineKennung,
                oeffnetGeschenk = oeffnetGeschenk,
                beiTermin = beiTermin,
                beiGeschenk = beiGeschenk,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Feld(
                wert = text,
                beiAenderung = { text = it.take(500) },
                platzhalter = "Schreib was …",
                modifier = Modifier.weight(1f),
            )
            Knopf(
                "Senden",
                {
                    beiSenden(partnerKennung, text)
                    text = ""
                },
                aktiv = text.isNotBlank(),
                kompakt = true,
            )
        }
    }
}

/**
 * Eine Sprechblase — eigene rechts in Amber, fremde links auf Fläche.
 *
 * Der Doppelhaken ist die Lesebestätigung; ob sie kommt, entscheidet die
 * Privatsphäre des Partners, nicht die App.
 *
 * <b>Ein Geschenk bekommt eine eigene Karte</b>, wie der Terminvorschlag.
 * Solange es zu ist, steht darin nicht, was es ist — das verrät auch der Server
 * erst beim Öffnen.
 */
@Composable
private fun Sprechblase(
    nachricht: Nachricht,
    eigene: Boolean,
    oeffnetGeschenk: Long?,
    beiTermin: (Long, Boolean) -> Unit,
    beiGeschenk: (Long) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (eigene) Spacer(Modifier.weight(0.2f))
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
            modifier = Modifier
                .weight(0.8f)
                .background(
                    if (eigene) Farben.Amber.copy(alpha = 0.14f) else Farben.Flaeche,
                    RoundedCornerShape(10.dp),
                )
                .padding(Abstand.Normal),
        ) {
            // Ein Terminvorschlag in der Zeile — mit Antwortknöpfen beim
            // Empfänger, solange er offen ist.
            if (nachricht.terminText != null || nachricht.terminZeitpunkt != null) {
                Text(
                    text = "📅 ${nachricht.terminText ?: "Termin"}" +
                        (nachricht.terminZeitpunkt?.let { " · ${zeitpunkt(it)}" } ?: ""),
                    style = Schrift.Klein,
                    color = Farben.AmberHell,
                )
                when (nachricht.terminStatus) {
                    "Zugesagt" -> SehrLeise("Zugesagt ✓")
                    "Abgesagt" -> SehrLeise("Abgesagt")
                    else -> if (!eigene) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                            Knopf("Zusagen", { beiTermin(nachricht.nr, true) }, kompakt = true)
                            Knopf(
                                "Absagen",
                                { beiTermin(nachricht.nr, false) },
                                art = Knopfart.Leise,
                                kompakt = true,
                            )
                        }
                    }
                }
            }

            nachricht.geschenk?.let { g ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(Abstand.Winzig),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (g.geoeffnet) Farben.Rand else Farben.AmberTief,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(Abstand.Klein),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                        Text("🎁 Geschenk", style = Schrift.MonoKlein, color = Farben.AmberHell)
                        if (g.geoeffnet) SehrLeise("geöffnet", mono = true)
                    }
                    Text(
                        text = when {
                            g.geoeffnet -> g.artikelName ?: "Geöffnet"
                            g.fuerMich -> "Du hast ein Geschenk erhalten."
                            else -> "Noch nicht geöffnet."
                        },
                        style = Schrift.Klein,
                        color = Farben.Text,
                    )
                    if (g.fuerMich && !g.geoeffnet) {
                        Knopf(
                            if (oeffnetGeschenk == g.nr) "Wird geöffnet …" else "Öffnen",
                            { beiGeschenk(g.nr) },
                            art = Knopfart.Haupt,
                            aktiv = oeffnetGeschenk == null,
                            kompakt = true,
                        )
                    }
                }
            }

            if (nachricht.text.isNotBlank()) {
                Text(text = nachricht.text, style = Schrift.Klein, color = Farben.Text)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(Modifier.weight(1f))
                SehrLeise(uhrzeit(nachricht.gesendetUm), mono = true)
                if (eigene && nachricht.gelesenUm != null) {
                    Text(
                        text = "✓✓",
                        style = Schrift.Winzig,
                        color = Farben.Amber,
                    )
                }
            }
        }
        if (!eigene) Spacer(Modifier.weight(0.2f))
    }
}
