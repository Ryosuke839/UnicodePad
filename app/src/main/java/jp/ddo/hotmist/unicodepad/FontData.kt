package jp.ddo.hotmist.unicodepad

import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class FontData {
    @Serializable
    sealed class BaseFont {
        var name: String = ""
        abstract val subtitle: String
        abstract fun getTypeface(): Typeface
        abstract val iterPaths: Iterator<String>

        class FontCouldNotBeLoadedException(base: Exception) : Exception(base)
    }

    class DummyFont(override val subtitle: String) : BaseFont() {
        override fun getTypeface(): Typeface {
            return Typeface.DEFAULT
        }

        override val iterPaths: Iterator<String>
            get() = iterator { }
    }

    @Serializable
    class SingleFont(val path: String) : BaseFont() {
        override val subtitle: String
            get() = File(path).name

        override fun getTypeface(): Typeface {
            return try {
                Typeface.createFromFile(path)
            } catch (e: RuntimeException) {
                throw FontCouldNotBeLoadedException(e)
            }
        }

        override val iterPaths: Iterator<String>
            get() = iterator { yield(path) }
    }

    @Serializable
    @RequiresApi(Build.VERSION_CODES.Q)
    class FallbackFont : BaseFont() {
        var paths: MutableList<String> = mutableListOf()

        override val subtitle: String
            get() = if (paths.isEmpty()) "" else "${File(paths[0]).name} +${paths.size - 1}"

        override fun getTypeface(): Typeface {
            return paths.fold(null as Typeface.CustomFallbackBuilder?) { b, path ->
                val font = Font.Builder(File(path)).build()
                val family = FontFamily.Builder(font).build()
                b?.addCustomFallback(family) ?: Typeface.CustomFallbackBuilder(family)
            }?.build() ?: Typeface.DEFAULT
        }

        override val iterPaths: Iterator<String>
            get() = paths.iterator()

        fun serialize(): String {
            return Json.encodeToString(serializer(), this)
        }

        fun deserialize(json: String) {
            val deserialized = Json.decodeFromString<FallbackFont>(json)
            this.name = deserialized.name
            this.paths = deserialized.paths
        }
    }

    private val fonts: MutableList<BaseFont> = mutableListOf()

    fun getFonts(): List<BaseFont> = fonts

    val iterPaths: Iterator<String>
        get() = iterator {
            fonts.forEach { yieldAll(it.iterPaths) }
        }

    fun add(font: BaseFont) {
        fonts.add(font)
    }

    fun delete(index: Int) {
        fonts.removeAt(index)
    }

    fun loadFromPreferences(pref: android.content.SharedPreferences) {
        val json = pref.getString("fonts_v2", null)
        if (json != null) {
            try {
                val loadedFonts = Json.decodeFromString<List<BaseFont>>(json)
                fonts.clear()
                fonts.addAll(loadedFonts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val fs = pref.getString("fontpath", null) ?: ""
            for (s in fs.split("\n").toTypedArray()) {
                if (s.isEmpty()) continue
                fonts.add(SingleFont(s))
            }
        }
    }

    fun saveToPreferences(edit: android.content.SharedPreferences.Editor) {
        edit.putString("fonts_v2", Json.encodeToString(ListSerializer(BaseFont.serializer()), fonts))
        edit.putString("fontpath", fonts.joinToString("\n") { it.iterPaths.asSequence().firstOrNull() ?: "" })
    }
}