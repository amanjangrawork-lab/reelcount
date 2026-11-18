package com.reelcount.util

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun `today is the local calendar day, not the UTC one`() {
        val expected = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        assertEquals(expected, TimeUtils.todayEpochDay())
    }

    @Test
    fun `epoch day values are stable within a call`() {
        assertEquals(TimeUtils.todayEpochDay(), TimeUtils.todayEpochDay())
    }
}
