package com.shotyou.app.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shotyou.app.R

/**
 * Builds and posts the notifications used by [GenerationWorker].
 *
 * One low-importance "Generation" channel is created lazily (once). The same notification id
 * ([NOTIF_ID]) is reused for the ongoing progress notification and the terminal (success/failure)
 * notification so a running job's notification is replaced in place when it finishes.
 *
 * All posting goes through [NotificationManagerCompat] and is guarded against a missing
 * `POST_NOTIFICATIONS` permission (Android 13+): a [SecurityException] simply no-ops.
 */
class GenerationNotifications(private val context: Context) {

    private var channelEnsured = false

    /** Create the notification channel once (no-op below API 26). */
    private fun ensureChannel() {
        if (channelEnsured) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_generation_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notif_channel_generation_desc)
            }
            manager.createNotificationChannel(channel)
        }
        channelEnsured = true
    }

    /** Ongoing, indeterminate progress notification ("Generating your photo…"). */
    fun buildProgress(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_generating_title))
            .setContentText(context.getString(R.string.notif_generating_text))
            .setSmallIcon(appIcon())
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildTerminal(titleRes: Int, textRes: Int): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(textRes))
            .setSmallIcon(appIcon())
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /** Post (or update) the ongoing progress notification. */
    fun notifyProgress() = post(NOTIF_ID, buildProgress())

    /** Post a success message under a SEPARATE id so it survives the foreground-service
     *  teardown (which cancels [NOTIF_ID]). */
    fun notifySuccess() =
        post(TERMINAL_ID, buildTerminal(R.string.notif_success_title, R.string.notif_success_text))

    /** Post a failure message under the separate terminal id. */
    fun notifyFailure() =
        post(TERMINAL_ID, buildTerminal(R.string.notif_failed_title, R.string.notif_failed_text))

    private fun post(id: Int, notification: Notification) {
        // Guard a missing POST_NOTIFICATIONS permission (Android 13+) — just no-op.
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // No notification permission; ignore.
        }
    }

    // A dedicated monochrome status-bar icon (a launcher/mipmap icon renders as a blank
    // square and can trigger "Bad notification for startForeground" on some OEMs).
    private fun appIcon(): Int = R.drawable.ic_stat_shotyou

    companion object {
        const val CHANNEL_ID = "shotyou_generation"
        const val NOTIF_ID = 4201
        const val TERMINAL_ID = 4202
    }
}
