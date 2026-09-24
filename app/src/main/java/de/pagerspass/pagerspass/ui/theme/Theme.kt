package de.pagerspass.pagerspass.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Das Erscheinungsbild der App.
 *
 * <b>Nur dunkel, und ohne Systemfarben.</b> Das Vorbild ist ein abgedunkelter
 * Leitstellenarbeitsplatz — die dunklen Flächen sind nicht die Nachtfassung
 * einer hellen, sondern die Sache selbst. Im Web steht dafür `color-scheme:
 * dark` ohne Gegenstück. Deshalb hier auch kein `dynamicColor`: Materials
 * Systemfarben würden Amber gegen das Hintergrundbild des Geräts tauschen, und
 * Amber ist in dieser Anwendung die Farbe für „hier bist du" und „das ist der
 * Hauptweg". Sie ist Bedeutung, nicht Geschmack.
 *
 * <b>Was Material hier tut und was nicht.</b> Wir benutzen Material3 für seine
 * Bausteine (Ripple, Textfeldverhalten, Rollverhalten), aber nicht für sein
 * Aussehen. Das `ColorScheme` unten ist deshalb kein Entwurf, sondern eine
 * Übersetzungstabelle: Sie sorgt dafür, dass ein Material-Baustein, den wir
 * *nicht* selbst nachgebaut haben, nicht in Lila aus der Seite fällt. Die
 * eigenen Bausteine in `ui/bausteine/` greifen auf `Farben` zu, nicht auf
 * `MaterialTheme.colorScheme` — dort steht die Absicht im Namen.
 */
@Composable
fun PagerSpassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeitstellenFarben,
        typography = MaterialStaffel,
        content = {
            // Der Regelfall für jeden Text, der seinen Stil nicht selbst setzt:
            // Fließtext in Listen und Kästen, in --text. Ohne das erbt Material
            // seine eigene Vorgabe (14 Punkt, Weiß) und jede Zeile, die einmal
            // vergessen wird, fällt einen Punkt kleiner aus als ihre Nachbarn.
            CompositionLocalProvider(
                LocalTextStyle provides Schrift.Normal.copy(color = Farben.Text),
                content = content,
            )
        },
    )
}

/**
 * Die Übersetzungstabelle nach Material.
 *
 * `primary` ist Amber, weil Material es für das nimmt, wofür wir Amber nehmen:
 * den Hauptweg. `error` ist Signal. Die Flächenstufen bilden unsere drei ab —
 * `surface` ist die Kastenfläche, `surfaceVariant` die gehobene, `background`
 * der Seitengrund. Was Material sonst noch kennt (Tertiär, Container-Paare),
 * zeigt auf die nächstgelegene eigene Farbe; nirgends steht ein Wert, den es
 * in `Farben` nicht gibt.
 */
private val LeitstellenFarben = darkColorScheme(
    primary = Farben.Amber,
    onPrimary = Farben.AufAmber,
    primaryContainer = Farben.AmberTief,
    onPrimaryContainer = Farben.Text,

    secondary = Farben.Blau,
    onSecondary = Color.White,
    secondaryContainer = Farben.FlaecheAktiv,
    onSecondaryContainer = Farben.Text,

    tertiary = Farben.Violett,
    onTertiary = Color.White,
    tertiaryContainer = Farben.FlaecheAktiv,
    onTertiaryContainer = Farben.Text,

    background = Farben.Bg,
    onBackground = Farben.Text,

    surface = Farben.Flaeche,
    onSurface = Farben.Text,
    surfaceVariant = Farben.FlaecheHoch,
    onSurfaceVariant = Farben.TextLeise,
    surfaceContainerLowest = Farben.BgTief,
    surfaceContainerLow = Farben.Flaeche,
    surfaceContainer = Farben.FlaecheHoch,
    surfaceContainerHigh = Farben.FlaecheAktiv,
    surfaceContainerHighest = Farben.FlaecheAktiv,

    error = Farben.Signal,
    onError = Color.White,
    errorContainer = Farben.SignalTief,
    onErrorContainer = Farben.Text,

    outline = Farben.RandHell,
    outlineVariant = Farben.Rand,
    scrim = Farben.Ueberlagerung,
)

/**
 * Dieselbe Übersetzung für die Schrift.
 *
 * Auch hier gilt: Die eigenen Bausteine nehmen `Schrift.*` direkt. Diese Tabelle
 * ist für die Material-Bausteine, die wir nicht selbst gebaut haben — damit ein
 * `AlertDialog` oder ein `DropdownMenu` nicht in Robotos Vorgabestaffel steht,
 * während die Seite darum in unserer steht.
 */
private val MaterialStaffel = Typography(
    displayLarge = Schrift.Anzeige,
    displayMedium = Schrift.Anzeige,
    displaySmall = Schrift.Schlagzeile,
    headlineLarge = Schrift.Schlagzeile,
    headlineMedium = Schrift.Schlagzeile,
    headlineSmall = Schrift.Titel,
    titleLarge = Schrift.Titel,
    titleMedium = Schrift.Gross,
    titleSmall = Schrift.Gross,
    bodyLarge = Schrift.Normal,
    bodyMedium = Schrift.Normal,
    bodySmall = Schrift.Klein,
    labelLarge = Schrift.Knopf,
    labelMedium = Schrift.Klein,
    labelSmall = Schrift.Winzig,
)
