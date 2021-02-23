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
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.RadioButton
import android.widget.TextView
import com.mobeta.android.dslv.DragSortListView.DropListener
import java.util.*

class TabsAdapter internal constructor(private val activity: Activity, private val list: AbsListView?) : BaseAdapter(), View.OnClickListener, DropListener {
    private val NUM_TABS = 6
    private var shownnum: Int
    private val idx: ArrayList<Int>
    private val single: ArrayList<Boolean>
    private val pref: SharedPreferences
    private val KEYS = arrayOf("rec", "list", "emoji", "find", "fav", "edt")
    private val RESS = intArrayOf(R.string.recent, R.string.list, R.string.emoji, R.string.find, R.string.favorite, R.string.edit)
    private val DEFS = booleanArrayOf(false, false, false, true, false, true)
    override fun getCount(): Int {
        return NUM_TABS + 2
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return 0
        return if (position == shownnum + 1) 0 else 1
    }

    override fun getItem(i: Int): Any {
        return null
    }

    override fun getItemId(i: Int): Long {
        return idx[i].toLong()
    }

    override fun getView(i: Int, view: View, viewGroup: ViewGroup): View {
        var view = view
        if (getItemViewType(i) == 0) {
            if (view == null) {
                view = TextView(activity)
            }
            (view as TextView).setText(if (i == 0) R.string.shown_desc else R.string.hidden_desc)
        } else {
            if (view == null) {
                view = activity.layoutInflater.inflate(R.layout.spinwidget, null)
                view.findViewById<View>(R.id.tabs_multiple).setOnClickListener(this)
                view.findViewById<View>(R.id.tabs_single).setOnClickListener(this)
            }
            (view.findViewById<View>(R.id.tabs_title) as TextView).setText(RESS[idx[i]])
            (view.findViewById<View>(R.id.tabs_multiple) as RadioButton).isChecked = !single[idx[i]]
            view.findViewById<View>(R.id.tabs_multiple).tag = idx[i]
            (view.findViewById<View>(R.id.tabs_single) as RadioButton).isChecked = single[idx[i]]
            view.findViewById<View>(R.id.tabs_single).tag = idx[i]
        }
        return view
    }

    override fun onClick(view: View) {
        val i = view.tag as Int
        single[i] = view.id == R.id.tabs_single
        val edit = pref.edit()
        edit.putString("single_" + KEYS[i], java.lang.Boolean.valueOf(single[i]).toString())
        edit.apply()
    }

    override fun drop(from: Int, to: Int) {
        if (to == 0) return
        if (from < shownnum + 1) --shownnum
        if (shownnum == 0) {
            ++shownnum
            return
        }
        if (to <= shownnum + 1) ++shownnum
        idx.add(to, idx.removeAt(from))
        val edit = pref.edit()
        edit.putInt("cnt_shown", shownnum)
        for (i in 1 until NUM_TABS + 2) {
            var ord = i - 1
            if (ord == shownnum) continue
            if (ord > shownnum) --ord
            edit.putInt("ord_" + KEYS[idx[i]], ord)
        }
        edit.apply()
        list?.invalidateViews()
    }

    init {
        pref = PreferenceManager.getDefaultSharedPreferences(activity)
        shownnum = pref.getInt("cnt_shown", NUM_TABS)
        idx = ArrayList(NUM_TABS + 2)
        for (i in 0 until NUM_TABS + 2) idx.add(-2)
        single = ArrayList(NUM_TABS)
        for (i in 0 until NUM_TABS) single.add(false)
        idx[0] = -1
        idx[shownnum + 1] = -1
        for (i in 0 until NUM_TABS) {
            var ord = pref.getInt("ord_" + KEYS[i], i) + 1
            if (ord > shownnum) ++ord
            if (idx[ord] == -2) idx[ord] = i else for (j in 0 until NUM_TABS + 2) if (idx[j] == -2) {
                idx[j] = i
                break
            }
            single[i] = java.lang.Boolean.valueOf(pref.getString("single_" + KEYS[i], java.lang.Boolean.valueOf(DEFS[i]).toString()))
        }
    }
}