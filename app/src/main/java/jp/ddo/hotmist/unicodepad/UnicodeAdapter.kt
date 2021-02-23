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
import android.database.DataSetObserver
import android.graphics.Typeface
import android.view.*
import android.widget.*
import com.mobeta.android.dslv.DragSortListView.DropListener
import com.mobeta.android.dslv.DragSortListView.RemoveListener

abstract class UnicodeAdapter(private val activity: Activity?, private val db: NameDatabase?, var single: Boolean) : BaseAdapter() {
    private var tf: Typeface? = null
    var view: AbsListView? = null
    open fun name(): Int {
        return 0
    }

    open fun instantiate(view: AbsListView?): View? {
        this.view = view
        return view
    }

    open fun destroy() {
        view = null
    }

    open fun save(edit: SharedPreferences.Editor) {}
    open fun show() {}
    open fun leave() {}
    open fun getItemString(arg0: Int): String {
        return String.format("%04X", getItemId(arg0).toInt())
    }

    override fun getItem(arg0: Int): Any {
        return String(Character.toChars(getItemId(arg0).toInt()))
    }

    override fun getItemViewType(arg0: Int): Int {
        return 0
    }

    @SuppressLint("NewApi")
    override fun getView(arg0: Int, arg1: View, arg2: ViewGroup): View {
        var arg1 = arg1
        return if (single) {
            if (arg1 == null) {
                val ct = CharacterView(arg2.context, null, android.R.attr.textAppearanceLarge)
                val pt = TextView(arg2.context, null, android.R.attr.textAppearanceSmall)
                pt.setPadding(0, 0, 0, 0)
                val nt = TextView(arg2.context, null, android.R.attr.textAppearanceSmall)
                nt.setPadding(0, 0, 0, 0)
                val vl = LinearLayout(arg2.context)
                vl.orientation = LinearLayout.VERTICAL
                vl.addView(pt, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                vl.addView(nt, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val hl = LinearLayout(arg2.context)
                hl.orientation = LinearLayout.HORIZONTAL
                val iv = ImageView(arg2.context)
                iv.setImageResource(android.R.drawable.ic_menu_sort_by_size)
                iv.id = R.id.HANDLE_ID
                if (!(this is DropListener || this is RemoveListener)) iv.visibility = View.GONE
                hl.addView(iv, LinearLayout.LayoutParams((arg2.context.resources.displayMetrics.scaledDensity * 24).toInt(), ViewGroup.LayoutParams.MATCH_PARENT))
                hl.addView(ct, LinearLayout.LayoutParams((arg2.context.resources.displayMetrics.scaledDensity * fontsize * 2 + padding * 2).toInt(), ViewGroup.LayoutParams.MATCH_PARENT))
                hl.addView(vl, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                arg1 = hl
            }
            ((arg1 as LinearLayout).getChildAt(1) as CharacterView).text = getItem(arg0) as String
            if (getItemId(arg0) != -1L) {
                ((arg1.getChildAt(2) as LinearLayout).getChildAt(0) as TextView).text = String.format("U+%04X", getItemId(arg0).toInt())
                ((arg1.getChildAt(2) as LinearLayout).getChildAt(1) as TextView).text = db!![getItemId(arg0).toInt(), "name"]
            } else {
                ((arg1.getChildAt(2) as LinearLayout).getChildAt(0) as TextView).text = (" " + getItemString(arg0)).replace(" ", " U+").substring(1)
                ((arg1.getChildAt(2) as LinearLayout).getChildAt(1) as TextView).text = db!![getItemString(arg0), "name"]
            }
            arg1.getChildAt(1).setPadding(padding, padding, padding, padding)
            (arg1.getChildAt(1) as CharacterView).setTextSize(fontsize)
            (arg1.getChildAt(1) as CharacterView).shrinkWidth(shrink)
            (arg1.getChildAt(1) as CharacterView).setTypeface(tf)
            (arg1.getChildAt(1) as CharacterView).drawSlash(true)
            val ver = if (getItemId(arg0) != -1L) db.getint(getItemId(arg0).toInt(), "version") else db.getint(getItemString(arg0), "version")
            (arg1.getChildAt(1) as CharacterView).setValid(ver != 0 && ver <= UnicodeActivity.Companion.univer)
            arg1
        } else {
            val tv: CharacterView
            tv = if (arg1 == null || arg1 !is CharacterView) {
                CharacterView(arg2.context, null, android.R.attr.textAppearanceLarge)
            } else arg1
            tv.setPadding(padding, padding, padding, padding)
            tv.setTextSize(fontsize)
            tv.shrinkWidth(shrink)
            tv.setTypeface(tf)
            tv.drawSlash(true)
            val ver = if (getItemId(arg0) != -1L) db!!.getint(getItemId(arg0).toInt(), "version") else db!!.getint(getItemString(arg0), "version")
            tv.setValid(ver != 0 && ver <= UnicodeActivity.Companion.univer)
            tv.text = getItem(arg0) as String
            tv
        }
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isEmpty(): Boolean {
        return count == 0
    }

    override fun registerDataSetObserver(arg0: DataSetObserver) {}
    override fun unregisterDataSetObserver(arg0: DataSetObserver) {}
    override fun areAllItemsEnabled(): Boolean {
        return true
    }

    override fun isEnabled(arg0: Int): Boolean {
        return true
    }

    protected fun runOnUiThread(action: Runnable?) {
        activity!!.runOnUiThread(action)
    }

    fun setTypeface(tf: Typeface?) {
        this.tf = tf
    }

    companion object {
        var padding = 3
        var fontsize = 18f
        var shrink = true
    }
}