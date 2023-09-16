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

internal class FindAdapter(activity: Activity, private val pref: SharedPreferences, private val db: NameDatabase, single: Boolean) : UnicodeAdapter(activity, db, single) {
    private var curList: Cursor? = null
    private var curEmoji: Cursor? = null
    private var saved: String = pref.getString("find", null) ?: ""
    private var adapter: CompleteAdapter? = null
    override fun name(): Int {
        return R.string.find
    }

    override fun instantiate(view: View): View {
        super.instantiate(view)
        val view = view as RecyclerView
        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        val find = ImageButton(activity)
        find.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, TypedValue().also {
            activity.theme.resolveAttribute(R.attr.search, it, true)
        }.resourceId, null))
        val text = AutoCompleteTextView(activity)
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
            curList?.close()
            curEmoji?.close()
            val curs = db.find(saved, UnicodeActivity.univer)
            curList = curs.first
            curEmoji = curs.second
            if ((curList?.count ?: 0) + (curEmoji?.count ?: 0) > 0)
                adapter?.update(saved)
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(text.windowToken, 0)
            invalidateViews()
        }
        return layout
    }

    override fun destroy() {
        adapter = null
        curList?.close()
        curList = null
        curEmoji?.close()
        curEmoji = null
        super.destroy()
    }

    override fun save(edit: SharedPreferences.Editor) {
        edit.putString("find", saved)
        adapter?.save(edit)
    }

    override fun getTitleCount(): Int {
        return 2
    }

    override fun getTitlePosition(i: Int): Int {
        return when (i) {
            0 -> 0
            1 -> (curList?.count ?: 0)
            else -> throw IndexOutOfBoundsException()
        }
    }

    override fun getTitleString(i: Int): String {
        return when (i) {
            0 -> "Code Point"
            1 -> "Emoji"
            else -> throw IndexOutOfBoundsException()
        }
    }

    override fun getCount(): Int {
        return (curList?.count ?: 0) + (curEmoji?.count ?: 0)
    }

    override fun getItemCodePoint(i: Int): Long {
        val offset = curList?.count ?: 0
        return if (i < offset) {
            curList?.let {
                if (i < 0) null else {
                    it.moveToPosition(i)
                    it.getInt(0).toLong()
                }
            } ?: 0
        } else -1
    }

    override fun getItemString(i: Int): String {
        val offset = curList?.count ?: 0
        return if (i < offset) {
            curList?.let {
                if (i < 0) return ""
                super.getItemString(i)
            }
        } else {
            curEmoji?.let {
                if (i >= offset + it.count) return ""
                it.moveToPosition(i - offset)
                it.getString(0)
            }
        } ?: ""
    }

    override fun getItem(i: Int): String {
        return getItemString(i).split(" ").joinToString("") { String(Character.toChars(it.toInt(16))) }
    }
}