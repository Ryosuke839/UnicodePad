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
package jp.ddo.hotmist.unicodepad

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import java.util.zip.ZipException
import java.util.zip.ZipFile

internal class FileChooser(private val activity: Activity, private val listener: Listener, private var path: String) : DialogInterface.OnClickListener, DialogInterface.OnCancelListener, OnRequestPermissionsResultCallback {
    private var roots: Array<String> = emptyArray()
    private var children: Array<String> = emptyArray()

    internal interface Listener {
        fun onFileChosen(path: String)
        fun onFileCancel()
    }

    fun show(): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                    return false
                }
                Toast.makeText(activity, R.string.denied, Toast.LENGTH_LONG).show()
            }
        }
        onClick(null, -1)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1) onClick(null, -1)
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    private fun openZip(path: String): ZipFile {
        var zf = ZipFile(path)
        try {
            val e = zf.entries()
            while (e.hasMoreElements()) {
                e.nextElement()
            }
        } catch (e1: IllegalArgumentException) {
            zf.close()
            run passed@{
                if (Build.VERSION.SDK_INT >= 24) {
                    // Try to find valid charset
                    for (charset in Charset.availableCharsets().values) {
                        zf = ZipFile(path, charset)
                        try {
                            val e = zf.entries()
                            while (e.hasMoreElements()) {
                                e.nextElement()
                            }
                        } catch (e2: IllegalArgumentException) {
                            zf.close()
                            continue
                        }
                        // Found
                        return@passed
                    }
                }
                throw IllegalArgumentException()
            }
        }
        return zf
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which != -1) {
            if (path.endsWith(".zip")) {
                if (children[which] == "../") path = path.substring(0, path.lastIndexOf('/') + 1) else {
                    try {
                        openZip(path).use { zf ->
                            val ze = zf.getEntry(children[which])
                            zf.getInputStream(ze).use { `is` ->
                                val of = File(activity.filesDir, String.format("%08x", ze.crc) + "/" + File(ze.name).name)
                                of.parentFile?.mkdirs()
                                FileOutputStream(of).use { os ->
                                    val buf = ByteArray(256)
                                    var size: Int
                                    while (`is`.read(buf).also { size = it } > 0) os.write(buf, 0, size)
                                }
                                try {
                                    if (path.startsWith(activity.filesDir.canonicalPath)) File(path).delete()
                                } catch (e: IOException) {
                                }
                                path = of.canonicalPath
                            }
                        }
                    } catch (e: ZipException) {
                        Toast.makeText(activity, R.string.cantread, Toast.LENGTH_SHORT).show()
                        listener.onFileCancel()
                        return
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(activity, R.string.malformed, Toast.LENGTH_SHORT).show()
                        listener.onFileCancel()
                        return
                    } catch (e: IOException) {
                    }
                }
            } else {
                if (path.length != 1 && which == 0) {
                    for (root in roots) if (path == "/$root") path = ""
                }
                if (path.isEmpty()) path = "/" else path += children[which]
            }
        }
        if (which != -1 && path[path.length - 1] != '/' && !path.endsWith(".zip")) {
            children = emptyArray()
            listener.onFileChosen(path)
        } else {
            try {
                if (path.length == 1) {
                    val dirs = Array(4) {""}
                    @Suppress("DEPRECATION")
                    dirs[0] = Environment.getExternalStorageDirectory().canonicalPath
                    dirs[1] = Environment.getDataDirectory().canonicalPath
                    dirs[2] = Environment.getDownloadCacheDirectory().canonicalPath
                    dirs[3] = Environment.getRootDirectory().canonicalPath
                    for (i in dirs.indices) for (j in dirs.indices) if (i != j && dirs[i].isNotEmpty() && dirs[j].isNotEmpty() && dirs[i].startsWith(dirs[j]) && File(dirs[j]).canRead()) dirs[i] = ""
                    var cnt = 0
                    for (dir1 in dirs) {
                        if (dir1.isEmpty()) continue
                        if (!File(dir1).canRead()) continue
                        ++cnt
                    }
                    roots = Array(cnt) {""}
                    var j = 0
                    for (dir in dirs) {
                        if (dir.isEmpty()) continue
                        if (!File(dir).canRead()) continue
                        roots[j] = dir.substring(1) + '/'
                        ++j
                    }
                    Arrays.sort(roots)
                    children = roots
                } else if (path.endsWith(".zip")) {
                    try {
                        openZip(path).use { zf ->
                            var cnt = if (which != -1) 1 else 0
                            run {
                                val e = zf.entries()
                                while (e.hasMoreElements()) {
                                    if (!e.nextElement().isDirectory) ++cnt
                                }
                            }
                            children = Array(cnt) { "" }
                            var j = 0
                            if (which != -1) children[j++] = "../"
                            val e = zf.entries()
                            while (e.hasMoreElements()) {
                                val entry = e.nextElement()
                                if (entry.isDirectory) {
                                    continue
                                }
                                children[j] = entry.name
                                ++j
                            }
                        }
                    } catch (e: ZipException) {
                        Toast.makeText(activity, R.string.cantread, Toast.LENGTH_SHORT).show()
                        listener.onFileCancel()
                        return
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(activity, R.string.malformed, Toast.LENGTH_SHORT).show()
                        listener.onFileCancel()
                        return
                    }
                } else {
                    val f = File(path)
                    f.canonicalPath.let {
                        path = it + if (!it.endsWith("/")) "/" else ""
                    }
                    val fl = f.listFiles()
                    if (fl == null) {
                        Toast.makeText(activity, R.string.cantread, Toast.LENGTH_SHORT).show()
                        listener.onFileCancel()
                        return
                    }
                    var cnt = 1
                    for (aFl1 in fl) if (aFl1.canRead()) ++cnt
                    children = Array(cnt) {""}
                    children[0] = "../"
                    var j = 1
                    for (aFl in fl) {
                        if (!aFl.canRead()) continue
                        children[j] = aFl.name + if (aFl.isDirectory) "/" else ""
                        ++j
                    }
                }
                AlertDialog.Builder(activity).setTitle(if (path.startsWith(activity.filesDir.canonicalPath)) path.substring(path.lastIndexOf('/') + 1) else path).setItems(children, this).setOnCancelListener(this).show()
            } catch (e: IOException) {
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        listener.onFileCancel()
    }
}