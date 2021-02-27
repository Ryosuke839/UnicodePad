/*
   Copyright 2018 Ryosuke839

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package jp.ddo.hotmist.unicodepad

import android.content.Context
import android.graphics.*
import android.graphics.Paint.FontMetricsInt
import android.os.Build
import android.text.Spannable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.EmojiSpan
import kotlin.math.ceil
import kotlin.math.floor

class CharacterView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private var paint: Paint = Paint()
    private var paintbg: Paint? = null
    private var paintsq: Paint? = null
    private var paintln: Paint? = null
    private var str: String
    private var span: EmojiSpan? = null
    private var offsetx: Int
    private var offsety: Int
    private var ascent = 0
    private var descent = 0
    private var size = 0f
    private var fullWidth: Int
    private var fullHeight: Int
    private var shrinkWidth: Boolean
    private var shrinkHeight: Boolean
    private var drawSlash: Boolean
    private var validChar: Boolean
    private var emojicache: Bitmap?
    private var invalid: Boolean
    private var cacheRect = Rect()
    private var drawRect = RectF()
    var text: String
        get() = str
        set(str) {
            if (this.str != str) invalid = true
            this.str = str
            span = null
            try {
                val emojiCompat = EmojiCompat.get()
                if (emojiCompat != null) {
                    val spanned = emojiCompat.process(str)
                    if (spanned is Spannable) {
                        val spans = spanned.getSpans(0, str.length, EmojiSpan::class.java)
                        if (spans.isNotEmpty()) {
                            span = spans[0]
                        }
                    }
                }
            } catch (e: IllegalStateException) {
            }
            requestLayout()
        }

    fun setTypeface(tf: Typeface?) {
        if (paint.typeface != tf) invalid = true
        paint.typeface = tf
        requestLayout()
    }

    fun setTextSize(size: Float) {
        if (this.size != context.resources.displayMetrics.scaledDensity * size) invalid = true
        this.size = context.resources.displayMetrics.scaledDensity * size
        requestLayout()
    }

    fun setTextColor(color: Int) {
        if (paint.color != color) invalid = true
        paint.color = color
        paintsq?.let {
            it.color = ColorUtils.setAlphaComponent(color, it.color / 0x1000000)
        }
    }

    override fun setBackgroundColor(color: Int) {
        paintbg = if (color == Color.TRANSPARENT) null else {
            (paintbg ?: Paint().also {
                it.style = Paint.Style.FILL
            }).also {
                it.color = color
            }
        }
    }

    fun setSquareAlpha(alpha: Int) {
        paintsq = if (alpha == 0) null else {
            (paintsq ?: Paint().also {
                paintsq = it
                it.style = Paint.Style.FILL
            }).also {
                it.color = ColorUtils.setAlphaComponent(paint.color, alpha)
            }
        }
    }

    fun setValid(valid: Boolean) {
        validChar = valid
    }

    fun drawLines(draw: Boolean) {
        paintln = if (!draw) null else {
            paintln ?: Paint().also {
                it.color = Color.RED
            }
        }
    }

    fun shrinkWidth(shrink: Boolean) {
        shrinkWidth = shrink
        requestLayout()
    }

    fun shrinkHeight(shrink: Boolean) {
        shrinkHeight = shrink
        requestLayout()
    }

    fun drawSlash(draw: Boolean) {
        drawSlash = draw
        requestLayout()
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var heightNew: Int
        var heightOld = 0
        var sizeu = size
        var sizeb = 0f
        var size = size
        paint.textSize = size
        var fm: Paint.FontMetrics
        var fmi: FontMetricsInt?
        while (true) {
            size = (sizeu + sizeb) / 2f
            paint.textSize = size
            fm = paint.fontMetrics
            fmi = paint.fontMetricsInt
            heightNew = (-fm.top + fm.bottom).toInt()
            if (heightNew > 0) {
                if (heightNew == heightOld) break
                heightOld = heightNew
                sizeb = size
            } else sizeu = size
        }
        heightNew += paddingTop + paddingBottom
        val measure = (Build.VERSION.SDK_INT < 23 || paint.hasGlyph(str)) && validChar || !drawSlash
        var widthNew: Int
        widthNew = (if (measure) paint.measureText(str) else fm.descent - fm.ascent).toInt() + paddingLeft + paddingRight
        if (Build.VERSION.SDK_INT >= 19) {
            span?.let{
                widthNew = it.getSize(paint, str, 0, str.length, fmi) + paddingLeft + paddingRight
            }
        }
        if (widthMode != MeasureSpec.EXACTLY) if (widthMode == MeasureSpec.UNSPECIFIED || widthSize > widthNew) widthSize = widthNew
        if (!shrinkHeight) if (heightMode != MeasureSpec.EXACTLY) if (heightMode == MeasureSpec.UNSPECIFIED || heightSize > heightNew) heightSize = heightNew
        if (shrinkWidth) if (widthNew > widthSize) {
            size = size * widthSize / (widthNew - paddingLeft - paddingRight)
            paint.textSize = size
            fm = paint.fontMetrics
            fmi = paint.fontMetricsInt
            widthNew = (if (measure) paint.measureText(str) else fm.descent - fm.ascent).toInt() + paddingLeft + paddingRight
            if (Build.VERSION.SDK_INT >= 19) {
                span?.let {
                    widthNew = it.getSize(paint, str, 0, str.length, fmi) + paddingLeft + paddingRight
                }
            }
            heightNew = (-fm.top + fm.bottom).toInt()
            heightNew += paddingTop + paddingBottom
        }
        if (shrinkHeight) if (heightMode != MeasureSpec.EXACTLY) if (heightMode == MeasureSpec.UNSPECIFIED || heightSize > heightNew) heightSize = heightNew
        offsetx = (widthSize - widthNew) / 2 + paddingLeft
        ascent = ((heightSize - heightNew) / 2 - fm.top + fm.ascent).toInt() + paddingTop
        offsety = ((heightSize - heightNew) / 2 - fm.top).toInt() + paddingTop
        descent = ((heightSize - heightNew) / 2 - fm.top + fm.descent).toInt() + paddingTop
        fullWidth = widthSize
        fullHeight = heightSize
        drawRect.set(offsetx.toFloat(), paddingTop.toFloat(), (fullWidth - offsetx + paddingLeft - paddingRight).toFloat(), (fullHeight - paddingBottom).toFloat())
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paintbg?.let {
            canvas.drawPaint(it)
        }
        paintsq?.let {
            for (j in floor(-fullWidth * 6.0 / fullHeight).toInt() until ceil(fullWidth * 6.0 / fullHeight).toInt()) for (i in 0..11) if ((i + j) % 2 == 0) canvas.drawRect((fullWidth / 2 + fullHeight * j / 12).toFloat(), (fullHeight * i / 12).toFloat(), (fullWidth / 2 + fullHeight * (j + 1) / 12).toFloat(), (fullHeight * (i + 1) / 12).toFloat(), it)
        }
        paintln?.let {
            canvas.drawLine(0f, ascent.toFloat(), fullWidth.toFloat(), ascent.toFloat(), it)
            canvas.drawLine(0f, offsety.toFloat(), fullWidth.toFloat(), offsety.toFloat(), it)
            canvas.drawLine(0f, descent.toFloat(), fullWidth.toFloat(), descent.toFloat(), it)
            if ((Build.VERSION.SDK_INT < 23 || paint.hasGlyph(str)) && validChar || !drawSlash) {
                canvas.drawLine(offsetx.toFloat(), 0f, offsetx.toFloat(), fullHeight.toFloat(), it)
                canvas.drawLine((fullWidth - offsetx).toFloat(), 0f, (fullWidth - offsetx).toFloat(), fullHeight.toFloat(), it)
            }
        }
        if (str.isNotEmpty()) {
            if (span != null) {
                if (invalid) cache
                emojicache?.also {
                    canvas.drawBitmap(it, cacheRect, drawRect, paint)
                } ?: span?.draw(canvas, str, 0, str.length, offsetx.toFloat(), ascent, offsety, descent, paint)
            } else if ((Build.VERSION.SDK_INT < 23 || paint.hasGlyph(str)) && validChar || !drawSlash) {
                if (invalid) cache
                emojicache?.also {
                    canvas.drawBitmap(it, cacheRect, drawRect, paint)
                } ?: canvas.drawText(str, offsetx.toFloat(), offsety.toFloat(), paint)
            } else {
                val sz = (descent - ascent).toFloat()
                canvas.drawLine(fullWidth / 2 - sz * .4f, ascent + sz * .1f, fullWidth / 2 - sz * .4f, ascent + sz * .9f, paint)
                canvas.drawLine(fullWidth / 2 - sz * .4f, ascent + sz * .1f, fullWidth / 2 + sz * .4f, ascent + sz * .1f, paint)
                canvas.drawLine(fullWidth / 2 + sz * .4f, ascent + sz * .1f, fullWidth / 2 + sz * .4f, ascent + sz * .9f, paint)
                canvas.drawLine(fullWidth / 2 - sz * .4f, ascent + sz * .9f, fullWidth / 2 + sz * .4f, ascent + sz * .9f, paint)
                if (!(Build.VERSION.SDK_INT < 23 || paint.hasGlyph(str))) canvas.drawLine(fullWidth / 2 - sz * .4f, ascent + sz * .1f, fullWidth / 2 + sz * .4f, ascent + sz * .9f, paint)
                if (!validChar) canvas.drawLine(fullWidth / 2 + sz * .4f, ascent + sz * .1f, fullWidth / 2 - sz * .4f, ascent + sz * .9f, paint)
            }
        }
    }

    private val cache: Unit
        get() {
            emojicache = null
            if (span != null || (Build.VERSION.SDK_INT < 23 || paint.hasGlyph(str)) && validChar && paint.measureText(str) > 0f) {
                var w = paint.measureText(str)
                span?.let {
                    if (Build.VERSION.SDK_INT >= 19) {
                        w = it.getSize(paint, str, 0, str.length, paint.fontMetricsInt).toFloat()
                    }
                }
                if (w > 256f) {
                    val size = paint.textSize
                    paint.textSize = size * 256f / w
                    val fm = paint.fontMetrics
                    var w2 = paint.measureText(str).toInt()
                    span?.let {
                        if (Build.VERSION.SDK_INT >= 19) {
                            w2 = it.getSize(paint, str, 0, str.length, paint.fontMetricsInt)
                        }
                    }
                    val bm = Bitmap.createBitmap(w2, (-fm.top + fm.bottom).toInt(), Bitmap.Config.ARGB_8888)
                    val tr = Bitmap.createBitmap(w2, (-fm.top + fm.bottom).toInt(), Bitmap.Config.ARGB_8888)
                    val cv = Canvas(bm)
                    span?.let {
                        if (Build.VERSION.SDK_INT >= 19) it.draw(cv, str, 0, str.length, 0f, 0, (-fm.top).toInt(), (-fm.top + fm.descent).toInt(), paint) else cv.drawText(str, 0f, -fm.top, paint)
                    }
                    if (!bm.sameAs(tr)) {
                        cv.drawColor(0, PorterDuff.Mode.CLEAR)
                        paint.style = Paint.Style.STROKE
                        span?.let {
                            if (Build.VERSION.SDK_INT >= 19) it.draw(cv, str, 0, str.length, 0f, 0, (-fm.top).toInt(), (-fm.top + fm.descent).toInt(), paint) else cv.drawText(str, 0f, -fm.top, paint)
                        }
                        if (bm.sameAs(tr)) {
                            paint.style = Paint.Style.FILL
                            span?.let {
                                if (Build.VERSION.SDK_INT >= 19) it.draw(cv, str, 0, str.length, 0f, 0, (-fm.top).toInt(), (-fm.top + fm.descent).toInt(), paint) else cv.drawText(str, 0f, -fm.top, paint)
                            }
                            emojicache = bm
                            cacheRect.set(0, 0, bm.width, bm.height)
                        } else paint.style = Paint.Style.FILL
                    }
                    paint.textSize = size
                }
            }
            invalid = false
        }

    init {
        val a = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        paint.color = a.getColor(0, Color.RED)
        a.recycle()
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.DEFAULT
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        str = ""
        offsetx = 0
        offsety = 0
        fullWidth = 0
        fullHeight = 0
        shrinkWidth = false
        shrinkHeight = false
        validChar = true
        drawSlash = true
        emojicache = null
        invalid = true
    }
}