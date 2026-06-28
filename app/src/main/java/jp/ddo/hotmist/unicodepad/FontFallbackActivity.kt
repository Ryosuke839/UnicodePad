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
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woxthebox.draglistview.DragListView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.CRC32

@RequiresApi(Build.VERSION_CODES.Q)
class FontFallbackActivity : BaseActivity() {
    private var fontIndex: Int = 0
    private lateinit var fontFallback: FontData.FallbackFont
    private lateinit var adapter: FontFallbackAdapter
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
        intent.extras?.let {
            fontIndex = it.getInt("fontIndex")
            fontFallback = FontData.FallbackFont().apply {
                deserialize(it.getString("fontFallback")!!)
            }
        }
        adapter = FontFallbackAdapter(this, fontFallback)
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(EditText(this@FontFallbackActivity).apply {
                setText(fontFallback.name)
                hint = fontFallback.subtitle
                setSingleLine()
                doAfterTextChanged {
                    fontFallback.name = it.toString()
                }
            }.also {
                adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                    override fun onChanged() {
                        super.onChanged()
                        it.post {
                            it.hint = fontFallback.subtitle
                        }
                    }

                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        super.onItemRangeInserted(positionStart, itemCount)
                        it.post {
                            it.hint = fontFallback.subtitle
                        }
                    }

                    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                        super.onItemRangeRemoved(positionStart, itemCount)
                        it.post {
                            it.hint = fontFallback.subtitle
                        }
                    }
                })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(DynamicDragListView(this@FontFallbackActivity, null).apply {
                setLayoutManager(LinearLayoutManager(this@FontFallbackActivity))
                setDragListListener(this@FontFallbackActivity.adapter)
                setAdapter(this@FontFallbackActivity.adapter, true)
                setCanDragHorizontally(false)
                setCanDragVertically(true)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(LinearLayout(this@FontFallbackActivity).apply {
                addView(Button(this@FontFallbackActivity).apply {
                    setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_add, 0, 0, 0)
                    setText(R.string.font_add_existing)
                    setOnClickListener {
                        val paths = FontData().apply {
                            loadFromPreferences(pref)
                        }.getFonts().mapNotNull {
                            (it as? FontData.SingleFont)?.path?.let { path ->
                                if (fontFallback.paths.contains(path)) null else path
                            }
                        }
                        if (paths.isEmpty()) {
                            Toast.makeText(this@FontFallbackActivity, R.string.font_no_existing, Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        AlertDialog.Builder(context)
                            .setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, paths.map {
                                File(it).name
                            })) { _, i ->
                                fontFallback.paths.add(paths[i])
                                try {
                                    fontFallback.getTypeface()
                                    adapter.notifyItemInserted(fontFallback.paths.size - 1)
                                } catch (e: FontData.BaseFont.FontCouldNotBeLoadedException) {
                                    Toast.makeText(this@FontFallbackActivity, R.string.cantopen, Toast.LENGTH_SHORT).show()
                                    fontFallback.paths.removeAt(fontFallback.paths.size - 1)
                                }
                            }
                            .show()
                    }
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(Button(this@FontFallbackActivity).apply {
                    setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_add, 0, 0, 0)
                    setText(R.string.font_add_file)
                    setOnClickListener {
                        openFontChooser()
                    }
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        })

        onBackPressedDispatcher.addCallback {
            setResult(RESULT_OK, Intent().apply {
                putExtra("fontIndex", fontIndex)
                putExtra("fontFallback", fontFallback.serialize())
            })
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        setResult(RESULT_OK, Intent().apply {
            putExtra("fontIndex", fontIndex)
            putExtra("fontFallback", fontFallback.serialize())
        })
        finish()
        return true
    }

    private fun openFontChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        try {
            this@FontFallbackActivity.startActivityForResult(intent, FONT_ADD_CODE)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this@FontFallbackActivity, R.string.explorer_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FONT_ADD_CODE) if (resultCode == RESULT_OK && data != null) {
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
            name = name.replace("[?:\"*|/\\\\<>]".toRegex(), "_")
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
                                    FileChooser(this@FontFallbackActivity, this, path).onClick(null, -1)
                                    return
                                }
                                fontFallback.paths.add(path)
                                try {
                                    fontFallback.getTypeface()
                                    adapter.notifyItemInserted(fontFallback.paths.size - 1)
                                } catch (e: FontData.BaseFont.FontCouldNotBeLoadedException) {
                                    Toast.makeText(this@FontFallbackActivity, R.string.cantopen, Toast.LENGTH_SHORT).show()
                                    try {
                                        if (path.startsWith(this@FontFallbackActivity.filesDir.canonicalPath)) File(path).delete()
                                    } catch (_: IOException) {
                                    }
                                    fontFallback.paths.removeAt(fontFallback.paths.size - 1)
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
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val FONT_ADD_CODE = 200
    }
}
