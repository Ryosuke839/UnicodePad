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
import android.content.Context
import android.content.res.AssetManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipInputStream

class NameDatabase(context: Context) {
    private val db: SQLiteDatabase = NameHelper(context).readableDatabase
    operator fun get(code: Int, column: String): String? {
        if (column == "name") {
            if (code in 0xE000..0xF8FF || code in 0xFFF80..0xFFFFD || code in 0x10FF80..0x10FFFD) return "Private Use"
            if (code in 0x3400..0x4DBF || code in 0x4E00..0x9FFC || code in 0x20000..0x2A6DD || code in 0x2A700..0x2B734 || code in 0x2B740..0x2B81D || code in 0x2B820..0x2CEA1 || code in 0x2CEB0..0x2EBE0 || code in 0x30000..0x3134A) return "CJK Unified Ideograph"
            if (code in 0xAC00..0xD7A3) return "Hangul Syllable"
            if (code in 0x17000..0x187F7) return "Tangut Character"
        }
        return get("name_table", code.toString(), column)
    }

    operator fun get(code: String, column: String): String? {
        return get("emoji_table", "'$code'", column)
    }

    private operator fun get(table: String, code: String, column: String): String? {
        return try {
            db.rawQuery("SELECT $column FROM $table WHERE id = $code", null).use { cur ->
                if (cur.count != 1) return null
                cur.moveToFirst()
                cur.getString(0)
            }
        } catch (e: SQLiteException) {
            "Error: " + e.localizedMessage
        }
    }

    fun getInt(code: Int, column: String): Int {
        if (column == "version") {
            if (code in 0xE000..0xF8FF || code in 0xFFF80..0xFFFFD || code in 0x10FF80..0x10FFFD) return 600
            if (code in 0x3400..0x4DB5 || code in 0x4E00..0x9FCB || code in 0x20000..0x2A6D6 || code in 0x2A700..0x2B734 || code in 0x2B740..0x2B81D) return 600
            if (code in 0x9FCC..0x9FCC) return 610
            if (code in 0x9FCD..0x9FD5 || code in 0x2B820..0x2CEA1) return 800
            if (code in 0x17000..0x187EC) return 900
            if (code in 0x9FD6..0x9FEA || code in 0x2CEB0..0x2EBE0) return 1000
            if (code in 0x9FEB..0x9FEF || code in 0x187ED..0x187F1) return 1100
            if (code in 0xAC00..0xD7A3) return 600
            if (code in 0x187F2..0x187F7) return 1200
            if (code in 0x4DB6..0x4DBF || code in 0x9FF0..0x9FFC || code in 0x2A6D7..0x2A6DD || code in 0x30000..0x3134A) return 1300
        }
        return getInt("name_table", code.toString(), column)
    }

    fun getInt(code: String, column: String): Int {
        return getInt("emoji_table", "'$code'", column)
    }

    private fun getInt(table: String, code: String, column: String): Int {
        return try {
            db.rawQuery("SELECT $column FROM $table WHERE id = $code", null).use { cur ->
                if (cur.count != 1)
                    return 0
                cur.moveToFirst()
                cur.getInt(0)
            }
        } catch (e: SQLiteException) {
            0
        }
    }

    @SuppressLint("Recycle")
    fun find(str: String, version: Int): Cursor? {
        val list = str.split(" +").toTypedArray()
        if (list.isEmpty()) return null
        val emojiVersion = when (version) {
            600, 610, 620, 630 -> 60
            700 -> 70
            800 -> 100
            else -> version
        }
        return try {
            db.rawQuery("SELECT id FROM name_table WHERE " + list.joinToString(" ") { "words LIKE '%$it%' AND " } + "version <= $version UNION ALL SELECT id FROM emoji_table WHERE " + list.joinToString(" ") { "name LIKE '%$it%' AND " } + "version <= $emojiVersion;", null)
        } catch (e: SQLiteException) {
            null
        }
    }

    @SuppressLint("Recycle")
    fun emoji(version: Int, mod: Boolean): Cursor? {
        val emojiVersion = when (version) {
            600, 610, 620, 630 -> 60
            700 -> 70
            800 -> 100
            else -> version
        }
        return try {
            db.rawQuery("SELECT id, grp, subgrp FROM emoji_table WHERE version <= $emojiVersion" + if (mod) ";" else " AND mod = 0;", null)
        } catch (e: SQLiteException) {
            null
        }
    }

    internal inner class NameHelper(private val context: Context) : SQLiteOpenHelper(context, "namedb", null, 1) {
        private val dbpath = "namedb"
        override fun onCreate(db: SQLiteDatabase) {}
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        override fun getReadableDatabase(): SQLiteDatabase {
            return try {
                val db = SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                try {
                    db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='name_table' OR name='emoji_table';", null).use { cur ->
                        cur.moveToFirst()
                        if (cur.getInt(0) != 2) throw SQLiteException()
                    }
                    db.rawQuery("SELECT COUNT(*) FROM 'name_table';", null).use { cur ->
                        cur.moveToFirst()
                        if (cur.getInt(0) != 33805) throw SQLiteException()
                    }
                    db.rawQuery("SELECT COUNT(*) FROM 'emoji_table';", null).use { cur ->
                        cur.moveToFirst()
                        if (cur.getInt(0) != 3295) throw SQLiteException()
                    }
                } catch (e: SQLiteException) {
                    db.close()
                    throw e
                }
                db
            } catch (e: SQLiteException) {
                if (e.message?.contains("attempt to write a readonly database") == true) {
                    SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS).close()
                    return SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                }
                extractZipFiles("namedb.zip")
                try {
                    SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                } catch (e: SQLiteException) {
                    if (e.message?.contains("attempt to write a readonly database") == true) {
                        SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS).close()
                        return SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                    }
                    throw e
                }
            }
        }

        private fun extractZipFiles(@Suppress("SameParameterValue") zipName: String) {
            try {
                val inputStream = context.assets.open(zipName, AssetManager.ACCESS_STREAMING)
                ZipInputStream(inputStream).use { zipInputStream ->
                    var zipEntry = zipInputStream.nextEntry
                    while (zipEntry != null) {
                        val entryName = zipEntry.name
                        var n: Int
                        context.openFileOutput(entryName, Context.MODE_PRIVATE).use { fileOutputStream ->
                            val buf = ByteArray(16384)
                            while (zipInputStream.read(buf, 0, 16384).also { n = it } > -1) fileOutputStream.write(buf, 0, n)
                        }
                        zipInputStream.closeEntry()
                        zipEntry = zipInputStream.nextEntry
                    }
                }
            } catch (e: FileNotFoundException) {
                throw Error("Cannot open database file to write.")
            } catch (e: IOException) {
                throw Error("Cannot open database file from asset.")
            }
        }
    }

}