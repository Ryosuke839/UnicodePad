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
import android.database.DataSetObserver
import android.graphics.Typeface
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobeta.android.dslv.DragSortListView.DropListener
import com.mobeta.android.dslv.DragSortListView.RemoveListener
import java.util.*

interface BaseUnicodeAdapter {
    fun getBaseAdapter(): UnicodeAdapter
}

abstract class UnicodeAdapter(protected val activity: Activity, private val db: NameDatabase, var single: Boolean) : RecyclerView.Adapter<UnicodeAdapter.ViewHolder>(), BaseUnicodeAdapter {
    private var typeface: Typeface? = null
    private var locale = Locale.ROOT
    protected var view: View? = null
    internal var lastPadding: Int = 0
    private var onItemClickListener: View.OnClickListener? = null
    private var onItemLongClickListener: View.OnLongClickListener? = null
    private val reslist = TypedValue().also {
        activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
    }.resourceId

    open fun name(): Int {
        return 0
    }

    open fun instantiate(view: View): View {
        this.view = view
        return view
    }

    open fun destroy() {
        view = null
    }

    open fun invalidateViews() {
        view.let {
            when (it) {
                is AbsListView -> it.invalidateViews()
                is RecyclerView -> notifyItemRangeChanged(0, itemCount)
            }
        }
    }

    open fun save(edit: SharedPreferences.Editor) {}
    open fun show() {}
    open fun leave() {}
    open fun getItemString(i: Int): String {
        return String.format("%04X", getItemId(i).toInt())
    }

    open fun getItem(i: Int): String {
        return String(Character.toChars(getItemId(i).toInt()))
    }

    override fun getItemViewType(i: Int): Int {
        return 0
    }

    fun setListener(listener: PageAdapter) {
        onItemClickListener = View.OnClickListener {
            val position = it.tag as Int
            listener.onItemClick(this, position, getItemId(position))
        }
        onItemLongClickListener = View.OnLongClickListener {
            val position = it.tag as Int
            listener.onItemLongClick(this, position)
            true
        }
    }


    abstract class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    abstract class CharacterViewHolder(view: View, val characterView: CharacterView) : ViewHolder(view)

    class CellViewHolder(view: CharacterView) : CharacterViewHolder(view, view)

    class RowViewHolder(view: LinearLayout, characterView: CharacterView, val codePointView: TextView, val nameView: TextView) : CharacterViewHolder(view, characterView)

    class GroupViewHolder(view: TextView) : ViewHolder(view) {
        val textView: TextView = view
    }

    class HeaderViewHolder(view: TextView) : ViewHolder(view) {
        val textView: TextView = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            1 -> GroupViewHolder(TextView(activity, null, android.R.attr.textAppearanceSmall))
            2 -> HeaderViewHolder(TextView(activity, null, android.R.attr.textAppearanceSmall))
            else -> if (single) {
                val view = LinearLayout(activity)
                view.orientation = LinearLayout.HORIZONTAL
                view.addView(ImageView(activity).also { imageView ->
                    imageView.setImageResource(TypedValue().also { tv ->
                        activity.theme.resolveAttribute(R.attr.drag_handle, tv, true)
                    }.resourceId)
                    imageView.id = R.id.HANDLE_ID
                    if (!(this is DropListener || this is RemoveListener)) imageView.visibility = View.GONE
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
                RowViewHolder(view, characterView, codePointView, nameView)
            } else {
                CellViewHolder(CharacterView(activity, null, android.R.attr.textAppearanceLarge))
            }.also {
                if (viewType != -1) {
                    it.view.setOnClickListener(onItemClickListener)
                    it.view.setOnLongClickListener(onItemLongClickListener)
                    it.view.setBackgroundResource(reslist)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is CharacterViewHolder -> {
                holder.characterView.let { characterView ->
                    characterView.setPadding(padding, padding, padding, padding + if (holder is CellViewHolder && position == itemCount - 1) lastPadding else 0)
                    characterView.setTextSize(fontsize)
                    characterView.shrinkWidth(shrink)
                    characterView.setTypeface(typeface, locale)
                    characterView.drawSlash(true)
                    val ver = if (getItemId(position) != -1L) db.getInt(getItemId(position).toInt(), "version") else db.getInt(getItemString(position), "version")
                    characterView.setValid(ver != 0 && ver <= UnicodeActivity.univer)
                    characterView.text = getItem(position)
                }
                if (holder is RowViewHolder) {
                    if (getItemId(position) != -1L) {
                        holder.codePointView.text = String.format("U+%04X", getItemId(position).toInt())
                        holder.nameView.text = db[getItemId(position).toInt(), "name"]
                    } else {
                        holder.codePointView.text = (" " + getItemString(position)).replace(" ", " U+").substring(1)
                        holder.nameView.text = db[getItemString(position), "name"]
                    }
                    holder.view.setPadding(0, 0, 0, if (position == itemCount - 1) lastPadding else 0)
                }
            }
            is GroupViewHolder -> {
                holder.textView.text = getItem(position)
            }
            is HeaderViewHolder -> {
                holder.textView.setText(if (position == 0) R.string.shown_desc else R.string.hidden_desc)
            }
        }
        holder.view.tag = position
    }

    override fun getBaseAdapter(): UnicodeAdapter {
        return this
    }

    protected fun runOnUiThread(action: Runnable?) {
        activity.runOnUiThread(action)
    }

    fun setTypeface(typeface: Typeface?, locale: Locale) {
        this.typeface = typeface
        this.locale = locale
        invalidateViews()
    }

    companion object {
        var padding = 3
        var fontsize = 18f
        var shrink = true
    }

    var layoutManager: GridLayoutManager? = null

    fun getLayoutManager(context: Context, spanCount: Int): GridLayoutManager {
        return GridLayoutManager(context, if (single) 1 else spanCount).also {
            it.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return 1
                }
            }
            layoutManager = it
        }
    }

    private lateinit var baseAdapter: BaseAdapter
    fun asBaseAdapter(): BaseAdapter {
        if (this::baseAdapter.isInitialized) return baseAdapter
        baseAdapter = BaseAdapterWrapper(this)
        return baseAdapter
    }

    class BaseAdapterWrapper(val adapter: UnicodeAdapter) : BaseAdapter(), BaseUnicodeAdapter {
        override fun getCount(): Int {
            return adapter.itemCount
        }

        override fun getItemId(posititon: Int): Long {
            return adapter.getItemId(posititon)
        }

        override fun getItem(posititon: Int): String {
            return adapter.getItem(posititon)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            (convertView?.let {
                if (adapter.single) {
                    val view = it as LinearLayout
                    val characterView = view.getChildAt(1) as CharacterView
                    val codePointView = (view.getChildAt(2) as LinearLayout).getChildAt(0) as TextView
                    val nameView = (view.getChildAt(2) as LinearLayout).getChildAt(1) as TextView
                    RowViewHolder(view, characterView, codePointView, nameView)
                } else {
                    val view = it as CharacterView
                    CellViewHolder(view)
                }
            } ?: adapter.onCreateViewHolder(parent!!, -1)).let {
                adapter.onBindViewHolder(it, position)
                return it.view
            }
        }

        override fun getBaseAdapter(): UnicodeAdapter {
            return adapter
        }

        override fun getItemViewType(position: Int): Int {
            return 0
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun isEmpty(): Boolean {
            return adapter.itemCount == 0
        }

        override fun registerDataSetObserver(observer: DataSetObserver?) {
        }

        override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        }

        override fun areAllItemsEnabled(): Boolean {
            return true
        }

        override fun isEnabled(position: Int): Boolean {
            return true
        }
    }
}