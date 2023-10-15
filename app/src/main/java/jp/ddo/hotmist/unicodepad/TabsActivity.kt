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
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.woxthebox.draglistview.DragListView

class TabsActivity : AppCompatActivity() {
    class DynamicDragListView(context: android.content.Context?, attrs: android.util.AttributeSet?) : DragListView(context, attrs) {
        init {
            super.onFinishInflate()
        }

        @SuppressLint("MissingSuperCall")
        override fun onFinishInflate() {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(THEME[(PreferenceManager.getDefaultSharedPreferences(this).getString("theme", null)?.toIntOrNull() ?: 2131492983) - 2131492983])
        super.onCreate(savedInstanceState)
        val view = DynamicDragListView(this, null)
        val adapter = TabsAdapter(this)
        view.setLayoutManager(LinearLayoutManager(this))
        view.setDragListListener(adapter)
        view.setAdapter(adapter, true)
        view.setCanDragHorizontally(false)
        view.setCanDragVertically(true)
        setContentView(view)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private val THEME = intArrayOf(
                R.style.Theme,
                R.style.Theme_Light,
                R.style.Theme_Light_DarkActionBar)
    }
}