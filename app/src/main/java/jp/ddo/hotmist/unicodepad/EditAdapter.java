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

import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;

import java.util.ArrayList;

class EditAdapter extends UnicodeAdapter implements TextWatcher
{
	private EditText edit;
	private ArrayList<Integer> list;

	EditAdapter(SharedPreferences pref, NameDatabase db, boolean single, EditText edit)
	{
		super(db, single);

		this.edit = edit;

		list = new ArrayList<>();
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
