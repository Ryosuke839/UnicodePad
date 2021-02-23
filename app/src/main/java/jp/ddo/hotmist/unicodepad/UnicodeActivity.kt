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
import android.os.Bundle
import android.os.Process
import androidx.preference.PreferenceManager
import android.provider.OpenableColumns
import android.text.*
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.FontRequest
import androidx.core.view.MenuItemCompat
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.EmojiCompat.InitCallback
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.zip.CRC32
import kotlin.math.max
import kotlin.math.min

class UnicodeActivity : AppCompatActivity(), View.OnClickListener, OnTouchListener, OnEditorActionListener, FontChooser.Listener {
    private var isMush = false
    private var created = false
    private lateinit var editText: EditText
    private lateinit var btnClear: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var btnCopy: Button
    private lateinit var btnFind: Button
    private lateinit var btnPaste: Button
    private lateinit var btnFinish: Button
    var chooser: FontChooser? = null
    private lateinit var scroll: LockableScrollView
    private lateinit var pager: ViewPager
    var adpPage: PageAdapter? = null
    private var adView: AdView? = null
    private var cm: ClipboardManager? = null
    private lateinit var pref: SharedPreferences
    private var disableime = false
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
        val themelist = intArrayOf(
                R.style.Theme,
                R.style.Theme_Light,
                R.style.Theme_Light_DarkActionBar)
        setTheme(themelist[Integer.valueOf(pref.getString("theme", "2131492983")!!) - 2131492983])
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setContentView(if (useEmoji == "null") R.layout.main else R.layout.main_emojicompat)
        editText = findViewById(R.id.text)
        editText.setOnTouchListener(this)
        editText.textSize = fontsize
        btnClear = findViewById(R.id.clear)
        btnClear.setOnClickListener(this)
        btnClear.visibility = if (pref.getBoolean("clear", false)) View.VISIBLE else View.GONE
        btnDelete = findViewById(R.id.delete)
        btnDelete.setOnTouchListener(this)
        btnCopy = findViewById(R.id.copy)
        btnCopy.setOnClickListener(this)
        btnFind = findViewById(R.id.find)
        btnFind.setOnClickListener(this)
        btnPaste = findViewById(R.id.paste)
        btnPaste.setOnClickListener(this)
        btnFinish = findViewById(R.id.finish)
        btnFinish.setOnClickListener(this)
        chooser = FontChooser(this, findViewById<View>(R.id.font) as Spinner, this)
        scroll = findViewById(R.id.scrollView)
        pager = findViewById(R.id.cpager)
        pager.offscreenPageLimit = 3
        adpPage = PageAdapter(this, pref, editText).also {
            pager.adapter = it
            scroll.setAdapter(it)
        }
        scroll.setLockView(pager, Integer.valueOf(pref.getString("scroll", "1")!!) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1)
        cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        disableime = pref.getBoolean("ime", true)
        pager.setCurrentItem(min(pref.getInt("page", 1), adpPage!!.count - 1), false)
        val it = intent
        val action = it.action
        editText.imeOptions = if (action != null && ACTION_INTERCEPT == action) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_SEND
        if (action != null && ACTION_INTERCEPT == action) {
            isMush = true
            val str = it.getStringExtra(REPLACE_KEY)
            if (str != null) editText.append(str)
            btnFinish.setText(R.string.finish)
        } else {
            isMush = false
            btnFinish.setText(R.string.share)
        }
        if (action != null && Intent.ACTION_SEND == action) {
            val str = it.getStringExtra(Intent.EXTRA_TEXT)
            if (str != null) editText.append(str)
        }
        if (!pref.getBoolean("no-ad", false)) {
            try {
                MobileAds.initialize(this) { }
                adView = AdView(this).also {
                    val outMetrics = DisplayMetrics()
                    windowManager.defaultDisplay.getMetrics(outMetrics)
                    it.adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, (outMetrics.widthPixels / outMetrics.density).toInt())
                    it.adUnitId = "ca-app-pub-8779692709020298/6882844952"
                    findViewById<LinearLayout>(R.id.adContainer).addView(adView)
                    val adRequest = AdRequest.Builder().build()
                    it.loadAd(adRequest)
                }
            } catch (e: NullPointerException) {
            }
        }
        created = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        scroll.setLockView(pager, Integer.valueOf(pref.getString("scroll", "1")!!) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1)
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
        adpPage!!.save(edit)
        chooser!!.Save(edit)
        edit.putInt("page", pager.currentItem)
        edit.apply()
        super.onPause()
    }

    override fun onClick(v: View) {
        if (v === btnClear) {
            editText.setText("")
        }
        if (v === btnCopy) {
            cm!!.text = editText.text.toString()
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }
        if (v === btnFind) {
            val str = editText.editableText.toString()
            if (str.isEmpty()) return
            val start = editText.selectionStart
            if (start == -1) return
            val end = editText.selectionEnd
            adpPage!!.showDesc(null, str.codePointCount(0, if (start == end) if (start == 0) 0 else start - 1 else min(start, end)), adpPage!!.aedt)
        }
        if (v === btnPaste) {
            editText.setText(cm!!.text)
        }
        if (v === btnFinish) {
            if (isMush) {
                replace(editText.text.toString())
            } else {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, editText.text.toString())
                startActivity(intent)
            }
        }
    }

    private var delay: Runnable? = null
    private var timer = 500
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.onTouchEvent(event)
        if (v === btnDelete) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> if (delay == null) {
                    delay = Runnable {
                        val str = editText.editableText.toString()
                        if (str.isEmpty()) return@Runnable
                        val start = editText.selectionStart
                        if (start < 1) return@Runnable
                        val end = editText.selectionEnd
                        if (start < 1) return@Runnable
                        if (start != end) editText.editableText.delete(min(start, end), max(start, end)) else if (start > 1 && Character.isSurrogatePair(str[start - 2], str[start - 1])) editText.editableText.delete(start - 2, start) else editText.editableText.delete(start - 1, start)
                        //							editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
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
        }
        if (v === editText) {
            if (disableime) (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.getWindowToken(), 0)
        }
        return true
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
            val uri = data.data
            var name = uri!!.path
            while (name!!.endsWith("/")) name = name.substring(0, name.length - 1)
            if (name.contains("/")) name = name.substring(name.lastIndexOf("/") + 1)
            val cursor = contentResolver.query(data.data!!, null, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                cursor.close()
            }
            name!!.replace("[?:\"*|/\\\\<>]".toRegex(), "_")
            try {
                val `is` = contentResolver.openInputStream(uri)
                val of = File(filesDir, "00000000/$name")
                of.parentFile.mkdirs()
                try {
                    val os: OutputStream = FileOutputStream(of)
                    val crc = CRC32()
                    val buf = ByteArray(256)
                    var size: Int
                    while (`is`!!.read(buf).also { size = it } > 0) {
                        os.write(buf, 0, size)
                        crc.update(buf, 0, size)
                    }
                    os.close()
                    val mf = File(filesDir, String.format("%08x", crc.value) + "/" + name)
                    mf.parentFile.mkdirs()
                    of.renameTo(mf)
                    chooser!!.onFileChosen(mf.canonicalPath)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                `is`!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else chooser!!.onFileCancel()
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
        try {
            fontsize = java.lang.Float.valueOf(pref.getString("textsize", "24.0")!!)
        } catch (e: NumberFormatException) {
        }
        try {
            univer = Integer.valueOf(pref.getString("universion", "Latest")!!.replace(".", ""))
        } catch (e: NumberFormatException) {
            univer = Int.MAX_VALUE
        }
        try {
            PageAdapter.column = Integer.valueOf(pref.getString("column", "8")!!)
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) PageAdapter.column = Integer.valueOf(pref.getString("columnl", PageAdapter.column.toString())!!)
        } catch (e: NumberFormatException) {
        }
        try {
            UnicodeAdapter.padding = Integer.valueOf(pref.getString("padding", "4")!!)
        } catch (e: NumberFormatException) {
        }
        try {
            UnicodeAdapter.fontsize = java.lang.Float.valueOf(pref.getString("gridsize", "24.0")!!)
        } catch (e: NumberFormatException) {
        }
        try {
            CharacterAdapter.fontsize = java.lang.Float.valueOf(pref.getString("viewsize", "120.0")!!)
        } catch (e: NumberFormatException) {
        }
        try {
            CharacterAdapter.checker = java.lang.Float.valueOf(pref.getString("checker", "15.0")!!)
        } catch (e: NumberFormatException) {
        }
        CharacterAdapter.lines = pref.getBoolean("lines", true)
        UnicodeAdapter.shrink = pref.getBoolean("shrink", true)
        CharacterAdapter.shrink = pref.getBoolean("shrink", true)
        try {
            RecentAdapter.maxitems = Integer.valueOf(pref.getString("recentsize", "256")!!)
        } catch (e: NumberFormatException) {
        }
        disableime = pref.getBoolean("ime", true)
        if (created) {
            btnClear.visibility = if (pref.getBoolean("clear", false)) View.VISIBLE else View.GONE
            editText.textSize = fontsize
            if (adpPage != null) adpPage!!.notifyDataSetChanged()
            scroll.setLockView(pager, Integer.valueOf(pref.getString("scroll", "1")!!) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1)
        }
        if (requestCode != -1) {
            val adContainer = findViewById<LinearLayout>(R.id.adContainer)
            if (adContainer != null) {
                if (!pref.getBoolean("no-ad", false)) {
                    if (adContainer.childCount == 0) {
                        MobileAds.initialize(this) { }
                        adView = AdView(this)
                        val outMetrics = DisplayMetrics()
                        windowManager.defaultDisplay.getMetrics(outMetrics)
                        adView!!.adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, (outMetrics.widthPixels / outMetrics.density).toInt())
                        adView!!.adUnitId = "ca-app-pub-8779692709020298/6882844952"
                        (findViewById<View>(R.id.adContainer) as LinearLayout).addView(adView)
                        val adRequest = AdRequest.Builder().build()
                        adView!!.loadAd(adRequest)
                    }
                } else {
                    if (adContainer.childCount > 0) {
                        adContainer.removeAllViews()
                    }
                }
            }
        }
    }

    private fun replace(result: String) {
        val data = Intent()
        data.putExtra(REPLACE_KEY, result)
        setResult(RESULT_OK, data)
        finish()
    }

    fun setPage(page: Int) {
        pager.currentItem = page
    }

    override fun onTypefaceChosen(typeface: Typeface?) {
        setTypeface(typeface)
    }

    private var oldtf: Typeface? = null
    private fun setTypeface(tf: Typeface?) {
        if (tf === oldtf) return
        oldtf = tf
        editText.typeface = tf
        adpPage!!.setTypeface(tf)
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (v == editText && event.keyCode == KeyEvent.KEYCODE_ENTER) {
            if (event.action == KeyEvent.ACTION_DOWN) btnFinish.performClick()
            return true
        }
        return false
    }

    companion object {
        private const val ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT"
        private const val REPLACE_KEY = "replace_key"
        private const val PID_KEY = "pid_key"
        private var fontsize = 24.0f
        var univer = 1000
    }
}