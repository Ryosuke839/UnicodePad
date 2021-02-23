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
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.text.InputType
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.TextView.OnEditorActionListener

internal class FindAdapter(activity: Activity, pref: SharedPreferences, private val db: NameDatabase, single: Boolean) : UnicodeAdapter(activity, db, single), View.OnClickListener, OnEditorActionListener {
    private var cur: Cursor?
    private var layout: LinearLayout? = null
    private var text: AutoCompleteTextView? = null
    private var clear: ImageButton? = null
    private var find: ImageButton? = null
    private var saved: String?
    private val pref: SharedPreferences?
    private var adapter: CompleteAdapter? = null
    public override fun name(): Int {
        return R.string.find
    }

    public override fun instantiate(grd: AbsListView?): View? {
        super.instantiate(grd)
        layout = LinearLayout(view!!.context)
        layout!!.orientation = LinearLayout.VERTICAL
        text = AutoCompleteTextView(view!!.context)
        text!!.setSingleLine()
        text!!.setText(saved)
        text!!.setHint(R.string.fhint)
        text!!.imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_FORCE_ASCII
        text!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        text!!.setOnEditorActionListener(this)
        if (adapter == null) adapter = CompleteAdapter(view!!.context, pref)
        text!!.setAdapter(adapter)
        text!!.threshold = 1
        clear = ImageButton(view!!.context)
        val tv = TypedValue()
        view!!.context.theme.resolveAttribute(R.attr.cancel, tv, true)
        clear!!.setImageDrawable(view!!.context.resources.getDrawable(tv.resourceId))
        clear!!.scaleType = ImageView.ScaleType.CENTER_INSIDE
        clear!!.setBackgroundDrawable(null)
        clear!!.setPadding(0, 0, 0, 0)
        clear!!.setOnClickListener(this)
        val fl = FrameLayout(view!!.context)
        fl.addView(text, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.rightMargin = (view!!.context.resources.displayMetrics.density * 10f).toInt()
        lp.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        fl.addView(clear, lp)
        find = ImageButton(view!!.context)
        view!!.context.theme.resolveAttribute(R.attr.search, tv, true)
        find!!.setImageDrawable(view!!.context.resources.getDrawable(tv.resourceId))
        val hl = LinearLayout(view!!.context)
        hl.orientation = LinearLayout.HORIZONTAL
        hl.addView(fl, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        hl.addView(find, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        layout!!.addView(hl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout!!.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        find!!.setOnClickListener(this)
        return layout
    }

    public override fun destroy() {
        view!!.setOnScrollListener(null)
        layout = null
        text = null
        find = null
        adapter = null
        if (cur != null) cur!!.close()
        cur = null
        super.destroy()
    }

    public override fun save(edit: SharedPreferences.Editor) {
        edit.putString("find", saved)
        if (adapter != null) adapter!!.save(edit)
    }

    override fun getCount(): Int {
        return if (cur != null) cur!!.count else 0
    }

    override fun getItemId(arg0: Int): Long {
        if (cur == null || arg0 < 0 || arg0 >= cur!!.count) return 0
        cur!!.moveToPosition(arg0)
        return cur!!.getInt(0).toLong()
    }

    @SuppressLint("DefaultLocale")
    override fun onClick(arg0: View) {
        if (arg0 === clear) {
            text!!.setText("")
        }
        if (arg0 === find) {
            saved = text!!.text.toString().replace("[^\\p{Alnum} \\-]".toRegex(), "")
            text!!.setText(saved)
            if (saved!!.isEmpty()) return
            if (adapter != null) adapter!!.update(saved!!)
            if (cur != null) cur!!.close()
            cur = db.find(saved!!, UnicodeActivity.Companion.univer)
            (text!!.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(text!!.windowToken, 0)
            view!!.invalidateViews()
        }
    }

    override fun onEditorAction(arg0: TextView, arg1: Int, arg2: KeyEvent): Boolean {
        if (arg0 === text && arg2 != null && arg2.keyCode == KeyEvent.KEYCODE_ENTER) {
            if (arg2.action == KeyEvent.ACTION_DOWN) find!!.performClick()
            return true
        }
        return false
    }

    init {
        saved = pref!!.getString("find", "")
        this.pref = pref
        cur = null
    }
}