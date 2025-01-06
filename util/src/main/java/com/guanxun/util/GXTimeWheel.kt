package com.guanxun.util

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalTime
import java.util.Locale

class GXTimeWheel : LinearLayout {
    lateinit var amPmWheel: GXWheel
    private lateinit var hourWheel: GXWheel
    private lateinit var minuteWheel: GXWheel
    private lateinit var tvSplitText: TextView
    private var attrs: AttributeSet? = null

    constructor(
        context: Context
    ) : this(context, null)

    constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : this(context, attrs, defStyleAttr, 0)

    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        this.attrs = attrs
        init(context, attrs)

    }

    /**
     * 配置信息
     */
    private lateinit var cfg: Config

    /**
     * 小时和分钟之间的字符,缺省为":"
     */
    var splitText = ":"
        set(value) {
            field = value
            tvSplitText.text = value
        }
        get() = tvSplitText.text.toString()

    /**
     * 分钟列表
     */
    private var minuteList = (0..59).toList()
        set(value) {
            field = value
            minuteWheel.data = value
        }

    /**
     * 小时示数
     */
    private val hourValue: Int
        get() = hourList[hourWheel.selectedIndex]

    /**
     * 分钟示数
     */
    private val minuteValue: Int
        get() = minuteList[minuteWheel.selectedIndex]

    /**
     * 小时列表
     */
    private var hourList = (0..23).toList()
        set(value) {
            field = if (cfg.mClockFormat24Hour) {
                value.filter {
                    it in minTimeByMinute / 60..maxTimeByMinute / 60
                }
            } else {
                val l = (0..<24)
                    .toList()
                    .filter { it in minTimeByMinute / 60..maxTimeByMinute / 60 }
                if (periodOf12HourClock == ClockFormat.AM) {
                    l.filter { it < 12 }.map { if (it == 0) 12 else it }
                } else {
                    l.filter { it >= 12 }.map { if (it == 12) 12 else it - 12 }
                }
            }
            hourWheel.data = field
        }

    /**
     * 用分钟表示的当前选择的时刻
     */
    private val currentValByMinute: Int
        get() {
            var minute = 0
            minute = if (amPmWheel.visibility == VISIBLE) {
                makeMinute(
                    hourValue,
                    minuteValue,
                    periodOf12HourClock
                )
            } else {
                makeMinute(hourValue, minuteValue)
            }
            return minute
        }

    /**
     * 当前选择的时刻
     */
    var currentTime: LocalTime = LocalTime.now()
        set(value) {
            require(makeMinute(value) in minTimeByMinute..maxTimeByMinute) {
                "currentTime is out of range between ${
                    LocalTime.of(
                        minTimeByMinute / 60,
                        minTimeByMinute % 60
                    )
                } and ${LocalTime.of(maxTimeByMinute / 60, maxTimeByMinute % 60)}"
            }
            field = value
            setWheel(value.hour * 60 + value.minute)
        }
        get() = LocalTime.of(0, 0).plusMinutes(currentValByMinute.toLong())

    /**
     * 记录当前clock表盘是24H,AM还是PM
     */
    private var periodOf12HourClock: ClockFormat = ClockFormat.AM

    /**
     * 允许时间最大值
     */
    private var maxTimeByMinute: Int = 24 * 60 - 1

    /**
     * 允许时间最小值
     */
    private var minTimeByMinute: Int = 0

    /**
     * True使用AM/PM 12小时制, false使用24小时制
     */
    var use12Hour: Boolean
        set(v) {
            cfg.mClockFormat24Hour = !v
            initWheel()
        }
        get() = !cfg.mClockFormat24Hour

    /**
     * 时间选择变化后的回调, time是选择的时间
     */
    var onSelectionChangedListener: (time: LocalTime) -> Unit = { _ -> }

    private fun init(context: Context, attrs: AttributeSet?) {
        val viewRoot = LayoutInflater.from(context).inflate(R.layout.view_time_wheel, this)
        attrs?.let {
            cfg = Config(context, it)
        }
        tvSplitText = viewRoot.findViewById(R.id.splitText)
        tvSplitText.textSize = px2Sp(context, cfg.mTextSize)
        tvSplitText.text = splitText
        amPmWheel = viewRoot.findViewById(R.id.amPm)
        hourWheel = viewRoot.findViewById(R.id.hourWheel)
        hourWheel.cfg = cfg
        minuteWheel = viewRoot.findViewById(R.id.minuteWheel)
        minuteWheel.cfg = cfg
        initWheel()
    }

    private fun initWheel() {
        tvSplitText.textSize = px2Sp(context, cfg.mTextSize)
        tvSplitText.text = splitText
        minuteWheel.customizedFormat = {
            String.format(Locale.getDefault(), "%02d", it)
        }
        amPmWheel.onSelectionChangedListener = { _, _ -> }
        hourWheel.onSelectionChangedListener = { _, _ -> }
        minuteWheel.onSelectionChangedListener = { _, _ -> }
        minuteWheel.data = minuteList
        if (cfg.mClockFormat24Hour) {
            amPmWheel.visibility = GONE
            periodOf12HourClock = ClockFormat.HOUR_24
        } else {
            amPmWheel.visibility = VISIBLE
        }

        setValidTimeRange(maxTimeByMinute, minTimeByMinute)
        amPmWheel.onSelectionChangedListener = { _, _ ->
            if (amPmWheel.data[amPmWheel.selectedIndex] != periodOf12HourClock.name) {
                val value = hourValue
                periodOf12HourClock =
                    ClockFormat.valueOf(amPmWheel.data[amPmWheel.selectedIndex].toString())
                hourList = (0..23).toList()
                val index = hourList.indexOf(value)
                if (index == -1) {
                    hourWheel.selectedIndex = 0
                } else {
                    hourWheel.selectedIndex = index
                }
                validateWheel()
            }
        }

        hourWheel.onSelectionChangedListener = { hourList, index ->
            validateWheel()
        }

        minuteWheel.onSelectionChangedListener = { minuteList, index ->
            validateWheel()
        }
    }

    /**
     * 仅供processWheel()中使用
     */
    private var pauseFlag = false


    private fun validateWheel() {
        //当前时间可能不合法,需要分别调整滚轮,这是重复调用validateWheel() 可能死循环.
        //这种情况需要设置和检查pauseFlag
        if (pauseFlag) return
        val minute = currentValByMinute
        if (minute in minTimeByMinute..maxTimeByMinute) {
            this.onSelectionChangedListener(currentTime)
        } else {
            pauseFlag = true
            if (minute > maxTimeByMinute) {
                setWheel(maxTimeByMinute)
            } else if (minute < minTimeByMinute) {
                setWheel(minTimeByMinute)
            }
            pauseFlag = false
        }
    }

    fun setValidTimeRange(maxHour: Int, maxMinute: Int, minHour: Int, minMinute: Int) {
        if (maxHour !in 0..23) throw IllegalArgumentException("Invalid maxHour $maxHour")
        if (minHour !in 0..23) throw IllegalArgumentException("Invalid minHour $minHour")
        if (maxMinute !in 0..59) throw IllegalArgumentException("Invalid maxMinute $maxMinute")
        if (minMinute !in 0..59) throw IllegalArgumentException("Invalid minMinute $minMinute")
        val maxT = makeMinute(maxHour, maxMinute)
        val minT = makeMinute(minHour, minMinute)
        if (maxT <= minT) throw IllegalArgumentException(
            "Max time $maxHour:$maxMinute is not greater than min time $minHour:$minMinute"
        )
        setValidTimeRange(maxT, minT)
    }

    private fun setValidTimeRange(maxByMinute: Int, minByMinute: Int) {
        if (maxByMinute <= minByMinute) throw IllegalArgumentException(
            "Max time $maxByMinute is not greater than min time $minByMinute"
        )
        minTimeByMinute = minByMinute
        maxTimeByMinute = maxByMinute
        if (minTimeByMinute >= 12 * 60) {
            amPmWheel.data = listOf("PM")
        } else if (maxTimeByMinute < 12 * 60) {
            amPmWheel.data = listOf("AM")
        } else {
            amPmWheel.data = listOf("AM", "PM")
        }
        hourList = (0..23).toList()
    }

    private fun setWheel(minute: Int) {
        var h = minute / 60
        val m = minute % 60
        if (amPmWheel.visibility == VISIBLE) {
            if (h > 12) {
                amPmWheel.selectedIndex = 1
                h -= 12
            } else {
                amPmWheel.selectedIndex = 0
                if (h == 0) h = 12
            }
        }
        hourWheel.selectedIndex = hourList.indexOf(h)
        minuteWheel.selectedIndex = minuteList.indexOf(m)
    }

    class Config(context: Context, attrs: AttributeSet?) : GXWheel.Config(context, attrs) {

        var mClockFormat24Hour = true

        init {
            Log.d(TAG, "CHILD: ")
            initAttrsAndDefault(context, attrs)
        }

        /**
         * 初始化自定义属性及默认值
         *
         * @param context
         * 上下文
         * @param attrs
         * attrs
         */
        private fun initAttrsAndDefault(context: Context, attrs: AttributeSet?) {
            mOrientation = Orientation.Vertical
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GXTimeWheel)
            val clockFormat = typedArray.getInt(R.styleable.GXTimeWheel_clockFormat_tp, 0)
            mClockFormat24Hour = clockFormat == 0
            mTextSize =
                typedArray.getDimension(R.styleable.GXTimeWheel_textSize_tp, _defaultTextSize)

            mTextLeftAndRightBoundaryMargin = typedArray.getDimension(
                R.styleable.GXTimeWheel_marginBetweenItem_tp,
                DEFAULT_TEXT_BOUNDARY_MARGIN
            )
            mTextColor =
                typedArray.getColor(
                    R.styleable.GXTimeWheel_focusTextColor_tp,
                    DEFAULT_NORMAL_TEXT_COLOR
                )
            mFocusTextColor = typedArray.getColor(
                R.styleable.GXTimeWheel_focusTextColor_tp,
                DEFAULT_SELECTED_TEXT_COLOR
            )
            showFocusFrame = typedArray.getBoolean(R.styleable.GXTimeWheel_showFocusFrame_tp, false)
            mFocusFrameWidth =
                typedArray.getDimension(
                    R.styleable.GXTimeWheel_focusFrameWidth_tp,
                    DEFAULT_DIVIDER_HEIGHT
                )

            mFocusFrameColor =
                typedArray.getColor(
                    R.styleable.GXTimeWheel_focusFrameColor_tp,
                    DEFAULT_SELECTED_TEXT_COLOR
                )
            mDrawFocusRect =
                typedArray.getBoolean(R.styleable.GXTimeWheel_drawFocusRect_tp, false)
            mFocusRectColor =
                typedArray.getColor(R.styleable.GXTimeWheel_focusRectColor_tp, Color.TRANSPARENT)

            mFocusHeightFactor = typedArray.getFloat(
                R.styleable.GXTimeWheel_focusHeightFactor_tp, 1f
            )

            mShowFocusCenterLineIndicator = typedArray.getBoolean(
                R.styleable.GXTimeWheel_showFocusCenterLineIndicator_tp, true
            )

            mFocusCenterLineIndicatorColor = typedArray.getColor(
                R.styleable.GXTimeWheel_focusCenterLineIndicatorColor_tp, Color.RED
            )

            mFocusCenterLineIndicatorWidth = typedArray.getDimension(
                R.styleable.GXTimeWheel_focusCenterLineIndicatorLineWidth_tp, dp2px(2f)
            )

            mFocusCenterLineIndicatorHeightFactor = typedArray.getFloat(
                R.styleable.GXTimeWheel_focusCenterLineIndicatorFactor_tp, 1f
            )

            mShowFocusSpotIndicator = typedArray.getBoolean(
                R.styleable.GXTimeWheel_showFocusSpotIndicator_tp, true
            )

            mFocusSpotIndicatorColor = typedArray.getColor(
                R.styleable.GXTimeWheel_focusSpotIndicatorColor_tp, Color.RED
            )

            mFocusSpotIndicatorRadius = typedArray.getDimension(
                R.styleable.GXTimeWheel_focusSpotIndicatorRadius_tp, dp2px(2f)
            )

            mFocusFrameSizeFactor = typedArray.getFloat(
                R.styleable.GXTimeWheel_focusFrameSizeFactor_tp, 1f
            )
            typedArray.recycle()
        }
    }
}