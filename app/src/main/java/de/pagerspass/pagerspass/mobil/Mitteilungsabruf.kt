package de.pagerspass.pagerspass.mobil

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.pagerspass.pagerspass.R
import de.pagerspass.pagerspass.netz.Ablage
import de.pagerspass.pagerspass.netz.Netz
import de.pagerspass.pagerspass.netz.Spielwege
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Der Mitteilungsweg ohne Google — Abruf statt Push.
 *
 * <b>Warum kein FCM.</b> Der Server kennt nur Web-Push (VAPID an Browser);
 * ein Google-Kanal bräuchte ein Firebase-Projekt, dessen Schlüssel und einen
 * zweiten Versandweg auf dem Server. Der Abruf braucht nichts davon: Ein
 * WorkManager-Lauf holt alle Viertelstunde, was ohnehin als REST daliegt —
 * Rundeneinladungen, Verwarnungen, Verwaltungsnachrichten — und meldet das
 * Neue als Systemmeldung. Die sechs Schalter des Kontos gelten dabei genauso,
 * wie der Server sie beim Web-Push anwendet.
 *
 * <b>Was der Abruf bewusst nicht kann:</b> Alarme bei komplett beendeter App.
 * Die erreichen das Gerät im Dienst über die stehende Verbindung samt
 * `Meldermeldung`; wer die App aus dem Speicher wirft, ist nicht mehr im
 * Dienst — die Runde holt ihn beim nächsten Start zurück.
 *
 * <b>Der Takt ist die Untergrenze des Systems.</b> Fünfzehn Minuten sind das
 * Minimum für periodische Arbeit; Android verschiebt die Läufe im Doze. Für
 * Einladungen und Verwaltungspost reicht das — für mehr bräuchte es FCM.
 */
object Mitteilungsabruf {

    private const val KANAL = "hinweise"
    private const val ABRUF = "mitteilungsabruf"
    private const val ERINNERUNG = "tageserinnerung"

    fun kanalAnlegen(zusammenhang: Context) {
        val verwalter = zusammenhang.getSystemService(NotificationManager::class.java) ?: return
        verwalter.createNotificationChannel(
            NotificationChannel(
                KANAL,
                "Hinweise",
                // Normal wichtig, kein Alarmton: Eine Einladung ist keine
                // Alarmierung — die hat ihren eigenen Kanal.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Einladungen und Nachrichten der Verwaltung" },
        )
    }

    /** Beide Läufe anmelden — idempotent, `KEEP` lässt Bestehendes stehen. */
    fun einrichten(zusammenhang: Context) {
        kanalAnlegen(zusammenhang)
        val netzNoetig = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        WorkManager.getInstance(zusammenhang).enqueueUniquePeriodicWork(
            ABRUF,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<Abrufwerker>(15, TimeUnit.MINUTES)
                .setConstraints(netzNoetig)
                .build(),
        )

        // Einmal am Tag, um 12 Uhr Europe/Berlin — wie die Tageserinnerung
        // des Web-Push. Der erste Lauf wird auf die nächste Mittagsstunde
        // gelegt, danach trägt der 24-Stunden-Takt.
        WorkManager.getInstance(zusammenhang).enqueueUniquePeriodicWork(
            ERINNERUNG,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<Erinnerungswerker>(24, TimeUnit.HOURS)
                .setInitialDelay(bisZwoelfUhr(), TimeUnit.MILLISECONDS)
                .setConstraints(netzNoetig)
                .build(),
        )
    }

    private fun bisZwoelfUhr(): Long {
        val berlin = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
        val jetzt = berlin.timeInMillis
        berlin.set(Calendar.HOUR_OF_DAY, 12)
        berlin.set(Calendar.MINUTE, 0)
        berlin.set(Calendar.SECOND, 0)
        if (berlin.timeInMillis <= jetzt) berlin.add(Calendar.DAY_OF_YEAR, 1)
        return berlin.timeInMillis - jetzt
    }

    /**
     * Eine Systemmeldung zeigen, die beim Antippen an [route] führt.
     *
     * <b>Die Kennung ist zugleich der Anfragecode des `PendingIntent`.</b> Mit
     * einem festen Code teilten sich alle Meldungen einen Intent, und
     * `FLAG_UPDATE_CURRENT` schriebe jeder früheren das Ziel der jüngsten ein.
     */
    internal fun melden(
        zusammenhang: Context,
        kennung: Int,
        titel: String,
        text: String,
        route: String,
    ) {
        if (!Meldermeldung.erlaubt(zusammenhang)) return
        val verwalter = zusammenhang.getSystemService(NotificationManager::class.java) ?: return

        val oeffnen = PendingIntent.getActivity(
            zusammenhang,
            kennung,
            Einsprung.absicht(zusammenhang, route),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        verwalter.notify(
            kennung,
            NotificationCompat.Builder(zusammenhang, KANAL)
                .setSmallIcon(R.drawable.ic_melder)
                .setContentTitle(titel)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(oeffnen)
                .build(),
        )
    }
}

/**
 * Der Viertelstunden-Lauf: holt, was neu ist, und meldet es einmal.
 *
 * <b>Der Merkzettel wird je Lauf frisch geschrieben.</b> Gemerkt wird, was der
 * Server gerade liefert — eine zurückgezogene Einladung fällt damit von
 * selbst wieder heraus und kann, falls sie wiederkommt, erneut melden.
 */
class Abrufwerker(
    zusammenhang: Context,
    parameter: WorkerParameters,
) : CoroutineWorker(zusammenhang, parameter) {

    override suspend fun doWork(): Result {
        val ablage = Ablage(applicationContext)
        val kennung = ablage.kennung() ?: return Result.success()
        if (ablage.merkmal() == null) return Result.success()

        val wege = Spielwege(Netz(ablage))
        val einstellungen = runCatching { wege.mitteilungseinstellungen(kennung) }
            .getOrNull() ?: return Result.retry()

        val gemeldet = ablage.gemeldeteHinweise()
        val jetztDa = mutableSetOf<String>()

        if (einstellungen.einladungen) {
            runCatching { wege.einladungen(kennung) }.getOrNull()?.forEach { e ->
                val schluessel = "einladung:${e.nr}"
                jetztDa += schluessel
                if (schluessel !in gemeldet && e.annehmbar) {
                    Mitteilungsabruf.melden(
                        applicationContext,
                        schluessel.hashCode(),
                        "Einladung — ${e.ort.ifBlank { e.roomCode }}",
                        "${e.vonName} lädt dich in eine Runde ein",
                        // Die Einladungen stehen unter Freunde → Kontakte, samt Annehmen.
                        route = FreundeWeg.KONTAKTE,
                    )
                }
            }
        }

        // Verwarnungen und Verwaltungspost haben keinen Schalter — mit
        // Absicht: Was die Verwaltung sagt, ist nicht abbestellbar.
        runCatching { wege.verwarnungen(kennung) }.getOrNull()?.forEach { v ->
            val schluessel = "verwarnung:${v.nr}"
            jetztDa += schluessel
            if (schluessel !in gemeldet) {
                Mitteilungsabruf.melden(
                    applicationContext,
                    schluessel.hashCode(),
                    "Verwarnung",
                    v.grund.take(140),
                    // Die Blende zum Bestätigen steht über dem Startbildschirm.
                    route = Weg.Dienst.adresse,
                )
            }
        }

        runCatching { wege.adminNachrichten(kennung) }.getOrNull()?.forEach { n ->
            val schluessel = "admin:${n.nr}"
            jetztDa += schluessel
            if (schluessel !in gemeldet) {
                Mitteilungsabruf.melden(
                    applicationContext,
                    schluessel.hashCode(),
                    n.titel.ifBlank { "Nachricht der Verwaltung" },
                    n.text.take(140),
                    route = Weg.Dienst.adresse,
                )
            }
        }

        ablage.hinweiseGemeldetMerken(jetztDa)
        return Result.success()
    }
}

/** Einmal täglich: „Deine Schicht wartet" — nur wenn gewünscht und nicht im Dienst. */
class Erinnerungswerker(
    zusammenhang: Context,
    parameter: WorkerParameters,
) : CoroutineWorker(zusammenhang, parameter) {

    override suspend fun doWork(): Result {
        val ablage = Ablage(applicationContext)
        val kennung = ablage.kennung() ?: return Result.success()
        if (ablage.merkmal() == null) return Result.success()
        // Wer gerade im Dienst ist, braucht keine Erinnerung an den Dienst.
        if (ablage.offeneRunde() != null) return Result.success()

        val wege = Spielwege(Netz(ablage))
        val einstellungen = runCatching { wege.mitteilungseinstellungen(kennung) }
            .getOrNull() ?: return Result.success()
        if (!einstellungen.tageserinnerung) return Result.success()

        Mitteilungsabruf.melden(
            applicationContext,
            "tageserinnerung".hashCode(),
            "PagerSpass",
            "Die Schicht des Tages wartet — einmal fahren, Punkte mitnehmen.",
            route = Weg.Dienst.adresse,
        )
        return Result.success()
    }
}
