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
import android.util.TypedValue
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

class PageAdapter(private val activity: UnicodeActivity, private val pref: SharedPreferences, private val edit: EditText) : PagerAdapter(), OnItemClickListener, OnItemLongClickListener {
    private var numPage: Int
    private val layouts = arrayOfNulls<View>(MAX_VIEWS)
    private val views = arrayOfNulls<AbsListView>(MAX_VIEWS)
    private val adapterList: ListAdapter
    private val adapterFind: FindAdapter
    private val adapterRecent: RecentAdapter
    private val adapterFavorite: FavoriteAdapter
    internal val adapterEdit: EditAdapter
    private val adapterEmoji: EmojiAdapter
    private var blist = false
    private var bfind = false
    private var brec = false
    private var bfav = false
    private var bedt = false
    private var bemoji = false
    private val adapters: Array<UnicodeAdapter>
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
        return activity.resources.getString(adapters[position].name())
    }

    override fun notifyDataSetChanged() {
        numPage = pref.getInt("cnt_shown", 6)
        adapters[pref.getInt("ord_list", 1)] = adapterList
        listpage = pref.getInt("ord_list", 1)
        adapters[pref.getInt("ord_find", 3)] = adapterFind
        adapters[pref.getInt("ord_rec", 0)] = adapterRecent
        recpage = pref.getInt("ord_rec", 0)
        if (recpage >= numPage) recpage = -1
        adapters[pref.getInt("ord_fav", 4)] = adapterFavorite
        adapters[pref.getInt("ord_edt", 5)] = adapterEdit
        adapters[pref.getInt("ord_emoji", 2)] = adapterEmoji
        super.notifyDataSetChanged()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        brec = pref.getString("single_rec", "false") == "true"
        adapterRecent.single = brec
        blist = pref.getString("single_list", "false") == "true"
        adapterList.single = blist
        bfind = pref.getString("single_find", "true") == "true"
        adapterFind.single = bfind
        bfav = pref.getString("single_fav", "false") == "true"
        adapterFavorite.single = bfav
        bedt = pref.getString("single_edt", "true") == "true"
        adapterEdit.single = bedt
        bemoji = pref.getString("single_emoji", "false") == "true"
        adapterEmoji.single = bemoji
        return if (adapters[position].single) {
            if (adapters[position] is DropListener || adapters[position] is RemoveListener) {
                DragSortListView(activity, null).also { view ->
                    val controller = DragSortController(view, R.id.HANDLE_ID, DragSortController.ON_DRAG, DragSortController.FLING_REMOVE, 0, R.id.HANDLE_ID)
                    controller.isRemoveEnabled = true
                    controller.removeMode = DragSortController.FLING_REMOVE
                    controller.isSortEnabled = true
                    controller.setBackgroundColor(TypedValue().also { tv ->
                        activity.theme.resolveAttribute(android.R.attr.windowBackground, tv, true)
                    }.data)
                    view.setFloatViewManager(controller)
                    view.setOnTouchListener(controller)
                }
            } else {
                ListView(activity)
            }
        } else {
            GridView(activity).also { view ->
                view.numColumns = column
                view.adapter = adapters[position]
            }
        }.let { view ->
            view.onItemClickListener = this
            view.onItemLongClickListener = this
            view.adapter = adapters[position]
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            views[position] = view
            adapters[position].instantiate(view).also { layout ->
                collection.addView(layout, 0)
                layouts[position] = layout
            }
        }
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
        adapters[position].destroy()
        layouts[position] = null
        views[position] = null
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        if (parent == null || id != -1L) {
            adapterRecent.add(id.toInt())
            if (recpage != -1 && page != recpage) views[recpage]?.invalidateViews()
        }
        val start = edit.selectionStart
        val end = edit.selectionEnd
        if (start == -1) return
        edit.editableText.replace(min(start, end), max(start, end), if (parent == null || id != -1L) String(Character.toChars(id.toInt())) else parent.adapter.getItem(position) as String)
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        showDesc(parent, position, (if (parent is DragSortListView) parent.inputAdapter else parent.adapter) as UnicodeAdapter)
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
        val adapter = CharacterAdapter(activity, ua, tf, db, adapterFavorite)
        pager.adapter = adapter
        pager.setCurrentItem(index, false)
        pager.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (activity.resources.displayMetrics.scaledDensity * (CharacterAdapter.fontsize * 1.8f + TextAppearanceSpan(activity, android.R.style.TextAppearance_Small).textSize * 2.4f + 32f)).toInt())
        val layout = LinearLayout(activity)
        layout.addView(pager)
        val builder = AlertDialog.Builder(activity)
                .setView(layout)
        if (view != null) builder.setPositiveButton(R.string.input, DialogInterface.OnClickListener { _, _ ->
            if (adapter.id != -1L) {
                adapterRecent.add(adapter.id.toInt())
                if (recpage != -1 && page != recpage) views[recpage]?.invalidateViews()
            }
            val start = edit.selectionStart
            val end = edit.selectionEnd
            if (start == -1) return@OnClickListener
            edit.editableText.replace(min(start, end), max(start, end), ua.getItem(adapter.index))
        })
        if (view !is AbsListView || view.adapter !== adapterEmoji) builder.setNeutralButton(R.string.inlist) { _, _ -> find(adapter.id.toInt()) }
        if (view is AbsListView && view.adapter === adapterRecent) builder.setNegativeButton(R.string.remrec) { _, _ ->
            adapterRecent.rem(adapter.id.toInt())
            views[recpage]?.invalidateViews()
        }
        if (view is AbsListView && view.adapter === adapterEdit) builder.setNegativeButton(R.string.delete) { _, _ ->
            val i = pager.currentItem
            val s = edit.editableText.toString()
            edit.editableText.delete(s.offsetByCodePoints(0, i), s.offsetByCodePoints(0, i + 1))
        }
        if (view is AbsListView && view.adapter === adapterList) builder.setNegativeButton(R.string.mark) { _, _ ->
            val edit = EditText(activity)
            AlertDialog.Builder(activity)
                    .setTitle(R.string.mark)
                    .setView(edit)
                    .setPositiveButton(R.string.mark) { _, _ -> adapterList.mark(adapter.id.toInt(), edit.text.toString()) }
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
        if (page != -1) adapters[page].leave()
        adapters[position].show()
        page = position
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    private fun find(code: Int) {
        if (code == -1) return
        activity.setPage(listpage)
        adapterList.find(code)
    }

    fun save(edit: SharedPreferences.Editor) {
        adapterList.save(edit)
        adapterFind.save(edit)
        adapterRecent.save(edit)
        adapterFavorite.save(edit)
        adapterEdit.save(edit)
        adapterEmoji.save(edit)
    }

    fun setTypeface(tf: Typeface?) {
        this.tf = tf
        adapterList.setTypeface(tf)
        adapterFind.setTypeface(tf)
        adapterRecent.setTypeface(tf)
        adapterFavorite.setTypeface(tf)
        adapterEdit.setTypeface(tf)
        adapterEmoji.setTypeface(tf)
        for (i in 0 until MAX_VIEWS) views[i]?.invalidateViews()
    }

    fun onSizeChanged(top: Int) {
        adapters.forEach {
            it.lastPadding = top
        }
    }

    companion object {
        var column = 8
        private const val MAX_VIEWS: Int = 6
    }

    init {
        val adapters = arrayOfNulls<UnicodeAdapter>(MAX_VIEWS)
        numPage = pref.getInt("cnt_shown", 6)
        adapterList = ListAdapter(activity, pref, db, blist)
        adapters[pref.getInt("ord_list", 1)] = adapterList
        listpage = pref.getInt("ord_list", 1)
        adapterFind = FindAdapter(activity, pref, db, bfind)
        adapters[pref.getInt("ord_find", 3)] = adapterFind
        adapterRecent = RecentAdapter(activity, pref, db, brec)
        adapters[pref.getInt("ord_rec", 0)] = adapterRecent
        recpage = pref.getInt("ord_rec", 0)
        if (recpage >= numPage) recpage = -1
        adapterFavorite = FavoriteAdapter(activity, pref, db, bfav)
        adapters[pref.getInt("ord_fav", 4)] = adapterFavorite
        adapterEdit = EditAdapter(activity, db, bedt, edit)
        adapters[pref.getInt("ord_edt", 5)] = adapterEdit
        adapterEmoji = EmojiAdapter(activity, pref, db, bemoji)
        adapters[pref.getInt("ord_emoji", 2)] = adapterEmoji
        this.adapters = adapters.filterNotNull().toTypedArray()
        page = -1
        tf = null
    }
}