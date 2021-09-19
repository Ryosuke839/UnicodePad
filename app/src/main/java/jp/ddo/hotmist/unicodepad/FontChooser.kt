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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build
import androidx.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.util.*

class FontChooser internal constructor(private val activity: Activity, private val spinner: Spinner, private val listener: Listener) : FileChooser.Listener {
    private val adapter: ArrayAdapter<String> = ArrayAdapter(activity, android.R.layout.simple_spinner_item)
    private var fontIndex: Int
    private val fontPaths = ArrayList<String>()

    internal interface Listener {
        fun onTypefaceChosen(typeface: Typeface?)
    }

    fun save(edit: SharedPreferences.Editor) {
        var fs = ""
        for (s in fontPaths) fs += """
     $s
     
     """.trimIndent()
        edit.putString("fontpath", fs)
        edit.putInt("fontidx", if (spinner.selectedItemId > 2) spinner.selectedItemId.toInt() - 2 else 0)
    }

    private fun add(path: String): Boolean {
        if (load(path) == null) return false

        // Add remove item
        if (adapter.count < 3) adapter.add(activity.resources.getString(R.string.rem))

        // Remove duplicated items
        for (i in fontPaths.indices) {
            if (path != fontPaths[i]) continue
            adapter.remove(adapter.getItem(i + 3))
            fontPaths.removeAt(i)
        }
        adapter.add(File(path).name)
        fontPaths.add(path)
        return true
    }

    private fun load(path: String): Typeface? {
        return try {
            Typeface.createFromFile(path)
        } catch (e: RuntimeException) {
            null
        }
    }

    private fun remove(which: Int) {
        adapter.remove(adapter.getItem(which + 3))
        try {
            if (fontPaths[which].startsWith(activity.filesDir.canonicalPath)) File(fontPaths[which]).delete()
        } catch (e: IOException) {
        }
        fontPaths.removeAt(which)
        if (fontIndex == which + 1) fontIndex = 0
        if (fontIndex > which + 1) --fontIndex

        // Remove remove item
        if (fontPaths.size == 0) adapter.remove(adapter.getItem(2))
        spinner.setSelection(if (fontIndex == 0) 0 else fontIndex + 2)
    }

    companion object {
        var FONT_REQUEST_CODE = 42
    }

    init {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapter.add(activity.resources.getString(R.string.normal))
        adapter.add(activity.resources.getString(R.string.add))
        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        val fs = pref.getString("fontpath", null) ?: ""
        for (s in fs.split("\n").toTypedArray()) {
            if (s.isEmpty()) continue
            add(s)
        }
        fontIndex = pref.getInt("fontidx", 0)
        if (fontIndex > fontPaths.size) fontIndex = 0
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            @SuppressLint("InlinedApi")
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (parent !== spinner) return
                when (position) {
                    0 -> {
                        listener.onTypefaceChosen(Typeface.DEFAULT)
                        fontIndex = 0
                    }
                    1 -> if (Build.VERSION.SDK_INT >= 19) {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        intent.type = "*/*"
                        activity.startActivityForResult(intent, FONT_REQUEST_CODE)
                    } else FileChooser(activity, this@FontChooser, "/").show()
                    2 -> {
                        val str: Array<String?> = arrayOfNulls(fontPaths.size)
                        for (i in str.indices) str[i] = adapter.getItem(i + 3)
                        AlertDialog.Builder(activity).setTitle(R.string.rem).setItems(str) { _, which -> remove(which) }.setOnCancelListener { spinner.setSelection(if (fontIndex == 0) 0 else fontIndex + 2) }.show()
                    }
                    else -> {
                        fontIndex = position - 2
                        val tf = load(fontPaths[position - 3])
                        if (tf != null) listener.onTypefaceChosen(tf) else remove(position - 3)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinner.setSelection(if (fontIndex == 0) 0 else fontIndex + 2)
    }

    override fun onFileChosen(path: String) {
        if (path.endsWith(".zip")) {
            FileChooser(activity, this, path).onClick(null, -1)
            return
        }
        if (add(path)) {
            spinner.setSelection(adapter.count - 1)
        } else {
            Toast.makeText(activity, R.string.cantopen, Toast.LENGTH_SHORT).show()
            try {
                if (path.startsWith(activity.filesDir.canonicalPath)) File(path).delete()
            } catch (e: IOException) {
            }
            spinner.setSelection(0)
        }
    }

    override fun onFileCancel() {
        spinner.setSelection(if (fontIndex == 0) 0 else fontIndex + 2)
    }
}