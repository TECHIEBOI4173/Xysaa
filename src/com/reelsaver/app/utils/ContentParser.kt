package com.reelsaver.app.utils

import com.reelsaver.app.model.SavedItem
import com.reelsaver.app.model.SavedItemType
import java.util.Date
import java.util.regex.Pattern

object ContentParser {

    // YouTube URL patterns
    private val YT_PATTERNS = listOf(
        Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:www\\.)?youtu\\.be/([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
        Pattern.compile("(?:https?://)?(?:m\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})")
    )

    // Instagram URL patterns
    private val INSTAGRAM_PATTERNS = listOf(
        Pattern.compile("(?:https?://)?(?:www\\.)?instagram\\.com/reel/([a-zA-Z0-9_-]+)/?"),
        Pattern.compile("(?:https?://)?(?:www\\.)?instagram\\.com/p/([a-zA-Z0-9_-]+)/?")
    )

    // Common place indicators in captions
    private val PLACE_KEYWORDS = listOf(
        "📍", "🗺", "🏖", "🏔", "🏕", "🌊", "🌴", "🏛", "🗼", "🗽", "🏯", "🏰",
        "location:", "place:", "at:", "visiting:", "📌"
    )

    private val PLACE_PATTERN = Pattern.compile(
        "(?:📍|📌|🗺️?|location[:\\s]+|place[:\\s]+|at[:\\s]+)([^\\n#@]{3,60})",
        Pattern.CASE_INSENSITIVE
    )

    private val HASHTAG_PATTERN = Pattern.compile("#([a-zA-Z0-9_]+)")

    private val MAPS_PATTERN = Pattern.compile(
        "(?:https?://)?(?:maps\\.google\\.com|goo\\.gl/maps|maps\\.app\\.goo\\.gl)/[^\\s]+"
    )

    data class ParseResult(
        val type: SavedItemType,
        val youtubeVideoIds: List<String> = emptyList(),
        val placeInfo: PlaceInfo? = null,
        val hashtags: List<String> = emptyList(),
        val mapsLinks: List<String> = emptyList(),
        val instagramCode: String = "",
        val rawText: String = ""
    )

    data class PlaceInfo(
        val name: String,
        val address: String = "",
        val mapsUrl: String = ""
    )

    fun parse(sharedText: String, sharedUrl: String = ""): ParseResult {
        val fullText = "$sharedText $sharedUrl".trim()

        // Extract hashtags
        val hashtags = extractHashtags(fullText)

        // Extract maps links
        val mapsLinks = extractMapsLinks(fullText)

        // Check for YouTube links first
        val ytIds = extractYouTubeIds(fullText)
        if (ytIds.isNotEmpty()) {
            return ParseResult(
                type = SavedItemType.YOUTUBE_VIDEO,
                youtubeVideoIds = ytIds,
                hashtags = hashtags,
                mapsLinks = mapsLinks,
                rawText = fullText
            )
        }

        // Check for Instagram reel
        val igCode = extractInstagramCode(fullText)

        // Try to extract place info
        val placeInfo = extractPlaceInfo(fullText, mapsLinks)

        return if (placeInfo != null) {
            ParseResult(
                type = SavedItemType.PLACE,
                placeInfo = placeInfo,
                hashtags = hashtags,
                mapsLinks = mapsLinks,
                instagramCode = igCode,
                rawText = fullText
            )
        } else if (igCode.isNotEmpty()) {
            ParseResult(
                type = SavedItemType.INSTAGRAM_REEL,
                instagramCode = igCode,
                hashtags = hashtags,
                rawText = fullText
            )
        } else {
            ParseResult(
                type = SavedItemType.UNKNOWN,
                hashtags = hashtags,
                rawText = fullText
            )
        }
    }

    fun extractYouTubeIds(text: String): List<String> {
        val ids = mutableListOf<String>()
        for (pattern in YT_PATTERNS) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val id = matcher.group(1)
                if (id != null && !ids.contains(id)) {
                    ids.add(id)
                }
            }
        }
        return ids
    }

    fun extractInstagramCode(text: String): String {
        for (pattern in INSTAGRAM_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1) ?: ""
            }
        }
        return ""
    }

    fun extractPlaceInfo(text: String, mapsLinks: List<String> = emptyList()): PlaceInfo? {
        // Try emoji-based place detection
        val matcher = PLACE_PATTERN.matcher(text)
        if (matcher.find()) {
            val name = matcher.group(1)?.trim()?.trimEnd(',', '.') ?: ""
            if (name.length >= 3) {
                return PlaceInfo(
                    name = name,
                    mapsUrl = mapsLinks.firstOrNull() ?: ""
                )
            }
        }

        // If maps link found, extract place from URL or use as address
        if (mapsLinks.isNotEmpty()) {
            val placeName = extractPlaceFromMapsUrl(mapsLinks.first())
            return PlaceInfo(
                name = placeName.ifEmpty { "Saved Location" },
                mapsUrl = mapsLinks.first()
            )
        }

        // Detect place from hashtags like #bali #paris #NYC
        val placeHashtags = extractHashtags(text).filter { tag ->
            KNOWN_TRAVEL_TAGS.any { it.equals(tag, ignoreCase = true) }
        }
        if (placeHashtags.isNotEmpty()) {
            return PlaceInfo(name = placeHashtags.first().replaceFirstChar { it.uppercase() })
        }

        return null
    }

    private fun extractPlaceFromMapsUrl(url: String): String {
        // Try to get place name from Google Maps URL
        val placePattern = Pattern.compile("[?&]q=([^&]+)")
        val matcher = placePattern.matcher(url)
        if (matcher.find()) {
            return java.net.URLDecoder.decode(matcher.group(1) ?: "", "UTF-8")
        }
        return ""
    }

    fun extractHashtags(text: String): List<String> {
        val tags = mutableListOf<String>()
        val matcher = HASHTAG_PATTERN.matcher(text)
        while (matcher.find()) {
            matcher.group(1)?.let { tags.add(it) }
        }
        return tags.distinct()
    }

    fun extractMapsLinks(text: String): List<String> {
        val links = mutableListOf<String>()
        val matcher = MAPS_PATTERN.matcher(text)
        while (matcher.find()) {
            links.add(matcher.group())
        }
        return links
    }

    fun buildSavedItem(result: ParseResult, userNotes: String = ""): SavedItem {
        return when (result.type) {
            SavedItemType.YOUTUBE_VIDEO -> {
                val firstId = result.youtubeVideoIds.first()
                SavedItem(
                    title = "YouTube Video",
                    description = result.rawText.take(200),
                    url = "https://www.youtube.com/watch?v=$firstId",
                    thumbnailUrl = "https://img.youtube.com/vi/$firstId/hqdefault.jpg",
                    type = SavedItemType.YOUTUBE_VIDEO,
                    youtubeVideoId = firstId,
                    tags = result.hashtags.joinToString(","),
                    sourceUrl = result.rawText,
                    notes = userNotes
                )
            }
            SavedItemType.PLACE -> {
                val place = result.placeInfo!!
                SavedItem(
                    title = place.name,
                    description = result.rawText.take(200),
                    url = place.mapsUrl,
                    type = SavedItemType.PLACE,
                    placeName = place.name,
                    placeAddress = place.address,
                    tags = result.hashtags.joinToString(","),
                    sourceUrl = result.instagramCode.let {
                        if (it.isNotEmpty()) "https://www.instagram.com/reel/$it/" else ""
                    },
                    notes = userNotes
                )
            }
            SavedItemType.INSTAGRAM_REEL -> {
                SavedItem(
                    title = "Instagram Reel",
                    description = result.rawText.take(200),
                    url = "https://www.instagram.com/reel/${result.instagramCode}/",
                    type = SavedItemType.INSTAGRAM_REEL,
                    tags = result.hashtags.joinToString(","),
                    sourceUrl = result.rawText,
                    notes = userNotes
                )
            }
            else -> {
                SavedItem(
                    title = "Saved Content",
                    description = result.rawText.take(200),
                    type = SavedItemType.UNKNOWN,
                    tags = result.hashtags.joinToString(","),
                    sourceUrl = result.rawText,
                    notes = userNotes
                )
            }
        }
    }

    private val KNOWN_TRAVEL_TAGS = setOf(
        "bali", "paris", "london", "tokyo", "newyork", "nyc", "dubai", "singapore",
        "rome", "barcelona", "amsterdam", "berlin", "sydney", "bangkok", "istanbul",
        "miami", "losangeles", "la", "chicago", "vegas", "lasvegas", "santorini",
        "maldives", "hawaii", "greece", "italy", "france", "spain", "japan",
        "india", "thailand", "indonesia", "bali", "phuket", "goa", "kerala",
        "rajasthan", "mumbai", "delhi", "bangalore", "hyderabad", "kolkata",
        "switzerland", "iceland", "norway", "canada", "australia", "newzealand",
        "scotland", "ireland", "portugal", "mexico", "cancun", "cuba", "brazil",
        "peru", "argentina", "colombia", "costarica", "morocco", "egypt", "kenya",
        "southafrica", "turkey", "croatia", "prague", "vienna", "budapest"
    )
}
