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
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * GXRatingBar 用5个五角星来打分, rating值为0星,0.5星一直到5星.
 * 可以用手指滑动或者点击来改变分值.
 */
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
        cfg = Config(context, attrs)
        p = PentagramBar()
    }

    //配置参数
    private var cfg: Config

    //星条模型
    private val p: PentagramBar

    /**
     * 五角星为mStarHeight为直径的内接五角星
     */
    private val mStarHeight: Float
        get() = cfg.mStarHeight

    //宽度为五角星外接圆的宽度,即直径
    private val mStarWidth: Float
        get() = mStarHeight

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
                height = (mStarHeight + paddingTop + paddingBottom).toInt()
            }

            MeasureSpec.UNSPECIFIED -> {
                height = (mStarHeight + paddingTop + paddingBottom).toInt()
            }
        }

        when (widthMode) {
            MeasureSpec.EXACTLY -> {
                width = widthSize
            }

            MeasureSpec.AT_MOST -> {
                width = (mStarWidth * 5 + paddingEnd + paddingStart).toInt()
            }

            MeasureSpec.UNSPECIFIED -> {
                width = (paddingStart + mStarWidth * 5 + paddingEnd).toInt()
            }
        }

        setMeasuredDimension(
            resolveSizeAndState(width, widthMeasureSpec, 0),
            resolveSizeAndState(height, heightMeasureSpec, 0)
        )
    }

    /**
     * rating发生变化时的回调
     */
    var onRatingChangedListener: (ratingValue: Float) -> Unit = { _ ->}

    /**
     * 分值,可以通过rating来控制点亮星数. 也可以通过rating来获得点亮的星数
     */
    var rating: Float = 4.5f
        set(value) {
            if (value<0 ||value>5f) return
            if (value !=field) onRatingChangedListener(value)
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 1..5) {
            when {
                rating == i.toFloat() - .5f ->
                    p.drawLeftHalfPentagram(canvas, i - 1, mStarWidth)

                rating >= i.toFloat() ->
                    p.drawFullPentagram(canvas, i - 1, mStarWidth)

                else ->
                    p.drawEmptyPentagram(canvas, i - 1, mStarWidth)
            }
        }
    }

    //手势状态,用来区分是否为"MOVE"
    private var mMotionState = ""
    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mMotionState = ""
                // 手指按下
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                mMotionState = "MOVE"
                // 手指移动
                rating = p.rating(event.x)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                //不是"MOVE",那么久更新rating,然后触发click事件
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

    /**
     * 星条模型,处理图像生成和相关坐标映射
     *
     * .....-x-x-x-x-x-.....
     *
     * . :padding
     *
     * - :space between pentagram
     *
     * x :pentagram
     */
    inner class PentagramBar {
        //实心五星画笔
        private val paintOfFill: Paint = Paint().apply {
            color = cfg.mStarColor
            style = Paint.Style.FILL
        }

        //空心五星画笔
        private val paintOfStroke: Paint = Paint().apply {
            strokeWidth = 1f
            color = Color.GRAY
            style = Paint.Style.STROKE
        }

        //星条宽度,不包括padding
        private val barWidth: Float
            get() = (6 * starSpaceRatio + 5) * mStarWidth

        //星条左数第一个space的左边坐标
        private val barLeft: Float
            get() {
                return when (cfg.mStarAlign) {
                    ALIGN_RIGHT -> width - paddingEnd - barWidth
                    ALIGN_CENTER -> (paddingStart + width - paddingEnd) / 2 - barWidth / 2
                    else -> paddingStart.toFloat()
                }
            }

        //星条顶边坐标
        private val barTop: Float
            get() = (paddingTop + height - paddingBottom).toFloat() / 2 - mStarHeight / 2

        //space between pentagram/
        private var starSpaceRatio: Float = .2f

        //第index个五角星的中心坐标, index从0开始
        private fun iconCenter(index: Int): Point {
            return Point(
                barLeft + ((index + 1) * starSpaceRatio + index) * mStarWidth + .5f * mStarWidth,
                barTop + mStarWidth / 2
            )
        }

        /**
         * 计算coordX对应的星数
         */
        fun rating(coordX: Float): Float {
            //忽略点击星条左右空白区域
            if (barLeft > coordX || coordX > barLeft + barWidth)
                return rating
            //coordX相对于星条左边的距离
            val effectiveX = coordX - barLeft
            //计算跨过n个完整的五角星和五角星左侧空白
            val n = floor(effectiveX / (1 + starSpaceRatio) / mStarWidth)
            //计算跨过n个完整的五角星和这个五角星右侧空白后的余数
            val remain =
                effectiveX - n * (1 + starSpaceRatio) * mStarWidth - starSpaceRatio * mStarWidth

            val result = when {
                remain >= 0 && remain < mStarWidth * .5f -> n + .5f
                remain >= mStarWidth * .5f -> n + 1f
                else -> n
            }
            return result
        }

        /**
         * 生成一个完整五角星的外轮廓
         * @param outerRadius 五角星外接圆半径
         * @param innerRadius 五角星内部五边形外接圆半径
         * @param center 五角星中心坐标
         */
        private fun makeFullPath(
            outerRadius: Float,
            innerRadius: Float,
            center: Point
        ): Path {
            val path = Path()
            var x = 0f
            var y = 0f
            path.moveTo(center.xFloat, center.yFloat - outerRadius)
            for (c in -90..270 step 72) {
                var c1 = c.toFloat()
                x = outerRadius * cos(c1 / 180 * Math.PI).toFloat() + center.xFloat
                y = outerRadius * sin(c1 / 180 * Math.PI).toFloat() + center.yFloat
                path.lineTo(x, y)
                c1 = c + 36f
                x = innerRadius * cos(c1 / 180 * Math.PI).toFloat() + center.xFloat
                y = innerRadius * sin(c1 / 180 * Math.PI).toFloat() + center.yFloat
                path.lineTo(x, y)

            }
            path.close()

            return path
        }

        /**
         * 生成一个五角星坐标一部的外轮廓
         * @param outerRadius 五角星外接圆半径
         * @param innerRadius 五角星内部五边形外接圆半径
         * @param center 五角星中心坐标
         */
        private fun makeLeftHalfPath(
            outerRadius: Float,
            innerRadius: Float,
            center: Point
        ): Path {
            val path = Path()
            var x = 0f
            var y = 0f

            path.moveTo(center.xFloat, center.yFloat - outerRadius)

            for (c in -90 downTo -270 step 72) {
                var c1 = c.toFloat()
                x = outerRadius * cos(c1 / 180 * Math.PI).toFloat() + center.xFloat
                y = outerRadius * sin(c1 / 180 * Math.PI).toFloat() + center.yFloat
                path.lineTo(x, y)
                c1 = c - 36f
                x = innerRadius * cos(c1 / 180 * Math.PI).toFloat() + center.xFloat
                y = innerRadius * sin(c1 / 180 * Math.PI).toFloat() + center.yFloat
                path.lineTo(x, y)

            }
            path.close()

            return path
        }

        /**
         * 画出第n个实心五角星
         * @param canvas 画布
         * @param n n从零计数
         * @param starDiameter 外接圆直径
         */
        fun drawFullPentagram(
            canvas: Canvas,
            n: Int,
            starDiameter: Float
        ) {
            val center = iconCenter(n)
            var p = makeFullPath(
                starDiameter / 2f,
                starDiameter * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfFill)
            p = makeFullPath(
                starDiameter / 2f,
                starDiameter * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfStroke)
        }

        /**
         * 画出第n个半实心五角星
         * @param canvas 画布
         * @param n n从零计数
         * @param starDiameter 外接圆直径
         */
        fun drawLeftHalfPentagram(
            canvas: Canvas,
            n: Int,
            starDiameter: Float
        ) {
            val center = iconCenter(n)
            var p = makeLeftHalfPath(
                starDiameter / 2f,
                starDiameter * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfFill)
            p = makeFullPath(
                starDiameter / 2f,
                starDiameter * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfStroke)
        }

        /**
         * 画出第n个五角星外边框
         * @param canvas 画布
         * @param n n从零计数
         * @param starDiameter 外接圆直径
         */
        fun drawEmptyPentagram(
            canvas: Canvas,
            n: Int,
            starDiameter: Float
        ) {
            val center = iconCenter(n)
            val p = makeFullPath(
                starDiameter / 2f,
                starDiameter * cfg.mInnerPentagonDiameterFactor / 2f, center
            )
            canvas.drawPath(p, paintOfStroke)
        }
    }

    /**
     * 配置信息
     */
    class Config(context: Context, attrs: AttributeSet?) {
        /**
         * 五角星外接圆直径
         */
        var mStarHeight: Float = 0f

        /**
         * 五角星对齐方式
         */
        var mStarAlign: Int = ALIGN_LEFT

        /**
         * 五角星的填充颜色
         */
        var mStarColor: Int = Color.YELLOW

        /**
         * 五角星内部五边形外接圆半径与五角星外接圆的比值
         */
        var mInnerPentagonDiameterFactor: Float = .55f
            set(value) {
                field = when{
                    value<=0f -> .55f
                    value > 1f -> 1f
                    else->value
                }
            }

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

            typedArray.recycle()
        }

    }
}