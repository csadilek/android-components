/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.app.DownloadManager.ACTION_VIEW_DOWNLOADS
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_NONE
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

internal object DownloadNotification: BroadcastReceiver() {

    private const val NOTIFICATION_CHANNEL_ID = "Downloads"

    /**
     * Responds to [PendingIntent]s fired by the site controls notification.
     */
    override fun onReceive(context: Context, intent: Intent) {
        // TODO: Not sure if this is the right place to override onReceieve

        // if intent is "pause intent" pause!
        // if intent is "resume intent" resume!

        Log.d("Sawyer", "onReceive!")
    }

    /**
     * Build the notification to be displayed while the download service is active.
     */
    fun createOngoingDownloadNotification(context: Context, fileName: String?, fileSize: Long?, pauseIntent: PendingIntent): Notification {
        val channelId = ensureChannelExists(context)

        // TODO: Maybe this?
        // createPendingIntent(ACTION_REFRESH, 2)

        val pauseAction =  NotificationCompat.Action.Builder(
                // TODO: Remove the drawable
                R.drawable.mozac_feature_download_ic_download,
                context.getString(R.string.mozac_feature_downloads_button_pause),
                pauseIntent
               // Pause the notification
        ).build()

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.mozac_feature_download_ic_ongoing_download)
            .setContentTitle(fileName)
            .setContentText(fileSize.toString())
            .setColor(ContextCompat.getColor(context, R.color.mozac_feature_downloads_notification))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(1, 0, true)
            .setOngoing(true)
            .addAction(pauseAction)
            .build()
    }

    /**
     * Build the notification to be displayed while the download service is active.
     */
    fun createPausedDownloadNotification(context: Context, fileName: String?): Notification {
        val channelId = ensureChannelExists(context)

        return NotificationCompat.Builder(context, channelId)
                // TODO: Set the drawable to be a pause icon
                .setSmallIcon(R.drawable.mozac_feature_download_ic_ongoing_download)
                .setContentTitle(fileName)
                .setContentText(context.getString(R.string.mozac_feature_downloads_paused_notification_text))
                .setColor(ContextCompat.getColor(context, R.color.mozac_feature_downloads_notification))
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setProgress(1, 0, true)
                .setOngoing(false)
                .build()
    }

    /**
     * Build the notification to be displayed when a download finishes.
     */
    fun createDownloadCompletedNotification(context: Context, fileName: String?): Notification {
        val channelId = ensureChannelExists(context)
        val intent = Intent(ACTION_VIEW_DOWNLOADS).apply {
            flags = FLAG_ACTIVITY_NEW_TASK
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.mozac_feature_download_ic_download)
            .setContentTitle(fileName)
            .setContentText(context.getString(R.string.mozac_feature_downloads_completed_notification_text2))
            .setColor(ContextCompat.getColor(context, R.color.mozac_feature_downloads_notification))
            .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
            .build()
    }

    /**
     * Build the notification to be displayed when a download fails to finish.
     */
    fun createDownloadFailedNotification(context: Context, fileName: String?): Notification {
        val channelId = ensureChannelExists(context)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.mozac_feature_download_ic_download)
            .setContentTitle(fileName)
            .setContentText(context.getString(R.string.mozac_feature_downloads_failed_notification_text2))
            .setColor(ContextCompat.getColor(context, R.color.mozac_feature_downloads_notification))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
    }

    /**
     * Check if notifications from the download channel are enabled.
     * Verifies that app notifications, channel notifications, and group notifications are enabled.
     */
    fun isChannelEnabled(context: Context): Boolean {
        return if (SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = context.getSystemService()!!
            if (!notificationManager.areNotificationsEnabled()) return false

            val channelId = ensureChannelExists(context)
            val channel = notificationManager.getNotificationChannel(channelId)
            if (channel.importance == IMPORTANCE_NONE) return false

            if (SDK_INT >= Build.VERSION_CODES.P) {
                val group = notificationManager.getNotificationChannelGroup(channel.group)
                group?.isBlocked != true
            } else {
                true
            }
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Make sure a notification channel for download notification exists.
     *
     * Returns the channel id to be used for download notifications.
     */
    private fun ensureChannelExists(context: Context): String {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = context.getSystemService()!!

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.mozac_feature_downloads_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannel(channel)
        }

        return NOTIFICATION_CHANNEL_ID
    }
}
