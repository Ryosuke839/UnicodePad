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
import com.mobeta.android.dslv.DragSortListView.DropListener
import com.mobeta.android.dslv.DragSortListView.RemoveListener
import java.util.*

internal class FavoriteAdapter(activity: Activity, pref: SharedPreferences, db: NameDatabase, single: Boolean) : UnicodeAdapter(activity, db, single), DropListener, RemoveListener {
    private var list: ArrayList<Int>
    private var temp: ArrayList<Int>
    override fun name(): Int {
        return R.string.favorite
    }

    override fun show() {
        truncate()
        view?.invalidateViews()
    }

    override fun leave() {
        commit()
        view?.invalidateViews()
    }

    override fun getCount(): Int {
        return temp.size
    }

    override fun getItemId(arg0: Int): Long {
        return temp[arg0].toLong()
    }

    fun add(code: Int) {
        list.remove(Integer.valueOf(code))
        list.add(code)
    }

    fun rem(code: Int) {
        list.remove(Integer.valueOf(code))
    }

    private fun commit() {
        runOnUiThread {
            if (list !== temp) {
                temp = list
            }
        }
    }

    private fun truncate() {
        runOnUiThread {
            if (list === temp) temp = ArrayList(list)
        }
    }

    fun isFavorite(code: Int): Boolean {
        return list.contains(code)
    }

    override fun save(edit: SharedPreferences.Editor) {
        var str = ""
        for (i in list) str += String(Character.toChars(i))
        edit.putString("fav", str)
    }

    override fun drop(from: Int, to: Int) {
        runOnUiThread {
            list = temp
            list.add(to, list.removeAt(from))
            truncate()
            view?.invalidateViews()
        }
    }

    override fun remove(which: Int) {
        runOnUiThread {
            list.remove(temp.removeAt(which))
            view?.invalidateViews()
        }
    }

    init {
        list = ArrayList()
        temp = list
        val str = pref.getString("fav", null) ?: ""
        var i = 0
        while (i < str.length) {
            val code = str.codePointAt(i)
            list.add(code)
            i += Character.charCount(code)
        }
    }
}