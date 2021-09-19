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

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.TypedValue
import androidx.preference.PreferenceManager
import com.mobeta.android.dslv.DragSortController
import com.mobeta.android.dslv.DragSortListView

class TabsActivity : Activity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(THEME[(PreferenceManager.getDefaultSharedPreferences(this).getString("theme", null)?.toIntOrNull() ?: 2131492983) - 2131492983])
        super.onCreate(savedInstanceState)
        val view = DragSortListView(this, null)
        val controller = DragSortController(view, R.id.HANDLE_ID, DragSortController.ON_DRAG, DragSortController.FLING_REMOVE)
        controller.isSortEnabled = true
        controller.setBackgroundColor(TypedValue().also { tv ->
            theme.resolveAttribute(android.R.attr.windowBackground, tv, true)
        }.data)
        view.setFloatViewManager(controller)
        view.setOnTouchListener(controller)
        view.adapter = TabsAdapter(this, view)
        setContentView(view)
    }

    companion object {
        private val THEME = intArrayOf(
                R.style.Theme,
                R.style.Theme_Light,
                R.style.Theme_Light_DarkActionBar)
    }
}