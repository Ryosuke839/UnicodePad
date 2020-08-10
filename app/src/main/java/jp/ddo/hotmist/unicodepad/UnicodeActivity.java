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
import androidx.core.view.MenuItemCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

public class UnicodeActivity extends AppCompatActivity implements OnClickListener, OnTouchListener, OnEditorActionListener, FontChooser.Listener
{
	private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	private static final String REPLACE_KEY = "replace_key";
	private boolean isMush;
	private EditText editText;
	private ImageButton btnClear;
	private ImageButton btnDelete;
	private Button btnCopy;
	private Button btnFind;
	private Button btnPaste;
	private Button btnFinish;
	FontChooser chooser;
	private LockableScrollView scroll;
	private ViewPager pager;
	PageAdapter adpPage;
	private AdView adView;
	@SuppressWarnings("deprecation")
	private ClipboardManager cm;
	private SharedPreferences pref;
	private boolean disableime;
	private static float fontsize = 24.0f;
	static int univer = 1000;

	public View onCreateView(View parent, String name, Context context, AttributeSet attrs)
	{
		return super.onCreateView(parent, name, context, attrs);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		onActivityResult(-1, 0, null);
		int[] themelist =
				{
						R.style.Theme,
						R.style.Theme_Light,
						R.style.Theme_Light_DarkActionBar,
				};
		setTheme(themelist[Integer.valueOf(pref.getString("theme", "2131492983")) - 2131492983]);
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.main);
		editText = (EditText)findViewById(R.id.text);
		editText.setOnTouchListener(this);
		editText.setTextSize(fontsize);
		btnClear = (ImageButton)findViewById(R.id.clear);
		btnClear.setOnClickListener(this);
		btnClear.setVisibility(pref.getBoolean("clear", false) ? View.VISIBLE : View.GONE);
		btnDelete = (ImageButton)findViewById(R.id.delete);
		btnDelete.setOnTouchListener(this);
		btnCopy = (Button)findViewById(R.id.copy);
		btnCopy.setOnClickListener(this);
		btnFind = (Button)findViewById(R.id.find);
		btnFind.setOnClickListener(this);
		btnPaste = (Button)findViewById(R.id.paste);
		btnPaste.setOnClickListener(this);
		btnFinish = (Button)findViewById(R.id.finish);
		btnFinish.setOnClickListener(this);
		chooser = new FontChooser(this, (Spinner)findViewById(R.id.font), this);
		scroll = (LockableScrollView)findViewById(R.id.scrollView);
		pager = (ViewPager)findViewById(R.id.cpager);
		pager.setOffscreenPageLimit(3);
		adpPage = new PageAdapter(this, pref, editText);
		pager.setAdapter(adpPage);
		scroll.setAdapter(adpPage);
		scroll.setLockView(pager, Integer.valueOf(pref.getString("scroll", "1")) + (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 1 : 0) > 1);

		//noinspection deprecation
		cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
		disableime = pref.getBoolean("ime", true);

		pager.setCurrentItem(Math.min(pref.getInt("page", 1), adpPage.getCount() - 1), false);

		Intent it = getIntent();
		String action = it.getAction();
		editText.setImeOptions(action != null && ACTION_INTERCEPT.equals(action) ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_SEND);
		if (action != null && ACTION_INTERCEPT.equals(action))
		{
			isMush = true;
			String str = it.getStringExtra(REPLACE_KEY);
			if (str != null)
				editText.append(str);
			btnFinish.setText(R.string.finish);
		}
		else
		{
			isMush = false;
			btnFinish.setText(R.string.share);
		}
		if (action != null && Intent.ACTION_SEND.equals(action))
		{
			String str = it.getStringExtra(Intent.EXTRA_TEXT);
			if (str != null)
				editText.append(str);
		}

		if (!pref.getBoolean("no-ad", false))
		{
			try
			{
				MobileAds.initialize(this, new OnInitializationCompleteListener()
				{
					@Override
					public void onInitializationComplete(InitializationStatus initializationStatus)
					{
					}
				});
				adView = new AdView(this);
				DisplayMetrics outMetrics = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
				adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, (int)(outMetrics.widthPixels / outMetrics.density)));
				adView.setAdUnitId("ca-app-pub-8779692709020298/6882844952");
				((LinearLayout)findViewById(R.id.adContainer)).addView(adView);
				AdRequest adRequest = new AdRequest.Builder().build();
				adView.loadAd(adRequest);
			}
			catch (NullPointerException e)
			{
			}
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);

		scroll.setLockView(pager, Integer.valueOf(pref.getString("scroll", "1")) + (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 1 : 0) > 1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuItem actionItem = menu.add("Setting");
		MenuItemCompat.setShowAsAction(actionItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		actionItem.setIcon(android.R.drawable.ic_menu_preferences);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		startActivityForResult(new Intent(this, SettingActivity.class), 0);

		return true;
	}

	@Override
	public void onPause()
	{
		SharedPreferences.Editor edit = pref.edit();
		adpPage.save(edit);
		chooser.Save(edit);
		edit.putInt("page", pager.getCurrentItem());
		edit.apply();
		super.onPause();
	}

	@Override
	public void onClick(View v)
	{
		if (v == btnClear)
		{
			editText.setText("");
		}
		if (v == btnCopy)
		{
			cm.setText(editText.getText().toString());
			Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
		}
		if (v == btnFind)
		{
			String str = editText.getEditableText().toString();
			if (str.length() == 0)
				return;
			int start = editText.getSelectionStart();
			if (start == -1)
				return;
			int end = editText.getSelectionEnd();
			adpPage.showDesc(null, str.codePointCount(0, start == end ? start == 0 ? 0 : start - 1 : Math.min(start, end)), new StringAdapter(str));
		}
		if (v == btnPaste)
		{
			editText.setText(cm.getText());
		}
		if (v == btnFinish)
		{
			if (isMush)
			{
				replace(editText.getText().toString());
			}
			else
			{
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, editText.getText().toString());
				startActivity(intent);
			}
		}
	}

	private Runnable delay = null;
	private int timer = 500;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		v.onTouchEvent(event);
		if (v == btnDelete)
		{
			switch (event.getAction())
			{
			case MotionEvent.ACTION_DOWN:
				if (delay == null)
				{
					delay = new Runnable()
					{
						@Override
						public void run()
						{
							String str = editText.getEditableText().toString();
							if (str.length() == 0)
								return;
							int start = editText.getSelectionStart();
							if (start < 1)
								return;
							int end = editText.getSelectionEnd();
							if (start < 1)
								return;
							if (start != end)
								editText.getEditableText().delete(Math.min(start, end), Math.max(start, end));
							else if (start > 1 && Character.isSurrogatePair(str.charAt(start - 2), str.charAt(start - 1)))
								editText.getEditableText().delete(start - 2, start);
							else
								editText.getEditableText().delete(start - 1, start);
//							editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
							if (delay != null)
							{
								editText.postDelayed(delay, timer);
								if (timer > 100)
									timer -= 200;
							}
						}
					};
					editText.post(delay);
				}
				break;
			case MotionEvent.ACTION_UP:
				editText.removeCallbacks(delay);
				delay = null;
				timer = 500;
				break;
			}
		}
		if (v == editText)
		{
			if (disableime)
				((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
		return true;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP)
		{
			startActivityForResult(new Intent(this, SettingActivity.class), 0);
			return true;
		}
		return super.dispatchKeyEvent(e);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == FontChooser.FONT_REQUEST_CODE)
			if (resultCode == Activity.RESULT_OK && data != null)
			{
				Uri uri = data.getData();
				String name = uri.getPath();
				while (name.endsWith("/"))
					name = name.substring(0, name.length() - 1);
				if (name.contains(("/")))
					name = name.substring(name.lastIndexOf("/") + 1);
				Cursor cursor = getContentResolver().query( data.getData(), null, null, null, null);
				if (cursor != null)
				{
					if (cursor.moveToFirst())
						name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
					cursor.close();
				}
				name.replaceAll("[?:\"*|/\\\\<>]", "_");

				try
				{
					InputStream is = getContentResolver().openInputStream(uri);
					File of = new File(getFilesDir(), "00000000/" + name);
					of.getParentFile().mkdirs();
					try
					{
						OutputStream os = new FileOutputStream(of);
						CRC32 crc = new CRC32();
						byte[] buf = new byte[256];
						int size;
						while ((size = is.read(buf)) > 0)
						{
							os.write(buf, 0, size);
							crc.update(buf, 0, size);
						}
						os.close();
						File mf = new File(getFilesDir(), String.format("%08x", crc.getValue()) + "/" + name);
						mf.getParentFile().mkdirs();
						of.renameTo(mf);
						chooser.onFileChosen(mf.getCanonicalPath());
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					is.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else
				chooser.onFileCancel();
		if (requestCode != -1)
			super.onActivityResult(requestCode, resultCode, data);
		try
		{
			fontsize = Float.valueOf(pref.getString("textsize", "24.0"));
		}
		catch (NumberFormatException e)
		{
		}
		try
		{
			univer = Integer.valueOf(pref.getString("universion", "Latest").replace(".", ""));
		}
		catch (NumberFormatException e)
		{
			univer = Integer.MAX_VALUE;
		}
		try
		{
			PageAdapter.column = Integer.valueOf(pref.getString("column", "8"));
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
				PageAdapter.column = Integer.valueOf(pref.getString("columnl", String.valueOf(PageAdapter.column)));
		}
		catch (NumberFormatException e)
		{
		}
		try
		{
			UnicodeAdapter.padding = Integer.valueOf(pref.getString("padding", "4"));
		}
		catch (NumberFormatException e)
		{
		}
		try
		{
			UnicodeAdapter.fontsize = Float.valueOf(pref.getString("gridsize", "24.0"));
		}
		catch (NumberFormatException e)
		{
		}
		try
		{
			CharacterAdapter.fontsize = Float.valueOf(pref.getString("viewsize", "120.0"));
		}
		catch (NumberFormatException e)
		{
		}
		try
		{
			CharacterAdapter.checker = Float.valueOf(pref.getString("checker", "15.0"));
		}
		catch (NumberFormatException e)
		{
		}
		CharacterAdapter.lines = pref.getBoolean("lines", true);
		UnicodeAdapter.shrink = pref.getBoolean("shrink", true);
		CharacterAdapter.shrink = pref.getBoolean("shrink", true);
		try
		{
			RecentAdapter.maxitems = Integer.valueOf(pref.getString("recentsize", "256"));
		}
		catch (NumberFormatException e)
		{
		}
		disableime = pref.getBoolean("ime", true);
		if (btnClear != null)
			btnClear.setVisibility(pref.getBoolean("clear", false) ? View.VISIBLE : View.GONE);
		if (editText != null)
			editText.setTextSize(fontsize);
		if (adpPage != null)
			adpPage.notifyDataSetChanged();
		if (scroll != null)
			scroll.setLockView(pager, Integer.valueOf(pref.getString("scroll", "1")) + (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 1 : 0) > 1);
		if (requestCode != -1)
		{
			LinearLayout adConteiner = (LinearLayout)findViewById(R.id.adContainer);
			if (adConteiner != null)
			{
				if (!pref.getBoolean("no-ad", false))
				{
					if (adConteiner.getChildCount() == 0)
					{
						MobileAds.initialize(this, new OnInitializationCompleteListener()
						{
							@Override
							public void onInitializationComplete(InitializationStatus initializationStatus)
							{
							}
						});
						adView = new AdView(this);
						DisplayMetrics outMetrics = new DisplayMetrics();
						getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
						adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, (int)(outMetrics.widthPixels / outMetrics.density)));
						adView.setAdUnitId("ca-app-pub-8779692709020298/6882844952");
						((LinearLayout)findViewById(R.id.adContainer)).addView(adView);
						AdRequest adRequest = new AdRequest.Builder().build();
						adView.loadAd(adRequest);
					}
				}
				else
				{
					if (adConteiner.getChildCount() > 0)
					{
						adConteiner.removeAllViews();
					}
				}
			}
		}
	}

	private void replace(String result)
	{
		Intent data = new Intent();
		data.putExtra(REPLACE_KEY, result);
		setResult(RESULT_OK, data);
		finish();
	}

	void setPage(int page)
	{
		pager.setCurrentItem(page);
	}

	public void onTypefaceChosen(Typeface typeface)
	{
		setTypeface(typeface);
	}

	private Typeface oldtf = null;

	private void setTypeface(Typeface tf)
	{
		if (tf == oldtf)
			return;
		oldtf = tf;
		editText.setTypeface(tf);
		adpPage.setTypeface(tf);
	}

	@Override
	public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2)
	{
		if (arg0 == editText && arg2 != null && arg2.getKeyCode() == KeyEvent.KEYCODE_ENTER)
		{
			if (arg2.getAction() == KeyEvent.ACTION_DOWN)
				btnFinish.performClick();
			return true;
		}
		return false;
	}

}
