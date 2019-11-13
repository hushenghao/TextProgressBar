package com.dede.textprogressbar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.addListener
import kotlin.math.max
import kotlin.math.min


class TextProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {


    private val PROGRESS_ANIM_DURATION = 100L

    private var currentDrawable: Drawable? = null

    private var maxProgress = 100
    private var progress = 0

    private val uiThreadId: Long = Thread.currentThread().id

    private val paint: Paint

    private var progressText: CharSequence? = null
    private var leftText: CharSequence? = null
    private val textPadding: Int
    private val textColor: Int

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TextProgressBar, defStyleAttr, 0)
        val drawable = a.getDrawable(R.styleable.TextProgressBar_progressDrawable)
        setProgressDrawable(drawable)

        maxProgress = a.getInt(R.styleable.TextProgressBar_maxProgress, maxProgress)
        progress = a.getInt(R.styleable.TextProgressBar_progress, progress)

        textColor = a.getColor(R.styleable.TextProgressBar_textColor, Color.BLACK)
        val textSize = a.getDimension(R.styleable.TextProgressBar_textSize, 10f)
        progressText = a.getText(R.styleable.TextProgressBar_progressText)
        leftText = a.getText(R.styleable.TextProgressBar_leftText)
        textPadding = a.getDimensionPixelSize(R.styleable.TextProgressBar_textPadding, 0)

        a.recycle()

        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = textColor
        paint.textSize = textSize
    }

    private fun setProgressDrawable(d: Drawable?) {
        if (d == currentDrawable)
            return

        if (currentDrawable != null) {
            currentDrawable!!.callback = null
            unscheduleDrawable(currentDrawable)
        }

        currentDrawable = d

        if (d != null) {
            d.callback = this
            val drawableHeight = d.minimumHeight
            if (height < drawableHeight) {
                requestLayout()
            }
        }

        updateDrawableBounds(height)

        doRefreshProgress(false)
    }

    private var onAnim = false
    private var animator: ValueAnimator? = null

    private fun doRefreshProgress(anim: Boolean) {
        val d = currentDrawable
        if (d != null) {
            if (anim) {
                if (animator != null && animator!!.isRunning) {
                    animator!!.cancel()
                }
                val r = (width * progress * 1f / maxProgress + .5).toInt()
                val valueAnimator = ValueAnimator.ofInt(d.bounds.right, r)
                valueAnimator.interpolator = LinearInterpolator()
                valueAnimator.duration = PROGRESS_ANIM_DURATION
                valueAnimator.addUpdateListener {
                    val copy = d.copyBounds()
                    copy.right = it.animatedValue as Int
                    d.bounds = copy
                }
                valueAnimator.addListener(onEnd = {
                    onAnim = false
                }, onCancel = {
                    onAnim = false
                }, onStart = {
                    onAnim = true
                })
                valueAnimator.start()
                this.animator = valueAnimator
            } else {
                invalidate()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var dw = 0
        var dh = 0

        val d = currentDrawable
        if (d != null) {
            dw = d.intrinsicWidth
            dh = d.intrinsicHeight
        }

        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        if (hMode == MeasureSpec.AT_MOST) {
            val metrics = paint.fontMetricsInt
            val th = metrics.bottom - metrics.top

            if (dh < th) {
                dh = th
            }
        }

        dw += paddingLeft + paddingRight
        dh += paddingTop + paddingBottom

        val measuredWidth = resolveSizeAndState(dw, widthMeasureSpec, 0)
        val measuredHeight = resolveSizeAndState(dh, heightMeasureSpec, 0)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateDrawableBounds(h)
    }

    private fun updateDrawableBounds(h: Int) {
        currentDrawable?.setBounds(0, 0, 0, h - paddingTop - paddingBottom)
    }

    private var onDrawing = false

    override fun onDraw(canvas: Canvas) {
        val metrics = paint.fontMetrics
        val textCenterY = height / 2f - (metrics.descent + metrics.ascent) / 2

        val d = currentDrawable

        // 绘制进度
        if (d != null) {
            if (!onAnim) {
                val r = (width * progress * 1f / maxProgress + .5).toInt()
                val copy = d.copyBounds()
                copy.right = r
                onDrawing = true
                d.bounds = copy
                onDrawing = false
            }
            d.draw(canvas)
        }

        var ltw = 0f// 左边文字宽度
        var drawLeft = true// 是否需要绘制左边文字
        // 测量左边文字
        if (!TextUtils.isEmpty(leftText)) {
            val lt = leftText!!
            val ltl = lt.length
            ltw = paint.measureText(lt, 0, ltl)
        } else {
            drawLeft = false
        }

        // 绘制进度文字
        if (d != null && !TextUtils.isEmpty(progressText)) {
            val t = progressText!!
            val len = t.length
            val tw = paint.measureText(t, 0, len)

            var x = d.bounds.right.toFloat()
            val pw = d.bounds.width()// 当前进度条宽度

            val signLt = ltw + textPadding * 2
            val signT = tw + textPadding * 2

            if (pw < signT) {// 不够绘制进度文字
                x += textPadding
                paint.textAlign = Paint.Align.LEFT
                if (pw < signLt) {// 也不够绘制左边文字
                    drawLeft = false
                }
            } else {// 够绘制进度文字
                x -= textPadding
                paint.textAlign = Paint.Align.RIGHT
                if (pw < ltw + tw + textPadding * 3) {// 不够绘制全部文字
                    drawLeft = false
                }
            }

            canvas.drawText(t, 0, len, x, textCenterY, paint)
        }

        // 绘制左边文字
        if (drawLeft) {
            val lt = leftText!!
            val end = lt.length
            paint.color = textColor
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(lt, 0, end, textPadding.toFloat(), textCenterY, paint)
        }
    }

    override fun invalidateDrawable(drawable: Drawable) {
        if (onDrawing)
            return
        super.invalidateDrawable(drawable)
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return who == currentDrawable || super.verifyDrawable(who)
    }

    @JvmOverloads
    @Synchronized
    fun setProgress(p: Int, anim: Boolean = false) {
        progress = min(maxProgress, max(p, 0))
        if (uiThreadId == Thread.currentThread().id) {
            doRefreshProgress(anim)
        } else {
            post { doRefreshProgress(anim) }
        }
    }

    @Synchronized
    fun getPorgress(): Int {
        return progress
    }

    fun setMaxProgress(max: Int) {
        if (max < 0) return
        maxProgress = max
        invalidate()
    }

    fun setProgressText(text: CharSequence?) {
        this.progressText = text
        if (onAnim) {
            return
        }
        invalidate()
    }

    fun setLeftText(text: CharSequence?) {
        this.leftText = text
        if (onAnim) {
            return
        }
        invalidate()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val parcelable = super.onSaveInstanceState()
        val savedState = SavedState(parcelable)
        savedState.progressText = progressText ?: ""
        savedState.leftText = leftText ?: ""
        savedState.maxProgress = maxProgress
        savedState.progress = progress
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        setLeftText(savedState.leftText)
        setProgressText(savedState.progressText)
        setMaxProgress(savedState.maxProgress)
        setProgress(savedState.progress, false)
    }

    class SavedState : BaseSavedState {

        var progress: Int = 0
        var maxProgress: Int = 0
        var leftText: CharSequence = ""
        var progressText: CharSequence = ""

        constructor(superState: Parcelable?) : super(superState)

        constructor(`in`: Parcel) : super(`in`) {
            progress = `in`.readInt()
            maxProgress = `in`.readInt()
            leftText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`)
            progressText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`)
        }

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeInt(progress)
            out?.writeInt(maxProgress)
            TextUtils.writeToParcel(leftText, out, flags)
            TextUtils.writeToParcel(progressText, out, flags)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState {
                return SavedState(`in`)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}