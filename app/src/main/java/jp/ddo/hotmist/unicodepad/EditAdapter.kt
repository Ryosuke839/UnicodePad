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
import android.content.SharedPreferences
import android.icu.text.BreakIterator
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import androidx.core.view.setPadding
import kotlin.collections.ArrayList
import kotlin.streams.toList

internal class EditAdapter(activity: Activity, pref: SharedPreferences, db: NameDatabase, single: Boolean, private val edit: EditText) : DragListUnicodeAdapter<Pair<CharSequence, Long>>(activity, db, single), TextWatcher {
    private var suspend = false
    private var sequence = 0L
    private var graphemeCluster = pref.getBoolean("grapheme_cluster", false)

    private fun setString(s: CharSequence) {
        mItemList = strToSequence(s).map { Pair(it, sequence++) }.toMutableList()
        notifyDataSetChanged()
    }

    private fun strToSequence(s: CharSequence): Sequence<CharSequence> {
        return sequence {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (graphemeCluster) {
                    BreakIterator.getCharacterInstance(locale).apply {
                        setText(s.toString())
                    }.let {
                        var start = it.first()
                        var end = it.next()
                        while (end != BreakIterator.DONE) {
                            yield(s.subSequence(start, end))
                            start = end
                            end = it.next()
                        }
                    }
                } else {
                    for (c in s.codePoints()) {
                        yield(String(Character.toChars(c)))
                    }
                }
            } else {
                for (i in s.indices) {
                    if (Character.isLowSurrogate(s[i])) continue
                    yield(s.subSequence(i, i + Character.charCount(Character.codePointAt(s, i))))
                }
            }
        }
    }

    private fun isSameString(s: CharSequence): Boolean {
        return strToSequence(s).map { it.toString() }.toList() == mItemList.map { it.first.toString() }.toList()
    }

    override fun instantiate(view: View): View {
        setString(edit.editableText)
        edit.addTextChangedListener(this)
        super.instantiate(view)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                addView(
                    Switch(activity).apply {
                        setText(R.string.grapheme_cluster)
                        isChecked = graphemeCluster
                        setOnCheckedChangeListener { _, isChecked ->
                            graphemeCluster = isChecked
                            if (edit.editableText.toString() != mItemList.joinToString("") { it.first }) {
                                setString(edit.editableText)
                                return@setOnCheckedChangeListener
                            }
                            if (edit.editableText.isEmpty()) return@setOnCheckedChangeListener
                            val iter = mItemList.iterator()
                            var next = iter.next()
                            var idxl = 0
                            var idxr = 0
                            mItemList = strToSequence(edit.editableText).map {
                                while (idxl < idxr) {
                                    idxl += next.first.length
                                    if (!iter.hasNext()) break
                                    next = iter.next()
                                }
                                idxr += it.length
                                if (next.first.toString() == it.toString()) {
                                    next
                                } else {
                                    Pair(it, sequence++)
                                }
                            }.toMutableList()
                            notifyDataSetChanged()
                        }
                        setPadding((activity.resources.displayMetrics.density * 8f).toInt())
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
                addView(
                    view,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                    )
                )
            }
        } else {
            view
        }
    }

    override fun destroy() {
        super.destroy()
        edit.removeTextChangedListener(this)
    }

    fun updateString() {
        if (suspend) return
        setString(edit.editableText)
    }

    override fun getUniqueItemId(position: Int): Long {
        return mItemList[position].second
    }

    override fun onItemDragStarted(position: Int) {
    }

    override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {
    }

    override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
        runOnUiThread {
            suspend = true
            val count = mItemList[toPosition].first.length
            val (from, to) = if (fromPosition < toPosition) {
                val p = mItemList.subList(0, fromPosition).sumOf { ch ->
                    ch.first.length
                }
                p to p + mItemList.subList(fromPosition, toPosition).sumOf { ch ->
                    ch.first.length
                }
            } else {
                val p = mItemList.subList(0, toPosition).sumOf { ch ->
                    ch.first.length
                }
                p + mItemList.subList(toPosition + 1, fromPosition + 1).sumOf { ch ->
                    ch.first.length
                } to p
            }
            edit.editableText.delete(from, from + count)
            edit.editableText.insert(to, mItemList[toPosition].first)
            suspend = false
            if (!isSameString(edit.editableText)) {
                setString(edit.editableText)
            }
        }
    }

    override fun name(): Int {
        return R.string.edit
    }

    override fun save(edit: SharedPreferences.Editor) {
        edit.putBoolean("grapheme_cluster", graphemeCluster)
    }

    override fun getItemCodePoint(i: Int): Long {
        return mItemList[i].first.let { cs ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cs.codePoints().toList()
            } else {
                cs.indices.mapNotNull { i ->
                    if (Character.isLowSurrogate(cs[i])) null else Character.codePointAt(cs, i)
                }
            }
        }.let { codePoints ->
            if (codePoints.count() == 1) codePoints.first().toLong() else -1
        }
    }

    override fun getItemString(i: Int): String {
        return  mItemList[i].first.let { cs ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cs.codePoints().toList().joinToString(" ") { String.format("%04X", it) }
            } else {
                cs.indices.mapNotNull { i ->
                    if (Character.isLowSurrogate(cs[i])) null else String.format("%04X", Character.codePointAt(cs, i))
                }.joinToString(" ")
            }
        }
    }

    override fun getItem(i: Int): String {
        return mItemList[i].first.toString()
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (suspend) return

        var index = 0
        var l = 0
        while (index < mItemList.size) {
            if (l >= start) break
            l += mItemList[index++].first.length
        }
        val startIndex = if (l > start) index - 1 else index
        while (index < mItemList.size) {
            if (l >= start + before) break
            l += mItemList[index++].first.length
        }
        val endIndex = index
        for (i in startIndex until endIndex) {
            mItemList.removeAt(startIndex)
        }
        notifyItemRangeRemoved(startIndex, endIndex - startIndex)

        val newList = strToSequence(s).toList()

        if (mItemList.subList(startIndex, mItemList.size) == newList.subList(newList.size + startIndex - mItemList.size, newList.size)) {
            val len = newList.size - mItemList.size
            mItemList.addAll(startIndex, newList.subList(startIndex, startIndex + len).map { it to sequence++ })
            notifyItemRangeInserted(startIndex, len)
        } else {
            val len = mItemList.size - startIndex
            while (mItemList.size > startIndex) {
                mItemList.removeAt(startIndex)
            }
            notifyItemRangeRemoved(startIndex, len)
            mItemList.addAll(startIndex, newList.subList(startIndex, newList.size).map { it to sequence++ })
            notifyItemRangeInserted(startIndex, mItemList.size - startIndex)
        }

        if (!isSameString(edit.editableText)) {
            setString(edit.editableText)
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (suspend) return
        if (count == 0) return

        if (!isSameString(edit.editableText)) {
            setString(edit.editableText)
        }
    }

    override fun afterTextChanged(s: Editable) {}

    init {
        mItemList = ArrayList()
    }
}