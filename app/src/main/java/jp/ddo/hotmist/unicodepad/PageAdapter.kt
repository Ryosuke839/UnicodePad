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
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.PagerTabStrip
import androidx.viewpager.widget.ViewPager
import com.woxthebox.draglistview.DragListView
import java.util.*
import kotlin.math.max
import kotlin.math.min

class PageAdapter(private val activity: UnicodeActivity, private val pref: SharedPreferences, private val edit: EditText) : PagerAdapter(), OnItemClickListener, OnItemLongClickListener {
    class DynamicDragListView(context: android.content.Context?, attrs: android.util.AttributeSet?) : DragListView(context, attrs) {
        init {
            super.onFinishInflate()
        }

        @SuppressLint("MissingSuperCall")
        override fun onFinishInflate() {
        }
    }

    private var numPage: Int
    private val layouts = arrayOfNulls<View>(MAX_VIEWS)
    private val views = arrayOfNulls<ViewGroup>(MAX_VIEWS)
    private val adapterList: ListAdapter
    private val adapterFind: FindAdapter
    private val adapterRecent: RecentAdapter
    private val adapterFavorite: FavoriteAdapter
    internal val adapterEdit: EditAdapter
    private val adapterEmoji: EmojiAdapter
    private var adapterCharacter: CharacterAdapter? = null
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
    private var locale = Locale.ROOT
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
        return adapters[position].let { adapter ->
            adapter.setListener(this)
            if (adapter is DragListUnicodeAdapter<*> && adapter.single) {
                DynamicDragListView(activity, null).also { view ->
                    view.setLayoutManager(LinearLayoutManager(activity))
                    view.setDragListListener(adapter)
                    view.setAdapter(adapter, false)
                    view.setCanDragHorizontally(false)
                    view.setCanDragVertically(true)
                }
            } else {
                check(adapter is RecyclerView.Adapter<*>)
                RecyclerView(activity).also { view ->
                    view.adapter = adapter
                    view.layoutManager = adapter.getLayoutManager(activity, column)
                    view.adapter = adapter
                }
            }.let { view ->
                view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                views[position] = view
                adapter.instantiate(view).also { layout ->
                    val scaleDetector = ScaleGestureDetector(activity, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            return (if (detector.scaleFactor > 1f) {
                                true
                            } else if (detector.scaleFactor < 1f) {
                                false
                            } else {
                                null
                            })?.let {
                                if (adapter.single != it) {
                                    pref.edit().putString(
                                        if (adapter === adapterRecent) "single_rec" else
                                            if (adapter === adapterList) "single_list" else
                                                if (adapter === adapterFind) "single_find" else
                                                    if (adapter === adapterFavorite) "single_fav" else
                                                        if (adapter === adapterEdit) "single_edt" else
                                                            if (adapter === adapterEmoji) "single_emoji" else null,
                                        it.toString()
                                    ).apply()
                                    notifyDataSetChanged()
                                }
                                true
                            } ?: false
                        }
                    })
                    (if (view is DragListView) view.recyclerView else view).setOnTouchListener { _, event ->
                        adapter.onTouch()
                        scaleDetector.onTouchEvent(event)
                        scaleDetector.isInProgress
                    }
                    collection.addView(layout, 0)
                    layouts[position] = layout
                }
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

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onItemClick(parent?.adapter as? UnicodeAdapter, position, id)
    }

    fun onItemClick(adapter: UnicodeAdapter?, position: Int, id: Long) {
        if (adapter == null || id >= 0) {
            adapterRecent.add(id.toInt())
        }
        val start = edit.selectionStart
        val end = edit.selectionEnd
        if (start == -1) return
        edit.editableText.replace(min(start, end), max(start, end), if (adapter == null || id >= 0) String(Character.toChars(id.toInt())) else adapter.getItem(position))
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        onItemLongClick(parent.adapter as UnicodeAdapter, position)
        return true
    }

    fun onItemLongClick(adapter: UnicodeAdapter, position: Int) {
        showDesc(adapter, position, adapter)
    }

    fun showDesc(parentAdapter: UnicodeAdapter?, index: Int, ua: UnicodeAdapter) {
        activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)?.let {
            (it as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(edit.windowToken, 0)
        }
        val pager = ViewPager(activity)
        pager.addView(PagerTabStrip(activity).apply {
            id = R.id.TAB_ID
        }, ViewPager.LayoutParams().apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            width = ViewGroup.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP
            isDecor = true
        })
        val adapter = CharacterAdapter(activity, ua.freeze(), tf, locale, db, adapterFavorite)
        adapterCharacter = adapter
        pager.adapter = adapter
        pager.setCurrentItem(index, false)
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                pager.requestLayout()
            }
            override fun onPageScrollStateChanged(state: Int) {}
        })
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        layout.addView(pager, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        layout.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            if (parentAdapter === adapterRecent) addView(Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
                text = activity.getString(R.string.remrec)
                setOnClickListener {
                    adapterRecent.rem(adapter.id.toInt())
                    adapterRecent.notifyItemRemoved(adapter.index)
                }
            }, LinearLayout.LayoutParams(Resources.getSystem().displayMetrics.widthPixels / 4, ViewGroup.LayoutParams.WRAP_CONTENT))
            if (view is AbsListView && parentAdapter === adapterEdit) addView(Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
                text = activity.getString(R.string.delete)
                setOnClickListener {
                    val i = pager.currentItem
                    val s = edit.editableText.toString()
                    edit.editableText.delete(s.offsetByCodePoints(0, i), s.offsetByCodePoints(0, i + 1))
                }
            }, LinearLayout.LayoutParams(Resources.getSystem().displayMetrics.widthPixels / 4, ViewGroup.LayoutParams.WRAP_CONTENT))
            if (view is RecyclerView && parentAdapter === adapterList) addView(Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
                text = activity.getString(R.string.mark)
                setOnClickListener {
                    val edit = EditText(activity)
                    AlertDialog.Builder(activity)
                            .setTitle(R.string.mark)
                            .setView(edit)
                            .setPositiveButton(R.string.mark) { _, _ -> adapterList.mark(adapter.id.toInt(), edit.text.toString()) }
                            .create().show()
                }
            }, LinearLayout.LayoutParams(Resources.getSystem().displayMetrics.widthPixels / 4, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(View(activity), LinearLayout.LayoutParams(0, 1, 1f))
            addView(Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
                text = activity.getString(R.string.find)
                isEnabled = adapter.getItemId(index) >= 0
                setOnClickListener { if (adapter.id >= 0) find(adapter.id.toInt()) }
                pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
                    override fun onPageSelected(position: Int) {
                        isEnabled = adapter.getItemId(position) >= 0
                    }
                    override fun onPageScrollStateChanged(state: Int) {}
                })
            }, LinearLayout.LayoutParams(Resources.getSystem().displayMetrics.widthPixels / 4, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(View(activity), LinearLayout.LayoutParams(0, 1, 1f))
            if (view != null) {
                addView(Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
                    text = activity.getString(android.R.string.copy)
                    setOnClickListener {
                        val cm = activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText(null, ua.getItem(adapter.index)))
                        if (Build.VERSION.SDK_INT <= 32) {
                            Toast.makeText(activity, R.string.copied, Toast.LENGTH_SHORT).show()
                        }
                    }
                }, LinearLayout.LayoutParams(Resources.getSystem().displayMetrics.widthPixels / 4, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
                    text = activity.getString(R.string.input)
                    setOnClickListener {
                        if (adapter.id >= 0) {
                            adapterRecent.add(adapter.id.toInt())
                        }
                        val start = edit.selectionStart
                        val end = edit.selectionEnd
                        if (start == -1) return@setOnClickListener
                        edit.editableText.replace(min(start, end), max(start, end), ua.getItem(adapter.index))
                    }
                }, LinearLayout.LayoutParams(Resources.getSystem().displayMetrics.widthPixels / 4, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        activity.setBottomSheetContent(layout, if (parentAdapter === adapterList) null else ua)
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
        if (code < 0) return
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

    fun setTypeface(tf: Typeface?, locale: Locale) {
        this.tf = tf
        this.locale = locale
        adapterList.setTypeface(tf, locale)
        adapterFind.setTypeface(tf, locale)
        adapterRecent.setTypeface(tf, locale)
        adapterFavorite.setTypeface(tf, locale)
        adapterEdit.setTypeface(tf, locale)
        adapterEmoji.setTypeface(tf, locale)
        adapterCharacter?.setTypeface(tf, locale)
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
        adapterEdit = EditAdapter(activity, pref, db, bedt, edit)
        adapters[pref.getInt("ord_edt", 5)] = adapterEdit
        adapterEmoji = EmojiAdapter(activity, pref, db, bemoji)
        adapters[pref.getInt("ord_emoji", 2)] = adapterEmoji
        this.adapters = adapters.filterNotNull().toTypedArray()
        page = -1
        tf = null
    }
}