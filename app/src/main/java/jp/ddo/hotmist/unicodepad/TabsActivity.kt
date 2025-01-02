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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.woxthebox.draglistview.DragListView

class TabsActivity : BaseActivity() {
    class DynamicDragListView(context: android.content.Context?, attrs: android.util.AttributeSet?) : DragListView(context, attrs) {
        init {
            super.onFinishInflate()
        }

        @SuppressLint("MissingSuperCall")
        override fun onFinishInflate() {
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@TabsActivity).apply {
                setText(R.string.tabs_hint)
                setPadding((8 * this@TabsActivity.resources.displayMetrics.density).toInt())
            })
            val view = DynamicDragListView(this@TabsActivity, null)
            val adapter = TabsAdapter(this@TabsActivity)
            view.setLayoutManager(LinearLayoutManager(this@TabsActivity))
            view.setDragListListener(adapter)
            view.setAdapter(adapter, true)
            view.setCanDragHorizontally(false)
            view.setCanDragVertically(true)
            addView(view)
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
