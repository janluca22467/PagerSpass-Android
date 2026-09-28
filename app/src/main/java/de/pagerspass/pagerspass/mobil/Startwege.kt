package de.pagerspass.pagerspass.mobil

import androidx.compose.runtime.Composable
import de.pagerspass.pagerspass.ansichten.Startgriffe
import de.pagerspass.pagerspass.ansichten.Startzusatz
import de.pagerspass.pagerspass.netz.Server

/**
 * Die Verdrahtung der Runden-Zusätze des Startbildschirms (Runde, Teil 1) —
 * ausgelagert, damit `PagerSpassApp.kt` nur eine Zeile dafür trägt.
 *
 * Kacheln und Fußknöpfe, deren Ziel die App (noch) nicht kennt, öffnen die
 * Adresse im Browser; ein `raum`-Ziel tritt der Runde bei.
 */
@Composable
internal fun startzusatz(
    stand: Sitzungsstand,
    daten: Seitenstand,
    start: Startstand,
    startdaten: Startdaten,
    runde: Runde,
    sitzung: Sitzung,
    oeffnen: (String) -> Unit,
    zumShop: () -> Unit,
): Startzusatz {
    val kennung = stand.konto?.kennung
    val name = stand.konto?.anzeigename.orEmpty()
    val katalog = daten.katalog.inhalt
    fun adresse(ziel: String): String =
        if (ziel.startsWith("http")) ziel else stand.server.trimEnd('/') + "/" + ziel.trimStart('/')

    return Startzusatz(
        start = start,
        leitstellen = katalog?.leitstellen.orEmpty(),
        fahrzeuge = katalog?.fahrzeuge.orEmpty(),
        clan = daten.wache.inhalt?.eigene,
        tagesschichtKreis = daten.tagesschicht.inhalt?.landkreis?.let { kreis ->
            katalog?.landkreise?.firstOrNull { it.id == kreis }?.name ?: kreis
        },
        griffe = Startgriffe(
            besetzen = { kreis, leitstelleId, ganz -> sitzung.raumEroeffnen(kreis.id, leitstelleId, ganz) },
            vorlagenLaden = { kennung?.let { startdaten.vorlagenLaden(it) } },
            vorlageStarten = { id ->
                kennung?.let { k -> startdaten.vorlageStartenUndDann(k, id) { code -> runde.beitreten(code, name) } }
            },
            vorlageLoeschen = { id -> kennung?.let { startdaten.vorlageLoeschen(it, id) } },
            vorlageEinloesen = { code -> kennung?.let { startdaten.vorlageEinloesen(it, code) } },
            vorlagenMeldungWeg = { startdaten.vorlagenMeldungWegnehmen() },
            vorlageninhalt = { id ->
                startdaten.vorlageninhalt(kennung ?: error("Bitte melde dich zuerst an."), id)
            },
            kreiswachen = { kreis -> startdaten.kreiswachen(kreis) },
            vorlageninhaltSichern = { id, neuerName, plaetze, bestand ->
                startdaten.vorlageninhaltSichern(
                    kennung ?: error("Bitte melde dich zuerst an."),
                    id,
                    neuerName,
                    plaetze,
                    bestand,
                )
            },
            umfrageAntworten = { option -> kennung?.let { startdaten.umfrageAntworten(it, option) } },
            clanrundeBeitreten = { code -> runde.beitreten(code, name) },
            clanrundeSchliessen = { id ->
                kennung?.let { startdaten.clanrundeSchliessen(it, id) }
                sitzung.wacheLaden(neu = true)
            },
            ausbildungHinweisWeg = { kennung?.let { startdaten.ausbildungHinweisWegnehmen(it) } },
            kachel = { k ->
                when {
                    k.premiumNoetig && stand.konto?.premiumAktiv != true -> zumShop()
                    k.aktion == "raum" -> runde.beitreten(k.ziel, name)
                    k.aktion == "link" -> oeffnen(adresse(k.ziel))
                    else -> oeffnen(adresse(k.pfad.ifBlank { k.ziel }))
                }
            },
            footer = { f -> oeffnen(adresse(f.ziel.ifBlank { f.pfad })) },
            vertragKuendigen = { oeffnen("${Server.BETRIEB}/vertrag-kuendigen") },
            vertragWiderrufen = { oeffnen("${Server.BETRIEB}/vertrag-widerrufen") },
        ),
    )
}
