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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FontChooser implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, AdapterView.OnItemSelectedListener, FileChooser.Listener
{
	private final Activity activity;
	private final Spinner spinner;
	private final Listener listener;
	private final ArrayAdapter<String> adapter;
	private int fidx;
	private final ArrayList<String> fontpath;

	interface Listener
	{
		void onTypefaceChosen(Typeface typeface);
	}

	FontChooser(Activity activity, Spinner spinner, Listener listener)
	{
		this.activity = activity;
		this.spinner = spinner;
		this.listener = listener;

		adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		adapter.add(activity.getResources().getString(R.string.normal));
		adapter.add(activity.getResources().getString(R.string.add));
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(this);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
		fontpath = new ArrayList<>();
		String fs = pref.getString("fontpath", "");
		for (String s : fs.split("\n"))
		{
			if (s.length() == 0)
				continue;
			Add(s);
		}
		fidx = pref.getInt("fontidx", 0);
		if (fidx > fontpath.size())
			fidx = 0;
		spinner.setSelection(fidx == 0 ? 0 : fidx + 2);
	}

	void Save(SharedPreferences.Editor edit)
	{
		String fs = "";
		for (String s : fontpath)
			fs += s + "\n";
		edit.putString("fontpath", fs);
		edit.putInt("fontidx", spinner.getSelectedItemId() > 2 ? (int)spinner.getSelectedItemId() - 2 : 0);
	}

	private boolean Add(String path)
	{
		if (Load(path) == null)
			return false;

		// Add remove item
		if (adapter.getCount() < 3)
			adapter.add(activity.getResources().getString(R.string.rem));

		// Remove duplicated items
		for (int i = 0; i < fontpath.size(); ++i)
		{
			if (!path.equals(fontpath.get(i)))
				continue;
			adapter.remove(adapter.getItem(i + 3));
			fontpath.remove(i);
		}

		adapter.add(new File(path).getName());
		fontpath.add(path);

		return true;
	}

	private Typeface Load(String path)
	{
		try
		{
			return Typeface.createFromFile(path);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private void Remove(int which)
	{
		adapter.remove(adapter.getItem(which + 3));
		//noinspection EmptyCatchBlock
		try
		{
			if (fontpath.get(which).startsWith(activity.getFilesDir().getCanonicalPath()))
				//noinspection ResultOfMethodCallIgnored
				new File(fontpath.get(which)).delete();
		}
		catch (IOException e)
		{
		}
		fontpath.remove(which);
		if (fidx == which + 1)
			fidx = 0;
		if (fidx > which + 1)
			--fidx;

		// Remove remove item
		if (fontpath.size() == 0)
			adapter.remove(adapter.getItem(2));
		spinner.setSelection(fidx == 0 ? 0 : fidx + 2);
	}

	@SuppressLint("InlinedApi")
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		if (parent != spinner)
			return;
		switch (position)
		{
		case 0:
			listener.onTypefaceChosen(Typeface.DEFAULT);
			fidx = 0;
			break;
		case 1:
			new FileChooser(activity, this).show();
			break;
		case 2:
			onClick(null, -1);
			break;
		default:
			fidx = position - 2;
			Typeface tf = Load(fontpath.get(position - 3));
			if (tf != null)
				listener.onTypefaceChosen(tf);
			else
				Remove(position - 3);
			break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if (which == -1)
		{
			String[] str;
			str = new String[fontpath.size()];
			for (int i = 0; i < str.length; ++i)
				str[i] = adapter.getItem(i + 3);
			new AlertDialog.Builder(activity).setTitle(R.string.rem).setItems(str, this).setOnCancelListener(this).show();
		}
		else
		{
			Remove(which);
		}
	}

	@Override
	public void onCancel(DialogInterface dialog)
	{
		spinner.setSelection(fidx == 0 ? 0 : fidx + 2);
	}

	public void onFileChosen(String path)
	{
		if (Add(path))
		{
			spinner.setSelection(adapter.getCount() - 1);
		}
		else
		{
			Toast.makeText(activity, R.string.cantopen, Toast.LENGTH_SHORT).show();
			//noinspection EmptyCatchBlock
			try
			{
				if (path.startsWith(activity.getFilesDir().getCanonicalPath()))
					//noinspection ResultOfMethodCallIgnored
					new File(path).delete();
			}
			catch (IOException e2)
			{
			}
			spinner.setSelection(0);
		}
	}

	public void onFileCancel()
	{
		spinner.setSelection(fidx == 0 ? 0 : fidx + 2);
	}
}
