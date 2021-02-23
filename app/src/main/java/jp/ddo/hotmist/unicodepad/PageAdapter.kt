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
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Typeface
import android.text.style.TextAppearanceSpan
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.PagerTabStrip
import androidx.viewpager.widget.ViewPager
import com.mobeta.android.dslv.DragSortController
import com.mobeta.android.dslv.DragSortListView
import com.mobeta.android.dslv.DragSortListView.DropListener
import com.mobeta.android.dslv.DragSortListView.RemoveListener
import kotlin.math.max
import kotlin.math.min

class PageAdapter(private val activity: UnicodeActivity, private val pref: SharedPreferences, arg: EditText?) : PagerAdapter(), OnItemClickListener, OnItemLongClickListener {
    private var numPage: Int
    private val layout = arrayOfNulls<View>(MAX_VIEWS)
    private val views = arrayOfNulls<AbsListView>(MAX_VIEWS)
    private val edit: EditText?
    private val alist: ListAdapter
    private val afind: FindAdapter
    private val arec: RecentAdapter
    private val afav: FavoriteAdapter
    internal val aedt: EditAdapter
    private val aemoji: EmojiAdapter
    private var blist = false
    private var bfind = false
    private var brec = false
    private var bfav = false
    private var bedt = false
    private var bemoji = false
    private val adps = arrayOfNulls<UnicodeAdapter>(MAX_VIEWS)
    private var recpage = -1
    private var listpage = -1
    private var page: Int
    private var tf: Typeface?
    private val db: NameDatabase = NameDatabase(activity)
    val view: View?
        get() = views[page]

    override fun getCount(): Int {
        return numPage
    }

    override fun getPageTitle(position: Int): CharSequence {
        return activity.resources.getString(adps[position]!!.name())
    }

    override fun notifyDataSetChanged() {
        numPage = pref.getInt("cnt_shown", 6)
        adps[pref.getInt("ord_list", 1)] = alist
        listpage = pref.getInt("ord_list", 1)
        adps[pref.getInt("ord_find", 3)] = afind
        adps[pref.getInt("ord_rec", 0)] = arec
        recpage = pref.getInt("ord_rec", 0)
        if (recpage >= numPage) recpage = -1
        adps[pref.getInt("ord_fav", 4)] = afav
        adps[pref.getInt("ord_edt", 5)] = aedt
        adps[pref.getInt("ord_emoji", 2)] = aemoji
        super.notifyDataSetChanged()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        brec = pref.getString("single_rec", "false") == "true"
        arec.single = brec
        blist = pref.getString("single_list", "false") == "true"
        alist.single = blist
        bfind = pref.getString("single_find", "true") == "true"
        afind.single = bfind
        bfav = pref.getString("single_fav", "false") == "true"
        afav.single = bfav
        bedt = pref.getString("single_edt", "true") == "true"
        aedt.single = bedt
        bemoji = pref.getString("single_emoji", "false") == "true"
        aemoji.single = bemoji
        if (adps[position]!!.single) {
            if (adps[position] is DropListener || adps[position] is RemoveListener) {
                val view = DragSortListView(activity, null)
                val controller = DragSortController(view, R.id.HANDLE_ID, DragSortController.ON_DRAG, DragSortController.FLING_REMOVE, 0, R.id.HANDLE_ID)
                controller.isRemoveEnabled = true
                controller.removeMode = DragSortController.FLING_REMOVE
                controller.isSortEnabled = true
                view.setFloatViewManager(controller)
                view.setOnTouchListener(controller)
                views[position] = view
            } else {
                views[position] = ListView(activity)
            }
        } else {
            val view = GridView(activity)
            view.numColumns = column
            view.adapter = adps[position]
            views[position] = view
        }
        views[position]!!.onItemClickListener = this
        views[position]!!.onItemLongClickListener = this
        views[position]!!.adapter = adps[position]
        views[position]!!.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layout[position] = adps[position]!!.instantiate(views[position])
        collection.addView(layout[position], 0)
        return layout[position]!!
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
        adps[position]!!.destroy()
        layout[position] = null
        views[position] = null
    }

    override fun isViewFromObject(arg0: View, arg1: Any): Boolean {
        return arg0 === arg1
    }

    override fun onItemClick(arg0: AdapterView<*>?, arg1: View, arg2: Int, arg3: Long) {
        if (arg3 != -1L) {
            arec.add(arg3.toInt())
            if (recpage != -1 && page != recpage && views[recpage] != null) views[recpage]!!.invalidateViews()
        }
        val start = edit!!.selectionStart
        val end = edit.selectionEnd
        if (start == -1) return
        edit.editableText.replace(min(start, end), max(start, end), if (arg3 != -1L) String(Character.toChars(arg3.toInt())) else arg0!!.adapter.getItem(arg2) as String)
    }

    override fun onItemLongClick(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long): Boolean {
        showDesc(arg0, arg2, (if (arg0 is DragSortListView) arg0.inputAdapter else arg0.adapter) as UnicodeAdapter)
        return true
    }

    private var dlg: AlertDialog? = null
    fun showDesc(view: View?, index: Int, ua: UnicodeAdapter) {
        val tab = PagerTabStrip(activity)
        tab.id = R.id.TAB_ID
        val layoutParams = ViewPager.LayoutParams()
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.gravity = Gravity.TOP
        val pager = ViewPager(activity)
        pager.addView(tab, layoutParams)
        val adapter = CharacterAdapter(activity, ua, tf, db, afav)
        pager.adapter = adapter
        pager.setCurrentItem(index, false)
        pager.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (activity.resources.displayMetrics.scaledDensity * (CharacterAdapter.fontsize * 1.8f + TextAppearanceSpan(activity, android.R.style.TextAppearance_Small).textSize * 2.4f + 32f)).toInt())
        val layout = LinearLayout(activity)
        layout.addView(pager)
        val builder = AlertDialog.Builder(activity)
                .setView(layout)
        if (view != null) builder.setPositiveButton(R.string.input, DialogInterface.OnClickListener { _, _ ->
            if (adapter.id != -1L) {
                arec.add(adapter.id.toInt())
                if (recpage != -1 && page != recpage && views[recpage] != null) views[recpage]!!.invalidateViews()
            }
            val start = edit!!.selectionStart
            val end = edit.selectionEnd
            if (start == -1) return@OnClickListener
            edit.editableText.replace(min(start, end), max(start, end), ua.getItem(adapter.index) as String)
        })
        if (view !is AbsListView || view.adapter !== aemoji) builder.setNeutralButton(R.string.inlist) { _, _ -> find(adapter.id.toInt()) }
        if (view is AbsListView && view.adapter === arec) builder.setNegativeButton(R.string.remrec) { _, _ ->
            arec.rem(adapter.id.toInt())
            if (views[recpage] != null) views[recpage]!!.invalidateViews()
        }
        if (view is AbsListView && view.adapter === aedt) builder.setNegativeButton(R.string.delete) { _, _ ->
            val i = pager.currentItem
            val s = edit!!.editableText.toString()
            edit.editableText.delete(s.offsetByCodePoints(0, i), s.offsetByCodePoints(0, i + 1))
        }
        if (view is AbsListView && view.adapter === alist) builder.setNegativeButton(R.string.mark) { _, _ ->
            val edit = EditText(activity)
            AlertDialog.Builder(activity)
                    .setTitle(R.string.mark)
                    .setView(edit)
                    .setPositiveButton(R.string.mark) { _, _ -> alist.mark(adapter.id.toInt(), edit.text.toString()) }
                    .create().show()
        }
        dlg?.let {
            if (it.isShowing) it.dismiss()
        }
        builder.create().let {
            dlg = it
            it.show()
        }
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        if (page == position) return
        if (page != -1) adps[page]!!.leave()
        adps[position]!!.show()
        page = position
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    private fun find(code: Int) {
        activity.setPage(listpage)
        alist.find(code)
    }

    fun save(edit: SharedPreferences.Editor) {
        alist.save(edit)
        afind.save(edit)
        arec.save(edit)
        afav.save(edit)
        aedt.save(edit)
        aemoji.save(edit)
    }

    fun setTypeface(tf: Typeface?) {
        this.tf = tf
        alist.setTypeface(tf)
        afind.setTypeface(tf)
        arec.setTypeface(tf)
        afav.setTypeface(tf)
        aedt.setTypeface(tf)
        aemoji.setTypeface(tf)
        for (i in 0 until MAX_VIEWS) if (views[i] != null) views[i]!!.invalidateViews()
    }

    companion object {
        var column = 8
        private const val MAX_VIEWS: Int = 6
    }

    init {
        numPage = pref.getInt("cnt_shown", 6)
        alist = ListAdapter(activity, pref, db, blist)
        adps[pref.getInt("ord_list", 1)] = alist
        listpage = pref.getInt("ord_list", 1)
        afind = FindAdapter(activity, pref, db, bfind)
        adps[pref.getInt("ord_find", 3)] = afind
        arec = RecentAdapter(activity, pref, db, brec)
        adps[pref.getInt("ord_rec", 0)] = arec
        recpage = pref.getInt("ord_rec", 0)
        if (recpage >= numPage) recpage = -1
        afav = FavoriteAdapter(activity, pref, db, bfav)
        adps[pref.getInt("ord_fav", 4)] = afav
        aedt = EditAdapter(activity, pref, db, bedt, arg)
        adps[pref.getInt("ord_edt", 5)] = aedt
        aemoji = EmojiAdapter(activity, pref, db, bemoji)
        adps[pref.getInt("ord_emoji", 2)] = aemoji
        page = -1
        edit = arg
        tf = null
    }
}