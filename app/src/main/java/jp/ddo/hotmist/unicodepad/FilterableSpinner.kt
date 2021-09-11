package jp.ddo.hotmist.unicodepad

import android.content.Context
import android.content.res.Resources.Theme
import android.database.DataSetObserver
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow.INPUT_METHOD_FROM_FOCUSABLE
import android.widget.SpinnerAdapter
import android.widget.ThemedSpinnerAdapter
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.ListPopupWindow


class FilterableSpinner(context: Context, attrs: AttributeSet?, defStyle: Int) : androidx.appcompat.widget.AppCompatSpinner(context, attrs, defStyle) {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.spinnerStyle)

    private var editText: AppCompatEditText? = null

    override fun performClick(): Boolean {
        editText?.setText("")
        val handled = super.performClick()
        try {
            val popupField = AppCompatSpinner::class.java.getDeclaredField("mPopup")
            popupField.isAccessible = true
            val popup = popupField.get(this as AppCompatSpinner)
            if (popup is ListPopupWindow) {
                popup.inputMethodMode = INPUT_METHOD_FROM_FOCUSABLE
            }
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return handled
    }

    override fun setAdapter(adapter: SpinnerAdapter?) {
        super.setAdapter(adapter)
        if (adapter != null) {
            try {
                val popupField = AppCompatSpinner::class.java.getDeclaredField("mPopup")
                popupField.isAccessible = true
                val popup = popupField.get(this as AppCompatSpinner)
                if (popup is ListPopupWindow && adapter is ArrayAdapter<*>) {
                    val filterableAdapter = FilterableAdapter(adapter)
                    popup.setAdapter(DropDownAdapter(filterableAdapter, (popupContext ?: context).theme))
                    editText = AppCompatEditText(context).also {
                        it.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                            }

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            }

                            override fun afterTextChanged(s: Editable?) {
                                filterableAdapter.filter(s.toString())
                            }
                        })
                        popup.setPromptView(it)
                    }
                    popup.setOnItemClickListener { _, _, _, id ->
                        setSelection(id.toInt())
                        popup.dismiss()
                    }
                }
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }

    private inner class FilterableAdapter(private val adapter: ArrayAdapter<*>) : ArrayAdapter<FilterableItem>(context, android.R.layout.simple_spinner_item) {
        private var filter: String = ""
        private var filtered = IntArray(adapter.count) {it}

        override fun getCount(): Int {
            return filtered.size
        }

        override fun getItem(position: Int): FilterableItem? {
            return adapter.getItem(filtered[position]) as? FilterableItem
        }

        override fun getItemId(position: Int): Long {
            return adapter.getItemId(filtered[position])
        }

        fun filter(filter: String) {
            if (this.filter == filter) {
                return
            }
            this.filter = filter
            filtered = if (filter == "") {
                IntArray(adapter.count) { it }
            } else {
                IntArray(adapter.count) { it }.filter { (adapter.getItem(it) as? FilterableItem)?.contains(filter) != false }.toIntArray()
            }
            notifyDataSetChanged()
        }

        init {
            setDropDownViewResource(R.layout.spinner_drop_down_item)
        }
    }

    private class DropDownAdapter(private val mAdapter: SpinnerAdapter?,
                                  dropDownTheme: Theme?) : android.widget.ListAdapter, SpinnerAdapter {
        private var mListAdapter: ListAdapter? = null
        override fun getCount(): Int {
            return mAdapter?.count ?: 0
        }

        override fun getItem(position: Int): Any {
            return mAdapter?.getItem(position)!!
        }

        override fun getItemId(position: Int): Long {
            return mAdapter?.getItemId(position) ?: -1
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            return getDropDownView(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
            return mAdapter?.getDropDownView(position, convertView, parent)
        }

        override fun hasStableIds(): Boolean {
            return mAdapter != null && mAdapter.hasStableIds()
        }

        override fun registerDataSetObserver(observer: DataSetObserver) {
            mAdapter?.registerDataSetObserver(observer)
        }

        override fun unregisterDataSetObserver(observer: DataSetObserver) {
            mAdapter?.unregisterDataSetObserver(observer)
        }

        override fun areAllItemsEnabled(): Boolean {
            val adapter = mListAdapter
            return adapter?.areAllItemsEnabled() ?: true
        }

        override fun isEnabled(position: Int): Boolean {
            val adapter = mListAdapter
            return adapter?.isEnabled(position) ?: true
        }

        override fun getItemViewType(position: Int): Int {
            return 0
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun isEmpty(): Boolean {
            return count == 0
        }

        init {
            if (mAdapter is ListAdapter) {
                mListAdapter = mAdapter
            }
            if (dropDownTheme != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && mAdapter is ThemedSpinnerAdapter) {
                    val themedAdapter = mAdapter
                    if (themedAdapter.dropDownViewTheme != dropDownTheme) {
                        themedAdapter.dropDownViewTheme = dropDownTheme
                    }
                } else if (mAdapter is androidx.appcompat.widget.ThemedSpinnerAdapter) {
                    val themedAdapter = mAdapter
                    if (themedAdapter.dropDownViewTheme == null) {
                        themedAdapter.dropDownViewTheme = dropDownTheme
                    }
                }
            }
        }
    }

    interface FilterableItem {
        override fun toString(): String
        fun contains(str: String): Boolean
    }
}