package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object DownloadNotificationHelper {

    private const val CHANNEL_ID = "newtube_downloads_channel"
    private const val CHANNEL_NAME = "Video Downloads"
    private const val CHANNEL_DESC = "Shows progress and completion notifications for downloaded videos and music"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Shows or updates active download progress notification
     * E.g.: "Ap ki video download ho rahi hai..." with progress bar
     */
    fun showProgressNotification(
        context: Context,
        notificationId: Int,
        title: String,
        progressPercent: Int,
        speed: String
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Downloading Video • $progressPercent%")
                .setContentText("${title.take(32)}... ($speed)")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progressPercent, false)
                .setContentIntent(pendingIntent)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Shows notification when download is starting
     */
    fun showStartingNotification(
        context: Context,
        notificationId: Int,
        title: String
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Download Shuru Ho Gaya Hai")
                .setContentText("Aap ki video download ho rahi hai: ${title.take(30)}...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setContentIntent(pendingIntent)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Shows notification when download completes successfully and is saved to Gallery
     */
    fun showCompletedNotification(
        context: Context,
        notificationId: Int,
        title: String
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("✅ Video Download Mukammal!")
                .setContentText("Aap ki video download ho chuki hai aur Gallery me save ho gayi hai")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Aap ki video \"$title\" download ho chuki hai aur bina kisi watermark ke phone ki Gallery me save ho gayi hai.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(pendingIntent)

            val manager = NotificationManagerCompat.from(context)
            manager.cancel(notificationId) // remove ongoing
            manager.notify(notificationId + 10000, builder.build())
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(notificationId)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
