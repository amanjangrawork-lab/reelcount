package com.reelcount.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per calendar day (stored as [java.time.LocalDate.toEpochDay]).
 *
 * The per-app breakdown lives in [perAppJson] as a JSON object of
 * `{"com.instagram.android": 12, ...}`. A JSON blob keeps the schema at one
 * row per day while allowing per-app tracking to grow.
 */
@Entity(
    tableName = "daily_counts",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class DailyCount(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "dateEpochDay")
    val dateEpochDay: Long,

    @ColumnInfo(name = "total")
    val total: Int,

    @ColumnInfo(name = "perAppJson")
    val perAppJson: String
)
