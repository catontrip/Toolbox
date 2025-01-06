package com.guanxun.util

import java.time.LocalTime

enum class ClockFormat {
    AM,
    PM,
    HOUR_24
}

fun makeMinute(hour: Int, minute: Int, format: ClockFormat = ClockFormat.HOUR_24): Int {
    return when (format) {
        ClockFormat.AM -> (hour % 12) * 60 + minute
        ClockFormat.PM -> (hour % 12 + 12) * 60 + minute
        else -> hour * 60 + minute
    }
}

fun makeMinute(t: LocalTime): Int {
    return makeMinute(t.hour, t.minute)
}

fun LocalTime.minuteOfDay():Int{
    return makeMinute(this)
}