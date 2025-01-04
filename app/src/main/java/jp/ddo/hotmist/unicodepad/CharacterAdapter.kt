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
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.viewpager.widget.PagerAdapter
import java.nio.charset.Charset
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal class CharacterAdapter(private val activity: UnicodeActivity, private val adapter: UnicodeAdapter, private var typeface: Typeface?, private var locale: Locale, private val db: NameDatabase, private val afav: FavoriteAdapter) : PagerAdapter() {
    var index = 0
        private set
    private val reslist = TypedValue().also {
        activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
    }.resourceId
    override fun getCount(): Int {
        return adapter.getCount()
    }

    override fun getPageTitle(position: Int): CharSequence {
        return (if (adapter.getItemCodePoint(position) >= 0) String.format("U+%04X ", adapter.getItemCodePoint(position)) else adapter.getItemString(position) + " ") + adapter.getItem(position)
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val text = CharacterView(activity)
        text.text = adapter.getItem(position)
        text.setTextSize(fontsize)
        text.setTypeface(typeface, locale)
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
        val itemid = adapter.getItemCodePoint(position).toInt()
        val emoji = adapter.getItemCodePoint(position) < 0
        val ver = if (!emoji) db.getInt(itemid, "version") else db.getInt(adapter.getItemString(position), "version")
        text.setValid(ver != 0 && ver <= UnicodeActivity.univer)
        val str = StringBuilder()
        if (!emoji) str.append(adapter.getItem(position))
        val textPadding = (6 * activity.resources.displayMetrics.scaledDensity).toInt()
        for (i in 0 until if (!emoji) 4 else 5) {
            if (i == 2) {
                val v = if (!emoji) db.getInt(itemid, cols[i]) else db.getInt(adapter.getItemString(position), emjs[i])
                val desc = TextView(activity)
                desc.text = if (!emoji) mods[i].toString() + String.format(Locale.US, "%d.%d.%d", v / 100, v / 10 % 10, v % 10) + (if (v == 600) " or earlier" else "") else mode[i].toString() + String.format(Locale.US, "%d.%d", v / 100, v / 10 % 10)
                desc.gravity = Gravity.CENTER_VERTICAL
                desc.setPadding(textPadding, 0, textPadding, 0)
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
                desc.setPadding(textPadding, 0, textPadding, 0)
                layout.addView(desc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                break
            }
            if (r == null) continue
            val l = r.split("\n").toTypedArray()
            for (s in l) {
                if (i == 0) {
                    layout.addView(LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        addView(TextView(activity, null, android.R.attr.textAppearanceMedium).apply {
                            this.text = if (adapter.getItemCodePoint(position) >= 0) String.format("U+%04X", adapter.getItemCodePoint(position)) else adapter.getItemString(position)
                            setTextIsSelectable(true)
                            gravity = Gravity.CENTER_VERTICAL
                            maxWidth = activity.resources.displayMetrics.widthPixels / 2 - (ResourcesCompat.getDrawable(activity.resources, android.R.drawable.btn_star, null)?.intrinsicWidth ?: 0)
                        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        addView(TextView(activity, null, android.R.attr.textAppearanceMedium).apply {
                            this.text = ": "
                            gravity = Gravity.CENTER_VERTICAL
                        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        addView(TextView(activity, null, android.R.attr.textAppearanceMedium).apply {
                            this.text = s
                            setTextIsSelectable(true)
                            gravity = Gravity.CENTER_VERTICAL
                        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                        if (!emoji) {
                            addView(CheckBox(activity).apply {
                                setButtonDrawable(android.R.drawable.btn_star)
                                gravity = Gravity.TOP
                                isChecked = afav.isFavorite(itemid)
                                setOnCheckedChangeListener { _, b -> if (b) afav.add(itemid) else afav.rem(itemid) }
                            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        }
                        setPadding(textPadding, 0, textPadding, 0)
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    continue
                }
                val hl = LinearLayout(activity)
                hl.orientation = LinearLayout.HORIZONTAL
                val it = TextView(activity)
                it.gravity = Gravity.CENTER_VERTICAL
                if (!emoji && i == 3) {
                    val charMap = mapOf(
                        "*" to "\u2022 ",
                        "=" to "= ",
                        "%" to "\u203B ",
                        "x" to "\u2192 ",
                        "~" to "~ ",
                        ":" to "\u2261 ",
                        "#" to "\u2248 ",
                        "@" to "\u2022 ",
                    )
                    it.text = charMap[s.substring(0, 1)] ?: s.substring(0, 1)
                    hl.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    if (s.startsWith("*") || s.startsWith("=") || s.startsWith("%") || s.startsWith("@")) {
                        it.gravity = Gravity.TOP
                        val desc = TextView(activity)
                        desc.text = s.substring(2)
                        desc.setTextIsSelectable(true)
                        hl.addView(desc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                    } else {
                        var cs = ""
                        var ps = ""
                        val ns = mutableListOf<String>()
                        Scanner(s.substring(2)).use { sc ->
                            while (sc.hasNext()) {
                                val ss = sc.next()
                                if (Regex("[0-9A-Fa-f]{4,6}").matches(ss)) {
                                    val tgt = Integer.parseInt(ss, 16)
                                    cs += String(Character.toChars(tgt))
                                    ps += String.format("U+%04X ", tgt)
                                } else {
                                    ns.add(ss)
                                }
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
                        ct.setTypeface(typeface, locale)
                        hl.addView(ct, LinearLayout.LayoutParams((activity.resources.displayMetrics.scaledDensity * UnicodeAdapter.fontsize * 2 + UnicodeAdapter.padding * 2).toInt(), ViewGroup.LayoutParams.MATCH_PARENT))
                        val pt = TextView(activity, null, android.R.attr.textAppearanceSmall)
                        pt.setPadding(0, 0, 0, 0)
                        pt.gravity = Gravity.CENTER_VERTICAL
                        pt.text = ps
                        if (ns.isNotEmpty()) {
                            val nt = TextView(activity, null, android.R.attr.textAppearanceSmall)
                            nt.setPadding(0, 0, 0, 0)
                            nt.gravity = Gravity.CENTER_VERTICAL
                            nt.text = ns.joinToString(" ")
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
                            while (j < cs.length) {
                                val code = cs.codePointAt(j)
                                activity.adpPage.onItemClick(null, view, -1, code.toLong())
                                j += Character.charCount(code)
                            }
                        }
                        hl.setOnLongClickListener { view ->
                            activity.adpPage.showDesc(null, view.id - 0x3F000000, StringAdapter(str.toString(), activity, db))
                            true
                        }
                        hl.setBackgroundResource(reslist)
                    }
                } else {
                    it.text = (if (!emoji) mods else mode)[i]
                    hl.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    val desc = TextView(activity)
                    desc.text = s
                    desc.setTextIsSelectable(true)
                    hl.addView(desc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                }
                hl.setPadding(textPadding, 0, textPadding, 0)
                layout.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
        val scroll = NestedScrollView(activity)
        scroll.addView(layout)
        collection.addView(scroll)
        collection.findViewById<View>(R.id.TAB_ID).measure(MeasureSpec.makeMeasureSpec(10, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        scroll.setTag(R.id.TAB_ID, position)
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
        get() = adapter.getItemCodePoint(index)

    fun getItemId(position: Int): Long {
        return adapter.getItemCodePoint(position)
    }

    fun setTypeface(typeface: Typeface?, locale: Locale) {
        this.typeface = typeface
        this.locale = locale
    }

    companion object {
        var fontsize = 160f
        var checker = 10f
        var lines = true
        var shrink = true
        private val cols = arrayOf("name", "utf8", "version", "lines")
        private val mods = arrayOf(null, "UTF-8: ", "from Unicode ", "")
        private val emjs = arrayOf("name", "utf8", "version", "grp", "subgrp")
        private val mode = arrayOf(null, "UTF-8: ", "from Unicode Emoji ", "Group: ", "Subgroup: ")
    }
}