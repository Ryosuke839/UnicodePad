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

import android.app.Activity;
import android.content.SharedPreferences;

import com.mobeta.android.dslv.DragSortListView;

class FavoriteAdapter extends UnicodeAdapter implements DragSortListView.DropListener, DragSortListView.RemoveListener
{
	private ArrayList<Integer> list;
	private ArrayList<Integer> temp;

	FavoriteAdapter(Activity activity, SharedPreferences pref, NameDatabase db, boolean single)
	{
		super(activity, db, single);

		list = new ArrayList<>();
		temp = list;
		String str = pref.getString("fav", "");
		for (int i = 0; i < str.length(); ++i)
		{
			int code = str.codePointAt(i);
			if (code > 0xFFFF)
				++i;
			list.add(code);
		}
	}

	@Override
	int name()
	{
		return R.string.favorite;
	}

	@Override
	void show()
	{
		trunc();
		if (view != null)
			view.invalidateViews();
	}

	@Override
	void leave()
	{
		commit();
		if (view != null)
			view.invalidateViews();
	}

	@Override
	public int getCount()
	{
		return temp.size();
	}

	@Override
	public long getItemId(int arg0)
	{
		return temp.get(arg0);
	}

	void add(int code)
	{
		list.remove(Integer.valueOf(code));
		list.add(code);
	}

	void rem(int code)
	{
		list.remove(Integer.valueOf(code));
	}

	private void commit()
	{
		if (list != temp)
		{
			temp = list;
		}
	}

	private void trunc()
	{
		if (list == temp)
			temp = new ArrayList<>(list);
	}

	boolean isfavorited(int code)
	{
		return list.contains(code);
	}

	@Override
	void save(SharedPreferences.Editor edit)
	{
		String str = "";
		for (Integer i : list)
			str += String.valueOf(Character.toChars(i));
		edit.putString("fav", str);
	}

	@Override
	public void drop(final int from, final int to)
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				list = temp;
				list.add(to, list.remove(from));
				trunc();

				if (view != null)
					view.invalidateViews();
			}
		});
	}

	@Override
	public void remove(final int which)
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				list.remove(temp.remove(which));

				if (view != null)
					view.invalidateViews();
			}
		});
	}
}
