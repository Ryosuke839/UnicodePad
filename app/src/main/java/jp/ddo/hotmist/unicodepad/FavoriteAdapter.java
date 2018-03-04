package jp.ddo.hotmist.unicodepad;

import java.util.ArrayList;

import android.content.SharedPreferences;

class FavoriteAdapter extends UnicodeAdapter
{
	private ArrayList<Integer> list;
	private ArrayList<Integer> temp;

	FavoriteAdapter(SharedPreferences pref, NameDatabase db, boolean single)
	{
		super(db, single);

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
		return temp.get(arg0);
	}

	void add(int code)
	{
		list.remove(Integer.valueOf(code));
		list.add(code);
	}

	void remove(int code)
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

}
