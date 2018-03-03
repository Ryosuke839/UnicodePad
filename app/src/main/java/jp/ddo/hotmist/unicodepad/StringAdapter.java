package jp.ddo.hotmist.unicodepad;

import java.util.ArrayList;

public class StringAdapter extends UnicodeAdapter
{
	ArrayList<Integer> list;

	StringAdapter(String str)
	{
		super(null, false);

		list = new ArrayList<Integer>();
		for (int i = 0; i < str.length(); ++i)
		{
			int code = str.codePointAt(i);
			if (code > 0xFFFF)
				++i;
			list.add(code);
		}
	}

	@Override
	public int getCount()
	{
		return list.size();
	}

	@Override
	public long getItemId(int arg0)
	{
		return list.get(arg0);
	}
}
