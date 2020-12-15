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

package jp.ddo.hotmist.unicodepad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.text.ClipboardManager;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class SettingActivity extends PreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener
{
	SharedPreferences pref;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		int[] themelist =
				{
						androidx.appcompat.R.style.Theme_AppCompat,
						androidx.appcompat.R.style.Theme_AppCompat_Light,
						androidx.appcompat.R.style.Theme_AppCompat_Light_DarkActionBar,
				};
		setTheme(themelist[Integer.valueOf(pref.getString("theme", "2131492983")) - 2131492983]);
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.setting);

		ListPreference univer = (ListPreference)findPreference("universion");
		univer.setOnPreferenceChangeListener(this);
		univer.setSummary(univer.getEntry());

		ListPreference emojicompat = (ListPreference)findPreference("emojicompat");
		emojicompat.setOnPreferenceChangeListener(this);
		emojicompat.setSummary(emojicompat.getEntry());

		Preference download = findPreference("download");
		download.setOnPreferenceClickListener(this);

		ListPreference theme = (ListPreference)findPreference("theme");
		theme.setOnPreferenceChangeListener(this);
		theme.setSummary(theme.getEntry());

		EditTextPreference textsize = (EditTextPreference)findPreference("textsize");
		textsize.setOnPreferenceChangeListener(this);
		textsize.setSummary(textsize.getText());

		ListPreference column = (ListPreference)findPreference("column");
		column.setOnPreferenceChangeListener(this);
		column.setSummary(column.getValue());

		ListPreference columnl = (ListPreference)findPreference("columnl");
		columnl.setOnPreferenceChangeListener(this);
		columnl.setSummary(columnl.getValue());

		Preference tabs = findPreference("tabs");
		tabs.setOnPreferenceClickListener(this);

		Preference clearRecents = findPreference("clear_recents");
		clearRecents.setOnPreferenceClickListener(this);

		Preference clearFavorites = findPreference("clear_favorites");
		clearFavorites.setOnPreferenceClickListener(this);

		EditTextPreference padding = (EditTextPreference)findPreference("padding");
		padding.setOnPreferenceChangeListener(this);
		padding.setSummary(padding.getText());

		EditTextPreference gridsize = (EditTextPreference)findPreference("gridsize");
		gridsize.setOnPreferenceChangeListener(this);
		gridsize.setSummary(gridsize.getText());

		EditTextPreference viewsize = (EditTextPreference)findPreference("viewsize");
		viewsize.setOnPreferenceChangeListener(this);
		viewsize.setSummary(viewsize.getText());

		EditTextPreference checker = (EditTextPreference)findPreference("checker");
		checker.setOnPreferenceChangeListener(this);
		checker.setSummary(checker.getText());

		EditTextPreference recentsize = (EditTextPreference)findPreference("recentsize");
		recentsize.setOnPreferenceChangeListener(this);
		recentsize.setSummary(recentsize.getText());

		ListPreference scroll = (ListPreference)findPreference("scroll");
		scroll.setOnPreferenceChangeListener(this);
		scroll.setSummary(scroll.getEntry());

		Preference legal_app = findPreference("legal_app");
		legal_app.setOnPreferenceClickListener(this);

		Preference legal_uni = findPreference("legal_uni");
		legal_uni.setOnPreferenceClickListener(this);

		setResult(RESULT_OK);
	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1)
	{
		if (arg0.hasKey())
		{
			String key = arg0.getKey();
			try
			{
				if (key.equals("column") ||
						key.equals("padding") ||
						key.equals("recentsize"))
					//noinspection ResultOfMethodCallIgnored: check string with exception
					Integer.valueOf(arg1.toString());
				if (key.equals("textsize") ||
						key.equals("gridsize") ||
						key.equals("viewsize") ||
						key.equals("checker"))
					//noinspection ResultOfMethodCallIgnored: check string with exception
					Float.valueOf(arg1.toString());
			}
			catch (NumberFormatException e)
			{
				return false;
			}
			if (key.equals("theme") || key.equals("emojicompat"))
			{
				Toast.makeText(this, R.string.theme_title, Toast.LENGTH_SHORT).show();
				setResult(RESULT_FIRST_USER);
			}
		}
		arg0.setSummary(arg0 instanceof ListPreference ? ((ListPreference)arg0).getEntries()[((ListPreference)arg0).findIndexOfValue(arg1.toString())] : arg1.toString());
		return true;
	}

	private boolean openPage(String uri)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		if (this.getPackageManager().queryIntentActivities(intent, 0).size() > 0)
		{
			// Show webpage
			startActivity(intent);
		}
		else
		{
			// Copy URI
			((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setText(uri);
			Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
		}
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference arg0)
	{
		String key = arg0.getKey();
		if (key.equals("download"))
		{
			return openPage(getString(R.string.download_uri));
		}
		if (key.equals("tabs"))
		{
			startActivity(new Intent(this, TabsActivity.class));
			return true;
		}
		if (key.equals("clear_recents"))
		{
			Toast.makeText(this, "Recents Cleared", Toast.LENGTH_SHORT).show();
			pref.edit().putString("rec", "").apply();
			startActivityForResult(new Intent(this, UnicodeActivity.class), -3);
			return true;
		}
		if (key.equals("clear_favorites"))
		{
			Toast.makeText(this, "Favorites Cleared", Toast.LENGTH_SHORT).show();
			pref.edit().putString("fav", "").apply();
			startActivityForResult(new Intent(this, UnicodeActivity.class), -4);
			return true;
		}
		if (key.equals("legal_app"))
		{
			return openPage("https://github.com/Ryosuke839/UnicodePad");
		}
		if (key.equals("legal_uni"))
		{
			return openPage("https://unicode.org/");
		}

		return false;
	}

}
