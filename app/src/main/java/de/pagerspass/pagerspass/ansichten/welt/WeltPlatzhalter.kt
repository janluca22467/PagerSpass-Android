package de.pagerspass.pagerspass.ansichten.welt

import androidx.compose.runtime.Composable
import de.pagerspass.pagerspass.mobil.Welt
import de.pagerspass.pagerspass.mobil.Weltzustand
import de.pagerspass.pagerspass.netz.Katalog
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis

/*
 * Blenden, die in der App noch nicht übertragen sind — jede steht hier, bis sie ihre eigene
 * Datei bekommt. Wer eine davon baut, löscht sie hier.
 */

@Composable
private fun NochNicht(was: String) {
    Leerhinweis("$was ist in der App noch nicht übertragen — im Browser geht es schon.")
}

@Composable
fun WeltWachenBlende(welt: Welt, stand: Weltzustand, griffe: Weltgriffe) = NochNicht("Die Wachenliste")

@Composable
fun WeltWachenseiteBlende(welt: Welt, stand: Weltzustand, katalog: Katalog?, wacheId: String?, griffe: Weltgriffe) =
    NochNicht("Die Wachenseite")

@Composable
fun WeltChatBlende(welt: Welt, stand: Weltzustand, griffe: Weltgriffe) = NochNicht("Der Chat")

@Composable
fun WeltGrosslageBlende(welt: Welt, stand: Weltzustand, griffe: Weltgriffe) = NochNicht("Der Großeinsatz")

@Composable
fun WeltKasseBlende(welt: Welt, stand: Weltzustand) = NochNicht("Die Kasse")

@Composable
fun WeltRanglisteBlende(welt: Welt, stand: Weltzustand, griffe: Weltgriffe) = NochNicht("Die Rangliste")

@Composable
fun WeltLaufbahnBlende(welt: Welt, stand: Weltzustand) = NochNicht("Die Laufbahn")

@Composable
fun WeltEinstellungBlende(
    welt: Welt,
    stand: Weltzustand,
    griffe: Weltgriffe,
    ebenen: Map<String, Boolean>,
    beiEbene: (String, Boolean) -> Unit,
) = NochNicht("Die Einstellungen")

@Composable
fun WeltLeiheBlende(welt: Welt, stand: Weltzustand, katalog: Katalog?, griffe: Weltgriffe) = NochNicht("Die Leihe")
