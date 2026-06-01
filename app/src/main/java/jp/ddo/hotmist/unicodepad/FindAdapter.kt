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
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.text.InputType
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView

internal class FindAdapter(activity: Activity, private val pref: SharedPreferences, private val db: NameDatabase, single: Boolean) : RecyclerUnicodeAdapter(activity, db, single) {
    private var cur: List<Pair<NameDatabase.SearchType, Cursor>> = emptyList()
    private var saved: String = pref.getString("find", null) ?: ""
    private var adapter: CompleteAdapter? = null
    override fun name(): Int {
        return R.string.find
    }

    override fun instantiate(view: View): View {
        super.instantiate(view)
        check(view is RecyclerView)
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        val find = ImageButton(activity)
        find.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, TypedValue().also {
            activity.theme.resolveAttribute(R.attr.search, it, true)
        }.resourceId, null))
        val text = AutoCompleteTextView(activity)
        text.id = R.id.searchText
        text.setSingleLine()
        text.setText(saved)
        text.setHint(R.string.fhint)
        text.imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_FORCE_ASCII
        text.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        text.setOnEditorActionListener { _, _, event ->
            if (event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.action == KeyEvent.ACTION_DOWN) find.performClick()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        if (adapter == null) adapter = CompleteAdapter(activity, pref)
        text.setAdapter(adapter)
        text.threshold = 1
        val clear = ImageButton(activity)
        clear.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, TypedValue().also {
            activity.theme.resolveAttribute(R.attr.cancel, it, true)
        }.resourceId, null))
        clear.scaleType = ImageView.ScaleType.CENTER_INSIDE
        clear.background = null
        clear.setPadding(0, 0, 0, 0)
        clear.setOnClickListener {
            text.setText("")
        }
        val fl = FrameLayout(activity)
        fl.addView(text, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.rightMargin = (activity.resources.displayMetrics.density * 10f).toInt()
        lp.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        fl.addView(clear, lp)
        val hl = LinearLayout(activity)
        hl.orientation = LinearLayout.HORIZONTAL
        hl.addView(fl, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        hl.addView(find, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        layout.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        find.setOnClickListener {
            saved = text.text.toString().replace("[^\\p{Alnum} \\-]".toRegex(), "")
            text.setText(saved)
            if (saved.isEmpty()) return@setOnClickListener
            cur.forEach { it.second.close() }
            cur = NameDatabase.SearchType.entries.mapNotNull {
                val c = db.find(saved, UnicodeActivity.univer, it) ?: return@mapNotNull null
                if (c.count > 0) it to c else null
            }.toList()
            if (!cur.isEmpty())
                adapter?.update(saved)
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(text.windowToken, 0)
            invalidateViews()
        }
        return layout
    }

    override fun destroy() {
        adapter = null
        super.destroy()
    }

    override fun save(edit: SharedPreferences.Editor) {
        edit.putString("find", saved)
        adapter?.save(edit)
    }

    override fun getTitleCount(): Int {
        return cur.size
    }

    override fun getTitlePosition(i: Int): Int {
        return cur.subList(0, i).sumOf { it.second.count }
    }

    override fun getTitleString(i: Int): String {
        return when (cur[i].first) {
            NameDatabase.SearchType.NAME -> activity.resources.getString(R.string.search_title_name)
            NameDatabase.SearchType.EMOJI -> activity.resources.getString(R.string.search_title_emoji)
            NameDatabase.SearchType.UNIHAN_CANTONESE -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_cantonese)
            NameDatabase.SearchType.UNIHAN_DEFINITION -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_definition)
            NameDatabase.SearchType.UNIHAN_FANQIE -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_fanqie)
            NameDatabase.SearchType.UNIHAN_HANGUL -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_hangul)
            NameDatabase.SearchType.UNIHAN_HANYU_PINLU -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_hanyu_pinlu)
            NameDatabase.SearchType.UNIHAN_HANYU_PINYIN -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_hanyu_pinyin)
            NameDatabase.SearchType.UNIHAN_JAPANESE -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_japanese)
            NameDatabase.SearchType.UNIHAN_JAPANESE_KUN -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_japanese_kun)
            NameDatabase.SearchType.UNIHAN_JAPANESE_ON -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_japanese_on)
            NameDatabase.SearchType.UNIHAN_KOREAN -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_korean)
            NameDatabase.SearchType.UNIHAN_MANDARIN -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_mandarin)
            NameDatabase.SearchType.UNIHAN_SMSZD2003_READINGS -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_smszd2003_readings)
            NameDatabase.SearchType.UNIHAN_TANG -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_tang)
            NameDatabase.SearchType.UNIHAN_TGHZ2013 -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_tghz2013)
            NameDatabase.SearchType.UNIHAN_VIETNAMESE -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_vietnamese)
            NameDatabase.SearchType.UNIHAN_XHC1983 -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_xhc1983)
            NameDatabase.SearchType.UNIHAN_ZHUANG -> activity.resources.getString(R.string.unihan) + " " + activity.resources.getString(R.string.unihan_zhuang)
        }
    }

    override fun getCount(): Int {
        return cur.sumOf { it.second.count }
    }

    override fun getItemCodePoint(i: Int): Long {
        var rem = i
        cur.forEach {
            if (rem < it.second.count) {
                if (it.first == NameDatabase.SearchType.EMOJI) {
                    return -1
                }
                it.second.moveToPosition(rem)
                return it.second.getInt(0).toLong()
            }
            rem -= it.second.count
        }
        return -1
    }

    override fun getItemString(i: Int): String {
        var rem = i
        cur.forEach {
            if (rem < it.second.count) {
                if (it.first == NameDatabase.SearchType.EMOJI) {
                    it.second.moveToPosition(rem)
                    return it.second.getString(0)
                }
                return super.getItemString(i)
            }
            rem -= it.second.count
        }
        return ""
    }

    override fun getItem(i: Int): String {
        return getItemString(i).split(" ").joinToString("") { String(Character.toChars(it.toInt(16))) }
    }

    override fun getItemTextColumn(i: Int): String {
        var rem = i
        cur.forEach {
            if (rem < it.second.count) {
                return when (it.first) {
                    NameDatabase.SearchType.UNIHAN_CANTONESE -> "kCantonese"
                    NameDatabase.SearchType.UNIHAN_DEFINITION -> "kDefinition"
                    NameDatabase.SearchType.UNIHAN_FANQIE -> "kFanqie"
                    NameDatabase.SearchType.UNIHAN_HANGUL -> "kHangul"
                    NameDatabase.SearchType.UNIHAN_HANYU_PINLU -> "kHanyuPinlu"
                    NameDatabase.SearchType.UNIHAN_HANYU_PINYIN -> "kHanyuPinyin"
                    NameDatabase.SearchType.UNIHAN_JAPANESE -> "kJapanese"
                    NameDatabase.SearchType.UNIHAN_JAPANESE_KUN -> "kJapaneseKun"
                    NameDatabase.SearchType.UNIHAN_JAPANESE_ON -> "kJapaneseOn"
                    NameDatabase.SearchType.UNIHAN_KOREAN -> "kKorean"
                    NameDatabase.SearchType.UNIHAN_MANDARIN -> "kMandarin"
                    NameDatabase.SearchType.UNIHAN_SMSZD2003_READINGS -> "kSMSZD2003Readings"
                    NameDatabase.SearchType.UNIHAN_TANG -> "kTang"
                    NameDatabase.SearchType.UNIHAN_TGHZ2013 -> "kTGHZ2013"
                    NameDatabase.SearchType.UNIHAN_VIETNAMESE -> "kVietnamese"
                    NameDatabase.SearchType.UNIHAN_XHC1983 -> "kXHC1983"
                    NameDatabase.SearchType.UNIHAN_ZHUANG -> "kZhuang"
                    else -> super.getItemTextColumn(i)
                }
            }
            rem -= it.second.count
        }
        return super.getItemTextColumn(i)
    }
}