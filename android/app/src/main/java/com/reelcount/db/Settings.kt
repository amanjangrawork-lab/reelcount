package com.reelcount.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row settings table (`id` is always [SINGLETON_ID]).
 *
 * [limit] is stored in the `daily_limit` column because `limit` is a reserved
 * SQLite keyword.
 */
@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ID,

    @ColumnInfo(name = "daily_limit", defaultValue = "100")
    val limit: Int = 100,

    @ColumnInfo(name = "blockEnabled", defaultValue = "0")
    val blockEnabled: Boolean = false,

    @ColumnInfo(name = "overlayEnabled", defaultValue = "1")
    val overlayEnabled: Boolean = true,

    /** Epoch millis until which every entry to Reels/Shorts is blocked. 0 = off. */
    @ColumnInfo(name = "allDayLockUntil", defaultValue = "0")
    val allDayLockUntil: Long = 0L,

    /** JSON object of package name to enabled flag. */
    @ColumnInfo(name = "perAppTogglesJson", defaultValue = "{}")
    val perAppTogglesJson: String = "{}"
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}
