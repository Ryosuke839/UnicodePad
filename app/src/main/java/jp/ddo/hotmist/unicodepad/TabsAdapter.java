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

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

public class TabsAdapter extends BaseAdapter implements OnClickListener, DragSortListView.DropListener
{
	private final int NUM_TABS = 6;
	private int shownnum;

	private ArrayList<Integer> idx;
	private ArrayList<Boolean> single;

	private Activity activity;
	private AbsListView list;
	private SharedPreferences pref;

	private final String[] KEYS = {"rec", "list", "emoji", "find", "fav", "edt"};
	private final int[] RESS = {R.string.recent, R.string.list, R.string.emoji, R.string.find, R.string.favorite, R.string.edit};
	private final boolean[] DEFS = {false, false, false, true, false, true};

	TabsAdapter(Activity activity, AbsListView list)
	{
		this.activity = activity;
		this.list = list;
		pref = PreferenceManager.getDefaultSharedPreferences(activity);

		shownnum = pref.getInt("cnt_shown", NUM_TABS);

		idx = new ArrayList<Integer>(NUM_TABS + 2);
		for (int i = 0; i < NUM_TABS + 2; ++i)
			idx.add(-2);

		single = new ArrayList<Boolean>(NUM_TABS);
		for (int i = 0; i < NUM_TABS; ++i)
			single.add(false);

		idx.set(0, -1);
		idx.set(shownnum + 1, -1);

		for (int i = 0; i < NUM_TABS; ++i)
		{
			int ord = pref.getInt("ord_" + KEYS[i], i) + 1;
			if (ord > shownnum)
				++ord;
			if (idx.get(ord) == -2)
				idx.set(ord, i);
			else
				for (int j = 0; j < NUM_TABS + 2; ++j)
					if (idx.get(j) == -2)
					{
						idx.set(j, i);
						break;
					}
			single.set(i, Boolean.valueOf(pref.getString("single_" + KEYS[i], Boolean.valueOf(DEFS[i]).toString())));
		}
	}

	@Override
	public int getCount()
	{
		return NUM_TABS + 2;
	}

	@Override
	public int getViewTypeCount()
	{
		return 2;
	}

	@Override
	public int getItemViewType(int position)
	{
		if (position == 0)
			return 0;
		if (position == shownnum + 1)
			return 0;
		return 1;
	}

	@Override
	public Object getItem(int i)
	{
		return null;
	}

	@Override
	public long getItemId(int i)
	{
		return idx.get(i);
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		if (getItemViewType(i) == 0)
		{
			if (view == null)
			{
				view = new TextView(activity);
			}
			((TextView)view).setText(i == 0 ? R.string.shown_desc : R.string.hidden_desc);
		} else
		{
			if (view == null)
			{
				view = activity.getLayoutInflater().inflate(R.layout.spinwidget, null);
				view.findViewById(R.id.tabs_multiple).setOnClickListener(this);
				view.findViewById(R.id.tabs_single).setOnClickListener(this);
			}
			((TextView)view.findViewById(R.id.tabs_title)).setText(RESS[idx.get(i)]);
			((RadioButton)view.findViewById(R.id.tabs_multiple)).setChecked(!single.get(idx.get(i)));
			view.findViewById(R.id.tabs_multiple).setTag(idx.get(i));
			((RadioButton)view.findViewById(R.id.tabs_single)).setChecked(single.get(idx.get(i)));
			view.findViewById(R.id.tabs_single).setTag(idx.get(i));
		}
		return view;
	}

	@Override
	public void onClick(View view)
	{
		int i = (Integer)view.getTag();
		single.set(i, view.getId() == R.id.tabs_single);

		Editor edit = pref.edit();
		edit.putString("single_" + KEYS[i], Boolean.valueOf(single.get(i)).toString());
		edit.apply();
	}

	@Override
	public void drop(int from, int to)
	{
		if (to == 0)
			return;
		if (from < shownnum + 1)
			--shownnum;
		if (shownnum == 0)
		{
			++shownnum;
			return;
		}
		if (to <= shownnum + 1)
			++shownnum;
		idx.add(to, idx.remove(from));

		Editor edit = pref.edit();
		edit.putInt("cnt_shown", shownnum);
		for (int i = 1; i < NUM_TABS + 2; ++i)
		{
			int ord = i - 1;
			if (ord == shownnum)
				continue;
			if (ord > shownnum)
				--ord;
			edit.putInt("ord_" + KEYS[idx.get(i)], ord);
		}
		edit.apply();

		if (list != null)
			list.invalidateViews();
	}
}
