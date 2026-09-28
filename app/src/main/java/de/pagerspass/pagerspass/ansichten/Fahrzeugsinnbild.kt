package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.fahrzeug.FahrzeugSymbol
import de.pagerspass.pagerspass.ui.theme.Farben

/**
 * Das Sinnbild eines Fahrzeugs — sein gezeichneter Riss (`FahrzeugSymbol.vue` im Web,
 * hier `ui/fahrzeug/FahrzeugSymbol`).
 *
 * Der Riss hängt am Typ: Jeder Katalogtyp hat seinen Bauplan mit Aufbau, Dachmodulen,
 * Lackierung und Markierung. Ohne `typ` (oder bei einem Typ, den der Katalog nicht
 * kennt) steht das Standardfahrzeug der Organisation da — ein LF 10, ein RTW, ein GKW,
 * ein Streifenwagen —, damit wenigstens die Farbe stimmt, die an der Silhouette zuerst
 * gelesen wird.
 *
 * @param groesse Die Länge des Fahrzeugs; die Breite ergibt sich aus dem Riss.
 * @param quer Liegt quer in der Zeile, Front nach rechts (die Listen des Icon-Editors).
 */
@Composable
fun Fahrzeugsinnbild(
    organisation: String,
    modifier: Modifier = Modifier,
    groesse: Dp = 28.dp,
    typ: String = "",
    quer: Boolean = false,
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        FahrzeugSymbol(typ = typ, organisation = organisation, groesse = groesse, quer = quer)
    }
}

/** Die Organisationsfarben — dieselben Werte wie `--org-*` in base.css. */
fun organisationsfarbeVon(organisation: String): Color = when (organisation) {
    "Feuerwehr" -> Farben.OrgFeuerwehr
    "Rettungsdienst" -> Farben.OrgRettungsdienst
    "Thw" -> Farben.OrgThw
    "Polizei" -> Farben.OrgPolizei
    else -> Farben.TextLeise
}

/**
 * Die Farbe eines Trägers — `HIORG_FARBE` im Web. DRK, Johanniter und Malteser
 * sollen auf einen Blick auseinanderzuhalten sein, statt nur als grauer Text zu
 * stehen.
 */
fun traegerfarbeVon(hiOrg: String): Color = when (hiOrg) {
    "Drk" -> Farben.HiorgDrk
    "Juh" -> Farben.HiorgJuh
    "Mhd" -> Farben.HiorgMhd
    "Asb" -> Farben.HiorgAsb
    "Dlrg" -> Farben.HiorgDlrg
    "Bergwacht" -> Farben.HiorgBergwacht
    "Wasserwacht" -> Farben.HiorgWasserwacht
    "Dgzrs" -> Farben.HiorgDgzrs
    "Brh" -> Farben.HiorgBrh
    "Werkfeuerwehr" -> Farben.HiorgWerkfeuerwehr
    "Privat" -> Farben.HiorgPrivat
    else -> Color.Transparent
}
