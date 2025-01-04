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
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woxthebox.draglistview.DragItemAdapter
import com.woxthebox.draglistview.DragListView
import java.util.*
import kotlin.collections.ArrayList

class GridLayoutManagerWrapper(context: Context, spanCount: Int) : GridLayoutManager(context, spanCount) {
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("GridLayoutManager", "onLayoutChildren: $e")
        } catch (e: IllegalArgumentException) {
            Log.e("GridLayoutManager", "onLayoutChildren: $e")
        }
    }
}

interface UnicodeAdapter {
    val activity: Activity
    var single: Boolean
    var typeface: Typeface?
    var locale: Locale
    var view: View?
    var lastPadding: Int
    var onItemClickListener: View.OnClickListener?
    var onItemLongClickListener: View.OnLongClickListener?
    val reslist: Int

    fun name(): Int {
        return 0
    }

    fun instantiate(view: View): View {
        this.view = view
        return view
    }

    fun destroy() {
        view = null
    }

    fun freeze(): UnicodeAdapter {
        return this
    }

    fun invalidateViews()

    fun save(edit: SharedPreferences.Editor) {}
    fun show() {}
    fun leave() {}

    fun getItemCodePoint(i: Int): Long {
        return -1
    }

    fun getItemString(i: Int): String {
        return String.format("%04X", getItemCodePoint(i).toInt())
    }

    fun getItem(i: Int): String {
        return String(Character.toChars(getItemCodePoint(i).toInt()))
    }

    fun getCount(): Int

    fun getItemIndex(position: Int): Pair<Int, Int> {
        return position to -1
    }

    fun setListener(listener: PageAdapter) {
        onItemClickListener = View.OnClickListener {
            it.tag.let { tag ->
                check(tag is ViewHolder)
                val position = getItemIndex(tag.absoluteAdapterPosition).first
                if (position >= 0) {
                    listener.onItemClick(this, position, getItemCodePoint(position))
                }
            }
        }
        onItemLongClickListener = View.OnLongClickListener {
            it.tag.let { tag ->
                check(tag is ViewHolder)
                val position = getItemIndex(tag.absoluteAdapterPosition).first
                if (position >= 0) {
                    listener.onItemLongClick(this, position)
                }
            }
            true
        }
    }

    fun setTypeface(typeface: Typeface?, locale: Locale) {
        this.typeface = typeface
        this.locale = locale
    }

    companion object {
        var padding = 3
        var fontsize = 18f
        var shrink = true
    }

    abstract class ViewHolder(val view: View) : DragItemAdapter.ViewHolder(view, R.id.HANDLE_ID, false)

    abstract class CharacterViewHolder(view: View, val characterView: CharacterView) : ViewHolder(view) {
        fun setTypeface(typeface: Typeface?, locale: Locale) {
            characterView.setTypeface(typeface, locale)
        }
    }

    class CellViewHolder(view: LinearLayout, characterView: CharacterView) : CharacterViewHolder(view, characterView)

    class RowViewHolder(view: LinearLayout, characterView: CharacterView, val codePointView: TextView, val nameView: TextView) : CharacterViewHolder(view, characterView)

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (single) {
            val view = LinearLayout(activity)
            view.orientation = LinearLayout.HORIZONTAL
            view.addView(ImageView(activity).also { imageView ->
                imageView.setImageResource(TypedValue().also { tv ->
                    activity.theme.resolveAttribute(R.attr.drag_handle, tv, true)
                }.resourceId)
                imageView.id = R.id.HANDLE_ID
                if (this !is DragItemAdapter<*, *>) imageView.visibility = View.GONE
            }, LinearLayout.LayoutParams((activity.resources.displayMetrics.scaledDensity * 24).toInt(), ViewGroup.LayoutParams.MATCH_PARENT))
            val characterView = CharacterView(activity, null, android.R.attr.textAppearanceLarge)
            view.addView(characterView, LinearLayout.LayoutParams((activity.resources.displayMetrics.scaledDensity * fontsize * 2 + padding * 2).toInt(), ViewGroup.LayoutParams.MATCH_PARENT))
            val codePointView = TextView(activity, null, android.R.attr.textAppearanceSmall)
            val nameView = TextView(activity, null, android.R.attr.textAppearanceSmall)
            view.addView(LinearLayout(activity).also { linearLayout ->
                linearLayout.orientation = LinearLayout.VERTICAL
                linearLayout.addView(codePointView.also { textView ->
                    textView.setPadding(0, 0, 0, 0)
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                linearLayout.addView(nameView.also { textView ->
                    textView.setPadding(0, 0, 0, 0)
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            view.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            RowViewHolder(view, characterView, codePointView, nameView)
        } else {
            val view = LinearLayout(activity)
            view.orientation = LinearLayout.HORIZONTAL
            view.addView(View(activity).also { imageView ->
                imageView.id = R.id.HANDLE_ID
                imageView.visibility = View.GONE
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT))
            val characterView = CharacterView(activity, null, android.R.attr.textAppearanceLarge)
            view.addView(characterView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            CellViewHolder(view, characterView)
        }.also {
            if (viewType != -1) {
                it.view.setOnClickListener(onItemClickListener)
                it.view.setOnLongClickListener(onItemLongClickListener)
                it.view.setBackgroundResource(reslist)
            }
        }
    }

    fun onBindViewHolder(db: NameDatabase, holder: CharacterViewHolder, position: Int, last: Boolean) {
        holder.characterView.let { characterView ->
            characterView.setPadding(padding, padding, padding, padding + if (last) lastPadding else 0)
            characterView.setTextSize(fontsize)
            characterView.shrinkWidth(shrink)
            characterView.setTypeface(typeface, locale)
            characterView.drawSlash(true)
            val ver = if (getItemCodePoint(position) >= 0) db.getInt(getItemCodePoint(position).toInt(), "version") else db.getInt(getItemString(position), "version")
            characterView.setValid(ver != 0 && ver <= UnicodeActivity.univer)
            characterView.text = getItem(position)
        }
        if (holder is RowViewHolder) {
            if (getItemCodePoint(position) >= 0) {
                holder.codePointView.text = String.format("U+%04X", getItemCodePoint(position).toInt())
                holder.nameView.text = db[getItemCodePoint(position).toInt(), "name"]
            } else {
                holder.codePointView.text = (" " + getItemString(position)).replace(" ", " U+").substring(1)
                holder.nameView.text = db[getItemString(position), "name"]
            }
        }
        holder.view.tag = holder
    }

    fun runOnUiThread(action: Runnable?) {
        activity.runOnUiThread(action)
    }

    fun onTouch() {}

    var layoutManager: GridLayoutManager?

    fun getLayoutManager(context: Context, spanCount: Int): GridLayoutManager {
        return GridLayoutManagerWrapper(context, if (single) 1 else spanCount).also {
            layoutManager = it
        }
    }

    interface DataObserver {
        fun onChanged()
    }

    fun registerDataObserver(observer: RecyclerView.AdapterDataObserver)
    fun unregisterDataObserver(observer: RecyclerView.AdapterDataObserver)
}

abstract class RecyclerUnicodeAdapter(override val activity: Activity, private val db: NameDatabase, override var single: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), UnicodeAdapter {
    override var typeface: Typeface? = null
    override var locale: Locale = Locale.ROOT
    override var view: View? = null
    override var lastPadding: Int = 0
    override var onItemClickListener: View.OnClickListener? = null
    override var onItemLongClickListener: View.OnLongClickListener? = null
    override val reslist = TypedValue().also {
        activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
    }.resourceId

    init {
        setHasStableIds(false)
    }

    override fun invalidateViews() {
        notifyDataSetChanged()
    }

    override fun registerDataObserver(observer: RecyclerView.AdapterDataObserver) {
        registerAdapterDataObserver(observer)
    }

    override fun unregisterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        unregisterAdapterDataObserver(observer)
    }

    final override fun getItemId(i: Int): Long {
        val (itemIndex, titleIndex) = getItemIndex(i)
        return if (itemIndex == -1) -1L - titleIndex else getItemCodePoint(itemIndex)
    }

    override fun getItemIndex(position: Int): Pair<Int, Int> {
        val titleIndex = searchTitlePosition(position)
        return if (titleIndex >= 0 && position == getTitlePosition(titleIndex) + titleIndex)
            -1 to titleIndex
        else
            position - titleIndex - 1 to -1
    }

    open fun getTitleCount(): Int {
        return 0
    }

    open fun getTitlePosition(i: Int): Int {
        throw IndexOutOfBoundsException()
    }

    open fun getTitleString(i: Int): String {
        throw IndexOutOfBoundsException()
    }

    protected fun searchTitlePosition(i: Int): Int {
        if (getTitleCount() == 0) return -1
        var l = 0
        var r = getTitleCount()
        while (l < r) {
            val m = (l + r) / 2
            if (getTitlePosition(m) + m <= i) l = m + 1 else r = m
        }
        return l - 1
    }

    protected fun searchItemPosition(i: Int): Int {
        if (getTitleCount() == 0) return i
        var l = 0
        var r = getTitleCount()
        while (l < r) {
            val m = (l + r) / 2
            if (getTitlePosition(m) <= i) l = m + 1 else r = m
        }
        return i + l
    }

    protected fun scrollToTitle(position: Int) {
        layoutManager?.scrollToPositionWithOffset(getTitlePosition(position) + position, 0)
    }

    protected fun scrollToItem(position: Int) {
        layoutManager?.scrollToPositionWithOffset(searchItemPosition(position), 0)
    }

    final override fun getItemCount(): Int {
        return getCount() + getTitleCount()
    }

    final override fun getItemViewType(i: Int): Int {
        val titleIndex = searchTitlePosition(i)
        return if (getTitleCount() > 0 && i == getTitlePosition(titleIndex) + titleIndex) 1 else 0
    }

    override fun setTypeface(typeface: Typeface?, locale: Locale) {
        super.setTypeface(typeface, locale)
        layoutManager?.let { layoutManager ->
            for (i in layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()) {
                val holder = (view as RecyclerView?)?.findViewHolderForAdapterPosition(i)
                if (holder is UnicodeAdapter.CharacterViewHolder) {
                    holder.setTypeface(typeface, locale)
                }
            }
        }
    }

    class GroupViewHolder(view: TextView) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view
    }

    class HeaderViewHolder(view: TextView) : RecyclerView.ViewHolder(view)

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> GroupViewHolder(TextView(activity, null, android.R.attr.textAppearanceSmall))
            2 -> HeaderViewHolder(TextView(activity, null, android.R.attr.textAppearanceSmall))
            else -> super.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (itemIndex, titleIndex) = if (getItemViewType(position) != -1) getItemIndex(position) else position to -1
        when (holder) {
            is UnicodeAdapter.CharacterViewHolder -> super<UnicodeAdapter>.onBindViewHolder(db, holder, itemIndex, position == itemCount - 1)
            is GroupViewHolder -> {
                holder.textView.text = getTitleString(titleIndex)
            }
            is HeaderViewHolder -> {
            }
        }
        if (holder !is UnicodeAdapter.CharacterViewHolder) {
            holder.itemView.setPadding(0, 0, 0, if (position == itemCount - 1) lastPadding else 0)
        }
    }

    override fun runOnUiThread(action: Runnable?) {
        activity.runOnUiThread(action)
    }

    override var layoutManager: GridLayoutManager? = null

    open fun instantiateSpanSizeLookup(context: Context, spanCount: Int): GridLayoutManager.SpanSizeLookup {
        return object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (getItemViewType(position) != 1 || single) 1 else spanCount
            }
        }.also {
            it.isSpanIndexCacheEnabled = true
        }
    }

    override fun getLayoutManager(context: Context, spanCount: Int): GridLayoutManager {
        return GridLayoutManagerWrapper(context, if (single) 1 else spanCount).also {
            it.spanSizeLookup = instantiateSpanSizeLookup(context, spanCount)
            layoutManager = it
        }
    }
}

abstract class DragListUnicodeAdapter<T>(override val activity: Activity, private val db: NameDatabase, override var single: Boolean) : DragItemAdapter<T, UnicodeAdapter.ViewHolder>(), DragListView.DragListListener, UnicodeAdapter {
    override var typeface: Typeface? = null
    override var locale: Locale = Locale.ROOT
    override var view: View? = null
    override var lastPadding: Int = 0
    override var onItemClickListener: View.OnClickListener? = null
    override var onItemLongClickListener: View.OnLongClickListener? = null
    override val reslist = TypedValue().also {
        activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
    }.resourceId

    class ClonedDragListUnicodeAdapter<T>(base: DragListUnicodeAdapter<T>) : DragListUnicodeAdapter<ClonedDragListUnicodeAdapter.Data>(base.activity, base.db, base.single) {
        data class Data(val codePoint: Long, val item: String, val itemString: String)

        init {
            mItemList = ArrayList()
            for (i in base.mItemList.indices) {
                mItemList.add(Data(base.getItemCodePoint(i), base.getItem(i), base.getItemString(i)))
            }
        }

        override fun getUniqueItemId(position: Int): Long {
            return position.toLong()
        }

        override fun onItemDragStarted(position: Int) {
        }

        override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {
        }

        override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
        }

        override fun getItemCodePoint(i: Int): Long {
            return mItemList[i].codePoint
        }

        override fun getItem(i: Int): String {
            return mItemList[i].item
        }

        override fun getItemString(i: Int): String {
            return mItemList[i].itemString
        }
    }
    override fun freeze(): UnicodeAdapter {
        return ClonedDragListUnicodeAdapter(this)
    }

    override fun invalidateViews() {
        notifyDataSetChanged()
    }

    override fun registerDataObserver(observer: RecyclerView.AdapterDataObserver) {
        registerAdapterDataObserver(observer)
    }

    override fun unregisterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        unregisterAdapterDataObserver(observer)
    }

    override fun getCount(): Int {
        return itemCount
    }

    override fun setTypeface(typeface: Typeface?, locale: Locale) {
        super.setTypeface(typeface, locale)
        ((view as? DragListView)?.recyclerView ?: view as? RecyclerView)?.let { recyclerView ->
            (recyclerView.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                for (i in layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()) {
                    val holder = recyclerView.findViewHolderForAdapterPosition(i)
                    if (holder is UnicodeAdapter.CharacterViewHolder) {
                        holder.setTypeface(typeface, locale)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnicodeAdapter.ViewHolder {
        return super.onCreateViewHolder(parent, viewType) as UnicodeAdapter.ViewHolder
    }

    override fun onBindViewHolder(holder: UnicodeAdapter.ViewHolder, position: Int) {
        super<UnicodeAdapter>.onBindViewHolder(db, holder as UnicodeAdapter.CharacterViewHolder, position, position == itemCount - 1)
        super<DragItemAdapter>.onBindViewHolder(holder, position)
    }

    override fun runOnUiThread(action: Runnable?) {
        activity.runOnUiThread(action)
    }

    override var layoutManager: GridLayoutManager? = null
}
