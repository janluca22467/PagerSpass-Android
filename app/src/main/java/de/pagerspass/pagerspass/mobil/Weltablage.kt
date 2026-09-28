package de.pagerspass.pagerspass.mobil

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Was die Welt auf diesem Gerät behält — die Ebenen der Karte und der Stand der
 * Einführung. Im Web liegt beides im `localStorage` (`pagerspass.welt.ebenen`,
 * `pagerspass.welt.einfuehrung.<kennung>`).
 *
 * <b>Ein eigener Speicher neben `Ablage`.</b> Dort steht, wer man ist; hier, wie man seine
 * Karte mag. Ein Gerät, das die Welt nie betreten hat, legt diese Datei gar nicht erst an.
 */
private val Context.weltablage by preferencesDataStore(name = "pagerspass_welt")

class Weltablage(private val zusammenhang: Context) {

    private val ebenenSchluessel = stringPreferencesKey("ebenen")

    /**
     * Die gemerkten Ebenen — nur bekannte Schlüssel: Eine ältere Fassung darf keine
     * Ebene mitbringen, die es nicht mehr gibt, und keine fehlen lassen.
     */
    suspend fun ebenen(): Map<String, Boolean> {
        val roh = zusammenhang.weltablage.data.first()[ebenenSchluessel] ?: return emptyMap()
        return roh.split(",")
            .mapNotNull { teil ->
                val (name, wert) = teil.split("=").takeIf { it.size == 2 } ?: return@mapNotNull null
                name to (wert == "1")
            }
            .toMap()
    }

    suspend fun ebenenSetzen(ebenen: Map<String, Boolean>) {
        zusammenhang.weltablage.edit { stand ->
            stand[ebenenSchluessel] = ebenen.entries.joinToString(",") {
                "${it.key}=${if (it.value) 1 else 0}"
            }
        }
    }

    /** Wie weit die Einführung für dieses Konto ist — geschaffte Schritte und ob beendet. */
    suspend fun einfuehrung(kennung: String): Pair<Int, Boolean> {
        val roh = zusammenhang.weltablage.data.first()[einfuehrungSchluessel(kennung)]
            ?: return 0 to false
        val teile = roh.split(",")
        return (teile.getOrNull(0)?.toIntOrNull() ?: 0) to (teile.getOrNull(1) == "1")
    }

    suspend fun einfuehrungSetzen(kennung: String, geschafft: Int, beendet: Boolean) {
        zusammenhang.weltablage.edit { stand ->
            stand[einfuehrungSchluessel(kennung)] = "$geschafft,${if (beendet) 1 else 0}"
        }
    }

    private fun einfuehrungSchluessel(kennung: String) = stringPreferencesKey("einfuehrung.$kennung")
}
