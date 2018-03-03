package jp.ddo.hotmist.unicodepad;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class TabsActivity extends PreferenceActivity implements OnClickListener, OnPreferenceChangeListener
{
	private final int NUM_TABS = 6;
	private SharedPreferences pref;
	private PreferenceCategory catshown;
	private PreferenceCategory cathidden;
	private ButtonListPreference single[] = new ButtonListPreference[NUM_TABS];
	private int shownnum = 6;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		pref = PreferenceManager.getDefaultSharedPreferences(this);

		shownnum = pref.getInt("cnt_shown", 6);

		PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);

		catshown = new PreferenceCategory(this);
		catshown.setTitle(R.string.shown_desc);
		catshown.setOrder(0);
		screen.addPreference(catshown);

		cathidden = new PreferenceCategory(this);
		cathidden.setTitle(R.string.hidden_desc);
		cathidden.setOrder(shownnum * 2);
		screen.addPreference(cathidden);

		ButtonListPreference single_rec = new ButtonListPreference(this);
		single_rec.setKey("single_rec");
		single_rec.setDefaultValue("false");
		single_rec.setTitle(R.string.recent);
		single_rec.setDialogTitle(R.string.single_rec_desc);
		single[pref.getInt("ord_rec", 0)] = single_rec;

		ButtonListPreference single_list = new ButtonListPreference(this);
		single_list.setKey("single_list");
		single_list.setDefaultValue("false");
		single_list.setTitle(R.string.list);
		single_list.setDialogTitle(R.string.single_list_desc);
		single[pref.getInt("ord_list", 1)] = single_list;

		ButtonListPreference single_emoji = new ButtonListPreference(this);
		single_emoji.setKey("single_emoji");
		single_emoji.setDefaultValue("false");
		single_emoji.setTitle(R.string.emoji);
		single_emoji.setDialogTitle(R.string.single_emoji_desc);
		single[pref.getInt("ord_emoji", 2)] = single_emoji;

		ButtonListPreference single_find = new ButtonListPreference(this);
		single_find.setKey("single_find");
		single_find.setDefaultValue("true");
		single_find.setTitle(R.string.find);
		single_find.setDialogTitle(R.string.single_find_desc);
		single[pref.getInt("ord_find", 3)] = single_find;

		ButtonListPreference single_fav = new ButtonListPreference(this);
		single_fav.setKey("single_fav");
		single_fav.setDefaultValue("false");
		single_fav.setTitle(R.string.favorite);
		single_fav.setDialogTitle(R.string.single_fav_desc);
		single[pref.getInt("ord_fav", 4)] = single_fav;

		ButtonListPreference single_edt = new ButtonListPreference(this);
		single_edt.setKey("single_edt");
		single_edt.setDefaultValue("true");
		single_edt.setTitle(R.string.edit);
		single_edt.setDialogTitle(R.string.single_edt_desc);
		single[pref.getInt("ord_edt", 5)] = single_edt;

		for (int i = 0; i < NUM_TABS; ++i)
		{
			single[i].setOnPreferenceChangeListener(this);
			single[i].setEntries(R.array.single);
			single[i].setEntryValues(R.array.single_val);
			single[i].setOrder(i * 2 + 1);
			screen.addPreference(single[i]);
		}

		setPreferenceScreen(screen);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		for (int i = 0; i < NUM_TABS; ++i)
		{
			single[i].setSummary(single[i].getEntry());
			single[i].setTag(Integer.valueOf(i));
			single[i].setOnClickListener(this);
			if (i == 0)
				single[i].setEnabled(false, true);
			if (i == NUM_TABS - 1 && shownnum != NUM_TABS)
				single[i].setEnabled(true, false);
		}
	}

	@Override
	public void onClick(View arg0)
	{
		int idx;
		ButtonListPreference tmp;
		Editor edit;

		switch (arg0.getId())
		{
		case R.id.buttonup:
			idx = (Integer)arg0.getTag();
			if (idx == shownnum)
			{
				cathidden.setOrder(++shownnum * 2);
				if (idx == NUM_TABS - 1)
					single[idx].setEnabled(true, true);
				edit = pref.edit();
				edit.putInt("cnt_shown", shownnum);
				edit.commit();
				break;
			}
			if (idx == 0)
				break;
			tmp = single[idx];
			single[idx] = single[idx - 1];
			single[idx - 1] = tmp;
			single[idx - 1].setOrder(idx * 2 - 1);
			single[idx].setOrder(idx * 2 + 1);
			single[idx - 1].setTag(Integer.valueOf(idx - 1));
			single[idx].setTag(Integer.valueOf(idx));
			edit = pref.edit();
			edit.putInt(single[idx - 1].getKey().replace("single_", "ord_"), idx - 1);
			edit.putInt(single[idx].getKey().replace("single_", "ord_"), idx);
			edit.commit();
			if (idx != 1)
				break;
			single[0].setEnabled(false, true);
			single[1].setEnabled(true, true);
			break;
		case R.id.buttondown:
			idx = (Integer)arg0.getTag();
			if (idx == shownnum - 1)
			{
				if (single[idx].getKey().equals("single_list"))
				{
					Toast.makeText(this, R.string.list_title, Toast.LENGTH_SHORT).show();
					break;
				}
				cathidden.setOrder(--shownnum * 2);
				if (idx == NUM_TABS - 1)
					single[idx].setEnabled(true, false);
				edit = pref.edit();
				edit.putInt("cnt_shown", shownnum);
				edit.commit();
				break;
			}
			if (idx == NUM_TABS - 1)
				break;
			tmp = single[idx];
			single[idx] = single[idx + 1];
			single[idx + 1] = tmp;
			single[idx + 1].setOrder(idx * 2 + 3);
			single[idx].setOrder(idx * 2 + 1);
			single[idx + 1].setTag(Integer.valueOf(idx + 1));
			single[idx].setTag(Integer.valueOf(idx));
			edit = pref.edit();
			edit.putInt(single[idx + 1].getKey().replace("single_", "ord_"), idx + 1);
			edit.putInt(single[idx].getKey().replace("single_", "ord_"), idx);
			edit.commit();
			if (idx != NUM_TABS - 2 || shownnum == NUM_TABS)
				break;
			single[NUM_TABS - 2].setEnabled(true, true);
			single[NUM_TABS - 1].setEnabled(true, false);
			break;
		}
	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1)
	{
		arg0.setSummary(arg0 instanceof ListPreference ? ((ListPreference)arg0).getEntries()[((ListPreference)arg0).findIndexOfValue(arg1.toString())] : arg1.toString());
		return true;
	}
}
