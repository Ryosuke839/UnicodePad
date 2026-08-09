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

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.woxthebox.draglistview.DragItemAdapter
import com.woxthebox.draglistview.DragListView
import java.io.File


class FontManagerAdapter internal constructor(private val activity: Activity, private val fontData: FontData) : DragItemAdapter<FontData.BaseFont, FontManagerAdapter.ItemViewHolder>(), OnClickListener, DragListView.DragListListener {
    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

    init {
        mItemList = fontData.getFonts()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(activity.layoutInflater.inflate(R.layout.fontitem, parent, false)).apply {
            button.setOnClickListener(this@FontManagerAdapter)
        }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.apply {
            fontData.getFonts()[position].let {
                if (it.name.isEmpty()) {
                    title.text = it.subtitle
                    subtitle.text = ""
                    subtitle.visibility = View.GONE
                } else {
                    title.text = it.name
                    subtitle.text = it.subtitle
                    subtitle.visibility = View.VISIBLE
                }
                button.tag = position
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getUniqueItemId(position: Int): Long {
        return fontData.getFonts()[position].hashCode().toLong()
    }

    override fun onClick(view: View) {
        val i = view.tag as Int
        val font = fontData.getFonts()[i]
        PopupMenu(activity, view).apply {
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.font_rename -> {
                        val editText = EditText(activity).apply {
                            setText(font.name)
                            hint = font.subtitle
                        }
                        AlertDialog.Builder(activity).apply {
                            setTitle(R.string.font_rename)
                            setView(editText)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                font.name = editText.text.toString()
                                notifyItemChanged(i)
                                pref.edit {
                                    fontData.saveToPreferences(this)
                                }
                            }
                            setNegativeButton(android.R.string.cancel, null)
                        }.show()
                        true
                    }
                    R.id.font_edit -> {
                        if (font is FontData.FallbackFont && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            activity.startActivityForResult(Intent(activity, FontFallbackActivity::class.java).apply {
                                putExtra("fontIndex", i)
                                putExtra("fontFallback", font.serialize())
                            }, FontManagerActivity.FONT_EDIT_CODE)
                        }
                        true
                    }
                    R.id.font_delete -> {
                        val unusedPaths = fontData.getFonts()[i].iterPaths.asSequence().toHashSet()
                        fontData.delete(i)
                        fontData.iterPaths.forEach {
                            unusedPaths.remove(it)
                        }
                        unusedPaths.forEach {
                            if (it.startsWith(activity.filesDir.canonicalPath)) {
                                File(it).let { f ->
                                    f.delete()
                                    f.parentFile?.let { dir ->
                                        if (dir.list()?.isEmpty() == true) {
                                            dir.delete()
                                        }
                                    }
                                }
                            }
                        }
                        notifyItemRemoved(i)
                        pref.edit {
                            fontData.saveToPreferences(this)
                        }
                        true
                    }
                    else -> false
                }
            }
            menu.apply {
                add(0, R.id.font_rename, 0, R.string.font_rename)
                if (font is FontData.FallbackFont) {
                    add(0, R.id.font_edit, 0, R.string.font_edit)
                }
                add(0, R.id.font_delete, 0, R.string.font_delete)
            }
            show()
        }
    }

    override fun onItemDragStarted(position: Int) {
    }

    override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {
    }

    override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
        pref.edit {
            fontData.saveToPreferences(this)
        }
    }

    class ItemViewHolder(itemView: View) : DragItemAdapter.ViewHolder(itemView, R.id.HANDLE_ID, false) {
        val title: TextView = itemView.findViewById(R.id.font_title)
        val subtitle: TextView = itemView.findViewById(R.id.font_subtitle)
        val button: ImageButton = itemView.findViewById(R.id.font_menu)
    }
}