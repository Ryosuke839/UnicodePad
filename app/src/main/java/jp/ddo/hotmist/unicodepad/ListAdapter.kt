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
import android.util.SparseArray
import android.util.TypedValue
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.content.res.ResourcesCompat
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal class ListAdapter(activity: Activity, pref: SharedPreferences, db: NameDatabase, single: Boolean) : UnicodeAdapter(activity, db, single) {
    private data class FromIndex (val codePoint: Int, val block: Int)
    private data class FromCodePoint (val index: Int, val end: Int, val block: Int)
    private var count = 0
    private val fromIndex: NavigableMap<Int, FromIndex> = TreeMap()
    private val fromCodePoint: NavigableMap<Int, FromCodePoint> = TreeMap()
    private val blockToIndex: MutableList<Int> = ArrayList()
    private val marks: NavigableMap<Int, String> = TreeMap()
    private var jump: Spinner? = null
    private var mark: Spinner? = null
    private var code: Button? = null
    private var current = -1
    private var head = -1
    private var scroll = pref.getInt("list", 0)
    private var resnormal = 0
    private var resselect = 0
    private var highlight = -1
    private var highTarget: View? = null
    private var guard = 0

    private inner class MapAdapter : SpinnerAdapter {
        override fun getCount(): Int {
            return if (marks.size == 0) 1 else marks.size + 2
        }

        override fun getItem(i: Int): String {
            when (i) {
                0 -> return activity.resources.getString(R.string.mark)
                1 -> return activity.resources.getString(R.string.rem)
                else -> {
                    var cnt = i
                    for ((key, value) in marks) if (--cnt == 1) return String.format("U+%04X %s", key, value)
                }
            }
            return ""
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getItemViewType(i: Int): Int {
            return 0
        }

        override fun getView(i: Int, view: View?, parent: ViewGroup): View {
            return ((view ?: (activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(android.R.layout.simple_spinner_item, parent, false)) as TextView).also {
                it.text = getItem(i)
                it.setTextColor(0x00000000)
            }
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
        override fun getDropDownView(i: Int, view: View?, parent: ViewGroup): View {
            return ((view ?: (activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(if (i == 0) R.layout.spinner_drop_down_void else R.layout.spinner_drop_down_item, parent, false)) as TextView).also {
                it.text = getItem(i)
            }
        }
    }

    override fun name(): Int {
        return R.string.list
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun instantiate(view: AbsListView): View {
        super.instantiate(view)
        fromIndex.clear()
        fromCodePoint.clear()
        blockToIndex.clear()
        count = 0
        val univer: Int = UnicodeActivity.univer
        fun add(begin: Int, end: Int) {
            fromIndex[count] = FromIndex(begin, fromIndex.size)
            fromCodePoint[begin] = FromCodePoint(count, end, fromCodePoint.size)
            blockToIndex.add(count)
            count += end + 1 - begin
        }
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
        @Suppress("DEPRECATION")
        resnormal = activity.resources.getColor(android.R.color.transparent)
        @Suppress("DEPRECATION")
        resselect = activity.resources.getColor(android.R.color.tab_indicator_text)
        return LinearLayout(activity).also { layout ->
            layout.orientation = LinearLayout.VERTICAL
            LinearLayout(activity).also { hl ->
                hl.orientation = LinearLayout.HORIZONTAL
                hl.gravity = Gravity.CENTER
                FrameLayout(activity).also { fl ->
                    mark = Spinner(activity).also { mark ->
                        fl.addView(mark, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                        mark.adapter = MapAdapter()
                        mark.setSelection(0)
                        mark.onItemSelectedListener = object : OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                if (position == 1) {
                                    val items = arrayOfNulls<CharSequence>(marks.size)
                                    var i = 0
                                    for ((key, value) in marks) items[i++] = String.format("U+%04X %s", key, value)
                                    AlertDialog.Builder(activity)
                                            .setTitle(R.string.rem)
                                            .setItems(items) { _, which ->
                                                var i2 = which
                                                for ((key) in marks) if (--i2 == -1) {
                                                    marks.remove(key)
                                                    break
                                                }
                                            }.show()
                                    mark.setSelection(0)
                                }
                                if (position > 1) {
                                    var i = position
                                    for ((key) in marks) if (--i == 1) find(key)
                                    mark.setSelection(0)
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                    }
                    jump = Spinner(activity).also { jump ->
                        fl.addView(jump, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                            it.rightMargin = (activity.resources.displayMetrics.scaledDensity * 22f).toInt()
                        })
                        val jstr = arrayOfNulls<String>(fromCodePoint.size)
                        val jmap = SparseArray<String>()
                        for (s in jump.context.resources.getStringArray(R.array.codes)) jmap.put(Integer.valueOf(s.substring(0, s.indexOf(' ')), 16), s.substring(s.indexOf(' ') + 1))
                        val jit: Iterator<Int> = fromCodePoint.keys.iterator()
                        var i = 0
                        while (jit.hasNext()) {
                            val c = jit.next()
                            jstr[i] = String.format("U+%04X %s", c, jmap[c])
                            ++i
                        }
                        val adp = ArrayAdapter(jump.context, android.R.layout.simple_spinner_item, jstr)
                        adp.setDropDownViewResource(R.layout.spinner_drop_down_item)
                        jump.adapter = adp
                        jump.onItemSelectedListener = object : OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                current = position
                                if (guard == 0) this@ListAdapter.view?.setSelection(this@ListAdapter.blockToIndex[position])
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                    }
                    hl.addView(fl, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                }
                code = Button(activity).also { code ->
                    code.text = "U+10DDDD"
                    code.setSingleLine()
                    val p = Paint()
                    p.textSize = code.textSize
                    hl.addView(code, LinearLayout.LayoutParams(code.paddingLeft + p.measureText("U+10DDDD").toInt() + code.paddingRight, ViewGroup.LayoutParams.WRAP_CONTENT))
                    code.setOnClickListener {
                        val edit = EditText(activity)
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
                        val text = TextView(activity)
                        text.text = "U+"
                        text.gravity = Gravity.CENTER
                        val del = ImageButton(activity, null, android.R.attr.buttonStyleSmall)
                        val tv = TypedValue()
                        activity.theme.resolveAttribute(R.attr.backspace, tv, true)
                        del.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, tv.resourceId, null))
                        del.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        del.setPadding(0, 0, 0, 0)
                        del.setOnClickListener(ocl)
                        val vl = LinearLayout(activity)
                        vl.orientation = LinearLayout.VERTICAL
                        vl.addView(LinearLayout(activity).also { layout ->
                            layout.addView(text, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                            p.textSize = edit.textSize
                            layout.addView(edit, LinearLayout.LayoutParams(edit.paddingLeft + Paint().also {
                                it.textSize = edit.textSize
                            }.measureText("10DDDD").toInt() + edit.paddingRight, ViewGroup.LayoutParams.WRAP_CONTENT))
                            layout.addView(del)
                            layout.gravity = Gravity.END
                        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                        val mlp = MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)
                        mlp.setMargins(0, 0, 0, 0)
                        for (i in 5 downTo 0) {
                            vl.addView(LinearLayout(activity).also {
                                for (j in if (i == 0) 2..2 else 0..2) {
                                    it.addView(Button(activity, null, android.R.attr.buttonStyleSmall).also { button ->
                                        button.text = String.format("%X", i * 3 + j - 2)
                                        button.setPadding(0, 0, 0, 0)
                                        button.setOnClickListener(ocl)
                                    }, LinearLayout.LayoutParams(mlp))
                                }
                                it.gravity = Gravity.CENTER_HORIZONTAL
                            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                        }
                        vl.gravity = Gravity.CENTER
                        val dlg = AlertDialog.Builder(activity)
                                .setTitle(R.string.code)
                                .setView(vl)
                                .setPositiveButton(android.R.string.search_go) { _, _ ->
                                    try {
                                        if (find(Integer.valueOf(edit.text.toString(), 16)) == -1) Toast.makeText(activity, R.string.nocode, Toast.LENGTH_SHORT).show()
                                    } catch (e: NumberFormatException) {
                                        Toast.makeText(activity, R.string.nocode, Toast.LENGTH_SHORT).show()
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
                layout.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            layout.addView(this.view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            this.view?.also { view ->
                view.setOnTouchListener { _, _ ->
                    highlight = -1
                    highTarget?.setBackgroundColor(resnormal)
                    highTarget = null
                    false
                }
                val e = fromCodePoint.floorEntry(scroll) ?: fromCodePoint.firstEntry()
                if (e.value.end < scroll) scroll = e.value.end
                view.setSelection(scroll - e.key + e.value.index)
                view.setOnScrollListener(object : AbsListView.OnScrollListener {
                    override fun onScrollStateChanged(p0: AbsListView?, p1: Int) { }

                    override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                        if (visibleItemCount == 0)
                            return
                        var firstItem = firstVisibleItem
                        if (view?.getChildAt(0)?.let { it.top * -2 > it.height } == true) firstItem += if (single) 1 else PageAdapter.column
                        val e2 = fromIndex.floorEntry(firstItem) ?: return
                        jump?.let {
                            if (guard == 0 && current != e2.value.block) {
                                current = e2.value.block
                                ++guard
                                it.setSelection(e2.value.block, false)
                                it.post { --guard }
                            }
                        }
                        code?.let {
                            if (head != firstItem - e2.key + e2.value.codePoint) {
                                head = firstItem - e2.key + e2.value.codePoint
                                it.text = String.format("U+%04X", head)
                            }
                        }
                        if (firstItem != 0) scroll = firstItem - e2.key + e2.value.codePoint
                    }
                })
            }
        }
    }

    override fun destroy() {
        view?.setOnScrollListener(null)
        jump = null
        mark = null
        code = null
        current = -1
        head = -1
        super.destroy()
    }

    fun find(code: Int): Int {
        val e = fromCodePoint.floorEntry(code) ?: fromCodePoint.firstEntry()
        if (e.value.end < code) return -1
        scroll = code
        highlight = scroll - e.key + e.value.index
        view?.let { view ->
            view.setSelection(scroll - e.key + e.value.index)
            highTarget?.setBackgroundColor(resnormal)
            highTarget = if (view.firstVisiblePosition <= highlight && highlight <= view.lastVisiblePosition) view.getChildAt(highlight - view.firstVisiblePosition) else null
            highTarget?.setBackgroundColor(resselect)
        }
        return scroll
    }

    fun mark(code: Int, name: String) {
        marks.remove(code)
        marks[code] = if (name.isNotEmpty()) name else "Unnamed Mark"
    }

    override fun save(edit: SharedPreferences.Editor) {
        edit.putInt("list", scroll)
        var str = ""
        val mit: Iterator<Map.Entry<Int, String>> = marks.entries.iterator()
        while (mit.hasNext()) {
            val code = mit.next()
            str += String.format(Locale.US, "%d %s", code.key, code.value)
            if (mit.hasNext()) str += "\n"
        }
        edit.putString("mark", str)
    }

    override fun getCount(): Int {
        return count
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        val ret = super.getView(i, view, viewGroup)
        if (i == highlight) {
            highTarget?.setBackgroundColor(resnormal)
            highTarget = ret
            highTarget?.setBackgroundColor(resselect)
        } else if (ret == highTarget) {
            highTarget?.setBackgroundColor(resnormal)
            highTarget = null
        }
        return ret
    }

    override fun getItemId(i: Int): Long {
        val e = fromIndex.floorEntry(i) ?: fromIndex.firstEntry()
        return (i - e.key + e.value.codePoint).toLong()
    }

    init {
        val str = pref.getString("mark", null) ?: ""
        for (s in str.split("\n").toTypedArray()) {
            val space = s.indexOf(' ')
            if (space != -1) try {
                marks[s.substring(0, space).toInt()] = s.substring(space + 1)
            } catch (e: NumberFormatException) {
            }
        }
    }
}