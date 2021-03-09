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

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import java.util.*

internal class CompleteAdapter(context: Context, pref: SharedPreferences) : BaseAdapter(), Filterable {
    private val lock = Any()
    private var filter: CompleteFilter? = null
    private var list: ArrayList<String>
    private var temp: ArrayList<String>? = null
    private val inflater: LayoutInflater
    private var current = ""
    fun update(str: String) {
        synchronized(lock) {
            for (s in str.split(" ").toTypedArray()) if (s.isNotEmpty()) {
                (temp ?: list).let {
                    it.remove(s)
                    if (it.size == 255) it.removeAt(254)
                    it.add(0, s)
                }
            }
        }
        notifyDataSetChanged()
    }

    fun save(edit: SharedPreferences.Editor) {
        var str = ""
        for (s in list) str += """
     $s
     
     """.trimIndent()
        edit.putString("comp", str)
    }

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): Any {
        return current + list[position] + " "
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return ((convertView ?: inflater.inflate(R.layout.spinner_drop_down_item, parent, false)) as TextView).also {
            it.text = list[position]
        }
    }

    override fun getFilter(): Filter {
        return filter ?: CompleteFilter().also {
            filter = it
        }
    }

    private inner class CompleteFilter : Filter() {
        override fun performFiltering(prefix: CharSequence?): FilterResults {
            val results = FilterResults()
            val array = temp ?: ArrayList(list).also { synchronized(lock) { temp = it } }
            val str = prefix?.toString() ?: ""
            val idx = str.lastIndexOf(' ')
            if (str.length == idx + 1) {
                var res: ArrayList<String>
                synchronized(lock) { res = ArrayList(array) }
                results.values = res
                results.count = res.size
            } else {
                val prefixString = str.toUpperCase(Locale.ENGLISH).substring(idx + 1)
                current = if (idx == -1) "" else str.substring(0, idx + 1)
                var values: ArrayList<String>
                synchronized(lock) { values = ArrayList(array) }
                val count = values.size
                val newValues = ArrayList<String>()
                for (i in 0 until count) {
                    val value = values[i]
                    val valueText = value.toUpperCase(Locale.ENGLISH)
                    if (valueText.startsWith(prefixString)) newValues.add(value)
                }
                results.values = newValues
                results.count = newValues.size
            }
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            list = results.values as ArrayList<String>? ?: ArrayList()
            if (results.count > 0) notifyDataSetChanged() else notifyDataSetInvalidated()
        }
    }

    init {
        list = ArrayList()
        for (s in (pref.getString("comp", null) ?: "").split("\n").toTypedArray()) if (s.isNotEmpty()) list.add(s)
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }
}