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
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import androidx.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit

class FontChooser internal constructor(private val activity: Activity, val spinner: Spinner, private val listener: Listener) {
    class FontChooserAdapter(val activity: Activity) : ArrayAdapter<FontData.BaseFont?>(activity, android.R.layout.simple_spinner_item, android.R.id.text1) {
        init {
            setDropDownViewResource(android.R.layout.simple_list_item_2)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getView(position, convertView, parent).apply {
                getItem(position)?.let {
                    findViewById<TextView>(android.R.id.text1).text = it.name.ifEmpty { it.subtitle }
                }
            }
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getDropDownView(position, convertView, parent).apply {
                getItem(position)?.let {
                    findViewById<TextView>(android.R.id.text1).text =
                        it.name.ifEmpty { it.subtitle }
                    findViewById<TextView>(android.R.id.text2).apply {
                        if (it.name.isEmpty()) {
                            text = ""
                            visibility = View.GONE
                        } else {
                            text = it.subtitle
                            visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private val fontData = FontData()
    private val adapter = FontChooserAdapter(activity)
    private var fontIndex = 0

    internal interface Listener {
        fun onTypefaceChosen(typeface: Typeface?)
    }

    fun save(edit: SharedPreferences.Editor) {
        edit.putInt("fontidx", if (spinner.selectedItemId > 1) spinner.selectedItemId.toInt() - 1 else 0)
    }


    fun load(pref: SharedPreferences) {
        val hash = if (fontIndex > 0) { fontData.getFonts().getOrNull(fontIndex - 1)?.iterPaths?.asSequence()?.toList() } else { listOf() }
        fontData.loadFromPreferences(pref)
        fontIndex = if (if (fontIndex > 0) { fontData.getFonts().getOrNull(fontIndex - 1)?.iterPaths?.asSequence()?.toList() } else { listOf() } != hash) {
            0
        } else {
            pref.getInt("fontidx", 0)
        }
        adapter.clear()
        adapter.add(FontData.DummyFont(activity.getString(R.string.normal)))
        adapter.add(FontData.DummyFont(activity.getString(R.string.font_manage)))
        for (font in fontData.getFonts()) {
            adapter.add(font)
        }
        if (fontIndex > fontData.getFonts().size) fontIndex = 0
        spinner.setSelection(if (fontIndex == 0) 0 else fontIndex + 1)
    }

    companion object {
        var FONT_REQUEST_CODE = 42
    }

    init {
        spinner.adapter = adapter
        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            @SuppressLint("InlinedApi")
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (parent !== spinner) return
                when (position) {
                    0 -> {
                        listener.onTypefaceChosen(Typeface.DEFAULT)
                        fontIndex = 0
                    }
                    1 -> {
                        activity.startActivityForResult(Intent(activity, FontManagerActivity::class.java), FONT_REQUEST_CODE)
                        spinner.setSelection(if (fontIndex == 0) 0 else fontIndex + 1)
                    }
                    else -> {
                        fontIndex = position - 1
                        pref.edit(true) {
                            // Commit default font in case of crash
                            putInt("fontidx", 0)
                        }
                        try {
                            val tf = fontData.getFonts()[fontIndex - 1].getTypeface()
                            listener.onTypefaceChosen(tf)
                            pref.edit {
                                putInt("fontidx", fontIndex)
                            }
                        } catch (e: FontData.BaseFont.FontCouldNotBeLoadedException) {
                            Toast.makeText(activity, R.string.cantopen, Toast.LENGTH_SHORT).show()
                            spinner.setSelection(0)
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        load(pref)
    }
}