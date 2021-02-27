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
import java.util.*

internal class StringAdapter(str: String, activity: Activity, db: NameDatabase) : UnicodeAdapter(activity, db, false) {
    private val list: ArrayList<Int> = ArrayList()
    override fun getCount(): Int {
        return list.size
    }

    override fun getItemId(i: Int): Long {
        return list[i].toLong()
    }

    init {
        var i = 0
        while (i < str.length) {
            val code = str.codePointAt(i)
            list.add(code)
            i += Character.charCount(code)
        }
    }
}