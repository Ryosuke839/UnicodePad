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
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import java.util.*
import kotlin.streams.toList

internal class EditAdapter(activity: Activity, db: NameDatabase, single: Boolean, private val edit: EditText) : DragListUnicodeAdapter<Long>(activity, db, single), TextWatcher {
    private var suspend = false
    private var sequence = 0L

    private fun setString(s: CharSequence) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mItemList = s.codePoints().mapToLong { it.toLong() or (sequence++ shl 32) }.toList().toMutableList()
        } else {
            mItemList = ArrayList()
            for (i in s.indices) {
                if (Character.isLowSurrogate(s[i])) continue
                mItemList.add(Character.codePointAt(s, i).toLong() or (sequence++ shl 32))
            }
        }
        notifyDataSetChanged()
    }

    private fun isSameString(s: CharSequence): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mItemList.map { it.toInt() } == s.codePoints().toList()
        } else {
            val list = ArrayList<Int>()
            for (i in s.indices) {
                if (Character.isLowSurrogate(s[i])) continue
                list.add(Character.codePointAt(s, i))
            }
            mItemList.map { it.toInt() } == list
        }
    }

    override fun instantiate(view: View): View {
        setString(edit.editableText)
        edit.addTextChangedListener(this)
        return super.instantiate(view)
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
        return mItemList[position].toLong()
    }

    override fun onItemDragStarted(position: Int) {
    }

    override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {
    }

    override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
        runOnUiThread {
            suspend = true
            val count = Character.charCount(mItemList[toPosition].toInt())
            val (from, to) = if (fromPosition < toPosition) {
                val p = mItemList.subList(0, fromPosition).sumOf { ch ->
                    Character.charCount(ch.toInt())
                }
                p to p + mItemList.subList(fromPosition, toPosition).sumOf { ch ->
                    Character.charCount(ch.toInt())
                }
            } else {
                val p = mItemList.subList(0, toPosition).sumOf { ch ->
                    Character.charCount(ch.toInt())
                }
                p + mItemList.subList(toPosition + 1, fromPosition + 1).sumOf { ch ->
                    Character.charCount(ch.toInt())
                } to p
            }
            edit.editableText.delete(from, from + count)
            edit.editableText.insert(to, String(Character.toChars(mItemList[toPosition].toInt())))
            suspend = false
            if (!isSameString(edit.editableText)) {
                setString(edit.editableText)
            }
        }
    }

    override fun name(): Int {
        return R.string.edit
    }

    override fun getItemCodePoint(i: Int): Long {
        return (mItemList[i] and 0x1FFFFF).toLong()
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (suspend) return
        if (count == 0) return
        val startCp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            s.subSequence(0, start).codePoints().count().toInt()
        } else {
            Character.codePointCount(s, 0, start)
        }
        val cps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            s.subSequence(start, start + count).codePoints().toList()
        } else {
            val list = ArrayList<Int>()
            for (i in 0 until count) {
                if (Character.isLowSurrogate(s[start + i])) continue
                list.add(Character.codePointAt(s, start + i))
            }
            list
        }
        cps.forEachIndexed { index, i ->
            mItemList.add(startCp + index, i.toLong() or (sequence++ shl 32))
        }
        notifyItemRangeInserted(startCp, cps.size)
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
        val startCp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            s.subSequence(0, start).codePoints().count().toInt()
        } else {
            Character.codePointCount(s, 0, start)
        }
        val countCp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            s.subSequence(start, start + count).codePoints().count().toInt()
        } else {
            Character.codePointCount(s, start, start + count)
        }
        for (i in 0 until countCp) {
            mItemList.removeAt(startCp)
        }
        notifyItemRangeRemoved(startCp, countCp)
    }

    override fun afterTextChanged(s: Editable) {}

    init {
        mItemList = ArrayList()
    }
}