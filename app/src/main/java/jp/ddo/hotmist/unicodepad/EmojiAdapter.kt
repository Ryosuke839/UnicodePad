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
import androidx.recyclerview.widget.RecyclerView
import java.util.*

internal class EmojiAdapter(activity: Activity, pref: SharedPreferences, private val db: NameDatabase, single: Boolean) : RecyclerUnicodeAdapter(activity, db, single) {
    private var cur: Cursor? = null
    private var jump: Spinner? = null
    private lateinit var map: NavigableMap<Int, Int>
    private lateinit var grp: MutableList<String>
    private lateinit var idx: MutableList<Int>
    private var current = pref.getInt("emoji", 0)
    private var tone = pref.getInt("tone", 0x2B1A)
    private var direction = pref.getInt("emoji_direction", 0x2B05)
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
            if (guard == 0) this@EmojiAdapter.scrollToTitle(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }
    override fun name(): Int {
        return R.string.emoji
    }

    init {
        setHasStableIds(true)
    }

    @SuppressLint("InlinedApi")
    override fun instantiate(view: View): View {
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
        hl.addView(Spinner(activity).apply {
            val adp = ArrayAdapter(activity, android.R.layout.simple_spinner_item, listOf("⬚", "\uD83C\uDFFB", "\uD83C\uDFFC", "\uD83C\uDFFD", "\uD83C\uDFFE", "\uD83C\uDFFF")).apply {
                setDropDownViewResource(R.layout.spinner_drop_down_item)
            }
            adapter = adp
            setSelection(adp.getPosition(Character.toChars(tone).concatToString()))
            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                    val item = adp.getItem(position)!!.codePointAt(0)
                    if (tone == item) return
                    tone = item
                    ++guard
                    initViews()
                    view.post { --guard }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            } }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        hl.addView(Spinner(activity).apply {
            val adp = ArrayAdapter(activity, android.R.layout.simple_spinner_item, listOf("\u2B05\uFE0F", "\u27A1\uFE0F")).apply {
                setDropDownViewResource(R.layout.spinner_drop_down_item)
            }
            adapter = adp
            setSelection(adp.getPosition(Character.toChars(direction).concatToString() + "\uFE0F"))
            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                    val item = adp.getItem(position)!!.codePointAt(0)
                    if (direction == item) return
                    direction = item
                    ++guard
                    initViews()
                    view.post { --guard }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            } }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        hl.setPadding(0, (activity.resources.displayMetrics.density * 8f).toInt(), 0, (activity.resources.displayMetrics.density * 8f).toInt())
        layout.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(this.view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        initViews()
        return layout
    }

    private fun initViews() {
        val view = (this.view as RecyclerView)
        val jump = this.jump!!
        view.setOnScrollListener(null)
        jump.onItemSelectedListener = null
        jump.adapter = null
        cur?.close()
        cur = db.emoji(UnicodeActivity.univer, tone, direction)
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
        }
        if (current >= grp.size) current = grp.size - 1
        invalidateViews()
        jump.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, grp).also {
            it.setDropDownViewResource(R.layout.spinner_drop_down_item)
        }
        scrollToTitle(current)
        jump.setSelection(current)
        view.setOnScrollListener(scrollListener)
        jump.onItemSelectedListener = selectListener
    }

    override fun destroy() {
        jump = null
        cur?.close()
        cur = null
        super.destroy()
    }

    override fun getTitleCount(): Int {
        return grp.size
    }

    override fun getTitlePosition(i: Int): Int {
        return idx[i]
    }

    override fun getTitleString(i: Int): String {
        return grp[i]
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder is UnicodeAdapter.CharacterViewHolder) holder.characterView.drawSlash(false)
    }

    override fun save(edit: SharedPreferences.Editor) {
        edit.putInt("emoji", current)
        edit.putInt("tone", tone)
        edit.putInt("emoji_direction", direction)
    }

    override fun getCount(): Int {
        return cur?.count ?: 0
    }

    override fun getItemCodePoint(i: Int): Long {
        return cur?.let {
            if (i < 0 || i >= it.count) return -1
            it.moveToPosition(i)
            it.getLong(3) - 0x800000000000000L
        } ?: -1
    }

    override fun getItemString(i: Int): String {
        return cur?.let {
            if (i < 0 || i >= it.count) return ""
            it.moveToPosition(i)
            it.getString(0)
        } ?: ""
    }

    override fun getItem(i: Int): String {
        return getItemString(i).let {
            if (it != "")
                it.split(" ").joinToString("") { joined -> String(Character.toChars(joined.toInt(16))) }
            else
                ""
        }
    }
}