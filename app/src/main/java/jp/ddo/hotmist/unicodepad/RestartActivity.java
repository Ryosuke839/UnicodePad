package jp.ddo.hotmist.unicodepad;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class RestartActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		android.os.Process.killProcess(getIntent().getIntExtra("pid_key", -1));

		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setClassName(getPackageName(), UnicodeActivity.class.getName());
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);

		finish();
		android.os.Process.killProcess(android.os.Process.myPid());
	}}
