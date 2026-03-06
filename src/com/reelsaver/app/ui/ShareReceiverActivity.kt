package com.reelsaver.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.reelsaver.app.R
import com.reelsaver.app.ReelSaverApp
import com.reelsaver.app.databinding.ActivityShareReceiverBinding
import com.reelsaver.app.model.SavedItemType
import com.reelsaver.app.utils.ContentParser

class ShareReceiverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareReceiverBinding
    private var parsedResult: ContentParser.ParseResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleSharedIntent(intent)
        setupButtons()
    }

    private fun handleSharedIntent(intent: Intent) {
        val sharedText = when {
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            }
            intent.action == Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            }
            else -> ""
        }

        if (sharedText.isEmpty()) {
            showError("No content was shared.")
            return
        }

        binding.tvRawContent.text = sharedText
        analyzeContent(sharedText)
    }

    private fun analyzeContent(text: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardResult.visibility = View.GONE

        // Parse in background
        Thread {
            val result = ContentParser.parse(text)
            parsedResult = result

            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                displayResult(result)
            }
        }.start()
    }

    private fun displayResult(result: ContentParser.ParseResult) {
        binding.cardResult.visibility = View.VISIBLE

        when (result.type) {
            SavedItemType.YOUTUBE_VIDEO -> {
                binding.tvDetectedType.text = "🎬 YouTube Video Detected"
                binding.tvDetectedType.setBackgroundResource(R.drawable.bg_tag_youtube)
                val ids = result.youtubeVideoIds.joinToString("\n") {
                    "▶ youtube.com/watch?v=$it"
                }
                binding.tvExtractedInfo.text = ids
                binding.ivThumbnail.visibility = View.VISIBLE
                // Load thumbnail using Glide
                loadYoutubeThumbnail(result.youtubeVideoIds.first())
            }

            SavedItemType.PLACE -> {
                binding.tvDetectedType.text = "📍 Place Detected"
                binding.tvDetectedType.setBackgroundResource(R.drawable.bg_tag_place)
                val place = result.placeInfo!!
                val info = buildString {
                    append("📍 ${place.name}")
                    if (place.address.isNotEmpty()) append("\n🏠 ${place.address}")
                    if (place.mapsUrl.isNotEmpty()) append("\n🗺 Maps link found")
                    if (result.hashtags.isNotEmpty()) {
                        append("\n\n🏷 Tags: ")
                        append(result.hashtags.take(5).joinToString(" ") { "#$it" })
                    }
                }
                binding.tvExtractedInfo.text = info
                binding.ivThumbnail.visibility = View.GONE
            }

            SavedItemType.INSTAGRAM_REEL -> {
                binding.tvDetectedType.text = "📱 Instagram Reel"
                binding.tvDetectedType.setBackgroundResource(R.drawable.bg_tag_instagram)
                binding.tvExtractedInfo.text = "Reel ID: ${result.instagramCode}\n\nTags: ${
                    result.hashtags.take(5).joinToString(" ") { "#$it" }
                }"
                binding.ivThumbnail.visibility = View.GONE
            }

            else -> {
                binding.tvDetectedType.text = "🔗 Content Saved"
                binding.tvDetectedType.setBackgroundResource(R.drawable.bg_tag_default)
                binding.tvExtractedInfo.text = "Content saved. Add notes below."
                binding.ivThumbnail.visibility = View.GONE
            }
        }
    }

    private fun loadYoutubeThumbnail(videoId: String) {
        try {
            val url = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            // Use Glide to load thumbnail
            com.bumptech.glide.Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_video_placeholder)
                .into(binding.ivThumbnail)
        } catch (e: Exception) {
            binding.ivThumbnail.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveItem()
        }

        binding.btnDiscard.setOnClickListener {
            finish()
        }
    }

    private fun saveItem() {
        val result = parsedResult ?: return
        val notes = binding.etNotes.text.toString()

        val item = ContentParser.buildSavedItem(result, notes)
        val app = application as ReelSaverApp
        val id = app.database.insertItem(item)

        if (id > 0) {
            Toast.makeText(this, "✅ Saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "❌ Failed to save. Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(msg: String) {
        binding.tvRawContent.text = msg
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
