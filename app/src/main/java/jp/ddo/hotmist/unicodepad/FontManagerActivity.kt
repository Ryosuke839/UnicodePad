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

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.woxthebox.draglistview.DragListView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.CRC32

class FontManagerActivity : BaseActivity() {
    private val fontData: FontData = FontData()
    private lateinit var adapter: FontManagerAdapter
    private lateinit var pref: SharedPreferences

    class DynamicDragListView(context: android.content.Context?, attrs: android.util.AttributeSet?) : DragListView(context, attrs) {
        init {
            super.onFinishInflate()
        }

        @SuppressLint("MissingSuperCall")
        override fun onFinishInflate() {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        fontData.loadFromPreferences(pref)
        adapter = FontManagerAdapter(this, fontData)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(DynamicDragListView(this@FontManagerActivity, null).apply {
                setLayoutManager(LinearLayoutManager(this@FontManagerActivity))
                setDragListListener(this@FontManagerActivity.adapter)
                setAdapter(this@FontManagerActivity.adapter, true)
                setCanDragHorizontally(false)
                setCanDragVertically(true)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(LinearLayout(this@FontManagerActivity).apply {
                addView(Button(this@FontManagerActivity).apply {
                    setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_add, 0, 0, 0)
                    setText(R.string.font_add)
                    setOnClickListener {
                        openFontChooser(FONT_ADD_CODE)
                    }
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    addView(Button(this@FontManagerActivity).apply {
                        setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_add, 0, 0, 0)
                        setText(R.string.font_add_fallback)
                        setOnClickListener {
                            openFontChooser(FONT_ADD_FALLBACK_CODE)
                        }
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun openFontChooser(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        try {
            this@FontManagerActivity.startActivityForResult(intent, requestCode)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this@FontManagerActivity, R.string.explorer_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FONT_ADD_CODE || requestCode == FONT_ADD_FALLBACK_CODE) if (resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            var name = uri.path ?: return
            while (name.endsWith("/")) name = name.substring(0, name.length - 1)
            if (name.contains("/")) name = name.substring(name.lastIndexOf("/") + 1)
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) name = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        OpenableColumns.DISPLAY_NAME
                    )
                )
            }
            name.replace("[?:\"*|/\\\\<>]".toRegex(), "_")
            try {
                (contentResolver.openInputStream(uri) ?: throw IOException()).use { `is` ->
                    val of = File(filesDir, "00000000/$name")
                    of.parentFile?.mkdirs()
                    FileOutputStream(of).use { os ->
                        val crc = CRC32()
                        val buf = ByteArray(256)
                        var size: Int
                        while (`is`.read(buf).also { size = it } > 0) {
                            os.write(buf, 0, size)
                            crc.update(buf, 0, size)
                        }
                        val mf = File(filesDir, String.format("%08x", crc.value) + "/" + name)
                        mf.parentFile?.mkdirs()
                        of.renameTo(mf)

                        object : FileChooser.Listener {
                            override fun onFileChosen(path: String) {
                                if (path.endsWith(".zip")) {
                                    FileChooser(this@FontManagerActivity, this, path).onClick(null, -1)
                                    return
                                }
                                val font = when (requestCode) {
                                    FONT_ADD_CODE -> FontData.SingleFont(path)
                                    FONT_ADD_FALLBACK_CODE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        FontData.FallbackFont().apply {
                                            paths.add(path)
                                        }
                                    } else null
                                    else -> null
                                }
                                try {
                                    if (font == null) throw FontData.BaseFont.FontCouldNotBeLoadedException(Exception())
                                    font.getTypeface()
                                    fontData.add(font)
                                    adapter.notifyItemInserted(fontData.getFonts().size - 1)
                                    pref.edit {
                                        fontData.saveToPreferences(this)
                                    }
                                } catch (e: FontData.BaseFont.FontCouldNotBeLoadedException) {
                                    Toast.makeText(this@FontManagerActivity, R.string.cantopen, Toast.LENGTH_SHORT).show()
                                    try {
                                        if (path.startsWith(this@FontManagerActivity.filesDir.canonicalPath)) File(path).delete()
                                    } catch (_: IOException) {
                                    }
                                }
                            }

                            override fun onFileCancel() {
                            }
                        }.onFileChosen(mf.canonicalPath)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (requestCode == FONT_EDIT_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) if (resultCode == RESULT_OK && data != null) {
            val fontIndex = data.getIntExtra("fontIndex", -1)
            val fontFallbackJson = data.getStringExtra("fontFallback") ?: return
            if (fontIndex < 0 || fontIndex >= fontData.getFonts().size) return
            val font = fontData.getFonts()[fontIndex]
            if (font !is FontData.FallbackFont) return
            val unusedPaths = font.paths.toHashSet()
            font.deserialize(fontFallbackJson)
            font.iterPaths.forEach {
                unusedPaths.remove(it)
            }
            unusedPaths.forEach {
                try {
                    if (it.startsWith(filesDir.canonicalPath)) {
                        File(it).let { f ->
                            f.delete()
                            f.parentFile?.let { dir ->
                                if (dir.list()?.isEmpty() == true) {
                                    dir.delete()
                                }
                            }
                        }
                    }
                } catch (_: IOException) {
                }
            }
            adapter.notifyItemChanged(fontIndex)
            pref.edit {
                fontData.saveToPreferences(this)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val FONT_ADD_CODE = 100
        const val FONT_ADD_FALLBACK_CODE = 101
        const val FONT_EDIT_CODE = 102
    }
}
