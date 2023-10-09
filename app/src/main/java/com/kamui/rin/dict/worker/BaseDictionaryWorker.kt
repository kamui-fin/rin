package com.kamui.rin.dict.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kamui.rin.CopyToClipboardReceiver
import com.kamui.rin.R

abstract class BaseDictionaryWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    abstract fun getNotificationId(): Int

    fun onException(error: Throwable) {
        val copyIntent = Intent(applicationContext, CopyToClipboardReceiver::class.java).apply {
            action = "COPY_TO_CLIPBOARD"
            putExtra("textToCopy", error.localizedMessage)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )


        val id = applicationContext.getString(R.string.notification_channel_id)
        val title = "Application Error"

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setOnlyAlertOnce(true)
            .setTicker(title)
            .setContentText(error.localizedMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(error.localizedMessage))
            .addAction(R.drawable.baseline_content_copy_24, "Copy Error", copyPendingIntent)
            .setSmallIcon(R.drawable.baseline_menu_book_24)
            .build()

        notificationManager.notify(400, notification)
    }

    private fun getNotification(text: String): Notification {
        val id = applicationContext.getString(R.string.notification_channel_id)
        val title = applicationContext.getString(R.string.notification_title)
        return NotificationCompat.Builder(applicationContext, id)
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.baseline_menu_book_24)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()
    }

    fun createForegroundInfo(progress: String): ForegroundInfo {
        createChannel()
        return ForegroundInfo(getNotificationId(), getNotification(progress))
    }

    fun updateNotification(status: String) {
        notificationManager.notify(getNotificationId(), getNotification(status))
    }

    private fun createChannel() {
        val channelId = applicationContext.getString(R.string.notification_channel_id)
        val descriptionText = applicationContext.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, "Rin notification channel", importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }
}