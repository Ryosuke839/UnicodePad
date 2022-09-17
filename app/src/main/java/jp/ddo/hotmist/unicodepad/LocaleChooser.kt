package jp.ddo.hotmist.unicodepad

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.util.*

class LocaleChooser internal constructor(private val context: Context, private val spinner : Spinner, private val listener : Listener) {
    private var locale: String
    private val locales = mutableListOf(context.resources.getString(R.string.locale_default).format(Locale.getDefault().toString()), "Root", Locale.JAPAN, Locale.KOREA, Locale.CHINA, Locale.TAIWAN, Locale("zh", "HK"), Locale("zh", "MO"), context.resources.getString(R.string.locale_other))
    private val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, locales)
    private val otherPos = locales.lastIndex

    internal interface Listener {
        fun onLocaleChosen(locale: Locale)
    }

    fun save(edit: SharedPreferences.Editor) {
        edit.putString("locale", locale)
    }

    private fun select(locale: String) {
        when (locale) {
            "" -> spinner.setSelection(0)
            "Root" -> spinner.setSelection(1)
            else -> {
                for (i in 2 until locales.size) {
                    if (locales[i].toString() == locale) {
                        spinner.setSelection(i)
                        return
                    }
                }
                while (locales.last() !is String)
                    locales.removeLast()
                locales.add(if (Build.VERSION.SDK_INT >= 21) Locale.forLanguageTag(locale) else {
                    val components = locale.split("_")
                    when (components.size) {
                        1 -> Locale(components[0])
                        2 -> Locale(components[0], components[1])
                        else -> Locale(components[0], components[1], components[2])
                    }
                })
                adapter.notifyDataSetChanged()
                spinner.setSelection(locales.lastIndex)
            }
        }
    }

    init {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        locale = pref.getString("locale", null) ?: ""

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == otherPos) {
                    val adapter = object : ArrayAdapter<Locale>(context, android.R.layout.simple_list_item_2, Locale.getAvailableLocales()) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            return (convertView
                                    ?: (context.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(android.R.layout.simple_list_item_2, parent, false)).apply {
                                findViewById<TextView>(android.R.id.text1).text = getItem(position).toString()
                                findViewById<TextView>(android.R.id.text2).text = getItem(position)?.displayName
                            }
                        }
                    }
                    AlertDialog.Builder(context).setTitle(R.string.locale_title).setAdapter(adapter) { _, i -> select(adapter.getItem(i).toString()) }.setOnCancelListener { select(locale) }.show()
                    return
                }
                locale = if (locales[position] is Locale) locales[position].toString() else if (locales[position].toString() == "Root") "Root" else ""
                listener.onLocaleChosen(locales[position] as? Locale ?: if (locales[position].toString() == "Root") Locale.ROOT else Locale.getDefault())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        select(locale)
    }
}