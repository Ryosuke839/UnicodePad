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
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		int[] themelist =
				{
						android.support.v7.appcompat.R.style.Theme_AppCompat,
						android.support.v7.appcompat.R.style.Theme_AppCompat_Light,
						android.support.v7.appcompat.R.style.Theme_AppCompat_Light_DarkActionBar,
				};
		setTheme(themelist[Integer.valueOf(pref.getString("theme", "2131492983")) - 2131492983]);
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.setting);

		ListPreference univer = (ListPreference)findPreference("universion");
		univer.setOnPreferenceChangeListener(this);
		univer.setSummary(univer.getEntry());

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
			if (key.equals("theme"))
				Toast.makeText(this, R.string.theme_title, Toast.LENGTH_SHORT).show();
		}
		arg0.setSummary(arg0 instanceof ListPreference ? ((ListPreference)arg0).getEntries()[((ListPreference)arg0).findIndexOfValue(arg1.toString())] : arg1.toString());
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference arg0)
	{
		String key = arg0.getKey();
		if (key.equals("download"))
		{
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/get/noto/"));
			if (this.getPackageManager().queryIntentActivities(intent, 0).size() > 0)
			{
				// Show webpage
				startActivity(intent);
			}
			else
			{
				// Copy URI
				((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setText("https://www.google.com/get/noto/");
				Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		if (key.equals("tabs"))
		{
			startActivity(new Intent(this, TabsActivity.class));
			return true;
		}

		return false;
	}

}
