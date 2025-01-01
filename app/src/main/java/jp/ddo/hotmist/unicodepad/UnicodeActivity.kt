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
import android.content.res.Resources.getSystem
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.provider.OpenableColumns
import android.text.*
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.doOnLayout
import androidx.core.view.setMargins
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.InitCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerTabStrip
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.ViewPagerBottomSheetBehavior
import smartdevelop.ir.eram.showcaseviewlib.GuideView
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.zip.CRC32
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min


@Suppress("DEPRECATION")
class UnicodeActivity : BaseActivity() {
    private lateinit var editText: EditText
    private var initialText by mutableStateOf<String?>(null)
    private var showBtnClear by mutableStateOf(false)
    private var showBtnRow by mutableStateOf(true)
    private var finishAction by mutableIntStateOf(R.string.finish)
    private lateinit var btnFinish: Button
    private lateinit var chooser: FontChooser
    private lateinit var locale: LocaleChooser
    private var scrollUi by mutableStateOf(false)
    private var scroll: LockableScrollView? = null
    private lateinit var pager: ViewPager
    internal lateinit var adpPage: PageAdapter
    private lateinit var bottomSheetBehavior: ViewPagerBottomSheetBehavior<View>
    private lateinit var bottomSheetView: ViewGroup
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
    private val viewTargets = mutableMapOf<Int, View>()
    private val composed = mutableStateOf(false)
    @SuppressLint("ClickableViewAccessibility")
    public override fun onCreate(savedInstanceState: Bundle?) {
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        onActivityResult(-1, 0, null)
        val useEmoji = pref.getString("emojicompat", "false")
        if (useEmoji != "null") {
            EmojiCompat.init(BundledEmojiCompatConfig(this)
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
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        editText = if (useEmoji != "null") { AppCompatEditText(this) } else { EditText(this) }
        adpPage = PageAdapter(this, pref, editText)
        pager = LockableViewPager(this).apply {
            addView(PagerTabStrip(this@UnicodeActivity).apply {
                viewTargets[R.id.ctab] = this
            }, ViewPager.LayoutParams().apply {
                width = ViewPager.LayoutParams.MATCH_PARENT
                height = ViewPager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.TOP
                isDecor = true
            })
        }
        chooser = FontChooser(this@UnicodeActivity, Spinner(this).apply {
            viewTargets[R.id.fontBar] = this
        }, object : FontChooser.Listener {
            override fun onTypefaceChosen(typeface: Typeface?) {
                setTypeface(typeface, oldlocale)
            }
        })
        locale = LocaleChooser(this@UnicodeActivity, Spinner(this), object : LocaleChooser.Listener {
            override fun onLocaleChosen(locale: Locale) {
                setTypeface(oldtf, locale)
            }
        })

        setContent {
            Column(
                modifier = Modifier.fillMaxHeight(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    val multiline = pref.getBoolean("multiline", false)
                    Box(
                        modifier = Modifier.weight(1f).heightIn(max = fontsize.dp * 4),
                    ) {
                        AndroidView(
                            factory = {
                                editText.apply {
                                    id = R.id.editText
                                    setOnTouchListener { view: View, motionEvent: MotionEvent ->
                                        view.onTouchEvent(motionEvent)
                                        if (disableime) (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                                            view.windowToken,
                                            0
                                        )
                                        true
                                    }
                                    textSize = fontsize
                                    maxLines = if (multiline) 3 else 1
                                    inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
                                    setOnEditorActionListener { _, actionId, keyEvent ->
                                        if (keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN && !multiline || actionId == EditorInfo.IME_ACTION_DONE) {
                                            btnFinish.performClick()
                                            true
                                        } else
                                            false
                                    }
                                    addTextChangedListener(object : TextWatcher {
                                        override fun beforeTextChanged(
                                            s: CharSequence?,
                                            start: Int,
                                            count: Int,
                                            after: Int
                                        ) {
                                        }

                                        override fun onTextChanged(
                                            s: CharSequence?,
                                            start: Int,
                                            before: Int,
                                            count: Int
                                        ) {
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
                                            history.add(
                                                Triple(
                                                    s.toString(),
                                                    selectionStart,
                                                    selectionEnd
                                                )
                                            )
                                            historyCursor = history.size - 1
                                            itemUndo.isEnabled = historyCursor > 0
                                            itemRedo.isEnabled = false
                                        }
                                    })
                                    requestFocus()
                                }
                            },
                            update = {
                                it.apply {
                                    imeOptions = when (finishAction) {
                                        R.string.finish -> EditorInfo.IME_ACTION_DONE
                                        else -> EditorInfo.IME_ACTION_SEND
                                    }
                                    if (initialText != null) {
                                        setText(initialText)
                                        setSelection(length())
                                        initialText = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (showBtnClear) {
                            AndroidView(
                                factory = { context -> ImageButton(context).apply {
                                    setImageResource(R.drawable.ic_action_cancel)
                                    contentDescription = resources.getString(R.string.clear)
                                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                                    setOnClickListener {
                                        editText.setText("")
                                    }
                                    TypedValue().also { value ->
                                        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true)
                                        background = AppCompatResources.getDrawable(context, value.resourceId)
                                    }
                                } },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp),
                            )
                        }
                    }
                    AndroidView(
                        factory = { context -> ImageButton(context).apply {
                            setImageResource(R.drawable.ic_action_backspace)
                            contentDescription = resources.getString(R.string.erase)
                            scaleType = ImageView.ScaleType.CENTER_INSIDE
                            cropToPadding = false
                            setOnTouchListener { view, motionEvent ->
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
                        } },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .height(fontsize.dp * 2),
                    )
                }
                @Composable
                fun MainView() {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = if (showBtnRow) Modifier.fillMaxWidth() else Modifier.height(0.dp),
                        ) {
                            AndroidView(
                                factory = { context -> Button(context, null, android.R.attr.buttonBarButtonStyle).apply {
                                    text = resources.getText(android.R.string.paste)
                                } },
                                update = {
                                    it.setOnClickListener {
                                        editText.setText(cm.text)
                                        editText.setSelection(editText.length())
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            AndroidView(
                                factory = { context -> Button(context, null, android.R.attr.buttonBarButtonStyle).apply {
                                    text = resources.getText(R.string.desc)
                                } },
                                update = {
                                    it.setOnClickListener {
                                        val str = editText.editableText.toString()
                                        if (str.isEmpty()) return@setOnClickListener
                                        val start = editText.selectionStart
                                        if (start == -1) return@setOnClickListener
                                        val end = editText.selectionEnd
                                        adpPage.adapterEdit.updateString()
                                        adpPage.showDesc(
                                            null,
                                            str.codePointCount(
                                                0,
                                                if (start == end) if (start == 0) 0 else start - 1 else min(
                                                    start,
                                                    end
                                                )
                                            ),
                                            adpPage.adapterEdit
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            AndroidView(
                                factory = { context -> Button(context, null, android.R.attr.buttonBarButtonStyle).apply {
                                    text = resources.getText(android.R.string.copy)
                                } },
                                update = {
                                    it.setOnClickListener {
                                        cm.text = editText.text.toString()
                                        if (Build.VERSION.SDK_INT <= 32) {
                                            Toast.makeText(
                                                this@UnicodeActivity,
                                                R.string.copied,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            AndroidView(
                                factory = { context -> Button(context, null, android.R.attr.buttonBarButtonStyle).apply {
                                    btnFinish = this
                                    viewTargets[R.id.finish] = this
                                    text = resources.getText(finishAction)
                                } },
                                update = {
                                    it.setOnClickListener {
                                        when {
                                            action == ACTION_INTERCEPT -> {
                                                setResult(RESULT_OK, Intent().apply {
                                                    putExtra(
                                                        REPLACE_KEY,
                                                        editText.text.toString()
                                                    )
                                                })
                                                finish()
                                            }

                                            Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT -> {
                                                setResult(RESULT_OK, Intent().apply {
                                                    putExtra(
                                                        Intent.EXTRA_PROCESS_TEXT,
                                                        editText.text
                                                    )
                                                })
                                                finish()
                                            }

                                            else -> {
                                                startActivity(Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        editText.text.toString()
                                                    )
                                                })
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AndroidView(
                                factory = { context -> TextView(context, null, android.R.attr.textAppearanceSmall).apply {
                                    text = resources.getText(R.string.font)
                                } },
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 8.dp),
                            )
                            AndroidView(
                                factory = { chooser.spinner },
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .weight(2f),
                            )
                            AndroidView(
                                factory = { locale.spinner },
                                modifier = Modifier.align(Alignment.CenterVertically).weight(1f),
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        ) {
                            Column {
                                AndroidView(
                                    factory = { context -> View(context).apply {
                                        viewTargets[R.id.cpager] = this
                                    } },
                                    modifier = Modifier.fillMaxWidth().weight(0.5f),
                                )
                                AndroidView(
                                    factory = { context -> View(context) },
                                    modifier = Modifier.fillMaxWidth().weight(0.5f),
                                )
                            }
                            AndroidView(
                                factory = { pager },
                                update = {
                                    pager.offscreenPageLimit = 3
                                    adpPage.also { adp ->
                                        pager.adapter = adp
                                        scroll?.setAdapter(adp)
                                    }
                                    scroll?.setLockView(pager, true)
                                    pager.setCurrentItem(min(pref.getInt("page", 1), adpPage.count - 1), false)
                                    it.adapter = adpPage
                                    it.setCurrentItem(min(pref.getInt("page", 1), adpPage.count - 1), false)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                AndroidView(
                    factory = { context -> CoordinatorLayout(context).apply {
                        addView(if (scrollUi) {
                            LockableScrollView(context).also {
                                    scroll = it
                                    it.addView(ComposeView(it.context).apply {
                                        setContent {
                                            MainView()
                                        }
                                    })
                                    it.clipToOutline = true
                                }
                        } else {
                            scroll = null
                            ComposeView(context).apply {
                                setContent {
                                    MainView()
                                }
                            }
                        })
                        addView(LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            setBackgroundResource(R.drawable.bottom_sheet_background)
                            elevation = 30f
                            addView(ImageView(context).apply {
                                setImageResource(R.drawable.bottom_sheet_bar)
                            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (4 * getSystem().displayMetrics.density).toInt()).apply {
                                setMargins((6 * getSystem().displayMetrics.density).toInt())
                                gravity = Gravity.CENTER
                            })
                            addView(LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                bottomSheetView = this
                            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                            setOnTouchListener { _, _ -> true }
                        }, CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, getSystem().displayMetrics.heightPixels / 2).apply {
                            behavior = ViewPagerBottomSheetBehavior<View>().apply {
                                state = BottomSheetBehavior.STATE_HIDDEN
                                isHideable = true
                                bottomSheetBehavior = this
                            }.also { behavior ->
                                val bottomSheetBackCallback = object : OnBackPressedCallback(true) {
                                    override fun handleOnBackPressed() {
                                        if (behavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                                            behavior.state = BottomSheetBehavior.STATE_HIDDEN
                                        } else {
                                            isEnabled = false
                                            onBackPressed()
                                        }
                                    }
                                }
                                bottomSheetBackCallback.isEnabled = false
                                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                                        bottomSheetBackCallback.isEnabled = newState != BottomSheetBehavior.STATE_HIDDEN
                                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                                            bottomSheetView.removeAllViews()
                                        } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                                            behavior.state = BottomSheetBehavior.STATE_HIDDEN
                                        }
                                    }

                                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                                    }
                                })
                                onBackPressedDispatcher.addCallback(this@UnicodeActivity, bottomSheetBackCallback)
                            }
                        })
                    }},
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                if (adCompat.showAdSettings) {
                    AndroidView(
                        factory = { context -> LinearLayout(context).apply {
                            id = R.id.adContainer
                            orientation = LinearLayout.VERTICAL
                        } },
                        update = {
                            adCompat.renderAdToContainer(this@UnicodeActivity, pref)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            LaunchedEffect(Unit) {
                composed.value = true
            }

            if (composed.value) {
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            if (!pref.getBoolean("skip_guide", false)) {
                                showGuide()
                            }
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
            }
        }

        cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        disableime = pref.getBoolean("ime", true)
        val it = intent
        action = it.action
        // handle the paste home screen shortcut
        if (action == "jp.ddo.hotmist.unicodepad.intent.action.PASTE") {
            val view = findViewById<View>(android.R.id.content).rootView
            // the ClipboardManager text becomes valid when the view is in focus.
            view.doOnLayout {
                history[0] = Triple(cm.text?.toString() ?: "", 0, 0)
                initialText = cm.text?.toString()
            }
        }
        when {
            action == ACTION_INTERCEPT -> it.getStringExtra(REPLACE_KEY)
            action == Intent.ACTION_SEND -> it.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT -> it.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }?.let {
            history[0] = Triple(it, 0, 0)
            initialText = it
        }
        if (action == ACTION_INTERCEPT || (Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT && !it.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false))) {
            finishAction = R.string.finish
        } else {
            finishAction = R.string.share
            action = null
        }
        created = true
    }

    fun setBottomSheetContent(view: View, ua: UnicodeAdapter?) {
        if (ua != null) {
            val observer = object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }

                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }

                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
            ua.registerDataObserver(observer)
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}
                override fun onViewDetachedFromWindow(v: View) {
                    ua.unregisterDataObserver(observer)
                }
            })
        }
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetView.removeAllViews()
        bottomSheetView.addView(view)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }


    private fun showGuide(index: Int = 0) {
        val titles = resources.getStringArray(R.array.guide_titles)
        val contents = resources.getStringArray(R.array.guide_contents)
        val targets = resources.obtainTypedArray(R.array.guide_targets).run {
            val ids = (0 until length()).map { getResourceIdOrThrow(it) }
            recycle()
            ids
        }
        val count = min(min(titles.size, contents.size), targets.size)
        if (index >= count || pref.getBoolean("skip_guide", false)) {
            pref.edit().putBoolean("skip_guide", true).apply()
            return
        }
        val targetView = when (targets[index]) {
            R.id.action_bar -> findViewById<ViewGroup>(targets[index]).run {
                (getChildAt(childCount - 1) as ViewGroup).run {
                    getChildAt(childCount - 1)
                }
            }
            else -> viewTargets[targets[index]]!!
        }
        GuideView.Builder(this)
                .setTitle(titles[index])
                .setContentText(contents[index])
                .setDismissType(DismissType.anywhere)
                .setTargetView(targetView)
                .setGuideListener {
                    showGuide(index + 1)
                }
                .build().also { guide ->
                    Button(this).let { button ->
                        button.text = resources.getText(
                                if (index != count - 1)
                                    R.string.guide_next
                                else
                                    R.string.guide_finish)
                        button.setOnClickListener {
                            guide.dismiss()
                        }
                        guide.addView(button, 0, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            gravity = Gravity.BOTTOM or Gravity.END
                            marginEnd = button.paddingEnd
                            bottomMargin = guide.navigationBarSize + button.paddingBottom
                        })
                    }
                    if (index != count - 1) {
                        Button(this).let { button ->
                            button.text = resources.getText(R.string.guide_skip)
                            button.setOnClickListener {
                                pref.edit().putBoolean("skip_guide", true).apply()
                                guide.dismiss()
                            }
                            guide.addView(button, 0, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                gravity = Gravity.BOTTOM or Gravity.START
                                marginStart = button.paddingStart
                                bottomMargin = guide.navigationBarSize + button.paddingBottom
                            })
                        }
                    }
                }
                .show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        scrollUi = (pref.getString("scroll", null)?.toIntOrNull() ?: 1) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1
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
            MENU_ID_PASTE -> {
                editText.setText(cm.text)
                editText.setSelection(editText.length())
            }
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
                val dialog = AlertDialog.Builder(this).setTitle(R.string.convert_).setNegativeButton(android.R.string.cancel) { _, _ -> }.setAdapter(adapter) { _, i -> editText.setText(adapter.getItem(i)?.second) }.show()
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) {
            startActivityForResult(Intent(this, SettingActivity::class.java), 0)
            return true
        }
        return super.dispatchKeyEvent(event)
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
        showBtnClear = pref.getBoolean("clear", false)
        showBtnRow = pref.getBoolean("buttons", true)
        scrollUi = (pref.getString("scroll", null)?.toIntOrNull() ?: 1) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1
        if (created) {
            editText.textSize = fontsize
            adpPage.notifyDataSetChanged()
            editText.apply {
                val multiline = pref.getBoolean("multiline", false)
                editText.maxLines = if (multiline) 3 else 1
                editText.inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
                setOnEditorActionListener { _, actionId, keyEvent ->
                    if (keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN && !multiline || actionId == EditorInfo.IME_ACTION_DONE) {
                        btnFinish.performClick()
                        true
                    } else
                        false
                }
            }
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
        editText.textLocale = locale
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
        private var fontsize = 24.0f
        internal var univer = 1000
    }
}