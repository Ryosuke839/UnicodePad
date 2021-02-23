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

class CharacterView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private var paint: Paint? = null
    private var paintbg: Paint? = null
    private var paintsq: Paint? = null
    private var paintln: Paint? = null
    private var str: String?
    private var span: EmojiSpan? = null
    private var offsetx: Int
    private var offsety: Int
    private var ascent = 0
    private var descent = 0
    private var size = 0f
    private var width: Int
    private var height: Int
    private var shrinkWidth: Boolean
    private var shrinkHeight: Boolean
    private var drawSlash: Boolean
    private var validChar: Boolean
    private var emojicache: Bitmap?
    private var invalid: Boolean
    var text: String?
        get() = str
        set(str) {
            if (this.str != str) invalid = true
            this.str = str
            span = null
            try {
                val emojiCompat = EmojiCompat.get()
                if (emojiCompat != null) {
                    val spanned = emojiCompat.process(str!!)
                    if (spanned is Spannable) {
                        val spans = spanned.getSpans(0, str.length, EmojiSpan::class.java)
                        if (spans.size > 0) {
                            span = spans[0]
                        }
                    }
                }
            } catch (e: IllegalStateException) {
            }
            requestLayout()
        }

    fun setTypeface(tf: Typeface?) {
        if (!(paint!!.typeface === tf || paint!!.typeface != null && paint!!.typeface == tf)) invalid = true
        paint!!.typeface = tf
        requestLayout()
    }

    fun setTextSize(size: Float) {
        if (this.size != context.resources.displayMetrics.scaledDensity * size) invalid = true
        this.size = context.resources.displayMetrics.scaledDensity * size
        requestLayout()
    }

    fun setTextColor(color: Int) {
        if (paint!!.color != color) invalid = true
        paint!!.color = color
        if (paintsq != null) paintsq!!.color = ColorUtils.setAlphaComponent(color, paintsq!!.color / 0x1000000)
    }

    override fun setBackgroundColor(color: Int) {
        if (color == Color.TRANSPARENT) paintbg = null else {
            if (paintbg == null) {
                paintbg = Paint()
                paintbg!!.style = Paint.Style.FILL
            }
            paintbg!!.color = color
        }
    }

    fun setSquareAlpha(alpha: Int) {
        if (alpha == 0) paintsq = null else {
            if (paintsq == null) {
                paintsq = Paint()
                paintsq!!.style = Paint.Style.FILL
            }
            paintsq!!.color = ColorUtils.setAlphaComponent(paint!!.color, alpha)
        }
    }

    fun setValid(valid: Boolean) {
        validChar = valid
    }

    fun drawLines(draw: Boolean) {
        if (!draw) paintln = null else {
            if (paintln == null) {
                paintln = Paint()
                paintln!!.color = Color.RED
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
        var size_ = size
        paint!!.textSize = size_
        var fm: Paint.FontMetrics
        var fmi: FontMetricsInt?
        while (true) {
            size_ = (sizeu + sizeb) / 2f
            paint!!.textSize = size_
            fm = paint!!.fontMetrics
            fmi = paint!!.fontMetricsInt
            heightNew = (-fm.top + fm.bottom).toInt()
            if (heightNew > 0) {
                if (heightNew == heightOld) break
                heightOld = heightNew
                sizeb = size_
            } else sizeu = size_
        }
        heightNew += paddingTop + paddingBottom
        val measure = (Build.VERSION.SDK_INT < 23 || paint!!.hasGlyph(str)) && validChar || !drawSlash
        var widthNew: Int
        widthNew = (if (measure) paint!!.measureText(str) else fm.descent - fm.ascent).toInt() + paddingLeft + paddingRight
        if (span != null && Build.VERSION.SDK_INT >= 19) {
            widthNew = span!!.getSize(paint!!, str, 0, str!!.length, fmi) + paddingLeft + paddingRight
        }
        if (widthMode != MeasureSpec.EXACTLY) if (widthMode == MeasureSpec.UNSPECIFIED || widthSize > widthNew) widthSize = widthNew
        if (!shrinkHeight) if (heightMode != MeasureSpec.EXACTLY) if (heightMode == MeasureSpec.UNSPECIFIED || heightSize > heightNew) heightSize = heightNew
        if (shrinkWidth) if (widthNew > widthSize) {
            size_ = size_ * widthSize / (widthNew - paddingLeft - paddingRight)
            paint!!.textSize = size_
            fm = paint!!.fontMetrics
            fmi = paint!!.fontMetricsInt
            widthNew = (if (measure) paint!!.measureText(str) else fm.descent - fm.ascent).toInt() + paddingLeft + paddingRight
            if (span != null && Build.VERSION.SDK_INT >= 19) {
                widthNew = span!!.getSize(paint!!, str, 0, str!!.length, fmi) + paddingLeft + paddingRight
            }
            heightNew = (-fm.top + fm.bottom).toInt()
            heightNew += paddingTop + paddingBottom
        }
        if (shrinkHeight) if (heightMode != MeasureSpec.EXACTLY) if (heightMode == MeasureSpec.UNSPECIFIED || heightSize > heightNew) heightSize = heightNew
        offsetx = (widthSize - widthNew) / 2 + paddingLeft
        ascent = ((heightSize - heightNew) / 2 - fm.top + fm.ascent).toInt() + paddingTop
        offsety = ((heightSize - heightNew) / 2 - fm.top).toInt() + paddingTop
        descent = ((heightSize - heightNew) / 2 - fm.top + fm.descent).toInt() + paddingTop
        width = widthSize
        height = heightSize
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (paintbg != null) canvas.drawPaint(paintbg!!)
        if (paintsq != null) for (j in Math.floor(-width * 6.0 / height).toInt() until Math.ceil(width * 6.0 / height).toInt()) for (i in 0..11) if ((i + j) % 2 == 0) canvas.drawRect((width / 2 + height * j / 12).toFloat(), (height * i / 12).toFloat(), (width / 2 + height * (j + 1) / 12).toFloat(), (height * (i + 1) / 12).toFloat(), paintsq!!)
        if (paintln != null) {
            canvas.drawLine(0f, ascent.toFloat(), width.toFloat(), ascent.toFloat(), paintln!!)
            canvas.drawLine(0f, offsety.toFloat(), width.toFloat(), offsety.toFloat(), paintln!!)
            canvas.drawLine(0f, descent.toFloat(), width.toFloat(), descent.toFloat(), paintln!!)
            if ((Build.VERSION.SDK_INT < 23 || paint!!.hasGlyph(str)) && validChar || !drawSlash) {
                canvas.drawLine(offsetx.toFloat(), 0f, offsetx.toFloat(), height.toFloat(), paintln!!)
                canvas.drawLine((width - offsetx).toFloat(), 0f, (width - offsetx).toFloat(), height.toFloat(), paintln!!)
            }
        }
        if (str!!.length > 0) {
            if (span != null) {
                if (invalid) cache
                if (emojicache == null) span!!.draw(canvas, str, 0, str!!.length, offsetx.toFloat(), ascent, offsety, descent, paint!!) else canvas.drawBitmap(emojicache!!, Rect(0, 0, emojicache!!.width, emojicache!!.height), RectF(offsetx.toFloat(), paddingTop.toFloat(), (width - offsetx + paddingLeft - paddingRight).toFloat(), (height - paddingBottom).toFloat()), paint)
            } else if ((Build.VERSION.SDK_INT < 23 || paint!!.hasGlyph(str)) && validChar || !drawSlash) {
                if (invalid) cache
                if (emojicache == null) canvas.drawText(str!!, offsetx.toFloat(), offsety.toFloat(), paint!!) else canvas.drawBitmap(emojicache!!, Rect(0, 0, emojicache!!.width, emojicache!!.height), RectF(offsetx.toFloat(), paddingTop.toFloat(), (width - offsetx + paddingLeft - paddingRight).toFloat(), (height - paddingBottom).toFloat()), paint)
            } else {
                val sz = (descent - ascent).toFloat()
                canvas.drawLine(width / 2 - sz * .4f, ascent + sz * .1f, width / 2 - sz * .4f, ascent + sz * .9f, paint!!)
                canvas.drawLine(width / 2 - sz * .4f, ascent + sz * .1f, width / 2 + sz * .4f, ascent + sz * .1f, paint!!)
                canvas.drawLine(width / 2 + sz * .4f, ascent + sz * .1f, width / 2 + sz * .4f, ascent + sz * .9f, paint!!)
                canvas.drawLine(width / 2 - sz * .4f, ascent + sz * .9f, width / 2 + sz * .4f, ascent + sz * .9f, paint!!)
                if (!(Build.VERSION.SDK_INT < 23 || paint!!.hasGlyph(str))) canvas.drawLine(width / 2 - sz * .4f, ascent + sz * .1f, width / 2 + sz * .4f, ascent + sz * .9f, paint!!)
                if (!validChar) canvas.drawLine(width / 2 + sz * .4f, ascent + sz * .1f, width / 2 - sz * .4f, ascent + sz * .9f, paint!!)
            }
        }
    }

    private val cache: Unit
        private get() {
            emojicache = null
            if (span != null || (Build.VERSION.SDK_INT < 23 || paint!!.hasGlyph(str)) && validChar && paint!!.measureText(str) > 0f) {
                var w = paint!!.measureText(str)
                if (span != null && Build.VERSION.SDK_INT >= 19) {
                    val fmi = paint!!.fontMetricsInt
                    w = span!!.getSize(paint!!, str, 0, str!!.length, fmi).toFloat()
                }
                if (w > 256f) {
                    val size_ = paint!!.textSize
                    paint!!.textSize = size_ * 256f / w
                    val fm = paint!!.fontMetrics
                    var w2 = paint!!.measureText(str).toInt()
                    if (span != null && Build.VERSION.SDK_INT >= 19) {
                        val fmi = paint!!.fontMetricsInt
                        w2 = span!!.getSize(paint!!, str, 0, str!!.length, fmi)
                    }
                    val bm = Bitmap.createBitmap(w2, (-fm.top + fm.bottom).toInt(), Bitmap.Config.ARGB_8888)
                    val tr = Bitmap.createBitmap(w2, (-fm.top + fm.bottom).toInt(), Bitmap.Config.ARGB_8888)
                    val cv = Canvas(bm)
                    if (span != null && Build.VERSION.SDK_INT >= 19) span!!.draw(cv, str, 0, str!!.length, 0f, 0, (-fm.top).toInt(), (-fm.top + fm.descent).toInt(), paint!!) else cv.drawText(str!!, 0f, -fm.top, paint!!)
                    if (!bm.sameAs(tr)) {
                        cv.drawColor(0, PorterDuff.Mode.CLEAR)
                        paint!!.style = Paint.Style.STROKE
                        if (span != null && Build.VERSION.SDK_INT >= 19) span!!.draw(cv, str, 0, str!!.length, 0f, 0, (-fm.top).toInt(), (-fm.top + fm.descent).toInt(), paint!!) else cv.drawText(str!!, 0f, -fm.top, paint!!)
                        if (bm.sameAs(tr)) {
                            paint!!.style = Paint.Style.FILL
                            if (span != null && Build.VERSION.SDK_INT >= 19) span!!.draw(cv, str, 0, str!!.length, 0f, 0, (-fm.top).toInt(), (-fm.top + fm.descent).toInt(), paint!!) else cv.drawText(str!!, 0f, -fm.top, paint!!)
                            emojicache = bm
                        } else paint!!.style = Paint.Style.FILL
                    }
                    paint!!.textSize = size_
                }
            }
            invalid = false
        }

    init {
        if (paint == null) {
            paint = Paint()
            val a = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
            paint.setColor(a.getColor(0, Color.RED))
            a.recycle()
            paint.setStyle(Paint.Style.FILL)
            paint.setTypeface(Typeface.DEFAULT)
            paint.setAntiAlias(true)
            paint.setFilterBitmap(true)
        }
        str = ""
        offsetx = 0
        offsety = 0
        width = 0
        height = 0
        shrinkWidth = false
        shrinkHeight = false
        validChar = true
        drawSlash = true
        emojicache = null
        invalid = true
    }
}