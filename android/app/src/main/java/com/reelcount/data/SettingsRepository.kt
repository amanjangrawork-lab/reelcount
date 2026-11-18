package com.reelcount.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.reelcount.db.ReelCountDatabase
import com.reelcount.db.Settings
import com.reelcount.util.PerAppCounts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single write path for settings.
 *
 * Room is the authoritative, documented store (the `settings` table). Because
 * the accessibility service reads settings on every scroll event, reads are
 * served from an EncryptedSharedPreferences mirror that is loaded once and
 * kept in memory. Every write updates the mirror, the in-memory cache and the
 * Room row together.
 */
class SettingsRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = openPrefs(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cached: ReelCountSettings = readFromPrefs(prefs)

    fun current(): ReelCountSettings = cached

    fun updateLimit(limit: Int) {
        write(cached.copy(limit = ReelCountSettings.coerceLimit(limit)))
    }

    fun setBlockEnabled(enabled: Boolean) {
        write(cached.copy(blockEnabled = enabled))
    }

    fun setOverlayEnabled(enabled: Boolean) {
        write(cached.copy(overlayEnabled = enabled))
    }

    fun setPerAppEnabled(pkg: String, enabled: Boolean) {
        val toggles = cached.perAppEnabled.toMutableMap()
        toggles[pkg] = enabled
        write(cached.copy(perAppEnabled = toggles))
    }

    fun setAllDayLockUntil(untilEpochMillis: Long) {
        write(cached.copy(allDayLockUntil = untilEpochMillis.coerceAtLeast(0L)))
    }

    fun clearAllDayLock() {
        write(cached.copy(allDayLockUntil = 0L))
    }

    /** Called at boot so an expired lock does not linger in the store. */
    fun clearExpiredLockIfNeeded() {
        val until = cached.allDayLockUntil
        if (until > 0L && until <= System.currentTimeMillis()) {
            clearAllDayLock()
        }
    }

    /** Reloads the mirror from Room, called once on startup. */
    suspend fun refreshFromDatabase() = withContext(Dispatchers.IO) {
        val stored = ReelCountDatabase.get(appContext).settingsDao().get() ?: return@withContext
        val snapshot = ReelCountSettings(
            limit = ReelCountSettings.coerceLimit(stored.limit),
            blockEnabled = stored.blockEnabled,
            overlayEnabled = stored.overlayEnabled,
            allDayLockUntil = stored.allDayLockUntil,
            perAppEnabled = PerAppCounts.parseToggles(stored.perAppTogglesJson)
        )
        cached = snapshot
        persistToPrefs(prefs, snapshot)
    }

    private fun write(snapshot: ReelCountSettings) {
        cached = snapshot
        persistToPrefs(prefs, snapshot)

        scope.launch {
            try {
                ReelCountDatabase.get(appContext).settingsDao().upsert(
                    Settings(
                        id = Settings.SINGLETON_ID,
                        limit = snapshot.limit,
                        blockEnabled = snapshot.blockEnabled,
                        overlayEnabled = snapshot.overlayEnabled,
                        allDayLockUntil = snapshot.allDayLockUntil,
                        perAppTogglesJson = PerAppCounts.serializeToggles(snapshot.perAppEnabled)
                    )
                )
            } catch (error: Exception) {
                Log.w(TAG, "Could not mirror settings to Room", error)
            }
        }
    }

    private fun persistToPrefs(prefs: SharedPreferences, snapshot: ReelCountSettings) {
        prefs.edit()
            .putInt(KEY_LIMIT, snapshot.limit)
            .putBoolean(KEY_BLOCK_ENABLED, snapshot.blockEnabled)
            .putBoolean(KEY_OVERLAY_ENABLED, snapshot.overlayEnabled)
            .putLong(KEY_ALL_DAY_LOCK_UNTIL, snapshot.allDayLockUntil)
            .putString(KEY_PER_APP, PerAppCounts.serializeToggles(snapshot.perAppEnabled))
            .apply()
    }

    private fun readFromPrefs(prefs: SharedPreferences): ReelCountSettings {
        return ReelCountSettings(
            limit = ReelCountSettings.coerceLimit(
                prefs.getInt(KEY_LIMIT, ReelCountSettings.DEFAULT_LIMIT)
            ),
            blockEnabled = prefs.getBoolean(KEY_BLOCK_ENABLED, false),
            overlayEnabled = prefs.getBoolean(KEY_OVERLAY_ENABLED, true),
            allDayLockUntil = prefs.getLong(KEY_ALL_DAY_LOCK_UNTIL, 0L),
            perAppEnabled = PerAppCounts.parseToggles(prefs.getString(KEY_PER_APP, null))
        )
    }

    companion object {
        private const val TAG = "SettingsRepository"
        private const val PREFS_NAME = "reelcount_settings_encrypted"
        private const val FALLBACK_PREFS_NAME = "reelcount_settings"

        private const val KEY_LIMIT = "limit"
        private const val KEY_BLOCK_ENABLED = "block_enabled"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_ALL_DAY_LOCK_UNTIL = "all_day_lock_until"
        private const val KEY_PER_APP = "per_app_toggles"

        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
        }

        /**
         * Encrypted preferences need a working keystore. On devices where the
         * keystore is broken the mirror degrades to private, unencrypted
         * preferences instead of crashing the app; Room stays authoritative.
         */
        private fun openPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (error: Exception) {
                Log.w(TAG, "Encrypted preferences unavailable, using private preferences", error)
                context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }
}
