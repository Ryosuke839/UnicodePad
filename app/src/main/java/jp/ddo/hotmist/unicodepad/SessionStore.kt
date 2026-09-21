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

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal object SessionMark {
    const val NONE = 0
    const val COPIED = 1 shl 0
    const val SHARED = 1 shl 1
    const val FAVORITE = 1 shl 2
    const val CURRENT = 1 shl 3
}

internal enum class SessionStatus {
    NEW, CURRENT, PREVIOUS, NONE
}

internal data class HistoryEntry(
    val text: String,
    val selStart: Int,
    val selEnd: Int,
)

internal class EditSession(
    val history: MutableList<HistoryEntry>,
    var cursor: Int,
    var mark: Int,
) {
    val text: String
        get() = history.getOrNull(cursor)?.text ?: ""
    val selStart: Int
        get() = history.getOrNull(cursor)?.selStart ?: 0
    val selEnd: Int
        get() = history.getOrNull(cursor)?.selEnd ?: 0
    val isFavorite: Boolean
        get() = mark and SessionMark.FAVORITE != 0

    fun isPrunableEmpty(): Boolean =
        (mark and SessionMark.CURRENT.inv()) == SessionMark.NONE && history.size <= 1 && text.isEmpty()
}

internal data class SessionListItem(
    val session: EditSession?,
    val text: String,
    val status: SessionStatus,
) {
    fun displayText(maxChars: Int = 80): String {
        val t = text.replace('\n', ' ').replace('\r', ' ')
        return if (t.length <= maxChars) t else t.substring(0, maxChars) + "…"
    }
}

internal class SessionStore(private val pref: SharedPreferences) {
    private val sessions = mutableListOf<EditSession>()

    val current: EditSession
        get() = findCurrent() ?: sessions.lastOrNull()?.also { applyCurrentMark(it) } ?: startNew()

    val isEmpty: Boolean
        get() = sessions.isEmpty()

    fun load() {
        sessions.clear()
        val json = pref.getString(PREF_SESSIONS, null) ?: return
        try {
            val obj = JSONObject(json)
            val arr = obj.optJSONArray("sessions") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val history = readHistory(s.optJSONArray("history") ?: JSONArray())
                if (history.isEmpty()) {
                    history.add(HistoryEntry("", 0, 0))
                }
                val cursor = s.optInt("cursor").coerceIn(0, history.lastIndex)
                sessions.add(
                    EditSession(
                        history = history,
                        cursor = cursor,
                        mark = s.optInt("mark"),
                    )
                )
            }
            if (sessions.isNotEmpty()) {
                applyCurrentMark(findCurrent() ?: sessions.last())
            }
        } catch (_: JSONException) {
            sessions.clear()
        }
    }

    fun save(edit: SharedPreferences.Editor? = null) {
        prune()
        val json = toJson()
        if (edit != null) {
            edit.putString(PREF_SESSIONS, json)
        } else {
            pref.edit { putString(PREF_SESSIONS, json) }
        }
    }

    fun startNew(text: String = ""): EditSession {
        val entry = HistoryEntry(text, text.length, text.length)
        val reusable = findCurrent()?.takeIf { it.isPrunableEmpty() }
            ?: sessions.lastOrNull()?.takeIf { it.isPrunableEmpty() }
        if (reusable != null) {
            reusable.history.clear()
            reusable.history.add(entry)
            reusable.cursor = 0
            applyCurrentMark(reusable)
            save()
            return reusable
        }
        val session = EditSession(
            history = mutableListOf(entry),
            cursor = 0,
            mark = SessionMark.CURRENT,
        )
        sessions.add(session)
        applyCurrentMark(session)
        prune()
        save()
        return session
    }

    fun armBranch(mark: Int) {
        val cur = current
        cur.mark = cur.mark or mark
        save()
    }

    fun toggleFavorite(session: EditSession) {
        session.mark = session.mark xor SessionMark.FAVORITE
        save()
    }

    fun delete(session: EditSession) {
        if (session === findCurrent()) return
        sessions.remove(session)
        save()
    }

    fun setCurrent(session: EditSession) {
        if (session !in sessions) return
        applyCurrentMark(session)
        save()
    }

    fun branch(source: EditSession = current): EditSession {
        val session = EditSession(
            history = source.history.map { it.copy() }.toMutableList(),
            cursor = source.cursor,
            mark = SessionMark.CURRENT,
        )
        sessions.add(session)
        applyCurrentMark(session)
        return session
    }

    fun recordEdit(text: String, selStart: Int, selEnd: Int) {
        var cur = current
        if (text == cur.text) return
        if (cur !== sessions.lastOrNull() || cur.mark != SessionMark.CURRENT) {
            cur = branch()
        }
        while (cur.history.size > cur.cursor + 1) {
            cur.history.removeAt(cur.history.lastIndex)
        }
        while (cur.history.size >= MAX_HISTORY) {
            cur.history.removeAt(0)
        }
        cur.history.add(HistoryEntry(text, selStart, selEnd))
        cur.cursor = cur.history.lastIndex
    }

    fun undo(): Boolean {
        var cur = current
        if (cur.cursor <= 0) return false
        if (cur !== sessions.lastOrNull() || cur.mark != SessionMark.CURRENT) {
            cur = branch()
        }
        cur.cursor -= 1
        return true
    }

    fun redo(): Boolean {
        var cur = current
        if (cur.cursor >= cur.history.lastIndex) return false
        if (cur !== sessions.lastOrNull() || cur.mark != SessionMark.CURRENT) {
            cur = branch()
        }
        cur.cursor += 1
        return true
    }

    fun listItems(atLaunch: Boolean = false): List<SessionListItem> {
        val items = mutableListOf(
            SessionListItem(null, "", SessionStatus.NEW)
        )
        sessions.asReversed().forEach { s ->
            val status = when {
                s.mark and SessionMark.CURRENT == 0 -> SessionStatus.NONE
                atLaunch -> SessionStatus.PREVIOUS
                else -> SessionStatus.CURRENT
            }
            items.add(SessionListItem(s, s.text, status))
        }
        return items
    }

    private fun findCurrent(): EditSession? =
        sessions.lastOrNull { it.mark and SessionMark.CURRENT != 0 }

    private fun applyCurrentMark(session: EditSession) {
        for (s in sessions) {
            if (s === session) {
                s.mark = s.mark or SessionMark.CURRENT
            } else {
                s.mark = s.mark and SessionMark.CURRENT.inv()
            }
        }
    }

    private fun prune() {
        val cur = findCurrent()
        sessions.removeAll { s ->
            s !== cur && s.isPrunableEmpty()
        }
        while (sessions.size > MAX_SESSIONS) {
            val to_remove = sessions.firstOrNull { it !== cur && !it.isFavorite } ?: break
            sessions.remove(to_remove)
        }
    }

    private fun toJson(): String {
        val obj = JSONObject()
        val arr = JSONArray()
        for (s in sessions) {
            val so = JSONObject()
            so.put("cursor", s.cursor)
            so.put("mark", s.mark)
            so.put("history", writeHistory(s.history))
            arr.put(so)
        }
        obj.put("sessions", arr)
        return obj.toString()
    }

    private fun writeHistory(history: List<HistoryEntry>): JSONArray {
        val hist = JSONArray()
        var prev: String? = null
        for (h in history) {
            hist.put(encodeHistoryEntry(prev, h))
            prev = h.text
        }
        return hist
    }

    private fun encodeHistoryEntry(prev: String?, h: HistoryEntry): JSONObject {
        val obj = JSONObject()
        if (prev == null) {
            obj.put("t", h.text)
        } else {
            val (start, deleteCount, insert) = commonSplice(prev, h.text)
            val full = JSONObject().put("t", h.text)
            val diff = JSONObject().put("d", encodeDiff(prev.length, start, deleteCount, insert))
            if (diff.toString().length < full.toString().length) {
                obj.put("d", diff.get("d"))
            } else {
                obj.put("t", h.text)
            }
        }
        obj.put("s", h.selStart)
        obj.put("e", h.selEnd)
        return obj
    }

    private fun readHistory(histArr: JSONArray): MutableList<HistoryEntry> {
        val history = mutableListOf<HistoryEntry>()
        var prev = ""
        for (j in 0 until histArr.length()) {
            val h = histArr.optJSONObject(j) ?: continue
            val text = decodeHistoryText(prev, h)
            history.add(HistoryEntry(text, h.optInt("s"), h.optInt("e")))
            prev = text
        }
        return history
    }

    private fun decodeHistoryText(prev: String, h: JSONObject): String {
        if (h.has("d")) {
            val d = h.optJSONObject("d")
            if (d != null) {
                decodeDiff(prev, d)?.let { return it }
            }
        }
        return if (h.has("t")) h.optString("t") else prev
    }

    private fun encodeDiff(prevLength: Int, start: Int, deleteCount: Int, insert: String): JSONObject {
        val d = JSONObject()
        if (start != prevLength) d.put("s", start)
        if (deleteCount != 0) d.put("n", deleteCount)
        if (insert.isNotEmpty()) d.put("i", insert)
        return d
    }

    private fun decodeDiff(prev: String, d: JSONObject): String? {
        val start = d.optInt("s", prev.length)
        val deleteCount = d.optInt("n", 0)
        val insert = d.optString("i", "")
        val end = start + deleteCount
        if (start in 0..prev.length && end in start..prev.length) {
            return prev.substring(0, start) + insert + prev.substring(end)
        }
        return null
    }

    private fun commonSplice(prev: String, curr: String): Triple<Int, Int, String> {
        val start = prev.commonPrefixWith(curr).length
        val suffixLen = prev.substring(start).commonSuffixWith(curr.substring(start)).length
        return Triple(start, prev.length - start - suffixLen, curr.substring(start, curr.length - suffixLen))
    }

    companion object {
        const val PREF_SESSIONS = "sessions"
        const val PREF_STARTUP = "session_startup"
        const val STARTUP_PREVIOUS = "previous"
        const val STARTUP_NEW = "new"
        const val STARTUP_CHOOSER = "chooser"
        const val MAX_SESSIONS = 32
        const val MAX_HISTORY = 256
    }
}

internal class SessionListAdapter(
    private val store: SessionStore,
    atLaunch: Boolean,
    private val onSelect: (EditSession?) -> Unit,
) : RecyclerView.Adapter<SessionListAdapter.ViewHolder>() {
    private val items = store.listItems(atLaunch).toMutableList()

    internal class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.session_text)
        val status: TextView = view.findViewById(R.id.session_status)
        val favorite: ImageButton = view.findViewById(R.id.session_favorite)
        val delete: ImageButton = view.findViewById(R.id.session_delete)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.sessionitem, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val session = item.session
        holder.text.text = item.displayText()
        holder.status.text = statusLabel(holder.itemView.context, item)
        holder.itemView.setOnClickListener { onSelect(session) }
        holder.favorite.run {
            isVisible = session != null
            setImageResource(starOf(session))
            setOnClickListener {
                if (session == null) return@setOnClickListener
                store.toggleFavorite(session)
                setImageResource(starOf(session))
            }
        }
        holder.delete.run {
            isVisible = session != null
            isEnabled = session != null && session !== store.current
            alpha = if (isEnabled) 1f else 0.3f
            setOnClickListener {
                val index = holder.bindingAdapterPosition
                if (session == null || index == RecyclerView.NO_POSITION) return@setOnClickListener
                store.delete(session)
                items.removeAt(index)
                notifyItemRemoved(index)
            }
        }
    }

    private fun starOf(session: EditSession?): Int =
        if (session?.isFavorite == true) R.drawable.ic_star else R.drawable.ic_star_border

    private fun statusLabel(context: Context, item: SessionListItem): String {
        val parts = mutableListOf<String>()
        when (item.status) {
            SessionStatus.NEW -> parts.add(context.getString(R.string.session_new))
            SessionStatus.CURRENT -> parts.add(context.getString(R.string.session_current))
            SessionStatus.PREVIOUS -> parts.add(context.getString(R.string.session_previous))
            SessionStatus.NONE -> {}
        }
        val mark = item.session?.mark ?: SessionMark.NONE
        if (mark and SessionMark.COPIED != 0) parts.add(context.getString(R.string.session_copied))
        if (mark and SessionMark.SHARED != 0) parts.add(context.getString(R.string.session_shared))
        return parts.joinToString(" / ")
    }
}
