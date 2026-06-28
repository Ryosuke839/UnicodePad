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

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import com.woxthebox.draglistview.DragItemAdapter
import com.woxthebox.draglistview.DragListView
import java.io.File


@RequiresApi(Build.VERSION_CODES.Q)
class FontFallbackAdapter internal constructor(private val activity: Activity, private val fontFallback: FontData.FallbackFont) : DragItemAdapter<String, FontFallbackAdapter.ItemViewHolder>(), OnClickListener, DragListView.DragListListener {
    init {
        mItemList = fontFallback.paths
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(activity.layoutInflater.inflate(R.layout.fontfallback, parent, false)).apply {
            button.setOnClickListener(this@FontFallbackAdapter)
        }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.apply {
            fontFallback.paths[position].let {
                title.text = File(it).name
                button.tag = position
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getUniqueItemId(position: Int): Long {
        return fontFallback.paths[position].hashCode().toLong()
    }

    override fun onClick(view: View) {
        val i = view.tag as Int
        fontFallback.paths.removeAt(i)
        notifyItemRemoved(i)
    }

    override fun onItemDragStarted(position: Int) {
    }

    override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {
    }

    override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
    }

    class ItemViewHolder(itemView: View) : DragItemAdapter.ViewHolder(itemView, R.id.HANDLE_ID, false) {
        val title: TextView = itemView.findViewById(R.id.font_title)
        val button: ImageButton = itemView.findViewById(R.id.font_menu)
    }
}