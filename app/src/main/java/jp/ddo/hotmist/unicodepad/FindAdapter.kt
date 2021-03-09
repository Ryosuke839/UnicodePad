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

internal class FindAdapter(activity: Activity, private val pref: SharedPreferences, private val db: NameDatabase, single: Boolean) : UnicodeAdapter(activity, db, single) {
    private var cur: Cursor? = null
    private var saved: String = pref.getString("find", null) ?: ""
    private var adapter: CompleteAdapter? = null
    override fun name(): Int {
        return R.string.find
    }

    override fun instantiate(view: AbsListView): View {
        super.instantiate(view)
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
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
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
        layout.addView(this.view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        find.setOnClickListener {
            saved = text.text.toString().replace("[^\\p{Alnum} \\-]".toRegex(), "")
            text.setText(saved)
            if (saved.isEmpty()) return@setOnClickListener
            cur?.close()
            cur = db.find(saved, UnicodeActivity.univer)
            if ((cur?.count ?: 0) > 0)
                adapter?.update(saved)
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(text.windowToken, 0)
            view.invalidateViews()
        }
        return layout
    }

    override fun destroy() {
        view?.setOnScrollListener(null)
        adapter = null
        cur?.close()
        cur = null
        super.destroy()
    }

    override fun save(edit: SharedPreferences.Editor) {
        edit.putString("find", saved)
        adapter?.save(edit)
    }

    override fun getCount(): Int {
        return cur?.count ?: 0
    }

    override fun getItemId(i: Int): Long {
        return cur?.let {
            if (i < 0 || i >= it.count) null else {
                it.moveToPosition(i)
                if (it.getType(0) == Cursor.FIELD_TYPE_INTEGER) it.getInt(0).toLong() else -1
            }
        } ?: 0
    }

    override fun getItemString(i: Int): String {
        return cur?.let {
            if (i < 0 || i >= it.count) return ""
            it.moveToPosition(i)
            if (it.getType(0) == Cursor.FIELD_TYPE_INTEGER) super.getItemString(i) else it.getString(0)
        } ?: ""
    }

    override fun getItem(i: Int): String {
        return getItemString(i).split(" ").joinToString("") { String(Character.toChars(it.toInt(16))) }
    }
}