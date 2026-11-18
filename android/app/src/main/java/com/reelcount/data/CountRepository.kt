package com.reelcount.data

import android.content.Context
import com.reelcount.db.DailyCount
import com.reelcount.db.ReelCountDatabase
import com.reelcount.util.PerAppCounts
import com.reelcount.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read/write access to the daily counters. */
class CountRepository private constructor(context: Context) {

    private val dao = ReelCountDatabase.get(context.applicationContext).dailyCountDao()

    /** Records one reel for [pkg] and returns the row as it now stands. */
    suspend fun increment(pkg: String, epochDay: Long = TimeUtils.todayEpochDay()): DailyCount =
        withContext(Dispatchers.IO) { dao.incrementTotal(epochDay, pkg) }

    suspend fun todayTotal(): Int = withContext(Dispatchers.IO) {
        dao.getByDay(TimeUtils.todayEpochDay())?.total ?: 0
    }

    suspend fun perAppCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        val row = dao.getByDay(TimeUtils.todayEpochDay())
        PerAppCounts.parse(row?.perAppJson)
    }

    suspend fun lastDays(days: Int): List<DailyCount> = withContext(Dispatchers.IO) {
        val end = TimeUtils.todayEpochDay()
        dao.getRange(end - (days - 1).coerceAtLeast(0), end)
    }

    companion object {
        @Volatile
        private var instance: CountRepository? = null

        fun get(context: Context): CountRepository {
            return instance ?: synchronized(this) {
                instance ?: CountRepository(context).also { instance = it }
            }
        }
    }
}
