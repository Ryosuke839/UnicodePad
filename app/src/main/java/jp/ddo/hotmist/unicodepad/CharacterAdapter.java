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

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

class CharacterAdapter extends PagerAdapter implements OnClickListener
{
	private UnicodeActivity context;
	private UnicodeAdapter adapter;
	private Typeface tf;
	private int index;
	private NameDatabase db;
	private FavoriteAdapter afav;
	static float fontsize = 160f;
	static float checker = 10f;
	static boolean lines = true;
	static boolean shrink = true;
	private int reslist;

	@SuppressLint({"NewApi", "PrivateResource"})
	CharacterAdapter(UnicodeActivity context, UnicodeAdapter adapter, Typeface tf, NameDatabase db, FavoriteAdapter afav)
	{
		if (Build.VERSION.SDK_INT >= 11)
		{
			TypedValue tv = new TypedValue();
			context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
			reslist = tv.resourceId;
		}
		else
		{
			reslist = android.support.v7.appcompat.R.drawable.abc_list_selector_holo_dark;
		}

		this.context = context;
		this.adapter = adapter;
		this.tf = tf;
		this.db = db;
		this.afav = afav;
	}

	@Override
	public int getCount()
	{
		return adapter.getCount();
	}

	@Override
	public CharSequence getPageTitle(int position)
	{
		return (adapter.getItemId(position) != -1 ? String.format("U+%04X ", adapter.getItemId(position)) : adapter.getItemString(position) + " ") + adapter.getItem(position);
	}

	private static final String[] cols = {"name", "utf8", "version", "comment", "alias", "formal", "xref", "vari", "decomp", "compat"};
	@SuppressWarnings("MismatchedReadAndWriteOfArray")
	private static final String[] mods = {null, "UTF-8: ", "from Unicode ", "\u2022 ", "= ", "\u203B ", "\u2192 ", "~ ", "\u2261 ", "\u2248 "};
	private static final String[] emjs = {"name", "utf8", "version", "grp", "subgrp", "", "id"};
	@SuppressWarnings("MismatchedReadAndWriteOfArray")
	private static final String[] mode = {null, "UTF-8: ", "from Unicode ", "Group: ", "Subgroup: ", null, ""};

	@SuppressLint("NewApi")
	@Override
	public Object instantiateItem(final ViewGroup collection, final int position)
	{
		CharacterView text = new CharacterView(context);
		text.setText((String)adapter.getItem(position));
		text.setTextSize(fontsize);
		text.setTypeface(tf);
		text.drawSlash(false);
		if (Build.VERSION.SDK_INT >= 11)
			text.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		text.setTextColor(Color.BLACK);
		text.setBackgroundColor(Color.WHITE);
		text.setSquareAlpha((int)Math.min(Math.max(checker * 2.55f, 0), 255));
		text.drawLines(lines);
		text.shrinkWidth(shrink);
		text.shrinkHeight(true);
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(text, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		final int itemid = (int)adapter.getItemId(position);
		final boolean emoji = itemid == -1;
		int ver = !emoji ? db.getint(itemid, "version") : db.getint(adapter.getItemString(position), "version");
		text.setValid(ver != 0 && ver <= UnicodeActivity.univer);
		final StringBuilder str = new StringBuilder();
		if (!emoji)
			str.append((String)adapter.getItem(position));
		OnLongClickListener lsn = new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View arg0)
			{
				context.adpPage.showDesc(arg0, arg0.getId() - 0x3F000000, new StringAdapter(str.toString()));
				return true;
			}
		};
		for (int i = 0; i < (!emoji ? 10 : 7); ++i)
		{
			if (emoji && i == 5)
				continue;
			if (i == 2)
			{
				int v = !emoji ? db.getint(itemid, cols[i]) : db.getint(adapter.getItemString(position), emjs[i]);
				TextView desc = new TextView(context);
				desc.setText((!emoji ? mods : mode)[i] + String.format(Locale.US, "%d.%d.%d", v / 100, v / 10 % 10, v % 10) + (v == 600 ? " or earlier" : ""));
				desc.setGravity(Gravity.CENTER_VERTICAL);
				layout.addView(desc, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				continue;
			}
			String r;
			if (i == 1)
			{
				byte[] a = ((String)adapter.getItem(position)).getBytes(StandardCharsets.UTF_8);
				StringBuilder sb = new StringBuilder(a.length * 3);
				for(byte b : a)
					sb.append(String.format("%02X ", b));
				sb.deleteCharAt(a.length * 3 - 1);
				r = sb.toString();
			} else
			{
				r = !emoji ? db.get(itemid, cols[i]) : db.get(adapter.getItemString(position), emjs[i]);
			}
			if (r == null && i == 0)
			{
				TextView desc = new TextView(context);
				desc.setText(R.string.notacharacter);
				layout.addView(desc, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				break;
			}
			if (r == null)
				continue;
			String[] l = r.split(emoji && i == 6 ? " " : "\n");
			for (String s : l)
			{
				if (i == 0)
				{
					TextView desc = new TextView(context, null, android.R.attr.textAppearanceMedium);
					desc.setText(s);
					if (Build.VERSION.SDK_INT >= 11)
						desc.setTextIsSelectable(true);
					desc.setGravity(Gravity.CENTER_VERTICAL);
					if (!emoji)
					{
						CheckBox fav = new CheckBox(context);
						fav.setButtonDrawable(android.R.drawable.btn_star);
						fav.setGravity(Gravity.TOP);
						fav.setChecked(afav.isfavorited(itemid));
						fav.setOnCheckedChangeListener(new OnCheckedChangeListener()
						{
							public void onCheckedChanged(CompoundButton arg0, boolean arg1)
							{
								if (arg1)
									afav.add(itemid);
								else
									afav.remove(itemid);
							}
						});
						LinearLayout hl = new LinearLayout(context);
						hl.setOrientation(LinearLayout.HORIZONTAL);
						hl.addView(desc, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
						hl.addView(fav, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
						layout.addView(hl, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
					}
					else
						layout.addView(desc, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
					continue;
				}
				LinearLayout hl = new LinearLayout(context);
				hl.setOrientation(LinearLayout.HORIZONTAL);
				TextView it = new TextView(context);
				it.setGravity(Gravity.CENTER_VERTICAL);
				it.setText((!emoji ? mods : mode)[i]);
				hl.addView(it, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
				if (i < 6)
				{
					TextView desc = new TextView(context);
					desc.setText(s);
					if (Build.VERSION.SDK_INT >= 11)
						desc.setTextIsSelectable(true);
					hl.addView(desc, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
				}
				else
				{
					String cs = "";
					String ps = "";
					String ns = null;
					Scanner sc = new Scanner(s);
					for (int j = 0; sc.hasNext(); ++j)
					{
						if (i == 9 && j == 0 && s.charAt(0) == '<')
						{
							ns = sc.next();
							continue;
						}
						int tgt = sc.nextInt(16);
						cs += String.valueOf(Character.toChars(tgt));
						ps += String.format("U+%04X ", tgt);
						if (i == 6)
						{
							String n = db.get(tgt, "name");
							ns = n != null ? n : "<not a character>";
							break;
						}
						if (i == 7 && j == 1)
						{
							sc.useDelimiter("\n");
							sc.skip(" ");
							ns = sc.hasNext() ? sc.next() : "";
							break;
						}
					}
					sc.close();
					if (ps.length() == 0)
						continue;
					ps = ps.substring(0, ps.length() - 1);

					CharacterView ct = new CharacterView(context, null, android.R.attr.textAppearanceLarge);
					ct.setPadding(0, 0, 0, 0);
					ct.setPadding(UnicodeAdapter.padding, UnicodeAdapter.padding, UnicodeAdapter.padding, UnicodeAdapter.padding);
					ct.drawSlash(false);
					ct.setTextSize(UnicodeAdapter.fontsize);
					ct.setText(cs);
					ct.setTypeface(tf);
					hl.addView(ct, new LinearLayout.LayoutParams((int)(context.getResources().getDisplayMetrics().scaledDensity * UnicodeAdapter.fontsize * 2 + UnicodeAdapter.padding * 2), LayoutParams.MATCH_PARENT));
					TextView pt = new TextView(context, null, android.R.attr.textAppearanceSmall);
					pt.setPadding(0, 0, 0, 0);
					pt.setGravity(Gravity.CENTER_VERTICAL);
					pt.setText(ps);
					if (ns != null)
					{
						TextView nt = new TextView(context, null, android.R.attr.textAppearanceSmall);
						nt.setPadding(0, 0, 0, 0);
						nt.setGravity(Gravity.CENTER_VERTICAL);
						nt.setText(ns);
						LinearLayout vl = new LinearLayout(context);
						vl.setOrientation(LinearLayout.VERTICAL);
						vl.addView(pt, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));
						vl.addView(nt, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));
						hl.addView(vl, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
					}
					else
						hl.addView(pt, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
					hl.setId(0x3F000000 + str.codePointCount(0, str.length()));
					str.append(cs);
					hl.setEnabled(true);
					hl.setClickable(true);
					hl.setFocusable(true);
					hl.setOnClickListener(this);
					hl.setOnLongClickListener(lsn);
					hl.setBackgroundResource(reslist);
				}
				layout.addView(hl, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			}
		}
		ScrollView scroll = new ScrollView(context);
		scroll.addView(layout);
		collection.addView(scroll);
		collection.findViewById(R.id.TAB_ID).measure(MeasureSpec.makeMeasureSpec(10, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		layout.setPadding(0, 0, 0, collection.findViewById(R.id.TAB_ID).getMeasuredHeight() * 2);
		return scroll;
	}

	@Override
	public void destroyItem(final ViewGroup collection, final int position, final Object view)
	{
		collection.removeView((View)view);
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1)
	{
		return arg0 == arg1;
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object)
	{
		index = position;
	}

	int getIndex()
	{
		return index;
	}

	long getId()
	{
		return adapter.getItemId(index);
	}

	@Override
	public void onClick(View v)
	{
		String s = ((CharacterView)((LinearLayout)v).getChildAt(1)).getText();
		for (int i = 0; i < s.length(); ++i)
		{
			int code = s.codePointAt(i);
			if (code > 0xFFFF)
				++i;
			context.adpPage.onItemClick(null, null, -1, code);
		}
	}

}
