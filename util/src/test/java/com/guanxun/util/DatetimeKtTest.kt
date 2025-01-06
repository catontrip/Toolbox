package com.guanxun.util

//import org.junit.Assert.*

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

class DatetimeKtTest {

    @Test
    fun makeMinute() {
        var result = com.guanxun.util.makeMinute(12, 0, ClockFormat.PM)
        assertThat(result).isEqualTo(720)
        result = com.guanxun.util.makeMinute(13, 0, ClockFormat.HOUR_24)
        assertThat(result).isEqualTo(780)
        result = com.guanxun.util.makeMinute(12, 0, ClockFormat.AM)
        assertThat(result).isEqualTo(0)
        result = com.guanxun.util.makeMinute(1, 30, ClockFormat.AM)
        assertThat(result).isEqualTo(90)
    }

    @Test
    fun `random test`() {
        for (i in 0..10000) {
            val h = randInt(1..12)
            val m = randInt(0..59)
            val t = LocalTime.of(h, m)
            val resultA = t.minuteOfDay()
            assertThat(resultA).isEqualTo(h * 60 + m)
            val resultB = makeMinute(h, m, ClockFormat.PM)
            val h1 = if (h != 12) h + 12 else h
            assertThat(resultB).isEqualTo(LocalTime.of(h1, m).minuteOfDay())
            val resultC = makeMinute(h, m, ClockFormat.AM)
            val h2 = if (h == 12) 0 else h
            assertThat(resultC).isEqualTo(LocalTime.of(h2, m).minuteOfDay())
        }
    }
}