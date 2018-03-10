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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.*;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

class NameDatabase
{
	private SQLiteDatabase db;

	NameDatabase(Context context)
	{
		db = new NameHelper(context).getReadableDatabase();
	}

	@SuppressWarnings("ConstantConditions")
	String get(int code, String column)
	{
		if (column.equals("name"))
		{
			if (0xE000 <= code && code <= 0xF8FF ||
					0xFFF80 <= code && code <= 0xFFFFD ||
					0x10FF80 <= code && code <= 0x10FFFD)
				return "Private Use";
			if (0x3400 <= code && code <= 0x4DB5 ||
					0x4E00 <= code && code <= 0x9FEA ||
					0x20000 <= code && code <= 0x2A6D6 ||
					0x2A700 <= code && code <= 0x2B734 ||
					0x2B740 <= code && code <= 0x2B81D ||
					0x2B820 <= code && code <= 0x2CEA1 ||
					0x2CEB0 <= code && code <= 0x2EBE0)
				return "CJK Unified Ideograph";
			if (0xAC00 <= code && code <= 0xD7A3)
				return "Hangul Syllable";
			if (0x17000 <= code && code <= 0x187EC)
				return "Tangut Character";
		}
		return get("name_table", String.valueOf(code), column);
	}

	String get(String code, String column)
	{
		return get("emoji_table", "'" + code + "'", column);
	}

	private String get(String table, String code, String column)
	{
		try
		{
			Cursor cur = db.rawQuery("SELECT " + column + " FROM " + table + " WHERE id = " + code, null);
			if (cur.getCount() != 1)
			{
				cur.close();
				return null;
			}
			cur.moveToFirst();
			String str = cur.getString(0);
			cur.close();
			return str;
		}
		catch (SQLiteException e)
		{
			return "Error: " + e.getLocalizedMessage();
		}
	}

	@SuppressWarnings("ConstantConditions")
	int getint(int code, String column)
	{
		if (column.equals("version"))
		{
			if (0xE000 <= code && code <= 0xF8FF ||
					0xFFF80 <= code && code <= 0xFFFFD ||
					0x10FF80 <= code && code <= 0x10FFFD)
				return 600;
			if (0x3400 <= code && code <= 0x4DB5 ||
					0x4E00 <= code && code <= 0x9FCB ||
					0x20000 <= code && code <= 0x2A6D6 ||
					0x2A700 <= code && code <= 0x2B734 ||
					0x2B740 <= code && code <= 0x2B81D)
				return 600;
			if (0x9FCC <= code && code <= 0x9FCC)
				return 610;
			if (0x9FCD <= code && code <= 0x9FD5 ||
					0x2B820 <= code && code <= 0x2CEA1)
				return 800;
			if (0x17000 <= code && code <= 0x187EC)
				return 900;
			if (0x9FD6 <= code && code <= 0x9FEA ||
					0x2CEB0 <= code && code <= 0x2EBE0)
				return 1000;
			if (0xAC00 <= code && code <= 0xD7A3)
				return 600;
		}
		return getint("name_table", String.valueOf(code), column);
	}

	int getint(String code, String column)
	{
		return getint("emoji_table", "'" + code + "'", column);
	}

	private int getint(String table, String code, String column)
	{
		try
		{
			Cursor cur = db.rawQuery("SELECT " + column + " FROM " + table + " WHERE id = " + code, null);
			if (cur.getCount() != 1)
			{
				cur.close();
				return 0;
			}
			cur.moveToFirst();
			int res = cur.getInt(0);
			cur.close();
			return res;
		}
		catch (SQLiteException e)
		{
			return 0;
		}
	}

	Cursor find(String str, int version)
	{
		String[] list = str.split(" +");
		if (list.length == 0)
			return null;
		String query = "SELECT id FROM name_table WHERE ";
		for (String s : list)
			query += "words LIKE '%" + s + "%' AND ";
		query += "version <= " + version + ";";
		try
		{
			return db.rawQuery(query, null);
		}
		catch (SQLiteException e)
		{
			return null;
		}
	}

	Cursor emoji(int version, boolean mod)
	{
		String query = "SELECT id, grp, subgrp FROM emoji_table WHERE version <= " + version + (mod ? ";" : " AND mod = 0;");
		try
		{
			return db.rawQuery(query, null);
		}
		catch (SQLiteException e)
		{
			return null;
		}
	}

	class NameHelper extends SQLiteOpenHelper
	{
		private Context context;
		private final String dbpath = "namedb";

		NameHelper(Context context)
		{
			super(context, "namedb", null, 1);
			this.context = context;
		}

		@Override
		public void onCreate(final SQLiteDatabase db)
		{
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
		}

		@Override
		public SQLiteDatabase getReadableDatabase()
		{
			try
			{
				SQLiteDatabase db = SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
				Cursor cur = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='name_table' OR name='emoji_table';", null);
				cur.moveToFirst();
				if (cur.getInt(0) != 2)
				{
					cur.close();
					db.close();
					throw new SQLiteException();
				}
				cur.close();
				Cursor cur2 = db.rawQuery("SELECT COUNT(*) FROM 'name_table';", null);
				cur2.moveToFirst();
				if (cur2.getInt(0) != 31630)
				{
					cur2.close();
					db.close();
					throw new SQLiteException();
				}
				cur2.close();
				Cursor cur3 = db.rawQuery("SELECT COUNT(*) FROM 'emoji_table';", null);
				cur3.moveToFirst();
				if (cur3.getInt(0) != 2623)
				{
					cur3.close();
					db.close();
					throw new SQLiteException();
				}
				cur3.close();
				return db;
			}
			catch (SQLiteException e)
			{
				if (e.getMessage() != null && e.getMessage().contains("attempt to write a readonly database"))
				{
					SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).getAbsolutePath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS).close();
					return SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
				}
				extractZipFiles("namedb.zip");
				try
				{
					return SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
				}
				catch (SQLiteException e1)
				{
					if (e.getMessage() != null && e.getMessage().contains("attempt to write a readonly database"))
					{
						SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).getAbsolutePath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS).close();
						return SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
					}
					throw e1;
				}
			}
		}

		private void extractZipFiles(String zipName)
		{
			try
			{
				InputStream inputStream = context.getAssets().open(zipName, AssetManager.ACCESS_STREAMING);

				ZipInputStream zipInputStream = new ZipInputStream(inputStream);
				ZipEntry zipEntry = zipInputStream.getNextEntry();

				while (zipEntry != null)
				{
					String entryName = zipEntry.getName();
					int n;
					FileOutputStream fileOutputStream = context.openFileOutput(entryName, Context.MODE_PRIVATE);

					byte[] buf = new byte[16384];
					while ((n = zipInputStream.read(buf, 0, 16384)) > -1)
						fileOutputStream.write(buf, 0, n);

					fileOutputStream.close();
					zipInputStream.closeEntry();
					zipEntry = zipInputStream.getNextEntry();
				}
				zipInputStream.close();
			}
			catch (FileNotFoundException e)
			{
				throw new Error("Cannot open database file to write.");
			}
			catch (IOException e)
			{
				throw new Error("Cannot open database file from asset.");
			}
		}
	}
}
