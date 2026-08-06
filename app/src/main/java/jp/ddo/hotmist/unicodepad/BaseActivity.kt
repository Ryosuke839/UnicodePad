package jp.ddo.hotmist.unicodepad

import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.PreferenceManager

private val THEME = intArrayOf(
    R.style.Theme,
    R.style.Theme_Light,
    R.style.Theme_Light_DarkActionBar,
    R.style.Theme_DayNight
)

abstract class BaseActivity : AppCompatActivity() {
    private var currentTheme = -1
    private fun getThemeFromPref(): Int {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        return THEME[(pref.getString("theme", null)?.toIntOrNull() ?: 2131492983) - 2131492983]
    }

    private lateinit var _toolbar: Toolbar
    val toolbar: Toolbar
        get() = _toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeFromPref().also { currentTheme = it })
        super.onCreate(savedInstanceState)

        (TypedValue().also { tv ->
            theme.resolveAttribute(R.attr.colorPrimary, tv, true)
        }.data).let { color ->
            ColorUtils.calculateLuminance(color).let { intensity ->
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = intensity > 0.5
                    isAppearanceLightNavigationBars = intensity > 0.5
                }
            }
        }

        _toolbar = Toolbar(this).apply {
            setSupportActionBar(this)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setBackgroundColor(TypedValue().also { tv ->
                theme.resolveAttribute(R.attr.colorPrimary, tv, true)
            }.data)
            TypedValue().also { tv ->
                theme.resolveAttribute(R.attr.actionBarTheme, tv, true)
            }.resourceId.let { resId ->
                setTitleTextColor(
                    obtainStyledAttributes(
                        resId,
                        intArrayOf(android.R.attr.textColorPrimary)
                    ).run {
                        val color = getColor(0, 0)
                        recycle()
                        color
                    })
                obtainStyledAttributes(
                    resId,
                    intArrayOf(android.R.attr.textColorSecondary)
                ).run {
                    val color = getColor(0, 0)
                    recycle()
                    color
                }.let { textColorSecondary ->
                    setSubtitleTextColor(textColorSecondary)
                    overflowIcon = overflowIcon?.apply {
                        setTint(textColorSecondary)
                    }
                    navigationIcon = navigationIcon?.apply {
                        setTint(textColorSecondary)
                    }
                }
            }
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            title = resources.getString(R.string.app_name)
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
                v.updatePadding(0, insets.top, 0, 0)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentTheme != getThemeFromPref()) {
            recreate()
        }
    }
}