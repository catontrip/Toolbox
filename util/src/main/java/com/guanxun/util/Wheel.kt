package com.guanxun.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.annotation.RawRes
import kotlin.math.abs

class GXWheel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {
    /**
     * 滚动坐标模型
     */
    private val model: WheelLinearModel = WheelLinearModel()

    private var view: V

    /**
     * xml 设置
     */
    var cfg = Config(context, attrs)
        set(v) {
            field = v
            if (field.mOrientation == Orientation.Horizontal) {
                model.itemSize = view.mItemWidth
            } else {
                model.itemSize = view.mItemHeight
            }
            view.calculateAvailableCanvasClip()
        }

    /**
     *   音频驱动
     */
    private var mSoundDriver = SoundDriver()


    /**
     * 滚动管理
     */
    private var mOverScroller: OverScroller = OverScroller(context)

    /**
     * 速度管理
     */
    private var mVelocityTracker: VelocityTracker? = null

    /**
     * 用户数据列表
     */
    var data: List<Any> = ArrayList()
        set(value) {
            if (value.isEmpty()) return
            field = value
            model.itemCount = data.size
            model.itemSize = view.mItemSize4Model
            selectedIndex = 0
            invalidate()
        }

    /**
     * 选中条目的下标
     */
    var selectedIndex = 0
        set(v) {
            if (data.isEmpty()) {
                field = 0
                return
            }
            field = v % data.size
            Log.d(TAG, "selectedIndex: ${model.offSet} ")
            model.turnNToCenter(field)
            Log.d(TAG, "selectedIndex: After ${model.offSet} ")
            invalidate()
            onSelectionChangedListener(data, field)
        }
        get() = model.indexOfItemAtFocus()

    /**
     * 用户定制化的条目格式化lambda，缺省调用toString
     */
    var customizedFormat: (Any) -> String = { it.toString() }
        set(value) {
            field = value
            model.itemSize = view.mItemWidth
        }

    /**
     * 转轮取向
     */
    var orientation: Orientation
        set(v) {
            cfg.mOrientation = v
            view = if (cfg.mOrientation == Orientation.Vertical) VerticalView()
            else HorizontalView()
        }
        get() = cfg.mOrientation

    /**
     * 选中是事件处理方法
     */
    var onSelectionChangedListener: ((List<Any>, Int) -> Unit) = { _, _ -> }

    /**
     * 选中是事件处理方法
     */
    var onItemClickListener: ((List<Any>, Int) -> Unit) = { _, _ -> }

    /**
     *   手指最后触摸的y轴坐标
     */
    private var mLastTouchY = 0f

    /**
     *   手指最后触摸的x轴坐标
     */
    private var mLastTouchX = 0f

    /**
     *  motion的状态。"":未动， "MOVE":手指拖动， "FLING":快速划动
     */
    private var mMotionState = ""

    init {
        model.onFocusItemChangedListener = { i ->
            Log.d(TAG, "$i onFocusItemChangedListener")
            mSoundDriver.playSound()
        }
        view = if (cfg.mOrientation == Orientation.Vertical) VerticalView()
        else HorizontalView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mSoundDriver.release()
    }

    /**
     * 打开音效播放
     * @param resourceId 音频Id
     */
    fun turnOnSoundEffect(@RawRes resourceId: Int) {
        mSoundDriver.load(context, resourceId)
    }

    /**
     * 关闭音效播放
     */
    fun turnOffSoundEffect() {
        mSoundDriver.unload()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mVelocityTracker = mVelocityTracker ?: VelocityTracker.obtain()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mMotionState = ""
                mVelocityTracker?.addMovement(event)
                Log.d(TAG, "onTouchEvent: ACTION_DOWN ${event.x}, ${event.y}")
                // 手指按下
                // 处理滑动事件嵌套 拦截事件序列
                parent?.requestDisallowInterceptTouchEvent(true)
                // 如果未滚动完成，强制滚动完成
                if (!mOverScroller.isFinished) {
                    // 强制滚动完成
                    mOverScroller.forceFinished(true)
                }
                mLastTouchX = event.x
                mLastTouchY = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                Log.d(TAG, "onTouchEvent: ACTION_MOVE ${event.x}, ${event.y}")
                mVelocityTracker?.addMovement(event)
                // 手指移动
                val moveX = event.x
                val deltaX = moveX - mLastTouchX
                val moveY = event.y
                val deltaY = moveY - mLastTouchY
                Log.d(TAG, "onTouchEvent: ACTION_MOVE $deltaY")
                if (cfg.mOrientation == Orientation.Horizontal) {
                    if (abs(deltaX) >= 1) {
                        // deltaY 上滑为正，下滑为负
                        model.turn(deltaX.toInt())
                        mLastTouchX = moveX
                        mMotionState = "MOVE"
                    }
                } else {
                    if (abs(deltaY) >= 1) {
                        // deltaY 左滑为正，右滑为负
                        model.turn(deltaY.toInt())
                        mLastTouchY = moveY
                        mMotionState = "MOVE"
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                Log.d(TAG, "onTouchEvent: ACTION_UP '$mMotionState', ${event.x}, ${event.y}")
                mVelocityTracker?.addMovement(event)
                // 手指抬起
                mVelocityTracker?.let { velocityTracker ->

                    flingHandler(velocityTracker, event)
                }
                mVelocityTracker?.recycle()
                mVelocityTracker = null
            }
            // 事件被取消
            MotionEvent.ACTION_CANCEL -> {
                // 回收
                mVelocityTracker?.recycle()
                mVelocityTracker = null
            }
        }
        invalidate()
        return true

    }

    private fun flingHandler(velocityTracker: VelocityTracker, event: MotionEvent) {
        velocityTracker.computeCurrentVelocity(50, cfg.mMaxFlingVelocity.toFloat())
        val velocityX = if (cfg.mOrientation == Orientation.Horizontal) {
            velocityTracker.xVelocity
        } else velocityTracker.yVelocity

        if (abs(velocityX) > cfg.mMinFlingVelocity) {
            // 快速滑动
            mOverScroller.forceFinished(true)
            if (cfg.mOrientation == Orientation.Horizontal) {
                mOverScroller.fling(
                    model.offSet, 0, -velocityX.toInt(), 0,
                    model.minOffset, model.maxOffset, 0, 0
                )
            } else {
                mOverScroller.fling(
                    0, model.offSet, -velocityX.toInt(), 0, 0, 0,
                    model.minOffset, model.maxOffset
                )
            }
            mMotionState = "FLING"
            Log.d(TAG, "onTouchEvent: FLING, $velocityX")
        } else {
            if (mMotionState == "MOVE") {
                val focusIndex = model.indexOfItemAtFocus()
                if (model.distanceOfNToCenter(focusIndex) != 0) {
                    model.turnNToCenter(focusIndex)
                    invalidate()
                }
                mMotionState = ""
                onSelectionChangedListener(data, focusIndex)
            } else if (mMotionState == "") {
                Log.d(TAG, "onTouchEvent: onItemClickListener ")
                val item = if (cfg.mOrientation == Orientation.Horizontal)
                    model.pointOfIndex(event.x.toInt() - model.coordSysOffset - paddingLeft)
                else
                    model.pointOfIndex(event.y.toInt() - model.coordSysOffset - paddingLeft)

                item?.let {
                    model.turnNToCenter(item.index)
                    onItemClickListener(data, item.index)
                    onSelectionChangedListener(data, item.index)
                    invalidate()
                }
            }
        }
    }

    override fun computeScroll() {
        view.computeScroll()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        view.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val currentIndex = selectedIndex
        Log.d(TAG, "onSizeChanged: $w, $h")
        view.onSizeChanged(w, h, oldw, oldh)
        view.calculateAvailableCanvasClip()
        //至此，wlm的参数才设置完毕，准备就绪。才可以设置选中的item
        selectedIndex = currentIndex
        Log.d(TAG, "onSizeChanged: $selectedIndex")
    }

    override fun onDraw(canvas: Canvas) {
        Log.d(TAG, "onDraw: ")
        view.onDraw(canvas)
    }

    /**
     * *******************************************************************************************
     *
     * V
     *
     * *******************************************************************************************
     */
    abstract inner class V {
        /**
         *
         */
        private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        /**
         * 焦点区域画笔
         */
        protected val outOfFocusTextPaint: Paint
            get() {
                mPaint.color = cfg.mTextColor
                mPaint.textSize = cfg.mTextSize
                mPaint.typeface = normalTypeface
                mPaint.textAlign = Paint.Align.CENTER
                return mPaint
            }
        private val fPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        /**
         * 焦点区域画笔
         */
        protected val focusPaint: Paint
            get() = fPaint
        protected val focusTextPaint: Paint
            get() {
                val p = fPaint
                p.color = cfg.mFocusTextColor
                p.textSize = cfg.mTextSize
                p.typeface = boldTypeface
                p.textAlign = Paint.Align.CENTER
                return p
            }
        private val normalTypeface: Typeface = Typeface.DEFAULT
        private val boldTypeface: Typeface = Typeface.DEFAULT_BOLD
        protected val focusSpotIndicatorPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
            get() {
                return field.apply {
                    color = cfg.mFocusSpotIndicatorColor
                }
            }

        /**
         *文字的最大宽度
         * */
        private val mMaxTextWidth: Int
            get() {
                var max = 0
                data.forEach {
                    val textWidth = focusTextPaint.measureText(customizedFormat(it)).toInt()
                    max = textWidth.coerceAtLeast(max)
                }
                return max
            }

        /**
         * 条目总宽度
         */
        val mItemWidth: Int
            get() = mMaxTextWidth + cfg.mTextLeftAndRightBoundaryMargin.toInt() * 2

        /**
         * 供WheelLineModel使用的item size
         */
        abstract val mItemSize4Model: Int

        /**
         * 条目中字体高度
         */
        private val mItemTextHeight: Int
            get() {
                var height: Int
                focusTextPaint.fontMetrics.apply {
                    // itemHeight实际等于字体高度+一个行间距
                    height = (bottom - top).toInt()
                }
                return height
            }

        /**
         * 条目总高度
         */
        val mItemHeight: Int
            get() {
                return mItemTextHeight + cfg.mTextLeftAndRightBoundaryMargin.toInt() * 2
            }

        /**
         * 条目Y轴上的顶边坐标
         */
        protected val mItemStart: Float
            get() = aperture.centerX() - mMaxTextWidth / 2

        /**
         * 条目Y轴上的底边坐标
         */
        protected val mItemEnd: Float
            get() = aperture.centerX() + mMaxTextWidth / 2

        /**
         * 条目Y轴上的底边坐标
         */
        protected val mItemBottom: Float
            get() = aperture.centerY() + mItemHeight / 2

        init {
            calculateAvailableCanvasClip()
        }

        /**
         * 以View左上角为原点的转轮矩形窗口
         */
        private var apertureByAbsolutCoord: RectF = RectF()

        /**
         * 计算生成apertureByAbsolutCoord
         */
        fun calculateAvailableCanvasClip() {
            val gap = (model.apertureMaxLength - model.apertureLength) / 2
            apertureByAbsolutCoord = if (cfg.mOrientation == Orientation.Horizontal) RectF(
                paddingStart.toFloat() + gap, paddingTop.toFloat(),
                width.toFloat() - paddingEnd - gap, height.toFloat() - paddingBottom
            )
            else RectF(
                paddingStart.toFloat(), paddingTop.toFloat() + gap,
                width.toFloat() - paddingEnd, height.toFloat() - paddingBottom - gap
            )
        }

        /**
         * 以转轮矩形窗口左上角为原点的窗口矩形
         */
        protected val aperture: RectF
            get() {
                return RectF(
                    0f, 0f,
                    apertureByAbsolutCoord.width(), apertureByAbsolutCoord.height()
                )
            }

        protected val focusFramePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
            get() {
                return field.apply {
                    color = cfg.mFocusFrameColor
                }
            }
        protected val focusLineIndicatorPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
            get() {
                return field.apply {
                    color = cfg.mFocusCenterLineIndicatorColor
                }
            }
        protected val focusRectPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
            get() {
                return field.apply {
                    color = cfg.mFocusFrameColor
                }
            }

        fun onDraw(canvas: Canvas) {
            Log.d(TAG, "onDraw: ")
            canvas.save()
            canvas.clipRect(apertureByAbsolutCoord)
            canvas.translate(
                apertureByAbsolutCoord.left,
                apertureByAbsolutCoord.top
            )
            drawBottomLayer(canvas)
            drawFocus(canvas)
            canvas.restore()
        }

        // 文字中心距离baseline的距离
        protected fun drawText(
            canvas: Canvas,
            paint: Paint,
            dataElement: Any,
            start: Float,
            centerY: Float
        ) {
            val baseline = centerY + paint.centerToBaseline()
            val text = customizedFormat(dataElement)
            canvas.drawText(text, (start + start + mItemWidth) / 2, baseline, paint)
        }

        abstract fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
        abstract fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int)
        abstract fun computeScroll()

        /**
         * 绘制整个视窗图像，焦点区域将被drawFocus()覆盖
         */
        abstract fun drawBottomLayer(canvas: Canvas)

        /**
         * 绘制焦点区域
         * @param canvas 画布
         */
        abstract fun drawFocus(canvas: Canvas)


    }

    /****************************************************************************************
     *
     *                               VerticalView
     *
     */
    inner class VerticalView() : V() {
        override val mItemSize4Model: Int
            get() = mItemHeight

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
                    height = mItemHeight * 7 + paddingTop + paddingBottom
                }

                MeasureSpec.UNSPECIFIED -> {
                    height = mItemHeight * 5 + paddingTop + paddingBottom
                }
            }

            when (widthMode) {
                MeasureSpec.EXACTLY -> {
                    width = widthSize
                }

                MeasureSpec.AT_MOST -> {
                    width = mItemWidth + paddingEnd + paddingStart
                }

                MeasureSpec.UNSPECIFIED -> {
                    width = paddingStart + mItemWidth + paddingEnd
                }
            }

            setMeasuredDimension(
                resolveSizeAndState(width, widthMeasureSpec, 0),
                resolveSizeAndState(height, heightMeasureSpec, 0)
            )
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            model.apertureMaxLength = h - paddingTop - paddingBottom
            model.itemSize = mItemHeight
            model.itemCount = data.size

            if (h - paddingTop - paddingBottom < mItemHeight)
                Log.e(
                    TAG,
                    "onSizeChanged: Height is too small to show item, please adjust view height."
                )
        }

        override fun computeScroll() {
            if (mOverScroller.computeScrollOffset()) {
                model.turn(mOverScroller.currY)
                postInvalidate()
            } else {
                Log.d(TAG, "computeScroll: FINISH")
                mOverScroller.abortAnimation()
                //Fling结束，把焦点区域中的条目调整为左右居中
                val focusIndex = model.indexOfItemAtFocus()

                if (model.distanceOfNToCenter(focusIndex) != 0 && mMotionState == "FLING") {
                    model.turnNToCenter(focusIndex)
                    onSelectionChangedListener(data, focusIndex)
                    postInvalidate()
                }
            }
        }

        override fun drawBottomLayer(canvas: Canvas) {
            for (i in data.indices) {
                val y = model.coordOfIndex(i).toFloat()
                val itemCenterY = (y + y + mItemHeight) / 2
                drawText(
                    canvas,
                    outOfFocusTextPaint,
                    data[i],
                    aperture.centerX() - mItemWidth / 2,
                    itemCenterY
                )
            }
            model.itemNeedSplitDisplay()?.let {
                val itemCenterY = (it.startCoord + it.startCoord + mItemHeight) / 2f
                drawText(
                    canvas,
                    outOfFocusTextPaint,
                    data[it.index],
                    aperture.centerX() - mItemWidth / 2,
                    itemCenterY
                )
            }
        }

        override fun drawFocus(canvas: Canvas) {
            canvas.save()
            canvas.clipRect(
                aperture.left,
                model.focusStart.toFloat(),
                aperture.right,
                model.focusEnd.toFloat()
            )
            drawFocusRect(canvas)

            for (i in data.indices) {
                val y = model.coordOfIndex(i).toFloat()
                val itemCenterY = (y + y + mItemHeight) / 2
                drawText(
                    canvas,
                    focusTextPaint,
                    data[i],
                    aperture.centerX() - mItemWidth / 2,
                    itemCenterY
                )
            }
            drawFocusCenterLineIndicator(canvas)
            drawFocusSpotIndicator(canvas)
            drawFocusFrame(canvas)
            canvas.restore()
        }

        /**
         * 绘制焦点区域背景矩形和两侧的平行标记线
         */
        private fun drawFocusRect(canvas: Canvas) {
            if (cfg.mDrawFocusRect) {
                val size = mItemWidth * cfg.mFocusHeightFactor
                val h = mItemHeight.toFloat()
                var w = size
                if (w > aperture.width()) w = aperture.width()

                val sharp = aperture.run {
                    RectF(
                        centerX() - w / 2, centerY() - h / 2,
                        centerX() + w / 2, centerY() + h / 2
                    )
                }
                canvas.drawRoundRect(
                    sharp, 20F, 20F, focusRectPaint
                )
            }
        }

        /**
         * 绘制焦点区域中心标记线
         */
        private fun drawFocusCenterLineIndicator(canvas: Canvas) {

            if (cfg.mShowFocusCenterLineIndicator) {
                val size = mItemWidth * cfg.mFocusCenterLineIndicatorHeightFactor
                val width = cfg.mFocusCenterLineIndicatorWidth
                val sharp = aperture.run {
                    RectF(
                        centerX() - size / 2, centerY() - width / 2,
                        centerX() + size / 2, centerY() + width / 2
                    )
                }
                canvas.drawRect(
                    sharp, focusLineIndicatorPaint
                )
            }
        }

        /**
         * 绘制焦点区域底边中心标记圆点
         */
        private fun drawFocusSpotIndicator(canvas: Canvas) {
            if (cfg.mShowFocusSpotIndicator) {

                val centerY = (model.focusStart.toFloat() + model.focusEnd.toFloat()) / 2
                canvas.drawCircle(
                    mItemStart - cfg.mFocusSpotIndicatorRadius, centerY,
                    cfg.mFocusSpotIndicatorRadius, focusSpotIndicatorPaint
                )
            }
        }

        /**
         * 绘制焦点区域左右边框
         * @param canvas 画布
         */
        private fun drawFocusFrame(canvas: Canvas) {
            val width = mItemWidth * cfg.mFocusFrameSizeFactor
            val left = aperture.centerX() - width / 2
            val right = aperture.centerX() + width / 2
            if (cfg.showFocusFrame) {
                canvas.drawRect(
                    left,
                    model.focusStart.toFloat(),
                    right,
                    model.focusStart.toFloat() + cfg.mFocusFrameWidth,
                    focusFramePaint
                )
                canvas.drawRect(
                    left, model.focusEnd.toFloat() - cfg.mFocusFrameWidth, right,
                    model.focusEnd.toFloat(), focusFramePaint
                )
            }
        }
    }


    /****************************************************************************************
     *
     *                               HorizontalView
     *
     */
    inner class HorizontalView : V() {
        override val mItemSize4Model: Int
            get() = mItemWidth

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
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
                    height = mItemHeight + paddingTop + paddingBottom
                }

                MeasureSpec.UNSPECIFIED -> {
                    height = mItemHeight + paddingTop + paddingBottom
                }
            }

            when (widthMode) {
                MeasureSpec.EXACTLY -> {
                    width = widthSize
                }

                MeasureSpec.AT_MOST -> {
                    width = widthSize
                }

                MeasureSpec.UNSPECIFIED -> {
                    width = widthSize
                }
            }

            setMeasuredDimension(
                resolveSizeAndState(width, widthMeasureSpec, 0),
                resolveSizeAndState(height, heightMeasureSpec, 0)
            )
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            model.apertureMaxLength = w - paddingLeft - paddingRight
            model.itemSize = mItemWidth
            model.itemCount = data.size
            if (h - paddingTop - paddingBottom < mItemHeight)
                Log.e(
                    TAG,
                    "onSizeChanged: Height is too small to show item, please adjust view height."
                )
        }

        override fun computeScroll() {
            if (mOverScroller.computeScrollOffset()) {
                model.turn(mOverScroller.currX)
                postInvalidate()
            } else {
                Log.d(TAG, "computeScroll: FINISH")
                mOverScroller.abortAnimation()
                //Fling结束，把焦点区域中的条目调整为左右居中
                val focusIndex = model.indexOfItemAtFocus()

                if (model.distanceOfNToCenter(focusIndex) != 0 && mMotionState == "FLING") {
                    model.turnNToCenter(focusIndex)
                    onSelectionChangedListener(data, focusIndex)
                    postInvalidate()
                }
            }
        }

        /**
         * 绘制整个视窗图像，焦点区域将被drawFocus()覆盖
         */
        override fun drawBottomLayer(canvas: Canvas) {

            for (i in data.indices) {
                val x = model.coordOfIndex(i).toFloat()
                drawText(canvas, outOfFocusTextPaint, data[i], x, aperture.centerY())
            }
            model.itemNeedSplitDisplay()?.let {
//        val itemCenterY = (it.startCoord + it.startCoord + mItemHeight) / 2f
                drawText(
                    canvas,
                    outOfFocusTextPaint,
                    data[it.index],
                    it.startCoord.toFloat(),
                    aperture.centerY()
                )
            }
        }

        /**
         * 绘制焦点区域
         * @param canvas 画布
         */
        override fun drawFocus(canvas: Canvas) {
            canvas.save()
            canvas.clipRect(
                model.focusStart.toFloat(),
                aperture.top,
                model.focusEnd.toFloat(),
                aperture.bottom
            )

            drawFocusRect(canvas)

            for (i in data.indices) {
                val x = model.coordOfIndex(i).toFloat()
                drawText(canvas, focusTextPaint, data[i], x, aperture.centerY())
            }
            drawFocusCenterLineIndicator(canvas)
            drawFocusSpotIndicator(canvas)
            drawFocusFrame(canvas)
            canvas.restore()
        }

        /**
         * 绘制焦点区域背景矩形和两侧的平行标记线
         */
        private fun drawFocusRect(canvas: Canvas) {
            val height = mItemHeight * cfg.mFocusHeightFactor
            var top = aperture.centerY() - height / 2
            if (top < 0) top = 0f
            var bottom = aperture.centerY() + height / 2
            if (bottom > aperture.centerY() * 2) bottom = aperture.centerY() * 2
            if (cfg.mDrawFocusRect) {
                canvas.drawRoundRect(
                    model.focusStart.toFloat(), top, model.focusEnd.toFloat(), bottom,
                    20F, 20F, focusRectPaint
                )
            }
        }

        /**
         * 绘制焦点区域中心标记线
         */
        private fun drawFocusCenterLineIndicator(canvas: Canvas) {
            if (cfg.mShowFocusCenterLineIndicator) {
                val height = mItemHeight * cfg.mFocusCenterLineIndicatorHeightFactor
                val top = aperture.centerY() - height / 2
                val bottom = aperture.centerY() + height / 2
                val centerX = (model.focusStart.toFloat() + model.focusEnd.toFloat()) / 2
                canvas.drawRect(
                    centerX - cfg.mFocusCenterLineIndicatorWidth / 2, top,
                    centerX + cfg.mFocusCenterLineIndicatorWidth / 2, bottom,
                    focusLineIndicatorPaint
                )
            }
        }

        /**
         * 绘制焦点区域底边中心标记圆点
         */
        private fun drawFocusSpotIndicator(canvas: Canvas) {
            if (cfg.mShowFocusSpotIndicator) {

                val centerX = (model.focusStart.toFloat() + model.focusEnd.toFloat()) / 2
                canvas.drawCircle(
                    centerX, mItemBottom - cfg.mFocusSpotIndicatorRadius,
                    cfg.mFocusSpotIndicatorRadius, focusSpotIndicatorPaint
                )
            }
        }


        /**
         * 绘制焦点区域左右边框
         * @param canvas 画布
         */
        private fun drawFocusFrame(canvas: Canvas) {
            val height = mItemHeight * cfg.mFocusFrameSizeFactor
            val top = aperture.centerY() - height / 2
            val bottom = aperture.centerY() + height / 2
            if (cfg.showFocusFrame) {
                canvas.drawRect(
                    model.focusStart.toFloat(),
                    top,
                    model.focusStart.toFloat() + cfg.mFocusFrameWidth,
                    bottom,
                    focusFramePaint
                )
                canvas.drawRect(
                    model.focusEnd.toFloat() - cfg.mFocusFrameWidth, top,
                    model.focusEnd.toFloat(), bottom, focusFramePaint
                )
            }
        }
    }


    open class Config(val context: Context, val attrs: AttributeSet?) {
        protected val _defaultTextSize = sp2px(50f)
        protected val DEFAULT_TEXT_BOUNDARY_MARGIN = dp2px(10f)
        protected val DEFAULT_DIVIDER_HEIGHT = dp2px(1f)
        protected val DEFAULT_NORMAL_TEXT_COLOR = Color.DKGRAY
        protected val DEFAULT_SELECTED_TEXT_COLOR = Color.BLACK
        var mOrientation = Orientation.Vertical

        // 字体大小
        var mTextSize = 0f

        // 字体外边距，目的是留有边距
        var mTextLeftAndRightBoundaryMargin = 0f

        // 文字颜色
        var mTextColor = 0

        // 选中item文字颜色
        var mFocusTextColor = 0

        // 是否显示焦点框左右边线
        var showFocusFrame = false

        // 焦点框左右边线高度
        var mFocusFrameWidth = 0f

        // 焦点框左右边线高度系数
        var mFocusFrameSizeFactor = 0f

        // 分割线的颜色
        var mFocusFrameColor = 0

        // 是否绘制焦点窗
        var mDrawFocusRect = false

        // 焦点窗背景颜色
        var mFocusRectColor = 0

        // 焦点窗左右边线高度系数
        var mFocusHeightFactor = 0f

        var mShowFocusCenterLineIndicator = true

        var mFocusCenterLineIndicatorColor = 0

        var mFocusCenterLineIndicatorWidth = 0f

        var mFocusCenterLineIndicatorHeightFactor = 0f

        var mShowFocusSpotIndicator = true

        var mFocusSpotIndicatorColor = 0

        var mFocusSpotIndicatorRadius = 0f

        var mMaxFlingVelocity = 0

        var mMinFlingVelocity = 0

        init {
            Log.d(TAG, "PARENT: ")
            initAttrsAndDefault(context, attrs)
            val viewConfiguration = ViewConfiguration.get(context)
            mMaxFlingVelocity = viewConfiguration.scaledMaximumFlingVelocity
            mMinFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity
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
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GXWheel)
            val mOrientationEnumValue = typedArray.getInt(R.styleable.GXWheel_orientation, 1)

            mOrientation = mOrientationEnumValue.toOrientation()
            mTextSize =
                typedArray.getDimension(R.styleable.GXWheel_textSize, _defaultTextSize)

            mTextLeftAndRightBoundaryMargin = typedArray.getDimension(
                R.styleable.GXWheel_marginBetweenItem,
                DEFAULT_TEXT_BOUNDARY_MARGIN
            )
            mTextColor =
                typedArray.getColor(
                    R.styleable.GXWheel_textColor,
                    DEFAULT_NORMAL_TEXT_COLOR
                )
            mFocusTextColor = typedArray.getColor(
                R.styleable.GXWheel_focusTextColor,
                DEFAULT_SELECTED_TEXT_COLOR
            )
            showFocusFrame = typedArray.getBoolean(R.styleable.GXWheel_showFocusFrame, false)
            mFocusFrameWidth =
                typedArray.getDimension(
                    R.styleable.GXWheel_focusFrameWidth,
                    DEFAULT_DIVIDER_HEIGHT
                )

            mFocusFrameColor =
                typedArray.getColor(
                    R.styleable.GXWheel_focusFrameColor,
                    DEFAULT_SELECTED_TEXT_COLOR
                )
            mDrawFocusRect =
                typedArray.getBoolean(R.styleable.GXWheel_drawFocusRect, false)
            mFocusRectColor =
                typedArray.getColor(R.styleable.GXWheel_focusRectColor, Color.TRANSPARENT)

            mFocusHeightFactor = typedArray.getFloat(
                R.styleable.GXWheel_focusHeightFactor, 1f
            )

            mShowFocusCenterLineIndicator = typedArray.getBoolean(
                R.styleable.GXWheel_showFocusCenterLineIndicator, true
            )

            mFocusCenterLineIndicatorColor = typedArray.getColor(
                R.styleable.GXWheel_focusCenterLineIndicatorColor, Color.RED
            )

            mFocusCenterLineIndicatorWidth = typedArray.getDimension(
                R.styleable.GXWheel_focusCenterLineIndicatorLineWidth, dp2px(2f)
            )

            mFocusCenterLineIndicatorHeightFactor = typedArray.getFloat(
                R.styleable.GXWheel_focusCenterLineIndicatorFactor, 1f
            )

            mShowFocusSpotIndicator = typedArray.getBoolean(
                R.styleable.GXWheel_showFocusSpotIndicator, true
            )

            mFocusSpotIndicatorColor = typedArray.getColor(
                R.styleable.GXWheel_focusSpotIndicatorColor, Color.RED
            )

            mFocusSpotIndicatorRadius = typedArray.getDimension(
                R.styleable.GXWheel_focusSpotIndicatorRadius, dp2px(2f)
            )

            mFocusFrameSizeFactor = typedArray.getFloat(
                R.styleable.GXWheel_focusFrameSizeFactor, 1f
            )
            typedArray.recycle()
        }
    }
}

enum class Orientation(val value: Int) {
    Vertical(0),
    Horizontal(1)
}

fun Int.toOrientation(): Orientation {
    return Orientation.entries.first { it.ordinal == this }
}
