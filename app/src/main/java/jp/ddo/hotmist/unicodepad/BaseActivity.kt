package jp.ddo.hotmist.unicodepad

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeFromPref().also { currentTheme = it })
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (currentTheme != getThemeFromPref()) {
            recreate()
        }
    }
}