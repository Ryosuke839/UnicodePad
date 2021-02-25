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

internal class CompleteAdapter(context: Context, pref: SharedPreferences?) : BaseAdapter(), Filterable {
    private val lock = Any()
    private var filter: CompleteFilter? = null
    private var list: ArrayList<String>
    private var temp: ArrayList<String>? = null
    private val inflater: LayoutInflater
    private var current = ""
    fun update(str: String) {
        synchronized(lock) {
            for (s in str.split(" ").toTypedArray()) if (s.isNotEmpty()) {
                (if (temp == null) list else temp)!!.remove(s)
                if ((if (temp == null) list else temp)!!.size == 255) (if (temp == null) list else temp)!!.removeAt(254)
                (if (temp == null) list else temp)!!.add(0, s)
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
        if (filter == null) filter = CompleteFilter()
        return filter!!
    }

    private inner class CompleteFilter : Filter() {
        override fun performFiltering(prefix: CharSequence): FilterResults {
            val results = FilterResults()
            if (temp == null) {
                synchronized(lock) { temp = ArrayList(list) }
            }
            val idx = prefix.toString().lastIndexOf(' ')
            if (prefix.length == idx + 1) {
                var res: ArrayList<String>
                synchronized(lock) { res = ArrayList(temp!!) }
                results.values = res
                results.count = res.size
            } else {
                val prefixString = prefix.toString().toUpperCase(Locale.ENGLISH).substring(idx + 1)
                current = if (idx == -1) "" else prefix.toString().substring(0, idx + 1)
                var values: ArrayList<String>
                synchronized(lock) { values = ArrayList(temp!!) }
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

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            list = results.values as ArrayList<String>
            if (results.count > 0) notifyDataSetChanged() else notifyDataSetInvalidated()
        }
    }

    init {
        list = ArrayList()
        for (s in pref!!.getString("comp", "")!!.split("\n").toTypedArray()) if (s.isNotEmpty()) list.add(s)
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }
}