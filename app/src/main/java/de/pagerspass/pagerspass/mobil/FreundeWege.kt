package de.pagerspass.pagerspass.mobil

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import de.pagerspass.pagerspass.ansichten.BrettInhalt
import de.pagerspass.pagerspass.ansichten.Buchreiter
import de.pagerspass.pagerspass.ansichten.EintragInhalt
import de.pagerspass.pagerspass.ansichten.FreundeListeInhalt
import de.pagerspass.pagerspass.ansichten.FreundeRahmen
import de.pagerspass.pagerspass.ansichten.Freundereiter
import de.pagerspass.pagerspass.ansichten.GespraechSeite
import de.pagerspass.pagerspass.ansichten.KontakteInhalt
import de.pagerspass.pagerspass.ansichten.NachrichtenInhalt
import de.pagerspass.pagerspass.ansichten.ProfilansichtInhalt
import de.pagerspass.pagerspass.netz.Rechtsstand
import de.pagerspass.pagerspass.netz.Server

/**
 * Die Wege des Freundebereichs — und wie sie im Web heißen.
 *
 * | Web                              | App                              |
 * |----------------------------------|----------------------------------|
 * | `/freunde`                       | `freunde` (Brett)                |
 * | `/freunde/liste`                 | `freunde/liste`                  |
 * | `/freunde/nachrichten`           | `freunde/nachrichten`            |
 * | `/freunde/kontakte`              | `freunde/kontakte`               |
 * | `/freunde/eintrag/:nr`           | `freunde/eintrag/{nr}`           |
 * | `/freunde/profil/:benutzername`  | `freunde/profil/{benutzername}`  |
 * | `/freunde/gespraech/:kennung`    | `freunde/gespraech/{kennung}`    |
 *
 * Jede dieser Adressen lässt sich als Zeichenkette ansteuern
 * (`steuerung.navigate("freunde/eintrag/42")`) — so führen auch die
 * Systemmeldungen hierher.
 */
object FreundeWeg {
    const val BRETT = "freunde"
    const val LISTE = "freunde/liste"
    const val NACHRICHTEN = "freunde/nachrichten"
    const val KONTAKTE = "freunde/kontakte"
    const val EINTRAG = "freunde/eintrag"
    const val PROFIL = "freunde/profil"
    const val GESPRAECH = "freunde/gespraech"

    /** Ob eine Adresse zum Freundebereich gehört — für die Markierung der Tableiste. */
    fun gehoertDazu(route: String?): Boolean =
        route == BRETT || route?.startsWith("$BRETT/") == true

    fun profil(benutzername: String) = "$PROFIL/$benutzername"
    fun eintrag(nr: Long) = "$EINTRAG/$nr"
    fun gespraech(kennung: String) = "$GESPRAECH/$kennung"
}

/**
 * Die Seiten des Freundebereichs im Navigationsgraphen.
 *
 * <b>Ein Aufruf in `PagerSpassApp`, alles andere hier.</b> Die Seiten kennen
 * weder Sitzung noch Sozialschicht; sie bekommen ihren Stand und Rückrufe. Die
 * Naht dazwischen steht an dieser einen Stelle.
 *
 * @param zurWahl Der Wechsel auf einen Weg der Tableiste (Wache, Dienstbuch).
 * @param beiEigenemProfil Die Seite, auf der man sein Profil gestaltet.
 * @param beiLink Eine Adresse im Browser öffnen — die Hausordnung am Brett.
 */
fun NavGraphBuilder.freundeWege(
    steuerung: NavHostController,
    sitzung: Sitzung,
    sozial: Sozial,
    runde: Runde,
    unterrand: Dp,
    zurWahl: (Weg) -> Unit,
    beiEigenemProfil: () -> Unit,
    beiLink: (String) -> Unit,
) {
    val griffe = Freundegriffe(steuerung, sitzung, sozial, runde, zurWahl)

    composable(FreundeWeg.BRETT) {
        FreundeRahmenFuer(Freundereiter.Brett, sitzung, sozial, griffe, unterrand) {
            val stand by sitzung.stand.collectAsStateWithLifecycle()
            val daten by sitzung.daten.collectAsStateWithLifecycle()
            val kreis by sozial.kreis.stand.collectAsStateWithLifecycle()
            val brett by sozial.brett.stand.collectAsStateWithLifecycle()
            val sozialstand by sozial.stand.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                sozial.brett.laden()
                // Die eigenen Schichten zum Anhängen — und aus ihnen ergibt sich
                // zugleich, ob öffentlich geschrieben werden darf.
                sitzung.buchLaden()
                sitzung.wacheLaden()
            }

            BrettInhalt(
                konto = stand.konto,
                meinProfil = daten.profil.inhalt,
                server = stand.server,
                imDienst = kreis.imDienst,
                eintraege = brett.eintraege,
                reiter = brett.reiter,
                kannMehr = brett.kannMehr,
                laeuft = brett.laeuft,
                istGeladen = brett.istGeladen,
                hatWache = daten.wache.inhalt?.eigene != null,
                darfOeffentlich = daten.buch.inhalt?.schichten.orEmpty().isNotEmpty(),
                schichten = daten.buch.inhalt?.schichten.orEmpty(),
                meldung = sozialstand.meldung,
                beiMeldungWeg = { sozial.meldungWegnehmen() },
                beiReiter = { sozial.brett.laden(it) },
                beiMehr = { sozial.brett.mehr() },
                beiSchreiben = { text, sicht, code -> sozial.brett.schreiben(text, sicht, code) },
                beiQuittieren = { sozial.brett.quittung(it) },
                beiEntfernen = { sozial.brett.entfernen(it) },
                beiMelden = { sozial.brett.melden(it, "Am Brett gemeldet") },
                beiKommentare = { steuerung.navigate(FreundeWeg.eintrag(it)) },
                beiProfil = griffe::profil,
                beiBezug = griffe::bezug,
                beiDazuschalten = { griffe.dazuschalten(it.anwesenheit?.roomCode) },
                beiHausordnung = { beiLink(Rechtsstand.adresse(Server.BETRIEB, "nutzungsbedingungen")) },
            )
        }
    }

    composable(FreundeWeg.LISTE) {
        FreundeRahmenFuer(Freundereiter.Liste, sitzung, sozial, griffe, unterrand) {
            val stand by sitzung.stand.collectAsStateWithLifecycle()
            val kreis by sozial.kreis.stand.collectAsStateWithLifecycle()
            val sozialstand by sozial.stand.collectAsStateWithLifecycle()

            FreundeListeInhalt(
                konto = stand.konto,
                bestaetigte = kreis.bestaetigte,
                laedt = kreis.laedt,
                geladen = kreis.geladen,
                fehler = kreis.fehler,
                server = stand.server,
                meldung = sozialstand.meldung,
                beiMeldungWeg = { sozial.meldungWegnehmen() },
                beiLaden = { sozial.kreis.laden() },
                beiProfil = griffe::profil,
                beiGespraech = { steuerung.navigate(FreundeWeg.gespraech(it)) },
                beiDazuschalten = { griffe.dazuschalten(it.anwesenheit?.roomCode) },
            )
        }
    }

    composable(FreundeWeg.NACHRICHTEN) {
        FreundeRahmenFuer(Freundereiter.Nachrichten, sitzung, sozial, griffe, unterrand) {
            val stand by sitzung.stand.collectAsStateWithLifecycle()
            val kreis by sozial.kreis.stand.collectAsStateWithLifecycle()
            val sozialstand by sozial.stand.collectAsStateWithLifecycle()

            NachrichtenInhalt(
                konto = stand.konto,
                bestaetigte = kreis.bestaetigte,
                ungelesen = kreis.ungelesen,
                laedt = kreis.laedt,
                geladen = kreis.geladen,
                fehler = kreis.fehler,
                server = stand.server,
                meldung = sozialstand.meldung,
                beiMeldungWeg = { sozial.meldungWegnehmen() },
                beiLaden = { sozial.kreis.laden() },
                beiProfil = griffe::profil,
                beiGespraech = { steuerung.navigate(FreundeWeg.gespraech(it)) },
                beiDazuschalten = { griffe.dazuschalten(it.anwesenheit?.roomCode) },
            )
        }
    }

    composable(FreundeWeg.KONTAKTE) {
        FreundeRahmenFuer(Freundereiter.Kontakte, sitzung, sozial, griffe, unterrand) {
            val stand by sitzung.stand.collectAsStateWithLifecycle()
            val kreis by sozial.kreis.stand.collectAsStateWithLifecycle()
            val sozialstand by sozial.stand.collectAsStateWithLifecycle()

            // Vorschläge und Einladungen frisch — sie ändern sich nach jeder Schicht.
            LaunchedEffect(Unit) { sozial.kreis.standHolen() }

            KontakteInhalt(
                konto = stand.konto,
                einladungen = kreis.einladungen,
                offeneAnfragen = kreis.offeneAnfragen,
                gestellteAnfragen = kreis.gestellteAnfragen,
                blockierte = kreis.blockierte,
                vorschlaege = kreis.vorschlaege,
                offeneVorschlaege = kreis.offeneVorschlaege,
                weggelegt = kreis.weggelegt.size,
                treffer = kreis.treffer,
                sucht = kreis.sucht,
                server = stand.server,
                meldung = sozialstand.meldung,
                beiMeldungWeg = { sozial.meldungWegnehmen() },
                beiEinladungAnnehmen = { e -> griffe.einladungAnnehmen(e) },
                beiEinladungAblehnen = { e -> sozial.einladungBeantworten(e.nr, false) },
                beiAntworten = { wen, ja -> sozial.kreis.antworten(wen, ja) },
                beiSuchen = { sozial.kreis.suchen(it) },
                beiAnfragen = { sozial.kreis.anfragen(it) },
                beiWeglegen = { sozial.kreis.weglegen(it) },
                beiZurueckholen = { sozial.kreis.zurueckholen() },
                beiLoesen = { sozial.kreis.loesen(it) },
                beiProfil = griffe::profil,
            )
        }
    }

    composable("${FreundeWeg.EINTRAG}/{nr}") { eintrag ->
        val nr = eintrag.arguments?.getString("nr")?.toLongOrNull() ?: 0L
        FreundeRahmenFuer(Freundereiter.Brett, sitzung, sozial, griffe, unterrand) {
            val stand by sitzung.stand.collectAsStateWithLifecycle()
            val brett by sozial.brett.stand.collectAsStateWithLifecycle()
            val sozialstand by sozial.stand.collectAsStateWithLifecycle()

            EintragInhalt(
                nr = nr,
                eintrag = brett.eintrag?.takeIf { it.nr == nr },
                laedt = brett.eintragLaedt,
                fehler = brett.eintragFehler,
                kommentare = if (brett.offenerEintrag == nr) brett.kommentare else emptyList(),
                server = stand.server,
                meldung = sozialstand.meldung,
                beiMeldungWeg = { sozial.meldungWegnehmen() },
                beiLaden = { sozial.brett.eintragOeffnen(it) },
                beiSchliessen = { sozial.brett.eintragSchliessen(it) },
                beiQuittieren = { sozial.brett.quittung(it) },
                beiEntfernen = { welcher ->
                    sozial.brett.entfernen(welcher) { ok -> if (ok) griffe.zumBrett() }
                },
                beiMelden = { sozial.brett.melden(it, "Am Brett gemeldet") },
                beiKommentarMelden = { sozial.brett.melden(it, "Kommentar am Brett gemeldet", alsKommentar = true) },
                beiKommentieren = { welcher, text, danach -> sozial.brett.kommentarSchreiben(welcher, text, danach) },
                beiProfil = griffe::profil,
                beiBezug = griffe::bezug,
                beiZurueck = { griffe.zurueck(FreundeWeg.BRETT) },
            )
        }
    }

    composable("${FreundeWeg.PROFIL}/{benutzername}") { eintrag ->
        val benutzername = eintrag.arguments?.getString("benutzername").orEmpty()
        FreundeRahmenFuer(Freundereiter.Liste, sitzung, sozial, griffe, unterrand, ausweis = false) {
            val stand by sitzung.stand.collectAsStateWithLifecycle()
            val daten by sitzung.daten.collectAsStateWithLifecycle()
            val kreis by sozial.kreis.stand.collectAsStateWithLifecycle()
            val sozialstand by sozial.stand.collectAsStateWithLifecycle()
            val rundenstand by runde.stand.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) { sitzung.wacheLaden() }

            val ansicht = kreis.profil(benutzername)
            val eigeneWache = daten.wache.inhalt?.eigene
            val raum = rundenstand.raum

            ProfilansichtInhalt(
                benutzername = benutzername,
                ansicht = ansicht,
                server = stand.server,
                meldung = sozialstand.meldung,
                eigeneRunde = raum?.takeIf { it.state != "Beendet" }?.code,
                darfInWacheEinladen = eigeneWache?.darfFuehren == true && ansicht.profil?.ich == false,
                beiMeldungWeg = { sozial.meldungWegnehmen() },
                beiLaden = { sozial.kreis.profilLaden(benutzername) },
                beiZurueck = { griffe.zurueck(FreundeWeg.LISTE) },
                beiGespraech = { steuerung.navigate(FreundeWeg.gespraech(it)) },
                beiAnfragen = { wen -> sozial.kreis.anfragen(wen) { sozial.kreis.profilLaden(benutzername) } },
                beiAntworten = { wen, ja ->
                    sozial.kreis.antworten(wen, ja) { sozial.kreis.profilLaden(benutzername) }
                },
                beiDazuschalten = { code -> griffe.dazuschalten(code) },
                beiZuMirEinladen = { wen, code ->
                    sozial.einladen(wen, code) { ok ->
                        if (ok) sozial.kreis.profilVermerk(benutzername, rundeEingeladen = true)
                    }
                },
                beiInWacheEinladen = { wen ->
                    eigeneWache?.let { sozial.kreis.inWacheEinladen(benutzername, it.id, wen) }
                },
                beiBeenden = { wen -> sozial.kreis.loesen(wen) { sozial.kreis.profilLaden(benutzername) } },
                beiBlockieren = { wen ->
                    sozial.kreis.blockieren(wen) { griffe.reiter(Freundereiter.Kontakte) }
                },
                beiMelden = { wen, grund ->
                    sozial.kreis.melden(wen, grund) { sozial.kreis.profilVermerk(benutzername, gemeldet = true) }
                },
                beiProfilGestalten = beiEigenemProfil,
                beiWache = { zurWahl(Weg.Wache) },
                beiQuittieren = { sozial.brett.quittung(it) },
                beiEntfernen = { nr -> sozial.brett.entfernen(nr) },
                beiEintragMelden = { sozial.brett.melden(it, "Am Brett gemeldet") },
                beiKommentare = { steuerung.navigate(FreundeWeg.eintrag(it)) },
                beiProfil = griffe::profil,
                beiBezug = griffe::bezug,
            )
        }
    }

    composable("${FreundeWeg.GESPRAECH}/{kennung}") { eintrag ->
        val kennung = eintrag.arguments?.getString("kennung").orEmpty()
        val stand by sitzung.stand.collectAsStateWithLifecycle()
        val kreis by sozial.kreis.stand.collectAsStateWithLifecycle()
        val sozialstand by sozial.stand.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) { if (!kreis.geladen) sozial.kreis.laden() }

        GespraechSeite(
            partnerKennung = kennung,
            partner = kreis.freund(kennung),
            geladen = kreis.geladen,
            verlauf = if (sozialstand.gespraechMit == kennung) sozialstand.verlauf else emptyList(),
            verlaufLaedt = sozialstand.laedt,
            meineKennung = stand.konto?.kennung.orEmpty(),
            server = stand.server,
            meldung = sozialstand.meldung,
            unterrand = unterrand,
            beiOeffnen = { sozial.gespraechOeffnen(it) },
            beiSchliessen = { sozial.gespraechSchliessen(it) },
            beiSenden = { text, terminText, terminZeit, danach ->
                sozial.senden(kennung, text, terminText, terminZeit, danach)
            },
            beiTermin = { nr, ja -> sozial.terminBeantworten(nr, ja) },
            beiGeschenk = { nr, danach ->
                sozial.geschenkOeffnen(nr) { inhalt, fehler ->
                    // Damit das Stück sofort im Profil zur Wahl steht.
                    if (inhalt != null) sitzung.shopLaden(neu = true)
                    danach(fehler)
                }
            },
            beiDazuschalten = { griffe.dazuschalten(it.anwesenheit?.roomCode) },
            beiProfil = griffe::profil,
            beiMeldungWeg = { sozial.meldungWegnehmen() },
            beiZurueck = { griffe.zurueck(FreundeWeg.NACHRICHTEN) },
            beiFreundesliste = { griffe.reiter(Freundereiter.Liste) },
        )
    }
}

/**
 * Die Wege, die mehrere Seiten des Bereichs gemeinsam gehen — dazuschalten, ins
 * Profil, zurück, und der Wechsel zwischen den vier Reitern.
 */
private class Freundegriffe(
    private val steuerung: NavHostController,
    private val sitzung: Sitzung,
    private val sozial: Sozial,
    private val runde: Runde,
    private val zurWahl: (Weg) -> Unit,
) {
    fun profil(benutzername: String) {
        if (benutzername.isNotBlank()) steuerung.navigate(FreundeWeg.profil(benutzername))
    }

    /** Zu einem Freund in die Runde — der Rundenrahmen übernimmt, sobald man drin ist. */
    fun dazuschalten(roomCode: String?) {
        val code = roomCode?.takeIf { it.isNotBlank() } ?: return
        runde.beitreten(code, sitzung.stand.value.konto?.anzeigename.orEmpty())
    }

    /** Eine Einladung annehmen: abhaken, dann beitreten oder zuschauen. */
    fun einladungAnnehmen(e: de.pagerspass.pagerspass.netz.Einladung) {
        sozial.einladungBeantworten(e.nr, true) { ok ->
            if (!ok) return@einladungBeantworten
            val name = sitzung.stand.value.konto?.anzeigename.orEmpty()
            if (e.alsZuschauer) runde.zuschauen(e.roomCode, name) else runde.beitreten(e.roomCode, name)
        }
    }

    /**
     * Wohin der Bezug einer eigenen Zeile führt: die Nachbesprechung der Schicht,
     * die Abzeichen, die Wache. Gibt es die genaue Seite (noch) nicht, landet man
     * auf dem Weg der Tableiste, zu dem sie gehört.
     */
    fun bezug(art: String, id: String) {
        when (art) {
            "Schicht" -> runCatching { steuerung.navigate(schichtweg(id)) }
                .onFailure { zurWahl(Weg.Dienstbuch) }
            "Abzeichen" -> runCatching { steuerung.navigate(Buchreiter.Abzeichen.weg) }
                .onFailure { zurWahl(Weg.Dienstbuch) }
            "Gemeinschaft" -> zurWahl(Weg.Wache)
        }
    }

    /** Zurück, wohin man kam — oder in den Bereich, wenn es kein „vorher" gibt (Mitteilung). */
    fun zurueck(ersatz: String) {
        if (!steuerung.popBackStack()) steuerung.navigate(ersatz)
    }

    fun zumBrett() {
        if (!steuerung.popBackStack(FreundeWeg.BRETT, inclusive = false)) {
            steuerung.navigate(FreundeWeg.BRETT)
        }
    }

    /**
     * Zwischen den vier Reitern wechseln. Der Verlauf hält nur das Brett darunter:
     * Wer dreimal zwischen Liste und Kontakten wechselt, landet mit einem Zurück
     * auf dem Brett und nicht dreimal rückwärts.
     */
    fun reiter(r: Freundereiter) {
        val ziel = when (r) {
            Freundereiter.Brett -> FreundeWeg.BRETT
            Freundereiter.Liste -> FreundeWeg.LISTE
            Freundereiter.Nachrichten -> FreundeWeg.NACHRICHTEN
            Freundereiter.Kontakte -> FreundeWeg.KONTAKTE
        }
        if (ziel == FreundeWeg.BRETT) {
            zumBrett()
            return
        }
        steuerung.navigate(ziel) {
            popUpTo(FreundeWeg.BRETT)
            launchSingleTop = true
        }
    }
}

/**
 * Der Rahmen samt allem, was er selbst braucht: das eigene Profil für den
 * Ausweis, die Zahlen für die Marken, und einmal der Stand des Bereichs.
 */
@Composable
private fun FreundeRahmenFuer(
    reiter: Freundereiter,
    sitzung: Sitzung,
    sozial: Sozial,
    griffe: Freundegriffe,
    unterrand: Dp,
    ausweis: Boolean = true,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val stand by sitzung.stand.collectAsStateWithLifecycle()
    val daten by sitzung.daten.collectAsStateWithLifecycle()
    val kreis by sozial.kreis.stand.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        sitzung.profilLaden()
        if (!kreis.geladen) sozial.kreis.standHolen()
    }

    FreundeRahmen(
        reiter = reiter,
        konto = stand.konto,
        meinProfil = daten.profil.inhalt,
        server = stand.server,
        freundeZahl = kreis.bestaetigte.size,
        imDienstZahl = kreis.imDienst.size,
        ungelesen = kreis.ungelesen,
        kontaktmarke = kreis.offeneAnfragen.size + kreis.einladungen.size,
        beiReiter = griffe::reiter,
        beiMeinProfil = {
            stand.konto?.benutzername?.let { griffe.profil(it) }
        },
        unterrand = unterrand,
        ausweis = ausweis,
        inhalt = inhalt,
    )
}
