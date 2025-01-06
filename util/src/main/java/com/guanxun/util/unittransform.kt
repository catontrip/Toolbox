package com.guanxun.util
import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_SP


const val TAG = "<=^=>"

/**
 * dp转px
 *
 * @param dpValue density-independent pixel
 * @return 转换后的px值
 */
fun dp2px(dpValue: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dpValue, Resources.getSystem().displayMetrics
    )
}

/**
 * sp转换px
 *
 * @param spValue scaled pixels
 * sp值
 * @return 转换后的px值
 */
fun sp2px(spValue: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, spValue, Resources.getSystem().displayMetrics
    )
}

/**
 * px转换sp
 *
 * @param pxValue pixels
 * sp值
 * @return 转换后的scaled pixels值
 */
fun px2Sp(context: Context, pxValue: Float) :Float{
    // 获取当前屏幕的缩放密度
    val scaledDensity = context.resources.displayMetrics.scaledDensity
    // 将 px 转换为 sp
    return pxValue / scaledDensity
}
