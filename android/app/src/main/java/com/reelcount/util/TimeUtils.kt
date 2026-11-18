package com.reelcount.util

import java.time.LocalDate
import java.time.ZoneId

/** Calendar helper shared by the counter, the charts and the notification. */
object TimeUtils {

    /** Today as an epoch day in the device's current time zone. */
    fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
}
