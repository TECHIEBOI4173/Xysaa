package com.reelsaver.app.model

import java.util.Date

enum class SavedItemType {
    PLACE,
    YOUTUBE_VIDEO,
    INSTAGRAM_REEL,
    UNKNOWN
}

data class SavedItem(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val url: String = "",
    val thumbnailUrl: String = "",
    val type: SavedItemType,
    val placeName: String = "",
    val placeAddress: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val youtubeVideoId: String = "",
    val youtubeChannelName: String = "",
    val tags: String = "",
    val sourceUrl: String = "",
    val savedAt: Date = Date(),
    val notes: String = ""
)
