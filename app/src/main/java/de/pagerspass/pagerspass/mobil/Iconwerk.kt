package de.pagerspass.pagerspass.mobil

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Blaulicht
import de.pagerspass.pagerspass.netz.IconGrenzen
import de.pagerspass.pagerspass.netz.Iconpack
import de.pagerspass.pagerspass.netz.Icontyp
import de.pagerspass.pagerspass.netz.Iconwege
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Packicon
import de.pagerspass.pagerspass.netz.Packimport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Die eigenen Fahrzeug-Icons — Bibliothek und Editor, das Gegenstück zu den
 * Skriptteilen von `IconBibliothekView.vue` und `IconEditorView.vue`.
 *
 * <b>Eine Klasse für beide Seiten, eine Instanz je Seite.</b> Beide lesen dieselben
 * drei Listen (Packs, Grenzen, Typen) und schreiben über dieselben Wege; getrennt
 * wären das zwei Abschriften derselben Ladefunktion.
 *
 * <b>Jede Änderung geht durch denselben Trichter</b> (`tun`): Sperre, Fehler und
 * Nachladen stehen einmal da und nicht vor jedem der Handgriffe.
 */
class Iconwerk(anwendung: Application) : AndroidViewModel(anwendung) {

    private val ablage = Ablage(anwendung)
    private val wege = Iconwege(Netz(ablage))
    private val aufloeser = anwendung.contentResolver

    private val _stand = MutableStateFlow(Iconstand())
    val stand: StateFlow<Iconstand> = _stand.asStateFlow()

    // -------------------------------------------------------- Die Bibliothek

    fun bibliothekLaden() = viewModelScope.launch {
        val server = ablage.server()
        _stand.update { it.copy(laedt = true, server = server) }
        // `coroutineScope`, damit ein scheiternder Abruf als Fehler hier ankommt und
        // nicht als Absturz am ViewModel vorbei.
        runCatching {
            coroutineScope {
                val packs = async { wege.packs() }
                val grenzen = async { wege.grenzen() }
                val typen = async { wege.typen() }
                Triple(packs.await(), grenzen.await(), typen.await())
            }
        }.onSuccess { (packs, grenzen, typen) ->
            _stand.update {
                it.copy(packs = packs, grenzen = grenzen, typen = typen, laedt = false, fehler = null)
            }
        }.onFailure { f ->
            _stand.update {
                it.copy(laedt = false, fehler = f.message ?: "Die Icon-Packs sind gerade nicht erreichbar.")
            }
        }
    }

    /**
     * Führt eine Änderung aus und lädt danach neu — der eine Trichter für alle
     * Handgriffe der Bibliothek.
     */
    private fun tun(arbeit: suspend () -> Unit) = viewModelScope.launch {
        if (_stand.value.sendet) return@launch
        _stand.update { it.copy(sendet = true, fehler = null) }
        runCatching { arbeit() }
            .onFailure { f -> _stand.update { it.copy(fehler = f.message ?: "Das ging nicht.") } }
        _stand.update { it.copy(sendet = false) }
        bibliothekLaden()
    }

    /**
     * Ein Pack anlegen — und direkt hinein: Ein leeres Pack anzulegen und dann auf
     * einer Übersicht zu stehen wäre ein Schritt, den niemand gewollt hat.
     */
    fun anlegen(name: String, beiNeu: (String) -> Unit) = tun {
        val neu = wege.anlegen(name.trim().ifEmpty { "Neues Pack" })
        beiNeu(neu.id)
    }

    fun umbenennen(pack: Iconpack, name: String) = tun { wege.umbenennen(pack.id, name) }

    fun loeschen(pack: Iconpack) = tun { wege.loeschen(pack.id) }

    fun teilen(pack: Iconpack) = tun {
        val code = wege.code(pack.id)
        _stand.update { it.copy(codeVon = pack.id to code) }
    }

    fun uebernehmen(code: String, danach: () -> Unit) = tun {
        if (code.isBlank()) return@tun
        wege.uebernehmen(code)
        danach()
    }

    /**
     * Ein Pack aus einer Zip einspielen. Der Bericht bleibt stehen, bis der nächste
     * Import läuft: Er ist die eigentliche Antwort — wer die Zip gebaut hat, will
     * die Gründe der übersprungenen Fahrzeuge lesen.
     */
    fun importieren(quelle: Uri) = tun {
        _stand.update { it.copy(importiert = null) }
        val (name, daten) = withContext(Dispatchers.IO) { Iconbild.lesen(aufloeser, quelle) }
        val bericht = wege.importieren(name, daten)
        _stand.update { it.copy(importiert = bericht) }
    }

    // ------------------------------------------------------------- Der Editor

    fun editorLaden(packId: String) = viewModelScope.launch {
        val server = ablage.server()
        _stand.update { it.copy(laedt = true, packId = packId, server = server) }
        runCatching {
            coroutineScope {
                val typen = async { wege.typen() }
                val icons = async { wege.icons(packId) }
                val grenzen = async { wege.grenzen() }
                val packs = async { wege.packs() }
                Iconladung(typen.await(), icons.await(), grenzen.await(), packs.await())
            }
        }.onSuccess { l ->
            val pack = l.packs.firstOrNull { it.id == packId }
            _stand.update {
                it.copy(
                    typen = l.typen,
                    icons = l.icons.associateBy { i -> i.vorlageId },
                    grenzen = l.grenzen,
                    packs = l.packs,
                    packname = pack?.name ?: "Icon-Pack",
                    packBytes = pack?.bytes ?: 0L,
                    laedt = false,
                    fehler = null,
                )
            }
        }.onFailure { f ->
            _stand.update {
                it.copy(laedt = false, fehler = f.message ?: "Das Pack ist gerade nicht erreichbar.")
            }
        }
    }

    /** Einen Typ wählen — die Meldung des vorigen verschwindet mit. */
    fun waehlen(vorlageId: String?) = _stand.update {
        it.copy(gewaehlt = vorlageId, fehler = null, hinweis = null, lichtwahl = null)
    }

    fun lichtWaehlen(index: Int?) = _stand.update { it.copy(lichtwahl = index) }

    fun fehlerSetzen(text: String?) = _stand.update { it.copy(fehler = text) }

    /** Der Mantel des Editors: laufen lassen, Fehler zeigen, nichts nachladen. */
    private fun bearbeiten(arbeit: suspend (packId: String, vorlageId: String) -> Unit) =
        viewModelScope.launch {
            val s = _stand.value
            val vorlageId = s.gewaehlt ?: return@launch
            if (s.laeuft) return@launch

            _stand.update { it.copy(laeuft = true, fehler = null) }
            runCatching { arbeit(s.packId, vorlageId) }
                .onFailure { f -> _stand.update { it.copy(fehler = f.message ?: "Das ging nicht.") } }
            _stand.update { it.copy(laeuft = false) }
        }

    /** Ein Bild hochladen: einpassen, nach WebP rechnen, ablegen. */
    fun hochladen(quelle: Uri) = bearbeiten { packId, vorlageId ->
        _stand.update { it.copy(hinweis = null) }
        val eingepasst = withContext(Dispatchers.IO) { Iconbild.einpassen(aufloeser, quelle) }
        val neu = wege.setzen(packId, vorlageId, eingepasst.webp)
        _stand.update {
            it.copy(
                icons = it.icons + (neu.vorlageId to neu),
                hinweis = if (eingepasst.verkleinert) {
                    "Übernommen — auf ${eingepasst.breite} × ${eingepasst.hoehe} Punkte verkleinert."
                } else {
                    "Übernommen."
                },
            )
        }
        standNachladen()
    }

    fun drehungUmlegen() = bearbeiten { packId, vorlageId ->
        val icon = _stand.value.icons[vorlageId] ?: return@bearbeiten
        val neu = !icon.dreht
        wege.drehung(packId, vorlageId, neu)
        _stand.update {
            it.copy(
                icons = it.icons + (vorlageId to icon.copy(dreht = neu)),
                hinweis = if (neu) {
                    "Dreht jetzt mit dem Kurs."
                } else {
                    "Steht jetzt still, egal wohin das Fahrzeug fährt."
                },
            )
        }
    }

    /**
     * Schreibt die ganze Lichtliste — die leere nimmt alle Lichter weg.
     *
     * Die Koordinaten sind 0–1 auf dem Bild, nicht auf der Bühne: Das Bild kann
     * beim Ersetzen andere Maße bekommen, und die Punkte sollen dann immer noch
     * „auf dem Dach" liegen.
     */
    fun blaulichtSetzen(neu: List<Blaulicht>, wahl: Int? = _stand.value.lichtwahl, meldung: String? = null) =
        bearbeiten { packId, vorlageId ->
            val icon = _stand.value.icons[vorlageId] ?: return@bearbeiten
            val vorher = icon.blaulichter.size

            wege.blaulicht(packId, vorlageId, neu)

            _stand.update {
                it.copy(
                    icons = it.icons + (vorlageId to icon.copy(blaulichter = neu)),
                    lichtwahl = if (wahl != null && wahl < neu.size) wahl else null,
                    hinweis = meldung ?: when {
                        neu.isEmpty() -> "Lichter entfernt — es blitzt wieder das ganze Bild."
                        neu.size < vorher -> "Licht entfernt."
                        neu.size == 1 -> "Das erste Blaulicht ist gesetzt und ausgewählt."
                        else -> "Das Icon trägt jetzt ${neu.size} Blaulichter."
                    },
                )
            }
        }

    /** Das Icon herausnehmen — für diesen Typ zeichnet das Spiel dann wieder selbst. */
    fun entfernen() = bearbeiten { packId, vorlageId ->
        wege.iconLoeschen(packId, vorlageId)
        _stand.update {
            it.copy(
                icons = it.icons - vorlageId,
                lichtwahl = null,
                hinweis = "Entfernt — für diesen Typ zeichnet das Spiel wieder selbst.",
            )
        }
        standNachladen()
    }

    /** Nur die belegten Bytes des Packs nachziehen — eine veraltete Zahl ist keine Meldung wert. */
    private suspend fun standNachladen() {
        runCatching { wege.packs() }.onSuccess { packs ->
            _stand.update { s ->
                s.copy(packBytes = packs.firstOrNull { it.id == s.packId }?.bytes ?: s.packBytes)
            }
        }
    }
}

/** Was beim Öffnen des Editors auf einmal geholt wird. */
private class Iconladung(
    val typen: List<Icontyp>,
    val icons: List<Packicon>,
    val grenzen: IconGrenzen,
    val packs: List<Iconpack>,
)

/** Was Bibliothek und Editor zeigen. */
data class Iconstand(
    /** Der eingestellte Server — die Bilder kommen als Pfad ohne ihn. */
    val server: String = "",
    val packs: List<Iconpack> = emptyList(),
    val grenzen: IconGrenzen? = null,
    /** Alle Fahrzeugtypen — die Bezugsgröße hinter „12 von 108 belegt". */
    val typen: List<Icontyp> = emptyList(),
    val laedt: Boolean = true,
    /** Ein Handgriff der Bibliothek läuft. */
    val sendet: Boolean = false,
    val fehler: String? = null,
    /** Welches Pack gerade seinen Code zeigt: Pack-Id und Code. */
    val codeVon: Pair<String, String>? = null,
    /** Der Bericht des letzten Zip-Imports. */
    val importiert: Packimport? = null,
    // Der Editor
    val packId: String = "",
    val packname: String = "",
    val packBytes: Long = 0,
    val icons: Map<String, Packicon> = emptyMap(),
    val gewaehlt: String? = null,
    /** Ein Handgriff des Editors läuft. */
    val laeuft: Boolean = false,
    val hinweis: String? = null,
    /** Das ausgewählte Licht — der eine Gegenstand, dessen Regler zu sehen sind. */
    val lichtwahl: Int? = null,
) {
    /** Ob keine weiteren Packs mehr gehen. */
    val voll: Boolean get() = grenzen != null && packs.size >= grenzen.maxPacks
}
