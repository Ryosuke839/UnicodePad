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
import android.content.res.AssetManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipInputStream

class NameDatabase(context: Context?) {
    private val db: SQLiteDatabase
    operator fun get(code: Int, column: String): String? {
        if (column == "name") {
            if (0xE000 <= code && code <= 0xF8FF || 0xFFF80 <= code && code <= 0xFFFFD || 0x10FF80 <= code && code <= 0x10FFFD) return "Private Use"
            if (0x3400 <= code && code <= 0x4DBF || 0x4E00 <= code && code <= 0x9FFC || 0x20000 <= code && code <= 0x2A6DD || 0x2A700 <= code && code <= 0x2B734 || 0x2B740 <= code && code <= 0x2B81D || 0x2B820 <= code && code <= 0x2CEA1 || 0x2CEB0 <= code && code <= 0x2EBE0 || 0x30000 <= code && code <= 0x3134A) return "CJK Unified Ideograph"
            if (0xAC00 <= code && code <= 0xD7A3) return "Hangul Syllable"
            if (0x17000 <= code && code <= 0x187F7) return "Tangut Character"
        }
        return get("name_table", code.toString(), column)
    }

    operator fun get(code: String?, column: String): String? {
        return get("emoji_table", "'$code'", column)
    }

    private operator fun get(table: String, code: String, column: String): String? {
        return try {
            val cur = db.rawQuery("SELECT $column FROM $table WHERE id = $code", null)
            if (cur.count != 1) {
                cur.close()
                return null
            }
            cur.moveToFirst()
            val str = cur.getString(0)
            cur.close()
            str
        } catch (e: SQLiteException) {
            "Error: " + e.localizedMessage
        }
    }

    fun getint(code: Int, column: String): Int {
        if (column == "version") {
            if (0xE000 <= code && code <= 0xF8FF || 0xFFF80 <= code && code <= 0xFFFFD || 0x10FF80 <= code && code <= 0x10FFFD) return 600
            if (0x3400 <= code && code <= 0x4DB5 || 0x4E00 <= code && code <= 0x9FCB || 0x20000 <= code && code <= 0x2A6D6 || 0x2A700 <= code && code <= 0x2B734 || 0x2B740 <= code && code <= 0x2B81D) return 600
            if (0x9FCC <= code && code <= 0x9FCC) return 610
            if (0x9FCD <= code && code <= 0x9FD5 ||
                    0x2B820 <= code && code <= 0x2CEA1) return 800
            if (0x17000 <= code && code <= 0x187EC) return 900
            if (0x9FD6 <= code && code <= 0x9FEA ||
                    0x2CEB0 <= code && code <= 0x2EBE0) return 1000
            if (0x9FEB <= code && code <= 0x9FEF ||
                    0x187ED <= code && code <= 0x187F1) return 1100
            if (0xAC00 <= code && code <= 0xD7A3) return 600
            if (0x187F2 <= code && code <= 0x187F7) return 1200
            if (0x4DB6 <= code && code <= 0x4DBF || 0x9FF0 <= code && code <= 0x9FFC || 0x2A6D7 <= code && code <= 0x2A6DD || 0x30000 <= code && code <= 0x3134A) return 1300
        }
        return getint("name_table", code.toString(), column)
    }

    fun getint(code: String?, column: String): Int {
        return getint("emoji_table", "'$code'", column)
    }

    private fun getint(table: String, code: String, column: String): Int {
        return try {
            val cur = db.rawQuery("SELECT $column FROM $table WHERE id = $code", null)
            if (cur.count != 1) {
                cur.close()
                return 0
            }
            cur.moveToFirst()
            val res = cur.getInt(0)
            cur.close()
            res
        } catch (e: SQLiteException) {
            0
        }
    }

    fun find(str: String, version: Int): Cursor? {
        val list = str.split(" +").toTypedArray()
        if (list.size == 0) return null
        var query = "SELECT id FROM name_table WHERE "
        for (s in list) query += "words LIKE '%$s%' AND "
        query += "version <= $version;"
        return try {
            db.rawQuery(query, null)
        } catch (e: SQLiteException) {
            null
        }
    }

    fun emoji(version: Int, mod: Boolean): Cursor? {
        var version = version
        when (version) {
            600, 610, 620, 630 -> version = 60
            700 -> version = 70
            800 -> version = 100
        }
        val query = "SELECT id, grp, subgrp FROM emoji_table WHERE version <= " + version + if (mod) ";" else " AND mod = 0;"
        return try {
            db.rawQuery(query, null)
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
                val cur = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='name_table' OR name='emoji_table';", null)
                cur.moveToFirst()
                if (cur.getInt(0) != 2) {
                    cur.close()
                    db.close()
                    throw SQLiteException()
                }
                cur.close()
                val cur2 = db.rawQuery("SELECT COUNT(*) FROM 'name_table';", null)
                cur2.moveToFirst()
                if (cur2.getInt(0) != 33805) {
                    cur2.close()
                    db.close()
                    throw SQLiteException()
                }
                cur2.close()
                val cur3 = db.rawQuery("SELECT COUNT(*) FROM 'emoji_table';", null)
                cur3.moveToFirst()
                if (cur3.getInt(0) != 3295) {
                    cur3.close()
                    db.close()
                    throw SQLiteException()
                }
                cur3.close()
                db
            } catch (e: SQLiteException) {
                if (e.message != null && e.message!!.contains("attempt to write a readonly database")) {
                    SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS).close()
                    return SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                }
                extractZipFiles("namedb.zip")
                try {
                    SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                } catch (e1: SQLiteException) {
                    if (e.message != null && e.message!!.contains("attempt to write a readonly database")) {
                        SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS).close()
                        return SQLiteDatabase.openDatabase(context.getFileStreamPath(dbpath).absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                    }
                    throw e1
                }
            }
        }

        private fun extractZipFiles(zipName: String) {
            try {
                val inputStream = context.assets.open(zipName, AssetManager.ACCESS_STREAMING)
                val zipInputStream = ZipInputStream(inputStream)
                var zipEntry = zipInputStream.nextEntry
                while (zipEntry != null) {
                    val entryName = zipEntry.name
                    var n: Int
                    val fileOutputStream = context.openFileOutput(entryName, Context.MODE_PRIVATE)
                    val buf = ByteArray(16384)
                    while (zipInputStream.read(buf, 0, 16384).also { n = it } > -1) fileOutputStream.write(buf, 0, n)
                    fileOutputStream.close()
                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
                zipInputStream.close()
            } catch (e: FileNotFoundException) {
                throw Error("Cannot open database file to write.")
            } catch (e: IOException) {
                throw Error("Cannot open database file from asset.")
            }
        }
    }

    init {
        db = NameHelper(context).getReadableDatabase()
    }
}