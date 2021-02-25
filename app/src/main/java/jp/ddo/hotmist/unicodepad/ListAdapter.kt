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
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.database.DataSetObserver
import android.graphics.*
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Pair
import android.util.SparseArray
import android.util.TypedValue
import android.view.*
import android.view.View.OnTouchListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.content.res.ResourcesCompat
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal class ListAdapter(activity: Activity, pref: SharedPreferences, db: NameDatabase, single: Boolean) : UnicodeAdapter(activity, db, single), OnItemSelectedListener, AbsListView.OnScrollListener, View.OnClickListener, OnTouchListener {
    private var count = 0
    private val emap: NavigableMap<Int, Pair<Int, Int>> = TreeMap()
    private val fmap: NavigableMap<Int, Pair<Int, Int>> = TreeMap()
    private val imap: NavigableMap<Int, Int> = TreeMap()
    private val jmap: MutableList<Int> = ArrayList()
    private val mmap: NavigableMap<Int, String> = TreeMap()
    private var layout: LinearLayout? = null
    private var jump: Spinner? = null
    private var mark: Spinner? = null
    private var code: Button? = null
    private var current: Int
    private var head: Int
    private var scroll: Int
    private var resnormal = 0
    private var resselect = 0
    private var highlight: Int
    private var hightarget: View?

    private inner class MapAdapter(var context: Context) : SpinnerAdapter {
        override fun getCount(): Int {
            return if (mmap.size == 0) 1 else mmap.size + 2
        }

        override fun getItem(arg0: Int): Any {
            var arg0 = arg0
            when (arg0) {
                0 -> return context.resources.getString(R.string.mark)
                1 -> return context.resources.getString(R.string.rem)
                else -> for ((key, value) in mmap) if (--arg0 == 1) return String.format("U+%04X %s", key, value)
            }
            return ""
        }

        override fun getItemId(arg0: Int): Long {
            return arg0.toLong()
        }

        override fun getItemViewType(arg0: Int): Int {
            return 0
        }

        override fun getView(arg0: Int, arg1: View?, arg2: ViewGroup): View {
            var arg1 = arg1
            if (arg1 == null) arg1 = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(android.R.layout.simple_spinner_item, arg2, false)
            (arg1 as TextView).text = getItem(arg0) as String
            arg1.setTextColor(0x00000000)
            return arg1
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun isEmpty(): Boolean {
            return false
        }

        override fun registerDataSetObserver(arg0: DataSetObserver) {}
        override fun unregisterDataSetObserver(arg0: DataSetObserver) {}
        override fun getDropDownView(arg0: Int, arg1: View, arg2: ViewGroup): View {
            var arg1 = arg1
            if (arg1 == null) arg1 = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(if (arg0 == 0) R.layout.spinner_drop_down_void else R.layout.spinner_drop_down_item, arg2, false)
            (arg1 as TextView).text = getItem(arg0) as String
            return arg1
        }
    }

    override fun name(): Int {
        return R.string.list
    }

    @SuppressLint("SetTextI18n")
    override fun instantiate(grd: AbsListView?): View? {
        super.instantiate(grd)
        emap.clear()
        fmap.clear()
        imap.clear()
        jmap.clear()
        count = 0
        val univer: Int = UnicodeActivity.univer
        add(0x0, 0x7F)
        add(0x80, 0xFF)
        add(0x100, 0x17F)
        add(0x180, 0x24F)
        add(0x250, 0x2AF)
        add(0x2B0, 0x2FF)
        add(0x300, 0x36F)
        add(0x370, 0x3FF)
        add(0x400, 0x4FF)
        add(0x500, 0x52F)
        add(0x530, 0x58F)
        add(0x590, 0x5FF)
        add(0x600, 0x6FF)
        add(0x700, 0x74F)
        add(0x750, 0x77F)
        add(0x780, 0x7BF)
        add(0x7C0, 0x7FF)
        add(0x800, 0x83F)
        add(0x840, 0x85F)
        if (univer >= 1000) add(0x860, 0x86F)
        if (univer >= 610) add(0x8A0, 0x8FF)
        add(0x900, 0x97F)
        add(0x980, 0x9FF)
        add(0xA00, 0xA7F)
        add(0xA80, 0xAFF)
        add(0xB00, 0xB7F)
        add(0xB80, 0xBFF)
        add(0xC00, 0xC7F)
        add(0xC80, 0xCFF)
        add(0xD00, 0xD7F)
        add(0xD80, 0xDFF)
        add(0xE00, 0xE7F)
        add(0xE80, 0xEFF)
        add(0xF00, 0xFFF)
        add(0x1000, 0x109F)
        add(0x10A0, 0x10FF)
        add(0x1100, 0x11FF)
        add(0x1200, 0x137F)
        add(0x1380, 0x139F)
        add(0x13A0, 0x13FF)
        add(0x1400, 0x167F)
        add(0x1680, 0x169F)
        add(0x16A0, 0x16FF)
        add(0x1700, 0x171F)
        add(0x1720, 0x173F)
        add(0x1740, 0x175F)
        add(0x1760, 0x177F)
        add(0x1780, 0x17FF)
        add(0x1800, 0x18AF)
        add(0x18B0, 0x18FF)
        add(0x1900, 0x194F)
        add(0x1950, 0x197F)
        add(0x1980, 0x19DF)
        add(0x19E0, 0x19FF)
        add(0x1A00, 0x1A1F)
        add(0x1A20, 0x1AAF)
        if (univer >= 700) add(0x1AB0, 0x1AFF)
        add(0x1B00, 0x1B7F)
        add(0x1B80, 0x1BBF)
        add(0x1BC0, 0x1BFF)
        add(0x1C00, 0x1C4F)
        add(0x1C50, 0x1C7F)
        if (univer >= 900) add(0x1C80, 0x1C8F)
        if (univer >= 610) add(0x1CC0, 0x1CCF)
        if (univer >= 1100) add(0x1C90, 0x1CBF)
        add(0x1CD0, 0x1CFF)
        add(0x1D00, 0x1D7F)
        add(0x1D80, 0x1DBF)
        add(0x1DC0, 0x1DFF)
        add(0x1E00, 0x1EFF)
        add(0x1F00, 0x1FFF)
        add(0x2000, 0x206F)
        add(0x2070, 0x209F)
        add(0x20A0, 0x20CF)
        add(0x20D0, 0x20FF)
        add(0x2100, 0x214F)
        add(0x2150, 0x218F)
        add(0x2190, 0x21FF)
        add(0x2200, 0x22FF)
        add(0x2300, 0x23FF)
        add(0x2400, 0x243F)
        add(0x2440, 0x245F)
        add(0x2460, 0x24FF)
        add(0x2500, 0x257F)
        add(0x2580, 0x259F)
        add(0x25A0, 0x25FF)
        add(0x2600, 0x26FF)
        add(0x2700, 0x27BF)
        add(0x27C0, 0x27EF)
        add(0x27F0, 0x27FF)
        add(0x2800, 0x28FF)
        add(0x2900, 0x297F)
        add(0x2980, 0x29FF)
        add(0x2A00, 0x2AFF)
        add(0x2B00, 0x2BFF)
        add(0x2C00, 0x2C5F)
        add(0x2C60, 0x2C7F)
        add(0x2C80, 0x2CFF)
        add(0x2D00, 0x2D2F)
        add(0x2D30, 0x2D7F)
        add(0x2D80, 0x2DDF)
        add(0x2DE0, 0x2DFF)
        add(0x2E00, 0x2E7F)
        add(0x2E80, 0x2EFF)
        add(0x2F00, 0x2FDF)
        add(0x2FF0, 0x2FFF)
        add(0x3000, 0x303F)
        add(0x3040, 0x309F)
        add(0x30A0, 0x30FF)
        add(0x3100, 0x312F)
        add(0x3130, 0x318F)
        add(0x3190, 0x319F)
        add(0x31A0, 0x31BF)
        add(0x31C0, 0x31EF)
        add(0x31F0, 0x31FF)
        add(0x3200, 0x32FF)
        add(0x3300, 0x33FF)
        add(0x3400, 0x4DBF)
        add(0x4DC0, 0x4DFF)
        add(0x4E00, 0x9FFF)
        add(0xA000, 0xA48F)
        add(0xA490, 0xA4CF)
        add(0xA4D0, 0xA4FF)
        add(0xA500, 0xA63F)
        add(0xA640, 0xA69F)
        add(0xA6A0, 0xA6FF)
        add(0xA700, 0xA71F)
        add(0xA720, 0xA7FF)
        add(0xA800, 0xA82F)
        add(0xA830, 0xA83F)
        add(0xA840, 0xA87F)
        add(0xA880, 0xA8DF)
        add(0xA8E0, 0xA8FF)
        add(0xA900, 0xA92F)
        add(0xA930, 0xA95F)
        add(0xA960, 0xA97F)
        add(0xA980, 0xA9DF)
        if (univer >= 700) add(0xA9E0, 0xA9FF)
        add(0xAA00, 0xAA5F)
        add(0xAA60, 0xAA7F)
        add(0xAA80, 0xAADF)
        if (univer >= 610) add(0xAAE0, 0xAAFF)
        add(0xAB00, 0xAB2F)
        if (univer >= 700) add(0xAB30, 0xAB6F)
        if (univer >= 800) add(0xAB70, 0xABBF)
        add(0xABC0, 0xABFF)
        add(0xAC00, 0xD7AF)
        add(0xD7B0, 0xD7FF)
        add(0xE000, 0xF8FF)
        add(0xF900, 0xFAFF)
        add(0xFB00, 0xFB4F)
        add(0xFB50, 0xFDFF)
        add(0xFE00, 0xFE0F)
        add(0xFE10, 0xFE1F)
        add(0xFE20, 0xFE2F)
        add(0xFE30, 0xFE4F)
        add(0xFE50, 0xFE6F)
        add(0xFE70, 0xFEFF)
        add(0xFF00, 0xFFEF)
        add(0xFFF0, 0xFFFF)
        add(0x10000, 0x1007F)
        add(0x10080, 0x100FF)
        add(0x10100, 0x1013F)
        add(0x10140, 0x1018F)
        add(0x10190, 0x101CF)
        add(0x101D0, 0x101FF)
        add(0x10280, 0x1029F)
        add(0x102A0, 0x102DF)
        if (univer >= 700) add(0x102E0, 0x102FF)
        add(0x10300, 0x1032F)
        add(0x10330, 0x1034F)
        if (univer >= 700) add(0x10350, 0x1037F)
        add(0x10380, 0x1039F)
        add(0x103A0, 0x103DF)
        add(0x10400, 0x1044F)
        add(0x10450, 0x1047F)
        add(0x10480, 0x104AF)
        if (univer >= 900) add(0x104B0, 0x104FF)
        if (univer >= 700) {
            add(0x10500, 0x1052F)
            add(0x10530, 0x1056F)
            add(0x10600, 0x1077F)
        }
        add(0x10800, 0x1083F)
        add(0x10840, 0x1085F)
        if (univer >= 700) add(0x10860, 0x1087F)
        if (univer >= 700) add(0x10880, 0x108AF)
        if (univer >= 800) add(0x108E0, 0x108FF)
        add(0x10900, 0x1091F)
        add(0x10920, 0x1093F)
        if (univer >= 610) add(0x10980, 0x1099F)
        if (univer >= 610) add(0x109A0, 0x109FF)
        add(0x10A00, 0x10A5F)
        add(0x10A60, 0x10A7F)
        if (univer >= 700) add(0x10A80, 0x10A9F)
        if (univer >= 700) add(0x10AC0, 0x10AFF)
        add(0x10B00, 0x10B3F)
        add(0x10B40, 0x10B5F)
        add(0x10B60, 0x10B7F)
        if (univer >= 700) add(0x10B80, 0x10BAF)
        add(0x10C00, 0x10C4F)
        add(0x10C80, 0x10CFF)
        if (univer >= 1100) add(0x10D00, 0x10D3F)
        add(0x10E60, 0x10E7F)
        if (univer >= 1300) add(0x10E80, 0x10EBF)
        if (univer >= 1100) {
            add(0x10F00, 0x10F2F)
            add(0x10F30, 0x10F6F)
        }
        if (univer >= 1300) add(0x10FB0, 0x10FDF)
        add(0x11000, 0x1107F)
        add(0x11080, 0x110CF)
        if (univer >= 610) {
            add(0x110D0, 0x110FF)
            add(0x11100, 0x1114F)
            if (univer >= 700) add(0x11150, 0x1117F)
            add(0x11180, 0x111DF)
            if (univer >= 700) {
                add(0x111E0, 0x111FF)
                add(0x11200, 0x1124F)
                if (univer >= 800) add(0x11280, 0x112AF)
                add(0x112B0, 0x112FF)
                add(0x11300, 0x1137F)
                if (univer >= 900) add(0x11400, 0x1147F)
                add(0x11480, 0x114DF)
                add(0x11580, 0x115FF)
                add(0x11600, 0x1165F)
                if (univer >= 900) add(0x11660, 0x1167F)
            }
            add(0x11680, 0x116CF)
            if (univer >= 800) add(0x11700, 0x1173F)
            if (univer >= 1100) add(0x11800, 0x1184F)
            if (univer >= 700) add(0x118A0, 0x118FF)
            if (univer >= 1300) add(0x11900, 0x1195F)
            if (univer >= 1200) add(0x119A0, 0x119FF)
            if (univer >= 1000) add(0x11A00, 0x11A4F)
            if (univer >= 1000) add(0x11A50, 0x11AAF)
            if (univer >= 700) add(0x11AC0, 0x11AFF)
            if (univer >= 900) add(0x11C00, 0x11C6F)
            if (univer >= 900) add(0x11C70, 0x11CBF)
            if (univer >= 1000) add(0x11D00, 0x11D5F)
            if (univer >= 1100) {
                add(0x11D60, 0x11DAF)
                add(0x11EE0, 0x11EFF)
            }
            if (univer >= 1300) add(0x11FB0, 0x11FBF)
            if (univer >= 1200) add(0x11FC0, 0x11FFF)
        }
        add(0x12000, 0x123FF)
        add(0x12400, 0x1247F)
        if (univer >= 800) add(0x12480, 0x1254F)
        add(0x13000, 0x1342F)
        if (univer >= 1200) add(0x13430, 0x1343F)
        if (univer >= 800) add(0x14400, 0x1467F)
        add(0x16800, 0x16A3F)
        if (univer >= 700) {
            add(0x16A40, 0x16A6F)
            add(0x16AD0, 0x16AFF)
            add(0x16B00, 0x16B8F)
        }
        if (univer >= 1100) add(0x16E40, 0x16E9F)
        if (univer >= 610) add(0x16F00, 0x16F9F)
        if (univer >= 900) {
            add(0x16FE0, 0x16FFF)
            add(0x17000, 0x187FF)
            add(0x18800, 0x18AFF)
        }
        if (univer >= 1300) {
            add(0x18B00, 0x18CFF)
            add(0x18D00, 0x18D8F)
        }
        add(0x1B000, 0x1B0FF)
        if (univer >= 1000) add(0x1B100, 0x1B12F)
        if (univer >= 1200) add(0x1B130, 0x1B16F)
        if (univer >= 1000) add(0x1B170, 0x1B2FF)
        if (univer >= 700) add(0x1BC00, 0x1BC9F)
        if (univer >= 700) add(0x1BCA0, 0x1BCAF)
        add(0x1D000, 0x1D0FF)
        add(0x1D100, 0x1D1FF)
        add(0x1D200, 0x1D24F)
        if (univer >= 1100) add(0x1D2E0, 0x1D2FF)
        add(0x1D300, 0x1D35F)
        add(0x1D360, 0x1D37F)
        add(0x1D400, 0x1D7FF)
        if (univer >= 800) add(0x1D800, 0x1DAAF)
        if (univer >= 900) add(0x1E000, 0x1E02F)
        if (univer >= 1200) add(0x1E100, 0x1E14F)
        if (univer >= 1200) add(0x1E2C0, 0x1E2FF)
        if (univer >= 700) add(0x1E800, 0x1E8DF)
        if (univer >= 900) add(0x1E900, 0x1E95F)
        if (univer >= 1100) add(0x1EC70, 0x1ECBF)
        if (univer >= 1200) add(0x1ED00, 0x1ED4F)
        if (univer >= 610) add(0x1EE00, 0x1EEFF)
        add(0x1F000, 0x1F02F)
        add(0x1F030, 0x1F09F)
        add(0x1F0A0, 0x1F0FF)
        add(0x1F100, 0x1F1FF)
        add(0x1F200, 0x1F2FF)
        add(0x1F300, 0x1F5FF)
        add(0x1F600, 0x1F64F)
        if (univer >= 700) add(0x1F650, 0x1F67F)
        add(0x1F680, 0x1F6FF)
        add(0x1F700, 0x1F77F)
        if (univer >= 700) add(0x1F780, 0x1F7FF)
        if (univer >= 700) add(0x1F800, 0x1F8FF)
        if (univer >= 800) add(0x1F900, 0x1F9FF)
        if (univer >= 1100) add(0x1FA00, 0x1FA6F)
        if (univer >= 1200) add(0x1FA70, 0x1FAFF)
        if (univer >= 1300) add(0x1FB00, 0x1FBFF)
        add(0x20000, 0x2A6DF)
        add(0x2A700, 0x2B73F)
        add(0x2B740, 0x2B81F)
        if (univer >= 800) add(0x2B820, 0x2CEAF)
        if (univer >= 1000) add(0x2CEB0, 0x2EBEF)
        add(0x2F800, 0x2FA1F)
        if (univer >= 1300) add(0x30000, 0x3134F)
        add(0xE0000, 0xE007F)
        add(0xE0100, 0xE01EF)
        add(0xF0000, 0xFFFFF)
        add(0x100000, 0x10FFFF)
        resnormal = view!!.context.resources.getColor(android.R.color.transparent)
        resselect = view!!.context.resources.getColor(android.R.color.tab_indicator_text)
        layout = LinearLayout(view!!.context)
        layout!!.orientation = LinearLayout.VERTICAL
        code = Button(view!!.context)
        code!!.text = "U+10DDDD"
        code!!.setSingleLine()
        jump = Spinner(view!!.context)
        mark = Spinner(view!!.context)
        val fl = FrameLayout(view!!.context)
        fl.addView(mark, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.rightMargin = (view!!.context.resources.displayMetrics.scaledDensity * 22f).toInt()
        fl.addView(jump, lp)
        val hl = LinearLayout(view!!.context)
        hl.orientation = LinearLayout.HORIZONTAL
        hl.gravity = Gravity.CENTER
        hl.addView(fl, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val p = Paint()
        p.textSize = code!!.textSize
        hl.addView(code, LinearLayout.LayoutParams(code!!.paddingLeft + p.measureText("U+10DDDD").toInt() + code!!.paddingRight, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout!!.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout!!.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val jstr = arrayOfNulls<String>(fmap.size)
        val jmap = SparseArray<String>()
        for (s in jump!!.context.resources.getStringArray(R.array.codes)) jmap.put(Integer.valueOf(s.substring(0, s.indexOf(' ')), 16), s.substring(s.indexOf(' ') + 1))
        val jit: Iterator<Int> = fmap.keys.iterator()
        var i = 0
        while (jit.hasNext()) {
            val c = jit.next()
            jstr[i] = String.format("U+%04X %s", c, jmap[c])
            ++i
        }
        val adp = ArrayAdapter(jump!!.context, android.R.layout.simple_spinner_item, jstr)
        adp.setDropDownViewResource(R.layout.spinner_drop_down_item)
        jump!!.adapter = adp
        mark!!.adapter = MapAdapter(mark!!.context)
        mark!!.setSelection(0)
        mark!!.onItemSelectedListener = this
        view!!.setOnTouchListener(this)
        code!!.setOnClickListener(this)
        val e = fmap.floorEntry(scroll)
        if (e.value.second < scroll) scroll = e.value.second
        view!!.setSelection(scroll - e.key + e.value.first)
        view!!.setOnScrollListener(this)
        jump!!.onItemSelectedListener = this
        return layout
    }

    override fun destroy() {
        view!!.setOnScrollListener(null)
        layout = null
        code = null
        jump = null
        mark = null
        current = -1
        head = -1
        super.destroy()
    }

    fun find(code: Int): Int {
        val e = fmap.floorEntry(code)
        if (e.value.second < code) return -1
        scroll = code
        highlight = scroll - e.key + e.value.first
        if (view != null) {
            view!!.setSelection(scroll - e.key + e.value.first)
            if (view!!.firstVisiblePosition <= highlight && highlight <= view!!.lastVisiblePosition) {
                if (hightarget != null) hightarget!!.setBackgroundColor(resnormal)
                view!!.getChildAt(highlight - view!!.firstVisiblePosition).let {
                    hightarget = it
                    it.setBackgroundColor(resselect)
                }
            }
        }
        return scroll
    }

    fun mark(code: Int, name: String) {
        mmap.remove(code)
        mmap[code] = if (name.isNotEmpty()) name else "Unnamed Mark"
    }

    override fun save(edit: SharedPreferences.Editor) {
        edit.putInt("list", scroll)
        var str = ""
        val mit: Iterator<Map.Entry<Int, String>> = mmap.entries.iterator()
        while (mit.hasNext()) {
            val code = mit.next()
            str += String.format(Locale.US, "%d %s", code.key, code.value)
            if (mit.hasNext()) str += "\n"
        }
        edit.putString("mark", str)
    }

    private fun add(begin: Int, end: Int) {
        emap[count] = Pair(begin, emap.size)
        fmap[begin] = Pair(count, end)
        imap[begin] = imap.size
        jmap.add(count)
        count += end + 1 - begin
    }

    override fun getCount(): Int {
        return count
    }

    override fun getView(arg0: Int, arg1: View?, arg2: ViewGroup): View {
        val ret = super.getView(arg0, arg1, arg2)
        if (arg0 == highlight) {
            if (hightarget != null) hightarget!!.setBackgroundColor(resnormal)
            hightarget = ret
            hightarget!!.setBackgroundColor(resselect)
        } else if (ret === hightarget) hightarget!!.setBackgroundColor(resnormal)
        return ret
    }

    override fun getItemId(arg0: Int): Long {
        val e = emap.floorEntry(arg0)
        return (arg0 - e.key + e.value.first).toLong()
    }

    private var guard = 0
    override fun onItemSelected(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {
        var arg2 = arg2
        if (arg0 === jump) {
            current = arg2
            if (view != null) if (guard == 0) view!!.setSelection(jmap[arg2])
        }
        if (arg0 === mark) {
            if (arg2 == 1) {
                val items = arrayOfNulls<CharSequence>(mmap.size)
                var i = 0
                for ((key, value) in mmap) items[i++] = String.format("U+%04X %s", key, value)
                AlertDialog.Builder(view!!.context)
                        .setTitle(R.string.rem)
                        .setItems(items) { arg0, arg1 ->
                            var arg1 = arg1
                            for ((key) in mmap) if (--arg1 == -1) {
                                mmap.remove(key)
                                break
                            }
                        }.show()
                mark!!.setSelection(0)
            }
            if (arg2 > 1) {
                for ((key) in mmap) if (--arg2 == 1) find(key)
                mark!!.setSelection(0)
            }
        }
    }

    override fun onNothingSelected(arg0: AdapterView<*>?) {}
    override fun onScroll(arg0: AbsListView, arg1: Int, arg2: Int, arg3: Int) {
        var arg1 = arg1
        if (arg0 === view) {
            if (view!!.getChildAt(0) != null && view!!.getChildAt(0).top * -2 > view!!.getChildAt(0).height) arg1 += if (single) 1 else PageAdapter.column
            val e = emap.floorEntry(arg1)
            if (arg2 != 0) {
                if (e != null) {
                    if (jump != null) {
                        if (guard == 0 && current != e.value.second) {
                            current = e.value.second
                            ++guard
                            jump!!.setSelection(e.value.second, false)
                            jump!!.post { --guard }
                        }
                    }
                    if (code != null) {
                        if (head != arg1 - e.key + e.value.first) {
                            head = arg1 - e.key + e.value.first
                            code!!.text = String.format("U+%04X", head)
                        }
                    }
                    if (arg1 != 0) scroll = arg1 - e.key + e.value.first
                }
            }
        }
    }

    override fun onScrollStateChanged(arg0: AbsListView, arg1: Int) {}
    @SuppressLint("ClickableViewAccessibility")
    override fun onClick(arg0: View) {
        if (arg0 === code) {
            val edit = EditText(arg0.getContext())
            val ocl = View.OnClickListener { v ->
                if (v is ImageButton) {
                    edit.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                } else {
                    val s = (v as Button).text.toString()
                    val start = edit.selectionStart
                    val end = edit.selectionEnd
                    if (start == -1) return@OnClickListener
                    edit.editableText.replace(min(start, end), max(start, end), s)
                    edit.setSelection(min(start, end) + s.length)
                }
            }
            edit.setText(String.format("%04X", head))
            edit.setSingleLine()
            edit.imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_FORCE_ASCII
            edit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            edit.gravity = Gravity.CENTER_VERTICAL
            edit.setOnTouchListener { v, event ->
                v.onTouchEvent(event)
                (v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
                true
            }
            val text = TextView(arg0.getContext())
            text.text = "U+"
            text.gravity = Gravity.CENTER
            val del = ImageButton(arg0.getContext(), null, android.R.attr.buttonStyleSmall)
            val tv = TypedValue()
            arg0.getContext().theme.resolveAttribute(R.attr.backspace, tv, true)
            del.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, tv.resourceId, null))
            del.scaleType = ImageView.ScaleType.CENTER_INSIDE
            del.setPadding(0, 0, 0, 0)
            del.setOnClickListener(ocl)
            val vl = LinearLayout(arg0.getContext())
            vl.orientation = LinearLayout.VERTICAL
            val layout = LinearLayout(arg0.getContext())
            layout.addView(text, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            val p = Paint()
            p.textSize = edit.textSize
            layout.addView(edit, LinearLayout.LayoutParams(edit.paddingLeft + p.measureText("10DDDD").toInt() + edit.paddingRight, ViewGroup.LayoutParams.WRAP_CONTENT))
            layout.addView(del)
            layout.gravity = Gravity.END
            vl.addView(layout, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            val mlp = MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)
            mlp.setMargins(0, 0, 0, 0)
            for (i in 5 downTo 0) {
                val hl = LinearLayout(arg0.getContext())
                for (j in if (i == 0) 2..2 else 0..2) {
                    val btn = Button(arg0.getContext(), null, android.R.attr.buttonStyleSmall)
                    btn.text = String.format("%X", i * 3 + j - 2)
                    btn.setPadding(0, 0, 0, 0)
                    btn.setOnClickListener(ocl)
                    hl.addView(btn, LinearLayout.LayoutParams(mlp))
                }
                hl.gravity = Gravity.CENTER_HORIZONTAL
                vl.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            vl.gravity = Gravity.CENTER
            val dlg = AlertDialog.Builder(arg0.getContext())
                    .setTitle(R.string.code)
                    .setView(vl)
                    .setPositiveButton(android.R.string.search_go) { _, _ ->
                        if (view != null) try {
                            if (find(Integer.valueOf(edit.text.toString(), 16)) == -1) Toast.makeText(view!!.context, R.string.nocode, Toast.LENGTH_SHORT).show()
                        } catch (e: NumberFormatException) {
                            Toast.makeText(view!!.context, R.string.nocode, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .create()
            dlg.show()
            val btn = dlg.getButton(DialogInterface.BUTTON_POSITIVE)
            edit.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(arg0: Editable) {
                    try {
                        Integer.valueOf(arg0.toString(), 16)
                        btn.isEnabled = true
                    } catch (e: NumberFormatException) {
                        btn.isEnabled = false
                    }
                }

                override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
                override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            })
            edit.setSelectAllOnFocus(true)
            edit.requestFocus()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(arg0: View, arg1: MotionEvent): Boolean {
        highlight = -1
        if (hightarget != null) if (view != null) {
            hightarget!!.setBackgroundColor(resnormal)
            hightarget = null
        }
        return false
    }

    init {
        current = -1
        head = -1
        scroll = pref.getInt("list", 0)
        highlight = -1
        hightarget = null
        val str = pref.getString("mark", "")
        for (s in str!!.split("\n").toTypedArray()) {
            val space = s.indexOf(' ')
            if (space != -1) try {
                mmap[Integer.valueOf(s.substring(0, space))] = s.substring(space + 1)
            } catch (e: NumberFormatException) {
            }
        }
    }
}