package jp.ddo.hotmist.unicodepad;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

abstract class UnicodeAdapter extends BaseAdapter
{
	private Typeface tf;
	private NameDatabase db;
	boolean single;
	GridView grid;

	static int padding = 3;
	static float fontsize = 18f;
	static boolean shrink = true;

	UnicodeAdapter(NameDatabase db, boolean single)
	{
		this.db = db;
		this.single = single;
		this.grid = null;
	}

	int name()
	{
		return 0;
	}

	View instantiate(GridView grd)
	{
		grid = grd;

		return grd;
	}

	void destroy()
	{
		grid = null;
	}

	void save(SharedPreferences.Editor edit)
	{
	}

	void show()
	{
	}

	void leave()
	{
	}

	public String getItemString(int arg0)
	{
		return String.format("%04X", (int)getItemId(arg0));
	}

	@Override
	public Object getItem(int arg0)
	{
		return String.valueOf(Character.toChars((int)getItemId(arg0)));
	}

	@Override
	public int getItemViewType(int arg0)
	{
		return 0;
	}

	@SuppressLint("NewApi")
	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2)
	{
		if (this.single)
		{
			if (arg1 == null)
			{
				CharacterView ct = new CharacterView(arg2.getContext(), null, android.R.attr.textAppearanceLarge);
				TextView pt = new TextView(arg2.getContext(), null, android.R.attr.textAppearanceSmall);
				pt.setPadding(0, 0, 0, 0);
				TextView nt = new TextView(arg2.getContext(), null, android.R.attr.textAppearanceSmall);
				nt.setPadding(0, 0, 0, 0);
				LinearLayout vl = new LinearLayout(arg2.getContext());
				vl.setOrientation(LinearLayout.VERTICAL);
				vl.addView(pt, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));
				vl.addView(nt, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));
				LinearLayout hl = new LinearLayout(arg2.getContext());
				hl.setOrientation(LinearLayout.HORIZONTAL);
				hl.addView(ct, new LinearLayout.LayoutParams((int)(arg2.getContext().getResources().getDisplayMetrics().scaledDensity * fontsize * 2 + padding * 2), LayoutParams.MATCH_PARENT));
				hl.addView(vl, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
				arg1 = hl;
			}
			((CharacterView)((LinearLayout)arg1).getChildAt(0)).setText((String)getItem(arg0));
			if (getItemId(arg0) != -1)
			{
				((TextView)((LinearLayout)((LinearLayout)arg1).getChildAt(1)).getChildAt(0)).setText(String.format("U+%04X", (int)getItemId(arg0)));
				((TextView)((LinearLayout)((LinearLayout)arg1).getChildAt(1)).getChildAt(1)).setText(db.get((int)getItemId(arg0), "name"));
			}
			else
			{
				((TextView)((LinearLayout)((LinearLayout)arg1).getChildAt(1)).getChildAt(0)).setText(String.format((" " + getItemString(arg0)).replace(" ", " U+").substring(1)));
				((TextView)((LinearLayout)((LinearLayout)arg1).getChildAt(1)).getChildAt(1)).setText(db.get(getItemString(arg0), "name"));
			}
			(((LinearLayout)arg1).getChildAt(0)).setPadding(padding, padding, padding, padding);
			((CharacterView)((LinearLayout)arg1).getChildAt(0)).setTextSize(fontsize);
			((CharacterView)((LinearLayout)arg1).getChildAt(0)).shrinkWidth(shrink);
			((CharacterView)((LinearLayout)arg1).getChildAt(0)).setTypeface(tf);
			((CharacterView)((LinearLayout)arg1).getChildAt(0)).drawSlash(true);
			int ver = getItemId(arg0) != -1 ? db.getint((int)getItemId(arg0), "version") : db.getint(getItemString(arg0), "version");
			((CharacterView)((LinearLayout)arg1).getChildAt(0)).setValid(ver != 0 && ver <= UnicodeActivity.univer);
			return arg1;
		}
		else
		{
			CharacterView tv;
			if (arg1 == null || !(arg1 instanceof CharacterView))
			{
				tv = new CharacterView(arg2.getContext(), null, android.R.attr.textAppearanceLarge);
			}
			else
				tv = (CharacterView)arg1;
			tv.setPadding(padding, padding, padding, padding);
			tv.setTextSize(fontsize);
			tv.shrinkWidth(shrink);
			tv.setTypeface(tf);
			tv.drawSlash(true);
			int ver = getItemId(arg0) != -1 ? db.getint((int)getItemId(arg0), "version") : db.getint(getItemString(arg0), "version");
			tv.setValid(ver != 0 && ver <= UnicodeActivity.univer);
			tv.setText((String)getItem(arg0));
			return tv;
		}
	}

	@Override
	public int getViewTypeCount()
	{
		return 1;
	}

	@Override
	public boolean hasStableIds()
	{
		return false;
	}

	@Override
	public boolean isEmpty()
	{
		return getCount() == 0;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver arg0)
	{
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0)
	{
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return true;
	}

	@Override
	public boolean isEnabled(int arg0)
	{
		return true;
	}

	void setTypeface(Typeface tf)
	{
		this.tf = tf;
	}
}
