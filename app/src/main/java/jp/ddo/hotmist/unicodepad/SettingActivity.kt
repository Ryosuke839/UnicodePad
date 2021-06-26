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
@file:Suppress("DEPRECATION")

package jp.ddo.hotmist.unicodepad

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.preference.Preference.OnPreferenceChangeListener
import android.text.ClipboardManager
import android.widget.Toast

class SettingActivity : PreferenceActivity(), OnPreferenceChangeListener {
    private val adCompat: AdCompat = AdCompatImpl()
    override fun onCreate(savedInstanceState: Bundle?) {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(THEME[(pref.getString("theme", null)?.toIntOrNull() ?: 2131492983) - 2131492983])
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.setting)
        fun setEntry(it: ListPreference) = run {
            it.onPreferenceChangeListener = this
            it.summary = it.entry
        }
        fun setValue(it: ListPreference) = run {
            it.onPreferenceChangeListener = this
            it.summary = it.value
        }
        fun setText(it: EditTextPreference) = run {
            it.onPreferenceChangeListener = this
            it.summary = it.text
        }
        setEntry(findPreference("universion") as ListPreference)
        setEntry(findPreference("emojicompat") as ListPreference)
        findPreference("download").also {
            it.setOnPreferenceClickListener {
                openPage(getString(R.string.download_uri))
            }
        }
        setEntry(findPreference("theme") as ListPreference)
        setText(findPreference("textsize") as EditTextPreference)
        setValue(findPreference("column") as ListPreference)
        setValue(findPreference("columnl") as ListPreference)
        findPreference("tabs").also {
            it.setOnPreferenceClickListener {
                startActivity(Intent(this, TabsActivity::class.java))
                true
            }
        }
        setText(findPreference("padding") as EditTextPreference)
        setText(findPreference("gridsize") as EditTextPreference)
        setText(findPreference("viewsize") as EditTextPreference)
        setText(findPreference("checker") as EditTextPreference)
        setText(findPreference("recentsize") as EditTextPreference)
        setEntry(findPreference("scroll") as ListPreference)
        findPreference("legal_app").also {
            it.setOnPreferenceClickListener {
                openPage("https://github.com/Ryosuke839/UnicodePad")
            }
        }
        findPreference("legal_uni").also {
            it.setOnPreferenceClickListener {
                openPage("https://unicode.org/")
            }
        }
        if (!adCompat.showAdSettings) {
            (findPreference("no-ad") as CheckBoxPreference).also {
                if (Build.VERSION.SDK_INT >= 26) {
                    it.parent?.removePreference(it)
                } else {
                    it.isEnabled = false
                    it.isChecked = true
                }
            }
        }
        setResult(RESULT_OK)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference.hasKey()) {
            val key = preference.key
            try {
                if (key == "column" || key == "padding" || key == "recentsize") newValue.toString().toInt()
                if (key == "textsize" || key == "gridsize" || key == "viewsize" || key == "checker") newValue.toString().toFloat()
            } catch (e: NumberFormatException) {
                return false
            }
            if (key == "theme" || key == "emojicompat") {
                Toast.makeText(this, R.string.theme_title, Toast.LENGTH_SHORT).show()
                setResult(RESULT_FIRST_USER)
            }
        }
        preference.summary = if (preference is ListPreference) preference.entries[preference.findIndexOfValue(newValue.toString())] else newValue.toString()
        return true
    }

    private fun openPage(uri: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        if (this.packageManager.queryIntentActivities(intent, 0).size > 0) {
            // Show web page
            startActivity(intent)
        } else {
            // Copy URI
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).text = uri
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }
        return true
    }

    companion object {
        private val THEME = intArrayOf(
                androidx.appcompat.R.style.Theme_AppCompat,
                androidx.appcompat.R.style.Theme_AppCompat_Light,
                androidx.appcompat.R.style.Theme_AppCompat_Light_DarkActionBar)
    }
}