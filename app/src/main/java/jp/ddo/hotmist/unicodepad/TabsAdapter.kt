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
import android.content.SharedPreferences
import android.graphics.Typeface
import android.util.TypedValue
import androidx.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.mobeta.android.dslv.DragSortListView.DropListener
import java.util.*
import kotlin.math.max

class TabsAdapter internal constructor(private val activity: Activity, private val list: AbsListView?) : BaseAdapter(), View.OnClickListener, DropListener {
    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    private val idx: ArrayList<Int> = ArrayList(NUM_TABS + 2)
    private val single: ArrayList<Boolean> = ArrayList(NUM_TABS)
    private var shownNum = pref.getInt("cnt_shown", NUM_TABS)
    override fun getCount(): Int {
        return NUM_TABS + 2
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return 0
        return if (position == shownNum + 1) 0 else 1
    }

    override fun getItem(i: Int): Any {
        return Any()
    }

    override fun getItemId(i: Int): Long {
        return idx[i].toLong()
    }

    @SuppressLint("InflateParams")
    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        return if (getItemViewType(i) == 0) {
            (view as TextView? ?: TextView(activity).also {
                it.setTextColor(TypedValue().also { tv ->
                    activity.theme.resolveAttribute(android.R.attr.textColorLink, tv, true)
                }.data)
                it.typeface = Typeface.DEFAULT_BOLD
                (activity.resources.displayMetrics.density * 16).toInt().let { padding ->
                    it.setPadding(padding, padding / 2, padding, padding / 2)
                }
            }).also {
                it.setText(if (i == 0) R.string.shown_desc else R.string.hidden_desc)
            }
        } else {
            (view ?: activity.layoutInflater.inflate(R.layout.spinwidget, null).also {
                it.findViewById<RadioButton>(R.id.tabs_multiple).setOnClickListener(this)
                it.findViewById<RadioButton>(R.id.tabs_single).setOnClickListener(this)
            }).also {
                it.findViewById<TextView>(R.id.tabs_title).setText(RESOURCES[idx[i]])
                it.findViewById<RadioButton>(R.id.tabs_multiple).let { rb ->
                    rb.isChecked = !single[idx[i]]
                    rb.tag = idx[i]
                }
                it.findViewById<RadioButton>(R.id.tabs_single).let { rb ->
                    rb.isChecked = single[idx[i]]
                    rb.tag = idx[i]
                }
            }
        }
    }

    override fun onClick(view: View) {
        val i = view.tag as Int
        single[i] = view.id == R.id.tabs_single
        val edit = pref.edit()
        edit.putString("single_" + KEYS[i], java.lang.Boolean.valueOf(single[i]).toString())
        edit.apply()
    }

    override fun drop(from: Int, to: Int) {
        if (idx[from] == 1 && to > shownNum) {
            Toast.makeText(activity, R.string.list_title, Toast.LENGTH_SHORT).show()
            return
        }
        if (from < shownNum + 1) --shownNum
        if (shownNum == 0) {
            ++shownNum
            return
        }
        if (to <= shownNum + 1) ++shownNum
        idx.add(max(to, 1), idx.removeAt(from))
        val edit = pref.edit()
        edit.putInt("cnt_shown", shownNum)
        for (i in 1 until NUM_TABS + 2) {
            var ord = i - 1
            if (ord == shownNum) continue
            if (ord > shownNum) --ord
            edit.putInt("ord_" + KEYS[idx[i]], ord)
        }
        edit.apply()
        list?.invalidateViews()
    }

    init {
        for (i in 0 until NUM_TABS + 2) idx.add(-2)
        for (i in 0 until NUM_TABS) single.add(false)
        idx[0] = -1
        idx[shownNum + 1] = -1
        for (i in 0 until NUM_TABS) {
            var ord = pref.getInt("ord_" + KEYS[i], i) + 1
            if (ord > shownNum) ++ord
            if (idx[ord] == -2) idx[ord] = i else for (j in 0 until NUM_TABS + 2) if (idx[j] == -2) {
                idx[j] = i
                break
            }
            single[i] = pref.getString("single_" + KEYS[i], null)?.toBoolean() ?: DEFAULTS[i]
        }
    }

    companion object {
        private const val NUM_TABS = 6
        private val KEYS = arrayOf("rec", "list", "emoji", "find", "fav", "edt")
        private val RESOURCES = intArrayOf(R.string.recent, R.string.list, R.string.emoji, R.string.find, R.string.favorite, R.string.edit)
        private val DEFAULTS = booleanArrayOf(false, false, false, true, false, true)
    }
}