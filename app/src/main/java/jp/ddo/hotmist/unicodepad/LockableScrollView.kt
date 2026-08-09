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
import android.util.AttributeSet
import android.view.*
import android.widget.ScrollView
import androidx.core.view.isVisible

class LockableScrollView : ScrollView {
    private lateinit var adapter: PageAdapter
    private var lockView: View? = null
    private var over = false

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!over) return false

        if (isTouchOnLockedContent(ev)) {
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                super.onInterceptTouchEvent(ev) // reset internal state only
            }
            return false
        }

        return super.onInterceptTouchEvent(ev)
    }

    private fun isTouchOnLockedContent(ev: MotionEvent): Boolean {
        if (!::adapter.isInitialized) return false
        val target = adapter.view?.takeIf { it.isVisible } ?: return false
        val rc = Rect()
        target.getGlobalVisibleRect(rc)
        return rc.contains(ev.rawX.toInt(), ev.rawY.toInt())
    }

    fun setAdapter(adapter: PageAdapter) {
        this.adapter = adapter
    }

    fun setLockView(lockView: View?, over: Boolean) {
        this.lockView = lockView
        this.over = over
        if (lockView == null || height == 0) return
        if (!over) scrollTo(0, 0)
        lockView.layoutParams.height = if (over) height else height - lockView.top
        lockView.requestLayout()
        adapter.onSizeChanged(if (over) lockView.top else 0)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pos = scrollY
        post {
            lockView?.let {
                it.layoutParams.height = if (over) h else h - it.top
                it.requestLayout()
                adapter.onSizeChanged(if (over) it.top else 0)
                it.post { scrollTo(0, pos) }
            }
        }
    }

    private var scroll = false
    override fun requestChildFocus(child: View, focused: View) {
        scroll = false
        super.requestChildFocus(child, focused)
        scroll = true
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        scroll = false
        super.onLayout(changed, l, t, r, b)
        scroll = true
    }

    override fun scrollTo(x: Int, y: Int) {
        if (scroll) super.scrollTo(x, y)
    }

    interface OnScrollListener {
        fun onScroll(remainingY: Int)
    }

    private var scrollListener: OnScrollListener? = null

    fun setOnScrollListener(listener: OnScrollListener) {
        scrollListener = listener
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val remainingY = (getChildAt(0)?.height ?: 0) - (height + scrollY)
        scrollListener?.onScroll(remainingY)
    }
}
