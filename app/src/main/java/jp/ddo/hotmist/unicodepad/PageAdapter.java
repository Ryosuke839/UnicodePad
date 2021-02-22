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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.PagerAdapter;
import android.text.style.TextAppearanceSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

class PageAdapter extends PagerAdapter implements OnItemClickListener, OnItemLongClickListener
{
	private UnicodeActivity activity;
	private final int MAX_VIEWS = 6;
	private int num_page;
	private View layout[] = new View[MAX_VIEWS];
	private AbsListView views[] = new AbsListView[MAX_VIEWS];
	private EditText edit;
	private ListAdapter alist;
	private FindAdapter afind;
	private RecentAdapter arec;
	private FavoriteAdapter afav;
	private EditAdapter aedt;
	private EmojiAdapter aemoji;
	private boolean blist;
	private boolean bfind;
	private boolean brec;
	private boolean bfav;
	private boolean bedt;
	private boolean bemoji;
	private UnicodeAdapter adps[] = new UnicodeAdapter[MAX_VIEWS];
	private int recpage = -1;
	private int listpage = -1;
	private int page;
	private Typeface tf;
	private NameDatabase db;
	static int column = 8;
	private SharedPreferences pref;

	PageAdapter(UnicodeActivity act, SharedPreferences pref, EditText arg)
	{
		activity = act;
		db = new NameDatabase(act);
		this.pref = pref;
		num_page = pref.getInt("cnt_shown", 6);
		adps[pref.getInt("ord_list", 1)] = alist = new ListAdapter(act, pref, db, blist);
		listpage = pref.getInt("ord_list", 1);
		adps[pref.getInt("ord_find", 3)] = afind = new FindAdapter(act, pref, db, bfind);
		adps[pref.getInt("ord_rec", 0)] = arec = new RecentAdapter(act, pref, db, brec);
		recpage = pref.getInt("ord_rec", 0);
		if (recpage >= num_page)
			recpage = -1;
		adps[pref.getInt("ord_fav", 4)] = afav = new FavoriteAdapter(act, pref, db, bfav);
		adps[pref.getInt("ord_edt", 5)] = aedt = new EditAdapter(act, pref, db, bedt, arg);
		adps[pref.getInt("ord_emoji", 2)] = aemoji = new EmojiAdapter(act, pref, db, bemoji);
		page = -1;
		edit = arg;
		tf = null;
	}

	public View getView()
	{
		return views[page];
	}

	@Override
	public int getCount()
	{
		return num_page;
	}

	@Override
	public CharSequence getPageTitle(int position)
	{
		return activity.getResources().getString(adps[position].name());
	}

	@Override
	public void notifyDataSetChanged()
	{
		num_page = pref.getInt("cnt_shown", 6);
		adps[pref.getInt("ord_list", 1)] = alist;
		listpage = pref.getInt("ord_list", 1);
		adps[pref.getInt("ord_find", 3)] = afind;
		adps[pref.getInt("ord_rec", 0)] = arec;
		recpage = pref.getInt("ord_rec", 0);
		if (recpage >= num_page)
			recpage = -1;
		adps[pref.getInt("ord_fav", 4)] = afav;
		adps[pref.getInt("ord_edt", 5)] = aedt;
		adps[pref.getInt("ord_emoji", 2)] = aemoji;

		super.notifyDataSetChanged();
	}

	@Override
	public Object instantiateItem(final ViewGroup collection, final int position)
	{
		brec = pref.getString("single_rec", "false").equals("true");
		arec.single = brec;
		blist = pref.getString("single_list", "false").equals("true");
		alist.single = blist;
		bfind = pref.getString("single_find", "true").equals("true");
		afind.single = bfind;
		bfav = pref.getString("single_fav", "false").equals("true");
		afav.single = bfav;
		bedt = pref.getString("single_edt", "true").equals("true");
		aedt.single = bedt;
		bemoji = pref.getString("single_emoji", "false").equals("true");
		aemoji.single = bemoji;

		if (adps[position].single)
		{
			if (adps[position] instanceof DragSortListView.DropListener || adps[position] instanceof DragSortListView.RemoveListener)
			{
				DragSortListView view = new DragSortListView(activity, null);
				DragSortController controller = new DragSortController(view, R.id.HANDLE_ID, DragSortController.ON_DRAG, DragSortController.FLING_REMOVE, 0, R.id.HANDLE_ID);
				controller.setRemoveEnabled(true);
				controller.setRemoveMode(DragSortController.FLING_REMOVE);
				controller.setSortEnabled(true);
				view.setFloatViewManager(controller);
				view.setOnTouchListener(controller);
				views[position] = view;
			} else
			{
				views[position] = new ListView(activity);
			}
		} else
		{
			GridView view = new GridView(activity);
			view.setNumColumns(column);
			view.setAdapter(adps[position]);
			views[position] = view;
		}
		views[position].setOnItemClickListener(this);
		views[position].setOnItemLongClickListener(this);
		views[position].setAdapter(adps[position]);
		views[position].setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		layout[position] = adps[position].instantiate(views[position]);
		collection.addView(layout[position], 0);
		return layout[position];
	}

	@Override
	public void destroyItem(final ViewGroup collection, final int position, final Object view)
	{
		collection.removeView((View)view);
		adps[position].destroy();
		layout[position] = null;
		views[position] = null;
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1)
	{
		return arg0 == arg1;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		if (arg3 != -1)
		{
			arec.add((int)arg3);
			if (recpage != -1 && page != recpage && views[recpage] != null)
				views[recpage].invalidateViews();
		}
		int start = edit.getSelectionStart();
		int end = edit.getSelectionEnd();
		if (start == -1)
			return;
		edit.getEditableText().replace(Math.min(start, end), Math.max(start, end), arg3 != -1 ? String.valueOf(Character.toChars((int)arg3)) : (String)arg0.getAdapter().getItem(arg2));
	}

	@Override
	public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1, final int arg2, final long arg3)
	{
		showDesc(arg0, arg2, (UnicodeAdapter)(arg0 instanceof DragSortListView ? ((DragSortListView)arg0).getInputAdapter() : arg0.getAdapter()));
		return true;
	}

	private AlertDialog dlg;

	void showDesc(final View view, int index, final UnicodeAdapter ua)
	{
		PagerTabStrip tab = new PagerTabStrip(activity);
		tab.setId(R.id.TAB_ID);
		ViewPager.LayoutParams layoutParams = new ViewPager.LayoutParams();
		layoutParams.height = LayoutParams.WRAP_CONTENT;
		layoutParams.width = LayoutParams.MATCH_PARENT;
		layoutParams.gravity = Gravity.TOP;

		final ViewPager pager = new ViewPager(activity);
		pager.addView(tab, layoutParams);
		final CharacterAdapter adapter = new CharacterAdapter(activity, ua, tf, db, afav);
		pager.setAdapter(adapter);
		pager.setCurrentItem(index, false);
		pager.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int)(activity.getResources().getDisplayMetrics().scaledDensity * (CharacterAdapter.fontsize * 1.8f + new TextAppearanceSpan(activity, android.R.style.TextAppearance_Small).getTextSize() * 2.4f + 32.f))));

		LinearLayout layout = new LinearLayout(activity);
		layout.addView(pager);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity)
				.setView(layout);

		if (view != null)
			builder.setPositiveButton(R.string.input, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					if (adapter.getId() != -1)
					{
						arec.add((int)adapter.getId());
						if (recpage != -1 && page != recpage && views[recpage] != null)
							views[recpage].invalidateViews();
					}
					int start = edit.getSelectionStart();
					int end = edit.getSelectionEnd();
					if (start == -1)
						return;
					edit.getEditableText().replace(Math.min(start, end), Math.max(start, end), (String)ua.getItem(adapter.getIndex()));
				}
			});
		if (!(view instanceof AbsListView) || ((AbsListView)view).getAdapter() != aemoji)
			builder.setNeutralButton(R.string.inlist, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					find((int)adapter.getId());
				}
			});
		if (view instanceof AbsListView && ((AbsListView)view).getAdapter() == arec)
			builder.setNegativeButton(R.string.remrec, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					arec.rem((int)adapter.getId());
					if (views[recpage] != null)
						views[recpage].invalidateViews();
				}
			});
		if (view instanceof AbsListView && ((AbsListView)view).getAdapter() == aedt)
			builder.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					int i = pager.getCurrentItem();
					String s = edit.getEditableText().toString();
					edit.getEditableText().delete(s.offsetByCodePoints(0, i), s.offsetByCodePoints(0, i + 1));
				}
			});
		if (view instanceof AbsListView && ((AbsListView)view).getAdapter() == alist)
			builder.setNegativeButton(R.string.mark, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					final EditText edit = new EditText(activity);
					new AlertDialog.Builder(activity)
							.setTitle(R.string.mark)
							.setView(edit)
							.setPositiveButton(R.string.mark, new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface arg0, int arg1)
								{
									alist.mark((int)adapter.getId(), edit.getText().toString());
								}
							})
							.create().show();
				}
			});

		if (dlg != null && dlg.isShowing())
			dlg.dismiss();
		dlg = builder.create();
		dlg.show();
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object)
	{
		if (page == position)
			return;
		if (page != -1)
			adps[page].leave();
		adps[position].show();
		page = position;
	}

	@Override
	public int getItemPosition(Object object)
	{
		return POSITION_NONE;
	}

	private void find(int code)
	{
		activity.setPage(listpage);
		alist.find(code);
	}

	void save(SharedPreferences.Editor edit)
	{
		alist.save(edit);
		afind.save(edit);
		arec.save(edit);
		afav.save(edit);
		aedt.save(edit);
		aemoji.save(edit);
	}

	void setTypeface(Typeface tf)
	{
		this.tf = tf;
		alist.setTypeface(tf);
		afind.setTypeface(tf);
		arec.setTypeface(tf);
		afav.setTypeface(tf);
		aedt.setTypeface(tf);
		aemoji.setTypeface(tf);
		for (int i = 0; i < MAX_VIEWS; ++i)
			if (views[i] != null)
				views[i].invalidateViews();
	}
}
