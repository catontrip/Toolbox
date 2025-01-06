package com.guanxun.util

import android.util.Log
import kotlin.math.abs


/**
 * 转轮在数轴上的转动模型。记录转轮状态，计算相应的各条目的坐标以及哪个条目当前处于视窗中央
 *
 *       xx-----=====-----xx
 *    Item8Item9Item0Item1Item2Item3Item4Item5Item6Item7
 *
 * xx-----=====-----xx 总长度为maxLength
 *
 * -----=====----- 为可视窗口Aperture，其长度为ApertureLength由系统自动计算不为大于maxLength，且为正好奇数个Item长度的总和。
 * 此系统坐标系原点为Aperture的起点。 其相对于以maxLength起点为原点的坐标系偏移量为coordSysOffset
 *
 * xx 为不可见部分
 *
 * ===== 为Focus
 *
 */
open class WheelLinearModel {
    /**
     * 初始可视窗口长度，优化后的实际长度是actualVisibleWindowLength
     */
    var apertureMaxLength: Int = 0
        set(value) {
            field = value
            reset()
        }

    /**
     * 每个条目的长度
     */
    var itemSize: Int = 0
        set(value) {
            field = value
            reset()
        }

    /**
     * 条目总数
     */
    var itemCount: Int = 0
        set(value) {
            field = value
            reset()
        }

    /**
     * 重置计算属性，当itemLen，itemCount，apertureMaxLength之一发生变化时，其他属性都需要重新计算
     */
    private fun reset() {
        if (itemSize == 0) return
//    val focusIndex = indexOfItemAtFocus()
        visibleItemCount = apertureMaxLength / itemSize
        if (visibleItemCount % 2 == 0) {
            visibleItemCount -= 1
        }
        apertureLength = visibleItemCount * itemSize
        focusCenter = apertureLength / 2f
        focusStart = (focusCenter - itemSize.toFloat() / 2).toInt()
        focusEnd = (focusCenter + itemSize.toFloat() / 2).toInt()
        offSet = focusStart
    }

    /**
     * 可视窗口可以显示几个完整条目
     */
    var visibleItemCount: Int = 0
        private set

    /**
     * 实际窗口尺寸应该正好显示奇数个条目，且不大于originVisibleWindowLength
     * actualVisibleWindowLength为自适应的满足上述条件的最大尺寸
     */
    var apertureLength: Int = 0
        private set

    /**
     * 是否允许循环显示。 itemCount少于visibleItemCount时必然需要加入占位空白条目，或者重复显示一些条目。
     * 所以现在不允许这种状况下循环显示
     */
    private val allowCycleDisplay: Boolean
        get() = visibleItemCount <= itemCount

    /**
     * 相对于以maxLength起点为原点的坐标系偏移量
     */
    val coordSysOffset: Int
        get() = (apertureMaxLength - apertureLength) / 2

    /**
     * 可视窗口中心线坐标。originVisibleWindowLength和actualVisibleWindowLength的中心线应该是重合的
     */
    var focusCenter: Float = 0f
        private set

    /**
     * 可视窗口起点坐标
     */
    var focusStart: Int = 0
        private set

    /**
     * 可视窗口终点坐标
     */
    var focusEnd: Int = 0
        private set

    /**
     * 所有条目总长度
     */
    private val totalItemLength: Int
        get() = itemSize * itemCount

    /**
     *  旋转方向。“+”正方向，“-”负方向， ""无方向
     */
    private var turnDirection = ""

    /**
     * 累积旋转偏移（圆周长度）
     */
    var offSet: Int = 0
        set(value) {
            if (value == field)
                return
            //记录当前焦点item
            val originFocusIndex = indexOfItemAtFocus()
            //计算方向
            val x = value - field
            turnDirection = when {
                x > 0 -> "+"
                x < 0 -> "-"
                else -> ""
            }
            //处理不可循环的滚动
            field = if (value <= minOffset) {
                minOffset
            } else if (value >= maxOffset) {
                maxOffset
            } else {
                value
            }
            val newFocusIndex = indexOfItemAtFocus()
            //顺序触发onFocusItemChangedListener
            if (originFocusIndex == newFocusIndex) return
            //originFocusIndex已经在上一次触发过了，在此一定要跳过
            if (turnDirection == "+") {
                if (newFocusIndex < originFocusIndex) {
                    for (i in originFocusIndex + 1..(newFocusIndex + itemCount))
                        onFocusItemChangedListener(i % itemCount)
                } else {
                    for (i in originFocusIndex + 1..newFocusIndex)
                        onFocusItemChangedListener(i)
                }
            }
            if (turnDirection == "-") {
                if (newFocusIndex < originFocusIndex) {
                    for (i in originFocusIndex - 1 downTo newFocusIndex + itemCount)
                        onFocusItemChangedListener(i % itemCount)
                } else {
                    for (i in originFocusIndex + 1..newFocusIndex)
                        onFocusItemChangedListener(i % itemCount)
                }
            }

        }

    /**
     * 当不能循环滚动显示时，offset的最小边界
     */
    val minOffset: Int
        get() {
            return if (allowCycleDisplay) {
                Int.MIN_VALUE
            } else {
                focusStart - (itemCount - 1) * itemSize
            }
        }

    /**
     * 当不能循环滚动显示时，offset的最大边界
     */
    val maxOffset: Int
        get() {
            return if (allowCycleDisplay) {
                Int.MAX_VALUE
            } else {
                focusStart
            }
        }

    /**
     * 转动时，当焦点item发生变化时触发
     */
    var onFocusItemChangedListener: (Int) -> Unit = { _ -> }

    /**
     * 累积偏移后下标为i的条目的起点坐标
     * @param i  条目下标
     * @return 条目i的起点下标
     */
    fun coordOfIndex(i: Int): Int {
        return coord(i, offSet, itemSize)
    }

    /**
     * 计算条目起始坐标
     * @param index 条目下标
     * @param offset 旋转偏移（圆周长度）
     * @param itemLength 条目的长度
     */
    private fun coord(index: Int, offset: Int, itemLength: Int): Int {
        if (totalItemLength == 0) {
            return 0
        }
        var result = offset + index * itemLength
        if (totalItemLength >= apertureLength) {
            result %= totalItemLength
            if (result > totalItemLength) {
                result -= totalItemLength
            } else if (result < 0) {
                result += totalItemLength
            }
        }
        return result
    }

    /**
     * 允许滚动显示时，需要在头部渐进渐出的那一个item的下标和坐标
     * @return 在头部渐进渐出的那一个item的下标和坐标
     */
    fun itemNeedSplitDisplay(): ItemParam? {
        if (!allowCycleDisplay) return null
        val item0StartToEndDistance = totalItemLength - offSet % totalItemLength
        val lastItemDisplaySpace = item0StartToEndDistance % itemSize
        if (lastItemDisplaySpace == 0)
            return null
        val indexOfItemNeedSplitDisplay = (item0StartToEndDistance / itemSize + itemCount) % itemCount
        val itemNeedSplitDisplay = ItemParam(
            index = indexOfItemNeedSplitDisplay,
            startCoord = -lastItemDisplaySpace
        )
        return itemNeedSplitDisplay
    }

    /**
     * @return 最为居中的item的下标
     */
    fun indexOfItemAtFocus(): Int {
        val item0X = coordOfIndex(0)
        val distance: Int = (item0X.toFloat() - focusCenter).toInt()
        if(itemSize==0)return 0
        val gap: Int = distance / itemSize
        if (itemCount == 0) return 0
        return if (distance < 0)
        //item[0]在可视窗口中心item前面，gap此时为负
            (itemCount - gap) % itemCount
        else
        //item[0]在可视窗口中心item后面，gap此时为正
            itemCount - gap - 1
    }

    /**
     * 沿圆周旋转
     * @param delta 圆周旋转距离
     */
    fun turn(delta: Int = 0) {
        if (delta == 0)
            return
        offSet += delta
    }

    /**
     * 把下标为index的条目旋转到视窗中央
     * @param index 条目下标
     */
    fun turnNToCenter(index: Int) {
        Log.d(TAG, "turnNToCenter: $index")
        Thread.currentThread().stackTrace.forEach {
            Log.d(TAG,"${it.className}.${it.methodName}")
        }
        turn(distanceOfNToCenter(index))
    }

    /**
     * 把下标为index的条目旋转到视窗中央
     * @param index 条目下标
     */
    fun distanceOfNToCenter(index: Int): Int {
        val coord = coordOfIndex(index)
        val distanceA = focusStart - coord
        val distanceB = if (distanceA < 0) {
            //coord>=focusStart
            focusStart + totalItemLength - coord
        } else {
            //coord<focusStart
            focusStart - totalItemLength - coord
        }

        Log.d(TAG, "distanceOfNToCenter: $distanceA")
        if (offSet + distanceA < minOffset|| offSet + distanceA > maxOffset)
            return distanceB
        if (offSet + distanceB < minOffset|| offSet + distanceB > maxOffset)
            return distanceA
        return if (abs(distanceB) > abs(distanceA)) distanceA
        else distanceB
    }

    /**
     * 判断pointCoord在哪一个item的范围内
     * @return pointCoord所属item的下标
     */
    fun pointOfIndex(pointCoord: Int): ItemParam? {
        for (i in 0..<itemCount) {
            val start = coordOfIndex(i)
            if (start <= pointCoord && pointCoord < start + itemSize)
                return ItemParam(index = i)
        }
        return null
    }

    /**
     * 描述一个item的下标和当前坐标
     */
    class ItemParam(val index: Int, val startCoord: Int = 0)
}