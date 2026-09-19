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
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.setMargins
import androidx.core.view.updatePadding
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.InitCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerTabStrip
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.ViewPagerBottomSheetBehavior
import smartdevelop.ir.eram.showcaseviewlib.GuideView
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import androidx.core.content.edit
import androidx.core.view.children


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
    private lateinit var sessionStore: SessionStore
    private var action: String? = null
    private var created = false
    private var disableime = false
    private var delay: Runnable? = null
    private var timer = 500
    private var applyingSession = false
    private var pendingSelection: Pair<Int, Int>? = null
    private var showChooserOnStart = false
    private val viewTargets = mutableMapOf<Int, View>()
    private val composed = mutableStateOf(false)
    @SuppressLint("ClickableViewAccessibility")
    public override fun onCreate(savedInstanceState: Bundle?) {
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        sessionStore = SessionStore(pref)
        sessionStore.load()
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
        }).apply {
            load(pref)
        }
        locale = LocaleChooser(this@UnicodeActivity, Spinner(this), object : LocaleChooser.Listener {
            override fun onLocaleChosen(locale: Locale) {
                setTypeface(oldtf, locale)
            }
        })

        cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        disableime = pref.getBoolean("ime", true)
        resolveLaunchSession(savedInstanceState)

        setContentView(ComposeView(this).apply {
            consumeWindowInsets = false
            setContent {
                val density = LocalDensity.current
                var toolBarHeight by remember {
                    mutableStateOf(0.dp)
                }
                var toolBarHeightOverride by remember {
                    mutableStateOf<Dp?>(null)
                }
                var editTextHeight by remember {
                    mutableStateOf(0.dp)
                }

                Box(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).fillMaxHeight(),
                ) {
                    Column(
                        modifier = Modifier.zIndex(1.0f)
                    ) {
                        AndroidView(
                            factory = { toolbar },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    with (density) {
                                        if (toolBarHeightOverride == null) {
                                            toolBarHeight = (coordinates.size.height - toolbar.paddingTop).toDp()
                                        }
                                    }
                                }.run {
                                    toolBarHeightOverride?.let { height ->
                                        this.height(height)
                                    } ?: this
                                },
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    with (density) {
                                        editTextHeight = coordinates.size.height.toDp()
                                    }
                                }
                                .background(Color(TypedValue().also { tv ->
                                    theme.resolveAttribute(android.R.attr.colorBackground, tv, true)
                                }.data)),
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
                                                    if (applyingSession) return
                                                    val text = s.toString()
                                                    if (text == sessionStore.current.text) {
                                                        return
                                                    }
                                                    sessionStore.recordEdit(text, selectionStart, selectionEnd)
                                                    updateUndoRedoMenu()
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
                                                applyingSession = true
                                                setText(initialText)
                                                applyPendingSelection(this)
                                                initialText = null
                                                applyingSession = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (showBtnClear) {
                                    AndroidView(
                                        factory = { context -> ImageButton(context).apply {
                                            setImageResource(TypedValue().also { value ->
                                                context.theme.resolveAttribute(R.attr.cancel, value, true)
                                            }.resourceId)
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
                                    setImageResource(TypedValue().also { value ->
                                        context.theme.resolveAttribute(R.attr.backspace, value, true)
                                    }.resourceId)
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
                    }
                    @Composable
                    fun MainView() {
                        Column {
                            Spacer(
                                Modifier.height(toolBarHeight),
                            )
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
                                            var pos = if (start == end) if (start == 0) 0 else start - 1 else min(start, end)
                                            var i = 0
                                            while (pos > 0) {
                                                pos -= adpPage.adapterEdit.getItem(i++).length
                                            }
                                            if (pos < 0) i--
                                            adpPage.showDesc(null, i, adpPage.adapterEdit)
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
                                            copyText(showToast = Build.VERSION.SDK_INT <= 32)
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
                                                    shareText()
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
                                        ViewCompat.setOnApplyWindowInsetsListener(it) { v, windowInsets ->
                                            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
                                            adpPage.onInsetChanged(insets.bottom)
                                            WindowInsetsCompat.CONSUMED
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    AndroidView(
                        factory = { context -> CoordinatorLayout(context).apply {
                            addView(LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                addView(if (scrollUi) {
                                    LockableScrollView(context).also {
                                        scroll = it
                                        it.addView(ComposeView(it.context).apply {
                                            setContent {
                                                MainView()
                                            }
                                        })
                                        it.clipToOutline = true
                                        it.setOnScrollListener(object : LockableScrollView.OnScrollListener {
                                            override fun onScroll(remainingY: Int) {
                                                val insetTop = density.run { toolbar.paddingTop.toDp() }
                                                val remainingDp = density.run { remainingY.toDp() }
                                                toolBarHeightOverride = if (remainingDp < toolBarHeight) {
                                                    insetTop + remainingDp
                                                } else {
                                                    null
                                                }
                                            }
                                        })
                                    }
                                } else {
                                    scroll = null
                                    ComposeView(context).apply {
                                        setContent {
                                            MainView()
                                        }
                                    }
                                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                            })
                            if (adCompat.showAdSettings) {
                                addView(LinearLayout(context).apply {
                                    id = R.id.adContainer
                                    orientation = LinearLayout.VERTICAL
                                    gravity = Gravity.BOTTOM
                                    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
                                        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
                                        v.setPadding(0, 0, 0, insets.bottom)
                                        WindowInsetsCompat.CONSUMED
                                    }
                                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
                            }
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
                                    ViewCompat.setOnApplyWindowInsetsListener(bottomSheetView) { v, windowInsets ->
                                        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
                                        v.updatePadding(0, 0, 0, insets.bottom)
                                        WindowInsetsCompat.CONSUMED
                                    }
                                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                                setOnTouchListener { _, _ -> true }
                            }, CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, getSystem().displayMetrics.heightPixels / 2).apply {
                                behavior = ViewPagerBottomSheetBehavior<View>().apply {
                                    isHideable = true
                                    state = BottomSheetBehavior.STATE_HIDDEN
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
                        update = {
                            if (adCompat.showAdSettings) {
                                val height = adCompat.renderAdToContainer(this@UnicodeActivity, pref)
                                adpPage.onAdHeightChanged((height * getSystem().displayMetrics.density).toInt())
                            }
                        },
                        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)).padding(0.dp, editTextHeight, 0.dp, 0.dp),
                    )
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
        })

        if (intent.action == ACTION_PASTE) {
            val view = findViewById<View>(android.R.id.content).rootView
            // the ClipboardManager text becomes valid when the view is in focus.
            view.doOnLayout {
                sessionStore.startNew(cm.text?.toString() ?: "")
                applyCurrentSessionToEditor()
            }
        }
        if (showChooserOnStart) {
            showSessionHistory(atLaunch = true)
        }
        created = true
    }

    private fun resolveLaunchSession(savedInstanceState: Bundle?) {
        val it = intent
        action = it.action
        val specialLaunch = action == ACTION_INTERCEPT
                || action == Intent.ACTION_SEND
                || action == ACTION_PASTE
                || (Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT)
        if (savedInstanceState == null) {
            if (specialLaunch) {
                sessionStore.startNew(when {
                    action == ACTION_PASTE -> null
                    action == ACTION_INTERCEPT -> it.getStringExtra(REPLACE_KEY)
                    action == Intent.ACTION_SEND -> it.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT -> it.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                    else -> null
                } ?: "")
            } else {
                when (pref.getString(SessionStore.PREF_STARTUP, SessionStore.STARTUP_PREVIOUS)) {
                    SessionStore.STARTUP_NEW -> sessionStore.startNew()
                    SessionStore.STARTUP_CHOOSER -> showChooserOnStart = !sessionStore.isEmpty
                }
            }
        }
        val current = sessionStore.current
        initialText = current.text
        pendingSelection = current.selStart to current.selEnd
        if (action == ACTION_INTERCEPT || (Build.VERSION.SDK_INT >= 23 && action == Intent.ACTION_PROCESS_TEXT && !it.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false))) {
            finishAction = R.string.finish
        } else {
            finishAction = R.string.share
            action = null
        }
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
            R.id.action_bar -> toolbar.children.last().let {
                (it as? ViewGroup)?.children?.last() ?: it
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
        menu.add(1, MENU_ID_SESSION, MENU_ID_SESSION, R.string.sessions).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
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
        updateUndoRedoMenu()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_ID_SETTING -> startActivityForResult(Intent(this, SettingActivity::class.java), 0)
            MENU_ID_SESSION -> showSessionHistory()
            MENU_ID_UNDO -> {
                val cur = sessionStore.current
                if (cur.cursor > 0) {
                    applyingSession = true
                    cur.cursor -= 1
                    applyHistoryEntry(cur)
                    applyingSession = false
                    updateUndoRedoMenu()
                }
            }
            MENU_ID_REDO -> {
                val cur = sessionStore.current
                if (cur.cursor < cur.history.lastIndex) {
                    applyingSession = true
                    cur.cursor += 1
                    applyHistoryEntry(cur)
                    applyingSession = false
                    updateUndoRedoMenu()
                }
            }
            MENU_ID_PASTE -> {
                sessionStore.startNew(cm.text?.toString() ?: "")
                applyCurrentSessionToEditor()
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
                var pos = if (start == end) if (start == 0) 0 else start - 1 else min(start, end)
                var i = 0
                while (pos > 0) {
                    pos -= adpPage.adapterEdit.getItem(i++).length
                }
                if (pos < 0) i--
                adpPage.showDesc(null, i, adpPage.adapterEdit)
            }
            MENU_ID_COPY -> copyText(showToast = true)
            MENU_ID_SHARE -> shareText()
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
        saveState()
        super.onPause()
    }

    fun saveState() {
        pref.edit {
            adpPage.save(this)
            chooser.save(this)
            locale.save(this)
            putInt("page", pager.currentItem)
            sessionStore.save(this)
        }
    }

    private fun updateUndoRedoMenu() {
        if (!::itemUndo.isInitialized) return
        val cur = sessionStore.current
        itemUndo.isEnabled = cur.cursor > 0
        itemRedo.isEnabled = cur.cursor < cur.history.lastIndex
    }

    private fun applyPendingSelection(et: EditText) {
        val sel = pendingSelection
        pendingSelection = null
        val len = et.length()
        if (sel != null) {
            et.setSelection(sel.first.coerceIn(0, len), sel.second.coerceIn(0, len))
        } else {
            et.setSelection(len)
        }
    }

    private fun applyHistoryEntry(session: EditSession = sessionStore.current) {
        val entry = session.history[session.cursor]
        editText.setText(entry.text)
        val len = editText.length()
        editText.setSelection(entry.selStart.coerceIn(0, len), entry.selEnd.coerceIn(0, len))
    }

    private fun applyCurrentSessionToEditor() {
        val session = sessionStore.current
        applyingSession = true
        pendingSelection = session.selStart to session.selEnd
        if (editText.parent != null) {
            editText.setText(session.text)
            applyPendingSelection(editText)
            initialText = null
        } else {
            initialText = session.text
        }
        applyingSession = false
        updateUndoRedoMenu()
    }

    private fun copyText(showToast: Boolean) {
        cm.text = editText.text.toString()
        sessionStore.armBranch(SessionMark.COPIED)
        if (showToast) {
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareText() {
        sessionStore.armBranch(SessionMark.SHARED)
        startActivity(Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, editText.text.toString())
        })
    }

    private fun showSessionHistory(atLaunch: Boolean = false) {
        val list = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(context)
        }
        val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.sessions)
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .setView(list)
                .create()
        list.adapter = SessionListAdapter(sessionStore, atLaunch) { session ->
            dialog.dismiss()
            if (session !== sessionStore.current) {
                if (session == null) {
                    sessionStore.startNew()
                } else {
                    sessionStore.branch(session)
                }
                applyCurrentSessionToEditor()
            }
        }
        dialog.show()
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
        val newScrollUi = (pref.getString("scroll", null)?.toIntOrNull() ?: 1) + (if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 0) > 1
        if (created) {
            if (scrollUi != newScrollUi) {
                saveState()
                recreate()
                return
            }
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
            chooser.load(pref)
        }
        scrollUi = newScrollUi
        if (requestCode != -1) {
            val height = adCompat.renderAdToContainer(this, pref)
            adpPage.onAdHeightChanged((height * getSystem().displayMetrics.density).toInt())
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
        fun visit(view: View) {
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    visit(view.getChildAt(i))
                }
            } else if (view is CharacterView) {
                view.setTypeface(tf, locale)
            }
        }
        visit(bottomSheetView)
    }

    companion object {
        private const val ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT"
        private const val ACTION_PASTE = "jp.ddo.hotmist.unicodepad.intent.action.PASTE"
        private const val REPLACE_KEY = "replace_key"
        private const val PID_KEY = "pid_key"
        private const val MENU_ID_SETTING = 45
        private const val MENU_ID_SESSION = 12
        private const val MENU_ID_UNDO = 13
        private const val MENU_ID_REDO = 14
        private const val MENU_ID_PASTE = 15
        private const val MENU_ID_CONVERT = 16
        private const val MENU_ID_DESC = 25
        private const val MENU_ID_COPY = 35
        private const val MENU_ID_SHARE = 36
        private const val MENU_ID_SEND = 37
        private var fontsize = 24.0f
        internal var univer = 1000
    }
}