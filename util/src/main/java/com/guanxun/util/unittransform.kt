package com.guanxun.util
import android.util.TypedValue
import android.content.res.Resources

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