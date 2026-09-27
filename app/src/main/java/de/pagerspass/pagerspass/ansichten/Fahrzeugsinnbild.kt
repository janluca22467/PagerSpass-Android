package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Rundung
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Das Sinnbild eines Fahrzeugs — der Platzhalter für den gezeichneten Riss.
 *
 * <b>Im Web steht hier eine Silhouette</b> (`FahrzeugSymbol.vue`, gebaut aus
 * `utils/fahrzeugRiss.ts` und `fahrzeugBauplan.ts`, gut zweitausend Zeilen je Typ
 * gezeichneter Aufbauten). Die App hat diesen Riss nicht; bis er kommt, trägt das
 * Sinnbild wenigstens die Auskunft, die an der Silhouette zuerst gelesen wird: die
 * Farbe der Organisation. Rot ist Feuerwehr, bevor man die Marke liest.
 */
@Composable
fun Fahrzeugsinnbild(
    organisation: String,
    modifier: Modifier = Modifier,
    groesse: Dp = 28.dp,
) {
    val farbe = organisationsfarbeVon(organisation)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(groesse)
            .background(farbe.copy(alpha = 0.16f), Rundung.Winzig)
            .border(1.dp, farbe.copy(alpha = 0.7f), Rundung.Winzig),
    ) {
        Icon(
            imageVector = Zeichen.Fahrzeug,
            contentDescription = null,
            tint = farbe,
            modifier = Modifier.size(groesse * 0.72f),
        )
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
