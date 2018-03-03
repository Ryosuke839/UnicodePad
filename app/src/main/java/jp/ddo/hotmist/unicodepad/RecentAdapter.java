package jp.ddo.hotmist.unicodepad;

import java.util.ArrayList;

import android.content.SharedPreferences;

class RecentAdapter extends UnicodeAdapter
{
	ArrayList<Integer> list;
	ArrayList<Integer> temp;
	static int maxitems = 16;

	RecentAdapter(SharedPreferences pref, NameDatabase db, boolean single)
	{
		super(db, single);

		list = new ArrayList<Integer>();
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

	void commit()
	{
		if (list != temp)
			temp = list;
	}

	void trunc()
	{
		if (list == temp)
			temp = new ArrayList<Integer>(list);
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
