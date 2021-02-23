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
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.viewpager.widget.ViewPager

class LockableViewPager : ViewPager {
    private var slop: Int
    private var x = 0f
    private var y = 0f

    constructor(context: Context?) : super(context!!) {
        slop = ViewConfiguration.get(context).scaledTouchSlop
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        slop = ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        try {
            val inmove = super.onInterceptTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.x
                    y = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = Math.abs(event.x - x)
                    val dy = Math.abs(event.y - y)
                    if (dx > slop && dx * .5 > dy) // no need of lock anymore?
                    //return true;
                        break
                }
            }
            return inmove
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }
}