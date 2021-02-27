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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

class FindAdapter extends UnicodeAdapter implements OnClickListener, OnEditorActionListener
{
	private Cursor cur;
	private LinearLayout layout;
	private AutoCompleteTextView text;
	private ImageButton clear;
	private ImageButton find;
	private String saved;
	private NameDatabase db;
	private SharedPreferences pref;
	private CompleteAdapter adapter;

	public FindAdapter(Activity activity, SharedPreferences pref, NameDatabase db, boolean single)
	{
		super(activity, db, single);

		this.db = db;
		saved = pref.getString("find", "");
		this.pref = pref;
		cur = null;
	}

	@Override
	int name()
	{
		return R.string.find;
	}

	@Override
	View instantiate(AbsListView grd)
	{
		super.instantiate(grd);

		layout = new LinearLayout(view.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		text = new AutoCompleteTextView(view.getContext());
		text.setSingleLine();
		text.setText(saved);
		text.setHint(R.string.fhint);
		text.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_FORCE_ASCII);
		text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
		text.setOnEditorActionListener(this);
		if (adapter == null)
			adapter = new CompleteAdapter(view.getContext(), pref);
		text.setAdapter(adapter);
		text.setThreshold(1);
		clear = new ImageButton(view.getContext());
		TypedValue tv = new TypedValue();
		view.getContext().getTheme().resolveAttribute(R.attr.cancel, tv, true);
		clear.setImageDrawable(view.getContext().getResources().getDrawable(tv.resourceId));
		clear.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		clear.setBackgroundDrawable(null);
		clear.setPadding(0, 0, 0, 0);
		clear.setOnClickListener(this);

		FrameLayout fl = new FrameLayout(view.getContext());
		fl.addView(text, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.rightMargin = (int)(view.getContext().getResources().getDisplayMetrics().density * 10.f);
		lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
		fl.addView(clear, lp);

		find = new ImageButton(view.getContext());
		view.getContext().getTheme().resolveAttribute(R.attr.search, tv, true);
		find.setImageDrawable(view.getContext().getResources().getDrawable(tv.resourceId));

		LinearLayout hl = new LinearLayout(view.getContext());
		hl.setOrientation(LinearLayout.HORIZONTAL);
		hl.addView(fl, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
		hl.addView(find, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		layout.addView(hl, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(view, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));

		find.setOnClickListener(this);

		return layout;
	}

	@Override
	void destroy()
	{
		view.setOnScrollListener(null);
		layout = null;
		text = null;
		find = null;
		adapter = null;

		if (cur != null)
			cur.close();
		cur = null;

		super.destroy();
	}

	@Override
	void save(SharedPreferences.Editor edit)
	{
		edit.putString("find", saved);
		if (adapter != null)
			adapter.save(edit);
	}

	@Override
	public int getCount()
	{
		return cur != null ? cur.getCount() : 0;
	}

	@Override
	public long getItemId(int arg0)
	{
		if (cur == null || arg0 < 0 || arg0 >= cur.getCount())
			return 0;
		cur.moveToPosition(arg0);
		return cur.getInt(0);
	}

	@SuppressLint("DefaultLocale")
	@Override
	public void onClick(View arg0)
	{
		if (arg0 == clear)
		{
			text.setText("");
		}
		if (arg0 == find)
		{
			saved = text.getText().toString().replaceAll("[^\\p{Alnum} \\-]", "");
			text.setText(saved);
			if (saved.length() == 0)
				return;
			if (adapter != null)
				adapter.update(saved);
			if (cur != null)
				cur.close();
			cur = db.find(saved, UnicodeActivity.univer);
			((InputMethodManager)text.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(text.getWindowToken(), 0);
			view.invalidateViews();
		}
	}

	@Override
	public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
	{
		if (arg0 == text && arg2 != null && arg2.getKeyCode() == KeyEvent.KEYCODE_ENTER)
		{
			if (arg2.getAction() == KeyEvent.ACTION_DOWN)
				find.performClick();
			return true;
		}
		return false;
	}

}
