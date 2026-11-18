package com.reelcount.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.reelcount.R

/** Notification channels used by the foreground service and the blocker. */
object NotificationChannels {

    const val CHANNEL_SERVICE = "reelcount_service"
    const val CHANNEL_BLOCKER = "reelcount_blocker"

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

        val blockerChannel = NotificationChannel(
            CHANNEL_BLOCKER,
            context.getString(R.string.notification_channel_blocker_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_blocker_description)
            setShowBadge(true)
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(blockerChannel)
    }
}
