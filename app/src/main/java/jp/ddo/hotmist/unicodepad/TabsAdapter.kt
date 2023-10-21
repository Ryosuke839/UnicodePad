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
import android.content.SharedPreferences
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import androidx.preference.PreferenceManager
import com.woxthebox.draglistview.DragItemAdapter
import com.woxthebox.draglistview.DragListView
import java.util.*
import kotlin.math.max


class TabsAdapter internal constructor(private val activity: Activity) : DragItemAdapter<Int, TabsAdapter.ViewHolder>(), OnClickListener, DragListView.DragListListener {
    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    private val idx: ArrayList<Int> = ArrayList(NUM_TABS + 2)
    private val single: ArrayList<Boolean> = ArrayList(NUM_TABS)
    private var shownNum = pref.getInt("cnt_shown", NUM_TABS)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) {
            HeaderViewHolder(activity.layoutInflater.inflate(R.layout.spinwidget_title, parent, false))
        } else {
            ItemViewHolder(activity.layoutInflater.inflate(R.layout.spinwidget, parent, false).also {
                it.findViewById<RadioButton>(R.id.tabs_multiple).setOnClickListener(this)
                it.findViewById<RadioButton>(R.id.tabs_single).setOnClickListener(this)
            })
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder is HeaderViewHolder) {
            holder.title.setText(if (position == 0) R.string.shown_desc else R.string.hidden_desc)
        } else if (holder is ItemViewHolder) {
            val i = idx[position]
            holder.title.setText(RESOURCES[i])
            holder.multiple.isChecked = !single[i]
            holder.multiple.tag = i
            holder.single.isChecked = single[i]
            holder.single.tag = i
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (idx[position] < 0) 0 else 1
    }

    override fun getUniqueItemId(position: Int): Long {
        return idx[position].toLong()
    }

    override fun onClick(view: View) {
        val i = view.tag as Int
        single[i] = view.id == R.id.tabs_single
        val edit = pref.edit()
        edit.putString("single_" + KEYS[i], java.lang.Boolean.valueOf(single[i]).toString())
        edit.apply()
    }

    override fun onItemDragStarted(position: Int) {
    }

    override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {
    }

    override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
        if (idx[toPosition] == 1 && toPosition > shownNum || idx[0] != -2) {
            idx.add(max(fromPosition, 1), idx.removeAt(toPosition))
            notifyItemRangeChanged(fromPosition, toPosition - fromPosition + 1)
            if (idx[0] == -2) {
                Toast.makeText(activity, R.string.list_title, Toast.LENGTH_SHORT).show()
            }
            return
        }
        shownNum = idx.indexOf(-3) - 1
        val edit = pref.edit()
        edit.putInt("cnt_shown", shownNum)
        for (i in 1 until NUM_TABS + 2) {
            var ord = i - 1
            if (ord == shownNum) continue
            if (ord > shownNum) --ord
            edit.putInt("ord_" + KEYS[idx[i]], ord)
        }
        edit.apply()
    }

    abstract class ViewHolder(itemView: View) : DragItemAdapter.ViewHolder(itemView, R.id.HANDLE_ID, false)

    class HeaderViewHolder(itemView: View) : ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tabs_title)
    }

    class ItemViewHolder(itemView: View) : ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tabs_title)
        val multiple: RadioButton = itemView.findViewById(R.id.tabs_multiple)
        val single: RadioButton = itemView.findViewById(R.id.tabs_single)
    }

    init {
        for (i in 0 until NUM_TABS + 2) idx.add(-2)
        for (i in 0 until NUM_TABS) single.add(false)
        idx[0] = -2
        idx[shownNum + 1] = -3
        for (i in 0 until NUM_TABS) {
            var ord = pref.getInt("ord_" + KEYS[i], i) + 1
            if (ord > shownNum) ++ord
            if (idx[ord] == -2) idx[ord] = i else for (j in 0 until NUM_TABS + 2) if (idx[j] == -2) {
                idx[j] = i
                break
            }
            single[i] = pref.getString("single_" + KEYS[i], null)?.toBoolean() ?: DEFAULTS[i]
        }
        mItemList = idx
    }

    companion object {
        private const val NUM_TABS = 6
        private val KEYS = arrayOf("rec", "list", "emoji", "find", "fav", "edt")
        private val RESOURCES = intArrayOf(R.string.recent, R.string.list, R.string.emoji, R.string.find, R.string.favorite, R.string.edit)
        private val DEFAULTS = booleanArrayOf(false, false, false, true, false, true)
    }
}