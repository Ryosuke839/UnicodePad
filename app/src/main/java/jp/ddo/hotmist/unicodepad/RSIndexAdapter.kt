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
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

internal class RSIndexAdapter(activity: Activity, pref: SharedPreferences, private val db: NameDatabase, single: Boolean) : RecyclerUnicodeAdapter(activity, db, single) {
    private var cur: Cursor? = null
    private var codepoints: MutableList<Long>? = null
    private var jump: Spinner? = null
    private lateinit var map: NavigableMap<Int, Int>
    private lateinit var group: MutableList<String>
    private lateinit var groupIndex: MutableList<Int>
    private lateinit var subGroup: MutableList<Pair<String, Int>>
    private var current = pref.getInt("rsindex", 0)
    private var guard = 0
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val manager = recyclerView.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
            var index = manager.findFirstVisibleItemPosition()
            val visibleItemCount = recyclerView.childCount
            index -= searchTitlePosition(index)
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
            if (guard == 0) this@RSIndexAdapter.scrollToTitle(groupIndex[position])
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }
    override fun name(): Int {
        return R.string.rsindex
    }

    init {
        setHasStableIds(true)
    }

    @SuppressLint("InlinedApi")
    override suspend fun instantiate(view: View): View {
        super.instantiate(view)
        val view = view as RecyclerView
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        val hl = LinearLayout(activity)
        hl.orientation = LinearLayout.HORIZONTAL
        hl.gravity = Gravity.CENTER
        val jump = Spinner(activity)
        this.jump = jump
        hl.addView(jump, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        hl.setPadding(0, (activity.resources.displayMetrics.density * 8f).toInt(), 0, (activity.resources.displayMetrics.density * 8f).toInt())
        layout.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(this.view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        initViews()
        return layout
    }

    private suspend fun initViews() {
        val view = (this.view as RecyclerView)
        val jump = this.jump!!
        view.setOnScrollListener(null)
        jump.onItemSelectedListener = null
        jump.adapter = null
        withContext(Dispatchers.IO) {
            cur?.close()
            cur = db.rsindex()
            map = TreeMap()
            group = ArrayList()
            groupIndex = ArrayList()
            subGroup = ArrayList()
            var last = 0 to 0
            cur?.let {
                it.moveToFirst()
                val codepoints = ArrayList<Long>(it.count)
                while (!it.isAfterLast) {
                    codepoints.add(it.getLong(3))
                    val curr = it.getInt(1) to it.getInt(2)
                    if (curr == last) {
                        it.moveToNext()
                        continue
                    }
                    val cp = curr.first - 1 + 0x2F00
                    if (curr.first != last.first) {
                        map[it.position] = map.size
                        group.add(activity.resources.getString(R.string.rsindex_group, curr.first, db[cp, "name"]?.substring(15)?.lowercase(), String(Character.toChars(cp))))
                        groupIndex.add(subGroup.size)
                    }
                    last = curr
                    subGroup.add(activity.resources.getString(R.string.rsindex_subgroup, curr.first, db[cp, "name"]?.substring(15)?.lowercase(), String(Character.toChars(cp)), curr.second) to it.position)
                    it.moveToNext()
                }
                this@RSIndexAdapter.codepoints = codepoints
            }
            if (current >= group.size) current = group.size - 1
        }
        invalidateViews()
        jump.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, group).also {
            it.setDropDownViewResource(R.layout.spinner_drop_down_item)
        }
        scrollToTitle(current)
        jump.setSelection(current)
        view.setOnScrollListener(scrollListener)
        jump.onItemSelectedListener = selectListener
    }

    override fun destroy() {
        jump = null
        codepoints = null
        cur?.close()
        cur = null
        super.destroy()
    }

    override fun getTitleCount(): Int {
        return subGroup.size
    }

    override fun getTitlePosition(i: Int): Int {
        return subGroup[i].second
    }

    override fun getTitleString(i: Int): String {
        return subGroup[i].first
    }

    override fun save(edit: SharedPreferences.Editor) {
        edit.putInt("rsindex", current)
    }

    override fun getCount(): Int {
        return codepoints?.size ?: 0
    }

    override fun getItemTextColumn(i: Int): String {
        return "kDefinition"
    }

    override fun getItemCodePoint(i: Int): Long {
        return codepoints?.getOrNull(i) ?: -1
    }
}