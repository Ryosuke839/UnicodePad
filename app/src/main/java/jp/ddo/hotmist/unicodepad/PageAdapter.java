package jp.ddo.hotmist.unicodepad;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.PagerAdapter;
import android.text.style.TextAppearanceSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.AdapterView.OnItemClickListener;

class PageAdapter extends PagerAdapter implements OnItemClickListener, OnItemLongClickListener
{
	private UnicodeActivity activity;
	private final int MAX_VIEWS = 6;
	private int num_page;
	private View layout[] = new View[MAX_VIEWS];
	private GridView grids[] = new GridView[MAX_VIEWS];
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
		adps[pref.getInt("ord_list", 1)] = alist = new ListAdapter(pref, db, blist);
		listpage = pref.getInt("ord_list", 1);
		adps[pref.getInt("ord_find", 3)] = afind = new FindAdapter(pref, db, bfind);
		adps[pref.getInt("ord_rec", 0)] = arec = new RecentAdapter(pref, db, brec);
		recpage = pref.getInt("ord_rec", 0);
		if (recpage >= num_page)
			recpage = -1;
		adps[pref.getInt("ord_fav", 4)] = afav = new FavoriteAdapter(pref, db, bfav);
		adps[pref.getInt("ord_edt", 5)] = aedt = new EditAdapter(pref, db, bedt, arg);
		adps[pref.getInt("ord_emoji", 2)] = aemoji = new EmojiAdapter(pref, db, bemoji);
		page = -1;
		edit = arg;
		tf = null;
	}

	public GridView getGridView()
	{
		return grids[page];
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

		if (grids[position] == null)
		{
			grids[position] = new GridView(activity);
			grids[position].setOnItemClickListener(this);
			grids[position].setOnItemLongClickListener(this);
			grids[position].setNumColumns(adps[position].single ? 1 : column);
			grids[position].setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}

		grids[position].setAdapter(adps[position]);
		layout[position] = adps[position].instantiate(grids[position]);
		collection.addView(layout[position], 0);
		return layout[position];
	}

	@Override
	public void destroyItem(final ViewGroup collection, final int position, final Object view)
	{
		collection.removeView((View)view);
		adps[position].destroy();
		layout[position] = null;
		grids[position] = null;
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
			if (recpage != -1 && page != recpage && grids[recpage] != null)
				grids[recpage].invalidateViews();
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
		showDesc(arg0, arg2, (UnicodeAdapter)((GridView)arg0).getAdapter());
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
						if (recpage != -1 && page != recpage && grids[recpage] != null)
							grids[recpage].invalidateViews();
					}
					int start = edit.getSelectionStart();
					int end = edit.getSelectionEnd();
					if (start == -1)
						return;
					edit.getEditableText().replace(Math.min(start, end), Math.max(start, end), (String)ua.getItem(adapter.getIndex()));
				}
			});
		if (!(view instanceof GridView) || ((GridView)view).getAdapter() != aemoji)
			builder.setNeutralButton(R.string.inlist, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					find((int)adapter.getId());
				}
			});
		if (view instanceof GridView && ((GridView)view).getAdapter() == arec)
			builder.setNegativeButton(R.string.remrec, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					arec.remove((int)adapter.getId());
					if (grids[recpage] != null)
						grids[recpage].invalidateViews();
				}
			});
		if (view instanceof GridView && ((GridView)view).getAdapter() == aedt)
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
		if (view instanceof GridView && ((GridView)view).getAdapter() == alist)
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
			if (grids[i] != null)
				grids[i].invalidateViews();
	}
}
