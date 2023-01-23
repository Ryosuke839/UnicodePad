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
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.provider.OpenableColumns
import android.text.*
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.FontRequest
import androidx.core.view.doOnLayout
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.InitCallback
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.CRC32
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min


@Suppress("DEPRECATION")
class UnicodeActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var btnClear: ImageButton
    private lateinit var btnRow: LinearLayout
    private lateinit var btnFinish: Button
    private lateinit var chooser: FontChooser
    private lateinit var locale: LocaleChooser
    private lateinit var scroll: LockableScrollView
    private lateinit var pager: ViewPager
    internal lateinit var adpPage: PageAdapter
    private lateinit var itemUndo: MenuItem
    private lateinit var itemRedo: MenuItem
    private val adCompat: AdCompat = AdCompatImpl()
    private lateinit var cm: ClipboardManager
    private lateinit var pref: SharedPreferences
    private var action: String? = null
    private var created = false
    private var disableime = false
    private var delay: Runnable? = null
    private var timer = 500
    private val history = mutableListOf(Triple("", 0, 0))
    private var historyCursor = 0
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
                            val locale = oldlocale
                            oldlocale = Locale.ROOT
                            setTypeface(tf, locale)
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
            it.setOnEditorActionListener { _, actionId, keyEvent ->
                if (keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_ACTION_DONE) {
                    btnFinish.performClick()
                    true
                } else
                    false
            }
            it.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (!::itemUndo.isInitialized) {
                        history[0] = Triple(s.toString(), 0, 0)
                        return
                    }
                    if (s.toString() == history[historyCursor].first) {
                        return
                    }
                    while (history.size > historyCursor + 1) {
                        history.removeLast()
                    }
                    while (history.size >= MAX_HISTORY) {
                        history.removeFirst()
                    }
                    history.add(Triple(s.toString(), it.selectionStart, it.selectionEnd))
                    historyCursor = history.size - 1
                    itemUndo.isEnabled = historyCursor > 0
                    itemRedo.isEnabled = false
                }
            })
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
                adpPage.adapterEdit.updateString()
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
        btnClear = findViewById<ImageButton>(R.id.clear).also {
            it.setOnClickListener {
                editText.setText("")
            }
            it.visibility = if (pref.getBoolean("clear", false)) View.VISIBLE else View.GONE
        }
        btnRow = findViewById<LinearLayout>(R.id.buttonBar).also {
            it.visibility = if (pref.getBoolean("buttons", true)) View.VISIBLE else View.GONE
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
        chooser = FontChooser(this, findViewById(R.id.font), object : FontChooser.Listener {
            override fun onTypefaceChosen(typeface: Typeface?) {
                setTypeface(typeface, oldlocale)
            }
        })
        locale = LocaleChooser(this, findViewById(R.id.locale), object : LocaleChooser.Listener {
            override fun onLocaleChosen(locale: Locale) {
                setTypeface(oldtf, locale)
            }
        })
        scroll = findViewById(R.id.scrollView)
        pager = findViewById(R.id.cpager)
        pager.offscreenPageLimit = 3
        adpPage = PageAdapter(this, pref, editText).also {
            pager.adapter = it
            scroll.setAdapter(it)
        }
        scroll.setLockView(pager, (pref.getString("scroll", null)?.toIntOrNull()
                ?: 1) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1)
        cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        disableime = pref.getBoolean("ime", true)
        pager.setCurrentItem(min(pref.getInt("page", 1), adpPage.count - 1), false)
        val it = intent
        action = it.action
        // handle the paste home screen shortcut
        if (action == "jp.ddo.hotmist.unicodepad.intent.action.PASTE") {
            val view = findViewById<View>(android.R.id.content).rootView
            // the ClipboardManager text becomes valid when the view is in focus.
            view.doOnLayout {
                history[0] = Triple(cm.text.toString(), 0, 0)
                editText.setText(cm.text)
            }
        }
        when {
            action == ACTION_INTERCEPT -> it.getStringExtra(REPLACE_KEY)
            action == Intent.ACTION_SEND -> it.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT -> it.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }?.let {
            history[0] = Triple(it, 0, 0)
            editText.setText(it)
        }
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
        scroll.setLockView(pager, (pref.getString("scroll", null)?.toIntOrNull()
                ?: 1) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            menu.setGroupDividerEnabled(true)
        }
        menu.add(4, MENU_ID_SETTING, MENU_ID_SETTING, R.string.data_setting).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER).setIcon(android.R.drawable.ic_menu_preferences)
        itemUndo = menu.add(1, MENU_ID_UNDO, MENU_ID_UNDO, R.string.undo).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setIcon(android.R.drawable.ic_menu_revert).setEnabled(false)
        itemRedo = menu.add(1, MENU_ID_REDO, MENU_ID_REDO, R.string.redo).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER).setEnabled(false)
        menu.add(1, MENU_ID_PASTE, MENU_ID_PASTE, android.R.string.paste).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(1, MENU_ID_CONVERT, MENU_ID_CONVERT, R.string.convert_).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER).setIcon(android.R.drawable.ic_menu_sort_alphabetically)
        menu.add(2, MENU_ID_DESC, MENU_ID_DESC, R.string.desc).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setIcon(android.R.drawable.ic_menu_info_details)
        menu.add(3, MENU_ID_COPY, MENU_ID_COPY, android.R.string.copy).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
        if (action == ACTION_INTERCEPT || (Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT)) {
            menu.add(3, MENU_ID_SHARE, MENU_ID_SHARE, R.string.share).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER).setIcon(android.R.drawable.ic_menu_share)
            menu.add(3, MENU_ID_SEND, MENU_ID_SEND, R.string.finish).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS).setIcon(android.R.drawable.ic_menu_send)
        } else {
            menu.add(3, MENU_ID_SHARE, MENU_ID_SHARE, R.string.share).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS).setIcon(android.R.drawable.ic_menu_share)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_ID_SETTING -> startActivityForResult(Intent(this, SettingActivity::class.java), 0)
            MENU_ID_UNDO -> {
                if (historyCursor > 0) {
                    historyCursor -= 1
                    history[historyCursor].also {
                        editText.setText(it.first)
                        editText.setSelection(it.second, it.third)
                    }
                    itemUndo.isEnabled = historyCursor > 0
                    itemRedo.isEnabled = historyCursor < history.size - 1
                }
            }
            MENU_ID_REDO -> {
                if (historyCursor < history.size - 1) {
                    historyCursor += 1
                    history[historyCursor].also {
                        editText.setText(it.first)
                        editText.setSelection(it.second, it.third)
                    }
                    itemUndo.isEnabled = historyCursor > 0
                    itemRedo.isEnabled = historyCursor < history.size - 1
                }
            }
            MENU_ID_PASTE -> editText.setText(cm.text)
            MENU_ID_CONVERT-> {
                val text = editText.text.toString()
                val adapter = object : ArrayAdapter<Pair<String, String>>(this, android.R.layout.simple_list_item_2, mutableListOf<Pair<String, String>>().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        add(Pair("NFC Normalization", android.icu.text.Normalizer2.getNFCInstance().normalize(text)))
                        add(Pair("NFD Normalization", android.icu.text.Normalizer2.getNFDInstance().normalize(text)))
                        add(Pair("NFKC Normalization", android.icu.text.Normalizer2.getNFKCInstance().normalize(text)))
                        add(Pair("NFKD Normalization", android.icu.text.Normalizer2.getNFKDInstance().normalize(text)))
                        add(Pair("NFKC_Casefold Normalization", android.icu.text.Normalizer2.getNFKCCasefoldInstance().normalize(text)))
                    } else {
                        add(Pair("NFC Normalization", java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC)))
                        add(Pair("NFD Normalization", java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)))
                        add(Pair("NFKC Normalization", java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)))
                        add(Pair("NFKD Normalization", java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKD)))
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        add(Pair("Lower Case", android.icu.text.CaseMap.toLower().apply(null, text)))
                        add(Pair("Upper Case", android.icu.text.CaseMap.toUpper().apply(null, text)))
                        add(Pair("Title Case", android.icu.text.CaseMap.toTitle().apply(null, android.icu.text.BreakIterator.getWordInstance(), text)))
                        add(Pair("Fold Case", android.icu.text.CaseMap.fold().apply(text)))
                    } else {
                        add(Pair("Lower Case", text.lowercase()))
                        add(Pair("Upper Case", text.uppercase()))
                    }
                }) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        return (convertView
                                ?: (context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(android.R.layout.simple_list_item_2, parent, false)).apply {
                            val elem = getItem(position)
                            findViewById<TextView>(android.R.id.text1).text = elem?.first
                            findViewById<TextView>(android.R.id.text2).text = elem?.second
                        }
                    }
                }
                val dialog = AlertDialog.Builder(this).setTitle(R.string.convert_).setAdapter(adapter) { _, i -> editText.setText(adapter.getItem(i)?.second) }.show()
                val handler = Handler()
                thread {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        for (id in android.icu.text.Transliterator.getAvailableIDs()) {
                            if (!dialog.isShowing) return@thread
                            val converted = android.icu.text.Transliterator.getInstance(id).transliterate(text)
                            if (converted == text) continue
                            handler.post {
                                adapter.add(Pair(android.icu.text.Transliterator.getDisplayName(id), converted))
                            }
                        }
                    }
                    handler.post {
                        dialog.setTitle(R.string.convert)
                    }
                }
            }
            MENU_ID_DESC -> run {
                val str = editText.editableText.toString()
                if (str.isEmpty()) return@run
                val start = editText.selectionStart
                if (start == -1) return@run
                val end = editText.selectionEnd
                adpPage.adapterEdit.updateString()
                adpPage.showDesc(null, str.codePointCount(0, if (start == end) if (start == 0) 0 else start - 1 else min(start, end)), adpPage.adapterEdit)
            }
            MENU_ID_COPY -> {
                cm.text = editText.text.toString()
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
            }
            MENU_ID_SHARE -> {
                startActivity(Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, editText.text.toString())
                })
            }
            MENU_ID_SEND -> when {
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
            }
        }
        return true
    }

    public override fun onPause() {
        val edit = pref.edit()
        adpPage.save(edit)
        chooser.save(edit)
        locale.save(edit)
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FontChooser.FONT_REQUEST_CODE) if (resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            var name = uri.path ?: return
            while (name.endsWith("/")) name = name.substring(0, name.length - 1)
            if (name.contains("/")) name = name.substring(name.lastIndexOf("/") + 1)
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
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
            btnRow.visibility = if (pref.getBoolean("buttons", true)) View.VISIBLE else View.GONE
            editText.textSize = fontsize
            adpPage.notifyDataSetChanged()
            scroll.setLockView(pager, (pref.getString("scroll", null)?.toIntOrNull()
                    ?: 1) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1)
        }
        if (requestCode != -1) {
            adCompat.renderAdToContainer(this, pref)
        }
    }

    fun setPage(page: Int) {
        pager.currentItem = page
    }

    private var oldtf: Typeface? = null
    private var oldlocale = Locale.ROOT
    private fun setTypeface(tf: Typeface?, locale: Locale) {
        if (tf === oldtf && locale == oldlocale) return
        oldtf = tf
        oldlocale = locale
        editText.typeface = tf
        if (Build.VERSION.SDK_INT >= 17) {
            editText.textLocale = locale
        }
        adpPage.setTypeface(tf, locale)
    }

    companion object {
        private const val ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT"
        private const val REPLACE_KEY = "replace_key"
        private const val PID_KEY = "pid_key"
        private const val MENU_ID_SETTING = 45
        private const val MENU_ID_UNDO = 13
        private const val MENU_ID_REDO = 14
        private const val MENU_ID_PASTE = 15
        private const val MENU_ID_CONVERT = 16
        private const val MENU_ID_DESC = 25
        private const val MENU_ID_COPY = 35
        private const val MENU_ID_SHARE = 36
        private const val MENU_ID_SEND = 37
        private const val MAX_HISTORY = 256
        private val THEME = intArrayOf(
                R.style.Theme,
                R.style.Theme_Light,
                R.style.Theme_Light_DarkActionBar)
        private var fontsize = 24.0f
        internal var univer = 1000
    }
}