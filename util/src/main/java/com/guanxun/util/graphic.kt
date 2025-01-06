package com.guanxun.util

import android.graphics.Paint
import java.time.LocalTime


/**
 * 获取文字中心到基线距离,结果是负值，因为baseline在center的下方
 */
fun Paint.centerToBaseline(): Float {
    return fontMetrics.centerToBaseline()
}

/**
 * 获取文字中心到基线距离,结果是负值，因为baseline在center的下方
 */
fun Paint.FontMetrics.centerToBaseline(): Float {
    return (descent - ascent) / 2 - descent
}


fun randDouble():Double{
    val x = Math.random()+LocalTime.now().nano.toDouble()/1000000000
    return if(x>=1) x-1 else x
}

fun randInt(range: IntRange):Int{
    return range.start+ (randDouble()*(range.endInclusive-range.start)).toInt()
}