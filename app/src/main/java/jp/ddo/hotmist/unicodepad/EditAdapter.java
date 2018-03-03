package jp.ddo.hotmist.unicodepad;

import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;

import java.util.ArrayList;

class EditAdapter extends UnicodeAdapter implements TextWatcher
{
	EditText edit;
	ArrayList<Integer> list;

	EditAdapter(SharedPreferences pref, NameDatabase db, boolean single, EditText edit)
	{
		super(db, single);

		this.edit = edit;

		list = new ArrayList<Integer>();
	}

	View instantiate(GridView grd)
	{
		list.clear();
		String str = edit.getEditableText().toString();
		for (int i = 0; i < str.length(); ++i)
		{
			int code = str.codePointAt(i);
			if (code > 0xFFFF)
				++i;
			list.add(code);
		}

		edit.addTextChangedListener(this);

		return super.instantiate(grd);
	}

	void destroy()
	{
		super.destroy();

		edit.removeTextChangedListener(this);
	}

	@Override
	int name()
	{
		return R.string.edit;
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

	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		if (before == 0 && count == 0)
			return;

		String str = s.toString();
		list.clear();
		for (int i = 0; i < str.length(); ++i)
		{
			int code = str.codePointAt(i);
			if (code > 0xFFFF)
				++i;
			list.add(code);
		}
		grid.invalidateViews();
	}

	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	public void afterTextChanged(Editable s)
	{
	}
}
