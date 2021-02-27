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
import android.database.Cursor
import android.os.Build
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import java.util.*

internal class EmojiAdapter(activity: Activity, pref: SharedPreferences, private val db: NameDatabase, single: Boolean) : UnicodeAdapter(activity, db, single), OnItemSelectedListener, AbsListView.OnScrollListener, CompoundButton.OnCheckedChangeListener {
    private var cur: Cursor? = null
    private var layout: LinearLayout? = null
    private var jump: Spinner? = null
    private lateinit var map: NavigableMap<Int, Int>
    private lateinit var grp: MutableList<String>
    private lateinit var idx: MutableList<Int>
    private var current: Int
    private var modifier: Boolean
    override fun name(): Int {
        return R.string.emoji
    }

    @SuppressLint("InlinedApi")
    override fun instantiate(view: AbsListView?): View? {
        super.instantiate(view)
        layout = LinearLayout(activity)
        layout!!.orientation = LinearLayout.VERTICAL
        val hl = LinearLayout(activity)
        hl.orientation = LinearLayout.HORIZONTAL
        hl.gravity = Gravity.CENTER
        jump = Spinner(activity)
        hl.addView(jump, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val modc = CheckBox(activity)
        modc.setText(R.string.modifier)
        modc.setPadding(0, 0, (activity.resources.displayMetrics.density * 8f).toInt(), 0)
        hl.addView(modc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (Build.VERSION.SDK_INT >= 21) hl.setPadding(0, (activity.resources.displayMetrics.density * 8f).toInt(), 0, (activity.resources.displayMetrics.density * 8f).toInt())
        layout!!.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout!!.addView(this.view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        cur = db.emoji(UnicodeActivity.univer, modifier)
        map = TreeMap()
        grp = ArrayList()
        idx = ArrayList()
        var last = ""
        cur!!.moveToFirst()
        while (!cur!!.isAfterLast) {
            val curr = cur!!.getString(1) + " / " + cur!!.getString(2)
            if (curr == last) {
                cur!!.moveToNext()
                continue
            }
            last = curr
            map[cur!!.position] = map.size
            grp.add(curr)
            idx.add(cur!!.position)
            cur!!.moveToNext()
        }
        if (current >= grp.size) current = grp.size - 1
        val adp = ArrayAdapter(activity, android.R.layout.simple_spinner_item, grp)
        adp.setDropDownViewResource(R.layout.spinner_drop_down_item)
        jump!!.adapter = adp
        this.view!!.setSelection(idx[current])
        jump!!.setSelection(current)
        this.view!!.setOnScrollListener(this)
        jump!!.onItemSelectedListener = this
        modc.isChecked = modifier
        modc.setOnCheckedChangeListener(this)
        return layout
    }

    override fun destroy() {
        view!!.setOnScrollListener(null)
        layout = null
        jump = null
        if (cur != null) cur!!.close()
        cur = null
        super.destroy()
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        return super.getView(i, view, viewGroup).also {
            ((if (single) (it as LinearLayout).getChildAt(1) else it) as CharacterView).drawSlash(false)
        }
    }

    override fun save(edit: SharedPreferences.Editor) {
        edit.putInt("emoji", current)
        edit.putBoolean("modifier", modifier)
    }

    override fun getCount(): Int {
        return if (cur != null) cur!!.count else 0
    }

    override fun getItemId(arg0: Int): Long {
        return -1
    }

    override fun getItemString(i: Int): String {
        return cur?.let {
            if (i < 0 || i >= it.count) return ""
            it.moveToPosition(i)
            it.getString(0)
        } ?: ""
    }

    override fun getItem(i: Int): String {
        val ss = getItemString(i).split(" ").toTypedArray()
        var res = ""
        for (s in ss) if (s.isNotEmpty()) res += String(Character.toChars(s.toInt(16)))
        return res
    }

    private var guard = 0
    override fun onItemSelected(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {
        if (arg0 === jump) {
            current = arg2
            if (view != null) if (guard == 0) view!!.setSelection(idx[arg2])
        }
    }

    override fun onNothingSelected(arg0: AdapterView<*>?) {}
    override fun onScroll(arg0: AbsListView, arg1: Int, arg2: Int, arg3: Int) {
        var arg1 = arg1
        if (arg0 === view) {
            if (view!!.getChildAt(0) != null && view!!.getChildAt(0).top * -2 > view!!.getChildAt(0).height) arg1 += if (single) 1 else PageAdapter.column
            if (!single) arg1 += PageAdapter.column - 1
            val e = map.floorEntry(arg1)
            if (arg2 != 0) {
                if (e != null) {
                    if (jump != null) {
                        if (guard == 0 && current != e.value) {
                            current = e.value
                            ++guard
                            jump!!.setSelection(e.value, false)
                            jump!!.post { --guard }
                        }
                    }
                }
            }
        }
    }

    override fun onScrollStateChanged(arg0: AbsListView, arg1: Int) {}
    override fun onCheckedChanged(arg0: CompoundButton, arg1: Boolean) {
        if (modifier == arg1) return
        modifier = arg1
        ++guard
        view!!.setOnScrollListener(null)
        jump!!.onItemSelectedListener = null
        jump!!.adapter = null
        if (cur != null) cur!!.close()
        cur = db.emoji(UnicodeActivity.univer, modifier)
        map = TreeMap()
        grp = ArrayList()
        idx = ArrayList()
        var last = ""
        cur!!.moveToFirst()
        while (!cur!!.isAfterLast) {
            val curr = cur!!.getString(1) + " / " + cur!!.getString(2)
            if (curr == last) {
                cur!!.moveToNext()
                continue
            }
            last = curr
            map[cur!!.position] = map.size
            grp.add(curr)
            idx.add(cur!!.position)
            cur!!.moveToNext()
        }
        if (current >= grp.size) current = grp.size - 1
        view!!.invalidateViews()
        val adp = ArrayAdapter(activity, android.R.layout.simple_spinner_item, grp)
        adp.setDropDownViewResource(R.layout.spinner_drop_down_item)
        jump!!.adapter = adp
        jump!!.setSelection(current)
        view!!.setSelection(idx[current])
        view!!.setOnScrollListener(this)
        jump!!.onItemSelectedListener = this
        view!!.post { --guard }
    }

    init {
        current = pref.getInt("emoji", 0)
        modifier = pref.getBoolean("modifier", true)
    }
}