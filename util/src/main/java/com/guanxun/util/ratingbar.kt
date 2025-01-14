package com.guanxun.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class GXRatingBar : View {
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
//        init(context, attrs)
        cfg = Config(context, attrs)
        p = PentagramBar()
        this.setOnClickListener {
            Toast.makeText(context, "X", Toast.LENGTH_SHORT).show()
        }
    }

    private var cfg: Config
    private val p: PentagramBar
    val mIconHeight: Float
        get() = cfg.mStarHeight
    val mIconWidth: Float
        get() = mIconHeight

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d(TAG, "onMeasure: ")
        var height = 0
        var width = 0
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        when (heightMode) {
            MeasureSpec.EXACTLY -> {
                height = heightSize
            }

            MeasureSpec.AT_MOST -> {
                height = (mIconHeight + paddingTop + paddingBottom).toInt()
            }

            MeasureSpec.UNSPECIFIED -> {
                height = (mIconHeight + paddingTop + paddingBottom).toInt()
            }
        }

        when (widthMode) {
            MeasureSpec.EXACTLY -> {
                width = widthSize
            }

            MeasureSpec.AT_MOST -> {
                width = (mIconWidth * 5 + paddingEnd + paddingStart).toInt()
            }

            MeasureSpec.UNSPECIFIED -> {
                width = (paddingStart + mIconWidth * 5 + paddingEnd).toInt()
            }
        }

        setMeasuredDimension(
            resolveSizeAndState(width, widthMeasureSpec, 0),
            resolveSizeAndState(height, heightMeasureSpec, 0)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

    }

    var rating: Float = 4.5f
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 1..5) {
            when {
                rating == i.toFloat() - .5f ->
                    p.drawLeftHalfPentagram(canvas, i - 1, mIconWidth)

                rating >= i.toFloat() ->
                    p.drawFullPentagram(canvas, i - 1, mIconWidth)

                else ->
                    p.drawEmptyPentagram(canvas, i - 1, mIconWidth)
            }
        }
    }

    private var mMotionState = ""
    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mMotionState = ""
                Log.d(TAG, "onTouchEvent: ACTION_DOWN ${event.x}, ${event.y}")
                // 手指按下
                // 处理滑动事件嵌套 拦截事件序列
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                Log.d(TAG, "onTouchEvent: ACTION_MOVE ${event.x}, ${event.y}")
                mMotionState = "MOVE"
                // 手指移动
                rating = p.rating(event.x)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                Log.d(TAG, "onTouchEvent: ACTION_UP '$mMotionState', ${event.x}, ${event.y}")
                if (mMotionState == "") {
                    rating = p.rating(event.x)
                    performClick()
                }
            }
            // 事件被取消
            MotionEvent.ACTION_CANCEL -> {
            }
        }
        invalidate()
        return true
    }


    inner class PentagramBar {
        private val paintOfFill: Paint = Paint().apply {
            color = cfg.mStarColor
            style = Paint.Style.FILL
        }

        private val paintOfStroke: Paint = Paint().apply {
            strokeWidth = 1f
            color = Color.GRAY
            style = Paint.Style.STROKE
        }

        private val barWidth: Float
            get() = (6 * iconSpaceRatio + 5) * mIconWidth

        private val barLeft: Float
            get() {
                return when (cfg.mStarAlign) {
                    ALIGN_RIGHT -> width - paddingEnd - barWidth
                    ALIGN_CENTER -> (paddingStart + width - paddingEnd) / 2 - barWidth / 2
                    else -> paddingStart.toFloat()
                }
            }

        private val barTop: Float
            get() = (paddingTop + height - paddingBottom).toFloat() / 2 - mIconHeight / 2

        var iconSpaceRatio: Float = .2f

        private fun iconCenter(index: Int): Point {
            return Point(
                barLeft + ((index + 1) * iconSpaceRatio + index) * mIconWidth + .5f * mIconWidth,
                barTop + mIconWidth / 2
            )
        }

        /**
         * 计算coordX对应的星数
         */
        fun rating(coordX: Float): Float {
            if (barLeft > coordX || coordX > barLeft + barWidth)
                return rating
            val effectiveX = coordX - barLeft
            //计算跨过n个完整的五角星和五角星左侧空白
            val n = floor(effectiveX / (1 + iconSpaceRatio) / mIconWidth)
            //计算跨过n个完整的五角星和这个五角星右侧空白后的余数
            val remain =
                effectiveX - n * (1 + iconSpaceRatio) * mIconWidth - iconSpaceRatio * mIconWidth

            Log.d(TAG, "rating: ${mIconWidth * .5f}, $remain, $n")
            val result = when {
                remain >= 0 && remain < mIconWidth * .5f -> n + .5f
                remain >= mIconWidth * .5f -> n + 1f
                else -> n
            }
            Log.d(TAG, "rating: ${mIconWidth * .5f}, $remain, $result")
            return result
        }

        private fun makeFullPath(
            longRadius: Float,
            shortRadius: Float,
            center: Point
        ): Path {
            val path = Path()
            var x = 0f
            var y = 0f
            path.moveTo(center.xFloat, center.yFloat - longRadius)
            for (c in -90..270 step 72) {
                var c1 = c.toFloat()
                x = longRadius * cos(c1 / 180 * Math.PI).toFloat() + center.xFloat
                y = longRadius * sin(c1 / 180 * Math.PI).toFloat() + center.yFloat
                path.lineTo(x, y)
                c1 = c + 36f
                x = shortRadius * cos(c1 / 180 * Math.PI).toFloat() + center.xFloat
                y = shortRadius * sin(c1 / 180 * Math.PI).toFloat() + center.yFloat
                path.lineTo(x, y)

            }
            path.close()

            return path
        }

        private fun makeLeftHalfPath(
            longRadius: Float,
            shortRadius: Float,
            center: Point
        ): Path {
            val path = Path()
            var x = 0f
            var y = 0f

            path.moveTo(center.xFloat, center.yFloat - longRadius)

            for (c in -90 downTo -270 step 72) {
                var c1 = c.toFloat()
                x = longRadius * cos(c1 / 180 * Math.PI).toFloat() + center.xFloat
                y = longRadius * sin(c1 / 180 * Math.PI).toFloat() + center.yFloat
                path.lineTo(x, y)
                c1 = c - 36f
                x = shortRadius * cos(c1 / 180 * Math.PI).toFloat() + center.xFloat
                y = shortRadius * sin(c1 / 180 * Math.PI).toFloat() + center.yFloat
                path.lineTo(x, y)

            }
            path.close()

            return path
        }

        fun drawFullPentagram(
            canvas: Canvas,
            n: Int,
            iconSize: Float
        ) {
            val center = iconCenter(n)
            var p = makeFullPath(
                iconSize / 2f,
                iconSize * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfFill)
            p = makeFullPath(
                iconSize / 2f,
                iconSize * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfStroke)
        }

        fun drawLeftHalfPentagram(
            canvas: Canvas,
            n: Int,
            iconSize: Float
        ) {
            val center = iconCenter(n)
            var p = makeLeftHalfPath(
                iconSize / 2f,
                iconSize * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfFill)
            p = makeFullPath(
                iconSize / 2f,
                iconSize * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfStroke)
        }

        fun drawEmptyPentagram(
            canvas: Canvas,
            n: Int,
            iconSize: Float
        ) {
            val center = iconCenter(n)
            val p = makeFullPath(
                iconSize / 2f,
                iconSize * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfStroke)
        }
    }

    class Config(context: Context, attrs: AttributeSet?) {
        var mStarHeight: Float = 0f
        var mStarAlign: Int = ALIGN_LEFT
        var mStarColor: Int = Color.YELLOW
        var mInnerPentagonDiameterFactor: Float = .55f

        init {
            initAttrsAndDefault(context, attrs)
        }

        private fun initAttrsAndDefault(context: Context, attrs: AttributeSet?) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GXRatingBar)
            mStarHeight = typedArray.getDimension(R.styleable.GXRatingBar_starHeight, 40f)
            mStarAlign = typedArray.getInt(R.styleable.GXRatingBar_starAlignment, ALIGN_LEFT)
            mStarColor = typedArray.getColor(R.styleable.GXRatingBar_starColor, Color.YELLOW)
            mInnerPentagonDiameterFactor =
                typedArray.getFloat(R.styleable.GXRatingBar_innerPentagonDiameterFactor, .55f)
            if (mInnerPentagonDiameterFactor > 1f) mInnerPentagonDiameterFactor = 1f
            typedArray.recycle()
        }

    }
}