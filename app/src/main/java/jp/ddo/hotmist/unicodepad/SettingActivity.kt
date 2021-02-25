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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.*
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.Preference.OnPreferenceClickListener
import android.text.ClipboardManager
import android.widget.Toast

class SettingActivity : PreferenceActivity(), OnPreferenceClickListener, OnPreferenceChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val themelist = intArrayOf(
                androidx.appcompat.R.style.Theme_AppCompat,
                androidx.appcompat.R.style.Theme_AppCompat_Light,
                androidx.appcompat.R.style.Theme_AppCompat_Light_DarkActionBar)
        setTheme(themelist[Integer.valueOf(pref.getString("theme", "2131492983")!!) - 2131492983])
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.setting)
        val univer = findPreference("universion") as ListPreference
        univer.onPreferenceChangeListener = this
        univer.summary = univer.entry
        val emojicompat = findPreference("emojicompat") as ListPreference
        emojicompat.onPreferenceChangeListener = this
        emojicompat.summary = emojicompat.entry
        val download = findPreference("download")
        download.onPreferenceClickListener = this
        val theme = findPreference("theme") as ListPreference
        theme.onPreferenceChangeListener = this
        theme.summary = theme.entry
        val textsize = findPreference("textsize") as EditTextPreference
        textsize.onPreferenceChangeListener = this
        textsize.summary = textsize.text
        val column = findPreference("column") as ListPreference
        column.onPreferenceChangeListener = this
        column.summary = column.value
        val columnl = findPreference("columnl") as ListPreference
        columnl.onPreferenceChangeListener = this
        columnl.summary = columnl.value
        val tabs = findPreference("tabs")
        tabs.onPreferenceClickListener = this
        val padding = findPreference("padding") as EditTextPreference
        padding.onPreferenceChangeListener = this
        padding.summary = padding.text
        val gridsize = findPreference("gridsize") as EditTextPreference
        gridsize.onPreferenceChangeListener = this
        gridsize.summary = gridsize.text
        val viewsize = findPreference("viewsize") as EditTextPreference
        viewsize.onPreferenceChangeListener = this
        viewsize.summary = viewsize.text
        val checker = findPreference("checker") as EditTextPreference
        checker.onPreferenceChangeListener = this
        checker.summary = checker.text
        val recentsize = findPreference("recentsize") as EditTextPreference
        recentsize.onPreferenceChangeListener = this
        recentsize.summary = recentsize.text
        val scroll = findPreference("scroll") as ListPreference
        scroll.onPreferenceChangeListener = this
        scroll.summary = scroll.entry
        val legalApp = findPreference("legal_app")
        legalApp.onPreferenceClickListener = this
        val legalUni = findPreference("legal_uni")
        legalUni.onPreferenceClickListener = this
        setResult(RESULT_OK)
    }

    override fun onPreferenceChange(arg0: Preference, arg1: Any): Boolean {
        if (arg0.hasKey()) {
            val key = arg0.key
            try {
                if (key == "column" || key == "padding" || key == "recentsize") Integer.valueOf(arg1.toString())
                if (key == "textsize" || key == "gridsize" || key == "viewsize" || key == "checker") java.lang.Float.valueOf(arg1.toString())
            } catch (e: NumberFormatException) {
                return false
            }
            if (key == "theme" || key == "emojicompat") {
                Toast.makeText(this, R.string.theme_title, Toast.LENGTH_SHORT).show()
                setResult(RESULT_FIRST_USER)
            }
        }
        arg0.summary = if (arg0 is ListPreference) arg0.entries[arg0.findIndexOfValue(arg1.toString())] else arg1.toString()
        return true
    }

    private fun openPage(uri: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        if (this.packageManager.queryIntentActivities(intent, 0).size > 0) {
            // Show webpage
            startActivity(intent)
        } else {
            // Copy URI
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).text = uri
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }
        return true
    }

    override fun onPreferenceClick(arg0: Preference): Boolean {
        val key = arg0.key
        if (key == "download") {
            return openPage(getString(R.string.download_uri))
        }
        if (key == "tabs") {
            startActivity(Intent(this, TabsActivity::class.java))
            return true
        }
        if (key == "legal_app") {
            return openPage("https://github.com/Ryosuke839/UnicodePad")
        }
        return if (key == "legal_uni") {
            openPage("https://unicode.org/")
        } else false
    }
}