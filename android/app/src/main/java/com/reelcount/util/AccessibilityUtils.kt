package com.reelcount.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.reelcount.service.ReelCountAccessibilityService

/** Helpers for reading and opening the system accessibility settings. */
object AccessibilityUtils {

    /**
     * True when ReelCount's accessibility service is listed as enabled.
     * Reads the same secure setting the system settings app writes.
     */
    fun isServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, ReelCountAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expectedFlat = expected.flattenToString()
        val expectedShort = expected.flattenToShortString()

        return enabled.split(':').any { entry ->
            entry.equals(expectedFlat, ignoreCase = true) ||
                entry.equals(expectedShort, ignoreCase = true)
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** True when the app may draw over other apps. */
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:" + context.packageName)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
