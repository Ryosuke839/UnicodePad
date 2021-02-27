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

import android.graphics.*
import android.util.TypedValue
import android.view.*
import android.view.View.MeasureSpec
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import java.nio.charset.Charset
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal class CharacterAdapter(private val activity: UnicodeActivity, private val adapter: UnicodeAdapter, private val tf: Typeface?, private val db: NameDatabase, private val afav: FavoriteAdapter) : PagerAdapter() {
    var index = 0
        private set
    private val reslist = TypedValue().also {
        activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
    }.resourceId
    override fun getCount(): Int {
        return adapter.count
    }

    override fun getPageTitle(position: Int): CharSequence {
        return (if (adapter.getItemId(position) != -1L) String.format("U+%04X ", adapter.getItemId(position)) else adapter.getItemString(position) + " ") + adapter.getItem(position)
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val text = CharacterView(activity)
        text.text = adapter.getItem(position)
        text.setTextSize(fontsize)
        text.setTypeface(tf)
        text.drawSlash(false)
        text.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        text.setTextColor(Color.BLACK)
        text.setBackgroundColor(Color.WHITE)
        text.setSquareAlpha(min(max(checker * 2.55f, 0f), 255f).toInt())
        text.drawLines(lines)
        text.shrinkWidth(shrink)
        text.shrinkHeight(true)
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(text, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val itemid = adapter.getItemId(position).toInt()
        val emoji = itemid == -1
        val ver = if (!emoji) db.getInt(itemid, "version") else db.getInt(adapter.getItemString(position), "version")
        text.setValid(ver != 0 && ver <= UnicodeActivity.univer)
        val str = StringBuilder()
        if (!emoji) str.append(adapter.getItem(position))
        for (i in 0 until if (!emoji) 10 else 7) {
            if (emoji && i == 5) continue
            if (i == 2) {
                val v = if (!emoji) db.getInt(itemid, cols[i]) else db.getInt(adapter.getItemString(position), emjs[i])
                val desc = TextView(activity)
                desc.text = if (!emoji) mods[i].toString() + String.format(Locale.US, "%d.%d.%d", v / 100, v / 10 % 10, v % 10) + (if (v == 600) " or earlier" else "") else mode[i].toString() + String.format(Locale.US, "%d.%d", v / 100, v / 10 % 10)
                desc.gravity = Gravity.CENTER_VERTICAL
                layout.addView(desc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                continue
            }
            val r: String? = if (i == 1) {
                val a = (adapter.getItem(position)).toByteArray(Charset.defaultCharset())
                val sb = StringBuilder(a.size * 3)
                for (b in a) sb.append(String.format("%02X ", b))
                sb.deleteCharAt(a.size * 3 - 1)
                sb.toString()
            } else {
                if (!emoji) db[itemid, cols[i]] else db[adapter.getItemString(position), emjs[i]]
            }
            if (r == null && i == 0) {
                val desc = TextView(activity)
                desc.setText(R.string.notacharacter)
                layout.addView(desc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                break
            }
            if (r == null) continue
            val l = r.split(if (emoji && i == 6) " " else "\n").toTypedArray()
            for (s in l) {
                if (i == 0) {
                    val desc = TextView(activity, null, android.R.attr.textAppearanceMedium)
                    desc.text = s
                    desc.setTextIsSelectable(true)
                    desc.gravity = Gravity.CENTER_VERTICAL
                    if (!emoji) {
                        val fav = CheckBox(activity)
                        fav.setButtonDrawable(android.R.drawable.btn_star)
                        fav.gravity = Gravity.TOP
                        fav.isChecked = afav.isFavorite(itemid)
                        fav.setOnCheckedChangeListener { _, b -> if (b) afav.add(itemid) else afav.rem(itemid) }
                        val hl = LinearLayout(activity)
                        hl.orientation = LinearLayout.HORIZONTAL
                        hl.addView(desc, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                        hl.addView(fav, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        layout.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    } else layout.addView(desc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    continue
                }
                val hl = LinearLayout(activity)
                hl.orientation = LinearLayout.HORIZONTAL
                val it = TextView(activity)
                it.gravity = Gravity.CENTER_VERTICAL
                it.text = (if (!emoji) mods else mode)[i]
                hl.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
                if (i < 6) {
                    val desc = TextView(activity)
                    desc.text = s
                    desc.setTextIsSelectable(true)
                    hl.addView(desc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                } else {
                    var cs = ""
                    var ps = ""
                    var ns: String? = null
                    Scanner(s).use { sc ->
                        var j = 0
                        while (sc.hasNext()) {
                            if (i == 9 && j == 0 && s[0] == '<') {
                                ns = sc.next()
                                ++j
                                continue
                            }
                            val tgt = sc.nextInt(16)
                            cs += String(Character.toChars(tgt))
                            ps += String.format("U+%04X ", tgt)
                            if (i == 6) {
                                val n = db[tgt, "name"]
                                ns = n ?: "<not a character>"
                                break
                            }
                            if (i == 7 && j == 1) {
                                sc.useDelimiter("\n")
                                sc.skip(" ")
                                ns = if (sc.hasNext()) sc.next() else ""
                                break
                            }
                            ++j
                        }
                    }
                    if (ps.isEmpty()) continue
                    ps = ps.substring(0, ps.length - 1)
                    val ct = CharacterView(activity, null, android.R.attr.textAppearanceLarge)
                    ct.setPadding(0, 0, 0, 0)
                    ct.setPadding(UnicodeAdapter.padding, UnicodeAdapter.padding, UnicodeAdapter.padding, UnicodeAdapter.padding)
                    ct.drawSlash(false)
                    ct.setTextSize(UnicodeAdapter.fontsize)
                    ct.text = cs
                    ct.setTypeface(tf)
                    hl.addView(ct, LinearLayout.LayoutParams((activity.resources.displayMetrics.scaledDensity * UnicodeAdapter.fontsize * 2 + UnicodeAdapter.padding * 2).toInt(), ViewGroup.LayoutParams.MATCH_PARENT))
                    val pt = TextView(activity, null, android.R.attr.textAppearanceSmall)
                    pt.setPadding(0, 0, 0, 0)
                    pt.gravity = Gravity.CENTER_VERTICAL
                    pt.text = ps
                    if (ns != null) {
                        val nt = TextView(activity, null, android.R.attr.textAppearanceSmall)
                        nt.setPadding(0, 0, 0, 0)
                        nt.gravity = Gravity.CENTER_VERTICAL
                        nt.text = ns
                        val vl = LinearLayout(activity)
                        vl.orientation = LinearLayout.VERTICAL
                        vl.addView(pt, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        vl.addView(nt, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        hl.addView(vl, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                    } else hl.addView(pt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                    hl.id = 0x3F000000 + str.codePointCount(0, str.length)
                    str.append(cs)
                    hl.isEnabled = true
                    hl.isClickable = true
                    hl.isFocusable = true
                    hl.setOnClickListener { view ->
                        var j = 0
                        while (j < str.length) {
                            val code = str.codePointAt(i)
                            activity.adpPage.onItemClick(null, view, -1, code.toLong())
                            j += Character.charCount(code)
                        }
                    }
                    hl.setOnLongClickListener { view ->
                        activity.adpPage.showDesc(view, view.id - 0x3F000000, StringAdapter(str.toString(), activity, db))
                        true
                    }
                    hl.setBackgroundResource(reslist)
                }
                layout.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
        val scroll = ScrollView(activity)
        scroll.addView(layout)
        collection.addView(scroll)
        collection.findViewById<View>(R.id.TAB_ID).measure(MeasureSpec.makeMeasureSpec(10, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        layout.setPadding(0, 0, 0, collection.findViewById<View>(R.id.TAB_ID).measuredHeight * 2)
        return scroll
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    override fun isViewFromObject(arg0: View, arg1: Any): Boolean {
        return arg0 === arg1
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        index = position
    }

    val id: Long
        get() = adapter.getItemId(index)

    companion object {
        var fontsize = 160f
        var checker = 10f
        var lines = true
        var shrink = true
        private val cols = arrayOf("name", "utf8", "version", "comment", "alias", "formal", "xref", "vari", "decomp", "compat")
        private val mods = arrayOf(null, "UTF-8: ", "from Unicode ", "\u2022 ", "= ", "\u203B ", "\u2192 ", "~ ", "\u2261 ", "\u2248 ")
        private val emjs = arrayOf("name", "utf8", "version", "grp", "subgrp", "", "id")
        private val mode = arrayOf(null, "UTF-8: ", "from Unicode Emoji ", "Group: ", "Subgroup: ", null, "")
    }
}