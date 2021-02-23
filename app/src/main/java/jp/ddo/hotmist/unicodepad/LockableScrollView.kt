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

class LockableScrollView : ScrollView {
    private var inmove = false
    private var adapter: PageAdapter? = null
    private var lockview: View? = null
    private var over = false

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!over) return false
        if (inmove && ev.actionMasked == MotionEvent.ACTION_UP) {
            inmove = false
            return true
        }
        var hit = false
        val v = adapter.getView()
        if (v != null && v.visibility == VISIBLE) {
            val rc = Rect()
            v.getGlobalVisibleRect(rc)
            if (rc.contains(ev.rawX.toInt(), ev.rawY.toInt())) hit = true
        }
        if (!inmove && !hit) return super.onInterceptTouchEvent(ev).also { inmove = it }
        inmove = super.onInterceptTouchEvent(ev)
        return false
    }

    fun setAdapter(adapter: PageAdapter?) {
        this.adapter = adapter
    }

    fun setLockView(lockview: View?, over: Boolean) {
        this.lockview = lockview
        this.over = over
        if (lockview == null || height == 0) return
        if (!over) scrollTo(0, 0)
        lockview.layoutParams.height = if (over) height else height - lockview.top
        lockview.requestLayout()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pos = scrollY
        post(Runnable {
            if (lockview == null) return@Runnable
            lockview!!.layoutParams.height = if (over) h else h - lockview!!.top
            lockview!!.requestLayout()
            lockview!!.post { scrollTo(0, pos) }
        })
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
}