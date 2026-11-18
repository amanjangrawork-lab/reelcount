package com.reelcount.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.reelcount.util.PerAppCounts

@Dao
interface DailyCountDao {

    @Upsert
    suspend fun upsert(count: DailyCount)

    @Query("SELECT * FROM daily_counts WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getByDay(dateEpochDay: Long): DailyCount?

    @Query(
        "SELECT * FROM daily_counts WHERE dateEpochDay BETWEEN :startDay AND :endDay " +
            "ORDER BY dateEpochDay ASC"
    )
    suspend fun getRange(startDay: Long, endDay: Long): List<DailyCount>

    /**
     * Adds one to the day total and to the app's counter in a single
     * transaction, returning the row as it now stands.
     */
    @Transaction
    suspend fun incrementTotal(dateEpochDay: Long, pkg: String): DailyCount {
        val existing = getByDay(dateEpochDay)
        val perApp = existing?.let { PerAppCounts.parse(it.perAppJson) } ?: mutableMapOf()
        perApp[pkg] = (perApp[pkg] ?: 0) + 1

        val updated = DailyCount(
            id = existing?.id ?: 0L,
            dateEpochDay = dateEpochDay,
            total = (existing?.total ?: 0) + 1,
            perAppJson = PerAppCounts.serialize(perApp)
        )
        upsert(updated)

        if (updated.id != 0L) {
            return updated
        }
        return updated.copy(id = getByDay(dateEpochDay)?.id ?: 0L)
    }
}
