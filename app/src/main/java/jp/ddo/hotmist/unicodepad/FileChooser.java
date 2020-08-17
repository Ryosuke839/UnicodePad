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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

class FileChooser implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, ActivityCompat.OnRequestPermissionsResultCallback
{
	private final Activity activity;
	private final Listener listener;
	private String path;
	private String[] roots;
	private String[] children;

	interface Listener
	{
		void onFileChosen(String path);

		void onFileCancel();
	}

	FileChooser(Activity activity, Listener listener, String path)
	{
		this.activity = activity;
		this.listener = listener;
		this.path = path;
	}

	public boolean show()
	{
		if (Build.VERSION.SDK_INT >= 23)
		{
			if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
			{
				if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE))
				{
					ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
					return false;
				}
				Toast.makeText(activity, R.string.denied, Toast.LENGTH_LONG).show();
			}
		}
		onClick(null, -1);
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		if (requestCode == 1)
			onClick(null, -1);
	}

	private ZipFile openZip(String path) throws IOException, IllegalArgumentException
	{
		ZipFile zf = new ZipFile(path);
		try
		{
			for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); e.nextElement())
				;
		}
		catch (IllegalArgumentException e1)
		{
			zf.close();
			PASSED:
			{
				if (Build.VERSION.SDK_INT >= 24)
				{
					// Try to find valid charset
					for (Charset charset : Charset.availableCharsets().values())
					{
						zf = new ZipFile(path, charset);
						try
						{
							for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); e.nextElement())
								;
						}
						catch (IllegalArgumentException e2)
						{
							zf.close();
							continue;
						}
						// Found
						break PASSED;
					}
				}
				// Not found
				throw new IllegalArgumentException();
			}
		}
		return zf;
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if (which != -1)
		{
			if (path.endsWith(".zip"))
			{
				if (children[which].equals(("../")))
					path = path.substring(0, path.lastIndexOf('/') + 1);
				else
				{
					//noinspection EmptyCatchBlock
					try
					{
						ZipFile zf = openZip(path);

						ZipEntry ze = zf.getEntry(children[which]);
						InputStream is = zf.getInputStream(ze);
						File of = new File(activity.getFilesDir(), String.format("%08x", ze.getCrc()) + "/" + new File(ze.getName()).getName());

						of.getParentFile().mkdirs();

						OutputStream os = new FileOutputStream(of);

						byte[] buf = new byte[256];
						int size;
						while ((size = is.read(buf)) > 0)
							os.write(buf, 0, size);

						os.close();
						is.close();
						zf.close();

						//noinspection EmptyCatchBlock
						try
						{
							if (path.startsWith(activity.getFilesDir().getCanonicalPath()))
								//noinspection ResultOfMethodCallIgnored
								new File(path).delete();
						}
						catch (IOException e)
						{
						}

						path = of.getCanonicalPath();
					}
					catch (ZipException e)
					{
						Toast.makeText(activity, R.string.cantread, Toast.LENGTH_SHORT).show();
						listener.onFileCancel();
						path = null;
						return;
					}
					catch (IllegalArgumentException e)
					{
						Toast.makeText(activity, R.string.malformed, Toast.LENGTH_SHORT).show();
						listener.onFileCancel();
						path = null;
						return;
					}
					catch (IOException e)
					{
					}
				}
			}
			else
			{
				if (path.length() != 1 && which == 0)
				{
					for (String root : roots)
						if (path.equals('/' + root))
							path = "";
				}
				if (path.length() == 0)
					path = "/";
				else
					path += children[which];
			}
		}

		if (which != -1 && path.charAt(path.length() - 1) != '/' && !path.endsWith(".zip"))
		{
			children = null;
			listener.onFileChosen(path);
			path = null;
		}
		else
		{
			//noinspection EmptyCatchBlock
			try
			{
				if (path.length() == 1)
				{
					String[] dirs = new String[4];
					dirs[0] = Environment.getExternalStorageDirectory().getCanonicalPath();
					dirs[1] = Environment.getDataDirectory().getCanonicalPath();
					dirs[2] = Environment.getDownloadCacheDirectory().getCanonicalPath();
					dirs[3] = Environment.getRootDirectory().getCanonicalPath();
					for (int i = 0; i < dirs.length; ++i)
						for (int j = 0; j < dirs.length; ++j)
							if (i != j && dirs[i].length() > 0 && dirs[j].length() > 0 && dirs[i].startsWith(dirs[j]) && new File(dirs[j]).canRead())
								dirs[i] = "";
					int cnt = 0;
					for (String dir1 : dirs)
					{
						if (!(dir1.length() > 0))
							continue;
						if (!new File(dir1).canRead())
							continue;
						++cnt;
					}
					roots = new String[cnt];
					int j = 0;
					for (String dir : dirs)
					{
						if (!(dir.length() > 0))
							continue;
						if (!new File(dir).canRead())
							continue;
						roots[j] = dir.substring(1) + '/';
						++j;
					}
					Arrays.sort(roots);
					children = roots;
				}
				else if (path.endsWith(".zip"))
				{
					try
					{
						ZipFile zf = openZip(path);
						int cnt = which != -1 ? 1 : 0;
						for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); )
							if (!e.nextElement().isDirectory())
								++cnt;
						children = new String[cnt];
						int j = 0;
						if (which != -1)
							children[j++] = "../";
						for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); )
						{
							ZipEntry entry = e.nextElement();
							if (entry.isDirectory())
								continue;
							children[j] = entry.getName();
							++j;
						}
						zf.close();
					}
					catch (ZipException e)
					{
						Toast.makeText(activity, R.string.cantread, Toast.LENGTH_SHORT).show();
						listener.onFileCancel();
						path = null;
						return;
					}
					catch (IllegalArgumentException e)
					{
						Toast.makeText(activity, R.string.malformed, Toast.LENGTH_SHORT).show();
						listener.onFileCancel();
						path = null;
						return;
					}
				}
				else
				{
					File f = new File(path);
					path = f.getCanonicalPath();
					if (!path.endsWith("/"))
						path += '/';
					File[] fl = f.listFiles();

					if (fl == null)
					{
						Toast.makeText(activity, R.string.cantread, Toast.LENGTH_SHORT).show();
						listener.onFileCancel();
						path = null;
						return;
					}
					int cnt = 1;
					for (File aFl1 : fl)
						if (aFl1.canRead())
							++cnt;
					children = new String[cnt];
					children[0] = "../";
					int j = 1;
					for (File aFl : fl)
					{
						if (!aFl.canRead())
							continue;
						children[j] = aFl.getName();
						if (aFl.isDirectory())
							children[j] += '/';
						++j;
					}
				}
				new AlertDialog.Builder(activity).setTitle(path.startsWith(activity.getFilesDir().getCanonicalPath()) ? path.substring(path.lastIndexOf('/') + 1) : path).setItems(children, this).setOnCancelListener(this).show();
			}
			catch (IOException e)
			{
			}
		}
	}

	@Override
	public void onCancel(DialogInterface dialog)
	{
		listener.onFileCancel();
	}
}
