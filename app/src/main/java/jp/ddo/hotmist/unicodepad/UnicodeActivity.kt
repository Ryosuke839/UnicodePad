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
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.OpenableColumns
import android.text.*
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.FontRequest
import androidx.core.view.MenuItemCompat
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.EmojiCompat.InitCallback
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.CRC32
import kotlin.math.max
import kotlin.math.min

@Suppress("DEPRECATION")
class UnicodeActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var btnClear: ImageButton
    private lateinit var btnFinish: Button
    private lateinit var chooser: FontChooser
    private lateinit var scroll: LockableScrollView
    private lateinit var pager: ViewPager
    internal lateinit var adpPage: PageAdapter
    private val adCompat: AdCompat = AdCompatImpl()
    private lateinit var cm: ClipboardManager
    private lateinit var pref: SharedPreferences
    private var action: String? = null
    private var created = false
    private var disableime = false
    private var delay: Runnable? = null
    private var timer = 500
    @SuppressLint("ClickableViewAccessibility")
    public override fun onCreate(savedInstanceState: Bundle?) {
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        onActivityResult(-1, 0, null)
        val useEmoji = pref.getString("emojicompat", "false")
        if (useEmoji != "null") {
            EmojiCompat.init(FontRequestEmojiCompatConfig(this, FontRequest(
                    "com.google.android.gms.fonts",
                    "com.google.android.gms",
                    "Noto Color Emoji Compat",
                    R.array.com_google_android_gms_fonts_certs))
                    .setReplaceAll(useEmoji == "true")
                    .registerInitCallback(object : InitCallback() {
                        override fun onInitialized() {
                            super.onInitialized()
                            val tf = oldtf
                            oldtf = null
                            setTypeface(tf)
                        }
                    }))
        }
        setTheme(THEME[(pref.getString("theme", null)?.toIntOrNull() ?: 2131492983) - 2131492983])
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setContentView(if (useEmoji == "null") R.layout.main else R.layout.main_emojicompat)
        editText = findViewById<EditText>(R.id.text).also {
            it.setOnTouchListener { view: View, motionEvent: MotionEvent ->
                view.onTouchEvent(motionEvent)
                if (disableime) (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
                true
            }
            it.textSize = fontsize
            it.setOnEditorActionListener { _, _, keyEvent ->
                if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) btnFinish.performClick()
                    true
                } else
                    false
            }
        }
        btnClear = findViewById<ImageButton>(R.id.clear).also {
            it.setOnClickListener {
                editText.setText("")
            }
            it.visibility = if (pref.getBoolean("clear", false)) View.VISIBLE else View.GONE
        }
        findViewById<ImageButton>(R.id.delete).also {
            it.setOnTouchListener { view: View, motionEvent: MotionEvent ->
                view.onTouchEvent(motionEvent)
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> if (delay == null) {
                        delay = Runnable {
                            val str = editText.editableText.toString()
                            if (str.isEmpty()) return@Runnable
                            val start = editText.selectionStart
                            if (start < 1) return@Runnable
                            val end = editText.selectionEnd
                            if (start < 1) return@Runnable
                            if (start != end) editText.editableText.delete(min(start, end), max(start, end)) else if (start > 1 && Character.isSurrogatePair(str[start - 2], str[start - 1])) editText.editableText.delete(start - 2, start) else editText.editableText.delete(start - 1, start)
                            if (delay != null) {
                                editText.postDelayed(delay, timer.toLong())
                                if (timer > 100) timer -= 200
                            }
                        }
                        editText.post(delay)
                    }
                    MotionEvent.ACTION_UP -> {
                        editText.removeCallbacks(delay)
                        delay = null
                        timer = 500
                    }
                }
                true
            }
        }
        findViewById<Button>(R.id.copy).also {
            it.setOnClickListener {
                cm.text = editText.text.toString()
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.find).also {
            it.setOnClickListener {
                val str = editText.editableText.toString()
                if (str.isEmpty()) return@setOnClickListener
                val start = editText.selectionStart
                if (start == -1) return@setOnClickListener
                val end = editText.selectionEnd
                adpPage.showDesc(null, str.codePointCount(0, if (start == end) if (start == 0) 0 else start - 1 else min(start, end)), adpPage.adapterEdit)
            }
        }
        findViewById<Button>(R.id.paste).also {
            it.setOnClickListener {
                editText.setText(cm.text)
            }
        }
        btnFinish = findViewById<Button>(R.id.finish).also {
            it.setOnClickListener {
                when {
                    action == ACTION_INTERCEPT -> {
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(REPLACE_KEY, editText.text.toString())
                        })
                        finish()
                    }
                    Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT -> {
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(Intent.EXTRA_PROCESS_TEXT, editText.text)
                        })
                        finish()
                    }
                    else -> {
                        startActivity(Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, editText.text.toString())
                        })
                    }
                }
            }
        }
        chooser = FontChooser(this, findViewById<View>(R.id.font) as Spinner, object : FontChooser.Listener {
            override fun onTypefaceChosen(typeface: Typeface?) {
                setTypeface(typeface)
            }
        })
        scroll = findViewById(R.id.scrollView)
        pager = findViewById(R.id.cpager)
        pager.offscreenPageLimit = 3
        adpPage = PageAdapter(this, pref, editText).also {
            pager.adapter = it
            scroll.setAdapter(it)
        }
        scroll.setLockView(pager, (pref.getString("scroll", null)?.toIntOrNull() ?: 1) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1)
        cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        disableime = pref.getBoolean("ime", true)
        pager.setCurrentItem(min(pref.getInt("page", 1), adpPage.count - 1), false)
        val it = intent
        action = it.action
        when {
            action == ACTION_INTERCEPT -> it.getStringExtra(REPLACE_KEY)
            action == Intent.ACTION_SEND -> it.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT -> it.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }?.let { editText.setText(it) }
        if (action == ACTION_INTERCEPT || (Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT && !it.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false))) {
            editText.imeOptions = EditorInfo.IME_ACTION_DONE
            btnFinish.setText(R.string.finish)
        } else {
            editText.imeOptions = EditorInfo.IME_ACTION_SEND
            btnFinish.setText(R.string.share)
            action = null
        }
        adCompat.renderAdToContainer(this, pref)
        created = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        scroll.setLockView(pager, (pref.getString("scroll", null)?.toIntOrNull() ?: 1) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val actionItem = menu.add("Setting")
        MenuItemCompat.setShowAsAction(actionItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS)
        actionItem.setIcon(android.R.drawable.ic_menu_preferences)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        startActivityForResult(Intent(this, SettingActivity::class.java), 0)
        return true
    }

    public override fun onPause() {
        val edit = pref.edit()
        adpPage.save(edit)
        chooser.save(edit)
        edit.putInt("page", pager.currentItem)
        edit.apply()
        super.onPause()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_MENU && e.action == KeyEvent.ACTION_UP) {
            startActivityForResult(Intent(this, SettingActivity::class.java), 0)
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FontChooser.FONT_REQUEST_CODE) if (resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            var name = uri.path ?: return
            while (name.endsWith("/")) name = name.substring(0, name.length - 1)
            if (name.contains("/")) name = name.substring(name.lastIndexOf("/") + 1)
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
            name.replace("[?:\"*|/\\\\<>]".toRegex(), "_")
            try {
                (contentResolver.openInputStream(uri) ?: throw IOException()).use { `is` ->
                    val of = File(filesDir, "00000000/$name")
                    of.parentFile?.mkdirs()
                    FileOutputStream(of).use { os ->
                        val crc = CRC32()
                        val buf = ByteArray(256)
                        var size: Int
                        while (`is`.read(buf).also { size = it } > 0) {
                            os.write(buf, 0, size)
                            crc.update(buf, 0, size)
                        }
                        val mf = File(filesDir, String.format("%08x", crc.value) + "/" + name)
                        mf.parentFile?.mkdirs()
                        of.renameTo(mf)
                        chooser.onFileChosen(mf.canonicalPath)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else chooser.onFileCancel()
        if (requestCode != -1) super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_FIRST_USER) {
            val intent = Intent()
            intent.setClassName(packageName, RestartActivity::class.java.name)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(PID_KEY, Process.myPid())
            startActivity(intent)
            finish()
            return
        }
        fontsize = pref.getString("textsize", null)?.toFloatOrNull() ?: 24f
        univer = pref.getString("universion", "Latest")?.replace(".", "")?.toIntOrNull() ?: Int.MAX_VALUE
        PageAdapter.column = pref.getString("column", null)?.toIntOrNull() ?: 8
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) PageAdapter.column = pref.getString("columnl", null)?.toIntOrNull() ?: PageAdapter.column
        UnicodeAdapter.padding = pref.getString("padding", null)?.toIntOrNull() ?: 4
        UnicodeAdapter.fontsize = pref.getString("gridsize", null)?.toFloatOrNull() ?: 24f
        CharacterAdapter.fontsize = pref.getString("viewsize", null)?.toFloatOrNull() ?: 120f
        CharacterAdapter.checker = pref.getString("checker", null)?.toFloatOrNull() ?: 15f
        CharacterAdapter.lines = pref.getBoolean("lines", true)
        UnicodeAdapter.shrink = pref.getBoolean("shrink", true)
        CharacterAdapter.shrink = pref.getBoolean("shrink", true)
        RecentAdapter.maxitems = pref.getString("recentsize", null)?.toIntOrNull() ?: 256
        disableime = pref.getBoolean("ime", true)
        if (created) {
            btnClear.visibility = if (pref.getBoolean("clear", false)) View.VISIBLE else View.GONE
            editText.textSize = fontsize
            adpPage.notifyDataSetChanged()
            scroll.setLockView(pager, (pref.getString("scroll", null)?.toIntOrNull() ?: 1) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1)
        }
        if (requestCode != -1) {
            adCompat.renderAdToContainer(this, pref)
        }
    }

    fun setPage(page: Int) {
        pager.currentItem = page
    }

    private var oldtf: Typeface? = null
    private fun setTypeface(tf: Typeface?) {
        if (tf === oldtf) return
        oldtf = tf
        editText.typeface = tf
        adpPage.setTypeface(tf)
    }

    companion object {
        private const val ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT"
        private const val REPLACE_KEY = "replace_key"
        private const val PID_KEY = "pid_key"
        private val THEME = intArrayOf(
                R.style.Theme,
                R.style.Theme_Light,
                R.style.Theme_Light_DarkActionBar)
        private var fontsize = 24.0f
        internal var univer = 1000
    }
}