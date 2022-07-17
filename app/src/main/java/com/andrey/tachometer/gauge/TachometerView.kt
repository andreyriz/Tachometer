package com.andrey.tachometer.gauge

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.StringRes
import com.andrey.tachometer.R
import kotlin.math.ceil


class TachometerView(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    companion object {
        private const val SPEED_FORMAT = "%.1f"
    }

    private var mHeight = 0
    private var mWidth = 0
    private var mRadius = 0
    private var mPadding = 0
    private var mCorrectionPadding = 25
    private var mNumbersMargin = 50
    private var numeralSpacing = 0
    private var mCenterTextRect = Rect()
    private var mUnitOfSpeed = Rect()
    private var rpmRightRect = Rect()
    private var mNumberOfRevolutions = Rect()
    private var rpmRect = Rect()
    private var maxWidthOfClock = 0
    private var maxHeightOfClock = 0
    private val startRadius = 135.0
    private val rect = RectF()
    private val DEGREES_IN_CIRCLE = 270f
    private val DIVIDER_ANGLE = 2f
    private var numbers = arrayOf("0", "10", "20", "30", "40", "50", "60")

    private var iconSizeWidth = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_PX,
        80f,
        context!!.resources.displayMetrics
    )
    private var iconSizeHeight = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_PX,
        32f,
        context!!.resources.displayMetrics
    )

    private val defaultPaint = Paint()
    private val greenPaint = Paint()
    private val yellowPaint = Paint()
    private val redPaint = Paint()
    private val lamePaint = Paint()
    private val whiteBoldPaint12 = Paint()
    private val whiteBoldPaint32 = Paint()
    private var speedUnit = SpeedUnits.MPH
    private var speed = 0f
    private var rpm = 0
    private var rpmToView = 0
    private var minWidth = 0
    private lateinit var canvas: Canvas

    init {
        initPaints()
    }

    private fun initPaints() {
        with(defaultPaint) {
            color = context.getColor(R.color.gray)
            strokeWidth = 40f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        with(greenPaint) {
            color = context.getColor(R.color.lame)
            strokeWidth = 40f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        with(yellowPaint) {
            color = context.getColor(R.color.yellow)
            strokeWidth = 40f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        with(redPaint) {
            color = context.getColor(R.color.red)
            strokeWidth = 40f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        with(lamePaint) {
            color = context.getColor(R.color.lame)
            strokeWidth = 7f
            isAntiAlias = true
        }

        with(whiteBoldPaint12) {
            color = Color.WHITE
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                16f,
                context.resources.displayMetrics
            )
            isAntiAlias = true
        }

        with(whiteBoldPaint32) {
            color = Color.WHITE
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                72f,
                context.resources.displayMetrics
            )
            isAntiAlias = true
        }
    }

    private fun initValues() {
        mHeight = height
        mWidth = width
        mPadding = numeralSpacing + 50
        minWidth = mWidth.coerceAtMost(mHeight)
        mRadius = minWidth / 2 - mPadding

        dx = (width - minWidth)/2f
    }

    private var dx=0f

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        initValues()
        canvas?.let {
            this.canvas = canvas
            //Draw circle image background
            drawCircleImage(it)

            //Draw the outermost circle first
            drawOuterCircle(it)

            //Draw numbers
            drawNumbers(it)

            //Draw center speed
            drawCenterSpeed(it)

            //Draw below speed units
            drawUnitOfSpeed(it)

            //draw scale around degrees
            drawFullScale(it)

            drawScale(it)

            //draw bottom text
            drawBottomText(it)

            drawRpmRightText(it)

            //drawRpm
            drawRpm(it)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)

        invalidate()
    }

    private fun drawNumbers(canvas: Canvas) {
        val size = 16f
        whiteBoldPaint12.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        whiteBoldPaint12.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            size,
            context.resources.displayMetrics
        )
        val rect = Rect()
        var x: Float
        var y: Float

        val mOuterBorderRadius = (Math.min(height/2f - mPadding - 10, width/2f - mPadding - 10))

        var nextNumDegree = 0
        for (i in numbers) {
            whiteBoldPaint12.getTextBounds(i, 0, i.length, rect)
            x = ((mOuterBorderRadius - (mOuterBorderRadius * 0.25)) * Math.cos(
                Math.toRadians(startRadius + nextNumDegree)
            ) + width/2f - rect.width() / 2).toFloat()
            y = ((mOuterBorderRadius - (mOuterBorderRadius * 0.25)) * Math.sin(
                Math.toRadians(startRadius + nextNumDegree)
            ) + height/2f + rect.height() / 2).toFloat()
            canvas.drawText(i, x, y, whiteBoldPaint12)
            nextNumDegree += 45
        }
    }

    private fun drawOuterCircle(canvas: Canvas?) {
        lamePaint.style = Paint.Style.STROKE
        lamePaint.strokeWidth = 7f
        canvas?.drawCircle(
            width/2f
            ,height/2f,
            (Math.min(height/2f - mPadding, width/2f - mPadding)).toFloat(),
            lamePaint
        )
    }

    private fun drawCircleImage(canvas: Canvas) {

        val bitmap: Bitmap =
            getCircularBitmap(
                Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_tachometer
                    ), minWidth - mPadding, minWidth - mPadding, false
                )
            )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val matrix = Matrix()

        matrix.postTranslate(
            mWidth - minWidth + mPadding/2f,
            (minWidth / 2 - bitmap.height / 2).toFloat()
        )


        canvas.drawBitmap(
            bitmap,  // Bitmap
            matrix,  // Matrix
            paint // Paint
        )
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {

        if (maxHeightOfClock == 0) {
            maxWidthOfClock = bitmap.width
            maxHeightOfClock = bitmap.height
        }

        val radius = (Math.min(bitmap.width, bitmap.height / 2)).toFloat()

        val canvasBitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val shader = BitmapShader(
            bitmap, Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = shader
        val canvas = Canvas(canvasBitmap)
        canvas.drawCircle(
            (bitmap.width / 2).toFloat(),
            (bitmap.height / 2).toFloat(),
            radius, paint
        )
        return canvasBitmap
    }

    private fun drawCenterSpeed(canvas: Canvas) {
        val size = 72f

        whiteBoldPaint32.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            size,
            context.resources.displayMetrics
        )

        val speedText = String.format("%.1f", speed)

        whiteBoldPaint32.getTextBounds(speedText, 0, speedText.length, mCenterTextRect)
        canvas.drawText(
            speedText,
            (width / 2 - mCenterTextRect.width() / 2).toFloat(),
            (height / 2 + mCenterTextRect.height() / 2).toFloat(),
            whiteBoldPaint32
        )
    }

    private fun drawUnitOfSpeed(canvas: Canvas) {
        val size = 32f

        lamePaint.style = Paint.Style.FILL
        lamePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        lamePaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            size,
            context.resources.displayMetrics
        )

        val tmp = context.getString(speedUnit.stringId)
        lamePaint.getTextBounds(tmp, 0, tmp.length, mUnitOfSpeed)

        canvas.drawText(
            tmp,
            (width / 2 - mUnitOfSpeed.width() / 2).toFloat(),
            (height / 2 + mCenterTextRect.height()).toFloat() + 20,
            lamePaint
        )
    }

    private fun drawScale(canvas: Canvas) {
        val size = 40f

        greenPaint.strokeWidth = size
        yellowPaint.strokeWidth = size
        redPaint.strokeWidth = size

        if (rpm > 0) {
            rect.set(
                dx + mCorrectionPadding
                        + 75f
                ,
                0f + mCorrectionPadding
                        + 75f
                ,
                dx+minWidth - mCorrectionPadding
                        - 75f
                ,
                minWidth - mCorrectionPadding
                        - 75f,
            )


            val angleOfSection = (DEGREES_IN_CIRCLE / 12) - DIVIDER_ANGLE
            var paint = greenPaint

            var sumOfDividers = ceil(rpm/5.0).toInt()

            if (sumOfDividers >= 7)
                paint = yellowPaint
            else
                if (sumOfDividers >= 10)
                    paint = redPaint

            if (sumOfDividers > 12) {
                sumOfDividers = 12
                paint = redPaint
            }

            for (i in 0 until sumOfDividers) {
                val startAngle = 135 + i * (angleOfSection + DIVIDER_ANGLE) + DIVIDER_ANGLE / 2
                canvas.drawArc(rect, startAngle, angleOfSection, false, paint)
            }
        }
    }

    private fun drawFullScale(canvas: Canvas) {
        val size = 40f
        defaultPaint.strokeWidth = size

        rect.set(
            dx + mCorrectionPadding
                    + 75f
            ,
            0f + mCorrectionPadding
                    + 75f
            ,
            dx+minWidth - mCorrectionPadding
                    - 75f
            ,
            minWidth - mCorrectionPadding
                    - 75f,
        )


        val angleOfSection = (DEGREES_IN_CIRCLE / 12) - DIVIDER_ANGLE
        for (i in 0..11) {
            val startAngle = 135 + i * (angleOfSection + DIVIDER_ANGLE) + DIVIDER_ANGLE / 2
            canvas.drawArc(rect, startAngle, angleOfSection, false, defaultPaint)
        }

        lamePaint.style = Paint.Style.STROKE
        val rectangle = RectF(
            dx + mCorrectionPadding
                    + 75f
            ,
            0f + mCorrectionPadding
                    + 75f
            ,
            dx+minWidth - mCorrectionPadding
                    - 75f
            ,
            minWidth - mCorrectionPadding
                    - 75f,
        )
        canvas.drawArc(rectangle, 105f, 25f, false, lamePaint)
        canvas.drawArc(rectangle, 50f, 20f, false, lamePaint)

    }

    private fun drawBottomText(canvas: Canvas) {
        val size = 32f
        whiteBoldPaint12.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        whiteBoldPaint12.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            size,
            context.resources.displayMetrics
        )

        val rpms = rpmToView.toString()

        whiteBoldPaint12.getTextBounds(rpms, 0, rpms.length, mNumberOfRevolutions)

        val mOuterBorderRadius = (Math.min(height/2f, width/2f))

        val x: Float = (width/2 -
                mNumberOfRevolutions.width()/2
                ).toFloat()
        val y: Float = ((mOuterBorderRadius - (mOuterBorderRadius * 0.25)) * Math.sin(
            Math.toRadians(startRadius)
        ) + height / 2
                + mNumberOfRevolutions.height()/2
                ).toFloat()
        canvas.drawText(rpms, x, y, whiteBoldPaint12)
    }

    private fun drawRpm(canvas: Canvas) {
        val size = 23f
        lamePaint.style = Paint.Style.FILL
        lamePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        lamePaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            size,
            context.resources.displayMetrics
        )

        val tmp = context.getString(R.string.rpm)

        lamePaint.getTextBounds(tmp, 0, tmp.length, rpmRect)

        val x: Float = (width / 2
                - rpmRect.width() / 2
                + 5
                ).toFloat()
        val y: Float = (height - mPadding - 35 - rpmRightRect.height()).toFloat()

        canvas.drawText(tmp, x, y, lamePaint)
    }

    private fun drawRpmRightText(canvas: Canvas) {
        val size = 14f

        val paint = Paint()
        paint.color = context.getColor(R.color.white_50)
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                size,
                context!!.resources.displayMetrics
            )

        val tmp = context.getString(R.string.multiplier)

        paint.getTextBounds(tmp, 0, tmp.length, rpmRightRect)

        val x:Float = (width / 2
                - rpmRightRect.width()/2
                ).toFloat()
        val y:Float = (height - mPadding - 25).toFloat()

        canvas.drawText(tmp, x, y, paint)
    }

    fun setSpeed(speed: Float) {
        this.speed = speed
    }

    fun setSpeedUnit(speedUnit: SpeedUnits) {
        this.speedUnit = speedUnit
        invalidate()
    }

    fun setRPM(rpm: Int) {
        this.rpmToView = rpm
        this.rpm = (ceil(rpm / 100.0)).toInt()
        invalidate()
    }

    enum class SpeedUnits(@StringRes val stringId: Int) {
        MPH(R.string.set_up_speed_unit_mph),
        KNOTS(R.string.set_up_speed_unit_knots),
        KMH(R.string.set_up_speed_unit_km_h)
    }
}