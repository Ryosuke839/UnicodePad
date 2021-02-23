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

abstract class UnicodeAdapter(private val activity: Activity, private val db: NameDatabase, var single: Boolean) : BaseAdapter() {
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

    override fun getItem(arg0: Int): String {
        return String(Character.toChars(getItemId(arg0).toInt()))
    }

    override fun getItemViewType(arg0: Int): Int {
        return 0
    }

    @SuppressLint("NewApi")
    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        return if (single) {
            (view as LinearLayout? ?: LinearLayout(activity).also {
                it.orientation = LinearLayout.HORIZONTAL
                it.addView(ImageView(activity).also { imageView ->
                    imageView.setImageResource(android.R.drawable.ic_menu_sort_by_size)
                    imageView.id = R.id.HANDLE_ID
                    if (!(this is DropListener || this is RemoveListener)) imageView.visibility = View.GONE
                }, LinearLayout.LayoutParams((activity.resources.displayMetrics.scaledDensity * 24).toInt(), ViewGroup.LayoutParams.MATCH_PARENT))
                it.addView(CharacterView(activity, null, android.R.attr.textAppearanceLarge), LinearLayout.LayoutParams((activity.resources.displayMetrics.scaledDensity * fontsize * 2 + padding * 2).toInt(), ViewGroup.LayoutParams.MATCH_PARENT))
                it.addView(LinearLayout(activity).also { linearLayout ->
                    linearLayout.orientation = LinearLayout.VERTICAL
                    linearLayout.addView(TextView(activity, null, android.R.attr.textAppearanceSmall).also { textView ->
                        textView.setPadding(0, 0, 0, 0)
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    linearLayout.addView(TextView(activity, null, android.R.attr.textAppearanceSmall).also { textView ->
                        textView.setPadding(0, 0, 0, 0)
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            }).also {
                (it.getChildAt(1) as CharacterView).let { characterView ->
                    characterView.text = getItem(i)
                    characterView.setPadding(padding, padding, padding, padding)
                    characterView.setTextSize(fontsize)
                    characterView.shrinkWidth(shrink)
                    characterView.setTypeface(tf)
                    characterView.drawSlash(true)
                    val ver = if (getItemId(i) != -1L) db.getint(getItemId(i).toInt(), "version") else db.getint(getItemString(i), "version")
                    characterView.setValid(ver != 0 && ver <= UnicodeActivity.univer)
                }
                (it.getChildAt(2) as LinearLayout).let { linearLayout ->
                    if (getItemId(i) != -1L) {
                        (linearLayout.getChildAt(0) as TextView).text = String.format("U+%04X", getItemId(i).toInt())
                        (linearLayout.getChildAt(1) as TextView).text = db[getItemId(i).toInt(), "name"]
                    } else {
                        (linearLayout.getChildAt(0) as TextView).text = (" " + getItemString(i)).replace(" ", " U+").substring(1)
                        (linearLayout.getChildAt(1) as TextView).text = db[getItemString(i), "name"]
                    }
                }
            }
        } else {
            (view as CharacterView? ?: CharacterView(activity, null, android.R.attr.textAppearanceLarge)).also {
                it.setPadding(padding, padding, padding, padding)
                it.setTextSize(fontsize)
                it.shrinkWidth(shrink)
                it.setTypeface(tf)
                it.drawSlash(true)
                val ver = if (getItemId(i) != -1L) db.getint(getItemId(i).toInt(), "version") else db.getint(getItemString(i), "version")
                it.setValid(ver != 0 && ver <= UnicodeActivity.univer)
                it.text = getItem(i)
            }
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
        activity.runOnUiThread(action)
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