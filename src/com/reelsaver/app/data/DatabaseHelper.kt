package com.reelsaver.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.reelsaver.app.model.SavedItem
import com.reelsaver.app.model.SavedItemType
import java.util.Date

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "reelsaver.db"
        const val DATABASE_VERSION = 1

        const val TABLE_ITEMS = "saved_items"
        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_DESCRIPTION = "description"
        const val COL_URL = "url"
        const val COL_THUMBNAIL = "thumbnail_url"
        const val COL_TYPE = "type"
        const val COL_PLACE_NAME = "place_name"
        const val COL_PLACE_ADDRESS = "place_address"
        const val COL_LAT = "latitude"
        const val COL_LON = "longitude"
        const val COL_YT_ID = "youtube_video_id"
        const val COL_YT_CHANNEL = "youtube_channel"
        const val COL_TAGS = "tags"
        const val COL_SOURCE_URL = "source_url"
        const val COL_SAVED_AT = "saved_at"
        const val COL_NOTES = "notes"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_ITEMS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_DESCRIPTION TEXT,
                $COL_URL TEXT,
                $COL_THUMBNAIL TEXT,
                $COL_TYPE TEXT NOT NULL,
                $COL_PLACE_NAME TEXT,
                $COL_PLACE_ADDRESS TEXT,
                $COL_LAT REAL DEFAULT 0,
                $COL_LON REAL DEFAULT 0,
                $COL_YT_ID TEXT,
                $COL_YT_CHANNEL TEXT,
                $COL_TAGS TEXT,
                $COL_SOURCE_URL TEXT,
                $COL_SAVED_AT INTEGER,
                $COL_NOTES TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEMS")
        onCreate(db)
    }

    fun insertItem(item: SavedItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, item.title)
            put(COL_DESCRIPTION, item.description)
            put(COL_URL, item.url)
            put(COL_THUMBNAIL, item.thumbnailUrl)
            put(COL_TYPE, item.type.name)
            put(COL_PLACE_NAME, item.placeName)
            put(COL_PLACE_ADDRESS, item.placeAddress)
            put(COL_LAT, item.latitude)
            put(COL_LON, item.longitude)
            put(COL_YT_ID, item.youtubeVideoId)
            put(COL_YT_CHANNEL, item.youtubeChannelName)
            put(COL_TAGS, item.tags)
            put(COL_SOURCE_URL, item.sourceUrl)
            put(COL_SAVED_AT, item.savedAt.time)
            put(COL_NOTES, item.notes)
        }
        return db.insert(TABLE_ITEMS, null, values)
    }

    fun getAllItems(): List<SavedItem> {
        val items = mutableListOf<SavedItem>()
        val db = readableDatabase
        val cursor = db.query(TABLE_ITEMS, null, null, null, null, null, "$COL_SAVED_AT DESC")

        cursor.use {
            while (it.moveToNext()) {
                items.add(cursorToItem(it))
            }
        }
        return items
    }

    fun getItemsByType(type: SavedItemType): List<SavedItem> {
        val items = mutableListOf<SavedItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ITEMS, null,
            "$COL_TYPE = ?", arrayOf(type.name),
            null, null, "$COL_SAVED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                items.add(cursorToItem(it))
            }
        }
        return items
    }

    fun searchItems(query: String): List<SavedItem> {
        val items = mutableListOf<SavedItem>()
        val db = readableDatabase
        val searchQuery = "%$query%"
        val cursor = db.query(
            TABLE_ITEMS, null,
            "$COL_TITLE LIKE ? OR $COL_DESCRIPTION LIKE ? OR $COL_PLACE_NAME LIKE ? OR $COL_TAGS LIKE ?",
            arrayOf(searchQuery, searchQuery, searchQuery, searchQuery),
            null, null, "$COL_SAVED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                items.add(cursorToItem(it))
            }
        }
        return items
    }

    fun deleteItem(id: Long): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_ITEMS, "$COL_ID = ?", arrayOf(id.toString())) > 0
    }

    fun updateNotes(id: Long, notes: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put(COL_NOTES, notes) }
        return db.update(TABLE_ITEMS, values, "$COL_ID = ?", arrayOf(id.toString())) > 0
    }

    private fun cursorToItem(cursor: android.database.Cursor): SavedItem {
        return SavedItem(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)) ?: "",
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)) ?: "",
            url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL)) ?: "",
            thumbnailUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_THUMBNAIL)) ?: "",
            type = SavedItemType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)) ?: "UNKNOWN"),
            placeName = cursor.getString(cursor.getColumnIndexOrThrow(COL_PLACE_NAME)) ?: "",
            placeAddress = cursor.getString(cursor.getColumnIndexOrThrow(COL_PLACE_ADDRESS)) ?: "",
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LAT)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LON)),
            youtubeVideoId = cursor.getString(cursor.getColumnIndexOrThrow(COL_YT_ID)) ?: "",
            youtubeChannelName = cursor.getString(cursor.getColumnIndexOrThrow(COL_YT_CHANNEL)) ?: "",
            tags = cursor.getString(cursor.getColumnIndexOrThrow(COL_TAGS)) ?: "",
            sourceUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_SOURCE_URL)) ?: "",
            savedAt = Date(cursor.getLong(cursor.getColumnIndexOrThrow(COL_SAVED_AT))),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)) ?: ""
        )
    }
}
