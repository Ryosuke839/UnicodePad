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

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemSelectedListener;

class EmojiAdapter extends UnicodeAdapter implements OnItemSelectedListener, OnScrollListener, OnCheckedChangeListener
{
	private Cursor cur;
	private LinearLayout layout;
	private Spinner jump;
	private NameDatabase db;
	private NavigableMap<Integer, Integer> map;
	private List<String> grp;
	private List<Integer> idx;
	private int current;
	private boolean modifier;

	public EmojiAdapter(SharedPreferences pref, NameDatabase db, boolean single)
	{
		super(db, single);

		this.db = db;
		cur = null;
		current = pref.getInt("emoji", 0);
		modifier = pref.getBoolean("modifier", true);
	}

	@Override
	int name()
	{
		return R.string.emoji;
	}

	@SuppressLint("InlinedApi")
	@Override
	View instantiate(GridView grd)
	{
		super.instantiate(grd);

		layout = new LinearLayout(grid.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout hl = new LinearLayout(grid.getContext());
		hl.setOrientation(LinearLayout.HORIZONTAL);
		hl.setGravity(Gravity.CENTER);
		jump = new Spinner(grid.getContext());
		hl.addView(jump, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.f));
		CheckBox modc = new CheckBox(grid.getContext());
		modc.setText(R.string.modifier);
		modc.setPadding(0, 0, (int)(grd.getContext().getResources().getDisplayMetrics().density * 8.f), 0);
		hl.addView(modc, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		if (Build.VERSION.SDK_INT >= 21)
			hl.setPadding(0, (int)(grd.getContext().getResources().getDisplayMetrics().density * 8.f), 0, (int)(grd.getContext().getResources().getDisplayMetrics().density * 8.f));
		layout.addView(hl, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(grid, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));

		cur = db.emoji(UnicodeActivity.univer, modifier);

		map = new TreeMap<>();
		grp = new ArrayList<>();
		idx = new ArrayList<>();

		String last = "";
		for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext())
		{
			String curr = cur.getString(1) + " / " + cur.getString(2);
			if (curr.equals(last))
				continue;
			last = curr;
			map.put(cur.getPosition(), map.size());
			grp.add(curr);
			idx.add(cur.getPosition());
		}
		if (current >= grp.size())
			current = grp.size() - 1;

		ArrayAdapter<String> adp = new ArrayAdapter<>(jump.getContext(), android.R.layout.simple_spinner_item, grp);
		adp.setDropDownViewResource(R.layout.spinner_drop_down_item);
		jump.setAdapter(adp);
		grid.setSelection(idx.get(current));
		jump.setSelection(current);
		grid.setOnScrollListener(this);
		jump.setOnItemSelectedListener(this);
		modc.setChecked(modifier);
		modc.setOnCheckedChangeListener(this);

		return layout;
	}

	@Override
	void destroy()
	{
		grid.setOnScrollListener(null);
		layout = null;
		jump = null;

		if (cur != null)
			cur.close();
		cur = null;

		map = null;
		grp = null;
		idx = null;

		super.destroy();
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2)
	{
		arg1 = super.getView(arg0, arg1, arg2);
		if (this.single)
			((CharacterView)((LinearLayout)arg1).getChildAt(0)).drawSlash(false);
		else
			((CharacterView)arg1).drawSlash(false);
		return arg1;
	}

	@Override
	void save(SharedPreferences.Editor edit)
	{
		edit.putInt("emoji", current);
		edit.putBoolean("modifier", modifier);
	}

	@Override
	public int getCount()
	{
		return cur != null ? cur.getCount() : 0;
	}

	@Override
	public long getItemId(int arg0)
	{
		return -1;
	}

	@Override
	public String getItemString(int arg0)
	{
		if (cur == null || arg0 < 0 || arg0 >= cur.getCount())
			return "";
		cur.moveToPosition(arg0);
		return cur.getString(0);
	}

	@Override
	public Object getItem(int arg0)
	{
		String[] ss = getItemString(arg0).split(" ");
		String res = "";
		for (String s : ss)
			if (s.length() > 0)
				res += String.valueOf(Character.toChars(Integer.parseInt(s, 16)));
		return res;
	}

	private int guard = 0;

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		if (arg0 == jump)
		{
			current = arg2;
			if (grid != null)
				if (guard == 0)
					grid.setSelection(idx.get(arg2));
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0)
	{
	}

	@Override
	public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3)
	{
		if (arg0 == grid)
		{
			if (grid.getChildAt(0) != null && grid.getChildAt(0).getTop() * -2 > grid.getChildAt(0).getHeight())
				arg1 += single ? 1 : PageAdapter.column;
			if (!single)
				arg1 += PageAdapter.column - 1;

			Entry<Integer, Integer> e = map.floorEntry(arg1);
			if (arg2 != 0)
			{
				if (e != null)
				{
					if (jump != null)
					{
						if (guard == 0 && current != e.getValue())
						{
							current = e.getValue();
							++guard;
							jump.setSelection(e.getValue(), false);
							jump.post(new Runnable()
							{
								@Override
								public void run()
								{
									--guard;
								}
							});
						}
					}
				}
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1)
	{
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1)
	{
		if (modifier == arg1)
			return;

		modifier = arg1;

		++guard;

		grid.setOnScrollListener(null);
		jump.setOnItemSelectedListener(null);
		jump.setAdapter(null);

		if (cur != null)
			cur.close();
		cur = db.emoji(UnicodeActivity.univer, modifier);

		map = new TreeMap<>();
		grp = new ArrayList<>();
		idx = new ArrayList<>();

		String last = "";
		for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext())
		{
			String curr = cur.getString(1) + " / " + cur.getString(2);
			if (curr.equals(last))
				continue;
			last = curr;
			map.put(cur.getPosition(), map.size());
			grp.add(curr);
			idx.add(cur.getPosition());
		}
		if (current >= grp.size())
			current = grp.size() - 1;

		grid.invalidateViews();

		ArrayAdapter<String> adp = new ArrayAdapter<>(jump.getContext(), android.R.layout.simple_spinner_item, grp);
		adp.setDropDownViewResource(R.layout.spinner_drop_down_item);
		jump.setAdapter(adp);
		jump.setSelection(current);
		grid.setSelection(idx.get(current));
		grid.setOnScrollListener(this);
		jump.setOnItemSelectedListener(this);
		grid.post(new Runnable()
		{
			@Override
			public void run()
			{
				--guard;
			}
		});
	}
}
