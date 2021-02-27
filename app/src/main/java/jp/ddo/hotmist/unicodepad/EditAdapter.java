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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;

import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

class EditAdapter extends UnicodeAdapter implements TextWatcher, DragSortListView.DropListener, DragSortListView.RemoveListener
{
	private EditText edit;
	private ArrayList<Integer> list;
	private boolean suspend = false;

	EditAdapter(Activity activity, SharedPreferences pref, NameDatabase db, boolean single, EditText edit)
	{
		super(activity, db, single);

		this.edit = edit;

		list = new ArrayList<>();
	}

	@Override
	View instantiate(AbsListView grd)
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
		if (suspend)
			return;

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
		view.invalidateViews();
	}

	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	public void afterTextChanged(Editable s)
	{
	}

	@Override
	public void drop(final int from, final int to)
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				suspend = true;
				int from_begin = 0;
				int from_end = 0;
				for (int i = 0; i < list.size(); ++i)
				{
					if (i == from)
						from_begin = from_end;
					if (list.get(i) > 0xFFFF)
						++from_end;
					++from_end;
					if (i == from)
						break;
				}
				edit.getEditableText().delete(from_begin, from_end);
				Integer ch = list.remove(from);
				int to_begin = 0;
				for (int i = 0; i < list.size(); ++i)
				{
					if (i == to)
						break;
					if (list.get(i) > 0xFFFF)
						++to_begin;
					++to_begin;
				}
				edit.getEditableText().insert(to_begin, String.valueOf(Character.toChars(ch)));
				edit.getEditableText().replace(0, edit.getEditableText().length(), edit.getEditableText());
				suspend = false;
				list.add(to, ch);

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
				suspend = true;
				int which_begin = 0;
				int which_end = 0;
				for (int i = 0; i < list.size(); ++i)
				{
					if (i == which)
						which_begin = which_end;
					if (list.get(i) > 0xFFFF)
						++which_end;
					++which_end;
					if (i == which)
						break;
				}
				edit.getEditableText().delete(which_begin, which_end);
				edit.getEditableText().replace(0, edit.getEditableText().length(), edit.getEditableText());
				suspend = false;
				list.remove(which);

				if (view != null)
					view.invalidateViews();
			}
		});
	}
}
