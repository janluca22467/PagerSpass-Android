package de.pagerspass.pagerspass.netz

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/*
 * ---------------------------------------------------------------------------
 * Übertragung einer Schicht („Streamer-Modus") — Frage, Zusage, Weg.
 *
 * <b>Eigene Datei, mit Absicht.</b> Die Einwilligung wird an zwei Stellen
 * gebraucht: beim Beitritt in eine übertragene Runde (hier, Runde Teil 1) und
 * auf der Privatsphäre-Seite (Bereich Konto). Beide Stränge sind parallel
 * entstanden; was hier steht, ist bewusst an einem Ort, damit beim
 * Zusammenführen doppelt Gebautes auf einen Blick zu finden ist.
 * ---------------------------------------------------------------------------
 */

/**
 * Die Frage, in die eingewilligt werden soll — Spiegel von
 * `EinwilligungsbedarfDto` (`JoinResult.einwilligung`).
 *
 * <b>Warum sie vom Server kommt.</b> Beim Beitritt kennt der Client den Raum
 * noch nicht: Kanal, Plattform und Mitschnitt stehen in Einstellungen, die er
 * erst mit dem Raumzustand bekäme — und den bekommt er gerade nicht.
 */
@Serializable
data class Uebertragungsfrage(
    val raumCode: String = "",
    /** Wer überträgt — der Anzeigename der Leitstelle dieser Runde. */
    val streamerName: String = "",
    /** `Twitch`, `YouTube`, `TikTok`, `Kick`, `Discord`, `Andere`. */
    val plattform: String = "Twitch",
    val kanal: String = "",
    val aufzeichnung: Boolean = false,
    /** Die Fassung, die anzuzeigen ist — sie geht beim Erteilen zurück. */
    val fassung: String = "",
    /** Ab welchem Alter man allein einwilligt; darunter kommen die Eltern dazu. */
    val mindestalterAllein: Int = UEBERTRAGUNG_MINDESTALTER,
    /** Ob für diesen Kanal schon ein Vorgang bei den Erziehungsberechtigten liegt. */
    val wartetAufEltern: Boolean = false,
    /** Die Adresse des Bogens für die Eltern; `null`, wenn keiner läuft. */
    val elternbogen: String? = null,
)

/**
 * Die geschriebene Einwilligung — die Antwort auf `POST /api/streaming/{k}/einwilligung`.
 *
 * <b>Sie kann gültig sein oder nicht.</b> Bei „unter 18" wartet sie auf den
 * Bogen der Erziehungsberechtigten; dann bleibt der Dialog offen und zeigt ihn.
 */
@Serializable
data class Uebertragungszusage(
    val id: String = "",
    val streamerName: String = "",
    val plattform: String = "Twitch",
    val kanal: String = "",
    val aufzeichnung: Boolean = false,
    val erteiltUm: String = "",
    val laeuftAbUm: String = "",
    val volljaehrig: Boolean = false,
    /** Ob sie wirkt — bei „unter 18" zunächst nicht. */
    val gilt: Boolean = false,
    val wartetAufEltern: Boolean = false,
    val elternzustimmungUm: String? = null,
    val widerrufenUm: String? = null,
    val fassung: String = "",
    val elternbogen: String? = null,
)

/** Der Weg zum Server — eine Anfrage, sonst nichts. */
class Uebertragungswege(private val netz: Netz) {

    /**
     * Die Einwilligung erteilen.
     *
     * Die Fassung ist die, die der Dialog angezeigt hat. Stimmt sie nicht mit der
     * des Servers, weist er ab — dann ist diese App älter als der Text.
     */
    suspend fun einwilligen(
        kennung: String,
        raumCode: String,
        volljaehrig: Boolean,
    ): Uebertragungszusage = netz.hole(
        "/api/streaming/${URLEncoder.encode(kennung, "UTF-8")}/einwilligung",
        "POST",
        buildJsonObject {
            put("raumCode", raumCode)
            put("volljaehrig", volljaehrig)
            put("fassung", UEBERTRAGUNG_FASSUNG)
        }.toString(),
    )
}

/**
 * Fassung des Einwilligungstextes — muss zeichengleich `UEBERTRAGUNG_FASSUNG`
 * in `web/src/recht/rechtstexte.ts` sein (und `Streamingrecht.Fassung` am Server).
 */
const val UEBERTRAGUNG_FASSUNG = "2026-09-15"

/** Ab diesem Alter willigt man allein ein — Spiegel von `MINDESTALTER_EINWILLIGUNG`. */
const val UEBERTRAGUNG_MINDESTALTER = 18

/** Die Plattformen ausgeschrieben — „Andere" heißt hier „Anderswo", eine Ortsangabe. */
val STREAMERPLATTFORMEN: List<Pair<String, String>> = listOf(
    "Twitch" to "Twitch",
    "YouTube" to "YouTube",
    "TikTok" to "TikTok",
    "Kick" to "Kick",
    "Discord" to "Discord",
    "Andere" to "Anderswo",
)

/**
 * Der Wortlaut, in den eingewilligt wird — wörtlich `UEBERTRAGUNG` aus
 * `web/src/recht/rechtstexte.ts`. Eine Einwilligung muss informiert sein; ein
 * Verweis auf eine Seite, die man erst öffnen muss, wäre keine.
 *
 * `## ` leitet eine Zwischenüberschrift ein, `- ` einen Listenpunkt, `**…**`
 * ist fett — dieselbe schmale Auszeichnung wie im Web.
 */
val UEBERTRAGUNG_TEXT: String = """
Diese Schicht wird von der Leitstelle nach außen übertragen — an ein Publikum, das wir nicht kennen und nicht auswählen. Damit dabei etwas von dir zu sehen und zu hören sein darf, brauchen wir deine Einwilligung. Sie ist freiwillig; ohne sie kannst du in dieser Runde nicht mitfahren, und das Spiel bleibt im Übrigen vollständig nutzbar.

## Wer überträgt

Der Kanal, die Plattform und die Angabe, ob mitgeschnitten wird, stehen in der Frage, die dir gestellt wird, sowie in der Lobby der Runde. Sie sind der Gegenstand deiner Einwilligung: Wechselt die Leitstelle den Kanal oder die Plattform, oder schaltet sie den Mitschnitt hinzu, wirst du erneut gefragt. Eine Einwilligung gilt nur für das, was in ihr steht.

Übertragen wird von der Leitstelle der Runde, nicht von uns. PagerSpass betreibt keinen Kanal, schneidet nichts mit und leitet nichts weiter.

## Was von dir dabei sein kann

- dein Anzeigename, dein Wappen, dein Rang und dein Profilbild, wie sie in Lobby und Mannschaftsliste stehen,
- deine **Stimme**, wenn du den Sprechfunk benutzt — und mit ihr alles, was sie über dich verrät,
- alles, was du in dieser Runde schreibst: Funksprüche, Lagemeldungen, Nachforderungen, Lobby-Chat, Einsatzstellenfunk und Leitstellendraht,
- deine Rolle, dein Fahrzeug, sein Funkrufname und was du damit tust.

Was **nicht** dabei sein kann, weil die Leitstelle es selbst nicht sieht: dein Benutzername, deine E-Mail-Adresse, dein Passwort, deine Direktnachrichten und alles Übrige aus deinem Konto.

## Was wir dazu speichern

Damit wir belegen können, dass du gefragt wurdest und was du geantwortet hast (Art. 7 Abs. 1 DSGVO): deine Kontokennung, die Kennung und den Anzeigenamen der übertragenden Leitstelle, Plattform, Kanal und Mitschnitt-Angabe, den Zeitpunkt, die Fassung dieses Textes samt Prüfwert, den Code der Runde, bei deren Gelegenheit du gefragt wurdest, deine Angabe zur Volljährigkeit und — als gesalzenen Hash, nicht im Klartext — IP-Adresse und Gerätekennung des Geräts, von dem aus du geantwortet hast. Näheres in Ziffer 6 b der Datenschutzerklärung.

## Wenn du noch nicht 18 bist

Dann kannst du diese Einwilligung nicht allein erteilen: Es geht um die Verbreitung deiner Stimme und deines Namens an ein öffentliches Publikum, und dafür müssen deine Erziehungsberechtigten zustimmen. Sag es in der Frage ehrlich — du bekommst dann einen Link zu einem Bogen, den ein Erziehungsberechtigter ausfüllt und unterschreibt. Bis der eingegangen ist, kommst du in eine übertragene Runde nicht hinein; in jede andere Runde weiterhin.

Dass du 18 bist oder älter, ist deine eigene Angabe. Wir prüfen sie nicht nach — dazu müssten wir Ausweisdaten erheben, und PagerSpass erhebt bewusst kein Geburtsdatum (Ziffer 2 der Datenschutzerklärung). Eine falsche Angabe hilft dir nicht: Sie macht die Einwilligung unwirksam und dich gegenüber deinen Erziehungsberechtigten erklärungsbedürftig.

## Wie lange, und wie du sie zurücknimmst

Die Einwilligung gilt für ein Jahr; danach fragen wir erneut. Du kannst sie **jederzeit** widerrufen, ohne Angabe von Gründen, unter „Konto → Privatsphäre → Übertragungen". Der Widerruf wirkt für die Zukunft: Ab ihm darf von dir nichts mehr übertragen werden. Die Rechtmäßigkeit dessen, was bis dahin lief, bleibt unberührt.

## Was wir nicht können

Wir können nicht rückholen, was schon gesendet wurde, und wir können nichts aus einem fremden Kanal löschen. Über den Inhalt eines Streams oder einer Aufzeichnung entscheidet allein die Person, die ihn betreibt — sie ist dafür auch verantwortlich. Wenn du willst, dass ein Mitschnitt verschwindet, ist sie die Adresse; wir können dabei nur vermitteln. Genau deshalb fragen wir **vorher**.

Verantwortlich für die Verarbeitung auf unserer Seite sind wir (Impressum). Für die Übertragung selbst ist die Leitstelle verantwortlich, die sie vornimmt. Fragen zu deiner Einwilligung: support@pagerspass.de
""".trim()
