package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ansichten.melder.Melderbild
import de.pagerspass.pagerspass.ansichten.melder.seitenverhaeltnis
import de.pagerspass.pagerspass.melder.Melderkatalog
import de.pagerspass.pagerspass.melder.Melderspieler
import de.pagerspass.pagerspass.mobil.Garagendaten
import de.pagerspass.pagerspass.netz.Fahrzeugvorlage
import de.pagerspass.pagerspass.netz.Shopartikel
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Etikett
import de.pagerspass.pagerspass.ui.bausteine.Feld
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Leise
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Pille
import de.pagerspass.pagerspass.ui.bausteine.Pillenreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.schmuck.Kontobild
import de.pagerspass.pagerspass.ui.schmuck.Kontoname
import de.pagerspass.pagerspass.ui.schmuck.Schmuck
import de.pagerspass.pagerspass.ui.schmuck.Wappen
import de.pagerspass.pagerspass.ui.schmuck.kopfband
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche

// ------------------------------------------------------------------ Vitrine

/**
 * Die Vitrine des Premium-Schaufensters — `PremiumSchaufenster.vue`.
 *
 * <b>Die Stücke selbst, keine Sätze über sie.</b> Ein Rahmen, ein Melder, ein
 * Muster lassen sich nicht in Worten zeigen; jede Kachel stellt deshalb die
 * echten Bausteine auf ihre Bühne — dasselbe `Kontobild`, dasselbe
 * `Melderbild`, dasselbe `kopfband`, die im Profil und im Dienst stehen. Eine
 * gemalte Vorschau wäre ein Versprechen, das das Profil danach einlösen müsste.
 *
 * Was kein Bild hat (Werkstatt, Klang, Schichtkarte), behält seinen Satz und
 * zeigt seine Teile als Pillen — wie im Web.
 */
@Composable
internal fun Vitrine() {
    val rahmen = Schmuck.RAHMEN.filter { it.premium }
    val muster = Schmuck.KOPFMUSTER.filter { it.premium }
    val gesichter = Melderkatalog.GESICHTER.filter { it.premium }
    val ton = Wappen.ton("schaufenster", 3)

    Abschnitt("Im Fenster") {
        Stueck("Dein Kartenkopf", "neu", "Statt eines gezeichneten Musters liegt hinter deinem Profilkopf eine " +
            "echte Karte — den Ausschnitt schiebst du dir selbst zurecht. Näher als eine Ortsansicht geht es nicht.") {
            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Farben.BgTief, RoundedCornerShape(9.dp))
                    .kopfband("premium-lagekarte", ton)
                    .padding(Abstand.Normal),
            ) {
                Column {
                    Text("Wo du Dienst tust", style = Schrift.Klein.copy(fontWeight = FontWeight.Bold), color = Farben.Text)
                    Text("Dein Ausschnitt, hinter deinem Namen.", style = Schrift.Winzig, color = Farben.TextLeise)
                }
            }
        }

        Stueck("Profilrahmen", "${rahmen.size}", "Zwei davon leuchten: Beim Lauflicht wandert der Schein um dein " +
            "Wappen, beim Nordlicht wechselt er die Farbe.") {
            Buehne {
                rahmen.forEach { r ->
                    Kontobild(kennung = "schaufenster", anzeigename = "P S", rahmen = r.id, wappenfarbe = 3, groesse = 44.dp)
                }
            }
        }

        Stueck("Melder-Gesichter", "${gesichter.size}", "Die Farbe gilt auf jedem Gerät — vom Piepser über die " +
            "Einsatzuhr bis zum Wandtableau.") {
            Buehne {
                gesichter.forEach { g ->
                    Melderbild(
                        bauform = "dienst",
                        gesicht = g.id,
                        alarm = null,
                        modifier = Modifier.width(44.dp).aspectRatio(seitenverhaeltnis("dienst")),
                    )
                }
            }
        }

        Stueck("Kopfmuster", "${muster.size}", "Das Farbband über deinem Profil — und als eines davon der Kartenkopf oben.") {
            Column(verticalArrangement = Arrangement.spacedBy(Abstand.Klein)) {
                muster.take(4).forEach { m ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .background(Farben.BgTief, RoundedCornerShape(6.dp))
                            .kopfband(m.id, ton)
                            .padding(horizontal = Abstand.Normal),
                    ) { Text(m.name, style = Schrift.Winzig, color = Farben.Text) }
                }
            }
        }

        Stueck("Titel & goldener Name", null, "Dein Name wird golden und trägt einen Stern — am Brett, in der " +
            "Lobby und in jeder Mannschaftsliste.") {
            Column(Modifier.fillMaxWidth().background(Farben.BgTief, RoundedCornerShape(9.dp)).padding(Abstand.Normal)) {
                Kontoname("Alex Bremer", premium = true, stil = Schrift.Gross)
                Text("Einsatzleitung", style = Schrift.Winzig, color = Farben.TextLeise)
            }
        }

        Stueck("Geräte", null, "Drei andere Arten Melder — quer in der Hand, am Handgelenk, im Flur — und acht " +
            "Gehäuse für dein Funkgerät, am Rechner wie am Handy. Am Funk ändert sich nichts.") {
            Buehne {
                listOf("quad", "uhr", "monitor").forEach { b ->
                    Melderbild(
                        bauform = b,
                        gesicht = "premium",
                        alarm = null,
                        modifier = Modifier.height(52.dp).aspectRatio(seitenverhaeltnis(b)),
                    )
                }
            }
        }

        Stueck("Werkstatt · Gehäuse", "18", "In der Gehäusewerkstatt ziehst du dir deinen Melder selbst zusammen — " +
            "achtzehn Bauteile, und es ist ein echtes Gerät. Gebaut wird am Rechner, getragen überall.") {
            Pillenreihe {
                listOf("Anzeigefeld", "Signalleuchte", "Quittiertaste", "Antenne", "Typenschild").forEach {
                    Pille(it, false, {}, aktiv = false)
                }
            }
        }

        Stueck("Werkstatt · Klang", "2", "Deinen Melderton klickst du ins Raster oder bringst ihn mit — als Datei " +
            "oder über dein Mikrofon. Alles bleibt auf deinem Gerät.") {
            Pillenreihe {
                listOf("Raster zeichnen", "Datei hochladen", "Selbst einsprechen", "Schneiden").forEach {
                    Pille(it, false, {}, aktiv = false)
                }
            }
        }

        Stueck("Handy-Funkbegleiter", null, "Dein Handy wird per QR-Code zum Funkgerät und Melder deines Platzes — " +
            "ohne zweite Anmeldung, mit Mitteilung bei Alarm auch bei dunklem Bildschirm.") {}

        Stueck("Schichtkarte", null, "Dazu deine eigene Zeile auf dem Bild, das du nach der Schicht teilst — dein " +
            "Motto, unter dem Namen der Leitstelle.") {}
    }
}

/** Eine Kachel der Vitrine: Titel, Zahl, Bühne, Satz. */
@Composable
private fun Stueck(titel: String, zahl: String?, satz: String, buehne: @Composable () -> Unit) {
    Kasten(abstandInnen = Abstand.Klein) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titel, style = Schrift.Normal, color = Farben.Text, modifier = Modifier.weight(1f))
            zahl?.let { SehrLeise(it, mono = true) }
        }
        buehne()
        SehrLeise(satz)
    }
}

/** Die getönte Bühne, auf der die Stücke nebeneinander stehen. */
@Composable
private fun Buehne(inhalt: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Abstand.Klein),
        verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
        modifier = Modifier
            .fillMaxWidth()
            .background(Farben.BgTief, RoundedCornerShape(9.dp))
            .padding(Abstand.Normal),
    ) { inhalt() }
}

// ---------------------------------------------------------------- Warenbühne

/**
 * Die Bühne einer Ware im Sortiment — `#buehne` in `ShopView.vue`: das Stück
 * selbst unter einem leisen Strahler, kein Sinnbild. Ein Ton hat kein Bild;
 * dort steht der Knopf, der ihn vorspielt.
 */
@Composable
internal fun Warenbuehne(a: Shopartikel) {
    val zusammenhang = LocalContext.current
    var laeuft by remember(a.stueckId) { mutableStateOf(false) }
    DisposableEffect(a.stueckId) { onDispose { if (laeuft) Melderspieler.stoppen("probe") } }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .background(Farben.BgTief, RoundedCornerShape(9.dp))
            .padding(Abstand.Klein),
    ) {
        when (a.art) {
            "Meldergesicht" -> Melderbild(
                bauform = "dienst",
                gesicht = a.stueckId,
                alarm = null,
                modifier = Modifier.height(72.dp).aspectRatio(seitenverhaeltnis("dienst")),
            )
            "Profilrahmen" -> Kontobild(
                kennung = "schaufenster",
                anzeigename = "P S",
                rahmen = a.stueckId,
                wappenfarbe = 3,
                groesse = 56.dp,
            )
            "Kopfmuster" -> Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .kopfband(a.stueckId, Wappen.ton("schaufenster", 3)),
            )
            "Wappenfarbe" -> Box(
                Modifier
                    .size(52.dp)
                    .background(Wappen.ton("", a.stueckId.toIntOrNull() ?: 0), CircleShape)
                    .border(2.dp, Farben.Rand, CircleShape),
            )
            "Melderton" -> Knopf(
                if (laeuft) "■ Läuft …" else "▶ Anhören",
                {
                    if (laeuft) {
                        Melderspieler.stoppen("probe")
                        laeuft = false
                    } else {
                        Melderspieler.probe(zusammenhang, a.stueckId, 1)
                        laeuft = true
                    }
                },
                art = Knopfart.Leise,
                kompakt = true,
            )
            "Titel" -> Text("„${a.name}“", style = Schrift.Gross.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF4D35E))
            else -> Unit
        }
    }
}

// ---------------------------------------------------------------- Autohaus

/**
 * Das Autohaus im Shop — `components/garage/Autohaus.vue`.
 *
 * <b>Es wohnt im Shop</b>, wie im Web; die Garage zeigt den Bestand. Oben die
 * Suche und die Reihen (nach Kategorie), darunter das Schaufenster mit dem
 * Wagen, den man gerade ansieht — es passiert ein-, zweimal je Abend und gilt
 * dauerhaft, also darf man das Fahrzeug einmal ganz ansehen, bevor man es holt.
 *
 * <b>Bezahlt wird nur mit Spielwährung:</b> mit einem Gutschein aus dem Aufstieg
 * oder mit Credits. Echtes Geld kommt hier nicht vor. Ein Kauf will zweimal
 * gedrückt sein (`kaufknopf`).
 */
@Composable
internal fun Autohaus(
    garage: Garagendaten?,
    katalog: List<Fahrzeugvorlage>,
    credits: Int,
    laeuft: Boolean,
    vorwahl: String?,
    beiHolen: (String, Boolean) -> Unit,
) {
    var suche by rememberSaveable { mutableStateOf("") }
    var gewaehlt by rememberSaveable(vorwahl) { mutableStateOf(vorwahl) }
    var scharf by remember { mutableStateOf<String?>(null) }

    if (garage == null) {
        SehrLeise("Das Autohaus wird geladen …")
        return
    }
    val nachId = katalog.associateBy { it.id }
    val gutscheine = garage.stand.offeneWahlen
    val auswahl = garage.angebot

    Kasten(marke = true, abstandInnen = Abstand.Klein) {
        Etikett(if (gutscheine > 0) "Fahrzeuggutschein" else "Credits")
        Text(
            if (gutscheine > 0) "$gutscheine Fahrzeuggutschein${if (gutscheine == 1) "" else "e"}" else "Fahrzeuge kaufen",
            style = Schrift.Gross.copy(fontWeight = FontWeight.Bold),
            color = Farben.Amber,
        )
        SehrLeise(
            if (gutscheine > 0) {
                "Jeder Aufstieg bringt einen Gutschein — lös ihn gegen ein Fahrzeug deiner Wahl ein, aus welcher " +
                    "Organisation auch immer. Die Entscheidung gilt dauerhaft. Daneben trägt jedes Fahrzeug seinen " +
                    "Preis: Kaufen geht immer, der Gutschein bleibt dabei stehen."
            } else {
                "Den nächsten Fahrzeuggutschein bringt dein nächster Aufstieg — wer nicht warten will, kauft mit " +
                    "Credits. Du hast ${zahl(credits)} davon."
            },
        )
    }

    if (auswahl.isEmpty()) {
        Leerhinweis("Im ganzen Katalog steht nichts mehr, was dir noch fehlt.")
        return
    }

    if (auswahl.size > 8) {
        Feld(suche, { suche = it.take(40) }, platzhalter = "Typ, Fähigkeit, Organisation …")
    }
    val begriff = suche.trim().lowercase()
    fun passt(id: String): Boolean {
        if (begriff.isEmpty()) return true
        val v = nachId[id]
        val zeile = auswahl.firstOrNull { it.id == id }
        return listOfNotNull(zeile?.typ, zeile?.organisation, zeile?.beschreibung, v?.kategorie, v?.hiOrg)
            .plus(v?.faehigkeiten.orEmpty())
            .any { it.lowercase().contains(begriff) }
    }
    val sichtbar = auswahl.filter { passt(it.id) }
    val imFenster = auswahl.firstOrNull { it.id == gewaehlt } ?: sichtbar.firstOrNull()

    // Das Schaufenster zuerst: am Handy untereinander, und der gewählte Wagen
    // soll nicht unter vierzig Reihen verschwinden.
    imFenster?.let { f ->
        val v = nachId[f.id]
        val preis = f.preis
        val fehlt = (preis ?: 0) - credits
        Column(
            verticalArrangement = Arrangement.spacedBy(Abstand.Klein),
            modifier = Modifier
                .fillMaxWidth()
                .flaeche(ecke = 12.dp, randfarbe = organisationsfarbe(f.organisation).copy(alpha = 0.7f))
                .padding(Abstand.Normal),
        ) {
            Etikett(listOfNotNull(f.organisation.ifBlank { null }, v?.kategorie?.ifBlank { null }).joinToString(" · "))
            if (f.tagesangebot) Text("Tagesangebot · 25 % Rabatt", style = Schrift.MonoKlein, color = Farben.Amber)
            Text(f.typ, style = Schrift.MonoNormal.copy(fontSize = Schrift.TITEL), color = Farben.Text)
            v?.hiOrg?.takeIf { it.isNotBlank() && it != "Keine" }?.let { SehrLeise(it, mono = true) }
            if (f.beschreibung.isNotBlank()) Leise(f.beschreibung)
            Row(horizontalArrangement = Arrangement.spacedBy(Abstand.Gross)) {
                Column {
                    Etikett("Besatzung")
                    Text(f.besatzung.ifBlank { "—" }, style = Schrift.MonoKlein, color = Farben.Text)
                }
            }
            Etikett("Kann")
            val faehig = v?.faehigkeiten.orEmpty()
            if (faehig.isEmpty()) {
                SehrLeise("Keine besonderen Fähigkeiten.")
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Haar),
                    verticalArrangement = Arrangement.spacedBy(Abstand.Haar),
                ) { faehig.forEach { Marke(it) } }
            }
            if (gutscheine > 0) {
                Knopf(
                    if (laeuft) "Wird eingestellt …" else "Gutschein einlösen: ${f.typ}",
                    { beiHolen(f.id, false) },
                    art = Knopfart.Haupt,
                    aktiv = !laeuft,
                    breit = true,
                )
            }
            if (preis != null) {
                Knopf(
                    when {
                        fehlt > 0 -> "Es fehlen ${zahl(fehlt)} Credits"
                        scharf == f.id -> "Wirklich für ${zahl(preis)} Credits kaufen?"
                        else -> "Kaufen — ${zahl(preis)} Credits"
                    },
                    {
                        if (scharf != f.id) {
                            scharf = f.id
                        } else {
                            scharf = null
                            beiHolen(f.id, true)
                        }
                    },
                    art = if (gutscheine == 0 || scharf == f.id) Knopfart.Haupt else Knopfart.Normal,
                    aktiv = !laeuft && fehlt <= 0,
                    breit = true,
                )
                f.regulaer?.let { SehrLeise("Regulär $it Credits", mono = true) }
            }
        }
    }

    if (sichtbar.isEmpty()) {
        Leerhinweis("Kein Treffer — der Katalog hat noch ${auswahl.size} Fahrzeuge für dich.")
        return
    }

    // Die Reihen — gruppiert nach Kategorie (sonst Organisation), wie `gruppiereFahrzeuge`.
    sichtbar
        .groupBy { nachId[it.id]?.kategorie?.ifBlank { null } ?: it.organisation.ifBlank { "Weitere" } }
        .toSortedMap()
        .forEach { (reihe, fahrzeuge) ->
            Text(reihe.uppercase(), style = Schrift.MonoKlein, color = Farben.TextSehrLeise)
            fahrzeuge.forEach { f ->
                val hier = imFenster?.id == f.id
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Abstand.Normal),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .flaeche(ecke = 9.dp, randfarbe = if (hier) Farben.Amber else Farben.Rand)
                        .clickable(role = Role.Button) {
                            gewaehlt = f.id
                            scharf = null
                        }
                        .padding(horizontal = Abstand.Normal, vertical = Abstand.Klein),
                ) {
                    Box(Modifier.size(10.dp).background(organisationsfarbe(f.organisation), CircleShape))
                    Text(
                        f.typ,
                        style = Schrift.MonoKlein,
                        color = Farben.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (f.tagesangebot) Marke("Heute", farbe = Farben.Amber)
                    f.preis?.let { SehrLeise("${zahl(it)} C", mono = true) }
                }
            }
        }
}
