package com.guanxun.util

import android.graphics.Paint
import java.time.LocalTime
import kotlin.math.pow
import kotlin.math.sqrt


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

fun randDouble(): Double {
    val x = Math.random() + LocalTime.now().nano.toDouble() / 1000000000
    return if (x >= 1) x - 1 else x
}

fun randInt(range: IntRange): Int {
    return range.first + (Math.random() * (range.last - range.first)).toInt()
}

data class Point(val x: Double, val y: Double) {
    constructor(p:Point) : this(p.x,p.y)
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    val xFloat: Float
        get() = x.toFloat()
    val yFloat: Float
        get() = y.toFloat()

    fun distanceTo(other: Point): Double {
        // 计算到另一个点的距离
        return sqrt((x - other.x).pow(2.0) + (y - other.y).pow(2.0))
    }
}

const val ALIGN_LEFT = 0
const val ALIGN_CENTER = 1
const val ALIGN_RIGHT = 2