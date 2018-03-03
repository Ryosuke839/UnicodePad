package jp.ddo.hotmist.unicodepad;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Paint;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

class ListAdapter extends UnicodeAdapter implements OnItemSelectedListener, OnScrollListener, OnClickListener, OnTouchListener
{
	private int count;
	private final NavigableMap<Integer, Pair<Integer, Integer>> emap = new TreeMap<Integer, Pair<Integer, Integer>>();
	private final NavigableMap<Integer, Pair<Integer, Integer>> fmap = new TreeMap<Integer, Pair<Integer, Integer>>();
	private final NavigableMap<Integer, Integer> imap = new TreeMap<Integer, Integer>();
	private final List<Integer> jmap = new ArrayList<Integer>();
	private final NavigableMap<Integer, String> mmap = new TreeMap<Integer, String>();
	private LinearLayout layout;
	private Spinner jump;
	private Spinner mark;
	private Button code;
	private int current;
	private int head;
	private int scroll;
	private int resnormal;
	private int resselect;
	private int highlight;
	private View hightarget;

	private class MapAdapter implements SpinnerAdapter
	{
		Context context;

		public MapAdapter(Context context)
		{
			this.context = context;
		}

		@Override
		public int getCount()
		{
			return mmap.size() == 0 ? 1 : mmap.size() + 2;
		}

		@Override
		public Object getItem(int arg0)
		{
			switch (arg0)
			{
				case 0:
					return context.getResources().getString(R.string.mark);
				case 1:
					return context.getResources().getString(R.string.rem);
				default:
					for (Entry<Integer, String> e : mmap.entrySet())
						if (--arg0 == 1)
							return String.format("U+%04X %s", e.getKey(), e.getValue());
			}
			return "";
		}

		@Override
		public long getItemId(int arg0)
		{
			return arg0;
		}

		@Override
		public int getItemViewType(int arg0)
		{
			return 0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2)
		{
			if (arg1 == null)
				arg1 = ((android.view.LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_spinner_item, arg2, false);
			((TextView)arg1).setText((String)getItem(arg0));
			((TextView)arg1).setTextColor(0x00000000);
			return arg1;
		}

		@Override
		public int getViewTypeCount()
		{
			return 1;
		}

		@Override
		public boolean hasStableIds()
		{
			return true;
		}

		@Override
		public boolean isEmpty()
		{
			return false;
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
		public View getDropDownView(int arg0, View arg1, ViewGroup arg2)
		{
			if (arg1 == null)
				arg1 = ((android.view.LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(arg0 == 0 ? R.layout.spinner_drop_down_void : R.layout.spinner_drop_down_item, arg2, false);
			((TextView)arg1).setText((String)getItem(arg0));
			return arg1;
		}

	}

	public ListAdapter(SharedPreferences pref, NameDatabase db, boolean single)
	{
		super(db, single);

		current = -1;
		head = -1;
		scroll = pref.getInt("list", 0);
		highlight = -1;
		hightarget = null;

		String str = pref.getString("mark", "");
		for (String s : str.split("\n"))
		{
			int space = s.indexOf(' ');
			if (space != -1)
				try
				{
					mmap.put(Integer.valueOf(s.substring(0, space)), s.substring(space + 1));
				} catch (NumberFormatException e)
				{
				}
		}
	}

	@Override
	int name()
	{
		return R.string.list;
	}

	@Override
	View instantiate(GridView grd)
	{
		super.instantiate(grd);

		emap.clear();
		fmap.clear();
		imap.clear();
		jmap.clear();
		count = 0;
		int univer = UnicodeActivity.univer;
		add(0x0, 0x7F);
		add(0x80, 0xFF);
		add(0x100, 0x17F);
		add(0x180, 0x24F);
		add(0x250, 0x2AF);
		add(0x2B0, 0x2FF);
		add(0x300, 0x36F);
		add(0x370, 0x3FF);
		add(0x400, 0x4FF);
		add(0x500, 0x52F);
		add(0x530, 0x58F);
		add(0x590, 0x5FF);
		add(0x600, 0x6FF);
		add(0x700, 0x74F);
		add(0x750, 0x77F);
		add(0x780, 0x7BF);
		add(0x7C0, 0x7FF);
		add(0x800, 0x83F);
		add(0x840, 0x85F);
		if (univer >= 1000)
			add(0x860, 0x86F);
		if (univer >= 610)
			add(0x8A0, 0x8FF);
		add(0x900, 0x97F);
		add(0x980, 0x9FF);
		add(0xA00, 0xA7F);
		add(0xA80, 0xAFF);
		add(0xB00, 0xB7F);
		add(0xB80, 0xBFF);
		add(0xC00, 0xC7F);
		add(0xC80, 0xCFF);
		add(0xD00, 0xD7F);
		add(0xD80, 0xDFF);
		add(0xE00, 0xE7F);
		add(0xE80, 0xEFF);
		add(0xF00, 0xFFF);
		add(0x1000, 0x109F);
		add(0x10A0, 0x10FF);
		add(0x1100, 0x11FF);
		add(0x1200, 0x137F);
		add(0x1380, 0x139F);
		add(0x13A0, 0x13FF);
		add(0x1400, 0x167F);
		add(0x1680, 0x169F);
		add(0x16A0, 0x16FF);
		add(0x1700, 0x171F);
		add(0x1720, 0x173F);
		add(0x1740, 0x175F);
		add(0x1760, 0x177F);
		add(0x1780, 0x17FF);
		add(0x1800, 0x18AF);
		add(0x18B0, 0x18FF);
		add(0x1900, 0x194F);
		add(0x1950, 0x197F);
		add(0x1980, 0x19DF);
		add(0x19E0, 0x19FF);
		add(0x1A00, 0x1A1F);
		add(0x1A20, 0x1AAF);
		if (univer >= 700)
			add(0x1AB0, 0x1AFF);
		add(0x1B00, 0x1B7F);
		add(0x1B80, 0x1BBF);
		add(0x1BC0, 0x1BFF);
		add(0x1C00, 0x1C4F);
		add(0x1C50, 0x1C7F);
		if (univer >= 900)
			add(0x1C80, 0x1C8F);
		if (univer >= 610)
			add(0x1CC0, 0x1CCF);
		add(0x1CD0, 0x1CFF);
		add(0x1D00, 0x1D7F);
		add(0x1D80, 0x1DBF);
		add(0x1DC0, 0x1DFF);
		add(0x1E00, 0x1EFF);
		add(0x1F00, 0x1FFF);
		add(0x2000, 0x206F);
		add(0x2070, 0x209F);
		add(0x20A0, 0x20CF);
		add(0x20D0, 0x20FF);
		add(0x2100, 0x214F);
		add(0x2150, 0x218F);
		add(0x2190, 0x21FF);
		add(0x2200, 0x22FF);
		add(0x2300, 0x23FF);
		add(0x2400, 0x243F);
		add(0x2440, 0x245F);
		add(0x2460, 0x24FF);
		add(0x2500, 0x257F);
		add(0x2580, 0x259F);
		add(0x25A0, 0x25FF);
		add(0x2600, 0x26FF);
		add(0x2700, 0x27BF);
		add(0x27C0, 0x27EF);
		add(0x27F0, 0x27FF);
		add(0x2800, 0x28FF);
		add(0x2900, 0x297F);
		add(0x2980, 0x29FF);
		add(0x2A00, 0x2AFF);
		add(0x2B00, 0x2BFF);
		add(0x2C00, 0x2C5F);
		add(0x2C60, 0x2C7F);
		add(0x2C80, 0x2CFF);
		add(0x2D00, 0x2D2F);
		add(0x2D30, 0x2D7F);
		add(0x2D80, 0x2DDF);
		add(0x2DE0, 0x2DFF);
		add(0x2E00, 0x2E7F);
		add(0x2E80, 0x2EFF);
		add(0x2F00, 0x2FDF);
		add(0x2FF0, 0x2FFF);
		add(0x3000, 0x303F);
		add(0x3040, 0x309F);
		add(0x30A0, 0x30FF);
		add(0x3100, 0x312F);
		add(0x3130, 0x318F);
		add(0x3190, 0x319F);
		add(0x31A0, 0x31BF);
		add(0x31C0, 0x31EF);
		add(0x31F0, 0x31FF);
		add(0x3200, 0x32FF);
		add(0x3300, 0x33FF);
		add(0x3400, 0x4DBF);
		add(0x4DC0, 0x4DFF);
		add(0x4E00, 0x9FFF);
		add(0xA000, 0xA48F);
		add(0xA490, 0xA4CF);
		add(0xA4D0, 0xA4FF);
		add(0xA500, 0xA63F);
		add(0xA640, 0xA69F);
		add(0xA6A0, 0xA6FF);
		add(0xA700, 0xA71F);
		add(0xA720, 0xA7FF);
		add(0xA800, 0xA82F);
		add(0xA830, 0xA83F);
		add(0xA840, 0xA87F);
		add(0xA880, 0xA8DF);
		add(0xA8E0, 0xA8FF);
		add(0xA900, 0xA92F);
		add(0xA930, 0xA95F);
		add(0xA960, 0xA97F);
		add(0xA980, 0xA9DF);
		if (univer >= 700)
			add(0xA9E0, 0xA9FF);
		add(0xAA00, 0xAA5F);
		add(0xAA60, 0xAA7F);
		add(0xAA80, 0xAADF);
		if (univer >= 610)
			add(0xAAE0, 0xAAFF);
		add(0xAB00, 0xAB2F);
		if (univer >= 700)
			add(0xAB30, 0xAB6F);
		if (univer >= 800)
			add(0xAB70, 0xABBF);
		add(0xABC0, 0xABFF);
		add(0xAC00, 0xD7AF);
		add(0xD7B0, 0xD7FF);
		add(0xE000, 0xF8FF);
		add(0xF900, 0xFAFF);
		add(0xFB00, 0xFB4F);
		add(0xFB50, 0xFDFF);
		add(0xFE00, 0xFE0F);
		add(0xFE10, 0xFE1F);
		add(0xFE20, 0xFE2F);
		add(0xFE30, 0xFE4F);
		add(0xFE50, 0xFE6F);
		add(0xFE70, 0xFEFF);
		add(0xFF00, 0xFFEF);
		add(0xFFF0, 0xFFFF);
		add(0x10000, 0x1007F);
		add(0x10080, 0x100FF);
		add(0x10100, 0x1013F);
		add(0x10140, 0x1018F);
		add(0x10190, 0x101CF);
		add(0x101D0, 0x101FF);
		add(0x10280, 0x1029F);
		add(0x102A0, 0x102DF);
		if (univer >= 700)
			add(0x102E0, 0x102FF);
		add(0x10300, 0x1032F);
		add(0x10330, 0x1034F);
		if (univer >= 700)
			add(0x10350, 0x1037F);
		add(0x10380, 0x1039F);
		add(0x103A0, 0x103DF);
		add(0x10400, 0x1044F);
		add(0x10450, 0x1047F);
		add(0x10480, 0x104AF);
		if (univer >= 900)
			add(0x104B0, 0x104FF);
		if (univer >= 700)
		{
			add(0x10500, 0x1052F);
			add(0x10530, 0x1056F);
			add(0x10600, 0x1077F);
		}
		add(0x10800, 0x1083F);
		add(0x10840, 0x1085F);
		if (univer >= 700)
			add(0x10860, 0x1087F);
		if (univer >= 700)
			add(0x10880, 0x108AF);
		if (univer >= 800)
			add(0x108E0, 0x108FF);
		add(0x10900, 0x1091F);
		add(0x10920, 0x1093F);
		if (univer >= 610)
			add(0x10980, 0x1099F);
		if (univer >= 610)
			add(0x109A0, 0x109FF);
		add(0x10A00, 0x10A5F);
		add(0x10A60, 0x10A7F);
		if (univer >= 700)
			add(0x10A80, 0x10A9F);
		if (univer >= 700)
			add(0x10AC0, 0x10AFF);
		add(0x10B00, 0x10B3F);
		add(0x10B40, 0x10B5F);
		add(0x10B60, 0x10B7F);
		if (univer >= 700)
			add(0x10B80, 0x10BAF);
		add(0x10C00, 0x10C4F);
		add(0x10C80, 0x10CFF);
		add(0x10E60, 0x10E7F);
		add(0x11000, 0x1107F);
		add(0x11080, 0x110CF);
		if (univer >= 610)
		{
			add(0x110D0, 0x110FF);
			add(0x11100, 0x1114F);
			if (univer >= 700)
				add(0x11150, 0x1117F);
			add(0x11180, 0x111DF);
			if (univer >= 700)
			{
				add(0x111E0, 0x111FF);
				add(0x11200, 0x1124F);
				if (univer >= 800)
					add(0x11280, 0x112AF);
				add(0x112B0, 0x112FF);
				add(0x11300, 0x1137F);
				if (univer >= 900)
					add(0x11400, 0x1147F);
				add(0x11480, 0x114DF);
				add(0x11580, 0x115FF);
				add(0x11600, 0x1165F);
				if (univer >= 900)
					add(0x11660, 0x1167F);
			}
			add(0x11680, 0x116CF);
			if (univer >= 800)
				add(0x11700, 0x1173F);
			if (univer >= 700)
				add(0x118A0, 0x118FF);
			if (univer >= 1000)
				add(0x11A00, 0x11A4F);
			if (univer >= 1000)
				add(0x11A50, 0x11AAF);
			if (univer >= 700)
				add(0x11AC0, 0x11AFF);
			if (univer >= 900)
				add(0x11C00, 0x11C6F);
			if (univer >= 900)
				add(0x11C70, 0x11CBF);
			if (univer >= 1000)
				add(0x11D00, 0x11D5F);
		}
		add(0x12000, 0x123FF);
		add(0x12400, 0x1247F);
		if (univer >= 800)
			add(0x12480, 0x1254F);
		add(0x13000, 0x1342F);
		if (univer >= 800)
			add(0x14400, 0x1467F);
		add(0x16800, 0x16A3F);
		if (univer >= 700)
		{
			add(0x16A40, 0x16A6F);
			add(0x16AD0, 0x16AFF);
			add(0x16B00, 0x16B8F);
		}
		if (univer >= 610)
			add(0x16F00, 0x16F9F);
		if (univer >= 900)
		{
			add(0x16FE0, 0x16FFF);
			add(0x17000, 0x187FF);
			add(0x18800, 0x18AFF);
		}
		add(0x1B000, 0x1B0FF);
		if (univer >= 1000)
			add(0x1B100, 0x1B12F);
		if (univer >= 1000)
			add(0x1B170, 0x1B2FF);
		if (univer >= 700)
			add(0x1BC00, 0x1BC9F);
		if (univer >= 700)
			add(0x1BCA0, 0x1BCAF);
		add(0x1D000, 0x1D0FF);
		add(0x1D100, 0x1D1FF);
		add(0x1D200, 0x1D24F);
		add(0x1D300, 0x1D35F);
		add(0x1D360, 0x1D37F);
		add(0x1D400, 0x1D7FF);
		if (univer >= 800)
			add(0x1D800, 0x1DAAF);
		if (univer >= 900)
			add(0x1E000, 0x1E02F);
		if (univer >= 700)
			add(0x1E800, 0x1E8DF);
		if (univer >= 900)
			add(0x1E900, 0x1E95F);
		if (univer >= 610)
			add(0x1EE00, 0x1EEFF);
		add(0x1F000, 0x1F02F);
		add(0x1F030, 0x1F09F);
		add(0x1F0A0, 0x1F0FF);
		add(0x1F100, 0x1F1FF);
		add(0x1F200, 0x1F2FF);
		add(0x1F300, 0x1F5FF);
		add(0x1F600, 0x1F64F);
		if (univer >= 700)
			add(0x1F650, 0x1F67F);
		add(0x1F680, 0x1F6FF);
		add(0x1F700, 0x1F77F);
		if (univer >= 700)
			add(0x1F780, 0x1F7FF);
		if (univer >= 700)
			add(0x1F800, 0x1F8FF);
		if (univer >= 800)
			add(0x1F900, 0x1F9FF);
		add(0x20000, 0x2A6DF);
		add(0x2A700, 0x2B73F);
		add(0x2B740, 0x2B81F);
		if (univer >= 800)
			add(0x2B820, 0x2CEAF);
		if (univer >= 1000)
			add(0x2CEB0, 0x2EBEF);
		add(0x2F800, 0x2FA1F);
		add(0xE0000, 0xE007F);
		add(0xE0100, 0xE01EF);
		add(0xF0000, 0xFFFFF);
		add(0x100000, 0x10FFFF);

		resnormal = grid.getContext().getResources().getColor(android.R.color.transparent);
		resselect = grid.getContext().getResources().getColor(android.R.color.tab_indicator_text);

		layout = new LinearLayout(grid.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		code = new Button(grid.getContext());
		code.setText("U+10DDDD");
		code.setSingleLine();
		jump = new Spinner(grid.getContext());
		mark = new Spinner(grid.getContext());

		FrameLayout fl = new FrameLayout(grid.getContext());
		fl.addView(mark, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		lp.rightMargin = (int)(grid.getContext().getResources().getDisplayMetrics().scaledDensity * 22f);
		fl.addView(jump, lp);

		LinearLayout hl = new LinearLayout(grid.getContext());
		hl.setOrientation(LinearLayout.HORIZONTAL);
		hl.setGravity(Gravity.CENTER);
		hl.addView(fl, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
		Paint p = new Paint();
		p.setTextSize(code.getTextSize());
		hl.addView(code, new LinearLayout.LayoutParams(code.getPaddingLeft() + (int)p.measureText("U+10DDDD") + code.getPaddingRight(), LayoutParams.WRAP_CONTENT));
		layout.addView(hl, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(grid, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
		String[] jstr = new String[fmap.size()];
		SparseArray<String> jmap = new SparseArray<String>();
		for (String s : jump.getContext().getResources().getStringArray(R.array.codes))
			jmap.put(Integer.valueOf(s.substring(0, s.indexOf(' ')), 16), s.substring(s.indexOf(' ') + 1));
		Iterator<Integer> jit = fmap.keySet().iterator();
		for (int i = 0; jit.hasNext(); ++i)
		{
			int c = jit.next();
			jstr[i] = String.format("U+%04X %s", c, jmap.get(c));
		}
		ArrayAdapter<String> adp = new ArrayAdapter<String>(jump.getContext(), android.R.layout.simple_spinner_item, jstr);
		adp.setDropDownViewResource(R.layout.spinner_drop_down_item);
		jump.setAdapter(adp);

		mark.setAdapter(new MapAdapter(mark.getContext()));
		mark.setSelection(0);
		mark.setOnItemSelectedListener(this);

		grid.setOnTouchListener(this);

		code.setOnClickListener(this);

		Entry<Integer, Pair<Integer, Integer>> e = fmap.floorEntry(scroll);
		if (e.getValue().second < scroll)
			scroll = e.getValue().second;
		grid.setSelection(scroll - e.getKey() + e.getValue().first);
		grid.setOnScrollListener(this);

		jump.setOnItemSelectedListener(this);

		return layout;
	}

	@Override
	void destroy()
	{
		grid.setOnScrollListener(null);
		layout = null;
		code = null;
		jump = null;
		mark = null;
		current = -1;
		head = -1;

		super.destroy();
	}

	int find(int code)
	{
		Entry<Integer, Pair<Integer, Integer>> e = fmap.floorEntry(code);
		if (e.getValue().second < code)
			return -1;
		scroll = code;
		highlight = scroll - e.getKey() + e.getValue().first;
		if (grid != null)
		{
			grid.setSelection(scroll - e.getKey() + e.getValue().first);
			if (grid.getFirstVisiblePosition() <= highlight && highlight <= grid.getLastVisiblePosition())
			{
				if (hightarget != null)
					hightarget.setBackgroundColor(resnormal);
				hightarget = grid.getChildAt(highlight - grid.getFirstVisiblePosition());
				hightarget.setBackgroundColor(resselect);
			}
		}
		return scroll;
	}

	void mark(int code, String name)
	{
		mmap.remove(code);
		mmap.put(code, name.length() > 0 ? name : "Unnamed Mark");
	}

	@Override
	void save(SharedPreferences.Editor edit)
	{
		edit.putInt("list", scroll);
		String str = "";
		for (Iterator<Entry<Integer, String>> mit = mmap.entrySet().iterator(); mit.hasNext(); )
		{
			Entry<Integer, String> code = mit.next();
			str += String.format("%d %s", code.getKey(), code.getValue());
			if (mit.hasNext())
				str += "\n";
		}
		edit.putString("mark", str);
	}

	private void add(int begin, int end)
	{
		emap.put(count, new Pair<Integer, Integer>(begin, emap.size()));
		fmap.put(begin, new Pair<Integer, Integer>(count, end));
		imap.put(begin, imap.size());
		jmap.add(count);
		count += end + 1 - begin;
	}

	@Override
	public int getCount()
	{
		return count;
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2)
	{
		View ret = super.getView(arg0, arg1, arg2);
		if (arg0 == highlight)
		{
			if (hightarget != null)
				hightarget.setBackgroundColor(resnormal);
			hightarget = ret;
			hightarget.setBackgroundColor(resselect);
		} else if (ret == hightarget)
			hightarget.setBackgroundColor(resnormal);
		return ret;
	}

	@Override
	public long getItemId(int arg0)
	{
		Entry<Integer, Pair<Integer, Integer>> e = emap.floorEntry(arg0);
		return arg0 - e.getKey() + e.getValue().first;
	}

	int guard = 0;

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		if (arg0 == jump)
		{
			current = arg2;
			if (grid != null)
				if (guard == 0)
					grid.setSelection(jmap.get(arg2));
		}
		if (arg0 == mark)
		{
			if (arg2 == 1)
			{
				CharSequence[] items = new CharSequence[mmap.size()];
				int i = 0;
				for (Entry<Integer, String> e : mmap.entrySet())
					items[i++] = String.format("U+%04X %s", e.getKey(), e.getValue());
				new AlertDialog.Builder(grid.getContext())
						.setTitle(R.string.rem)
						.setItems(items, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface arg0, int arg1)
							{
								for (Entry<Integer, String> e : mmap.entrySet())
									if (--arg1 == -1)
									{
										mmap.remove(e.getKey());
										break;
									}
							}
						}).show();
				mark.setSelection(0);
			}
			if (arg2 > 1)
			{
				for (Entry<Integer, String> e : mmap.entrySet())
					if (--arg2 == 1)
						find(e.getKey());
				mark.setSelection(0);
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0)
	{
	}

	@Override
	public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3)
	{
		if (arg0 == grid)
		{
			if (grid.getChildAt(0) != null && grid.getChildAt(0).getTop() * -2 > grid.getChildAt(0).getHeight())
				arg1 += single ? 1 : PageAdapter.column;

			Entry<Integer, Pair<Integer, Integer>> e = emap.floorEntry(arg1);
			if (arg2 != 0)
			{
				if (e != null)
				{
					if (jump != null)
					{
						if (guard == 0 && current != e.getValue().second)
						{
							current = e.getValue().second;
							++guard;
							jump.setSelection(e.getValue().second, false);
							jump.post(new Runnable()
							{
								@Override
								public void run()
								{
									--guard;
								}
							});
						}
					}

					if (code != null)
					{
						if (head != arg1 - e.getKey() + e.getValue().first)
						{
							head = arg1 - e.getKey() + e.getValue().first;
							code.setText(String.format("U+%04X", head));
						}
					}
				}
				if (arg1 != 0)
					scroll = arg1 - e.getKey() + e.getValue().first;
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1)
	{
	}

	@SuppressLint("InlinedApi")
	@Override
	public void onClick(final View arg0)
	{
		if (arg0 == code)
		{
			final EditText edit = new EditText(arg0.getContext());
			OnClickListener ocl = new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (v instanceof ImageButton)
					{
						edit.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
					} else
					{
						String s = ((Button)v).getText().toString();
						int start = edit.getSelectionStart();
						int end = edit.getSelectionEnd();
						if (start == -1)
							return;
						edit.getEditableText().replace(Math.min(start, end), Math.max(start, end), s);
						edit.setSelection(Math.min(start, end) + s.length());
					}
				}
			};
			edit.setText(String.format("%04X", head));
			edit.setSingleLine();
			edit.setImeOptions(Build.VERSION.SDK_INT >= 16 ? EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_FORCE_ASCII : EditorInfo.IME_ACTION_GO);
			edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
			edit.setGravity(Gravity.CENTER_VERTICAL);
			edit.setOnTouchListener(new OnTouchListener()
			{
				@SuppressLint("ClickableViewAccessibility")
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					v.onTouchEvent(event);
					((InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
					return true;
				}
			});
			TextView text = new TextView(arg0.getContext());
			text.setText("U+");
			text.setGravity(Gravity.CENTER);
			ImageButton del = new ImageButton(arg0.getContext(), null, android.R.attr.buttonStyleSmall);
			TypedValue tv = new TypedValue();
			arg0.getContext().getTheme().resolveAttribute(R.attr.backspace, tv, true);
			del.setImageDrawable(arg0.getContext().getResources().getDrawable(tv.resourceId));
			del.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
			del.setPadding(0, 0, 0, 0);
			del.setOnClickListener(ocl);
			LinearLayout vl = new LinearLayout(arg0.getContext());
			vl.setOrientation(LinearLayout.VERTICAL);
			LinearLayout layout = new LinearLayout(arg0.getContext());
			layout.addView(text, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			Paint p = new Paint();
			p.setTextSize(edit.getTextSize());
			layout.addView(edit, new LinearLayout.LayoutParams(edit.getPaddingLeft() + (int)p.measureText("10DDDD") + edit.getPaddingRight(), LayoutParams.WRAP_CONTENT));
			layout.addView(del);
			layout.setGravity(Gravity.RIGHT);
			vl.addView(layout, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.WRAP_CONTENT, ViewGroup.MarginLayoutParams.WRAP_CONTENT);
			mlp.setMargins(0, 0, 0, 0);
			for (int i = 5; i >= 0; --i)
			{
				LinearLayout hl = new LinearLayout(arg0.getContext());
				for (int j = i == 0 ? 2 : 0; j < 3; ++j)
				{
					Button btn = new Button(arg0.getContext(), null, android.R.attr.buttonStyleSmall);
					btn.setText(String.format("%X", i * 3 + j - 2));
					btn.setPadding(0, 0, 0, 0);
					btn.setOnClickListener(ocl);
					hl.addView(btn, new LinearLayout.LayoutParams(mlp));
				}
				hl.setGravity(Gravity.CENTER_HORIZONTAL);
				vl.addView(hl, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			}
			vl.setGravity(Gravity.CENTER);
			AlertDialog dlg = new AlertDialog.Builder(arg0.getContext())
					.setTitle(R.string.code)
					.setView(vl)
					.setPositiveButton(android.R.string.search_go, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							if (grid != null)
								try
								{
									if (find(Integer.valueOf(edit.getText().toString(), 16)) == -1)
										Toast.makeText(grid.getContext(), R.string.nocode, Toast.LENGTH_SHORT).show();
								} catch (NumberFormatException e)
								{
								}
						}
					})
					.create();
			dlg.show();
			final Button btn = dlg.getButton(DialogInterface.BUTTON_POSITIVE);
			edit.addTextChangedListener(new TextWatcher()
			{

				@Override
				public void afterTextChanged(Editable arg0)
				{
					try
					{
						Integer.valueOf(arg0.toString(), 16);
						btn.setEnabled(true);
					} catch (NumberFormatException e)
					{
						btn.setEnabled(false);
					}
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
				{
				}

				@Override
				public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
				{
				}
			});
/*			edit.setOnEditorActionListener(new OnEditorActionListener()
			{
				@Override
				public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
				{
					if (arg2 != null && arg2.getKeyCode() == KeyEvent.KEYCODE_ENTER)
					{
						if (arg2.getAction() == KeyEvent.ACTION_DOWN)
							btn.performClick();
						return true;
					}
					return false;
				}				
			});*/
			edit.setSelectAllOnFocus(true);
			edit.requestFocus();
//			edit.selectAll();
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View arg0, MotionEvent arg1)
	{
		highlight = -1;
		if (hightarget != null)
			if (grid != null)
			{
				hightarget.setBackgroundColor(resnormal);
				hightarget = null;
			}
		return false;
	}

}
