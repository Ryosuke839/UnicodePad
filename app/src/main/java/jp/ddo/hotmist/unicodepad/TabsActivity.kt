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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar)
            addView(TextView(this@TabsActivity).apply {
                setText(R.string.tabs_hint)
                setPadding((8 * this@TabsActivity.resources.displayMetrics.density).toInt())
            })
            addView(DynamicDragListView(this@TabsActivity, null).apply {
                setLayoutManager(LinearLayoutManager(this@TabsActivity))
                TabsAdapter(this@TabsActivity).let { adapter ->
                    setDragListListener(adapter)
                    setAdapter(adapter, true)
                }
                setCanDragHorizontally(false)
                setCanDragVertically(true)
                recyclerView.clipToPadding = false
                ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.updatePadding(0, 0, 0, insets.bottom)
                    WindowInsetsCompat.CONSUMED
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

            ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updatePadding(insets.left, 0, insets.right)
                windowInsets
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
