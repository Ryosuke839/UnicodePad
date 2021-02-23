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

internal class RecentAdapter(activity: Activity, pref: SharedPreferences, db: NameDatabase, single: Boolean) : UnicodeAdapter(activity, db, single), DropListener, RemoveListener {
    private var list: ArrayList<Int>
    private var temp: ArrayList<Int>
    override fun name(): Int {
        return R.string.recent
    }

    override fun show() {
        trunc()
        if (view != null) view!!.invalidateViews()
    }

    override fun leave() {
        commit()
        if (view != null) view!!.invalidateViews()
    }

    override fun getCount(): Int {
        return temp.size
    }

    override fun getItemId(arg0: Int): Long {
        return temp[temp.size - arg0 - 1].toLong()
    }

    fun add(code: Int) {
        runOnUiThread {
            list.remove(Integer.valueOf(code))
            list.add(code)
            if (list.size >= maxitems) list.removeAt(0)
        }
    }

    operator fun rem(code: Int) {
        runOnUiThread {
            list.remove(Integer.valueOf(code))
            if (list !== temp) temp.remove(Integer.valueOf(code))
            if (view != null) view!!.invalidateViews()
        }
    }

    private fun commit() {
        if (list !== temp) temp = list
    }

    private fun trunc() {
        if (list === temp) temp = ArrayList(list)
    }

    override fun save(edit: SharedPreferences.Editor) {
        var str = ""
        for (i in list) str += String(Character.toChars(i))
        edit.putString("rec", str)
    }

    override fun drop(from: Int, to: Int) {
        runOnUiThread {
            list = temp
            val i = temp.removeAt(temp.size - from - 1)
            temp.add(temp.size - to, i)
            trunc()
            if (view != null) view!!.invalidateViews()
        }
    }

    override fun remove(which: Int) {
        runOnUiThread {
            list.remove(temp.removeAt(temp.size - which - 1))
            if (view != null) view!!.invalidateViews()
        }
    }

    companion object {
        var maxitems = 16
    }

    init {
        list = ArrayList()
        temp = list
        val str = pref!!.getString("rec", "")
        var num = 0
        run {
            var i = 0
            while (i < str!!.length) {
                if (str.codePointAt(i) > 0xFFFF) ++i
                ++num
                ++i
            }
        }
        var i = 0
        while (i < str!!.length) {
            val code = str.codePointAt(i)
            if (code > 0xFFFF) ++i
            if (--num < maxitems) list.add(code)
            ++i
        }
    }
}