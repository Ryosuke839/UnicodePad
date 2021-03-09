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

internal class EmojiAdapter(activity: Activity, pref: SharedPreferences, private val db: NameDatabase, single: Boolean) : UnicodeAdapter(activity, db, single) {
    private var cur: Cursor? = null
    private var jump: Spinner? = null
    private lateinit var map: NavigableMap<Int, Int>
    private lateinit var grp: MutableList<String>
    private lateinit var idx: MutableList<Int>
    private var current = pref.getInt("emoji", 0)
    private var modifier = pref.getBoolean("modifier", true)
    private var guard = 0
    private val scrollListener = object : AbsListView.OnScrollListener {
        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            var index = firstVisibleItem
            if (view.getChildAt(0) != null && view.getChildAt(0).top * -2 > view.getChildAt(0).height) index += if (single) 1 else PageAdapter.column
            if (!single) index += PageAdapter.column - 1
            if (visibleItemCount != 0) {
                map.floorEntry(index)?.let { e ->
                    jump?.let { jump ->
                        if (guard == 0 && current != e.value) {
                            current = e.value
                            ++guard
                            jump.setSelection(e.value, false)
                            jump.post { --guard }
                        }
                    }
                }
            }
        }
    }
    private val selectListener = object : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            current = position
            if (guard == 0) this@EmojiAdapter.view?.setSelection(idx[position])
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }
    override fun name(): Int {
        return R.string.emoji
    }

    @SuppressLint("InlinedApi")
    override fun instantiate(view: AbsListView): View {
        super.instantiate(view)
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        val hl = LinearLayout(activity)
        hl.orientation = LinearLayout.HORIZONTAL
        hl.gravity = Gravity.CENTER
        val jump = Spinner(activity)
        this.jump = jump
        hl.addView(jump, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val modc = CheckBox(activity)
        modc.setText(R.string.modifier)
        modc.setPadding(0, 0, (activity.resources.displayMetrics.density * 8f).toInt(), 0)
        hl.addView(modc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (Build.VERSION.SDK_INT >= 21) hl.setPadding(0, (activity.resources.displayMetrics.density * 8f).toInt(), 0, (activity.resources.displayMetrics.density * 8f).toInt())
        layout.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(this.view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        cur = db.emoji(UnicodeActivity.univer, modifier)
        map = TreeMap()
        grp = ArrayList()
        idx = ArrayList()
        var last = ""
        cur?.let {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val curr = it.getString(1) + " / " + it.getString(2)
                if (curr == last) {
                    it.moveToNext()
                    continue
                }
                last = curr
                map[it.position] = map.size
                grp.add(curr)
                idx.add(it.position)
                it.moveToNext()
            }
            if (current >= grp.size) current = grp.size - 1
        }
        jump.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, grp).also {
            it.setDropDownViewResource(R.layout.spinner_drop_down_item)
        }
        view.setSelection(idx[current])
        jump.setSelection(current)
        view.setOnScrollListener(scrollListener)
        jump.onItemSelectedListener = selectListener
        modc.isChecked = modifier
        modc.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (modifier == isChecked) return@setOnCheckedChangeListener
            modifier = isChecked
            ++guard
            view.setOnScrollListener(null)
            jump.onItemSelectedListener = null
            jump.adapter = null
            cur?.close()
            cur = db.emoji(UnicodeActivity.univer, modifier)
            map = TreeMap()
            grp = ArrayList()
            idx = ArrayList()
            var last2 = ""
            cur?.let {
                it.moveToFirst()
                while (!it.isAfterLast) {
                    val curr = it.getString(1) + " / " + it.getString(2)
                    if (curr == last2) {
                        it.moveToNext()
                        continue
                    }
                    last2 = curr
                    map[it.position] = map.size
                    grp.add(curr)
                    idx.add(it.position)
                    it.moveToNext()
                }
            }
            if (current >= grp.size) current = grp.size - 1
            view.invalidateViews()
            jump.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, grp).also {
                it.setDropDownViewResource(R.layout.spinner_drop_down_item)
            }
            jump.setSelection(current)
            view.setSelection(idx[current])
            view.setOnScrollListener(scrollListener)
            jump.onItemSelectedListener = selectListener
            view.post { --guard }
        }
        return layout
    }

    override fun destroy() {
        view?.setOnScrollListener(null)
        jump = null
        cur?.close()
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
        return cur?.count ?: 0
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
        return getItemString(i).split(" ").joinToString("") { String(Character.toChars(it.toInt(16))) }
    }
}