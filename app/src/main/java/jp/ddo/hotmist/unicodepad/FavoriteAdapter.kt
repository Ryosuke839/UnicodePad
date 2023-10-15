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
import androidx.recyclerview.widget.RecyclerView
import java.util.*

internal class FavoriteAdapter(activity: Activity, pref: SharedPreferences, db: NameDatabase, single: Boolean) : DragListUnicodeAdapter<Int>(activity, db, single) {
    override fun getUniqueItemId(position: Int): Long {
        return getItemCodePoint(position)
    }

    override fun onItemDragStarted(position: Int) {
    }

    override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {
    }

    override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
    }

    override fun name(): Int {
        return R.string.favorite
    }

    override fun show() {
    }

    override fun leave() {
    }

    override fun getItemCodePoint(i: Int): Long {
        return mItemList[i].toLong()
    }

    fun add(code: Int) {
        val position = mItemList.indexOf(code)
        if (position != -1) {
            changeItemPosition(position, 0)
        } else {
            addItem(itemCount, code)
        }
    }

    fun rem(code: Int) {
        val position = mItemList.indexOf(code)
        if (position != -1) {
            removeItem(position)
        }
    }

    fun isFavorite(code: Int): Boolean {
        return mItemList.contains(code)
    }

    override fun save(edit: SharedPreferences.Editor) {
        var str = ""
        for (i in mItemList) str += String(Character.toChars(i))
        edit.putString("fav", str)
    }

    init {
        mItemList = ArrayList()
        val str = pref.getString("fav", null) ?: ""
        var i = 0
        while (i < str.length) {
            val code = str.codePointAt(i)
            mItemList.add(code)
            i += Character.charCount(code)
        }
    }
}