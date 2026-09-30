package de.pagerspass.pagerspass.ansichten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.pagerspass.pagerspass.mobil.Bereich as Bereichsstand
import de.pagerspass.pagerspass.mobil.Buchdaten
import de.pagerspass.pagerspass.netz.Abzeichen
import de.pagerspass.pagerspass.netz.Gemeinschaft
import de.pagerspass.pagerspass.netz.Konto
import de.pagerspass.pagerspass.netz.Schicht
import de.pagerspass.pagerspass.ui.bausteine.Abschnitt
import de.pagerspass.pagerspass.ui.bausteine.Bereich
import de.pagerspass.pagerspass.ui.bausteine.Fortschritt
import de.pagerspass.pagerspass.ui.bausteine.Kasten
import de.pagerspass.pagerspass.ui.bausteine.Knopf
import de.pagerspass.pagerspass.ui.bausteine.Knopfart
import de.pagerspass.pagerspass.ui.bausteine.Leerhinweis
import de.pagerspass.pagerspass.ui.bausteine.Marke
import de.pagerspass.pagerspass.ui.bausteine.Reiter
import de.pagerspass.pagerspass.ui.bausteine.Reiterreihe
import de.pagerspass.pagerspass.ui.bausteine.SehrLeise
import de.pagerspass.pagerspass.ui.bausteine.Seite
import de.pagerspass.pagerspass.ui.bausteine.Seitenkopf
import de.pagerspass.pagerspass.ui.bausteine.Wegzeile
import de.pagerspass.pagerspass.ui.schmuck.Profilzeile
import de.pagerspass.pagerspass.ui.schmuck.Profilbanner
import de.pagerspass.pagerspass.ui.theme.Abstand
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift
import de.pagerspass.pagerspass.ui.theme.flaeche
import de.pagerspass.pagerspass.ui.zeichen.Zeichen

/**
 * Die fünf übrigen Wege der Tableiste, angebunden.
 *
 * <b>Jede Seite lädt beim Öffnen und nur, was fehlt.</b> Der Ladeaufruf steht in
 * einem `LaunchedEffect` und die Sitzung bricht ab, wenn schon etwas dasteht —
 * sonst holt jeder Wechsel zwischen zwei Reitern der Leiste alles neu.
 *
 * <b>Die Seiten kennen die Sitzung nicht.</b> Sie bekommen ihren Bereich und
 * zwei Rückrufe. Das ist dieselbe Trennung wie beim Startbildschirm: Was der
 * Server liefert und was die Seite zeigt, sind zwei Dinge, und die Naht dazwischen
 * gehört an eine Stelle (`PagerSpassApp`).
 */

// ---------------------------------------------------------------- Dienstbuch

// Das Dienstbuch steht in `DienstbuchSeiten.kt` — mit fünf Reitern ist es
// eine eigene Seite und kein Abschnitt dieser Datei mehr.

// -------------------------------------------------------------------- Wache

/**
 * Die Wache.
 *
 * <b>Entweder die eigene oder die Suche</b> — nie beides. Wer in einer
 * Gemeinschaft ist, will hinein; wer in keiner ist, zur Liste. Dieselbe Weiche
 * wie in der Tableiste des Webs: Derselbe Knopf soll auf beiden Zweigen
 * denselben Weg nehmen.
 */
// ------------------------------------------------------------------ Freunde
//
// Die Freundeseite steht seit dem Ausbau auf vier Wege (Brett, Freunde,
// Nachrichten, Kontakte) in `FreundeSeiten.kt`.

// Shop und Konto stehen in `ShopSeiten.kt` und `KontoSeiten.kt` — sie sind
// mit Premium, Postfach und den Nebenwegen des Kontos zu groß für diese Sammlung
// geworden.
