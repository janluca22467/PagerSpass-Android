package de.pagerspass.pagerspass.ansichten.welt

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import de.pagerspass.pagerspass.ui.theme.Farben
import de.pagerspass.pagerspass.ui.theme.Schrift

/**
 * Was die Seiten der Welt vom Rahmen der App brauchen, ohne es durch jede
 * Seite zu reichen: den Server (für Profilbilder) und den Weg ins Profil
 * eines anderen Spielers.
 *
 * <b>Als CompositionLocal und nicht als Parameter.</b> Rangliste, Leihmarkt und
 * Wachenseite liegen drei Aufrufe tief unter dem Rahmen; ein Parameter müsste
 * durch den Arbeitsplatz und jede Seite dazwischen wandern, die ihn selbst nie
 * anfasst. Gesetzt wird beides genau einmal, in `WeltRahmen`.
 */
val LocalWeltServer = staticCompositionLocalOf { "" }

/**
 * Der Weg ins Profil — `null`, wo es keinen gibt (dann bleibt ein Name Text).
 *
 * Im Web ist der Name ein `RouterLink` auf `/freunde/profil/:benutzername`
 * (`Kontoname.vue`), und der Wechsel der Route verlässt die Welt. Die App tut
 * dasselbe: Der Rahmen schließt die Welt und öffnet das Profil — zurück kommt
 * man über „World“ auf dem Startbildschirm, wie im Web über die Leiste.
 */
val LocalWeltProfil = staticCompositionLocalOf<((String) -> Unit)?> { null }

/**
 * Ein leiser Satz, in dem ein Name steht — und der Name führt ins Profil.
 *
 * <b>Nur mit Benutzernamen.</b> Der Anzeigename ist nicht eindeutig; die
 * Profiladresse ist auf den Benutzernamen gebaut. Ohne ihn (der Besitzer zeigt
 * sich nicht, ein altes Angebot) bleibt es ein Satz — ein Weg ins Leere wäre
 * schlimmer als keiner. Dieselbe Regel wie `Kontoname.vue`.
 *
 * <b>Hervorgehoben, nicht unterstrichen.</b> Am Finger gibt es kein „darüber“;
 * der Name steht etwas heller und fetter als der Satz, damit man ihn als Weg
 * erkennt, ohne dass die Liste wie ein Inhaltsverzeichnis aussieht.
 */
@Composable
fun Namenssatz(
    vorher: String,
    name: String?,
    benutzername: String?,
    nachher: String = "",
    winzig: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val profil = LocalWeltProfil.current
    val farbe = if (winzig) Farben.TextSehrLeise else Farben.TextLeise
    val text = buildAnnotatedString {
        append(vorher)
        val sichtbar = name ?: "—"
        val ziel = benutzername?.takeIf { it.isNotBlank() && name != null }
        if (ziel != null && profil != null) {
            withLink(
                LinkAnnotation.Clickable(
                    tag = "profil:$ziel",
                    styles = TextLinkStyles(SpanStyle(color = Farben.Text, fontWeight = FontWeight.SemiBold)),
                    linkInteractionListener = { profil(ziel) },
                ),
            ) { append(sichtbar) }
        } else {
            append(sichtbar)
        }
        append(nachher)
    }
    Text(text = text, style = if (winzig) Schrift.Winzig else Schrift.Klein, color = farbe, modifier = modifier)
}

/**
 * Die Kartenebenen über den Neustart hinweg — `localStorage` im Web
 * (`ebenen.ts`), hier dieselbe Gerätedatei wie die Einführung
 * (`pagerspass_welt`). Eine Geräteeinstellung: Wer auf dem Handy die fremden
 * Fahrzeuge ausblendet, will sie beim nächsten Öffnen nicht wieder sehen.
 */
object Ebenenablage {
    private const val DATEI = "pagerspass_welt"
    private const val SCHLUESSEL = "ebenen"

    /** Liest die gemerkten Ebenen in `ebenen` — Fehlendes bleibt an. */
    fun laden(zusammenhang: Context, ebenen: Weltebenen) {
        val aus = runCatching {
            zusammenhang.getSharedPreferences(DATEI, Context.MODE_PRIVATE).getString(SCHLUESSEL, null)
        }.getOrNull()?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: return
        ebenen.lagen = "lagen" !in aus
        ebenen.eigene = "eigene" !in aus
        ebenen.wachen = "wachen" !in aus
        ebenen.pois = "pois" !in aus
        ebenen.wege = "wege" !in aus
        ebenen.fremde = "fremde" !in aus
        ebenen.fremdeWachen = "fremdeWachen" !in aus
        ebenen.grosslage = "grosslage" !in aus
    }

    /** Merkt, was ausgeschaltet ist — nur das, damit neue Ebenen an starten. */
    fun merken(zusammenhang: Context, ebenen: Weltebenen) {
        val aus = listOfNotNull(
            "lagen".takeIf { !ebenen.lagen },
            "eigene".takeIf { !ebenen.eigene },
            "wachen".takeIf { !ebenen.wachen },
            "pois".takeIf { !ebenen.pois },
            "wege".takeIf { !ebenen.wege },
            "fremde".takeIf { !ebenen.fremde },
            "fremdeWachen".takeIf { !ebenen.fremdeWachen },
            "grosslage".takeIf { !ebenen.grosslage },
        ).joinToString(",")
        runCatching {
            zusammenhang.getSharedPreferences(DATEI, Context.MODE_PRIVATE).edit().putString(SCHLUESSEL, aus).apply()
        }
    }
}
