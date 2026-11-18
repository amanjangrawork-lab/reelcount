package com.reelcount.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reelcount.data.SettingsRepository
import com.reelcount.util.AccessibilityUtils

/**
 * Restarts the counter service after a reboot or an app update.
 *
 * The accessibility service itself is restored by the system, so all that is
 * needed here is the foreground service and a settings refresh.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        try {
            SettingsRepository.get(context).clearExpiredLockIfNeeded()
        } catch (error: Exception) {
            Log.w(TAG, "Could not refresh settings on boot", error)
        }

        if (AccessibilityUtils.isServiceEnabled(context)) {
            ReelCountForegroundService.start(context)
        }
    }

    private companion object {
        const val TAG = "ReelCountBoot"
    }
}
