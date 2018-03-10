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

import android.content.SharedPreferences;

class RecentAdapter extends UnicodeAdapter
{
	private ArrayList<Integer> list;
	private ArrayList<Integer> temp;
	static int maxitems = 16;

	RecentAdapter(SharedPreferences pref, NameDatabase db, boolean single)
	{
		super(db, single);

		list = new ArrayList<>();
		temp = list;
		String str = pref.getString("rec", "");
		int num = 0;
		for (int i = 0; i < str.length(); ++i)
		{
			if (str.codePointAt(i) > 0xFFFF)
				++i;
			++num;
		}
		for (int i = 0; i < str.length(); ++i)
		{
			int code = str.codePointAt(i);
			if (code > 0xFFFF)
				++i;
			if (--num < maxitems)
				list.add(code);
		}
	}

	@Override
	int name()
	{
		return R.string.recent;
	}

	@Override
	void show()
	{
		trunc();
		if (grid != null)
			grid.invalidateViews();
	}

	@Override
	void leave()
	{
		commit();
		if (grid != null)
			grid.invalidateViews();
	}

	@Override
	public int getCount()
	{
		return temp.size();
	}

	@Override
	public long getItemId(int arg0)
	{
		return temp.get(temp.size() - arg0 - 1);
	}

	void add(int code)
	{
		list.remove(Integer.valueOf(code));
		list.add(code);
		if (list.size() >= maxitems)
			list.remove(0);
	}

	void remove(int code)
	{
		list.remove(Integer.valueOf(code));
		if (list != temp)
			temp.remove(Integer.valueOf(code));

		if (grid != null)
			grid.invalidateViews();
	}

	private void commit()
	{
		if (list != temp)
			temp = list;
	}

	private void trunc()
	{
		if (list == temp)
			temp = new ArrayList<>(list);
	}

	@Override
	void save(SharedPreferences.Editor edit)
	{
		String str = "";
		for (Integer i : list)
			str += String.valueOf(Character.toChars(i));
		edit.putString("rec", str);
	}

}
