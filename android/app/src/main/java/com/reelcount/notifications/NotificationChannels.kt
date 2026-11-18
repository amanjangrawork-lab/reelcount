package com.reelcount.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.reelcount.R

/** Notification channel used by the foreground service. */
object NotificationChannels {

    const val CHANNEL_SERVICE = "reelcount_service"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            context.getString(R.string.notification_channel_service_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_service_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        manager.createNotificationChannel(serviceChannel)
    }
}
