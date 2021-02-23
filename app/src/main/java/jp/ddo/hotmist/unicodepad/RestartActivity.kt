package jp.ddo.hotmist.unicodepad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Process

class RestartActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Process.killProcess(intent.getIntExtra("pid_key", -1))
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName(packageName, UnicodeActivity::class.java.name)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
        Process.killProcess(Process.myPid())
    }
}