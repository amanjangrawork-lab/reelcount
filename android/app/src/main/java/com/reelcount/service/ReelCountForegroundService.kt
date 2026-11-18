package com.reelcount.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.reelcount.MainActivity
import com.reelcount.R
import com.reelcount.data.CountRepository
import com.reelcount.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Small persistent service whose only job is a live notification with today's
 * count and a Stop action. Keeping a foreground service alive also keeps the
 * process (and therefore the accessibility service) warm.
 */
class ReelCountForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        NotificationChannels.ensureCreated(this)
        startForegroundCompat(count)

        scope.launch {
            try {
                val total = CountRepository.get(applicationContext).todayTotal()
                count = total
                updateCount(applicationContext, total)
            } catch (error: Exception) {
                Log.w(TAG, "Could not read today's total", error)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat(total: Int) {
        val notification = buildNotification(this, total)
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } catch (error: Exception) {
            Log.e(TAG, "Could not enter foreground state", error)
            stopSelf()
        }
    }

    private fun stopSelfSafely() {
        running = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "ReelCountService"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN = 10
        private const val REQUEST_STOP = 11

        const val ACTION_STOP = "com.reelcount.action.STOP_SERVICE"

        @Volatile
        private var running: Boolean = false

        @Volatile
        private var count: Int = 0

        fun start(context: Context) {
            if (running) {
                return
            }
            val intent = Intent(context, ReelCountForegroundService::class.java)
            runCatching { context.startForegroundService(intent) }
                .onFailure { Log.w(TAG, "Could not start counter service", it) }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReelCountForegroundService::class.java)
                .setAction(ACTION_STOP)
            runCatching { context.startService(intent) }
                .onFailure { Log.w(TAG, "Could not stop counter service", it) }
        }

        /** Refreshes the notification text with the new total. */
        fun updateCount(context: Context, total: Int) {
            count = total
            if (!running) {
                return
            }
            runCatching {
                val manager = NotificationManagerCompat.from(context)
                if (manager.areNotificationsEnabled()) {
                    manager.notify(NOTIFICATION_ID, buildNotification(context, total))
                }
            }.onFailure { Log.w(TAG, "Could not update notification", it) }
        }

        /**
         * The one notification builder. Both the initial foreground notification
         * and every later update go through it, so the tap target and the Stop
         * action cannot drift apart.
         */
        private fun buildNotification(context: Context, total: Int): Notification {
            val openApp = PendingIntent.getActivity(
                context,
                REQUEST_OPEN,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stop = PendingIntent.getService(
                context,
                REQUEST_STOP,
                Intent(context, ReelCountForegroundService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(context, NotificationChannels.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_stat_reelcount)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text, total))
                .setContentIntent(openApp)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(0, context.getString(R.string.notification_stop), stop)
                .build()
        }
    }
}
