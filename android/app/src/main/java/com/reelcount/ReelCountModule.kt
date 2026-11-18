package com.reelcount

import android.os.Bundle
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.reelcount.data.CountRepository
import com.reelcount.data.ReelCountSettings
import com.reelcount.data.SettingsRepository
import com.reelcount.util.AccessibilityUtils
import com.reelcount.util.PerAppCounts
import com.reelcount.util.TimeUtils
import com.reelcount.util.TrackedApps
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * JavaScript bridge. Exposed as `NativeModules.ReelCount` and reached through
 * `TurboModuleRegistry.getEnforcing('ReelCount')`, which resolves this module
 * through React Native's TurboModule interop layer.
 */
class ReelCountModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val settingsRepo = SettingsRepository.get(reactContext)
    private val countRepo = CountRepository.get(reactContext)

    init {
        ReelCountEvents.setEmitter { name, payload -> emitToJs(name, payload) }
        scope.launch {
            runCatching { settingsRepo.refreshFromDatabase() }
        }
    }

    override fun getName(): String = NAME

    // ------------------------------------------------------------- counting

    @ReactMethod
    fun getTodayCount(promise: Promise) {
        scope.launch {
            try {
                promise.resolve(countRepo.todayTotal())
            } catch (error: Exception) {
                promise.reject(ERROR_DB, error.message, error)
            }
        }
    }

    @ReactMethod
    fun getPerAppCounts(promise: Promise) {
        scope.launch {
            try {
                val counts = countRepo.perAppCounts()
                val map = Arguments.createMap()
                for (pkg in TrackedApps.ALL) {
                    map.putInt(pkg, counts[pkg] ?: 0)
                }
                for ((pkg, value) in counts) {
                    if (!TrackedApps.ALL.contains(pkg)) {
                        map.putInt(pkg, value)
                    }
                }
                promise.resolve(map)
            } catch (error: Exception) {
                promise.reject(ERROR_DB, error.message, error)
            }
        }
    }

    /** Daily totals for the last [days] days, oldest first, gaps filled with 0. */
    @ReactMethod
    fun getDailyTotals(days: Double, promise: Promise) {
        val dayCount = days.toInt().coerceIn(1, 365)
        scope.launch {
            try {
                val rows = countRepo.lastDays(dayCount)
                val byDay = rows.associateBy { it.dateEpochDay }
                val today = TimeUtils.todayEpochDay()
                val result = Arguments.createArray()

                for (offset in (dayCount - 1) downTo 0) {
                    val epochDay = today - offset
                    val row = byDay[epochDay]
                    val entry = Arguments.createMap()
                    entry.putDouble("dateEpochDay", epochDay.toDouble())
                    entry.putString(
                        "label",
                        LocalDate.ofEpochDay(epochDay).dayOfMonth.toString()
                    )
                    entry.putInt("total", row?.total ?: 0)
                    entry.putMap("perApp", perAppMap(row?.perAppJson))
                    result.pushMap(entry)
                }

                promise.resolve(result)
            } catch (error: Exception) {
                promise.reject(ERROR_DB, error.message, error)
            }
        }
    }

    /** Totals for the last [days] days plus per-app totals across that range. */
    @ReactMethod
    fun getRangeSummary(days: Double, promise: Promise) {
        val dayCount = days.toInt().coerceIn(1, 365)
        scope.launch {
            try {
                val rows = countRepo.lastDays(dayCount)
                val perAppTotals = mutableMapOf<String, Int>()
                var total = 0

                for (row in rows) {
                    total += row.total
                    for ((pkg, value) in PerAppCounts.parse(row.perAppJson)) {
                        perAppTotals[pkg] = (perAppTotals[pkg] ?: 0) + value
                    }
                }

                val perApp = Arguments.createMap()
                for (pkg in TrackedApps.ALL) {
                    perApp.putInt(pkg, perAppTotals[pkg] ?: 0)
                }

                val summary = Arguments.createMap()
                summary.putInt("days", dayCount)
                summary.putInt("total", total)
                summary.putInt("averagePerDay", if (dayCount > 0) total / dayCount else 0)
                summary.putInt("bestDay", rows.maxOfOrNull { it.total } ?: 0)
                summary.putMap("perApp", perApp)

                promise.resolve(summary)
            } catch (error: Exception) {
                promise.reject(ERROR_DB, error.message, error)
            }
        }
    }

    // ------------------------------------------------------------- settings

    @ReactMethod
    fun setLimit(limit: Double, promise: Promise) {
        try {
            settingsRepo.updateLimit(limit.toInt())
            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(ERROR_SETTINGS, error.message, error)
        }
    }

    @ReactMethod
    fun setBlockEnabled(enabled: Boolean, promise: Promise) {
        try {
            settingsRepo.setBlockEnabled(enabled)
            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(ERROR_SETTINGS, error.message, error)
        }
    }

    @ReactMethod
    fun setOverlayEnabled(enabled: Boolean, promise: Promise) {
        try {
            settingsRepo.setOverlayEnabled(enabled)
            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(ERROR_SETTINGS, error.message, error)
        }
    }

    @ReactMethod
    fun setPerAppEnabled(pkg: String, enabled: Boolean, promise: Promise) {
        try {
            settingsRepo.setPerAppEnabled(pkg, enabled)
            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(ERROR_SETTINGS, error.message, error)
        }
    }

    @ReactMethod
    fun setAllDayLockUntil(epochMillis: Double, promise: Promise) {
        try {
            settingsRepo.setAllDayLockUntil(epochMillis.toLong())
            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(ERROR_SETTINGS, error.message, error)
        }
    }

    @ReactMethod
    fun getSettings(promise: Promise) {
        try {
            promise.resolve(settingsToMap(settingsRepo.current()))
        } catch (error: Exception) {
            promise.reject(ERROR_SETTINGS, error.message, error)
        }
    }

    // ------------------------------------------------------------ permissions

    @ReactMethod
    fun isAccessibilityServiceEnabled(promise: Promise) {
        promise.resolve(AccessibilityUtils.isServiceEnabled(reactContext))
    }

    @ReactMethod
    fun openAccessibilitySettings(promise: Promise) {
        try {
            AccessibilityUtils.openAccessibilitySettings(reactContext)
            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(ERROR_PERMISSION, error.message, error)
        }
    }

    @ReactMethod
    fun isOverlayPermissionGranted(promise: Promise) {
        promise.resolve(AccessibilityUtils.canDrawOverlays(reactContext))
    }

    @ReactMethod
    fun requestOverlayPermission(promise: Promise) {
        try {
            AccessibilityUtils.openOverlaySettings(reactContext)
            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(ERROR_PERMISSION, error.message, error)
        }
    }

    // ---------------------------------------------------------------- events

    @ReactMethod
    fun addListener(eventName: String) {
        // Required by NativeEventEmitter consumers; this module emits through
        // the device event emitter, so there is nothing to subscribe to.
    }

    @ReactMethod
    fun removeListeners(count: Double) {
        // See addListener.
    }

    private fun emitToJs(nativeEvent: String, payload: Bundle) {
        val jsName = when (nativeEvent) {
            ReelCountEvents.EVENT_REEL_COUNTED -> "onReelCounted"
            ReelCountEvents.EVENT_LIMIT_REACHED -> "onLimitReached"
            ReelCountEvents.EVENT_BLOCK_STATE_CHANGED -> "onBlockStateChanged"
            else -> nativeEvent
        }

        try {
            reactContext.emitDeviceEvent(jsName, Arguments.fromBundle(payload))
        } catch (ignored: Exception) {
            // The JS instance may already be torn down.
        }
    }

    private fun perAppMap(json: String?): WritableMap {
        val map = Arguments.createMap()
        val counts = PerAppCounts.parse(json)
        for (pkg in TrackedApps.ALL) {
            map.putInt(pkg, counts[pkg] ?: 0)
        }
        return map
    }

    private fun settingsToMap(settings: ReelCountSettings): WritableMap {
        val map = Arguments.createMap()
        map.putInt("limit", settings.limit)
        map.putBoolean("blockEnabled", settings.blockEnabled)
        map.putBoolean("overlayEnabled", settings.overlayEnabled)
        map.putDouble("allDayLockUntil", settings.allDayLockUntil.toDouble())
        map.putBoolean("lockedNow", settings.isLockedNow())

        val toggles = Arguments.createMap()
        for (pkg in TrackedApps.ALL) {
            toggles.putBoolean(pkg, settings.isAppEnabled(pkg))
        }
        map.putMap("perAppEnabled", toggles)

        return map
    }

    private companion object {
        const val NAME = "ReelCount"
        const val ERROR_DB = "E_DB"
        const val ERROR_SETTINGS = "E_SETTINGS"
        const val ERROR_PERMISSION = "E_PERMISSION"
    }
}
