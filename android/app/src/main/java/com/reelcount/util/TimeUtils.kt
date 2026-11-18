package com.reelcount.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Calendar helpers shared by the counter, the charts and the notification. */
object TimeUtils {

    /** Today as an epoch day in the device's current time zone. */
    fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    fun epochDayDaysAgo(days: Int): Long = LocalDate.now(ZoneId.systemDefault()).minusDays(days.toLong()).toEpochDay()

    /** "09:41" style label for the notification and the overlay. */
    fun formatClock(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return String.format(Locale.US, "%02d:%02d", time.hour, time.minute)
    }

    /** Human text for the remaining all-day lock, e.g. "3 h 12 m". */
    fun describeRemaining(untilEpochMillis: Long, nowEpochMillis: Long = System.currentTimeMillis()): String {
        val remaining = untilEpochMillis - nowEpochMillis
        if (remaining <= 0L) {
            return "0 m"
        }
        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
        return if (hours > 0) "$hours h $minutes m" else "$minutes m"
    }
}
